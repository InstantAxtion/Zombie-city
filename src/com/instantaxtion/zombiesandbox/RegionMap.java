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
        mainBase(used);
    }

    /**
     * The main army base, on the biggest block with a street right outside its front gate (the blocks these maps cut
     * back can have lost theirs). The usual pick of a block is then skipped (as on Desert City, whose base is out in
     * the desert): the extra bases on the bigger maps go up as usual.
     */
    void mainBase(boolean[] used) {
        int best = -1, bestArea = 0;
        for (int k = 0; k < city.blocks.size(); k++) {
            int[] b = city.blocks.get(k);
            int bw = b[2] - b[0] - 2, bh = b[3] - b[1] - 2;
            if (used[k] || b.length > 4 || bw < 9 || bh < 9 || bw * bh <= bestArea) continue;
            int gx = b[0] + 1 + bw / 2;
            if (b[3] >= city.h || !gen.isRoad(gx, b[3]) || !gen.isRoad(gx - 1, b[3]) || !gen.isRoad(gx + 1, b[3])) continue;
            bestArea = bw * bh;
            best = k;
        }
        if (best < 0) return;
        used[best] = true;
        int[] b = city.blocks.get(best);
        city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], SIDEWALK);
        baseGen.militaryBase(b[0] + 1, b[1] + 1, b[2] - b[0] - 2, b[3] - b[1] - 2);
        gen.testRange = new int[4];
    }

    /** Blocks the streets no longer reach on any side (cut off by water or rock): open ground, with the odd tree. */
    void dropRoadless(Random rr) {
        // (A scrap of street left over, a few tiles on its own, isn't a way in.)
        int n = city.w * city.h;
        int[] comp = new int[n];
        int[] q = new int[n];
        List<Integer> sizes = new java.util.ArrayList<Integer>();
        java.util.Arrays.fill(comp, -1);
        for (int i = 0; i < n; i++) {
            if (comp[i] >= 0 || city.tiles[i] != ROAD) continue;
            int id = sizes.size(), hd = 0, tl = 0;
            q[tl++] = i;
            comp[i] = id;
            while (hd < tl) {
                int c = q[hd++], x = c % city.w, y = c / city.w;
                for (int k = 0; k < 4; k++) {
                    int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = y + (k == 2 ? 1 : k == 3 ? -1 : 0);
                    if (nx < 0 || ny < 0 || nx >= city.w || ny >= city.h) continue;
                    int j = ny * city.w + nx;
                    if (comp[j] >= 0 || city.tiles[j] != ROAD) continue;
                    comp[j] = id;
                    q[tl++] = j;
                }
            }
            sizes.add(tl);
        }
        int big = 0;
        for (int k = 0; k < sizes.size(); k++) if (sizes.get(k) > sizes.get(big)) big = k;
        for (int k = city.blocks.size() - 1; k >= 0; k--) {
            int[] b = city.blocks.get(k);
            boolean road = false;
            for (int x = b[0] + 1; x < b[2] - 1 && !road; x++) road = onMain(x, b[1] - 1, comp, big) || onMain(x, b[3], comp, big);
            for (int y = b[1] + 1; y < b[3] - 1 && !road; y++) road = onMain(b[0] - 1, y, comp, big) || onMain(b[2], y, comp, big);
            if (road) continue;
            city.blocks.remove(k);
            for (int y = b[1]; y < b[3]; y++)
                for (int x = b[0]; x < b[2]; x++) city.tiles[y * city.w + x] = rr.nextFloat() < 0.06f ? TREE : GRASS;
        }
    }

    /**
     * A road along the edge of mask (rock or water): the open ground d0 to d1 tiles from it (counting diagonally),
     * outside every block, running the way dir (1 north-south, 2 east-west) - the streets that ran into the mask
     * now end at it.
     */
    void rimRoad(boolean[] mask, int d0, int d1, byte dir) {
        int n = city.w * city.h;
        boolean[] inBlock = new boolean[n];
        for (int[] b : city.blocks)
            for (int y = Math.max(0, b[1]); y < Math.min(city.h, b[3]); y++)
                for (int x = Math.max(0, b[0]); x < Math.min(city.w, b[2]); x++) inBlock[y * city.w + x] = true;
        int[] d = new int[n];
        int[] q = new int[n];
        java.util.Arrays.fill(d, 99);
        int hd = 0, tl = 0;
        for (int i = 0; i < n; i++)
            if (mask[i]) {
                d[i] = 0;
                q[tl++] = i;
            }
        while (hd < tl) {
            int c = q[hd++], x = c % city.w, y = c / city.w;
            if (d[c] >= d1) continue;
            for (int dy = -1; dy <= 1; dy++)
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx, ny = y + dy;
                    if (nx < 0 || ny < 0 || nx >= city.w || ny >= city.h) continue;
                    int j = ny * city.w + nx;
                    if (d[j] <= d[c] + 1) continue;
                    d[j] = d[c] + 1;
                    q[tl++] = j;
                }
        }
        for (int i = 0; i < n; i++) {
            if (d[i] < d0 || d[i] > d1 || inBlock[i] || mask[i]) continue;
            byte t = city.tiles[i];
            if (t == ROAD) {
                if (city.roadDir[i] != dir) city.roadDir[i] = 3;
                continue;
            }
            if (t != GRASS && t != TREE && t != SIDEWALK && t != SAND) continue;
            city.tiles[i] = ROAD;
            city.roadDir[i] = dir;
        }
    }

    /**
     * Homes on the open ground left beside a road (strips too narrow for a block): up to max of them, each three by
     * four tiles with clear ground round it and a road a step from its door.
     */
    int roadsideHomes(int max, Random rr, int[] roofs, int[] walls) {
        int made = 0;
        for (int y = 3; y < city.h - 6 && made < max; y++)
            for (int x = 3; x < city.w - 6 && made < max; x++) {
                boolean tall = ((x * 7 + y * 3) & 1) == 0;
                int w = tall ? 3 : 4, h = tall ? 4 : 3;
                if (!open(x - 1, y - 1, w + 2, h + 2)) continue;
                boolean road = false;
                for (int k = -2; k <= Math.max(w, h) + 1 && !road; k++)
                    road = gen.isRoad(x + k, y - 2) || gen.isRoad(x + k, y + h + 1) || gen.isRoad(x - 2, y + k) || gen.isRoad(x + w + 1, y + k);
                if (!road || rr.nextFloat() < 0.3f) continue;
                city.natureLot(x, y, w, h, HOUSE, 1, roofs[rr.nextInt(roofs.length)], walls[rr.nextInt(walls.length)], null, -1);
                made++;
                x += w + 2;
            }
        return made;
    }

    boolean onMain(int x, int y, int[] comp, int big) {
        return x >= 0 && y >= 0 && x < city.w && y < city.h && comp[y * city.w + x] == big;
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
        return x < 0 || y < 0 || x >= city.w || y >= city.h ? GRASS : city.tiles[y * city.w + x];
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

    /**
     * The blocks mask runs through: each cut back to the part either side of it (across x if acrossX, else across y),
     * three tiles clear, the ground between left open with the odd tree.
     */
    void cutBlocks(boolean[] mask, boolean acrossX, Random rr) {
        cutBlocks(mask, acrossX, rr, 3);
    }

    void cutBlocks(boolean[] mask, boolean acrossX, Random rr, int gap) {
        int W = city.w;
        for (int k = city.blocks.size() - 1; k >= 0; k--) {
            int[] bk = city.blocks.get(k);
            int wl = Integer.MAX_VALUE, wh = Integer.MIN_VALUE;
            for (int y = bk[1]; y < bk[3]; y++)
                for (int x = bk[0]; x < bk[2]; x++)
                    if (mask[y * W + x]) {
                        int c = acrossX ? x : y;
                        wl = Math.min(wl, c);
                        wh = Math.max(wh, c);
                    }
            if (wh < wl) continue;
            city.blocks.remove(k);
            int lo0 = acrossX ? bk[0] : bk[1], hi0 = acrossX ? bk[2] : bk[3];
            int q0 = wl - gap, q1 = wh + gap;
            if (q0 - lo0 >= 7) city.blocks.add(acrossX ? new int[]{bk[0], bk[1], q0, bk[3]} : new int[]{bk[0], bk[1], bk[2], q0});
            else q0 = lo0;
            if (hi0 - q1 - 1 >= 7) city.blocks.add(acrossX ? new int[]{q1 + 1, bk[1], bk[2], bk[3]} : new int[]{bk[0], q1 + 1, bk[2], bk[3]});
            else q1 = hi0 - 1;
            for (int y = bk[1]; y < bk[3]; y++)
                for (int x = bk[0]; x < bk[2]; x++) {
                    int c = acrossX ? x : y;
                    if (c < q0 || c > q1 || mask[y * W + x]) continue;
                    city.tiles[y * W + x] = rr.nextFloat() < 0.05f ? TREE : GRASS;
                }
        }
    }
}
