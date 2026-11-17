package com.instantaxtion.zombiesandbox;

import static com.instantaxtion.zombiesandbox.Fleet.*;

import java.util.ArrayList;

/**
 * ((27.3) Part of the vehicle code, out of Fleet.) Police patrol cars and motorcycles: starting and staffing patrols from the stations, the cars standing by there with their officers in them, answering calls, traffic stops, ammunition runs, roadblocks, and the patrol car's own step (updatePatrol).
 */
final class FleetPatrols {
    final Fleet fleet;
    final World w;
    final City city;

    FleetPatrols(Fleet fleet) {
        this.fleet = fleet;
        this.w = fleet.w;
        this.city = fleet.city;
    }

    /** The park rangers' truck: out of the ranger station along the dirt lanes, one ranger at the wheel. */
    Vehicle startRangerPatrol(City.Building station) {
        float[] start = city.rangerLot != null ? city.nearestDrivable(city.rangerLot[0], city.rangerLot[1])
                : city.nearestDrivable(station.doorX, station.doorY);
        if (start == null) return null;
        Vehicle v = fleet.make(CRUISER);
        v.x = start[0];
        v.y = start[1];
        v.patrol = true;
        v.agency = 3;
        v.state = PATROL;
        v.number = 30 + (++fleet.unitCount);
        if (!fleet.routing.newDestination(v)) return null;
        Entity e = w.create(Entity.COP, v.x, v.y);
        w.makeRanger(e);
        e.rig = v; fleet.crews.note(e);
        v.crew.add(e);
        fleet.vehicles.add(v);
        return v;
    }

    /** A police car with two officers, setting off on patrol from here. */
    Vehicle placePolice(float x, float y) {
        for (int tries = 0; tries < 6; tries++) {
            float[] d = fleet.routing.randomRoad();
            if (d == null || World.hypot(d[0] - x, d[1] - y) < 250) continue;
            int before = fleet.vehicles.size();
            if (fleet.service.send(Entity.COP, 2, x, y, d[0], d[1], null, null, city.placeName(d[0], d[1])) && fleet.vehicles.size() > before) {
                Vehicle v = fleet.vehicles.get(fleet.vehicles.size() - 1);
                v.timer = 0.3f;
                return v;
            }
        }
        return null;
    }

    /**
     * Police close the main approaches to a new safe zone. For each side of the zone, the nearest road running
     * towards it gets a cruiser sent from the precinct; on arrival it parks angled across the inbound lane with
     * cones out and an officer posted beside it. Returns how many were sent.
     */
    int roadblocks(Dispatch.SafeZone z) {
        City.Facility precinct = city.nearestFacility(City.FACILITY_POLICE, z.x, z.y);
        if (precinct == null) return 0;
        int tx0 = (int) (z.x / City.T), ty0 = (int) (z.y / City.T);
        int rMin = (int) ((z.r + 30) / City.T), rMax = (int) ((z.r + 90) / City.T);
        int sent = 0;
        // North, south, west, east: walk outwards along a narrow band looking for a road heading into the zone.
        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        for (int[] dv : dirs) {
            if (sent >= 3) break;
            float[] best = null;
            for (int dist = rMin; dist <= rMax && best == null; dist++)
                for (int side = -3; side <= 3 && best == null; side++) {
                    int tx = tx0 + dv[0] * dist + (dv[0] == 0 ? side : 0), ty = ty0 + dv[1] * dist + (dv[1] == 0 ? side : 0);
                    if (tx < 2 || ty < 2 || tx >= city.w - 2 || ty >= city.h - 2) continue;
                    int dir = city.roadDirAt(tx, ty);
                    // The road must run towards the zone (north-south above and below it, east-west to the sides).
                    if (dir != (dv[0] == 0 ? 1 : 2)) continue;
                    float x = tx * City.T + City.T / 2f, y = ty * City.T + City.T / 2f;
                    boolean taken = false;
                    for (int k = 0; k < fleet.vehicles.size() && !taken; k++)
                        if (World.hypot(fleet.vehicles.get(k).x - x, fleet.vehicles.get(k).y - y) < 24) taken = true;
                    if (!taken) best = new float[]{x, y, dir};
                }
            if (best == null) continue;
            int before = fleet.vehicles.size();
            if (!fleet.service.send(Entity.COP, 1, precinct.x, precinct.y, best[0], best[1], null, null, city.placeName(best[0], best[1])))
                continue;
            for (int k = before; k < fleet.vehicles.size(); k++) {
                Vehicle v = fleet.vehicles.get(k);
                v.block = z;
                v.blockDir = (int) best[2];
                v.timer = 1 + sent * 2;
            }
            sent++;
        }
        return sent;
    }

    /** A roadblock car has arrived: it parks angled across the lane, lights on, and its officer takes post. */
    void setUpRoadblock(Vehicle v) {
        float along = v.blockDir == 1 ? (float) Math.PI / 2 : 0;
        v.angle = along + (w.rnd.nextBoolean() ? 1 : -1) * 0.75f;
        v.parked = true;
        v.state = BLOCK;
        v.speed = 0;
        v.cones = true;
        float nx = (float) Math.cos(v.angle + 1.57f), ny = (float) Math.sin(v.angle + 1.57f);
        float[] p = city.findWalkable(v.x + nx * 10, v.y + ny * 10);
        if (p != null && v.passengers > 0) {
            Entity cop = w.spawn(Entity.COP, p[0], p[1]);
            if (cop != null) {
                cop.task = Dispatch.T_POST;
                cop.postX = p[0];
                cop.postY = p[1];
                v.guard = cop;
            }
        }
        v.passengers = 0;
        if (v.block != null) putUpBarrier(v, v.block.x, v.block.y);
    }

