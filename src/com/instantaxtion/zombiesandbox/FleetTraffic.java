package com.instantaxtion.zombiesandbox;

import static com.instantaxtion.zombiesandbox.Fleet.*;

import java.util.ArrayList;

/**
 * ((27.3) Part of the vehicle code, out of Fleet.) Everyday traffic: the cars people drive about town, through traffic and people driving out, fuel and the pumps, car alarms, and a car put down by hand.
 */
final class FleetTraffic {
    final Fleet fleet;
    final World w;
    final City city;

    FleetTraffic(Fleet fleet) {
        this.fleet = fleet;
        this.w = fleet.w;
        this.city = fleet.city;
    }

    float[] pumpFuel() {
        if (fleet.pumpFuel == null) {
            fleet.pumpFuel = new float[city.pumps.size()];
            java.util.Arrays.fill(fleet.pumpFuel, 6);
        }
        return fleet.pumpFuel;
    }

    /**
     * (10.32) Ordinary cars burn fuel as they go. Running low, a driver pulls in at the nearest pump that has any
     * (once things are bad, everyone tops up, and the pumps run dry); run out and the car is left where it stops.
     * Returns true while it's stopped at a pump filling up (or has just run dry).
     */
    boolean fuel(Vehicle v, float dt) {
        if (v.type != CAR || v.player != null || v.model == M_BUS) return false;
        v.fuel -= Math.abs(v.speed) * dt / CAR_RANGE;
        v.refuelCd -= dt;
        // (Before the emergency nobody runs dry: they fill up somewhere along the way.)
        if (v.fuel <= 0.05f && w.alert < 2) v.fuel = 0.6f;
        if (v.fuel <= 0) {
            v.fuel = 0;
            fleet.ranDry++;
            // (They get out where it stopped, fleeing or not.)
            fleet.service.dropRiders(v, true);
            v.parked = true;
            v.speed = 0;
            v.field = null;
            v.pumpAt = -1;
            fleet.driving.pullOver(v);
            return true;
        }
        float[] pf = pumpFuel();
        if (v.pumpAt >= 0 && v.pumpAt < pf.length) {
            float[] p = city.pumps.get(v.pumpAt);
            if (World.hypot(p[0] - v.x, p[1] - v.y) < 30) {
                v.speed = Math.max(0, v.speed - dt * 160);
                v.refuelT += dt;
                if (v.refuelT > 3.5f) {
                    float take = Math.min(1 - v.fuel, pf[v.pumpAt]);
                    v.fuel += take;
                    pf[v.pumpAt] -= take;
                    v.pumpAt = -1;
                    v.refuelT = 0;
                    v.field = null;
                    v.destCd = 0;
                    if (!fleet.routing.newDestination(v)) v.stuckTimer = Math.max(v.stuckTimer, 5);
                    if (!fleet.drySaid) {
                        float left = 0, all = 0;
                        for (float f : pf) left += f;
                        all = pf.length * 6;
                        if (pf.length > 0 && left < all * 0.25f) {
                            fleet.drySaid = true;
                            w.dispatch.say(Dispatch.WHO_INFO, null, "Gas stations across town are running dry.", v.x, v.y);
                            w.timeline("The gas stations are running dry");
                        }
                    }
                }
                return true;
            }
            if (pf[v.pumpAt] < 0.05f) v.pumpAt = -1;
            return false;
        }
        float want = w.alert >= 2 ? 0.55f : 0.12f;
        if (v.fuel < want && v.refuelCd <= 0 && !v.through) {
            v.refuelCd = 25;
            int best = -1;
            float bd = 2600;
            for (int i = 0; i < pf.length; i++) {
                if (pf[i] < 0.3f || city.pumps.get(i)[2] <= 0) continue;
                float[] p = city.pumps.get(i);
                float d = (float) World.hypot(p[0] - v.x, p[1] - v.y);
                if (d < bd) {
                    bd = d;
                    best = i;
                }
            }
            if (best >= 0) {
                float[] p = city.pumps.get(best);
                float[] at = city.nearestDrivable(p[0], p[1]);
                if (at != null && fleet.routing.route(v, at[0], at[1])) {
                    v.pumpAt = best;
                    v.refuelT = 0;
                }
            }
        }
        return false;
    }

