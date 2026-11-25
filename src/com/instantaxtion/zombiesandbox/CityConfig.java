package com.instantaxtion.zombiesandbox;

import java.util.Random;

/**
 * The "New Game" settings. The player picks a map preset, the map size and who is in the city; everything
 * about how the preset's city is built (building style, density, layout, stations, base) comes from the preset.
 */
final class CityConfig implements OptionSet {
    static final String[] PRESETS = {"Classic", "Downtown", "Suburbs", "Industrial", "Parkland", "Old Town",
            "Small Town", "Campus", "Metropolis", "Village", "Downtown Towers", "Military Town", "Lakeside", "Harbour", "Islands",
            // ((12.7) The places of maps taken out of the game, the real cities among them, are kept so the others keep
            // their numbers: see removed.) ((15.6) The first of them is the Prison Island now.)
            "Prison Island", "River City",
            // ((22.9) Desert City, in the old Seattle's place.)
            "Desert City",
            // ((25.0) Theme Park Island, in the old New York's place.)
            "Theme Park Island",
            // ((26.0) Space Center, in the old Sydney's place.)
            "Space Center",
            // ((28.0) Highway Crossroads, in the old Tokyo's place.)
            "Highway Crossroads", "Border Town",
            "Military Airbase",
            // (10.60)
            "Archipelago",
            // ((28.0) Canyon Town, in the old Mountain Valley's place.)
            "Canyon Town",
            "Bridge City", "Swamp Bayou", "Lighthouse Coast", "Stadium City",
            // ((28.0) Frozen North.)
            "Frozen North"};
    /** One line about each map, shown on the New Game screen. */
    static final String[] PRESET_INFO = {
            "A bit of everything: towers, suburbs and parks.",
            "Office towers and shopping streets.",
            "Streets of houses with yards and schools.",
            "Warehouses and truck yards.",
            "Big parks, sports fields and low buildings.",
            "Narrow streets of shops, churches and old houses.",
            "A quiet little town with a main street.",
            "Colleges, sports fields and student flats.",
            "A huge, dense city of towers, with an airport.",
            "Cottages among fields. Help is far away.",
            "Skyscrapers packed onto narrow streets, with an airport.",
            "A huge army base in the middle, the town round its gates. Far more soldiers.",
            "Homes and parks round a lake.",
            "A port of docks, cranes and container stacks.",
            "Two to four islands joined by bridges, each its own kind of town: city, suburbs, port, old town.",
            "A coastal town with a prison on an island in the bay. The outbreak starts inside its walls.",
            "A river through the middle of town, crossed by a few bridges.",
            "A desert town with a casino strip, oases and an army test range.",
            "A resort island with a theme park, beach hotels and a ferry, and the staff town across the strait.",
            "A rocket on its pad, the assembly building, mission control and a landing strip, beside a small town.",
            "Two highways cross out in open country, with truck stops, motels and a small town nearby.",
            "A town split by a fenced border, crossed at a few checkpoints.",
            "A huge air base of runways and hangars, its town at the gate. Far more soldiers.",
            "A tropical island city ringed by five palm-fringed village islands, all joined by bridges.",
            "A red-rock town on a mesa and the canyon floor, split by a deep gorge with few bridges.",
            "A city on many small islands in a bay, joined by long bridges.",
            "A bayou town of stilt houses and boardwalks, cut off by the swamp.", "Fishing villages along a winding cliff road, a harbour town and a lighthouse.",
            "A city round a huge stadium, its arena, fan zones and training pitches, with a light rail to downtown.",
            "An icebound harbour town with an airstrip and long snowy roads out to cabins and a radar station.",
    };

    static final int STYLE_MIXED = 0, STYLE_OFFICES = 1, STYLE_HOUSES = 2, STYLE_WAREHOUSES = 3;

    static final int OPT_PRESET = 0, OPT_SIZE = 1, OPT_CIVILIANS = 2, OPT_COPS = 3, OPT_MILITARY = 4,
            OPT_ZOMBIES = 5, OPT_RESERVES = 6, OPT_COUNTRY = 7;
    /** The country round the town (10.20): not one of the eight in v (it's kept in outskirts). */
    static final int OPT_OUTSKIRTS = 8;
    private static final String[] OUTSKIRTS = {"Default", "Little nature"};
    /** (10.47) Whether the people flee the city once the news breaks (not one of the eight in v either). */
    static final int OPT_EXODUS = 9;
    private static final String[] EXODUS = {"Off", "On foot", "Bus & train", "All ways"};
    /**
     * ((32.3) Civilians on or off, on the New Game screen (not one of the eight in v: it sets OPT_CIVILIANS to None, or
     * back to the usual share). Off, the police and army are half as many again (forceCut).
     */
    static final int OPT_NO_CIV = 11;
    private static final String[] CIVS = {"On", "Off"};

    /** ((32.3) A city with no civilians in it. */
    boolean noCivilians() { return v[OPT_CIVILIANS] == 0; }
    /** ((20.0) How hard the game is. ((21.1) Not a choice: always Normal since the campaign's missions went in 28.2.) */
    int difficulty = Director.NORMAL;
    private static final String[] LABELS = {"Map", "Map size", "Civilians", "Cops", "Military", "Zombies",
            "Reinforcements", "Country"};
    private static final String[][] VALUES = {
            PRESETS,
            // (Since 10.18 the three sizes offered are called Default, Large and Massive.) ((16.1) Now Default, Big and
            // Large: the Gigantic, the Colossal and the new Vast; the old Default and Large kept for old city codes.)
            {"Tiny", "Small", "Medium", "Standard", "Old large", "Massive", "Default", "Big", "Large"},
            {"None", "Few", "Some", "Most", "All"},
            {"0", "5", "10", "20", "40", "60"},
            {"0", "5", "10", "20", "40"},
            {"0", "1", "5", "20", "50", "100", "200"},
            // (Since 10.20 help from outside is just on or off: the old Low and Medium count as On.)
            {"Off", "On (Low)", "On (Medium)", "On"},
            Country.NAMES,
    };
    /** Tiles across for each size; cities built before 10.10 (no Tiny or Huge then) were a quarter smaller. */
    private static final int[] SIZES = {160, 240, 320, 400, 560, 704, 1024, 1280, 1810};
    private static final int[] OLD_SIZES = {192, 192, 256, 320, 448, 448, 448, 448, 448};
    /** The size settings, by name. */
    static final int TINY = 0, SMALL = 1, MEDIUM = 2, LARGE = 3, MASSIVE = 4, HUGE = 5, GIGANTIC = 6;
    /** (13.0) The biggest map yet: 1280 tiles across, a quarter wider again than Gigantic. */
    static final int COLOSSAL = 7;
    /**
     * ((16.1) Bigger again: twice the ground of the Colossal (1810 tiles across), with the same number of people, so a
     * city spread out with room between. On the New Game screen the three offered are now called Default (the
     * Gigantic), Big (the Colossal) and Large (this).)
     */
    static final int VAST = 8;
    /** Share of the city's residents out and about when the game starts. */
    private static final float[] RESIDENT_SHARE = {0, 0.2f, 0.45f, 0.7f, 1f};
    private static final int[] ZOMBIES = {0, 1, 5, 20, 50, 100, 200};

