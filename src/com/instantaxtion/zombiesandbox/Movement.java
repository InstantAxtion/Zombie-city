package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Getting about: steering, moving and colliding, climbing and vaulting, wandering, fleeing, and finding the way (doors, paths, lures).
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class Movement {
    private final World w;

    Movement(World w) {
        this.w = w;
    }

    @SuppressWarnings("unchecked")
    void buildOccGrid() {
        w.occGW = (int) (w.city.worldW() / World.OCC_CELL) + 1;
        w.occGH = (int) (w.city.worldH() / World.OCC_CELL) + 1;
        if (w.occGrid == null || w.occGrid.length != w.occGW * w.occGH) w.occGrid = new java.util.ArrayList[w.occGW * w.occGH];
        for (java.util.ArrayList<City.Building> l : w.occGrid) if (l != null) l.clear();
        for (int i = 0, n = w.city.buildings.size(); i < n; i++) {
            City.Building b = w.city.buildings.get(i);
            if (b.occupants.isEmpty() && b.visitors.isEmpty()) continue;
            int gx = Math.max(0, Math.min(w.occGW - 1, (int) (b.doorX / World.OCC_CELL))), gy = Math.max(0, Math.min(w.occGH - 1, (int) (b.doorY / World.OCC_CELL)));
            java.util.ArrayList<City.Building> l = w.occGrid[gy * w.occGW + gx];
            if (l == null) w.occGrid[gy * w.occGW + gx] = l = new java.util.ArrayList<City.Building>();
            l.add(b);
        }
        w.occTimer = 0.5f;
    }

    World.DoorRoute doorField(City.Building b) {
        World.DoorRoute f = w.doorFields.get(b);
        if (f == null) {
            if (w.pathBudget < 0.05f) return null;
            w.pathBudget -= 0.05f;
            f = w.spareDoor != null ? w.spareDoor : new World.DoorRoute();
            w.spareDoor = null;
            int dx = (int) (b.doorX / City.T), dy = (int) (b.doorY / City.T);
            f.x0 = dx - World.DOOR_R;
            f.y0 = dy - World.DOOR_R;
            java.util.Arrays.fill(f.d, City.FAR);
            if (w.doorBuckets == null) w.doorBuckets = new int[9][World.DOOR_S * World.DOOR_S * 4];
            if (w.city.walkCost == null) w.city.computeWalkCost();
            int[] sizes = new int[9];
            int start = World.DOOR_R * World.DOOR_S + World.DOOR_R;
            f.d[start] = 0;
            sizes[0] = 1;
            w.doorBuckets[0][0] = start;
            int pending = 1;
            for (int d = 0; pending > 0 && d < 400; d++) {
                int bi = d % 9;
                int[] bk = w.doorBuckets[bi];
                int size = sizes[bi];
                sizes[bi] = 0;
                pending -= size;
                for (int k = 0; k < size; k++) {
                    int t = bk[k];
                    if (f.d[t] != d) continue;
                    int lx = t % World.DOOR_S, ly = t / World.DOOR_S;
                    for (int q = 0; q < 4; q++) {
                        int nx = lx + (q == 0 ? -1 : q == 1 ? 1 : 0), ny = ly + (q == 2 ? -1 : q == 3 ? 1 : 0);
                        if (nx < 0 || ny < 0 || nx >= World.DOOR_S || ny >= World.DOOR_S) continue;
                        int wx = f.x0 + nx, wy = f.y0 + ny;
                        if (wx < 0 || wy < 0 || wx >= w.city.w || wy >= w.city.h) continue;
                        int wi = wy * w.city.w + wx;
                        if (w.city.solid[wi]) continue;
                        int nl = ny * World.DOOR_S + nx, nd = d + w.city.walkCost[wi];
                        if (nd >= f.d[nl]) continue;
                        f.d[nl] = nd;
                        int b2 = nd % 9;
                        if (sizes[b2] >= w.doorBuckets[b2].length) continue;
                        w.doorBuckets[b2][sizes[b2]++] = nl;
                        pending++;
                    }
                }
            }
            w.doorFields.put(b, f);
        }
        return f;
    }

    /** Follows a door route downhill; false if this one is off its patch or cut off. */
    boolean followDoor(Entity e, World.DoorRoute f, float speed) {
        return followPatch(e, f.x0, f.y0, World.DOOR_S, f.d, speed, false);
    }

    /** Follows a route patch ({@code s} tiles square from x0, y0) downhill; false if off it or cut off. */
    boolean followPatch(Entity e, int x0, int y0, int s, int[] dd, float speed, boolean ahead) {
        int tx = (int) (e.x / City.T), ty = (int) (e.y / City.T);
        int lx = tx - x0, ly = ty - y0;
        if (lx < 1 || ly < 1 || lx >= s - 1 || ly >= s - 1) return false;
        int cur = dd[ly * s + lx];
        if (cur >= City.FAR || cur == 0) return false;
        int best = cur, bx = -1, by = -1;
        for (int oy = -1; oy <= 1; oy++)
            for (int ox = -1; ox <= 1; ox++) {
                if (ox == 0 && oy == 0) continue;
                if (w.city.solidTile(tx + ox, ty + oy)) continue;
                if (ox != 0 && oy != 0 && (w.city.solidTile(tx + ox, ty) || w.city.solidTile(tx, ty + oy))) continue;
                int v = dd[(ly + oy) * s + lx + ox];
                if (v < best) {
                    best = v;
                    bx = tx + ox;
                    by = ty + oy;
                }
            }
        if (bx < 0) return false;
        if (ahead) {
            // (10.39) On down the route a few tiles more, and straight for the furthest of those in sight,
            // rather than zig-zagging tile to tile.
            int cx = bx, cy = by, v = best, fx = -1, fy = -1;
            for (int step = 1; step <= 6 && v > 0; step++) {
                int nb = v, nx = -1, ny = -1;
                for (int oy = -1; oy <= 1; oy++)
                    for (int ox = -1; ox <= 1; ox++) {
                        int qx = cx + ox - x0, qy = cy + oy - y0;
                        if ((ox == 0 && oy == 0) || qx < 0 || qy < 0 || qx >= s || qy >= s) continue;
                        int q = dd[qy * s + qx];
                        if (q < nb && !w.city.solidTile(cx + ox, cy + oy)) {
                            nb = q;
                            nx = cx + ox;
                            ny = cy + oy;
                        }
                    }
                if (nx < 0) break;
                cx = nx;
                cy = ny;
                v = nb;
                if (step == 3 || step == 6) {
                    float px = cx * City.T + City.T / 2f, py = cy * City.T + City.T / 2f;
                    if (w.city.los(e.x, e.y, px, py)) {
                        fx = cx;
                        fy = cy;
                    } else break;
                }
            }
            if (fx >= 0) {
                bx = fx;
                by = fy;
            }
        }
        float gx = bx * City.T + City.T / 2f - e.x, gy = by * City.T + City.T / 2f - e.y;
        float d = (float) Math.sqrt(gx * gx + gy * gy) + 0.001f;
        steer(e, gx / d, gy / d, speed);
        return true;
    }

    /**
     * Walks to a building's door around the building (not into its wall): a path field to the door is
     * made the first time anyone needs it. Returns false if it had to fall back to walking straight.
     */
    boolean walkTo(Entity e, City.Building b, float ddx, float ddy, float d, float speed) {
        if (d < 20 && w.city.los(e.x, e.y, b.doorX, b.doorY) && !e.blocked) {
            steer(e, ddx / d, ddy / d, speed);
            return true;
        }
        // (10.37) Running for a door in a hurry (to hide, say), not too far off: everyone heading for that door
        // shares one route round the buildings to it, instead of each planning their own.
        if ((e.task == Dispatch.T_HIDE || e.fleeTimer > 0) && d < World.DOOR_R * City.T * 0.9f
                && (e.pathDest != b || e.path == null || e.path.length == 0)) {
            World.DoorRoute route = doorField(b);
            if (route != null && followDoor(e, route, speed)) {
                e.blocked = false;
                return true;
            }
        }
        if (e.pathDest != b || e.path == null) {
            if (w.pathBudget < 0.25f) {
                // Too many new routes this second: plan properly in a moment
                // (someone in no hurry strolls along the pavement meanwhile instead of cutting across the road).
                if (keepsRules(e)) wander(e, speed * 0.7f);
                else steer(e, ddx / d, ddy / d, speed);
                return false;
            }
            w.pathBudget -= 0.25f;
            e.path = w.city.findPath(e.x, e.y, b.doorX, b.doorY, 9000);
            e.pathIdx = 0;
            e.pathDest = b;
            if (e.path == null) {
                // No way there on foot: try again in a while.
                e.path = new int[0];
            }
        }
        e.blocked = false;
        if (followPath(e, speed)) return true;
        steer(e, ddx / d, ddy / d, speed);
        return false;
    }

    /** Follows a walking route tile by tile, waiting at the kerb for traffic. False when there's none to follow. */
    boolean followPath(Entity e, float speed) {
        int[] p = e.path;
        if (p == null || p.length == 0) return false;
        int w = this.w.city.w;
        if (e.pathSeen != p) {
            e.pathSeen = p;
            e.pathRetries = 0;
            e.pathBest = Float.MAX_VALUE;
            e.pathStall = 0;
        }
        // (10.36) The routes are straight lines from corner to corner now (see Nav): on to the next corner once
        // this one is reached, or once it can be seen past and the one after is clear to walk to.
        while (e.pathIdx < p.length) {
            int t = p[e.pathIdx];
            float px = (t % w + 0.5f) * City.T, py = (t / w + 0.5f) * City.T;
            // ((11.1) Each person a little to one side of the line between the corners, their own way, so a crowd
            // going the same way spreads across the pavement instead of walking in single file. Not the last
            // corner: that's where they're going.)
            if (e.type == Entity.CIVILIAN && e.pathIdx > 0 && e.pathIdx < p.length - 1) {
                int q = p[e.pathIdx - 1];
                float sx = px - (q % w + 0.5f) * City.T, sy = py - (q / w + 0.5f) * City.T, sl = (float) Math.sqrt(sx * sx + sy * sy);
                if (sl > 1) {
                    float side = (((e.nameSeed >>> 5) & 31) / 31f - 0.5f) * City.T * 0.8f;
                    float ox = px - sy / sl * side, oy = py + sx / sl * side;
                    if (!this.w.city.solidAt(ox, oy)) {
                        px = ox;
                        py = oy;
                    }
                }
            }
            float dx = px - e.x, dy = py - e.y, dd = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
            if (dd < 8 && e.pathIdx < p.length - 1) {
                e.pathIdx++;
                e.pathBest = Float.MAX_VALUE;
                continue;
            }
            // ((12.0) The first corner out of sight from here (they've been pushed or wandered since it was planned,
            // and a wall's in the way): planned again from where they are, not slid to and fro along the wall.)
            if (e.pathIdx == 0 && dd > 20 && this.w.time - e.replanAt > 3 && !this.w.city.los(e.x, e.y, px, py)) {
                e.replanAt = this.w.time;
                e.path = null;
                return false;
            }
            // Pushed well off the line it was walking: plan it again from here. (10.49) Not on the way to the first
            // corner: with no corner before it, that measured the way still to go, and threw out (and planned again,
            // every moment) every route whose first leg was long.
            int q0 = e.pathIdx > 0 ? p[e.pathIdx - 1] : 0;
            if (e.pathIdx > 0 && World.distToSegment(e.x, e.y, (q0 % w + 0.5f) * City.T, (q0 / w + 0.5f) * City.T, px, py) > 40) {
                // (10.37) Back onto the route where it can be seen from here, rather than a whole new plan.
                int back = -1;
                for (int k = Math.max(0, e.pathIdx - 1); k < Math.min(p.length, e.pathIdx + 6) && back < 0; k++) {
                    int q = p[k];
                    float qx = (q % w + 0.5f) * City.T, qy = (q / w + 0.5f) * City.T;
                    if ((qx - e.x) * (qx - e.x) + (qy - e.y) * (qy - e.y) < 160 * 160 && this.w.city.los(e.x, e.y, qx, qy)) back = k;
                }
                if (back < 0 || e.pathRetries >= 3) {
                    e.path = null;
                    return false;
                }
                e.pathRetries++;
                e.pathIdx = back;
                e.pathBest = Float.MAX_VALUE;
                // (The corner before it is behind them now: straight for this one.)
                float bx = (p[back] % w + 0.5f) * City.T, by = (p[back] / w + 0.5f) * City.T;
                float bdx = bx - e.x, bdy = by - e.y, bd = (float) Math.sqrt(bdx * bdx + bdy * bdy) + 0.001f;
                steer(e, bdx / bd, bdy / bd, speed);
                return true;
            }
            // Getting no closer (stuck on a corner, in a crowd): give it a moment, then plan again.
            if (dd < e.pathBest - 2) {
                e.pathBest = dd;
                e.pathStall = 0;
            } else if ((e.pathStall += 1 / 30f) > 2.5f) {
                e.pathStall = 0;
                e.pathBest = Float.MAX_VALUE;
                // (10.37) A step aside and on along the same route the first time or two; a new plan only after that.
                if (e.pathRetries++ >= 2) e.path = null;
                e.unstick = 0.4f;
                e.unstickAngle = (float) Math.atan2(dy, dx) + (this.w.rnd.nextBoolean() ? 1.2f : -1.2f);
                return false;
            }
            // Waiting at the kerb for traffic before stepping into the road.
            if (keepsRules(e)) {
                int ahead = this.w.city.tileIndex(e.x + dx / dd * 12, e.y + dy / dd * 12), cur = this.w.city.tileIndex(e.x, e.y);
                if (ahead >= 0 && cur >= 0 && this.w.city.tiles[ahead] == City.ROAD && this.w.city.tiles[cur] != City.ROAD
                        && carComing((ahead % w + 0.5f) * City.T, (ahead / w + 0.5f) * City.T)) {
                    steer(e, 0, 0, 0);
                    return true;
                }
            }
            steer(e, dx / dd, dy / dd, speed);
            return true;
        }
        return false;
    }

    void steer(Entity e, float x, float y, float speed) {
        e.mx = x;
        e.my = y;
        e.want = speed;
    }

    void wander(Entity e, float speed) {
        if (e.wanderTimer <= 0 || e.blocked) {
            e.blocked = false;
            e.wanderTimer = 2 + w.rnd.nextFloat() * 4;
            e.paused = !e.isZombie() && w.rnd.nextFloat() < 0.2f;
            // Mostly carry on or turn a corner; only now and then turn right round (which looks like pacing).
            float roll = w.rnd.nextFloat();
            float base = Math.round(e.wanderAngle / (World.TAU / 4)) * (World.TAU / 4);
            e.wanderAngle = (e.isZombie() || e.wanderTimer < -1 ? w.rnd.nextInt(4) * World.TAU / 4
                    : roll < 0.5f ? base : roll < 0.92f ? base + (w.rnd.nextBoolean() ? 1 : -1) * World.TAU / 4 : base + World.TAU / 2)
                    + (w.rnd.nextFloat() - 0.5f) * 0.4f;
            // (10.51) Out for a stroll in town, people (and police on foot) keep to the pavements and squares: a way
            // that leads out onto the grass is swapped for one along the paving, where there is one.
            if (!e.isZombie() && e.fear <= 0 && !w.city.outOfTown(e.x, e.y) && onGrassAhead(e, e.wanderAngle)) {
                float best = e.wanderAngle;
                for (int k = 1; k <= 3; k++) {
                    float a = e.wanderAngle + k * World.TAU / 4;
                    if (!onGrassAhead(e, a)) {
                        best = a;
                        break;
                    }
                }
                e.wanderAngle = best;
            }
            // People who saw a zombie recently keep away from where it was.
            if (e.fear > 0 && !e.isZombie() && !e.isArmed()) {
                float tx = e.threatX - e.x, ty = e.threatY - e.y;
                if (tx * (float) Math.cos(e.wanderAngle) + ty * (float) Math.sin(e.wanderAngle) > 0) e.wanderAngle += (float) Math.PI;
                e.paused = false;
            } else if (e.type == Entity.CIVILIAN && (e.home != null || (e.homeChecked && w.city.outOfTown(e.x, e.y)))) {
                // A stroll or a run doesn't go on for ever into the middle of nowhere: out in the country, or
                // a long way from home in town, it turns back towards home (or, with no home, towards town).
                float[] s = e.home == null ? w.civilianAI.nearestSettlement(e.x, e.y) : null;
                float hx = e.home != null ? e.home.doorX - e.x : s[0] - e.x, hy = e.home != null ? e.home.doorY - e.y : s[1] - e.y;
                float leash = w.city.outOfTown(e.x, e.y) ? 150 : 520;
                if (hx * hx + hy * hy > leash * leash) {
                    e.wanderAngle = (float) Math.atan2(hy, hx) + (w.rnd.nextFloat() - 0.5f) * 0.9f;
                    e.paused = false;
                }
            }
        }
        if (!e.paused && keepsRules(e)) {
            // Strolling people stay on the pavement: they turn back at the kerb unless it's a crossing,
            // and wait there while a car goes by.
            int tx = (int) (e.x / City.T), ty = (int) (e.y / City.T);
            float ax = e.x + (float) Math.cos(e.wanderAngle) * City.T * 0.8f, ay = e.y + (float) Math.sin(e.wanderAngle) * City.T * 0.8f;
            int nx = (int) (ax / City.T), ny = (int) (ay / City.T);
            if ((nx != tx || ny != ty) && w.city.tileIndex(ax, ay) >= 0 && w.city.tiles[w.city.tileIndex(ax, ay)] == City.ROAD
                    && w.city.tiles[w.city.tileIndex(e.x, e.y)] != City.ROAD) {
                if (!w.city.crosswalk(nx, ny)) {
                    // At the kerb: turn to walk along the pavement (whichever way isn't road), not back.
                    float left = e.wanderAngle - World.TAU / 4, right = e.wanderAngle + World.TAU / 4;
                    boolean lOk = w.city.tiles[w.city.tileIndex(e.x + (float) Math.cos(left) * City.T, e.y + (float) Math.sin(left) * City.T)] != City.ROAD
                            && !w.city.solidAt(e.x + (float) Math.cos(left) * City.T, e.y + (float) Math.sin(left) * City.T);
                    boolean rOk = w.city.tiles[w.city.tileIndex(e.x + (float) Math.cos(right) * City.T, e.y + (float) Math.sin(right) * City.T)] != City.ROAD
                            && !w.city.solidAt(e.x + (float) Math.cos(right) * City.T, e.y + (float) Math.sin(right) * City.T);
                    float back = e.wanderAngle + (float) Math.PI;
                    float bx = e.x + (float) Math.cos(back) * City.T * 0.8f, by = e.y + (float) Math.sin(back) * City.T * 0.8f;
                    boolean bOk = w.city.tileIndex(bx, by) >= 0 && w.city.tiles[w.city.tileIndex(bx, by)] != City.ROAD && !w.city.solidAt(bx, by);
                    if (lOk && (!rOk || w.rnd.nextBoolean())) e.wanderAngle = left;
                    else if (rOk) e.wanderAngle = right;
                    else if (bOk) e.wanderAngle = back;
                    else {
                        // ((12.0) On a strip of grass between two roads, road every way: turning round only faced
                        // the other road, and they turned round again and again for good. They cross, once it's clear.)
                        e.wanderTimer = Math.max(e.wanderTimer, 3);
                        if (carComing(ax, ay)) {
                            steer(e, 0, 0, 0);
                            return;
                        }
                        steer(e, (float) Math.cos(e.wanderAngle), (float) Math.sin(e.wanderAngle), speed);
                        return;
                    }
                    e.wanderTimer = Math.max(e.wanderTimer, 3);
                } else if (carComing(ax, ay)) {
                    steer(e, 0, 0, 0);
                    return;
                }
            }
        }
        if (e.paused) steer(e, 0, 0, 0);
        else steer(e, (float) Math.cos(e.wanderAngle), (float) Math.sin(e.wanderAngle), speed);
    }

    /** (10.51) The way at angle a leads onto grass (or into trees) a couple of tiles on. */
    boolean onGrassAhead(Entity e, float a) {
        float ax = e.x + (float) Math.cos(a) * City.T * 2, ay = e.y + (float) Math.sin(a) * City.T * 2;
        int t = w.city.tileIndex(ax, ay);
        if (t < 0) return true;
        byte k = w.city.tiles[t];
        return k == City.GRASS || k == City.TREE;
    }

    /** Ordinary folk going about their day mind the traffic; anyone running for their life doesn't. */
    boolean keepsRules(Entity e) {
        if (e == w.controlled) return false;
        if (e.type == Entity.CIVILIAN) return e.fleeTimer <= 0 && e.fear <= 0 && e.task != Dispatch.T_SEEK;
        // Police and soldiers on an ordinary patrol walk on the pavement too; in a fight, anything goes.
        if (e.type == Entity.COP || e.type == Entity.SOLDIER)
            return (e.task == Dispatch.T_NONE || e.task == Dispatch.T_POST || e.task == Dispatch.T_GUARD)
                    && w.city.fieldAt(w.city.zombieDist, e.x, e.y) > 18 && !e.aiming;
        return false;
    }

    /** A vehicle on the ground heading for this spot and close enough that stepping out would be a mistake. */
    boolean carComing(float x, float y) {
        for (int i = 0, n = w.fleet.vehicles.size(); i < n; i++) {
            Fleet.Vehicle v = w.fleet.vehicles.get(i);
            if (Fleet.offRoad(v) || v.broken || Math.abs(v.speed) < 8) continue;
            float dx = x - v.x, dy = y - v.y;
            float reach = 36 + Math.abs(v.speed) * 1.6f;
            if (dx * dx + dy * dy > reach * reach) continue;
            float d = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
            float s = v.speed < 0 ? -1 : 1;
            // In its path: ahead of it, and not far to one side.
            float along = ((float) Math.cos(v.angle) * dx + (float) Math.sin(v.angle) * dy) * s;
            if (along > -10 && Math.abs((float) Math.cos(v.angle) * dy - (float) Math.sin(v.angle) * dx) < 26) return true;
            if (d < 22) return true;
        }
        return false;
    }

    /** Walks one tile downhill in a distance field. Returns false if there is nowhere lower to go. */
    /** (10.31) The same, along a 16-bit field (the facilities'). */
    boolean followField(Entity e, City.WalkField field, float speed) {
        if (field == null) return false;
        int tx = (int) (e.x / City.T), ty = (int) (e.y / City.T);
        int cur = field.code(tx, ty);
        // (Out of its reach, or there already.)
        if (cur <= 1) return false;
        int best = 0, bx = -1, by = -1;
        for (int oy = -1; oy <= 1; oy++) {
            for (int ox = -1; ox <= 1; ox++) {
                if (ox == 0 && oy == 0) continue;
                int nx = tx + ox, ny = ty + oy;
                if (w.city.solidTile(nx, ny)) continue;
                if (ox != 0 && oy != 0 && (w.city.solidTile(tx + ox, ty) || w.city.solidTile(tx, ty + oy))) continue;
                int code = field.code(nx, ny);
                if (code == 0) continue;
                // (How much nearer than here: the place itself nearest of all.)
                int v = code == 1 ? -100000 : City.wrapDiff(code, cur);
                if (v < best) {
                    best = v;
                    bx = nx;
                    by = ny;
                }
            }
        }
        return stepTo(e, tx, ty, bx, by, speed);
    }

    /**
     * To (x, y), d away, by the field that leads there; or, failing that, the way there as anyone would find it
     * ((10.54) a call without a way worked out). ((12.0) A field with no way on from where they stand (a wall or a
     * barricade put up since it was worked out) is left alone for a few seconds: it used to fail on one tile and
     * work on the next, and they went back and forth between the two - and straight at the call - and stood there.)
     */
    void headFor(Entity e, City.WalkField field, float x, float y, float d, float speed) {
        if (w.time >= e.fieldOffUntil && followField(e, field, speed)) return;
        if (field != null && w.time >= e.fieldOffUntil) e.fieldOffUntil = w.time + 4;
        if (d > 60) w.wilds.walkToSpot(e, x, y, speed);
        else w.wilds.steerClear(e, (x - e.x) / d, (y - e.y) / d, speed);
    }

    boolean followField(Entity e, int[] field, float speed) {
        if (field == null) return false;
        int w = this.w.city.w;
        int tx = (int) (e.x / City.T), ty = (int) (e.y / City.T);
        int cur = field[this.w.city.tileIndex(e.x, e.y)];
        if (cur >= City.FAR || cur == 0) return false;
        int best = cur, bx = -1, by = -1;
        for (int oy = -1; oy <= 1; oy++) {
            for (int ox = -1; ox <= 1; ox++) {
                if (ox == 0 && oy == 0) continue;
                int nx = tx + ox, ny = ty + oy;
                if (this.w.city.solidTile(nx, ny)) continue;
                if (ox != 0 && oy != 0 && (this.w.city.solidTile(tx + ox, ty) || this.w.city.solidTile(tx, ty + oy))) continue;
                int v = field[ny * w + nx];
                if (v < best) {
                    best = v;
                    bx = nx;
                    by = ny;
                }
            }
        }
        return stepTo(e, tx, ty, bx, by, speed);
    }

    /** One step of following a field: towards tile (bx, by) from (tx, ty), or false if there's nowhere better. */
    boolean stepTo(Entity e, int tx, int ty, int bx, int by, float speed) {
        int w = this.w.city.w;
        if (bx < 0) return false;
        // Calm people wait at the kerb for traffic to pass.
        if (keepsRules(e) && this.w.city.tiles[by * w + bx] == City.ROAD && this.w.city.tiles[ty * w + tx] != City.ROAD
                && carComing(bx * City.T + City.T / 2f, by * City.T + City.T / 2f)) {
            steer(e, 0, 0, 0);
            return true;
        }
        float gx = bx * City.T + City.T / 2f - e.x, gy = by * City.T + City.T / 2f - e.y;
        float d = (float) Math.sqrt(gx * gx + gy * gy) + 0.001f;
        steer(e, gx / d, gy / d, speed);
        return true;
    }

    /** Runs away: prefers tiles far from zombies that also point away from the threat. */
    /** ((28.3) Solid and not a tree: what can't be slipped past. */
    private boolean wallish(int x, int y) {
        City c = w.city;
        return c.solidTile(x, y) && (x < 0 || y < 0 || x >= c.w || y >= c.h || c.tiles[y * c.w + x] != City.TREE);
    }

    void flee(Entity e, float ax, float ay, float speed) {
        int w = this.w.city.w;
        int tx = (int) (e.x / City.T), ty = (int) (e.y / City.T);
        float bestScore = -Float.MAX_VALUE;
        float bx = ax, by = ay;
        for (int oy = -1; oy <= 1; oy++) {
            for (int ox = -1; ox <= 1; ox++) {
                if (ox == 0 && oy == 0) continue;
                int nx = tx + ox, ny = ty + oy;
                // ((10.51) A fence or the highway barrier with open ground past it is a way out too: over it.)
                // ((28.3) And a tree with open ground past it: a tree is only its trunk, and they slip past it. In a
                // wood, taking every tree for a wall left people in the gaps between them with nowhere to run, going
                // to and fro from one gap to the next.)
                boolean over = false;
                int past = 0;
                if (this.w.city.solidTile(nx, ny)) {
                    if (nx < 1 || ny < 1 || nx >= w - 1 || ny >= this.w.city.h - 1) continue;
                    byte k = this.w.city.tiles[ny * w + nx];
                    if (k == City.FENCE && (ox == 0 || oy == 0) && !this.w.city.solidTile(nx + ox, ny + oy)) {
                        over = true;
                        nx += ox;
                        ny += oy;
                    } else if (k == City.TREE) {
                        // (Up to six trees deep, to the open ground past them.)
                        while (past < 6 && this.w.city.tiles[ny * w + nx] == City.TREE) {
                            past++;
                            nx += ox;
                            ny += oy;
                            if (nx < 0 || ny < 0 || nx >= w || ny >= this.w.city.h) break;
                        }
                        if (nx < 0 || ny < 0 || nx >= w || ny >= this.w.city.h || this.w.city.solidTile(nx, ny)) continue;
                    } else continue;
                }
                // (Not cutting a corner past a wall or a fence; past a tree, yes.)
                if (ox != 0 && oy != 0 && (wallish(tx + ox, ty) || wallish(tx, ty + oy))) continue;
                float len = (ox != 0 && oy != 0) ? 1.414f : 1f;
                float dot = (ox * ax + oy * ay) / len;
                int v = Math.min(12, this.w.city.zombieDist[ny * w + nx]);
                float score = v * 1.2f + dot * 2.5f - (over ? 1.5f : past * 0.4f);
                if (score > bestScore) {
                    bestScore = score;
                    bx = ox / len;
                    by = oy / len;
                }
            }
        }
        // ((15.9) Straight away from them only when that's open ground: on a beach or out on a pier it's the water,
        // and they ran into the sea's edge and stood there pushing at it.)
        float reach = e.radius + 6;
        boolean awayOpen = !this.w.city.circleBlocked(e.x + ax * reach, e.y + ay * reach, e.radius);
        steer(e, bx * 0.7f + (awayOpen ? ax * 0.3f : 0), by * 0.7f + (awayOpen ? ay * 0.3f : 0), speed);
        float l = (float) Math.sqrt(e.mx * e.mx + e.my * e.my) + 0.001f;
        e.mx /= l;
        e.my /= l;
    }

    void move(Entity e, float dt) {
        // ((17.6) In a gunfight: to cover, and pinned down by fire.)
        if (World.COVER) w.armedAI.coverStep(e, dt);
        // (10.51) Halfway over a fence or the highway barrier: nothing else until they're down the other side.
        if (e.vaultT > 0) {
            e.vaultT -= dt;
            float f = 1 - Math.max(0, e.vaultT) / Math.max(0.05f, e.vaultDur);
            e.x = e.vaultX0 + (e.vaultX1 - e.vaultX0) * f;
            e.y = e.vaultY0 + (e.vaultY1 - e.vaultY0) * f;
            e.vx = e.vy = 0;
            return;
        }
        // The living tire: a sprint lasts a few seconds, then they slow to a jog until they get their breath back.
        // The dead never tire.
        // ((10.51) A real sprint: running for their life with breath to spare they go flat out, for ten seconds or
        // so, then jog until they've got it back - quicker standing still than on the move.)
        // ((28.9) Winded, they jog - under a sprint - and get their breath back as they go, and don't sprint again till
        // they've half of it back. They used to jog at a pace that still counted as a sprint: their breath never came
        // back while they kept moving, and anyone running to a call was out of breath the whole way.)
        if (!e.isUndead() && e.type != Entity.DOG) {
            if (e.winded) e.want = Math.min(e.want, e.speed * 1.3f);
            if (e.want > e.speed * 1.35f) {
                if (e.fleeTimer > 0 && e.stamina > 0.3f && e != w.controlled) e.want *= 1.12f;
                e.stamina -= dt * World.STAMINA_DRAIN;
                if (e.stamina < 0.15f) e.winded = true;
            } else {
                e.stamina = Math.min(1, e.stamina + dt * (e.want < 1 ? 0.12f : 0.07f));
                if (e.stamina >= 0.5f) e.winded = false;
            }
            if (e.stamina < 0) e.stamina = 0;
        }
        // ((14.5) The long dead have rotted: slower on their feet.)
        // ((20.0) And quicker or slower by the difficulty.)
        if (e.isUndead() && e.want > 0) e.want *= (1 - w.zombieAI.rot(e)) * w.zPace;
        // ((21.3) A runner runs: wherever it's going - after prey, to a noise, roaming, with a horde - it goes there at
        // a run, never a shamble.)
        if (e.type == Entity.RUNNER && e.isUndead() && e.want > 0.5f && e != w.controlled) e.want = Math.max(e.want, e.runSpeed * w.zPace);
        // ((21.0) Tangled in razor wire.)
        if (w.defences.any() && e.isUndead() && e.want > 0) e.want *= w.defences.snag(w, e, dt);
        // ((15.0) A horse is quickest on grass, trails and open ground.)
        if (e.role == Entity.ROLE_MOUNTED && e.type == Entity.COP && e.want > 0) {
            int t = w.city.tileIndex(e.x, e.y);
            if (t >= 0 && (w.city.tiles[t] == City.GRASS || w.city.tiles[t] == City.TRAIL || w.city.tiles[t] == City.DIRT)) e.want *= 1.15f;
        }
        // ((29.2) Running from the dead, never at them: a fleeing civilian whose way on (home, after their family, to the
        // station) leads straight at one of the dead close by veers off round it instead. In a measured outbreak one in
        // fourteen of the seconds civilians spent near the dead they spent running at them, knowing where they were.)
        // (Not one who's standing to fight it.)
        if (e.type == Entity.CIVILIAN && e.fleeTimer > 0 && e.goal != World.G_FIGHT && e.want > 1 && (e.mx != 0 || e.my != 0) && e != w.controlled) {
            Entity z = w.spatial.nearestZombie(e.x, e.y, 35);
            if (z != null) {
                float zx = z.x - e.x, zy = z.y - e.y, zd = (float) Math.sqrt(zx * zx + zy * zy) + 0.001f;
                float ml = (float) Math.sqrt(e.mx * e.mx + e.my * e.my) + 0.001f, mx = e.mx / ml, my = e.my / ml;
                if ((zx * mx + zy * my) / zd > 0.5f) {
                    // Off to the side it was already leaning, and away.
                    float side = zx * my - zy * mx >= 0 ? 1 : -1;
                    float nx = -zx / zd * 0.6f + (-zy / zd) * side * 0.8f, ny = -zy / zd * 0.6f + (zx / zd) * side * 0.8f;
                    float nl = (float) Math.sqrt(nx * nx + ny * ny) + 0.001f;
                    e.mx = nx / nl;
                    e.my = ny / nl;
                }
            }
        }
        if (e.want > 1 && (e.mx != 0 || e.my != 0)) lookAhead(e);
        // ((11.6) A long step - the phone catching up after a slow frame, or the faster speeds - is walked as several
        // ordinary ones for anyone being worked out every frame (everyone on screen): in one long step the easing
        // into a new direction was all at once, so a single odd moment's wish to go the other way turned them right
        // round and back, and people and units on screen jittered and twitched whenever the frame rate dipped.)
        if (dt > World.SUB_STEP * 1.25f && e.lodStep == 1) {
            int n = Math.min(4, (int) Math.ceil(dt / World.SUB_STEP));
            for (int i = 0; i < n; i++) moveStep(e, dt / n);
            return;
        }
        moveStep(e, dt);
    }

    void moveStep(Entity e, float dt) {
        float k = Math.min(1, dt * 10);
        e.vx += (e.mx * e.want - e.vx) * k;
        e.vy += (e.my * e.want - e.vy) * k;
        float sx = e.vx * dt, sy = e.vy * dt;
        if (w.city.elev != null && (sx != 0 || sy != 0)) {
            // Uphill is hard going (for the dead too), downhill a little quicker.
            float l = (float) Math.sqrt(sx * sx + sy * sy);
            float g = w.city.rise(e.x, e.y, sx / l, sy / l);
            float f = g > 0 ? 1 / (1 + g * 0.3f) : Math.min(1.1f, 1 - g * 0.04f);
            sx *= f;
            sy *= f;
            // Wading through a stream.
            if (w.city.ford != null && w.city.isFord(w.city.tileIndex(e.x, e.y))) {
                sx *= 0.55f;
                sy *= 0.55f;
            }
        }
        float bx = e.x, by = e.y;
        boolean moved = tryMove(e, sx, sy);
        if (!moved && e.want > 0) {
            e.blocked = true;
            // (10.51) The living, running, up against a fence or the highway's concrete barrier with open ground
            // beyond: over it they go. (The dead can't climb.)
            if (!e.isUndead() && e.type != Entity.DOG && e.want > e.speed * 1.2f && e.ride == null) tryVault(e, e.radius + 3);
        }
        float went = Math.abs(e.x - bx) + Math.abs(e.y - by);
        if (e.want > 5 && went < e.want * dt * 0.2f) e.stuckTime += dt;
        else e.stuckTime = Math.max(0, e.stuckTime - dt * 2);
        float sp = (float) Math.sqrt(e.vx * e.vx + e.vy * e.vy);
        if (sp > 2 && !e.aiming) e.angle = World.turn(e.angle, World.fastAtan2(e.vy, e.vx), dt * 10);
        e.phase += sp * dt * 0.35f;
    }

    void lookAhead(Entity e) {
        float l = (float) Math.sqrt(e.mx * e.mx + e.my * e.my);
        if (l < 0.01f) return;
        float dx = e.mx / l, dy = e.my / l, reach = e.radius + 5;
        if (!w.city.circleBlocked(e.x + dx * reach, e.y + dy * reach, e.radius)) {
            e.climbPress = 0;
            return;
        }
        // (10.51) Running at a fence they can get over: over it, not round.
        if (!e.isUndead() && e.type != Entity.DOG && e.want > e.speed * 1.2f && e.ride == null && e != w.controlled
                && tryVault(e, reach + 3)) return;
        // ((13.4) The dead climb over too - fences, barricades, the quarantine walls - but only after pressing up
        // against it a while, and slowly.)
        if (e.isUndead() && e != w.controlled && e.want > 1 && zombieClimb(e, reach + 3)) return;
        for (int step = 1; step <= 4; step++) {
            for (int side = 0; side < 2; side++) {
                float a = step * 0.42f * (side == 0 ? e.slideSide : -e.slideSide);
                float c = (float) Math.cos(a), s = (float) Math.sin(a);
                float rx = dx * c - dy * s, ry = dx * s + dy * c;
                if (!w.city.circleBlocked(e.x + rx * reach, e.y + ry * reach, e.radius)) {
                    e.mx = rx * l;
                    e.my = ry * l;
                    if (side == 1) e.slideSide = -e.slideSide;
                    return;
                }
            }
        }
    }

    /**
     * ((13.4) Whether the first thing in the way on the straight line from z to t is a fence or a barricade, one tile
     * thick, with open ground the other side: something to climb.
     */
    boolean climbable(Entity z, Entity t) {
        float dx = t.x - z.x, dy = t.y - z.y, d = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
        dx /= d;
        dy /= d;
        for (float s = City.T * 0.5f; s < Math.min(d, City.T * 3); s += City.T * 0.5f) {
            int i = w.city.tileIndex(z.x + dx * s, z.y + dy * s);
            if (i < 0 || !w.city.solid[i]) continue;
            if (w.city.tiles[i] != City.FENCE) return false;
            int j = w.city.tileIndex(z.x + dx * (s + City.T), z.y + dy * (s + City.T));
            return j >= 0 && !w.city.solid[j];
        }
        return false;
    }

    /** ((13.4) One of the dead up against a barrier it wants to get past: over it, once it's pushed at it a while. */
    boolean zombieClimb(Entity z, float probe) {
        // ((25.0) A giant doesn't climb: it goes through.)
        if (z.type == Entity.GIANT) return false;
        float was = z.climbPress;
        z.climbPress += w.stepDt * Math.max(1, z.lodStep);
        // ((26.0) With a crowd of them pressed up against the same stretch, they climb up on each other's backs: over
        // sooner, and quicker. How many there are is looked at a few times while it pushes, not every step.)
        if (z.climbPress < World.CLIMB_PRESS * 0.3f) return false;
        if (z.climbPress < World.CLIMB_PRESS && (int) (was * 5) == (int) (z.climbPress * 5)) return false;
        int pile = w.spatial.countZombiesNear(z.x, z.y, World.PILE_REACH);
        float bonus = World.pileBonus(pile);
        if (z.climbPress < World.CLIMB_PRESS / bonus) return false;
        if (!tryVault(z, probe)) {
            z.climbPress = 0;
            return false;
        }
        z.climbPress = 0;
        float t = z.type == Entity.BRUTE ? World.CLIMB_BRUTE : z.type == Entity.RUNNER ? World.CLIMB_RUNNER : World.CLIMB_TIME;
        z.vaultDur = z.vaultT = t * (0.85f + w.rnd.nextFloat() * 0.3f) / (float) Math.sqrt(bonus);
        w.emit(Sfx.CLIMB, z.x, z.y);
        if (pile >= World.PILE_SAY) pileUp(z.x, z.y, pile);
        return true;
    }

    /** ((26.0) A pile of the dead going over a wall: picked out on the screen, and now and then on the radio. */
    void pileUp(float x, float y, int n) {
        w.stats.highlight("Climbing over each other!", x, y);
        if (w.time - w.pileSaid < 60) return;
        w.pileSaid = w.time;
        w.dispatch.say(Dispatch.WHO_INFO, null, "The dead are piling up against the wall on " + w.city.placeName(x, y)
                + " - " + n + " of them in a heap, climbing over each other to get in!", x, y);
    }

    boolean tryVault(Entity e, float probe) {
        return tryVault(e, probe, false);
    }

    /** ((13.4) zoneWall: a stretch of a safe zone's wall will do too (the dead climbing in). */
    boolean tryVault(Entity e, float probe, boolean zoneWall) {
        float l = (float) Math.sqrt(e.mx * e.mx + e.my * e.my);
        if (l < 0.01f) return false;
        float ux = e.mx / l, uy = e.my / l;
        // Straight at it, along the grid (a fence is a row of tiles): the tile in front, and the one past it.
        if (Math.abs(ux) > Math.abs(uy)) {
            ux = Math.signum(ux);
            uy = 0;
        } else {
            uy = Math.signum(uy);
            ux = 0;
        }
        int t1 = w.city.tileIndex(e.x + ux * probe, e.y + uy * probe);
        boolean wall = zoneWall && t1 >= 0 && w.dispatch.wallOwner != null && w.dispatch.wallOwner[t1] != null && w.dispatch.wallSeg[t1] >= 0;
        if (t1 < 0 || (w.city.tiles[t1] != City.FENCE && !wall)) return false;
        int fx = t1 % w.city.w, fy = t1 / w.city.w;
        float lx = (fx + 0.5f) * City.T + ux * City.T, ly = (fy + 0.5f) * City.T + uy * City.T;
        // Keep the line along the fence they were at, only the crossing is to the tile's middle.
        if (ux != 0) ly = e.y;
        else lx = e.x;
        if (w.city.circleBlocked(lx, ly, e.radius)) return false;
        e.vaultT = e.vaultDur = World.VAULT_TIME;
        e.vaultX0 = e.x;
        e.vaultY0 = e.y;
        e.vaultX1 = lx;
        e.vaultY1 = ly;
        e.blocked = false;
        e.path = null;
        return true;
    }

    boolean tryMove(Entity e, float sx, float sy) {
        boolean ok = true;
        float bx0 = e.x, by0 = e.y;
        if (sx != 0) {
            if (!w.city.circleBlocked(e.x + sx, e.y, e.radius)) e.x += sx;
            else ok = false;
        }
        if (sy != 0) {
            if (!w.city.circleBlocked(e.x, e.y + sy, e.radius)) e.y += sy;
            else ok = false;
        }
        if (!ok && (sx != 0 || sy != 0) && e.x == bx0 && e.y == by0) {
            // Walked straight into something round (a tree trunk): step off at an angle, whichever side is free,
            // keeping to the side we last went round on so we don't dither.
            for (float t : World.SIDESTEPS) {
                float a = t * e.slideSide;
                float c = (float) Math.cos(a), s = (float) Math.sin(a);
                float rx = sx * c - sy * s, ry = sx * s + sy * c;
                if (!w.city.circleBlocked(e.x + rx, e.y + ry, e.radius)) {
                    e.x += rx;
                    e.y += ry;
                    if (t < 0) e.slideSide = -e.slideSide;
                    break;
                }
            }
        }
        return ok;
    }

    void separate() {
        // ((13.1) In a big city, those off screen get out of each other's way every other frame, a little harder each
        // time: it was a good part of each frame, for crowds nobody is looking at.)
        boolean big = w.entities.size() > 6000;
        // ((13.5) Every fourth step with over eighteen thousand.)
        int sepEvery = w.entities.size() > 18000 ? 4 : 2;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            // (Not those passed over this step: like those with lodStep 0, they've not moved.)
            if (i < w.lodWake.length && w.lodWake[i] - w.lodFrame > 0 && w.lodSet[i] != w.lodFrame) continue;
            if (i < w.lodQuiet.length && w.lodQuiet[i] == w.lodFrame) continue;
            Entity e = w.entities.get(i);
            if (e.dead || e.lodStep == 0) continue;
            if (big && (i + w.lodFrame) % sepEvery != 0 && !w.onScreen(e)) continue;
            // ((16.8) And in the biggest crowds, not at all for someone going calmly about their day off screen: nobody
            // sees two of them brush past each other, and they're kept apart again the moment they're looked at. Over
            // a tenth of each step on the biggest maps.)
            if (sepEvery == 4 && e.type == Entity.CIVILIAN && e.task == Dispatch.T_NONE && e.fleeTimer <= 0 && e.stun <= 0 && !w.onScreen(e)) continue;
            // ((10.34) Someone standing still is left be: whoever walks into them steps aside themselves.)
            if (e.want <= 0.5f && e.stun <= 0 && e.vx == 0 && e.vy == 0 && w.entities.size() > 6000) continue;
            // Only the cells within touching distance (the biggest body is under 8 across, plus a little for
            // movement since the grid was built).
            float reach = e.radius + 10;
            int cx0 = Math.max(0, (int) ((e.x - reach) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((e.x + reach) / World.CELL));
            int cy0 = Math.max(0, (int) ((e.y - reach) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((e.y + reach) / World.CELL));
            float pushX = 0, pushY = 0, ex = e.x, ey = e.y, er = e.radius;
            // ((27.4) The grid's arrays held here, not looked up again for each of everyone near.)
            final Entity[] sorted = w.sorted;
            final float[] sX = w.sortedX, sY = w.sortedY, sR = w.sortedR;
            final int[] start = w.cellStart, count = w.cellCount;
            final int gw = w.gw;
            for (int cy = cy0; cy <= cy1; cy++) {
                for (int cx = cx0; cx <= cx1; cx++) {
                    int c = cy * gw + cx;
                    for (int k = start[c], end = start[c] + count[c]; k < end; k++) {
                        // ((13.1) Where they were when the grid was made, with a little to spare for this frame's
                        // steps: only the few that close are looked at properly.)
                        float sx = ex - sX[k], sy = ey - sY[k], sm = er + sR[k] + 4;
                        if (sx * sx + sy * sy >= sm * sm) continue;
                        Entity o = sorted[k];
                        if (o == e || o.dead) continue;
                        float ddx = e.x - o.x, ddy = e.y - o.y;
                        float min = e.radius + o.radius;
                        // ((17.1) Indoors, in the doorways and between the desks, people squeeze past each other rather
                        // than jam solid (not past the dead).)
                        if (w.time - e.inT < 1 && w.time - o.inT < 1 && e.isZombie() == o.isZombie()) min *= 0.35f;
                        // ((26.0) The dead pushing at a wall crush up together into a heap against it.)
                        else if (e.climbPress > 0 && o.climbPress > 0) min *= 0.6f;
                        float d2 = ddx * ddx + ddy * ddy;
                        if (d2 >= min * min) continue;
                        float d = (float) Math.sqrt(d2);
                        if (d < 0.01f) {
                            ddx = w.rnd.nextFloat() - 0.5f;
                            ddy = w.rnd.nextFloat() - 0.5f;
                            d = 0.5f;
                        }
                        // A panicking crowd tramples people (but not a group that's sticking together).
                        if (e.type == Entity.CIVILIAN && o.type == Entity.CIVILIAN && e.fleeTimer > 0 && e.want > e.speed * 1.3f
                                && o.stun <= 0 && e.leader == null && o.leader != e && e.leader != o && w.rnd.nextFloat() < 0.012f) {
                            o.stun = 1.4f;
                            // ((29.1) Knocked down and hurt, but not trampled to death: never below half their health. In a
                            // crowd running from the dead, being trampled killed several times as many as the dead did.)
                            if (o.hp > o.maxHp * 0.5f) o.hp = Math.max(o.maxHp * 0.5f, o.hp - 4);
                            o.hurt = 1;
                            w.trampled++;
                        }
                        float share = o.mass / (e.mass + o.mass);
                        float overlap = (min - d) * share * 0.5f;
                        pushX += ddx / d * overlap;
                        pushY += ddy / d * overlap;
                    }
                }
            }
            if (pushX != 0 || pushY != 0) tryMove(e, pushX, pushY);
        }
    }
}
