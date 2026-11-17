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
 * (27.3) The waterside maps: the harbour's coast and docks, and the lake in the middle of the lakeside town. Taken
 * out of City.java as it stood; the city itself (its tiles, buildings and everything the game asks of it) is `city`.
 */
final class CoastMap extends CityPart {
    CoastMap(City city) {
        super(city);
    }

    /** Lakes, as {x0, y0, x1, y1} tiles. */
    final List<int[]> lakes = new ArrayList<int[]>();

    /** The sea along the bottom of the map (Harbour): a concrete quay with docks, cranes, containers and ships. */
    void coast() {
        city.bridge = new boolean[city.w * city.h];
        city.fill(0, city.seaY, city.w, 2, SIDEWALK);
        int shore = city.seaY + 2;
        city.fill(0, shore, city.w, city.h - shore, WATER);
        double ph = city.rnd.nextDouble() * 10;
        // ((15.6) The Prison Island's bay: a promenade along the water and a few boats, no docks.)
        if (city.cfg.prisonIsland()) {
            city.fill(0, shore, city.w, 2, PLAZA);
            for (int k = 0; k < city.w / 60; k++) {
                float bx = (4 + city.rnd.nextInt(city.w - 10)) * T, by = (shore + 6 + city.rnd.nextInt(Math.max(1, city.h - shore - 10))) * T;
                if (by / T >= city.h - 1 || city.tiles[(int) (by / T) * city.w + (int) (bx / T)] != WATER) continue;
                city.addDecor(D_BOAT, bx, by, bx + 30, by + 10, 1);
            }
            return;
        }
        {
            // The quay, and docks reaching out into the harbour.
            int quay = 4;
            city.fill(0, shore, city.w, quay, LOT);
            int qy = shore + quay;
            for (int x = 14 + city.rnd.nextInt(10); x < city.w - 20; x += 34 + city.rnd.nextInt(12)) {
                int len = Math.min(city.h - qy - 6, 12 + city.rnd.nextInt(6));
                city.fill(x, qy, 7, len, LOT);
                // Cranes on the dock, containers stacked, and a ship alongside.
                city.addDecor(D_CRANE, (x + 1) * T, (qy + 2) * T, (x + 6) * T, (qy + 5) * T, 0);
                city.addDecor(D_CONTAINERS, (x + 1) * T, (qy + 6) * T, (x + 6) * T, (qy + Math.min(len - 1, 12)) * T, city.rnd.nextInt(100));
                boolean right = city.rnd.nextBoolean();
                float sx0 = right ? (x + 7) * T + 3 : (x - 5) * T - 3;
                city.addDecor(D_SHIP, sx0, (qy + 1) * T, sx0 + 5 * T, (qy + len) * T, city.rnd.nextInt(4));
            }
            // Containers along the quay between the docks.
            for (int x = 4; x < city.w - 12; x += 26)
                if (city.tiles[(qy - 1) * city.w + x] == LOT && city.tiles[(qy - 1) * city.w + x + 8] == LOT)
                    city.addDecor(D_CONTAINERS, x * T, (shore + 1) * T, (x + 8) * T, (shore + 3) * T, city.rnd.nextInt(100));
            for (int k = 0; k < city.w / 40; k++) {
                float bx = (4 + city.rnd.nextInt(city.w - 10)) * T, by = (qy + 22 + city.rnd.nextInt(Math.max(1, city.h - qy - 26))) * T;
                if (by / T >= city.h - 1 || city.tiles[(int) (by / T) * city.w + (int) (bx / T)] != WATER) continue;
                city.addDecor(D_BOAT, bx, by, bx + 30, by + 10, 1);
            }
        }
    }

