package com.instantaxtion.zombiesandbox;

import java.io.File;
import java.lang.reflect.Field;

/** Vehicles and dispatch: traffic, junctions, steering, calls and the radio. */
final class DrivingTests extends TestKit {
    static void register() {
        test("driving: traffic keeps its distance, junctions don't lock, no circles, patrol cars don't spin", FAST, 40, DrivingTests.class,
                "trafficDistance", "junctions", "noCircles", "noSpins");
        test("vehicles: steering clear of the dead, the player at the wheel, alarms, burning wrecks and the shelter van", FULL, 20, DrivingTests.class,
                "steerClear", "letGoOfTheWheel", "takeControl", "carsAndVan");
        test("dispatch: calls reach the map and follow the dead, infection zones, hot spots, a quieter radio, unit history; drones and the helicopter mark the dead", FAST, 30, DrivingTests.class,
                "callsBoard", "infectionZones", "radioAndHotSpots", "historyAndRadio", "droneMarks", "policeDrone", "helicopterMarks");
    }

    static void trafficDistance() throws Exception {
        CityConfig c = cfg(0, CityConfig.MEDIUM, 3);
        World w = new World(c);
        w.populate(c);
        w.viewX0 = 0;
        w.viewY0 = 0;
        w.viewX1 = w.city.worldW();
        w.viewY1 = w.city.worldH();
        java.util.HashMap<Fleet.Vehicle, Float> cd = new java.util.HashMap<Fleet.Vehicle, Float>();
        int crashes = 0, onRoad = 0, wrong = 0;
        for (int f = 0; f < 30 * 90; f++) {
            w.update(1 / 30f);
            w.evCount = 0;
            for (Fleet.Vehicle v : w.fleet.vehicles) {
                Float prev = cd.get(v);
                cd.put(v, v.crashCd);
                if (prev != null && v.crashCd > prev + 0.05f && v.crashCd > 0.55f) crashes++;
                if (!v.patrol || v.parked || v.speed < 10 || f % 10 != 0) continue;
                int tx = (int) (v.x / City.T), ty = (int) (v.y / City.T);
                if (w.city.junctionIdAt(v.x, v.y) >= 0 || w.city.roadDirAt(tx, ty) == 0) continue;
                float fx = (float) Math.cos(v.angle), fy = (float) Math.sin(v.angle);
                int dx = Math.abs(fx) > Math.abs(fy) ? (int) Math.signum(fx) : 0, dy = dx == 0 ? (int) Math.signum(fy) : 0;
                float act = (v.x - (tx + 0.5f) * City.T) * -dy + (v.y - (ty + 0.5f) * City.T) * dx;
                float mine = w.city.laneOffset(tx, ty, dx, dy), theirs = -w.city.laneOffset(tx, ty, -dx, -dy);
                if (Math.abs(mine - theirs) < 6) continue;
                onRoad++;
                if (Math.abs(act - theirs) + 3 < Math.abs(act - mine)) wrong++;
            }
        }
        // (Each crash shows up on both cars.)
        // (About one crash for every 13 vehicles about at most: the bigger maps since 10.10 have more traffic.)
        check(crashes / 2 <= Math.max(6, w.fleet.vehicles.size() / 13), (crashes / 2) + " crashes in a minute and a half of ordinary traffic ("
                + w.fleet.vehicles.size() + " vehicles)");
        check(onRoad > 50, "patrol cars out on the roads (" + onRoad + " samples)");
        check(wrong <= onRoad * 0.04f, "patrol cars on the wrong side " + wrong + " of " + onRoad);
    }

    static void junctions() throws Exception {
        // (A Small town, not a Tiny one: a whole police force on patrol in a Tiny town on map version 40 and later can jam
        // solid - a size the game doesn't offer; the health check watches traffic at the Default size.)
        CityConfig c = cfg(CityConfig.SMALL, 23);
        World w = new World(c);
        w.populate(c);
        // A minute of town traffic, police on patrol among it.
        float paved = 0, pavement = 0;
        java.util.Map<Fleet.Vehicle, float[]> at = new java.util.HashMap<Fleet.Vehicle, float[]>();
        int stuck = 0, moving = 0;
        for (int f = 0; f < 30 * 60; f++) {
            w.update(1 / 30f);
            for (int i = 0; i < w.fleet.vehicles.size(); i++) {
                Fleet.Vehicle v = w.fleet.vehicles.get(i);
                if (v.removedFromFleet || v.player != null || Fleet.offRoad(v) || Fleet.airborne(v)) continue;
                if (v.passing > 0 && !v.parked && Math.abs(v.speed) > 1) {
                    int t = w.city.tileIndex(v.x, v.y);
                    if (t < 0) continue;
                    if (w.city.tiles[t] == City.SIDEWALK || w.city.tiles[t] == City.PLAZA) pavement += 1 / 30f;
                    else paved += 1 / 30f;
                }
                // Stuck: on its way somewhere, and not 40 units further on in 30 seconds.
                boolean trying = !v.parked && !v.broken && (v.state == Fleet.DRIVE || v.state == Fleet.RETURN || v.state == Fleet.PATROL
                        || (v.type == Fleet.CAR && v.state == Fleet.WAIT));
                float[] a = at.get(v);
                if (a == null || !trying || Math.hypot(v.x - a[0], v.y - a[1]) > 40) at.put(v, a = new float[]{v.x, v.y, w.time, 0});
                if (trying && a[3] == 0 && w.time - a[2] > 30) {
                    a[3] = 1;
                    stuck++;
                }
            }
        }
        for (int i = 0; i < w.fleet.vehicles.size(); i++)
            if (!Fleet.offRoad(w.fleet.vehicles.get(i)) && !w.fleet.vehicles.get(i).parked) moving++;
        // Going round something, it keeps to the road: never up on the pavement.
        check(paved > 1 && pavement < paved * 0.02f, "passing on the road, not the pavement (" + (int) paved + " s vs " + pavement + " s)");
        check(stuck <= Math.max(3, moving / 8), "hardly anything sat stuck for half a minute (" + stuck + " of " + moving + ")");
    }

