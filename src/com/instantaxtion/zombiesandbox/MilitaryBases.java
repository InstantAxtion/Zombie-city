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
 * (27.3) The army's bases: the town's own, the big one out in the country, the Military Town's great base, and what
 * each kind has on it. Taken out of City.java as it stood; the city itself (its tiles, buildings and everything the
 * game asks of it) is `city`.
 */
final class MilitaryBases extends CityPart {
    MilitaryBases(City city) {
        super(city);
    }

    /** A fenced compound with gates, barracks, a helipad, tents, a watchtower and army trucks. */
    void militaryBase(int x, int y, int bw, int bh) {
        militaryBase(x, y, bw, bh, 13);
    }

    Facility militaryBase(int x, int y, int bw, int bh, long salt) {
        city.fill(x, y, bw, bh, BASE);
        for (int i = x; i < x + bw; i++) {
            city.tiles[y * city.w + i] = FENCE;
            city.tiles[(y + bh - 1) * city.w + i] = FENCE;
        }
        for (int j = y; j < y + bh; j++) {
            city.tiles[j * city.w + x] = FENCE;
            city.tiles[j * city.w + x + bw - 1] = FENCE;
        }
        int gx = x + bw / 2 - 1;
        for (int i = gx; i < gx + 3; i++) {
            city.tiles[y * city.w + i] = BASE;
            city.tiles[(y + bh - 1) * city.w + i] = BASE;
        }
        int gy = y + bh / 2 - 1;
        for (int j = gy; j < gy + 3; j++) city.tiles[j * city.w + x + bw - 1] = BASE;

        int ax = x + 1, ay = y + 1, aw = bw - 2, ah = bh - 2;
        // ((15.5) The Military Town's great base: rows and rows of barracks, an airfield, and a gate on every side.)
        boolean great = gen.bigBase != null && x == gen.bigBase[0] + 1 && y == gen.bigBase[1] + 1;
        if (great) for (int j = gy0(bh, y); j < gy0(bh, y) + 3; j++) city.tiles[j * city.w + x] = BASE;
        // Barracks along the top, leaving a lane by the fence and around the gate.
        int left = ax + 2, right = ax + aw - 2;
        if (aw >= 16) {
            for (int row = 0; row < (great ? 6 : 2) && ay + 1 + row * 6 + 4 <= ay + ah / 2; row++) {
                int by = ay + 2 + row * 6;
                int mid = gx - 2;
                if (mid - left >= 5) lotGen.addFacilityLot(left, by, mid - left, 4, BARRACKS, 1);
                if (right - (gx + 5) >= 5) lotGen.addFacilityLot(gx + 5, by, right - (gx + 5), 4, BARRACKS, 1);
            }
        } else {
            // Narrow base: long barracks down both sides of the parade ground.
            int len = Math.max(3, ah / 2 - 1);
            lotGen.addFacilityLot(ax + 1, ay + 2, 4, len, BARRACKS, 1);
            lotGen.addFacilityLot(ax + aw - 5, ay + 2, 4, len, BARRACKS, 1);
        }
        lotGen.addFacilityLot(ax, ay + ah - 2, 2, 2, TOWER, 3);
        lotGen.addFacilityLot(ax + aw - 2, ay, 2, 2, TOWER, 3);
        // The open lower half depends on what kind of base it is (trucks are parked along the bottom right).
        Random bt = new Random(city.cfg.seed * 977L + salt);
        // ((10.52) Never the same kind twice on one map.)
        int type = great ? BT_AIR : forceBaseType >= 0 ? forceBaseType : bt.nextInt(BASE_TYPES.length);
        for (int k = 0; k < 8 && city.cfg.civic >= 23 && !great; k++) {
            boolean dup = false;
            for (Facility f : city.facilities) if (f.kind == FACILITY_BASE && f.baseType == type) dup = true;
            if (!dup) break;
            type = (type + 1) % BASE_TYPES.length;
        }
        // ((15.5) The great base: the airfield on the one side, an armoured vehicle park on the other.)
        if (great) {
            int split = aw * 3 / 5;
            baseLayout(BT_AIR, ax, ay, split, ah, bt);
            baseLayout(BT_ARMOUR, ax + split, ay, aw - split, ah, bt);
        } else baseLayout(type, ax, ay, aw, ah, bt);
        for (int i = ax + aw / 2 + 2; i < ax + aw - 1; i++) {
            int ty = ay + ah - 2;
            if (city.rnd.nextFloat() < 0.75f && city.tiles[ty * city.w + i] == BASE && !city.hasNeighbor(i, ty, CAR)) {
                city.tiles[ty * city.w + i] = CAR;
                city.carKind.set(ty * city.w + i, (byte) (2));
            }
        }
        float cx = (ax + aw / 2f) * T, cy = (ay + ah * 0.62f) * T;
        float[] c = city.findWalkable(cx, cy);
        if (c == null) c = new float[]{cx, cy};
        float r = Math.min(great ? 320 : 110, Math.min(aw, ah) * T * 0.42f);
        String bn = city.country.bases[city.rnd.nextInt(city.country.bases.length)];
        // (Two bases on a map since 10.30: never the same name.)
        for (int k = 0; k < 12 && city.cfg.civic >= 14; k++) {
            boolean taken = false;
            for (Facility f : city.facilities) if (f.kind == FACILITY_BASE && f.name.startsWith(bn)) taken = true;
            if (!taken) break;
            bn = city.country.bases[city.rnd.nextInt(city.country.bases.length)];
        }
        Facility base = new Facility(FACILITY_BASE, c[0], c[1], r, (gx + 1.5f) * T, (y + bh - 2.5f) * T, bn);
        base.baseType = type;
        if (type != BT_GARRISON) base.name += " (" + BASE_TYPES[type] + ")";
        // Two guards inside each gate.
        float[][] gates = {{(gx + 1.5f) * T, (y + bh - 2.5f) * T}, {(gx + 1.5f) * T, (y + 1.5f) * T},
                {(x + bw - 2.5f) * T, (gy + 1.5f) * T}};
        for (int i = 0; i < gates.length; i++) {
            boolean across = i < 2;
            for (int k = -1; k <= 1; k += 2) {
                float[] p = city.findWalkable(gates[i][0] + (across ? k * 14 : 0), gates[i][1] + (across ? 0 : k * 14));
                if (p != null) base.posts.add(p);
            }
        }
        city.facilities.add(base);
        return base;
    }

