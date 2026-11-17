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
 * (27.3) The country round the town: neighbourhoods on winding roads, villages and hamlets, farms and the lanes to
 * them. Taken out of City.java as it stood; the city itself (its tiles, buildings and everything the game asks of
 * it) is `city`.
 */
final class CityOutskirts extends CityPart {
    CityOutskirts(City city) {
        super(city);
    }

    void keepPaved(Street st) {
        if (gen.keepPaved == null) gen.keepPaved = new boolean[city.w * city.h];
        for (int y = Math.max(0, st.y0); y < Math.min(city.h, st.y1); y++)
            for (int x = Math.max(0, st.x0); x < Math.min(city.w, st.x1); x++) gen.keepPaved[y * city.w + x] = true;
    }

    /**
     * (10.30) A paved road from (x, y), just off the end of a town's street, across open country to the nearest
     * road that isn't one of the town's own. False (and nothing laid) if there's no way, or it's very long.
     */
    boolean pavedLink(int x, int y, int[] box) {
        if (x < 2 || y < 2 || x >= city.w - 2 || y >= city.h - 2) return false;
        int[] prev = new int[city.w * city.h];
        // (Steering clear of the town itself: {x0, y0, x1, y1}, x1/y1 exclusive.)
        int end = gen.roadSearch(y * city.w + x, box[0] + 1, box[1] + 1, box[2] - box[0] - 2, box[3] - box[1] - 2, prev);
        if (end < 0) return false;
        List<Integer> path = new ArrayList<Integer>();
        for (int c = end; c != -1; c = prev[c]) path.add(0, c);
        if (path.size() > 220) return false;
        List<Float> pts = new ArrayList<Float>();
        for (int k = 0; k < path.size() - 1; k += 4) {
            pts.add(path.get(k) % city.w + 0.5f);
            pts.add(path.get(k) / city.w + 0.5f);
        }
        pts.add(end % city.w + 0.5f);
        pts.add(end / city.w + 0.5f);
        float[] line = new float[pts.size()];
        for (int k = 0; k < line.length; k++) line[k] = pts.get(k);
        for (int it = 0; it < 2; it++) line = CityGenerator.chaikin(line);
        gen.curveOnGrass = true;
        curveRoad(line, false);
        gen.curveOnGrass = false;
        return true;
    }

    /**
     * Open country around the town: dirt roads winding out from the ring road to the edge of the map, lanes off
     * them to farms and cabins, and woods.
     */
    void countryside(int bx0, int by0, int bx1, int by1) {
        int[][] outs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        List<int[]> lane = new ArrayList<int[]>();
        // A village's lanes carry on from the ends of its streets.
        for (int[] e : villageEnds) dirtRoad(e[0], e[1], e[2], e[3], lane);
        for (int[] o : villageEnds.isEmpty() ? outs : new int[0][]) {
            int roads = 1 + (city.w > 360 ? 1 : 0) + (city.rnd.nextFloat() < 0.4f ? 1 : 0);
            // (Just the one lane out into the wild side.)
            int wideSide = o[0] < 0 ? 0 : o[0] > 0 ? 1 : o[1] < 0 ? 2 : 3;
            if (wideSide == gen.wildSide) roads = 1;
            for (int k = 0; k < roads; k++) {
                // Start on the ring road, somewhere along this side (clear of the railway).
                int x, y;
                if (o[0] == 0) {
                    x = bx0 + 6 + city.rnd.nextInt(Math.max(1, bx1 - bx0 - 12));
                    y = o[1] < 0 ? by0 : by1 - 1;
                } else {
                    x = o[0] < 0 ? bx0 : bx1 - 1;
                    y = by0 + 6 + city.rnd.nextInt(Math.max(1, by1 - by0 - 12));
                    if (city.railY0 >= 0 && Math.abs(y - city.railY0) < 6) y = city.railY0 - 6;
                }
                dirtRoad(x, y, o[0], o[1], lane);
            }
        }
        // On big maps, side tracks branch off the lanes out to the remote corners.
        int branches = city.w > 360 ? 4 + city.rnd.nextInt(3) : city.w > 240 ? 1 + city.rnd.nextInt(2) : 0;
        int mains = lane.size();
        for (int k = 0; k < branches && mains > 0; k++) {
            int[] c = lane.get(city.rnd.nextInt(mains));
            if (c[0] >= bx0 - 8 && c[0] < bx1 + 8 && c[1] >= by0 - 8 && c[1] < by1 + 8) continue;
            if (gen.inWildBand(c[0], c[1])) continue;
            int s = city.rnd.nextBoolean() ? 1 : -1;
            if (c[2] == 1) dirtRoad(c[0], c[1], s, 0, lane);
            else dirtRoad(c[0], c[1], 0, s, lane);
        }
        // Farms and cabins along the lanes, set back a little with a track to the road (more of them since 10.20).
        boolean busy = city.cfg.servicePads();
        for (int k = 0; k < lane.size(); k += busy ? 2 : 3) {
            int[] c = lane.get(k);
            if (city.rnd.nextFloat() > (busy ? 0.55f : 0.35f)) continue;
            boolean farmhouse = city.rnd.nextFloat() < (busy ? 0.35f : 0.55f);
            int fw = farmhouse ? 18 : 6, fh = farmhouse ? 16 : 6;
            int side = city.rnd.nextBoolean() ? 1 : -1;
            boolean vert = c[2] == 1;
            int fx = vert ? c[0] + side * 4 + (side < 0 ? -fw : 0) : c[0] - fw / 2;
            int fy = vert ? c[1] - fh / 2 : c[1] + side * 4 + (side < 0 ? -fh : 0);
            if (!allGrass(fx - 1, fy - 1, fw + 2, fh + 2)) continue;
            if (farmhouse) farm(fx, fy, fw, fh);
            else lotGen.addLot(fx + 1, fy + 1, 4, 4, HOUSE);
            // The track from the road.
            int tx = c[0], ty = c[1];
            for (int s = 1; s < 5; s++) {
                int ax = vert ? tx + side * s : tx, ay = vert ? ty : ty + side * s;
                if (ax >= 0 && ay >= 0 && ax < city.w && ay < city.h && city.tiles[ay * city.w + ax] == GRASS) city.tiles[ay * city.w + ax] = DIRT;
            }
        }
        if (busy) outskirtHouses(bx0, by0, bx1, by1);
        // Woods: clumps of trees out in the fields.
        int woods = (city.w * city.h - (bx1 - bx0) * (by1 - by0)) / 500;
        // (Little nature: open farmland, a copse here and there.)
        if (city.cfg.littleNature()) woods /= 6;
        for (int k = 0; k < woods; k++) {
            int cx = city.rnd.nextInt(city.w), cy = city.rnd.nextInt(city.h);
            if (cx >= bx0 - 2 && cx < bx1 + 2 && cy >= by0 - 2 && cy < by1 + 2) continue;
            int r = 3 + city.rnd.nextInt(6);
            for (int y = cy - r; y <= cy + r; y++)
                for (int x = cx - r; x <= cx + r; x++) {
                    if (x < 1 || y < 1 || x >= city.w - 1 || y >= city.h - 1 || city.tiles[y * city.w + x] != GRASS) continue;
                    if ((x - cx) * (x - cx) + (y - cy) * (y - cy) > r * r * (0.6f + city.rnd.nextFloat() * 0.4f)) continue;
                    if (city.hasNeighbor(x, y, DIRT) || city.hasNeighbor(x, y, ROAD) || city.hasNeighbor(x, y, BUILDING) || city.rnd.nextFloat() < 0.35f) continue;
                    city.tiles[y * city.w + x] = TREE;
                }
        }
        // A few lone trees along the lanes.
        for (int[] c : lane)
            if (city.rnd.nextFloat() < (city.cfg.littleNature() ? 0.015f : 0.06f)) {
                int x = c[0] + (c[2] == 0 ? 2 : 0) * (city.rnd.nextBoolean() ? 1 : -1), y = c[1] + (c[2] == 0 ? 0 : 2) * (city.rnd.nextBoolean() ? 1 : -1);
                if (x > 0 && y > 0 && x < city.w - 1 && y < city.h - 1 && city.tiles[y * city.w + x] == GRASS && !city.hasNeighbor(x, y, DIRT)) city.tiles[y * city.w + x] = TREE;
            }
    }

    /**
     * A two-tile dirt road from (x, y) out to the edge of the map, drifting from side to side. Every tile of the
     * centre line is added to lane as {x, y, vertical ? 1 : 0}.
     */
    void dirtRoad(int x, int y, int dx, int dy, List<int[]> lane) {
        boolean vertical = dx == 0, joined = false;
        int drift = 0;
        for (int step = 0; step < city.w * 2; step++) {
            x += dx;
            y += dy;
            if (x < 0 || y < 0 || x >= city.w || y >= city.h) break;
            // Now and then the road bends a little one way or the other.
            if (step > 4 && city.rnd.nextFloat() < 0.18f) drift = city.rnd.nextInt(3) - 1;
            if (step > 4 && drift != 0 && city.rnd.nextFloat() < 0.5f) {
                if (vertical) x = Math.max(2, Math.min(city.w - 3, x + drift));
                else y = Math.max(2, Math.min(city.h - 3, y + drift));
            }
            if (city.railY0 >= 0 && !vertical && y >= city.railY0 - 2 && y <= city.railY0 + city.railRows + 1) y = city.railY0 - 3;
            for (int t = 0; t < 2; t++) {
                int ax = vertical ? x + t : x, ay = vertical ? y : y + t;
                if (ax < 0 || ay < 0 || ax >= city.w || ay >= city.h) continue;
                byte old = city.tiles[ay * city.w + ax];
                if (old == GRASS || old == TREE) city.tiles[ay * city.w + ax] = DIRT;
                else if (old == RAIL) {
                    // A level crossing.
                    city.tiles[ay * city.w + ax] = ROAD;
                    city.roadDir[ay * city.w + ax] = 1;
                } else if (step > 2 && old != DIRT && !(gen.ringTile != null && gen.ringTile[ay * city.w + ax])) joined = true;
                // A bend: fill the corner so the road stays connected.
                if (vertical && ay > 0 && city.tiles[(ay - 1) * city.w + ax] != DIRT && city.tiles[(ay - 1) * city.w + ax] == GRASS) city.tiles[(ay - 1) * city.w + ax] = DIRT;
                if (!vertical && ax > 0 && city.tiles[ay * city.w + ax - 1] != DIRT && city.tiles[ay * city.w + ax - 1] == GRASS) city.tiles[ay * city.w + ax - 1] = DIRT;
            }
            lane.add(new int[]{x, y, vertical ? 1 : 0});
            if (joined) break;
        }
    }

    /** Where a village's streets end, as {x, y, dx, dy}: the lanes out into the country start there. */
    final List<int[]> villageEnds = new ArrayList<int[]>();

