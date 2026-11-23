package com.instantaxtion.zombiesandbox;

import static com.instantaxtion.zombiesandbox.Fleet.*;

import java.util.ArrayList;

/**
 * ((27.3) Part of the vehicle code, out of Fleet.) Helicopters: those on their pads at stations and bases, launching, airlifts, the rescue helicopter, landing zones, and the helicopter's own step (updateHeli).
 */
final class FleetHelis {
    final Fleet fleet;
    final World w;
    final City city;

    FleetHelis(Fleet fleet) {
        this.fleet = fleet;
        this.w = fleet.w;
        this.city = fleet.city;
    }

    /** ((17.6) One of the city's helicopters shot down: spinning, losing height, and a fireball where it comes down. */
    boolean heliCrash(Vehicle v, float dt) {
        v.angle += dt * 5;
        v.x += (float) Math.cos(v.angle) * 20 * dt;
        v.y += (float) Math.sin(v.angle) * 20 * dt;
        v.alt = Math.max(0, v.alt - dt * 0.5f);
        if (v.alt > 0) return false;
        v.crew.clear();
        v.riders.clear();
        v.driver = null;
        w.explode(v.x, v.y, 40, 150);
        w.dispatch.say(Dispatch.WHO_MILITARY, null, (v.kind == K_POLICE_HELI || v.kind == K_RESCUE_HELI ? "Police: " : "Military: ")
                + "Helicopter down on " + city.placeName(v.x, v.y) + "!", v.x, v.y, true);
        w.timeline("Helicopter shot down", v.x, v.y);
        return true;
    }

    private int armyPads;

    void stationHelis() {
        armyPads = 0;
        java.util.List<float[]> pads = city.helipads();
        for (int k = 0; k < pads.size(); k++) {
            float[] p = pads.get(k);
            Vehicle v = null;
            for (int i = 0; i < fleet.vehicles.size() && v == null; i++) if (fleet.vehicles.get(i).padAt == p) v = fleet.vehicles.get(i);
            if (v == null) {
                if (fleet.padLost.contains(p)) continue;
                v = fleet.make(HELI);
                v.padAt = p;
                v.pad = true;
                v.x = v.homeX = v.baseX = p[0];
                v.y = v.homeY = v.baseY = p[1];
                v.angle = -(float) Math.PI / 2;
                v.alt = 0;
                v.state = IDLE;
                int kind = City.padKind(p);
                if (kind == City.PAD_POLICE) fleet.setKind(v, K_POLICE_HELI);
                // ((31.0) Map version 51: two army pads in three have a gunship on them (the first a transport).)
                else if (kind == City.PAD_ARMY && city.cfg.civic >= 51 && armyPads++ % 3 != 0) {
                    fleet.setKind(v, K_GUNSHIP);
                    v.number = ++fleet.airNumber;
                }
                else if (kind == City.PAD_RESCUE) {
                    fleet.setKind(v, K_RESCUE_HELI);
                    v.number = ++fleet.rescueNumber;
                } else v.number = ++fleet.airNumber;
                fleet.vehicles.add(v);
            }
            if (v.state != IDLE) continue;
            // The flight crew, standing by on the pad.
            for (int i = v.flight.size() - 1; i >= 0; i--) if (v.flight.get(i).dead && !v.crew.contains(v.flight.get(i))) v.flight.remove(i);
            boolean army = v.kind != K_POLICE_HELI && v.kind != K_RESCUE_HELI;
            int want = v.kind == K_RESCUE_HELI ? 4 : 1;
            // (After loading a saved game the crew are already standing about: they're taken back on, not doubled.)
            while (v.flight.size() < want) {
                boolean pilot = livePilot(v) == null;
                Entity found = null;
                float fd = Float.MAX_VALUE;
                for (int i = 0, n = w.entities.size(); i < n; i++) {
                    Entity e = w.entities.get(i);
                    if (e.dead || (pilot ? e.role != Entity.ROLE_PILOT : e.role != Entity.ROLE_SAR) || e.type != (army ? Entity.SOLDIER : Entity.COP)) continue;
                    float d = (float) World.hypot(e.x - p[0], e.y - p[1]);
                    if (d >= fd || inFlight(e)) continue;
                    fd = d;
                    found = e;
                }
                if (found == null) break;
                v.flight.add(found);
            }
            // ((11.0) Once the first crew's gone, a new pilot comes from those who signed up as reserve pilots, not
            // from nowhere. With none left, it stays on the ground.)
            if (livePilot(v) == null && v.crewSpawns > 0) {
                if (v.pilotComing == null || v.pilotComing.dead || v.pilotComing.pilotFor != v) v.pilotComing = w.callReservePilot(v);
                if (v.flight.isEmpty()) continue;
            }
            while (v.flight.size() < want && v.crewSpawns < 8) {
                boolean pilot = livePilot(v) == null;
                if (pilot && v.crewSpawns > 0) break;
                v.crewSpawns++;
                Entity e = pilot ? w.spawn(army ? Entity.SOLDIER : Entity.COP, p[0] + 30, p[1] + 10)
                        : w.spawnCop(Entity.ROLE_SAR, p[0] + 30, p[1] + 10);
                if (e == null) break;
                if (pilot) {
                    e.role = Entity.ROLE_PILOT;
                    e.callsign = v.number;
                } else {
                    e.callsign = v.number;
                    e.member = v.flight.size() - 1;
                }
                v.flight.add(e);
            }
            for (int i = 0; i < v.flight.size(); i++) {
                Entity e = v.flight.get(i);
                if (e.dead || e.task == Dispatch.T_POST || e.task == Dispatch.T_BOARD) continue;
                if (e.task != Dispatch.T_NONE && e.task != Dispatch.T_RESCUE) continue;
                standBy(v, e, i);
            }
        }
    }