    /** ((22.9) A kind of base to lay out instead of a random one (-1 for random). */
    int forceBaseType = -1;

    /**
     * (10.30) On the Massive maps: a big army base out in open country, well away from the towns, fenced, with
     * its barracks, vehicles and the rest. A paved road runs from its gate to the nearest road, and the army
     * keeps two checkpoints on it, a barrier, sandbags and a hut, one near the gate and one where it joins.
     */
    void outskirtsBase() {
        int bw = city.w >= 650 ? 70 : 58, bh = city.w >= 650 ? 48 : 40;
        // ((10.52) Bigger on the Massive maps, and a smaller one on the Default ones, closer in.)
        boolean small = !city.cfg.massive();
        if (city.cfg.civic >= 23) {
            bw = small ? 46 : city.w >= 650 ? 86 : 72;
            bh = small ? 32 : city.w >= 650 ? 58 : 48;
        }
        int clear = small ? 10 : 40;
        // ((22.9) Desert City: a base of its own size, nearer the edge of town, with its test range behind it.)
        int rh = 0;
        if (city.desert) {
            bw = city.w >= 650 ? 72 : 54;
            bh = city.w >= 650 ? 46 : 36;
            rh = bh + 6;
            clear = 12;
        }
        float tcx = (city.townX0 + city.townX1) / 2f, tcy = (city.townY0 + city.townY1) / 2f;
        int[] prev = new int[city.w * city.h];
        for (int tries = 0; tries < 1500; tries++) {
            int x = 14 + city.rnd.nextInt(Math.max(1, city.w - bw - 28)), y = 14 + city.rnd.nextInt(Math.max(1, city.h - bh - 28));
            if (city.desert && y - rh < 14) continue;
            if (!gen.allOpen(x - 6, y - 6 - rh, bw + 12, bh + 12 + rh)) continue;
            // (Kept off the wild side of the map, the hills and the park, unless there's nowhere else.)
            // ((10.52) The smaller one on a Default map never: there's not the room for the park there as well.)
            if ((tries < 900 || small) && (gen.inWildBand(x, y - rh, small) || gen.inWildBand(x + bw, y - rh, small) || gen.inWildBand(x, y, small) || gen.inWildBand(x + bw, y + bh, small) || gen.inWildBand(x + bw, y, small) || gen.inWildBand(x, y + bh, small))) continue;
            if (city.railY0 >= 0 && city.railY0 + city.railRows >= y - 8 && city.railY0 <= y + bh + 8) continue;
            // (Out in the country, not on the edge of town.)
            if (x + bw > city.townX0 - clear && x < city.townX1 + clear && y + bh > city.townY0 - clear - rh && y - rh < city.townY1 + clear) continue;
            // The gate that faces town.
            int gx = x + bw / 2 - 1, gy = y + bh / 2 - 1;
            int[][] gates = {{gx + 1, y + bh, 0, 1}, {gx + 1, y - 1, 0, -1}, {x + bw, gy + 1, 1, 0}};
            int[] gate = gates[0];
            float gd = Float.MAX_VALUE;
            for (int[] g : gates) {
                // (The test range is behind the top gate.)
                if (city.desert && g[3] < 0) continue;
                float d = (float) World.hypot(g[0] - tcx, g[1] - tcy);
                if (d < gd) {
                    gd = d;
                    gate = g;
                }
            }
            // A straight stretch out from the gate (the first checkpoint is on it), then the way across open
            // country to the nearest road (not the highway).
            int stub = 9;
            Street gateRoad = gate[3] > 0 ? new Street(gate[0] - 1, gate[1], gate[0] + 2, gate[1] + stub, true, true)
                    : gate[3] < 0 ? new Street(gate[0] - 1, gate[1] - stub + 1, gate[0] + 2, gate[1] + 1, true, true)
                    : new Street(gate[0], gate[1] - 1, gate[0] + stub, gate[1] + 2, false, true);
            if (!gen.allOpen(gateRoad.x0, gateRoad.y0, gateRoad.x1 - gateRoad.x0, gateRoad.y1 - gateRoad.y0)) continue;
            int ex = gate[0] + gate[2] * stub, ey = gate[1] + gate[3] * stub;
            int start = ey * city.w + ex;
            int end = gen.roadSearch(start, x - 1, y - 1, bw + 2, bh + 2, prev);
            if (end < 0) continue;
            List<Integer> path = new ArrayList<Integer>();
            for (int c = end; c != -1; c = prev[c]) path.add(0, c);
            if (path.size() > 260) continue;
            if (city.desert) {
                boolean crosses = false;
                for (int c : path) if (c % city.w >= x - 1 && c % city.w <= x + bw && c / city.w >= y - rh - 1 && c / city.w < y) crosses = true;
                if (crosses) continue;
            }
            if (city.desert) forceBaseType = BT_TRAINING;
            Facility base = militaryBase(x, y, bw, bh, 29);
            forceBaseType = -1;
            base.satellite = true;
            if (city.desert) {
                // The town's own base, out here: the test range fenced off behind it, through its top gate.
                base.satellite = false;
                base.name = base.name.replace("(" + BASE_TYPES[BT_TRAINING] + ")", "(Test Range)");
                desertMap.testRange(x, y - rh, bw, rh, x + bw / 2 - 1);
            }
            gen.carve(gateRoad);
            {
                float ang = (float) Math.atan2(gate[3], gate[2]);
                float px = (gate[0] + gate[2] * 4 + 0.5f) * T, py = (gate[1] + gate[3] * 4 + 0.5f) * T;
                city.checkpoints.add(new float[]{px, py, ang});
                for (int sd = -1; sd <= 1; sd += 2) {
                    float[] q = city.findWalkable(px - (float) Math.sin(ang) * sd * 2.4f * T, py + (float) Math.cos(ang) * sd * 2.4f * T);
                    if (q != null) city.checkpointPosts.add(q);
                }
            }
            // The road on from there: smoothed out from the line it takes.
            List<Float> pts = new ArrayList<Float>();
            pts.add(gate[0] + gate[2] * (stub - 1) + 0.5f);
            pts.add(gate[1] + gate[3] * (stub - 1) + 0.5f);
            for (int k = 3; k < path.size() - 1; k += 4) {
                pts.add(path.get(k) % city.w + 0.5f);
                pts.add(path.get(k) / city.w + 0.5f);
            }
            pts.add(end % city.w + 0.5f);
            pts.add(end / city.w + 0.5f);
            float[] line = new float[pts.size()];
            for (int k = 0; k < line.length; k++) line[k] = pts.get(k);
            for (int it = 0; it < 2; it++) line = CityGenerator.chaikin(line);
            gen.curveOnGrass = true;
            landGen.curveRoad(line, false);
            gen.curveOnGrass = false;
            // The second checkpoint: before the road joins.
            int n = path.size();
            int[] at = n >= 16 ? new int[]{n - 8} : new int[0];
            for (int a : at) {
                int c = path.get(a), c0 = path.get(Math.max(0, a - 3)), c1 = path.get(Math.min(n - 1, a + 3));
                float ang = (float) Math.atan2(c1 / city.w - c0 / city.w, c1 % city.w - c0 % city.w);
                float px = (c % city.w + 0.5f) * T, py = (c / city.w + 0.5f) * T;
                city.checkpoints.add(new float[]{px, py, ang});
                for (int sd = -1; sd <= 1; sd += 2) {
                    float ox = (float) -Math.sin(ang) * sd * 2.4f * T, oy = (float) Math.cos(ang) * sd * 2.4f * T;
                    float[] q = city.findWalkable(px + ox, py + oy);
                    if (q != null) city.checkpointPosts.add(q);
                }
            }
            return;
        }
    }

