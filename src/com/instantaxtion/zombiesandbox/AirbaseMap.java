package com.instantaxtion.zombiesandbox;

import java.util.Random;

import static com.instantaxtion.zombiesandbox.City.*;

/**
 * ((28.0) The Military Airbase: one great air base along the top or the bottom of the town - its runways, taxiways and
 * apron, the hangars, the control tower, the fuel farm, the heliport and motor pool, the barracks rows by the main gate,
 * all fenced - and the small town at the gate (and, now and then, base housing round one end). The town's streets are
 * laid out as anywhere; the base is laid out here. Everything it changes is for this map alone.
 */
final class AirbaseMap extends CityPart {
    AirbaseMap(City city) {
        super(city);
    }

    /** Whether the base is along the top of the town (the town below it), and its rectangle (tiles, x1/y1 exclusive). */
    boolean up;
    int ax0, ax1, baseY0, baseY1, depth;
    /** The main gate's road (its first column, three wide), the service gate's, and the inner street's depth. */
    int gateX = -1, gate2X = -1, cInner;
    /** Depths (from the fence facing town) where the support row starts and the airfield starts. */
    int cSupport, cField;

    static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Tiles {x0, y0, x1, y1} for x0..x1 and depths c0..c1 into the base from the fence facing town. */
    int[] rc(int x0, int c0, int x1, int c1) {
        return up ? new int[]{x0, baseY1 - c1, x1, baseY1 - c0} : new int[]{x0, baseY0 + c0, x1, baseY0 + c1};
    }

    /** The tile row at depth c. */
    int yAt(int c) {
        return up ? baseY1 - 1 - c : baseY0 + c;
    }

    /** Lays t over x0..x1, c0..c1, leaving the base's own roads be. */
    void fillC(int x0, int c0, int x1, int c1, byte t) {
        int[] q = rc(x0, c0, x1, c1);
        for (int y = Math.max(0, q[1]); y < Math.min(city.h, q[3]); y++)
            for (int x = Math.max(0, q[0]); x < Math.min(city.w, q[2]); x++)
                if (city.tiles[y * city.w + x] != ROAD) city.tiles[y * city.w + x] = t;
    }

    void road(int x0, int c0, int x1, int c1, boolean vertical, boolean main) {
        int[] q = rc(x0, c0, x1, c1);
        gen.carve(new Street(q[0], q[1], q[2], q[3], vertical, main));
    }

    /**
     * The base's ground kept paved over until its places go up (so nothing out in the country takes it meanwhile), the
     * road along its fence on the town's side, the roads in through its gates, and the town's streets on the rest.
     */
    void layout(int bx0, int by0, int bx1, int by1, int in) {
        Random r = new Random(city.cfg.seed * 6151L + 22);
        int W = bx1 - bx0, H = by1 - by0;
        up = r.nextBoolean();
        depth = clamp(Math.round(H * (0.54f + 0.08f * r.nextFloat())), 44, 340);
        depth = Math.min(depth, H - 36);
        // Now and then base housing round one end of it, the base shorter for it.
        int end = W >= 230 && r.nextFloat() < 0.6f ? (r.nextBoolean() ? -1 : 1) : 0;
        int E = end != 0 ? Math.round(W * (0.14f + 0.07f * r.nextFloat())) : 0;
        ax0 = bx0 + in + (end < 0 ? E + 5 : 0);
        ax1 = bx1 - in - (end > 0 ? E + 5 : 0);
        baseY0 = up ? by0 + in : by1 - in - depth;
        baseY1 = baseY0 + depth;
        city.fill(ax0, baseY0, ax1 - ax0, depth, SIDEWALK);
        // The perimeter road, the whole width of the town, along the fence on the town's side.
        int py0 = up ? baseY1 : baseY0 - 5;
        gen.carve(new Street(bx0 + in, py0, bx1 - in, py0 + 5, false, true));
        // The road down the base's end, past the housing.
        int ex0 = end < 0 ? ax0 - 5 : ax1;
        if (end != 0) gen.carve(new Street(ex0, up ? baseY0 : py0, ex0 + 5, up ? py0 + 5 : baseY1, true, true));
        // The depths: the barracks by the fence, the inner street, the support row (hangars, tower, fuel), the airfield.
        int L = ax1 - ax0;
        cInner = clamp(Math.round(depth * 0.27f), 13, 64);
        cSupport = cInner + 3;
        cField = cSupport + clamp(Math.round(depth * 0.19f), 10, 46);
        // The main gate, somewhere along the middle; its road in to the inner street, and the avenue out through town.
        gateX = ax0 + Math.round(L * (0.32f + 0.36f * r.nextFloat())) - 1;
        road(gateX, -1, gateX + 3, cInner, true, true);
        road(ax0 + 3, cInner, ax1 - 3, cInner + 3, false, false);
        // A service gate near one end.
        gate2X = r.nextBoolean() ? ax0 + clamp(L / 9, 8, 60) : ax1 - clamp(L / 9, 8, 60) - 3;
        if (Math.abs(gate2X - gateX) > 20) road(gate2X, -1, gate2X + 3, cInner, true, false);
        else gate2X = -1;
        // The town: an avenue straight out from the gate, and the streets either side of it.
        int ty0 = up ? py0 + 5 : by0 + in, ty1 = up ? by1 - in : py0;
        int vx = gateX - 1;
        if (ty1 - ty0 >= 8) gen.carve(new Street(vx, ty0, vx + 5, ty1, true, true));
        splitRegion(bx0 + in, ty0, vx, ty1);
        splitRegion(vx + 5, ty0, bx1 - in, ty1);
        if (end < 0) splitRegion(bx0 + in, baseY0, ex0, baseY1);
        if (end > 0) splitRegion(ex0 + 5, baseY0, bx1 - in, baseY1);
        nameDistricts();
    }