    /**
     * A village: one high street across the middle, a crossroad and a back lane, with plots strung along
     * them and open grass between. No grid.
     */
    void villageCore(int bx0, int by0, int bx1, int by1) {
        int cy = (by0 + by1) / 2 - 1 + city.rnd.nextInt(7) - 3, cx = (bx0 + bx1) / 2 - 1 + city.rnd.nextInt(9) - 4;
        List<Street> own = new ArrayList<Street>();
        Street high = new Street(bx0, cy, bx1, cy + 3, false, false);
        Street cross = new Street(cx, by0, cx + 3, by1, true, false);
        gen.carve(high);
        gen.carve(cross);
        own.add(high);
        own.add(cross);
        villageEnds.add(new int[]{bx0, cy, -1, 0});
        villageEnds.add(new int[]{bx1 - 1, cy, 1, 0});
        villageEnds.add(new int[]{cx, by0, 0, -1});
        villageEnds.add(new int[]{cx, by1 - 1, 0, 1});
        // A back lane off the crossroad, running part way to one side.
        int off = (24 + city.rnd.nextInt(8)) * (city.rnd.nextBoolean() ? 1 : -1), ly = cy + off;
        if (ly > by0 + 20 && ly < by1 - 24) {
            boolean east = city.rnd.nextBoolean();
            int len = (bx1 - bx0) / 2 - 12 - city.rnd.nextInt(16);
            Street back = east ? new Street(cx + 3, ly, cx + 3 + len, ly + 3, false, false) : new Street(cx - len, ly, cx, ly + 3, false, false);
            gen.carve(back);
            own.add(back);
        }
        // Plots along both sides of each street, with gaps of open ground between them.
        for (Street st : own) plotsAlong(st, bx0, by0, bx1, by1, 1);
        // Hedgerows and garden trees on the open ground in between.
        for (int y = by0; y < by1; y++)
            for (int x = bx0; x < bx1; x++)
                if (city.tiles[y * city.w + x] == GRASS && city.rnd.nextFloat() < 0.03f && !city.hasNeighbor(x, y, ROAD) && !city.hasNeighbor(x, y, SIDEWALK))
                    city.tiles[y * city.w + x] = TREE;
    }

    /** Plots along both sides of a street, with gaps of open ground between them. flag: 1 village, 2 hamlet. */
    int plotsAlong(Street st, int bx0, int by0, int bx1, int by1, int flag) {
        int made = 0;
        int from = st.vertical ? st.y0 : st.x0, to = st.vertical ? st.y1 : st.x1;
        for (int side = -1; side <= 1; side += 2) {
            int p = from + 1 + city.rnd.nextInt(3);
            while (p < to - 12) {
                int pw = 15 + city.rnd.nextInt(5), pd = 15 + city.rnd.nextInt(5);
                int x0, y0, x1, y1;
                if (st.vertical) {
                    y0 = p;
                    y1 = p + pw;
                    x0 = side < 0 ? st.x0 - pd : st.x1;
                    x1 = side < 0 ? st.x0 : st.x1 + pd;
                } else {
                    x0 = p;
                    x1 = p + pw;
                    y0 = side < 0 ? st.y0 - pd : st.y1;
                    y1 = side < 0 ? st.y0 : st.y1 + pd;
                }
                if (x0 >= bx0 && y0 >= by0 && x1 <= bx1 && y1 <= by1 && allGrass(x0, y0, x1 - x0, y1 - y0)) {
                    city.fill(x0, y0, x1 - x0, y1 - y0, SIDEWALK);
                    city.blocks.add(new int[]{x0, y0, x1, y1, flag});
                    made++;
                    p += pw + 1 + (city.rnd.nextFloat() < 0.3f ? 6 + city.rnd.nextInt(10) : 0);
                } else p += 3;
            }
        }
        return made;
    }

    /**
     * A stretch of a big map's suburbs laid out as a neighbourhood of its own instead of more grid: a spine road
     * with closes (cul-de-sacs) off both sides, a crescent round a green, houses round a village green, flats
     * set in parkland, big plots out on the edge of town, or a gated estate. Its roads are laid now and its
     * homes with the rest of the blocks (fillNeighbourhood). False if it can't be done here.
     */
    boolean neighbourhood(int x0, int y0, int x1, int y1, float d) {
        int mx = (x0 + x1) / 2, my = (y0 + y1) / 2;
        boolean rT = gen.isRoad(mx, y0 - 1), rB = gen.isRoad(mx, y1), rL = gen.isRoad(x0 - 1, my), rR = gen.isRoad(x1, my);
        if (!rT && !rB && !rL && !rR) return false;
        int dt = city.districtType(mx, my);
        // ((27.5) Map version 40: the new kinds first, more often than not, each where it belongs.)
        if (gen.look != null && newNeighbourhood(x0, y0, x1, y1, d, dt, rT, rB, rL, rR)) return true;
        int type;
        float r = city.rnd.nextFloat();
        if (dt == DT_INDUSTRIAL) type = NB_WORKS;
        else if (d > 0.8f && dt != DT_MIDTOWN && r < 0.15f) type = NB_ACRES;
        else {
            r = city.rnd.nextFloat();
            if (dt == DT_MIDTOWN) type = r < 0.5f ? NB_ESTATE : NB_GREEN;
            else type = r < 0.3f ? NB_CLOSES : r < 0.52f ? NB_CRESCENT : r < 0.72f ? NB_GREEN : r < 0.84f ? NB_ESTATE : NB_GATED;
        }
        // Next door to one just like it? Try something else: every neighbourhood its own.
        for (int tries = 0; tries < 3 && type != NB_WORKS && sameNearby(mx, my, type); tries++) {
            r = city.rnd.nextFloat();
            type = dt == DT_MIDTOWN ? (r < 0.5f ? NB_ESTATE : NB_GREEN)
                    : r < 0.25f ? NB_CLOSES : r < 0.45f ? NB_CRESCENT : r < 0.65f ? NB_GREEN : r < 0.8f ? NB_ESTATE : NB_GATED;
        }
        // (Since 10.25 roads needn't be straight: some suburbs are laid out with winding streets instead.)
        if (city.cfg.civic >= 10 && (type == NB_CLOSES || type == NB_CRESCENT || (city.cfg.civic >= 28 && type == NB_GREEN))
                && city.rnd.nextFloat() < (city.cfg.civic >= 28 ? 0.75f : 0.4f)) type = NB_WINDING;
        int[] green = null;
        switch (type) {
            case NB_WINDING:
                if (!winding(x0, y0, x1, y1, rT, rB, rL, rR)) return false;
                break;
            case NB_CLOSES:
            case NB_GATED:
                if (!closes(x0, y0, x1, y1, rT, rB, rL, rR)) return false;
                break;
            case NB_CRESCENT:
            case NB_ESTATE:
            case NB_WORKS:
                green = crescent(x0, y0, x1, y1, rT, rB, rL, rR);
                if (green == null) return false;
                break;
            case NB_GREEN:
                green = villageGreen(x0, y0, x1, y1, rT, rB, rL, rR);
                if (green == null) return false;
                break;
            default:
                break;
        }
        city.blocks.add(green == null ? new int[]{x0, y0, x1, y1, 3, type}
                : new int[]{x0, y0, x1, y1, 3, type, green[0], green[1], green[2], green[3]});
        nameNeighbourhood(x0, y0, x1, y1, type, dt);
        return true;
    }

    /**
     * ((27.5) Map version 40) The new kinds of neighbourhood: a trailer park round its loop, a tower estate of slab
     * blocks set in parkland, a market quarter of narrow lanes and little shops round a square, a college quad, a
     * circus (a round place with streets running out of it on the diagonal to the corners), rows of terraces down
     * straight parallel streets, and old warehouses made lofts. False if none of them suits or fits here.
     */
    boolean newNeighbourhood(int x0, int y0, int x1, int y1, float d, int dt, boolean rT, boolean rB, boolean rL, boolean rR) {
        java.util.Random r = gen.look.r;
        // (The Archipelago's outer islands stay the palm-fringed villages they are.)
        if (city.cfg.preset() == CityConfig.ARCHIPELAGO && city.island((x0 + x1) / 2, (y0 + y1) / 2) >= 1) return false;
        int[] opts;
        switch (dt) {
            case DT_SUBURB:
            case DT_PARKSIDE: opts = d > 0.55f ? new int[]{NB_TRAILERS, NB_TERRACES, NB_CIRCUS, NB_TOWERS} : new int[]{NB_TERRACES, NB_CIRCUS, NB_TOWERS}; break;
            case DT_MIDTOWN: opts = new int[]{NB_MARKET, NB_TOWERS, NB_CIRCUS, NB_TERRACES}; break;
            case DT_OLDTOWN: opts = new int[]{NB_MARKET, NB_TERRACES, NB_CIRCUS}; break;
            case DT_CAMPUS: opts = new int[]{NB_QUAD, NB_QUAD, NB_CIRCUS}; break;
            case DT_INDUSTRIAL: opts = new int[]{NB_LOFTS, NB_LOFTS, NB_TRAILERS}; break;
            default: return false;
        }
        // (The curving cul-de-sacs of the suburbs keep most of their turns, and the islands get fewer of the new kinds.)
        float chance = city.waterKind == CityConfig.W_ISLANDS ? 0.3f : dt == DT_SUBURB || dt == DT_PARKSIDE ? 0.38f : 0.62f;
        if (dt != DT_CAMPUS && r.nextFloat() > chance) return false;
        int start = r.nextInt(opts.length);
        for (int k = 0; k < opts.length; k++) {
            int type = opts[(start + k) % opts.length];
            if (k < opts.length - 1 && sameNearby((x0 + x1) / 2, (y0 + y1) / 2, type)) continue;
            // (Not too many of any one kind, however big the map: the old kinds have their turn too.)
            int had = 0;
            for (int[] o : city.blocks) if (o.length > 5 && o[4] == 3 && o[5] == type) had++;
            if (had >= 2 + city.w / 200) continue;
            int[] green = layoutNew(type, x0, y0, x1, y1, rT, rB, rL, rR);
            if (green == null) continue;
            city.blocks.add(green.length < 4 ? new int[]{x0, y0, x1, y1, 3, type}
                    : new int[]{x0, y0, x1, y1, 3, type, green[0], green[1], green[2], green[3]});
            nameNeighbourhood(x0, y0, x1, y1, type, dt);
            return true;
        }
        return false;
    }

