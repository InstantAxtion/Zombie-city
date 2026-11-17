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
 * (27.3) The prisons: the Prison Island out in the bay, and the walled prison on the edge of town on other maps.
 * Taken out of City.java as it stood; the city itself (its tiles, buildings and everything the game asks of it) is
 * `city`.
 */
final class PrisonMap extends CityPart {
    PrisonMap(City city) {
        super(city);
    }

    /**
     * (15.6) The island out in the bay, the prison on it: a high wall all round (rock-solid: nobody climbs it), a gate
     * facing the causeway that runs across the water to the seafront, cell blocks, an admin block and watchtowers in a
     * concrete yard, and a ferry landing on the island and one on the mainland.
     */
    void prisonIsland() {
        int shore = city.seaY + 2;
        int sea = city.h - shore;
        // ((15.8) Far bigger: half the width of the map, and most of the depth of the bay.)
        int iw = Math.min(150, Math.max(50, (int) (city.w * 0.5f))), ih = Math.min(80, Math.max(30, sea - 18));
        int ix0 = Math.max(4, city.w / 2 - iw / 2 - iw / 5), iy0 = shore + Math.max(9, (sea - ih) / 2);
        if (iy0 + ih > city.h - 3) ih = city.h - 3 - iy0;
        int ix1 = ix0 + iw, iy1 = iy0 + ih;
        city.fill(ix0, iy0, iw, ih, GRASS);
        // The compound: three tiles in from the shore, inside a wall.
        int cx0 = ix0 + 3, cy0 = iy0 + 3, cx1 = ix1 - 3, cy1 = iy1 - 3;
        city.fill(cx0, cy0, cx1 - cx0, cy1 - cy0, LOT);
        // ((15.6) A wall two tiles thick.)
        for (int t = 0; t < PRISON_WALL; t++) {
            for (int x = cx0; x < cx1; x++) {
                city.tiles[(cy0 + t) * city.w + x] = ROCK;
                city.tiles[(cy1 - 1 - t) * city.w + x] = ROCK;
            }
            for (int y = cy0; y < cy1; y++) {
                city.tiles[y * city.w + cx0 + t] = ROCK;
                city.tiles[y * city.w + cx1 - 1 - t] = ROCK;
            }
        }
        city.prison = new int[]{cx0, cy0, cx1, cy1};
        // The causeway: from the seafront road straight across the water to the gate.
        int gx = (cx0 + cx1) / 2 - 1;
        if (city.bridge == null) city.bridge = new boolean[city.w * city.h];
        for (int y = city.seaY - 3; y < cy0; y++)
            for (int x = gx; x < gx + 3; x++) if (city.tiles[y * city.w + x] == WATER) city.bridge[y * city.w + x] = true;
        gen.carve(new Street(gx, city.seaY - 3, gx + 3, cy0, true, false));
        // The gate: shut (part of the wall) till the prison falls.
        for (int y = cy0; y < cy0 + PRISON_WALL; y++)
            for (int x = gx; x < gx + 3; x++) city.tiles[y * city.w + x] = ROCK;
        city.prisonGate = new int[]{gx, cy0, gx + 3, cy0 + PRISON_WALL};
        // A gatehouse either side of the gate, just inside.
        int gh0 = cy0 + PRISON_WALL;
        gen.govLot(gx - 4, gh0, 3, 3, JAIL);
        gen.govLot(gx + 4, gh0, 3, 3, JAIL);
        // Watchtowers on the wall: the four corners, and halfway along the long sides.
        city.prisonTowers.clear();
        float m = PRISON_WALL * T / 2f;
        java.util.List<float[]> twl = new ArrayList<float[]>();
        twl.add(new float[]{cx0 * T + m, cy0 * T + m});
        twl.add(new float[]{cx1 * T - m, cy0 * T + m});
        twl.add(new float[]{cx0 * T + m, cy1 * T - m});
        twl.add(new float[]{cx1 * T - m, cy1 * T - m});
        // ((15.8) And more along every side of the bigger wall, every 30 tiles or so (not over the gate).)
        int along = Math.max(1, (cx1 - cx0) / 30);
        for (int k = 1; k < along; k++) {
            float x = (cx0 + (cx1 - cx0) * k / (float) along) * T;
            twl.add(new float[]{x, cy1 * T - m});
            if (Math.abs(x - (gx + 1.5f) * T) > 8 * T) twl.add(new float[]{x, cy0 * T + m});
        }
        int down = Math.max(1, (cy1 - cy0) / 30);
        for (int k = 1; k < down; k++) {
            float y = (cy0 + (cy1 - cy0) * k / (float) down) * T;
            twl.add(new float[]{cx0 * T + m, y});
            twl.add(new float[]{cx1 * T - m, y});
        }
        float[][] tw = twl.toArray(new float[0][]);
        for (float[] t : tw) city.prisonTowers.add(t);
        // Where the guards stand: inside the gate, at the foot of each tower, and at the barrier where the causeway
        // meets the seafront.
        city.prisonPosts.clear();
        city.prisonPosts.add(new float[]{(gx + 0.5f) * T, (gh0 + 1.5f) * T});
        city.prisonPosts.add(new float[]{(gx + 2.5f) * T, (gh0 + 1.5f) * T});
        for (float[] t : tw) {
            float px = t[0] + (t[0] < (cx0 + cx1) / 2f * T ? 2.5f : -2.5f) * T, py = t[1] + (t[1] < (cy0 + cy1) / 2f * T ? 2.5f : -2.5f) * T;
            if (t[1] > cy0 * T + 3 * T && t[1] < cy1 * T - 3 * T) py = t[1];
            if (t[0] > cx0 * T + 3 * T && t[0] < cx1 * T - 3 * T) px = t[0];
            city.prisonPosts.add(new float[]{px, py});
        }
        city.prisonPosts.add(new float[]{(gx - 0.8f) * T, (shore + 0.5f) * T});
        city.prisonPosts.add(new float[]{(gx + 3.8f) * T, (shore + 0.5f) * T});
        city.prisonBarrier = new float[]{gx * T, (shore + 1) * T, (gx + 3) * T};
        // Cell blocks along the yard, the admin block by the gate.
        int yx0 = cx0 + 3, yx1 = cx1 - 3, yy0 = cy0 + 4, yy1 = cy1 - 3;
        int blocks = Math.max(2, Math.min(6, (yx1 - yx0) / 18));
        int bw = Math.min(16, (yx1 - yx0 - (blocks - 1) * 6) / blocks), gap = (yx1 - yx0 - blocks * bw) / Math.max(1, blocks - 1);
        // ((15.8) Two rows of cell blocks on the bigger yard, with the exercise yard between them.)
        int rows = yy1 - yy0 >= 40 ? 2 : 1, bh = Math.max(5, Math.min(10, (yy1 - yy0) / (rows == 2 ? 5 : 3)));
        for (int row = 0; row < rows; row++)
            for (int k = 0; k < blocks; k++) {
                int bx = yx0 + k * (bw + gap), by = row == 0 ? yy0 + 5 : yy1 - bh - 2;
                if (row == 0 && bx + bw > gx - 5 && bx < gx + 8) continue;
                gen.govLot(bx, by, bw, bh, JAIL);
            }
        Building townJail = city.jail;
        gen.govLot(gx + 9, yy0, Math.max(4, Math.min(10, yx1 - gx - 10)), 3, JAIL);
        // The ferry: a landing on the island's far side, and one on the seafront across from it.
        int fy = iy0 + ih / 2;
        for (int x = ix1; x < ix1 + 4 && x < city.w; x++) {
            city.tiles[fy * city.w + x] = PIER;
            city.tiles[(fy + 1) * city.w + x] = PIER;
        }
        int mx = Math.min(city.w - 4, ix1 + 18);
        for (int y = shore; y < shore + 5 && y < city.h; y++) {
            city.tiles[y * city.w + mx] = PIER;
            city.tiles[y * city.w + mx + 1] = PIER;
        }
        city.ferry = new float[]{(ix1 + 6) * T, (fy + 1) * T, (mx + 1) * T, (shore + 7) * T};
        city.jail = townJail;
    }

