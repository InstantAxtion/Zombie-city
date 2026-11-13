package com.instantaxtion.zombiesandbox;

/**
 * Sound effect ids shared by the simulation (which emits them) and {@link Sound} (which plays them), and what the
 * mixer needs to know about each: ((13.4) how many takes of it there are, how much it matters when there are more
 * sounds than voices, how many may play at once, how far it carries and how much of it echoes round the streets).
 */
final class Sfx {
    static final int PISTOL = 0, RIFLE = 1, EXPLOSION = 2, BITE = 3, GROAN = 4, GROAN_DEEP = 5, SCREAM = 6,
            CLICK = 7, RADIO = 8, PHONE = 9, SHRIEK = 10, THUD = 11, SIREN = 12, ROTOR = 13, BARK = 14,
            HORN = 15, CANNON = 16, MG = 17, SNIPER = 18, COLLAPSE = 19, CRASH = 20, /* 21 was the fire hose (10.49) */ AMB_SIREN = 22, ENGINE = 23,
            JET = 24, SPIT = 25, BURST = 26, ALARM = 27, SHIELD = 28,
            // (10.24)
            SHOTGUN = 29, REVOLVER = 30, GLASS = 31, POUND = 32, HUNTING = 33,
            // ((13.4) A sound of its own for each gun, and for what goes with them.)
            SMG = 34, CARBINE = 35, DMR = 36, RELOAD = 37, DRY_FIRE = 38, RICOCHET = 39, MELEE = 40, LAUNCHER = 41,
            CLIMB = 42,
            // ((14.4) The tension under the music as things get worse: a heartbeat over a low drone.)
            HEART = 43,
            // ((19.0) The Sound Update: the streets, the radio and the menus. (19.6: the other modes' sounds went.))
            CASH = 44, CHIME = 45, STAR = 46, STING_WIN = 47, STING_LOSE = 48, STING_ALERT = 49, ERROR = 50, BUZZ = 51,
            DEBRIS = 52, WHINE = 53, SQUELCH = 54, MIL_RADIO = 55, TAP = 56, PANIC = 57;
    static final int COUNT = 58;

    /** ((19.0) The mixer's groups, each with its own level: the guns, blasts, the voices of the living and the dead,
     * vehicles, everything else in the world, the radio, the menus and stings. */
    static final int BUS_GUNS = 0, BUS_BLASTS = 1, BUS_VOICES = 2, BUS_VEHICLES = 3, BUS_WORLD = 4, BUS_RADIO = 5, BUS_UI = 6;
    static final int BUSES = 7;
    static final int[] BUS = new int[COUNT];

    /** ((19.0) The looping beds of ambience, mixed under everything: each follows what's on the screen. */
    static final int LOOP_CITY = 0, LOOP_WIND = 1, LOOP_BIRDS = 2, LOOP_CROWD = 3, LOOP_WATER = 4, LOOP_TENSION = 5;
    static final int LOOPS = 6;
    static final String[] LOOP_NAMES = {"City", "Wind", "Birds", "Crowd", "Water", "Tension"};

    /** Shortest gap between two plays of the same sound, in seconds, so crowds don't turn into noise. */
    static final float[] MIN_GAP = {0.05f, 0.04f, 0.08f, 0.09f, 0.9f, 1.4f, 0.8f, 0f, 2f, 2f, 2f, 0.1f, 4f, 1.2f, 0.8f,
            4f, 0.3f, 0.06f, 0.3f, 1f, 0.2f, 1.5f, 4f, 2.5f,
            4f, 0.3f, 0.2f, 8f, 0.15f,
            0.08f, 0.06f, 0.6f, 0.9f, 0.2f,
            0.05f, 0.05f, 0.12f, 0.25f, 0.3f, 0.12f, 0.1f, 0.3f, 0.5f, 1.6f,
            // (19.0)
            0.08f, 0.1f, 0.12f, 1f, 1f, 1f, 0.15f, 1f, 0.4f, 3f, 0.3f, 0.5f, 0.04f, 2f};
    /** Background sounds that get on the nerves when they loop; these fade and space out with repetition. */
    static final boolean[] REPEATS = new boolean[COUNT];

    /** ((13.4) Takes of each sound, made a little differently, picked at random so a firefight isn't one sample. */
    static final int[] VARIANTS = new int[COUNT];
    /** ((13.4) How much it matters, 0 to 9: with every voice busy, a new sound takes the place of a lesser one. */
    static final int[] PRIORITY = new int[COUNT];
    /** ((13.4) How many of it may sound at once: the oldest makes way. */
    static final int[] MAX_VOICES = new int[COUNT];
    /** ((13.4) How much of it echoes back off the buildings, 0 to 1. */
    static final float[] ECHO = new float[COUNT];