    // How each preset's city is built:  style, density, height, parks, traffic, layout, stations, base
    private static final int[][] MAPS = {
            {STYLE_MIXED, 1, 1, 2, 1, 1, 1, 1},       // Classic
            {STYLE_OFFICES, 2, 3, 1, 2, 0, 2, 0},     // Downtown
            {STYLE_HOUSES, 1, 0, 2, 1, 2, 1, 0},      // Suburbs
            {STYLE_WAREHOUSES, 1, 1, 0, 2, 1, 1, 1},  // Industrial
            {STYLE_MIXED, 0, 1, 3, 1, 2, 1, 0},       // Parkland
            {STYLE_HOUSES, 2, 0, 1, 1, 2, 1, 0},      // Old Town
            {STYLE_HOUSES, 1, 0, 2, 1, 1, 1, 0},      // Small Town
            {STYLE_MIXED, 1, 1, 3, 1, 2, 1, 0},       // Campus
            {STYLE_OFFICES, 2, 3, 1, 2, 1, 3, 1},     // Metropolis
            {STYLE_HOUSES, 0, 0, 3, 1, 2, 1, 0},      // Village
            {STYLE_OFFICES, 2, 3, 0, 2, 0, 3, 1},     // ((15.2) Downtown Towers, in the old Seaside's place)
            {STYLE_MIXED, 1, 1, 2, 1, 1, 1, 1},       // ((15.5) Military Town, in the old River City's place)
            {STYLE_HOUSES, 1, 0, 3, 1, 2, 1, 0},      // Lakeside
            {STYLE_MIXED, 1, 1, 1, 2, 0, 1, 1},       // Harbour
            {STYLE_MIXED, 1, 1, 2, 1, 1, 2, 1},       // Islands
            {STYLE_MIXED, 1, 1, 2, 1, 1, 2, 1},       // ((15.6) Prison Island, in the old Los Angeles' place; (15.8) with a base)
            {STYLE_MIXED, 1, 1, 2, 1, 1, 2, 1},       // ((22.0) River City, in the old Portland's place)
            {STYLE_HOUSES, 1, 0, 1, 1, 1, 2, 1},      // ((22.9) Desert City, in the old Seattle's place)
            {STYLE_MIXED, 1, 2, 3, 1, 1, 1, 0},       // ((25.0) Theme Park Island, in the old New York's place)
            {STYLE_HOUSES, 1, 1, 2, 1, 1, 1, 0},      // ((26.0) Space Center, in the old Sydney's place)
            {STYLE_HOUSES, 1, 0, 1, 1, 1, 1, 1},      // ((28.0) Highway Crossroads, in the old Tokyo's place)
            {STYLE_MIXED, 1, 1, 2, 1, 1, 2, 1},       // ((28.0) Border Town)
            {STYLE_MIXED, 1, 1, 2, 1, 1, 1, 1},       // ((28.0) Military Airbase)
            {STYLE_MIXED, 1, 2, 2, 1, 1, 2, 1},       // Archipelago
            {STYLE_MIXED, 1, 1, 1, 1, 1, 2, 1},       // ((28.0) Canyon Town, in the old Mountain Valley's place)
            {STYLE_MIXED, 2, 2, 1, 2, 0, 2, 1},       // ((28.0) Bridge City)
            {STYLE_HOUSES, 1, 0, 1, 1, 2, 1, 1},      // ((28.0) Swamp Bayou)
            {STYLE_HOUSES, 1, 0, 2, 1, 2, 1, 1},      // ((28.0) Lighthouse Coast)
            {STYLE_MIXED, 1, 2, 2, 1, 1, 2, 1},       // ((28.0) Stadium City)
            {STYLE_HOUSES, 1, 1, 1, 1, 1, 1, 1},      // ((28.0) Frozen North)
    };

    // Landmarks per medium map:  churches, schools, fire stations, supermarkets, gas stations, cemeteries,
    // then the share of building blocks that become rows of shops (percent).
    private static final int[][] LANDMARKS = {
            {1, 1, 1, 1, 1, 1, 25},   // Classic
            {1, 0, 1, 1, 0, 0, 55},   // Downtown
            {1, 1, 1, 1, 1, 1, 12},   // Suburbs
            {0, 0, 1, 0, 2, 0, 8},    // Industrial
            {1, 1, 0, 0, 0, 1, 10},   // Parkland
            {2, 1, 1, 1, 1, 1, 45},   // Old Town
            {1, 1, 1, 1, 2, 1, 35},   // Small Town
            {1, 3, 1, 1, 1, 0, 20},   // Campus
            {2, 2, 2, 2, 1, 0, 50},   // Metropolis
            {1, 1, 1, 1, 1, 1, 20},   // Village
            {1, 1, 2, 2, 1, 0, 12},   // Downtown Towers (few shopping rows: the blocks are towers)
            {1, 1, 1, 1, 2, 1, 30},   // Military Town
            {1, 1, 1, 1, 1, 1, 15},   // Lakeside
            {1, 1, 1, 1, 2, 0, 20},   // Harbour
            {2, 2, 2, 2, 2, 1, 30},   // Islands
            {2, 2, 2, 2, 2, 1, 40}, {2, 2, 2, 2, 1, 1, 40},
            {1, 1, 1, 1, 3, 1, 30},   // Desert City
            {1, 0, 1, 1, 1, 0, 35},   // Theme Park Island
            {1, 1, 1, 1, 1, 0, 30},   // Space Center
            {1, 0, 1, 0, 3, 0, 20},   // Highway Crossroads
            {1, 1, 1, 1, 2, 1, 35},
            {1, 1, 1, 1, 1, 1, 25},   // ((28.0) Military Airbase)
            {2, 2, 2, 2, 2, 1, 35},   // Archipelago
            {1, 1, 1, 1, 1, 1, 30},   // Canyon Town
            {2, 2, 2, 2, 2, 1, 40},   // ((28.0) Bridge City)
            {2, 1, 1, 1, 2, 1, 30},   // ((28.0) Swamp Bayou)
            {2, 1, 1, 1, 1, 1, 35},   // ((28.0) Lighthouse Coast)
            {1, 2, 1, 1, 1, 0, 40},   // ((28.0) Stadium City)
            {1, 1, 1, 1, 1, 1, 25},   // Frozen North
    };
    // Share of office lots that become apartment blocks, parking garages and pharmacies (percent).
    private static final int[][] BUILDING_MIX = {
            {15, 6, 5}, {15, 15, 5}, {20, 0, 6}, {5, 8, 2}, {15, 3, 6}, {10, 0, 8}, {10, 2, 8}, {20, 5, 6},
            {25, 15, 4}, {0, 0, 10}, {30, 18, 4}, {20, 8, 5}, {15, 2, 6}, {10, 6, 3}, {20, 8, 5},
            {20, 12, 5}, {20, 6, 5}, {8, 4, 6}, {35, 0, 5}, {20, 0, 5}, {35, 12, 5}, {35, 4, 6},
            {30, 8, 4},
            {20, 8, 5}, {12, 3, 6},
            {25, 10, 5}, {12, 3, 6}, {12, 3, 6}, {30, 0, 6},
            {15, 0, 5},
    };
    // Which kinds of park each map likes: park, playground, sports field, courts, garden, skatepark.
    private static final int[][] PARK_MIX = {
            {3, 2, 1, 1, 1, 1},
            {2, 1, 0, 2, 0, 2},
            {3, 3, 1, 1, 2, 0},
            {1, 0, 1, 1, 0, 2},
            {4, 2, 2, 1, 2, 1},
            {3, 1, 0, 0, 3, 0},
            {3, 2, 2, 1, 2, 1},
            {2, 1, 4, 4, 1, 1},
            {2, 1, 1, 3, 0, 2},
            {4, 2, 2, 0, 3, 0},
            {3, 2, 1, 2, 2, 1},
            {3, 1, 1, 2, 2, 1},
            {4, 2, 1, 1, 3, 0},
            {2, 1, 1, 1, 0, 2},
            {3, 2, 1, 2, 2, 1},
            {3, 2, 1, 2, 1, 2}, {4, 2, 1, 1, 2, 1}, {4, 1, 1, 2, 1, 2}, {3, 3, 0, 2, 3, 0}, {3, 2, 2, 1, 2, 1},
            {2, 1, 1, 1, 3, 0}, {3, 2, 1, 2, 1, 1},
            {2, 1, 3, 2, 0, 0},
            {3, 2, 1, 2, 2, 1}, {4, 2, 1, 1, 3, 0},
            {3, 2, 1, 2, 2, 1}, {4, 2, 1, 1, 3, 0}, {4, 2, 1, 1, 3, 0}, {3, 2, 3, 3, 0, 1},
            {3, 2, 1, 1, 0, 0},
    };

