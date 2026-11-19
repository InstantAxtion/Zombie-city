package com.instantaxtion.zombiesandbox;

import android.graphics.Canvas;
import android.graphics.Paint;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.City.*;

/**
 * ((28.0) Frozen North: a harbour town round the head of a bay frozen over, its quays and piers with ships iced in;
 * out across the snow, an airstrip, a radar and research station, and cabins scattered far out along long roads.
 * The snow and the ice are only a look: the ground, the roofs, the banks along the roads.
 */
final class FrozenNorthMap extends RegionMap {
    FrozenNorthMap(City city) {
        super(city);
    }

    final Random r = new Random(city.cfg.seed * 6151L + 29);
    /** Which edge the bay opens on (0 left, 1 right, 2 top, 3 bottom), and its shape along that edge. */
    int side, L;
    float uc, mouth, depth, sea;
    boolean[] bay;
    int[] town;
    /** What went up, for the test. */
    boolean airstrip, station;
    int cabins, piers;

    /** The tile at u along the bay's edge, v in from it. */
    int idx(int u, int v) {
        switch (side) {
            case 0: return u * city.w + v;
            case 1: return u * city.w + (L - 1 - v);
            case 2: return v * city.w + u;
            default: return (L - 1 - v) * city.w + u;
        }
    }

    /** A rectangle u0..u1, v0..v1 as {x0, y0, x1, y1}. */
    int[] rect(int u0, int v0, int u1, int v1) {
        switch (side) {
            case 0: return new int[]{v0, u0, v1, u1};
            case 1: return new int[]{L - v1, u0, L - v0, u1};
            case 2: return new int[]{u0, v0, u1, v1};
            default: return new int[]{u0, L - v1, u1, L - v0};
        }
    }

    @Override
    int[] townRect(int bx0, int by0, int bx1, int by1) {
        L = city.w;
        side = r.nextInt(4);
        uc = L * (0.4f + r.nextFloat() * 0.2f);
        mouth = L * (0.08f + r.nextFloat() * 0.03f);
        depth = L * (0.26f + r.nextFloat() * 0.06f);
        sea = Math.max(6, L * 0.035f);
        // The town round the head of the bay, out along both its shores.
        // ((29.0) Map version 43: wider and deeper, more of the map town than snow.)
        boolean more = city.cfg.moreTown();
        int tw = Math.round(L * (more ? 0.86f : 0.72f)), u0 = Math.max(6, Math.round(uc - tw / 2f)), u1 = Math.min(L - 6, u0 + tw);
        int v0 = Math.round(sea) - 2, v1 = Math.min(L - 36, Math.round(depth + L * (more ? 0.42f : 0.26f)));
        town = rect(u0, v0, u1, v1);
        return town;
    }

