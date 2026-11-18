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
 * (27.3) The Islands and the Archipelago: where the islands lie, their outlines, the bridges between them and the
 * causeway to the mainland. Taken out of City.java as it stood; the city itself (its tiles, buildings and everything
 * the game asks of it) is `city`.
 */
final class IslandsMap extends CityPart {
    IslandsMap(City city) {
        super(city);
    }

    /**
     * ((26.1) What a district on island il is (map version 39). The Islands: the first the city (downtown and midtown),
     * the second the suburbs, the third the port and its works, the fourth the old town. The Archipelago: the big island
     * as it was, the islands round it villages of houses among the palms.
     */
    int islandDistrict(int il, int was) {
        if (il < 0) return was;
        float roll = new Random(city.cfg.seed * 7919L + city.districts.size() * 31L).nextFloat();
        if (city.cfg.archipelago()) return il == 0 ? was : roll < 0.7f ? DT_SUBURB : DT_PARKSIDE;
        switch (il) {
            case 0: return roll < 0.45f ? DT_DOWNTOWN : DT_MIDTOWN;
            case 1: return roll < 0.75f ? DT_SUBURB : DT_PARKSIDE;
            case 2: return roll < 0.7f ? DT_INDUSTRIAL : DT_MIDTOWN;
            default: return roll < 0.6f ? DT_OLDTOWN : roll < 0.8f ? DT_CAMPUS : DT_MIDTOWN;
        }
    }

    /** Where the three islands are: two across the top, one below between them, a little different each time. */
    void islandLayout() {
        Random r = new Random(city.cfg.seed * 41L + 3);
        int j = city.w / 64;
        if (city.cfg.archipelago()) {
            archipelagoLayout(r, j);
            return;
        }
        if (city.themePark) {
            parkMap.themeParkLayout(r, j);
            return;
        }
        // (Since 10.10 there are two, three or four islands, laid out a few different ways.)
        int count = city.cfg.nature() ? 2 + new Random(city.cfg.seed * 73L + 9).nextInt(3) : 3;
        float[][] f;
        if (count == 2) {
            boolean across = r.nextBoolean();
            f = across ? new float[][]{{0.03f, 0.08f, 0.47f, 0.8f}, {0.53f, 0.18f, 0.97f, 0.92f}}
                    : new float[][]{{0.08f, 0.03f, 0.88f, 0.46f}, {0.14f, 0.54f, 0.94f, 0.94f}};
            // (Side by side: joined across the middle; one above the other: joined down the middle.)
            gen.islandLinks = across ? new int[][]{{0, 1, 0}} : new int[][]{{0, 1, 1}};
            gen.causewayIsland = 1;
        } else if (count == 4) {
            f = new float[][]{{0.03f, 0.03f, 0.47f, 0.46f}, {0.53f, 0.04f, 0.97f, 0.47f}, {0.03f, 0.54f, 0.47f, 0.94f},
                    {0.53f, 0.53f, 0.97f, 0.95f}};
            gen.islandLinks = new int[][]{{0, 1, 0}, {0, 2, 1}, {1, 3, 1}, {2, 3, 0}};
            gen.causewayIsland = 2 + r.nextInt(2);
        } else {
            f = new float[][]{{0.03f, 0.03f, 0.475f, 0.465f}, {0.525f, 0.035f, 0.97f, 0.47f}, {0.19f, 0.535f, 0.81f, 0.935f}};
            gen.islandLinks = new int[][]{{0, 1, 0}, {0, 2, 1}, {1, 2, 1}};
            gen.causewayIsland = 2;
        }
        city.islandRects = new int[count][];
        for (int i = 0; i < count; i++)
            city.islandRects[i] = new int[]{(int) (city.w * f[i][0]) + r.nextInt(j), (int) (city.h * f[i][1]) + r.nextInt(j),
                    (int) (city.w * f[i][2]) - r.nextInt(j), (int) (city.h * f[i][3]) - r.nextInt(j)};
        city.islandNames = new String[count];
        city.islandNames[0] = city.name;
        for (int i = 1; i < count; i++) {
            String n = city.country.townName(new Random(city.cfg.seed * 131L + i * 17));
            for (int k = 0; k < i && n != null; k++) if (n.equals(city.islandNames[k])) n = null;
            city.islandNames[i] = n != null ? n : city.country.townName(new Random(city.cfg.seed * 977L + i));
        }
    }

