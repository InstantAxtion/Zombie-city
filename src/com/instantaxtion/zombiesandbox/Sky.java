package com.instantaxtion.zombiesandbox;

import java.util.Random;

/**
 * ((20.0) The sky over the city: the time of day and the weather, and what they do to everyone under it. Night
 * shortens how far the living see and how well they shoot (less under street lights, not at all by a flare), while
 * the dead see a little less but move quicker and hear further. Rain muffles sound and dims sight; fog hides
 * everything beyond a street's width; a storm is heavy rain with thunder that stirs the dead. The world asks this
 * class for the numbers; nothing in here touches the simulation except through World's own methods.
 */
final class Sky {
    /** The Time setting: the full cycle, or held at noon or at midnight. */
    static final int TIME_CYCLE = 0, TIME_DAY = 1, TIME_NIGHT = 2;
    /** The Weather setting: changing by itself, or held at one kind (the kind + 1). */
    static final int W_DYNAMIC = 0;
    static final int CLEAR = 0, RAIN = 1, FOG = 2, STORM = 3;
    static final String[] NAMES = {"Clear", "Rain", "Fog", "Storm"};
    /** A whole day, in seconds of play: about seven minutes of daylight, five of dusk, night and dawn. */
    static final float DAY_SECONDS = 720;
    /** A new game starts at eight in the morning. */
    static final float START = 8 / 24f;
    private static final float DAWN0 = 0.23f, DAWN1 = 0.31f, DUSK0 = 0.77f, DUSK1 = 0.85f;

    /** The time of day: 0 midnight, 0.5 noon. */
    float clock = START;
    /** Dawns seen since the game began. */
    int days;
    int timeMode, weatherMode;
    int weather = CLEAR;
    /** Seconds until changing weather next changes. */
    float weatherLeft = 150;
    /** How hard it's raining and how thick the fog is, 0 to 1: eased, so weather comes in and clears. */
    float rain, fog;
    /** A lightning flash, 1 fading to 0 (for the screen). */
    float flash;
    private float strikeCd = 6;
    private boolean wasNight;
    private final Random rnd;

    Sky(long seed) {
        rnd = new Random(seed);
        weatherLeft = 150 + rnd.nextFloat() * 120;
    }

    void configure(int timeMode, int weatherMode) {
        this.timeMode = timeMode;
        this.weatherMode = weatherMode;
        if (timeMode == TIME_DAY) clock = 0.5f;
        else if (timeMode == TIME_NIGHT) clock = 0;
        if (weatherMode != W_DYNAMIC) {
            weather = weatherMode - 1;
            rain = rainTarget();
            fog = weather == FOG ? 1 : 0;
        }
        wasNight = night();
    }

    /** After loading a saved sky: no announcement of a night that had already fallen. */
    void resume() {
        wasNight = night();
    }

    /** How much daylight there is: 1 in the day, 0 at night, between at dawn and dusk. */
    float light() {
        float c = clock;
        if (c < DAWN0 || c >= DUSK1) return 0;
        if (c < DAWN1) return (c - DAWN0) / (DAWN1 - DAWN0);
        if (c < DUSK0) return 1;
        return 1 - (c - DUSK0) / (DUSK1 - DUSK0);
    }

    float dark() {
        return 1 - light();
    }

    boolean night() {
        return light() < 0.4f;
    }

    /** Nothing in the sky changing anything: broad daylight and clear. */
    boolean plain() {
        return rain < 0.01f && fog < 0.01f && light() >= 1;
    }

    private float rainTarget() {
        return weather == RAIN ? 0.65f : weather == STORM ? 1 : 0;
    }

    /** Seconds until night next falls (0 at night, or never with the time held). */
    float untilNight() {
        if (timeMode != TIME_CYCLE || night()) return 0;
        float at = DUSK0 + (DUSK1 - DUSK0) * 0.6f;
        float d = at - clock;
        if (d < 0) d += 1;
        return d * DAY_SECONDS;
    }

