package com.instantaxtion.zombiesandbox;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.City.*;

/**
 * (27.3) The road network: the highway and its interchanges, the beltway and ring road, slip roads, lanes and
 * driveways, and the tidying passes that join it all up. Taken out of City.java as it stood; the city itself (its
 * tiles, buildings and everything the game asks of it) is `city`.
 */
final class CityRoads extends CityPart {
    CityRoads(City city) {
        super(city);
    }

    /**
     * Driveways (10.19): a house, cabin or farm whose front door opens onto nothing but grass gets a track out
     * to the nearest road or lane: dirt out in the country, paved where it meets a pavement in town. Two tiles
     * wide where there's room, so a car fits.
     */
    void driveways() {
        int n = city.w * city.h;
        int[] seen = new int[n], prev = new int[n], q = new int[n];
        int stamp = 0;
        for (Building b : city.buildings) {
            if ((b.kind != HOUSE && b.kind != BARN) || b.doorX <= 0) continue;
            int tx = (int) (b.doorX / T), ty = (int) (b.doorY / T);
            if (tx < 1 || ty < 1 || tx >= city.w - 1 || ty >= city.h - 1) continue;
            boolean reached = false;
            for (int dy = -1; dy <= 1 && !reached; dy++)
                for (int dx = -1; dx <= 1; dx++)
                    if (drivewayEnd(tx + dx, ty + dy)) reached = true;
            if (reached) continue;
            if (straightDrive(b)) continue;
            stamp++;
            int head = 0, tail = 0, start = ty * city.w + tx, goal = -1;
            q[tail++] = start;
            seen[start] = stamp;
            prev[start] = -1;
            // (Each step's length, so it stops a sensible way out.)
            int levelEnd = tail, depth = 0;
            while (head < tail && goal < 0 && depth <= 40) {
                int c = q[head++], x = c % city.w, y = c / city.w;
                for (int k = 0; k < 4 && goal < 0; k++) {
                    int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = y + (k == 2 ? 1 : k == 3 ? -1 : 0);
                    if (nx < 1 || ny < 1 || nx >= city.w - 1 || ny >= city.h - 1) continue;
                    int j = ny * city.w + nx;
                    if (seen[j] == stamp) continue;
                    byte t = city.tiles[j];
                    if (drivewayEnd(nx, ny)) {
                        goal = c;
                        prev[j] = c;
                        break;
                    }
                    if (t != GRASS && t != TREE) continue;
                    if (city.bridge != null && city.bridge[j]) continue;
                    if (gen.byHighway(nx, ny, 1)) continue;
                    seen[j] = stamp;
                    prev[j] = c;
                    q[tail++] = j;
                }
                if (head == levelEnd) {
                    depth++;
                    levelEnd = tail;
                }
            }
            if (goal < 0) continue;
            byte end = drivewaySurface(goal % city.w, goal / city.w);
            for (int c = goal, last = -1; c >= 0; last = c, c = prev[c]) {
                city.tiles[c] = end;
                city.solid[c] = false;
                // The second lane of it: the tile to one side of the way it runs (clear of the house).
                int step = last >= 0 ? last - c : prev[c] >= 0 ? c - prev[c] : 0;
                if (step == 0 || c == start) continue;
                int side = step == 1 || step == -1 ? city.w : 1;
                int o = c + side;
                if (o >= 0 && o < n && (city.tiles[o] == GRASS || city.tiles[o] == TREE) && !nextToBuilding(o)) {
                    city.tiles[o] = end;
                    city.solid[o] = false;
                }
            }
        }
    }

    /**
     * The usual kind: straight out from the side of the house nearest a road, two tiles wide, a little to one
     * side of the middle (where the garage would be). False if no side has a clear straight run to a road.
     */
    boolean straightDrive(Building b) {
        int x0 = (int) (b.x0 / T), y0 = (int) (b.y0 / T), x1 = (int) Math.ceil(b.x1 / T) - 1, y1 = (int) Math.ceil(b.y1 / T) - 1;
        int best = -1, bestLen = 31, bestScore = Integer.MAX_VALUE, bestSx = 0, bestSy = 0;
        // (The front, where the door is, wins unless another side is a lot nearer the road.)
        float cx = (b.x0 + b.x1) / 2, cy = (b.y0 + b.y1) / 2;
        float rx = (b.doorX - cx) / Math.max(1, b.x1 - b.x0), ry = (b.doorY - cy) / Math.max(1, b.y1 - b.y0);
        int front = Math.abs(rx) > Math.abs(ry) ? (rx > 0 ? 0 : 1) : (ry > 0 ? 2 : 3);
        for (int side = 0; side < 4; side++) {
            int dx = side == 0 ? 1 : side == 1 ? -1 : 0, dy = side == 2 ? 1 : side == 3 ? -1 : 0;
            int len0 = dx != 0 ? y1 - y0 + 1 : x1 - x0 + 1;
            if (len0 < 3) continue;
            // (A third of the way along the wall.)
            int along = len0 / 3;
            int sx = dx > 0 ? x1 + 1 : dx < 0 ? x0 - 1 : x0 + along, sy = dy > 0 ? y1 + 1 : dy < 0 ? y0 - 1 : y0 + along;
            int px = dx != 0 ? 0 : 1, py = dx != 0 ? 1 : 0;
            int len = 0;
            boolean ok = false;
            for (int k = 0; k < 30; k++) {
                int ax = sx + dx * k, ay = sy + dy * k, bx = ax + px, by = ay + py;
                if (ax < 1 || ay < 1 || bx >= city.w - 1 || by >= city.h - 1 || ax >= city.w - 1 || ay >= city.h - 1) break;
                byte ta = city.tiles[ay * city.w + ax], tb = city.tiles[by * city.w + bx];
                if (drivewayEnd(ax, ay) || drivewayEnd(bx, by)) {
                    ok = true;
                    break;
                }
                if (gen.byHighway(ax, ay, 1) || gen.byHighway(bx, by, 1)) break;
                if ((ta != GRASS && ta != TREE) || (tb != GRASS && tb != TREE)) break;
                if (city.bridge != null && (city.bridge[ay * city.w + ax] || city.bridge[by * city.w + bx])) break;
                len = k + 1;
            }
            int score = len - (side == front ? 8 : 0);
            if (ok && score < bestScore) {
                bestScore = score;
                bestLen = len;
                best = side;
                bestSx = sx;
                bestSy = sy;
            }
        }
        if (best < 0) return false;
        int dx = best == 0 ? 1 : best == 1 ? -1 : 0, dy = best == 2 ? 1 : best == 3 ? -1 : 0;
        int px = dx != 0 ? 0 : 1, py = dx != 0 ? 1 : 0;
        int ex = bestSx + dx * bestLen, ey = bestSy + dy * bestLen;
        byte end = drivewaySurface(ex, ey);
        for (int k = 0; k < bestLen; k++)
            for (int q = 0; q <= 1; q++) {
                int i = (bestSy + dy * k + py * q) * city.w + bestSx + dx * k + px * q;
                city.tiles[i] = end;
                city.solid[i] = false;
            }
        return true;
    }

    /** What a drive meeting the road at (x, y) is made of: a dirt track off a lane or out in the country, paved in town. */
    byte drivewaySurface(int x, int y) {
        if (city.tiles[y * city.w + x] == DIRT) return DIRT;
        for (int j = Math.max(0, y - 3); j <= Math.min(city.h - 1, y + 3); j++)
            for (int i = Math.max(0, x - 3); i <= Math.min(city.w - 1, x + 3); i++)
                if (city.tiles[j * city.w + i] == SIDEWALK || city.tiles[j * city.w + i] == LOT) return LOT;
        return DIRT;
    }

    boolean drivewayEnd(byte t) {
        return t == ROAD || t == DIRT || t == LOT || t == SIDEWALK;
    }

    /**
     * (10.34) Every road joins up: a lane or street left on its own (one that stopped at a pavement, a park or a
     * field) is carried on, the shortest way across open ground or pavement, to the rest of the roads.
     */
    void joinRoads() {
        if (city.cfg.civic < 18) return;
        int n = city.w * city.h;
        int[] comp = new int[n];
        Arrays.fill(comp, -1);
        List<Integer> sizes = new ArrayList<Integer>();
        int[] q = new int[n];
        for (int i = 0; i < n; i++) {
            if (comp[i] >= 0 || !roadish(i)) continue;
            int id = sizes.size(), head = 0, tail = 0;
            q[tail++] = i;
            comp[i] = id;
            while (head < tail) {
                int c = q[head++], x = c % city.w, y = c / city.w;
                // (Over a bridge counts as joined.)
                int link = city.bridgeTo(c);
                for (int k = 0; k < (link >= 0 ? 5 : 4); k++) {
                    int nx = k == 4 ? link % city.w : x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = k == 4 ? link / city.w : y + (k == 2 ? 1 : k == 3 ? -1 : 0);
                    if (nx < 0 || ny < 0 || nx >= city.w || ny >= city.h) continue;
                    int j = ny * city.w + nx;
                    if (comp[j] >= 0 || !roadish(j)) continue;
                    comp[j] = id;
                    q[tail++] = j;
                }
            }
            sizes.add(tail);
        }
        if (sizes.size() < 2) return;
        int main = 0;
        for (int k = 1; k < sizes.size(); k++) if (sizes.get(k) > sizes.get(main)) main = k;
        int[] prev = new int[n], seen = new int[n];
        int stamp = 0;
        boolean[] joined = new boolean[sizes.size()];
        joined[main] = true;
        // ((22.0) Each piece's tiles listed once, in one pass over the map: looking through the whole map for each piece
        // was the slowest part of making a big city.)
        int nc = sizes.size();
        int[] at = new int[nc + 1];
        for (int i = 0; i < n; i++) if (comp[i] >= 0) at[comp[i] + 1]++;
        for (int k = 0; k < nc; k++) at[k + 1] += at[k];
        int[] byComp = new int[at[nc]], fill = Arrays.copyOf(at, nc);
        for (int i = 0; i < n; i++) if (comp[i] >= 0) byComp[fill[comp[i]]++] = i;
        for (int id = 0; id < sizes.size(); id++) {
            if (joined[id] || sizes.get(id) < 4) continue;
            stamp++;
            int head = 0, tail = 0, goal = -1;
            for (int k = at[id]; k < at[id + 1]; k++) {
                int i = byComp[k];
                if (comp[i] == id) {
                    q[tail++] = i;
                    seen[i] = stamp;
                    prev[i] = -1;
                }
            }
            int levelEnd = tail, depth = 0;
            // (A long way, if need be: across the fields the far side of the highway to a bridge.)
            while (head < tail && goal < 0 && depth <= 240) {
                int c = q[head++], x = c % city.w, y = c / city.w;
                for (int k = 0; k < 4 && goal < 0; k++) {
                    int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = y + (k == 2 ? 1 : k == 3 ? -1 : 0);
                    if (nx < 1 || ny < 1 || nx >= city.w - 1 || ny >= city.h - 1) continue;
                    int j = ny * city.w + nx;
                    if (seen[j] == stamp) continue;
                    seen[j] = stamp;
                    prev[j] = c;
                    if (comp[j] >= 0 && joined[comp[j]]) {
                        goal = j;
                        break;
                    }
                    byte t = city.tiles[j];
                    boolean open = t == GRASS || t == TREE || t == SIDEWALK || t == PLAZA || t == SAND || t == TRAIL || t == RAIL;
                    if (!open || gen.byHighway(nx, ny, 1) || (city.bridge != null && city.bridge[j]) || nearRamp(j)) continue;
                    q[tail++] = j;
                }
                if (head == levelEnd) {
                    depth++;
                    levelEnd = tail;
                }
            }
            if (goal < 0) continue;
            // Lay it, two tiles wide, from the road it reached back to this one.
            for (int c = prev[goal]; c >= 0 && comp[c] != id; c = prev[c]) {
                int p = prev[c];
                int dx = p >= 0 ? p % city.w - c % city.w : 0, dy = p >= 0 ? p / city.w - c / city.w : 0;
                pave(c, dx, dy);
                int sx = c % city.w - dy, sy = c / city.w + dx;
                if (sx >= 1 && sy >= 1 && sx < city.w - 1 && sy < city.h - 1 && !gen.byHighway(sx, sy, 1)) pave(sy * city.w + sx, dx, dy);
            }
            joined[id] = true;
        }
    }

    /**
     * (10.40) The last word on the roads of a city built since 10.40: bits of lane and road that lead nowhere
     * (left when a house didn't go up, or stopped short of something) are taken up, and every home is joined to
     * a real road or lane by a drive of its own - not to the pavement, nor to another house's drive.
     */
    void tidyRoads() {
        frontageRoads();
        separateRamps();
        pruneSpurs();
        connectHomes();
        lanesFromDrives();
        // (And any bit of lane still on its own joined up, with no bits of paved street left in the lanes.)
        joinRoads();
        if (city.cfg.civic >= 20) closeGaps();
        unpaveLaneStubs();
        clearOrphans();
        if (city.cfg.civic >= 20) {
            pruneDeadLanes();
            closeGaps();
        }
        // ((11.0) With the town right up to the highway now, a drive the gaps were closed with can end up beside
        // a ramp: it stays a yard, not a way onto the ramp.)
        if (city.cfg.civic >= 26 && city.drive != null && city.slipWay != null)
            for (int i = 0; i < city.w * city.h; i++) if (city.drive[i] && city.tiles[i] == LOT && nearRamp(i)) city.drive[i] = false;
    }


    /**
     * (10.43) A lane that just stops in the middle of a field (nothing at its end: no house, yard, car park, path
     * or shore) is taken back to where it last met something.
     */
    void pruneDeadLanes() {
        for (int round = 0; round < 300; round++) {
            boolean changed = false;
            for (int y = 1; y < city.h - 1; y++)
                for (int x = 1; x < city.w - 1; x++) {
                    int i = y * city.w + x;
                    if (city.tiles[i] != DIRT || (city.bridge != null && city.bridge[i])) continue;
                    int four = 0;
                    for (int k = 0; k < 4; k++) {
                        byte o = city.tiles[i + (k == 0 ? 1 : k == 1 ? -1 : k == 2 ? city.w : -city.w)];
                        if (o == ROAD || o == DIRT || o == CAR || o == LOT) four++;
                    }
                    if (four > 1) continue;
                    boolean near = false;
                    for (int dy = -1; dy <= 1 && !near; dy++)
                        for (int dx = -1; dx <= 1; dx++) {
                            byte o = city.tiles[(y + dy) * city.w + x + dx];
                            if (o == LOT || o == BUILDING || o == BASE || o == PIER || o == SIDEWALK || o == PLAZA || o == RAIL
                                    || o == TRAIL || o == WATER || o == SAND) near = true;
                        }
                    if (near) continue;
                    city.tiles[i] = GRASS;
                    changed = true;
                }
            if (!changed) break;
        }
    }

