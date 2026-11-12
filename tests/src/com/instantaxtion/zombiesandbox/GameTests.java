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
                    // ((16.5) No hospital any more: it had nothing left to do.)
                    check(city.nearestFacility(City.FACILITY_HOSPITAL, 0, 0) == null, name + " has no hospital");
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
        test("11.3: only the USA, and the deleted maps are gone for good", new Check() {
            public void run() {
                check(Country.NAMES.length == 1 && Country.get(5) == Country.get(Country.USA), "the USA is the only country");
                CityConfig old = new CityConfig();
                check(old.applyCode("12-44") && old.country() == Country.USA, "codes build American cities");
                check(!new CityConfig().applyCode("22-77@16"), "a code for another country doesn't load");
                // ((12.7) The real cities too: Los Angeles, Portland, Seattle and New York, 15 to 18.)
                // ((15.2) Not 10: its place went to Downtown Towers. (15.5) Nor 11: the Military Town. (15.6) Nor 15:
                // the Prison Island.)
                for (int p : new int[]{ 16, 17, 18, 19, 20, 21, 22, 24, 25, 26, 27, 28}) {
                    check(CityConfig.removed(p) && CityConfig.retired(CityConfig.OPT_PRESET, p) && CityConfig.PRESETS[p].isEmpty(), "map " + p + " is gone");
                    check(!new CityConfig().applyCode(String.valueOf("123456789ABCDEFGHIJKLMNOPQRST".charAt(p)) + "3-5"), "and its city code doesn't load");
                    CityConfig c = new CityConfig();
                    c.v[CityConfig.OPT_PRESET] = p;
                    c.normalize();
                    check(c.preset() == CityConfig.METROPOLIS, "nor does it build");
                }
                check(!CityConfig.removed(CityConfig.ISLANDS) && !CityConfig.removed(CityConfig.ARCHIPELAGO), "the island maps stay");
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
        test("water maps: a lake and a port; nobody ends up in the water", new Check() {
            public void run() {
                for (int preset = 12; preset < 14; preset++) {
                    CityConfig c = new CityConfig();
                    c.seed = 21 + preset;
                    c.v[CityConfig.OPT_PRESET] = preset;
                    c.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                    c.v[CityConfig.OPT_ZOMBIES] = 3;
                    World w = new World(c);
                    w.populate(c);
                    City city = w.city;
                    String name = CityConfig.PRESETS[preset];
                    int water = 0;
                    for (byte t : city.tiles) if (t == City.WATER) water++;
                    check(water > city.w * 8, name + ": water (" + water + " tiles)");
                    check(city.armory != null && city.cityHall != null, name + ": city hall and a National Guard armory");
                    switch (city.waterKind) {
                        case CityConfig.W_LAKE: check(city.lakes.size() == 1, name + ": a lake"); break;
                        default: check(city.seaY > 0, name + ": the sea"); break;
                    }
                    check(!w.boats.isEmpty(), name + ": boats on the water");
                    for (int f = 0; f < 30 * 30; f++) w.update(1 / 30f);
                    int wet = 0;
                    for (Entity e : w.entities)
                        if (!e.dead && !e.hidden && city.tiles[city.tileIndex(e.x, e.y)] == City.WATER && !city.isFord(city.tileIndex(e.x, e.y))) wet++;
                    for (Fleet.Vehicle v : w.fleet.vehicles)
                        if (v.type != Fleet.HELI && v.type != Fleet.JET && city.tiles[city.tileIndex(v.x, v.y)] == City.WATER) wet++;
                    check(wet == 0, name + ": " + wet + " people or cars in the water");
                }
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
        test("Islands: two to four island towns, joined up, one fixed size, nobody stranded", new Check() {
            public void run() {
                CityConfig c = new CityConfig();
                c.seed = 9;
                c.set(CityConfig.OPT_PRESET, CityConfig.ISLANDS);
                c.set(CityConfig.OPT_SIZE, CityConfig.SMALL);
                check(c.tiles() == CityConfig.ISLANDS_TILES && c.v[CityConfig.OPT_SIZE] == CityConfig.COLOSSAL, "Islands is always " + CityConfig.ISLANDS_TILES + " tiles");
                CityConfig back = new CityConfig();
                check(back.applyCode(c.code()) && back.tiles() == CityConfig.ISLANDS_TILES, "its city code (" + c.code() + ") builds the Islands again");
                City city = new City(c);
                check(city.w > 448, "bigger than a Massive map");
                int n = city.islandRects == null ? 0 : city.islandRects.length;
                check(n >= 2 && n <= 4, "two to four islands (" + n + ")");
                java.util.HashSet<Integer> counts = new java.util.HashSet<Integer>();
                for (int sd = 1; sd <= 12; sd++) {
                    CityConfig o = new CityConfig();
                    o.seed = sd;
                    o.set(CityConfig.OPT_PRESET, CityConfig.ISLANDS);
                    counts.add(new City(o, 0.05f).islandRects.length);
                }
                check(counts.size() >= 2, "the number of islands varies from city to city " + counts);
                // Every bit of land can be walked to from the causeway (no islets, no cut-off scraps of coast).
                int[] q0 = new int[city.w * city.h];
                boolean[] seen = new boolean[city.w * city.h];
                int start = city.tileIndex(city.causewayEnd[0], city.causewayEnd[1]), head = 0, tail = 0;
                q0[tail++] = start;
                seen[start] = true;
                while (head < tail) {
                    int t = q0[head++], x = t % city.w, y = t / city.w;
                    int[] nb = {x > 0 ? t - 1 : -1, x < city.w - 1 ? t + 1 : -1, y > 0 ? t - city.w : -1, y < city.h - 1 ? t + city.w : -1};
                    for (int j : nb)
                        if (j >= 0 && !seen[j] && city.tiles[j] != City.WATER) {
                            seen[j] = true;
                            q0[tail++] = j;
                        }
                }
                int stranded = 0;
                for (int t = 0; t < city.w * city.h; t++) if (city.tiles[t] != City.WATER && !seen[t]) stranded++;
                check(stranded == 0, "no land cut off from the rest (" + stranded + " tiles)");
                int[] precincts = new int[n], fire = new int[n], buildings = new int[n];
                for (City.Facility f : city.facilities) {
                    int i = city.island((int) (f.x / City.T), (int) (f.y / City.T));
                    if (i < 0) continue;
                    if (f.kind == City.FACILITY_POLICE) precincts[i]++;
                    if (f.kind == City.FACILITY_FIRE) fire[i]++;
                }
                for (City.Building b : city.buildings) {
                    int i = city.island((int) (b.doorX / City.T), (int) (b.doorY / City.T));
                    if (i >= 0) buildings[i]++;
                }
                int[] dist = new int[city.w * city.h];
                for (int i = 0; i < n; i++) {
                    int[] q = city.islandRects[i];
                    // ((16.9) No fire stations in new cities.)
                    check(buildings[i] > 150 && precincts[i] > 0 && fire[i] == 0, city.islandNames[i] + ": a town (" + buildings[i]
                            + " buildings) with its own police station");
                    // (A street near the middle of the island.)
                    float[] spot = null;
                    for (int r = 0; r < 60 && spot == null; r++)
                        for (int dy = -r; dy <= r && spot == null; dy++)
                            for (int dx = -r; dx <= r && spot == null; dx++) {
                                int tx = (q[0] + q[2]) / 2 + dx, ty = (q[1] + q[3]) / 2 + dy;
                                if (city.tiles[ty * city.w + tx] == City.ROAD) spot = new float[]{(tx + 0.5f) * City.T, (ty + 0.5f) * City.T};
                            }
                    boolean road = spot != null && city.driveField(dist, spot[0], spot[1])
                            && dist[city.tileIndex(city.causewayEnd[0], city.causewayEnd[1])] < City.FAR;
                    check(road, city.islandNames[i] + " can be reached by road from the mainland causeway");
                }
                float[] edge = city.edgeRoad(new java.util.Random(1));
                check(edge != null && city.drivable(edge[0], edge[1]), "convoys come in over the causeway");
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
        test("10.10: new soldiers, police, vehicles; the Guard; buses; the homeless", new Check() {
            public void run() {
                CityConfig c = new CityConfig();
                c.seed = 5;
                c.v[CityConfig.OPT_SIZE] = CityConfig.LARGE;
                c.v[CityConfig.OPT_MILITARY] = 4;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                World w = new World(c);
                w.populate(c);
                int medic = 0, gren = 0, marks = 0;
                for (Entity e : w.entities) {
                    if (e.role == Entity.ROLE_CORPSMAN) medic++;
                    if (e.role == Entity.ROLE_GRENADIER) gren++;
                    if (e.role == Entity.ROLE_MARKSMAN) marks++;
                }
                // ((13.9) Combat medics are back: gone from 10.50 to 13.8.)
                check(medic > 0 && gren > 0, "squads have grenadiers and combat medics (" + medic + ", " + gren + ")");
                check(marks > 0, "a police marksman at the precinct (" + marks + ")");
                int bikes = 0;
                for (Fleet.Vehicle v : w.fleet.vehicles) if (v.kind == Fleet.K_BIKE) bikes++;
                check(bikes > 0, "police motorcycles on patrol (" + bikes + ")");
                check(w.dispatch.riotVans > 0 && w.dispatch.policeAir > 0, "riot vans and the police helicopter are ready");
                Entity doc = null;
                for (Entity e : w.entities) if (e.type == Entity.SOLDIER && e.task == Dispatch.T_NONE && e.rig == null) doc = e;
                // Someone who turns up later has no home.
                Entity drifter = w.spawn(Entity.CIVILIAN, doc.x, doc.y + 40);
                for (int f = 0; f < 60; f++) w.update(1 / 30f);
                check(drifter.homeChecked && drifter.home == null, "a spawned person is homeless");
                // At war: no bus leaves town with people aboard.
                w.alert = 2;
                for (int f = 0; f < 600; f++) w.update(1 / 30f);
                for (Fleet.Vehicle v : w.fleet.vehicles)
                    check(!(v.type == Fleet.CAR && v.model == Fleet.M_BUS && v.fleeing), "a bus is leaving town");
                // A big outbreak brings out the National Guard by itself.
                CityConfig g = new CityConfig();
                g.seed = 6;
                g.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                g.v[CityConfig.OPT_ZOMBIES] = 6;
                g.v[CityConfig.OPT_RESERVES] = 3;
                World gw = new World(g);
                gw.populate(g);
                gw.outbreak = true;
                gw.outbreakTime = 120;
                // (A big one for the size of the place: busier outskirts since 10.20 mean more people.)
                int big = Math.max(80, gw.humanCount() / 8 + 20);
                // ((10.63) Room for them: a city this full reaches the test's population cap.)
                gw.maxEntities += big + 50;
                for (int i = 0; i < big; i++) gw.spawn(Entity.ZOMBIE, gw.city.worldW() / 2 + i % 120, gw.city.worldH() / 2 + i / 120 * 4);
                gw.callNationalGuard();
                check(gw.guardCalled, "the governor calls out the Guard for a big outbreak, with army reserves still to come");
            }
        });
        test("10.14: fords, winding trails, rangers, fire lookouts, the rescue helicopter, nobody reports what nobody saw", new Check() {
            public void run() {
                CityConfig c = new CityConfig();
                c.seed = 9;
                c.v[CityConfig.OPT_PRESET] = 6;
                c.v[CityConfig.OPT_SIZE] = CityConfig.LARGE;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                // (The map these checks were written for: as built before 10.20's busier outskirts.)
                c.civic = 6;
                World w = new World(c);
                w.populate(c);
                City city = w.city;
                w.viewX0 = 0;
                w.viewY0 = 0;
                w.viewX1 = city.worldW();
                w.viewY1 = city.worldH();
                // Streams can be waded: water, but not in the way, and a route goes straight across.
                int fords = 0, across = 0, tried = 0;
                for (int i = 0; i < city.w * city.h; i++) {
                    if (!city.isFord(i)) continue;
                    fords++;
                    check(!city.solid[i], "a ford is not a wall");
                    int x = i % city.w, y = i / city.w;
                    if (tried >= 10 || i % 5 != 0 || x < 4 || y < 4 || x >= city.w - 4 || y >= city.h - 4) continue;
                    int ia = y * city.w + x - 3, ib = y * city.w + x + 3;
                    if (city.solid[ia] || city.solid[ib] || city.tiles[ia] == City.WATER || city.tiles[ib] == City.WATER) continue;
                    tried++;
                    int[] p = city.findPath((x - 2.5f) * City.T, (y + 0.5f) * City.T, (x + 3.5f) * City.T, (y + 0.5f) * City.T, 9000);
                    if (p != null && p.length <= 12) across++;
                }
                check(fords > 20, "streams you can wade (" + fords + " tiles)");
                check(tried == 0 || across * 2 >= tried, "a short way straight across a stream (" + across + " of " + tried + ")");
                // Waterfalls sit across the stream, as wide as it is.
                for (float[] f : city.falls) check(f.length > 4 && f[4] >= City.T / 2f && f[4] <= City.T * 2.5f, "a waterfall as wide as its stream (" + (f.length > 4 ? f[4] : 0) + ")");
                // Trails: drawn as winding lines, walks along them, a ranger station and fire towers.
                check(!city.trailLines.isEmpty() && city.hikes.size() >= 3, "trails to walk (" + city.hikes.size() + ")");
                check(city.rangerStationName != null && w.rangerStation != null, "a ranger station");
                check(!city.fireTowers.isEmpty(), "fire lookout towers");
                int rangers = 0, trucks = 0, aloft = 0;
                for (Entity e : w.entities) {
                    if (!e.dead && e.agency == 3) rangers++;
                    if (!e.dead && e.aloft && e.job == Entity.J_LOOKOUT) aloft++;
                }
                for (Fleet.Vehicle v : w.fleet.vehicles) if (v.agency == 3) trucks++;
                check(rangers >= 2 && trucks == 1, "rangers on foot and in their truck (" + rangers + ", " + trucks + ")");
                check(aloft == city.fireTowers.size(), "a lookout up every tower (" + aloft + ")");
                // A shift change: the relief walks up from the ranger station and takes over.
                Entity first = w.towerKeeper[0];
                w.towerShift[0] = 0;
                boolean changed = false;
                int hiking = 0;
                for (int f = 0; f < 30 * 240 && !changed; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                    changed = w.towerKeeper[0] != null && w.towerKeeper[0] != first;
                }
                check(changed && first.job == Entity.J_OFFDUTY && !first.aloft, "the next lookout takes over and the last one heads home");
                for (Entity e : w.entities) if (!e.dead && e.hike != null && e.hikeIdx > 5) hiking++;
                check(hiking > 0, "people out along the trails (" + hiking + ")");
                // One of the dead out in the woods with nobody to see it: no news, no alert.
                CityConfig q = new CityConfig();
                q.seed = 9;
                q.v[CityConfig.OPT_PRESET] = 6;
                q.v[CityConfig.OPT_SIZE] = CityConfig.LARGE;
                q.v[CityConfig.OPT_ZOMBIES] = 0;
                World lone = new World(q);
                lone.populate(q);
                lone.update(1 / 30f);
                float[] far = null;
                for (int k = 0; k < 4000 && far == null; k++) {
                    float[] p = lone.city.randomWalkable(lone.rnd);
                    if (!lone.city.inTown(p[0], p[1]) && !lone.peopleNear(p[0], p[1], 700)) far = p;
                }
                if (far != null) {
                    lone.spawn(Entity.ZOMBIE, far[0], far[1]);
                    for (int f = 0; f < 30 * 40; f++) lone.update(1 / 30f);
                    check(lone.alert == 0 || lone.dispatch.calls > 0 || lone.bites > 0, "nobody reports one of them that nobody has seen");
                }
                // The dead round a fire tower: the rescue helicopter lowers its team and lifts people out.
                float[] t = city.fireTowers.get(0);
                // (No rangers about: with help close by, nobody calls the helicopter.)
                for (Entity e : w.entities) if (e.agency == 3 && e.rig == null) {
                    e.dead = true;
                    e.removed = true;
                }
                for (int i = 0; i < 4; i++) w.spawn(Entity.ZOMBIE, t[0] - 70 + i * 8, t[1] - 50);
                w.alert = Math.max(1, w.alert);
                boolean sent = false, lowered = false;
                for (int f = 0; f < 30 * 120 && !lowered; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                    for (Fleet.Vehicle v : w.fleet.vehicles)
                        if (v.kind == Fleet.K_RESCUE_HELI) {
                            sent = true;
                            if (v.team != null && !v.team.isEmpty()) lowered = true;
                        }
                }
                check(sent && lowered, "the rescue helicopter comes and lowers its team");
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
        test("10.18: fewer maps, no animals, hikers out, people get clear, dirt lanes", new Check() {
            public void run() throws Exception {
                check(CityConfig.retired(CityConfig.OPT_PRESET, 0) && CityConfig.retired(CityConfig.OPT_PRESET, 7), "Classic and Campus are gone");
                CityConfig d = new CityConfig();
                // ((16.1) Since renamed: Default is the Gigantic now (see the 16.1 test).)
                check(d.values(CityConfig.OPT_SIZE)[CityConfig.GIGANTIC].equals("Default") && d.values(CityConfig.OPT_SIZE)[CityConfig.HUGE].equals("Massive"),
                        "the sizes are named");
                check(CityConfig.retired(CityConfig.OPT_ZOMBIES, 3) && d.zombies() == 0, "no choosing how many zombies: the game starts quiet");
                // No animals.
                CityConfig c = new CityConfig();
                c.civic = 25; // ((11.0) Map version 25: the country round the town as it was before the thin band.)
                c.seed = 9;
                c.v[CityConfig.OPT_PRESET] = 6;
                World w = new World(c);
                // (The same run every time: what happens next is chance, and a test shouldn't be.)
                w.rnd.setSeed(18);
                w.populate(c);
                int dogs = 0, hikers = 0;
                for (Entity e : w.entities) {
                    if (e.type == Entity.DOG || e.type == Entity.ZOMBIE_DOG) dogs++;
                    if (!e.dead && e.job == Entity.J_HIKER && e.jobStep == 1) hikers++;
                }
                check(dogs == 0 && w.birds.isEmpty() && w.animals.isEmpty() && w.spawn(Entity.DOG, 100, 100) == null, "no animals");
                check(hikers >= 5, "people already out on the trails (" + hikers + ")");
                // People get out of where the dead are.
                for (int f = 0; f < 30 * 5; f++) w.update(1 / 30f);
                float[] cp = null;
                for (Entity e : w.entities) if (!e.dead && e.type == Entity.CIVILIAN && w.city.inTown(e.x, e.y)) cp = new float[]{e.x, e.y};
                int sp = 0;
                for (int i = 0; i < 400 && sp < 30; i++) {
                    float[] q = w.city.findWalkable(cp[0] + (i % 20) * 6 - 60, cp[1] + (i / 20) * 6 - 60);
                    if (q != null && w.spawn(Entity.ZOMBIE, q[0], q[1]) != null) sp++;
                }
                w.alert = 2;
                int evac = 0;
                for (int f = 0; f < 30 * 12; f++) {
                    w.update(1 / 30f);
                    // (Since 10.37 getting clear is the civilian's own choice: evacuating, running, or hiding.)
                    if (f % 30 == 0) for (Entity e : w.entities)
                        if (!e.dead && e.type == Entity.CIVILIAN && (e.task == Dispatch.T_EVACUATE || e.goal == World.G_FLEE || e.goal == World.G_HIDE || e.goal == World.G_EVAC)) evac++;
                }
                check(evac > 0, "people get clear of the outbreak");
                // Dirt lanes stay dirt.
                CityConfig lc = new CityConfig();
                // ((10.52) Seed 3: with another police station seed 2's lanes meet a main road at a paved junction.)
                lc.seed = 3;
                lc.v[CityConfig.OPT_PRESET] = 8;
                lc.v[CityConfig.OPT_SIZE] = CityConfig.MASSIVE;
                // ((16.5) Map version 29, the streets this seed was picked for: without the hospital, a side road's end
                // meets a lane by a main road, and the count takes that short crossing for a stub.)
                lc.civic = 29;
                City lanes = new City(lc, 0.05f);
                int stubs = 0;
                for (int y = 0; y < lanes.h; y++)
                    for (int x = 1; x < lanes.w - 1; x++) {
                        if (lanes.tiles[y * lanes.w + x - 1] != City.DIRT || lanes.tiles[y * lanes.w + x] != City.ROAD) continue;
                        int k = x;
                        while (k < lanes.w && lanes.tiles[y * lanes.w + k] == City.ROAD) k++;
                        int across = 0;
                        for (int dy = -6; dy <= 6; dy++)
                            if (y + dy >= 0 && y + dy < lanes.h && lanes.tiles[(y + dy) * lanes.w + (x + k) / 2] == City.ROAD) across++;
                        // (A town's street with pavements along it, that a lane runs on from, is a street, not a stub.)
                        boolean street = false;
                        for (int dy = -3; dy <= 3; dy++)
                            if (y + dy >= 0 && y + dy < lanes.h && lanes.tiles[(y + dy) * lanes.w + (x + k) / 2] == City.SIDEWALK) street = true;
                        if (k < lanes.w && k - x < 25 && lanes.tiles[y * lanes.w + k] == City.DIRT && across <= 4 && !street) stubs++;
                    }
                check(stubs == 0, stubs + " stretches of tarmac in the middle of a dirt lane");
            }
        });
        test("10.19: gunfire heard, the ranger off the wall, out of hiding sooner, resupply, driveways, quick close-ups", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.seed = 9;
                c.v[CityConfig.OPT_PRESET] = 6;
                World w = new World(c);
                w.populate(c);
                for (int f = 0; f < 30 * 2; f++) w.update(1 / 30f);
                // Everyone hears a gunshot: people nearby run, an idle cop goes to look.
                Entity civ = null;
                for (Entity e : w.entities)
                    if (!e.dead && e.type == Entity.CIVILIAN && e.fleeTimer <= 0 && e.leader == null && e.task == Dispatch.T_NONE && w.city.inTown(e.x, e.y)) {
                        civ = e;
                        break;
                    }
                check(civ != null, "someone out in town");
                float[] gp = w.city.findWalkable(civ.x + 60, civ.y);
                Entity cop = w.spawn(Entity.COP, gp[0], gp[1]);
                float[] lp = w.city.findWalkable(civ.x - 60, civ.y);
                Entity other = w.spawn(Entity.COP, lp[0], lp[1]);
                // (Into the grid of who's where, then a zombie for the cop to shoot at.)
                w.update(1 / 30f);
                Entity z = w.spawn(Entity.ZOMBIE, cop.x + 40, cop.y);
                other.fear = 0;
                other.aiming = false;
                other.task = Dispatch.T_NONE;
                civ.fleeTimer = 0;
                w.heardShot(cop, z, 250);
                check(civ.fleeTimer > 0 && civ.aware, "people near a gunshot run from it");
                check(other.fear > 0 && Math.hypot(other.threatX - z.x, other.threatY - z.y) < 30, "an idle cop goes to see what the shooting is");
                z.dead = true;
                // The ranger minding the station stands out front, not against the wall.
                CityConfig wc = new CityConfig();
                wc.civic = 25; // ((11.0) Map version 25: the country round the town as it was before the thin band.)
                wc.seed = 4;
                wc.v[CityConfig.OPT_PRESET] = 8;
                wc.v[CityConfig.OPT_SIZE] = CityConfig.MASSIVE;
                World ww = new World(wc);
                ww.populate(wc);
                check(ww.rangerStation != null, "a ranger station");
                java.lang.reflect.Method spot = World.class.getDeclaredMethod("stationSpot", float.class, float.class);
                spot.setAccessible(true);
                City.Building rs = ww.rangerStation;
                for (float a : new float[]{0, 0.9f, -0.7f}) {
                    float[] p = (float[]) spot.invoke(ww, a, 30f);
                    float gap = Math.max(Math.max(rs.x0 - p[0], p[0] - rs.x1), Math.max(rs.y0 - p[1], p[1] - rs.y1));
                    check(gap > 6 && !ww.city.solidAt(p[0], p[1]), "the station keeper's spot is clear of the walls (" + gap + ")");
                }
                // ((16.14) Nobody goes into a building now: one sent to hide at a house stays out in the street.)
                Entity hid = null;
                City.Building home = null;
                for (Entity e : w.entities)
                    if (!e.dead && e.type == Entity.CIVILIAN && e.leader == null && e != civ) {
                        for (City.Building b : w.city.buildings)
                            if (b.kind == City.HOUSE && !b.collapsed && Math.hypot(b.doorX - e.x, b.doorY - e.y) < 400) {
                                home = b;
                                break;
                            }
                        if (home != null) {
                            hid = e;
                            break;
                        }
                    }
                java.lang.reflect.Method enter = World.class.getDeclaredMethod("enterBuilding", Entity.class, City.Building.class);
                enter.setAccessible(true);
                enter.invoke(w, hid, home);
                check(!home.occupants.contains(hid) && !hid.removed && w.entities.contains(hid), "nobody goes inside to hide");
                hid.task = Dispatch.T_HIDE;
                hid.building = home;
                for (int f = 0; f < 30; f++) w.update(1 / 30f);
                // ((17.1) Hiding is walking in and staying: nobody vanishes into the house's list.)
                check(home.occupants.isEmpty() && (hid.task != Dispatch.T_HIDE || hid.stayIn == home), "and anyone told to hide walks in, or thinks again");
                // Supply trucks after it's over: they come in and fill the shelves and armouries.
                City.Facility station = null;
                for (City.Facility f : ww.city.facilities) if (f.kind == City.FACILITY_POLICE) station = f;
                station.ammo = 50;
                City.Building store = null;
                for (City.Building b : ww.city.buildings) if (b.kind == City.MARKET && !b.collapsed) store = b;
                if (store != null) store.food = 0;
                java.lang.reflect.Method restock = World.class.getDeclaredMethod("restockTown");
                restock.setAccessible(true);
                java.util.List<Fleet.Vehicle> trucks = new java.util.ArrayList<Fleet.Vehicle>();
                for (int k = 0; k < 12; k++) {
                    ww.time = k * 4;
                    restock.invoke(ww);
                }
                for (Fleet.Vehicle v : ww.fleet.vehicles) if (v.restock) trucks.add(v);
                check(ww.restockSent > 0 && !trucks.isEmpty(), "supply trucks sent in (" + trucks.size() + ")");
                for (Fleet.Vehicle v : trucks) ww.restocked(v);
                check(station.ammo >= 3000, "the police armoury restocked");
                check(store == null || store.food >= 400, "the supermarket restocked");
                // Driveways: houses out in the country all have a way to the road.
                int cut5 = 0, cut6 = 0;
                for (int civic = 5; civic <= 6; civic++) {
                    CityConfig dc = new CityConfig();
                    dc.seed = 5;
                    dc.v[CityConfig.OPT_PRESET] = 8;
                    dc.civic = civic;
                    City city = new City(dc, 0.05f);
                    for (City.Building b : city.buildings) {
                        if (b.kind != City.HOUSE || city.inTown(b.doorX, b.doorY)) continue;
                        boolean reached = false;
                        int x0 = (int) (b.x0 / City.T) - 1, y0 = (int) (b.y0 / City.T) - 1;
                        int x1 = (int) Math.ceil(b.x1 / City.T), y1 = (int) Math.ceil(b.y1 / City.T);
                        for (int y = Math.max(0, y0); y <= Math.min(city.h - 1, y1); y++)
                            for (int x = Math.max(0, x0); x <= Math.min(city.w - 1, x1); x++) {
                                byte t = city.tiles[y * city.w + x];
                                if (t == City.ROAD || t == City.DIRT || t == City.LOT || t == City.SIDEWALK) reached = true;
                            }
                        if (!reached) {
                            if (civic == 5) cut5++;
                            else cut6++;
                        }
                    }
                }
                check(cut5 > 5 && cut6 * 5 <= cut5, "driveways to country houses (cut off: " + cut5 + " before, " + cut6 + " now)");
                // Close-ups draw quickly: just their own part of the map.
                City big = ww.city;
                long t0 = System.nanoTime();
                for (int k = 0; k < 8; k++) big.renderRegion(big.worldW() / 2 + (k % 4) * 192, big.worldH() / 2 + (k / 4) * 192, 192, 2);
                long ms = (System.nanoTime() - t0) / 8000000;
                check(ms < 60, "a close-up takes " + ms + " ms");
            }
        });
        test("10.24: streets line up, road surfaces, hills in town, holstered guns, new weapons and sounds", new Check() {
            public void run() throws Exception {
                // Streets carry straight on across junctions far more often than before.
                int[] jogs = new int[2];
                for (int k = 0; k < 2; k++)
                    for (int preset : new int[]{0, 15}) {
                        CityConfig c = new CityConfig();
                        c.v[CityConfig.OPT_PRESET] = preset;
                        c.seed = 7;
                        c.normalize();
                        c.seed = 7;
                        c.civic = k == 0 ? 8 : 9;
                        City city = new City(c, 0.05f);
                        for (City.Street a : city.streets)
                            for (City.Street b : city.streets) {
                                if (a == b || a.vertical != b.vertical) continue;
                                int gap = a.vertical ? b.y0 - a.y1 : b.x0 - a.x1;
                                float off = a.vertical ? Math.abs((a.x0 + a.x1) - (b.x0 + b.x1)) / 2f : Math.abs((a.y0 + a.y1) - (b.y0 + b.y1)) / 2f;
                                if (gap >= 1 && gap <= 6 && off >= 1 && off <= 10) jogs[k]++;
                            }
                    }
                check(jogs[1] * 10 < jogs[0] * 7, "fewer streets jog at a junction (" + jogs[0] + " before, " + jogs[1] + " now)");
                // Cobbles in the old town, concrete in the industrial district, chip-seal out in the country.
                CityConfig oc = new CityConfig();
                oc.v[CityConfig.OPT_PRESET] = 5;
                oc.seed = 3;
                oc.normalize();
                oc.seed = 3;
                City old = new City(oc, 0.05f);
                int[] surf = new int[4];
                for (int y = 0; y < old.h; y++)
                    for (int x = 0; x < old.w; x++)
                        if (old.tiles[y * old.w + x] == City.ROAD) surf[old.roadSurface(x, y)]++;
                check(surf[City.SURF_COBBLE] > 50 && surf[City.SURF_ASPHALT] > 50, "cobbled streets in the old town (" + java.util.Arrays.toString(surf) + ")");
                // Hills in town, not just out in the country.
                float[] spread = new float[2];
                for (int k = 0; k < 2; k++) {
                    CityConfig c = new CityConfig();
                    c.v[CityConfig.OPT_PRESET] = 0;
                    c.seed = 11;
                    c.normalize();
                    c.seed = 11;
                    c.civic = k == 0 ? 8 : 9;
                    City city = new City(c, 0.05f);
                    float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
                    for (int y = city.townY0 + 4; y < city.townY1 - 4; y += 3)
                        for (int x = city.townX0 + 4; x < city.townX1 - 4; x += 3) {
                            float e = city.elevation((x + 0.5f) * City.T, (y + 0.5f) * City.T);
                            lo = Math.min(lo, e);
                            hi = Math.max(hi, e);
                        }
                    spread[k] = hi - lo;
                }
                check(spread[1] > spread[0] * 1.5f && spread[1] > 8, "the town has hills of its own (" + spread[0] + " before, " + spread[1] + " now)");
                // Guns away on an ordinary day.
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 2;
                World w = new World(c);
                w.populate(c);
                Entity cop = null;
                for (Entity e : w.entities) if (e.type == Entity.COP && e.task == Dispatch.T_NONE && !e.aiming) cop = e;
                check(cop != null && !w.weaponsOut(cop), "a police officer on a quiet day has the pistol holstered");
                w.alert = 2;
                check(w.weaponsOut(cop), "guns out once everyone knows about the outbreak");
                // New guns and tools, and sounds to go with them.
                check(World.magFor(Entity.W_HUNTING) == 5 && World.magFor(Entity.W_REVOLVER) == 6, "hunting rifle and revolver magazines");
                check("Machete".equals(Entity.MELEE_NAMES[Entity.M_MACHETE]) && "Crowbar".equals(Entity.MELEE_NAMES[Entity.M_CROWBAR]), "machetes and crowbars");
                for (int id : new int[]{Sfx.SHOTGUN, Sfx.REVOLVER, Sfx.GLASS, Sfx.POUND, Sfx.HUNTING})
                    check(Synth.effect(id).length > 1000 && Sfx.MIN_GAP.length == Sfx.COUNT, "new sound " + id);
            }
        });
        test("10.25: curved streets in the suburbs", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_PRESET] = 8;
                c.seed = 7;
                c.normalize();
                c.seed = 7;
                City city = new City(c, 0.05f);
                check(city.curves.size() >= 3, "curved streets laid out (" + city.curves.size() + ")");
                check(city.neighbourhoodKinds()[City.NB_WINDING] >= 1, "a neighbourhood of winding streets");
                // Every curve is road all along, and joins the rest of the streets (nothing cut off).
                int[] field = new int[city.w * city.h];
                float[] start = city.nearestDrivable(city.worldW() / 2, city.worldH() / 2);
                check(city.driveField(field, start[0], start[1], false, -1), "a road network");
                for (float[] l : city.curves) {
                    if (l.length == 3) continue;
                    for (int k = 0; k < l.length / 2; k++) {
                        int tx = (int) Math.floor(l[k * 2]), ty = (int) Math.floor(l[k * 2 + 1]);
                        check(city.tiles[ty * city.w + tx] == City.ROAD, "road under the curve at " + tx + "," + ty);
                    }
                    int mid = l.length / 4;
                    int i = (int) Math.floor(l[mid * 2 + 1]) * city.w + (int) Math.floor(l[mid * 2]);
                    check(field[i] < City.FAR, "the curve joins the streets");
                }
                CityConfig old = new CityConfig();
                old.v[CityConfig.OPT_PRESET] = 8;
                old.seed = 7;
                old.normalize();
                old.seed = 7;
                old.civic = 9;
                check(new City(old, 0.05f).curves.isEmpty(), "saved cities from before keep their straight streets");
            }
        });
        test("10.28: no bloaters, tougher zombies, ring road, stations on the roads, a slower start", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_PRESET] = 8;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 4;
                c.normalize();
                c.seed = 4;
                World w = new World(c);
                w.populate(c);
                for (int k = 0; k < 4000; k++) check(w.turnType(Entity.CIVILIAN) != Entity.BLOATER, "nobody comes back a bloater");
                float[] q = w.city.findWalkable(w.city.worldW() / 2, w.city.worldH() / 2);
                Entity z = w.spawn(Entity.ZOMBIE, q[0], q[1]);
                check(z.hp >= 90 * 0.75f, "an ordinary zombie takes more stopping (" + z.hp + ", frail ones a quarter less)");
                // The ring road round town, and every station on the road network.
                java.lang.reflect.Field rs = City.class.getDeclaredField("ringStreets");
                rs.setAccessible(true);
                check(!((java.util.List<?>) rs.get(w.city)).isEmpty(), "a ring road round the town");
                for (int preset : new int[]{2, 9}) {
                    CityConfig vc = new CityConfig();
                    vc.v[CityConfig.OPT_PRESET] = preset;
                    vc.seed = 4;
                    vc.normalize();
                    vc.seed = 4;
                    City city = new City(vc, 0.05f);
                    int[] f = new int[city.w * city.h];
                    city.driveField(f, city.worldW() / 2, city.worldH() / 2);
                    for (City.Facility fa : city.facilities) {
                        if (fa.kind != City.FACILITY_POLICE && fa.kind != City.FACILITY_FIRE) continue;
                        float[] p = city.nearestDrivable(fa.gateX > 0 ? fa.gateX : fa.x, fa.gateY > 0 ? fa.gateY : fa.y);
                        check(p != null && f[city.tileIndex(p[0], p[1])] < City.FAR, fa.name + " is on the road network");
                    }
                }
                // Nobody knows at first: a couple of calls aren't news yet. (None of the dead left from above to
                // bring in more.)
                w.clearZombies();
                w.outbreak = true;
                w.dispatch.calls = 2;
                for (int f = 0; f < 30 * 5; f++) w.update(1 / 30f);
                check(w.alert == 0, "two strange calls aren't news yet (alert " + w.alert + ")");
            }
        });
        test("10.29: highway interchanges with slip roads, no lights on the highway", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_PRESET] = 8;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 7;
                c.normalize();
                c.seed = 7;
                World w = new World(c);
                w.populate(c);
                City city = w.city;
                check(city.hwyAxis >= 0 && !city.hwyExits.isEmpty(), "a highway with ways on and off");
                boolean ew = city.hwyAxis == 0;
                int len = ew ? city.w : city.h;
                for (int k = 0; k < len; k++)
                    for (int a = 0; a < 7; a++) {
                        int x = ew ? k : city.hwyAt + a, y = ew ? city.hwyAt + a : k;
                        check(!city.junctionAt(x, y), "no junction (or lights) on the highway at " + x + "," + y);
                        check(city.tiles[y * city.w + x] != City.CAR, "nobody parked on the highway at " + x + "," + y);
                    }
                int[] f = new int[city.w * city.h];
                float[] mid = city.nearestDrivable((city.townX0 + city.townX1) / 2f * City.T, (city.townY0 + city.townY1) / 2f * City.T);
                city.driveField(f, mid[0], mid[1]);
                for (int[] ex : city.hwyExits) {
                    check(ex.length == 5, "an interchange, not a crossroads");
                    int kc = ex[0] + ex[1] / 2;
                    int x = ew ? kc : city.hwyAt + 3, y = ew ? city.hwyAt + 3 : kc;
                    check(city.tiles[y * city.w + x] == City.FENCE, "the road from town doesn't cut across the highway at " + kc);
                    // The slip road joins the near carriageway (and so town can be reached from the highway there).
                    int la = ex[2] < 0 ? city.hwyAt + 5 : city.hwyAt + 1;
                    int sx = ew ? ex[3] : la, sy = ew ? la : ex[3];
                    check(f[sy * city.w + sx] < City.FAR, "the way off at " + ex[3] + " leads into town");
                }
                // Both carriageways reach town (by the slip roads and the gaps to turn round in).
                for (int a : new int[]{1, 5}) {
                    int x = ew ? len / 2 : city.hwyAt + a, y = ew ? city.hwyAt + a : len / 2;
                    check(f[y * city.w + x] < City.FAR, "carriageway " + a + " connects to town");
                }
            }
        });
        test("10.30: Massive maps have more towns and a big army base out in the country with checkpoints", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.civic = 25; // ((11.0) Map version 25: the country round the town as it was before the thin band.)
                c.v[CityConfig.OPT_PRESET] = 1;
                c.v[CityConfig.OPT_SIZE] = CityConfig.MASSIVE;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 5;
                c.normalize();
                c.seed = 5;
                check(c.outskirtsBase(), "a Massive map gets a base out in the country");
                World w = new World(c);
                w.populate(c);
                City city = w.city;
                City.Facility base = null;
                int towns = 0;
                for (City.Facility f : city.facilities) {
                    if (f.kind == City.FACILITY_BASE && f.satellite) base = f;
                    if (f.kind == City.FACILITY_POLICE && f.satellite) towns++;
                }
                check(base != null, "the big base out in the country");
                check(towns >= 4, "four or more small towns with their own police (" + towns + ")");
                check(!city.inTown(base.x, base.y), "the base is out of town");
                check(!city.checkpoints.isEmpty() && !city.checkpointPosts.isEmpty(), "checkpoints on the road to the base");
                // Its gate is on the road network: trucks can drive from it into town.
                int[] f = new int[city.w * city.h];
                float[] mid = city.nearestDrivable((city.townX0 + city.townX1) / 2f * City.T, (city.townY0 + city.townY1) / 2f * City.T);
                city.driveField(f, mid[0], mid[1]);
                float[] gate = city.nearestDrivable(base.gateX, base.gateY);
                check(gate != null && f[city.tileIndex(gate[0], gate[1])] < City.FAR, "the base's road leads into town");
                for (float[] cp : city.checkpoints) {
                    int guards = 0, soldiers = 0;
                    for (Entity e : w.entities)
                        if (e.type == Entity.SOLDIER && Math.hypot(e.x - cp[0], e.y - cp[1]) < 80) guards++;
                    for (Entity e : w.entities)
                        if (e.type == Entity.SOLDIER && Math.hypot(e.x - base.x, e.y - base.y) < 600) soldiers++;
                    check(guards >= 1, "soldiers on the checkpoint");
                    check(soldiers >= 10, "the base has a garrison (" + soldiers + ")");
                }
                // ((10.52) A Large map has a smaller one now; a Medium one none.)
                CityConfig l = new CityConfig();
                l.civic = 25; // ((11.0) Map version 25: the country round the town as it was before the thin band.)
                l.v[CityConfig.OPT_PRESET] = 8;
                l.v[CityConfig.OPT_SIZE] = CityConfig.LARGE;
                l.normalize();
                check(l.outskirtsBase(), "a country base on a Large map too");
                l.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                check(!l.outskirtsBase(), "none on a Medium map");
            }
        });
        test("10.32: bridges over the highway, a big map saves and loads, evacuation, fallback bases", new Check() {
            public void run() throws Exception {
                // Bridges at the interchanges: the road goes over, the highway runs on under it untouched.
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_PRESET] = 8;
                c.seed = 7;
                c.normalize();
                c.seed = 7;
                City city = new City(c, 0.05f);
                check(!city.overpasses.isEmpty(), "bridges over the highway (" + city.overpasses.size() + ")");
                check(!city.bridgeLink.isEmpty(), "their ends linked for routes");
                int bad = 0;
                for (java.util.Map.Entry<Integer, Integer> e : city.bridgeLink.entrySet())
                    if (city.bridgeTo(e.getValue()) != e.getKey() || city.tiles[e.getKey()] != City.ROAD) bad++;
                check(bad == 0, "every bridge is a road both ways (" + bad + " not)");
                // From town over a bridge to the far carriageway, and from the highway into town.
                boolean ew = city.hwyAxis == 0;
                int len = ew ? city.w : city.h;
                int[] f = new int[city.w * city.h];
                float[] mid = city.nearestDrivable((city.townX0 + city.townX1) / 2f * City.T, (city.townY0 + city.townY1) / 2f * City.T);
                city.driveField(f, mid[0], mid[1]);
                for (int a : new int[]{1, 5}) {
                    int x = ew ? len / 2 : city.hwyAt + a, y = ew ? city.hwyAt + a : len / 2;
                    check(f[y * city.w + x] < City.FAR, "into town from carriageway " + a);
                }
                // A Massive city, its bases, checkpoints and thousands of people, saves and loads back.
                CityConfig m = new CityConfig();
                m.civic = 25; // ((11.0) Map version 25: the country round the town as it was before the thin band.)
                m.v[CityConfig.OPT_PRESET] = 8;
                m.v[CityConfig.OPT_SIZE] = CityConfig.HUGE;
                m.v[CityConfig.OPT_ZOMBIES] = 0;
                m.seed = 5;
                m.normalize();
                m.seed = 5;
                World w = new World(m);
                w.populate(m);
                check(!w.city.checkpoints.isEmpty(), "the country base has its checkpoints");
                int bases = 0;
                for (City.Facility fa : w.city.facilities) if (fa.kind == City.FACILITY_BASE) bases++;
                check(bases >= 2, "two army bases (" + bases + ")");
                for (int i = 0; i < 30 * 20; i++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                // The town's base falls: command moves to the one still standing.
                City.Facility main = w.city.mainBase();
                main.fallen = true;
                City.Facility next = w.city.mainBase();
                check(next != null && next != main && !next.fallen, "a fallen base hands over to the next");
                Entity z = w.spawn(Entity.ZOMBIE, mid[0], mid[1]);
                z.armored = true;
                w.timeline("Test moment");
                w.fleet.pumpFuel()[0] = 2.5f;
                File file = File.createTempFile("zcs", ".dat");
                SaveGame.save(w, file);
                World l = SaveGame.load(file);
                file.delete();
                check(l.city.cfg.code().equals(w.city.cfg.code()) && l.city.w == w.city.w, "the same Massive city");
                check(l.entities.size() > w.entities.size() * 9 / 10, "its people come back (" + l.entities.size() + " of " + w.entities.size() + ")");
                check(l.city.checkpoints.size() == w.city.checkpoints.size(), "and its checkpoints");
                boolean armored = false;
                for (Entity e : l.entities) if (e.isZombie() && e.armored) armored = true;
                check(armored, "armoured dead stay armoured");
                boolean moment = false;
                for (World.Moment mo : l.timeline) if (mo.text.equals("Test moment")) moment = true;
                check(moment, "the timeline is kept");
                int fallen = 0;
                for (City.Facility fa : l.city.facilities) if (fa.fallen) fallen++;
                check(fallen == 1 && l.city.mainBase() != null && !l.city.mainBase().fallen, "the fallen base stays fallen");
                check(l.fleet.pumpFuel != null && Math.abs(l.fleet.pumpFuel[0] - 2.5f) < 0.01f, "the pumps keep what fuel they had");
                // On the emergency broadcast, people get in their cars and leave.
                l.alert = 2;
                for (int i = 0; i < 30 * 10 && !l.fleet.evacStarted; i++) {
                    l.update(1 / 30f);
                    l.evCount = 0;
                }
                check(l.fleet.evacStarted, "the evacuation starts");
            }
        });
        test("10.33: nothing joins the highway, more police and army, fuller maps, several outbreaks shared out", new Check() {
            public void run() throws Exception {
                // Nothing but the slip roads and bridges (and the turn-round at each end of the map) meets the
                // highway, and no house fronts onto it.
                for (int[] ps : new int[][]{{9, 4}, {1, 5}, {8, 3}}) {
                    CityConfig c = new CityConfig();
                    c.v[CityConfig.OPT_PRESET] = ps[0];
                    c.seed = ps[1];
                    c.normalize();
                    c.seed = ps[1];
                    City city = new City(c, 0.02f);
                    if (city.oneWay == null) continue;
                    java.lang.reflect.Field sf = City.class.getDeclaredField("slipTile");
                    sf.setAccessible(true);
                    boolean[] slip = (boolean[]) sf.get(city);
                    int len = city.hwyAxis == 0 ? city.w : city.h, touching = 0, doors = 0;
                    for (int y = 0; y < city.h; y++)
                        for (int x = 0; x < city.w; x++) {
                            int i = y * city.w + x, along = city.hwyAxis == 0 ? x : y;
                            if (city.oneWay[i] != 0 || along < 3 || along >= len - 3) continue;
                            byte t = city.tiles[i];
                            if (t != City.ROAD && t != City.DIRT && t != City.SIDEWALK && t != City.LOT) continue;
                            if ((slip != null && slip[i]) || city.bridgeTo(i) >= 0) continue;
                            boolean adj = false;
                            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                                int nx = x + d[0], ny = y + d[1];
                                if (nx >= 0 && ny >= 0 && nx < city.w && ny < city.h && city.oneWay[ny * city.w + nx] != 0) adj = true;
                            }
                            if (adj) touching++;
                        }
                    for (City.Building b : city.buildings) {
                        if (b.doorX <= 0) continue;
                        int tx = (int) (b.doorX / City.T), ty = (int) (b.doorY / City.T);
                        for (int dy = -2; dy <= 2; dy++)
                            for (int dx = -2; dx <= 2; dx++) {
                                int nx = tx + dx, ny = ty + dy;
                                if (nx >= 0 && ny >= 0 && nx < city.w && ny < city.h && city.oneWay[ny * city.w + nx] != 0) {
                                    doors++;
                                    dx = dy = 3;
                                }
                            }
                    }
                    check(touching == 0, "map " + ps[0] + ": no road meets the highway but the slip roads (" + touching + ")");
                    check(doors == 0, "map " + ps[0] + ": no front door on the highway (" + doors + ")");
                }
                // Fuller country: the city's own map is mostly town and towns now.
                CityConfig g = new CityConfig();
                g.v[CityConfig.OPT_PRESET] = 1;
                g.seed = 5;
                g.normalize();
                g.seed = 5;
                City full = new City(g, 0.02f);
                int green = 0;
                for (byte t : full.tiles) if (t == City.GRASS || t == City.TREE || t == City.ROCK || t == City.WATER) green++;
                check(green * 100 / full.tiles.length < 40, "under 40% of the map is fields and woods (" + green * 100 / full.tiles.length + "%)");
                // More police and soldiers.
                CityConfig m = new CityConfig();
                m.v[CityConfig.OPT_PRESET] = 8;
                m.v[CityConfig.OPT_SIZE] = CityConfig.HUGE;
                m.v[CityConfig.OPT_ZOMBIES] = 0;
                m.seed = 3;
                m.normalize();
                m.seed = 3;
                World w = new World(m);
                w.setMaxPopulation(0);
                w.populate(m);
                int cops = 0, soldiers = 0;
                for (Entity e : w.entities) {
                    if (e.type == Entity.COP) cops++;
                    if (e.type == Entity.SOLDIER) soldiers++;
                }
                // (Counting the officers out in their patrol cars too.)
                for (Fleet.Vehicle v : w.fleet.vehicles)
                    for (Entity e : v.crew) {
                        if (e.type == Entity.COP) cops++;
                        if (e.type == Entity.SOLDIER) soldiers++;
                    }
                check(cops >= 300, "plenty of police on a Massive map (" + cops + ")");
                check(soldiers >= 80, "and soldiers (" + soldiers + ")");
                // (10.35) Always more police than soldiers, by a long way.
                check(cops >= soldiers * 2, "far more police than soldiers (" + cops + " against " + soldiers + ")");
                // Three outbreaks at once round the city: every one of them gets officers.
                City city = w.city;
                float[][] spots = {{0.25f, 0.3f}, {0.75f, 0.35f}, {0.5f, 0.75f}};
                float[][] at = new float[3][];
                for (int s = 0; s < 3; s++) {
                    float x = city.townX0 + (city.townX1 - city.townX0) * spots[s][0], y = city.townY0 + (city.townY1 - city.townY0) * spots[s][1];
                    at[s] = city.nearestDrivable(x * City.T, y * City.T);
                    for (int k = 0; k < 8 + s * 4; k++) w.spawn(Entity.ZOMBIE, at[s][0] + (k % 4) * 8, at[s][1] + (k / 4) * 8);
                }
                boolean[] served = new boolean[3];
                for (int f = 0; f < 30 * 60; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                    for (Dispatch.Incident inc : w.dispatch.incidents)
                        for (int s = 0; s < 3; s++)
                            if (Math.hypot(inc.x - at[s][0], inc.y - at[s][1]) < 400 && inc.cops + w.fleet.inbound(inc) > 0) served[s] = true;
                    // (Or it's already been put down.)
                    if (f > 30 * 5) for (int s = 0; s < 3; s++) if (w.countZombiesNear(at[s][0], at[s][1], 400) == 0) served[s] = true;
                    if (served[0] && served[1] && served[2]) break;
                }
                for (int s = 0; s < 3; s++) check(served[s], "outbreak " + (s + 1) + " of 3 gets officers");
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
        test("10.41: nothing joins a ramp halfway, every ramp drivable; country police stations run their own cars", new Check() {
            public void run() throws Exception {
                for (int[] ps : new int[][]{{0, 3}, {2, 3}, {6, 3}}) {
                    CityConfig c = new CityConfig();
                    c.v[CityConfig.OPT_PRESET] = ps[0];
                    c.seed = ps[1];
                    c.normalize();
                    c.seed = ps[1];
                    City city = new City(c, 0.05f);
                    String m = "map " + ps[0] + "/" + ps[1] + ": ";
                    check(city.slipWay != null, m + "slip roads");
                    int w = city.w, lanes = 0, stranded = 0;
                    for (int i = w; i < w * (city.h - 1); i++) {
                        if (city.slipWay[i] == 0) continue;
                        for (int k = 0; k < 4; k++) {
                            int j = i + (k == 0 ? 1 : k == 1 ? -1 : k == 2 ? w : -w);
                            // (Where the ramps meet the road that crosses the highway is what they're for.)
                            int kk = city.hwyAxis == 0 ? j % w : j / w;
                            boolean exitRoad = false;
                            for (int[] e : city.hwyExits) if (kk >= e[0] - 1 && kk <= e[0] + e[1]) exitRoad = true;
                            if (!exitRoad && city.slipWay[j] == 0 && (city.tiles[j] == City.DIRT || (city.tiles[j] == City.LOT && city.drive != null && city.drive[j]))) lanes++;
                        }
                    }
                    check(lanes == 0, m + "no lane or drive joins an on or off ramp (" + lanes + ")");
                    int[] f = new int[w * city.h];
                    // (From a road near the middle of town: the very middle can be a park.)
                    float[] mid = null;
                    for (int s = 0; s < 12 && mid == null; s++)
                        mid = city.nearestDrivable(((city.townX0 + city.townX1) / 2f + s * 10) * City.T, (city.townY0 + city.townY1) / 2f * City.T);
                    city.driveField(f, mid[0], mid[1]);
                    for (int i = 0; i < w * city.h; i++) if (city.slipWay[i] != 0 && city.tiles[i] == City.ROAD && f[i] >= City.FAR) stranded++;
                    check(stranded == 0, m + "every ramp leads somewhere (" + stranded + " tiles of ramp lead nowhere)");
                }
                // A small town's police station out in the country: two cars of its own, on its own patch.
                CityConfig c = new CityConfig();
                c.civic = 25; // ((11.0) Map version 25: the country round the town as it was before the thin band.)
                c.v[CityConfig.OPT_PRESET] = 0;
                c.v[CityConfig.OPT_SIZE] = CityConfig.MASSIVE;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 3;
                c.normalize();
                c.seed = 3;
                World w = new World(c);
                w.populate(c);
                City.Facility sat = null;
                for (City.Facility f : w.city.facilities) if (f.kind == City.FACILITY_POLICE && f.satellite) sat = f;
                check(sat != null, "a country police station");
                for (int f = 0; f < 30 * 30; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                int own = 0, near = 0;
                for (Fleet.Vehicle v : w.fleet.vehicles)
                    if (v.station == sat && v.patrol && !v.broken) {
                        own++;
                        if (Math.hypot(v.x - sat.x, v.y - sat.y) < 1500) near++;
                    }
                check(own >= 2 && near >= 2, sat.name + " has its own cars round it (" + near + " of " + own + ")");
                // Its officers sent a long way take one of its cars.
                // (Since 11.1 they're out on the beat round it: the nearest two.)
                java.util.List<Entity> two = new java.util.ArrayList<Entity>();
                for (int k = 0; k < 2; k++) {
                    Entity best = null;
                    for (Entity e : w.entities)
                        if (e.type == Entity.COP && e.task == Dispatch.T_NONE && e.rig == null && !two.contains(e) && Math.hypot(e.x - sat.x, e.y - sat.y) < 900
                                && (best == null || Math.hypot(e.x - sat.x, e.y - sat.y) < Math.hypot(best.x - sat.x, best.y - sat.y))) best = e;
                    if (best != null) two.add(best);
                }
                check(two.size() == 2, "officers on the beat round the station");
                float[] far = null;
                java.util.Random r = new java.util.Random(4);
                for (int k = 0; k < 2000 && far == null; k++) {
                    float an = r.nextFloat() * 6.283f, d = 700 + r.nextFloat() * 300;
                    float[] p = w.city.nearestDrivable(sat.x + (float) Math.cos(an) * d, sat.y + (float) Math.sin(an) * d);
                    if (p != null && w.fleet.canDrive(sat.gateX > 0 ? sat.gateX : sat.x, sat.gateY > 0 ? sat.gateY : sat.y, p[0], p[1])) far = p;
                }
                check(far != null, "a road a way off");
                Dispatch.Incident inc = new Dispatch.Incident();
                inc.x = far[0];
                inc.y = far[1];
                inc.place = "the test";
                // ((11.7) One of the cars parked there - none appear from nowhere.)
                int before = w.fleet.vehicles.size(), parked = 0;
                for (Fleet.Vehicle v : w.fleet.vehicles) if (v.standby && v.type == Fleet.CRUISER && Math.hypot(v.x - sat.x, v.y - sat.y) < sat.r + 300) parked++;
                check(parked >= 2, "the station has cars parked on its lot (" + parked + ")");
                check(w.fleet.stationCar(two, sat, inc) && w.fleet.vehicles.size() == before, "one of the station's cars for them");
                Fleet.Vehicle car = w.fleet.vehicles.get(w.fleet.vehicles.size() - 1);
                boolean rolled = false;
                for (int f = 0; f < 30 * 60 && !rolled; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                    rolled = car.crew.size() == 2 && car.speed > 5;
                }
                check(rolled, "they get in and drive off (crew " + car.crew.size() + ")");
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
        test("10.47: with Fleeing citizens on, people leave the city once the news breaks and come home once it's over", new Check() {
            public void run() throws Exception {
                for (int on = 0; on < 2; on++) {
                    CityConfig c = new CityConfig();
                    c.v[CityConfig.OPT_PRESET] = 0;
                    c.seed = 5;
                    c.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                    c.v[CityConfig.OPT_ZOMBIES] = 0;
                    c.normalize();
                    c.seed = 5;
                    c.exodus = on;
                    check(c.get(CityConfig.OPT_EXODUS) == on && c.values(CityConfig.OPT_EXODUS).length == 4, "the option reads back");
                    World w = new World(c);
                    w.rnd.setSeed(4);
                    w.populate(c);
                    for (int f = 0; f < 150; f++) w.update(1 / 30f);
                    java.util.Random r = new java.util.Random(7);
                    float[] q0 = w.city.randomWalkableInTown(r);
                    int sp = 0;
                    for (int i = 0; i < 1600 && sp < 40; i++) {
                        float[] q = w.city.findWalkable(q0[0] + (i % 40) * 8 - 160, q0[1] + (i / 40) * 8 - 160);
                        if (q != null && w.spawn(Entity.ZOMBIE, q[0], q[1]) != null) sp++;
                    }
                    w.alert = 2;
                    w.outbreak = true;
                    int leaving = 0;
                    for (int f = 0; f < 30 * 90; f++) {
                        w.update(1 / 30f);
                        if (f % 30 == 0) for (Entity e : w.entities) if (!e.dead && e.task == Dispatch.T_LEAVE) leaving++;
                        // ((10.52) Once a few are gone: over the beltway the outbreak can be over, and them home again,
                        // inside the minute and a half.)
                        if (on == 1 && w.away.size() >= 5) break;
                    }
                    if (on == 0) {
                        check(w.fled == 0 && leaving == 0 && w.away.isEmpty(), "with it off nobody leaves (" + w.fled + ")");
                        continue;
                    }
                    check(leaving > 0 && w.fled > 0 && !w.away.isEmpty(), "with it on people head out of the city and leave it (" + w.fled + " gone)");
                    // Saved and loaded: those away are still away.
                    File f = File.createTempFile("exodus", ".sav");
                    SaveGame.save(w, f);
                    World l = SaveGame.load(f);
                    f.delete();
                    check(l.city.cfg.exodus() && l.away.size() == w.away.size(), "a saved game keeps the option and who's away (" + l.away.size() + ")");
                    // Over: they come home.
                    for (int k = 0; k < 30 * 200 && (w.aftermath != World.AFTER_RECOVERY || w.afterTime < 90); k++) {
                        if (k % 15 == 0) w.clearZombies();
                        w.update(1 / 30f);
                    }
                    check(w.aftermath == World.AFTER_RECOVERY, "the outbreak ends");
                    int still = 0;
                    for (Entity e : w.entities) if (!e.dead && e.task == Dispatch.T_LEAVE) still++;
                    check(still == 0, "nobody's still leaving once it's over (" + still + ")");
                    check(w.cameHome > 0, "and those who fled come home (" + w.cameHome + " of " + w.fled + ")");
                }
            }
        });
        test("10.48: an empty police car is taken by officers on foot nearby; (10.49) people leave by evacuation bus and train", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                // (A Metropolis with a railway station: buses and trains both.)
                c.v[CityConfig.OPT_PRESET] = 8;
                c.seed = 1;
                c.v[CityConfig.OPT_SIZE] = CityConfig.LARGE;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.normalize();
                c.seed = 1;
                c.exodus = CityConfig.EXODUS_TRANSIT;
                check(c.values(CityConfig.OPT_EXODUS).length == 4, "four ways to leave (off, on foot, bus & train, all)");
                World w = new World(c);
                w.rnd.setSeed(4);
                w.populate(c);
                for (int f = 0; f < 150; f++) w.update(1 / 30f);
                // A patrol car whose crew is gone, parked in the street, and two officers with nothing to do near it.
                Fleet.Vehicle car = null;
                for (Fleet.Vehicle v : w.fleet.vehicles) if (v.patrol && v.type == Fleet.CRUISER && !v.crew.isEmpty() && v.kind != Fleet.K_BIKE) car = v;
                check(car != null, "a patrol car to use");
                car.crew.clear();
                car.parked = true;
                car.state = Fleet.ABANDONED;
                car.speed = 0;
                java.util.List<Entity> cops = new java.util.ArrayList<Entity>();
                for (int k = 0; k < 2; k++) {
                    float[] q = w.city.findWalkable(car.x + 40 + k * 20, car.y + 30);
                    Entity e = q == null ? null : w.spawnCop(0, q[0], q[1]);
                    // ((11.4) A pair on a foot beat: the rest go back to the precinct for a car of their own.)
                    if (e != null) e.callsign = 8 + k;
                    if (e != null) cops.add(e);
                }
                check(!cops.isEmpty(), "officers on foot near it");
                boolean taken = false;
                for (int f = 0; f < 30 * 40 && !taken; f++) {
                    w.update(1 / 30f);
                    // ((11.4) Or other officers on their way back to the precinct for a car: they want one too.)
                    for (Entity e : car.crew) if (e.type == Entity.COP) taken = true;
                }
                check(taken, "they take the car (state " + car.state + ", crew " + car.crew.size() + ")");
                // Bus and train: once the news breaks, people wait at the stops and on the platform, and get out of the
                // city aboard.
                check(w.city.stationX >= 0, "a station on this map");
                // ((10.52) Eighty of the dead, nearer the middle: with another police station on the map fifty were all
                // put down before anyone was on their way.)
                java.util.Random r = new java.util.Random(3);
                float[] q0 = w.city.randomWalkableInTown(r);
                int sp = 0;
                for (int i = 0; i < 1600 && sp < 80; i++) {
                    float[] q = w.city.findWalkable(q0[0] + (i % 40) * 8 - 160, q0[1] + (i / 40) * 8 - 160);
                    if (q != null && w.spawn(Entity.ZOMBIE, q[0], q[1]) != null) sp++;
                }
                w.alert = 2;
                w.outbreak = true;
                int bus = 0, train = 0, onFoot = 0;
                for (int f = 0; f < 30 * 150 && (bus == 0 || train == 0 || w.fledByTransit == 0); f++) {
                    // ((10.52) Kept going: with the police that much stronger the dead can all be down before the
                    // train comes in, and the city calms down.)
                    w.alert = 2;
                    w.outbreak = true;
                    w.update(1 / 30f);
                    if (f % 30 != 0) continue;
                    for (Fleet.Vehicle v : w.fleet.vehicles) {
                        if (v.evacStop >= 0) bus = Math.max(bus, v.riders.size());
                        if (v.type == Fleet.TRAIN) train = Math.max(train, v.riders.size());
                    }
                    for (Entity e : w.entities) if (!e.dead && e.task == Dispatch.T_LEAVE && e.exitStop == -1) onFoot++;
                }
                check(!w.evacStops.isEmpty(), "evacuation bus stops around town (" + w.evacStops.size() + ")");
                check(bus > 0, "people get on an evacuation bus");
                check(train > 0, "and on an evacuation train at the station (" + bus + " on a bus, " + w.fledByTransit + " gone, " + w.alert + "/" + w.outbreak + ")");
                check(w.fledByTransit > 0 && w.fled == w.fledByTransit, "and leave the city aboard (" + w.fledByTransit + " of " + w.fled + ")");
                check(onFoot == 0, "nobody walks out with only buses and trains chosen (" + onFoot + ")");
            }
        });
        test("11.0: Marines by boat on the island maps, a thin band of country, the new units", new Check() {
            public void run() throws Exception {
                for (int p : new int[]{CityConfig.ISLANDS}) {
                    CityConfig c = new CityConfig();
                    c.v[CityConfig.OPT_PRESET] = p;
                    c.v[CityConfig.OPT_SIZE] = CityConfig.LARGE;
                    c.v[CityConfig.OPT_ZOMBIES] = 0;
                    c.normalize();
                    c.seed = 5;
                    World w = new World(c);
                    City city = w.city;
                    String name = CityConfig.PRESETS[p];
                    int water = 0;
                    for (int i = 0; i < city.w * city.h; i++) if (city.tiles[i] == City.WATER) water++;
                    check(water > city.w * city.h / 12, name + ": plenty of water (" + water + ")");
                    check(city.buildings.size() > 300, name + ": towns on the islands (" + city.buildings.size() + ")");
                    // Marines come in over the water to the shore by the fighting, and get out on dry land.
                    // (A street the sea can reach: one a long way inland has no landing near it.)
                    java.util.Random qr = new java.util.Random(3);
                    boolean landed = false;
                    for (int k = 0; k < 12 && !landed; k++) {
                        float[] q = city.randomStreetSpot(qr);
                        landed = w.fleet.marineLanding(16, q[0], q[1], "here", null);
                    }
                    check(landed, name + ": a way in by water");
                    int boats = 0;
                    for (Fleet.Vehicle v : w.fleet.vehicles) if (v.type == Fleet.BOAT) boats++;
                    check(boats == 2, name + ": two landing craft (" + boats + ")");
                    for (int f = 0; f < 30 * 90; f++) w.update(1 / 30f);
                    int marines = 0, wet = 0;
                    for (Entity e : w.entities)
                        if (!e.dead && e.role == Entity.ROLE_MARINE) {
                            marines++;
                            int t = city.tileIndex(e.x, e.y);
                            if (t >= 0 && city.tiles[t] == City.WATER) wet++;
                        }
                    // (Ashore and since given a lift by a truck counts too.)
                    for (Fleet.Vehicle v : w.fleet.vehicles)
                        if (v.type != Fleet.BOAT && !v.broken) for (Entity e : v.crew) if (e.role == Entity.ROLE_MARINE && e.hp > 0) marines++;
                    check(marines >= 12, name + ": the Marines ashore (" + marines + ")");
                    check(wet == 0, name + ": none of them left in the water (" + wet + ")");
                    CityConfig back = new CityConfig();
                    check(back.applyCode(c.code()) && back.v[CityConfig.OPT_PRESET] == p, name + ": its city code");
                }
                // A thin band of country round every other map.
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_PRESET] = 8;
                c.v[CityConfig.OPT_SIZE] = CityConfig.LARGE;
                c.normalize();
                check(c.coreFraction() >= 0.85f, "nine tenths town (" + c.coreFraction() + ")");
                // The new roles.
                // ((14.0) And the combat engineers and the mortar teams.)
                // ((18.1) And the negotiator.)
                check(Entity.ROLE_NAMES.length == 23 && "Negotiator".equals(Entity.ROLE_NAMES[Entity.ROLE_NEGOTIATOR]) && "Prison guard".equals(Entity.ROLE_NAMES[Entity.ROLE_PRISON_GUARD]) && "Mounted officer".equals(Entity.ROLE_NAMES[Entity.ROLE_MOUNTED]) && "Marine".equals(Entity.ROLE_NAMES[Entity.ROLE_MARINE])
                        && "Combat engineer".equals(Entity.ROLE_NAMES[Entity.ROLE_ENGINEER]) && "Mortar team".equals(Entity.ROLE_NAMES[Entity.ROLE_MORTAR]), "every role named");
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
        test("13.7: a truck waits for its squad to run to it from a long way off, and leaves once they're all aboard", new Check() {
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
                w.clearZombies();
                City.Facility base = w.city.mainBase();
                check(base != null, "the city has a base");
                java.util.List<Entity> squad = new java.util.ArrayList<Entity>();
                for (Entity e : w.entities)
                    if (squad.size() < 4 && !e.dead && e.type == Entity.SOLDIER && e.rig == null && Math.hypot(e.x - base.x, e.y - base.y) < base.r + 200) squad.add(e);
                check(squad.size() == 4, "four soldiers at the base");
                // Somewhere a long run from the base (most of a minute and a half on foot), and a call the other way.
                java.util.Random r = new java.util.Random(3);
                float[] spot = null, call = null;
                for (int k = 0; k < 4000 && (spot == null || call == null); k++) {
                    float an = r.nextFloat() * 6.283f;
                    float[] p = w.city.findWalkable(base.x + (float) Math.cos(an) * 1500, base.y + (float) Math.sin(an) * 1500);
                    if (spot == null) {
                        if (p != null && Math.hypot(p[0] - base.x, p[1] - base.y) > 1350 && w.city.findPath(p[0], p[1], base.x, base.y, 9000) != null) spot = p;
                        continue;
                    }
                    float[] q = w.city.nearestDrivable(base.x - (float) Math.cos(an) * 1500, base.y - (float) Math.sin(an) * 1500);
                    if (q != null && Math.hypot(q[0] - base.x, q[1] - base.y) > 1200 && w.fleet.canDrive(base.x, base.y, q[0], q[1])) call = q;
                }
                check(spot != null && call != null, "a spot a long run off, and a call");
                for (int i = 0; i < squad.size(); i++) {
                    Entity e = squad.get(i);
                    e.task = Dispatch.T_NONE;
                    e.x = spot[0] + (i % 2) * 6;
                    e.y = spot[1] + (i / 2) * 6;
                    e.path = null;
                }
                Dispatch.Incident inc = new Dispatch.Incident();
                inc.x = call[0];
                inc.y = call[1];
                inc.place = "the test";
                inc.zombiesNear = 1;
                w.dispatch.incidents.add(inc);
                int before = w.fleet.vehicles.size();
                check(w.fleet.troopTruck(squad, base, inc), "a truck for them from the base");
                Fleet.Vehicle truck = null;
                for (int i = before; i < w.fleet.vehicles.size(); i++) if (w.fleet.vehicles.get(i).state == Fleet.MUSTER) truck = w.fleet.vehicles.get(i);
                check(truck != null, "the truck waits at the base");
                boolean left = false;
                float waited = 0;
                for (int f = 0; f < 30 * 120 && !left; f++) {
                    inc.resolved = false;
                    inc.age = 0;
                    w.update(1 / 30f);
                    w.evCount = 0;
                    waited += 1 / 30f;
                    left = truck.state != Fleet.MUSTER;
                }
                String how = "";
                for (Entity e : squad) how += " [task " + e.task + (e.rig == truck ? " this truck" : e.rig == null ? " no truck" : " another") + ", " + (int) Math.hypot(e.x - truck.x, e.y - truck.y) + " off]";
                check(left && waited > 45, "it waited for them (" + (int) waited + " s, state " + truck.state + ", crew " + truck.crew.size() + how + ")");
                check(truck.crew.size() == 4, "all four aboard when it left (" + truck.crew.size() + how + ")");
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
        test("14.2: after the outbreak, fallen buildings are rebuilt one at a time", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 61;
                c.normalize();
                c.seed = 61;
                World w = new World(c);
                w.populate(c);
                City.Building b = null;
                for (City.Building x : w.city.buildings)
                    if (b == null && !x.collapsed && x.capacity > 0 && x.height > 0 && x.name != null && x.x1 - x.x0 > 30) b = x;
                check(b != null, "a building to knock down");
                float height = b.height;
                int capacity = b.capacity;
                w.city.collapse(b);
                check(b.collapsed && b.height == 0, "it falls");
                // Someone standing on the rubble: not built over them.
                Entity stander = w.spawn(Entity.CIVILIAN, (b.x0 + b.x1) / 2, (b.y0 + b.y1) / 2);
                java.lang.reflect.Method sa = World.class.getDeclaredMethod("startAftermath", int.class);
                sa.setAccessible(true);
                sa.invoke(w, World.AFTER_RECOVERY);
                boolean site = false, early = false;
                // ((15.1) Time enough after they step off: anyone walking across the site holds the work up too.)
                for (int f = 0; f < 30 * 320 && b.collapsed; f++) {
                    stander.x = (b.x0 + b.x1) / 2;
                    stander.y = (b.y0 + b.y1) / 2;
                    if (f > 30 * 175) stander.dead = stander.removed = true;
                    w.update(1 / 30f);
                    w.evCount = 0;
                    if (b.rebuildT > 0) site = true;
                    if (!b.collapsed && f <= 30 * 175) early = true;
                }
                check(site, "a building site while the work goes on");
                check(!early, "not built over someone standing on the site");
                check(!b.collapsed && b.height == height && b.capacity == capacity, "rebuilt as it was (" + b.height + "/" + height + ", " + b.capacity + "/" + capacity + ")");
                int tx = (int) ((b.x0 + b.x1) / 2 / City.T), ty = (int) ((b.y0 + b.y1) / 2 / City.T);
                check(w.city.solid[ty * w.city.w + tx], "solid walls again");
                java.io.File f = java.io.File.createTempFile("zcsb", ".dat");
                SaveGame.save(w, f);
                World l = SaveGame.load(f);
                f.delete();
                City.Building lb = l.city.buildings.get(w.city.buildings.indexOf(b));
                check(!lb.collapsed, "still standing after a save");
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
        test("14.4: the timeline shows the moments and takes you where they happened; the sounds of the city", new Check() {
            public void run() throws Exception {
                GameView v = new GameView(new android.app.Activity());
                int[] size = {1080, 2400};
                v.layout(size[0], size[1]);
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 3;
                v.startGame(c);
                v.awaitLoad();
                ((Menu) field(v, "menu")).screen = Menu.NONE;
                // (The first frame takes in the city that's been built.)
                for (int k = 0; k < 3; k++) draw(v, size);
                ((Menu) field(v, "menu")).screen = Menu.NONE;
                World w = (World) field(v, "world");
                float mx = w.city.worldW() * 0.3f, my = w.city.worldH() * 0.7f;
                w.timeline("Something happened everywhere");
                w.timeline("Something happened here", mx, my);
                // Saved with where.
                java.io.File f = java.io.File.createTempFile("zcst", ".dat");
                SaveGame.save(w, f);
                World l = SaveGame.load(f);
                f.delete();
                World.Moment lm = l.timeline.get(l.timeline.size() - 1);
                check(Math.abs(lm.x - mx) < 0.1f && Math.abs(lm.y - my) < 0.1f && Float.isNaN(l.timeline.get(l.timeline.size() - 2).x), "moments saved with where they happened");
                // The panel draws, and tapping the newest (top) row goes there.
                for (int k = 0; k < 3; k++) draw(v, size);
                ((Menu) field(v, "menu")).screen = Menu.NONE;
                Field open = GameView.class.getDeclaredField("timeOpen");
                open.setAccessible(true);
                open.setBoolean(v, true);
                draw(v, size);
                check(v.drawErrors == 0, "the timeline draws");
                @SuppressWarnings("unchecked")
                java.util.List<RectF> rows = (java.util.List<RectF>) field(v, "timeRows");
                check(rows.size() >= 2, "both moments listed (" + rows.size() + ")");
                java.lang.reflect.Method tap = GameView.class.getDeclaredMethod("timelineTap", float.class, float.class);
                tap.setAccessible(true);
                tap.invoke(v, rows.get(0).centerX(), rows.get(0).centerY());
                Field camX = GameView.class.getDeclaredField("camX"), scale = GameView.class.getDeclaredField("scale");
                camX.setAccessible(true);
                scale.setAccessible(true);
                float centre = camX.getFloat(v) + size[0] / scale.getFloat(v) / 2;
                check(Math.abs(centre - mx) < 60 && !open.getBoolean(v), "tapping it goes there (" + (int) centre + " vs " + (int) mx + ")");
                // The city's sounds: the heartbeat is made like any other.
                check(Synth.effect(Sfx.HEART).length > Synth.RATE, "the heartbeat sound made");
                for (int k = 0; k < 5; k++) draw(v, size);
                check(v.drawErrors == 0, "frames with the new effects draw");
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
        test("14.7: a squad follows the soldier you play and shoots what they shoot; you fly a helicopter and it flies home", new Check() {
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
                for (int f = 0; f < 30; f++) w.update(1 / 30f);
                // The soldier with the most of their own kind round them, out in the open.
                Entity me = null;
                int best = 0;
                for (Entity e : w.entities) {
                    if (e.dead || e.type != Entity.SOLDIER || e.rig != null || e.ride != null) continue;
                    int n = 0;
                    for (Entity o : w.entities)
                        if (o != e && !o.dead && o.type == Entity.SOLDIER && o.rig == null && o.ride == null && Math.hypot(o.x - e.x, o.y - e.y) < World.SQUAD_REACH
                                && (o.task == Dispatch.T_NONE || o.task == Dispatch.T_POST || o.task == Dispatch.T_RESPOND || o.task == Dispatch.T_HOLD)) n++;
                    if (n > best) { best = n; me = e; }
                }
                check(me != null && best >= 2, "a soldier with others near (" + best + ")");
                w.controlled = me;
                w.ctrlAction(World.CA_SQUAD);
                java.util.List<Entity> squad = new java.util.ArrayList<Entity>();
                for (Entity o : w.entities) if (o.commandedBy == me && o.task == Dispatch.T_SQUAD) squad.add(o);
                check(squad.size() >= 2 && squad.size() <= World.SQUAD_MAX, "a squad gathered (" + squad.size() + ")");
                // You walk off; they keep with you.
                float[] dir = {1, 0};
                for (int f = 0; f < 30 * 14; f++) {
                    if (f % 60 == 0) {
                        float a = (f / 60) * 1.3f;
                        dir[0] = (float) Math.cos(a);
                        dir[1] = (float) Math.sin(a);
                    }
                    w.joyX = dir[0] * 0.6f;
                    w.joyY = dir[1] * 0.6f;
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                w.joyX = w.joyY = 0;
                for (int f = 0; f < 30 * 4; f++) { w.update(1 / 30f); w.evCount = 0; }
                int near = 0, still = 0;
                for (Entity o : squad) {
                    if (o.task == Dispatch.T_SQUAD) still++;
                    if (!o.dead && Math.hypot(o.x - me.x, o.y - me.y) < 70) near++;
                }
                check(still == squad.size() && near * 3 >= squad.size() * 2, "they kept with you (" + near + " of " + squad.size() + " near, " + still + " still yours)");
                // Dead ahead: you open fire and so do they.
                java.util.List<Entity> dead = new java.util.ArrayList<Entity>();
                for (int z = 0; z < 6; z++) {
                    float[] p = w.city.findWalkable(me.x + 90 + (z % 3) * 7, me.y + (z / 3) * 7);
                    if (p == null || !w.city.los(me.x, me.y, p[0], p[1])) continue;
                    Entity zz = w.spawn(Entity.ZOMBIE, p[0], p[1]);
                    dead.add(zz);
                }
                int[] fired = new int[squad.size()];
                w.ctrlAttack = true;
                for (int f = 0; f < 30 * 8; f++) {
                    for (Entity zz : dead) zz.speed = zz.runSpeed = 0;
                    for (int i = 0; i < squad.size(); i++) if (squad.get(i).aiming) fired[i]++;
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                w.ctrlAttack = false;
                int shooters = 0, down = 0;
                for (int k : fired) if (k > 0) shooters++;
                for (Entity zz : dead) if (zz.dead || zz.hp <= 0) down++;
                check(dead.size() >= 3 && shooters >= 1 && down * 2 >= dead.size(), "the squad fired with you (" + shooters + " shooting, " + down + " of " + dead.size() + " down)");
                w.ctrlAction(World.CA_SQUAD);
                int left = 0;
                for (Entity o : w.entities) if (o.commandedBy == me || o.task == Dispatch.T_SQUAD) left++;
                check(left == 0, "dismissed, they go back to their orders (" + left + " left)");
                // A helicopter on its pad: in, up, away, out - and it flies home.
                Fleet.Vehicle heli = null;
                for (Fleet.Vehicle v : w.fleet.vehicles) if (v.type == Fleet.HELI && v.state == Fleet.IDLE && !v.broken && v.player == null) heli = v;
                check(heli != null, "a helicopter waiting on a pad");
                float[] by = w.city.findWalkable(heli.x + 8, heli.y);
                me.x = by[0];
                me.y = by[1];
                int[] opts = new int[8];
                int n = w.ctrlOptions(opts);
                boolean canBoard = false;
                for (int i = 0; i < n; i++) if (opts[i] == World.CA_ENTER_CAR) canBoard = true;
                check(canBoard, "you can climb in");
                w.ctrlAction(World.CA_ENTER_CAR);
                check(heli.player == me, "you're flying it");
                float hx = heli.x, hy = heli.y;
                float mx = hx < w.city.worldW() / 2f ? 1 : -1;
                for (int f = 0; f < 30 * 10; f++) {
                    w.joyX = mx;
                    w.joyY = 0;
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                w.joyX = 0;
                float flown = (float) Math.hypot(heli.x - hx, heli.y - hy);
                check(heli.alt > 0.4f && flown > 400, "it took off and flew where you pointed (" + (int) flown + " away, alt " + heli.alt + ")");
                w.ctrlAction(World.CA_EXIT_CAR);
                check(!me.dead && heli.player == null && Math.hypot(me.x - heli.x, me.y - heli.y) < 40, "you're down on the ground by it");
                for (int f = 0; f < 30 * 40; f++) { w.update(1 / 30f); w.evCount = 0; }
                float home = (float) Math.hypot(heli.x - hx, heli.y - hy);
                check(home < flown / 2, "it flew itself back home (" + (int) home + " from the pad, state " + heli.state + ")");
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
        test("14.9: someone in the nook where two buildings meet gets out and round, not into the wall; a pad helicopter puts everyone down", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_PRESET] = 0;
                c.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 79;
                c.normalize();
                c.seed = 79;
                World w = new World(c);
                w.populate(c);
                City city = w.city;
                java.lang.reflect.Method walk = World.class.getDeclaredMethod("walkToSpot", Entity.class, float.class, float.class, float.class);
                walk.setAccessible(true);
                java.lang.reflect.Method move = World.class.getDeclaredMethod("move", Entity.class, float.class);
                move.setAccessible(true);
                java.lang.reflect.Field budget = World.class.getDeclaredField("pathBudget");
                budget.setAccessible(true);
                // Nooks: open ground walled in on two sides by building (west and south), open to the north, with
                // open ground past the building to the west - where they're trying to get to.
                int tried = 0, out = 0;
                for (int ty = 4; ty < city.h - 4 && tried < 12; ty++)
                    for (int tx = 8; tx < city.w - 4 && tried < 12; tx++) {
                        if (city.solidTile(tx, ty) || !city.solidTile(tx - 1, ty) || !city.solidTile(tx, ty + 1) || city.solidTile(tx, ty - 1)
                                || city.solidTile(tx + 1, ty) || city.tiles[(ty) * city.w + tx - 1] != City.BUILDING) continue;
                        int gx = tx - 2;
                        while (gx > 1 && city.solidTile(gx, ty)) gx--;
                        if (tx - gx < 3 || tx - gx > 8 || city.solidTile(gx, ty)) continue;
                        // (Its nearest open ground somewhere other than this very nook.)
                        float[] q = city.findWalkable((tx - 1.5f) * City.T, (ty + 0.5f) * City.T);
                        if (q == null || Math.hypot(q[0] - (tx + 0.5f) * City.T, q[1] - (ty + 0.5f) * City.T) < 20) continue;
                        tried++;
                        // Where they're making for is inside the building itself (a door, a spot by the wall): no way
                        // there as it stands. They used to walk straight at it, into the wall, for good.
                        float sx = (tx + 0.3f) * City.T, sy = (ty + 0.7f) * City.T, dx = (tx - 1.5f) * City.T, dy = (ty + 0.5f) * City.T;
                        float[] near = city.findWalkable(dx, dy);
                        Entity e = w.spawn(Entity.CIVILIAN, sx, sy);
                        float best = 999;
                        // ((17.1) Or, a spot in a building people walk into, in through its door to it.)
                        City.Building inB = city.insideOf(dx, dy);
                        boolean gotIn = false;
                        for (int f = 0; f < 30 * 12; f++) {
                            budget.setFloat(w, 3);
                            walk.invoke(w, e, dx, dy, e.speed);
                            move.invoke(w, e, 1 / 30f);
                            best = Math.min(best, (float) Math.hypot(e.x - near[0], e.y - near[1]));
                            if (inB != null && city.insideOf(e.x, e.y) == inB) gotIn = true;
                        }
                        if (best < 12 || gotIn) out++;
                        e.dead = true;
                        e.removed = true;
                    }
                check(tried >= 3 && out * 3 >= tried * 2, "out of the nook and round to open ground by where they were going (" + out + " of " + tried + ")");
                // Mortar teams dug into pits at the base, kept there rather than sent out with the squads.
                int pits = 0;
                for (Entity e : w.entities) if (!e.dead && e.role == Entity.ROLE_MORTAR && e.task == Dispatch.T_POST) pits++;
                check(pits >= 1, "mortar teams in their pits at the base (" + pits + ")");
                // An army helicopter off its pad with a squad: all of them go down at the landing zone, not all but two.
                Fleet.Vehicle heli = null;
                for (Fleet.Vehicle v : w.fleet.vehicles)
                    if (v.type == Fleet.HELI && v.state == Fleet.IDLE && v.kind != Fleet.K_POLICE_HELI && v.kind != Fleet.K_RESCUE_HELI) heli = v;
                check(heli != null, "an army helicopter on its pad");
                for (int f = 0; f < 30 * 3; f++) w.update(1 / 30f);
                java.util.Random r = new java.util.Random(2);
                float[] at = null;
                for (int k = 0; k < 4000 && at == null; k++) {
                    float[] q = city.randomWalkableInTown(r);
                    if (Math.hypot(q[0] - heli.x, q[1] - heli.y) > 900) at = q;
                }
                // (The dead there, so the call stays live till they're down.)
                java.util.List<Entity> pack = new java.util.ArrayList<Entity>();
                for (int k = 0; k < 5; k++) {
                    float[] p = city.findWalkable(at[0] + k * 6, at[1]);
                    if (p != null) pack.add(w.spawn(Entity.ZOMBIE, p[0], p[1]));
                }
                check(w.fleet.launchHeli(heli, at[0], at[1], "the test", null, 6), "launched");
                int aboard = 0, dropped = -1;
                for (int f = 0; f < 30 * 120 && dropped < 0; f++) {
                    // (Tough enough that the door gunner can't finish them off and send it home before it lands.)
                    for (Entity zz : pack) {
                        zz.speed = zz.runSpeed = 0;
                        zz.hp = zz.maxHp = 5000;
                        zz.dead = false;
                    }
                    int before = heli.crew.size();
                    if (heli.state == Fleet.SPOOL) aboard = Math.max(aboard, heli.crew.size() - 1);
                    w.update(1 / 30f);
                    w.evCount = 0;
                    if (heli.state == Fleet.CIRCLE && before > heli.crew.size()) dropped = before - heli.crew.size();
                }
                check(aboard >= 3 && dropped == aboard, "everyone aboard but the pilot went down (" + dropped + " of " + aboard + ")");
                // The police helicopter's pilot is lost: an officer puts in to fly it, trains on the pad, and is its pilot.
                Fleet.Vehicle ph = null;
                for (Fleet.Vehicle v : w.fleet.vehicles) if (v.type == Fleet.HELI && v.kind == Fleet.K_POLICE_HELI && v.state == Fleet.IDLE) ph = v;
                check(ph != null, "a police helicopter on its pad");
                Entity old = w.fleet.livePilot(ph);
                check(old != null, "with its pilot");
                old.hp = 0;
                old.dead = true;
                Entity trainee = null, flier = null;
                for (int f = 0; f < 30 * 150 && flier == null; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                    if (trainee == null)
                        for (Entity e : w.entities) if (!e.dead && e.task == Dispatch.T_TRAIN && e.pilotFor == ph) trainee = e;
                    Entity p = w.fleet.livePilot(ph);
                    if (p != null && p != old) flier = p;
                }
                check(trainee != null && trainee.type == Entity.COP, "an officer put in to fly it");
                check(flier == trainee && flier.role == Entity.ROLE_PILOT, "and once trained, is its pilot (" + (flier == null ? "none" : flier == trainee ? "them" : "someone else") + ")");
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
        test("15.1: six save slots; lives saved counted; the best police and soldiers go into the hall of fame", new Check() {
            public void run() throws Exception {
                // A soldier puts down one of the dead right on a civilian: a life saved.
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_PRESET] = 0;
                c.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 81;
                c.normalize();
                c.seed = 81;
                World w = new World(c);
                w.populate(c);
                java.util.Random r = new java.util.Random(3);
                float[] at = null;
                for (int k = 0; k < 4000 && at == null; k++) {
                    float[] q = w.city.randomWalkableInTown(r);
                    if (w.city.findWalkable(q[0] + 60, q[1]) != null && w.city.los(q[0], q[1], q[0] + 60, q[1])) at = q;
                }
                // (Nobody else about to put it down first.)
                for (Entity o : w.entities) if (!o.dead && Math.hypot(o.x - at[0], o.y - at[1]) < 400) { o.dead = true; o.removed = true; }
                Entity soldier = w.spawn(Entity.SOLDIER, at[0], at[1]);
                float[] cp = w.city.findWalkable(at[0] + 60, at[1]);
                Entity civ = w.spawn(Entity.CIVILIAN, cp[0], cp[1]);
                // (The dead between them, so the soldier has a clear shot past nobody.)
                Entity z = w.spawn(Entity.ZOMBIE, cp[0] - 7, cp[1]);
                for (int f = 0; f < 30 * 10 && !(z.dead || z.hp <= 0); f++) {
                    civ.x = cp[0];
                    civ.y = cp[1];
                    civ.hp = civ.maxHp;
                    civ.infected = false;
                    civ.stun = 1;
                    z.speed = z.runSpeed = 0;
                    z.x = cp[0] - 7;
                    z.y = cp[1];
                    // (Facing it: one of the dead standing still behind someone's back isn't noticed.)
                    if (f < 5) soldier.angle = (float) Math.atan2(z.y - soldier.y, z.x - soldier.x);
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(z.dead || z.hp <= 0, "put down");
                // (Whoever got it - the soldier, or a patrol passing - has a life saved to their name.)
                int savedAll = 0;
                for (Entity o : w.entities) savedAll += o.saved;
                for (Fleet.Vehicle v : w.fleet.vehicles) {
                    for (Entity o : v.crew) savedAll += o.saved;
                    for (Entity o : v.riders) savedAll += o.saved;
                }
                check(savedAll >= 1, "a life saved (" + savedAll + "; soldier kills " + soldier.kills + ", civ " + (int) Math.hypot(civ.x - z.x, civ.y - z.y) + " from it, civ dead " + civ.dead + " infected " + civ.infected + ")");
                // The hall of fame: the best kept, best first, and the same person not twice.
                Records rec = new Records(new android.app.Activity());
                soldier.kills = 30;
                Entity cop = null;
                for (Entity o : w.entities) if (!o.dead && o.type == Entity.COP && cop == null) cop = o;
                cop.kills = 12;
                cop.saved = 4;
                rec.check(w);
                rec.check(w);
                java.util.List<Records.Hero> hall = rec.hall();
                int mine = 0;
                for (Records.Hero h : hall) if (h.game == w.gameId) mine++;
                check(mine == 2, "both in the hall of fame, once each (" + mine + ")");
                check(hall.get(0).score() >= hall.get(hall.size() - 1).score(), "best first");
                soldier.kills = 40;
                rec.check(w);
                boolean up = false;
                for (Records.Hero h : rec.hall()) if (h.game == w.gameId && h.seed == soldier.nameSeed && h.kills == 40) up = true;
                check(up, "moving up as they go");
                // Kept in a save.
                java.io.File f = java.io.File.createTempFile("zcsb", ".dat");
                SaveGame.save(w, f);
                World l = SaveGame.load(f);
                f.delete();
                check(l.gameId == w.gameId, "the game told apart after loading");
                Entity ls = null;
                for (Entity o : l.entities) if (o.nameSeed == soldier.nameSeed && o.type == Entity.SOLDIER) ls = o;
                check(ls != null && ls.saved == soldier.saved, "lives saved kept in the save");
                // Six slots, and a game saved to the last of them and loaded back.
                check(Menu.SLOTS == 6, "six slots");
                GameView gv = new GameView(new android.app.Activity());
                gv.layout(1080, 2400);
                CityConfig gc = new CityConfig();
                gc.v[CityConfig.OPT_ZOMBIES] = 0;
                gv.startGame(gc);
                gv.awaitLoad();
                for (int k = 0; k < 3; k++) draw(gv, new int[]{1080, 2400});
                gv.deleteSlot(6);
                check(gv.slotInfo(6) == null, "slot 6 empty");
                // (Tried again if another copy of these tests running alongside had the same file open.)
                boolean saved6 = false;
                for (int k = 0; k < 5 && !saved6; k++) {
                    saved6 = gv.saveSlot(6) && gv.slotInfo(6) != null;
                    if (!saved6) Thread.sleep(200);
                }
                check(saved6, "saved to slot 6");
                check(gv.loadSlot(6), "and loaded from it");
                gv.deleteSlot(6);
            }
        });
        test("15.2: the Downtown Towers map - offered, taller, denser, more and narrower streets, hardly a park", new Check() {
            public void run() {
                check(!CityConfig.retired(CityConfig.OPT_PRESET, CityConfig.TOWERS) && !CityConfig.removed(CityConfig.TOWERS), "offered on the New Game screen");
                World[] ws = new World[2];
                int[] presets = {CityConfig.TOWERS, CityConfig.METROPOLIS};
                for (int k = 0; k < 2; k++) {
                    CityConfig c = new CityConfig();
                    c.v[CityConfig.OPT_PRESET] = presets[k];
                    c.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                    c.v[CityConfig.OPT_ZOMBIES] = 0;
                    c.seed = 5;
                    c.normalize();
                    c.seed = 5;
                    check(c.v[CityConfig.OPT_PRESET] == presets[k], "kept as chosen");
                    ws[k] = new World(c);
                    ws[k].populate(c);
                }
                long[] h = new long[2];
                int[] n = new int[2], tall = new int[2];
                for (int k = 0; k < 2; k++)
                    for (City.Building b : ws[k].city.buildings) {
                        if (b.height <= 0) continue;
                        n[k]++;
                        h[k] += (long) b.height;
                        if (b.height > 200) tall[k]++;
                    }
                check(tall[0] >= 10 && tall[0] > tall[1] * 2 && h[0] / n[0] > h[1] / n[1], "skyscrapers (" + tall[0] + " very tall, against " + tall[1] + ")");
                check(ws[0].city.junctions.size() > ws[1].city.junctions.size() * 1.2f, "more streets (" + ws[0].city.junctions.size() + " junctions, against " + ws[1].city.junctions.size() + ")");
                int stations = 0;
                for (City.Facility f : ws[0].city.facilities) if (f.kind == City.FACILITY_POLICE) stations++;
                check(ws[0].humans > 1000 && stations >= 2, "people and police (" + ws[0].humans + ", " + stations + " stations)");
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
        test("15.5: the Military Town map - a great base in the middle, the town round its gates, far more soldiers", new Check() {
            public void run() {
                check(!CityConfig.retired(CityConfig.OPT_PRESET, CityConfig.MILITARY_TOWN) && !CityConfig.removed(CityConfig.MILITARY_TOWN), "offered on the New Game screen");
                World[] ws = new World[2];
                int[] presets = {CityConfig.MILITARY_TOWN, CityConfig.METROPOLIS};
                for (int k = 0; k < 2; k++) {
                    CityConfig c = new CityConfig();
                    c.v[CityConfig.OPT_PRESET] = presets[k];
                    c.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                    c.v[CityConfig.OPT_ZOMBIES] = 0;
                    c.seed = 6;
                    c.normalize();
                    c.seed = 6;
                    ws[k] = new World(c);
                    ws[k].setMaxPopulation(0);
                    ws[k].populate(c);
                }
                World w = ws[0];
                City.Facility base = w.city.mainBase();
                check(base != null && base.r > ws[1].city.mainBase().r * 2, "a great base (" + (base == null ? 0 : base.r) + " across, against " + ws[1].city.mainBase().r + ")");
                float mid = (float) Math.hypot(base.x - w.city.worldW() / 2f, base.y - w.city.worldH() / 2f);
                check(mid < w.city.worldW() * 0.15f, "in the middle of town (" + (int) mid + " from it)");
                int[] soldiers = new int[2];
                for (int k = 0; k < 2; k++) for (Entity e : ws[k].entities) if (e.type == Entity.SOLDIER) soldiers[k]++;
                check(soldiers[0] >= 450 && soldiers[0] > soldiers[1] * 1.3f, "far more soldiers (" + soldiers[0] + " against " + soldiers[1] + ")");
                check(base.ammo > ws[1].city.mainBase().ammo * 2, "a well-stocked armoury");
                int bases = 0;
                for (City.Facility f : w.city.facilities) if (f.kind == City.FACILITY_BASE) bases++;
                check(bases == 1, "the one great base (" + bases + ")");
                // Out through the gates into town, on foot.
                java.util.Random r = new java.util.Random(2);
                int out = 0;
                for (int k = 0; k < 6; k++) {
                    float[] q = null;
                    for (int t = 0; t < 400 && q == null; t++) {
                        float[] p = w.city.randomWalkableInTown(r);
                        if (Math.hypot(p[0] - base.x, p[1] - base.y) > base.r * 1.6f) q = p;
                    }
                    int[] path = w.city.findPath(base.x, base.y, q[0], q[1], 60000);
                    if (path != null && path.length > 0) out++;
                }
                check(out == 6, "out of the base into town on foot (" + out + " of 6)");
                // And a garrison that rolls out when the dead come.
                for (int f = 0; f < 30 * 20; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
            }
        });
        test("15.6: the Prison Island - a walled prison in the bay; the outbreak starts inside; when it falls the gate opens", new Check() {
            public void run() throws Exception {
                check(!CityConfig.retired(CityConfig.OPT_PRESET, CityConfig.PRISON_ISLAND) && !CityConfig.removed(CityConfig.PRISON_ISLAND), "offered on the New Game screen");
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_PRESET] = CityConfig.PRISON_ISLAND;
                c.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 7;
                c.normalize();
                c.seed = 7;
                World w = new World(c);
                w.setMaxPopulation(0);
                w.populate(c);
                City city = w.city;
                check(city.prison != null && city.prisonGate != null && city.ferry != null, "a prison, its gate and the ferry");
                // ((15.8) A big one, and an army base in town.)
                check(city.prison[2] - city.prison[0] >= 60 && city.prison[3] - city.prison[1] >= 30, "a big prison (" + (city.prison[2] - city.prison[0]) + " by " + (city.prison[3] - city.prison[1]) + ")");
                check(city.mainBase() != null, "an army base");
                int inmates = 0, guards = 0;
                Entity anInmate = null;
                for (Entity e : w.entities) {
                    if (e.inmate) {
                        inmates++;
                        anInmate = e;
                        check(w.inPrison(e.x, e.y), "every inmate inside the walls");
                    }
                    if (e.role == Entity.ROLE_PRISON_GUARD) guards++;
                }
                check(inmates >= 30 && guards >= 6, "inmates and guards (" + inmates + ", " + guards + ")");
                int cells = 0;
                for (City.Building b : city.buildings) if (b.name != null && b.name.startsWith("Cell Block")) cells++;
                check(cells >= 2, "cell blocks (" + cells + ")");
                // Shut in: no way on foot from the yard to the town while the gate is shut.
                float gx = (city.prisonGate[0] + 1.5f) * City.T, gy = city.prisonGate[1] * City.T;
                float[] town = city.findWalkable(gx, (city.seaY - 8) * City.T);
                check(city.findPath(anInmate.x, anInmate.y, town[0], town[1], 60000) == null, "no way out while the gate is shut");
                // A quiet spell, then the outbreak starts inside the walls.
                float[] f0 = w.ferryBoat == null ? null : new float[]{w.ferryBoat[0], w.ferryBoat[1]};
                for (int f = 0; f < 30 * (int) (World.PRISON_QUIET + 4); f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                int bitten = 0;
                for (Entity e : w.entities) if (e.infected && w.inPrison(e.x, e.y)) bitten++;
                check(w.prisonSeeded && bitten >= 1, "inmates bitten inside (" + bitten + ")");
                check(f0 != null && Math.hypot(w.ferryBoat[0] - f0[0], w.ferryBoat[1] - f0[1]) > 20, "the ferry under way");
                for (Entity e : w.entities) if (e.isZombie() && !w.inPrison(e.x, e.y)) check(false, "none of the dead outside yet");
                // It falls: the gate opens, and there's a way out over the causeway.
                w.fallPrison();
                check(w.prisonFallen && !city.solidAt(gx, gy + City.T / 2f), "the gate open");
                int loose = 0;
                for (Entity e : w.entities) if (e.inmate) loose++;
                check(loose == 0, "nobody locked up any more");
                Entity someone = null;
                for (Entity e : w.entities) if (!e.dead && !e.isZombie() && w.inPrison(e.x, e.y) && e.type == Entity.CIVILIAN) someone = e;
                // (From open ground where they stand: someone pressed against a wall in the crush starts on its edge.)
                float[] from = city.findWalkable(someone.x, someone.y);
                check(city.findPath(from[0], from[1], town[0], town[1], 120000) != null, "a way out now");
                // Kept in a save: still fallen, the gate still open.
                java.io.File file = java.io.File.createTempFile("zcsb", ".dat");
                SaveGame.save(w, file);
                World l = SaveGame.load(file);
                file.delete();
                check(l.prisonFallen && !l.city.solidAt(gx, gy + City.T / 2f), "still open after a save");
            }
        });
        test("15.9: Marines land from just offshore, near the fight, and count at once; people stop at the water's edge", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_PRESET] = CityConfig.PRISON_ISLAND;
                c.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 9;
                c.normalize();
                c.seed = 9;
                World w = new World(c);
                w.setMaxPopulation(0);
                w.populate(c);
                City city = w.city;
                // A call a little way inland from the seafront.
                java.util.Random r = new java.util.Random(1);
                float[] at = null;
                for (int k = 0; k < 5000 && at == null; k++) {
                    float[] q = city.randomWalkableInTown(r);
                    if (q[1] > (city.seaY - 30) * City.T && q[1] < (city.seaY - 8) * City.T) at = q;
                }
                Dispatch.Incident inc = w.dispatch.sighting(at[0], at[1], null);
                w.recountLive();
                int before = w.counts[Entity.SOLDIER];
                check(w.fleet.marineLanding(16, at[0], at[1], "the test", inc), "Marines sent by boat");
                w.recountLive();
                check(w.counts[Entity.SOLDIER] == before + 16, "counted at once, still aboard (" + (w.counts[Entity.SOLDIER] - before) + " of 16)");
                Fleet.Vehicle boat = null;
                for (Fleet.Vehicle v : w.fleet.vehicles) if (v.type == Fleet.BOAT) boat = v;
                check(boat.boatPath.length <= Fleet.MARINE_RUN, "in from just offshore (" + boat.boatPath.length + " tiles)");
                int ashore = 0;
                for (int f = 0; f < 30 * 40 && ashore < 16; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                    ashore = 0;
                    for (Entity e : w.entities) if (!e.dead && e.role == Entity.ROLE_MARINE) ashore++;
                }
                check(ashore == 16, "all of them ashore within 40 seconds (" + ashore + ")");
                float near = Float.MAX_VALUE;
                for (Entity e : w.entities) if (!e.dead && e.role == Entity.ROLE_MARINE) near = Math.min(near, (float) Math.hypot(e.x - at[0], e.y - at[1]));
                check(near < 700, "put ashore near the call (" + (int) near + " away)");
                // Someone who ran from the dead to the water's edge, the dead long behind: they stop there.
                float[] edge = city.findWalkable(at[0], (city.seaY + 1.5f) * City.T);
                Entity e = w.spawn(Entity.CIVILIAN, edge[0], edge[1]);
                e.threatX = edge[0];
                e.threatY = edge[1] - 200;
                e.fear = 40;
                e.fleeTimer = 6;
                e.aware = true;
                float px = 0, py = 0;
                for (int f = 0; f < 30 * 6; f++) {
                    if (f == 30 * 3) {
                        px = e.x;
                        py = e.y;
                    }
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                // (Off along the shore, or stopped: not standing there pushing at the water.)
                float moved = (float) Math.hypot(e.x - px, e.y - py);
                check(e.want < 1 || moved > 20, "not pushing at the water (want " + e.want + ", moved " + moved + " in 3 seconds)");
            }
        });
        test("16.0: the airport - troops flown in, evacuation flights, and it can fall; boats from the docks on the water", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_PRESET] = CityConfig.METROPOLIS;
                c.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 10;
                c.normalize();
                c.seed = 10;
                World w = new World(c);
                w.setMaxPopulation(0);
                w.populate(c);
                City city = w.city;
                check(city.airport != null && city.runway != null && city.apron != null, "an airport");
                boolean terminal = false;
                for (City.Building b : city.buildings) if ("Airport Terminal".equals(b.name)) terminal = true;
                check(terminal, "its terminal");
                // Soldiers flown in.
                java.util.Random r = new java.util.Random(2);
                float[] t = city.randomWalkableInTown(r);
                Dispatch.Incident inc = w.dispatch.sighting(t[0], t[1], null);
                w.recountLive();
                int before = w.counts[Entity.SOLDIER];
                check(w.fleet.flyInTroops(40, inc, "the test"), "a transport sent");
                w.recountLive();
                check(w.counts[Entity.SOLDIER] == before + 40, "counted while aboard");
                check(w.fleet.sendAirliner(-1), "an airliner sent");
                Fleet.Vehicle airliner = null;
                for (Fleet.Vehicle v : w.fleet.vehicles) if (v.kind == Fleet.K_AIRLINER) airliner = v;
                int landed = 0;
                boolean atStand = false, gone = false;
                for (int f = 0; f < 30 * 200 && !gone; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                    if (airliner.busPhase == 3) atStand = true;
                    gone = !w.fleet.vehicles.contains(airliner);
                    if (f == 30 * 60) for (Entity e : w.entities) if (!e.dead && e.type == Entity.SOLDIER && e.outsider) landed++;
                }
                check(landed >= 30, "the soldiers got off at the airport (" + landed + ")");
                check(atStand && gone, "the airliner landed, came to the terminal, and took off again (" + atStand + ", " + gone + ")");
                // The dead take the terminal: the airport falls.
                java.util.List<Entity> pack = new java.util.ArrayList<Entity>();
                for (int k = 0; k < 10; k++) {
                    float[] p = city.findWalkable(city.apron[0] + (k % 5) * 8, city.apron[1] + 4 * City.T + (k / 5) * 8);
                    if (p != null) pack.add(w.spawn(Entity.ZOMBIE, p[0], p[1]));
                }
                for (Entity o : w.entities) if (!o.dead && (o.isArmed() || o.hasGun) && Math.hypot(o.x - city.apron[0], o.y - city.apron[1]) < 600) {
                    o.dead = true;
                    o.removed = true;
                }
                for (int f = 0; f < 30 * 25 && !w.airportClosed; f++) {
                    for (Entity z : pack) {
                        z.speed = z.runSpeed = 0;
                        z.hp = z.maxHp = 100000;
                    }
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(w.airportClosed, "the airport fell to the dead");
                check(!w.fleet.flyInTroops(10, inc, "x") && !w.fleet.sendAirliner(-1), "no flights in or out");
                // Docks on a map on the water, each with a way in by sea.
                CityConfig pc = new CityConfig();
                pc.v[CityConfig.OPT_PRESET] = CityConfig.PRISON_ISLAND;
                pc.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                pc.v[CityConfig.OPT_ZOMBIES] = 0;
                pc.seed = 10;
                pc.normalize();
                pc.seed = 10;
                World pw = new World(pc);
                pw.setMaxPopulation(0);
                pw.populate(pc);
                java.lang.reflect.Method ad = World.class.getDeclaredMethod("addDocks");
                ad.setAccessible(true);
                int docks = (Integer) ad.invoke(pw);
                check(docks >= 1, "docks on the waterfront (" + docks + ")");
                for (World.EvacStop st : pw.evacStops) if (st.dock) check(st.route != null && st.route.length > 0 && st.route.length <= Fleet.MARINE_RUN, "a way in by sea to each");
            }
        });
        test("16.1: map sizes - Default, Big and a new Large with twice the ground and the same people", new Check() {
            public void run() {
                int offered = 0;
                for (int s = 0; s <= CityConfig.VAST; s++) if (!CityConfig.retired(CityConfig.OPT_SIZE, s)) offered++;
                check(offered == 3 && !CityConfig.retired(CityConfig.OPT_SIZE, CityConfig.GIGANTIC) && !CityConfig.retired(CityConfig.OPT_SIZE, CityConfig.COLOSSAL)
                        && !CityConfig.retired(CityConfig.OPT_SIZE, CityConfig.VAST) && CityConfig.retired(CityConfig.OPT_SIZE, CityConfig.LARGE), "three sizes offered, the old Default gone");
                check("Default".equals(CityConfig.sizeName(CityConfig.GIGANTIC)) && "Big".equals(CityConfig.sizeName(CityConfig.COLOSSAL))
                        && "Large".equals(CityConfig.sizeName(CityConfig.VAST)), "called Default, Big and Large");
                CityConfig big = new CityConfig(), large = new CityConfig();
                big.v[CityConfig.OPT_SIZE] = CityConfig.COLOSSAL;
                large.v[CityConfig.OPT_SIZE] = CityConfig.VAST;
                float area = (float) large.tiles() * large.tiles() / ((float) big.tiles() * big.tiles());
                check(area > 1.95f && area < 2.05f, "twice the ground (" + area + ")");
                check(big.residentsTarget() == large.residentsTarget(), "the same number of people");
                CityConfig fresh = new CityConfig();
                fresh.v[CityConfig.OPT_SIZE] = CityConfig.LARGE;
                fresh.dropRetired();
                check(fresh.v[CityConfig.OPT_SIZE] == CityConfig.GIGANTIC, "the New Game screen starts on Default");
                check(CityConfig.LARGE != CityConfig.VAST && large.code().length() > 0, "a city code for it");
            }
        });
        test("16.3: the names of the districts and islands are on dry land, never out on the water", new Check() {
            public void run() {
                for (int preset : new int[]{CityConfig.ISLANDS, CityConfig.ARCHIPELAGO, CityConfig.PRISON_ISLAND})
                    for (long seed : new long[]{77, 1234}) {
                        CityConfig c = new CityConfig();
                        c.v[CityConfig.OPT_PRESET] = preset;
                        c.seed = seed;
                        c.normalize();
                        c.seed = seed;
                        City city = new City(c, 0.05f);
                        int named = 0;
                        for (City.District d : city.districts) {
                            if (!d.labelled()) continue;
                            named++;
                            check(city.tiles[city.tileIndex(d.cx, d.cy)] != City.WATER, d.name + " is named out on the water (map " + preset + ")");
                        }
                        check(named >= 4, "the districts are named (" + named + ", map " + preset + ")");
                        if (city.islandRects != null) {
                            check(city.islandLabel != null && city.islandLabel.length == city.islandRects.length * 2, "a place for each island's name");
                            for (int k = 0; k < city.islandRects.length; k++)
                                check(city.tiles[city.tileIndex(city.islandLabel[k * 2], city.islandLabel[k * 2 + 1])] != City.WATER,
                                        city.islandNames[k] + " is named out at sea");
                        }
                    }
            }
        });
        test("16.5: no hospital in a new city; an older city keeps its building, but nobody's sent there and it has no way on foot worked out", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                c.seed = 41;
                World w = new World(c);
                check(w.city.nearestFacility(City.FACILITY_HOSPITAL, 0, 0) == null, "no hospital");
                for (City.Building b : w.city.buildings) check(b.kind != City.HOSPITAL, "no hospital building");
                CityConfig o = new CityConfig();
                o.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                o.seed = 41;
                o.civic = 29;
                World ow = new World(o);
                City.Facility h = ow.city.nearestFacility(City.FACILITY_HOSPITAL, 0, 0);
                check(h != null, "an older city still has its hospital (saves rebuild the same)");
                for (int i = 0; i < 400 && ow.city.warmFacilityField(); i++) { }
                check(((java.util.Map<?, ?>) field(ow.city, "facilityFields")).get(h) == null, "no way on foot to it worked out ahead");
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
        test("16.8: the quick ways to find gun stores, things on the ground and open ground give the same answers as the long ways", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 9;
                World w = new World(c);
                java.util.Random r = new java.util.Random(3);
                // Gun stores: the nearest one with stock, by the grid and by looking at every one.
                java.lang.reflect.Method near = World.class.getDeclaredMethod("gunStoreNear", Entity.class, float.class);
                near.setAccessible(true);
                java.util.List<City.Building> stores = new java.util.ArrayList<City.Building>();
                // ((16.9) There are no gun stores now: some shops made into them here, to try the grid on.)
                int nth = 0;
                for (City.Building b : w.city.buildings) if (b.kind == City.SHOP && nth++ % 4 == 0) {
                    b.shopType = 1;
                    stores.add(b);
                }
                check(stores.size() > 2, "gun stores (" + stores.size() + ")");
                for (City.Building b : stores) b.stock = r.nextInt(3) == 0 ? 0 : 10;
                Entity probe = new Entity();
                int same = 0;
                for (int k = 0; k < 400; k++) {
                    probe.x = r.nextFloat() * w.city.worldW();
                    probe.y = r.nextFloat() * w.city.worldH();
                    float radius = 200 + r.nextFloat() * 900;
                    City.Building want = null;
                    float bd = radius * radius;
                    for (City.Building b : stores) {
                        if (b.stock <= 0 || b.collapsed || b.infestKnown) continue;
                        float d = (b.doorX - probe.x) * (b.doorX - probe.x) + (b.doorY - probe.y) * (b.doorY - probe.y);
                        if (d < bd) {
                            bd = d;
                            want = b;
                        }
                    }
                    if (near.invoke(w, probe, radius) == want) same++;
                }
                check(same == 400, "the nearest gun store by the grid is the nearest one (" + same + " of 400)");
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
        test("17.0: a gang war - gangs on their turf, no zombies, police fight them, civilians only hurt in crossfire", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_ZOMBIES] = 3;
                c.mode = CityConfig.MODE_GANGS;
                c.seed = 21;
                World w = new World(c);
                w.populate(c);
                w.recount();
                check(w.hostileMode() && w.mode() == CityConfig.MODE_GANGS, "the game is a gang war");
                int gang = 0, undead = 0, kits = 0, colours = 0;
                java.util.HashSet<Integer> gangs = new java.util.HashSet<Integer>();
                for (Entity e : w.entities) {
                    if (e.isUndead()) undead++;
                    if (e.type != Entity.GANGSTER) continue;
                    gang++;
                    gangs.add(e.squad);
                    kits |= 1 << e.kit;
                    if (e.body != 0 && e.head == World.GANG_COLORS[World.fmod(e.squad, World.GANG_COLORS.length)]) colours++;
                    check(e.hasGun && e.ammo + e.reserve > 0, "every gang member is armed");
                    check(e.isZombie() && e.isHostile() && !e.isUndead(), "a gang member is on the other side, and living");
                }
                check(undead == 0, "no zombies (" + undead + ")");
                check(gang >= 20, "the gangs are out (" + gang + ")");
                check(gangs.size() >= 2, "more than one gang (" + gangs.size() + ")");
                check((kits & 7) == 7 && (kits & 8) != 0, "thugs, shooters, enforcers and a boss");
                check(colours == gang, "each in their gang's colours");
                check(w.zombieCount() == gang && w.outbreak, "they count as the other side, and it's on");
                check(w.hostileReserve > 0, "more can be recruited");
                for (boolean t : w.trait) check(!t, "no virus strain");
                // Nobody unarmed is ever a target.
                java.lang.reflect.Method ht = World.class.getDeclaredMethod("hostileTarget", Entity.class, float.class);
                ht.setAccessible(true);
                int shots = 0;
                for (int i = 0; i < 30 * 40; i++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                    if (i % 30 == 0)
                        for (Entity e : w.entities) {
                            if (e.dead || e.type != Entity.GANGSTER) continue;
                            Entity t = (Entity) ht.invoke(w, e, 300f);
                            if (t == null) continue;
                            shots++;
                            check(t.isArmed() || t.hasGun || t.isHostile() || t == w.controlled, "only the armed are targets");
                            if (t.isHostile()) check(t.squad != e.squad, "a gang fights only its rivals");
                        }
                }
                check(shots > 0, "the gangs found someone to fight");
                int infected = 0;
                for (Entity e : w.entities) if (!e.dead && (e.infected || e.isUndead())) infected++;
                check(infected == 0, "nobody bitten or turned (" + infected + ")");
                check(w.zombiesKilled > 0, "the police took some down (" + w.zombiesKilled + ")");
                check(w.counts[Entity.COP] + w.counts[Entity.SOLDIER] > 0, "the police are still fighting");
                // The messages speak of gangs, not the dead.
                check(w.reword("THE DEAD ARE BACK").equals("THE GANGS ARE BACK"), "the dead become the gangs");
                check(w.reword("3 zombies near the station").equals("3 gang members near the station"), "zombies become gang members");
                // The spawn tool's kits, and they join the gang that's right there.
                float[] p = w.city.randomWalkableInTown(new java.util.Random(4));
                w.newHostileGroup(p[0], p[1]);
                Entity a = w.spawnHostile(2, p[0], p[1]), b = w.spawnHostile(1, p[0] + 6, p[1]);
                check(a != null && b != null && a.type == Entity.GANGSTER && a.kit == 2 && a.weapon == Entity.W_SHOTGUN
                        && b.weapon == Entity.W_SMG && a.squad == b.squad, "spawned as one gang crew with their kits");
                check(w.hostileTitle(a).contains(World.GANG_NAMES[World.fmod(a.squad, World.GANG_NAMES.length)]), "the card names the gang");
                check(w.hostileDoing(a) != null, "and says what they're doing");
            }
        });
        test("17.0: an invasion - squads in from the edge, objectives taken, waves, and a winner either way", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_ZOMBIES] = 3;
                c.mode = CityConfig.MODE_ARMY;
                c.seed = 22;
                World w = new World(c);
                w.populate(c);
                w.recount();
                int army = 0, kits = 0, nearEdge = 0;
                java.util.HashSet<Integer> squads = new java.util.HashSet<Integer>();
                float ww = w.city.worldW(), hh = w.city.worldH();
                for (Entity e : w.entities) {
                    if (e.type != Entity.INVADER) continue;
                    army++;
                    squads.add(e.squad);
                    kits |= 1 << e.kit;
                    check(e.body == World.ARMY_BODY && e.hasGun, "in the enemy's uniform, armed");
                    if (Math.min(Math.min(e.x, e.y), Math.min(ww - e.x, hh - e.y)) < 300) nearEdge++;
                }
                check(army >= 20 && squads.size() >= 3, "squads of enemy soldiers (" + army + " in " + squads.size() + ")");
                check(kits == 7, "riflemen, machine gunners and marksmen");
                check(nearEdge > army / 2, "coming in from the edge (" + nearEdge + " of " + army + ")");
                // Each squad heads for the nearest of the stations, bases and city hall.
                for (int i = 0; i < 30 * 5; i++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                int headed = 0;
                for (Entity e : w.entities) if (!e.dead && e.type == Entity.INVADER && e.postX >= 0) headed++;
                check(headed > 0, "they have objectives");
                // An objective with the enemy there and none of the city's defenders: taken.
                java.lang.reflect.Method take = World.class.getDeclaredMethod("nextObjective", float.class, float.class);
                take.setAccessible(true);
                java.lang.reflect.Field ox = World.class.getDeclaredField("objX"), oy = World.class.getDeclaredField("objY"),
                        tk = World.class.getDeclaredField("objTaken");
                ox.setAccessible(true);
                oy.setAccessible(true);
                tk.setAccessible(true);
                int o = (Integer) take.invoke(w, 0f, 0f);
                float tx = ((float[]) ox.get(w))[o], ty = ((float[]) oy.get(w))[o];
                for (Entity e : w.entities) if (!e.dead && (e.isArmed()) && World.hypot(e.x - tx, e.y - ty) < 200) { e.dead = true; e.removed = true; }
                int moved = 0;
                for (Entity e : w.entities)
                    if (!e.dead && e.type == Entity.INVADER && moved < 4) {
                        e.x = tx + moved * 5;
                        e.y = ty;
                        e.postX = tx;
                        e.postY = ty;
                        moved++;
                    }
                for (int i = 0; i < 30 * 3 && !((boolean[]) tk.get(w))[o]; i++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(((boolean[]) tk.get(w))[o], "the enemy took an objective");
                // Waves: with most of them down, more come in.
                int before = w.hostileReserve;
                for (Entity e : w.entities) if (e.type == Entity.INVADER) e.hp = 0;
                for (int i = 0; i < 30 * 20 && w.hostileReserve == before; i++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(w.hostileReserve < before && w.counts[Entity.INVADER] > 0, "another squad came in");
                // All of them gone, none to come: the city wins.
                for (int i = 0; i < 30 * 20 && w.warResult == 0; i++) {
                    w.hostileReserve = 0;
                    for (Entity e : w.entities) if (e.isHostile()) e.hp = 0;
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(w.warResult == 1 && "THE INVASION IS DEFEATED".equals(w.warBanner), "the city won (" + w.warBanner + ")");
                // And the other way: every officer and soldier down, nobody left to send.
                World l = new World(c);
                l.populate(c);
                l.recount();
                for (int i = 0; i < 30 * 20 && l.warResult == 0; i++) {
                    l.dispatch.policeReserve = l.dispatch.squadReserve = 0;
                    // (Those in their cars and trucks too: out they come, and down.)
                    for (Fleet.Vehicle v : new java.util.ArrayList<Fleet.Vehicle>(l.fleet.vehicles))
                        if (!v.broken && (v.driver != null || !v.crew.isEmpty() || v.passengers > 0)) l.fleet.damage(v, 1e6f, true);
                    for (Entity e : l.entities) if (e.isArmed()) e.hp = 0;
                    l.update(1 / 30f);
                    l.evCount = 0;
                }
                check(l.warResult == 2 && "THE CITY HAS FALLEN".equals(l.warBanner), "the enemy won (" + l.warBanner + ", armed " + (l.counts[Entity.COP] + l.counts[Entity.SOLDIER])
                        + ", riding " + l.riding + ", enemy " + l.zombieCount() + ", outbreak " + l.outbreak + ", aftermath " + l.aftermath + ")");
            }
        });
        test("17.0: a gang war or an invasion saves and loads back, mode and all", new Check() {
            public void run() throws Exception {
                for (int mode = 1; mode <= 2; mode++) {
                    CityConfig c = new CityConfig();
                    c.v[CityConfig.OPT_ZOMBIES] = 2;
                    c.mode = mode;
                    c.seed = 30 + mode;
                    World w = new World(c);
                    w.populate(c);
                    for (int i = 0; i < 30 * 5; i++) {
                        w.update(1 / 30f);
                        w.evCount = 0;
                    }
                    File f = File.createTempFile("zcs", ".dat");
                    SaveGame.save(w, f);
                    World l = SaveGame.load(f);
                    f.delete();
                    check(l.city.cfg.mode == mode && l.hostileMode(), "the mode came back");
                    check(l.hostileReserve == w.hostileReserve && l.hostileStart == w.hostileStart, "and the fighters still to come");
                    int kw = 0, kl = 0, nw = 0, nl = 0;
                    // ((17.2) Those riding in a stolen car too: on loading, they're out of it.)
                    for (Entity e : w.entities) if (!e.dead && e.isHostile()) { nw++; kw += e.kit * 7 + e.squad; }
                    for (Fleet.Vehicle v : w.fleet.vehicles) for (Entity e : v.riders) if (e.isHostile()) { nw++; kw += e.kit * 7 + e.squad; }
                    for (Entity e : l.entities) if (!e.dead && e.isHostile()) { nl++; kl += e.kit * 7 + e.squad; }
                    for (Fleet.Vehicle v : l.fleet.vehicles) for (Entity e : v.riders) if (e.isHostile()) { nl++; kl += e.kit * 7 + e.squad; }
                    check(nw == nl && kw == kl, "each with their kit and gang or squad (" + nw + "/" + nl + ")");
                }
                // Drawn: the enemy tool and its picker for each mode, and the people on the map.
                GameView v = new GameView(new android.app.Activity());
                int[] size = {2400, 1080};
                v.layout(size[0], size[1]);
                Menu m = (Menu) field(v, "menu");
                for (int mode = 1; mode <= 2; mode++) {
                    CityConfig c = new CityConfig();
                    c.v[CityConfig.OPT_ZOMBIES] = 2;
                    c.mode = mode;
                    v.startGame(c);
                    v.awaitLoad();
                    m.screen = Menu.NONE;
                    for (int k = 0; k < 10; k++) draw(v, size);
                    Field pk = GameView.class.getDeclaredField("picker");
                    pk.setAccessible(true);
                    pk.setInt(v, 6);
                    draw(v, size);
                    pk.setInt(v, -1);
                    World w = (World) field(v, "world");
                    Field fo = GameView.class.getDeclaredField("follow");
                    fo.setAccessible(true);
                    for (Entity e : w.entities) if (!e.dead && e.isHostile()) { fo.set(v, e); break; }
                    draw(v, size);
                    check(v.drawErrors == 0, "drew without an error (" + v.drawErrors + ")");
                }
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
        test("17.2: open interiors; gang bosses in hideouts, raids, surrender and arrest, turf, corners, robberies, stolen cars", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_ZOMBIES] = 3;
                c.mode = CityConfig.MODE_GANGS;
                c.seed = 23;
                World w = new World(c);
                w.rnd.setSeed(23);
                w.populate(c);
                w.recount();
                City city = w.city;
                // Open floors in shops and offices, rooms in homes.
                int open = 0, rooms = 0;
                for (City.Building b : city.buildings) {
                    if (!city.enterable(b)) continue;
                    int in = Interiors.insideOf(b);
                    if (Interiors.open(in)) {
                        check(city.planOf(b).rooms.size() == 1, "an open floor is one room");
                        open++;
                    } else if (city.planOf(b).rooms.size() >= 2) rooms++;
                }
                check(open > 10 && rooms > 10, "open floors (" + open + ") and homes with rooms (" + rooms + ")");
                // A boss to each gang, sat hidden in its hideout with two bodyguards.
                java.lang.reflect.Method ho = World.class.getDeclaredMethod("hideout", int.class);
                ho.setAccessible(true);
                for (int f = 0; f < 30 * 3; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                int bosses = 0, guards = 0, gangs = 0;
                for (int g = 0; g < World.GANG_NAMES.length; g++) {
                    Entity b = w.gangBoss[g];
                    if (b == null) continue;
                    gangs++;
                    if (b.kit == 3 && b.type == Entity.GANGSTER && b.weapon == Entity.W_REVOLVER && city.insideOf(b.x, b.y) == ho.invoke(w, g) && b.hidden) bosses++;
                }
                for (Entity e : w.entities) if (!e.dead && e.type == Entity.GANGSTER && e.kit == 2) guards++;
                check(gangs >= 2 && bosses == gangs, "each gang's boss hidden in its hideout (" + bosses + " of " + gangs + ")");
                check(guards >= gangs * 2, "with bodyguards (" + guards + ")");
                check(w.districtOwner != null && w.turfVersion > 0, "turf held");
                int held = 0;
                for (int d : w.districtOwner) if (d >= 0) held++;
                check(held >= 1, "gangs hold districts (" + held + ")");
                // A beaten gang member with an officer right there gives up, is cuffed and walked off.
                Entity cop = null, thug = null;
                for (Entity e : w.entities) {
                    if (e.dead) continue;
                    if (cop == null && e.type == Entity.COP && e.ride == null && e.rig == null && city.insideOf(e.x, e.y) == null) cop = e;
                    if (thug == null && e.type == Entity.GANGSTER && e.kit == 0 && city.insideOf(e.x, e.y) == null && !e.hidden) thug = e;
                }
                check(cop != null && thug != null, "an officer and a gang member");
                w.surrender(thug);
                // (Somewhere quiet for it: the officer would rightly see to a gunfight first.)
                for (Entity e : w.entities) if (!e.dead && e.isHostile() && Math.hypot(e.x - thug.x, e.y - thug.y) < 350) { e.dead = true; e.removed = true; }
                check(thug.type == Entity.CIVILIAN && thug.arrested == 1 && !thug.hasGun && w.arrests >= 1, "hands up, unarmed, not the enemy any more");
                float[] near = city.findWalkable(thug.x + 20, thug.y);
                cop.x = near[0];
                cop.y = near[1];
                cop.task = Dispatch.T_NONE;
                boolean cuffed = false;
                for (int f = 0; f < 30 * 45 && !cuffed; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                    cuffed = thug.arrested == 2 || thug.removed;
                }
                check(cuffed, "an officer came and cuffed them (captor " + (thug.captor != null) + ", cop has " + (cop.escortee == thug) + ", d " + (int) Math.hypot(cop.x - thug.x, cop.y - thug.y)
                        + ", captor d " + (thug.captor == null ? -1 : (int) Math.hypot(thug.captor.x - thug.x, thug.captor.y - thug.y)) + ", t " + (int) thug.arrestT + ", arrested " + thug.arrested + " dead " + thug.dead + ")");
                // A robbery: in through the shop door, the alarm and the 911 call.
                Entity robber = null;
                for (Entity e : w.entities)
                    if (robber == null && !e.dead && e.type == Entity.GANGSTER && e.kit <= 1 && !w.gangBrokenUp(e.squad) && e.ride == null) robber = e;
                City.Building shop = null;
                int quiet = Integer.MAX_VALUE;
                for (City.Building b : city.buildings)
                    if (b.kind == City.SHOP && city.enterable(b)) {
                        int n = w.countZombiesNear(b.doorX, b.doorY, 250);
                        if (n < quiet) {
                            quiet = n;
                            shop = b;
                        }
                    }
                check(robber != null && shop != null, "a gang member and a shop");
                robber.x = shop.doorX;
                robber.y = shop.doorY;
                robber.foe = null;
                robber.foeT = 999;
                robber.corner = 0;
                robber.carJob = 0;
                robber.robB = shop;
                robber.robT = 0;
                robber.robPhase = 0;
                boolean robbed = false;
                for (int f = 0; f < 30 * 20 && !robbed && !robber.dead && robber.type == Entity.GANGSTER; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                    robbed = robber.robPhase == 1;
                }
                boolean said = false;
                for (World.Moment mo : w.timeline) said |= mo.text.startsWith("Armed robbery");
                check(robbed && said, "in through the door, robbing it, and it's reported");
                // A stolen car: off a lot, with a crew aboard.
                int tile = -1;
                for (int i = 0; i < city.tiles.length && tile < 0; i++) {
                    if (city.tiles[i] != City.CAR) continue;
                    float tx = (i % city.w + 0.5f) * City.T, ty = (i / city.w + 0.5f) * City.T;
                    if (city.parkedCarNear(tx, ty, 4) == i && w.countZombiesNear(tx, ty, 300) == 0 && city.nearestDrivable(tx, ty) != null
                            && Math.hypot(city.nearestDrivable(tx, ty)[0] - tx, city.nearestDrivable(tx, ty)[1] - ty) < 50) tile = i;
                }
                check(tile >= 0, "a car parked on a lot");
                float cx = (tile % city.w + 0.5f) * City.T, cy = (tile / city.w + 0.5f) * City.T;
                float[] spot = city.findWalkable(cx, cy);
                java.lang.reflect.Field jobs = World.class.getDeclaredField("carJobs");
                jobs.setAccessible(true);
                @SuppressWarnings("unchecked") java.util.ArrayList<float[]> jl = (java.util.ArrayList<float[]>) jobs.get(w);
                int crew = 0, gang = -1;
                for (Entity e : w.entities) {
                    if (crew >= 2 || e.dead || e.type != Entity.GANGSTER || e.kit == 3 || w.gangBrokenUp(e.squad) || e.ride != null) continue;
                    if (gang >= 0 && World.fmod(e.squad, World.GANG_NAMES.length) != gang) continue;
                    gang = World.fmod(e.squad, World.GANG_NAMES.length);
                    e.x = spot[0] + crew * 4;
                    e.y = spot[1];
                    e.foe = null;
                    e.foeT = 999;
                    e.corner = 0;
                    e.robB = null;
                    e.carJob = 9999;
                    e.carX = spot[0];
                    e.carY = spot[1];
                    crew++;
                }
                jl.add(new float[]{9999, gang, tile, 0, -1});
                boolean stolen = false;
                for (int f = 0; f < 30 * 10 && !stolen; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                    if (city.tiles[tile] == City.CAR) continue;
                    for (Fleet.Vehicle v : w.fleet.vehicles)
                        if (v.gang == gang && !v.riders.isEmpty() && Math.hypot(v.x - cx, v.y - cy) < 200) stolen = true;
                }
                check(stolen, "the car's gone from the lot, the gang aboard and away (tile " + city.tiles[tile] + ")");
                // The boss goes down: the gang falls apart, and recruits no more.
                int g0 = -1;
                for (int g = 0; g < World.GANG_NAMES.length && g0 < 0; g++) if (w.gangBoss[g] != null) g0 = g;
                Entity boss = w.gangBoss[g0];
                boss.hp = 0;
                for (int f = 0; f < 30 * 3; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(w.gangBrokenUp(g0), "the boss's gang is finished");
                for (int f = 0; f < 30 * 60; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                int left = 0;
                for (Entity e : w.entities)
                    if (!e.dead && e.type == Entity.GANGSTER && World.fmod(e.squad, World.GANG_NAMES.length) == g0) left++;
                check(left <= 3, "its members give up or get out of town (" + left + " left after a minute)");
                // Saved and loaded: the gangs that are finished, the arrests, who's under arrest.
                File file = File.createTempFile("zcs", ".dat");
                SaveGame.save(w, file);
                World l = SaveGame.load(file);
                file.delete();
                check(l.gangBroken == w.gangBroken && l.gangHadBoss == w.gangHadBoss && l.arrests == w.arrests, "gangs and arrests saved");
                int aw = 0, al = 0;
                for (Entity e : w.entities) if (!e.dead && e.arrested != 0) aw++;
                for (Entity e : l.entities) if (!e.dead && e.arrested != 0) al++;
                check(aw == al, "and who's under arrest (" + aw + "/" + al + ")");
                // Drawn: the turf, the hideouts, people under arrest.
                GameView v = new GameView(new android.app.Activity());
                int[] size = {2400, 1080};
                v.layout(size[0], size[1]);
                Menu m = (Menu) field(v, "menu");
                v.startGame(c);
                v.awaitLoad();
                m.screen = Menu.NONE;
                for (int k = 0; k < 30; k++) draw(v, size);
                check(v.drawErrors == 0, "drew without an error (" + v.drawErrors + ")");
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
                c2.mode = CityConfig.MODE_GANGS;
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
                // The war bar: the gangs count, and the police in their cars; not swamped by the crowd.
                World w = (World) field(v, "world");
                w.recount();
                for (int i = 0; i < 30 * 3; i++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(w.outbreak, "the war's on");
                check(w.warBalance > 0.2f && w.warBalance < 0.9f, "the bar shows a fight, not 97 to 3 (" + w.warBalance + ")");
                // All of the other side gone: it says so.
                for (int i = 0; i < 30 * 8; i++) {
                    w.hostileReserve = 0;
                    for (Entity e : w.entities) if (e.isHostile()) e.hp = 0;
                    w.update(1 / 30f);
                    w.evCount = 0;
                    if (!w.outbreak) break;
                }
                check(w.warBalance > 0.9f || !w.outbreak, "and the city's side ahead once they're down (" + w.warBalance + ")");
            }
        });
        test("17.4: the enemy's armour, air, artillery, drones and paratroopers; its ground and the occupation", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_ZOMBIES] = 3;
                c.mode = CityConfig.MODE_ARMY;
                c.seed = 24;
                World w = new World(c);
                w.rnd.setSeed(24);
                w.populate(c);
                w.recount();
                City city = w.city;
                int tanks = 0;
                for (Fleet.Vehicle v : w.fleet.vehicles) if (v.enemy && v.type == Fleet.TANK) tanks++;
                check(tanks >= 1, "enemy armour comes in with the first squads (" + tanks + ")");
                check(!Fleet.army(w.fleet.vehicles.get(0)) || !w.fleet.vehicles.get(0).enemy, "the enemy's aren't the army's");
                // A truck bringing a squad: it unloads them when there's a fight in reach.
                float[] goal = w.enemyObjective(city.worldW() / 2, city.worldH() / 2);
                float[] entry = city.edgeRoad(new java.util.Random(3));
                Fleet.Vehicle truck = w.fleet.enemyIn(Fleet.ENEMY_APC, entry[0], entry[1], goal[0], goal[1]);
                check(truck != null && truck.enemy && truck.kind == Fleet.K_APC, "an enemy APC on its way in");
                java.lang.reflect.Method aboard = World.class.getDeclaredMethod("aboard", Fleet.Vehicle.class, Entity.class);
                aboard.setAccessible(true);
                java.lang.reflect.Method kit = World.class.getDeclaredMethod("kitOut", Entity.class, int.class, int.class);
                kit.setAccessible(true);
                for (int k = 0; k < 4; k++) {
                    Entity e = w.spawn(Entity.INVADER, truck.x, truck.y);
                    kit.invoke(w, e, 0, 999);
                    aboard.invoke(w, truck, e);
                }
                w.recount();
                check(w.counts[Entity.INVADER] >= 4, "the troops aboard count");
                check(w.fleet.describe(truck).startsWith("Enemy"), "it's called the enemy's");
                // Put city soldiers in its path: it stops and they climb out.
                Entity s0 = null;
                for (Entity e : w.entities) if (!e.dead && e.type == Entity.SOLDIER && e.ride == null) { s0 = e; break; }
                float[] near = city.findWalkable(truck.x + 60, truck.y);
                s0.hp = s0.maxHp = 100000;
                s0.x = near[0];
                s0.y = near[1];
                for (int f = 0; f < 30 * 3 && !truck.riders.isEmpty(); f++) {
                    s0.x = near[0];
                    s0.y = near[1];
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(truck.riders.isEmpty(), "the squad climbs out at the fight (state " + truck.state + ", in fleet " + w.fleet.vehicles.contains(truck) + ", riders " + truck.riders.size() + ", d " + (int) Math.hypot(s0.x - truck.x, s0.y - truck.y) + ", near " + w.cityForceNear(truck.x, truck.y, 200) + ", soldier dead " + s0.dead + ")");
                // A city tank knocks out enemy armour in reach.
                Fleet.Vehicle cityTank = null;
                java.lang.reflect.Method placeTank = Fleet.class.getDeclaredMethod("placeTank", float.class, float.class);
                placeTank.setAccessible(true);
                cityTank = (Fleet.Vehicle) placeTank.invoke(w.fleet, truck.x + 150, truck.y);
                if (cityTank == null) cityTank = (Fleet.Vehicle) placeTank.invoke(w.fleet, truck.x - 150, truck.y);
                check(cityTank != null && !cityTank.enemy, "a city tank");
                cityTank.x = truck.x + 120;
                cityTank.y = truck.y;
                boolean out = false;
                for (int f = 0; f < 30 * 30 && !out; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                    out = truck.broken || !w.fleet.vehicles.contains(truck);
                }
                check(out, "the city's tank knocks out the APC (hp " + truck.hp + ")");
                // An attack helicopter: comes in and circles, and leaves once the enemy's beaten.
                Fleet.Vehicle heli = w.fleet.enemyIn(Fleet.ENEMY_HELI, -100, city.worldH() / 2, goal[0], goal[1]);
                check(heli != null && heli.type == Fleet.HELI && heli.enemy, "an enemy helicopter");
                for (int f = 0; f < 30 * 2; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(heli.x > -100, "flying in");
                // Paratroopers come down before they fight; drones go up; artillery falls.
                java.lang.reflect.Field t = World.class.getDeclaredField("paraCd");
                t.setAccessible(true);
                java.lang.reflect.Field dc = World.class.getDeclaredField("droneCd");
                dc.setAccessible(true);
                java.lang.reflect.Field ac = World.class.getDeclaredField("artCd");
                ac.setAccessible(true);
                w.outbreakTime = Math.max(w.outbreakTime, 60);
                w.hostileReserve = Math.max(w.hostileReserve, 20);
                t.setFloat(w, 0);
                dc.setFloat(w, 0);
                ac.setFloat(w, 0);
                int before = w.grenades.size();
                java.lang.reflect.Method life = World.class.getDeclaredMethod("armyLife");
                life.setAccessible(true);
                life.invoke(w);
                int para = 0;
                for (Entity e : w.entities) if (!e.dead && e.type == Entity.INVADER && e.paraT > 0) para++;
                check(para >= 3, "paratroopers coming down (" + para + ")");
                check(w.enemyDrones.size() >= 1 || w.grenades.size() > before, "a drone up or shells on the way");
                // The ground it holds: a district with its troops and none of ours.
                for (int f = 0; f < 30 * 6; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                java.lang.reflect.Method front = World.class.getDeclaredMethod("updateFront");
                front.setAccessible(true);
                // Clear one district of our side, put enemy soldiers in it.
                int dIdx = city.districtIndex(city.worldW() / 2, city.worldH() / 2);
                City.District dist = city.districts.get(dIdx);
                for (Entity e : w.entities) if (!e.dead && e.isArmed() && city.districtIndex(e.x, e.y) == dIdx) { e.dead = true; e.removed = true; }
                java.util.ArrayList<Entity> guards = new java.util.ArrayList<Entity>();
                float[] spot = city.randomWalkableInTown(new java.util.Random(7));
                for (int tries = 0; tries < 200 && city.districtIndex(spot[0], spot[1]) != dIdx; tries++) spot = city.randomWalkableInTown(new java.util.Random(tries));
                for (int k = 0; k < 4; k++) {
                    Entity e = w.spawn(Entity.INVADER, spot[0] + k * 6, spot[1]);
                    kit.invoke(w, e, 0, 998);
                    guards.add(e);
                }
                w.recount();
                front.invoke(w);
                check(w.enemyHolds(spot[0], spot[1]), "the enemy holds " + dist.name);
                // Someone out in it: held, walked off, freed when the police get there.
                Entity civ = w.spawn(Entity.CIVILIAN, spot[0] + 20, spot[1]);
                civ.stayIn = null;
                java.lang.reflect.Method occ = World.class.getDeclaredMethod("occupy");
                occ.setAccessible(true);
                for (int k = 0; k < 10 && civ.arrested != 3; k++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                    civ.x = spot[0] + 20;
                    civ.y = spot[1];
                    occ.invoke(w);
                }
                check(civ.arrested == 3 || civ.stayIn != null, "held by the enemy, or indoors for the curfew");
                if (civ.arrested == 3) {
                    for (Entity g : guards) { g.dead = true; g.removed = true; }
                    Entity cop = w.spawn(Entity.COP, civ.x + 10, civ.y);
                    for (int f = 0; f < 30 * 3 && civ.arrested == 3; f++) {
                        cop.x = civ.x + 10;
                        cop.y = civ.y;
                        w.update(1 / 30f);
                        w.evCount = 0;
                    }
                    check(civ.arrested == 0 && w.freed >= 1, "freed by the police");
                }
                // The enemy beaten: its armour and air pull out, and the city wins.
                for (int f = 0; f < 30 * 30 && w.warResult == 0; f++) {
                    w.hostileReserve = 0;
                    for (Entity e : w.entities) if (e.isHostile()) e.hp = 0;
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(w.warResult == 1, "the city won (" + w.warResult + ")");
                boolean pulling = true;
                for (Fleet.Vehicle v : w.fleet.vehicles) if (v.enemy && !v.broken && v.state != Fleet.RETURN && v.state != Fleet.FLY_OUT) pulling = false;
                check(pulling, "what's left of its armour pulls out");
                // Drawn: the front, the armour, the drones.
                GameView v = new GameView(new android.app.Activity());
                int[] size = {2400, 1080};
                v.layout(size[0], size[1]);
                Menu m = (Menu) field(v, "menu");
                v.startGame(c);
                v.awaitLoad();
                m.screen = Menu.NONE;
                for (int k = 0; k < 20; k++) draw(v, size);
                check(v.drawErrors == 0, "drew without an error (" + v.drawErrors + ")");
            }
        });
        test("17.5: the wounded go down and can be saved or bleed out; the after-action report", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.v[CityConfig.OPT_ZOMBIES] = 0;
                c.seed = 25;
                World w = new World(c);
                w.rnd.setSeed(25);
                w.populate(c);
                w.recount();
                java.lang.reflect.Method cleanup = World.class.getDeclaredMethod("cleanup");
                cleanup.setAccessible(true);
                // Shot (not bitten): most go down rather than die.
                int down = 0, dead = 0, tried = 0;
                for (Entity e : new java.util.ArrayList<Entity>(w.entities)) {
                    if (tried >= 40 || e.dead || e.type != Entity.CIVILIAN || e.ride != null) continue;
                    tried++;
                    e.hp = -5;
                    cleanup.invoke(w);
                    if (!e.dead && e.bleedT > 0 && e.hp > 0) down++;
                    else if (e.dead) dead++;
                }
                check(down > tried / 3 && dead > 0, "some go down, some die outright (" + down + " down, " + dead + " dead of " + tried + ")");
                // Bitten: no going down.
                Entity bitten = null;
                for (Entity e : w.entities) if (!e.dead && e.type == Entity.CIVILIAN && e.bleedT == 0) { bitten = e; break; }
                bitten.killedByZombie = true;
                bitten.hp = -1;
                cleanup.invoke(w);
                check(bitten.dead, "the bitten don't go down, they die");
                // Down, and an officer comes to them and gets them back up.
                Entity hurt = null;
                for (Entity e : w.entities) if (!e.dead && e.bleedT > 0) { hurt = e; break; }
                Entity cop = null;
                for (Entity e : w.entities) if (!e.dead && e.type == Entity.COP && e.ride == null && e.rig == null) { cop = e; break; }
                float[] near = w.city.findWalkable(hurt.x + 30, hurt.y);
                cop.x = near[0];
                cop.y = near[1];
                cop.task = Dispatch.T_NONE;
                int savedBefore = w.woundedSaved;
                for (int f = 0; f < 30 * 25 && hurt.bleedT > 0 && !hurt.dead; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(!hurt.dead && hurt.bleedT == 0 && hurt.hp > 1 && w.woundedSaved > savedBefore, "someone patched them up (" + hurt.bleedT + ", hp " + hurt.hp + ")");
                // Left alone: they bleed out.
                Entity alone = null;
                for (Entity e : w.entities) if (!e.dead && e.bleedT > 0) { alone = e; break; }
                if (alone == null) {
                    for (Entity e : w.entities) if (!e.dead && e.type == Entity.CIVILIAN && e.bleedT == 0 && e.ride == null) { alone = e; break; }
                    alone.hp = -5;
                    for (int k = 0; k < 20 && alone.bleedT <= 0 && !alone.dead; k++) {
                        alone.hp = -5;
                        cleanup.invoke(w);
                    }
                }
                if (alone != null && !alone.dead && alone.bleedT > 0) {
                    alone.bleedT = 0.5f;
                    if (alone.rescuer != null) alone.rescuer.rescueOf = null;
                    alone.rescuer = null;
                    int bled = w.bledOut;
                    for (int f = 0; f < 30 * 2; f++) {
                        w.update(1 / 30f);
                        w.evCount = 0;
                    }
                    check(alone.dead && w.bledOut > bled, "nobody got to them in time: bled out");
                }
                // Not shot at again while down.
                Entity g = w.spawn(Entity.GANGSTER, 0, 0);
                java.lang.reflect.Method foe = World.class.getDeclaredMethod("hostileFoe", Entity.class, Entity.class);
                foe.setAccessible(true);
                Entity lying = null;
                for (Entity e : w.entities) if (!e.dead && e.type == Entity.COP) { lying = e; break; }
                lying.bleedT = 30;
                check(!(Boolean) foe.invoke(w, g, lying), "the other side leaves the wounded be");
                lying.bleedT = 0;
                // Saved and loaded: the counts and who's down.
                File file = File.createTempFile("zcs", ".dat");
                SaveGame.save(w, file);
                World l = SaveGame.load(file);
                file.delete();
                check(l.woundedSaved == w.woundedSaved && l.bledOut == w.bledOut, "the record's kept");
                // The report: drawn, at the end of a game it opens itself.
                GameView v = new GameView(new android.app.Activity());
                int[] size = {1080, 2000};
                v.layout(size[0], size[1]);
                Menu m = (Menu) field(v, "menu");
                CityConfig c2 = new CityConfig();
                c2.v[CityConfig.OPT_ZOMBIES] = 2;
                c2.mode = CityConfig.MODE_GANGS;
                v.startGame(c2);
                v.awaitLoad();
                for (int k = 0; k < 3; k++) draw(v, size);
                m.screen = Menu.NONE;
                World gw = (World) field(v, "world");
                for (int f = 0; f < 30 * 30 && gw.warResult == 0; f++) {
                    gw.hostileReserve = 0;
                    for (Entity e : gw.entities) if (e.isHostile()) e.hp = 0;
                    gw.update(1 / 30f);
                    gw.evCount = 0;
                }
                check(gw.warResult == 1, "the gang war's over");
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
                check(c.mission == 0 && c.mode == m0.mode && c.zombies() == m0.foes, "the mission's city and enemy");
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
                float tx = gw.city.worldW() / 2, ty = gw.city.worldH() / 2;
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
        test("18.1: heists - crews drive in, rob, take hostages; standoffs talked down; getaways; the ending; saves", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.mode = CityConfig.MODE_HEIST;
                c.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                c.v[CityConfig.OPT_ZOMBIES] = 4;
                c.seed = 7;
                World w = new World(c);
                w.setMaxPopulation(5000);
                w.rnd.setSeed(7);
                w.populate(c);
                check(w.hostileReserve >= 2 && w.counts[Entity.GANGSTER] == 0, "crews to come, none on the streets at first (" + w.hostileReserve + ")");
                // A job: a crew drives in and goes in, the alarm goes, hostages are taken, the police box them in.
                World.Heist h = null;
                boolean robbed = false, inside = false, hostages = false, standoff = false;
                for (int f = 0; f < 30 * 400 && !(standoff && inside); f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                    for (World.Heist x : w.heists) {
                        h = x;
                        robbed |= x.phase >= World.H_ROB;
                        hostages |= x.hostages > 0;
                        standoff |= x.phase == World.H_STANDOFF;
                        if (f % 15 == 0)
                            for (Entity e : w.entities) if (!e.dead && e.squad == x.squad && w.city.insideOf(e.x, e.y) == x.b) inside = true;
                    }
                }
                check(h != null && robbed, "a crew got to its target and the robbery's on");
                check(inside, "robbers inside the building");
                check(standoff || w.heistsStopped > 0, "the police boxed them in");
                check(w.heistsStarted >= 1, "counted");
                // Hostages: held where they are, freed once the police reach them with no robber by them.
                Entity civ = null;
                for (Entity e : w.entities) if (!e.dead && e.type == Entity.CIVILIAN && e.arrested == 0 && e.ride == null) { civ = e; break; }
                World.Heist fake = new World.Heist();
                fake.squad = 999;
                fake.b = w.city.buildings.get(0);
                w.heists.add(fake);
                civ.arrested = 3;
                civ.heldBy = 999;
                civ.postX = civ.x;
                civ.postY = civ.y;
                int freed = w.freed;
                Entity cop = w.spawnCop(0, civ.x + 10, civ.y);
                for (int f = 0; f < 60 && civ.arrested != 0; f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(civ.arrested == 0 && w.freed > freed, "a hostage freed by an officer");
                w.heists.remove(fake);
                // A negotiator: a cop role of its own.
                Entity neg = w.spawnCop(Entity.ROLE_NEGOTIATOR, civ.x, civ.y);
                check(neg != null && neg.role == Entity.ROLE_NEGOTIATOR && "Negotiator".equals(Entity.ROLE_NAMES[neg.role]), "negotiators");
                // Saved and loaded: the tally kept.
                w.heistsEscaped = 1;
                w.lootStolen = 123000;
                File file = File.createTempFile("zcs", ".dat");
                SaveGame.save(w, file);
                World l = SaveGame.load(file);
                file.delete();
                check(l.mode() == CityConfig.MODE_HEIST && l.heistsEscaped == 1 && l.lootStolen == 123000 && l.heistsStarted == w.heistsStarted, "kept in a save");
                // The getaway: a crew running for it in a car is shot at, and if it reaches the edge of town it's away.
                World g = new World(Campaign.config(7));
                g.setMaxPopulation(5000);
                g.populate(g.city.cfg);
                Campaign.begin(g, "");
                g.hostileReserve = 0;
                java.lang.reflect.Method start = World.class.getDeclaredMethod("startHeist");
                start.setAccessible(true);
                check((Boolean) start.invoke(g), "a heist started");
                World.Heist gh = g.heists.get(0);
                java.lang.reflect.Method esc = World.class.getDeclaredMethod("beginEscape", World.Heist.class, String.class);
                esc.setAccessible(true);
                // (Everyone out of the car at the door, then away.)
                for (int f = 0; f < 30 * 120 && gh.phase == World.H_DRIVE; f++) {
                    g.update(1 / 30f);
                    g.evCount = 0;
                }
                if (g.heists.contains(gh) && gh.phase != World.H_ESCAPE) esc.invoke(g, gh, "test");
                boolean drove = false;
                for (int f = 0; f < 30 * 240 && g.heists.contains(gh); f++) {
                    for (Entity e : g.entities) if (e.type == Entity.COP && !e.dead) e.hp = 0;
                    g.update(1 / 30f);
                    g.evCount = 0;
                    drove |= gh.car != null && gh.car.gang == gh.squad && gh.phase == World.H_ESCAPE;
                }
                check(drove, "they drove off in the getaway car");
                check(g.heistsEscaped == 1 && g.lootStolen > 0, "and with nobody to stop them, got away (" + g.heistsEscaped + ", $" + g.lootStolen + ")");
            }
        });
        test("18.2: prison break - the riot, guards overpowered, the gate broken, escapes and recaptures, the ending, saves", new Check() {
            public void run() throws Exception {
                CityConfig c = new CityConfig();
                c.mode = CityConfig.MODE_PRISON;
                c.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                c.seed = 3;
                c.normalize();
                check(c.prisonIsland(), "a prison break is on the Prison Island");
                World w = new World(c);
                w.setMaxPopulation(5000);
                w.rnd.setSeed(3);
                w.populate(c);
                check(w.city.prison != null && w.counts[Entity.GANGSTER] == 0 && w.riotStage == 0, "quiet at first");
                // No zombies on the island: nobody's bitten.
                boolean riot = false, armed = false, gate = false, out = false;
                for (int f = 0; f < 30 * 200 && !(gate && out); f++) {
                    w.update(1 / 30f);
                    w.evCount = 0;
                    riot |= w.riotStage >= 1;
                    gate |= w.riotStage == 2 && w.prisonFallen;
                    if (f % 30 == 0)
                        for (Entity e : w.entities)
                            if (!e.dead && e.squad == World.RIOT_SQUAD && e.type == Entity.GANGSTER) {
                                armed |= e.hasGun;
                                out |= !w.inPrison(e.x, e.y);
                            }
                }
                check(riot && w.riotTotal > 10, "the riot started (" + w.riotTotal + ")");
                check(w.zombieCount() == w.counts[Entity.GANGSTER], "no zombies");
                check(armed, "guards' shotguns taken");
                check(gate, "the gate broken");
                check(out, "rioters out over the causeway");
                // A rioter badly hurt with the law right by: back in the cells.
                Entity r = null;
                for (Entity e : w.entities) if (!e.dead && e.squad == World.RIOT_SQUAD && e.type == Entity.GANGSTER) { r = e; break; }
                if (r != null) {
                    int before = w.recaptured;
                    r.hp = 5;
                    w.spawnCop(0, r.x + 8, r.y);
                    for (int f = 0; f < 90 && r.type == Entity.GANGSTER && !r.dead; f++) {
                        w.update(1 / 30f);
                        w.evCount = 0;
                    }
                    check(r.dead || (r.type == Entity.CIVILIAN && w.recaptured > before), "a hurt rioter gives up");
                }
                // Saved and loaded.
                File file = File.createTempFile("zcs", ".dat");
                SaveGame.save(w, file);
                World l = SaveGame.load(file);
                file.delete();
                check(l.mode() == CityConfig.MODE_PRISON && l.riotStage == w.riotStage && l.riotTotal == w.riotTotal && l.prisonEscaped == w.prisonEscaped
                        && l.recaptured == w.recaptured && l.prisonFallen, "kept in a save");
                // The end: nobody left rioting.
                for (int f = 0; f < 30 * 300 && w.warResult == 0; f++) {
                    for (Entity e : w.entities) if (e.type == Entity.GANGSTER) e.hp = 0;
                    w.update(1 / 30f);
                    w.evCount = 0;
                }
                check(w.warResult == 1, "the riot's over and the city held (" + w.warResult + ", " + w.prisonEscaped + " of " + w.riotTotal + " away)");
            }
        });
        test("18.3: cartel convoys and dirty cops; terror bombs made safe or going off; disasters; the alien mothership; saves; drawing", new Check() {
            public void run() throws Exception {
                // The cartel: a convoy sets off, an officer's bought.
                CityConfig c = new CityConfig();
                c.mode = CityConfig.MODE_CARTEL;
                c.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                c.v[CityConfig.OPT_ZOMBIES] = 3;
                c.seed = 21;
                World w = new World(c);
                w.setMaxPopulation(4000);
                w.populate(c);
                check(w.gangLike() && w.counts[Entity.GANGSTER] > 0 && w.gangHadBoss != 0, "cartel cells on their turf, with jefes");
                java.lang.reflect.Method convoy = World.class.getDeclaredMethod("startConvoy");
                convoy.setAccessible(true);
                convoy.invoke(w);
                boolean drove = false;
                for (Fleet.Vehicle v : w.fleet.vehicles) drove |= v.getaway && v.gang >= 0 && !v.riders.isEmpty();
                check(drove, "a convoy on the road");
                java.lang.reflect.Field bribe = World.class.getDeclaredField("bribeCd");
                bribe.setAccessible(true);
                bribe.setFloat(w, 0);
                java.lang.reflect.Method life = World.class.getDeclaredMethod("cartelLife");
                life.setAccessible(true);
                life.invoke(w);
                check(w.corruptCops == 1, "an officer bought");
                check(w.reword("THE GANGS ARE BROKEN").equals("THE CARTEL IS BROKEN"), "cartel words");
                // Terror: a bomb made safe by the bomb squad; one left alone goes off.
                CityConfig tc = new CityConfig();
                tc.mode = CityConfig.MODE_TERROR;
                tc.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                tc.v[CityConfig.OPT_ZOMBIES] = 3;
                tc.seed = 22;
                World tw = new World(tc);
                tw.setMaxPopulation(4000);
                tw.populate(tc);
                int techs = 0;
                for (Entity e : tw.entities) if (!e.dead && e.role == Entity.ROLE_BOMB_TECH) techs++;
                check(techs > 0 && tw.hostileReserve >= 3, "bomb squads at the stations, attacks to come");
                float[] spot = tw.city.randomWalkableInTown(tw.rnd);
                tw.plantBomb(spot[0], spot[1]);
                float[] bomb = tw.bombs.get(0);
                Entity tech = tw.spawnCop(Entity.ROLE_BOMB_TECH, bomb[0] + 3, bomb[1]);
                tech.task = Dispatch.T_POST;
                tech.postX = bomb[0];
                tech.postY = bomb[1];
                java.lang.reflect.Method tl = World.class.getDeclaredMethod("terrorLife");
                tl.setAccessible(true);
                java.lang.reflect.Field tcd = World.class.getDeclaredField("terrorCd");
                tcd.setAccessible(true);
                for (int s = 0; s < 12; s++) {
                    tcd.setFloat(tw, 1000);
                    tech.x = bomb[0] + 3;
                    tech.y = bomb[1];
                    tw.recount();
                    tw.update(1 / 30f);
                    tl.invoke(tw);
                }
                check(tw.bombsDefused == 1 && tw.bombs.isEmpty(), "made safe (" + tw.bombsDefused + ")");
                tech.dead = true;
                float[] far = tw.city.randomWalkableInTown(tw.rnd);
                tw.plantBomb(far[0], far[1]);
                tw.bombs.get(0)[2] = 1;
                for (Entity e : tw.entities) if (e.type == Entity.COP || e.type == Entity.SOLDIER) e.dead = true;
                tw.recount();
                tl.invoke(tw);
                check(tw.bombsExploded == 1, "and one left alone goes off");
                tw.gunmen(far[0], far[1], 3);
                int gm = 0;
                for (Entity e : tw.entities) if (!e.dead && e.squad >= World.TERROR_SQUAD && e.type == Entity.GANGSTER) gm++;
                check(gm >= 2, "gunmen");
                // Disasters: each kind, buildings and people hurt; it ends.
                CityConfig dc = new CityConfig();
                dc.mode = CityConfig.MODE_DISASTER;
                dc.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                dc.v[CityConfig.OPT_ZOMBIES] = 3;
                dc.seed = 23;
                World dw = new World(dc);
                dw.setMaxPopulation(4000);
                dw.populate(dc);
                check(dw.zombieCount() == 0 && dw.hostileReserve >= 3, "no enemy, disasters to come");
                float[] mid = {dw.city.worldW() / 2, dw.city.worldH() / 2};
                for (int k = 0; k < 4; k++) dw.startDisaster(k, mid[0], mid[1]);
                float hp0 = 0;
                for (City.Building b : dw.city.buildings) if (!b.collapsed) hp0 += b.hp;
                for (int f = 0; f < 30 * 20; f++) {
                    dw.update(1 / 30f);
                    dw.evCount = 0;
                }
                float hp1 = 0;
                for (City.Building b : dw.city.buildings) if (!b.collapsed) hp1 += b.hp;
                check(hp1 < hp0, "buildings damaged");
                boolean kinds[] = new boolean[4];
                for (World.Hazard h : dw.hazards) kinds[h.kind] = true;
                check(kinds[World.HZ_TORNADO] || dw.disastersDone > 0, "a tornado about");
                dw.hostileReserve = 0;
                for (int f = 0; f < 30 * 400 && dw.warResult == 0; f++) {
                    dw.update(1 / 30f);
                    dw.evCount = 0;
                }
                check(dw.warResult != 0 && dw.disastersDone >= 4, "the disasters end (" + dw.warResult + ", " + dw.disastersDone + ")");
                // Aliens: the mothership, drones dropping under it; shot down, the drones power down.
                CityConfig ac = new CityConfig();
                ac.mode = CityConfig.MODE_ALIENS;
                ac.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                ac.v[CityConfig.OPT_ZOMBIES] = 3;
                ac.seed = 24;
                World aw = new World(ac);
                aw.setMaxPopulation(4000);
                aw.populate(ac);
                check(aw.motherAlive && aw.motherHp > 0 && aw.counts[Entity.INVADER] > 0, "the mothership and its drones");
                aw.airstrike(aw.motherX, aw.motherY);
                check(aw.motherHp < aw.motherMax, "an airstrike on it hurts it");
                // Saved and loaded.
                File file = File.createTempFile("zcs", ".dat");
                SaveGame.save(aw, file);
                World l = SaveGame.load(file);
                file.delete();
                check(l.motherAlive && Math.abs(l.motherHp - aw.motherHp) < 1 && l.mode() == CityConfig.MODE_ALIENS, "the mothership's kept in a save");
                aw.motherHp = 1;
                for (int f = 0; f < 30 * 120 && aw.warResult == 0; f++) {
                    aw.update(1 / 30f);
                    aw.evCount = 0;
                }
                check(!aw.motherAlive && aw.warResult == 1, "shot down, and its drones power down (" + aw.warResult + ", " + aw.counts[Entity.INVADER] + ")");
                File f2 = File.createTempFile("zcs", ".dat");
                SaveGame.save(tw, f2);
                World l2 = SaveGame.load(f2);
                f2.delete();
                check(l2.bombsDefused == 1 && l2.bombsExploded == 1, "the bombs kept in a save");
                // Every new mode drawn, with its disasters, bombs and mothership on screen.
                for (int m = CityConfig.MODE_CARTEL; m <= CityConfig.MODE_ALIENS; m++) {
                    GameView v = new GameView(new android.app.Activity());
                    int[] size = {1080, 2000};
                    v.layout(size[0], size[1]);
                    Menu menu = (Menu) field(v, "menu");
                    CityConfig vc = new CityConfig();
                    vc.mode = m;
                    vc.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                    vc.v[CityConfig.OPT_ZOMBIES] = 2;
                    v.startGame(vc);
                    v.awaitLoad();
                    for (int k = 0; k < 3; k++) draw(v, size);
                    menu.screen = Menu.NONE;
                    World gw = (World) field(v, "world");
                    if (m == CityConfig.MODE_DISASTER) for (int k = 0; k < 4; k++) gw.startDisaster(k, gw.city.worldW() / 2, gw.city.worldH() / 2);
                    if (m == CityConfig.MODE_TERROR) gw.plantBomb(gw.city.worldW() / 2, gw.city.worldH() / 2);
                    float fx = m == CityConfig.MODE_ALIENS ? gw.motherX : gw.city.worldW() / 2, fy = m == CityConfig.MODE_ALIENS ? gw.motherY : gw.city.worldH() / 2;
                    v.jumpTo(fx, fy);
                    for (int k = 0; k < 60; k++) {
                        gw.update(1 / 30f);
                        gw.evCount = 0;
                    }
                    for (int k = 0; k < 3; k++) draw(v, size);
                    menu.open(Menu.REPORT);
                    draw(v, size);
                    check(v.drawErrors == 0, CityConfig.MODES[m] + " drawn without an error (" + v.drawErrors + ")");
                }
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
                Entity hid = w.spawn(Entity.GANGSTER, spot[0], spot[1]);
                hid.hp = hid.maxHp = 1e9f;
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
                // In a real gunfight, some of them make for cover, and get there.
                CityConfig g = new CityConfig();
                g.v[CityConfig.OPT_ZOMBIES] = 4;
                g.mode = CityConfig.MODE_GANGS;
                g.seed = 27;
                World gw = new World(g);
                gw.populate(g);
                int going = 0, inCover = 0;
                for (int f = 0; f < 30 * 25; f++) {
                    gw.update(1 / 30f);
                    gw.evCount = 0;
                    if (f % 15 != 0) continue;
                    for (Entity e : gw.entities) {
                        if (e.dead || gw.time - e.lastAimT > 0.6f) continue;
                        if (e.coverX >= 0) going++;
                        if (gw.inCover(e, e.coverFromX, e.coverFromY)) inCover++;
                    }
                }
                check(going > 10 && inCover > 10, "fighters make for cover and fight from it (" + going + " heading, " + inCover + " in cover)");
                // Helicopters can be shot down: an enemy one over soldiers, a city one under gang fire.
                CityConfig ac = new CityConfig();
                ac.v[CityConfig.OPT_ZOMBIES] = 1;
                ac.mode = CityConfig.MODE_ARMY;
                ac.seed = 28;
                World aw = new World(ac);
                aw.populate(ac);
                Entity sol = null;
                for (Entity e : aw.entities) if (!e.dead && e.type == Entity.SOLDIER && e.ride == null) { sol = e; break; }
                Fleet.Vehicle eh = aw.fleet.enemyIn(Fleet.ENEMY_HELI, sol.x - 60, sol.y, sol.x, sol.y);
                float hp0 = eh.hp;
                for (Entity e : aw.entities) if (!e.dead && e.type == Entity.SOLDIER && e.ride == null && Math.hypot(e.x - sol.x, e.y - sol.y) < 400) e.hp = e.maxHp = 1e6f;
                for (int f = 0; f < 30 * 20 && !eh.broken; f++) {
                    eh.x = sol.x + 40;
                    eh.y = sol.y;
                    aw.update(1 / 30f);
                    aw.evCount = 0;
                }
                check(eh.hp < hp0, "soldiers' rifles hurt an enemy helicopter (" + eh.hp + " of " + hp0 + ")");
                Fleet.Vehicle mine = null;
                for (Fleet.Vehicle v : aw.fleet.vehicles) if (v.type == Fleet.HELI && !v.enemy) { mine = v; break; }
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
