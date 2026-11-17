package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.City.*;

/**
 * ((28.0) The builder of a map laid out its own way on the water: Bridge City (BridgeCityMap), Swamp Bayou (SwampMap)
 * and Lighthouse Coast (LighthouseMap). CityGenerator asks it at each step of building the city (layout, the districts,
 * the streets, the places, the last touches); every other map has none, and builds as before. The helpers here are the
 * pieces the three share: a town's patch of streets, a curving road (a causeway over the water), boardwalks and piers.
 */
abstract class WaterMap extends CityPart {
    WaterMap(City city) {
        super(city);
    }

    /** The builder for this city's map, or null if it's none of these. */
    static WaterMap of(City city) {
        CityConfig c = city.cfg;
        WaterMap m = c.bridgeCity() ? new BridgeCityMap(city) : c.swamp() ? new SwampMap(city) : c.lighthouse() ? new LighthouseMap(city) : null;
        if (m != null) m.wire();
        return m;
    }

    /** The country as this map paints it: the swamp's moss and murky water, the coast's cold sea. */
    static Country country(CityConfig cfg, Country c) {
        if (cfg.swamp()) return c.tinted(0xFF5C6A3A, 0xFF8E8A7E, 0xFF44583C);
        if (cfg.lighthouse()) return c.tinted(0xFF587A46, 0, 0xFF22587E);
        if (cfg.bridgeCity()) return c.tinted(0, 0, 0xFF285E8A);
        return c;
    }

    /** Where the town and the water go, before the districts (placeTown). */
    void layout() {
    }

    /** Where district k's centre goes (makeDistricts): (x, y) as worked out, or somewhere of the map's own. */
    float[] districtSpot(int k, float x, float y, Random r) {
        return new float[]{x, y};
    }

    /** What kind district k is (makeDistricts). */
    int districtType(int k, float x, float y, int was) {
        return was;
    }

    /** The ground, the water and the streets (layStreets, after the districts). */
    abstract void streets();

    /** The map's own places, on blocks no one has taken yet (placeBases, before the army base). */
    void places(boolean[] used) {
    }

    /** The last touches (finishGround). */
    void finish() {
    }

    // ------------------------------------------------------------------ shared pieces

    int W() {
        return city.w;
    }

    boolean in(int x, int y) {
        return x >= 0 && y >= 0 && x < city.w && y < city.h;
    }

    byte at(int x, int y) {
        return in(x, y) ? city.tiles[y * city.w + x] : WATER;
    }

    void set(int x, int y, byte t) {
        if (!in(x, y)) return;
        int i = y * city.w + x;
        city.tiles[i] = t;
        if (t != ROAD) {
            city.roadDir[i] = 0;
            city.mainRoad[i] = false;
        }
    }

    /** A town's streets: a ring road round x0..x1, y0..y1 and the blocks inside it split up as usual. */
    void townPatch(int x0, int y0, int x1, int y1) {
        city.fill(x0, y0, x1 - x0, y1 - y0, SIDEWALK);
        gen.carve(new Street(x0, y0, x1, y0 + 3, false, true));
        gen.carve(new Street(x0, y1 - 3, x1, y1, false, true));
        gen.carve(new Street(x0, y0, x0 + 3, y1, true, true));
        gen.carve(new Street(x1 - 3, y0, x1, y1, true, true));
        if (x1 - x0 - 6 >= 8 && y1 - y0 - 6 >= 8) gen.split(x0 + 3, y0 + 3, x1 - 3, y1 - 3, 0, -1, -1);
        else city.blocks.add(new int[]{x0 + 3, y0 + 3, x1 - 3, y1 - 3});
    }

    /**
     * A road along a smooth line of points {x, y, ...} (tiles): a causeway, raised on a grassy bank where it runs over
     * water or marsh (verge tiles either side), a bridge where it crosses open water marked in deep[].
     */
    void causeway(float[] pts, int verge, boolean[] deep) {
        int n = pts.length / 2;
        if (city.bridge == null) city.bridge = new boolean[city.w * city.h];
        if (city.bridgeKind == null) city.bridgeKind = new byte[city.w * city.h];
        // The bank first, then the road over it.
        float rb = 1.6f + verge;
        List<Integer> wetRoad = new ArrayList<Integer>();
        for (int k = 0; k < n - 1; k++) {
            float ax = pts[k * 2], ay = pts[k * 2 + 1], bx = pts[k * 2 + 2], by = pts[k * 2 + 3];
            float dx = bx - ax, dy = by - ay, len = (float) Math.sqrt(dx * dx + dy * dy);
            int steps = Math.max(1, (int) (len * 3));
            for (int s = 0; s <= steps; s++) {
                float px = ax + dx * s / steps, py = ay + dy * s / steps;
                for (int y = City.fl(py - rb); y <= City.fl(py + rb); y++)
                    for (int x = City.fl(px - rb); x <= City.fl(px + rb); x++) {
                        if (!in(x, y)) continue;
                        float ox = x + 0.5f - px, oy = y + 0.5f - py;
                        float d2 = ox * ox + oy * oy;
                        if (d2 > rb * rb) continue;
                        int i = y * city.w + x;
                        byte t = city.tiles[i];
                        boolean isDeep = deep != null && deep[i];
                        if (d2 <= 1.6f * 1.6f) {
                            if (isDeep && t == WATER) wetRoad.add(i);
                            continue;
                        }
                        if (isDeep) continue;
                        if (t == WATER || t == TREE || t == PIER || t == SAND || t == ROCK) city.tiles[i] = GRASS;
                    }
            }
        }
        landGen.curveRoad(pts, false);
        for (int i : wetRoad)
            if (city.tiles[i] == ROAD) {
                city.bridge[i] = true;
                city.bridgeKind[i] = BK_CONCRETE;
            }
    }

