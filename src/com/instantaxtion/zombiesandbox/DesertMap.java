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
 * (27.3) Desert City: the casino strip, the oases, the test range, and the sand everything else stands on. Taken out
 * of City.java as it stood; the city itself (its tiles, buildings and everything the game asks of it) is `city`.
 */
final class DesertMap extends CityPart {
    DesertMap(City city) {
        super(city);
    }

    /**
     * The casino strip: the blocks along the middle stretch of the first great street across town, each a casino,
     * a grand hotel, a nightclub, a wedding chapel and the like, or a big neon-lit building of its own.
     */
    void strip(boolean[] used) {
        Street st = gen.stripSt;
        boolean v = st.vertical;
        float mid = v ? (city.townY0 + city.townY1) / 2f : (city.townX0 + city.townX1) / 2f;
        float reach = (v ? city.townY1 - city.townY0 : city.townX1 - city.townX0) * 0.3f;
        if (gen.lmRnd == null) {
            gen.lmRnd = new Random(city.cfg.seed * 7919L + 35);
            gen.lmCount = new int[Landmarks.all().size()];
        }
        Random r = new Random(city.cfg.seed * 1301L + 39);
        int next = 0;
        for (int k = 0; k < city.blocks.size(); k++) {
            int[] b = city.blocks.get(k);
            if (used[k] || b.length > 4) continue;
            boolean touches = v ? (b[2] == st.x0 || b[0] == st.x1) && b[1] < st.y1 && b[3] > st.y0
                    : (b[3] == st.y0 || b[1] == st.y1) && b[0] < st.x1 && b[2] > st.x0;
            if (!touches) continue;
            float along = v ? (b[1] + b[3]) / 2f : (b[0] + b[2]) / 2f;
            if (Math.abs(along - mid) > reach) continue;
            int ix = b[0] + 1, iy = b[1] + 1, iw = b[2] - b[0] - 2, ih = b[3] - b[1] - 2;
            if (iw < 5 || ih < 5) continue;
            used[k] = true;
            city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], SIDEWALK);
            gen.curDistrict = city.districtType(ix + iw / 2, iy + ih / 2);
            int s = Math.min(iw, ih), l = Math.max(iw, ih);
            Landmarks.Def pick = null;
            for (int t = 0; t < STRIP.length && pick == null; t++) {
                Landmarks.Def d = CityGenerator.landmarkNamed(STRIP[(next + t) % STRIP.length]);
                if (d != null && s >= d.minS && l >= d.minL) {
                    pick = d;
                    next = (next + t + 1) % STRIP.length;
                }
            }
            if (pick != null) {
                lotGen.placeLandmark(pick, ix, iy, iw, ih);
                continue;
            }
            // Too small for any of them: a neon-lit block of its own on a forecourt.
            city.fill(ix, iy, iw, ih, PLAZA);
            int[] col = NEON[r.nextInt(NEON.length)];
            int bx = ix + 1, by = iy + 1, bw = iw - 2, bh = ih - 2;
            city.fill(bx, by, bw, bh, BUILDING);
            city.buildingLots.add(new int[]{bx, by, bw, bh, col[0], r.nextInt(100000), OFFICE, 3 + r.nextInt(5), col[1], -1});
        }
    }

    /**
     * The army's test range behind the desert base: open sand fenced off (x0, y0, rw by rh tiles), through a gap in
     * its fence opposite the base's top gate (columns gx to gx + 2), with targets, craters and wrecks drawn on it.
     */
    void testRange(int x0, int y0, int rw, int rh, int gx) {
        gen.testRange = new int[]{x0, y0, x0 + rw, y0 + rh};
        city.fill(x0, y0, rw, rh, SAND);
        for (int i = x0; i < x0 + rw; i++) {
            city.tiles[y0 * city.w + i] = FENCE;
            city.tiles[(y0 + rh - 1) * city.w + i] = i >= gx && i < gx + 3 ? SAND : FENCE;
        }
        for (int j = y0; j < y0 + rh; j++) {
            city.tiles[j * city.w + x0] = FENCE;
            city.tiles[j * city.w + x0 + rw - 1] = FENCE;
        }
        city.decor.add(new float[]{D_RANGE, (x0 + 1) * T, (y0 + 1) * T, (x0 + rw - 1) * T, (y0 + rh - 1) * T, 0, city.rnd.nextInt(1 << 20)});
    }

    /** A park in the desert: watered green, with palms, and a pond in it if it's big enough (an oasis in town). */
    void desertPark(int x, int y, int pw, int ph) {
        if (city.lush == null) city.lush = new boolean[city.w * city.h];
        for (int j = y; j < y + ph; j++) for (int i = x; i < x + pw; i++) city.lush[j * city.w + i] = true;
        if (pw >= 10 && ph >= 10) pond(x + pw * 0.32f, y + ph * 0.35f, Math.min(pw, ph) * 0.2f, city.rnd, x, y, x + pw, y + ph);
        for (int j = y; j < y + ph; j++)
            for (int i = x; i < x + pw; i++)
                if (city.tiles[j * city.w + i] == GRASS && city.rnd.nextFloat() < 0.12f && !city.hasNeighbor(i, j, TREE) && !city.hasNeighbor(i, j, WATER))
                    city.tiles[j * city.w + i] = TREE;
        // A path across, clear of the pond.
        int cy = y + ph * 3 / 4;
        for (int i = x; i < x + pw; i++) if (city.tiles[cy * city.w + i] == GRASS || city.tiles[cy * city.w + i] == TREE) city.tiles[cy * city.w + i] = PLAZA;
    }

    /**
     * A pond (centre and radius in tiles), on open sand or grass only and inside the rectangle given, its level set a
     * little below its lowest shore. Returns how many tiles it covers.
     */
    int pond(float cx, float cy, float rad, Random r, int bx0, int by0, int bx1, int by1) {
        float ph1 = r.nextFloat() * 6.3f, ph2 = r.nextFloat() * 6.3f;
        int reach = (int) (rad * 1.4f) + 1;
        List<Integer> cells = new ArrayList<Integer>();
        float level = Float.MAX_VALUE;
        for (int y = Math.max(by0, (int) cy - reach); y < Math.min(by1, (int) cy + reach + 1); y++)
            for (int x = Math.max(bx0, (int) cx - reach); x < Math.min(bx1, (int) cx + reach + 1); x++) {
                float dx = x + 0.5f - cx, dy = y + 0.5f - cy;
                double a = Math.atan2(dy, dx);
                float rr = rad * (1 + 0.18f * (float) Math.sin(3 * a + ph1) + 0.1f * (float) Math.sin(5 * a + ph2));
                if (dx * dx + dy * dy >= rr * rr) continue;
                int i = y * city.w + x;
                if (city.tiles[i] != GRASS && city.tiles[i] != TREE) continue;
                cells.add(i);
                if (city.elev != null) level = Math.min(level, city.elev.get(i));
            }
        for (int i : cells) {
            city.tiles[i] = WATER;
            city.unpine(i);
            if (city.elev != null) city.elev.set(i, level - 1);
        }
        return cells.size();
    }

    /**
     * Desert City's land, once the city is built: oases out on the open sand, green and palms round all its water, and
     * every tree a palm, a cactus or a bush of desert scrub (most of the country's woods are bare sand).
     */
    void desertify() {
        if (city.lush == null) city.lush = new boolean[city.w * city.h];
        Random r = new Random(city.cfg.seed * 4111L + 39);
        boolean outside = city.townX0 > 0;
        if (outside) {
            int want = city.w >= 600 ? 7 : city.w >= 300 ? 4 : 2;
            boolean named = false;
            for (int tries = 0; tries < 900 && want > 0; tries++) {
                int rad = 5 + r.nextInt(5), R = rad + 5;
                int cx = R + 2 + r.nextInt(Math.max(1, city.w - 2 * R - 4)), cy = R + 2 + r.nextInt(Math.max(1, city.h - 2 * R - 4));
                if (cx > city.townX0 - 10 && cx < city.townX1 + 10 && cy > city.townY0 - 10 && cy < city.townY1 + 10) continue;
                if (!gen.allOpen(cx - R, cy - R, 2 * R + 1, 2 * R + 1)) continue;
                if (pond(cx + 0.5f, cy + 0.5f, rad, r, 0, 0, city.w, city.h) < 8) continue;
                want--;
                if (!named) {
                    named = true;
                    String[] words = city.country.districtWords;
                    city.natureLabel(words[r.nextInt(words.length)] + " Oasis", (cx + 0.5f) * T, (cy + rad + 2.5f) * T, NL_LAKE);
                }
            }
        }
        // How far each tile is from the water (up to 4).
        int[] dist = new int[city.w * city.h];
        Arrays.fill(dist, 99);
        int[] q = new int[city.w * city.h];
        int qh = 0, qt = 0;
        for (int i = 0; i < city.w * city.h; i++)
            if (city.tiles[i] == WATER) {
                dist[i] = 0;
                q[qt++] = i;
            }
        while (qh < qt) {
            int i = q[qh++], x = i % city.w, y = i / city.w;
            if (dist[i] >= 4) continue;
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
            if (dist[i] < 1 || dist[i] > 4) continue;
            byte t = city.tiles[i];
            if (t != GRASS && t != TREE && t != TRAIL) continue;
            int x = i % city.w, y = i / city.w;
            if (dist[i] <= 3 || ((x * 7 + y * 3) & 3) == 0) city.lush[i] = true;
            if (t == GRASS && dist[i] <= 2 && r.nextFloat() < 0.3f && !city.hasNeighbor(x, y, TREE) && !city.hasNeighbor(x, y, ROAD)
                    && !city.hasNeighbor(x, y, BUILDING) && !city.hasNeighbor(x, y, DIRT)) city.tiles[i] = TREE;
        }
        // A cactus here and there out on the open sand.
        if (outside)
            for (int y = 2; y < city.h - 2; y++)
                for (int x = 2; x < city.w - 2; x++) {
                    int i = y * city.w + x;
                    if (city.tiles[i] != GRASS || city.lush[i] || (x >= city.townX0 && x < city.townX1 && y >= city.townY0 && y < city.townY1)) continue;
                    if (r.nextFloat() >= 0.006f || city.hasNeighbor(x, y, TREE) || city.hasNeighbor(x, y, ROAD) || city.hasNeighbor(x, y, DIRT)
                            || city.hasNeighbor(x, y, BUILDING) || city.hasNeighbor(x, y, FENCE) || city.hasNeighbor(x, y, LOT)) continue;
                    city.tiles[i] = TREE;
                    city.setTreeKind(i, Country.TK_CACTUS);
                }
        // Every tree a desert one; most of the woods out in the country just sand.
        for (int i = 0; i < city.w * city.h; i++) {
            if (city.tiles[i] != TREE) continue;
            int x = i % city.w, y = i / city.w;
            boolean inTown = x >= city.townX0 && x < city.townX1 && y >= city.townY0 && y < city.townY1;
            byte kind;
            if (city.lush[i]) kind = Country.TK_PALM;
            else if (city.treeKindAt(i) == Country.TK_CACTUS) kind = Country.TK_CACTUS;
            else if (inTown) {
                float f = r.nextFloat();
                kind = f < 0.5f ? Country.TK_PALM : f < 0.75f ? Country.TK_CACTUS : Country.TK_SCRUB;
            } else if (r.nextFloat() < 0.14f) kind = r.nextFloat() < 0.5f ? Country.TK_CACTUS : Country.TK_SCRUB;
            else {
                city.tiles[i] = GRASS;
                city.unpine(i);
                continue;
            }
            city.setTreeKind(i, kind);
        }
    }
}
