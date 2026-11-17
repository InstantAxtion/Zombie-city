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
 * (27.3) River City: the river through the middle of town, its banks and bridges, and the tidying of the roads it
 * cut. Taken out of City.java as it stood; the city itself (its tiles, buildings and everything the game asks of it)
 * is `city`.
 */
final class RiverMap extends CityPart {
    RiverMap(City city) {
        super(city);
    }

    /**
     * ((22.0) River City. ((22.1) The town is laid out first; then a river winds right across the map through it - bends
     * of its own on every map, narrowing and widening as it goes, running across or down the map. Blocks it cuts
     * through become riverside park. Only a few streets cross it, each where a straight street ran right across
     * its course, so a bridge carries the street on, in line, from bank to bank: the great avenues on wide bridges,
     * an ordinary street on a narrower one, and a footbridge between the parks.)
     */
    void riverTown(int bx0, int by0, int bx1, int by1, int in) {
        gen.splitTop(bx0 + in, by0 + in, bx1 - in, by1 - in);
        Random rr = new Random(city.cfg.seed * 613L + 29);
        boolean across = rr.nextBoolean();
        int L = across ? city.w : city.h;
        // Across the river: where the town is, that way.
        int lo = across ? by0 : bx0, hi = across ? by1 : bx1, span = hi - lo;
        float base = lo + span * (0.4f + rr.nextFloat() * 0.2f);
        float a1 = span * (0.08f + rr.nextFloat() * 0.1f), a2 = span * (0.02f + rr.nextFloat() * 0.05f);
        float f1 = (float) (Math.PI * 2 / (L * (0.55f + rr.nextFloat() * 0.6f))), f2 = f1 * (2.6f + rr.nextFloat() * 2.2f);
        float p1 = rr.nextFloat() * 6.28f, p2 = rr.nextFloat() * 6.28f, p3 = rr.nextFloat() * 6.28f;
        // ((22.3) Narrower on the smallest maps, now they are smaller: it was taking half the town.)
        float w0 = Math.max(city.cfg.civic >= 34 ? 5 : 8, Math.min(18, span / 18f)), wv = w0 * (0.25f + rr.nextFloat() * 0.2f), f3 = f1 * (1.3f + rr.nextFloat());
        boolean[] mask = new boolean[city.w * city.h];
        int[] edge0 = new int[L], edge1 = new int[L];
        city.riverC = new float[L];
        city.riverHalf = new float[L];
        for (int a = 0; a < L; a++) {
            float c = base + a1 * (float) Math.sin(a * f1 + p1) + a2 * (float) Math.sin(a * f2 + p2);
            float half = (w0 + wv * (float) Math.sin(a * f3 + p3)) / 2;
            c = Math.max(lo + in + half + 4, Math.min(hi - in - half - 4, c));
            city.riverC[a] = c;
            city.riverHalf[a] = half;
            edge0[a] = Math.round(c - half);
            edge1[a] = Math.round(c + half);
            for (int b = Math.max(0, edge0[a]); b <= Math.min((across ? city.h : city.w) - 1, edge1[a]); b++) mask[across ? b * city.w + a : a * city.w + b] = true;
        }
        city.river = mask;
        city.riverAcross = across;
        city.riverEdge0 = edge0;
        city.riverEdge1 = edge1;
        // Where a straight street runs right across the river's course: a run of crossing tiles all road, the same
        // way, from one bank to the other.
        java.util.List<int[]> runs = new ArrayList<int[]>();
        int dirAcross = across ? 1 : 2;
        int start = -1;
        for (int a = 0; a <= L; a++) {
            boolean ok = a < L && a > 2 && a < L - 3;
            for (int b = edge0[Math.min(a, L - 1)] - 1; ok && b <= edge1[Math.min(a, L - 1)] + 1; b++) {
                int t = across ? b * city.w + a : a * city.w + b;
                if (b < 0 || b >= (across ? city.h : city.w) || city.tiles[t] != ROAD || city.roadDir[t] != dirAcross) ok = false;
            }
            if (ok && start < 0) start = a;
            if (!ok && start >= 0) {
                if (a - start >= 2) runs.add(new int[]{start, a - start});
                start = -1;
            }
        }
        // The widest first (the avenues), then the rest at random; well spread out along the river.
        java.util.Collections.shuffle(runs, rr);
        java.util.Collections.sort(runs, new java.util.Comparator<int[]>() {
            @Override
            public int compare(int[] x, int[] y) {
                return (y[1] >= 5 ? 1 : 0) - (x[1] >= 5 ? 1 : 0);
            }
        });
        int want = Math.max(2, Math.min(4, L / 160 + 1)), apart = L / (want + 2);
        java.util.List<int[]> chosen = new ArrayList<int[]>();
        for (int[] r : runs) {
            if (chosen.size() >= want) break;
            boolean far = true;
            for (int[] o : chosen) if (Math.abs(o[0] - r[0]) < apart) far = false;
            if (far) chosen.add(r);
        }
        boolean[] keep = new boolean[city.w * city.h];
        if (city.bridge == null) city.bridge = new boolean[city.w * city.h];
        city.bridgeKind = new byte[city.w * city.h];
        // (No two alike: stone, steel and concrete in a different order on every map, the avenues on steel or stone.)
        java.util.List<Byte> kinds = new ArrayList<Byte>(java.util.Arrays.asList(BK_STONE, BK_STEEL, BK_CONCRETE));
        java.util.Collections.shuffle(kinds, rr);
        for (int ci = 0; ci < chosen.size(); ci++) {
            int[] r = chosen.get(ci);
            byte kind = kinds.get(ci % kinds.size());
            if (r[1] >= 5 && kind == BK_CONCRETE) kind = kinds.get((ci + 1) % kinds.size());
            for (int a = r[0]; a < r[0] + r[1]; a++)
                for (int b = edge0[a]; b <= edge1[a]; b++) {
                    int t = across ? b * city.w + a : a * city.w + b;
                    keep[t] = true;
                    city.bridge[t] = true;
                    city.bridgeKind[t] = kind;
                }
        }
        // The blocks it runs through: park down to the water, with trees.
        for (int k = city.blocks.size() - 1; k >= 0; k--) {
            int[] bk = city.blocks.get(k);
            boolean wet = false;
            for (int y = bk[1]; y < bk[3] && !wet; y++) for (int x = bk[0]; x < bk[2] && !wet; x++) if (mask[y * city.w + x]) wet = true;
            if (!wet) continue;
            city.blocks.remove(k);
            // ((22.3) On the new, smaller maps, only the part of the block by the water: the rest, on either side, is
            // still built on. The smallest River City was nearly all park.)
            int q0 = -1, q1 = -1;
            if (city.cfg.civic >= 34) {
                int lo0 = across ? bk[1] : bk[0], hi0 = across ? bk[3] : bk[2], wl = hi0, wh = lo0 - 1;
                for (int y = bk[1]; y < bk[3]; y++)
                    for (int x = bk[0]; x < bk[2]; x++)
                        if (mask[y * city.w + x]) {
                            int b = across ? y : x;
                            wl = Math.min(wl, b);
                            wh = Math.max(wh, b);
                        }
                q0 = wl - 3;
                q1 = wh + 3;
                if (q0 - lo0 >= 6) city.blocks.add(across ? new int[]{bk[0], bk[1], bk[2], q0} : new int[]{bk[0], bk[1], q0, bk[3]});
                else q0 = lo0;
                if (hi0 - q1 - 1 >= 6) city.blocks.add(across ? new int[]{bk[0], q1 + 1, bk[2], bk[3]} : new int[]{q1 + 1, bk[1], bk[2], bk[3]});
                else q1 = hi0 - 1;
            }
            for (int y = bk[1]; y < bk[3]; y++)
                for (int x = bk[0]; x < bk[2]; x++) {
                    int t = y * city.w + x;
                    if (mask[t]) continue;
                    int b = across ? y : x;
                    if (q0 >= 0 && (b < q0 || b > q1)) continue;
                    city.tiles[t] = rr.nextFloat() < 0.07f ? TREE : GRASS;
                }
        }
        // The water, but for the bridges.
        for (int t = 0; t < city.w * city.h; t++) {
            if (!mask[t] || keep[t]) continue;
            city.tiles[t] = WATER;
            city.roadDir[t] = 0;
            city.mainRoad[t] = false;
        }
        city.riverBridges = chosen.size();
        if (city.cfg.landmarks()) pruneRiverRoads();
        // A footbridge between the parks, where there's park on both banks and no bridge near.
        for (int a = L / 5; a < L * 4 / 5; a += 3) {
            boolean near = false;
            for (int[] o : chosen) if (Math.abs(o[0] - a) < apart / 2) near = true;
            if (near) continue;
            int b0 = edge0[a] - 1, b1 = edge1[a] + 1, n0 = across ? b0 * city.w + a : a * city.w + b0, n1 = across ? b1 * city.w + a : a * city.w + b1;
            int m0 = across ? b0 * city.w + a + 1 : (a + 1) * city.w + b0, m1 = across ? b1 * city.w + a + 1 : (a + 1) * city.w + b1;
            if (b0 < 0 || b1 >= (across ? city.h : city.w) || city.tiles[n0] != GRASS || city.tiles[n1] != GRASS || city.tiles[m0] != GRASS || city.tiles[m1] != GRASS) continue;
            for (int d = 0; d < 2; d++)
                for (int b = b0; b <= b1; b++) {
                    int t = across ? b * city.w + a + d : (a + d) * city.w + b;
                    if (city.tiles[t] == WATER) {
                        city.bridge[t] = true;
                        city.bridgeKind[t] = BK_WOOD;
                    }
                    city.tiles[t] = SIDEWALK;
                }
            city.riverBridges++;
            break;
        }
    }

