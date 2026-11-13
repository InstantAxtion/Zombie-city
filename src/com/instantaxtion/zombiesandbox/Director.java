package com.instantaxtion.zombiesandbox;

import java.util.Random;

/**
 * ((20.0) The director: paces an outbreak so it has a shape - pressure building, a peak, a breather - rather than
 * one long slide either way. It watches how the fight is going and, depending on the difficulty, sends stragglers
 * in from the country while things are quiet, a horde when night falls, a siege on a station or base in the small
 * hours on the harder settings, holds off while the city is being overwhelmed, and on the easier ones has supplies
 * dropped to a force that's on its knees. It only ever acts through World's own events.
 */
final class Director {
    static final int EASY = 0, NORMAL = 1, HARD = 2, NIGHTMARE = 3;
    static final String[] NAMES = {"Easy", "Normal", "Hard", "Nightmare"};
    /** By difficulty: how quick and tough the dead are, how often a bite infects, what a bullet does, what a game scores. */
    static final float[] Z_SPEED = {0.93f, 1, 1.06f, 1.12f}, Z_HP = {0.85f, 1, 1.2f, 1.4f}, INFECT = {0.8f, 1, 1.15f, 1.3f},
            GUN = {1.2f, 1, 0.92f, 0.85f}, SCORE = {0.6f, 1, 1.5f, 2.2f};
    /** How many come at nightfall, and in a straggling group, by difficulty. */
    private static final int[] SURGE = {0, 18, 30, 45}, STRAGGLERS = {0, 5, 9, 14};

    static final int CALM = 0, BUILD = 1, PEAK = 2, RESPITE = 3;
    static final String[] PHASES = {"Calm", "Building", "Peak", "Respite"};

    final int level;
    int phase = CALM;
    /** How hard pressed the city is, 0 to 1, smoothed over half a minute or so. */
    float stress;
    /** Seconds in this phase, until the next group of stragglers, and since help was last dropped. */
    float phaseT, nextWave = 90, reliefCd;
    /** What it has done, for the report. */
    int hordes, stragglers, reliefs, sieges;
    private int lastLost = -1;
    private boolean siegedTonight;
    private final Random rnd;

    Director(int level, long seed) {
        this.level = Math.max(EASY, Math.min(NIGHTMARE, level));
        rnd = new Random(seed);
        nextWave = 80 + rnd.nextFloat() * 40;
    }

    /** Once a second, from the world. */
    void tick(World w) {
        if (!w.outbreak || w.warResult != 0 || w.missionResult != 0) {
            phase = CALM;
            stress *= 0.9f;
            lastLost = -1;
            return;
        }
        // How it's going: who's being lost, and how many of the dead there are to the living.
        int lost = w.civiliansLost + w.copsLost + w.soldiersLost;
        int fell = lastLost < 0 ? 0 : Math.max(0, lost - lastLost);
        lastLost = lost;
        float z = w.zombieCount(), h = Math.max(1, w.humans);
        float now = Math.min(1, fell / 4f + Math.min(0.7f, 2.5f * z / (z + h)) + (w.warBalance < 0.3f ? 0.3f : 0));
        stress += (now - stress) * 0.06f;
        phaseT += 1;
        switch (phase) {
            case CALM:
                phase = BUILD;
                phaseT = 0;
                break;
            case BUILD:
                if (stress > 0.62f) to(PEAK);
                break;
            case PEAK:
                // (A peak doesn't last for ever: half a minute to a minute and a half, less the harder it is.)
                if (stress < 0.45f || phaseT > 90 - level * 15) to(RESPITE);
                break;
            default:
                if (phaseT > 50 - level * 8) to(BUILD);
        }
        if (reliefCd > 0) reliefCd -= 1;
        // Building: stragglers wander in from the country, at night on Normal, any time on the harder settings.
        if (phase == BUILD && STRAGGLERS[level] > 0 && (level >= HARD || w.sky.night())) {
            nextWave -= 1;
            if (nextWave <= 0) {
                nextWave = (level == NORMAL ? 110 : level == HARD ? 95 : 75) + rnd.nextFloat() * 50;
                float[] at = target(w);
                int n = STRAGGLERS[level] + rnd.nextInt(4);
                if (at != null && !w.hordeFrom(at[0], at[1], n, false).isEmpty()) stragglers++;
            }
        }
        // In the small hours on the harder settings: a horde makes for a station or a base.
        if (level >= HARD && w.sky.night() && !siegedTonight && phase != PEAK && w.sky.untilDawn() < Sky.DAY_SECONDS * 0.16f
                && w.sky.untilDawn() > 30) {
            City.Facility f = w.city.nearestFacility(rnd.nextBoolean() ? City.FACILITY_BASE : City.FACILITY_POLICE,
                    rnd.nextFloat() * w.city.worldW(), rnd.nextFloat() * w.city.worldH());
            siegedTonight = true;
            if (f != null && !w.hordeFrom(f.x, f.y, SURGE[level], false).isEmpty()) {
                sieges++;
                w.dispatch.say(Dispatch.WHO_INFO, null, "A horde is making straight for " + f.name + ".", f.x, f.y);
                w.timeline("A horde lays siege to " + f.name, f.x, f.y);
            }
        }
        // A force on its knees, on the easier settings: supplies dropped to whoever's still fighting.
        if (level <= NORMAL && reliefCd <= 0 && w.warBalance < 0.3f && w.mission() == null && w.outbreakTime > 120) {
            float[] at = armedCentre(w);
            if (at != null) {
                reliefCd = level == EASY ? 120 : 240;
                reliefs++;
                w.supplyDrop(at[0], at[1]);
            }
        }
    }

    private void to(int p) {
        phase = p;
        phaseT = 0;
    }

    /** Night falls on an outbreak: the dead come in from the dark. */
    void nightFell(World w) {
        siegedTonight = false;
        if (!w.outbreak || w.warResult != 0 || w.missionResult != 0 || SURGE[level] == 0) return;
        float[] at = target(w);
        if (at == null) return;
        if (!w.hordeFrom(at[0], at[1], SURGE[level] + rnd.nextInt(8), true).isEmpty()) hordes++;
    }

    /** Where the dead make for: where most of the living are, near enough. */
    private float[] target(World w) {
        float sx = 0, sy = 0;
        int n = 0;
        for (int i = rnd.nextInt(7), m = w.entities.size(); i < m; i += 7) {
            Entity e = w.entities.get(i);
            if (e.dead || e.removed || e.isZombie() || e.type == Entity.DOG) continue;
            sx += e.x;
            sy += e.y;
            n++;
        }
        if (n == 0) return null;
        return new float[]{sx / n + (rnd.nextFloat() - 0.5f) * 300, sy / n + (rnd.nextFloat() - 0.5f) * 300};
    }

    private float[] armedCentre(World w) {
        float sx = 0, sy = 0;
        int n = 0;
        for (int i = 0, m = w.entities.size(); i < m; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || e.removed || (e.type != Entity.COP && e.type != Entity.SOLDIER)) continue;
            sx += e.x;
            sy += e.y;
            n++;
        }
        if (n == 0) return null;
        float cx = sx / n, cy = sy / n;
        // (To one of them, not to the middle of them all - which may be the middle of a lake.)
        Entity best = null;
        float bd = Float.MAX_VALUE;
        for (int i = 0, m = w.entities.size(); i < m; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || e.removed || (e.type != Entity.COP && e.type != Entity.SOLDIER)) continue;
            float d = (e.x - cx) * (e.x - cx) + (e.y - cy) * (e.y - cy);
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        return best == null ? null : new float[]{best.x, best.y};
    }
}