    @Override
    boolean layStreets(int bx0, int by0, int bx1, int by1, int in) {
        int n = city.w * city.h;
        // Everything outside town is snow (the ring round town is laid already).
        for (int y = 0; y < city.h; y++)
            for (int x = 0; x < city.w; x++)
                if (x < bx0 || y < by0 || x >= bx1 || y >= by1) clear(y * city.w + x, GRASS);
        gen.split(bx0 + in, by0 + in, bx1 - in, by1 - in, 0, -1, -1);
        // The bay: the sea along the edge, and the inlet reaching into town, its shore wandering.
        bay = new boolean[n];
        boolean[] open = new boolean[n], inlet = new boolean[n];
        float p1 = r.nextFloat() * 6.3f, p2 = r.nextFloat() * 6.3f;
        for (int u = 0; u < L; u++)
            for (int v = 0; v < L; v++) {
                int i = idx(u, v);
                if (v < sea + 2 * (float) Math.sin(u * 0.05f + p1)) open[i] = true;
                float t = v / depth;
                if (t < 1) {
                    float hw = mouth * (float) Math.sqrt(1 - t * t) * (1 + 0.12f * (float) Math.sin(v * 0.09f + p2));
                    float cu = uc + mouth * 0.25f * (float) Math.sin(v * 0.03f + p1);
                    if (Math.abs(u + 0.5f - cu) < hw && !open[i]) inlet[i] = true;
                }
                bay[i] = open[i] || inlet[i];
            }
        // Blocks the water reaches cut back from it; the streets cut off stop short of it.
        boolean alongX = side >= 2;
        cutBlocks(open, !alongX, r, 4);
        cutBlocks(inlet, alongX, r, 4);
        for (int i = 0; i < n; i++) if (bay[i]) clear(i, WATER);
        // The shore road in town, the quay between it and the water; the streets that ran on into the bay end at it.
        boolean[] inTown = new boolean[n];
        for (int y = Math.max(0, by0 - 1); y < Math.min(city.h, by1 + 1); y++)
            for (int x = Math.max(0, bx0 - 1); x < Math.min(city.w, bx1 + 1); x++) inTown[y * city.w + x] = true;
        boolean[] shore = new boolean[n];
        for (int i = 0; i < n; i++) shore[i] = bay[i];
        rimRoad(shore, 2, 4, (byte) (alongX ? 1 : 2));
        for (int i = 0; i < n; i++) if (!inTown[i] && city.tiles[i] == ROAD && city.roadDir[i] != 0 && !nearTownRoad(i, bx0, by0, bx1, by1)) clear(i, GRASS);
        dropRoadless(r);
        quays();
        // Districts out on the snow have no name to show.
        for (District d : city.districts) {
            int x = (int) d.x, y = (int) d.y;
            if (x < bx0 || y < by0 || x >= bx1 || y >= by1 || (x >= 0 && y >= 0 && x < city.w && y < city.h && bay[y * city.w + x])) d.unnamed = true;
        }
        // Out on the snow: the airstrip, the radar station, the cabins, each on a long road back to town.
        airstrip();
        station();
        cabins();
        return true;
    }

    boolean nearTownRoad(int i, int bx0, int by0, int bx1, int by1) {
        int x = i % city.w, y = i / city.w;
        return x >= bx0 - 1 && x <= bx1 && y >= by0 - 1 && y <= by1;
    }

    /** Along the town's shore: a paved quay, and piers out into the ice with ships frozen in beside them. */
    void quays() {
        int[] q = town;
        for (int y = Math.max(1, q[1]); y < Math.min(city.h - 1, q[3]); y++)
            for (int x = Math.max(1, q[0]); x < Math.min(city.w - 1, q[2]); x++) {
                int i = y * city.w + x;
                if (city.tiles[i] != GRASS) continue;
                if (city.wet(x + 1, y) || city.wet(x - 1, y) || city.wet(x, y + 1) || city.wet(x, y - 1)) city.tiles[i] = SIDEWALK;
            }
        // Piers: from the quay straight out across the inlet's sides, every so often.
        int want = Math.max(2, L / 70);
        List<Integer> shore = new ArrayList<Integer>();
        for (int y = Math.max(1, q[1]); y < Math.min(city.h - 1, q[3]); y++)
            for (int x = Math.max(1, q[0]); x < Math.min(city.w - 1, q[2]); x++)
                if (city.tiles[y * city.w + x] == SIDEWALK && (city.wet(x + 1, y) || city.wet(x - 1, y) || city.wet(x, y + 1) || city.wet(x, y - 1))) shore.add(y * city.w + x);
        java.util.Collections.shuffle(shore, r);
        List<int[]> made = new ArrayList<int[]>();
        for (int si = 0; si < shore.size() && piers < want; si++) {
            int x = shore.get(si) % city.w, y = shore.get(si) / city.w;
            boolean near = false;
            for (int[] m : made) if (Math.abs(m[0] - x) + Math.abs(m[1] - y) < 24) near = true;
            if (near) continue;
            int dx = city.wet(x + 1, y) ? 1 : city.wet(x - 1, y) ? -1 : 0, dy = dx != 0 ? 0 : city.wet(x, y + 1) ? 1 : city.wet(x, y - 1) ? -1 : 0;
            if (dx == 0 && dy == 0) continue;
            int len = 8 + r.nextInt(5);
            boolean ok = true;
            for (int k = 1; k <= len + 3 && ok; k++)
                for (int s = -2; s <= 3; s++) {
                    int px = x + dx * k + (dy != 0 ? s : 0), py = y + dy * k + (dx != 0 ? s : 0);
                    if (tile(px, py) != WATER) ok = false;
                }
            if (!ok) continue;
            made.add(new int[]{x, y});
            for (int k = 1; k <= len; k++)
                for (int s = 0; s <= 1; s++) {
                    int px = x + dx * k + (dy != 0 ? s : 0), py = y + dy * k + (dx != 0 ? s : 0);
                    city.tiles[py * city.w + px] = PIER;
                }
            float ex = x + dx * len, ey = y + dy * len;
            // A ship iced in alongside, a crane at the root.
            float sx0 = (dy != 0 ? x + 2.5f : Math.min(x + dx, ex) + 0.5f) * T, sy0 = (dx != 0 ? y + 2.5f : Math.min(y + dy, ey) + 0.5f) * T;
            float sw = (dy != 0 ? 4 : len - 1) * T, sh = (dx != 0 ? 4 : len - 1) * T;
            if (dy != 0) city.addDecor(D_SHIP, sx0, sy0, sx0 + sw, sy0 + sh, r.nextInt(4));
            else city.addDecor(D_BOAT, sx0, sy0, sx0 + sw * 0.6f, sy0 + Math.min(sh, 12), 1);
            piers++;
        }
    }

