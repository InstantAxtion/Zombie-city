package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.City.*;

/**
 * ((28.0) Swamp Bayou: a town on the bank of a wide bayou that winds right across a vast swamp - pools and channels,
 * marsh and cypress, the water murky and the ground mossy. The town's riverfront has its quay, piers, bait-and-tackle
 * shops and the airboat landing; stilt houses stand out over the water on boardwalks round the town's edge; fishing
 * camps lie far out in the swamp, a long boardwalk back to the road. Few roads: the town's streets, a couple of
 * hamlets, and the raised causeways joining them, one over the bayou on a bridge and out to the far side, one out to
 * the edge of the map. The police's and the army's boats have the bayou.
 */
final class SwampMap extends WaterMap {
    SwampMap(City city) {
        super(city);
    }

    /** The bayou runs along x (across) or along y. */
    boolean across;
    /** The town is on the near (low) side of the bayou. */
    boolean townLow;
    int N, wB;
    float base;
    /** The bayou's middle at each place along it. */
    float[] cb;
    /** The town and the hamlets ({x0, y0, x1, y1} tiles, ring road included). */
    int[] town;
    final List<int[]> hamlets = new ArrayList<int[]>();
    /** The bayou's open water (bridges over it, and nothing built on it). */
    boolean[] deep;
    /** Ground the town and the hamlets take (for naming the districts out in the swamp). */
    boolean[] built;
    /** The causeways' lines (tiles). */
    final List<float[]> roads = new ArrayList<float[]>();
    /** The front street: the stretch of the town's ring road along the bayou {a0, a1, c} (c the street's inner row). */
    int frontC;
    Random r;

    int X(int a, int c) {
        return across ? a : c;
    }

    int Y(int a, int c) {
        return across ? c : a;
    }

    /** A rectangle in the bayou's frame (a along it, c across it) as tiles {x0, y0, x1, y1}. */
    int[] rect(int a0, int c0, int a1, int c1) {
        return across ? new int[]{a0, c0, a1, c1} : new int[]{c0, a0, c1, a1};
    }

    @Override
    void layout() {
        N = city.w;
        r = new Random(city.cfg.seed * 6151L + 26);
        across = r.nextBoolean();
        townLow = r.nextBoolean();
        wB = Math.max(6, Math.round(N / 26f));
        base = N * (0.5f + (r.nextFloat() - 0.5f) * 0.12f);
        int TA = Math.max(52, Math.round(N * 0.4f)), TC = Math.max(36, Math.round(N * 0.24f));
        int quay = 4;
        int a0 = Math.round(N * (0.06f + r.nextFloat() * 0.48f));
        a0 = Math.min(a0, N - TA - Math.max(8, N / 16));
        int a1 = a0 + TA;
        int c0, c1;
        if (townLow) {
            c1 = Math.round(base - wB / 2f) - quay;
            c0 = c1 - TC;
            frontC = c1 - 3;
        } else {
            c0 = Math.round(base + wB / 2f) + quay;
            c1 = c0 + TC;
            frontC = c0;
        }
        int[] t = rect(a0, c0, a1, c1);
        town = t;
        // The bayou: bends of its own, held straight along the town's quay.
        cb = new float[N];
        float A1 = N * (0.06f + r.nextFloat() * 0.05f), A2 = N * 0.02f;
        float f1 = (float) (Math.PI * 2 / (N * (0.7f + r.nextFloat() * 0.6f))), f2 = f1 * 2.7f;
        float p1 = r.nextFloat() * 6.28f, p2 = r.nextFloat() * 6.28f;
        int ramp = Math.max(20, N / 10);
        for (int a = 0; a < N; a++) {
            float m = A1 * (float) Math.sin(a * f1 + p1) + A2 * (float) Math.sin(a * f2 + p2);
            float out = a < a0 ? (a0 - a) / (float) ramp : a >= a1 ? (a - a1 + 1) / (float) ramp : 0;
            float s = Math.min(1, out);
            s = s * s * (3 - 2 * s);
            cb[a] = base + m * s;
        }
        // The hamlets: on solid ground out in the swamp, clear of the town and the bayou.
        int want = N < 200 ? 1 : N < 500 ? 2 : 3;
        int HS = Math.max(28, Math.round(N * 0.12f));
        for (int tries = 0; tries < 400 && hamlets.size() < want; tries++) {
            int hs = HS + r.nextInt(Math.max(1, HS / 3));
            int ha = 6 + r.nextInt(Math.max(1, N - hs - 12)), hc = 6 + r.nextInt(Math.max(1, N - hs - 12));
            int[] q = rect(ha, hc, ha + hs, hc + hs);
            boolean ok = !near(q, town, Math.max(30, N / 10));
            for (int[] o : hamlets) if (near(q, o, Math.max(30, N / 8))) ok = false;
            // (Well back from the bayou's banks.)
            for (int a = ha - 4; a <= ha + hs + 4 && ok; a += 2) {
                int aa = Math.max(0, Math.min(N - 1, a));
                if (hc - 8 < cb[aa] + wB / 2f + 4 && hc + hs + 8 > cb[aa] - wB / 2f - 4) ok = false;
            }
            if (ok) hamlets.add(q);
        }
    }

