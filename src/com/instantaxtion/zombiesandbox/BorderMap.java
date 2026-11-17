package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.City.*;

/**
 * ((28.0) Border Town: a town split right across the map by a fenced border - two fences with a cleared strip and
 * watchtowers between them, nothing crossing it but the roads through its two or three checkpoints. Each crossing has a
 * customs yard on either side; each side its border patrol station and a frontier road along the fence. The two halves
 * are built differently: one side a grid of red brick, the other old stucco quarters of narrow wandering streets with
 * terracotta roofs and warm paving. Everything it changes is for this map alone.
 */
final class BorderMap extends CityPart {
    BorderMap(City city) {
        super(city);
    }

    /** Whether the border runs north to south (the fence down a column); where its first fence is (across). */
    boolean vert;
    int bc = -1, cd;
    /** The range along the border, and across it. */
    int a0, a1, c0, c1;
    /** Where each crossing's road starts (along the border, five tiles wide). */
    final List<Integer> crossings = new ArrayList<Integer>();
    /** The customs yards' blocks and the border patrol stations' (one each side), as added to the city's blocks. */
    final List<int[]> yards = new ArrayList<int[]>(), patrols = new ArrayList<int[]>();
    /** Which side of the border each district is on (true for the far side, across from the first). */
    boolean[] farSide;

    static final int FENCE_W = 7;

    /** A rectangle given along the border (a) and across it (c), as tiles {x0, y0, x1, y1}. */
    int[] rect(int ra0, int rc0, int ra1, int rc1) {
        return vert ? new int[]{rc0, ra0, rc1, ra1} : new int[]{ra0, rc0, ra1, rc1};
    }

    int tile(int a, int c) {
        return vert ? a * city.w + c : c * city.w + a;
    }

    /** A street along the border (alongA) or across it. */
    void road(int ra0, int rc0, int ra1, int rc1, boolean alongA, boolean main) {
        int[] q = rect(ra0, rc0, ra1, rc1);
        gen.carve(new Street(q[0], q[1], q[2], q[3], alongA == vert, main));
    }

    void splitAC(int ra0, int rc0, int ra1, int rc1) {
        if (ra1 - ra0 < 6 || rc1 - rc0 < 6) return;
        int[] q = rect(ra0, rc0, ra1, rc1);
        gen.split(q[0], q[1], q[2], q[3], 1, -1, -1);
    }

    /** Where the across coordinate c is: the near side (false) or the far side of the border. */
    boolean far(int c) {
        return (c >= bc + FENCE_W / 2) != swap;
    }

    /** Whether the far side (the old quarters) is the first side, before the border, rather than the second. */
    boolean swap;
    /** The far side's squares: a block by each crossing's avenue, kept for a plaza. */
    final List<int[]> plazas = new ArrayList<int[]>();

    /**
     * The far side's streets: a close grid of small square blocks, all the same size, three-tile streets between them,
     * and the block by the crossing's avenue nearest the frontier kept for the square.
     */
    void gridRegion(int ra0, int rc0, int ra1, int rc1, Random r, boolean plazaAtStart) {
        if (ra1 - ra0 < 6 || rc1 - rc0 < 6) return;
        int B = 15 + r.nextInt(6);
        int na = Math.max(1, Math.round((ra1 - ra0 + 3) / (float) (B + 3))), nc = Math.max(1, Math.round((rc1 - rc0 + 3) / (float) (B + 3)));
        int[] as = cuts(ra0, ra1, na), cs = cuts(rc0, rc1, nc);
        for (int i = 1; i < na; i++) road(as[i] - 3, rc0, as[i], rc1, false, false);
        for (int j = 1; j < nc; j++) road(ra0, cs[j] - 3, ra1, cs[j], true, false);
        // (The square by the frontier road, in the middle of the stretch along it.)
        int pi = na / 2, pj = plazaAtStart ? 0 : nc - 1;
        for (int i = 0; i < na; i++)
            for (int j = 0; j < nc; j++) {
                int[] q = rect(as[i], cs[j], as[i + 1] - 3, cs[j + 1] - 3);
                if (q[2] - q[0] < 4 || q[3] - q[1] < 4) continue;
                city.blocks.add(q);
                if (i == pi && j == pj && q[2] - q[0] >= 10 && q[3] - q[1] >= 10) plazas.add(q);
            }
    }

