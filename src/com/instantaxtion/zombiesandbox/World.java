package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/** The simulation: people, zombies, gunfire, explosions and all the visual effects they leave behind. */
final class World {
    // ------------------------------------------------------------------ ((27.3) the systems
    // (What the city does lives in these, each with this World in hand; the state and the frame's loop stay here.)
    final Population population = new Population(this);
    final Spatial spatial = new Spatial(this);
    final ZombieAI zombieAI = new ZombieAI(this);
    final CivilianAI civilianAI = new CivilianAI(this);
    final DailyLife dailyLife = new DailyLife(this);
    final Indoors indoors = new Indoors(this);
    final DogAI dogAI = new DogAI(this);
    final ArmedAI armedAI = new ArmedAI(this);
    final Police police = new Police(this);
    final Posts posts = new Posts(this);
    final Ammo ammo = new Ammo(this);
    final Combat combat = new Combat(this);
    final Death death = new Death(this);
    final Noise noise = new Noise(this);
    final Movement movement = new Movement(this);
    final Rides rides = new Rides(this);
    final Overwatch rooftops = new Overwatch(this);
    final Fieldworks fieldworks = new Fieldworks(this);
    final Experience experience = new Experience(this);
    final Stats stats = new Stats(this);
    final War war = new War(this);
    final Recruitment recruiting = new Recruitment(this);
    final Containment containment = new Containment(this);
    final Rescue rescue = new Rescue(this);
    final Exodus exodus = new Exodus(this);
    final BaseLife baseLife = new BaseLife(this);
    final Holdouts holdoutLife = new Holdouts(this);
    final Raiders raiders = new Raiders(this);
    final Prison prison = new Prison(this);
    final Events events = new Events(this);
    final Sandbox sandbox = new Sandbox(this);
    final PlayerControl playerControl = new PlayerControl(this);
    final Aftermath aftermathLife = new Aftermath(this);
    final Wilds wilds = new Wilds(this);
    final Effects effects = new Effects(this);

    /** ((11.5) The length of (x, y): Math.hypot is careful about overflow we never get near, and many times slower.) */
    static double hypot(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }

    /** ((12.0) A game clock: m:ss, or h:mm:ss once past an hour - it read 75:03.) */
    /** ((12.0) "1 hostile", "3 hostiles": a count and the word for that many.) */
    static String count(int n, String one, String many) {
        return n + " " + (n == 1 ? one : many);
    }

    static String clock(int secs) {
        secs = Math.max(0, secs);
        // ((12.0) Put together by hand: String.format wrote it in the phone's own digits (Arabic, Persian...) among
        // English words, and it's asked for every frame.)
        StringBuilder sb = new StringBuilder(8);
        if (secs >= 3600) sb.append(secs / 3600).append(':').append(secs / 600 % 6).append(secs / 60 % 10);
        else sb.append(secs / 60);
        return sb.append(':').append(secs / 10 % 6).append(secs % 10).toString();
    }

    static final float TAU = (float) (Math.PI * 2);

    static final int[] SHIRTS = {
            0xFFD9534F, 0xFF5BC0DE, 0xFFF0AD4E, 0xFF5CB85C, 0xFFEEEEEE, 0xFF9B59B6, 0xFFE67E22, 0xFF34495E,
            0xFFF1C40F, 0xFF1ABC9C, 0xFFE84393, 0xFF795548, 0xFF222222, 0xFF8BC34A
    };
    static final int[] HAIR = {0xFF2B1B10, 0xFF4A2E1A, 0xFF111111, 0xFFC8A45A, 0xFF8B4A2B, 0xFF777777};
    static final int[] DOGS = {0xFF8B5A2B, 0xFF2B2320, 0xFFD9C49A, 0xFFB07A3E, 0xFF6E6A66, 0xFFEDE6DA};
    static final int[] SKINS = {0xFFF1C27D, 0xFFE0AC69, 0xFFC68642, 0xFF8D5524, 0xFFFFDBAC};
    /** ((22.6) Map version 37: more shirts, hair and skin tones. */
    static final int[] SHIRTS37 = {
            0xFFD9534F, 0xFF5BC0DE, 0xFFF0AD4E, 0xFF5CB85C, 0xFFEEEEEE, 0xFF9B59B6, 0xFFE67E22, 0xFF34495E,
            0xFFF1C40F, 0xFF1ABC9C, 0xFFE84393, 0xFF795548, 0xFF222222, 0xFF8BC34A, 0xFF2E86C1, 0xFFA93226,
            0xFF117864, 0xFFF5B7B1, 0xFFAED6F1, 0xFFD7BDE2, 0xFF6E2C00, 0xFF1B2631, 0xFFB7950B, 0xFF7D3C98,
            0xFF48C9B0, 0xFFFAD7A0, 0xFF808B96, 0xFFC0392B, 0xFF27AE60, 0xFFE8DAEF, 0xFF154360, 0xFFF39C12};
    static final int[] HAIR37 = {0xFF2B1B10, 0xFF4A2E1A, 0xFF111111, 0xFFC8A45A, 0xFF8B4A2B, 0xFF777777,
            0xFFE6CE8A, 0xFFA0522D, 0xFFB03A1E, 0xFFDDDDDD, 0xFF5A3A28, 0xFF3A2A20, 0xFF6A4A8A, 0xFF2E6AA0};
    static final int[] SKINS37 = {0xFFF1C27D, 0xFFE0AC69, 0xFFC68642, 0xFF8D5524, 0xFFFFDBAC, 0xFFF6D3B3,
            0xFFD9A066, 0xFFA86B3C, 0xFF6B3E1E, 0xFF4A2A16};
    static final class Corpse {
        float x, y, angle, radius, rise, age;
        int body, head, riseType, origin, nameSeed;
        /** Thrown by a blast or a car: sliding (and spinning) to a stop. */
        float vx, vy, spin;
        boolean zombie;
    }

    static final class Grenade {
        float sx, sy, tx, ty, t, dur;
        /** ((14.0) A mortar shell: high and slow, and a bigger blast.) */
        boolean shell;
        /** ((14.9) Where a shell was aimed (marked on the ground while it's in the air).) */
        float aimX, aimY;
        /** ((15.3) Who threw it (or fired the mortar): the dead it kills are theirs.) */
        Entity owner;
    }

    static final class Explosion {
        float x, y, r, life, max;
    }

    /** A pigeon: pecks around on the ground and flies off when startled. */
    static final class Bird {
        float x, y, tx, ty, flap, timer;
        boolean flying;
    }

    /** A gun dropped by a fallen cop or soldier. */
    static final class Pickup {
        float x, y, age;
        int rounds;
        boolean claimed;
        /** A supply crate: guns for several people, and ammo for anyone passing. Drops by parachute. */
        int uses;
        float drop;
        /** A particular gun (Entity.W_*), or a bat or axe (Entity.M_*), and when the player may pick it up again. */
        int weapon, melee;
        float lockUntil;
    }

    /** A barricade on a tile: {x, y, hp, tile index, the tile it replaced}. */
    final ArrayList<float[]> barriers = new ArrayList<float[]>();

    final City city;
    final Dispatch dispatch;
    /** ((12.0) For tests and tools: a fixed seed makes a run repeat exactly; -1, the default, is different each game.) */
    static long testSeed = -1;
    final Random rnd = testSeed >= 0 ? new Random(testSeed) : new Random();
    final ArrayList<Entity> entities = new ArrayList<Entity>();
    final ArrayList<Corpse> corpses = new ArrayList<Corpse>();
    final ArrayList<Grenade> grenades = new ArrayList<Grenade>();
    final ArrayList<Explosion> explosions = new ArrayList<Explosion>();
    final ArrayList<Pickup> pickups = new ArrayList<Pickup>();
    final ArrayList<Bird> birds = new ArrayList<Bird>();
    /** Explosions waiting to go off (gas pumps catching): {x, y, radius, damage, delay}. */
    final ArrayList<float[]> pendingBlasts = new ArrayList<float[]>();
    /** Tiles holding a burnt-out car or gas pump (saved with the game). */
    final boolean[] burned;
    final Fleet fleet;
    /** ((26.0) Convoy columns. */
    final Convoys convoys = new Convoys(this);
    /** ((26.0) Police boats, the mobile command post and special forces. */
    final Ops ops;

    // History for the stats screen: humans and zombies sampled every statStep seconds.
    static final int HISTORY = 240;
    final int[] histHumans = new int[HISTORY], histZombies = new int[HISTORY], histArmed = new int[HISTORY];
    /** ((23.0) The dead put down so far, alongside (not kept in saves: it starts again from a load). */
    final int[] histKills = new int[HISTORY];
    /** People who killed zombies and have died since: {name seed, type, role, kills}. */
    final ArrayList<int[]> fallenHeroes = new ArrayList<int[]>();
    int histCount;
    float statStep = 2f;
    float statTimer;
    int shotsFired, healed, peakZombies, hiding;
    /** Every living civilian: on the street, indoors, or in a car (so the count doesn't jump as people go in and out). */
    int civilians;
    /** Zombies shut inside buildings, out of sight. */
    int lurking;
    /** The power station has been overrun. (hospitalLost is only kept for saved games: no hospital care since 10.50.) */
    boolean blackout, hospitalLost;
    float powerThreat;
    /** Army checkpoints: bitten people turned away at the gate. */
    int turnedAway;
    /** Recruitment: the most cops and soldiers there have been, how many civilians joined up or refused. */
    int peakCops, peakSoldiers, recruits, refused;
    /** ((27.5) Volunteers signed up after the broadcast: as police auxiliaries, and with the army. */
    int volunteersPolice, volunteersArmy;
    /** How far the infection has evolved (0-5), how long the outbreak has run, and the recovery afterwards. */
    int mutation;
    /** Counters for records and achievements. */
    int tanksDeployed, collapsedCount, carsWrecked, biggestHorde;
    float outbreakTime, calmTime;
    /**
     * How far word of the outbreak has got: 0 nobody knows, 1 the first reports (word spreads person to
     * person), 2 everyone knows (emergency broadcast). Anyone who hears gunfire or sees one knows anyway.
     */
    int alert;
    /** ((15.1) This game, told apart from every other (for the hall of fame), and kept in its saves.) */
    long gameId = new java.util.Random().nextLong();
    boolean recovering;
    float lifeTimer;
    float recruitTimer, lastRecruitSay = -100;
    boolean policeDrive, armyDrive;
    final ArrayList<Entity> newRecruits = new ArrayList<Entity>();

    // Spatial hash (counting sort into cells).
    /** ((16.10) 64 across, not 32: a quarter as many cells, and their counts a quarter of the memory - about 17 MB less on
     * the Large map - for no difference in speed (measured). */
    static final int CELL = 64;
    final int gw, gh;
    final int[] cellStart, cellCount, zCount, aCount;
    Entity[] sorted = new Entity[512];
    /**
     * ((13.1) Where each in the grid was, and how big, in the grid's own order: looking through a cell for who's
     * close goes through these, side by side in memory, and only looks at the person themselves when they are.)
     */
    float[] sortedX = new float[512], sortedY = new float[512], sortedR = new float[512];
    int lodFrame;
    /** Counts up each time the grid is rebuilt; a leader marked with the current one has someone following. */
    int hashFrame;

    // Particles.
    static final int MAXP = 2500;
    final float[] px = new float[MAXP], py = new float[MAXP], pvx = new float[MAXP], pvy = new float[MAXP];
    final float[] plife = new float[MAXP], pmax = new float[MAXP], psize = new float[MAXP];
    final int[] pcol = new int[MAXP];
    final byte[] ptype = new byte[MAXP];
    static final byte P_DOT = 0, P_SMOKE = 1, P_FLASH = 2, P_FIRE = 3, P_CASING = 4, P_DEBRIS = 5;
    int pnext;

    // Bullet tracers.
    static final int MAXT = 300;
    final float[] tx0 = new float[MAXT], ty0 = new float[MAXT], tx1 = new float[MAXT], ty1 = new float[MAXT];
    final float[] tlife = new float[MAXT];
    int tnext;

    // Marks on the ground that fade (10.17): footprints, trails of blood, drag marks and spent cases.
    static final int MAXM = 1800;
    static final byte M_FOOT = 0, M_DRIP = 1, M_DRAG = 2, M_CASING = 3, M_PAW = 4;
    final float[] mx = new float[MAXM], my = new float[MAXM], mang = new float[MAXM], mborn = new float[MAXM];
    final byte[] mkind = new byte[MAXM];
    /** Which foot (footprints), or how dark (blood). */
    final byte[] mside = new byte[MAXM];
    int mcount;
    int mnext;
    /** How long each kind of mark lasts, in seconds. */
    // ((10.59) Blood drips and smears, and spent cases, go sooner: a big outbreak left the streets red for minutes.)
    static final float[] MARK_LIFE = {45, 50, 40, 120, 40};

    // Gunsmoke hanging over a firefight (10.17): slow puffs drifting off on the breeze.
    static final int MAXH = 260;
    final float[] hx = new float[MAXH], hy = new float[MAXH], hborn = new float[MAXH], hsize = new float[MAXH];
    int hnext;
    static final float HAZE_LIFE = 11;
    float marksTick;

    void mark(float x, float y, float angle, byte kind, int side) {
        effects.mark(x, y, angle, kind, side);
    }

    static final int MAXD = 700;
    final float[] dx = new float[MAXD], dy = new float[MAXD], dr = new float[MAXD];
    final int[] dcol = new int[MAXD];
    /** (10.59) How old each splat is: blood and the like fade away (see decalFade). */
    final float[] dage = new float[MAXD];
    static final float DECAL_FADE = 30, DECAL_LIFE = 50;

    float decalFade(int i) {
        return effects.decalFade(i);
    }
    /** Decal shape: a round splat, a skid streak (dang = direction, dr = length) or a puddle. */
    static final byte D_SPLAT = 0, D_SKID = 1, D_PUDDLE = 2, D_ACID = 3, D_GLASS = 4;
    final byte[] dkind = new byte[MAXD];
    final float[] dang = new float[MAXD];
    int dcount;
    int dnext;

    /** Sound events for GameView to play: {@link Sfx} id plus position. */
    static final int MAX_EVENTS = 128;
    final int[] evType = new int[MAX_EVENTS];
    final float[] evX = new float[MAX_EVENTS], evY = new float[MAX_EVENTS];
    int evCount;

    int maxEntities = 1600;
    /** How far past the cap people already in the game (getting out of cars) and help from outside may go. */
    static final int OVER_CAP = 900;
    /** Set while help from outside is getting out of its vehicles (or coming in on foot). */
    boolean arriving;
    /** Detail level, lowered on a phone that's struggling (see GameView.tuneQuality). */
    static final int Q_FULL = 0, Q_REDUCED = 1, Q_LOW = 2;
    int quality = Q_FULL;

    void setQuality(int q) {
        quality = Math.max(Q_FULL, Math.min(Q_LOW, q));
        fleet.trafficScale = quality == Q_FULL ? 1f : quality == Q_REDUCED ? 0.7f : 0.45f;
    }

    /** For comparing: false works everyone out in full every step, on screen or not. */
    static boolean LOD = true;
    /** For comparing: 2 works civilians out in full off screen too. (The dead and the armed always are.) */
    static int LOD_FULL;

    boolean onScreen(Entity e) {
        if (!LOD) return true;
        // ((10.57) Zoomed right out, people are a dot or two across: they count as out of sight, and get the
        // lighter treatment, instead of the whole city thinking every frame because it's all on screen.)
        return viewX1 >= 0 && viewX1 - viewX0 < 2000 && e.x > viewX0 - 80 && e.x < viewX1 + 80 && e.y > viewY0 - 80 && e.y < viewY1 + 80;
    }

    /** (10.54) Lets go of everything that can be worked out again when it's next needed, when memory runs short. */
    void freeMemory() {
        fieldCache.clear();
        city.clearFieldCaches();
        for (Dispatch.Incident inc : dispatch.incidents) inc.field = null;
        for (int i = 0, n = entities.size(); i < n; i++) if (entities.get(i).incident != null) entities.get(i).orderField = null;
    }

    /** ((22.4) The civilians at the start: on map version 36 the size's head count less the police and soldiers. */
    static int civiliansFor(CityConfig cfg, City city, int[] force) {
        int n = cfg.civilians(city.totalResidents, city.w);
        return cfg.popLadder() ? Math.max(n / 2, n - force[0] - force[1]) : n;
    }

    /** Auto's cap: room for everyone who lives there twice over (the dead), and ((22.4) on map version 36, for its people. */
    static int autoCap(City city) {
        int cap = Math.max(2500, city.totalResidents * 2 + 1000);
        if (city.cfg.popLadder()) cap = Math.max(cap, city.cfg.civilians(city.totalResidents, city.w) * 2 + 1000);
        return cap;
    }