    /** Current index into VALUES for each option. */
    final int[] v = {METROPOLIS, LARGE, PEOPLE_ABOUT, 2, 1, 0, 3, 0};
    /** The map a new game starts on (since Classic went in 10.18). */
    static final int METROPOLIS = 8;

    /**
     * Choices no longer offered on the New Game screen (10.17): five of the maps (Industrial, Parkland, Old
     * Town, Small Town and Harbour), the three smallest map sizes and seven of the countries. Cities from older
     * saves and shared codes that use them still build as they were.
     */
    /**
     * ((26.1) What to call this city's map where the player sees it: the map's name if it's one on offer, else nothing.)
     */
    String mapLabel() {
        int p = v[OPT_PRESET];
        return retired(OPT_PRESET, p) ? null : PRESETS[p];
    }

    static boolean retired(int option, int value) {
        // (The real cities went in 10.28.)
        // (Downtown, Village and Lakeside went in 10.50.)
        if (option == OPT_PRESET) return value == 0 || value == 1 || value == 3 || value == 4 || value == 5 || value == 6 || value == 7
                || value == 9 || value == 12 || value == 13
                // ((27.5) And three more maps went in 27.5: 10, 11 and 16. Their cities still build for old saves and codes.)
                || value == 10 || value == 11 || value == 16
                // ((28.5) And five of 28.0's: Highway Crossroads, Border Town, the Military Airbase, Canyon Town and Stadium
                // City. Their cities still build for old saves and codes.)
                || value == CROSSROADS || value == BORDER_TOWN || value == AIRBASE || value == CANYON || value == STADIUM_CITY
                // ((31.0) And Theme Park Island. It still builds for old saves and codes.)
                || value == THEME_PARK
                // ((31.1) And the Space Center and the Frozen North. They still build for old saves and codes.)
                || value == SPACE_CENTER || value == FROZEN_NORTH
                || removed(value);
        // (The number of zombies at the start isn't a choice any more: the game starts quiet.)
        if (option == OPT_ZOMBIES) return value != 0;
        // ((13.0) Large and Massive are gone too: Default, Gigantic and the new Colossal.)
        // ((16.1) And the old Default too: Default, Big and Large are the Gigantic, Colossal and Vast.)
        if (option == OPT_SIZE) return value < GIGANTIC;
        if (option == OPT_COUNTRY) return value != Country.USA;
        // ((29.0) Reinforcements are always on now.)
        if (option == OPT_RESERVES) return value != RESERVES_ON;
        return false;
    }
    /**
     * The city's seed: a new one each launch (the time-seeded Random) and on every visit to the New Game screen (Menu calls
     * newSeed). ((27.5) From a billion, not a million: codes and saves already carry any long.)
     */
    long seed = new Random().nextInt(SEEDS);
    static final int SEEDS = 1000000000;
    /** Start the next game on the city from {@link #code()} instead of a new random one. */
    boolean keepCity;
    /**
     * The country round the town (10.20): 0 as it comes (woods, hills, mountains), 1 little nature (open fields
     * and farmland, few trees, no mountains). Part of the city code ("!" at the end).
     */
    int outskirts;
    /**
     * (10.47) Fleeing citizens: 0 off (everyone sits it out in town, as before); otherwise, once the news breaks,
     * most of the people leave the city, and come home once the outbreak is over - 1 on foot, 2 (10.49) by
     * evacuation bus and train only, 3 by bus or train if a stop is near, on foot if not. (In 10.48, 2 and 3 were
     * by car.) Not part of the city code.
     */
    int exodus;
    static final int EXODUS_OFF = 0, EXODUS_FOOT = 1, EXODUS_TRANSIT = 2, EXODUS_ALL = 3;

    boolean exodus() { return exodus != EXODUS_OFF; }

    boolean littleNature() { return (outskirts == 1 || crossroads()) && civic >= 7; }
    /**
     * Which public buildings the city was laid out with: 0 as before 10.4, 1 the government quarter (10.4),
     * 2 and the National Guard armory (10.5), 3 the bigger maps of 10.10 with hills, mountains, lakes, streams
     * and nature parks out in the country, 4 the wilds of 10.14 (streams you can wade, trails that cross the
     * lanes, a ranger station and fire lookout towers), 5 the lanes of 10.18 (no stretch of tarmac in the
     * middle of a dirt lane) and the real cities, 6 the driveways of 10.19, 7 the helipads and busier outskirts of 10.20, 8 dirt lanes that stay dirt into town (10.23), 9 streets that line up, more kinds of road and hills in town (10.24), 10 curved streets in the suburbs (10.25), 11 farm fields and park paths kept clear (10.26), 12 the ring road, wider avenues and stations on the road network (10.28), 13 highway interchanges with slip roads instead of crossroads (10.29), 14 fuller country on the Massive maps with a big army base and its checkpoints (10.30), 15 one-way slip roads that run in alongside the highway, and the Gigantic size (10.31), 16 bridges over the highway at the interchanges, the army base checkpoints (10.32), 17 nothing joining the highway but its slip roads, and fuller maps (10.33), 18 every road joined up, and livelier maps (10.34), 21 another police station on every map from the Default size up, 22 the beltway right round the map, 23 an army base out in the country from the Default size up (bigger on the Massive maps) and a police headquarters, 24 buildings of more shapes than a box (10.52), 25 about the same number of people on every map (10.63), 28 winding neighbourhoods on more of the map and homes set back by different amounts (11.5), 29 the airport on the big city maps (16.0), 30 no hospital (16.5), 31 no fire stations (16.9), 40 every city its own (27.5: its grid, district mix, downtown, park, colours and the new neighbourhoods), 41 police stations on every island (28.0), 42 the quick reaction force's base near the middle of town (28.7), 43 more town on the mostly-country maps (29.0), 44 every map but the island ones a fifth smaller across (29.7), 45 Prison Island and Theme Park Island too (29.8). Saved games rebuild with what they had.
     */
    // ((31.2) 52: no recruitment, and no medevac - the wounded fight on (World.noRecruits, World.noMedevac).)
    // ((32.0) 53: the mass outbreak's balance (Balance).)
    // ((32.1) 54: a quarter more police, soldiers and help from outside (forceCut).)
    // ((32.2) 55: slow zombies (World.slowDead).)
    // ((32.3) 56: the police hold the perimeter, the army does the killing.)
    // ((32.4) 57: help from outside sized for tens of thousands of the dead (Dispatch.massHelp, the National Guard).)
    // ((32.7) 58: no safe zones (Dispatch).)
    // ((32.7) 59: no house interiors (City.enterable, BuildingRenderer.drawInsides).)
    int civic = 59;

    /** ((16.9) Fire stations: only in cities built before 16.9. There's been no fire service since 10.49. */
    boolean fireStations() { return civic < 31; }

