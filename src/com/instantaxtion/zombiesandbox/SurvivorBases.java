package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;

/**
 * (25.0) Survivor bases: armed survivors who band together pick a building and wall off the ground round it - on their
 * own, nobody tells them to. The wall goes up a section at a time while it's quiet (wooden planks, then sandbags, then
 * concrete), with a gate or two that is shut when the dead come near and opened again once they've gone; watchtowers
 * at the corners for their best shots; and a workshop that makes rounds, with an armoury that hands guns to the
 * unarmed among them. Separate from the police and army safe zones: the authorities' shelters are for now, these are
 * the survivors' own, for as long as they last.
 */
final class SurvivorBases {
    private SurvivorBases() {
    }

    static final int MAX_BASES = 4;
    /** The work a wall section, and each later upgrade of the whole wall (to sandbags, to concrete), takes. */
    static final float SECTION_WORK = 1, SANDBAG_WORK = 70, CONCRETE_WORK = 150;
    static final String[] TIER_NAMES = {"wooden", "sandbagged", "concrete"};

    static final class Base {
        City.Building b;
        String name;
        /** The walled ground, in tiles (inclusive): the building and a margin round it. */
        int tx0, ty0, tx1, ty1;
        /** The open tiles round the edge, in order round it, that the wall goes on (buildings along it are wall already). */
        int[] ring = new int[0];
        /** How often each ring section couldn't be built (someone always standing there): given up on after a while. */
        int[] fails = new int[0];
        /** The gate tiles (part of the ring, never walled for good). */
        int[] gates = new int[0];
        /** Watchtower spots {x, y}, just inside the corners. */
        float[][] towers = new float[0][];
        /** 0 wooden, 1 sandbagged, 2 concrete; work towards the next section or upgrade. */
        int tier;
        float work;
        /** Every section of the ring walled at least once: the towers and workshop can go up. */
        boolean walled;
        int towersUp;
        /** The workshop's rounds in store, and its progress to the next box of them. */
        float stash, workshop;
        float age, quietFor, emptyFor, saidCd;
        int said;
        /** Counted each second: everyone who belongs, those with guns on the posts, and the dead inside the walls. */
        transient int members, guards, inside, engineers;
        /** How many it has room for. */
        int room = 30;
        boolean fallen;
        /** ((26.0) Two bases joined into one walled settlement. */
        boolean settlement;

        float cx() {
            return (tx0 + tx1 + 1) * City.T / 2f;
        }

        float cy() {
            return (ty0 + ty1 + 1) * City.T / 2f;
        }

        boolean contains(float x, float y, int margin) {
            int tx = City.fl(x / City.T), ty = City.fl(y / City.T);
            return tx >= tx0 - margin && tx <= tx1 + margin && ty >= ty0 - margin && ty <= ty1 + margin;
        }

        boolean isGate(int t) {
            for (int g : gates) if (g == t) return true;
            return false;
        }
    }

    /**
     * Lays a base out round building b: the ground, the ring of open tiles its wall goes on, its gates and towers.
     * Null if the building won't do (too big, at the map's edge, water in the way, or too much open ring to wall).
     */
    static Base plan(City city, City.Building b) {
        float T = City.T;
        int bx0 = City.fl(b.x0 / T), by0 = City.fl(b.y0 / T), bx1 = City.fl((b.x1 - 1) / T), by1 = City.fl((b.y1 - 1) / T);
        if (bx1 - bx0 > 12 || by1 - by0 > 12) return null;
        return planRect(city, b, bx0 - 3, by0 - 3, bx1 + 3, by1 + 3, 90);
    }