    /** Sets the population cap; 0 means Auto, sized to the city: everyone who lives there, and room for the dead. */
    void setMaxPopulation(int cap) {
        maxEntities = cap > 0 ? cap : autoCap(city);
    }
    boolean gore = true;
    /** ((18.4) The Destruction setting: heavy. */
    boolean heavy;
    int collapseChain;

    float time, shake;
    /** What the player can see (world units), so cars aren't made to vanish or appear in plain view. */
    float viewX0, viewY0, viewX1 = -1, viewY1 = -1;
    float fieldTimer;
    final int[] counts = new int[Entity.TYPE_COUNT];
    int humans, zombies, turned, zombiesKilled, civiliansLost;
    /** People who got away on evacuation trains. */
    int evacuated;
    String message;
    float messageTime;
    /** How fast sprinting uses up stamina (per second, out of 1). */
    static float STAMINA_DRAIN = 0.1f;
    /** Balance: how much of a gun's damage counts against zombies, and how likely a bite is to infect. */
    static float GUN_DAMAGE = 0.2f, BITE_INFECT = 2.2f;
    /** Chance any hit on a zombie is a headshot that drops it on the spot. */
    static float HEADSHOT = 0.04f;
    /** ((12.1) Balance) How much of the time police and soldiers land the stopping shot they're trained for. */
    static float TRAINED_HEADSHOT = 0.1f;
    /** (12.9) How close one of the dead can come before someone with an empty gun gets away from it. */
    static final float DRY_KEEP = 60;
    /** ((12.1) Balance) Chance a zombie brought down by body shots alone gets back up a few seconds later. */
    static float GET_UP = 0.6f;
    /** How far (in tiles of walking) idle zombies can smell the living. */
    /** How far (in tiles, by the way round) the dead can smell the living: the street, not across town. */
    static int SCENT = 40;
    /** Balance: how much health police and soldiers have compared to their base. */
    static float ARMED_HP = 0.6f;
    /** The city has fallen once fewer than this share of its people are left alive (and the dead outnumber them). */
    static float FALLEN = 0.25f;

    /** An outbreak is under way (zombies and people both on the map). */
    boolean outbreak;

    // ------------------------------------------------------------------ the strain and the war

    static final int TR_FAST_TURN = 0, TR_SLOW_TURN = 1, TR_VIRULENT = 2, TR_WEAK_BITE = 3, TR_TOUGH = 4, TR_FRAIL = 5,
            TR_SWIFT = 6, TR_RESTLESS = 7, TR_KEEN = 8, TR_DULL = 9,
            // ((12.4) More to tell one outbreak from the next.)
            TR_PACK = 10, TR_LONER = 11, TR_QUICK_RISE = 12, TR_SLOW_RISE = 13, TR_RUNNERS = 14, TR_BRUTES = 15, TR_RELENTLESS = 16,
            TR_COUNT = 17;
    /** The traits saves before 12.4 hold (the rest are saved after them, in version 51). */
    static final int TR_OLD = 10;
    static final String[] TRAIT_NAMES = {"Fast-acting", "Slow-acting", "Virulent", "Weak bite", "Tough", "Frail",
            "Swift", "Restless dead", "Keen senses", "Dull senses", "Pack hunters", "Loners", "Quick to rise", "Slow to rise",
            "Many runners", "Many brutes", "Relentless"};
    static final String[] TRAIT_REVEALS = {
            "Doctors: the bitten are turning in seconds. Get away from anyone who's been bitten.",
            "Doctors: this infection takes its time. The bitten can walk around for a while before they turn.",
            "Doctors: nearly every bite is infecting people.",
            "Doctors: a lot of bites aren't infecting people at all.",
            "Police: it takes far more bullets to put them down than it should.",
            "Police: they go down easier than we feared.",
            "Witnesses: they're faster than they look.",
            "Reports: people who were never bitten are getting back up too.",
            "Survivors: they can spot you from a long way off.",
            "Survivors: they don't notice you until you're close.",
            "Survivors: when one of them finds you, the rest come running. They hunt in packs.",
            "Reports: they keep to themselves. One spotting you doesn't bring the others.",
            "Reports: the dead are getting back up within moments of falling.",
            "Reports: it takes the dead a good while to get back up.",
            "Witnesses: a lot of the newly turned are running, not shambling.",
            "Military: we're seeing far more of the big ones than expected.",
            "Survivors: once they're after you, they don't give up.",
    };
    /** (12.4) Each strain's look: a tint over the dead's skin, so one outbreak doesn't look like the last. */
    static final int[] STRAIN_TINTS = {0, 0xFF8C918C, 0xFF8E7092, 0xFF6C8AA6, 0xFFB4A45E, 0xFF9E6A5C, 0xFF5E9E7A};

    int strainTint() {
        return war.strainTint();
    }
    /** This outbreak's strain: a few hidden traits, found out one by one as the war goes on. */
    final boolean[] trait = new boolean[TR_COUNT], traitKnown = new boolean[TR_COUNT];
    String strainName;
    int bites;
    /** Share of the fighting strength on the people's side (0 to 1), and who is ahead: 1 people, -1 zombies. */
    float warBalance = 0.5f;
    boolean warFresh = true;
    int warLead;
    /** How the last war ended: 0 not yet, 1 the city survived, 2 the city fell. */
    int warResult;
    /** A big announcement across the screen (the end of the war, a turning point) and how long it shows. */
    String warBanner, warSub;
    float warBannerTime;
    /** Counts banners, so the screen can queue each one once. */
    int bannerSeq;
    int startHumans;

    /** How ready the city is: 0 normal, 1 unprepared, 2 panicking, 3 a gun town, 4 well prepared. */
    int readiness;
    boolean readinessKnown;
    static final String[] READINESS_NEWS = {"",
            "Police: we weren't ready for this. Ammunition is short, and the towns around can spare only half the help.",
            "News: panic is sweeping the city. People are running instead of hiding.",
            "News: more people here own a gun than anyone thought, and they're using them.",
            "Military: we've been preparing for this. Extra units are standing by."};

    // ------------------------------------------------------------------ (11.5) how this city's leaders decide

    /**
     * (11.5) Who's making the decisions this time, hidden like the strain: a hawk (seals districts off early, bombs
     * sooner), a cautious hand (keeps people indoors, calls in the Guard), slow to act (everything late), or a town
     * that arms its people. Together with the strain and how ready the city was, no two outbreaks go the same way.
     */
    static final int CMD_BALANCED = 0, CMD_HAWK = 1, CMD_CAUTIOUS = 2, CMD_SLOW = 3, CMD_ARMED = 4, CMD_COUNT = 5;
    int command;
    /** Which of the city's emergency decisions have been taken (bits by DEC_), and time until it next thinks. */
    int decisions;
    /** ((12.0) Zombies put down by the police, the army and civilians (the rest went to cars, blasts and the like).) */
    final int[] killsBy = new int[3];
    float decideCd = 45;
    static final int DEC_CURFEW = 1, DEC_QUARANTINE = 2, DEC_GUNS = 4, DEC_GUARD = 8, DEC_CONVOY = 16;

    void leadersDecide(float dt) {
        war.leadersDecide(dt);
    }

    float turnTime(float base) {
        return war.turnTime(base);
    }

    void banner(String title, String sub) {
        stats.highlight(title.charAt(0) + title.substring(1).toLowerCase(), controlled != null ? controlled.x : city.worldW() / 2,
                controlled != null ? controlled.y : city.worldH() / 2);
        warBanner = title;
        warSub = sub;
        warBannerTime = 6;
        bannerSeq++;
        dispatch.say(Dispatch.WHO_INFO, null, title.charAt(0) + title.substring(1).toLowerCase() + ". " + sub,
                city.worldW() / 2, city.worldH() / 2);
    }

    World(CityConfig cfg) {
        city = new City(cfg);
        City.progress(0.72f, "Traffic and services");
        Country.current = city.country;
        war.rollStrain();
        // ((17.0) No virus in a gang war or an invasion.)
        dispatch = new Dispatch(this, cfg.reinforcements());
        fleet = new Fleet(this);
        ops = new Ops(this);
        burned = new boolean[city.w * city.h];
        gw = (int) Math.ceil(city.worldW() / CELL);
        gh = (int) Math.ceil(city.worldH() / CELL);
        cellStart = new int[gw * gh + 1];
        zCount = new int[gw * gh];
        aCount = new int[gw * gh];
        cellCount = new int[gw * gh];
        // ((13.5) In place of three arrays of where the next of each kind went.)
        hashCnt = new int[gw * gh * 3];
        // ((20.0) The director, with dice of its own, so nothing else's luck changes with it.)
        difficulty = Math.max(0, Math.min(3, cfg.difficulty));
        zPace = Director.Z_SPEED[difficulty];
        director = new Director(difficulty, (testSeed >= 0 ? testSeed + 7 : new Random().nextLong()) * 31 + 5);
        applyRules();
        // ((11.5) No courthouse armoury: the government buildings are just buildings now.)
        if (false) {
            City.Building b = city.courthouse;
            courtArmoury = new City.Facility(City.FACILITY_POLICE, b.doorX, b.doorY, 40, b.doorX, b.doorY, b.name);
        }
    }

    boolean build(float x, float y, int kind) {
        return population.build(x, y, kind);
    }

    /**
     * (10.56) The city's own police and army as the game starts them: {officers in town (the small towns' own
     * stations included), soldiers at the town's base, soldiers at each base out in the country}.
     */
    static int[] forces(CityConfig cfg, City city) {
        City.Facility base = city.mainBase();
        int cops = cfg.cops(city.totalResidents), soldiers = cfg.soldiers(city.totalResidents);
        // (A small town's own police station out in the country has its own officers, on top of the city's.)
        for (City.Facility f : city.facilities) if (f.kind == City.FACILITY_POLICE && f.satellite) cops += 6;
        // (10.35) The police always outnumber the army: every soldier on the map (both bases and the checkpoints)
        // comes to no more than two in five of the police force, the town's base taking most of it.
        int satBases = 0;
        for (City.Facility f : city.facilities) if (f.kind == City.FACILITY_BASE && f.satellite && f != base) satBases++;
        int military = Math.max(6, Math.round(cops * 0.4f) - city.checkpointPosts.size());
        int townShare = satBases > 0 && base != null ? military * 3 / 5 : military;
        int satEach = satBases > 0 ? Math.max(6, (military - (base != null ? townShare : 0)) / satBases) : 0;
        // ((15.5) Not in the Military Town: there the army outnumbers the police many times over.)
        if (soldiers > 0 && !cfg.militaryTown()) soldiers = Math.min(soldiers, Math.max(6, townShare));
        return new int[]{cops, soldiers, satEach};
    }

    /**
     * (10.56) Everyone the game will start with on this city, for the New Game screen: {people, police, soldiers},
     * counted the way the game counts them (the highway patrol and sheriff's deputies,
     * the rangers, the marksmen, the checkpoint guards and the guardsmen at the armory all included). Before, it
     * showed only the residents and the town's own officers, a good deal short of what the game then had.
     */
    static int[] forecast(CityConfig cfg, City city, int maxPopulation) {
        int[] f = forces(cfg, city);
        int cap = maxPopulation > 0 ? maxPopulation : autoCap(city);
        int people = Math.min(civiliansFor(cfg, city, f), Math.max(cap / 2, cap * 4 / 5 - f[0] - f[1]));
        if (cfg.wilds()) people += city.fireTowers.size();
        int stations = 0, satBases = 0;
        City.Facility base = city.mainBase();
        for (City.Facility x : city.facilities) {
            if (x.kind == City.FACILITY_POLICE && !x.fed) stations++;
            if (x.kind == City.FACILITY_BASE && x.satellite && x != base) satBases++;
        }
        int cops = f[0];
        if (cfg.nature()) cops += stations;
        if (city.hwyAxis >= 0) cops += Math.max(4, (city.hwyAxis == 0 ? city.w : city.h) / 50);
        if (city.country.ruralName != null && city.townX0 > 0) cops += city.w > 300 ? 3 : 2;
        // (Two rangers at the station and at each outpost, and one more out in each of their trucks.)
        if (cfg.wilds() && cops > 0) {
            if (city.rangerStationName != null) cops += 3;
            cops += 3 * city.outpostNames.size();
        }
        int soldiers = 0;
        if (f[1] > 0) {
            soldiers = f[1] + city.checkpointPosts.size() + satBases * f[2] + (f[1] >= 5 ? 1 : 0);
        }
        // (The helicopters' crews on their pads: a rescue helicopter's four, a pilot for each of the others.)
        for (float[] p : city.helipads()) {
            int kind = City.padKind(p);
            if (kind == City.PAD_RESCUE) cops += 4;
            else if (kind == City.PAD_POLICE) cops++;
            else soldiers++;
        }
        if (city.armory != null && city.armoryGate != null) soldiers += 4;
        return new int[]{people, cops, soldiers};
    }

    void populate(CityConfig cfg) {
        population.populate(cfg);
    }

    /**
     * Everyone lives somewhere. People are taken household by household from the city's homes: some are at
     * home, some out on errands, and some households out together (with the dog).
     */
    boolean popLadderNow;

    void spawnBirds() {
        wilds.spawnBirds();
    }

    /** Redraws the map in the current Graphics setting, keeping the scorch marks. */
    void redrawCity() {
        city.redraw();
        for (int i = 0; i < burned.length; i++) if (burned[i]) city.charTile(i % city.w, i / city.w);
    }

    /** Marks a car or pump tile as burnt and scorches it into the map. */
    void burnTile(int i) {
        burned[i] = true;
        city.charTile(i % city.w, i / city.w);
    }

    void blastLater(float x, float y, float radius, float damage, float delay) {
        combat.blastLater(x, y, radius, damage, delay);
    }

    boolean personAhead(float x, float y, float angle) {
        return spatial.personAhead(x, y, angle);
    }

    void armCivilian(Entity e, int rounds) {
        population.armCivilian(e, rounds);
    }

    int zombieCount() {
        return counts[Entity.ZOMBIE] + counts[Entity.RUNNER] + counts[Entity.BRUTE] + counts[Entity.ZOMBIE_DOG] + counts[Entity.BRUISER]
                + counts[Entity.GIANT] + counts[Entity.SCREAMER] + counts[Entity.ARMOURED];
    }

    int humanCount() {
        return counts[Entity.CIVILIAN] + counts[Entity.COP] + counts[Entity.SOLDIER];
    }

    /** People sitting in cars right now. */
    int riding;

    void emit(int sound, float x, float y) {
        if (evCount >= MAX_EVENTS) return;
        evType[evCount] = sound;
        evX[evCount] = x;
        evY[evCount] = y;
        evCount++;
    }

    // ------------------------------------------------------------------ creation

    Entity spawn(int type, float x, float y) {
        return population.spawn(type, x, y);
    }

    Entity puppetZombie(int i) {
        return population.puppetZombie(i);
    }

    Entity spawnCop(int role, float x, float y) {
        return population.spawnCop(role, x, y);
    }

    void kitOut(Entity e) {
        population.kitOut(e);
    }

    void stationUnits() {
        population.stationUnits();
    }

    /** ((22.0) The ready-made groups in the pickers: who's in each, by role. */
    static final int[] FIRE_TEAM = {Entity.ROLE_RIFLE, Entity.ROLE_RIFLE, Entity.ROLE_GRENADIER, Entity.ROLE_GUNNER, Entity.ROLE_CORPSMAN};
    static final int[] RIFLE_SQUAD = {Entity.ROLE_COMMANDER, Entity.ROLE_RIFLE, Entity.ROLE_RIFLE, Entity.ROLE_RIFLE, Entity.ROLE_RIFLE,
            Entity.ROLE_GRENADIER, Entity.ROLE_GUNNER, Entity.ROLE_CORPSMAN, Entity.ROLE_SNIPER};
    static final int[] RESPONSE_TEAM = {0, 0, 0, Entity.ROLE_RIOT, Entity.ROLE_MARKSMAN};
    static final int[] TACTICAL_TEAM = {Entity.ROLE_SWAT, Entity.ROLE_SWAT, Entity.ROLE_SWAT, Entity.ROLE_SWAT, Entity.ROLE_MARKSMAN, Entity.ROLE_K9};

    ArrayList<Entity> spawnGroup(int[] roles, boolean cops, float x, float y) {
        return population.spawnGroup(roles, cops, x, y);
    }

    Entity spawnSoldier(int role, float x, float y) {
        return population.spawnSoldier(role, x, y);
    }

    void applyRole(Entity e, int role) {
        population.applyRole(e, role);
    }

    void makeAgency(Entity e, int agency) {
        population.makeAgency(e, agency);
    }

    void reinforce(Entity e) {
        population.reinforce(e);
    }

