package com.instantaxtion.zombiesandbox;

/**
 * ((21.0) A commander's perks: earned for good by the stars won across the campaign, and in force in every mission
 * after. Nothing is bought or chosen: three more stars, one more perk. They apply in missions only, never in the
 * sandbox.
 */
final class Perks {
    private Perks() {
    }

    static final int ARMOUR = 0, AIM = 1, LOGISTICS = 2, QUARTERMASTER = 3, FUNDING = 4, SAPPERS = 5, FIRE_SUPPORT = 6, OLD_HANDS = 7;
    /** The stars each takes. */
    static final int[] STARS = {3, 6, 9, 12, 15, 18, 21, 24};
    static final String[] NAMES = {"Body armour", "Marksmanship", "Logistics", "Quartermaster", "Funding", "Sappers", "Fire support", "Old hands"};
    static final String[] INFO = {
            "Police and soldiers can take 15% more.",
            "Police and soldiers shoot 8% straighter.",
            "Every mission starts with a fifth more money.",
            "Flares, mines, razor wire, crates and medkits at half price.",
            "A fifth more funding each minute.",
            "Barricades and sandbags are half as tough again, and razor wire lasts half as long again.",
            "Airstrikes and mortar barrages cost a third less.",
            "Sixteen veterans carry over between missions, not ten."};

    /** Whether the commander of this mission has a perk. */
    static boolean has(World w, int perk) {
        return w.perkStars >= STARS[perk] && w.city.cfg.mission >= 0;
    }

    /** How many perks that many stars have earned. */
    static int count(int stars) {
        int n = 0;
        for (int s : STARS) if (stars >= s) n++;
        return n;
    }

    /** The stars the next perk takes, or -1 with all of them earned. */
    static int next(int stars) {
        for (int s : STARS) if (stars < s) return s;
        return -1;
    }
}
