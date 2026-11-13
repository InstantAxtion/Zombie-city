package com.instantaxtion.zombiesandbox;

import android.graphics.Canvas;
import android.graphics.RectF;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;

/**
 * Runs the game on a desktop JVM (with small stand-ins for the Android classes in tests/stubs) and checks
 * that cities generate, the simulation runs, saves round-trip, checkpoints work and every screen draws. Run with ./test.sh.
 */
public final class GameTests {
    private static int passed, failed;

    public static void main(String[] args) throws Exception {
        test("every map generates at the smallest size, and the classic map at every size", new Check() {
            public void run() {
                // (Every map at Medium is built by the government buildings test.)
                for (int p = 0; p < CityConfig.PRESETS.length + 5; p++) {
                    // (Maps taken out of the game don't build.)
                    if (p < CityConfig.PRESETS.length && CityConfig.removed(p)) continue;
                    CityConfig c = new CityConfig();
                    boolean sizes = p >= CityConfig.PRESETS.length;
                    c.v[CityConfig.OPT_PRESET] = sizes ? 0 : p;
                    c.v[CityConfig.OPT_SIZE] = sizes ? p - CityConfig.PRESETS.length + 1 : CityConfig.TINY;
                    c.seed = 1000 + p;
                    City city = new City(c, 0.1f);
                    String name = CityConfig.PRESETS[c.v[CityConfig.OPT_PRESET]] + " size " + c.v[CityConfig.OPT_SIZE];
                    check(city.buildings.size() > 20, name + " has buildings");
                }
            }
        });
        final World[] world = new World[1];
        test("a busy outbreak runs for two minutes", new Check() {
            public void run() {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_CIVILIANS] = 4;
                c.v[CityConfig.OPT_COPS] = 3;
                c.v[CityConfig.OPT_MILITARY] = 3;
                c.v[CityConfig.OPT_ZOMBIES] = 4;
                c.seed = 77;
                World w = new World(c);
                w.populate(c);
                w.panic(w.city.worldW() / 2, w.city.worldH() / 2);
                w.hordeEvent(w.city.worldW() / 2, w.city.worldH() / 2);
                w.explode(w.city.worldW() / 3, w.city.worldH() / 3, 70, 400);
                for (int i = 0; i < 30 * 120; i++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(w.humans + w.zombies > 0, "someone is left");
                check(w.entities.size() <= w.maxEntities, "population cap holds");
                world[0] = w;
            }
        });
        testAfter("saves load back the same", new Check() {
            public void run() throws Exception {
                World w = world[0];
                File f = File.createTempFile("zcs", ".dat");
                SaveGame.save(w, f);
                World l = SaveGame.load(f);
                check(l.city.cfg.code().equals(w.city.cfg.code()), "same city");
                // (People inside on errands come back out at the door.)
                int visiting = 0;
                for (City.Building b : w.city.buildings) visiting += b.visitors.size();
                int posted = 0;
                for (Fleet.Vehicle v : w.fleet.vehicles) if (v.guard != null && w.entities.contains(v.guard)) posted++;
                int dead = 0;
                for (Entity e : w.entities) if (e.dead) dead++;
                int carried = 0;
                // (Crews aboard patrol cars, engines and patrols are saved as people too.)
                // ((12.9) And a helicopter's flight and the team it lowers.)
                for (Fleet.Vehicle v : w.fleet.vehicles)
                    carried += Math.max(0, v.passengers) + v.crew.size() + (v.team != null ? v.team.size() : 0) + (v.flight != null ? v.flight.size() : 0);
                check(Math.abs(l.entities.size() - (w.entities.size() - dead - posted + visiting)) <= w.fleet.riderCount() + carried + 8,
                        "same people (" + l.entities.size() + " vs " + w.entities.size() + " - " + dead + " dead - " + posted + " posted + " + visiting
                                + " visiting, riders " + w.fleet.riderCount() + ", aboard " + carried + ")");
                check(l.zombiesKilled == w.zombiesKilled, "same stats");
                check(l.histCount == w.histCount && (w.histCount == 0 || l.histArmed[w.histCount - 1] == w.histArmed[w.histCount - 1]),
                        "same police and army history");
                check(l.turnedAway == w.turnedAway && l.medkits.size() == w.medkits.size(), "same checkpoints and medkits");
                f.delete();
            }
        });
        test("the war ends with a winner", new Check() {
            public void run() {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 9;
                World w = new World(c);
                w.populate(c);
                float[] p = w.city.randomWalkable(new java.util.Random(2));
                Entity z = w.spawn(Entity.ZOMBIE, p[0], p[1]);
                for (City.Building b : w.city.buildings) b.lurkers = 0;
                w.recount();
                check(w.outbreak, "an outbreak started");
                z.hp = 0;
                for (int i = 0; i < 30 * 20 && w.warResult == 0; i++) {
                    for (Entity e : w.entities) if (e.isZombie()) e.hp = 0;
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(w.warResult == 1, "the city survived");
                check(w.warBanner != null, "the ending was announced");
                check(w.strainName != null, "the strain has a name");
            }
        });
        test("take control of anyone, and the outbreak replay", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_ZOMBIES] = 3;
                c.seed = 5;
                World w = new World(c);
                w.populate(c);
                for (int i = 0; i < 30 * 20; i++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                Entity soldier = null;
                // (One with room to walk east, not up against a wall.)
                for (Entity e : w.entities)
                    if (!e.dead && e.type == Entity.SOLDIER && (soldier == null || !w.city.circleBlocked(e.x + 12, e.y, e.radius))) soldier = e;
                check(soldier != null, "a soldier to play");
                w.controlled = soldier;
                w.joyX = 1;
                float x0 = soldier.x, y0 = soldier.y;
                for (int i = 0; i < 30 * 2; i++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(w.controlled != soldier || Math.hypot(soldier.x - x0, soldier.y - y0) > 20, "the stick moves them");
                // Put a zombie in front and hold the attack button.
                w.joyX = 0;
                float[] p = w.city.findWalkable(soldier.x + 40, soldier.y);
                Entity z = w.spawn(Entity.ZOMBIE, p[0], p[1]);
                int shots = w.shotsFired;
                w.ctrlAttack = true;
                for (int i = 0; i < 30; i++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(w.controlled != soldier || w.shotsFired > shots, "the attack button shoots");
                // A zombie bites.
                Entity civ = null;
                for (Entity e : w.entities) if (!e.dead && e.type == Entity.CIVILIAN) civ = e;
                Entity me = w.spawn(Entity.ZOMBIE, civ.x + 5, civ.y);
                civ.stun = 5;
                w.controlled = me;
                w.joyX = 0;
                w.ctrlAttack = true;
                int bites = w.bites;
                for (int i = 0; i < 90 && w.bites == bites && !civ.dead && !civ.removed; i++) {
                    // Chase them with the stick.
                    float dx = civ.x - me.x, dy = civ.y - me.y, d = (float) Math.hypot(dx, dy) + 0.01f;
                    w.joyX = dx / d;
                    w.joyY = dy / d;
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(w.controlled != me || w.bites > bites || civ.dead || civ.removed, "a controlled zombie bites");
                w.controlled = null;
                check(!w.replayMeta.isEmpty() && w.replayMeta.size() == w.replayDots.size(), "the replay is recorded");
                File f = File.createTempFile("zcs", ".dat");
                SaveGame.save(w, f);
                World l = SaveGame.load(f);
                check(l.replayMeta.size() == w.replayMeta.size() && l.turnEvents.size() == w.turnEvents.size(), "the replay is saved");
                f.delete();
            }
        });
        test("cars never go round in circles (round a median, a roundabout or a highway gap)", new Check() {
            public void run() {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_SIZE] = CityConfig.MASSIVE;
                c.v[CityConfig.OPT_ZOMBIES] = 2;
                c.seed = 5;
                World w = new World(c);
                w.populate(c);
                java.util.HashMap<Fleet.Vehicle, float[]> m = new java.util.HashMap<Fleet.Vehicle, float[]>();
                int circling = 0;
                for (int f = 0; f < 30 * 120; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                    for (Fleet.Vehicle v : w.fleet.vehicles) {
                        if (v.type == Fleet.HELI || v.type == Fleet.JET || v.type == Fleet.TRAIN || v.parked) continue;
                        float[] t = m.get(v);
                        // turned so far, x0, y0, x1, y1, time, last angle, flagged
                        if (t == null || t[5] > 10) {
                            t = new float[]{0, v.x, v.y, v.x, v.y, 0, v.angle, t != null ? t[7] : 0};
                            m.put(v, t);
                        }
                        float da = v.angle - t[6];
                        while (da > Math.PI) da -= 2 * Math.PI;
                        while (da < -Math.PI) da += 2 * Math.PI;
                        t[0] += da;
                        t[6] = v.angle;
                        t[5] += 1 / 30f;
                        t[1] = Math.min(t[1], v.x);
                        t[2] = Math.min(t[2], v.y);
                        t[3] = Math.max(t[3], v.x);
                        t[4] = Math.max(t[4], v.y);
                        if (t[7] == 0 && Math.abs(t[0]) > 3 * Math.PI * 2 && t[3] - t[1] < 90 && t[4] - t[2] < 90) {
                            t[7] = 1;
                            circling++;
                        }
                    }
                }
                check(circling == 0, circling + " vehicles went round in circles");
            }
        });
        test("new kinds of building, and saved cities still rebuild the same", new Check() {
            public void run() {
                {
                    CityConfig c = new CityConfig();
                    c.seed = 42;
                    c.v[CityConfig.OPT_SIZE] = CityConfig.MASSIVE;
                    City city = new City(c);
                    java.util.HashSet<String> names = new java.util.HashSet<String>();
                    for (City.Building b : city.buildings) if (b.typeName() != null) names.add(b.typeName());
                    int found = 0;
                    for (String n : new String[]{"Barber shop", "Brownstone", "Trailer home", "BBQ smokehouse"}) if (names.contains(n)) found++;
                    check(found >= 2, found + " of the newer kinds of building");
                }
                // (The same city as version 10.0 built from this seed, building for building, for games saved
                // before the government quarter.)
                CityConfig c = new CityConfig();
                c.seed = 42;
                c.v[CityConfig.OPT_PRESET] = 0;
                c.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                c.civic = 0;
                City city = new City(c);
                long h = 17;
                for (City.Building b : city.buildings) h = h * 31 + (long) (b.x0 * 7 + b.y0 * 13 + b.x1 * 17 + b.y1 * 19 + b.kind);
                check(city.buildings.size() == 396 && h == 0x66f79941134006cfL, "same layout as before (" + city.buildings.size() + ", " + Long.toHexString(h) + ")");
            }
        });
        test("doors clear of trees, the armory manned, SWAT called to a horde, parked army trucks used", new Check() {
            public void run() {
                CityConfig c = new CityConfig();
                c.seed = 6;
                c.v[CityConfig.OPT_PRESET] = 0;
                c.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                World w = new World(c);
                w.populate(c);
                City city = w.city;
                int blocked = 0;
                for (City.Building b : city.buildings) {
                    if (b.doorX <= 0) continue;
                    int tx = (int) (b.doorX / City.T), ty = (int) (b.doorY / City.T);
                    for (int dy = -1; dy <= 1; dy++)
                        for (int dx = -1; dx <= 1; dx++) if (city.tiles[(ty + dy) * city.w + tx + dx] == City.TREE) blocked++;
                }
                check(blocked == 0, blocked + " trees in doorways");
                int guards = 0;
                for (Entity e : w.entities)
                    if (e.type == Entity.SOLDIER && e.role == Entity.ROLE_GUARD && Math.hypot(e.x - city.armory.doorX, e.y - city.armory.doorY) < 400) guards++;
                check(guards >= 2, guards + " guardsmen on duty at the armory");
                check(w.dispatch.swatTeams >= 1, "a SWAT team at the precinct");
                // A horde turns up near people: the SWAT team is sent.
                Entity near = null;
                for (Entity e : w.entities) if (e.type == Entity.CIVILIAN && !e.hidden && !e.dead && city.cityHall != null
                        && Math.hypot(e.x - city.cityHall.doorX, e.y - city.cityHall.doorY) < 600) near = e;
                if (near == null) for (Entity e : w.entities) if (e.type == Entity.CIVILIAN && !e.hidden && !e.dead) near = e;
                int teams = w.dispatch.swatTeams;
                // (The 911 call depends on who sees them first: a big enough horde, and long enough, that it always comes.)
                for (int k = 0; k < 20; k++) w.spawn(Entity.ZOMBIE, near.x + 40 + (k % 7) * 3, near.y + (k / 7) * 4);
                boolean sent = false;
                for (int f = 0; f < 30 * 150 && !sent; f++) {
                    w.update(1 / 30f);
                    if (w.dispatch.swatTeams < teams) sent = true;
                }
                check(sent, "SWAT sent to the horde");
                // Army trucks leaving a base take the ones parked there.
                CityConfig b = new CityConfig();
                b.seed = 5;
                b.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                City base = new City(b);
                City.Facility f = base.nearestFacility(City.FACILITY_BASE, 0, 0);
                check(f != null && base.takeParkedTruck(f.x, f.y, 600) != null, "a parked truck drives off the base");
            }
        });
        test("police and soldiers carry a real load and make it count; (10.50: no bloater gas)", new Check() {
            public void run() {
                CityConfig c = new CityConfig();
                c.seed = 3;
                c.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                c.v[CityConfig.OPT_ZOMBIES] = 4;
                World w = new World(c);
                w.populate(c);
                // (An unprepared city has only half the spare rounds.)
                boolean half = w.readiness == 1;
                for (Entity e : w.entities) {
                    if (e.type == Entity.COP && e.role == 0 && e.agency == 0 && e.weapon == Entity.W_STD)
                        check(e.ammo + e.reserve == (half ? 15 + 22 : 60), "an officer carries 60 rounds (" + (e.ammo + e.reserve) + ")");
                    if (e.type == Entity.SOLDIER && e.role == Entity.ROLE_RIFLE)
                        check(e.ammo + e.reserve == (half ? 30 + 90 : 210), "a soldier carries 210 rounds (" + (e.ammo + e.reserve) + ")");
                }
                // ((11.4) Different units carry different guns: patrol officers with shotguns and carbines among them,
                // SWAT with submachine guns or carbines, each named on the card.)
                java.util.List<Entity> police = new java.util.ArrayList<Entity>();
                for (Entity e : w.entities) if (e.type == Entity.COP) police.add(e);
                for (Fleet.Vehicle v : w.fleet.vehicles) for (Entity e : v.crew) if (e.type == Entity.COP) police.add(e);
                java.util.Set<Integer> guns = new java.util.HashSet<Integer>();
                for (Entity e : police) if (e.role == 0 && e.agency == 0) guns.add(e.gunKind());
                check(guns.contains(Entity.W_PISTOL) && guns.contains(Entity.W_SHOTGUN) && guns.contains(Entity.W_CARBINE),
                        "patrol officers carry pistols, shotguns and carbines " + guns);
                Entity swat = w.spawnCop(Entity.ROLE_SWAT, police.get(0).x, police.get(0).y);
                check(swat != null && (swat.weapon == Entity.W_SMG || swat.weapon == Entity.W_CARBINE) && swat.ammo == 30,
                        "SWAT carry a submachine gun or a carbine");
                Entity riot = w.spawnCop(Entity.ROLE_RIOT, police.get(0).x, police.get(0).y);
                check(riot != null && riot.weapon == Entity.W_SHOTGUN && riot.gunName().equals("Shotgun"), "riot officers carry a shotgun");
                for (Entity e : w.entities)
                    if (e.type == Entity.SOLDIER && e.role == Entity.ROLE_GUNNER) check(e.gunName().equals("Machine gun"), "a gunner's machine gun is named");
                // Two minutes into a busy outbreak, hardly anyone has run dry.
                for (int f = 0; f < 30 * 120; f++) w.update(1 / 30f);
                int armed = 0, dry = 0;
                for (Entity e : w.entities) {
                    if (e.dead || (e.type != Entity.COP && e.type != Entity.SOLDIER)) continue;
                    armed++;
                    if (e.ammo + e.reserve == 0) dry++;
                }
                check(dry * 8 <= armed, dry + " of " + armed + " police and soldiers out of ammo after two minutes");
            }
        });
        test("traffic keeps its distance: hardly any crashes, and patrol cars keep to their side", new Check() {
            public void run() {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_PRESET] = 0;
                c.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 3;
                World w = new World(c);
                w.populate(c);
                w.viewX0 = 0;
                w.viewY0 = 0;
                w.viewX1 = w.city.worldW();
                w.viewY1 = w.city.worldH();
                java.util.HashMap<Fleet.Vehicle, Float> cd = new java.util.HashMap<Fleet.Vehicle, Float>();
                int crashes = 0, onRoad = 0, wrong = 0;
                for (int f = 0; f < 30 * 90; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                    for (Fleet.Vehicle v : w.fleet.vehicles) {
                        Float prev = cd.get(v);
                        cd.put(v, v.crashCd);
                        if (prev != null && v.crashCd > prev + 0.05f && v.crashCd > 0.55f) crashes++;
                        if (!v.patrol || v.parked || v.speed < 10 || f % 10 != 0) continue;
                        int tx = (int) (v.x / City.T), ty = (int) (v.y / City.T);
                        if (w.city.junctionIdAt(v.x, v.y) >= 0 || w.city.roadDirAt(tx, ty) == 0) continue;
                        float fx = (float) Math.cos(v.angle), fy = (float) Math.sin(v.angle);
                        int dx = Math.abs(fx) > Math.abs(fy) ? (int) Math.signum(fx) : 0, dy = dx == 0 ? (int) Math.signum(fy) : 0;
                        float act = (v.x - (tx + 0.5f) * City.T) * -dy + (v.y - (ty + 0.5f) * City.T) * dx;
                        float mine = w.city.laneOffset(tx, ty, dx, dy), theirs = -w.city.laneOffset(tx, ty, -dx, -dy);
                        if (Math.abs(mine - theirs) < 6) continue;
                        onRoad++;
                        if (Math.abs(act - theirs) + 3 < Math.abs(act - mine)) wrong++;
                    }
                }
                // (Each crash shows up on both cars.)
                // (About one crash for every 13 vehicles about at most: the bigger maps since 10.10 have more traffic.)
                check(crashes / 2 <= Math.max(6, w.fleet.vehicles.size() / 13), (crashes / 2) + " crashes in a minute and a half of ordinary traffic ("
                        + w.fleet.vehicles.size() + " vehicles)");
                check(onRoad > 50, "patrol cars out on the roads (" + onRoad + " samples)");
                check(wrong <= onRoad * 0.04f, "patrol cars on the wrong side " + wrong + " of " + onRoad);
            }
        });
        test("the Build tool, and its edits in the city code and saves", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 77;
                World w = new World(c);
                w.populate(c);
                int b0 = w.city.buildings.size();
                float[] p = w.city.findWalkable(200, 200);
                int ok = 0;
                for (int r = 0; r < 12 && ok == 0; r++)
                    for (int k = 0; k < 10; k++) if (w.build(p[0] + k * 16, p[1] + r * 32, City.ED_ROAD)) ok++;
                for (int k = 0; k < 40 && w.city.buildings.size() == b0; k++) w.build(p[0] + 30 + k * 40, p[1] + 90, City.ED_HOUSE);
                check(ok > 0 && w.city.buildings.size() == b0 + 1, "roads and a house go in");
                w.city.editsDone();
                w.redrawCity();
                CityConfig c2 = new CityConfig();
                check(c2.applyCode(c.code()) && c2.edits.size() == c.edits.size(), "the city code carries the edits");
                City again = new City(c2, 0.1f);
                int diff = 0;
                for (int i = 0; i < again.tiles.length; i++) if (again.tiles[i] != w.city.tiles[i]) diff++;
                check(diff == 0 && again.buildings.size() == w.city.buildings.size(), "the same city is built from the code");
                File f = File.createTempFile("zcs", ".dat");
                SaveGame.save(w, f);
                World l = SaveGame.load(f);
                check(l.city.buildings.size() == w.city.buildings.size(), "a save keeps the edits");
                f.delete();
                CityConfig plain = new CityConfig();
                check(plain.applyCode("12-48392") && plain.edits.isEmpty(), "plain codes still work");
                check(!plain.applyCode("not a code"), "junk isn't taken for a city code");
            }
        });
        test("10.10: map sizes, the lie of the land, each country's countryside", new Check() {
            public void run() {
                CityConfig c = new CityConfig();
                int last = 0;
                for (int s = CityConfig.TINY; s <= CityConfig.HUGE; s++) {
                    c.v[CityConfig.OPT_SIZE] = s;
                    check(c.tiles() > last, CityConfig.class.getSimpleName() + " size " + s + " is bigger than the one before (" + c.tiles() + ")");
                    last = c.tiles();
                }
                c.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                check(c.tiles() == 320, "Medium is a quarter bigger than it was (" + c.tiles() + ")");
                c.civic = 2;
                check(c.tiles() == 256, "a city saved before 10.10 rebuilds at its old size");
                CityConfig old = new CityConfig();
                check(old.applyCode("12-44") && old.v[CityConfig.OPT_SIZE] == CityConfig.MEDIUM, "old codes keep their size name");
                // A USA city: hills, a mountain, water, a park with trails, a campsite.
                CityConfig u = new CityConfig();
                u.civic = 25; // ((11.0) Map version 25: the country round the town as it was before the thin band.)
                u.seed = 7;
                u.v[CityConfig.OPT_SIZE] = CityConfig.LARGE;
                City city = new City(u, 0.1f);
                int[] n = new int[20];
                for (byte b : city.tiles) n[b]++;
                float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
                for (int q = 0; q < city.elev.length; q++) {
                    float e = city.elev.get(q);
                    lo = Math.min(lo, e);
                    hi = Math.max(hi, e);
                }
                check(hi - lo > 30, "the land isn't flat (" + (int) lo + " to " + (int) hi + ")");
                check(n[City.ROCK] > 0 && n[City.WATER] > 0 && n[City.TRAIL] > 10, "mountain rock, water and trails ("
                        + n[City.ROCK] + ", " + n[City.WATER] + ", " + n[City.TRAIL] + ")");
                check(city.parkName != null, "a nature park");
                boolean camp = false;
                for (City.Building b : city.buildings) if (b.name != null && b.name.startsWith("Camp Store")) camp = b.residents > 0;
                check(camp, "a campsite with campers");
                // Gun stores only where people can buy guns.
                {
                    CityConfig g = new CityConfig();
                    g.seed = 3;
                    g.v[CityConfig.OPT_SIZE] = CityConfig.LARGE;
                    int guns = 0;
                    for (City.Building b : new City(g, 0.1f).buildings) if (b.kind == City.SHOP && b.shopType == 1) guns++;
                    // ((16.9) No gun stores any more.)
                    check(guns == 0, guns + " gun stores (there are none now)");
                }
            }
        });
        test("10.15: walled safe zones with gates, the dead climb fire towers, people keep running", new Check() {
            public void run() {
                CityConfig c = new CityConfig();
                c.civic = 25; // ((11.0) Map version 25: the same streets as when this was written.)
                c.seed = 77;
                c.v[CityConfig.OPT_PRESET] = 0;
                c.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                World w = new World(c);
                w.populate(c);
                w.viewX0 = 0;
                w.viewY0 = 0;
                w.viewX1 = w.city.worldW();
                w.viewY1 = w.city.worldH();
                w.update(1 / 30f);
                // A safe zone: a block walled in, gates onto the street, tents inside.
                // (On a park: open ground all round its edge to wall in.)
                for (float[] a : w.city.openAreas) {
                    if (!w.dispatch.zones.isEmpty()) break;
                    if (!w.city.inTown(a[0], a[1])) continue;
                    w.dispatch.orderZone(a[0], a[1]);
                    if (!w.dispatch.zones.isEmpty() && w.dispatch.zones.get(0).wall.length <= 10) w.dispatch.removeZoneAt(w.dispatch.zones.get(0).x, w.dispatch.zones.get(0).y);
                }
                check(!w.dispatch.zones.isEmpty(), "a safe zone is set up");
                Dispatch.SafeZone z = w.dispatch.zones.get(0);
                check(z.wall.length > 10 && z.gateTiles.length >= 1 && z.tents.size() >= 3 && z.medX > 0 && z.foodX > 0,
                        "a wall (" + z.wall.length + "), gates (" + z.gateTiles.length + "), tents (" + z.tents.size() + "), a medical and a food tent");
                w.alert = 2;
                int seeking = 0;
                for (Entity e : w.entities)
                    if (e.type == Entity.CIVILIAN && !e.dead && Math.hypot(e.x - z.x, e.y - z.y) < 700 && seeking < 30) {
                        e.aware = true;
                        e.task = Dispatch.T_SEEK;
                        seeking++;
                    }
                for (int f = 0; f < 30 * 150 && (!z.open || z.sheltered < 5); f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(z.open, "the wall goes up and it opens");
                int solid = 0;
                for (int k = 0; k < z.wall.length; k++) if (z.up[k] && w.city.solid[z.wall[k]]) solid++;
                check(solid * 10 >= z.wall.length * 9, "the wall is solid (" + solid + " of " + z.wall.length + ")");
                check(z.sheltered >= 5, "people come in through the gates (" + z.sheltered + ")");
                int inside = 0;
                for (Entity e : w.entities) if (!e.dead && e.task == Dispatch.T_SHELTER && e.zone == z && z.inside(e.x, e.y)) inside++;
                check(inside >= 3, "the sheltered are inside the wall (" + inside + ")");
                // The dead claw through a section; it's mended once they're gone.
                int k = z.wall.length / 3;
                w.dispatch.damageWall(z, k, 1000);
                check(!z.up[k] && !w.city.solid[z.wall[k]], "a section broken down is a gap in the wall");
                boolean mended = false;
                // (A guard has to walk over to it first, from wherever on the wall they were.)
                for (int f = 0; f < 30 * 150 && !mended; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                    mended = z.up[k];
                }
                check(mended, "the guards mend the breach");
                // The dead at the wall claw at it.
                for (int i = 0; i < 6; i++) {
                    int kk = (i * z.wall.length / 6 + 1) % z.wall.length;
                    float[] q = z.innerOf(z.wall[kk], w.city.w);
                    int wt = z.wall[kk];
                    float ox = wt % w.city.w == z.tx0 ? -1 : wt % w.city.w == z.tx1 ? 1 : 0;
                    float oy = wt / w.city.w == z.ty0 ? -1 : wt / w.city.w == z.ty1 ? 1 : 0;
                    w.spawn(Entity.BRUTE, q[0] + ox * City.T * 2, q[1] + oy * City.T * 2);
                }
                for (Entity e : w.entities) {
                    if (e.isArmed()) {
                        e.ammo = 0;
                        e.reserve = 0;
                    }
                    // (Nobody left outside for them to go after instead.)
                    if (!e.dead && !e.isZombie() && !z.inside(e.x, e.y) && Math.hypot(e.x - z.x, e.y - z.y) < 900) {
                        e.dead = true;
                        e.removed = true;
                    }
                }
                float weakest = 100;
                for (int f = 0; f < 30 * 8; f++) {
                    w.update(1 / 30f);
                    for (int i = 0; i < z.hp.length; i++) if (i != k) weakest = Math.min(weakest, z.hp[i]);
                }
                check(weakest < 99, "the dead claw at the wall (weakest section " + (int) weakest + ")");
                // Up a fire tower: they have to climb to get the lookout (it takes them a while).
                CityConfig t = new CityConfig();
                t.civic = 25; // ((11.0) Map version 25: the country round the town as it was before the thin band.)
                t.seed = 9;
                t.v[CityConfig.OPT_PRESET] = 6;
                t.v[CityConfig.OPT_SIZE] = CityConfig.LARGE;
                t.v[CityConfig.OPT_ZOMBIES] = 0;
                World tw = new World(t);
                tw.populate(t);
                Entity keeper = tw.towerKeeper[0];
                float hp = keeper.hp;
                Entity climber = tw.spawn(Entity.ZOMBIE, keeper.x + 6, keeper.y + 6);
                for (int f = 0; f < 30 * 3; f++) tw.update(1 / 30f);
                check(keeper.hp >= hp, "not bitten straight away up the tower");
                for (int f = 0; f < 30 * 15; f++) {
                    if (climber.dead) break;
                    tw.update(1 / 30f);
                }
                check(climber.dead || keeper.hp < hp || keeper.dead, "the dead can climb up and get at the lookout");
                // Someone who has just seen one keeps running after it's out of sight.
                // (Someone just out and about in town, with open ground beside them for it to appear on.)
                Entity runner = null;
                for (Entity e : w.entities) {
                    if (e.type != Entity.CIVILIAN || e.dead || e.task != Dispatch.T_NONE || !w.city.inTown(e.x, e.y) || e.aloft
                            || e.leader != null || e.job == Entity.J_LOOKOUT || e.job == Entity.J_RELIEF || e.job == Entity.J_HIKER) continue;
                    float[] q = w.city.findWalkable(e.x + 30, e.y);
                    if (q != null && Math.hypot(q[0] - e.x - 30, q[1] - e.y) < 6) runner = e;
                }
                if (runner != null) {
                    Entity scare = w.spawn(Entity.ZOMBIE, runner.x + 30, runner.y);
                    for (int f = 0; f < 10; f++) w.update(1 / 30f);
                    float sx = scare.x, sy = scare.y;
                    scare.dead = true;
                    scare.removed = true;
                    for (int f = 0; f < 30 * 6; f++) w.update(1 / 30f);
                    // ((17.1) Or gone into a building to hide.)
                    check(runner.dead || runner.task != Dispatch.T_NONE || runner.fleeTimer > 0 || Math.hypot(runner.x - sx, runner.y - sy) > 90
                                    || runner.stayIn != null || w.city.insideOf(runner.x, runner.y) != null,
                            "still getting away six seconds after it's out of sight (" + (int) Math.hypot(runner.x - sx, runner.y - sy) + " away)");
                }
            }
        });
        test("10.16: ammo, rescue teams, backup, rangers, containment, spawning", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.seed = 4;
                c.v[CityConfig.OPT_SIZE] = CityConfig.LARGE;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                World w = new World(c);
                w.populate(c);
                for (int f = 0; f < 30 * 10; f++) w.update(1 / 30f);
                float cx = w.city.worldW() / 2f, cy = w.city.worldH() / 2f;
                // (Open ground near the middle: since 11.1 a big block can sit right on it.)
                for (int r = 0; r < 40 && w.city.findWalkable(cx, cy) == null; r++) cx += City.T * 4;
                // Every kind of officer can be spawned.
                check(w.spawnCop(-1, cx, cy).agency == 1 && w.spawnCop(-2, cx, cy).agency == 2 && w.spawnCop(-3, cx, cy).agency == 3
                        && w.spawnCop(Entity.ROLE_SAR, cx, cy).role == Entity.ROLE_SAR, "highway patrol, deputies, rangers and rescuers spawn");
                // Highway patrol only comes into town as backup once it's under attack.
                for (Fleet.Vehicle v : w.fleet.vehicles) if (v.patrol && v.agency == 0) v.broken = true;
                w.alert = 0;
                Fleet.Vehicle q = w.fleet.patrolFor(cx, cy);
                check(q == null || q.agency == 0, "highway patrol keeps to the highway while it's quiet");
                w.alert = 2;
                q = w.fleet.patrolFor(cx, cy);
                check(q != null && q.agency > 0 && q.agency < 3, "highway patrol comes in as backup when the town is attacked");
                for (Fleet.Vehicle v : w.fleet.vehicles) if (v.patrol && v.agency == 0) v.broken = false;
                // Out of rounds with no ammo left anywhere: borrow from someone with plenty, or scout.
                for (City.Facility f : w.city.facilities) f.ammo = 0;
                if (w.courtArmoury != null) w.courtArmoury.ammo = 0;
                w.alert = 0;
                float[] dp1 = null;
                for (int k = 0; k < 40 && dp1 == null; k++) dp1 = w.city.findWalkable(cx + 300 + k * 20, cy + (k % 5) * 30);
                Entity dry = w.spawnCop(0, dp1[0], dp1[1]), rich = w.spawnCop(0, dp1[0] + 40, dp1[1]);
                if (rich == null) rich = w.spawnCop(0, dp1[0], dp1[1] + 30);
                dry.ammo = 0;
                dry.reserve = 0;
                rich.reserve = 400;
                for (int f = 0; f < 30 * 20; f++) w.update(1 / 30f);
                check(dry.ammo + dry.reserve > 0, "a dry officer borrows rounds from one with plenty (" + (dry.ammo + dry.reserve) + ")");
                for (Entity e : w.entities) if (e.isArmed()) {
                    e.ammo = 0;
                    e.reserve = 0;
                }
                for (int f = 0; f < 30 * 4; f++) w.update(1 / 30f);
                int scouts = 0, armed = 0;
                for (Entity e : w.entities) if (!e.dead && e.isArmed()) {
                    armed++;
                    if (e.scout) scouts++;
                }
                check(scouts * 2 >= armed, "with no rounds anywhere they scout instead (" + scouts + " of " + armed + ")");
                // An outbreak bunched in one place gets a cordon round it.
                World o = new World(c);
                o.populate(c);
                for (int f = 0; f < 30 * 10; f++) o.update(1 / 30f);
                int sp = 0;
                for (int i = 0; i < 1600 && sp < 90; i++) {
                    float x = cx + (i % 40) * 6 - 120, y = cy + (i / 40) * 6 - 120;
                    if (!o.city.solidAt(x, y) && o.spawn(Entity.ZOMBIE, x, y) != null) sp++;
                }
                o.alert = 2;
                int most = 0;
                for (int f = 0; f < 30 * 40; f++) {
                    o.update(1 / 30f);
                    if (f % 30 == 0) {
                        int n = 0;
                        for (Entity e : o.entities) if (!e.dead && e.task == Dispatch.T_CORDON) n++;
                        most = Math.max(most, n);
                    }
                }
                check(most >= 4, "police and soldiers hold a perimeter round the outbreak (" + most + ")");
                // In the wilds: one ranger minds the station; the rescue team is dropped off and the helicopter goes back.
                CityConfig t = new CityConfig();
                t.civic = 25; // ((11.0) Map version 25: the country round the town as it was before the thin band.)
                t.seed = 9;
                t.v[CityConfig.OPT_PRESET] = 6;
                t.v[CityConfig.OPT_SIZE] = CityConfig.LARGE;
                t.v[CityConfig.OPT_ZOMBIES] = 0;
                World tw = new World(t);
                tw.populate(t);
                for (int f = 0; f < 30 * 40; f++) tw.update(1 / 30f);
                City.Building rs = tw.rangerStation;
                boolean minded = false;
                for (Entity e : tw.entities) if (!e.dead && e.agency == 3 && rs != null && Math.hypot(e.x - rs.doorX, e.y - rs.doorY) < 40) minded = true;
                check(minded, "a ranger is at the ranger station");
                float[] tower = tw.city.fireTowers.get(0);
                tw.spawn(Entity.ZOMBIE, tower[0] - 70, tower[1] - 50);
                tw.spawn(Entity.ZOMBIE, tower[0] - 62, tower[1] - 50);
                tw.alert = Math.max(1, tw.alert);
                boolean lowered = false, standby = false;
                for (int f = 0; f < 30 * 150 && !standby; f++) {
                    tw.update(1 / 30f);
                    for (Fleet.Vehicle v : tw.fleet.vehicles) if (v.kind == Fleet.K_RESCUE_HELI) {
                        if (v.team != null) lowered = true;
                        if (v.state == Fleet.STANDBY) standby = true;
                    }
                }
                check(lowered && standby, "the rescue team is lowered and the helicopter waits at base for their call");
            }
        });
        test("10.37: a civilian's brain - runs or hides from the dead, the brave stand and shoot, sticks to a plan", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_PRESET] = 0;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 9;
                c.normalize();
                c.seed = 9;
                World w = new World(c);
                // (The same run every time: what happens next is chance, and a test shouldn't be.)
                w.rnd.setSeed(9);
                w.populate(c);
                for (int f = 0; f < 30 * 3; f++) w.update(1 / 30f);
                // Who someone is decides how brave they are: always the same for the same person, and not
                // everyone alike.
                int brave = 0, timid = 0;
                for (Entity e : w.entities) {
                    if (e.type != Entity.CIVILIAN) continue;
                    check(World.bravery(e) == World.bravery(e) && World.bravery(e) >= 0 && World.bravery(e) <= 1, "bravery from 0 to 1");
                    if (World.bravery(e) > 0.7f) brave++;
                    else if (World.bravery(e) < 0.3f) timid++;
                }
                check(brave > 5 && timid > 5, "some brave, some not (" + brave + " brave, " + timid + " timid)");
                // An unarmed civilian with one of the dead coming: running or hiding within a moment, not carrying on.
                int tried = 0, reacted = 0, armedTried = 0, fought = 0;
                for (Entity e : new java.util.ArrayList<Entity>(w.entities)) {
                    if (tried >= 6 && armedTried >= 4) break;
                    if (e.type != Entity.CIVILIAN || e.dead || e.hidden || e.ride != null || e.leader != null || e.task != Dispatch.T_NONE
                            || e.militia || e.scavenge != null || e.job == Entity.J_LOOKOUT) continue;
                    boolean arm = World.bravery(e) > 0.75f && armedTried < 4;
                    if (!arm && tried >= 6) continue;
                    // (Some people already own a gun, and may well stand and use it: not who this is about.)
                    if (!arm && e.hasGun) continue;
                    // ((12.3) In front of them, where they'd see it coming: from behind, a stunned one standing still
                    // isn't noticed at this distance, by design.)
                    float zx = e.x + (float) Math.cos(e.angle) * 60, zy = e.y + (float) Math.sin(e.angle) * 60;
                    if (w.city.solidAt(zx, zy) || !w.city.los(e.x, e.y, zx, zy)) {
                        zx = e.x + 60;
                        zy = e.y;
                        if (w.city.solidAt(zx, zy) || !w.city.los(e.x, e.y, zx, zy)) {
                            zx = e.x - 60;
                            if (w.city.solidAt(zx, zy) || !w.city.los(e.x, e.y, zx, zy)) continue;
                        }
                    }
                    w.clearZombies();
                    if (arm) w.armCivilian(e, 30);
                    Entity z = w.spawn(Entity.ZOMBIE, zx, zy);
                    z.stun = 2;
                    int ammo = e.ammo + e.reserve;
                    boolean ran = false, shot = false;
                    for (int f = 0; f < 30 && !e.dead; f++) {
                        w.update(1 / 30f);
                        if (e.goal == World.G_FLEE || e.goal == World.G_HIDE || e.goal == World.G_ZONE || e.hidden) ran = true;
                        if (e.goal == World.G_FIGHT) shot = true;
                    }
                    if (arm) {
                        armedTried++;
                        if (shot && e.ammo + e.reserve < ammo) fought++;
                    } else {
                        tried++;
                        // ((14.8) Or stand and fight it with whatever's to hand, if brave enough or cornered.)
                        if (ran || shot) reacted++;
                    }
                    w.clearZombies();
                    for (int f = 0; f < 30 * 2; f++) w.update(1 / 30f);
                }
                check(tried >= 4 && reacted == tried, "the unarmed run, hide or fight back (" + reacted + " of " + tried + ")");
                check(armedTried >= 2 && fought * 2 >= armedTried, "the brave with a gun stand and shoot (" + fought + " of " + armedTried + ")");
                // With no guns lying about, nobody sets off to pick one up.
                w.pickups.clear();
                // An outbreak: everyone sticks to what they decide for a while, rather than flipping between two
                // things every moment.
                java.util.Random r = new java.util.Random(4);
                for (int k = 0; k < 25; k++) {
                    float[] q = w.city.randomWalkableInTown(r);
                    if (q != null) w.spawn(Entity.ZOMBIE, q[0], q[1]);
                }
                java.util.IdentityHashMap<Entity, int[]> seen = new java.util.IdentityHashMap<Entity, int[]>();
                int pick = 0;
                for (int f = 0; f < 30 * 20; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                    for (Entity e : w.entities) {
                        if (e.type != Entity.CIVILIAN || e.dead || e.hidden) continue;
                        int[] g = seen.get(e);
                        if (g == null) seen.put(e, g = new int[]{e.goal, 0});
                        else if (g[0] != e.goal) {
                            g[0] = e.goal;
                            g[1]++;
                        }
                        if (e.goal == World.G_PICKUP && w.pickups.isEmpty()) pick++;
                    }
                }
                int people = 0, switches = 0, flighty = 0;
                for (int[] g : seen.values()) {
                    people++;
                    switches += g[1];
                    if (g[1] > 10) flighty++;
                }
                check(people > 50 && switches < people * 2, "a few changes of mind in twenty seconds, not constant (" + switches + " over " + people + " people)");
                check(flighty * 50 <= people, "hardly anyone changes their mind more than every few seconds (" + flighty + " of " + people + ")");
                check(pick == 0, "nobody goes for a gun that isn't there (" + pick + ")");
            }
        });
        test("10.38: police and soldiers - fall back when overrun, stand when backed up, fight on the way to a ride, pull back hurt", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_PRESET] = 0;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 12;
                c.normalize();
                c.seed = 12;
                // ((16.9) Map version 30, the streets this was written on: in newer cities the FBI field office's agents
                // are near enough to join in, and the officer here isn't alone.)
                c.civic = 30;
                World w = new World(c);
                // (The same run every time: what happens next is chance, and a test shouldn't be.)
                w.rnd.setSeed(38);
                w.populate(c);
                for (int f = 0; f < 30 * 2; f++) w.update(1 / 30f);
                // Somewhere open in town, well away from the stations.
                java.util.Random r = new java.util.Random(6);
                float[] spot = null;
                for (int k = 0; k < 400 && spot == null; k++) {
                    float[] q = w.city.randomWalkableInTown(r);
                    if (q == null || w.armedNear(q[0], q[1], 400) > 0) continue;
                    boolean open = true;
                    for (int a = 0; a < 16 && open; a++) {
                        double an = a * Math.PI / 8;
                        for (int d = 20; d <= 120 && open; d += 20)
                            if (w.city.solidAt(q[0] + (float) Math.cos(an) * d, q[1] + (float) Math.sin(an) * d)) open = false;
                    }
                    if (open) spot = q;
                }
                check(spot != null, "an open square to test in");
                // Alone with a crowd of the dead coming from one side: an officer falls back from it, shooting.
                Entity cop = w.spawn(Entity.COP, spot[0], spot[1]);
                for (int k = 0; k < 14; k++) {
                    double an = (k - 7) * 0.12;
                    w.spawn(Entity.ZOMBIE, spot[0] + (float) Math.cos(an) * (70 + (k % 3) * 12), spot[1] + (float) Math.sin(an) * (70 + (k % 3) * 12));
                }
                boolean fellBack = false;
                for (int f = 0; f < 45 && !cop.dead; f++) {
                    w.update(1 / 30f);
                    if (cop.goal == World.A_FALLBACK) fellBack = true;
                }
                check(fellBack && cop.x < spot[0] - 15, "alone against fourteen, the officer falls back (" + (int) (cop.x - spot[0]) + " px)");
                // The same crowd with a line of officers: they stand and shoot.
                w.clearZombies();
                for (Entity e : w.entities) if (e.isArmed() && Math.hypot(e.x - spot[0], e.y - spot[1]) < 300) e.dead = e.removed = true;
                for (int f = 0; f < 3; f++) w.update(1 / 30f);
                java.util.List<Entity> line = new java.util.ArrayList<Entity>();
                for (int k = 0; k < 6; k++) line.add(w.spawn(Entity.COP, spot[0], spot[1] - 50 + k * 20));
                for (int k = 0; k < 14; k++) {
                    double an = (k - 7) * 0.12;
                    w.spawn(Entity.ZOMBIE, spot[0] + (float) Math.cos(an) * (90 + (k % 3) * 12), spot[1] + (float) Math.sin(an) * (90 + (k % 3) * 12));
                }
                int fallBacks = 0;
                for (int f = 0; f < 30; f++) {
                    w.update(1 / 30f);
                    for (Entity e : line) if (e.goal == World.A_FALLBACK) fallBacks++;
                }
                check(fallBacks == 0, "six officers together hold against fourteen (" + fallBacks + ")");
                // Badly hurt with one in sight: pulls back. (On their own, so nobody else shoots it first.)
                w.clearZombies();
                for (int k = 1; k < line.size(); k++) line.get(k).dead = line.get(k).removed = true;
                Entity hurt = line.get(0);
                hurt.hp = hurt.maxHp * 0.2f;
                hurt.task = Dispatch.T_NONE;
                w.spawn(Entity.ZOMBIE, hurt.x + 60, hurt.y);
                boolean pulledBack = false;
                for (int f = 0; f < 45 && !pulledBack; f++) {
                    w.update(1 / 30f);
                    if (hurt.goal == World.A_WOUNDED) pulledBack = true;
                }
                check(hurt.dead || pulledBack, "a badly hurt officer pulls back");
                // On the way to a vehicle with one of the dead right there: deals with it first.
                w.clearZombies();
                Fleet.Vehicle ride = null;
                for (Fleet.Vehicle v : w.fleet.vehicles)
                    if (!v.broken && !v.parked && Math.hypot(v.x - spot[0], v.y - spot[1]) > 500) {
                        ride = v;
                        break;
                    }
                check(ride != null, "a vehicle to board");
                hurt.dead = hurt.removed = true;
                Entity boarder = w.spawn(Entity.COP, spot[0], spot[1]);
                boarder.task = Dispatch.T_BOARD;
                boarder.rig = ride;
                int ammo = boarder.ammo + boarder.reserve;
                w.spawn(Entity.ZOMBIE, boarder.x + 30, boarder.y);
                for (int f = 0; f < 60 && boarder.ammo + boarder.reserve >= ammo; f++) w.update(1 / 30f);
                check(boarder.ammo + boarder.reserve < ammo, "shoots the one in the way before going on to the vehicle");
                // No crawlers any more, and far fewer of the bitten come back as anything but an ordinary zombie.
                int special = 0;
                // ((12.4) The usual rates: not a strain with many runners or brutes.)
                w.trait[World.TR_RUNNERS] = w.trait[World.TR_BRUTES] = false;
                for (int k = 0; k < 10000; k++) {
                    int t = w.turnType(k % 2 == 0 ? Entity.CIVILIAN : Entity.COP);
                    check(t != Entity.CRAWLER, "nobody comes back a crawler");
                    if (t != Entity.ZOMBIE) special++;
                }
                check(special > 500 && special < 1300, "about one in eleven comes back as a special kind (" + special + " in 10000)");
                // A gun left on the ground is gone after a minute; a bat stays a while longer.
                w.pickups.clear();
                World.Pickup gun = new World.Pickup(), bat = new World.Pickup();
                gun.x = bat.x = spot[0];
                gun.y = bat.y = spot[1] + 200;
                gun.rounds = 10;
                bat.melee = Entity.M_BAT;
                w.pickups.add(gun);
                w.pickups.add(bat);
                for (int f = 0; f < 30 * 55; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(w.pickups.contains(gun) || gun.claimed, "the gun is still there after 55 seconds");
                for (int f = 0; f < 30 * 7; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(!w.pickups.contains(gun), "the gun is gone after a minute");
                check(w.pickups.contains(bat) || bat.claimed, "a bat lasts longer");
            }
        });
        test("10.40: one road network - every home on a road or lane, no roads to nowhere, lanes and drives joined up", new Check() {
            public void run() throws Exception {
                for (int[] ps : new int[][]{{0, 3}, {1, 3}, {4, 3}, {2, 5}}) {
                    CityConfig c = new CityConfig();
                    c.v[CityConfig.OPT_PRESET] = ps[0];
                    c.seed = ps[1];
                    c.normalize();
                    c.seed = ps[1];
                    City city = new City(c, 0.05f);
                    int w = city.w, h = city.h, n = w * h;
                    byte[] t = city.tiles;
                    String m = "map " + ps[0] + "/" + ps[1] + ": ";
                    // The roads and lanes are one network.
                    int[] comp = new int[n];
                    java.util.Arrays.fill(comp, -1);
                    int[] q = new int[n];
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
                                // ((10.52) A drive that joins the rest across a paved lot is joined up: cars cross lots.)
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
                    check(stray == 0, m + "no bits of road or lane on their own (" + stray + ")");
                    // Every home reaches that network by its own drive (or is on a street, by the pavement).
                    int homes = 0, off = 0;
                    int[] seen = new int[n];
                    int stamp = 0;
                    for (City.Building b : city.buildings) {
                        if (b.kind != City.HOUSE && b.kind != City.BARN) continue;
                        homes++;
                        stamp++;
                        int hd = 0, tl = 0;
                        boolean ok = false;
                        for (int y = (int) (b.y0 / City.T) - 1; y <= (int) Math.ceil(b.y1 / City.T); y++)
                            for (int x = (int) (b.x0 / City.T) - 1; x <= (int) Math.ceil(b.x1 / City.T); x++) {
                                if (x < 0 || y < 0 || x >= w || y >= h) continue;
                                int j = y * w + x;
                                if (t[j] == City.SIDEWALK) ok = true;
                                if ((t[j] == City.LOT || t[j] == City.DIRT || t[j] == City.ROAD || t[j] == City.CAR) && seen[j] != stamp) {
                                    seen[j] = stamp;
                                    q[tl++] = j;
                                }
                            }
                        while (hd < tl && !ok && tl < 20000) {
                            int cc = q[hd++], x = cc % w, y = cc / w;
                            if ((t[cc] == City.ROAD || t[cc] == City.DIRT) && comp[cc] == main) ok = true;
                            for (int k = 0; k < 4; k++) {
                                int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = y + (k == 2 ? 1 : k == 3 ? -1 : 0);
                                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                                int j = ny * w + nx;
                                if (seen[j] == stamp || !(t[j] == City.LOT || t[j] == City.DIRT || t[j] == City.ROAD || t[j] == City.CAR)) continue;
                                seen[j] = stamp;
                                q[tl++] = j;
                            }
                        }
                        if (!ok) off++;
                    }
                    check(off * 50 <= homes, m + "every home (bar the odd one) is on a road or lane (" + off + " of " + homes + " not)");
                    // No lane or road ending in the middle of nowhere.
                    int dead = 0;
                    for (int y = 1; y < h - 1; y++)
                        for (int x = 1; x < w - 1; x++) {
                            int i = y * w + x;
                            if (!(t[i] == City.ROAD || t[i] == City.DIRT) || (city.bridge != null && city.bridge[i])) continue;
                            int four = 0;
                            for (int k = 0; k < 4; k++) {
                                byte o = t[i + (k == 0 ? 1 : k == 1 ? -1 : k == 2 ? w : -w)];
                                if (o == City.ROAD || o == City.DIRT || o == City.CAR || o == City.LOT) four++;
                            }
                            if (four > 1) continue;
                            boolean near = false;
                            for (int dy = -1; dy <= 1; dy++)
                                for (int dx = -1; dx <= 1; dx++) {
                                    byte o = t[(y + dy) * w + x + dx];
                                    if (o == City.LOT || o == City.BUILDING || o == City.BASE || o == City.PIER || o == City.SIDEWALK || o == City.PLAZA
                                            || o == City.RAIL || o == City.TRAIL || o == City.WATER || o == City.SAND) near = true;
                                }
                            if (!near) dead++;
                        }
                    check(dead <= 6, m + "hardly a lane or road ends in nothing (" + dead + ")");
                    // Drives known for drawing, and none winding from house to house: those are lanes now.
                    check(city.drive != null, m + "drives are known");
                    int[] dc = new int[n];
                    java.util.Arrays.fill(dc, -1);
                    int chains = 0;
                    for (int i = 0; i < n; i++) {
                        if (dc[i] >= 0 || !city.drive[i] || t[i] != City.LOT) continue;
                        int hd = 0, tl = 0;
                        q[tl++] = i;
                        dc[i] = i;
                        java.util.Set<Integer> hs = new java.util.HashSet<Integer>();
                        while (hd < tl) {
                            int cc = q[hd++], x = cc % w, y = cc / w;
                            for (int dy = -1; dy <= 1; dy++)
                                for (int dx = -1; dx <= 1; dx++) {
                                    int nx = x + dx, ny = y + dy;
                                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                                    int j = ny * w + nx, bi = city.buildingAt.get(j);
                                    // ((10.52) A house's wing is the same house.)
                                    City.Building hb = bi >= 0 ? city.buildings.get(bi) : null;
                                    if (hb != null && hb.main != null) hb = hb.main;
                                    if (hb != null && (hb.kind == City.HOUSE || hb.kind == City.BARN)) hs.add(city.buildings.indexOf(hb));
                                    if ((dx != 0 && dy != 0) || dc[j] >= 0 || !city.drive[j] || t[j] != City.LOT) continue;
                                    dc[j] = i;
                                    q[tl++] = j;
                                }
                        }
                        if (hs.size() >= 3) chains++;
                    }
                    check(chains == 0, m + "no tarmac drive winding past three homes (" + chains + ")");
                }
            }
        });
        test("10.45: calls always reach the map and follow the dead, units help each other, louder guns, shoves can fail, yards kept clear", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.civic = 25; // ((11.0) Map version 25: the same streets as when this was written.)
                c.v[CityConfig.OPT_PRESET] = 0;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 3;
                c.normalize();
                c.seed = 3;
                World w = new World(c);
                w.rnd.setSeed(45);
                w.populate(c);
                for (int f = 0; f < 30; f++) w.update(1 / 30f);
                Dispatch d = w.dispatch;
                // A full board of calls gone quiet: a new 911 call still gets on the map (a quiet one makes way).
                java.util.Random r = new java.util.Random(5);
                // ((10.51) The board holds more in a bigger city: d.maxCalls(). (10.52) From empty: a call opened in the
                // first second would leave it one over.)
                d.incidents.clear();
                Entity caller = null;
                for (Entity e : w.entities) if (e.type == Entity.CIVILIAN && !e.dead && w.city.inTown(e.x, e.y)) caller = e;
                float[] zp = w.city.findWalkable(caller.x + 50, caller.y);
                Entity z = w.spawn(Entity.ZOMBIE, zp[0], zp[1]);
                // (None of them where the new call is: one within 200 would take the call as its own.)
                while (d.incidents.size() < d.maxCalls()) {
                    float[] q = w.city.randomWalkableInTown(r);
                    if (Math.hypot(q[0] - z.x, q[1] - z.y) < 300) continue;
                    Dispatch.Incident inc = new Dispatch.Incident();
                    inc.x = q[0];
                    inc.y = q[1];
                    inc.place = "test";
                    d.incidents.add(inc);
                }
                boolean before = w.callsLost;
                w.callsLost = false;
                d.call(caller, z);
                w.callsLost = before;
                boolean onMap = false;
                for (Dispatch.Incident inc : d.incidents) if (Math.hypot(inc.x - z.x, inc.y - z.y) < 30) onMap = true;
                check(onMap && d.incidents.size() <= d.maxCalls(), "a call made with the board full still shows (" + d.incidents.size() + " open)");
                // A call with nothing at it closes in seconds; one whose dead have moved on goes with them.
                d.incidents.clear();
                w.clearZombies();
                float[] a = w.city.randomWalkableInTown(r);
                Dispatch.Incident empty = d.sighting(a[0], a[1], null);
                float[] b = null;
                for (int k = 0; k < 400 && b == null; k++) {
                    float[] q = w.city.randomWalkableInTown(r);
                    if (Math.hypot(q[0] - a[0], q[1] - a[1]) > 900) b = q;
                }
                Dispatch.Incident moving = d.sighting(b[0], b[1], null);
                float[] zq = w.city.findWalkable(b[0] + 200, b[1]);
                Entity walker = w.spawn(Entity.ZOMBIE, zq[0], zq[1]);
                walker.stun = 30;
                for (int f = 0; f < 45; f++) w.update(1 / 30f);
                // (Unless there's a call there already: then this one hands over to it and closes.)
                boolean covered = false;
                for (Dispatch.Incident o : d.incidents) if (Math.hypot(o.x - walker.x, o.y - walker.y) < 60) covered = true;
                check(covered && (Math.hypot(moving.x - walker.x, moving.y - walker.y) < 60 || moving.resolved), "the call goes with the dead ("
                        + (int) Math.hypot(moving.x - walker.x, moving.y - walker.y) + " away)");
                for (int f = 0; f < 30 * 15; f++) w.update(1 / 30f);
                check(!d.incidents.contains(empty), "a call with none of the dead near it is closed within seconds");
                // Seen from the road on the way to a call: a sighting is put on the map there.
                int open = d.incidents.size();
                float[] s2 = w.city.randomWalkableInTown(r);
                Dispatch.Incident seen = d.sighting(s2[0], s2[1], null);
                check(seen != null && d.incidents.size() == open + 1 && d.sighting(s2[0] + 30, s2[1], null) == seen,
                        "a sighting from a passing unit becomes a call (and the same spot isn't reported twice)");
                // Louder guns carry further.
                Entity p = new Entity(), sg = new Entity(), hr = new Entity(), mg = new Entity();
                p.weapon = Entity.W_PISTOL;
                sg.weapon = Entity.W_SHOTGUN;
                hr.weapon = Entity.W_HUNTING;
                mg.type = Entity.SOLDIER;
                mg.role = Entity.ROLE_GUNNER;
                check(World.gunLoudness(p) < World.gunLoudness(sg) && World.gunLoudness(sg) < World.gunLoudness(hr)
                        && World.gunLoudness(hr) < World.gunLoudness(mg), "a pistol is quieter than a shotgun, a deer rifle, a machine gun");
                // An officer hears another fighting several of the dead, and goes to help.
                w.clearZombies();
                Entity shooter = null, helper = null;
                for (Entity e : w.entities) {
                    if (e.dead || e.type != Entity.COP || e.task != Dispatch.T_NONE || e.rig != null) continue;
                    // ((10.56) Not one standing with a crowd at the station: they shot the dead down before
                    // anyone needed to hear a thing.)
                    if (shooter == null) {
                        int armed = 0;
                        for (Entity o : w.entities)
                            if (o != e && !o.dead && (o.type == Entity.COP || o.type == Entity.SOLDIER) && Math.hypot(o.x - e.x, o.y - e.y) < 160) armed++;
                        if (armed <= 1) shooter = e;
                    } else if (helper == null && Math.hypot(e.x - shooter.x, e.y - shooter.y) < 200) helper = e;
                }
                if (shooter != null && helper == null) {
                    float[] hp = w.city.findWalkable(shooter.x + 120, shooter.y);
                    helper = w.spawn(Entity.COP, hp[0], hp[1]);
                }
                check(shooter != null && helper != null, "two officers");
                helper.fear = 0;
                helper.aiming = false;
                Entity target = null;
                // (Four together: round one open spot, so none is put somewhere else by a wall in the way. (10.56) Off
                // the road: officers start out on the pavements now, and a passing car ran them over.)
                float[] q0 = w.city.findWalkable(shooter.x + 40, shooter.y + 10);
                for (int k = 0; k < 60; k++) {
                    float[] q = w.city.findWalkable(shooter.x + 40 * (float) Math.cos(k * 0.7f), shooter.y + 40 * (float) Math.sin(k * 0.7f));
                    if (q == null) continue;
                    boolean road = false;
                    for (int j = 0; j < 4; j++) if (w.city.tiles[w.city.tileIndex(q[0] + (j % 2) * 6, q[1] + (j / 2) * 6)] == City.ROAD) road = true;
                    if (!road) {
                        q0 = q;
                        break;
                    }
                }
                for (int k = 0; k < 4; k++) {
                    float[] q = w.city.findWalkable(q0[0] + (k % 2) * 6, q0[1] + (k / 2) * 6);
                    Entity zz = w.spawn(Entity.ZOMBIE, q[0], q[1]);
                    if (target == null) target = zz;
                }
                // (Once they're on the map's grid.)
                w.update(1 / 30f);
                // (Not already fighting them: it's the shot heard that's being tried.)
                helper.fear = 0;
                helper.aiming = false;
                w.heardShot(shooter, target, 260);
                check(helper.fear >= 20 && Math.hypot(helper.threatX - target.x, helper.threatY - target.y) < 30,
                        "an officer who hears another in a fight with several of them goes to help (fear " + helper.fear + ", " + w.countZombiesNear(target.x, target.y, 90) + " of the dead there)");
                // A shove isn't a sure thing.
                w.clearZombies();
                int stunned = 0, tries = 200;
                Entity cop = shooter;
                for (int k = 0; k < tries; k++) {
                    float[] q = w.city.findWalkable(cop.x + 8, cop.y);
                    Entity zz = w.spawn(Entity.ZOMBIE, q[0], q[1]);
                    zz.stun = 0;
                    w.shove(cop, zz, true);
                    if (zz.stun > 0) stunned++;
                    zz.dead = true;
                    w.clearZombies();
                    // ((16.2) Out of the list now, not at the next step: two hundred of them reached the cap.)
                    w.entities.remove(zz);
                }
                check(stunned > tries * 0.4f && stunned < tries * 0.95f, "shoves work most of the time, not every time (" + stunned + " of " + tries + ")");
                // Trucks waiting at a base for their squads wait in the yard, clear of the gate.
                City.Facility base = null;
                for (City.Facility f : w.city.facilities) if (f.kind == City.FACILITY_BASE && f.gateX > 0) base = f;
                check(base != null, "a base with a gate");
                float[] g = w.city.nearestDrivable(base.gateX, base.gateY);
                float[] bay = w.fleet.bay(g, base);
                check(bay != null && Math.hypot(bay[0] - base.gateX, bay[1] - base.gateY) >= City.T * 3.4f,
                        "a truck mustering at the base waits in the yard, off the gateway");
            }
        });
        test("10.46: infection zones round the dead - perimeter, army checkpoints on the roads in, worst first, lifted once clear", new Check() {
            public void run() {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_PRESET] = 0;
                c.seed = 5;
                c.v[CityConfig.OPT_SIZE] = 3;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.normalize();
                c.seed = 5;
                World w = new World(c);
                w.rnd.setSeed(4);
                w.populate(c);
                for (int f = 0; f < 150; f++) w.update(1 / 30f);
                java.util.Random r = new java.util.Random(7);
                float[] q0 = w.city.randomWalkableInTown(r);
                int sp = 0;
                for (int i = 0; i < 1600 && sp < 150; i++) {
                    float[] q = w.city.findWalkable(q0[0] + (i % 40) * 8 - 160, q0[1] + (i / 40) * 8 - 160);
                    if (q != null && w.spawn(Entity.ZOMBIE, q[0], q[1]) != null) sp++;
                }
                // And a few on their own, a long way off: a lesser zone, if one at all.
                float[] q1 = null;
                for (int k = 0; k < 200 && (q1 == null || Math.hypot(q1[0] - q0[0], q1[1] - q0[1]) < 2000); k++) q1 = w.city.randomWalkableInTown(r);
                for (int i = 0; i < 400 && sp < 160; i++) {
                    float[] q = w.city.findWalkable(q1[0] + (i % 20) * 8 - 80, q1[1] + (i / 20) * 8 - 80);
                    if (q != null && w.spawn(Entity.ZOMBIE, q[0], q[1]) != null) sp++;
                }
                w.alert = 2;
                w.outbreak = true;
                boolean declared = false, roads = true, invariant = true, sent = false;
                int perimeter = 0, checkpoints = 0;
                for (int f = 0; f < 30 * 60; f++) {
                    w.update(1 / 30f);
                    if (f % 30 != 0) continue;
                    for (World.InfectionZone z : w.infection) {
                        if (Math.hypot(z.x - q0[0], z.y - q0[1]) < z.r + 200) declared = true;
                        // The worst is always held; one that's waiting its turn has no checkpoints and nobody on its line.
                        if (z == w.infection.get(0) && !z.critical) invariant = false;
                        if (!z.critical && !z.checkpoints.isEmpty()) invariant = false;
                        for (float[] cp : z.checkpoints) {
                            if (!w.city.checkpoints.contains(cp)) roads = false;
                            if (w.city.tiles[(int) (cp[1] / City.T) * w.city.w + (int) (cp[0] / City.T)] != City.ROAD) roads = false;
                        }
                        checkpoints = Math.max(checkpoints, z.checkpoints.size());
                        for (int k = 0; k < z.guardSpots.size() && k < z.guards.length; k++) {
                            Entity g = z.guards[k];
                            if (g != null && g.type == Entity.SOLDIER && (!g.dead || World.riding(g))) sent = true;
                        }
                    }
                    int n = 0;
                    for (Entity e : w.entities) {
                        if (e.dead || e.task != Dispatch.T_CORDON) continue;
                        n++;
                        World.InfectionZone z = w.zoneOfPost(e.postX, e.postY);
                        if (z == null || !z.critical) invariant = false;
                    }
                    perimeter = Math.max(perimeter, n);
                }
                check(declared, "an infection zone is declared round the bunch of the dead");
                check(checkpoints >= 1 && roads, "the army closes the roads into it (" + checkpoints + " checkpoints, all on the road)");
                check(perimeter >= 4, "police on its perimeter, not called there by anyone (" + perimeter + ")");
                check(sent, "soldiers are sent out to man the checkpoints");
                check(invariant, "the worst zone is held first; the others wait, with nobody on them, till there's someone for them");
                // Not lifted while any of them are about; lifted, roads and all, once they're gone.
                boolean any = !w.infection.isEmpty();
                // (Kept clear: anyone bitten who turns meanwhile is taken care of too.)
                for (int f = 0; f < 30 * 25; f++) {
                    if (f % 15 == 0) w.clearZombies();
                    w.update(1 / 30f);
                }
                check(!any || w.infection.isEmpty(), "with the dead gone the zone is lifted (" + w.infection.size() + " left)");
                int left = 0;
                for (Entity e : w.entities) if (!e.dead && e.task == Dispatch.T_CORDON) left++;
                check(left == 0, "and everyone on its perimeter stands down (" + left + ")");
            }
        });
        test("11.5: guards at the gates replaced, kinds of building with uses, leaders who decide, new layouts", new Check() {
            public void run() {
                CityConfig c = new CityConfig();
                check(c.civic >= 28, "new cities are built the 11.5 way");
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 3;
                World w = new World(c);
                w.populate(c);
                int held = 0, k = -1;
                for (int i = 0; i < w.rosterHolders.size(); i++)
                    if (w.rosterHolders.get(i) != null) {
                        held++;
                        if (k < 0) k = i;
                    }
                check(held > 0, "the gates and checkpoints are on the roster with their guards (" + held + ")");
                Entity dead = w.rosterHolders.get(k);
                dead.dead = true;
                w.staffPosts();
                Entity next = w.rosterHolders.get(k);
                check(next != null && next != dead && !next.dead && next.task == Dispatch.T_POST,
                        "a guard killed at a gate is replaced by the nearest free soldier");
                check(Variants.use(Variants.gov(City.ARMORY)) == Variants.U_NONE, "government buildings have no special use now");
                int tools = -1, vault = -1, glass = -1;
                for (int i = 0; i < Variants.ALL_VARIANTS.size(); i++) {
                    String n = Variants.ALL_VARIANTS.get(i).name;
                    if (n.equals("Lumber yard")) tools = i;
                    if (n.equals("Self storage")) vault = i;
                    if (n.equals("Boutique")) glass = i;
                }
                check(Variants.use(tools) == Variants.U_TOOLS && Variants.use(vault) == Variants.U_VAULT && Variants.use(glass) == Variants.U_GLASS,
                        "lumber yards, storage units and boutiques each have their use");
                City.Building b = w.city.buildings.get(0);
                b.use = Variants.U_VAULT;
                float strong = World.doorWeakness(b);
                b.use = Variants.U_GLASS;
                check(strong < 0.5f && World.doorWeakness(b) > 1.2f, "a vault door holds far longer than a glass front");
                // The leaders take their decisions as the outbreak goes on.
                w.outbreak = true;
                w.alert = 2;
                for (int i = 0; i < 40; i++) w.spawn(Entity.ZOMBIE, w.city.worldW() / 2f + i * 3, w.city.worldH() / 2f);
                w.update(1 / 30f);
                for (int i = 0; i < 80 && w.decisions == 0; i++) w.leadersDecide(100);
                check(w.decisions != 0, "the city's leaders make an emergency decision (" + w.command + ")");
            }
        });
        test("11.5: the quick ways give the same answers as the long ones", new Check() {
            public void run() {
                int routes = 0, spots = 0;
                for (int preset : new int[]{0, 8, 14}) {
                    CityConfig c = new CityConfig();
                    c.v[CityConfig.OPT_PRESET] = preset;
                    c.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                    c.seed = 21 + preset;
                    c.normalize();
                    World w = new World(c);
                    w.populate(c);
                    City city = w.city;
                    java.util.Random r = new java.util.Random(5);
                    int[] f1 = new int[city.w * city.h], f2 = new int[city.w * city.h];
                    // An army truck leaves its bay and another parks: the routes know.
                    City.Facility base = city.mainBase();
                    if (base != null) city.takeParkedTruck(base.x, base.y, 600);
                    for (int k = 0; k < 25; k++) {
                        float tx = r.nextFloat() * city.worldW(), ty = r.nextFloat() * city.worldH();
                        int from = r.nextBoolean() ? -1 : r.nextInt(city.w * city.h);
                        boolean strict = r.nextBoolean();
                        City.slowSteps = false;
                        boolean a = city.driveField(f1, tx, ty, strict, from);
                        City.slowSteps = true;
                        boolean b = city.driveField(f2, tx, ty, strict, from);
                        City.slowSteps = false;
                        check(a == b && java.util.Arrays.equals(f1, f2), "the same route from the table as the long way (" + preset + ", " + k + ")");
                        routes++;
                        // ((13.6) And off road, where the table now does the steps a car could take too.)
                        City.slowSteps = false;
                        a = city.offroadField(f1, tx, ty, from);
                        City.slowSteps = true;
                        b = city.offroadField(f2, tx, ty, from);
                        City.slowSteps = false;
                        check(a == b && java.util.Arrays.equals(f1, f2), "the same off-road route from the table as the long way (" + preset + ", " + k + ")");
                    }
                    // A barricade goes up: nobody walks through it.
                    float[] spot = null;
                    for (int k = 0; k < 200 && spot == null; k++) {
                        float x = r.nextFloat() * city.worldW(), y = r.nextFloat() * city.worldH();
                        if (!city.circleBlocked(x, y, 4)) spot = w.placeBarricade(x, y);
                    }
                    if (spot != null) check(city.circleBlocked(spot[0], spot[1], 4), "a new barricade blocks the way");
                    for (int k = 0; k < 4000; k++) {
                        float x = r.nextFloat() * city.worldW(), y = r.nextFloat() * city.worldH(), rad = 2 + r.nextFloat() * 10;
                        City.slowSteps = false;
                        boolean a = city.circleBlocked(x, y, rad);
                        City.slowSteps = true;
                        boolean b = city.circleBlocked(x, y, rad);
                        City.slowSteps = false;
                        check(a == b, "the open-ground shortcut agrees with the long way");
                        spots++;
                    }
                }
                check(routes > 50 && spots > 10000, "checked enough");
            }
        });
        test("13.6: routes to a busy goal are just as short from the kept search, and a change on the map is taken into account", new Check() {
            public void run() {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_PRESET] = 8;
                c.v[CityConfig.OPT_SIZE] = CityConfig.LARGE;
                c.seed = 36;
                c.normalize();
                World w = new World(c);
                City city = w.city;
                java.util.Random r = new java.util.Random(9);
                int compared = 0, same = 0;
                for (int round = 0; round < 2; round++) {
                    if (round == 1) {
                        // A barricade goes up somewhere: the kept searches are thrown away and the routes still agree.
                        for (int k = 0; k < 200; k++) {
                            float x = r.nextFloat() * city.worldW(), y = r.nextFloat() * city.worldH();
                            if (!city.circleBlocked(x, y, 4) && w.placeBarricade(x, y) != null) break;
                        }
                    }
                    for (int goal = 0; goal < 6; goal++) {
                        float gx, gy;
                        do {
                            gx = r.nextFloat() * city.worldW();
                            gy = r.nextFloat() * city.worldH();
                        } while (city.circleBlocked(gx, gy, 4));
                        for (int k = 0; k < 12; k++) {
                            float sx, sy;
                            do {
                                sx = r.nextFloat() * city.worldW();
                                sy = r.nextFloat() * city.worldH();
                            } while (city.circleBlocked(sx, sy, 4) || Math.max(Math.abs(sx - gx), Math.abs(sy - gy)) < 40 * City.T);
                            int[] kept = city.findPath(sx, sy, gx, gy, 9000);
                            Nav.plainSearch = true;
                            int[] plain = city.findPath(sx, sy, gx, gy, 9000);
                            Nav.plainSearch = false;
                            check((kept == null) == (plain == null), "a way there either way, or neither");
                            if (kept == null || plain == null) continue;
                            float lk = routeLength(city, sx, sy, kept), lp = routeLength(city, sx, sy, plain);
                            // ((16.0) Within 8%, not 3: the kept search is the exact cheapest by the routes' own costs - a step
                            // beside a wall or over rough ground dearer - where the other's estimate can cut closer along a
                            // wall. Out of the fenced airport grounds (a single gate) that came to 6% further on foot.)
                            check(lk <= lp * 1.08f + 2 * City.T, "as short from the kept search (" + lk + " vs " + lp + ")");
                            check(kept[kept.length - 1] == plain[plain.length - 1], "to the same place");
                            compared++;
                            if (java.util.Arrays.equals(kept, plain)) same++;
                        }
                    }
                }
                check(compared > 100 && same * 2 > compared, "compared enough (" + compared + ", " + same + " the very same)");
            }
        });
        test("13.8: routine talk stays off the radio, the same news isn't repeated, and the hot spots list the worst fights first", new Check() {
            public void run() {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 12;
                c.normalize();
                c.seed = 12;
                World w = new World(c);
                w.populate(c);
                Dispatch d = w.dispatch;
                Entity cop = null;
                for (Entity e : w.entities) if (!e.dead && e.type == Entity.COP) cop = e;
                int before = d.messageCount;
                d.chatter(Dispatch.WHO_POLICE, cop, "10-4, en route.", cop.x, cop.y);
                check(d.messageCount == before && cop.talkTimer > 0, "routine talk: seen to talk, not on the radio");
                d.say(Dispatch.WHO_INFO, null, "Info: Stampede on Main St! People are being trampled.", 0, 0);
                d.say(Dispatch.WHO_INFO, null, "Info: Stampede on Oak Ave! People are being trampled.", 0, 0);
                check(d.messageCount == before + 1, "the same news twice in a row goes out once");
                for (int f = 0; f < 30 * 16; f++) w.update(1 / 30f);
                int at = d.messageCount;
                d.say(Dispatch.WHO_INFO, null, "Info: Stampede on Elm Rd! People are being trampled.", 0, 0);
                check(d.messageCount == at + 1, "and again once a while has passed");
                // Two fights: the bigger one first, with its numbers.
                w.clearZombies();
                d.incidents.clear();
                java.util.Random r = new java.util.Random(2);
                float[][] spots = new float[2][];
                for (int k = 0; k < 2; k++) {
                    float[] q;
                    do q = w.city.randomWalkableInTown(r);
                    while (k == 1 && Math.hypot(q[0] - spots[0][0], q[1] - spots[0][1]) < 800);
                    spots[k] = q;
                    for (int z = 0; z < (k == 0 ? 3 : 7); z++) {
                        float[] p = w.city.findWalkable(q[0] + (z % 3) * 8, q[1] + (z / 3) * 8);
                        w.spawn(Entity.ZOMBIE, p[0], p[1]);
                    }
                    Dispatch.Incident inc = new Dispatch.Incident();
                    inc.x = q[0];
                    inc.y = q[1];
                    inc.place = "spot " + k;
                    d.incidents.add(inc);
                }
                w.update(1 / 30f);
                java.util.List<World.HotSpot> hot = w.hotSpots(8);
                check(hot.size() == 2, "both fights listed (" + hot.size() + ")");
                if (hot.size() == 2) {
                    check(hot.get(0).place.equals("spot 1") && hot.get(0).dead >= 6 && hot.get(1).dead >= 2,
                            "the worst first (" + hot.get(0).place + " " + hot.get(0).dead + ", " + hot.get(1).place + " " + hot.get(1).dead + ")");
                }
                w.clearZombies();
                w.update(1 / 30f);
                check(w.hotSpots(8).isEmpty(), "nothing listed with no fighting");
            }
        });
        test("13.9: squads have a combat medic again, who patches up the wounded but not the bitten", new Check() {
            public void run() {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 21;
                c.normalize();
                c.seed = 21;
                World w = new World(c);
                w.populate(c);
                int medics = 0, soldiers = 0;
                for (Entity e : w.entities)
                    if (!e.dead && e.type == Entity.SOLDIER) {
                        soldiers++;
                        if (e.role == Entity.ROLE_CORPSMAN) medics++;
                    }
                check(soldiers < 8 || medics * 8 >= soldiers, "about one soldier in four is a medic (" + medics + " of " + soldiers + ")");
                java.util.Random r = new java.util.Random(4);
                float[] q = w.city.randomWalkableInTown(r);
                Entity medic = w.spawnSoldier(Entity.ROLE_CORPSMAN, q[0], q[1]);
                float[] a = w.city.findWalkable(q[0] + 60, q[1]), b = w.city.findWalkable(q[0] - 60, q[1]);
                Entity hurt = w.spawnSoldier(Entity.ROLE_RIFLE, a[0], a[1]);
                Entity bitten = w.spawnSoldier(Entity.ROLE_RIFLE, b[0], b[1]);
                hurt.hp = hurt.maxHp * 0.3f;
                bitten.hp = bitten.maxHp * 0.2f;
                bitten.infected = true;
                bitten.infectTimer = 999;
                for (Entity e : new Entity[]{medic, hurt, bitten}) {
                    e.task = Dispatch.T_HOLD;
                    e.postX = e.x;
                    e.postY = e.y;
                }
                float before = hurt.hp;
                for (int f = 0; f < 30 * 20; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(hurt.hp >= before + hurt.maxHp * 0.4f, "the wounded soldier patched up (" + (int) before + " to " + (int) hurt.hp + ")");
                check(bitten.hp <= bitten.maxHp * 0.25f, "the bitten one left alone (" + (int) bitten.hp + ")");
            }
        });
        test("13.9: soldiers low on rounds at a fight far from the base get an ammo truck, and fill up at it", new Check() {
            public void run() {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_PRESET] = 0;
                c.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 77;
                c.normalize();
                c.seed = 77;
                World w = new World(c);
                w.populate(c);
                City.Facility base = w.city.mainBase();
                check(base != null, "a base");
                // A call a long drive from the base, with three soldiers on it nearly out of rounds.
                java.util.Random r = new java.util.Random(6);
                float[] at = null;
                for (int k = 0; k < 4000 && at == null; k++) {
                    float[] q = w.city.randomWalkableInTown(r);
                    float[] road = w.city.nearestDrivable(q[0], q[1]);
                    if (road != null && Math.hypot(q[0] - base.x, q[1] - base.y) > 1200 && w.fleet.canDrive(base.x, base.y, road[0], road[1])) at = q;
                }
                check(at != null, "a call a long way off");
                Dispatch.Incident inc = new Dispatch.Incident();
                inc.x = at[0];
                inc.y = at[1];
                inc.place = "the test";
                inc.zombiesNear = 3;
                w.dispatch.incidents.add(inc);
                java.util.List<Entity> squad = new java.util.ArrayList<Entity>();
                for (int k = 0; k < 3; k++) {
                    float[] p = w.city.findWalkable(at[0] + k * 8, at[1]);
                    Entity e = w.spawnSoldier(Entity.ROLE_RIFLE, p[0], p[1]);
                    e.task = Dispatch.T_RESPOND;
                    e.incident = inc;
                    e.onScene = true;
                    e.ammo = 0;
                    e.reserve = 0;
                    squad.add(e);
                }
                inc.soldiers = 3;
                // (A few of the dead far off, so the outbreak isn't over.)
                float[] farSpot = null;
                for (int k = 0; k < 4000 && farSpot == null; k++) {
                    float[] q = w.city.randomWalkable(r);
                    if (Math.hypot(q[0] - at[0], q[1] - at[1]) > 1500 && Math.hypot(q[0] - base.x, q[1] - base.y) > 1500) farSpot = q;
                }
                check(farSpot != null, "somewhere far off for a few of the dead");
                int baseAmmo = base.ammo;
                Fleet.Vehicle truck = null;
                boolean filled = false;
                for (int f = 0; f < 30 * 240 && !filled; f++) {
                    inc.resolved = false;
                    inc.age = 0;
                    // (A call with none of the dead at it is closed: this one stays open, as a real fight would.)
                    if (!w.dispatch.incidents.contains(inc)) w.dispatch.incidents.add(inc);
                    // (Kept going: police shoot the dead, and the outbreak would be over.)
                    if (f % 30 == 0 && w.zombies < 2) w.spawn(Entity.ZOMBIE, farSpot[0], farSpot[1]);
                    w.update(1 / 30f);
                    w.evCount = 0;
                    for (Fleet.Vehicle v : w.fleet.vehicles) if (v.ammoTruck) truck = v;
                    int full = 0;
                    for (Entity e : squad) if (!e.dead && e.ammo + e.reserve > 60) full++;
                    filled = full >= 2;
                }
                check(truck != null, "an ammo truck sent out from the base");
                check(truck == null || base.ammo < baseAmmo, "loaded from the base's armoury");
                int nearTruck = 0;
                for (Entity e : squad) if (truck != null && !e.dead && Math.hypot(e.x - truck.x, e.y - truck.y) < 60) nearTruck++;
                check(filled && nearTruck >= 1, "the soldiers filled up at the truck, not the base (" + filled + ", " + nearTruck + " by it)");
            }
        });
        test("14.0: the engineers' bulldozer pushes wrecks off the roads, and engineers get a safe zone's wall up faster", new Check() {
            public void run() {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_PRESET] = 0;
                c.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 77;
                c.normalize();
                c.seed = 77;
                World w = new World(c);
                w.populate(c);
                Fleet.Vehicle dozer = null;
                for (Fleet.Vehicle v : w.fleet.vehicles) if (v.kind == Fleet.K_DOZER) dozer = v;
                check(dozer != null && dozer.parked, "the base has its bulldozer, parked");
                int engineers = 0;
                for (Entity e : w.entities) if (!e.dead && e.role == Entity.ROLE_ENGINEER) engineers++;
                check(engineers == 2, "and two combat engineers (" + engineers + ")");
                // Two wrecks on roads in town.
                java.util.Random r = new java.util.Random(8);
                java.util.List<Fleet.Vehicle> wrecks = new java.util.ArrayList<Fleet.Vehicle>();
                for (int k = 0; k < 400 && wrecks.size() < 2; k++) {
                    float[] q = w.city.randomWalkableInTown(r);
                    Fleet.Vehicle car = w.fleet.placeCar(q[0], q[1]);
                    if (car == null) continue;
                    int t = w.city.tileIndex(car.x, car.y);
                    if (t < 0 || w.city.tiles[t] != City.ROAD || !w.city.inTown(car.x, car.y) || Math.hypot(car.x - dozer.x, car.y - dozer.y) > 2000) {
                        w.fleet.discard(car);
                        continue;
                    }
                    car.broken = true;
                    car.parked = true;
                    car.speed = 0;
                    wrecks.add(car);
                }
                check(wrecks.size() == 2, "two wrecks on the road");
                // Nothing happens on a calm day; once the dead are about, the bulldozer goes out.
                for (int f = 0; f < 30 * 10; f++) w.update(1 / 30f);
                check(dozer.parked && !wrecks.get(0).removedFromFleet, "left be on a calm day");
                boolean cleared = false;
                for (int f = 0; f < 30 * 240 && !cleared; f++) {
                    w.alert = Math.max(w.alert, 1);
                    w.update(1 / 30f);
                    w.evCount = 0;
                    cleared = wrecks.get(0).removedFromFleet && wrecks.get(1).removedFromFleet;
                }
                check(cleared, "both wrecks pushed off the road (" + w.fleet.dozerCleared + ")");
                check(w.fleet.vehicles.contains(dozer) && !dozer.broken, "the bulldozer is still in service");
                // A safe zone going up: a free engineer goes to help. (One placed there: on a calm day the base's own may be
                // indoors.)
                w.alert = 2;
                for (float[] a : w.city.openAreas) {
                    if (!w.dispatch.zones.isEmpty()) break;
                    if (!w.city.inTown(a[0], a[1])) continue;
                    float[] p = w.city.findWalkable(a[0] + 200, a[1]);
                    Entity eng = p == null ? null : w.spawnSoldier(Entity.ROLE_ENGINEER, p[0], p[1]);
                    w.dispatch.orderZone(a[0], a[1]);
                    if (w.dispatch.zones.isEmpty() && eng != null) eng.dead = eng.removed = true;
                }
                check(!w.dispatch.zones.isEmpty(), "a safe zone set up");
                int helping = 0;
                for (Entity e : w.entities)
                    if (!e.dead && e.role == Entity.ROLE_ENGINEER && e.task == Dispatch.T_GUARD && !w.dispatch.zones.isEmpty() && e.zone == w.dispatch.zones.get(0)) helping++;
                check(helping >= 1, "engineers sent to help get its wall up (" + helping + ")");
            }
        });
        test("14.0: a mortar team shells a pack of the dead a few streets off, but not with police beside them", new Check() {
            public void run() {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 31;
                c.normalize();
                c.seed = 31;
                World w = new World(c);
                w.populate(c);
                int mortars = 0;
                for (Entity e : w.entities) if (!e.dead && e.role == Entity.ROLE_MORTAR) mortars++;
                check(w.city.mainBase() == null || mortars >= 1, "the base has a mortar team (" + mortars + ")");
                java.util.Random r = new java.util.Random(3);
                for (int round = 0; round < 2; round++) {
                    w.clearZombies();
                    w.dispatch.incidents.clear();
                    // A mortar team, and 400 off a pack of ten of the dead at a call (with police beside them, the second time).
                    float[] m = null, pack = null;
                    for (int k = 0; k < 4000 && pack == null; k++) {
                        m = w.city.randomWalkableInTown(r);
                        float a = r.nextFloat() * 6.283f;
                        float[] q = w.city.findWalkable(m[0] + (float) Math.cos(a) * 400, m[1] + (float) Math.sin(a) * 400);
                        if (q != null && Math.abs(Math.hypot(q[0] - m[0], q[1] - m[1]) - 400) < 40 && w.countZombiesNear(q[0], q[1], 300) == 0) pack = q;
                    }
                    Entity mortar = w.spawnSoldier(Entity.ROLE_MORTAR, m[0], m[1]);
                    mortar.task = Dispatch.T_HOLD;
                    mortar.postX = m[0];
                    mortar.postY = m[1];
                    java.util.List<Entity> dead = new java.util.ArrayList<Entity>();
                    for (int z = 0; z < 10; z++) {
                        float[] p = w.city.findWalkable(pack[0] + (z % 4) * 6, pack[1] + (z / 4) * 6);
                        Entity zz = w.spawn(Entity.ZOMBIE, p[0], p[1]);
                        zz.speed = zz.runSpeed = 0;
                        dead.add(zz);
                    }
                    java.util.List<Entity> cops = new java.util.ArrayList<Entity>();
                    if (round == 1)
                        for (int k = 0; k < 2; k++) {
                            float[] p = w.city.findWalkable(pack[0] + 30, pack[1] + k * 10);
                            Entity cop = w.spawn(Entity.COP, p[0], p[1]);
                            cop.ammo = cop.reserve = 0;
                            cop.task = Dispatch.T_HOLD;
                            cop.postX = p[0];
                            cop.postY = p[1];
                            cops.add(cop);
                        }
                    // (Everyone on the map's grid of who's where before the call comes in.)
                    for (int f = 0; f < 5; f++) {
                        for (Entity zz : dead) zz.speed = zz.runSpeed = 0;
                        w.update(1 / 30f);
                    }
                    Dispatch.Incident inc = new Dispatch.Incident();
                    inc.x = pack[0];
                    inc.y = pack[1];
                    inc.place = "the test";
                    inc.zombiesNear = 10;
                    w.dispatch.incidents.add(inc);
                    int rounds = mortar.grenades;
                    for (int f = 0; f < 30 * 20; f++) {
                        inc.resolved = false;
                        if (!w.dispatch.incidents.contains(inc)) w.dispatch.incidents.add(inc);
                        for (Entity zz : dead) zz.speed = zz.runSpeed = 0;
                        // (In the first round nobody else armed wanders up to the pack: a passing patrol would - rightly -
                        // stop the mortar firing.)
                        if (round == 0)
                            for (Entity o : w.entities)
                                if (!o.dead && o.isArmed() && o != mortar && Math.hypot(o.x - pack[0], o.y - pack[1]) < 140) o.dead = o.removed = true;
                        // (The officers stay put beside them: out of rounds, they'd otherwise walk off.)
                        for (Entity cop : cops) {
                            cop.x = cop.postX;
                            cop.y = cop.postY;
                            cop.hp = cop.maxHp;
                        }
                        w.update(1 / 30f);
                        w.evCount = 0;
                    }
                    int down = 0;
                    for (Entity zz : dead) if (zz.dead || zz.hp <= 0) down++;
                    if (round == 0) check(mortar.grenades < rounds && down >= 3, "the pack shelled (" + (rounds - mortar.grenades) + " shells, " + down + " down)");
                    else check(mortar.grenades == rounds, "no shells with police beside the pack (" + (rounds - mortar.grenades) + ")");
                    mortar.dead = mortar.removed = true;
                    for (Entity cop : cops) cop.dead = cop.removed = true;
                }
            }
        });
        test("14.2: the city quarantined - nobody leaves - and air strikes again while it gets worse", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.exodus = CityConfig.EXODUS_ALL;
                c.seed = 41;
                c.normalize();
                c.seed = 41;
                // ((16.5) Map version 29, the streets this pack's spot was chosen on.)
                c.civic = 29;
                World w = new World(c);
                w.populate(c);
                // A pack of the dead, an outbreak under way, and people leaving town.
                java.util.Random r = new java.util.Random(2);
                float[] q = w.city.randomWalkableInTown(r);
                for (int z = 0; z < 20; z++) {
                    float[] p = w.city.findWalkable(q[0] + (z % 5) * 6, q[1] + (z / 5) * 6);
                    w.spawn(Entity.ZOMBIE, p[0], p[1]);
                }
                w.alert = 2;
                for (int f = 0; f < 30 * 4; f++) w.update(1 / 30f);
                int leaving = 0;
                for (Entity e : w.entities)
                    if (!e.dead && e.type == Entity.CIVILIAN && e.task == Dispatch.T_NONE && leaving < 20) {
                        e.task = Dispatch.T_LEAVE;
                        leaving++;
                    }
                java.lang.reflect.Method qc = World.class.getDeclaredMethod("quarantineCity");
                qc.setAccessible(true);
                qc.invoke(w);
                check(w.quarantined && !w.exodusOn(), "quarantined: nobody sets off");
                int still = 0;
                for (Entity e : w.entities) if (!e.dead && e.task == Dispatch.T_LEAVE) still++;
                check(still == 0, "those on their way out turned back (" + still + " still leaving)");
                for (int f = 0; f < 30 * 5; f++) w.update(1 / 30f);
                still = 0;
                for (Entity e : w.entities) if (!e.dead && e.task == Dispatch.T_LEAVE) still++;
                check(still == 0, "and nobody else sets off (" + still + ")");
                // Air strikes: a first, then another.
                java.lang.reflect.Method as = World.class.getDeclaredMethod("callAirStrike", int.class, boolean.class);
                as.setAccessible(true);
                // (The pack still there: the police may have thinned it out by now.)
                for (int z = 0; z < 14; z++) {
                    float[] p = w.city.findWalkable(q[0] + (z % 5) * 6, q[1] + (z / 5) * 6);
                    w.spawn(Entity.ZOMBIE, p[0], p[1]);
                }
                w.recount();
                check((Boolean) as.invoke(w, 8, true) && w.firebombTime > 0, "an air strike called on the pack");
                w.firebombTime = 0;
                check((Boolean) as.invoke(w, 8, false) && w.firebombTime > 0, "and another");
                check(!(Boolean) as.invoke(w, 500, false), "not on a crowd too small");
                // Kept in a save.
                w.strikes = 2;
                w.strikeCd = 33;
                java.io.File f = java.io.File.createTempFile("zcsq", ".dat");
                SaveGame.save(w, f);
                World l = SaveGame.load(f);
                f.delete();
                check(l.quarantined && l.strikes == 2 && Math.abs(l.strikeCd - 33) < 0.01f, "the quarantine and strikes saved");
            }
        });
        test("14.2: raider gangs strip the shops and shoot survivors there, and the police shoot back", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 52;
                c.normalize();
                c.seed = 52;
                World w = new World(c);
                w.populate(c);
                java.lang.reflect.Method arm = World.class.getDeclaredMethod("armCivilian", Entity.class, int.class);
                arm.setAccessible(true);
                // A long outbreak, the dead far away: gangs form among armed survivors.
                java.util.Random r = new java.util.Random(5);
                float[] far = w.city.randomWalkable(r);
                for (int z = 0; z < 3; z++) w.spawn(Entity.ZOMBIE, far[0], far[1]);
                for (Entity e : w.entities) if (!e.dead && e.type == Entity.CIVILIAN && r.nextFloat() < 0.3f) arm.invoke(w, e, 30);
                for (int f = 0; f < 30 * 3; f++) w.update(1 / 30f);
                check(w.outbreak, "an outbreak on");
                w.outbreakTime = 400;
                java.lang.reflect.Method rd = World.class.getDeclaredMethod("raiders");
                rd.setAccessible(true);
                for (int k = 0; k < 3000 && w.gangsFormed == 0; k++) rd.invoke(w);
                int gang = 0;
                for (Entity e : w.entities) if (!e.dead && e.raider) gang++;
                check(w.gangsFormed == 1 && gang >= 3, "a gang formed (" + gang + ")");
                // A gang of three near a supermarket: they strip it. Then another, with an armed survivor at the shop and a
                // police officer on the corner: they shoot, and the officer shoots back.
                City.Building shop = null;
                for (City.Building b : w.city.buildings)
                    if (shop == null && !b.collapsed && b.name != null && b.kind == City.MARKET && Math.hypot(b.doorX - far[0], b.doorY - far[1]) > 600) shop = b;
                check(shop != null, "a supermarket");
                for (Entity e : w.entities) e.raider = false;
                Entity boss = null;
                boolean stripped = false, shotAt = false, shotBack = false;
                for (int round = 0; round < 2; round++) {
                    shop.stock = 200;
                    float[] p0 = w.city.findWalkable(shop.doorX, shop.doorY + 60);
                    boss = w.spawn(Entity.CIVILIAN, p0[0], p0[1]);
                    arm.invoke(w, boss, 30);
                    boss.raider = true;
                    for (int k = 0; k < 2; k++) {
                        float[] p = w.city.findWalkable(p0[0] + 8 * (k + 1), p0[1]);
                        Entity o = w.spawn(Entity.CIVILIAN, p[0], p[1]);
                        arm.invoke(w, o, 30);
                        o.raider = true;
                        o.leader = boss;
                    }
                    Entity victim = null, cop = null;
                    float[] pv = null;
                    if (round == 1) {
                        pv = w.city.findWalkable(shop.doorX + 40, shop.doorY);
                        victim = w.spawn(Entity.CIVILIAN, pv[0], pv[1]);
                        arm.invoke(w, victim, 30);
                        victim.hp = victim.maxHp = 400;
                        float[] pc = w.city.findWalkable(shop.doorX - 70, shop.doorY + 40);
                        cop = w.spawn(Entity.COP, pc[0], pc[1]);
                        cop.task = Dispatch.T_HOLD;
                        cop.postX = pc[0];
                        cop.postY = pc[1];
                    }
                    w.alert = 2;
                    float bossHp = boss.hp;
                    for (int f = 0; f < 30 * 40; f++) {
                        if (victim != null) {
                            victim.x = pv[0];
                            victim.y = pv[1];
                            victim.task = Dispatch.T_NONE;
                        }
                        // (The dead kept going far off: with none left the outbreak's over and the gang breaks up.)
                        if (f % 30 == 0 && w.zombies < 2) w.spawn(Entity.ZOMBIE, far[0], far[1]);
                        w.update(1 / 30f);
                        w.evCount = 0;
                        if (round == 0 && shop.stock == 0) stripped = true;
                        if (victim != null && victim.hp < 400) shotAt = true;
                        if (round == 1 && (boss.hp < bossHp || boss.dead)) shotBack = true;
                    }
                }
                check(stripped, "the shop stripped bare");
                check(shotAt, "the armed survivor there shot at");
                check(shotBack, "the police officer shot back at them");
                // Kept in a save.
                Entity alive = boss.dead ? null : boss;
                for (Entity e : w.entities) if (alive == null && !e.dead && e.raider) alive = e;
                if (alive != null) {
                    java.io.File f = java.io.File.createTempFile("zcsr", ".dat");
                    SaveGame.save(w, f);
                    World l = SaveGame.load(f);
                    f.delete();
                    int kept = 0;
                    for (Entity e : l.entities) if (e.raider) kept++;
                    check(kept >= 1, "raiders saved (" + kept + ")");
                }
            }
        });
        test("14.2: families hide a bitten relative's bite at the safe zone gate", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_SIZE] = CityConfig.TINY;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 9;
                c.normalize();
                c.seed = 9;
                World w = new World(c);
                w.populate(c);
                w.setMaxPopulation(100000);
                java.lang.reflect.Method cp = World.class.getDeclaredMethod("checkpoint", Entity.class, Dispatch.SafeZone.class);
                cp.setAccessible(true);
                Dispatch.SafeZone z = new Dispatch.SafeZone();
                z.place = "the test";
                float[] p = w.city.randomWalkable(new java.util.Random(1));
                Entity parent = w.spawn(Entity.CIVILIAN, p[0], p[1]);
                int[] through = new int[2];
                for (int k = 0; k < 800; k++) {
                    boolean family = k % 2 == 1;
                    Entity e = w.spawn(Entity.CIVILIAN, p[0], p[1]);
                    e.infected = true;
                    e.infectTimer = 999;
                    if (family) e.leader = parent;
                    boolean turnedAway = (Boolean) cp.invoke(w, e, z);
                    if (!turnedAway) through[family ? 1 : 0]++;
                    check(!e.concealed || family, "only a family hides a bite");
                    e.dead = e.removed = true;
                }
                // Loners get through when the bite's missed (40%); a family hides it 45% of the rest (about 67% through).
                check(through[0] > 120 && through[0] < 200, "loners through only when missed (" + through[0] + " of 400)");
                check(through[1] > 230 && through[1] > through[0] + 50, "families get more of their bitten through (" + through[1] + " of 400)");
                check(w.bitesHidden > 50, "bites hidden counted (" + w.bitesHidden + ")");
            }
        });
        test("14.3: units call for backup when outnumbered, out of rounds too; drones mark the dead and send people to safety", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 73;
                c.normalize();
                c.seed = 73;
                // ((16.9) Map version 30, the streets this was written on: in newer cities the FBI field office's agents
                // are near enough to join in, and the officer here isn't alone.)
                c.civic = 30;
                World w = new World(c);
                w.populate(c);
                java.util.Random r = new java.util.Random(7);
                for (int round = 0; round < 2; round++) {
                    // A lone officer and four of the dead on them (out of rounds, the second time).
                    w.clearZombies();
                    w.dispatch.incidents.clear();
                    float[] q = null;
                    for (int k = 0; k < 2000 && q == null; k++) {
                        float[] p = w.city.randomWalkableInTown(r);
                        if (w.armedNear(p[0], p[1], 400) == 0) q = p;
                    }
                    check(q != null, "somewhere with no police about");
                    Entity cop = w.spawn(Entity.COP, q[0], q[1]);
                    // ((16.9) An ordinary patrol officer: SWAT or an FBI agent may now put down four before it's time to call.)
                    cop.role = Entity.ROLE_RIFLE;
                    cop.agency = 0;
                    cop.hp = cop.maxHp = 10000;
                    if (round == 1) cop.ammo = cop.reserve = 0;
                    for (int z = 0; z < 4; z++) {
                        float[] p = w.city.findWalkable(q[0] + 30 + z * 6, q[1] + 10);
                        w.spawn(Entity.ZOMBIE, p[0], p[1]);
                    }
                    boolean called = false;
                    for (int f = 0; f < 30 * 6 && !called; f++) {
                        w.update(1 / 30f);
                        w.evCount = 0;
                        for (Dispatch.Incident inc : w.dispatch.incidents)
                            if (inc.backupNeed > 0 && inc.backupBy != null && Math.hypot(inc.x - cop.x, inc.y - cop.y) < 250) called = true;
                    }
                    check(called, (round == 0 ? "a lone officer against four" : "an officer out of rounds") + " calls for backup");
                    cop.dead = cop.removed = true;
                }
                // A drone: more of them now, and over a pack of the dead it marks them.
                w.clearZombies();
                w.alert = 2;
                float[] pack = w.city.randomWalkableInTown(r);
                for (int z = 0; z < 6; z++) w.spawn(Entity.ZOMBIE, pack[0] + z * 5, pack[1]);
                w.update(1 / 30f);
                check(!w.drones.isEmpty(), "drones up (" + w.drones.size() + ")");
                World.Drone d = w.drones.get(0);
                d.state = World.DRONE_OUT;
                d.x = d.tx = pack[0];
                d.y = d.ty = pack[1];
                d.spotCd = 0;
                d.sweepCd = 99;
                w.update(1 / 30f);
                int marked = 0;
                for (Entity e : w.entities) if (!e.dead && e.isZombie() && e.spottedUntil > w.time) marked++;
                check(marked >= 5, "the dead below it marked (" + marked + ")");
                // And with a safe zone open, its loudspeaker sends the people below there.
                w.clearZombies();
                for (float[] a : w.city.openAreas) {
                    if (!w.dispatch.zones.isEmpty()) break;
                    if (w.city.inTown(a[0], a[1])) w.dispatch.orderZone(a[0], a[1]);
                }
                if (!w.dispatch.zones.isEmpty()) {
                    Dispatch.SafeZone z = w.dispatch.zones.get(0);
                    z.open = true;
                    z.built = 1;
                    java.util.List<Entity> below = new java.util.ArrayList<Entity>();
                    for (int k = 0; k < 5; k++) {
                        float[] p = w.city.findWalkable(d.x + k * 8, d.y + 20);
                        Entity e = w.spawn(Entity.CIVILIAN, p[0], p[1]);
                        e.task = Dispatch.T_NONE;
                        below.add(e);
                    }
                    java.lang.reflect.Method sp = World.class.getDeclaredMethod("droneSpeaker", World.Drone.class);
                    sp.setAccessible(true);
                    w.update(1 / 30f);
                    for (Entity e : below) {
                        e.task = Dispatch.T_NONE;
                        e.leader = null;
                    }
                    sp.invoke(w, d);
                    int seeking = 0;
                    for (Entity e : below) if (e.task == Dispatch.T_SEEK && e.heardZone) seeking++;
                    check(seeking >= 3, "people below told to make for the safe zone (" + seeking + ")");
                }
            }
        });
        test("14.5: the dead rot, big hordes march across the city, and the wounded go to a field hospital", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_PRESET] = 0;
                c.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 77;
                c.normalize();
                c.seed = 77;
                World w = new World(c);
                w.populate(c);
                java.util.Random r = new java.util.Random(2);
                // Rot: nothing for the first minutes, then slower and frailer.
                float[] q = w.city.randomWalkableInTown(r);
                Entity fresh = w.spawn(Entity.ZOMBIE, q[0], q[1]);
                Entity old = w.spawn(Entity.ZOMBIE, q[0] + 20, q[1]);
                w.time = 5000;
                fresh.risen = 5000 - 120;
                old.risen = 5000 - 2400;
                check(w.rot(fresh) == 0 && Math.abs(w.rot(old) - World.ROT_MAX) < 0.001f, "rot: none at first, then up to the most (" + w.rot(old) + ")");
                old.want = fresh.want = 20;
                old.mx = fresh.mx = 1;
                java.lang.reflect.Method mv = World.class.getDeclaredMethod("move", Entity.class, float.class);
                mv.setAccessible(true);
                float ox = old.x, fx = fresh.x;
                for (int k = 0; k < 30; k++) {
                    old.want = fresh.want = 20;
                    old.mx = fresh.mx = 1;
                    old.my = fresh.my = 0;
                    mv.invoke(w, old, 1 / 30f);
                    mv.invoke(w, fresh, 1 / 30f);
                }
                check(old.x - ox < (fresh.x - fx) * 0.8f, "the rotted one slower (" + (int) (old.x - ox) + " vs " + (int) (fresh.x - fx) + ")");
                w.clearZombies();
                // A big horde marches across the city for a crowd of people far off.
                float[] h = w.city.randomWalkableInTown(r);
                float[] crowd = null;
                for (int k = 0; k < 4000 && crowd == null; k++) {
                    float[] p = w.city.randomWalkableInTown(r);
                    if (Math.hypot(p[0] - h[0], p[1] - h[1]) > 2200) crowd = p;
                }
                check(crowd != null, "somewhere far off for a crowd");
                for (int k = 0; k < 25; k++) {
                    float[] p = w.city.findWalkable(crowd[0] + (k % 5) * 8, crowd[1] + (k / 5) * 8);
                    w.spawn(Entity.CIVILIAN, p[0], p[1]);
                }
                Entity lead = w.spawn(Entity.ZOMBIE, h[0], h[1]);
                lead.leadsHorde = true;
                lead.hordeSize = 20;
                w.update(1 / 30f);
                java.lang.reflect.Method pr = World.class.getDeclaredMethod("pickRoam", Entity.class);
                pr.setAccessible(true);
                boolean marched = false;
                for (int k = 0; k < 60 && !marched; k++) {
                    lead.leadsHorde = true;
                    lead.hordeSize = 20;
                    pr.invoke(w, lead);
                    // (For the biggest crowd it finds: in a town that's often somewhere else busy, but always a long way.)
                    marched = lead.migrating && Math.hypot(lead.roamX - lead.x, lead.roamY - lead.y) > 1400;
                }
                check(marched, "the horde sets off on a march across the city");
                // The field hospital: set up once the army's in it; a badly hurt soldier goes there and is patched up.
                w.clearZombies();
                float[] far = w.city.randomWalkable(r);
                w.alert = 2;
                Field ob = World.class.getDeclaredField("outbreak");
                ob.setAccessible(true);
                java.lang.reflect.Method fh = World.class.getDeclaredMethod("fieldHospital");
                fh.setAccessible(true);
                w.spawn(Entity.ZOMBIE, far[0], far[1]);
                w.update(1 / 30f);
                ob.setBoolean(w, true);
                fh.invoke(w);
                check(!Float.isNaN(w.hospitalX), "a field hospital set up");
                float[] sp = w.city.findWalkable(w.hospitalX + 300, w.hospitalY);
                Entity hurt = w.spawnSoldier(Entity.ROLE_RIFLE, sp[0], sp[1]);
                hurt.hp = hurt.maxHp * 0.2f;
                // (No medics about: it's the hospital being tried.)
                for (Entity e : w.entities) if (e.role == Entity.ROLE_CORPSMAN) e.role = Entity.ROLE_RIFLE;
                boolean healed = false;
                for (int f = 0; f < 30 * 60 && !healed; f++) {
                    if (w.zombies < 1) w.spawn(Entity.ZOMBIE, far[0], far[1]);
                    w.update(1 / 30f);
                    w.evCount = 0;
                    healed = hurt.hp >= hurt.maxHp;
                }
                check(healed && w.treated >= 1, "the wounded soldier patched up at the field hospital");
                // Saved.
                java.io.File f = java.io.File.createTempFile("zcsh", ".dat");
                SaveGame.save(w, f);
                World l = SaveGame.load(f);
                f.delete();
                check(Math.abs(l.hospitalX - w.hospitalX) < 0.1f, "the field hospital saved");
            }
        });
        test("14.6: the army sends an IFV to the worst of it and a gun truck to a horde on the road, and both cut them down", new Check() {
            public void run() {
                for (int round = 0; round < 2; round++) {
                    CityConfig c = new CityConfig();
                    c.v[CityConfig.OPT_PRESET] = 0;
                    c.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                    c.v[CityConfig.OPT_ZOMBIES] = 0;
                    c.seed = 77;
                    c.normalize();
                    c.seed = 77;
                    World w = new World(c);
                    w.populate(c);
                    City.Facility base = w.city.mainBase();
                    check(base != null, "a base");
                    java.util.List<Entity> squad = new java.util.ArrayList<Entity>();
                    for (Entity e : w.entities)
                        if (squad.size() < 4 && !e.dead && e.type == Entity.SOLDIER && e.rig == null && Math.hypot(e.x - base.x, e.y - base.y) < base.r + 200) squad.add(e);
                    // A call far off: the worst of it (an IFV), or a pack on an open road (a gun truck).
                    java.util.Random r = new java.util.Random(3 + round);
                    float[] at = null;
                    for (int k = 0; k < 4000 && at == null; k++) {
                        float[] q = w.city.randomWalkableInTown(r);
                        int t = w.city.tileIndex(q[0], q[1]);
                        boolean road = w.city.tiles[t] == City.ROAD;
                        if (Math.hypot(q[0] - base.x, q[1] - base.y) > 1000 && (round == 1) == road) at = q;
                    }
                    Dispatch.Incident inc = new Dispatch.Incident();
                    inc.x = at[0];
                    inc.y = at[1];
                    inc.place = "the test";
                    inc.zombiesNear = round == 0 ? 20 : 14;
                    int before = w.fleet.vehicles.size();
                    check(w.fleet.troopTruck(squad, base, inc), "a vehicle for them");
                    Fleet.Vehicle v = w.fleet.vehicles.get(w.fleet.vehicles.size() - 1);
                    check(w.fleet.vehicles.size() > before && v.kind == (round == 0 ? Fleet.K_IFV : Fleet.K_GUNTRUCK),
                            (round == 0 ? "an IFV" : "a gun truck") + " (kind " + v.kind + ")");
                    // A pack of the dead in range of it while it waits for its squad.
                    java.util.List<Entity> dead = new java.util.ArrayList<Entity>();
                    for (int z = 0; z < 8; z++) {
                        float[] p = w.city.findWalkable(v.x + 110 + (z % 4) * 6, v.y + (z / 4) * 6);
                        if (p == null || !w.city.los(v.x, v.y, p[0], p[1])) continue;
                        Entity zz = w.spawn(Entity.ZOMBIE, p[0], p[1]);
                        zz.speed = zz.runSpeed = 0;
                        dead.add(zz);
                    }
                    for (int f = 0; f < 30 * 12; f++) {
                        for (Entity zz : dead) zz.speed = zz.runSpeed = 0;
                        w.update(1 / 30f);
                        w.evCount = 0;
                    }
                    int down = 0;
                    for (Entity zz : dead) if (zz.dead || zz.hp <= 0) down++;
                    check(dead.size() >= 4 && down * 2 >= dead.size(), "it cuts them down (" + down + " of " + dead.size() + ")");
                }
            }
        });
        test("14.8: civilians fight back together with what's to hand; roadblock barriers hold the dead; the police helicopter marks them", new Check() {
            public void run() {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_PRESET] = 0;
                c.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 78;
                c.normalize();
                c.seed = 78;
                World w = new World(c);
                w.populate(c);
                for (int f = 0; f < 30; f++) w.update(1 / 30f);
                // Three civilians out in a street, one of the dead in front of them.
                java.util.Random r = new java.util.Random(5);
                float[] at = null;
                for (int k = 0; k < 4000 && at == null; k++) {
                    float[] q = w.city.randomWalkableInTown(r);
                    if (w.city.findWalkable(q[0] + 30, q[1]) != null && w.city.los(q[0], q[1], q[0] + 30, q[1]) && w.city.findWalkable(q[0], q[1] + 12) != null) at = q;
                }
                java.util.List<Entity> folk = new java.util.ArrayList<Entity>();
                for (int k = 0; k < 3; k++) {
                    float[] p = w.city.findWalkable(at[0], at[1] + k * 6);
                    Entity e = w.spawn(Entity.CIVILIAN, p[0], p[1]);
                    e.melee = Entity.M_BAT;
                    // (Not the timid: they'd run, as they should.)
                    while (World.bravery(e) < 0.5f) e.nameSeed++;
                    e.hasGun = false;
                    e.ammo = e.reserve = 0;
                    folk.add(e);
                }
                float[] zp = w.city.findWalkable(at[0] + 24, at[1] + 6);
                Entity z = w.spawn(Entity.ZOMBIE, zp[0], zp[1]);
                float hp0 = z.hp;
                int fought = 0;
                for (int f = 0; f < 30 * 10 && !(z.dead || z.hp <= 0); f++) {
                    z.speed = z.runSpeed = 0;
                    for (Entity e : folk) if (e.goal == World.G_FIGHT) fought++;
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(fought > 0 && (z.dead || z.hp < hp0), "they stood and fought it (" + fought + " frames fighting, hp " + z.hp + " of " + hp0 + ")");

                // A roadblock's barriers across a road: the dead are held at them till they push them over.
                float[] road = null;
                int dir = 0;
                for (int k = 0; k < 20000 && road == null; k++) {
                    float[] q = w.city.randomWalkableInTown(r);
                    int tx = (int) (q[0] / City.T), ty = (int) (q[1] / City.T);
                    int d = w.city.roadDirAt(tx, ty);
                    if (d == 1 || d == 2) {
                        road = new float[]{tx * City.T + City.T / 2f, ty * City.T + City.T / 2f};
                        dir = d;
                    }
                }
                check(road != null, "a straight road");
                Fleet.Vehicle v = w.fleet.placeTank(road[0], road[1]);
                w.fleet.vehicles.remove(v);
                v.x = road[0];
                v.y = road[1];
                v.blockDir = dir;
                float ax = dir == 1 ? 0 : 1, ay = dir == 1 ? 1 : 0;
                // The zone lies one way along the road; the dead come from the other.
                w.fleet.putUpBarrier(v, v.x + ax * 300, v.y + ay * 300);
                check(v.barHp > 0 && Math.hypot(v.barX1 - v.barX0, v.barY1 - v.barY0) >= 12, "barriers up across the road");
                float bx = (v.barX0 + v.barX1) / 2, by = (v.barY0 + v.barY1) / 2;
                // (Nobody else about: no one to shoot the dead before they reach it, or to draw them off elsewhere.)
                for (Entity o : w.entities)
                    if (!o.dead && !o.isZombie() && Math.hypot(o.x - bx, o.y - by) < 500) {
                        o.dead = true;
                        o.removed = true;
                    }
                float[] bait = w.city.findWalkable(bx + ax * 30, by + ay * 30);
                Entity held = w.spawn(Entity.CIVILIAN, bait[0], bait[1]);
                java.util.List<Entity> pack = new java.util.ArrayList<Entity>();
                for (int k = 0; k < 8; k++) {
                    float[] p = w.city.findWalkable(bx - ax * (30 + k * 4) + ay * (k % 3 - 1) * 5, by - ay * (30 + k * 4) + ax * (k % 3 - 1) * 5);
                    if (p != null) pack.add(w.spawn(Entity.ZOMBIE, p[0], p[1]));
                }
                int crossedEarly = 0;
                boolean wentDown = false;
                for (int f = 0; f < 30 * 40 && !wentDown; f++) {
                    held.speed = held.runSpeed = 0;
                    held.x = bait[0];
                    held.y = bait[1];
                    held.hp = held.maxHp;
                    w.update(1 / 30f);
                    // (They head for the one beyond, whatever else catches their eye.)
                    for (Entity zz : pack) {
                        if (zz.dead) continue;
                        float ddx = bait[0] - zz.x, ddy = bait[1] - zz.y, dd = (float) Math.hypot(ddx, ddy) + 0.001f;
                        zz.x += ddx / dd * 0.7f;
                        zz.y += ddy / dd * 0.7f;
                    }
                    w.barrierHold(v, 1 / 30f);
                    w.evCount = 0;
                    if (f == 30 * 6)
                        for (Entity zz : pack) if (!zz.dead && ((zz.x - bx) * ax + (zz.y - by) * ay) > 6) crossedEarly++;
                    wentDown = v.barDown;
                }
                check(crossedEarly == 0, "none of them through while the barriers stood (" + crossedEarly + ")");
                check(wentDown, "in the end they pushed them over (" + v.barHp + " left)");

                // A police helicopter over a fight: it marks the dead below and moves over to where they're thickest.
                float[] f0 = w.city.findWalkable(at[0], at[1]);
                java.util.List<Entity> crowd = new java.util.ArrayList<Entity>();
                for (int k = 0; k < 10; k++) {
                    float[] p = w.city.findWalkable(f0[0] + 120 + (k % 4) * 5, f0[1] + (k / 4) * 5);
                    if (p != null) crowd.add(w.spawn(Entity.ZOMBIE, p[0], p[1]));
                }
                for (int f = 0; f < 3; f++) w.update(1 / 30f);
                Fleet.Vehicle h = w.fleet.placeTank(f0[0], f0[1]);
                w.fleet.vehicles.remove(h);
                h.x = f0[0] + 60;
                h.y = f0[1];
                h.tx = f0[0];
                h.ty = f0[1];
                w.heliSpot(h, 1f);
                int marked = 0;
                for (Entity zz : crowd) if (zz.spottedUntil > w.time) marked++;
                check(marked * 2 >= crowd.size(), "it marked the dead below (" + marked + " of " + crowd.size() + ")");
                check(Math.hypot(h.tx - (f0[0] + 120), h.ty - f0[1]) < 60, "and moved its hover over the crowd");
            }
        });
        test("15.0: mounted police - quicker, seeing further, riding the parks; traffic police hold junctions for lights and sirens", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_PRESET] = 0;
                c.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 80;
                c.normalize();
                c.seed = 80;
                World w = new World(c);
                w.populate(c);
                java.util.List<Entity> horses = new java.util.ArrayList<Entity>(), traffic = new java.util.ArrayList<Entity>();
                Entity plain = null;
                for (Entity e : w.entities) {
                    if (e.type != Entity.COP) continue;
                    if (e.role == Entity.ROLE_MOUNTED) horses.add(e);
                    else if (e.role == Entity.ROLE_TRAFFIC) traffic.add(e);
                    else if (e.role == Entity.ROLE_RIFLE && plain == null) plain = e;
                }
                check(horses.size() >= 2, "mounted officers (" + horses.size() + ")");
                check(traffic.size() >= 2, "traffic officers (" + traffic.size() + ")");
                Entity m = horses.get(0);
                check(m.speed > plain.speed * 1.3f && m.runSpeed > plain.runSpeed * 1.3f && m.mass > 2, "a horse is quicker and a crowd gives way to it");
                // Traffic officers on their junctions, holding them for lights and sirens.
                int on = 0, held = -1;
                for (int id = 0; id < w.city.junctions.size(); id++) if (w.trafficOfficer(id) != null) {
                    on++;
                    held = id;
                }
                check(on >= 2, "traffic officers on junctions (" + on + ")");
                Fleet.Vehicle v = w.fleet.placeTank(w.city.worldW() / 2f, w.city.worldH() / 2f);
                w.fleet.vehicles.remove(v);
                w.holdJunction(held, v);
                check(w.junctionHeldFor(held) && w.trafficOfficer(held).waveUntil > w.time, "the junction held, the officer waving it through");
                // Out riding: a mounted officer with nothing doing heads for open ground.
                for (int f = 0; f < 30 * 20; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                int riding = 0;
                for (Entity h : horses) if (!h.dead && h.patrolX != 0) riding++;
                check(riding >= 1, "out riding the parks (" + riding + ")");
                // Up on a horse they see the dead round them and mark them for the others.
                for (Entity hh : horses) if (!hh.dead && !hh.removed && hh.ride == null) m = hh;
                float[] p = w.city.findWalkable(m.x + 90, m.y);
                Entity z = w.spawn(Entity.ZOMBIE, p[0], p[1]);
                z.speed = z.runSpeed = 0;
                boolean marked = false;
                for (int f = 0; f < 30 * 2 && !marked; f++) {
                    // (Tough enough not to be shot down before the next look round.)
                    z.hp = z.maxHp = 100000;
                    w.update(1 / 30f);
                    w.evCount = 0;
                    marked = z.spottedUntil > w.time;
                }
                check(marked, "the dead nearby marked for the others");
                // The dead come near: the traffic officers leave their junctions and join in.
                java.util.List<Entity> near = new java.util.ArrayList<Entity>(), threatened = new java.util.ArrayList<Entity>();
                for (Entity t : traffic) {
                    // (Where they can see it: along the street from their corner.)
                    float[] q = null;
                    for (int k = 0; k < 8 && q == null; k++) {
                        float ang = k * (float) Math.PI / 4;
                        float[] p2 = w.city.findWalkable(t.x + (float) Math.cos(ang) * 60, t.y + (float) Math.sin(ang) * 60);
                        if (p2 != null && w.city.los(t.x, t.y, p2[0], p2[1])) q = p2;
                    }
                    if (q != null) {
                        near.add(w.spawn(Entity.ZOMBIE, q[0], q[1]));
                        threatened.add(t);
                    }
                }
                for (int f = 0; f < 30 * 3; f++) {
                    for (Entity zz : near) {
                        zz.hp = zz.maxHp = 100000;
                        zz.speed = zz.runSpeed = 0;
                    }
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                int still = 0;
                // (Those with one of the dead still within reach of them: one put down by someone else doesn't count.)
                for (Entity t : threatened) if (!t.dead && t.task == Dispatch.T_POST && w.countZombiesNear(t.x, t.y, 140) > 0) still++;
                check(threatened.size() >= 1, "the dead near some of them");
                check(still == 0, "off their junctions with the dead near (" + still + " still there)");
            }
        });
        test("15.3: the dead a grenade kills count as the thrower's kills", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_PRESET] = 0;
                c.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 82;
                c.normalize();
                c.seed = 82;
                World w = new World(c);
                w.populate(c);
                java.util.Random r = new java.util.Random(4);
                float[] at = null;
                for (int k = 0; k < 4000 && at == null; k++) {
                    float[] q = w.city.randomWalkableInTown(r);
                    if (w.city.findWalkable(q[0] + 70, q[1]) != null && w.city.los(q[0], q[1], q[0] + 70, q[1])) at = q;
                }
                // (Nobody else about to shoot them first.)
                for (Entity o : w.entities) if (!o.dead && Math.hypot(o.x - at[0], o.y - at[1]) < 500) { o.dead = true; o.removed = true; }
                Entity soldier = w.spawn(Entity.SOLDIER, at[0], at[1]);
                soldier.ammo = soldier.reserve = 0;
                float[] p = w.city.findWalkable(at[0] + 70, at[1]);
                java.util.List<Entity> pack = new java.util.ArrayList<Entity>();
                for (int k = 0; k < 4; k++) {
                    Entity z = w.spawn(Entity.ZOMBIE, p[0] + (k % 2) * 4, p[1] + (k / 2) * 4);
                    // (Already hurt: a grenade doesn't always finish off a fresh one.)
                    z.hp = 50;
                    z.armored = false;
                    pack.add(z);
                }
                for (int f = 0; f < 3; f++) w.update(1 / 30f);
                java.lang.reflect.Method tg = World.class.getDeclaredMethod("throwGrenade", Entity.class, float.class, float.class);
                tg.setAccessible(true);
                // (Straight onto them: no scatter.)
                tg.invoke(w, soldier, p[0] + 2, p[1] + 2);
                World.Grenade g = w.grenades.get(w.grenades.size() - 1);
                g.tx = p[0] + 2;
                g.ty = p[1] + 2;
                int before = soldier.kills;
                for (int f = 0; f < 45; f++) {
                    for (Entity z : pack) z.speed = z.runSpeed = 0;
                    soldier.ammo = soldier.reserve = 0;
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                int down = 0;
                for (Entity z : pack) if (z.dead || z.hp <= 0) down++;
                check(down >= 2, "the grenade killed some (" + down + ")");
                check(soldier.kills - before == down, "every one of them the thrower's kill (" + (soldier.kills - before) + " of " + down + ")");
            }
        });
        test("15.4: mortar teams have only the rounds they carry, and once out they go and fight", new Check() {
            public void run() {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_PRESET] = 0;
                c.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 83;
                c.normalize();
                c.seed = 83;
                World w = new World(c);
                w.populate(c);
                java.util.List<Entity> teams = new java.util.ArrayList<Entity>();
                for (Entity e : w.entities) if (!e.dead && e.role == Entity.ROLE_MORTAR && e.task == Dispatch.T_POST) teams.add(e);
                check(teams.size() >= 2, "mortar teams in their pits (" + teams.size() + ")");
                Entity a = teams.get(0), b = teams.get(1);
                check(a.grenades == World.MORTAR_ROUNDS, "a full load to start with");
                a.grenades = 5;
                b.grenades = 0;
                for (int f = 0; f < 30 * 40; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(a.grenades == 5, "no more rounds from the base (" + a.grenades + ")");
                check(a.task == Dispatch.T_POST, "one with rounds left stays in its pit");
                check(b.task != Dispatch.T_POST, "one with none left has gone to fight (task " + b.task + ")");
            }
        });
        test("16.6: the base's armoury isn't emptied at the start by everyone short of rounds topping up; parked vehicles drawn close up", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 77;
                c.normalize();
                c.seed = 77;
                World w = new World(c);
                w.populate(c);
                City.Facility base = w.city.mainBase();
                check(base != null, "a base");
                // (As in a city caught unprepared: everyone with half their spare rounds.)
                int short_ = 0;
                for (Entity e : w.entities)
                    if (e.isArmed() && (e.type == Entity.SOLDIER || e.type == Entity.COP)) {
                        e.reserve = World.fullReserve(e) / 2;
                        short_++;
                    }
                check(short_ > 50, "plenty short of rounds (" + short_ + ")");
                base.ammo = 9000;
                for (int f = 0; f < 30 * 10; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(base.ammo >= 4500, "half the base's armoury still there after the first ten seconds (" + base.ammo + ")");
                check(base.ammo < 9000, "but those at it did top up some (" + base.ammo + ")");
                // Every parked vehicle in view drawn again, sharp, without trouble.
                w.city.drawParked(new android.graphics.Canvas(android.graphics.Bitmap.createBitmap(64, 64, android.graphics.Bitmap.Config.ARGB_8888)), 0, 0, w.city.worldW(), w.city.worldH());
                int trucks = 0;
                for (City.Facility f : w.city.facilities)
                    if (f.kind == City.FACILITY_BASE) {
                        int n = w.city.parkedTrucks(f.x, f.y, Math.max(260, f.r * 2.5f));
                        for (Fleet.Vehicle v : w.fleet.vehicles)
                            if (v.type == Fleet.TRUCK && v.standby && World.hypot(v.x - f.x, v.y - f.y) < Math.max(260, f.r * 2.5f)) n++;
                        check(n >= 2, f.name + " has trucks to be seen (" + n + ")");
                        trucks += n;
                    }
                check(trucks > 0, "trucks at the bases");
            }
        });
        test("16.8: the quick ways to find things on the ground and open ground give the same answers as the long ways", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 9;
                World w = new World(c);
                java.util.Random r = new java.util.Random(3);
                int same;
                // Things on the ground: the same, by the grid and the long way.
                w.pickups.clear();
                for (int k = 0; k < 120; k++) {
                    World.Pickup p = new World.Pickup();
                    p.x = r.nextFloat() * w.city.worldW();
                    p.y = r.nextFloat() * w.city.worldH();
                    p.claimed = r.nextInt(5) == 0;
                    w.pickups.add(p);
                }
                same = 0;
                for (int k = 0; k < 400; k++) {
                    float x = r.nextFloat() * w.city.worldW(), y = r.nextFloat() * w.city.worldH(), radius = 50 + r.nextFloat() * 1500;
                    World.Pickup want = null;
                    float bd = radius * radius;
                    for (World.Pickup p : w.pickups) {
                        if (p.claimed || p.drop > 0 || p.melee > 0) continue;
                        float d = (p.x - x) * (p.x - x) + (p.y - y) * (p.y - y);
                        if (d < bd) {
                            bd = d;
                            want = p;
                        }
                    }
                    if (w.nearestPickup(x, y, radius) == want) same++;
                    // (And when one goes, it's gone from the grid at once.)
                    if (k == 200) w.pickups.remove(0);
                }
                check(same == 400, "the nearest thing on the ground by the grid is the nearest one (" + same + " of 400)");
                // Open ground, kept a bit a tile: the same as working it out the long way, and after a tile changes.
                int differ = 0;
                for (int k = 0; k < 4000; k++) {
                    float x = r.nextFloat() * w.city.worldW(), y = r.nextFloat() * w.city.worldH(), rad = 2 + r.nextFloat() * 5;
                    boolean quick = w.city.circleBlocked(x, y, rad);
                    City.slowSteps = true;
                    boolean slow = w.city.circleBlocked(x, y, rad);
                    City.slowSteps = false;
                    if (quick != slow) differ++;
                }
                check(differ == 0, "open ground by the bits agrees with the long way (" + differ + " differ)");
            }
        });
        test("16.9: no gun stores or fire stations; an FBI field office with its agents and cars; police trained differently; no barricading", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 77;
                c.normalize();
                c.seed = 77;
                World w = new World(c);
                w.populate(c);
                for (int f = 0; f < 60; f++) w.update(1 / 30f);
                int guns = 0, fire = 0;
                for (City.Building b : w.city.buildings) if (b.kind == City.SHOP && b.shopType == 1) guns++;
                for (City.Facility f : w.city.facilities) if (f.kind == City.FACILITY_FIRE) fire++;
                check(guns == 0 && fire == 0, "no gun stores (" + guns + ") and no fire stations (" + fire + ")");
                City.Facility office = null;
                for (City.Facility f : w.city.facilities) if (f.fed) office = f;
                check(office != null && office.kind == City.FACILITY_POLICE, "an FBI field office");
                int agents = 0, cars = 0;
                for (Entity e : w.entities) if (e.type == Entity.COP && e.agency == 4 && Math.hypot(e.x - office.x, e.y - office.y) < 400) agents++;
                for (Fleet.Vehicle v : w.fleet.vehicles) if (v.agency == 4 && Math.hypot(v.x - office.x, v.y - office.y) < 500) cars++;
                check(agents >= 8 && cars >= 2, "agents (" + agents + ") and their cars (" + cars + ") at the office from the start");
                // A small share of civilians carry a gun.
                int civ = 0, armed = 0;
                for (Entity e : w.entities) if (e.type == Entity.CIVILIAN) {
                    civ++;
                    if (e.hasGun) armed++;
                }
                check(armed > 0 && armed < civ * 0.16f, "a few civilians armed (" + armed + " of " + civ + ")");
                // Trained differently: SWAT and FBI shoot better than a patrol officer, traffic police worse; SWAT hold on longer.
                Entity swat = new Entity(), traffic = new Entity(), patrol = new Entity(), fbi = new Entity();
                swat.type = traffic.type = patrol.type = fbi.type = Entity.COP;
                swat.role = Entity.ROLE_SWAT;
                traffic.role = Entity.ROLE_TRAFFIC;
                fbi.agency = 4;
                check(World.doctrine(swat)[0] > World.doctrine(patrol)[0] && World.doctrine(fbi)[0] > World.doctrine(patrol)[0]
                        && World.doctrine(traffic)[0] < World.doctrine(patrol)[0], "aim: SWAT and FBI better, traffic worse");
                check(World.doctrine(swat)[5] > World.doctrine(patrol)[5] && World.doctrine(traffic)[5] < World.doctrine(patrol)[5], "nerve: SWAT more, traffic less");
                check(World.doctrine(new Entity()) == null, "only police have a police training");
                // Nobody barricades: a building with people sheltering in it keeps the door it has.
                City.Building home = null;
                for (City.Building b : w.city.buildings) if (b.capacity > 4 && !b.collapsed && b.doorX > 0) { home = b; break; }
                check(home != null, "a building to shelter in");
                home.barricade = 40;
                for (int k = 0; k < 4; k++) {
                    Entity p = w.spawn(Entity.CIVILIAN, home.doorX, home.doorY);
                    p.dead = p.removed = true;
                    home.occupants.add(p);
                }
                for (int f = 0; f < 30 * 10; f++) w.update(1 / 30f);
                check(home.barricade <= 40.01f, "the door isn't shored up (" + home.barricade + ")");
            }
        });
        test("17.1: walk-in buildings - walls with doorways, sight through doors only, in and out on foot, visits, the dead follow", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 41;
                World w = new World(c);
                w.rnd.setSeed(41);
                w.populate(c);
                City city = w.city;
                // A building people can walk into, with a room or two.
                City.Building b = null;
                int enterable = 0;
                for (City.Building o : city.buildings)
                    if (city.enterable(o)) {
                        enterable++;
                        if (b == null && city.planOf(o).rooms.size() >= 3 && o.x1 - o.x0 < 140 && o.y1 - o.y0 < 140) b = o;
                    }
                check(enterable > city.buildings.size() / 3, "most buildings can be walked into (" + enterable + " of " + city.buildings.size() + ")");
                check(b != null && b.walls != null && b.walls.length >= 20, "with walls round it and between its rooms");
                // The walls are in the way; the doorway isn't.
                check(!city.circleBlocked(b.inX, b.inY, 3.8f) && !city.circleBlocked(b.gapX, b.gapY, 3.8f), "the doorway is open");
                float wx = (b.x0 + b.x1) / 2, wy = b.y0 + 1;
                if (Math.abs(b.gapY - (b.y0 + 1)) < 2 && Math.abs(b.gapX - wx) < 12) wy = b.y1 - 1;
                check(city.circleBlocked(wx, wy, 3.8f), "a wall is in the way");
                check(city.solidAt(b.x0 + 1, (b.y0 + b.y1) / 2) && !city.solidAt(b.inX, b.inY), "solid where the walls are, not inside");
                Interiors.Plan p = city.planOf(b);
                float[] far = null;
                for (float[] sp : p.spots) if (far == null || Math.hypot(sp[0] - b.inX, sp[1] - b.inY) > Math.hypot(far[0] - b.inX, far[1] - b.inY)) far = sp;
                check(far != null && city.insideOf(far[0], far[1]) == b, "a place to be inside it");
                // Seen through the doorway, not through the walls.
                check(city.los(b.doorX, b.doorY, b.inX, b.inY), "seen through the door");
                float ox = b.doorX < b.x0 ? b.x0 - 20 : b.x1 + 20, oy = (b.y0 + b.y1) / 2;
                if (b.doorX >= b.x0 && b.doorX <= b.x1) ox = b.x0 - 20;
                check(!city.los(ox, oy, (b.x0 + b.x1) / 2, (b.y0 + b.y1) / 2), "not through the wall");
                // Walking in from the street to the far room, and out again: every step a short one, never a jump.
                java.lang.reflect.Method walk = World.class.getDeclaredMethod("walkToSpot", Entity.class, float.class, float.class, float.class);
                walk.setAccessible(true);
                java.lang.reflect.Method move = World.class.getDeclaredMethod("move", Entity.class, float.class);
                move.setAccessible(true);
                java.lang.reflect.Field budget = World.class.getDeclaredField("pathBudget");
                budget.setAccessible(true);
                float[] start = city.findWalkable(b.doorX + (b.doorX - (b.x0 + b.x1) / 2) * 0.6f, b.doorY + (b.doorY - (b.y0 + b.y1) / 2) * 0.6f);
                Entity e = w.spawn(Entity.CIVILIAN, start[0], start[1]);
                check(city.insideOf(e.x, e.y) == null, "starts outside");
                float jump = 0;
                boolean got = false;
                for (int f = 0; f < 30 * 40 && !got; f++) {
                    budget.setFloat(w, 3);
                    float px = e.x, py = e.y;
                    walk.invoke(w, e, far[0], far[1], e.speed);
                    move.invoke(w, e, 1 / 30f);
                    jump = Math.max(jump, (float) Math.hypot(e.x - px, e.y - py));
                    got = Math.hypot(e.x - far[0], e.y - far[1]) < 5;
                }
                check(got, "walked in through the door to the far room (" + (int) Math.hypot(e.x - far[0], e.y - far[1]) + " short)");
                boolean out = false;
                for (int f = 0; f < 30 * 40 && !out; f++) {
                    budget.setFloat(w, 3);
                    float px = e.x, py = e.y;
                    walk.invoke(w, e, start[0], start[1], e.speed);
                    move.invoke(w, e, 1 / 30f);
                    jump = Math.max(jump, (float) Math.hypot(e.x - px, e.y - py));
                    out = Math.hypot(e.x - start[0], e.y - start[1]) < 5;
                }
                check(out, "and back out to the street");
                check(jump < 3, "on foot all the way, no jumps (" + jump + ")");
                e.dead = true;
                e.removed = true;
                // An ordinary day: people go in and out of buildings and are there inside, never taken off the map.
                int entered = 0, left = 0, seenInside = 0;
                java.util.IdentityHashMap<Entity, Boolean> was = new java.util.IdentityHashMap<Entity, Boolean>();
                for (int f = 0; f < 30 * 60; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                    if (f % 15 != 0) continue;
                    for (Entity o : w.entities) {
                        if (o.dead) continue;
                        boolean in = city.insideOf(o.x, o.y) != null;
                        Boolean prev = was.put(o, in);
                        if (prev != null && !prev && in) entered++;
                        if (prev != null && prev && !in) left++;
                    }
                }
                for (Entity o : w.entities) if (!o.dead && city.insideOf(o.x, o.y) != null) seenInside++;
                int listed = 0;
                for (City.Building o : city.buildings) listed += o.visitors.size() + o.occupants.size();
                check(entered > 30 && left > 10, "people walk in and out (" + entered + " in, " + left + " out)");
                check(seenInside > 10 && listed == 0, "there inside, on the map (" + seenInside + "), nobody vanished into a building (" + listed + ")");
                w.countInside();
                int over = 0;
                for (City.Building o : w.occupied) if (o.inside > World.room(o) * 2) over++;
                check(over == 0, "no building packed past its room (" + over + ")");
                // One of the dead after someone who's gone inside: in it goes after them.
                Entity v = null;
                for (Entity o : w.entities)
                    if (!o.dead && o.type == Entity.CIVILIAN && o.stayIn != null && city.insideOf(o.x, o.y) == o.stayIn) {
                        v = o;
                        break;
                    }
                check(v != null, "someone inside");
                City.Building vb = city.insideOf(v.x, v.y);
                Entity z = w.spawn(Entity.ZOMBIE, vb.doorX, vb.doorY);
                z.memory = 10;
                z.lastX = v.x;
                z.lastY = v.y;
                boolean zin = false;
                for (int f = 0; f < 30 * 15 && !zin && !z.dead; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                    zin = city.insideOf(z.x, z.y) == vb;
                }
                check(zin, "the dead follow them in through the door");
            }
        });
        test("17.3: the menu's city and the last game are let go of while the next one loads; the war bar adds up", new Check() {
            public void run() throws Exception {
                GameView v = new GameView(new android.app.Activity());
                int[] size = {1200, 900};
                v.layout(size[0], size[1]);
                for (int k = 0; k < 3; k++) draw(v, size);
                java.lang.ref.WeakReference<Object> menuCity = new java.lang.ref.WeakReference<Object>(((World) field(v, "world")).city);
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_ZOMBIES] = 2;
                v.startGame(c);
                for (int i = 0; i < 4 && menuCity.get() != null; i++) {
                    System.gc();
                    Thread.sleep(100);
                }
                check(menuCity.get() == null, "the menu's city is let go of as soon as the game starts building");
                v.awaitLoad();
                Menu m = (Menu) field(v, "menu");
                for (int k = 0; k < 10; k++) draw(v, size);
                m.screen = Menu.NONE;
                for (int k = 0; k < 10; k++) draw(v, size);
                java.lang.ref.WeakReference<Object> first = new java.lang.ref.WeakReference<Object>(field(v, "world"));
                CityConfig c2 = new CityConfig();
                c2.v[CityConfig.OPT_ZOMBIES] = 4;
                v.startGame(c2);
                for (int i = 0; i < 4 && first.get() != null; i++) {
                    System.gc();
                    Thread.sleep(100);
                }
                check(first.get() == null, "and the last game, once saved, while the next one builds");
                v.awaitLoad();
                for (int k = 0; k < 10; k++) draw(v, size);
                m.screen = Menu.NONE;
                for (int k = 0; k < 10; k++) draw(v, size);
                check(v.drawErrors == 0, "drew without an error");
                // The war bar: the dead against the city.
                World w = (World) field(v, "world");
                w.recount();
                for (int i = 0; i < 30 * 3; i++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(w.outbreak, "the war's on");
                // All of the dead gone: it says so.
                for (int i = 0; i < 30 * 20; i++) {
                    for (Entity e : w.entities) {
                        if (e.isZombie()) e.hp = 0;
                        e.infected = false;
                    }
                    w.update(1 / 30f);
                    w.evCount = 0;
                    if (!w.outbreak) break;
                }
                check(w.warBalance > 0.9f || !w.outbreak, "and the city's side ahead once they're down (" + w.warBalance + ")");
            }
        });
        test("17.5: the after-action report; nobody goes down wounded any more", new Check() {
            public void run() throws Exception {
                // ((20.0) Shot is shot: no going down to bleed and be saved.)
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_SIZE] = CityConfig.TINY;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 25;
                World w = new World(c);
                w.populate(c);
                Entity shot = null;
                for (Entity e : w.entities) if (!e.dead && e.type == Entity.CIVILIAN && e.ride == null) { shot = e; break; }
                int n = w.entities.size();
                shot.hp = 0;
                w.update(1 / 30f);
                w.evCount = 0;
                check(shot.dead && !w.entities.contains(shot) && w.entities.size() < n, "someone with no health left is dead at once");
                // The report: drawn, at the end of a game it opens itself.
                GameView v = new GameView(new android.app.Activity());
                int[] size = {1080, 2000};
                v.layout(size[0], size[1]);
                Menu m = (Menu) field(v, "menu");
                CityConfig c2 = new CityConfig();
                c2.v[CityConfig.OPT_ZOMBIES] = 2;
                v.startGame(c2);
                v.awaitLoad();
                for (int k = 0; k < 3; k++) draw(v, size);
                m.screen = Menu.NONE;
                World gw = (World) field(v, "world");
                for (int f = 0; f < 30 * 3; f++) {
                    gw.update(1 / 30f);
                    gw.evCount = 0;
                }
                for (int f = 0; f < 30 * 30 && gw.warResult == 0; f++) {
                    for (Entity e : gw.entities) {
                        if (e.isZombie()) e.hp = 0;
                        e.infected = false;
                    }
                    gw.update(1 / 30f);
                    gw.evCount = 0;
                }
                check(gw.warResult == 1, "the outbreak's over");
                gw.warBannerTime = 0;
                draw(v, size);
                check(m.screen == Menu.REPORT, "and the report's up");
                java.lang.reflect.Field sc = Menu.class.getDeclaredField("scroll");
                sc.setAccessible(true);
                for (int k = 0; k < 4; k++) {
                    sc.setFloat(m, k * 600);
                    draw(v, size);
                }
                check(v.drawErrors == 0, "drawn without an error (" + v.drawErrors + ")");
            }
        });
        test("18.0: campaign missions - set forces, a budget, commander deploys, objectives, stars, veterans, saves", new Check() {
            public void run() throws Exception {
                Campaign.Mission m0 = Campaign.MISSIONS[0];
                CityConfig c = Campaign.config(0);
                check(c.mission == 0 && c.zombies() == m0.foes, "the mission's city and enemy");
                World w = new World(c);
                w.rnd.setSeed(3);
                w.populate(c);
                Campaign.begin(w, "");
                w.recount();
                check(w.mission() == m0, "it's a mission");
                check(w.counts[Entity.COP] <= m0.cops && w.counts[Entity.COP] >= m0.cops - 4, "the police cut to the mission's (" + w.counts[Entity.COP] + ")");
                check(w.counts[Entity.SOLDIER] <= m0.soldiers, "and the soldiers (" + w.counts[Entity.SOLDIER] + ")");
                check(w.dispatch.policeReserve == 0 && w.dispatch.squadReserve == 0, "no help from outside");
                // The budget: start money, spent, earned.
                check(Campaign.budget(w) == m0.budget, "starts with the mission's money");
                check(Campaign.spend(w, 10) && Campaign.budget(w) == m0.budget - 10, "spending takes it off");
                check(!Campaign.spend(w, 100000), "can't spend what isn't there");
                int before = Campaign.budget(w);
                w.zombiesKilled += 5;
                check(Campaign.budget(w) > before, "kills earn money");
                float[] at = Campaign.arrival(w, true, w.city.worldW() / 2, w.city.worldH() / 2);
                check(at != null, "police have somewhere to come in from");
                // Run out the clock: failed.
                w.missionStart = w.time - m0.limit - 1;
                java.lang.reflect.Method life = World.class.getDeclaredMethod("cityLife");
                life.setAccessible(true);
                life.invoke(w);
                check(w.missionResult == 2 && "Out of time.".equals(w.missionWhy), "out of time, the mission's failed (" + w.missionResult + ", " + w.missionWhy + ")");
                // Won: every zombie down.
                World w2 = new World(Campaign.config(0));
                w2.populate(w2.city.cfg);
                Campaign.begin(w2, "");
                for (int f = 0; f < 30 * 40 && w2.missionResult == 0; f++) {
                    for (Entity e : w2.entities) if (e.isUndead()) e.hp = 0;
                    w2.update(1 / 30f);
                    w2.evCount = 0;
                }
                check(w2.missionResult == 1 && w2.missionStars >= 1, "cleared: won with stars (" + w2.missionResult + ", " + w2.missionStars + ")");
                // Saved and loaded.
                w2.spent = 37;
                File file = File.createTempFile("zcs", ".dat");
                SaveGame.save(w2, file);
                World l = SaveGame.load(file);
                file.delete();
                check(l.mission() == m0 && l.spent == 37 && l.missionResult == 1 && l.missionStars == w2.missionStars, "the mission's kept in a save");
                // Veterans: a ranked officer goes on the roster and comes back in the next mission.
                Entity hero = null;
                for (Entity e : w2.entities) if (!e.dead && e.type == Entity.COP) { hero = e; break; }
                hero.kills = 20;
                String roster = Campaign.veterans(w2);
                check(Campaign.rosterSize(roster) >= 1 && roster.contains("," + hero.nameSeed + ";"), "a ranked officer's a veteran (" + roster + ")");
                World w3 = new World(Campaign.config(1));
                w3.populate(w3.city.cfg);
                Campaign.begin(w3, roster);
                boolean back = false;
                for (Entity e : w3.entities) back |= e.type == Entity.COP && e.nameSeed == hero.nameSeed && e.kills == 20;
                check(back, "and is back in the next mission");
                // The campaign's progress on the phone.
                Records rec = new Records(new android.app.Activity());
                check(rec.unlocked(0) && !rec.unlocked(1), "only the first mission open at first");
                rec.missionWon(0, 2, roster);
                check(rec.unlocked(1) && rec.missionStars(0) == 2 && rec.veterans().equals(roster), "winning opens the next and keeps the stars and the roster");
                rec.missionWon(0, 1, "");
                check(rec.missionStars(0) == 2, "the best stars kept");
                // The commander's screen: only their tools, buying units that come in and are sent to the spot.
                GameView v = new GameView(new android.app.Activity());
                int[] size = {1080, 2000};
                v.layout(size[0], size[1]);
                Menu menu = (Menu) field(v, "menu");
                v.startGame(Campaign.config(2));
                v.awaitLoad();
                for (int k = 0; k < 3; k++) draw(v, size);
                menu.screen = Menu.NONE;
                World gw = (World) field(v, "world");
                check(gw.mission() == Campaign.MISSIONS[2], "the mission's started from the view");
                java.lang.reflect.Method hidden = GameView.class.getDeclaredMethod("hiddenTool", int.class);
                hidden.setAccessible(true);
                check((Boolean) hidden.invoke(v, 2) && (Boolean) hidden.invoke(v, 6) && !(Boolean) hidden.invoke(v, 3) && !(Boolean) hidden.invoke(v, 1),
                        "no people or enemy to spawn, police and orders there");
                java.lang.reflect.Field tool = GameView.class.getDeclaredField("tool");
                tool.setAccessible(true);
                tool.setInt(v, 3);
                gw.recount();
                int cops = gw.counts[Entity.COP], money = Campaign.budget(gw);
                java.lang.reflect.Method cmd = GameView.class.getDeclaredMethod("commandAt", float.class, float.class);
                cmd.setAccessible(true);
                float tx = 0, ty = 0;
                for (Entity e : gw.entities)
                    if (!e.dead && e.type == Entity.CIVILIAN && e.ride == null && gw.city.insideOf(e.x, e.y) == null) {
                        tx = e.x;
                        ty = e.y;
                        break;
                    }
                cmd.invoke(v, tx, ty);
                gw.recount();
                check(gw.counts[Entity.COP] == cops + 1 && Campaign.budget(gw) == money - Campaign.COP_COST[0], "a cop bought (" + cops + " -> "
                        + gw.counts[Entity.COP] + ", $" + money + " -> $" + Campaign.budget(gw) + ")");
                Entity newest = null;
                for (Entity e : gw.entities) if (e.type == Entity.COP && e.task == Dispatch.T_MOVE) newest = e;
                check(newest != null && World.hypot(newest.postX - tx, newest.postY - ty) < 120, "and sent to the spot");
                gw.spent = 1000000;
                cmd.invoke(v, tx, ty);
                gw.recount();
                check(gw.counts[Entity.COP] == cops + 1, "no money, no more");
                for (int k = 0; k < 3; k++) draw(v, size);
                // The campaign screen and the mission report draw, and a finished mission opens its report.
                menu.open(Menu.CAMPAIGN);
                draw(v, size);
                menu.screen = Menu.NONE;
                Campaign.finish(gw, 1, null);
                gw.warBannerTime = 0;
                draw(v, size);
                check(menu.screen == Menu.REPORT, "the report's up at the end");
                draw(v, size);
                check(v.drawErrors == 0, "drawn without an error (" + v.drawErrors + ")");
            }
        });
        test("18.4: a police drone sent to watch; looters; heavy destruction", new Check() {
            public void run() throws Exception {
                check(Synth.TRACKS == 10, "ten pieces of music");
                // A drone sent out: it gets there, and marks the dead under it.
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                c.v[CityConfig.OPT_ZOMBIES] = 2;
                c.seed = 31;
                World w = new World(c);
                w.setMaxPopulation(4000);
                w.populate(c);
                Entity foe = null;
                for (Entity e : w.entities) if (!e.dead && e.isZombie()) { foe = e; break; }
                float[] d = w.launchDrone(foe.x, foe.y);
                for (int f = 0; f < 30 * 40; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(World.hypot(d[0] - d[2], d[1] - d[3]) < 60 && w.droneOver(d[2], d[3]), "over the spot, marking it");
                for (int f = 0; f < 30 * (int) World.WATCH_LIFE; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(w.policeDrones.isEmpty(), "and home when its battery's done");
                // Looters: once the outbreak's gone on a while, looting gangs form.
                CityConfig lc = new CityConfig();
                lc.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                lc.v[CityConfig.OPT_ZOMBIES] = 5;
                lc.seed = 32;
                World lw = new World(lc);
                lw.setMaxPopulation(4000);
                lw.populate(lc);
                check(lw.zombieCount() > 0, "a zombie outbreak");
                java.lang.reflect.Field formed = World.class.getDeclaredField("gangsFormed");
                formed.setAccessible(true);
                for (int f = 0; f < 30 * 30; f++) {
                    lw.update(1 / 30f);
                    lw.evCount = 0;
                }
                // (However quickly the police get on top of the dead: the outbreak's still on for this.)
                java.lang.reflect.Method raid = World.class.getDeclaredMethod("raiders");
                raid.setAccessible(true);
                for (int k = 0; k < 600 && formed.getInt(lw) == 0; k++) {
                    lw.outbreak = true;
                    lw.outbreakTime = Math.max(lw.outbreakTime, 400);
                    lw.recount();
                    raid.invoke(lw);
                }
                check(formed.getInt(lw) > 0, "looters out (" + formed.getInt(lw) + " gangs by " + (int) lw.outbreakTime + "s)");
                // Heavy destruction: the same blast does far more.
                float[] lost = new float[2];
                for (int k = 0; k < 2; k++) {
                    CityConfig hc = new CityConfig();
                    hc.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                    hc.v[CityConfig.OPT_ZOMBIES] = 0;
                    hc.seed = 33;
                    World hw = new World(hc);
                    hw.populate(hc);
                    hw.heavy = k == 1;
                    City.Building b = null;
                    for (City.Building x : hw.city.buildings) if (b == null && !x.collapsed && x.x1 - x.x0 > 30 && x.maxHp > 0) b = x;
                    float before = 0;
                    for (City.Building x : hw.city.buildings) if (!x.collapsed) before += x.hp;
                    hw.explode((b.x0 + b.x1) / 2, (b.y0 + b.y1) / 2, 70, 300);
                    float after = 0;
                    for (City.Building x : hw.city.buildings) if (!x.collapsed) after += x.hp;
                    lost[k] = before - after;
                }
                check(lost[1] > lost[0] * 1.2f, "heavy destruction does more (" + (int) lost[0] + " -> " + (int) lost[1] + ")");
            }
        });
        test("18.5: drones under way are kept in a save", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                c.v[CityConfig.OPT_ZOMBIES] = 3;
                c.seed = 41;
                World w = new World(c);
                w.setMaxPopulation(4000);
                w.populate(c);
                w.launchDrone(w.city.worldW() / 2, w.city.worldH() / 2);
                File file = File.createTempFile("zcs", ".dat");
                SaveGame.save(w, file);
                World l = SaveGame.load(file);
                file.delete();
                check(l.policeDrones.size() == 1, "the drone still up");
                for (int f = 0; f < 60; f++) {
                    l.update(1 / 30f);
                    l.evCount = 0;
                }
                check(l.zombieCount() > 0 || l.warResult != 0, "carries on");
            }
        });
        test("19.0: the Sound Update - new sounds, ambience beds, the mixer's groups, mono, deafening, captions, the Sound Lab", new Check() {
            public void run() throws Exception {
                check(Sfx.MIN_GAP.length == Sfx.COUNT && Sfx.NAMES.length == Sfx.COUNT && Sfx.BUS.length == Sfx.COUNT, "every sound has its settings and a name");
                // Every new sound: made, not silent, nothing broken in it.
                for (int id = Sfx.CASH; id < Sfx.COUNT; id++)
                    for (int v = 0; v < Sfx.VARIANTS[id]; v++) {
                        short[] s = Synth.effect(id, v);
                        int peak = 0;
                        for (short x : s) peak = Math.max(peak, Math.abs(x));
                        check(s.length > Synth.RATE / 50 && peak > 4000, Sfx.NAMES[id] + " take " + v + " (" + s.length + ", " + peak + ")");
                    }
                // The ambience beds: the right length, not silent, and no click where they loop round.
                for (int l = 0; l < Sfx.LOOPS; l++) {
                    short[] s = Synth.ambience(l);
                    int peak = 0;
                    for (short x : s) peak = Math.max(peak, Math.abs(x));
                    check(s.length == (int) (Synth.LOOP_SECONDS * Synth.RATE) && peak > 4000, Sfx.LOOP_NAMES[l] + " bed (" + s.length + ", " + peak + ")");
                    int seam = Math.abs(s[s.length - 1] - s[0]);
                    check(seam < peak * 0.6f, Sfx.LOOP_NAMES[l] + " loops without a click (" + seam + " of " + peak + ")");
                }
                // The mixer: a bed playing; a group turned off; mono; deafened.
                Mixer m = new Mixer(48000);
                m.setLoopSamples(Sfx.LOOP_WIND, Synth.ambience(Sfx.LOOP_WIND));
                m.setLoop(Sfx.LOOP_WIND, 0.8f, 0.5f, 0);
                float[] buf = new float[512 * 2];
                float energy = 0;
                for (int k = 0; k < 60; k++) {
                    m.render(buf, 512);
                    for (float f : buf) energy += f * f;
                }
                check(energy > 1 && m.loopGain(Sfx.LOOP_WIND) > 0.4f, "an ambience bed plays (" + energy + ")");
                m.setLoop(Sfx.LOOP_WIND, 0, 0, 0);
                for (int k = 0; k < 200; k++) m.render(buf, 512);
                check(m.loopGain(Sfx.LOOP_WIND) < 0.02f, "and fades away");
                m.setSamples(Sfx.PISTOL, new short[][]{Synth.effect(Sfx.PISTOL, 0)});
                m.setBus(Sfx.BUS_GUNS, 0);
                m.trigger(Sfx.PISTOL, 1, 0, 0, 1);
                m.render(buf, 512);
                check(m.playing() == 0, "a group turned right down plays nothing");
                m.setBus(Sfx.BUS_GUNS, 1);
                m.setMono(true);
                m.trigger(Sfx.PISTOL, 1, 1, 0, 1);
                m.render(buf, 512);
                boolean same = true;
                for (int f = 0; f < 512; f++) same &= Math.abs(buf[f * 2] - buf[f * 2 + 1]) < 1e-5f;
                check(same, "mono: the same in both ears");
                m.setMono(false);
                m.shock(1);
                check(m.shockLevel() > 0.9f, "deafened by a blast");
                for (int k = 0; k < 400; k++) m.render(buf, 512);
                check(m.shockLevel() < 0.01f, "and it wears off");
                // The game: the ambience follows the screen; captions for sounds off it.
                GameView v = new GameView(new android.app.Activity());
                int[] size = {1080, 2000};
                v.layout(size[0], size[1]);
                Menu menu = (Menu) field(v, "menu");
                Settings st = (Settings) field(v, "settings");
                st.set(22, 1);
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                c.v[CityConfig.OPT_ZOMBIES] = 3;
                c.seed = 51;
                v.startGame(c);
                v.awaitLoad();
                for (int k = 0; k < 3; k++) draw(v, size);
                menu.screen = Menu.NONE;
                World w = (World) field(v, "world");
                for (int k = 0; k < 12; k++) draw(v, size);
                Sound snd = (Sound) field(v, "sound");
                check(snd.loopGain[Sfx.LOOP_CITY] > 0, "the city's hum under the game");
                w.emit(Sfx.RIFLE, w.city.worldW() / 2 + 2000, w.city.worldH() / 2);
                java.lang.reflect.Field cam = GameView.class.getDeclaredField("camX");
                cam.setAccessible(true);
                v.jumpTo(w.city.worldW() / 2, w.city.worldH() / 2);
                w.emit(Sfx.RIFLE, w.city.worldW() / 2 + 700, w.city.worldH() / 2);
                draw(v, size);
                java.util.List<?> caps = (java.util.List<?>) field(v, "captions");
                check(!caps.isEmpty(), "a caption for gunfire off the screen");
                // The Sound Lab draws, and plays what's tapped.
                menu.open(Menu.SOUNDS);
                draw(v, size);
                java.lang.reflect.Method act = Menu.class.getDeclaredMethod("act", int.class);
                act.setAccessible(true);
                act.invoke(menu, 6000 + Sfx.PANIC);
                check(snd.lastPlayed == Sfx.PANIC, "the Sound Lab plays a sound");
                java.lang.reflect.Field pid = Menu.class.getDeclaredField("previewId");
                pid.setAccessible(true);
                check(pid.getInt(menu) == Sfx.PANIC, "and shows which one's playing");
                act.invoke(menu, 7000 + Sfx.LOOP_BIRDS);
                draw(v, size);
                check(snd.loopGain[Sfx.LOOP_BIRDS] > 0.3f, "and an ambience bed");
                check(v.drawErrors == 0, "drawn without an error (" + v.drawErrors + ")");
            }
        });
        test("19.5: the Visuals Update - light, blasts, craters, smoke, wrecks burning, the screen's effects", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 61;
                World w = new World(c);
                w.populate(c);
                Visuals fx = new Visuals();
                float cx = w.city.worldW() / 2, cy = w.city.worldH() / 2;
                float vx0 = cx - 400, vy0 = cy - 400, vx1 = cx + 400, vy1 = cy + 400;
                // A blast on the screen: light, a shockwave, a crater, debris, sparks, smoke, a ping on the map.
                w.evCount = 0;
                w.emit(Sfx.EXPLOSION, cx, cy);
                fx.events(w, vx0, vy0, vx1, vy1);
                check(!fx.lights.isEmpty() && !fx.waves.isEmpty() && fx.craters.size() == 1 && !fx.debris.isEmpty() && !fx.sparks.isEmpty()
                        && !fx.smoke.isEmpty() && fx.pings.size() == 1, "a blast's light, shockwave, crater, debris, sparks, smoke and ping");
                check(fx.flash > 0, "a flash when it's in the middle of the screen");
                // Far off the screen: only the ping.
                w.evCount = 0;
                w.emit(Sfx.EXPLOSION, cx + 3000, cy);
                int before = fx.count();
                fx.events(w, vx0, vy0, vx1, vy1);
                check(fx.count() == before && fx.pings.size() == 2, "one off the screen: just its ping on the map");
                // A while on: the flash and light gone, the crater kept, debris come down.
                for (int k = 0; k < 90; k++) fx.update(w, 1 / 30f, vx0, vy0, vx1, vy1, null);
                check(fx.flash == 0 && fx.waves.isEmpty() && fx.craters.size() == 1, "the flash and shockwave gone, the crater stays");
                boolean landed = true;
                for (float[] d : fx.debris) landed &= d[2] <= 0;
                check(landed, "the debris has come down");
                // A wreck burns.
                Fleet.Vehicle car = w.fleet.vehicles.isEmpty() ? null : w.fleet.vehicles.get(0);
                if (car != null) {
                    car.broken = true;
                    fx.lights.clear();
                    fx.sparks.clear();
                    float wx0 = car.x - 400, wy0 = car.y - 400;
                    for (int k = 0; k < 60; k++) fx.update(w, 1 / 30f, wx0, wy0, wx0 + 800, wy0 + 800, null);
                    check(!fx.sparks.isEmpty() || !fx.lights.isEmpty(), "a wreck burning");
                }
                // Low effects: far less.
                Visuals low = new Visuals();
                low.quality = 0;
                w.evCount = 0;
                for (int k = 0; k < 20; k++) w.emit(Sfx.EXPLOSION, cx + k, cy);
                low.events(w, vx0, vy0, vx1, vy1);
                Visuals high = new Visuals();
                high.events(w, vx0, vy0, vx1, vy1);
                check(low.count() < high.count() / 2, "Low draws far less (" + low.count() + " / " + high.count() + ")");
                // A new city: nothing carried over.
                World w2 = new World(c);
                w2.evCount = 0;
                fx.events(w2, vx0, vy0, vx1, vy1);
                check(fx.craters.isEmpty() && fx.count() == 0, "a new city starts clean");
                // Drawn, all of it: the ground, above, and the screen (with and without the bars).
                android.graphics.Bitmap bm = android.graphics.Bitmap.createBitmap(800, 800, android.graphics.Bitmap.Config.ARGB_8888);
                android.graphics.Canvas cv = new android.graphics.Canvas(bm);
                fx.events(w, vx0, vy0, vx1, vy1);
                w.evCount = 0;
                w.emit(Sfx.EXPLOSION, cx, cy);
                fx.events(w, vx0, vy0, vx1, vy1);
                fx.drawGround(cv, vx0, vy0, vx1, vy1);
                fx.drawAbove(cv, vx0, vy0, vx1, vy1);
                fx.hurt = 1;
                fx.daze = 1;
                for (int m = 0; m < 2; m++) fx.drawScreen(cv, 800, 800, 0.9f, m == 0);
                check(true, "drawn");
            }
        });
        test("20.0: flares draw the dead, mines go off under them, both kept in a save and drawn", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 71;
                World w = new World(c);
                w.setMaxPopulation(3000);
                w.populate(c);
                w.update(1 / 30f);
                Entity cop = null;
                for (Entity e : w.entities) if (!e.dead && e.type == Entity.COP && e.ride == null && w.city.insideOf(e.x, e.y) == null) { cop = e; break; }
                check(cop != null, "a cop on the street");
                // A flare: the dead are drawn to it, and it burns out.
                float fx = cop.x, fy = cop.y;
                Entity z = w.spawn(Entity.ZOMBIE, fx + 150, fy);
                if (z == null) z = w.spawn(Entity.ZOMBIE, fx - 150, fy);
                check(z != null, "one of the dead near");
                for (Entity e : w.entities) if (!e.isZombie()) e.ammo = e.reserve = 0;
                w.flare(fx, fy);
                check(w.flares.size() == 1 && w.flaresFired == 1, "a flare burning");
                boolean lured = false;
                for (int f = 0; f < 90; f++) {
                    z.memory = 0;
                    z.hp = z.maxHp;
                    w.update(1 / 30f);
                    w.evCount = 0;
                    lured |= z.noiseTimer > 0 && World.hypot(z.noiseX - fx, z.noiseY - fy) < 60;
                }
                check(lured, "the dead are drawn to it");
                for (int f = 0; f < 30 * (int) World.FLARE_LIFE; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(w.flares.isEmpty(), "and it burns out");
                // Mines: armed after a moment, set off by the dead and not by the living.
                for (Entity e : w.entities) if (e.isZombie()) e.hp = 0;
                int laid = w.layMines(fx, fy);
                check(laid > 0 && w.mines.size() == laid, "mines laid (" + laid + ")");
                float[] m0 = w.mines.get(0);
                Entity walker = w.spawn(Entity.CIVILIAN, m0[0], m0[1]);
                for (int f = 0; f < 30 * 5; f++) {
                    if (walker != null) { walker.x = m0[0]; walker.y = m0[1]; }
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(w.mines.size() == laid && w.minesBlown == 0, "the living don't set them off");
                for (Entity e : w.entities) if (!e.isZombie()) e.ammo = e.reserve = 0;
                Entity zm = w.spawn(Entity.ZOMBIE, m0[0] + 2, m0[1]);
                zm.hp = zm.maxHp = 1e5f;
                for (int f = 0; f < 60 && w.minesBlown == 0; f++) {
                    zm.x = m0[0] + 2;
                    zm.y = m0[1];
                    zm.hp = zm.maxHp;
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                for (int f = 0; f < 20; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(w.minesBlown == 1 && w.mines.size() == laid - 1 && (zm.dead || zm.hp < zm.maxHp), "one of the dead does (" + w.minesBlown + " blown, "
                        + w.mines.size() + " of " + laid + " left)");
                // Saved and loaded.
                w.flare(fx, fy);
                w.director.nextHorde = 123;
                File file = File.createTempFile("zcs", ".dat");
                SaveGame.save(w, file);
                World l = SaveGame.load(file);
                file.delete();
                check(l.city.cfg.difficulty == Director.NORMAL && l.flares.size() == 1 && l.mines.size() == w.mines.size() && l.minesBlown == 1
                        && l.director.nextHorde == 123, "flares, mines and the director are kept in a save");
                // Drawn.
                Visuals fx2 = new Visuals();
                android.graphics.Bitmap bm = android.graphics.Bitmap.createBitmap(600, 600, android.graphics.Bitmap.Config.ARGB_8888);
                android.graphics.Canvas cv = new android.graphics.Canvas(bm);
                fx2.events(w, fx - 300, fy - 300, fx + 300, fy + 300);
                fx2.drawAbove(cv, fx - 300, fy - 300, fx + 300, fy + 300);
                check(true, "drawn");
                // The tools: named, described and priced.
                java.lang.reflect.Field names = GameView.class.getDeclaredField("EVENT_NAMES"), info = GameView.class.getDeclaredField("EVENT_INFO");
                names.setAccessible(true);
                info.setAccessible(true);
                String[] en = (String[]) names.get(null), ei = (String[]) info.get(null);
                check(en.length >= 9 && ei.length == en.length && Campaign.EVENT_COST.length == en.length && "Flare".equals(en[7]) && "Mines".equals(en[8]),
                        "flares and mines among the events");
                check(Campaign.EVENT_COST[7] > 0 && Campaign.EVENT_COST[8] > 0, "a commander pays for them");
            }
        });
        test("20.0: a mission's difficulty and the director's hordes - and none of either in the sandbox; the score", new Check() {
            public void run() throws Exception {
                // The sandbox: always Normal, and nothing comes in by itself however long it goes on.
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_SIZE] = CityConfig.TINY;
                c.v[CityConfig.OPT_ZOMBIES] = 3;
                c.difficulty = Director.NIGHTMARE;
                c.seed = 73;
                World sb = new World(c);
                sb.populate(c);
                check(sb.difficulty == Director.NORMAL && sb.director.level == Director.NORMAL, "a sandbox game is always Normal");
                Entity sz = null;
                for (Entity e : sb.entities) if (e.type == Entity.ZOMBIE) { sz = e; break; }
                float normalHp = sz.maxHp;
                int zs = sb.zombieCount(), crates = sb.pickups.size();
                for (int t = 0; t < 1500; t++) {
                    sb.outbreak = true;
                    sb.outbreakTime = 400 + t;
                    sb.warBalance = 0.1f;
                    sb.director.stress = 0;
                    sb.director.tick(sb);
                }
                sb.recount();
                check(sb.director.hordes == 0 && sb.director.stragglers == 0 && sb.director.sieges == 0 && sb.zombieCount() == zs && sb.pickups.size() == crates
                        && sb.incoming.isEmpty(), "nothing comes into a sandbox game by itself: no hordes, stragglers, sieges or supplies");
                // The director still follows how it's going there (for the line at the top of the screen).
                boolean peaked = false, rested = false;
                sb.director.tick(sb);
                check(sb.director.phase == Director.BUILD, "building");
                for (int t = 0; t < 60; t++) {
                    sb.civiliansLost += 6;
                    sb.director.tick(sb);
                    peaked |= sb.director.phase == Director.PEAK;
                }
                check(peaked, "a peak when people are dying fast");
                for (int t = 0; t < 120; t++) {
                    sb.director.stress = Math.min(sb.director.stress, 0.3f);
                    sb.director.tick(sb);
                    rested |= sb.director.phase == Director.RESPITE;
                }
                check(rested && sb.director.phase == Director.BUILD, "then a respite, then building again");
                // A mission on Hard (Overrun): tougher dead, stragglers, a horde, in time a siege.
                CityConfig mc = Campaign.config(5);
                World w = new World(mc);
                w.setMaxPopulation(2400);
                w.populate(mc);
                Campaign.begin(w, "");
                check(w.difficulty == Director.HARD && w.director.level == Director.HARD, "a mission has its own difficulty");
                Entity mz = null;
                for (Entity e : w.entities) if (e.type == Entity.ZOMBIE) { mz = e; break; }
                check(mz.maxHp > normalHp * 1.1f, "the dead are tougher on Hard (" + normalHp + " -> " + mz.maxHp + ")");
                int before = w.zombieCount();
                for (int t = 0; t < 700 && (w.director.hordes == 0 || w.director.sieges == 0 || w.director.stragglers == 0); t++) {
                    w.outbreak = true;
                    w.director.stress = 0;
                    w.director.tick(w);
                }
                w.recount();
                check(w.director.stragglers >= 1 && w.director.hordes >= 1 && w.director.sieges >= 1 && w.zombieCount() > before,
                        "stragglers, a horde and a siege come in (" + w.director.stragglers + ", " + w.director.hordes + ", " + w.director.sieges + ")");
                // Kept through a save, and a sandbox save that says Hard loads as Normal.
                File file = File.createTempFile("zcs", ".dat");
                SaveGame.save(w, file);
                World l = SaveGame.load(file);
                file.delete();
                check(l.difficulty == Director.HARD && l.director.level == Director.HARD && l.mission() == w.mission(), "a mission's difficulty is kept in a save");
                sb.city.cfg.difficulty = Director.HARD;
                file = File.createTempFile("zcs", ".dat");
                SaveGame.save(sb, file);
                World sl = SaveGame.load(file);
                file.delete();
                check(sl.difficulty == Director.NORMAL && sl.director.level == Director.NORMAL, "a sandbox save loads as Normal");
                // The score: kills count, and more in a harder mission.
                sb.zombiesKilled = w.zombiesKilled = 100;
                sb.civiliansLost = w.civiliansLost = 0;
                sb.copsLost = w.copsLost = sb.soldiersLost = w.soldiersLost = 0;
                sb.alphasKilled = w.alphasKilled = 0;
                sb.warResult = w.warResult = 0;
                check(sb.score() == 1000 && w.score() > sb.score(), "a hundred kills score, more on Hard (" + sb.score() + ", " + w.score() + ")");
                // No difficulty to choose on the New Game screen.
                boolean offered = false;
                CityConfig oc = new CityConfig();
                for (int i = 0; i < oc.count(); i++) offered |= oc.label(i).toLowerCase().contains("difficult");
                check(!offered && oc.difficulty == Director.NORMAL && oc.copy().difficulty == Director.NORMAL, "no difficulty on the New Game screen");
            }
        });
        test("20.0: the campaign's missions and their objectives", new Check() {
            public void run() throws Exception {
                check(Campaign.MISSIONS.length >= 8, "the missions");
                for (int i = 0; i < Campaign.MISSIONS.length; i++) {
                    Campaign.Mission m = Campaign.MISSIONS[i];
                    CityConfig mc = Campaign.config(i);
                    check(mc.mission == i && mc.difficulty == m.difficulty && mc.exodus == m.exodus, m.title + ": the mission's rules");
                    check(m.goal >= 0 && m.goal < Campaign.GOAL_NAMES.length, m.title + " has an objective");
                }
                // One small city stands in for each counted objective.
                CityConfig c = Campaign.config(5);
                World w = new World(c);
                w.setMaxPopulation(1800);
                w.populate(c);
                Campaign.begin(w, "");
                check(w.difficulty == Director.HARD, "Overrun is on Hard");
                for (int id : new int[]{3, 6}) {
                    Campaign.Mission m = Campaign.MISSIONS[id];
                    w.city.cfg.mission = id;
                    w.missionResult = 0;
                    w.missionStart = w.time;
                    w.fled = 0;
                    w.zombiesKilled = 0;
                    check(w.mission() == m && !Campaign.goalText(w).isEmpty() && Campaign.progress(w).startsWith("0 / " + m.goalN), m.title + " says what it wants");
                    Campaign.tick(w);
                    check(w.missionResult == 0, m.title + ": not won yet");
                    if (m.goal == Campaign.G_EVAC) w.fled = m.goalN;
                    else w.zombiesKilled = m.goalN;
                    Campaign.tick(w);
                    check(w.missionResult == 1 && w.missionStars >= 1, m.title + ": won on its objective (" + w.missionResult + ")");
                }
                // And they run out of time like the rest.
                w.city.cfg.mission = 6;
                w.missionResult = 0;
                w.zombiesKilled = 0;
                w.missionStart = w.time - Campaign.MISSIONS[6].limit - 1;
                Campaign.tick(w);
                check(w.missionResult == 2, "out of time");
                // The campaign screen draws with all of them.
                GameView v = new GameView(new android.app.Activity());
                int[] size = {1080, 2000};
                v.layout(size[0], size[1]);
                Menu menu = (Menu) field(v, "menu");
                for (int i = 0; i < 3; i++) draw(v, size);
                menu.open(Menu.CAMPAIGN);
                java.lang.reflect.Field sc = Menu.class.getDeclaredField("scroll");
                sc.setAccessible(true);
                for (int i = 0; i < 4; i++) {
                    sc.setFloat(menu, i * 500);
                    draw(v, size);
                }
                menu.open(Menu.SETUP);
                for (int i = 0; i < 5; i++) {
                    sc.setFloat(menu, i * 500);
                    draw(v, size);
                }
                check(v.drawErrors == 0, "the campaign and setup screens draw (" + v.drawErrors + ")");
            }
        });
        test("21.0: razor wire tangles and kills the dead and wears out; machine-gun nests; the mortar barrage; saves", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_SIZE] = CityConfig.TINY;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 81;
                World w = new World(c);
                w.populate(c);
                w.update(1 / 30f);
                Entity cop = null;
                for (Entity e : w.entities) if (!e.dead && e.type == Entity.COP && e.ride == null && w.city.insideOf(e.x, e.y) == null) { cop = e; break; }
                check(cop != null, "a cop on the street");
                float sx = cop.x, sy = cop.y;
                // (Nobody about to get in the way.)
                for (Entity e : w.entities) if (!e.isZombie() && World.hypot(e.x - sx, e.y - sy) < 500) { e.removed = true; e.dead = true; }
                for (Fleet.Vehicle fv : new java.util.ArrayList<Fleet.Vehicle>(w.fleet.vehicles)) if (World.hypot(fv.x - sx, fv.y - sy) < 500) w.fleet.discard(fv);
                w.update(1 / 30f);
                // Wire: one coil to a tile.
                float[] coil = w.layWire(sx, sy);
                check(coil != null && w.defences.wires.size() == 1 && w.defences.wireAt(w, sx, sy) && coil[2] == Defences.WIRE_HP, "a coil laid");
                check(w.layWire(sx, sy) == null, "not two on a tile");
                // One of the dead in it: slowed to a third, torn at, and the coil wears.
                Entity z = w.spawn(Entity.ZOMBIE, coil[0], coil[1]);
                z.x = coil[0];
                z.y = coil[1];
                float hp = z.hp;
                float pace = w.defences.snag(w, z, 0.5f);
                check(pace == Defences.WIRE_SLOW && Math.abs(hp - z.hp - Defences.WIRE_HURT * 0.5f) < 0.01f && coil[2] < Defences.WIRE_HP, "tangled: slowed and hurt (" + pace + ")");
                z.x = coil[0] + City.T * 3;
                check(w.defences.snag(w, z, 0.5f) == 1, "and not off the wire");
                // Walking through it kills them in the end.
                z.x = coil[0];
                for (int f = 0; f < 30 * 20 && !z.dead; f++) {
                    z.x = coil[0];
                    z.y = coil[1];
                    z.noiseX = coil[0] + 400;
                    z.noiseY = coil[1];
                    z.noiseTimer = 10;
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(z.dead && w.defences.wireKills == 1, "one of the dead dies in the wire (" + z.hp + ")");
                // And it wears out.
                Entity brute = w.spawn(Entity.BRUTE, coil[0], coil[1]);
                for (int k = 0; k < 400 && !w.defences.wires.isEmpty(); k++) {
                    brute.x = coil[0];
                    brute.y = coil[1];
                    brute.hp = brute.maxHp;
                    w.defences.snag(w, brute, 0.1f);
                }
                check(w.defences.wires.isEmpty() && !w.defences.wireAt(w, sx, sy), "worn through");
                brute.hp = 0;
                // A machine-gun nest: two gunners who hold, sandbags, ammunition.
                int walls = w.barriers.size(), crates = w.pickups.size();
                java.util.ArrayList<Entity> crew = w.mgNest(sx, sy);
                check(crew.size() == 2 && crew.get(0).role == Entity.ROLE_GUNNER && crew.get(0).task == Dispatch.T_HOLD, "a crew of two gunners holding");
                check(w.barriers.size() >= walls + 3 && w.pickups.size() == crates + 1 && w.defences.nests == 1, "sandbags and a crate (" + (w.barriers.size() - walls) + " bags)");
                float px = crew.get(0).postX, py = crew.get(0).postY;
                for (int f = 0; f < 30 * 6; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(!crew.get(0).dead && World.hypot(crew.get(0).x - px, crew.get(0).y - py) < 20, "they stay at the gun");
                // The barrage.
                float bx = sx + 260, by = sy;
                float[] open = w.city.findWalkable(bx, by);
                if (open != null) { bx = open[0]; by = open[1]; }
                java.util.ArrayList<Entity> pack = new java.util.ArrayList<Entity>();
                for (int k = 0; k < 8; k++) {
                    Entity p = w.spawn(Entity.ZOMBIE, bx + (k % 3) * 6 - 6, by + (k / 3) * 6 - 6);
                    if (p != null) pack.add(p);
                }
                float[][] at = new float[pack.size()][];
                for (int k = 0; k < pack.size(); k++) at[k] = new float[]{pack.get(k).x, pack.get(k).y};
                w.barrage(bx, by);
                for (int f = 0; f < 30 * 8; f++) {
                    for (int k = 0; k < pack.size(); k++) if (!pack.get(k).dead) { pack.get(k).x = at[k][0]; pack.get(k).y = at[k][1]; }
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                int down = 0;
                for (Entity p : pack) if (p.dead || p.hp < p.maxHp) down++;
                check(w.barrages == 1 && down >= pack.size() / 2, "a barrage lands on them (" + down + " of " + pack.size() + " hit)");
                // An airborne squad dropped in, holding the spot.
                java.util.ArrayList<Entity> drop = w.airborne(sx, sy);
                int gunners = 0;
                for (Entity e : drop) if (e.role == Entity.ROLE_GUNNER) gunners++;
                check(drop.size() >= 5 && gunners == 1 && drop.get(1).task == Dispatch.T_MOVE || drop.get(1).task == Dispatch.T_HOLD, "an airborne squad lands (" + drop.size() + ")");
                // Engineers wire the streets into a safe zone, a coil at a time.
                w.dispatch.orderZone(sx, sy);
                check(!w.dispatch.zones.isEmpty(), "a safe zone");
                Dispatch.SafeZone zone = w.dispatch.zones.get(0);
                Entity eng = w.spawnSoldier(Entity.ROLE_ENGINEER, zone.x, zone.y);
                java.lang.reflect.Method ew = World.class.getDeclaredMethod("engineersWire");
                ew.setAccessible(true);
                java.lang.reflect.Field wcd = World.class.getDeclaredField("wireCd");
                wcd.setAccessible(true);
                int wiresBefore = w.defences.wires.size();
                w.outbreak = true;
                w.update(1 / 30f);
                for (int k = 0; k < 40; k++) {
                    eng.x = zone.x;
                    eng.y = zone.y;
                    w.outbreak = true;
                    wcd.setFloat(w, 0);
                    ew.invoke(w);
                }
                int zoneWire = w.defences.wires.size() - wiresBefore;
                check(zoneWire > 0 && zoneWire <= World.ZONE_WIRE, "engineers wire the zone's approaches, up to ten coils (" + zoneWire + ")");
                // The situation, on the radio.
                java.lang.reflect.Method sit = World.class.getDeclaredMethod("sitrep");
                sit.setAccessible(true);
                int said = w.dispatch.messageCount;
                w.outbreakTime = 300;
                sit.invoke(w);
                check(w.dispatch.messageCount > said, "a situation report");
                // Kept in a save; cleared with the barricades.
                w.layWire(sx + City.T * 4, sy);
                w.layWire(sx + City.T * 5, sy);
                int coils = w.defences.wires.size();
                check(coils >= 1, "more wire down");
                w.defences.wires.get(0)[2] = 123;
                File file = File.createTempFile("zcs", ".dat");
                SaveGame.save(w, file);
                World l = SaveGame.load(file);
                file.delete();
                check(l.defences.wires.size() >= coils - 2 && l.defences.wires.size() <= coils && l.defences.wires.get(0)[2] == 123 && l.defences.wireKills == w.defences.wireKills && l.defences.nests == 1
                        && l.barrages == 1 && l.defences.wireAt(l, w.defences.wires.get(0)[0], w.defences.wires.get(0)[1]), "wire and the counts are kept in a save (" + l.defences.wires.size() + " of " + coils + " coils, first "
                        + (l.defences.wires.isEmpty() ? -1 : l.defences.wires.get(0)[2]) + ", kills " + l.defences.wireKills + "/" + w.defences.wireKills + ", nests " + l.defences.nests + ", barrages " + l.barrages + ")");
                w.clearBarricades();
                check(w.defences.wires.isEmpty() && w.barriers.isEmpty(), "cleared with the barricades");
                // Drawn.
                w.layWire(sx, sy);
                Visuals fx = new Visuals();
                android.graphics.Bitmap bm = android.graphics.Bitmap.createBitmap(600, 600, android.graphics.Bitmap.Config.ARGB_8888);
                android.graphics.Canvas cv = new android.graphics.Canvas(bm);
                fx.events(w, sx - 300, sy - 300, sx + 300, sy + 300);
                fx.drawGround(cv, sx - 300, sy - 300, sx + 300, sy + 300);
                // The tools: named, described and priced.
                for (String[] pair : new String[][]{{"PLACE_NAMES", "PLACE_INFO"}, {"EVENT_NAMES", "EVENT_INFO"}}) {
                    java.lang.reflect.Field names = GameView.class.getDeclaredField(pair[0]), info = GameView.class.getDeclaredField(pair[1]);
                    names.setAccessible(true);
                    info.setAccessible(true);
                    String[] n = (String[]) names.get(null), i = (String[]) info.get(null);
                    int[] cost = pair[0].startsWith("PLACE") ? Campaign.PLACE_COST : Campaign.EVENT_COST;
                    check(n.length == i.length && n.length == cost.length, pair[0] + ": every tool named, described and priced (" + n.length + ", " + i.length + ", " + cost.length + ")");
                }
                check(Campaign.PLACE_COST[9] > 0 && Campaign.PLACE_COST[10] > 0 && Campaign.EVENT_COST[9] > 0, "a commander pays for wire, nests and barrages");
            }
        });
        test("21.0: an alpha leads a big horde and it scatters when the alpha falls; marksmen go for it; the last of the dead are found", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_SIZE] = CityConfig.TINY;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 82;
                World w = new World(c);
                w.populate(c);
                w.update(1 / 30f);
                Entity cop = null;
                for (Entity e : w.entities) if (!e.dead && e.type == Entity.COP && e.ride == null && w.city.insideOf(e.x, e.y) == null) { cop = e; break; }
                float sx = cop.x, sy = cop.y;
                // (Nobody about to get in the way.)
                for (Entity e : w.entities) if (!e.isZombie() && World.hypot(e.x - sx, e.y - sy) < 500) { e.removed = true; e.dead = true; }
                for (Fleet.Vehicle fv : new java.util.ArrayList<Fleet.Vehicle>(w.fleet.vehicles)) if (World.hypot(fv.x - sx, fv.y - sy) < 500) w.fleet.discard(fv);
                w.update(1 / 30f);
                // A horde of thirteen: its leader becomes an alpha.
                java.util.ArrayList<Entity> horde = new java.util.ArrayList<Entity>();
                for (int k = 0; k < 13; k++) {
                    Entity z = w.spawn(Entity.ZOMBIE, sx + (k % 4) * 7, sy + (k / 4) * 7);
                    if (z != null) horde.add(z);
                }
                check(horde.size() >= World.ALPHA_HORDE, "a horde's worth");
                Entity lead = horde.get(0);
                lead.leadsHorde = true;
                for (int k = 1; k < horde.size(); k++) horde.get(k).hordeLeader = lead;
                float before = lead.maxHp;
                java.lang.reflect.Method th = World.class.getDeclaredMethod("thinkHorde", Entity.class);
                th.setAccessible(true);
                // (A leader of a while, with a fresh count of its horde.)
                lead.leadSeen = true;
                lead.leadStart = -1000;
                java.lang.reflect.Field ta = World.class.getDeclaredField("tallyAt");
                ta.setAccessible(true);
                ta.setInt(w, -1);
                th.invoke(w, lead);
                check(lead.alpha && lead.maxHp > before * 2 && lead.hp == lead.maxHp && w.alphas == 1, "the leader of a big horde is an alpha, far tougher (" + before + " -> " + lead.maxHp + ")");
                th.invoke(w, lead);
                check(w.alphas == 1, "once");
                check(!horde.get(1).alpha, "the rest aren't");
                // Marksmen go for the alpha before nearer ones.
                Entity sniper = w.spawnSoldier(Entity.ROLE_SNIPER, sx - 60, sy);
                sniper.ammo = sniper.reserve = 0;
                Entity near = horde.get(1);
                java.lang.reflect.Method pick = World.class.getDeclaredMethod("pickTarget", Entity.class, float.class);
                pick.setAccessible(true);
                w.update(1 / 30f);
                w.evCount = 0;
                sniper.fear = 1;
                Entity chosen = (Entity) pick.invoke(w, sniper, 400f);
                boolean seesAlpha = w.city.los(sniper.x, sniper.y, lead.x, lead.y);
                check(!seesAlpha || chosen == lead, "a sniper picks the alpha out of the horde (" + (chosen == null ? "nobody" : "one at " + (int) World.hypot(chosen.x - sniper.x, chosen.y - sniper.y))
                        + ", alpha at " + (int) World.hypot(lead.x - sniper.x, lead.y - sniper.y) + " dead " + lead.dead + " hidden " + lead.hidden + ")");
                // Fire discipline: with no alpha to go for, a second gun takes a different one of the crowd.
                lead.alpha = false;
                Entity s2 = w.spawnSoldier(Entity.ROLE_RIFLE, sniper.x, sniper.y + 4), s3 = w.spawnSoldier(Entity.ROLE_RIFLE, sniper.x, sniper.y - 4);
                s2.ammo = s2.reserve = s3.ammo = s3.reserve = 0;
                s2.fear = s3.fear = 1;
                w.time += 2;
                Entity first = (Entity) pick.invoke(w, s2, 400f), second = (Entity) pick.invoke(w, s3, 400f);
                check(first == null || second == null || first != second, "two guns side by side pick different ones of a crowd");
                lead.alpha = true;
                // Kept in a save.
                File file = File.createTempFile("zcs", ".dat");
                SaveGame.save(w, file);
                World l = SaveGame.load(file);
                file.delete();
                int la = 0;
                for (Entity e : l.entities) if (e.alpha) la++;
                check(la == 1 && l.alphas == 1, "the alpha's kept in a save");
                // The alpha falls: the horde scatters.
                for (int k = 1; k < horde.size(); k++) horde.get(k).hordeLeader = lead;
                lead.hp = 0;
                w.update(1 / 30f);
                w.evCount = 0;
                int scattered = 0;
                for (int k = 1; k < horde.size(); k++) if (horde.get(k).hordeLeader == null && horde.get(k).scatterT > 0) scattered++;
                check(lead.dead && w.alphasKilled == 1 && scattered >= horde.size() - 3, "the alpha's dead and its horde scatters (" + scattered + ")");
                boolean said = false;
                for (World.Moment m : w.timeline) said |= m.text.startsWith("An alpha is killed");
                check(said, "and it's on the timeline");
                Entity loose = horde.get(2);
                th.invoke(w, loose);
                check(loose.hordeLeader == null && !loose.leadsHorde, "the scattered don't form up again at once");
                int sc = w.score();
                w.alphasKilled++;
                check(w.score() >= sc, "alphas score");
                // The mop-up: down to a handful, each one's whereabouts is known.
                java.lang.reflect.Method mop = World.class.getDeclaredMethod("mopUp");
                mop.setAccessible(true);
                w.outbreak = true;
                w.outbreakTime = 300;
                w.recount();
                check(w.zombieCount() > 0 && w.zombieCount() <= World.MOP_UP, "a handful left (" + w.zombieCount() + ")");
                mop.invoke(w);
                check(w.mopping && w.lastDead.length == w.zombieCount() * 2, "the last of them are found (" + w.lastDead.length / 2 + ")");
                boolean marked = true;
                for (Entity e : w.entities) if (e.isZombie() && !e.dead && e.hp > 0) marked &= e.spottedUntil > w.time;
                check(marked, "and marked for the units");
                for (Entity e : w.entities) if (e.isZombie()) e.hp = 0;
                w.update(1 / 30f);
                w.recount();
                mop.invoke(w);
                check(!w.mopping && w.lastDead.length == 0, "nothing to point at once they're gone");
                // One wedged inside a wall is got out, in the mop-up at once and anywhere else within twenty seconds.
                City.Building wall = null;
                for (City.Building b : w.city.buildings) {
                    int ti = w.city.tileIndex((b.x0 + b.x1) / 2, (b.y0 + b.y1) / 2);
                    if (!b.collapsed && ti >= 0 && w.city.solid[ti] && !w.city.enterable(b) && w.city.solidAt((b.x0 + b.x1) / 2, (b.y0 + b.y1) / 2)) { wall = b; break; }
                }
                check(wall != null, "a solid building");
                Entity stuck = w.spawn(Entity.ZOMBIE, sx, sy);
                stuck.x = (wall.x0 + wall.x1) / 2;
                stuck.y = (wall.y0 + wall.y1) / 2;
                java.lang.reflect.Method un = World.class.getDeclaredMethod("unstick");
                un.setAccessible(true);
                java.lang.reflect.Field ucd = World.class.getDeclaredField("unstickCd");
                ucd.setAccessible(true);
                for (int k = 0; k < 4; k++) {
                    check(k == 0 || w.unstuck == 0, "not at the first look");
                    ucd.setFloat(w, 0);
                    un.invoke(w);
                }
                int st = w.city.tileIndex(stuck.x, stuck.y);
                check(w.unstuck == 1 && !w.city.solidAt(stuck.x, stuck.y), "one of the dead stuck in a wall is put out in the open");
                stuck.x = (wall.x0 + wall.x1) / 2;
                stuck.y = (wall.y0 + wall.y1) / 2;
                w.outbreak = true;
                w.outbreakTime = 300;
                w.recount();
                mop.invoke(w);
                check(!w.city.solidAt(stuck.x, stuck.y), "and at once in the mop-up");
                // A horde from outside is pointed out.
                check(w.incoming.isEmpty(), "nothing coming");
                java.util.ArrayList<Entity> in = w.hordeFrom(sx, sy, 9, false);
                check(in.size() >= 5 && w.incoming.size() == 1 && (Integer) w.incoming.get(0)[2] == in.size(), "a horde coming in is pointed out");
                for (Entity e : w.entities) if (e.isZombie()) e.hp = 0;
                w.update(1 / 30f);
                // Not while there are plenty of them.
                for (int k = 0; k < World.MOP_UP + 4; k++) w.spawn(Entity.ZOMBIE, sx + k * 5, sy + 20);
                w.outbreak = true;
                w.recount();
                mop.invoke(w);
                check(!w.mopping, "no mop-up with plenty left");
            }
        });
        test("21.0: commander perks from stars; the twelve missions; holding the station and keeping people alive", new Check() {
            public void run() throws Exception {
                check(Perks.count(0) == 0 && Perks.count(9) == 3 && Perks.next(9) == 12 && Perks.next(24) == -1 && Perks.NAMES.length == Perks.STARS.length
                        && Perks.INFO.length == Perks.STARS.length, "perks by stars");
                check(Campaign.MISSIONS.length == 12, "twelve missions");
                for (Campaign.Mission m : Campaign.MISSIONS) check(m.goal >= 0 && m.goal < Campaign.GOAL_NAMES.length, m.title + " has an objective");
                CityConfig c = Campaign.config(8);
                World w = new World(c);
                w.setMaxPopulation(1800);
                w.populate(c);
                Campaign.Mission m = Campaign.MISSIONS[8];
                w.setMaxPopulation(2400);
                // No perks without stars; all of them with enough.
                Campaign.begin(w, "", 0);
                check(w.perkStars == 0 && !Perks.has(w, Perks.ARMOUR) && Campaign.budget(w) == m.budget && Campaign.placeCost(w) == Campaign.PLACE_COST
                        && Campaign.eventCost(w) == Campaign.EVENT_COST, "no stars, no perks");
                Entity any = null;
                for (Entity e : w.entities) if (!e.dead && e.type == Entity.COP && e.ride == null && w.city.insideOf(e.x, e.y) == null) { any = e; break; }
                Entity plain = w.spawnCop(0, any.x + 6, any.y);
                Campaign.begin(w, "", 24);
                check(Perks.has(w, Perks.OLD_HANDS) && Campaign.budget(w) == m.budget * 6 / 5, "Logistics: a fifth more to start with");
                Entity tough = w.spawnCop(0, any.x - 6, any.y);
                check(plain != null && tough != null && Math.abs(tough.maxHp - plain.maxHp * 1.15f) < 0.5f, "Body armour (" + plain.maxHp + " -> " + tough.maxHp + ")");
                check(Campaign.placeCost(w)[9] == (Campaign.PLACE_COST[9] + 1) / 2 && Campaign.placeCost(w)[1] == Campaign.PLACE_COST[1]
                        && Campaign.eventCost(w)[7] == Campaign.EVENT_COST[7] / 2 && Campaign.eventCost(w)[4] == Campaign.EVENT_COST[4] * 2 / 3
                        && Campaign.eventCost(w)[9] == Campaign.EVENT_COST[9] * 2 / 3, "Quartermaster and Fire support take money off");
                float[] bar = w.placeBarricade(tough.x + City.T * 3, tough.y);
                check(bar == null || bar[2] > 300, "Sappers build tougher");
                // Never in the sandbox.
                w.city.cfg.mission = -1;
                check(!Perks.has(w, Perks.ARMOUR), "perks are for missions only");
                w.city.cfg.mission = 8;
                // Holding the station: lost once the dead have had it, with nobody armed there, for twenty seconds.
                City.Facility f = Campaign.site(w);
                check(f != null && Campaign.goalText(w).startsWith("Hold ") && m.goal == Campaign.G_DEFEND, "a station to hold");
                Campaign.begin(w, "", 0);
                Campaign.tick(w);
                check(w.missionResult == 0 && w.siteT == 0, "holding");
                // (Everyone of the city's gone from there: off the streets, out of the cars, out of the buildings.)
                for (Entity e : w.entities) {
                    if (e.type == Entity.COP || e.type == Entity.SOLDIER || World.hypot(e.x - f.x, e.y - f.y) < 600 && !e.isZombie()) { e.removed = true; e.dead = true; }
                    e.hasGun = false;
                }
                for (Fleet.Vehicle v : new java.util.ArrayList<Fleet.Vehicle>(w.fleet.vehicles)) w.fleet.discard(v);
                for (City.Building b : w.city.buildings) b.occupants.clear();
                w.update(1 / 30f);
                float[] gate = w.city.findWalkable(f.x, f.y);
                java.util.ArrayList<Entity> at = new java.util.ArrayList<Entity>();
                for (int k = 0; k < 9; k++) {
                    Entity z = w.spawn(Entity.ZOMBIE, gate[0] + (k % 3) * 6, gate[1] + (k / 3) * 6);
                    if (z != null) at.add(z);
                }
                float[][] pos = new float[at.size()][];
                for (int k = 0; k < at.size(); k++) pos[k] = new float[]{at.get(k).x, at.get(k).y};
                check(at.size() >= 6, "the dead at the gate");
                for (int f2 = 0; f2 < 30 * 26 && w.missionResult == 0; f2++) {
                    for (int k = 0; k < at.size(); k++) { at.get(k).x = pos[k][0]; at.get(k).y = pos[k][1]; at.get(k).hp = at.get(k).maxHp; }
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(w.missionResult == 2 && w.missionWhy != null && w.missionWhy.endsWith("has fallen."), "the station falls (" + w.missionResult + ", " + w.missionWhy + ", " + w.siteT + ", dead there " + w.countZombiesNear(f.x, f.y, Campaign.reach(f))
                        + ", armed " + w.armedNear(f.x, f.y, Campaign.reach(f) + 30) + ")");
                // Keeping people alive.
                Campaign.Mission pm = Campaign.MISSIONS[9];
                w.city.cfg.mission = 9;
                w.missionResult = 0;
                w.missionStart = w.time - 30;
                w.warResult = 0;
                check(pm.goal == Campaign.G_PROTECT && Campaign.goalText(w).startsWith("Keep " + pm.goalN), "people to keep alive");
                w.humans = pm.goalN + 50;
                Campaign.tick(w);
                check(w.missionResult == 0 && Campaign.progress(w).startsWith((pm.goalN + 50) + " alive"), "enough alive");
                w.missionStart = w.time - pm.limit - 1;
                Campaign.tick(w);
                check(w.missionResult == 1, "and still at the end: won");
                w.missionResult = 0;
                w.missionStart = w.time - 30;
                w.humans = pm.goalN - 1;
                Campaign.tick(w);
                check(w.missionResult == 2 && w.missionWhy.startsWith("Too many lost"), "too many lost");
                // Saved with its perks.
                w.perkStars = 12;
                File file = File.createTempFile("zcs", ".dat");
                SaveGame.save(w, file);
                World l = SaveGame.load(file);
                file.delete();
                check(l.perkStars == 12, "the perks are kept in a save");
                // The campaign screen, with perks earned and one to come.
                GameView v = new GameView(new android.app.Activity());
                int[] size = {1080, 2000};
                v.layout(size[0], size[1]);
                Menu menu = (Menu) field(v, "menu");
                Records rec = (Records) field(v, "records");
                for (int i = 0; i < 4; i++) rec.missionWon(i, 3, "");
                check(rec.totalStars() == 12 && Perks.count(rec.totalStars()) == 4, "twelve stars, four perks");
                for (int i = 0; i < 3; i++) draw(v, size);
                menu.open(Menu.CAMPAIGN);
                java.lang.reflect.Field sc = Menu.class.getDeclaredField("scroll");
                sc.setAccessible(true);
                for (int i = 0; i < 6; i++) {
                    sc.setFloat(menu, i * 500);
                    draw(v, size);
                }
                check(v.drawErrors == 0, "the campaign screen draws with its perks (" + v.drawErrors + ")");
                // In a game: the last of the dead pointed out, an alpha drawn, wire on the ground.
                CityConfig gc = new CityConfig();
                gc.v[CityConfig.OPT_SIZE] = CityConfig.TINY;
                gc.v[CityConfig.OPT_ZOMBIES] = 2;
                gc.seed = 83;
                v.startGame(gc);
                v.awaitLoad();
                for (int i = 0; i < 3; i++) draw(v, size);
                menu.screen = Menu.NONE;
                World gw = (World) field(v, "world");
                Entity gz = null;
                for (Entity e : gw.entities) if (e.isZombie()) { gz = e; break; }
                gz.alpha = true;
                gw.layWire(gz.x + 10, gz.y);
                gw.outbreak = true;
                gw.outbreakTime = 300;
                java.lang.reflect.Method mop = World.class.getDeclaredMethod("mopUp");
                mop.setAccessible(true);
                mop.invoke(gw);
                check(gw.lastDead.length > 0, "the last of them known");
                for (int i = 0; i < 4; i++) draw(v, size);
                v.jumpTo(gz.x, gz.y);
                for (int i = 0; i < 4; i++) draw(v, size);
                check(v.drawErrors == 0, "drawn without an error (" + v.drawErrors + ")");
            }
        });
        test("21.0: police and soldiers fight inside buildings, and can be ordered into one", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_SIZE] = CityConfig.TINY;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 91;
                World w = new World(c);
                w.populate(c);
                w.update(1 / 30f);
                // A building people walk into, and two spots in it in sight of each other.
                City.Building room = null;
                float[] p = null, q = null;
                for (City.Building b : w.city.buildings) {
                    if (!w.city.enterable(b)) continue;
                    p = null;
                    for (float y = b.y0 + 6; y < b.y1 - 6 && q == null; y += 4)
                        for (float x = b.x0 + 6; x < b.x1 - 6 && q == null; x += 4) {
                            if (w.city.insideOf(x, y) != b || w.city.circleBlocked(x, y, 4)) continue;
                            if (p == null) { p = new float[]{x, y}; continue; }
                            double d = Math.hypot(x - p[0], y - p[1]);
                            if (d > 22 && d < 60 && w.city.los(p[0], p[1], x, y)) q = new float[]{x, y};
                        }
                    if (q != null) { room = b; break; }
                }
                check(room != null, "a room to fight in");
                for (int who = 0; who < 2; who++) {
                    Entity gun = who == 0 ? w.spawnCop(0, p[0], p[1]) : w.spawnSoldier(Entity.ROLE_RIFLE, p[0], p[1]);
                    gun.x = p[0];
                    gun.y = p[1];
                    Entity z = w.spawn(Entity.ZOMBIE, q[0], q[1]);
                    z.hp = z.maxHp = 5000;
                    int shots = w.shotsFired;
                    boolean aimed = false;
                    for (int f = 0; f < 150; f++) {
                        z.x = q[0];
                        z.y = q[1];
                        gun.x = p[0];
                        gun.y = p[1];
                        gun.hp = gun.maxHp;
                        w.update(1 / 30f);
                        w.evCount = 0;
                        aimed |= gun.aiming;
                    }
                    check(aimed && w.shotsFired - shots >= 4 && z.hp < z.maxHp, (who == 0 ? "a police officer" : "a soldier") + " indoors opens fire on one of the dead in the room ("
                            + (w.shotsFired - shots) + " shots)");
                    z.hp = 0;
                    gun.removed = true;
                    gun.dead = true;
                    w.update(1 / 30f);
                }
                // With nothing to shoot, they still leave a building they've no business in.
                Entity idle = w.spawnCop(0, p[0], p[1]);
                idle.x = p[0];
                idle.y = p[1];
                for (int f = 0; f < 30 * 30 && w.city.insideOf(idle.x, idle.y) != null; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(w.city.insideOf(idle.x, idle.y) == null, "an officer with nothing to do indoors walks out");
                // Ordered to a spot inside: in by the door, and they stay.
                java.util.ArrayList<Entity> sent = new java.util.ArrayList<Entity>();
                sent.add(idle);
                w.dispatch.order(sent, q[0], q[1]);
                boolean in = false;
                for (int f = 0; f < 30 * 60 && !in; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                    in = w.city.insideOf(idle.x, idle.y) == room && World.hypot(idle.x - idle.postX, idle.y - idle.postY) < 12;
                }
                check(in, "ordered to a spot inside a building, they go in to it (" + (int) World.hypot(idle.x - idle.postX, idle.y - idle.postY) + " away)");
                for (int f = 0; f < 30 * 6; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(w.city.insideOf(idle.x, idle.y) == room, "and stay there");
            }
        });
        test("21.2: smarter fighting - breaking out of a closing ring by the gap, reloading in a lull, shooting on the move, working round a victim", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.v[CityConfig.OPT_CIVILIANS] = 0;
                c.seed = 95;
                World w = new World(c);
                w.populate(c);
                // Open ground with room all round.
                float[] p = null;
                for (int r = 0; r < 4000 && p == null; r++) {
                    float x = 400 + w.rnd.nextFloat() * (w.city.worldW() - 800), y = 400 + w.rnd.nextFloat() * (w.city.worldH() - 800);
                    boolean ok = true;
                    for (int dx = -9; dx <= 9 && ok; dx += 2) for (int dy = -9; dy <= 9 && ok; dy += 2) if (w.city.solidAt(x + dx * City.T, y + dy * City.T)) ok = false;
                    if (ok) p = new float[]{x, y};
                }
                check(p != null, "open ground");
                for (Entity e : w.entities) if (World.hypot(e.x - p[0], e.y - p[1]) < 600) { e.removed = true; e.dead = true; }
                for (Fleet.Vehicle v : new java.util.ArrayList<Fleet.Vehicle>(w.fleet.vehicles)) if (World.hypot(v.x - p[0], v.y - p[1]) < 600) w.fleet.discard(v);
                w.update(1 / 30f);
                // A ring of the dead closing in from every side but one: the gap is to the east.
                Entity cop = w.spawnCop(0, p[0], p[1]);
                java.util.ArrayList<Entity> ring = new java.util.ArrayList<Entity>();
                for (int k = 0; k < 12; k++) {
                    double a = Math.toRadians(70 + k * 20);
                    Entity z = w.spawn(Entity.ZOMBIE, p[0] + (float) Math.cos(a) * 55, p[1] + (float) Math.sin(a) * 55);
                    if (z != null) ring.add(z);
                }
                w.update(1 / 30f);
                w.sense(cop);
                check(cop.tN >= 10 && cop.tSides >= 5 && cop.escX > 0.7f, "the ring read: " + cop.tN + " of them from " + cop.tSides + " sides, the way out east (" + cop.escX + ", " + cop.escY + ")");
                float x0 = cop.x;
                for (int f = 0; f < 30 * 3; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(cop.x - x0 > 30 && !cop.dead, "alone against a dozen, they fall back by the gap (" + (int) (cop.x - x0) + " east)");
                for (Entity z : ring) z.hp = 0;
                w.update(1 / 30f);
                // A squad of six, not outnumbered enough to fall back: the ring closing round them, they break out of it.
                java.util.ArrayList<Entity> squad = new java.util.ArrayList<Entity>();
                for (int k = 0; k < 6; k++) {
                    Entity s6 = w.spawnCop(0, p[0] - 4 + (k % 3) * 4, p[1] - 3 + (k / 3) * 6);
                    s6.ammo = s6.magSize;
                    squad.add(s6);
                }
                ring.clear();
                for (int k = 0; k < 16; k++) {
                    double a = Math.toRadians(70 + k * 14);
                    Entity z = w.spawn(Entity.ZOMBIE, p[0] + (float) Math.cos(a) * 60, p[1] + (float) Math.sin(a) * 60);
                    if (z != null) {
                        z.hp = z.maxHp = 1e4f;
                        ring.add(z);
                    }
                }
                int b0 = w.breakouts;
                float sx0 = 0;
                for (Entity s6 : squad) sx0 += s6.x;
                for (int f = 0; f < 30 * 3; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                float sx1 = 0;
                int up = 0;
                for (Entity s6 : squad) {
                    sx1 += s6.x;
                    if (!s6.dead) up++;
                }
                check(w.breakouts > b0 && (sx1 - sx0) / squad.size() > 25, "a squad being surrounded breaks out by the gap (" + (w.breakouts - b0) + " broke out, "
                        + (int) ((sx1 - sx0) / squad.size()) + " east on average, " + up + " standing)");
                for (Entity z : ring) z.hp = 0;
                for (Entity s6 : squad) { s6.removed = true; s6.dead = true; }
                w.update(1 / 30f);
                // A lull with the gun getting low: reload now.
                cop.ammo = 3;
                cop.reload = 0;
                cop.x = p[0];
                cop.y = p[1];
                Entity far = w.spawn(Entity.ZOMBIE, p[0] + 130, p[1]);
                far.noiseTimer = 0;
                for (int f = 0; f < 10 && cop.reload <= 0 && cop.ammo == 3; f++) {
                    far.x = p[0] + 130;
                    far.y = p[1];
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(cop.reload > 0 || cop.ammo > 3, "a gun nearly empty is reloaded in a lull, not with the dead on them");
                far.hp = 0;
                w.update(1 / 30f);
                // Under orders to move, still shooting what's close on the way. (With the city knowing what this is: before,
                // an officer holds fire on a lone "suspect" a little way off.)
                w.alert = 2;
                java.util.ArrayList<Entity> sent = new java.util.ArrayList<Entity>();
                sent.add(cop);
                cop.ammo = cop.magSize;
                cop.reload = 0;
                w.dispatch.order(sent, p[0] - 300, p[1]);
                Entity near = w.spawn(Entity.ZOMBIE, cop.x - 50, cop.y + 20);
                near.hp = near.maxHp = 1e5f;
                int shots = w.shotsFired;
                for (int f = 0; f < 30 * 2; f++) {
                    near.x = cop.x - 50;
                    near.y = cop.y + 20;
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(cop.task == Dispatch.T_MOVE && w.shotsFired > shots, "on the move under orders, they shoot one of the dead close by (" + (w.shotsFired - shots) + " shots, task " + cop.task + " dead " + cop.dead + " ammo " + cop.ammo
                        + " reload " + cop.reload + " d " + (int) World.hypot(near.x - cop.x, near.y - cop.y) + " los " + w.city.los(cop.x, cop.y, near.x, near.y) + " goal " + cop.goal + " ride " + (cop.ride != null)
                        + " pick " + (pickT(w, cop, 160) == near) + " fear " + cop.fear + " post " + (int) cop.postX + "," + (int) cop.postY + " at " + (int) cop.x + "," + (int) cop.y + " inside " + (w.city.insideOf(cop.x, cop.y) != null) + " postInside " + (w.city.insideOf(cop.postX, cop.postY) != null) + ")");
                near.hp = 0;
                w.update(1 / 30f);
                // One of the dead on someone, with them in the way: it's still the target, and they work round for a shot.
                Entity gun = w.spawnCop(0, p[0], p[1]);
                gun.ammo = gun.magSize;
                Entity victim = w.spawn(Entity.CIVILIAN, p[0] + 40, p[1]);
                Entity biter = w.spawn(Entity.ZOMBIE, p[0] + 48, p[1]);
                biter.hp = biter.maxHp = 1e5f;
                for (int f = 0; f < 5; f++) {
                    victim.x = p[0] + 40;
                    victim.y = p[1];
                    biter.x = p[0] + 48;
                    biter.y = p[1];
                    biter.biteCd = 0.5f;
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                java.lang.reflect.Method pick = World.class.getDeclaredMethod("pickTarget", Entity.class, float.class);
                pick.setAccessible(true);
                gun.fear = 5;
                gun.x = p[0];
                gun.y = p[1];
                gun.angle = 0;
                check(pick.invoke(w, gun, 160f) == biter, "the one biting someone is still the target with them in the line of fire");
                float gy0 = gun.y;
                boolean shot = false;
                for (int f = 0; f < 30 * 2 && !shot; f++) {
                    victim.x = p[0] + 40;
                    victim.y = p[1];
                    biter.x = p[0] + 48;
                    biter.y = p[1];
                    biter.biteCd = 0.5f;
                    victim.hp = victim.maxHp;
                    victim.infected = false;
                    int before = w.shotsFired;
                    w.update(1 / 30f);
                    w.evCount = 0;
                    shot = w.shotsFired > before;
                }
                check(Math.abs(gun.y - gy0) > 2 || shot, "they step round for an angle (" + (gun.y - gy0) + ")");
                check(victim.hp > 0, "and nobody shoots through the one being bitten");
            }
        });
        test("21.3: runners always run; fires drawn under people; the war meter a set size; the dead work out in full off screen", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_SIZE] = CityConfig.TINY;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 96;
                World w = new World(c);
                w.populate(c);
                w.update(1 / 30f);
                // A runner on its own, nothing to chase: still at a run, not a shamble.
                float[] p = w.city.findWalkable(w.city.worldW() / 2, w.city.worldH() / 2);
                Entity r = w.spawn(Entity.RUNNER, p[0], p[1]);
                Entity z = w.spawn(Entity.ZOMBIE, p[0] + 4, p[1]);
                float fastest = 0, slowest = 1e9f, zfast = 0;
                for (int f = 0; f < 30 * 8; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                    float sp = (float) Math.hypot(r.vx, r.vy);
                    if (r.want > 0.5f && f > 30) {
                        fastest = Math.max(fastest, sp);
                        if (sp > 1) slowest = Math.min(slowest, r.want);
                    }
                    zfast = Math.max(zfast, (float) Math.hypot(z.vx, z.vy));
                }
                check(slowest >= r.runSpeed * 0.85f, "a runner moving is always running (wants " + slowest + ", runs at " + r.runSpeed + ")");
                // The dead off screen are worked out every step, like those on screen.
                World.LOD = true;
                check(r.lodStep == 1 || r.dead, "a runner off screen is worked out every step (" + r.lodStep + ")");
                // Fires on the ground are drawn with the ground, under people.
                Visuals fx = new Visuals();
                fx.events(w, p[0] - 300, p[1] - 300, p[0] + 300, p[1] + 300);
                w.evCount = 0;
                w.emit(Sfx.EXPLOSION, p[0], p[1]);
                fx.events(w, p[0] - 300, p[1] - 300, p[0] + 300, p[1] + 300);
                w.evCount = 0;
                for (int k = 0; k < 60; k++) fx.update(w, 1 / 30f, p[0] - 300, p[1] - 300, p[0] + 300, p[1] + 300, null);
                int ground = 0, above = 0;
                for (float[] s2 : fx.sparks) if (s2[8] > 0) ground++; else above++;
                for (float[] l : fx.lights) if (l[8] > 0) ground++;
                check(ground > 0, "where something blew up, the fire after is on the ground (" + ground + " ground, " + above + " in the air)");
                android.graphics.Bitmap bm = android.graphics.Bitmap.createBitmap(600, 600, android.graphics.Bitmap.Config.ARGB_8888);
                android.graphics.Canvas cv = new android.graphics.Canvas(bm);
                fx.drawGround(cv, p[0] - 300, p[1] - 300, p[0] + 300, p[1] + 300);
                fx.drawAbove(cv, p[0] - 300, p[1] - 300, p[0] + 300, p[1] + 300);
                // The war meter: the same size however long the line of figures above it.
                GameView v = new GameView(new android.app.Activity());
                int[] size = {1080, 2000};
                v.layout(size[0], size[1]);
                Menu menu = (Menu) field(v, "menu");
                CityConfig gc = new CityConfig();
                gc.v[CityConfig.OPT_SIZE] = CityConfig.TINY;
                gc.v[CityConfig.OPT_ZOMBIES] = 3;
                gc.seed = 97;
                v.startGame(gc);
                v.awaitLoad();
                for (int i = 0; i < 3; i++) draw(v, size);
                menu.screen = Menu.NONE;
                World gw = (World) field(v, "world");
                gw.outbreak = true;
                draw(v, size);
                android.graphics.RectF meter = (android.graphics.RectF) field(v, "meterRect");
                float dp = (Float) field(v, "dp");
                check(meter.width() > 150 * dp && meter.width() < 260 * dp, "the war meter is a set size (" + meter.width() / dp + " dp)");
                check(v.drawErrors == 0, "drawn without an error");
            }
        });
        test("17.6: gunfights use cover - harder to hit behind it, they move to it, and fire pins them down", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 26;
                World w = new World(c);
                w.rnd.setSeed(26);
                w.populate(c);
                w.recount();
                City city = w.city;
                // A spot right behind a building's wall, the shooter out in the open beyond it.
                Entity probe = new Entity();
                float[] spot = null, from = null;
                for (City.Building b : city.buildings) {
                    if (b.collapsed || city.enterable(b)) continue;
                    float x = b.x1 + 7, y = (b.y0 + b.y1) / 2;
                    float fx = b.x0 - 120, fy = y;
                    if (city.circleBlocked(x, y, 4) || city.solidAt(fx, fy)) continue;
                    probe.x = x;
                    probe.y = y;
                    if (w.inCover(probe, fx, fy)) {
                        spot = new float[]{x, y};
                        from = new float[]{fx, fy};
                        break;
                    }
                }
                check(spot != null, "a wall to hide behind");
                probe.x = spot[0] + 200;
                probe.y = spot[1];
                float[] open = city.findWalkable(probe.x, probe.y);
                probe.x = open[0];
                probe.y = open[1];
                // Shots at someone behind cover hit half as often as at someone out in the open.
                Entity shooter = w.spawn(Entity.COP, from[0], from[1]);
                Entity hid = w.spawn(Entity.CIVILIAN, spot[0], spot[1]);
                hid.hasGun = true;
                hid.hp = hid.maxHp = 1e6f;
                java.lang.reflect.Method fire = World.class.getDeclaredMethod("fire", Entity.class, Entity.class, float.class, float.class);
                fire.setAccessible(true);
                int hitsHid = 0, hitsOpen = 0;
                for (int k = 0; k < 400; k++) {
                    hid.x = spot[0];
                    hid.y = spot[1];
                    float before = hid.hp;
                    shooter.ammo = 30;
                    shooter.suppress = 0;
                    fire.invoke(w, shooter, hid, (float) Math.hypot(hid.x - shooter.x, hid.y - shooter.y), 300f);
                    if (hid.hp < before) hitsHid++;
                    hid.x = shooter.x + 60;
                    hid.y = shooter.y;
                    hid.suppress = 0;
                    before = hid.hp;
                    shooter.ammo = 30;
                    fire.invoke(w, shooter, hid, 60f, 300f);
                    if (hid.hp < before) hitsOpen++;
                }
                check(hitsHid < hitsOpen * 0.75f, "harder to hit behind cover (" + hitsHid + " vs " + hitsOpen + " in the open)");
                // Misses pin the target down, and it wears off.
                hid.suppress = 0;
                for (int k = 0; k < 60; k++) {
                    shooter.ammo = 30;
                    fire.invoke(w, shooter, hid, 60f, 300f);
                }
                check(hid.suppress > 0.5f, "pinned down by fire (" + hid.suppress + ")");
                java.lang.reflect.Method cs = World.class.getDeclaredMethod("coverStep", Entity.class, float.class);
                cs.setAccessible(true);
                hid.lastAimT = -99;
                for (int k = 0; k < 90; k++) cs.invoke(w, hid, 1 / 30f);
                check(hid.suppress < 0.4f, "and it wears off");
                // The city's helicopters can be shot down, but take more than one round.
                CityConfig ac = new CityConfig();
                ac.v[CityConfig.OPT_ZOMBIES] = 1;
                ac.seed = 28;
                World aw = new World(ac);
                aw.populate(ac);
                Fleet.Vehicle mine = null;
                for (Fleet.Vehicle v : aw.fleet.vehicles) if (v.type == Fleet.HELI) { mine = v; break; }
                if (mine != null) {
                    float m0 = mine.hp;
                    aw.fleet.damage(mine, 10, false);
                    check(mine.hp < m0 && mine.hp > 0, "and gunfire hurts the city's own helicopters, which take more than one round");
                }
            }
        });
        test("every screen draws", new Check() {
            public void run() throws Exception {
                GameView v = new GameView(new android.app.Activity());
                for (int[] size : new int[][]{{2400, 1080}, {1080, 2400}, {2560, 1600}}) {
                    v.layout(size[0], size[1]);
                    Menu m = (Menu) field(v, "menu");
                    for (int s : new int[]{Menu.MAIN, Menu.SETUP, Menu.SETTINGS, Menu.NOTES, Menu.RECORDS}) {
                        m.open(s);
                        draw(v, size);
                    }
                    CityConfig c = new CityConfig();
                    c.v[CityConfig.OPT_ZOMBIES] = 3;
                    v.startGame(c);
                    v.awaitLoad();
                    draw(v, size);
                    m.screen = Menu.NONE;
                    for (int k = 0; k < 20; k++) draw(v, size);
                    // Every spawn picker opens and draws.
                    Field pk = GameView.class.getDeclaredField("picker");
                    pk.setAccessible(true);
                    for (int t = 0; t < 13; t++) {
                        pk.setInt(v, t);
                        draw(v, size);
                    }
                    pk.setInt(v, -1);
                    m.open(Menu.PAUSE);
                    draw(v, size);
                    m.open(Menu.STATS);
                    draw(v, size);
                    m.open(Menu.LOG);
                    draw(v, size);
                    m.open(Menu.STATS);
                    java.lang.reflect.Method act = Menu.class.getDeclaredMethod("act", int.class);
                    act.setAccessible(true);
                    act.invoke(m, 24);
                    draw(v, size);
                    m.open(Menu.SAVES);
                    draw(v, size);
                    check(v.drawErrors == 0, "every frame drew without an error (" + v.drawErrors + ")");
                    // Both graphics styles draw, and switching redraws the map.
                    Settings st = (Settings) field(v, "settings");
                    World w = (World) field(v, "world");
                    for (int g = 0; g < 2; g++) {
                        st.set(13, g);
                        v.settingsChanged();
                        check(w.city.drawnRealistic == (g == 1), "map redrawn in the chosen style");
                        m.screen = Menu.NONE;
                        draw(v, size);
                    }
                    m.screen = Menu.NONE;
                }
            }
        });
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    /** How far it is along a walking route's corners from (sx, sy). */
    private static float routeLength(City city, float sx, float sy, int[] route) {
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
    private static int alive(World w) {
        int n = w.fleet.riderCount();
        for (Entity e : w.entities) if (!e.dead && !e.isZombie()) n++;
        for (City.Building b : w.city.buildings) n += b.visitors.size() + b.occupants.size();
        return n;
    }

    interface Check {
        void run() throws Exception;
    }

    /**
     * test.sh runs the tests in several JVMs at once: SHARD of SHARDS takes every SHARDS-th test (ONLY, if set,
     * picks tests by the start of their name instead).
     */
    private static int index = -1, slot;

    private static boolean mine(String name) {
        String only = System.getenv("ONLY");
        if (only != null) return name.startsWith(only);
        String shards = System.getenv("SHARDS");
        return shards == null || slot % Integer.parseInt(shards) == Integer.parseInt(System.getenv("SHARD"));
    }

    /** A test that carries on from the one before (it runs in the same JVM as it). */
    private static void testAfter(String name, Check c) {
        run(name, c);
    }

    private static void test(String name, Check c) {
        slot = ++index;
        run(name, c);
    }

    private static void run(String name, Check c) {
        if (!mine(name)) return;
        long t = System.currentTimeMillis();
        // ((20.0) Every test plays out the same way each run: the simulation's dice are seeded from the test's name.)
        World.testSeed = name.hashCode() & 0x7fffffffL;
        try {
            c.run();
            passed++;
            System.out.println("PASS  " + name + "  (" + (System.currentTimeMillis() - t) + " ms)");
        } catch (Throwable e) {
            failed++;
            System.out.println("FAIL  " + name + ": " + e);
            e.printStackTrace(System.out);
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    private static Entity pickT(World w, Entity e, float r) throws Exception {
        java.lang.reflect.Method m = World.class.getDeclaredMethod("pickTarget", Entity.class, float.class);
        m.setAccessible(true);
        return (Entity) m.invoke(w, e, r);
    }

    private static Object field(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    private static void draw(GameView v, int[] size) throws Exception {
        Field last = GameView.class.getDeclaredField("lastFrame");
        last.setAccessible(true);
        last.set(v, System.nanoTime() - 33000000L);
        BufferedImage img = new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_ARGB);
        java.lang.reflect.Method onDraw = GameView.class.getDeclaredMethod("onDraw", Canvas.class);
        onDraw.setAccessible(true);
        onDraw.invoke(v, new Canvas(img.createGraphics()));
    }
}