    /** Ordinary traffic: cars driving around town between random spots on the roads. */
    void spawnTraffic(int n) {
        int buses = 0;
        for (int i = 0; i < n; i++) {
            // Some of it is through traffic on the highway, end to end (spread along it, not queued at the ends).
            if (city.hwyAxis >= 0 && w.rnd.nextFloat() < 0.3f && throughTraffic() < throughTarget() * 1.3f) {
                spawnThrough(true);
                continue;
            }
            float[] start = fleet.routing.randomRoad();
            // New cars turn up out of sight, not out of thin air in front of you.
            for (int k = 0; k < 8 && start != null && fleet.routing.inView(start[0], start[1]); k++) start = fleet.routing.randomRoad();
            if (start == null || fleet.routing.inView(start[0], start[1])) continue;
            Vehicle v = fleet.make(CAR);
            v.state = CRUISE;
            v.x = start[0];
            v.y = start[1];
            pickModel(v, buses < Math.max(1, n / 14));
            if (v.model == M_BUS) buses++;
            v.angle = w.rnd.nextInt(4) * (float) Math.PI / 2;
            if (!fleet.routing.newDestination(v)) continue;
            fleet.vehicles.add(v);
        }
    }

    /** Cars out on the highway. */
    int throughTraffic() {
        int n = 0;
        for (int i = 0; i < fleet.vehicles.size(); i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (v.type == CAR && !v.parked && city.onHighway(v.x, v.y)) n++;
        }
        return n;
    }

    /** (10.29) How many cars the highway carries while things are calm: one every couple of dozen tiles. */
    int throughTarget() {
        if (city.hwyAxis < 0) return 0;
        return Math.max(4, Math.round((city.hwyAxis == 0 ? city.w : city.h) / 24f * Math.max(0.4f, fleet.trafficScale)));
    }

    /** A family car leaving town for the highway (and off the end of it), out of sight when it sets off. */
    void spawnEvacuee() {
        float[] start = null;
        for (int k = 0; k < 12; k++) {
            float[] s = fleet.routing.randomRoad();
            if (s != null && city.inTown(s[0], s[1]) && !fleet.routing.inView(s[0], s[1])) {
                start = s;
                break;
            }
        }
        float[] run = city.highwayRun(w.rnd);
        if (start == null || run == null) return;
        Vehicle v = fleet.make(CAR);
        v.state = CRUISE;
        v.x = start[0];
        v.y = start[1];
        pickModel(v, false);
        v.angle = w.rnd.nextInt(4) * (float) Math.PI / 2;
        v.fleeing = true;
        v.through = true;
        if (fleet.routing.route(v, run[2], run[3])) fleet.vehicles.add(v);
    }

    void spawnThrough(boolean anywhere) {
        float[] run = city.highwayRun(w.rnd);
        if (run == null) return;
        float t = anywhere ? w.rnd.nextFloat() * 0.85f : 0;
        float sx = run[0] + (run[2] - run[0]) * t, sy = run[1] + (run[3] - run[1]) * t;
        if (fleet.routing.inView(sx, sy)) return;
        // (Not on top of a car already there.)
        for (int i = 0; i < fleet.vehicles.size(); i++) {
            Vehicle o = fleet.vehicles.get(i);
            if (Math.abs(o.x - sx) < 3 * City.T && Math.abs(o.y - sy) < 3 * City.T) return;
        }
        Vehicle v = fleet.make(CAR);
        v.state = CRUISE;
        v.x = sx;
        v.y = sy;
        pickModel(v, false);
        v.angle = (float) Math.atan2(run[3] - run[1], run[2] - run[0]);
        v.through = true;
        if (w.rnd.nextFloat() < 0.3f) {
            float[] d = fleet.routing.randomRoad();
            if (d != null && city.inTown(d[0], d[1]) && fleet.routing.route(v, d[0], d[1])) {
                v.through = false;
                fleet.vehicles.add(v);
                return;
            }
        }
        if (fleet.routing.route(v, run[2], run[3])) fleet.vehicles.add(v);
    }

