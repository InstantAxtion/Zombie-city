package com.instantaxtion.zombiesandbox;

import android.graphics.Canvas;
import android.graphics.Paint;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.City.*;

/**
 * ((28.0) Highway Crossroads: open country (few trees, no mountains, long views) where two highways cross. The first
 * is the map's highway as on any country map; the second, a divided highway of its own, crosses over it at a big
 * interchange (the bridge over, slip roads on and off both ways). Along the second, out from the interchange, the
 * roadside stops: a truck stop, motels, fuel, repair garages and a weigh station. The small town sits off in one
 * corner of the map, and farms and hamlets are scattered over the rest.
 */
final class CrossroadsMap extends RegionMap {
    CrossroadsMap(City city) {
        super(city);
    }

    final Random r = new Random(city.cfg.seed * 4409L + 20);
    /** Which corner the town is in. */
    int sx, sy;
    boolean hwyVertical;
    /** The second highway: its streets, where it runs (k0, k0 + W across it), and the roadside stops' parcels. */
    final List<Street> route = new ArrayList<Street>();
    int k2 = -1, W = 7;
    final List<int[]> stops = new ArrayList<int[]>();
    static final int TRUCK = 0, MOTEL = 1, GARAGE = 2, FUEL = 3, WEIGH = 4;
    static final int[] ORDER = {TRUCK, MOTEL, FUEL, GARAGE, WEIGH, MOTEL, GARAGE, FUEL, MOTEL, GARAGE};
    /** How many of each went up, for the test. */
    final int[] built = new int[5];
    boolean interchange;

    @Override
    int[] townRect(int bx0, int by0, int bx1, int by1) {
        sx = r.nextBoolean() ? 1 : -1;
        sy = r.nextBoolean() ? 1 : -1;
        hwyVertical = r.nextBoolean();
        int hw = (bx1 - bx0) / 2, hh = (by1 - by0) / 2;
        int cx = Math.round(city.w / 2f + sx * city.w * 0.19f), cy = Math.round(city.h / 2f + sy * city.h * 0.19f);
        cx = Math.max(hw + 12, Math.min(city.w - hw - 12, cx));
        cy = Math.max(hh + 12, Math.min(city.h - hh - 12, cy));
        gen.shiftX = cx - city.w / 2;
        gen.shiftY = cy - city.h / 2;
        // The highway on the open side, across the map the way chosen.
        gen.wildSide = hwyVertical ? (sx > 0 ? 1 : 0) : (sy > 0 ? 3 : 2);
        return new int[]{cx - hw, cy - hh, cx + hw, cy + hh};
    }

    @Override
    void afterHighway(int bx0, int by0, int bx1, int by1) {
        if (city.hwyAxis < 0) return;
        boolean ew = city.hwyAxis == 0;
        int at = city.hwyAt, len = ew ? city.h : city.w, len2 = ew ? city.w : city.h;
        // Where it crosses: out in the open, between the town and the edge of the map.
        int lo = ew ? bx0 : by0, hi = ew ? bx1 : by1, s = ew ? sx : sy;
        int k = s > 0 ? lo / 2 - W / 2 : hi + (len2 - hi) / 2 - W / 2;
        k = Math.max(Math.min(48, len2 / 3), Math.min(len2 - Math.min(48, len2 / 3) - W, k));
        k2 = k;
        int G = 6;
        // The far side (before the highway) and the near side (after it), the way across the map.
        if (at - 1 > 2) addRoute(0, at - 1, ew);
        if (at + 7 + G < len - 2) addRoute(at + 7 + G, len, ew);
        if (city.cfg.civic >= 16 && at >= 7) {
            roadGen.interchange(new int[]{k2, W, G, 0}, ew, at, -1, at + 7, -1);
            interchange = true;
        }
        reserveStops(at, len, ew);
    }

    void addRoute(int r0, int r1, boolean ew) {
        Street st = ew ? new Street(k2, r0, k2 + W, r1, true, true) : new Street(r0, k2, r1, k2 + W, false, true);
        gen.carve(st);
        route.add(st);
        city.streets.add(st);
    }