    static void noCircles() throws Exception {
        CityConfig c = cfg(CityConfig.MASSIVE, 5);
        c.v[CityConfig.OPT_ZOMBIES] = 2;
        World w = new World(c);
        w.populate(c);
        java.util.HashMap<Fleet.Vehicle, float[]> m = new java.util.HashMap<Fleet.Vehicle, float[]>();
        int circling = 0;
        for (int f = 0; f < 30 * 120; f++) {
            w.update(1 / 30f);
            w.evCount = 0;
            for (Fleet.Vehicle v : w.fleet.vehicles) {
                if (v.type == Fleet.HELI || v.type == Fleet.JET || v.type == Fleet.TRAIN || v.parked) continue;
                float[] t = m.get(v);
                // turned so far, x0, y0, x1, y1, time, last angle, flagged
                if (t == null || t[5] > 10) {
                    t = new float[]{0, v.x, v.y, v.x, v.y, 0, v.angle, t != null ? t[7] : 0};
                    m.put(v, t);
                }
                float da = v.angle - t[6];
                while (da > Math.PI) da -= 2 * Math.PI;
                while (da < -Math.PI) da += 2 * Math.PI;
                t[0] += da;
                t[6] = v.angle;
                t[5] += 1 / 30f;
                t[1] = Math.min(t[1], v.x);
                t[2] = Math.min(t[2], v.y);
                t[3] = Math.max(t[3], v.x);
                t[4] = Math.max(t[4], v.y);
                if (t[7] == 0 && Math.abs(t[0]) > 3 * Math.PI * 2 && t[3] - t[1] < 90 && t[4] - t[2] < 90) {
                    t[7] = 1;
                    circling++;
                }
            }
        }
        check(circling == 0, circling + " vehicles went round in circles");
    }

    static void noSpins() throws Exception {
        CityConfig c = cfg(CityConfig.ARCHIPELAGO, CityConfig.LARGE, 1);
        World w = new World(c);
        City city = w.city;
        // Two streets side by side with a strip of grass between them (no boulevard's one-way sides): a car's lane
        // is on its own street, never across the grass.
        int strips = 0, across = 0;
        for (int y = 2; y < city.h - 2; y++)
            for (int x = 2; x < city.w - 2; x++) {
                if (city.tiles[y * city.w + x] != City.ROAD || (city.divided != null && city.divided[y * city.w + x] != 0)
                        || city.junctionAt(x, y) || city.hwyTile(x, y)) continue;
                for (int d = 0; d < 4; d++) {
                    int dx = d == 0 ? 1 : d == 1 ? -1 : 0, dy = d == 2 ? 1 : d == 3 ? -1 : 0;
                    boolean strip = false;
                    for (int s = -3; s <= 3; s++)
                        if (city.isMedian(x - dy * s, y + dx * s) && city.paved(x - dy * (s + Integer.signum(s)), y + dx * (s + Integer.signum(s)))) strip = true;
                    if (!strip) continue;
                    strips++;
                    float off = city.laneOffset(x, y, dx, dy, 1);
                    for (float f = 0; f <= 1.001f; f += 0.125f) {
                        int i = city.tileIndex((x + 0.5f) * City.T - dy * off * f, (y + 0.5f) * City.T + dx * off * f);
                        if (city.tiles[i] != City.ROAD && city.tiles[i] != City.CAR) {
                            across++;
                            break;
                        }
                    }
                }
            }
        check(strips > 100 && across == 0, "lanes beside a grass strip stay on their own street (" + across + " of " + strips + " across it)");
        // Forty seconds of a normal game: no patrol car turns a full circle on the spot (more than 300 degrees inside
        // 40 px in under 8 s). There were sixty-odd.
        w.populate(c);
        java.util.Map<Fleet.Vehicle, float[]> hist = new java.util.IdentityHashMap<Fleet.Vehicle, float[]>();
        final int N = 80;
        int spins = 0;
        for (int f = 0; f < 30 * 40; f++) {
            w.update(1 / 30f);
            w.evCount = 0;
            if (f % 3 != 0) continue;
            for (Fleet.Vehicle v : w.fleet.vehicles) {
                if (v.removedFromFleet || Fleet.offRoad(v) || !v.patrol) continue;
                // [0] count, [1] unwrapped angle, [2] last raw angle, [3] cooldown, then x, y, angle per sample.
                float[] h = hist.get(v);
                if (h == null) {
                    hist.put(v, h = new float[4 + N * 3]);
                    h[1] = h[2] = v.angle;
                }
                float da = v.angle - h[2];
                while (da > Math.PI) da -= 2 * Math.PI;
                while (da < -Math.PI) da += 2 * Math.PI;
                h[1] += da;
                h[2] = v.angle;
                int n = (int) h[0], at = 4 + (n % N) * 3;
                h[at] = v.x;
                h[at + 1] = v.y;
                h[at + 2] = h[1];
                h[0] = ++n;
                if (h[3] > 0) {
                    h[3]--;
                    continue;
                }
                float minX = v.x, maxX = v.x, minY = v.y, maxY = v.y;
                for (int k = 1; k < Math.min(n, N); k++) {
                    int b = 4 + ((n - 1 - k) % N) * 3;
                    minX = Math.min(minX, h[b]);
                    maxX = Math.max(maxX, h[b]);
                    minY = Math.min(minY, h[b + 1]);
                    maxY = Math.max(maxY, h[b + 1]);
                    if (maxX - minX > 80 || maxY - minY > 80) break;
                    if (Math.abs(h[1] - h[b + 2]) > Math.toRadians(300)) {
                        spins++;
                        h[3] = N;
                        break;
                    }
                }
            }
        }
        check(spins <= 20, "patrol cars don't turn circles on the spot (" + spins + " in 40 s)");
    }