    /** Somewhere a car can drive (or park), for closeGaps: not the highway or its ramps. */
    boolean gapWay(int x, int y) {
        if (x < 0 || y < 0 || x >= city.w || y >= city.h) return false;
        int i = y * city.w + x;
        byte t = city.tiles[i];
        if (t == ROAD) return (city.oneWay == null || city.oneWay[i] == 0) && (city.slipWay == null || city.slipWay[i] == 0);
        return t == DIRT || t == LOT || t == CAR;
    }

    /** Open ground a gap can be closed over. */
    boolean gapFill(int x, int y) {
        if (x < 1 || y < 1 || x >= city.w - 1 || y >= city.h - 1) return false;
        int i = y * city.w + x;
        return city.tiles[i] == GRASS && !nearRamp(i);
    }

    void fillAs(int x, int y, int from) {
        int i = y * city.w + x;
        city.tiles[i] = city.tiles[from] == DIRT ? DIRT : LOT;
        if (city.drive != null) city.drive[i] = city.tiles[i] == LOT;
    }

    /**
     * (10.43) Gaps between lanes, drives and roads that nearly meet: a lane or drive that ends a tile short of
     * another road, with only grass between, is carried on to meet it; and two that only touch corner to corner
     * are joined across the corner, so they meet in a smooth bend rather than a pinch.
     */
    void closeGaps() {
        for (int round = 0; round < 2; round++)
            for (int y = 1; y < city.h - 1; y++)
                for (int x = 1; x < city.w - 1; x++) {
                    int i = y * city.w + x;
                    if (!scrap(i)) continue;
                    int n = 0, bx = 0, by = 0;
                    for (int k = 0; k < 4; k++) {
                        int dx = k == 0 ? 1 : k == 1 ? -1 : 0, dy = k == 2 ? 1 : k == 3 ? -1 : 0;
                        if (gapWay(x + dx, y + dy)) {
                            n++;
                            bx = dx;
                            by = dy;
                        }
                    }
                    // A dead end: straight on across one tile of grass to a road or lane.
                    if (n == 1) {
                        int dx = -bx, dy = -by, fx = x + 2 * dx, fy = y + 2 * dy;
                        if (gapFill(x + dx, y + dy) && gapWay(fx, fy)) {
                            byte ft = city.tiles[fy * city.w + fx];
                            if (ft == ROAD || ft == DIRT || scrap(fy * city.w + fx)) fillAs(x + dx, y + dy, i);
                        }
                    }
                    // Corner to corner with another way, and nothing joining them: across the corner.
                    for (int dx = -1; dx <= 1; dx += 2)
                        for (int dy = -1; dy <= 1; dy += 2) {
                            if (!gapWay(x + dx, y + dy) || gapWay(x + dx, y) || gapWay(x, y + dy)) continue;
                            if (gapFill(x + dx, y)) fillAs(x + dx, y, i);
                            else if (gapFill(x, y + dy)) fillAs(x, y + dy, i);
                        }
                }
    }

    /** Scraps of lane or drive joined to nothing (left where a drive that ran to a ramp was cut off) go. */
    void clearOrphans() {
        int n = city.w * city.h;
        boolean[] main = mainNetwork();
        int[] comp = new int[n];
        Arrays.fill(comp, -1);
        int[] q = new int[n];
        for (int i = 0; i < n; i++) {
            if (comp[i] >= 0 || !scrap(i)) continue;
            int head = 0, tail = 0;
            q[tail++] = i;
            comp[i] = i;
            boolean joined = false;
            while (head < tail) {
                int c = q[head++], x = c % city.w, y = c / city.w;
                for (int k = 0; k < 4; k++) {
                    int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = y + (k == 2 ? 1 : k == 3 ? -1 : 0);
                    if (nx < 0 || ny < 0 || nx >= city.w || ny >= city.h) continue;
                    int j = ny * city.w + nx;
                    byte t = city.tiles[j];
                    if (main[j] || t == ROAD || t == CAR || (t == LOT && !scrap(j))) joined = true;
                    if (comp[j] >= 0 || !scrap(j)) continue;
                    comp[j] = i;
                    q[tail++] = j;
                }
            }
            if (joined || tail > 40) continue;
            for (int k = 0; k < tail; k++) {
                city.tiles[q[k]] = GRASS;
                if (city.drive != null) city.drive[q[k]] = false;
            }
        }
    }

    boolean scrap(int i) {
        return city.tiles[i] == DIRT || (city.tiles[i] == LOT && city.drive != null && city.drive[i]);
    }