    /** n pieces of lo..hi, three-tile streets between them: the starts, and hi plus 3 at the end. */
    private static int[] cuts(int lo, int hi, int n) {
        int[] s = new int[n + 1];
        for (int i = 0; i <= n; i++) s[i] = lo + Math.round((hi - lo + 3) * i / (float) n);
        s[n] = hi + 3;
        for (int i = 1; i < n; i++) s[i] = Math.max(s[i], lo + 4);
        return s;
    }

    void layout(int bx0, int by0, int bx1, int by1, int in) {
        Random r = new Random(city.cfg.seed * 9173L + 21);
        vert = r.nextBoolean();
        swap = r.nextBoolean();
        a0 = vert ? by0 : bx0;
        a1 = vert ? by1 : bx1;
        c0 = vert ? bx0 : by0;
        c1 = vert ? bx1 : by1;
        int span = c1 - c0, len = a1 - a0;
        bc = c0 + Math.round(span * (0.42f + 0.16f * r.nextFloat()));
        cd = AirbaseMap.clamp(Math.round(span * 0.05f), 12, 28);
        splitDistricts(r);
        // The frontier road along the fence on each side, the customs zone between it and the fence.
        int fa0 = bc - cd - 5, fb0 = bc + FENCE_W + cd;
        road(a0 + in, fa0, a1 - in, fa0 + 5, true, true);
        road(a0 + in, fb0, a1 - in, fb0 + 5, true, true);
        // The crossings: two, or three on the bigger maps, each an avenue right across the town.
        int n = len >= 300 ? 3 : 2;
        int lc = AirbaseMap.clamp(Math.round(cd * 1.6f), 18, 40);
        for (int k = 0; k < n; k++) {
            int at = a0 + Math.round(len * (k + 0.5f) / n + (r.nextFloat() - 0.5f) * len * 0.12f / n) - 2;
            at = Math.max(a0 + in + lc + 8, Math.min(a1 - in - lc - 13, at));
            crossings.add(at);
            road(at, c0 + in, at + 5, c1 - in, false, true);
        }
        // The fence: two lines with the cleared strip between, right across the map; the ring road cut where it meets it.
        for (int a = 0; a < (vert ? city.h : city.w); a++)
            for (int c = bc; c < bc + FENCE_W; c++) {
                int i = tile(a, c);
                if (city.tiles[i] == ROAD && inCrossing(a)) continue;
                city.tiles[i] = c == bc || c == bc + FENCE_W - 1 ? FENCE : SAND;
                city.roadDir[i] = 0;
                city.mainRoad[i] = false;
            }
        // The town either side: the strips between the crossings, out from the frontier roads and in the customs zones.
        int prev = a0 + in;
        for (int k = 0; k <= n; k++) {
            int next = k < n ? crossings.get(k) : a1 - in;
            if (swap) gridRegion(prev, c0 + in, next, fa0, r, false);
            else splitAC(prev, c0 + in, next, fa0);
            if (!swap) gridRegion(prev, fb0 + 5, next, c1 - in, r, true);
            else splitAC(prev, fb0 + 5, next, c1 - in);
            // The customs zones: a yard each side just before each crossing, the patrol station just after the first.
            for (int side = 0; side < 2; side++) {
                int zc0 = side == 0 ? bc - cd : bc + FENCE_W, zc1 = zc0 + cd;
                int z0 = prev, z1 = next;
                if (k < n) {
                    int[] y = rect(next - lc, zc0, next, zc1);
                    city.blocks.add(y);
                    yards.add(y);
                    z1 = next - lc;
                }
                if (k > 0 && k == 1) {
                    int[] p = rect(prev, zc0, prev + lc, zc1);
                    city.blocks.add(p);
                    patrols.add(p);
                    z0 = prev + lc;
                }
                if (z1 - z0 >= 6) splitAC(z0, zc0, z1, zc1);
            }
            if (k < n) prev = crossings.get(k) + 5;
        }
    }

    boolean inCrossing(int a) {
        for (int at : crossings) if (a >= at && a < at + 5) return true;
        return false;
    }

