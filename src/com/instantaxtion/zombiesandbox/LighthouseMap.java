package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.City.*;

/**
 * ((28.0) Lighthouse Coast: the sea along one side of the map (a different side, and a different shore, on every map),
 * a rugged coast of cliffs no one can climb, with coves and their beaches between headlands. One winding coast road
 * runs the length of it from edge to edge, and strung along it are the fishing villages, each at a cove with its quay
 * and piers, and the harbour town, the biggest, with its breakwater, fish market and coast guard. Out on the furthest
 * headland stands the lighthouse at the end of its own lane. Inland: hills, woods and farms down their lanes.
 */
final class LighthouseMap extends WaterMap {
    LighthouseMap(City city) {
        super(city);
    }

    /** Which side the sea is on: 0 top, 1 bottom, 2 left, 3 right. */
    int sea;
    int N;
    /** How far in from the sea's edge of the map the land starts, and the road's middle line, at each place along. */
    float[] shore, road;
    /** Where the coves are (along the coast), and the headland the lighthouse is on. */
    final List<Integer> coves = new ArrayList<Integer>();
    int headA = -1;
    /** The villages {a0, a1} along the coast, the harbour town first; and as tiles. */
    final List<int[]> spans = new ArrayList<int[]>();
    final List<int[]> towns = new ArrayList<int[]>();
    int[] light;
    boolean[] built;
    Random r;

    /** Tile x of the frame's (a along the coast, c in from the sea's edge). */
    int X(int a, int c) {
        return sea == 2 ? c : sea == 3 ? N - 1 - c : a;
    }

    int Y(int a, int c) {
        return sea == 0 ? c : sea == 1 ? N - 1 - c : a;
    }

    float Xf(float a, float c) {
        return sea == 2 ? c : sea == 3 ? N - c : a;
    }

    float Yf(float a, float c) {
        return sea == 0 ? c : sea == 1 ? N - c : a;
    }

    /** A rectangle in the frame as tiles {x0, y0, x1, y1}. */
    int[] rect(int a0, int c0, int a1, int c1) {
        int xa = X(a0, c0), ya = Y(a0, c0), xb = X(a1 - 1, c1 - 1), yb = Y(a1 - 1, c1 - 1);
        return new int[]{Math.min(xa, xb), Math.min(ya, yb), Math.max(xa, xb) + 1, Math.max(ya, yb) + 1};
    }

