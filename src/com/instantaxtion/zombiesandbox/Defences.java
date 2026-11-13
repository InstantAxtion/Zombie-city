package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;

/**
 * ((21.0) Field defences, beyond the barricade: razor wire and machine-gun nests.
 * <p>
 * Razor wire goes down a tile at a time. It stops nobody, but the dead walk straight into it: tangled in a coil they
 * go at a third of their pace and it tears at them, and every one that drags through wears the coil down until it's
 * gone. The living know where the gaps are and step through.
 * <p>
 * A machine-gun nest is two soldiers with a machine gun, a horseshoe of sandbags round them and a crate of
 * ammunition: they hold the spot and don't leave it.
 */
final class Defences {
    /** What a coil of wire can take, how slow the dead are in it, and what it does to them each second. */
    static final float WIRE_HP = 400, WIRE_SLOW = 0.3f, WIRE_HURT = 8, WIRE_WEAR = 12;
    static final int MAX_WIRE = 600;
    /** How much tougher a nest's sandbags are than a barricade. */
    static final float SANDBAG_HP = 520;

    /** Coils of wire: x, y, what it can still take, its tile. */
    final ArrayList<float[]> wires = new ArrayList<float[]>();
    /** Which tiles have wire on them (made when the first coil goes down). */
    private boolean[] wired;
    /** The dead that died in the wire, and the nests built. */
    int wireKills, nests;

    boolean any() {
        return !wires.isEmpty();
    }

    boolean wireAt(World w, float x, float y) {
        if (wired == null) return false;
        int t = w.city.tileIndex(x, y);
        return t >= 0 && wired[t];
    }

    /** A coil of wire on the tile at (x, y), or null if there's no laying one there. */
    float[] layWire(World w, float x, float y) {
        City city = w.city;
        int tx = City.fl(x / City.T), ty = City.fl(y / City.T);
        if (tx < 0 || ty < 0 || tx >= city.w || ty >= city.h || wires.size() >= MAX_WIRE) return null;
        int i = ty * city.w + tx;
        byte t = city.tiles[i];
        if (city.solid[i] || t == City.WATER || t == City.RAIL) return null;
        if (wired == null) wired = new boolean[city.w * city.h];
        if (wired[i]) return null;
        float[] c = {tx * City.T + City.T / 2f, ty * City.T + City.T / 2f, WIRE_HP, i};
        wires.add(c);
        wired[i] = true;
        return c;
    }

    /** A coil put back from a save. */
    void restore(World w, float x, float y, float hp) {
        float[] c = layWire(w, x, y);
        if (c != null) c[2] = hp;
    }

    /**
     * One of the dead on the move: how much of their pace the wire leaves them (1 off the wire). Tangled, they're
     * torn at and the coil wears.
     */
    float snag(World w, Entity z, float dt) {
        if (wired == null) return 1;
        int t = w.city.tileIndex(z.x, z.y);
        if (t < 0 || !wired[t]) return 1;
        for (int i = 0, n = wires.size(); i < n; i++) {
            float[] c = wires.get(i);
            if ((int) c[3] != t) continue;
            boolean up = z.hp > 0;
            z.hp -= WIRE_HURT * dt;
            z.hurt = Math.max(z.hurt, 0.3f);
            if (up && z.hp <= 0) wireKills++;
            c[2] -= (z.type == Entity.BRUTE ? WIRE_WEAR * 5 : WIRE_WEAR) * dt;
            if (w.rnd.nextFloat() < dt * 0.8f) w.emit(Sfx.WIRE, c[0], c[1]);
            if (c[2] <= 0) {
                wires.remove(i);
                wired[t] = false;
                w.wireGone(c[0], c[1]);
            }
            return z.type == Entity.BRUTE ? 0.6f : WIRE_SLOW;
        }
        // (The table said wire and there's none: put right.)
        wired[t] = false;
        return 1;
    }

    void clear() {
        wires.clear();
        if (wired != null) java.util.Arrays.fill(wired, false);
    }

    /**
     * A nest at (x, y): two machine gunners holding the spot, sandbags on three sides of them and ammunition to hand.
     * Returns the crew (empty if there's no room for them).
     */
    ArrayList<Entity> buildNest(World w, float x, float y) {
        ArrayList<Entity> crew = new ArrayList<Entity>();
        float[] spot = w.city.findWalkable(x, y);
        if (spot == null) return crew;
        float cx = (City.fl(spot[0] / City.T) + 0.5f) * City.T, cy = (City.fl(spot[1] / City.T) + 0.5f) * City.T;
        for (int k = 0; k < 2; k++) {
            Entity g = w.spawnSoldier(Entity.ROLE_GUNNER, cx + (k == 0 ? -3 : 3), cy);
            if (g == null) continue;
            g.task = Dispatch.T_HOLD;
            g.postX = g.x;
            g.postY = g.y;
            g.reserve += g.magSize * 3;
            crew.add(g);
        }
        if (crew.isEmpty()) return crew;
        // Sandbags: a horseshoe, open towards the nearest road so there's a way in and out.
        int[][] ring = {{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}};
        int open = 6;
        for (int k : new int[]{1, 3, 4, 6}) {
            int ti = w.city.tileIndex(cx + ring[k][0] * City.T, cy + ring[k][1] * City.T);
            if (ti >= 0 && w.city.tiles[ti] == City.ROAD) {
                open = k;
                break;
            }
        }
        for (int k = 0; k < ring.length; k++) {
            if (k == open) continue;
            float[] b = w.placeBarricade(cx + ring[k][0] * City.T, cy + ring[k][1] * City.T);
            if (b != null) b[2] = SANDBAG_HP;
        }
        w.placeCrate(cx, cy + 4, 0);
        nests++;
        return crew;
    }
}
