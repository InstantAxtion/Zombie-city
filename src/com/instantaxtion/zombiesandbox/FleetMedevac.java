package com.instantaxtion.zombiesandbox;

import static com.instantaxtion.zombiesandbox.Fleet.*;

import java.util.ArrayList;

/**
 * ((27.5) Part of the vehicle code.) Getting the wounded out, and the vehicles mended.
 *
 * Police and soldiers badly hurt a long way from any care (or with the dead about) wait where they are for a ride: an
 * ambulance in from the hospital (or the edge of town) for the police, the army's armoured ambulance from its base for
 * soldiers - and for police where the dead are too thick for an ordinary ambulance. It picks them up (and any other
 * wounded waiting close by), takes them to the nearest care - a command post's aid station, the field hospital, a
 * hospital, or their own station or base - and drops them there to be patched up (see ArmedAI.hospitalStep).
 *
 * Police cars badly knocked about go back to the station and are repaired there; anything of the police's or the
 * army's parked at home is mended over time, and nothing below REPAIR_BELOW is sent out until it is.
 */
final class FleetMedevac {
    final Fleet fleet;
    final World w;
    final City city;

    /** Armoured ambulances kept at each base; police cars and army vehicles below this much of their hp aren't sent out. */
    static final int ARMOURED_PER_BASE = 2;
    static final float REPAIR_BELOW = 0.45f, REPAIR_TIME = 60;
    /** Ordinary ambulances out on pickups at once, at the most. */
    static final int MAX_AMBULANCES = 4;
    /** Phases of a pickup: none (parked at home), on the way to the wounded, taking them aboard, to care, home. */
    static final int MP_NONE = 0, MP_TO = 1, MP_LOAD = 2, MP_CARE = 3, MP_HOME = 4;

    /** Wounded picked up so far; vehicles mended and back in service. */
    int pickups, repaired;
    private float tick, baseCd;

    FleetMedevac(Fleet fleet) {
        this.fleet = fleet;
        this.w = fleet.w;
        this.city = fleet.city;
    }

    static boolean armoured(Vehicle v) {
        return v.kind == K_ARMD_AMB;
    }

    /** Whether a vehicle's too knocked about to be sent anywhere until it's mended. */
    static boolean needsRepair(Vehicle v) {
        return !v.broken && v.maxHp > 0 && v.hp < v.maxHp * REPAIR_BELOW;
    }

    /** Twice a second: pickups handed out, armoured ambulances stocked, damaged cars sent in, parked ones mended. */
    void update(float dt) {
        if ((tick -= dt) > 0) return;
        float step = 0.5f - tick;
        tick = 0.5f;
        // ((31.2) Map version 52: no ambulances out for the wounded; vehicles are still mended at home.)
        if (!w.noMedevac) {
            if ((baseCd -= step) <= 0) {
                baseCd = 10;
                stockBases();
            }
            assignPickups();
        }
        repairs(step);
    }

    // ------------------------------------------------------------------ the armoured ambulances