    /** ((28.7) The quick reaction force's small base near the middle of town (map version 42, wherever there's an army). */
    /**
     * ((29.0) Map version 43: more town on the maps that were mostly country - Lighthouse Coast's harbour town and
     * villages bigger and one more village, Swamp Bayou's town bigger with more and bigger hamlets, Frozen North's town
     * wider and deeper, Desert City's spreading over more of the desert.)
     */
    boolean moreTown() { return civic >= 43; }

    boolean qrfBase() { return civic >= 42 && militaryBase() && !bigGarrison(); }

    /** ((16.9) An FBI field office in town (cities built since 16.9). */
    boolean fbiOffice() { return civic >= 31; }

    /** ((21.4) Map version 32: three or four small islands round the Archipelago's big one.) */
    boolean moreIslands() { return civic >= 32; }

    /** ((22.0) Map version 33: a second mall and a walled prison on the bigger maps, malls that shelter more. */
    boolean v22() { return civic >= 33; }

    /** ((22.4) Map version 35: one-of-a-kind landmarks, fewer car parks, roads that stop short of the river. */
    boolean landmarks() { return civic >= 35; }

    /** ((16.5) A hospital in town: only in cities built before 16.5. It had nothing left to do. */
    boolean hospital() { return civic < 30; }

    /** Hills, mountains, streams, lakes, parks and campsites (cities built since 10.10). */
    boolean nature() { return civic >= 3; }

    /** Fords, trails that cross the lanes and the railway, the ranger station and fire towers (since 10.14). */
    boolean wilds() { return civic >= 4; }

    /** Country lanes that stay dirt all the way (since 10.18). */
    boolean dirtLanes() { return civic >= 5; }

    /** Driveways out to the road from houses and farms that open onto nothing but grass (since 10.19). */
    boolean driveways() { return civic >= 6; }

    /** Helipads for the police and rescue helicopters, livelier outskirts with their own small towns (since 10.20). */
    boolean servicePads() { return civic >= 7; }

    int preset() {
        return v[OPT_PRESET];
    }


    /** Water on the map: none, the sea along one side (a beach or a port), a river, or a lake. */
    static final int W_NONE = 0, W_LAKE = 3, W_HARBOUR = 4, W_ISLANDS = 5, W_RIVER = 6;
    /** ((28.0) Water laid out by the map's own class: the swamp's bayous, the coast's sea. */
    static final int W_SHORE = 7;
    /** ((22.0) River City. */
    static final int RIVER_CITY = 16;
    /** The Islands map, and its one size (tiles across: a bit bigger than Massive). */
    static final int ISLANDS = 14, ISLANDS_TILES = 1280;
    /** (10.60) The Archipelago (always one size, like Islands). */
    static final int ARCHIPELAGO = 23, ARCHIPELAGO_TILES = 1280;

    /**
     * (11.3) Maps taken out of the game altogether, code and all: Sydney, Tokyo and Paris (their countries are
     * gone), Seaside, River City, the Garrison State, the Mountain Valley, Bay City, Delta Marsh, Fjord Town and
     * Canal City; ((12.7) and the last of the real cities, Los Angeles, Portland, Seattle and New York). Their
     * numbers stay taken so the other maps keep theirs; saves and city codes on them no longer load.
     */
    static boolean removed(int preset) {
        // ((11.5) Suburbs (2) too.)
        // ((15.2) Not 10 any more: it's Downtown Towers now.)
        // ((15.5) Nor 11: it's the Military Town.)
        // ((15.6) Nor 15: the Prison Island.)
        // ((22.0) Nor 16: River City.)
        // ((22.9) Nor 17: Desert City.)
        // ((25.0) Nor 18: Theme Park Island.)
        // ((26.0) Nor 19: the Space Center.)
        // ((28.0) Nor 20 and 24: Highway Crossroads and Canyon Town.)
        // ((28.0) Nor 21, 22 and 28: Border Town, the Military Airbase and Stadium City; nor 25, 26 and 27: Bridge City,
        // Swamp Bayou and Lighthouse Coast.)
        return preset == 2;
    }

    boolean archipelago() { return v[OPT_PRESET] == ARCHIPELAGO; }

    /**
     * ((22.9) Desert City: sand for grass, cacti, palms and scrub for trees, low adobe sprawl on wide roads, a casino
     * strip down the main boulevard, oases, and the army base out in the desert with its test range. Everything it
     * changes in the building of a city is for this map alone, so every other map builds exactly as before.)
     */
    static final int DESERT = 17;

    boolean desert() { return v[OPT_PRESET] == DESERT; }

    /**
     * ((25.0) Theme Park Island: a resort island out in the sea, the theme park of rides in the middle of it, beach
     * hotels round its shore, one bridge across the strait to the mainland (and the staff town there), and a ferry.
     * Everything it changes in the building of a city is for this map alone, so every other map builds as before.)
     */
    static final int THEME_PARK = 18;

    boolean themePark() { return v[OPT_PRESET] == THEME_PARK; }

    /**
     * ((26.0) The Space Center: the space center itself along one side of the map (launch pads with a rocket standing
     * by its service tower, the Vehicle Assembly Building, the crawlerway out to the pads, mission control, labs, offices,
     * fuel tanks and a landing strip, in open scrub and marsh), and the small staff town on the rest. Everything it
     * changes in the building of a city is for this map alone, so every other map builds as before.)
     */
    static final int SPACE_CENTER = 19;

    boolean spaceCenter() { return v[OPT_PRESET] == SPACE_CENTER; }

    /**
     * ((28.0) Highway Crossroads, Canyon Town and Frozen North: each laid out and painted by a class of its own
     * (RegionMap). Everything they change in the building of a city is for them alone.)
     */
    static final int CROSSROADS = 20, CANYON = 24, FROZEN_NORTH = 29;

    boolean crossroads() { return v[OPT_PRESET] == CROSSROADS; }

    boolean canyon() { return v[OPT_PRESET] == CANYON; }

    boolean frozenNorth() { return v[OPT_PRESET] == FROZEN_NORTH; }

    /**
     * ((28.0) Three maps of their own, each built by its own class (BorderMap, AirbaseMap, StadiumMap): a town split by
     * a fenced border with its crossings; a great air base with its town at the gate; a city round a stadium complex.
     */
    static final int BORDER_TOWN = 21, AIRBASE = 22, STADIUM_CITY = 28;

    boolean borderTown() { return v[OPT_PRESET] == BORDER_TOWN; }

    boolean airbase() { return v[OPT_PRESET] == AIRBASE; }

    boolean stadiumCity() { return v[OPT_PRESET] == STADIUM_CITY; }

    /** ((28.0) A map whose army far outnumbers the usual: the Military Town and the Military Airbase. */
    boolean bigGarrison() { return militaryTown() || airbase(); }

    /**
     * ((28.0) Bridge City (many small islands in a bay joined by long bridges), Swamp Bayou (a town on the edge of a
     * swamp: stilt houses, boardwalks, causeways) and Lighthouse Coast (fishing villages along a cliff road, a harbour
     * town, a lighthouse). Each is built by its own class (see WaterMap); every other map builds as before.)
     */
    static final int BRIDGE_CITY = 25, SWAMP = 26, LIGHTHOUSE = 27;

    boolean bridgeCity() { return v[OPT_PRESET] == BRIDGE_CITY; }

    boolean swamp() { return v[OPT_PRESET] == SWAMP; }

    boolean lighthouse() { return v[OPT_PRESET] == LIGHTHOUSE; }

    /** ((15.2) The Downtown Towers map: skyscrapers on narrow streets.) */
    static final int TOWERS = 10;

    boolean towers() { return v[OPT_PRESET] == TOWERS; }

    /** ((15.5) The Military Town map: a huge base in the middle, the town round its gates.) */
    static final int MILITARY_TOWN = 11;

