package com.instantaxtion.zombiesandbox;

import java.io.File;
import java.lang.reflect.Field;

/** The world: saves, civilians, the dead, events, safe zones and defences. */
final class WorldTests extends TestKit {
    static void register() {
        test("saves: a busy outbreak runs two minutes and loads back the same; vehicles, calls, drones and Build edits kept", FAST, 40, WorldTests.class,
                "busyOutbreakSaves", "vehiclesKept", "callsKept", "dronesKept", "buildToolEdits");
        test("civilians: run, hide or fight back; bands, hidden bites, armed hunters; off screen they wait; nobody goes down wounded", FULL, 25, WorldTests.class,
                "civilianBrain", "fightBackTogether", "hiddenBites", "bands", "huntersArmed", "keepRunning", "offScreenWait", "deadAtOnce");
        test("zombies: rot, marching hordes, alphas, mega hordes, runners, the armoured dead; they claw cars, climb heaps, follow noise and climb towers", FULL, 30, WorldTests.class,
                "rotAndMarch", "alphas", "runnersRun", "engineAndMegaHorde", "armouredDead", "clawClimbHear", "turnRatesAndPickups", "fireTower");
        test("events: the war's end, quarantine and air strikes, the giant, a riot, a prison break, a plane crash, raider gangs and looters, leaders decide", FULL, 35, WorldTests.class,
                "warEnds", "quarantine", "giantAndEvents", "raiderGangs", "looters", "heavyDestruction", "leadersDecide", "cargoPlane");
        test("safe zones and defences: walls and gates, barricades, razor wire, gun nests, barrages, roadblocks, flares and mines", FULL, 30, WorldTests.class,
                "safeZoneWalls", "engineersHelpZone", "wireNestsBarrage", "flaresAndMines", "roadblockBarriers", "barricadeKinds");
    }

