package com.instantaxtion.zombiesandbox;

import static com.instantaxtion.zombiesandbox.Fleet.*;

import java.util.ArrayList;

/**
 * ((27.3) Part of the vehicle code, out of Fleet.) How a vehicle drives along its route: the planned line ahead (corridor), junction rules and lights, following and queueing, giving way, kerbs, collisions, bridges and decks, and the step that moves it (driveStep). Also the grid of which vehicles are near which.
 */
final class FleetDriving {
    final Fleet fleet;
    final World w;
    final City city;

    FleetDriving(Fleet fleet) {
        this.fleet = fleet;
        this.w = fleet.w;
        this.city = fleet.city;
    }

    /**
     * Everyday traffic gives way to lights and sirens: one coming up behind and the car pulls over to the
     * kerb and slows to let it by; one crossing ahead (or racing up to the junction it's at) and it holds back.
     */
    void giveWay(Vehicle v, float dt) {
        if (v.yieldT > 0) v.yieldT -= dt;
        if (fleet.sirens.isEmpty() || v.yieldT > 0.3f) return;
        float fx = (float) Math.cos(v.angle), fy = (float) Math.sin(v.angle);
        for (int i = 0; i < fleet.sirens.size(); i++) {
            Vehicle o = fleet.sirens.get(i);
            if (o == v) continue;
            float dx = o.x - v.x, dy = o.y - v.y, d2 = dx * dx + dy * dy;
            if (d2 > 200 * 200) continue;
            float ahead = dx * fx + dy * fy;
            float same = fx * (float) Math.cos(o.angle) + fy * (float) Math.sin(o.angle);
            if (ahead < -8 && same > 0.6f) {
                // Coming up behind in the same direction: over to the kerb, nearly stopped.
                v.yieldT = 2.5f;
                v.yieldStop = false;
                return;
            }
            // (10.51) Coming the other way down the street: over to the kerb too, to leave it the middle.
            if (ahead > 8 && same < -0.6f && d2 < 130 * 130) {
                v.yieldT = 1.8f;
                v.yieldStop = false;
                return;
            }
            float ox = (float) Math.cos(o.angle), oy = (float) Math.sin(o.angle);
            boolean closing = -dx * ox - dy * oy > 0;
            if (d2 < 70 * 70 && closing && Math.abs(same) < 0.6f) {
                // Coming across the junction ahead: hold back until it's through.
                v.yieldT = 1.2f;
                v.yieldStop = true;
                return;
            }
        }
    }

    /** (10.54) How far a car at (x, y) can move towards the kerb along (nx, ny) before its side is against it. */
    float kerbGap(float x, float y, float nx, float ny) {
        for (int k = 1; k <= 30; k++) {
            int tx = (int) ((x + nx * k) / City.T), ty = (int) ((y + ny * k) / City.T);
            if (tx < 0 || ty < 0 || tx >= city.w || ty >= city.h) return Math.max(0, k - 6.5f);
            if (city.tiles[ty * city.w + tx] != City.ROAD || city.onHighway(x + nx * k, y + ny * k) != city.onHighway(x, y))
                return Math.max(0, k - 6.5f);
        }
        return 23.5f;
    }