    boolean militaryTown() { return v[OPT_PRESET] == MILITARY_TOWN; }

    /** ((15.6) The Prison Island map: a coastal town, and a walled prison on an island in the bay.) */
    static final int PRISON_ISLAND = 15;

    boolean prisonIsland() { return v[OPT_PRESET] == PRISON_ISLAND; }

    /** ((15.10) An airport on the edge of town: on the big city maps, from the Default size up.) */
    boolean airport() { return (v[OPT_PRESET] == METROPOLIS || v[OPT_PRESET] == TOWERS) && v[OPT_SIZE] >= MEDIUM && civic >= 29; }

    /** Islands only comes in one size: it counts as Massive for everything that depends on size. */
    void normalize() {
        // ((18.2) A prison break is on the Prison Island: it's the map with the prison.)
        // ((13.0) The island maps come at the biggest size: Colossal, 1280 tiles across (640 and 720 before).)
        if (v[OPT_PRESET] == ISLANDS || v[OPT_PRESET] == ARCHIPELAGO) v[OPT_SIZE] = COLOSSAL;
        if (removed(v[OPT_PRESET])) v[OPT_PRESET] = METROPOLIS;
        v[OPT_COUNTRY] = Country.USA;
    }

    int water() {
        switch (v[OPT_PRESET]) {
            case 12: return W_LAKE;
            case 13: case PRISON_ISLAND: return W_HARBOUR;
            case ISLANDS: case ARCHIPELAGO: case THEME_PARK: case BRIDGE_CITY: return W_ISLANDS;
            case SWAMP: case LIGHTHOUSE: return W_SHORE;
            case RIVER_CITY: return W_RIVER;
            default: return W_NONE;
        }
    }

    /**
     * A short code that rebuilds this exact city: map preset and size, then the seed, e.g. "12-483920".
     * Share it and anyone can play the same streets.
     */
    String code() {
        return CODE_MAPS.charAt(v[OPT_PRESET]) + "" + v[OPT_SIZE] + "-" + seed
                + (outskirts == 1 ? "!" : "") + (edits.isEmpty() ? "" : "~" + editString());
    }

    /** Changes made with the Build tool: {tile x, tile y, what}. They're part of the city code. */
    final java.util.ArrayList<int[]> edits = new java.util.ArrayList<int[]>();
    private static final String B64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    /** The edits packed into text: three bytes each, base64 (URL-safe). */
    String editString() {
        StringBuilder sb = new StringBuilder();
        for (int[] e : edits) {
            int bits = (e[0] & 0xFF) << 16 | (e[1] & 0xFF) << 8 | (e[2] & 0xFF);
            for (int k = 3; k >= 0; k--) sb.append(B64.charAt((bits >> (k * 6)) & 63));
        }
        return sb.toString();
    }

    void setEdits(String text) {
        edits.clear();
        if (text == null) return;
        for (int i = 0; i + 4 <= text.length(); i += 4) {
            int bits = 0;
            for (int k = 0; k < 4; k++) {
                int d = B64.indexOf(text.charAt(i + k));
                if (d < 0) return;
                bits = bits << 6 | d;
            }
            edits.add(new int[]{bits >> 16 & 0xFF, bits >> 8 & 0xFF, bits & 0xFF});
        }
    }

    /** The character for each map in a city code. */
    private static final String CODE_MAPS = "123456789ABCDEFGHIJKLMNOPQRSTU";

    /** (12.0) The same city with the same options, to play again from the start. */
    CityConfig copy() {
        CityConfig c = new CityConfig();
        System.arraycopy(v, 0, c.v, 0, Math.min(v.length, c.v.length));
        c.seed = seed;
        c.outskirts = outskirts;
        c.exodus = exodus;
        c.force = force;
        c.difficulty = difficulty;
        c.civic = civic;
        for (int[] e : edits) c.edits.add(e.clone());
        c.normalize();
        return c;
    }

    /** Reads a city code. Returns false (changing nothing) if it isn't one. */
    boolean applyCode(String text) {
        if (text == null) return false;
        // ((12.0) As pasted from a message: any kind of space, quotes round it, the "CITY" the New Game screen
        // shows before it, and a dash that a chat app made of the hyphen all came out as "That isn't a city code".)
        String t = text.replaceAll("[\\s\u00A0\u2007\u202F\"'\u201C\u201D\u2018\u2019]", "")
                .replaceAll("[\u2010\u2011\u2012\u2013\u2014\u2212]", "-");
        if (t.regionMatches(true, 0, "CITY", 0, 4)) t = t.substring(4);
        if (t.startsWith(":")) t = t.substring(1);
        String ed = null;
        int tilde = t.indexOf('~');
        if (tilde > 0) {
            ed = t.substring(tilde + 1);
            t = t.substring(0, tilde);
        }
        // Little nature out of town: a "!" (since 10.20).
        int out = 0;
        if (t.endsWith("!")) {
            out = 1;
            t = t.substring(0, t.length() - 1);
        }
        // (Codes for a city in another country, after an "@", no longer load: since 11.3 there's only the USA.)
        int at = t.indexOf('@');
        if (at > 0) {
            if (!t.substring(at + 1).equals("0")) return false;
            t = t.substring(0, at);
        }
        int dash = t.indexOf('-');
        if (dash != 2 || t.length() < 4 || t.length() > 22) return false;
        // (The size digit: 0 Tiny, 1 Small, 2 Medium, 3 Large, 4 Massive, 5 Huge. Codes from before Tiny was
        // added used 1 to 4 for Small to Massive, so they still mean the same size.)
        int preset = CODE_MAPS.indexOf(Character.toUpperCase(t.charAt(0))), size = t.charAt(1) - '0';
        if (preset < 0 || preset >= PRESETS.length || removed(preset) || size < 0 || size >= SIZES.length) return false;
        long s;
        try {
            s = Long.parseLong(t.substring(3));
        } catch (NumberFormatException e) {
            return false;
        }
        if (s < 0) return false;
        v[OPT_PRESET] = preset;
        v[OPT_SIZE] = size;
        seed = s;
        v[OPT_COUNTRY] = Country.USA;
        outskirts = out;
        normalize();
        setEdits(ed);
        keepCity = true;
        return true;
    }

    /** Picks a new random city (keeping the options). */
    void newSeed(Random r) {
        edits.clear();
        seed = r.nextInt(SEEDS);
        // (A real city stays the same.)
        normalize();
    }

    int tiles() {
        // ((22.3) Map version 34: every city a fifth smaller across (nine tenths, twice), the same number of people in it -
        // an outbreak quicker to work out, a new city quicker to make and a third less memory for the map.)
        boolean small = civic >= 34;
        int t;
        if (v[OPT_PRESET] == ISLANDS) t = nature() ? (small ? 1040 : ISLANDS_TILES) : 512;
        else if (v[OPT_PRESET] == ARCHIPELAGO) t = small ? 1040 : ARCHIPELAGO_TILES;
        // ((28.0) The water maps are never smaller than the Small size: their towns, islands and bridges need the room.)
        else if ((bridgeCity() || swamp() || lighthouse()) && v[OPT_SIZE] < SMALL) t = SIZES_V34[SMALL];
        else t = (small ? SIZES_V34 : nature() ? SIZES : OLD_SIZES)[v[OPT_SIZE]];
        // ((29.7) Map version 44: a fifth smaller across again (in multiples of eight tiles), the island maps as they were.)
        // ((30.3) Map version 46: every map a fifth smaller again, the island maps too.)
        float s = shrinkScale();
        return s < 1 ? Math.max(64, Math.round(t * s / 8f) * 8) : t;
    }

