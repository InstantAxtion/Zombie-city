package com.instantaxtion.zombiesandbox;

/** Someone in the city: a civilian, a cop, a soldier, a medic, a dog or one of the zombie kinds. */
final class Entity {
    static final int CIVILIAN = 0, COP = 1, SOLDIER = 2, MEDIC = 3, DOG = 4, RAIDER = 5, ZOMBIE = 6, RUNNER = 7,
            BRUTE = 8, CRAWLER = 9, SCREAMER = 10, ZOMBIE_DOG = 11, /** ((22.6) In the old spitter's place. */ BRUISER = 12, BLOATER = 13,
            /** (Firefighters went in 10.49: any in an old save load as civilians.) */
            FIREFIGHTER = 14,
            /** (The other game modes' gang members and enemy soldiers went in 19.6: any in an old save load as zombies.) */
            GANGSTER = 15, INVADER = 16;
    static final int TYPE_COUNT = 17;

    /**
     * (10.50) The kind someone of kind t is now: the kinds taken out of the game (crawlers, bloaters, spitters and
     * screamers come back as ordinary zombies; firefighters, medics and raiders are ordinary people).
     */
    static int current(int t) {
        switch (t) {
            case CRAWLER: case BLOATER: case SCREAMER: return ZOMBIE;
            case FIREFIGHTER: case MEDIC: case RAIDER: return CIVILIAN;
            case GANGSTER: case INVADER: return ZOMBIE;
            default: return t;
        }
    }
    static final String[] NAMES = {"Civilian", "Cop", "Soldier", "Medic", "Dog", "Raider", "Zombie", "Runner", "Brute",
            "Crawler", "Screamer", "Zombie dog", "Bruiser", "Bloater", "Firefighter", "Gang member", "Enemy soldier"};

    /** Soldiers' jobs: rifleman, the commander, snipers and machine gunners. */
    static final int ROLE_RIFLE = 0, ROLE_COMMANDER = 1, ROLE_SNIPER = 2, ROLE_GUNNER = 3;
    /** Police jobs: riot officers carry shields, K9 handlers work with a police dog. */
    static final int ROLE_RIOT = 4, ROLE_K9 = 5;
    /** The National Guard: soldiers called up by the governor to protect civilians. */
    static final int ROLE_GUARD = 6;
    /** SWAT: police in body armour with carbines, called out to the worst trouble. */
    static final int ROLE_SWAT = 7;
    /** Soldiers: the squad's combat medic (patches up the wounded under fire) and its grenadier (a launcher). */
    static final int ROLE_CORPSMAN = 8, ROLE_GRENADIER = 9;
    /** Police: a marksman with a scoped rifle. */
    static final int ROLE_MARKSMAN = 10;
    /** Police special operations: search and rescue, lowered from the rescue helicopter on the winch. */
    static final int ROLE_SAR = 11;
    /** A helicopter pilot (10.20): stands by the pad until their aircraft is needed. */
    static final int ROLE_PILOT = 12;
    /** (11.0) Marines landed by boat; army recon scouts. (13 was the bomb technician, gone in 11.3.) */
    static final int ROLE_MARINE = 14, ROLE_RECON = 15;
    /** (14.0) Army combat engineers (two at each base): they get a safe zone's wall up three times as fast. */
    static final int ROLE_ENGINEER = 16;
    /** (14.0) An army mortar team: shells packs of the dead a few streets off (its rounds are kept as grenades). */
    static final int ROLE_MORTAR = 17;
    /** ((15.0) Police officers on horseback, and traffic officers at the busy junctions.) */
    static final int ROLE_MOUNTED = 18, ROLE_TRAFFIC = 19;
    /** ((15.6) The guards at the island prison.) */
    static final int ROLE_PRISON_GUARD = 20;
    static final String[] ROLE_NAMES = {"Soldier", "Commander", "Sniper", "Gunner", "Riot officer", "K9 handler",
            "National Guard", "SWAT officer", "Combat medic", "Grenadier", "Police marksman", "Rescue specialist", "Pilot",
            "(unused)", "Marine", "Recon scout", "Combat engineer", "Mortar team", "Mounted officer", "Traffic officer", "Prison guard"};