    @Override
    void layout() {
        N = city.w;
        r = new Random(city.cfg.seed * 4271L + 27);
        sea = r.nextInt(4);
        shore = new float[N];
        road = new float[N];
        float base = N * (0.27f + r.nextFloat() * 0.06f);
        float A = N * 0.03f, f = (float) (Math.PI * 2 / (N * (0.5f + r.nextFloat() * 0.4f))), p = r.nextFloat() * 6.28f;
        // The villages' places along the coast: spread out, the harbour town at the widest.
        int nv = Math.max(2, Math.min(5, 2 + N / 260));
        int VA = Math.max(40, Math.round(N * 0.1f)), HA = Math.max(64, Math.round(N * 0.22f));
        int total = HA + (nv - 1) * VA;
        int free = N - total - 2 * Math.max(10, N / 20);
        int harbourAt = r.nextInt(nv);
        int a = Math.max(10, N / 20);
        float gapEach = Math.max(8, free / (float) nv);
        for (int k = 0; k < nv; k++) {
            int len = k == harbourAt ? HA : VA;
            int a0 = a + Math.round(gapEach * (0.3f + r.nextFloat() * 0.7f));
            if (a0 + len > N - 6) break;
            int[] sp = {a0, a0 + len};
            if (k == harbourAt) spans.add(0, sp);
            else spans.add(sp);
            a = a0 + len;
        }
        // The shore: a slow wander, a cove in front of each village, headlands between them.
        float[] bump = new float[N];
        for (int[] sp : spans) {
            float mid = (sp[0] + sp[1]) / 2f, wd = (sp[1] - sp[0]) * 0.45f, depth = Math.max(10, (sp[1] - sp[0]) * 0.5f);
            for (int i = 0; i < N; i++) bump[i] += depth * (float) Math.exp(-((i - mid) * (i - mid)) / (2 * wd * wd));
            coves.add(Math.round(mid));
        }
        // The headlands: out between the coves, the biggest the lighthouse's.
        int bestGap = -1;
        List<Integer> stops = new ArrayList<Integer>();
        stops.add(0);
        for (int[] sp : spans) {
            stops.add(sp[0]);
            stops.add(sp[1]);
        }
        stops.add(N);
        java.util.Collections.sort(stops);
        for (int k = 0; k + 1 < stops.size(); k += 2) {
            int g0 = stops.get(k), g1 = stops.get(k + 1);
            if (g1 - g0 < 24) continue;
            int mid = (g0 + g1) / 2;
            float hd = Math.min(base * 0.6f, (g1 - g0) * 0.35f), wd = (g1 - g0) * 0.26f;
            for (int i = 0; i < N; i++) bump[i] -= hd * (float) Math.exp(-((i - mid) * (i - mid)) / (2 * wd * wd));
            if (g1 - g0 > bestGap && mid > 20 && mid < N - 20) {
                bestGap = g1 - g0;
                headA = mid;
            }
        }
        for (int i = 0; i < N; i++) shore[i] = base + A * (float) Math.sin(i * f + p) + bump[i] + 2 * (float) Math.sin(i * 0.21f + p * 3);
        // The road: well up from the shore, smoothed; straight past each village (its front street).
        int win = Math.max(12, N / 40);
        float[] hi = new float[N];
        for (int i = 0; i < N; i++) {
            float m = 0;
            for (int j = Math.max(0, i - win); j <= Math.min(N - 1, i + win); j++) m = Math.max(m, shore[j]);
            hi[i] = m + Math.max(12, N / 50f);
        }
        for (int i = 0; i < N; i++) {
            float s = 0;
            int c = 0;
            for (int j = Math.max(0, i - win); j <= Math.min(N - 1, i + win); j++) {
                s += hi[j];
                c++;
            }
            road[i] = Math.max(hi[i] - 4, s / c);
        }
        int ramp = Math.max(16, N / 24);
        float[] was = road.clone();
        for (int[] sp : spans) {
            float cv = 0;
            for (int i = Math.max(0, sp[0] - ramp); i < Math.min(N, sp[1] + ramp); i++) cv = Math.max(cv, was[i]);
            cv = Math.round(cv) + 0.5f;
            for (int i = Math.max(0, sp[0] - ramp); i < Math.min(N, sp[1] + ramp); i++) {
                float t = i < sp[0] ? (i - (sp[0] - ramp)) / (float) ramp : i >= sp[1] ? ((sp[1] + ramp) - i) / (float) ramp : 1;
                t = t * t * (3 - 2 * t);
                road[i] = Math.max(road[i], road[i] * (1 - t) + cv * t);
            }
            for (int i = sp[0]; i < sp[1]; i++) road[i] = cv;
        }
        // The towns: inland of the road, their front street on it.
        for (int k = 0; k < spans.size(); k++) {
            int[] sp = spans.get(k);
            int c0 = Math.round(road[sp[0]] - 1.5f);
            int depth = k == 0 ? Math.max(48, Math.round(N * 0.22f)) : Math.max(34, Math.round(N * 0.11f));
            depth = Math.min(depth, N - c0 - 8);
            towns.add(rect(sp[0], c0, sp[1], c0 + depth));
        }
    }

    @Override
    float[] districtSpot(int k, float x, float y, Random rr) {
        int[] h = towns.get(0);
        if (k == 0) return new float[]{(h[0] + h[2]) / 2f, (h[1] + h[3]) / 2f};
        if (rr.nextFloat() < 0.3f) return new float[]{x, y};
        int[] q = towns.get(rr.nextFloat() < 0.4f ? 0 : rr.nextInt(towns.size()));
        return new float[]{q[0] + 3 + rr.nextFloat() * (q[2] - q[0] - 6), q[1] + 3 + rr.nextFloat() * (q[3] - q[1] - 6)};
    }

