package com.instantaxtion.zombiesandbox;

import static com.instantaxtion.zombiesandbox.Fleet.*;

import java.util.ArrayList;

/**
 * ((27.3) Part of the vehicle code, out of Fleet.) Police cars and army trucks sent to a call (and the police's vans and BearCats): driving there, arriving, standing by, picking their people up again, heading home and parking - updateCar - and wrecks.
 */
final class FleetService {
    final Fleet fleet;
    final World w;
    final City city;

    FleetService(Fleet fleet) {
        this.fleet = fleet;
        this.w = fleet.w;
        this.city = fleet.city;
    }

    /**
     * Sends units by road: cops in cruisers (two per car), soldiers in one truck. Returns false if there is
     * no road route, in which case the caller lets them walk.
     */
    boolean send(int unitType, int count, float fromX, float fromY, float toX, float toY, Dispatch.Incident inc,
                 Dispatch.SafeZone zone, String place) {
        float[] start = null;
        // An army truck from a base or the armory: one of the trucks parked there drives out.
        // (10.24) First one of the cars or trucks parked back at the station or base after an earlier call.
        if (fleet.fromParked) start = takeStandby(unitType == Entity.COP ? CRUISER : TRUCK, fromX, fromY, 300);
        if (start == null && unitType == Entity.SOLDIER && fleet.fromParked) start = city.takeParkedTruck(fromX, fromY, 260);
        if (start == null) start = city.nearestDrivable(fromX, fromY);
        if (start == null) return false;
        int[] full = fleet.routing.scratchField();
        // ((27.4) Round the dead on the way, to the call.)
        fleet.routing.hazardOn(w.zombies > 0 ? 100 : 0, toX, toY, FleetRouting.HAZ_CALL);
        boolean way;
        try {
            way = city.driveField(full, toX, toY, false, city.tileIndex(start[0], start[1]));
        } finally {
            fleet.routing.hazardOff();
        }
        if (!way || full[city.tileIndex(start[0], start[1])] >= City.FAR) return false;
        Route field = fleet.routing.corridor(full, start[0], start[1]);
        boolean cops = unitType == Entity.COP;
        int cars = cops ? (count + 1) / 2 : 1;
        for (int i = 0; i < cars; i++) {
            Vehicle v = fleet.make(cops ? CRUISER : TRUCK);
            v.state = WAIT;
            v.timer = 1.5f + i * 1.2f;
            v.x = start[0];
            v.y = start[1];
            v.homeX = start[0];
            v.homeY = start[1];
            v.tx = toX;
            v.ty = toY;
            v.field = field;
            v.passengerType = unitType;
            v.passengers = cops ? Math.min(2, count - i * 2) : count;
            v.incident = inc;
            v.zone = zone;
            v.place = place;
            v.lastDist = Float.MAX_VALUE;
            v.spin = 0;
            fleet.vehicles.add(v);
        }
        return true;
    }

    /**
     * The SWAT van: one armoured truck carrying the team to an incident. ((12.9) The team standing by at the station,
     * who climb in at the gate: before, six officers appeared out of the van when it got there.)
     */
    boolean sendSwat(City.Facility station, Dispatch.Incident inc) {
        java.util.List<Entity> team = fleet.armyUnits.standby(station, Entity.COP, Entity.ROLE_SWAT, 6);
        if (team.size() < 2 || !fleet.routing.canDrive(station.gateX, station.gateY, inc.x, inc.y)) return false;
        Vehicle v = fleet.armyUnits.musterUnit(team, station, inc, 0, Entity.COP);
        if (v == null) return false;
        v.swat = true;
        return true;
    }

    /** (11.0) A BearCat: an armoured police truck with its tactical officers, for the worst of it; it drives on through. */
    boolean sendBearCat(City.Facility station, Dispatch.Incident inc) {
        java.util.List<Entity> team = fleet.armyUnits.standby(station, Entity.COP, Entity.ROLE_SWAT, 6);
        if (team.size() < 2 || !fleet.routing.canDrive(station.gateX, station.gateY, inc.x, inc.y)) return false;
        return fleet.armyUnits.musterUnit(team, station, inc, K_BEARCAT, Entity.COP) != null;
    }

    /**
     * (11.1) The Place tool's BearCat: six tactical officers in their armoured truck, off from where it's put to
     * the worst call going (or out on patrol if there isn't one).
     */
    Vehicle placeBearCat(float x, float y) {
        Dispatch.Incident worst = null;
        for (int i = 0; i < w.dispatch.incidents.size(); i++) {
            Dispatch.Incident inc = w.dispatch.incidents.get(i);
            if (!inc.resolved && (worst == null || inc.zombiesNear > worst.zombiesNear)) worst = inc;
        }
        float[] d = worst != null ? new float[]{worst.x, worst.y} : fleet.routing.randomRoad();
        if (d == null) return null;
        int before = fleet.vehicles.size();
        fleet.fromParked = false;
        boolean sent = send(Entity.SOLDIER, 6, x, y, d[0], d[1], worst, null, city.placeName(d[0], d[1]));
        fleet.fromParked = true;
        if (!sent || fleet.vehicles.size() == before) return null;
        Vehicle v = fleet.vehicles.get(fleet.vehicles.size() - 1);
        fleet.setKind(v, K_BEARCAT);
        v.passengerType = Entity.COP;
        v.timer = 0.3f;
        return v;
    }

    /**
     * ((22.0) The Place tool's armoured police van: parked where it's put, doors open. People running from the dead
     * nearby climb in, a dozen at most, and are safe inside until it's wrecked - or until someone drives them away.
     */
    Vehicle placeShelterVan(float x, float y) {
        float[] p = city.nearestDrivable(x, y);
        if (p == null) return null;
        Vehicle v = fleet.make(TRUCK);
        fleet.setKind(v, K_RIOT_VAN);
        v.hp = v.maxHp = 700;
        v.x = p[0];
        v.y = p[1];
        v.angle = w.rnd.nextInt(4) * (float) Math.PI / 2;
        v.shelter = true;
        v.parked = true;
        v.state = IDLE;
        v.speed = 0;
        v.number = ++fleet.unitCount;
        fleet.vehicles.add(v);
        fleet.shelters.add(v);
        return v;
    }

    /** The riot van: six riot officers with shields, from the precinct to a big crowd of the dead. */
    boolean sendRiotVan(City.Facility station, Dispatch.Incident inc) {
        // ((12.9) The riot squad standing by at the station, not six who appeared when the van got there.)
        java.util.List<Entity> squad = fleet.armyUnits.standby(station, Entity.COP, Entity.ROLE_RIOT, 6);
        if (squad.size() < 2 || !fleet.routing.canDrive(station.gateX, station.gateY, inc.x, inc.y)) return false;
        return fleet.armyUnits.musterUnit(squad, station, inc, K_RIOT_VAN, Entity.COP) != null;
    }