    private void splitRegion(int x0, int y0, int x1, int y1) {
        if (x1 - x0 >= 6 && y1 - y0 >= 6) gen.split(x0, y0, x1, y1, 1, -1, -1);
    }

    /** The districts out on the base take the base's names, not a town's. */
    private void nameDistricts() {
        String[] names = {"Flight Line", "Airfield", "Hangar Row", "North Field", "South Field", "East Ramp", "West Ramp"};
        int k = 0;
        for (District d : city.districts) {
            if (d.x < ax0 || d.x >= ax1 || d.y < baseY0 || d.y >= baseY1) continue;
            d.name = names[Math.min(k++, names.length - 1)];
            if (k > names.length) d.unnamed = true;
            d.theme = null;
        }
    }

    /**
     * Everything on the base, once the country round town is laid out: the fence and its gates, the barracks rows,
     * the support row, the airfield; the base itself (its garrison, guards, helicopters and trucks); and the runway for
     * the transports and the evacuation flights.
     */
    void places(boolean[] used) {
        if (!city.cfg.airbase() || gateX < 0) return;
        if (gen.lmRnd == null) {
            gen.lmRnd = new Random(city.cfg.seed * 7919L + 35);
            gen.lmCount = new int[Landmarks.all().size()];
        }
        Random r = new Random(city.cfg.seed * 4129L + 7);
        int L = ax1 - ax0;
        // The ground: paved round the barracks and the support row, grass out on the airfield; the fence round it all.
        fillC(ax0, 0, ax1, cField, BASE);
        fillC(ax0, cField, ax1, depth, GRASS);
        for (int x = ax0; x < ax1; x++)
            for (int c : new int[]{0, depth - 1}) {
                int i = yAt(c) * city.w + x;
                if (city.tiles[i] != ROAD) city.tiles[i] = FENCE;
            }
        for (int c = 0; c < depth; c++)
            for (int x : new int[]{ax0, ax1 - 1}) {
                int i = yAt(c) * city.w + x;
                if (city.tiles[i] != ROAD) city.tiles[i] = FENCE;
            }
        barracks(r);
        support(r, L);
        airfield(r, L);
        facility();
    }