    static {
        for (int id : new int[] {GROAN, GROAN_DEEP, SCREAM, RADIO, PHONE, SHRIEK, SIREN, ROTOR, BARK, HORN,
                AMB_SIREN, ENGINE, JET, SPIT, ALARM, COLLAPSE, POUND, CLIMB}) REPEATS[id] = true;
        java.util.Arrays.fill(VARIANTS, 2);
        java.util.Arrays.fill(PRIORITY, 4);
        java.util.Arrays.fill(MAX_VOICES, 3);
        java.util.Arrays.fill(ECHO, 0.15f);
        for (int id = 0; id < COUNT; id++)
            if (gun(id)) {
                VARIANTS[id] = 4;
                PRIORITY[id] = 6;
                MAX_VOICES[id] = 5;
                ECHO[id] = 0.45f;
            }
        for (int id : new int[] {GROAN, GROAN_DEEP, SCREAM, BITE}) {
            VARIANTS[id] = 4;
            MAX_VOICES[id] = 4;
            ECHO[id] = 0.1f;
        }
        for (int id : new int[] {EXPLOSION, CANNON, COLLAPSE}) {
            VARIANTS[id] = 3;
            PRIORITY[id] = 9;
            MAX_VOICES[id] = 3;
            ECHO[id] = 0.6f;
        }
        for (int id : new int[] {SIREN, AMB_SIREN, ALARM, PHONE, HORN}) {
            VARIANTS[id] = 1;
            MAX_VOICES[id] = 1;
            ECHO[id] = 0.25f;
        }
        for (int id : new int[] {RADIO, CLICK}) {
            VARIANTS[id] = 1;
            PRIORITY[id] = 8;
            MAX_VOICES[id] = 2;
            ECHO[id] = 0;
        }
        for (int id : new int[] {ROTOR, JET, ENGINE}) {
            VARIANTS[id] = 1;
            MAX_VOICES[id] = 2;
        }
        for (int id : new int[] {RELOAD, DRY_FIRE, RICOCHET, MELEE, CLIMB}) {
            VARIANTS[id] = 3;
            PRIORITY[id] = 3;
            ECHO[id] = 0.1f;
        }
        PRIORITY[RICOCHET] = 2;
        MAX_VOICES[RICOCHET] = 2;
        MAX_VOICES[CLIMB] = 2;
        VARIANTS[HEART] = 1;
        PRIORITY[HEART] = 1;
        MAX_VOICES[HEART] = 1;
        ECHO[HEART] = 0;
        PRIORITY[LAUNCHER] = 7;
        ECHO[LAUNCHER] = 0.35f;
        PRIORITY[SCREAM] = 5;
        PRIORITY[THUD] = 2;
        PRIORITY[GROAN] = PRIORITY[GROAN_DEEP] = 3;
        // ((19.0) The new sounds.)
        for (int id : new int[] {CASH, CHIME, STAR, STING_WIN, STING_LOSE, STING_ALERT, ERROR, TAP, SQUELCH, MIL_RADIO, WHINE}) {
            VARIANTS[id] = 1;
            PRIORITY[id] = 8;
            MAX_VOICES[id] = 2;
            ECHO[id] = 0.05f;
        }
        MAX_VOICES[STAR] = 3;
        PRIORITY[WHINE] = 9;
        for (int id : new int[] {PANIC}) {
            VARIANTS[id] = 3;
            PRIORITY[id] = 5;
            MAX_VOICES[id] = 2;
            ECHO[id] = 0.3f;
            REPEATS[id] = true;
        }
        VARIANTS[DEBRIS] = 3;
        PRIORITY[DEBRIS] = 5;
        ECHO[DEBRIS] = 0.4f;
        VARIANTS[BUZZ] = 1;
        MAX_VOICES[BUZZ] = 1;
        REPEATS[BUZZ] = true;
        // The groups.
        java.util.Arrays.fill(BUS, BUS_WORLD);
        for (int id = 0; id < COUNT; id++) if (gun(id)) BUS[id] = BUS_GUNS;
        for (int id : new int[] {EXPLOSION, CANNON, COLLAPSE, LAUNCHER, DEBRIS, CRASH}) BUS[id] = BUS_BLASTS;
        for (int id : new int[] {BITE, GROAN, GROAN_DEEP, SCREAM, SHRIEK, SPIT, BARK, PANIC}) BUS[id] = BUS_VOICES;
        for (int id : new int[] {SIREN, AMB_SIREN, ROTOR, ENGINE, JET, HORN, BUZZ}) BUS[id] = BUS_VEHICLES;
        for (int id : new int[] {RADIO, PHONE, SQUELCH, MIL_RADIO}) BUS[id] = BUS_RADIO;
        for (int id : new int[] {CLICK, CASH, CHIME, STAR, STING_WIN, STING_LOSE, STING_ALERT, ERROR, TAP, WHINE}) BUS[id] = BUS_UI;
    }