    /** Arrived at a scene: pull in to the kerb, nose angled in, rather than stopping in the middle of the lane. */
    void kerbAtScene(Vehicle v, float dt) {
        // ((28.0) Still in a junction box (nowhere free off it last time, or shoved into it since): another look every
        // couple of seconds.)
        if (v.kerbed && !v.pulling && v.player == null && (int) (v.anim * 0.5f) != (int) ((v.anim - dt) * 0.5f)
                && city.junctionIdAt(v.x, v.y) >= 0) offLane(v);
        if (!v.kerbed) {
            v.kerbed = true;
            v.pullOff = false;
            // ((28.0) Stopped in a junction (or, help from outside, in the middle of a wide road): off the lane - the
            // kerb of a street off it, a parking lot, the verge. Left in the box, it blocked the junction for everyone,
            // and the next car in for the call stopped behind it, and the next.)
            if (city.junctionIdAt(v.x, v.y) >= 0) offLane(v);
            float nx = (float) -Math.sin(v.angle), ny = (float) Math.cos(v.angle);
            int hand = 1;
            for (float s = 4; s <= 22; s += 3) {
                float x = v.x + nx * s * hand, y = v.y + ny * s * hand;
                int tx = (int) (x / City.T), ty = (int) (y / City.T);
                if (tx < 0 || ty < 0 || tx >= city.w || ty >= city.h) break;
                byte t = city.tiles[ty * city.w + tx];
                if (t == City.ROAD || t == City.CAR) continue;
                // ((27.5) Right in against it - 6.5 off the kerb left it half in the lane, and the traffic behind
                // queued for it.) (Not in a junction: there it stays where it is.)
                if (city.junctionIdAt(v.x, v.y) >= 0) break;
                v.pullX = x - nx * hand * 4.5f;
                v.pullY = y - ny * hand * 4.5f;
                v.pulling = true;
                break;
            }
            if (!v.pulling && v.reinforcement && v.player == null) offLane(v);
        }
        if (v.pulling) {
            float dx = v.pullX - v.x, dy = v.pullY - v.y, d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d < 0.5f || (!v.pullOff && !city.drivable(v.pullX, v.pullY))) v.pulling = v.pullOff = false;
            else {
                float step = Math.min(d, (v.pullOff ? 30 : 18) * dt);
                v.x += dx / d * step;
                v.y += dy / d * step;
                // Nose in towards the kerb a little.
                v.angle += dt * 0.5f * Math.min(1, d / 4);
            }
        }
    }

    /**
     * ((28.0) A spot off the lane for a car stopped where it shouldn't stay (a junction box, the middle of the road): the
     * nearest within four tiles of the kerb of a road that isn't a junction, a car park or yard, or the verge beside the
     * road - clear of other vehicles and of anything solid. It slides over there (kerbAtScene). Nothing in reach: it
     * stays where it is.
     */
    void offLane(Vehicle v) {
        int tx0 = (int) (v.x / City.T), ty0 = (int) (v.y / City.T), W = city.w;
        float best = Float.MAX_VALUE, bx = 0, by = 0;
        for (int y = ty0 - 4; y <= ty0 + 4; y++)
            for (int x = tx0 - 4; x <= tx0 + 4; x++) {
                if (x < 1 || y < 1 || x >= W - 1 || y >= city.h - 1) continue;
                int i = y * W + x;
                if (city.solid[i]) continue;
                byte t = city.tiles[i];
                float px = (x + 0.5f) * City.T, py = (y + 0.5f) * City.T;
                if (city.junctionIdAt(px, py) >= 0) continue;
                if (t == City.ROAD) {
                    if (city.onHighway(px, py)) continue;
                    // The kerb: right in against the edge of the road, on the side with no road beside it.
                    int[] nb = {i - 1, i + 1, i - W, i + W};
                    int[] ox = {-1, 1, 0, 0}, oy = {0, 0, -1, 1};
                    boolean found = false;
                    for (int k = 0; k < 4 && !found; k++) {
                        byte n = city.tiles[nb[k]];
                        if (n == City.ROAD || n == City.CAR || n == City.WATER) continue;
                        px += ox[k] * 3.5f;
                        py += oy[k] * 3.5f;
                        found = true;
                    }
                    if (!found) continue;
                } else if (t == City.LOT || t == City.BASE || t == City.DIRT || t == City.GRASS || t == City.SAND) {
                    // A car park or a yard; or the verge, right beside the road.
                    if (t != City.LOT && t != City.BASE) {
                        boolean verge = false;
                        int[] nb = {i - 1, i + 1, i - W, i + W};
                        for (int n : nb) if (city.tiles[n] == City.ROAD && city.junctionIdAt((n % W + 0.5f) * City.T, (n / W + 0.5f) * City.T) < 0) verge = true;
                        if (!verge || (t != City.DIRT && city.steepAt(i))) continue;
                    }
                } else continue;
                float d = (float) World.hypot(px - v.x, py - v.y);
                if (d >= best || d > City.T * 4.5f) continue;
                if (vehicleWithin(px, py, 11, v)) continue;
                best = d;
                bx = px;
                by = py;
            }
        if (best == Float.MAX_VALUE) return;
        v.pullX = bx;
        v.pullY = by;
        v.pulling = true;
        v.pullOff = true;
    }

    /** ((28.0) Another vehicle (on the ground) with its centre within r of (x, y). */
    boolean vehicleWithin(float x, float y, float r, Vehicle self) {
        for (int i = 0; i < fleet.vehicles.size(); i++) {
            Vehicle o = fleet.vehicles.get(i);
            if (o == self || o.removedFromFleet || airborne(o)) continue;
            if (o.pulling && World.hypot(o.pullX - x, o.pullY - y) < r) return true;
            if (Math.abs(o.x - x) >= r || Math.abs(o.y - y) >= r) continue;
            if (World.hypot(o.x - x, o.y - y) < r) return true;
        }
        return false;
    }

    void buildGrid() {
        fleet.gridW = (int) (city.worldW() / GC) + 1;
        fleet.gridH = (int) (city.worldH() / GC) + 1;
        int cells = fleet.gridW * fleet.gridH;
        if (fleet.gridStart.length != cells + 1) {
            fleet.gridStart = new int[cells + 1];
            fleet.gridFill = new int[cells];
        }
        java.util.Arrays.fill(fleet.gridStart, 0);
        int n = fleet.vehicles.size();
        if (fleet.gridV.length < n) fleet.gridV = new Vehicle[n * 2];
        for (int i = 0; i < n; i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (offRoad(v)) continue;
            fleet.gridStart[cellOf(v.x, v.y) + 1]++;
        }
        for (int c = 0; c < cells; c++) fleet.gridStart[c + 1] += fleet.gridStart[c];
        System.arraycopy(fleet.gridStart, 0, fleet.gridFill, 0, cells);
        fleet.gridN = 0;
        for (int i = 0; i < n; i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (offRoad(v)) continue;
            int c = cellOf(v.x, v.y);
            v.gi = fleet.gridFill[c];
            fleet.gridV[fleet.gridFill[c]++] = v;
            fleet.gridN++;
        }
    }

    int cellOf(float x, float y) {
        int cx = Math.max(0, Math.min(fleet.gridW - 1, (int) (x / GC))), cy = Math.max(0, Math.min(fleet.gridH - 1, (int) (y / GC)));
        return cy * fleet.gridW + cx;
    }

    int gather(float x, float y, float r) {
        if (fleet.gridW == 0) return 0;
        int n = 0;
        int c0x = Math.max(0, (int) ((x - r) / GC)), c1x = Math.min(fleet.gridW - 1, (int) ((x + r) / GC));
        int c0y = Math.max(0, (int) ((y - r) / GC)), c1y = Math.min(fleet.gridH - 1, (int) ((y + r) / GC));
        for (int cy = c0y; cy <= c1y; cy++)
            for (int cx = c0x; cx <= c1x; cx++)
                for (int i = fleet.gridStart[cy * fleet.gridW + cx], ie = fleet.gridStart[cy * fleet.gridW + cx + 1]; i < ie; i++) {
                    Vehicle v = fleet.gridV[i];
                    if (v.removedFromFleet) continue;
                    if (n == fleet.found.length) fleet.found = java.util.Arrays.copyOf(fleet.found, n * 2);
                    fleet.found[n++] = v;
                }
        return n;
    }

    /** Vehicles bump into each other: hard hits damage both. */
    void collide() {
        for (int i = 0; i < fleet.gridN; i++) {
            Vehicle a = fleet.gridV[i];
            int acx = Math.max(0, Math.min(fleet.gridW - 1, (int) (a.x / GC))), acy = Math.max(0, Math.min(fleet.gridH - 1, (int) (a.y / GC)));
            for (int gy = Math.max(0, acy - 1); gy <= Math.min(fleet.gridH - 1, acy + 1); gy++)
            for (int gx = Math.max(0, acx - 1); gx <= Math.min(fleet.gridW - 1, acx + 1); gx++)
            for (int j = fleet.gridStart[gy * fleet.gridW + gx], je = fleet.gridStart[gy * fleet.gridW + gx + 1]; j < je; j++) {
                if (j <= i) continue;
                Vehicle b = fleet.gridV[j];
                if (levels(a, b)) continue;
                float dx = b.x - a.x, dy = b.y - a.y;
                float d2 = dx * dx + dy * dy;
                if (d2 >= 22 * 22) continue;
                float d = (float) Math.sqrt(d2) + 0.001f, nx = dx / d, ny = dy / d;
                // ((28.0) How far apart they have to be along the line between them, by their shapes: nose to tail most
                // of both their lengths, side by side most of their widths. It was a circle a little over half a car
                // long round each, so two could sit with their bodies over each other - in a queue, or crossing in a
                // junction - before anything pushed them apart.)
                float reach = (extent(a, nx, ny) + extent(b, nx, ny)) * 0.88f;
                if (d >= reach) continue;
                // Closing speed along the line between them.
                float va = a.speed * ((float) Math.cos(a.angle) * nx + (float) Math.sin(a.angle) * ny);
                float vb = b.speed * ((float) Math.cos(b.angle) * nx + (float) Math.sin(b.angle) * ny);
                float closing = va - vb;
                float push = (reach - d) * 0.5f;
                // Nobody is shoved off the road (onto grass, a kerb, a median): the other car takes the push.
                boolean aOk = city.drivable(a.x - nx * push, a.y - ny * push), bOk = city.drivable(b.x + nx * push, b.y + ny * push);
                float pa = aOk ? (bOk ? push : push * 2) : 0, pb = bOk ? (aOk ? push : push * 2) : 0;
                if (pa > 0 && !city.drivable(a.x - nx * pa, a.y - ny * pa)) pa = push;
                if (pb > 0 && !city.drivable(b.x + nx * pb, b.y + ny * pb)) pb = push;
                a.x -= nx * pa;
                a.y -= ny * pa;
                b.x += nx * pb;
                b.y += ny * pb;
                // (A nudge in a queue is just a nudge: it takes a real impact to make a crash.)
                // (A crash is the same real closing speed as before 10.20, now that everything drives faster.)
                if (closing > 45 * DRIVE_SCALE && a.crashCd <= 0 && b.crashCd <= 0) {
                    a.crashCd = b.crashCd = 0.6f;
                    float dmg = closing * 0.35f / DRIVE_SCALE;
                    fleet.damage(a, dmg, false);
                    fleet.damage(b, dmg, false);
                    w.skid(a.x, a.y, a.angle, Math.min(14, a.speed * 0.2f + 4));
                    w.skid(b.x, b.y, b.angle, Math.min(14, b.speed * 0.2f + 4));
                    a.speed *= 0.3f;
                    b.speed *= 0.3f;
                    float cx = (a.x + b.x) / 2, cy = (a.y + b.y) / 2;
                    w.emit(Sfx.CRASH, cx, cy);
                    for (int k = 0; k < 8; k++)
                        w.particle(cx, cy, w.rnd.nextFloat() * 60 - 30, w.rnd.nextFloat() * 60 - 30, 0.25f, 0.7f,
                                0xFFFFD27A, World.P_DOT);
                    w.noise(cx, cy, 160);
                }
            }
        }
    }

    /** ((28.0) How far v's body reaches from its centre along (nx, ny): its half length and half width, by its angle. */
    static float extent(Vehicle v, float nx, float ny) {
        float c = Math.abs((float) Math.cos(v.angle) * nx + (float) Math.sin(v.angle) * ny);
        return v.length() * c + v.halfWidth() * (float) Math.sqrt(Math.max(0, 1 - c * c));
    }

    float junctionRule(Vehicle v, float dt) {
        v.held = false;
        // Lights and sirens (and anyone racing to a rescue) go through on red, but slow down for the junction
        // and wait for anything already crossing it.
        boolean emergency = !fleet.calmRules && ((v.type != CAR && !(v.patrol && v.state == PATROL)) || v.riders.size() > 0 && v.rescue);
        if (emergency && v.type != CRUISER && v.type != AMBULANCE && v.type != TRUCK && v.type != CAR)
            return Float.MAX_VALUE;
        int here = city.junctionIdAt(v.x, v.y);
        if (here >= 0) {
            // Through: next time round it has to wait its turn again.
            if (here == v.clearedJ) v.clearedJ = -1;
            v.waitJ = -1;
            return Float.MAX_VALUE;
        }
        // Follow the route from the car's own tile to the first junction tile: that step is the way in,
        // and the line across the end of the tile before it is where the junction starts.
        int id = -1, W = city.w, dirx = 0, diry = 0;
        float dist = 0;
        if (v.field != null && v.pathT >= 0) {
            int prev = city.tileIndex(v.x, v.y), t = v.pathT;
            // ((24.0) The junction ahead is the same as last time while it's on the same tile of the same route: looked
            // for along the route only when that changes, not every step for every car.)
            if (v.jaOf != v.field || v.jaFrom != prev || v.jaT != t || v.jaDx != v.pdx || v.jaDy != v.pdy) {
                v.jaOf = v.field;
                v.jaFrom = prev;
                v.jaT = t;
                v.jaDx = v.pdx;
                v.jaDy = v.pdy;
                v.jaId = -1;
                for (int k = 0; k < 7; k++) {
                    int nid = t == prev ? -1 : city.junctionIdAt((t % W + 0.5f) * City.T, (t / W + 0.5f) * City.T);
                    if (nid >= 0) {
                        int sx = t % W - prev % W, sy = t / W - prev / W;
                        if (Math.abs(sx) + Math.abs(sy) == 1) {
                            dirx = sx;
                            diry = sy;
                        } else {
                            dirx = v.pdx;
                            diry = v.pdy;
                        }
                        if (dirx == 0 && diry == 0) break;
                        v.jaId = nid;
                        v.jaTile = t;
                        v.jaDirX = dirx;
                        v.jaDirY = diry;
                        break;
                    }
                    int next = downhill(v.field, t);
                    if (next == t) break;
                    prev = t;
                    t = next;
                }
            }
            if (v.jaId >= 0) {
                t = v.jaTile;
                dirx = v.jaDirX;
                diry = v.jaDirY;
                float ex = (t % W + 0.5f - dirx * 0.5f) * City.T, ey = (t / W + 0.5f - diry * 0.5f) * City.T;
                dist = (ex - v.x) * dirx + (ey - v.y) * diry;
                if (dist < 90) id = v.jaId;
            } else dirx = diry = 0;
        }
        if (id < 0) {
            v.clearedJ = -1;
            v.waitJ = -1;
            return Float.MAX_VALUE;
        }
        if (id == v.clearedJ) return Float.MAX_VALUE;
        int[] j = city.junctions.get(id);
        // ((15.0) A traffic officer on the junction: lights and sirens are waved straight through, and the rest held
        // at the line while they go.)
        if (dist < 90 && w.trafficOfficer(id) != null) {
            if (emergency) {
                w.holdJunction(id, v);
                return Float.MAX_VALUE;
            }
            float stopLine = dist - City.T - v.length() - 1;
            if (w.junctionHeldFor(id) && stopLine > -6 && v.player == null) {
                v.held = stopLine < 6;
                return Math.max(0, stopLine) * 1.8f;
            }
        }
        // Stop with the front bumper at the line, before the zebra crossing.
        float stopAt = dist - City.T - v.length() - 1;
        float approach = Math.max(0, stopAt) * 1.8f;
        boolean vertical = diry != 0;
        // A bend: slow for the corner, but nobody stops.
        if (j[4] == City.J_BEND) return Float.MAX_VALUE;
        // (10.42) Don't block the box: whatever the lights say, nobody pulls into the junction until there's room
        // on the far side to get out of it again (unless it's been waiting so long the queue must be going nowhere).
        // (10.53) Already over the line, its nose in the junction: it carries on across once nothing's crossing in
        // front. Stopping there for the red blocked the box, and the queue behind it sat for good, green or not.
        if (stopAt < -6 && j[4] != City.J_ROUNDABOUT && crossClear(id, v, dirx, diry)) {
            v.clearedJ = id;
            return Float.MAX_VALUE;
        }
        // ((27.5) For good, now: after twenty seconds it used to pull in anyway, stopped in the box, and locked the
        // cross traffic too - the knots in the middle of junctions. Sat there long enough, it finds another way
        // (unjam) instead.)
        if (!emergency && v.player == null && stopAt < 30 && exitBlocked(v, id)) {
            v.held = stopAt < 6;
            return approach;
        }
        if (emergency) {
            if (!crossClear(id, v, dirx, diry)) {
                v.held = stopAt < 6;
                return approach;
            }
            return Math.max(35, stopAt * 1.8f);
        }
        if (j[4] == City.J_LIGHTS && !v.fleeing) {
            city.lightDemand(id, vertical);
            int light = city.lightState(id, vertical, w.time);
            if (light == 0) {
                // Green, but something is still crossing in front: let it clear first.
                if (stopAt > 2 && !crossClear(id, v, dirx, diry)) {
                    v.held = stopAt < 6;
                    return approach;
                }
                return Float.MAX_VALUE;
            }
            // Amber: carry on only if it's too late to stop.
            if (light == 1 && v.speed > 40 && stopAt < v.speed * 0.2f) {
                v.clearedJ = id;
                return Float.MAX_VALUE;
            }
            v.held = stopAt < 6;
            return approach;
        }
        if (v.waitJ != id) {
            if (j[4] == City.J_ROUNDABOUT) {
                // ((10.53) Not for ever, though: after a few seconds at the line it edges on when its own way across
                // is clear. Something stalled on the far side of the roundabout had the way in shut for good.)
                if (stopAt < 10 && (junctionClear(id, v) || (v.stillT > 6 && crossClear(id, v, dirx, diry)))) {
                    v.clearedJ = id;
                    return Float.MAX_VALUE;
                }
                v.held = stopAt < 6;
                return Math.max(approach, stopAt < 10 ? 0 : 25);
            }
            if (stopAt < 4 && v.speed < 6) {
                v.waitJ = id;
                v.waitT = 0;
            }
            v.held = stopAt < 6;
            return Math.max(approach, 4);
        }
        v.waitT += dt;
        v.held = true;
        if (v.waitT > 1.2f && (junctionClear(id, v) || v.waitT > 7)) {
            v.clearedJ = id;
            v.waitJ = -1;
            return Float.MAX_VALUE;
        }
        return 0;
    }

    /**
     * (10.42) The line a vehicle is about to drive: from where it is, along the next steps of its route, each in
     * its lane (and through a junction in the lane it will leave by), as points in v.cor.
     */
    void buildCorridor(Vehicle v) {
        // (10.49) The same as last time while it's on the same tile of the same route: it doesn't change.
        if (v.field != null && v.corOf == v.field && v.corOfT == v.pathT && v.corOfDx == v.pdx && v.corOfDy == v.pdy) {
            System.arraycopy(v.corTs, 0, fleet.corTiles, 0, COR);
            fleet.corPi = v.corOfPi;
            return;
        }
        fleet.corPi = 0;
        buildCorridorNow(v);
        v.corOf = v.field;
        v.corOfT = v.pathT;
        v.corOfDx = v.pdx;
        v.corOfDy = v.pdy;
        v.corOfPi = fleet.corPi;
        System.arraycopy(fleet.corTiles, 0, v.corTs, 0, COR);
    }

    /**
     * (11.3) Getting in lane for a turn, as drivers do: the last few tiles before a junction where the route turns
     * right, over in the outside lane, and before a left turn the inside one (and round the corner into the
     * matching lane on the far side). Straight on, each car keeps its own lane, so all of a wide road is used.
     * Cars on a call keep to the inside lane throughout.
     */
    void turnLanes(Vehicle v, int[] ts, int m) {
        java.util.Arrays.fill(fleet.corLane, 0, m, -1);
        if (v.lightsOn()) return;
        int W = city.w, prevExit = -1;
        for (int i = 1; i < m; i++) {
            if (!city.junctionAt(ts[i] % W, ts[i] / W) || city.junctionAt(ts[i - 1] % W, ts[i - 1] / W)) continue;
            int ax = Integer.signum(ts[i] % W - ts[i - 1] % W), ay = Integer.signum(ts[i] / W - ts[i - 1] / W);
            int k = i;
            while (k < m - 1 && city.junctionAt(ts[k] % W, ts[k] / W)) k++;
            if (city.junctionAt(ts[k] % W, ts[k] / W)) break;
            int ex = Integer.signum(ts[k] % W - ts[k - 1] % W), ey = Integer.signum(ts[k] / W - ts[k - 1] / W);
            int cross = ax * ey - ay * ex;
            int lane = cross > 0 ? 2 : cross < 0 ? 0 : -1;
            // (y runs down the screen: turning clockwise is turning right.)
            if (lane >= 0)
                for (int j = Math.max(prevExit + 1, i - 6); j <= k - 1; j++) if (fleet.corLane[j] < 0) fleet.corLane[j] = lane;
            prevExit = k;
            i = k;
        }
    }

    void buildCorridorNow(Vehicle v) {
        v.corN = 0;
        if (v.field == null || v.pathT < 0) return;
        int W = city.w;
        // The tiles: one back along the way it came, then on down the route (up to a bridge).
        int[] ts = fleet.corTiles;
        int m = 0;
        if (v.pdx != 0 || v.pdy != 0) {
            int bx = v.pathT % W - v.pdx, by = v.pathT / W - v.pdy;
            if (bx >= 0 && by >= 0 && bx < W && by < city.h) ts[m++] = by * W + bx;
        }
        fleet.corPi = m;
        ts[m++] = v.pathT;
        for (int t = v.pathT; m < COR; ) {
            int nt = downhill(v.field, t);
            if (nt == t || Math.abs(nt % W - t % W) + Math.abs(nt / W - t / W) > 1) break;
            ts[m++] = nt;
            t = nt;
        }
        if (m < 2) return;
        turnLanes(v, ts, m);
        float lastAlong = Float.NaN;
        int lastAxisX = 0, lastAxisY = 0;
        boolean lastJ = false;
        fleet.jEntryL = Float.NaN;
        for (int i = 0; i < m; i++) {
            int t = ts[i], tx = t % W, ty = t / W;
            // Its heading here: over the tiles either side (a staircase of steps is one diagonal, not a zig-zag).
            int a = ts[Math.max(0, i - 1)], b = ts[Math.min(m - 1, i + 1)];
            float dx = b % W - a % W, dy = b / W - a / W, dl = (float) Math.sqrt(dx * dx + dy * dy);
            if (dl < 0.01f) {
                dx = v.pdx;
                dy = v.pdy;
                dl = Math.max(1, (float) Math.sqrt(dx * dx + dy * dy));
            }
            dx /= dl;
            dy /= dl;
            int sx = Math.abs(dx) >= Math.abs(dy) ? (int) Math.signum(dx) : 0, sy = sx == 0 ? (int) Math.signum(dy) : 0;
            float px, py;
            boolean inJ = city.junctionAt(tx, ty);
            if (inJ) {
                // (10.42) Through a junction: along the lane it came in by to where that crosses the lane it leaves
                // by (measured on the road beyond, not on the junction's own tiles), and out along that - the
                // corner is cut smoothly by steering for a point ahead on the line.
                int sdx = i + 1 < m ? Integer.signum(ts[i + 1] % W - tx) : lastAxisX, sdy = i + 1 < m ? Integer.signum(ts[i + 1] / W - ty) : lastAxisY;
                int ex = sdx, ey = sdy, k = i, first = i;
                while (first > 0 && city.junctionAt(ts[first - 1] % W, ts[first - 1] / W)) first--;
                while (k < m - 1 && city.junctionAt(ts[k] % W, ts[k] / W)) {
                    ex = Integer.signum(ts[k + 1] % W - ts[k] % W);
                    ey = Integer.signum(ts[k + 1] / W - ts[k] / W);
                    k++;
                }
                boolean out = !city.junctionAt(ts[k] % W, ts[k] / W) && Math.abs(ex) + Math.abs(ey) == 1;
                float le = 0;
                if (out) {
                    int E = ts[k];
                    float oe = city.laneOffset(E % W, E / W, ex, ey, fleet.corLane[first] >= 0 ? fleet.corLane[first] : laneOf(v));
                    le = ex != 0 ? (E / W + 0.5f) * City.T + ex * oe : (E % W + 0.5f) * City.T - ey * oe;
                }
                int ax = fleet.jEntryX, ay = fleet.jEntryY;
                if (i == first || Float.isNaN(fleet.jEntryL)) {
                    ax = lastAxisX;
                    ay = lastAxisY;
                    fleet.jEntryX = ax;
                    fleet.jEntryY = ay;
                    fleet.jEntryL = Float.isNaN(lastAlong) || (ax == 0 && ay == 0) ? Float.NaN : lastAlong;
                }
                float li = fleet.jEntryL, cx = (tx + 0.5f) * City.T, cy = (ty + 0.5f) * City.T;
                px = cx;
                py = cy;
                // (Only where the lines run over road the whole way: not across a roundabout's island.)
                if (i == first) {
                    fleet.jSmooth = false;
                    // (10.51) A roundabout: round the island the way the traffic goes (anticlockwise where they
                    // drive on the right), from where it came in to the lane it leaves by.
                    fleet.rbOn = false;
                    int jid = city.junctionIdAt(cx, cy);
                    int[] jb = jid >= 0 ? city.junctions.get(jid) : null;
                    if (jb != null && jb[4] == City.J_ROUNDABOUT && out && v.corN > 0) {
                        fleet.rbCx = (jb[0] + jb[2] + 1) * City.T / 2f;
                        fleet.rbCy = (jb[1] + jb[3] + 1) * City.T / 2f;
                        fleet.rbR = (Math.min(jb[2] - jb[0], jb[3] - jb[1]) + 1) * City.T * 0.31f;
                        int E = ts[k];
                        float x2 = ex != 0 ? (E % W + 0.5f) * City.T : le, y2 = ex != 0 ? le : (E / W + 0.5f) * City.T;
                        fleet.rbA0 = (float) Math.atan2(v.cor[v.corN * 2 - 1] - fleet.rbCy, v.cor[v.corN * 2 - 2] - fleet.rbCx);
                        float a1 = (float) Math.atan2(y2 - fleet.rbCy, x2 - fleet.rbCx), d = a1 - fleet.rbA0;
                        // ((11.5) Round the way the traffic goes, and no further: what's left of the way round once
                        // it's partway there (worked out again on each tile) is just that - the old rule sent anyone
                        // with less than a sixth of a turn to go right round again, over the island. Only a U-turn,
                        // whose way out is just behind its way in, goes all the way round.)
                        while (d > 0.25f) d -= (float) (Math.PI * 2);
                        while (d <= -Math.PI * 2 + 0.25f) d += (float) (Math.PI * 2);
                        fleet.rbD = d;
                        fleet.rbOn = true;
                    }
                    if (out && !Float.isNaN(li) && v.corN > 0) {
                        float x0 = v.cor[v.corN * 2 - 2], y0 = v.cor[v.corN * 2 - 1];
                        int E = ts[k];
                        float x2 = ex != 0 ? (E % W + 0.5f) * City.T : le, y2 = ex != 0 ? le : (E / W + 0.5f) * City.T;
                        float x1 = x2, y1 = y2;
                        if (ax * ex + ay * ey == 0) {
                            x1 = ax != 0 ? le : li;
                            y1 = ax != 0 ? li : le;
                        }
                        fleet.jSmooth = onRoute(v, x0, y0, x1, y1) && onRoute(v, x1, y1, x2, y2);
                    }
                }
                if (fleet.rbOn) {
                    float fr = (i - first + 1) / (float) (k - first + 1), ra = fleet.rbA0 + fleet.rbD * fr;
                    px = fleet.rbCx + (float) Math.cos(ra) * fleet.rbR;
                    py = fleet.rbCy + (float) Math.sin(ra) * fleet.rbR;
                } else if (!fleet.jSmooth) {
                    float o = city.laneOffset(tx, ty, sdx, sdy, laneOf(v));
                    px = cx - sdy * o;
                    py = cy + sdx * o;
                } else if (out && !Float.isNaN(li) && ax * ex + ay * ey == 0) {
                    // A turn: the corner where the two lanes cross.
                    float kx = ax != 0 ? le : li, ky = ax != 0 ? li : le;
                    // On the way in (up to the corner), or on the way out (from it): whichever is nearer.
                    float inX = ax != 0 ? cx : li, inY = ax != 0 ? li : cy;
                    if ((inX - kx) * ax + (inY - ky) * ay > 0) {
                        inX = kx;
                        inY = ky;
                    }
                    float outX = ex != 0 ? cx : le, outY = ex != 0 ? le : cy;
                    if ((outX - kx) * ex + (outY - ky) * ey < 0) {
                        outX = kx;
                        outY = ky;
                    }
                    boolean useIn = World.hypot(inX - cx, inY - cy) <= World.hypot(outX - cx, outY - cy);
                    px = useIn ? inX : outX;
                    py = useIn ? inY : outY;
                } else if (out && !Float.isNaN(li) && ax == ex && ay == ey) {
                    // Straight over: from the one lane line to the other, a little at a time.
                    // ((11.0) Where the street beyond doesn't line up, an easy S through the middle of the junction,
                    // in its own lane in and its own lane out, rather than angling across from the moment it's in.)
                    float fr = (i - first + 1) / (float) (k - first + 1);
                    if (Math.abs(le - li) > City.T * 0.6f) fr = fr * fr * (3 - 2 * fr);
                    float l = li + (le - li) * fr;
                    if (ax == 0) px = l;
                    else py = l;
                } else if (out && sdx == ex && sdy == ey) {
                    if (ex != 0) py = le;
                    else px = le;
                } else {
                    float o = city.laneOffset(tx, ty, sdx, sdy, laneOf(v));
                    px = cx - sdy * o;
                    py = cy + sdx * o;
                }
                sx = sdx;
                sy = sdy;
            } else {
                float off = city.laneOffset(tx, ty, sx, sy, fleet.corLane[i] >= 0 ? fleet.corLane[i] : laneOf(v));
                // (On a diagonal, or a curved street: no lanes to measure across it; near the middle.)
                // ((11.1) But on a curved street still on your own side of it, a lane's width from the one coming
                // the other way: down the middle, they met head on.)
                if (city.curveAt(tx, ty)) off = City.T * 0.27f;
                else if (Math.min(Math.abs(dx), Math.abs(dy)) > 0.3f) off = Math.max(-City.T * 0.4f, Math.min(City.T * 0.4f, off));
                px = (tx + 0.5f) * City.T - dy * off;
                py = (ty + 0.5f) * City.T + dx * off;
                // On along a straight bit of road: keep to the lane it's in (a route stepping from row to row of a
                // wide road is a gentle drift across, not a swerve every tile).
                // (Only where that keeps it on the route, though: not off the end of a slip road and on along the
                // highway it should have left.)
                if (i > 0 && sx == lastAxisX && sy == lastAxisY && !lastJ && !Float.isNaN(lastAlong)) {
                    // ((11.1) Quicker into a lane over on the far side of a wide road: at a third of a tile a step it
                    // was half a block on the wrong half of the road getting there.)
                    float maxShift = City.T * (Math.abs((sx == 0 ? px : py) - lastAlong) > City.T * 1.2f ? 0.7f : 0.3f), qx = px, qy = py;
                    if (sx == 0) qx = lastAlong + Math.max(-maxShift, Math.min(maxShift, px - lastAlong));
                    else qy = lastAlong + Math.max(-maxShift, Math.min(maxShift, py - lastAlong));
                    int q = city.tileIndex(qx, qy), fq = v.field.get(q);
                    if (q == t || (fq < City.FAR && Math.abs(fq - v.field.get(t)) <= 6)) {
                        px = qx;
                        py = qy;
                    }
                }
            }
            lastJ = inJ;
            lastAxisX = sx;
            lastAxisY = sy;
            lastAlong = sx == 0 ? px : py;
            v.cor[v.corN * 2] = px;
            v.cor[v.corN * 2 + 1] = py;
            v.corN++;
        }
    }

    /** Every tile under the line from one point to another is on the vehicle's route. */
    boolean onRoute(Vehicle v, float x0, float y0, float x1, float y1) {
        float d = (float) World.hypot(x1 - x0, y1 - y0);
        int n = Math.max(1, (int) (d / 4));
        for (int s = 0; s <= n; s++) {
            float f = s / (float) n;
            if (v.field.get(city.tileIndex(x0 + (x1 - x0) * f, y0 + (y1 - y0) * f)) >= City.FAR) return false;
        }
        return true;
    }

    /** The point dist along a vehicle's planned line on from the nearest point of it to the vehicle. */
    float[] pursuitPoint(Vehicle v, float dist) {
        // Where it is on the line: the nearest point of the first few legs.
        int seg0 = 1;
        float best = Float.MAX_VALUE, f0 = 0;
        for (int i = 1; i < Math.min(v.corN, 5); i++) {
            float ax = v.cor[i * 2 - 2], ay = v.cor[i * 2 - 1], sx = v.cor[i * 2] - ax, sy = v.cor[i * 2 + 1] - ay;
            float l2 = sx * sx + sy * sy;
            float f = l2 < 0.001f ? 0 : Math.max(0, Math.min(1, ((v.x - ax) * sx + (v.y - ay) * sy) / l2));
            float d = (float) World.hypot(ax + sx * f - v.x, ay + sy * f - v.y);
            if (d < best) {
                best = d;
                seg0 = i;
                f0 = f;
            }
        }
        if (best > 40) return null;
        float left = dist + f0 * (float) World.hypot(v.cor[seg0 * 2] - v.cor[seg0 * 2 - 2], v.cor[seg0 * 2 + 1] - v.cor[seg0 * 2 - 1]);
        for (int i = seg0; i < v.corN; i++) {
            float ax = v.cor[i * 2 - 2], ay = v.cor[i * 2 - 1], bx = v.cor[i * 2], by = v.cor[i * 2 + 1];
            float seg = (float) World.hypot(bx - ax, by - ay);
            if (seg >= left && seg > 0.001f) {
                float f = left / seg;
                return point(ax + (bx - ax) * f, ay + (by - ay) * f);
            }
            left -= seg;
        }
        return v.corN >= 2 ? point(v.cor[v.corN * 2 - 2], v.cor[v.corN * 2 - 1]) : null;
    }

    float[] point(float x, float y) {
        fleet.pursuit[0] = x;
        fleet.pursuit[1] = y;
        return fleet.pursuit;
    }

    /**
     * How far along a vehicle's planned line another one is, if it's on that line (within a lane's width of
     * it); -1 if it isn't in the way.
     */
    float alongCorridor(Vehicle v, Vehicle o, float width) {
        // (From level with the vehicle: the first leg runs from a little behind it.)
        float run = -(float) (((v.x - v.cor[0]) * Math.cos(v.angle)) + ((v.y - v.cor[1]) * Math.sin(v.angle)));
        for (int i = 1; i < v.corN; i++) {
            float ax = v.cor[i * 2 - 2], ay = v.cor[i * 2 - 1], bx = v.cor[i * 2], by = v.cor[i * 2 + 1];
            float sx = bx - ax, sy = by - ay, l2 = sx * sx + sy * sy;
            if (l2 < 0.001f) continue;
            float f = ((o.x - ax) * sx + (o.y - ay) * sy) / l2;
            float seg = (float) Math.sqrt(l2);
            if (f >= 0 && f <= 1) {
                float px = ax + sx * f, py = ay + sy * f;
                if (World.hypot(o.x - px, o.y - py) < width) {
                    fleet.hitDx = sx / seg;
                    fleet.hitDy = sy / seg;
                    return run + f * seg;
                }
            }
            run += seg;
        }
        return -1;
    }

    /** The next tile down a route from tile t (t itself at the end). */
    int downhill(Route field, int t) {
        int at = field.find(t);
        if (at >= 0) {
            if (field.next == null) field.next = new byte[field.keys.length];
            int W = city.w, n = field.next[at];
            if (n == 0) {
                int bt = downhillOn(field, t);
                field.next[at] = (byte) (bt == t ? 1 : bt == t + 1 ? 2 : bt == t - 1 ? 3 : bt == t + W ? 4 : bt == t - W ? 5 : 6);
                return bt;
            }
            if (n == 1) return t;
            if (n == 2) return t + 1;
            if (n == 3) return t - 1;
            if (n == 4) return t + W;
            if (n == 5) return t - W;
            int link = city.bridgeTo(t);
            return link >= 0 ? link : t;
        }
        return downhillOn(field, t);
    }

    int downhillOn(Route field, int t) {
        int W = city.w, tx = t % W, ty = t / W, here = field.get(t), best = Integer.MAX_VALUE, bt = t;
        boolean inJ = city.junctionAt(tx, ty);
        for (int k = 0; k < 4; k++) {
            int nx = tx + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = ty + (k == 2 ? 1 : k == 3 ? -1 : 0);
            if (nx < 0 || ny < 0 || nx >= W || ny >= city.h) continue;
            int d = field.get(ny * W + nx);
            if (d >= here) continue;
            // Downhill, but on a tie keep to the lane: a step across the road outside a junction only if
            // it's really the shorter way.
            int rd = city.roadDirAt(nx, ny);
            if (!inJ && !city.junctionAt(nx, ny) && ((rd == 1 && k < 2) || (rd == 2 && k >= 2))) d += 14;
            if (d < best) {
                best = d;
                bt = ny * W + nx;
            }
        }
        // (10.32) Or over a bridge.
        int link = city.bridgeTo(t);
        if (link >= 0 && field.get(link) < here && field.get(link) < best) bt = link;
        return bt;
    }

    int downhillRaw(int[] field, int t) {
        int W = city.w, tx = t % W, ty = t / W, here = field[t], best = Integer.MAX_VALUE, bt = t;
        boolean inJ = city.junctionAt(tx, ty);
        for (int k = 0; k < 4; k++) {
            int nx = tx + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = ty + (k == 2 ? 1 : k == 3 ? -1 : 0);
            if (nx < 0 || ny < 0 || nx >= W || ny >= city.h) continue;
            int d = field[ny * W + nx];
            if (d >= here) continue;
            // Downhill, but on a tie keep to the lane: a step across the road outside a junction only if
            // it's really the shorter way.
            int rd = city.roadDirAt(nx, ny);
            if (!inJ && !city.junctionAt(nx, ny) && ((rd == 1 && k < 2) || (rd == 2 && k >= 2))) d += 14;
            if (d < best) {
                best = d;
                bt = ny * W + nx;
            }
        }
        // (10.32) Or over a bridge.
        int link = city.bridgeTo(t);
        if (link >= 0 && field[link] < here && field[link] < best) bt = link;
        return bt;
    }

    /** The way out of junction id along a vehicle's route has a car stopped in it. */
    boolean exitBlocked(Vehicle v, int id) {
        int W = city.w, t = v.pathT;
        // ((12.0) Where it comes out of this junction is worked out once from where it is on its route, not again
        // every moment it waits: fourteen steps down its route, for every car at every junction, every frame.)
        if (v.exitJ != id || v.exitFrom != t || v.exitRoute != v.field) {
            v.exitJ = id;
            v.exitFrom = t;
            v.exitRoute = v.field;
            v.exitTile = -1;
            boolean through = false;
            for (int k = 0; k < 14; k++) {
                boolean inJ = city.junctionIdAt((t % W + 0.5f) * City.T, (t / W + 0.5f) * City.T) == id;
                if (inJ) through = true;
                else if (through) {
                    v.exitTile = t;
                    break;
                }
                int n = downhill(v.field, t);
                if (n == t) break;
                t = n;
            }
        }
        if (v.exitTile < 0) return false;
        t = v.exitTile;
        // ((27.5) Which way out: only something stopped on its own side of the road there blocks it - one waiting on
        // the far side to come the other way into the junction was taken for a blocked exit, and the two sides held
        // each other at the line. A wreck in its lane does block it (wrecks count as parked, and were left out).)
        int nt = downhill(v.field, t);
        int odx = nt % W - t % W, ody = nt / W - t / W;
        if (Math.abs(odx) + Math.abs(ody) != 1) odx = ody = 0;
        {
            {
                float ex = (t % W + 0.5f) * City.T, ey = (t / W + 0.5f) * City.T;
                // ((11.5) Only those in the grid squares round the spot, not every vehicle on the map.)
                int c0x = Math.max(0, (int) ((ex - City.T) / GC)), c1x = Math.min(fleet.gridW - 1, (int) ((ex + City.T) / GC));
                int c0y = Math.max(0, (int) ((ey - City.T) / GC)), c1y = Math.min(fleet.gridH - 1, (int) ((ey + City.T) / GC));
                for (int cy = c0y; cy <= c1y; cy++)
                    for (int cx = c0x; cx <= c1x; cx++)
                        for (int i = fleet.gridStart[cy * fleet.gridW + cx], ie = fleet.gridStart[cy * fleet.gridW + cx + 1]; i < ie; i++) {
                            Vehicle o = fleet.gridV[i];
                            if (o == v || o.removedFromFleet || (o.parked && !o.broken) || offRoad(o) || Math.abs(o.speed) > 4) continue;
                            if (Math.abs(o.x - ex) < City.T * 0.9f && Math.abs(o.y - ey) < City.T * 0.9f
                                    && city.junctionIdAt(o.x, o.y) < 0
                                    && (odx == 0 && ody == 0 || (o.x - ex) * -ody + (o.y - ey) * odx > -3)) return true;
                        }
                return false;
            }
        }
    }

    /** Nobody else driving in the junction. */
    /** Nothing moving across the junction in front of a vehicle coming in along (dx, dy). */
    boolean crossClear(int id, Vehicle self, int dx, int dy) {
        int[] j = city.junctions.get(id);
        int c0x = Math.max(0, (j[0] - 1) * City.T / GC), c1x = Math.min(fleet.gridW - 1, (j[2] + 2) * City.T / GC);
        int c0y = Math.max(0, (j[1] - 1) * City.T / GC), c1y = Math.min(fleet.gridH - 1, (j[3] + 2) * City.T / GC);
        for (int cy = c0y; cy <= c1y; cy++)
            for (int cx = c0x; cx <= c1x; cx++)
                for (int i = fleet.gridStart[cy * fleet.gridW + cx], ie = fleet.gridStart[cy * fleet.gridW + cx + 1]; i < ie; i++) {
                    Vehicle o = fleet.gridV[i];
                    if (o == self || o.removedFromFleet || offRoad(o) || o.parked || Math.abs(o.speed) < 5) continue;
                    if (city.junctionIdAt(o.x, o.y) != id) continue;
                    if (Math.abs((float) Math.cos(o.angle) * dx + (float) Math.sin(o.angle) * dy) < 0.6f) return false;
                }
        return true;
    }

    boolean junctionClear(int id, Vehicle self) {
        // ((11.5) Only the grid squares over the junction's box.)
        int[] j = city.junctions.get(id);
        int c0x = Math.max(0, (j[0] - 1) * City.T / GC), c1x = Math.min(fleet.gridW - 1, (j[2] + 2) * City.T / GC);
        int c0y = Math.max(0, (j[1] - 1) * City.T / GC), c1y = Math.min(fleet.gridH - 1, (j[3] + 2) * City.T / GC);
        for (int cy = c0y; cy <= c1y; cy++)
            for (int cx = c0x; cx <= c1x; cx++)
                for (int i = fleet.gridStart[cy * fleet.gridW + cx], ie = fleet.gridStart[cy * fleet.gridW + cx + 1]; i < ie; i++) {
                    Vehicle o = fleet.gridV[i];
                    if (o == self || o.removedFromFleet || offRoad(o) || o.parked) continue;
                    if (city.junctionIdAt(o.x, o.y) == id) return false;
                }
        return true;
    }

    float followLimit(Vehicle v) {
        float fx = (float) Math.cos(v.angle), fy = (float) Math.sin(v.angle);
        float look = 30 + Math.abs(v.speed) * 1.6f, limit = Float.MAX_VALUE;
        v.queued = false;
        int depth = 0;
        // ((12.0) On a roundabout the traffic already going round has the right of way: nobody on it stops for one
        // waiting at the entry to come on - they were each waiting on the other, and the roundabout locked solid.)
        int ring = city.junctionIdAt(v.x, v.y);
        if (ring >= 0 && city.junctions.get(ring)[4] != City.J_ROUNDABOUT) ring = -1;
        int c0x = Math.max(0, (int) ((v.x - look) / GC)), c1x = Math.min(fleet.gridW - 1, (int) ((v.x + look) / GC));
        int c0y = Math.max(0, (int) ((v.y - look) / GC)), c1y = Math.min(fleet.gridH - 1, (int) ((v.y + look) / GC));
        for (int cy = c0y; cy <= c1y; cy++)
        for (int cx = c0x; cx <= c1x; cx++)
        for (int i = fleet.gridStart[cy * fleet.gridW + cx], ie = fleet.gridStart[cy * fleet.gridW + cx + 1]; i < ie; i++) {
            Vehicle o = fleet.gridV[i];
            if (o == v || o.removedFromFleet || levels(v, o)) continue;
            if (ring >= 0 && Math.abs(o.speed) < 3 && !o.parked && !o.broken && city.junctionIdAt(o.x, o.y) != ring) continue;
            float dx = o.x - v.x, dy = o.y - v.y;
            float ahead = dx * fx + dy * fy;
            float side = Math.abs(dx * -fy + dy * fx);
            // ((27.5) Pulled in to the kerb and stopped there (at a scene, waiting for its officers): beside the lane,
            // not in it - the traffic goes on by.)
            if (o.kerbed && !o.pulling && Math.abs(o.speed) < 1 && side > 7 && v.player == null) continue;
            // (10.42) Two that have ended up on top of one another, each waiting for the other to move: one of
            // them (whichever, so long as it's always the same one) drives out from under.
            if (dx * dx + dy * dy < 64 && v.stillT > 3 && o.stillT > 3 && v.x * 7 + v.y < o.x * 7 + o.y) continue;
            // (Likewise two stuck facing each other's sides in the middle of a junction, each queued behind the
            // other: the knot only comes undone if one of them goes first.)
            if (v.stillT > 3 && o.stillT > 3 && o.queued && v.player == null && city.junctionIdAt(v.x, v.y) >= 0
                    && city.junctionIdAt(v.x, v.y) == city.junctionIdAt(o.x, o.y) && v.x * 7 + v.y < o.x * 7 + o.y) continue;
            // (10.42) In the way only if it's on the line this one is about to drive (round the bend or across
            // the junction too), not just somewhere in front of its bonnet: a car beside it in a junction,
            // turning across behind it or waiting in the next lane isn't in its way. (Right up against the front
            // of it, anything is.)
            // ((12.0) Stopped and waiting on one stopped behind it: it's the front of the queue, and nothing is in its
            // way. (A route turning back on itself ran its line back through the queue, and the front car waited on
            // the cars waiting on it.))
            if (ahead < -2 && v.stillT > 3 && o.stillT > 1 && v.player == null) continue;
            if (v.corN >= 3 && v.player == null) {
                float along = alongCorridor(v, o, 9 + o.length() * 0.15f);
                boolean nose = ahead > 0 && ahead < (v.length() + o.length()) * 0.5f + 8 && side < 8;
                if (along < 0 && !nose) continue;
                if (along >= 0) ahead = along;
                if (ahead <= 0 || ahead > look) continue;
                // (Crossing its line further on, through a junction: the junction's rules sort out who goes
                // first. Only one right across its bonnet is something to stop for here - and not to queue behind.)
                // (Stopped across it, it's in the way like anything else.)
                if (along >= 0 && !nose && o.player == null && Math.abs(o.speed) >= 3
                        && Math.abs((float) Math.cos(o.angle) * fleet.hitDx + (float) Math.sin(o.angle) * fleet.hitDy) < 0.5f) {
                    float g = ahead - (v.length() + o.length()) * 0.5f;
                    if (g > 12) continue;
                    limit = Math.min(limit, Math.max(0, g - 4) * 2);
                    continue;
                }
            } else {
                if (ahead <= 0 || ahead > look) continue;
                if (side > 9) continue;
            }
            // How fast it's going our way (oncoming or crossing counts as standing still).
            float lead = Math.max(0, o.speed * ((float) Math.cos(o.angle) * fx + (float) Math.sin(o.angle) * fy));
            float gap = ahead - (v.length() + o.length()) * 0.5f - 5;
            // (10.32) In the panic, now and then someone runs right into the car in front.
            if (gap < -3 && w.alert >= 2 && v.type == CAR && o.type == CAR && v.player == null && v.speed > 30 && !o.broken
                    && w.rnd.nextFloat() < 0.04f) {
                fleet.damage(v, v.maxHp * 0.7f, false);
                fleet.damage(o, o.maxHp * 0.5f, false);
                w.emit(Sfx.CRASH, v.x, v.y);
                w.noise(v.x, v.y, 160);
            }
            // Something stopped for good in the lane (parked, wrecked, or stood there a while and not just
            // queueing): pull out and go round it, slowly, rather than wait behind it for ever.
            boolean stuckThere = o.parked || o.broken || (o.stillT > 5 && !o.held && !o.queued)
                    // ((12.0) Nose to nose with one coming the other way in the same lane, both stopped: each waited
                    // for the other for good. Both go round, each to its own side, and they pass.)
                    || (gap < 16 && Math.abs(v.speed) < 3 && Math.abs(o.speed) < 3 && v.stillT > 1.5f && o.stillT > 1.5f && o.player == null
                        && (float) Math.cos(v.angle - o.angle) < -0.7f)
                    // ((12.0) A police car, army truck or ambulance stopped at a scene, loading, or waiting for its
                    // crew (not a roadblock: that's there to stop them): it isn't going anywhere. Its "queued" was left over from before it stopped, so the
                    // traffic behind it took it for a queue and sat there with it for good.)
                    || ((o.type == CRUISER || o.type == TRUCK || o.type == AMBULANCE) && o.player == null && o.stillT > 3
                        && (o.state == SCENE || o.state == RECALL || o.state == LOAD
                            // ((27.4) Nor one waiting at the kerb for its officers to come and get in: that can be a
                            // minute and a half, and the queue behind took it for a red light and waited it out.)
                            || o.state == PICKUP))
                    || (o.yieldT > 0 && !o.yieldStop && v.lightsOn())
                    // (Lights and sirens go round a queue sitting at a red light, too.)
                    || (v.lightsOn() && o.type == CAR && o.player == null && o.stillT > 1f)
                    // (10.46) An army truck on a call has no lights, but doesn't sit behind a stopped car either.
                    || (v.type == TRUCK && v.state == DRIVE && v.incident != null && o.player == null && o.stillT > 2f)
                    // (Gridlock, everyone waiting on everyone and nobody on a light: edge round and break it.)
                    || (o.stillT > 6 && v.stillT > 6 && !o.held && !o.queuedHeld)
                    // (10.42) Or a knot where everyone's waiting on everyone else at once: longer than any red
                    // light lasts and it isn't a queue, it's stuck.
                    || (o.stillT > 25 && v.stillT > 10 && o.type == CAR && o.player == null)
                    // ((11.1) Police and army on a call don't sit behind another of their own stopped at a scene, a
                    // roadblock or a pickup either - only one waiting at a red light.)
                    || ((v.lightsOn() || (v.type == TRUCK && v.state == DRIVE && v.incident != null)) && o.type != CAR && o.player == null
                        && !o.held && !o.queuedHeld && o.stillT > (o.state == DRIVE ? 4f : 1.5f));
            // ((27.5) Round it only where that's safe: see canPass. Otherwise it waits behind it like a queue - and the
            // way round a wreck it can't pass is another route (unjam), not up on the pavement.)
            if (stuckThere && v.player == null && v.type != TANK && canPass(v, o, gap)) {
                if (gap < 40) v.passing = PASS_T;
                limit = Math.min(limit, 30 + Math.max(0, gap));
                continue;
            }
            // ((16.9) Overtaking: the army on a job, or anything on lights and sirens, doesn't trail behind slower
            // traffic going the same way - a tank, an APC, a car that won't pull over. Out and past it, a little
            // quicker than it's going, then back in. (Not in a junction, nor past one sat at a light.) Military
            // convoys used to crawl along behind their slowest.)
            boolean urgent = v.player == null && v.type != TANK && !v.parked
                    && (v.lightsOn() || (army(v) && (v.state == DRIVE || v.state == RETURN) && v.incident != null));
            if (urgent && lead > 2 && gap < 45 && !o.held && !o.queued && !o.parked && o.player == null
                    && lead < 60 && city.junctionIdAt(v.x, v.y) < 0 && city.junctionIdAt(o.x, o.y) < 0 && canPass(v, o, gap)) {
                v.passing = PASS_T;
                limit = Math.min(limit, Math.max(lead + 30, 45));
                continue;
            }
            if (gap < 25 && lead < 5) {
                v.queued = true;
                int d = o.held ? 1 : o.holdDepth > 0 ? o.holdDepth + 1 : 0;
                if (d > 0 && (depth == 0 || d < depth)) depth = d;
            }
            float ok = gap <= 0 ? 0 : Math.min(lead + gap * 1.2f, (float) Math.sqrt(2 * 260 * gap));
            if (gap < 10) ok = Math.min(ok, lead * 0.8f);
            limit = Math.min(limit, ok);
        }
        v.holdDepth = depth;
        v.queuedHeld = depth > 0 && depth <= 15;
        return limit;
    }

    boolean carAhead(Vehicle v) {
        float fx = (float) Math.cos(v.angle), fy = (float) Math.sin(v.angle);
        int c0x = Math.max(0, (int) ((v.x - 24) / GC)), c1x = Math.min(fleet.gridW - 1, (int) ((v.x + 24) / GC));
        int c0y = Math.max(0, (int) ((v.y - 24) / GC)), c1y = Math.min(fleet.gridH - 1, (int) ((v.y + 24) / GC));
        for (int cy = c0y; cy <= c1y; cy++)
        for (int cx = c0x; cx <= c1x; cx++)
        for (int i = fleet.gridStart[cy * fleet.gridW + cx], ie = fleet.gridStart[cy * fleet.gridW + cx + 1]; i < ie; i++) {
            Vehicle o = fleet.gridV[i];
            // ((12.0) A queue is traffic: something parked, wrecked or left in the lane isn't one to sit behind. It
            // kept the car's stuck timer down, so it never went round or found another way - it crept along behind
            // the wreck for good.)
            if (o == v || o.removedFromFleet || o.parked || o.broken || o.standby || o.state == ABANDONED) continue;
            float dx = o.x - v.x, dy = o.y - v.y;
            float ahead = dx * fx + dy * fy, side = Math.abs(dx * -fy + dy * fx);
            if (ahead > 0 && ahead < 24 && side < 8) return true;
        }
        return false;
    }

    /**
     * Where a stopped car ends up: the kerb to its right (across all the lanes its way, on a wide road - never
     * left in the middle of one), or failing that a near one to its left.
     */
    void pullOver(Vehicle v) {
        if (v.type != CAR) return;
        v.pullTried = true;
        float nx = (float) -Math.sin(v.angle), ny = (float) Math.cos(v.angle);
        for (int side = 1; side >= -1; side -= 2)
            for (float s = 4; s <= (side > 0 ? City.T * 4.5f : 26); s += 3) {
                float x = v.x + nx * s * side, y = v.y + ny * s * side;
                int tx = (int) (x / City.T), ty = (int) (y / City.T);
                if (tx < 0 || ty < 0 || tx >= city.w || ty >= city.h) break;
                byte t = city.tiles[ty * city.w + tx];
                if (t == City.ROAD || t == City.CAR) continue;
                v.pullX = x - nx * side * 6.5f;
                v.pullY = y - ny * side * 6.5f;
                v.pulling = true;
                return;
            }
    }

    /**
     * (10.32) The army's checkpoints on the road to the country base: anyone else pulls up at the line and waits
     * to be waved through (and once things are bad, civilians are turned back). The army's own vehicles, and
     * anything on lights and sirens, go straight through.
     */
    float checkpointLimit(Vehicle v, float dt) {
        if (v.player != null || v.type == TRUCK || v.type == TANK || v.lightsOn() || fleet.routing.emergency(v)) return Float.MAX_VALUE;
        float fx = (float) Math.cos(v.angle), fy = (float) Math.sin(v.angle);
        for (int k = 0; k < city.checkpoints.size(); k++) {
            if (v.cleared == k) continue;
            float[] cp = city.checkpoints.get(k);
            float rx = (float) Math.cos(cp[2]), ry = (float) Math.sin(cp[2]);
            if (Math.abs(fx * rx + fy * ry) < 0.6f) continue;
            float dx = cp[0] - v.x, dy = cp[1] - v.y;
            float ahead = dx * fx + dy * fy, side = Math.abs(dx * -fy + dy * fx);
            if (ahead < 0 || ahead > 140 || side > 26) continue;
            float gap = ahead - 40 - v.length() * 0.5f;
            if (gap > 3) return Math.max(6, (float) Math.sqrt(2 * 220 * gap));
            v.cpWait += dt;
            if (v.cpWait > 2.5f) {
                v.cpWait = 0;
                v.cleared = k;
                // (Once it's on the news, the army turns civilians back.)
                if (w.alert >= 2 && v.type == CAR && !v.fleeing) {
                    v.field = null;
                    v.destCd = 0;
                    fleet.routing.newDestination(v);
                }
            }
            return 0;
        }
        return Float.MAX_VALUE;
    }

    /** (10.32) On a bridge over the highway and crossing it: up on the deck, above the traffic underneath. */
    boolean onDeck(Vehicle v) {
        if (city.overpassAt(v.x, v.y) < 0) return false;
        float along = city.hwyAxis == 0 ? Math.abs((float) Math.cos(v.angle)) : Math.abs((float) Math.sin(v.angle));
        return along < 0.6f;
    }

    /** One on a bridge and the other on the highway under it: they pass without touching. */
    boolean levels(Vehicle a, Vehicle b) {
        if (city.overpasses.isEmpty()) return false;
        // ((12.0) Each car's level worked out once where it is, not again for every car near it.)
        int da = deck(a), db = deck(b);
        if (da < 0 && db < 0) return false;
        return (da == 1) != (db == 1);
    }

    int deck(Vehicle v) {
        if (v.deck == -2 || v.deckX != v.x || v.deckY != v.y || v.deckA != v.angle) {
            v.deckX = v.x;
            v.deckY = v.y;
            v.deckA = v.angle;
            v.deck = city.overpassAt(v.x, v.y) < 0 ? -1 : onDeck(v) ? 1 : 0;
        }
        return v.deck;
    }

    boolean onBridge(Vehicle v) {
        return !city.overpasses.isEmpty() && onDeck(v);
    }

    /** Running zombies over dents the car. */
    void hit(Vehicle v, float impact) {
        if (impact > 0) fleet.damage(v, impact, false);
    }

    /** ((27.4) The tiles a route search may not step onto, for unjam: the blocker's own and the one past it. */
    private final int[] jamTiles = new int[2];
    /** ((27.4) Times so far a vehicle found a way round the dead ahead (steerClear), and round a jam or a wreck (unjam). */
    int steered, unjammed;

    /**
     * ((27.4) The dead on the road just ahead - three or more within the next hundred-odd feet - and that isn't where
     * it's going: it plans its way again, round them (the search keeps clear of crowds of the dead: see City.hazard).
     * Not armour, which drives through them; not a patrol out to shoot them, a convoy (which halts and fights), a
     * command post truck (which has its own: Ops.postBlocked), an armed pickup, or anything going to that very fight
     * - the call's own rules stop it short of them.) True if it has a new way.
     */
    boolean steerClear(Vehicle v) {
        if (w.zombies == 0 || fleet.routing.hazardMul(v) < 100 || v.loop || v.convoy != null || isPost(v) || v.recon
                || v.kind == K_PICKUP || v.type == TRAIN || airborne(v)) return false;
        if (!w.zombieWithin(v.x, v.y, 190)) return false;
        float fx = (float) Math.cos(v.angle), fy = (float) Math.sin(v.angle);
        int n = w.countZombiesNear(v.x + fx * 60, v.y + fy * 60, 45) + w.countZombiesNear(v.x + fx * 135, v.y + fy * 135, 50);
        if (n < 3) return false;
        float hx = v.x + fx * 95, hy = v.y + fy * 95;
        if (World.hypot(v.tx - hx, v.ty - hy) < 240) return false;
        v.rerouteCd = 4;
        if (!fleet.routing.route(v, v.tx, v.ty)) return false;
        steered++;
        // (Hemmed in, hardly moving, and the new way is back the way it came: back out of them.)
        if (Math.abs(v.speed) < 6) backOutIfBehind(v);
        return true;
    }

    /** ((27.4) If v's way now starts behind it and there's room there, it backs out first (see steerAndMove). */
    void backOutIfBehind(Vehicle v) {
        float fx = (float) Math.cos(v.angle), fy = (float) Math.sin(v.angle);
        int own = city.tileIndex(v.x, v.y), next = downhill(v.field, own);
        float nx = (next % city.w + 0.5f) * City.T - v.x, ny = (next / city.w + 0.5f) * City.T - v.y;
        if (next != own && nx * fx + ny * fy < 0 && clearBehind(v, 20)) v.backT = 2.5f;
    }

    /**
     * ((27.4) Sat in a queue that isn't moving (not one waiting at a light) for twelve seconds, or stopped at all for
     * twenty-five: whatever's in front - a wreck, a knot of traffic, a junction locked solid - is taken as a wall,
     * and it looks for another way round. With none, it keeps its place (and the gridlock breaker still has it in
     * the end). True if it has a new way.
     */
    boolean unjam(Vehicle v) {
        if (v.stillT < 5 || v.parked || v.convoy != null || isPost(v) || v.type == TRAIN || airborne(v)) return false;
        if (!((v.queued && !v.queuedHeld && !v.held) || v.stillT > 25)) return false;
        float fx = (float) Math.cos(v.angle), fy = (float) Math.sin(v.angle);
        Vehicle block = null;
        float bd = 45;
        for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
            Vehicle o = fleet.vehicles.get(i);
            if (o == v || o.removedFromFleet || airborne(o) || Math.abs(o.speed) > 5) continue;
            float dx = o.x - v.x, dy = o.y - v.y, ahead = dx * fx + dy * fy, side = Math.abs(dx * -fy + dy * fx);
            if (ahead > 4 && ahead < bd && side < 10) {
                bd = ahead;
                block = o;
            }
        }
        // ((27.5) Twelve seconds before it gives up on a queue - but only five behind something that isn't going to move
        // at all (parked, a wreck, a car left at a scene) when it can't get round it on the road: there's no waiting
        // that out, and with passing kept off the pavement, finding another way is the only way.)
        boolean fixed = block != null && (block.parked || block.broken || block.state == ABANDONED
                || ((block.state == SCENE || block.state == RECALL || block.state == LOAD || block.state == PICKUP) && block.player == null));
        if (v.stillT < (fixed && v.passing <= 0 ? 5 : 12) && v.stillT <= 25) return false;
        // (The wall: where the blocker is, and just past it - never right on top of this one, or it can't leave.)
        float at = Math.max(block != null ? bd + 6 : 24, 36);
        jamTiles[0] = city.tileIndex(v.x + fx * at, v.y + fy * at);
        jamTiles[1] = city.tileIndex(v.x + fx * (at + City.T), v.y + fy * (at + City.T));
        v.rerouteCd = 10;
        if (!fleet.routing.routeAround(v, jamTiles, 2)) return false;
        unjammed++;
        // The new way starts back the way it came, and the wall's in front: back out (if there's room behind) so the
        // nose can come round - standing still, the wheels turn nothing.
        backOutIfBehind(v);
        return true;
    }

    /** ((27.5) Knots undone, and how many times one backed off to do it. */
    int gaveWay;

    /**
     * ((27.5) A give-way deadlock: this one is stopped behind another that's stopped behind another ... that's stopped
     * behind this one - nose to nose, or three or four across each other's bows, each waiting for the next. Nobody's
     * at a light and nobody will ever move. Of the ring, the one lowest in a fixed order (by where they stand) that
     * has room behind it backs straight off a length, and the one it was blocking has room to get by. No car drives
     * through another.
     */
    void giveWay(Vehicle v) {
        if (v.stillT < 6 || v.held || v.queuedHeld || v.giveWayT > 0 || v.backT > 0 || v.player != null || v.parked) return;
        Vehicle cur = blockerOf(v);
        int n = 0;
        for (int k = 0; k < 6 && cur != null && cur != v; k++) {
            if (cur.held || cur.queuedHeld || cur.parked || cur.broken || cur.player != null || cur.stillT < 3) return;
            ring[n++] = cur;
            cur = blockerOf(cur);
        }
        if (cur != v || !clearBehind(v, 20)) return;
        // (Another in the ring, ranked lower, with room to back off itself: it goes, not this one.)
        for (int k = 0; k < n; k++)
            if (rank(ring[k]) < rank(v) && clearBehind(ring[k], 20)) return;
        v.giveWayT = 1.5f;
        gaveWay++;
    }

    private final Vehicle[] ring = new Vehicle[6];

    /** ((27.5) A fixed order between any two vehicles, by where they stand. */
    static float rank(Vehicle v) {
        return v.x * 7 + v.y;
    }

    /** ((27.5) The nearest vehicle stopped right in front of v (in its way), or null. */
    Vehicle blockerOf(Vehicle v) {
        float fx = (float) Math.cos(v.angle), fy = (float) Math.sin(v.angle);
        Vehicle best = null;
        float bd = Float.MAX_VALUE;
        int c0x = Math.max(0, (int) ((v.x - 40) / GC)), c1x = Math.min(fleet.gridW - 1, (int) ((v.x + 40) / GC));
        int c0y = Math.max(0, (int) ((v.y - 40) / GC)), c1y = Math.min(fleet.gridH - 1, (int) ((v.y + 40) / GC));
        for (int cy = c0y; cy <= c1y; cy++)
            for (int cx = c0x; cx <= c1x; cx++)
                for (int i = fleet.gridStart[cy * fleet.gridW + cx], ie = fleet.gridStart[cy * fleet.gridW + cx + 1]; i < ie; i++) {
                    Vehicle o = fleet.gridV[i];
                    if (o == v || o.removedFromFleet || levels(v, o) || Math.abs(o.speed) > 3) continue;
                    float dx = o.x - v.x, dy = o.y - v.y, ahead = dx * fx + dy * fy, side = Math.abs(dx * -fy + dy * fx);
                    float reach = (v.length() + o.length()) * 0.5f + 14;
                    // (On the line it's about to drive, round the bend too - the way followLimit sees what's in its way.)
                    if (v.corN >= 3 && !(ahead > 0 && side < 8)) {
                        float along = alongCorridor(v, o, 9 + o.length() * 0.15f);
                        if (along < 0) continue;
                        ahead = along;
                        side = 0;
                    }
                    if (ahead > 0 && ahead < reach && side < 11 && ahead < bd) {
                        bd = ahead;
                        best = o;
                    }
                }
        return best;
    }

    /** ((27.4) Nothing (that isn't in the air) within dist behind it, in its lane. */
    boolean clearBehind(Vehicle v, float dist) {
        float fx = (float) Math.cos(v.angle), fy = (float) Math.sin(v.angle);
        for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
            Vehicle o = fleet.vehicles.get(i);
            if (o == v || o.removedFromFleet || airborne(o)) continue;
            float dx = o.x - v.x, dy = o.y - v.y, back = -(dx * fx + dy * fy), side = Math.abs(dx * -fy + dy * fx);
            if (back > 0 && back < dist + (v.length() + o.length()) * 0.5f && side < 9) return false;
        }
        return !city.solidAt(v.x - fx * (v.length() * 0.5f + 6), v.y - fy * (v.length() * 0.5f + 6));
    }

    /**
     * One step of driving downhill in the vehicle's field, keeping to the right-hand side of the road.
     * Returns false once it has arrived (or has no route).
     */
    boolean driveStep(Vehicle v, float dt, float max, float throttle) {
        if (v.field == null) return false;
        // ((27.4) A few times a second: the dead thick on the road ahead, or stuck behind something that isn't going to
        // move - a new way round, if there is one.)
        if (v.player == null && v.rerouteCd <= 0 && (int) (v.anim * 2.5f) != (int) ((v.anim - dt) * 2.5f)) {
            if (steerClear(v) || unjam(v)) {
                v.pathField = null;
                if (v.field == null) return false;
            } else giveWay(v);
        }
        max = roadSpeed(v, dt, max);
        // (Turned back there: it has a new way to go, or none yet.)
        if (v.field == null) return false;
        int W = city.w;
        // Follow the route tile by tile (the "path tile"), and drive in the lane beside it.
        // (10.32) Crossing a bridge, the route steps from one end of it to the other.
        boolean crossing = v.pathT >= 0 && city.bridgeTo(v.pathT) >= 0 && city.overpassAt(v.x, v.y) >= 0;
        if (v.pathField != v.field || v.pathT < 0 || v.field.get(v.pathT) >= City.FAR
                || (!crossing && World.hypot((v.pathT % W + 0.5f) * City.T - v.x, (v.pathT / W + 0.5f) * City.T - v.y) > 44)) {
            findRouteTile(v);
            if (v.pathT < 0) {
                // Off the route's corridor altogether (shoved aside, or round something): plan it again from here.
                // ((11.1) The wait before the next try whether or not this one finds a way: one that didn't was
                // tried again every frame, a whole-city search each time.)
                if (v.rerouteCd > 0) return false;
                v.rerouteCd = 3;
                if (!fleet.routing.route(v, v.tx, v.ty)) return false;
                v.pathField = null;
                return driveStep(v, dt, max, throttle);
            }
        }
        // Pushed over onto the far side of a barrier (or a kerb) from its route tile: carry on from where it is
        // instead of driving along the barrier towards a lane it can't reach (the wrong way, on the highway).
        if (!crossing) {
            int own = city.tileIndex(v.x, v.y);
            if (own != v.pathT) {
                int mid = city.tileIndex(((v.pathT % W + 0.5f) * City.T + v.x) / 2, ((v.pathT / W + 0.5f) * City.T + v.y) / 2);
                if (v.field.get(mid) >= City.FAR) {
                    // (10.32) Not a slip road running the other way, though: back onto the route beside it.
                    if (v.field.get(own) < City.FAR && v.field.get(own) <= v.field.get(v.pathT) + 30) v.pathT = own;
                    // (Only a real barrier between: a kerb corner cut in a junction is nothing to plan round.)
                    else if (v.rerouteCd <= 0 && (city.solidTile(mid % W, mid / W) || city.onHighway(v.x, v.y))
                            && fleet.routing.route(v, v.tx, v.ty)) {
                        // (Off the route on the wrong side: plan the way again from where it is.)
                        v.rerouteCd = 3;
                        v.pathField = null;
                        return driveStep(v, dt, max, throttle);
                    }
                }
            }
        }
        // (10.42) Already on a tile further along the route than its path tile (cutting a corner on the line it
        // planned): carry on from there, instead of going back round for the tile it skipped.
        if (!crossing) {
            int own = city.tileIndex(v.x, v.y), fo = v.field.get(own);
            if (own != v.pathT && fo < v.field.get(v.pathT) && fo > 4
                    && Math.abs(own % W - v.pathT % W) + Math.abs(own / W - v.pathT / W) <= 2) v.pathT = own;
        }
        int cur = v.field.get(v.pathT);
        if (cur <= 4) return false;
        int nt0 = downhill(v.field, v.pathT);
        if (nt0 == v.pathT && cur > 40) {
            // ((16.6) Nowhere lower beside it, a long way short of the end: a tile at the edge of its route's corridor,
            // where it was pushed aside, whose way on lies just outside it. That was taken for arriving - an ammo truck
            // stopped on an open road halfway to the fight and waited there. The lowest tile close by, or a new plan.)
            int best = cur;
            for (int y = v.pathT / W - 2; y <= v.pathT / W + 2; y++)
                for (int x = v.pathT % W - 2; x <= v.pathT % W + 2; x++) {
                    if (x < 0 || y < 0 || x >= W || y >= city.h) continue;
                    int fv = v.field.get(y * W + x);
                    if (fv < best && !city.solidTile(x, y)) {
                        best = fv;
                        nt0 = y * W + x;
                    }
                }
            if (nt0 == v.pathT) {
                if (v.rerouteCd <= 0 && fleet.routing.route(v, v.tx, v.ty)) {
                    v.rerouteCd = 3;
                    v.pathField = null;
                    return driveStep(v, dt, max, throttle);
                }
                return false;
            }
        }
        int tx = v.pathT % W, ty = v.pathT / W, nt = nt0, bx = nt % W, by = nt / W;
        if (bx == tx && by == ty) return false;
        int ddx = Integer.signum(bx - tx), ddy = Integer.signum(by - ty);
        // Keep to your own side of the road, in the middle of the lane.
        float off = city.laneOffset(bx, by, ddx, ddy, laneOf(v));
        if (v.careful > 0) v.careful -= dt;
        // (Going round something stopped in the lane: out towards the middle of the road for a moment.)
        if (v.passing > 0) off = passingOffset(v, dt, bx, by, ddx, ddy, off);
        // Into a junction in your own lane (measured on the road you're on): the turn is made inside it,
        // not by cutting across the lanes before the stop line.
        if (ddx * v.pdx + ddy * v.pdy > 0 && city.junctionIdAt((bx + 0.5f) * City.T, (by + 0.5f) * City.T) >= 0
                && city.junctionIdAt((tx + 0.5f) * City.T, (ty + 0.5f) * City.T) < 0)
            off = city.laneOffset(tx, ty, ddx, ddy, laneOf(v));
        // A step across the road outside a junction (a U-turn mid-block, or pulling out from the far kerb):
        // straight over, slowly, not along a "lane" that runs the wrong way.
        // ((10.49) Not along a curved road, though: there every step is half sideways.)
        int rd = city.junctionIdAt(v.x, v.y) < 0 && city.junctionIdAt((bx + 0.5f) * City.T, (by + 0.5f) * City.T) < 0
                && !city.curveAt(bx, by) ? city.roadDirAt(bx, by) : 0;
        if ((rd == 1 && ddx != 0) || (rd == 2 && ddy != 0)) {
            off = 0;
            max = Math.min(max, 22);
        }
        if (v.pulled) {
            // Pulled over by highway patrol: onto the shoulder, and stop.
            off += City.T * 0.5f;
            max = Math.min(max, Math.max(0, Math.abs(v.speed) - 3));
        } else if (v.yieldT > 0 && v.type == CAR && v.player == null) {
            // Giving way: over to the kerb and slow (or held back from the junction).
            if (v.yieldStop) max = Math.min(max, city.junctionIdAt(v.x, v.y) >= 0 ? max : 2);
            else {
                // ((10.54) Right over against the kerb, however wide the road: a car only edging over left too
                // little room, and the police car or army truck behind it still couldn't get by.)
                int hand = 1;
                float ln = (float) Math.sqrt(ddx * ddx + ddy * ddy);
                float nx = -ddy / ln * hand, ny = ddx / ln * hand;
                float lx = bx * City.T + City.T / 2f - ddy * off, ly = by * City.T + City.T / 2f + ddx * off;
                off += hand * kerbGap(lx, ly, nx, ny);
                // (And it slides over there now, not only once it's rolled forward enough to steer across.)
                float here = kerbGap(v.x, v.y, (float) -Math.sin(v.angle) * hand, (float) Math.cos(v.angle) * hand);
                if (here > 0.5f) {
                    float step = Math.min(here, 18 * dt);
                    float sx = (float) -Math.sin(v.angle) * hand * step, sy = (float) Math.cos(v.angle) * hand * step;
                    if (city.drivable(v.x + sx, v.y + sy)) {
                        v.x += sx;
                        v.y += sy;
                    }
                }
                max = Math.min(max, 7);
            }
        }
        if (v.careful > 0) {
            // Going round in circles: slowly, straight for the middle of each tile until it's back on track.
            off = 0;
            max = Math.min(max, 16);
        }
        float gx = bx * City.T + City.T / 2f - ddy * off, gy = by * City.T + City.T / 2f + ddx * off;
        if (World.hypot(gx - v.x, gy - v.y) < 11 || ((v.x - (bx + 0.5f) * City.T) * ddx + (v.y - (by + 0.5f) * City.T) * ddy) > -2) {
            v.pathT = by * W + bx;
            // Only a step along the road counts as the way it's going, not a turn inside a junction.
            if (city.junctionIdAt(v.x, v.y) < 0) {
                v.pdx = ddx;
                v.pdy = ddy;
            }
        }
        max = turnSpeed(v, ddx, ddy, max);
        // (10.28) Through a junction the route steps across tile by tile, and the side of the lane flipped with
        // every step: the car swung from side to side. Inside one, keep to the lane for the way out instead.
        if (v.careful <= 0 && v.passing <= 0 && !v.pulled && city.junctionIdAt(v.x, v.y) >= 0
                && city.junctionIdAt((bx + 0.5f) * City.T, (by + 0.5f) * City.T) >= 0) {
            int t = nt;
            for (int s = 0; s < 10; s++) {
                int n = downhill(v.field, t);
                if (n == t) break;
                int sx = Integer.signum(n % W - t % W), sy = Integer.signum(n / W - t / W);
                if (city.junctionIdAt((n % W + 0.5f) * City.T, (n / W + 0.5f) * City.T) < 0) {
                    float o2 = city.laneOffset(bx, by, sx, sy, laneOf(v));
                    gx = bx * City.T + City.T / 2f - sy * o2;
                    gy = by * City.T + City.T / 2f + sx * o2;
                    break;
                }
                t = n;
            }
        }
        // (10.42) The line it's about to drive, and (driving normally) steering for a point on along it, further
        // ahead the faster it's going: smooth through the bends and round the corners of a junction, instead of
        // aiming tile to tile.
        buildCorridor(v);
        if (v.corN > fleet.corPi + 1 && !crossing) reachLinePoints(v);
        if (v.careful <= 0 && v.passing <= 0 && !v.pulled && v.yieldT <= 0 && v.corN >= 3 && !crossing
                && Math.abs(bx - tx) + Math.abs(by - ty) == 1) {
            float[] p = pursuitPoint(v, 13 + Math.abs(v.speed) * 0.3f);
            if (p != null) {
                // (The line is planned afresh every moment, and can shift a little as it goes: ease the aim over.)
                if (v.aiming && World.hypot(p[0] - v.aimX, p[1] - v.aimY) < City.T * 2) {
                    float k = Math.min(1, dt * 6);
                    v.aimX += (p[0] - v.aimX) * k;
                    v.aimY += (p[1] - v.aimY) * k;
                } else {
                    v.aimX = p[0];
                    v.aimY = p[1];
                }
                v.aiming = true;
                gx = v.aimX;
                gy = v.aimY;
            } else v.aiming = false;
        } else v.aiming = false;
        steerAndMove(v, dt, gx, gy, max, throttle, cur);
        return true;
    }

    /**
     * ((27.3) A step of driveStep.) Its route is new, or it's been knocked away from its place on it: the tile of the route
     * it's on, or the nearest one; and right off the road with none in reach, it's put back on the nearest road. Leaves
     * v.pathT at -1 if it found none.
     */
    void findRouteTile(Vehicle v) {
        int W = city.w;
        v.pathField = v.field;
        v.pathT = -1;
        int ti = city.tileIndex(v.x, v.y), tx0 = ti % W, ty0 = ti / W, bestV = City.FAR;
        // On the route where it is: carry on from here. Knocked off the road: head back to the nearest bit.
        if (v.field.get(ti) < City.FAR) v.pathT = ti;
        for (int r = 0; r <= 2 && v.pathT < 0; r++)
            for (int y = ty0 - r; y <= ty0 + r; y++)
                for (int x = tx0 - r; x <= tx0 + r; x++) {
                    if (x < 0 || y < 0 || x >= W || y >= city.h) continue;
                    int fv = v.field.get(y * W + x);
                    if (fv < bestV) {
                        bestV = fv;
                        v.pathT = y * W + x;
                    }
                }
        byte under = city.tiles[ti];
        if (v.pathT < 0 && v.rerouteCd <= 0 && under != City.ROAD && under != City.DIRT && under != City.CAR && under != City.LOT) {
            // ((11.1) Right off the road - in a yard, a park, inside a block - with no road under it to find the
            // way from: back onto the nearest road (it had nowhere else to drive from, and tried again for ever),
            // then plan from there.)
            for (int r = 1, found = 0; r <= 12 && found == 0; r++)
                for (int y = ty0 - r; y <= ty0 + r && found == 0; y++)
                    for (int x = tx0 - r; x <= tx0 + r; x++) {
                        if (Math.max(Math.abs(x - tx0), Math.abs(y - ty0)) != r || x < 0 || y < 0 || x >= W || y >= city.h) continue;
                        if (city.tiles[y * W + x] != City.ROAD || (city.oneWay != null && city.oneWay[y * W + x] != 0)) continue;
                        v.x = (x + 0.5f) * City.T;
                        v.y = (y + 0.5f) * City.T;
                        v.speed = 0;
                        found = 1;
                        break;
                    }
        }
    }

    /** ((27.3) A step of driveStep.) Going round something stopped in the lane: how far over from the route's tile it drives while it does (off is its own lane's). */
    float passingOffset(Vehicle v, float dt, int bx, int by, int ddx, int ddy, float off) {
        v.passing -= dt;
        boolean inJ = city.junctionIdAt(v.x, v.y) >= 0;
        // ((27.5) Not in a junction while it's moving: swinging out there cut the corner up onto the pavement. It keeps
        // to its way across and the junction's rules sort out who goes first. (Lights and sirens, or one stuck there
        // behind something that isn't moving, edge round it - on the road.))
        if (inJ && !v.lightsOn() && v.stillT <= 3) return off;
        // (In a junction only to get out of a jam: there a stopped car is usually just waiting to turn.)
        if (!inJ || v.stillT > 0 || v.speed < 20) {
            // ((11.1) Where there's more than one lane each way, round it by the next lane over, not the
            // oncoming one.)
            if (city.lanesEachWay(bx, by, ddx, ddy) > 1 && !inJ) {
                float o2 = city.laneOffset(bx, by, ddx, ddy, laneOf(v) == 0 ? 2 : 0);
                if (roadAt(bx, by, ddx, ddy, o2)) off = o2;
                else v.passing = 0;
            } else {
                // ((12.0) Over to the other side of the route's tile: the oncoming lane.) ((27.5) Only if that's road:
                // never round by the kerb side, up over it onto the pavement (it used to, where the oncoming side
                // wasn't drivable - and the pavement counted as drivable). With no road there it gives up the pass
                // and waits behind.)
                float sg = off == 0 ? 1 : Math.signum(off);
                float sw = off - sg * City.T * 1.1f, sw2 = off - sg * City.T * 0.75f;
                if (roadAt(bx, by, ddx, ddy, sw)) off = sw;
                else if (roadAt(bx, by, ddx, ddy, sw2)) off = sw2;
                // (In a car park or a yard there's no kerb: round by the other side, if that's clear of the building.)
                else if (lotAt(bx, by) && roadAt(bx, by, ddx, ddy, off + sg * City.T * 1.1f)) off = off + sg * City.T * 1.1f;
                else v.passing = 0;
            }
        }
        return off;
    }

    /** ((27.5) Tile (bx, by) is a car park or a yard (a station's lot, a base's). */
    boolean lotAt(int bx, int by) {
        if (bx < 0 || by < 0 || bx >= city.w || by >= city.h) return false;
        byte t = city.tiles[bx + by * city.w];
        return t == City.LOT || t == City.BASE;
    }

    /** ((27.5) The road itself (not the pavement, a plaza or a lot) at offset off across tile (bx, by), heading (ddx, ddy). */
    boolean roadAt(int bx, int by, int ddx, int ddy, float off) {
        int i = city.tileIndex((bx + 0.5f) * City.T - ddy * off, (by + 0.5f) * City.T + ddx * off);
        return i >= 0 && passable(i, city.tiles[bx + by * city.w]);
    }

    /**
     * ((27.5) Somewhere a vehicle on a tile of kind own, going round another, may drive: the road (parking bays too), or
     * a country lane - and out on a lane in the country, the verge beside it (two meeting on a lane one car wide pull
     * over onto the grass to pass). In a car park or a yard, the rest of it. Never the pavement, a plaza or anything
     * solid.
     */
    boolean passable(int i, byte own) {
        byte t = city.tiles[i];
        if (city.solid[i]) return false;
        if (t == City.ROAD || t == City.DIRT || t == City.CAR) return true;
        if (own == City.DIRT) return t == City.GRASS || t == City.SAND || t == City.TRAIL;
        if (own == City.LOT || own == City.BASE) return t == City.LOT || t == City.BASE;
        return false;
    }

    /** ((27.5) All of the line lat over from v (to its right; negative, its left), for reach ahead, somewhere it may pass on. */
    boolean sideClear(Vehicle v, float fx, float fy, float lat, float reach, byte ownT) {
        for (float a = 0; a <= reach; a += 6) {
            int i = city.tileIndex(v.x + fx * a - fy * lat, v.y + fy * a + fx * lat);
            if (i < 0 || !passable(i, ownT)) return false;
        }
        return true;
    }

    /** ((27.5) How long a pass goes on once there's nothing left ahead to go round: back into its lane promptly. */
    static final float PASS_T = 0.8f;

    /**
     * ((27.5) Whether v may go round o, stopped in its lane gap ahead, now: on the right side - a lane over the same way
     * on a wide road, else the oncoming lane - all of it road (never the pavement), with nothing coming in that lane
     * for the length of the pass and a good way beyond, and not at or near a junction (lights and sirens may, round a
     * queue at a light, so long as the way's clear). Once it's going round it carries on (passingOffset still keeps
     * it on the road).
     */
    boolean canPass(Vehicle v, Vehicle o, float gap) {
        if (v.passing > 0) return true;
        // (Sat behind something that isn't moving - parked, a wreck, a car at a scene, a knot - for a few seconds: it may
        // edge round it near a junction, or in one, carefully, if the way's clear. Overtaking traffic that's moving, or
        // only just stopped, never there.)
        boolean stuck = v.stillT > 3 && Math.abs(o.speed) < 2;
        boolean lights = v.lightsOn();
        if (city.junctionIdAt(v.x, v.y) >= 0 && !stuck && !lights) return false;
        float fx = (float) Math.cos(v.angle), fy = (float) Math.sin(v.angle), rx = -fy, ry = fx;
        float reach = Math.max(0, gap) + (v.length() + o.length()) * 0.5f + 18;
        if (!lights && !stuck)
            for (float a = -City.T; a <= reach + City.T * 2; a += City.T * 0.5f)
                if (city.junctionIdAt(v.x + fx * a, v.y + fy * a) >= 0) return false;
        int dx = v.pdx, dy = v.pdy;
        if (dx == 0 && dy == 0) {
            if (Math.abs(fx) > Math.abs(fy)) dx = fx > 0 ? 1 : -1;
            else dy = fy > 0 ? 1 : -1;
        }
        int own = city.tileIndex(v.x, v.y);
        if (own < 0) return false;
        // (In a car park or a yard - a station's lot, a base's - behind one parked there, or anything else that isn't
        // moving: round it, there's no lane or pavement to keep to. Its nose there is often still swinging round a turn,
        // or pointing at the building, so a line straight ahead of it ran into the wall, and a car leaving the lot waited
        // behind one parked in a bay for good - with the lot's other cars, one off to a call among them, queued behind it.)
        if ((stuck || o.parked || o.broken) && lotAt(own % city.w, own / city.w)) return true;
        boolean oncoming = city.lanesEachWay(own % city.w, own / city.w, dx, dy) <= 1;
        float lat = oncoming ? -City.T * 1.05f : laneOf(v) == 0 ? City.T * 0.95f : -City.T * 0.95f;
        // All of it road (or a lane and its verge, out in the country). (On a narrow street, a little less far over.)
        byte ownT = city.tiles[own];
        if (!sideClear(v, fx, fy, lat, reach, ownT)) {
            lat *= 0.7f;
            if (!sideClear(v, fx, fy, lat, reach, ownT)) return false;
        }
        // (Stuck ten seconds behind something that isn't moving - a knot, not a slow car: it edges out round it at a
        // crawl, on the road, whatever's in the other lane. It still stops short of anything in its way there
        // (followLimit), and traffic coming the other way sees it and waits; the alternative is nobody ever moves.)
        if (v.stillT > 10 && Math.abs(o.speed) < 2) return true;
        // Nothing in that lane: coming towards it far enough off to finish (more the faster it comes), or there at all.
        // (One standing still in it beyond the end of the pass isn't coming: it's queued the other way, and it'll see
        // this one in front of it if it moves. It's oncoming traffic on the move that needs the long clear stretch.)
        // (Stuck a good while - a knot, not a slow car - it'll take a shorter gap, crawling, than one overtaking at speed.)
        float far = reach + (oncoming ? (v.stillT > 15 ? 40 : 80) : 25);
        int c0x = Math.max(0, (int) ((v.x - far - 40) / GC)), c1x = Math.min(fleet.gridW - 1, (int) ((v.x + far + 40) / GC));
        int c0y = Math.max(0, (int) ((v.y - far - 40) / GC)), c1y = Math.min(fleet.gridH - 1, (int) ((v.y + far + 40) / GC));
        for (int cy = c0y; cy <= c1y; cy++)
            for (int cx = c0x; cx <= c1x; cx++)
                for (int i = fleet.gridStart[cy * fleet.gridW + cx], ie = fleet.gridStart[cy * fleet.gridW + cx + 1]; i < ie; i++) {
                    Vehicle q = fleet.gridV[i];
                    if (q == v || q == o || q.removedFromFleet || offRoad(q) || levels(v, q)) continue;
                    float qx = q.x - v.x, qy = q.y - v.y, a = qx * fx + qy * fy, s = qx * rx + qy * ry;
                    if (Math.abs(s - lat) > City.T * 0.5f) continue;
                    float towards = q.speed * -((float) Math.cos(q.angle) * fx + (float) Math.sin(q.angle) * fy);
                    float clear = Math.abs(q.speed) < 3 ? reach + 14 : far + Math.max(0, towards) * 1.5f;
                    if (a > -14 && a < clear) return false;
                }
        return true;
    }

    /** ((27.3) A step of driveStep.) The most it may do here: the open-road scale, rough ground, whatever's ahead in the lane, a checkpoint. */
    float roadSpeed(Vehicle v, float dt, float max) {
        // (10.20) Open-road speeds are nearly twice what they were, next to people on foot (who walk briskly in
        // this world): a car in town now covers ground like a car. Crawls, turns and queues are as before.
        if (max > 40) max = 40 + (max - 40) * DRIVE_SCALE;
        // (Off the road, across the grass: slower going.)
        if (city.rough(v.x, v.y)) max *= 0.65f;
        // Keep a safe distance behind whatever is ahead in the lane.
        max = Math.min(max, followLimit(v));
        // (10.32) And stop at an army checkpoint.
        if (!city.checkpoints.isEmpty()) max = Math.min(max, checkpointLimit(v, dt));
        return max;
    }

    /**
     * ((27.3) A step of driveStep: the most it may do with what's coming up on its route, going (ddx, ddy) now.)
     * Slow down for a turn coming up on the route: round a corner at a walking-pace crawl, not at cruising speed.
     * (Emergency vehicles take corners a little quicker, but still slow enough to keep to their own side:
     * the faster it's going, the further ahead it looks for the turn.)
     */
    float turnSpeed(Vehicle v, int ddx, int ddy, float max) {
        int W = city.w;
        // ((10.49) Off or on to a slip road: a sweeping bend, not a street corner.)
        float round = city.onSlip(v.x, v.y) ? 50 : v.type == CAR ? 26 : 31;
        int t = v.pathT, steps = 0, look = Math.min(8, 4 + (int) (Math.abs(v.speed) / 30));
        for (; steps < look; steps++) {
            int n = downhill(v.field, t);
            if (n == t) break;
            int sx = Integer.signum(n % W - t % W), sy = Integer.signum(n / W - t / W);
            if (sx != ddx || sy != ddy) {
                // (10.49) A curve (a slip road, a bend in the country) is a staircase of little turns, tile by
                // tile, that keeps coming back to the way it was going: not a corner to crawl round. Only a turn
                // that goes on the new way for a few tiles is one.
                int u = n, back = 0, kept = 0;
                for (int k = 0; k < 3; k++) {
                    int m = downhill(v.field, u);
                    if (m == u) break;
                    int qx = Integer.signum(m % W - u % W), qy = Integer.signum(m / W - u / W);
                    if (qx == ddx && qy == ddy) back++;
                    else if (qx == sx && qy == sy) kept++;
                    u = m;
                }
                // (On a slip road, any bend short of the junction at its end is taken at road speed.)
                boolean slipBend = city.onSlip(v.x, v.y) && city.onSlip((n % W + 0.5f) * City.T, (n / W + 0.5f) * City.T)
                        && city.junctionIdAt((n % W + 0.5f) * City.T, (n / W + 0.5f) * City.T) < 0;
                if ((back > 0 && kept < 2) || slipBend) {
                    // (Still a bend: a little slower the tighter the staircase, but at road speed.)
                    max = Math.min(max, 70 + steps * City.T * 0.75f + back * 12);
                    t = n;
                    continue;
                }
                max = Math.min(max, round + steps * City.T * 0.75f);
                break;
            }
            t = n;
        }
        return max;
    }

    /**
     * ((27.3) A step of driveStep.)
     * (10.42) Driving the lane where the route's tiles lie across the road from it (a wide road, or a U-turn):
     * reaching the line's point for the next tile or two counts as reaching those tiles.
     */
    void reachLinePoints(Vehicle v) {
        for (int j = Math.min(v.corN - 1, fleet.corPi + 3); j > fleet.corPi; j--)
            if (World.hypot(v.cor[j * 2] - v.x, v.cor[j * 2 + 1] - v.y) < City.T * 0.55f) {
                v.pathT = fleet.corTiles[j];
                int c2 = v.field.get(v.pathT);
                if (c2 < v.lastDist - 1) {
                    v.lastDist = c2;
                    v.stuckTimer = 0;
                    v.spin = 0;
                }
                break;
            }
    }

    /** ((27.3) A step of driveStep.) Turns towards (gx, gy) as fast as its speed lets it, picks its speed for the turn and the limit, and moves; cur is how far it has left, to tell when it's getting on. */
    void steerAndMove(Vehicle v, float dt, float gx, float gy, float max, float throttle, int cur) {
        float want = (float) Math.atan2(gy - v.y, gx - v.x);
        float diff = want - v.angle;
        while (diff > Math.PI) diff -= Math.PI * 2;
        while (diff < -Math.PI) diff += Math.PI * 2;
        if (v.giveWayT > 0) {
            // ((27.5) Giving way in a knot: straight back, slowly, until it's backed a length or something's behind.)
            v.giveWayT -= dt;
            if (!clearBehind(v, 4)) v.giveWayT = 0;
            else {
                v.speed += (-10 - v.speed) * Math.min(1, dt * 3.2f);
                v.x += (float) Math.cos(v.angle) * v.speed * dt;
                v.y += (float) Math.sin(v.angle) * v.speed * dt;
                return;
            }
        }
        if (v.backT > 0) {
            // ((27.4) Backing out of a jam: slowly, the nose swinging round towards the new way, until it's pointing
            // more or less along it or something's behind.)
            v.backT -= dt;
            if (Math.abs(diff) < 0.6f || !clearBehind(v, 4)) v.backT = 0;
            else {
                float steer = Math.abs(v.speed) < 1.5f ? 0 : dt * 2.4f;
                v.angle += Math.max(-steer, Math.min(steer, diff));
                v.speed += (-12 - v.speed) * Math.min(1, dt * 3.2f);
                v.x += (float) Math.cos(v.angle) * v.speed * dt;
                v.y += (float) Math.sin(v.angle) * v.speed * dt;
                return;
            }
        }
        // Wheels only turn the car while it's moving.
        // (Standing still, or as good as - held at a junction or in a jam - it doesn't turn on the spot.)
        float steer = Math.abs(v.speed) < 1.5f ? 0 : dt * 5 * Math.min(1, 0.05f + Math.abs(v.speed) / 18f);
        // (10.42) Following its line: ease the wheel round, rather than snapping onto every last fraction of a degree.
        float turned = Math.max(-steer, Math.min(steer, v.aiming ? diff * Math.min(1, dt * 9) : diff));
        v.angle += turned;
        // A full circle and more without getting any nearer: it's missed its turn and is orbiting.
        // (10.49) Not on a slip road, though: one that loops round under the highway isn't a car going in circles.
        // ((20.0) A slip road's loop is allowed for, not ignored: a car that had overshot its turn at speed could go round
        // and round one for good.)
        boolean slip = city.onSlip(v.x, v.y);
        v.spin += turned;
        if (Math.abs(v.spin) > (slip ? 11f : 7.5f) && v.player == null) {
            v.spin = 0;
            // ((19.6) Round again while still flagged from the last time - a driver whose errand doesn't act on it,
            // such as one on a call: longer at the careful crawl, which makes even a tight turn.)
            v.careful = v.orbited ? 12 : 4;
            v.orbited = true;
            int own = city.tileIndex(v.x, v.y);
            if (v.field.get(own) < City.FAR) v.pathT = own;
        }
        float target = max * throttle * (0.35f + 0.65f * Math.max(0, (float) Math.cos(diff)));
        // ((20.0) Pointing well away from where it's going - a U-turn, a junction taken too fast: down to a crawl that
        // turns inside a lane's width. A quick motorcycle used to swing wide, miss, and go round again.)
        if (Math.abs(diff) > 1.1f && v.player == null) target = Math.min(target, 16);
        // A badly damaged car limps along.
        if (v.hp < v.maxHp * 0.3f) target *= 0.6f;
        // Brake harder than you accelerate.
        v.speed += (target - v.speed) * Math.min(1, dt * (target < v.speed ? 5f : 3.2f));
        v.x += (float) Math.cos(v.angle) * v.speed * dt;
        v.y += (float) Math.sin(v.angle) * v.speed * dt;
        if (cur < v.lastDist - 1) {
            v.lastDist = cur;
            v.stuckTimer = 0;
            v.spin = 0;
        }
    }
}