    static void steerClear() throws Exception {
        CityConfig c = cfg(CityConfig.TINY, 41);
        World w = new World(c);
        w.populate(c);
        w.update(1 / 30f);
        // A police car's way across town, with none of the dead about.
        java.util.Random r = new java.util.Random(7);
        Fleet.Vehicle car = w.fleet.make(Fleet.CRUISER);
        int[] path = new int[4096];
        int n = 0;
        float[] a = null, b = null;
        for (int k = 0; k < 200 && n < 30; k++) {
            a = w.city.nearestDrivable(w.city.randomStreetSpot(r)[0], w.city.randomStreetSpot(r)[1]);
            b = w.city.nearestDrivable(w.city.randomStreetSpot(r)[0], w.city.randomStreetSpot(r)[1]);
            if (a == null || b == null || Math.hypot(a[0] - b[0], a[1] - b[1]) < 700) continue;
            car.x = a[0];
            car.y = a[1];
            if (w.fleet.routing.route(car, b[0], b[1])) n = w.fleet.routing.traceRoute(car, path);
        }
        check(n >= 30, "a way across town (" + n + ")");
        int len = tilesTo(w, car);
        // A crowd of the dead standing in the middle of it.
        int mid = path[n / 2];
        float mx = (mid % w.city.w + 0.5f) * City.T, my = (mid / w.city.w + 0.5f) * City.T;
        java.util.List<Entity> crowd = new java.util.ArrayList<Entity>();
        for (int k = 0; k < 16; k++) {
            float[] q = w.city.findWalkable(mx + (k % 4) * 5 - 8, my + (k / 4) * 5 - 8);
            if (q == null) continue;
            Entity z = w.spawn(Entity.ZOMBIE, q[0], q[1]);
            untouchable(z);
            crowd.add(z);
        }
        step(w, quiet(w, mx, my, 400, crowd.toArray()));
        check(w.zombies > 0 && near(w, path, n, crowd) > 0, "the dead on the way it planned (" + near(w, path, n, crowd) + ")");
        // Planned again: round them.
        car.x = a[0];
        car.y = a[1];
        check(w.fleet.routing.route(car, b[0], b[1]), "a new way");
        int n2 = w.fleet.routing.traceRoute(car, path);
        int len2 = tilesTo(w, car);
        check(near(w, path, n2, crowd) == 0 && len2 < len * 2, "round them, not through them, and not far out of the way (" + len + " -> " + len2 + " tiles)");
        // An APC minds them far less: its way is no longer than the car's.
        Fleet.Vehicle apc = w.fleet.make(Fleet.TRUCK);
        w.fleet.setKind(apc, Fleet.K_APC);
        apc.x = a[0];
        apc.y = a[1];
        check(w.fleet.routing.route(apc, b[0], b[1]) && tilesTo(w, apc) <= tilesTo(w, car), "armour cares less (" + tilesTo(w, apc) + " tiles, the car " + tilesTo(w, car) + ")");
        // A car on the old line with the crowd just ahead of it, going somewhere past them: it plans round them.
        // (A hundred or so short of them on the way it planned, whatever the size of a tile or the bends in the way, and
        // heading for somewhere well past them: the end of its way.)
        int before = path[0];
        float bestD = Float.MAX_VALUE;
        for (int j = 0; j < n / 2; j++) {
            float d = (float) Math.hypot((path[j] % w.city.w + 0.5f) * City.T - mx, (path[j] / w.city.w + 0.5f) * City.T - my);
            if (Math.abs(d - 105) < bestD) {
                bestD = Math.abs(d - 105);
                before = path[j];
            }
        }
        Fleet.Vehicle cruiser = w.fleet.make(Fleet.CRUISER);
        cruiser.x = (before % w.city.w + 0.5f) * City.T;
        cruiser.y = (before / w.city.w + 0.5f) * City.T;
        cruiser.angle = (float) Math.atan2(my - cruiser.y, mx - cruiser.x);
        int end = path[n - 1];
        cruiser.tx = (end % w.city.w + 0.5f) * City.T;
        cruiser.ty = (end / w.city.w + 0.5f) * City.T;
        if (Math.hypot(cruiser.tx - mx, cruiser.ty - my) < 300) {
            cruiser.tx = b[0];
            cruiser.ty = b[1];
        }
        check(w.fleet.driving.steerClear(cruiser) && w.fleet.driving.steered == 1, "the dead ahead: a new way round");
        // A wreck across the way: planned round it.
        int[] p3 = new int[4096];
        int m = w.fleet.routing.traceRoute(car, p3);
        int wall = p3[m / 2];
        check(w.fleet.routing.routeAround(car, new int[]{wall}, 1), "a way round a blockage");
        int m2 = w.fleet.routing.traceRoute(car, p3);
        boolean through = false;
        for (int i = 0; i < m2; i++) if (Math.abs(p3[i] % w.city.w - wall % w.city.w) <= 1 && Math.abs(p3[i] / w.city.w - wall / w.city.w) <= 1) through = true;
        check(!through && w.city.blockN == 0 && w.city.hazZ == null, "past it, and nothing left set for the next search");
    }

