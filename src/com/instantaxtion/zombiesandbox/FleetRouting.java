package com.instantaxtion.zombiesandbox;

import static com.instantaxtion.zombiesandbox.Fleet.*;

import java.util.ArrayList;

/**
 * ((27.3) Part of the vehicle code, out of Fleet.) Where vehicles go: destinations, route searches over the road network (City.driveField), corridors along a route, and the checks on whether somewhere can be driven to.
 */
final class FleetRouting {
    final Fleet fleet;
    final World w;
    final City city;

    FleetRouting(Fleet fleet) {
        this.fleet = fleet;
        this.w = fleet.w;
        this.city = fleet.city;
    }

    /** (10.33) A road somewhere in town (for an army patrol through the streets), or null. */
    float[] randomTownRoad() {
        int x0 = city.townX0, y0 = city.townY0, x1 = Math.max(x0 + 1, city.townX1), y1 = Math.max(y0 + 1, city.townY1);
        for (int k = 0; k < 100; k++) {
            int tx = x0 + w.rnd.nextInt(x1 - x0), ty = y0 + w.rnd.nextInt(y1 - y0);
            if (tx < 0 || ty < 0 || tx >= city.w || ty >= city.h) continue;
            if (city.tiles[ty * city.w + tx] == City.ROAD && (city.oneWay == null || city.oneWay[ty * city.w + tx] == 0))
                return new float[]{tx * City.T + City.T / 2f, ty * City.T + City.T / 2f};
        }
        return null;
    }

    /**
     * ((16.8) A road (or, for country, a lane outside town) somewhere between rmin and rmax from (x, y), or null.
     * Trips were picked from anywhere on the map and only then turned down for being too far: on the Large map a
     * random road is within reach of a car one time in a hundred, so after its ten tries almost every patrol and
     * everyday trip went right across the map anyway - a route search of hundreds of thousands of tiles each, the
     * worst of the stutters on the big maps.)
     */
    float[] randomRoadNear(float x, float y, float rmin, float rmax, boolean country) {
        for (int k = 0; k < 120; k++) {
            double a = w.rnd.nextDouble() * Math.PI * 2;
            float r = rmin + w.rnd.nextFloat() * (rmax - rmin);
            int tx = (int) ((x + (float) Math.cos(a) * r) / City.T), ty = (int) ((y + (float) Math.sin(a) * r) / City.T);
            if (tx < 0 || ty < 0 || tx >= city.w || ty >= city.h) continue;
            int i = ty * city.w + tx;
            if (country) {
                if (tx >= city.townX0 && tx < city.townX1 && ty >= city.townY0 && ty < city.townY1) continue;
                if (city.tiles[i] == City.DIRT) return new float[]{(tx + 0.5f) * City.T, (ty + 0.5f) * City.T};
            } else if (city.tiles[i] == City.ROAD && (city.oneWay == null || city.oneWay[i] == 0))
                return new float[]{(tx + 0.5f) * City.T, (ty + 0.5f) * City.T};
        }
        return null;
    }

    float[] randomRoad() {
        for (int k = 0; k < 200; k++) {
            int tx = w.rnd.nextInt(city.w), ty = w.rnd.nextInt(city.h);
            // (Nobody's trip ends on the highway.)
            if (city.tiles[ty * city.w + tx] == City.ROAD && (city.oneWay == null || city.oneWay[ty * city.w + tx] == 0))
                return new float[]{tx * City.T + City.T / 2f, ty * City.T + City.T / 2f};
        }
        return null;
    }