    static boolean near(int[] a, int[] b, int m) {
        return a[0] < b[2] + m && b[0] < a[2] + m && a[1] < b[3] + m && b[1] < a[3] + m;
    }

    static boolean inside(int[] q, float x, float y) {
        return x >= q[0] && y >= q[1] && x < q[2] && y < q[3];
    }

    @Override
    float[] districtSpot(int k, float x, float y, Random rr) {
        if (k == 0) return new float[]{(town[0] + town[2]) / 2f, (town[1] + town[3]) / 2f};
        if (rr.nextFloat() < 0.35f) return new float[]{x, y};
        // In the town, or now and then in a hamlet.
        int[] q = !hamlets.isEmpty() && rr.nextFloat() < 0.3f ? hamlets.get(rr.nextInt(hamlets.size())) : town;
        return new float[]{q[0] + 4 + rr.nextFloat() * (q[2] - q[0] - 8), q[1] + 4 + rr.nextFloat() * (q[3] - q[1] - 8)};
    }

    @Override
    int districtType(int k, float x, float y, int was) {
        // (The hamlets: old houses and homes.)
        for (int[] h : hamlets) if (inside(h, x, y)) return was == DT_DOWNTOWN || was == DT_INDUSTRIAL ? DT_OLDTOWN : was;
        return was == DT_DOWNTOWN && k > 0 ? DT_MIDTOWN : was;
    }