    /**
     * A tarmac drive that winds on from house to house (serving three homes or more, or running a long way) is
     * really a lane: it becomes one, dirt, with the homes along it, instead of a strip of car park through the
     * fields. (A shop's or a station's forecourt is left as it is.)
     */
    void lanesFromDrives() {
        int n = city.w * city.h;
        int[] comp = new int[n];
        Arrays.fill(comp, -1);
        int[] q = new int[n];
        for (int i = 0; i < n; i++) {
            if (comp[i] >= 0 || !city.drive[i] || city.tiles[i] != LOT) continue;
            int head = 0, tail = 0;
            q[tail++] = i;
            comp[i] = i;
            java.util.HashSet<Integer> homes = new java.util.HashSet<Integer>();
            boolean other = false;
            while (head < tail) {
                int c = q[head++], x = c % city.w, y = c / city.w;
                for (int dy = -1; dy <= 1; dy++)
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx, ny = y + dy;
                        if (nx < 0 || ny < 0 || nx >= city.w || ny >= city.h) continue;
                        int j = ny * city.w + nx;
                        int bi = city.buildingAt.get(j);
                        if (bi >= 0 && bi < city.buildings.size()) {
                            int k = city.buildings.get(bi).kind;
                            if (k == HOUSE || k == BARN) homes.add(bi);
                            else other = true;
                        }
                        if (city.tiles[j] == LOT && !city.drive[j]) other = true;
                        if ((dx != 0 && dy != 0) || comp[j] >= 0 || !city.drive[j] || city.tiles[j] != LOT) continue;
                        comp[j] = i;
                        q[tail++] = j;
                    }
            }
            if (homes.size() < 3 && (tail <= 14 || other)) continue;
            for (int k = 0; k < tail; k++) {
                int c = q[k], x = c % city.w, y = c / city.w;
                // (Right by a shop or a car park it stays tarmac: their forecourt.)
                boolean forecourt = false;
                for (int dy = -1; dy <= 1 && !forecourt; dy++)
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx, ny = y + dy;
                        if (nx < 0 || ny < 0 || nx >= city.w || ny >= city.h) continue;
                        int j = ny * city.w + nx, bi = city.buildingAt.get(j);
                        if ((city.tiles[j] == LOT && !city.drive[j] && comp[j] != i) || (bi >= 0 && bi < city.buildings.size()
                                && city.buildings.get(bi).kind != HOUSE && city.buildings.get(bi).kind != BARN)) {
                            forecourt = true;
                            break;
                        }
                    }
                if (forecourt) continue;
                city.tiles[c] = DIRT;
                city.drive[c] = false;
            }
        }
    }

    /** Which of the roads and lanes are the main network (joined up, over the bridges too). */
    boolean[] mainNetwork() {
        int n = city.w * city.h;
        int[] comp = new int[n];
        Arrays.fill(comp, -1);
        int[] q = new int[n];
        int best = -1, bestSize = 0;
        for (int i = 0; i < n; i++) {
            if (comp[i] >= 0 || !roadish(i)) continue;
            int head = 0, tail = 0;
            q[tail++] = i;
            comp[i] = i;
            while (head < tail) {
                int c = q[head++], x = c % city.w, y = c / city.w;
                int link = city.bridgeTo(c);
                for (int k = 0; k < (link >= 0 ? 5 : 4); k++) {
                    int nx = k == 4 ? link % city.w : x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = k == 4 ? link / city.w : y + (k == 2 ? 1 : k == 3 ? -1 : 0);
                    if (nx < 0 || ny < 0 || nx >= city.w || ny >= city.h) continue;
                    int j = ny * city.w + nx;
                    if (comp[j] >= 0 || !roadish(j)) continue;
                    comp[j] = i;
                    q[tail++] = j;
                }
            }
            if (tail > bestSize) {
                bestSize = tail;
                best = i;
            }
        }
        boolean[] main = new boolean[n];
        for (int i = 0; i < n; i++) main[i] = comp[i] == best && best >= 0;
        return main;
    }

    /**
     * Frontage roads: nothing joins the highway but at its interchanges, so the streets and lanes that come up to
     * it on either side are joined to each other by a road running alongside it, instead of each stopping dead
     * at the verge.
     */
    void frontageRoads() {
        if (city.oneWay == null || city.hwyAxis < 0) return;
        int len = city.hwyAxis == 0 ? city.w : city.h;
        for (int side = 0; side < 2; side++) {
            // The two rows just clear of the highway's verge, and the row beyond them the roads come up to.
            int a0 = side == 0 ? -3 : 8, a1 = side == 0 ? -2 : 9, beyond = side == 0 ? -4 : 10;
            if (!hwyRowOk(a0) || !hwyRowOk(a1) || !hwyRowOk(beyond)) continue;
            int start = -1, stubs = 0, last = -1;
            boolean paved = false;
            for (int k = 2; k <= len - 2; k++) {
                boolean clear = k < len - 2;
                if (clear)
                    for (int a = a0; a <= a1 && clear; a++) {
                        int i = frontTile(k, a);
                        byte t = city.tiles[i];
                        if (city.oneWay[i] != 0 || (city.bridge != null && city.bridge[i]) || city.buildingAt.get(i) >= 0
                                || (city.slipWay != null && city.slipWay[i] != 0) || nearRamp(i)
                                || !(t == GRASS || t == TREE || t == DIRT || t == ROAD || t == TRAIL || t == SAND)) clear = false;
                    }
                if (!clear) {
                    if (stubs >= 2) layFrontage(start, last, a0, a1, paved);
                    start = -1;
                    stubs = 0;
                    paved = false;
                    continue;
                }
                int ui = frontTile(k, beyond), oi = frontTile(k, side == 0 ? a1 : a0);
                byte up = city.tiles[ui], on = city.tiles[oi];
                // (A street or lane of the town's, not the highway's own slip roads.)
                boolean upRoad = (up == ROAD || up == DIRT) && city.oneWay[ui] == 0 && (city.slipTile == null || !city.slipTile[ui]);
                boolean onRoad = (on == ROAD || on == DIRT) && city.oneWay[oi] == 0 && (city.slipTile == null || !city.slipTile[oi]);
                boolean stub = upRoad || onRoad;
                if (stub && (last < 0 || k - last > 1)) {
                    // (A gap of more than sixty tiles: two separate stretches.)
                    if (start >= 0 && k - last > 60) {
                        if (stubs >= 2) layFrontage(start, last, a0, a1, paved);
                        start = -1;
                        stubs = 0;
                        paved = false;
                    }
                    if (start < 0) start = k;
                    stubs++;
                }
                if (stub) {
                    last = k;
                    if (up == ROAD || on == ROAD) paved = true;
                }
            }
        }
    }

    /**
     * A tile right beside a slip road (an on or off ramp), but not on the road it leaves from or joins: nothing
     * else may run there, so no street, lane or drive joins a ramp halfway along it.
     */
    boolean nearRamp(int i) {
        if (city.slipWay == null || city.cfg.civic < 19) return false;
        int x = i % city.w, y = i / city.w;
        if (city.slipWay[i] != 0 || onExitRoad(x, y)) return false;
        for (int dy = -1; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx, ny = y + dy;
                if (nx >= 0 && ny >= 0 && nx < city.w && ny < city.h && city.slipWay[ny * city.w + nx] != 0) return true;
            }
        return false;
    }

    /** On the road that crosses the highway at an interchange (where its ramps are meant to meet it). */
    boolean onExitRoad(int x, int y) {
        int k = city.hwyAxis == 0 ? x : y;
        for (int[] e : city.hwyExits)
            if (k >= e[0] - 1 && k <= e[0] + e[1]) return true;
        return false;
    }

    /** Cuts off any road, lane or drive that has come to touch an on or off ramp halfway along it. */
    void separateRamps() {
        if (city.slipWay == null) return;
        int n = city.w * city.h;
        byte[] was = new byte[n];
        List<Integer> cut = new ArrayList<Integer>();
        for (int i = 0; i < n; i++) {
            byte t = city.tiles[i];
            if ((t == ROAD || t == DIRT || t == LOT) && (city.oneWay == null || city.oneWay[i] == 0) && nearRamp(i)
                    && (city.bridge == null || !city.bridge[i])) {
                was[i] = t;
                cut.add(i);
                city.tiles[i] = GRASS;
            }
        }
        if (cut.isEmpty()) return;
        // (Where a ramp's own end runs into a road alongside the highway rather than the one crossing it, that's
        // the way on or off: whatever was cut there goes back, until every ramp can be driven again.)
        int[] f = new int[n];
        for (int round = 0; round < 4; round++) {
            localReach(f);
            boolean restored = false;
            for (int i : cut) {
                if (city.tiles[i] != GRASS) continue;
                int x = i % city.w, y = i / city.w;
                boolean strands = false;
                for (int dy = -1; dy <= 1 && !strands; dy++)
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx, ny = y + dy;
                        if (nx < 0 || ny < 0 || nx >= city.w || ny >= city.h) continue;
                        int j = ny * city.w + nx;
                        if (city.slipWay[j] != 0 && f[j] == 0) {
                            strands = true;
                            break;
                        }
                    }
                if (!strands) continue;
                city.tiles[i] = was[i];
                restored = true;
            }
            if (!restored) break;
        }
        for (int i : cut)
            if (city.tiles[i] == GRASS) {
                city.solid[i] = false;
                city.roadDir[i] = 0;
                city.mainRoad[i] = false;
            }
    }

    /**
     * Which roads, lanes and drives join up with the main network by the local roads alone (not along the
     * highway itself): 1 for those that do, 0 for the rest.
     */
    void localReach(int[] f) {
        int n = city.w * city.h;
        Arrays.fill(f, 0);
        boolean[] main = mainNetwork();
        // Forward (can get there from the town's streets) and back (can get from there to them), by the rules the
        // traffic drives by: the highway one way, down each side of a boulevard, the ramps the way they run.
        byte[] reach = new byte[n];
        int[] q = new int[n];
        for (int dir = 0; dir < 2; dir++) {
            int tail = 0;
            for (int i = 0; i < n; i++)
                if (main[i] && (city.slipWay == null || city.slipWay[i] == 0) && (city.oneWay == null || city.oneWay[i] == 0)) {
                    reach[i] |= 1 << dir;
                    q[tail++] = i;
                }
            int head = 0;
            while (head < tail) {
                int c = q[head++], x = c % city.w, y = c / city.w;
                int link = city.bridgeTo(c);
                if (link >= 0 && (reach[link] & (1 << dir)) == 0) {
                    reach[link] |= 1 << dir;
                    q[tail++] = link;
                }
                for (int k = 0; k < 4; k++) {
                    int dx = k == 0 ? 1 : k == 1 ? -1 : 0, dy = k == 2 ? 1 : k == 3 ? -1 : 0;
                    int nx = x + dx, ny = y + dy;
                    if (nx < 0 || ny < 0 || nx >= city.w || ny >= city.h) continue;
                    int j = ny * city.w + nx;
                    byte t = city.tiles[j];
                    if ((reach[j] & (1 << dir)) != 0 || !(t == ROAD || t == DIRT || t == LOT || t == CAR)) continue;
                    // (Forward: a step from c on to j; back: a step from j on to c.)
                    if (dir == 0 ? !driveStep(c, j, dx, dy) : !driveStep(j, c, -dx, -dy)) continue;
                    reach[j] |= 1 << dir;
                    q[tail++] = j;
                }
            }
        }
        for (int i = 0; i < n; i++) f[i] = reach[i] == 3 ? 1 : 0;
    }

    /** A step a vehicle may take from tile a to the next tile b (dx, dy), by the one-way rules. */
    boolean driveStep(int a, int b, int dx, int dy) {
        if (city.oneWay != null && (city.oneWay[a] != 0) != (city.oneWay[b] != 0) && (city.bridgeTo(a) >= 0 || city.bridgeTo(b) >= 0)) return false;
        if (city.oneWay != null && (!city.oneWayOk(a, dx, dy) || !city.oneWayOk(b, dx, dy))) return false;
        if (city.divided != null && (!wayOk(city.divided, a, dx, dy) || !wayOk(city.divided, b, dx, dy))) return false;
        return city.slipWay == null || city.slipOk(a, b, dx, dy);
    }

    boolean hwyRowOk(int a) {
        int r = city.hwyAt + a;
        return r >= 1 && r < (city.hwyAxis == 0 ? city.h : city.w) - 1;
    }

    int frontTile(int k, int a) {
        return city.hwyAxis == 0 ? (city.hwyAt + a) * city.w + k : k * city.w + city.hwyAt + a;
    }

    void layFrontage(int k0, int k1, int a0, int a1, boolean paved) {
        for (int k = k0; k <= k1 + 1; k++)
            for (int a = a0; a <= a1; a++) {
                int i = frontTile(k, a);
                if (city.tiles[i] == ROAD || (city.tiles[i] == DIRT && !paved)) continue;
                city.tiles[i] = paved ? ROAD : DIRT;
                city.solid[i] = false;
                city.opaque[i] = false;
                city.roadDir[i] = (byte) (paved ? (city.hwyAxis == 0 ? 2 : 1) : 0);
            }
    }

    boolean roadTile(int i) {
        byte t = city.tiles[i];
        return t == ROAD || t == DIRT || t == CAR || t == LOT;
    }

    /** Something that gives the road beside it a reason to be there. */
    boolean anchors(byte t) {
        return t == BUILDING || t == LOT || t == BASE || t == PIER || t == SIDEWALK || t == PLAZA || t == FENCE || t == PUMP
                || t == RAIL || t == TRAIL || t == WATER || t == SAND || t == STATUE;
    }

    /** Takes up the ends of roads and lanes that lead nowhere, a tile at a time, back to where they join something. */
    void pruneSpurs() {
        int n = city.w * city.h;
        int[] q = new int[n];
        boolean[] queued = new boolean[n];
        int head = 0, tail = 0;
        for (int i = 0; i < n; i++)
            if (city.tiles[i] == DIRT || city.tiles[i] == ROAD) {
                q[tail++] = i;
                queued[i] = true;
            }
        int removed = 0;
        while (head != tail && removed < n / 20) {
            int c = q[head];
            head = (head + 1) % n;
            queued[c] = false;
            if (!spur(c)) continue;
            city.tiles[c] = GRASS;
            city.solid[c] = false;
            city.roadDir[c] = 0;
            city.mainRoad[c] = false;
            removed++;
            int x = c % city.w, y = c / city.w;
            for (int dy = -1; dy <= 1; dy++)
                for (int dx = -1; dx <= 1; dx++) {
                    int j = (y + dy) * city.w + x + dx;
                    if ((dx == 0 && dy == 0) || queued[j] || (city.tiles[j] != DIRT && city.tiles[j] != ROAD)) continue;
                    q[tail] = j;
                    tail = (tail + 1) % n;
                    queued[j] = true;
                }
        }
    }

    /** The very end of a road or lane, with nothing it serves beside it, that can go without cutting anything off. */
    boolean spur(int c) {
        byte t = city.tiles[c];
        if (t != DIRT && t != ROAD) return false;
        int x = c % city.w, y = c / city.w;
        if (x < 3 || y < 3 || x >= city.w - 3 || y >= city.h - 3) return false;
        if ((city.bridge != null && city.bridge[c]) || (city.oneWay != null && city.oneWay[c] != 0) || (city.slipTile != null && city.slipTile[c])
                || (city.curveTile != null && city.curveTile[c]) || (gen.keepPaved != null && gen.keepPaved[c]) || city.buildingAt.get(c) >= 0) return false;
        int ring = 0;
        for (int dy = -1; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                int j = (y + dy) * city.w + x + dx;
                if (anchors(city.tiles[j]) || city.buildingAt.get(j) >= 0) return false;
                if (roadTile(j)) ring++;
            }
        if (ring > 3) return false;
        // (A lane passing a home a step or two off still serves it.)
        for (int dy = -2; dy <= 2; dy++)
            for (int dx = -2; dx <= 2; dx++) {
                int bi = city.buildingAt.get((y + dy) * city.w + x + dx);
                if (bi >= 0 && bi < city.buildings.size() && (city.buildings.get(bi).kind == HOUSE || city.buildings.get(bi).kind == BARN)) return false;
            }
        boolean l = roadTile(c - 1), r = roadTile(c + 1), u = roadTile(c - city.w), d = roadTile(c + city.w);
        int four = (l ? 1 : 0) + (r ? 1 : 0) + (u ? 1 : 0) + (d ? 1 : 0);
        if (four <= 1) return true;
        if (four > 2 || (l && r) || (u && d)) return false;
        // Two, round a corner: still joined through the tile between them once this one goes.
        int diag = (y + (u ? -1 : 1)) * city.w + x + (l ? -1 : 1);
        return roadTile(diag);
    }

    /**
     * Every home joined to a real road or lane: a house or barn whose own way out reaches only grass, the
     * pavement or a neighbour's drive gets a drive of its own (straight out where there's a clear run, round
     * things where not), two tiles wide where there's room; across the pavement at a dropped kerb if that's
     * the last thing between it and the road.
     */
    void connectHomes() {
        int n = city.w * city.h;
        city.drive = new boolean[n];
        for (int i = 0; i < n; i++) city.drive[i] = city.tiles[i] == LOT && !lotPatch(i);
        // (Not the edge of a car park either.)
        for (int i = 0; i < n; i++)
            if (city.drive[i]) {
                int x = i % city.w, y = i / city.w;
                for (int dy = -1; dy <= 1 && city.drive[i]; dy++)
                    for (int dx = -1; dx <= 1; dx++) {
                        int xx = x + dx, yy = y + dy;
                        if (xx >= 0 && yy >= 0 && xx < city.w && yy < city.h && city.tiles[yy * city.w + xx] == LOT && lotPatch(yy * city.w + xx)) {
                            city.drive[i] = false;
                            break;
                        }
                    }
            }
        int[] seen = new int[n], prev = new int[n], q = new int[n];
        int stamp = 0;
        boolean[] main = mainNetwork();
        for (Building b : city.buildings) {
            if ((b.kind != HOUSE && b.kind != BARN) || b.doorX <= 0) continue;
            int bx0 = (int) (b.x0 / T) - 1, by0 = (int) (b.y0 / T) - 1, bx1 = (int) Math.ceil(b.x1 / T), by1 = (int) Math.ceil(b.y1 / T);
            if (bx0 < 2 || by0 < 2 || bx1 >= city.w - 2 || by1 >= city.h - 2) continue;
            // On the road already: through its own drive (drive, lane or road all the way, no pavement).
            stamp++;
            int head = 0, tail = 0;
            boolean onStreet = false;
            for (int y = by0; y <= by1; y++)
                for (int x = bx0; x <= bx1; x++) {
                    int j = y * city.w + x;
                    if (city.tiles[j] == SIDEWALK) onStreet = true;
                    if (!roadTile(j) || seen[j] == stamp) continue;
                    seen[j] = stamp;
                    q[tail++] = j;
                }
            if (onStreet) continue;
            boolean reached = false;
            while (head < tail && !reached && tail < 4000) {
                int c = q[head++], x = c % city.w, y = c / city.w;
                if ((city.tiles[c] == ROAD || city.tiles[c] == DIRT || city.tiles[c] == CAR) && main[c] && !gen.byHighway(x, y, 1)) {
                    reached = true;
                    break;
                }
                for (int k = 0; k < 4; k++) {
                    // ((10.49) Not off the edge of the map: a road out to the edge of it ran the search off the end.)
                    int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = y + (k == 2 ? 1 : k == 3 ? -1 : 0);
                    if (nx < 0 || ny < 0 || nx >= city.w || ny >= city.h) continue;
                    int j = ny * city.w + nx;
                    if (seen[j] == stamp || !roadTile(j)) continue;
                    seen[j] = stamp;
                    q[tail++] = j;
                }
            }
            if (reached) continue;
            // A new drive: out from the house over open ground to the nearest real road or lane.
            stamp++;
            head = 0;
            tail = 0;
            for (int y = by0; y <= by1; y++)
                for (int x = bx0; x <= bx1; x++) {
                    int j = y * city.w + x;
                    if (y != by0 && y != by1 && x != bx0 && x != bx1) continue;
                    byte t = city.tiles[j];
                    if (t != GRASS && t != TREE && !roadTile(j)) continue;
                    seen[j] = stamp;
                    prev[j] = -1;
                    q[tail++] = j;
                }
            int goal = -1, levelEnd = tail, depth = 0;
            while (head < tail && goal < 0 && depth <= 150) {
                int c = q[head++], x = c % city.w, y = c / city.w;
                for (int k = 0; k < 4 && goal < 0; k++) {
                    int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = y + (k == 2 ? 1 : k == 3 ? -1 : 0);
                    if (nx < 1 || ny < 1 || nx >= city.w - 1 || ny >= city.h - 1) continue;
                    int j = ny * city.w + nx;
                    if (seen[j] == stamp) continue;
                    seen[j] = stamp;
                    prev[j] = c;
                    byte t = city.tiles[j];
                    if ((t == ROAD || t == DIRT) && main[j] && !gen.byHighway(nx, ny, 1) && (city.oneWay == null || city.oneWay[j] == 0)
                            && (city.slipWay == null || city.slipWay[j] == 0) && !nearRamp(j)) {
                        goal = j;
                        break;
                    }
                    if (gen.byHighway(nx, ny, 1) || (city.bridge != null && city.bridge[j]) || nearRamp(j)) continue;
                    // (The pavement only right at the end, where the drive crosses it to the road.)
                    boolean kerb = t == SIDEWALK && city.tiles[prev[j]] != SIDEWALK;
                    // (Across a footpath too: it's only a path.)
                    if (t != GRASS && t != TREE && t != LOT && t != TRAIL && t != SAND && !kerb) continue;
                    q[tail++] = j;
                }
                if (head == levelEnd) {
                    depth++;
                    levelEnd = tail;
                }
            }
            if (goal < 0) continue;
            byte end = city.tiles[goal] == DIRT ? DIRT : LOT;
            for (int c = prev[goal], last = goal; c >= 0; last = c, c = prev[c]) {
                if (city.tiles[c] == GRASS || city.tiles[c] == TREE || city.tiles[c] == SIDEWALK || city.tiles[c] == TRAIL || city.tiles[c] == SAND) {
                    city.tiles[c] = end;
                    city.solid[c] = false;
                    city.opaque[c] = false;
                    if (end == LOT) city.drive[c] = true;
                }
                // The second lane of it, to one side, where there's grass.
                int step = last - c, side = step == 1 || step == -1 ? city.w : 1;
                int o = c + side;
                if (o > 0 && o < n && (city.tiles[o] == GRASS || city.tiles[o] == TREE) && !nextToBuilding(o) && !gen.byHighway(o % city.w, o / city.w, 1) && !nearRamp(o)) {
                    city.tiles[o] = end;
                    city.solid[o] = false;
                    city.opaque[o] = false;
                    if (end == LOT) city.drive[o] = true;
                }
            }
        }
    }

    /** The middle of a car park (a paved patch at least three tiles across), not a drive. */
    boolean lotPatch(int i) {
        int x = i % city.w, y = i / city.w, c = 0;
        for (int dy = -1; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++) {
                int xx = x + dx, yy = y + dy;
                if (xx >= 0 && yy >= 0 && xx < city.w && yy < city.h && city.tiles[yy * city.w + xx] == LOT) c++;
            }
        return c == 9;
    }

    boolean roadish(int i) {
        return (city.tiles[i] == ROAD || city.tiles[i] == DIRT) && (city.oneWay == null || city.oneWay[i] == 0);
    }

    /** One tile of a joining road: dirt over open ground, road over pavement and the tracks. */
    void pave(int i, int dx, int dy) {
        byte t = city.tiles[i];
        if (t == GRASS || t == TREE || t == SAND || t == TRAIL) city.tiles[i] = DIRT;
        else if (t == SIDEWALK || t == PLAZA || t == RAIL) {
            city.tiles[i] = ROAD;
            city.roadDir[i] = (byte) (dx != 0 ? 2 : 1);
        }
    }

    /** (10.33) Homes, shops and barns right beside the highway go: nothing fronts onto it. */
    void clearHighwaySides() {
        if (city.cfg.civic < 17 || city.oneWay == null) return;
        for (int i = city.buildingLots.size() - 1; i >= 0; i--) {
            int[] l = city.buildingLots.get(i);
            int k = l[6];
            if (k != HOUSE && k != BARN && k != SHOP && k != WAREHOUSE && k != APARTMENT && k != KIOSK && k != GARAGE) continue;
            boolean near = false;
            for (int y = l[1]; y < l[1] + l[3] && !near; y++)
                for (int x = l[0]; x < l[0] + l[2] && !near; x++) near = gen.byHighway(x, y, 3);
            if (!near) continue;
            city.buildingLots.remove(i);
            for (int y = Math.max(0, l[1]); y < Math.min(city.h, l[1] + l[3]); y++)
                for (int x = Math.max(0, l[0]); x < Math.min(city.w, l[0] + l[2]); x++)
                    if (city.tiles[y * city.w + x] == BUILDING || city.tiles[y * city.w + x] == LOT) city.tiles[y * city.w + x] = GRASS;
        }
    }

    /** (10.33) The road at tile (x, y) is one a drive can join: not the highway, nor right beside it. */
    boolean drivewayEnd(int x, int y) {
        return drivewayEnd(city.tiles[y * city.w + x]) && !gen.byHighway(x, y, 1);
    }

    boolean nextToBuilding(int i) {
        int x = i % city.w, y = i / city.w;
        for (int dy = -1; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx, ny = y + dy;
                if (nx >= 0 && ny >= 0 && nx < city.w && ny < city.h && city.tiles[ny * city.w + nx] == BUILDING) return true;
            }
        return false;
    }

    /**
     * A country lane doesn't turn into tarmac for a stretch and back to dirt (10.18): a bit of paved street
     * left out in the fields, cut off from the rest of the roads, with a dirt lane running into it from two
     * sides, becomes part of the lane (the lane's own line dirt, the rest grass).
     */
    void unpaveLaneStubs() {
        int n = city.w * city.h;
        int[] comp = new int[n];
        Arrays.fill(comp, -1);
        int[] q = new int[n];
        List<int[]> parts = new ArrayList<int[]>();
        int biggest = 0;
        for (int s0 = 0; s0 < n; s0++) {
            if (city.tiles[s0] != ROAD || comp[s0] >= 0) continue;
            int id = parts.size(), head = 0, tail = 0;
            q[tail++] = s0;
            comp[s0] = id;
            while (head < tail) {
                int c = q[head++], x = c % city.w, y = c / city.w;
                if (x > 0 && comp[c - 1] < 0 && city.tiles[c - 1] == ROAD) { comp[c - 1] = id; q[tail++] = c - 1; }
                if (x < city.w - 1 && comp[c + 1] < 0 && city.tiles[c + 1] == ROAD) { comp[c + 1] = id; q[tail++] = c + 1; }
                if (y > 0 && comp[c - city.w] < 0 && city.tiles[c - city.w] == ROAD) { comp[c - city.w] = id; q[tail++] = c - city.w; }
                if (y < city.h - 1 && comp[c + city.w] < 0 && city.tiles[c + city.w] == ROAD) { comp[c + city.w] = id; q[tail++] = c + city.w; }
            }
            parts.add(Arrays.copyOf(q, tail));
            biggest = Math.max(biggest, tail);
        }
        for (int id = 0; id < parts.size(); id++) {
            int[] part = parts.get(id);
            if (part.length >= biggest / 3 || part.length > 600) continue;
            boolean bridged = false;
            for (int c : part) if ((city.bridge != null && city.bridge[c]) || (gen.keepPaved != null && gen.keepPaved[c])) bridged = true;
            if (bridged) continue;
            // The dirt it touches, in separate places (one lane in is a hamlet's street; two is a lane running on).
            List<Integer> touch = new ArrayList<Integer>();
            for (int c : part) {
                int x = c % city.w, y = c / city.w;
                int[] nb = {x > 0 ? c - 1 : -1, x < city.w - 1 ? c + 1 : -1, y > 0 ? c - city.w : -1, y < city.h - 1 ? c + city.w : -1};
                for (int j : nb) if (j >= 0 && city.tiles[j] == DIRT && !touch.contains(j)) touch.add(j);
            }
            int groups = 0;
            int[] group = new int[touch.size()];
            Arrays.fill(group, -1);
            List<Integer> reps = new ArrayList<Integer>();
            for (int a = 0; a < touch.size(); a++) {
                if (group[a] >= 0) continue;
                int g = groups++;
                reps.add(touch.get(a));
                List<Integer> st = new ArrayList<Integer>();
                st.add(a);
                group[a] = g;
                while (!st.isEmpty()) {
                    int cur = st.remove(st.size() - 1), cx = touch.get(cur) % city.w, cy = touch.get(cur) / city.w;
                    for (int b = 0; b < touch.size(); b++) {
                        if (group[b] >= 0) continue;
                        int bx = touch.get(b) % city.w, by = touch.get(b) / city.w;
                        if (Math.abs(bx - cx) <= 2 && Math.abs(by - cy) <= 2) {
                            group[b] = g;
                            st.add(b);
                        }
                    }
                }
            }
            if (groups == 0) continue;
            if (groups < 2) {
                // A bit of town street a dirt lane runs into (10.23): it carries on as dirt rather than turning to
                // tarmac for a stretch, since it doesn't join the paved roads anyway.
                if (city.cfg.civic < 8) continue;
                boolean rail = false;
                for (int c : part) if (city.railY0 >= 0 && c / city.w >= city.railY0 - 1 && c / city.w <= city.railY0 + city.railRows) rail = true;
                if (rail) continue;
                for (int c : part) {
                    int x = c % city.w, y = c / city.w;
                    int[] nb = {x > 0 ? c - 1 : -1, x < city.w - 1 ? c + 1 : -1, y > 0 ? c - city.w : -1, y < city.h - 1 ? c + city.w : -1};
                    for (int j : nb) if (j >= 0 && city.tiles[j] == CAR) city.tiles[j] = DIRT;
                    city.tiles[c] = DIRT;
                    city.roadDir[c] = 0;
                    city.mainRoad[c] = false;
                    if (city.oneWay != null) city.oneWay[c] = 0;
                }
                continue;
            }
            // The paving goes back to grass, and the lane runs on through it, two tiles wide like the rest.
            for (int c : part) {
                city.tiles[c] = GRASS;
                city.roadDir[c] = 0;
                city.mainRoad[c] = false;
                if (city.oneWay != null) city.oneWay[c] = 0;
            }
            int r0 = reps.get(0);
            for (int g = 1; g < reps.size(); g++) {
                int x = r0 % city.w, y = r0 / city.w, tx = reps.get(g) % city.w, ty = reps.get(g) / city.w;
                for (int guard = 0; guard < city.w + city.h && (x != tx || y != ty); guard++) {
                    if (x != tx) x += Integer.signum(tx - x);
                    else y += Integer.signum(ty - y);
                    for (int j = y; j < y + 2; j++)
                        for (int i = x; i < x + 2; i++) {
                            if (i < 0 || j < 0 || i >= city.w || j >= city.h) continue;
                            if (city.tiles[j * city.w + i] == GRASS || city.tiles[j * city.w + i] == TREE) city.tiles[j * city.w + i] = DIRT;
                        }
                }
            }
        }
    }

    /**
     * (10.52) A lane that only touches another lane, a drive or a road corner to corner, with grass (or a tree)
     * between: the corner becomes lane, so the two join (nobody drives across a corner of grass).
     */
    void mendLanePinches() {
        for (int pass = 0; pass < 3; pass++) {
            boolean mended = false;
            for (int y = 1; y < city.h - 1; y++)
                for (int x = 1; x < city.w - 1; x++) {
                    if (city.tiles[y * city.w + x] != DIRT) continue;
                    for (int dx = -1; dx <= 1; dx += 2) {
                        int o = (y + 1) * city.w + x + dx, a = y * city.w + x + dx, b = (y + 1) * city.w + x;
                        if (!laneWay(city.tiles[o]) || laneWay(city.tiles[a]) || city.tiles[a] == CAR || laneWay(city.tiles[b]) || city.tiles[b] == CAR) continue;
                        int g = city.tiles[a] == GRASS || city.tiles[a] == TREE ? a : city.tiles[b] == GRASS || city.tiles[b] == TREE ? b : -1;
                        if (g < 0) continue;
                        city.tiles[g] = DIRT;
                        mended = true;
                    }
                }
            if (!mended) break;
        }
    }

    static boolean laneWay(byte t) {
        return t == DIRT || t == ROAD || t == LOT;
    }

    /**
     * A divided highway across the countryside from one edge of the map to the other, three lanes each way
     * with a concrete barrier between, and a road or two from town joining it at lights.
     */
    void highway(int bx0, int by0, int bx1, int by1) {
        boolean ew = city.railY0 < 0 && city.rnd.nextBoolean();
        // (Along the narrow side of town, clear of the wild country, if it can.)
        if (gen.wildSide >= 0) ew = city.railY0 < 0 && gen.wildSide >= 2;
        city.hwyAxis = ew ? 0 : 1;
        int lo = ew ? by0 : bx0, hi = ew ? by1 : bx1, len = ew ? city.h : city.w, len2 = ew ? city.w : city.h;
        boolean before = city.rnd.nextBoolean();
        if (gen.wildSide >= 0 && (gen.wildSide >= 2) == ew) before = gen.wildSide == 1 || gen.wildSide == 3;
        int at = before ? lo / 2 - 3 : hi + (len - hi) / 2 - 3;
        // (Since 10.29 well clear of town, with room for the slip roads in between.)
        if (city.cfg.civic >= 13) at = before ? Math.min(at, lo - 18) : Math.max(at, hi + 11);
        // (Since 10.32 with room the far side for the slip roads off the bridges, where there's room to spare.)
        if (city.cfg.civic >= 16) at = before ? Math.max(at, Math.min(8, lo - 12)) : Math.min(at, Math.max(len - 15, hi + 12));
        at = Math.max(3, Math.min(len - 10, at));
        city.hwyAt = at;
        String[] hn = city.country.highways[city.rnd.nextInt(city.country.highways.length)];
        city.hwyName = hn[0];
        city.hwyShield = hn[1];
        city.oneWay = new byte[city.w * city.h];
        for (int k = 0; k < len2; k++)
            for (int a = 0; a < 7; a++) {
                int x = ew ? k : at + a, y = ew ? at + a : k;
                int i = y * city.w + x;
                // (The railway crosses on a bridge: the highway carries on underneath, a pier in the middle.)
                city.tiles[i] = a == 3 ? FENCE : ROAD;
                city.roadDir[i] = (byte) (ew ? 2 : 1);
                city.mainRoad[i] = true;
                if (a == 3) continue;
                // Keeping right, the first carriageway (north, or west) carries traffic west (or south).
                boolean firstSide = a < 3, neg = firstSide;
                city.oneWay[i] = (byte) (ew ? (neg ? 2 : 1) : (neg ? 3 : 4));
            }
        // At each end, a way across the barrier (where it leaves the map), so nobody is stuck going one way.
        for (int k : new int[]{0, 1, len2 - 2, len2 - 1}) {
            int x = ew ? k : at + 3, y = ew ? at + 3 : k;
            city.tiles[y * city.w + x] = ROAD;
        }
        // Roads from town: carry a street on out to the highway where one ends facing it.
        // (side: the way from the town out to the highway; edge: the first row or column beside it, on the town side.)
        int side = before ? -1 : 1, edge = before ? at + 7 : at - 1;
        List<int[]> cands = new ArrayList<int[]>();
        int mid2 = ew ? (bx0 + bx1) / 2 : (by0 + by1) / 2;
        int runStart = -1, runD = 0;
        for (int k = 4; k < len2 - 4; k++) {
            // Walk from the highway towards town across open country; a street that carries straight on into
            // town from there is one to join up.
            int d = 0, x = ew ? k : edge, y = ew ? edge : k;
            while (d < len && x >= 0 && y >= 0 && x < city.w && y < city.h
                    && (city.tiles[y * city.w + x] == GRASS || city.tiles[y * city.w + x] == DIRT || city.tiles[y * city.w + x] == TREE)) {
                d++;
                x -= ew ? 0 : side;
                y -= ew ? side : 0;
            }
            boolean ok = d >= 2;
            for (int j = 0; j < 10 && ok; j++) {
                int ax = x - (ew ? 0 : side * j), ay = y - (ew ? side * j : 0);
                if (ax < 0 || ay < 0 || ax >= city.w || ay >= city.h || city.tiles[ay * city.w + ax] != ROAD) ok = false;
            }
            if (ok && runStart < 0) {
                runStart = k;
                runD = d;
            }
            if ((!ok || k == len2 - 5) && runStart >= 0) {
                int width = (ok ? k + 1 : k) - runStart;
                if (width >= 3 && width <= 6) cands.add(new int[]{runStart, width, runD, Math.abs(runStart + width / 2 - mid2)});
                runStart = -1;
            }
            if (ok) runD = Math.max(runD, d);
        }
        java.util.Collections.sort(cands, new java.util.Comparator<int[]>() {
            public int compare(int[] p, int[] q) {
                return p[3] - q[3];
            }
        });
        int made = 0;
        List<Integer> used = new ArrayList<Integer>();
        List<int[]> picked = new ArrayList<int[]>();
        for (int[] cnd : cands) {
            // ((11.0) More ways on and off: the town comes right up to it now.)
            if (made >= (city.cfg.civic >= 26 ? (len2 > 600 ? 5 : len2 > 300 ? 4 : 3) : len2 > 300 ? 3 : 2)) break;
            boolean near = false;
            for (int u : used) if (Math.abs(u - cnd[0]) < 40) near = true;
            if (near || cnd[1] > 6) continue;
            used.add(cnd[0]);
            made++;
            // (Since 10.29: an interchange, laid once the frontage road is in. See interchange.)
            if (city.cfg.civic >= 13) {
                picked.add(cnd);
                continue;
            }
            // The connecting road across the fields (a street of its own, with a name), and a gap in the
            // barrier so traffic can turn across.
            int r0 = Math.min(edge, edge - side * (cnd[2] - 1)), r1 = Math.max(edge, edge - side * (cnd[2] - 1)) + 1;
            gen.carve(ew ? new Street(cnd[0], r0, cnd[0] + cnd[1], r1, true, cnd[1] >= 5) : new Street(r0, cnd[0], r1, cnd[0] + cnd[1], false, cnd[1] >= 5));
            for (int q = 0; q < cnd[1]; q++) {
                int k = cnd[0] + q;
                int x = ew ? k : at + 3, y = ew ? at + 3 : k;
                city.tiles[y * city.w + x] = ROAD;
            }
            city.hwyExits.add(new int[]{cnd[0], cnd[1], side});
        }
        // Where town comes right up to the highway, a frontage road runs along beside it, and the streets that
        // stop just short of it carry on to meet it (instead of ending in the verge).
        int f0 = before ? at + 8 : at - 4;
        // (Not since 10.29: the slip roads join the streets from town instead.)
        boolean room = city.cfg.civic < 13 && f0 >= 0 && f0 + 3 <= len && (before ? lo - (f0 + 3) : f0 - hi) <= 6;
        for (int k = 0; k < len2 && room; k++)
            for (int a = 0; a < 3; a++) {
                int x = ew ? k : f0 + a, y = ew ? f0 + a : k;
                byte t = city.tiles[y * city.w + x];
                if (t != GRASS && t != TREE && t != DIRT && t != ROAD && t != RAIL) room = false;
            }
        if (room) {
            gen.carve(ew ? new Street(0, f0, city.w, f0 + 3, false, false) : new Street(f0, 0, f0 + 3, city.h, true, false));
            int step = before ? 1 : -1, from = before ? f0 + 3 : f0 - 1;
            for (int k = 0; k < len2; k++) {
                int x = ew ? k : from, y = ew ? from : k, cx = x, cy = y, d = 0;
                while (d < 7 && cx >= 0 && cy >= 0 && cx < city.w && cy < city.h
                        && (city.tiles[cy * city.w + cx] == GRASS || city.tiles[cy * city.w + cx] == TREE || city.tiles[cy * city.w + cx] == DIRT)) {
                    d++;
                    if (ew) cy += step;
                    else cx += step;
                }
                if (d == 0 || d >= 7 || !gen.isRoad(cx, cy) || city.roadDir[cy * city.w + cx] != (ew ? 1 : 2)) continue;
                for (int q = 0; q < d; q++) {
                    int gx = ew ? x : x + step * q, gy = ew ? y + step * q : y;
                    city.tiles[gy * city.w + gx] = ROAD;
                    city.roadDir[gy * city.w + gx] = (byte) (ew ? 1 : 2);
                }
            }
        }
        for (int[] cnd : picked) interchange(cnd, ew, at, side, edge, room ? f0 : -1);
    }

    /**
     * (10.29) Where a road from town meets the highway: an interchange, not a crossroads. The road stops short
     * of the highway (at the frontage road, if there is one) and two curving slip roads join the near
     * carriageway, the way off it before the junction and the way on after it. A gap in the median a little way
     * beyond each lets traffic turn round to and from the far carriageway. Nothing crosses the highway, and
     * there are no lights on it. Recorded in hwyExits as {k, width, side, where the way off leaves, flow}.
     */
    void interchange(int[] cnd, boolean ew, int at, int side, int edge, int f0) {
        int k0 = cnd[0], width = cnd[1], d = cnd[2], len2 = ew ? city.w : city.h;
        // The road out from town, over the fields, up to the frontage road or four tiles short of the verge.
        int stop = f0 >= 0 ? (side < 0 ? f0 + 3 : f0 - 1) : edge - side * 4;
        int far = edge - side * (d - 1), last = Integer.MIN_VALUE;
        for (int r = far; (r - stop) * side <= 0 && r >= 0 && r < (ew ? city.h : city.w); r += side) {
            boolean open = true;
            for (int q = 0; q < width && open; q++) {
                int x = ew ? k0 + q : r, y = ew ? r : k0 + q;
                if (city.tiles[y * city.w + x] == ROAD) open = false;
            }
            if (!open) break;
            last = r;
        }
        if (last != Integer.MIN_VALUE) {
            int r0 = Math.min(far, last), r1 = Math.max(far, last) + 1;
            gen.carve(ew ? new Street(k0, r0, k0 + width, r1, true, true) : new Street(r0, k0, r1, k0 + width, false, true));
        }
        float startA = f0 >= 0 ? f0 + 1.5f : (last != Integer.MIN_VALUE ? last : edge - side * d) + 0.5f;
        // The near carriageway, and which way its traffic flows (along the highway).
        float lane = side < 0 ? at + 6f : at + 1f;
        int la = side < 0 ? at + 5 : at + 1;
        int ow = city.oneWay[ew ? la * city.w : la];
        int flow = ow == 1 || ow == 3 ? 1 : -1;
        float kc = k0 + width / 2f, reach = 18;
        // (10.32) Where there's room the other side: a diamond interchange, the road crossing over the highway on a
        // bridge, with slip roads on and off both carriageways. Nothing crosses the highway on the level.
        int farRoom = side < 0 ? at : (ew ? city.h : city.w) - (at + 7);
        if (city.cfg.civic >= 16 && farRoom >= 7) {
            // The road on up to the highway, and over it.
            int nearEdge = side < 0 ? at + 7 : at - 1;
            for (int r = nearEdge; (r - nearEdge) * -side <= 6 && r >= 0 && r < (ew ? city.h : city.w); r -= side) {
                boolean open = true;
                for (int q = 0; q < width; q++) {
                    int x = ew ? k0 + q : r, y = ew ? r : k0 + q;
                    if (city.tiles[y * city.w + x] == ROAD) open = false;
                }
                if (!open) break;
                for (int q = 0; q < width; q++) {
                    int x = ew ? k0 + q : r, y = ew ? r : k0 + q;
                    city.tiles[y * city.w + x] = ROAD;
                    city.roadDir[y * city.w + x] = (byte) (ew ? 1 : 2);
                    city.mainRoad[y * city.w + x] = true;
                }
            }
            // (The highway runs on under it untouched: the bridge's two ends are linked over the top, for routes.)
            for (int q = 0; q < width; q++) {
                int[] ends = new int[2];
                for (int e = 0; e < 2; e++) {
                    int a = e == 0 ? -1 : 7;
                    int x = ew ? k0 + q : at + a, y = ew ? at + a : k0 + q;
                    ends[e] = x < 0 || y < 0 || x >= city.w || y >= city.h ? -1 : y * city.w + x;
                    if (ends[e] < 0) continue;
                    city.tiles[ends[e]] = ROAD;
                    city.solid[ends[e]] = false;
                    city.roadDir[ends[e]] = (byte) (ew ? 1 : 2);
                    city.mainRoad[ends[e]] = true;
                }
                if (ends[0] >= 0 && ends[1] >= 0) {
                    city.bridgeLink.put(ends[0], ends[1]);
                    city.bridgeLink.put(ends[1], ends[0]);
                }
            }
            // A short stub the far side, for the slip roads there.
            // (The slip roads there run in along the bridge's end itself: traffic off them turns straight onto it.)
            int s0 = side < 0 ? at - 3 : at + 8, s1 = side < 0 ? at - 1 : at + 10;
            gen.carve(ew ? new Street(k0, s0, k0 + width, s1, true, true) : new Street(s0, k0, s1, k0 + width, false, true));
            float farA = side < 0 ? at - 1.5f : at + 8.5f;
            city.overpasses.add(ew ? new float[]{k0 * T, (at - 1) * T, (k0 + width) * T, (at + 8) * T}
                    : new float[]{(at - 1) * T, k0 * T, (at + 8) * T, (k0 + width) * T});
            for (int r = 0; r < 2; r++) {
                int dir = r == 0 ? -flow : flow;
                float k1 = kc + dir * (width / 2f - 1.5f);
                slipRoad(ew, k1, startA, dir, dir == flow, side, at);
                // The far carriageway runs the other way: on after the bridge, off before it.
                slipRoad(ew, k1, farA, dir, dir == -flow, -side, at);
            }
            city.hwyExits.add(new int[]{k0, width, side, Math.round(kc - flow * SLIP_LEN), flow});
            return;
        }
        // (10.31) One-way slip roads that run in alongside the highway before they join it.
        if (city.cfg.civic >= 15) {
            for (int r = 0; r < 2; r++) {
                int dir = r == 0 ? -flow : flow;
                float k1 = kc + dir * (width / 2f - 1.5f);
                if (!slipRoad(ew, k1, startA, dir, dir == flow, side, at)) continue;
                int kx = Math.round(k1 + dir * (SLIP_LEN + 14));
                for (int q = -1; q <= 1; q++) {
                    int k = kx + q;
                    if (k < 3 || k > len2 - 4) continue;
                    int x = ew ? k : at + 3, y = ew ? at + 3 : k;
                    if (city.tiles[y * city.w + x] == FENCE) {
                        city.tiles[y * city.w + x] = ROAD;
                        city.roadDir[y * city.w + x] = (byte) (ew ? 2 : 1);
                    }
                }
            }
            city.hwyExits.add(new int[]{k0, width, side, Math.round(kc - flow * SLIP_LEN), flow});
            return;
        }
        for (int r = 0; r < 2; r++) {
            int dir = r == 0 ? -flow : flow;
            float k3 = kc + dir * reach;
            if (k3 < 6 || k3 > len2 - 7) continue;
            float k1 = kc + dir * (width / 2f - 1.5f);
            float[] pts = ew ? CityGenerator.bezier(k1, startA, k1, (startA + lane) / 2, k1 + dir * reach * 0.45f, lane, k3, lane, 30)
                    : CityGenerator.bezier(startA, k1, (startA + lane) / 2, k1, lane, k1 + dir * reach * 0.45f, lane, k3, 30);
            landGen.curveRoad(pts, false);
            // (The highway's own lanes keep running along it under the slip road.)
            for (int k = Math.max(0, (int) Math.min(k1, k3) - 2); k < Math.min(len2, (int) Math.max(k1, k3) + 3); k++)
                for (int a = 0; a < 7; a++) {
                    int x = ew ? k : at + a, y = ew ? at + a : k;
                    if (city.tiles[y * city.w + x] == ROAD) city.roadDir[y * city.w + x] = (byte) (ew ? 2 : 1);
                }
            // The gap in the median, a little way on from where the slip road joins.
            int kx = Math.round(kc + dir * (reach + 12));
            for (int q = -1; q <= 1; q++) {
                int k = kx + q;
                if (k < 3 || k > len2 - 4) continue;
                int x = ew ? k : at + 3, y = ew ? at + 3 : k;
                if (city.tiles[y * city.w + x] == FENCE) {
                    city.tiles[y * city.w + x] = ROAD;
                    city.roadDir[y * city.w + x] = (byte) (ew ? 2 : 1);
                }
            }
        }
        city.hwyExits.add(new int[]{k0, width, side, Math.round(kc - flow * reach), flow});
        slipZone(ew, kc - reach - 2, kc + reach + 2, side < 0 ? at + 7 : startA - 1, side < 0 ? startA + 1 : at - 1);
    }

    /**
     * (10.52) Out in the country, a four-lane beltway right round the map, a little in from its edge: its four
     * sides meet at the corners, the country lanes and the ring road's spokes join it where they cross, it goes
     * over lakes on bridges, crosses the railway on the level, and crosses the highway on a bridge. A side
     * that would run along beside the highway is the highway itself, the sides either side joining it.
     */
    void beltway() {
        final int rw = 5;
        int[] lim = {city.h, city.h, city.w, city.w};
        // Which sides are the highway itself (it runs within 24 tiles of that edge).
        boolean[] isHwy = new boolean[4];
        if (city.hwyAxis >= 0) {
            int s0 = city.hwyAxis == 0 ? 0 : 2;
            int len = city.hwyAxis == 0 ? city.h : city.w;
            if (city.hwyAt < 24) isHwy[s0] = true;
            else if (len - (city.hwyAt + 7) < 24) isHwy[s0 + 1] = true;
        }
        int[] at = {3, city.h - 3 - rw, 3, city.w - 3 - rw};
        boolean[] ok = new boolean[4];
        for (int s = 0; s < 4; s++) ok[s] = !isHwy[s];
        // Each side as far out as it can be, clear of anything in the way (twice round: a side's reach depends
        // on where the sides across its ends are).
        for (int pass = 0; pass < 2; pass++)
            for (int s = 0; s < 4; s++) {
                if (isHwy[s]) continue;
                ok[s] = false;
                for (int in = 3; in <= 18 && !ok[s]; in++) {
                    int a = s == 0 || s == 2 ? in : lim[s] - in - rw;
                    int[] span = beltSpan(s, at, isHwy, rw);
                    if (beltClear(s, a, span[0], span[1], rw)) {
                        at[s] = a;
                        ok[s] = true;
                    }
                }
            }
        int sides = 0;
        for (int s = 0; s < 4; s++) if (ok[s] || isHwy[s]) sides++;
        if (sides < 4) {
            // Not right round: no beltway (a road that ends out in the fields is no loop).
            return;
        }
        city.beltTile = new boolean[city.w * city.h];
        for (int s = 0; s < 4; s++) {
            if (!ok[s]) continue;
            boolean horiz = s < 2;
            int[] span = beltSpan(s, at, isHwy, rw);
            boolean crosses = city.hwyAxis == (horiz ? 1 : 0);
            for (int k = span[0]; k < span[1]; k++) {
                // (Over the highway on a bridge: its two ends linked over the top, the highway untouched.)
                if (crosses && k >= city.hwyAt - 1 && k <= city.hwyAt + 7) continue;
                for (int q = 0; q < rw; q++) {
                    int x = horiz ? k : at[s] + q, y = horiz ? at[s] + q : k;
                    beltPave(x, y, horiz);
                }
            }
            if (crosses && city.hwyAt - 1 >= span[0] && city.hwyAt + 7 < span[1]) {
                for (int q = 0; q < rw; q++) {
                    int[] ends = new int[2];
                    for (int e = 0; e < 2; e++) {
                        int k = e == 0 ? city.hwyAt - 1 : city.hwyAt + 7;
                        int x = horiz ? k : at[s] + q, y = horiz ? at[s] + q : k;
                        beltPave(x, y, horiz);
                        city.solid[y * city.w + x] = false;
                        ends[e] = y * city.w + x;
                    }
                    city.bridgeLink.put(ends[0], ends[1]);
                    city.bridgeLink.put(ends[1], ends[0]);
                }
                city.overpasses.add(horiz ? new float[]{(city.hwyAt - 1) * T, at[s] * T, (city.hwyAt + 8) * T, (at[s] + rw) * T}
                        : new float[]{at[s] * T, (city.hwyAt - 1) * T, (at[s] + rw) * T, (city.hwyAt + 8) * T});
            }
            gen.beltStreets.add(horiz ? new Street(span[0], at[s], span[1], at[s] + rw, false, true)
                    : new Street(at[s], span[0], at[s] + rw, span[1], true, true));
        }
    }

    /** Where a side of the beltway runs from and to: between the sides across its ends, or the highway. */
    int[] beltSpan(int s, int[] at, boolean[] isHwy, int rw) {
        boolean horiz = s < 2;
        int a = horiz ? 2 : 0, b = horiz ? 3 : 1;
        int from = isHwy[a] ? city.hwyAt + 7 : at[a], to = isHwy[b] ? city.hwyAt : at[b] + rw;
        return new int[]{from, to};
    }

    /** Whether a side of the beltway at a (its first row or column) can run from..to: nothing built in the way. */
    boolean beltClear(int s, int a, int from, int to, int rw) {
        boolean horiz = s < 2;
        if (to - from < 40) return false;
        boolean crosses = city.hwyAxis == (horiz ? 1 : 0);
        int wet = 0;
        for (int k = from; k < to; k++) {
            if (crosses && k >= city.hwyAt - 2 && k <= city.hwyAt + 8) continue;
            boolean water = false;
            for (int q = 0; q < rw; q++) {
                int x = horiz ? k : a + q, y = horiz ? a + q : k;
                if (x < 1 || y < 1 || x >= city.w - 1 || y >= city.h - 1) return false;
                int i = y * city.w + x;
                byte t = city.tiles[i];
                // (A street's pavement where the beltway crosses it is part of the crossing.)
                if (t == SIDEWALK) {
                    boolean street = false;
                    for (int e = -2; e <= 2 && !street; e++) {
                        int sx = horiz ? x + e : x, sy = horiz ? y : y + e;
                        if (e != 0 && sx >= 0 && sy >= 0 && sx < city.w && sy < city.h && city.tiles[sy * city.w + sx] == ROAD) street = true;
                    }
                    if (street) continue;
                }
                if (t == BUILDING || t == BASE || t == LOT || t == PLAZA || t == SIDEWALK || t == FENCE || t == CAR
                        || (city.slipTile != null && city.slipTile[i]) || (t == RAIL && horiz)) {
                    return false;
                }
                if (t == WATER) water = true;
            }
            wet = water ? wet + 1 : 0;
            // (Over a pond or the end of a lake on a bridge; not along one.)
            if (wet > 30) return false;
        }
        return true;
    }

    void beltPave(int x, int y, boolean horiz) {
        int i = y * city.w + x;
        if (city.tiles[i] == WATER) {
            if (city.bridge == null) city.bridge = new boolean[city.w * city.h];
            city.bridge[i] = true;
        }
        city.roadDir[i] = city.tiles[i] == RAIL ? 1 : city.roadDir[i] != 0 && city.tiles[i] == ROAD ? 3 : (byte) (horiz ? 2 : 1);
        city.tiles[i] = ROAD;
        city.mainRoad[i] = true;
        city.beltTile[i] = true;
    }

    /**
     * (10.52) On the water maps, where the town runs right to the edge of the map and the shore: the streets
     * round its edge (along the map's edge, the beach and the water) are its beltway, main roads driven fast,
     * with the bridges at the edge of the map (and the Islands' bridges between them) part of it.
     */
    void outerBelt() {
        city.beltTile = new boolean[city.w * city.h];
        for (Street st : city.streets) {
            int len = st.vertical ? st.y1 - st.y0 : st.x1 - st.x0;
            if (len < 12) continue;
            boolean outer = st.x0 <= 2 || st.y0 <= 2 || st.x1 >= city.w - 2 || st.y1 >= city.h - 2;
            for (int side = -1; side <= 1 && !outer; side += 2) {
                int open = 0;
                for (int f = 1; f <= 3; f++) {
                    int k = (st.vertical ? st.y0 : st.x0) + len * f / 4;
                    boolean clear = true;
                    for (int d = 1; d <= 8 && clear; d++) {
                        int x = st.vertical ? (side < 0 ? st.x0 - d : st.x1 - 1 + d) : k;
                        int y = st.vertical ? k : (side < 0 ? st.y0 - d : st.y1 - 1 + d);
                        if (x < 0 || y < 0 || x >= city.w || y >= city.h) break;
                        byte t = city.tiles[y * city.w + x];
                        if (t == SIDEWALK || t == BUILDING || t == LOT || t == PLAZA || t == ROAD || t == BASE) clear = false;
                    }
                    if (clear) open++;
                }
                if (open >= 2) outer = true;
            }
            if (!outer) continue;
            st.name = BELTWAY;
            for (int y = Math.max(0, st.y0); y < Math.min(city.h, st.y1); y++)
                for (int x = Math.max(0, st.x0); x < Math.min(city.w, st.x1); x++) {
                    int i = y * city.w + x;
                    if (city.tiles[i] != ROAD) continue;
                    city.beltTile[i] = true;
                    city.mainRoad[i] = true;
                }
        }
        if (city.bridge != null)
            for (int i = 0; i < city.w * city.h; i++) {
                if (!city.bridge[i] || city.tiles[i] != ROAD) continue;
                int x = i % city.w, y = i / city.w;
                if (city.waterKind == CityConfig.W_ISLANDS || x < 8 || y < 8 || x >= city.w - 8 || y >= city.h - 8) {
                    city.beltTile[i] = true;
                    city.mainRoad[i] = true;
                }
            }
    }

    /**
     * (10.31) A slip road from a road at (k0, a0) (along and across the highway) to the near carriageway, dir
     * along it: curving round to run in alongside the highway as an extra lane for a stretch, then merging into
     * the outside lane (an on-ramp; an off-ramp the same the other way round). One way along its length. False if
     * it would run off the map.
     */
    boolean slipRoad(boolean ew, float k0, float a0, int dir, boolean on, int side, int at) {
        int len2 = ew ? city.w : city.h;
        float kEnd = k0 + dir * SLIP_LEN;
        if (kEnd < 6 || kEnd > len2 - 7) return false;
        float aAux = side < 0 ? at + 7.7f : at - 0.7f, aOut = side < 0 ? at + 6.5f : at + 0.5f;
        float kA = k0 + dir * 12, kB = k0 + dir * (SLIP_LEN - 7);
        float[] bend = CityGenerator.bezier(k0, a0, k0, (a0 + aAux) / 2, k0 + dir * 6, aAux, kA, aAux, 24);
        List<Float> ka = new ArrayList<Float>();
        for (int i = 0; i < bend.length; i += 2) {
            ka.add(bend[i]);
            ka.add(bend[i + 1]);
        }
        for (int i = 1; i <= 6; i++) {
            ka.add(kA + (kB - kA) * i / 6f);
            ka.add(aAux);
        }
        for (int i = 1; i <= 4; i++) {
            ka.add(kB + (kEnd - kB) * i / 4f);
            ka.add(aAux + (aOut - aAux) * i / 4f);
        }
        // In the direction traffic goes: on from the road, or off the highway onto it.
        int n = ka.size() / 2;
        float[] pts = new float[n * 2];
        for (int i = 0; i < n; i++) {
            int j = on ? i : n - 1 - i;
            float k = ka.get(j * 2), a = ka.get(j * 2 + 1);
            pts[i * 2] = ew ? k : a;
            pts[i * 2 + 1] = ew ? a : k;
        }
        // (10.32) Roads already there (a frontage road it crosses) stay two way.
        int bx0 = Integer.MAX_VALUE, by0 = Integer.MAX_VALUE, bx1 = Integer.MIN_VALUE, by1 = Integer.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            bx0 = Math.min(bx0, (int) pts[i * 2] - 3);
            bx1 = Math.max(bx1, (int) pts[i * 2] + 3);
            by0 = Math.min(by0, (int) pts[i * 2 + 1] - 3);
            by1 = Math.max(by1, (int) pts[i * 2 + 1] + 3);
        }
        bx0 = Math.max(0, bx0);
        by0 = Math.max(0, by0);
        bx1 = Math.min(city.w - 1, bx1);
        by1 = Math.min(city.h - 1, by1);
        boolean[] was = new boolean[(bx1 - bx0 + 1) * (by1 - by0 + 1)];
        for (int y = by0; y <= by1; y++)
            for (int x = bx0; x <= bx1; x++)
                was[(y - by0) * (bx1 - bx0 + 1) + x - bx0] = city.tiles[y * city.w + x] == ROAD && (city.curveTile == null || !city.curveTile[y * city.w + x]);
        gen.curveOnGrass = true;
        landGen.curveRoad(pts, false);
        gen.curveOnGrass = false;
        // (The highway's own lanes keep running along it under the merge.)
        for (int k = (int) Math.min(k0, kEnd) - 2; k < (int) Math.max(k0, kEnd) + 3; k++)
            for (int a = 0; a < 7; a++) {
                int x = ew ? k : at + a, y = ew ? at + a : k;
                if (x >= 0 && y >= 0 && x < city.w && y < city.h && city.tiles[y * city.w + x] == ROAD) city.roadDir[y * city.w + x] = (byte) (ew ? 2 : 1);
            }
        markSlipWay(pts);
        for (int y = by0; y <= by1; y++)
            for (int x = bx0; x <= bx1; x++)
                if (was[(y - by0) * (bx1 - bx0 + 1) + x - bx0]) city.slipWay[y * city.w + x] = 0;
        slipZone(ew, Math.min(k0, kEnd) - 2, Math.max(k0, kEnd) + 2, side < 0 ? at + 7 : a0 - 1, side < 0 ? a0 + 1 : at - 1);
        return true;
    }

    /** One way along a slip road's centre line {x, y, ...} (in the way traffic goes), off the highway itself. */
    void markSlipWay(float[] pts) {
        if (city.slipWay == null) city.slipWay = new byte[city.w * city.h];
        int n = pts.length / 2;
        for (int k = 0; k < n - 1; k++) {
            float ax = pts[k * 2], ay = pts[k * 2 + 1], dx = pts[k * 2 + 2] - ax, dy = pts[k * 2 + 3] - ay;
            byte code = (byte) (Math.abs(dx) >= Math.abs(dy) ? (dx > 0 ? 1 : 2) : (dy > 0 ? 3 : 4));
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            int steps = Math.max(1, (int) (len * 3));
            for (int s = 0; s <= steps; s++) {
                float px = ax + dx * s / steps, py = ay + dy * s / steps;
                for (int y = City.fl(py - 1.6f); y <= City.fl(py + 1.6f); y++)
                    for (int x = City.fl(px - 1.6f); x <= City.fl(px + 1.6f); x++) {
                        if (x < 0 || y < 0 || x >= city.w || y >= city.h) continue;
                        float ox = x + 0.5f - px, oy = y + 0.5f - py;
                        if (ox * ox + oy * oy > 1.6f * 1.6f) continue;
                        int i = y * city.w + x;
                        if (city.tiles[i] != ROAD || (city.oneWay != null && city.oneWay[i] != 0) || city.curveTile == null || !city.curveTile[i]) continue;
                        byte had = city.slipWay[i];
                        if (city.slipAng == null) city.slipAng = new OffHeap.Bytes(city.w * city.h);
                        float ang = (float) Math.atan2(dy, dx);
                        // (Where two slip roads meet going different ways: both ways there.)
                        if (had == 0) {
                            city.slipWay[i] = code;
                            city.slipAng.set(i, (byte) (Math.round(ang * (256 / (Math.PI * 2)))));
                        } else if (had > 0 && Math.cos(ang - city.slipAng.get(i) * (Math.PI * 2 / 256)) < 0.3f) city.slipWay[i] = -1;
                    }
            }
        }
    }

    /** Marks the slip roads' area, from k0 to k1 along the highway and a0 to a1 across it. */
    void slipZone(boolean ew, float k0, float k1, float a0, float a1) {
        int ka = City.fl(Math.min(k0, k1)), kb = (int) Math.ceil(Math.max(k0, k1));
        int aa = City.fl(Math.min(a0, a1)), ab = (int) Math.ceil(Math.max(a0, a1));
        int[] z = ew ? new int[]{ka, aa, kb, ab} : new int[]{aa, ka, ab, kb};
        z[0] = Math.max(0, z[0]);
        z[1] = Math.max(0, z[1]);
        z[2] = Math.min(city.w - 1, z[2]);
        z[3] = Math.min(city.h - 1, z[3]);
        city.slipZones.add(z);
        if (city.slipTile == null) city.slipTile = new boolean[city.w * city.h];
        for (int y = z[1]; y <= z[3]; y++)
            for (int x = z[0]; x <= z[2]; x++)
                if (city.curveTile != null && city.curveTile[y * city.w + x]) city.slipTile[y * city.w + x] = true;
    }

    /**
     * (10.28) A ring road round the town out in the country: two lanes each way, a little way out from the edge
     * of town, joined to every street that reaches the edge, crossing the railway on the level, and joined to
     * the highway by curving slip roads on and off. A side that would run into the highway or off the map is
     * left out, so it wraps most of the town.
     */
    void ringRoad(int bx0, int by0, int bx1, int by1) {
        if (city.waterKind != CityConfig.W_NONE) return;
        // (A village is too small for one.)
        if (city.cfg.density() == 0 && city.cfg.style() == CityConfig.STYLE_HOUSES) return;
        int o = 5, rw = 5;
        int x0 = bx0 - o - rw, y0 = by0 - o - rw, x1 = bx1 + o, y1 = by1 + o;
        // The highway's band, to keep clear of (and which side of town it runs).
        int h0 = city.hwyAxis >= 0 ? city.hwyAt - 2 : -100, h1 = city.hwyAxis >= 0 ? city.hwyAt + 9 : -100;
        int[] at = {y0, y1, x0, x1};
        boolean[] ok = new boolean[4];
        for (int s = 0; s < 4; s++) {
            boolean horiz = s < 2;
            int a = at[s], lim = horiz ? city.h : city.w;
            ok[s] = a >= 3 && a + rw <= lim - 3;
            // (Open on the side facing the wild country, the hills and the park.)
            int[] wildOf = {2, 3, 0, 1};
            if (gen.wildSide >= 0 && wildOf[s] == gen.wildSide) ok[s] = false;
            if (ok[s] && city.hwyAxis == (horiz ? 0 : 1) && a + rw > h0 && a < h1) ok[s] = false;
        }
        // Where each side runs from and to: corner to corner, or up to the highway where a side is left out.
        int[] from = new int[4], to = new int[4];
        for (int s = 0; s < 4; s++) {
            boolean horiz = s < 2;
            int lo = horiz ? x0 : y0, hi = horiz ? x1 + rw : y1 + rw;
            int startSide = horiz ? 2 : 0, endSide = horiz ? 3 : 1;
            if (!ok[startSide]) lo = city.hwyAxis == (horiz ? 1 : 0) && h1 > 0 && h1 <= at[s == 0 || s == 1 ? 2 : 0] + rw + 2 ? h1 : Math.max(3, horiz ? x0 + rw : y0 + rw);
            if (!ok[endSide]) hi = city.hwyAxis == (horiz ? 1 : 0) && h0 >= at[s == 0 || s == 1 ? 3 : 1] - 2 ? h0 + 1 : Math.min((horiz ? city.w : city.h) - 3, horiz ? x1 : y1);
            from[s] = Math.max(3, lo);
            to[s] = Math.min((horiz ? city.w : city.h) - 3, hi);
            // Nothing in the way: no buildings, water, fences or a base.
            for (int k = from[s]; k < to[s] && ok[s]; k++)
                for (int q = 0; q < rw && ok[s]; q++) {
                    int x = horiz ? k : at[s] + q, y = horiz ? at[s] + q : k;
                    byte t = city.tiles[y * city.w + x];
                    if (t == BUILDING || t == WATER || t == BASE || t == LOT || t == PLAZA || t == SIDEWALK
                            || (t == FENCE && !(city.hwyAxis >= 0 && (horiz ? y : x) >= city.hwyAt && (horiz ? y : x) <= city.hwyAt + 6))) ok[s] = false;
                }
        }
        int sides = 0;
        for (boolean b : ok) if (b) sides++;
        if (sides < 2) return;
        for (int s = 0; s < 4; s++) {
            if (!ok[s] || to[s] - from[s] < 20) continue;
            boolean horiz = s < 2;
            Street st = horiz ? new Street(from[s], at[s], to[s], at[s] + rw, false, true) : new Street(at[s], from[s], at[s] + rw, to[s], true, true);
            gen.carve(st);
            gen.ringStreets.add(st);
            if (gen.ringTile == null) gen.ringTile = new boolean[city.w * city.h];
            for (int yy = st.y0; yy < st.y1; yy++)
                for (int xx = st.x0; xx < st.x1; xx++) gen.ringTile[yy * city.w + xx] = true;
            // Where it meets the highway (a side that runs up to it): a gap in the barrier to turn either way.
            if (city.hwyAxis == (horiz ? 1 : 0) && city.cfg.civic < 13)
                for (int k = 0; k < rw; k++) {
                    int c = horiz ? at[s] + k : at[s] + k;
                    int bx = horiz ? city.hwyAt + 3 : c, by = horiz ? c : city.hwyAt + 3;
                    if (horiz) { bx = city.hwyAt + 3; by = at[s] + k; } else { bx = at[s] + k; by = city.hwyAt + 3; }
                    if (bx >= 0 && by >= 0 && bx < city.w && by < city.h && city.tiles[by * city.w + bx] == FENCE
                            && (from[s] <= city.hwyAt + 7 && to[s] >= city.hwyAt - 1)) {
                        city.tiles[by * city.w + bx] = ROAD;
                        city.roadDir[by * city.w + bx] = 3;
                    }
                }
            // Joined to the streets of town that reach the edge facing it.
            int in = s == 0 || s == 2 ? 1 : -1, edge = s == 0 || s == 2 ? at[s] + rw : at[s] - 1;
            int last = -100;
            for (int k = from[s] + 4; k < to[s] - 4; k++) {
                int d = 0, x = horiz ? k : edge, y = horiz ? edge : k;
                while (d < o + 4 && x >= 0 && y >= 0 && x < city.w && y < city.h
                        && (city.tiles[y * city.w + x] == GRASS || city.tiles[y * city.w + x] == TREE || city.tiles[y * city.w + x] == DIRT)) {
                    d++;
                    if (horiz) y += in;
                    else x += in;
                }
                if (d == 0 || d >= o + 4 || k - last < 22) continue;
                boolean road = true;
                for (int j = 0; j < 6 && road; j++) {
                    int ax = horiz ? x : x + in * j, ay = horiz ? y + in * j : y;
                    int bx = horiz ? x + 2 : x + in * j, by = horiz ? y + in * j : y + 2;
                    if (!gen.isRoad(ax, ay) || !gen.isRoad(bx, by)) road = false;
                }
                if (!road) continue;
                last = k;
                int r0 = Math.min(edge, edge + in * (d - 1)), r1 = Math.max(edge, edge + in * (d - 1)) + 1;
                gen.carve(horiz ? new Street(k, r0, k + 3, r1, true, false) : new Street(r0, k, r1, k + 3, false, false));
            }
        }
        // On and off the highway: two curving slip roads from the side of the ring that faces it.
        if (city.hwyAxis < 0) return;
        int face = city.hwyAxis == 0 ? (city.hwyAt < y0 ? 0 : 1) : (city.hwyAt < x0 ? 2 : 3);
        if (!ok[face]) return;
        boolean horiz = face < 2;
        float ringEdge = face == 0 || face == 2 ? at[face] : at[face] + rw;
        boolean nearIsHigh = city.hwyAt < at[face];
        float lane = city.hwyAt + (nearIsHigh ? 5.5f : 1.5f);
        float span = to[face] - from[face];
        // (10.31) One way, running in alongside the highway (see slipRoad).
        if (city.cfg.civic >= 15) {
            int la = nearIsHigh ? city.hwyAt + 5 : city.hwyAt + 1;
            int ow = city.oneWay[horiz ? la * city.w : la];
            int flow = ow == 1 || ow == 3 ? 1 : -1;
            for (int r = 0; r < 2; r++) {
                float k = from[face] + span * (r == 0 ? 0.3f : 0.7f);
                int dir = r == 0 ? -1 : 1;
                if (!slipRoad(horiz, k, ringEdge, dir, dir == flow, nearIsHigh ? -1 : 1, city.hwyAt)) continue;
                // ((10.33) No gap in the median to turn round where there's a bridge to cross on instead.)
                if (city.cfg.civic >= 17 && !city.overpasses.isEmpty()) continue;
                int kg = Math.round(k + dir * (SLIP_LEN + 14));
                for (int q = -1; q <= 1; q++) {
                    int bx = horiz ? kg + q : city.hwyAt + 3, by = horiz ? city.hwyAt + 3 : kg + q;
                    if (bx >= 3 && by >= 3 && bx < city.w - 3 && by < city.h - 3 && city.tiles[by * city.w + bx] == FENCE) {
                        city.tiles[by * city.w + bx] = ROAD;
                        city.roadDir[by * city.w + bx] = (byte) (horiz ? 2 : 1);
                    }
                }
            }
            return;
        }
        for (int r = 0; r < 2; r++) {
            float k = from[face] + span * (r == 0 ? 0.3f : 0.7f), dir = r == 0 ? -1 : 1;
            float k2 = k + dir * 16;
            float mid = (ringEdge + lane) / 2;
            float[] pts = horiz ? CityGenerator.bezier(k, ringEdge, k, mid, k2, mid, k2, lane, 30) : CityGenerator.bezier(ringEdge, k, mid, k, mid, k2, lane, k2, 30);
            // (Since 10.29 it runs into the highway alongside it, merging, rather than straight at it.)
            if (city.cfg.civic >= 13) {
                float ln = nearIsHigh ? city.hwyAt + 6f : city.hwyAt + 1f;
                k2 = k + dir * 20;
                pts = horiz ? CityGenerator.bezier(k, ringEdge, k, (ringEdge + ln) / 2, k + dir * 9, ln, k2, ln, 30)
                        : CityGenerator.bezier(ringEdge, k, (ringEdge + ln) / 2, k, ln, k + dir * 9, ln, k2, 30);
            }
            landGen.curveRoad(pts, false);
            if (city.cfg.civic >= 13) slipZone(horiz, Math.min(k, k2) - 2, Math.max(k, k2) + 2, ringEdge, nearIsHigh ? city.hwyAt + 7 : city.hwyAt - 1);
            // And a gap in the barrier there, to reach the far carriageway. (Since 10.29 a little way on from the
            // slip road, to turn round in, rather than straight across where it joins.)
            int kg = Math.round(k2 + (city.cfg.civic >= 13 ? dir * 12 : 0));
            if (city.cfg.civic >= 13)
                for (int kk = Math.round(Math.min(k, k2)) - 2; kk <= Math.round(Math.max(k, k2)) + 2; kk++)
                    for (int a = 0; a < 7; a++) {
                        int x = horiz ? kk : city.hwyAt + a, y = horiz ? city.hwyAt + a : kk;
                        if (x >= 0 && y >= 0 && x < city.w && y < city.h && city.tiles[y * city.w + x] == ROAD) city.roadDir[y * city.w + x] = (byte) (horiz ? 2 : 1);
                    }
            for (int q = -1; q <= 1; q++) {
                int bx = horiz ? kg + q : city.hwyAt + 3, by = horiz ? city.hwyAt + 3 : kg + q;
                if (bx >= 0 && by >= 0 && bx < city.w && by < city.h && city.tiles[by * city.w + bx] == FENCE) {
                    city.tiles[by * city.w + bx] = ROAD;
                    city.roadDir[by * city.w + bx] = 3;
                }
            }
        }
    }

    /**
     * (10.28) Every police and fire station (the small towns' out in the country too) on the road network: one
     * that can't be reached from town gets a lane laid from it to the nearest road that can, round buildings,
     * water and fences.
     */
    void connectStations() {
        int n = city.w * city.h;
        int[] field = new int[n];
        float[] c0 = city.nearestDrivable((city.townX0 + city.townX1) / 2f * T, (city.townY0 + city.townY1) / 2f * T);
        if (c0 == null) {
            c0 = null;
            for (int i = 0; i < n && c0 == null; i++) if (city.tiles[i] == ROAD) c0 = new float[]{(i % city.w + 0.5f) * T, (i / city.w + 0.5f) * T};
            if (c0 == null) return;
        }
        int[] prev = new int[n], queue = new int[n];
        // (Plain flood over the drivable tiles: the route tables aren't built yet.) ((16.2) Once, then carried on from
        // the new lanes laid to each station, rather than all over again for every station: the same tiles are
        // reached, and the map comes out the same, in a fraction of the time.)
        Arrays.fill(field, FAR);
        int st0 = city.tileIndex(c0[0], c0[1]), qt = 0;
        field[st0] = 0;
        queue[qt++] = st0;
        floodDrive(field, queue, qt);
        java.util.ArrayList<Integer> added = new java.util.ArrayList<Integer>();
        for (Facility f : city.facilities) {
            if (f.kind != FACILITY_POLICE && f.kind != FACILITY_FIRE) continue;
            if (added.size() > 0) {
                qt = 0;
                for (int a2 = 0; a2 < added.size(); a2++) {
                    int j = added.get(a2), x = j % city.w, y = j / city.w;
                    if (field[j] < FAR || city.driveCost(j) < 0) continue;
                    boolean touches = (x > 0 && field[j - 1] < FAR) || (x < city.w - 1 && field[j + 1] < FAR) || (y > 0 && field[j - city.w] < FAR) || (y < city.h - 1 && field[j + city.w] < FAR);
                    if (!touches) continue;
                    field[j] = 1;
                    queue[qt++] = j;
                }
                floodDrive(field, queue, qt);
                added.clear();
            }
            float[] p = city.nearestDrivable(f.gateX > 0 ? f.gateX : f.x, f.gateY > 0 ? f.gateY : f.y);
            if (p == null) p = new float[]{f.x, f.y};
            int s0 = city.tileIndex(p[0], p[1]);
            if (s0 < 0 || field[s0] < FAR) continue;
            Arrays.fill(prev, -2);
            int head = 0, tail = 0, goal = -1;
            queue[tail++] = s0;
            prev[s0] = -1;
            while (head < tail && goal < 0) {
                int c = queue[head++], x = c % city.w, y = c / city.w;
                for (int k = 0; k < 4; k++) {
                    int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = y + (k == 2 ? 1 : k == 3 ? -1 : 0);
                    if (nx < 1 || ny < 1 || nx >= city.w - 1 || ny >= city.h - 1) continue;
                    int j = ny * city.w + nx;
                    if (prev[j] != -2) continue;
                    byte t = city.tiles[j];
                    if (t == BUILDING || t == WATER || t == FENCE || t == BASE || t == RAIL || t == ROCK || t == PIER || city.solid[j] && t != TREE) continue;
                    prev[j] = c;
                    if (field[j] < FAR) {
                        goal = j;
                        break;
                    }
                    queue[tail++] = j;
                }
            }
            if (goal < 0) continue;
            for (int c = prev[goal]; c >= 0; c = prev[c]) {
                int x = c % city.w, y = c / city.w;
                for (int dy = 0; dy <= 1; dy++)
                    for (int dx = 0; dx <= 1; dx++) {
                        int j = (y + dy) * city.w + x + dx;
                        if (x + dx >= city.w || y + dy >= city.h) continue;
                        byte t = city.tiles[j];
                        if (t == GRASS || t == TREE || t == SAND || t == TRAIL) {
                            city.tiles[j] = DIRT;
                            city.solid[j] = false;
                            added.add(j);
                        }
                    }
            }
        }
    }

    /** (16.2) The flood over drivable tiles carried on from the queue's first qt tiles (already marked in field). */
    void floodDrive(int[] field, int[] queue, int qt) {
        int qh = 0;
        while (qh < qt) {
            int c = queue[qh++], x = c % city.w, y = c / city.w;
            for (int k = 0; k < 4; k++) {
                int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = y + (k == 2 ? 1 : k == 3 ? -1 : 0);
                if (nx < 0 || ny < 0 || nx >= city.w || ny >= city.h) continue;
                int j = ny * city.w + nx;
                if (field[j] < FAR || city.driveCost(j) < 0) continue;
                field[j] = field[c] + 1;
                queue[qt++] = j;
            }
        }
    }

    /**
     * Streets that stop dead in a field look like a mistake. A stub running on past the last houses is dug up
     * (back to the junction, if that was all it was), and a street that still ends with nothing beyond it
     * carries on as a dirt lane to the nearest other road or lane, or out to the edge of the map.
     */
    void tidyDeadEnds() {
        boolean[] dug = new boolean[city.w * city.h];
        for (int[] e : deadEnds()) gen.trimStub(e, dug);
        fixJunctions(dug);
        for (int[] e : deadEnds()) laneOn(e);
    }

    boolean dirtAt(int x, int y) {
        return x >= 0 && y >= 0 && x < city.w && y < city.h && city.tiles[y * city.w + x] == DIRT;
    }

    /** A junction that lost a street to trimStub is a plain bend or a straight run if that's all it now is. */
    void fixJunctions(boolean[] dug) {
        boolean[] seen = new boolean[city.w * city.h];
        int[] q = new int[city.w * city.h];
        for (int s = 0; s < city.w * city.h; s++) {
            if (seen[s] || city.tiles[s] != ROAD || city.roadDir[s] != 3) continue;
            int head = 0, tail = 0;
            q[tail++] = s;
            seen[s] = true;
            boolean touched = false, ns = false, ew = false;
            while (head < tail) {
                int i = q[head++], x = i % city.w, y = i / city.w;
                for (int k = 0; k < 4; k++) {
                    int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = y + (k == 2 ? 1 : k == 3 ? -1 : 0);
                    if (nx < 0 || ny < 0 || nx >= city.w || ny >= city.h) continue;
                    int j = ny * city.w + nx;
                    if (dug[j]) touched = true;
                    if (city.tiles[j] != ROAD) continue;
                    if (city.roadDir[j] == 3) {
                        if (!seen[j]) {
                            seen[j] = true;
                            q[tail++] = j;
                        }
                    } else if (city.roadDir[j] == 1 && k >= 2) ns = true;
                    else if (city.roadDir[j] == 2 && k < 2) ew = true;
                }
            }
            if (!touched || (ns && ew) || (!ns && !ew)) continue;
            for (int k = 0; k < tail; k++) city.roadDir[q[k]] = (byte) (ns ? 1 : 2);
        }
    }

    /**
     * Carries a dead end on as a two-tile dirt lane, by the shortest (and straightest) way across open ground
     * to a road or lane well away from it along the roads, or to the edge of the map.
     */
    void laneOn(int[] e) {
        int x = e[0], y = e[1], n = e[2], dx = e[3], dy = e[4];
        int px = dx == 0 ? 1 : 0, py = 1 - px;
        // How far every road and lane is from this end, going by road.
        int[] far = new int[city.w * city.h];
        Arrays.fill(far, Integer.MAX_VALUE);
        int[] q = new int[city.w * city.h];
        int head = 0, tail = 0;
        for (int k = 0; k < n; k++) {
            int i = (y + py * k) * city.w + x + px * k;
            far[i] = 0;
            q[tail++] = i;
        }
        while (head < tail) {
            int i = q[head++], cx = i % city.w, cy = i / city.w;
            if (far[i] > 600) continue;
            for (int k = 0; k < 4; k++) {
                int nx = cx + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = cy + (k == 2 ? 1 : k == 3 ? -1 : 0);
                if (nx < 0 || ny < 0 || nx >= city.w || ny >= city.h) continue;
                int j = ny * city.w + nx;
                if (far[j] != Integer.MAX_VALUE || (city.tiles[j] != ROAD && city.tiles[j] != DIRT)) continue;
                far[j] = far[i] + 1;
                q[tail++] = j;
            }
        }
        // The lane's 2x2 footprint, by its top-left tile; it starts just beyond the middle of the end.
        int a = Math.max(0, n / 2 - 1);
        int sx = x + px * a + (dx > 0 ? 1 : dx < 0 ? -2 : 0), sy = y + py * a + (dy > 0 ? 1 : dy < 0 ? -2 : 0);
        if (!laneFits(sx, sy)) return;
        // Dijkstra over (tile, heading): a step costs 2, a turn 5 more, so lanes run fairly straight.
        int[] cost = new int[city.w * city.h * 4], from = new int[city.w * city.h * 4];
        Arrays.fill(cost, Integer.MAX_VALUE);
        int startDir = dx > 0 ? 0 : dx < 0 ? 1 : dy > 0 ? 2 : 3;
        java.util.PriorityQueue<long[]> pq = new java.util.PriorityQueue<long[]>(64, new java.util.Comparator<long[]>() {
            public int compare(long[] p, long[] r) {
                return Long.compare(p[0], r[0]);
            }
        });
        int s0 = (sy * city.w + sx) * 4 + startDir;
        cost[s0] = 0;
        from[s0] = -1;
        pq.add(new long[]{0, s0});
        int goal = -1;
        while (!pq.isEmpty()) {
            long[] top = pq.poll();
            int st = (int) top[1], c = (int) top[0];
            if (c != cost[st]) continue;
            int cell = st / 4, dir = st % 4, cx = cell % city.w, cy = cell / city.w;
            if (laneArrives(cx, cy, far, c / 2)) {
                goal = st;
                break;
            }
            if (c > 300) break;
            for (int k = 0; k < 4; k++) {
                if ((k ^ 1) == dir) continue;
                int nx = cx + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = cy + (k == 2 ? 1 : k == 3 ? -1 : 0);
                if (!laneFits(nx, ny)) continue;
                int ns = (ny * city.w + nx) * 4 + k, nc = c + 2 + (k != dir ? 5 : 0);
                if (nc >= cost[ns]) continue;
                cost[ns] = nc;
                from[ns] = st;
                pq.add(new long[]{nc, ns});
            }
        }
        if (goal < 0) return;
        for (int st = goal; st >= 0; st = from[st]) {
            int cell = st / 4, cx = cell % city.w, cy = cell / city.w;
            for (int j = cy; j < cy + 2; j++)
                for (int i = cx; i < cx + 2; i++)
                    if (gen.open(i, j)) city.tiles[j * city.w + i] = DIRT;
        }
    }

    /** Open ground for a lane's 2x2 footprint, not up against the highway or the railway. */
    boolean laneFits(int x, int y) {
        if (x < 0 || y < 0 || x + 1 >= city.w || y + 1 >= city.h) return false;
        for (int j = y - 1; j <= y + 2; j++)
            for (int i = x - 1; i <= x + 2; i++) {
                boolean inside = i >= x && i <= x + 1 && j >= y && j <= y + 1;
                if (inside && !gen.open(i, j)) return false;
                if (i < 0 || j < 0 || i >= city.w || j >= city.h) continue;
                if (city.tiles[j * city.w + i] == RAIL || (city.oneWay != null && city.oneWay[j * city.w + i] != 0)) return false;
            }
        return true;
    }

    /** Whether a lane's footprint at (x, y), len tiles out, has reached the map edge or a road worth joining. */
    boolean laneArrives(int x, int y, int[] far, int len) {
        if ((x == 0 || y == 0 || x + 2 >= city.w || y + 2 >= city.h) && len < 90) return true;
        for (int j = y - 1; j <= y + 2; j++)
            for (int i = x - 1; i <= x + 2; i++) {
                if ((i == x - 1 || i == x + 2) && (j == y - 1 || j == y + 2)) continue;
                if (i < 0 || j < 0 || i >= city.w || j >= city.h) continue;
                int f = far[j * city.w + i];
                if ((city.tiles[j * city.w + i] == ROAD || city.tiles[j * city.w + i] == DIRT) && (f == Integer.MAX_VALUE || f > len * 2 + 30))
                    return true;
            }
        return false;
    }

    /**
     * Road ends with nowhere to go: {x, y, width, dx, dy} for the last row of a street (its first tile, how
     * many tiles across) that stops facing (dx, dy) into open fields.
     */
    List<int[]> deadEnds() {
        List<int[]> out = new ArrayList<int[]>();
        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        for (int[] d : dirs) {
            int px = d[0] == 0 ? 1 : 0, py = 1 - px;
            for (int y = 0; y < city.h; y++)
                for (int x = 0; x < city.w; x++) {
                    if (!endTile(x, y, d) || endTile(x - px, y - py, d)) continue;
                    int n = 0;
                    while (endTile(x + px * n, y + py * n, d)) n++;
                    // (A one-tile gap is a median or a planter, not the end of the road.)
                    boolean dead = n >= 2;
                    // A street turning the corner, or ending at a side street, isn't a dead end.
                    for (int b = 0; b < n + 1 && dead; b++) {
                        int rx = x - d[0] * b, ry = y - d[1] * b;
                        if (gen.isRoad(rx - px, ry - py) || gen.isRoad(rx + px * n, ry + py * n)
                                || dirtAt(rx - px, ry - py) || dirtAt(rx + px * n, ry + py * n)) dead = false;
                    }
                    for (int k = 0; k < n && dead; k++)
                        for (int s = 1; s <= 3 && dead; s++) {
                            int bx = x + px * k + d[0] * s, by = y + py * k + d[1] * s;
                            if (bx < 0 || by < 0 || bx >= city.w || by >= city.h) dead = s > 1;
                            else if (city.tiles[by * city.w + bx] != GRASS && city.tiles[by * city.w + bx] != TREE) dead = false;
                        }
                    if (dead) out.add(new int[]{x, y, n, d[0], d[1]});
                }
        }
        return out;
    }

    /** A tile on the last row of a street running towards (d[0], d[1]), with no road beyond it. */
    boolean endTile(int x, int y, int[] d) {
        if (x < 0 || y < 0 || x >= city.w || y >= city.h) return false;
        int i = y * city.w + x;
        if (city.tiles[i] != ROAD || city.roadDir[i] != (d[0] == 0 ? 1 : 2)) return false;
        int bx = x + d[0], by = y + d[1];
        return bx < 0 || by < 0 || bx >= city.w || by >= city.h || city.tiles[by * city.w + bx] != ROAD;
    }

    /**
     * Four-way junctions sometimes become roundabouts: a round island in the middle that traffic drives around.
     * Only junctions with a road coming in on every side.
     */
    void roundabouts() {
        for (int y = 5; y < city.h - 10; y++)
            for (int x = 5; x < city.w - 10; x++)
                for (int sz = 3; sz <= 5; sz += 2) {
                    if (city.railY0 >= 0 && y + sz >= city.railY0 - 3 && y <= city.railY0 + city.railRows + 3) continue;
                    boolean ok = arm(x - 1, y, sz, false) && arm(x + sz, y, sz, false) && arm(x, y - 1, sz, true)
                            && arm(x, y + sz, sz, true);
                    for (int j = 0; j < sz && ok; j++)
                        for (int k = 0; k < sz && ok; k++) if (city.tiles[(y + j) * city.w + x + k] != ROAD) ok = false;
                    if (!ok || city.rnd.nextFloat() > 0.4f) continue;
                    int island = sz == 3 ? 1 : 3, o = (sz - island) / 2;
                    for (int j = 0; j < island; j++)
                        for (int k = 0; k < island; k++) {
                            city.tiles[(y + o + j) * city.w + x + o + k] = GRASS;
                            city.roadDir[(y + o + j) * city.w + x + o + k] = 0;
                        }
                    for (int j = 0; j < sz; j++)
                        for (int k = 0; k < sz; k++) if (city.roadDir[(y + j) * city.w + x + k] != 0) city.roadDir[(y + j) * city.w + x + k] = 3;
                    city.addDecor(D_ROUNDABOUT, x * T, y * T, (x + sz) * T, (y + sz) * T, island);
                }
    }

    /** A road exactly sz tiles wide leading away at (x, y): vertical (running north-south) or horizontal. */
    boolean arm(int x, int y, int sz, boolean vertical) {
        for (int k = 0; k < sz; k++) {
            int i = vertical ? x + k : x, j = vertical ? y : y + k;
            if (city.roadDir[j * city.w + i] != (vertical ? 1 : 2)) return false;
        }
        int bi = vertical ? x - 1 : x, bj = vertical ? y : y - 1, ai = vertical ? x + sz : x, aj = vertical ? y : y + sz;
        return city.roadDir[bj * city.w + bi] == 0 && city.roadDir[aj * city.w + ai] == 0;
    }

    /**
     * Makes sure the town isn't cut in two by the railway: streets that run up to the line from one side are
     * carried across it (cutting through the block opposite) until there's a level crossing every so often.
     */
    void ensureCrossings(int bx0, int bx1, int railY0, int railRows) {
        List<Integer> xs = new ArrayList<Integer>();
        for (int x = 0; x < city.w; x++)
            if (city.tiles[railY0 * city.w + x] == ROAD && (x == 0 || city.tiles[railY0 * city.w + x - 1] != ROAD)) xs.add(x);
        int want = Math.max(2, (bx1 - bx0) / 26);
        List<Street> cands = new ArrayList<Street>();
        for (Street st : city.streets) if (st.vertical && (st.y1 == railY0 || st.y0 == railY0 + railRows)) cands.add(st);
        java.util.Collections.shuffle(cands, city.rnd);
        for (Street st : cands) {
            if (xs.size() >= want) break;
            boolean close = false;
            for (int x : xs) if (Math.abs(x - st.x0) < 14) close = true;
            if (close) continue;
            boolean fromAbove = st.y1 == railY0;
            int dy = fromAbove ? 1 : -1, y = fromAbove ? railY0 + railRows : railY0 - 1;
            // Carry on across the block beyond until meeting a road.
            while (y > 0 && y < city.h - 1) {
                boolean road = false;
                for (int x = st.x0; x < st.x1; x++) if (city.tiles[y * city.w + x] == ROAD) road = true;
                if (road) break;
                y += dy;
            }
            if (y <= 0 || y >= city.h - 1) continue;
            int lo = fromAbove ? railY0 : y, hi = fromAbove ? y + 1 : railY0 + railRows;
            boolean estate = false;
            for (int[] b : city.blocks)
                if (b.length > 4 && b[4] == 3 && b[2] > st.x0 && b[0] < st.x1 && b[3] > lo && b[1] < hi) estate = true;
            if (estate) continue;
            gen.carve(new Street(st.x0, lo, st.x1, hi, true, false));
            for (int ry = railY0; ry < railY0 + railRows; ry++)
                for (int x = st.x0; x < st.x1; x++) city.roadDir[ry * city.w + x] = 1;
            // The block it cut through becomes two.
            for (int k = city.blocks.size() - 1; k >= 0; k--) {
                int[] b = city.blocks.get(k);
                if (b[2] <= st.x0 || b[0] >= st.x1 || b[3] <= lo || b[1] >= hi) continue;
                city.blocks.remove(k);
                if (st.x0 - b[0] >= 3) city.blocks.add(new int[]{b[0], b[1], st.x0, b[3]});
                if (b[2] - st.x1 >= 3) city.blocks.add(new int[]{st.x1, b[1], b[2], b[3]});
            }
            xs.add(st.x0);
        }
    }

    /**
     * The highway is only joined at its proper exits: a dirt track or a bit of lot running right up to the
     * edge of a carriageway (where cars would cut on and off the highway, and end up going round in
     * circles trying to) is grassed over.
     */
    void sealHighway() {
        city.sealed = true;
        if (city.hwyAxis < 0 || city.oneWay == null) return;
        boolean ew = city.hwyAxis == 0;
        int len = ew ? city.w : city.h;
        for (int k = 0; k < len; k++)
            for (int a : new int[]{-1, 7}) {
                int x = ew ? k : city.hwyAt + a, y = ew ? city.hwyAt + a : k;
                if (x < 0 || y < 0 || x >= city.w || y >= city.h) continue;
                int i = y * city.w + x;
                if (city.tiles[i] != DIRT && city.tiles[i] != LOT) continue;
                int hx = ew ? x : city.hwyAt + (a < 0 ? 0 : 6), hy = ew ? city.hwyAt + (a < 0 ? 0 : 6) : y;
                if (city.oneWay[hy * city.w + hx] == 0) continue;
                city.tiles[i] = GRASS;
                city.solid[i] = false;
            }
    }
}