    static void letGoOfTheWheel() throws Exception {
        CityConfig c = cfg(CityConfig.TINY, 41);
        World w = new World(c);
        w.populate(c);
        w.update(1 / 30f);
        for (int kind = 0; kind < 2; kind++) {
            Fleet.Vehicle car = null;
            for (Fleet.Vehicle v : w.fleet.vehicles)
                if (!v.broken && v.player == null && !Fleet.offRoad(v) && (kind == 0 ? v.type == Fleet.CRUISER && v.patrol : v.type == Fleet.CAR && !v.parked)) {
                    car = v;
                    break;
                }
            if (car == null && kind == 1) {
                float[] q = w.city.randomStreetSpot(new java.util.Random(6));
                car = w.fleet.placeCar(q[0], q[1]);
            }
            check(car != null, kind == 0 ? "a patrol car" : "a car in traffic");
            Entity e = w.spawn(kind == 0 ? Entity.COP : Entity.CIVILIAN, car.x, car.y);
            w.controlled = e;
            w.driveInto(e, car);
            // The player takes the wheel, sits there stopped a while, and lets go.
            for (int f = 0; f < 90; f++) w.update(1 / 30f);
            check(car.player == e && Math.abs(car.speed) < 1, "stopped with the player at the wheel");
            w.letGoOfCar();
            w.controlled = null;
            float x0 = car.x, y0 = car.y;
            float moved = 0;
            for (int f = 0; f < 30 * 20; f++) {
                w.update(1 / 30f);
                moved = Math.max(moved, (float) Math.hypot(car.x - x0, car.y - y0));
            }
            // ((29.2) Or on its way and stopped at the lights: that's driving on too.)
            check(car.player == null && car.state != Fleet.ABANDONED && (moved > 60 || (moved > 30 && (car.held || car.queuedHeld))),
                    (kind == 0 ? "the patrol car" : "the car") + " drives on without the player (moved " + (int) moved + ", state " + car.state + ", model " + car.model + ", riders " + car.riders.size()
                            + ", through " + car.through + ", fleeing " + car.fleeing + ", field " + (car.field != null) + ", held " + car.held + ", speed " + (int) car.speed + ", tile " + w.city.tiles[w.city.tileIndex(car.x, car.y)] + ")");
            if (kind == 0) check(car.crew.contains(e) || !e.removed, "the officer who was driving is its crew now");
        }
    }

    static void takeControl() throws Exception {
        CityConfig c = cfg(CityConfig.LARGE, 5);
        c.v[CityConfig.OPT_ZOMBIES] = 3;
        World w = new World(c);
        w.populate(c);
        for (int i = 0; i < 30 * 20; i++) {
            w.update(1 / 30f);
            w.evCount = 0;
        }
        Entity soldier = null;
        // (One with room to walk east, not up against a wall.)
        for (Entity e : w.entities)
            if (!e.dead && e.type == Entity.SOLDIER && (soldier == null || !w.city.circleBlocked(e.x + 12, e.y, e.radius))) soldier = e;
        check(soldier != null, "a soldier to play");
        w.controlled = soldier;
        w.joyX = 1;
        float x0 = soldier.x, y0 = soldier.y;
        for (int i = 0; i < 30 * 2; i++) {
            w.update(1 / 30f);
            w.evCount = 0;
        }
        check(w.controlled != soldier || Math.hypot(soldier.x - x0, soldier.y - y0) > 20, "the stick moves them");
        // Put a zombie in front and hold the attack button.
        w.joyX = 0;
        float[] p = w.city.findWalkable(soldier.x + 40, soldier.y);
        Entity z = w.spawn(Entity.ZOMBIE, p[0], p[1]);
        int shots = w.shotsFired;
        w.ctrlAttack = true;
        for (int i = 0; i < 30; i++) {
            w.update(1 / 30f);
            w.evCount = 0;
        }
        check(w.controlled != soldier || w.shotsFired > shots, "the attack button shoots");
        // A zombie bites.
        Entity civ = null;
        for (Entity e : w.entities) if (!e.dead && e.type == Entity.CIVILIAN) civ = e;
        Entity me = w.spawn(Entity.ZOMBIE, civ.x + 5, civ.y);
        civ.stun = 5;
        w.controlled = me;
        w.joyX = 0;
        w.ctrlAttack = true;
        int bites = w.bites;
        for (int i = 0; i < 90 && w.bites == bites && !civ.dead && !civ.removed; i++) {
            // Chase them with the stick.
            float dx = civ.x - me.x, dy = civ.y - me.y, d = (float) Math.hypot(dx, dy) + 0.01f;
            w.joyX = dx / d;
            w.joyY = dy / d;
            w.update(1 / 30f);
            w.evCount = 0;
        }
        check(w.controlled != me || w.bites > bites || civ.dead || civ.removed, "a controlled zombie bites");
        w.controlled = null;
        check(!w.replayMeta.isEmpty() && w.replayMeta.size() == w.replayDots.size(), "the replay is recorded");
        File f = File.createTempFile("zcs", ".dat");
        SaveGame.save(w, f);
        World l = SaveGame.load(f);
        check(l.replayMeta.size() == w.replayMeta.size() && l.turnEvents.size() == w.turnEvents.size(), "the replay is saved");
        f.delete();
    }

