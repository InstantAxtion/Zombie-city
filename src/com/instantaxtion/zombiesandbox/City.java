package com.instantaxtion.zombiesandbox;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Procedurally generated city: tile map, pre-rendered ground bitmap, line of sight and flow fields.
 *
 * <p>((27.3) This file used to hold all of it, 16,000 lines. Now the city is built by {@link CityGenerator} (its
 * generate() a list of named steps) with {@link CityRoads}, {@link CityBlocks}, {@link CityOutskirts} and
 * {@link MilitaryBases}, each special map by a class of its own ({@link IslandsMap}, {@link ThemeParkMap},
 * {@link SpaceCenterMap}, {@link DesertMap}, {@link PrisonMap}, {@link RiverMap}, {@link CoastMap},
 * {@link AirportMap}), and painted by {@link CityPainter}. What's left here is the map itself - its tiles, buildings,
 * facilities and the rest - and everything the game asks of it while playing: walking and driving fields, line of
 * sight, places and names, junctions and lights, edits, and the map picture's storage. Nothing about how a city comes
 * out changed: the code moved as it stood, every map version still builds the same map.)
 */
final class City {
    static final int T = 16;
    static final int FAR = 1 << 20;

    static final byte ROAD = 0, SIDEWALK = 1, BUILDING = 2, GRASS = 3, TREE = 4, PLAZA = 5, CAR = 6,
            STATUE = 7, LOT = 8, BASE = 9, FENCE = 10, PUMP = 11, RUBBLE = 12, RAIL = 13, DIRT = 14, WATER = 15, SAND = 16,
            PIER = 17, ROCK = 18, TRAIL = 19;

    static final int OFFICE = 0, HOUSE = 1, WAREHOUSE = 2, STATION = 3, BARRACKS = 4, TOWER = 5, HOSPITAL = 6,
            SHOP = 7, CHURCH = 8, SCHOOL = 9, FIRE_STATION = 10, MARKET = 11, KIOSK = 12, SPIRE = 13, CRYPT = 14,
            APARTMENT = 15, GARAGE = 16, PHARMACY = 17, TRAIN_STATION = 18, MALL = 19, STADIUM = 20, POWER = 21,
            BARN = 22, SILO = 23, CITY_HALL = 24, COURTHOUSE = 25, JAIL = 26, CALL_CENTRE = 27, WORKS = 28, ARMORY = 29;
    /** Ground decorations drawn into the map: {kind, x0, y0, x1, y1, variant} in world units. */
    static final int D_COURT = 0, D_FIELD = 1, D_PLAYGROUND = 2, D_GARDEN = 3, D_GRAVE = 4, D_SKATE = 5,
            D_CANOPY = 6, D_ALLEY = 7, D_SITE = 8, D_POWER = 9, D_HELIPAD = 10, D_BAY = 11, D_BANDSTAND = 12,
            D_FLOWERS = 13, D_CROPS = 14, D_ROUNDABOUT = 15, D_FLOODLIGHT = 16, D_FOUNTAIN = 17, D_FLAGS = 18,
            D_YARD = 19, D_MAST = 20, D_SALT = 21, D_PILES = 22, D_UMBRELLA = 23, D_LIFEGUARD = 24, D_BOAT = 25,
            D_SHIP = 26, D_CRANE = 27, D_CONTAINERS = 28, D_TENT = 29, D_CAMPFIRE = 30, D_PICNIC = 31, D_FALLS = 32,
            D_SIGN = 33, D_LOOKOUT = 34, D_FIRETOWER = 35, D_LANDMARK = 36, D_STRINGS = 37,
            // ((22.9) Desert City's army test range.)
            D_RANGE = 38;
    static final int FACILITY_POLICE = 0, FACILITY_BASE = 1, FACILITY_HOSPITAL = 2, FACILITY_FIRE = 3;

    /** A police station or military base: where reinforcements come from and a preferred safe zone. */
    static final class Facility {
        final int kind;
        final float x, y, r, gateX, gateY;
        String name;
        /** Rounds of ammunition in the armoury. */
        int ammo;
        boolean dryAnnounced;
        /** Military bases: which kind (see BASE_TYPES). */
        int baseType;
        /** Guard posts (station entrance, base gates) as {x, y}. */
        final List<float[]> posts = new ArrayList<float[]>();
        /** Bases: the alarm is sounding (the dead are at the wire), and time until the next patrol goes out. */
        boolean alarm;
        /** ((11.5) Police stations: everyone free has been sent out in this emergency (said once).) */
        boolean turnedOut;
        float patrolCd = 30, quietFor;
        /** A small town's own police or fire station out in the country (10.20). */
        boolean satellite;
        /** ((16.9) The FBI field office: a police facility of the federal agents' own, not a precinct. */
        boolean fed;
        /** (10.32) An army base the dead have overrun: command moves elsewhere. And how long it's been going. */
        boolean fallen;
        /** (12.3) An army base's trucks in its garages, besides those parked out in the yard (Fleet.stockBases). */
        int spareTrucks;
        float fallingFor;

