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
 * (27.3) Theme Park Island: the resort island, the park and its rides, the hotels, the ferry and the staff town on
 * the mainland. Taken out of City.java as it stood; the city itself (its tiles, buildings and everything the game
 * asks of it) is `city`.
 */
final class ThemeParkMap extends CityPart {
    ThemeParkMap(City city) {
        super(city);
    }

    int mainShore;

    // ------------------------------------------------------------------ ((25.0) Theme Park Island)

    /**
     * Where Theme Park Island's two places are: the island itself (islandRects[0]), most of the top of the map, and
     * across the strait below it the staff town on the mainland (islandRects[1]), which runs along the bottom of the map.
     */
    void themeParkLayout(Random r, int j) {
        int mainH = Math.max(32, Math.round(city.h * 0.15f));
        mainShore = city.h - mainH;
        int strait = Math.max(22, Math.round(city.h * 0.075f));
        int mx = Math.max(8, Math.round(city.w * 0.06f)), my = Math.max(8, Math.round(city.h * 0.045f));
        int ix0 = mx + r.nextInt(j), iy0 = my + r.nextInt(j), ix1 = city.w - mx - r.nextInt(j), iy1 = mainShore - strait;
        int sw = Math.max(40, Math.round(city.w * 0.24f)), scx = city.w / 2 + Math.round((r.nextFloat() - 0.5f) * city.w * 0.2f);
        int sy0 = mainShore + Math.max(7, Math.round(city.h * 0.03f)), sy1 = city.h - Math.max(3, Math.round(city.h * 0.03f));
        city.islandRects = new int[][]{{ix0, iy0, ix1, iy1}, {scx - sw / 2, sy0, scx + sw / 2, sy1}};
        gen.islandLinks = new int[][]{{0, 1, 1}};
        gen.causewayIsland = 1;
        String town = city.country.townName(new Random(city.cfg.seed * 131L + 17));
        if (town.equals(city.name)) town = city.country.townName(new Random(city.cfg.seed * 977L + 1));
        city.islandNames = new String[]{city.name + " Island", town};
    }

