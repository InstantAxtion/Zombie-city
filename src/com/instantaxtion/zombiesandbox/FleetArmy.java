package com.instantaxtion.zombiesandbox;

import static com.instantaxtion.zombiesandbox.Fleet.*;

import java.util.ArrayList;

/**
 * ((27.3) Part of the vehicle code, out of Fleet.) The army's vehicles: troop trucks and squads mounting up at the base, base patrols, drivers, the trucks kept on a base, supply, restock and ammo trucks, recon Humvees, the engineers' bulldozers, and tanks.
 */
final class FleetArmy {
    final Fleet fleet;
    final World w;
    final City city;

    FleetArmy(Fleet fleet) {
        this.fleet = fleet;
        this.w = fleet.w;
        this.city = fleet.city;
    }

    /**
     * (11.0) Army recon: a Humvee with two scouts that roams the whole town once it knows, radioing in every horde
     * it comes across as a call for the others, and drives on rather than stopping to fight (its roof gun keeps the
     * dead off it).
     */
    boolean startRecon(City.Facility base) {
        // ((12.9) The base's own scouts, standing by there, get in: none are made up for it.)
        java.util.List<Entity> scouts = standby(base, Entity.SOLDIER, Entity.ROLE_RECON, 2);
        if (scouts.isEmpty()) return false;
        Vehicle v = startReconAt(base.gateX > 0 ? base.gateX : base.x, base.gateY > 0 ? base.gateY : base.y, scouts);
        return v != null;
    }

    /** (11.1) A recon Humvee starting from the nearest road to (x, y) (placed with the Place tool too). */
    Vehicle startReconAt(float x, float y) {
        return startReconAt(x, y, null);
    }

    /** ...with these scouts aboard (two new ones when null: the Place tool's). */
    Vehicle startReconAt(float x, float y, java.util.List<Entity> scouts) {
        float[] g = city.nearestDrivable(x, y);
        if (g == null) return null;
        Vehicle v = fleet.make(TRUCK);
        fleet.setKind(v, K_HUMVEE);
        v.recon = true;
        v.x = v.homeX = g[0];
        v.y = v.homeY = g[1];
        v.number = ++fleet.reconNumber;
        v.state = DRIVE;
        if (!reconDestination(v)) return null;
        if (scouts != null) {
            for (Entity e : scouts) {
                e.task = Dispatch.T_NONE;
                e.dead = true;
                e.removed = true;
                e.rig = v; fleet.crews.note(e);
                v.crew.add(e);
            }
        } else for (int k = 0; k < 2; k++) {
            Entity e = w.create(Entity.SOLDIER, v.x, v.y);
            w.applyRole(e, Entity.ROLE_RECON);
            e.rig = v; fleet.crews.note(e);
            v.crew.add(e);
        }
        fleet.vehicles.add(v);
        return v;
    }

    boolean reconDestination(Vehicle v) {
        for (int k = 0; k < 6; k++) {
            float[] q = city.randomStreetSpot(w.rnd);
            float[] p = q != null ? city.nearestDrivable(q[0], q[1]) : null;
            if (p != null && World.hypot(p[0] - v.x, p[1] - v.y) > 400 && fleet.routing.route(v, p[0], p[1])) return true;
        }
        return false;
    }

    boolean reconStep(Vehicle v, float dt) {
        if (v.crew.isEmpty()) {
            v.parked = true;
            v.state = ABANDONED;
            return false;
        }
        fleet.turret(v, dt);
        v.stuckTimer += dt;
        boolean moving = fleet.driving.driveStep(v, dt, 100, fleet.driving.carAhead(v) ? 0.5f : 1f);
        if (moving) fleet.driving.hit(v, w.runOver(v.x, v.y, 8, v.speed, v.angle));
        if (!moving || World.hypot(v.tx - v.x, v.ty - v.y) < 40 || v.stuckTimer > 6) {
            v.speed = 0;
            v.stuckTimer = 0;
            if (!reconDestination(v)) v.stuckTimer = 3;
        }
        v.reconCd -= dt;
        if (v.reconCd <= 0) {
            v.reconCd = 1;
            int n = w.countZombiesNear(v.x, v.y, 190);
            if (n >= 4) {
                boolean covered = false;
                for (int i = 0; i < w.dispatch.incidents.size() && !covered; i++) {
                    Dispatch.Incident inc = w.dispatch.incidents.get(i);
                    if (!inc.resolved && World.hypot(inc.x - v.x, inc.y - v.y) < 260) covered = true;
                }
                if (!covered) {
                    Entity z = w.nearestZombie(v.x, v.y, 190);
                    if (z != null) w.dispatch.sighting(z.x, z.y, null);
                    if (w.time - v.reconSaid > 20) {
                        v.reconSaid = w.time;
                        w.dispatch.say(Dispatch.WHO_MILITARY, v.crew.get(0), "Recon " + v.number + ": " + (n >= 10 ? "Horde of about " + n / 5 * 5 : World.count(n, "hostile", "hostiles"))
                                + " on " + city.placeName(v.x, v.y) + ". Marked. Moving on.", v.x, v.y);
                    }
                }
            }
        }
        return false;
    }

