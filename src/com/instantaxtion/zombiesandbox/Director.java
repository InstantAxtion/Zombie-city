package com.instantaxtion.zombiesandbox;

import java.util.Random;

/**
 * ((20.0) The director: paces an outbreak so it has a shape - pressure building, a peak, a breather - rather than
 * one long slide either way. It watches how the fight is going. ((28.2) It used to send stragglers, hordes and sieges
 * in from outside in campaign missions; the campaign is gone, and in the sandbox it only ever watched.)
 */
final class Director {
    static final int EASY = 0, NORMAL = 1, HARD = 2, NIGHTMARE = 3;
    static final String[] NAMES = {"Easy", "Normal", "Hard", "Nightmare"};
    /** By difficulty: how quick and tough the dead are, how often a bite infects, what a bullet does, what a game scores. */
    static final float[] Z_SPEED = {0.93f, 1, 1.06f, 1.12f}, Z_HP = {0.85f, 1, 1.2f, 1.4f}, INFECT = {0.8f, 1, 1.15f, 1.3f},
            GUN = {1.2f, 1, 0.92f, 0.85f}, SCORE = {0.6f, 1, 1.5f, 2.2f};

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
    /** What it has done, for the report. ((28.2) Always 0 now; these and the timers above are kept for saved games.) */
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
        if (!w.outbreak || w.warResult != 0) {
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
        // ((21.1) The sandbox is the player's: nothing comes in that they didn't put there. ((28.2) Only the campaign's
        // missions had stragglers, hordes and sieges from outside, and they're gone.)
    }

    private void to(int p) {
        phase = p;
        phaseT = 0;
    }
}