    /**
     * Theme Park Island: the island's ring of streets, the park in the middle inside a ring of its own (left as open
     * plaza for its rides: see parkRides), the town's grid in the four pieces round it, a rounded outline with a wide
     * beach all round; across the strait the mainland's shore and woods along the bottom of the map, the staff town on
     * the coast road there, the one bridge across, a road off the map, and the ferry's two piers.
     */
    void themeParkIsland() {
        city.fill(0, 0, city.w, city.h, WATER);
        city.bridge = new boolean[city.w * city.h];
        Random sr = new Random(city.cfg.seed * 59L + 25);
        int[] q = city.islandRects[0], s = city.islandRects[1];
        int x0 = q[0], y0 = q[1], x1 = q[2], y1 = q[3], iw = x1 - x0, ih = y1 - y0;
        city.fill(x0 - 1, y0 - 1, iw + 2, ih + 2, SIDEWALK);
        gen.carve(new Street(x0, y0, x1, y0 + 3, false, false));
        gen.carve(new Street(x0, y1 - 3, x1, y1, false, false));
        gen.carve(new Street(x0, y0, x0 + 3, y1, true, false));
        gen.carve(new Street(x1 - 3, y0, x1, y1, true, false));
        int pw = Math.min(iw - 30, Math.max(30, Math.min(Math.round(iw * (city.w < 300 ? 0.3f : 0.48f)), 190 + iw / 8)));
        int ph = Math.min(ih - 22, Math.max(28, Math.min(Math.round(ih * 0.52f), 160 + ih / 8)));
        int px0 = x0 + (iw - pw) / 2 + Math.round((sr.nextFloat() - 0.5f) * (iw - pw) * 0.3f);
        int py0 = y0 + (ih - ph) / 2 + Math.round((sr.nextFloat() - 0.5f) * (ih - ph) * 0.3f);
        int px1 = px0 + pw, py1 = py0 + ph;
        // (The park's long sides are avenues right across the island.)
        gen.carve(new Street(x0, py0, x1, py0 + 3, false, true));
        gen.carve(new Street(x0, py1 - 3, x1, py1, false, true));
        gen.carve(new Street(px0, py0, px0 + 3, py1, true, true));
        gen.carve(new Street(px1 - 3, py0, px1, py1, true, true));
        city.parkRect = new int[]{px0 + 3, py0 + 3, px1 - 3, py1 - 3};
        city.fill(px0 + 3, py0 + 3, pw - 6, ph - 6, PLAZA);
        gen.split(x0 + 3, y0 + 3, x1 - 3, py0, 0, -1, -1);
        gen.split(x0 + 3, py1, x1 - 3, y1 - 3, 0, -1, -1);
        gen.split(x0 + 3, py0 + 3, px0, py1 - 3, 0, -1, -1);
        gen.split(px1, py0 + 3, x1 - 3, py1 - 3, 0, -1, -1);
        // The staff town's grid.
        int sx0 = s[0], sy0 = s[1], sx1 = s[2], sy1 = s[3];
        city.fill(sx0 - 1, sy0 - 1, sx1 - sx0 + 2, sy1 - sy0 + 2, SIDEWALK);
        gen.carve(new Street(sx0, sy0, sx1, sy0 + 3, false, true));
        gen.carve(new Street(sx0, sy1 - 3, sx1, sy1, false, false));
        gen.carve(new Street(sx0, sy0, sx0 + 3, sy1, true, false));
        gen.carve(new Street(sx1 - 3, sy0, sx1, sy1, true, false));
        gen.split(sx0 + 3, sy0 + 3, sx1 - 3, sy1 - 3, 0, -1, -1);
        // The island's outline: a rounded shape, gently lobed (the park well inside it); the staff town keeps its all.
        float[] amp = new float[4], ph0 = new float[4];
        for (int k = 0; k < 4; k++) {
            amp[k] = 0.03f + sr.nextFloat() * 0.05f;
            ph0[k] = sr.nextFloat() * (float) Math.PI * 2;
        }
        List<int[]> kept = new ArrayList<int[]>();
        for (int[] b : city.blocks) {
            float bx = (b[0] + b[2]) / 2f, by = (b[1] + b[3]) / 2f;
            int il = city.island((int) bx, (int) by);
            if (il == 1) kept.add(b);
            if (il != 0) continue;
            float hx = iw / 2f, hy = ih / 2f;
            float nx = (bx - (x0 + x1) / 2f) / hx, ny = (by - (y0 + y1) / 2f) / hy;
            float a = (float) Math.atan2(ny, nx);
            float d = (float) Math.pow(Math.pow(Math.abs(nx), 2.6) + Math.pow(Math.abs(ny), 2.6), 1 / 2.6);
            float r = 0.94f;
            for (int k = 0; k < 4; k++) r += amp[k] * (float) Math.sin((k + 2) * a + ph0[k]);
            if (d < r) kept.add(b);
        }
        boolean[] town = new boolean[city.w * city.h];
        for (int[] b : kept)
            for (int y = Math.max(0, b[1] - 3); y < Math.min(city.h, b[3] + 3); y++)
                for (int x = Math.max(0, b[0] - 3); x < Math.min(city.w, b[2] + 3); x++) {
                    int t = y * city.w + x;
                    boolean inside = x >= b[0] && x < b[2] && y >= b[1] && y < b[3];
                    if (inside || city.tiles[t] == ROAD) town[t] = true;
                }
        for (int y = py0; y < py1; y++) for (int x = px0; x < px1; x++) town[y * city.w + x] = true;
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
        int[] biggest = new int[3];
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
        // The island's coast: a little grass and palms behind a wide beach of sand most of the way round.
        int[] dist = gen.chamfer(0);
        double p0 = sr.nextDouble() * 10, ck = Math.max(0.35, Math.min(1, city.w / 600.0));
        for (int t = 0; t < city.w * city.h; t++) {
            if (dist[t] <= 0 || dist[t] > 230) continue;
            int x = t % city.w, y = t / city.w;
            if (y > mainShore - 10) continue;
            double coast = 12 + 5 * Math.sin(x * 0.043 + y * 0.021 + p0) + 3.5 * Math.sin(x * 0.019 - y * 0.051 + p0 * 2)
                    + 2.2 * Math.sin(x * 0.17 + y * 0.09 + p0 * 4) + 0.7 * Math.sin((x + y) * 0.37 + p0 * 5);
            coast = Math.max(7, Math.min(22, coast)) * ck;
            double beach = Math.max(3, 5.5 + 2.5 * Math.sin(x * 0.04 + y * 0.065 + p0 * 1.7)) * Math.max(0.5, ck);
            double d = dist[t] / 10.0;
            if (d > coast) continue;
            city.tiles[t] = d > coast - beach ? SAND : sr.nextFloat() < (d > 3 ? 0.2f : 0.06f) && d > 1.2 ? TREE : GRASS;
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
        // The mainland: a beach along its shore, then grass and woods down to the edge of the map.
        double m0 = sr.nextDouble() * 10;
        for (int x = 0; x < city.w; x++) {
            int shore = mainShore + (int) Math.round(2.2 * Math.sin(x * 0.045 + m0) + 1.4 * Math.sin(x * 0.11 + m0 * 2));
            int beach = 2 + (int) Math.round(1.5 + 1.5 * Math.sin(x * 0.07 + m0 * 3));
            for (int y = Math.max(0, shore); y < city.h; y++) {
                int t = y * city.w + x;
                if (city.tiles[t] != WATER) continue;
                double wood = Math.sin(x * 0.06 + m0) * Math.sin(y * 0.05 - m0) + 0.4 * Math.sin((x + y) * 0.13);
                city.tiles[t] = y < shore + beach ? SAND : y < shore + beach + 1 ? GRASS : sr.nextFloat() < (wood > 0.25 ? 0.55f : 0.07f) ? TREE : GRASS;
            }
        }
        // The coast road along the mainland, through the staff town, off the map both ways.
        gen.carve(new Street(0, sy0, sx0, sy0 + 3, false, true));
        gen.carve(new Street(sx1, sy0, city.w, sy0 + 3, false, true));
        // The bridge across the strait, and the road off the map from the staff town.
        int[] a = city.islandRects[0], b = city.islandRects[1];
        int lo = Math.max(a[0], b[0]), hi = Math.min(a[2], b[2]);
        int bx = gen.mainLine(true, lo, hi, (lo + hi) / 2 - 1, a, b);
        gen.linkRoad(true, bx, (a[3] + mainShore) / 2, true);
        int cx = gen.mainLine(true, b[0], b[2], (b[0] + b[2]) / 2 - 1, b, null);
        if (Math.abs(cx - bx) < 6) cx = bx;
        gen.linkRoad(true, cx, city.h - 1, false);
        city.causewayEnd = new float[]{(cx + 1.5f) * T, (city.h - 1.5f) * T};
        islandsMap.dropStrandedLand();
        themeParkFerry(bx, sr);
        // People go down to the beaches.
        int mx = (x0 + x1) / 2, my = (y0 + y1) / 2;
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
        // Boats out at sea.
        for (int k = 0; k < city.w / 20; k++) {
            int ox = 4 + city.rnd.nextInt(city.w - 40), oy = 4 + city.rnd.nextInt(city.h - 8);
            if (city.tiles[oy * city.w + ox] != WATER || city.tiles[oy * city.w + ox + 2] != WATER || city.bridge[oy * city.w + ox]) continue;
            city.addDecor(D_BOAT, ox * T, oy * T, ox * T + 30, oy * T + 10, city.rnd.nextInt(3));
        }
    }

    /**
     * The ferry across the strait: a pier out from the island's south shore and one from the mainland's, straight across
     * from each other well along from the bridge, a path from each up to the road, and the two landings (city.ferry).
     */
    void themeParkFerry(int bridgeX, Random sr) {
        int[] q = city.islandRects[0];
        int best = -1, bestTop = 0, bestBot = 0;
        float bs = -Float.MAX_VALUE;
        for (int fx = q[0] + 8; fx < q[2] - 8; fx++) {
            if (Math.abs(fx - bridgeX) < Math.max(24, city.w / 9)) continue;
            int top = -1, bot = -1;
            for (int y = (q[1] + q[3]) / 2; y < mainShore + 10; y++)
                if (city.tiles[y * city.w + fx] == WATER) {
                    top = y;
                    break;
                }
            if (top < 0) continue;
            for (int y = Math.min(city.h - 1, mainShore + 10); y > top; y--)
                if (city.tiles[y * city.w + fx] == WATER) {
                    bot = y;
                    break;
                }
            if (bot - top < 16) continue;
            boolean clear = true;
            for (int y = top; y <= bot && clear; y++)
                for (int x = fx - 3; x <= fx + 4 && clear; x++)
                    if (city.tiles[y * city.w + x] != WATER || city.bridge[y * city.w + x]) clear = false;
            if (!clear) continue;
            // (Land under both ends: the island's shore, and the mainland's.)
            if (city.tiles[(top - 1) * city.w + fx + 1] == WATER || city.tiles[(bot + 1) * city.w + fx + 1] == WATER) continue;
            float score = -(bot - top) * 0.5f - Math.abs(Math.abs(fx - bridgeX) - city.w * 0.22f) * 0.3f;
            if (score > bs) {
                bs = score;
                best = fx;
                bestTop = top;
                bestBot = bot;
            }
        }
        if (best < 0) return;
        int fx = best, len = Math.max(4, Math.min(7, (bestBot - bestTop) / 5));
        for (int k = 0; k < len; k++) {
            city.tiles[(bestTop + k) * city.w + fx] = PIER;
            city.tiles[(bestTop + k) * city.w + fx + 1] = PIER;
            city.tiles[(bestBot - k) * city.w + fx] = PIER;
            city.tiles[(bestBot - k) * city.w + fx + 1] = PIER;
        }
        city.ferry = new float[]{(fx + 1) * T, (bestTop + len + 1.5f) * T, (fx + 1) * T, (bestBot - len - 0.5f) * T};
        // A paved path from each pier to the nearest road.
        for (int dir = -1; dir <= 1; dir += 2) {
            for (int y = dir < 0 ? bestTop - 1 : bestBot + 1; y > 0 && y < city.h - 1; y += dir) {
                byte t0 = city.tiles[y * city.w + fx], t1 = city.tiles[y * city.w + fx + 1];
                if (t0 == ROAD || t1 == ROAD || t0 == BUILDING || t1 == BUILDING || t0 == WATER || t0 == SIDEWALK) break;
                for (int x = fx - 1; x <= fx + 2; x++) {
                    byte t = city.tiles[y * city.w + x];
                    if (t == GRASS || t == TREE || t == SAND) {
                        city.tiles[y * city.w + x] = x == fx - 1 || x == fx + 2 ? PLAZA : SIDEWALK;
                        city.unpine(y * city.w + x);
                    }
                }
            }
        }
        city.natureLabel("Ferry", (fx + 1) * T, (bestTop + len + 3) * T, NL_LANDMARK);
    }

    /** Theme Park Island's own places: the park's rides, the resort hotels along the beach, the staff town's clinic and housing. */
    void themeParkPlaces(boolean[] used) {
        if (gen.lmRnd == null) {
            gen.lmRnd = new Random(city.cfg.seed * 7919L + 35);
            gen.lmCount = new int[Landmarks.all().size()];
        }
        if (city.parkRect != null) parkRides();
        // The staff town: the clinic in the middle of it, the staff housing round it.
        int[] s = city.islandRects[1];
        float scx = (s[0] + s[2]) / 2f, scy = (s[1] + s[3]) / 2f;
        List<Integer> staff = new ArrayList<Integer>();
        for (int k = 0; k < city.blocks.size(); k++) {
            int[] b = city.blocks.get(k);
            if (!used[k] && b.length <= 4 && city.island((b[0] + b[2]) / 2, (b[1] + b[3]) / 2) == 1) staff.add(k);
        }
        int placed = 0;
        while (!staff.isEmpty() && placed < 3) {
            int bi = 0;
            float bd = Float.MAX_VALUE;
            for (int i = 0; i < staff.size(); i++) {
                int[] b = city.blocks.get(staff.get(i));
                float d = (float) Math.hypot((b[0] + b[2]) / 2f - scx, (b[1] + b[3]) / 2f - scy);
                if (d < bd) {
                    bd = d;
                    bi = i;
                }
            }
            int k = staff.remove(bi);
            if (placeOn(k, used, placed == 0 ? "Staff Clinic" : "Staff Housing")) placed++;
        }
        // The resort along the beach: hotels, villas and pool clubs on the blocks by the sand.
        int next = 0;
        for (int k = 0; k < city.blocks.size(); k++) {
            int[] b = city.blocks.get(k);
            if (used[k] || b.length > 4 || city.island((b[0] + b[2]) / 2, (b[1] + b[3]) / 2) != 0) continue;
            boolean shore = false;
            for (int y = Math.max(0, b[1] - 8); y < Math.min(city.h, b[3] + 8) && !shore; y++)
                for (int x = Math.max(0, b[0] - 8); x < Math.min(city.w, b[2] + 8) && !shore; x++)
                    if (city.tiles[y * city.w + x] == SAND) shore = true;
            if (!shore) continue;
            // (On the small maps, villas only: the big hotels would leave no room for anyone to live.)
            for (int t = 0; t < RESORT.length; t++)
                if (placeOn(k, used, city.w < 300 ? "Beach Villas" : RESORT[(next + t) % RESORT.length])) {
                    next = (next + t + 1) % RESORT.length;
                    break;
                }
        }
    }

    /** Puts landmark design name on block k (if it fits), marking it used. */
    boolean placeOn(int k, boolean[] used, String name) {
        int[] b = city.blocks.get(k);
        int iw = b[2] - b[0] - 2, ih = b[3] - b[1] - 2;
        Landmarks.Def d = CityGenerator.landmarkNamed(name);
        if (d == null || Math.min(iw, ih) < d.minS || Math.max(iw, ih) < d.minL) return false;
        used[k] = true;
        city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], SIDEWALK);
        lotGen.placeLandmark(d, b[0] + 1, b[1] + 1, iw, ih);
        return true;
    }

    /**
     * The theme park: its plaza cut into parcels by walks three tiles wide, the big roller coaster across one corner,
     * the castle at the heart of it, the Ferris wheel by it, the gate on the side facing the bridge, and a ride on every
     * other parcel.
     */
    void parkRides() {
        int x0 = city.parkRect[0], y0 = city.parkRect[1], x1 = city.parkRect[2], y1 = city.parkRect[3], pw = x1 - x0, ph = y1 - y0;
        Random r = new Random(city.cfg.seed * 1709L + 43);
        int cw = Math.max(20, Math.min(96, Math.round(pw * 0.4f))), ch = Math.max(18, Math.min(72, Math.round(ph * 0.5f)));
        if (pw - cw < 12) cw = pw;
        if (ph - ch < 12) ch = ph;
        // (On the smallest maps no big coaster: rides and halls all over.)
        if (city.w < 200) cw = ch = 0;
        boolean left = r.nextBoolean();
        int cx0 = left ? x0 : x1 - cw;
        int[] coaster = {cx0, y0, cx0 + cw, y0 + ch};
        List<int[]> parcels = new ArrayList<int[]>();
        int m = Math.max(16, Math.min(34, Math.round(Math.min(pw, ph) * 0.22f)));
        if (cw == 0) parkSplit(x0, y0, x1, y1, m, r, parcels);
        if (cw > 0 && cw < pw) parkSplit(left ? x0 + cw + 3 : x0, y0, left ? x1 : x1 - cw - 3, y0 + ch, m, r, parcels);
        if (cw > 0 && ch < ph) parkSplit(x0, y0 + ch + 3, x1, y1, m, r, parcels);
        if (cw > 0) gen.placeParcel(coaster, "Thunder Mountain Coaster");
        // The heart: the parcel nearest the middle of the park, then the Ferris wheel's (the nearest that's big enough).
        float mx = (x0 + x1) / 2f, my = (y0 + y1) / 2f;
        int gate = -1;
        float gd = Float.MAX_VALUE;
        for (int i = 0; i < parcels.size(); i++) {
            int[] pc = parcels.get(i);
            if (pc[3] != y1) continue;
            float d = Math.abs((pc[0] + pc[2]) / 2f - mx);
            if (d < gd) {
                gd = d;
                gate = i;
            }
        }
        boolean[] done = new boolean[parcels.size()];
        if (gate >= 0 && gen.placeParcel(parcels.get(gate), "Park Gate")) done[gate] = true;
        for (String star : new String[]{"Fairytale Castle", "Ferris Wheel"}) {
            int bi = -1;
            float bd = Float.MAX_VALUE;
            for (int i = 0; i < parcels.size(); i++) {
                int[] pc = parcels.get(i);
                if (done[i]) continue;
                Landmarks.Def d = CityGenerator.landmarkNamed(star);
                int s0 = Math.min(pc[2] - pc[0], pc[3] - pc[1]), l0 = Math.max(pc[2] - pc[0], pc[3] - pc[1]);
                if (d == null || s0 < d.minS || l0 < d.minL) continue;
                float dd = (float) Math.hypot((pc[0] + pc[2]) / 2f - mx, (pc[1] + pc[3]) / 2f - my) - 2.5f * s0;
                if (dd < bd) {
                    bd = dd;
                    bi = i;
                }
            }
            if (bi >= 0 && gen.placeParcel(parcels.get(bi), star)) done[bi] = true;
        }
        int next = r.nextInt(RIDES.length);
        for (int i = 0; i < parcels.size(); i++) {
            if (done[i]) continue;
            int[] pc = parcels.get(i);
            for (int t = 0; t < RIDES.length; t++)
                if (gen.placeParcel(pc, RIDES[(next + t) % RIDES.length])) {
                    next = (next + t + 1) % RIDES.length;
                    break;
                }
        }
        city.openAreas.add(new float[]{mx * T, my * T, 1});
    }

    /** Cuts a piece of the park into parcels no more than m tiles a side, with walks three tiles wide between. */
    void parkSplit(int x0, int y0, int x1, int y1, int m, Random r, List<int[]> out) {
        int bw = x1 - x0, bh = y1 - y0;
        if (bw < 6 || bh < 6) return;
        boolean canX = bw >= 21, canY = bh >= 21;
        if ((bw <= m && bh <= m) || (!canX && !canY)) {
            out.add(new int[]{x0, y0, x1, y1});
            return;
        }
        boolean cutX = canX && (!canY || bw >= bh);
        int len = cutX ? bw : bh, at = Math.round(len * (0.38f + r.nextFloat() * 0.24f));
        at = Math.max(9, Math.min(len - 12, at));
        if (cutX) {
            parkSplit(x0, y0, x0 + at, y1, m, r, out);
            parkSplit(x0 + at + 3, y0, x1, y1, m, r, out);
        } else {
            parkSplit(x0, y0, x1, y0 + at, m, r, out);
            parkSplit(x0, y0 + at + 3, x1, y1, m, r, out);
        }
    }

    /**
     * The tropics, once the city is built: palms instead of broadleaf trees near the island's beaches, and umbrellas
     * and lifeguard towers along the sand.
     */
    void tropics() {
        Random r = new Random(city.cfg.seed * 4111L + 43);
        int[] dist = new int[city.w * city.h];
        Arrays.fill(dist, 99);
        int[] q = new int[city.w * city.h];
        int qh = 0, qt = 0;
        for (int i = 0; i < city.w * city.h; i++)
            if (city.tiles[i] == SAND) {
                dist[i] = 0;
                q[qt++] = i;
            }
        while (qh < qt) {
            int i = q[qh++], x = i % city.w, y = i / city.w;
            if (dist[i] >= 14) continue;
            for (int k = 0; k < 4; k++) {
                int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = y + (k == 2 ? 1 : k == 3 ? -1 : 0);
                if (nx < 0 || ny < 0 || nx >= city.w || ny >= city.h) continue;
                int n = ny * city.w + nx;
                if (dist[n] <= dist[i] + 1) continue;
                dist[n] = dist[i] + 1;
                q[qt++] = n;
            }
        }
        for (int i = 0; i < city.w * city.h; i++) {
            if (city.tiles[i] != TREE) continue;
            int x = i % city.w, y = i / city.w;
            // ((26.1) The Archipelago: palms all over the outer islands, and along the big one's shore.)
            boolean palm = city.themePark ? city.island(x, y) == 0 || (y < mainShore && dist[i] < 14) || dist[i] < 4 : city.island(x, y) >= 1 || dist[i] < 10;
            if (palm) city.setTreeKind(i, Country.TK_PALM);
        }
        int shoreY = city.themePark ? mainShore : city.h;
        // Along the island's beaches: umbrellas on the sand near the water, a lifeguard tower now and then.
        int guard = 0;
        for (int y = 4; y < shoreY - 4; y++)
            for (int x = 4; x < city.w - 4; x++) {
                int i = y * city.w + x;
                if (city.tiles[i] != SAND || ((x * 5 + y * 3) % 11) != 0) continue;
                boolean wet = false, busy = false;
                for (int dy = -3; dy <= 3; dy++)
                    for (int dx = -3; dx <= 3; dx++) {
                        byte t = city.tiles[(y + dy) * city.w + x + dx];
                        if (t == WATER) wet = true;
                        if (t == ROAD || t == PIER || t == BUILDING || city.bridge[(y + dy) * city.w + x + dx]) busy = true;
                    }
                if (!wet || busy || r.nextFloat() < 0.35f) continue;
                if (++guard % 9 == 0) city.addDecor(D_LIFEGUARD, x * T + 3, y * T + 2, x * T + 13, y * T + 12, 0);
                else city.addDecor(D_UMBRELLA, x * T + 1, y * T + 1, x * T + 15, y * T + 15, r.nextInt(5));
            }
    }
}