    /** ((30.3) How much smaller across than its size used to be (1 none): a fifth on 44 (45 for the two islands), a fifth again on 46. */
    float shrinkScale() {
        float s = shrunk() ? SHRINK : 1;
        if (civic >= 46) s *= SHRINK;
        // ((30.4) Map version 47: a fifth smaller again.)
        if (civic >= 47) s *= SHRINK;
        return s;
    }

    /** ((29.7) How much smaller across the maps are from map version 44. */
    static final float SHRINK = 0.8f;

    /**
     * ((29.7) Map version 44, and not one of the island maps (Islands, Archipelago, Prison Island, Theme Park Island).
     * ((29.8) Map version 45: Prison Island and Theme Park Island too; only Islands and the Archipelago keep their size.)
     */
    boolean shrunk() {
        int p = v[OPT_PRESET];
        if (civic < 44 || p == ISLANDS || p == ARCHIPELAGO) return false;
        return civic >= 45 || (p != PRISON_ISLAND && p != THEME_PARK);
    }

    /**
     * ((29.7) Residents counted as if the city were its size before 44 (its homes cover less ground now): the same number
     * of people, police, soldiers and medics in a smaller city.
     */
    int fullResidents(int residents) {
        float s = shrinkScale();
        return s < 1 ? Math.round(residents / (s * s)) : residents;
    }

    /** ((22.3) The sizes from map version 34: 81% across of those before (in multiples of eight tiles). */
    private static final int[] SIZES_V34 = {128, 192, 256, 320, 456, 568, 832, 1040, 1464};

    /** A massive (or huge) map: the city sits in the middle of open countryside. */
    boolean massive() { return v[OPT_SIZE] >= MASSIVE; }

    /** (16.1) A size's name as offered. */
    static String sizeName(int size) {
        return size >= 0 && size < VALUES[OPT_SIZE].length ? VALUES[OPT_SIZE][size] : "";
    }
    /** (10.30) A big army base out in the country: every Massive and Huge map with country round the town. */
    boolean outskirtsBase() { return civic >= 14 && civic < 26 && (massive() || (civic >= 23 && v[OPT_SIZE] >= LARGE)) && water() == W_NONE && coreFraction() < 0.99f; }

    int size() { return v[OPT_SIZE]; }

    /**
     * How much of the map (across) is town; the rest is countryside with dirt roads, farms and woods. A village
     * is a small cluster in the fields, a small town has fields round the edge, and massive maps are mostly
     * country around a city.
     */
    float coreFraction() {
        int p = v[OPT_PRESET];
        // (Towns on the water fill the whole map.)
        if (water() != W_NONE) return 1f;
        // ((28.0) Canyon Town and Frozen North lay out the whole map themselves; Highway Crossroads is mostly country.)
        if (p == CANYON || p == FROZEN_NORTH) return 1f;
        if (p == CROSSROADS) return 0.38f;
        // ((28.0) Border Town: the town fills the map, the border right across it.)
        if (p == BORDER_TOWN) return 1f;
        float f = p == 9 ? 0.55f : p == 6 ? 0.72f : 1f;
        // A massive map is nearly all town: a thin band of country round the edge for the highway and farms.
        if (massive()) f = p == 9 ? 0.6f : p == 6 ? 0.88f : 0.92f;
        if (!nature()) return f;
        // Since 10.10 the maps are a quarter bigger, and the extra room is countryside: hills and woods,
        // mountains, lakes and streams, a nature park and campsites round a town a little bigger than before.
        // ((11.0) Less nature: a thin band of country round a town that fills nine tenths of the map, whatever the map.)
        // (Just wide enough for the highway and its interchanges: 28 tiles each side, or a little more on the big maps.
        // The Garrison State keeps its open country for its bases and the prison.)
        // ((22.9) Desert City: open desert round a town that sprawls over seven tenths of the map.)
        if (p == DESERT) return moreTown() ? 0.85f : 0.7f;
        if (civic >= 26) {
            int t = tiles();
            float band = Math.max(28, t * 0.04f) * 2f / t;
            return Math.max(p == 9 ? 0.72f : 0.78f, 1 - band);
        }
        if (p == 9) return v[OPT_SIZE] <= SMALL ? 0.6f : 0.5f;
        if (p == 9) return v[OPT_SIZE] <= SMALL ? 0.6f : 0.5f;
        float town = new float[]{1f, 0.86f, 0.8f, 0.76f, 0.74f, 0.66f, 0.66f, 0.66f, 0.66f}[v[OPT_SIZE]];
        // (Since 10.30 the Massive and Huge maps have more country round the town, for its towns and the base.)
        if (civic >= 14 && massive()) town = v[OPT_SIZE] >= GIGANTIC ? 0.7f : v[OPT_SIZE] >= HUGE ? 0.6f : 0.66f;
        // (10.33) Less empty country: the town reaches further out (the country beyond fills with towns too).
        if (civic >= 17) town = Math.min(1f, town + (v[OPT_SIZE] >= GIGANTIC ? 0.04f : massive() ? 0.08f : 0.02f));
        // ((10.56) The Garrison State keeps more open country round the town, for its bases and the prison.)
        return p == 6 ? town * 0.8f : town;
    }
    /** How many of a city's residents (everyone its homes can house) are in the game. */
    int civilians(int residents) {
        return Math.round(residents * RESIDENT_SHARE[v[OPT_CIVILIANS]] * POP_MORE);
    }

    /** ((26.0) A tenth more people in every city. */
    static final float POP_MORE = 1.1f * 1.05f;
    /** ((29.2) Five percent more police and five percent more soldiers in every city. */
    static final float FORCE_MORE = 1.05f;

    /** ((22.4) Map version 36: how many live in a sandbox city of each size, at "Most", on a city of middling density. */
    private static final int[] POP_TILES = {128, 192, 256, 320, 456, 568, 832, 1040, 1464};
    private static final int[] POP_TARGET = {2500, 5000, 8000, 12000, 16000, 20000, 25000, 28000, 30000};

    boolean popLadder() { return civic >= 36; }

    /** ((22.6) Map version 37: more colours for homes, shops, flats, cars and people, more names for streets and places. */
    boolean variety() { return civic >= 37; }

    /** ((22.8) Map version 38: themed neighbourhoods, more colours, another hundred landmarks. */
    boolean neighbourhoods() { return civic >= 38; }