    /**
     * (14.8) Crowd barriers: a line of them across the road and the pavements either side, a little way out from the
     * roadblock car on the side away from the zone (zx, zy), out to the buildings or fences each side.
     */
    void putUpBarrier(Vehicle v, float zx, float zy) {
        boolean ns = v.blockDir == 1;
        // The road runs north-south (ns) or east-west; the line goes across it.
        float ax = ns ? 0 : 1, ay = ns ? 1 : 0;
        float side = ((zx - v.x) * ax + (zy - v.y) * ay) >= 0 ? 1 : -1;
        float cx = v.x - ax * side * 16, cy = v.y - ay * side * 16;
        float px = ay, py = ax;
        float lo = 0, hi = 0;
        while (lo > -70 && !city.solidAt(cx + px * (lo - 4), cy + py * (lo - 4))) lo -= 4;
        while (hi < 70 && !city.solidAt(cx + px * (hi + 4), cy + py * (hi + 4))) hi += 4;
        if (hi - lo < 12) return;
        v.barX0 = cx + px * lo;
        v.barY0 = cy + py * lo;
        v.barX1 = cx + px * hi;
        v.barY1 = cy + py * hi;
        v.barNx = ax;
        v.barNy = ay;
        v.barIn = (int) side;
        v.barHp = BARRIER_HP;
        v.barDown = false;
    }

    // ------------------------------------------------------------------ patrol cars

    /** A patrol car out on the roads with two officers aboard (at the start of the game). */
    Vehicle startPatrol(City.Facility station) {
        float[] start = fleet.routing.randomRoad();
        // ((10.41) A country station's cars start out from it, on its own patch.)
        if (station != null && station.satellite) {
            float[] s = city.nearestDrivable(station.gateX > 0 ? station.gateX : station.x, station.gateY > 0 ? station.gateY : station.y);
            if (s != null) start = s;
        }
        if (start == null) return null;
        Vehicle v = fleet.make(CRUISER);
        v.x = start[0];
        v.y = start[1];
        v.angle = w.rnd.nextInt(4) * (float) Math.PI / 2;
        v.patrol = true;
        v.station = station;
        v.state = PATROL;
        v.number = ++fleet.unitCount;
        if (!fleet.routing.newDestination(v)) return null;
        // (Every fifth unit is a motorcycle officer, riding alone.)
        boolean bike = city.cfg.nature() && v.number % 5 == 0;
        if (bike) fleet.setKind(v, K_BIKE);
        for (int k = 0; k < (bike ? 1 : 2); k++) {
            Entity e = w.create(Entity.COP, v.x, v.y);
            e.rig = v; fleet.crews.note(e);
            v.crew.add(e);
        }
        fleet.vehicles.add(v);
        return v;
    }

    /** What a patrol car is called on the radio. */
    String callsign(Vehicle v) {
        Country c = city.country;
        // ((12.0) A car out of the station with no number of its own yet gets one: on the radio it was "Car 0".)
        if (v.number <= 0) v.number = ++fleet.unitCount;
        return v.agency == 1 ? c.hpName + " " + v.number : v.agency == 2 ? c.ruralName + " " + v.number : v.agency == 3 ? "Ranger " + v.number
                : v.agency == 4 ? Dispatch.federal(c) + " " + v.number : (v.kind == K_BIKE ? "Motor " : "Car ") + v.number;
    }

    /**
     * A highway patrol car on the highway, or a sheriff's car out on the country roads, with its officer
     * aboard (at the start of the game).
     */
    Vehicle startAgencyPatrol(int agency) {
        float[] start = agency == 1 ? city.randomHighway(w.rnd) : city.randomCountryRoad(w.rnd);
        if (start == null) return null;
        Vehicle v = fleet.make(CRUISER);
        v.x = start[0];
        v.y = start[1];
        v.patrol = true;
        v.agency = agency;
        v.state = PATROL;
        v.number = agency * 10 + (++fleet.unitCount);
        if (!fleet.routing.newDestination(v)) return null;
        Entity e = w.create(Entity.COP, v.x, v.y);
        e.agency = agency;
        Country c = city.country;
        e.body = agency == 1 ? c.hpShirt : c.ruralShirt;
        e.rig = v; fleet.crews.note(e);
        v.crew.add(e);
        fleet.vehicles.add(v);
        return v;
    }