    /** Full spare ammo for a unit's weapon. */
    static int fullReserve(Entity e) {
        // ((11.4) Their own gun's rounds, where it isn't the usual.)
        int spare = spareFor(e.weapon);
        if (spare >= 0 && (e.type == Entity.COP || e.type == Entity.SOLDIER)) return spare;
        if (e.type != Entity.SOLDIER)
            return e.role == Entity.ROLE_SWAT ? 150 : e.role == Entity.ROLE_MARKSMAN ? 40 : e.type == Entity.COP ? 45 : 48;
        switch (e.role) {
            case Entity.ROLE_CORPSMAN: return 120;
            case Entity.ROLE_GRENADIER: return 150;
            case Entity.ROLE_COMMANDER: return 60;
            case Entity.ROLE_SNIPER: return 45;
            case Entity.ROLE_GUNNER: return 200;
            case Entity.ROLE_GUARD: return 120;
            default: return 180;
        }
    }

    Entity create(int type, float x, float y) {
        return population.create(type, x, y);
    }

    int bestKills() {
        return stats.bestKills();
    }

    ArrayList<Entity> topFighters(int n) {
        return stats.topFighters(n);
    }

    /** ((13.3) What someone is, for the Top Fighters list: "SWAT officer", "Machine gunner", "Civilian"...) */
    static String kindOf(Entity e) {
        if (e.role == Entity.ROLE_VOLUNTEER && e.type == Entity.COP) return "Police auxiliary";
        if (e.role > 0 && e.role < Entity.ROLE_NAMES.length && (e.type == Entity.COP || e.type == Entity.SOLDIER)) return Entity.ROLE_NAMES[e.role];
        if (e.type == Entity.COP) return "Police officer";
        return e.type >= 0 && e.type < Entity.NAMES.length ? Entity.NAMES[e.type] : "";
    }

    ArrayList<int[]> heroes(int n) {
        return stats.heroes(n);
    }

    void afterLoad() {
        population.afterLoad();
    }

