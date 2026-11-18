package com.instantaxtion.zombiesandbox;

/**
 * ((21.0) A commander's perks, in force in every mission (never in the sandbox). ((22.0) Now a tree: three branches -
 * Firepower, Defence and Logistics - each perk needing the one before it in its branch. A point to spend for every
 * four stars won, chosen on the Campaign screen.)
 */
final class Perks {
    private Perks() {
    }

    static final int ARMOUR = 0, AIM = 1, LOGISTICS = 2, QUARTERMASTER = 3, FUNDING = 4, SAPPERS = 5, FIRE_SUPPORT = 6, OLD_HANDS = 7;
    /** ((22.0) The new ones. */
    static final int HEAVY = 8, FORTIFIED = 9, ENGINEERS = 10, RAPID = 11;
    static final String[] NAMES = {"Body armour", "Marksmanship", "Logistics", "Quartermaster", "Funding", "Sappers", "Fire support", "Old hands",
            "Heavy weapons", "Fortified", "Field engineers", "Rapid response"};
    static final String[] INFO = {
            "Police and soldiers can take 15% more.",
            "Police and soldiers shoot 8% straighter.",
            "Every mission starts with a fifth more money.",
            "Flares, mines, razor wire, crates and medkits at half price.",
            "A fifth more funding each minute.",
            "Barricades and sandbags are half as tough again, and razor wire lasts half as long again.",
            "Airstrikes and mortar barrages cost a third less.",
            "Sixteen veterans carry over between missions, not ten.",
            "Gunners and grenadiers carry half as much ammunition again.",
            "Safe zones open with their walls already reinforced.",
            "Combat engineers dig in twice as often, and put down more.",
            "Police and soldiers cost 15% less."};
    /** ((22.0) The three branches, each in order (a perk needs the one before it). */
    static final String[] BRANCH_NAMES = {"Firepower", "Defence", "Logistics"};
    static final int[][] BRANCHES = {{AIM, FIRE_SUPPORT, HEAVY}, {ARMOUR, SAPPERS, FORTIFIED, ENGINEERS},
            {LOGISTICS, QUARTERMASTER, FUNDING, RAPID, OLD_HANDS}};
    static final int COUNT = NAMES.length;
    /** Stars for each point to spend. */
    static final int STARS_PER_POINT = 4;
    /** (Before 22.0 the first eight came by themselves at these stars: kept for those who had them.) */
    private static final int[] OLD_STARS = {3, 6, 9, 12, 15, 18, 21, 24};

    /** Whether the commander of this mission has a perk (w.perkStars holds the perks taken, one bit each, since 22.0). */
    static boolean has(World w, int perk) {
        return (w.perkStars & (1 << perk)) != 0 && w.city.cfg.mission >= 0;
    }

    static boolean taken(int mask, int perk) {
        return (mask & (1 << perk)) != 0;
    }

    static int count(int mask) {
        return Integer.bitCount(mask & ((1 << COUNT) - 1));
    }

    /** Points earned by so many stars. */
    static int points(int stars) {
        return stars / STARS_PER_POINT;
    }

    /** Points left to spend. */
    static int spare(int mask, int stars) {
        return Math.max(0, points(stars) - count(mask));
    }

    /** The perk before this one in its branch, or -1 for the first of a branch. */
    static int before(int perk) {
        for (int[] b : BRANCHES)
            for (int i = 0; i < b.length; i++) if (b[i] == perk) return i == 0 ? -1 : b[i - 1];
        return -1;
    }

    /** Whether this perk can be taken now. */
    static boolean open(int mask, int stars, int perk) {
        if (taken(mask, perk) || spare(mask, stars) <= 0) return false;
        int b = before(perk);
        return b < 0 || taken(mask, b);
    }

    /** The perks a commander had from their stars before 22.0, kept. */
    static int legacy(int stars) {
        int m = 0;
        for (int k = 0; k < OLD_STARS.length; k++) if (stars >= OLD_STARS[k]) m |= 1 << k;
        return m;
    }
}