    /** What kind of car this is, and its paint, as the country's streets would have them. */
    void pickModel(Vehicle v, boolean busOk) {
        Country c = city.country;
        int m = c.carModels[w.rnd.nextInt(c.carModels.length)];
        if (m == M_BUS && !busOk) m = M_SEDAN;
        v.model = m;
        v.color = m == M_TAXI ? c.taxiColor : m == M_BUS ? c.busColor : (w.city.cfg.variety() ? c.carColors37 : c.carColors)[w.rnd.nextInt((w.city.cfg.variety() ? c.carColors37 : c.carColors).length)];
    }

    /** A car dropped into traffic by the player. */
    Vehicle placeCar(float x, float y) {
        float[] p = city.nearestDrivable(x, y);
        if (p == null) return null;
        Vehicle v = fleet.make(CAR);
        v.state = CRUISE;
        v.x = p[0];
        v.y = p[1];
        pickModel(v, false);
        v.angle = w.rnd.nextInt(4) * (float) Math.PI / 2;
        if (!fleet.routing.newDestination(v)) return null;
        fleet.vehicles.add(v);
        return v;
    }

    /**
     * ((22.0) A parked car with nobody in it, knocked about: its alarm goes off (chance in a hundred: odds), whooping for
     * twenty-five seconds and drawing every one of the dead for streets around. Once each.
     */
    void tripAlarm(Vehicle v, float odds) {
        if (v.type != CAR || v.alarmed || v.burnt || v.driver != null || !v.riders.isEmpty() || !(v.parked || v.state == ABANDONED)) return;
        if (w.rnd.nextFloat() >= odds) return;
        v.alarmed = true;
        v.alarm = 25;
        fleet.alarms++;
        w.noise(v.x, v.y, 220);
    }