    /** The roads of one of the new kinds: its green {x0, y0, x1, y1}, an empty array for none, or null if it won't fit. */
    int[] layoutNew(int type, int x0, int y0, int x1, int y1, boolean rT, boolean rB, boolean rL, boolean rR) {
        int bw = x1 - x0, bh = y1 - y0;
        switch (type) {
            case NB_TRAILERS: {
                // A loop lane inside the block, a lane across the middle of it, and one or two ways in.
                if (bw < 38 || bh < 38) return null;
                int rx0 = x0 + 8, rx1 = x1 - 8, ry0 = y0 + 8, ry1 = y1 - 8;
                lane3(false, rx0, rx1, ry0);
                lane3(false, rx0, rx1, ry1 - 3);
                lane3(true, ry0, ry1, rx0);
                lane3(true, ry0, ry1, rx1 - 3);
                boolean across = bw >= bh;
                if ((across ? ry1 - ry0 : rx1 - rx0) >= 30) {
                    int c = across ? (ry0 + ry1) / 2 - 1 : (rx0 + rx1) / 2 - 1;
                    lane3(!across, across ? rx0 : ry0, across ? rx1 : ry1, c);
                }
                entrances(x0, y0, x1, y1, rx0, ry0, rx1, ry1, rT, rB, rL, rR, 1 + gen.look.r.nextInt(2));
                return new int[0];
            }
            case NB_TERRACES:
            case NB_LOFTS: {
                // Straight streets side by side right through the block, from the road on one side to the other.
                boolean vert;
                if (rT && rB && (bh >= bw || !(rL && rR))) vert = true;
                else if (rL && rR) vert = false;
                else return null;
                int span = vert ? bw : bh, a0 = vert ? y0 : x0, a1 = vert ? y1 : x1, c0 = vert ? x0 : y0;
                int step = type == NB_LOFTS ? 20 + gen.look.r.nextInt(3) : 12 + gen.look.r.nextInt(3);
                int n = (span - 8) / step;
                if (n < 2) return null;
                int first = c0 + (span - (n - 1) * step - 3) / 2;
                for (int k = 0; k < n; k++) lane3(vert, a0, a1, first + k * step);
                // A street across them all in the middle, on a long block.
                if (a1 - a0 >= 50) lane3(!vert, first, first + (n - 1) * step + 3, (a0 + a1) / 2 - 1);
                return new int[0];
            }
            case NB_MARKET: {
                // Narrow lanes both ways, close together, and the market square in the middle.
                if (bw < 38 || bh < 38) return null;
                int step = 12 + gen.look.r.nextInt(2);
                int nx = (bw - 6) / step, ny = (bh - 6) / step;
                if (nx < 2 || ny < 2) return null;
                int fx = x0 + (bw - (nx - 1) * step - 3) / 2, fy = y0 + (bh - (ny - 1) * step - 3) / 2;
                for (int k = 0; k < nx; k++) lane3(true, y0, y1, fx + k * step);
                for (int k = 0; k < ny; k++) lane3(false, x0, x1, fy + k * step);
                int cx = fx + ((nx - 1) / 2) * step + 3, cy = fy + ((ny - 1) / 2) * step + 3;
                return new int[]{cx + 1, cy + 1, cx + step - 4, cy + step - 4};
            }
            case NB_QUAD:
            case NB_TOWERS:
                return villageGreen(x0, y0, x1, y1, rT, rB, rL, rR);
            case NB_CIRCUS: {
                if (bw < 40 || bh < 40) return null;
                float cx = (x0 + x1) / 2f, cy = (y0 + y1) / 2f, rad = Math.max(7, Math.min(bw, bh) * 0.17f);
                float[] ring = new float[2 * 65];
                for (int k = 0; k <= 64; k++) {
                    double t = k / 64.0 * Math.PI * 2;
                    ring[k * 2] = cx + (float) Math.cos(t) * rad;
                    ring[k * 2 + 1] = cy + (float) Math.sin(t) * rad;
                }
                int spokes = 0;
                // On the diagonal to every corner with a street along both sides of it.
                boolean[] corner = {rT && rL, rT && rR, rB && rL, rB && rR};
                float[][] at = {{x0 + 0.5f, y0 + 0.5f}, {x1 - 0.5f, y0 + 0.5f}, {x0 + 0.5f, y1 - 0.5f}, {x1 - 0.5f, y1 - 0.5f}};
                float[][] lines = new float[4][];
                for (int k = 0; k < 4; k++) {
                    if (!corner[k]) continue;
                    float ex = at[k][0], ey = at[k][1], dx = ex - cx, dy = ey - cy, len = (float) Math.sqrt(dx * dx + dy * dy);
                    float sx = cx + dx / len * rad, sy = cy + dy / len * rad;
                    lines[k] = CityGenerator.bezier(sx, sy, sx + (ex - sx) / 3, sy + (ey - sy) / 3, sx + (ex - sx) * 2 / 3, sy + (ey - sy) * 2 / 3, ex, ey, 24);
                    spokes++;
                }
                // Straight out to the street on any side where there aren't two diagonals already.
                if (spokes < 2) {
                    if (rT) lane3(true, y0, Math.round(cy - rad) + 1, Math.round(cx) - 1);
                    if (rB) lane3(true, Math.round(cy + rad) - 1, y1, Math.round(cx) - 1);
                    if (rL) lane3(false, x0, Math.round(cx - rad) + 1, Math.round(cy) - 1);
                    if (rR) lane3(false, Math.round(cx + rad) - 1, x1, Math.round(cy) - 1);
                }
                curveRoad(ring, true);
                for (float[] l : lines) if (l != null) curveRoad(l, false);
                int g = Math.round(rad * 0.62f);
                return clearOfRoad(new int[]{Math.round(cx) - g, Math.round(cy) - g, Math.round(cx) + g, Math.round(cy) + g});
            }
            default:
                return null;
        }
    }

    /** Lanes in from the streets round the block to a loop {rx0, ry0, rx1, ry1} inside it: up to n of them. */
    void entrances(int x0, int y0, int x1, int y1, int rx0, int ry0, int rx1, int ry1, boolean rT, boolean rB, boolean rL, boolean rR, int n) {
        List<Integer> sides = new ArrayList<Integer>();
        if (rT) sides.add(0);
        if (rB) sides.add(1);
        if (rL) sides.add(2);
        if (rR) sides.add(3);
        java.util.Collections.shuffle(sides, gen.look.r);
        for (int k = 0; k < Math.min(n, sides.size()); k++) {
            int sd = sides.get(k);
            if (sd < 2) {
                int c = rx0 + (rx1 - rx0) / 3 + gen.look.r.nextInt(Math.max(1, (rx1 - rx0) / 3));
                if (sd == 0) lane3(true, y0, ry0 + 1, c);
                else lane3(true, ry1 - 1, y1, c);
            } else {
                int c = ry0 + (ry1 - ry0) / 3 + gen.look.r.nextInt(Math.max(1, (ry1 - ry0) / 3));
                if (sd == 2) lane3(false, x0, rx0 + 1, c);
                else lane3(false, rx1 - 1, x1, c);
            }
        }
    }

    boolean sameNearby(int x, int y, int type) {
        for (int[] b : city.blocks)
            if (b.length > 5 && b[4] == 3 && (b[5] == type || (type == NB_GATED && b[5] == NB_CLOSES) || (type == NB_CLOSES && b[5] == NB_GATED))
                    && World.hypot((b[0] + b[2]) / 2f - x, (b[1] + b[3]) / 2f - y) < 130) return true;
        return false;
    }

    /** A two-lane street three tiles wide, running north-south (vertical) from a0 to a1 at column c, or east-west at row c. */
    void lane3(boolean vertical, int a0, int a1, int c) {
        gen.carve(vertical ? new Street(c, a0, c + 3, a1, true, false) : new Street(a0, c, a1, c + 3, false, false));
    }

    /** A turning circle at the end of a street (vertical or not) whose last row is at a, the street at c..c+2. */
    void bulb(boolean vertical, int a, int c, boolean atStart) {
        int lo = atStart ? a - 1 : a - 3, hi = atStart ? a + 3 : a + 1;
        for (int along = lo; along <= hi; along++)
            for (int across = c - 1; across <= c + 3; across++) {
                boolean corner = (along == lo || along == hi) && (across == c - 1 || across == c + 3);
                if (corner) continue;
                int x = vertical ? across : along, y = vertical ? along : across;
                if (x < 0 || y < 0 || x >= city.w || y >= city.h) continue;
                int i = y * city.w + x;
                if (city.tiles[i] != ROAD) {
                    city.tiles[i] = ROAD;
                    city.roadDir[i] = (byte) (vertical ? 1 : 2);
                }
            }
    }

    /** A spine road through (or into) the block with closes off it on both sides, staggered. */
    boolean closes(int x0, int y0, int x1, int y1, boolean rT, boolean rB, boolean rL, boolean rR) {
        int bw = x1 - x0, bh = y1 - y0;
        boolean vert = (rT || rB) && (bh >= bw || !(rL || rR));
        int a0 = vert ? y0 : x0, a1 = vert ? y1 : x1, c0 = vert ? x0 : y0, c1 = vert ? x1 : y1;
        boolean s0 = vert ? rT : rL, s1 = vert ? rB : rR;
        if (a1 - a0 < 36 || c1 - c0 < 34) return false;
        int c = c0 + (c1 - c0 - 3) / 2 + city.rnd.nextInt(5) - 2;
        int sa0 = s0 ? a0 : a0 + 9, sa1 = s1 ? a1 : a1 - 9;
        lane3(vert, sa0, sa1, c);
        if (!s0) bulb(vert, sa0, c, true);
        if (!s1) bulb(vert, sa1 - 1, c, false);
        for (int side = -1; side <= 1; side += 2) {
            int depth = side < 0 ? c - c0 : c1 - (c + 3);
            int len = depth - 10;
            if (len < 6) continue;
            int step = 17 + city.rnd.nextInt(3);
            for (int a = sa0 + 8 + (side > 0 ? step / 2 : 0); a + 3 <= sa1 - 8; a += step + city.rnd.nextInt(3)) {
                // Now and then a gap: a bigger garden, or a footpath through.
                if (city.rnd.nextFloat() < 0.15f) continue;
                int e0 = side < 0 ? c - len : c + 3, e1 = side < 0 ? c : c + 3 + len;
                lane3(!vert, e0, e1, a);
                bulb(!vert, side < 0 ? e0 : e1 - 1, a, side < 0);
            }
        }
        return true;
    }

    /**
     * A crescent: a loop off one of the streets round the block, its two arms joined at the back, with a green
     * in the middle. Returns the green {x0, y0, x1, y1}.
     */
    int[] crescent(int x0, int y0, int x1, int y1, boolean rT, boolean rB, boolean rL, boolean rR) {
        List<Integer> sides = new ArrayList<Integer>();
        if (rT) sides.add(0);
        if (rB) sides.add(1);
        if (rL) sides.add(2);
        if (rR) sides.add(3);
        int side = sides.get(city.rnd.nextInt(sides.size()));
        boolean legsVert = side < 2;
        int a0 = legsVert ? x0 : y0, a1 = legsVert ? x1 : y1, c0 = legsVert ? y0 : x0, c1 = legsVert ? y1 : x1;
        if (a1 - a0 < 36 || c1 - c0 < 30) return null;
        boolean fromLow = side == 0 || side == 2;
        int p1 = a0 + 9 + city.rnd.nextInt(3), p2 = a1 - 12 - city.rnd.nextInt(3);
        int depth = c1 - c0 - 10 - city.rnd.nextInt(4);
        int l0 = fromLow ? c0 : c1 - depth, l1 = fromLow ? c0 + depth : c1;
        if (city.cfg.civic >= 10) {
            // (Since 10.25) A real crescent: the two arms sweep round in one smooth curve at the back.
            float e = fromLow ? c0 - 0.5f : c1 + 0.5f, back = fromLow ? c0 + depth - 1.5f : c1 - depth + 1.5f;
            float pull = e + (back - e) / 0.75f;
            float a = p1 + 1.5f, b2 = p2 + 1.5f;
            float[] pts = legsVert ? CityGenerator.bezier(a, e, a, pull, b2, pull, b2, e, 48) : CityGenerator.bezier(e, a, pull, a, pull, b2, e, b2, 48);
            curveRoad(pts, false);
            int[] g = legsVert ? new int[]{p1 + 3, fromLow ? c0 : c1 - depth + 3, p2, fromLow ? c0 + depth - 3 : c1}
                    : new int[]{fromLow ? c0 : c1 - depth + 3, p1 + 3, fromLow ? c0 + depth - 3 : c1, p2};
            return clearOfRoad(g);
        }
        lane3(legsVert, l0, l1, p1);
        lane3(legsVert, l0, l1, p2);
        int cc = fromLow ? c0 + depth - 3 : c1 - depth;
        lane3(!legsVert, p1, p2 + 3, cc);
        int g0 = fromLow ? c0 : cc + 3, g1 = fromLow ? cc : c1;
        return legsVert ? new int[]{p1 + 3, g0, p2, g1} : new int[]{g0, p1 + 3, g1, p2};
    }