    /**
     * The districts cut in two along the border (each half its own district), each side's own kinds of district - a
     * grid of towers, offices and works on the near side, old quarters and homes on the far - and its own look.
     */
    private void splitDistricts(Random r) {
        List<District> ds = city.districts;
        int n = ds.size();
        int[] twin = new int[n];
        for (int k = 0; k < n; k++) twin[k] = -1;
        int W = city.w, H = city.h;
        // A twin for every district with ground on both sides.
        boolean[][] has = new boolean[n][2];
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                int d = city.districtAt.get(y * W + x);
                if (d >= 0 && d < n) has[d][far(vert ? x : y) ? 1 : 0] = true;
            }
        List<String> words = new ArrayList<String>(java.util.Arrays.asList(city.country.districtWords));
        java.util.Collections.shuffle(words, r);
        int word = 0;
        for (int k = 0; k < n && ds.size() < 120; k++) {
            if (!has[k][0] || !has[k][1]) continue;
            District t = new District();
            t.type = ds.get(k).type;
            t.name = ds.get(k).name;
            twin[k] = ds.size();
            ds.add(t);
        }
        int m = ds.size();
        farSide = new boolean[m];
        float[] sx = new float[m], sy = new float[m];
        for (District d : ds) d.tiles = 0;
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                int i = y * W + x, d = city.districtAt.get(i);
                if (d < 0 || d >= n) continue;
                boolean f = far(vert ? x : y);
                // (The twin takes the half on the side its district's middle isn't.)
                if (twin[d] >= 0 && f != far(Math.round(vert ? ds.get(d).x : ds.get(d).y))) {
                    d = twin[d];
                    city.districtAt.set(i, (byte) d);
                }
                farSide[d] = f;
                sx[d] += x;
                sy[d] += y;
                ds.get(d).tiles++;
            }
        for (int k = 0; k < m; k++) {
            District d = ds.get(k);
            if (d.tiles == 0) {
                d.unnamed = true;
                continue;
            }
            d.x = sx[k] / d.tiles;
            d.y = sy[k] / d.tiles;
            d.cx = (d.x + 0.5f) * T;
            d.cy = (d.y + 0.5f) * T;
        }
        // Each side its own kinds, names and look.
        java.util.HashSet<String> names = new java.util.HashSet<String>();
        for (int k = 0; k < m; k++) {
            District d = ds.get(k);
            int was = d.type;
            if (farSide[k]) {
                if (d.type == DT_DOWNTOWN || d.type == DT_MIDTOWN) d.type = r.nextFloat() < 0.75f ? DT_OLDTOWN : DT_MIDTOWN;
                else if (d.type == DT_CAMPUS) d.type = DT_SUBURB;
                d.theme = FAR;
            } else {
                if (d.type == DT_OLDTOWN) d.type = DT_MIDTOWN;
                d.theme = NEAR;
            }
            String wd = words.get(word++ % words.size());
            if (d.type != was || names.contains(d.name)) d.name = name(d.type, wd, farSide[k], r);
            if (names.contains(d.name)) d.name = wd + (farSide[k] ? " Quarter" : " District");
            names.add(d.name);
        }
    }

    private static String name(int type, String wd, boolean far, Random r) {
        String[] o;
        switch (type) {
            case DT_OLDTOWN: o = far ? new String[]{wd + " Quarter", "Old " + wd, wd + " Plaza", "The Old Market"} : new String[]{wd + " Square"}; break;
            case DT_MIDTOWN: o = far ? new String[]{wd + " Plaza", wd + " Arcades"} : new String[]{wd + " Square", wd + " Center", "Midtown"}; break;
            case DT_SUBURB: o = far ? new String[]{wd + " Hills", "Villa " + wd, wd + " Gardens"} : new String[]{wd + " Heights", wd + " Park"}; break;
            case DT_INDUSTRIAL: o = new String[]{wd + " Freight Yards", wd + " Works", wd + " Depots"}; break;
            case DT_DOWNTOWN: o = new String[]{"Downtown", wd + " Center"}; break;
            default: o = new String[]{wd + " Green", wd + " Fields"}; break;
        }
        return o[r.nextInt(o.length)];
    }

    /** The near side's look: red and brown brick under slate, grey paving. */
    static final Neighbourhoods.Theme NEAR = new Neighbourhoods.Theme("Brick Side", 0x7F, false,
            new int[]{0xFF4A525C, 0xFF3E4650, 0xFF5A5F66, 0xFF46413C},
            new int[]{0xFF9A4A38, 0xFFA8543A, 0xFF8E4434, 0xFF7C4A3A, 0xFFB86A48},
            new int[]{0xFF3E4650, 0xFF4A525C}, new int[]{0xFFA8543A, 0xFF8E4434, 0xFF5E6E7E},
            new int[]{0xFF8E4434, 0xFF9A6A50, 0xFF6E7884, 0xFF7C8894}, 0xFF8E8C88, 0);
    /** The far side's: whitewash, ochre and pastel stucco under terracotta, warm sandy paving, flags over its streets. */
    static final Neighbourhoods.Theme FAR = new Neighbourhoods.Theme("Stucco Side", 0x7F, false,
            new int[]{0xFFB4532E, 0xFFC2643A, 0xFFA84A2A, 0xFFC8743E},
            new int[]{0xFFEDE6D6, 0xFFE8D2A0, 0xFFE6B87A, 0xFFD8E0C8, 0xFFF0C8B8, 0xFFC8D8E0},
            new int[]{0xFFB4532E, 0xFFC2643A}, new int[]{0xFFEDE6D6, 0xFFE6B87A, 0xFF4A9AA8, 0xFFD86A4A},
            new int[]{0xFFEDE6D6, 0xFFE8D2A0, 0xFFE6B87A}, 0xFFC8AE84, 3);

    /**
     * The customs yards, the border patrol stations, the checkpoints at the crossings (one either side of the strip),
     * and the watchtowers along the strip.
     */
    void places(boolean[] used) {
        if (!city.cfg.borderTown() || bc < 0) return;
        if (gen.lmRnd == null) {
            gen.lmRnd = new Random(city.cfg.seed * 7919L + 35);
            gen.lmCount = new int[Landmarks.all().size()];
        }
        Landmarks.Def yard = CityGenerator.landmarkNamed("Customs Yard");
        for (int k = 0; k < city.blocks.size(); k++) {
            int[] b = city.blocks.get(k);
            boolean isYard = false, isPatrol = false;
            for (int[] y : yards) if (y == b) isYard = true;
            for (int[] p : patrols) if (p == b) isPatrol = true;
            if (!isYard && !isPatrol) continue;
            used[k] = true;
            city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], SIDEWALK);
            int ix = b[0] + 1, iy = b[1] + 1, iw = b[2] - b[0] - 2, ih = b[3] - b[1] - 2;
            if (isYard && yard != null && Math.min(iw, ih) >= yard.minS && Math.max(iw, ih) >= yard.minL) {
                lotGen.placeLandmark(yard, ix, iy, iw, ih);
            } else if (isPatrol) {
                lotGen.serviceBuilding(ix, iy, iw, ih, STATION, 90 + patrols.indexOf(b));
                Facility f = city.facilities.get(city.facilities.size() - 1);
                boolean f2 = (vert ? (b[0] + b[2]) / 2 : (b[1] + b[3]) / 2) > bc;
                f.name = "Border Patrol (" + (vert ? (f2 ? "East" : "West") : (f2 ? "South" : "North")) + ")";
            }
        }
        // The far side's squares: paved, a statue, trees round the edge, a gathering place.
        for (int k = 0; k < city.blocks.size(); k++) {
            int[] b = city.blocks.get(k);
            if (used[k] || !plazas.contains(b)) continue;
            used[k] = true;
            city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], SIDEWALK);
            lotGen.plaza(b[0] + 1, b[1] + 1, b[2] - b[0] - 2, b[3] - b[1] - 2);
            city.openAreas.add(new float[]{(b[0] + b[2]) / 2f * T, (b[1] + b[3]) / 2f * T, 1});
        }
        // A checkpoint on each side of the strip at every crossing.
        for (int at : crossings)
            for (int side = 0; side < 2; side++) {
                int c = side == 0 ? bc - 3 : bc + FENCE_W + 2;
                float px = vert ? (c + 0.5f) * T : (at + 2.5f) * T, py = vert ? (at + 2.5f) * T : (c + 0.5f) * T;
                float ang = (float) (vert ? (side == 0 ? 0 : Math.PI) : (side == 0 ? Math.PI / 2 : -Math.PI / 2));
                city.checkpoints.add(new float[]{px, py, ang});
                for (int sd = -1; sd <= 1; sd += 2) {
                    float[] q = city.findWalkable(px + (vert ? 0 : sd * 3.2f * T), py + (vert ? sd * 3.2f * T : 0));
                    if (q != null) city.checkpointPosts.add(q);
                }
            }
        // Watchtowers in the strip, every so often.
        int len = vert ? city.h : city.w;
        for (int a = 20; a + 2 < len - 6; a += 64) {
            boolean clear = true;
            for (int at : crossings) if (a + 2 > at - 8 && a < at + 13) clear = false;
            if (!clear) continue;
            int[] q = rect(a, bc + 2, a + 2, bc + 4);
            lotGen.addFacilityLot(q[0], q[1], 2, 2, TOWER, 3);
            city.lotNames.put(city.buildingLots.get(city.buildingLots.size() - 1), "Border Watchtower");
        }
    }
}
