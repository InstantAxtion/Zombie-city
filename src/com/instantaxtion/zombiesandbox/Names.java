package com.instantaxtion.zombiesandbox;

/** Names for the people (and dogs) of the city, picked from each one's name seed. */
final class Names {
    private static Country here() {
        Country c = Country.current;
        return c != null ? c : Country.get(Country.USA);
    }

    private Names() {
    }

    static String person(int seed) {
        int s = seed & 0x7FFFFFFF;
        Country c = here();
        return c.first[s % c.first.length] + " " + c.last[(s / c.first.length) % c.last.length];
    }

    // ((25.0) Fuller rank ladders, and promotion up them: a step for every two levels of experience (World.gainXp).)
    static final String[] COP_RANKS = {"Officer", "Officer II", "Senior Officer", "Corporal", "Sergeant", "Staff Sergeant",
            "Detective", "Senior Detective", "Lieutenant", "Captain", "Commander"};
    static final String[] TROOPER_RANKS = {"Trooper", "Senior Trooper", "Master Trooper", "Corporal", "Sergeant", "Lieutenant", "Captain"};
    static final String[] DEPUTY_RANKS = {"Deputy", "Senior Deputy", "Corporal", "Sergeant", "Lieutenant", "Captain", "Undersheriff"};
    static final String[] RANGER_RANKS = {"Ranger", "Senior Ranger", "Lead Ranger", "District Ranger", "Chief Ranger"};
    static final String[] AGENT_RANKS = {"Agent", "Special Agent", "Senior Special Agent", "Supervisory Special Agent",
            "Assistant Special Agent in Charge", "Special Agent in Charge"};
    static final String[] SOLDIER_RANKS = {"Pvt.", "PV2", "PFC", "Spc.", "Cpl.", "Sgt.", "SSgt.", "SFC", "MSgt.", "1st Sgt.", "Sgt. Maj."};
    static final String[] OFFICER_RANKS = {"2nd Lt.", "1st Lt.", "Capt.", "Maj.", "Lt. Col.", "Col."};

    /** Where on its ladder someone starts (most near the bottom), from their seed. */
    private static int start(int seed, int mul, int[] cuts) {
        int r = ((seed * mul) >>> 8) % 100;
        int i = 0;
        while (i < cuts.length && r >= cuts[i]) i++;
        return i;
    }

    /** The rank ladder a police officer is on (by agency), and their place on it now. */
    static String[] copLadder(Entity e) {
        return e.agency == 1 ? TROOPER_RANKS : e.agency == 2 ? DEPUTY_RANKS : e.agency == 3 ? RANGER_RANKS : e.agency == 4 ? AGENT_RANKS : COP_RANKS;
    }

    static int copRankIndex(Entity e) {
        String[] l = copLadder(e);
        int base = l == COP_RANKS ? start(e.nameSeed, 0x9E3779B1, new int[]{40, 58, 70, 80, 88, 93, 96, 98})
                : start(e.nameSeed, 0x9E3779B1, new int[]{55, 78, 90, 97});
        return Math.min(l.length - 1, base + e.level / 2);
    }

    static String copRank(Entity e) {
        return copLadder(e)[copRankIndex(e)];
    }

    /** A soldier's rank: enlisted, or an officer's for commanders and pilots. */
    static boolean officer(Entity e) {
        return e.role == Entity.ROLE_COMMANDER || e.role == Entity.ROLE_PILOT;
    }

    static int soldierRankIndex(Entity e) {
        if (officer(e)) return Math.min(OFFICER_RANKS.length - 1, (e.role == Entity.ROLE_COMMANDER ? 2 : 1) + e.level / 2);
        int base = start(e.nameSeed, 0x85EBCA6B, new int[]{18, 32, 50, 66, 78, 88, 94, 97, 99});
        return Math.min(SOLDIER_RANKS.length - 1, base + e.level / 2);
    }

    static String soldierRank(Entity e) {
        return officer(e) ? OFFICER_RANKS[soldierRankIndex(e)] : SOLDIER_RANKS[soldierRankIndex(e)];
    }

    /** Anyone's rank (police or army), or null. */
    static String rank(Entity e) {
        return e.type == Entity.COP ? copRank(e) : e.type == Entity.SOLDIER ? soldierRank(e) : null;
    }

    // ((26.0) A survivor hero is known by a name the others gave them.)
    static final String[] NICKS = {"Hawkeye", "Doc", "Lucky", "Iron", "Ghost", "Bulldog", "Sparrow", "Tank", "Ace", "Red",
            "Steady", "Bones", "Wolf", "Preacher", "Hammer", "Sarge", "Blaze", "Kid", "Slick", "Tex"};

    /** "First 'Nick' Last". */
    static String hero(int seed) {
        String n = person(seed);
        int sp = n.indexOf(' ');
        String nick = NICKS[((seed * 0x2545F491) >>> 8) % NICKS.length];
        return sp < 0 ? n + " '" + nick + "'" : n.substring(0, sp) + " '" + nick + "'" + n.substring(sp);
    }

    static String dog(int seed) {
        Country c = here();
        return c.dogs[(seed & 0x7FFFFFFF) % c.dogs.length];
    }
}