    @Override
    int districtType(int k, float x, float y, int was) {
        for (int i = 1; i < towns.size(); i++) {
            int[] q = towns.get(i);
            if (x >= q[0] && y >= q[1] && x < q[2] && y < q[3]) return was == DT_DOWNTOWN || was == DT_INDUSTRIAL || was == DT_CAMPUS ? DT_OLDTOWN : was;
        }
        return was == DT_DOWNTOWN && k > 0 ? DT_MIDTOWN : was;
    }

    boolean inCove(int a) {
        for (int[] sp : spans) if (a >= sp[0] - 4 && a < sp[1] + 4) return true;
        return false;
    }

    @Override
    void streets() {
        int n = N * N;
        long seed = city.cfg.seed * 53L + 2727;
        built = new boolean[n];
        if (city.bridge == null) city.bridge = new boolean[n];
        // The sea, the shore (cliffs, or a beach in a cove), the land.
        for (int a = 0; a < N; a++) {
            boolean cove = inCove(a);
            float s = shore[a];
            int beach = cove ? 3 + (int) (2 * noise(a / 9f, 0, seed)) : 0;
            int cliff = cove ? 0 : 3 + (int) (5 * noise(a / 7f, 3, seed));
            for (int c = 0; c < N; c++) {
                int x = X(a, c), y = Y(a, c), i = y * N + x;
                byte t;
                if (c < s) t = WATER;
                else if (c < s + beach) t = SAND;
                else if (c < s + cliff) t = ROCK;
                else t = GRASS;
                city.tiles[i] = t;
            }
        }
        // Rocks offshore under the cliffs.
        for (int k = 0; k < N / 6; k++) {
            int a = r.nextInt(N);
            if (inCove(a)) continue;
            int c = Math.round(shore[a]) - 1 - r.nextInt(4);
            for (int d = 0; d < 1 + r.nextInt(3); d++) set(X(a + d, c), Y(a + d, c), ROCK);
        }
        for (int[] q : towns) {
            townPatch(q[0], q[1], q[2], q[3]);
            for (int y = q[1]; y < q[3]; y++) for (int x = q[0]; x < q[2]; x++) if (in(x, y)) built[y * N + x] = true;
        }
        // The coast road, edge to edge.
        int step = 6;
        int np = N / step + 3;
        float[] pts = new float[np * 2];
        for (int k = 0; k < np; k++) {
            float a = -6 + k * step;
            float c = road[Math.max(0, Math.min(N - 1, Math.round(a)))];
            pts[k * 2] = Xf(a, c);
            pts[k * 2 + 1] = Yf(a, c);
        }
        causeway(pts, 0, null);
        harbours();
        lighthouse();
        farms();
        // Woods and meadows inland; scrub and pines on the headlands.
        for (int i = 0; i < n; i++) {
            if (city.tiles[i] != GRASS || built[i]) continue;
            int x = i % N, y = i / N;
            float wood = fbm(x / 30f, y / 30f, seed + 5);
            if (r.nextFloat() < (wood > 0.52f ? 0.55f : wood > 0.45f ? 0.14f : 0.03f) && clearOfWays(x, y)) city.tiles[i] = TREE;
        }
    }

    boolean clearOfWays(int x, int y) {
        for (int dy = -2; dy <= 2; dy++)
            for (int dx = -2; dx <= 2; dx++) {
                byte t = at(x + dx, y + dy);
                if (t == ROAD || t == PIER || t == BUILDING || t == SIDEWALK || t == PLAZA || t == LOT || t == DIRT) return false;
            }
        return true;
    }

