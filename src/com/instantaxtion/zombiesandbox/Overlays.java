package com.instantaxtion.zombiesandbox;

import android.graphics.Canvas;

/**
 * ((27.5) A map overlay, one at a time, chosen with the chip by the minimap: where the dead are thickest
 * ("Infection heat"), or how far each part of the city is from the nearest police officer or soldier ("Police/army
 * reach", with the parts nobody covers in red). Worked out once a second over the spatial grid's cells (World.CELL),
 * kept as a shade per cell and drawn as translucent runs over the map, so nothing is allocated while drawing.
 */
final class Overlays {
    static final int OFF = 0, HEAT = 1, REACH = 2, MODES = 3;
    static final String[] NAMES = {"Overlay: off", "Infection heat", "Police/army reach"};
    /** Shades for each level (0 = nothing drawn). Heat: amber to deep red. Reach: covered blue, thin, gap, nobody. */
    static final int[] HEAT_COL = {0, 0x50FFC040, 0x70FF8A30, 0x90F04A28, 0xB0D01818};
    static final int[] REACH_COL = {0, 0x3858A8FF, 0x58E8D048, 0x78F08A30, 0x98E02828};
    /** Reach, in cells, for each band: within 3 cells is covered, 6 thin, 10 a gap, beyond nobody. */
    static final int NEAR = 3, THIN = 6, GAP = 10;

    final GameView gv;
    int mode;
    byte[] level;
    boolean[] water;
    int[] dist, queue;
    World built;
    float builtAt = -99;
    int builtMode = -1;
    /** Cells counted on the last pass: zombies (heat) or units found (reach), for the legend and the test. */
    int found;

    Overlays(GameView gv) {
        this.gv = gv;
    }

    void cycle() {
        mode = (mode + 1) % MODES;
        builtMode = -1;
    }

    /** Recomputed when the mode or world changed or a second has passed (the game's clock, so paused it holds). */
    void refresh(World w) {
        if (mode == OFF) return;
        if (built == w && builtMode == mode && Math.abs(w.time - builtAt) < 1) return;
        int n = w.gw * w.gh;
        if (built != w || level == null || level.length != n) {
            level = new byte[n];
            dist = new int[n];
            queue = new int[n];
            water = new boolean[n];
            City city = w.city;
            for (int gy = 0; gy < w.gh; gy++)
                for (int gx = 0; gx < w.gw; gx++) {
                    int tx = (gx * World.CELL + World.CELL / 2) / City.T, ty = (gy * World.CELL + World.CELL / 2) / City.T;
                    water[gy * w.gw + gx] = tx >= city.w || ty >= city.h || city.tiles[ty * city.w + tx] == City.WATER;
                }
        }
        built = w;
        builtMode = mode;
        builtAt = w.time;
        if (mode == HEAT) heat(w);
        else reach(w);
    }

    /** Each cell's dead with half of its neighbours', in five shades. */
    void heat(World w) {
        int gw = w.gw, gh = w.gh;
        found = 0;
        for (int gy = 0; gy < gh; gy++)
            for (int gx = 0; gx < gw; gx++) {
                int c = gy * gw + gx, s = w.zCount[c] * 2;
                found += w.zCount[c];
                if (gx > 0) s += w.zCount[c - 1];
                if (gx < gw - 1) s += w.zCount[c + 1];
                if (gy > 0) s += w.zCount[c - gw];
                if (gy < gh - 1) s += w.zCount[c + gw];
                level[c] = (byte) (s == 0 ? 0 : s < 4 ? 1 : s < 12 ? 2 : s < 30 ? 3 : 4);
            }
    }

    /** How many cells from the nearest police officer or soldier (on foot, or crewing a vehicle), by a search out from them all. */
    void reach(World w) {
        int gw = w.gw, gh = w.gh, n = gw * gh, head = 0, tail = 0;
        java.util.Arrays.fill(dist, Integer.MAX_VALUE);
        found = 0;
        for (int c = 0; c < n; c++) {
            if (w.aCount[c] == 0) continue;
            for (int k = w.cellStart[c] + w.zCount[c], end = k + w.aCount[c]; k < end; k++) {
                Entity e = w.sorted[k];
                if (e.dead || e.type != Entity.COP && e.type != Entity.SOLDIER) continue;
                dist[c] = 0;
                queue[tail++] = c;
                found++;
                break;
            }
        }
        for (int i = 0, m = w.fleet.vehicles.size(); i < m; i++) {
            Fleet.Vehicle v = w.fleet.vehicles.get(i);
            if (v.crew.isEmpty() || v.crew.get(0).type != Entity.COP && v.crew.get(0).type != Entity.SOLDIER) continue;
            int gx = (int) (v.x / World.CELL), gy = (int) (v.y / World.CELL);
            if (gx < 0 || gy < 0 || gx >= gw || gy >= gh || dist[gy * gw + gx] == 0) continue;
            dist[gy * gw + gx] = 0;
            queue[tail++] = gy * gw + gx;
            found++;
        }
        while (head < tail) {
            int c = queue[head++], d = dist[c] + 1, gx = c % gw;
            if (gx > 0 && dist[c - 1] > d) { dist[c - 1] = d; queue[tail++] = c - 1; }
            if (gx < gw - 1 && dist[c + 1] > d) { dist[c + 1] = d; queue[tail++] = c + 1; }
            if (c >= gw && dist[c - gw] > d) { dist[c - gw] = d; queue[tail++] = c - gw; }
            if (c + gw < n && dist[c + gw] > d) { dist[c + gw] = d; queue[tail++] = c + gw; }
        }
        for (int c = 0; c < n; c++) {
            int d = dist[c];
            level[c] = (byte) (water[c] ? 0 : d <= NEAR ? 1 : d <= THIN ? 2 : d <= GAP ? 3 : 4);
        }
    }

    /** Over the map, in world coordinates (the canvas already set to the camera): a rect for each run of one shade along a row. */
    void draw(Canvas c, World w, float vx0, float vy0, float vx1, float vy1) {
        if (mode == OFF) return;
        refresh(w);
        int[] cols = mode == HEAT ? HEAT_COL : REACH_COL;
        int gw = w.gw;
        int x0 = Math.max(0, (int) (vx0 / World.CELL)), x1 = Math.min(gw - 1, (int) (vx1 / World.CELL));
        int y0 = Math.max(0, (int) (vy0 / World.CELL)), y1 = Math.min(w.gh - 1, (int) (vy1 / World.CELL));
        for (int gy = y0; gy <= y1; gy++) {
            int gx = x0;
            while (gx <= x1) {
                int lv = level[gy * gw + gx], run = gx;
                while (run + 1 <= x1 && level[gy * gw + run + 1] == lv) run++;
                if (lv > 0) {
                    gv.fill.setColor(cols[lv]);
                    c.drawRect(gx * World.CELL, gy * World.CELL, (run + 1) * World.CELL, (gy + 1) * World.CELL, gv.fill);
                }
                gx = run + 1;
            }
        }
    }
}