    /** A village green with a road all round it and a road or two out to the streets. Returns the green. */
    int[] villageGreen(int x0, int y0, int x1, int y1, boolean rT, boolean rB, boolean rL, boolean rR) {
        int bw = x1 - x0, bh = y1 - y0;
        if (bw < 44 || bh < 44) return null;
        int gw = Math.min(18, bw - 32), gh = Math.min(18, bh - 32);
        int gx0 = x0 + (bw - gw) / 2 + city.rnd.nextInt(3) - 1, gy0 = y0 + (bh - gh) / 2 + city.rnd.nextInt(3) - 1;
        int rx0 = gx0 - 3, ry0 = gy0 - 3, rx1 = gx0 + gw + 3, ry1 = gy0 + gh + 3;
        boolean oval = city.cfg.civic >= 10;
        if (oval) {
            // (Since 10.25) An oval road round the green, not a square.
            float cx = (rx0 + rx1) / 2f, cy = (ry0 + ry1) / 2f, ax = (rx1 - rx0) / 2f - 1.5f, ay = (ry1 - ry0) / 2f - 1.5f;
            float[] pts = new float[2 * 65];
            for (int k = 0; k <= 64; k++) {
                double t = k / 64.0 * Math.PI * 2;
                pts[k * 2] = cx + (float) Math.cos(t) * ax;
                pts[k * 2 + 1] = cy + (float) Math.sin(t) * ay;
            }
            curveRoad(pts, true);
        } else {
            lane3(false, rx0, rx1, ry0);
            lane3(false, rx0, rx1, ry1 - 3);
            lane3(true, ry0, ry1, rx0);
            lane3(true, ry0, ry1, rx1 - 3);
        }
        // Roads out: one or two (more is just a grid again), off-centre.
        List<Integer> sides = new ArrayList<Integer>();
        if (rT) sides.add(0);
        if (rB) sides.add(1);
        if (rL) sides.add(2);
        if (rR) sides.add(3);
        java.util.Collections.shuffle(sides, city.rnd);
        int outs = Math.min(sides.size(), 1 + city.rnd.nextInt(2));
        for (int k = 0; k < outs; k++) {
            int sd = sides.get(k);
            if (sd < 2) {
                int c = gx0 + gw / 3 + city.rnd.nextInt(Math.max(1, gw / 3));
                if (sd == 0) lane3(true, y0, ry0 + (oval ? 2 : 0), c);
                else lane3(true, ry1 - (oval ? 2 : 0), y1, c);
            } else {
                int c = gy0 + gh / 3 + city.rnd.nextInt(Math.max(1, gh / 3));
                if (sd == 2) lane3(false, x0, rx0 + (oval ? 2 : 0), c);
                else lane3(false, rx1 - (oval ? 2 : 0), x1, c);
            }
        }
        if (oval) return clearOfRoad(new int[]{gx0, gy0, gx0 + gw, gy0 + gh});
        return new int[]{gx0, gy0, gx0 + gw, gy0 + gh};
    }

    /**
     * Winding streets (10.25): one or two roads that bend their way through the block in an S from the street on
     * one side to the street on the other, with a curving close or two off them. Homes face them all along.
     */
    boolean winding(int x0, int y0, int x1, int y1, boolean rT, boolean rB, boolean rL, boolean rR) {
        int bw = x1 - x0, bh = y1 - y0;
        boolean vert;
        if ((rT && rB) && (!(rL && rR) || bh >= bw)) vert = true;
        else if (rL && rR) vert = false;
        else return false;
        int len = vert ? bh : bw, span = vert ? bw : bh;
        if (len < 36 || span < 30) return false;
        int n = span >= 60 ? 2 : 1;
        float a0 = (vert ? y0 : x0) - 0.5f, a1 = (vert ? y1 : x1) + 0.5f, s0 = vert ? x0 : y0;
        for (int k = 0; k < n; k++) {
            // Where it leaves each street, and how far it swings over in between.
            float lane = s0 + span * (k + 0.5f) / n;
            float swing = Math.min(span / (2.5f * n), 9) * (city.rnd.nextBoolean() ? 1 : -1);
            float in = lane - swing * 0.5f + city.rnd.nextInt(3) - 1, out = lane + swing * 0.5f + city.rnd.nextInt(3) - 1;
            float m1 = a0 + (a1 - a0) * 0.38f, m2 = a0 + (a1 - a0) * 0.62f;
            float[] pts = vert ? CityGenerator.bezier(in, a0, in + swing * 1.6f, m1, out - swing * 1.6f, m2, out, a1, 56)
                    : CityGenerator.bezier(a0, in, m1, in + swing * 1.6f, m2, out - swing * 1.6f, a1, out, 56);
            curveRoad(pts, false);
            // A curving close off the outside of the bend, ending in a turning circle.
            if (len >= 48 && city.rnd.nextFloat() < 0.7f) {
                int at = 22 + city.rnd.nextInt(Math.max(1, (int) (pts.length / 2 * 0.5f)));
                at = Math.min(pts.length / 2 - 12, at);
                float px = pts[at * 2], py = pts[at * 2 + 1];
                float side = swing > 0 ? -1 : 1, reach = Math.min(span / (2f * n) - 5, 11);
                if (reach >= 6) {
                    float ex = vert ? px + side * reach : px + 4, ey = vert ? py + 4 : py + side * reach;
                    float[] spur = CityGenerator.bezier(px, py, vert ? px + side * reach * 0.5f : px, vert ? py : py + side * reach * 0.5f,
                            vert ? ex : ex - 3, vert ? ey - 3 : ey, ex, ey, 20);
                    curveRoad(spur, false);
                    int cx = Math.round(ex), cy = Math.round(ey);
                    for (int y = cy - 2; y <= cy + 2; y++)
                        for (int x = cx - 2; x <= cx + 2; x++) {
                            if (x <= x0 || y <= y0 || x >= x1 - 1 || y >= y1 - 1 || (Math.abs(x - cx) == 2 && Math.abs(y - cy) == 2)) continue;
                            int i = y * city.w + x;
                            if (city.tiles[i] != ROAD) {
                                city.tiles[i] = ROAD;
                                city.roadDir[i] = 3;
                                city.curveTile[i] = true;
                            }
                        }
                    city.curves.add(new float[]{ex, ey, -2.5f});
                }
            }
        }
        return true;
    }

    /**
     * A street three tiles wide along a smooth centre line {x, y, ...} (in tiles): the tiles under it become road
     * (running the way the curve goes there), and the line is kept to draw the road smoothly over them.
     */
    void curveRoad(float[] pts, boolean closed) {
        int n = pts.length / 2;
        for (int k = 0; k < n - 1; k++) {
            float ax = pts[k * 2], ay = pts[k * 2 + 1], bx = pts[k * 2 + 2], by = pts[k * 2 + 3];
            float dx = bx - ax, dy = by - ay, len = (float) Math.sqrt(dx * dx + dy * dy);
            byte dir = (byte) (Math.abs(dx) > Math.abs(dy) ? 2 : 1);
            int steps = Math.max(1, (int) (len * 4));
            for (int s = 0; s <= steps; s++) {
                float px = ax + dx * s / steps, py = ay + dy * s / steps;
                for (int y = City.fl(py - 1.6f); y <= City.fl(py + 1.6f); y++)
                    for (int x = City.fl(px - 1.6f); x <= City.fl(px + 1.6f); x++) {
                        if (x < 0 || y < 0 || x >= city.w || y >= city.h) continue;
                        float ox = x + 0.5f - px, oy = y + 0.5f - py;
                        if (ox * ox + oy * oy > 1.6f * 1.6f) continue;
                        int i = y * city.w + x;
                        if (city.tiles[i] == ROAD) {
                            if (city.roadDir[i] != dir && city.roadDir[i] != 0) city.roadDir[i] = 3;
                            continue;
                        }
                        city.tiles[i] = ROAD;
                        city.roadDir[i] = dir;
                        if (city.curveTile == null) city.curveTile = new boolean[city.w * city.h];
                        city.curveTile[i] = true;
                        if (gen.curveOnGrass) {
                            if (city.slipTile == null) city.slipTile = new boolean[city.w * city.h];
                            city.slipTile[i] = true;
                            if (gen.keepPaved == null) gen.keepPaved = new boolean[city.w * city.h];
                            gen.keepPaved[i] = true;
                        }
                    }
            }
        }
        city.curves.add(pts);
    }

    /** The largest part of a green {x0, y0, x1, y1} that keeps two tiles clear of any road, or null. */
    int[] clearOfRoad(int[] g) {
        int x0 = g[0], y0 = g[1], x1 = g[2], y1 = g[3];
        for (int guard = 0; guard < 40 && x1 - x0 >= 4 && y1 - y0 >= 4; guard++) {
            boolean top = false, bottom = false, left = false, right = false;
            for (int x = x0; x < x1; x++) {
                if (city.roadNear(x, y0, 1)) top = true;
                if (city.roadNear(x, y1 - 1, 1)) bottom = true;
            }
            for (int y = y0; y < y1; y++) {
                if (city.roadNear(x0, y, 1)) left = true;
                if (city.roadNear(x1 - 1, y, 1)) right = true;
            }
            if (!top && !bottom && !left && !right) return new int[]{x0, y0, x1, y1};
            if (top) y0++;
            if (bottom) y1--;
            if (left) x0++;
            if (right) x1--;
        }
        // (Too small for a green: an empty one, so the neighbourhood still goes ahead round its roads.)
        return x1 - x0 >= 4 && y1 - y0 >= 4 ? new int[]{x0, y0, x1, y1} : new int[]{x0, y0, x0, y0};
    }

