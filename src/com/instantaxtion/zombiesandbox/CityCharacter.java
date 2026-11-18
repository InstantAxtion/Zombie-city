package com.instantaxtion.zombiesandbox;

import java.util.Random;

/**
 * ((27.5) Every city its own, on map version 40.) The few big choices that make one seed's city unlike the next, drawn
 * once from numbers of their own so nothing else in the build shifts: how fine or coarse the street grid is and how
 * often a street jogs at a junction, how much of the town is laid out as neighbourhoods, which kinds of district it
 * leans to, where downtown sits and how far it spreads, whether it has a great park and where, which corner the
 * airport takes, and the colours of its homes.
 */
final class CityCharacter {
    /** The district mix it leans to. */
    static final int MIX_BALANCED = 0, MIX_INDUSTRIAL = 1, MIX_GREEN = 2, MIX_HISTORIC = 3, MIX_SPRAWL = 4, MIX_CAMPUS = 5,
            MIX_COMMERCE = 6, MIX_COUNT = 7;

    final Random r;
    /** Block size, times the usual (smaller is a finer grid). */
    final float grain;
    /** How often a street doesn't line up across a junction (0: a clean grid). */
    final float jog;
    /** How much more (or less) of the town is laid out as neighbourhoods. */
    final float nbRate;
    final int mix;
    /** Where downtown is, as a share of the town's size off its middle, and how far it spreads (1 as before). */
    final float dtX, dtY, core;
    /** A great park: where (a share of the town's size from its corner), or none. */
    final boolean bigPark;
    final float parkX, parkY;
    /** The airport in the top right instead of the top left. */
    final boolean airportRight;
    /** The homes' colours: an index into Neighbourhoods.PALETTES. */
    final int palette;
    /** More (or fewer, and so bigger) districts than the usual for the size of town. */
    final int moreDistricts;

    CityCharacter(long seed) {
        r = new Random(seed * 0x9E3779B97F4A7C15L + 40);
        grain = 0.75f + r.nextFloat() * 0.27f;
        float j = r.nextFloat();
        jog = j < 0.45f ? 0 : j < 0.8f ? 0.18f : 0.38f;
        nbRate = 0.5f + r.nextFloat() * 0.7f;
        mix = r.nextInt(MIX_COUNT);
        float a = r.nextFloat() * (float) Math.PI * 2, d = 0.08f + r.nextFloat() * 0.18f;
        dtX = (float) Math.cos(a) * d;
        dtY = (float) Math.sin(a) * d;
        core = 0.65f + r.nextFloat() * 0.9f;
        bigPark = r.nextFloat() < 0.6f;
        parkX = 0.2f + r.nextFloat() * 0.6f;
        parkY = 0.2f + r.nextFloat() * 0.6f;
        airportRight = r.nextBoolean();
        palette = r.nextInt(Neighbourhoods.PALETTES.length);
        moreDistricts = r.nextInt(6) - 2;
    }

    /** How much more likely a district is of this type, in a city leaning this way. */
    float weight(int t) {
        switch (mix) {
            case MIX_INDUSTRIAL: return t == City.DT_INDUSTRIAL ? 2.6f : t == City.DT_PARKSIDE ? 0.5f : 1;
            case MIX_GREEN: return t == City.DT_PARKSIDE ? 2.8f : t == City.DT_SUBURB ? 1.3f : t == City.DT_INDUSTRIAL ? 0.4f : 1;
            case MIX_HISTORIC: return t == City.DT_OLDTOWN ? 3.2f : t == City.DT_CAMPUS ? 1.4f : 1;
            case MIX_SPRAWL: return t == City.DT_SUBURB ? 2.4f : t == City.DT_MIDTOWN ? 0.6f : 1;
            case MIX_CAMPUS: return t == City.DT_CAMPUS ? 3.5f : t == City.DT_PARKSIDE ? 1.3f : 1;
            case MIX_COMMERCE: return t == City.DT_MIDTOWN ? 2f : t == City.DT_DOWNTOWN ? 1.6f : t == City.DT_SUBURB ? 0.7f : 1;
            default: return 1;
        }
    }
}
