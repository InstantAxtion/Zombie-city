package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;

/**
 * Vehicles: police cruisers and army trucks that drive reinforcements from the precinct or base along the
 * roads (running over zombies on the way), fire engines, ordinary traffic and the helicopter that flies in
 * for air support. Cars take damage from zombies, crashes and blasts, and break down when it is too much.
 *
 * <p>((27.3) Fleet itself is the front of the vehicle code: the Vehicle type and its constants, the list of vehicles and
 * the state every part shares, the step (update) that hands each vehicle to the part that drives its kind, damage,
 * names and status lines, and one-line methods passing the rest of the game's calls on. The work is in the parts:
 * FleetRouting (destinations and route searches), FleetDriving (driving along a route: corridors, junctions, queues,
 * collisions), FleetTraffic (everyday cars), FleetPatrols (police patrol cars), FleetService (cars and trucks sent to a
 * call, and wrecks), FleetArmy (troop trucks, base patrols, supply, recon, bulldozers, tanks), FleetHelis, FleetBoats,
 * FleetAircraft, FleetTrains, FleetAmbulances, FleetPickups (survivors' pickups and convoy vehicles), FleetCommand
 * (command post trucks), FleetCrews (who's aboard what), FleetEvac (evacuation buses) and FleetPlayer (you driving).
 * A part reaches the shared state as fleet.x and another part as fleet.part.method(...).)
 */
final class Fleet {
    /** (10.42) Points in a vehicle's planned line ahead. */
    static final int COR = 12;
    /** (Type 4 was the fire engine, gone since 10.49.) */
    static final int CRUISER = 0, TRUCK = 1, HELI = 2, CAR = 3, TANK = 5, AMBULANCE = 6, TRAIN = 7, JET = 8, BOAT = 9;

    /** Aircraft fly over everything: no crashes, no zombies clawing at them. */
    static boolean airborne(Vehicle v) {
        return v.type == HELI || v.type == JET;
    }
    /** (11.0) Not in road traffic: aircraft, trains and boats. */
    static boolean offRoad(Vehicle v) {
        return v.type == HELI || v.type == JET || v.type == TRAIN || v.type == BOAT;
    }
    /** (10.49) How many an evacuation train takes. */
    static final int TRAIN_SEATS = 240;
    /** A train: a locomotive and three carriages, this long in world units. */
    static final float TRAIN_LENGTH = 136;
    static final int WAIT = 0, DRIVE = 1, RETURN = 2, FLY_IN = 3, CIRCLE = 4, FLY_OUT = 5, CRUISE = 6,
            ABANDONED = 7, SPOOL = 9, LAND = 10, ENGAGE = 11, LOAD = 12, BLOCK = 13,
            PATROL = 14, SCENE = 15, RECALL = 16, IDLE = 17, MUSTER = 18, HOVER = 19, STANDBY = 20, DROP = 21, PICKUP = 22;
    static final int[] CAR_COLORS = {0xFFB03A2E, 0xFF2E5FB0, 0xFFE0E0E0, 0xFF222428, 0xFFD4A21C, 0xFF3C8A4E,
            0xFF8A8F96, 0xFF6B2E8A, 0xFFE07A2E};
    // ((17.6) A helicopter can be shot down now: 220, from 1 - nothing could hurt one before.)
    static final float[] MAX_HP = {160, 260, 220, 100, 240, 700, 160, 1, 1, 1};
    /** Most people a car will squeeze in. */
    static final int SEATS = 4;
    /** ((26.1) A Humvee carries six.) */
    static final int HUMVEE_SEATS = 6;
    /**
     * What a truck, police car or helicopter is beyond its basic type (since 10.10): an armoured personnel
     * carrier, a Humvee, a riot police van, a police motorcycle, the police helicopter.
     */
    static final int K_APC = 1, K_HUMVEE = 2, K_RIOT_VAN = 3, K_BIKE = 4, K_POLICE_HELI = 5, K_RESCUE_HELI = 6;
    /** (11.0) The police's armoured BearCat. (8 was the bomb squad's truck, gone in 11.3.) */
    static final int K_BEARCAT = 7;
    /** (14.0) The army engineers' bulldozer: pushes wrecks off the roads in town. */
    static final int K_DOZER = 8;
    /**
     * ((14.6) An infantry fighting vehicle: tracked and heavily armoured, with an autocannon whose rounds burst among
     * the dead; and a gun truck: an open truck with a fast-firing gun that pours long bursts into a horde.)
     */
    static final int K_IFV = 9, K_GUNTRUCK = 10;
    /** ((15.10) Planes at the airport: an airliner taking people out, a military transport flying troops in.) */
    static final int K_AIRLINER = 11, K_TRANSPORT = 12;
    /** ((25.0) A passenger jet in trouble, coming down low over the city to crash. */
    static final int K_CRASH = 13;
    /** ((26.0) Survivors' armed pickup: a gun bolted in the bed. A number well clear of the others' kinds.) */
    static final int K_PICKUP = 20;
    static final int PK_HOME = 0, PK_SORTIE = 1, PK_BACK = 2, PK_CONVOY = 3, PK_ROAM = 4;
    /** ((26.0) A police patrol boat on the water (see Ops), and the police's mobile command post truck. */
    static final int K_POLICE_BOAT = 14, K_COMMAND = 15;
    /** ((26.1) The army's mobile command post truck, kept at its base.) */
    static final int K_ARMY_POST = 16;
    /**
     * ((27.5) The army's MRAP: a mine-resistant armoured truck with a gunner up top, for a squad sent into the worst of
     * it; and its fire support truck: a mortar on the back, kept at base and brought up behind the lines to shell the
     * big packs (see FleetFireSupport). Numbers well clear of the others' kinds.)
     */
    static final int K_MRAP = 30, K_FIRE_SUPPORT = 31;
    /**
     * ((28.7) The quick reaction force's van (type TRUCK): light and quick, five soldiers aboard, kept at its base
     * near the middle of town (see Dispatch.quickReaction).
     */
    static final int K_RAPID_VAN = 32, RAPID_SEATS = 5;
    /** ((31.0) The army's attack helicopter: a gunship with a cannon, sent over the worst of the fighting (FleetHelis.gunships). */
    static final int K_GUNSHIP = 33;
    /** ((27.5) The army's armoured ambulance (type AMBULANCE), parked at its base for the wounded: see FleetMedevac.) */
    static final int K_ARMD_AMB = 17;

    /** ((26.1) A mobile command post truck, the police's or the army's. */
    static boolean isPost(Vehicle v) {
        return v.kind == K_COMMAND || v.kind == K_ARMY_POST;
    }

    /** Kinds of everyday car (which ones are about depends on the country). */
    static final int M_SEDAN = 0, M_PICKUP = 1, M_SUV = 2, M_KEI = 3, M_HATCH = 4, M_VAN = 5, M_TAXI = 6, M_BUS = 7,
            M_BEETLE = 8;
    static final String[] MODEL_NAMES = {"Car", "Pickup", "SUV", "Kei car", "Hatchback", "Van", "Taxi", "Bus", "Beetle"};
    /** Half length and half width of each kind of car. */
    static final float[] MODEL_HL = {8f, 8.8f, 8.4f, 6.2f, 7f, 8.6f, 8f, 15f, 6.8f};
    static final float[] MODEL_HW = {4.4f, 4.6f, 4.8f, 3.8f, 4.2f, 4.6f, 4.4f, 5.6f, 4.2f};