    /**
     * A vehicle's route, kept compact: the distance to go on each tile of a corridor three tiles either side of
     * the way from where it set off (everything else is off the route). A whole-map field per car took most of
     * a phone's memory on the big maps.
     */
    /**
     * ((13.3) Lets go of routes nothing will drive again: those of vehicles parked up, and old ones still held on to
     * as the route a lane, a junction or a stretch of road was worked out from after the vehicle had a new one. On
     * a Colossal map they came to a hundred megabytes, and phones ran out of memory.)
     */
    void releaseRoutes() {
        for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (v.parked && v.player == null && v.speed == 0) v.field = null;
            if (v.pathField != v.field) v.pathField = null;
            if (v.exitRoute != v.field) v.exitRoute = null;
            if (v.corOf != v.field) v.corOf = null;
            if (v.jaOf != v.field) v.jaOf = null;
        }
    }

    /**
     * (10.60) The tiles v's route takes from where it is to where it's going (downhill along its route, a tile at a
     * time), into out; how many. For the map to show the way a vehicle you're following will go.
     */
    int traceRoute(Vehicle v, int[] out) {
        Route r = v.field;
        if (r == null) return 0;
        int w = city.w, h = city.h;
        int t = city.tileIndex(v.x, v.y), cur = r.get(t), n = 0;
        while (cur > 0 && cur < City.FAR && n < out.length) {
            int x = t % w, y = t / w, best = -1, bv = cur;
            for (int oy = -1; oy <= 1; oy++)
                for (int ox = -1; ox <= 1; ox++) {
                    int nx = x + ox, ny = y + oy;
                    if ((ox == 0 && oy == 0) || nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                    int val = r.get(ny * w + nx);
                    if (val < bv) {
                        bv = val;
                        best = ny * w + nx;
                    }
                }
            if (best < 0) break;
            t = best;
            cur = bv;
            // (Every other tile is plenty for a line.)
            if ((n == 0 || (cur & 1) == 0)) out[n++] = t;
        }
        return n;
    }

    int[] scratchField() {
        // ((16.10) Shared with the ways on foot: see City.scratch.)
        return city.scratch();
    }

    /** The corridor along the way down a full field from (x, y). */
    Route corridor(int[] field, float x, float y) {
        int W = city.w, H = city.h, ti = city.tileIndex(x, y), tx0 = ti % W, ty0 = ti / W, t = ti, best = City.FAR;
        // From its own tile if that's on the way (not the nearest low tile, which may be over the barrier on the
        // other carriageway); otherwise from the best road beside it.
        if (field[ti] >= City.FAR)
            for (int j = ty0 - 2; j <= ty0 + 2; j++)
                for (int i = tx0 - 2; i <= tx0 + 2; i++)
                    if (i >= 0 && j >= 0 && i < W && j < H && field[j * W + i] < best) {
                        best = field[j * W + i];
                        t = j * W + i;
                    }
        int[] path = new int[64];
        int n = 0;
        path[n++] = ti;
        for (int guard = 0; guard < W * H; guard++) {
            if (n == path.length) path = java.util.Arrays.copyOf(path, n * 2);
            path[n++] = t;
            if (field[t] <= 4) break;
            int next = fleet.driving.downhillRaw(field, t);
            if (next == t) break;
            t = next;
        }
        Route r = new Route(n * 10 + 64);
        for (int q = 0; q < n; q++) {
            int px = path[q] % W, py = path[q] / W;
            // (Along the highway, both carriageways and the verges: a car that ends up on the other side of the
            // barrier must still know which way is on.)
            // ((11.1) Four tiles either side elsewhere: the whole of a main road, both its lanes, from a route that
            // runs down one side of it - at three, a car in the outside lane was off its route, and planned it again.)
            int R = city.nearHighway((px + 0.5f) * City.T, (py + 0.5f) * City.T, 6 * City.T) ? 9 : 4;
            for (int j = Math.max(0, py - R); j <= Math.min(H - 1, py + R); j++)
                for (int i = Math.max(0, px - R); i <= Math.min(W - 1, px + R); i++) {
                    int f = field[j * W + i];
                    if (f < City.FAR) r.put(j * W + i, f);
                }
        }
        return r;
    }

    /** The route's distance at (x, y), or from the road right next to it. */
    int nearField(int[] field, float x, float y) {
        int ti = city.tileIndex(x, y), tx = ti % city.w, ty = ti / city.w, best = City.FAR;
        for (int j = ty - 1; j <= ty + 1; j++)
            for (int i = tx - 1; i <= tx + 1; i++)
                if (i >= 0 && j >= 0 && i < city.w && j < city.h) best = Math.min(best, field[j * city.w + i]);
        return best;
    }

    /** True if (x, y) is on the player's screen (or near it). */
    /** ((11.5) Well away from the screen: further out than a car goes in a second or two.) */
    boolean farOff(Vehicle v) {
        return v.x < w.viewX0 - 300 || v.x > w.viewX1 + 300 || v.y < w.viewY0 - 300 || v.y > w.viewY1 + 300;
    }

    boolean inView(float x, float y) {
        return x > w.viewX0 - 60 && x < w.viewX1 + 60 && y > w.viewY0 - 60 && y < w.viewY1 + 60;
    }

    /**
     * ((24.0) Waiting in a queue isn't being stuck - unless the queue hasn't moved in 40 seconds: then it's a knot, and
     * the car looks for another way. One still knotted after 90 seconds where nobody's watching is lifted out onto
     * the nearest open road (a hundred patrol cars had jammed a dirt track for good, each waiting on the next).)
     */
    boolean gridlocked(Vehicle v) {
        if (v.player != null || v.stillT < 40) return false;
        // ((28.5) Well away from the screen, not just off it: lifted from just past the edge, a car was seen to jump.)
        if (v.stillT > 90 && farOff(v)) {
            for (int tries = 0; tries < 6; tries++) {
                float a = w.rnd.nextFloat() * 6.2832f, r = 120 + tries * 60;
                float[] d = city.nearestDrivable(v.x + (float) Math.cos(a) * r, v.y + (float) Math.sin(a) * r);
                if (d == null || inView(d[0], d[1]) || vehicleNear(d[0], d[1], 14, v)) continue;
                v.x = d[0];
                v.y = d[1];
                v.speed = 0;
                v.stillT = 0;
                v.stuckTimer = 0;
                v.destCd = 0;
                break;
            }
        }
        return true;
    }

    boolean vehicleNear(float x, float y, float r, Vehicle self) {
        for (int i = 0; i < fleet.vehicles.size(); i++) {
            Vehicle o = fleet.vehicles.get(i);
            if (o != self && !airborne(o) && Math.abs(o.x - x) < r && Math.abs(o.y - y) < r) return true;
        }
        return false;
    }

    /** ((29.2) A search that goes ahead whatever the budget: a patrol at its spot, with nowhere next. */
    boolean urgent;

    boolean newDestination(Vehicle v) {
        if (v.destCd > 0) return false;
        // ((28.0) Nothing found: it keeps the way it had, not the last one tried and turned down - one that began back the
        // way it came, which it then swung round on the spot to take.)
        Route was = v.field;
        float wtx = v.tx, wty = v.ty, wDist = v.lastDist;
        boolean ok = pickDestination(v);
        if (!ok) {
            v.destCd = 2 + w.rnd.nextFloat() * 2;
            if (v.field != was) {
                v.field = was;
                v.tx = wtx;
                v.ty = wty;
                v.lastDist = wDist;
            }
        }
        return ok;
    }

    boolean pickDestination(Vehicle v) {
        // (At most a few full route searches each time.)
        int routes = 0;
        // Highway patrol keeps to the highway, sheriffs to the country roads.
        if (v.patrol && v.agency > 0 && v.state == PATROL)
            for (int tries = 0; tries < 12; tries++) {
                float[] d = v.agency == 1 ? city.randomHighway(w.rnd) : null;
                // ((16.8) A sheriff's next stop picked within its own patch, not anywhere in the country.)
                if (v.agency == 2 && tries < 10) d = randomRoadNear(v.x, v.y, 300, 1400, true);
                if (v.agency != 1 && d == null) d = city.randomCountryRoad(w.rnd);
                // (Highway patrol: on ahead along its own carriageway, so it never has to turn round.)
                if (d != null && v.agency == 1 && city.onHighway(v.x, v.y)) {
                    // On ahead to the far end; once there, round onto the other side and back.
                    float[] mine = null, other = null;
                    for (int q = 0; q < 8 && (mine == null || other == null); q++) {
                        float[] run = city.highwayRun(w.rnd);
                        if (run == null) break;
                        if (Math.abs(city.hwyAxis == 0 ? run[1] - v.y : run[0] - v.x) < 3 * City.T) mine = run;
                        else other = run;
                    }
                    if (mine == null || other == null) continue;
                    float[] run = World.hypot(mine[2] - v.x, mine[3] - v.y) < 300 ? other : mine;
                    d = new float[]{run[2], run[3]};
                }
                // (Sheriffs keep to their own patch of country rather than crossing town; rangers to the lanes
                // round their park.)
                if (d != null && v.agency == 2 && tries < 10 && World.hypot(d[0] - v.x, d[1] - v.y) > 1400) continue;
                if (d != null && v.agency == 3 && city.rangerLot != null && tries < 10
                        && World.hypot(d[0] - city.rangerLot[0], d[1] - city.rangerLot[1]) > 1100) continue;
                if (d != null && World.hypot(d[0] - v.x, d[1] - v.y) > 300) {
                    if (route(v, d[0], d[1])) return true;
                    if (++routes >= 3) break;
                }
            }
        // (10.29) Now and then a trip out of town, by the highway: off the end of it and gone.
        if (v.type == CAR && !v.patrol && v.player == null && v.riders.isEmpty() && !v.through && city.hwyAxis >= 0
                && w.alert < 2 && w.rnd.nextFloat() < 0.04f) {
            float[] run = city.highwayRun(w.rnd);
            if (run != null && route(v, run[2], run[3])) {
                v.through = true;
                return true;
            }
            routes++;
        }
        // ((10.41) A country station's cars keep to their own patch of the country: somewhere round the station.)
        if (v.patrol && v.agency == 0 && v.station != null && v.station.satellite)
            for (int tries = 0; tries < 10; tries++) {
                float a = w.rnd.nextFloat() * 6.2832f, r = 300 + w.rnd.nextFloat() * 1000;
                float[] d = city.nearestDrivable(v.station.x + (float) Math.cos(a) * r, v.station.y + (float) Math.sin(a) * r);
                if (d == null || World.hypot(d[0] - v.x, d[1] - v.y) < 250) continue;
                if (route(v, d[0], d[1])) return true;
                if (++routes >= 4) break;
            }
        // ((16.8) On the bigger maps, picked within reach to begin with (see randomRoadNear).)
        float reach = city.w >= 900 ? 1800 : 2400;
        boolean local = (v.type == CAR || v.patrol) && city.w >= 600;
        // ((26.2) Parked off the road (a station's yard, facing in): the way out is behind it, so anywhere will do. It
        // only ever looked on ahead - into the yard - and the patrol cars sat there with their officers in them.)
        boolean offRoad = offTheRoad(v);
        for (int tries = 0; tries < 12; tries++) {
            float[] d = local && tries < 10 ? randomRoadNear(v.x, v.y, tries < 6 ? 400 : 160, reach, false) : randomRoad();
            if (d == null || World.hypot(d[0] - v.x, d[1] - v.y) < (tries < 6 ? 400 : 160)) continue;
            // ((10.34) On the biggest maps everyday trips stay local: a drive right across one is a long search.)
            // ((16.2) Closer again on the Default and bigger maps: each of those searches was a stutter of 20 to 40 ms.)
            if ((v.type == CAR || v.patrol) && city.w >= 600 && tries < 10 && World.hypot(d[0] - v.x, d[1] - v.y) > (city.w >= 900 ? 1800 : 2400)) continue;
            // Somewhere on ahead, as a rule: a car doesn't spin round in the road for its next trip.
            float hx = (float) Math.cos(v.angle), hy = (float) Math.sin(v.angle);
            if (!offRoad && tries < 8 && (d[0] - v.x) * hx + (d[1] - v.y) * hy < 0) continue;
            if (route(v, d[0], d[1])) {
                if (offRoad || tries >= 5 || (v.type != CAR && !v.patrol) || routes >= 3) return true;
                // And the way there starts ahead too (not a U-turn across the lanes, as a patrol car
                // turning round for every new beat would): no step back the way it came in the first few.
                int t = city.tileIndex(v.x, v.y);
                boolean back = false;
                for (int k = 0; k < 5 && !back; k++) {
                    int n = fleet.driving.downhill(v.field, t);
                    if (n == t) break;
                    if ((n % city.w - t % city.w) * hx + (n / city.w - t / city.w) * hy < -0.5f) back = true;
                    t = n;
                }
                if (!back) return true;
            }
            if (++routes >= 4) break;
        }
        return false;
    }

    /** The nearest way out of town by road (a tile centre), or null. */
    float[] exitNear(float x, float y) {
        if (fleet.exits == null) {
            java.util.ArrayList<Integer> list = new java.util.ArrayList<Integer>();
            int W = city.w, H = city.h;
            for (int i = 0; i < W; i += 1) {
                if (city.tiles[i] == City.ROAD) list.add(i);
                if (city.tiles[(H - 1) * W + i] == City.ROAD) list.add((H - 1) * W + i);
            }
            for (int j = 1; j < H - 1; j++) {
                if (city.tiles[j * W] == City.ROAD) list.add(j * W);
                if (city.tiles[j * W + W - 1] == City.ROAD) list.add(j * W + W - 1);
            }
            fleet.exits = new int[list.size()];
            for (int i = 0; i < fleet.exits.length; i++) fleet.exits[i] = list.get(i);
        }
        float best = Float.MAX_VALUE;
        int bt = -1;
        for (int t : fleet.exits) {
            float ex = (t % city.w + 0.5f) * City.T, ey = (t / city.w + 0.5f) * City.T;
            float d = (ex - x) * (ex - x) + (ey - y) * (ey - y) + w.rnd.nextFloat() * 90000;
            if (d < best) {
                best = d;
                bt = t;
            }
        }
        return bt < 0 ? null : new float[]{(bt % city.w + 0.5f) * City.T, (bt / city.w + 0.5f) * City.T};
    }

    boolean route(Vehicle v, float x, float y) {
        // ((10.34) Patrol cars on their rounds share the budget too: there are a couple of hundred on a Gigantic map.)
        if ((v.type == CAR || (v.patrol && v.state == PATROL && v.incident == null)) && v.player == null) {
            if (fleet.routeBudget <= 0 && !urgent) return false;
            fleet.routeBudget--;
        }
        // (10.45) Everyday traffic has no business inside an army base: it was driving in among the trucks at the gate.
        if (v.type == CAR && v.player == null) {
            for (City.Facility f : city.facilities)
                if (f.kind == City.FACILITY_BASE && World.hypot(x - f.x, y - f.y) < f.r + City.T * 2) return false;
        }
        int[] field = scratchField();
        // ((16.8) An everyday trip gives up on a search that's gone on far longer than a drive of its length takes:
        // it can always go somewhere else. See City.searchCap.)
        boolean casual = (v.type == CAR || (v.patrol && v.state == PATROL && v.incident == null)) && v.player == null && v.convoy == null;
        // ((26.2) Not from off the road - a station's yard, a lot: the search has to cross every paved tile to get back to
        // it, gave up every time, and patrol cars sat in the yard with their officers in them for the whole game.)
        if (casual && offTheRoad(v)) casual = false;
        // ((29.2) Nor for a patrol at its spot with nowhere next: on the one-way slip roads of an interchange the capped
        // search always gave up, and the car went round and round the loop, a few yards from where it had been going.)
        if (urgent) casual = false;
        city.searchCap = casual ? 15000 : 0;
        // ((27.4) Round the streets thick with the dead - all but where it's going, if that's to a call.)
        hazardOn(hazardMul(v), x, y, emergency(v) ? HAZ_CALL : HAZ_TRIP);
        try {
            // ((28.0) On the move, its way starts on ahead: on to a junction or a gap in the median to turn, or round the
            // block. Planned from its own tile with nothing behind it ruled out, the shortest way to somewhere behind (or
            // off to the side, or just past the end of a route that turned hard) began with a step back the way it came,
            // and the car swung round on the spot to take it - a full circle where the step back was across a median it
            // couldn't cross. Only with no way on at all (a dead end) is it planned the short way, back.)
            if (noTurningBack(v)) {
                noLoosen = true;
                try {
                    if (routeOn(v, x, y, field)) return true;
                } finally {
                    noLoosen = false;
                    city.noGoN = 0;
                }
                if (city.capped) return false;
            }
            return routeOn(v, x, y, field);
        } finally {
            city.searchCap = 0;
            hazardOff();
        }
    }

    /** ((28.0) The search under way may not fall back on one over the pavements (see routeOn). */
    private boolean noLoosen;
    /** ((28.0) Set while a vehicle plans its way back out of trouble on purpose (the dead thick on the road ahead). */
    boolean mayTurnBack;

    /**
     * ((28.0) If v is driving along a road, on ahead at a fair speed, the row of tiles right behind it, all the way
     * across the road (both sides of it, and a median), is put out of bounds for the search about to be made (City.noGo):
     * its way can't start by turning round where it is. True if it was.
     */
    boolean noTurningBack(Vehicle v) {
        city.noGoN = 0;
        if (mayTurnBack || v.player != null || offRoad(v) || v.speed < 8 || v.backT > 0 || v.giveWayT > 0) return false;
        int own = city.tileIndex(v.x, v.y);
        if (own < 0 || city.tiles[own] != City.ROAD || city.hwyTile(own % city.w, own / city.w)) return false;
        float c = (float) Math.cos(v.angle), sn = (float) Math.sin(v.angle);
        // (Square on to the road, not halfway round a corner.)
        if (Math.max(Math.abs(c), Math.abs(sn)) < 0.82f) return false;
        int dx = Math.abs(c) >= Math.abs(sn) ? (c > 0 ? 1 : -1) : 0, dy = dx == 0 ? (sn > 0 ? 1 : -1) : 0;
        int bx = own % city.w - dx, by = own / city.w - dy;
        for (int s = -4; s <= 4; s++) {
            int x = bx + dy * s, y = by + dx * s;
            if (x < 0 || y < 0 || x >= city.w || y >= city.h || city.driveCost(y * city.w + x) < 0) continue;
            city.noGo[city.noGoN++] = y * city.w + x;
        }
        return city.noGoN > 0;
    }

    /**
     * ((27.4) How much a vehicle minds the dead on the way, out of 100: armour (the APC, IFV, BearCat, tank and the
     * bulldozer) hardly at all, everything else a great deal; nothing while there aren't any, or when it's you driving.
     */
    int hazardMul(Vehicle v) {
        if (v.player != null || w.zombies == 0) return 0;
        if (v.type == TANK || v.kind == K_APC || v.kind == K_IFV || v.kind == K_BEARCAT || v.kind == K_DOZER) return 25;
        return 100;
    }

    /** ((27.4) The square round where it's going where the dead don't count, in tiles: a call's, and any other trip's. */
    static final int HAZ_CALL = 7, HAZ_TRIP = 2;

    /** ((27.4) The next route search keeps clear of the dead (see City.hazard), mul of 100, but not within r tiles of (tx, ty). */
    void hazardOn(int mul, float tx, float ty, int r) {
        if (mul <= 0) return;
        city.hazZ = w.zCount;
        city.hazGw = w.gw;
        city.hazGh = w.gh;
        city.hazCell = Math.max(1, World.CELL / City.T);
        city.hazMul = mul;
        city.hazTx = (int) (tx / City.T);
        city.hazTy = (int) (ty / City.T);
        city.hazR = r;
    }

    void hazardOff() {
        city.hazZ = null;
        city.blockN = 0;
    }

    /**
     * ((27.4) Plans v's way again, past the tiles given (a wreck, a jam, a knot at a junction), which it won't drive
     * through, and round the dead as usual. False if there's no other way: it keeps the route it has.
     */
    boolean routeAround(Vehicle v, int[] tiles, int n) {
        city.blockN = 0;
        for (int i = 0; i < n && i < city.blockT.length; i++) if (tiles[i] >= 0) city.blockT[city.blockN++] = tiles[i];
        // (Not if it's sitting on one of them: then nothing gets it out.)
        int own = city.tileIndex(v.x, v.y);
        for (int i = 0; i < city.blockN; i++) {
            int b = city.blockT[i];
            if (Math.abs(own % city.w - b % city.w) <= 1 && Math.abs(own / city.w - b / city.w) <= 1) {
                city.blockN = 0;
                return false;
            }
        }
        try {
            return route(v, v.tx, v.ty);
        } finally {
            city.blockN = 0;
        }
    }

    /** ((26.2) Standing on something that isn't a road or a lane - a lot, a station's yard, the pavement. */
    boolean offTheRoad(Vehicle v) {
        int i = city.tileIndex(v.x, v.y);
        return i >= 0 && city.tiles[i] != City.ROAD && city.tiles[i] != City.DIRT;
    }

    boolean routeOn(Vehicle v, float x, float y, int[] field) {
        // (Everyday traffic and patrol cars keep to the roads; emergencies cut across whatever's paved.)
        // ((26.0) Not a convoy's bus: it keeps to the roads, but a long way in can't be given up on like a trip to the shops.)
        boolean strict = (v.type == CAR || (v.patrol && v.state != DRIVE)) && v.convoy == null;
        int from = city.tileIndex(v.x, v.y);
        // ((16.2) Not when it isn't on a road itself (pulled up on a lot or a pavement): a roads-only search never got
        // back to it, so it went right through every road on the map - up to 80 ms on the Big map - before trying the
        // other way. Straight to the other way.)
        if (strict && !city.roadBeside(from)) strict = false;
        // (10.27) Four-wheel drive on an emergency: across open country where that's quicker.
        // ((10.34) Only when the call isn't by a road anyway: a search across open country is costly.)
        if (!strict && emergency(v) && allWheel(v) && !city.roadNear(x, y, 5) && city.offroadField(field, x, y, from) && nearField(field, v.x, v.y) < City.FAR) {
            v.field = corridor(field, v.x, v.y);
            v.tx = x;
            v.ty = y;
            v.lastDist = Float.MAX_VALUE;
            v.spin = 0;
            v.stuckTimer = 0;
            return true;
        }
        if (!city.driveField(field, x, y, strict, from) || nearField(field, v.x, v.y) >= City.FAR) {
            // Off the road (shoved onto the pavement, or parked on a lot): find the way back over whatever's paved.
            // ((16.8) Not after an everyday search that gave up: the other way would only give up too.)
            if (!strict || city.capped || noLoosen || !city.driveField(field, x, y, false, from) || nearField(field, v.x, v.y) >= City.FAR) return false;
        }
        v.field = corridor(field, v.x, v.y);
        v.tx = x;
        v.ty = y;
        v.lastDist = Float.MAX_VALUE;
        v.spin = 0;
        v.stuckTimer = 0;
        return true;
    }

    /** On an emergency: to a call, picking up its people, or heading out on one, once the city knows. */
    boolean emergency(Vehicle v) {
        if (v.incident != null && !v.incident.resolved) return true;
        if (v.pickup || v.block != null) return true;
        return w.alert >= 2 && v.state != RETURN && !v.parkHome && (v.passengers > 0 || !v.crew.isEmpty()) && !v.leaving;
    }

    /**
     * A civilian car sitting in the way of help coming onto the map (parked, wrecked or stopped for good, with
     * nobody aboard) is cleared off the road. Returns true if it was.
     */
    boolean clearFromEntry(Vehicle o) {
        if (o.type != CAR || o.player != null || !o.riders.isEmpty() || o.block != null) return false;
        if (!(o.parked || o.broken || o.stillT > 3)) return false;
        o.removedFromFleet = true;
        fleet.vehicles.remove(o);
        return true;
    }

    /** (10.52) How far it is to drive from one point to another (in the route's own units), or City.FAR. */
    int driveCost(float fx, float fy, float tx, float ty) {
        float[] start = city.nearestDrivable(fx, fy);
        if (start == null) return City.FAR;
        int[] full = scratchField();
        int si = city.tileIndex(start[0], start[1]);
        return city.driveField(full, tx, ty, false, si) ? full[si] : City.FAR;
    }

    /** Whether there's a way to drive from one point to another. */
    boolean canDrive(float fx, float fy, float tx, float ty) {
        float[] start = city.nearestDrivable(fx, fy);
        if (start == null) return false;
        int si = city.tileIndex(start[0], start[1]);
        // ((16.2) Whether there's a way by road from one part of town to another hardly ever changes: asked again
        // and again for the same stations and calls, it was a whole search each time (then another for the route).)
        long key = ((long) (si / city.w / 8) << 48) | ((long) (si % city.w / 8) << 32) | ((long) ((int) (ty / City.T) / 8) << 16) | ((int) (tx / City.T) / 8);
        float[] known = fleet.canDriveSeen.get(key);
        if (known != null && w.time - known[0] < 10) return known[1] > 0;
        int[] full = scratchField();
        boolean ok = city.driveField(full, tx, ty, false, si) && full[si] < City.FAR;
        if (fleet.canDriveSeen.size() > 4000) fleet.canDriveSeen.clear();
        fleet.canDriveSeen.put(key, new float[]{w.time, ok ? 1 : 0});
        return ok;
    }

    /** (14.6) On or right beside a road (an open street for the gun truck). */
    boolean onRoad(float x, float y) {
        int tx = (int) (x / City.T), ty = (int) (y / City.T);
        for (int j = ty - 2; j <= ty + 2; j++)
            for (int i = tx - 2; i <= tx + 2; i++)
                if (i >= 0 && j >= 0 && i < city.w && j < city.h && city.tiles[j * city.w + i] == City.ROAD) return true;
        return false;
    }

    /**
     * (10.45) A spot near p with no other vehicle on it: several trucks or cars mustering at the same gate used to
     * be put on top of one another there, and blocked it. The nearest clear bit of road, yard or car park.
     */
    float[] clearSpot(float[] p) {
        if (p == null || !fleet.armyUnits.occupied(p[0], p[1])) return p;
        int tx = (int) (p[0] / City.T), ty = (int) (p[1] / City.T);
        for (int r = 1; r <= 5; r++)
            for (int dy = -r; dy <= r; dy++)
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
                    int x = tx + dx, y = ty + dy;
                    if (x < 0 || y < 0 || x >= city.w || y >= city.h) continue;
                    byte t = city.tiles[y * city.w + x];
                    if (t != City.ROAD && t != City.LOT && t != City.BASE) continue;
                    float px = (x + 0.5f) * City.T, py = (y + 0.5f) * City.T;
                    if (city.solidAt(px, py) || fleet.armyUnits.occupied(px, py) || !city.los(p[0], p[1], px, py)) continue;
                    return new float[]{px, py};
                }
        return p;
    }

    /**
     * (11.0) The nearest open spot to (x, y), within reach, with none of the dead within 60 of it (the spot itself if
     * it's clear). Null if there's nowhere.
     */
    float[] clearSpot(float x, float y, float reach) {
        for (float r = 0; r <= reach; r += 30)
            for (int k = 0; k < (r == 0 ? 1 : 12); k++) {
                float a = k * (float) Math.PI / 6 + r * 0.02f;
                float[] p = city.findWalkable(x + (float) Math.cos(a) * r, y + (float) Math.sin(a) * r);
                if (p == null || city.circleBlocked(p[0], p[1], 6)) continue;
                if (w.countZombiesNear(p[0], p[1], 60) == 0) return p;
            }
        return null;
    }
}