    boolean updateWreck(Vehicle v, float dt) {
        v.speed = 0;
        if (v.standby && !v.broken) {
            if (v.pulling) {
                float dx = v.pullX - v.x, dy = v.pullY - v.y, d = (float) Math.sqrt(dx * dx + dy * dy);
                if (d < 0.5f) v.pulling = false;
                else {
                    float step = Math.min(d, 20 * dt);
                    v.x += dx / d * step;
                    v.y += dy / d * step;
                }
            }
            return standbyStep(v, dt);
        }
        if (v.block != null && !v.broken && v.block.removed) {
            // The zone has closed: the officer gets back in, the cones come in and the car drives off.
            v.block = null;
            v.cones = false;
            v.barHp = 0;
            v.barX0 = v.barX1 = v.barY0 = v.barY1 = 0;
            if (v.guard != null && !v.guard.dead && World.hypot(v.guard.x - v.x, v.guard.y - v.y) < 80) {
                v.guard.dead = true;
                v.guard.removed = true;
            }
            v.guard = null;
            v.parked = false;
            v.state = RETURN;
            float[] home = fleet.routing.randomRoad();
            return home == null || !fleet.routing.route(v, home[0], home[1]);
        }
        // ((14.8) Its crowd barriers holding the dead back.)
        if (v.block != null && v.barHp > 0) w.barrierHold(v, dt);
        // ((11.3) A car left parked out in the road - stopped where it was, its driver gone - is put by the kerb
        // first, out of the lanes.)
        if (v.type == CAR && !v.broken && !v.pulling && !v.pullTried && v.player == null) {
            int t = city.tileIndex(v.x, v.y);
            if (t >= 0 && city.tiles[t] == City.ROAD && !city.junctionAt((int) (v.x / City.T), (int) (v.y / City.T))) fleet.driving.pullOver(v);
            v.pullTried = true;
        }
        if (v.pulling) {
            // Rolling to a stop against the kerb.
            float dx = v.pullX - v.x, dy = v.pullY - v.y, d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d < 0.5f) v.pulling = false;
            else {
                float step = Math.min(d, 20 * dt);
                v.x += dx / d * step;
                v.y += dy / d * step;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ driving

    boolean updateCar(Vehicle v, float dt) {
        if (!v.parked) v.pullTried = false;
        // ((27.5) Or out for the wounded (see FleetMedevac).)
        if (v.type == AMBULANCE) return v.medevac ? fleet.medevac.step(v, dt) : fleet.ambulances.ambulanceStep(v, dt);
        if (v.kind == K_DOZER && v.state == SCENE) return fleet.armyUnits.dozerPush(v, dt);
        // ((14.0) Close, and the traffic rules won't take it the last bit up to a stopped vehicle: it goes straight in.)
        if (v.kind == K_DOZER && v.state == DRIVE && v.clearing != null && !v.clearing.removedFromFleet
                && World.hypot(v.clearing.x - v.x, v.clearing.y - v.y) < 110 && (Math.abs(v.speed) < 6 || World.hypot(v.clearing.x - v.x, v.clearing.y - v.y) < 30)) {
            v.state = SCENE;
            v.timer = DOZER_PUSH;
            v.speed = 0;
            return false;
        }
        if (v.recon) return fleet.armyUnits.reconStep(v, dt);
        if (isPost(v)) return fleet.commandTrucks.commandPostStep(v, dt);
        if (v.kind == K_FIRE_SUPPORT) return fleet.fireSupport.step(v, dt);
        if (v.type == TRUCK) fleet.turret(v, dt);
        if (v.state == PICKUP) return pickupStep(v, dt);
        if (v.state == SCENE && v.type == CRUISER) return cruiserAtScene(v, dt);
        // ((11.7) Out to pick people up: it heads for where they are now, not where they were when it set off.)
        if (v.state == DRIVE && v.pickup && !v.squad.isEmpty() && (v.aimCd -= dt) <= 0) {
            v.aimCd = 3;
            float sx = 0, sy = 0;
            int n = 0;
            for (int i = 0; i < v.squad.size(); i++) {
                Entity e = v.squad.get(i);
                if (e.dead || e.removed || e.rig != v) continue;
                sx += e.x;
                sy += e.y;
                n++;
            }
            if (n > 0) {
                sx /= n;
                sy /= n;
                float[] spot = city.nearestDrivable(sx, sy);
                if (spot != null && World.hypot(spot[0] - v.tx, spot[1] - v.ty) > 60 && World.hypot(sx - v.x, sy - v.y) > 90)
                    fleet.routing.route(v, spot[0], spot[1]);
            }
        }
        if (v.state == DRIVE && v.pickup && v.stuckTimer > 12) {
            v.state = PICKUP;
            v.timer = 0;
            v.speed = 0;
            return false;
        }
        if (v.state == SCENE && v.type == TRUCK) return truckAtScene(v, dt);
        if (v.state == MUSTER) return musterStep(v, dt);
        if (v.state == WAIT) {
            v.timer -= dt;
            if (v.timer <= 0) {
                v.state = DRIVE;
                if (v.type == CRUISER) w.emit(Sfx.SIREN, v.x, v.y);
            }
            return false;
        }
        // On the way home empty: hold back rather than drive through a crowd of the dead, and if they don't
        // move off, leave the car parked where it is.
        if (v.state == RETURN && (v.type == CRUISER || v.type == TRUCK) && hordeAhead(v)) return holdForHorde(v, dt);
        v.hold = 0;
        v.stuckTimer += dt;
        if (fleet.trains.trainComing(v)) {
            v.speed = Math.max(0, v.speed - dt * 150);
            return false;
        }
        // ((10.60) Help from outside on its way to a call that's over: on to the one that needs it most instead.)
        if (v.reinforcement && v.state == DRIVE && v.zone == null && v.incident != null && v.rerouteCd <= 0
                && (v.incident.resolved || (v.incident.zombiesNear == 0 && v.incident.age > 20))) {
            v.rerouteCd = 4;
            Dispatch.Incident n = w.dispatch.neediest(v.x, v.y, v.incident);
            if (n != null) {
                float[] road = city.nearestDrivable(n.x, n.y);
                if (road != null && fleet.routing.route(v, road[0], road[1])) {
                    v.incident = n;
                    v.place = n.place;
                    v.state = DRIVE;
                }
            }
        }
        // ((10.51) Quicker on a call, the army above all: the trucks used to trundle to a fight.)
        boolean call = fleet.routing.emergency(v) && v.state != RETURN;
        float top = v.type == CRUISER ? (call ? 120 : 105) : v.kind == K_APC ? (call ? 85 : 68) : v.kind == K_HUMVEE ? (call ? 115 : 100) : v.kind == K_MRAP ? (call ? 95 : 78)
                : v.kind == K_RIOT_VAN ? (call ? 100 : 85) : (call ? 100 : 80);
        // ((24.2) Not on a call (going home, off to restock, empty): the lights and the junctions' rules, like
        // everyone else - they went straight through red lights and across traffic. Waiting at one isn't being stuck.)
        if (!call && v.player == null && v.type != TANK && v.kind != K_DOZER && w.alert < 2) {
            fleet.calmRules = true;
            try {
                top = Math.min(top, fleet.driving.junctionRule(v, dt));
            } finally {
                fleet.calmRules = false;
            }
            if (v.held) v.stuckTimer = Math.min(v.stuckTimer, 1);
        }
        boolean arrived = !fleet.driving.driveStep(v, dt, top, fleet.driving.carAhead(v) ? 0.5f : 1f);
        // A patrol that comes on a crowd of the dead stops and lets the gunner work; the crew stay aboard.
        if (v.loop && v.state == DRIVE && hordeAhead(v)) {
            v.speed = Math.max(0, v.speed - dt * 200);
            v.stuckTimer = 0;
            v.idleTimer += dt;
            if (v.idleTimer < 12) return false;
            arrived = true;
        }
        // (10.45) The dead spotted on the way to a call: deal with them first.
        if (v.state == DRIVE && !arrived && !v.loop && !v.pickup && !v.restock && v.zone == null && v.block == null) {
            Dispatch.Incident here = spotOnTheWay(v, dt);
            if (here != null) {
                v.incident = here;
                v.place = here.place;
                arrived = true;
                v.speed *= 0.3f;
            }
        }
        // Don't drive into a horde: stop short and let the troops out (an APC drives on in: it's armoured).
        if (v.state == DRIVE && !v.loop && v.kind != K_APC && v.kind != K_BEARCAT && (v.passengers > 0 || !v.crew.isEmpty()) && hordeAhead(v)) {
            arrived = true;
            v.speed *= 0.3f;
        }
        // Give up and stop where we are if the car hasn't made progress for a while.
        // (Reinforcements on their long drive in wait a little longer in a jam before getting out to walk.)
        // ((16.7) But an ammo truck has nobody aboard to walk the rest, and the fight it's for is waiting on it: held
        // up, it plans its way again and keeps at it a good while before it gives up - it used to stop for good on
        // an open road thousands of feet short after four seconds in a jam.)
        if (v.ammoTruck && v.state == DRIVE && !arrived && v.stuckTimer > 4 && v.jamTries < 4 && v.rerouteCd <= 0 && fleet.routing.route(v, v.tx, v.ty)) {
            v.jamTries++;
            v.rerouteCd = 5;
            v.pathField = null;
        } else if (v.stuckTimer > (v.reinforcement ? 12 : 4)) arrived = true;
        if (arrived) return arrivedStep(v, dt);
        fleet.driving.hit(v, w.runOver(v.x, v.y, 8, v.speed, v.angle));
        if (v.type == CRUISER && v.soundCd <= 0 && v.state == DRIVE) {
            v.soundCd = 3.5f;
            w.emit(Sfx.SIREN, v.x, v.y);
        }
        return false;
    }

    /** A police car parked up at the call, lights going. */
    boolean cruiserAtScene(Vehicle v, float dt) {
        // Parked up at the call, lights going, until it's over (or there's been nothing for a while).
        v.speed = Math.max(0, v.speed - dt * 200);
        v.timer += dt;
        Dispatch.Incident inc = v.incident;
        v.quiet = inc != null && w.countZombiesNear(inc.x, inc.y, 220) > 0 ? 0 : v.quiet + dt;
        if (inc == null || inc.resolved || v.quiet > 25 || v.timer > 420) {
            v.state = PICKUP;
            v.timer = 0;
            v.pickup = true;
        }
        return false;
    }

    /** An army truck (or APC) stopped at the scene. */
    boolean truckAtScene(Vehicle v, float dt) {
        // An APC that dropped its squad stays a while to cover them with its gun, then heads home.
        v.speed = Math.max(0, v.speed - dt * 200);
        v.timer -= dt;
        v.quiet = w.countZombiesNear(v.x, v.y, 200) > 0 ? 0 : v.quiet + dt;
        // ((13.9) An ammo truck goes once it's handed everything out.)
        if (v.timer <= 0 || v.quiet > 20 || (v.ammoTruck && v.cargo <= 0)) {
            v.state = RETURN;
            v.speed = 0;
            v.standby = false;
            return !fleet.routing.route(v, v.homeX, v.homeY);
        }
        return false;
    }

    /** At the gate (or the station) waiting for its squad or officers to climb aboard; then off. */
    boolean musterStep(Vehicle v, float dt) {
        // A troop truck at the gate, waiting for its squad to climb aboard.
        v.speed = 0;
        float was = v.timer;
        v.timer += dt;
        // ((13.7) Who's still on the way, once a second: a squad sent to a truck a few streets off takes most of a
        // minute to run there, and the truck gave up after 45 seconds and left them to walk the whole way.)
        if (v.coming < 0 || (int) v.timer != (int) was) v.coming = comingTo(v);
        // (10.34: quicker off the mark: most of the squad aboard after a few seconds will do.)
        // ((13.3) Longer for the rest: it used to go with two thirds aboard after six seconds, and leave the others
        // behind to walk.)
        // ((13.7) Off as soon as everyone still coming is aboard; the stragglers left behind only once the wait is long
        // and most are in.)
        // (Never with one of them almost there.)
        boolean straggling = v.coming == 0 || v.comingNear > 120;
        boolean ready = v.crew.size() >= v.crewWanted || (!v.crew.isEmpty() && v.coming == 0)
                || (straggling && v.timer > 30 && v.crew.size() * 3 >= v.crewWanted * 2) || (straggling && v.timer > 60 && !v.crew.isEmpty())
                || (v.timer > 90 && !v.crew.isEmpty());
        if (!ready) {
            // ((10.52) A car taking outside help home waits longer: the station it came from can be a walk away,
            // and giving up only sent another, and another.)
            // ((13.7) Any truck or car waits as long while its people are on their way, and not at all once nobody is.)
            if (v.timer > 90 || (v.timer > 3 && v.coming == 0)) {
                dropCrew(v);
                // ((12.3) An army truck nobody came for goes back in its bay, or stays parked where it is: it
                // vanished before, and a base soon had none left to send anyone anywhere.)
                if (v.type == TRUCK && v.passengerType == Entity.SOLDIER && !v.leaving && !v.reinforcement && !v.broken && v.crew.isEmpty()) return fleet.armyUnits.backInPool(v);
                return true;
            }
            return false;
        }
        dropCrew(v);
        Dispatch.Incident inc = v.incident;
        if (v.leaving) {
            // Outside help going home: out of the city the way they came.
            if (!fleet.routing.route(v, v.tx, v.ty)) {
                for (Entity e : new ArrayList<Entity>(v.crew)) e.rig = null;
                fleet.crews.crewOut(v, true);
                return true;
            }
            v.state = RETURN;
            v.homeX = v.tx;
            v.homeY = v.ty;
            v.stuckTimer = 0;
            return false;
        }
        if (v.loop) {
            // A patrol: off round the roads near the base.
            if (!fleet.routing.route(v, v.tx, v.ty)) {
                for (Entity e : new ArrayList<Entity>(v.crew)) e.rig = null;
                fleet.crews.crewOut(v, true);
                return true;
            }
            v.state = DRIVE;
            v.stuckTimer = 0;
            w.dispatch.say(Dispatch.WHO_MILITARY, v.crew.get(0), "Patrol mounted up. Sweeping " + v.place + ".", v.x, v.y);
            return false;
        }
        if (inc == null || inc.resolved || !fleet.routing.route(v, inc.x, inc.y)) {
            for (Entity e : new ArrayList<Entity>(v.crew)) e.rig = null;
            fleet.crews.crewOut(v, true);
            // ((12.3) The call over before they set off: the truck stays, back in the pool.)
            if (v.type == TRUCK && v.passengerType == Entity.SOLDIER && !v.reinforcement && !v.broken) return fleet.armyUnits.backInPool(v);
            return true;
        }
        v.state = DRIVE;
        v.stuckTimer = 0;
        w.dispatch.chatter(Dispatch.WHO_MILITARY, v.crew.get(0), "Mounted up, " + v.crew.size() + " aboard. Rolling out to "
                + inc.place + ".", v.x, v.y);
        return false;
    }

    /** On the way home empty with a crowd of the dead ahead: it holds back, and if they don't move off it's left parked where it is. */
    boolean holdForHorde(Vehicle v, float dt) {
        v.speed = Math.max(0, v.speed - dt * 220);
        v.hold += dt;
        v.stuckTimer = 0;
        if (v.hold > 25) {
            v.speed = 0;
            v.parked = true;
            v.state = ABANDONED;
            // (Anyone still aboard gets out.)
            if (!v.crew.isEmpty()) fleet.crews.crewOut(v, true);
        }
        return false;
    }

    /** It has got where it was going (or as near as it can): what happens there, by what it was doing - home and parked, its squad or officers out at the call, a delivery made, off the map. */
    boolean arrivedStep(Vehicle v, float dt) {
        if (v.kind == K_DOZER) return fleet.armyUnits.dozerArrived(v);
        if (v.state == DRIVE && v.block != null) {
            // A roadblock car: only set up if the zone still needs it and it got close to its spot.
            if (v.block.removed || World.hypot(v.x - v.tx, v.y - v.ty) > 90) {
                v.block = null;
            } else {
                fleet.patrols.setUpRoadblock(v);
                return false;
            }
        }
        if (v.state == DRIVE && v.supply > 0 && v.zone != null) {
            // The ammo gets through (if the truck reached the zone).
            Dispatch.SafeZone z = v.zone;
            z.supplyComing = false;
            if (!z.removed && World.hypot(v.x - z.x, v.y - z.y) < z.r + 90) {
                z.ammo += v.supply;
                z.food = Math.min(z.foodMax, z.food + z.foodMax * 0.7f);
                w.dispatch.say(Dispatch.WHO_MILITARY, null, "Military: Supply truck at " + z.place + ". " + v.supply
                        + " rounds and food delivered.", v.x, v.y);
            } else {
                w.dispatch.say(Dispatch.WHO_MILITARY, null, "Military: The supply truck couldn't get through to "
                        + z.place + ". Turning back.", v.x, v.y);
            }
            v.supply = 0;
        }
        if (v.state == DRIVE && v.pickup) {
            // Out where its people are: wait for them to climb in.
            v.state = PICKUP;
            v.timer = 0;
            v.speed = 0;
            return false;
        }
        if (v.state == RETURN && v.leaving) return leaveTheMap(v);
        if (v.state == RETURN && v.parkHome) return homeAndPark(v);
        if (v.state == DRIVE && v.restock) {
            if (World.hypot(v.x - v.tx, v.y - v.ty) < 120) w.restocked(v);
            v.state = RETURN;
            v.speed = 0;
            return !fleet.routing.route(v, v.homeX, v.homeY);
        }
        if (v.state == DRIVE && v.loop) {
            // The end of the patrol's sweep: back to base.
            v.state = RETURN;
            v.idleTimer = 0;
            v.speed = 0;
            return !fleet.routing.route(v, v.homeX, v.homeY);
        }
        if (v.state == RETURN && v.loop) {
            // Back at base: the patrol gets out.
            fleet.crews.crewOut(v, true);
            city.parkTruck(v.homeX, v.homeY);
            return true;
        }
        if (v.state == DRIVE && !v.crew.isEmpty()) {
            // The squad jumps down and goes in; the truck heads back.
            ArrayList<Entity> out = new ArrayList<Entity>(v.crew);
            fleet.crews.crewOut(v, true);
            Dispatch.Incident inc = v.incident;
            for (Entity e : out) {
                e.rig = null;
                if (inc != null && !inc.resolved) {
                    e.task = Dispatch.T_RESPOND;
                    e.incident = inc;
                    e.onScene = false;
                }
            }
            v.squad.addAll(out);
            if (!out.isEmpty() && v.place != null)
                w.dispatch.chatter(Dispatch.WHO_MILITARY, out.get(0), "On the ground at " + v.place + ". Moving in.", v.x, v.y);
        }
        if (v.state == DRIVE) return unloadAtCall(v);
        // ((13.9) Rounds it didn't hand out go back in the armoury.)
        if (v.state == RETURN && v.type == TRUCK && v.cargo > 0 && World.hypot(v.x - v.homeX, v.y - v.homeY) < 200) {
            City.Facility f = city.nearestFacility(City.FACILITY_BASE, v.x, v.y);
            if (f != null) f.ammo += v.cargo;
            v.cargo = 0;
        }
        // ((24.2) Held up in a jam on the way back isn't back: it vanished from the road into its bay across town,
        // in plain sight. Plan the way again; only where nobody's watching does it give up and go straight home.)
        if (v.state == RETURN && v.type == TRUCK && !v.swat && !v.restock && !v.broken && !v.leaving
                && World.hypot(v.x - v.homeX, v.y - v.homeY) > 160 && fleet.routing.inView(v.x, v.y)) {
            if (v.rerouteCd <= 0 && v.jamTries < 8) {
                v.jamTries++;
                v.rerouteCd = 4;
                fleet.routing.route(v, v.homeX, v.homeY);
            }
            if (v.jamTries < 8) {
                v.stuckTimer = 0;
                return false;
            }
        }
        v.jamTries = 0;
        // An army truck back at base parks in its bay again.
        if (v.state == RETURN && v.type == TRUCK && !v.swat && !v.restock && !v.broken && !w.peopleNear(v.homeX, v.homeY, 14)
                && w.countZombiesNear(v.homeX, v.homeY, 14) == 0)
            city.parkTruck(v.homeX, v.homeY);
        else if (v.state == RETURN && v.type == TRUCK && !v.swat && !v.restock && !v.broken && !v.reinforcement && !v.leaving
                && city.tiles[city.tileIndex(v.x, v.y)] != City.ROAD) {
            // ((12.3) Its bay taken (someone standing in it): into the base's garages, or parked where it is,
            // not gone for good.)
            if (!v.crew.isEmpty()) fleet.crews.crewOut(v, true);
            City.Facility f = fleet.armyUnits.yardOf(v.x, v.y);
            if (f != null && f.kind == City.FACILITY_BASE) {
                f.spareTrucks++;
                return true;
            }
            v.state = IDLE;
            v.speed = 0;
            v.parked = true;
            v.standby = true;
            return false;
        }
        return true;
    }

    /** Outside help going home, at the edge of the map: gone, with whoever's aboard. */
    boolean leaveTheMap(Vehicle v) {
        // Off the map: they're home, and back in the pool if they're needed again.
        if (World.hypot(v.x - v.homeX, v.y - v.homeY) > 120) {
            // ((10.53) Not there yet: the way planned ran out short of the edge (or it was put off it).
            // Plan it again from here rather than put them out in the fields to be fetched all over again.)
            if (v.rerouteCd <= 0 && fleet.routing.route(v, v.homeX, v.homeY)) {
                v.rerouteCd = 2;
                return false;
            }
            if (v.rerouteCd > 0) return false;
            if (!v.crew.isEmpty()) fleet.crews.crewOut(v, true);
            return true;
        }
        w.dispatch.wentHome(v.passengerType, v.crew.size());
        v.crew.clear();
        return true;
    }

    /** Back at its station or base to park (or held up short of it: a new way there). */
    boolean homeAndPark(Vehicle v) {
        // ((24.0) A jam on the way home isn't home: it parked up for good in the middle of the road, hundreds
        // of feet short. Plan the way again; only a long way off and getting nowhere does it give up and stay.)
        if (v.depot != null && World.hypot(v.x - v.depot.x, v.y - v.depot.y) > v.depot.r + City.T * 3) {
            if (v.jamTries < 6 && v.rerouteCd <= 0 && fleet.routing.route(v, v.depot.gateX, v.depot.gateY)) {
                v.jamTries++;
                v.rerouteCd = 4;
                v.stuckTimer = 0;
                return false;
            }
            if (v.rerouteCd > 0 && v.jamTries < 6) {
                v.stuckTimer = 0;
                return false;
            }
        }
        v.jamTries = 0;
        // Home: anyone aboard gets out (off duty), and it parks up ready for the next call. ((26.3) Two officers
        // who drove a police car back stay in it, waiting for the next call.)
        boolean wait = v.type == CRUISER && v.crew.size() <= 2 && v.squad.isEmpty() && v.depot != null && v.depot.kind == City.FACILITY_POLICE;
        for (int i = 0; i < v.crew.size() && wait; i++) if (v.crew.get(i).type != Entity.COP || v.crew.get(i).role != Entity.ROLE_RIFLE) wait = false;
        if (!v.crew.isEmpty() && !wait) fleet.crews.crewOut(v, true);
        parkUp(v);
        return false;
    }

    /** At the call: whoever it carried gets out, and it stays to cover them, stands by near them, or heads back. */
    boolean unloadAtCall(Vehicle v) {
        fleet.crews.unload(v, true);
        v.passengers = 0;
        if ((v.kind == K_APC || v.kind == K_IFV || v.kind == K_GUNTRUCK || v.kind == K_MRAP) && v.supply == 0) {
            // The APC stays to give its squad covering fire.
            v.state = SCENE;
            v.timer = 90;
            v.quiet = 0;
            v.speed = 0;
            v.standby = true;
            return false;
        }
        // ((10.56) A troop truck that's brought a squad into town waits near them for a while, standing by:
        // when they're sent on to the next fight, they climb back in (Fleet.mountUp) instead of walking.)
        if (v.type == TRUCK && v.supply == 0 && v.passengerType == Entity.SOLDIER && v.incident != null && city.inTown(v.x, v.y)) {
            v.state = SCENE;
            v.timer = v.ammoTruck ? 300 : 150;
            v.quiet = -40;
            v.speed = 0;
            v.standby = true;
            return false;
        }
        // (10.28) A police car waits at the scene while its officers deal with the call, then takes them home.
        if (v.type == CRUISER && v.incident != null && !v.incident.resolved && !v.squad.isEmpty()) {
            v.state = SCENE;
            v.timer = 0;
            v.quiet = 0;
            v.speed = 0;
            return false;
        }
        if (headHome(v)) return false;
        v.state = RETURN;
        v.speed = 0;
        return !fleet.routing.route(v, v.homeX, v.homeY);
    }

    /**
     * Once its people are out (10.24), a police car or army truck heads back to the nearest police station or
     * base to park, ready for the next call. Not a supply run, a roadblock car or one delivering to a safe zone.
     */
    boolean headHome(Vehicle v) {
        if (v.type != CRUISER && v.type != TRUCK) return false;
        if (v.restock || v.zone != null || v.block != null || v.loop || v.broken || v.kind == K_APC) return false;
        if (v.squad.isEmpty() && !v.pickup) return false;
        if (!v.squad.isEmpty()) v.passengerType = v.squad.get(0).type;
        City.Facility f = city.nearestFacility(v.passengerType == Entity.SOLDIER ? City.FACILITY_BASE : City.FACILITY_POLICE, v.x, v.y);
        if (f == null) return false;
        int there = 0;
        for (int i = 0; i < fleet.vehicles.size(); i++) if (fleet.vehicles.get(i).depot == f && fleet.vehicles.get(i) != v) there++;
        if (there >= DEPOT_ROOM) return false;
        // (Not an emergency any more: home by road.)
        v.incident = null;
        v.parkHome = true;
        if (!fleet.routing.route(v, f.gateX, f.gateY)) {
            v.parkHome = false;
            return false;
        }
        v.depot = f;
        v.parkHome = true;
        v.pickup = false;
        v.incident = null;
        v.state = RETURN;
        v.speed = 0;
        v.stuckTimer = 0;
        v.jamTries = 0;
        return true;
    }

    /** Parked at its station or base, off the road on the lot or the yard, clear of the others. */
    void parkUp(Vehicle v) {
        v.state = IDLE;
        v.speed = 0;
        v.parked = true;
        v.standby = true;
        v.parkHome = false;
        v.stageCd = 4;
        int tx = (int) (v.x / City.T), ty = (int) (v.y / City.T);
        // (10.45) Not in the gateway, though: one after another they parked round the gate and blocked it, so
        // nothing could get in or out. Well inside, in sight of where it is now.
        City.Facility d = v.depot;
        for (int r = 1; r <= 12; r++)
            for (int dy = -r; dy <= r; dy++)
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
                    int x = tx + dx, y = ty + dy;
                    if (x < 0 || y < 0 || x >= city.w || y >= city.h) continue;
                    byte t = city.tiles[y * city.w + x];
                    if (t != City.LOT && t != City.BASE && t != City.SIDEWALK && t != City.PLAZA) continue;
                    float px = (x + 0.5f) * City.T, py = (y + 0.5f) * City.T;
                    if (city.solidAt(px, py)) continue;
                    if (d != null && d.gateX > 0 && World.hypot(px - d.gateX, py - d.gateY) < City.T * 3.2f) continue;
                    if (d != null && r > 2 && !city.los(v.x, v.y, px, py)) continue;
                    // (And not out on the pavement beyond the fence.)
                    if (d != null && World.hypot(px - d.x, py - d.y) > d.r + City.T * 2) continue;
                    boolean taken = false;
                    for (int i = 0; i < fleet.vehicles.size() && !taken; i++) {
                        Vehicle o = fleet.vehicles.get(i);
                        if (o != v && !airborne(o) && Math.abs(o.x - px) < 12 && Math.abs(o.y - py) < 12) taken = true;
                        if (o != v && o.standby && o.pulling && Math.abs(o.pullX - px) < 12 && Math.abs(o.pullY - py) < 12) taken = true;
                    }
                    if (taken) continue;
                    v.pullX = px;
                    v.pullY = py;
                    v.pulling = true;
                    return;
                }
    }

    /** Parked and waiting: drives out to fetch its people once they're done and a long way off. */
    boolean standbyStep(Vehicle v, float dt) {
        v.stageCd -= dt;
        if (v.stageCd > 0) return false;
        v.stageCd = 4;
        float sx = 0, sy = 0;
        int n = 0;
        for (int i = v.squad.size() - 1; i >= 0; i--) {
            Entity e = v.squad.get(i);
            if (e.dead || e.removed || e.isZombie()) {
                v.squad.remove(i);
                continue;
            }
            if (e.task != Dispatch.T_NONE || e.aiming || e.rig != null) continue;
            if (World.hypot(e.x - v.x, e.y - v.y) < 600 || w.countZombiesNear(e.x, e.y, 160) > 0) continue;
            sx += e.x;
            sy += e.y;
            n++;
        }
        // (Wait until most of them are free, so it isn't back and forth for each one.)
        if (n == 0 || n * 2 < v.squad.size()) return false;
        float[] spot = city.nearestDrivable(sx / n, sy / n);
        if (spot == null) return false;
        v.parked = false;
        v.pulling = false;
        v.standby = false;
        v.pickup = true;
        if (!fleet.routing.route(v, spot[0], spot[1])) {
            v.parked = true;
            v.standby = true;
            v.pickup = false;
            return false;
        }
        v.state = DRIVE;
        v.stuckTimer = 0;
        v.place = city.placeName(spot[0], spot[1]);
        if (v.squad.size() > 0)
            w.dispatch.chatter(v.passengerType == Entity.SOLDIER ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, v.squad.get(0),
                    (v.type == TRUCK ? "Truck" : "Car") + " coming out to " + v.place + " to pick us up.", v.x, v.y);
        return false;
    }

    /** Out where its people are, waiting while they climb in; then back to the station or base. */
    boolean pickupStep(Vehicle v, float dt) {
        v.speed = Math.max(0, v.speed - dt * 200);
        v.timer += dt;
        int walking = 0;
        for (Entity e : v.squad) {
            if (e.dead || e.removed || e.isZombie()) continue;
            if (World.hypot(e.x - v.x, e.y - v.y) > 700 || (e.task != Dispatch.T_NONE && e.task != Dispatch.T_BOARD)) continue;
            walking++;
            if (e.task != Dispatch.T_BOARD) {
                e.rig = v; fleet.crews.note(e);
                e.task = Dispatch.T_BOARD;
            }
        }
        // ((13.3) A minute and a half for them to get there, not 45 seconds.)
        if ((walking > 0 && v.timer < 90) || v.timer < 2) return false;
        v.pickup = false;
        for (Entity e : v.squad)
            if (!e.dead && e.task == Dispatch.T_BOARD && e.rig == v) {
                e.task = Dispatch.T_NONE;
                e.rig = null;
            }
        // ((11.0) Picked up for a call: off to it.)
        Dispatch.Incident to = v.pickupFor;
        v.pickupFor = null;
        if (to != null && !to.resolved && !v.crew.isEmpty()) {
            v.squad.clear();
            if (v.type == CRUISER) {
                v.patrol = true;
                if (fleet.patrols.respond(v, to)) return false;
                v.state = PATROL;
                if (!fleet.routing.newDestination(v)) v.stuckTimer = 99;
                return false;
            }
            float[] road = city.nearestDrivable(to.x, to.y);
            if (road != null && fleet.routing.route(v, road[0], road[1])) {
                v.incident = to;
                v.place = to.place;
                v.state = DRIVE;
                v.stuckTimer = 0;
                w.dispatch.chatter(Dispatch.WHO_MILITARY, v.crew.get(0), "Mounted up, " + v.crew.size() + " aboard. On our way to " + to.place + ".", v.x, v.y);
                return false;
            }
        }
        if (v.wasPatrol && !v.crew.isEmpty()) {
            // (10.48) A patrol car taken over: back out on patrol with its new crew.
            v.wasPatrol = false;
            v.patrol = true;
            v.squad.clear();
            v.state = PATROL;
            v.incident = null;
            if (!fleet.routing.newDestination(v)) v.stuckTimer = 99;
            return false;
        }
        if (headHome(v)) return false;
        if (!v.crew.isEmpty()) fleet.crews.crewOut(v, true);
        v.state = RETURN;
        return !fleet.routing.route(v, v.homeX, v.homeY);
    }

    /**
     * (10.27) Outside police or soldiers going home: one of the cars or trucks parked in town (or one that comes
     * round for them) picks them up and drives out at (ex, ey). False if there's nothing to take them.
     */
    boolean sendHome(java.util.List<Entity> group, float ex, float ey) {
        if (group.isEmpty()) return false;
        boolean cops = group.get(0).type == Entity.COP;
        float sx = 0, sy = 0;
        for (Entity e : group) {
            sx += e.x;
            sy += e.y;
        }
        sx /= group.size();
        sy /= group.size();
        float[] p = takeStandby(cops ? CRUISER : TRUCK, sx, sy, 700);
        if (p == null) p = city.nearestDrivable(sx, sy);
        if (p == null) return false;
        Vehicle v = fleet.make(cops ? CRUISER : TRUCK);
        v.x = p[0];
        v.y = p[1];
        v.state = MUSTER;
        v.leaving = true;
        v.reinforcement = true;
        v.crewWanted = group.size();
        v.tx = ex;
        v.ty = ey;
        v.place = "home";
        v.passengerType = cops ? Entity.COP : Entity.SOLDIER;
        v.angle = (float) Math.atan2(ey - p[1], ex - p[0]);
        for (Entity e : group) {
            e.rig = v; fleet.crews.note(e);
            e.task = Dispatch.T_BOARD;
        }
        fleet.vehicles.add(v);
        return true;
    }

    /** A car or truck parked at a station or base near (x, y), taken for a new call (removed; its spot returned). */
    float[] takeStandby(int type, float x, float y, float radius) {
        Vehicle best = null;
        float bd = radius * radius;
        for (int i = 0; i < fleet.vehicles.size(); i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (!v.standby || v.type != type || v.broken || FleetMedevac.needsRepair(v)) continue;
            float d = (v.x - x) * (v.x - x) + (v.y - y) * (v.y - y);
            if (d < bd) {
                bd = d;
                best = v;
            }
        }
        if (best == null) return null;
        // ((26.3) Anyone sitting in it waiting gets out first: it's being taken for others.)
        if (!best.crew.isEmpty()) fleet.crews.crewOut(best, true);
        best.removedFromFleet = true;
        fleet.vehicles.remove(best);
        return new float[]{best.x, best.y};
    }

    /**
     * A crowd of the dead on the road ahead (up to about 160 units out) or already round the car: police
     * and troops stop short there and go in on foot, rather than driving into the middle of them.
     */
    /**
     * (10.45) Officers or soldiers on their way to a call keep an eye out: one of the dead in sight close by (and
     * not part of the call they're going to) and they stop, radio it in and take it on there. The call there,
     * or null. Only with people aboard to get out, and not on a car that's just set off from the scene.
     */
    Dispatch.Incident spotOnTheWay(Vehicle v, float dt) {
        v.reconCd -= dt;
        if (v.spotCd > 0) return null;
        v.spotCd = 0.6f;
        // ((16.9) A police car or van with only its driver aboard is no different: the driver pulls up and takes them on.
        // It used to drive straight past, as if there was nobody in it to get out.)
        boolean lone = v.crew.isEmpty() && v.driver != null && (v.type == CRUISER || policeTruck(v)) && v.state == DRIVE;
        if ((v.crew.isEmpty() && !lone) || v.player != null || (v.type != CRUISER && v.type != TRUCK)) return null;
        Entity z = w.nearestZombie(v.x, v.y, 105);
        if (z == null || !city.los(v.x, v.y, z.x, z.y)) return null;
        Dispatch.Incident inc = v.incident;
        if (inc != null && World.hypot(z.x - inc.x, z.y - inc.y) < 240) return null;
        Entity who = lone ? v.driver : v.crew.get(0);
        // (Out of rounds: nothing to fight with, so they drive on.)
        boolean armed = false;
        if (lone) armed = who.ammo + who.reserve > 0;
        for (Entity e : v.crew) if (e.ammo + e.reserve > 0) armed = true;
        if (!armed) return null;
        Dispatch.Incident seen = w.dispatch.sighting(z.x, z.y, who);
        if (seen != null && lone) {
            // (The driver becomes the car's crew, and gets out with it like any crew.)
            Entity d = v.driver;
            v.driver = null;
            v.realDriver = false;
            d.rig = v; fleet.crews.note(d);
            d.dead = true;
            d.removed = true;
            v.crew.add(d);
        }
        return seen;
    }

    boolean hordeAhead(Vehicle v) {
        if (!w.zombieWithin(v.x, v.y, 200)) return false;
        float ca = (float) Math.cos(v.angle), sa = (float) Math.sin(v.angle);
        for (float d = 45; d <= 125; d += 40)
            if (w.countZombiesNear(v.x + ca * d, v.y + sa * d, 50) >= 4) return true;
        return w.countZombiesNear(v.x, v.y, 45) >= 3;
    }

    /** At the end of a trip the riders get out (at the safe zone, if that's where they were going). */
    void arrive(Vehicle v) {
        if (v.riders.isEmpty()) return;
        boolean zone = v.rescue && v.zone != null && !v.zone.removed;
        if (zone)
            w.dispatch.say(Dispatch.WHO_INFO, null, "A driver dropped " + v.riders.size()
                    + (v.riders.size() == 1 ? " person" : " people") + " at the " + v.zone.place + " safe zone.", v.x, v.y);
        for (int i = 0; i < v.riders.size(); i++) {
            Entity e = v.riders.get(i);
            if (!w.release(e, v.x + (float) Math.cos(v.angle + 1.57f) * 9, v.y + (float) Math.sin(v.angle + 1.57f) * 9))
                continue;
            if (zone) e.task = Dispatch.T_SEEK;
        }
        v.riders.clear();
        v.rescue = false;
        v.zone = null;
    }

    void dropRiders(Vehicle v, boolean panic) {
        // (10.48) People fleeing the city who've got to the edge of the map (or out of sight on the way): gone.
        float edge = 6 * City.T;
        if (v.exodus && !panic && (!fleet.routing.inView(v.x, v.y) || v.x < edge || v.y < edge || v.x > city.worldW() - edge || v.y > city.worldH() - edge)) {
            w.droveOut(v.riders);
            return;
        }
        for (int i = 0; i < v.riders.size(); i++) {
            Entity e = v.riders.get(i);
            float a = v.angle + (i % 2 == 0 ? 1.57f : -1.57f);
            if (!w.release(e, v.x + (float) Math.cos(a) * 9, v.y + (float) Math.sin(a) * 9)) continue;
            if (panic) {
                e.fleeTimer = 3;
                e.threatX = v.x;
                e.threatY = v.y;
            }
        }
        v.riders.clear();
        v.rescue = false;
    }

    /**
     * (11.0) Officers or soldiers sent to a call a long way off don't walk it: an empty police car or army truck
     * close by takes them, or with none, one comes out from the station or base to pick them up, and drives them
     * there. False if there's nothing to take them (they go on foot). ((13.3) With no call, inc null: a lift back to
     * the station or base.)
     */
    boolean driveTo(java.util.List<Entity> group, Dispatch.Incident inc) {
        if (group.isEmpty()) return false;
        int type = group.get(0).type, vtype = type == Entity.COP ? CRUISER : TRUCK;
        float sx = 0, sy = 0;
        for (Entity e : group) {
            sx += e.x;
            sy += e.y;
        }
        sx /= group.size();
        sy /= group.size();
        Vehicle v = null;
        // ((11.7) An empty one a bit further off is still quicker than walking.)
        float bd = 450;
        for (int i = 0, m = fleet.vehicles.size(); i < m; i++) {
            Vehicle o = fleet.vehicles.get(i);
            if (o.type != vtype || o.broken || o.burnt || o.player != null || o.removedFromFleet || !o.crew.isEmpty() || o.block != null
                    || o.zone != null || o.loop || o.shelter || o.leaving || o.kind == K_BIKE || o.kind == K_DOZER || o.pickupFor != null || o.state == PICKUP || o.state == MUSTER) continue;
            if (!o.standby && !o.parked && o.state != ABANDONED) continue;
            float d = (float) World.hypot(o.x - sx, o.y - sy);
            if (d < bd && w.countZombiesNear(o.x, o.y, 80) == 0) {
                bd = d;
                v = o;
            }
        }
        boolean fetch = v == null;
        if (fetch) {
            City.Facility f = city.nearestFacility(type == Entity.COP ? City.FACILITY_POLICE : City.FACILITY_BASE, sx, sy);
            // ((12.3) The army sends a truck from further off: driven, even a long way round beats the walk.)
            boolean fromBase = f != null && (inc == null || World.hypot(f.x - sx, f.y - sy) <= World.hypot(inc.x - sx, inc.y - sy) * (type == Entity.SOLDIER ? 1.5f : 0.8f));
            Entity drv = null;
            if (fromBase) {
                for (int i = 0; i < fleet.vehicles.size() && v == null; i++) {
                    Vehicle o = fleet.vehicles.get(i);
                    if (o.standby && o.type == vtype && !o.broken && o.kind != K_BIKE && o.pickupFor == null && World.hypot(o.x - f.x, o.y - f.y) < f.r + 300) v = o;
                }
                // ((11.7) Only a car or truck parked there, and only with someone there to drive it out: nothing comes
                // out of a station or base with nobody in it, and none appear from nowhere when they're all out.)
                drv = fleet.armyUnits.freeDriver(f, type, group);
                // ((16.9) A base's trucks come out with one of the motor pool at the wheel when there's nobody else free
                // to drive: soldiers far from base used to be left to walk it - most of them, late on, with everyone out.)
                if (drv == null && !(type == Entity.SOLDIER && !f.fallen)) v = null;
                else if (v == null && type == Entity.SOLDIER && !f.fallen) {
                    // ((12.3) One of the trucks in the base's motor pool.)
                    float[] p = fleet.armyUnits.baseTruck(f);
                    if (p != null) {
                        p = fleet.armyUnits.bay(p, f);
                        v = fleet.make(TRUCK);
                        v.x = p[0];
                        v.y = p[1];
                        v.homeX = p[0];
                        v.homeY = p[1];
                        v.angle = (float) Math.atan2(sy - p[1], sx - p[0]);
                        v.state = IDLE;
                        v.speed = 0;
                        v.parked = true;
                        v.standby = true;
                        fleet.vehicles.add(v);
                    }
                }
            }
            if (v == null && type == Entity.COP) {
                // ((24.3) Or a patrol car on its rounds nearby with seats to spare: it swings by for them and takes them
                // all to the call, its own crew too. Officers far from their station walked to nearly every call.)
                drv = null;
                float best = 1500;
                for (int i = 0, m = fleet.vehicles.size(); i < m; i++) {
                    Vehicle o = fleet.vehicles.get(i);
                    if (o.type != CRUISER || !o.patrol || o.state != PATROL || o.incident != null || o.crew.isEmpty() || o.crew.size() > 2
                            || o.kind == K_BIKE || o.pickupFor != null || o.player != null || o.broken || o.burnt || o.removedFromFleet
                            || o.block != null || o.zone != null || o.loop || o.leaving || o.shelter) continue;
                    float d = (float) World.hypot(o.x - sx, o.y - sy);
                    if (d < best && w.countZombiesNear(o.x, o.y, 80) == 0) {
                        best = d;
                        v = o;
                    }
                }
            }
            if (v == null && type == Entity.SOLDIER) {
                // ((12.3) Or an army truck standing by somewhere in town, after dropping a squad off: it drives over.)
                drv = null;
                // ((16.9) From further off: a truck a couple of miles away is still quicker than the walk.)
                float best = 3000;
                for (int i = 0, m = fleet.vehicles.size(); i < m; i++) {
                    Vehicle o = fleet.vehicles.get(i);
                    if (!o.standby || o.type != TRUCK || o.broken || o.burnt || o.kind == K_BIKE || o.pickupFor != null || o.player != null
                            || o.removedFromFleet || !o.crew.isEmpty() || o.block != null || o.zone != null || o.loop || o.shelter || o.leaving || o.supply > 0) continue;
                    float d = (float) World.hypot(o.x - sx, o.y - sy);
                    if (d < best && w.countZombiesNear(o.x, o.y, 80) == 0) {
                        best = d;
                        v = o;
                    }
                }
            }
            if (v == null) return false;
            float[] spot = city.nearestDrivable(sx, sy);
            if (spot == null || !fleet.routing.route(v, spot[0], spot[1])) return false;
            fleet.armyUnits.seatDriver(v, drv);
            v.aimCd = 3;
            v.state = DRIVE;
            v.stuckTimer = 0;
            v.place = city.placeName(spot[0], spot[1]);
        } else {
            v.state = PICKUP;
            v.timer = 0;
            v.speed = 0;
        }
        v.parked = false;
        v.standby = false;
        v.pulling = false;
        v.pickup = true;
        v.wasPatrol = false;
        v.patrol = false;
        v.incident = null;
        v.pickupFor = inc;
        v.reclaimCd = 30;
        v.squad.clear();
        v.squad.addAll(group);
        v.passengerType = type;
        for (Entity e : group) {
            w.dispatch.releaseForResupply(e);
            e.rig = v; fleet.crews.note(e);
            e.task = Dispatch.T_BOARD;
        }
        return true;
    }

    /** Officers in patrol cars on their way to this call. */
    int inbound(Dispatch.Incident inc) {
        int n = 0;
        for (int i = 0, m = fleet.vehicles.size(); i < m; i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (v.patrol && v.incident == inc && v.state == DRIVE) n += v.crew.size();
            else if (v.patrol && v.incident == inc && v.state == MUSTER) n += Math.max(v.crew.size(), v.crewWanted);
            else if (v.pickupFor == inc && v.passengerType == Entity.COP) n += v.squad.size();
        }
        return n;
    }

    /** (13.7) How many tied to this vehicle are still alive and on their way to climb in (and the nearest's distance). */
    int comingTo(Vehicle v) {
        int n = 0;
        float near = Float.MAX_VALUE;
        for (int i = 0, m = w.entities.size(); i < m; i++) {
            Entity e = w.entities.get(i);
            if (e.rig == v && !e.dead && e.task == Dispatch.T_BOARD) {
                n++;
                near = Math.min(near, (float) World.hypot(e.x - v.x, e.y - v.y));
            }
        }
        v.comingNear = near;
        return n;
    }

    /** Lets go of any crew member still tied to the vehicle (they carry on on foot). */
    void dropCrew(Vehicle v) {
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.rig == v) {
                // ((27.4) A job that was getting into this vehicle is over: the vehicle let them go.)
                if (e.owner != null && e.owner.rig == v) e.owner = null;
                e.rig = null;
                if (e.task == Dispatch.T_BOARD) e.task = Dispatch.T_NONE;
            }
        }
    }
}