    /** At each village's cove: a quay along the beach, piers and boats; at the harbour town a breakwater too. */
    void harbours() {
        for (int k = 0; k < spans.size(); k++) {
            int[] sp = spans.get(k);
            int mid = (sp[0] + sp[1]) / 2;
            // The quay: the beach's top edge, paved, the width of the village.
            for (int a = sp[0] + 2; a < sp[1] - 2; a++) {
                int c = Math.round(shore[a]);
                for (int d = 0; d < 3; d++) {
                    int x = X(a, c + d), y = Y(a, c + d);
                    if (at(x, y) == SAND || at(x, y) == GRASS || at(x, y) == ROCK) set(x, y, PLAZA);
                }
            }
            int len = k == 0 ? Math.max(8, N / 40) : Math.max(5, N / 70);
            for (int a = sp[0] + 5; a < sp[1] - 6; a += k == 0 ? 14 : 20) {
                int c = Math.round(shore[a]) - 1;
                for (int d = 0; d < len; d++)
                    for (int w = 0; w < 2; w++) {
                        int x = X(a + w, c - d), y = Y(a + w, c - d);
                        if (at(x, y) == WATER) set(x, y, PIER);
                    }
                int bx = X(a + 3, c - len / 2), by = Y(a + 3, c - len / 2);
                if (at(bx, by) == WATER) city.addDecor(D_BOAT, bx * T, by * T, bx * T + 22, by * T + 8, r.nextInt(2));
            }
            if (k == 0) {
                // The breakwater: two arms of rock out from the ends of the harbour, turning in towards each other.
                int out = Math.max(10, N / 30);
                for (int e = 0; e < 2; e++) {
                    int a = e == 0 ? sp[0] + 1 : sp[1] - 3;
                    int c0 = Math.round(shore[a]);
                    for (int d = 0; d < out; d++)
                        for (int w = 0; w < 2; w++) set(X(a + w, c0 - d), Y(a + w, c0 - d), ROCK);
                    int tip = c0 - out;
                    int dir = e == 0 ? 1 : -1;
                    for (int d = 0; d < (sp[1] - sp[0]) / 4; d++)
                        for (int w = 0; w < 2; w++) set(X(a + dir * d, tip + w), Y(a + dir * d, tip + w), ROCK);
                }
                for (int b = 0; b < 4; b++) {
                    int a = sp[0] + 6 + r.nextInt(Math.max(1, sp[1] - sp[0] - 12)), c = Math.round(shore[a]) - 4 - r.nextInt(Math.max(1, out - 6));
                    int bx = X(a, c), by = Y(a, c);
                    if (at(bx, by) == WATER && at(bx + 2, by) == WATER) city.addDecor(D_BOAT, bx * T, by * T, bx * T + 26, by * T + 9, r.nextInt(3));
                }
            }
        }
        // Boats out at sea.
        for (int k = 0; k < N / 25; k++) {
            int a = r.nextInt(N), c = r.nextInt(Math.max(1, Math.round(shore[a]) - 6));
            int x = X(a, c), y = Y(a, c);
            if (at(x, y) == WATER && at(x + 2, y) == WATER) city.addDecor(D_BOAT, x * T, y * T, x * T + 30, y * T + 10, r.nextInt(3));
        }
    }

    /** The lighthouse on the furthest headland, its lane down from the coast road. */
    void lighthouse() {
        if (headA < 0) {
            // (No headland between the villages: the furthest point out.)
            float best = Float.MAX_VALUE;
            for (int a = 20; a < N - 20; a++)
                if (!inCove(a) && shore[a] < best) {
                    best = shore[a];
                    headA = a;
                }
        }
        if (headA < 0) return;
        int L = 14, S = 12;
        int a0 = headA - L / 2;
        int c0 = Math.round(shore[headA]) + 3;
        for (int a = a0 - 1; a <= a0 + L; a++) c0 = Math.max(c0, Math.round(shore[Math.max(0, Math.min(N - 1, a))]) + 3);
        int[] q = rect(a0, c0, a0 + L, c0 + S);
        // Ground for it: grass, cliffs round the seaward side.
        for (int y = q[1] - 1; y <= q[3]; y++) for (int x = q[0] - 1; x <= q[2]; x++) if (in(x, y)) set(x, y, SIDEWALK);
        light = q;
        for (int y = q[1]; y < q[3]; y++) for (int x = q[0]; x < q[2]; x++) built[y * N + x] = true;
        // The lane: from the parcel's landward side up to the coast road.
        float ca = headA + 0.5f, cStart = c0 + S + 1.5f, cEnd = road[headA];
        if (cEnd - cStart > 2) {
            float[] pts = {Xf(ca, cStart), Yf(ca, cStart), Xf(ca, cEnd), Yf(ca, cEnd)};
            causeway(pts, 0, null);
        }
    }