    /** Somewhere out on the snow for a w by h place: well clear of town, the water and everything else. */
    int[] spot(int w, int h, int clearance, int tries) {
        // (At random first, then every place in turn from a random start.)
        int cols = Math.max(1, (city.w - w - 16) / 3), rows = Math.max(1, (city.h - h - 16) / 3), off = r.nextInt(cols * rows);
        for (int t = 0; t < tries + (tries > 1 ? cols * rows : 0); t++) {
            int x = 8 + r.nextInt(Math.max(1, city.w - w - 16)), y = 8 + r.nextInt(Math.max(1, city.h - h - 16));
            if (t >= tries) {
                int c = (off + t - tries) % (cols * rows);
                x = 8 + (c % cols) * 3;
                y = 8 + (c / cols) * 3;
            }
            if (x + w > town[0] - 12 && x < town[2] + 12 && y + h > town[1] - 12 && y < town[3] + 12) continue;
            if (!open(x - clearance, y - clearance, w + 2 * clearance, h + 2 * clearance)) continue;
            return new int[]{x, y};
        }
        return null;
    }

    void airstrip() {
        int aw = Math.min(130, Math.round(L * 0.42f)), ah = 26;
        int[] s = null;
        for (; aw >= 56 && s == null; aw -= 8) s = spot(aw + 6, ah + 4, 4, 300);
        if (s == null) return;
        aw += 8;
        int x0 = s[0], y0 = s[1], x1 = x0 + aw, y1 = y0 + ah;
        gen.carve(new Street(x0, y1, x1 + 5, y1 + 3, false, true));
        airportMap.airportGrounds(x0, y0, x1, y1);
        if (!roadToNetwork(x1 + 5, y1 + 1, x0, y0, aw + 5, ah + 3, city.w * 2)) roadToNetwork(x0 - 1, y1 + 1, x0, y0, aw + 5, ah + 3, city.w * 2);
        city.natureLabel(city.name + " Airstrip", (x0 + aw / 2f) * T, (y1 + 4.5f) * T, NL_LANDMARK);
        airstrip = true;
    }