    /*
     * ((13.5) What the simulation looks at for everyone every step - what they are, where, how they are, and how
     * often they need thinking about - declared first and together, so it sits side by side in memory: looking at
     * each of thirty thousand people used to mean fetching four or five scattered pieces of them.)
     */
    boolean dead, removed;
    Entity leader;
    int type, role, task;
    float x, y, vx, vy, angle;
    float hp, maxHp, radius, speed, runSpeed, mass = 1f;
    float fleeTimer, stun, charge, noiseTimer, vaultT;
    int lodZ, lodZAt, lodH, lodHAt, followerFrame, nameSeed;
    /** (10.31) How many frames' worth this one moves this frame: 1, or for the calm off screen 3 every third frame (0 between). */
    transient int lodStep = 1;

    // Desired movement for this tick: unit direction and speed.
    float mx, my, want;

    float cooldown, reload, biteCd, grenadeCd, meleeCd;
    float wanderAngle, wanderTimer, infectTimer;
    /** (11.1) Police and soldiers with nothing on: where their beat or duty takes them next, and how long they wait there. */
    float dutyX, dutyY, dutyWait, dutyFor;
    /** (11.2) The way along the street an officer on the beat is walking. */
    float dutyHx, dutyHy;
    /** (11.2) The partner an officer walks the beat with (the one with the lower callsign leads), and when to look for one. */
    Entity beatMate;
    float mateCd;
    int dutyStep;
    float hurt, phase;
    float threatX, threatY;
    /** Rounds in the magazine, magazine size and spare rounds. Nobody gets more without resupplying. */
    int ammo, magSize, reserve, burst, grenades;
    /** A civilian carrying a gun (their own, or one picked up). */
    boolean hasGun;

    /** Safe zone checkpoints: already checked for bites, and turned away. */
    boolean screened, refused;
    /** (14.2) Bitten, and their family is hiding it: waved through the checks, and nobody's ready when they turn. */
    transient boolean concealed;
    boolean infected, killedByZombie, gibbed, aiming, blocked, paused;

    int body, head, skin;
    /** For zombies: what the zombie used to be, or -1 if it was spawned as a zombie. */
    int origin = -1;

    // Zombies: where a noise came from, and the screamer's cooldown.
    float noiseX, noiseY, screamCd;

    // Dispatch: current job (task, at the top), radio callsign and speech icons.
    Dispatch.Incident incident;
    /** ((12.0) The call whose perimeter they have a place on, and where on it.) */
    transient Dispatch.Incident ringOf;
    transient float ringA;
    Dispatch.SafeZone zone;
    int slot, callsign, squad, member;
    boolean onScene, outOfAmmoSaid;
    /** A spot of their own picked for a while (a stroll inside a safe zone's wall). */
    float spotX, spotY;
    /** Police or soldiers who came in from outside the city (10.27): they go home again once it's over. */
    boolean outsider;
    /** (10.48) Seconds an officer or soldier has stood about on foot with no car and nothing to do. */
    float strandedFor;
    /** (10.49) The evacuation bus stop someone fleeing the city is waiting at (World.STOP_TRAIN: the train), or -1. */
    int exitStop = -1;
    /** (10.48) Seconds before someone who couldn't leave (no car near) thinks about it again. */
    float exodusCd;
    /** (10.32) One of the dead still in a police or army vest and helmet: harder to put down. */
    boolean armored;
    float phoneTimer, talkTimer, callCd, taskTimer;
    /** A spot to stand at: guard posts and player orders. */
    float postX, postY;
    /** Path to an ordered destination. */
    City.WalkField orderField;
    /** The building a civilian is running to hide in. */
    City.Building building;
    /** A dropped gun a civilian is going to pick up. */
    World.Pickup pickup;

