package com.instantaxtion.zombiesandbox;

import static com.instantaxtion.zombiesandbox.Fleet.*;

import java.util.ArrayList;

/**
 * ((27.3) Part of the vehicle code, out of Fleet.) Who's in which vehicle: boarding, getting out, being dragged out, drivers, crews on foot and the vehicles that go back for them.
 */
final class FleetCrews {
    final Fleet fleet;
    final World w;
    final City city;

    FleetCrews(Fleet fleet) {
        this.fleet = fleet;
        this.w = fleet.w;
        this.city = fleet.city;
    }

    /** Called by World when someone climbs in. */
    void boarded(Vehicle v, Entity e) {
        v.riders.add(e);
        if (v.rescue || v.type != CAR) return;
        // Take them to a safe zone with room if there is one.
        Dispatch.SafeZone best = null;
        float bd = Float.MAX_VALUE;
        for (int i = 0; i < w.dispatch.zones.size(); i++) {
            Dispatch.SafeZone z = w.dispatch.zones.get(i);
            if (z.full) continue;
            float d = (z.x - v.x) * (z.x - v.x) + (z.y - v.y) * (z.y - v.y);
            if (d < bd) {
                bd = d;
                best = z;
            }
        }
        if (best != null && fleet.routing.route(v, best.x, best.y)) {
            v.rescue = true;
            v.zone = best;
        }
    }

    /**
     * ((26.0) The dead have a window in: one of those aboard - a rider (never you), one of the crew, one of the squad
     * it's carrying, the driver - is dragged out at (x, y). Who it was, or null if there was nobody they could reach.
     */
    Entity dragOut(Vehicle v, float x, float y) {
        for (int i = v.riders.size() - 1; i >= 0; i--) {
            Entity e = v.riders.get(i);
            if (e == v.player || e == w.controlled) continue;
            if (!w.release(e, x, y)) return null;
            v.riders.remove(i);
            return e;
        }
        if (!v.crew.isEmpty()) {
            Entity e = v.crew.get(v.crew.size() - 1);
            if (!w.release(e, x, y)) return null;
            v.crew.remove(v.crew.size() - 1);
            // (Still its crew, on foot.)
            e.rig = v;
            note(e);
            e.taskTimer = 0;
            return e;
        }
        if (v.passengers > 0 && (v.type == CRUISER || v.type == TRUCK)) {
            Entity e = w.spawn(v.passengerType, x, y);
            if (e == null) return null;
            v.passengers--;
            if (v.swat || v.kind == K_BEARCAT) w.applyRole(e, Entity.ROLE_SWAT);
            else if (v.guardUnit) w.applyRole(e, Entity.ROLE_GUARD);
            else if (v.passengerType == Entity.COP && v.agency > 0) w.makeAgency(e, v.agency);
            v.squad.add(e);
            return e;
        }
        if (v.driver != null && (v.type == CRUISER || v.type == TRUCK)) {
            Entity d = v.driver, e = d;
            if (v.realDriver) {
                if (!w.release(d, x, y)) return null;
                v.realDriver = false;
                d.rig = null;
            } else {
                e = w.spawn(d.type, x, y);
                if (e == null) return null;
                e.nameSeed = d.nameSeed;
                if (d.agency > 0) w.makeAgency(e, d.agency);
                if (d.role == Entity.ROLE_GUARD) w.applyRole(e, Entity.ROLE_GUARD);
            }
            // (Left where it is with nobody in it, for someone to take back later: see reclaim.)
            v.driver = null;
            v.parked = true;
            v.state = ABANDONED;
            v.speed = 0;
            v.field = null;
            return e;
        }
        // The driver of an everyday car, stopped in traffic: the car's left where it is.
        if (v.type == CAR && !v.parked && v.state == CRUISE && v.model != M_BUS) {
            Entity e = w.spawn(Entity.CIVILIAN, x, y);
            if (e == null) return null;
            v.parked = true;
            v.state = ABANDONED;
            v.speed = 0;
            v.field = null;
            fleet.driving.pullOver(v);
            return e;
        }
        return null;
    }