    /**
     * (10.60) The Archipelago: one big island in the middle with the city on it, and two to five smaller island
     * towns round it ((21.4) three or four now) (south, west, east, north, then the north-east), each joined to the big one by a bridge (the
     * north-east one by way of the east), and the causeway from the southern one to the mainland.
     */
    void archipelagoLayout(Random r, int j) {
        int extra = city.cfg.moreIslands() ? 3 + new Random(city.cfg.seed * 73L + 9).nextInt(2) : 2 + new Random(city.cfg.seed * 73L + 9).nextInt(4);
        // ((26.1) A longer chain: all five round the big one.)
        if (city.cfg.distinctIslands()) extra = 5;
        float[][] all = {
                {0.27f, 0.26f, 0.73f, 0.72f},     // the big island
                {0.32f, 0.775f, 0.66f, 0.95f},    // south
                {0.03f, 0.34f, 0.215f, 0.64f},    // west
                {0.785f, 0.30f, 0.97f, 0.58f},    // east
                {0.36f, 0.03f, 0.64f, 0.205f},    // north
                {0.785f, 0.04f, 0.97f, 0.245f},   // north-east
        };
        int[][] links = {{0, 1, 1}, {2, 0, 0}, {0, 3, 0}, {4, 0, 1}, {5, 3, 1}};
        int count = 1 + extra;
        gen.islandLinks = new int[extra][];
        for (int k = 0; k < extra; k++) gen.islandLinks[k] = links[k];
        gen.causewayIsland = 1;
        city.islandRects = new int[count][];
        for (int i = 0; i < count; i++) {
            float[] f = all[i];
            city.islandRects[i] = new int[]{(int) (city.w * f[0]) + r.nextInt(j), (int) (city.h * f[1]) + r.nextInt(j),
                    (int) (city.w * f[2]) - r.nextInt(j), (int) (city.h * f[3]) - r.nextInt(j)};
        }
        city.islandNames = new String[count];
        city.islandNames[0] = city.name;
        for (int i = 1; i < count; i++) {
            String n = city.country.townName(new Random(city.cfg.seed * 131L + i * 17));
            for (int k = 0; k < i && n != null; k++) if (n.equals(city.islandNames[k])) n = null;
            city.islandNames[i] = n != null ? n : city.country.townName(new Random(city.cfg.seed * 977L + i));
        }
    }