    /** A smooth line from (ax, ay) to (bx, by), wandering to one side and back by up to wob tiles. */
    static float[] wander(float ax, float ay, float bx, float by, float wob, Random r) {
        float dx = bx - ax, dy = by - ay, len = (float) Math.sqrt(dx * dx + dy * dy);
        int n = Math.max(2, (int) (len / 6));
        float nx = -dy / Math.max(1, len), ny = dx / Math.max(1, len);
        double p1 = r.nextDouble() * 6.28, f1 = 1 + r.nextInt(2);
        float a1 = wob * (r.nextFloat() * 2 - 1), a2 = wob * 0.35f * (r.nextFloat() * 2 - 1);
        float[] pts = new float[(n + 1) * 2];
        for (int k = 0; k <= n; k++) {
            float t = k / (float) n;
            float env = (float) Math.sin(Math.PI * t);
            float off = env * (a1 * (float) Math.sin(Math.PI * t * f1) + a2 * (float) Math.sin(t * 9 + p1));
            pts[k * 2] = ax + dx * t + nx * off;
            pts[k * 2 + 1] = ay + dy * t + ny * off;
        }
        return pts;
    }

    /** A boardwalk two tiles wide (pier planks: people walk it, no car can) along a row or a column. */
    void boardwalk(int x0, int y0, int x1, int y1) {
        int ax = Math.min(x0, x1), bx = Math.max(x0, x1), ay = Math.min(y0, y1), by = Math.max(y0, y1);
        for (int y = ay; y <= by + (ax == bx ? 0 : 1); y++)
            for (int x = ax; x <= bx + (ay == by ? 0 : 1); x++) {
                if (!in(x, y)) continue;
                byte t = at(x, y);
                if (t == WATER || t == GRASS || t == TREE || t == SAND) set(x, y, PIER);
            }
    }

    /** Whether every tile of x..x+w, y..y+h is one of the given kinds. */
    boolean all(int x, int y, int w, int h, byte... kinds) {
        for (int j = y; j < y + h; j++)
            for (int i = x; i < x + w; i++) {
                if (!in(i, j)) return false;
                byte t = at(i, j);
                boolean ok = false;
                for (byte k : kinds) if (t == k) ok = true;
                if (!ok || city.buildingAt.get(j * city.w + i) >= 0) return false;
            }
        return true;
    }

    /** Smooth value noise, 0 to 1 (seeded by s). */
    static float noise(float x, float y, long s) {
        int x0 = City.fl(x), y0 = City.fl(y);
        float fx = x - x0, fy = y - y0;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        float a = lat(x0, y0, s), b = lat(x0 + 1, y0, s), c = lat(x0, y0 + 1, s), d = lat(x0 + 1, y0 + 1, s);
        return a + (b - a) * fx + (c - a) * fy + (a - b - c + d) * fx * fy;
    }

    static float lat(int x, int y, long s) {
        long h = x * 374761393L + y * 668265263L + s * 2246822519L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h ^= h >>> 16;
        return (h & 0xFFFF) / 65535f;
    }

    /** Noise with detail: four octaves. */
    static float fbm(float x, float y, long s) {
        float sum = 0, amp = 0.5f, f = 1, norm = 0;
        for (int o = 0; o < 4; o++) {
            sum += amp * noise(x * f, y * f, s + o * 101);
            norm += amp;
            amp *= 0.5f;
            f *= 2.03f;
        }
        return sum / norm;
    }

    /** Land the town can't be walked to from (start: a tile in town) goes back to water, so nobody's stranded. */
    void dropStranded(int start) {
        int n = city.w * city.h;
        boolean[] seen = new boolean[n];
        int[] q = new int[n];
        int head = 0, tail = 0;
        q[tail++] = start;
        seen[start] = true;
        while (head < tail) {
            int c = q[head++], x = c % city.w, y = c / city.w;
            for (int k = 0; k < 4; k++) {
                int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = y + (k == 2 ? 1 : k == 3 ? -1 : 0);
                if (!in(nx, ny)) continue;
                int j = ny * city.w + nx;
                if (seen[j] || (city.tiles[j] == WATER && (city.bridge == null || !city.bridge[j]))) continue;
                seen[j] = true;
                q[tail++] = j;
            }
        }
        for (int i = 0; i < n; i++)
            if (!seen[i] && city.tiles[i] != WATER) {
                city.tiles[i] = WATER;
                city.roadDir[i] = 0;
                city.mainRoad[i] = false;
            }
    }

    /** Districts whose middle is out in the wilds (not on ground the town takes): given a name of the wilds. */
    void nameWild(boolean[] town, String[] ends) {
        Random r = new Random(city.cfg.seed * 401L + 28);
        for (District d : city.districts) {
            int x = (int) d.x, y = (int) d.y;
            if (!in(x, y) || town[y * city.w + x]) continue;
            String w = city.country.districtWords[r.nextInt(city.country.districtWords.length)];
            String name = w + ends[r.nextInt(ends.length)];
            for (District o : city.districts) if (o != d && o.name.equals(name)) name = w + " " + ends[0].trim();
            d.name = name;
            d.type = DT_PARKSIDE;
        }
    }
}