    @Override
    void streets() {
        int n = N * N;
        long seed = city.cfg.seed * 31L + 2626;
        deep = new boolean[n];
        built = new boolean[n];
        if (city.bridge == null) city.bridge = new boolean[n];
        if (city.bridgeKind == null) city.bridgeKind = new byte[n];
        // The swamp: pools and channels among the marsh.
        for (int y = 0; y < N; y++)
            for (int x = 0; x < N; x++) {
                float v = fbm(x / 22f, y / 22f, seed) * 0.75f + fbm(x / 7f, y / 7f, seed + 9) * 0.25f;
                city.tiles[y * N + x] = v < 0.43f ? WATER : GRASS;
            }
        // The bayou, edge to edge.
        for (int a = 0; a < N; a++) {
            int lo = Math.round(cb[a] - wB / 2f), hi = Math.round(cb[a] + wB / 2f);
            for (int c = lo; c < hi; c++) {
                int x = X(a, c), y = Y(a, c);
                if (!in(x, y)) continue;
                deep[y * N + x] = true;
                city.tiles[y * N + x] = WATER;
            }
        }
        // A side channel or two off it, narrower, wandering out to an edge of the map.
        int side = N < 250 ? 1 : 2;
        for (int s = 0; s < side; s++) {
            int aS = Math.round(N * (0.15f + r.nextFloat() * 0.7f));
            if (aS >= town[across ? 0 : 1] - 30 && aS <= town[across ? 2 : 3] + 30) aS = (aS + N / 2) % N;
            boolean toLow = townLow ? true : false;
            // (Away from the town's bank.)
            toLow = !townLow;
            float a = aS, c = cb[Math.max(0, Math.min(N - 1, aS))];
            float wS = Math.max(3, wB * 0.45f);
            float drift = (r.nextFloat() - 0.5f) * 0.9f;
            for (int step = 0; step < N * 2; step++) {
                c += toLow ? -1 : 1;
                a += drift + (float) Math.sin(step * 0.05f + s) * 0.7f;
                if (c < -2 || c > N + 2 || a < 0 || a >= N) break;
                for (int dc = -2; dc <= 2; dc++)
                    for (float da = -wS / 2; da <= wS / 2; da += 0.5f) {
                        int x = X(Math.round(a + da), Math.round(c) + dc), y = Y(Math.round(a + da), Math.round(c) + dc);
                        if (!in(x, y)) continue;
                        deep[y * N + x] = true;
                        city.tiles[y * N + x] = WATER;
                    }
            }
        }
        // Solid ground round the town and the hamlets, its edge wandering.
        List<int[]> all = new ArrayList<int[]>();
        all.add(town);
        all.addAll(hamlets);
        for (int[] q : all) {
            int m = 12;
            for (int y = Math.max(0, q[1] - m); y < Math.min(N, q[3] + m); y++)
                for (int x = Math.max(0, q[0] - m); x < Math.min(N, q[2] + m); x++) {
                    int dx = Math.max(Math.max(q[0] - x, x - q[2] + 1), 0), dy = Math.max(Math.max(q[1] - y, y - q[3] + 1), 0);
                    float d = (float) Math.sqrt(dx * dx + dy * dy);
                    float edge = 4 + 7 * noise(x / 9f, y / 9f, seed + 3);
                    int i = y * N + x;
                    if (d <= 0) built[i] = true;
                    if (d > edge || deep[i]) continue;
                    city.tiles[i] = GRASS;
                }
        }
        // The quay along the front, between the front street and the water.
        int qa0 = across ? town[0] : town[1], qa1 = across ? town[2] : town[3];
        for (int a = qa0; a < qa1; a++)
            for (int c = townLow ? frontC + 3 : frontC - 4; c < (townLow ? frontC + 7 : frontC); c++) {
                int x = X(a, c), y = Y(a, c);
                if (in(x, y) && !deep[y * N + x]) city.tiles[y * N + x] = PLAZA;
            }
        townPatch(town[0], town[1], town[2], town[3]);
        for (int[] h : hamlets) townPatch(h[0], h[1], h[2], h[3]);
        causeways();
        piers();
        stiltHouses();
        fishingCamps();
        // Trees: cypress stands in the marsh, the odd one by the town.
        for (int i = 0; i < n; i++) {
            if (city.tiles[i] != GRASS || built[i]) continue;
            int x = i % N, y = i / N;
            float wood = noise(x / 15f, y / 15f, seed + 17);
            if (r.nextFloat() < (wood > 0.55f ? 0.45f : 0.12f) && clearOfWays(x, y)) city.tiles[i] = TREE;
        }
        dropStranded(((town[1] + town[3]) / 2) * N + (town[0] + town[2]) / 2, 0.7f, r);
    }

    /** No road, pier or building right beside (so trees never block a way). */
    boolean clearOfWays(int x, int y) {
        for (int dy = -1; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++) {
                byte t = at(x + dx, y + dy);
                if (t == ROAD || t == PIER || t == BUILDING || t == SIDEWALK || t == PLAZA || t == LOT) return false;
            }
        return true;
    }

    /** A point on a ring road's middle line: the middle of side s (0 low a, 1 high a, 2 low c, 3 high c) of rectangle q. */
    float[] sideMid(int[] q, int s) {
        float mx = (q[0] + q[2]) / 2f, my = (q[1] + q[3]) / 2f;
        boolean alongX = across ? s < 2 : s >= 2;
        boolean low = s % 2 == 0;
        if (alongX) return new float[]{low ? q[0] + 1.5f : q[2] - 1.5f, my};
        return new float[]{mx, low ? q[1] + 1.5f : q[3] - 1.5f};
    }