    /** The back lane: a dirt lane winding through the farmland behind the harbour town, out to the coast road at each end. */
    final List<int[]> lane = new ArrayList<int[]>();

    void backLane() {
        int[] h = towns.get(0);
        int[] sp = spans.get(0);
        int ha = (sp[0] + sp[1]) / 2;
        int hc = Math.round(road[ha]) + (sea < 2 ? h[3] - h[1] : h[2] - h[0]) - 2;
        int cl = hc + Math.max(14, (N - hc) / 3);
        if (cl > N - 20) return;
        // From the harbour town's back street out to the lane's line, then along it each way.
        lane.add(new int[]{X(ha, hc), Y(ha, hc)});
        int[] start = {X(ha, cl), Y(ha, cl)};
        landGen.laneTo(lane.get(0)[0], lane.get(0)[1], start[0], start[1]);
        for (int dir = -1; dir <= 1; dir += 2) {
            int[] at = start;
            int a = ha;
            while (true) {
                a += dir * Math.max(30, N / 14);
                if (a < 12 || a > N - 13) break;
                int c = cl + Math.round((noise(a / 60f, 5, city.cfg.seed) - 0.5f) * N * 0.12f);
                c = Math.max(Math.round(road[a]) + 20, Math.min(N - 14, c));
                int[] nx = {X(a, c), Y(a, c)};
                landGen.laneTo(at[0], at[1], nx[0], nx[1]);
                lane.add(nx);
                at = nx;
            }
            // Down to the coast road near the edge.
            int ea = dir < 0 ? 8 : N - 9;
            landGen.laneTo(at[0], at[1], X(ea, Math.round(road[ea])), Y(ea, Math.round(road[ea])));
        }
    }

    /** Farms inland, each down a lane to the back lane or the coast road. */
    void farms() {
        backLane();
        int want = N / 50;
        for (int t = 0, made = 0; t < 200 && made < want; t++) {
            int a = 10 + r.nextInt(N - 40);
            int c = Math.round(road[a]) + 14 + r.nextInt(Math.max(1, N - Math.round(road[a]) - 60));
            int fw = 22 + r.nextInt(10), fh = 16 + r.nextInt(8);
            int[] q = rect(a, c, a + fw, c + fh);
            boolean ok = true;
            for (int y = q[1] - 4; y < q[3] + 4 && ok; y++)
                for (int x = q[0] - 4; x < q[2] + 4 && ok; x++) if (!in(x, y) || at(x, y) != GRASS || built[y * N + x]) ok = false;
            if (!ok) continue;
            landGen.farm(q[0], q[1], q[2] - q[0], q[3] - q[1]);
            for (int y = q[1]; y < q[3]; y++) for (int x = q[0]; x < q[2]; x++) built[y * N + x] = true;
            // The lane: from the farm's seaward edge to the road.
            float ra = a + fw / 2f;
            int ax = X(Math.round(ra), c - 1), ay = Y(Math.round(ra), c - 1);
            int tx = X(Math.round(ra), Math.round(road[Math.round(ra)])), ty = Y(Math.round(ra), Math.round(road[Math.round(ra)]));
            // (To the back lane if it's nearer.)
            for (int[] l : lane)
                if (Math.abs(l[0] - ax) + Math.abs(l[1] - ay) < Math.abs(tx - ax) + Math.abs(ty - ay)) {
                    tx = l[0];
                    ty = l[1];
                }
            landGen.laneTo(ax, ay, tx, ty);
            made++;
        }
    }