    void getOut(Vehicle v, boolean panic) {
        Entity driver = w.spawn(Entity.CIVILIAN, v.x + (float) Math.cos(v.angle + 1.57f) * 9,
                v.y + (float) Math.sin(v.angle + 1.57f) * 9);
        if (driver != null && panic) {
            driver.fleeTimer = 3;
            driver.threatX = v.x;
            driver.threatY = v.y;
        }
    }

    void unload(Vehicle v, boolean announce) {
        w.arriving = v.reinforcement || v.guardUnit;
        try {
            unloadAll(v, announce);
        } finally {
            w.arriving = false;
        }
    }

    void unloadAll(Vehicle v, boolean announce) {
        Entity first = null;
        for (int i = 0; i < v.passengers; i++) {
            Entity e = w.spawn(v.passengerType, v.x + (float) Math.cos(v.angle + 1.57f) * (8 + i * 3),
                    v.y + (float) Math.sin(v.angle + 1.57f) * (8 + i * 3));
            if (e == null) continue;
            if (first == null) first = e;
            v.squad.add(e);
            if (v.guardUnit) w.applyRole(e, Entity.ROLE_GUARD);
            if (v.kind == K_RIOT_VAN) w.applyRole(e, Entity.ROLE_RIOT);
            if (v.kind == K_BEARCAT) w.applyRole(e, Entity.ROLE_SWAT);
            if (v.type == BOAT) w.applyRole(e, Entity.ROLE_MARINE);
            // (Each SWAT team has a marksman with it.)
            if (v.swat) w.applyRole(e, i == v.passengers - 1 && city.cfg.nature() ? Entity.ROLE_MARKSMAN : Entity.ROLE_SWAT);
            else if (v.passengerType == Entity.SOLDIER && !v.guardUnit && v.type != BOAT && city.cfg.nature()) w.kitOut(e);
            else if (v.passengerType == Entity.COP && v.agency > 0) w.makeAgency(e, v.agency);
            if (v.reinforcement) {
                if (!v.guardUnit && !v.restock) e.outsider = true;
                w.reinforce(e);
                w.dispatch.convoyUnloaded(v, e);
            }
            if (v.incident != null && !v.incident.resolved) {
                e.task = Dispatch.T_RESPOND;
                e.incident = v.incident;
            } else if (v.zone != null && !v.zone.removed) {
                e.task = Dispatch.T_GUARD;
                e.zone = v.zone;
            }
        }
        if (first == null) return;
        int who = v.passengerType == Entity.SOLDIER ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE;
        if (announce && v.place != null)
            w.dispatch.chatter(who, first, "On the ground at " + v.place + ". Moving in.", v.x, v.y);
        else if (!announce)
            w.dispatch.say(who, first, (v.type == TRUCK ? "Our truck is wrecked" : "Our car is wrecked")
                    + " on " + city.placeName(v.x, v.y) + ". Continuing on foot.", v.x, v.y);
    }

    // ------------------------------------------------------------------ fire engines

    // ------------------------------------------------------------------ crews

    /** Someone climbs aboard (out of the world until they get out again). */
    void board(Vehicle v, Entity e) {
        String what = vehicleWord(v);
        w.history(e, ("aeiou".indexOf(what.charAt(0)) >= 0 ? "got into an " : "got into a ") + what);
        e.task = Dispatch.T_NONE;
        e.rig = v; note(e);
        e.dead = true;
        e.removed = true;
        e.vx = e.vy = 0;
        v.crew.add(e);
    }

    /** Everyone aboard gets out beside it. They stay its crew (unless it's home: then they're off duty). */
    /**
     * (12.0) Takes a vehicle off the map for good (a wreck towed away, one you undo): whoever was still in it gets
     * out first, and anything still tied to it knows it's gone.
     */
    void discard(Vehicle v) {
        if (!v.crew.isEmpty()) crewOut(v, true);
        if (v.driver != null && v.realDriver) driverOut(v, false);
        v.driver = null;
        v.removedFromFleet = true;
        fleet.vehicles.remove(v);
    }

    /** ((27.5) Just what it is ("police car", "armoured personnel carrier"), for a card's history. */
    String vehicleWord(Vehicle v) {
        String d = fleet.describe(v);
        int cut = d.indexOf("  -  ");
        return (cut > 0 ? d.substring(0, cut) : d).toLowerCase(java.util.Locale.ROOT);
    }