    void causeways() {
        float wob = N * 0.05f;
        // Out to the edge of the map along the bayou, from the town's nearer end.
        boolean lowEnd = (across ? town[0] : town[1]) < N - (across ? town[2] : town[3]);
        float[] s0 = sideMid(town, lowEnd ? 0 : 1);
        float ec = (across ? s0[1] : s0[0]) + (townLow ? -1 : 1) * N * 0.06f;
        float ea = lowEnd ? -4 : N + 4;
        float[] e = across ? new float[]{ea, ec} : new float[]{ec, ea};
        float[] p = wander(s0[0], s0[1], e[0], e[1], wob, r);
        causeway(p, 2, deep);
        roads.add(p);
        // Over the bayou from the front street on a bridge, and on across the swamp to the far edge.
        float aB = (across ? town[0] + (town[2] - town[0]) * (0.3f + r.nextFloat() * 0.4f) : town[1] + (town[3] - town[1]) * (0.3f + r.nextFloat() * 0.4f));
        aB = Math.round(aB) + 0.5f;
        float cF = townLow ? frontC + 1.5f : frontC + 1.5f;
        float cFar = townLow ? base + wB / 2f + 10 : base - wB / 2f - 10;
        float cEnd = townLow ? N + 4 : -4;
        float[] b0 = across ? new float[]{aB, cF} : new float[]{cF, aB};
        float[] b1 = across ? new float[]{aB, cFar} : new float[]{cFar, aB};
        float ea2 = aB + (r.nextFloat() - 0.5f) * N * 0.3f;
        float[] b2 = across ? new float[]{ea2, cEnd} : new float[]{cEnd, ea2};
        float[] straight = {b0[0], b0[1], b1[0], b1[1]};
        causeway(straight, 2, deep);
        float[] on = wander(b1[0], b1[1], b2[0], b2[1], wob, r);
        causeway(on, 2, deep);
        roads.add(straight);
        roads.add(on);
        // Each hamlet joined to the nearest road.
        for (int[] h : hamlets) {
            float hx = (h[0] + h[2]) / 2f, hy = (h[1] + h[3]) / 2f;
            float best = Float.MAX_VALUE;
            float[] to = null;
            for (float[] rd : roads)
                for (int k = 0; k + 1 < rd.length; k += 2) {
                    float d = (rd[k] - hx) * (rd[k] - hx) + (rd[k + 1] - hy) * (rd[k + 1] - hy);
                    if (rd[k] < 2 || rd[k + 1] < 2 || rd[k] > N - 3 || rd[k + 1] > N - 3) continue;
                    if (d < best) {
                        best = d;
                        to = new float[]{rd[k], rd[k + 1]};
                    }
                }
            for (int s = 0; s < 4; s++) {
                // (The town's own sides too, but never the front.)
                if (s == (townLow ? 3 : 2)) continue;
                float[] m = sideMid(town, s);
                float d = (m[0] - hx) * (m[0] - hx) + (m[1] - hy) * (m[1] - hy);
                if (d < best) {
                    best = d;
                    to = m;
                }
            }
            if (to == null) continue;
            // From the hamlet's side facing it.
            float dx = to[0] - hx, dy = to[1] - hy;
            float[] from = Math.abs(dx) > Math.abs(dy) ? new float[]{dx < 0 ? h[0] + 1.5f : h[2] - 1.5f, hy} : new float[]{hx, dy < 0 ? h[1] + 1.5f : h[3] - 1.5f};
            float[] q = wander(from[0], from[1], to[0], to[1], wob * 0.6f, r);
            causeway(q, 2, deep);
            roads.add(q);
        }
    }