    /** Lays out the open part of a base for its kind: what stands there, and its helipads and tents. */
    void baseLayout(int type, int ax, int ay, int aw, int ah, Random r) {
        float x0 = (ax + 2) * T, x1 = (ax + aw - 1) * T, y0 = (ay + ah / 2 + 1) * T, y1 = (ay + ah - 2) * T;
        if (y1 - y0 < 3 * T) y0 = y1 - 3 * T;
        float bw = x1 - x0, bh = y1 - y0;
        switch (type) {
            case BT_AIR: {
                // A runway across the bottom, hangars and parked jets above it, helipads at the end.
                float rh = Math.min(46, bh * 0.3f);
                city.baseDecor.add(new float[]{BD_RUNWAY, x0, y1 - rh, x1, y1});
                int hangars = Math.max(1, Math.min(3, (int) (bw / 70)));
                float hw = Math.min(56, bw * 0.6f / hangars);
                for (int k = 0; k < hangars; k++) {
                    float hx = x0 + 4 + k * (hw + 6);
                    city.baseDecor.add(new float[]{BD_HANGAR, hx, y0 + 2, hx + hw, Math.max(y0 + 14, y1 - rh - 18)});
                }
                for (int k = 0; k < 3; k++) {
                    float jx = x0 + bw * 0.62f + k * 18, jy = y1 - rh - 10;
                    if (jx + 12 < x1) city.baseDecor.add(new float[]{BD_JET, jx, jy - 7, jx + 12, jy + 7});
                }
                city.helipads.add(new float[]{x1 - 26, y0 + 22});
                break;
            }
            case BT_ARMOUR: {
                // A concrete tank park with rows of tanks, a vehicle shed, and the helipad.
                float px0 = x0 + bw * 0.08f, px1 = x0 + bw * 0.7f;
                city.baseDecor.add(new float[]{BD_HARDSTAND, px0, y0 + 2, px1, y1 - 2});
                int cols = Math.max(2, (int) ((px1 - px0 - 8) / 24)), rows = Math.max(1, Math.min(4, (int) ((bh - 10) / 34)));
                for (int q = 0; q < rows; q++)
                    for (int k = 0; k < cols; k++) {
                        float tx = px0 + 6 + k * 24, ty = y0 + 14 + q * 34;
                        city.baseDecor.add(new float[]{BD_TANK, tx, ty, tx + 14, ty + 20});
                    }
                city.baseDecor.add(new float[]{BD_SHED, px1 + 6, y0 + 2, x1 - 4, y0 + Math.min(70, bh * 0.5f)});
                city.helipads.add(new float[]{x1 - 26, y1 - 24});
                break;
            }
            case BT_TRAINING: {
                // A firing range with targets at the far end, an assault course, and rows of tents.
                float rh = Math.min(60, bh * 0.32f);
                city.baseDecor.add(new float[]{BD_RANGE, x0 + 2, y1 - rh, x0 + bw * 0.75f, y1 - 2});
                city.baseDecor.add(new float[]{BD_OBSTACLE, x0 + 4, y0 + 4, x0 + bw * 0.45f, y0 + Math.min(70, bh * 0.4f)});
                for (int i = 0; i < 8; i++)
                    city.tents.add(new float[]{x0 + bw * 0.52f + (i % 4) * 24, y0 + 14 + (i / 4) * 20});
                city.helipads.add(new float[]{x1 - 24, y1 - 24});
                break;
            }
            case BT_SPECIAL: {
                // A kill house for close-quarters training, two helipads, a mast and sandbagged positions.
                city.baseDecor.add(new float[]{BD_KILLHOUSE, x0 + 4, y0 + 4, x0 + Math.min(120, bw * 0.4f), y0 + Math.min(90, bh - 8)});
                city.helipads.add(new float[]{x0 + bw * 0.55f, y0 + bh * 0.5f});
                city.helipads.add(new float[]{x0 + bw * 0.82f, y0 + bh * 0.5f});
                city.baseDecor.add(new float[]{BD_MAST, x1 - 10, y0 + 4, x1 - 4, y0 + 10});
                for (int k = 0; k < 3; k++) {
                    float sx = x0 + bw * (0.48f + k * 0.18f);
                    city.baseDecor.add(new float[]{BD_SANDBAGS, sx - 7, y1 - 14, sx + 7, y1 - 2});
                }
                break;
            }
            case BT_SUPPLY: {
                // A fuel farm, stacks of containers and a long store shed.
                float fs = Math.min(34, bh * 0.3f);
                int fuel = Math.max(2, Math.min(4, (int) (bw * 0.4f / (fs + 6))));
                for (int k = 0; k < fuel; k++) {
                    float fx = x0 + 6 + k * (fs + 6);
                    city.baseDecor.add(new float[]{BD_FUEL, fx, y0 + 4, fx + fs, y0 + 4 + fs});
                }
                for (int q = 0; q < 3; q++)
                    for (int k = 0; k < 5; k++) {
                        float cx = x0 + bw * 0.45f + k * 13, cy = y0 + 6 + q * 10;
                        if (cx + 11 < x1) city.baseDecor.add(new float[]{BD_CONTAINER, cx, cy, cx + 11, cy + 7, r.nextInt(5)});
                    }
                city.baseDecor.add(new float[]{BD_SHED, x0 + 4, y1 - Math.min(50, bh * 0.35f), x0 + bw * 0.7f, y1 - 2});
                city.helipads.add(new float[]{x1 - 24, y1 - 24});
                break;
            }
            case BT_RADAR: {
                // Radar domes and a dish on its tower, aerial masts, and a helipad.
                float ds = Math.min(44, bh * 0.45f);
                int domes = Math.max(1, Math.min(3, (int) (bw * 0.55f / (ds + 10))));
                for (int k = 0; k < domes; k++) {
                    float dx = x0 + 10 + k * (ds + 10);
                    city.baseDecor.add(new float[]{BD_RADAR, dx, y0 + 6, dx + ds, y0 + 6 + ds});
                }
                city.baseDecor.add(new float[]{BD_DISH, x0 + bw * 0.64f, y0 + 6, x0 + bw * 0.64f + ds * 1.1f, y0 + 6 + ds * 1.1f});
                for (int k = 0; k < 3; k++) {
                    float mx = x0 + 10 + k * 26;
                    city.baseDecor.add(new float[]{BD_MAST, mx, y1 - 12, mx + 6, y1 - 6});
                }
                city.helipads.add(new float[]{x1 - 24, y1 - 24});
                break;
            }
            case BT_GUARD: {
                // A parade ground with its flagpole, the vehicle shed, and a few tents.
                city.baseDecor.add(new float[]{BD_HARDSTAND, x0 + 4, y0 + 2, x0 + bw * 0.55f, y1 - 2});
                city.baseDecor.add(new float[]{BD_FLAG, x0 + bw * 0.3f - 3, y0 + bh * 0.5f - 3, x0 + bw * 0.3f + 3, y0 + bh * 0.5f + 3});
                city.baseDecor.add(new float[]{BD_SHED, x0 + bw * 0.6f, y0 + 2, x1 - 4, y0 + Math.min(34, bh * 0.5f)});
                for (int i = 0; i < 2; i++) city.tents.add(new float[]{x0 + bw * 0.66f + i * 22, y1 - 12});
                city.helipads.add(new float[]{x1 - 24, y1 - 24});
                break;
            }
            default:
                // A garrison: the helipad and a block of tents.
                city.helipads.add(new float[]{(ax + aw * 0.3f) * T, (ay + ah * 0.72f) * T});
                for (int i = 0; i < 4; i++)
                    city.tents.add(new float[]{(ax + aw * 0.55f + (i % 2) * 2.2f) * T, (ay + ah * 0.55f + (i / 2) * 2f) * T});
        }
    }