    /** (13.9) Ammo trucks out now (not on their way home). */
    int ammoTrucksOut() {
        int n = 0;
        for (int i = 0, m = fleet.vehicles.size(); i < m; i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (v.ammoTruck && v.cargo > 0 && !v.broken && v.state != RETURN) n++;
        }
        return n;
    }

    /**
     * (13.9) An army truck standing by with spare rounds within reach of (x, y): stopped, not wrecked, not a supply
     * run to a safe zone. The nearest, or null.
     */
    Vehicle ammoTruckNear(float x, float y, float reach) {
        Vehicle best = null;
        float bd = reach;
        for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (v.type != TRUCK || v.cargo <= 0 || v.broken || v.burnt || v.removedFromFleet || v.speed > 3 || v.supply > 0) continue;
            float d = (float) World.hypot(v.x - x, v.y - y);
            if (d < bd) {
                bd = d;
                best = v;
            }
        }
        return best;
    }

    /** (13.9) An ammo truck already on its way to this call. */
    boolean ammoTruckFor(Dispatch.Incident inc) {
        for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (v.ammoTruck && v.incident == inc && !v.broken && v.cargo > 0) return true;
        }
        return false;
    }

    /** (13.9) An ammo truck on its way to a fight within reach of (x, y), to wait for; or null. */
    Vehicle ammoTruckComing(float x, float y, float reach) {
        for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (v.ammoTruck && v.cargo > 0 && !v.broken && !v.removedFromFleet && v.state != RETURN && World.hypot(v.tx - x, v.ty - y) < reach) return v;
        }
        return null;
    }

    /** (13.9) An army truck from the base with a load of ammunition for the soldiers at a fight, which waits there. */
    Vehicle sendAmmoTruck(City.Facility from, Dispatch.Incident inc) {
        int take = Math.min(AMMO_TRUCK_CARGO, from.ammo - AMMO_KEEP);
        if (take <= 0) return null;
        int before = fleet.vehicles.size();
        if (!fleet.service.send(Entity.SOLDIER, 0, from.x, from.y, inc.x, inc.y, inc, null, inc.place) || fleet.vehicles.size() == before) return null;
        Vehicle v = fleet.vehicles.get(fleet.vehicles.size() - 1);
        v.ammoTruck = true;
        from.ammo -= take;
        v.cargo = take;
        v.incident = inc;
        v.passengerType = Entity.SOLDIER;
        return v;
    }

    /** Each army base still standing has its bulldozer, parked in the yard (made again after a game is loaded). */
    void ensureDozers() {
        ensureDozers0();
    }

    void ensureDozers0() {
        for (City.Facility f : city.facilities) {
            if (f.kind != City.FACILITY_BASE || f.fallen) continue;
            boolean has = false;
            for (int i = 0; i < fleet.vehicles.size() && !has; i++) has = fleet.vehicles.get(i).kind == K_DOZER && fleet.vehicles.get(i).dozerBase == f;
            if (has) continue;
            float[] g = f.gateX > 0 ? new float[]{f.gateX, f.gateY} : city.nearestDrivable(f.x, f.y);
            if (g == null) continue;
            float[] p = bay(g, f);
            if (p == null) continue;
            Vehicle v = fleet.make(TRUCK);
            v.kind = K_DOZER;
            v.dozerBase = f;
            v.x = v.homeX = p[0];
            v.y = v.homeY = p[1];
            v.angle = (float) Math.atan2(f.y - p[1], f.x - p[0]);
            v.state = IDLE;
            v.parked = true;
            v.speed = 0;
            v.passengerType = Entity.SOLDIER;
            fleet.vehicles.add(v);
        }
    }

    /** A wreck or an abandoned vehicle left on a road in town, not already being seen to; the nearest to (x, y). */
    Vehicle wreckToClear(float x, float y, float reach) {
        Vehicle best = null;
        float bd = reach;
        for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
            Vehicle o = fleet.vehicles.get(i);
            if (o.removedFromFleet || o.kind == K_DOZER || o.player != null || o.standby || !o.riders.isEmpty()) continue;
            if (o.type != CAR && o.type != CRUISER && o.type != TRUCK && o.type != AMBULANCE) continue;
            if (!o.broken && !o.burnt && o.state != ABANDONED) continue;
            int t = city.tileIndex(o.x, o.y);
            if (t < 0 || city.tiles[t] != City.ROAD || !city.inTown(o.x, o.y)) continue;
            boolean taken = false;
            for (int k = 0; k < n && !taken; k++) taken = fleet.vehicles.get(k).clearing == o;
            if (taken) continue;
            float d = (float) World.hypot(o.x - x, o.y - y);
            if (d < bd) {
                bd = d;
                best = o;
            }
        }
        return best;
    }

    /** (14.0) Every few seconds: a bulldozer parked at its base goes out to the nearest wreck in the way, once the dead are about. */
    void dozers() {
        if (w.alert < 1) return;
        for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (v.kind != K_DOZER || !v.parked || v.broken || v.burnt) continue;
            if (v.dozerBase != null && v.dozerBase.fallen) continue;
            Vehicle o = wreckToClear(v.x, v.y, DOZER_REACH);
            if (o == null || !dozerTo(v, o)) continue;
            w.dispatch.say(Dispatch.WHO_MILITARY, null, "Military: Engineers are taking the bulldozer out to clear a wreck off "
                    + city.placeName(o.x, o.y) + ".", o.x, o.y);
        }
    }

    boolean dozerTo(Vehicle v, Vehicle o) {
        float[] p = city.nearestDrivable(o.x, o.y);
        if (p == null) return false;
        v.parked = false;
        v.state = DRIVE;
        v.stuckTimer = 0;
        v.clearing = o;
        if (!fleet.routing.route(v, p[0], p[1])) {
            v.clearing = null;
            v.parked = true;
            v.state = IDLE;
            return false;
        }
        v.place = city.placeName(o.x, o.y);
        return true;
    }

    /** At the end of a drive: at the wreck, push it; home, park. */
    boolean dozerArrived(Vehicle v) {
        v.speed = 0;
        Vehicle o = v.clearing;
        if (v.state == DRIVE && o != null && !o.removedFromFleet && World.hypot(o.x - v.x, o.y - v.y) < 40) {
            v.state = SCENE;
            v.timer = DOZER_PUSH;
            v.angle = (float) Math.atan2(o.y - v.y, o.x - v.x);
            return false;
        }
        if (v.state == RETURN) {
            v.state = IDLE;
            v.parked = true;
            v.clearing = null;
            return false;
        }
        return dozerNext(v);
    }

    /** Pushing the wreck: a few seconds, then it's off the road for good. */
    boolean dozerPush(Vehicle v, float dt) {
        Vehicle o = v.clearing;
        v.speed = 0;
        if (o == null || o.removedFromFleet) return dozerNext(v);
        float dx = o.x - v.x, dy = o.y - v.y, d = (float) Math.sqrt(dx * dx + dy * dy);
        v.angle = (float) Math.atan2(dy, dx);
        if (d > 20) {
            // (Up to it, slowly, over whatever's in the way.)
            float step = Math.min(d - 18, 18 * dt);
            v.x += dx / d * step;
            v.y += dy / d * step;
            return false;
        }
        v.timer -= dt;
        // (Shoving it along, a little at a time.)
        float a = v.angle;
        o.x += (float) Math.cos(a) * 4 * dt;
        o.y += (float) Math.sin(a) * 4 * dt;
        if (v.soundCd <= 0) {
            v.soundCd = 1.2f;
            w.emit(Sfx.CRASH, o.x, o.y);
        }
        if (v.timer > 0) return false;
        fleet.crews.discard(o);
        fleet.dozerCleared++;
        return dozerNext(v);
    }

    /** On to the next wreck near here, or back to base. */
    boolean dozerNext(Vehicle v) {
        v.clearing = null;
        Vehicle o = w.alert >= 1 ? wreckToClear(v.x, v.y, DOZER_REACH / 2) : null;
        if (o != null && dozerTo(v, o)) return false;
        v.state = RETURN;
        v.stuckTimer = 0;
        if (!fleet.routing.route(v, v.homeX, v.homeY)) {
            // (No way home from here: it waits where it is.)
            v.state = IDLE;
            v.parked = true;
        }
        return false;
    }

    /** An army truck carrying ammunition from an armoury to a safe zone. */
    boolean sendSupply(City.Facility from, Dispatch.SafeZone z, int rounds) {
        int before = fleet.vehicles.size();
        if (!fleet.service.send(Entity.SOLDIER, 0, from.x, from.y, z.x, z.y, null, z, z.place) || fleet.vehicles.size() == before) return false;
        Vehicle v = fleet.vehicles.get(fleet.vehicles.size() - 1);
        v.supply = rounds;
        return true;
    }

    /**
     * After the outbreak, a supply truck from outside: in from the edge of the map at (fromX, fromY) to a store
     * or an armoury, unloads, and drives back out the way it came.
     */
    boolean sendRestock(float fromX, float fromY, City.Building b, City.Facility f) {
        int before = fleet.vehicles.size();
        fleet.fromParked = false;
        float tx = b != null ? b.doorX : f.x, ty = b != null ? b.doorY : f.y;
        boolean sent = fleet.service.send(Entity.SOLDIER, 0, fromX, fromY, tx, ty, null, null, b != null ? b.name : f.name);
        fleet.fromParked = true;
        if (!sent || fleet.vehicles.size() == before) return false;
        Vehicle v = fleet.vehicles.get(fleet.vehicles.size() - 1);
        v.restock = true;
        v.restockShop = b;
        v.restockFac = f;
        v.reinforcement = true;
        v.angle = (float) Math.atan2(ty - v.y, tx - v.x);
        return true;
    }

    /** A tank rolls out of the base towards heavy fighting. */
    boolean sendTank(float fromX, float fromY, float toX, float toY, String place) {
        float[] start = city.nearestDrivable(fromX, fromY);
        if (start == null) return false;
        Vehicle v = fleet.make(TANK);
        v.x = start[0];
        v.y = start[1];
        if (!fleet.routing.route(v, toX, toY)) return false;
        v.state = WAIT;
        v.timer = 2;
        v.homeX = start[0];
        v.homeY = start[1];
        v.place = place;
        v.angle = v.turret = (float) Math.atan2(toY - v.y, toX - v.x);
        fleet.vehicles.add(v);
        w.tanksDeployed++;
        return true;
    }
    Vehicle placeTank(float x, float y) {
        float[] p = city.nearestDrivable(x, y);
        if (p == null) return null;
        Vehicle v = fleet.make(TANK);
        v.x = p[0];
        v.y = p[1];
        v.homeX = p[0];
        v.homeY = p[1];
        v.state = ENGAGE;
        v.timer = 240;
        v.place = city.placeName(p[0], p[1]);
        v.angle = v.turret = w.rnd.nextInt(4) * (float) Math.PI / 2;
        fleet.vehicles.add(v);
        w.tanksDeployed++;
        return v;
    }

    /**
     * (10.26) Soldiers go by road whenever they can: a squad sent to a call more than a short walk away first
     * climbs into the nearest army truck parked near it (back at a base after an earlier run, or in a base's
     * bays) and drives there. False if the call is close or there's no truck near them (they go on foot).
     */
    boolean mountUp(java.util.List<Entity> squad, Dispatch.Incident inc) {
        if (squad.isEmpty() || inc == null) return false;
        float sx = 0, sy = 0;
        for (Entity e : squad) {
            sx += e.x;
            sy += e.y;
        }
        sx /= squad.size();
        sy /= squad.size();
        if (World.hypot(inc.x - sx, inc.y - sy) < 350) return false;
        float[] p = fleet.service.takeStandby(TRUCK, sx, sy, 600);
        if (p == null) p = city.takeParkedTruck(sx, sy, 600);
        if (p == null) return false;
        int kind = city.cfg.nature() ? (inc.zombiesNear >= 18 ? K_IFV : inc.zombiesNear >= 12 && fleet.routing.onRoad(inc.x, inc.y) ? K_GUNTRUCK
                : inc.zombiesNear >= 8 && mrapFree(null) ? K_MRAP : inc.zombiesNear >= 10 ? K_APC : squad.size() <= HUMVEE_SEATS ? K_HUMVEE : 0) : 0;
        musterTruck(squad, p, inc, kind);
        return true;
    }

    /**
     * (10.45) Where a vehicle should wait for its people at a station or base: in the yard, off the gateway (so
     * the ones coming and going can get by), as near the gate as that allows. Else just a clear spot near p.
     */
    float[] bay(float[] p, City.Facility f) {
        if (p == null) return null;
        if (f == null || f.gateX <= 0) return fleet.routing.clearSpot(p);
        float keep = f.kind == City.FACILITY_BASE ? City.T * 3.5f : City.T * 2.2f;
        int tx = (int) (f.x / City.T), ty = (int) (f.y / City.T), rr = (int) (f.r / City.T) + 2;
        float best = Float.MAX_VALUE;
        float[] out = null;
        for (int y = ty - rr; y <= ty + rr; y++)
            for (int x = tx - rr; x <= tx + rr; x++) {
                if (x < 0 || y < 0 || x >= city.w || y >= city.h) continue;
                byte t = city.tiles[y * city.w + x];
                if (t != City.LOT && t != City.BASE) continue;
                float px = (x + 0.5f) * City.T, py = (y + 0.5f) * City.T;
                float g = (float) World.hypot(px - f.gateX, py - f.gateY);
                if (g < keep || g >= best || World.hypot(px - f.x, py - f.y) > f.r + City.T) continue;
                if (city.solidAt(px, py) || occupied(px, py)) continue;
                best = g;
                out = new float[]{px, py};
            }
        return out != null ? out : fleet.routing.clearSpot(p);
    }

    /** The station or base whose yard (x, y) is in, if any. */
    City.Facility yardOf(float x, float y) {
        for (City.Facility f : city.facilities)
            if ((f.kind == City.FACILITY_BASE || f.kind == City.FACILITY_POLICE)
                    && (World.hypot(x - f.x, y - f.y) < f.r + City.T * 4 || (f.gateX > 0 && World.hypot(x - f.gateX, y - f.gateY) < City.T * 5))) return f;
        return null;
    }

    boolean occupied(float x, float y) {
        for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
            Vehicle o = fleet.vehicles.get(i);
            if (offRoad(o)) continue;
            if (Math.abs(o.x - x) < 13 && Math.abs(o.y - y) < 13) return true;
        }
        return false;
    }

    /** A truck at p waiting for this squad to climb in, then off to the call. */
    Vehicle musterTruck(java.util.List<Entity> squad, float[] p, Dispatch.Incident inc, int kind) {
        return musterTruck(squad, p, inc, kind, true);
    }

    /**
     * ((28.5) As above. Where p is where a vehicle really stood (taken from where it was parked - see takeStandby and
     * City.takeParkedTruck), the new one starts right there, and, keepKind, as what was parked there: it used to vanish
     * and a truck of whatever kind the call wanted turn up in a bay by the gate.)
     */
    Vehicle musterTruck(java.util.List<Entity> squad, float[] p, Dispatch.Incident inc, int kind, boolean keepKind) {
        boolean stood = p.length > 2;
        if (keepKind && p.length > 3) kind = (int) p[3];
        if (!stood) p = bay(p, yardOf(p[0], p[1]));
        Vehicle v = fleet.make(TRUCK);
        v.x = p[0];
        v.y = p[1];
        v.homeX = p[0];
        v.homeY = p[1];
        if (kind != 0) fleet.setKind(v, kind);
        v.angle = (float) Math.atan2(inc.y - p[1], inc.x - p[0]);
        v.state = MUSTER;
        v.crewWanted = squad.size();
        v.incident = inc;
        v.tx = inc.x;
        v.ty = inc.y;
        v.place = inc.place;
        v.passengerType = Entity.SOLDIER;
        for (Entity e : squad) {
            e.rig = v; fleet.crews.note(e);
            e.task = Dispatch.T_BOARD;
        }
        fleet.vehicles.add(v);
        return v;
    }

    /**
     * (12.9) A special unit standing by at its station or base (see World.stationUnits): those of this kind and role
     * on their posts there (the SWAT team's marksman with it), up to max.
     */
    java.util.List<Entity> standby(City.Facility f, int type, int role, int max) {
        java.util.List<Entity> out = new ArrayList<Entity>();
        for (int i = 0, n = w.entities.size(); i < n && out.size() < max; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || e.type != type || e.task != Dispatch.T_POST || e == w.controlled) continue;
            if (e.role != role && !(role == Entity.ROLE_SWAT && e.role == Entity.ROLE_MARKSMAN)) continue;
            if (World.hypot(e.x - f.x, e.y - f.y) > f.r + 140) continue;
            // ((27.4) Not one on another job (a post's guard, a crew of its own) that happens to be standing here.)
            if (e.owner != null && e.owner.priority >= Jobs.P_CREW && !w.ops.jobs.isFree(e, Jobs.P_CREW)) continue;
            out.add(e);
        }
        return out;
    }

    /** (12.9) The unit's own officers climb into their vehicle at the gate and it drives them out. */
    Vehicle musterUnit(java.util.List<Entity> squad, City.Facility f, Dispatch.Incident inc, int kind, int type) {
        float[] g = city.nearestDrivable(f.gateX > 0 ? f.gateX : f.x, f.gateY > 0 ? f.gateY : f.y);
        if (g == null || squad.isEmpty()) return null;
        Vehicle v = musterTruck(squad, g, inc, kind);
        v.passengerType = type;
        return v;
    }

    /**
     * A Humvee patrol from the base: the squad mounts up, drives out to (tx, ty) and back, its gunner firing on
     * any of the dead it passes. They stay aboard unless the Humvee is wrecked.
     */
    boolean basePatrol(java.util.List<Entity> squad, City.Facility base, float tx, float ty) {
        // ((11.7) One of the trucks parked on the base, or no patrol: none appear from nowhere.)
        float[] p = city.takeParkedTruck(base.x, base.y, Math.max(260, base.r * 2.5f));
        if (p == null || squad.isEmpty()) return false;
        p = bay(p, base);
        Vehicle v = fleet.make(TRUCK);
        fleet.setKind(v, K_HUMVEE);
        v.loop = true;
        v.x = p[0];
        v.y = p[1];
        v.homeX = p[0];
        v.homeY = p[1];
        v.angle = (float) Math.atan2(ty - p[1], tx - p[0]);
        v.state = MUSTER;
        v.crewWanted = squad.size();
        v.tx = tx;
        v.ty = ty;
        v.place = city.placeName(tx, ty);
        v.passengerType = Entity.SOLDIER;
        for (Entity e : squad) {
            e.rig = v; fleet.crews.note(e);
            e.task = Dispatch.T_BOARD;
        }
        fleet.vehicles.add(v);
        return true;
    }

    /** Patrols out from this base now. */
    int basePatrols(City.Facility base) {
        int n = 0;
        for (int i = 0, m = fleet.vehicles.size(); i < m; i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (v.loop && !v.broken && World.hypot(v.homeX - base.x, v.homeY - base.y) < Math.max(300, base.r * 3)) n++;
        }
        return n;
    }

    /** Soldiers mustering for, or riding to, this call in trucks. */
    int inboundSoldiers(Dispatch.Incident inc) {
        int n = 0;
        for (int i = 0, m = fleet.vehicles.size(); i < m; i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (v.type == TRUCK && v.incident == inc && (v.state == MUSTER || v.state == DRIVE)) n += Math.max(v.crew.size(), v.crewWanted);
            else if (v.pickupFor == inc && v.passengerType == Entity.SOLDIER) n += v.squad.size();
        }
        return n;
    }

    /**
     * (11.7) Someone free at a station or base to drive one of its vehicles out: an officer or soldier with nothing
     * to do, inside the grounds, not one of those it's going for. Null if there's nobody.
     */
    Entity freeDriver(City.Facility f, int type, java.util.List<Entity> not) {
        if (f == null) return null;
        Entity best = null;
        float bd = f.r + 200;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || e.type != type || e.task != Dispatch.T_NONE || e.rig != null || e.ride != null || e.aiming || e == w.controlled) continue;
            if (e.role == Entity.ROLE_PILOT || e.role == Entity.ROLE_SAR || e.role == Entity.ROLE_COMMANDER || e.role == Entity.ROLE_MARKSMAN) continue;
            if (not != null && not.contains(e)) continue;
            float d = (float) World.hypot(e.x - f.x, e.y - f.y);
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        return best;
    }

    /** (11.7) Puts someone at the wheel: they're off the street, in the vehicle, until it's parked home again. */
    void seatDriver(Vehicle v, Entity d) {
        if (d == null) return;
        d.task = Dispatch.T_NONE;
        d.rig = v; fleet.crews.note(d);
        d.dead = true;
        d.removed = true;
        d.vx = d.vy = 0;
        v.driver = d;
        v.realDriver = true;
    }

    /**
     * (11.7) Each police station's spare cars, parked on its lot from the start (besides those out on patrol): one
     * for every two of the officers based there, two at the least, eight at the most. Calls and new patrols take
     * these; when they're all out, officers go on foot until one comes back.
     */
    /**
     * (12.3) Each army base's motor pool, made up to strength when the game starts (and when one is loaded): a truck for
     * every four of the soldiers based there, four at the least and 40 at the most, so a squad sent anywhere drives
     * there. The trucks parked in the yard count towards it; the rest are in its garages.
     */
    /**
     * (12.3) An army truck that isn't needed after all, still at its bay: parked in the bay again if it's free (true:
     * the truck is a parked one on the map now), else left parked where it stands, standing by.
     */
    boolean backInPool(Vehicle v) {
        v.incident = null;
        v.speed = 0;
        // ((28.7) A quick reaction force's van nobody got into: back on standby in the yard, a van.)
        if (v.kind == K_RAPID_VAN) {
            fleet.service.parkUp(v);
            return false;
        }
        if (city.tiles[city.tileIndex(v.homeX, v.homeY)] == City.BASE && !w.peopleNear(v.homeX, v.homeY, 10)) {
            city.parkTruck(v.homeX, v.homeY);
            return true;
        }
        City.Facility f = yardOf(v.x, v.y);
        if (f != null && f.kind == City.FACILITY_BASE) {
            f.spareTrucks++;
            return true;
        }
        v.state = IDLE;
        v.parked = true;
        v.standby = true;
        return false;
    }

    /**
     * (12.3) Where one of the base's trucks starts from: one parked in the yard, else one out of its garages (by the
     * gate), else null: they're all out.
     */
    float[] baseTruck(City.Facility f) {
        // ((16.6) One standing ready in the yard first.)
        float[] p = fleet.service.takeStandby(TRUCK, f.x, f.y, Math.max(260, f.r * 2.5f));
        if (p != null) return p;
        p = city.takeParkedTruck(f.x, f.y, Math.max(260, f.r * 2.5f));
        if (p != null || f.spareTrucks <= 0 || f.fallen) return p;
        float[] g = f.gateX > 0 ? new float[]{f.gateX, f.gateY} : city.nearestDrivable(f.x, f.y);
        if (g == null) return null;
        f.spareTrucks--;
        return bay(g, f);
    }

    /** ((27.5) MRAPs each base has (out or in), and whether base f (null: any) still has one to send. */
    static final int MRAPS_PER_BASE = 3;

    boolean mrapFree(City.Facility f) {
        int out = 0, bases = 0;
        for (City.Facility b : city.facilities) if (b.kind == City.FACILITY_BASE && !b.fallen) bases++;
        for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (v.kind != K_MRAP || v.broken) continue;
            if (f == null || World.hypot(v.homeX - f.x, v.homeY - f.y) < Math.max(260, f.r * 2.5f)) out++;
        }
        return out < (f == null ? bases * MRAPS_PER_BASE : MRAPS_PER_BASE);
    }

    void stockBases() {
        for (City.Facility f : city.facilities) {
            if (f.kind != City.FACILITY_BASE || f.fallen) continue;
            float rad = Math.max(260, f.r * 2.5f);
            int soldiers = 0;
            for (int i = 0, n = w.entities.size(); i < n; i++) {
                Entity e = w.entities.get(i);
                if (!e.dead && e.type == Entity.SOLDIER && World.hypot(e.x - f.x, e.y - f.y) < rad) soldiers++;
            }
            if (soldiers == 0) continue;
            // ((28.7) The quick reaction force's base: its four vans in the yard, and nothing else.)
            if (f.qrf) {
                stockRapidVans(f);
                continue;
            }
            int have = city.parkedTrucks(f.x, f.y, rad);
            for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
                Vehicle v = fleet.vehicles.get(i);
                if (v.type == TRUCK && v.standby && v.kind != K_RAPID_VAN && World.hypot(v.x - f.x, v.y - f.y) < rad) have++;
            }
            int want = Math.max(4, Math.min(40, (soldiers + 3) / 4));
            f.spareTrucks = Math.max(0, want - have);
            // ((16.6) A few of them out in the yard where they can be seen, parked and ready, when the base's layout
            // left no room for any along its bottom row - an airfield or a vehicle park across it. The rest were all
            // in the garages, and the base looked as if it had no vehicles at all.)
            int show = Math.min(want, SHOWN_TRUCKS) - have;
            float[] g = f.gateX > 0 ? new float[]{f.gateX, f.gateY} : city.nearestDrivable(f.x, f.y);
            for (int k = 0; k < show && f.spareTrucks > 0 && g != null; k++) {
                float[] p = bay(g, f);
                if (p == null || World.hypot(p[0] - f.x, p[1] - f.y) > rad) break;
                Vehicle v = fleet.make(TRUCK);
                v.x = p[0];
                v.y = p[1];
                v.homeX = p[0];
                v.homeY = p[1];
                v.angle = (float) Math.atan2(f.y - p[1], f.x - p[0]);
                v.depot = f;
                v.state = IDLE;
                v.speed = 0;
                v.parked = true;
                v.standby = true;
                fleet.vehicles.add(v);
                f.spareTrucks--;
            }
        }
    }

    /** ((28.7) The quick reaction force's four vans, made up in its yard (counting those already there or out). */
    static final int RAPID_VANS = 4;

    void stockRapidVans(City.Facility f) {
        f.spareTrucks = 0;
        int have = 0;
        for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (v.kind == K_RAPID_VAN && v.depot == f && !v.broken) have++;
        }
        float[] g = f.gateX > 0 ? new float[]{f.gateX, f.gateY} : city.nearestDrivable(f.x, f.y);
        for (int k = have; k < RAPID_VANS && g != null; k++) {
            float[] p = bay(g, f);
            if (p == null) break;
            Vehicle v = fleet.make(TRUCK);
            fleet.setKind(v, K_RAPID_VAN);
            v.x = p[0];
            v.y = p[1];
            v.homeX = p[0];
            v.homeY = p[1];
            v.angle = (float) Math.atan2(f.y - p[1], f.x - p[0]);
            v.depot = f;
            v.passengerType = Entity.SOLDIER;
            v.state = IDLE;
            v.speed = 0;
            v.parked = true;
            v.standby = true;
            fleet.vehicles.add(v);
        }
    }

    // ------------------------------------------------------------------ tanks and ambulances

    boolean updateTank(Vehicle v, float dt) {
        v.cannonCd -= dt;
        v.gunCd -= dt;
        if (v.state == WAIT) {
            v.timer -= dt;
            if (v.timer <= 0) v.state = DRIVE;
            return false;
        }
        // The turret tracks the nearest zombie; the cannon fires at crowds, the coax gun at stragglers.
        // Only what the crew can see: no shooting through buildings.
        Entity z = w.nearestVisibleZombie(v.x, v.y, 260);
        float aim = z != null ? (float) Math.atan2(z.y - v.y, z.x - v.x) : v.angle;
        v.turret = turnTo(v.turret, aim, dt * 1.8f);
        if (z != null) {
            float off = Math.abs(aim - v.turret);
            if (v.cannonCd <= 0 && off < 0.15f && (w.countZombiesNear(z.x, z.y, 40) >= 3 || z.type == Entity.GIANT) && !w.peopleNear(z.x, z.y, 55)
                    && World.hypot(z.x - v.x, z.y - v.y) > 60) {
                v.cannonCd = 3.5f;
                w.tankShell(v.x + (float) Math.cos(v.turret) * 14, v.y + (float) Math.sin(v.turret) * 14, z.x, z.y);
            } else if (v.gunCd <= 0 && World.hypot(z.x - v.x, z.y - v.y) < 160) {
                v.gunCd = 0.15f;
                w.airShot(v.x, v.y, z);
            }
        }
        if (v.soundCd <= 0 && v.state != ENGAGE) {
            v.soundCd = 1.1f;
            w.emit(Sfx.ENGINE, v.x, v.y);
        }
        if (v.state == DRIVE) {
            v.stuckTimer += dt;
            boolean moving = fleet.driving.driveStep(v, dt, 45, 1);
            if (moving) w.runOver(v.x, v.y, 10, Math.max(v.speed, 25), v.angle);
            // (A tank coming in from outside waits out a jam on the way in.)
            if (!moving || v.stuckTimer > (v.reinforcement ? 20 : 5)) {
                v.state = ENGAGE;
                v.timer = 70;
                v.speed = 0;
            }
            return false;
        }
        if (v.state == ENGAGE) {
            v.speed = 0;
            v.timer -= dt;
            v.idleTimer = z == null ? v.idleTimer + dt : 0;
            if (v.timer <= 0 || v.idleTimer > 15) {
                v.state = RETURN;
                w.dispatch.say(Dispatch.WHO_MILITARY, null, "Military: Armor pulling back to base.", v.x, v.y);
                return fleet.service.routeHomeOrPark(v);
            }
            return false;
        }
        v.stuckTimer += dt;
        boolean moving = fleet.driving.driveStep(v, dt, 45, 1);
        if (moving) w.runOver(v.x, v.y, 10, Math.max(v.speed, 25), v.angle);
        // ((24.2) A tank stuck on its way back was taken off the map after eight seconds, in front of you. In view and
        // still a way off, it tries another way first.)
        if ((!moving || v.stuckTimer > 8) && fleet.routing.inView(v.x, v.y) && World.hypot(v.x - v.homeX, v.y - v.homeY) > 160 && v.jamTries < 8) {
            if (v.rerouteCd <= 0) {
                v.jamTries++;
                v.rerouteCd = 4;
                v.stuckTimer = 0;
                fleet.routing.route(v, v.homeX, v.homeY);
            }
            return false;
        }
        return !moving || v.stuckTimer > 8;
    }
}