    /** Piers out into the bayou from the quay, every so often, with boats tied up. */
    void piers() {
        int qa0 = across ? town[0] : town[1], qa1 = across ? town[2] : town[3];
        int len = Math.max(3, Math.min(10, wB * 2 / 5));
        int bank = townLow ? frontC + 7 : frontC - 5;
        for (int a = qa0 + 8; a < qa1 - 8; a += 16 + r.nextInt(10)) {
            boolean clear = true;
            for (int k = 0; k < len && clear; k++)
                for (int d = -2; d <= 3; d++) {
                    int c = townLow ? bank + k : bank - k;
                    int x = X(a + d, c), y = Y(a + d, c);
                    if (!in(x, y) || city.tiles[y * N + x] != WATER || city.bridge[y * N + x]) clear = false;
                }
            if (!clear) continue;
            for (int k = 0; k < len; k++)
                for (int d = 0; d < 2; d++) {
                    int c = townLow ? bank + k : bank - k;
                    set(X(a + d, c), Y(a + d, c), PIER);
                }
            // A boat alongside.
            int c = townLow ? bank + len / 2 : bank - len / 2;
            int bx = X(a + 3, c), by = Y(a + 3, c);
            if (across) city.addDecor(D_BOAT, bx * T, by * T, bx * T + 22, by * T + 8, r.nextInt(2));
            else city.addDecor(D_BOAT, (bx - 1) * T, (by - 2) * T, (bx - 1) * T + 22, (by - 2) * T + 8, r.nextInt(2));
        }
    }

    /**
     * Stilt houses: boardwalks out from the edge of the town and the hamlets over the swamp, small houses on posts along
     * them each side.
     */
    void stiltHouses() {
        List<int[]> all = new ArrayList<int[]>();
        all.add(town);
        all.addAll(hamlets);
        for (int ti = 0; ti < all.size(); ti++) {
            int[] q = all.get(ti);
            int walks = ti == 0 ? Math.max(3, N / 90) : Math.max(1, N / 250);
            for (int w = 0, tries = 0; w < walks && tries < walks * 12; tries++) {
                int s = r.nextInt(4);
                // (Not off the town's front: that's the quay.)
                if (ti == 0 && ((across && s == (townLow ? 3 : 2)) || (!across && s == (townLow ? 1 : 0)))) continue;
                int dx = s == 0 ? -1 : s == 1 ? 1 : 0, dy = s == 2 ? -1 : s == 3 ? 1 : 0;
                int x, y;
                if (dx != 0) {
                    x = dx < 0 ? q[0] - 1 : q[2];
                    y = q[1] + 6 + r.nextInt(Math.max(1, q[3] - q[1] - 14));
                } else {
                    y = dy < 0 ? q[1] - 1 : q[3];
                    x = q[0] + 6 + r.nextInt(Math.max(1, q[2] - q[0] - 14));
                }
                int len = 14 + r.nextInt(Math.max(1, Math.min(24, N / 20)));
                if (stiltWalk(x, y, dx, dy, len)) w++;
            }
        }
    }

    /** A boardwalk from (x, y) out (dx, dy) len tiles, and houses along it: false if there's no room. */
    boolean stiltWalk(int x, int y, int dx, int dy, int len) {
        // Clear ground: water or marsh all the way, nothing built, no road.
        for (int k = 0; k < len + 4; k++)
            for (int s = -5; s <= 6; s++) {
                int xx = x + dx * k + (dx == 0 ? s : 0), yy = y + dy * k + (dy == 0 ? s : 0);
                if (!in(xx, yy)) return false;
                byte t = at(xx, yy);
                if (k > 0 && (t == ROAD || t == PIER || t == BUILDING || deep[yy * N + xx])) return false;
                if (k == 0 && s >= 0 && s <= 1 && t != GRASS && t != WATER && t != TREE) return false;
            }
        boardwalk(x, y, x + dx * (len - 1), y + dy * (len - 1));
        // The houses: either side, every few tiles, from a little way out.
        for (int k = 4; k + 3 < len; k += 5)
            for (int sd = -1; sd <= 1; sd += 2) {
                int hw = 3, hl = 3;
                int hx, hy;
                if (dx != 0) {
                    hx = Math.min(x + dx * k, x + dx * (k + hl - 1));
                    hy = sd < 0 ? y - hw : y + 2;
                } else {
                    hy = Math.min(y + dy * k, y + dy * (k + hl - 1));
                    hx = sd < 0 ? x - hw : x + 2;
                }
                if (!all(hx, hy, hw, hl, WATER, GRASS, TREE)) continue;
                lotGen.addLot(hx, hy, hw, hl, HOUSE);
            }
        // A platform at the end, with a boat.
        int ex = x + dx * (len - 1), ey = y + dy * (len - 1);
        boardwalk(ex - 1, ey - 1, ex + 1, ey + 1);
        return true;
    }