    static final class Vehicle {
        int type, state;
        /** Where it is in the step's grid of who's near whom; set once it has left the fleet. */
        int gi;
        /** ((12.0) Up on a bridge deck (1), on the road near one (0), or near none (-1), as of where and which way it was last asked.) */
        transient int deck = -2;
        transient float deckX, deckY, deckA;
        boolean removedFromFleet;
        /** Seconds stopped; pulling out round something stopped in the lane (seconds left); queued behind traffic. */
        float stillT, passing;
        /**
         * ((29.2) How long it's been crawling (under 8) on and off: it builds while it crawls and drains three times as fast
         * once it's away, so a car that inches forward in a knot of them doesn't start again from nothing each time.
         */
        float knotT;
        /** ((29.2) Times in a row newDestination has found nowhere (FleetRouting.urgent). */
        int pickFails;
        /** ((30.2) Moving up at a scene to where its people have got to (FleetService.moveUp), and until it next looks. */
        boolean moveUp;
        float moveCd = 8;
        /** ((30.8) What it was doing when the player took the wheel: given back to it when they let go (FleetPlayer.handBack). */
        transient int heldState = -1;
        /** ((30.9) Sweeps a base patrol has driven since it left the base. */
        transient int loops;
        transient boolean heldPatrol, heldLoop;
        transient Dispatch.Incident heldIncident;
        transient float heldTx, heldTy;
        /** ((30.6) When its route search was first put off for want of time this step (-1: it wasn't): see FleetRouting.busyFor. */
        transient float deniedAt = -1;
        /** ((27.4) Backing out of a jam or a dead end towards a new way round (seconds left). */
        transient float backT;
        /** ((27.5) Backing straight off a length to undo a knot it's part of, giving the other the room (seconds left). */
        transient float giveWayT;
        /** ((24.0) How long a helicopter has held over its landing spot for the dead round it. */
        float holdT;
        /** How far it has turned without getting any nearer (a car going round in circles), and how long it
         *  then drives carefully, slowly and straight at the middle of each tile of its route. */
        float spin, careful;
        /** Pulled over for lights and sirens coming up behind (or holding at a junction for one): seconds left. */
        float yieldT;
        boolean yieldStop;
        /** Pulled over by highway patrol (stopped on the shoulder); and the car a patrol car has pulled over. */
        boolean pulled;
        Vehicle stopping;
        float stopT, stopCd = 20;
        /** Parked at the kerb at a scene, nose angled in. */
        boolean kerbed;
        /** Queued behind traffic; and somewhere up that queue, someone waiting at a light or a junction. */
        boolean queued, queuedHeld;
        /** How many cars up the queue that someone waiting is (0 none): in a ring of cars it only grows. */
        int holdDepth;
        /** For ordinary cars: what kind of car it is (M_SEDAN and so on). */
        int model;
        /** The tile of its route it is driving along, and the route that belongs to. */
        int pathT = -1;
        Route pathField;
        /** At junctions: the one it has been let through, the one it is waiting at and for how long. */
        int clearedJ = -1, waitJ = -1;
        /** ((12.0) Where it leaves junction exitJ (a tile, -1 none), as worked out from tile exitFrom of route exitRoute.) */
        int exitJ = -1, exitFrom = -1, exitTile = -1;
        Route exitRoute;
        float waitT;
        boolean held;
        /** The way along the road it was last heading (one step of its route). */
        int pdx, pdy;
        /** (10.42) The line it's about to drive (in its lane, through the junctions): {x, y} points, from a tile back on. */
        final float[] cor = new float[2 * COR];
        int corN;
        /** (10.49) What that line was worked out from (route, tile, heading), its tiles, and where the tile is among them. */
        Route corOf;
        int corOfT = -1, corOfDx, corOfDy, corOfPi;
        final int[] corTs = new int[COR];
        /** ((24.0) The junction ahead (-1 none), its tile and the way in, and what that was worked out from (see junctionRule). */
        Route jaOf;
        int jaFrom = -1, jaT = -1, jaDx, jaDy, jaId = -1, jaTile, jaDirX, jaDirY;
        /** (10.45) Until it next looks out for the dead on its way. */
        float spotCd;
        /** Where it's steering for on that line, and whether it is. */
        float aimX, aimY;
        boolean aiming;
        float x, y, angle, speed, timer, stuckTimer, lastDist, gunCd, soundCd, circle;
        /** It went round in a circle (missed its turn); traffic gives up that route. */
        boolean orbited;
        /** ((21.3) Its crew out on foot this frame (see onFoot), and which frame that was. */
        final ArrayList<Entity> foot = new ArrayList<Entity>();
        int footFrame = -1;
        /** A SWAT team's armoured van (a truck carrying police). */
        boolean swat;
        /** Which kind of truck, police car or helicopter (K_APC and so on; 0 the usual). */
        int kind;
        /** A patrol out from the base and back: the crew stay aboard unless they're needed. */
        boolean loop;
        /** How long a car on its way home has been holding back from the dead on the road ahead. */
        float hold;
        /** Rounds fired by the door gunner (it fires in short bursts). */
        int burst;
        float tx, ty, homeX, homeY;
        Route field;
        int passengerType, passengers;
        Dispatch.Incident incident;
        /** (10.56) A patrol car at a call that's over: the next call its officers are sent to, to drive them there. */
        Dispatch.Incident onward;
        /** (10.60) A patrol car taking its officers to restock: where it's driving them (a station or an armoury). */
        City.Facility ammoRun;
        /** (10.60) A helicopter putting troops down: seconds to the next look at its landing zone, and wave-offs so far. */
        float lzCheck, holdSaidCd;
        int lzTries;
        /** (11.0) Picking up officers or soldiers to drive them to this call (see driveTo). */
        Dispatch.Incident pickupFor;
        Dispatch.SafeZone zone;
        String place;
        /** Lights flash on cruisers; the helicopter's rotor spins. */
        float anim;
        int color;
        boolean parked;
        /** (10.48) A patrol car taken over by other officers: back on patrol once they're aboard. */
        boolean wasPatrol;
        float reclaimCd;
        /** Damage: smoking below half, broken down (and maybe burning) at zero. */
        float hp, maxHp, crashCd, hailCd, waitTimer;
        boolean broken, burnt;
        /** People who flagged the car down and jumped in. */
        final ArrayList<Entity> riders = new ArrayList<Entity>();
        /** Driving the riders to a safe zone. */
        boolean rescue;
        /** ((17.2) A car a gang has stolen, with its crew aboard: which gang (-1 none). */
        int number;
        /** ((26.0) An armed pickup's base (null if it roams), what it's doing, its timer, its load of rounds, and the base it's taking them to. */
        SurvivorBases.Base pbase, ptarget;
        /** ((26.0) The convoy column it's part of, what it is in it (Convoys.V_*), whether it has its way planned, and when to try again. */
        Convoys.Column convoy;
        int convoyRole;
        boolean convoyRouted;
        float routeCd;
        int pmode;
        float pTimer, pCargo;
        /** Helicopter height (0 on the pad, 1 cruising) and how hard it is banking. */
        float alt, bank;
        /** The helicopter took off from (and lands back on) a helipad, rather than flying in from the edge. */
        boolean pad;
        /** (10.49) An evacuation train has had its departure on the news. */
        boolean said;
        /** Tanks: where the turret points and the cannon's reload. */
        float turret, cannonCd, idleTimer;
        /** Ambulances: the person they came for. */
        Entity patient;
        /** An army truck carrying the National Guard. */
        boolean guardUnit;
        /** Bringing reinforcements in from outside the city (10.17): tagged on the map. */
        boolean reinforcement;
        /**
         * A police car or army truck that has dropped its people off (10.24): it drives back to the nearest police
         * station or base and parks there (standby) until it's needed for the next call, or until its people are
         * done and need picking up (pickup: it drives out, waits for them to climb in, and brings them back).
         */
        boolean parkHome;
        /** ((27.5) An ambulance out for the wounded (see FleetMedevac), and how far through the pickup it is. */
        boolean medevac;
        int medPhase;
        /** Taking outside help home again (10.27). */
        boolean leaving;
        float stageCd;
        /** The station or base it parks at. */
        City.Facility depot;
        /** The people it dropped off. */
        final ArrayList<Entity> squad = new ArrayList<Entity>();
        /** A fire engine's crew is already off fighting the fire. */
        /**
         * The real people aboard: a fire crew or two patrol officers. They're out of the world while they ride,
         * get out at the scene (and stay its crew), and walk back to it before it leaves.
         */
        final ArrayList<Entity> crew = new ArrayList<Entity>();
        /**
         * (11.3) Who's at the wheel of a police car, army truck or ambulance out on the road with nobody else
         * aboard (on its way to pick people up, or back from dropping them off): a real person, not an empty
         * seat. They go inside once it's parked back home, and get out if it's wrecked.
         */
        Entity driver;
        /** ((11.7) The driver is one of the city's own officers or soldiers, taken from the station or base, who goes
         * back to it when the vehicle is parked home again.) */
        boolean realDriver;
        /** ((11.7) Out to pick people up: seconds until it looks again for where they actually are.) */
        float aimCd;
        /** A patrol car (not a backup cruiser): drives a beat and answers calls. */
        boolean patrol;
        /** Whose car it is: 0 the city police, 1 highway patrol, 2 the sheriff, 3 the park rangers. */
        int agency;
        /** The rescue helicopter: its team on the ground, calling them back up, and where it flies home to. */
        ArrayList<Entity> team;
        boolean recall;
        /** The rescue helicopter: back at base waiting for its team's call, or on its way to pick them up. */
        boolean standby, pickup;
        float baseX, baseY;
        /**
         * A helicopter's own helipad (10.20): every aircraft is parked on one from the start, its crew standing by
         * it, and comes back to it. Its flight crew (the pilot, and the rescue team), whether it's in the air
         * yet, its velocity (it can move any way it likes, not just nose first), and time to refuel.
         */
        float[] padAt;
        final ArrayList<Entity> flight = new ArrayList<Entity>();
        float hvx, hvy, refuel, spoolTime = 1;
        /** Air-assault troops still aboard to put down, and the incident they're for. */
        int troops;
        /** Flying cover for a convoy coming in. */
        Dispatch.Convoy escort;
        /** The station it belongs to (fire engines and patrol cars), and how many it waits to take aboard. */
        City.Facility station;
        int crewWanted, crewSpawns;
        /** ((13.7) While mustering: how many of those it's waiting for are still on their way to it, counted each second.) */
        transient int coming = -1;
        /** ((13.7) And how far off the nearest of them is.) */
        transient float comingNear;
        /** (11.1) Which lane it keeps to on a road with more than one each way (0 the inside one). */
        int lane;
        /** (11.0) A reserve pilot on their way to fly it. */
        Entity pilotComing;
        /** (11.0) An army recon vehicle (roams, reports, doesn't stop), and how long since it last radioed a horde in. */
        boolean recon;
        float reconSaid = -99, reconCd;
        /** (11.0) A landing boat's way in over the water (tiles, edge to shore) and how far along it is. */
        int[] boatPath;
        int boatAt;
        float checkCd, quiet;
        /**
         * ((26.0) A police boat: what it's doing (Ops.J_*), its number, the landing it's making for (or last left from),
         * its home landing, and the officers it's waiting to take across. And on anything: a roof gun of its own, with a
         * gunner (see mountedGun) - set it on a vehicle that should have one.
         */
        transient int boatJob, boatNum;
        transient float landX, landY, homeLandX, homeLandY, jobT;
        transient Entity waitFor;
        transient int[] boatPath2;
        transient ArrayList<Entity> waiting;
        boolean gunMount;

        /**
         * Whether anyone is inside for the dead to go after: a driver in moving traffic, the crew of an engine
         * or patrol car, riders, a tank or ambulance crew, or you at the wheel. An empty or abandoned vehicle
         * isn't worth their trouble.
         */
        boolean occupied() {
            if (player != null || !riders.isEmpty() || !crew.isEmpty() || passengers > 0 || driver != null) return true;
            // ((11.3) Parked up ready at the station or base: nobody in it.)
            if (parked || broken || standby) return false;
            if (type == CRUISER) return !patrol;
            return type == CAR || type == TANK || type == AMBULANCE || type == TRUCK;
        }

        /** Whether the roof lights are going (not on a quiet patrol, or parked at the station). */
        /** (10.32) The checkpoint it was last waved through, and how long it's waited at one. */
        int cleared = -1;
        float cpWait;
        /** (10.32) Fuel in the tank (0 to 1); the pump it's heading for (-1 none), and time there. */
        float fuel = 1, refuelT, refuelCd;
        int pumpAt = -1;

        boolean lightsOn() {
            if (broken) return false;
            // ((10.51) Only on a call: on the way to one, at the scene, or pulling someone over. Not on the rounds,
            // driving back to the station or parked.)
            if (type == CRUISER) return stopping != null || state == SCENE || (state == DRIVE && (incident != null || !patrol));
            if (type == AMBULANCE) return state != WAIT;
            return false;
        }
        /** A parked car's alarm going off (seconds left) after zombies bumped into it. */
        float alarm;
        /** ((26.0) The dead clawing at it (see World.clawAt): time since it was last looked at, how far they've got into
         *  it, how many were on it. */
        transient float clawAcc, breakT;
        transient int clawN;
        /** ((22.0) Seconds until its engine next carries to the dead round about; its alarm has gone off already. */
        transient float engineCd;
        transient boolean alarmed;
        /** ((22.0) An armoured police van parked as a shelter: people running from the dead climb in. */
        boolean shelter;
        /** ((11.5) Off screen, whether this step is the one it sits out.) */
        int lodTick;
        /** Police roadblocks: the safe zone this car is closing a road for, which way that road runs, cones out,
         *  and the officer posted beside it. */
        Dispatch.SafeZone block;
        int blockDir;
        boolean cones;
        Entity guard;
        /**
         * ((14.8) The crowd barriers put up across the road and pavements beside a roadblock: where the line runs,
         * which side is the zone's, and how much more pushing it'll take before the dead have it down.)
         */
        float barX0, barY0, barX1, barY1, barNx, barNy;
        int barIn;
        float barHp;
        boolean barDown;
        /** ((14.8) A police helicopter spotting from above: when it next looks, moves on to the worst of it, and calls it in.) */
        float heliLook, spotMove, spotSaid;
        /** A supply truck: rounds of ammunition it's carrying to a safe zone. */
        int supply;
        /**
         * ((13.9) An ammo truck: the rounds it's carrying for soldiers at a fight, who fill up from it there instead
         * of going back to the base.)
         */
        int cargo;
        boolean ammoTruck;
        /** ((16.7) Times an ammo truck held up on its way has planned its way again, rather than stopping there. */
        int jamTries;
        /** ((28.3) Times help on its way to a call, held up far short of it, has looked for another way past (FleetService). */
        int detours;
        /** ((28.6) Waiting to pick its people up: the nearest of them coming, and how long since anyone got nearer. */
        float pickNear, pickStall;
        /** ((32.0) Times in a row it has stopped to pick its squad up and come away with nobody (FleetService.truckAtScene). */
        int pickTries;
        /** ((14.0) A bulldozer: the wreck it's on its way to push off the road, its base, and the seconds left pushing.) */
        Vehicle clearing;
        City.Facility dozerBase;
        /** A supply truck from outside, once it's over (10.19): restocking this store or armoury, then off the map. */
        boolean restock;
        City.Building restockShop;
        City.Facility restockFac;
        /** The player's character, at the wheel. */
        Entity player;
        /** A stopped car rolls over to the kerb. */
        float pullX, pullY;
        boolean pulling;
        /** ((28.0) Pulling off the lane to a spot found by FleetDriving.offLane (a verge isn't somewhere it drives). */
        transient boolean pullOff;
        /** (11.3) Already put by the kerb (or tried) since it last stopped. */
        boolean pullTried;
        /** Getting out of town now everyone knows (runs red lights once it's clear, drives faster). */
        boolean fleeing;
        /** (10.48) Taking people out of the city (they're gone with it at the edge of the map). */
        boolean exodus;
        /** (10.49) An evacuation bus: the stop it's for (or -1), and how far along it is (to the stop, there, leaving). */
        int evacStop = -1, busPhase;
        /** Through traffic on the highway: leaves the map at the far end. */
        boolean through;
        /** After failing to find somewhere to go (or a way out), a while before trying again: routes are costly. */
        float destCd, fleeCd, rerouteCd;

