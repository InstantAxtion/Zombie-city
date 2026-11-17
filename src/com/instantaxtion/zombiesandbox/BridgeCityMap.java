package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.City.*;

/**
 * ((28.0) Bridge City: a city spread over six to ten small islands in a bay, laid out on a rough grid of their own (a
 * different grid, and different islands missing from it, on every map), joined only by long bridges - the choke points
 * - with a causeway out to the mainland. The first island is downtown, one on the outer edge is the port (docks,
 * cranes, ships), the rest are homes. The islands themselves (their streets, shores and bridges) are built as the
 * Islands' are (IslandsMap.islands), from the layout here.
 */
final class BridgeCityMap extends WaterMap {
    BridgeCityMap(City city) {
        super(city);
    }

    /** Which island is the port (an index into city.islandRects), and its side facing open water (-1 up, 1 down). */
    int port = -1, portSide = -1;
    /** Each island's place on the grid {col, row}. */
    int[][] cellOf;

    @Override
    void layout() {
        int N = city.w;
        Random r = new Random(city.cfg.seed * 2803L + 25);
        int[][] grids = N < 160 ? new int[][]{{3, 2}, {2, 3}} : N < 300 ? new int[][]{{3, 2}, {2, 3}, {3, 3}}
                : new int[][]{{3, 3}, {4, 3}, {3, 4}, {4, 2}, {2, 4}};
        int[] g = grids[r.nextInt(grids.length)];
        int cols = g[0], rows = g[1];
        int cells = cols * rows;
        // How many go missing from the grid: none on the small maps, up to four on a big grid (six to ten islands).
        int keepMin = cells >= 9 ? Math.min(8, cells - 1) : 6, keepMax = Math.min(10, cells);
        int keep = keepMin + r.nextInt(keepMax - keepMin + 1);
        float M = N * 0.03f, cw = (N - 2 * M) / cols, ch = (N - 2 * M) / rows;
        float gap = Math.max(22, N * 0.06f);
        for (int attempt = 0; attempt < 60; attempt++) {
            Random a = new Random(city.cfg.seed * 7717L + attempt * 31L);
            boolean[] on = new boolean[cells];
            java.util.Arrays.fill(on, true);
            for (int drop = cells - keep, guard = 0; drop > 0 && guard < 200; guard++) {
                int k = a.nextInt(cells);
                if (!on[k] || k / cols == rows - 1 && countRow(on, cols, rows - 1) <= 1) continue;
                on[k] = false;
                drop--;
            }
            // Downtown: the kept cell nearest the middle.
            int dt = -1;
            float best = Float.MAX_VALUE;
            for (int k = 0; k < cells; k++) {
                if (!on[k]) continue;
                float dx = k % cols - (cols - 1) / 2f, dy = k / cols - (rows - 1) / 2f, d = dx * dx + dy * dy + a.nextFloat() * 0.3f;
                if (d < best) {
                    best = d;
                    dt = k;
                }
            }
            List<Integer> order = new ArrayList<Integer>();
            order.add(dt);
            for (int k = 0; k < cells; k++) if (on[k] && k != dt) order.add(k);
            int n = order.size();
            int[][] rects = new int[n][];
            cellOf = new int[n][];
            for (int i = 0; i < n; i++) {
                int k = order.get(i), cx = k % cols, cy = k / cols;
                float maxS = 1 - gap / cw, maxT = 1 - gap / ch;
                float s = Math.min(maxS, (i == 0 ? 0.8f : 0.6f) + a.nextFloat() * 0.2f), t = Math.min(maxT, (i == 0 ? 0.8f : 0.6f) + a.nextFloat() * 0.2f);
                float fx = M + cx * cw, fy = M + cy * ch;
                float ox = gap / 2 + a.nextFloat() * Math.max(0, (1 - s) * cw - gap), oy = gap / 2 + a.nextFloat() * Math.max(0, (1 - t) * ch - gap);
                rects[i] = new int[]{Math.round(fx + ox), Math.round(fy + oy), Math.round(fx + ox + s * cw), Math.round(fy + oy + t * ch)};
                cellOf[i] = new int[]{cx, cy};
            }
            // The bridges: between neighbours on the grid that face each other far enough; a tree of them, and a few more.
            List<int[]> cand = new ArrayList<int[]>();
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++) {
                    if (i == j) continue;
                    int[] ci = cellOf[i], cj = cellOf[j];
                    boolean across = ci[1] == cj[1] && cj[0] == ci[0] + 1, down = ci[0] == cj[0] && cj[1] == ci[1] + 1;
                    if (!across && !down) continue;
                    int lo = across ? Math.max(rects[i][1], rects[j][1]) : Math.max(rects[i][0], rects[j][0]);
                    int hi = across ? Math.min(rects[i][3], rects[j][3]) : Math.min(rects[i][2], rects[j][2]);
                    if (hi - lo < Math.max(14, N / 40)) continue;
                    cand.add(new int[]{i, j, down ? 1 : 0});
                }
            java.util.Collections.shuffle(cand, a);
            int[] root = new int[n];
            for (int i = 0; i < n; i++) root[i] = i;
            List<int[]> links = new ArrayList<int[]>();
            List<int[]> spare = new ArrayList<int[]>();
            for (int[] c : cand) {
                int x = find(root, c[0]), y = find(root, c[1]);
                if (x != y) {
                    root[x] = y;
                    links.add(c);
                } else spare.add(c);
            }
            int groups = 0;
            for (int i = 0; i < n; i++) if (find(root, i) == i) groups++;
            if (groups > 1) continue;
            // A loop or two, so not every island hangs by one bridge (but most do: the bridges are the way across).
            for (int[] c : spare) if (a.nextFloat() < 0.3f) links.add(c);
            // The causeway to the mainland, from an island on the bottom row (not downtown if there's another).
            int cw0 = -1;
            for (int i = 0; i < n; i++)
                if (cellOf[i][1] == rows - 1 && (cw0 < 0 || cw0 == 0 || a.nextBoolean())) cw0 = i;
            if (cw0 < 0) continue;
            // The port: on the top row if it can be, its docks reaching out to the open bay.
            for (int i = 1; i < n; i++)
                if (cellOf[i][1] == 0 && (port < 0 || a.nextBoolean())) port = i;
            portSide = -1;
            if (port < 0)
                for (int i = 1; i < n; i++)
                    if (cellOf[i][1] == rows - 1 && i != cw0) {
                        port = i;
                        portSide = 1;
                    }
            city.islandRects = rects;
            gen.islandLinks = links.toArray(new int[0][]);
            gen.causewayIsland = cw0;
            break;
        }
        if (city.islandRects == null) {
            // (Never seen: a plain row of islands.)
            int n = 6;
            city.islandRects = new int[n][];
            cellOf = new int[n][];
            gen.islandLinks = new int[n - 1][];
            float w = (N - 2 * M) / n;
            for (int i = 0; i < n; i++) {
                city.islandRects[i] = new int[]{Math.round(M + i * w + gap / 2), Math.round(N * 0.3f), Math.round(M + (i + 1) * w - gap / 2), Math.round(N * 0.7f)};
                cellOf[i] = new int[]{i, 0};
                if (i > 0) gen.islandLinks[i - 1] = new int[]{i - 1, i, 0};
            }
            gen.causewayIsland = 0;
        }
        int n = city.islandRects.length;
        city.islandNames = new String[n];
        city.islandNames[0] = city.name;
        for (int i = 1; i < n; i++) {
            String nm = city.country.townName(new Random(city.cfg.seed * 131L + i * 17));
            for (int k = 0; k < i && nm != null; k++) if (nm.equals(city.islandNames[k])) nm = null;
            city.islandNames[i] = nm != null ? nm : city.country.townName(new Random(city.cfg.seed * 977L + i));
        }
    }

    static int countRow(boolean[] on, int cols, int row) {
        int c = 0;
        for (int x = 0; x < cols; x++) if (on[row * cols + x]) c++;
        return c;
    }

    static int find(int[] root, int i) {
        while (root[i] != i) i = root[i] = root[root[i]];
        return i;
    }

    @Override
    int districtType(int k, float x, float y, int was) {
        int il = city.island((int) x, (int) y);
        if (il < 0) return was;
        float roll = new Random(city.cfg.seed * 7919L + k * 31L).nextFloat();
        if (il == 0) return roll < 0.7f ? DT_DOWNTOWN : DT_MIDTOWN;
        if (il == port) return roll < 0.8f ? DT_INDUSTRIAL : DT_MIDTOWN;
        // The rest: homes, an old quarter here and there, a college on one.
        return roll < 0.55f ? DT_SUBURB : roll < 0.7f ? DT_PARKSIDE : roll < 0.82f ? DT_OLDTOWN : roll < 0.9f ? DT_CAMPUS : DT_MIDTOWN;
    }

    @Override
    void streets() {
        islandsMap.islands();
        // Each bridge its own: steel, stone or concrete.
        if (city.bridgeKind == null) city.bridgeKind = new byte[city.w * city.h];
        Random r = new Random(city.cfg.seed * 557L + 25);
        int n = city.w * city.h;
        int[] q = new int[n];
        boolean[] seen = new boolean[n];
        byte[] kinds = {BK_STEEL, BK_STONE, BK_CONCRETE, BK_STEEL};
        for (int i = 0; i < n; i++) {
            if (seen[i] || !city.bridge[i]) continue;
            byte kind = kinds[r.nextInt(kinds.length)];
            int head = 0, tail = 0;
            q[tail++] = i;
            seen[i] = true;
            while (head < tail) {
                int c = q[head++], x = c % city.w, y = c / city.w;
                city.bridgeKind[c] = kind;
                for (int k = 0; k < 4; k++) {
                    int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = y + (k == 2 ? 1 : k == 3 ? -1 : 0);
                    if (!in(nx, ny)) continue;
                    int j = ny * city.w + nx;
                    if (seen[j] || !city.bridge[j]) continue;
                    seen[j] = true;
                    q[tail++] = j;
                }
            }
        }
        docks();
    }

    /** The port island's docks: concrete piers out into the bay, cranes and containers on them, a ship alongside. */
    void docks() {
        if (port < 0) return;
        int[] rc = city.islandRects[port];
        Random r = new Random(city.cfg.seed * 389L + 7);
        int dir = portSide;
        int made = 0;
        for (int x = rc[0] + 6 + r.nextInt(8); x < rc[2] - 12; x += 22 + r.nextInt(10)) {
            // From the middle of the island out to the shore.
            int y = (rc[1] + rc[3]) / 2;
            while (in(x, y) && !(at(x, y) == WATER && at(x + 6, y) == WATER && at(x + 3, y) == WATER)) y += dir;
            if (!in(x, y)) continue;
            int len = Math.max(8, Math.min(18, city.w / 50));
            // Open water all the way out and round it: no bridge, no other island.
            boolean clear = true;
            for (int k = -1; k < len + 6 && clear; k++)
                for (int xx = x - 6; xx <= x + 12 && clear; xx++) {
                    int yy = y + dir * k;
                    if (!in(xx, yy) || (k >= 2 && at(xx, yy) != WATER) || city.bridge[yy * city.w + xx]) clear = false;
                }
            if (!clear) continue;
            int y0 = dir < 0 ? y - len + 1 : y - 1, y1 = dir < 0 ? y + 1 : y + len;
            // Joined to the land behind it (the shore's grass or sand).
            for (int yy = y0; yy <= y1; yy++)
                for (int xx = x; xx < x + 7; xx++) if (at(xx, yy) == WATER || at(xx, yy) == SAND || at(xx, yy) == TREE) set(xx, yy, LOT);
            int far = dir < 0 ? y0 : y1;
            city.addDecor(D_CRANE, (x + 1) * T, (far + (dir < 0 ? 1 : -4)) * T, (x + 6) * T, (far + (dir < 0 ? 4 : -1)) * T, 0);
            int c0 = Math.min(y0, y1) + (dir < 0 ? 5 : 1), c1 = Math.max(y0, y1) - (dir < 0 ? 1 : 5);
            if (c1 - c0 >= 3) city.addDecor(D_CONTAINERS, (x + 1) * T, c0 * T, (x + 6) * T, c1 * T, r.nextInt(100));
            boolean right = r.nextBoolean();
            float sx0 = right ? (x + 7) * T + 3 : (x - 5) * T - 3;
            city.addDecor(D_SHIP, sx0, (Math.min(y0, y1) + 1) * T, sx0 + 5 * T, (Math.max(y0, y1)) * T, r.nextInt(4));
            if (++made >= 5) break;
        }
        // Boats about the bay.
        for (int k = 0; k < city.w / 30; k++) {
            int bx = 4 + r.nextInt(city.w - 40), by = 4 + r.nextInt(city.h - 8);
            if (at(bx, by) != WATER || at(bx + 2, by) != WATER || city.bridge[by * city.w + bx]) continue;
            city.addDecor(D_BOAT, bx * T, by * T, bx * T + 30, by * T + 10, r.nextInt(3));
        }
    }
}