    /** Fishing camps far out in the swamp: cabins on a platform, a long boardwalk back to the nearest road. */
    void fishingCamps() {
        int want = 2 + N / 200;
        for (int tries = 0, made = 0; tries < 300 && made < want; tries++) {
            int cx = 10 + r.nextInt(N - 20), cy = 10 + r.nextInt(N - 20);
            boolean ok = true;
            for (int y = cy - 6; y <= cy + 6 && ok; y++)
                for (int x = cx - 7; x <= cx + 7 && ok; x++) {
                    byte t = at(x, y);
                    if (!in(x, y) || built[y * N + x] || deep[y * N + x] || t == ROAD || t == PIER || t == BUILDING) ok = false;
                }
            if (!ok) continue;
            // The nearest road (a causeway's middle line), not too near and not too far.
            float best = Float.MAX_VALUE;
            float[] to = null;
            for (float[] rd : roads)
                for (int k = 0; k + 1 < rd.length; k += 2) {
                    if (rd[k] < 3 || rd[k + 1] < 3 || rd[k] > N - 4 || rd[k + 1] > N - 4) continue;
                    float d = (rd[k] - cx) * (rd[k] - cx) + (rd[k + 1] - cy) * (rd[k + 1] - cy);
                    if (d < best) {
                        best = d;
                        to = new float[]{rd[k], rd[k + 1]};
                    }
                }
            if (to == null || best < 25 * 25 || best > (N * 0.3f) * (N * 0.3f)) continue;
            int tx = (int) to[0], ty = (int) to[1];
            // The way back: along x then y, or y then x, whichever crosses nothing but swamp till the road's bank.
            int[] corner = walkable(cx, cy, tx, cy, tx, ty) ? new int[]{tx, cy} : walkable(cx, cy, cx, ty, tx, ty) ? new int[]{cx, ty} : null;
            if (corner == null) continue;
            city.fill(cx - 4, cy - 3, 9, 7, PIER);
            lotGen.addLot(cx - 4, cy - 3, 3, 3, HOUSE);
            lotGen.addLot(cx + 2, cy - 3, 3, 3, HOUSE);
            if (r.nextBoolean()) lotGen.addLot(cx - 1, cy + 1, 3, 3, HOUSE);
            walkTo(cx, cy, corner[0], corner[1]);
            walkTo(corner[0], corner[1], tx, ty);
            city.addDecor(D_BOAT, (cx + 5) * T, cy * T, (cx + 5) * T + 22, cy * T + 8, 0);
            made++;
        }
    }

    /** Whether boardwalks from (ax, ay) to the corner (bx, by) and on towards (tx, ty) cross nothing they shouldn't. */
    boolean walkable(int ax, int ay, int bx, int by, int tx, int ty) {
        return clearLine(ax, ay, bx, by) && clearLine(bx, by, tx, ty);
    }

    boolean clearLine(int ax, int ay, int bx, int by) {
        int n = Math.max(Math.abs(bx - ax), Math.abs(by - ay));
        for (int k = 0; k <= n; k++) {
            int x = ax + Integer.signum(bx - ax) * Math.min(k, Math.abs(bx - ax)), y = ay + Integer.signum(by - ay) * Math.min(k, Math.abs(by - ay));
            for (int d = 0; d < 2; d++) {
                byte t = at(x + (ax == bx ? d : 0), y + (ay == by ? d : 0));
                if (t == ROAD) return true;
                if (t == BUILDING || t == LOT || t == SIDEWALK || t == PLAZA || t == BASE) return false;
            }
        }
        return true;
    }

