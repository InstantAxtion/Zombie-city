package com.instantaxtion.zombiesandbox;

/**
 * ((32.0) Map version 53: balance for the mass outbreak.) With a few dozen of the dead about, the city's police and army
 * fight as they always have. With thousands, they shredded a horde of three thousand in a minute and a half: grenades
 * into the packed dead, headshot after headshot from hundreds of guns, and every body shot holding one up. So the bigger
 * the outbreak - from 300 of the dead up to 1800, then no further - the more of it there is to get through: the dead
 * take more stopping (fewer clean kills to the head, three times the body shots, half the blast), shrug off the police's
 * stopping power, bite harder, and a shooter with the dead pressing round them shoots worse. (From how many of
 * the dead there are, as it stands.)
 */
final class Balance {
    private Balance() {
    }

    static float headshot(World w) {
        return 1 - 0.6f * massFor(w);
    }

    /**
     * ((32.3) Map version 56: the police are kitted out to hold a line - bite-proof sleeves, gloves and vests: a bite does
     * a quarter of the harm (policeGear), and one in twenty that would have infected does (policeInfect). 1 before.
     */
    static float policeGear(World w) {
        return w.city.cfg.civic >= 56 ? 0.25f : 1f;
    }

    static float policeInfect(World w) {
        return w.city.cfg.civic >= 56 ? 0.05f : 1f;
    }

    /** ((32.3) Map version 56: the army does the killing - its soldiers a third surer of the head shot. */
    static float soldierAim(World w) {
        return w.city.cfg.civic >= 56 ? 1.35f : 1f;
    }

    /** Divides a body shot's damage to the dead. */
    static float toughness(World w) {
        return 1 + 2 * massFor(w);
    }

    static float blast(World w) {
        return 1 - 0.65f * massFor(w);
    }

    static float stagger(World w) {
        return 1 - 0.75f * massFor(w);
    }

    static float bite(World w) {
        return 1 + 0.4f * massFor(w);
    }

    /** How much worse each of the dead past the third within 60 makes a shot. */
    static float panic(World w) {
        return 0.15f * massFor(w);
    }

    /** ((Map version 53) From how many of the dead there are: 300 or fewer, 0; 1800 or more, 1. */
    static float massFor(World w) {
        if (w.city.cfg.civic < 53) return 0;
        return Math.max(0, Math.min(1, (w.zombies - 300) / 1500f));
    }
}
