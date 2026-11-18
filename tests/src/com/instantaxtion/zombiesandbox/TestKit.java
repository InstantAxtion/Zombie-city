package com.instantaxtion.zombiesandbox;

import android.graphics.Canvas;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;

/**
 * What the game's tests share: the list of tests and how test.sh picks and runs them, the cities they're built on
 * ({@link #cfg}), the set-ups that keep a scenario to itself ({@link Quiet}), and small helpers. The tests themselves
 * are in MapTests, WorldTests, ForcesTests, DrivingTests and UiTests; GameTests runs them.
 */
abstract class TestKit {
    // ------------------------------------------------------------------ the list of tests

    /** A test run after every merge (FAST=1 ./test.sh), or only in the full run. */
    static final boolean FAST = true, FULL = false;

    /**
     * One of the 25: a few parts, each a static method of its class that builds what it needs and checks one thing.
     * Every part is played out with its own dice (the test's and the part's names, plus ZC_SEED_SALT), so one part
     * taking another turn doesn't change the next; and every part is tried, a failing one named in the result.
     */
    static final class Entry {
        final String name;
        final boolean fast;
        /** About how many seconds it takes: test.sh's shards are dealt out by it, longest first. */
        final int secs;
        final Class<?> cls;
        final String[] parts;

        Entry(String name, boolean fast, int secs, Class<?> cls, String[] parts) {
            this.name = name;
            this.fast = fast;
            this.secs = secs;
            this.cls = cls;
            this.parts = parts;
        }
    }

    static final java.util.List<Entry> ALL = new java.util.ArrayList<Entry>();
    static int passed, failed;

    static void test(String name, boolean fast, int secs, Class<?> cls, String... parts) {
        ALL.add(new Entry(name, fast, secs, cls, parts));
    }