    /** Lays a boardwalk from (ax, ay) to (bx, by) (a straight line), stopping at the first road. */
    void walkTo(int ax, int ay, int bx, int by) {
        int n = Math.max(Math.abs(bx - ax), Math.abs(by - ay));
        for (int k = 0; k <= n; k++) {
            int x = ax + Integer.signum(bx - ax) * Math.min(k, Math.abs(bx - ax)), y = ay + Integer.signum(by - ay) * Math.min(k, Math.abs(by - ay));
            boolean hit = false;
            for (int d = 0; d < 2; d++) {
                int xx = x + (ax == bx ? d : 0), yy = y + (ay == by ? d : 0);
                if (at(xx, yy) == ROAD) hit = true;
                else if (at(xx, yy) == WATER || at(xx, yy) == GRASS || at(xx, yy) == TREE) set(xx, yy, PIER);
            }
            if (hit) return;
        }
    }

    @Override
    void places(boolean[] used) {
        if (gen.lmRnd == null) {
            gen.lmRnd = new Random(city.cfg.seed * 7919L + 35);
            gen.lmCount = new int[Landmarks.all().size()];
        }
        // Along the front street: the bait shops, the seafood shack and the airboat landing.
        String[] front = {"Bait & Tackle", "Airboat Landing", "Seafood Shack", "Bait & Tackle"};
        int next = 0;
        int c = townLow ? frontC : frontC + 3;
        for (int pass = 0; pass < 2 && next < front.length; pass++)
            for (int k = 0; k < city.blocks.size() && next < front.length; k++) {
                int[] b = city.blocks.get(k);
                if (used[k] || b.length > 4) continue;
                boolean touches = across ? (townLow ? b[3] == c : b[1] == c) : (townLow ? b[2] == c : b[0] == c);
                if (!touches || freeBlocks(town, used) <= 2) continue;
                if (place(b, front[next])) {
                    used[k] = true;
                    next++;
                }
            }
        // One in each hamlet.
        for (int[] h : hamlets)
            for (int k = 0; k < city.blocks.size(); k++) {
                int[] b = city.blocks.get(k);
                if (used[k] || b.length > 4 || !inside(h, (b[0] + b[2]) / 2f, (b[1] + b[3]) / 2f) || freeBlocks(h, used) <= 2) continue;
                if (place(b, "Bait & Tackle")) {
                    used[k] = true;
                    break;
                }
            }
    }

    boolean place(int[] b, String name) {
        int[] in = {b[0] + 1, b[1] + 1, b[2] - 1, b[3] - 1};
        Landmarks.Def d = CityGenerator.landmarkNamed(name);
        int pw = in[2] - in[0], ph = in[3] - in[1];
        if (d == null || Math.min(pw, ph) < d.minS || Math.max(pw, ph) < d.minL || Math.max(pw, ph) > 40) return false;
        city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], SIDEWALK);
        return gen.placeParcel(in, name);
    }

    @Override
    void finish() {
        // Flat: the swamp lies at the water's level.
        if (city.elev != null) for (int i = 0; i < N * N; i++) city.elev.set(i, city.elev.get(i) * 0.12f);
        if (city.treeKind != null)
            for (int i = 0; i < N * N; i++)
                if (city.tiles[i] == TREE && !built[i] && r.nextFloat() < 0.25f) city.setTreeKind(i, Country.TK_SCRUB);
        nameWild(built, new String[]{" Bayou", " Swamp", " Marsh", " Slough"});
        // Boats out on the bayou.
        for (int k = 0; k < N / 25; k++) {
            int a = 4 + r.nextInt(N - 8);
            int c = Math.round(cb[a]);
            int x = X(a, c), y = Y(a, c);
            if (!in(x, y) || at(x, y) != WATER || city.bridge[y * N + x]) continue;
            city.addDecor(D_BOAT, x * T, y * T, x * T + 22, y * T + 8, r.nextInt(2));
        }
    }
}