    /**
     * (15.5) The Military Town: a great base in the middle (see militaryBase), a ring of main roads round it, and the
     * town's streets laid out in the eight pieces round that (as round the lake on Lakeside).
     */
    void militaryTown(int bx0, int by0, int bx1, int by1, int in) {
        int lw = Math.min(110, (int) ((bx1 - bx0) * 0.4f)), lh = Math.min(90, (int) ((by1 - by0) * 0.36f));
        int lx0 = (bx0 + bx1) / 2 - lw / 2, ly0 = (by0 + by1) / 2 - lh / 2;
        int lx1 = lx0 + lw, ly1 = ly0 + lh;
        gen.carve(new Street(bx0, ly0 - 3, bx1, ly0, false, true));
        gen.carve(new Street(bx0, ly1, bx1, ly1 + 3, false, true));
        gen.carve(new Street(lx0 - 3, by0, lx0, by1, true, true));
        gen.carve(new Street(lx1, by0, lx1 + 3, by1, true, true));
        int[] xs = {bx0 + in, lx0 - 3, lx0, lx1, lx1 + 3, bx1 - in}, ys = {by0 + in, ly0 - 3, ly0, ly1, ly1 + 3, by1 - in};
        for (int j = 0; j < 3; j++)
            for (int i = 0; i < 3; i++) {
                if (i == 1 && j == 1) continue;
                if (xs[i * 2 + 1] - xs[i * 2] < 6 || ys[j * 2 + 1] - ys[j * 2] < 6) continue;
                gen.split(xs[i * 2], ys[j * 2], xs[i * 2 + 1], ys[j * 2 + 1], 1, -1, -1);
            }
        city.fill(lx0, ly0, lw, lh, SIDEWALK);
        gen.bigBase = new int[]{lx0, ly0, lx1, ly1};
    }
}