    /** The barracks: rows of them either side of the gate road, a parade ground and a sports field by the inner street. */
    private void barracks(Random r) {
        int rows = Math.max(1, Math.min(6, (cInner - 3) / 6));
        int[][] spans = {{ax0 + 3, gateX - 3}, {gateX + 6, ax1 - 3}};
        for (int[] sp : spans) {
            for (int row = 0; row < rows; row++) {
                int c0 = 2 + row * 6;
                int x = sp[0];
                while (x + 8 <= sp[1]) {
                    int lw = Math.min(sp[1] - x, 14 + r.nextInt(9));
                    if (gate2X >= 0 && x < gate2X + 5 && x + lw > gate2X - 2) {
                        x = gate2X + 5;
                        continue;
                    }
                    if (lw >= 8) {
                        int[] q = rc(x, c0, x + lw, c0 + 4);
                        lotGen.addFacilityLot(q[0], q[1], q[2] - q[0], q[3] - q[1], BARRACKS, 1 + (row == 0 ? 0 : r.nextInt(2)));
                    }
                    x += lw + 3;
                }
            }
        }
        // Between the last row and the inner street, by the gate: the parade ground with its flag, and a sports field.
        int c0 = 2 + rows * 6, c1 = cInner - 1;
        if (c1 - c0 >= 5) {
            int[] pg = rc(gateX + 6, c0, Math.min(ax1 - 3, gateX + 6 + Math.max(16, (c1 - c0) * 3)), c1);
            city.baseDecor.add(new float[]{BD_HARDSTAND, pg[0] * T, pg[1] * T, pg[2] * T, pg[3] * T});
            float fx = (pg[0] + pg[2]) / 2f * T, fy = (pg[1] + pg[3]) / 2f * T;
            city.baseDecor.add(new float[]{BD_FLAG, fx - 3, fy - 3, fx + 3, fy + 3});
            int sx1 = gateX - 3, sx0 = Math.max(ax0 + 3, sx1 - Math.max(20, (c1 - c0) * 2));
            if (sx1 - sx0 >= 12) {
                int[] sf = rc(sx0, c0, sx1, c1);
                city.fill(sf[0], sf[1], sf[2] - sf[0], sf[3] - sf[1], GRASS);
                city.addDecor(D_FIELD, sf[0] * T + 3, sf[1] * T + 3, sf[2] * T - 3, sf[3] * T - 3, 0);
            }
        }
    }

    /**
     * The support row between the inner street and the airfield, left to right in a seeded order: the fuel farm, the
     * motor pool, the hangars, the control tower and the headquarters, the heliport.
     */
    private void support(Random r, int L) {
        int c0 = cSupport + 1, c1 = cField - 1, sd = c1 - c0;
        int x = ax0 + 3, xEnd = ax1 - 3;
        boolean fuelFirst = r.nextBoolean();
        // The tower about the middle, the rest round it.
        int towerAt = ax0 + L / 2 - 8;
        int hangarW = clamp(Math.round(sd * 1.5f), 14, 44);
        boolean tower = false, fuel = sd >= 14;
        int fuelW = clamp(sd * 2, 22, 48) + 3, heliW = Math.min(40, Math.max(16, L / 8)) + 3;
        if (fuelFirst && fuel) x = fuelFarm(x, c0, c1, r);
        x = motorPool(x, c0, c1, r);
        // (Room kept at the far end for the heliport, and the fuel farm if it's there.)
        int reserve = heliW + (!fuelFirst && fuel ? fuelW : 0);
        while (true) {
            if (!tower && x + hangarW >= towerAt && x + 19 <= xEnd - reserve) {
                tower = true;
                x = controlTower(x, c0, c1);
                continue;
            }
            if (x + hangarW > xEnd - reserve) break;
            hangar(rc(x, c0, x + hangarW, c1), r);
            x += hangarW + 3;
        }
        if (!tower && x + 19 <= xEnd - reserve) x = controlTower(x, c0, c1);
        if (!fuelFirst && fuel && x + fuelW <= xEnd - heliW + 3) x = fuelFarm(x, c0, c1, r);
        if (x + 16 <= xEnd) heliport(x, c0, c1, xEnd);
    }

    /** A hangar: the building at the back, its doors open onto the airfield, the apron in front. */
    private void hangar(int[] q, Random r) {
        int w = q[2] - q[0], h = q[3] - q[1];
        int bd = Math.max(5, h * 2 / 3);
        int y = up ? q[3] - bd : q[1];
        plainLot(q[0] + 1, y, w - 2, bd, WAREHOUSE, 3, 0xFF8A9096, 0xFF9AA0A6);
        name("Aircraft Hangar");
        int ay0 = up ? q[1] : q[1] + bd, ay1 = up ? q[3] - bd : q[3];
        if (ay1 - ay0 >= 2) city.baseDecor.add(new float[]{BD_HARDSTAND, q[0] * T, ay0 * T, q[2] * T, ay1 * T});
        float jx = (q[0] + q[2]) / 2f * T, jy = (ay0 + ay1) / 2f * T;
        if (ay1 - ay0 >= 3) city.baseDecor.add(new float[]{BD_JET, jx - 14, jy - 11, jx + 14, jy + 11});
    }

    /** A building of the base's own plain look (none of the town's varieties). */
    private void plainLot(int x, int y, int lw, int lh, int kind, int floors, int roof, int wall) {
        if (lw < 1 || lh < 1) return;
        city.fill(x, y, lw, lh, BUILDING);
        city.buildingLots.add(new int[]{x, y, lw, lh, roof, city.rnd.nextInt(100000), kind, floors, wall, -1});
    }

