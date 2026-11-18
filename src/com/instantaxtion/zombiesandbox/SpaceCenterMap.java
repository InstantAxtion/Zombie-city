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
 * (27.3) The Space Center: the launch complex, the crawlerway, the assembly building and mission control, and the
 * staff town beside it. Taken out of City.java as it stood; the city itself (its tiles, buildings and everything the
 * game asks of it) is `city`.
 */
final class SpaceCenterMap extends CityPart {
    SpaceCenterMap(City city) {
        super(city);
    }

    int scSide, scFar = -1;
    final List<int[]> scParcels = new ArrayList<int[]>();
    final List<String> scNames = new ArrayList<String>();

    // ------------------------------------------------------------------ ((26.0) The Space Center)

    /**
     * Which side of the town the Space Center's space center takes (a little over half of it, the whole way along that
     * side), and what's left for the staff town.
     */
    void spaceCenterSplit(int bx0, int by0, int bx1, int by1) {
        Random sr = new Random(city.cfg.seed * 83L + 19);
        sr.nextInt();
        scSide = sr.nextInt(4);
        boolean lr = scSide < 2;
        int across = lr ? bx1 - bx0 : by1 - by0;
        int depth = Math.round(across * 0.52f);
        switch (scSide) {
            case 0:
                gen.scRect = new int[]{bx0, by0, bx0 + depth, by1};
                gen.scTown = new int[]{bx0 + depth, by0, bx1, by1};
                break;
            case 1:
                gen.scRect = new int[]{bx1 - depth, by0, bx1, by1};
                gen.scTown = new int[]{bx0, by0, bx1 - depth, by1};
                break;
            case 2:
                gen.scRect = new int[]{bx0, by0, bx1, by0 + depth};
                gen.scTown = new int[]{bx0, by0 + depth, bx1, by1};
                break;
            default:
                gen.scRect = new int[]{bx0, by1 - depth, bx1, by1};
                gen.scTown = new int[]{bx0, by0, bx1, by1 - depth};
                break;
        }
    }

    /** The space center's length along the staff town (a), and its depth out from it (c). */
    int scLA() {
        return scSide < 2 ? gen.scRect[3] - gen.scRect[1] : gen.scRect[2] - gen.scRect[0];
    }

    int scLC() {
        return scSide < 2 ? gen.scRect[2] - gen.scRect[0] : gen.scRect[3] - gen.scRect[1];
    }

    /** A rectangle in the space center's own frame (a along the staff town, c out from it) as tiles {x0, y0, x1, y1}. */
    int[] scr(int a0, int c0, int a1, int c1) {
        int[] q = gen.scRect;
        switch (scSide) {
            case 0: return new int[]{q[2] - c1, q[1] + a0, q[2] - c0, q[1] + a1};
            case 1: return new int[]{q[0] + c0, q[1] + a0, q[0] + c1, q[1] + a1};
            case 2: return new int[]{q[0] + a0, q[3] - c1, q[0] + a1, q[3] - c0};
            default: return new int[]{q[0] + a0, q[1] + c0, q[0] + a1, q[1] + c1};
        }
    }

    /** A street three tiles wide over a0..a1, c0..c1 of the space center's frame (running the long way). */
    void scRoad(int a0, int c0, int a1, int c1, boolean main) {
        if (a1 - a0 < 3 || c1 - c0 < 3 || a0 < 0 || c0 < 0 || a1 > scLA() || c1 > scLC()) return;
        int[] q = scr(a0, c0, a1, c1);
        boolean alongA = a1 - a0 >= c1 - c0;
        gen.carve(new Street(q[0], q[1], q[2], q[3], (scSide < 2) == alongA, main));
    }

    /** Lays aside a0..a1, c0..c1 for design name (ring 1: a block, its pavement round the design). */
    void scPlace(int a0, int c0, int a1, int c1, String name, int ring) {
        if (a1 - a0 < 4 || c1 - c0 < 4 || a0 < 0 || c0 < 0 || a1 > scLA() || c1 > scLC()) return;
        int[] q = scr(a0, c0, a1, c1);
        scParcels.add(new int[]{q[0], q[1], q[2], q[3], ring});
        scNames.add(name);
    }