    void nameNeighbourhood(int x0, int y0, int x1, int y1, int type, int dt) {
        if (city.districtAt == null || city.districts.size() > 110) return;
        String[][] ends = {{" Meadows", " Fields", " Chase", " Rise"}, {" Crescent", " Gardens", " Park"},
                {" Green", " Common"}, {" Estate", " Court", " Towers"}, {" Acres", " Ranch", " Farms"},
                {" Manor", " Reserve", " Hills"}, {" Business Park", " Trading Estate", " Commerce Park"}, {" Way", " Bends", " Vale"},
                {" Park Homes", " Mobile Park", " Trailer Court"}, {" Towers", " House Estate", " Heights"}, {" Market", " Bazaar", " Arcade"},
                {" College", " Quad", " Halls"}, {" Circus", " Place", " Circle"}, {" Terrace", " Row", " Rows"}, {" Lofts", " Yards", " Wharf"}};
        String word = city.country.districtWords[city.rnd.nextInt(city.country.districtWords.length)];
        String[] e = ends[type];
        District d = new District();
        d.type = dt;
        d.name = city.country.districtName(word + e[city.rnd.nextInt(e.length)], word, dt);
        for (District o : city.districts) if (o.name.equals(d.name)) return;
        d.x = (x0 + x1) / 2f;
        d.y = (y0 + y1) / 2f;
        d.cx = d.x * T;
        d.cy = d.y * T;
        // ((27.5) Map version 40: the new kinds their own look, the rest the city's.)
        if (gen.look != null && city.cfg.neighbourhoods() && !city.desert) {
            d.theme = type >= NB_TRAILERS ? Neighbourhoods.NB_THEMES[type - NB_TRAILERS] : city.districts.get(city.districtAt.get(((y0 + y1) / 2) * city.w + (x0 + x1) / 2)).theme;
            if (d.theme == null) d.theme = Neighbourhoods.PALETTES[gen.look.palette];
        }
        int id = city.districts.size();
        city.districts.add(d);
        for (int y = Math.max(0, y0); y < Math.min(city.h, y1); y++)
            for (int x = Math.max(0, x0); x < Math.min(city.w, x1); x++) {
                city.districts.get(city.districtAt.get(y * city.w + x)).tiles--;
                city.districtAt.set(y * city.w + x, (byte) (id));
                d.tiles++;
            }
    }

    /** The homes, gardens and green of a neighbourhood laid out by neighbourhood(). */
    void fillNeighbourhood(int[] b) {
        int x0 = b[0], y0 = b[1], x1 = b[2], y1 = b[3], type = b[5];
        if (x1 - x0 < 4 || y1 - y0 < 4) return;
        gen.curDistrict = city.districtType((x0 + x1) / 2, (y0 + y1) / 2);
        for (int y = y0; y < y1; y++)
            for (int x = x0; x < x1; x++)
                if (city.tiles[y * city.w + x] != ROAD) city.tiles[y * city.w + x] = GRASS;
        // A pavement round the edge and along every road.
        for (int y = y0; y < y1; y++)
            for (int x = x0; x < x1; x++) {
                int i = y * city.w + x;
                if (city.tiles[i] == ROAD) continue;
                boolean edge = x == x0 || y == y0 || x == x1 - 1 || y == y1 - 1, by = false;
                if (type != NB_ACRES)
                    for (int dy = -1; dy <= 1 && !by; dy++)
                        for (int dx = -1; dx <= 1 && !by; dx++)
                            if (inside(x + dx, y + dy, x0, y0, x1, y1) && gen.isRoad(x + dx, y + dy)) by = true;
                if (edge || by) city.tiles[i] = SIDEWALK;
            }
        // The green, with a ring of homes round it if it's a big one.
        int[] keep = null;
        if (b.length >= 10) {
            int gx0 = b[6] + 1, gy0 = b[7] + 1, gx1 = b[8] - 1, gy1 = b[9] - 1;
            if (type != NB_GREEN && gx1 - gx0 >= 26 && gy1 - gy0 >= 26) {
                gx0 += 7;
                gy0 += 7;
                gx1 -= 7;
                gy1 -= 7;
            }
            if (gx1 - gx0 >= 4 && gy1 - gy0 >= 4) {
                keep = new int[]{gx0, gy0, gx1, gy1};
                int gw = gx1 - gx0, gh = gy1 - gy0;
                boolean small = gw <= 16 && gh <= 16;
                if (type == NB_MARKET) {
                    // ((27.5) The market square: paved, stalls' awnings round a fountain.)
                    city.fill(gx0, gy0, gw, gh, PLAZA);
                    city.addDecor(D_FOUNTAIN, ((gx0 + gx1) / 2 - 1) * T, ((gy0 + gy1) / 2 - 1) * T, ((gx0 + gx1) / 2 + 1) * T, ((gy0 + gy1) / 2 + 1) * T, 0);
                    if (gw >= 6 && gh >= 6) city.addDecor(D_UMBRELLA, (gx0 + 1) * T, (gy0 + 1) * T, (gx1 - 1) * T, (gy1 - 1) * T, 0);
                    city.openAreas.add(new float[]{(gx0 + gx1) / 2f * T, (gy0 + gy1) / 2f * T, 2});
                } else if (type == NB_QUAD) {
                    // ((27.5) The quad: a lawn with paths corner to corner and a fountain where they cross.)
                    city.fill(gx0, gy0, gw, gh, GRASS);
                    for (int k = 0; k < Math.max(gw, gh); k++) {
                        int px = gx0 + k * gw / Math.max(gw, gh), py = gy0 + k * gh / Math.max(gw, gh);
                        city.tiles[py * city.w + px] = PLAZA;
                        city.tiles[(gy1 - 1 - (py - gy0)) * city.w + px] = PLAZA;
                    }
                    city.addDecor(D_FOUNTAIN, ((gx0 + gx1) / 2 - 1) * T, ((gy0 + gy1) / 2 - 1) * T, ((gx0 + gx1) / 2 + 1) * T, ((gy0 + gy1) / 2 + 1) * T, 0);
                    city.openAreas.add(new float[]{(gx0 + gx1) / 2f * T, (gy0 + gy1) / 2f * T, 0});
                } else if (type == NB_TOWERS) {
                    // ((27.5) Slab blocks standing in the grass, paths between them.)
                    city.fill(gx0, gy0, gw, gh, GRASS);
                    boolean wide = gw >= gh;
                    int n = Math.max(1, Math.min(3, (wide ? gw : gh) / 8));
                    for (int k = 0; k < n; k++) {
                        int sw = wide ? 5 : Math.min(gw - 4, 12), sh = wide ? Math.min(gh - 4, 12) : 5;
                        int sx = wide ? gx0 + 1 + k * (gw - 2) / n + ((gw - 2) / n - sw) / 2 : gx0 + (gw - sw) / 2;
                        int sy = wide ? gy0 + (gh - sh) / 2 : gy0 + 1 + k * (gh - 2) / n + ((gh - 2) / n - sh) / 2;
                        if (sw >= 4 && sh >= 4 && allType(sx, sy, sw, sh, GRASS)) lotGen.addLot(sx, sy, sw, sh, APARTMENT);
                    }
                    for (int x = gx0; x < gx1; x++) if (city.tiles[((gy0 + gy1) / 2) * city.w + x] == GRASS) city.tiles[((gy0 + gy1) / 2) * city.w + x] = PLAZA;
                    city.openAreas.add(new float[]{(gx0 + gx1) / 2f * T, (gy0 + gy1) / 2f * T, 0});
                } else if (type == NB_WORKS) {
                    // A yard with a big distribution depot in the middle.
                    city.fill(gx0, gy0, gw, gh, LOT);
                    if (gw >= 12 && gh >= 12) lotGen.addLot(gx0 + 3, gy0 + 3, gw - 6, gh - 6, WAREHOUSE);
                    city.openAreas.add(new float[]{(gx0 + 1.5f) * T, (gy0 + 1.5f) * T, 2});
                } else if (type == NB_ESTATE && small && city.rnd.nextFloat() < 0.5f) {
                    lotGen.parking(gx0, gy0, gw, gh);
                    city.openAreas.add(new float[]{(gx0 + gx1) / 2f * T, (gy0 + gy1) / 2f * T, 2});
                } else {
                    if (gw > 20 && gh > 20) {
                        // A big green is a park, with a playground in a corner, not a sports complex.
                        lotGen.park(gx0, gy0, gw, gh);
                        if (city.rnd.nextFloat() < 0.5f) lotGen.bandstand(gx0, gy0, gw, gh);
                        int qw = Math.min(8, gw / 3), qh = Math.min(8, gh / 3);
                        for (int j = gy0 + 1; j < gy0 + 1 + qh; j++)
                            for (int i = gx0 + 1; i < gx0 + 1 + qw; i++) if (city.tiles[j * city.w + i] == TREE) city.tiles[j * city.w + i] = GRASS;
                        city.addDecor(D_PLAYGROUND, (gx0 + 1) * T + 3, (gy0 + 1) * T + 3, (gx0 + 1 + qw) * T - 2, (gy0 + 1 + qh) * T - 2, 0);
                    } else lotGen.parkVariant(gx0, gy0, gw, gh);
                    city.openAreas.add(new float[]{(gx0 + gx1) / 2f * T, (gy0 + gy1) / 2f * T, 0});
                }
            }
        }
        // A gated estate: a fence all round inside the pavement, open only where the road goes in, and a gatehouse.
        if (type == NB_GATED) {
            for (int y = y0 + 1; y < y1 - 1; y++)
                for (int x = x0 + 1; x < x1 - 1; x++) {
                    if (x != x0 + 1 && x != x1 - 2 && y != y0 + 1 && y != y1 - 2) continue;
                    if (city.tiles[y * city.w + x] == GRASS) city.tiles[y * city.w + x] = FENCE;
                }
            for (int y = y0 + 1; y < y1 - 1; y++)
                for (int x = x0 + 1; x < x1 - 1; x++) {
                    if (city.tiles[y * city.w + x] != SIDEWALK || (x != x0 + 1 && x != x1 - 2 && y != y0 + 1 && y != y1 - 2)) continue;
                    // Just inside the gate, beside the pavement.
                    int gx = x == x0 + 1 ? x + 1 : x == x1 - 2 ? x - 2 : x + 1, gy = y == y0 + 1 ? y + 1 : y == y1 - 2 ? y - 2 : y + 1;
                    if (allType(gx, gy, 2, 2, GRASS)) {
                        lotGen.addLot(gx, gy, 2, 2, KIOSK);
                        break;
                    }
                }
        }
        switch (type) {
            case NB_ESTATE:
                frontage(x0, y0, x1, y1, keep, APARTMENT, 8, 11, 2, 3, 0f);
                frontage(x0, y0, x1, y1, keep, HOUSE, 4, 5, 1, 2, 0.06f);
                break;
            case NB_ACRES: {
                frontage(x0, y0, x1, y1, keep, HOUSE, 5, 6, 5, 8, 0.05f);
                // Paddocks and crops out the back.
                int fx0 = x0 + 16, fy0 = y0 + 16, fx1 = x1 - 16, fy1 = y1 - 16;
                if (fx1 - fx0 >= 10 && fy1 - fy0 >= 10) {
                    boolean wide = fx1 - fx0 >= fy1 - fy0;
                    int n = 1 + (Math.max(fx1 - fx0, fy1 - fy0) >= 24 ? 1 : 0), span = (wide ? fx1 - fx0 : fy1 - fy0) / n;
                    for (int k = 0; k < n; k++) {
                        int a = (wide ? fx0 : fy0) + k * span, b2 = a + span - (k < n - 1 ? 2 : 0);
                        int cx0 = wide ? a : fx0, cy0 = wide ? fy0 : a, cx1 = wide ? b2 : fx1, cy1 = wide ? fy1 : b2;
                        boolean clear = true;
                        for (int j = cy0; j < cy1 && clear; j++)
                            for (int i = cx0; i < cx1 && clear; i++) if (city.tiles[j * city.w + i] != GRASS && city.tiles[j * city.w + i] != TREE) clear = false;
                        if (!clear) continue;
                        city.fill(cx0, cy0, cx1 - cx0, cy1 - cy0, GRASS);
                        city.addDecor(D_CROPS, cx0 * T + 3, cy0 * T + 3, cx1 * T - 3, cy1 * T - 3, city.rnd.nextInt(3));
                        for (int j = cy0 - 1; j <= cy1; j++)
                            for (int i = cx0 - 1; i <= cx1; i++) if (city.tiles[j * city.w + i] == TREE) city.tiles[j * city.w + i] = GRASS;
                    }
                    keep = new int[]{fx0 - 1, fy0 - 1, fx1 + 1, fy1 + 1};
                }
                break;
            }
            case NB_WORKS:
                frontage(x0, y0, x1, y1, keep, WAREHOUSE, 9, 13, 3, 3, 0f);
                frontage(x0, y0, x1, y1, keep, OFFICE, 6, 8, 2, 2, 0.1f);
                break;
            case NB_TRAILERS:
                // ((27.5) Mobile homes, close together.)
                frontage(x0, y0, x1, y1, keep, HOUSE, 3, 4, 1, 1, 0.04f);
                break;
            case NB_TOWERS:
                frontage(x0, y0, x1, y1, keep, APARTMENT, 6, 9, 3, 4, 0f);
                break;
            case NB_MARKET:
                // Little shops shoulder to shoulder, flats over some.
                frontage(x0, y0, x1, y1, keep, SHOP, 3, 5, 0, 0, 0f);
                frontage(x0, y0, x1, y1, keep, APARTMENT, 4, 5, 0, 0, 0f);
                break;
            case NB_QUAD:
                frontage(x0, y0, x1, y1, keep, OFFICE, 6, 9, 2, 2, 0f);
                frontage(x0, y0, x1, y1, keep, APARTMENT, 5, 7, 2, 2, 0.1f);
                break;
            case NB_CIRCUS:
                // Tall terraced town houses round the circus and along the diagonals.
                frontage(x0, y0, x1, y1, keep, gen.curDistrict == DT_SUBURB || gen.curDistrict == DT_PARKSIDE ? HOUSE : APARTMENT, 4, 6, 1, 0, 0f);
                frontage(x0, y0, x1, y1, keep, HOUSE, 4, 5, 1, 1, 0.05f);
                break;
            case NB_TERRACES:
                // Terraces: narrow houses wall to wall down both sides of every street.
                frontage(x0, y0, x1, y1, keep, HOUSE, 3, 4, 1, 0, 0f);
                break;
            case NB_LOFTS:
                frontage(x0, y0, x1, y1, keep, WAREHOUSE, 7, 10, 0, 1, 0f);
                frontage(x0, y0, x1, y1, keep, APARTMENT, 5, 7, 0, 1, 0.05f);
                break;
            case NB_GATED:
                // Inside the fence, every house faces the estate's own roads.
                frontage(x0 + 1, y0 + 1, x1 - 1, y1 - 1, keep, HOUSE, 4, 5, 1, 2, 0.06f);
                break;
            default:
                frontage(x0, y0, x1, y1, keep, HOUSE, 4, 5, 1, 2, 0.06f);
                break;
        }
        // Trees in the back gardens (orchards and windbreaks out on the big plots).
        float trees = type == NB_ACRES ? 0.22f : 0.1f;
        for (int y = y0 + 1; y < y1 - 1; y++)
            for (int x = x0 + 1; x < x1 - 1; x++) {
                if (keep != null && inside(x, y, keep[0], keep[1], keep[2], keep[3])) continue;
                if (city.tiles[y * city.w + x] == GRASS && city.rnd.nextFloat() < trees && !city.hasNeighbor(x, y, TREE)
                        && !city.hasNeighbor(x, y, BUILDING) && !city.hasNeighbor(x, y, ROAD) && !city.hasNeighbor(x, y, SIDEWALK))
                    city.tiles[y * city.w + x] = TREE;
            }
    }