    /**
     * ((22.0) A walled prison on a regular map: the island prison's wall, gate, watchtowers and cell blocks, on a big
     * block of its own, the gate facing the road along its top. Nothing starts in it by itself.
     */
    void landPrison(int x0, int y0, int x1, int y1) {
        city.fill(x0, y0, x1 - x0, y1 - y0, GRASS);
        int cx0 = x0 + 1, cy0 = y0 + 1, cx1 = x1 - 1, cy1 = y1 - 1;
        city.fill(cx0, cy0, cx1 - cx0, cy1 - cy0, LOT);
        for (int t = 0; t < PRISON_WALL; t++) {
            for (int x = cx0; x < cx1; x++) {
                city.tiles[(cy0 + t) * city.w + x] = ROCK;
                city.tiles[(cy1 - 1 - t) * city.w + x] = ROCK;
            }
            for (int y = cy0; y < cy1; y++) {
                city.tiles[y * city.w + cx0 + t] = ROCK;
                city.tiles[y * city.w + cx1 - 1 - t] = ROCK;
            }
        }
        city.prison = new int[]{cx0, cy0, cx1, cy1};
        int gx = (cx0 + cx1) / 2 - 1;
        // A drive from the road above to the gate.
        gen.carve(new Street(gx, y0 - 2, gx + 3, cy0, true, false));
        for (int y = cy0; y < cy0 + PRISON_WALL; y++)
            for (int x = gx; x < gx + 3; x++) city.tiles[y * city.w + x] = ROCK;
        city.prisonGate = new int[]{gx, cy0, gx + 3, cy0 + PRISON_WALL};
        int gh0 = cy0 + PRISON_WALL;
        float m = PRISON_WALL * T / 2f;
        city.prisonTowers.clear();
        city.prisonTowers.add(new float[]{cx0 * T + m, cy0 * T + m});
        city.prisonTowers.add(new float[]{cx1 * T - m, cy0 * T + m});
        city.prisonTowers.add(new float[]{cx0 * T + m, cy1 * T - m});
        city.prisonTowers.add(new float[]{cx1 * T - m, cy1 * T - m});
        city.prisonPosts.clear();
        city.prisonPosts.add(new float[]{(gx + 0.5f) * T, (gh0 + 1.5f) * T});
        city.prisonPosts.add(new float[]{(gx + 2.5f) * T, (gh0 + 1.5f) * T});
        for (float[] t : city.prisonTowers) {
            float px = t[0] + (t[0] < (cx0 + cx1) / 2f * T ? 2.5f : -2.5f) * T, py = t[1] + (t[1] < (cy0 + cy1) / 2f * T ? 2.5f : -2.5f) * T;
            city.prisonPosts.add(new float[]{px, py});
        }
        int yx0 = cx0 + 3, yx1 = cx1 - 3, yy0 = cy0 + 4, yy1 = cy1 - 3;
        int blocks = Math.max(2, Math.min(4, (yx1 - yx0) / 14));
        int bw = Math.max(5, Math.min(12, (yx1 - yx0 - (blocks - 1) * 4) / blocks)), gap = (yx1 - yx0 - blocks * bw) / Math.max(1, blocks - 1);
        int bh = Math.max(4, Math.min(7, (yy1 - yy0) / 3));
        Building townJail = city.jail;
        for (int k = 0; k < blocks; k++) {
            int bx = yx0 + k * (bw + gap);
            gen.govLot(bx, yy1 - bh - 1, bw, bh, JAIL);
        }
        city.jail = townJail;
        city.openAreas.add(new float[]{(cx0 + cx1) / 2f * T, (cy0 + cy1) / 2f * T, 0});
    }
}