    /**
     * The tests this run takes: ONLY (a start of a name) picks by name; otherwise FAST=1 keeps the fast tier, and SHARD
     * of SHARDS takes its share - dealt out longest first to whichever shard has least so far, so the shards finish
     * together. (PART=a,b runs only those parts of what's picked: for working on one.)
     */
    static java.util.List<Entry> mine() {
        java.util.List<Entry> out = new java.util.ArrayList<Entry>();
        String only = System.getenv("ONLY");
        if (only != null && !only.isEmpty()) {
            for (Entry e : ALL) if (e.name.startsWith(only)) out.add(e);
            return out;
        }
        boolean fastOnly = "1".equals(System.getenv("FAST"));
        java.util.List<Entry> pick = new java.util.ArrayList<Entry>();
        for (Entry e : ALL) if (!fastOnly || e.fast) pick.add(e);
        String shards = System.getenv("SHARDS");
        if (shards == null) return pick;
        int n = Integer.parseInt(shards), me = Integer.parseInt(System.getenv("SHARD"));
        java.util.List<Entry> byCost = new java.util.ArrayList<Entry>(pick);
        java.util.Collections.sort(byCost, new java.util.Comparator<Entry>() {
            public int compare(Entry a, Entry b) {
                return a.secs != b.secs ? b.secs - a.secs : a.name.compareTo(b.name);
            }
        });
        int[] load = new int[n];
        java.util.Set<Entry> ours = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Entry, Boolean>());
        for (Entry e : byCost) {
            int best = 0;
            for (int k = 1; k < n; k++) if (load[k] < load[best]) best = k;
            load[best] += e.secs;
            if (best == me) ours.add(e);
        }
        for (Entry e : pick) if (ours.contains(e)) out.add(e);
        return out;
    }

    static void runAll() {
        for (Entry e : mine()) run(e);
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    static void run(Entry e) {
        long t = System.currentTimeMillis();
        String only = System.getenv("PART");
        java.util.List<String> wanted = only == null || only.isEmpty() ? null : java.util.Arrays.asList(only.split(","));
        StringBuilder fails = new StringBuilder(), times = new StringBuilder();
        for (String part : e.parts) {
            if (wanted != null && !wanted.contains(part)) continue;
            long t0 = System.currentTimeMillis();
            // ((20.0) Every test plays out the same way each run: the simulation's dice are seeded from its name.)
            // ((27.3) Plus ZC_SEED_SALT (a system property or an environment variable), to play it out with other dice:
            // a test that only passes with one set of them is checking luck, not the game.)
            World.testSeed = ((e.name + "/" + part).hashCode() & 0x7fffffffL) + seedSalt();
            try {
                java.lang.reflect.Method m = e.cls.getDeclaredMethod(part);
                m.setAccessible(true);
                m.invoke(null);
            } catch (Throwable x) {
                Throwable cause = x instanceof java.lang.reflect.InvocationTargetException ? x.getCause() : x;
                fails.append(fails.length() == 0 ? "" : "; ").append(part).append(": ").append(cause);
                System.out.println("  part " + part + " failed: " + cause);
                cause.printStackTrace(System.out);
            }
            City.slowSteps = false;
            Nav.plainSearch = false;
            Jobs.enforce = true;
            World.LOD = true;
            times.append(times.length() == 0 ? "" : ", ").append(part).append(" ").append((System.currentTimeMillis() - t0) / 100 / 10f).append("s");
        }
        if (fails.length() == 0) {
            passed++;
            System.out.println("PASS  " + e.name + "  (" + (System.currentTimeMillis() - t) + " ms)");
        } else {
            failed++;
            System.out.println("FAIL  " + e.name + ": " + fails);
        }
        if (System.getenv("TIMES") != null) System.out.println("      " + times);
    }

    // ------------------------------------------------------------------ the cities the tests are built on

    /**
     * The map version every test city is built on, whatever new cities are: a test's streets don't move when the map
     * version goes up (CityConfig.civic: 39, 40, 41...). A test about a newer map version sets civic itself.
     */
    static final int TEST_CIVIC = 41;

    /** A test city: this map, size and seed, on TEST_CIVIC, the game starting quiet (none of the dead). */
    static CityConfig cfg(int preset, int size, long seed) {
        CityConfig c = new CityConfig();
        c.v[CityConfig.OPT_PRESET] = preset;
        c.v[CityConfig.OPT_SIZE] = size;
        c.v[CityConfig.OPT_ZOMBIES] = 0;
        c.seed = seed;
        c.normalize();
        c.seed = seed;
        c.civic = TEST_CIVIC;
        return c;
    }

    /** The usual test city: Metropolis. */
    static CityConfig cfg(int size, long seed) {
        return cfg(CityConfig.METROPOLIS, size, seed);
    }

    /** A campaign mission's city, on TEST_CIVIC like the rest. */
    static CityConfig mission(int i) {
        CityConfig c = Campaign.config(i);
        c.civic = TEST_CIVIC;
        return c;
    }

    /** A world on that city, its people placed. */
    static World world(CityConfig c) {
        World w = new World(c);
        w.populate(c);
        return w;
    }

    /**
     * A spot on the road between min and max from (x, y) that can be driven to from there: for sending a vehicle
     * somewhere, rather than a fixed offset that may be water or a wall on another map.
     */
    static float[] driveSpot(World w, float x, float y, float min, float max, long seed) {
        java.util.Random r = new java.util.Random(seed);
        for (int k = 0; k < 600; k++) {
            float[] q = w.city.randomStreetSpot(r);
            if (q == null) continue;
            q = w.city.nearestDrivable(q[0], q[1]);
            if (q == null) continue;
            double d = Math.hypot(q[0] - x, q[1] - y);
            if (d >= min && d <= max && w.fleet.canDrive(x, y, q[0], q[1])) return q;
        }
        throw new AssertionError("no road " + (int) min + " to " + (int) max + " away to drive to");
    }

    /**
     * A world out in the wilds: a ranger station and fire lookouts in the country round the town. The first seed from
     * this one whose map has them (how the country round a town falls is the map's business, not the test's).
     */
    static World wildWorld(long seed) {
        for (long s = seed; s < seed + 16; s++) {
            CityConfig c = cfg(CityConfig.LARGE, s);
            City city = new City(c, 0.1f);
            if (city.fireTowers.isEmpty() || city.rangerStationName == null) continue;
            return world(c);
        }
        throw new AssertionError("no map with a ranger station and fire towers");
    }


    /**
     * The roads and lanes joined up (cars cross lots and parked cars): for each tile the piece of road it's on, or -1;
     * [n] is the biggest piece's number, [n + 1] how many pieces of four tiles or more are apart from it.
     */
    static int[] roadPieces(City city) {
        int w = city.w, h = city.h, n = w * h;
        byte[] t = city.tiles;
        int[] comp = new int[n + 2], q = new int[n];
        java.util.Arrays.fill(comp, -1);
        java.util.List<Integer> sizes = new java.util.ArrayList<Integer>();
        for (int i = 0; i < n; i++) {
            if (comp[i] >= 0 || !(t[i] == City.ROAD || t[i] == City.DIRT)) continue;
            int id = sizes.size(), hd = 0, tl = 0;
            q[tl++] = i;
            comp[i] = id;
            while (hd < tl) {
                int cc = q[hd++], x = cc % w, y = cc / w;
                for (int k = 0; k < 4; k++) {
                    int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = y + (k == 2 ? 1 : k == 3 ? -1 : 0);
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                    int j = ny * w + nx;
                    if (comp[j] >= 0 || !(t[j] == City.ROAD || t[j] == City.DIRT || t[j] == City.CAR || t[j] == City.LOT)) continue;
                    comp[j] = id;
                    q[tl++] = j;
                }
            }
            sizes.add(tl);
        }
        int main = 0, stray = 0;
        for (int k = 1; k < sizes.size(); k++) if (sizes.get(k) > sizes.get(main)) main = k;
        for (int k = 0; k < sizes.size(); k++) if (k != main && sizes.get(k) >= 4) stray++;
        comp[n] = main;
        comp[n + 1] = stray;
        return comp;
    }

    // ------------------------------------------------------------------ helpers

    /** ((28.0) Police alive on the map and aboard its vehicles (crews and riders). */
    static int policeNow(World w) {
        int n = 0;
        for (Entity e : w.entities) if (!e.dead && e.type == Entity.COP) n++;
        for (Fleet.Vehicle v : w.fleet.vehicles) {
            for (Entity e : v.crew) if (e.type == Entity.COP) n++;
            for (Entity e : v.riders) if (e.type == Entity.COP) n++;
        }
        return n;
    }

    /** ((28.0) How far someone is from the nearest police station. */
    static float stationDist(World w, Entity e) {
        float best = Float.MAX_VALUE;
        for (City.Facility f : w.city.facilities)
            if (f.kind == City.FACILITY_POLICE) best = Math.min(best, (float) World.hypot(e.x - f.x, e.y - f.y));
        return best;
    }

    /** How far it is along a walking route's corners from (sx, sy). */
    static float routeLength(City city, float sx, float sy, int[] route) {
        float len = 0, x = sx, y = sy;
        for (int t : route) {
            float nx = (t % city.w + 0.5f) * City.T, ny = (t / city.w + 0.5f) * City.T;
            len += (float) Math.hypot(nx - x, ny - y);
            x = nx;
            y = ny;
        }
        return len;
    }

    /** Everyone alive: out and about, inside on errands, sheltering, or riding in a vehicle. */
    static int alive(World w) {
        int n = w.fleet.riderCount();
        for (Entity e : w.entities) if (!e.dead && !e.isZombie()) n++;
        for (City.Building b : w.city.buildings) n += b.visitors.size() + b.occupants.size();
        return n;
    }


    // ------------------------------------------------------------------ ((27.3) set-ups that don't depend on luck

    /**
     * A patch of the map kept to the test's own: everyone else within r of (x, y) - the living, and with zombiesToo the
     * dead as well - and every vehicle there (crew, riders and all) is taken off, when it's made and again each step
     * (step/stepUntil), so nobody wandering or driving in can shoot the test's zombie first, stand in a line of fire or
     * draw the dead off. The kept ones (entities or vehicles) are left alone.
     */
    static final class Quiet {
        float x, y;
        final float r;
        final boolean zombiesToo;
        /** If set, the patch moves with this one (centred on it each sweep). */
        Entity around;
        final java.util.Set<Object> keep = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Object, Boolean>());

        Quiet(float x, float y, float r, boolean zombiesToo) {
            this.x = x;
            this.y = y;
            this.r = r;
            this.zombiesToo = zombiesToo;
        }

        /** Keeps these (entities, vehicles, or whole lists of them) out of the sweep. */
        Quiet keep(Object... o) {
            for (Object k : o) {
                if (k instanceof java.util.Collection) keepAll((java.util.Collection<?>) k);
                else if (k != null) keep.add(k);
            }
            return this;
        }

        Quiet keepAll(java.util.Collection<?> o) {
            for (Object k : o) if (k != null) keep.add(k);
            return this;
        }

        /** The patch goes where e goes. */
        Quiet follow(Entity e) {
            around = e;
            keep.add(e);
            return this;
        }

        void sweep(World w) {
            if (around != null) {
                x = around.x;
                y = around.y;
            }
            for (int i = 0, n = w.entities.size(); i < n; i++) {
                Entity e = w.entities.get(i);
                if (e.dead || keep.contains(e) || (e.isZombie() && !zombiesToo)) continue;
                if (Math.hypot(e.x - x, e.y - y) < r) e.dead = e.removed = true;
            }
            for (int i = w.fleet.vehicles.size() - 1; i >= 0; i--) {
                Fleet.Vehicle v = w.fleet.vehicles.get(i);
                if (keep.contains(v) || Math.hypot(v.x - x, v.y - y) >= r) continue;
                boolean ours = false;
                for (Entity e : v.crew) ours |= keep.contains(e);
                for (Entity e : v.riders) ours |= keep.contains(e);
                if (ours) continue;
                v.crew.clear();
                v.riders.clear();
                v.passengers = 0;
                w.fleet.discard(v);
            }
        }
    }

    /** Keeps everyone but the test's own (and the dead) out of r round (x, y): see Quiet. Swept once now. */
    static Quiet quiet(World w, float x, float y, float r, Object... keep) {
        Quiet q = new Quiet(x, y, r, false).keep(keep);
        q.sweep(w);
        return q;
    }

    /** The same, the dead too (only the kept ones stay). */
    static Quiet empty(World w, float x, float y, float r, Object... keep) {
        Quiet q = new Quiet(x, y, r, true).keep(keep);
        q.sweep(w);
        return q;
    }

    /** One step of the world (a 30th of a second), the quiet patch swept first. */
    static void step(World w, Quiet q) {
        if (q != null) q.sweep(w);
        w.update(1 / 30f);
        w.evCount = 0;
    }

    /** What a test waits for (and what it does to its own each step: hold someone, keep a zombie standing...). */
    interface Cond {
        boolean done();
    }

    /** Steps the world until c is done or secs have gone by; true if it was done. */
    static boolean stepUntil(World w, Quiet q, float secs, Cond c) {
        for (int f = 0, n = Math.round(secs * 30); f < n; f++) {
            if (c.done()) return true;
            step(w, q);
        }
        return c.done();
    }

    /**
     * The dead as they come, with none of the strain's quirks (each world rolls its own: tough, frail, swift...): for a
     * test that measures one of them - its health, its pace - rather than the luck of the roll.
     */
    static void plainStrain(World w) {
        java.util.Arrays.fill(w.trait, false);
        w.mutation = 0;
    }

    /** Pins e where it is meant to be (call each step). */
    static void hold(Entity e, float x, float y) {
        e.x = x;
        e.y = y;
        e.vx = e.vy = 0;
    }

    /** ((27.4) How many tiles a vehicle's route takes it, start to finish. */
    static int tilesTo(World w, Fleet.Vehicle v) {
        int t = w.city.tileIndex(v.x, v.y), n = 0;
        while (v.field.get(t) > 4 && n < 100000) {
            int next = w.fleet.driving.downhill(v.field, t);
            if (next == t) break;
            t = next;
            n++;
        }
        return n;
    }

    /** ((27.4) How many tiles of a traced route come within 20 of any of these. */
    static int near(World w, int[] path, int n, java.util.List<Entity> crowd) {
        int c = 0;
        for (int i = 0; i < n; i++) {
            float x = (path[i] % w.city.w + 0.5f) * City.T, y = (path[i] / w.city.w + 0.5f) * City.T;
            for (Entity z : crowd)
                if (Math.hypot(z.x - x, z.y - y) < 20) {
                    c++;
                    break;
                }
        }
        return c;
    }

    /** A test's zombie that stays where it's put and can't be finished off (call each step). */
    static void untouchable(Entity z) {
        z.speed = z.runSpeed = 0;
        z.vx = z.vy = 0;
        z.hp = z.maxHp = Math.max(z.maxHp, 100000);
    }

    static long seedSalt() {
        String salt = System.getProperty("ZC_SEED_SALT");
        if (salt == null) salt = System.getenv("ZC_SEED_SALT");
        try {
            return salt == null ? 0 : Long.parseLong(salt.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    static Entity pickT(World w, Entity e, float r) throws Exception {
        WM m = wm("pickTarget", Entity.class, float.class);
        m.setAccessible(true);
        return (Entity) m.invoke(w, e, r);
    }

    static Object field(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    static void draw(GameView v, int[] size) throws Exception {
        Field last = GameView.class.getDeclaredField("lastFrame");
        last.setAccessible(true);
        last.set(v, System.nanoTime() - 33000000L);
        BufferedImage img = new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_ARGB);
        java.lang.reflect.Method onDraw = GameView.class.getDeclaredMethod("onDraw", Canvas.class);
        onDraw.setAccessible(true);
        onDraw.invoke(v, new Canvas(img.createGraphics()));
    }

    /**
     * ((27.3) A method of the World's, wherever it now lives: on World itself, or on one of the systems World was split
     * into (w.combat, w.zombieAI, ...). Invoked with the World as the target, like the Method it stands for was.
     */
    static final class WM {
        final java.lang.reflect.Method m;
        final java.lang.reflect.Field holder;

        WM(java.lang.reflect.Method m, java.lang.reflect.Field holder) {
            this.m = m;
            this.holder = holder;
        }

        void setAccessible(boolean b) {
            m.setAccessible(b);
        }

        Object invoke(Object target, Object... args) throws Exception {
            return m.invoke(holder == null ? target : holder.get(target), args);
        }
    }

    static WM wm(String name, Class<?>... types) throws Exception {
        try {
            return new WM(World.class.getDeclaredMethod(name, types), null);
        } catch (NoSuchMethodException e) {
            for (java.lang.reflect.Field f : World.class.getDeclaredFields()) {
                if (f.getType().isPrimitive() || f.getType().getPackage() != World.class.getPackage()) continue;
                try {
                    java.lang.reflect.Method m = f.getType().getDeclaredMethod(name, types);
                    f.setAccessible(true);
                    return new WM(m, f);
                } catch (NoSuchMethodException ignored) {
                }
            }
            throw e;
        }
    }
}