    /** Parcels along the second highway, out from the interchange, both sides of it, kept open for the stops. */
    void reserveStops(int at, int len, boolean ew) {
        int want = Math.min(ORDER.length, 4 + city.w / 150);
        int next = 0;
        List<int[]> spots = new ArrayList<int[]>();
        for (int d = 18; d < len; d += 4)
            for (int dir = -1; dir <= 1; dir += 2) {
                int rr = dir < 0 ? at - 8 - d : at + 18 + d;
                spots.add(new int[]{rr, dir, d});
            }
        java.util.Collections.sort(spots, new java.util.Comparator<int[]>() {
            public int compare(int[] a, int[] b) {
                return a[2] - b[2];
            }
        });
        for (int[] sp : spots) {
            if (next >= want) break;
            int type = ORDER[next];
            int lr = type == TRUCK ? 32 : type == MOTEL ? 24 : type == WEIGH ? 20 : 16, dk = type == TRUCK ? 26 : type == MOTEL ? 16 : 13;
            Landmarks.Def m = CityGenerator.landmarkNamed("Motel");
            if (type == MOTEL && m != null) {
                lr = Math.max(lr, m.minL + 4);
                dk = Math.max(dk, m.minS + 4);
            }
            int r0 = sp[1] < 0 ? sp[0] - lr : sp[0];
            if (r0 < 26 || r0 + lr > len - 26) continue;
            for (int lat = -1; lat <= 1; lat += 2) {
                int k0 = lat < 0 ? k2 - dk : k2 + W;
                int x0 = ew ? k0 : r0, y0 = ew ? r0 : k0, w = ew ? dk : lr, h = ew ? lr : dk;
                // (Room round it, so the farms and lanes keep off.)
                int gx0 = ew ? (lat < 0 ? x0 - 3 : x0) : x0 - 3, gy0 = ew ? y0 - 3 : (lat < 0 ? y0 - 3 : y0);
                if (!open(gx0, gy0, w + 3, h + 3)) continue;
                boolean clash = false;
                for (int[] o : stops) if (x0 < o[2] + 6 && o[0] < x0 + w + 6 && y0 < o[3] + 6 && o[1] < y0 + h + 6) clash = true;
                if (clash) continue;
                city.fill(x0, y0, w, h, SIDEWALK);
                stops.add(new int[]{x0, y0, x0 + w, y0 + h, type, ew ? (lat < 0 ? 0 : 1) : (lat < 0 ? 2 : 3)});
                next++;
                break;
            }
        }
    }

