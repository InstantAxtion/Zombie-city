package com.instantaxtion.zombiesandbox;

import java.util.Random;

/**
 * ((20.0) The director: paces an outbreak so it has a shape - pressure building, a peak, a breather - rather than
 * one long slide either way. It watches how the fight is going and, depending on the difficulty, sends stragglers
 * in from the country while things are quiet, a horde every few minutes, a siege on a station or base on the
 * harder settings, and holds off while the city is being overwhelmed. ((21.1) In campaign missions only: in the sandbox it
 * watches and sends nothing.) It only ever acts through World's own events.
 */
final class Director {
    static final int EASY = 0, NORMAL = 1, HARD = 2, NIGHTMARE = 3;
    static final String[] NAMES = {"Easy", "Normal", "Hard", "Nightmare"};
    /** By difficulty: how quick and tough the dead are, how often a bite infects, what a bullet does, what a game scores. */
    static final float[] Z_SPEED = {0.93f, 1, 1.06f, 1.12f}, Z_HP = {0.85f, 1, 1.2f, 1.4f}, INFECT = {0.8f, 1, 1.15f, 1.3f},
            GUN = {1.2f, 1, 0.92f, 0.85f}, SCORE = {0.6f, 1, 1.5f, 2.2f};
    /** How many come in a horde, and in a straggling group, by difficulty. */
    private static final int[] SURGE = {0, 18, 30, 45}, STRAGGLERS = {0, 5, 9, 14};

    static final int CALM = 0, BUILD = 1, PEAK = 2, RESPITE = 3;
    static final String[] PHASES = {"Calm", "Building", "Peak", "Respite"};

    int level;
    int phase = CALM;
    /** How hard pressed the city is, 0 to 1, smoothed over half a minute or so. */
    float stress;
    /** Seconds in this phase, until the next group of stragglers, and since help was last dropped. */
    float phaseT, nextWave = 90;
    /** Seconds of outbreak until the next horde from outside, and until the next siege (Hard and above). */
    float nextHorde = 330, nextSiege = 480;
    /** What it has done, for the report. */
    int hordes, stragglers, sieges;
    private int lastLost = -1;
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
        // ((21.1) The sandbox is the player's: nothing comes in that they didn't put there. Only a campaign mission has
        // hordes of its own - and not one to clear the city, which has what it starts with to clear.)
        boolean sealed = w.mission() == null || w.mission().goal == Campaign.G_CLEAR;
        // Building: stragglers wander in from the country - once it's gone on five minutes on Normal, from the start on
        // the harder settings.
        if (!sealed && phase == BUILD && STRAGGLERS[level] > 0 && (level >= HARD || w.outbreakTime > 300)) {
            nextWave -= 1;
            if (nextWave <= 0) {
                nextWave = (level == NORMAL ? 110 : level == HARD ? 95 : 75) + rnd.nextFloat() * 50;
                float[] at = target(w);
                int n = STRAGGLERS[level] + rnd.nextInt(4);
                if (at != null && !w.hordeFrom(at[0], at[1], n, false).isEmpty()) stragglers++;
            }
        }
        // Every few minutes, except while the city is at its worst: a horde in from outside.
        if (!sealed && SURGE[level] > 0 && phase != PEAK && (nextHorde -= 1) <= 0) {
            nextHorde = (level == NORMAL ? 330 : level == HARD ? 270 : 220) + rnd.nextFloat() * 60;
            float[] at = target(w);
            if (at != null && !w.hordeFrom(at[0], at[1], SURGE[level] + rnd.nextInt(8), true).isEmpty()) hordes++;
        }
        // On the harder settings, every so often a horde makes straight for a station or a base.
        if (!sealed && level >= HARD && phase != PEAK && (nextSiege -= 1) <= 0) {
            nextSiege = (level == HARD ? 480 : 380) + rnd.nextFloat() * 90;
            City.Facility f = w.city.nearestFacility(rnd.nextBoolean() ? City.FACILITY_BASE : City.FACILITY_POLICE,
                    rnd.nextFloat() * w.city.worldW(), rnd.nextFloat() * w.city.worldH());
            if (f != null && !w.hordeFrom(f.x, f.y, SURGE[level], false).isEmpty()) {
                sieges++;
                w.dispatch.say(Dispatch.WHO_INFO, null, "A horde is making straight for " + f.name + ".", f.x, f.y);
                w.timeline("A horde lays siege to " + f.name, f.x, f.y);
            }
        }
    }

    private void to(int p) {
        phase = p;
        phaseT = 0;
    }

    /** Where the dead make for: where most of the living are, near enough. */
    private float[] target(World w) {
        // ((21.0) In a mission to hold a site, that's where they all make for.)
        if (w.mission() != null && w.mission().goal == Campaign.G_DEFEND) {
            City.Facility f = Campaign.site(w);
            if (f != null) return new float[]{f.x + (rnd.nextFloat() - 0.5f) * 80, f.y + (rnd.nextFloat() - 0.5f) * 80};
        }
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
}
