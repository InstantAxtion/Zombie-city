package com.instantaxtion.zombiesandbox;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

/** Renders the world, runs the game loop and handles all touch input and on-screen buttons. */
final class GameView extends View implements Menu.Host {
    // ((27.3) What used to be all in this class, now in classes of their own (each holds this view as gv).)
    final WorldRenderer worldRenderer = new WorldRenderer(this);
    final BuildingRenderer buildingRenderer = new BuildingRenderer(this);
    final VehicleRenderer vehicleRenderer = new VehicleRenderer(this);
    final EntityRenderer entityRenderer = new EntityRenderer(this);
    final MapLabels mapLabels = new MapLabels(this);
    final Hud hud = new Hud(this);
    final Inspector inspector = new Inspector(this);
    final Overlays overlays = new Overlays(this);
    final ToolBar toolBar = new ToolBar(this);
    final TouchInput touchInput = new TouchInput(this);
    final GameAudio audio = new GameAudio(this);
    final SaveSlots saveSlots = new SaveSlots(this);

    static final int TOOL_PAN = 0, TOOL_ORDER = 1, TOOL_CIV = 2, TOOL_MIL = 4, TOOL_ZOMBIE = 6, TOOL_PLACE = 7,
            TOOL_EVENT = 8, TOOL_ZONE = 9, TOOL_BOMB = 10, TOOL_ERASE = 11, TOOL_BUILD = 12, TOOL_SQUAD = 13;
    /** ((22.4) The order of the tools on the bar. */
    static final int[] BAR_ORDER = {0, 1, 2, 3, 4, 13, 6, 7, 8, 9, 10, 11, 12, 5};
    // Tool index -> entity type spawned (or -1). The zombie tool spawns the selected zombie variant.
    static final int[] TOOL_TYPE = {-1, -1, Entity.CIVILIAN, Entity.COP, Entity.SOLDIER, Entity.CIVILIAN,
            Entity.ZOMBIE, -1, -1, -1, -1, -1, -1, -1};
    static final String[] TOOL_NAMES = {"Move", "Orders", "People", "Police", "Military", "Medic", "Zombies",
            "Place", "Events", "Safe Zone", "Bomb", "Erase", "Build", "Squads"};
    /** ((22.4) Ready-made groups, a whole one to a tap: what's in each (type, then roles for police and soldiers). */
    static final String[] SQUAD_NAMES = {"Fire team", "Rifle squad", "Sniper team", "Engineer squad", "Response team",
            "Tactical team", "Riot line", "K9 patrol", "Survivors", "Horde"};
    static final String[] SQUAD_INFO = {"Four soldiers and a medic: rifles, a grenadier, a machine gunner",
            "A commander and eight soldiers with a gunner and a medic", "A sniper and a recon scout to spot for them",
            "Two combat engineers who dig in, with two riflemen to cover them", "Three officers, a riot shield and a marksman",
            "Four SWAT, a marksman and a K9 handler", "Five riot shields shoulder to shoulder", "A K9 handler and a partner",
            "Six civilians together", "Twenty of the dead: walkers, runners and a brute"};
    static final int[][] SQUAD_ROLES = {World.FIRE_TEAM, World.RIFLE_SQUAD, {Entity.ROLE_SNIPER, Entity.ROLE_RECON},
            {Entity.ROLE_ENGINEER, Entity.ROLE_ENGINEER, Entity.ROLE_RIFLE, Entity.ROLE_RIFLE}, World.RESPONSE_TEAM,
            World.TACTICAL_TEAM, {Entity.ROLE_RIOT, Entity.ROLE_RIOT, Entity.ROLE_RIOT, Entity.ROLE_RIOT, Entity.ROLE_RIOT},
            {Entity.ROLE_K9, 0}, null, null};
    /** 0 soldiers, 1 police, 2 civilians, 3 the dead. */
    static final int[] SQUAD_SIDE = {0, 0, 0, 0, 1, 1, 1, 1, 2, 3};
    static final String[] BUILD_NAMES = {"Road", "Pavement", "Grass", "Trees", "Wall", "House", "Shop", "Clear"};
    static final String[] BUILD_INFO = {"Drag to lay a road", "Drag to pave", "Drag to grass over", "Drag to plant trees",
            "Drag to build a wall nobody can cross", "Tap to put up a house", "Tap to put up a shop", "Grass over, or knock a building down"};
    /** Tools no longer on the bar: the medic (now in the People picker), Orders, Place, Events and Build. */
    boolean hiddenTool(int t) {
        // ((25.0) Events back on the bar: the plane crash, prison break and riot, and the airstrike with its cooldown.)
        return t == 5 || t == TOOL_ORDER || t == TOOL_PLACE || t == TOOL_BUILD || t == TOOL_ZONE;
    }
    static final String[] PLACE_NAMES = {"Car", "Police car", "Tank", "Barricade", "Crate", "Medkit", "BearCat", "Recon Humvee", "Police drone", "Razor wire", "MG nest", "Armoured van", "Sandbags", "Concrete barrier",
            "Command post", "Special forces", "Police boat"};
    static final String[] EVENT_NAMES = {"Horde", "Panic", "Outbreak", "Supply drop", "Airstrike",
            "City alarm", "Infect", "Flare", "Mines", "Barrage", "Airborne", "Mega horde", "Plane crash", "Prison break", "Riot",
            "Supply convoy"};
    static final int TOOL_COP = 3;
    // (Negative roles are the other agencies: World.spawnCop.)
    static final int[] COP_ROLES = {0, Entity.ROLE_RIOT, Entity.ROLE_SWAT, Entity.ROLE_MARKSMAN,
            -1, -2, -3, Entity.ROLE_SAR, Entity.ROLE_K9, -4};
    static final String[] COP_NAMES = {"Cop", "Riot cop", "SWAT", "Marksman", "Highway patrol", "Sheriff's deputy",
            "Park ranger", "Search & rescue", "K9 unit", "Motorcycle cop"};
    static final String[] CLEAR_NAMES = {"Everyone", "Zombies only", "Bodies & blood", "Wrecks",
            "Barricades"};
    static final int[] ZOMBIE_VARIANTS = {Entity.ZOMBIE, Entity.RUNNER, Entity.BRUISER, Entity.BRUTE, Entity.GIANT, Entity.ARMOURED};
    static final int[] CIV_VARIANTS = {Entity.CIVILIAN};
    static final int[] MIL_ROLES = {Entity.ROLE_RIFLE, Entity.ROLE_COMMANDER, Entity.ROLE_SNIPER, Entity.ROLE_GUNNER,
            Entity.ROLE_GUARD, Entity.ROLE_GRENADIER, Entity.ROLE_MARINE, Entity.ROLE_RECON, Entity.ROLE_CORPSMAN, Entity.ROLE_ENGINEER, Entity.ROLE_MORTAR};
    static final String[] MIL_NAMES = {"Soldier", "Commander", "Sniper", "Machine gunner", "National Guard",
            "Grenadier", "Marine", "Recon scout", "Combat medic", "Combat engineer", "Mortar team"};
    // One line about each option in the pickers.
    static final String[] CIV_INFO = {"Goes about their day, runs and hides from zombies"};
    static final String[] COP_INFO = {"Pistol, answers 911 calls", "Shield blocks bites from the front",
            "Body armour and a carbine; sent to the worst trouble",
            "Scoped rifle: picks off the dead from a long way off", "State trooper: works the highways, backs up the town",
            "County deputy: patrols the back roads", "Patrols the trails and dirt roads, guards the ranger station",
            "Rescue specialist: finds the cut-off and gets them out",
            "Handler and police dog: the dog sniffs out hidden zombies and bites",
            "On a police motorcycle: patrols fast and calls in the hordes it passes"};
    static final String[] MIL_INFO = {"Rifle in bursts, grenades for crowds", "Boosts soldiers nearby, calls in support",
            "Long-range scoped rifle", "Belt-fed machine gun", "Guardsmen who protect civilians and safe zones",
            "Grenade launcher for packs of the dead", "Well armed, tough, lands from the sea",
            "Quick and light: scouts ahead and marks hordes", "Patches up wounded police and soldiers (not bites)",
            "Gets a safe zone's wall up three times as fast", "Shells packs of the dead a few streets off"};
    /** What the enemy tool makes: one of the dead. */
    int enemyType() {
        return ZOMBIE_VARIANTS[Math.min(zombieVariant, ZOMBIE_VARIANTS.length - 1)];
    }

    String[] enemyNames() {
        return ENEMY_NAMES;
    }

    /** ((24.0) Worked out once, not on every frame the tool's label is drawn.) */
    static final String[] ENEMY_NAMES = namesOf(ZOMBIE_VARIANTS), CIV_NAMES = namesOf(CIV_VARIANTS);

    static String[] namesOf(int[] types) {
        String[] n = new String[types.length];
        for (int k = 0; k < n.length; k++) n[k] = Entity.NAMES[types[k]];
        return n;
    }

    /** ((24.0) The i-th of a set of rectangles kept for reuse from frame to frame, made the first time it's wanted. */
    static RectF pooled(java.util.ArrayList<RectF> pool, int i) {
        while (pool.size() <= i) pool.add(new RectF());
        return pool.get(i);
    }

    /** ((24.0) The marks along a jet's or helicopter's side, for drawing: not a new array for each one each frame. */
    static final float[] SIDE_MARKS = {-3.6f, -2.0f, 2.0f, 3.6f};
    final RectF bannerRect = new RectF(), boatRect = new RectF();
    final java.util.ArrayList<RectF> hotPool = new java.util.ArrayList<RectF>(), timePool = new java.util.ArrayList<RectF>(),
            tacPool = new java.util.ArrayList<RectF>();

    /** ((25.0) The Events button's name: the airstrike's with the seconds left while it's not ready. */
    String eventLabel() {
        int wait = eventVariant == 4 ? world.airstrikeWait() : 0;
        return wait > 0 ? "Airstrike " + wait + "s" : EVENT_NAMES[eventVariant];
    }

    String rowLabel(int t) {
        return ROW_LABELS[t];
    }