    void crewOut(Vehicle v, boolean offDuty) {
        for (int i = 0; i < v.crew.size(); i++) {
            Entity e = v.crew.get(i);
            float a = v.angle + (i % 2 == 0 ? 1.57f : -1.57f);
            if (!w.release(e, v.x + (float) Math.cos(a) * (10 + i * 3), v.y + (float) Math.sin(a) * (10 + i * 3))) continue;
            // ((27.4) A job that was getting into this vehicle is over once they're out of it.)
            if (e.owner != null && e.owner.rig == v) e.owner = null;
            e.rig = offDuty ? null : v;
            if (!offDuty) note(e);
            w.history(e, "got out of the " + vehicleWord(v));
            e.taskTimer = 0;
        }
        v.crew.clear();
    }

    /**
     * (11.3) Every police car, army truck and ambulance on the road has someone driving it: one with nobody
     * aboard gets its driver (an officer, a soldier or a paramedic), who stays with it until it's parked at
     * home again.
     */
    void drivers() {
        for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (v.type != CRUISER && v.type != TRUCK && v.type != AMBULANCE) continue;
            if (v.removedFromFleet) {
                if (v.driver != null && v.realDriver) driverOut(v, false);
                v.driver = null;
                continue;
            }
            if (v.broken) {
                if (v.driver != null) driverOut(v, true);
                continue;
            }
            boolean home = v.parked || v.standby || v.state == ABANDONED || v.player != null;
            if (home) {
                // ((11.7) One of the station's own goes back inside; a driver from outside goes with the vehicle.)
                if (v.driver != null && v.realDriver) {
                    float a = v.angle - 1.57f;
                    w.release(v.driver, v.x + (float) Math.cos(a) * 10, v.y + (float) Math.sin(a) * 10);
                    v.driver.rig = null;
                }
                v.driver = null;
                v.realDriver = false;
                continue;
            }
            // ((11.7) Waiting at the station for its crew to climb in: whoever gets in first drives.)
            // ((26.0) Nor the command post set up at the scene: nobody needs to sit at its wheel.)
            // ((26.1) Nor one parked in the yard at home.)
            if (v.state == MUSTER || (isPost(v) && (v.state == SCENE || v.state == WAIT))) continue;
            // ((28.3) Stopped at its call with its own people out on foot round it - a patrol car's crew, the squad it
            // brought: nobody sits in it. A driver was made up for every patrol car at a call the moment its crew got
            // out, and sat there out of the fight till they came back; a real one at the wheel gets out and joins them.)
            if ((v.state == SCENE || v.state == RECALL) && v.crew.isEmpty() && (crewOnFoot(v) > 0 || squadAbout(v))) {
                if (v.driver != null) {
                    if (v.realDriver) joinCrewOnFoot(v);
                    else v.driver = null;
                }
                continue;
            }
            // ((12.0) Its own crew aboard: one of them is driving, not an extra person nobody sent.)
            if (v.driver != null && !v.realDriver && !v.crew.isEmpty()) v.driver = null;
            if (v.driver == null && v.crew.isEmpty() && v.riders.isEmpty() && v.passengers == 0) {
                int type = v.type == AMBULANCE ? (v.kind == K_ARMD_AMB ? Entity.SOLDIER : Entity.CIVILIAN) : v.type == CRUISER || policeTruck(v) ? Entity.COP : Entity.SOLDIER;
                // ((12.0) A driver from outside isn't on the city's roll: they used to take a callsign or a place in a
                // squad, so the officers and squads after them were numbered with gaps and squads came up short.)
                int cops = w.dispatch.copCount, soldiers = w.dispatch.soldierCount;
                Entity d = w.create(type, v.x, v.y);
                w.dispatch.copCount = cops;
                w.dispatch.soldierCount = soldiers;
                if (type == Entity.COP && v.agency > 0) w.makeAgency(d, v.agency);
                if (v.guardUnit) w.applyRole(d, Entity.ROLE_GUARD);
                d.rig = v; note(d);
                v.driver = d;
            }
        }
    }

    /** ((28.3) Someone it brought still on their feet near it. */
    private boolean squadAbout(Vehicle v) {
        for (int i = 0; i < v.squad.size(); i++) {
            Entity e = v.squad.get(i);
            if (!e.dead && !e.removed && World.hypot(e.x - v.x, e.y - v.y) < 500) return true;
        }
        return false;
    }

    /** ((28.3) The driver gets out and is one of its crew on foot, on its call like the rest; back in when it goes. */
    private void joinCrewOnFoot(Vehicle v) {
        Entity d = v.driver;
        driverOut(v, false);
        if (d == null || d.dead || d.removed) return;
        d.rig = v;
        note(d);
        w.history(d, "got out of the " + vehicleWord(v));
        if (v.incident != null && !v.incident.resolved) {
            d.task = Dispatch.T_RESPOND;
            d.incident = v.incident;
        }
    }

    /** (11.3) The driver of a wrecked vehicle gets out and carries on on foot. */
    /** The driver gets out: shaken and hurt if it's been wrecked. */
    void driverOut(Vehicle v, boolean hurt) {
        Entity d = v.driver;
        v.driver = null;
        float a = v.angle - 1.57f;
        if (v.realDriver) {
            v.realDriver = false;
            d.rig = null;
            // ((12.0) Only hurt in a wreck: one just getting out at the end of the trip lost a third of their health.)
            if (w.release(d, v.x + (float) Math.cos(a) * 10, v.y + (float) Math.sin(a) * 10) && hurt) d.hp *= 0.7f;
            return;
        }
        Entity e = w.spawn(d.type, v.x + (float) Math.cos(a) * 10, v.y + (float) Math.sin(a) * 10);
        if (e == null) return;
        e.nameSeed = d.nameSeed;
        if (d.agency > 0) w.makeAgency(e, d.agency);
        if (d.role == Entity.ROLE_GUARD) w.applyRole(e, Entity.ROLE_GUARD);
        e.hp *= 0.7f;
    }

    /**
     * (10.48) A police car or army truck left with nobody to drive it - its crew dead, or gone off elsewhere -
     * is taken over by an officer (or soldier) standing about near it with nothing to do, who picks up the others
     * nearby on foot as well: a patrol car goes back on patrol with them, anything else takes them back to the
     * station or base (from where they're sent out again).
     */
    void reclaim() {
        // (How long each has been standing about.)
        for (int k = 0, n = w.entities.size(); k < n; k++) {
            Entity e = w.entities.get(k);
            if (e.type != Entity.COP && e.type != Entity.SOLDIER) continue;
            if (strandedUnit(e, e.type)) e.strandedFor += 3;
            else if (e.strandedFor > 0) e.strandedFor = 0;
        }
        fetchStranded();
        for (int i = 0; i < fleet.vehicles.size(); i++) {
            Vehicle v = fleet.vehicles.get(i);
            if ((v.type != CRUISER && v.type != TRUCK) || v.broken || v.burnt || v.player != null || v.standby || v.removedFromFleet
                    || !v.crew.isEmpty() || v.block != null || v.zone != null || v.loop || v.shelter || v.leaving || v.state == PICKUP || v.state == MUSTER) continue;
            if (!v.parked && v.state != ABANDONED) continue;
            v.reclaimCd -= 3;
            if (v.reclaimCd > 0 || crewOnFoot(v) > 0 || w.countZombiesNear(v.x, v.y, 120) > 0) continue;
            int type = v.type == CRUISER ? Entity.COP : Entity.SOLDIER;
            // ((11.3) A patrol car can be taken by officers on the beat: they carry on patrolling in it. Not
            // otherwise - that would only take them back to the station.)
            boolean beat = v.type == CRUISER && v.patrol;
            // Someone free close by...
            Entity first = null;
            float bd = 350;
            for (int k = 0, n = w.entities.size(); k < n; k++) {
                Entity e = w.entities.get(k);
                if (!(beat ? freeUnit(e, type) : strandedUnit(e, type))) continue;
                float d = (float) World.hypot(e.x - v.x, e.y - v.y);
                if (d < bd) {
                    bd = d;
                    first = e;
                }
            }
            if (first == null) continue;
            // ... and the others free nearby, as many as it holds.
            int seats = v.type == CRUISER ? SEATS : 10;
            ArrayList<Entity> group = new ArrayList<Entity>();
            group.add(first);
            for (int k = 0, n = w.entities.size(); k < n && group.size() < seats; k++) {
                Entity e = w.entities.get(k);
                if (e != first && (beat ? freeUnit(e, type) : strandedUnit(e, type)) && World.hypot(e.x - v.x, e.y - v.y) < 600) group.add(e);
            }
            v.reclaimCd = 30;
            v.wasPatrol = v.patrol;
            v.patrol = false;
            v.parked = false;
            v.state = PICKUP;
            v.pickup = true;
            v.timer = 0;
            v.speed = 0;
            v.incident = null;
            v.squad.clear();
            v.squad.addAll(group);
            v.passengerType = type;
            for (Entity e : group) {
                e.rig = v; note(e);
                e.task = Dispatch.T_BOARD;
            }
            w.dispatch.chatter(type == Entity.COP ? Dispatch.WHO_POLICE : Dispatch.WHO_MILITARY, first, (v.type == CRUISER ? "Taking this car" : "Taking this truck")
                    + " on " + city.placeName(v.x, v.y) + (group.size() > 1 ? ", picking up " + (group.size() - 1) + " more." : "."), v.x, v.y);
        }
    }

    /**
     * (10.48) Officers or soldiers left on foot with nothing to do, a long way from their station or base and no
     * empty car or truck near them: one parked there drives out to fetch them.
     */
    void fetchStranded() {
        for (int t = 0; t < 2; t++) {
            int type = t == 0 ? Entity.COP : Entity.SOLDIER;
            for (int k = 0, n = w.entities.size(); k < n; k++) {
                Entity e = w.entities.get(k);
                // ((11.0) Sooner, and from nearer the station, than before: units were left standing about for long.)
                if (!strandedUnit(e, type) || e.strandedFor < 9 || w.countZombiesNear(e.x, e.y, 250) > 0) continue;
                City.Facility f = city.nearestFacility(type == Entity.COP ? City.FACILITY_POLICE : City.FACILITY_BASE, e.x, e.y);
                if (f == null || World.hypot(f.x - e.x, f.y - e.y) < 600) continue;
                // One parked at the station or base.
                Vehicle v = null;
                for (int i = 0; i < fleet.vehicles.size() && v == null; i++) {
                    Vehicle o = fleet.vehicles.get(i);
                    if (o.standby && o.type == (type == Entity.COP ? CRUISER : TRUCK) && !o.broken && o.kind != K_BIKE
                            && World.hypot(o.x - f.x, o.y - f.y) < f.r + 300) v = o;
                }
                // ((11.7) None there, or nobody there to drive it: they make their own way back.)
                if (v == null || fleet.armyUnits.freeDriver(f, type, null) == null) continue;
                ArrayList<Entity> group = new ArrayList<Entity>();
                for (int j = 0; j < n && group.size() < (v.type == CRUISER ? SEATS : 10); j++) {
                    Entity o = w.entities.get(j);
                    if (strandedUnit(o, type) && World.hypot(o.x - e.x, o.y - e.y) < 400) group.add(o);
                }
                float[] spot = city.nearestDrivable(e.x, e.y);
                if (spot == null) continue;
                v.parked = false;
                v.pulling = false;
                v.standby = false;
                v.pickup = true;
                if (!fleet.routing.route(v, spot[0], spot[1])) {
                    v.parked = true;
                    v.standby = true;
                    v.pickup = false;
                    continue;
                }
                v.squad.clear();
                v.squad.addAll(group);
                v.passengerType = type;
                v.state = DRIVE;
                v.stuckTimer = 0;
                v.place = city.placeName(spot[0], spot[1]);
                fleet.armyUnits.seatDriver(v, fleet.armyUnits.freeDriver(f, type, group));
                // (They wait for it where they are.)
                for (Entity o : group) o.strandedFor = -90;
                // (Not on the radio every time: one in a while is enough to know it's happening.)
                if (w.time > fleet.fetchSaidAt) {
                    fleet.fetchSaidAt = w.time + 25;
                    w.dispatch.say(type == Entity.COP ? Dispatch.WHO_POLICE : Dispatch.WHO_MILITARY, e,
                        (v.type == TRUCK ? "Truck" : "Car") + " coming out to " + v.place + " to pick up " + (type == Entity.COP ? World.count(group.size(), "officer", "officers") : World.count(group.size(), "soldier", "soldiers")) + ".", v.x, v.y);
                }
                return;
            }
        }
    }

    /** An officer or soldier on foot with no car of their own and nothing to do. */
    boolean strandedUnit(Entity e, int type) {
        return freeUnit(e, type) && !w.onDuty(e);
    }

    /** (11.3) Free to take over a car: no call to answer (an officer on the beat counts). */
    boolean freeUnit(Entity e, int type) {
        // (Not the park rangers, who keep to the wilds on foot.)
        return !e.dead && !e.removed && e.type == type && e.rig == null && e.task == Dispatch.T_NONE && !e.aiming && e != w.controlled
                && e.leader == null && e.role != Entity.ROLE_K9 && e.agency != 3;
    }

    /** Calls the crew on foot back aboard. Returns how many are still out. */
    int recall(Vehicle v) {
        int out = 0;
        ArrayList<Entity> mine = onFoot(v);
        for (int i = 0; i < mine.size(); i++) {
            Entity e = mine.get(i);
            if (e.dead || e.rig != v) continue;
            out++;
            if (e.task != Dispatch.T_BOARD) {
                w.dispatch.releaseForResupply(e);
                e.task = Dispatch.T_BOARD;
            }
        }
        return out;
    }

    /** Crew members still on foot. */
    int crewOnFoot(Vehicle v) {
        int out = 0;
        ArrayList<Entity> mine = onFoot(v);
        for (int i = 0; i < mine.size(); i++) {
            Entity e = mine.get(i);
            if (!e.dead && e.rig == v) out++;
        }
        return out;
    }

    /**
     * (12.0) Those out of v who belong to it (its crew on foot), as they were when first asked this frame: callers
     * still check each one's rig, as someone sent off since will have had theirs cleared.
     */
    ArrayList<Entity> onFoot(Vehicle v) {
        if (fleet.rigFrame != fleet.frameNo) {
            fleet.rigFrame = fleet.frameNo;
            // ((22.3) From the list of those given a vehicle, not by looking through everyone in the city each step:
            // that was a thirtieth of each step in an outbreak. The whole city is still looked through once a second
            // for anyone the list missed.)
            if (fleet.frameNo % 30 == 0)
                for (int i = 0, n = w.entities.size(); i < n; i++) {
                    Entity e = w.entities.get(i);
                    if (!e.dead && e.rig != null) note(e);
                }
            for (int i = fleet.rigged.size() - 1; i >= 0; i--) {
                Entity e = fleet.rigged.get(i);
                Vehicle r = e.rig;
                if (r == null || (e.dead && (!e.removed || !r.crew.contains(e)))) {
                    e.footGen = 0;
                    int last = fleet.rigged.size() - 1;
                    fleet.rigged.set(i, fleet.rigged.get(last));
                    fleet.rigged.remove(last);
                    continue;
                }
                if (e.dead) continue;
                if (r.footFrame != fleet.frameNo) {
                    r.footFrame = fleet.frameNo;
                    r.foot.clear();
                }
                r.foot.add(e);
            }
        }
        return v.footFrame == fleet.frameNo ? v.foot : NOBODY;
    }

    void note(Entity e) {
        if (e.footGen == 1) return;
        e.footGen = 1;
        fleet.rigged.add(e);
    }

    /** People aboard vehicles, by type (they still count). */
    void countCrews(int[] counts) {
        for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
            Vehicle v = fleet.vehicles.get(i);
            ArrayList<Entity> c = v.crew;
            for (int k = 0; k < c.size(); k++) counts[c.get(k).type]++;
            // ((15.9) And the police and soldiers riding along, and reinforcements still aboard the trucks, cars,
            // boats and helicopters bringing them in: they weren't counted till they climbed out.)
            for (int k = 0; k < v.riders.size(); k++) {
                int t = v.riders.get(k).type;
                if (t == Entity.COP || t == Entity.SOLDIER) counts[t]++;
            }
            if (v.passengers > 0 && (v.passengerType == Entity.COP || v.passengerType == Entity.SOLDIER) && !v.removedFromFleet)
                counts[v.passengerType] += v.passengers;
        }
    }
}
