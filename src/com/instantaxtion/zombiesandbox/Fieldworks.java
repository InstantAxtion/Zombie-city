package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Defences and tools on the ground: lures, flares, mines, wire, machine-gun nests, mortars, engineers' works and the police drones.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class Fieldworks {
    private final World w;

    Fieldworks(World w) {
        this.w = w;
    }

    void toolsStep(float dt) {
        if (w.flares.isEmpty() && w.mines.isEmpty()) return;
        w.toolTick -= dt;
        boolean tick = w.toolTick <= 0;
        if (tick) w.toolTick = 1;
        for (int i = w.flares.size() - 1; i >= 0; i--) {
            float[] f = w.flares.get(i);
            f[2] -= dt;
            if (f[2] <= 0) {
                w.flares.remove(i);
                continue;
            }
            // The dead are drawn to it.
            if (tick) lure(f[0], f[1], World.FLARE_LURE);
        }
        for (int i = w.mines.size() - 1; i >= 0; i--) {
            float[] m = w.mines.get(i);
            if (m[2] > 0) {
                m[2] -= dt;
                continue;
            }
            if (!tick && (w.lodFrame + i) % 3 != 0) continue;
            if (w.spatial.zombieAt(m[0], m[1], World.MINE_TRIGGER)) {
                w.mines.remove(i);
                w.minesBlown++;
                w.combat.explode(m[0], m[1], 42, 260);
            }
        }
    }

    /** The dead within r turn towards a spot, as for a noise, without anyone hearing a thing. */
    void lure(float x, float y, float r) {
        int cx0 = Math.max(0, (int) ((x - r) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((x + r) / World.CELL));
        int cy0 = Math.max(0, (int) ((y - r) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((y + r) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c], end = w.cellStart[c] + w.zCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o.dead || o.hp <= 0 || !o.isZombie() || o.memory > 0) continue;
                    float dx = o.x - x, dy = o.y - y;
                    if (dx * dx + dy * dy > r * r) continue;
                    o.noiseX = x + w.rnd.nextFloat() * 50 - 25;
                    o.noiseY = y + w.rnd.nextFloat() * 50 - 25;
                    o.noiseTimer = 6;
                }
            }
    }

    /** A flare burning on the ground for most of a minute: the dead for a long way round are drawn to it. */
    void flare(float x, float y) {
        if (w.flares.size() >= 12) w.flares.remove(0);
        w.flares.add(new float[]{x, y, World.FLARE_LIFE});
        w.flaresFired++;
        w.emit(Sfx.FLARE, x, y);
        w.say("Flare burning at " + w.city.placeName(x, y));
    }

    /** A handful of mines laid round a spot: armed after a few seconds, set off by the dead only. Returns how many. */
    int layMines(float x, float y) {
        int laid = 0;
        for (int k = 0; k < 6 && w.mines.size() < 120; k++) {
            double a = k * Math.PI / 3 + w.rnd.nextFloat() * 0.5f;
            float r = k == 0 ? 0 : 16 + w.rnd.nextFloat() * 8;
            float mx = x + (float) Math.cos(a) * r, my = y + (float) Math.sin(a) * r;
            if (w.city.solidAt(mx, my)) continue;
            w.mines.add(new float[]{mx, my, World.MINE_ARM});
            laid++;
        }
        if (laid > 0) w.say(laid + " mines laid at " + w.city.placeName(x, y));
        return laid;
    }

    /** A coil of razor wire on the tile at (x, y); null if it can't go there. */
    float[] layWire(float x, float y) {
        return w.defences.layWire(w, x, y);
    }

    void wireGone(float x, float y) {
        for (int k = 0; k < 6; k++) w.effects.particle(x, y, w.rnd.nextFloat() * 40 - 20, w.rnd.nextFloat() * 40 - 20, 0.5f, 1f, 0xFFB8BCC2, World.P_DEBRIS);
    }

    /** A machine-gun nest at (x, y): its crew, or an empty list if there's no room. */
    ArrayList<Entity> mgNest(float x, float y) {
        ArrayList<Entity> crew = w.defences.buildNest(w, x, y);
        if (!crew.isEmpty())
            w.dispatch.say(Dispatch.WHO_MILITARY, crew.get(0), "Gun's up at " + w.city.placeName(x, y) + ". We hold here.", x, y);
        return crew;
    }

    /** A mortar barrage: eight shells walked across the spot over a few seconds. */
    void barrage(float x, float y) {
        w.barrages++;
        w.combat.shells(x, y, 0);
        w.dispatch.say(Dispatch.WHO_MILITARY, null, "Military: Fire mission on " + w.city.placeName(x, y) + ". Rounds on the way - keep clear!", x, y);
    }

    /**
     * ((27.0) A command post's fire mission: the mortars at the army's base shell a big pack of the dead the post has
     * spotted - 25 or more together within reach of it, nobody living within 120 of them, not in a safe zone or a
     * survivors' base. The spot {x, y, how many}, or null with no base standing with soldiers at it, or nothing to shell.
     */
    float[] fireMission(float px, float py, float reach, String caller, int who) {
        City.Facility base = w.city.mainBase();
        if (base == null || base.fallen || w.zombies < 25) return null;
        boolean manned = false;
        float rad = Math.max(260, base.r * 2.5f);
        for (int i = 0, n = w.entities.size(); i < n && !manned; i++) {
            Entity e = w.entities.get(i);
            manned = !e.dead && e.type == Entity.SOLDIER && Math.abs(e.x - base.x) < rad && Math.abs(e.y - base.y) < rad;
        }
        if (!manned) return null;
        float[] pk = safePack(px, py, reach, 25, 120, 0);
        if (pk == null) return null;
        float bx = pk[0], by = pk[1];
        int best = (int) pk[2];
        w.combat.shells(bx, by, World.FIRE_DELAY);
        w.fireMissions++;
        String place = w.city.placeName(bx, by);
        w.dispatch.say(who, null, caller + ": Fire mission, " + best + " of the dead in the open on " + place + ". Requesting mortars.", px, py, true);
        w.dispatch.say(Dispatch.WHO_MILITARY, null, base.name + ": Fire mission on " + place + ". Rounds on the way - keep clear!", bx, by);
        w.stats.timeline("A command post called mortars down on " + place, bx, by);
        return new float[]{bx, by, best};
    }

    /**
     * ((27.5) Out of fireMission.) The biggest pack of the dead (at least min, counted within 80) within reach of
     * (px, py) and further than near from it, with nobody armed or not turned within clear of it and not in a safe
     * zone or a survivors' base: {x, y, count}, or null.
     */
    float[] safePack(float px, float py, float reach, int min, float clear, float near) {
        int best = min - 1;
        float bx = 0, by = 0;
        for (int i = 0, n = w.entities.size(), seen = 0; i < n; i++) {
            Entity z = w.entities.get(i);
            if (z.dead || !z.isZombie()) continue;
            // (Every fourth of them: a pack of 25 has a few looked at.)
            if ((seen++ & 3) != 0) continue;
            float d = (float) World.hypot(z.x - px, z.y - py);
            if (d > reach || d < near) continue;
            int c = w.spatial.countZombiesNear(z.x, z.y, 80);
            if (c <= best) continue;
            if (w.spatial.armedNear(z.x, z.y, clear) > 0 || w.spatial.countCiviliansNear(z.x, z.y, clear) > 0) continue;
            // ((27.5) A safe zone's compound and half as much again round it: the margin is a scale (it was 60, which
            // put most of the city off limits whenever a zone was open).)
            boolean kept = false;
            for (int k = 0; k < w.dispatch.zones.size() && !kept; k++) kept = !w.dispatch.zones.get(k).removed && w.dispatch.zones.get(k).contains(z.x, z.y, 1.5f);
            for (int k = 0; k < w.bases.size() && !kept; k++) kept = w.bases.get(k).contains(z.x, z.y, 3);
            if (kept) continue;
            best = c;
            bx = z.x;
            by = z.y;
        }
        return best >= min ? new float[]{bx, by, best} : null;
    }

    /** An airborne squad dropped on the spot: six soldiers, a machine gunner among them, who hold it. */
    ArrayList<Entity> airborne(float x, float y) {
        ArrayList<Entity> squad = new ArrayList<Entity>();
        float[] spot = w.city.findWalkable(x, y);
        if (spot == null) return squad;
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3;
            Entity e = w.population.spawnSoldier(k == 0 ? Entity.ROLE_GUNNER : Entity.ROLE_RIFLE, spot[0] + (float) Math.cos(a) * 9, spot[1] + (float) Math.sin(a) * 9);
            if (e == null) continue;
            squad.add(e);
            for (int p = 0; p < 5; p++) w.effects.particle(e.x, e.y, w.rnd.nextFloat() * 30 - 15, w.rnd.nextFloat() * 30 - 15, 0.7f, 1.4f, 0xFFD8D2C0, World.P_DEBRIS);
        }
        if (squad.isEmpty()) return squad;
        w.dispatch.order(squad, spot[0], spot[1]);
        w.emit(Sfx.ROTOR, spot[0], spot[1]);
        w.dispatch.say(Dispatch.WHO_MILITARY, squad.get(0), "Airborne on the ground at " + w.city.placeName(spot[0], spot[1]) + ". " + squad.size() + " of us, holding.", spot[0], spot[1]);
        return squad;
    }

    /**
     * ((22.0) Combat engineers dig in wherever they're holding: with the dead about but none on top of them, sandbags
     * go down on the street between them and the trouble, a few at a time, up to six round each of them. Only the
     * engineers build: the others fight.
     */
    void engineersFortify() {
        if (!w.outbreak || (w.fortifyCd -= 1) > 0) return;
        w.fortifyCd = 12;
        int most = 6;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || e.type != Entity.SOLDIER || e.role != Entity.ROLE_ENGINEER || e.rig != null || e.ride != null) continue;
            if (e.task != Dispatch.T_HOLD && e.task != Dispatch.T_POST && e.task != Dispatch.T_GUARD && e.task != Dispatch.T_CORDON) continue;
            if (w.spatial.countZombiesNear(e.x, e.y, 60) > 0) continue;
            float[] c = w.spatial.zombieCentre(e.x, e.y, 400);
            if (c[2] < 1) continue;
            int have = 0;
            for (float[] b : w.barriers) if (World.hypot(b[0] - e.x, b[1] - e.y) < 90) have++;
            if (have >= most) continue;
            float dx = c[0] - e.x, dy = c[1] - e.y, d = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
            dx /= d;
            dy /= d;
            int laid = 0;
            for (int k = -1; k <= 1 && have + laid < most; k++) {
                // A short line across the way they'd come, thirty-odd units out.
                float bx = e.x + dx * 34 - dy * k * City.T, by = e.y + dy * 34 + dx * k * City.T;
                int t = w.city.tileIndex(bx, by);
                if (t < 0 || (w.city.tiles[t] != City.ROAD && w.city.tiles[t] != City.SIDEWALK)) continue;
                float[] b = w.sandbox.placeBarricade(bx, by);
                if (b == null) continue;
                World.setTier(b, 1);
                laid++;
            }
            if (laid > 0) {
                w.sandbagsLaid += laid;
                if (!e.fortifySaid) {
                    e.fortifySaid = true;
                    w.dispatch.chatter(Dispatch.WHO_MILITARY, e, "Engineers: sandbags going down at " + w.city.placeName(e.x, e.y) + ".", e.x, e.y);
                }
            }
        }
    }

    void engineersWire() {
        if (!w.outbreak || w.dispatch.zones.isEmpty() || (w.wireCd -= 1) > 0) return;
        w.wireCd = 10;
        for (int zi = 0; zi < w.dispatch.zones.size(); zi++) {
            Dispatch.SafeZone z = w.dispatch.zones.get(zi);
            if (z.removed) continue;
            int have = 0;
            for (int i = 0, n = w.defences.wires.size(); i < n; i++) {
                float[] c = w.defences.wires.get(i);
                if (World.hypot(c[0] - z.x, c[1] - z.y) < z.r + 70) have++;
            }
            if (have >= World.ZONE_WIRE) continue;
            Entity eng = null;
            for (int i = 0, n = w.spatial.gatherArmed(z.x, z.y, z.r + 120); i < n; i++)
                if (w.gathered[i].type == Entity.SOLDIER && w.gathered[i].role == Entity.ROLE_ENGINEER && !w.gathered[i].dead) {
                    eng = w.gathered[i];
                    break;
                }
            if (eng == null) continue;
            for (int tries = 0; tries < 16; tries++) {
                double a = w.rnd.nextFloat() * Math.PI * 2;
                float wx = z.x + (float) Math.cos(a) * (z.r + 26), wy = z.y + (float) Math.sin(a) * (z.r + 26);
                int t = w.city.tileIndex(wx, wy);
                if (t < 0 || (w.city.tiles[t] != City.ROAD && w.city.tiles[t] != City.SIDEWALK)) continue;
                if (layWire(wx, wy) == null) continue;
                if (w.wireSaid.add(z))
                    w.dispatch.say(Dispatch.WHO_MILITARY, eng, "Engineers: wiring the streets into " + z.place + ".", z.x, z.y);
                break;
            }
        }
    }

    /** A drone up from the nearest police station (or right there if there's none) to watch over (x, y). */
    float[] launchDrone(float x, float y) {
        City.Facility st = w.city.nearestFacility(City.FACILITY_POLICE, x, y);
        float sx = st != null ? st.gateX : x, sy = st != null ? st.gateY : y;
        float[] d = {sx, sy, x, y, World.WATCH_LIFE, 2};
        w.policeDrones.add(d);
        w.dispatch.say(Dispatch.WHO_POLICE, null, "Police: Drone up, heading for " + w.city.placeName(x, y) + ".", x, y);
        return d;
    }

    boolean droneOver(float x, float y) {
        for (int i = 0, n = w.policeDrones.size(); i < n; i++) {
            float[] d = w.policeDrones.get(i);
            float dx = d[0] - x, dy = d[1] - y;
            if (dx * dx + dy * dy < World.WATCH_SEE * World.WATCH_SEE && World.hypot(d[0] - d[2], d[1] - d[3]) < 60) return true;
        }
        return false;
    }

    /** Each frame: the drones fly out to where they're watching and circle it; now and then they report what they see. */
    void dronesStep(float dt) {
        for (int i = w.policeDrones.size() - 1; i >= 0; i--) {
            float[] d = w.policeDrones.get(i);
            d[4] -= dt;
            if (d[4] <= 0) {
                w.policeDrones.remove(i);
                continue;
            }
            float a = w.time * 0.6f + i;
            float gx = d[2] + (float) Math.cos(a) * 40, gy = d[3] + (float) Math.sin(a) * 40;
            float dx = gx - d[0], dy = gy - d[1], dl = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
            float step = Math.min(dl, 110 * dt);
            d[0] += dx / dl * step;
            d[1] += dy / dl * step;
            d[5] -= dt;
            if (((int) (d[4] * 2)) % 3 == 0 && w.rnd.nextFloat() < dt * 2) w.emit(Sfx.BUZZ, d[0], d[1]);
            if (d[5] > 0 || World.hypot(d[0] - d[2], d[1] - d[3]) > 60) continue;
            d[5] = 6;
            // What it sees: the other side in numbers gets the police sent; a bomb gets the bomb squad.
            int seen = 0;
            float sx = 0, sy = 0;
            for (int k = 0, n = w.entities.size(); k < n; k++) {
                Entity e = w.entities.get(k);
                if (e.dead || !e.isZombie() || World.hypot(e.x - d[0], e.y - d[1]) > World.WATCH_SEE) continue;
                seen++;
                sx += e.x;
                sy += e.y;
            }
            if (seen > 0) {
                w.dispatch.sighting(sx / seen, sy / seen, null);
                if (w.rnd.nextFloat() < 0.4f)
                    w.dispatch.say(Dispatch.WHO_POLICE, null, "Drone: " + seen + " hostile" + (seen == 1 ? "" : "s") + " near " + w.city.placeName(sx / seen, sy / seen)
                            + ". Marking them.", sx / seen, sy / seen);
            }
        }
    }

    void updateDrones(float dt) {
        if (!w.dronesMade && w.alert >= 1) {
            w.dronesMade = true;
            for (City.Facility f : w.city.facilities) {
                // ((14.3) Eight at most, from four.)
                if (f.kind != City.FACILITY_POLICE || w.drones.size() >= World.MAX_DRONES) continue;
                World.Drone d = new World.Drone();
                d.x = d.homeX = d.tx = f.x;
                d.y = d.homeY = d.ty = f.y;
                d.number = w.drones.size() + 1;
                w.drones.add(d);
            }
            if (!w.drones.isEmpty())
                w.dispatch.say(Dispatch.WHO_POLICE, null, "Dispatch: Drone units, get airborne. Eyes over the hot spots, call in everything you see.",
                        w.drones.get(0).x, w.drones.get(0).y);
        }
        for (int i = 0; i < w.drones.size(); i++) {
            World.Drone d = w.drones.get(i);
            if (d.state == World.DRONE_DOCKED) {
                // ((14.3) Quicker to charge, and longer in the air: 5 and 0.55 a second, from 3 and 0.8.)
                d.battery = Math.min(100, d.battery + dt * 5);
                if (d.battery >= 95 && w.alert >= 1 && w.zombies > 0 && droneTarget(d)) d.state = World.DRONE_OUT;
                continue;
            }
            float dx = d.tx - d.x, dy = d.ty - d.y, dist = (float) Math.sqrt(dx * dx + dy * dy);
            float step = Math.min(dist, 95 * dt);
            if (dist > 0.01f) {
                d.x += dx / dist * step;
                d.y += dy / dist * step;
            }
            d.battery -= dt * 0.55f;
            if (d.state == World.DRONE_HOME) {
                if (dist < 6) {
                    d.state = World.DRONE_DOCKED;
                    d.x = d.homeX;
                    d.y = d.homeY;
                }
                continue;
            }
            if (d.battery < 22 || w.zombies == 0) {
                d.state = World.DRONE_HOME;
                d.tx = d.homeX;
                d.ty = d.homeY;
                continue;
            }
            // Over the target: a sweep round it, then on to the next.
            d.sweepCd -= dt;
            if (dist < 15) {
                if (d.sweepCd <= 0 && !droneTarget(d)) {
                    float a = w.rnd.nextFloat() * World.TAU;
                    d.tx = Math.max(20, Math.min(w.city.worldW() - 20, d.x + (float) Math.cos(a) * 160));
                    d.ty = Math.max(20, Math.min(w.city.worldH() - 20, d.y + (float) Math.sin(a) * 160));
                } else if (d.sweepCd > 0) {
                    float a = w.rnd.nextFloat() * World.TAU;
                    d.tx = d.x + (float) Math.cos(a) * 120;
                    d.ty = d.y + (float) Math.sin(a) * 120;
                }
            }
            d.spotCd -= dt;
            if (d.spotCd <= 0) {
                d.spotCd = 1;
                droneSpot(d);
            }
            d.speakCd -= dt;
            if (d.speakCd <= 0) {
                d.speakCd = 5;
                droneSpeaker(d);
            }
        }
    }

    /** (11.0) Where a drone goes next: the worst infection zone or call nobody much is at. False if there's nowhere. */
    boolean droneTarget(World.Drone d) {
        float best = 0, bx = 0, by = 0;
        for (int i = 0; i < w.infection.size(); i++) {
            World.InfectionZone z = w.infection.get(i);
            float s = z.zombies + 5 - (float) World.hypot(z.x - d.x, z.y - d.y) / 400;
            for (World.Drone o : w.drones) if (o != d && o.state == World.DRONE_OUT && World.hypot(o.tx - z.x, o.ty - z.y) < z.r + 100) s -= 8;
            if (s > best) {
                best = s;
                bx = z.x + w.rnd.nextFloat() * z.r - z.r / 2;
                by = z.y + w.rnd.nextFloat() * z.r - z.r / 2;
            }
        }
        for (int i = 0; i < w.dispatch.incidents.size(); i++) {
            Dispatch.Incident inc = w.dispatch.incidents.get(i);
            if (inc.resolved || inc.cordonPost >= 0) continue;
            float s = inc.zombiesNear + 3 - (inc.cops + inc.soldiers) - (float) World.hypot(inc.x - d.x, inc.y - d.y) / 400;
            for (World.Drone o : w.drones) if (o != d && o.state == World.DRONE_OUT && World.hypot(o.tx - inc.x, o.ty - inc.y) < 200) s -= 8;
            if (s > best) {
                best = s;
                bx = inc.x;
                by = inc.y;
            }
        }
        if (best <= 0) return false;
        d.tx = bx;
        d.ty = by;
        d.sweepCd = 12;
        return true;
    }

    /**
     * (14.3) A drone's loudspeaker: civilians below are told where the nearest safe zone with room is, and those with
     * nowhere better to go make for it.
     */
    void droneSpeaker(World.Drone d) {
        if (!w.dispatch.hasRoom()) return;
        int sent = 0;
        for (int i = 0, n = w.spatial.gatherLiving(d.x, d.y, World.DRONE_SEE); i < n; i++) {
            Entity e = w.gathered[i];
            if (e.dead || e.type != Entity.CIVILIAN || e == w.controlled || e.raider || e.militia || e.infected) continue;
            e.heardZone = true;
            e.aware = true;
            if (e.task == Dispatch.T_NONE && e.leader == null) {
                e.task = Dispatch.T_SEEK;
                sent++;
            }
        }
        if (sent >= 3 && w.time - d.saidAt > 30) {
            d.saidAt = w.time;
            w.dispatch.say(Dispatch.WHO_POLICE, null, "Drone " + d.number + " (loudspeaker): Everyone on " + w.city.placeName(d.x, d.y)
                    + ", make for the nearest safe zone now!", d.x, d.y);
        }
    }

    /**
     * (11.0) What a drone sees below it: the dead out in the open, and any it can see moving inside buildings.
     * ((14.3) And it marks the dead it sees for the units on the ground: they're noticed even behind someone's back,
     * fired on from further off, and hit in the head more often.)
     */
    void droneSpot(World.Drone d) {
        int cx0 = Math.max(0, (int) ((d.x - World.DRONE_SEE) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((d.x + World.DRONE_SEE) / World.CELL));
        int cy0 = Math.max(0, (int) ((d.y - World.DRONE_SEE) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((d.y + World.DRONE_SEE) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c], end = k + w.zCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (!o.dead && (o.x - d.x) * (o.x - d.x) + (o.y - d.y) * (o.y - d.y) < World.DRONE_SEE * World.DRONE_SEE) o.spottedUntil = w.time + 1.6f;
                }
            }
        int seen = w.spatial.countZombiesNear(d.x, d.y, World.DRONE_SEE);
        int inside = 0;
        City.Building found = null;
        for (int i = 0, n = w.city.buildings.size(); i < n; i++) {
            City.Building b = w.city.buildings.get(i);
            if (b.lurkers <= 0 || b.collapsed || Math.abs(b.doorX - d.x) > World.DRONE_SEE || Math.abs(b.doorY - d.y) > World.DRONE_SEE) continue;
            if (!b.infestKnown) {
                b.infestKnown = true;
                found = b;
            }
            inside += b.lurkers;
        }
        if (seen + inside == 0) return;
        boolean covered = false;
        for (int i = 0; i < w.dispatch.incidents.size() && !covered; i++) {
            Dispatch.Incident inc = w.dispatch.incidents.get(i);
            if (!inc.resolved && World.hypot(inc.x - d.x, inc.y - d.y) < 250) covered = true;
        }
        if (covered && found == null) return;
        float sx = found != null ? found.doorX : d.x, sy = found != null ? found.doorY : d.y;
        if (!covered && seen > 0) {
            Entity z = w.spatial.nearestZombie(d.x, d.y, World.DRONE_SEE);
            if (z != null) {
                sx = z.x;
                sy = z.y;
            }
            w.dispatch.sighting(sx, sy, null);
        }
        if (w.time - d.saidAt > 20) {
            d.saidAt = w.time;
            w.dispatch.say(Dispatch.WHO_POLICE, null, "Drone " + d.number + ": " + (found != null ? "Movement inside " + (found.name != null ? found.name : "a building")
                    + " on " + w.city.placeName(sx, sy) + ", " + found.lurkers + " of them." : seen + (seen == 1 ? " hostile" : " hostiles")
                    + " on " + w.city.placeName(sx, sy) + ". Calling it in."), sx, sy);
        }
    }
}