    String toolName(int t) {
        return TOOL_NAMES[t];
    }

    static final String[] ZOMBIE_INFO = {"Slow, relentless shambler", "Fast and fragile",
            "Between the two: quicker than a zombie, tougher, a heavy bite and a short lunge", "Big and tough, shoves people aside",
            "Huge and slow: smashes barricades, throws cars aside; takes a squad, a tank or explosives",
            "A dead officer or soldier still in armour: the helmet stops the first shot to the head"};
    static final String[] PLACE_INFO = {"A car in traffic", "A police car with two officers", "Holds the area for a few minutes",
            "Drag to build a wall zombies must batter down", "Ammo for anyone passing", "Heals the hurt nearby",
            "Armoured truck with six tactical officers, off to the worst call", "Two scouts who roam the town and mark every horde",
            "Flies from the station to watch the spot: marks the enemy there (better aim, no hiding in cover) and calls in what it sees",
            "Drag to lay coils: the dead tangle in it, slowed to a third and torn at, until it wears out",
            "Two machine gunners behind sandbags with a crate of ammunition: they hold the spot",
            "Parked with its doors open: a dozen people running from the dead can shelter inside. Drive it away with them",
            "Drag to build: over twice as tough as a wooden barricade", "Drag to build: five times as tough as a wooden barricade",
            "The police's mobile HQ, set up where it's put: steadies officers near it, hands out ammo, runs the calls round it. One at a time",
            "Four elite soldiers with suppressed rifles, roped down from a helicopter where you tap",
            "A police launch on the nearest water: patrols, picks up people trapped at the shore, ferries officers across"};
    static final String[] EVENT_INFO = {"A horde arrives from the edge of town", "People panic and run",
            "An outbreak inside a building", "A crate on a parachute",
            "A jet bombs the spot", "Sirens send everyone to safety", "Someone here is bitten",
            "Burns for 45 seconds: the dead for a long way round are drawn to it",
            "Six mines round the spot: only the dead set them off",
            "Eight mortar shells walked across the spot, a couple of seconds after the call",
            "Six soldiers with a machine gun dropped on the spot, to hold it",
            "Hundreds of the dead come in from the edge of town, heading for the spot",
            "An airliner comes down on the spot: fire, wreckage, survivors - and some infected aboard",
            "Every inmate breaks out of the prison at once (maps with a prison); the police round them up",
            "The crowd here riots for a couple of minutes: looting, cars set alight, riot police called in",
            "Tap a safe zone, survivor base or police station: an escorted column of trucks brings it ammunition"};
    static final int BTN_PAUSE = 0, BTN_SPEED = 1, BTN_BRUSH = 2, BTN_CLEAR = 3, BTN_MENU = 4, BTN_TOP = 5;
    /** ((21.4) The 3D / bird's-eye button went to Settings, View.) */
    static final int TOP_BUTTONS = 6;
    static final int[] SPEEDS = {1, 2, 4, 8};
    static final int[] BRUSHES = {1, 10, 25, 50};
    static final int[] SHOP_AWNINGS = {0xFFD83A3A, 0xFF2E7D4F, 0xFF2E5FB0, 0xFFE8A21C, 0xFF8A2E6B};
    static final int[] ROW_COLORS = {0xFFF0AD4E, 0xFF4F7BE0, 0xFF8FA05A, 0xFF7CC24E};
    static final String[] ROW_LABELS = {"Civilians", "Cops", "Military", "Zombies"};

    World world;
    final Random rnd = new Random();
    float dp;
    final float baseDp;
    final Settings settings;
    final Sound sound;
    final Menu menu;
    final Records records;
    float recordTimer;
    /**
     * Big announcements (the war turning, achievements) are queued and shown one at a time in a single band,
     * so they never land on top of each other: {title, subtitle, colour}.
     */
    final java.util.ArrayList<Object[]> headlines = new java.util.ArrayList<Object[]>();
    float headTime;
    int seenBanner;
    static final float HEAD_SECONDS = 4.5f;
    /** Where the radio feed ended this frame (the headline band goes below it in portrait). */
    float feedBottom;

    void headline(String title, String sub, int color) {
        // A newer word on the war replaces one still waiting; never more than three in the queue.
        for (int i = headlines.size() - 1; i >= 1; i--)
            if (((Integer) headlines.get(i)[2]) == color && color != 0xFFFFD86B) headlines.remove(i);
        if (headlines.size() >= 3) headlines.remove(headlines.size() - 1);
        headlines.add(new Object[]{title, sub, color});
    }
    boolean hasGame;
    float menuTime, savedCamX, savedCamY, savedScale;
    float fps, fpsTimer;
    int lastMessageCount;
    float frameDt;
    /** The tool bar folded away, and the minimap closed (for more of the city on screen). */
    boolean toolsFolded, miniClosed;
    final RectF foldTab = new RectF(), miniX = new RectF(), mapChip = new RectF();
    /**
     * The radio feed on screen is paced: new messages wait their turn, each new line comes up a few seconds
     * after the last and stays long enough to read. (A backlog is thinned out; everything is in the log.)
     */
    final java.util.ArrayList<Dispatch.Message> feedQueue = new java.util.ArrayList<Dispatch.Message>(),
            feedOn = new java.util.ArrayList<Dispatch.Message>();
    final java.util.ArrayList<Float> feedOnAge = new java.util.ArrayList<Float>();
    int feedSeen;
    float feedGap;
    static final float FEED_LIFE = 14, FEED_EVERY = 3.5f;
    final RectF feedRect = new RectF();
    final RectF[] feedLines = {new RectF(), new RectF(), new RectF(), new RectF()};
    final Dispatch.Message[] feedMsgs = new Dispatch.Message[4];
    int feedCount;
    int fpsFrames;

    float camX, camY, scale = 2f;
    boolean running, simPaused;
    long lastFrame;
    int speedIdx, brushIdx;
    /** (11.3) Frames that failed to draw (and were skipped). */
    int drawErrors;
    int tool = TOOL_PAN;
    int iconCopBody;
    final RectF tmpRect = new RectF();

    /** The police picker's names, with this country's highway and rural police. */
    String[] copNames() {
        String[] n = COP_NAMES.clone();
        Country land = world.city.country;
        if (land.hpName != null) n[4] = land.hpName;
        n[5] = land.ruralName != null ? land.ruralName : "Rural police";
        return n;
    }
    int zombieVariant, civVariant, milVariant, placeVariant, eventVariant, copVariant, buildVariant, zoneVariant, squadVariant;
    /** ((22.0) The Zone tool's choices: an ordinary safe zone, or the biggest block near the spot. */
    static final String[] ZONE_NAMES = {"Safe zone", "Large safe zone"};
    static final String[] ZONE_INFO = {"Walls in a block and guards it; it gets stronger the longer it holds",
            "As big a block as there is nearby, for many more people"};
    /** The map changed under the Build tool: redraw it when the finger lifts. */
    boolean buildDirty;
    int lastBuildTile = -1;
    /** What the last tap (or drag) of a spawn or place tool created, so Undo can take it back. */
    final java.util.ArrayList<Object> lastAction = new java.util.ArrayList<Object>();
    final RectF undoRect = new RectF();
    boolean clearMenu;
    /** Director cam: the camera goes wherever the action is. */
    final RectF[] clearRects = new RectF[CLEAR_NAMES.length];
    /** (Since 10.24 the stats start as one short line: tap it for the full breakdown.) */
    boolean statsCollapsed = true, autoCollapsed;
    /** Units picked with the Orders tool. */
    final java.util.ArrayList<Entity> selection = new java.util.ArrayList<Entity>();
    float orderX, orderY, orderMarker;
    Entity follow;
    /** (10.60) Police and soldiers on their way to each call, for its badge; and the followed vehicle's route. */
    final java.util.HashMap<Dispatch.Incident, int[]> inbound = new java.util.HashMap<Dispatch.Incident, int[]>();
    final int[] routeBuf = new int[700];
    final RectF badgeRect = new RectF();
    final Path routePath = new Path();

    /** ((11.5) Everyone on screen this frame, and when the calls' badges were last counted.) */
    final java.util.ArrayList<Entity> onScreen = new java.util.ArrayList<Entity>();
    /** ((11.5) Zoomed out, each side's dots this frame (x, y pairs), drawn together; and the round pen for them.) */
    final float[][] dotBuf = new float[8][];
    final int[] dotN = new int[8];
    final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    {
        dotPaint.setStyle(Paint.Style.STROKE);
        dotPaint.setStrokeCap(Paint.Cap.ROUND);
    }
    /** ((11.5) The part of the map on screen this frame (world units, with a little to spare).) */
    float viewL, viewT, viewR, viewB;
    float inboundAt = -9;

    void tally(Dispatch.Incident inc, boolean army, int n) {
        int[] k = inbound.get(inc);
        if (k == null) inbound.put(inc, k = new int[2]);
        k[army ? 1 : 0] += n;
    }
    /** A vehicle the camera is following (tap one), with its card. */
    Fleet.Vehicle followV;
    boolean wasControlling;
    float hintTime = 14f;

    // Layout.
    final RectF[] toolRects = new RectF[TOOL_NAMES.length];
    final RectF[] topRects = new RectF[TOP_BUTTONS];
    /** The stats panel as drawn this frame (it shrinks when collapsed). */
    final RectF statsShown = new RectF();
    final RectF statsRect = new RectF();
    float barTop;
    boolean portrait;

    // Input.
    static final int MODE_NONE = 0, MODE_UI = 1, MODE_WORLD = 2, MODE_GESTURE = 3;
    int mode;
    float downX, downY, lastX, lastY, lastSpawnX, lastSpawnY;
    // ((11.5) A flicked pan glides on a little: the map's speed in world units a second, and when the finger last moved.)
    float panVX, panVY;
    long panAt;
    boolean dragged;
    float pinchDist, pinchMidX, pinchMidY;

