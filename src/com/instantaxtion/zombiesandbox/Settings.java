package com.instantaxtion.zombiesandbox;

import android.content.Context;
import android.content.SharedPreferences;

/** Player settings, saved between sessions. */
final class Settings implements OptionSet {
    private static final String[] VOLUME = {"Off", "25%", "50%", "75%", "100%"};
    private static final String[] ON_OFF = {"Off", "On"};
    private static final String[] LABELS = {"Music", "Sound effects", "View", "Blood", "Health bars",
            "Screen shake", "Show FPS", "Max population", "Radio messages", "Minimap",
            "Text & buttons", "Battery saver", "Name tags", "Graphics", "Vibration", "Radio lines on screen", "Destruction",
            // ((19.0) The Sound Update.)
            "Ambience", "Radio chatter", "Menu & mission sounds", "Mono audio", "Dynamic range", "Sound captions", "Shell shock",
            // ((19.5) The Visuals Update.)
            "Effects", "Screen effects", "Colour grading"};
    private static final String[][] VALUES = {VOLUME, VOLUME, {"Bird's-eye", "3D"}, ON_OFF, ON_OFF, ON_OFF, ON_OFF,
            {"Auto", "2500", "5000", "8000", "12000"}, ON_OFF, ON_OFF, {"Small", "Normal", "Large"}, ON_OFF, ON_OFF,
            {"Classic", "Realistic"}, ON_OFF, {"1", "2", "3", "4"}, {"Normal", "Heavy"},
            VOLUME, VOLUME, VOLUME, ON_OFF, {"Full", "Night"}, ON_OFF, ON_OFF,
            {"Low", "Medium", "High", "Ultra"}, ON_OFF, ON_OFF};
    private static final String[] KEYS = {"music", "sfx", "buildings3d", "gore", "healthBars", "shake", "fps",
            "maxPop2", "radio", "minimap", "uiSize", "battery", "nameTags", "graphics", "vibration", "feedLines", "destruction",
            "ambience", "radioVol", "uiVol", "mono", "nightMode", "captions", "shellShock", "effects", "screenFx", "grading"};
    private static final int[] DEFAULTS = {2, 3, 1, 1, 1, 1, 0, 0, 1, 1, 1, 0, 1, 1, 1, 1, 0, 3, 3, 3, 0, 0, 0, 1, 2, 1, 1};
    /** 0 is Auto: room for everyone who lives in the city, and plenty of zombies. */
    private static final int[] MAX_POP = {0, 2500, 5000, 8000, 12000};

    /**
     * ((21.4) The settings offered, by index: the rest stay as they come (FPS, radio, name tags, radio lines,
     * destruction, the extra volumes and sound options, screen effects and colour grading).
     */
    private static final int[] SHOWN = {0, 1, 2, 3, 4, 5, 7, 9, 10, 11, 13, 24, 14};

    private final SharedPreferences prefs;
    private final int[] v = DEFAULTS.clone();

    Settings(Context context) {
        prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        for (int i = 0; i < v.length; i++) {
            int saved = prefs.getInt(KEYS[i], DEFAULTS[i]);
            v[i] = saved >= 0 && saved < VALUES[i].length && shown(i) ? saved : DEFAULTS[i];
        }
    }

    float music() { return v[0] / 4f; }
    float sfx() { return v[1] / 4f; }
    /** 3D (GTA 2 style, buildings lean) or bird's-eye (flat, straight down). */
    boolean buildings3d() { return v[2] == 1; }
    void setBuildings3d(boolean on) { put(2, on ? 1 : 0); }
    boolean gore() { return v[3] == 1; }
    boolean healthBars() { return v[4] == 1; }
    boolean shake() { return v[5] == 1; }
    boolean showFps() { return v[6] == 1; }
    int maxPopulation() { return MAX_POP[v[7]]; }
    boolean vibration() { return v[14] == 1; }
    /** ((18.4) Heavy destruction: blasts tear buildings apart, and a collapse brings down what's next to it. */
    boolean heavyDestruction() { return v[16] == 1; }
    /** ((19.0) The levels of the ambience, the radio and the menus' and missions' sounds, 0 to 1. */
    float ambience() { return v[17] / 4f; }
    float radioVolume() { return v[18] / 4f; }
    float uiVolume() { return v[19] / 4f; }
    /** One channel for both ears. */
    boolean mono() { return v[20] == 1; }
    /** Night mode: the loud brought down, the quiet lifted. */
    boolean nightMode() { return v[21] == 1; }
    /** Words on the screen for the sounds that matter, with which way they came from. */
    boolean captions() { return v[22] == 1; }
    /** Ringing ears and dulled hearing after a blast close by. */
    boolean shellShock() { return v[23] == 1; }
    /** ((19.5) How much of the effects is drawn: 0 low to 3 ultra. */
    int effects() { return v[24]; }
    /** Flashes and the red of danger over the screen. */
    boolean screenEffects() { return v[25] == 1; }
    /** Each mode's own colour over the city. */
    boolean colourGrading() { return v[26] == 1; }
    /** ((12.0) How many radio messages show at once along the top.) */
    int feedLines() { return v[15] + 1; }
    boolean radio() { return v[8] == 1; }
    boolean minimap() { return v[9] == 1; }
    /** How big the buttons and text are compared to normal. */
    float uiScale() { return v[10] == 0 ? 0.85f : v[10] == 2 ? 1.2f : 1f; }
    /** Battery saver: 30 frames a second, and fewer still while paused. */
    boolean batterySaver() { return v[11] == 1; }
    /** Names above people when zoomed right in. */
    boolean nameTags() { return v[12] == 1; }
    /** Realistic graphics (textured ground, soft shadows, shading) or the classic flat look. */
    boolean realistic() { return v[13] == 1; }

    /** True if the player has not opened the patch notes since this version was installed. */
    boolean hasUnreadNotes() {
        return prefs.getInt("notesSeen", 0) < PatchNotes.VERSION_CODE;
    }

    boolean tutorialDone() {
        return prefs.getInt("tutorialDone", 0) == 1;
    }

    void markTutorialDone() {
        prefs.edit().putInt("tutorialDone", 1).apply();
    }

    void markNotesRead() {
        prefs.edit().putInt("notesSeen", PatchNotes.VERSION_CODE).apply();
    }

    private static boolean shown(int raw) {
        for (int k : SHOWN) if (k == raw) return true;
        return false;
    }

    @Override public int count() { return SHOWN.length; }
    @Override public String label(int i) { return LABELS[SHOWN[i]]; }
    @Override public String[] values(int i) { return VALUES[SHOWN[i]]; }
    @Override public int get(int i) { return v[SHOWN[i]]; }

    /** (12.0) Every setting back as it comes. */
    void reset() {
        for (int i = 0; i < v.length; i++) put(i, DEFAULTS[i]);
    }

    @Override
    public void set(int i, int value) {
        put(SHOWN[i], value);
    }

    /** By the setting's own index (KEYS), shown or not. */
    void put(int raw, int value) {
        v[raw] = value;
        prefs.edit().putInt(KEYS[raw], value).apply();
    }
}