    static void busyOutbreakSaves() throws Exception {
        CityConfig c = cfg(CityConfig.LARGE, 77);
        c.v[CityConfig.OPT_CIVILIANS] = 4;
        c.v[CityConfig.OPT_COPS] = 3;
        c.v[CityConfig.OPT_MILITARY] = 3;
        c.v[CityConfig.OPT_ZOMBIES] = 4;
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

    static void vehiclesKept() throws Exception {
        CityConfig c = cfg(CityConfig.TINY, 274);
        World w = new World(c);
        w.populate(c);
        for (int f = 0; f < 30 * 3; f++) w.update(1 / 30f);
        // The police command truck sent out, and a patrol car on its beat.
        float[] p = driveSpot(w, w.ops.police.truck.x, w.ops.police.truck.y, 400, 900, 3);
        Fleet.Vehicle cp = w.ops.placePost(p[0], p[1]);
        for (int f = 0; f < 30 * 4; f++) w.update(1 / 30f);
        check(cp != null && !cp.crew.isEmpty() && cp.state != Fleet.WAIT, "a command truck out on the road with its crew");
        Fleet.Vehicle car = null;
        for (Fleet.Vehicle v : w.fleet.vehicles)
            if (v.type == Fleet.CRUISER && v.patrol && v.state == Fleet.PATROL && v.crew.size() == 2 && !v.parked) car = v;
        check(car != null, "a patrol car out on its beat");
        int kept = 0;
        for (Fleet.Vehicle v : w.fleet.vehicles) if (VehicleSave.keep(v)) kept++;
        java.util.Set<Integer> crewNames = new java.util.HashSet<Integer>();
        for (Entity e : car.crew) crewNames.add(e.nameSeed);
        w.stats.recount();
        int cops = w.counts[Entity.COP];
        File f = File.createTempFile("zc274", ".dat");
        SaveGame.save(w, f);
        World l = SaveGame.load(f);
        f.delete();
        int back = 0;
        Fleet.Vehicle lcar = null, lcp = null;
        for (Fleet.Vehicle v : l.fleet.vehicles) {
            if (VehicleSave.keep(v)) back++;
            if (v.type == Fleet.CRUISER && v.number == car.number) lcar = v;
            if (v.kind == Fleet.K_COMMAND) lcp = v;
        }
        check(back == kept, "every one of them back (" + back + " of " + kept + ")");
        check(lcar != null && Math.hypot(lcar.x - car.x, lcar.y - car.y) < 1 && lcar.state == Fleet.PATROL && lcar.crew.size() == 2,
                "the patrol car where it was, on patrol, with its two officers aboard");
        java.util.Set<Integer> lNames = new java.util.HashSet<Integer>();
        if (lcar != null) for (Entity e : lcar.crew) lNames.add(e.nameSeed);
        check(lNames.equals(crewNames), "the same two officers");
        boolean onFoot = false;
        for (Entity e : l.entities) if (!e.dead && crewNames.contains(e.nameSeed) && e.type == Entity.COP) onFoot = true;
        check(!onFoot, "and not on foot beside it as well");
        check(lcp != null && l.ops.police.truck == lcp && Math.hypot(lcp.x - cp.x, lcp.y - cp.y) < 1 && lcp.crew.size() == cp.crew.size(),
                "the command truck still on the road with its crew, still the post's truck");
        l.stats.recount();
        check(l.counts[Entity.COP] == cops, "the police all counted (" + l.counts[Entity.COP] + " vs " + cops + ")");
        float x0 = lcar.x, y0 = lcar.y;
        for (int k = 0; k < 30 * 8; k++) l.update(1 / 30f);
        check(Math.hypot(lcar.x - x0, lcar.y - y0) > 20 || lcar.state != Fleet.PATROL, "and they carry on (" + (int) Math.hypot(lcar.x - x0, lcar.y - y0) + ")");
    }

    static void callsKept() throws Exception {
        CityConfig c = cfg(CityConfig.TINY, 275);
        World w = new World(c);
        w.populate(c);
        for (int f = 0; f < 30 * 3; f++) w.update(1 / 30f);
        // A patrol car out on its beat sent to a call a long way off, and an officer on foot answering it too.
        Fleet.Vehicle car = null;
        for (Fleet.Vehicle v : w.fleet.vehicles)
            if (v.type == Fleet.CRUISER && v.patrol && v.state == Fleet.PATROL && v.crew.size() == 2 && !v.parked) car = v;
        check(car != null, "a patrol car out on its beat");
        Dispatch.Incident inc = null;
        java.util.Random r = new java.util.Random(5);
        for (int k = 0; k < 200 && inc == null; k++) {
            float[] q = w.city.randomStreetSpot(r);
            if (q == null || Math.hypot(q[0] - car.x, q[1] - car.y) < 700) continue;
            inc = w.dispatch.sighting(q[0], q[1], null);
            if (inc != null && !w.fleet.respond(car, inc)) inc = null;
        }
        check(inc != null && car.incident == inc && car.state == Fleet.DRIVE, "the car on its way to a call");
        float[] p = w.city.findWalkable(inc.x + 200, inc.y);
        Entity cop = w.spawnCop(0, p[0], p[1]);
        cop.task = Dispatch.T_RESPOND;
        cop.incident = inc;
        for (int f = 0; f < 30; f++) w.update(1 / 30f);
        check(car.incident == inc && cop.task == Dispatch.T_RESPOND && cop.incident == inc && !inc.resolved, "still on their way a second on");
        File f = File.createTempFile("zc275", ".dat");
        SaveGame.save(w, f);
        World l = SaveGame.load(f);
        f.delete();
        Dispatch.Incident li = null;
        for (Dispatch.Incident o : l.dispatch.incidents) if (Math.hypot(o.x - inc.x, o.y - inc.y) < 1 && o.place.equals(inc.place)) li = o;
        check(li != null && !li.resolved && li.reported == inc.reported, "the call is on the board again, where it was");
        Fleet.Vehicle lcar = null;
        for (Fleet.Vehicle v : l.fleet.vehicles) if (v.type == Fleet.CRUISER && v.number == car.number) lcar = v;
        check(lcar != null && lcar.incident == li && lcar.state == Fleet.DRIVE && lcar.crew.size() == 2, "the car still on its way to it, lights on, its officers aboard");
        Entity lcop = null;
        for (Entity e : l.entities) if (!e.dead && e.type == Entity.COP && e.nameSeed == cop.nameSeed) lcop = e;
        check(lcop != null && lcop.task == Dispatch.T_RESPOND && lcop.incident == li, "the officer on foot still answering it");
        float d0 = (float) Math.hypot(lcar.x - li.x, lcar.y - li.y);
        for (int k = 0; k < 30 * 3; k++) l.update(1 / 30f);
        check(lcar.incident == li && (lcar.state == Fleet.SCENE || Math.hypot(lcar.x - li.x, lcar.y - li.y) < d0 - 30),
                "and the car carries on to it, not back on patrol (" + (int) d0 + " -> " + (int) Math.hypot(lcar.x - li.x, lcar.y - li.y) + ")");
    }

    static void dronesKept() throws Exception {
        CityConfig c = cfg(CityConfig.SMALL, 41);
        c.v[CityConfig.OPT_ZOMBIES] = 3;
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

    static void buildToolEdits() throws Exception {
        CityConfig c = cfg(CityConfig.LARGE, 77);
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

    static void civilianBrain() throws Exception {
        CityConfig c = cfg(0, CityConfig.LARGE, 9);
        World w = new World(c);
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
            if (e.type != Entity.CIVILIAN || e.dead || e.ride != null || e.leader != null || e.task != Dispatch.T_NONE
                    || e.militia || e.scavenge != null || e.job == Entity.J_LOOKOUT || e.inmate
                    || w.city.insideOf(e.x, e.y) != null) continue;
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
                if (e.goal == World.G_FLEE || e.goal == World.G_HIDE || e.goal == World.G_ZONE) ran = true;
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
                if (e.type != Entity.CIVILIAN || e.dead) continue;
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

    static void fightBackTogether() throws Exception {
        CityConfig c = cfg(0, CityConfig.MEDIUM, 78);
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

    }

    static void hiddenBites() throws Exception {
        CityConfig c = cfg(CityConfig.TINY, 9);
        World w = new World(c);
        w.populate(c);
        w.setMaxPopulation(100000);
        WM cp = wm("checkpoint", Entity.class, Dispatch.SafeZone.class);
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

    static void bands() throws Exception {
        CityConfig c = cfg(CityConfig.SMALL, 2301);
        World w = new World(c);
        w.populate(c);
        float[] p = w.city.findWalkable(w.city.worldW() / 2, w.city.worldH() / 2);
        // A civilian with a gun gathers frightened people near the dead. (Nobody else about for them to fall in
        // behind instead.)
        Quiet band = quiet(w, p[0], p[1], 300);
        Entity lead = w.spawn(Entity.CIVILIAN, p[0] + 20, p[1]);
        w.armCivilian(lead, 12);
        java.util.List<Entity> scared = new java.util.ArrayList<Entity>();
        for (int k = 0; k < 4; k++) {
            Entity e = w.spawn(Entity.CIVILIAN, p[0] + 26 + k * 4, p[1] + 6);
            if (e != null) scared.add(e);
        }
        w.spawn(Entity.ZOMBIE, p[0] + 60, p[1] + 60);
        w.outbreak = true;
        band.keep(lead).keepAll(scared);
        for (int f = 0; f < 3; f++) step(w, band);
        for (Entity e : scared) {
            e.fleeTimer = 5;
            e.task = Dispatch.T_NONE;
            e.leader = null;
        }
        lead.lodZ = 5;
        WM bands = wm("bands");
        bands.setAccessible(true);
        bands.invoke(w);
        int joined = 0;
        for (Entity e : scared) if (e.leader == lead) joined++;
        check(joined >= 2, "frightened people fall in behind someone with a gun (" + joined + ")");
    }

    static void huntersArmed() throws Exception {
        CityConfig c = cfg(CityConfig.LARGE, 3);
        World w = new World(c);
        w.populate(c);
        int hunters = 0, armed = 0;
        for (Entity e : w.entities)
            if (e.trade() == Entity.TR_HUNTER || e.trade() == Entity.TR_VETERAN) {
                hunters++;
                if (e.canShoot()) armed++;
            }
        check(hunters > 0 && armed >= hunters * 0.9f, "hunters and ex-soldiers about, armed (" + armed + " of " + hunters + ")");
        CityConfig weak = c.copy(), strong = c.copy();
        weak.force = 0;
        strong.force = 2;
        check(weak.cops(10000) < c.cops(10000) && c.cops(10000) < strong.cops(10000), "police strength follows the setting");
    }

    static void keepRunning() throws Exception {
        CityConfig c = cfg(0, CityConfig.MEDIUM, 77);
        World w = new World(c);
        w.populate(c);
        w.viewX0 = 0;
        w.viewY0 = 0;
        w.viewX1 = w.city.worldW();
        w.viewY1 = w.city.worldH();
        w.update(1 / 30f);
        // Someone who has just seen one keeps running after it's out of sight.
        // (Someone timid and unarmed just out and about in town - not one who'd stand and fight it - with open ground
        // in front of them for it to appear on, in plain sight.)
        Entity runner = null;
        float rx = 0, ry = 0;
        for (Entity e : w.entities) {
            if (e.type != Entity.CIVILIAN || e.dead || e.task != Dispatch.T_NONE || !w.city.inTown(e.x, e.y) || e.aloft || e.hasGun || e.isArmed()
                    || World.bravery(e) > 0.4f || e.leader != null || e.ride != null || w.city.insideOf(e.x, e.y) != null
                    || e.job == Entity.J_LOOKOUT || e.job == Entity.J_RELIEF || e.job == Entity.J_HIKER) continue;
            float zx = e.x + (float) Math.cos(e.angle) * 30, zy = e.y + (float) Math.sin(e.angle) * 30;
            float[] q = w.city.findWalkable(zx, zy);
            if (q != null && Math.hypot(q[0] - zx, q[1] - zy) < 6 && w.city.los(e.x, e.y, q[0], q[1])) {
                runner = e;
                rx = q[0];
                ry = q[1];
            }
        }
        check(runner != null, "someone out in town");
        {
            Quiet calm = quiet(w, runner.x, runner.y, 300, runner);
            Entity scare = w.spawn(Entity.ZOMBIE, rx, ry);
            calm.keep(scare);
            for (int f = 0; f < 10; f++) step(w, calm);
            float sx = scare.x, sy = scare.y;
            scare.dead = true;
            scare.removed = true;
            for (int f = 0; f < 30 * 6; f++) step(w, calm);
            // ((17.1) Or gone into a building to hide.)
            check(runner.dead || runner.task != Dispatch.T_NONE || runner.fleeTimer > 0 || Math.hypot(runner.x - sx, runner.y - sy) > 90
                            || runner.stayIn != null || w.city.insideOf(runner.x, runner.y) != null,
                    "still getting away six seconds after it's out of sight (" + (int) Math.hypot(runner.x - sx, runner.y - sy) + " away)");
        }
    }

    static void offScreenWait() throws Exception {
        CityConfig c = cfg(CityConfig.MEDIUM, 2210);
        World w = new World(c);
        w.populate(c);
        // The screen on one corner of the map.
        w.viewX0 = 0;
        w.viewY0 = 0;
        w.viewX1 = 400;
        w.viewY1 = 800;
        for (int f = 0; f < 30 * 3; f++) { w.update(1 / 30f); w.evCount = 0; }
        Entity calm = null;
        for (Entity e : w.entities)
            if (e.type == Entity.CIVILIAN && !e.dead && e.task == Dispatch.T_NONE && e.rig == null && e.ride == null && e.x > 2000 && e.y > 2000) { calm = e; break; }
        check(calm != null, "someone calm off screen");
        float x0 = calm.x, y0 = calm.y;
        for (int f = 0; f < 30 * 3; f++) { w.update(1 / 30f); w.evCount = 0; }
        check(calm.x == x0 && calm.y == y0, "in peace, off screen, they wait where they are");
        float[] p = w.city.findWalkable(calm.x + 300, calm.y);
        w.spawn(Entity.ZOMBIE, p[0], p[1]);
        w.recount();
        boolean moved = false;
        for (int f = 0; f < 30 * 4 && !moved; f++) {
            w.update(1 / 30f);
            w.evCount = 0;
            moved = calm.x != x0 || calm.y != y0 || calm.dead;
        }
        check(moved, "the first of the dead and they're going about their day again");
    }

    static void deadAtOnce() throws Exception {
        // ((20.0) Shot is shot: no going down to bleed and be saved.)
        CityConfig c = cfg(CityConfig.TINY, 25);
        World w = new World(c);
        w.populate(c);
        Entity shot = null;
        for (Entity e : w.entities) if (!e.dead && e.type == Entity.CIVILIAN && e.ride == null) { shot = e; break; }
        int n = w.entities.size();
        shot.hp = 0;
        w.update(1 / 30f);
        w.evCount = 0;
        check(shot.dead && !w.entities.contains(shot) && w.entities.size() < n, "someone with no health left is dead at once");
    }

    static void rotAndMarch() throws Exception {
        CityConfig c = cfg(0, CityConfig.MEDIUM, 77);
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
        WM mv = wm("move", Entity.class, float.class);
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
        WM pr = wm("pickRoam", Entity.class);
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
    }

    static void alphas() throws Exception {
        CityConfig c = cfg(CityConfig.TINY, 82);
        World w = new World(c);
        w.populate(c);
        w.update(1 / 30f);
        Entity cop = null;
        for (Entity e : w.entities) if (!e.dead && e.type == Entity.COP && e.ride == null && w.city.insideOf(e.x, e.y) == null) { cop = e; break; }
        float sx = cop.x, sy = cop.y;
        // (Nobody about to get in the way, nor to shoot the test's dead first.)
        Quiet nobody = quiet(w, sx, sy, 500);
        step(w, nobody);
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
        WM th = wm("thinkHorde", Entity.class);
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
        WM pick = wm("pickTarget", Entity.class, float.class);
        pick.setAccessible(true);
        w.update(1 / 30f);
        w.evCount = 0;
        sniper.fear = 1;
        Entity chosen = (Entity) pick.invoke(w, sniper, 400f);
        boolean seesAlpha = w.city.los(sniper.x, sniper.y, lead.x, lead.y);
        check(!seesAlpha || chosen == lead, "a sniper picks the alpha out of the horde (" + (chosen == null ? "nobody" : "one at " + (int) World.hypot(chosen.x - sniper.x, chosen.y - sniper.y))
                + ", alpha at " + (int) World.hypot(lead.x - sniper.x, lead.y - sniper.y) + " dead " + lead.dead + ")");
        // Fire discipline: with no alpha to go for, a second gun takes a different one of the crowd. (Further back, out
        // past the range where one nearly on them gets every gun.)
        lead.alpha = false;
        float[] back = w.city.findWalkable(sniper.x - 50, sniper.y);
        if (back == null || !w.city.los(back[0], back[1], horde.get(1).x, horde.get(1).y)) back = new float[]{sniper.x, sniper.y};
        Entity s2 = w.spawnSoldier(Entity.ROLE_RIFLE, back[0], back[1] + 4), s3 = w.spawnSoldier(Entity.ROLE_RIFLE, back[0], back[1] - 4);
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
        WM mop = wm("mopUp");
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
        WM un = wm("unstick");
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

    static void runnersRun() throws Exception {
        CityConfig c = cfg(CityConfig.TINY, 96);
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
    }

    static void engineAndMegaHorde() throws Exception {
        CityConfig c = cfg(CityConfig.SMALL, 220);
        World w = new World(c);
        w.populate(c);
        w.update(1 / 30f);
        float[] p = w.city.findWalkable(w.city.worldW() / 2, w.city.worldH() / 2);
        // An engine going by: the dead within earshot turn to it.
        // (No armed civilian about to settle it with one shot before the engine goes by.)
        for (Entity o : w.entities)
            if (o.type == Entity.CIVILIAN && Math.hypot(o.x - p[0], o.y - p[1]) < 400) o.ammo = o.reserve = 0;
        Entity z = w.spawn(Entity.ZOMBIE, p[0], p[1]);
        w.recount();
        w.update(1 / 30f);
        z.memory = 0;
        z.noiseTimer = 0;
        w.engineLure(p[0] + 80, p[1], 150);
        check(z.noiseTimer > 0 && Math.abs(z.noiseX - (p[0] + 80)) < 1, "the dead turn towards an engine");
        // A mega horde: hundreds, one horde behind an alpha.
        java.util.ArrayList<Entity> mega = w.megaHorde(p[0], p[1]);
        check(mega.size() >= 200 && mega.get(0).alpha && mega.get(0).leadsHorde, "a mega horde of " + mega.size() + " behind an alpha");
        for (Entity m : mega) { m.dead = true; m.removed = true; }
        w.update(1 / 30f);
        w.evCount = 0;
    }

    static void armouredDead() throws Exception {
        CityConfig c = cfg(CityConfig.TINY, 27);
        World w = new World(c);
        w.populate(c);
        w.update(1 / 30f);
        check(w.zombieCount() == 0 && w.counts[Entity.ARMOURED] == 0, "none by themselves");
        float[] p = w.city.randomStreetSpot(new java.util.Random(4));
        for (Entity e : w.entities) if (!e.dead && Math.hypot(e.x - p[0], e.y - p[1]) < 700) e.dead = e.removed = true;
        // The armoured dead: a soldier always comes back armoured, its helmet good for one shot to the head.
        Entity z = w.spawn(Entity.ARMOURED, p[0] + 20, p[1]);
        check(z.armored && z.helmet && z.hp > 120 && z.head != z.skin, "an armoured zombie: tougher, in a helmet");
        w.loseHelmet(z);
        check(!z.helmet && z.head == z.skin && !z.dead, "the helmet takes the first shot to the head and comes off");
        // (Half a dozen: now and then one comes back a runner or a brute instead.)
        for (int k = 0; k < 6; k++) {
            Entity sol = w.spawnSoldier(Entity.ROLE_RIFLE, p[0] - 30 - k * 8, p[1]);
            sol.killedByZombie = true;
            sol.hp = 0;
        }
        int rose = 0, others = 0;
        for (int f = 0; f < 30 * 12 && rose < 3; f++) {
            w.update(1 / 30f);
            w.evCount = 0;
            rose = 0;
            others = 0;
            for (Entity e : w.entities) {
                if (e.dead || e == z || e.origin != Entity.SOLDIER) continue;
                if (e.type == Entity.ARMOURED) rose++;
                else if (e.type == Entity.ZOMBIE) others++;
            }
        }
        check(rose >= 3 && others == 0, "soldiers bitten to death get up armoured, never as ordinary zombies (" + rose + ", " + others + ")");
        // Kept in a save; and the old kinds in those numbers, from an older save, would be ordinary zombies.
        File f = File.createTempFile("zc27", ".dat");
        SaveGame.save(w, f);
        World l = SaveGame.load(f);
        f.delete();
        l.recount();
        w.recount();
        check(l.counts[Entity.ARMOURED] == w.counts[Entity.ARMOURED] && l.counts[Entity.ARMOURED] >= 1 && Entity.current(Entity.SCREAMER) == Entity.ZOMBIE, "kept in a save; a screamer in an older save loads as a zombie");
    }

    static void clawClimbHear() throws Exception {
        CityConfig c = cfg(CityConfig.SMALL, 31);
        c.v[CityConfig.OPT_COPS] = 0;
        c.v[CityConfig.OPT_MILITARY] = 0;
        World w = new World(c);
        w.populate(c);
        w.update(1 / 30f);
        java.util.Random tr = new java.util.Random(26);
        // A parked car with someone sheltering in it, and the dead all round it.
        float[] s = w.city.randomStreetSpot(tr);
        Fleet.Vehicle v = w.fleet.placeCar(s[0], s[1]);
        v.parked = true;
        v.state = Fleet.ABANDONED;
        v.speed = 0;
        v.field = null;
        Entity p = w.spawn(Entity.CIVILIAN, v.x, v.y);
        p.dead = p.removed = true;
        v.riders.add(p);
        for (int k = 0; k < 6; k++) w.spawn(Entity.ZOMBIE, v.x + (float) Math.cos(k) * 8, v.y + (float) Math.sin(k) * 8);
        float hp0 = v.hp;
        // (Nobody else about for them to go after instead, nor to shoot them off the car.)
        Quiet car = quiet(w, v.x, v.y, 300, v, p);
        for (int f = 0; f < 30 * 8 && v.riders.contains(p); f++) step(w, car);
        check(v.hp < hp0, "the dead on a parked car wear it down (" + (int) v.hp + "/" + (int) hp0 + ")");
        check(!v.riders.contains(p) && w.dragged >= 1, "and drag out the one inside (broken " + v.broken + ", dragged " + w.dragged + ", riders " + v.riders.size() + ", breakT " + v.breakT + ", on it " + v.clawN + ", speed " + v.speed + ", occ " + v.occupied() + ")");
        // A ring of barricades round someone: one of the dead alone climbs it at the usual pace, a heap of them faster.
        boolean heaped = false;
        String got = "";
        for (int tries = 0; tries < 300 && !heaped; tries++) {
            float[] q = w.city.randomStreetSpot(tr);
            int tx = (int) (q[0] / City.T), ty = (int) (q[1] / City.T);
            float cx = (tx + 0.5f) * City.T, cy = (ty + 0.5f) * City.T;
            if (w.city.solid[ty * w.city.w + tx] || World.hypot(cx - v.x, cy - v.y) < 400) continue;
            java.util.ArrayList<float[]> ring = new java.util.ArrayList<float[]>();
            boolean ok = true;
            for (int dy = -1; dy <= 1 && ok; dy++)
                for (int dx = -1; dx <= 1 && ok; dx++) {
                    if ((dx == 0 && dy == 0) || w.city.solid[(ty + dy) * w.city.w + tx + dx]) continue;
                    float[] b = w.placeBarricade(cx + dx * City.T, cy + dy * City.T);
                    if (b == null) ok = false;
                    else ring.add(b);
                }
            Entity h = ok && ring.size() >= 5 ? w.spawn(Entity.CIVILIAN, cx, cy) : null;
            if (h == null) {
                for (float[] b : ring) w.removeBarricade(b);
                continue;
            }
            java.util.ArrayList<Entity> heap = new java.util.ArrayList<Entity>();
            for (int k = 0; k < 10; k++) {
                Entity z = w.spawn(Entity.ZOMBIE, cx + 45 + (k % 4) * 6, cy - 9 + (k / 4) * 6);
                if (z == null) continue;
                z.angle = (float) Math.PI;
                heap.add(z);
            }
            int climbs = 0;
            float dur = 0;
            boolean[] was = new boolean[heap.size()];
            for (int f = 0; f < 30 * 6; f++) {
                h.stun = 10;
                h.hp = h.maxHp;
                h.infected = false;
                h.x = cx;
                h.y = cy;
                w.update(1 / 30f);
                for (int k = 0; k < heap.size(); k++) {
                    Entity z = heap.get(k);
                    if (z.vaultT > 0 && !was[k]) {
                        climbs++;
                        dur += z.vaultDur;
                    }
                    was[k] = z.vaultT > 0;
                }
            }
            // (The quickest one of the ordinary dead ever gets over on its own is 0.85 of CLIMB_TIME.)
            heaped = climbs >= 3 && dur / climbs < World.CLIMB_TIME * 0.85f;
            got += " " + climbs + "/" + (climbs > 0 ? dur / climbs : 0);
            for (Entity z : heap) z.dead = z.removed = true;
            h.dead = h.removed = true;
            for (float[] b : ring) w.removeBarricade(b);
            if (got.length() > 60) break;
        }
        check(World.pileBonus(1) == 1 && World.pileBonus(8) > 2, "a heap of the dead gets over a wall faster than one");
        check(heaped, "ten of them at a barricade climb over each other, quicker than one alone could (climbs/seconds each:" + got + ")");
        // Gunfire: a rifle shot isn't heard from a few streets off, but sustained fire draws the dead in from there.
        for (Entity e : w.entities) if (e.isZombie()) e.dead = e.removed = true;
        Entity z = null;
        float[] g = null;
        for (int tries = 0; tries < 200 && z == null; tries++) {
            g = w.city.randomStreetSpot(tr);
            float a = tr.nextFloat() * 6.283f;
            float[] zp = w.city.findWalkable(g[0] + (float) Math.cos(a) * 470, g[1] + (float) Math.sin(a) * 470);
            if (zp == null || World.hypot(zp[0] - g[0], zp[1] - g[1]) < 430 || World.hypot(zp[0] - g[0], zp[1] - g[1]) > 520) continue;
            z = w.spawn(Entity.ZOMBIE, zp[0], zp[1]);
        }
        check(z != null, "one of the dead a few streets from a fight");
        w.update(1 / 30f);
        w.noise(g[0], g[1], 265 * World.BATTLE_CARRY);
        check(z.noiseTimer <= 0, "a single shot doesn't carry that far");
        boolean drawn = false;
        // (Nobody within sight of it to go after instead; and the only noise it hears is this one.)
        Quiet near = quiet(w, z.x, z.y, 600).follow(z);
        for (int f = 0; f < 30 * 8 && !drawn; f++) {
            if (f % 15 == 0) {
                // (A sound somewhere else on the map - a car alarm, a fight across town - isn't this one.)
                if (World.hypot(z.noiseX - g[0], z.noiseY - g[1]) > 120) z.noiseTimer = 0;
                // (Nor is it after someone it saw: it has nothing to do but listen.)
                z.memory = z.searchTimer = 0;
                w.battleNoise(g[0], g[1], 265);
            }
            step(w, near);
            drawn = z.noiseTimer > 0 && World.hypot(z.noiseX - g[0], z.noiseY - g[1]) < 120;
        }
        check(drawn, "but a fight going on draws it in (noise " + z.noiseTimer + " at " + (int) World.hypot(z.noiseX - g[0], z.noiseY - g[1]) + ", memory " + z.memory + ", dead " + z.dead + ", away " + (int) World.hypot(z.x - g[0], z.y - g[1]) + ")");
    }

    static void turnRatesAndPickups() throws Exception {
        CityConfig c = cfg(0, CityConfig.LARGE, 12);
        World w = new World(c);
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
        // No crawlers (nor giants), and far fewer of the bitten come back as anything but an ordinary zombie.
        int special = 0;
        // ((12.4) The usual rates: not a strain with many runners or brutes.)
        w.trait[World.TR_RUNNERS] = w.trait[World.TR_BRUTES] = false;
        for (int k = 0; k < 10000; k++) {
            int t = w.turnType(k % 2 == 0 ? Entity.CIVILIAN : Entity.COP);
            check(t != Entity.GIANT, "nobody comes back a giant");
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

    static void fireTower() throws Exception {
        // Up a fire tower: they have to climb to get the lookout (it takes them a while).
        World tw = wildWorld(9);
        check(tw.towerKeeper.length > 0, "a lookout up a fire tower");
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
    }

    static void warEnds() throws Exception {
        CityConfig c = cfg(CityConfig.LARGE, 9);
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

    static void quarantine() throws Exception {
        CityConfig c = new CityConfig();
        c.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
        c.v[CityConfig.OPT_ZOMBIES] = 0;
        c.exodus = CityConfig.EXODUS_ALL;
        c.seed = 41;
        c.normalize();
        c.seed = 41;
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
        WM qc = wm("quarantineCity");
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
        WM as = wm("callAirStrike", int.class, boolean.class);
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

    static void giantAndEvents() throws Exception {
        CityConfig c = cfg(CityConfig.MEDIUM, 77);
        World w = new World(c);
        w.populate(c);
        w.update(1 / 30f);
        check(w.city.prison != null, "the map has a prison");
        // The giant: spawned by hand, a squad shooting at it, a barricade in its way. (Measured as it comes, not
        // as the world's strain of the dead happens to make it.)
        plainStrain(w);
        java.util.Random tr = new java.util.Random(25);
        Entity g = null;
        float[] bar = null;
        for (int k = 0; k < 400 && bar == null; k++) {
            float[] s = w.city.randomStreetSpot(tr);
            bar = w.placeBarricade(s[0] + City.T * 1.5f, s[1]);
            if (bar == null) continue;
            g = w.spawn(Entity.GIANT, s[0], s[1]);
            if (g == null) {
                w.removeBarricade(bar);
                bar = null;
            }
        }
        check(g != null && g.type == Entity.GIANT && g.isZombie() && g.maxHp >= World.GIANT_HP * 0.8f, "a giant, spawned by hand (" + (g == null ? "-" : g.maxHp) + ")");
        java.util.ArrayList<Entity> squad = w.spawnGroup(World.FIRE_TEAM, false, g.x, g.y - 70);
        float hp0 = g.hp;
        for (int f = 0; f < 30 * 3; f++) w.update(1 / 30f);
        check(!w.barriers.contains(bar), "it smashes the barricade in a moment");
        check(!g.dead && g.hp > 0, "it stands up to a fire team for three seconds (" + (int) g.hp + "/" + (int) g.maxHp + ")");
        check(g.hp < hp0, "and they do hurt it (" + (int) hp0 + " to " + (int) g.hp + ")");
        for (int k = 0; k < 2000; k++) check(w.turnType(Entity.CIVILIAN) != Entity.GIANT, "nobody turns into a giant");
        for (Entity e : squad) e.dead = e.removed = true;
        // The riot: the crowd, riot police, no zombies made; it ends.
        float[] rs = w.city.randomStreetSpot(tr);
        int zBefore = w.zombieCount() - 1;
        for (Entity e : w.entities) if (e.isZombie() && e != g) e.dead = e.removed = true;
        check(w.riot(rs[0], rs[1]), "a riot starts");
        int rioters = 0;
        for (Entity e : w.entities) if (e.riotUntil > w.time) rioters++;
        check(rioters >= 10, "a crowd riots (" + rioters + ")");
        for (int f = 0; f < 30 * 5; f++) w.update(1 / 30f);
        int z = 0;
        for (Entity e : w.entities) if (e.isZombie() && !e.dead && e != g) z++;
        check(z == 0, "a riot makes no zombies (" + z + ", " + zBefore + " before)");
        w.riot[2] = w.time;
        for (int f = 0; f < 45; f++) w.update(1 / 30f);
        check(w.riot == null, "and calms down");
        // The prison break.
        check(w.prisonBreak(), "the inmates break out");
        int loose = 0;
        for (Entity e : w.entities) if (e.escapee && !e.dead) loose++;
        check(loose >= 20 && !w.prisonBreak(), "every inmate is out (" + loose + "), and only once");
        for (int f = 0; f < 30 * 4; f++) w.update(1 / 30f);
        // The plane crash.
        float[] ps = w.city.randomStreetSpot(tr);
        int people = w.counts[Entity.CIVILIAN];
        check(w.planeCrash(ps[0], ps[1]), "a plane is coming down");
        for (int f = 0; f < 30 * 9 && w.crashes.isEmpty(); f++) w.update(1 / 30f);
        check(w.crashes.size() == 1, "it crashes, leaving a burning wreck");
        for (int f = 0; f < 30 * 2; f++) w.update(1 / 30f);
        check(w.counts[Entity.CIVILIAN] > people - 10, "with survivors walking out of it");
        // The airstrike: free, once every minute and a half.
        check(w.freeAirstrike(ps[0] + 300, ps[1]) && !w.freeAirstrike(ps[0], ps[1]) && w.airstrikeWait() > 80, "an airstrike, then a wait ("
                + w.airstrikeWait() + "s)");
        // All of it kept in a save.
        java.io.File file = java.io.File.createTempFile("zc25", ".dat");
        SaveGame.save(w, file);
        World l = SaveGame.load(file);
        file.delete();
        int giants = 0, esc = 0;
        for (Entity e : l.entities) {
            if (e.type == Entity.GIANT) giants++;
            if (e.escapee) esc++;
        }
        check(giants == 1 && esc > 0 && l.crashes.size() == 1 && l.airstrikeWait() > 70, "the giant, the escapees, the wreck and the cooldown kept in a save ("
                + giants + ", " + esc + ", " + l.crashes.size() + ", " + l.airstrikeWait() + ")");
        l.time += World.AIRSTRIKE_COOLDOWN;
        check(l.freeAirstrike(ps[0], ps[1]), "ready again once the wait is over");
    }

    static void raiderGangs() throws Exception {
        CityConfig c = cfg(CityConfig.SMALL, 52);
        World w = new World(c);
        w.populate(c);
        WM arm = wm("armCivilian", Entity.class, int.class);
        arm.setAccessible(true);
        // A long outbreak, the dead far away: gangs form among armed survivors.
        java.util.Random r = new java.util.Random(5);
        float[] far = w.city.randomWalkable(r);
        for (int z = 0; z < 3; z++) w.spawn(Entity.ZOMBIE, far[0], far[1]);
        for (Entity e : w.entities) if (!e.dead && e.type == Entity.CIVILIAN && r.nextFloat() < 0.3f) arm.invoke(w, e, 30);
        for (int f = 0; f < 30 * 3; f++) w.update(1 / 30f);
        check(w.outbreak, "an outbreak on");
        w.outbreakTime = 400;
        WM rd = wm("raiders");
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

    static void looters() throws Exception {
        // Looters: once the outbreak's gone on a while, looting gangs form.
        CityConfig lc = cfg(CityConfig.SMALL, 32);
        lc.v[CityConfig.OPT_ZOMBIES] = 5;
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
        WM raid = wm("raiders");
        raid.setAccessible(true);
        for (int k = 0; k < 600 && formed.getInt(lw) == 0; k++) {
            lw.outbreak = true;
            lw.outbreakTime = Math.max(lw.outbreakTime, 400);
            lw.recount();
            raid.invoke(lw);
        }
        check(formed.getInt(lw) > 0, "looters out (" + formed.getInt(lw) + " gangs by " + (int) lw.outbreakTime + "s)");
    }

    static void heavyDestruction() throws Exception {
        // Heavy destruction: the same blast does far more.
        float[] lost = new float[2];
        for (int k = 0; k < 2; k++) {
            CityConfig hc = cfg(CityConfig.SMALL, 33);
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

    static void leadersDecide() throws Exception {
        CityConfig c = new CityConfig();
        c.v[CityConfig.OPT_ZOMBIES] = 0;
        c.seed = 3;
        World w = new World(c);
        w.populate(c);
        // The leaders take their decisions as the outbreak goes on.
        w.outbreak = true;
        w.alert = 2;
        for (int i = 0; i < 40; i++) w.spawn(Entity.ZOMBIE, w.city.worldW() / 2f + i * 3, w.city.worldH() / 2f);
        w.update(1 / 30f);
        for (int i = 0; i < 80 && w.decisions == 0; i++) w.leadersDecide(100);
        check(w.decisions != 0, "the city's leaders make an emergency decision (" + w.command + ")");
    }

    static void cargoPlane() throws Exception {
        CityConfig c = cfg(CityConfig.SMALL, 2301);
        World w = new World(c);
        w.populate(c);
        float[] p = w.city.findWalkable(w.city.worldW() / 2, w.city.worldH() / 2);
        // The cargo plane over a zone drops its three crates.
        java.lang.reflect.Field tf = World.class.getDeclaredField("cargoTarget");
        tf.setAccessible(true);
        tf.set(w, new float[]{p[0], p[1]});
        w.cargoPlane = new float[]{p[0] - 800, p[1], 1, 0, 3, 0};
        int before = w.pickups.size();
        for (int f = 0; f < 30 * 6 && w.cargoPlane != null; f++) w.update(1 / 30f);
        check(w.pickups.size() >= before + 3, "the cargo plane drops its crates (" + (w.pickups.size() - before) + ")");
    }

    static void safeZoneWalls() throws Exception {
        CityConfig c = cfg(0, CityConfig.MEDIUM, 77);
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
    }

    static void engineersHelpZone() throws Exception {
        CityConfig c = cfg(0, CityConfig.MEDIUM, 77);
        World w = new World(c);
        w.populate(c);
        Fleet.Vehicle dozer = null;
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

    static void wireNestsBarrage() throws Exception {
        CityConfig c = cfg(CityConfig.TINY, 81);
        World w = new World(c);
        w.populate(c);
        w.update(1 / 30f);
        Entity cop = null;
        for (Entity e : w.entities) if (!e.dead && e.type == Entity.COP && e.ride == null && w.city.insideOf(e.x, e.y) == null) { cop = e; break; }
        check(cop != null, "a cop on the street");
        float sx = cop.x, sy = cop.y;
        // (Nobody about to get in the way, nor to shoot the test's dead first.)
        Quiet nobody = quiet(w, sx, sy, 500);
        step(w, nobody);
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
            hold(z, coil[0], coil[1]);
            z.noiseX = coil[0] + 400;
            z.noiseY = coil[1];
            z.noiseTimer = 10;
            step(w, nobody);
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
        WM ew = wm("engineersWire");
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
        WM sit = wm("sitrep");
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

    static void flaresAndMines() throws Exception {
        CityConfig c = cfg(CityConfig.SMALL, 71);
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
        // (Hurt by it - and then out of the way, so it doesn't wander onto the next one.)
        boolean hurt = zm.dead || zm.hp < zm.maxHp;
        zm.hp = 0;
        for (int f = 0; f < 20; f++) {
            w.update(1 / 30f);
            w.evCount = 0;
        }
        check(w.minesBlown == 1 && w.mines.size() == laid - 1 && hurt, "one of the dead does (" + w.minesBlown + " blown, "
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

    static void roadblockBarriers() throws Exception {
        CityConfig c = cfg(0, CityConfig.MEDIUM, 78);
        World w = new World(c);
        w.populate(c);
        for (int f = 0; f < 30; f++) w.update(1 / 30f);
        // A roadblock's barriers across a road: the dead are held at them till they push them over.
        // (A straight stretch of road the barriers go up across: the first of the town's that takes them.)
        java.util.Random r = new java.util.Random(5);
        Fleet.Vehicle v = null;
        int dir = 0;
        float ax = 0, ay = 0;
        for (int k = 0; k < 20000 && v == null; k++) {
            float[] q = w.city.randomWalkableInTown(r);
            int tx = (int) (q[0] / City.T), ty = (int) (q[1] / City.T);
            int d = w.city.roadDirAt(tx, ty);
            if (d != 1 && d != 2) continue;
            Fleet.Vehicle t = w.fleet.placeTank(tx * City.T + City.T / 2f, ty * City.T + City.T / 2f);
            if (t == null) continue;
            w.fleet.vehicles.remove(t);
            t.x = tx * City.T + City.T / 2f;
            t.y = ty * City.T + City.T / 2f;
            t.blockDir = d;
            float bx0 = d == 1 ? 0 : 1, by0 = d == 1 ? 1 : 0;
            // The zone lies one way along the road; the dead come from the other.
            w.fleet.putUpBarrier(t, t.x + bx0 * 300, t.y + by0 * 300);
            if (t.barHp > 0 && Math.hypot(t.barX1 - t.barX0, t.barY1 - t.barY0) >= 12) {
                v = t;
                dir = d;
                ax = bx0;
                ay = by0;
            }
        }
        check(v != null, "barriers up across the road");
        float bx = (v.barX0 + v.barX1) / 2, by = (v.barY0 + v.barY1) / 2;
        // (Nobody else about: no one to shoot the dead before they reach it, or to draw them off elsewhere.)
        Quiet calm = quiet(w, bx, by, 500);
        float[] bait = w.city.findWalkable(bx + ax * 30, by + ay * 30);
        Entity held = w.spawn(Entity.CIVILIAN, bait[0], bait[1]);
        calm.keep(held);
        java.util.List<Entity> pack = new java.util.ArrayList<Entity>();
        for (int k = 0; k < 8; k++) {
            float[] p = w.city.findWalkable(bx - ax * (30 + k * 4) + ay * (k % 3 - 1) * 5, by - ay * (30 + k * 4) + ax * (k % 3 - 1) * 5);
            if (p != null) pack.add(w.spawn(Entity.ZOMBIE, p[0], p[1]));
        }
        int crossedEarly = 0;
        boolean wentDown = false;
        for (int f = 0; f < 30 * 40 && !wentDown; f++) {
            held.speed = held.runSpeed = 0;
            hold(held, bait[0], bait[1]);
            held.hp = held.maxHp;
            step(w, calm);
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

    }

    static void barricadeKinds() throws Exception {
        CityConfig c = cfg(CityConfig.RIVER_CITY, CityConfig.SMALL, 2201);
        World w = new World(c);
        City city = w.city;
        // Barricades: three kinds, kept in a save.
        w.populate(c);
        w.update(1 / 30f);
        float[] p = city.randomWalkable(new java.util.Random(3));
        float[] wood = null, bags = null, wall = null;
        java.util.Random tr = new java.util.Random(5);
        for (int k = 0; k < 400 && (wood == null || bags == null || wall == null); k++) {
            float[] spot = city.randomWalkable(tr);
            if (wood == null) wood = w.placeBarricade(spot[0], spot[1]);
            else if (bags == null) bags = w.placeBarricade(spot[0], spot[1], 1);
            else wall = w.placeBarricade(spot[0], spot[1], 2);
        }
        check(wood != null && bags != null && wall != null && wood[2] < bags[2] && bags[2] < wall[2], "sandbags tougher than planks, concrete tougher still ("
                + (wood == null ? "-" : wood[2]) + ", " + (bags == null ? "-" : bags[2]) + ", " + (wall == null ? "-" : wall[2]) + ")");
        // A group in one tap.
        java.util.ArrayList<Entity> squad = w.spawnGroup(World.RIFLE_SQUAD, false, p[0], p[1] + 60);
        check(squad.size() == World.RIFLE_SQUAD.length, "a rifle squad in one go (" + squad.size() + ")");
    }
}