    /**
     * A lake filling a piece of town between the streets: grass round the shore with trees, an irregular
     * shoreline, a little wooded island on a big lake, a jetty and rowing boats.
     */
    void lake(int x0, int y0, int x1, int y1) {
        lakes.add(new int[]{x0, y0, x1, y1});
        city.fill(x0, y0, x1 - x0, y1 - y0, SIDEWALK);
        city.fill(x0 + 1, y0 + 1, x1 - x0 - 2, y1 - y0 - 2, GRASS);
        float cx = (x0 + x1) / 2f, cy = (y0 + y1) / 2f, rx = (x1 - x0) / 2f - 3, ry = (y1 - y0) / 2f - 3;
        double ph = city.rnd.nextDouble() * 6;
        for (int y = y0; y < y1; y++)
            for (int x = x0; x < x1; x++) {
                float dx = (x + 0.5f - cx) / rx, dy = (y + 0.5f - cy) / ry;
                double a = Math.atan2(dy, dx);
                double r = 1 - 0.08 * Math.sin(a * 3 + ph) - 0.05 * Math.sin(a * 5 + ph * 2);
                if (dx * dx + dy * dy <= r * r) city.tiles[y * city.w + x] = WATER;
            }
        if (rx >= 10 && ry >= 10) {
            float ix = cx + rx * 0.3f, iy = cy - ry * 0.25f;
            for (int y = (int) iy - 3; y <= (int) iy + 3; y++)
                for (int x = (int) ix - 3; x <= (int) ix + 3; x++)
                    if ((x + 0.5f - ix) * (x + 0.5f - ix) + (y + 0.5f - iy) * (y + 0.5f - iy) <= 7) city.tiles[y * city.w + x] = GRASS;
            city.tiles[(int) iy * city.w + (int) ix] = TREE;
        }
        // Trees round the shore, leaving a walk along the water's edge.
        for (int y = y0 + 1; y < y1 - 1; y++)
            for (int x = x0 + 1; x < x1 - 1; x++) {
                if (city.tiles[y * city.w + x] != GRASS || city.rnd.nextFloat() > 0.14f) continue;
                boolean nearWater = false;
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) if (city.tiles[(y + dy) * city.w + x + dx] == WATER) nearWater = true;
                if (!nearWater) city.tiles[y * city.w + x] = TREE;
            }
        // A jetty out from the south shore.
        int jx = (int) cx - 1, jy = y1 - 2;
        while (jy > y0 && city.tiles[jy * city.w + jx] != WATER) jy--;
        for (int k = 0; k < 5 && jy - k > y0; k++) {
            city.tiles[(jy - k) * city.w + jx] = PIER;
            city.tiles[(jy - k) * city.w + jx + 1] = PIER;
        }
        for (int k = 0; k < 2 + (int) (rx * ry / 60); k++) {
            float bx = (cx + (city.rnd.nextFloat() - 0.5f) * rx) * T, by = (cy + (city.rnd.nextFloat() - 0.5f) * ry) * T;
            if (city.tiles[(int) (by / T) * city.w + (int) (bx / T)] == WATER && city.tiles[(int) (by / T) * city.w + (int) (bx / T) + 1] == WATER)
                city.addDecor(D_BOAT, bx, by, bx + 18, by + 8, 0);
        }
        city.openAreas.add(new float[]{cx * T, (y1 - 1.5f) * T, 0});
    }

    void lakeTown(int bx0, int by0, int bx1, int by1, int in) {
        int lw = (int) ((bx1 - bx0) * 0.36f), lh = (int) ((by1 - by0) * 0.3f);
        int lx0 = (bx0 + bx1) / 2 - lw / 2 + city.rnd.nextInt(9) - 4, ly0 = (by0 + by1) / 2 - lh / 2 + city.rnd.nextInt(9) - 4;
        int lx1 = lx0 + lw, ly1 = ly0 + lh;
        gen.carve(new Street(bx0, ly0 - 3, bx1, ly0, false, true));
        gen.carve(new Street(bx0, ly1, bx1, ly1 + 3, false, true));
        gen.carve(new Street(lx0 - 3, by0, lx0, by1, true, true));
        gen.carve(new Street(lx1, by0, lx1 + 3, by1, true, true));
        int[] xs = {bx0 + in, lx0 - 3, lx0, lx1, lx1 + 3, bx1 - in}, ys = {by0 + in, ly0 - 3, ly0, ly1, ly1 + 3, by1 - in};
        for (int j = 0; j < 3; j++)
            for (int i = 0; i < 3; i++) {
                if (i == 1 && j == 1) continue;
                if (xs[i * 2 + 1] - xs[i * 2] < 6 || ys[j * 2 + 1] - ys[j * 2] < 6) continue;
                gen.split(xs[i * 2], ys[j * 2], xs[i * 2 + 1], ys[j * 2 + 1], 1, -1, -1);
            }
        lake(lx0, ly0, lx1, ly1);
    }
}