        /**
         * Half its length, as it's drawn (VehicleRenderer's hl). ((28.0) Not its whole length, as the collisions took it:
         * two cars sixteen long were pushed apart only once their centres were under ten apart, so a queue or a knot was
         * drawn as cars driven into one another. See FleetDriving.extent.)
         */
        float length() {
            if (kind == K_APC) return 11f;
            if (kind == K_HUMVEE) return 8.5f;
            if (kind == K_RAPID_VAN) return 8.8f;
            if (kind == K_MRAP || kind == K_FIRE_SUPPORT) return 10.5f;
            if (kind == K_BIKE) return 4.5f;
            return type == TANK ? 11f : type == TRUCK || type == AMBULANCE ? 9.5f
                    : type == CAR ? MODEL_HL[model] : 8f;
        }

        /** ((28.0) Half its width, as it's drawn (near enough VehicleRenderer's hw). */
        float halfWidth() {
            if (kind == K_BIKE) return 2f;
            if (kind == K_APC || kind == K_MRAP || kind == K_FIRE_SUPPORT || type == TANK) return 5.6f;
            if (kind == K_HUMVEE) return 5.4f;
            return type == TRUCK ? 5.2f : type == AMBULANCE ? 4.9f : type == CAR ? MODEL_HW[model] : 4.4f;
        }
    }

    /** What a vehicle is, for its card: "Police car - Unit 14 (Highway Patrol)", "Taxi", "Engine 2"... */
    String describe(Vehicle v) {
        switch (v.type) {
            case CRUISER:
                if (v.kind == K_BIKE) return "Police motorcycle  -  " + patrols.callsign(v);
                if (v.agency == 3) return "Park rangers' truck  -  " + patrols.callsign(v);
                String car = "Police car";
                return v.number > 0 ? car + "  -  " + patrols.callsign(v) : car;
            case TRUCK:
                if (v.kind == K_APC) return "Armoured personnel carrier" + (v.guardUnit ? "  -  " + Country.guard()[1] : "");
                if (v.kind == K_IFV) return "Infantry fighting vehicle";
                if (v.kind == K_GUNTRUCK) return "Army gun truck";
                if (v.kind == K_MRAP) return "Army MRAP  -  mine-resistant, a gunner up top";
                if (v.kind == K_FIRE_SUPPORT) return "Army fire support truck  -  mortar carrier";
                if (v.kind == K_HUMVEE) return (v.guardUnit ? Country.guard()[1] + " " : "") + "Humvee" + (v.loop ? "  -  base patrol" : "");
                if (v.shelter) return "Armoured police van  -  shelter, " + v.riders.size() + " of " + SHELTER_SEATS + " inside";
                if (v.kind == K_RIOT_VAN) return "Riot police van";
                if (v.kind == K_RAPID_VAN) return "Quick reaction force van";
                if (v.kind == K_BEARCAT) return "Police BearCat";
                if (v.kind == K_DOZER) return "Army engineers' bulldozer";
                if (v.kind == K_COMMAND) return "Police mobile command post";
                if (v.kind == K_ARMY_POST) return "Army mobile command post";
                return v.swat ? Country.swat()[0] + " van" : v.guardUnit ? Country.guard()[1] + " truck" : v.supply > 0 ? "Army supply truck" : v.ammoTruck ? "Army ammo truck  -  " + v.cargo + " rounds" : "Army truck";
            case HELI: return v.kind == K_RESCUE_HELI ? "Rescue helicopter" : v.kind == K_POLICE_HELI ? "Police helicopter"
                    : v.kind == K_GUNSHIP ? "Gunship " + Math.max(1, v.number) + "  -  army attack helicopter" : "Air " + Math.max(1, v.number) + "  -  army helicopter";
            case TANK: return "Tank";
            case AMBULANCE: return v.kind == K_ARMD_AMB ? "Army armoured ambulance" : "Ambulance";
            case TRAIN: return "Train";
            case JET: return "Jet";
            case BOAT: return v.kind == K_POLICE_BOAT ? "Police boat  -  Marine " + v.boatNum : v.evacStop >= 0 ? "Evacuation boat" : "Landing craft";
            default:
                return MODEL_NAMES[v.model] + (v.through ? "  -  through traffic" : v.fleeing ? "  -  leaving town" : "");
        }
    }

    /** A short line about what a vehicle is doing, shown above it on the map. */
    String status(Vehicle v) {
        String where = v.place != null ? v.place : null;
        // ((27.5) Being mended at home.)
        String fix = FleetMedevac.repairing(v);
        if (fix != null) return describe(v).split("  -  ")[0] + ": " + fix;
        if (v.kind == K_POLICE_BOAT || isPost(v)) return w.ops.status(v);
        switch (v.type) {
            case HELI:
                if (v.state == IDLE) {
                    String who = v.kind == K_RESCUE_HELI ? "Rescue helicopter" : v.kind == K_POLICE_HELI ? "Police helicopter" : v.kind == K_GUNSHIP ? "Gunship " + v.number : "Air " + v.number;
                    return who + ": " + (v.refuel > 0 ? "Refuelling on the pad (" + (int) v.refuel + "s)" : helis.livePilot(v) == null ? "Grounded, no pilot" : "On the pad, ready");
                }
                if (v.state == MUSTER) return (v.kind == K_RESCUE_HELI ? "Rescue helicopter" : v.kind == K_POLICE_HELI ? "Police helicopter" : "Air " + v.number)
                        + ": Crew boarding (" + v.crew.size() + "/" + v.crewWanted + ")";
                if (v.kind == K_RESCUE_HELI) {
                    switch (v.state) {
                        case FLY_IN: return "Rescue helicopter: En route" + (where != null ? " to " + where : "");
                        case HOVER: return v.team == null ? "Rescue helicopter: Coming into a hover" : v.pickup ? "Rescue helicopter: Winching everyone up (" + v.passengers + " aboard)"
                                : "Rescue helicopter: Lowering the team";
                        case STANDBY: return "Rescue helicopter: At base, waiting for the team's call";
                        default: return v.passengers > 0 ? "Rescue helicopter: " + v.passengers + " aboard, back to base" : "Rescue helicopter: Returning";
                    }
                }
                if (v.kind == K_POLICE_HELI) {
                    switch (v.state) {
                        case FLY_IN: return "Police helicopter: En route" + (where != null ? " to " + where : "");
                        case CIRCLE: return "Police helicopter: Marksman engaging (" + (int) v.timer + "s)";
                        default: return "Police helicopter: Returning";
                    }
                }
                String air = "Air " + Math.max(1, v.number);
                switch (v.state) {
                    case SPOOL: return air + ": Starting up";
                    case FLY_IN: return v.alt < 0.9f ? air + ": Taking off" : air + ": En route" + (where != null ? " to " + where : "");
                    case DROP: return air + ": Putting troops down";
                    case CIRCLE: return v.escort != null ? air + ": Flying cover for the convoy" : air + ": Door gunners engaging (" + (int) v.timer + "s)";
                    case LAND: return air + ": Landing";
                    default: return air + ": Returning to base";
                }
            case TRAIN:
                if (v.state == WAIT) return "Train: At the platform (" + (int) v.timer + "s)";
                if (!v.pad && city.stationX >= 0) return "Train: Stopping at " + city.stationName;
                return "Train: Passing through";
            case JET:
                return v.timer > 0 ? "Jet: Bombs away!" : "Jet: Attack run";
            case TANK:
                if (v.broken) return "Tank: Knocked out";
                switch (v.state) {
                    case WAIT: return "Tank: Starting engine";
                    case DRIVE: return "Tank: Moving to " + (where != null ? where : "contact");
                    case ENGAGE: return v.cannonCd > 2.5f ? "Tank: Firing!" : "Tank: Engaging";
                    default: return "Tank: Returning to base";
                }
            case AMBULANCE:
                if (v.medevac) {
                    String who = v.kind == K_ARMD_AMB ? "Armoured ambulance" : "Ambulance";
                    if (v.broken) return who + ": Wrecked";
                    if (v.medPhase == FleetMedevac.MP_NONE) return who + ": Parked, ready";
                    if (v.medPhase == FleetMedevac.MP_TO) return who + ": On the way to the wounded" + (where != null ? " on " + where : "");
                    if (v.medPhase == FleetMedevac.MP_LOAD) return who + ": Taking the wounded aboard";
                    if (v.medPhase == FleetMedevac.MP_CARE) return who + ": " + v.riders.size() + " wounded aboard, to care";
                    return who + ": Returning";
                }
                if (v.broken) return "Ambulance: Broken down";
                // ((12.0) Parked at its station: it said "Responding".)
                if (v.parked || v.standby) return "Ambulance: Parked, ready";
                if (v.state == RETURN) return v.riders.isEmpty() ? "Ambulance: Returning" : "Ambulance: Patient on board";
                if (v.state == LOAD) return "Ambulance: Loading patient";
                return "Ambulance: Responding";
            case CRUISER:
            case TRUCK: {
                if (v.kind == K_FIRE_SUPPORT) return fireSupport.status(v, where);
                String who = v.kind == K_MRAP ? "MRAP" : v.kind == K_APC ? "APC" : v.kind == K_HUMVEE ? "Humvee" : v.kind == K_RAPID_VAN ? "QRF van" : v.kind == K_RIOT_VAN ? "Riot van"
                        : v.kind == K_BIKE ? "Police motorcycle" : v.kind == K_BEARCAT ? "BearCat" : v.type == CRUISER ? "Police car" : v.swat ? "SWAT van" : "Army truck";
                if (v.restock) return v.state == RETURN ? "Supply truck: Heading out of town"
                        : "Supply truck: Delivering" + (where != null ? " to " + where : "");
                // ((12.0) A wreck is a wreck, whatever it was doing: it went on saying "Responding" or "On patrol".)
                if (v.broken) return who + ": Wrecked";
                // ((24.0) An APC or troop truck standing by its squad at a fight isn't parked at base.)
                if (v.type == TRUCK && v.state == SCENE) return who + ": Fire support" + (where != null ? " at " + where : "");
                if (v.standby) return who + ": Parked at " + (v.depot != null ? v.depot.name : "base") + ", ready";
                // ((12.0) Left where it stopped, or parked, or off to restock: it said "Returning" or "Responding".)
                if (v.state == ABANDONED) return who + ": Left where it stopped, nobody aboard";
                if (v.recon) return who + ": Recon sweep" + (where != null ? " near " + where : "");
                if (v.ammoRun != null && v.state == DRIVE) return who + ": Off to " + v.ammoRun.name + " to restock ammo";
                if (v.parked && v.crew.isEmpty() && v.driver == null) return who + ": Parked";
                if (v.state == PICKUP) return who + ": Picking up its " + (v.passengerType == Entity.SOLDIER ? "squad" : "officers");
                if (v.pickup && v.state == DRIVE) return who + ": Going to pick up its " + (v.passengerType == Entity.SOLDIER ? "squad" : "officers")
                        + (v.driver != null && v.realDriver ? ", " + Names.person(v.driver.nameSeed) + " driving" : "");
                // ((12.0) Waiting at the station or base while its crew climb in: it said "Returning".)
                if (v.state == MUSTER) return who + (v.type == CRUISER ? ": Officers getting in" : policeTruck(v) ? ": Officers mounting up" : ": Squad mounting up");
                if (v.parkHome && v.state == RETURN) return who + ": Back to " + (v.depot != null ? v.depot.name : "base") + (v.crew.isEmpty() ? "" : " with " + v.crew.size() + " aboard");
                if (v.type == CRUISER && v.state == SCENE && !v.patrol) return who + ": At the scene" + (where != null ? " on " + where : "");
                if (v.type == TRUCK && v.state == SCENE) return who + ": Fire support" + (where != null ? " at " + where : "");
                if (v.loop && v.state == DRIVE) return who + ": Patrolling" + (where != null ? " to " + where : "");
                if (v.state == BLOCK) return "Police car: Roadblock" + (where != null ? " on " + where : "");
                if (v.block != null && v.state == DRIVE) return "Police car: Setting up a roadblock" + (where != null ? " on " + where : "");
                if (v.patrol && v.state == PATROL) return who + ": On patrol";
                if (v.patrol && v.state == SCENE) return who + ": At the scene" + (where != null ? " on " + where : "");
                if (v.patrol && v.state == RECALL) return who + ": Picking up its officers";
                if (v.state == WAIT) return who + ": Loading up";
                if (v.state == DRIVE) return who + ": " + (army(v) ? "Carrying squad" : "Responding") + (where != null ? " to " + where : "");
                return who + ": Returning";
            }
            default:
                // ((26.0) The survivors' armed pickups.)
                if (v.kind == K_PICKUP && !v.broken && !v.parked) {
                    String who = v.pbase != null ? v.pbase.name + "'s armed pickup" : "Armed pickup (survivors)";
                    return who + ": " + (v.pmode == PK_HOME ? "standing by at the gate" : v.pmode == PK_SORTIE ? "out after the dead"
                            : v.pmode == PK_BACK ? "heading back" : v.pmode == PK_CONVOY ? "supply run to " + (v.ptarget != null ? v.ptarget.name : "another base")
                            : "hunting the dead") + ", " + v.crew.size() + " aboard";
                }
                if (v.broken) return v.burnt ? "Burnt out" : "Broken down";
                if (v.state == ABANDONED) return "Abandoned";
                if (v.rescue) return "Driving " + v.riders.size() + " to safety";
                if (v.waitTimer > 0) return "Picking someone up";
                if (!v.riders.isEmpty()) return v.riders.size() + " on board";
                return null;
        }
    }

