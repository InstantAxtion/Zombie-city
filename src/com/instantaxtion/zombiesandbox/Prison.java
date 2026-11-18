package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * The island prison: its yard, its life, its fall, and the ferry.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class Prison {
    private final World w;

    Prison(World w) {
        this.w = w;
    }

    /** (15.6) A random spot in the prison yard, or null. */
    float[] prisonYardSpot() {
        int[] p = w.city.prison;
        for (int k = 0; k < 40; k++) {
            float x = (p[0] + 2 + w.rnd.nextFloat() * (p[2] - p[0] - 4)) * City.T, y = (p[1] + 2 + w.rnd.nextFloat() * (p[3] - p[1] - 4)) * City.T;
            int t = w.city.tileIndex(x, y);
            if (t >= 0 && !w.city.solid[t] && w.city.tiles[t] == City.LOT) return new float[]{x, y};
        }
        return null;
    }

    /** (15.6) Inside the prison walls (the yard and the buildings in it)? */
    boolean inPrison(float x, float y) {
        int[] p = w.city.prison;
        return p != null && x > p[0] * City.T && x < p[2] * City.T && y > p[1] * City.T && y < p[3] * City.T;
    }

    /**
     * (15.6) Once a second on the Prison Island: the outbreak starts inside the walls (an inmate bitten) if it hasn't
     * started anywhere else first; and once the dead are loose in there and the guards are down to the last few, the
     * prison falls: the gate opens, and inmates and the dead alike pour out over the causeway.
     */
    void prisonLife() {
        // ((22.0) Only on the Prison Island does it start in there by itself: a prison on any other map stays quiet
        // unless the dead get in.)
        if (!w.prisonSeeded && !w.outbreak && w.time > World.PRISON_QUIET && w.city.cfg.prisonIsland()) {
            w.prisonSeeded = true;
            java.util.List<Entity> in = new java.util.ArrayList<Entity>();
            for (Entity e : w.entities) if (e.inmate && !e.dead && !e.infected) in.add(e);
            // (A fight in the yard: several bitten, turning one after another all over the yard, too many at once
            // for the guards to put down one by one.)
            java.util.Collections.shuffle(in, w.rnd);
            for (int k = 0; k < Math.min(8 + in.size() / 50, in.size()); k++) {
                Entity e = in.get(k);
                e.infected = true;
                e.infectTimer = 10 + k * 4 + w.rnd.nextFloat() * 6;
                if (w.patientZero == null) w.death.markPatientZero(e);
                if (k == 0) w.stats.timeline("A fight in the prison yard: inmates bitten", e.x, e.y);
            }
        }
        if (w.prisonFallen) return;
        int dead = 0, guards = 0;
        for (Entity e : w.entities) {
            if (e.dead || !inPrison(e.x, e.y)) continue;
            if (e.isZombie()) dead++;
            else if (e.role == Entity.ROLE_PRISON_GUARD) guards++;
        }
        if ((dead >= 6 && (guards <= 1 || dead > guards * 2)) || (w.blackout && dead > 0)) fallPrison();
    }

    /** (15.6) The prison falls: the gate opens and everyone inside is loose. */
    void fallPrison() {
        if (w.prisonFallen || w.city.prison == null) return;
        w.prisonFallen = true;
        w.city.openPrisonGate();
        w.containment.routesChanged();
        int loose = 0;
        for (Entity e : w.entities)
            if (e.inmate) {
                e.inmate = false;
                if (!e.dead) loose++;
            }
        float gx = (w.city.prisonGate[0] + 1.5f) * City.T, gy = w.city.prisonGate[1] * City.T;
        boolean island = w.city.cfg.prisonIsland();
        w.banner("PRISON BREAK", "The dead have overrun the " + (island ? "island " : "") + "prison and its gate is open. " + loose + " inmates are loose.");
        w.dispatch.say(Dispatch.WHO_POLICE, null, island ? "Police Command: The island prison has fallen. Inmates and the dead coming over the causeway. All units to the seafront!"
                : "Police Command: The prison has fallen. Inmates and the dead are pouring out of the gate. All units, contain them!", gx, gy);
        w.stats.timeline(island ? "The island prison fell" : "The prison fell", gx, gy);
        w.dispatch.sighting(gx, gy - 4 * City.T, null);
    }

    /** (15.6) The ferry: across the bay from one landing to the other, a little wait at each, and back. */
    void moveFerry(float dt) {
        if (w.ferryBoat == null) {
            float[] f = w.city.ferry;
            w.ferryBoat = new float[]{f[0], f[1], 0, 1, 0, 1};
        }
        float[] b = w.ferryBoat, f = w.city.ferry;
        if (w.ferryWait > 0) {
            w.ferryWait -= dt;
            b[4] = 0;
            return;
        }
        float tx = f[w.ferryLeg * 2], ty = f[w.ferryLeg * 2 + 1];
        float dx = tx - b[0], dy = ty - b[1], d = (float) Math.sqrt(dx * dx + dy * dy);
        if (d < 3) {
            w.ferryWait = 12;
            w.ferryLeg = 1 - w.ferryLeg;
            return;
        }
        b[4] = Math.min(28, d * 0.8f + 4);
        b[2] += (dx / d - b[2]) * Math.min(1, dt * 2);
        b[3] += (dy / d - b[3]) * Math.min(1, dt * 2);
        float l = (float) Math.sqrt(b[2] * b[2] + b[3] * b[3]) + 0.001f;
        b[0] += b[2] / l * b[4] * dt;
        b[1] += b[3] / l * b[4] * dt;
    }
}