    boolean inFlight(Entity e) {
        for (int i = 0; i < fleet.vehicles.size(); i++) if (fleet.vehicles.get(i).flight.contains(e)) return true;
        return false;
    }

    /** A member of the flight crew waits at the side of the pad. */
    void standBy(Vehicle v, Entity e, int i) {
        e.task = Dispatch.T_POST;
        e.rig = null;
        e.postX = v.padAt[0] + 30 + (i % 2) * 8;
        e.postY = v.padAt[1] - 8 + i * 9;
    }

    /** Helicopters of a kind (0 the army's) the city has. */
    int helisOfKind(int kind) {
        int n = 0;
        for (int i = 0; i < fleet.vehicles.size(); i++) if (fleet.vehicles.get(i).type == HELI && fleet.vehicles.get(i).kind == kind) n++;
        return n;
    }

    /** The helicopter's pilot, alive (on foot or aboard), or null: no pilot, no flight. */
    Entity livePilot(Vehicle v) {
        for (int i = 0; i < v.flight.size(); i++) {
            Entity e = v.flight.get(i);
            if (e.role == Entity.ROLE_PILOT && (!e.dead || v.crew.contains(e))) return e;
        }
        return null;
    }

    /** The nearest helicopter of a kind (0 the army's) sitting on its pad ready to go, or null. */
    Vehicle readyHeli(int kind, float x, float y) {
        Vehicle best = null;
        float bd = Float.MAX_VALUE;
        for (int i = 0; i < fleet.vehicles.size(); i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (v.type != HELI || v.kind != kind || v.state != IDLE || v.refuel > 0 || v.padAt == null || livePilot(v) == null) continue;
            if (kind == K_RESCUE_HELI && sarOnFoot(v) == 0) continue;
            float d = (v.x - x) * (v.x - x) + (v.y - y) * (v.y - y);
            if (d < bd) {
                bd = d;
                best = v;
            }
        }
        return best;
    }

    int sarOnFoot(Vehicle v) {
        int n = 0;
        for (Entity e : v.flight) if (!e.dead && e.role == Entity.ROLE_SAR) n++;
        return n;
    }

    /**
     * Sends a helicopter off its pad: the pilot (and the rescue team, door gunners from the base or a police
     * marksman) walk out to it and climb aboard, it spins up, and only then does it take off. extra: how many
     * more to take from the free units nearby (door gunners, troops).
     */
    /**
     * ((31.0) Every few seconds in a real outbreak: each gunship ready on its pad goes up over the worst fight that
     * no other gunship is over already (six of the dead or more).
     */
    private float gunshipCd = 3;