    final World w;
    final City city;
    final ArrayList<Vehicle> vehicles = new ArrayList<Vehicle>();
    int unitCount;
    /** ((27.4) The vehicles came back from a save (VehicleSave): the stations aren't stocked with cars afresh on top of them.) */
    boolean fromSave;
    /** How many patrol cars the police keep on the streets. */
    int patrolTarget;
    /** ((11.4) Patrol cars on the streets, counted once a frame.) */
    int patrolCount;
    float staffCd;

    Fleet(World w) {
        this.w = w;
        this.city = w.city;
        routing = new FleetRouting(this);
        driving = new FleetDriving(this);
        cars = new FleetTraffic(this);
        patrols = new FleetPatrols(this);
        service = new FleetService(this);
        armyUnits = new FleetArmy(this);
        helis = new FleetHelis(this);
        boats = new FleetBoats(this);
        aircraft = new FleetAircraft(this);
        trains = new FleetTrains(this);
        ambulances = new FleetAmbulances(this);
        medevac = new FleetMedevac(this);
        convoyUnits = new FleetPickups(this);
        commandTrucks = new FleetCommand(this);
        fireSupport = new FleetFireSupport(this);
        crews = new FleetCrews(this);
        evacBuses = new FleetEvac(this);
        playerDriving = new FleetPlayer(this);
    }

    // ((27.3) The parts the vehicle code is in: Fleet keeps the vehicles themselves, what every part shares, and the
    // step that hands each vehicle to the part that drives it.)
    final FleetRouting routing;
    final FleetDriving driving;
    final FleetTraffic cars;
    final FleetPatrols patrols;
    final FleetService service;
    final FleetArmy armyUnits;
    final FleetHelis helis;
    final FleetBoats boats;
    final FleetAircraft aircraft;
    final FleetTrains trains;
    final FleetAmbulances ambulances;
    final FleetMedevac medevac;
    final FleetPickups convoyUnits;
    final FleetCommand commandTrucks;
    final FleetFireSupport fireSupport;
    final FleetCrews crews;
    final FleetEvac evacBuses;
    final FleetPlayer playerDriving;

    void clear() {
        vehicles.clear();
        shelters.clear();
    }

    /** (11.1) The army's (tanks, trucks, Humvees, its helicopters and boats) rather than the police's or anyone else's. */
    static boolean army(Vehicle v) {
        if (v.type == TANK || v.type == JET || (v.type == BOAT && v.kind != K_POLICE_BOAT)) return true;
        if (v.type == HELI) return v.kind != K_POLICE_HELI && v.kind != K_RESCUE_HELI;
        return v.type == TRUCK && !policeTruck(v);
    }

    /** ((26.0) Nobody gets into it through a window: tanks, APCs, IFVs, BearCats, SWAT and riot vans, the shelter van. */
    static boolean armoured(Vehicle v) {
        return v.type == TANK || v.kind == K_APC || v.kind == K_IFV || v.kind == K_BEARCAT || v.kind == K_RIOT_VAN || v.kind == K_MRAP || v.swat || v.shelter;
    }

    /** (11.1) The police's trucks: SWAT vans, riot vans and BearCats. */
    static boolean policeTruck(Vehicle v) {
        return v.type == TRUCK && (v.swat || v.kind == K_RIOT_VAN || v.kind == K_BEARCAT || v.kind == K_COMMAND);
    }

    /** (11.1) The lane a vehicle keeps to: on a call, the inside one, to get by. */
    static int laneOf(Vehicle v) {
        return v.lightsOn() ? 0 : v.lane;
    }

    Vehicle make(int type) {
        Vehicle v = new Vehicle();
        v.type = type;
        // ((11.1) A third keep to the inside lane, the rest (and every truck and bus) to the outside.)
        v.lane = type == TRUCK || type == TANK ? 2 : w.rnd.nextInt(3);
        v.hp = v.maxHp = MAX_HP[type];
        // (10.32) Ordinary cars set off with anything from a third of a tank to a full one.
        v.fuel = type == CAR ? 0.3f + w.rnd.nextFloat() * 0.7f : 1;
        return v;
    }

    /** (10.32) How far a full tank takes an ordinary car (world units). */
    static final float CAR_RANGE = 22000;
    /** Fuel left in each gas pump (in full tanks), and the radio about pumps running dry. */
    float[] pumpFuel;
    boolean drySaid;
    int ranDry;

    /** Makes a truck, police car or helicopter into one of its other kinds (K_APC...). */
    void setKind(Vehicle v, int kind) {
        v.kind = kind;
        float hp = kind == K_APC ? 520 : kind == K_HUMVEE ? 220 : kind == K_RAPID_VAN ? 240 : kind == K_RIOT_VAN ? 380 : kind == K_BIKE ? 70 : kind == K_BEARCAT ? 600
                : kind == K_IFV ? 800 : kind == K_GUNTRUCK ? 260 : kind == K_MRAP ? 700 : kind == K_FIRE_SUPPORT ? 320 : kind == K_COMMAND || kind == K_ARMY_POST ? 900 : kind == K_POLICE_BOAT ? 300 : v.maxHp;
        v.hp = v.maxHp = hp;
    }

    /**
     * ((26.0) Whether a vehicle has a gun mounted on it with a gunner: the APC, IFV, gun truck and Humvee as before, and
     * now the army's troop trucks, the police's BearCats, and the police boats (the officers aboard
     * shooting from it). Anything else opts in with gunMount (a survivors' pickup: v.gunMount = true), or a kind added here.
     */
    static boolean mountedGun(Vehicle v) {
        if (v.gunMount) return true;
        switch (v.kind) {
            // ((26.1) Not riot vans: no gun on their roof.)
            case K_APC: case K_HUMVEE: case K_IFV: case K_GUNTRUCK: case K_BEARCAT: case K_POLICE_BOAT: case K_MRAP:
                return true;
            case 0:
                return troopTruck(v);
            default:
                return false;
        }
    }

    /** ((26.0) An army truck carrying a squad (not a supply or ammo truck, the SWAT van or one from outside with stock). */
    static boolean troopTruck(Vehicle v) {
        return v.type == TRUCK && v.kind == 0 && !v.swat && !v.restock && v.supply <= 0 && !v.ammoTruck;
    }

    /** ((26.0) A roof gun drawn on top of a vehicle that isn't built round one (see GameView.drawVehicle). */
    static boolean addedGun(Vehicle v) {
        return mountedGun(v) && v.kind != K_APC && v.kind != K_IFV && v.kind != K_GUNTRUCK && v.kind != K_HUMVEE && v.kind != K_MRAP && v.type != BOAT;
    }

    /**
     * ((26.0) Someone up on the gun: always on the vehicles built round it (as before); on the others only out on a job
     * with somebody aboard or at the scene - not parked at the station or base, sheltering people, or left in the road.
     */
    static boolean gunManned(Vehicle v) {
        if (v.broken) return false;
        if (v.kind == K_APC || v.kind == K_IFV || v.kind == K_GUNTRUCK || v.kind == K_HUMVEE || v.kind == K_MRAP) return true;
        if (v.parked || v.standby || v.shelter || v.state == ABANDONED || v.state == MUSTER || v.player != null) return false;
        return v.type == BOAT || v.convoy != null || !v.crew.isEmpty() || v.driver != null || v.passengers > 0 || v.state == SCENE;
    }

    /** ((26.0) How far a vehicle's mounted gun reaches, and how hard each round hits. */
    static float gunRange(Vehicle v) {
        switch (v.kind) {
            case K_IFV: return 240;
            case K_GUNTRUCK: return 210;
            case K_APC: return 170;
            case K_MRAP: return 180;
            case K_BEARCAT: case K_POLICE_BOAT: return 160;
            case K_HUMVEE: return 140;
            case K_RIOT_VAN: return 130;
            default: return 150;
        }
    }

    static float gunDamage(Vehicle v) {
        switch (v.kind) {
            case K_IFV: return 40;
            case K_GUNTRUCK: return 15;
            case K_APC: return 24;
            case K_MRAP: return 20;
            case K_BEARCAT: return 16;
            case K_RIOT_VAN: return 12;
            case K_POLICE_BOAT: return 11;
            default: return 18;
        }
    }