    /** Two armoured ambulances parked in each standing base's yard (one lost is replaced after a while). */
    private void stockBases() {
        for (City.Facility f : city.facilities) {
            if (f.kind != City.FACILITY_BASE || f.fallen) continue;
            int have = 0;
            for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
                Vehicle v = fleet.vehicles.get(i);
                if (armoured(v) && !v.broken && v.depot == f) have++;
            }
            for (int k = have; k < ARMOURED_PER_BASE; k++) {
                float[] g = city.nearestDrivable(f.gateX > 0 ? f.gateX : f.x, f.gateY > 0 ? f.gateY : f.y);
                if (g == null) break;
                float[] p = fleet.bay(g, f);
                if (p == null) p = g;
                // (Not fleet.make: that draws on the world's dice, and parking these shouldn't change what else happens.)
                Vehicle v = new Vehicle();
                v.type = AMBULANCE;
                v.lane = 2;
                v.fuel = 1;
                v.kind = K_ARMD_AMB;
                v.hp = v.maxHp = 420;
                v.x = p[0];
                v.y = p[1];
                v.homeX = g[0];
                v.homeY = g[1];
                v.angle = (float) Math.atan2(g[1] - p[1], g[0] - p[0]);
                v.depot = f;
                v.medevac = true;
                v.parked = true;
                v.state = WAIT;
                v.passengerType = Entity.SOLDIER;
                fleet.vehicles.add(v);
            }
        }
    }

    // ------------------------------------------------------------------ pickups

    /** Whether e is waiting for a ride out, and who's coming for them. */
    Vehicle comingFor(Entity e) {
        for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (v.medevac && !v.broken && !v.removedFromFleet && v.medPhase != MP_NONE && v.squad.contains(e)) return v;
        }
        return null;
    }

    /** The wounded waiting with nobody coming: the nearest free vehicle that can reach them goes. */
    private void assignPickups() {
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (!e.waitPickup || e.dead || e.removed || e.infected) continue;
            if (e.casevac != null && (e.casevac.removedFromFleet || e.casevac.broken || !e.casevac.squad.contains(e))) e.casevac = null;
            if (e.casevac == null) e.casevac = comingFor(e);
            if (e.casevac != null || w.time < e.casevacAt) continue;
            e.casevacAt = w.time + 5;
            boolean hot = w.countZombiesNear(e.x, e.y, 150) >= 2;
            Vehicle v = null;
            if (e.type == Entity.SOLDIER || hot) v = freeArmoured(e.x, e.y);
            if (v == null && !hot) v = civilAmbulance(e.x, e.y);
            if (v == null) continue;
            float[] to = driveTo(v, e.x, e.y);
            if (to == null) {
                if (v.medPhase == MP_NONE && !armoured(v)) {
                    v.removedFromFleet = true;
                    fleet.vehicles.remove(v);
                }
                continue;
            }
            v.parked = false;
            v.medPhase = MP_TO;
            v.state = DRIVE;
            v.timer = 0;
            v.stuckTimer = 0;
            v.tx = to[0];
            v.ty = to[1];
            v.place = city.placeName(to[0], to[1]);
            v.squad.clear();
            // (Them, and the others of the wounded waiting close by with nobody coming.)
            for (int k = 0; k < n && v.squad.size() < 4; k++) {
                Entity o = w.entities.get(k);
                if (o.waitPickup && !o.dead && !o.removed && o.casevac == null && World.hypot(o.x - e.x, o.y - e.y) < 150) {
                    v.squad.add(o);
                    o.casevac = v;
                }
            }
            w.emit(Sfx.AMB_SIREN, v.x, v.y);
            w.dispatch.chatter(e.type == Entity.SOLDIER || armoured(v) ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, null,
                    (armoured(v) ? "Medevac: Armoured ambulance rolling to " : "EMS: Ambulance on its way to ") + v.place + " for the wounded.", e.x, e.y);
        }
    }

    /**
     * Routes v to the drivable spot nearest (x, y), or failing that to the nearest road: the nearest paved tile can be a
     * yard or a lot the roads don't reach. Where it's going, or null if it can't get there.
     */
    private float[] driveTo(Vehicle v, float x, float y) {
        float[] to = city.nearestDrivable(x, y);
        if (to != null && fleet.routing.route(v, to[0], to[1])) return to;
        int tx = (int) (x / City.T), ty = (int) (y / City.T);
        for (int r = 1; r <= 10; r++)
            for (int j = ty - r; j <= ty + r; j++)
                for (int i = tx - r; i <= tx + r; i++) {
                    if (Math.max(Math.abs(i - tx), Math.abs(j - ty)) != r || i < 0 || j < 0 || i >= city.w || j >= city.h) continue;
                    if (city.tiles[j * city.w + i] != City.ROAD) continue;
                    float[] p = {i * City.T + City.T / 2f, j * City.T + City.T / 2f};
                    return fleet.routing.route(v, p[0], p[1]) ? p : null;
                }
        return null;
    }

    private Vehicle freeArmoured(float x, float y) {
        Vehicle best = null;
        float bd = Float.MAX_VALUE;
        for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (!armoured(v) || v.broken || v.medPhase != MP_NONE || v.player != null || needsRepair(v)) continue;
            float d = (float) World.hypot(v.x - x, v.y - y);
            if (d < bd) {
                bd = d;
                best = v;
            }
        }
        return best;
    }

    /** An ambulance out from the hospital (or in from the edge of town) for a pickup at (x, y); null if none can come. */
    private Vehicle civilAmbulance(float x, float y) {
        int out = 0;
        for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (v.medevac && !armoured(v) && !v.removedFromFleet) out++;
        }
        if (out >= MAX_AMBULANCES) return null;
        City.Facility h = city.nearestFacility(City.FACILITY_HOSPITAL, x, y);
        float[] start = h != null ? city.nearestDrivable(h.gateX, h.gateY) : null;
        if (start == null) {
            float[] e = city.edgeSpawn(x, y);
            start = e != null ? city.nearestDrivable(e[0], e[1]) : null;
        }
        if (start == null) return null;
        Vehicle v = fleet.make(AMBULANCE);
        v.x = v.homeX = start[0];
        v.y = v.homeY = start[1];
        v.angle = (float) Math.atan2(y - start[1], x - start[0]);
        v.medevac = true;
        v.passengerType = Entity.CIVILIAN;
        fleet.vehicles.add(v);
        return v;
    }

    /** (From FleetService.updateCar.) An ambulance out for the wounded: there, taking them aboard, to care, home. */
    boolean step(Vehicle v, float dt) {
        switch (v.medPhase) {
            case MP_NONE:
                v.speed = 0;
                if (!armoured(v)) return true;
                return false;
            case MP_TO: {
                // (Nobody left to fetch: home.)
                for (int k = v.squad.size() - 1; k >= 0; k--) {
                    Entity e = v.squad.get(k);
                    if (e.dead && e.ride != v || e.removed && e.ride != v || e.infected || !e.waitPickup) v.squad.remove(k);
                }
                if (v.squad.isEmpty()) return goHome(v);
                v.timer += dt;
                boolean moving = fleet.driving.driveStep(v, dt, armoured(v) ? 70 : 85, fleet.driving.carAhead(v) ? 0.5f : 1f);
                if (moving) fleet.driving.hit(v, w.runOver(v.x, v.y, 8, v.speed, v.angle));
                float d = (float) World.hypot(v.tx - v.x, v.ty - v.y);
                if (d < 40 || (!moving && d < 140) || v.timer > 150) {
                    v.medPhase = MP_LOAD;
                    v.state = LOAD;
                    v.speed = 0;
                    v.timer = 0;
                }
                return false;
            }
            case MP_LOAD: {
                v.speed = 0;
                v.timer += dt;
                for (int k = v.squad.size() - 1; k >= 0; k--) {
                    Entity e = v.squad.get(k);
                    if (e.ride == v) continue;
                    if (e.dead || e.removed || e.infected || !e.waitPickup) {
                        v.squad.remove(k);
                        continue;
                    }
                    if (World.hypot(e.x - v.x, e.y - v.y) < 30) {
                        e.dead = true;
                        e.removed = true;
                        e.vx = e.vy = 0;
                        e.ride = v;
                        e.casevac = v;
                        v.riders.add(e);
                    }
                }
                boolean all = true;
                for (int k = 0; k < v.squad.size(); k++) if (v.squad.get(k).ride != v) all = false;
                boolean danger = !armoured(v) && w.countZombiesNear(v.x, v.y, 60) >= 3;
                if (all || danger || v.timer > 30) {
                    // (Those who didn't make it aboard go back to waiting for another.)
                    for (int k = v.squad.size() - 1; k >= 0; k--) {
                        Entity e = v.squad.get(k);
                        if (e.ride != v) {
                            e.casevac = null;
                            v.squad.remove(k);
                        }
                    }
                    if (v.riders.isEmpty()) return goHome(v);
                    float[] care = careFor(v.riders.get(0), v.x, v.y);
                    float[] to = care != null ? driveTo(v, care[0], care[1]) : null;
                    if (to == null) {
                        unload(v);
                        return goHome(v);
                    }
                    v.medPhase = MP_CARE;
                    v.state = RETURN;
                    v.tx = to[0];
                    v.ty = to[1];
                    v.timer = 0;
                    v.stuckTimer = 0;
                    v.place = city.placeName(to[0], to[1]);
                    pickups += v.riders.size();
                }
                return false;
            }
            case MP_CARE: {
                v.timer += dt;
                boolean moving = fleet.driving.driveStep(v, dt, armoured(v) ? 70 : 85, fleet.driving.carAhead(v) ? 0.5f : 1f);
                if (moving) fleet.driving.hit(v, w.runOver(v.x, v.y, 8, v.speed, v.angle));
                float d = (float) World.hypot(v.tx - v.x, v.ty - v.y);
                if (d < 50 || (!moving && d < 160) || v.timer > 240) {
                    unload(v);
                    return goHome(v);
                }
                return false;
            }
            default: {
                v.timer += dt;
                boolean moving = fleet.driving.driveStep(v, dt, 60, fleet.driving.carAhead(v) ? 0.5f : 1f);
                float d = (float) World.hypot(v.homeX - v.x, v.homeY - v.y);
                if (d < 40 || (!moving && d < 120) || v.timer > 200) {
                    if (!armoured(v)) return true;
                    // (Back in its bay in the yard.)
                    float[] p = v.depot != null ? fleet.bay(new float[]{v.x, v.y}, v.depot) : null;
                    if (p != null && World.hypot(p[0] - v.x, p[1] - v.y) < City.T * 8) {
                        v.x = p[0];
                        v.y = p[1];
                    }
                    v.medPhase = MP_NONE;
                    v.state = WAIT;
                    v.parked = true;
                    v.speed = 0;
                    v.squad.clear();
                }
                return false;
            }
        }
    }

    /** The wounded out at the care they were brought to, to be patched up there. */
    private void unload(Vehicle v) {
        for (Entity e : new ArrayList<Entity>(v.riders)) {
            if (!w.release(e, v.x, v.y)) continue;
            e.casevac = null;
            e.waitPickup = false;
            e.inCare = true;
            e.task = Dispatch.T_HEAL;
            e.hospitalT = 0;
        }
        v.riders.clear();
        v.squad.clear();
    }

    private boolean goHome(Vehicle v) {
        for (Entity e : v.squad) if (e.casevac == v) e.casevac = null;
        v.squad.clear();
        if (!armoured(v)) {
            v.medPhase = MP_HOME;
            v.state = RETURN;
            v.timer = 0;
            return fleet.service.routeHomeOrPark(v);
        }
        v.medPhase = MP_HOME;
        v.state = RETURN;
        v.timer = 0;
        v.stuckTimer = 0;
        if (!fleet.routing.route(v, v.homeX, v.homeY)) {
            v.medPhase = MP_NONE;
            v.state = WAIT;
            v.parked = true;
            v.speed = 0;
        }
        return false;
    }

    /**
     * Where someone of e's side wounded near (x, y) is patched up: their side's command post set up as an aid station,
     * the field hospital, a hospital, or else their own station or base. Null with nowhere at all.
     */
    float[] careFor(Entity e, float x, float y) {
        Ops.Post aid = w.ops.aidFor(e, w.hospitalX, w.hospitalY);
        if (aid != null) return new float[]{aid.x, aid.y};
        if (!Float.isNaN(w.hospitalX)) return new float[]{w.hospitalX, w.hospitalY};
        City.Facility h = city.nearestFacility(City.FACILITY_HOSPITAL, x, y);
        if (h != null && !h.fallen) return outdoors(h.gateX > 0 ? h.gateX : h.x, h.gateY > 0 ? h.gateY : h.y);
        City.Facility home = e.type == Entity.SOLDIER ? city.mainBase() : null;
        if (home == null || home.fallen) {
            home = null;
            float bd = Float.MAX_VALUE;
            for (City.Facility f : city.facilities) {
                if (f.fallen || f.kind != (e.type == Entity.SOLDIER ? City.FACILITY_BASE : City.FACILITY_POLICE)) continue;
                float d = (float) World.hypot(f.x - x, f.y - y);
                if (d < bd) {
                    bd = d;
                    home = f;
                }
            }
        }
        if (home == null) return null;
        return outdoors(home.gateX > 0 ? home.gateX + (home.x - home.gateX) * 0.4f : home.x, home.gateY > 0 ? home.gateY + (home.y - home.gateY) * 0.4f : home.y);
    }

    /** A spot near (x, y) out in the open: indoors, they'd only be walked straight back out again. */
    private float[] outdoors(float x, float y) {
        City.Building b = city.insideOf(x, y);
        if (b != null) {
            x = b.doorX;
            y = b.doorY;
        }
        float[] p = city.findWalkable(x, y);
        if (p != null && city.insideOf(p[0], p[1]) != null && b != null) p = new float[]{b.doorX, b.doorY};
        return p != null ? p : new float[]{x, y};
    }

    // ------------------------------------------------------------------ repairs

    /**
     * Police cars on their rounds badly knocked about go back to the station; police and army vehicles parked at home
     * (and the armoured ambulances) are mended there, a little at a time.
     */
    private void repairs(float dt) {
        for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (v.broken || v.player != null || v.maxHp <= 0) continue;
            if (v.type != CRUISER && v.type != TRUCK && !armoured(v)) continue;
            if (v.type == CRUISER && v.patrol && v.state == PATROL && v.incident == null && !v.crew.isEmpty() && needsRepair(v) && v.kind != K_BIKE) {
                // (In to the station for repairs, the crew in it.)
                v.squad.clear();
                v.pickup = true;
                if (fleet.service.headHome(v)) {
                    v.patrol = false;
                    w.dispatch.chatter(Dispatch.WHO_POLICE, v.crew.get(0), fleet.patrols.callsign(v) + ", the car's in a bad way. Bringing it in for repairs.", v.x, v.y);
                } else v.pickup = false;
                continue;
            }
            if (v.hp >= v.maxHp || !atHome(v)) continue;
            boolean was = v.hp < v.maxHp * 0.95f;
            v.hp = Math.min(v.maxHp, v.hp + v.maxHp * dt / REPAIR_TIME);
            if (was && v.hp >= v.maxHp * 0.95f) {
                v.hp = v.maxHp;
                repaired++;
            }
        }
    }

    /** Parked in the yard at its station or base (or, a command truck, parked at home). */
    static boolean atHome(Vehicle v) {
        if (isPost(v)) return v.state == WAIT;
        if (armoured(v)) return v.medPhase == MP_NONE && v.parked;
        return v.standby || (v.parked && v.depot != null && World.hypot(v.x - v.depot.x, v.y - v.depot.y) < v.depot.r + 300);
    }

    /** "Repairing, 70%" for a vehicle being mended at home; null otherwise. */
    static String repairing(Vehicle v) {
        if (v.broken || v.maxHp <= 0 || v.hp >= v.maxHp * 0.95f || !atHome(v)) return null;
        if (v.type != CRUISER && v.type != TRUCK && !armoured(v)) return null;
        return "Repairing, " + (int) (100 * v.hp / v.maxHp) + "%";
    }
}
