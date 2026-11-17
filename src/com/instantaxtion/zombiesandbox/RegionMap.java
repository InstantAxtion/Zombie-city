package com.instantaxtion.zombiesandbox;

import android.graphics.Canvas;
import android.graphics.Paint;
import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.City.*;

/**
 * ((28.0) A map laid out and painted by a class of its own: Highway Crossroads (CrossroadsMap), Canyon Town
 * (CanyonMap) and Frozen North (FrozenNorthMap). The city's builder and painter ask it at a few points
 * (City.region); on every other map there is none, and nothing changes.
 */
abstract class RegionMap extends CityPart {
    RegionMap(City city) {
        super(city);
    }

    /** The map's own builder, wired to the city's other parts, or null if the map has none. */
    static RegionMap of(City city) {
        RegionMap m = city.cfg.crossroads() ? new CrossroadsMap(city) : city.cfg.canyon() ? new CanyonMap(city)
                : city.cfg.frozenNorth() ? new FrozenNorthMap(city) : null;
        if (m != null) m.wire();
        return m;
    }

    /** Where the town goes ({x0, y0, x1, y1} in tiles), given where it would. */
    int[] townRect(int bx0, int by0, int bx1, int by1) {
        return new int[]{bx0, by0, bx1, by1};
    }

    /** Lays the streets (the districts are in); false to have the town laid out as usual. */
    boolean layStreets(int bx0, int by0, int bx1, int by1, int in) {
        return false;
    }

    /** Once the highway (if any) is in, before the ring road, the beltway and the country. */
    void afterHighway(int bx0, int by0, int bx1, int by1) {
    }

    /** Its own places, on blocks before the bases, services and landmarks take theirs. */
    void places(boolean[] used) {
    }

    /** Last, once every block is filled and the streets are named. */
    void finish() {
    }

    /** The colour of open ground (grass) here. */
    abstract int ground(int x, int y);

    /** The colour of bare rock here. */
    int rock(int x, int y) {
        return 0xFF7E796E;
    }

    /** The colour of open water here, or 0 for the usual. */
    int water(int x, int y) {
        return 0;
    }

    /** Draws a tile's ground texture itself (true), or leaves it to the painter (false). */
    boolean texture(Canvas c, Paint p, byte t, int x, int y, float fx, float fy, Random r) {
        return false;
    }

    /** Draws over the ground, the roads and the decorations, under the buildings (tiles X0..X1, Y0..Y1). */
    void paintOver(Canvas c, Paint p, int X0, int Y0, int X1, int Y1) {
    }

    // ------------------------------------------------------------------ helpers the maps share

    static int mix(int a, int b, float f) {
        f = Math.max(0, Math.min(1, f));
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return 0xFF000000 | Math.round(ar + (br - ar) * f) << 16 | Math.round(ag + (bg - ag) * f) << 8 | Math.round(ab + (bb - ab) * f);
    }

    byte tile(int x, int y) {
        return x < 0 || y < 0 || x >= city.w || y >= city.h ? WATER : city.tiles[y * city.w + x];
    }

    /** Whether the rectangle is all open grass or trees (and on the map). */
    boolean open(int x0, int y0, int w, int h) {
        if (x0 < 1 || y0 < 1 || x0 + w > city.w - 1 || y0 + h > city.h - 1) return false;
        for (int y = y0; y < y0 + h; y++)
            for (int x = x0; x < x0 + w; x++) {
                byte t = city.tiles[y * city.w + x];
                if (t != GRASS && t != TREE) return false;
            }
        return true;
    }

    /** Clears a tile to open ground: no road, no direction. */
    void clear(int i, byte t) {
        city.tiles[i] = t;
        city.roadDir[i] = 0;
        city.mainRoad[i] = false;
        if (city.boulevard != null) city.boulevard[i] = false;
    }

    /**
     * A winding road from tile (x, y) across open ground to the nearest road (outside the rectangle rx, ry, rw, rh),
     * smoothed and laid as a curve three tiles wide. False if there's no way.
     */
    boolean roadToNetwork(int x, int y, int rx, int ry, int rw, int rh, int maxLen) {
        if (x < 1 || y < 1 || x >= city.w - 1 || y >= city.h - 1) return false;
        int[] prev = new int[city.w * city.h];
        int start = y * city.w + x;
        int end = gen.roadSearch(start, rx, ry, rw, rh, prev);
        if (end < 0) return false;
        List<Integer> path = new java.util.ArrayList<Integer>();
        for (int c = end; c != -1; c = prev[c]) path.add(0, c);
        if (path.size() > maxLen) return false;
        List<Float> pts = new java.util.ArrayList<Float>();
        pts.add(x + 0.5f);
        pts.add(y + 0.5f);
        for (int k = 3; k < path.size() - 1; k += 4) {
            pts.add(path.get(k) % city.w + 0.5f);
            pts.add(path.get(k) / city.w + 0.5f);
        }
        pts.add(end % city.w + 0.5f);
        pts.add(end / city.w + 0.5f);
        float[] line = new float[pts.size()];
        for (int k = 0; k < line.length; k++) line[k] = pts.get(k);
        for (int it = 0; it < 2; it++) line = CityGenerator.chaikin(line);
        gen.curveOnGrass = true;
        landGen.curveRoad(line, false);
        gen.curveOnGrass = false;
        return true;
    }
}