    /**
     * The mounted gun of an APC, IFV, gun truck or Humvee - and ((26.0) of anything mountedGun says has one): short
     * bursts at the nearest of the dead in reach, the turret turning to them.
     */
    void turret(Vehicle v, float dt) {
        if (!mountedGun(v) || !gunManned(v)) return;
        v.gunCd -= dt;
        if (v.gunCd > 0) return;
        Entity z = w.nearestZombie(v.x, v.y, gunRange(v));
        if (z == null) {
            v.gunCd = 0.6f;
            // ((26.0) Nothing to shoot: the gun swings back round to the front.)
            if (v.kind != K_APC && v.kind != K_IFV) v.turret = turnTo(v.turret, v.angle, 0.5f);
            return;
        }
        v.turret = (float) Math.atan2(z.y - v.y, z.x - v.x);
        if (w.turretShot(v.x, v.y, z, gunDamage(v))) {
            v.burst++;
            shotsFrom++;
            if (v.kind == K_IFV) {
                // ((14.6) Each round bursts: the dead round it are hit too. Three at a time, a breath between.)
                w.cannonSplash(z.x, z.y, 16, 30);
                v.gunCd = v.burst % 3 == 0 ? 1.4f : 0.45f;
            } else if (v.kind == K_GUNTRUCK) {
                // ((14.6) Long bursts, very fast: twenty rounds, then a moment to let the barrels cool.)
                v.gunCd = v.burst % 20 == 0 ? 1.6f : 0.05f;
            } else if (v.kind == K_RIOT_VAN) {
                // ((26.0) The riot van's gunner: aimed shots, a few at a time.)
                v.gunCd = v.burst % 4 == 0 ? 1.2f : 0.3f;
            } else if (v.kind == K_POLICE_BOAT) {
                // ((26.0) The officers aboard, each with their own rifle: the more of them, the quicker the shots.)
                v.gunCd = 0.9f / (2 + v.crew.size());
            } else v.gunCd = v.burst % 5 == 0 ? 1.1f : 0.14f;
        } else v.gunCd = 0.4f;
    }
    /** ((26.0) Rounds fired from mounted guns, all told (for the tests). */
    int shotsFrom;

    /**
     * A car on the highway: anywhere along its carriageway (at the start), or coming on at the far end of it.
     * Most carry on through and off the other end; some come off at an interchange, into town.
     */
    /** (10.32) The evacuation: cars still coming, and whether it has started. */
    int evacLeft;
    boolean evacStarted;
    float evacTimer;

    static final class Route {
        int[] keys;
        /**
         * ((16.4) The distances, two bytes each - four only for a route that needs it, and few do: the routes the
         * vehicles carry were the biggest thing in a Large map's memory after the map itself.)
         */
        private char[] vals16;
        private int[] vals32;
        private int mask, size;

        Route(int entries) {
            // ((13.3) Up to three quarters full, not half: a route was a quarter of a Colossal map's memory.)
            int n = 16;
            while (n * 3 < entries * 4) n <<= 1;
            keys = new int[n];
            vals16 = new char[n];
            java.util.Arrays.fill(keys, -1);
            mask = n - 1;
        }

        private void grow() {
            int[] ok = keys, ov32 = vals32;
            char[] ov16 = vals16;
            keys = new int[ok.length * 2];
            if (ov32 != null) vals32 = new int[ok.length * 2];
            else vals16 = new char[ok.length * 2];
            java.util.Arrays.fill(keys, -1);
            mask = keys.length - 1;
            size = 0;
            for (int i = 0; i < ok.length; i++) if (ok[i] != -1) put(ok[i], ov32 != null ? ov32[i] : ov16[i]);
        }

        private int slot(int k) {
            int h = k * 0x9E3779B1;
            return (h ^ (h >>> 15)) & mask;
        }

        void put(int k, int v) {
            next = null;
            if ((size + 1) * 4 > keys.length * 3) grow();
            if (vals32 == null && (v < 0 || v > 0xFFFF)) {
                vals32 = new int[vals16.length];
                for (int i = 0; i < vals16.length; i++) vals32[i] = vals16[i];
                vals16 = null;
            }
            int i = slot(k);
            while (keys[i] != -1 && keys[i] != k) i = (i + 1) & mask;
            if (keys[i] == -1) size++;
            keys[i] = k;
            if (vals32 != null) vals32[i] = v;
            else vals16[i] = (char) v;
        }

        int get(int k) {
            int i = slot(k);
            while (true) {
                int key = keys[i];
                if (key == k) return vals32 != null ? vals32[i] : vals16[i];
                if (key == -1) return City.FAR;
                i = (i + 1) & mask;
            }
        }

        /** Where key k is kept, or -1 if it's off the route. */
        int find(int k) {
            int i = slot(k);
            while (true) {
                int key = keys[i];
                if (key == k) return i;
                if (key == -1) return -1;
                i = (i + 1) & mask;
            }
        }

        /**
         * (10.49) The next tile down from each tile, once worked out: a route doesn't change. ((16.4) As which way,
         * a byte, not the tile itself: 0 not yet, 1 stay, 2-5 east, west, south, north, 6 over the bridge.)
         */
        byte[] next;
    }

    /** One whole-map buffer the route searches share. */
    int[] scratch;

    /** Road tiles on the edge of the map: the ways out of town. */
    int[] exits;

    /** Points the vehicle at a new goal. Returns false if it can't be reached by road. */
    /**
     * Route searches left this step for everyday traffic: when the alarm goes up and everyone wants a new route
     * at once, they're spread over a few frames instead of one long stall (the rest try again a moment later).
     * Emergency services never wait.
     */
    int routeBudget = Integer.MAX_VALUE;
    /** ((28.1) What was left of it at the end of the last step, carried over (see update). */
    transient int routeBudgetLeft;

    /** Four-wheel drive: army trucks, Humvees, APCs and tanks, and the sheriff's, rangers' and federal agents' SUVs. */
    static boolean allWheel(Vehicle v) {
        if (v.type == TRUCK) return !v.restock;
        if (v.type == TANK) return true;
        return v.type == CRUISER && (v.agency == 2 || v.agency == 3 || v.agency == 4);
    }

    int movingTraffic() {
        int n = 0;
        for (int i = 0; i < vehicles.size(); i++) if (vehicles.get(i).type == CAR && !vehicles.get(i).parked) n++;
        return n;
    }

    /** People riding in cars right now (they still count as alive). */
    int riderCount() {
        int n = 0;
        for (int i = 0; i < vehicles.size(); i++) n += vehicles.get(i).riders.size();
        return n;
    }

    /** (16.2) Recent answers to canDrive, by where from and to (eight-tile squares): {when, 1 if there's a way}. */
    final java.util.HashMap<Long, float[]> canDriveSeen = new java.util.HashMap<Long, float[]>();

    /** Trucks sent out take one of the army trucks parked nearby (not the SWAT van, which has its own). */
    boolean fromParked = true;

    /** ((22.0) The armoured vans parked as shelters, and how many people each takes. */
    final ArrayList<Vehicle> shelters = new ArrayList<Vehicle>();
    static final int SHELTER_SEATS = 12;

    int reconNumber;

    /** (15.9) How far out (tiles of water) the landing craft start their run in to the shore. */
    static final int MARINE_RUN = 70;

    /** (11.0) A landing boat: in along its way over the water, Marines ashore, a while covering them, then gone. */
    /** (15.9) How many an evacuation boat takes off. */
    static final int BOAT_SEATS = 24;

    int rescueNumber;

    /**
     * (13.9) The rounds an ammo truck takes out to a fight, from the base's armoury (enough to fill up four soldiers),
     * how many may be out at once, and what the base keeps back for itself.
     */
    static final int AMMO_TRUCK_CARGO = 800, AMMO_TRUCKS_OUT = 2, AMMO_KEEP = 2000;

    // ------------------------------------------------------------------ (14.0) the engineers' bulldozer

    /** Wrecks the bulldozers have pushed off the roads this game. */
    int dozerCleared;

    /** How long pushing a wreck off the road takes, and how far a bulldozer goes for one. */
    static final float DOZER_PUSH = 4, DOZER_REACH = 2500;

    /** (15.10) How many an airliner takes out, and how many soldiers a transport brings. */
    static final int PLANE_SEATS = 120, TRANSPORT_LOAD = 60;

    /** ((25.0) How far out the crashing jet starts. */
    static final float CRASH_RUN = 1100;

    int count(int type) {
        int n = 0;
        for (int i = 0; i < vehicles.size(); i++) if (vehicles.get(i).type == type && !vehicles.get(i).broken) n++;
        return n;
    }

    /** (14.8) How much pushing (the dead's seconds against it) a line of crowd barriers takes before it goes over. */
    static final float BARRIER_HP = 45;

    // ------------------------------------------------------------------ helicopters on their pads (10.20)

    int airNumber;

    /**
     * Every helipad has its helicopter, parked there from the start, with its flight crew standing by it: the
     * army's (a pilot), the police's (a pilot) and the rescue service's (a pilot and a three-strong rescue team).
     * Run now and then: a helicopter lost is not replaced, but a pilot who's been killed is (from whoever's
     * nearby, while there are any).
     */
    final java.util.HashSet<float[]> padStocked = new java.util.HashSet<float[]>();

    /** Helipads whose helicopter has been lost for good. */
    final java.util.HashSet<float[]> padLost = new java.util.HashSet<float[]>();

    int trafficTarget;
    /** Less background traffic on a struggling phone. */
    float trafficScale = 1;
    float trafficTimer, trainTimer = 20, hwyTimer = 5;

    void update(float dt) {
        // ((27.5) A base's fire support truck out to a big fight.)
        fireSupport.tick(dt);
        helis.gunships(dt);
        frameNo++;
        if (frameNo % 30 == 0) routing.releaseRoutes();
        patrolCount = patrols.patrolCars();
        patrols.staffPatrols(dt);
        patrols.outbreakSweep(dt);
        medevac.update(dt);
        timers(dt);
        if (trafficTimer <= 0) {
            trafficTimer = 6;
            armyUnits.dozers();
            if (movingTraffic() < trafficTarget * trafficScale && w.zombieCount() < 10 && w.alert < 2) cars.spawnTraffic(1);
            towAway();
        }
        driving.buildGrid();
        // (10.32) A route on the biggest maps searches a lot more road: fewer of them a frame there.
        int area = city.w * city.h;
        // ((16.2) One a step on the biggest maps: a couple of hundred patrol cars need only a few new routes a second.)
        int perStep = area > 600000 ? 1 : area > 300000 ? 3 : 6;
        findSirens();
        boolean lodOn = w.viewX1 >= 0 && w.viewX1 - w.viewX0 < 2000 && vehicles.size() > 120;
        // ((16.8) Every fourth step with over four hundred on the map, four steps' worth: under a tile's length, even
        // at highway speed.)
        int lodEvery = vehicles.size() > 400 ? 4 : 2;
        // ((28.1) What a step leaves unused is kept for the next few: the cars well off screen (most of them, on a big
        // map) only drive every second or fourth step, all on the same step, and asked for their new trips then - one
        // search between the lot, the rest refused. On the biggest maps the patrol cars got a new beat a third as often
        // as they reached the end of one, and went round and round its last spot. The same searches a second on average.)
        routeBudget = Math.min(routeBudgetLeft + perStep, perStep * (lodOn ? lodEvery : 1));
        for (int i = vehicles.size() - 1; i >= 0; i--) {
            Vehicle v = vehicles.get(i);
            // ((11.5) Well off screen (out past the edge, not just round it), everyday traffic and units on their
            // rounds drive every other step, two steps' worth: nobody sees it, and the city has hundreds of cars.
            // Back near the screen they're worked out every step again, well before they come into view.)
            float vdt = dt;
            if (lodOn && v.player == null && !airborne(v) && v.type != TRAIN && v.type != BOAT && routing.farOff(v)) {
                if (++v.lodTick < lodEvery) continue;
                v.lodTick = 0;
                vdt = dt * lodEvery;
            } else v.lodTick = 0;
            wearAndNoise(v, vdt);
            boolean done = step(v, vdt);
            // ((15.0) Lights and sirens coming up to a junction with a traffic officer on it: the officer holds the
            // rest of the traffic and waves it through.)
            if (!done && w.trafficPost != null && v.speed > 8 && !offRoad(v) && v.lightsOn() && ((i + (int) (w.time * 30)) & 7) == 0) {
                float ca = (float) Math.cos(v.angle), sa = (float) Math.sin(v.angle);
                for (int k = 1; k <= 4; k++) {
                    int jid = city.junctionIdAt(v.x + ca * k * City.T, v.y + sa * k * City.T);
                    if (jid >= 0) {
                        if (w.trafficOfficer(jid) != null) w.holdJunction(jid, v);
                        break;
                    }
                }
            }
            if (done) {
                // ((12.0) One of the city's own officers or soldiers at the wheel gets out, rather vanishing.)
                if (v.driver != null && v.realDriver) crews.driverOut(v, false);
                // (10.48) Whoever was still aboard a car fleeing the city went with it.
                if (v.exodus && !v.riders.isEmpty()) w.droveOut(v.riders);
                v.removedFromFleet = true;
                vehicles.remove(i);
                continue;
            }
            // At a scene: in to the kerb, out of the traffic's way.
            // ((27.5) And while it waits for its officers to come back to it, or to come and get in: the queue behind a
            // police car stopped in the lane for that sat there as long as it did.)
            // ((30.8) And a troop truck waiting there for its squad.)
            if (((v.type == CRUISER && (v.state == SCENE || v.state == RECALL || v.state == PICKUP)) || (v.type == AMBULANCE && v.state == LOAD)
                    || (v.type == TRUCK && v.state == SCENE && !v.ammoTruck && v.kind != K_FIRE_SUPPORT && v.kind != K_DOZER && v.clearing == null
                        && v.passengerType == Entity.SOLDIER && !v.squad.isEmpty() && !v.moveUp))
                    && Math.abs(v.speed) < 1 && !v.broken && v.player == null)
                driving.kerbAtScene(v, vdt);
            else if (Math.abs(v.speed) > 10) v.kerbed = false;
            // ((28.6) Sat in a junction at a scene (a call right on the corner, every spot off it taken when it pulled
            // in): once a second it looks again, a little further afield, until it's out of the box and off the lane.)
            if ((v.state == SCENE || v.state == RECALL) && v.type == CRUISER && !v.pulling && !v.broken && v.player == null && v.stillT > 5
                    && (int) w.time != (int) (w.time - vdt) && city.junctionIdAt(v.x, v.y) >= 0)
                driving.offLane(v, 6);
            if (!airborne(v) && v.type != TRAIN && v.hp < v.maxHp * 0.5f) smoke(v, vdt);
        }
        routeBudgetLeft = Math.max(0, routeBudget);
        routeBudget = Integer.MAX_VALUE;
        driving.buildGrid();
        driving.collide();
        city.updateLights(dt);
    }