    static void carsAndVan() throws Exception {
        CityConfig c = cfg(CityConfig.SMALL, 220);
        World w = new World(c);
        w.populate(c);
        w.update(1 / 30f);
        float[] p = w.city.findWalkable(w.city.worldW() / 2, w.city.worldH() / 2);
        // A parked car caught by a blast burns out.
        int carTile = -1;
        for (int i = 0; i < w.city.tiles.length && carTile < 0; i++)
            if (w.city.tiles[i] == City.CAR && w.city.carKind.get(i) <= 1) carTile = i;
        if (carTile >= 0) {
            float cx = (carTile % w.city.w + 0.5f) * City.T, cy = (carTile / w.city.w + 0.5f) * City.T;
            int chained = w.carsChained;
            w.explode(cx, cy, 60, 200);
            check(w.carsChained > chained && w.city.carKind.get(carTile) == 7, "a parked car in a blast burns out");
        }
        // A parked car knocked about sets its alarm off.
        Fleet.Vehicle car = w.fleet.placeCar(p[0], p[1]);
        if (car != null) {
            car.parked = true;
            car.speed = 0;
            car.driver = null;
            car.riders.clear();
            for (int k = 0; k < 20 && car.alarm <= 0; k++) w.fleet.tripAlarm(car, 1);
            check(car.alarm > 0, "a parked car's alarm goes off");
        }
        // The shelter van: people running from the dead climb in.
        Fleet.Vehicle van = w.fleet.placeShelterVan(p[0], p[1] - 100);
        check(van != null, "an armoured van parks");
        Entity runner = w.spawn(Entity.CIVILIAN, van.x + 40, van.y);
        runner.fleeTimer = 10;
        w.recount();
        for (int f = 0; f < 30 * 6 && !van.riders.contains(runner); f++) { runner.fleeTimer = 10; w.update(1 / 30f); w.evCount = 0; }
        check(van.riders.contains(runner), "someone running from the dead shelters in the van");
        // Driving it: the player in the driver's seat.
        check(w.canDrive(van), "the van can be driven");
        Entity me = w.spawn(Entity.COP, van.x, van.y);
        w.controlled = me;
        w.driveInto(me, van);
        check(van.player == me, "the player takes the wheel");
    }

    static void callsBoard() throws Exception {
        CityConfig c = cfg(0, CityConfig.LARGE, 3);
        World w = new World(c);
        w.populate(c);
        for (int f = 0; f < 30; f++) w.update(1 / 30f);
        Dispatch d = w.dispatch;
        // A full board of calls gone quiet: a new 911 call still gets on the map (a quiet one makes way).
        java.util.Random r = new java.util.Random(5);
        // ((10.51) The board holds more in a bigger city: d.maxCalls(). (10.52) From empty: a call opened in the
        // first second would leave it one over.)
        d.incidents.clear();
        Entity caller = null;
        for (Entity e : w.entities) if (e.type == Entity.CIVILIAN && !e.dead && w.city.inTown(e.x, e.y)) caller = e;
        float[] zp = w.city.findWalkable(caller.x + 50, caller.y);
        Entity z = w.spawn(Entity.ZOMBIE, zp[0], zp[1]);
        // (None of them where the new call is: one within 200 would take the call as its own.)
        while (d.incidents.size() < d.maxCalls()) {
            float[] q = w.city.randomWalkableInTown(r);
            if (Math.hypot(q[0] - z.x, q[1] - z.y) < 300) continue;
            Dispatch.Incident inc = new Dispatch.Incident();
            inc.x = q[0];
            inc.y = q[1];
            inc.place = "test";
            d.incidents.add(inc);
        }
        boolean before = w.callsLost;
        w.callsLost = false;
        d.call(caller, z);
        w.callsLost = before;
        boolean onMap = false;
        for (Dispatch.Incident inc : d.incidents) if (Math.hypot(inc.x - z.x, inc.y - z.y) < 30) onMap = true;
        check(onMap && d.incidents.size() <= d.maxCalls(), "a call made with the board full still shows (" + d.incidents.size() + " open)");
        // A call with nothing at it closes in seconds; one whose dead have moved on goes with them.
        d.incidents.clear();
        w.clearZombies();
        float[] a = w.city.randomWalkableInTown(r);
        Dispatch.Incident empty = d.sighting(a[0], a[1], null);
        float[] b = null;
        for (int k = 0; k < 400 && b == null; k++) {
            float[] q = w.city.randomWalkableInTown(r);
            if (Math.hypot(q[0] - a[0], q[1] - a[1]) > 900) b = q;
        }
        Dispatch.Incident moving = d.sighting(b[0], b[1], null);
        float[] zq = w.city.findWalkable(b[0] + 200, b[1]);
        Entity walker = w.spawn(Entity.ZOMBIE, zq[0], zq[1]);
        walker.stun = 30;
        for (int f = 0; f < 45; f++) w.update(1 / 30f);
        // (Unless there's a call there already: then this one hands over to it and closes.)
        boolean covered = false;
        for (Dispatch.Incident o : d.incidents) if (Math.hypot(o.x - walker.x, o.y - walker.y) < 60) covered = true;
        check(covered && (Math.hypot(moving.x - walker.x, moving.y - walker.y) < 60 || moving.resolved), "the call goes with the dead ("
                + (int) Math.hypot(moving.x - walker.x, moving.y - walker.y) + " away)");
        for (int f = 0; f < 30 * 15; f++) w.update(1 / 30f);
        check(!d.incidents.contains(empty), "a call with none of the dead near it is closed within seconds");
        // Seen from the road on the way to a call: a sighting is put on the map there.
        int open = d.incidents.size();
        float[] s2 = w.city.randomWalkableInTown(r);
        Dispatch.Incident seen = d.sighting(s2[0], s2[1], null);
        check(seen != null && d.incidents.size() == open + 1 && d.sighting(s2[0] + 30, s2[1], null) == seen,
                "a sighting from a passing unit becomes a call (and the same spot isn't reported twice)");
    }