    // (leader, at the top: family, followers stay with their leader.)
    /** ((11.6) The way a soldier's squad is heading, eased, for their place in the wedge: 0, 0 for not yet known.) */
    transient float formHx, formHy;
    /** Zombie hordes: members follow a leader who roams the city. */
    Entity hordeLeader;
    boolean leadsHorde;
    /** ((21.0) The alpha at the head of a big horde: far tougher; when it falls the horde scatters. */
    boolean alpha;
    /** ((21.0) How many guns picked this one out lately, and until when that counts (fire is spread along a crowd). */
    transient int aimN;
    transient float aimT;
    /** ((21.0) How many looks running they've been standing inside something solid (World.unstick). */
    transient byte inSolid;
    /** ((21.0) Sent in after one of the last of the dead (the mop-up): seconds until they're let go again. */
    transient float sweepT;
    /** ((21.0) In the mop-up: where one of the last of the dead was last seen to move, and for how many seconds it hasn't. */
    transient float mopX, mopY;
    transient int mopT;
    /** ((21.0) Indoors and armed: whether there's one of the dead to shoot from where they stand, looked for a few times a second. */
    transient float indoorLook;
    transient boolean indoorSees;
    /** ((21.2) The fight as last read (World.sense): the dead near, from how many sides, the nearest, the way out. */
    transient int tN, tSides, escK = -1, breakCount;
    transient float tNear = 999, escX, escY, senseAt, breakT;
    /** How bad the best way out is: the dead along it (a hundred more if it's walled off). */
    transient float escCost;
    /** ((21.3) A horde leader's last tally (World.hordeTally): followers, those strung out behind, and when. */
    transient int tallyN, tallyBehind, tallyFrame = -1;
    transient int leadStart;
    transient boolean leadSeen;
    /** ((21.0) Scattered from a horde whose alpha fell: seconds until they'll join another. */
    transient float scatterT;
    float roamX, roamY, feedTimer;
    /**
     * (10.58) The dead at a door that holds: how long this one has been at it, and the building it gave up on (and
     * till when). Quiet survivors behind a good door are left alone for a while, so a fallen city has people in it
     * for longer.
     */
    float doorTime, ignoreUntil;
    /** (10.59) A horde that heard something charges for it as one: how long this one's still running, and (on the leader) how soon it can be set off again. */
    float surge, surgeCd;
    /** (11.0) Seconds until an officer or soldier walking to a far call next looks for a ride (see World.thinkArmed). */
    transient float driveCd;
    /** ((13.9) A combat medic: who they're on their way to patch up, how long they've been at it, and the wait till they look again.) */
    transient Entity patient;
    transient float treatT, medicCd;
    /** ((13.9) The army truck with spare rounds they're on their way to, instead of the base.) */
    transient Fleet.Vehicle supplyTruck;
    /** ((14.3) Until when a police drone overhead has this one of the dead marked for the units on the ground.) */
    transient float spottedUntil;
    /** ((14.5) The dead: when this one rose (game time), for how far it's rotted.) */
    float risen;
    /** ((14.5) A horde leader on a long march across the city to the biggest crowd of the living.) */
    transient boolean migrating;
    /** ((14.5) Time spent being treated at the field hospital so far.) */
    transient float hospitalT;
    /** ((14.7) In the squad of the one you're playing: who, and their place in the line behind them.) */
    transient Entity commandedBy;
    /** ((14.9) When a way that couldn't be found is next looked for again.) */
    transient float pathRetry;
    /** ((15.6) Locked up in the island prison (till it falls).) */
    boolean inmate;
    /** ((14.9) Flying lessons on the pad so far, for an officer or soldier training to be its pilot.) */
    float trainT;
    /** ((14.9) A mortar team: when it last fired (a new fire mission after a lull is called in), and its restocking.) */
    transient float lastShot = -999;
    /** ((15.0) A traffic officer waving an emergency vehicle through; a mounted officer's next look round, and patrol spot.) */
    transient float waveUntil, lookT, patrolX, patrolY;
    transient int squadSlot;
    /** (14.2) One of a gang of armed looters (its leader has no leader). */
    boolean raider;
    /**
     * ((17.1) Inside a building: the one they mean to spend time in (on an errand, at work or home, or hiding) and for
     * how long; how long they've been on the way there; where in it they're making for; and the way through its rooms.
     */
    transient City.Building stayIn;
    transient float stayT, stayWalk, inSpotX, inSpotY, inSpotT, inSpotA;
    transient boolean stayHide;
    transient float[] route;
    transient int routeI;
    transient float routeTx, routeTy;
    transient City.Building routeB;
    /** ((17.1) When they were last finding their way in, out of or round a building: they squeeze past each other there. */
    transient float inT = -9;
    /**
     * ((17.6) Cover: where the one they're trading shots with is and when they last fired, the spot they're making for
     * (coverX -1 for none) and when they next look for one; and how pinned down they are by fire coming their way (0-1).
     */
    transient float coverFromX, coverFromY, lastAimT = -99, coverT, coverX = -1, coverY, suppress;
    /** ((14.2) A raider who's opened fire on people lately: police, soldiers and armed survivors shoot back while this lasts.) */
    transient float hostileT;
    /** ((14.2) The shop a raider gang's leader is on the way to strip.) */
    transient City.Building raidShop;
    /** (11.0) Signed up as a reserve pilot; and the helicopter they've been called up to fly. */
    transient boolean pilotReserve;
    transient Fleet.Vehicle pilotFor;
    City.Building doorAt, ignoreB;
    int hordeSize;
    boolean hordeAnnounced;
    /** Civilians: already asked to sign up (and said no), and where they are going to enlist. */
    boolean asked;
    City.Facility enlistAt;
    /** Zombies approach from their own angle so a crowd surrounds its prey. */
    float flank;
    // (nameSeed, at the top: picks this person's name, see Names.)
    /** Zombies shot or run down by this person. */
    int kills;
    /** ((15.1) Lives saved: one of the dead put down right on someone, or a wounded officer or soldier patched up.) */
    int saved;
    /** Newly turned zombies are quicker for a while. */
    float fresh;
    /** How long someone remembers where they last saw a zombie (threatX/threatY). */
    float fear;
    /** Getting unstuck: how long they've been making no progress, and a sidestep in progress. */
    float stuckTime, unstick, unstickAngle;
    /** Civilians: a cop or soldier nearby to run towards. */
    Entity protector;
    /** A car this civilian has flagged down and is running to. */
    Fleet.Vehicle ride;
    /** Zombies: where the prey was last seen, how long they remember it, and time left searching there. */
    float lastX, lastY, memory, searchTimer, moanCd;
    /** (10.39) The way the one they were after was going when last seen; a horde leader's trail (World.TRAIL
     * points, newest at trailHead - 1) and whether it's slowed to let stragglers catch up. */
    transient float trackX, trackY;
    transient float[] trail;
    transient int trailHead, trailN;
    transient boolean hordeWait;
    /** (10.39) Had one of the dead in sight last time they thought about it. */
    transient boolean sawThreat;
    /** Where this one last left a footprint or a drop of blood, and how long their feet stay wet after wading. */
    float markX = -1, markY, wet;
    boolean markLeft;
    /** Getting out of an area the dead have taken (10.18): how long to stay clear once out. */
    float evacTime;
    /** Civilians: their home, and the errand they are on (a place to go and how long to stay). */
    City.Building home, errand;
    /**
     * Everyday life: what this person does with their day (J_ constants), where they work, and the state of
     * whatever they're doing now (a run, a delivery round, a stall, a chat).
     */
    int job;
    /** Knows the dead are walking: no more everyday life, only getting somewhere safe. */
    boolean aware;
    /** The fire engine or patrol car this person crews (null if none). */
    Fleet.Vehicle rig;
    /** ((22.3) On Fleet's list of those with a vehicle (1) or not (0). */
    int footGen;
    /** For police: 0 the city police, 1 highway patrol, 2 the sheriff's department (see Country). */
    int agency;
    /** A walking route (tile indices) to a building, how far along it they are, and where it goes. */
    int[] path;
    int pathIdx;
    City.Building pathDest;
    City.Building work;
    float jobTimer, chat;
    int jobStep;
    Entity chatWith;
    float[] spot;
    java.util.ArrayList<City.Building> round;
    static final int J_NONE = 0, J_WORKER = 1, J_HOMEBODY = 2, J_ERRANDS = 3, J_SHOPKEEPER = 4, J_STUDENT = 5,
            J_JOGGER = 6, J_PARK = 7, J_POSTIE = 8, J_VENDOR = 9,
            // Out in the wilds (10.14): hiking the trails, fishing at a lake, round the campfire, and the fire
            // lookouts: on duty up a tower, on the way to start a shift, and heading home after one.
            J_HIKER = 10, J_FISHER = 11, J_CAMPER = 12, J_LOOKOUT = 13, J_RELIEF = 14, J_OFFDUTY = 15,
            // (11.1) A day at the beach: on a towel in the sun, or a walk along the water's edge.
            J_BEACH = 16;
    /** Up a fire lookout tower: the dead have to climb the stairs to get at them. */
    boolean aloft;
    /** A walk along a trail (see City.hikes): how far along, and which way (1 out, -1 back). */
    float[] hike;
    int hikeIdx, hikeDir = 1;
    /** Where a planned walking route (path) goes, when it's to a spot rather than a building. */
    float planX = -1, planY = -1;
    /** Rescue team: seconds left coming down the winch line, and the person they're bringing back. */
    float roping;
    Entity escort;
    /**
     * Police and soldiers with no ammunition left anywhere to get: they keep watch near their station and
     * call in what they see. And how long before they next think about restocking while running low.
     */
    boolean scout;
    float lowCd;
    /** A zombie: how long it has been climbing the stairs of a fire tower. */
    float climb;
    float errandTimer;
    boolean homeChecked;
    /** The last hard knock (blast, car, charge), so the body flies the right way if it kills them. Fades fast. */
    float knockX, knockY;
    /** Brutes: a charge in progress (seconds left), its direction and cooldown. Crawlers: lying in wait. */
    float chargeCd, chargeX, chargeY;
    boolean hidden;
    /** Armed residents holding a building together. */
    boolean militia;
    /** When they last bit, fired, swung or shoved, screamed and spat (world time): for the animations. */
    float biteAt = -9, shotAt = -9, swingAt = -9, screamAt = -9, spitAt = -9;
    /** A survivor group this person belongs to; a supply run they're on, and what they're bringing back. */
    World.Holdout holdout;
    City.Building scavenge;
    int carryFood, carryAmmo;
    boolean homeward;
    /** The gun they carry if it isn't their standard issue (pistol, shotgun or rifle), and a bat or axe. */
    int weapon, melee;
    static final int W_STD = 0, W_PISTOL = 1, W_SHOTGUN = 2, W_RIFLE = 3, W_HUNTING = 4, W_REVOLVER = 5;
    /** (11.4) A submachine gun (SWAT, the feds), a carbine (troopers, recon, some patrol officers), a marksman rifle. */
    static final int W_SMG = 6, W_CARBINE = 7, W_DMR = 8;
    static final int M_NONE = 0, M_BAT = 1, M_AXE = 2, M_MACHETE = 3, M_CROWBAR = 4;
    static final String[] WEAPON_NAMES = {"", "Pistol", "Shotgun", "Rifle", "Hunting rifle", "Revolver", "Submachine gun", "Carbine",
            "Marksman rifle"};
    static final String[] MELEE_NAMES = {"", "Bat", "Axe", "Machete", "Crowbar"};