    @Override
    void places(boolean[] used) {
        mainBase(used);
        if (gen.lmRnd == null) {
            gen.lmRnd = new Random(city.cfg.seed * 7919L + 35);
            gen.lmCount = new int[Landmarks.all().size()];
        }
        String[] repair = {"Truck & Diesel Repair", "Auto Repair Garage", "Tire & Brake Shop", "Towing & Recovery"};
        int rep = 0;
        for (int[] s : stops) {
            int ix = s[0] + 1, iy = s[1] + 1, iw = s[2] - s[0] - 2, ih = s[3] - s[1] - 2;
            // The side the road is on: 0 right, 1 left, 2 below, 3 above.
            int face = s[5];
            gen.curDistrict = DT_INDUSTRIAL;
            switch (s[4]) {
                case MOTEL:
                    if (gen.placeParcel(new int[]{ix, iy, ix + iw, iy + ih}, "Motel")) built[MOTEL]++;
                    break;
                case FUEL:
                    lotGen.gasStation(ix, iy, iw, ih);
                    built[FUEL]++;
                    break;
                case TRUCK: {
                    // The fuel canopy by the road, the truck stop's building and its lot for the rigs behind.
                    int[] f = strip(ix, iy, iw, ih, face, 13);
                    lotGen.gasStation(f[0], f[1], f[2], f[3]);
                    int[] b = strip(ix, iy, iw, ih, face, -1);
                    int[] rest = shrinkFrom(b, face, 14);
                    city.fill(rest[0], rest[1], rest[2], rest[3], LOT);
                    int bw = Math.min(10, rest[2] - 2), bh = Math.min(6, rest[3] - 2);
                    if (bw >= 4 && bh >= 3) city.natureLot(rest[0] + 1, rest[1] + 1, bw, bh, OFFICE, 1, 0xFF8A3A2E, 0xFFD8C8A8, "Truck Stop", -1);
                    for (int t = 0; t < 3; t++) city.addDecor(D_BAY, (rest[0] + rest[2] - 6) * T, (rest[1] + 1 + t * 3) * T, (rest[0] + rest[2] - 1) * T, (rest[1] + 3 + t * 3) * T, 0);
                    built[TRUCK]++;
                    break;
                }
                case GARAGE: {
                    city.fill(ix, iy, iw, ih, LOT);
                    int[] b = shrinkFrom(new int[]{ix, iy, iw, ih}, face, 5);
                    city.natureLot(b[0] + 1, b[1] + 1, Math.max(3, b[2] - 2), Math.max(3, b[3] - 2), WAREHOUSE, 1, 0xFF5A6470, 0xFFB8B0A0,
                            repair[rep++ % repair.length], -1);
                    built[GARAGE]++;
                    break;
                }
                default: {
                    // The weigh station: the scale pad along the road, the booth beside it.
                    int[] pad = strip(ix, iy, iw, ih, face, 4);
                    city.fill(pad[0], pad[1], pad[2], pad[3], LOT);
                    int[] b = shrinkFrom(new int[]{ix, iy, iw, ih}, face, 6);
                    city.natureLot(b[0] + 2, b[1] + 1, 5, 4, OFFICE, 1, 0xFF4A5A6A, 0xFFE0D8C8, "Weigh Station", -1);
                    built[WEIGH]++;
                    break;
                }
            }
        }
    }

    /** The strip of a parcel d deep along the road side (all of it if d < 0). */
    static int[] strip(int x, int y, int w, int h, int face, int d) {
        if (d < 0) return new int[]{x, y, w, h};
        switch (face) {
            case 0: return new int[]{x + w - d, y, d, h};
            case 1: return new int[]{x, y, d, h};
            case 2: return new int[]{x, y + h - d, w, d};
            default: return new int[]{x, y, w, d};
        }
    }

    /** What's left of a parcel {x, y, w, h} with d taken off its road side. */
    static int[] shrinkFrom(int[] q, int face, int d) {
        int x = q[0], y = q[1], w = q[2], h = q[3];
        switch (face) {
            case 0: return new int[]{x, y, w - d, h};
            case 1: return new int[]{x + d, y, w - d, h};
            case 2: return new int[]{x, y, w, h - d};
            default: return new int[]{x, y + d, w, h - d};
        }
    }

    @Override
    public String toString() {
        return "interchange " + interchange + " k2 " + k2 + " at " + city.hwyAt + " axis " + city.hwyAxis + " truck " + built[TRUCK] + " motel " + built[MOTEL] + " fuel " + built[FUEL]
                + " garage " + built[GARAGE] + " weigh " + built[WEIGH];
    }

    @Override
    void finish() {
        // The second highway's name: another of the country's highways.
        String[][] hw = city.country.highways;
        String name = hw[(int) (Math.abs(city.cfg.seed) % hw.length)][0];
        if (name.equals(city.hwyName)) name = hw[(int) ((Math.abs(city.cfg.seed) + 1) % hw.length)][0];
        for (Street st : route) st.name = name;
    }

    @Override
    int ground(int x, int y) {
        // Open prairie: dry, golden green, in broad soft swells.
        float n = (float) (Math.sin(x * 0.031f + y * 0.012f) * Math.sin(y * 0.037f - x * 0.009f + 2.1f));
        int step = Math.round(n * 2);
        int base = city.drawnRealistic ? 0xFF6B7A3A : 0xFF6E7E3C;
        return mix(base, step > 0 ? 0xFFA8A060 : 0xFF46562A, Math.abs(step) * 0.12f);
    }
}