    private void name(String n) {
        city.lotNames.put(city.buildingLots.get(city.buildingLots.size() - 1), n);
    }

    private int controlTower(int x, int c0, int c1) {
        int w = 16;
        int[] q = rc(x, c0, x + w, c1);
        int h = q[3] - q[1];
        int ty = up ? q[1] + 1 : q[3] - 4;
        lotGen.addFacilityLot(q[0] + 6, ty, 3, 3, TOWER, 7);
        name("Control Tower");
        int oh = Math.min(6, h - 5);
        if (oh >= 3) {
            int oy = up ? q[3] - oh - 1 : q[1] + 1;
            plainLot(q[0] + 1, oy, w - 2, oh, OFFICE, 2, 0xFF6A7066, 0xFFC8C4B4);
            name("Wing Headquarters");
        }
        return x + w + 3;
    }

    private int fuelFarm(int x, int c0, int c1, Random r) {
        int w = clamp((c1 - c0) * 2, 22, 48);
        int[] q = rc(x, c0, x + w, c1);
        Landmarks.Def d = CityGenerator.landmarkNamed("Fuel Farm");
        if (d != null && gen.placeParcel(q, "Fuel Farm")) {
            for (int j = q[1]; j < q[3]; j++)
                for (int i = q[0]; i < q[2]; i++) if (city.tiles[j * city.w + i] == LOT) city.tiles[j * city.w + i] = BASE;
        } else {
            float fs = Math.min(34, (q[3] - q[1]) * T * 0.6f);
            for (int k = 0; k < 3; k++) {
                float fx = q[0] * T + 6 + k * (fs + 6);
                if (fx + fs < q[2] * T) city.baseDecor.add(new float[]{BD_FUEL, fx, q[1] * T + 6, fx + fs, q[1] * T + 6 + fs});
            }
        }
        return x + w + 3;
    }

    /** The motor pool: a hardstand with the army's trucks in rows. */
    private int motorPool(int x, int c0, int c1, Random r) {
        int w = clamp((c1 - c0) * 2, 16, 40);
        int[] q = rc(x, c0, x + w, c1);
        city.baseDecor.add(new float[]{BD_HARDSTAND, q[0] * T, q[1] * T, q[2] * T, q[3] * T});
        int shed = Math.min(5, (q[3] - q[1]) / 3);
        if (shed >= 3) {
            plainLot(q[0] + 1, up ? q[3] - shed : q[1], w - 2, shed, WAREHOUSE, 1, 0xFF5E6A48, 0xFF7C8456);
            name("Motor Pool");
        }
        int rowsY0 = up ? q[1] + 1 : q[1] + shed + 1, rowsY1 = up ? q[3] - shed - 1 : q[3] - 1;
        for (int j = rowsY0; j < rowsY1; j += 3)
            for (int i = q[0] + 1; i < q[2] - 1; i += 2) {
                int t = j * city.w + i;
                if (city.tiles[t] != BASE || r.nextFloat() < 0.3f) continue;
                city.tiles[t] = CAR;
                city.carKind.set(t, (byte) 2);
            }
        return x + w + 3;
    }

    /** The heliport: a hardstand with a row of pads, the army's helicopters on them. */
    private int heliport(int x, int c0, int c1, int xEnd) {
        int w = Math.min(xEnd - x, Math.max(40, (ax1 - ax0) / 8));
        int[] q = rc(x, c0, x + w, c1);
        city.baseDecor.add(new float[]{BD_HARDSTAND, q[0] * T, q[1] * T, q[2] * T, q[3] * T});
        float py = (q[1] + q[3]) / 2f * T;
        int pads = 0;
        for (float px = q[0] * T + 40; px < q[2] * T - 30 && pads < 4; px += 80, pads++) city.helipads.add(new float[]{px, py});
        return x + w + 3;
    }