    static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return 0xFF000000 | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8)
                | (int) (ab + (bb - ab) * t);
    }

    void erase(float x, float y, float r) {
        population.erase(x, y, r);
    }

    void clearAll() {
        population.clearAll();
    }

    // ------------------------------------------------------------------ update

    /**
     * ((11.5) How far the dead (or, for the dead, the living) are from someone, for deciding how often they need
     * thinking about: looked up afresh every few steps, not every step - the fields themselves are only worked
     * out a few times a second.)
     */
    int lodZDist(Entity e) {
        if (lodFrame - e.lodZAt >= 6 || e.lodZAt == 0) {
            e.lodZ = city.fieldAt(city.zombieDist, e.x, e.y);
            // (Spread out a little, so not everyone made at once looks again on the same step.)
            e.lodZAt = lodFrame - (e.nameSeed & 3);
        }
        return e.lodZ;
    }

    int lodHDist(Entity e) {
        if (lodFrame - e.lodHAt >= 6 || e.lodHAt == 0) {
            e.lodH = city.fieldAt(city.humanDist, e.x, e.y);
            e.lodHAt = lodFrame - (e.nameSeed & 3);
        }
        return e.lodH;
    }

    /** (10.34) How many frames' worth at a time the calm off screen, and the busy off screen, move and think. */
    int lodCalm = 3, lodBusy = 2, lodNear = 2;
    /**
     * ((13.1) For each place in the list of people, the step on which whoever's there next has anything to do, when
     * they're calm and off screen: until then they're passed over without being looked at at all. Just looking
     * at each of thirty thousand people every step, to find there was nothing to do, was a fifth of the time.)
     */
    int[] lodWake = new int[0], lodSet = new int[0];
    /** ((16.8) The step on which each was last calm and off screen in the biggest cities: see separate(). */
    int[] lodQuiet = new int[0];
    float lodViewX = -1e9f, lodViewY = -1e9f;

    /** ((13.1) Whoever's at this place in the list has nothing to do until their next turn, every so many steps.) */
    /** ((16.8) Over eighteen thousand people: the biggest cities' slower pace for those off screen. */
    boolean lodHuge;

    /** ((22.1) Calm civilians off screen wait in peace (see update); a switch for comparisons. */
    static boolean PAUSE_UNSEEN = true;
    boolean peaceful;

    void sleep(int i, int every) {
        // (Not if they've just died, or been taken over, or anything has been added in the meantime.)
        if (i >= lodWake.length || entities.get(i).dead || entities.get(i) == controlled) return;
        lodWake[i] = lodFrame + every - (i + lodFrame) % every;
        lodSet[i] = lodFrame;
    }

    /** ((24.0) This step's length, for what's timed deep in the movement code (it counted frames, not time). */
    float stepDt = 1 / 30f;

    void update(float dt) {
        stepDt = dt;
        beginStep(dt);
        scentFields(dt);
        fieldworks.toolsStep(dt);
        events.eventsStep(dt);
        ops.step(dt);
        dispatch.update(dt);
        if ((rosterCd -= dt) <= 0) {
            rosterCd = 5;
            posts.staffPosts();
        }
        watchControlled();
        stats.recordReplay(dt);
        lodFrame++;
        wakeForView();
        thinkAndMoveAll(dt);
        for (int i = 0; i < surging.size(); i++) zombieAI.surge(surging.get(i));
        surging.clear();
        movement.separate();
        civilianAI.stampedeNews(dt);
        fleet.update(dt);
        convoys.update(dt);
        if (!policeDrones.isEmpty()) fieldworks.dronesStep(dt);
        combat.updateBlasts(dt);
        war.updateFirebomb(dt);
        sandbox.updateMedkits(dt);
        if (alarmTime > 0) alarmTime -= dt;
        wilds.updateBirds(dt);
        fadeNoiseRings(dt);
        wilds.updateWildlife(dt);
        if (lodFrame % 15 == 0) containment.checkpointGuards();
        if (lodFrame % 150 == 0) containment.watchBases(dt * 150);
        coolHeat(dt);
        wilds.updateTowers(dt);
        containment.updateCordon(dt);
        rides.rideCheck(dt);
        fieldworks.updateDrones(dt);
        exodus.updateDanger(dt);
        combat.updateGrenades(dt);
        // ((13.5) Every other step in the biggest cities: it means looking at everyone. (Anyone at nought in between
        // is passed over as if gone: see the loop above.))
        if (entities.size() <= 18000 || (lodFrame & 1) == 0) death.cleanup();
        death.updateCorpses(dt);
        indoors.updateBuildings(dt);
        holdoutLife.updateHoldouts(dt);
        aftermathLife.updateAftermath(dt);
        exodus.updateExodus(dt);
        sandbox.updatePickups(dt);
        effects.updateEffects(dt);
        if (!boats.isEmpty()) exodus.moveBoats(dt);
        if (city.ferry != null) prison.moveFerry(dt);
        if (cargoPlane != null) aftermathLife.moveCargoPlane(dt);
        slowClocks(dt);
        countHeads(dt);
    }

    /** The start of a step: the clock, the walking router's budget and map, and the grid of who's where. */
    private void beginStep(final float dt) {
        // ((10.36) The walking router is quick enough for far more routes than before: about forty a second.)
        pathBudget = Math.min(12, pathBudget + dt * 40);
        // (10.49) The walking router's map, worked out a few clusters at a time instead of all at once mid-trip.
        // ((13.3) More at a time on the bigger maps: a Colossal map has six thousand clusters, which took over a minute
        // of play at three a step.)
        int clusters = (city.w / Nav.CS) * (city.h / Nav.CS);
        city.warmPaths((quality == Q_FULL ? 3 : 1) + clusters / 200);
        time += dt;
        shake = Math.max(0, shake - dt * 3);
        if (messageTime > 0) messageTime -= dt;
        if (warBannerTime > 0) warBannerTime -= dt;
        // ((11.5) In a crowd of thousands, the grid of who's where every other step: nobody moves more than a step or
        // two in between, and everything that asks it allows for that.)
        // ((13.5) And every fourth step with over eighteen thousand: it was a fifth of each step on the biggest maps.)
        int hashEvery = entities.size() < 3000 ? 1 : entities.size() > 18000 ? 4 : 2;
        if (lodFrame % hashEvery == 0 || hashSize < 0) spatial.buildHash();
        // ((13.3) The ways on foot to the stations and bases, one at a time ahead of need (see City.warmFacilityField).)
        if (lodFrame % 15 == 7) city.warmFacilityField();
    }

    /** The scent searches (where the living and the dead are from anywhere), started every so often and worked through a slice a step. */
    private void scentFields(final float dt) {
        fieldTimer -= dt;
        occTimer -= dt;
        // ((24.0) A pass of the scent searches is started every so often and worked through a slice a step (see
        // City.startFields), not done all at once: it was the longest step of every few.)
        float fieldEvery = quality == Q_FULL ? 0.25f : quality == Q_REDUCED ? 0.4f : 0.6f;
        // ((10.34) Half as often in a crowded city: it's a whole-map search, and nobody moves far in between.)
        if (entities.size() > 12000) fieldEvery *= 2;
        // ((11.5) And half as often again from six thousand.)
        else if (entities.size() > 6000) fieldEvery *= 1.5f;
        // ((13.5) With over eighteen thousand, each field every three quarters of a second.)
        if (entities.size() > 18000) fieldEvery *= 1.5f;
        if (fieldTimer <= 0 && !city.fieldsBusy()) {
            fieldTimer = fieldEvery;
            city.startFields(entities, true, true);
        }
        if (city.fieldsBusy()) {
            // Enough a step to be done a step or so before the next pass is due.
            int steps = Math.max(1, (int) (fieldEvery / Math.max(dt, 1e-3f)) - 1);
            city.fieldWork(entities, Math.max(FIELD_MIN_WORK, city.fieldLastWork / steps + 1));
        }
    }

    /** The one the player is: let go of when they or their car are gone. */
    private void watchControlled() {
        if (controlled != null) {
            Fleet.Vehicle car = controlledCar;
            if (car != null && (!fleet.vehicles.contains(car) || car.player != controlled || !controlled.removed)) {
                if (car.player == controlled) car.player = null;
                controlledCar = null;
            }
            if (controlledCar == null && (controlled.dead || controlled.removed)) {
                controlled = null;
                joyX = joyY = 0;
                ctrlAttack = false;
            }
        }
    }

    /** Who's looked at this step: everyone again when peace ends or the view moves. */
    private void wakeForView() {
        if (lodWake.length < entities.size()) {
            lodWake = Arrays.copyOf(lodWake, entities.size() + 1024);
            lodSet = Arrays.copyOf(lodSet, lodWake.length);
            lodQuiet = Arrays.copyOf(lodQuiet, lodWake.length);
        }
        // ((22.1) Peace: nobody dead anywhere, and a screen to be off. The moment that ends, everyone is looked at again.)
        boolean wasPeaceful = peaceful;
        peaceful = PAUSE_UNSEEN && zombies == 0 && viewX1 > viewX0 && controlled == null;
        if (wasPeaceful && !peaceful) Arrays.fill(lodWake, 0);
        // (Looking somewhere else, everyone is looked at again: those coming into view move straight away.)
        if (Math.abs(viewX0 - lodViewX) > 24 || Math.abs(viewY0 - lodViewY) > 24) {
            Arrays.fill(lodWake, 0);
            lodViewX = viewX0;
            lodViewY = viewY0;
        }
    }

    /** Everyone thinks and moves - in full where it matters, less often where nobody is watching and nothing is near. */
    private void thinkAndMoveAll(final float dt) {
        for (int i = 0, n = entities.size(); i < n; i++) {
            if (lodWake[i] - lodFrame > 0) continue;
            Entity e = entities.get(i);
            e.lodStep = 1;
            if (e.dead || e.hp <= 0) continue;
            // ((21.3) The dead are worked out in full every step wherever they are: with the lighter treatment off screen,
            // fights nobody was watching went differently - fewer bitten, fewer killed - than the same fights on screen.
            // There are few enough of them that it costs next to nothing. (Police and soldiers only get the lighter
            // treatment a long way from any of the dead, and civilians' makes no difference: both are kept.)
            if (!LOD || e.isZombie() || (LOD_FULL & 2) != 0 && e.type == Entity.CIVILIAN) {
                think(e, dt);
                if (!e.dead && e.lodStep > 0) movement.move(e, dt);
                continue;
            }
            if (i == 0) {
                // ((11.5) From six thousand people up, not twelve: a Large city in a panic is about ten thousand.)
                boolean crowd = entities.size() > 6000;
                // ((13.1) And with over eighteen thousand (the 20,000 to 30,000 of the bigger cities now), those with
                // nothing going on and nobody watching less often again.)
                boolean huge = entities.size() > 18000;
                // ((13.5) A little less often again in the biggest cities: nobody sees them, and nothing's near.)
                // ((16.8) Every fifteenth step with over eighteen thousand: half a second's walk at a time - under a tile,
                // never through a wall - for people nobody is watching, with nothing going on and none of the dead near.)
                lodCalm = huge ? 15 : crowd ? 4 : 3;
                lodHuge = huge;
                lodBusy = huge ? 6 : crowd ? 3 : 2;
                lodNear = huge ? 4 : 2;
            }
            // ((22.1) In peace - not one of the dead anywhere - someone going about their day where nobody can see them
            // waits where they are until they're on screen, or the first of the dead turns up: what they do off
            // screen then can't matter to anyone. The tens of thousands of calm walkers off screen were most of
            // each step before an outbreak.)
            if (peaceful && e.type == Entity.CIVILIAN && e.task == Dispatch.T_NONE && e.fleeTimer <= 0 && e != controlled
                    && e.rig == null && e.ride == null && !onScreen(e)) {
                e.lodStep = 0;
                sleep(i, 45);
                continue;
            }
            // Far from any zombie and just going about their day: they decide what to do a third as often
            // (but still move every frame), which lets a city of thousands run smoothly.
            // ((10.31) Families and friends out together too, while whoever they're with is calm as well.)
            if (e.type == Entity.CIVILIAN && e.task == Dispatch.T_NONE && e.fleeTimer <= 0 && e != controlled
                    && (e.leader == null || (e.leader.type == Entity.CIVILIAN && e.leader.task == Dispatch.T_NONE && e.leader.fleeTimer <= 0 && e.leader != controlled))
                    && lodZDist(e) > 30) {
                boolean seen = onScreen(e);
                // (10.31) Off screen they also only move, and get out of each other's way, every third frame, three
                // frames' worth at a time: with tens of thousands in a Gigantic city, that's most of the work.
                // ((10.34) A step further still with over twelve thousand people about.)
                if (!seen) e.lodStep = (i + lodFrame) % lodCalm == 0 ? lodCalm : 0;
                // (On a struggling phone, those off screen even less often.)
                int every = quality == Q_FULL || seen ? lodCalm : quality == Q_REDUCED ? 5 : 8;
                // ((16.8) In the biggest cities, off screen, they still move as often but make up their minds every other
                // time: twice a second or so for someone strolling along with nothing near, which nobody sees. Deciding
                // was the largest single part of each step on the biggest maps.)
                int decide = !seen && lodHuge && every == lodCalm ? every * 2 : every;
                if ((i + lodFrame) % decide == 0) think(e, dt * decide);
                if (!seen && every == lodCalm) sleep(i, lodCalm);
                // ((16.8) Marked for separate(), which then passes them over without looking at them again.)
                if (!seen && lodHuge && e.task == Dispatch.T_NONE && e.fleeTimer <= 0 && e.stun <= 0) lodQuiet[i] = lodFrame;
            } else if (e.isZombie() && e.charge <= 0 && e.noiseTimer <= 0 && e != controlled && !onScreen(e)
                    && lodHDist(e) > 40) {
                // The same for the dead shuffling about off screen with nobody near (since 10.31 on any phone,
                // and they move every other frame too).
                e.lodStep = (i + lodFrame) % lodBusy == 0 ? lodBusy : 0;
                int every = quality == Q_LOW ? 3 : lodBusy;
                if ((i + lodFrame) % every == 0) think(e, dt * every);
                if (every == lodBusy) sleep(i, lodBusy);
            } else if ((e.type == Entity.COP || e.type == Entity.SOLDIER) && e != controlled && !e.aiming && e.rig == null
                    && (e.task == Dispatch.T_NONE || e.task == Dispatch.T_POST || e.task == Dispatch.T_GUARD)
                    && !onScreen(e) && lodZDist(e) > 40) {
                // ((10.57) Police and soldiers with nothing near them, on patrol, at a post or guarding a zone, off
                // screen: every other frame too, like everyone else with nothing going on.)
                e.lodStep = (i + lodFrame) % lodBusy == 0 ? lodBusy : 0;
                if ((i + lodFrame) % lodBusy == 0) think(e, dt * lodBusy);
                sleep(i, lodBusy);
            } else if (lodHuge && (e.type == Entity.COP || e.type == Entity.SOLDIER) && e != controlled && !e.aiming && e.rig == null
                    && e.ride == null && !onScreen(e) && lodZDist(e) > 40) {
                // ((16.8) In the biggest cities, police and soldiers off screen on their way somewhere, with none of the
                // dead for forty tiles: every other step too. Hundreds of them were worked out every step.)
                e.lodStep = (i + lodFrame) % 2 == 0 ? 2 : 0;
                if ((i + lodFrame) % 2 == 0) think(e, dt * 2);
                sleep(i, 2);
            } else if (e.type == Entity.CIVILIAN && e != controlled && !onScreen(e) && lodZDist(e) > 30) {
                // (10.31) And people off screen who know what's going on but have none of the dead near them:
                // every other frame (a Gigantic city in a panic is tens of thousands of them).
                e.lodStep = (i + lodFrame) % lodBusy == 0 ? lodBusy : 0;
                int every = quality == Q_FULL ? lodBusy : quality == Q_REDUCED ? 3 : 4;
                // ((16.8) Every other time in the biggest cities, as above.)
                int decide = lodHuge && every == lodBusy ? every * 2 : every;
                if ((i + lodFrame) % decide == 0) think(e, dt * decide);
                if (every == lodBusy) sleep(i, lodBusy);
            } else if (e != controlled && e.rig == null && !e.aiming && e.ride == null && !onScreen(e)
                    && (e.type == Entity.CIVILIAN ? lodZDist(e) > 8
                        : e.isZombie() && e.charge <= 0 && lodHDist(e) > 8)) {
                // ((11.5) Off screen and near the trouble but not in the thick of it - running from the dead a few
                // streets off, or one of the dead heading for the living: every other frame, two frames' worth.
                // Only those right up against it, or on screen, are worked out every frame.)
                // ((13.1) Every third frame in the biggest cities.)
                e.lodStep = (i + lodFrame) % lodNear == 0 ? lodNear : 0;
                if ((i + lodFrame) % lodNear == 0) think(e, dt * lodNear);
                sleep(i, lodNear);
            } else think(e, dt);
            // ((13.1) Each moves as soon as they've thought, in the same pass: a second pass through tens of
            // thousands of people just to move them cost almost as much again in fetching each one.)
            if (!e.dead && e.lodStep > 0) movement.move(e, dt * e.lodStep);
        }
    }

    /** The rings of a noise spreading out, fading. */
    private void fadeNoiseRings(final float dt) {
        for (int i = noiseRings.size() - 1; i >= 0; i--) {
            float[] r = noiseRings.get(i);
            r[3] += dt;
            if (r[3] > 1.4f) noiseRings.remove(i);
        }
    }

    /** How loud the fighting has been, dying away. */
    private void coolHeat(final float dt) {
        if (fightHeat != null && lodFrame % 30 == 0) {
            float keep = (float) Math.pow(0.98, dt * 30);
            for (int i = 0; i < fightHeat.length; i++) fightHeat[i] *= keep;
        }
        if (lodFrame % 30 == 15) noise.coolBattle((float) Math.pow(0.97, dt * 30));
    }

    /** What happens every second or few: the city's life, recruitment, and recruits joining. */
    private void slowClocks(final float dt) {
        lifeTimer -= dt;
        if (lifeTimer <= 0) {
            lifeTimer = 1;
            aftermathLife.cityLife();
        }
        recruitTimer -= dt;
        if (recruitTimer <= 0) {
            recruitTimer = 4;
            recruiting.recruitment();
        }
        if (!newRecruits.isEmpty()) {
            for (int i = 0; i < newRecruits.size(); i++) entities.add(newRecruits.get(i));
            newRecruits.clear();
        }
    }

    /** The head counts and the history for the graphs. */
    private void countHeads(final float dt) {
        // ((11.5) The head counts every other step in a crowd of thousands: they're only read off for the panels.)
        // ((13.5) Every fourth step with over eighteen thousand: the numbers on screen don't need it more often.)
        // ((22.3) Every eighth in peace, with nobody turning: the counts hardly change. A tenth of a quiet step.)
        if (entities.size() < 3000 || (lodFrame & (peaceful ? 7 : entities.size() > 18000 ? 3 : 1)) == 0) stats.recount();
        statTimer += dt;
        if (statTimer >= statStep) {
            statTimer = 0;
            stats.sampleStats();
        }
    }

    /** Commanders on the map this tick (their soldiers shoot better when they're close). */
    final ArrayList<Entity> commanders = new ArrayList<Entity>();

    void recountLive() {
        stats.recountLive();
    }

    /** ((12.0) Who's inside the buildings, as last counted, and when it's next counted.) */
    float insideAt = -1;
    int insideHiding, insideVisiting, insideCivilians, insideLurking, insideBitten;
    /** ((12.5) The people counted as of the last look indoors, shown until the next.) */
    int heldHumans = -1, heldCivilians;

    void recountInsideNow() {
        stats.recountInsideNow();
    }

    /** ((13.1) The bitten out and about, counted in recount. */
    int bittenOut;

    void recount() {
        stats.recount();
    }

    void say(String msg) {
        message = msg;
        messageTime = 4f;
    }

    /** ((11.5) Each one's cell, from the first pass over everyone to the second.) */
    /** ((24.0) The least of a pass of the scent searches done in a step, in tiles: a small map's all at once.) */
    static final int FIELD_MIN_WORK = 40000;

    void collectInView(float x0, float y0, float x1, float y1, ArrayList<Entity> out) {
        spatial.collectInView(x0, y0, x1, y1, out);
    }

    int followers(Entity e) {
        return spatial.followers(e);
    }

    Entity followersOf;
    float followersAt;
    int followersN;

    /** ((13.5) How far anyone may have gone since the grid was made: a few steps at a run, and a little more. */
    static final float GRID_SLACK = 10;
    int[] hashCell = new int[64];
    /** ((13.5) Each cell's count of the dead, the armed and the rest, side by side; then where the next of each goes. */
    int[] hashCnt;
    byte[] hashKind = new byte[64];
    float[] hashX = new float[64], hashY = new float[64], hashR = new float[64];
    int hashSize = -1;
    /** ((12.0) The cells with anyone in them, as of the last grid.) */
    int[] usedCells = new int[256];
    int usedN;

    /** Working space for nearest: those in range, still to be looked at. */
    float[] nearD = new float[64];
    Entity[] nearE = new Entity[64];
    int nearN;

    /** (10.31) The living in the grid cells within r of (x, y) (some a little further), into gathered[]. */
    Entity[] gathered = new Entity[256];

    int gatherLiving(float x, float y, float r) {
        return spatial.gatherLiving(x, y, r);
    }

    boolean zombieWithin(float x, float y, float radius) {
        return spatial.zombieWithin(x, y, radius);
    }

    int countZombiesNear(float x, float y, float radius) {
        return spatial.countZombiesNear(x, y, radius);
    }

    // ------------------------------------------------------------------ AI

    void think(Entity e, float dt) {
        tickClocks(e, dt);
        if (turnStep(e, dt)) return;
        if (bittenStepsAway(e)) return;
        if (e.fresh > 0) {
            e.fresh -= dt;
            if (e.fresh <= 0) {
                e.speed /= freshWalk(e);
                e.runSpeed /= freshRun(e);
            }
        }
        // ((24.0) Reloading goes on whatever else is happening: it stopped while they were dazed or stepping round a corner.)
        combat.reloadTick(e, dt);
        if (e.stun > 0) {
            e.stun -= dt;
            movement.steer(e, 0, 0, 0);
            return;
        }
        if (e == controlled) {
            e.stuckTime = 0;
            e.unstick = 0;
            playerControl.thinkControlled(e, dt);
            return;
        }
        if (unstickStep(e, dt)) return;
        if (e.leader != null && (e.leader.dead && !e.leader.removed)) e.leader = null;
        // ((12.0) Still down as crew of a car or truck that's gone - parked up and taken out again, wrecked and towed,
        // or off the map - left an officer tied to nothing, passed over for every car and patrol after.)
        if (e.rig != null && e.rig.removedFromFleet) {
            e.rig = null;
            if (e.task == Dispatch.T_BOARD) e.task = Dispatch.T_NONE;
        }
        if (dryAndCornered(e)) return;
        // ((12.3) A lift that fell through - the truck gave up waiting, or went - and they're left with nothing to
        // do: back to the job they had. Taken somewhere else on the way, they get on with that.)
        if (e.rideFrom != null && e.task != Dispatch.T_BOARD && !(e.task == Dispatch.T_RESPOND && e.incident == e.rideFrom)) {
            if (e.task == Dispatch.T_NONE) rides.afterRide(e, e.rideFrom);
            e.rideFrom = null;
        }
        // ((17.1) Going into a building, spending time in one, or getting out of one.)
        if (WALK_IN && indoors.indoorThink(e, dt)) return;
        if (boardingStep(e)) return;
        if (e.isZombie()) zombieAI.thinkZombie(e, dt);
        else if (e.isArmed()) armedAI.thinkArmed(e, dt);
        else if (e.type == Entity.DOG) dogAI.thinkDog(e, dt);
        else civilianAI.thinkCivilian(e, dt);
    }

    /** Their clocks run down, and a knock wears off. */
    private void tickClocks(final Entity e, final float dt) {
        e.hurt = Math.max(0, e.hurt - dt * 4);
        if (e.knockX != 0 || e.knockY != 0) {
            float f = Math.max(0, 1 - dt * 4);
            e.knockX *= f;
            e.knockY *= f;
            if (Math.abs(e.knockX) + Math.abs(e.knockY) < 3) e.knockX = e.knockY = 0;
        }
        e.phoneTimer -= dt;
        e.talkTimer -= dt;
        e.callCd -= dt;
        e.alertCd -= dt;
        e.cooldown -= dt;
        e.biteCd -= dt;
        e.grenadeCd -= dt;
        e.meleeCd -= dt;
        e.wanderTimer -= dt;
        e.fleeTimer -= dt;
        e.taskTimer -= dt;
        e.fear -= dt;
    }

    /** Bitten, and their time is up. True when that was their step. */
    private boolean turnStep(final Entity e, final float dt) {
        if (e.infected) {
            e.infectTimer -= dt;
            if (e.infectTimer <= 0) {
                e.hp = 0;
                e.killedByZombie = true;
                // ((14.2) A hidden bite, turning among the people sheltering in a safe zone.)
                if (e.concealed && e.zone != null && !e.zone.removed && e.task == Dispatch.T_SHELTER)
                    dispatch.say(e.zone.military ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, null, (e.zone.military ? "Military: " : "Police: ")
                            + "Someone has turned inside the " + e.zone.place + " safe zone. Their family had hidden the bite!", e.x, e.y);
                return true;
            }
        }
        return false;
    }

    /** An officer or soldier who knows they're turning says so and steps away from the others. True when that was their step. */
    private boolean bittenStepsAway(final Entity e) {
        // ((24.0) An officer or soldier who knows they're turning says so and steps away from the others, rather than
        // dying and rising in the middle of their squad.)
        if (e.infected && e.isArmed() && e != controlled && e.infectTimer < 4 && e.ride == null && e.rig == null) {
            if (!e.biteSaid) {
                e.biteSaid = true;
                dispatch.chatter(e.type == Entity.SOLDIER ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, e,
                        "I'm bitten. Keep away from me - do what you have to.", e.x, e.y);
            }
            e.task = Dispatch.T_NONE;
            e.aiming = false;
            float ax = 0, ay = 0;
            for (int i = 0, n = spatial.gatherArmed(e.x, e.y, 70); i < n; i++) {
                Entity o = gathered[i];
                if (o == e || o.dead) continue;
                float ox = e.x - o.x, oy = e.y - o.y, od = (float) Math.sqrt(ox * ox + oy * oy) + 0.001f;
                ax += ox / od;
                ay += oy / od;
            }
            float al = (float) Math.sqrt(ax * ax + ay * ay);
            if (al > 0.01f) wilds.steerClear(e, ax / al, ay / al, e.speed);
            else movement.steer(e, 0, 0, 0);
            return true;
        }
        return false;
    }

    /** Stuck on a corner or in a doorway: stepping aside by a way that's open. True when that was their step. */
    private boolean unstickStep(final Entity e, final float dt) {
        // Stuck on a corner or in a doorway (big brutes especially): step aside and try another way.
        if (e.unstick > 0) {
            e.unstick -= dt;
            movement.steer(e, (float) Math.cos(e.unstickAngle), (float) Math.sin(e.unstickAngle), e.speed);
            return true;
        }
        if (e.stuckTime > 1.2f) {
            e.stuckTime = 0;
            float heading = fastAtan2(e.my, e.mx);
            e.unstickAngle = heading + (rnd.nextBoolean() ? 1 : -1) * (1.6f + rnd.nextFloat() * 1f);
            // ((14.9) Off a way that's actually open: a random one could be straight back into the corner of the
            // nook where two buildings meet, and they stood there pushing at the wall time after time.)
            float reach = e.radius + 8;
            if (city.circleBlocked(e.x + (float) Math.cos(e.unstickAngle) * reach, e.y + (float) Math.sin(e.unstickAngle) * reach, e.radius)) {
                float bestA = e.unstickAngle, best = -9;
                for (int k = 0; k < 12; k++) {
                    float a = heading + k * TAU / 12;
                    float ux = (float) Math.cos(a), uy = (float) Math.sin(a);
                    if (city.circleBlocked(e.x + ux * reach, e.y + uy * reach, e.radius)) continue;
                    // (Sideways round it before back the way they came.)
                    float score = 1 - Math.abs(Math.abs(((a - heading) % TAU + TAU) % TAU - (float) Math.PI) - (float) Math.PI / 2) / (float) Math.PI + rnd.nextFloat() * 0.2f;
                    if (score > best) {
                        best = score;
                        bestA = a;
                    }
                }
                e.unstickAngle = bestA;
            }
            e.unstick = 0.6f + rnd.nextFloat() * 0.6f;
            e.blocked = false;
        }
        return false;
    }

    /** Out of rounds with one of the dead close: away from it first (or to a partner with a magazine to spare). True when that was their step. */
    private boolean dryAndCornered(final Entity e) {
        // ((12.9) Out of rounds, with nothing else to fight with, and one of the dead close: away from it first, then on
        // with whatever they were doing. They used to walk straight past (or into) it on the way to a ride, a partner
        // with a spare magazine or the armoury.)
        if (e != controlled && !e.isZombie() && (e.isArmed() || e.hasGun) && e.melee <= 0 && e.ammo <= 0 && e.reserve <= 0
                && e.reload <= 0) {
            Entity z = spatial.nearest(e, DRY_KEEP, true, false);
            // ((13.3) A partner close by with rounds to spare: to them, round the dead (that's below), rather than
            // only ever backing off from the dead and never getting a magazine.)
            if (z != null && e.isArmed() && e.task != Dispatch.T_BORROW && e.task != Dispatch.T_BOARD) {
                Entity mate = ammo.lenderFor(e, 160);
                if (mate != null) {
                    dispatch.releaseForResupply(e);
                    e.task = Dispatch.T_BORROW;
                    e.protector = mate;
                    e.taskTimer = 15;
                }
            }
            if (e.task == Dispatch.T_BORROW && e.protector != null && !e.protector.dead) z = null;
            boolean jumpIn = z != null && e.task == Dispatch.T_BOARD && e.rig != null
                    && World.hypot(e.rig.x - e.x, e.rig.y - e.y) < 16 && World.hypot(z.x - e.x, z.y - e.y) > 10;
            if (z != null && !jumpIn) {
                // ((14.2) Out of rounds with the dead on them: calling for help as they back off.)
                if (e.isArmed()) armedAI.maybeCallBackup(e);
                float ax = e.x - z.x, ay = e.y - z.y, ad = (float) Math.sqrt(ax * ax + ay * ay) + 0.001f;
                e.aiming = false;
                e.threatX = z.x;
                e.threatY = z.y;
                // (Straight away from it: the map of where the dead are is a moment old, and with one this close it
                // could send them the wrong way. Round a wall only when one is in the way.)
                if (e.blocked) movement.flee(e, ax / ad, ay / ad, e.runSpeed);
                else movement.steer(e, ax / ad, ay / ad, e.runSpeed);
                return true;
            }
        }
        return false;
    }

    /** On the way to a vehicle - unless one of the dead needs dealing with first. True when that was their step. */
    private boolean boardingStep(final Entity e) {
        if (e.task == Dispatch.T_BOARD) {
            // (10.37) Police and soldiers on their way to a vehicle stop to deal with one of the dead that gets
            // close, instead of walking on with it biting them.
            boolean defend = e.isArmed() && (e.ammo > 0 || e.reserve > 0) && spatial.nearest(e, 45, true, false) != null;
            // ((21.2) Nor do they walk on past one of the dead with its teeth in someone.)
            if (!defend && TACTICS && e.isArmed() && (e.ammo > 0 || e.reserve > 0)) {
                Entity z = spatial.nearest(e, 70, true, true);
                defend = z != null && z.biteCd > 0;
            }
            if (!defend && rides.boardStep(e)) return true;
        }
        return false;
    }

    /** ((18.0) The campaign mission being played, or null in a sandbox game. */
    Campaign.Mission mission() {
        return Campaign.get(city.cfg.mission);
    }

    /** ((18.0) A mission's running record: when it started, what's been spent, and how it ended (1 won, 2 failed). */
    float missionStart;
    int spent, missionResult, missionStars;
    String missionWhy;

    int gatherArmed(float x, float y, float r) {
        return spatial.gatherArmed(x, y, r);
    }

    // ------------------------------------------------------------------ (18.4) police drones

    // ---- ((20.0) The director, the difficulty, flares and mines ----

    final Director director;
    /** Easy, Normal, Hard or Nightmare (Director.NAMES). */
    int difficulty;
    /** How quick the dead are for the difficulty. */
    float zPace;

    /**
     * ((21.1) Only a campaign mission has a difficulty - its own. A sandbox game is always Normal, whatever it was
     * saved as: nothing about it is harder or easier than what the player puts in it.
     */
    void applyRules() {
        Campaign.Mission m = mission();
        difficulty = m != null ? Math.max(0, Math.min(3, m.difficulty)) : Director.NORMAL;
        zPace = Director.Z_SPEED[difficulty];
        director.level = difficulty;
        city.cfg.difficulty = difficulty;
    }
    /** Flares burning: x, y, seconds left. Mines laid: x, y, seconds until armed. */
    final ArrayList<float[]> flares = new ArrayList<float[]>();
    final ArrayList<float[]> mines = new ArrayList<float[]>();
    int flaresFired, minesBlown;
    float toolTick;
    static final float FLARE_LIFE = 45, FLARE_LURE = 330, MINE_TRIGGER = 9, MINE_ARM = 3;

    void flare(float x, float y) {
        fieldworks.flare(x, y);
    }

    int layMines(float x, float y) {
        return fieldworks.layMines(x, y);
    }

    int score() {
        return stats.score();
    }

    // ---- ((21.2) Tactics: reading the fight ----

    static boolean TACTICS = true;
    /** How far round someone looks to read a fight. */
    static final float SENSE_R = 110;
    static final int DIRS = 16;
    static final float[] DIR_X = new float[DIRS], DIR_Y = new float[DIRS];
    static {
        for (int k = 0; k < DIRS; k++) {
            DIR_X[k] = (float) Math.cos(k * Math.PI * 2 / DIRS);
            DIR_Y[k] = (float) Math.sin(k * Math.PI * 2 / DIRS);
        }
    }
    final float[] cone = new float[DIRS];
    int breakouts;
    float breakSaidAt = -99;

    void sense(Entity e) {
        armedAI.sense(e);
    }

    float reloadTime(Entity e) {
        return combat.reloadTime(e);
    }

    // ---- ((21.0) Defences, alphas, the barrage and the mop-up ----

    final Defences defences = new Defences();
    /** The stars a commander had going into this mission: what perks they have (Perks). */
    int perkStars;
    /** ((22.0) Retake the district: which one (City.districts), and how long it's been clear of the dead. */
    int retakeDistrict = -1;
    float retakeClearT;
    /** How big a horde has to be to have an alpha at its head; how many of the dead left counts as mopping up. */
    static final int ALPHA_HORDE = 12, MOP_UP = 12;
    int alphas, alphasKilled, barrages;
    /** ((21.0) A mission's site to hold: how long it's been in the hands of the dead (Campaign.G_DEFEND). */
    float siteT;
    /** The last of the dead, once there are few enough to hunt: x, y pairs (for the screen's pointers). */
    float[] lastDead = new float[0];
    boolean mopping;
    float mopCd;
    final ArrayList<Entity> sweepers = new ArrayList<Entity>();

    float[] layWire(float x, float y) {
        return fieldworks.layWire(x, y);
    }

    void wireGone(float x, float y) {
        fieldworks.wireGone(x, y);
    }

    ArrayList<Entity> mgNest(float x, float y) {
        return fieldworks.mgNest(x, y);
    }

    void barrage(float x, float y) {
        fieldworks.barrage(x, y);
    }

    /** ((27.0) How long after it's called a command post's fire mission starts landing, and how long the shells take. */
    static final float FIRE_DELAY = 3, FIRE_LASTS = 6.5f;
    /** ((27.0) Fire missions called by the command posts so far. */
    int fireMissions;

    float[] fireMission(float px, float py, float reach, String caller, int who) {
        return fieldworks.fireMission(px, py, reach, caller, who);
    }

    // ------------------------------------------------------------------ ((27.0) rooftop overwatch

    /** Those up on a roof now (for the picture). */
    final ArrayList<Entity> overwatch = new ArrayList<Entity>();
    /** How near a call a roof has to be, and how tall the building (three floors). */
    static final float ROOF_FIND = 150, ROOF_HEIGHT = City.FLOOR * 3 - 0.5f;
    int roofsTaken;

    void comeDown(Entity e) {
        rooftops.comeDown(e);
    }

    ArrayList<Entity> airborne(float x, float y) {
        return fieldworks.airborne(x, y);
    }

    float wireCd;
    final java.util.HashSet<Dispatch.SafeZone> wireSaid = new java.util.HashSet<Dispatch.SafeZone>();
    static final int ZONE_WIRE = 10;

    void zonePosts(Dispatch.SafeZone z) {
        containment.zonePosts(z);
    }

    float fortifyCd;
    int sandbagsLaid;

    /** ((21.0) Hordes just come in from outside: {the one at its head, seconds left to point it out, how many}. */
    final ArrayList<Object[]> incoming = new ArrayList<Object[]>();
    float unstickCd;
    int unstuck, flushed;

    /** The police's drones: {x, y, where it's watching x, y, seconds left, seconds to its next report}. */
    final ArrayList<float[]> policeDrones = new ArrayList<float[]>();
    /** How far a drone sees, and how long one stays up. */
    static final float WATCH_SEE = 200, WATCH_LIFE = 150;

    float[] launchDrone(float x, float y) {
        return fieldworks.launchDrone(x, y);
    }

    boolean droneOver(float x, float y) {
        return fieldworks.droneOver(x, y);
    }

    /** Police and soldiers killed. (The wounded of 17.5 - going down, bleeding out, being saved - went in 20.0.) */
    int copsLost, soldiersLost;

    // ------------------------------------------------------------------ (17.6) cover and suppression

    /** Gunfights use cover: walls, buildings, parked cars and trees between a shooter and the one shooting at them. */
    static final boolean COVER = true;

    boolean inCover(Entity e, float fx, float fy) {
        return armedAI.inCover(e, fx, fy);
    }

    final Entity coverProbe = new Entity();

    /** Makes zombies within a radius head for a noise (gunfire, explosions, a screamer). */
    /** (10.59) Hordes set off by a noise this round (see noise and surge). */
    final ArrayList<Entity> surging = new ArrayList<Entity>();
    float surgeSaid;

    void engineLure(float x, float y, float radius) {
        zombieAI.engineLure(x, y, radius);
    }

    /** ((26.0) How much further gunfire and blasts carry to the dead than they did. */
    static final float BATTLE_CARRY = 1.25f;
    /**
     * ((26.0) The sound of fighting, in squares this size: how loud it's been lately (dying away over a minute or so)
     * and where in the square, on average, it came from. The dead with nothing to do within earshot of a square that's
     * been loud for a while go to it (see battleDraw).
     */
    static final float BATTLE_CELL = 200;
    /** ((26.0) How loud a square must have been to draw a lone zombie, and a horde. */
    static final float BATTLE_DRAW = 6, BATTLE_DRAW_HORDE = 4;
    float[] battleHeat, battleX, battleY;
    int battleW;
    float battleMax, battleSaid = -999, battleDrawX, battleDrawY;
    int battleDrawn;

    void battleNoise(float x, float y, float radius) {
        noise.battleNoise(x, y, radius);
    }

    void noise(float x, float y, float radius) {
        noise.noise(x, y, radius);
    }

    /** (10.45) How far one of this person's shots is heard: louder guns carry further. */
    static float gunLoudness(Entity e) {
        if (e.role == Entity.ROLE_GUNNER) return 330;
        if (e.role == Entity.ROLE_SNIPER) return 340;
        // ((26.0) A suppressed rifle: heard only close by.)
        if (e.role == Entity.ROLE_SF && e.weapon == Entity.W_STD) return 90;
        switch (e.weapon) {
            case Entity.W_PISTOL:
            case Entity.W_REVOLVER: return 180;
            case Entity.W_SHOTGUN: return 245;
            case Entity.W_RIFLE: return 265;
            case Entity.W_HUNTING: return 285;
            case Entity.W_SMG: return 200;
            case Entity.W_CARBINE: return 240;
            case Entity.W_DMR: return 290;
            case Entity.W_CROSSBOW: return 30;
            default: return e.type == Entity.SOLDIER ? 255 : e.type == Entity.COP ? 205 : 190;
        }
    }

    /** How much better than the standard this one shoots: 0 to 0.15, fixed for the person (from their name). */
    /**
     * (10.59) Veteran ranks: kills earned in the fighting make a better shot and a steadier hand. 0 none, 1 blooded
     * (5 kills), 2 seasoned (15), 3 veteran (35), 4 elite (70).
     */
    static final int[] RANK_KILLS = {0, 5, 15, 35, 70};
    static final String[] RANK_NAMES = {"", "Blooded", "Seasoned", "Veteran", "Elite"};

    static int rank(Entity e) {
        int r = 0;
        while (r + 1 < RANK_KILLS.length && e.kills >= RANK_KILLS[r + 1]) r++;
        return r;
    }

    // ------------------------------------------------------------------ ((23.0) experience, levels and perks

    /** The experience each level starts at: level 1 at nothing, level 7 at 700. */
    static final int[] LEVEL_XP = {0, 30, 80, 160, 280, 450, 700};
    static final int P_STEADY = 1, P_QUICK = 2, P_TOUGH = 4, P_FLEET = 8, P_MEDIC = 16, P_NERVE = 32;
    static final String[] PERK_NAMES = {"Steady aim", "Quick hands", "Tough", "Fleet-footed", "Field medic", "Nerves of steel"};
    /** The order each kind of unit takes its perks in, from level 2. */
    static final int[] PERKS_SOLDIER = {P_STEADY, P_TOUGH, P_QUICK, P_NERVE, P_FLEET, P_MEDIC};
    static final int[] PERKS_COP = {P_STEADY, P_QUICK, P_NERVE, P_TOUGH, P_MEDIC, P_FLEET};
    static final int[] PERKS_CIVILIAN = {P_FLEET, P_NERVE, P_TOUGH, P_STEADY, P_QUICK, P_MEDIC};
    static final int[] PERKS_MEDIC = {P_MEDIC, P_TOUGH, P_NERVE, P_FLEET, P_STEADY, P_QUICK};

    static boolean perk(Entity e, int p) {
        return (e.perks & p) != 0;
    }

    void gainXp(Entity e, float amount) {
        experience.gainXp(e, amount);
    }

    static String perkName(int p) {
        for (int k = 0; k < PERK_NAMES.length; k++) if (p == 1 << k) return PERK_NAMES[k];
        return "";
    }

    /** Their perks by name, or "" if none yet. */
    static String perkList(Entity e) {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < PERK_NAMES.length; k++)
            if ((e.perks & (1 << k)) != 0) sb.append(sb.length() > 0 ? ", " : "").append(PERK_NAMES[k]);
        return sb.toString();
    }

    /** ((23.0) The three living with the most experience (looked at every couple of seconds). */
    final Entity[] topUnits = new Entity[3];
    float topAt = -10;

    Entity[] topUnits() {
        return experience.topUnits();
    }

    /** ((23.0) People taken under someone's wing (survivor bands and escorts), this game. */
    int bandsFormed;

    static float aimBonus(Entity e) {
        int h = e.nameSeed * 0x9E3779B1;
        h ^= h >>> 15;
        // ((26.0) Hunters and ex-soldiers shoot like they've done it before.)
        int tr = e.trade();
        return ((h & 0x7FFFFFFF) % 1000) / 1000f * 0.15f + (tr == Entity.TR_HUNTER || tr == Entity.TR_VETERAN ? 0.2f : 0) + (e.hero() ? 0.1f : 0)
                // ((27.5) Volunteers: a few days' worth of training at most.)
                - (e.role == Entity.ROLE_VOLUNTEER ? 0.12f : 0);
    }

    void heardShot(Entity shooter, Entity target, float radius) {
        noise.heardShot(shooter, target, radius);
    }

    /**
     * (12.1) How fast one of the dead goes after someone it has spotted: at a run once it's close, quicker than its
     * walk before that. ((12.6) An ordinary zombie only breaks into its run in the last few steps (50, not 110) and
     * only a little quicker than its shamble before: they were too fast. Runners and brutes as before.)
     */
    /** (12.7) How much faster a runner is than an ordinary zombie, at every pace. */
    static final float RUNNER_PACE = 1.5f;

    static float chaseSpeed(Entity z, float d) {
        // ((12.7) Runners chase the same way, only faster: half as fast again at every step.)
        boolean shambler = z.type == Entity.ZOMBIE || z.type == Entity.RUNNER;
        // ((23.0) Three or more closing on the same one: a frenzy, a little quicker.)
        return (d < (shambler ? 50 : 110) ? z.runSpeed : z.speed * (shambler ? 1.1f : 1.3f)) * (z.frenzy ? 1.12f : 1);
    }

    /** How much quicker the freshly turned are for a while: ((12.6) a shambler only a little). */
    static float freshWalk(Entity z) {
        return z.type == Entity.ZOMBIE || z.type == Entity.RUNNER ? 1.08f : 1.2f;
    }

    static float freshRun(Entity z) {
        return z.type == Entity.ZOMBIE || z.type == Entity.RUNNER ? 1.12f : 1.3f;
    }

    int tallyAt = -1;

    // ------------------------------------------------------------------ wildlife

    /** Deer and foxes out in the country: they bolt when the dead come, and foxes can be infected. */
    static final class Animal {
        float x, y, vx, vy, angle, timer, flee;
        int kind;
        boolean dead;
    }

    final ArrayList<Animal> animals = new ArrayList<Animal>();
    float wildlifeSaidCd;

    void spawnWildlife() {
        wilds.spawnWildlife();
    }

    static final int TRAIL = 16;

    float hordeLaneSaid, hordeHeatSaid;
    /** (10.32) The noise of fighting, in big squares of the map: it dies away over a minute or two. */
    float[] fightHeat;
    /** ((17.5) All the fighting this game, square by square (fightHeat without the fading). */
    float[] battleMap;
    static final float HEAT_CELL = 400;

    int heatW() {
        return noise.heatW();
    }

    float[] loudestFight(float x, float y, float reach) {
        return noise.loudestFight(x, y, reach);
    }

    float hordeDriftSaid;

    static final int CAR_CELL = 128;
    float carGridTime = -1;
    int[] carHead, carNext = new int[0];
    Fleet.Vehicle[] carList = new Fleet.Vehicle[0];
    int carCount;

    /** A car worth going after: one driving by, or one with people in it. */
    /** ((26.0) Below this speed the dead on a vehicle claw at it (see clawAt); faster, they're shaken off. */
    static final float CLAW_SPEED = 15;
    /** ((26.0) Below this they get a window in and drag someone out, after this much clawing (zombie-seconds, up to four at once). */
    static final float BREAK_SPEED = 8, BREAK_IN = 7;
    float dragSaid = -999, clawSaid = -999;

    void clawAt(Fleet.Vehicle v, float dt) {
        zombieAI.clawAt(v, dt);
    }

    /** ((26.0) How many the dead have dragged out of vehicles this game. */
    int dragged;

    void shove(Entity e, Entity z, boolean strong) {
        zombieAI.shove(e, z, strong);
    }

    // ------------------------------------------------------------------ taking control

    /** The one the player is controlling (their AI is off), or null; and the stick and attack button. */
    Entity controlled;
    float joyX, joyY;
    boolean ctrlAttack;

    // ------------------------------------------------------------------ weapons

    static int magFor(int kind) {
        switch (kind) {
            case Entity.W_SHOTGUN: return 6;
            case Entity.W_RIFLE: case Entity.W_SMG: case Entity.W_CARBINE: return 30;
            case Entity.W_HUNTING: return 5;
            case Entity.W_REVOLVER: return 6;
            case Entity.W_DMR: return 20;
            case Entity.W_CROSSBOW: return 1;
            default: return 12;
        }
    }

    /** (11.4) The rounds carried for a gun beyond the one in it. */
    static int spareFor(int kind) {
        switch (kind) {
            case Entity.W_SHOTGUN: return 30;
            case Entity.W_SMG: case Entity.W_CARBINE: return 120;
            case Entity.W_DMR: return 80;
            case Entity.W_HUNTING: return 25;
            case Entity.W_CROSSBOW: return 20;
            default: return -1;
        }
    }

    void issue(Entity e, int kind) {
        combat.issue(e, kind);
    }

    void setGun(Entity e, int kind, int rounds) {
        combat.setGun(e, kind, rounds);
    }

    /** (14.8) The swing being made is a civilian's, fighting back with what was to hand (see brawlStep). */
    boolean wildSwing;

    void stockArmouries() {
        population.stockArmouries();
    }

    // ------------------------------------------------------------------ the player's options

    /** Where the player's character is when they aren't on foot. */
    Fleet.Vehicle controlledCar;
    static final int CA_ENTER_CAR = 0, CA_EXIT_CAR = 1, CA_ENTER = 2, CA_EXIT = 3, CA_BARRICADE = 4, CA_SEARCH = 5,
            CA_RALLY = 6, CA_GRENADE = 7, CA_SQUAD = 8;
    static final String[] CA_NAMES = {"Get in", "Get out", "Go inside", "Go out", "Barricade", "Search", "Rally", "Grenade", "Squad"};
    /** ((14.7) What the one you're playing is shooting at, for their squad to shoot at too. */
    Entity ctrlTarget;
    /** (14.7) The most a squad you lead can have, and how far round you they're gathered from. */
    static final int SQUAD_MAX = 6;
    static final float SQUAD_REACH = 180;

    int ctrlOptions(int[] out) {
        return playerControl.ctrlOptions(out);
    }

    boolean canDrive(Fleet.Vehicle v) {
        return playerControl.canDrive(v);
    }

    /** ((27.5) Control ending (or passing to someone else) while the player is at the wheel: the vehicle goes back to the game. */
    void letGoOfCar() {
        if (controlledCar != null && controlled != null && controlledCar.player == controlled) fleet.handBack(controlledCar, controlled);
        controlledCar = null;
    }

    void driveInto(Entity e, Fleet.Vehicle v) {
        playerControl.driveInto(e, v);
    }

    void ctrlAction(int a) {
        playerControl.ctrlAction(a);
    }

    // ------------------------------------------------------------------ outbreak replay

    /** Snapshots every few seconds: {time, zombies, people, sampled zombies, sampled people} and dots as x,y pairs. */
    final ArrayList<float[]> replayMeta = new ArrayList<float[]>();
    final ArrayList<short[]> replayDots = new ArrayList<short[]>();
    /** Where and when people turned: {time, x, y}. */
    final ArrayList<float[]> turnEvents = new ArrayList<float[]>();
    float replayTimer, replayStep = 4;
    static final int REPLAY_SAMPLE = 250, REPLAY_FRAMES = 900;

    int sheltered;
    boolean k9Spawning;

    // ------------------------------------------------------------------ (10.37) the civilian's brain

    static final int G_ROUTINE = 0, G_FLEE = 1, G_FIGHT = 2, G_HIDE = 3, G_ZONE = 4, G_SHELTER = 5,
            G_RESTOCK = 7, G_PICKUP = 8, G_HEAL = 9, G_EVAC = 10, G_FOLLOW = 11, G_PATROL = 12, G_SUPPLY = 13, G_LEAVE = 14, G_COUNT = 15;
    final float[] goalScore = new float[G_COUNT];

    /** How brave someone is (0 to 1), from who they are: the same person is always the same. */
    static float bravery(Entity e) {
        int hsh = e.nameSeed * 0x9E3779B1;
        return ((hsh >>> 8) & 0xFF) / 255f;
    }

    /** How cautious (0 to 1): the careful head for cover sooner, and stay longer. */
    static float caution(Entity e) {
        int hsh = (e.nameSeed + 77) * 0x85EBCA6B;
        return ((hsh >>> 8) & 0xFF) / 255f;
    }

    /** (14.8) How brave someone must be to take on the dead with what's to hand all by themselves. */
    static final float BRAWL_BRAVE = 0.8f;

    /** (14.8) How often civilians have taken up something to fight with, and the dead they've put down with it. */
    int civBrawls, civBrawlKills;

    /** Once a second: word of the outbreak gets round. */
    /** (11.7) Whether the city has learned that a bite spreads it. */
    boolean biteKnown;

    /**
     * ((17.1) People walk into buildings and out again on their own feet - through the door, from room to room - and
     * are there inside, to be seen and to be found, the whole time. ((16.14) Nobody vanishes into one any more.)
     */
    static final boolean WALK_IN = true;

    /** Buildings someone was in at the last count. */
    final ArrayList<City.Building> occupied = new ArrayList<City.Building>();

    void countInside() {
        indoors.countInside();
    }

    /** Starts a stay inside: on an errand, at work or home (hide false), or hiding from the dead (hide true). */
    /** (17.1) How many fit in a building at once, on its one floor: a person for every so much floor. */
    static int room(City.Building b) {
        return Math.max(4, (int) ((b.x1 - b.x0) * (b.y1 - b.y0) / 280));
    }

    void beginStay(Entity e, City.Building b, float stay, boolean hide) {
        indoors.beginStay(e, b, stay, hide);
    }

    boolean indoorMove(Entity e, float tx, float ty, float speed) {
        return indoors.indoorMove(e, tx, ty, speed);
    }

    void emptyAllBuildings() {
        indoors.emptyAllBuildings();
    }

    /** Everyone out on errands inside buildings right now. */
    int visiting;

    // ------------------------------------------------------------------ everyday life

    /** Places people work: offices, warehouses, the hospital, schools, the mall and supermarkets. */
    ArrayList<City.Building> workplaces;

    /** Gives someone a life: a job and somewhere to do it, or a way of spending the day. */
    /** Postal workers and street vendors handed out so far. */
    final int[] jobsGiven = new int[2];

    void assignJob(Entity e) {
        dailyLife.assignJob(e);
    }

    /** ((22.0) The buildings of each kind, kept from one look to the next: going through every building in the city for
     * each of thirty thousand residents was a fifth of making a new city.) */
    final java.util.HashMap<Integer, ArrayList<City.Building>> ofKind = new java.util.HashMap<Integer, ArrayList<City.Building>>();
    int ofKindFor = -1;

    /**
     * Walking routes to the parks and plazas (each one a whole map of distances), the most recently used
     * kept up to a memory budget: on the biggest maps they used to pile up until the phone ran out of memory.
     * One that's dropped is worked out again when it's next needed. ((10.55) Kept at 16 bits, so twice as many
     * fit: with too few kept, people setting out on their walks had them worked out again and again, and the
     * big maps lagged with hardly anything happening.)
     */
    final java.util.LinkedHashMap<float[], City.WalkField> fieldCache = new java.util.LinkedHashMap<float[], City.WalkField>(32, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<float[], City.WalkField> eldest) {
            return size() > dailyLife.fieldBudget();
        }
    };

    final java.util.HashMap<float[], float[]> areaPoints = new java.util.HashMap<float[], float[]>();

    float[] areaPoint(float[] a) {
        return dailyLife.areaPoint(a);
    }

    boolean onTowel(Entity e) {
        return dailyLife.onTowel(e);
    }

    boolean atStall(Entity e) {
        return dailyLife.atStall(e);
    }

    String jobTitle(Entity e) {
        return dailyLife.jobTitle(e);
    }

    static String placeLabel(City.Building b) {
        if (b.name != null) return b.name;
        switch (b.kind) {
            case City.SHOP: return "a shop";
            case City.PHARMACY: return "a pharmacy";
            case City.KIOSK: return "a gas station";
            case City.WAREHOUSE: return "a warehouse";
            case City.HOUSE: return "a house";
            case City.APARTMENT: return "an apartment block";
            default: return "an office";
        }
    }

    String lifeActivity(Entity e) {
        return dailyLife.lifeActivity(e);
    }

    int[] insideCounts(City.Building b) {
        return dailyLife.insideCounts(b);
    }

    /** Places people go on errands: the named landmarks and a handful of shops (so their routes stay cached). */
    ArrayList<City.Building> errandSpots;

    // ------------------------------------------------------------------ medkits

    /** Medkits {x, y, uses}: anyone hurt nearby is patched up. */
    final ArrayList<float[]> medkits = new ArrayList<float[]>();

    float[] placeMedkit(float x, float y) {
        return sandbox.placeMedkit(x, y);
    }

    // ---- ((25.0) The giant, and the plane crash, the prison break and the riot ----

    /** ((25.0) The giant: its health, how far past its reach it smashes barricades, and how fast. */
    static final float GIANT_HP = 1200, GIANT_REACH = 10, GIANT_SMASH = 240;

    float giantSaidAt = -99;
    int giantsPlaced, giantsKilled;

    void giantPlaced(Entity z) {
        events.giantPlaced(z);
    }

    /** ((25.0) The airstrike's cooldown: game time it's next ready. */
    float airstrikeReady;
    static final float AIRSTRIKE_COOLDOWN = 90;

    int airstrikeWait() {
        return events.airstrikeWait();
    }

    boolean freeAirstrike(float x, float y) {
        return events.freeAirstrike(x, y);
    }

    /** ((25.0) Where planes came down: x, y, heading, seconds since. */
    final ArrayList<float[]> crashes = new ArrayList<float[]>();
    static final float CRASH_BURN = 75, CRASH_SMOKE = 200;
    int planeCrashes;

    boolean planeCrash(float x, float y) {
        return events.planeCrash(x, y);
    }

    void planeCrashed(float x, float y, float a) {
        events.planeCrashed(x, y, a);
    }

    /** ((25.0) The prison break: every inmate out of the gate at once and scattering. False if there's no prison, or it's already open. */
    int escaped, recaptured;

    boolean prisonBreak() {
        return events.prisonBreak();
    }

    /** ((25.0) The riot: x, y, when it ends; the cars burnt, the shops looted, those held; the riot squad sent. */
    float[] riot;
    int riotCars, riotLoots, riotHeld;
    final ArrayList<Entity> riotSquad = new ArrayList<Entity>();
    int riots;
    static final float RIOT_TIME = 120;

    boolean riot(float x, float y) {
        return events.riot(x, y);
    }

    float eventsTick;

    void airstrike(float x, float y) {
        events.airstrike(x, y);
    }

    void cityAlarm() {
        events.cityAlarm();
    }

    /** Seconds the siren is still wailing (GameView keeps playing it). */
    float alarmTime;

    Entity infectAt(float x, float y) {
        return death.infectAt(x, y);
    }

    /** The first person (or zombie) of the outbreak, and where it started. */
    Entity patientZero;
    String outbreakPlace;

    void markPatientZero(Entity e) {
        death.markPatientZero(e);
    }

    // ------------------------------------------------------------------ the infection's family tree

    /** Who bit whom (by person), how many each one has turned, and patient zero. */
    final java.util.HashMap<Integer, Integer> bitBy = new java.util.HashMap<Integer, Integer>();
    final java.util.HashMap<Integer, Integer> victims = new java.util.HashMap<Integer, Integer>();
    int patientZeroSeed;

    int[] chainFromPatientZero() {
        return death.chainFromPatientZero();
    }

    // ------------------------------------------------------------------ highlights and noise

    /** Big moments, for the replay: {time, x, y} and what happened. */
    final ArrayList<float[]> highlightAt = new ArrayList<float[]>();
    final ArrayList<String> highlightText = new ArrayList<String>();

    void highlight(String text, float x, float y) {
        stats.highlight(text, x, y);
    }

    /** Sounds spreading out, so you can see what the dead are hearing: {x, y, radius, age}. */
    final ArrayList<float[]> noiseRings = new ArrayList<float[]>();

    // ------------------------------------------------------------------ sandbox tools

    /** Puts a barricade on the tile at (x, y). Returns it, or null if the tile can't take one. */
    /** ((22.0) What each kind of barricade can take: wooden planks, sandbags, a concrete barrier. */
    static final float[] BARRICADE_HP = {220, Defences.SANDBAG_HP, 1200};

    float[] placeBarricade(float x, float y, int tier) {
        return sandbox.placeBarricade(x, y, tier);
    }

    /** ((22.0) Makes a barricade one of the tougher kinds (at full strength for that kind). */
    static void setTier(float[] b, int tier) {
        b[5] = tier;
        b[2] = Math.max(b[2], BARRICADE_HP[Math.max(0, Math.min(2, tier))]);
    }

    float[] placeBarricade(float x, float y) {
        return sandbox.placeBarricade(x, y);
    }

    void removeBarricade(float[] b) {
        sandbox.removeBarricade(b);
    }

    World.Pickup placeCrate(float x, float y, float drop) {
        return sandbox.placeCrate(x, y, drop);
    }

    void supplyDrop(float x, float y) {
        sandbox.supplyDrop(x, y);
    }

    ArrayList<Entity> hordeEvent(float x, float y) {
        return sandbox.hordeEvent(x, y);
    }

    ArrayList<Entity> megaHorde(float x, float y) {
        return sandbox.megaHorde(x, y);
    }

    int megaHordes;

    ArrayList<Entity> hordeFrom(float x, float y, int count, boolean announce) {
        return sandbox.hordeFrom(x, y, count, announce);
    }

    void panic(float x, float y) {
        sandbox.panic(x, y);
    }

    boolean indoorOutbreakAt(float x, float y) {
        return sandbox.indoorOutbreakAt(x, y);
    }

    void clearZombies() {
        sandbox.clearZombies();
    }

    void clearBodies() {
        sandbox.clearBodies();
    }

    void clearWrecks() {
        sandbox.clearWrecks();
    }

    void clearBarricades() {
        sandbox.clearBarricades();
    }

    // ------------------------------------------------------------------ outbreaks, evolution and recovery

    void indoorOutbreak(City.Building b, int infected) {
        death.indoorOutbreak(b, infected);
    }

    // ------------------------------------------------------------------ boats

    /** Boats out on the water, just for looks: {x, y, dx, dy, speed, kind (0 rowing, 1 motor, 2 yacht, 3 tug)}. */
    final ArrayList<float[]> boats = new ArrayList<float[]>();

    void launchBoats() {
        exodus.launchBoats();
    }

    // ------------------------------------------------------------------ the government

    /** Government buildings overrun (each comes back once it's cleared and people are there again). */
    boolean hallLost, callsLost, worksLost, armoryLost;
    /** Inmates still locked up in the jail, and whether they've broken out. */
    int inmates;
    boolean jailBroken;
    /** Crews from the works depot have boarded up this many shelters. */
    int boardedUp;
    /** The courthouse's armoury, where police can restock (not one of the city's facilities). */
    City.Facility courtArmoury;
    float jailThreat, noGuards;

    boolean broadcasting() {
        return war.broadcasting();
    }

    City.Building powerStation;
    boolean powerChecked;

    /** Once a second: the infection evolves, and after it's over the city slowly recovers. */
    // ------------------------------------------------------------------ ((23.0) the cargo plane

    /** The cargo plane overhead: {x, y, heading x, heading y, crates left, seconds to the next}, or null. */
    float[] cargoPlane;
    float[] cargoTarget;
    float cargoCd = 150;

    // ------------------------------------------------------------------ resupply after the outbreak (10.19)

    /** Supply trucks sent in since it went quiet (and whether the radio has said so). */
    int restockSent;
    boolean restockSaid;
    static final int RESTOCK_TRUCKS = 14;

    void restocked(Fleet.Vehicle v) {
        aftermathLife.restocked(v);
    }

    // ------------------------------------------------------------------ recruitment

    /** (11.0) The city has asked for reserve pilots (once, when the outbreak is on the news). */
    boolean pilotsAsked;

    /** (14.9) How long an officer or soldier trains on the pad before they can fly its helicopter. */
    static final float PILOT_TRAINING = 40;

    Entity callReservePilot(Fleet.Vehicle v) {
        return recruiting.callReservePilot(v);
    }

    /** An armed civilian with no bullets left walks to the nearest supermarket that still has some. */
    /** People who armed themselves at a store. */
    int armedAtStores;

    /** Dogs stay by their owner, bark at zombies and go for them if their owner is in danger. */
    float shareSaid;
    int carsChained;
    int fallbacks;
    int magsShared;

    float sniffSaid;

    int k9Finds;

    final ArrayList<City.Building> doorPaths = new ArrayList<City.Building>();
    /** How many new door routes may be worked out right now (each is a search over the whole map). */
    float pathBudget = 3;

    /**
     * Routes to the doors of buildings under siege, shared by all the dead outside. Each covers just the
     * streets round the building (the dead only besiege a building they're close to), so they're cheap.
     */
    static final int DOOR_R = 24, DOOR_S = DOOR_R * 2 + 1;

    static final class DoorRoute {
        int x0, y0;
        final int[] d = new int[DOOR_S * DOOR_S];
    }

    final java.util.LinkedHashMap<City.Building, DoorRoute> doorFields = new java.util.LinkedHashMap<City.Building, DoorRoute>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<City.Building, DoorRoute> eldest) {
            // ((11.0) Three times as many kept (about 10 KB each): with fifteen thousand people going home and
            // hiding, the routes to their doors were worked out again and again. The one dropped is reused.)
            if (size() <= 480) return false;
            spareDoor = eldest.getValue();
            return true;
        }
    };
    DoorRoute spareDoor;
    int[][] doorBuckets;

    /** Buildings with people inside, by area (refreshed twice a second), for the dead looking for a way in. */
    static final int OCC_CELL = 160;
    java.util.ArrayList<City.Building>[] occGrid;
    int occGW, occGH;
    float occTimer;

    /**
     * (10.39) A route patch round a spot the dead are drawn to (a noise, where someone was last seen): every one
     * of them heading there shares it, instead of each planning a route of their own. Kept for twenty seconds
     * (things change: barricades, wrecks), the last few dozen of them.
     */
    static final class Lure {
        int x0, y0;
        float made;
        final int[] d = new int[LURE_S * LURE_S];
    }

    static final int LURE_R = 40, LURE_S = LURE_R * 2 + 1;
    final java.util.LinkedHashMap<Integer, Lure> lures = new java.util.LinkedHashMap<Integer, Lure>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<Integer, Lure> eldest) {
            // ((27.4) Kept to be used again for the next one (it's worked out afresh, every tile): each is 26 KB, and
            // a big horde roaming the streets made and dropped dozens a second.)
            if (size() > 40) {
                if (spareLures.size() < 8) spareLures.add(eldest.getValue());
                return true;
            }
            return false;
        }
    };
    final java.util.ArrayList<Lure> spareLures = new java.util.ArrayList<Lure>();
    int[][] lureBuckets;
    int luresMade;

    World.Lure lureField(float x, float y) {
        return zombieAI.lureField(x, y);
    }

    static float distToSegment(float x, float y, float ax, float ay, float bx, float by) {
        float vx = bx - ax, vy = by - ay, l2 = vx * vx + vy * vy;
        float t = l2 < 0.001f ? 0 : Math.max(0, Math.min(1, ((x - ax) * vx + (y - ay) * vy) / l2));
        float dx = x - (ax + vx * t), dy = y - (ay + vy * t);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /** A pharmacy with stock left, within maxDist. */
    /** ((13.1) The pharmacies, looked up once: every hurt person looked through every building in the city for one.) */
    java.util.List<City.Building> pharmacies;

    /**
     * The army checks everyone coming into its safe zones for bites. Most bites are found, and there's no
     * treating a bite: the person is turned away. Returns true if they were stopped at the gate.
     */
    float checkpointSaid;

    /** (10.32) Something that happened in the outbreak, and when (seconds of game time). */
    static final class Moment {
        float time;
        String text;
        /** ((14.4) Where it happened, to go and look from the timeline; NaN for what happened everywhere.) */
        float x = Float.NaN, y = Float.NaN;
    }

    /** The outbreak as it happened, for the summary. */
    final ArrayList<Moment> timeline = new ArrayList<Moment>();

    void timeline(String text) {
        stats.timeline(text);
    }

    void timeline(String text, float x, float y) {
        stats.timeline(text, x, y);
    }

    /** (14.2) How often a family hides a bitten relative's bite at a check. */
    static final float HIDE_BITE = 0.45f;
    /** Bites families have hidden this game (and got through the checks). */
    int bitesHidden;

    /** Whether the governor has already called out the National Guard this outbreak (and how many times, when). */
    boolean guardCalled;
    int guardWave;
    float guardCalledAt;

    void callNationalGuard() {
        war.callNationalGuard();
    }

    /**
     * ((12.1) Balance) How fast someone falls back while still shooting: a steady step back, not a sprint (at most of
     * a run before, as quick as the dead could run, so they could never close); outnumbered, a little quicker.
     */
    static float backOff(Entity e, boolean swarmed) {
        return Math.min(e.runSpeed * 0.8f, e.speed * (swarmed ? 1.3f : 1.1f));
    }

    /** ((13.3) The dead further off than this, in tiles, and soldiers with nothing to do ride out to them. */
    static final int RIDE_TILES = 50;
    /** ((13.3) Further than this from base and soldiers with nothing to do ride back. */
    static final float RIDE_HOME = 600;

    // ------------------------------------------------------------------ (14.2) raider gangs

    /** Gangs formed this game, and raiders out on the streets right now (counted each second). */
    int gangsFormed, raidersActive;
    static final int MAX_GANGS = 3;
    /** How far a raider shoots at people round a shop, and how long they count as hostile after firing. */
    static final float RAID_REACH = 100, HOSTILE_TIME = 20;

    // ------------------------------------------------------------------ (14.5) rot, migrations, the field hospital

    /** When the dead start to rot (seconds after rising), how long until they're as rotten as they get, and how much. */
    static final float ROT_START = 480, ROT_SPAN = 1200, ROT_MAX = 0.4f;

    float rot(Entity z) {
        return zombieAI.rot(z);
    }

    float migrationSaid = -999;

    /** (14.5) The field hospital: where it is (NaN until the army sets it up), and the time each patient has had there. */
    float hospitalX = Float.NaN, hospitalY = Float.NaN;
    static final float HOSPITAL_REACH = 1800, HOSPITAL_TIME = 8;

    /** Police and soldiers patched up at the field hospital this game. */
    int treated;

    /** (14.9) Mortar shells fired so far. */
    int mortarShots;

    // ------------------------------------------------------------------ (15.0) mounted and traffic police

    /** (15.0) The traffic officer on each junction (by its id), if any, and until when they're holding traffic for one coming through. */
    Entity[] trafficPost;
    float[] junctionHeld;
    /** (15.0) How far a mounted officer sees from up on a horse (marking the dead for the units round them). */
    static final float MOUNT_SEE = 150;

    Entity trafficOfficer(int id) {
        return police.trafficOfficer(id);
    }

    boolean trafficLooked;

    void holdJunction(int id, Fleet.Vehicle v) {
        police.holdJunction(id, v);
    }

    boolean junctionHeldFor(int id) {
        return police.junctionHeldFor(id);
    }

    // ------------------------------------------------------------------ (15.6) the island prison

    /** (15.6) The island prison has fallen (its gate open), and the outbreak has been started inside it. */
    boolean prisonFallen, prisonSeeded;
    /** (15.6) The ferry across the bay: {x, y, heading x, heading y, speed, kind}, which landing it's making for, and its wait there. */
    float[] ferryBoat;
    int ferryLeg;
    float ferryWait;

    boolean inPrison(float x, float y) {
        return prison.inPrison(x, y);
    }

    /** (15.6) How long the Prison Island stays quiet before the outbreak starts inside the walls. */
    static final float PRISON_QUIET = 45;

    void fallPrison() {
        prison.fallPrison();
    }

    /** (14.0) Mortars: rounds carried, how near and far they'll fire, how often, and the blast. */
    static final int MORTAR_ROUNDS = 16;
    static final float MORTAR_MIN = 150, MORTAR_MAX = 1100, MORTAR_EVERY = 6, MORTAR_BLAST = 48, MORTAR_DAMAGE = 170;
    /** (14.9) The fewest of the dead together worth a shell. */
    static final int MORTAR_PACK = 5;

    /** How far a combat medic goes to someone wounded, and how long patching them up takes. */
    static final float MEDIC_REACH = 160, MEDIC_TIME = 3.5f;

    /** ((11.4) The share of officers out in patrol cars.) */
    static final float PATROL_SHARE = 0.85f;

    /** ((11.4) One pair of officers in four walks a beat; the rest patrol by car.) */
    static boolean footPatrol(Entity e) {
        return (e.callsign / 2) % 4 == 0;
    }

    // ------------------------------------------------------------------ (11.5) posts kept manned

    /** Gate and checkpoint posts {x, y} that are kept manned, and who has each now (null for nobody). */
    final ArrayList<float[]> rosterPosts = new ArrayList<float[]>();
    final ArrayList<Entity> rosterHolders = new ArrayList<Entity>();
    float rosterCd = 5;

    void buildRoster() {
        posts.buildRoster();
    }

    void staffPosts() {
        posts.staffPosts();
    }

    String dutyText(Entity e) {
        return posts.dutyText(e);
    }

    float rideTimer;
    final java.util.HashMap<Integer, ArrayList<Entity>> rideGroups = new java.util.HashMap<Integer, ArrayList<Entity>>();
    float squadRideSaidAt;

    /**
     * (12.3) Soldiers drive anywhere more than a few blocks off: every couple of seconds, each squad (their own or
     * reinforcements) on foot with a long way to go - out to guard a safe zone, to a checkpoint or the cordon, to a
     * call, after the dead, or home to base - takes a truck: one from the base when they're on it, an empty one
     * near them, or one sent out to fetch them. Only with none of those to be had do they walk it.
     */
    /** ((24.3) Further than this from where they're going, police and soldiers drive. */
    static final float RIDE_FROM = 350;

    boolean queueingAtGate(Entity e) {
        return posts.queueingAtGate(e);
    }

    String postName(float x, float y) {
        return posts.postName(x, y);
    }

    boolean onDuty(Entity e) {
        return posts.onDuty(e);
    }

    int soldierDutyOf(Entity e, City.Facility base) {
        return posts.soldierDutyOf(e, base);
    }

    // ------------------------------------------------------------------ (10.37) police and soldiers' brain

    static final int A_DUTY = 0, A_FIGHT = 1, A_WOUNDED = 2, A_FALLBACK = 3, A_SEARCH = 4, A_COUNT = 5;
    final float[] armedScore = new float[A_COUNT];

    float[] zombieCentre(float x, float y, float radius) {
        return spatial.zombieCentre(x, y, radius);
    }

    static final String[] STREAK_LINES = {"That's %d! Keep them coming!", "%d down. I'm not done yet.",
            "%d of them! Somebody count for me!", "Make that %d. This street is ours."};

    boolean weaponsOut(Entity e) {
        return armedAI.weaponsOut(e);
    }

    /** (13.8) One of the busiest fights going on right now, for the hot spots list. */
    static final class HotSpot {
        float x, y;
        String place;
        int dead, armed;
        boolean zone;
    }

    /** How far round a call or a safe zone's wall the dead and the armed are counted for its hot spot. */
    static final float HOT_REACH = 160;

    java.util.List<World.HotSpot> hotSpots(int max) {
        return armedAI.hotSpots(max);
    }

    int armedNear(float x, float y, float r) {
        return spatial.armedNear(x, y, r);
    }

    /** (10.51) When police and soldiers last called each other in on the radio. */
    float jointSaid;

    // ------------------------------------------------------------------ the war: supplies, escalation

    /** (13.9) How far soldiers go to a truck for rounds, and how far from the base a fight must be for an ammo truck. */
    static final float TRUCK_REACH = 500, AMMO_TRUCK_FROM = 700;

    /** The army escalates as the city is lost: first it seals off the worst district, then it bombs it. */
    int escalation;
    float firebombTime, firebombX, firebombY;
    /**
     * ((14.2) The whole city sealed: every road out closed by the army, nobody let out, infected or not. And the air
     * strikes so far, and the time till the army will think about another.)
     */
    boolean quarantined;
    int strikes;
    float strikeCd;
    static final int MAX_STRIKES = 6;
    String firebombPlace;

    float[][] exits() {
        return war.exits();
    }

    // ------------------------------------------------------------------ (25.0) survivor bases

    final ArrayList<SurvivorBases.Base> bases = new ArrayList<SurvivorBases.Base>();
    int basesFounded, basesLost;
    float baseFoundedAt = -999;

    SurvivorBases.Base foundBase(City.Building b, boolean announce) {
        return baseLife.foundBase(b, announce);
    }

    void restoreSettlement(int bi, int x0, int y0, int x1, int y1, int room) {
        baseLife.restoreSettlement(bi, x0, y0, x1, y1, room);
    }

    SurvivorBases.Base restoreBase(City.Building b) {
        return baseLife.restoreBase(b);
    }

    void joinInto(SurvivorBases.Base m, SurvivorBases.Base a, SurvivorBases.Base b) {
        baseLife.joinInto(m, a, b);
    }

    // ------------------------------------------------------------------ after the war

    /**
     * The war decided isn't the end. A city that survives starts to recover: people come out, the bodies
     * and wrecks are cleared, damaged buildings are patched up, shops reopen, survivor groups take their
     * barricades down - though the dead may yet come back. A city that falls has its survivors hold on in
     * hiding while the army gets together one last force to take it back.
     */
    static final int AFTER_NONE = 0, AFTER_RECOVERY = 1, AFTER_FALLEN = 2;
    int aftermath;
    float clearSince = -1;
    float afterTime, flareCd;
    int flareUps, repaired, cleared, towed;
    boolean retakeSent, retakeWarned;
    float afterTick;
    int afterSaid;

    /** (14.2) How long rebuilding a fallen building takes, and the gap before the next one starts. */
    static final float REBUILD_TIME = 45, REBUILD_GAP = 20;
    int rebuilt;
    float rebuildCd = 120;

    /**
     * (14.2) Two minutes into the recovery, the fallen buildings go back up one at a time: a fenced-off site with
     * the work going on, then the building as it was, with room for people again. Not while anyone's on the site.
     */
    final java.util.HashSet<Entity> siteSeen = new java.util.HashSet<Entity>();
    int siteWait;

    // ------------------------------------------------------------------ survivor groups

    /**
     * A group of survivors that has made a building its home and means to stay: they fortify the street
     * round it bit by bit, take in people who turn up, send out supply runs for food and ammunition, keep
     * guards on the door .
     */
    static final class Holdout {
        City.Building b;
        String name;
        /** How far the fortifications have got (0-100), the group's spare ammunition, and its record. */
        float fort, runTimer = 40, recruitTimer, guardTimer;
        int stash, runs, said, count;
        float x, y;
        boolean gone;
    }

    final java.util.ArrayList<Holdout> holdouts = new java.util.ArrayList<Holdout>();
    float holdoutTimer = 5;

    World.Holdout found(City.Building b, boolean announce) {
        return holdoutLife.found(b, announce);
    }

    int members(World.Holdout h) {
        return holdoutLife.members(h);
    }

    static String cap(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    boolean peopleNear(float x, float y, float radius) {
        return spatial.peopleNear(x, y, radius);
    }

    /** The squad member a soldier sticks with: the lowest-numbered one still standing. */
    /** ((13.3) The soldiers of each squad, listed again once a second: for squadLead, which went through everyone. */
    final java.util.HashMap<Integer, ArrayList<Entity>> squadRoster = new java.util.HashMap<Integer, ArrayList<Entity>>();
    int squadRosterAt = -999;

    /**
     * ((16.9) How each kind of police officer is trained, as {aim, head shots, time between shots, reload time,
     * distance kept from the dead, nerve (how outnumbered before falling back)}: 1 and 50 and 4 for an ordinary
     * patrol officer. SWAT go in close, fast and accurate and hold on; marksmen stay well back; riot police hold a
     * close line; mounted officers use their speed to keep their distance; traffic police are no gunfighters and pull
     * back early; state troopers and park rangers shoot well at range; sheriff's deputies fight close with their
     * shotguns; FBI agents are steady, accurate and quick to reload.
     */
    static final float[] D_PATROL = {1f, 1f, 1f, 1f, 50, 4}, D_SWAT = {1.25f, 1.35f, 0.85f, 0.75f, 32, 7},
            D_MARKSMAN = {1f, 1f, 1f, 0.9f, 110, 3.5f}, D_RIOT = {0.9f, 0.85f, 1f, 1f, 28, 6.5f}, D_K9 = {0.95f, 0.95f, 1f, 1f, 45, 4.5f},
            D_MOUNTED = {0.85f, 0.85f, 1.05f, 1.1f, 85, 3.5f}, D_TRAFFIC = {0.8f, 0.75f, 1.15f, 1.2f, 70, 3},
            D_TROOPER = {1.1f, 1.1f, 0.95f, 0.9f, 60, 4.5f}, D_SHERIFF = {1f, 1.05f, 1f, 1f, 35, 5},
            D_RANGER = {1.15f, 1.2f, 1f, 1f, 75, 4}, D_FBI = {1.2f, 1.25f, 0.9f, 0.75f, 60, 5.5f};

    /** ((16.9) A police officer's training (see D_PATROL), or null for anyone else. */
    static float[] doctrine(Entity e) {
        if (e.type != Entity.COP) return null;
        switch (e.role) {
            case Entity.ROLE_SWAT: return D_SWAT;
            case Entity.ROLE_MARKSMAN: return D_MARKSMAN;
            case Entity.ROLE_RIOT: return D_RIOT;
            case Entity.ROLE_K9: return D_K9;
            case Entity.ROLE_MOUNTED: return D_MOUNTED;
            case Entity.ROLE_TRAFFIC: return D_TRAFFIC;
        }
        switch (e.agency) {
            case 1: return D_TROOPER;
            case 2: return D_SHERIFF;
            case 3: return D_RANGER;
            case 4: return D_FBI;
        }
        return D_PATROL;
    }

    // ------------------------------------------------------------------ hiding indoors, pickups

    /** Until the next count of who's on their way to each building. */
    float headingTimer;

    void burstOut(City.Building b) {
        indoors.burstOut(b);
    }

    /**
     * (11.5) How quickly the dead get through a building's door: a vault-like door holds out more than twice as long,
     * a glass front goes half again as quick.
     */
    static float doorWeakness(City.Building b) {
        int use = useOf(b);
        return use == Variants.U_VAULT ? 0.45f : use == Variants.U_GLASS ? 1.5f : use == Variants.U_TOOLS ? 0.8f : 1;
    }

    /** (11.5) What a building is good for when the dead come (Variants.U_). */
    static int useOf(City.Building b) {
        if (b.use < 0) b.use = b.kind == City.PHARMACY || b.kind == City.HOSPITAL ? Variants.U_MEDICAL : Variants.use(b.variant);
        return b.use;
    }

    static boolean isShop(City.Building b) {
        return b.kind == City.SHOP || b.kind == City.MARKET || b.kind == City.MALL || b.kind == City.PHARMACY || b.kind == City.KIOSK;
    }

    void smash(City.Building b) {
        indoors.smash(b);
    }

    boolean apocalypse() {
        return indoors.apocalypse();
    }

    World.Pickup nearestPickup(float x, float y, float radius) {
        return sandbox.nearestPickup(x, y, radius);
    }

    static final float PICK_CELL = 256;
    int[] pickStart, pickIdx = new int[64], pickCellOf = new int[64];
    int pickGW, pickGH;

    int[] pickCur;

    int pickupsAt = -1, pickupsN;

    static boolean inside(Entity e, Dispatch.SafeZone z, float margin) {
        return z.contains(e.x, e.y, margin);
    }

    /**
     * A safe zone's guards: in through a gate if they're outside; two at each gate, the rest along the inside
     * of the wall facing out, and some of those off to cover (and mend) any breach. While it's going up they
     * work on the next stretch of wall.
     */
    /** ((16.8) How far, in steps of walking cost, the way to a safe zone's gates is worked out for its guards. */
    static final int ZONE_REACH = 500;

    /** (12.8) How far round (radians, -pi to pi) from one heading to another. */
    static float angleDiff(float from, float to) {
        float diff = to - from;
        while (diff > Math.PI) diff -= TAU;
        while (diff < -Math.PI) diff += TAU;
        return diff;
    }

    static float turn(float from, float to, float amount) {
        float diff = to - from;
        while (diff > Math.PI) diff -= TAU;
        while (diff < -Math.PI) diff += TAU;
        if (amount > 1) amount = 1;
        return from + diff * amount;
    }

    // ------------------------------------------------------------------ movement

    static final float SUB_STEP = 1 / 30f;

    /** (10.57) atan2 to within a third of a degree, several times quicker: which way someone faces as they walk. */
    static float fastAtan2(float y, float x) {
        float ax = Math.abs(x), ay = Math.abs(y);
        if (ax < 1e-9f && ay < 1e-9f) return 0;
        float a = Math.min(ax, ay) / Math.max(ax, ay), s = a * a;
        float r = ((-0.0464964749f * s + 0.15931422f) * s - 0.327622764f) * s * a + a;
        if (ay > ax) r = 1.57079637f - r;
        if (x < 0) r = 3.14159274f - r;
        return y < 0 ? -r : r;
    }

    static final float VAULT_TIME = 0.55f;
    /** ((13.4) How long the dead take to get over a barrier: the ordinary ones, the quick ones, the big ones. */
    static final float CLIMB_TIME = 2.4f, CLIMB_RUNNER = 1.8f, CLIMB_BRUTE = 3.5f;
    /** ((13.4) How long one of the dead pushes at a barrier before it starts to climb. */
    static final float CLIMB_PRESS = 1.2f;

    /** ((13.4) How close the living must be, beyond a barrier, for the dead to climb it rather than go round. */
    static final float CLIMB_REACH = 7 * City.T;

    /** ((26.0) How close together the dead at a wall have to be to climb on each other, and how many make news. */
    static final float PILE_REACH = 20;
    static final int PILE_SAY = 6;
    float pileSaid = -999;

    /** ((26.0) How much faster a pile of n of the dead (counting the one climbing) gets over or through a wall. */
    static float pileBonus(int n) {
        return n < 3 ? 1 : Math.min(3, 1 + 0.3f * (n - 2));
    }

    /** ((13.5) One caught by a blast at (x, y): hurt, thrown and frightened, the more the closer. */
    /** ((15.3) Whose grenade or mortar shell is going off now, if anyone's: the dead it kills are theirs.) */
    Entity blastBy;

    /** Moves with wall sliding. Returns false if the move was (at least partly) blocked. */
    static final float[] SIDESTEPS = {0.8f, -0.8f, 1.4f, -1.4f};

    /** People knocked down in the crush, and when that was last reported. */
    int trampled;
    int trampledSaid;
    float stampedeCd;

    // ------------------------------------------------------------------ death, corpses, explosions

    int turnType(int was) {
        return death.turnType(was);
    }

    void loseHelmet(Entity t) {
        death.loseHelmet(t);
    }

    /** (10.59) How long a body that won't get up stays where it fell, in seconds. */
    static final float CORPSE_LIFE = 60;

    float runOver(float x, float y, float r, float speed, float angle) {
        return rides.runOver(x, y, r, speed, angle);
    }

    boolean hail(Fleet.Vehicle v) {
        return rides.hail(v);
    }

    void callRide(Entity e, Fleet.Vehicle v) {
        rides.callRide(e, v);
    }

    boolean release(Entity o, float x, float y) {
        return rides.release(o, x, y);
    }

    Fleet.Vehicle vehicleAt(float x, float y) {
        return rides.vehicleAt(x, y);
    }

    Entity nearestZombie(float x, float y, float radius) {
        return spatial.nearestZombie(x, y, radius);
    }

    void tankShell(float fx, float fy, float tx, float ty) {
        combat.tankShell(fx, fy, tx, ty);
    }

    Entity nearestVisibleZombie(float x, float y, float radius) {
        return spatial.nearestVisibleZombie(x, y, radius);
    }

    /** ((24.0) The zombies near a gunner, nearest first, for nearestVisibleZombie.) */
    Entity[] visZ = new Entity[64];
    float[] visD = new float[64];

    void airShot(float fx, float fy, Entity t) {
        combat.airShot(fx, fy, t);
    }

    void marksmanShot(float fx, float fy, Entity t) {
        combat.marksmanShot(fx, fy, t);
    }

    void cannonSplash(float x, float y, float r, float dmg) {
        combat.cannonSplash(x, y, r, dmg);
    }

    boolean turretShot(float fx, float fy, Entity t, float dmg) {
        return combat.turretShot(fx, fy, t, dmg);
    }

    void explode(float x, float y, float radius, float damage) {
        combat.explode(x, y, radius, damage);
    }

    final ArrayList<City.Building> hitBuildings = new ArrayList<City.Building>();

    /** Which wall of a building faces a point: 0 north, 1 south, 2 west, 3 east (as GameView draws them). */
    static int wallSide(City.Building b, float x, float y) {
        float dn = Math.abs(y - b.y0), ds = Math.abs(y - b.y1), dw = Math.abs(x - b.x0), de = Math.abs(x - b.x1);
        if (y < b.y0) dn -= 1000;
        if (y > b.y1) ds -= 1000;
        if (x < b.x0) dw -= 1000;
        if (x > b.x1) de -= 1000;
        float m = Math.min(Math.min(dn, ds), Math.min(dw, de));
        return m == dn ? 0 : m == ds ? 1 : m == dw ? 2 : 3;
    }

    /** Distance along a wall from the corner GameView starts drawing it at. */
    static float along(City.Building b, int side, float x, float y) {
        switch (side) {
            case 0: return Math.max(0, Math.min(b.x1 - b.x0, x - b.x0));
            case 1: return Math.max(0, Math.min(b.x1 - b.x0, b.x1 - x));
            case 2: return Math.max(0, Math.min(b.y1 - b.y0, b.y1 - y));
            default: return Math.max(0, Math.min(b.y1 - b.y0, y - b.y0));
        }
    }

    void collapseBuilding(City.Building b) {
        combat.collapseBuilding(b);
    }

    // ------------------------------------------------------------------ effects

    void particle(float x, float y, float vx, float vy, float life, float size, int color, byte type) {
        effects.particle(x, y, vx, vy, life, size, color, type);
    }

    /** ((14.4) How long a tracer shows: long enough for its streak to be seen flying.) */
    static final float TRACER_LIFE = 0.13f;

    void decal(float x, float y, float r, int color, byte kind, float angle) {
        effects.decal(x, y, r, color, kind, angle);
    }

    void skid(float x, float y, float angle, float length) {
        effects.skid(x, y, angle, length);
    }

    // ------------------------------------------------------------------ the wilds (10.14)

    /** Park rangers' shirts. */
    static final int RANGER_SHIRT = 0xFF8C7C54;
    /** The ranger station (null if there's no park), each fire tower's lookout on duty and the one on the way. */
    City.Building rangerStation;
    /** The ranger who stays at the station while the others are out on the trails. */
    Entity stationKeeper;
    Entity[] towerKeeper, towerRelief;
    float[] towerShift;
    float rescueCd = 5, wildsTick;

    void makeRanger(Entity e) {
        population.makeRanger(e);
    }

    // ------------------------------------------------------------------ getting out (10.18)

    /** The map in cells of DANGER_CELL tiles: how many of the dead are in each, and how far each is from one that has them. */
    static final int DANGER_CELL = 20;
    float[] danger;
    int[] dangerDist;
    int dgw, dgh;
    float dangerTick;

    int dangerDistAt(float x, float y) {
        return exodus.dangerDistAt(x, y);
    }

    // ------------------------------------------------------------------ fleeing the city (10.47)

    /** Those who've fled the city (out of the world until they come home), and how it's gone. */
    final ArrayList<Entity> away = new ArrayList<Entity>();
    boolean exodusStarted;
    int fled, cameHome, exodusSaid, fledByTransit;
    float exodusTick;

    /**
     * (10.49) An evacuation bus stop: where the bus pulls up on the road, where people wait on the pavement beside
     * it, and the bus on its way to it or waiting there (or null).
     */
    static final class EvacStop {
        float bx, by, x, y;
        String place;
        boolean closed;
        Fleet.Vehicle bus;
        float busCd;
        /** (10.60) How many have been given a place to wait here since the last bus (see waitSpot). */
        int waiting;
        /** ((15.9) A dock on the water: boats come in to (bx, by) from offshore along this way, not buses by road.) */
        boolean dock;
        int[] route;
        /** ((15.10) The airport: evacuation flights from here.) */
        boolean airport;
    }

    /** (10.60) How many have been given a place on the platform since the last train. */
    int trainWaiting;
    /** (10.60) How far from a stop people wait for the bus, at the most (and how far off it still picks them up). */
    static final float STOP_REACH = 64;

    /** (10.49) The evacuation bus stops (chosen when people start leaving); and the trains, if there's a station. */
    final ArrayList<EvacStop> evacStops = new ArrayList<EvacStop>();
    /** Who waits for the train: their stop number. */
    static final int STOP_TRAIN = -2;

    // ------------------------------------------------------------------ (15.10) the airport

    /** (15.10) The airport has fallen to the dead (no flights in or out), and how long they've held the terminal. */
    boolean airportClosed;
    float airportThreat, airportClear;

    boolean exodusOn() {
        return exodus.exodusOn();
    }

    boolean byTransit() {
        return exodus.byTransit();
    }
    /** Where the roads (or the causeway) leave the map: the ways out, and back in. */
    float[][] exits;

    /** One in eight won't leave their home whatever happens. */
    static boolean stays(Entity e) {
        return (e.nameSeed & 7) == 0;
    }

    /** The walk planner found no way to there on foot. */
    static boolean unreachable(Entity e, float x, float y) {
        return e.path != null && e.path.length == 0 && e.pathDest == null && e.planX == x && e.planY == y && World.hypot(x - e.x, y - e.y) > 60;
    }

    void droveOut(java.util.List<Entity> riders) {
        exodus.droveOut(riders);
    }

    int boardEvac(Fleet.Vehicle v, int stop, float x, float y, float radius, int room) {
        return exodus.boardEvac(v, stop, x, y, radius, room);
    }

    int waitingFor(int stop, float x, float y, boolean there) {
        return exodus.waitingFor(stop, x, y, there);
    }

    // ------------------------------------------------------------------ containment (10.16, infection zones 10.46)

    static final int CORDON_POSTS = 12, MAX_ZONES = 4, MAX_CHECKPOINTS = 4;

    /**
     * (10.46) A designated infection zone: the ground round one bunch of the dead. Police throw a perimeter round
     * it (twelve posts, a patrol car driving a crew out to any nobody can walk to) and the army closes the roads
     * into it with checkpoints. It follows the dead as they move, and is lifted only once there are none of them
     * left in it - or, with several zones and not enough people for all of them, it waits its turn while the
     * worst are held.
     */
    static final class InfectionZone {
        float x, y, r, quietFor, priority, saidAt;
        int zombies, people;
        boolean critical, wasCritical;
        String place;
        /**
         * (11.0) The quarantine wall: how long the zone has stood where it is, the ring of tiles it's going up on (and
         * how far round it's got), and each tile walled so far with what it was before {tile, old}.
         */
        float stableFor;
        int[] ring;
        int ringAt;
        final ArrayList<int[]> wall = new ArrayList<int[]>();
        final float[] post = new float[CORDON_POSTS * 2];
        final int[] manned = new int[CORDON_POSTS];
        final Dispatch.Incident[] postCall = new Dispatch.Incident[CORDON_POSTS];
        /**
         * Army checkpoints on the roads in: {x, y, angle} (also in city.checkpoints while the zone stands). ((27.5) None put
         * up any more - the list stays, always empty - and whether the zone's ring is up is ringUp.)
         */
        final ArrayList<float[]> checkpoints = new ArrayList<float[]>();
        boolean ringUp;
        /** Two guard spots at each checkpoint, and the soldiers on them (or on their way). */
        final ArrayList<float[]> guardSpots = new ArrayList<float[]>();
        final Entity[] guards = new Entity[MAX_CHECKPOINTS * 2];
        final Dispatch.Incident[] guardCall = new Dispatch.Incident[MAX_CHECKPOINTS * 2];
    }

    final ArrayList<InfectionZone> infection = new ArrayList<InfectionZone>();
    float cordonTick = 3;
    float[] clusterX = new float[16], clusterY = new float[16], clusterR = new float[16];
    int[] clusterN = new int[16];

    /**
     * How a zone ranks against the others: one already held, with people on its perimeter and squads on their way,
     * only gives way to somewhere clearly worse (not one that's just a few more of them this minute).
     */
    static float rank(InfectionZone z) {
        return z.priority * (z.wasCritical ? 1.6f : 1);
    }

    /** Aboard a vehicle (out of the world for the ride, not dead). */
    static boolean riding(Entity g) {
        return g.dead && g.removed && g.rig != null && g.hp > 0;
    }

    /**
     * (11.0) Police drones: one at each police station (four at most), up once the outbreak's on the news. Each flies
     * out over the infection zones and the calls with the fewest on them, sweeps round, and radios in the dead it
     * sees, those inside buildings included, as new calls; it comes home to recharge when its battery runs low.
     */
    static final class Drone {
        float x, y, tx, ty, homeX, homeY, battery = 100, spotCd, sweepCd, speakCd;
        int number, state;
        float saidAt = -99;
    }

    static final int DRONE_DOCKED = 0, DRONE_OUT = 1, DRONE_HOME = 2, MAX_DRONES = 8;
    static final float DRONE_SEE = 170;
    final ArrayList<Drone> drones = new ArrayList<Drone>();
    boolean dronesMade;

    /** (14.3) How much likelier a hit to the head is on one of the dead a drone has marked. */
    static final float SPOTTED_AIM = 1.3f;

    /** (14.8) How far a police helicopter sees from its hover, marking the dead for the units below. */
    static final float HELI_SEE = 210;

    void heliSpot(Fleet.Vehicle v, float dt) {
        containment.heliSpot(v, dt);
    }

    void barrierHold(Fleet.Vehicle v, float dt) {
        containment.barrierHold(v, dt);
    }

    World.InfectionZone zoneOfPost(float x, float y) {
        return containment.zoneOfPost(x, y);
    }

    /** How many rescue teams the police have (their helicopters): more on the bigger maps and with more precincts. */
    int rescueTeams = 1;

    int rescueStep(Fleet.Vehicle v, float dt) {
        return rescue.rescueStep(v, dt);
    }

    boolean rescueCalled(Fleet.Vehicle v) {
        return rescue.rescueCalled(v);
    }

    final java.util.ArrayList<Entity> sarClaimed = new java.util.ArrayList<>();

    void dropRescued(Fleet.Vehicle v) {
        rescue.dropRescued(v);
    }

    // ------------------------------------------------------------------ the dead finding their way (10.15)

    // ------------------------------------------------------------------ ammunition running out (10.16)

    float rideSaidAt;

    City.Facility restockFor(Entity e) {
        return ammo.restockFor(e);
    }

    int restockCrew(java.util.List<Entity> crew, City.Facility f) {
        return ammo.restockCrew(crew, f);
    }

}