    /** The radar and research station: fenced, its dome and dishes, labs and quarters, a gate onto its road. */
    void station() {
        int sw = Math.round(clamp(L * 0.08f, 22, 44)), sh = Math.round(sw * 0.75f);
        int[] s = spot(sw, sh + 6, 4, 600);
        if (s == null) return;
        int x0 = s[0], y0 = s[1], x1 = x0 + sw, y1 = y0 + sh;
        city.fill(x0, y0, sw, sh, LOT);
        for (int x = x0; x < x1; x++) {
            city.tiles[y0 * city.w + x] = FENCE;
            city.tiles[(y1 - 1) * city.w + x] = FENCE;
        }
        for (int y = y0; y < y1; y++) {
            city.tiles[y * city.w + x0] = FENCE;
            city.tiles[y * city.w + x1 - 1] = FENCE;
        }
        int gx = x0 + sw / 2 - 1;
        for (int x = gx; x < gx + 3; x++) city.tiles[(y1 - 1) * city.w + x] = LOT;
        gen.carve(new Street(gx, y1, gx + 3, y1 + 5, true, true));
        // The labs and the quarters along the back; the radar dome and the dishes out front.
        int bw = Math.max(5, sw / 3 - 2);
        city.natureLot(x0 + 2, y0 + 2, bw, 5, OFFICE, 2, 0xFFE4E8EA, 0xFFC84A30, "Research Station", 6);
        city.natureLot(x1 - 2 - bw, y0 + 2, bw, 5, OFFICE, 1, 0xFFE4E8EA, 0xFF3A5A7A, "Radar Station", -1);
        city.natureLot(x0 + 2, y1 - 7, 4, 3, WAREHOUSE, 1, 0xFF8A9096, 0xFF6A7076, "Generator Shed", -1);
        float cx = (x0 + sw / 2f) * T, cy = (y0 + sh * 0.55f) * T, rad = Math.min(sw, sh) * T * 0.16f;
        city.baseDecor.add(new float[]{BD_RADAR, cx - rad, cy - rad, cx + rad, cy + rad});
        city.baseDecor.add(new float[]{BD_DISH, (x1 - 7) * T, (y1 - 8) * T, (x1 - 3) * T, (y1 - 4) * T});
        city.baseDecor.add(new float[]{BD_DISH, (x1 - 7) * T, (y0 + 9) * T, (x1 - 3) * T, (y0 + 13) * T});
        city.baseDecor.add(new float[]{BD_MAST, (x0 + 8) * T, (y1 - 6) * T, (x0 + 9) * T, (y1 - 5) * T});
        roadToNetwork(gx + 1, y1 + 5, x0, y0, sw, sh + 4, city.w * 2);
        city.natureLabel("Radar Station", cx, (y1 + 1.5f) * T, NL_LANDMARK);
        station = true;
    }

    static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    void cabins() {
        String[] names = {"Trapper's Cabin", "Hunting Lodge", "Fishing Cabin", "Log Cabin", "Homestead", "Ranger's Cabin", "Old Cabin"};
        int want = Math.round(clamp(L / 55f, 5, 18));
        for (int t = 0; t < want * 30 && cabins < want; t++) {
            int[] s = spot(9, 7, 3, 1);
            if (s == null) continue;
            int x = s[0], y = s[1];
            int count = 1 + r.nextInt(2);
            // The cabins side by side, the road out from in front of them.
            city.natureLot(x + 1, y + 1, 4, 3, HOUSE, 1, 0xFF6A4A36, 0xFF8A6A4A, names[r.nextInt(names.length)], 2 + r.nextInt(3));
            if (count > 1) city.natureLot(x + 6, y + 1, 3, 3, HOUSE, 1, 0xFF5A4A3E, 0xFF7A5A40, names[r.nextInt(names.length)], 1 + r.nextInt(3));
            if (roadToNetwork(x + 3, y + 5, x, y, 9, 5, city.w * 2)) cabins++;
            else {
                // (Nowhere to reach: no cabin.)
                for (int j = city.buildingLots.size() - 1; j >= 0 && count > 0; j--, count--) {
                    int[] l = city.buildingLots.remove(j);
                    city.fill(l[0], l[1], l[2], l[3], GRASS);
                }
            }
        }
    }

    @Override
    void finish() {
        // Pines for trees, every one; snow on every roof.
        for (int i = 0; i < city.w * city.h; i++) if (city.tiles[i] == TREE) city.setTreeKind(i, Country.TK_PINE);
        for (int[] l : city.buildingLots) l[4] = mix(l[4], 0xFFF2F5F8, 0.7f);
    }