    @Override
    void places(boolean[] used) {
        if (gen.lmRnd == null) {
            gen.lmRnd = new Random(city.cfg.seed * 7919L + 35);
            gen.lmCount = new int[Landmarks.all().size()];
        }
        if (light != null) gen.placeParcel(light, "Lighthouse");
        // The harbour town's fish market down on the quay, between the front street and the water.
        boolean market = false;
        int[] hs = spans.get(0);
        int mid = (hs[0] + hs[1]) / 2;
        for (int off = 0; off < (hs[1] - hs[0]) / 2 - 10 && !market; off += 6)
            for (int sg = -1; sg <= 1 && !market; sg += 2) {
                int a0 = mid + sg * off - 8, a1 = a0 + 16;
                float sh = 0, rd = Float.MAX_VALUE;
                for (int a = a0; a < a1; a++) {
                    sh = Math.max(sh, shore[a]);
                    rd = Math.min(rd, road[a]);
                }
                int c0 = (int) Math.ceil(sh) + 4, c1 = (int) Math.floor(rd - 2.6f);
                if (c1 - c0 < 8) continue;
                c0 = Math.max(c0, c1 - 12);
                int[] pc = rect(a0, c0, a1, c1);
                market = gen.placeParcel(pc, "Fish Market");
                if (market) for (int y = pc[1]; y < pc[3]; y++) for (int x = pc[0]; x < pc[2]; x++) built[y * N + x] = true;
            }
        // On the harbour town's front street: the fish market and the coast guard; a fish market in each village.
        for (int t = 0; t < towns.size(); t++) {
            String[] names = t == 0 ? new String[]{market ? "Coast Guard Station" : "Fish Market", "Coast Guard Station"} : new String[]{"Fish Market"};
            int[] q = towns.get(t);
            int next = 0;
            for (int k = 0; k < city.blocks.size() && next < names.length; k++) {
                int[] b = city.blocks.get(k);
                if (used[k] || b.length > 4 || !front(q, b) || freeBlocks(q, used) <= (t == 0 ? 3 : 2)) continue;
                int[] in = {b[0] + 1, b[1] + 1, b[2] - 1, b[3] - 1};
                Landmarks.Def d = CityGenerator.landmarkNamed(names[next]);
                int pw = in[2] - in[0], ph = in[3] - in[1];
                if (d == null || Math.min(pw, ph) < d.minS || Math.max(pw, ph) < d.minL || Math.max(pw, ph) > 40) continue;
                city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], SIDEWALK);
                gen.placeParcel(in, names[next++]);
                used[k] = true;
            }
        }
    }

    /** Whether block b is on town q's front street (the coast road's side). */
    boolean front(int[] q, int[] b) {
        if (b[0] < q[0] || b[1] < q[1] || b[2] > q[2] || b[3] > q[3]) return false;
        switch (sea) {
            case 0: return b[1] == q[1] + 3;
            case 1: return b[3] == q[3] - 3;
            case 2: return b[0] == q[0] + 3;
            default: return b[2] == q[2] - 3;
        }
    }

    @Override
    void finish() {
        // The sea's districts named for the water, the country's for the hills.
        Random nr = new Random(city.cfg.seed * 401L + 27);
        String[] bay = {" Bay", " Sound", " Point", " Reach"}, hills = {" Hills", " Woods", " Downs", " Head"};
        for (District d : city.districts) {
            int x = (int) d.x, y = (int) d.y;
            if (!in(x, y) || built[y * N + x]) continue;
            boolean wet = at(x, y) == WATER;
            boolean inTown = false;
            for (int[] q : towns) if (x >= q[0] - 6 && y >= q[1] - 6 && x < q[2] + 6 && y < q[3] + 6) inTown = true;
            if (inTown) continue;
            String w = city.country.districtWords[nr.nextInt(city.country.districtWords.length)];
            String[] e = wet ? bay : hills;
            String name = w + e[nr.nextInt(e.length)];
            for (District o : city.districts) if (o != d && o.name.equals(name)) name = w + (wet ? " Cove" : " Fields");
            d.name = name;
        }
        // Pines on the cliff tops.
        if (city.treeKind != null)
            for (int i = 0; i < N * N; i++) if (city.tiles[i] == TREE && r.nextFloat() < 0.4f) city.setTreeKind(i, Country.TK_PINE);
    }
}