    /**
     * The people at the start: from the size of the map on version 36 (a smooth step up from size to size, thinner
     * in the country presets, the People option a share of it), from the homes there are before that and in missions.
     */
    int civilians(int residents, int tiles) {
        if (!popLadder()) return civilians(residents);
        // ((29.7) As many as the size had before it shrank.)
        float sc = shrinkScale();
        if (sc < 1) tiles = Math.round(tiles / sc);
        int target = POP_TARGET[POP_TARGET.length - 1];
        if (tiles <= POP_TILES[0]) target = POP_TARGET[0];
        else
            for (int k = 1; k < POP_TILES.length; k++)
                if (tiles <= POP_TILES[k]) {
                    float f = (tiles - POP_TILES[k - 1]) / (float) (POP_TILES[k] - POP_TILES[k - 1]);
                    target = Math.round(POP_TARGET[k - 1] + f * (POP_TARGET[k] - POP_TARGET[k - 1]));
                    break;
                }
        float dens = density() == 0 ? 0.5f : 1f;
        if (civic >= 46) {
            // ((30.3) Map version 46: 5,000 people on the Default size, 7,500 on Big, 10,000 on Large (the island maps as Big),
            // the country towns no fewer than the Default, whatever the map; smaller (old) sizes fewer, in step.)
            float k = tiles >= POP_TILES[6] ? Math.min(1f, (tiles - POP_TILES[6]) / (float) (POP_TILES[8] - POP_TILES[6])) : -1;
            float people = k >= 0 ? (tiles <= POP_TILES[7] ? 5000 + 2500 * (tiles - POP_TILES[6]) / (float) (POP_TILES[7] - POP_TILES[6])
                    : 7500 + 2500 * (tiles - POP_TILES[7]) / (float) (POP_TILES[8] - POP_TILES[7]))
                    : 5000f * target / POP_TARGET[6];
            if (k < 0) people *= dens;
            // ((30.4) Map version 47: fewer in step with the smaller ground (a fifth smaller across, so 64% of the people).)
            float area = civic >= 47 ? SHRINK * SHRINK : 1;
            return Math.round(Math.min(10000, people) * area * RESIDENT_SHARE[v[OPT_CIVILIANS]] / RESIDENT_SHARE[PEOPLE_ABOUT]);
        }
        return Math.round(target * dens * RESIDENT_SHARE[v[OPT_CIVILIANS]] / RESIDENT_SHARE[PEOPLE_ABOUT] * POP_MORE);
    }
    /**
     * Police on duty when the game starts: about one officer for every sixteen residents (far more than a real city
     * has on shift), eight more per station, at least twelve.
     */
    int cops(int residents) {
        residents = fullResidents(residents);
        // About one officer for every 16 residents the game simulates, plus eight for each station ((12.2) more than
        // ever: one in 24 and six a station up to 12.0, one in 48 and three in 12.1).
        // ((13.0) Up to 800 now, for the bigger cities: 480 before.)
        return Math.round(Math.max(12, Math.min(800, Math.round(residents / 16f) + policeStations() * 8)) * FORCE_SCALE[force] * FORCE_MORE * forceCut());
    }

    /** ((30.4) Map version 47: police, soldiers and the help from outside 15% fewer. ((32.1) Map version 54: a quarter more again.) */
    float forceCut() {
        float f = civic >= 54 ? 0.85f * 1.25f : civic >= 47 ? 0.85f : 1f;
        // ((32.3) Map version 56: no civilians - half as many police and soldiers again.)
        return civic >= 56 && noCivilians() ? f * 1.5f : f;
    }

    /** ((26.0) Police/army strength (a New Game choice, not one of the eight in v): Weak, Normal, Strong. */
    static final int OPT_FORCE = 10;
    private static final String[] FORCES = {"Weak", "Normal", "Strong"};
    /** ((29.0) What every new game has: Strong, and reinforcements On. */
    static final int FORCE_STRONG = 2, RESERVES_ON = 3;
    static final float[] FORCE_SCALE = {0.55f, 1f, 1.6f};
    int force = 1;

    /** Soldiers at the base (if the map has one): a garrison that grows with the city. */
    int soldiers(int residents) {
        residents = fullResidents(residents);
        if (!militaryBase() && !outskirtsBase()) return 0;
        // (10.33: bigger garrisons.)
        // ((12.2) Bigger again: one in 40 and twelve more, up to 160 (one in 50 up to 120 to 12.0, one in 70 in 12.1).)
        // ((13.0) Up to 300 with the more bases: 160 before.)
        int n = Math.max(20, Math.min(300, Math.round(residents / 40f) + 12));
        // ((15.5) The Military Town's great base: three times the garrison, and never fewer than 450.)
        // ((28.0) The Military Airbase: two and a half times the garrison, never fewer than 380.)
        int big = militaryTown() ? Math.min(700, Math.max(450, n * 3)) : airbase() ? Math.min(600, Math.max(380, n * 5 / 2)) : n;
        return Math.round(big * FORCE_SCALE[force] * FORCE_MORE * forceCut());
    }


    /**
     * (10.63) How many should live in the city (cities built since map version 25): enough that with most of them
     * about (the usual setting) there are some 20,000 people on the Default size, rising to 30,000 on the Colossal
     * ((13.0) from 15,000 to 20,000 before).
     */
    int residentsTarget() {
        int s = v[OPT_SIZE];
        float people = s <= LARGE ? 20000 : s == MASSIVE ? 22500 : s == HUGE ? 25000 : s == GIGANTIC ? 27500 : 30000;
        return Math.round(people / RESIDENT_SHARE[PEOPLE_ABOUT]);
    }

    /** What each Reinforcements setting means, in plain words. */
    static final String[] RESERVE_INFO = {
            "Off: nobody else comes. Only the police, soldiers and helicopters already in the city fight.",
            "On: help comes from outside the city.",
            "On: help comes from outside the city.",
            "On: police come first, half a minute or so after being asked: three quarters as many officers again as the city has, in waves from the towns around "
                    + "(their police, highway patrol, SWAT teams, sheriff's deputies and federal agents), sooner and bigger "
                    + "the worse the city is losing. Then three quarters as many soldiers again as the city has, in three waves: a quick reaction force in Humvees, "
                    + "the main convoy with armour, and air assaults by helicopter wherever it's worst."};
    int zombies() {
        return ZOMBIES[v[OPT_ZOMBIES]];
    }
    /** Which country the city is in (see {@link Country}). */
    int country() { return v[OPT_COUNTRY]; }
    /** 0 off, 1 low, 2 medium, 3 high: how many reserve squads and backup waves can be called in. */
    int reinforcements() { return v[OPT_RESERVES]; }

    private int map(int i) { return MAPS[v[OPT_PRESET]][i]; }
    int style() { return map(0); }
    int density() { return map(1); }
    int height() { return map(2); }
    int parks() { return map(3); }
    int traffic() { return map(4); }
    int layout() { return map(5); }
    /** Police stations: the map's own number, and more on Large and Massive maps. */
    int policeStations() {
        int n = map(6);
        int s = v[OPT_SIZE];
        int was = n == 0 ? 0 : Math.max(1, n - (s == TINY ? 1 : 0)) + (s >= LARGE ? 1 : 0) + (s >= MASSIVE ? 1 : 0) + (s >= HUGE ? 1 : 0) + (s >= GIGANTIC ? 2 : 0)
                // ((10.52) One more on every map from the Default size up.)
                + (civic >= 21 && s >= LARGE ? 1 : 0);
        // ((13.0) Twice as many, and two more again on the Colossal map.)
        int n2 = was * 2 + (s >= COLOSSAL ? 2 : 0);
        // ((30.4) Map version 47: in step with the smaller maps - about half as many on maps half as wide as they were.)
        return civic >= 47 && n2 > 0 ? Math.max(1, Math.round(n2 * shrinkScale())) : n2;
    }

    /** Medics: the hospital's, and the paramedics at the fire stations, growing with the city. */
    int medics(int residents) {
        residents = fullResidents(residents);
        return Math.max(4, Math.min(36, Math.round(residents / 110f)));
    }
    /** ((26.1) Every map has an army base since map version 39.) */
    boolean militaryBase() { return map(7) == 1 || civic >= 39; }

    /**
     * ((26.1) Map version 39: Islands and Archipelago told apart - each of the Islands its own kind of town (the city,
     * the suburbs, the port, the old town), the Archipelago a tropical chain (palms and beaches, its outer islands villages).
     */
    boolean distinctIslands() { return civic >= 39; }

    /** (13.0) Army bases in town besides the main one: one on the Default and Large maps, up to four on the Colossal. */
    int extraBases() {
        if (!militaryBase() || bigGarrison()) return 0;
        int s = v[OPT_SIZE];
        int n = s < LARGE ? 0 : s <= MASSIVE ? 1 : s == HUGE ? 2 : s == GIGANTIC ? 3 : 4;
        // ((30.4) Map version 47: fewer, for the smaller maps.)
        return civic >= 47 ? Math.round(n * shrinkScale()) : n;
    }