    static void infectionZones() throws Exception {
        CityConfig c = cfg(0, CityConfig.LARGE, 5);
        World w = new World(c);
        w.populate(c);
        for (int f = 0; f < 150; f++) w.update(1 / 30f);
        java.util.Random r = new java.util.Random(7);
        float[] q0 = w.city.randomWalkableInTown(r);
        int sp = 0;
        for (int i = 0; i < 1600 && sp < 150; i++) {
            float[] q = w.city.findWalkable(q0[0] + (i % 40) * 8 - 160, q0[1] + (i / 40) * 8 - 160);
            if (q != null && w.spawn(Entity.ZOMBIE, q[0], q[1]) != null) sp++;
        }
        // And a few on their own, a long way off: a lesser zone, if one at all.
        float[] q1 = null;
        for (int k = 0; k < 200 && (q1 == null || Math.hypot(q1[0] - q0[0], q1[1] - q0[1]) < 2000); k++) q1 = w.city.randomWalkableInTown(r);
        for (int i = 0; i < 400 && sp < 160; i++) {
            float[] q = w.city.findWalkable(q1[0] + (i % 20) * 8 - 80, q1[1] + (i / 20) * 8 - 80);
            if (q != null && w.spawn(Entity.ZOMBIE, q[0], q[1]) != null) sp++;
        }
        w.alert = 2;
        w.outbreak = true;
        boolean declared = false, roads = true, invariant = true, sent = false, staleBefore = false;
        int perimeter = 0, checkpoints = 0;
        for (int f = 0; f < 30 * 60; f++) {
            w.update(1 / 30f);
            if (f % 30 != 0) continue;
            for (World.InfectionZone z : w.infection) {
                if (Math.hypot(z.x - q0[0], z.y - q0[1]) < z.r + 200) declared = true;
                // The worst is always held; one that's waiting its turn has no checkpoints and nobody on its line.
                if (z == w.infection.get(0) && !z.critical) invariant = false;
                if (!z.critical && !z.checkpoints.isEmpty()) invariant = false;
                for (float[] cp : z.checkpoints) {
                    if (!w.city.checkpoints.contains(cp)) roads = false;
                    if (w.city.tiles[(int) (cp[1] / City.T) * w.city.w + (int) (cp[0] / City.T)] != City.ROAD) roads = false;
                }
                checkpoints = Math.max(checkpoints, z.checkpoints.size());
                for (int k = 0; k < z.guardSpots.size() && k < z.guards.length; k++) {
                    Entity g = z.guards[k];
                    if (g != null && g.type == Entity.SOLDIER && (!g.dead || World.riding(g))) sent = true;
                }
            }
            int n = 0;
            // (A unit whose zone has just moved off its post lets it go at its next look: not held two samples running.)
            boolean stale = false;
            for (Entity e : w.entities) {
                if (e.dead || e.task != Dispatch.T_CORDON) continue;
                n++;
                World.InfectionZone z = w.zoneOfPost(e.postX, e.postY);
                if (z == null || !z.critical) stale = true;
            }
            if (stale && staleBefore) invariant = false;
            staleBefore = stale;
            perimeter = Math.max(perimeter, n);
        }
        check(declared, "an infection zone is declared round the bunch of the dead");
        // ((27.5) No checkpoints put up on the roads in, in the middle of a game, any more.)
        check(checkpoints == 0 && roads, "no checkpoints put up on the roads into it (" + checkpoints + ")");
        check(perimeter >= 4, "police on its perimeter, not called there by anyone (" + perimeter + ")");
        check(invariant, "the worst zone is held first; the others wait, with nobody on them, till there's someone for them");
        // Not lifted while any of them are about; lifted, roads and all, once they're gone.
        boolean any = !w.infection.isEmpty();
        // (Kept clear: anyone bitten who turns meanwhile is taken care of too.)
        for (int f = 0; f < 30 * 25; f++) {
            if (f % 15 == 0) w.clearZombies();
            w.update(1 / 30f);
        }
        check(!any || w.infection.isEmpty(), "with the dead gone the zone is lifted (" + w.infection.size() + " left)");
        int left = 0;
        for (Entity e : w.entities) if (!e.dead && e.task == Dispatch.T_CORDON) left++;
        check(left == 0, "and everyone on its perimeter stands down (" + left + ")");
    }