    static boolean inside(int x, int y, int x0, int y0, int x1, int y1) {
        return x >= x0 && y >= y0 && x < x1 && y < y1;
    }

    boolean allType(int x, int y, int fw, int fh, byte t) {
        for (int j = y; j < y + fh; j++)
            for (int i = x; i < x + fw; i++)
                if (i < 0 || j < 0 || i >= city.w || j >= city.h || city.tiles[j * city.w + i] != t) return false;
        return true;
    }

    /**
     * Homes facing the roads: behind every bit of pavement with a road beyond it, one set back a garden's depth
     * (setback tiles) from the pavement, sizes min..max, at least gap tiles from the next. A few plots stay empty.
     */
    void frontage(int x0, int y0, int x1, int y1, int[] keep, int kind, int min, int max, int setback, int gap, float empty) {
        int[] ddx = {1, -1, 0, 0}, ddy = {0, 0, 1, -1};
        for (int y = y0; y < y1; y++)
            for (int x = x0; x < x1; x++) {
                if (city.tiles[y * city.w + x] != SIDEWALK) continue;
                for (int k = 0; k < 4; k++) {
                    int dx = ddx[k], dy = ddy[k];
                    if (!city.paved(x + dx, y + dy)) continue;
                    int hw = min + city.rnd.nextInt(max - min + 1), hd = min + city.rnd.nextInt(max - min + 1);
                    // ((11.5) Not all in one ruler-straight line: each a little further back or nearer than the next.)
                    int back = city.cfg.civic >= 28 && setback >= 2 ? Math.max(1, setback + city.rnd.nextInt(3) - 1) : setback;
                    int fx = x - dx * (1 + back), fy = y - dy * (1 + back);
                    int lx, ly, lw, lh;
                    if (dx != 0) {
                        lw = hd;
                        lh = hw;
                        lx = dx > 0 ? fx - hd + 1 : fx;
                        ly = y - hw / 2;
                    } else {
                        lw = hw;
                        lh = hd;
                        lx = x - hw / 2;
                        ly = dy > 0 ? fy - hd + 1 : fy;
                    }
                    if (lx < x0 + 1 || ly < y0 + 1 || lx + lw > x1 - 1 || ly + lh > y1 - 1) continue;
                    if (keep != null && lx < keep[2] && lx + lw > keep[0] && ly < keep[3] && ly + lh > keep[1]) continue;
                    if (!allType(lx, ly, lw, lh, GRASS)) continue;
                    boolean clear = true;
                    for (int j = ly - gap; j < ly + lh + gap && clear; j++)
                        for (int i = lx - gap; i < lx + lw + gap && clear; i++)
                            if (i >= 0 && j >= 0 && i < city.w && j < city.h && city.tiles[j * city.w + i] == BUILDING) clear = false;
                    if (!clear) continue;
                    if (city.rnd.nextFloat() < empty) {
                        // An empty plot: mark it taken with a tree so the next house doesn't crowd in.
                        city.tiles[(ly + lh / 2) * city.w + lx + lw / 2] = TREE;
                        break;
                    }
                    lotGen.addLot(lx, ly, lw, lh, kind);
                    // A drive from the road to the house.
                    for (int s = 1; s <= back; s++) {
                        int ax = x - dx * s, ay = y - dy * s;
                        if (city.tiles[ay * city.w + ax] == GRASS) city.tiles[ay * city.w + ax] = SIDEWALK;
                    }
                    if (kind == HOUSE && setback >= 4 && city.rnd.nextFloat() < 0.4f) {
                        // Out on the big plots, a barn behind the house.
                        int bx = dx > 0 ? lx - 6 : dx < 0 ? lx + lw + 2 : lx, by = dy > 0 ? ly - 6 : dy < 0 ? ly + lh + 2 : ly;
                        if (bx > x0 + 1 && by > y0 + 1 && bx + 4 < x1 - 1 && by + 4 < y1 - 1 && allType(bx - 1, by - 1, 6, 6, GRASS))
                            lotGen.addLot(bx, by, 4, 4, BARN);
                    }
                    break;
                }
            }
    }

