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

    /** ((22.6) A police officer's rank, the same every time for each: most are officers, some sergeants and up. */
    static String copRank(int seed) {
        int r = ((seed * 0x9E3779B1) >>> 8) % 100;
        return r < 58 ? "Officer" : r < 72 ? "Senior Officer" : r < 84 ? "Corporal" : r < 93 ? "Sergeant" : r < 98 ? "Detective" : "Lieutenant";
    }

    /** ((22.6) A soldier's rank: privates and specialists mostly, a few NCOs. */
    static String soldierRank(int seed) {
        int r = ((seed * 0x85EBCA6B) >>> 8) % 100;
        return r < 30 ? "Pvt." : r < 52 ? "PFC" : r < 70 ? "Spc." : r < 84 ? "Cpl." : r < 94 ? "Sgt." : r < 98 ? "SSgt." : "Lt.";
    }

    static String dog(int seed) {
        Country c = here();
        return c.dogs[(seed & 0x7FFFFFFF) % c.dogs.length];
    }
}