    // Paints and scratch objects.
    final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint bmpPaint = new Paint();
    final RectF oval = new RectF();
    final RectF oval2 = new RectF();
    final Entity[] icons = new Entity[Entity.TYPE_COUNT];
    final Matrix wallMatrix = new Matrix();
    final float[] wallSrc = new float[8], wallDst = new float[8];
    final Rect roofSrc = new Rect();
    final RectF roofDst = new RectF();
    City.Building[] visible = new City.Building[64];
    /** Walls go partly see-through when zoomed right in. */
    float wallFade = 1;
    float[] visibleKey = new float[64];

    GameView(Context context) {
        super(context);
        dp = baseDp = getResources().getDisplayMetrics().density;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        for (int i = 0; i < toolRects.length; i++) toolRects[i] = new RectF();
        for (int i = 0; i < TOP_BUTTONS; i++) topRects[i] = new RectF();
        for (int i = 0; i < clearRects.length; i++) clearRects[i] = new RectF();
        buildIcons();
        settings = new Settings(context);
        City.realistic = settings.realistic();
        sound = new Sound(context);
        menu = new Menu(this, settings, dp);
        records = new Records(context);
        menu.records = records;
        menuCity();
        applySettings();
        // ((12.0) For a keyboard and mouse: a Chromebook, or a phone with a keyboard plugged in.)
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
    }

    /** The main menu's live demo city in the background: a Tiny one. */
    void menuCity() {
        CityConfig demo = new CityConfig();
        // (Not the Islands: it's too big to build behind the menu.)
        do demo.v[CityConfig.OPT_PRESET] = rnd.nextInt(CityConfig.ISLANDS);
        while (CityConfig.retired(CityConfig.OPT_PRESET, demo.v[CityConfig.OPT_PRESET]));
        demo.v[CityConfig.OPT_MILITARY] = 1;
        demo.v[CityConfig.OPT_ZOMBIES] = 2;
        // ((14.3) A Tiny one: it was a Large city of 20,000 people (120 MB, and seconds of building as the app opened),
        // which sat in memory alongside a big saved game while it loaded - and a Colossal game on top of it ran the
        // phone out of memory.)
        demo.v[CityConfig.OPT_SIZE] = CityConfig.TINY;
        try {
            loadWorld(demo);
        } catch (OutOfMemoryError oom) {
            // ((14.3) Short of memory even so (another game of ours still being let go of): once more, after a clear-out.)
            world = null;
            System.gc();
            loadWorld(demo);
        }
    }

    // ------------------------------------------------------------------ (12.0) keyboard and mouse

    /** The arrow keys (or W, A, S, D) held down. */
    boolean keyUp, keyDown, keyLeft, keyRight, keyMoved;

    @Override
    public boolean onKeyDown(int code, KeyEvent ev) {
        if (loading) return true;
        if (menu.isOpen()) {
            if (code == KeyEvent.KEYCODE_ESCAPE) {
                onBack();
                return true;
            }
            return super.onKeyDown(code, ev);
        }
        if (setKey(code, true)) return true;
        switch (code) {
            case KeyEvent.KEYCODE_SPACE:
                // (Fire, swing or bite while you're someone; otherwise pause.)
                if (world.controlled != null) world.ctrlAttack = true;
                else if (ev.getRepeatCount() == 0) touchInput.pressTop(BTN_PAUSE);
                return true;
            case KeyEvent.KEYCODE_P:
                if (ev.getRepeatCount() == 0) touchInput.pressTop(BTN_PAUSE);
                return true;
            case KeyEvent.KEYCODE_1:
            case KeyEvent.KEYCODE_2:
            case KeyEvent.KEYCODE_3:
            case KeyEvent.KEYCODE_4:
                speedIdx = Math.min(SPEEDS.length - 1, code - KeyEvent.KEYCODE_1);
                click();
                return true;
            case KeyEvent.KEYCODE_EQUALS:
            case KeyEvent.KEYCODE_PLUS:
            case KeyEvent.KEYCODE_NUMPAD_ADD:
                zoomBy(1.25f, getWidth() / 2f, barTop / 2f);
                return true;
            case KeyEvent.KEYCODE_MINUS:
            case KeyEvent.KEYCODE_NUMPAD_SUBTRACT:
                zoomBy(0.8f, getWidth() / 2f, barTop / 2f);
                return true;
            case KeyEvent.KEYCODE_H:
                if (ev.getRepeatCount() == 0) hudHidden = !hudHidden;
                return true;
            case KeyEvent.KEYCODE_TAB: {
                // Tab: on to the next of the people you've pinned.
                if (ev.getRepeatCount() > 0 || world.controlled != null) return true;
                java.util.ArrayList<Entity> live = new java.util.ArrayList<Entity>();
                for (Entity p : pinned) if (!p.dead) live.add(p);
                if (live.isEmpty()) {
                    world.say("Pin someone first (the star on their card)");
                    return true;
                }
                int at = live.indexOf(follow);
                follow = live.get((at + 1) % live.size());
                followV = null;
                inspectB = null;
                inspectZ = null;
                inspectBase = null;
                click();
                return true;
            }
            case KeyEvent.KEYCODE_M:
                // (M: the big map, open and shut. It used to open the pause menu when the map wasn't up.)
                if (ev.getRepeatCount() > 0 || world.controlled != null) return true;
                tactical = !tactical;
                tacDown = false;
                tacTimer = 0;
                click();
                return true;
            case KeyEvent.KEYCODE_ESCAPE:
                if (ev.getRepeatCount() > 0) return true;
                if (tactical) tactical = false;
                else if (hotOpen) hotOpen = false;
                else if (world.controlled != null && code == KeyEvent.KEYCODE_ESCAPE) touchInput.releaseControl();
                else if (picker >= 0 || clearMenu) {
                    picker = -1;
                    clearMenu = false;
                } else menu.open(Menu.PAUSE);
                click();
                return true;
        }
        return super.onKeyDown(code, ev);
    }

    @Override
    public boolean onKeyUp(int code, KeyEvent ev) {
        if (setKey(code, false)) return true;
        if (code == KeyEvent.KEYCODE_SPACE && world != null) {
            world.ctrlAttack = false;
            return true;
        }
        return super.onKeyUp(code, ev);
    }