    static void radioAndHotSpots() throws Exception {
        CityConfig c = cfg(CityConfig.SMALL, 12);
        World w = new World(c);
        w.populate(c);
        Dispatch d = w.dispatch;
        Entity cop = null;
        for (Entity e : w.entities) if (!e.dead && e.type == Entity.COP) cop = e;
        int before = d.messageCount;
        d.chatter(Dispatch.WHO_POLICE, cop, "10-4, en route.", cop.x, cop.y);
        check(d.messageCount == before && cop.talkTimer > 0, "routine talk: seen to talk, not on the radio");
        d.say(Dispatch.WHO_INFO, null, "Info: Stampede on Main St! People are being trampled.", 0, 0);
        d.say(Dispatch.WHO_INFO, null, "Info: Stampede on Oak Ave! People are being trampled.", 0, 0);
        check(d.messageCount == before + 1, "the same news twice in a row goes out once");
        // ((27.5) Within half a minute it's counted onto the line already there instead.)
        for (int f = 0; f < 30 * 32; f++) w.update(1 / 30f);
        int at = d.messageCount;
        d.say(Dispatch.WHO_INFO, null, "Info: Stampede on Elm Rd! People are being trampled.", 0, 0);
        check(d.messageCount == at + 1, "and again once a while has passed");
        // Two fights: the bigger one first, with its numbers.
        w.clearZombies();
        d.incidents.clear();
        java.util.Random r = new java.util.Random(2);
        float[][] spots = new float[2][];
        for (int k = 0; k < 2; k++) {
            float[] q;
            do q = w.city.randomWalkableInTown(r);
            while (k == 1 && Math.hypot(q[0] - spots[0][0], q[1] - spots[0][1]) < 800);
            spots[k] = q;
            for (int z = 0; z < (k == 0 ? 3 : 7); z++) {
                float[] p = w.city.findWalkable(q[0] + (z % 3) * 8, q[1] + (z / 3) * 8);
                w.spawn(Entity.ZOMBIE, p[0], p[1]);
            }
            Dispatch.Incident inc = new Dispatch.Incident();
            inc.x = q[0];
            inc.y = q[1];
            inc.place = "spot " + k;
            d.incidents.add(inc);
        }
        w.update(1 / 30f);
        java.util.List<World.HotSpot> hot = w.hotSpots(8);
        check(hot.size() == 2, "both fights listed (" + hot.size() + ")");
        if (hot.size() == 2) {
            check(hot.get(0).place.equals("spot 1") && hot.get(0).dead >= 6 && hot.get(1).dead >= 2,
                    "the worst first (" + hot.get(0).place + " " + hot.get(0).dead + ", " + hot.get(1).place + " " + hot.get(1).dead + ")");
        }
        w.clearZombies();
        w.update(1 / 30f);
        check(w.hotSpots(8).isEmpty(), "nothing listed with no fighting");
    }

    static void historyAndRadio() throws Exception {
        GameView v = new GameView(new android.app.Activity());
        int[] size = {1080, 2400};
        v.layout(size[0], size[1]);
        CityConfig c = new CityConfig();
        c.v[CityConfig.OPT_SIZE] = CityConfig.TINY;
        c.v[CityConfig.OPT_ZOMBIES] = 3;
        v.startGame(c);
        v.awaitLoad();
        draw(v, size);
        ((Menu) field(v, "menu")).screen = Menu.NONE;
        World w = (World) field(v, "world");
        for (int f = 0; f < 60; f++) w.update(1 / 30f);
        // History: a kill by name, then the same thing twice running kept once; a bystander's isn't kept.
        Entity cop = null, civ = null;
        for (Entity e : w.entities)
            if (!e.dead && e.type == Entity.COP && cop == null) cop = e;
            else if (!e.dead && e.type == Entity.CIVILIAN && !e.isArmed() && !e.hero() && civ == null) civ = e;
        int had = cop.histN;
        Entity brute = w.spawn(Entity.BRUTE, cop.x + 30, cop.y);
        w.experience.killMade(cop, brute);
        w.history(cop, "bitten");
        w.history(cop, "bitten");
        w.history(civ, "bitten");
        boolean brute0 = false;
        for (int k = had; k < cop.histN; k++) brute0 |= cop.hist[k % Entity.HIST].equals("killed a brute");
        check(brute0 && cop.hist[(cop.histN - 1) % Entity.HIST].equals("bitten") && !cop.hist[(cop.histN - 2) % Entity.HIST].equals("bitten") && civ.histN == 0,
                "a unit's history kept, once for the same thing twice; a bystander's not (" + java.util.Arrays.toString(cop.hist) + ")");
        v.follow = cop;
        draw(v, size);
        // Radio: three calls for backup from one fight make one line; lower-priority news is capped each minute.
        Dispatch d = w.dispatch;
        int at = d.messageCount;
        for (int k = 0; k < 3; k++) d.say(Dispatch.WHO_POLICE, null, "Unit 5: Shots fired! Requesting backup!", 400, 400, true);
        check(d.messageCount == at + 1 && d.log.get(d.log.size() - 1).text.endsWith("(3 times)"), "repeats merged into one line");
        at = d.messageCount;
        String[] spots = {"tower", "bridge", "mall", "school", "park", "dock", "depot", "yard", "plaza", "field", "garage", "church"};
        for (String sp : spots) d.say(Dispatch.WHO_POLICE, null, "Patrols report movement by the " + sp + ".", 0, 0);
        check(d.messageCount - at <= Dispatch.LOW_CAP && d.quietCapped > 0, "routine news capped (" + (d.messageCount - at) + ")");
    }