        Facility(int kind, float x, float y, float r, float gateX, float gateY, String name) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.r = r;
            this.gateX = gateX;
            this.gateY = gateY;
            this.name = name;
        }
    }

    static final int[] ROOFS = {
            0xFF6B6F78, 0xFF7A6A5C, 0xFF5C6670, 0xFF8A7F70, 0xFF6E5D5A, 0xFF59646B, 0xFF7C7C74,
            0xFF4F5A66, 0xFF866E5E, 0xFF6A7468
    };
    static final int[] WALLS = {
            0xFF9A8F80, 0xFFA8746A, 0xFF8C959E, 0xFFB5A58A, 0xFF7E8A7A, 0xFF9B7E6B, 0xFFA0A4A8, 0xFF6F7B88
    };
    /** ((22.8) Map version 38: offices, glass and warehouses in many more colours. */
    static final int[] ROOFS38 = {0xFF6B6F78, 0xFF7A6A5C, 0xFF5C6670, 0xFF8A7F70, 0xFF6E5D5A, 0xFF59646B, 0xFF7C7C74,
            0xFF4F5A66, 0xFF866E5E, 0xFF6A7468, 0xFF3E4650, 0xFF5E4A3E, 0xFF4A5A4A, 0xFF7A5A4A, 0xFF5A5A66, 0xFF8A8478,
            0xFF3A3E44, 0xFF6E7A82};
    static final int[] WALLS38 = {0xFF9A8F80, 0xFFA8746A, 0xFF8C959E, 0xFFB5A58A, 0xFF7E8A7A, 0xFF9B7E6B, 0xFFA0A4A8,
            0xFF6F7B88, 0xFFC8B8A0, 0xFFB86A50, 0xFFD8C8A8, 0xFF8898A8, 0xFFA8B0A0, 0xFFC0A890, 0xFF7A6A8A, 0xFFB8A070,
            0xFF5E6E7A, 0xFFD0C0B0, 0xFF9A5A48, 0xFFE0D8C8};
    static final int[] GLASS_WALLS38 = {0xFF6F8898, 0xFF7C94A4, 0xFF5E7686, 0xFF8898A4, 0xFF6A7E8C, 0xFF4E6A84,
            0xFF8AA6BE, 0xFF3E5A74, 0xFF7A9A8A, 0xFF5A7A6A, 0xFF9AB4C8, 0xFF6A6A84, 0xFFA8B8C0};
    static final int[] WAREHOUSE_ROOFS38 = {0xFF8A9096, 0xFF7C848A, 0xFF9AA0A4, 0xFF6F777D, 0xFF7D8A80, 0xFF8E5A44,
            0xFF4E6A5E, 0xFF5A6E7E, 0xFFA0A8B0, 0xFF6E5A4A, 0xFF3E5A74, 0xFF7A3A2A};
    static final int[] WAREHOUSE_WALLS38 = {0xFF8A8F94, 0xFF9C8F7F, 0xFF7F8A8F, 0xFF8F8575, 0xFFB86A50, 0xFF7E9A88,
            0xFF8FA8C0, 0xFFC8B89A, 0xFF9A6A50, 0xFFA8A098};
    static final int[] WAREHOUSE_ROOFS = {0xFF8A9096, 0xFF7C848A, 0xFF9AA0A4, 0xFF6F777D, 0xFF7D8A80};
    static final int[] WAREHOUSE_WALLS = {0xFF8A8F94, 0xFF9C8F7F, 0xFF7F8A8F, 0xFF8F8575};
    static final int[] CAR_COLORS = {
            0xFFB03A2E, 0xFF2E5FB0, 0xFFE0E0E0, 0xFF222428, 0xFFD4A21C, 0xFF3C8A4E, 0xFF8A8F96, 0xFF6B2E8A
    };
    static final float[] PARK_CHANCE = {0f, 0.07f, 0.15f, 0.35f};
    static final float[] CAR_CHANCE = {0f, 0.018f, 0.05f};

    /** A building footprint (world units) with a height, drawn standing up by GameView. */
    static final class Building {
        final float x0, y0, x1, y1;
        float height;
        /** ((14.2) Its height and room before it fell, to build it back to; and seconds left of the work on it.) */
        float builtHeight;
        int builtCapacity;
        transient float rebuildT;
        /**
         * ((17.1) Walking in: whether people can (0 not worked out yet, 1 yes, 2 no), its floor plan, its walls as
         * {x0, y0, x1, y1} after each other (the outside walls with a gap for the door, and the rooms'), where the
         * doorway is from inside and out, and how many are in it now.)
         */
        transient byte enter;
        transient Interiors.Plan plan;
        transient float[] walls;
        transient float inX, inY, gapX, gapY;
        transient int inside;
        final int roof, wall, seed, kind;
        /** Civilians can hide inside homes, offices and warehouses: the door, room and barricade. */
        float doorX, doorY, barricade = 100, calmTimer;
        int capacity;
        /** How many people live here: families in houses, flats in apartment blocks, a flat over some shops. */
        int residents;
        final List<Entity> occupants = new ArrayList<Entity>();
        /** Blast damage: a building knocked down to 0 collapses into rubble. */
        float hp, maxHp;
        boolean collapsed;
        /** Churches, schools and supermarkets have names (they can become safe zones). */
        String name;
        /** Supermarkets and gun stores: rounds of ammo on the shelves; pharmacies: medicine left. */
        int stock;
        /** Food for anyone sheltering here (and for foragers). */
        int food;
        /** Shops: 0 a general store, 2 a diner (1 was a gun store). */
        int shopType;
        /** What kind of building it is beyond its basic kind (see {@link Variants}), or -1. */
        int variant = -1;
        /** (10.52) A wing of a building that isn't a box: the building it's part of (null for the building itself). */
        Building main;

        /** "Café", "Factory", "Bank"... or null for a plain building. */
        String typeName() {
            Variants.V v = Variants.get(variant);
            return v != null ? v.name : null;
        }
        /** Zombies shut inside, waiting. Nobody knows until they burst out (then {@link #infestKnown}). */
        int lurkers;
        /**
         * A fight inside: the dead got in while people were still there (lurkers and occupants together).
         * The one the defenders are fighting now has intruderHp left; flash is a muzzle flash to draw.
         */
        float intruderHp, fightTime, flash, fightSaid;
        boolean fighting;
        /** A survivor group has made this building its home. */
        World.Holdout holdout;
        /** People inside on an errand (shopping, at church, at school), and how many are on their way. */
        final List<Entity> visitors = new ArrayList<Entity>();
        int heading;
        boolean infestKnown;
        /** Shop windows smashed in, and shelves stripped bare. */
        boolean smashed, looted;
        /**
         * How it looks after what it's been through (10.17, for drawing only): soot from a fire (0 to 1),
         * windows shot out in a fight inside (0 to 1), and the door broken in by the dead.
         */
        float scorch, shotUp;
        /** ((11.5) Variants.U_ use, worked out when first wanted; -1 not yet.) */
        int use = -1;
        boolean doorBroken;
        /** Whether survivors inside running out of food has been reported (kept in saves). */
        boolean outOfFood;
        /** Path to the door (made when someone first needs it). */
        int[] field;
        /** Bullet holes and scorch marks on the walls: {side, along, up, size} each, in a ring buffer. */
        // ((13.3) Made with the first mark: most buildings are never hit, and sixteen thousand empty ones were 12 MB.)
        float[] marks;
        int markCount, markNext;

        float[] marks() {
            if (marks == null) marks = new float[MAX_MARKS * 4];
            return marks;
        }

        void mark(int side, float along, float up, float size) {
            float[] marks = marks();
            int i = markNext * 4;
            marks[i] = side;
            marks[i + 1] = along;
            marks[i + 2] = up;
            marks[i + 3] = size;
            markNext = (markNext + 1) % MAX_MARKS;
            if (markCount < MAX_MARKS) markCount++;
        }

        Building(float x0, float y0, float x1, float y1, float height, int roof, int wall, int seed, int kind) {
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
            this.height = height;
            this.roof = roof;
            this.wall = wall;
            this.seed = seed;
            this.kind = kind;
        }
    }

    static final int MAX_MARKS = 48;
    static final float FLOOR = 12f;
    static final float TREE_HEIGHT = 14f;

    final CityConfig cfg;
    /** ((22.9) Desert City (CityConfig.desert): everything it changes in building and drawing the map is for it alone. */
    final boolean desert;
    /** ((22.9) Desert City: ground kept green (the oases, the parks, gardens round water); null elsewhere. */
    boolean[] lush;
    /** ((25.0) Theme Park Island (CityConfig.themePark): everything it changes in building and drawing the map is for it alone. */
    final boolean themePark;
    /** ((25.0) Theme Park Island: the park inside its ring of streets ({x0, y0, x1, y1} in tiles), and the mainland's shore. */
    int[] parkRect;
    /** ((26.0) The Space Center (CityConfig.spaceCenter): everything it changes in building and drawing the map is for it alone. */
    final boolean spaceCenter;
    /** Where the city is: its look and its names. */
    final Country country;
    /** The town's name, the same every time for a given city code. */
    final String name;
    final int w, h;
    final byte[] tiles;
    final boolean[] solid;
    final boolean[] opaque;
    /** ((24.0) Not final: each changes places with a spare table when a new search is done - see commit.) */
    int[] humanDist, zombieDist;
    final int[] queue;
    final Random rnd;
    /**
     * ((22.1) The map picture, in tiles of TILE_PX square: drawn whole once into one big picture and cut up, the big
     * one let go. On a phone the screen keeps its own copy of every picture it draws, and copies it over again
     * whenever it changes: the whole map was one picture of up to 75 MB, copied over every time an army truck left
     * or came back to its bay (more than once a second in an outbreak). Now only the tile that changed is.)
     */
    Bitmap[] mapTiles;
    int mapCols, mapRows, mapW, mapH;
    static final int TILE_PX = 512;
    Bitmap.Config mapConfig;
    /** (Only while the whole map is being drawn; null otherwise.) */
    Bitmap bitmap;
    float maxHeight;
    /** The Graphics setting: realistic ground, shadows and roofs, or the classic flat look. */
    static volatile boolean realistic = true;
    /** Which look the map bitmap was last drawn with. */
    boolean drawnRealistic;
    final float detail;

    // ------------------------------------------------------------------ districts

    static final int DT_DOWNTOWN = 0, DT_MIDTOWN = 1, DT_OLDTOWN = 2, DT_SUBURB = 3, DT_INDUSTRIAL = 4, DT_CAMPUS = 5,
            DT_PARKSIDE = 6, DT_COUNT = 7;
    static final String[] DISTRICT_KINDS = {"Downtown", "Midtown", "Old Town", "Suburb", "Industrial", "University",
            "Parkside"};

    /** A neighbourhood of the city with its own character: tall towers, shops, old streets, homes or industry. */
    static final class District {
        int type;
        float x, y;
        String name;
        /** The middle of the district's area (world units) and how many tiles it covers. */
        float cx, cy;
        int tiles;
        /** ((16.3) Hardly any dry land of its own (one out at sea, off the islands): no name drawn for it. */
        boolean unnamed;
        /** ((22.8) Its character (colours, paving, landmarks), or null. */
        Neighbourhoods.Theme theme;

        /** Big enough, and dry enough, to have its name on the map. */
        boolean labelled() {
            return tiles >= 40 && !unnamed;
        }
    }

    final List<District> districts = new ArrayList<District>();
    /** Which district each tile belongs to. */
    OffHeap.Bytes districtAt;

    int districtIndex(float x, float y) {
        if (districtAt == null) return -1;
        return districtAt.get(tileIndex(x, y));
    }

    District districtOf(float x, float y) {
        if (districtAt == null || districts.isEmpty()) return null;
        return districts.get(districtAt.get(tileIndex(x, y)));
    }

    int districtType(int tx, int ty) {
        if (districtAt == null) return DT_MIDTOWN;
        tx = Math.max(0, Math.min(w - 1, tx));
        ty = Math.max(0, Math.min(h - 1, ty));
        return districts.get(districtAt.get(ty * w + tx)).type;
    }

    static final int SURF_ASPHALT = 0, SURF_COBBLE = 1, SURF_CONCRETE = 2, SURF_CHIPSEAL = 3;

    /** A street: a band of road tiles {x0, y0, x1, y1} (x1/y1 exclusive) running one way, with its name. */
    static final class Street {
        final int x0, y0, x1, y1, width;
        final boolean vertical, main;
        String name;

        Street(int x0, int y0, int x1, int y1, boolean vertical, boolean main) {
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
            this.vertical = vertical;
            this.main = main;
            width = vertical ? x1 - x0 : y1 - y0;
        }
    }

    final List<Street> streets = new ArrayList<Street>();
    /** Road direction per tile: 0 none, 1 north-south, 2 east-west, 3 junction. */
    final byte[] roadDir;
    /** Tiles that are part of a main road. */
    final boolean[] mainRoad;
    /** Block rectangles (including their sidewalk ring) as {x0, y0, x1, y1}, x1/y1 exclusive. */
    final List<int[]> blocks = new ArrayList<int[]>();

    /** The town's blocks (between its streets): {x0, y0, x1, y1, ...} in tiles. */
    List<int[]> blocks() {
        return blocks;
    }
    /** 0 normal car, 1 police car, 2 army truck. */
    OffHeap.Bytes carKind;
    final List<float[]> helipads = new ArrayList<float[]>();
    final List<float[]> tents = new ArrayList<float[]>();
    /**
     * Military bases come in kinds, each laid out its own way inside the same fence and barracks: an air base
     * with a runway and hangars, an armoured base with its tank park, a training camp with ranges and an
     * assault course, and so on. (Chosen with numbers of their own, so the rest of the city doesn't move.)
     */
    static final String[] BASE_TYPES = {"Garrison", "Air Base", "Armoured Base", "Training Camp", "Special Forces Base",
            "Supply Depot", "Radar Station", "Guard Headquarters"};
    static final int BT_GARRISON = 0, BT_AIR = 1, BT_ARMOUR = 2, BT_TRAINING = 3, BT_SPECIAL = 4, BT_SUPPLY = 5,
            BT_RADAR = 6, BT_GUARD = 7;
    static final int BD_RUNWAY = 0, BD_HANGAR = 1, BD_TANK = 2, BD_RANGE = 3, BD_OBSTACLE = 4, BD_RADAR = 5,
            BD_DISH = 6, BD_FUEL = 7, BD_CONTAINER = 8, BD_MAST = 9, BD_SANDBAGS = 10, BD_KILLHOUSE = 11, BD_JET = 12,
            BD_SHED = 13, BD_FLAG = 14, BD_HARDSTAND = 15,
            // ((15.10) The airport: an airliner parked at the terminal.)
            BD_AIRLINER = 16;
    /** What's inside a base beyond barracks: {kind, x0, y0, x1, y1} in world units. */
    final List<float[]> baseDecor = new ArrayList<float[]>();
    final List<float[]> paths = new ArrayList<float[]>();
    /** Curved streets (cities since 10.25): centre lines {x, y, x, y, ...} in tiles, drawn as smooth roads. */
    final List<float[]> curves = new ArrayList<float[]>();
    /** Tiles a curved street was laid over (drawn as pavement under the smooth road). */
    boolean[] curveTile;
    final List<Facility> facilities = new ArrayList<Facility>();
    final List<float[]> decor = new ArrayList<float[]>();
    /** Gas pumps as {x, y, alive}: an explosion nearby sets them off. */
    final List<float[]> pumps = new ArrayList<float[]>();
    /** Building lots: {x, y, w, h, roof, seed, kind, floors, wall} in tiles. */
    final List<int[]> buildingLots = new ArrayList<int[]>();
    final List<Building> buildings = new ArrayList<Building>();
    /** Everyone the city's homes can house. */
    int totalResidents;
    /** Which building stands on each tile (index into buildings), or -1. */
    /** ((16.11) Kept outside the game's memory: see OffHeap.) */
    final OffHeap.Ints buildingAt;
    /** Tree canopies as {x, y, radius}; drawn above the ground by GameView. */
    final List<float[]> trees = new ArrayList<float[]>();
    /** Street lamps as {x, y}. */
    final List<float[]> lamps = new ArrayList<float[]>();
    final List<int[]> statues = new ArrayList<int[]>();
    /** Open spaces where a safe zone can be set up, as {x, y, kind}: 0 park, 1 plaza, 2 parking lot. */
    final List<float[]> openAreas = new ArrayList<float[]>();
    int origin;


    /**
     * ((16.9) How many pixels across the map picture may be: 6144, from 4096, made for a Galaxy S26 Ultra. Its pixels
     * are kept outside the game's own memory, and at 16 bits a pixel that's 75 MB, under the 100 MB a picture may be to
     * be drawn at all. Half as sharp again on every big map.)
     */
    static final float MAP_PIXELS = 6144f;

    City(CityConfig cfg) {
        // The map picture is kept to MAP_PIXELS across, so a big map still fits.
        this(cfg, Math.min(1f, MAP_PIXELS / (cfg.tiles() * T)));
    }

    /** A city; with a detail below 1 the map bitmap is drawn smaller (for the New Game preview). */
    /** (10.32) How far along building the city is (0 to 1) and what it's doing, for the loading screen. */
    static volatile float progress;
    static volatile String stage = "";

    static void progress(float p, String what) {
        progress = p;
        stage = what;
    }

    City(CityConfig cfg, float detail) {
        progress(0.03f, "Laying out the streets");
        this.cfg = cfg;
        this.desert = cfg.desert();
        this.themePark = cfg.themePark();
        this.spaceCenter = cfg.spaceCenter();
        this.detail = detail;
        w = h = cfg.tiles();
        rnd = new Random(cfg.seed);
        Variants.extra = new Random(cfg.seed * 7919L + 101);
        Random nameRnd = new Random(cfg.seed * 31 + 7);
        country = Country.get(cfg.country());
        name = country.townName(nameRnd);
        tiles = new byte[w * h];
        solid = new boolean[w * h];
        opaque = new boolean[w * h];
        humanDist = new int[w * h];
        zombieDist = new int[w * h];
        queue = new int[w * h];
        roadDir = new byte[w * h];
        mainRoad = new boolean[w * h];
        carKind = new OffHeap.Bytes(w * h);
        buildingAt = new OffHeap.Ints(w * h);
        buildingAt.fill(-1);
        gen.generate();
        roadGen.clearHighwaySides();
        roadGen.joinRoads();
        progress(0.4f, "Putting up the buildings");
        // (10.52) Map version 24: buildings of more shapes than a box.
        if (cfg.civic >= 24) gen.shapeLots();
        if (cfg.driveways()) {
            // The city as generated, its driveways, and only then the Build tool's edits on top (as they were
            // made on it), so a city rebuilt from its code comes out the same as the one that was edited.
            int generated = buildingLots.size();
            gen.computeSolid();
            for (int i = 0; i < generated; i++) gen.createBuilding(buildingLots.get(i));
            roadGen.driveways();
            if (cfg.servicePads()) lotGen.servicePads();
            // Again now the drives are in: a lane they made into a bit of paved street carries on as dirt.
            if (cfg.civic >= 8) roadGen.unpaveLaneStubs();
            if (cfg.civic >= 12) roadGen.connectStations();
            if (cfg.civic >= 19) roadGen.tidyRoads();
            if (cfg.civic >= 22) roadGen.mendLanePinches();
            for (int[] e : cfg.edits) applyEdit(e[0], e[1], e[2], false);
            gen.computeSolid();
            for (int i = generated; i < buildingLots.size(); i++) gen.createBuilding(buildingLots.get(i));
        } else {
            for (int[] e : cfg.edits) applyEdit(e[0], e[1], e[2], false);
            gen.computeSolid();
            for (int[] l : buildingLots) gen.createBuilding(l);
        }
        // ((15.10) The airport's terminal.)
        if (airport != null)
            for (Building b : buildings)
                if (b.kind == JAIL && b.x0 >= airport[0] * T && b.x1 <= airport[2] * T && b.y0 >= airport[1] * T && b.y1 <= airport[3] * T) {
                    b.name = "Airport Terminal";
                    b.variant = -1;
                    b.capacity = 0;
                }
        // ((26.0) The Vehicle Assembly Building takes in as many as a mall.)
        if (spaceCenter)
            for (Building b : buildings) {
                Variants.V vv = Variants.get(b.variant);
                if (vv != null && "Vehicle Assembly Building".equals(vv.name)) b.capacity = 150;
            }
        // ((15.6) The prison's buildings: the cell blocks lettered, the admin block, nobody sheltering in any of them.)
        if (prison != null) {
            char letter = 'A';
            for (Building b : buildings) {
                if (b.x0 < prison[0] * T || b.x1 > prison[2] * T || b.y0 < prison[1] * T || b.y1 > prison[3] * T) continue;
                if (b.kind == JAIL) {
                    b.name = b.x1 - b.x0 <= 3 * T + 1 ? "Gatehouse" : b.y1 - b.y0 <= 3 * T + 1 ? "Prison Admin" : "Cell Block " + letter++;
                    // (Not the county jail's look, with its sign on the roof.)
                    b.variant = -1;
                }
                b.capacity = 0;
                b.residents = 0;
            }
        }
        // (A city built before 10.20 gets its police and rescue helipads too, on open ground as it is,
        // without touching a tile, so it still rebuilds exactly as it was.)
        if (!cfg.servicePads()) {
            padsOnly = true;
            lotGen.servicePads();
            padsOnly = false;
        }
        // ((10.63) Map version 25: every map about as populous as the next, whatever its size or kind.)
        if (cfg.civic >= 25) gen.balancePopulation(cfg.residentsTarget());
        gen.clearDoorways();
        Arrays.fill(humanDist, FAR);
        Arrays.fill(zombieDist, FAR);
        abortFields();
        // ((11.5) Anything worked out from the tiles while the city was being built is let go of: it's done now.)
        clear3 = null;
        clr = null;
        stepC = null;
        // ((16.3) Where the names go on the map: on dry land.)
        gen.placeLabels();
        // ((16.2) Where it's too steep to drive, worked out now rather than at the first drive off road mid-game.)
        if (elev != null) steepAt(0);
        progress(0.5f, "Painting the map");
        // The big map uses 16-bit colour to keep memory down.
        mapW = Math.max(1, (int) (w * T * detail));
        mapH = Math.max(1, (int) (h * T * detail));
        mapConfig = w * detail > 140 ? Bitmap.Config.RGB_565 : Bitmap.Config.ARGB_8888;
        redraw();
    }

    // ------------------------------------------------------------------ the Build tool

    static final int ED_ROAD = 0, ED_PAVE = 1, ED_GRASS = 2, ED_TREES = 3, ED_WALL = 4, ED_HOUSE = 5, ED_SHOP = 6, ED_CLEAR = 7;

    /**
     * Changes the map: paints a tile, or puts up a house or a shop. While generating, before the buildings are
     * made (live = false); or during a game (live = true), when the building is made at once. Returns the new
     * building, or null.
     */
    Building applyEdit(int tx, int ty, int kind, boolean live) {
        // ((11.5) An edit made while the city is built: the tables made from the tiles are worked out afresh after.
        // One made during a game puts them right tile by tile, as it goes - see solidChanged.)
        if (!live) {
            stepC = null;
            clear3 = null;
        clr = null;
        }
        if (live) renderVersion++;
        if (tx < 1 || ty < 1 || tx >= w - 1 || ty >= h - 1) return null;
        int i = ty * w + tx;
        if (kind == ED_HOUSE || kind == ED_SHOP) {
            int lw = kind == ED_HOUSE ? 2 : 3, lh = 2;
            if (tx + lw >= w || ty + lh >= h) return null;
            for (int y = ty; y < ty + lh; y++)
                for (int x = tx; x < tx + lw; x++) {
                    byte t = tiles[y * w + x];
                    if (t == BUILDING || t == ROAD || t == RAIL || t == BASE || buildingAt.get(y * w + x) >= 0) return null;
                }
            lotGen.addLot(tx, ty, lw, lh, kind == ED_HOUSE ? HOUSE : SHOP);
            if (!live) return null;
            for (int y = ty; y < ty + lh; y++)
                for (int x = tx; x < tx + lw; x++) {
                    solid[y * w + x] = true;
                    solidChanged(y * w + x);
                    opaque[y * w + x] = true;
                    roadDir[y * w + x] = 0;
                }
            Building nb = gen.createBuilding(buildingLots.get(buildingLots.size() - 1));
            if (nb != null) clearDoorway(nb);
            return nb;
        }
        if (buildingAt.get(i) >= 0 && !buildings.isEmpty()) {
            // Only Clear touches a building: it knocks it down.
            if (kind != ED_CLEAR || !live) return null;
            Building b = buildings.get(buildingAt.get(i));
            if (!b.collapsed) collapse(b);
            return null;
        }
        byte t = tiles[i];
        if (t == BUILDING || t == RAIL || t == BASE) return null;
        byte nt = kind == ED_ROAD ? ROAD : kind == ED_PAVE ? SIDEWALK : kind == ED_TREES ? TREE : kind == ED_WALL ? FENCE : GRASS;
        tiles[i] = nt;
        roadDir[i] = (byte) (nt == ROAD ? 3 : 0);
        if (live) {
            solid[i] = nt == TREE || nt == FENCE;
            solidChanged(i);
            opaque[i] = false;
            // (The walking costs of the pavement and road change too.)
            if (walkCost != null) walkCost[i] = (byte) (nt != ROAD ? 1 : 8);
        }
        return null;
    }

    /** After live edits: routes to the police stations, base and hospital are worked out again. */
    void editsDone() {
        facilityFields.clear();
        fieldsNeeded = 0;
    }

    /**
     * (10.31) The way on foot to each facility (police stations, the base, the hospital...), worked out the first
     * time someone needs it, and only the most recently used kept: a Gigantic city has dozens of facilities, and
     * one of these for each took most of a phone's memory. ((10.54) 32 MB of them at most, not 96.)
     */
    final java.util.LinkedHashMap<Facility, WalkField> facilityFields = new java.util.LinkedHashMap<Facility, WalkField>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<Facility, WalkField> eldest) {
            // ((10.55) But never fewer than there are stations, bases and hospitals: on a Gigantic map with twenty of
            // them, keeping only a dozen meant people heading for the nearest station had theirs worked out again
            // and again, and the game crawled with hardly a zombie about.)
            return size() > Math.max(fieldsNeeded(), (int) (24L * 1024 * 1024 / (2L * w * h)));
        }
    };
    int[] fieldTmp;
    int fieldsNeeded;
    /** ((13.3) Facilities whose way on foot is out of date (a building came down), to be worked out again in turn. */
    final java.util.LinkedHashSet<Facility> staleFields = new java.util.LinkedHashSet<Facility>();

    /** (10.55) How many facilities anyone walks to (all but the fire stations and the hospital), and a couple to spare. */
    int fieldsNeeded() {
        if (fieldsNeeded == 0) {
            int n = 2;
            for (Facility f : facilities) if (f.kind != FACILITY_FIRE && f.kind != FACILITY_HOSPITAL) n++;
            fieldsNeeded = Math.max(8, n);
        }
        return fieldsNeeded;
    }

    /**
     * ((16.10) How far, in steps of walking cost, the way on foot to a station or a base is worked out: a long walk,
     * not the whole map. Beyond it people find their own way, as to anywhere else. The whole map, for a score of
     * stations and bases, was 60 MB on the Large map - more than a phone's game memory could spare.)
     */
    static final int FACILITY_REACH = 600;

    /** (Kept compact: see WalkField.) */
    WalkField field(Facility f) {
        WalkField a = facilityFields.get(f);
        if (a == null) {
            a = walkField16(f.x, f.y, FACILITY_REACH);
            facilityFields.put(f, a);
        }
        return a;
    }

    static final int FAR_C = 0xFFFF;

    /**
     * ((13.3) Works out the way on foot to one more facility nobody has needed yet, ahead of time: a few a second
     * from the start, instead of a dozen at once in the same moment when the panic starts - on a Colossal map that
     * held the game up for seconds, long enough for a phone to say the game wasn't responding.) False when there's
     * nothing left to do.
     */
    boolean warmFacilityField() {
        if (!staleFields.isEmpty()) {
            Facility f = staleFields.iterator().next();
            staleFields.remove(f);
            if (facilityFields.containsKey(f)) facilityFields.put(f, walkField16(f.x, f.y, FACILITY_REACH));
            return true;
        }
        if (facilityFields.size() >= fieldsNeeded()) return false;
        for (int i = 0, n = facilities.size(); i < n; i++) {
            Facility f = facilities.get(i);
            // ((16.5) Nor the hospital, in a city that still has one: nobody walks there.)
            if (f.kind == FACILITY_FIRE || f.kind == FACILITY_HOSPITAL || facilityFields.containsKey(f)) continue;
            field(f);
            return true;
        }
        return false;
    }

    /** (10.54) Drops the facilities' ways on foot (they're worked out again when next wanted). */
    /**
     * ((13.3) The ground has changed a little (a building down, a stretch of quarantine wall up): the ways on foot to
     * the facilities are worked out again one at a time (see warmFacilityField), and the old ones used meanwhile.
     */
    void fieldsStale() {
        staleFields.addAll(facilityFields.keySet());
    }

    void clearFieldCaches() {
        facilityFields.clear();
        staleFields.clear();
    }

    /**
     * ((16.4) A way on foot to one place, kept small. Following one only ever compares a tile with the tiles beside
     * it, and two tiles side by side are never more than a couple of steps apart in walking cost - so each tile keeps
     * its distance in one byte, wrapping round, and the order between neighbours comes out just the same. And only
     * over the ground it reaches: a call's way, worked out a few hundred tiles out, is a box that size, not the whole
     * map. Each was the whole map at two bytes a tile - 6.5 MB on a Large map, dozens kept at once; a player's
     * order, at four, was 13 MB.)
     */
    static final class WalkField {
        int x0, y0, fw, fh;
        byte[] d = new byte[0];

        /** 0: out of reach; 1: the place itself; otherwise 2 + the distance, wrapped round at WRAP. */
        int code(int tx, int ty) {
            int x = tx - x0, y = ty - y0;
            if (x < 0 || y < 0 || x >= fw || y >= fh) return 0;
            return d[y * fw + x] & 0xFF;
        }
    }

    static final int WRAP = 250;

    /** How much further b is than a, from their codes (both 2 or more): exact for neighbours. */
    static int wrapDiff(int b, int a) {
        int dd = (b - a) % WRAP;
        if (dd > WRAP / 2) dd -= WRAP;
        else if (dd < -WRAP / 2) dd += WRAP;
        return dd;
    }

    /** Packs a whole-map walking field (FAR for out of reach) into a WalkField. */
    WalkField packField(int[] dist) {
        int x0 = w, y0 = h, x1 = -1, y1 = -1;
        for (int y = 0, i = 0; y < h; y++)
            for (int x = 0; x < w; x++, i++)
                if (dist[i] < FAR_C) {
                    if (x < x0) x0 = x;
                    if (x > x1) x1 = x;
                    if (y < y0) y0 = y;
                    if (y > y1) y1 = y;
                }
        WalkField f = new WalkField();
        if (x1 < 0) return f;
        f.x0 = x0;
        f.y0 = y0;
        f.fw = x1 - x0 + 1;
        f.fh = y1 - y0 + 1;
        f.d = new byte[f.fw * f.fh];
        for (int y = 0; y < f.fh; y++)
            for (int x = 0, i = (y + y0) * w + x0, o = y * f.fw; x < f.fw; x++, i++, o++) {
                int v = dist[i];
                f.d[o] = (byte) (v >= FAR_C ? 0 : v == 0 ? 1 : 2 + v % WRAP);
            }
        return f;
    }

    /** A walking field to the nearest of n points. */
    WalkField walkFieldFrom(float[] xs, float[] ys, int n) {
        return walkFieldFrom(xs, ys, n, FAR);
    }

    WalkField walkFieldFrom(float[] xs, float[] ys, int n, int limit) {
        int[] tmp = walkScratch();
        walkFieldFromPoints(tmp, xs, ys, n, limit);
        return packField(tmp);
    }

    /** Whether a walking field reaches (x, y). */
    boolean reaches(WalkField f, float x, float y) {
        return f != null && f.code((int) (x / T), (int) (y / T)) != 0;
    }

    /** (10.31) A walking field to (x, y). */
    WalkField walkField16(float x, float y) {
        return walkField16(x, y, FAR);
    }

    /** (10.55) The same, only searching out to limit (people walk to a park nearby, not across the map). */
    WalkField walkField16(float x, float y, int limit) {
        int[] tmp = walkScratch();
        walkFieldFromPoints(tmp, new float[]{x}, new float[]{y}, 1, limit);
        return packField(tmp);
    }

    /**
     * ((16.10) The one whole-map scratch array the ways on foot and the vehicles' route searches share: they're never
     * worked out at the same moment, and each was 12.5 MB on the Large map. A way on foot fills all of it, so the next
     * route search clears all of it first (see driveField).)
     */
    int[] scratch() {
        if (fieldTmp == null) fieldTmp = new int[w * h];
        return fieldTmp;
    }

    /** The scratch array for a way on foot, which leaves all of it written: the next route search clears it all. */
    int[] walkScratch() {
        int[] a = scratch();
        if (dfLast == a) dfTouchedN = -1;
        return a;
    }
    /** While drawing a close-up: only this region (in tiles, with a margin). */
    boolean regionOnly;
    int rx0, ry0, rx1, ry1;
    float rwx0, rwy0, rwx1, rwy1;
    /** Goes up whenever the map picture changes after it's drawn (rubble, scorch marks, edits). */
    volatile int renderVersion;

    /**
     * Small changes to the map picture since the view last looked (a truck gone from its bay, a burnt-out car,
     * rubble): {x0, y0, x1, y1} each. Only the close-ups over them are drawn again; a change to renderVersion
     * throws them all away, which used to stall the game and blur the whole screen every time a truck moved.
     */
    final java.util.concurrent.ConcurrentLinkedQueue<float[]> changes = new java.util.concurrent.ConcurrentLinkedQueue<float[]>();

    void changed(float x0, float y0, float x1, float y1) {
        if (changes.size() < 200) changes.add(new float[]{x0, y0, x1, y1});
        else renderVersion++;
    }
    int gy0() { return regionOnly ? ry0 : 0; }

    /** True when drawing a close-up and this box (with room for shadows) is nowhere near it. */
    boolean offRegion(float x0, float y0, float x1, float y1) {
        return regionOnly && (x1 < rwx0 - 90 || x0 > rwx1 + 30 || y1 < rwy0 - 90 || y0 > rwy1 + 30);
    }

    /**
     * A sharp close-up of part of the map: the square from (wx0, wy0), size world units across, drawn at res
     * pixels per world unit, exactly as the whole map is drawn (just more finely).
     */
    synchronized Bitmap renderRegion(float wx0, float wy0, float size, float res) {
        int px = Math.max(1, Math.round(size * res));
        Bitmap out = Bitmap.createBitmap(px, px, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(out);
        c.scale(res, res);
        c.translate(-wx0, -wy0);
        c.clipRect(wx0, wy0, wx0 + size, wy0 + size);
        setRes(res);
        regionOnly = true;
        rwx0 = wx0;
        rwy0 = wy0;
        rwx1 = wx0 + size;
        rwy1 = wy0 + size;
        rx0 = Math.max(0, (int) (wx0 / T) - 3);
        ry0 = Math.max(0, (int) (wy0 / T) - 3);
        rx1 = Math.min(w, (int) ((wx0 + size) / T) + 3);
        ry1 = Math.min(h, (int) ((wy0 + size) / T) + 3);
        try {
            art.render(c);
            for (Building b : buildings) if (b.collapsed && !offRegion(b.x0, b.y0, b.x1, b.y1)) art.drawRubble(c, b);
            for (int[] t : charred) charTile(c, t[0], t[1]);
        } finally {
            regionOnly = false;
        }
        return out;
    }

    /**
     * Lettering on the roofs (shop names, POLICE, FIRE...): kept as text and drawn over the map by the view at
     * screen resolution, instead of into the map picture where it blurs as soon as you zoom in.
     */
    static final class Label {
        final String text;
        final float x, y, size;
        final int color, building;
        /** (10.52) A sign on the roof: (x, y) is its middle, and it's drawn on a plate. */
        boolean sign;

        Label(String text, float x, float y, float size, int color, int building) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.size = size;
            this.color = color;
            this.building = building;
        }
    }

    final List<Label> labels = new ArrayList<Label>();
    boolean collectLabels;

    void label(String text, float x, float y, float size, int color) {
        if (!collectLabels) return;
        int tx = Math.max(0, Math.min(w - 1, (int) (x / T))), ty = Math.max(0, Math.min(h - 1, (int) (y / T)));
        labels.add(new Label(text, x, y, size, color, buildingAt.get(ty * w + tx)));
    }

    /** (10.52) A roof sign centred on (x, y), on a plate: it reads the same on a flat roof or across a ridge. */
    void sign(String text, float x, float y, float size, int color) {
        if (!collectLabels) return;
        label(text, x, y, size, color);
        labels.get(labels.size() - 1).sign = true;
    }

    /** Draws (or redraws, after the Graphics setting changes) the ground bitmap. */
    synchronized void redraw() {
        renderVersion++;
        labels.clear();
        trees.clear();
        computeJunctions();
        bitmap = Bitmap.createBitmap(mapW, mapH, mapConfig);
        Canvas canvas = new Canvas(bitmap);
        if (detail != 1f) canvas.scale(detail, detail);
        setRes(detail);
        drawnRealistic = realistic;
        collectLabels = true;
        art.render(canvas);
        collectLabels = false;
        for (Building b : buildings) if (b.collapsed) art.drawRubble(canvas, b);
        cutTiles();
        // (Sorted top to bottom, so the view only looks at the rows of trees on screen.)
        java.util.Collections.sort(trees, new java.util.Comparator<float[]>() {
            @Override
            public int compare(float[] a, float[] b) {
                return Float.compare(a[1], b[1]);
            }
        });
    }

    /** ((22.1) The whole map picture just drawn, cut into tiles; the big picture let go. */
    void cutTiles() {
        int cols = (mapW + TILE_PX - 1) / TILE_PX, rows = (mapH + TILE_PX - 1) / TILE_PX;
        Bitmap[] t = mapTiles != null && mapCols == cols && mapRows == rows ? mapTiles : new Bitmap[cols * rows];
        android.graphics.Rect src = new android.graphics.Rect();
        RectF dst = new RectF();
        for (int ty = 0; ty < rows; ty++)
            for (int tx = 0; tx < cols; tx++) {
                int x0 = tx * TILE_PX, y0 = ty * TILE_PX, tw = Math.min(TILE_PX, mapW - x0), th = Math.min(TILE_PX, mapH - y0);
                Bitmap b = t[ty * cols + tx];
                if (b == null || b.getWidth() != tw || b.getHeight() != th) t[ty * cols + tx] = b = Bitmap.createBitmap(tw, th, mapConfig);
                src.set(x0, y0, x0 + tw, y0 + th);
                dst.set(0, 0, tw, th);
                new Canvas(b).drawBitmap(bitmap, src, dst, null);
            }
        mapTiles = t;
        mapCols = cols;
        mapRows = rows;
        bitmap.recycle();
        bitmap = null;
    }

    /** ((22.1) Something drawn into the map picture between world (x0, y0) and (x1, y1): into each tile it touches. */
    interface MapPainter {
        void paint(Canvas c);
    }

    void paintMap(float x0, float y0, float x1, float y1, MapPainter painter) {
        if (mapTiles == null) return;
        int px0 = Math.max(0, (int) Math.floor(x0 * detail) - 2), py0 = Math.max(0, (int) Math.floor(y0 * detail) - 2);
        int px1 = Math.min(mapW - 1, (int) Math.ceil(x1 * detail) + 2), py1 = Math.min(mapH - 1, (int) Math.ceil(y1 * detail) + 2);
        if (px1 < px0 || py1 < py0) return;
        for (int ty = py0 / TILE_PX; ty <= py1 / TILE_PX; ty++)
            for (int tx = px0 / TILE_PX; tx <= px1 / TILE_PX; tx++) {
                Canvas c = new Canvas(mapTiles[ty * mapCols + tx]);
                c.translate(-tx * TILE_PX, -ty * TILE_PX);
                if (detail != 1f) c.scale(detail, detail);
                c.clipRect(x0, y0, x1, y1);
                painter.paint(c);
            }
    }

    final android.graphics.Rect tileSrc = new android.graphics.Rect();
    final RectF tileDst = new RectF();

    /**
     * ((22.1) Draws the part of the map picture from (sx0, sy0) to (sx1, sy1) - in its pixels - into dst, a tile at a
     * time (the whole map with sx1 < 0).
     */
    void drawMap(Canvas c, float sx0, float sy0, float sx1, float sy1, RectF dst, Paint paint) {
        if (mapTiles == null) return;
        if (sx1 < 0) {
            sx0 = sy0 = 0;
            sx1 = mapW;
            sy1 = mapH;
        }
        float kx = dst.width() / Math.max(1e-3f, sx1 - sx0), ky = dst.height() / Math.max(1e-3f, sy1 - sy0);
        int c0 = Math.max(0, (int) (sx0 / TILE_PX)), c1 = Math.min(mapCols - 1, (int) ((sx1 - 1e-3f) / TILE_PX));
        int r0 = Math.max(0, (int) (sy0 / TILE_PX)), r1 = Math.min(mapRows - 1, (int) ((sy1 - 1e-3f) / TILE_PX));
        for (int ty = r0; ty <= r1; ty++)
            for (int tx = c0; tx <= c1; tx++) {
                Bitmap b = mapTiles[ty * mapCols + tx];
                float ox = tx * TILE_PX, oy = ty * TILE_PX;
                float ix0 = Math.max(sx0, ox), iy0 = Math.max(sy0, oy), ix1 = Math.min(sx1, ox + b.getWidth()), iy1 = Math.min(sy1, oy + b.getHeight());
                if (ix1 <= ix0 || iy1 <= iy0) continue;
                tileSrc.set((int) Math.floor(ix0 - ox), (int) Math.floor(iy0 - oy), (int) Math.ceil(ix1 - ox), (int) Math.ceil(iy1 - oy));
                tileDst.set(dst.left + (ox + tileSrc.left - sx0) * kx, dst.top + (oy + tileSrc.top - sy0) * ky,
                        dst.left + (ox + tileSrc.right - sx0) * kx, dst.top + (oy + tileSrc.bottom - sy0) * ky);
                c.drawBitmap(b, tileSrc, tileDst, paint);
            }
    }

    /** The first tree (in the sorted list) at or below y. */
    int firstTreeAt(float y) {
        int lo = 0, hi = trees.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (trees.get(mid)[1] < y) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }
    /**
     * Water: what kind the map has (CityConfig.W_), the first row of the sea (or -1), the river's first row
     * and width (or -1), and which road and pavement tiles are bridges over it.
     */
    int waterKind, seaY = -1;
    boolean[] bridge;

    // ------------------------------------------------------------------ the lie of the land (see Terrain)

    /** Each tile's height (null on cities built before 10.10, which are flat). */
    OffHeap.Floats elev;
    /** What kind each tree is (Country.TK_: broadleaf unless set; null on cities built before 10.10). */
    OffHeap.Bytes treeKind;
    /** Waterfalls: {x, y, dx, dy} (world units; the way the water falls). */
    final List<float[]> falls = new ArrayList<float[]>();
    /** Names out in the country (the park, mountains, a lake, the falls, campsites) and where: {x, y, kind}. */
    final List<String> natureNames = new ArrayList<String>();
    final List<float[]> natureSpots = new ArrayList<float[]>();
    static final int NL_PARK = 0, NL_PEAK = 1, NL_LAKE = 2, NL_FALLS = 3, NL_CAMP = 4, NL_LANDMARK = 5;
    /** The nature park (null if there is none), its middle and radius in world units. */
    String parkName;
    float parkX, parkY, parkR;

    // The wilds (10.14): trails drawn as winding lines, the hikes along them, where they cross roads and the
    // railway, the ranger station and the fire lookouts.
    final List<float[]> trailLines = new ArrayList<float[]>();
    /** Walks along the trails from where they start (the trailhead, a campsite, a lane) to something worth seeing. */
    final List<float[]> hikes = new ArrayList<float[]>();
    final List<Integer> hikeKinds = new ArrayList<Integer>();
    static final int HIKE_LOOKOUT = 0, HIKE_FALLS = 1, HIKE_LAKE = 2, HIKE_CAMP = 3, HIKE_TOWER = 4, HIKE_WOODS = 5;
    /** {x, y, tile kind} where a trail crosses a road or the railway. */
    final List<float[]> trailCrossings = new ArrayList<float[]>();
    float trailheadX, trailheadY;
    String rangerStationName;
    /** (10.34) Ranger outposts by the other mountains: where their trucks park, and their names. */
    final List<float[]> outposts = new ArrayList<float[]>();
    final List<String> outpostNames = new ArrayList<String>();
    /** Where the rangers' truck parks. */
    float[] rangerLot;
    /** Fire lookout towers: {x, y}, and their names. */
    final List<float[]> fireTowers = new ArrayList<float[]>();
    final List<String> fireTowerNames = new ArrayList<String>();

    /** Takes away any decoration (a crop field, a garden) under this patch of tiles, for something built there. */
    void clearDecorIn(int tx, int ty, int tw, int th) {
        float x0 = tx * T, y0 = ty * T, x1 = (tx + tw) * T, y1 = (ty + th) * T;
        for (int i = decor.size() - 1; i >= 0; i--) {
            float[] d = decor.get(i);
            if (d[3] > x0 && d[1] < x1 && d[4] > y0 && d[2] < y1) decor.remove(i);
        }
    }

    /** A campsite's middle is here. */
    boolean isCampAt(float x, float y) {
        for (int i = 0; i < natureSpots.size(); i++) {
            float[] q = natureSpots.get(i);
            if (q[2] == NL_CAMP && Math.abs(q[0] - x) < T && Math.abs(q[1] + 30 - y) < T) return true;
        }
        return false;
    }
    final java.util.IdentityHashMap<int[], String> lotNames = new java.util.IdentityHashMap<int[], String>();
    final java.util.IdentityHashMap<int[], Integer> lotResidents = new java.util.IdentityHashMap<int[], Integer>();

    void setTreeKind(int i, byte kind) {
        if (treeKind == null) treeKind = new OffHeap.Bytes(w * h);
        treeKind.set(i, (byte) (kind));
    }

    byte treeKindAt(int i) {
        return treeKind == null ? 0 : treeKind.get(i);
    }

    void unpine(int i) {
        if (treeKind != null) treeKind.set(i, (byte) (0));
    }

    /** Shallow stream water anyone can wade across (since 10.14): it's water, but not in the way. */
    boolean[] ford;

    void setFord(int i, boolean on) {
        if (ford == null) {
            if (!on) return;
            ford = new boolean[w * h];
        }
        ford[i] = on;
    }

    boolean isFord(int i) {
        return ford != null && i >= 0 && i < ford.length && ford[i] && tiles[i] == WATER;
    }

    void setBridge(int i) {
        if (bridge == null) bridge = new boolean[w * h];
        bridge[i] = true;
    }

    void natureLabel(String name, float x, float y, int kind) {
        natureNames.add(name);
        natureSpots.add(new float[]{x, y, kind});
    }

    void addNatureDecor(int kind, float x0, float y0, float x1, float y1, int variant) {
        addDecor(kind, x0, y0, x1, y1, variant);
    }

    /** A building out in the country with its own name (and, if residents >= 0, that many living there). */
    void natureLot(int x, int y, int lw, int lh, int kind, int floors, int roof, int wall, String name, int residents) {
        fill(x, y, lw, lh, BUILDING);
        int[] l = {x, y, lw, lh, roof, rnd.nextInt(100000), kind, floors, wall, -1};
        buildingLots.add(l);
        lotNames.put(l, name);
        lotResidents.put(l, residents);
    }

    /** The ground's height here (0 on a flat map). */
    float elevation(float x, float y) {
        if (elev == null) return 0;
        int tx = (int) (x / T), ty = (int) (y / T);
        if (tx < 0 || ty < 0 || tx >= w || ty >= h) return 0;
        return elev.get(ty * w + tx);
    }

    /** How steeply the ground rises (height per tile) going from (x, y) in direction (dx, dy) (a unit vector). */
    float rise(float x, float y, float dx, float dy) {
        if (elev == null) return 0;
        return elevation(x + dx * T, y + dy * T) - elevation(x, y);
    }

    /** The government buildings (null where the city has none). */
    Building cityHall, courthouse, jail, callCentre, worksDepot, armory;
    /** The armory compound's gate, as {x, y} (world units), or null. */
    float[] armoryGate;

    /** Tiles in front of a door (no street tree is planted on them). */
    boolean[] doorway;

    /** One building's doorway (also for a house put up with the Build tool, as it would be in the code's city). */
    void clearDoorway(Building b) {
        if (b.doorX <= 0) return;
        if (doorway == null) doorway = new boolean[w * h];
        int tx = (int) (b.doorX / T), ty = (int) (b.doorY / T);
        for (int dy = -1; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++) {
                int x = tx + dx, y = ty + dy;
                if (x < 0 || y < 0 || x >= w || y >= h) continue;
                int i = y * w + x;
                doorway[i] = true;
                if (tiles[i] != TREE) continue;
                tiles[i] = isPlazaTree(x, y) ? PLAZA : GRASS;
                solid[i] = false;
            }
    }

    // ------------------------------------------------------------------ (10.40) one road network

    /** (10.40) Drive tiles (a house's or barn's own way out to the road), for drawing them as drives. */
    boolean[] drive;

    float worldW() {
        return w * T;
    }

    float worldH() {
        return h * T;
    }

    // ------------------------------------------------------------------ generation

    /** The railway: tile rows it runs along, or -1 if the map has none. */
    int railY0 = -1, railRows;
    /** Level crossings as {x0, x1} (tile columns), and the middle of the station platform (or -1). */
    final List<int[]> crossings = new ArrayList<int[]>();
    float stationX = -1, stationY;
    String stationName;

    /** True if any tile around the block's edge is part of a main road (shops and businesses line up there). */
    boolean onMainRoad(int x0, int y0, int x1, int y1) {
        for (int x = x0 - 1; x <= x1; x++)
            for (int k = 0; k < 2; k++) {
                int y = k == 0 ? y0 - 1 : y1;
                if (x >= 0 && y >= 0 && x < w && y < h && mainRoad[y * w + x]) return true;
            }
        for (int y = y0 - 1; y <= y1; y++)
            for (int k = 0; k < 2; k++) {
                int x = k == 0 ? x0 - 1 : x1;
                if (x >= 0 && y >= 0 && x < w && y < h && mainRoad[y * w + x]) return true;
            }
        return false;
    }

    /** Lays road tiles for a street, marking junctions where it crosses another. */
    /** (11.1) The tiles of the six-lane boulevards (seven across). */
    boolean[] boulevard;

    /** Marks a split's returned street centre as a north-south street. */
    static final int VERT = 1 << 20;

    /** True if a neighbouring road tile is a junction (cars don't park there). */
    boolean hasNeighborDir(int x, int y) {
        for (int dy = -2; dy <= 2; dy++)
            for (int dx = -2; dx <= 2; dx++) {
                int nx = x + dx, ny = y + dy;
                if (nx >= 0 && ny >= 0 && nx < w && ny < h && roadDir[ny * w + nx] == 3) return true;
            }
        return false;
    }

    /** ((15.5) The row the side gates of a base bh high from y open on: the middle three.) */
    static int gy0(int bh, int y) {
        return y + bh / 2 - 1;
    }

    /** (10.30) Army checkpoints on the road to the country base: {x, y, angle of the road} in world units. */
    final List<float[]> checkpoints = new ArrayList<float[]>();
    /** Where the soldiers stand at them (two each), in world units. */
    final List<float[]> checkpointPosts = new ArrayList<float[]>();

    /** The town's own army base (not the big one out in the country), or any base. */
    Facility mainBase() {
        Facility any = null, standing = null;
        for (Facility f : facilities) {
            if (f.kind != FACILITY_BASE) continue;
            if (!f.satellite && !f.fallen) return f;
            if (any == null) any = f;
            if (standing == null && !f.fallen) standing = f;
        }
        // (10.32) The town's base overrun: the one out in the country takes over.
        return standing != null ? standing : any;
    }

    // ------------------------------------------------------------------ islands

    /** The Islands map: each island's town, as {x0, y0, x1, y1} tiles (ring road included), and its name. */
    int[][] islandRects;
    String[] islandNames;
    /** ((16.3) Where each island's name goes on the map (world x, y), on its own dry land; null without islands. */
    float[] islandLabel;
    /** Where the causeway to the mainland reaches the edge of the map {x, y} (convoys come in over it). */
    float[] causewayEnd;

    /** Which island a tile is on (its town's rectangle, with the shore round it), or -1 out at sea. */
    int island(int tx, int ty) {
        if (islandRects == null) return -1;
        for (int i = 0; i < islandRects.length; i++) {
            int[] q = islandRects[i];
            if (tx >= q[0] - 10 && tx < q[2] + 10 && ty >= q[1] - 10 && ty < q[3] + 10) return i;
        }
        return -1;
    }


    // ------------------------------------------------------------------ water

    List<float[]> beachSpots;

    /**
     * (11.1) Spots on the sand by the water, for a day at the beach: {x, y} at tile centres, every few tiles
     * along every beach on the map (the seaside, the bays, the islands' coves). Empty with no beach.
     */
    List<float[]> beachSpots() {
        if (beachSpots != null) return beachSpots;
        beachSpots = new ArrayList<float[]>();
        for (int y = 2; y < h - 2; y++)
            for (int x = 2; x < w - 2; x++) {
                if (tiles[y * w + x] != SAND || ((x * 3 + y * 5) % 7) != 0) continue;
                boolean water = false;
                for (int d = 1; d <= 6 && !water; d++)
                    for (int[] v : new int[][]{{d, 0}, {-d, 0}, {0, d}, {0, -d}}) {
                        int nx = x + v[0], ny = y + v[1];
                        if (nx >= 0 && ny >= 0 && nx < w && ny < h && tiles[ny * w + nx] == WATER) water = true;
                    }
                if (water && !solid[y * w + x]) beachSpots.add(new float[]{(x + 0.5f) * T, (y + 0.5f) * T});
            }
        return beachSpots;
    }

    /**
     * Lakeside: a big lake in the middle of town, with avenues running along its shores and right across
     * town, and the rest of the town laid out around them.
     */
    /**
     * ((15.6) The Prison Island: the prison compound inside its walls (tiles: x0, y0, x1, y1), the gate in its wall
     * (tiles: x0, y0, x1, y1), and the ferry's two landings (world: x0, y0, x1, y1).)
     */
    int[] prison, prisonGate;
    float[] ferry;
    /** ((15.6) The prison's watchtowers on its wall, where its guards are posted, and the barrier on the seafront: {x0, y, x1}.) */
    final List<float[]> prisonTowers = new ArrayList<float[]>(), prisonPosts = new ArrayList<float[]>();
    float[] prisonBarrier;
    /** (15.6) How thick the prison wall is, in tiles. */
    static final int PRISON_WALL = 2;

    /** (15.10) An airliner from above, nose along angle a, sized u (about a ninth of its length). */
    static void drawAirliner(Canvas c, Paint p, float x, float y, float u, float a, int tail) {
        c.save();
        c.translate(x, y);
        c.rotate((float) Math.toDegrees(a));
        p.setColor(0x40000000);
        c.drawRect(-4.5f * u + 2, -0.55f * u + 2, 4.5f * u + 2, 0.55f * u + 2, p);
        // Wings and tailplane.
        p.setColor(0xFFC8CCD2);
        android.graphics.Path wing = new android.graphics.Path();
        wing.moveTo(0.8f * u, 0);
        wing.lineTo(-1.2f * u, -4.2f * u);
        wing.lineTo(-2.0f * u, -4.2f * u);
        wing.lineTo(-1.0f * u, 0);
        wing.lineTo(-2.0f * u, 4.2f * u);
        wing.lineTo(-1.2f * u, 4.2f * u);
        wing.close();
        c.drawPath(wing, p);
        c.drawRect(-4.4f * u, -1.6f * u, -3.6f * u, 1.6f * u, p);
        // Engines under the wings.
        p.setColor(0xFF8A9098);
        c.drawRect(-0.6f * u, -2.3f * u, 0.4f * u, -1.7f * u, p);
        c.drawRect(-0.6f * u, 1.7f * u, 0.4f * u, 2.3f * u, p);
        // Fuselage, nose and tail fin.
        p.setColor(0xFFF4F6F8);
        c.drawRect(-4.5f * u, -0.55f * u, 4.0f * u, 0.55f * u, p);
        c.drawCircle(4.0f * u, 0, 0.55f * u, p);
        p.setColor(tail);
        c.drawRect(-4.6f * u, -0.25f * u, -3.4f * u, 0.25f * u, p);
        p.setColor(0xFF2A3A4A);
        c.drawRect(3.6f * u, -0.3f * u, 4.1f * u, 0.3f * u, p);
        c.restore();
    }

    /** (15.6) The prison has fallen: its gate opens, and the causeway leads in and out. */
    void openPrisonGate() {
        if (prisonGate == null) return;
        for (int y = prisonGate[1]; y < prisonGate[3]; y++)
            for (int x = prisonGate[0]; x < prisonGate[2]; x++) {
                tiles[y * w + x] = ROAD;
                solid[y * w + x] = false;
                roadDir[y * w + x] = 1;
                // ((16.0) And the walking routes told: the way out was still worked out as through a wall.)
                solidChanged(y * w + x);
            }
        clear3 = null;
        clr = null;
        stepC = null;
        if (mapTiles != null) redrawArea(prisonGate[0] * T - 2 * T, prisonGate[1] * T - T, prisonGate[2] * T + 2 * T, prisonGate[3] * T + 3 * T);
    }

    /**
     * ((15.10) The airport: its fenced grounds (tiles: x0, y0, x1, y1), the runway {x0, y0, x1, y1} (world), where the
     * planes park at the terminal {x, y}, and where people wait to board {x, y}.)
     */
    int[] airport;
    float[] runway, apron, gateArea;

    /** ((22.0) River City's river: ((22.1) which tiles it covers, and how many bridges cross it. */
    boolean[] river;
    int riverBridges;
    /** Running across the map (else down it), and its banks at each point along it. */
    boolean riverAcross;
    int[] riverEdge0, riverEdge1;
    /** ((22.4) The river's true middle and half width along its course (tiles), for drawing smooth banks. */
    float[] riverC, riverHalf;
    /** ((22.1) What each river bridge tile is built of (BK_), or null. */
    byte[] bridgeKind;
    static final byte BK_STONE = 1, BK_STEEL = 2, BK_CONCRETE = 3, BK_WOOD = 4;

    /**
     * An army truck parked on a base (or at the armory) near (x, y) drives off: its spot is cleared, and its
     * place returned as {x, y} so the departing truck starts from there. Null if none is parked nearby.
     */
    float[] takeParkedTruck(float x, float y, float radius) {
        int tx0 = Math.max(0, (int) ((x - radius) / T)), tx1 = Math.min(w - 1, (int) ((x + radius) / T));
        int ty0 = Math.max(0, (int) ((y - radius) / T)), ty1 = Math.min(h - 1, (int) ((y + radius) / T));
        int best = -1;
        float bd = radius * radius;
        for (int ty = ty0; ty <= ty1; ty++)
            for (int tx = tx0; tx <= tx1; tx++) {
                int i = ty * w + tx;
                if (tiles[i] != CAR || carKind.get(i) != 2) continue;
                float dx = (tx + 0.5f) * T - x, dy = (ty + 0.5f) * T - y, d = dx * dx + dy * dy;
                if (d < bd) {
                    bd = d;
                    best = i;
                }
            }
        if (best < 0) return null;
        tiles[best] = BASE;
        carKind.set(best, (byte) (0));
        solid[best] = false;
        stepsChanged(best);
        int bx = best % w, by = best / w;
        // Paint the empty bay over the parked truck (and its shadow) on the map picture.
        final float ex0 = bx * T, ey0 = by * T, ex1 = bx * T + T + 2, ey1 = by * T + T + 2;
        paintMap(ex0, ey0, ex1, ey1, new MapPainter() {
            @Override
            public void paint(Canvas c) {
                Paint p = new Paint();
                p.setColor(0xFF6A6E5E);
                c.drawRect(ex0, ey0, ex1, ey1, p);
            }
        });
        changed(ex0, ey0, ex1, ey1);
        return new float[]{(bx + 0.5f) * T, (by + 0.5f) * T};
    }

    /**
     * (10.49) Where evacuation buses pick people up: up to n places along the main roads in town, spread out
     * across it. Each is {x, y on the road where the bus stops, x, y on the pavement beside it where people wait}.
     */
    List<float[]> evacuationStops(int n, Random r) {
        List<float[]> cand = new ArrayList<float[]>();
        int x0 = Math.max(2, townX0), y0 = Math.max(2, townY0), x1 = Math.min(w - 3, townX1), y1 = Math.min(h - 3, townY1);
        for (int k = 0; k < 6000 && cand.size() < 400; k++) {
            int tx = x0 + r.nextInt(Math.max(1, x1 - x0)), ty = y0 + r.nextInt(Math.max(1, y1 - y0));
            int i = ty * w + tx;
            if (tiles[i] != ROAD || !mainRoad[i] || (oneWay != null && oneWay[i] != 0) || (slipTile != null && slipTile[i])
                    || junctionAt(tx, ty) || onHighway((tx + 0.5f) * T, (ty + 0.5f) * T)) continue;
            int dir = roadDir[i];
            if (dir != 1 && dir != 2) continue;
            // The pavement beside it: across the road from where it runs.
            for (int sd = -1; sd <= 1; sd += 2) {
                int qx = tx, qy = ty;
                for (int step = 0; step < 4; step++) {
                    if (dir == 1) qx += sd;
                    else qy += sd;
                    if (qx < 0 || qy < 0 || qx >= w || qy >= h || tiles[qy * w + qx] != ROAD) break;
                }
                if (qx < 0 || qy < 0 || qx >= w || qy >= h || tiles[qy * w + qx] != SIDEWALK) continue;
                cand.add(new float[]{(tx + 0.5f) * T, (ty + 0.5f) * T, (qx + 0.5f) * T, (qy + 0.5f) * T});
                break;
            }
        }
        List<float[]> out = new ArrayList<float[]>();
        if (cand.isEmpty()) return out;
        // Spread out: each next one as far as it can be from those already chosen (the first nearest the middle).
        float cx = (x0 + x1) * T / 2f, cy = (y0 + y1) * T / 2f;
        float[] first = null;
        float bd = Float.MAX_VALUE;
        for (float[] c : cand) {
            float d = (float) World.hypot(c[0] - cx, c[1] - cy);
            if (d < bd) {
                bd = d;
                first = c;
            }
        }
        out.add(first);
        while (out.size() < n) {
            float[] best = null;
            float far = 0;
            for (float[] c : cand) {
                float near = Float.MAX_VALUE;
                for (float[] o : out) near = Math.min(near, (float) World.hypot(c[0] - o[0], c[1] - o[1]));
                if (near > far) {
                    far = near;
                    best = c;
                }
            }
            if (best == null || far < 30 * T) break;
            out.add(best);
        }
        return out;
    }

    /** (12.3) How many army trucks are parked within radius of (x, y). */
    int parkedTrucks(float x, float y, float radius) {
        int tx0 = Math.max(0, (int) ((x - radius) / T)), tx1 = Math.min(w - 1, (int) ((x + radius) / T));
        int ty0 = Math.max(0, (int) ((y - radius) / T)), ty1 = Math.min(h - 1, (int) ((y + radius) / T));
        int n = 0;
        for (int ty = ty0; ty <= ty1; ty++)
            for (int tx = tx0; tx <= tx1; tx++) {
                int i = ty * w + tx;
                if (tiles[i] != CAR || carKind.get(i) != 2) continue;
                float dx = (tx + 0.5f) * T - x, dy = (ty + 0.5f) * T - y;
                if (dx * dx + dy * dy < radius * radius) n++;
            }
        return n;
    }

    /**
     * ((22.0) A parked car caught by a blast burns out where it stands (kind 7, a blackened shell). True if there was one
     * there to burn.
     */
    boolean burnParkedCar(int i) {
        if (i < 0 || i >= tiles.length || tiles[i] != CAR || carKind.get(i) > 1) return false;
        carKind.set(i, (byte) 7);
        int x = i % w, y = i / w;
        redrawArea(x * T - T, y * T - T, x * T + 2 * T, y * T + 2 * T);
        return true;
    }

    /** An army truck parks in an empty bay on a base (the spot one drove off from). */
    void parkTruck(float x, float y) {
        int tx = (int) (x / T), ty = (int) (y / T);
        if (tx < 0 || ty < 0 || tx >= w || ty >= h) return;
        int i = ty * w + tx;
        if (tiles[i] != BASE) return;
        tiles[i] = CAR;
        carKind.set(i, (byte) (2));
        solid[i] = true;
        clearChanged(i);
        stepsChanged(i);
        final int px = tx, py = ty;
        paintMap(tx * T - 4, ty * T - 4, tx * T + T + 4, ty * T + T + 4, new MapPainter() {
            @Override
            public void paint(Canvas c) {
                art.drawCar(c, new Paint(), px, py);
            }
        });
        changed(tx * T - 4, ty * T - 4, tx * T + T + 4, ty * T + T + 4);
    }

    /** Out in the countryside round a town (or past the edge of town). */
    boolean outOfTown(float x, float y) {
        if (townX0 <= 0) return false;
        int tx = (int) (x / T), ty = (int) (y / T);
        return tx < townX0 || ty < townY0 || tx >= townX1 || ty >= townY1;
    }

    /** A bridge (road or pavement over water). */
    boolean isBridge(int tx, int ty) {
        return bridge != null && tx >= 0 && ty >= 0 && tx < w && ty < h && bridge[ty * w + tx];
    }

    /** ((16.9) The FBI field office's roof colour, which also tells it apart when its roof is painted.) */
    static final int FED_ROOF = 0xFF3A3F47;

    /** Which way the road on this tile runs: 1 north-south, 2 east-west, 3 a junction, 0 not a road. */
    int roadDirAt(int x, int y) {
        return x < 0 || y < 0 || x >= w || y >= h ? 0 : roadDir[y * w + x];
    }

    boolean hasNeighbor(int x, int y, byte type) {
        for (int dy = -1; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx, ny = y + dy;
                if (nx >= 0 && ny >= 0 && nx < w && ny < h && tiles[ny * w + nx] == type) return true;
            }
        return false;
    }

    void fill(int x, int y, int fw, int fh, byte t) {
        for (int j = y; j < y + fh; j++)
            for (int i = x; i < x + fw; i++)
                if (i >= 0 && j >= 0 && i < w && j < h) tiles[j * w + i] = t;
    }

    static final int[] OLD_WALLS = {0xFFA8543A, 0xFFB86A48, 0xFFD8C8A8, 0xFF9A4A38, 0xFFC8B090, 0xFFE0D2B4};
    static final int[] OLD_ROOFS = {0xFF8A4A32, 0xFF6E4A3E, 0xFF5A5E66, 0xFF9A5838, 0xFF7A3E2E};
    static final int[] GLASS_WALLS = {0xFF6F8898, 0xFF7C94A4, 0xFF5E7686, 0xFF8898A4, 0xFF6A7E8C};

    void addDecor(int kind, float x0, float y0, float x1, float y1, int variant) {
        decor.add(new float[]{kind, x0, y0, x1, y1, variant});
    }

    // ------------------------------------------------------------------ the highway

    /** The highway: which way it runs (0 east-west, 1 north-south, -1 there isn't one), its first row or column. */
    int hwyAxis = -1, hwyAt;
    String hwyName, hwyShield;
    /** Which way traffic must go on each tile (0 any; 1 east, 2 west, 3 south, 4 north). */
    byte[] oneWay;
    /** Where roads join the highway: {x, y} tile at the side of the highway, and which side (+1/-1). */
    final List<int[]> hwyExits = new ArrayList<int[]>();

    // ------------------------------------------------------------------ the beltway (10.52)

    static final String BELTWAY = "Beltway";
    /** The beltway's tiles: four lanes right round the map, driven at nearly highway speed. */
    boolean[] beltTile;

    boolean onBelt(float x, float y) {
        if (beltTile == null) return false;
        int tx = (int) (x / T), ty = (int) (y / T);
        return tx >= 0 && ty >= 0 && tx < w && ty < h && beltTile[ty * w + tx];
    }

    /** (10.32) Road bridges over the highway at the interchanges: {x0, y0, x1, y1} in world units. */
    final List<float[]> overpasses = new ArrayList<float[]>();

    /** The bridge over the highway at (x, y), or -1. */
    /** (10.32) Each end of a bridge's lanes, and the tile at the other end of it. */
    final java.util.HashMap<Integer, Integer> bridgeLink = new java.util.HashMap<Integer, Integer>();

    /** The other end of the bridge from tile i, or -1. */
    int bridgeTo(int i) {
        if (bridgeLink.isEmpty()) return -1;
        // ((11.5) A tile mask first: this is asked of every tile a route search looks at.)
        if (bridgeEnds == null || bridgeEndsN != bridgeLink.size()) {
            boolean[] m = new boolean[w * h];
            for (Integer k : bridgeLink.keySet()) if (k >= 0 && k < m.length) m[k] = true;
            bridgeEnds = m;
            bridgeEndsN = bridgeLink.size();
        }
        if (i < 0 || i >= bridgeEnds.length || !bridgeEnds[i]) return -1;
        Integer o = bridgeLink.get(i);
        return o == null ? -1 : o;
    }

    boolean[] bridgeEnds;
    int bridgeEndsN = -1;
    boolean[] overMask;
    int overMaskN = -1;

    int overpassAt(float x, float y) {
        if (overpasses.isEmpty()) return -1;
        // ((10.53) Half a tile to spare either side of the deck: a car keeping to its lane runs along the bridge's
        // edge, and a pixel over it was taken to be down on the highway underneath, and turned onto it.)
        float mx = hwyAxis == 0 ? T * 0.5f : 0, my = hwyAxis == 1 ? T * 0.5f : 0;
        // ((11.5) Tiles near no bridge deck are ruled out at once.)
        if (overMask == null || overMaskN != overpasses.size()) {
            boolean[] m = new boolean[w * h];
            for (float[] o : overpasses)
                for (int ty = Math.max(0, City.fl((o[1] - my) / T) - 1); ty <= Math.min(h - 1, (int) ((o[3] + my) / T) + 1); ty++)
                    for (int tx = Math.max(0, City.fl((o[0] - mx) / T) - 1); tx <= Math.min(w - 1, (int) ((o[2] + mx) / T) + 1); tx++)
                        m[ty * w + tx] = true;
            overMask = m;
            overMaskN = overpasses.size();
        }
        int tx = City.fl(x / T), ty = City.fl(y / T);
        if (tx < 0 || ty < 0 || tx >= w || ty >= h || !overMask[ty * w + tx]) return -1;
        for (int i = 0; i < overpasses.size(); i++) {
            float[] o = overpasses.get(i);
            if (x >= o[0] - mx && x < o[2] + mx && y >= o[1] - my && y < o[3] + my) return i;
        }
        return -1;
    }

    /** (10.31) How far along the highway a slip road reaches from the road it leaves, to where it merges. */
    static final int SLIP_LEN = 28;
    /** One-way slip roads: which way along each tile traffic may go (as oneWay), and (10.32) its exact heading. */
    byte[] slipWay;
    /** ((13.3) The way each slip road tile runs, in 256ths of a turn: a byte, not a float, for each tile of the map.) */
    OffHeap.Bytes slipAng;

    /** (10.32) A step from tile n to tile t (dx, dy) that doesn't go against a slip road's way, at either end. */
    boolean slipOk(int n, int t, int dx, int dy) {
        return slipStep(n, dx, dy) && slipStep(t, dx, dy);
    }

    boolean slipStep(int i, int dx, int dy) {
        if (slipWay[i] <= 0) return true;
        if (slipAng == null) return wayOk(slipWay, i, dx, dy);
        float a = slipAng.get(i) * (float) (Math.PI * 2 / 256);
        return dx * (float) Math.cos(a) + dy * (float) Math.sin(a) > -0.35f;
    }

    /** (10.29) Where slip roads run between the highway and a road joining it: {x0, y0, x1, y1} in tiles. */
    final List<int[]> slipZones = new ArrayList<int[]>();
    boolean[] slipTile;

    /** Whether this column of the railway is the bridge over the highway (no level crossing there). */
    boolean underRailBridge(int tx) {
        return railY0 >= 0 && hwyAxis == 1 && tx >= hwyAt - 1 && tx <= hwyAt + 7;
    }

    boolean underRailBridge(float x, float y) {
        if (!underRailBridge((int) (x / T))) return false;
        float ry = (railY0 + railRows / 2f) * T;
        return Math.abs(y - ry) < (railRows / 2f + 3) * T;
    }

    /** Whether a car may go (dx, dy) on tile i. */
    /** One-way lanes down each side of a tree-lined median (null if there are none). */
    byte[] divided;

    static boolean wayOk(byte[] ways, int i, int dx, int dy) {
        switch (ways[i]) {
            case 1: return dx != -1;
            case 2: return dx != 1;
            case 3: return dy != -1;
            case 4: return dy != 1;
            default: return true;
        }
    }

    boolean oneWayOk(int i, int dx, int dy) {
        switch (oneWay[i]) {
            case 1: return dx != -1;
            case 2: return dx != 1;
            case 3: return dy != -1;
            case 4: return dy != 1;
            default: return true;
        }
    }

    /** The middle of the carriageway a highway tile is on (across the highway, in world units). */
    float hwyCentre(int tx, int ty) {
        int a = (hwyAxis == 0 ? ty : tx) - hwyAt;
        return (hwyAt + (a < 3 ? 1.5f : 5.5f)) * T;
    }

    /** Inside the town (not out in the country). */
    boolean inTown(float x, float y) {
        return x >= townX0 * T && x < townX1 * T && y >= townY0 * T && y < townY1 * T;
    }

    boolean nearHighway(float x, float y, float r) {
        if (hwyAxis < 0) return false;
        float c = (hwyAt + 3.5f) * T;
        return Math.abs((hwyAxis == 0 ? y : x) - c) < r;
    }

    boolean onHighway(float x, float y) {
        if (oneWay == null) return false;
        int tx = (int) (x / T), ty = (int) (y / T);
        return tx >= 0 && ty >= 0 && tx < w && ty < h && oneWay[ty * w + tx] != 0;
    }

    /** (10.49) On one of the slip roads on and off the highway (or a link road out across the country). */
    boolean onSlip(float x, float y) {
        if (slipTile == null) return false;
        int tx = (int) (x / T), ty = (int) (y / T);
        return tx >= 0 && ty >= 0 && tx < w && ty < h && slipTile[ty * w + tx];
    }

    /** A random spot on the highway (a tile centre), or null if there's no highway. */
    float[] randomHighway(Random r) {
        if (hwyAxis < 0) return null;
        for (int k = 0; k < 40; k++) {
            int along = 2 + r.nextInt((hwyAxis == 0 ? w : h) - 4), a = r.nextBoolean() ? 1 : 5;
            int x = hwyAxis == 0 ? along : hwyAt + a, y = hwyAxis == 0 ? hwyAt + a : along;
            if (oneWay[y * w + x] != 0) return new float[]{(x + 0.5f) * T, (y + 0.5f) * T};
        }
        return null;
    }

    /**
     * Through traffic on the highway: where a car comes onto the map on one carriageway and where it leaves
     * at the far end, as {x0, y0, x1, y1}; null if there's no highway.
     */
    float[] highwayRun(Random r) {
        if (hwyAxis < 0) return null;
        int a = r.nextBoolean() ? 1 : 5, len = hwyAxis == 0 ? w : h;
        int x = hwyAxis == 0 ? 0 : hwyAt + a, y = hwyAxis == 0 ? hwyAt + a : 0;
        int ow = oneWay[y * w + x];
        // Comes in where the traffic flows from.
        boolean fromLow = ow == 1 || ow == 3;
        int k0 = fromLow ? 1 : len - 2, k1 = fromLow ? len - 2 : 1;
        float c = (hwyAt + a + 0.5f) * T;
        return hwyAxis == 0 ? new float[]{(k0 + 0.5f) * T, c, (k1 + 0.5f) * T, c} : new float[]{c, (k0 + 0.5f) * T, c, (k1 + 0.5f) * T};
    }

    /** A random spot on a country lane, out of town, or null. */
    float[] randomCountryRoad(Random r) {
        for (int k = 0; k < 300; k++) {
            int x = r.nextInt(w), y = r.nextInt(h);
            if (x >= townX0 && x < townX1 && y >= townY0 && y < townY1) continue;
            if (tiles[y * w + x] == DIRT) return new float[]{(x + 0.5f) * T, (y + 0.5f) * T};
        }
        return null;
    }
    /** The town's centre and each hamlet's, in world units: where hordes wander between on big maps. */
    final List<float[]> settlements = new ArrayList<float[]>();

    // ------------------------------------------------------------------ neighbourhoods

    static final int NB_CLOSES = 0, NB_CRESCENT = 1, NB_GREEN = 2, NB_ESTATE = 3, NB_ACRES = 4, NB_GATED = 5, NB_WORKS = 6, NB_WINDING = 7;

    boolean roadNear(int x, int y, int r) {
        for (int j = y - r; j <= y + r; j++)
            for (int i = x - r; i <= x + r; i++)
                if (i >= 0 && j >= 0 && i < w && j < h && tiles[j * w + i] == ROAD) return true;
        return false;
    }

    /** Helipads by who uses them (the third value, when there is one): the army's, the police's, the rescue service's. */
    static final int PAD_ARMY = 0, PAD_POLICE = 1, PAD_RESCUE = 2;

    static int padKind(float[] p) {
        return p.length > 2 ? (int) p[2] : PAD_ARMY;
    }

    /** Every helipad on the map (each one has its aircraft from the start, since 10.20). */
    List<float[]> helipads() {
        return helipads;
    }

    boolean padsOnly;

    /** The building standing at a world position, or null. */
    Building buildingAt(float x, float y) {
        int tx = City.fl(x / T), ty = City.fl(y / T);
        if (tx < 0 || ty < 0 || tx >= w || ty >= h) return null;
        int i = buildingAt.get(ty * w + tx);
        return i < 0 || buildings.get(i).collapsed ? null : buildings.get(i);
    }

    /** Knocks a building down: its tiles become a walkable heap of rubble. */
    void collapse(Building b) {
        if (!b.collapsed) {
            b.builtHeight = b.height;
            b.builtCapacity = b.capacity;
        }
        b.collapsed = true;
        b.height = 0;
        b.occupants.clear();
        b.capacity = 0;
        int tx0 = (int) (b.x0 / T), ty0 = (int) (b.y0 / T), tx1 = (int) (b.x1 / T), ty1 = (int) (b.y1 / T);
        for (int j = ty0; j < ty1; j++)
            for (int i = tx0; i < tx1; i++) {
                int k = j * w + i;
                if (tiles[k] != BUILDING) continue;
                tiles[k] = RUBBLE;
                solid[k] = false;
                solidChanged(k);
                opaque[k] = false;
            }
        art.drawRubble(b);
        // ((13.3) The ways on foot still work (a building down only opens new ways): each is worked out again in its
        // turn, not all at once the next moment - which stopped a Colossal game dead every time a building fell.)
        fieldsStale();
    }

    /**
     * (14.2) A fallen building put back up: the rubble cleared, the walls and roof as they were, room for as many
     * inside. False if it isn't down (or anything's still on the ground there - the caller checks for people).
     */
    boolean rebuild(Building b) {
        if (!b.collapsed) return false;
        int tx0 = (int) (b.x0 / T), ty0 = (int) (b.y0 / T), tx1 = (int) (b.x1 / T), ty1 = (int) (b.y1 / T);
        for (int j = ty0; j < ty1; j++)
            for (int i = tx0; i < tx1; i++) {
                int k = j * w + i;
                if (tiles[k] != RUBBLE) continue;
                tiles[k] = BUILDING;
                solid[k] = true;
                solidChanged(k);
                opaque[k] = true;
            }
        b.collapsed = false;
        b.height = b.builtHeight > 0 ? b.builtHeight : FLOOR + 3;
        b.capacity = b.builtCapacity;
        b.hp = b.maxHp;
        b.markCount = 0;
        b.smashed = false;
        b.rebuildT = 0;
        redrawArea(b.x0 - 6, b.y0 - 6, b.x1 + 6, b.y1 + 6);
        fieldsStale();
        return true;
    }

    /** (14.2) Draws this part of the map again, as it is now, into the map picture (a building put back up). */
    synchronized void redrawArea(final float x0, final float y0, final float x1, final float y1) {
        paintMap(x0, y0, x1, y1, new MapPainter() {
            @Override
            public void paint(Canvas c) {
                setRes(detail);
                regionOnly = true;
                rwx0 = x0;
                rwy0 = y0;
                rwx1 = x1;
                rwy1 = y1;
                rx0 = Math.max(0, (int) (x0 / T) - 3);
                ry0 = Math.max(0, (int) (y0 / T) - 3);
                rx1 = Math.min(w, (int) (x1 / T) + 3);
                ry1 = Math.min(h, (int) (y1 / T) + 3);
                try {
                    art.render(c);
                    for (Building b : buildings) if (b.collapsed && !offRegion(b.x0, b.y0, b.x1, b.y1)) art.drawRubble(c, b);
                    for (int[] t : charred) charTile(c, t[0], t[1]);
                } finally {
                    regionOnly = false;
                }
            }
        });
        changed(x0, y0, x1, y1);
    }

    /** Scorches a burnt-out car into the map. */
    void charTile(final int tx, final int ty) {
        charred.add(new int[]{tx, ty});
        paintMap(tx * T - T, ty * T - T, tx * T + 2 * T, ty * T + 2 * T, new MapPainter() {
            @Override
            public void paint(Canvas c) {
                charTile(c, tx, ty);
            }
        });
        changed(tx * T - T, ty * T - T, tx * T + 2 * T, ty * T + 2 * T);
    }

    /** Burnt-out cars scorched into the map, so a close-up picture of the area shows them too. */
    final List<int[]> charred = new java.util.concurrent.CopyOnWriteArrayList<int[]>();

    void charTile(Canvas c, int tx, int ty) {
        Paint p = new Paint();
        p.setAntiAlias(true);
        float cx = tx * T + T / 2f, cy = ty * T + T / 2f;
        p.setColor(0xB0101010);
        c.drawCircle(cx, cy, 11, p);
        p.setColor(0xFF2A2522);
        c.drawRect(cx - 7, cy - 4, cx + 7, cy + 4, p);
        p.setColor(0xFF4A2F22);
        c.drawRect(cx - 5, cy - 2.5f, cx + 5, cy + 2.5f, p);
    }

    // ------------------------------------------------------------------ rendering

    /**
     * ((22.0) How many pixels to a world unit the picture being drawn has, and whether that's enough for the
     * finest grain - specks under a pixel across, millions of them on a big map, which made a new city slow to
     * paint and couldn't be seen. Close-ups are drawn finer and keep them.)
     */
    boolean fine = true;

    void setRes(float res) {
        fine = res >= 1f;
    }

    /**
     * On the big maps the treetops are painted into the map picture too, for when you're zoomed out: then the
     * view leaves them to the picture instead of drawing tens of thousands of them every frame.
     */
    boolean canopies() {
        return detail < 0.999f;
    }

    /** Below this zoom (where the view shows the map picture, not sharp close-ups) the treetops are in the picture. */
    float canopyZoom() {
        return canopies() ? detail * 1.6f : 0;
    }

    boolean wet(int x, int y) {
        return x >= 0 && y >= 0 && x < w && y < h && tiles[y * w + x] == WATER;
    }

    /** The river's bank line at the boundary before step a of its course (tiles across), averaged for smoothness. */
    float bankAt(int a, boolean top) {
        int L = riverC.length;
        int a0 = Math.max(0, Math.min(L - 1, a - 1)), a1 = Math.max(0, Math.min(L - 1, a));
        float v0 = top ? riverC[a0] - riverHalf[a0] : riverC[a0] + riverHalf[a0] + 1;
        float v1 = top ? riverC[a1] - riverHalf[a1] : riverC[a1] + riverHalf[a1] + 1;
        return (v0 + v1) / 2;
    }

    boolean isPlazaTree(int x, int y) {
        for (int dy = -1; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx, ny = y + dy;
                if (nx >= 0 && ny >= 0 && nx < w && ny < h && tiles[ny * w + nx] == PLAZA) return true;
            }
        return false;
    }

    boolean paved(int x, int y) {
        if (x < 0 || y < 0 || x >= w || y >= h) return false;
        byte t = tiles[y * w + x];
        return t == ROAD || t == CAR;
    }

    final Random viewRnd = new Random();
    final Paint viewPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * ((16.6) The parked vehicles in a stretch of the map, drawn again sharp over the map picture, for a close look:
     * on the bigger maps the picture has a fifth of a pixel or less to a unit, and a truck parked on a base was a
     * smudge of olive on olive ground - the bases looked as if they had no vehicles at all.)
     */
    void drawParked(Canvas c, float x0, float y0, float x1, float y1) {
        int tx0 = Math.max(0, (int) (x0 / T)), ty0 = Math.max(0, (int) (y0 / T));
        int tx1 = Math.min(w - 1, (int) (x1 / T)), ty1 = Math.min(h - 1, (int) (y1 / T));
        for (int y = ty0; y <= ty1; y++)
            for (int x = tx0; x <= tx1; x++)
                if (tiles[y * w + x] == CAR) art.drawCar(c, viewPaint, x, y, viewRnd);
    }

    static int darken(int c, float f) {
        int r = (int) (((c >> 16) & 0xFF) * f), g = (int) (((c >> 8) & 0xFF) * f), b = (int) ((c & 0xFF) * f);
        return (c & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    static int lighten(int c, float f) {
        int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
        r = (int) (r + (255 - r) * f);
        g = (int) (g + (255 - g) * f);
        b = (int) (b + (255 - b) * f);
        return (c & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    // ------------------------------------------------------------------ queries

    /** ((11.5) Rounded down to a whole number, without Math.floor's trip through a double.) */
    static int fl(float v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    static int fl(double v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    boolean solidTile(int tx, int ty) {
        if (tx < 0 || ty < 0 || tx >= w || ty >= h) return true;
        return solid[ty * w + tx];
    }

    boolean solidAt(float x, float y) {
        int tx = City.fl(x / T), ty = City.fl(y / T);
        if (!solidTile(tx, ty)) return false;
        // ((17.1) Inside a building people can walk into: only where its walls are.)
        if (tx >= 0 && ty >= 0 && tx < w && ty < h && tiles[ty * w + tx] == BUILDING) {
            Building b = enterableTile(tx, ty);
            if (b != null) return wallHit(b, x, y, 1.3f);
        }
        return true;
    }

    /** Radius of a tree trunk, for walking into. */
    static final float TRUNK = 2.5f;

    // ------------------------------------------------------------------ (17.1) walking into buildings

    /** The doorways: wide enough for anyone to get through, the biggest of the dead included. */
    static final float DOOR_W = 13, WALL_HALF = 1.2f;

    /**
     * Can people walk into it: a building of its own (not a wing, not rubble) with an inside to it, big enough for a
     * room or two. Its walls are worked out the first time it's asked.
     */
    boolean enterable(Building b) {
        if (b == null || b.collapsed || b.main != null) return false;
        if (b.enter == 0) {
            boolean ok = b.capacity > 0 && b.x1 - b.x0 >= 28 && b.y1 - b.y0 >= 28 && Interiors.insideOf(b) >= 0 && b.doorX > 0;
            if (ok) ok = buildWalls(b);
            b.enter = ok ? (byte) 1 : (byte) 2;
        }
        return b.enter == 1;
    }

    /** The building whose inside (x, y) is in, if people can walk in there; else null. */
    Building insideOf(float x, float y) {
        int tx = City.fl(x / T), ty = City.fl(y / T);
        if (tx < 0 || ty < 0 || tx >= w || ty >= h) return null;
        int i = buildingAt.get(ty * w + tx);
        if (i < 0) return null;
        Building b = buildings.get(i);
        if (!enterable(b) || x <= b.x0 + 1 || x >= b.x1 - 1 || y <= b.y0 + 1 || y >= b.y1 - 1) return null;
        return b;
    }

    /** An enterable building on this tile, or null. */
    Building enterableTile(int tx, int ty) {
        if (tx < 0 || ty < 0 || tx >= w || ty >= h) return null;
        int i = buildingAt.get(ty * w + tx);
        if (i < 0) return null;
        Building b = buildings.get(i);
        return enterable(b) ? b : null;
    }

    Interiors.Plan planOf(Building b) {
        if (b.plan == null) {
            int inside = Interiors.insideOf(b);
            if (inside < 0) return null;
            b.plan = Interiors.make(b, inside);
        }
        return b.plan;
    }

    /** The outside walls, with the doorway on the side the door is, and the walls between the rooms. */
    boolean buildWalls(Building b) {
        Interiors.Plan p = planOf(b);
        if (p == null) return false;
        float x0 = b.x0 + 1, y0 = b.y0 + 1, x1 = b.x1 - 1, y1 = b.y1 - 1;
        float dx = b.doorX, dy = b.doorY;
        // Which side the door is on: the one it's furthest beyond.
        float[] over = {y0 - dy, dy - y1, x0 - dx, dx - x1};
        int side = 0;
        for (int k = 1; k < 4; k++) if (over[k] > over[side]) side = k;
        if (over[side] < -4) return false;
        boolean across = side < 2;
        float lo = (across ? x0 : y0) + DOOR_W / 2 + 2, hi = (across ? x1 : y1) - DOOR_W / 2 - 2;
        if (hi < lo) return false;
        float g = Math.max(lo, Math.min(hi, across ? dx : dy));
        java.util.ArrayList<float[]> segs = new java.util.ArrayList<float[]>();
        float[][] edges = {{x0, y0, x1, y0}, {x0, y1, x1, y1}, {x0, y0, x0, y1}, {x1, y0, x1, y1}};
        for (int k = 0; k < 4; k++) {
            float[] e = edges[k];
            if (k != side) {
                segs.add(e);
                continue;
            }
            if (across) {
                segs.add(new float[]{x0, e[1], g - DOOR_W / 2, e[1]});
                segs.add(new float[]{g + DOOR_W / 2, e[1], x1, e[1]});
            } else {
                segs.add(new float[]{e[0], y0, e[0], g - DOOR_W / 2});
                segs.add(new float[]{e[0], g + DOOR_W / 2, e[0], y1});
            }
        }
        float nx = side == 2 ? -1 : side == 3 ? 1 : 0, ny = side == 0 ? -1 : side == 1 ? 1 : 0;
        float ex = across ? g : side == 2 ? x0 : x1, ey = across ? (side == 0 ? y0 : y1) : g;
        // (A room's wall that would close the doorway off is left short of it.)
        float keep = DOOR_W / 2 + 4;
        for (float[] wl : p.walls) {
            float[] q = wl.clone();
            for (int end = 0; end < 2; end++) {
                int i = end * 2, o = 2 - i;
                float ddx = q[i] - ex, ddy = q[i + 1] - ey;
                if (ddx * ddx + ddy * ddy >= keep * keep) continue;
                float lx = q[o] - q[i], ly = q[o + 1] - q[i + 1], len = (float) Math.sqrt(lx * lx + ly * ly);
                if (len < 14) {
                    q = null;
                    break;
                }
                q[i] += lx / len * 11;
                q[i + 1] += ly / len * 11;
            }
            if (q != null) segs.add(q);
        }
        float[] all = new float[segs.size() * 4];
        for (int i = 0; i < segs.size(); i++) System.arraycopy(segs.get(i), 0, all, i * 4, 4);
        b.walls = all;
        b.gapX = ex;
        b.gapY = ey;
        b.inX = ex - nx * 7;
        b.inY = ey - ny * 7;
        return true;
    }

    static float segDist2(float px, float py, float ax, float ay, float bx, float by) {
        float vx = bx - ax, vy = by - ay, l2 = vx * vx + vy * vy;
        float t = l2 <= 0 ? 0 : ((px - ax) * vx + (py - ay) * vy) / l2;
        t = t < 0 ? 0 : t > 1 ? 1 : t;
        float qx = ax + vx * t - px, qy = ay + vy * t - py;
        return qx * qx + qy * qy;
    }

    /** Does a circle touch any of a building's walls? */
    boolean wallHit(Building b, float x, float y, float r) {
        float[] wl = b.walls;
        float rr = (r + WALL_HALF) * (r + WALL_HALF);
        for (int i = 0; i < wl.length; i += 4) {
            float ax = wl[i], ay = wl[i + 1], bx = wl[i + 2], by = wl[i + 3];
            if (x + r + 2 < Math.min(ax, bx) || x - r - 2 > Math.max(ax, bx) || y + r + 2 < Math.min(ay, by) || y - r - 2 > Math.max(ay, by)) continue;
            if (segDist2(x, y, ax, ay, bx, by) < rr) return true;
        }
        return false;
    }

    /** Does the line between two points cross one of a building's walls? */
    boolean wallCross(Building b, float x0, float y0, float x1, float y1) {
        float[] wl = b.walls;
        for (int i = 0; i < wl.length; i += 4) {
            float ax = wl[i], ay = wl[i + 1], bx = wl[i + 2], by = wl[i + 3];
            float d1 = (bx - ax) * (y0 - ay) - (by - ay) * (x0 - ax), d2 = (bx - ax) * (y1 - ay) - (by - ay) * (x1 - ax);
            if ((d1 > 0) == (d2 > 0)) continue;
            float d3 = (x1 - x0) * (ay - y0) - (y1 - y0) * (ax - x0), d4 = (x1 - x0) * (by - y0) - (y1 - y0) * (bx - x0);
            if ((d3 > 0) != (d4 > 0)) return true;
        }
        return false;
    }

    /**
     * The way from one point in a building to another through its doorways, as the points to walk to in turn (the
     * last is the end). The building's own doorway counts as a point outside it ((gapX, gapY) pushed out).
     */
    float[] roomRoute(Building b, float sx, float sy, float tx, float ty) {
        Interiors.Plan p = planOf(b);
        java.util.ArrayList<float[]> a = toEntrance(b, p, sx, sy), c = toEntrance(b, p, tx, ty);
        // Drop the shared tail: where the two ways to the entrance meet.
        int ia = a.size() - 1, ic = c.size() - 1;
        while (ia >= 0 && ic >= 0 && a.get(ia) == c.get(ic)) {
            ia--;
            ic--;
        }
        int n = (ia + 1) + (ic + 1) + 1;
        float[] out = new float[n * 2];
        int k = 0;
        for (int i = 0; i <= ia; i++) {
            out[k++] = a.get(i)[0];
            out[k++] = a.get(i)[1];
        }
        for (int i = ic; i >= 0; i--) {
            out[k++] = c.get(i)[0];
            out[k++] = c.get(i)[1];
        }
        out[k++] = tx;
        out[k] = ty;
        return out;
    }

    /** The doorways from a point's room out to the building's entrance (the entrance itself last). */
    java.util.ArrayList<float[]> toEntrance(Building b, Interiors.Plan p, float x, float y) {
        java.util.ArrayList<float[]> out = new java.util.ArrayList<float[]>();
        float[] entrance = b.plan == p && p.entrance != null ? p.entrance : (p.entrance = new float[]{b.inX, b.inY});
        for (int guard = 0; guard < 24; guard++) {
            int r = roomAt(p, x, y);
            if (r < 0) break;
            float[] room = p.rooms.get(r);
            if (p.doorOf == null) p.doorOf = new float[p.rooms.size()][];
            if (p.doorOf[r] == null) p.doorOf[r] = new float[]{room[5], room[6]};
            float[] d = p.doorOf[r];
            // (This room's door is the building's own: out there.)
            if (Math.abs(d[0] - clampX(b, b.doorX)) < 6 && Math.abs(d[1] - clampY(b, b.doorY)) < 6 || roomHasEntrance(b, room)) break;
            out.add(d);
            // Across the doorway, into the next room.
            float cx = (room[0] + room[2]) / 2, cy = (room[1] + room[3]) / 2;
            float ox = d[0] - cx, oy = d[1] - cy;
            if (Math.abs(ox) * (room[3] - room[1]) > Math.abs(oy) * (room[2] - room[0])) {
                x = d[0] + (ox > 0 ? 4 : -4);
                y = d[1];
            } else {
                x = d[0];
                y = d[1] + (oy > 0 ? 4 : -4);
            }
        }
        out.add(entrance);
        return out;
    }

    static float clampX(Building b, float x) {
        return Math.max(b.x0 + 6, Math.min(b.x1 - 6, x));
    }

    static float clampY(Building b, float y) {
        return Math.max(b.y0 + 6, Math.min(b.y1 - 6, y));
    }

    /** Is the building's entrance in this room's walls? */
    static boolean roomHasEntrance(Building b, float[] room) {
        return b.inX >= room[0] - 1 && b.inX <= room[2] + 1 && b.inY >= room[1] - 1 && b.inY <= room[3] + 1;
    }

    static int roomAt(Interiors.Plan p, float x, float y) {
        int best = -1;
        float bd = Float.MAX_VALUE;
        for (int i = 0, n = p.rooms.size(); i < n; i++) {
            float[] r = p.rooms.get(i);
            if (x >= r[0] && x <= r[2] && y >= r[1] && y <= r[3]) return i;
            float ddx = Math.max(r[0] - x, Math.max(0, x - r[2])), ddy = Math.max(r[1] - y, Math.max(0, y - r[3]));
            if (ddx + ddy < bd) {
                bd = ddx + ddy;
                best = i;
            }
        }
        return best;
    }

    /** True if a circle at (x, y) overlaps any solid tile or leaves the map. */
    boolean circleBlocked(float x, float y, float r) {
        // ((11.5) Out in the open - nothing solid on the tile or any round it - there's nothing to hit: one look.)
        if (r < T && !slowSteps) {
            int cx = City.fl(x / T), cy = City.fl(y / T);
            if (cx >= 0 && cy >= 0 && cx < w && cy < h) {
                if (clear3 == null) buildClear3();
                int ci = cy * w + cx;
                if ((clear3[ci >>> 6] & (1L << ci)) != 0) return false;
                // ((22.1) Or near something, but not that near.)
                if (clr == null) buildClr();
                int sx = City.fl(x / CLR_CELL), sy = City.fl(y / CLR_CELL);
                int room = clr[sy * w * 2 + sx];
                if (room > r) {
                    float dx = x - (sx * CLR_CELL + CLR_CELL / 2f), dy = y - (sy * CLR_CELL + CLR_CELL / 2f), m = room - r;
                    if (dx * dx + dy * dy <= m * m) return false;
                }
            }
        }
        int tx0 = City.fl((x - r) / T), tx1 = City.fl((x + r) / T);
        int ty0 = City.fl((y - r) / T), ty1 = City.fl((y + r) / T);
        Building seen0 = null, seen1 = null;
        for (int ty = ty0; ty <= ty1; ty++) {
            for (int tx = tx0; tx <= tx1; tx++) {
                if (!solidTile(tx, ty)) continue;
                // ((17.1) A building people can walk into: only its walls are in the way.)
                Building eb = tx >= 0 && ty >= 0 && tx < w && ty < h && tiles[ty * w + tx] == BUILDING ? enterableTile(tx, ty) : null;
                if (eb != null) {
                    if (eb != seen0 && eb != seen1) {
                        if (wallHit(eb, x, y, r)) return true;
                        if (seen0 == null) seen0 = eb;
                        else seen1 = eb;
                    }
                    continue;
                }
                if (tx >= 0 && ty >= 0 && tx < w && ty < h && tiles[ty * w + tx] == TREE) {
                    // A tree is its trunk, not the whole tile: people slip between trees and slide round them.
                    float dx = x - (tx * T + T / 2f), dy = y - (ty * T + T / 2f), rr = r + TRUNK;
                    if (dx * dx + dy * dy < rr * rr) return true;
                    continue;
                }
                float nx = Math.max(tx * T, Math.min(x, tx * T + T));
                float ny = Math.max(ty * T, Math.min(y, ty * T + T));
                float dx = x - nx, dy = y - ny;
                if (dx * dx + dy * dy < r * r) return true;
            }
        }
        return false;
    }

    /**
     * ((11.5) For each tile, whether it and the eight round it are all open ground on the map: a circle smaller than
     * a tile centred there can't touch anything solid. Only ever says "open" when it is: a tile that turns solid
     * puts it right for itself and its neighbours (one that opens up is only checked the long way until then).)
     */
    /** ((16.8) A bit a tile, not a byte: an eighth of the size, so far more of it stays close at hand - it's looked at
     * for every step anyone takes, all over the map.) */
    long[] clear3;

    /**
     * ((22.1) How far each spot is from anything solid: for each quarter of a tile (8 units across), the distance in
     * whole units from its middle to the nearest solid tile among the nine round it - capped at 20, a unit short to
     * allow for walls drawn a fraction outside their building - so a person whose circle fits inside that clearance
     * can't be touching anything, without looking at a single tile, building or wall. Most people walk the
     * pavements beside buildings, where the open-ground mask above says no and the full look was a fifth of each step.
     */
    byte[] clr;
    static final int CLR_CELL = 8;

    void buildClr() {
        int cw = w * 2;
        byte[] c = new byte[cw * h * 2];
        for (int ty = 0; ty < h; ty++) for (int tx = 0; tx < w; tx++) clrTile(c, tx, ty);
        clr = c;
    }

    void clrTile(byte[] c, int tx, int ty) {
        int cw = w * 2;
        for (int sy = 0; sy < 2; sy++)
            for (int sx = 0; sx < 2; sx++) {
                float cx = (tx * 2 + sx) * CLR_CELL + CLR_CELL / 2f, cy = (ty * 2 + sy) * CLR_CELL + CLR_CELL / 2f;
                float best = 21;
                for (int ky = ty - 1; ky <= ty + 1; ky++)
                    for (int kx = tx - 1; kx <= tx + 1; kx++) {
                        if (!solidTile(kx, ky)) continue;
                        float dx = Math.max(0, Math.max(kx * T - cx, cx - (kx * T + T))), dy = Math.max(0, Math.max(ky * T - cy, cy - (ky * T + T)));
                        best = Math.min(best, (float) Math.sqrt(dx * dx + dy * dy));
                    }
                c[(ty * 2 + sy) * cw + tx * 2 + sx] = (byte) Math.max(0, Math.min(20, (int) (best - 1)));
            }
    }

    void buildClear3() {
        long[] c = new long[(w * h >>> 6) + 1];
        clear3 = c;
        for (int i = 0; i < w * h; i++) if (openRound(i)) c[i >>> 6] |= 1L << i;
    }

    boolean openRound(int i) {
        int x = i % w, y = i / w;
        if (x < 1 || y < 1 || x >= w - 1 || y >= h - 1) return false;
        for (int j = -1; j <= 1; j++)
            for (int k = -1; k <= 1; k++)
                if (solid[i + j * w + k]) return false;
        return true;
    }

    /** ((11.5) A tile has changed: the open-ground mask round it is worked out again.) */
    void clearChanged(int i) {
        if (clr != null && i >= 0 && i < w * h) {
            int x = i % w, y = i / w;
            for (int j = Math.max(0, y - 1); j <= Math.min(h - 1, y + 1); j++)
                for (int k = Math.max(0, x - 1); k <= Math.min(w - 1, x + 1); k++) clrTile(clr, k, j);
        }
        if (clear3 == null || i < 0 || i >= w * h) return;
        int x = i % w, y = i / w;
        for (int j = Math.max(0, y - 1); j <= Math.min(h - 1, y + 1); j++)
            for (int k = Math.max(0, x - 1); k <= Math.min(w - 1, x + 1); k++) {
                int ci = j * w + k;
                if (openRound(ci)) clear3[ci >>> 6] |= 1L << ci;
                else clear3[ci >>> 6] &= ~(1L << ci);
            }
    }

    /** Can someone walk straight from one point to the other (nothing solid in the way but the odd tree)? */
    boolean passLine(float x0, float y0, float x1, float y1) {
        float dx = x1 - x0, dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        int steps = (int) (len / 6f) + 1;
        for (int i = 1; i < steps; i++) {
            float f = i / (float) steps;
            int tx = City.fl((x0 + dx * f) / T), ty = City.fl((y0 + dy * f) / T);
            if (tx < 0 || ty < 0 || tx >= w || ty >= h) return false;
            int k = ty * w + tx;
            if (solid[k] && tiles[k] != TREE) return false;
        }
        return true;
    }

    boolean los(float x0, float y0, float x1, float y1) {
        float dx = x1 - x0, dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        int steps = (int) (len / 5f) + 1;
        Building seen = null;
        for (int i = 1; i < steps; i++) {
            float f = i / (float) steps;
            int tx = City.fl((x0 + dx * f) / T), ty = City.fl((y0 + dy * f) / T);
            if (tx < 0 || ty < 0 || tx >= w || ty >= h) return false;
            if (opaque[ty * w + tx]) {
                // ((17.1) Into, out of or across a building people can walk into: through its doorways, not its walls.)
                Building b = enterableTile(tx, ty);
                if (b == null) return false;
                if (b != seen) {
                    seen = b;
                    if (wallCross(b, x0, y0, x1, y1)) return false;
                }
            }
        }
        return true;
    }

    int tileIndex(float x, float y) {
        int tx = Math.max(0, Math.min(w - 1, (int) (x / T)));
        int ty = Math.max(0, Math.min(h - 1, (int) (y / T)));
        return ty * w + tx;
    }

    int fieldAt(int[] field, float x, float y) {
        return field[tileIndex(x, y)];
    }

    /** Finds the centre of a walkable tile near (x, y), or null if there is none close by. */
    float[] findWalkable(float x, float y) {
        int tx = City.fl(x / T), ty = City.fl(y / T);
        if (!solidTile(tx, ty) && !circleBlocked(x, y, 4f)) return new float[]{x, y};
        // ((17.2) Inside a building people walk into, clear of its walls: right there.)
        if (insideOf(x, y) != null && !circleBlocked(x, y, 4f)) return new float[]{x, y};
        for (int r = 1; r <= 5; r++) {
            float best = Float.MAX_VALUE;
            float[] res = null;
            for (int j = ty - r; j <= ty + r; j++)
                for (int i = tx - r; i <= tx + r; i++) {
                    if (Math.max(Math.abs(i - tx), Math.abs(j - ty)) != r || solidTile(i, j)) continue;
                    float cx = i * T + T / 2f, cy = j * T + T / 2f;
                    float d = (cx - x) * (cx - x) + (cy - y) * (cy - y);
                    if (d < best) {
                        best = d;
                        res = new float[]{cx, cy};
                    }
                }
            if (res != null) return res;
        }
        return null;
    }

    /** The town part of the map (tiles), inside any countryside. */
    int townX0, townY0, townX1, townY1;

    /** A random walkable spot in town (the whole map if there's no countryside). */
    float[] randomWalkableInTown(Random r) {
        for (int k = 0; k < 500; k++) {
            int tx = townX0 + r.nextInt(Math.max(1, townX1 - townX0)), ty = townY0 + r.nextInt(Math.max(1, townY1 - townY0));
            byte t = tiles[ty * w + tx];
            if (t == SIDEWALK || t == PLAZA || t == GRASS || t == SAND || ((t == ROAD || t == LOT) && r.nextFloat() < 0.2f))
                return new float[]{tx * T + 3 + r.nextFloat() * (T - 6), ty * T + 3 + r.nextFloat() * (T - 6)};
        }
        return randomWalkable(r);
    }

    /**
     * (10.56) Somewhere in town people would be out and about: on a pavement or a square (not standing in a field
     * or someone's lawn, as the start of the game used to put them). Falls back to anywhere walkable in town.
     */
    float[] randomStreetSpot(Random r) {
        for (int k = 0; k < 800; k++) {
            int tx = townX0 + r.nextInt(Math.max(1, townX1 - townX0)), ty = townY0 + r.nextInt(Math.max(1, townY1 - townY0));
            byte t = tiles[ty * w + tx];
            if (t == SIDEWALK || t == PLAZA)
                return new float[]{tx * T + 3 + r.nextFloat() * (T - 6), ty * T + 3 + r.nextFloat() * (T - 6)};
        }
        return randomWalkableInTown(r);
    }

    /** A drivable spot on the edge of the map (where convoys come in), or null. */
    float[] edgeRoad(Random r) {
        if (causewayEnd != null) return new float[]{causewayEnd[0], causewayEnd[1]};
        for (int k = 0; k < 400; k++) {
            int side = r.nextInt(4), pos = r.nextInt(w);
            int tx = side == 0 ? pos : side == 1 ? pos : side == 2 ? 1 : w - 2, ty = side == 0 ? 1 : side == 1 ? h - 2 : pos;
            int i = ty * w + tx;
            if (driveCost(i) > 0 && driveCost(i) <= 4) return new float[]{tx * T + T / 2f, ty * T + T / 2f};
        }
        return null;
    }

    float[] randomWalkable(Random r) {
        for (int k = 0; k < 500; k++) {
            int tx = r.nextInt(w), ty = r.nextInt(h);
            byte t = tiles[ty * w + tx];
            if (t == SIDEWALK || t == PLAZA || t == GRASS || ((t == ROAD || t == LOT) && r.nextFloat() < 0.2f))
                return new float[]{tx * T + 3 + r.nextFloat() * (T - 6), ty * T + 3 + r.nextFloat() * (T - 6)};
        }
        return new float[]{T * 1.5f, T * 1.5f};
    }

    // ------------------------------------------------------------------ places

    /** A street corner name for a world position, like "Oak St & 3rd Ave". */
    String placeName(float x, float y) {
        if (hwyAxis >= 0) {
            int t = (int) ((hwyAxis == 0 ? y : x) / T);
            if (t >= hwyAt - 2 && t <= hwyAt + 8) return hwyName;
        }
        // Out in the wilds: the campsite or the park.
        if (!natureNames.isEmpty() && !inTown(x, y)) {
            for (int i = 0; i < natureSpots.size(); i++) {
                float[] q = natureSpots.get(i);
                if (q[2] == NL_CAMP && World.hypot(q[0] - x, q[1] + 30 - y) < 9 * T) return natureNames.get(i);
            }
            if (parkName != null && World.hypot(parkX - x, parkY - y) < parkR) return parkName;
        }
        // The nearest street each way, if the point is beside it.
        Street across = null, along = null;
        float bestV = 6 * T, bestH = 6 * T;
        for (Street st : streets) {
            if (st.vertical) {
                if (y < st.y0 * T - T || y > st.y1 * T + T) continue;
                float d = Math.abs(x - (st.x0 + st.x1) / 2f * T);
                if (d < bestV) {
                    bestV = d;
                    across = st;
                }
            } else {
                if (x < st.x0 * T - T || x > st.x1 * T + T) continue;
                float d = Math.abs(y - (st.y0 + st.y1) / 2f * T);
                if (d < bestH) {
                    bestH = d;
                    along = st;
                }
            }
        }
        if (across != null && along != null && !across.name.equals(along.name)) return along.name + " & " + across.name;
        if (across != null || along != null) return (across != null && (along == null || bestV < bestH) ? across : along).name;
        // Inside a big block: the nearest street at all.
        Street best = null;
        float bd = Float.MAX_VALUE;
        for (Street st : streets) {
            float nx = Math.max(st.x0 * T, Math.min(x, st.x1 * T)), ny = Math.max(st.y0 * T, Math.min(y, st.y1 * T));
            float d = (nx - x) * (nx - x) + (ny - y) * (ny - y);
            if (d < bd) {
                bd = d;
                best = st;
            }
        }
        return best != null ? best.name : "downtown";
    }

    /** A name for an open area, like "Pine St Park". */
    String areaName(float[] area) {
        String street = placeName(area[0], area[1]);
        int amp = street.indexOf(" & ");
        if (amp > 0) street = street.substring(0, amp);
        String[] kinds = {"Park", "Plaza", "Parking Lot"};
        return street + " " + kinds[(int) area[2]];
    }

    /** The nearest facility of a kind to (x, y), or null if the map has none. */
    Facility nearestFacility(int kind, float x, float y) {
        Facility best = null;
        float bd = Float.MAX_VALUE;
        for (Facility f : facilities) {
            if (f.kind != kind) continue;
            float d = (f.x - x) * (f.x - x) + (f.y - y) * (f.y - y);
            if (d < bd) {
                bd = d;
                best = f;
            }
        }
        return best;
    }

    /** A walkable spot near the edge of the map, as close as possible to (tx, ty). */
    float[] edgeSpawn(float tx, float ty) {
        float best = Float.MAX_VALUE;
        float[] res = null;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int d = Math.min(Math.min(x, y), Math.min(w - 1 - x, h - 1 - y));
                if (d > origin + 2 || solid[y * w + x]) continue;
                float cx = x * T + T / 2f, cy = y * T + T / 2f;
                float dd = (cx - tx) * (cx - tx) + (cy - ty) * (cy - ty);
                if (dd < best) {
                    best = dd;
                    res = new float[]{cx, cy};
                }
            }
        return res;
    }

    // ------------------------------------------------------------------ flow fields

    /** How far out (tiles) the distance to the nearest of the dead is known. */
    int zombieLimit = 70;

    void computeFields(List<Entity> entities) {
        computeFields(entities, true, true);
    }

    /**
     * ((13.5) One or both: on the biggest maps they're worked out in turn, not in the same step.)
     * ((24.0) All at once, there and then: after a load, or for a test. In play they're worked out a slice at a time - see
     * startFields and fieldWork.)
     */
    void computeFields(List<Entity> entities, boolean doZombies, boolean doHumans) {
        startFields(entities, doZombies, doHumans);
        fieldWork(entities, Integer.MAX_VALUE);
    }

    /**
     * (24.0) The scent searches, spread over several steps instead of all in one: each whole-map search was ten
     * milliseconds or more, done four times a second in the middle of drawing - the longest steps of all. The search
     * in progress fills a spare table, which changes places with the one everybody reads when it's done, so nobody
     * ever sees half a field. The zombie field first, then the human one (which uses the new zombie field). The spare
     * table is cleared a slice at a time too, before each search.
     */
    int[] fieldBack;
    /** The tiles set in the spare table (-1 not known: all of it is cleared), and how far clearing them has got. */
    int[] backTouched;
    int nBackTouched = -1, backCleared;
    /** 0 nothing being worked out, 1 the zombie field, 2 the human field. */
    int fieldPhase;
    /** Still clearing the spare table before the search starts. */
    boolean fClearing;
    int fqHead, fqTail, fLimit, fZombieLimit;
    boolean fDoHumans, fNearZombiesOnly;
    /** How many tiles the last whole pass went through, and this one so far: to judge how much to do a step. */
    int fieldLastWork;
    int fieldWorkNow;

    /** A pass of the searches is under way. */
    boolean fieldsBusy() {
        return fieldPhase != 0;
    }

    /** Starts a new pass of the searches (dropping any under way), with everyone where they are now. */
    void startFields(List<Entity> entities, boolean doZombies, boolean doHumans) {
        abortFields();
        // Only near distances matter (how far the nearest zombie or person is, within smelling range), so
        // the searches stop spreading after a while instead of covering a whole massive map.
        // (10.31) Only the dead go by the scent of the living: with none about (a calm city of tens
        // of thousands) that search is skipped.
        boolean hunters = false;
        for (int i = 0, n = entities.size(); i < n; i++) {
            Entity e = entities.get(i);
            if (e.dead) continue;
            if (e.isZombie()) hunters = true;
        }
        // ((10.34) Not so far out in a crowded city: that search covered most of a Gigantic map.)
        fZombieLimit = entities.size() > 12000 ? 48 : 70;
        // (And with only the dead to smell them, only the living within reach of one count: the rest of the city
        // can't be smelled from anywhere a zombie is.)
        fNearZombiesOnly = true;
        fDoHumans = doHumans && hunters;
        fieldWorkNow = 0;
        if (fieldBack == null || fieldBack.length != w * h) {
            fieldBack = new int[w * h];
            nBackTouched = -1;
        }
        if (doZombies) beginSearch(true);
        else if (fDoHumans) beginSearch(false);
    }

    /** Drops the pass under way: what it put in the spare table is cleared with the rest next time. */
    void abortFields() {
        if (fieldPhase == 0) return;
        if (!fClearing) {
            // (The table was clear when it started: everything it has put there since is in its queue.)
            if (backTouched == null || backTouched.length < fqTail) backTouched = new int[fqTail + fqTail / 2 + 64];
            System.arraycopy(queue, 0, backTouched, 0, fqTail);
            nBackTouched = fqTail;
        }
        fieldPhase = 0;
        fqHead = fqTail = 0;
    }

    void beginSearch(boolean zombies) {
        fieldPhase = zombies ? 1 : 2;
        fLimit = zombies ? fZombieLimit : (int) (World.SCENT * 1.7f) + 4;
        fClearing = true;
        backCleared = 0;
        fqHead = fqTail = 0;
    }

    /**
     * Goes on with the pass under way, through about budget tiles. True if that finished it (the new fields are in
     * place).
     */
    boolean fieldWork(List<Entity> entities, int budget) {
        if (fieldPhase == 0) return false;
        while (fieldPhase != 0 && budget > 0) {
            boolean zombies = fieldPhase == 1;
            if (fClearing) {
                // Clearing the spare table: the tiles the field it last held reached, or all of it.
                int[] back = fieldBack;
                if (nBackTouched < 0) {
                    int to = (int) Math.min((long) back.length, (long) backCleared + Math.max(budget, 1) * 4L);
                    Arrays.fill(back, backCleared, to, FAR);
                    budget -= (to - backCleared) / 4 + 1;
                    backCleared = to;
                    if (backCleared < back.length) break;
                } else {
                    int[] tch = backTouched;
                    int to = (int) Math.min((long) nBackTouched, (long) backCleared + Math.max(budget, 1) * 2L);
                    for (int i = backCleared; i < to; i++) back[tch[i]] = FAR;
                    budget -= (to - backCleared) / 2 + 1;
                    backCleared = to;
                    if (backCleared < nBackTouched) break;
                }
                fClearing = false;
                nearZombiesOnly = !zombies && fNearZombiesOnly;
                fqTail = seed(back, entities, zombies);
                nearZombiesOnly = false;
                fqHead = 0;
                continue;
            }
            int h0 = fqHead;
            fqTail = spread(fieldBack, fqTail, fLimit, budget);
            budget -= fqHead - h0;
            if (fqHead < fqTail) break;
            fieldWorkNow += fqTail;
            commit(zombies);
            if (zombies) zombieLimit = fZombieLimit;
            if (zombies && fDoHumans) beginSearch(false);
            else {
                fieldPhase = 0;
                fieldLastWork = fieldWorkNow;
                return true;
            }
        }
        return false;
    }

    /** The finished search's table changes places with the field everybody reads; the old one is the spare now. */
    void commit(boolean zombies) {
        int[] old = zombies ? zombieDist : humanDist;
        int[] oldT = zombies ? touchedZ : touchedH;
        int oldN = zombies ? nTouchedZ : nTouchedH;
        int end = fqTail;
        // The new field's list of tiles, in the array the spare table's list was in (it's been cleared).
        int[] tch = backTouched;
        if (tch == null || tch.length < end) tch = new int[end + end / 2 + 64];
        System.arraycopy(queue, 0, tch, 0, end);
        if (zombies) {
            zombieDist = fieldBack;
            touchedZ = tch;
            nTouchedZ = end;
        } else {
            humanDist = fieldBack;
            touchedH = tch;
            nTouchedH = end;
        }
        fieldBack = old;
        backTouched = oldT;
        nBackTouched = oldT == null ? -1 : oldN;
        backCleared = 0;
        fqHead = fqTail = 0;
    }

    /** Can a vehicle drive over this tile, and at what cost: roads are cheap, sidewalks and lots dearer. */
    int driveCost(int i) {
        byte t = tiles[i];
        if (t == ROAD) return 2;
        if (t == DIRT) return 4;
        if (t == LOT || t == BASE) return 3;
        if (t == SIDEWALK || t == PLAZA) return 8;
        if (offroad && (t == GRASS || t == SAND || t == TRAIL) && !solid[i] && !steepAt(i)) return 6;
        return -1;
    }

    /** (10.34) A road or lane within r tiles of (x, y). */
    boolean roadNear(float x, float y, int r) {
        int tx = (int) (x / T), ty = (int) (y / T);
        for (int j = Math.max(0, ty - r); j <= Math.min(h - 1, ty + r); j++)
            for (int i = Math.max(0, tx - r); i <= Math.min(w - 1, tx + r); i++)
                if (tiles[j * w + i] == ROAD || tiles[j * w + i] == DIRT) return true;
        return false;
    }

    /** Open ground (not a road, lot or pavement): an off-road vehicle takes it slower. */
    boolean rough(float x, float y) {
        int i = tileIndex(x, y);
        if (i < 0) return false;
        byte t = tiles[i];
        return t == GRASS || t == SAND || t == TRAIL;
    }

    boolean drivable(float x, float y) {
        int tx = City.fl(x / T), ty = City.fl(y / T);
        return tx >= 0 && ty >= 0 && tx < w && ty < h && driveCost(ty * w + tx) > 0;
    }

    /** (16.2) A road (or a lane: anything a roads-only search goes along) on tile i or one beside it. */
    boolean roadBeside(int i) {
        if (i < 0) return false;
        int x = i % w, y = i / w;
        for (int dy = -1; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx, ny = y + dy;
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                int c = driveCost(ny * w + nx);
                if (c >= 0 && c <= 4) return true;
            }
        return false;
    }

    /** The nearest drivable tile centre to (x, y), or null. */
    float[] nearestDrivable(float x, float y) {
        int tx = (int) (x / T), ty = (int) (y / T);
        for (int r = 0; r <= 6; r++)
            for (int j = ty - r; j <= ty + r; j++)
                for (int i = tx - r; i <= tx + r; i++) {
                    if (Math.max(Math.abs(i - tx), Math.abs(j - ty)) != r) continue;
                    if (i < 0 || j < 0 || i >= w || j >= h || driveCost(j * w + i) < 0) continue;
                    return new float[]{i * T + T / 2f, j * T + T / 2f};
                }
        return null;
    }

    /** Driving cost field to (x, y) for vehicles (Dijkstra; prefers roads). Returns false if unreachable. */
    boolean driveField(int[] dist, float x, float y) {
        return driveField(dist, x, y, false);
    }

    /** With strict on, only roads and dirt tracks: everyday traffic doesn't cut across pavements and car parks. */
    boolean driveField(int[] dist, float x, float y, boolean strict) {
        return driveField(dist, x, y, strict, -1);
    }

    /**
     * As above, but done once the search has got back to the vehicle at tile from (and a little beyond, for the
     * lanes either side of its way): a trip across town needn't cost a search of the whole map. Tiles further
     * out are left FAR.
     */
    /**
     * (10.27) A route for an all-wheel-drive vehicle in an emergency: as driveField, but open ground (grass,
     * sand, a trail) can be crossed too where it isn't steep, at a cost, so it only cuts across country when
     * that's really quicker. Never through trees, water, buildings, fences or rock.
     */
    boolean offroadField(int[] dist, float x, float y, int from) {
        offroad = true;
        try {
            return driveField(dist, x, y, false, from);
        } finally {
            offroad = false;
        }
    }

    boolean offroad;
    boolean[] steep;

    /** Too steep to drive across: the ground rises or falls sharply to a neighbouring tile. */
    boolean steepAt(int i) {
        if (elev == null) return false;
        if (steep == null) {
            boolean[] s = new boolean[w * h];
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    int k = y * w + x;
                    float e = elev.get(k), m = 0;
                    if (x > 0) m = Math.max(m, Math.abs(e - elev.get(k - 1)));
                    if (x < w - 1) m = Math.max(m, Math.abs(e - elev.get(k + 1)));
                    if (y > 0) m = Math.max(m, Math.abs(e - elev.get(k - w)));
                    if (y < h - 1) m = Math.max(m, Math.abs(e - elev.get(k + w)));
                    s[k] = m > 2.2f;
                }
            steep = s;
        }
        return steep[i];
    }

    boolean driveField(int[] dist, float x, float y, boolean strict, int from) {
        // ((11.5) Only the tiles the last search reached are put back, not the whole map: a search for a trip
        // across town touches a small part of it.)
        if (dist == dfLast && dfTouchedN >= 0) {
            for (int i = 0; i < dfTouchedN; i++) dist[dfTouched[i]] = FAR;
        } else Arrays.fill(dist, FAR);
        dfLast = dist;
        dfTouchedN = 0;
        try {
            return driveSearch(dist, x, y, strict, from);
        } finally {
            if (dfTouchedN > dist.length / 3) dfTouchedN = -1;
        }
    }

    /**
     * ((27.4) For one route search (set by FleetRouting around it, null otherwise): the dead on the map, so the way
     * keeps clear of streets thick with them - how many of the dead in each cell of World.CELL (hazGw cells across,
     * hazCell tiles to a cell), how much the vehicle minds them (hazMul, out of 100: armour minds less), and a square
     * of hazR tiles round (hazTx, hazTy) - where it's going - where they don't count, so it can still get to a fight.
     * And up to four tiles it can't get past at all (a wreck or a jam in the way), blockT, with the tiles round them.)
     */
    int[] hazZ;
    int hazGw, hazGh, hazCell = 4, hazMul = 100, hazTx = -99, hazTy = -99, hazR;
    final int[] blockT = new int[4];
    int blockN;

    /** ((27.4) What stepping onto tile n costs over and above the road for this search; -1 if it's blocked. */
    int hazard(int n) {
        int x = n % w, y = n / w;
        for (int i = 0; i < blockN; i++) {
            int b = blockT[i];
            if (Math.abs(x - b % w) <= 1 && Math.abs(y - b / w) <= 1) return -1;
        }
        if (hazZ == null || (Math.abs(x - hazTx) <= hazR && Math.abs(y - hazTy) <= hazR)) return 0;
        int cx = x / hazCell, cy = y / hazCell;
        if (cx >= hazGw || cy >= hazGh) return 0;
        int z = hazZ[cy * hazGw + cx];
        if (z == 0) return 0;
        // (A few: a little out of the way to miss them. A crowd: well out of the way. Never more than the search's
        // ring of buckets allows a step: see DIAL.)
        int c = z < 3 ? 2 : z < 6 ? 8 : z < 12 ? 16 : 24;
        return c * hazMul / 100;
    }

    int[] dfLast;
    int[] dfTouched = new int[4096];
    int dfTouchedN = -1;

    void dfTouch(int t) {
        if (dfTouchedN < 0) return;
        if (dfTouchedN == dfTouched.length) {
            if (dfTouchedN > dfLast.length / 3) {
                dfTouchedN = -1;
                return;
            }
            dfTouched = Arrays.copyOf(dfTouched, dfTouchedN * 2);
        }
        dfTouched[dfTouchedN++] = t;
    }

    /**
     * ((16.8) The most tiles a search may look at before giving up, or 0 for no limit: set for everyday trips, which
     * can always go somewhere else. One whose car couldn't be reached by the roads it keeps to (pulled onto a lot, or
     * on the wrong side of a one-way system) went through a third of a Large map - 60 ms or more - before failing.)
     */
    int searchCap;
    int searched;
    /** ((16.8) The last search gave up at searchCap. */
    boolean capped;

    boolean driveSearch(int[] dist, float x, float y, boolean strict, int from) {
        searched = 0;
        capped = false;
        int fx = from < 0 ? -9 : from % w, fy = from < 0 ? -9 : from / w, stopAt = Integer.MAX_VALUE;
        // (Its own tile, if it's on the road: a tile near it may be the far carriageway, a long way round.)
        int fc = from < 0 ? -1 : driveCost(from);
        boolean fromRoad = fc >= 0 && (!strict || fc <= 4);
        float[] p = nearestDrivable(x, y);
        if (p == null) return false;
        // Dial's algorithm: step costs are small whole numbers, so a ring of buckets (one per distance) does
        // the priority queue's job without the boxing.
        // ((10.36) And towards the vehicle, when there is one: A*, each tile ranked by its distance to the target
        // plus at least the cost of the rest of the way to the vehicle, so the search heads for it instead of
        // spreading out evenly over half the map. The distances it leaves are the same.)
        for (int b = 0; b < DIAL; b++) {
            if (dialB[b] == null) dialB[b] = new int[256];
            dialN[b] = 0;
        }
        boolean aim = from >= 0;
        // ((27.4) The dead or a blockage to keep out of, for this search.)
        boolean haz = hazZ != null || blockN > 0;
        int s0 = tileIndex(p[0], p[1]);
        dist[s0] = 0;
        dfTouch(s0);
        int k0 = aim ? 2 * (Math.abs(s0 % w - fx) + Math.abs(s0 / w - fy)) : 0;
        dialPush(s0, k0);
        int left = 1;
        for (int d = k0; left > 0 && d <= stopAt; d++) {
          int bk = d & (DIAL - 1);
          while (dialN[bk] > 0) {
            int t = dialB[bk][--dialN[bk]];
            left--;
            int tx = t % w, ty = t / w;
            int ht = aim ? 2 * (Math.abs(tx - fx) + Math.abs(ty - fy)) : 0;
            if (dist[t] + ht != d) continue;
            if (searchCap > 0 && ++searched > searchCap) {
                for (int b = 0; b < DIAL; b++) dialN[b] = 0;
                capped = true;
                return false;
            }
            int gt = dist[t];
            // Back at the vehicle: finish off the lanes round it, then stop.
            if (stopAt == Integer.MAX_VALUE && (fromRoad ? t == from : Math.abs(tx - fx) <= 1 && Math.abs(ty - fy) <= 1)) stopAt = d + (aim ? 60 : 160);
            int link = bridgeTo(t);
            if (!slowSteps) {
                // ((11.5) The quick way: each step's cost and rules looked up from the table made once for the map.)
                // ((13.6) Off road too: any step a car can take is the same for a four-wheel drive, which can also go
                // onto open ground: only those steps are worked out the long way. Off-road searches did every step
                // the long way, and an emergency crossing a big map held the game up for a moment.)
                if (stepC == null) buildSteps();
                int sb = t * 4;
                for (int k = 0; k < 4; k++) {
                    int sc = stepC.get(sb + k);
                    if (sc < 0) {
                        if (!offroad) continue;
                        int nn = k == 0 ? t + 1 : k == 1 ? t - 1 : k == 2 ? t + w : t - w;
                        if (nn < 0 || nn >= w * h) continue;
                        byte nt = tiles[nn];
                        if (nt != GRASS && nt != SAND && nt != TRAIL) continue;
                        sc = stepCost(t, k);
                        if (sc < 0) continue;
                    }
                    if (strict && (sc & 64) == 0) continue;
                    int c = sc & 63, n = k == 0 ? t + 1 : k == 1 ? t - 1 : k == 2 ? t + w : t - w;
                    if (haz) {
                        int hz = hazard(n);
                        if (hz < 0) continue;
                        c += hz;
                    }
                    if (gt + c >= dist[n]) continue;
                    if (dist[n] == FAR) dfTouch(n);
                    dist[n] = gt + c;
                    dialPush(n, gt + c + (aim ? 2 * (Math.abs(n % w - fx) + Math.abs(n / w - fy)) : 0));
                    left++;
                }
                if (link >= 0) {
                    int c = driveCost(link) + 16, hz = haz ? hazard(link) : 0;
                    // (Not much on a bridge's step: it's already long, and the ring of buckets has room for so much.)
                    c += Math.min(8, Math.max(0, hz));
                    if (driveCost(link) >= 0 && hz >= 0 && gt + c < dist[link]) {
                        if (dist[link] == FAR) dfTouch(link);
                        dist[link] = gt + c;
                        dialPush(link, gt + c + (aim ? 2 * (Math.abs(link % w - fx) + Math.abs(link / w - fy)) : 0));
                        left++;
                    }
                }
                continue;
            }
            for (int k = 0; k < (link >= 0 ? 5 : 4); k++) {
                if (k == 4) {
                    // (10.32) Over the bridge, from its other end. ((10.36) Costed as the eight tiles it spans.)
                    int c = driveCost(link) + 16, hz = haz ? hazard(link) : 0;
                    // (Not much on a bridge's step: it's already long, and the ring of buckets has room for so much.)
                    c += Math.min(8, Math.max(0, hz));
                    if (driveCost(link) >= 0 && hz >= 0 && gt + c < dist[link]) {
                        if (dist[link] == FAR) dfTouch(link);
                        dist[link] = gt + c;
                        dialPush(link, gt + c + (aim ? 2 * (Math.abs(link % w - fx) + Math.abs(link / w - fy)) : 0));
                        left++;
                    }
                    continue;
                }
                int nx = tx + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = ty + (k == 2 ? 1 : k == 3 ? -1 : 0);
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                int n = ny * w + nx, c = driveCost(n);
                if (strict && c > 4) continue;
                // (Nothing on or off the highway at a bridge's end: it goes over.)
                if (link >= 0 && oneWay != null && oneWay[n] != 0) continue;
                if (oneWay != null && oneWay[t] != 0 && bridgeTo(n) >= 0) continue;
                // One way only on the highway (a car here would drive from n to t).
                if (oneWay != null && c >= 0 && (!oneWayOk(n, tx - nx, ty - ny) || !oneWayOk(t, tx - nx, ty - ny))) continue;
                // (And down each side of a boulevard.)
                if (divided != null && c >= 0 && (!wayOk(divided, n, tx - nx, ty - ny) || !wayOk(divided, t, tx - nx, ty - ny))) continue;
                // (And down the slip roads, since 10.31.)
                if (slipWay != null && c >= 0 && !slipOk(n, t, tx - nx, ty - ny)) continue;
                // A step across the road (from lane to lane) outside a junction is a last resort: routes keep
                // to their row of the road and only change rows at the junctions, so cars don't swerve over.
                // (Emergencies too: lights and sirens still keep to their own side.)
                if (c >= 0 && c <= 4 && junctionId.get(n) < 0 && junctionId.get(t) < 0) {
                    byte rd = roadDir[n];
                    if ((rd == 1 && k < 2) || (rd == 2 && k >= 2)) c += 14;
                }
                if (c >= 0 && haz) {
                    int hz = hazard(n);
                    if (hz < 0) continue;
                    c += hz;
                }
                if (c < 0 || gt + c >= dist[n]) continue;
                if (dist[n] == FAR) dfTouch(n);
                dist[n] = gt + c;
                dialPush(n, gt + c + (aim ? 2 * (Math.abs(nx - fx) + Math.abs(ny - fy)) : 0));
                left++;
            }
          }
        }
        for (int b = 0; b < DIAL; b++) dialN[b] = 0;
        return true;
    }

    /**
     * ((11.5) For each tile and each way off it (east, west, south, north), what a car driving onto it from that
     * neighbour pays, with all the rules (one way, the boulevard's sides, slip roads, keeping to its row of the
     * road) already applied: -1 if it can't, the cost otherwise, plus 64 if everyday traffic may use it too.
     * Worked out once for the map, and again for a tile and its neighbours when it changes.)
     */
    OffHeap.Bytes stepC;
    /** (For the tests: the long way, to check the tables against.) */
    static boolean slowSteps;

    void buildSteps() {
        if (junction == null) computeJunctions();
        OffHeap.Bytes sc = new OffHeap.Bytes(w * h * 4);
        stepC = sc;
        // (The table is for ordinary traffic, whatever search asked for it first.)
        boolean was = offroad;
        offroad = false;
        for (int t = 0; t < w * h; t++) fillSteps(t);
        offroad = was;
    }

    void fillSteps(int t) {
        for (int k = 0; k < 4; k++) stepC.set(t * 4 + k, (byte) (stepCost(t, k)));
    }

    /** The step from tile t in direction k (right, left, down, up): its cost, +64 if onto road or track; -1 if not allowed. */
    int stepCost(int t, int k) {
        int tx = t % w, ty = t / w, link = bridgeTo(t);
        int nx = tx + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = ty + (k == 2 ? 1 : k == 3 ? -1 : 0);
        if (nx < 0 || ny < 0 || nx >= w || ny >= h) return -1;
        int n = ny * w + nx, c = driveCost(n), base = c;
        if (link >= 0 && oneWay != null && oneWay[n] != 0) return -1;
        if (oneWay != null && oneWay[t] != 0 && bridgeTo(n) >= 0) return -1;
        if (oneWay != null && c >= 0 && (!oneWayOk(n, tx - nx, ty - ny) || !oneWayOk(t, tx - nx, ty - ny))) return -1;
        if (divided != null && c >= 0 && (!wayOk(divided, n, tx - nx, ty - ny) || !wayOk(divided, t, tx - nx, ty - ny))) return -1;
        if (slipWay != null && c >= 0 && !slipOk(n, t, tx - nx, ty - ny)) return -1;
        if (c >= 0 && c <= 4 && junctionId.get(n) < 0 && junctionId.get(t) < 0) {
            byte rd = roadDir[n];
            if ((rd == 1 && k < 2) || (rd == 2 && k >= 2)) c += 14;
        }
        if (c < 0) return -1;
        return c | (base <= 4 ? 64 : 0);
    }

    /** ((11.5) A tile has changed: its steps, and its neighbours' onto it, are worked out again.) */
    void stepsChanged(int i) {
        if (stepC == null || i < 0 || i >= w * h) return;
        boolean was = offroad;
        offroad = false;
        fillSteps(i);
        int x = i % w, y = i / w;
        if (x > 0) fillSteps(i - 1);
        if (x < w - 1) fillSteps(i + 1);
        if (y > 0) fillSteps(i - w);
        if (y < h - 1) fillSteps(i + w);
        offroad = was;
    }

    /** Buckets in the ring: more than the biggest step a search's ranking can take (a bridge, 40). */
    static final int DIAL = 64;
    final int[][] dialB = new int[DIAL][];
    final int[] dialN = new int[DIAL];

    void dialPush(int t, int d) {
        int b = d & (DIAL - 1);
        if (dialN[b] == dialB[b].length) dialB[b] = Arrays.copyOf(dialB[b], dialB[b].length * 2);
        dialB[b][dialN[b]++] = t;
    }

    /**
     * What a step onto each tile costs someone on foot who keeps to the rules: pavement, grass and zebra
     * crossings 1, a junction 3 (cut across only if there's no crossing), the middle of a road 8.
     */
    byte[] walkCost;
    int[][] walkBuckets;

    /** Where roads meet: paved both ways for further than any one road is wide. */
    boolean[] junction;

    boolean sealed;

    void computeJunctions() {
        stepC = null;
        if (!sealed) roadGen.sealHighway();
        junction = new boolean[w * h];
        int[] run = new int[w * h];
        for (int y = 0; y < h; y++) {
            int x = 0;
            while (x < w) {
                if (!paved(x, y)) { x++; continue; }
                int s = x;
                while (x < w && paved(x, y)) x++;
                for (int k = s; k < x; k++) run[y * w + k] = x - s;
            }
        }
        for (int x = 0; x < w; x++) {
            int y = 0;
            while (y < h) {
                if (!paved(x, y)) { y++; continue; }
                int s = y;
                while (y < h && paved(x, y)) y++;
                for (int k = s; k < y; k++)
                    // ((11.1) A boulevard is seven across all along it: only where something crosses it is a junction.)
                    if (y - s >= 7 && run[k * w + x] >= 7 && (boulevard == null || !boulevard[k * w + x] || Math.min(y - s, run[k * w + x]) > 7))
                        junction[k * w + x] = true;
            }
        }
        // (10.29) Nothing on the highway is a junction: slip roads merge into it, and the gaps in the median
        // are for turning round, not crossroads with lights.
        if (cfg.civic >= 13 && hwyAxis >= 0)
            for (int k = 0, len = hwyAxis == 0 ? w : h; k < len; k++)
                for (int a = -1; a <= 7; a++) {
                    int x = hwyAxis == 0 ? k : hwyAt + a, y = hwyAxis == 0 ? hwyAt + a : k;
                    if (x >= 0 && y >= 0 && x < w && y < h) junction[y * w + x] = false;
                }
        // (Nor where the slip roads fork off a road joining it: they merge.)
        for (int[] z : slipZones)
            for (int y = z[1]; y <= z[3]; y++)
                for (int x = z[0]; x <= z[2]; x++) junction[y * w + x] = false;
        // Group the junction tiles into junctions, and decide how each is controlled.
        junctionId = new OffHeap.Ints(w * h);
        junctionId.fill(-1);
        junctions.clear();
        int[] stack = new int[w * h];
        for (int i = 0; i < w * h; i++) {
            if (!junction[i] || junctionId.get(i) >= 0) continue;
            int id = junctions.size(), top = 0, x0 = w, y0 = h, x1 = -1, y1 = -1, size = 0;
            stack[top++] = i;
            junctionId.set(i, id);
            while (top > 0) {
                int t = stack[--top], tx = t % w, ty = t / w;
                size++;
                x0 = Math.min(x0, tx);
                y0 = Math.min(y0, ty);
                x1 = Math.max(x1, tx);
                y1 = Math.max(y1, ty);
                for (int q = 0; q < 4; q++) {
                    int nx = tx + (q == 0 ? 1 : q == 1 ? -1 : 0), ny = ty + (q == 2 ? 1 : q == 3 ? -1 : 0);
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                    int n = ny * w + nx;
                    if (junction[n] && junctionId.get(n) < 0) {
                        junctionId.set(n, id);
                        stack[top++] = n;
                    }
                }
            }
            // A ring with an island is a roundabout; where an avenue is involved there are traffic lights;
            // the rest are stop junctions.
            boolean island = false;
            for (int y = Math.max(0, y0 - 1); y <= Math.min(h - 1, y1 + 1) && !island; y++)
                for (int x = Math.max(0, x0 - 1); x <= Math.min(w - 1, x1 + 1); x++)
                    // ((10.51) A real island: road all round it. The end of a median reaching into a big crossroads
                    // isn't one, and made the crossroads a roundabout nobody drove properly.)
                    if (x > x0 && x < x1 && y > y0 && y < y1 && (tiles[y * w + x] == GRASS || tiles[y * w + x] == TREE)
                            && tiles[y * w + x - 1] == ROAD && tiles[y * w + x + 1] == ROAD && tiles[(y - 1) * w + x] == ROAD
                            && tiles[(y + 1) * w + x] == ROAD) island = true;
            int type = island ? J_ROUNDABOUT : (x1 - x0 >= 4 || y1 - y0 >= 4) ? J_LIGHTS : J_STOP;
            // Just two roads meeting at a corner: a bend, not a junction anyone stops at.
            if (type == J_STOP) {
                boolean n = false, s = false, e = false, wst = false;
                for (int x = x0; x <= x1; x++) {
                    if (paved(x, y0 - 1)) n = true;
                    if (paved(x, y1 + 1)) s = true;
                }
                for (int y = y0; y <= y1; y++) {
                    if (paved(x0 - 1, y)) wst = true;
                    if (paved(x1 + 1, y)) e = true;
                }
                if ((n ? 1 : 0) + (s ? 1 : 0) + (e ? 1 : 0) + (wst ? 1 : 0) == 2 && (n || s) && (e || wst)) type = J_BEND;
            }
            junctions.add(new int[]{x0, y0, x1, y1, type, (id * 7919) % 23});
        }
        // Junctions only a tile or two apart are one junction as far as anyone driving is concerned.
        int n = junctions.size();
        int[] root = new int[n];
        for (int i = 0; i < n; i++) root[i] = i;
        // ((22.0) Only those near each other compared, through a coarse grid: every junction against every other was
        // millions of comparisons on a big map.)
        final int G = 32;
        int gw = w / G + 2, gh = h / G + 2;
        java.util.ArrayList<java.util.ArrayList<Integer>> grid = new java.util.ArrayList<java.util.ArrayList<Integer>>(gw * gh);
        for (int g = 0; g < gw * gh; g++) grid.add(null);
        for (int i = 0; i < n; i++) {
            int[] a = junctions.get(i);
            for (int gy = Math.max(0, (a[1] - 3) / G); gy <= Math.min(gh - 1, (a[3] + 3) / G); gy++)
                for (int gx = Math.max(0, (a[0] - 3) / G); gx <= Math.min(gw - 1, (a[2] + 3) / G); gx++) {
                    java.util.ArrayList<Integer> cell = grid.get(gy * gw + gx);
                    if (cell == null) grid.set(gy * gw + gx, cell = new java.util.ArrayList<Integer>());
                    cell.add(i);
                }
        }
        for (java.util.ArrayList<Integer> cell : grid)
            for (int ci = 0; cell != null && ci < cell.size(); ci++)
            for (int ck = ci + 1; ck < cell.size(); ck++) {
                int i = cell.get(ci), k = cell.get(ck);
                int[] a = junctions.get(i), b = junctions.get(k);
                if (a[0] - 3 <= b[2] && b[0] - 3 <= a[2] && a[1] - 3 <= b[3] && b[1] - 3 <= a[3]) {
                    int ra = i, rb = k;
                    while (root[ra] != ra) ra = root[ra];
                    while (root[rb] != rb) rb = root[rb];
                    if (ra != rb) root[Math.max(ra, rb)] = Math.min(ra, rb);
                }
            }
        boolean merged = false;
        for (int i = 0; i < n; i++) {
            int r = i;
            while (root[r] != r) r = root[r];
            root[i] = r;
            if (r != i) merged = true;
        }
        if (merged) {
            int[] newId = new int[n];
            List<int[]> out = new ArrayList<int[]>();
            for (int i = 0; i < n; i++) {
                if (root[i] != i) continue;
                newId[i] = out.size();
                out.add(junctions.get(i).clone());
            }
            for (int i = 0; i < n; i++) {
                if (root[i] == i) continue;
                int[] into = out.get(newId[root[i]]), from = junctions.get(i);
                newId[i] = newId[root[i]];
                into[0] = Math.min(into[0], from[0]);
                into[1] = Math.min(into[1], from[1]);
                into[2] = Math.max(into[2], from[2]);
                into[3] = Math.max(into[3], from[3]);
                // Lights win over a stop sign; a roundabout stays a roundabout; a bend next to anything is a stop.
                if (into[4] == J_BEND || from[4] == J_BEND) {
                    int o = into[4] == J_BEND ? from[4] : into[4];
                    into[4] = o == J_BEND ? J_STOP : o;
                } else if (into[4] != J_ROUNDABOUT && from[4] != J_STOP) into[4] = from[4];
                // Big enough together (both sides of a divided highway): lights.
                if (into[4] == J_STOP && (into[2] - into[0] >= 4 || into[3] - into[1] >= 4)) into[4] = J_LIGHTS;
            }
            for (int i = 0; i < w * h; i++) if (junctionId.get(i) >= 0) junctionId.set(i, newId[junctionId.get(i)]);
            junctions.clear();
            junctions.addAll(out);
        }
        // The road inside a junction's box (the short links between merged parts) belongs to it too.
        for (int id = 0; id < junctions.size(); id++) {
            int[] jb = junctions.get(id);
            for (int y = jb[1]; y <= jb[3]; y++)
                for (int x = jb[0]; x <= jb[2]; x++)
                    if (junctionId.get(y * w + x) < 0 && paved(x, y)) junctionId.set(y * w + x, id);
        }
    }

    static final int J_STOP = 0, J_LIGHTS = 1, J_ROUNDABOUT = 2, J_BEND = 3;
    /** Junctions: {x0, y0, x1, y1 (tiles, inclusive), type, light timing offset}. */
    final List<int[]> junctions = new ArrayList<int[]>();
    OffHeap.Ints junctionId;

    /** Which junction a point is in, or -1. */
    int junctionIdAt(float x, float y) {
        if (junction == null) computeJunctions();
        int tx = City.fl(x / T), ty = City.fl(y / T);
        return tx < 0 || ty < 0 || tx >= w || ty >= h ? -1 : junctionId.get(ty * w + tx);
    }

    /** Traffic lights: 0 green, 1 amber, 2 red for traffic going north-south (vertical) or east-west. */
    /**
     * Traffic lights that answer to the traffic: each set stays green while its own traffic flows and nobody
     * waits the other way; with someone waiting, it changes after a short minimum green (or a longer maximum, if
     * its own traffic keeps coming). Phases: 0 north-south green, 1 its amber, 2 all red, 3 east-west green,
     * 4 its amber, 5 all red.
     */
    byte[] lightPhase;
    float[] lightT;
    int[] demandNS, demandEW;

    void initLights() {
        int n = junctions.size();
        lightPhase = new byte[n];
        lightT = new float[n];
        demandNS = new int[n];
        demandEW = new int[n];
        for (int i = 0; i < n; i++) {
            lightPhase[i] = (byte) ((i * 7919) % 2 == 0 ? 0 : 3);
            lightT[i] = (i * 31) % 6;
        }
    }

    /** A vehicle coming up to lights, heading north-south (vertical) or east-west. */
    void lightDemand(int id, boolean vertical) {
        if (demandNS == null || id < 0 || id >= demandNS.length) return;
        if (vertical) demandNS[id]++;
        else demandEW[id]++;
    }

    void updateLights(float dt) {
        if (lightPhase == null || lightPhase.length != junctions.size()) initLights();
        for (int i = 0; i < lightPhase.length; i++) {
            lightT[i] += dt;
            int p = lightPhase[i];
            float t = lightT[i];
            boolean nsGreen = p == 0;
            if (p == 0 || p == 3) {
                int mine = nsGreen ? demandNS[i] : demandEW[i], other = nsGreen ? demandEW[i] : demandNS[i];
                if (other > 0 && t >= 6 && (mine == 0 || t >= 16)) next(i);
            } else if ((p == 1 || p == 4) && t >= 2.5f) next(i);
            else if ((p == 2 || p == 5) && t >= 1.2f) next(i);
            demandNS[i] = demandEW[i] = 0;
        }
    }

    void next(int i) {
        lightPhase[i] = (byte) ((lightPhase[i] + 1) % 6);
        lightT[i] = 0;
    }

    int lightState(int id, boolean vertical, float time) {
        if (lightPhase != null && id < lightPhase.length) {
            int p = lightPhase[id];
            if (vertical) return p == 0 ? 0 : p == 1 ? 1 : 2;
            return p == 3 ? 0 : p == 4 ? 1 : 2;
        }
        float t = (time + junctions.get(id)[5]) % 26f;
        // North-south: green 0-9, amber 9-12, then red while east-west has green 13-22 and amber 22-25
        // (a second of all-red in between each way).
        if (vertical) return t < 9 ? 0 : t < 12 ? 1 : 2;
        return t >= 13 && t < 22 ? 0 : t >= 22 && t < 25 ? 1 : 2;
    }

    /** (10.42) A tile of a curved street (laid along a curve, drawn smoothly): no lanes to measure across. */
    boolean curveAt(int tx, int ty) {
        return curveTile != null && tx >= 0 && ty >= 0 && tx < w && ty < h && curveTile[ty * w + tx];
    }

    /**
     * Where the lane is, across the road: how far (world units, to the driver's right) the middle of the lane
     * on the proper side of the road is from the middle of tile (tx, ty), for someone heading (dx, dy). The
     * other side of a median counts as the same road.
     */
    float laneOffset(int tx, int ty, int dx, int dy) {
        return laneOffset(tx, ty, dx, dy, -1);
    }

    /**
     * (11.1) How many lanes each way a road has here: one on an ordinary street, two on a main road five tiles
     * across, three on a boulevard seven across.
     */
    int lanesEachWay(int tx, int ty, int dx, int dy) {
        int rx = -dy, ry = dx, n = 1;
        for (int s = 1; s <= 6; s++) {
            if (!paved(tx + rx * s, ty + ry * s) || hwyTile(tx + rx * s, ty + ry * s)) break;
            n++;
        }
        for (int s = 1; s <= 6; s++) {
            if (!paved(tx - rx * s, ty - ry * s) || hwyTile(tx - rx * s, ty - ry * s)) break;
            n++;
        }
        return n >= 7 ? 3 : n >= 5 ? 2 : 1;
    }

    /**
     * Where to drive across the road at (tx, ty) heading (dx, dy): the offset from the tile's middle to the
     * middle of the lane. (11.1) With lane >= 0, which of the lanes on that side of a road with more than one
     * each way: 0 the inside (overtaking) lane, then outwards (a higher number than there are lanes is the
     * outside one). -1 is the old single line, between them.
     */
    float laneOffset(int tx, int ty, int dx, int dy, int laneNo) {
        // On the highway: the inside lane of your own carriageway.
        if (oneWay != null && oneWay[ty * w + tx] != 0) {
            float c = hwyCentre(tx, ty), here = hwyAxis == 0 ? (ty + 0.5f) * T : (tx + 0.5f) * T;
            float side = hwyAxis == 0 ? dx : -dy;
            // (Across the highway, at the crossovers at the ends: straight over. And never more than a lane
            // and a bit off the tile, whatever tile it's on.)
            if ((hwyAxis == 0 ? dx : dy) == 0) return 0;
            float off = (c - here) * side + T * 0.7f;
            return Math.max(-T * 1.3f, Math.min(T * 1.3f, off));
        }
        if (junctionAt(tx, ty)) return 4.5f;
        // (10.31) A one-way slip road: down the middle of it.
        if (slipWay != null && slipWay[ty * w + tx] > 0) return 0;
        int rx = -dy, ry = dx;
        int right = 0, left = 0;
        // (The highway is a road of its own: a frontage road beside it doesn't count its lanes as its own.)
        for (int s = 1; s <= 6; s++) {
            int x = tx + rx * s, y = ty + ry * s;
            if (hwyTile(x, y)) break;
            if (paved(x, y)) right = s;
            else if (isMedian(x, y) && paved(x + rx, y + ry) && !hwyTile(x + rx, y + ry)) continue;
            else break;
        }
        for (int s = 1; s <= 6; s++) {
            int x = tx - rx * s, y = ty - ry * s;
            if (hwyTile(x, y)) break;
            if (paved(x, y)) left = s;
            else if (isMedian(x, y) && paved(x - rx, y - ry) && !hwyTile(x - rx, y - ry)) continue;
            else break;
        }
        float centre = (right - left) / 2f, half = (right + left + 1) / 2f;
        int per = half >= 3.4f ? 3 : half >= 2.4f ? 2 : 1;
        if (laneNo >= 0 && per > 1) {
            float lw = half / per;
            return (centre + (Math.min(laneNo, per - 1) + 0.5f) * lw) * T;
        }
        float lane = half * 0.42f;
        return (centre + lane) * T;
    }

    boolean hwyTile(int x, int y) {
        return oneWay != null && x >= 0 && y >= 0 && x < w && y < h && oneWay[y * w + x] != 0;
    }

    boolean isMedian(int x, int y) {
        if (x < 0 || y < 0 || x >= w || y >= h) return false;
        byte t = tiles[y * w + x];
        return t == GRASS || t == TREE;
    }

    boolean junctionAt(int x, int y) {
        if (junction == null) computeJunctions();
        return x >= 0 && y >= 0 && x < w && y < h && junction[y * w + x];
    }

    /** A road tile with zebra stripes: next to a junction but not in it. */
    boolean crosswalk(int x, int y) {
        if (x < 0 || y < 0 || x >= w || y >= h || tiles[y * w + x] != ROAD || junctionAt(x, y)) return false;
        return junctionAt(x - 1, y) || junctionAt(x + 1, y) || junctionAt(x, y - 1) || junctionAt(x, y + 1);
    }

    void computeWalkCost() {
        computeJunctions();
        walkCost = new byte[w * h];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                byte t = tiles[i];
                // (Wading across a stream is slow going: a bridge nearby is better.)
                // ((10.51) Not across the middle of a roundabout: round by the zebra crossings on its arms.)
                boolean ring = junction[i] && junctionId.get(i) >= 0 && junctions.get(junctionId.get(i))[4] == J_ROUNDABOUT;
                walkCost[i] = (byte) (isFord(i) ? 6 : t != ROAD ? 1 : crosswalk(x, y) ? 1 : ring ? 8 : junction[i] ? 3 : 8);
            }
    }

    /**
     * Like fieldFromPoints, but for walking the way people really do: along the pavement, across at the
     * zebra crossings. Roads without a pavement (country lanes) still get used when there's no other way.
     */
    void walkFieldFromPoints(int[] dist, float[] xs, float[] ys, int n) {
        walkFieldFromPoints(dist, xs, ys, n, FAR);
    }

    /** As above, but only out to a walking distance of limit (in steps of cost). */
    void walkFieldFromPoints(int[] dist, float[] xs, float[] ys, int n, int limit) {
        if (walkCost == null) computeWalkCost();
        Arrays.fill(dist, FAR);
        // ((10.31) Each starts small and grows as needed: nine whole-map arrays were 36MB on a Gigantic map.)
        if (walkBuckets == null) walkBuckets = new int[9][Math.max(1024, w * h / 16)];
        int[] sizes = new int[9];
        int pending = 0;
        for (int i = 0; i < n; i++) {
            int t = tileIndex(xs[i], ys[i]);
            if (dist[t] != 0) {
                dist[t] = 0;
                walkBuckets[0][sizes[0]++] = t;
                pending++;
            }
        }
        for (int d = 0; pending > 0; d++) {
            int bi = d % 9;
            if (d > limit) {
                // Past the limit: what's left in the queue stays unreached.
                for (int k = 0; k < 9; k++) sizes[k] = 0;
                break;
            }
            int[] bk = walkBuckets[bi];
            int size = sizes[bi];
            for (int k = 0; k < size; k++) {
                int t = bk[k];
                if (dist[t] != d) continue;
                int tx = t % w, ty = t / w;
                for (int q = 0; q < 4; q++) {
                    int nb;
                    if (q == 0) { if (tx == 0) continue; nb = t - 1; }
                    else if (q == 1) { if (tx == w - 1) continue; nb = t + 1; }
                    else if (q == 2) { if (ty == 0) continue; nb = t - w; }
                    else { if (ty == h - 1) continue; nb = t + w; }
                    if (solid[nb]) continue;
                    int nd = d + walkCost[nb];
                    if (nd >= dist[nb]) continue;
                    dist[nb] = nd;
                    int b2 = nd % 9;
                    if (sizes[b2] == walkBuckets[b2].length) walkBuckets[b2] = Arrays.copyOf(walkBuckets[b2], Math.min(w * h * 4, sizes[b2] * 2));
                    walkBuckets[b2][sizes[b2]++] = nb;
                    pending++;
                }
            }
            pending -= size;
            sizes[bi] = 0;
        }
    }

    /**
     * A walking route from (sx, sy) to (tx, ty) as tile indices (start excluded), keeping to pavements and
     * crossings like walkFieldFromPoints, or null if there's none within maxExpand tiles searched.
     */
    /** (10.36) The walking router (see Nav), made when it's first needed. */
    Nav nav;
    /** Gates in the safe zones' walls: doors, open or shut, never walls to route round. */
    boolean[] gates;

    void solidChanged(int i) {
        if (nav != null) nav.touch(i);
        stepsChanged(i);
        clearChanged(i);
    }

    /** (11.0) Changes one tile during a game (the quarantine walls going up and coming down). */
    void setLiveTile(int i, byte t) {
        tiles[i] = t;
        solid[i] = t == TREE || t == FENCE || t == BUILDING || t == ROCK;
        solidChanged(i);
        if (walkCost != null) walkCost[i] = (byte) (t != ROAD ? 1 : 8);
    }

    void markGate(int i) {
        if (gates == null) gates = new boolean[w * h];
        if (!gates[i]) {
            gates[i] = true;
            solidChanged(i);
        }
        // ((11.5) A gate opens and shuts: the open-ground mask round it each time.)
        clearChanged(i);
    }

    boolean gateTile(int i) {
        return gates != null && gates[i];
    }

    /**
     * (10.36) A walking route from (sx, sy) to (tx, ty): the tiles to head for in turn (the corners of a
     * straightened route, the last one the destination), or null if there's no way there. By the hierarchical
     * router, however far it is (maxExpand mattered only to the old search, since removed).
     */
    int[] findPath(float sx, float sy, float tx, float ty, int maxExpand) {
        if (walkCost == null) computeWalkCost();
        if (nav == null) nav = new Nav(this);
        return nav.path(sx, sy, tx, ty);
    }

    /** (10.49) A few more of the walking router's clusters worked out ahead of need (once it's in use). */
    void warmPaths(int n) {
        // ((13.3) From the start, not once someone first needs a route: on a Colossal map the first few moments of a
        // panic worked out hundreds of clusters at once and held the game up for most of a second.)
        if (nav == null) {
            if (walkCost == null) computeWalkCost();
            nav = new Nav(this);
        }
        nav.warm(n);
    }

    /** More places the living can be smelled from: the walls of safe zones with people inside (set by Dispatch). */
    int[] extraScent;
    int extraScentN;

    boolean nearZombiesOnly;

    /**
     * ((12.0) The tiles each field's last search reached, so only those are cleared next time - not the whole map.)
     * ((24.0) The queue of the search under way is the list of what it has reached in the spare table.)
     */
    int[] touchedZ, touchedH;
    int nTouchedZ = -1, nTouchedH = -1;

    /** The tiles a search starts from, into dist (all FAR to begin with) and the queue; how many. */
    int seed(int[] dist, List<Entity> entities, boolean zombies) {
        int tail = 0;
        if (!zombies && extraScent != null)
            for (int i = 0; i < extraScentN; i++) {
                int t = extraScent[i];
                if (dist[t] != 0) {
                    dist[t] = 0;
                    queue[tail++] = t;
                }
            }
        for (int i = 0, n = entities.size(); i < n; i++) {
            Entity e = entities.get(i);
            if (e.dead || e.isZombie() != zombies) continue;
            int t = tileIndex(e.x, e.y);
            if (nearZombiesOnly && zombieDist[t] >= FAR) continue;
            if (dist[t] != 0) {
                dist[t] = 0;
                queue[tail++] = t;
            }
        }
        return tail;
    }

    /** ((24.0) Carries on from fqHead, through at most budget tiles; leaves fqHead where it got to.) */
    int spread(int[] dist, int tail, int limit, int budget) {
        int head = fqHead;
        int stop = budget >= Integer.MAX_VALUE - head ? Integer.MAX_VALUE : head + budget;
        // ((22.3) Whether a tile is on the edge of the map from a table, not worked out by dividing for each of the
        // hundreds of thousands it spreads over: a third of the search.)
        if (rim == null || rim.length != w * h) {
            rim = new boolean[w * h];
            for (int x = 0; x < w; x++) rim[x] = rim[(h - 1) * w + x] = true;
            for (int y = 0; y < h; y++) rim[y * w] = rim[y * w + w - 1] = true;
        }
        boolean[] rim = this.rim, solid = this.solid;
        int w = this.w;
        int[] queue = this.queue;
        while (head < tail && head < stop) {
            int t = queue[head++];
            int nd = dist[t] + 1;
            if (nd > limit) continue;
            if (!rim[t]) {
                int n = t - 1;
                if (!solid[n] && dist[n] > nd) { dist[n] = nd; queue[tail++] = n; }
                n = t + 1;
                if (!solid[n] && dist[n] > nd) { dist[n] = nd; queue[tail++] = n; }
                n = t - w;
                if (!solid[n] && dist[n] > nd) { dist[n] = nd; queue[tail++] = n; }
                n = t + w;
                if (!solid[n] && dist[n] > nd) { dist[n] = nd; queue[tail++] = n; }
                continue;
            }
            int tx = t % w, ty = t / w;
            if (tx > 0) tail = visit(dist, t - 1, nd, tail);
            if (tx < w - 1) tail = visit(dist, t + 1, nd, tail);
            if (ty > 0) tail = visit(dist, t - w, nd, tail);
            if (ty < h - 1) tail = visit(dist, t + w, nd, tail);
        }
        fqHead = head;
        return tail;
    }

    boolean[] rim;

    int visit(int[] dist, int n, int nd, int tail) {
        if (solid[n] || dist[n] <= nd) return tail;
        dist[n] = nd;
        queue[tail++] = n;
        return tail;
    }

    /** The casino strip's designs, in the order they come along it (the Casino over and over). */
    static final String[] STRIP = {"Casino", "Grand Hotel", "Casino", "Nightclub", "Wedding Chapel Gardens", "Casino",
            "Boutique Hotel", "Pyramid Building", "Casino", "Motel", "Wax Museum", "Casino", "Multiplex Cinema", "Retro Diner",
            "UFO Diner"};
    /** Neon for the strip's own big buildings: {roof, wall}. */
    static final int[][] NEON = {{0xFFD8A030, 0xFFF8E8B0}, {0xFF8E2A6A, 0xFFF070B0}, {0xFF2A6A8E, 0xFF40D0D0},
            {0xFFC03050, 0xFFF8D040}, {0xFFE8E0D0, 0xFFB850D0}, {0xFF3A3A48, 0xFFFF7040}};

    /**
     * A palm, a saguaro or a bush of desert scrub seen from above (kind Country.TK_*), at alpha a (0 to 1). Drawn into
     * the map picture and by the view close up.
     */
    static void desertTree(Canvas c, Paint p, float x, float y, float r, int kind, float a, float spin) {
        if (kind == Country.TK_PALM) {
            // Long fronds out from the crown.
            for (int layer = 0; layer < 2; layer++) {
                int col = layer == 0 ? 0xFF3E7430 : 0xFF5E9A40;
                float len = layer == 0 ? r : r * 0.72f;
                p.setColor(withAlpha(col, a));
                for (int k = 0; k < 7; k++) {
                    double an = spin + k * Math.PI * 2 / 7 + layer * 0.45f;
                    float ex = x + (float) Math.cos(an) * len, ey = y + (float) Math.sin(an) * len;
                    float mx = x + (float) Math.cos(an) * len * 0.5f, my = y + (float) Math.sin(an) * len * 0.5f;
                    float nx = -(float) Math.sin(an) * len * 0.17f, ny = (float) Math.cos(an) * len * 0.17f;
                    Path f = new Path();
                    f.moveTo(x, y);
                    f.quadTo(mx + nx, my + ny, ex, ey);
                    f.quadTo(mx - nx, my - ny, x, y);
                    f.close();
                    c.drawPath(f, p);
                }
            }
            p.setColor(withAlpha(0xFF7A5A34, a));
            c.drawCircle(x, y, r * 0.16f, p);
        } else if (kind == Country.TK_CACTUS) {
            // A saguaro: the ribbed column and its arms, upturned.
            float u = r * 0.55f;
            p.setColor(withAlpha(0xFF3E6A34, a));
            c.drawCircle(x, y, u, p);
            float ax = (float) Math.cos(spin) * u * 1.5f, ay = (float) Math.sin(spin) * u * 1.5f;
            c.drawCircle(x + ax, y + ay, u * 0.55f, p);
            c.drawCircle(x - ay * 0.8f, y + ax * 0.8f, u * 0.45f, p);
            p.setColor(withAlpha(0xFF5E9050, a));
            c.drawCircle(x - u * 0.2f, y - u * 0.2f, u * 0.6f, p);
            c.drawCircle(x + ax - u * 0.1f, y + ay - u * 0.1f, u * 0.3f, p);
            p.setColor(withAlpha(0xFFE8E0A0, a));
            c.drawCircle(x - u * 0.25f, y - u * 0.25f, Math.max(0.5f, u * 0.14f), p);
        } else {
            // Scrub: a low grey-green clump.
            p.setColor(withAlpha(0xFF6E7448, a));
            for (int k = 0; k < 5; k++) {
                double an = spin + k * 1.3f;
                c.drawCircle(x + (float) Math.cos(an) * r * 0.35f, y + (float) Math.sin(an) * r * 0.35f, r * 0.42f, p);
            }
            p.setColor(withAlpha(0xFF98A068, a));
            for (int k = 0; k < 3; k++) {
                double an = spin + 0.6f + k * 2.1f;
                c.drawCircle(x + (float) Math.cos(an) * r * 0.25f - r * 0.1f, y + (float) Math.sin(an) * r * 0.25f - r * 0.1f, r * 0.22f, p);
            }
        }
    }

    static int withAlpha(int col, float a) {
        return ((int) (((col >>> 24) & 255) * Math.max(0, Math.min(1, a))) << 24) | (col & 0xFFFFFF);
    }

    /** Theme Park Island's beach hotels, in the order they come round the shore. */
    static final String[] RESORT = {"Resort Hotel", "Beach Villas", "Pool Club", "Tower Hotel", "Beach Villas",
            "Resort Hotel", "Surf Lodge", "Pool Club"};
    /** The park's rides and halls, in the order they're handed out round the middle. */
    static final String[] RIDES = {"Log Flume", "Carousel", "Drop Tower", "Teacups", "Swing Ride", "Food Court",
            "Pirate Ship", "Bumper Cars", "Spinning Coaster", "Splash Pad", "Midway Games", "Arcade Hall"};

    // ------------------------------------------------------------------ ((27.3) the parts of a city

    /**
     * ((27.3) What used to be all in this one file: the map is built by the generator and its helpers (one for each
     * special map), and painted by the painter. This class keeps the map itself and what the game asks of it.
     * (After every other field, so each part finds the city's own fields set as they always were.)
     */
    final CityGenerator gen = new CityGenerator(this);
    final CityPainter art = new CityPainter(this);
    final CityRoads roadGen = new CityRoads(this);
    final CityBlocks lotGen = new CityBlocks(this);
    final CityOutskirts landGen = new CityOutskirts(this);
    final MilitaryBases baseGen = new MilitaryBases(this);
    final IslandsMap islandsMap = new IslandsMap(this);
    final ThemeParkMap parkMap = new ThemeParkMap(this);
    final SpaceCenterMap spaceMap = new SpaceCenterMap(this);
    final DesertMap desertMap = new DesertMap(this);
    final PrisonMap prisonMap = new PrisonMap(this);
    final RiverMap riverMap = new RiverMap(this);
    final CoastMap coastMap = new CoastMap(this);
    final AirportMap airportMap = new AirportMap(this);

    {
        gen.wire();
        art.wire();
        roadGen.wire();
        lotGen.wire();
        landGen.wire();
        baseGen.wire();
        islandsMap.wire();
        parkMap.wire();
        spaceMap.wire();
        desertMap.wire();
        prisonMap.wire();
        riverMap.wire();
        coastMap.wire();
        airportMap.wire();
    }
}