    /**
     * ((22.4) Streets the river cut, with no bridge, stop at the last junction before it instead of running into
     * the water: each such stretch, junction to water, becomes grass, and the pavements left beside nothing with it.
     */
    void pruneRiverRoads() {
        boolean[] gone = new boolean[city.w * city.h];
        // ((22.8) Again until nothing more goes: a street cut off at a corner of the bank only shows as one once the
        // street along the bank beside it has gone (map version 38 on).)
        for (int pass = 0; pass < (city.cfg.neighbourhoods() ? 4 : 1); pass++)
            if (!pruneRiverPass(gone)) break;
        pruneOrphanPavements(gone);
    }

    boolean pruneRiverPass(boolean[] gone) {
        boolean any = false;
        boolean[] seen = new boolean[city.w * city.h];
        for (int t0 = 0; t0 < city.w * city.h; t0++) {
            if (city.tiles[t0] != ROAD || seen[t0] || (city.bridge != null && city.bridge[t0])) continue;
            int x0 = t0 % city.w, y0 = t0 / city.w;
            if (!city.wet(x0 - 1, y0) && !city.wet(x0 + 1, y0) && !city.wet(x0, y0 - 1) && !city.wet(x0, y0 + 1)) continue;
            // Which way the street runs, from its shape: further along than across.
            int vl = 0, hl = 0;
            for (int k = 1; k <= 12 && y0 - k >= 0 && city.tiles[t0 - k * city.w] == ROAD; k++) vl++;
            for (int k = 1; k <= 12 && y0 + k < city.h && city.tiles[t0 + k * city.w] == ROAD; k++) vl++;
            for (int k = 1; k <= 12 && x0 - k >= 0 && city.tiles[t0 - k] == ROAD; k++) hl++;
            for (int k = 1; k <= 12 && x0 + k < city.w && city.tiles[t0 + k] == ROAD; k++) hl++;
            int d = vl > hl ? 1 : vl < hl ? 2 : city.roadDir[t0];
            if (d != 1 && d != 2) continue;
            // Along the street (down it for a north-south one), a row of it at a time across its width, both ways
            // from here until a crossing street (the row much wider), its end, or the water.
            boolean ns = d == 1;
            int along0 = ns ? y0 : x0, lat0 = ns ? x0 : y0, AL = ns ? city.h : city.w, LA = ns ? city.w : city.h;
            int[] run0 = latRun(ns, along0, lat0, LA);
            // (Not from where a crossing street runs through: that row is a long way across.)
            if (run0[1] - run0[0] + 1 > 9) continue;
            java.util.List<int[]> rows = new ArrayList<int[]>();
            rows.add(new int[]{along0, run0[0], run0[1]});
            boolean intoWater = false, tooLong = false, toBridge = false;
            int maxW = run0[1] - run0[0] + 1;
            for (int dir = -1; dir <= 1; dir += 2) {
                int l0 = run0[0], l1 = run0[1], prevW = l1 - l0 + 1;
                for (int k = along0 + dir; ; k += dir) {
                    if (k < 0 || k >= AL) break;
                    if (Math.abs(k - along0) > 90) {
                        tooLong = true;
                        break;
                    }
                    // The street in this row: the run of road over where it was in the last.
                    int st = -1;
                    for (int q = l0; q <= l1 && st < 0; q++) if (roadAt(ns, k, q)) st = q;
                    int wetN = 0;
                    for (int q = l0; q <= l1; q++) if (wetAt(ns, k, q)) wetN++;
                    if (st < 0) {
                        if (wetN * 2 >= l1 - l0 + 1) intoWater = true;
                        break;
                    }
                    int[] run = latRun(ns, k, st, LA);
                    int rw = run[1] - run[0] + 1;
                    if (rw > prevW + 2) break;
                    for (int q = run[0]; q <= run[1]; q++) if (bridgeAt(ns, k, q)) toBridge = true;
                    rows.add(new int[]{k, run[0], run[1]});
                    maxW = Math.max(maxW, rw);
                    if (wetN > 0 && rw < prevW) intoWater = true;
                    prevW = Math.max(prevW, rw);
                    l0 = Math.min(l0, run[0]);
                    l1 = Math.max(l1, run[1]);
                }
            }
            boolean eaten = false;
            for (int[] r : rows) if ((r[2] - r[1] + 1) * 10 < maxW * 7) eaten = true;
            for (int[] r : rows)
                for (int q = r[1]; q <= r[2]; q++) seen[ns ? r[0] * city.w + q : q * city.w + r[0]] = true;
            // (A street onto a bridge stays, however the bank cuts across it.)
            if (tooLong || toBridge || !(intoWater || eaten)) continue;
            for (int[] r : rows)
                for (int q = r[1]; q <= r[2]; q++) {
                    int t = ns ? r[0] * city.w + q : q * city.w + r[0];
                    city.tiles[t] = city.rnd.nextFloat() < 0.06f ? TREE : GRASS;
                    city.roadDir[t] = 0;
                    city.mainRoad[t] = false;
                    gone[t] = true;
                    any = true;
                }
        }
        return any;
    }