    /**
     * Traffic: drives around, brakes for people and other cars, ploughs through zombies, stops for people
     * waving it down, and gets abandoned when the driver is surrounded.
     */
    boolean updateTraffic(Vehicle v, float dt) {
        if (v.parked) return false;
        if (fuel(v, dt)) return false;
        if (v.pulled) {
            // (Waiting for the officer: not stuck, not a jam.)
            v.stuckTimer = 0;
            if (w.alert > 0) v.pulled = false;
        }
        fleet.driving.giveWay(v, dt);
        boolean zombiesClose = w.countZombiesNear(v.x, v.y, 30) >= 2;
        v.waitTimer -= dt;
        v.hailCd -= dt;
        // Someone is waving: pick them up if there's room.
        if (v.hailCd <= 0 && v.riders.size() < SEATS && !zombiesClose) {
            v.hailCd = 0.5f;
            if (w.hail(v)) v.waitTimer = 4;
        }
        v.timer += dt;
        // Everyone knows: drop the errand and get out of town.
        v.fleeCd -= dt;
        // (Not through traffic, already on its way out, nor anyone on the highway: no turning round on it.)
        if (w.alert >= 2 && !v.fleeing && !v.through && !v.rescue && v.player == null && v.fleeCd <= 0
                && !city.onHighway(v.x, v.y)) {
            v.fleeCd = 4 + w.rnd.nextFloat() * 3;
            if (v.model == M_BUS) {
                // Buses never leave town with people aboard: with a safe zone open the bus takes its passengers
                // there; otherwise the service stops, and the driver pulls in and lets everyone off.
                if (!v.riders.isEmpty()) fleet.crews.boarded(v, v.riders.remove(v.riders.size() - 1));
                if (!v.rescue) {
                    fleet.service.dropRiders(v, false);
                    v.parked = true;
                    v.speed = 0;
                    v.field = null;
                    fleet.driving.pullOver(v);
                    fleet.crews.getOut(v, false);
                    return false;
                }
            } else {
                float[] exit = fleet.routing.exitNear(v.x, v.y);
                if (exit != null && fleet.routing.route(v, exit[0], exit[1])) v.fleeing = true;
            }
        }
        float limit = fleet.driving.junctionRule(v, dt);
        boolean person = w.personAhead(v.x, v.y, v.angle) || fleet.trains.trainComing(v);
        if ((person && v.timer < 2.5f) || (v.waitTimer > 0 && !zombiesClose) || fleet.trains.trainComing(v)) {
            // Brake for pedestrians (then creep through, nudging them aside) or wait for someone to get in.
            if (v.speed > 45 && person) w.skid(v.x, v.y, v.angle, 6);
            v.speed = Math.max(0, v.speed - dt * 200);
            if (!person) v.timer = 0;
        } else {
            if (!person) v.timer = 0;
            v.stuckTimer += dt;
            boolean queue = fleet.driving.carAhead(v);
            float throttle = queue && v.stuckTimer < 3 ? 0.15f : 1f;
            // Waiting at a red light, a stop sign or behind a queue isn't being stuck.
            if ((v.held || queue) && !fleet.routing.gridlocked(v)) v.stuckTimer = Math.min(v.stuckTimer, 1);
            // (On the highway: highway speeds.)
            // (10.49) A slip road on or off the highway at nearly highway speed, not a town crawl.
            float cruise = city.onHighway(v.x, v.y) ? 115 : city.onSlip(v.x, v.y) || city.onBelt(v.x, v.y) ? 100 : v.fleeing ? 90 : 70;
            boolean going = fleet.driving.driveStep(v, dt, Math.min(limit, zombiesClose ? 60 : cruise), throttle);
            // A panicking driver who's gone round in a circle gives up on that way out and goes somewhere else.
            if (v.orbited) {
                v.orbited = false;
                if (v.fleeing) {
                    v.fleeing = false;
                    v.fleeCd = 20;
                    if (!fleet.routing.newDestination(v)) v.stuckTimer = Math.max(v.stuckTimer, 5);
                } else if (!v.through && v.riders.isEmpty() && v.evacStop < 0) {
                    // ((11.5) And anyone else just driving about: somewhere else.)
                    v.destCd = 0;
                    fleet.routing.newDestination(v);
                }
            }
            if (!going) {
                fleet.service.arrive(v);
                // Out of town (where nobody sees it go, or off the edge of the map): gone. (Waiting at the edge
                // in sight, they used to block the way out for everyone behind.)
                float edge = 3 * City.T;
                boolean atEdge = v.x < edge || v.y < edge || v.x > city.worldW() - edge || v.y > city.worldH() - edge;
                if ((v.fleeing || v.through) && (!fleet.routing.inView(v.x, v.y) || atEdge)) {
                    fleet.service.dropRiders(v, false);
                    return true;
                }
                if (v.fleeing || v.through) {
                    // (At the edge of town, in sight: pull in and wait.)
                    fleet.service.dropRiders(v, false);
                    v.parked = true;
                    v.speed = 0;
                    v.field = null;
                    fleet.driving.pullOver(v);
                    return false;
                }
                // (Nowhere yet: wait a moment and look again before giving up.)
                if (!fleet.routing.newDestination(v)) v.stuckTimer = Math.max(v.stuckTimer, 5);
            } else {
                fleet.driving.hit(v, w.runOver(v.x, v.y, 8, v.speed, v.angle));
                if (v.broken) return false;
            }
        }
        if (v.stuckTimer > 8 && !zombiesClose && v.destCd <= 0) {
            v.stuckTimer = 0;
            if (!fleet.routing.newDestination(v)) {
                if (!fleet.routing.inView(v.x, v.y)) {
                    fleet.service.dropRiders(v, false);
                    return true;
                }
                // Nowhere to go: pull in and park (it's towed later, out of sight).
                fleet.service.dropRiders(v, false);
                v.parked = true;
                v.speed = 0;
                v.field = null;
                fleet.driving.pullOver(v);
            }
        }
        // Surrounded: the driver gets out and runs.
        if (zombiesClose && v.speed < 12) {
            v.parked = true;
            v.state = ABANDONED;
            v.speed = 0;
            v.field = null;
            fleet.driving.pullOver(v);
            fleet.crews.getOut(v, true);
            fleet.service.dropRiders(v, true);
        }
        return false;
    }
}