    /**
     * Three island towns in the sea. Each town is laid out on its own street grid, then given a natural
     * outline (lobes, bays and headlands: the blocks outside it go back to the sea, with the roads that only
     * served them); round what's left grows a coast of grass and woods with sandy beaches here and rocky,
     * wooded shore there. Bridges then join the islands street to street, a causeway runs from the southern
     * island to the mainland, and a few islets lie offshore.
     */
    void islands() {
        city.fill(0, 0, city.w, city.h, WATER);
        city.bridge = new boolean[city.w * city.h];
        Random sr = new Random(city.cfg.seed * 59L + 11);
        for (int[] q : city.islandRects) {
            int x0 = q[0], y0 = q[1], x1 = q[2], y1 = q[3];
            city.fill(x0 - 1, y0 - 1, x1 - x0 + 2, y1 - y0 + 2, SIDEWALK);
            gen.carve(new Street(x0, y0, x1, y0 + 3, false, false));
            gen.carve(new Street(x0, y1 - 3, x1, y1, false, false));
            gen.carve(new Street(x0, y0, x0 + 3, y1, true, false));
            gen.carve(new Street(x1 - 3, y0, x1, y1, true, false));
            gen.split(x0 + 3, y0 + 3, x1 - 3, y1 - 3, 0, -1, -1);
        }
        // Each island's outline: a rounded shape with lobes, bays and headlands.
        int n = city.islandRects.length;
        float[][] amp = new float[n][4], ph = new float[n][4];
        for (int i = 0; i < n; i++)
            for (int k = 0; k < 4; k++) {
                amp[i][k] = 0.05f + sr.nextFloat() * (k == 0 ? 0.14f : 0.09f);
                ph[i][k] = sr.nextFloat() * (float) Math.PI * 2;
            }
        List<int[]> kept = new ArrayList<int[]>();
        for (int[] b : city.blocks) {
            float bx = (b[0] + b[2]) / 2f, by = (b[1] + b[3]) / 2f;
            int i = city.island((int) bx, (int) by);
            if (i < 0) continue;
            int[] q = city.islandRects[i];
            float hx = (q[2] - q[0]) / 2f, hy = (q[3] - q[1]) / 2f;
            float nx = (bx - (q[0] + q[2]) / 2f) / hx, ny = (by - (q[1] + q[3]) / 2f) / hy;
            float a = (float) Math.atan2(ny, nx);
            float d = (float) Math.pow(Math.pow(Math.abs(nx), 2.6) + Math.pow(Math.abs(ny), 2.6), 1 / 2.6);
            float r = 0.95f;
            for (int k = 0; k < 4; k++) r += amp[i][k] * (float) Math.sin((k + 2) * a + ph[i][k]);
            if (d < r) kept.add(b);
        }
        // The town: the blocks kept and the roads beside them. Everything else of the grid goes back to the sea.
        boolean[] town = new boolean[city.w * city.h];
        for (int[] b : kept)
            for (int y = Math.max(0, b[1] - 3); y < Math.min(city.h, b[3] + 3); y++)
                for (int x = Math.max(0, b[0] - 3); x < Math.min(city.w, b[2] + 3); x++) {
                    int t = y * city.w + x;
                    boolean inside = x >= b[0] && x < b[2] && y >= b[1] && y < b[3];
                    if (inside || city.tiles[t] == ROAD) town[t] = true;
                }
        for (int t = 0; t < city.w * city.h; t++)
            if (!town[t] && city.tiles[t] != WATER) {
                city.tiles[t] = WATER;
                city.roadDir[t] = 0;
                city.mainRoad[t] = false;
            }
        // Stray bits of road (and blocks only they reach) go too.
        int[] comp = new int[city.w * city.h];
        Arrays.fill(comp, -1);
        List<Integer> sizes = new ArrayList<Integer>(), compIsland = new ArrayList<Integer>();
        int[] qu = new int[city.w * city.h];
        for (int s0 = 0; s0 < city.w * city.h; s0++) {
            if (city.tiles[s0] != ROAD || comp[s0] >= 0) continue;
            int id = sizes.size(), head = 0, tail = 0;
            qu[tail++] = s0;
            comp[s0] = id;
            while (head < tail) {
                int c = qu[head++], x = c % city.w, y = c / city.w;
                int[] nb = {x > 0 ? c - 1 : -1, x < city.w - 1 ? c + 1 : -1, y > 0 ? c - city.w : -1, y < city.h - 1 ? c + city.w : -1};
                for (int jn : nb)
                    if (jn >= 0 && comp[jn] < 0 && city.tiles[jn] == ROAD) {
                        comp[jn] = id;
                        qu[tail++] = jn;
                    }
            }
            sizes.add(tail);
            compIsland.add(city.island(s0 % city.w, s0 / city.w));
        }
        // ((21.4) Measured island by island: a small island town's streets were thrown away for being small next to
        // the big island's, and left its blocks standing in the sea.)
        int[] biggest = new int[n + 1];
        for (int k = 0; k < sizes.size(); k++) {
            int il = compIsland.get(k) + 1;
            biggest[il] = Math.max(biggest[il], sizes.get(k));
        }
        for (int t = 0; t < city.w * city.h; t++)
            if (comp[t] >= 0 && sizes.get(comp[t]) < biggest[compIsland.get(comp[t]) + 1] / 6) {
                city.tiles[t] = WATER;
                city.roadDir[t] = 0;
                city.mainRoad[t] = false;
            }
        city.blocks.clear();
        for (int[] b : kept) {
            boolean touches = false;
            for (int y = Math.max(0, b[1] - 1); y <= Math.min(city.h - 1, b[3]) && !touches; y++)
                for (int x = Math.max(0, b[0] - 1); x <= Math.min(city.w - 1, b[2]) && !touches; x++)
                    if (city.tiles[y * city.w + x] == ROAD) touches = true;
            if (touches) city.blocks.add(b);
            else city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], WATER);
        }
        // The coast: grassland and woods round the town, then the shore - sand where there's a beach,
        // grass and trees right down to the water where it's rocky.
        // (Distance from each island's town, in tenths of a tile, measured straight rather than in steps: a smooth
        // shore. Each island's land keeps well clear of the others', so there's always a channel between them.)
        int[][] dists = new int[n][];
        for (int il = 0; il < n; il++) dists[il] = gen.chamfer(il);
        int[] dist = new int[city.w * city.h];
        for (int t = 0; t < city.w * city.h; t++) {
            int own = Integer.MAX_VALUE, other = Integer.MAX_VALUE;
            int mine = -1;
            for (int il = 0; il < n; il++)
                if (dists[il][t] < own) {
                    other = own;
                    own = dists[il][t];
                    mine = il;
                } else other = Math.min(other, dists[il][t]);
            dist[t] = own + 100 > other ? (own == 0 ? 0 : 1 << 20) : own;
        }
        double p0 = sr.nextDouble() * 10;
        for (int t = 0; t < city.w * city.h; t++) {
            if (dist[t] <= 0 || dist[t] > 230) continue;
            int x = t % city.w, y = t / city.w;
            // How far the land runs out from the town: slowly wandering (headlands and bays), with a little
            // ruffle along the shore.
            double coast = 7.5 + 6 * Math.sin(x * 0.043 + y * 0.021 + p0) + 4 * Math.sin(x * 0.019 - y * 0.051 + p0 * 2)
                    + 2.6 * Math.sin(x * 0.17 + y * 0.09 + p0 * 4) + 1.8 * Math.sin((x - y) * 0.11 + p0 * 3)
                    + 0.7 * Math.sin((x + y) * 0.37 + p0 * 5);
            coast = Math.max(2.2, Math.min(22, coast));
            double beach = Math.max(0, 1.6 + 2.0 * Math.sin(x * 0.04 + y * 0.065 + p0 * 1.7));
            double d = dist[t] / 10.0;
            if (d > coast) continue;
            city.tiles[t] = d > coast - beach ? SAND : sr.nextFloat() < (d > 3 ? 0.22f : 0.08f) && d > 1.2 ? TREE : GRASS;
        }
        // Streets that ran on into the water stop at the last corner instead.
        boolean[] dug = new boolean[city.w * city.h];
        for (int[] e : roadGen.deadEnds()) gen.trimStub(e, dug);
        roadGen.fixJunctions(dug);
        for (int t = 0; t < city.w * city.h; t++) if (dug[t]) city.tiles[t] = GRASS;
        for (int si = city.streets.size() - 1; si >= 0; si--) {
            Street st = city.streets.get(si);
            boolean any = false;
            for (int y = st.y0; y < st.y1 && !any; y++)
                for (int x = st.x0; x < st.x1 && !any; x++)
                    if (x >= 0 && y >= 0 && x < city.w && y < city.h && city.tiles[y * city.w + x] == ROAD) any = true;
            if (!any) city.streets.remove(si);
        }
        // Bridges, street to street between neighbouring islands, and a causeway out to the mainland.
        for (int[] l : gen.islandLinks) {
            int[] a = city.islandRects[l[0]], b = city.islandRects[l[1]];
            if (l[2] == 0) {
                int lo = Math.max(a[1], b[1]), hi = Math.min(a[3], b[3]);
                gen.linkRoad(false, gen.mainLine(false, lo, hi, (lo + hi) / 2 - 1, a, b), (a[2] + b[0]) / 2, true);
            } else {
                int lo = Math.max(a[0], b[0]), hi = Math.min(a[2], b[2]);
                gen.linkRoad(true, gen.mainLine(true, lo, hi, (lo + hi) / 2 - 1, a, b), (a[3] + b[1]) / 2, true);
            }
        }
        int[] c = city.islandRects[gen.causewayIsland];
        int cx = gen.mainLine(true, c[0], c[2], (c[0] + c[2]) / 2 - 1, c, null);
        gen.linkRoad(true, cx, city.h - 1, false);
        city.causewayEnd = new float[]{(cx + 1.5f) * T, (city.h - 1.5f) * T};
        // Islets offshore: rocks with a tree or two, clear of the bridges. (Not since 10.10: people ended up
        // stranded on them.)
        for (int k = 0, made = 0; k < 200 && made < 7 && !city.cfg.nature(); k++) {
            int ix = 8 + sr.nextInt(city.w - 16), iy = 8 + sr.nextInt(city.h - 16), ir = 2 + sr.nextInt(4);
            boolean clear = true;
            for (int y = iy - ir - 6; y <= iy + ir + 6 && clear; y++)
                for (int x = ix - ir - 6; x <= ix + ir + 6 && clear; x++)
                    if (x < 0 || y < 0 || x >= city.w || y >= city.h || city.tiles[y * city.w + x] != WATER) clear = false;
            if (!clear) continue;
            made++;
            for (int y = iy - ir - 1; y <= iy + ir + 1; y++)
                for (int x = ix - ir - 1; x <= ix + ir + 1; x++) {
                    double dd = World.hypot(x - ix, y - iy) + Math.sin(x * 1.3 + y * 0.7) * 0.6;
                    if (dd <= ir - 0.5) city.tiles[y * city.w + x] = sr.nextFloat() < 0.35f ? TREE : GRASS;
                    else if (dd <= ir + 0.6) city.tiles[y * city.w + x] = SAND;
                }
        }
        // Any land the bridges don't reach (a scrap of coast cut off by a bay) goes back to the sea: nobody
        // should be stranded where they can't walk out.
        if (city.cfg.nature()) dropStrandedLand();
        // People go down to the beaches.
        for (int i = 0; i < n; i++) {
            int[] q = city.islandRects[i];
            int mx = (q[0] + q[2]) / 2, my = (q[1] + q[3]) / 2;
            for (int[] d : new int[][]{{0, 1}, {-1, 0}, {1, 0}, {0, -1}}) {
                for (int s1 = 0; s1 < city.w; s1++) {
                    int x = mx + d[0] * s1, y = my + d[1] * s1;
                    if (x < 0 || y < 0 || x >= city.w || y >= city.h || city.tiles[y * city.w + x] == WATER) break;
                    if (city.tiles[y * city.w + x] == SAND) {
                        city.openAreas.add(new float[]{(x + 0.5f) * T, (y + 0.5f) * T, 0});
                        break;
                    }
                }
            }
        }
        // Boats out at sea.
        for (int k = 0; k < city.w / 20; k++) {
            int bx = 4 + city.rnd.nextInt(city.w - 40), by = 4 + city.rnd.nextInt(city.h - 8);
            if (city.tiles[by * city.w + bx] != WATER || city.tiles[by * city.w + bx + 2] != WATER) continue;
            city.addDecor(D_BOAT, bx * T, by * T, bx * T + 30, by * T + 10, city.rnd.nextInt(3));
        }
    }

    void dropStrandedLand() {
        int[] comp = new int[city.w * city.h];
        Arrays.fill(comp, -1);
        int[] q = new int[city.w * city.h];
        int start = (int) (city.causewayEnd[1] / T) * city.w + (int) (city.causewayEnd[0] / T);
        if (city.tiles[start] == WATER) return;
        int head = 0, tail = 0;
        q[tail++] = start;
        comp[start] = 0;
        while (head < tail) {
            int c = q[head++], x = c % city.w, y = c / city.w;
            int[] nb = {x > 0 ? c - 1 : -1, x < city.w - 1 ? c + 1 : -1, y > 0 ? c - city.w : -1, y < city.h - 1 ? c + city.w : -1};
            for (int j : nb)
                if (j >= 0 && comp[j] < 0 && city.tiles[j] != WATER) {
                    comp[j] = 0;
                    q[tail++] = j;
                }
        }
        boolean[] keep = new boolean[city.w * city.h];
        for (int[] b : city.blocks)
            for (int y = Math.max(0, b[1]); y < Math.min(city.h, b[3]); y++)
                for (int x = Math.max(0, b[0]); x < Math.min(city.w, b[2]); x++) keep[y * city.w + x] = true;
        for (int t = 0; t < city.w * city.h; t++)
            if (city.tiles[t] != WATER && comp[t] < 0 && !keep[t]) {
                city.tiles[t] = WATER;
                city.roadDir[t] = 0;
                city.mainRoad[t] = false;
            }
    }
}