    /** The kind of gun in their hands. */
    int gunKind() {
        return weapon != W_STD ? weapon : type == SOLDIER ? W_RIFLE : W_PISTOL;
    }

    /** (11.4) What the gun in their hands is called, the specialists' own included. */
    String gunName() {
        if (weapon == W_STD) {
            if (type == SOLDIER) {
                switch (role) {
                    case ROLE_GUNNER: return "Machine gun";
                    case ROLE_SNIPER: return "Sniper rifle";
                    case ROLE_GRENADIER: return "Rifle and grenade launcher";
                    case ROLE_MORTAR: return "Carbine and mortar";
                    case ROLE_COMMANDER: return "Pistol";
                    default: return "Assault rifle";
                }
            }
            if (role == ROLE_MARKSMAN) return "Scoped rifle";
        }
        return WEAPON_NAMES[gunKind()];
    }
    /** The last place this one went on an errand (so they go somewhere else next). */
    City.Building lastErrand;
    // ((13.5) followerFrame (set to the world's grid frame when someone is following this one), lodZ, lodZAt, lodH and
    // lodHAt (the distances last looked up for the level of detail, and when) and lodStep: declared at the top.)
    /** (10.34) Until a fleeing civilian looks round for a cop or soldier again. */
    transient float protectorCd;
    /** (10.36) Following a route: the closest yet to the point it's heading for, and how long since that improved. */
    transient float pathBest = Float.MAX_VALUE, pathStall;
    transient int[] pathSeen;
    transient int pathRetries;
    /** ((12.0) When their route was last thrown out for a first corner they couldn't see.) */
    transient float replanAt = -99;
    /** ((12.0) Until when a field that led nowhere from here is left alone, for a planned route instead.) */
    transient float fieldOffUntil = -99;
    /** ((12.0) Inside the wall of the zone they guard, as of the last look.) */
    transient boolean wasInside;
    /** ((12.1) Balance) A zombie last brought down by shots to the body, not the head: it may get up again. */
    transient boolean bodyShot;
    /** (12.3) Not to be offered another lift before this time (World.rideCheck). */
    transient float rideCd;
    /** ((21.4) When they last got a lift, how far they had to go then, and how many lifts in a row got them nowhere.) */
    transient float rideAt = -999, rideLeft;
    transient int rideWasted;
    /** ((22.0) A police dog's steps until it next sniffs the air; a bite the dogs have found. */
    transient float sniffT, magCd;
    boolean sniffed;
    transient boolean fallbackSaid, fortifySaid;
    /** (12.3) The lift they're on, or waiting for: what they were doing before it, to go back to if it falls through. */
    transient Dispatch.Incident rideFrom;
    /** (10.37) What the brain has them doing (World.G_*), until it thinks again, and the building it had in mind. */
    int goal;
    transient float goalCd, goalTime;
    transient City.Building hideCandidate, storeCandidate;
    transient World.Pickup pickCandidate;
    /** Heard the safe zones on the news and means to go. */
    boolean heardZone;
    /** Which way (1 or -1) this one steps round obstacles it walks straight into. */
    float slideSide = 1;
    /** Breath for sprinting (0 to 1): the living tire, the dead don't. */
    float stamina = 1;
    /** Police and soldiers: when they last radioed a sighting to the others, and a retreat already called. */
    float shareCd;
    /** (10.50) Until a civilian running from the dead next shouts a warning to those about. */
    float warnCd;
    /** (10.51) Climbing over a fence: time left, and from where to where. */
    float vaultX0, vaultY0, vaultX1, vaultY1;
    /** ((13.4) How long this climb over takes: a moment for the living, a struggle for the dead.) */
    float vaultDur = 0.55f;
    /** ((13.4) How long one of the dead has been pressed up against a barrier it could climb. */
    float climbPress;
    /** Time until this shooter's next gunshot alerts the people around (one per burst, not per round). */
    float alertCd;
    /** When they last came out of hiding into a quiet street (they don't go straight back in on hearsay). */
    float emerged = -999;
    boolean retreatSaid;

    /** One of the dead: who the police and the army fight, and who everyone else runs from. */
    boolean isZombie() {
        return type >= ZOMBIE && type <= BLOATER;
    }

    static boolean isZombieType(int type) {
        return type >= ZOMBIE && type <= BLOATER;
    }

    /** ((17.0) One of the dead themselves: they bite, rot, shamble, climb, get back up. */
    boolean isUndead() {
        return type >= ZOMBIE && type <= BLOATER;
    }

    boolean isArmed() {
        return type == COP || type == SOLDIER;
    }

    /** Anyone who can shoot: police, soldiers and civilians with a gun. */
    boolean canShoot() {
        return isArmed() || hasGun;
    }
}