    void gunships(float dt) {
        if ((gunshipCd -= dt) > 0) return;
        gunshipCd = 3;
        if (w.zombies < 12) return;
        for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (v.type != HELI || v.kind != K_GUNSHIP || v.state != IDLE || v.refuel > 0 || livePilot(v) == null) continue;
            Dispatch.Incident best = null;
            for (int k = 0, m = w.dispatch.incidents.size(); k < m; k++) {
                Dispatch.Incident q = w.dispatch.incidents.get(k);
                if (q.resolved || q.zombiesNear < 6 || (best != null && q.zombiesNear <= best.zombiesNear)) continue;
                boolean covered = false;
                for (int j = 0; j < n && !covered; j++) {
                    Vehicle o = fleet.vehicles.get(j);
                    if (o != v && o.kind == K_GUNSHIP && o.state != IDLE && o.incident != null && World.hypot(o.incident.x - q.x, o.incident.y - q.y) < 400) covered = true;
                }
                if (!covered) best = q;
            }
            if (best == null) return;
            if (launchHeli(v, best.x, best.y, best.place, best, 0))
                w.dispatch.say(Dispatch.WHO_MILITARY, null, "Gunship " + v.number + " scrambling for " + best.place + ".", best.x, best.y);
        }
    }

    boolean launchHeli(Vehicle v, float tx, float ty, String place, Dispatch.Incident inc, int extra) {
        if (v == null || v.state != IDLE) return false;
        Entity pilot = livePilot(v);
        if (pilot == null) return false;
        v.tx = tx;
        v.ty = ty;
        v.place = place;
        v.incident = inc;
        v.team = null;
        v.pickup = false;
        v.recall = false;
        v.standby = false;
        v.passengers = 0;
        v.troops = 0;
        v.state = MUSTER;
        v.timer = 0;
        int n = 0;
        for (Entity e : v.flight) {
            if (e.dead) continue;
            e.task = Dispatch.T_BOARD;
            e.rig = v; fleet.crews.note(e);
            n++;
        }
        int type = v.kind == K_POLICE_HELI || v.kind == K_RESCUE_HELI ? Entity.COP : Entity.SOLDIER;
        for (int k = 0; k < extra; k++) {
            Entity best = null;
            float bd = 600 * 600;
            for (int i = 0, m = w.entities.size(); i < m; i++) {
                Entity e = w.entities.get(i);
                if (e.dead || e.type != type || e.task != Dispatch.T_NONE || e.rig != null || e.agency != 0 || e.scout) continue;
                if (e.role == Entity.ROLE_COMMANDER || e.role == Entity.ROLE_GUARD || e.ammo + e.reserve <= 0) continue;
                float d = (e.x - v.x) * (e.x - v.x) + (e.y - v.y) * (e.y - v.y);
                if (d < bd) {
                    bd = d;
                    best = e;
                }
            }
            if (best == null) break;
            best.task = Dispatch.T_BOARD;
            best.rig = v; fleet.crews.note(best);
            n++;
        }
        v.crewWanted = n;
        return true;
    }

    /**
     * (10.52) Reinforcements by air: an army helicopter flies in from the edge of the map at (fx, fy) with a squad
     * aboard, puts them down near (tx, ty), covers them for a minute with its door guns, and flies back out the way
     * it came. Returns the soldiers (still aboard), or null if there's no room.
     */
    ArrayList<Entity> airlift(int count, float fx, float fy, float tx, float ty, String place, Dispatch.Incident inc) {
        if (w.entities.size() + count + 3 > w.maxEntities + World.OVER_CAP) return null;
        Vehicle v = fleet.make(HELI);
        v.x = fx;
        v.y = fy;
        v.angle = (float) Math.atan2(ty - fy, tx - fx);
        v.alt = 1;
        v.state = FLY_IN;
        v.tx = tx;
        v.ty = ty;
        v.place = place;
        v.incident = inc;
        v.padAt = null;
        v.baseX = fx;
        v.baseY = fy;
        v.number = ++fleet.airNumber;
        v.reinforcement = true;
        ArrayList<Entity> squad = new ArrayList<Entity>();
        for (int i = 0; i < count + 3; i++) {
            Entity e = w.create(Entity.SOLDIER, fx, fy);
            // (The pilot, the squad, and last the two door gunners, who stay aboard.)
            if (i == 0) {
                e.role = Entity.ROLE_PILOT;
                v.flight.add(e);
            } else if (i <= count) {
                w.kitOut(e);
                squad.add(e);
            }
            e.dead = true;
            e.removed = true;
            e.rig = v; fleet.crews.note(e);
            v.crew.add(e);
        }
        v.troops = count;
        fleet.vehicles.add(v);
        return squad;
    }

    /** Lands it back on its pad: everyone climbs out, the flight crew stand by, the rest go back to their duties. */
    void landed(Vehicle v) {
        v.alt = 0;
        v.hvx = v.hvy = v.speed = 0;
        v.x = v.padAt[0];
        v.y = v.padAt[1];
        v.state = IDLE;
        v.refuel = 40;
        v.escort = null;
        v.team = null;
        v.incident = null;
        ArrayList<Entity> out = new ArrayList<Entity>(v.crew);
        fleet.crews.crewOut(v, true);
        for (Entity e : out) {
            int i = v.flight.indexOf(e);
            if (i >= 0 && !e.dead) standBy(v, e, i);
        }
    }

    /**
     * (10.27) A police or army helicopter checks its call is still live: once the call's over, or there's been
     * nothing to shoot near it for a few seconds, it goes on to the nearest call that still has the dead at it,
     * or (with none) stands down on its pad or flies home. True if it has just been sent home from the pad.
     */
    boolean staleCall(Vehicle v, float dt) {
        if (v.kind == K_RESCUE_HELI || v.escort != null) return false;
        // ((10.52) Reinforcements flown in put their troops down whatever: it's the soldiers' call after that.)
        if (v.reinforcement && v.troops > 0) return false;
        if (v.state != MUSTER && v.state != SPOOL && v.state != FLY_IN && v.state != CIRCLE) return false;
        v.quiet = w.countZombiesNear(v.tx, v.ty, 260) > 0 ? 0 : v.quiet + dt;
        boolean over = v.incident != null && v.incident.resolved;
        if (!over && v.quiet < (v.state == CIRCLE ? 12 : 6)) return false;
        v.quiet = 0;
        String was = v.place != null ? v.place : "the scene";
        boolean police = v.kind == K_POLICE_HELI;
        int who = police ? Dispatch.WHO_POLICE : Dispatch.WHO_MILITARY;
        String name = police ? "Police helicopter" : "Air " + Math.max(1, v.number);
        Dispatch.Incident next = w.dispatch.liveCallNear(v.x, v.y, 1800, v.incident);
        if (next != null) {
            v.incident = next;
            v.tx = next.x;
            v.ty = next.y;
            v.place = next.place;
            if (v.state == CIRCLE) v.state = FLY_IN;
            w.dispatch.say(who, null, name + ": Nothing left at " + was + ". Moving on to " + next.place + ".", v.x, v.y);
            return false;
        }
        if (v.state == MUSTER || v.state == SPOOL) {
            // Still on the pad: stand down.
            for (int i = 0, n = w.entities.size(); i < n; i++) {
                Entity e = w.entities.get(i);
                if (!e.dead && e.rig == v && e.task == Dispatch.T_BOARD) {
                    e.task = Dispatch.T_NONE;
                    e.rig = null;
                }
            }
            if (v.padAt != null) {
                landed(v);
                v.refuel = 5;
                w.dispatch.say(who, null, name + ": Call's over at " + was + ". Standing down.", v.x, v.y);
                return true;
            }
        }
        v.state = FLY_OUT;
        v.incident = null;
        v.homeX = v.baseX;
        v.homeY = v.baseY;
        w.dispatch.say(who, null, name + ": All quiet at " + was + ". Heading back.", v.x, v.y);
        return false;
    }

    /**
     * (14.7) Flying a helicopter: it lifts off, then goes wherever the stick points (over anything: it's in the air),
     * nose first, and hovers when you let go. The door gunner fires at the dead below - faster with the button held.
     */
    boolean playerHeli(Vehicle v, float dt) {
        Entity e = v.player;
        v.alt += (0.8f - v.alt) * Math.min(1, dt * 0.8f);
        float jx = w.joyX, jy = w.joyY, mag = Math.min(1, (float) Math.sqrt(jx * jx + jy * jy));
        float gx = v.x, gy = v.y, speed = 0, face = v.angle;
        if (v.alt > 0.4f && mag > 0.15f) {
            face = (float) Math.atan2(jy, jx);
            gx = v.x + jx / mag * 220;
            gy = v.y + jy / mag * 220;
            speed = 135 * mag;
        }
        gx = Math.max(10, Math.min(city.worldW() - 10, gx));
        gy = Math.max(10, Math.min(city.worldH() - 10, gy));
        flyTo(v, gx, gy, Math.max(speed, 1), face, dt);
        v.x = Math.max(5, Math.min(city.worldW() - 5, v.x));
        v.y = Math.max(5, Math.min(city.worldH() - 5, v.y));
        e.x = v.x;
        e.y = v.y;
        if (v.soundCd <= 0) {
            v.soundCd = 0.6f;
            w.emit(Sfx.ROTOR, v.x, v.y);
        }
        v.gunCd -= dt;
        if (v.alt > 0.4f && v.gunCd <= 0) {
            Entity z = w.nearestZombie(v.x, v.y, 210);
            if (z != null) {
                v.burst++;
                v.gunCd = w.ctrlAttack ? (v.burst % 10 == 0 ? 0.6f : 0.08f) : (v.burst % 6 == 0 ? 1.4f : 0.13f);
                w.airShot(v.x, v.y, z);
            } else v.gunCd = 0.4f;
        }
        return false;
    }

    // ------------------------------------------------------------------ helicopter

    /** ((27.3) What a state's step says of the helicopter: fly on to where it aims, this step is over, or it has gone for good. */
    static final int HELI_GO = 0, HELI_STOP = 1, HELI_GONE = 2;
    /** (Where the helicopter being stepped is to fly to: see updateHeli.) */
    private final float[] heliAim = new float[4];

    boolean updateHeli(Vehicle v, float dt) {
        if (staleCall(v, dt)) return false;
        if (v.state == IDLE) {
            // On the pad: rotor still, refuelling after a flight.
            v.refuel = Math.max(0, v.refuel - dt);
            v.speed = v.hvx = v.hvy = 0;
            v.alt = 0;
            return false;
        }
        if (v.state == MUSTER) return heliMuster(v, dt);
        if (v.soundCd <= 0) {
            v.soundCd = v.state == SPOOL ? 0.9f : 0.6f;
            w.emit(Sfx.ROTOR, v.x, v.y);
        }
        // Rotor wash kicks up dust when it's low.
        if ((v.alt < 0.6f && v.state != SPOOL) || (v.state == SPOOL && v.timer < 3)) {
            if (w.rnd.nextFloat() < dt * 25) {
                float a = w.rnd.nextFloat() * (float) Math.PI * 2;
                w.particle(v.x + (float) Math.cos(a) * 10, v.y + (float) Math.sin(a) * 10, (float) Math.cos(a) * 45,
                        (float) Math.sin(a) * 45, 0.8f, 2.5f, 0xFFB8AE98, World.P_SMOKE);
            }
        }
        if (v.state == SPOOL) {
            // Spinning up on the pad (or wherever it set down).
            v.timer -= dt;
            if (v.timer <= 0) v.state = FLY_IN;
            return false;
        }
        // ((27.3) Where it flies to this step, and how - {x, y, speed, facing or NaN} - worked out by its state's step.)
        float[] aim = heliAim;
        aim[0] = v.x;
        aim[1] = v.y;
        aim[2] = 110;
        aim[3] = Float.NaN;
        // (Escorting a convoy: wherever its head is now.)
        if (v.escort != null) {
            if (w.dispatch.convoys.contains(v.escort)) {
                v.tx = v.escort.x;
                v.ty = v.escort.y;
            } else if (v.state == CIRCLE) {
                v.escort = null;
                v.timer = Math.min(v.timer, 40);
            }
        }
        int next = HELI_GO;
        if (v.state == FLY_IN) heliFlyIn(v, dt, aim);
        else if (v.state == DROP) heliDrop(v, dt, aim);
        else if (v.state == HOVER) heliHover(v, dt, aim);
        else if (v.state == STANDBY) heliStandby(v, dt, aim);
        else if (v.state == CIRCLE) heliCircle(v, dt, aim);
        else if (v.state == FLY_OUT) next = heliFlyOut(v, dt, aim);
        else next = heliLanding(v, dt, aim);
        if (next != HELI_GO) return next == HELI_GONE;
        flyTo(v, aim[0], aim[1], aim[2], aim[3], dt);
        return false;
    }

    /** On the pad with the rotor still, waiting for the crew to walk out and climb aboard. */
    boolean heliMuster(Vehicle v, float dt) {
        // On the pad with the rotor still, waiting for the crew to walk out and climb aboard.
        v.timer += dt;
        Entity pilot = livePilot(v);
        boolean pilotIn = pilot != null && v.crew.contains(pilot);
        if (pilot == null || (!pilotIn && v.timer > 60)) {
            landed(v);
            v.refuel = 5;
            return false;
        }
        if (pilotIn && (v.crew.size() >= v.crewWanted || v.timer > 30)) {
            // Anyone who didn't make it in time stays behind.
            for (int i = 0, n = w.entities.size(); i < n; i++) {
                Entity e = w.entities.get(i);
                if (!e.dead && e.rig == v && e.task == Dispatch.T_BOARD) {
                    e.task = Dispatch.T_NONE;
                    e.rig = null;
                }
            }
            // ((14.9) Everyone aboard but the pilot goes down: it used to keep two of the squad back as door
            // gunners, so a helicopter with seven aboard put only four down.)
            if (v.kind != K_POLICE_HELI && v.kind != K_RESCUE_HELI) v.troops = Math.max(0, v.crew.size() - 1);
            v.state = SPOOL;
            v.spoolTime = v.timer = 4.5f;
        }
        return false;
    }

    /** ((27.3) A step of updateHeli.) Flying in. Where it should fly to goes in aim: {x, y, speed, facing or NaN}. */
    void heliFlyIn(Vehicle v, float dt, float[] aim) {
        // A careful climb off the pad, then cruise, slowing for the approach.
        v.alt = Math.min(1, v.alt + dt * 0.15f);
        aim[0] = v.tx;
        aim[1] = v.ty;
        float d = (float) World.hypot(aim[0] - v.x, aim[1] - v.y);
        aim[2] = 15 + 95 * v.alt * v.alt;
        if (v.alt < 0.35f) {
            // (Straight up first: no skimming across the rooftops.)
            aim[0] = v.x;
            aim[1] = v.y;
        }
        if (v.kind == K_RESCUE_HELI && d < 150) {
            // ((11.0) Not over the dead: the nearest spot to them that's clear, for the winch.)
            float[] spot = fleet.routing.clearSpot(v.tx, v.ty, 260);
            if (spot != null) {
                v.tx = spot[0];
                v.ty = spot[1];
            }
            v.state = HOVER;
            v.timer = 0;
            w.dispatch.say(Dispatch.WHO_POLICE, null, "Rescue " + v.number + ": Over " + (v.place != null ? v.place : "the scene")
                    + (v.pickup ? ". Coming in for the pickup, winch going down." : ". Coming into the hover, team going down on the winch."), v.x, v.y);
        } else if (v.kind != K_POLICE_HELI && v.kind != K_RESCUE_HELI && v.troops > 0 && d < 220) {
            // Air assault: put the troops down first, clear of the dead.
            v.state = DROP;
            v.lzTries = 0;
            v.lzCheck = 1;
            float[] lz = landingZone(v.tx, v.ty, v.x, v.y);
            v.homeX = lz[0];
            v.homeY = lz[1];
        } else if (d < 170 && v.kind != K_RESCUE_HELI) {
            v.state = CIRCLE;
            v.circle = (float) Math.atan2(v.y - v.ty, v.x - v.tx);
            v.hold = 0;
            if (v.kind == K_GUNSHIP) {
                v.timer = 120;
                w.dispatch.say(Dispatch.WHO_MILITARY, null, "Gunship " + v.number + ": Over " + (v.place != null ? v.place : "the target")
                        + ". Cannon hot, engaging the pack.", v.x, v.y);
            } else if (v.kind == K_POLICE_HELI) {
                // ((31.0) Longer over the scene: two minutes and a half, not a minute and a half.)
                v.timer = 150;
                w.dispatch.say(Dispatch.WHO_POLICE, null, "Police helicopter: Over " + (v.place != null ? v.place : "the scene")
                        + ". Marksman at the door, spotlight on. We'll call them out for you.", v.x, v.y);
            } else {
                v.timer = v.escort != null ? 600 : 60;
                w.dispatch.say(Dispatch.WHO_MILITARY, null, "Air " + v.number + ": " + (v.escort != null ? "With the convoy, flying cover."
                        : "On station over " + (v.place != null ? v.place : "the target") + ". Door gunners clear to engage."), v.x, v.y);
            }
        }
    }

    /** ((27.3) A step of updateHeli.) Over the drop. Where it should fly to goes in aim: {x, y, speed, facing or NaN}. */
    void heliDrop(Vehicle v, float dt, float[] aim) {
        // Down low over the landing zone, steady, while the troops jump out.
        aim[0] = v.homeX;
        aim[1] = v.homeY;
        aim[2] = 45;
        float d = (float) World.hypot(aim[0] - v.x, aim[1] - v.y);
        // ((10.60) Watching the landing zone all the way down: the dead closing on it (the rotors draw them), and
        // it climbs away and picks another, rather than putting the squad down on top of them.)
        v.lzCheck -= dt;
        if (v.lzCheck <= 0) {
            v.lzCheck = 0.5f;
            if (w.countZombiesNear(aim[0], aim[1], 70) > 0 && v.lzTries < 4) {
                v.lzTries++;
                float[] lz = landingZone(v.tx, v.ty, v.x, v.y);
                v.homeX = lz[0];
                v.homeY = lz[1];
                aim[0] = v.homeX;
                aim[1] = v.homeY;
                d = (float) World.hypot(aim[0] - v.x, aim[1] - v.y);
                v.alt = Math.max(v.alt, 0.5f);
                if (v.lzTries == 1)
                    w.dispatch.say(Dispatch.WHO_MILITARY, null, "Air " + v.number + ": LZ's hot, waving off. Finding another.", v.x, v.y);
            }
        }
        if (d < 25) v.alt += (0.12f - v.alt) * Math.min(1, dt * 0.9f);
        if (d < 6 && v.alt < 0.2f && v.speed < 8 && (w.countZombiesNear(v.x, v.y, 45) == 0 || v.lzTries >= 4)) {
            unloadTroops(v);
            v.state = CIRCLE;
            v.circle = (float) Math.atan2(v.y - v.ty, v.x - v.tx);
            v.hold = 0;
            v.timer = 60;
        }
    }

    /** ((27.3) A step of updateHeli.) Holding over a spot. Where it should fly to goes in aim: {x, y, speed, facing or NaN}. */
    void heliHover(Vehicle v, float dt, float[] aim) {
        // Right over them, low, holding steady while the team works.
        aim[0] = v.tx;
        aim[1] = v.ty;
        aim[2] = 40;
        aim[3] = v.angle;
        v.alt += (0.5f - v.alt) * Math.min(1, dt * 0.8f);
        // ((11.0) The dead come to the spot before the team is down: shift to another, and nobody goes down the
        // line until it's clear.)
        boolean hot = !v.pickup && v.team == null && w.countZombiesNear(aim[0], aim[1], 55) > 0;
        if (hot) {
            v.lzCheck -= dt;
            if (v.lzCheck <= 0) {
                v.lzCheck = 2;
                float[] spot = fleet.routing.clearSpot(aim[0], aim[1], 260);
                if (spot != null) {
                    v.tx = spot[0];
                    v.ty = spot[1];
                }
            }
        }
        int step = !hot && World.hypot(aim[0] - v.x, aim[1] - v.y) < 8 && v.speed < 10 ? w.rescueStep(v, dt) : 0;
        if (step == 1) {
            // The team's down: back to base to wait for their call.
            v.state = FLY_OUT;
            v.standby = true;
            // ((16.5) Anyone aboard comes back to base with them: there's no hospital.)
            boolean lift = v.passengers > 0;
            v.homeX = v.baseX;
            v.homeY = v.baseY;
            w.dispatch.chatter(Dispatch.WHO_POLICE, null, "Rescue " + v.number + ": Team's on the ground. "
                    + (lift ? "Got the lookout off the tower, bringing them back to base." : "Heading back to base.")
                    + " Call us when you need us.", v.x, v.y);
        } else if (step == 2) {
            v.state = FLY_OUT;
            v.standby = false;
            v.homeX = v.baseX;
            v.homeY = v.baseY;
            w.dispatch.say(Dispatch.WHO_POLICE, null, "Rescue " + v.number + ": Everyone's aboard. "
                    + (v.passengers > 0 ? v.passengers + " survivor" + (v.passengers == 1 ? "" : "s") + ", heading back to base." : "Nobody left to bring out. Returning."),
                    v.x, v.y);
        }
    }

    /** ((27.3) A step of updateHeli.) Standing by. Where it should fly to goes in aim: {x, y, speed, facing or NaN}. */
    void heliStandby(Vehicle v, float dt, float[] aim) {
        // Waiting on the pad, rotor turning slowly, for the team to call.
        aim[0] = v.homeX;
        aim[1] = v.homeY;
        aim[2] = 12;
        v.alt = Math.max(0, v.alt - dt * 0.2f);
        if (w.rescueCalled(v)) {
            v.state = FLY_IN;
            v.pickup = true;
            v.standby = false;
        }
    }

    /** ((27.3) A step of updateHeli.) Circling its target. Where it should fly to goes in aim: {x, y, speed, facing or NaN}. */
    void heliCircle(Vehicle v, float dt, float[] aim) {
        v.timer -= dt;
        // Not a tight circle: it holds a hover off to one side with the door towards the fighting, then
        // shifts a sixth of the way round and holds again.
        v.hold -= dt;
        if (v.hold <= 0) {
            v.hold = 7 + w.rnd.nextFloat() * 4;
            v.circle += v.escort != null ? 0 : 1.05f;
        }
        float r = v.escort != null ? 70 : v.kind == K_POLICE_HELI ? 110 : v.kind == K_GUNSHIP ? 100 : 140;
        // ((31.0) A gunship follows its fight as it moves, and moves on to the next nearest when it's over.)
        if (v.kind == K_GUNSHIP && v.hold > 0 && (int) (v.timer * 0.5f) != (int) ((v.timer + dt) * 0.5f)) {
            Dispatch.Incident inc = v.incident;
            if (inc == null || inc.resolved) {
                inc = null;
                float bd = 1500 * 1500;
                for (int i = 0, n = w.dispatch.incidents.size(); i < n; i++) {
                    Dispatch.Incident q = w.dispatch.incidents.get(i);
                    if (q.resolved || q.zombiesNear < 3) continue;
                    float d = (q.x - v.x) * (q.x - v.x) + (q.y - v.y) * (q.y - v.y);
                    if (d < bd) {
                        bd = d;
                        inc = q;
                    }
                }
                v.incident = inc;
            }
            if (inc != null) {
                v.tx = inc.x;
                v.ty = inc.y;
                v.place = inc.place;
            }
        }
        aim[0] = v.tx + (float) Math.cos(v.circle) * r;
        aim[1] = v.ty + (float) Math.sin(v.circle) * r;
        aim[2] = v.escort != null ? 75 : 45;
        float d = (float) World.hypot(aim[0] - v.x, aim[1] - v.y);
        // Side-on to the target (the door gunner's side), sliding sideways from one hover to the next
        // rather than swinging the nose round each time.
        if (v.escort == null && d < 200) aim[3] = (float) Math.atan2(v.ty - v.y, v.tx - v.x) + (float) Math.PI / 2;
        v.alt += (0.75f - v.alt) * Math.min(1, dt);
        // Door gunners (the police helicopter's marksman takes careful single shots).
        v.gunCd -= dt;
        if (v.kind == K_GUNSHIP) {
            // ((31.0) The gunship's cannon: short bursts of shells that burst among a pack, never where the living are.)
            if (v.gunCd <= 0) {
                Entity z = w.nearestZombie(v.tx, v.ty, 220);
                if (z == null) z = w.nearestZombie(v.x, v.y, 260);
                if (z != null && !w.spatial.peopleNear(z.x, z.y, 24)) {
                    v.burst++;
                    v.gunCd = v.burst % 6 == 0 ? 1.1f : 0.16f;
                    w.combat.cannonBurst(v.x, v.y, z.x + w.rnd.nextFloat() * 10 - 5, z.y + w.rnd.nextFloat() * 10 - 5);
                } else v.gunCd = 0.4f;
            }
        } else if (v.kind == K_POLICE_HELI) {
            // ((14.8) Spotting from above for the units on the ground.)
            if (v.escort == null) w.heliSpot(v, dt);
            if (v.gunCd <= 0) {
                Entity z = w.nearestZombie(v.x, v.y, 220);
                v.gunCd = z != null ? 1.3f : 0.5f;
                if (z != null) w.marksmanShot(v.x, v.y, z);
            }
        } else if (v.gunCd <= 0) {
            Entity z = w.nearestZombie(v.x, v.y, 210);
            if (z != null) {
                v.burst++;
                v.gunCd = v.burst % 6 == 0 ? 1.4f : 0.13f;
                w.airShot(v.x, v.y, z);
            } else {
                v.gunCd = 0.5f;
            }
        }
        if (v.timer <= 0) {
            v.state = FLY_OUT;
            v.escort = null;
            v.homeX = v.baseX;
            v.homeY = v.baseY;
            if (v.kind == K_POLICE_HELI)
                w.dispatch.say(Dispatch.WHO_POLICE, null, "Police helicopter: Low on fuel, heading back to the pad.", v.x, v.y);
            else w.dispatch.say(Dispatch.WHO_MILITARY, null, "Air " + v.number + ": Winchester, returning to base to rearm.", v.x, v.y);
        }
    }

    /** ((27.3) A step of updateHeli.) On its way out. HELI_GO to fly on to where it now aims; HELI_STOP or HELI_GONE when this step is over (gone: off the map for good). */
    int heliFlyOut(Vehicle v, float dt, float[] aim) {
        v.alt += (1 - v.alt) * Math.min(1, dt);
        aim[0] = v.homeX;
        aim[1] = v.homeY;
        aim[2] = 110;
        float d = (float) World.hypot(aim[0] - v.x, aim[1] - v.y);
        // ((10.60) The dead round the pad, or the hospital doors: it holds overhead until they're dealt with,
        // rather than setting down and letting everyone out among them.)
        // ((24.0) Not for ever, though: with no more than a straggler or two about after a minute and a quarter it sets
        // down all the same. One idle zombie by the pad held a rescue flight and its passengers overhead for good.)
        if (d < 6 && v.speed < 12) v.holdT += dt;
        else v.holdT = 0;
        int round = d < 6 && v.speed < 12 ? w.countZombiesNear(aim[0], aim[1], 70) : 0;
        if (round > 0 && (v.holdT < 75 || round > 2)) {
            if (v.holdSaidCd <= 0) {
                v.holdSaidCd = 30;
                w.dispatch.say(v.kind == K_POLICE_HELI || v.kind == K_RESCUE_HELI ? Dispatch.WHO_POLICE : Dispatch.WHO_MILITARY, null,
                        (v.kind == K_RESCUE_HELI ? "Rescue " : v.kind == K_POLICE_HELI ? "Police helicopter " : "Air ") + v.number
                                + ": The dead are all round where we set down. Holding overhead till it's clear.", v.x, v.y);
            }
        } else if (d < 6 && v.speed < 12) {
            if (v.kind == K_RESCUE_HELI && v.passengers > 0) {
                // Set them down at the hospital, then home.
                w.dropRescued(v);
                v.homeX = v.baseX;
                v.homeY = v.baseY;
                return HELI_STOP;
            }
            if (v.padAt == null) return HELI_GONE;
            v.state = LAND;
        }
        return HELI_GO;
    }

    /** ((27.3) A step of updateHeli.) Any other state: coming down, and on the ground. HELI_GO to fly on to where it now aims; HELI_STOP or HELI_GONE when this step is over (gone: off the map for good). */
    int heliLanding(Vehicle v, float dt, float[] aim) {
        // Settle onto the pad.
        aim[0] = v.homeX;
        aim[1] = v.homeY;
        aim[2] = 8;
        aim[3] = v.angle;
        v.alt -= dt * 0.2f;
        // ((10.60) And back up if they come for it as it settles.)
        if (v.alt > 0.05f && w.countZombiesNear(aim[0], aim[1], 50) > (v.holdT >= 75 ? 2 : 0)) {
            v.state = FLY_OUT;
            v.alt += dt * 0.4f;
        } else if (v.alt <= 0) {
            if (v.padAt == null) return HELI_GONE;
            if (v.kind == K_RESCUE_HELI && v.standby) {
                v.alt = 0;
                v.state = STANDBY;
                return HELI_STOP;
            }
            landed(v);
            return HELI_STOP;
        }
        return HELI_GO;
    }

    /**
     * A helicopter moves any way it likes, not just nose first: it accelerates towards where it wants to be,
     * slows to arrive there, and can hang still over a spot. The nose follows the way it's going (or points
     * where it's told to while it hovers), and it banks into each change of course.
     */
    void flyTo(Vehicle v, float gx, float gy, float speed, float face, float dt) {
        speed *= World.VEHICLE_SPEED;
        float dx = gx - v.x, dy = gy - v.y, d = (float) Math.sqrt(dx * dx + dy * dy);
        float acc = 32;
        float want = d < 0.5f ? 0 : Math.min(speed, (float) Math.sqrt(2 * acc * 0.8f * d));
        float wvx = d < 0.5f ? 0 : dx / d * want, wvy = d < 0.5f ? 0 : dy / d * want;
        float ax = wvx - v.hvx, ay = wvy - v.hvy, al = (float) Math.sqrt(ax * ax + ay * ay), max = acc * dt;
        if (al > max) {
            ax *= max / al;
            ay *= max / al;
        }
        v.hvx += ax;
        v.hvy += ay;
        v.x += v.hvx * dt;
        v.y += v.hvy * dt;
        v.speed = (float) Math.sqrt(v.hvx * v.hvx + v.hvy * v.hvy);
        float heading = !Float.isNaN(face) ? face : v.speed > 12 ? (float) Math.atan2(v.hvy, v.hvx) : v.angle;
        float diff = heading - v.angle;
        while (diff > Math.PI) diff -= Math.PI * 2;
        while (diff < -Math.PI) diff += Math.PI * 2;
        v.angle += Math.max(-dt * 0.9f, Math.min(dt * 0.9f, diff));
        // Bank with the sideways push.
        float side = (-ax * (float) Math.sin(v.angle) + ay * (float) Math.cos(v.angle)) / Math.max(dt, 0.001f);
        v.bank += (Math.max(-1.2f, Math.min(1.2f, side * 0.03f)) - v.bank) * Math.min(1, dt * 3);
    }

    /** Somewhere open to put troops down near (x, y), a little way back from the nearest of the dead. */
    /**
     * (10.60) Where to put troops down for a fight at (x, y), coming in from (fx, fy): open ground (a street, a square,
     * a field) a good way off the fighting, with none of the dead near it and as few as can be further out,
     * on the near side if it can; with nowhere clear, the quietest spot found. It used to look only away
     * from the nearest one, check a few steps round the spot, and with nothing clear set down on the fight itself.
     */
    float[] landingZone(float x, float y, float fx, float fy) {
        float[] best = null, quiet = null;
        float bs = Float.MAX_VALUE, qs = Float.MAX_VALUE;
        float ax = fx - x, ay = fy - y, al = (float) World.hypot(ax, ay) + 0.01f;
        for (float r = 140; r <= 420; r += 40)
            for (int k = 0; k < 16; k++) {
                float a = k * (float) Math.PI / 8 + r * 0.01f;
                float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
                float[] p = city.findWalkable(x + ca * r, y + sa * r);
                if (p == null || city.circleBlocked(p[0], p[1], 10)) continue;
                int near = w.countZombiesNear(p[0], p[1], 90), around = w.countZombiesNear(p[0], p[1], 180);
                // (Nearer the fight is better, and on the side it's coming from; the dead about count most.)
                float score = r * 0.4f + around * 60 - (ca * ax + sa * ay) / al * 30;
                if (near == 0 && score < bs) {
                    bs = score;
                    best = p;
                }
                float qscore = near * 100 + around * 20 + r * 0.1f;
                if (qscore < qs) {
                    qs = qscore;
                    quiet = p;
                }
            }
        if (best != null) return best;
        if (quiet != null) return quiet;
        float[] p = city.findWalkable(fx, fy);
        return p != null ? p : new float[]{fx, fy};
    }

    /** The troops jump down: everyone aboard but the pilot (and a reinforcement's two door gunners), off to the fighting. */
    void unloadTroops(Vehicle v) {
        Entity pilot = livePilot(v);
        // ((14.9) Only reinforcements flown in from outside bring door gunners of their own, who stay aboard.)
        int keep = v.reinforcement ? 2 : 0;
        Entity first = null;
        for (int i = v.crew.size() - 1; i >= 0; i--) {
            Entity e = v.crew.get(i);
            if (e == pilot) continue;
            if (keep > 0) {
                keep--;
                continue;
            }
            v.crew.remove(i);
            float a = v.angle + (i % 2 == 0 ? 1.57f : -1.57f);
            if (!w.release(e, v.x + (float) Math.cos(a) * 12, v.y + (float) Math.sin(a) * 12)) continue;
            e.rig = null;
            if (v.incident != null && !v.incident.resolved) {
                e.task = Dispatch.T_RESPOND;
                e.incident = v.incident;
                e.onScene = false;
            } else {
                e.fear = 15;
                e.threatX = v.tx;
                e.threatY = v.ty;
            }
            if (first == null) first = e;
        }
        v.troops = 0;
        if (first != null && first.role == Entity.ROLE_SF)
            w.dispatch.say(Dispatch.WHO_MILITARY, first, "Viper team is on the ground at " + (v.place != null ? v.place : "the LZ") + ". Moving in quiet.", v.x, v.y, true);
        else if (first != null)
            w.dispatch.say(Dispatch.WHO_MILITARY, first, "Boots on the ground at " + (v.place != null ? v.place : "the LZ") + ". Moving up.", v.x, v.y);
    }
}