    /**
     * Gives a town in the country a ragged, organic edge: blocks outside a wobbly outline become fields again,
     * and the roads that only served them go too. What's left is one connected town with streets running out
     * of it, and the town bounds shrink to fit.
     */
    void erodeTown(int bx0, int by0, int bx1, int by1) {
        float cx = (bx0 + bx1) / 2f, cy = (by0 + by1) / 2f, rad = (bx1 - bx0) / 2f, radY = city.cfg.civic >= 18 ? (by1 - by0) / 2f : rad;
        // (Since 10.10 every town but a small one fills its square out to a ragged edge: the country round it
        // is the extra room the bigger maps have.)
        boolean square = city.cfg.nature() ? city.cfg.preset() != 6 && city.cfg.preset() != 9 : city.w >= 400;
        float[] amp = new float[4], ph = new float[4];
        // (10.34) Livelier outlines: rounder or squarer from one map to the next, lopsided, with bays and bulges.
        boolean lively = city.cfg.civic >= 18;
        for (int k = 0; k < 4; k++) {
            amp[k] = square ? (lively ? 0.02f + city.rnd.nextFloat() * 0.06f : 0.01f + city.rnd.nextFloat() * 0.025f)
                    : 0.04f + city.rnd.nextFloat() * (k == 0 ? 0.14f : 0.08f);
            ph[k] = city.rnd.nextFloat() * (float) Math.PI * 2;
        }
        float stretch = square ? 1f : 0.8f + city.rnd.nextFloat() * 0.4f;
        double power = lively && square ? 2.6 + city.rnd.nextFloat() * 4 : 6;
        float lobe = lively ? 0.04f + city.rnd.nextFloat() * 0.08f : 0, lobePh = city.rnd.nextFloat() * (float) Math.PI * 2;
        List<int[]> kept = new ArrayList<int[]>();
        for (int[] b : city.blocks) {
            float x = (b[0] + b[2]) / 2f - cx, y = ((b[1] + b[3]) / 2f - cy) * stretch;
            float a = (float) Math.atan2(y, x), d = (float) Math.sqrt(x * x + y * y) / rad;
            // A massive town fills its square out to a ragged edge (rounded corners, not a circle in a field);
            // a small town just loses its corners.
            if (square) {
                float ax = Math.abs(x) / rad, ay = Math.abs(y) / radY;
                d = (float) Math.pow(Math.pow(ax, power) + Math.pow(ay, power), 1 / power);
            }
            float r = square ? (lively ? 1.1f : 1.06f) : 1.02f;
            for (int k = 0; k < 4; k++) r += amp[k] * (float) Math.sin((k + 2) * a + ph[k]);
            // (One side bulging out, the other pulled in.)
            r += lobe * (float) Math.cos(a - lobePh);
            boolean keep = d < r && !(d > r - 0.14f && city.rnd.nextFloat() < (square ? 0.12f : 0.3f));
            if (keep) kept.add(b);
            else city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], GRASS);
        }
        trimTo(kept, bx0, by0, bx1, by1);
    }

    /**
     * ((27.5) Map version 40) A big town's outline its own: one or two of its corners left as open country, never the
     * airport's. Each corner a different size, so no two towns have the same shape.
     */
    void notchTown(int bx0, int by0, int bx1, int by1, java.util.Random r) {
        int w = bx1 - bx0, h = by1 - by0;
        int[] air = city.airport;
        List<int[]> notches = new ArrayList<int[]>();
        int want = 1 + r.nextInt(2), start = r.nextInt(4);
        for (int k = 0; k < 4 && notches.size() < want; k++) {
            int c = (start + k) % 4;
            boolean right = (c & 1) != 0, bottom = (c & 2) != 0;
            int nw = Math.round(w * (0.16f + r.nextFloat() * 0.16f)), nh = Math.round(h * (0.16f + r.nextFloat() * 0.16f));
            int x0 = right ? bx1 - nw : bx0, y0 = bottom ? by1 - nh : by0;
            int[] n = {x0, y0, x0 + nw, y0 + nh};
            if (air != null && n[0] < air[2] + 6 && air[0] - 6 < n[2] && n[1] < air[3] + 6 && air[1] - 6 < n[3]) continue;
            notches.add(n);
        }
        if (notches.isEmpty()) return;
        List<int[]> kept = new ArrayList<int[]>();
        for (int[] b : city.blocks) {
            float x = (b[0] + b[2]) / 2f, y = (b[1] + b[3]) / 2f;
            boolean out = false;
            for (int[] n : notches) if (x >= n[0] && x < n[2] && y >= n[1] && y < n[3]) out = true;
            if (out) city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], GRASS);
            else kept.add(b);
        }
        trimTo(kept, bx0, by0, bx1, by1);
    }

    /** What's left of town after its edge is worn away: the roads out in the new fields dug up, stray bits gone. */
    private void trimTo(List<int[]> kept, int bx0, int by0, int bx1, int by1) {
        // Roads more than a few tiles from the town are dug up.
        boolean[] near = new boolean[city.w * city.h];
        for (int[] b : kept)
            for (int y = Math.max(0, b[1] - 3); y < Math.min(city.h, b[3] + 3); y++)
                for (int x = Math.max(0, b[0] - 3); x < Math.min(city.w, b[2] + 3); x++) near[y * city.w + x] = true;
        for (int y = by0; y < by1; y++)
            for (int x = bx0; x < bx1; x++) {
                int i = y * city.w + x;
                if ((city.tiles[i] == ROAD || city.tiles[i] == SIDEWALK) && !near[i]) gen.unroad(i);
            }
        // Keep the big connected pieces of road network; stray bits (and the blocks only they reach) go.
        int[] comp = new int[city.w * city.h];
        Arrays.fill(comp, -1);
        List<Integer> sizes = new ArrayList<Integer>();
        int[] q = new int[city.w * city.h];
        for (int s = 0; s < city.w * city.h; s++) {
            if (city.tiles[s] != ROAD || comp[s] >= 0) continue;
            int id = sizes.size(), head = 0, tail = 0;
            q[tail++] = s;
            comp[s] = id;
            while (head < tail) {
                int i = q[head++], x = i % city.w, y = i / city.w;
                int[] nb = {x > 0 ? i - 1 : -1, x < city.w - 1 ? i + 1 : -1, y > 0 ? i - city.w : -1, y < city.h - 1 ? i + city.w : -1};
                for (int j : nb)
                    if (j >= 0 && comp[j] < 0 && city.tiles[j] == ROAD) {
                        comp[j] = id;
                        q[tail++] = j;
                    }
            }
            sizes.add(tail);
        }
        int biggest = 0;
        for (int sz : sizes) biggest = Math.max(biggest, sz);
        for (int i = 0; i < city.w * city.h; i++)
            if (comp[i] >= 0 && sizes.get(comp[i]) < biggest / 5) gen.unroad(i);
        List<int[]> connected = new ArrayList<int[]>();
        for (int[] b : kept) {
            boolean touches = false;
            for (int y = Math.max(0, b[1] - 1); y <= Math.min(city.h - 1, b[3]) && !touches; y++)
                for (int x = Math.max(0, b[0] - 1); x <= Math.min(city.w - 1, b[2]) && !touches; x++)
                    if (city.tiles[y * city.w + x] == ROAD) touches = true;
            if (touches) connected.add(b);
            else city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], GRASS);
        }
        city.blocks.clear();
        city.blocks.addAll(connected);
        for (int i = city.streets.size() - 1; i >= 0; i--) {
            Street st = city.streets.get(i);
            boolean any = false;
            for (int y = st.y0; y < st.y1 && !any; y++)
                for (int x = st.x0; x < st.x1 && !any; x++)
                    if (x >= 0 && y >= 0 && x < city.w && y < city.h && city.tiles[y * city.w + x] == ROAD) any = true;
            if (!any) city.streets.remove(i);
        }
        // Fields where town used to be belong to the countryside, and the districts' names move to what's left.
        if (gen.countryDistrict >= 0) {
            float[] sx = new float[city.districts.size()], sy = new float[city.districts.size()];
            for (District d : city.districts) d.tiles = 0;
            for (int i = 0; i < city.w * city.h; i++) {
                if (!near[i]) city.districtAt.set(i, (byte) (gen.countryDistrict));
                int id = city.districtAt.get(i);
                city.districts.get(id).tiles++;
                sx[id] += i % city.w;
                sy[id] += i / city.w;
            }
            for (int k = 0; k < city.districts.size(); k++) {
                District d = city.districts.get(k);
                if (k == gen.countryDistrict || d.tiles == 0) continue;
                d.cx = (sx[k] / d.tiles + 0.5f) * T;
                d.cy = (sy[k] / d.tiles + 0.5f) * T;
            }
        }
        // The town is now as big as what's left.
        int x0 = city.w, y0 = city.h, x1 = 0, y1 = 0;
        for (int[] b : city.blocks) {
            x0 = Math.min(x0, b[0]);
            y0 = Math.min(y0, b[1]);
            x1 = Math.max(x1, b[2]);
            y1 = Math.max(y1, b[3]);
        }
        if (x1 > x0) {
            city.townX0 = Math.max(0, x0 - 3);
            city.townY0 = Math.max(0, y0 - 3);
            city.townX1 = Math.min(city.w, x1 + 3);
            city.townY1 = Math.min(city.h, y1 + 3);
        }
        // Lanes out into the country start from the ends of the roads on each side of town.
        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        for (int[] d : dirs) {
            int best = -1;
            float bestV = -Float.MAX_VALUE;
            for (int i = 0; i < city.w * city.h; i++) {
                if (city.tiles[i] != ROAD) continue;
                int x = i % city.w, y = i / city.w;
                if (city.railY0 >= 0 && y >= city.railY0 - 3 && y <= city.railY0 + city.railRows + 2) continue;
                // Only a road end with open country ahead of it.
                boolean clear = true;
                for (int k = 1; k <= 8 && clear; k++)
                    for (int t = 0; t < 2 && clear; t++) {
                        int ax = x + d[0] * k + (d[0] == 0 ? t : 0), ay = y + d[1] * k + (d[1] == 0 ? t : 0);
                        if (ax < 0 || ay < 0 || ax >= city.w || ay >= city.h) clear = false;
                        else if (city.tiles[ay * city.w + ax] != GRASS && city.tiles[ay * city.w + ax] != TREE) clear = false;
                    }
                if (!clear) continue;
                float v = x * d[0] + y * d[1] + city.rnd.nextFloat() * 4;
                if (v > bestV) {
                    bestV = v;
                    best = i;
                }
            }
            if (best >= 0) villageEnds.add(new int[]{best % city.w, best / city.w, d[0], d[1]});
        }
    }


    /**
     * Hamlets out in the country: a short street of cottages and a farm or two, with a lane back to town.
     * Each is a named place of its own.
     */
    void hamlets(int count) {
        float tcx = (city.townX0 + city.townX1) / 2f, tcy = (city.townY0 + city.townY1) / 2f;
        city.settlements.add(new float[]{tcx * T, tcy * T});
        List<float[]> made = new ArrayList<float[]>();
        for (int k = 0; k < count; k++) {
            for (int tries = 0; tries < (city.cfg.civic >= 14 && city.cfg.massive() ? 500 : 250); tries++) {
                int cx = 30 + city.rnd.nextInt(Math.max(1, city.w - 60)), cy = 30 + city.rnd.nextInt(Math.max(1, city.h - 60));
                // Clear of the town's own (eroded, irregular) edge rather than its bounding box.
                if (!gen.allOpen(cx - 28, cy - 28, 56, 56)) continue;
                // ((10.33) And well back from the highway.)
                if (gen.byHighway(cx, cy, 34)) continue;
                // (The wild side of the map is left to the hills and the park.)
                if (gen.inWildBand(cx, cy) || gen.inWildBand(cx - 28, cy - 28) || gen.inWildBand(cx + 28, cy + 28)) continue;
                if (city.railY0 >= 0 && Math.abs(cy - city.railY0) < 30) continue;
                boolean far = true;
                for (float[] o : made) if (World.hypot(o[0] - cx, o[1] - cy) < (city.cfg.civic >= 17 ? 64 : city.cfg.civic >= 14 && city.cfg.massive() ? 75 : 90)) far = false;
                if (!far) continue;
                boolean vertical = city.rnd.nextBoolean();
                // (The first two or three, on a big map since 10.20, are small towns: a longer main street with a
                // cross street, their own police and fire stations, and houses all along.)
                boolean town = city.cfg.servicePads() && k < (city.cfg.civic >= 14 && city.cfg.massive() ? (city.w >= 900 ? 10 : city.w >= 650 ? 7 : 5) : city.w >= 600 ? 3 : city.w >= 300 ? 2 : 1)
                        + (city.cfg.civic >= 17 ? (city.w >= 650 ? 4 : 2) : 0);
                boolean big = town && city.cfg.civic >= 14 && city.cfg.massive();
                int len = big ? 60 + city.rnd.nextInt(12) : town ? 46 + city.rnd.nextInt(10) : 30 + city.rnd.nextInt(20);
                Street st = vertical ? new Street(cx - 1, cy - len / 2, cx + 2, cy + len / 2, true, false)
                        : new Street(cx - len / 2, cy - 1, cx + len / 2, cy + 2, false, false);
                gen.carve(st);
                int firstFacility = city.facilities.size();
                if (town) {
                    Street cross = vertical ? new Street(cx - 14, cy + 6, cx + 15, cy + 9, false, false)
                            : new Street(cx + 6, cy - 14, cx + 9, cy + 15, true, false);
                    gen.carve(cross);
                    // (10.30) A bigger town on the Massive maps: a second cross street the other side of the middle,
                    // houses all along all three, and its streets stay paved.
                    // ((10.43) Laid out before the stations, so they're built round it rather than it being run
                    // straight through them.)
                    Street cross2 = !big ? null : vertical ? new Street(cx - 14, cy - 12, cx + 15, cy - 9, false, false)
                            : new Street(cx - 12, cy - 14, cx - 9, cy + 15, true, false);
                    boolean first = city.cfg.civic >= 20;
                    if (cross2 != null && first) gen.carve(cross2);
                    // Police and fire stations facing the main street, on the side away from the cross street.
                    // ((10.43) Between the two cross streets, in a bigger town.)
                    int px = vertical ? cx + 3 : cx - 13, py = vertical ? cy - 13 : cy + 3;
                    int fx = vertical ? cx - 13 : cx - 13, fy = vertical ? cy - 13 : cy - 12;
                    if (first) {
                        px = vertical ? cx + 3 : cx - 7;
                        py = vertical ? cy - 7 : cy + 3;
                        fx = vertical ? cx - 13 : cx - 7;
                        fy = vertical ? cy - 7 : cy - 12;
                    }
                    if (gen.allOpen(px - 1, py - 1, 12, 11)) lotGen.serviceBuilding(px, py, 10, 9, STATION, city.facilities.size() + 1);
                    if (city.cfg.fireStations() && gen.allOpen(fx - 1, fy - 1, 12, 11)) lotGen.serviceBuilding(fx, fy, 10, 9, FIRE_STATION, 0);
                    if (big) {
                        if (!first) gen.carve(cross2);
                        for (Street o : new Street[]{st, cross, cross2}) keepPaved(o);
                        plotsAlong(cross2, cx - 38, cy - 38, cx + 38, cy + 38, 2);
                    }
                    plotsAlong(cross, cx - 32, cy - 32, cx + 32, cy + 32, 2);
                    plotsAlong(st, cx - 32, cy - 32, cx + 32, cy + 32, 2);
                } else if (city.cfg.civic >= 17) {
                    // (10.33) A hamlet is a proper little place too: a cross street with houses along it.
                    Street cross = vertical ? new Street(cx - 12, cy + 4, cx + 13, cy + 6, false, false)
                            : new Street(cx + 4, cy - 12, cx + 6, cy + 13, true, false);
                    gen.carve(cross);
                    plotsAlong(cross, cx - 28, cy - 28, cx + 28, cy + 28, 2);
                }
                plotsAlong(st, cx - 25, cy - 25, cx + 25, cy + 25, 2);
                made.add(new float[]{cx, cy});
                city.settlements.add(new float[]{(cx + 0.5f) * T, (cy + 0.5f) * T});
                // A lane from the end nearer town, back to the nearest road.
                int ex, ey;
                if (vertical) {
                    ex = cx;
                    ey = tcy < cy ? st.y0 - 1 : st.y1;
                } else {
                    ex = tcx < cx ? st.x0 - 1 : st.x1;
                    ey = cy;
                }
                // (10.30: a paved road for a town on the Massive maps, if there's a way across.)
                if (!big || !pavedLink(ex, ey, new int[]{Math.min(st.x0, cx - 14), Math.min(st.y0, cy - 14), Math.max(st.x1, cx + 15), Math.max(st.y1, cy + 15)}))
                    laneTo(ex, ey, (int) tcx, (int) tcy);
                int named = city.districts.size();
                nameHamlet(cx, cy);
                // Its stations go by its name.
                String place = city.districts.size() > named ? city.districts.get(named).name : null;
                for (int f = firstFacility; f < city.facilities.size(); f++) {
                    Facility fa = city.facilities.get(f);
                    fa.satellite = true;
                    if (place != null) fa.name = place + (fa.kind == FACILITY_POLICE ? " Police" : " Fire Dept");
                }
                break;
            }
        }
    }

    /**
     * The edge of town (10.20): houses in their gardens strung along the outside of the road round the town,
     * a second row here and there behind them, instead of open fields right up to the last street.
     */
    void outskirtHouses(int bx0, int by0, int bx1, int by1) {
        for (int side = 0; side < 4; side++) {
            boolean horiz = side < 2;
            int dx = side == 2 ? -1 : side == 3 ? 1 : 0, dy = side == 0 ? -1 : side == 1 ? 1 : 0;
            int from = horiz ? bx0 : by0, to = horiz ? bx1 : by1;
            for (int p = from + 2 + city.rnd.nextInt(4); p < to - 6; p += 8 + city.rnd.nextInt(4)) {
                // Out from the edge of town, past the last road (or lane) to the first open ground.
                int sx = horiz ? p + 3 : side == 2 ? bx0 : bx1 - 1, sy = !horiz ? p + 3 : side == 0 ? by0 : by1 - 1;
                // (From a little inside the town's bounds, as its edge is ragged: the first open ground past a road
                // with fields beyond it.)
                int edge = Integer.MIN_VALUE;
                boolean road = false;
                for (int k = -24; k < 30 && edge == Integer.MIN_VALUE; k++) {
                    int qx = sx + dx * k, qy = sy + dy * k;
                    if (qx < 1 || qy < 1 || qx >= city.w - 1 || qy >= city.h - 1) break;
                    byte t = city.tiles[qy * city.w + qx];
                    if (t == ROAD || t == SIDEWALK || t == DIRT) road = true;
                    else if (road && t == GRASS) {
                        boolean open = true;
                        for (int j = 1; j <= 8 && open; j++) {
                            int ox = qx + dx * j, oy = qy + dy * j;
                            if (ox < 1 || oy < 1 || ox >= city.w - 1 || oy >= city.h - 1 || city.tiles[oy * city.w + ox] != GRASS) open = false;
                        }
                        if (open) edge = k;
                        else road = false;
                    } else road = false;
                }
                if (edge == Integer.MIN_VALUE) continue;
                // ((10.33) Deeper rows of them since 10.33: less open field right up to town.)
                for (int row = 0; row < (city.cfg.civic >= 17 ? 3 : 2); row++) {
                    if (row >= 1 && city.rnd.nextFloat() > (city.cfg.civic >= 17 ? 0.8f : 0.45f)) break;
                    int out = edge + 1 + row * 8;
                    // (The 6x6 garden, its near edge `out` tiles from the edge of town.)
                    int x = horiz ? p : side == 2 ? sx - out - 5 : sx + out;
                    int y = !horiz ? p : side == 0 ? sy - out - 5 : sy + out;
                    if (!allGrass(x, y, 6, 6)) break;
                    lotGen.addLot(x + 1, y + 1, 4, 4, HOUSE);
                }
            }
        }
    }

    /** A winding two-tile dirt lane from (x, y) towards (tx, ty), until it meets a road. */
    void laneTo(int x, int y, int tx, int ty) {
        for (int step = 0; step < city.w * 3; step++) {
            int dx = tx - x, dy = ty - y;
            if (dx == 0 && dy == 0) break;
            boolean alongX = Math.abs(dx) * (0.7f + city.rnd.nextFloat() * 0.6f) > Math.abs(dy);
            if (alongX) x += Integer.signum(dx);
            else y += Integer.signum(dy);
            boolean hit = false;
            // ((10.33) Never on to the highway: a lane that runs up to it stops short.)
            if (gen.byHighway(x, y, 2) || gen.byHighway(x + 1, y + 1, 2)) break;
            for (int j = y; j < y + 2; j++)
                for (int i = x; i < x + 2; i++) {
                    if (i < 0 || j < 0 || i >= city.w || j >= city.h) continue;
                    int k = j * city.w + i;
                    if (city.tiles[k] == ROAD) hit = step > 2;
                    else if (city.tiles[k] == RAIL) {
                        // A level crossing.
                        city.tiles[k] = ROAD;
                        city.roadDir[k] = 1;
                    } else if (city.tiles[k] == GRASS || city.tiles[k] == TREE) city.tiles[k] = DIRT;
                    else if (city.tiles[k] != DIRT && step > 2) hit = true;
                }
            if (hit) break;
        }
    }

    void nameHamlet(int cx, int cy) {
        if (city.districtAt == null) return;
        District d = new District();
        d.type = DT_PARKSIDE;
        d.name = city.country.districtWords[city.rnd.nextInt(city.country.districtWords.length)] + city.country.hamletEnds[city.rnd.nextInt(city.country.hamletEnds.length)];
        for (District o : city.districts) if (o.name.equals(d.name)) d.name = d.name + " Village";
        d.x = cx;
        d.y = cy;
        d.cx = (cx + 0.5f) * T;
        d.cy = (cy - 8) * T;
        int id = city.districts.size();
        if (id > 120) return;
        city.districts.add(d);
        for (int y = Math.max(0, cy - 16); y < Math.min(city.h, cy + 16); y++)
            for (int x = Math.max(0, cx - 16); x < Math.min(city.w, cx + 16); x++) {
                if ((x - cx) * (x - cx) + (y - cy) * (y - cy) > 16 * 16) continue;
                District old = city.districts.get(city.districtAt.get(y * city.w + x));
                old.tiles--;
                city.districtAt.set(y * city.w + x, (byte) (id));
                d.tiles++;
            }
    }

    boolean allGrass(int x, int y, int fw, int fh) {
        for (int j = y; j < y + fh; j++)
            for (int i = x; i < x + fw; i++)
                if (i < 1 || j < 1 || i >= city.w - 1 || j >= city.h - 1 || city.tiles[j * city.w + i] != GRASS) return false;
        return true;
    }

    /** A farm: the farmhouse, a barn and a silo in the yard, and fields of crops. */
    void farm(int x, int y, int bw, int bh) {
        city.fill(x, y, bw, bh, GRASS);
        boolean wide = bw >= bh;
        int yard = 8;
        int yx = x, yy = y;
        lotGen.addLot(yx + 1, yy + 1, 5, 4, HOUSE);
        lotGen.addFacilityLot(wide ? yx + 1 : yx + 7, wide ? yy + 7 : yy + 1, 6, 4, BARN, 1);
        if (wide ? bh >= 15 : bw >= 15) lotGen.addFacilityLot(wide ? yx + 4 : yx + 14, wide ? yy + 12 : yy + 4, 2, 2, SILO, 3);
        int fx = wide ? x + yard + 1 : x, fy = wide ? y : y + yard + 1;
        int fw = wide ? bw - yard - 1 : bw, fh = wide ? bh : bh - yard - 1;
        // Split the land into two or three fields with different crops.
        int fields = Math.max(1, Math.min(3, (wide ? fw : fh) / 10));
        for (int k = 0; k < fields; k++) {
            float a0 = (wide ? fx : fy) + (wide ? fw : fh) * k / (float) fields, a1 = (wide ? fx : fy) + (wide ? fw : fh) * (k + 1) / (float) fields;
            if (wide) city.addDecor(D_CROPS, a0 * T + 3, fy * T + 3, a1 * T - 3, (fy + fh) * T - 3, city.rnd.nextInt(3));
            else city.addDecor(D_CROPS, fx * T + 3, a0 * T + 3, (fx + fw) * T - 3, a1 * T - 3, city.rnd.nextInt(3));
        }
        if (city.cfg.civic >= 11) {
            if (gen.fieldTile == null) gen.fieldTile = new boolean[city.w * city.h];
            for (int j = fy; j < fy + fh; j++)
                for (int i = fx; i < fx + fw; i++) if (i >= 0 && j >= 0 && i < city.w && j < city.h) gen.fieldTile[j * city.w + i] = true;
        }
    }
}