    boolean setKey(int code, boolean down) {
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_UP: case KeyEvent.KEYCODE_W: keyUp = down; return true;
            case KeyEvent.KEYCODE_DPAD_DOWN: case KeyEvent.KEYCODE_S: keyDown = down; return true;
            case KeyEvent.KEYCODE_DPAD_LEFT: case KeyEvent.KEYCODE_A: keyLeft = down; return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT: case KeyEvent.KEYCODE_D: keyRight = down; return true;
            default: return false;
        }
    }

    /** The keys held down this frame: they walk whoever you're controlling, or move the map. */
    void applyKeys(float dt) {
        int kx = (keyRight ? 1 : 0) - (keyLeft ? 1 : 0), ky = (keyDown ? 1 : 0) - (keyUp ? 1 : 0);
        if (world.controlled != null) {
            if (kx != 0 || ky != 0) {
                float l = (float) Math.sqrt(kx * kx + ky * ky);
                world.joyX = kx / l;
                world.joyY = ky / l;
                keyMoved = true;
            } else if (keyMoved) {
                // (Let go of the keys: stop, unless a thumb's on the stick.)
                keyMoved = false;
                if (joyId < 0) world.joyX = world.joyY = 0;
            }
            return;
        }
        keyMoved = false;
        if (kx == 0 && ky == 0 || menu.isOpen()) return;
        follow = null;
        followV = null;
        panVX = panVY = 0;
        float sp = 700 * dp / scale * dt;
        camX += kx * sp;
        camY += ky * sp;
        clampCamera();
    }

    /** Zooms by f, keeping the spot under (sx, sy) on the screen where it is. */
    void zoomBy(float f, float sx, float sy) {
        float wx = worldX(sx), wy = worldY(sy);
        float minScale = Math.min(getWidth() / world.city.worldW(), getHeight() / world.city.worldH()) * 0.8f;
        scale = Math.max(minScale, Math.min(10f, scale * f));
        if (follow == null && followV == null) {
            camX = wx - sx / scale;
            camY = wy - sy / scale;
        }
        clampCamera();
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent ev) {
        // The mouse wheel: zooms the map in and out round the pointer, or scrolls a menu's list.
        if ((ev.getSource() & InputDevice.SOURCE_CLASS_POINTER) != 0 && ev.getActionMasked() == MotionEvent.ACTION_SCROLL && !loading) {
            float v = ev.getAxisValue(MotionEvent.AXIS_VSCROLL);
            if (v != 0) {
                if (menu.isOpen()) menu.wheel(v);
                else zoomBy(v > 0 ? 1.15f : 1 / 1.15f, ev.getX(), ev.getY());
            }
            return true;
        }
        return super.onGenericMotionEvent(ev);
    }

    void buildIcons() {
        int[] bodies = {0xFFD9534F, 0xFF23408E, 0xFF55623A, 0xFFF2F2F2, 0xFFB07A3E, 0xFF2E2622, 0xFF4E5A3E, 0xFF6B3A36,
                0xFF4D3F4F, 0xFF3A3036, 0xFF6A6F60, 0xFF5E6444, 0xFF5E6A3E, 0xFF6E7A48, 0xFF8A6A34, 0xFFC0282D, 0xFF7A6A4A};
        int[] heads = {0xFF4A2E1A, 0xFF141C38, 0xFF3C4628, 0xFF4A2E1A, 0xFF8C6232, 0xFF9E2A22, 0xFF7C9A5E, 0xFF9DAA70,
                0xFF6F8D55, 0xFF5C7448, 0xFFC8D0B4, 0xFF4E5438, 0xFFA8BE52, 0xFF8E9A5A, 0xFFC8302A, 0xFF1A1410, 0xFF2E2E30};
        float[] radii = {3.6f, 3.8f, 4f, 3.7f, 2.8f, 3.7f, 3.8f, 3.5f, 6.5f, 8f, 3.5f, 2.8f, 3.6f, 5.4f, 3.9f, 3.7f, 4f};
        for (int t = 0; t < Entity.TYPE_COUNT; t++) {
            Entity e = new Entity();
            e.type = t;
            e.radius = radii[t];
            e.body = bodies[t];
            e.head = heads[t];
            e.skin = e.isUndead() ? heads[t] : t == Entity.DOG ? bodies[t] : 0xFFE0AC69;
            if (t == Entity.ZOMBIE_DOG) e.skin = 0xFF7A8058;
            e.angle = (float) (-Math.PI / 2);
            e.hp = e.maxHp = 1;
            e.phase = 1.2f;
            icons[t] = e;
        }
    }

    void loadWorld(CityConfig cfg) {
        world = new World(cfg);
        world.setMaxPopulation(settings.maxPopulation());
        world.gore = settings.gore();
        world.heavy = settings.heavyDestruction();
        world.populate(cfg);
        follow = null;
        followV = null;
        // ((10.63) Pins belong to the city they were made in: a new map starts with none, not stars left lying on
        // the ground where those people stood in the last one.)
        pinned.clear();
        inspectB = null;
        inspectZ = null;
        inspectBase = null;
        if (getWidth() > 0) centerCamera();
    }

    void applySettings() {
        float want = baseDp * settings.uiScale();
        if (want != dp) {
            dp = want;
            menu.setDp(dp);
            if (getWidth() > 0) onSizeChanged(getWidth(), getHeight(), getWidth(), getHeight());
        }
        world.setMaxPopulation(settings.maxPopulation());
        world.gore = settings.gore();
        world.heavy = settings.heavyDestruction();
        sound.setVolumes(settings.music(), settings.sfx());
        // ((19.0) The groups: the radio and the menus' sounds on their own levels; the listening options.)
        vfx.quality = settings.effects();
        vfx.screen = settings.screenEffects();
        vfx.grading = settings.colourGrading();
        float[] buses = {1, 1, 1, 1, 1, settings.radioVolume() * 1.2f, settings.uiVolume() * 1.2f};
        sound.setMix(buses, settings.ambience(), settings.mono(), settings.nightMode());
        realistic = settings.realistic();
        City.realistic = realistic;
        if (world.city.drawnRealistic != realistic) world.redrawCity();
    }

    /** The Graphics setting, cached for drawing. */
    boolean realistic = true;

    // ------------------------------------------------------------------ Menu.Host

    @Override
    public boolean hasGame() {
        return hasGame;
    }

    /** (See SaveSlots.) */
    @Override
    public boolean saveSlot(int slot) {
        return saveSlots.saveSlot(slot);
    }

    /** ((12.0) What's in each save slot, read once rather than off the storage every frame the menu is up.) */
    // ((15.1) Six slots now, and the autosave.)
    final String[] slotText = new String[Menu.SLOTS + 1];
    final boolean[] slotKnown = new boolean[Menu.SLOTS + 1];

    /** (See SaveSlots.) */
    @Override
    public String slotInfo(int slot) {
        return saveSlots.slotInfo(slot);
    }

    /** (See SaveSlots.) */
    @Override
    public boolean deleteSlot(int slot) {
        return saveSlots.deleteSlot(slot);
    }

    /** (See SaveSlots.) */
    @Override
    public boolean loadSlot(int slot) {
        return saveSlots.loadSlot(slot);
    }

    @Override
    public void watch(Entity e) {
        continueGame();
        Object in = e.dead ? mapLabels.whereIs(e) : null;
        if (in instanceof Fleet.Vehicle) {
            Fleet.Vehicle v = (Fleet.Vehicle) in;
            jumpTo(v.x, v.y);
            followV = v;
        } else if (!e.dead) {
            jumpTo(e.x, e.y);
            follow = e;
        } else world.say(Names.person(e.nameSeed) + " is out of sight");
        click();
    }

    @Override
    public void jumpTo(float x, float y) {
        follow = null;
        followV = null;
        camX = x - getWidth() / scale / 2;
        camY = y - barTop / scale / 2;
        clampCamera();
    }

    /** Photo mode: no buttons or panels until the screen is tapped. */
    boolean hudHidden;

    @Override
    public void hideHud() {
        hudHidden = true;
    }

    @Override
    public boolean copyText(String label, String text) {
        try {
            android.content.ClipboardManager cm = (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null) return false;
            cm.setPrimaryClip(android.content.ClipData.newPlainText(label, text));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public boolean canContinue() {
        return hasGame || saveSlots.saveFile().exists();
    }

    @Override
    public boolean saveGame() {
        return saveSlots.saveSlot(0);
    }

    @Override
    public World world() {
        return world;
    }

    /** Shows a text box for typing a city code; set by the activity (which owns Android's dialogs). */
    interface CodePrompt {
        void ask(String current);
    }

    CodePrompt codePrompt;

    @Override
    public void askCityCode(String current) {
        if (codePrompt != null) codePrompt.ask(current);
    }

    /** Called with whatever the player typed into the city code box. */
    void cityCodeEntered(String code) {
        menu.cityCodeEntered(code);
        invalidate();
    }

    @Override
    public void continueGame() {
        if (!hasGame && menu.screen == Menu.MAIN) {
            // Pick up the saved game (on the loader thread).
            if (saveSlots.saveFile().exists()) loadSave(saveSlots.saveFile());
            return;
        }
        if (menu.screen == Menu.MAIN) {
            camX = savedCamX;
            camY = savedCamY;
            scale = savedScale;
        }
        menu.screen = Menu.NONE;
    }

    /**
     * A new city is built on a thread of its own (a massive one takes a while on a phone, and doing it on the
     * screen's thread froze the app until Android gave up on it); meanwhile the screen says so.
     */
    volatile World built;
    volatile Throwable buildError;
    Thread loader;
    boolean loading;
    float loadingTime;
    String loadFailed;
    float loadFailedTime;

    /** What the loader is doing: a new city, or a saved game (and which file). */
    boolean loadingSave;
    java.io.File loadingFile;

    @Override
    public void startGame(final CityConfig cfg) {
        // (10.50) A new game always starts in the 3D view (the View button still switches it).
        settings.setBuildings3d(true);
        final int maxPop = settings.maxPopulation();
        final boolean gore = settings.gore(), heavy = settings.heavyDestruction();
        startLoader(false, null, new Runnable() {
            @Override
            public void run() {
                World w = new World(cfg);
                w.setMaxPopulation(maxPop);
                w.gore = gore;
                w.heavy = heavy;
                w.populate(cfg);
                built = w;
            }
        });
    }

    /** Loads a saved game on the loader thread. */
    void loadSave(final java.io.File f) {
        startLoader(true, f, new Runnable() {
            @Override
            public void run() {
                try {
                    built = SaveGame.load(f);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    void startLoader(boolean save, java.io.File f, final Runnable job) {
        if (loading) return;
        loading = true;
        loadingSave = save;
        loadingFile = f;
        loadingTime = 0;
        shownProgress = 0;
        City.progress(0, save ? "Loading the save" : "");
        built = null;
        buildError = null;
        // ((17.3) The city on screen - the menu's demo city, or the game you were in (saved first) - is let go of now,
        // not kept until the new one's ready: two cities in memory at once was the peak, and the biggest maps on a
        // phone came close to its limit there.)
        if (world != null) {
            if (hasGame) saveGame();
            world.freeMemory();
            world = null;
        }
        hasGame = false;
        chunks.clear();
        // (And what was kept from it: the minimap's picture and the map-picture renderer's hold on it.)
        miniWorld = null;
        miniMap = null;
        chunkCity = null;
        hotWorld = null;
        tacWorld = null;
        miniCountWorld = null;
        labelCity = null;
        java.util.Arrays.fill(visible, null);
        onScreen.clear();
        plans.clear();
        loose.clear();
        roofLabels.clear();
        byDoor.clear();
        records.forget();
        vfx.forget();
        follow = null;
        followV = null;
        inspectB = null;
        inspectZ = null;
        inspectBase = null;
        selection.clear();
        lastAction.clear();
        pinned.clear();
        System.gc();
        loader = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    job.run();
                    if (released) built = null;
                } catch (Throwable e) {
                    buildError = e;
                }
            }
        }, "city builder");
        loader.start();
        invalidate();
    }

    /** A saved game is ready: into it. */
    void finishLoad() {
        world = built;
        reportShown = world != null && world.warResult != 0;
        built = null;
        loading = false;
        applySettings();
        hasGame = true;
        follow = null;
        followV = null;
        // ((10.63) Pins belong to the city they were made in: a new map starts with none, not stars left lying on
        // the ground where those people stood in the last one.)
        pinned.clear();
        selection.clear();
        inspectB = null;
        inspectZ = null;
        inspectBase = null;
        lastAction.clear();
        tool = TOOL_PAN;
        lastMessageCount = world.dispatch.messageCount;
        feedSeen = world.dispatch.messageCount;
        feedQueue.clear();
        feedOn.clear();
        feedOnAge.clear();
        centerCamera();
        menu.screen = Menu.NONE;
        toolSetup();
    }

    /** Waits for a city being built (for the tests). */
    void awaitLoad() throws InterruptedException {
        if (loader != null) loader.join();
    }

    /** The city is ready: into the game. */
    /** ((17.5) Whether this game's report has been put up at its end already. */
    boolean reportShown;

    void finishStart() {
        reportShown = false;
        world = built;
        built = null;
        loading = false;
        follow = null;
        followV = null;
        // ((10.63) Pins belong to the city they were made in: a new map starts with none, not stars left lying on
        // the ground where those people stood in the last one.)
        pinned.clear();
        inspectB = null;
        inspectZ = null;
        inspectBase = null;
        if (getWidth() > 0) centerCamera();
        records.gameStarted();
        autoCollapsed = false;
        statsCollapsed = true;
        applySettings();
        hasGame = true;
        selection.clear();
        simPaused = false;
        speedIdx = 0;
        tool = TOOL_PAN;
        hintTime = 14f;
        lastMessageCount = 0;
        menu.screen = Menu.NONE;
        toolSetup();
    }

    /** ((18.0) The tool bar for the game just started or loaded. */
    void toolSetup() {
        picker = -1;
        if (getWidth() > 0) onSizeChanged(getWidth(), getHeight(), getWidth(), getHeight());
    }

    @Override
    public void toMainMenu() {
        saveGame();
        savedCamX = camX;
        savedCamY = camY;
        savedScale = scale;
        menu.open(Menu.MAIN);
    }

    @Override
    public void preview(int sfx) {
        sound.play(sfx, 0.85f, 0);
    }

    @Override
    public void quit() {
        if (getContext() instanceof Activity) ((Activity) getContext()).finish();
    }

    @Override
    public void settingsChanged() {
        applySettings();
    }

    @Override
    public void click() {
        sound.play(Sfx.CLICK, 0.8f, 0);
    }

    /** System back button. Returns false when the app should close. */
    boolean onBack() {
        if (loading) return true;
        if (tactical && !menu.isOpen()) {
            tactical = false;
            click();
            return true;
        }
        if (hotOpen && !menu.isOpen()) {
            hotOpen = false;
            click();
            return true;
        }
        if (timeOpen && !menu.isOpen()) {
            timeOpen = false;
            click();
            return true;
        }
        // ((12.0) Back shuts whatever is open over the city first - a picker, the Clear menu, a card, being someone -
        // as on any other app, instead of going straight to the pause menu.)
        if (!menu.isOpen() && hasGame) {
            boolean shut = true;
            if (world.controlled != null) touchInput.releaseControl();
            else if (picker >= 0 || clearMenu) {
                picker = -1;
                clearMenu = false;
            } else if (follow != null || followV != null || inspectB != null || inspectZ != null || inspectBase != null) {
                follow = null;
                followV = null;
                inspectB = null;
                inspectZ = null;
                inspectBase = null;
            } else shut = false;
            if (shut) {
                click();
                return true;
            }
        }
        boolean handled = menu.back();
        if (handled) click();
        return handled;
    }

    /** (14.3) This screen's gone: anything still loading is dropped when it's done, not kept. */
    volatile boolean released;

    void release() {
        released = true;
        sound.release();
        chunks.clear();
        if (world != null) world.freeMemory();
    }

    void centerCamera() {
        if (world == null) return;
        scale = Math.max(getWidth(), getHeight()) / (34f * City.T);
        camX = world.city.worldW() / 2 - getWidth() / scale / 2;
        camY = world.city.worldH() / 2 - getHeight() / scale / 2;
    }

    void resume() {
        sound.resume();
        running = true;
        lastFrame = System.nanoTime();
        postInvalidateOnAnimation();
    }

    void pause() {
        saveGame();
        sound.pause();
        running = false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        portrait = h > w;
        int n = 0;
        for (int i = 0; i < toolRects.length; i++) if (!hiddenTool(i)) n++;
        int[] shown = new int[n];
        for (int j = 0, k = 0; j < BAR_ORDER.length; j++) {
            int i = BAR_ORDER[j];
            if (hiddenTool(i)) toolRects[i].setEmpty();
            else shown[k++] = i;
        }
        int perRow = portrait ? (n + 1) / 2 : n;
        int rows = (n + perRow - 1) / perRow;
        float gap = 5 * dp, rowH = 56 * dp;
        barTop = h - rows * rowH - 8 * dp;
        if (toolsFolded) {
            // Folded away: just a thin strip to bring the tools back.
            barTop = h - 30 * dp;
            for (int i = 0; i < toolRects.length; i++) toolRects[i].setEmpty();
            rows = 0;
        }
        foldTab.set(w / 2f - 30 * dp, barTop - 16 * dp, w / 2f + 30 * dp, barTop);
        float bw = Math.min(104 * dp, (w - 16 * dp - gap * (perRow - 1)) / perRow);
        for (int r = 0; r < rows; r++) {
            int first = r * perRow, count = Math.min(perRow, n - first);
            float x = (w - (bw * count + gap * (count - 1))) / 2;
            float top = barTop + 6 * dp + r * rowH;
            for (int k = first; k < first + count; k++) {
                toolRects[shown[k]].set(x, top, x + bw, top + rowH - 6 * dp);
                x += bw + gap;
            }
        }
        float th = 32 * dp, tgap = (portrait ? 4 : 5) * dp, tw, tx;
        int nb = TOP_BUTTONS;
        if (portrait) {
            tw = (w - 16 * dp - tgap * (nb - 1)) / nb;
            tx = 8 * dp;
        } else {
            tw = Math.min(62 * dp, (w * 0.5f - tgap * (nb - 1)) / nb);
            tx = w - 10 * dp - tw * nb - tgap * (nb - 1);
        }
        for (int i = 0; i < nb; i++) {
            topRects[i].set(tx, 8 * dp, tx + tw, 8 * dp + th);
            tx += tw + tgap;
        }
        float lh = 17 * dp;
        if (portrait) {
            float top = 10 * dp + th + 8 * dp;
            statsRect.set(10 * dp, top, 10 * dp + Math.min(260 * dp, w - 20 * dp), top + lh * 12 + 12 * dp + GRAPH_H * dp);
            feedRect.set(10 * dp, statsRect.bottom + 8 * dp, w - 10 * dp, statsRect.bottom + 8 * dp + 4 * 24 * dp);
        } else {
            statsRect.set(10 * dp, 10 * dp, 200 * dp, 10 * dp + lh * 15 + 12 * dp + GRAPH_H * dp);
            feedRect.set(topRects[0].left, topRects[0].bottom + 8 * dp, w - 10 * dp, topRects[0].bottom + 8 * dp + 4 * 24 * dp);
        }
        if (oldw == 0) {
            centerCamera();
        } else if (world != null) {
            // Keep the same spot in the middle of the screen when the phone rotates.
            camX += (oldw - w) / 2f / scale;
            camY += (oldh - h) / 2f / scale;
            clampCamera();
        }
    }

    // ------------------------------------------------------------------ loop

    /** (10.57) Time not yet simulated, and the shortest step the simulation takes (a thirtieth of a second). */
    float simAcc;
    /** (10.60) How the city is doing, as the stats panel says it (see Dispatch.pressure). */
    static final String[] PRESSURE = {"", "Stretched", "Losing", "Overrun"};
    /** ((13.3) Until the memory in use is next looked at, and until it can be freed again. */
    float memoryCheck, memoryFreedAgo = 99;

    /**
     * ((13.3) Close to the phone's memory limit: let go of everything that can be worked out again (the ways on
     * foot, the sharp close-ups of the map) before it runs out, rather than after - running out was what crashed
     * the biggest maps.)
     */
    void guardMemory(float dt) {
        memoryFreedAgo += dt;
        memoryCheck -= dt;
        if (memoryCheck > 0) return;
        memoryCheck = 2;
        Runtime rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory(), max = rt.maxMemory();
        if (max > 0 && used > max * 0.85f && memoryFreedAgo > 20) trimMemory();
    }

    /** ((13.3) Frees what can be worked out again; also when the phone says it's short of memory. */
    void trimMemory() {
        memoryFreedAgo = 0;
        chunks.clear();
        if (world != null) world.freeMemory();
    }

    /** (10.59) Simulation steps that failed and were skipped (see onDraw). */
    int simErrors;
    static final float SIM_STEP = 1 / 30f;
    /** ((22.0) How long a step of the simulation has been taking, in milliseconds (a running average). */
    float stepMs;

    /** (10.59) Room for the live graph in the stats panel (dp). */
    static final float GRAPH_H = 58;

    @Override
    protected void onDraw(Canvas c) {
        // ((22.6) Whatever goes wrong in a frame - the sounds, the effects, the drawing - costs that frame, not the
        // game: it used to be only the drawing that was guarded, and a crowd zoomed in on could close the app.)
        int saves = c.getSaveCount();
        try {
            frame(c);
        } catch (OutOfMemoryError oom) {
            c.restoreToCount(saves);
            trimMemory();
            System.gc();
            invalidate();
        } catch (RuntimeException ex) {
            c.restoreToCount(saves);
            frameErrors++;
            noteError("Frame", ex);
            invalidate();
        }
    }

    /** ((22.6) Frames that went wrong and were skipped. */
    int frameErrors;

    /** ((22.6) Something that went wrong and was got over: said on screen briefly, and in the log. */
    void noteError(String where, Throwable ex) {
        System.err.println("Zombie City: " + where + " failed: " + ex);
        if (loadFailed == null) {
            StackTraceElement[] st = ex.getStackTrace();
            loadFailed = where + " hiccup: " + ex.getClass().getSimpleName() + (st.length > 0 ? " at " + st[0].getMethodName() + ":" + st[0].getLineNumber() : "");
            loadFailedTime = 4;
        }
    }

    void frame(Canvas c) {
        long now = System.nanoTime();
        float rawDt = (now - lastFrame) / 1e9f;
        float dt = Math.min(0.05f, rawDt);
        lastFrame = now;
        frameDt = dt;
        if (loading) {
            loadingTime += dt;
            if (built != null) {
                if (loadingSave) finishLoad();
                else finishStart();
            } else if (buildError != null) {
                loading = false;
                boolean oom = buildError instanceof OutOfMemoryError;
                if (loadingSave) {
                    // ((12.0) And why, where the save says: a map no longer in the game, or a version this one can't read.)
                    Throwable why = buildError.getCause() != null ? buildError.getCause() : buildError;
                    String reason = why instanceof java.io.IOException && why.getMessage() != null
                            ? (why.getMessage().contains("no longer in the game") ? ": its map is no longer in the game"
                            : why.getMessage().contains("Unsupported save version") ? ": it's from a version of the game this one can't read" : "")
                            : "";
                    loadFailed = oom ? "Not enough memory to load that game on this phone." : "That save couldn't be loaded" + reason + ".";
                    // (A broken autosave goes, so Continue doesn't keep failing; one too big for memory stays.)
                    if (!oom && loadingFile != null && loadingFile.equals(saveSlots.saveFile())) loadingFile.delete();
                } else loadFailed = oom
                        ? "Not enough memory for a city that big on this phone. Try a smaller map size."
                        : "Couldn't build that city (" + buildError.getClass().getSimpleName() + "). Try another.";
                loadFailedTime = 6;
                buildError = null;
                System.gc();
                // ((17.3) Back to the menu, with its demo city put back up.)
                if (world == null) {
                    menuCity();
                    applySettings();
                    centerCamera();
                    menu.open(Menu.MAIN);
                }
            } else {
                hud.drawLoading(c);
                if (running) postInvalidateOnAnimation();
                return;
            }
        }
        fpsFrames++;
        fpsTimer += rawDt;
        if (fpsTimer >= 0.5f) {
            fps = fpsFrames / fpsTimer;
            fpsFrames = 0;
            fpsTimer = 0;
        }

        // ((17.5) The game's over: the after-action report, once the ending's been shown.)
        if (hasGame && world != null && world.warResult != 0 && !reportShown && world.warBannerTime <= 0 && !menu.isOpen()) {
            reportShown = true;
            menu.open(Menu.REPORT);
        }
        boolean inGame = !menu.isOpen();
        boolean simulate = inGame ? !simPaused : menu.liveBackground();
        if (!simulate) simAcc = 0;
        // ((10.57) The city is worked out at most thirty times a second, however fast the screen redraws: a 60 Hz
        // phone used to run the whole simulation every frame, twice the work for the same game (four times on a
        // 120 Hz one). Frames in between just draw it again.)
        if (simulate) {
            simAcc = Math.min(0.25f, simAcc + dt * (inGame ? SPEEDS[speedIdx] : 1));
            if (simAcc < SIM_STEP * 0.97f) simulate = false;
        }
        stepSimulation(inGame, simulate);
        // ((12.0) The tip waits while a headline is up: it was written across it.)
        updateView(dt, inGame);
        // Headlines: one at a time, each for a few seconds.
        updateNewsAndSound(dt, inGame, simulate);

        // ((11.3) Something going wrong while drawing (or memory running out zoomed right in) costs a frame, not
        // the game: the map's close-ups are let go of if it's memory, and what went wrong is shown briefly.)
        drawFrame(c, dt);
        if (inGame && hasGame) tuneQuality(rawDt, (System.nanoTime() - now) / 1e6f);
        if (running) {
            // ((22.0) The city only changes thirty times a second, and nothing's drawn in between those steps: with
            // nobody touching the screen, it's drawn as often as it changes and no more. Drawing it again at the
            // screen's 60 or 120 a second was most of the work on a fast screen, and left the simulation short.
            // Panning, zooming, tapping and the menus still get every frame.)
            boolean lively = menu.isOpen() || mode != MODE_NONE || panVX != 0 || panVY != 0 || System.nanoTime() - lastInputNs < 700_000_000L;
            paced = !lively && !settings.batterySaver();
            if (settings.batterySaver()) postInvalidateDelayed(simPaused && !menu.isOpen() || menu.screen == Menu.PAUSE ? 100 : 33);
            else if (lively) postInvalidateOnAnimation();
            else {
                long spent = (System.nanoTime() - now) / 1_000_000L;
                postInvalidateDelayed(Math.max(1, (simPaused ? 100 : 33) - spent));
            }
        }
    }

    /** ((27.3) From frame.) Steps the simulation for the time gone by (at the chosen speed), in steps short enough to be stable and few enough to keep the frame rate up. */
    void stepSimulation(boolean inGame, boolean simulate) {
        if (simulate) {
            int speed = inGame ? SPEEDS[speedIdx] : 1;
            float total = simAcc;
            simAcc = 0;
            // (10.50) Faster speeds take longer steps rather than only more of them: on a phone the extra steps
            // used to be cut short by the time limit below, and 2x, 4x and 8x hardly ran any faster.
            float maxStep = speed >= 8 ? 0.08f : speed >= 4 ? 0.066f : speed >= 2 ? 0.05f : 0.034f;
            // ((22.0) On a phone where a step takes a while, longer steps rather than more of them: two slow steps a
            // frame made each frame slower, which made for two steps the next frame, and so on - the game ran at a
            // crawl. One longer step a frame keeps it moving; the faster speeds already step this far.)
            if (stepMs > 22) maxStep = Math.max(maxStep, 0.066f);
            else if (stepMs > 12) maxStep = Math.max(maxStep, 0.05f);
            int steps = Math.max(1, (int) Math.ceil(total / maxStep));
            float step = total / steps;
            // Never spend much more than about half a frame simulating: when that's used up, what's left of the
            // time goes in one last, longer step (up to a tenth of a second) instead of being lost.
            long simStart = System.nanoTime();
            guardMemory(frameDt);
            try {
                for (int i = 0; i < steps; i++) {
                    long t0 = System.nanoTime();
                    world.update(step);
                    stepMs += ((System.nanoTime() - t0) / 1e6f - stepMs) * 0.1f;
                    if (i < steps - 1 && System.nanoTime() - simStart > 14000000L) {
                        world.update(Math.min(0.1f, step * (steps - 1 - i)));
                        break;
                    }
                }
            } catch (OutOfMemoryError oom) {
                // (10.54) Out of memory in the middle of a huge outbreak: let go of everything that can be worked out
                // again (the ways on foot, the sharp close-ups of the map) and carry on, rather than crash.
                world.freeMemory();
                chunks.clear();
                System.gc();
            } catch (RuntimeException ex) {
                // ((10.59) Something odd in one step of a vast outbreak: that step is lost, not the whole game.)
                simErrors++;
                System.err.println("Zombie City: a simulation step failed: " + ex);
            }
        }
    }

    /** ((27.3) From frame.) The hint, the order marker and the selection; the camera (flicked, with the keys, drifting on the menu, or after whoever's followed); the music's mood, pins and the records. */
    void updateView(float dt, boolean inGame) {
        if (inGame && hintTime > 0 && headlines.isEmpty()) hintTime -= dt;
        if (orderMarker > 0) orderMarker -= dt;
        for (int i = selection.size() - 1; i >= 0; i--) if (selection.get(i).dead) selection.remove(i);
        // The camera stays on whoever you control; if they die, say so.
        if (world.controlled != null) follow = world.controlled;
        else if (wasControlling) {
            joyId = attackId = -1;
            if (follow != null && follow.dead) world.say("You didn't make it");
        }
        wasControlling = world.controlled != null;
        if (panVX != 0 || panVY != 0) {
            if (mode != MODE_NONE || follow != null || followV != null || menu.liveBackground()) panVX = panVY = 0;
            else {
                float ox = camX, oy = camY;
                camX += panVX * dt;
                camY += panVY * dt;
                clampCamera();
                float k = (float) Math.exp(-4.5f * dt);
                panVX = camX == ox ? 0 : panVX * k;
                panVY = camY == oy ? 0 : panVY * k;
                if (Math.abs(panVX) + Math.abs(panVY) < 8) panVX = panVY = 0;
            }
        }
        if (!menu.isOpen()) applyKeys(dt);
        if (menu.liveBackground()) driftCamera(dt);
        else if (follow != null) {
            if (follow.dead && follow != world.controlled) {
                // Got into a car, a patrol car or a truck: follow that instead.
                Fleet.Vehicle in = follow.removed ? (follow.ride != null ? follow.ride : follow.rig) : null;
                if (in != null && !in.removedFromFleet && world.fleet.vehicles.contains(in)) followV = in;
                else if (follow.removed) {
                    // ((12.0) Gone indoors (or into a car some other way): that building's card, or that car,
                    // instead of the card just closing.)
                    Object at = mapLabels.whereIs(follow);
                    if (at instanceof Fleet.Vehicle) followV = (Fleet.Vehicle) at;
                    else if (at instanceof City.Building && tool == TOOL_PAN) inspectB = (City.Building) at;
                }
                follow = null;
            }
            else {
                float tx = follow.x - getWidth() / scale / 2, ty = follow.y - (barTop / 2) / scale;
                float k = Math.min(1, dt * 6);
                camX += (tx - camX) * k;
                camY += (ty - camY) * k;
            }
        }
        else if (followV != null) {
            if (followV.removedFromFleet || !world.fleet.vehicles.contains(followV)) followV = null;
            else {
                float tx = followV.x - getWidth() / scale / 2, ty = followV.y - (barTop / 2) / scale;
                float k = Math.min(1, dt * 6);
                camX += (tx - camX) * k;
                camY += (ty - camY) * k;
            }
        }
        if (inGame) audio.updateMood(dt);
        if (inGame && ((int) (world.time * 2)) != ((int) ((world.time - dt) * 2))) mapLabels.updatePins();
        if (hasGame && inGame) {
            recordTimer -= dt;
            if (recordTimer <= 0) {
                recordTimer = 2;
                records.check(world);
            }
        }
    }

    /** ((27.3) From frame.) Banners and headlines, the music, and this frame's sounds, radio and captions. */
    void updateNewsAndSound(float dt, boolean inGame, boolean simulate) {
        if (world.bannerSeq != seenBanner) {
            seenBanner = world.bannerSeq;
            // ((19.0) A sting under each big announcement: bright for a win, dark for a loss, an alert for the rest.)
            if (world.warBanner != null && hasGame && !menu.isOpen()) {
                boolean won = world.warResult == 1, lost = world.warResult == 2;
                sound.play(won ? Sfx.STING_WIN : lost ? Sfx.STING_LOSE : Sfx.STING_ALERT, 0.8f, 0);
            }
            if (world.warBanner != null) {
                boolean good = world.warBanner.contains("SURVIVES") || world.warBanner.contains("TURNING");
                headline(world.warBanner, world.warSub, good ? 0xFF8FB8FF : 0xFF9BE070);
            }
        }
        if (!headlines.isEmpty()) {
            headTime += dt;
            if (headTime > HEAD_SECONDS) {
                headlines.remove(0);
                headTime = 0;
            }
        }
        sound.playTrack(inGame || menu.screen == Menu.PAUSE ? mood : Synth.TRACK_MENU);
        if (world.alarmTime > 0) sound.play(Sfx.ALARM, 0.7f, 0);
        // ((19.5) The city's bangs as things to see too, before they're played and cleared; and the effects a frame on.)
        {
            float vw0 = getWidth() / scale, vh0 = getHeight() / scale;
            vfx.events(world, camX, camY, camX + vw0, camY + vh0);
            vfx.update(world, simulate ? dt : 0, camX, camY, camX + vw0, camY + vh0, onScreen);
        }
        audio.playSounds(inGame ? 1f : 0.35f);
        if (inGame) {
            audio.heartbeat(dt, 1f);
            audio.distantSounds(dt, 1f);
        }
        if (world.dispatch.messageCount != lastMessageCount) {
            if (inGame && settings.radio()) audio.radioSound();
            lastMessageCount = world.dispatch.messageCount;
        }
        if (inGame) audio.ambience(dt);
        else audio.ambienceQuiet();
        audio.playLater(dt);
    }

    /** ((27.3) From frame.) Draws it all - the world, the panels, the menu over them - with a drawing error noted rather than fatal; then a failed load's message and the frame rate. */
    void drawFrame(Canvas c, float dt) {
        int saves = c.getSaveCount();
        try {
            worldRenderer.drawWorld(c);
            // ((19.5) Over the whole screen, in the game: danger, flashes, the colour grade, cinematic bars.)
            if (!menu.isOpen() && hasGame) vfx.drawScreen(c, getWidth(), getHeight(), intensity, hudHidden);
            if (menu.isOpen()) menu.draw(c, getWidth(), getHeight(), dt);
            else if (!hudHidden) {
                hud.drawUi(c);
                if (tactical) hud.drawTactical(c);
                else if (hotOpen) hud.drawHot(c);
                else if (timeOpen) hud.drawTimeline(c);
            }
        } catch (OutOfMemoryError oom) {
            c.restoreToCount(saves);
            chunks.clear();
            world.freeMemory();
            System.gc();
        } catch (RuntimeException ex) {
            c.restoreToCount(saves);
            drawErrors++;
            if (loadFailed == null) {
                StackTraceElement[] st = ex.getStackTrace();
                loadFailed = "Drawing hiccup: " + ex.getClass().getSimpleName() + (st.length > 0 ? " at " + st[0].getMethodName() + ":" + st[0].getLineNumber() : "");
                loadFailedTime = 4;
            }
            System.err.println("Zombie City: a frame failed to draw: " + ex);
        }
        if (loadFailed != null) {
            loadFailedTime -= dt;
            if (loadFailedTime <= 0) loadFailed = null;
            else hud.drawBanner(c, loadFailed);
        }
        if (settings.showFps()) {
            text.setTextSize(12 * dp);
            text.setTextAlign(Paint.Align.RIGHT);
            text.setColor(0xFFFFFF66);
            c.drawText((int) (fps + 0.5f) + " FPS" + (world.quality == World.Q_REDUCED ? "  (reduced detail)"
                            : world.quality == World.Q_LOW ? "  (low detail)" : ""), getWidth() - 8 * dp,
                    menu.isOpen() ? getHeight() - 30 * dp : barTop - 8 * dp, text);
        }
    }

    /** ((22.0) When the screen was last touched, and whether frames are being drawn only as often as the city changes. */
    long lastInputNs;
    boolean paced;

    /**
     * Keeps the game smooth on slower phones: it watches how long each frame takes (and the frame rate) and,
     * if the phone is struggling, steps the detail down - people far off screen think less often, there's
     * less background traffic, fewer sparks and specks - and steps it back up once there's room again.
     */
    float workMs = 8, slowT, fastT;

    void tuneQuality(float rawDt, float ms) {
        if (rawDt > 0.25f) return; // (a hitch: the app was paused, or a GC)
        workMs += (ms - workMs) * 0.05f;
        boolean capped = settings.batterySaver() || paced;
        boolean slow = workMs > 21 || (!capped && fps > 0 && fps < 38);
        boolean fast = workMs < 11 && (capped || fps > 52);
        slowT = slow ? slowT + rawDt : 0;
        fastT = fast ? fastT + rawDt : 0;
        if (slowT > 2.5f && world.quality < World.Q_LOW) {
            world.setQuality(world.quality + 1);
            slowT = 0;
        } else if (fastT > 8 && world.quality > World.Q_FULL) {
            world.setQuality(world.quality - 1);
            fastT = 0;
        }
    }

    float shownProgress;

    // ------------------------------------------------------------------ music, minimap

    int mood = Synth.TRACK_GAME, lastShots;
    float moodTimer, moodHold;
    /** (14.4) How tense things are now, 0 to 1 (see updateMood), and the time to the next heartbeat under the music. */
    float intensity;
    float heartCd;

    int moodBase = Synth.TRACK_GAME, moodVariant;
    float moodPlayed;


    final RectF miniRect = new RectF(), camChip = new RectF();
    android.graphics.Bitmap miniMap;
    World miniWorld;
    int[] heat;
    int miniCollapsed = -1;
    final Rect miniSrc = new Rect();

    // ------------------------------------------------------------------ (13.8) hot spots

    /**
     * The busiest fights right now, the most of the dead first: a call or a safe zone each, with how many of the dead
     * and how many police and soldiers are there. Tap one to go there.
     */
    boolean hotOpen, hotDown;
    final RectF hotBtn = new RectF(), hotChip = new RectF(), hotClose = new RectF();
    java.util.List<World.HotSpot> hot = new java.util.ArrayList<World.HotSpot>();
    final java.util.ArrayList<RectF> hotRows = new java.util.ArrayList<RectF>();
    float hotAt = -99;
    World hotWorld;
    static final int HOT_MAX = 8;

    // ------------------------------------------------------------------ (14.4) the outbreak's timeline

    /** The key moments of this outbreak, newest first, with when; tap one that happened somewhere to go there. */
    boolean timeOpen, timeDown;
    final RectF timeBtn = new RectF(), timeChip = new RectF(), timeClose = new RectF();
    final java.util.ArrayList<RectF> timeRows = new java.util.ArrayList<RectF>();
    final java.util.ArrayList<World.Moment> timeShown = new java.util.ArrayList<World.Moment>();

    // ------------------------------------------------------------------ (10.32) tactical map

    /** The whole map full screen: where the dead are, the towns, bases, checkpoints, calls and help coming in. */
    boolean tactical, tacDown;
    final RectF tacBtn = new RectF(), tacRect = new RectF(), tacClose = new RectF();
    /** ((27.5) The overlay button on the minimap's left edge, its legend below the minimap, and the chip with the minimap closed. */
    final RectF overlayBtn = new RectF(), overlayKey = new RectF(), overlayChip = new RectF();
    android.graphics.Bitmap tacMap;
    World tacWorld;
    int tacCollapsed = -1;
    int[] tacZ, tacH;
    float tacTimer;
    static final int TAC_GRID = 64;

    final java.util.ArrayList<RectF> tacUsed = new java.util.ArrayList<RectF>();

    World miniCountWorld;
    float miniCountAt;
    int miniCount;
    android.graphics.Bitmap miniDots;
    Canvas miniDotsCanvas;
    float miniTimer;

    /** Redraws the minimap picture: the map, the infection heatmap and a dot for (most) people. */
    /** The minimap's pictures are this many pixels across (the whole map). */
    static final int MINI = 512;
    /** The part of the world the minimap shows. */
    float miniX0, miniY0, miniW = 1, miniH = 1;

    int[] miniSeen;
    int miniStamp;

    /** Draws the city (no buttons) into a picture for sharing. */
    void screenshotNow() {
        if (shareHandler == null) {
            world.say("Screenshots aren't available here");
            return;
        }
        try {
            android.graphics.Bitmap b = android.graphics.Bitmap.createBitmap(getWidth(), getHeight(), android.graphics.Bitmap.Config.ARGB_8888);
            Canvas bc = new Canvas(b);
            worldRenderer.drawWorld(bc);
            text.setTextAlign(Paint.Align.RIGHT);
            text.setTextSize(12 * dp);
            text.setColor(0xCCFFFFFF);
            bc.drawText(world.city.name + "  -  Zombie City Sandbox  -  city " + world.city.cfg.code(), getWidth() - 10 * dp,
                    getHeight() - 10 * dp, text);
            shareHandler.share(b);
        } catch (Throwable t) {
            world.say("Couldn't take a screenshot");
        }
    }

    /** Saves and shares a picture; set by the activity. */
    interface ShareHandler {
        void share(android.graphics.Bitmap picture);
    }

    ShareHandler shareHandler;

    @Override
    public void screenshot() {
        screenshotNow();
    }

    /** Slowly glides the camera over the city behind the main menu. */
    void driftCamera(float dt) {
        menuTime += dt;
        scale = Math.max(getWidth(), getHeight()) / (30f * City.T);
        float ww = world.city.worldW(), wh = world.city.worldH();
        float tx = ww / 2 + ww * 0.3f * (float) Math.sin(menuTime * 0.021f);
        float ty = wh / 2 + wh * 0.3f * (float) Math.sin(menuTime * 0.029f + 1);
        camX = tx - getWidth() / scale / 2;
        camY = ty - getHeight() / scale / 2;
    }

    /** Plays the simulation's sound events that happened on (or near) the screen. */
    // ------------------------------------------------------------------ haptics

    float lastHp = -1;
    int buzzShots;
    long lastBuzz;

    /** A little buzz when you shoot, a hard one when you're hurt, and a thump for crashes and nearby blasts. */
    float hurtHp = -1;

    /** (14.4) Gunfire, blasts and sirens too far off to hear as they are, gathered: how many, and which way (sum of x). */
    final int[] farCount = new int[3];
    final float[] farX = new float[3];
    final float[] farCd = new float[3];
    int farGun = Sfx.RIFLE;

    // ------------------------------------------------------------------ (19.0) the Sound Update

    float shockCd;
    /** ((19.5) The Visuals Update's effects. */
    final Visuals vfx = new Visuals();
    /** Sounds to play a moment from now: {seconds left, id, volume}. */
    final java.util.ArrayList<float[]> soon = new java.util.ArrayList<float[]>();

    /** The radio as each message comes in: a phone for a 911 call, the army's two beeps, the police's hiss; then the squelch. */
    float radioQuietUntil;

    // ---- The ambience ----

    float ambT;

    // ---- Captions ----

    /** On the screen now: {kind, x direction, y direction, seconds left}. */
    final java.util.ArrayList<float[]> captions = new java.util.ArrayList<float[]>();
    static final String[] CAPTION_TEXT = {"Gunfire", "Explosion", "Siren", "Screaming", "Alarm", "Helicopter", "Crash",
            "Building collapsing", "Horde"};

    // ------------------------------------------------------------------ world rendering

    final android.graphics.Rect mapSrc = new android.graphics.Rect();
    final RectF mapDst = new RectF();

    // ------------------------------------------------------------------ sharp close-ups of the map

    /**
     * The map picture holds about one pixel per world unit, so zoomed in it would look soft. Close in, the
     * squares of map on screen are drawn again at several pixels per unit (in the background, a few at a time)
     * and laid over it; the last few dozen are kept.
     */
    static final float CHUNK = 192;
    static final int MAX_CHUNKS = 44;
    /** How fine the close-ups being drawn are (pixels per world unit), or 0 when there are none. */
    int closeRes;
    final java.util.concurrent.ConcurrentHashMap<Long, android.graphics.Bitmap> chunks =
            new java.util.concurrent.ConcurrentHashMap<Long, android.graphics.Bitmap>();
    final java.util.Set<Long> chunkPending = java.util.Collections.newSetFromMap(
            new java.util.concurrent.ConcurrentHashMap<Long, Boolean>());
    final java.util.concurrent.LinkedBlockingDeque<Object[]> chunkJobs = new java.util.concurrent.LinkedBlockingDeque<Object[]>();
    final java.util.HashSet<Long> chunksWanted = new java.util.HashSet<Long>();
    volatile java.util.Set<Long> chunksOnScreen = new java.util.HashSet<Long>();
    volatile City chunkCity;
    volatile int chunkVersion;
    Thread chunkWorker;

    final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    /** Each building's roof lettering, looked up once per map drawing. */
    final java.util.IdentityHashMap<City.Building, java.util.ArrayList<City.Label>> roofLabels =
            new java.util.IdentityHashMap<City.Building, java.util.ArrayList<City.Label>>();
    City labelCity;
    int labelVersion = -1;

    /** Which sides already have a dot in each patch of the screen this frame (frame number << 8 | sides). */
    int[] dotSeen;
    int dotFrame;

    /**
     * Draws trees and buildings standing up, GTA 2 style: a camera hangs above the middle of the screen,
     * so anything tall leans away from the centre and you see the walls that face the camera.
     */
    final Path pinePath = new Path();

    final android.graphics.Path wearPath = new android.graphics.Path();

    /** What you see through a see-through roof: the floor, rooms, furniture and anyone hiding inside. */
    int ihash;
    float ia;

    Entity[] puppets;
    final java.util.HashMap<City.Building, Integer[]> byDoor = new java.util.HashMap<City.Building, Integer[]>();

    final java.util.HashMap<City.Building, Interiors.Plan> plans = new java.util.HashMap<City.Building, Interiors.Plan>();
    final java.util.HashMap<City.Building, java.util.ArrayList<float[]>> loose = new java.util.HashMap<City.Building, java.util.ArrayList<float[]>>();

    /** (17.1) The walls to draw for the plan being drawn: the building's own, as people walk round them. */
    float[] planWalls;

    final Path carPath = new Path();

    /** ((25.0) How much bigger a giant is drawn than the room it takes up. */
    static final float GIANT_DRAW = 2.3f;

    static final int[] LEAF_DARK = {0xFF24481C, 0xFF2A4E1E, 0xFF2E4A22};
    static final int[] LEAF_MID = {0xFF33662A, 0xFF3A6E2C, 0xFF3F6A30};
    static final int[] LEAF_LIGHT = {0xFF4E8A3C, 0xFF5A9444, 0xFF55883E};

    float screenX(float wx) {
        return (wx - camX) * scale;
    }

    float screenY(float wy) {
        return (wy - camY) * scale;
    }

    /** Names over safe zones and 911 markers, drawn at screen size so they stay readable. */
    /** True if a label would sit under the stats panel, the radio feed or the top buttons. */
    /**
     * Zoomed out, where the dead are thick on the ground glows red (the more of them, the stronger), so the
     * front lines read at a glance. Counted on a coarse grid a few times a second.
     */
    int[] dangerGrid;
    float dangerTimer;
    static final int DANGER_CELL = 96;

    /** (11.5) The districts' names, letter-spaced for the map. */
    final java.util.HashMap<City.District, String> spacedNames = new java.util.HashMap<City.District, String>();

    /** Map labels already placed this frame: a label that would land on one of them (or on the panels) is skipped. */
    final RectF[] placed = new RectF[80];
    int placedCount;

    /** ((16.3) The place the camera is over, named for a moment as it moves into it, close in. */
    City.District bannerDistrict;
    String bannerTitle, bannerSub;
    float bannerT;

    /** ((12.0) Each radio line as cut to fit the screen, and the width it was cut for.) */
    final java.util.WeakHashMap<Dispatch.Message, String> feedFit = new java.util.WeakHashMap<Dispatch.Message, String>();
    final java.util.WeakHashMap<Dispatch.Message, Float> feedFitW = new java.util.WeakHashMap<Dispatch.Message, Float>();

    static int alpha(int color, float a) {
        int base = (color >>> 24) & 0xFF;
        int na = (int) (base * Math.max(0, Math.min(1, a)));
        return (na << 24) | (color & 0xFFFFFF);
    }

    static final int[] HAIR = {0xFF1A1410, 0xFF3B2A1A, 0xFF6B4A2A, 0xFFC8A060, 0xFF8A3A1A, 0xFF8A8A8A};
    static final int[] HATS = {0xFFD83A3A, 0xFF2E5FB0, 0xFF3C8A4E, 0xFFE0C050, 0xFF222428, 0xFFE07A2E};

    // ------------------------------------------------------------------ UI rendering

    /**
     * The tug of war under the stats panel: how much of the fighting strength is on the people's side. The
     * marker slides as the battle swings, and a big banner marks turning points and the end of the war.
     */
    /** Where the war meter's panel ends (0 when it isn't shown), and the space it takes. */
    float meterBottom;
    final RectF meterRect = new RectF();

    /** ((12.0) When Clear Everyone was first tapped (nanoTime), waiting for the second tap. */
    long clearArmedAt;

    final RectF driveRect = new RectF();

    // ------------------------------------------------------------------ taking control

    final RectF takeRect = new RectF(), releaseRect = new RectF(), pinRect = new RectF();
    /** Pinned characters: an icon at the edge of the screen points to each one that's off screen. */
    final java.util.ArrayList<Entity> pinned = new java.util.ArrayList<Entity>();
    final RectF[] pinRects = {new RectF(), new RectF(), new RectF(), new RectF(), new RectF(), new RectF()};
    final Entity[] pinIcons = new Entity[6];
    int pinCount;

    /**
     * ((12.0) Where the pinned who are out of sight are - the building they went into, the car they got in - so
     * their pins still point there instead of vanishing until they come out.)
     */
    final java.util.IdentityHashMap<Entity, Object> pinWhere = new java.util.IdentityHashMap<Entity, Object>();

    final Path lastPath = new Path();
    final RectF[] ctxRects = {new RectF(), new RectF(), new RectF(), new RectF(), new RectF(), new RectF()};
    final int[] ctxActions = new int[6];
    int ctxCount;
    /** The stick (a floating one, wherever the left thumb goes down) and the attack button. */
    int joyId = -1, attackId = -1;
    float joyCX, joyCY, joyKX, joyKY, attackX, attackY, attackR;

    /** Name tags already drawn this frame (so they don't pile up in a crowd). */
    final RectF[] tagRects = new RectF[120];

    {
        for (int i = 0; i < tagRects.length; i++) tagRects[i] = new RectF();
    }

    /** The tool whose option picker is open, or -1. */
    int picker = -1;
    final RectF[] pickerRects = new RectF[20];
    final RectF pickerPanel = new RectF();
    int pickerCount;

    // ------------------------------------------------------------------ input

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // ((22.6) A touch that goes wrong is ignored rather than closing the game.)
        try {
            return touchInput.touch(ev);
        } catch (RuntimeException ex) {
            mode = MODE_NONE;
            noteError("Touch", ex);
            return true;
        }
    }

    float worldX(float sx) {
        return sx / scale + camX;
    }

    float worldY(float sy) {
        return sy / scale + camY;
    }

    float boxX0, boxY0, boxX1, boxY1;
    boolean boxing;
    /** ((12.0) The last tap on the map, for a double tap.) */
    long lastTapAt;
    float lastTapX, lastTapY;

    /** ((25.1) The survivor base whose card is showing.) */
    SurvivorBases.Base inspectBase;

    /** ((12.0) The safe zone whose card is showing.) */
    Dispatch.SafeZone inspectZ;

    /** The building whose card is showing. */
    City.Building inspectB;
    static final String[] KIND_NAMES = {"Offices", "House", "Warehouse", "Police station", "Barracks",
            "Watchtower", "Hospital", "Shop", "Church", "School", "Fire station", "Supermarket", "Gas station",
            "Steeple", "Crypt", "Apartment block", "Parking garage", "Pharmacy", "Train station", "Shopping mall",
            "Stadium", "Power station", "Barn", "Silo", "City hall", "Courthouse", "Jail", "Emergency call centre",
            "Public works depot", "National Guard armory"};

    /** ((25.0) The last giant put down that the radio's been told about (once a tap, not once a giant). */
    Entity giantSaid;

    void clampCamera() {
        float vw = getWidth() / scale, vh = getHeight() / scale;
        camX = Math.max(-vw / 2, Math.min(world.city.worldW() - vw / 2, camX));
        camY = Math.max(-vh / 2, Math.min(world.city.worldH() - vh / 2, camY));
    }

    /** ((27.3) For TouchInput, which can't reach the View's own handling of a touch it doesn't want.) */
    boolean superTouch(MotionEvent ev) {
        return super.onTouchEvent(ev);
    }
}