    /**
     * The airfield: the apron along the support row, the taxiway, one runway or two, the links between them, all paved
     * on the grass; the weapons store out beyond on the bigger bases.
     */
    private void airfield(Random r, int L) {
        int S = depth - 1 - cField;
        int[] rows = LandmarkArt28.airfieldRows(S);
        int ap1 = rows[1], t0 = rows[2], t1 = rows[3], a0 = rows[4], a1 = rows[5], b0 = rows[6], b1 = rows[7];
        int x0 = ax0 + 1, x1 = ax1 - 1;
        Landmarks.Def def = LandmarkArt28.airfield(up);
        if (def != null) {
            int[] q = rc(x0, cField, x1, depth - 1);
            lotGen.placeLandmark(def, q[0], q[1], q[2] - q[0], q[3] - q[1]);
        }
        fillC(x0, cField, x1, depth - 1, GRASS);
        fillC(x0, cField, x1, cField + ap1, BASE);
        fillC(x0 + 2, cField + t0, x1 - 2, cField + t1, BASE);
        int ra0 = x0 + Math.round(L * 0.03f), ra1 = x1 - Math.round(L * 0.03f);
        fillC(ra0, cField + a0, ra1, cField + a1, BASE);
        for (float f : LandmarkArt28.LINKS_A) {
            int lx = x0 + Math.round((x1 - x0) * f) - 1;
            fillC(lx, cField + t1, lx + 3, cField + a0, BASE);
        }
        if (b0 >= 0) {
            int rb0 = x0 + Math.round(L * 0.12f), rb1 = x1 - Math.round(L * 0.12f);
            fillC(rb0, cField + b0, rb1, cField + b1, BASE);
            for (float f : LandmarkArt28.LINKS_B) {
                int lx = x0 + Math.round((x1 - x0) * f) - 1;
                fillC(lx, cField + a1, lx + 3, cField + b0, BASE);
            }
        }
        // The aircraft use the main runway; the transports and airliners park at the apron by the tower.
        int[] rw = rc(ra0, cField + a0, ra1, cField + a1);
        city.runway = new float[]{rw[0] * T, rw[1] * T, rw[2] * T, rw[3] * T};
        int[] ap = rc(ax0 + L / 2 - 6, cField, ax0 + L / 2 + 6, cField + ap1);
        city.apron = new float[]{(ap[0] + ap[2]) / 2f * T, up ? (ap[1] + 1) * T : (ap[3] - 5) * T};
        // The weapons store: rows of earth-covered bunkers out past the runways.
        int far = (b0 >= 0 ? b1 : a1) + 8, end = S - 4;
        if (end - far >= 12) {
            int bxs = x0 + Math.round(L * (r.nextBoolean() ? 0.06f : 0.62f)), bxe = bxs + Math.round(L * 0.3f);
            for (int c = far; c + 4 <= end && c < far + 40; c += 8)
                for (int bx = bxs; bx + 5 <= bxe; bx += 9) {
                    int[] q = rc(bx, cField + c, bx + 5, cField + c + 4);
                    plainLot(q[0], q[1], q[2] - q[0], q[3] - q[1], WAREHOUSE, 1, 0xFF6A7A4A, 0xFF7A8A5A);
                    name("Ammunition Bunker");
                }
        }
    }

    /** The base: its middle by the barracks, the main gate, the guards there and the checkpoint just outside. */
    private void facility() {
        int L = ax1 - ax0;
        float gx = (gateX + 1.5f) * T, gy = (yAt(1) + 0.5f) * T;
        int[] mid = rc(ax0 + L / 2 - 2, cInner / 2, ax0 + L / 2 + 2, cInner / 2 + 1);
        float cx = (gateX + 1.5f) * T, cy = (mid[1] + 0.5f) * T;
        float[] c = city.findWalkable(cx, cy);
        if (c == null) c = new float[]{cx, cy};
        String bn = city.country.bases[city.rnd.nextInt(city.country.bases.length)];
        Facility base = new Facility(FACILITY_BASE, c[0], c[1], Math.min(320, Math.max(110, cInner * T * 1.3f)), gx, gy,
                bn + " (" + BASE_TYPES[BT_AIR] + ")");
        base.baseType = BT_AIR;
        float in = up ? -T * 2.5f : T * 2.5f;
        for (int k = -1; k <= 1; k += 2) {
            float[] p = city.findWalkable(gx + k * 30, gy + in);
            if (p != null) base.posts.add(p);
        }
        if (gate2X >= 0) {
            float[] p = city.findWalkable((gate2X + 1.5f) * T + 26, gy + in);
            if (p != null) base.posts.add(p);
        }
        city.facilities.add(base);
        // The checkpoint on the road in, just inside the gate.
        float ang = (float) (up ? -Math.PI / 2 : Math.PI / 2);
        float px = gx, py = (yAt(4) + 0.5f) * T;
        city.checkpoints.add(new float[]{px, py, ang});
        for (int sd = -1; sd <= 1; sd += 2) {
            float[] q = city.findWalkable(px + sd * 2.4f * T, py);
            if (q != null) city.checkpointPosts.add(q);
        }
    }
}