    /**
     * Highway patrol, while all is quiet, pulls the odd driver over: the car pulls onto the shoulder, the
     * patrol car stops behind it with its lights going, and after a while both drive on. Returns true while
     * it's stopped behind one.
     */
    boolean trafficStop(Vehicle v, float dt) {
        Vehicle c = v.stopping;
        if (c != null) {
            v.stopT += dt;
            boolean over = c.removedFromFleet || c.broken || c.parked || v.stopT > 26 || w.alert > 0
                    || World.hypot(c.x - v.x, c.y - v.y) > 220 || v.state != PATROL;
            if (over) {
                c.pulled = false;
                v.stopping = null;
                v.stopCd = 40 + w.rnd.nextFloat() * 50;
                return false;
            }
            // Up behind it, then stop.
            if (World.hypot(c.x - v.x, c.y - v.y) < 34 && Math.abs(c.speed) < 3) {
                v.speed = Math.max(0, v.speed - dt * 120);
                return true;
            }
            return false;
        }
        v.stopCd -= dt;
        if (v.stopCd > 0 || w.alert > 0 || v.state != PATROL || !city.onHighway(v.x, v.y)) return false;
        v.stopCd = 8;
        float fx = (float) Math.cos(v.angle), fy = (float) Math.sin(v.angle);
        for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
            Vehicle o = fleet.vehicles.get(i);
            if (o.type != CAR || o.player != null || o.pulled || o.fleeing || o.parked || o.broken || !o.riders.isEmpty()) continue;
            float dx = o.x - v.x, dy = o.y - v.y, ahead = dx * fx + dy * fy;
            if (ahead < 30 || ahead > 170 || Math.abs(dx * fy - dy * fx) > 30) continue;
            if (fx * (float) Math.cos(o.angle) + fy * (float) Math.sin(o.angle) < 0.8f || !city.onHighway(o.x, o.y)) continue;
            o.pulled = true;
            v.stopping = o;
            v.stopT = 0;
            w.emit(Sfx.SIREN, v.x, v.y);
            return false;
        }
        return false;
    }

    /** Patrol cars on the streets (not wrecked or abandoned). */
    int patrolCars() {
        int n = 0;
        for (int i = 0, m = fleet.vehicles.size(); i < m; i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (v.patrol && v.agency == 0 && !v.broken && !v.parked) n++;
        }
        return n;
    }

    /** Every so often a station puts another car on the streets if it has two officers free. */
    /**
     * ((24.3) A patrol car out on its own with one officer aboard (the other lost, or left at a scene) picks up a
     * partner: an officer on foot nearby with nothing on. A fifth of the police cars about were going round alone.)
     */
    void pairUp() {
        for (int i = 0, m = fleet.vehicles.size(); i < m; i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (v.type != CRUISER || !v.patrol || v.state != PATROL || v.crew.size() != 1 || v.kind == K_BIKE || v.incident != null
                    || v.player != null || v.pickupFor != null || v.broken) continue;
            Entity best = null;
            float bd = 320;
            for (int k = 0, n = w.gatherArmed(v.x, v.y, bd); k < n; k++) {
                Entity e = w.gathered[k];
                if (e.dead || e.type != Entity.COP || e.task != Dispatch.T_NONE || e.rig != null || e == w.controlled || World.footPatrol(e)
                        || e.role == Entity.ROLE_K9 || e.role == Entity.ROLE_MARKSMAN || e.role == Entity.ROLE_SAR || e.role == Entity.ROLE_PILOT
                        || e.ammo + e.reserve <= 0) continue;
                float d = (float) World.hypot(e.x - v.x, e.y - v.y);
                if (d < bd) {
                    bd = d;
                    best = e;
                }
            }
            if (best == null) continue;
            float[] spot = city.nearestDrivable(best.x, best.y);
            if (spot == null || !fleet.routing.route(v, spot[0], spot[1])) continue;
            v.state = DRIVE;
            v.stuckTimer = 0;
            v.pickup = true;
            v.wasPatrol = true;
            v.patrol = false;
            v.pickupFor = null;
            v.squad.clear();
            v.squad.add(best);
            v.passengerType = Entity.COP;
            best.rig = v;
            fleet.crews.note(best);
            best.task = Dispatch.T_BOARD;
            return;
        }
    }

    void staffPatrols(float dt) {
        fleet.staffCd -= dt;
        if (fleet.staffCd > 0) return;
        fleet.staffCd = 4;
        pairUp();
        seatStandby(false);
        // ((11.4) In quiet times every officer who isn't on a foot beat gets a car; under attack, only up to the
        // target, the rest guarding the precinct.)
        if (w.alert >= 1 && fleet.patrolCount >= fleet.patrolTarget) return;
        for (City.Facility f : city.facilities) {
            if (f.kind != City.FACILITY_POLICE || w.countZombiesNear(f.x, f.y, 120) > 0) continue;
            // ((26.3) A car waiting there with its officers already in it goes out on patrol first (two always kept back).)
            Vehicle seated = seatedCar(f);
            if (seated != null && parkedAt(f) > 2 && launch(seated, f, null)) return;
            boolean bike = city.cfg.nature() && (fleet.unitCount + 1) % 5 == 0;
            int want = bike ? 1 : 2;
            ArrayList<Entity> two = new ArrayList<Entity>();
            for (int i = 0, n = w.entities.size(); i < n && two.size() < want; i++) {
                Entity e = w.entities.get(i);
                if (e.dead || e.type != Entity.COP || e.task != Dispatch.T_NONE || e.rig != null) continue;
                if (e.role == Entity.ROLE_MARKSMAN || e.role == Entity.ROLE_K9) continue;
                // ((11.4) The foot patrols keep walking their beat.)
                if (World.footPatrol(e) && w.alert < 1) continue;
                if (fleet.patrolCount >= fleet.patrolTarget && (e.role != Entity.ROLE_RIFLE || e.agency != 0)) continue;
                if (World.hypot(e.x - f.x, e.y - f.y) < 220) two.add(e);
            }
            if (two.size() < want) continue;
            // ((11.7) Only the cars parked at the station, and two always kept back for calls: when the rest are out,
            // officers walk their beat instead.)
            int parked = 0;
            for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
                Vehicle o = fleet.vehicles.get(i);
                if (o.standby && o.type == CRUISER && !o.broken && World.hypot(o.x - f.x, o.y - f.y) < f.r + 300) parked++;
            }
            if (parked <= 2) continue;
            float[] p = fleet.service.takeStandby(CRUISER, f.x, f.y, f.r + 300);
            if (p == null) continue;
            Vehicle v = fleet.make(CRUISER);
            v.x = p[0];
            v.y = p[1];
            v.angle = (float) Math.atan2(p[1] - f.y, p[0] - f.x);
            v.patrol = true;
            v.station = f;
            v.state = MUSTER;
            v.crewWanted = want;
            v.number = ++fleet.unitCount;
            if (bike) fleet.setKind(v, K_BIKE);
            for (Entity e : two) {
                e.rig = v; fleet.crews.note(e);
                e.task = Dispatch.T_BOARD;
            }
            fleet.vehicles.add(v);
            return;
        }
    }

    /**
     * (10.41) Officers at a station sent to a call a long way off take one of its cars (two to a car), instead of
     * walking there: it pulls up at the station's gate, they get in, and it drives to the call with the lights
     * on. False if there's no road from the station.
     */
    boolean stationCar(java.util.List<Entity> officers, City.Facility f, Dispatch.Incident inc) {
        if (officers.isEmpty() || inc == null) return false;
        float[] p = city.nearestDrivable(f.gateX > 0 ? f.gateX : f.x, f.gateY > 0 ? f.gateY : f.y);
        if (p == null || !fleet.routing.canDrive(p[0], p[1], inc.x, inc.y)) return false;
        // ((26.3) A car with its officers already in it, waiting: it goes, lights on, and these two stay at the station.)
        Vehicle seated = seatedCar(f);
        if (seated != null && launch(seated, f, inc)) return true;
        // ((11.7) One of the cars parked there, if there's one left: none appear out of nowhere.)
        p = fleet.service.takeStandby(CRUISER, f.x, f.y, f.r + 300);
        if (p == null) return false;
        Vehicle v = fleet.make(CRUISER);
        v.x = p[0];
        v.y = p[1];
        v.angle = (float) Math.atan2(inc.y - p[1], inc.x - p[0]);
        v.patrol = true;
        v.station = f;
        // ((16.9) The FBI's own cars, from the field office.)
        if (f.fed) v.agency = 4;
        v.state = MUSTER;
        v.crewWanted = officers.size();
        v.incident = inc;
        v.number = ++fleet.unitCount;
        for (Entity e : officers) {
            e.rig = v; fleet.crews.note(e);
            e.task = Dispatch.T_BOARD;
        }
        fleet.vehicles.add(v);
        return true;
    }

    /** ((26.3) Police cars parked at station f, ready. */
    int parkedAt(City.Facility f) {
        int n = 0;
        for (int i = 0, m = fleet.vehicles.size(); i < m; i++) {
            Vehicle o = fleet.vehicles.get(i);
            if (o.standby && o.type == CRUISER && !o.broken && World.hypot(o.x - f.x, o.y - f.y) < f.r + 300) n++;
        }
        return n;
    }

    /** ((26.3) A car parked at station f with its officers sitting in it, waiting for a call, or null. */
    Vehicle seatedCar(City.Facility f) {
        for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
            Vehicle o = fleet.vehicles.get(i);
            if (o.standby && o.type == CRUISER && !o.broken && !FleetMedevac.needsRepair(o) && !o.crew.isEmpty() && o.squad.isEmpty()
                    && World.hypot(o.x - f.x, o.y - f.y) < f.r + 300) return o;
        }
        return null;
    }

    /** ((26.3) A waiting car's officers drive it out: to the call (lights on), or with none, out on patrol. */
    boolean launch(Vehicle v, City.Facility f, Dispatch.Incident inc) {
        v.standby = false;
        v.parked = false;
        v.patrol = true;
        v.station = f;
        if (f.fed) v.agency = 4;
        if (v.number == 0) v.number = ++fleet.unitCount;
        v.stuckTimer = 0;
        if (inc != null ? respond(v, inc) : (v.state = PATROL) == PATROL && fleet.routing.newDestination(v)) return true;
        v.standby = true;
        v.parked = true;
        v.patrol = false;
        v.state = IDLE;
        v.incident = null;
        return false;
    }

    /**
     * ((26.3) The cars parked at each station have two officers sitting in them, waiting for a call - at the start (fill),
     * free officers at the station, two always left on their feet; later, those who drove a car back - and they get out
     * to fight if the dead come to the station.
     */
    void seatStandby(boolean fill) {
        for (City.Facility f : city.facilities) {
            if (f.kind != City.FACILITY_POLICE || f.fallen) continue;
            boolean danger = w.countZombiesNear(f.x, f.y, f.r + 200) > 0;
            ArrayList<Vehicle> empty = new ArrayList<Vehicle>();
            for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
                Vehicle o = fleet.vehicles.get(i);
                if (!o.standby || o.type != CRUISER || o.broken || !o.squad.isEmpty() || World.hypot(o.x - f.x, o.y - f.y) >= f.r + 300) continue;
                if (danger && !o.crew.isEmpty()) fleet.crews.crewOut(o, true);
                else if (o.crew.isEmpty()) empty.add(o);
            }
            if (!fill || danger || empty.isEmpty()) continue;
            ArrayList<Entity> free = new ArrayList<Entity>();
            for (int i = 0, n = w.entities.size(); i < n; i++) {
                Entity e = w.entities.get(i);
                if (e.dead || e.type != Entity.COP || e.task != Dispatch.T_NONE || e.rig != null || e.ride != null || e == w.controlled) continue;
                if (e.role != Entity.ROLE_RIFLE || e.outsider || (e.agency == 4) != f.fed || World.footPatrol(e)) continue;
                if (World.hypot(e.x - f.x, e.y - f.y) < 220) free.add(e);
            }
            for (int k = 0; k < empty.size() && free.size() >= 4; k++) {
                Vehicle v = empty.get(k);
                for (int j = 0; j < 2; j++) fleet.crews.board(v, free.remove(free.size() - 1));
                v.passengerType = Entity.COP;
            }
        }
    }

    /** The nearest patrol car free to answer a call. */
    Vehicle patrolFor(float x, float y) {
        Vehicle best = null;
        float bd = Float.MAX_VALUE;
        for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (!v.patrol || v.state != PATROL || v.broken || v.parked || v.crew.isEmpty() || FleetMedevac.needsRepair(v)) continue;
            // Highway patrol and sheriffs keep to their own roads until the town is under attack; then they
            // come in as backup (when no town car is nearer). Rangers stay out in the country.
            boolean town = city.inTown(x, y) && !city.nearHighway(x, y, 300);
            if (town && (v.agency == 3 || (v.agency > 0 && w.alert < 2))) continue;
            float d = (v.x - x) * (v.x - x) + (v.y - y) * (v.y - y);
            if (town && v.agency > 0) d += 500 * 500;
            if (d < bd) {
                bd = d;
                best = v;
            }
        }
        return best;
    }

    /** Sends a patrol car to a call, lights and siren. */
    /**
     * (10.45) Shots fired close by, and someone in a fight with several of the dead: a patrol car on its rounds
     * with officers aboard turns and goes to help.
     */
    void heardFight(float x, float y, float radius, float tx, float ty) {
        Dispatch.Incident inc = null;
        for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (v.type != CRUISER || !v.patrol || v.state != PATROL || v.crew.isEmpty() || v.broken || v.player != null) continue;
            if ((v.x - x) * (v.x - x) + (v.y - y) * (v.y - y) > radius * radius) continue;
            if (inc == null) inc = w.dispatch.sighting(tx, ty, null);
            if (inc == null) return;
            v.incident = inc;
            if (respond(v, inc)) {
                v.place = inc.place;
                w.dispatch.chatter(Dispatch.WHO_POLICE, v.crew.get(0), callsign(v) + ", shots fired at " + inc.place + ". Responding.", tx, ty);
            } else v.incident = null;
        }
    }

    /** (10.45) Everything on its way to one call goes on to another instead (where the dead have gone). */
    void redirect(Dispatch.Incident from, Dispatch.Incident to) {
        // ((16.2) Calls merge with one close by: those on their way keep the way they have, and arrive among it all
        // the same. A new search each - a dozen in one frame - was a stutter of up to 80 ms.)
        boolean near = World.hypot(from.x - to.x, from.y - to.y) < 400;
        for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (v.incident != from) continue;
            v.incident = to;
            v.place = to.place;
            if (v.state == DRIVE && !near && !fleet.routing.route(v, to.x, to.y)) v.incident = from;
        }
    }

    boolean respond(Vehicle v, Dispatch.Incident inc) {
        v.state = DRIVE;
        if (!fleet.routing.route(v, inc.x, inc.y)) {
            v.state = PATROL;
            return false;
        }
        v.incident = inc;
        v.stuckTimer = 0;
        w.emit(Sfx.SIREN, v.x, v.y);
        w.dispatch.chatter(Dispatch.WHO_POLICE, null, "Dispatch: " + callsign(v) + ", respond to " + inc.place + ".", inc.x, inc.y);
        if (!v.crew.isEmpty()) w.dispatch.chatter(Dispatch.WHO_POLICE, v.crew.get(0), callsign(v) + ", en route.", v.x, v.y);
        return true;
    }

    void stockStations() {
        for (City.Facility f : city.facilities) {
            if (f.kind != City.FACILITY_POLICE) continue;
            int have = 0, officers = 0;
            for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
                Vehicle v = fleet.vehicles.get(i);
                if (v.type == CRUISER && v.standby && World.hypot(v.x - f.x, v.y - f.y) < f.r + 300) have++;
            }
            for (int i = 0, n = w.entities.size(); i < n; i++) {
                Entity e = w.entities.get(i);
                if (!e.dead && e.type == Entity.COP && World.hypot(e.x - f.x, e.y - f.y) < f.r + 250) officers++;
            }
            int want = Math.max(2, Math.min(8, (officers + 1) / 2)) - have;
            float[] g = city.nearestDrivable(f.gateX > 0 ? f.gateX : f.x, f.gateY > 0 ? f.gateY : f.y);
            for (int k = 0; k < want && g != null; k++) {
                float[] p = fleet.armyUnits.bay(g, f);
                if (p == null) break;
                Vehicle v = fleet.make(CRUISER);
                v.x = p[0];
                v.y = p[1];
                v.homeX = p[0];
                v.homeY = p[1];
                v.angle = (float) Math.atan2(f.y - p[1], f.x - p[0]);
                v.depot = f;
                if (f.fed) v.agency = 4;
                v.state = IDLE;
                v.speed = 0;
                v.parked = true;
                v.standby = true;
                fleet.vehicles.add(v);
            }
        }
        // ((26.3) And their officers in them, waiting.)
        seatStandby(true);
    }

    /**
     * A patrol car: drives its beat by the rules of the road, stops and gets out when the officers see
     * one of them, answers calls with lights and siren, and drives on once its officers are back in.
     */
    /** (11.1) Someone of the crew still walking up to the car and nearly there (they come in off the beat now). */
    boolean partnerComing(Vehicle v) {
        // ((12.0) From the list of who belongs to which car: asked every frame while a car waits, it looked through everyone.)
        ArrayList<Entity> mine = fleet.crews.onFoot(v);
        for (int i = 0; i < mine.size(); i++) {
            Entity e = mine.get(i);
            if (!e.dead && e.rig == v && e.task == Dispatch.T_BOARD && Math.abs(e.x - v.x) < 400 && Math.abs(e.y - v.y) < 400) return true;
        }
        return false;
    }

    /**
     * ((22.0) A motorcycle officer scouts as they ride: a pack of the dead they pass is called in and marked for
     * the others, like the army's recon.
     */
    void bikeScout(Vehicle v, float dt) {
        if ((v.reconCd -= dt) > 0) return;
        v.reconCd = 1;
        int n = w.countZombiesNear(v.x, v.y, 220);
        if (n < 6) return;
        for (int i = 0; i < w.dispatch.incidents.size(); i++) {
            Dispatch.Incident inc = w.dispatch.incidents.get(i);
            if (!inc.resolved && World.hypot(inc.x - v.x, inc.y - v.y) < 280) return;
        }
        Entity z = w.nearestZombie(v.x, v.y, 220);
        if (z != null) w.dispatch.sighting(z.x, z.y, null);
        fleet.bikeSightings++;
        if (w.time - v.reconSaid > 25) {
            v.reconSaid = w.time;
            w.dispatch.say(Dispatch.WHO_POLICE, v.crew.get(0), callsign(v) + ": " + (n >= 10 ? "Horde of about " + n / 5 * 5 : World.count(n, "of the dead", "of the dead"))
                    + " on " + city.placeName(v.x, v.y) + ". Calling it in.", v.x, v.y);
        }
    }

    /** ((22.0) A motorcycle officer put down on the nearest road, out on patrol (the Cop picker's Motorcycle cop). */
    Entity startBikeAt(float x, float y) {
        float[] start = city.nearestDrivable(x, y);
        if (start == null) return null;
        Vehicle v = fleet.make(CRUISER);
        v.x = start[0];
        v.y = start[1];
        v.angle = w.rnd.nextInt(4) * (float) Math.PI / 2;
        v.patrol = true;
        v.station = city.nearestFacility(City.FACILITY_POLICE, x, y);
        v.state = PATROL;
        v.number = ++fleet.unitCount;
        if (!fleet.routing.newDestination(v)) return null;
        fleet.setKind(v, K_BIKE);
        Entity e = w.create(Entity.COP, v.x, v.y);
        e.rig = v; fleet.crews.note(e);
        v.crew.add(e);
        fleet.vehicles.add(v);
        return e;
    }

    boolean updatePatrol(Vehicle v, float dt) {
        if (v.kind == K_BIKE && !v.crew.isEmpty() && w.zombies > 0 && v.state != MUSTER) bikeScout(v, dt);
        v.checkCd -= dt;
        boolean check = v.checkCd <= 0;
        if (check) v.checkCd = 0.5f;
        if (v.state == MUSTER) return patrolMuster(v, dt);
        if (v.state == SCENE || v.state == RECALL) return patrolAtScene(v, check);
        if (v.agency == 1 && trafficStop(v, dt)) return false;
        // Officers spot one of them from the car: pull up and get out (well short of a crowd of them). A
        // moving car looks every moment, so it doesn't drive on into them between looks.
        boolean look = check || (Math.abs(v.speed) > 15 && w.zombieWithin(v.x, v.y, 190));
        // ((12.0) Not in the middle of a junction, though: it drives on out of it first. Stopped there for the scene,
        // it blocked the junction for everyone until its officers came back.)
        if (look && v.crew.size() > 0 && (city.junctionIdAt(v.x, v.y) < 0 || Math.abs(v.speed) < 3) && (fleet.service.hordeAhead(v)
                || (w.countZombiesNear(v.x, v.y, 90) > 0 && w.countZombiesNear(v.x, v.y, 30) < 4))) return patrolContact(v);
        if (v.state == DRIVE && v.ammoRun != null) return ammoRunStep(v, dt);
        if (v.state == DRIVE) return patrolToCall(v, dt);
        return patrolBeat(v, dt, check);
    }

    /** On its beat: an ordinary car on the road, keeping to the rules. */
    boolean patrolBeat(Vehicle v, float dt, boolean check) {
        // On patrol: an ordinary car on the road, keeping to the rules.
        if (v.crew.isEmpty()) {
            v.parked = true;
            return false;
        }
        if (check && crewNeedsAmmo(v) && ammoRun(v)) return false;
        float limit = fleet.driving.junctionRule(v, dt);
        boolean person = w.personAhead(v.x, v.y, v.angle);
        // Brake for someone in the road, then creep through (in a crowd nobody gets anywhere otherwise).
        if (fleet.trains.trainComing(v) || (person && v.timer < 2.5f)) {
            v.speed = Math.max(0, v.speed - dt * 200);
            if (person) v.timer += dt;
            return false;
        }
        if (!person) v.timer = 0;
        v.stuckTimer += dt;
        boolean queue = fleet.driving.carAhead(v);
        if ((v.held || queue) && !fleet.routing.gridlocked(v)) v.stuckTimer = Math.min(v.stuckTimer, 1);
        if (!fleet.driving.driveStep(v, dt, Math.min(limit, person ? 15 : city.onHighway(v.x, v.y) ? 105 : city.onSlip(v.x, v.y) || city.onBelt(v.x, v.y) ? 95 : 60), queue ? 0.15f : 1f) || v.stuckTimer > 10) {
            v.stuckTimer = 0;
            fleet.routing.newDestination(v);
        } else if (v.orbited) {
            // ((11.5) Gone round in a circle - a turn too tight for it off a wide road - on its rounds: somewhere else
            // instead, rather than round the block again and again.)
            v.orbited = false;
            v.destCd = 0;
            fleet.routing.newDestination(v);
        }
        return false;
    }

    /** A patrol car at the station waiting for its officers to climb in. */
    boolean patrolMuster(Vehicle v, float dt) {
        v.speed = 0;
        v.timer += dt;
        if (v.crew.size() >= v.crewWanted || (v.timer > 12 && !v.crew.isEmpty() && (v.timer > 40 || !partnerComing(v)))) {
            fleet.service.dropCrew(v);
            // ((10.41) Mounted up for a call: off to it with the lights on; a patrol car after.)
            Dispatch.Incident inc = v.incident;
            if (crewNeedsAmmo(v) && ammoRun(v)) return false;
            if (inc != null && !inc.resolved && respond(v, inc)) return false;
            v.incident = null;
            v.state = PATROL;
            if (!fleet.routing.newDestination(v)) v.stuckTimer = 99;
        } else if (v.timer > 40) {
            fleet.service.dropCrew(v);
            v.parked = true;
        }
        return false;
    }

    /** A patrol car stopped at a scene (or calling its officers back to it): it waits while they deal with it, then takes them on. */
    boolean patrolAtScene(Vehicle v, boolean check) {
        v.speed = 0;
        if (!check) return false;
        if (v.state == SCENE) {
            // Waits while its officers deal with it, then calls them back. (10.48) Not for those who've been
            // given somewhere else to be - a perimeter, a post, another call a long way off: the car goes on
            // without them (and they find their own way, or another car, later).
            // ((12.0) Its own people, from the list of who belongs to which car: these four looked through
            // everyone in the city, for every car at a scene.)
            ArrayList<Entity> mine = fleet.crews.onFoot(v);
            for (int i = 0; i < mine.size(); i++) {
                Entity e = mine.get(i);
                if (e.dead || e.rig != v) continue;
                if (e.task == Dispatch.T_CORDON || e.task == Dispatch.T_POST || e.task == Dispatch.T_GUARD
                        || World.hypot(e.x - v.x, e.y - v.y) > 500) e.rig = null;
            }
            // ((10.60) One of its officers out of rounds, with the station a long walk off: everyone back in
            // the car, and it drives them there to restock instead of them walking it with the dead behind.)
            for (int i = 0; i < mine.size(); i++) {
                Entity e = mine.get(i);
                if (e.dead || e.rig != v || e.task != Dispatch.T_RESUPPLY || World.hypot(e.x - v.x, e.y - v.y) > 320) continue;
                City.Facility f = w.restockFor(e);
                if (f == null || World.hypot(f.x - v.x, f.y - v.y) < 450) continue;
                v.onward = null;
                v.state = RECALL;
                v.timer = 0;
                return false;
            }
            boolean busy = w.countZombiesNear(v.x, v.y, 130) > 0;
            // ((10.56) Its officers sent on to another call a fair way off, and nothing left here: they come
            // back to the car and it drives them there, instead of it waiting here while they walk.)
            if (!busy && v.onward == null) {
                for (int i = 0; i < mine.size(); i++) {
                    Entity e = mine.get(i);
                    if (e.dead || e.rig != v || e.task != Dispatch.T_RESPOND || e.incident == null || e.incident == v.incident || e.incident.resolved) continue;
                    if (World.hypot(e.incident.x - v.x, e.incident.y - v.y) < 450) continue;
                    v.onward = e.incident;
                    v.state = RECALL;
                    v.timer = 0;
                    return false;
                }
            }
            for (int i = 0; i < mine.size() && !busy; i++) {
                Entity e = mine.get(i);
                if (!e.dead && e.rig == v && e.task == Dispatch.T_RESPOND) busy = true;
            }
            v.quiet = busy ? 0 : v.quiet + 0.5f;
            if (fleet.crews.crewOnFoot(v) == 0 && v.crew.isEmpty()) {
                v.parked = true;
                v.state = ABANDONED;
                return false;
            }
            if (v.quiet >= 6) {
                v.state = RECALL;
                v.timer = 0;
            }
            return false;
        }
        v.timer += 0.5f;
        int out = fleet.crews.recall(v);
        if (out > 0 && v.timer < (v.onward != null ? 20 : 40)) return false;
        fleet.service.dropCrew(v);
        Dispatch.Incident onward = v.onward;
        v.onward = null;
        if (v.crew.isEmpty()) {
            v.parked = true;
            v.state = ABANDONED;
            return false;
        }
        // ((10.60) Out of rounds: to the station to restock first, not on to the next call with empty guns.)
        if (crewNeedsAmmo(v) && ammoRun(v)) return false;
        if (onward != null && !onward.resolved && respond(v, onward)) return false;
        v.state = PATROL;
        v.incident = null;
        if (!fleet.routing.newDestination(v)) v.stuckTimer = 99;
        return false;
    }

    /** The officers have seen the dead from the car: it stops where it is and they get out. */
    boolean patrolContact(Vehicle v) {
v.speed = 0;
v.state = SCENE;
v.quiet = 0;
Dispatch.Incident inc = v.incident;
ArrayList<Entity> out = new ArrayList<Entity>(v.crew);
fleet.crews.crewOut(v, false);
if (inc != null && !inc.resolved) {
    for (Entity e : out) {
        e.task = Dispatch.T_RESPOND;
        e.incident = inc;
        e.onScene = false;
    }
} else if (v.soundCd <= 0 && !out.isEmpty()) {
    v.soundCd = 4;
    w.dispatch.chatter(Dispatch.WHO_POLICE, out.get(0), callsign(v) + ", contact on "
            + city.placeName(v.x, v.y) + ". We're out of the car.", v.x, v.y);
}
return false;
    }

    /** A patrol car on its way to a call, lights and siren: there, or stopped short of it, its officers get out. */
    boolean patrolToCall(Vehicle v, float dt) {
        Dispatch.Incident inc = v.incident;
        if (inc == null || inc.resolved) {
            v.state = PATROL;
            v.incident = null;
            if (!fleet.routing.newDestination(v)) v.stuckTimer = 99;
            return false;
        }
        if (v.soundCd <= 0) {
            v.soundCd = 3.5f;
            w.emit(Sfx.SIREN, v.x, v.y);
        }
        v.stuckTimer += dt;
        // (10.45) The dead spotted on the way: pull up and deal with them first.
        Dispatch.Incident here = fleet.service.spotOnTheWay(v, dt);
        if (here != null) {
            v.incident = here;
            inc = here;
        }
        boolean moving = here == null && fleet.driving.driveStep(v, dt, v.kind == K_BIKE ? 130 : 115, fleet.driving.carAhead(v) ? 0.5f : 1f);
        if (moving) fleet.driving.hit(v, w.runOver(v.x, v.y, 8, v.speed, v.angle));
        if (!moving || ((World.hypot(inc.x - v.x, inc.y - v.y) < 50 || v.stuckTimer > 6) && city.junctionIdAt(v.x, v.y) < 0)) {
            v.speed = 0;
            v.state = SCENE;
            v.quiet = 0;
            ArrayList<Entity> out = new ArrayList<Entity>(v.crew);
            fleet.crews.crewOut(v, false);
            for (Entity e : out) {
                e.task = Dispatch.T_RESPOND;
                e.incident = inc;
                e.onScene = false;
            }
        }
        return false;
    }

    /** (10.60) Officers aboard short of rounds: any of them down to less than a magazine, spare rounds and all. */
    boolean crewNeedsAmmo(Vehicle v) {
        for (int i = 0; i < v.crew.size(); i++) {
            Entity e = v.crew.get(i);
            if (e.hasGun && e.magSize > 0 && e.ammo + e.reserve < e.magSize) return true;
        }
        return false;
    }

    /**
     * (10.60) A patrol car drives its officers to the nearest station or armoury with ammunition left, lights on,
     * and they restock there without getting out. False if there's nowhere left with any, or no road to it.
     */
    boolean ammoRun(Vehicle v) {
        if (v.crew.isEmpty() || v.player != null) return false;
        City.Facility f = w.restockFor(v.crew.get(0));
        if (f == null || !fleet.routing.route(v, f.x, f.y)) return false;
        v.state = DRIVE;
        v.incident = null;
        v.onward = null;
        v.ammoRun = f;
        v.stuckTimer = 0;
        w.dispatch.chatter(Dispatch.WHO_POLICE, v.crew.get(0), callsign(v) + ", we're nearly out of rounds. Heading to " + f.name + " to restock.", v.x, v.y);
        return true;
    }

    /** (10.60) On the way to restock; there, everyone aboard fills up and the car goes back on patrol, free for calls. */
    boolean ammoRunStep(Vehicle v, float dt) {
        City.Facility f = v.ammoRun;
        v.stuckTimer += dt;
        boolean moving = fleet.driving.driveStep(v, dt, 100, fleet.driving.carAhead(v) ? 0.5f : 1f);
        if (moving) fleet.driving.hit(v, w.runOver(v.x, v.y, 8, v.speed, v.angle));
        float d = (float) World.hypot(f.x - v.x, f.y - v.y);
        if (moving && d > Math.max(60, f.r) && v.stuckTimer < 8) return false;
        v.speed = 0;
        v.ammoRun = null;
        if (d < f.r + 160 && w.restockCrew(v.crew, f) > 0 && !v.crew.isEmpty())
            w.dispatch.say(Dispatch.WHO_POLICE, v.crew.get(0), callsign(v) + ", restocked at " + f.name + ". Available.", v.x, v.y);
        else if (crewNeedsAmmo(v)) {
            // (Couldn't get through: out, and the rest of the way on foot.)
            fleet.crews.crewOut(v, false);
            return false;
        }
        v.state = PATROL;
        if (!fleet.routing.newDestination(v)) v.stuckTimer = 99;
        return false;
    }
}