    static void droneMarks() throws Exception {
        World w = world(cfg(CityConfig.SMALL, 73));
        java.util.Random r = new java.util.Random(7);
        // A drone: more of them now, and over a pack of the dead it marks them.
        w.clearZombies();
        w.alert = 2;
        float[] pack = w.city.randomWalkableInTown(r);
        for (int z = 0; z < 6; z++) w.spawn(Entity.ZOMBIE, pack[0] + z * 5, pack[1]);
        w.update(1 / 30f);
        check(!w.drones.isEmpty(), "drones up (" + w.drones.size() + ")");
        World.Drone d = w.drones.get(0);
        d.state = World.DRONE_OUT;
        d.x = d.tx = pack[0];
        d.y = d.ty = pack[1];
        d.spotCd = 0;
        d.sweepCd = 99;
        w.update(1 / 30f);
        int marked = 0;
        for (Entity e : w.entities) if (!e.dead && e.isZombie() && e.spottedUntil > w.time) marked++;
        check(marked >= 5, "the dead below it marked (" + marked + ")");
        // And with a safe zone open, its loudspeaker sends the people below there.
        w.clearZombies();
        for (float[] a : w.city.openAreas) {
            if (!w.dispatch.zones.isEmpty()) break;
            if (w.city.inTown(a[0], a[1])) w.dispatch.orderZone(a[0], a[1]);
        }
        if (!w.dispatch.zones.isEmpty()) {
            Dispatch.SafeZone z = w.dispatch.zones.get(0);
            z.open = true;
            z.built = 1;
            java.util.List<Entity> below = new java.util.ArrayList<Entity>();
            for (int k = 0; k < 5; k++) {
                float[] p = w.city.findWalkable(d.x + k * 8, d.y + 20);
                Entity e = w.spawn(Entity.CIVILIAN, p[0], p[1]);
                e.task = Dispatch.T_NONE;
                below.add(e);
            }
            WM sp = wm("droneSpeaker", World.Drone.class);
            sp.setAccessible(true);
            w.update(1 / 30f);
            for (Entity e : below) {
                e.task = Dispatch.T_NONE;
                e.leader = null;
            }
            sp.invoke(w, d);
            int seeking = 0;
            for (Entity e : below) if (e.task == Dispatch.T_SEEK && e.heardZone) seeking++;
            check(seeking >= 3, "people below told to make for the safe zone (" + seeking + ")");
        }
    }

    static void policeDrone() throws Exception {
        // A drone sent out: it gets there, and marks the dead under it.
        CityConfig c = cfg(CityConfig.SMALL, 31);
        c.v[CityConfig.OPT_ZOMBIES] = 2;
        World w = new World(c);
        w.setMaxPopulation(4000);
        w.populate(c);
        Entity foe = null;
        for (Entity e : w.entities) if (!e.dead && e.isZombie()) { foe = e; break; }
        float[] d = w.launchDrone(foe.x, foe.y);
        for (int f = 0; f < 30 * 40; f++) {
            w.update(1 / 30f);
            w.evCount = 0;
        }
        check(World.hypot(d[0] - d[2], d[1] - d[3]) < 60 && w.droneOver(d[2], d[3]), "over the spot, marking it");
        for (int f = 0; f < 30 * (int) World.WATCH_LIFE; f++) {
            w.update(1 / 30f);
            w.evCount = 0;
        }
        check(w.policeDrones.isEmpty(), "and home when its battery's done");
    }

    static void helicopterMarks() throws Exception {
        CityConfig c = cfg(0, CityConfig.MEDIUM, 78);
        World w = new World(c);
        w.populate(c);
        for (int f = 0; f < 30; f++) w.update(1 / 30f);
        // A police helicopter over a fight: it marks the dead below and moves over to where they're thickest.
        java.util.Random r = new java.util.Random(5);
        float[] f0 = null;
        for (int k = 0; k < 4000 && f0 == null; k++) {
            float[] q = w.city.randomWalkableInTown(r);
            if (w.city.findWalkable(q[0] + 120, q[1]) != null) f0 = q;
        }
        check(f0 != null, "a street for the fight");
        // (Nobody about to shoot the crowd before the helicopter gets a look.)
        Quiet calm = quiet(w, f0[0] + 120, f0[1], 400);
        java.util.List<Entity> crowd = new java.util.ArrayList<Entity>();
        for (int k = 0; k < 10; k++) {
            float[] p = w.city.findWalkable(f0[0] + 120 + (k % 4) * 5, f0[1] + (k / 4) * 5);
            if (p != null) crowd.add(w.spawn(Entity.ZOMBIE, p[0], p[1]));
        }
        for (int f = 0; f < 3; f++) step(w, calm);
        Fleet.Vehicle h = w.fleet.placeTank(f0[0], f0[1]);
        w.fleet.vehicles.remove(h);
        h.x = f0[0] + 60;
        h.y = f0[1];
        h.tx = f0[0];
        h.ty = f0[1];
        w.heliSpot(h, 1f);
        int marked = 0;
        for (Entity zz : crowd) if (zz.spottedUntil > w.time) marked++;
        check(marked * 2 >= crowd.size(), "it marked the dead below (" + marked + " of " + crowd.size() + ")");
        check(Math.hypot(h.tx - (f0[0] + 120), h.ty - f0[1]) < 60, "and moved its hover over the crowd");
    }
}