    /** How many of a landmark (0 church, 1 school, 2 fire station, 3 supermarket, 4 gas station, 5 cemetery). */
    int landmarks(int kind) {
        int n = LANDMARKS[v[OPT_PRESET]][kind];
        // Fire stations: one more on a Large map, two more on a Massive one.
        // (Every town has at least one.)
        int s = v[OPT_SIZE];
        if (kind == 2 && !fireStations()) return 0;
        if (kind == 2) {
            n = Math.max(1, n);
            if (s >= LARGE) return n + s - MEDIUM;
        }
        if (s == TINY) return Math.min(n, kind == 2 || kind == 3 ? 1 : 0);
        if (s == SMALL) return n > 1 ? 1 : n;
        if (s == LARGE) return n + (n + 1) / 2;
        if (s == HUGE) return n * 2;
        if (s >= GIGANTIC) return n * 3;
        return n;
    }

    int shopShare() { return LANDMARKS[v[OPT_PRESET]][6]; }

    // Which maps have a railway line (with a station and passing trains).
    // Which maps have a railway line: 0 never, 1 about half the time (it depends on the city), 2 always.
    private static final int[] RAIL = {1, 0, 0, 2, 0, 0, 1, 0, 2, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    boolean hasRail() {
        int r = RAIL[v[OPT_PRESET]];
        return r == 2 || (r == 1 && Math.abs(seed % 2) == 0);
    }

    // Districts:  downtown, midtown, old town, suburb, industrial, campus, parkside (relative weights), then the
    // kind of district at the heart of town.
    private static final int[][] DISTRICT_MIX = {
            {3, 3, 2, 4, 2, 1, 2, 0},   // Classic
            {6, 5, 2, 0, 1, 0, 1, 0},   // Downtown
            {0, 1, 1, 8, 0, 1, 2, 1},   // Suburbs
            {1, 1, 0, 2, 7, 0, 0, 4},   // Industrial
            {1, 2, 1, 3, 0, 1, 6, 6},   // Parkland
            {0, 2, 7, 2, 0, 0, 1, 2},   // Old Town
            {0, 1, 3, 6, 0, 0, 2, 2},   // Small Town
            {0, 2, 1, 3, 0, 6, 2, 5},   // Campus
            {6, 5, 1, 1, 3, 0, 1, 0},   // Metropolis
            {0, 0, 3, 6, 0, 0, 3, 2},   // Village
            {9, 4, 1, 0, 1, 0, 0, 0},   // Downtown Towers
            {1, 3, 1, 6, 2, 0, 2, 3},   // Military Town
            {1, 2, 1, 6, 0, 1, 4, 6},   // Lakeside
            {2, 3, 1, 2, 5, 0, 0, 1},   // Harbour
            {3, 4, 2, 4, 1, 1, 2, 0},   // Islands
            {3, 3, 2, 3, 1, 1, 1, 0}, {3, 3, 2, 3, 1, 1, 1, 0},
            {1, 3, 1, 8, 2, 0, 1, 0},   // Desert City
            {4, 3, 0, 3, 0, 0, 2, 6},   // Theme Park Island
            {0, 3, 0, 6, 1, 1, 2, 1},   // Space Center
            {0, 2, 2, 7, 2, 0, 2, 1},   // Highway Crossroads
            {2, 4, 3, 4, 1, 0, 2, 1},
            {1, 3, 0, 6, 1, 0, 2, 1},   // ((28.0) Military Airbase)
            {3, 4, 2, 4, 1, 1, 2, 0},   // Archipelago
            {1, 3, 3, 5, 1, 0, 1, 1},   // Canyon Town
            {4, 4, 2, 4, 2, 1, 1, 0},   // ((28.0) Bridge City)
            {0, 2, 3, 6, 1, 0, 2, 2},   // ((28.0) Swamp Bayou)
            {0, 2, 4, 5, 1, 0, 2, 2},   // ((28.0) Lighthouse Coast)
            {4, 4, 1, 3, 1, 1, 3, 0},   // ((28.0) Stadium City)
            {0, 3, 2, 6, 3, 0, 1, 1},   // Frozen North
    };

    /** How likely each kind of district is on this map. */
    int districtWeight(int type) { return DISTRICT_MIX[v[OPT_PRESET]][type]; }

    /** The kind of district in the middle of town. */
    int coreDistrict() { return DISTRICT_MIX[v[OPT_PRESET]][7]; }

    /** Percent of office lots that become apartments, garages and pharmacies. */
    int[] buildingMix() { return BUILDING_MIX[v[OPT_PRESET]]; }

    /** Relative weights of the park kinds for this map. */
    int[] parkMix() { return PARK_MIX[v[OPT_PRESET]]; }

    /**
     * (10.50) A fresh New Game screen leaves behind the choices no longer offered (a map or country taken off, the
     * people about and outskirts settings that aren't choices now).
     */
    void dropRetired() {
        if (retired(OPT_PRESET, v[OPT_PRESET])) v[OPT_PRESET] = METROPOLIS;
        if (retired(OPT_COUNTRY, v[OPT_COUNTRY])) v[OPT_COUNTRY] = Country.USA;
        // ((16.1) A size no longer offered: the Default.)
        if (retired(OPT_SIZE, v[OPT_SIZE])) v[OPT_SIZE] = GIGANTIC;
        // ((32.3) Civilians on or off is a choice again: off is kept, anything else is the usual share.)
        if (v[OPT_CIVILIANS] != 0) v[OPT_CIVILIANS] = PEOPLE_ABOUT;
        outskirts = 0;
        // ((29.0) Every new game: the police and army at their strongest, and reinforcements on. Neither is a choice now.)
        force = FORCE_STRONG;
        v[OPT_RESERVES] = RESERVES_ON;
        normalize();
    }

    /** (10.50) The share of residents out and about when a game starts: no longer a setting. */
    static final int PEOPLE_ABOUT = 3;

    void randomize(Random r) {
        boolean none = noCivilians();
        for (int i = 0; i < v.length; i++) {
            // (A random city is a made-up one: not one of the real cities.)
            do v[i] = r.nextInt(VALUES[i].length);
            while (retired(i, v[i]));
        }
        v[OPT_SIZE] = GIGANTIC;
        force = FORCE_STRONG;
        normalize();
        v[OPT_CIVILIANS] = none ? 0 : PEOPLE_ABOUT;
        outskirts = 0;
        keepCity = false;
    }

    // ------------------------------------------------------------------ OptionSet

    @Override public int count() { return LABELS.length; }
    @Override public String label(int i) { return LABELS[i]; }
    @Override public String[] values(int i) {
        return i == OPT_OUTSKIRTS ? OUTSKIRTS : i == OPT_EXODUS ? EXODUS : i == OPT_FORCE ? FORCES : i == OPT_NO_CIV ? CIVS : VALUES[i];
    }
    @Override public int get(int i) {
        return i == OPT_OUTSKIRTS ? outskirts : i == OPT_EXODUS ? exodus : i == OPT_FORCE ? force : i == OPT_NO_CIV ? (noCivilians() ? 1 : 0) : v[i];
    }
    @Override public void set(int i, int value) {
        if (i == OPT_OUTSKIRTS) {
            outskirts = value;
            return;
        }
        if (i == OPT_EXODUS) {
            exodus = value;
            return;
        }
        if (i == OPT_FORCE) {
            force = Math.max(0, Math.min(2, value));
            return;
        }
        if (i == OPT_NO_CIV) {
            v[OPT_CIVILIANS] = value == 1 ? 0 : PEOPLE_ABOUT;
            return;
        }
        v[i] = value;
        normalize();
    }
}