    /** ((26.0) The same over any ground (tiles, inclusive): a settlement of two bases joined, too. */
    static Base planRect(City city, City.Building b, int tx0, int ty0, int tx1, int ty1, int maxRing) {
        float T = City.T;
        Base s = new Base();
        s.b = b;
        s.tx0 = tx0;
        s.ty0 = ty0;
        s.tx1 = tx1;
        s.ty1 = ty1;
        if (s.tx0 < 2 || s.ty0 < 2 || s.tx1 >= city.w - 2 || s.ty1 >= city.h - 2) return null;
        // The ring, clockwise from the top left corner.
        ArrayList<Integer> ring = new ArrayList<Integer>();
        int W = city.w;
        for (int x = s.tx0; x <= s.tx1; x++) ring.add(s.ty0 * W + x);
        for (int y = s.ty0 + 1; y <= s.ty1; y++) ring.add(y * W + s.tx1);
        for (int x = s.tx1 - 1; x >= s.tx0; x--) ring.add(s.ty1 * W + x);
        for (int y = s.ty1 - 1; y > s.ty0; y--) ring.add(y * W + s.tx0);
        ArrayList<Integer> open = new ArrayList<Integer>();
        for (int i = 0; i < ring.size(); i++) {
            int t = ring.get(i);
            byte k = city.tiles[t];
            if (k == City.WATER || k == City.RAIL) return null;
            // (A fence or barricade is a stretch of wall already - the base's own, once it's built.)
            if (city.solid[t] && k != City.FENCE) continue;
            open.add(t);
        }
        if (open.size() < 8 || open.size() > maxRing) return null;
        s.ring = new int[open.size()];
        s.fails = new int[open.size()];
        for (int i = 0; i < s.ring.length; i++) s.ring[i] = open.get(i);
        // The gate: the ring tile nearest the building's door, and a second on the far side of a big ring.
        int g0 = nearestRing(s, city, b.doorX, b.doorY);
        if (s.ring.length > 120) {
            // (A settlement: a gate on each side.)
            float cx = s.cx(), cy = s.cy(), hw = (s.tx1 - s.tx0) * City.T / 2f, hh = (s.ty1 - s.ty0) * City.T / 2f;
            int a = nearestRing(s, city, cx - hw, cy), bb = nearestRing(s, city, cx + hw, cy), c = nearestRing(s, city, cx, cy - hh), d = nearestRing(s, city, cx, cy + hh);
            s.gates = new int[]{a, bb, c, d};
        } else if (s.ring.length > 40) {
            int g1 = nearestRing(s, city, 2 * s.cx() - b.doorX, 2 * s.cy() - b.doorY);
            s.gates = g1 != g0 ? new int[]{g0, g1} : new int[]{g0};
        } else s.gates = new int[]{g0};
        // Towers just inside each corner, where there's open ground for one.
        ArrayList<float[]> towers = new ArrayList<float[]>();
        int[][] corners = {{s.tx0 + 1, s.ty0 + 1}, {s.tx1 - 1, s.ty0 + 1}, {s.tx1 - 1, s.ty1 - 1}, {s.tx0 + 1, s.ty1 - 1}};
        for (int[] c : corners) {
            int t = c[1] * W + c[0];
            if (!city.solid[t] && city.tiles[t] != City.WATER) towers.add(new float[]{(c[0] + 0.5f) * T, (c[1] + 0.5f) * T});
        }
        s.towers = towers.toArray(new float[towers.size()][]);
        // (Room for as many as the open ground inside the wall will take: one for every three tiles of it.)
        int open2 = 0;
        for (int y = s.ty0 + 1; y < s.ty1; y++)
            for (int x = s.tx0 + 1; x < s.tx1; x++) if (!city.solid[y * W + x]) open2++;
        s.room = Math.max(8, Math.min(30, open2 / 3));
        return s;
    }

    private static int nearestRing(Base s, City city, float x, float y) {
        int best = s.ring[0];
        float bd = Float.MAX_VALUE;
        for (int t : s.ring) {
            float tx = (t % city.w + 0.5f) * City.T, ty = (t / city.w + 0.5f) * City.T;
            float d = (tx - x) * (tx - x) + (ty - y) * (ty - y);
            if (d < bd) {
                bd = d;
                best = t;
            }
        }
        return best;
    }

    /** The base's name: "Fort" or "Camp" and its street, or the building's own name. */
    static String nameFor(City city, City.Building b, int seed) {
        String place = city.placeName(b.doorX, b.doorY);
        int amp = place.indexOf(" & ");
        if (amp > 0) place = place.substring(0, amp);
        String[] forms = {"Fort %s", "Camp %s", "%s Stronghold", "%s Haven", "The %s Compound"};
        String core = place.replace(" Street", "").replace(" Avenue", "").replace(" Road", "").replace(" Lane", "").replace(" Drive", "");
        if (b.name != null && (seed & 3) == 0) return b.name + " Camp";
        return String.format(forms[Math.abs(seed) % forms.length], core);
    }

    /** How many of the ring's sections have a wall on them now. */
    static int wallsUp(City city, Base s) {
        int n = 0;
        for (int i = 0; i < s.ring.length; i++) {
            int t = s.ring[i];
            if (!s.isGate(t) && (city.tiles[t] == City.FENCE || s.fails[i] >= GIVE_UP)) n++;
        }
        return n;
    }

    static final int GIVE_UP = 25;

    /** The next section of the ring to build: not a gate, not up, not given up on. -1 when there's none. */
    static int nextSection(City city, Base s, int skip) {
        for (int i = 0; i < s.ring.length; i++) {
            int t = s.ring[i];
            if (i == skip || s.isGate(t) || city.tiles[t] == City.FENCE || s.fails[i] >= GIVE_UP) continue;
            return i;
        }
        return -1;
    }

    /** The barricade on tile t, or null. */
    static float[] barrierAt(World w, int t) {
        for (int i = 0; i < w.barriers.size(); i++) {
            float[] b = w.barriers.get(i);
            if ((int) b[3] == t) return b;
        }
        return null;
    }
}