    /**
     * ((27.3) One step for one vehicle, by the part that drives its kind: the player's, helicopters, jets, trains, boats,
     * survivors' pickups, convoy columns, wrecks and parked ones, tanks, everyday cars and evacuation buses, patrol cars,
     * and the rest - cars and trucks sent somewhere (FleetService.updateCar, which hands on ambulances, bulldozers, recon
     * Humvees and command trucks). True when it's gone from the map.
     */
    boolean step(Vehicle v, float vdt) {
        boolean done;
        if (v.player != null) done = playerDriving.updatePlayerVehicle(v, vdt);
        else if (v.type == HELI && v.broken) done = helis.heliCrash(v, vdt);
        else if (v.type == HELI) done = helis.updateHeli(v, vdt);
        else if (v.type == JET) done = aircraft.updateJet(v, vdt);
        else if (v.type == TRAIN) done = trains.updateTrain(v, vdt);
        else if (v.type == BOAT) done = boats.updateBoat(v, vdt);
        else if (v.kind == K_PICKUP && !v.parked) done = convoyUnits.updatePickup(v, vdt);
        else if (v.convoy != null && !v.broken) done = w.convoys.step(v, vdt);
        else if (v.broken || v.parked) done = service.updateWreck(v, vdt);
        else if (v.type == TANK) done = armyUnits.updateTank(v, vdt);
        else if (v.type == CAR) done = v.evacStop >= 0 ? evacBuses.updateEvacBus(v, vdt) : cars.updateTraffic(v, vdt);
        else if (v.patrol) done = patrols.updatePatrol(v, vdt);
        else done = service.updateCar(v, vdt);
        return done;
    }