    /** ((13.4) Moved here from the view.) How far a sound carries, compared with the usual (most of a screen). */
    static float carry(int type) {
        switch (type) {
            case EXPLOSION: case CANNON: case COLLAPSE: return 3.2f;
            case PANIC: case DEBRIS: return 1.4f;
            case ROTOR: case JET: return 2.6f;
            case SIREN: case AMB_SIREN: case ALARM: return 2.2f;
            case MG: case RIFLE: case SHOTGUN: case SNIPER: case HUNTING: case BURST: case CARBINE: case DMR: case LAUNCHER: return 2.2f;
            case PISTOL: case REVOLVER: case SMG: return 1.6f;
            case CRASH: case HORN: return 1.4f;
            case GROAN: case GROAN_DEEP: case BITE: case SPIT: case CLIMB: return 0.7f;
            case RELOAD: case DRY_FIRE: case MELEE: case RICOCHET: return 0.6f;
            default: return 1f;
        }
    }

    /**
     * Listener fatigue: every play of an effect adds "heat" that cools by half every fifteen seconds. A hot repeating
     * sound waits longer between plays and comes in quieter, so a siren or groan that keeps firing settles into
     * the background instead of hammering the same sample.
     */
    static final class Fatigue {
        private final long[] last = new long[COUNT];
        private final float[] heat = new float[COUNT];

        Fatigue() {
            java.util.Arrays.fill(last, Long.MIN_VALUE / 2);
        }

        /** Volume multiplier for playing {@code id} at {@code now} (ms), or 0 to skip it. */
        float gain(int id, long now) {
            long since = now - last[id];
            float h = heat[id] * (float) Math.pow(0.5, since / 15000.0);
            boolean rep = REPEATS[id];
            float gap = MIN_GAP[id] * (rep ? 1 + h * 0.35f : 1);
            if (since < gap * 1000) return 0;
            last[id] = now;
            heat[id] = h + 1;
            // ((11.7) Gunfire stays loud through a long firefight: it fades a little as shots pile up, not to a patter.)
            if (gun(id)) return Math.max(0.55f, 1 / (1 + h * 0.3f));
            return rep ? Math.max(0.25f, 1 / (1 + h * 0.6f)) : Math.max(0.6f, 1 / (1 + h * 0.03f));
        }
    }

    /** ((19.0) What each sound is called, in the Sound Lab. */
    static final String[] NAMES = {"Pistol", "Rifle", "Explosion", "Bite", "Groan", "Deep groan", "Scream", "Click", "Radio", "Phone",
            "Shriek", "Thud", "Siren", "Rotor", "Bark", "Horn", "Tank gun", "Machine gun", "Sniper", "Collapse", "Crash", "(unused)",
            "Ambulance", "Engine", "Jet", "Spit", "Burst", "Alarm", "Shield", "Shotgun", "Revolver", "Glass", "Pounding", "Hunting rifle",
            "SMG", "Carbine", "Marksman rifle", "Reload", "Dry fire", "Ricochet", "Melee", "Launcher", "Climb", "Heartbeat",
            "Cash", "Objective", "Star", "Victory sting", "Defeat sting", "Alert sting", "Error", "Drone",
            "Debris", "Ringing ears", "Squelch", "Army radio", "Tap", "Crowd panic"};

    /** ((19.0) A sound of the menus and the mission, not of the world: never placed, muffled or echoed. */
    static boolean ui(int id) {
        return BUS[id] == BUS_UI;
    }

    /** (11.7) A gunshot of any kind. */
    static boolean gun(int id) {
        return id == PISTOL || id == RIFLE || id == MG || id == SNIPER || id == BURST || id == SHOTGUN || id == REVOLVER || id == HUNTING
                || id == SMG || id == CARBINE || id == DMR;
    }

    private Sfx() {
    }
}