    @Override
    public String toString() {
        return "airstrip " + airstrip + " station " + station + " cabins " + cabins + " piers " + piers;
    }

    // ------------------------------------------------------------------ painting

    @Override
    int ground(int x, int y) {
        float n = (float) (Math.sin(x * 0.051f + y * 0.023f) * Math.sin(y * 0.047f - x * 0.017f + 0.7f));
        int step = Math.round(n * 2);
        return mix(0xFFE6ECF0, step > 0 ? 0xFFFFFFFF : 0xFFB8C8D8, Math.abs(step) * 0.12f);
    }

    @Override
    int rock(int x, int y) {
        return 0xFF8A9098;
    }

    @Override
    int water(int x, int y) {
        // Sea ice: pale, bluer in the floes' cracks.
        float n = (float) (Math.sin(x * 0.21f + y * 0.07f) * Math.sin(y * 0.19f - x * 0.05f));
        return n > 0.55f ? 0xFFB4CCDC : n < -0.6f ? 0xFFE4EEF4 : 0xFFCCDDE8;
    }

    @Override
    boolean texture(Canvas c, Paint p, byte t, int x, int y, float fx, float fy, Random rn) {
        if (t == GRASS || t == TREE || t == TRAIL) {
            // Wind-carved ripples in the snow, a blue shadow here and there.
            if (rn.nextFloat() < 0.4f) {
                p.setColor(0x30587898);
                float sx = fx + rn.nextFloat() * T, sy = fy + rn.nextFloat() * T;
                c.drawRect(sx, sy, sx + 3 + rn.nextFloat() * 6, sy + 0.8f, p);
            }
            if (rn.nextFloat() < 0.25f) {
                p.setColor(0x60FFFFFF);
                c.drawCircle(fx + rn.nextFloat() * T, fy + rn.nextFloat() * T, 1 + rn.nextFloat() * 2, p);
            }
            return true;
        }
        if (t == WATER) {
            // Cracks across the ice.
            p.setColor(0x50607E98);
            float ax = fx + rn.nextFloat() * T, ay = fy + rn.nextFloat() * T;
            if (rn.nextFloat() < 0.35f) c.drawLine(ax, ay, ax + rn.nextFloat() * 12 - 6, ay + rn.nextFloat() * 12 - 6, p);
            if (rn.nextFloat() < 0.08f) {
                p.setColor(0x70FFFFFF);
                c.drawCircle(ax, ay, 2 + rn.nextFloat() * 3, p);
            }
            return true;
        }
        return false;
    }

    @Override
    void paintOver(Canvas c, Paint p, int X0, int Y0, int X1, int Y1) {
        // Banks of ploughed snow along the roads' edges.
        Random rn = new Random();
        for (int y = Math.max(1, Y0); y < Math.min(city.h - 1, Y1); y++)
            for (int x = Math.max(1, X0); x < Math.min(city.w - 1, X1); x++) {
                byte t = city.tiles[y * city.w + x];
                if (t != GRASS && t != SIDEWALK) continue;
                for (int k = 0; k < 4; k++) {
                    int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = y + (k == 2 ? 1 : k == 3 ? -1 : 0);
                    if (city.tiles[ny * city.w + nx] != ROAD) continue;
                    rn.setSeed(x * 73856093L ^ y * 19349663L ^ k);
                    float ex = x * T + T / 2f + (nx - x) * T * 0.42f, ey = y * T + T / 2f + (ny - y) * T * 0.42f;
                    for (int q = 0; q < 3; q++) {
                        float along = (q - 1) * T * 0.33f + (rn.nextFloat() - 0.5f) * 3;
                        float bx = ex + (ny != y ? along : 0), by = ey + (nx != x ? along : 0), rad = (t == GRASS ? 3.2f : 2.2f) + rn.nextFloat() * 1.6f;
                        p.setColor(0x30304860);
                        c.drawCircle(bx + 1.2f, by + 1.4f, rad, p);
                        p.setColor(0xFFF4F7FA);
                        c.drawCircle(bx, by, rad, p);
                    }
                }
            }
    }
}