    static int clampI(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /**
     * The Space Center's streets: the avenue between the space center and the staff town, the parkway through the
     * space center with the tech area's two rows of blocks between them, a loop out past the launch pads and one
     * along the landing strip; and where each of its places goes (put up in spaceCenterPlaces). The staff town's
     * streets on the rest of the town.
     */
    void spaceCenterTown() {
        // (Paved over until its places go up, so nothing out in the country takes the open ground meanwhile.)
        int[] q = gen.scRect;
        city.fill(q[0], q[1], q[2] - q[0], q[3] - q[1], SIDEWALK);
        int LA = scLA(), LC = scLC();
        float s = LC / 444f;
        int rowD = clampI(Math.round(34 * s), 16, 48);
        int cP = 6 + 2 * rowD, m0 = cP + 3;
        // The avenue along the staff town, the parkway, and the ends joining them.
        scRoad(0, 0, LA, 3, true);
        gen.split(gen.scTown[0], gen.scTown[1], gen.scTown[2], gen.scTown[3], 0, -1, -1);
        scRoad(0, cP, LA, cP + 3, true);
        scRoad(0, 0, 3, cP + 3, false);
        scRoad(LA - 3, 0, LA, cP + 3, false);
        // The tech area: two rows of blocks between the avenue and the parkway.
        int bl0 = clampI(Math.round(72 * s), 20, 96), nb = clampI((Math.round(LA * 0.6f) - 3) / (bl0 + 3), 1, 4);
        int aT0 = Math.round(LA * 0.42f) - (nb * (bl0 + 3) + 3) / 2, aT1 = aT0 + nb * (bl0 + 3) + 3;
        int bl = (aT1 - aT0 - 3 * (nb + 1)) / nb;
        String[][] tech = {{"Visitor Complex", "Space Center HQ", "Crew Quarters", "Astronaut Training"},
                {"Propulsion Lab", "Mission Control", "Payload Processing", "Tracking Station"}};
        if (bl >= 8) {
            for (int k = 0; k <= nb; k++) scRoad(aT0 + k * (bl + 3), 3, aT0 + k * (bl + 3) + 3, cP, false);
            scRoad(aT0, 3 + rowD, aT0 + nb * (bl + 3) + 3, 6 + rowD, false);
            for (int k = 0; k < nb; k++) {
                int a0 = aT0 + 3 + k * (bl + 3);
                // (Mission control in the middle of the row by the parkway, whatever the number of blocks.)
                int j = nb >= 3 ? k : k == 0 ? 1 : 0;
                scPlace(a0, 3, a0 + bl, 3 + rowD, tech[0][k], 1);
                scPlace(a0, 6 + rowD, a0 + bl, 6 + 2 * rowD, tech[1][j], 1);
            }
        }
        // Out past the parkway: the assembly building, the crawlerway out to the two pads, the fuel farm between them.
        int P = clampI(Math.round(62 * s), 18, 80), Pc = Math.round(P * 1.15f), cw = 8, st = clampI(Math.round(10 * s), 3, 14);
        int margin = clampI(Math.round(40 * s), 4, 60);
        int cF = LC - margin - 3, pc1 = cF - 4, pc0 = pc1 - Pc, al1 = pc0 - st, al0 = al1 - cw;
        int Va = clampI(Math.round(60 * s), 16, 80), Vc = clampI(Math.round(76 * s), 18, 96), g = clampI(Math.round(10 * s), 2, 14);
        int vc0 = m0 + g, vc1 = vc0 + Vc;
        int aA = Math.round(LA * 0.16f), aB = Math.round(LA * 0.40f), aV = Math.round(LA * 0.28f);
        int pa0 = aA - P / 2, pb0 = aB - P / 2;
        boolean full = al0 - vc1 >= 6 && pa0 >= 14 && pb0 - (pa0 + P) >= 20;
        if (!full) {
            // (On the small maps: the pad and the assembly building side by side out past the parkway.)
            int d = Math.min(LC - 2 - m0, 32);
            int pa = Math.max(4, Math.round(LA * 0.12f));
            scPlace(pa, m0 + 1, pa + Math.max(P, d), m0 + 1 + d, "Launch Pad A", 0);
            int va = pa + Math.max(P, d) + 6;
            scPlace(va, m0 + 1, va + Va, m0 + 1 + Math.min(d, Vc), "Vehicle Assembly Building", 0);
            return;
        }
        scPlace(aV - Va / 2, vc0, aV - Va / 2 + Va, vc1, "Vehicle Assembly Building", 0);
        scPlace(aV - cw / 2, vc1, aV - cw / 2 + cw, al0, "Crawlerway", 0);
        scPlace(aA - cw / 2, al0, aB - cw / 2 + cw, al1, "Crawlerway", 0);
        scPlace(aA - cw / 2, al1, aA - cw / 2 + cw, pc0, "Crawlerway", 0);
        scPlace(aB - cw / 2, al1, aB - cw / 2 + cw, pc0, "Crawlerway", 0);
        scPlace(pa0, pc0, pa0 + P, pc1, "Launch Pad A", 0);
        scPlace(pb0, pc0, pb0 + P, pc1, "Launch Pad B", 0);
        int fl = Math.min(pb0 - pa0 - P - 20, clampI(Math.round(80 * s), 24, 110)), fa = (pa0 + P + pb0) / 2 - fl / 2;
        scPlace(fa, pc0 + Pc / 6, fa + fl, pc1 - Pc / 6, "Fuel Farm", 0);
        scFar = cF;
        // The loop out past the pads.
        int ra = pa0 - 11, rb = pb0 + P + 8;
        if (ra >= 4) {
            scRoad(ra, cP + 3, ra + 3, cF, false);
            scRoad(rb, cP + 3, rb + 3, cF, false);
            scRoad(ra, cF, rb + 3, cF + 3, false);
        }
        // The landing strip, along the way past the pads, with its own loop off the parkway.
        int RW = clampI(Math.round(30 * s), 12, 36), cR = (m0 + cF) / 2 - RW / 2;
        int ra0 = Math.max(rb + 20, Math.round(LA * 0.54f)), ra1 = Math.round(LA * 0.96f);
        if (ra1 - ra0 >= 60 && cR - 4 >= m0 + 4) {
            scRoad(ra0, cR - 4, ra1, cR - 1, false);
            scRoad(ra0, cP + 3, ra0 + 3, cR - 4, false);
            scRoad(ra1 - 3, cP + 3, ra1, cR - 4, false);
            scPlace(ra0 + 4, cR, ra1 - 4, cR + RW, "Landing Strip", 0);
        }
    }

    /** Whether design name fits a pw by ph parcel. */
    static boolean fitsDesign(String name, int pw, int ph) {
        Landmarks.Def d = CityGenerator.landmarkNamed(name);
        return d != null && Math.min(pw, ph) >= d.minS && Math.max(pw, ph) >= d.minL;
    }

    /**
     * The Space Center's places, on the ground spaceCenterTown laid aside: each design where it goes; then the open
     * ground of the space center as Florida scrub, with ponds and marsh, palms and pines.
     */
    void spaceCenterPlaces() {
        if (gen.lmRnd == null) {
            gen.lmRnd = new Random(city.cfg.seed * 7919L + 35);
            gen.lmCount = new int[Landmarks.all().size()];
        }
        int[] q = gen.scRect;
        for (int y = q[1]; y < q[3]; y++)
            for (int x = q[0]; x < q[2]; x++) if (city.tiles[y * city.w + x] == SIDEWALK) city.tiles[y * city.w + x] = GRASS;
        boolean[] busy = new boolean[city.w * city.h];
        for (int i = 0; i < scParcels.size(); i++) {
            int[] b = scParcels.get(i);
            String name = scNames.get(i);
            int in = b[4];
            if (!fitsDesign(name, b[2] - b[0] - 2 * in, b[3] - b[1] - 2 * in)) continue;
            if (in > 0) city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], SIDEWALK);
            gen.placeParcel(new int[]{b[0] + in, b[1] + in, b[2] - in, b[3] - in}, name);
            for (int y = Math.max(0, b[1] - 3); y < Math.min(city.h, b[3] + 3); y++)
                for (int x = Math.max(0, b[0] - 3); x < Math.min(city.w, b[2] + 3); x++) busy[y * city.w + x] = true;
        }
        int LA = scLA(), LC = scLC();
        float s = LC / 444f;
        Random r = new Random(city.cfg.seed * 2027L + 61);
        // Ponds and marsh out in the scrub, clear of the roads and places.
        int ponds = Math.max(1, LA * LC / 9000);
        for (int k = 0; k < ponds; k++) {
            float cx = q[0] + 6 + r.nextFloat() * Math.max(1, q[2] - q[0] - 12), cy = q[1] + 6 + r.nextFloat() * Math.max(1, q[3] - q[1] - 12);
            float rx = 4 + r.nextFloat() * Math.max(2, 30 * s), ry = 4 + r.nextFloat() * Math.max(2, 20 * s);
            double ph = r.nextDouble() * 6.28;
            for (int y = (int) (cy - ry - 2); y <= (int) (cy + ry + 2); y++)
                for (int x = (int) (cx - rx - 2); x <= (int) (cx + rx + 2); x++) {
                    if (x < q[0] + 2 || y < q[1] + 2 || x >= q[2] - 2 || y >= q[3] - 2) continue;
                    float dx = (x + 0.5f - cx) / rx, dy = (y + 0.5f - cy) / ry;
                    double a = Math.atan2(dy, dx);
                    double rr = 1 - 0.18 * Math.sin(a * 3 + ph) - 0.1 * Math.sin(a * 5 + ph * 2);
                    if (dx * dx + dy * dy > rr * rr) continue;
                    if (scClear(x, y, 3, busy)) city.tiles[y * city.w + x] = WATER;
                }
        }
        // Out past the road beyond the pads, the lagoon along the far side, its shore wandering.
        if (scFar > 0) {
            double l0 = r.nextDouble() * 10;
            for (int a = 0; a < LA; a++) {
                int c0 = scFar + 8 + (int) Math.round(3 + 3 * Math.sin(a * 0.035 + l0) + 2 * Math.sin(a * 0.11 + l0 * 2));
                int c1 = LC - 4 - (int) Math.round(2 + 2 * Math.sin(a * 0.05 + l0 * 3));
                for (int cc = c0; cc < c1; cc++) {
                    int[] t = scr(a, cc, a + 1, cc + 1);
                    int x = t[0], y = t[1];
                    if (x < 0 || y < 0 || x >= city.w || y >= city.h || busy[y * city.w + x] || city.tiles[y * city.w + x] != GRASS) continue;
                    city.tiles[y * city.w + x] = WATER;
                }
            }
        }
        // The scrub: pines and palms, thicker in patches, thin by the roads.
        double p0 = r.nextDouble() * 10;
        for (int y = q[1]; y < q[3]; y++)
            for (int x = q[0]; x < q[2]; x++) {
                int i = y * city.w + x;
                if (city.tiles[i] != GRASS || busy[i] || !scClear(x, y, 1, busy)) continue;
                double wood = Math.sin(x * 0.07 + p0) * Math.sin(y * 0.06 - p0) + 0.5 * Math.sin((x - y) * 0.11 + p0 * 2);
                if (r.nextFloat() >= (wood > 0.45 ? 0.3f : 0.025f)) continue;
                city.tiles[i] = TREE;
                if (r.nextFloat() < 0.3f) city.setTreeKind(i, Country.TK_PALM);
            }
    }

    /** Whether the ground within d tiles of x, y is all open grass, trees or water (and none of it laid aside). */
    boolean scClear(int x, int y, int d, boolean[] busy) {
        for (int yy = y - d; yy <= y + d; yy++)
            for (int xx = x - d; xx <= x + d; xx++) {
                if (xx < 0 || yy < 0 || xx >= city.w || yy >= city.h) return false;
                int i = yy * city.w + xx;
                byte t = city.tiles[i];
                if (busy[i] || (t != GRASS && t != TREE && t != WATER)) return false;
            }
        return true;
    }
}