    /** ((27.3) A step of update.) Who's out with lights and sirens: everyone else gives way to them. */
    void findSirens() {
        // Who's out with lights and sirens: everyone else gives way to them.
        sirens.clear();
        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle o = vehicles.get(i);
            // ((10.51) Army trucks and tanks on their way to the fighting too: everyone pulls over for them.)
            boolean army = (o.type == TRUCK || o.type == TANK) && o.state == DRIVE && routing.emergency(o);
            // ((28.6) And an escorted convoy on the move: traffic cutting in between its vehicles at a junction locked the
            // column and the cars into a ring that never came undone.)
            boolean column = o.convoy != null && !o.broken;
            if (o.player == null && !airborne(o) && (o.lightsOn() || army || column) && Math.abs(o.speed) > 18) sirens.add(o);
        }
    }

    /** ((27.3) A step of update.) Old wrecks and parked cars are towed away, but never while you're watching. */
    void towAway() {
        int parked = 0;
        // Old wrecks and parked cars are towed away, but never while you're watching.
        for (int i = vehicles.size() - 1; i >= 0; i--) {
            Vehicle o = vehicles.get(i);
            // (A wreck left in the middle of a junction goes first.)
            boolean blocking = o.parked && city.junctionIdAt(o.x, o.y) >= 0;
            // ((12.0) Not a station's police cars or a base's trucks parked up ready on their lot - but anything left
            // in a junction is, police car or army truck: one abandoned there locked the junction up for good.)
            if (o.standby && !blocking) continue;
            if (o.type != CAR && !blocking && o.state != ABANDONED && !o.broken) continue;
            if (o.parked && o.block == null && o.riders.isEmpty() && o.player == null && (++parked > 25 || blocking)
                    && !routing.inView(o.x, o.y)) {
                o.removedFromFleet = true;
                vehicles.remove(i);
            }
        }
    }

    /** ((27.3) A step of update.) What goes on with any vehicle, whatever it's doing: its alarm, the dead clawing at it, its engine drawing them, and its timers. */
    void wearAndNoise(Vehicle v, float vdt) {
        // ((22.0) The dead blundering into a parked car can set it off.)
        if (v.type == CAR && !v.alarmed && w.zombies > 0 && (v.parked || v.state == ABANDONED) && (v.engineCd -= vdt) <= 0) {
            v.engineCd = 1;
            if (w.countZombiesNear(v.x, v.y, 14) > 0) cars.tripAlarm(v, 0.2f);
        }
        // ((26.0) The dead clawing at it, if it's stopped with people in it: four times a second.)
        if ((v.clawAcc += vdt) >= 0.25f) {
            if (w.zombies > 0) w.clawAt(v, v.clawAcc);
            v.clawAcc = 0;
        }
        if (v.alarm > 0) {
            // Whoop, whoop: every zombie for streets around comes to see.
            v.alarm -= vdt;
            if (v.broken || v.burnt) v.alarm = 0;
            else if (((int) (v.alarm * 2)) != ((int) ((v.alarm + vdt) * 2))) {
                w.emit(Sfx.HORN, v.x, v.y);
                if (((int) v.alarm) % 4 == 0) w.noise(v.x, v.y, 180);
            }
        }
        // ((22.0) The dead follow the sound of an engine: anything driving along draws those within earshot after
        // it, a siren or a tank from furthest off. A horde can end up trailing a car down the street.)
        if (w.zombies > 0 && v.type != HELI && v.type != TRAIN && v.type != BOAT && v.type != JET && !v.parked && !v.broken
                && Math.abs(v.speed) > 25 && (v.engineCd -= vdt) <= 0) {
            v.engineCd = 2 + w.rnd.nextFloat();
            w.engineLure(v.x, v.y, v.lightsOn() ? 230 : v.type == TANK ? 210 : v.type == TRUCK ? 150 : 110);
        }
        v.anim += vdt;
        // (Time spent crawling: nudging back and forth in a jam still counts.)
        if (Math.abs(v.speed) < 8) v.stillT += vdt;
        else if (Math.abs(v.speed) > 15) v.stillT = 0;
        if (v.parked || Math.abs(v.speed) > 15) v.knotT = Math.max(0, v.knotT - vdt * 3);
        else if (Math.abs(v.speed) < 8) v.knotT += vdt;
        v.soundCd -= vdt;
        v.destCd -= vdt;
        v.rerouteCd -= vdt;
        v.holdSaidCd -= vdt;
        v.crashCd -= vdt;
    }

    /** ((27.3) A step of update.) The things done every so often: drivers, reclaiming vehicles, evacuation buses, trains, people driving out, the pumps, the highway's own traffic. */
    void timers(float dt) {
        driverTimer -= dt;
        if (driverTimer <= 0) {
            driverTimer = 0.5f;
            crews.drivers();
        }
        // ((32.8) Anyone let out of a vehicle without being put down anywhere: back on the street.)
        strayTimer -= dt;
        if (strayTimer <= 0) {
            strayTimer = 2;
            crews.findStrays();
        }
        // Keep the streets busy while the city is still calm; clear old wrecks.
        reclaimTimer -= dt;
        if (reclaimTimer <= 0) {
            reclaimTimer = 3;
            crews.reclaim();
        }
        busTimer -= dt;
        if (busTimer <= 0) {
            busTimer = 2;
            evacBuses.sendEvacBuses();
            // (People waiting on the platform: the next train comes soon, and stops for them.)
            if (city.railY0 >= 0 && city.stationX >= 0 && w.exodusOn() && w.byTransit()
                    && w.waitingFor(World.STOP_TRAIN, city.stationX, city.stationY, false) >= 3) trainTimer = Math.min(trainTimer, 20);
        }
        trafficTimer -= dt;
        trainTimer -= dt;
        if (trainTimer <= 0 && city.railY0 >= 0) {
            trainTimer = 70 + w.rnd.nextFloat() * 50;
            trains.spawnTrain();
        }
        // (10.32) The emergency broadcast: everyone with a car and somewhere to go makes for the highway at once.
        if (w.alert >= 2 && !evacStarted) {
            evacStarted = true;
            // ((12.5) Only when civilians drive at all: none do now.)
            evacLeft = city.hwyAxis >= 0 && trafficTarget > 0 ? Math.max(10, trafficTarget / 2) : 0;
            if (evacLeft > 0) w.timeline("People fleeing the city: the roads out are jammed");
        }
        if (evacLeft > 0) {
            evacTimer -= dt;
            if (evacTimer <= 0) {
                evacTimer = 1.2f + w.rnd.nextFloat();
                evacLeft--;
                cars.spawnEvacuee();
            }
        }
        // (Tankers keep the pumps topped up while things are normal.)
        if (w.alert < 2 && pumpFuel != null) for (int i = 0; i < pumpFuel.length; i++) pumpFuel[i] = Math.min(6, pumpFuel[i] + dt * 0.01f);
        // (10.29) The highway has traffic of its own, coming on at the ends, so it doesn't empty out.
        hwyTimer -= dt;
        if (hwyTimer <= 0) {
            hwyTimer = 2;
            if (city.hwyAxis >= 0 && w.alert < 2 && trafficTarget > 0 && cars.throughTraffic() < cars.throughTarget()) cars.spawnThrough(false);
        }
    }

    final ArrayList<Vehicle> sirens = new ArrayList<Vehicle>();

    // ------------------------------------------------------------------ who's near whom

    /** Vehicles on the ground bucketed by 128-unit square, rebuilt each step: who's near whom without asking everyone. */
    static final int GC = 128;
    Vehicle[] gridV = new Vehicle[64];
    int[] gridStart = new int[1], gridFill = new int[1];
    int gridW, gridH, gridN;

    /**
     * ((11.5) The vehicles on the ground in the grid squares within r of (x, y), as of the last time the grid was
     * built, into found[0..n): for the world's questions about cars nearby without going through all of them.
     * The caller checks how near each really is. Not to be asked again while going through the last answer.)
     */
    Vehicle[] found = new Vehicle[32];

    // ------------------------------------------------------------------ damage

    /** Hurts a vehicle; at zero it breaks down and everyone inside gets out. */
    void damage(Vehicle v, float amount, boolean blast) {
        // ((17.6) Helicopters included: they were left out with the trains and boats, and gunfire never touched them.)
        if ((offRoad(v) && v.type != HELI) || amount <= 0) return;
        if (v.type == HELI && v.broken) return;
        // (Armour: the dead can claw at an APC or the riot van all day.)
        if (!blast && (v.kind == K_APC || v.kind == K_RIOT_VAN)) amount *= 0.3f;
        // ((27.5) The MRAP: built to shrug off claws and blasts alike, though not for ever.)
        if (v.kind == K_MRAP) amount *= blast ? 0.5f : 0.3f;
        v.hp -= amount;
        cars.tripAlarm(v, 0.6f);
        if (v.broken) {
            if (!v.burnt && (blast || v.hp < -v.maxHp * 0.5f)) burn(v);
            return;
        }
        if (v.hp > 0) return;
        v.broken = true;
        v.parked = true;
        w.carsWrecked++;
        // The crew bails out (and the driver). ((17.6) Not from a helicopter in the air: they go down with it.)
        if (v.type != HELI && !v.crew.isEmpty()) crews.crewOut(v, true);
        if (v.type != HELI && v.driver != null) crews.driverOut(v, true);
        v.speed = 0;
        v.field = null;
        w.emit(Sfx.THUD, v.x, v.y);
        for (int k = 0; k < 10; k++)
            w.particle(v.x, v.y, w.rnd.nextFloat() * 40 - 20, w.rnd.nextFloat() * 40 - 20, 0.3f, 0.8f, 0xFFFFD27A, World.P_DOT);
        // Anyone still inside gets out and carries on on foot.
        if ((v.type == CRUISER || v.type == TRUCK) && v.state <= DRIVE && v.passengers > 0) {
            crews.unload(v, false);
            v.passengers = 0;
        }
        if (v.type == CAR && v.state == CRUISE) {
            v.state = ABANDONED;
            crews.getOut(v, true);
            driving.pullOver(v);
        }
        service.dropRiders(v, true);
        if (v.supply > 0) {
            w.dispatch.say(Dispatch.WHO_MILITARY, null, "Military: We've lost the supply truck on " + city.placeName(v.x, v.y)
                    + ". The ammo's gone.", v.x, v.y);
            if (v.zone != null) v.zone.supplyComing = false;
            v.supply = 0;
        }
        if (v.type == TANK) {
            // The crew bails out.
            w.dispatch.say(Dispatch.WHO_MILITARY, null, "Military: Our tank is knocked out on " + city.placeName(v.x, v.y)
                    + "! Crew bailing out.", v.x, v.y);
            for (int k = 0; k < 2; k++) {
                Entity crew = w.spawn(Entity.SOLDIER, v.x + (float) Math.cos(v.angle + 1.57f) * (10 + k * 4),
                        v.y + (float) Math.sin(v.angle + 1.57f) * (10 + k * 4));
                if (crew != null) crew.hp *= 0.6f;
            }
        }
        if (blast || v.hp < -v.maxHp * 0.3f || w.rnd.nextFloat() < 0.3f) burn(v);
    }

    void burn(Vehicle v) {
        v.burnt = true;
        // ((22.0) Half of them go up now (a third before), and close enough to catch the next one: a street of parked
        // cars can go one after another.)
        if (w.rnd.nextFloat() < 0.5f) w.blastLater(v.x, v.y, 48, 160, 2 + w.rnd.nextFloat() * 4);
    }

    int alarms;

    void smoke(Vehicle v, float dt) {
        float f = 1 - Math.max(0, v.hp) / (v.maxHp * 0.5f);
        if (w.rnd.nextFloat() < dt * (2 + f * 6))
            w.particle(v.x + (float) Math.cos(v.angle) * v.length() * 0.6f, v.y + (float) Math.sin(v.angle) * v.length() * 0.6f,
                    4 + w.rnd.nextFloat() * 4, -8 - w.rnd.nextFloat() * 6, 1.5f + w.rnd.nextFloat(), 2 + w.rnd.nextFloat() * 2,
                    f > 0.8f ? 0xFF2A2A2A : 0xFF6A6A6A, World.P_SMOKE);
    }

    /**
     * The rules at junctions. Traffic lights: stop on red (and on amber if there's room to), go on green.
     * Stop junctions: come to a full stop at the line, then go when the junction is clear (or after a few
     * seconds, so nobody waits for ever). Roundabouts: give way to anything already on it. Returns the
     * fastest the car may go right now.
     */
    /** ((24.2) junctionRule for a police or army vehicle that isn't on a call: the everyday rules. */
    boolean calmRules;

    /** (11.3) The lane to be in on each tile of the corridor (-1 the car's own): see turnLanes. */
    final int[] corLane = new int[COR];

    /** Working space for buildCorridor: its tiles, where the path tile is among them, and the junction it's in. */
    final int[] corTiles = new int[COR];
    int corPi, jEntryX, jEntryY;
    float jEntryL;
    boolean jSmooth, rbOn;
    float rbCx, rbCy, rbR, rbA0, rbD;

    /** ((11.5) The one point pursuitPoint hands back, reused: it's asked of every car every step.) */
    final float[] pursuit = new float[2];

    /** The way the line runs where alongCorridor last found something on it. */
    float hitDx, hitDy;

    /** Someone else's car close in front: time to brake. */
    /**
     * The fastest a vehicle may go and still stop behind whatever is ahead of it in its lane: room to stop at
     * its own speed (a two-second gap, less in town), and never faster than the one in front once close.
     */
    /** How much quicker than the old open-road speeds vehicles drive (10.20). */
    static final float DRIVE_SCALE = 1.8f;

    /** How many cars and trucks a station or base keeps parked before the rest go back where they came from. */
    static final int DEPOT_ROOM = 14;

    float reclaimTimer, fetchSaidAt, busTimer, driverTimer;

    /** How many an evacuation bus takes. */
    static final int BUS_SEATS = 36;

    /**
     * ((12.0) Who belongs to which vehicle while out of it, gathered in one look through everyone a frame.) ((21.3) Kept
     * on each vehicle (Vehicle.foot, stamped with the frame), not in a map looked up for each of thousands of officers
     * every frame.)
     */
    static final ArrayList<Entity> NOBODY = new ArrayList<Entity>();
    int frameNo, rigFrame = -1;

    /** ((22.3) Everyone given a vehicle to belong to, for onFoot. */
    final ArrayList<Entity> rigged = new ArrayList<Entity>();
    /** ((32.8) Seconds to the next look for strays (FleetCrews.findStrays). */
    private float strayTimer = 2;

    /**
     * A squad from the base mounts up: a truck pulls up at the gate, the soldiers climb aboard, and it drives
     * them to the fighting (the same soldiers, names, kills and all). False if there's no road for it.
     */
    boolean troopTruck(java.util.List<Entity> squad, City.Facility base, Dispatch.Incident inc) {
        return troopTruck(squad, base, inc, -1);
    }

    /** A truck (or, kind >= 0, a vehicle of that kind) for this squad from the base. */
    boolean troopTruck(java.util.List<Entity> squad, City.Facility base, Dispatch.Incident inc, int forceKind) {
        // (One of the trucks parked on the base, if there's one left; otherwise one comes round to the gate.)
        // ((11.7) One of the trucks parked on the base, or no patrol: none appear from nowhere.)
        if (squad.size() < 2 || service.gateBusy(base)) return false;
        float[] p = armyUnits.baseTruck(base);
        if (p == null) return false;
        int kind = 0;
        if (city.cfg.nature()) {
            // An APC into a big fight (or from an armoured base), a Humvee for a fast run a long way out.
            float far = (float) World.hypot(inc.x - p[0], inc.y - p[1]);
            // ((14.6) The worst of it gets the IFV; a big pack out on the roads, the gun truck.)
            kind = inc.zombiesNear >= 18 ? K_IFV : inc.zombiesNear >= 12 && routing.onRoad(inc.x, inc.y) ? K_GUNTRUCK
                    // ((27.5) A dangerous call: one of the base's few MRAPs, while there's one.)
                    : inc.zombiesNear >= 8 && base.baseType != City.BT_ARMOUR && armyUnits.mrapFree(base) ? K_MRAP
                    : inc.zombiesNear >= 10 || (base.baseType == City.BT_ARMOUR && inc.zombiesNear >= 5) ? K_APC
                    : far > 900 && squad.size() <= HUMVEE_SEATS ? K_HUMVEE : 0;
            if (forceKind >= 0) kind = forceKind;
        }
        armyUnits.musterTruck(squad, p, inc, kind, forceKind < 0);
        return true;
    }

    /** ((16.6) Trucks a base has standing in its yard at the least, where it has them. */
    static final int SHOWN_TRUCKS = 4;

    int bikeSightings;

    static float turnTo(float from, float to, float amount) {
        float diff = to - from;
        while (diff > Math.PI) diff -= Math.PI * 2;
        while (diff < -Math.PI) diff += Math.PI * 2;
        return from + Math.max(-amount, Math.min(amount, diff));
    }

    // ------------------------------------------------------------------ ((27.3) what the rest of the game calls, passed on to the part that does it

    /** See FleetTraffic. */
    float[] pumpFuel() {
        return cars.pumpFuel();
    }

    /** See FleetPickups. */
    int pickups() {
        return convoyUnits.pickups();
    }

    /** See FleetPickups. */
    int pickupsOf(SurvivorBases.Base b) {
        return convoyUnits.pickupsOf(b);
    }

    /** See FleetPickups. */
    Vehicle makePickup(float x, float y, float angle, SurvivorBases.Base b, java.util.List<Entity> crew) {
        return convoyUnits.makePickup(x, y, angle, b, crew);
    }

    /** See FleetPickups. */
    Vehicle convoyVehicle(int role, float x, float y, float angle, Convoys.Column c) {
        return convoyUnits.convoyVehicle(role, x, y, angle, c);
    }

    /** See FleetPickups. */
    boolean routeTo(Vehicle v, float x, float y) {
        return convoyUnits.routeTo(v, x, y);
    }

    /** See FleetPickups. */
    boolean convoyDrive(Vehicle v, float dt, float max) {
        return convoyUnits.convoyDrive(v, dt, max);
    }

    /** See FleetPickups. */
    void convoyGun(Vehicle v, float dt) {
        convoyUnits.convoyGun(v, dt);
    }

    /** See FleetPickups. */
    void takeAsPickup(Vehicle v, java.util.List<Entity> crew) {
        convoyUnits.takeAsPickup(v, crew);
    }

    /** See FleetRouting. */
    float[] randomTownRoad() {
        return routing.randomTownRoad();
    }

    /** See FleetRouting. */
    int traceRoute(Vehicle v, int[] out) {
        return routing.traceRoute(v, out);
    }

    /** See FleetRouting. */
    boolean inView(float x, float y) {
        return routing.inView(x, y);
    }

    /** See FleetService. */
    boolean send(int unitType, int count, float fromX, float fromY, float toX, float toY, Dispatch.Incident inc, Dispatch.SafeZone zone, String place) {
        return service.send(unitType, count, fromX, fromY, toX, toY, inc, zone, place);
    }

    /** See FleetService. */
    boolean send(int unitType, int count, float fromX, float fromY, float toX, float toY, Dispatch.Incident inc, Dispatch.SafeZone zone, String place, int per) {
        return service.send(unitType, count, fromX, fromY, toX, toY, inc, zone, place, per);
    }

    /** See FleetRouting. */
    boolean clearFromEntry(Vehicle o) {
        return routing.clearFromEntry(o);
    }

    /** See FleetRouting. */
    int driveCost(float fx, float fy, float tx, float ty) {
        return routing.driveCost(fx, fy, tx, ty);
    }

    /** See FleetRouting. */
    boolean canDrive(float fx, float fy, float tx, float ty) {
        return routing.canDrive(fx, fy, tx, ty);
    }

    /** See FleetService. */
    boolean sendSwat(City.Facility station, Dispatch.Incident inc) {
        return service.sendSwat(station, inc);
    }

    /** See FleetService. */
    boolean sendBearCat(City.Facility station, Dispatch.Incident inc) {
        return service.sendBearCat(station, inc);
    }

    /** See FleetArmy. */
    boolean startRecon(City.Facility base) {
        return armyUnits.startRecon(base);
    }

    /** See FleetArmy. */
    Vehicle startReconAt(float x, float y) {
        return armyUnits.startReconAt(x, y);
    }

    /** See FleetService. */
    Vehicle placeBearCat(float x, float y) {
        return service.placeBearCat(x, y);
    }

    /** See FleetService. */
    Vehicle placeShelterVan(float x, float y) {
        return service.placeShelterVan(x, y);
    }

    /** See FleetCommand. */
    Vehicle commandPost(float sx, float sy, float tx, float ty, java.util.List<Entity> crew, int kind) {
        return commandTrucks.commandPost(sx, sy, tx, ty, crew, kind);
    }

    /** See FleetCommand. */
    Vehicle parkPost(City.Facility f, int kind) {
        return commandTrucks.parkPost(f, kind);
    }

    /** See FleetCommand. */
    void parkHome(Vehicle v, City.Facility f) {
        commandTrucks.parkHome(v, f);
    }

    /** See FleetCommand. */
    boolean postGo(Vehicle v, float tx, float ty) {
        return commandTrucks.postGo(v, tx, ty);
    }

    /** See FleetCommand. */
    void seat(Vehicle v, java.util.List<Entity> crew) {
        commandTrucks.seat(v, crew);
    }

    /** See FleetCommand. */
    void musterPost(Vehicle v, java.util.List<Entity> crew, float tx, float ty) {
        commandTrucks.musterPost(v, crew, tx, ty);
    }

    /** See FleetCommand. */
    boolean movePost(Vehicle v, float tx, float ty) {
        return commandTrucks.movePost(v, tx, ty);
    }

    /** See FleetBoats. */
    Vehicle policeBoat(float x, float y, float lx, float ly, int number) {
        return boats.policeBoat(x, y, lx, ly, number);
    }

    /** See FleetBoats. */
    int[][] seaApproach(float tx, float ty) {
        return boats.seaApproach(tx, ty);
    }

    /** See FleetBoats. */
    boolean marineLanding(int count, float tx, float ty, String place, Dispatch.Incident inc) {
        return boats.marineLanding(count, tx, ty, place, inc);
    }

    /** See FleetService. */
    boolean sendRiotVan(City.Facility station, Dispatch.Incident inc) {
        return service.sendRiotVan(station, inc);
    }

    /** See FleetPatrols. */
    Vehicle startRangerPatrol(City.Building station) {
        return patrols.startRangerPatrol(station);
    }

    /** See FleetArmy. */
    int ammoTrucksOut() {
        return armyUnits.ammoTrucksOut();
    }

    /** See FleetArmy. */
    Vehicle ammoTruckNear(float x, float y, float reach) {
        return armyUnits.ammoTruckNear(x, y, reach);
    }

    /** See FleetArmy. */
    boolean ammoTruckFor(Dispatch.Incident inc) {
        return armyUnits.ammoTruckFor(inc);
    }

    /** See FleetArmy. */
    Vehicle ammoTruckComing(float x, float y, float reach) {
        return armyUnits.ammoTruckComing(x, y, reach);
    }

    /** See FleetArmy. */
    Vehicle sendAmmoTruck(City.Facility from, Dispatch.Incident inc) {
        return armyUnits.sendAmmoTruck(from, inc);
    }

    /** See FleetArmy. */
    void ensureDozers() {
        armyUnits.ensureDozers();
    }

    /** See FleetArmy. */
    boolean sendSupply(City.Facility from, Dispatch.SafeZone z, int rounds) {
        return armyUnits.sendSupply(from, z, rounds);
    }

    /** See FleetArmy. */
    boolean sendRestock(float fromX, float fromY, City.Building b, City.Facility f) {
        return armyUnits.sendRestock(fromX, fromY, b, f);
    }

    /** See FleetAircraft. */
    void sendJet(float tx, float ty) {
        aircraft.sendJet(tx, ty);
    }

    /** See FleetAircraft. */
    boolean flyInTroops(int count, Dispatch.Incident inc, String place) {
        return aircraft.flyInTroops(count, inc, place);
    }

    /** See FleetAircraft. */
    void sendCrashingPlane(float tx, float ty) {
        aircraft.sendCrashingPlane(tx, ty);
    }

    /** See FleetAmbulances. */
    boolean sendAmbulance(float tx, float ty) {
        return ambulances.sendAmbulance(tx, ty);
    }

    /** See FleetArmy. */
    boolean sendTank(float fromX, float fromY, float toX, float toY, String place) {
        return armyUnits.sendTank(fromX, fromY, toX, toY, place);
    }

    /** See FleetTraffic. */
    Vehicle placeCar(float x, float y) {
        return cars.placeCar(x, y);
    }

    /** See FleetPatrols. */
    Vehicle placePolice(float x, float y) {
        return patrols.placePolice(x, y);
    }

    /** See FleetPatrols. */
    int roadblocks(Dispatch.SafeZone z) {
        return patrols.roadblocks(z);
    }

    /** See FleetPatrols. */
    void putUpBarrier(Vehicle v, float zx, float zy) {
        patrols.putUpBarrier(v, zx, zy);
    }

    /** See FleetArmy. */
    Vehicle placeTank(float x, float y) {
        return armyUnits.placeTank(x, y);
    }

    /** See FleetHelis. */
    void stationHelis() {
        helis.stationHelis();
    }

    /** See FleetHelis. */
    int helisOfKind(int kind) {
        return helis.helisOfKind(kind);
    }

    /** See FleetHelis. */
    Entity livePilot(Vehicle v) {
        return helis.livePilot(v);
    }

    /** See FleetHelis. */
    Vehicle readyHeli(int kind, float x, float y) {
        return helis.readyHeli(kind, x, y);
    }

    /** See FleetHelis. */
    boolean launchHeli(Vehicle v, float tx, float ty, String place, Dispatch.Incident inc, int extra) {
        return helis.launchHeli(v, tx, ty, place, inc, extra);
    }

    /** See FleetHelis. */
    ArrayList<Entity> airlift(int count, float fx, float fy, float tx, float ty, String place, Dispatch.Incident inc) {
        return helis.airlift(count, fx, fy, tx, ty, place, inc);
    }

    /** See FleetTrains. */
    boolean crossingClosed(float x) {
        return trains.crossingClosed(x);
    }

    /** See FleetDriving. */
    int gather(float x, float y, float r) {
        return driving.gather(x, y, r);
    }

    /** See FleetTraffic. */
    void tripAlarm(Vehicle v, float odds) {
        cars.tripAlarm(v, odds);
    }

    /** See FleetService. */
    boolean sendHome(java.util.List<Entity> group, float ex, float ey) {
        return service.sendHome(group, ex, ey);
    }

    /** See FleetDriving. */
    boolean onBridge(Vehicle v) {
        return driving.onBridge(v);
    }

    /** See FleetPlayer. */
    void takeWheel(Vehicle v, Entity e) {
        playerDriving.takeWheel(v, e);
    }

    /** See FleetPlayer. */
    void handBack(Vehicle v, Entity e) {
        playerDriving.handBack(v, e);
    }

    void leaveWheel(Vehicle v, Entity e) {
        playerDriving.leaveWheel(v, e);
    }

    /** See FleetCrews. */
    void boarded(Vehicle v, Entity e) {
        crews.boarded(v, e);
    }

    /** See FleetCrews. */
    Entity dragOut(Vehicle v, float x, float y) {
        return crews.dragOut(v, x, y);
    }

    /** See FleetCrews. */
    void board(Vehicle v, Entity e) {
        crews.board(v, e);
    }

    /** See FleetCrews. */
    void discard(Vehicle v) {
        crews.discard(v);
    }

    /** See FleetCrews. */
    void crewOut(Vehicle v, boolean offDuty) {
        crews.crewOut(v, offDuty);
    }

    /** See FleetEvac. */
    void exodusOver() {
        evacBuses.exodusOver();
    }

    /** See FleetCrews. */
    int crewOnFoot(Vehicle v) {
        return crews.crewOnFoot(v);
    }

    /** See FleetCrews. */
    void note(Entity e) {
        crews.note(e);
    }

    /** See FleetCrews. */
    void countCrews(int[] counts) {
        crews.countCrews(counts);
    }

    /** See FleetPatrols. */
    Vehicle startPatrol(City.Facility station) {
        return patrols.startPatrol(station);
    }

    /** See FleetPatrols. */
    Vehicle startAgencyPatrol(int agency) {
        return patrols.startAgencyPatrol(agency);
    }

    /** See FleetPatrols. */
    boolean stationCar(java.util.List<Entity> officers, City.Facility f, Dispatch.Incident inc) {
        return patrols.stationCar(officers, f, inc);
    }

    /** See FleetPatrols. */
    Vehicle patrolFor(float x, float y) {
        return patrols.patrolFor(x, y);
    }

    /** See FleetPatrols. */
    void heardFight(float x, float y, float radius, float tx, float ty) {
        patrols.heardFight(x, y, radius, tx, ty);
    }

    /** See FleetPatrols. */
    void redirect(Dispatch.Incident from, Dispatch.Incident to) {
        patrols.redirect(from, to);
    }

    /** See FleetPatrols. */
    boolean respond(Vehicle v, Dispatch.Incident inc) {
        return patrols.respond(v, inc);
    }

    /** See FleetArmy. */
    boolean mountUp(java.util.List<Entity> squad, Dispatch.Incident inc) {
        return armyUnits.mountUp(squad, inc);
    }

    /** See FleetArmy. */
    float[] bay(float[] p, City.Facility f) {
        return armyUnits.bay(p, f);
    }

    /** See FleetArmy. */
    java.util.List<Entity> standby(City.Facility f, int type, int role, int max) {
        return armyUnits.standby(f, type, role, max);
    }

    /** See FleetArmy. */
    boolean basePatrol(java.util.List<Entity> squad, City.Facility base, float tx, float ty) {
        return armyUnits.basePatrol(squad, base, tx, ty);
    }

    /** See FleetArmy. */
    int basePatrols(City.Facility base) {
        return armyUnits.basePatrols(base);
    }

    /** See FleetArmy. */
    int inboundSoldiers(Dispatch.Incident inc) {
        return armyUnits.inboundSoldiers(inc);
    }

    /** See FleetArmy. */
    void stockBases() {
        armyUnits.stockBases();
    }

    /** See FleetPatrols. */
    void stockStations() {
        patrols.stockStations();
    }

    /** See FleetService. */
    boolean driveTo(java.util.List<Entity> group, Dispatch.Incident inc) {
        return service.driveTo(group, inc);
    }

    /** See FleetService. */
    int inbound(Dispatch.Incident inc) {
        return service.inbound(inc);
    }

    /** See FleetPatrols. */
    Entity startBikeAt(float x, float y) {
        return patrols.startBikeAt(x, y);
    }
}