    /** Seconds until dawn (0 by day, or never with the time held). */
    float untilDawn() {
        if (timeMode != TIME_CYCLE || !night()) return 0;
        float at = DAWN0 + (DAWN1 - DAWN0) * 0.4f;
        float d = at - clock;
        if (d < 0) d += 1;
        return d * DAY_SECONDS;
    }

    void update(World w, float dt) {
        if (timeMode == TIME_CYCLE) {
            clock += dt / DAY_SECONDS;
            if (clock >= 1) clock -= 1;
        }
        boolean n = night();
        if (n != wasNight) {
            wasNight = n;
            if (n) w.nightFell();
            else {
                days++;
                w.dawnBroke();
            }
        }
        if (weatherMode == W_DYNAMIC) {
            weatherLeft -= dt;
            if (weatherLeft <= 0) {
                int was = weather;
                float roll = rnd.nextFloat();
                // (Clear more often than not; a storm tends to follow rain, and to end in it.)
                if (was == STORM) weather = roll < 0.6f ? RAIN : CLEAR;
                else if (was == RAIN) weather = roll < 0.3f ? STORM : roll < 0.85f ? CLEAR : FOG;
                else if (was == FOG) weather = roll < 0.8f ? CLEAR : RAIN;
                else weather = roll < 0.45f ? RAIN : roll < 0.7f ? FOG : roll < 0.8f ? STORM : CLEAR;
                weatherLeft = (weather == CLEAR ? 200 : weather == STORM ? 80 : 120) + rnd.nextFloat() * 120;
                if (weather != was) w.weatherChanged(was, weather);
            }
        }
        float rt = rainTarget(), ft = weather == FOG ? 1 : 0, ease = dt / 15f;
        rain += Math.max(-ease, Math.min(ease, rt - rain));
        fog += Math.max(-ease, Math.min(ease, ft - fog));
        if (flash > 0) flash = Math.max(0, flash - dt * 2.5f);
        if (weather == STORM && rain > 0.8f) {
            strikeCd -= dt;
            if (strikeCd <= 0) {
                strikeCd = 5 + rnd.nextFloat() * 10;
                flash = 1;
                w.thunder(rnd.nextFloat() * w.city.worldW(), rnd.nextFloat() * w.city.worldH());
            }
        }
    }

    /** Sets the weather from now on (the sandbox's tool): it comes in over a few seconds. */
    void setWeather(int kind) {
        weather = kind;
        weatherLeft = 180 + rnd.nextFloat() * 120;
    }

    /** Jumps the clock to nightfall or to morning (the sandbox's tool). */
    void skipTo(boolean toNight) {
        if (timeMode != TIME_CYCLE) timeMode = TIME_CYCLE;
        clock = toNight ? DUSK1 - 0.02f : DAWN1 - 0.03f;
    }

    // ---- What it does ----

    /** How far the living see, of the usual: lit is 0 in the dark, 1 by a flare, between under street lights. */
    float sightLiving(float lit) {
        return (1 - 0.45f * dark() * (1 - lit)) * (1 - 0.5f * fog) * (1 - 0.15f * rain);
    }

    /** How far the dead see: they never relied on their eyes much. */
    float sightDead() {
        return (1 - 0.2f * dark()) * (1 - 0.4f * fog) * (1 - 0.1f * rain);
    }

    /** How far a noise carries: further in the still of the night, less through rain. */
    float hearing() {
        return (1 + 0.2f * dark()) * (1 - 0.3f * rain);
    }

    /** How quick the dead are: bolder in the dark. */
    float pace() {
        return 1 + 0.12f * dark();
    }

    /** How well the living shoot. */
    float aim(float lit) {
        return (1 - 0.18f * dark() * (1 - lit)) * (1 - 0.12f * fog) * (1 - 0.08f * rain);
    }

    /** The time as a clock reads it. */
    String timeText() {
        int mins = (int) (clock * 24 * 60) % (24 * 60);
        int h = mins / 60, m = mins % 60;
        return (h < 10 ? "0" : "") + h + ":" + (m < 10 ? "0" : "") + m;
    }

    String weatherText() {
        return rain > 0.3f && weather == CLEAR ? "Clearing" : NAMES[weather];
    }
}