    void pruneOrphanPavements(boolean[] gone) {
        // Pavements by a street that's gone and by no other: grass too.
        for (int t = 0; t < city.w * city.h; t++) {
            if (city.tiles[t] != SIDEWALK) continue;
            int x = t % city.w, y = t / city.w;
            boolean byGone = false, byRoad = false;
            for (int dy = -1; dy <= 1; dy++)
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx, ny = y + dy;
                    if (nx < 0 || ny < 0 || nx >= city.w || ny >= city.h) continue;
                    int q = ny * city.w + nx;
                    if (gone[q]) byGone = true;
                    if (city.tiles[q] == ROAD) byRoad = true;
                }
            if (byGone && !byRoad) city.tiles[t] = GRASS;
        }
    }

    boolean roadAt(boolean ns, int along, int lat) {
        int x = ns ? lat : along, y = ns ? along : lat;
        // ((22.8) A car parked on the street is the street.)
        return x >= 0 && y >= 0 && x < city.w && y < city.h && (city.tiles[y * city.w + x] == ROAD || (city.tiles[y * city.w + x] == CAR && city.cfg.neighbourhoods()));
    }

    boolean bridgeAt(boolean ns, int along, int lat) {
        int x = ns ? lat : along, y = ns ? along : lat;
        return city.bridge != null && x >= 0 && y >= 0 && x < city.w && y < city.h && city.bridge[y * city.w + x];
    }

    boolean wetAt(boolean ns, int along, int lat) {
        return ns ? city.wet(lat, along) : city.wet(along, lat);
    }

    /** The run of road across the street through (along, lat): its first and last tile across. */
    int[] latRun(boolean ns, int along, int lat, int LA) {
        int l0 = lat, l1 = lat;
        while (l0 > 0 && roadAt(ns, along, l0 - 1)) l0--;
        while (l1 < LA - 1 && roadAt(ns, along, l1 + 1)) l1++;
        return new int[]{l0, l1};
    }
}
