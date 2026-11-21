package com.instantaxtion.zombiesandbox;

import java.io.File;
import java.lang.reflect.Field;

/** Police and army: combat, tactics, backup, the army, commanders, medics, volunteers, survivor bases. */
final class ForcesTests extends TestKit {
    static void register() {
        test("combat: rounds and guns, police rounds count, grenades, cover, shoves and loud guns", FAST, 30, ForcesTests.class,
                "loadsAndGuns", "grenadeKills", "holdOwnAndGrenades", "cover", "loudGunsHelpShove");
        test("combat tactics: falling back, standing together, breaking out, covering a reload, fighting indoors", FULL, 20, ForcesTests.class,
                "fallBackAndStand", "smarterFighting", "focusFire", "indoors");
        test("police: at their stations at the start; mounted, traffic, K9 and motorcycle officers; backup calls; nobody idle near a fight or left at the wheel; jobs; training", FAST, 25, ForcesTests.class,
                "policeStart", "mountedAndTraffic", "backupCalls", "idleNearFight", "nobodyLeftAtTheWheel", "jobs", "doctrine", "k9AndBikes");
        test("police backup: outside backup arrives without piling up; every kind spawned; SWAT to a horde; cordons; rounds shared", FAST, 40, ForcesTests.class,
                "outsideBackup", "spawningAndCordons", "rangersAndRescue", "swatToHorde", "magsShared");
        test("army: guards, trucks, IFVs and gun trucks, ammo trucks, mortars, the bulldozer and the armoury", FAST, 35, ForcesTests.class,
                "armoryGuardsTrucks", "gateGuardsReplaced", "ifvAndGunTruck", "ammoTruck", "mortarShells", "mortarRounds", "bulldozer", "armouryNotEmptied", "truckBay", "trucksDontVanish", "quickReactionForce");
        test("commanders, the MRAP and fire support; command posts, fire missions, overwatch, boats and special forces", FULL, 25, ForcesTests.class,
                "commanders", "fireMissions", "boatsPostsSpecialForces");
        test("medics and repair: combat medics, the field hospital, ambulances to care and back, repairs", FULL, 20, ForcesTests.class,
                "combatMedic", "fieldHospital", "ambulancesAndRepair");
        test("volunteers and experience: civilians sign up, are posted and capped; levels and perks", FULL, 25, ForcesTests.class,
                "volunteers", "volunteersCapped", "experience");
        test("survivor bases: founded, walled and manned, joined into settlements; raids and robbed convoys", FULL, 25, ForcesTests.class,
                "survivorBase", "settlement", "raidsAndConvoys");
    }

    static void loadsAndGuns() throws Exception {
        CityConfig c = cfg(CityConfig.MEDIUM, 3);
        c.v[CityConfig.OPT_ZOMBIES] = 4;
        World w = new World(c);
        w.populate(c);
        // (An unprepared city has only half the spare rounds.)
        boolean half = w.readiness == 1;
        for (Entity e : w.entities) {
            // ((27.5) A 15-round magazine and four spares, from three.) ((28.8) And twice the spares since: 135 rounds.)
            if (e.type == Entity.COP && e.role == 0 && e.agency == 0 && e.weapon == Entity.W_STD)
                check(e.ammo + e.reserve == (half ? 15 + 30 * World.AMMO_MORE : 15 + 60 * World.AMMO_MORE), "an officer carries 135 rounds (" + (e.ammo + e.reserve) + ")");
            if (e.type == Entity.SOLDIER && e.role == Entity.ROLE_RIFLE)
                check(e.ammo + e.reserve == (half ? 30 + 90 * World.AMMO_MORE : 30 + 180 * World.AMMO_MORE), "a soldier carries 390 rounds (" + (e.ammo + e.reserve) + ")");
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
        // ((29.1) The army's blasts hurt the living but don't kill them; the player's bomb kills anyone.)
        for (int k = 0; k < 2; k++) {
            float[] q = w.city.findWalkable(w.city.worldW() * (k == 0 ? 0.3f : 0.7f), w.city.worldH() * 0.3f);
            Entity sol = w.spawnSoldier(Entity.ROLE_RIFLE, q[0] + 4, q[1]), civ = w.spawn(Entity.CIVILIAN, q[0] - 4, q[1]), z = w.spawn(Entity.ZOMBIE, q[0], q[1] + 3);
            w.recount();
            if (k == 1) w.playerBomb = true;
            w.explode(q[0], q[1], 46, 210);
            w.playerBomb = false;
            if (k == 0) check(sol.hp > 0 && civ.hp > 0 && civ.hp < civ.maxHp && z.hp <= 0, "a shell among them: the dead go down, the living are hurt but live ("
                    + (int) sol.hp + ", " + (int) civ.hp + ", " + (int) z.hp + ")");
            else check(sol.hp <= 0 && civ.hp <= 0 && z.hp <= 0, "the player's bomb kills everyone it catches");
        }
    }

    static void grenadeKills() throws Exception {
        CityConfig c = cfg(0, CityConfig.SMALL, 82);
        World w = new World(c);
        w.populate(c);
        java.util.Random r = new java.util.Random(4);
        float[] at = null;
        for (int k = 0; k < 4000 && at == null; k++) {
            float[] q = w.city.randomWalkableInTown(r);
            if (w.city.findWalkable(q[0] + 70, q[1]) != null && w.city.los(q[0], q[1], q[0] + 70, q[1])) at = q;
        }
        // (Nobody else about to shoot them first.)
        Quiet alone = empty(w, at[0], at[1], 500);
        Entity soldier = w.spawn(Entity.SOLDIER, at[0], at[1]);
        alone.keep(soldier);
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
        alone.keepAll(pack);
        for (int f = 0; f < 3; f++) step(w, alone);
        WM tg = wm("throwGrenade", Entity.class, float.class, float.class);
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
            step(w, alone);
        }
        int down = 0;
        for (Entity z : pack) if (z.dead || z.hp <= 0) down++;
        check(down >= 2, "the grenade killed some (" + down + ")");
        check(soldier.kills - before == down, "every one of them the thrower's kill (" + (soldier.kills - before) + " of " + down + ")");
    }

    static void holdOwnAndGrenades() throws Exception {
        CityConfig c = cfg(CityConfig.TINY, 275);
        World w = new World(c);
        w.setMaxPopulation(6000);
        w.populate(c);
        w.update(1 / 30f);
        w.outbreak = true;
        w.alert = 2;
        // An open stretch of street, with a pack of the dead down it.
        java.util.Random r = new java.util.Random(5);
        float[] p = null, q = null;
        for (int k = 0; k < 400 && q == null; k++) {
            p = w.city.randomStreetSpot(r);
            double a = r.nextFloat() * 6.283;
            float[] t = w.city.findWalkable(p[0] + (float) Math.cos(a) * 120, p[1] + (float) Math.sin(a) * 120);
            if (t != null && w.city.los(p[0], p[1], t[0], t[1]) && w.city.insideOf(p[0], p[1]) == null && w.city.insideOf(t[0], t[1]) == null) q = t;
        }
        check(q != null, "an open stretch of street");
        // A service round in the body: real damage now, and it checks the one it hits.
        Entity cop = w.spawnCop(0, p[0], p[1]);
        cop.weapon = Entity.W_STD;
        Entity z = w.spawn(Entity.ZOMBIE, q[0], q[1]);
        float hp = z.hp;
        for (int k = 0; k < 40 && z.hp == hp; k++) {
            w.combat.fire(cop, z, (float) Math.hypot(z.x - cop.x, z.y - cop.y), 300);
            if (z.hp <= 0) z.hp = hp;
        }
        check(z.hp < hp && hp - z.hp >= Combat.POLICE_PISTOL * World.GUN_DAMAGE * 0.8f && z.stun > 0, "a police round hurts and checks one of the dead (" + (hp - z.hp) + ")");
        z.dead = z.removed = true;
        cop.dead = cop.removed = true;
        // Two officers against ten: they put down at least half of them (most of the time all of them).
        java.util.List<Entity> cops = new java.util.ArrayList<Entity>(), pack = new java.util.ArrayList<Entity>();
        for (int i = 0; i < 2; i++) cops.add(w.spawnCop(0, p[0] + i * 8 - 4, p[1]));
        for (int i = 0; i < 10; i++) {
            float[] s = w.city.findWalkable(q[0] + (i % 5) * 6 - 12, q[1] + (i / 5) * 6 - 3);
            pack.add(w.spawn(Entity.ZOMBIE, s != null ? s[0] : q[0], s != null ? s[1] : q[1]));
        }
        Quiet qt = empty(w, p[0], p[1], 600, cops, pack);
        final java.util.List<Entity> fpack = pack;
        boolean cleared = stepUntil(w, qt, 75, new Cond() {
            public boolean done() {
                for (Entity e : fpack) if (!e.dead && e.hp > 0) return false;
                return true;
            }
        });
        int kills = 0;
        for (Entity e : pack) if (e.dead || e.hp <= 0) kills++;
        check(cleared || kills >= 5, "two officers put down at least half a pack of ten (" + kills + ")");
        for (Entity e : cops) e.dead = e.removed = true;
        // Special forces, Marines, the Guard and recon throw what they carry (they never did); the mortar team keeps its shells.
        int[] roles = {Entity.ROLE_SF, Entity.ROLE_MARINE, Entity.ROLE_GUARD, Entity.ROLE_RECON};
        for (int role : roles) {
            Entity g = w.spawnSoldier(role, p[0], p[1]);
            int had = g.grenades;
            java.util.List<Entity> nades = new java.util.ArrayList<Entity>();
            for (int i = 0; i < 13; i++) {
                float[] s = w.city.findWalkable(q[0] + (i % 5) * 6 - 12, q[1] + (i / 5) * 6 - 6);
                nades.add(w.spawn(Entity.ZOMBIE, s != null ? s[0] : q[0], s != null ? s[1] : q[1]));
            }
            Quiet gq = empty(w, p[0], p[1], 600, g, nades);
            final Entity fg = g;
            final int fhad = had;
            stepUntil(w, gq, 20, new Cond() {
                public boolean done() {
                    return fg.grenades < fhad;
                }
            });
            check(had > 0 && g.grenades < had, Entity.ROLE_NAMES[role] + " throws a grenade at a pack (" + had + " -> " + g.grenades + ")");
            g.dead = g.removed = true;
            for (Entity e : nades) e.dead = e.removed = true;
        }
    }

    static void cover() throws Exception {
        CityConfig c = cfg(CityConfig.LARGE, 26);
        World w = new World(c);
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
        WM fire = wm("fire", Entity.class, Entity.class, float.class, float.class);
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
        WM cs = wm("coverStep", Entity.class, float.class);
        cs.setAccessible(true);
        hid.lastAimT = -99;
        for (int k = 0; k < 90; k++) cs.invoke(w, hid, 1 / 30f);
        check(hid.suppress < 0.4f, "and it wears off");
        // The city's helicopters can be shot down, but take more than one round.
        CityConfig ac = cfg(CityConfig.LARGE, 28);
        ac.v[CityConfig.OPT_ZOMBIES] = 1;
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

    static void loudGunsHelpShove() throws Exception {
        CityConfig c = cfg(0, CityConfig.LARGE, 3);
        World w = new World(c);
        w.populate(c);
        for (int f = 0; f < 30; f++) w.update(1 / 30f);
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
        // (Two officers on their own on a street, 120 apart: nobody else about to shoot the dead first.)
        w.clearZombies();
        Entity shooter = null, helper = null;
        java.util.Random r = new java.util.Random(5);
        for (int k = 0; k < 400 && helper == null; k++) {
            float[] s = w.city.randomStreetSpot(r);
            float[] h = s == null ? null : w.city.findWalkable(s[0] + 120 * (float) Math.cos(k), s[1] + 120 * (float) Math.sin(k));
            if (h == null || Math.abs(Math.hypot(h[0] - s[0], h[1] - s[1]) - 120) > 20) continue;
            shooter = w.spawnCop(0, s[0], s[1]);
            helper = w.spawnCop(0, h[0], h[1]);
        }
        check(shooter != null && helper != null, "two officers");
        shooter.role = helper.role = Entity.ROLE_RIFLE;
        shooter.task = helper.task = Dispatch.T_NONE;
        Quiet calm = quiet(w, shooter.x, shooter.y, 400, shooter, helper);
        step(w, calm);
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
        step(w, calm);
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
    }

    static void fallBackAndStand() throws Exception {
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
        // Badly hurt with one in sight: ((27.5) out of it - to care (FleetMedevac). With the dead right there, they call for
        // a ride out and fight on where they are till it comes, rather than turning their back to pull away on foot.)
        // (On their own, so nobody else shoots it first.)
        w.clearZombies();
        for (int k = 1; k < line.size(); k++) line.get(k).dead = line.get(k).removed = true;
        Entity hurt = line.get(0);
        hurt.hp = hurt.maxHp * 0.2f;
        hurt.task = Dispatch.T_NONE;
        w.spawn(Entity.ZOMBIE, hurt.x + 60, hurt.y);
        boolean toCare = false;
        for (int f = 0; f < 45 && !toCare; f++) {
            w.update(1 / 30f);
            if (hurt.task == Dispatch.T_HEAL && hurt.waitPickup) toCare = true;
        }
        check(hurt.dead || toCare, "a badly hurt officer calls for a ride out to care (" + hurt.task + ", " + hurt.waitPickup + ")");
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
    }

    static void smarterFighting() throws Exception {
        CityConfig c = cfg(CityConfig.SMALL, 95);
        c.v[CityConfig.OPT_CIVILIANS] = 0;
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
        cop.ammo = 2;
        cop.reload = 0;
        cop.x = p[0];
        cop.y = p[1];
        Entity far = w.spawn(Entity.ZOMBIE, p[0] + 130, p[1]);
        far.noiseTimer = 0;
        for (int f = 0; f < 10 && cop.reload <= 0 && cop.ammo == 2; f++) {
            far.x = p[0] + 130;
            far.y = p[1];
            w.update(1 / 30f);
            w.evCount = 0;
        }
        check(cop.reload > 0 || cop.ammo > 2, "a gun nearly empty is reloaded in a lull, not with the dead on them");
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
        int shots = w.shotsFired, moving = 0;
        for (int f = 0; f < 30 * 2; f++) {
            near.x = cop.x - 50;
            near.y = cop.y + 20;
            boolean ordered = cop.task == Dispatch.T_MOVE;
            int before = w.shotsFired;
            w.update(1 / 30f);
            w.evCount = 0;
            // (Shots taken while still under the order: the dead kept on them, they may later drop it to fight.)
            if (ordered && w.shotsFired > before) moving++;
        }
        check(moving > 0, "on the move under orders, they shoot one of the dead close by (" + (w.shotsFired - shots) + " shots, " + moving + " under orders, task " + cop.task + " dead " + cop.dead + " ammo " + cop.ammo
                + " reload " + cop.reload + " d " + (int) World.hypot(near.x - cop.x, near.y - cop.y) + " los " + w.city.los(cop.x, cop.y, near.x, near.y) + " goal " + cop.goal + " ride " + (cop.ride != null)
                + " pick " + (pickT(w, cop, 160) == near) + " fear " + cop.fear + " post " + (int) cop.postX + "," + (int) cop.postY + " at " + (int) cop.x + "," + (int) cop.y + " inside " + (w.city.insideOf(cop.x, cop.y) != null) + " postInside " + (w.city.insideOf(cop.postX, cop.postY) != null) + ")");
        near.hp = 0;
        w.update(1 / 30f);
        // One of the dead on someone, with them in the way: it's still the target, and they work round for a shot.
        Entity gun = w.spawnCop(0, p[0], p[1]);
        // (Holding fire while it's set up: with 27.5's police rounds a close shot can drop even this one.)
        gun.ammo = 0;
        gun.reserve = 0;
        Entity victim = w.spawn(Entity.CIVILIAN, p[0] + 40, p[1]);
        Entity biter = w.spawn(Entity.ZOMBIE, p[0] + 48, p[1]);
        biter.hp = biter.maxHp = 1e5f;
        // (Nobody else about, living or dead: just the three of them.)
        Quiet bite = empty(w, p[0], p[1], 300, gun, victim, biter);
        // (Nor a grenade from the fight before still on its way down.)
        w.grenades.clear();
        for (int f = 0; f < 5; f++) {
            bite.sweep(w);
            victim.x = p[0] + 40;
            victim.y = p[1];
            biter.x = p[0] + 48;
            biter.y = p[1];
            biter.biteCd = 0.5f;
            w.update(1 / 30f);
            w.evCount = 0;
        }
        WM pick = wm("pickTarget", Entity.class, float.class);
        pick.setAccessible(true);
        gun.ammo = gun.magSize;
        gun.fear = 5;
        gun.x = p[0];
        gun.y = p[1];
        gun.angle = 0;
        check(pick.invoke(w, gun, 160f) == biter, "the one biting someone is still the target with them in the line of fire");
        float gx0 = gun.x, gy0 = gun.y;
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
            step(w, bite);
            shot = w.shotsFired > before;
        }
        check(Math.hypot(gun.x - gx0, gun.y - gy0) > 2 || shot, "they step round for an angle (" + (int) Math.hypot(gun.x - gx0, gun.y - gy0) + " moved, gun " + gun.ammo + "/" + gun.reserve + ", task " + gun.task + ", dead " + gun.dead + ", aiming " + gun.aiming + ")");
        check(victim.hp > 0, "and nobody shoots through the one being bitten");
    }

    static void focusFire() throws Exception {
        CityConfig c = cfg(CityConfig.TINY, 274);
        World w = new World(c);
        w.populate(c);
        w.update(1 / 30f);
        w.outbreak = true;
        w.alert = 2;
        // A straight stretch of street to line them up on.
        java.util.Random r = new java.util.Random(7);
        float[] s0 = null;
        float ux = 0, uy = 0;
        for (int k = 0; k < 400 && s0 == null; k++) {
            float[] q = w.city.randomStreetSpot(r);
            if (q == null) continue;
            for (int dir = 0; dir < 2 && s0 == null; dir++) {
                float ax = dir == 0 ? 1 : 0, ay = dir == 1 ? 1 : 0;
                boolean clear = true;
                for (int u = -140; u <= 140 && clear; u += 6)
                    for (int v = -14; v <= 28 && clear; v += 7)
                        if (w.city.solidAt(q[0] + ax * u + ay * v, q[1] + ay * u + ax * v)) clear = false;
                if (clear && w.city.los(q[0] - ax * 140, q[1] - ay * 140, q[0] + ax * 140, q[1] + ay * 140)) {
                    s0 = q;
                    ux = ax;
                    uy = ay;
                }
            }
        }
        check(s0 != null, "a straight stretch of street");
        // Covering a reload: B with one of the dead 50 off on its own side, and one 80 off that's 30 from A, who's
        // reloading. B takes the one going for A. (A and that one a step to the side: not in B's line of fire.)
        Entity b = w.spawnSoldier(Entity.ROLE_RIFLE, s0[0], s0[1]);
        Entity a = w.spawnSoldier(Entity.ROLE_RIFLE, s0[0] + ux * 50 + uy * 20, s0[1] + uy * 50 + ux * 20);
        Entity z1 = w.spawn(Entity.ZOMBIE, s0[0] + ux * 80 + uy * 20, s0[1] + uy * 80 + ux * 20);
        Entity z2 = w.spawn(Entity.ZOMBIE, s0[0] - ux * 50, s0[1] - uy * 50);
        Quiet q = empty(w, s0[0], s0[1], 400, a, b, z1, z2);
        for (Entity z : new Entity[]{z1, z2}) untouchable(z);
        w.update(1 / 30f);
        b.fear = 20;
        a.reload = 2;
        check(w.combat.mateReloading(b, Combat.MATE_R), "B sees A reloading");
        Entity pick = w.combat.pickTarget(b, 230);
        check(pick == z1, "with A reloading, B covers A: shoots the one going for A, not the nearer one (" + (pick == z1 ? "z1" : pick == z2 ? "z2" : "none") + ")");
        // Not both reloading at once: a lull with A reloading doesn't start B's.
        check(!w.combat.mateCovering(b, 60) || w.combat.mateReloading(b, Combat.MATE_R), "a reload is covered only by someone not reloading");
        for (Entity o : new Entity[]{a, b, z1, z2}) o.dead = o.removed = true;
        // Falling back together: three officers running low on rounds, a pack of sixteen closing. They all go, to
        // one place.
        java.util.List<Entity> cops = new java.util.ArrayList<Entity>();
        for (int i = 0; i < 3; i++) {
            Entity o = w.spawnCop(0, s0[0] + uy * (i - 1) * 10, s0[1] + ux * (i - 1) * 10);
            o.task = Dispatch.T_NONE;
            o.ammo = 5;
            o.reserve = 6;
            cops.add(o);
        }
        java.util.List<Entity> pack = new java.util.ArrayList<Entity>();
        for (int i = 0; i < 16; i++) {
            float[] p = w.city.findWalkable(s0[0] + ux * (32 + (i % 4) * 7) + uy * ((i / 4) * 7 - 10), s0[1] + uy * (32 + (i % 4) * 7) + ux * ((i / 4) * 7 - 10));
            if (p != null) pack.add(w.spawn(Entity.ZOMBIE, p[0], p[1]));
        }
        q = empty(w, s0[0], s0[1], 600, cops, pack);
        final java.util.List<Entity> cs = cops;
        final World fw = w;
        boolean together = stepUntil(w, q, 6, new Cond() {
            public boolean done() {
                Entity c0 = cs.get(0);
                if (c0.rallyT <= fw.time) return false;
                for (Entity o : cs) if (o.dead || o.rallyT <= fw.time || o.rallyX != c0.rallyX || o.rallyY != c0.rallyY) return false;
                return true;
            }
        });
        check(together, "all three fall back, and to the same place (" + w.armedAI.groupFallbacks + ")");
        float[] zc = w.spatial.zombieCentre(s0[0] + ux * 45, s0[1] + uy * 45, 200);
        float before = 0;
        for (Entity o : cops) before += (float) Math.hypot(o.x - zc[0], o.y - zc[1]);
        for (int f = 0; f < 30 * 3; f++) step(w, q);
        float after = 0, spread = 0;
        int up = 0;
        for (Entity o : cops) {
            if (o.dead || o.infected) continue;
            up++;
            after += (float) Math.hypot(o.x - zc[0], o.y - zc[1]);
            for (Entity p : cops) if (!p.dead && !p.infected) spread = Math.max(spread, (float) Math.hypot(o.x - p.x, o.y - p.y));
        }
        check(up >= 2 && after / up > before / cops.size() + 40, "they open the gap from the pack (" + (int) (before / cops.size()) + " -> " + (int) (after / Math.max(1, up)) + ", " + up + " up)");
        check(spread < 160, "and keep together while they go (" + (int) spread + ")");
    }

    static void indoors() throws Exception {
        CityConfig c = cfg(CityConfig.TINY, 91);
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
            // (Nobody else in the room or round it: nobody in the line of fire, nothing else to shoot at.)
            Quiet room0 = empty(w, (p[0] + q[0]) / 2, (p[1] + q[1]) / 2, 160, gun, z);
            int shots = w.shotsFired;
            boolean aimed = false, hit = false;
            for (int f = 0; f < 150; f++) {
                // (A shot to the head ends it at once, however tough it is: another of the dead in its place, so
                // it's how they fight in a room that's measured, not whether the first shot was lucky.)
                if (z.dead) {
                    hit = true;
                    z = w.spawn(Entity.ZOMBIE, q[0], q[1]);
                    z.hp = z.maxHp = 5000;
                    room0.keep(z);
                }
                hold(z, q[0], q[1]);
                hold(gun, p[0], p[1]);
                gun.hp = gun.maxHp;
                step(w, room0);
                aimed |= gun.aiming;
                hit |= z.dead || z.hp < z.maxHp;
            }
            check(aimed && w.shotsFired - shots >= 4 && hit, (who == 0 ? "a police officer" : "a soldier") + " indoors opens fire on one of the dead in the room ("
                    + (w.shotsFired - shots) + " shots, aimed " + aimed + ", task " + gun.task + ", ammo " + gun.ammo + "/" + gun.reserve + ", mag " + gun.magSize + ", role " + gun.role + ", melee " + gun.melee + ", dead " + gun.dead + ", rig " + (gun.rig != null) + ")");
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

    static void policeStart() throws Exception {
        CityConfig c = cfg(CityConfig.MASSIVE, 280);
        World w = new World(c);
        w.populate(c);
        w.update(1 / 30f);
        // At the start: aboard a car, at a station, or on a post (traffic officers) - nobody loose on foot.
        int aboard = 0, loose = 0, total = policeNow(w);
        for (Fleet.Vehicle v : w.fleet.vehicles) for (Entity e : v.crew) if (e.type == Entity.COP) aboard++;
        for (Entity e : w.entities)
            if (!e.dead && e.type == Entity.COP && e.agency != 3 && e.task == Dispatch.T_NONE && stationDist(w, e) > 300) loose++;
        check(aboard > total / 2 && loose == 0, "in their cars and at their stations (" + aboard + " of " + total + " aboard, " + loose + " loose)");
        // Most of those on foot at the city's precincts, the special units among them; a country town's station keeps a few.
        int sats = 0, atSats = 0, special = 0;
        for (City.Facility f : w.city.facilities) {
            if (f.kind != City.FACILITY_POLICE || !f.satellite) continue;
            sats++;
            for (Entity e : w.entities) {
                if (e.dead || e.type != Entity.COP || World.hypot(e.x - f.x, e.y - f.y) > f.r) continue;
                atSats++;
                if (e.role == Entity.ROLE_SWAT || e.role == Entity.ROLE_RIOT) special++;
            }
        }
        check(sats > 0 && atSats <= sats * 6 && special == 0, "a few at each country town's station, no SWAT or riot squads ("
                + atSats + " at " + sats + ", " + special + " special)");
    }

    static void mountedAndTraffic() throws Exception {
        CityConfig c = cfg(0, CityConfig.MEDIUM, 80);
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
        // (Nobody else close by to shoot it first; and it can't be shot down before the next look round.)
        Entity z = w.spawn(Entity.ZOMBIE, p[0], p[1]);
        Quiet ride = quiet(w, p[0], p[1], 400, m, z);
        boolean marked = false;
        for (int f = 0; f < 30 * 2 && !marked; f++) {
            untouchable(z);
            step(w, ride);
            marked = z.spottedUntil > w.time;
        }
        check(marked, "the dead nearby marked for the others (" + m.task + " " + m.dead + " " + (int) Math.hypot(z.x - m.x, z.y - m.y) + " " + z.dead + ")");
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

    static void backupCalls() throws Exception {
        CityConfig c = cfg(CityConfig.SMALL, 73);
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
                    if (inc.backupNeed > 0 && Math.hypot(inc.x - cop.x, inc.y - cop.y) < 250) called = true;
            }
            check(called, (round == 0 ? "a lone officer against four" : "an officer out of rounds") + " calls for backup");
            cop.dead = cop.removed = true;
        }
    }

    static void idleNearFight() throws Exception {
        CityConfig c = cfg(CityConfig.TINY, 2751);
        World w = new World(c);
        w.populate(c);
        w.update(1 / 30f);
        // A long straight street: a call at one end, the dead a little way past it, an officer on the ring on
        // the near side - out of pistol range of them. They step in until they have them in their sights.
        java.util.Random r = new java.util.Random(11);
        float[] s0 = null;
        float ux = 0, uy = 0;
        for (int k = 0; k < 600 && s0 == null; k++) {
            float[] q = w.city.randomStreetSpot(r);
            if (q == null) continue;
            for (int dir = 0; dir < 2 && s0 == null; dir++) {
                float ax = dir == 0 ? 1 : 0, ay = dir == 1 ? 1 : 0;
                boolean clear = true;
                for (int u = -150; u <= 110 && clear; u += 6)
                    for (int v = -8; v <= 8 && clear; v += 8)
                        if (w.city.solidAt(q[0] + ax * u + ay * v, q[1] + ay * u + ax * v)) clear = false;
                if (clear) {
                    s0 = q;
                    ux = ax;
                    uy = ay;
                }
            }
        }
        check(s0 != null, "a long straight street");
        java.util.List<Entity> pack = new java.util.ArrayList<Entity>();
        for (int i = 0; i < 6; i++) {
            Entity z = w.spawn(Entity.ZOMBIE, s0[0] + ux * (70 + i * 7) + uy * ((i % 2) * 6 - 3), s0[1] + uy * (70 + i * 7) + ux * ((i % 2) * 6 - 3));
            untouchable(z);
            pack.add(z);
        }
        final Entity cop = w.spawnCop(0, s0[0] - ux * 125, s0[1] - uy * 125);
        Quiet q = empty(w, s0[0], s0[1], 500, cop, pack);
        w.update(1 / 30f);
        Dispatch.Incident inc = w.dispatch.sighting(s0[0], s0[1], null);
        check(inc != null, "a call");
        cop.task = Dispatch.T_RESPOND;
        cop.incident = inc;
        cop.onScene = true;
        cop.ringOf = inc;
        cop.ringA = (float) Math.atan2(-uy, -ux);
        check(Math.hypot(pack.get(0).x - cop.x, pack.get(0).y - cop.y) > 190, "the dead out of pistol range of their place on the ring");
        boolean fired = stepUntil(w, q, 15, new Cond() {
            public boolean done() {
                cop.ammo = Math.max(cop.ammo, 5);
                return cop.aiming;
            }
        });
        check(fired, "the officer on the ring steps in until they have them in their sights, not stands there (" + (int) Math.hypot(pack.get(0).x - cop.x, pack.get(0).y - cop.y) + " off)");
    }

    static void jobs() throws Exception {
        CityConfig c = cfg(CityConfig.TINY, 274);
        World w = new World(c);
        w.populate(c);
        w.update(1 / 30f);
        Jobs j = w.ops.jobs;
        Object crew = new Object(), other = new Object();
        float[] p = w.city.randomStreetSpot(new java.util.Random(6));
        float[] pa = w.city.findWalkable(p[0] + 50, p[1]), pb = w.city.findWalkable(p[0] + 150, p[1]);
        final Entity a = w.spawnCop(0, pa[0], pa[1]), b = w.spawnCop(0, pb[0], pb[1]);
        // The rules: a job keeps a unit from anything that matters less, not more; only its holder lets it go.
        j.claim(a, crew, Jobs.P_CREW, "test crew");
        check(!j.isFree(a, Jobs.P_POST) && j.isFree(a, Jobs.P_MISSION) && j.isFree(b, Jobs.P_POST), "a job keeps a unit from lesser ones, not greater");
        check("On: test crew".equals(Jobs.describe(a)) && Jobs.describe(b) == null, "and its card says whose it is");
        check(!j.release(a, other) && a.owner != null, "only its holder lets it go");
        // A call right by them goes to the free officer, not the nearer one on a job.
        empty(w, p[0], p[1], 500, a, b);
        java.lang.reflect.Method nf = Dispatch.class.getDeclaredMethod("nearestFree", int.class, float.class, float.class, int.class, float.class);
        nf.setAccessible(true);
        java.util.List<?> picked = (java.util.List<?>) nf.invoke(w.dispatch, Entity.COP, p[0], p[1], 1, 400f);
        check(picked.size() == 1 && picked.get(0) == b, "the call goes to the free officer, not the nearer one on a job");
        Jobs.enforce = false;
        picked = (java.util.List<?>) nf.invoke(w.dispatch, Entity.COP, p[0], p[1], 1, 400f);
        Jobs.enforce = true;
        check(picked.size() == 1 && picked.get(0) == a, "(without jobs, the nearer one would have been taken)");
        final Dispatch.Incident inc = w.dispatch.sighting(p[0], p[1], null);
        // Taken off it by someone else all the same: the next look round counts it, and the job's gone.
        int yanks = j.yanks;
        a.task = Dispatch.T_RESPOND;
        a.incident = inc;
        j.audit();
        check(j.yanks == yanks + 1 && a.owner == null, "taken by another all the same: counted, and let go");
        // The holder let it go, and it's anyone's.
        j.claim(b, crew, Jobs.P_CREW, "test crew");
        check(j.release(b, crew) && b.owner == null && j.isFree(b, Jobs.P_CREW), "let go by its holder: anyone's");
        // Out of a vehicle the job was getting into: it's over.
        Fleet.Vehicle car = w.fleet.vehicles.get(0);
        j.claim(b, crew, Jobs.P_CREW, "test crew");
        b.rig = car;
        j.claim(b, car, Jobs.P_CREW, "crew of a police car");
        w.fleet.board(car, b);
        w.fleet.crewOut(car, true);
        check(b.owner == null && !b.removed, "out of the vehicle: no longer its crew");
    }

    static void doctrine() throws Exception {
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
    }

    static void k9AndBikes() throws Exception {
        CityConfig c = cfg(CityConfig.SMALL, 220);
        World w = new World(c);
        w.populate(c);
        w.update(1 / 30f);
        float[] p = w.city.findWalkable(w.city.worldW() / 2, w.city.worldH() / 2);
        // K9: the dog finds a bite.
        Entity handler = w.spawnCop(Entity.ROLE_K9, p[0], p[1]);
        Entity dog = null;
        for (Entity e : w.entities) if (e.type == Entity.DOG && e.leader == handler) dog = e;
        check(dog != null, "a K9 handler comes with a police dog");
        w.spawn(Entity.ZOMBIE, p[0] + 30, p[1]);
        Entity bitten = w.spawn(Entity.CIVILIAN, p[0] - 20, p[1]);
        bitten.infected = true;
        w.recount();
        for (int f = 0; f < 30 * 3; f++) { w.update(1 / 30f); w.evCount = 0; }
        check(bitten.sniffed, "and finds the bite");
        // A motorcycle officer, out on patrol.
        Entity rider = w.spawnCop(-4, p[0], p[1]);
        check(rider != null && rider.rig != null && rider.rig.kind == Fleet.K_BIKE, "the Motorcycle cop rides a police motorcycle");
    }

    /**
     * (28.3) At a call, everyone gets out: a patrol car whose officers are out on foot round it has nobody sitting in
     * it. A driver used to be made up for each one the moment its crew got out, and sat there out of the fight.
     */
    static void nobodyLeftAtTheWheel() throws Exception {
        CityConfig c = cfg(CityConfig.SMALL, 91);
        World w = new World(c);
        w.populate(c);
        java.util.Random r = new java.util.Random(3);
        for (int pack = 0; pack < 5; pack++) {
            float[] p = w.city.randomWalkableInTown(r);
            for (int k = 0; k < 6; k++) {
                float[] q = w.city.findWalkable(p[0] + (k % 3) * 8, p[1] + (k / 3) * 8);
                if (q != null) w.spawn(Entity.ZOMBIE, q[0], q[1]);
            }
        }
        w.recount();
        int seen = 0, sitting = 0;
        String eg = "";
        for (int f = 0; f < 30 * 90; f++) {
            w.update(1 / 30f);
            w.evCount = 0;
            if (f % 15 != 0) continue;
            for (Fleet.Vehicle v : w.fleet.vehicles) {
                // (Not the bulldozer: its driver is at work, pushing the wreck.)
                if (v.removedFromFleet || v.kind == Fleet.K_DOZER || (v.state != Fleet.SCENE && v.state != Fleet.RECALL) || !v.crew.isEmpty() || w.fleet.crewOnFoot(v) == 0) continue;
                seen++;
                if (v.driver != null) {
                    sitting++;
                    eg = w.fleet.describe(v);
                }
            }
        }
        check(seen > 0, "police cars at a call with their officers out (" + seen + ")");
        check(sitting == 0, "nobody left sitting in one (" + sitting + " of " + seen + (eg.isEmpty() ? "" : ", e.g. " + eg) + ")");
    }

    /**
     * (28.5) An army truck on its way back to base, held up a long way short of it, isn't taken off the map and put in
     * its bay: it plans its way home again, and in the end parks where it is. Patrol trucks too. They used to vanish
     * from the road and turn up at the base.
     */
    static void trucksDontVanish() throws Exception {
        CityConfig c = cfg(CityConfig.SMALL, 280);
        World w = new World(c);
        w.populate(c);
        City.Facility base = w.city.mainBase();
        check(base != null, "a base");
        float[] far = null;
        java.util.Random r = new java.util.Random(9);
        for (int k = 0; k < 400 && far == null; k++) {
            float[] s = w.city.randomStreetSpot(r);
            if (s != null && Math.hypot(s[0] - base.x, s[1] - base.y) > 600) far = s;
        }
        check(far != null, "a street a long way from the base");
        for (boolean loop : new boolean[]{false, true}) {
            Fleet.Vehicle v = w.fleet.make(Fleet.TRUCK);
            v.x = far[0];
            v.y = far[1];
            v.homeX = base.gateX > 0 ? base.gateX : base.x;
            v.homeY = base.gateY > 0 ? base.gateY : base.y;
            v.passengerType = Entity.SOLDIER;
            v.state = Fleet.RETURN;
            v.loop = loop;
            if (loop) w.fleet.setKind(v, Fleet.K_HUMVEE);
            w.fleet.vehicles.add(v);
            boolean gone = false;
            // Held up over and over, past all its tries at another way home (as the game does, until it's parked).
            for (int k = 0; k < 12 && !gone && !v.parked; k++) {
                v.rerouteCd = 0;
                gone = w.fleet.service.arrivedStep(v, 1 / 30f);
            }
            String what = loop ? "a patrol Humvee" : "a truck";
            check(!gone && !v.removedFromFleet, what + " held up far from base stays on the map");
            check(Math.hypot(v.x - far[0], v.y - far[1]) < 1, what + " hasn't jumped anywhere");
            check(v.parked || v.state == Fleet.RETURN, what + " parks where it is, or keeps going (state " + v.state + ")");
        }
        // The dot each carries zoomed out: the police's, the army's, none for anyone else's.
        Fleet.Vehicle cop = w.fleet.make(Fleet.CRUISER), van = w.fleet.make(Fleet.TRUCK), truck = w.fleet.make(Fleet.TRUCK), tank = w.fleet.make(Fleet.TANK), car = w.fleet.make(Fleet.CAR);
        w.fleet.setKind(van, Fleet.K_RIOT_VAN);
        truck.passengerType = Entity.SOLDIER;
        check(WorldRenderer.unitSide(cop) == 1 && WorldRenderer.unitSide(van) == 1 && WorldRenderer.unitSide(truck) == 2 && WorldRenderer.unitSide(tank) == 2
                && WorldRenderer.unitSide(car) < 0, "police cars carry the police's dot, the army's vehicles the army's");
    }

    /**
     * (28.7) The quick reaction force: a small base near the middle of town (map version 42), twenty soldiers standing
     * by in its yard and four fast vans; a call and a van goes, five aboard, the rest staying ready.
     */
    static void quickReactionForce() throws Exception {
        CityConfig old = cfg(CityConfig.METROPOLIS, CityConfig.SMALL, 280);
        City oc = new City(old, 0.05f);
        boolean had = false;
        for (City.Facility f : oc.facilities) had |= f.qrf;
        check(!had, "no quick reaction force on a city built before map version 42");
        CityConfig c = cfg(CityConfig.METROPOLIS, CityConfig.SMALL, 280);
        c.civic = 42;
        World w = new World(c);
        w.populate(c);
        City.Facility q = null;
        for (City.Facility f : w.city.facilities) if (f.qrf) q = f;
        check(q != null && q.kind == City.FACILITY_BASE && q.baseType == City.BT_RAPID && q.name.endsWith("(Rapid Response)"), "a quick reaction force base");
        float mx = (w.city.townX0 + w.city.townX1) / 2f * City.T, my = (w.city.townY0 + w.city.townY1) / 2f * City.T;
        check(Math.hypot(q.x - mx, q.y - my) < w.city.worldW() * 0.25f, "near the middle of town (" + (int) Math.hypot(q.x - mx, q.y - my) + ")");
        check(q.r < 110, "a small one (" + (int) q.r + ")");
        int ready = 0, vans = 0;
        for (Entity e : w.entities)
            if (!e.dead && e.type == Entity.SOLDIER && e.task == Dispatch.T_POST && Math.hypot(e.postX - q.x, e.postY - q.y) < q.r + 100) ready++;
        for (Fleet.Vehicle v : w.fleet.vehicles) if (v.kind == Fleet.K_RAPID_VAN && v.parked && v.standby) vans++;
        check(ready == Population.QRF_SOLDIERS, "twenty standing by (" + ready + ")");
        check(vans == FleetArmy.RAPID_VANS, "four vans in the yard (" + vans + ")");
        // A call across town: a van goes, five aboard.
        java.util.Random r = new java.util.Random(3);
        float[] call = null;
        for (int k = 0; k < 400 && call == null; k++) {
            float[] s2 = w.city.randomStreetSpot(r);
            if (s2 != null && Math.hypot(s2[0] - q.x, s2[1] - q.y) > 600 && Math.hypot(s2[0] - q.x, s2[1] - q.y) < 1600) call = s2;
        }
        check(call != null, "a street across town");
        w.outbreak = true;
        java.util.List<Entity> dead = new java.util.ArrayList<Entity>();
        for (int i = 0; i < 4; i++) {
            float[] zp = w.city.findWalkable(call[0] + 12 * (float) Math.cos(i * 1.6f), call[1] + 12 * (float) Math.sin(i * 1.6f));
            Entity z = zp == null ? null : w.spawn(Entity.ZOMBIE, zp[0], zp[1]);
            if (z != null) dead.add(z);
        }
        check(!dead.isEmpty(), "the dead at the call");
        java.lang.reflect.Method ni = Dispatch.class.getDeclaredMethod("newIncident", float.class, float.class, int.class);
        ni.setAccessible(true);
        ni.invoke(w.dispatch, call[0], call[1], 4);
        Fleet.Vehicle van = null;
        for (int f = 0; f < 30 * 60 && van == null; f++) {
            // (The dead at the call stay put and can't be put down - one shot down is back at once: the call stays open.)
            for (int i = 0; i < dead.size(); i++) {
                Entity z = dead.get(i);
                if (z.dead || z.removed) {
                    z = w.spawn(Entity.ZOMBIE, call[0] + 12 * (float) Math.cos(i * 1.6f), call[1] + 12 * (float) Math.sin(i * 1.6f));
                    if (z == null) continue;
                    dead.set(i, z);
                }
                z.speed = z.runSpeed = 0;
                z.hp = z.maxHp = 100000;
            }
            w.update(1 / 30f);
            w.evCount = 0;
            for (Fleet.Vehicle v : w.fleet.vehicles)
                if (v.kind == Fleet.K_RAPID_VAN && v.state == Fleet.DRIVE && v.crew.size() == Fleet.RAPID_SEATS) van = v;
        }
        check(van != null, "a van out to the call with five aboard");
        int still = 0, out = 0;
        for (Entity e : w.entities)
            if (!e.dead && e.type == Entity.SOLDIER && e.task == Dispatch.T_POST && Math.hypot(e.postX - q.x, e.postY - q.y) < q.r + 100) still++;
        for (Fleet.Vehicle v : w.fleet.vehicles) if (v.kind == Fleet.K_RAPID_VAN && !v.standby) out++;
        check(still == Population.QRF_SOLDIERS - out * Fleet.RAPID_SEATS, "the rest still ready (" + still + ", " + out + " vans out)");
        check(FleetService.topSpeed(Fleet.TRUCK, Fleet.K_RAPID_VAN, true) > FleetService.topSpeed(Fleet.TRUCK, Fleet.K_HUMVEE, true)
                && FleetService.topSpeed(Fleet.TRUCK, Fleet.K_RAPID_VAN, true) > FleetService.topSpeed(Fleet.CRUISER, 0, true), "the vans are the quickest on the road");
        // Saved and loaded: still four vans, still the quick reaction force's.
        java.io.File file = java.io.File.createTempFile("zcs", ".dat");
        SaveGame.save(w, file);
        World l = SaveGame.load(file);
        file.delete();
        int lv = 0;
        City.Facility lq = null;
        for (City.Facility f : l.city.facilities) if (f.qrf) lq = f;
        for (Fleet.Vehicle v : l.fleet.vehicles) if (v.kind == Fleet.K_RAPID_VAN) lv++;
        check(lq != null && lv == FleetArmy.RAPID_VANS, "a save keeps the base and its four vans (" + lv + ")");
    }

    static void outsideBackup() throws Exception {
        CityConfig c = cfg(CityConfig.SMALL, 280);
        World w = new World(c);
        w.populate(c);
        w.update(1 / 30f);
        // A call in the middle of town, with a few of the dead there that can't be put down (it stays open).
        java.util.Random r = new java.util.Random(5);
        float[] call = null;
        float best = Float.MAX_VALUE;
        for (int k = 0; k < 300; k++) {
            float[] s = w.city.randomStreetSpot(r);
            if (s == null) continue;
            float d = (float) Math.hypot(s[0] - w.city.worldW() / 2, s[1] - w.city.worldH() / 2);
            if (d < best) {
                best = d;
                call = s;
            }
        }
        w.outbreak = true;
        java.util.List<Entity> dead = new java.util.ArrayList<Entity>();
        for (int i = 0; i < 4; i++) {
            Entity z = w.spawn(Entity.ZOMBIE, call[0] + 20 * (float) Math.cos(i * 1.6f), call[1] + 20 * (float) Math.sin(i * 1.6f));
            if (z != null) dead.add(z);
        }
        Dispatch dp = w.dispatch;
        java.lang.reflect.Method ni = Dispatch.class.getDeclaredMethod("newIncident", float.class, float.class, int.class);
        ni.setAccessible(true);
        Dispatch.Incident inc = (Dispatch.Incident) ni.invoke(dp, call[0], call[1], 4);
        java.lang.reflect.Method pb = Dispatch.class.getDeclaredMethod("policeBackup", Dispatch.Incident.class);
        pb.setAccessible(true);
        @SuppressWarnings("unchecked") java.util.List<Integer> bag = (java.util.List<Integer>) field(dp, "waveBag");
        Field cd = Dispatch.class.getDeclaredField("policeCd");
        cd.setAccessible(true);
        // Two waves of another town's police, the city losing badly: twenty officers each.
        dp.policeReserve = 100;
        int sent = 0;
        for (int k = 0; k < 2; k++) {
            bag.clear();
            bag.add(0);
            dp.pressure = 3;
            cd.set(dp, 0f);
            int before = dp.policeReserve;
            check((Boolean) pb.invoke(dp, inc), "wave " + (k + 1) + " called");
            sent += before - dp.policeReserve;
        }
        dp.policeReserve = 0;
        java.util.Set<Fleet.Vehicle> cars = new java.util.HashSet<Fleet.Vehicle>();
        java.util.List<float[]> drops = new java.util.ArrayList<float[]>();
        java.util.Map<Fleet.Vehicle, Integer> aboardWas = new java.util.HashMap<Fleet.Vehicle, Integer>();
        int outNear = 0, outFar = 0;
        float overlapT = 0;
        int crowded = 0;
        for (int f = 0; f < 30 * 150; f++) {
            for (int i = 0; i < dead.size(); i++) {
                Entity z = dead.get(i);
                if (z.dead || z.removed) {
                    z = w.spawn(Entity.ZOMBIE, call[0] + 20 * (float) Math.cos(i * 1.6f), call[1] + 20 * (float) Math.sin(i * 1.6f));
                    if (z == null) continue;
                    dead.set(i, z);
                }
                z.speed = z.runSpeed = 0;
                z.hp = z.maxHp = 100000;
            }
            w.update(1 / 30f);
            java.util.List<Fleet.Vehicle> on = new java.util.ArrayList<Fleet.Vehicle>();
            for (Fleet.Vehicle v : w.fleet.vehicles) {
                if (!v.reinforcement || v.type != Fleet.CRUISER || v.removedFromFleet) continue;
                // (Bringing officers in: not one come to take some home.)
                if (v.passengers > 0 && v.state != Fleet.RETURN && cars.add(v)) {
                    check(v.passengers <= Fleet.SEATS, "no more aboard than it seats (" + v.passengers + ")");
                    drops.add(new float[]{v.tx, v.ty});
                }
                on.add(v);
                // Where its officers get out.
                Integer was = aboardWas.put(v, v.passengers);
                if (cars.contains(v) && was != null && was > 0 && v.passengers == 0) {
                    if (Math.hypot(v.x - call[0], v.y - call[1]) < 300) outNear += was;
                    else outFar += was;
                }
            }
            // Nothing driven into anything else: two backup cars' centres never under a car's width apart.
            boolean over = false;
            for (int i = 0; i < on.size(); i++)
                for (int j = i + 1; j < on.size(); j++)
                    if (Math.hypot(on.get(i).x - on.get(j).x, on.get(i).y - on.get(j).y) < 8) over = true;
            if (over) overlapT += 1 / 30f;
            if (f % 30 == 0) {
                int near = 0;
                for (Fleet.Vehicle v : on) if (Math.hypot(v.x - inc.x, v.y - inc.y) < 60) near++;
                crowded = Math.max(crowded, near);
            }
        }
        // Four to a car: forty officers in ten cars, not twenty.
        check(sent == 40 && cars.size() == 10, "40 officers in 10 cars (" + sent + " in " + cars.size() + ")");
        // Each to a drop-off of its own round the call, none in a junction.
        int spots = 0;
        for (int i = 0; i < drops.size(); i++) {
            boolean seen = false;
            for (int j = 0; j < i; j++) if (Math.hypot(drops.get(i)[0] - drops.get(j)[0], drops.get(i)[1] - drops.get(j)[1]) < 20) seen = true;
            if (!seen) spots++;
            check(w.city.junctionIdAt(drops.get(i)[0], drops.get(i)[1]) < 0 || Math.hypot(drops.get(i)[0] - call[0], drops.get(i)[1] - call[1]) < 40,
                    "no drop-off in a junction");
        }
        check(spots >= 6, "the cars spread over several drop-off points (" + spots + " for " + drops.size() + " cars)");
        check(overlapT < 1, "no two backup cars on top of each other (" + overlapT + " s)");
        check(crowded <= 4, "no pile of cars at the call (" + crowded + " within 60)");
        // The officers out of their cars near the call.
        int stopped = 0;
        for (Fleet.Vehicle v : cars)
            if (!v.removedFromFleet && Math.abs(v.speed) < 1 && w.city.junctionIdAt(v.x, v.y) >= 0) stopped++;
        // (Most of them: a car held up in traffic a long way off can let its officers out to go on on foot.)
        check(outNear >= sent * 3 / 4, "the officers got out near the call (" + outNear + " of " + sent + " within 300, " + outFar + " further off)");
        check(stopped == 0, "no empty car left in a junction (" + stopped + ")");
    }

    static void spawningAndCordons() throws Exception {
        CityConfig c = cfg(CityConfig.LARGE, 4);
        World w = new World(c);
        w.populate(c);
        for (int f = 0; f < 30 * 10; f++) w.update(1 / 30f);
        float cx = w.city.worldW() / 2f, cy = w.city.worldH() / 2f;
        // (Open ground near the middle: since 11.1 a big block can sit right on it.)
        for (int r = 0; r < 40 && w.city.findWalkable(cx, cy) == null; r++) cx += City.T * 4;
        // Every kind of officer can be spawned.
        check(w.spawnCop(-1, cx, cy).agency == 1 && w.spawnCop(-2, cx, cy).agency == 2 && w.spawnCop(-3, cx, cy).agency == 3
                && w.spawnCop(Entity.ROLE_SAR, cx, cy).role == Entity.ROLE_SAR, "highway patrol, deputies, rangers and rescuers spawn");
        // Highway patrol only comes into town as backup once it's under attack. (None of the city's own free.)
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
    }

    static void rangersAndRescue() throws Exception {
        // In the wilds: one ranger minds the station; the rescue team is dropped off and the helicopter goes back.
        World tw = wildWorld(9);
        City.Building rs = tw.rangerStation;
        check(rs != null, "a ranger station");
        int samples = 0, minded = 0;
        boolean first = false;
        float nearest = 0;
        for (int f = 0; f < 30 * 40; f++) {
            tw.update(1 / 30f);
            if (f % 30 != 29 || f < 30 * 10) continue;
            samples++;
            float best = Float.MAX_VALUE;
            for (Entity e : tw.entities) if (!e.dead && e.agency == 3) best = Math.min(best, (float) Math.hypot(e.x - rs.doorX, e.y - rs.doorY));
            // ((28.0) Or waiting in their truck there, as the police wait in their cars at the stations.)
            for (Fleet.Vehicle v : tw.fleet.vehicles)
                for (Entity e : v.crew) if (e.agency == 3) best = Math.min(best, (float) Math.hypot(v.x - rs.doorX, v.y - rs.doorY) - 40);
            nearest = best;
            if (best < 60) minded++;
            if (samples == 1) first = best < 60;
        }
        // (Minded once the day has started, and still at forty seconds: the one minding it never goes off as crew.)
        check(first && nearest < 60, "a ranger minds the ranger station (" + minded + " of " + samples + " looks, the nearest " + (int) nearest + " off at 40 s)");
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

    static void swatToHorde() throws Exception {
        CityConfig c = cfg(0, CityConfig.MEDIUM, 6);
        World w = new World(c);
        w.populate(c);
        City city = w.city;
        check(w.dispatch.swatTeams >= 1, "a SWAT team at the precinct");
        // A horde turns up near people: the SWAT team is sent.
        Entity near = null;
        for (Entity e : w.entities) if (e.type == Entity.CIVILIAN && !e.dead && city.cityHall != null
                && Math.hypot(e.x - city.cityHall.doorX, e.y - city.cityHall.doorY) < 600) near = e;
        if (near == null) for (Entity e : w.entities) if (e.type == Entity.CIVILIAN && !e.dead) near = e;
        int teams = w.dispatch.swatTeams;
        // (The 911 call depends on who sees them first: a big enough horde, and long enough, that it always comes.)
        for (int k = 0; k < 20; k++) w.spawn(Entity.ZOMBIE, near.x + 40 + (k % 7) * 3, near.y + (k / 7) * 4);
        boolean sent = false;
        for (int f = 0; f < 30 * 150 && !sent; f++) {
            w.update(1 / 30f);
            if (w.dispatch.swatTeams < teams) sent = true;
        }
        check(sent, "SWAT sent to the horde");
    }

    static void magsShared() throws Exception {
        CityConfig c = cfg(CityConfig.SMALL, 220);
        World w = new World(c);
        w.populate(c);
        w.update(1 / 30f);
        float[] p = w.city.findWalkable(w.city.worldW() / 2, w.city.worldH() / 2);
        // Sharing a magazine.
        Entity a = w.spawn(Entity.SOLDIER, p[0], p[1] + 200), b = w.spawn(Entity.SOLDIER, p[0] + 10, p[1] + 200);
        b.ammo = 0;
        b.reserve = 2;
        int before = b.reserve;
        a.magCd = 0;
        // ((28.8) A full load: "plenty" is over half of it, and the loads doubled.)
        a.reserve = World.fullReserve(a);
        w.recount();
        for (int f = 0; f < 30 * 3 && b.reserve <= before; f++) { w.update(1 / 30f); w.evCount = 0; }
        check(b.reserve > before, "a soldier with plenty passes a mag to one running low");
    }

    static boolean offTheRoad(World w, Fleet.Vehicle v) {
        if (v.removedFromFleet) return true;
        int t = w.city.tileIndex(v.x, v.y);
        return t < 0 || w.city.tiles[t] != City.ROAD;
    }

    static void armoryGuardsTrucks() throws Exception {
        CityConfig c = cfg(0, CityConfig.MEDIUM, 6);
        World w = new World(c);
        w.populate(c);
        City city = w.city;
        int guards = 0;
        for (Entity e : w.entities)
            if (e.type == Entity.SOLDIER && e.role == Entity.ROLE_GUARD && Math.hypot(e.x - city.armory.doorX, e.y - city.armory.doorY) < 400) guards++;
        check(guards >= 2, guards + " guardsmen on duty at the armory");
        // Army trucks leaving a base take the ones parked there.
        CityConfig b = cfg(CityConfig.MEDIUM, 5);
        City base = new City(b);
        City.Facility f = base.nearestFacility(City.FACILITY_BASE, 0, 0);
        check(f != null && base.takeParkedTruck(f.x, f.y, 600) != null, "a parked truck drives off the base");
    }

    static void gateGuardsReplaced() throws Exception {
        CityConfig c = new CityConfig();
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
    }

    static void ifvAndGunTruck() throws Exception {
        for (int round = 0; round < 2; round++) {
            CityConfig c = cfg(0, CityConfig.MEDIUM, 77);
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

    static void ammoTruck() throws Exception {
        CityConfig c = cfg(0, CityConfig.MEDIUM, 77);
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

    static void mortarShells() throws Exception {
        CityConfig c = cfg(CityConfig.SMALL, 31);
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

    static void mortarRounds() throws Exception {
        CityConfig c = cfg(0, CityConfig.MEDIUM, 83);
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

    static void bulldozer() throws Exception {
        CityConfig c = cfg(0, CityConfig.MEDIUM, 77);
        World w = new World(c);
        w.populate(c);
        Fleet.Vehicle dozer = null;
        for (Fleet.Vehicle v : w.fleet.vehicles) if (v.kind == Fleet.K_DOZER) dozer = v;
        check(dozer != null && dozer.parked, "the base has its bulldozer, parked");
        int engineers = 0;
        for (Entity e : w.entities) if (!e.dead && e.role == Entity.ROLE_ENGINEER) engineers++;
        check(engineers >= 1, "and combat engineers (" + engineers + ")");
        // Two wrecks on roads in town. (Not in a junction: one left there is towed away whatever happens.)
        java.util.Random r = new java.util.Random(8);
        java.util.List<Fleet.Vehicle> wrecks = new java.util.ArrayList<Fleet.Vehicle>();
        for (int k = 0; k < 400 && wrecks.size() < 2; k++) {
            float[] q = w.city.randomWalkableInTown(r);
            Fleet.Vehicle car = w.fleet.placeCar(q[0], q[1]);
            if (car == null) continue;
            int t = w.city.tileIndex(car.x, car.y);
            if (t < 0 || w.city.tiles[t] != City.ROAD || !w.city.inTown(car.x, car.y) || Math.hypot(car.x - dozer.x, car.y - dozer.y) > 2000
                    || w.city.junctionIdAt(car.x, car.y) >= 0) {
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
        check(dozer.parked && !wrecks.get(0).removedFromFleet, "left be on a calm day (parked " + dozer.parked + ", wreck gone " + wrecks.get(0).removedFromFleet + ", zombies " + w.zombies + ")");
        boolean cleared = false;
        // ((26.0) Up to seven minutes: how long it takes swings a lot with the traffic round it, run to run.)
        for (int f = 0; f < 30 * 420 && !cleared; f++) {
            w.alert = Math.max(w.alert, 1);
            w.update(1 / 30f);
            w.evCount = 0;
            // ((29.2) Off the road either way: towed or pushed away, or shoved onto the pavement by the traffic going by -
            // then there's nothing left for the bulldozer to clear.)
            cleared = offTheRoad(w, wrecks.get(0)) && offTheRoad(w, wrecks.get(1));
        }
        check(cleared, "both wrecks pushed off the road (" + w.fleet.dozerCleared + ")");
    }

    static void armouryNotEmptied() throws Exception {
        CityConfig c = cfg(CityConfig.MEDIUM, 77);
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

    static void truckBay() throws Exception {
        CityConfig c = cfg(0, CityConfig.LARGE, 3);
        World w = new World(c);
        w.populate(c);
        for (int f = 0; f < 30; f++) w.update(1 / 30f);
        // Trucks waiting at a base for their squads wait in the yard, clear of the gate.
        City.Facility base = null;
        for (City.Facility f : w.city.facilities) if (f.kind == City.FACILITY_BASE && f.gateX > 0) base = f;
        check(base != null, "a base with a gate");
        float[] g = w.city.nearestDrivable(base.gateX, base.gateY);
        float[] bay = w.fleet.bay(g, base);
        check(bay != null && Math.hypot(bay[0] - base.gateX, bay[1] - base.gateY) >= City.T * 3.4f,
                "a truck mustering at the base waits in the yard, off the gateway");
    }

    static void commanders() throws Exception {
        CityConfig c = cfg(CityConfig.MEDIUM, 13);
        World w = new World(c);
        w.populate(c);
        w.update(1 / 30f);
        w.outbreak = true;
        w.alert = 2;
        // A commander and three squads on a street, a pack of 30 coming at them from 300 off down it.
        java.util.Random r = new java.util.Random(5);
        float[] p = null, z0 = null;
        for (int k = 0; k < 400 && z0 == null; k++) {
            p = w.city.randomStreetSpot(r);
            double a = r.nextFloat() * 6.283;
            float[] q = w.city.findWalkable(p[0] + (float) Math.cos(a) * 300, p[1] + (float) Math.sin(a) * 300);
            if (q != null && Math.hypot(q[0] - p[0], q[1] - p[1]) > 250 && w.city.los(p[0], p[1], q[0], q[1])) z0 = q;
        }
        check(z0 != null, "a street with room");
        Entity cm = w.spawnSoldier(Entity.ROLE_COMMANDER, p[0], p[1]);
        java.util.List<Entity> ours = new java.util.ArrayList<Entity>();
        ours.add(cm);
        for (int s = 0; s < 3; s++)
            for (int m = 0; m < 5; m++) {
                float[] q = w.city.findWalkable(p[0] + m * 4 - 8, p[1] + s * 5 - 5);
                Entity e = w.spawnSoldier(Entity.ROLE_RIFLE, q[0], q[1]);
                e.squad = 901 + s;
                e.member = m + 1;
                ours.add(e);
            }
        java.util.List<Entity> pack = new java.util.ArrayList<Entity>();
        for (int k = 0; k < 30; k++) {
            float[] q = w.city.findWalkable(z0[0] + r.nextFloat() * 50 - 25, z0[1] + r.nextFloat() * 50 - 25);
            if (q != null) pack.add(w.spawn(Entity.ZOMBIE, q[0], q[1]));
        }
        Quiet q = empty(w, p[0], p[1], 700, ours, pack);
        final Entity fc = cm;
        check(stepUntil(w, q, 5, new Cond() {
            public boolean done() {
                return fc.cmdLine;
            }
        }), "the commander forms a line");
        String card = w.commanding.describe(cm);
        check(card != null && card.startsWith("Commanding: 3 squads"), "the card says so (" + card + ")");
        // Spread out, not stacked: each on a place of their own on the line.
        int on = 0, stacked = 0;
        for (Entity e : ours) {
            if (e == cm || e.lineT <= w.time) continue;
            on++;
            for (Entity o : ours) if (o != e && o != cm && o.lineT > w.time && Math.hypot(o.lineX - e.lineX, o.lineY - e.lineY) < 6) stacked++;
        }
        check(on >= 12 && stacked == 0, "squads spread along it (" + on + " on it, " + stacked + " stacked)");
        // The line about to break: the dead on it and the rounds gone - they all go back, together, to one place.
        for (Entity z : pack) {
            float[] s0 = w.city.findWalkable(cm.cmdAx + r.nextFloat() * 40 - 20, cm.cmdAy + r.nextFloat() * 40 - 20);
            if (s0 != null) {
                z.x = s0[0];
                z.y = s0[1];
            }
        }
        for (Entity e : ours) e.reserve = 0;
        w.spatial.buildHash();
        cm.cmdCd = 0;
        w.commanding.lead(cm, true, 0);
        int back = 0;
        for (Entity e : ours) if (e.rallyT > w.time && e.rallyX == cm.rallyX && e.rallyY == cm.rallyY) back++;
        check(w.commanding.retreats == 1 && back >= 12, "the commander pulls them all back together (" + back + ")");
        // The MRAP: armoured, a gunner, hard to hurt.
        Fleet.Vehicle m = w.fleet.make(Fleet.TRUCK);
        w.fleet.setKind(m, Fleet.K_MRAP);
        check(Fleet.mountedGun(m) && Fleet.gunManned(m) && Fleet.armoured(m) && m.maxHp >= 700, "the MRAP has a manned gun and armour");
        check(w.fleet.describe(m).contains("MRAP"), "and is named");
        // Fire support trucks parked at the bases; one goes out to a big fight and shells the pack, not the people.
        City.Facility home = null;
        for (City.Facility f : w.city.facilities) if (home == null && w.fleet.fireSupport.ready(f) > 0) home = f;
        check(home != null, "a fire support truck in a base's garage");
        for (Entity o : ours) o.dead = o.removed = true;
        for (Entity z : pack) z.dead = z.removed = true;
        float[] f0 = null;
        for (int k = 0; k < 400 && f0 == null; k++) {
            float[] g = w.city.randomStreetSpot(r);
            float d = (float) Math.hypot(g[0] - home.x, g[1] - home.y);
            boolean kept = false;
            for (int i = 0; i < w.dispatch.zones.size(); i++) kept |= w.dispatch.zones.get(i).contains(g[0], g[1], 2.5f);
            for (int i = 0; i < w.bases.size(); i++) kept |= w.bases.get(i).contains(g[0], g[1], 15);
            if (d > 500 && d < 900 && !kept) f0 = g;
        }
        java.util.List<Entity> fight = new java.util.ArrayList<Entity>();
        for (int k = 0; k < 40; k++) {
            float[] g = w.city.findWalkable(f0[0] + r.nextFloat() * 60 - 30, f0[1] + r.nextFloat() * 60 - 30);
            if (g != null) fight.add(w.spawn(Entity.ZOMBIE, g[0], g[1]));
        }
        for (Entity z : fight) untouchable(z);
        // (Two soldiers fighting them, out of the blast.)
        float ex = home.x - f0[0], ey = home.y - f0[1], el = (float) Math.hypot(ex, ey);
        float[] sp = w.city.findWalkable(f0[0] + ex / el * 220, f0[1] + ey / el * 220);
        java.util.List<Entity> guns = new java.util.ArrayList<Entity>();
        for (int k = 0; k < 2; k++) guns.add(w.spawnSoldier(Entity.ROLE_RIFLE, sp[0] + k * 6, sp[1]));
        final Quiet q2 = quiet(w, f0[0], f0[1], 200, fight, guns);
        final World fw = w;
        final java.util.List<Entity> fg = guns, fz = fight;
        boolean fired = stepUntil(w, q2, 90, new Cond() {
            public boolean done() {
                for (Entity g : fg) hold(g, g.x, g.y);
                for (Entity z : fz) z.vx = z.vy = 0;
                return fw.fleet.fireSupport.salvos > 0;
            }
        });
        Fleet.Vehicle fs = null;
        for (Fleet.Vehicle v : w.fleet.vehicles) if (v.kind == Fleet.K_FIRE_SUPPORT) fs = v;
        check(fired && fs != null, "the fire support truck comes out and fires (" + w.fleet.fireSupport.salvos + ")");
        check(Math.hypot(fs.x - f0[0], fs.y - f0[1]) > 200, "from behind the lines, well back from the pack");
        // Never on people: a pack with a soldier among it isn't a target.
        Entity among = w.spawnSoldier(Entity.ROLE_RIFLE, fight.get(0).x, fight.get(0).y);
        w.spatial.buildHash();
        check(among != null && w.fieldworks.safePack(f0[0], f0[1], 100, 25, 150, 0) == null, "not with friends close to it");
    }

    static void fireMissions() throws Exception {
        CityConfig c = cfg(CityConfig.LARGE, 27);
        World w = new World(c);
        w.populate(c);
        for (int f = 0; f < 45; f++) w.update(1 / 30f);
        check(w.city.mainBase() != null, "an army base for the mortars");
        // A police command post set up on a street well away from the base and its own headquarters.
        java.util.Random r = new java.util.Random(4);
        float[] at = null;
        for (int k = 0; k < 200 && at == null; k++) {
            float[] q = w.city.randomStreetSpot(r);
            q = q == null ? null : w.city.nearestDrivable(q[0], q[1]);
            if (q == null || World.hypot(q[0] - w.city.mainBase().x, q[1] - w.city.mainBase().y) < 900) continue;
            if (w.city.findWalkable(q[0] + 420, q[1]) == null) continue;
            at = q;
        }
        check(at != null, "a street for the post");
        Ops.Post p = w.ops.police;
        if (p.truck != null) w.fleet.discard(p.truck);
        w.ops.restorePost(p, at[0], at[1], 0, at[0], at[1] + 17, 900);
        check(p.up && p.truck != null, "a command post set up");
        // The aid station: a wounded officer by the tent is patched up; one hurt badly further off makes for it.
        Entity hurt = w.spawnCop(0, p.x + 20, p.y), far = w.spawnCop(0, p.x + 200, p.y);
        hurt.hp = hurt.maxHp * 0.5f;
        hurt.task = Dispatch.T_POST;
        hurt.postX = hurt.x;
        hurt.postY = hurt.y;
        far.hp = far.maxHp * 0.2f;
        check(w.ops.aidFor(far, Float.NaN, Float.NaN) == p && w.ops.aidFor(far, far.x + 10, far.y) == null,
                "the post is the aid station when it's nearer than the field hospital");
        // The fire mission: forty of the dead in the open 420 off, nobody living near them.
        float[] pack = w.city.findWalkable(at[0] + 420, at[1]);
        java.util.List<Entity> dead = new java.util.ArrayList<Entity>();
        for (int k = 0; k < 40; k++) {
            Entity z = w.spawn(Entity.ZOMBIE, pack[0] + (k % 8) * 5 - 18, pack[1] + (k / 8) * 5 - 10);
            if (z == null) continue;
            z.speed = z.runSpeed = 0;
            dead.add(z);
        }
        p.fireCd = 0;
        // ((27.5) A big fight with people at it brings the base's fire support truck out too, and its salvos count
        // as fire missions: the post's own is the one left over.)
        int shells = w.fireMissions, salvos = w.fleet.fireSupport.salvos;
        boolean marked = false;
        for (int f = 0; f < 30 * 16; f++) {
            for (Entity o : w.entities)
                if (!o.dead && !o.isZombie() && o != hurt && o != far && Math.hypot(o.x - pack[0], o.y - pack[1]) < 320) o.dead = o.removed = true;
            far.hp = Math.min(far.hp, far.maxHp * 0.3f);
            w.update(1 / 30f);
            w.evCount = 0;
            marked |= p.fireUntil > w.time && Math.hypot(p.fireX - pack[0], p.fireY - pack[1]) < 80;
        }
        int left = 0;
        for (Entity z : dead) if (!z.dead) left++;
        check(p.missions == 1 && w.fireMissions - (w.fleet.fireSupport.salvos - salvos) == shells + 1 && marked,
                "the post called one fire mission on them, marked while inbound (" + p.missions + ", " + (w.fireMissions - shells) + " in all)");
        check(left < dead.size() - 10, "and the shells landed among them (" + left + " of " + dead.size() + " left)");
        check(hurt.hp >= hurt.maxHp && p.treated >= 1, "a wounded officer at the tent was patched up (" + p.treated + ", " + hurt.hp + "/" + hurt.maxHp + ", " + (int) Math.hypot(hurt.x - p.x, hurt.y - p.y) + " off, up " + p.up + ")");
        check(w.fleet.status(p.truck).contains("treated") && w.fleet.status(p.truck).contains("fire mission"), "both on its status line: " + w.fleet.status(p.truck));
        // Overwatch: a marksman sent to a call beside a tall building takes its roof.
        City.Building tall = null;
        float[] call = null;
        for (City.Building b : w.city.buildings) {
            if (b.collapsed || b.height < World.ROOF_HEIGHT + 6) continue;
            float[] q = w.city.findWalkable(b.x1 + 60, (b.y0 + b.y1) / 2);
            if (q == null || q[0] < b.x1 + 35 || q[0] > b.x1 + 90 || Math.abs(q[1] - (b.y0 + b.y1) / 2) > 30) continue;
            if (Math.hypot(q[0] - at[0], q[1] - at[1]) < 700 || w.city.findWalkable(b.x1 + 9, q[1]) == null) continue;
            tall = b;
            call = q;
            break;
        }
        check(tall != null, "a tall building by a street");
        for (Entity o : w.entities) if (!o.dead && Math.hypot(o.x - call[0], o.y - call[1]) < 500) o.dead = o.removed = true;
        Dispatch.Incident inc = w.dispatch.sighting(call[0], call[1], null);
        Entity z = w.spawn(Entity.ZOMBIE, call[0], call[1]);
        z.speed = z.runSpeed = 0;
        Entity m = w.spawnCop(Entity.ROLE_MARKSMAN, call[0] + 30, call[1] + 10);
        boolean up = false;
        for (int f = 0; f < 30 * 20 && !up; f++) {
            z.hp = z.maxHp = 100000;
            inc.resolved = false;
            m.task = Dispatch.T_RESPOND;
            m.incident = inc;
            w.update(1 / 30f);
            w.evCount = 0;
            up = m.onTower && m.roof != null;
        }
        check(up && w.overwatch.contains(m) && m.roofX > m.roof.x0 && m.roofX < m.roof.x1 && m.roofY > m.roof.y0 && m.roofY < m.roof.y1,
                "the marksman is up on the roof (" + m.onTower + ", " + (m.roof != null) + ")");
        // One of the dead at the foot of the wall can't get at them.
        Entity z2 = w.spawn(Entity.ZOMBIE, m.x + 12, m.y);
        z2.hp = z2.maxHp = 100000;
        float hp = m.hp;
        for (int f = 0; f < 30 * 4; f++) {
            z.hp = z2.hp = 100000;
            inc.resolved = false;
            m.task = Dispatch.T_RESPOND;
            m.incident = inc;
            w.update(1 / 30f);
            w.evCount = 0;
        }
        check(m.hp >= hp && !m.infected && m.onTower, "out of the dead's reach up there (" + m.hp + " of " + hp + ")");
        // The call over: down again.
        z.dead = z.removed = z2.dead = z2.removed = true;
        inc.resolved = true;
        for (int f = 0; f < 45; f++) w.update(1 / 30f);
        check(m.roof == null && !m.onTower && !w.overwatch.contains(m), "and down again once the call is over");
    }

    static void boatsPostsSpecialForces() throws Exception {
        CityConfig c = cfg(13, CityConfig.MEDIUM, 77);
        World w = new World(c);
        w.populate(c);
        check(w.ops.boats.size() >= 1, "police boats moored on the water (" + w.ops.boats.size() + ")");
        Fleet.Vehicle b = w.ops.boats.get(w.ops.boats.size() - 1);
        float bx = b.x, by = b.y;
        for (int f = 0; f < 30 * 12; f++) w.update(1 / 30f);
        check(World.hypot(b.x - bx, b.y - by) > 50 && b.type == Fleet.BOAT, "and out on patrol (" + (int) World.hypot(b.x - bx, b.y - by) + ")");
        // Someone cornered at the water's edge by one of the dead.
        int[] s = null;
        for (int k = 0; k < 6 && s == null; k++) s = w.ops.shoreNear(b.homeLandX + 300 - k * 120, b.homeLandY - 30, 30, true);
        check(s != null, "a bit of shore");
        float lx = (s[1] % w.city.w + 0.5f) * City.T, ly = (s[1] / w.city.w + 0.5f) * City.T;
        Entity civ = w.spawn(Entity.CIVILIAN, lx, ly), z = w.spawn(Entity.ZOMBIE, lx + 70, ly);
        // (Nobody else about to shoot the one cornering them, or to take them off first: only the boats come.)
        Quiet shore = quiet(w, lx, ly, 400, civ, z, w.ops.boats);
        for (int k = 0; k < 50 && w.ops.rescued == 0; k++) {
            civ.stun = z.stun = 5;
            for (int f = 0; f < 30; f++) {
                // (Still cornered until a boat has taken the job: one shot from a passing boat's gun doesn't end
                // the test - another of the dead takes its place.)
                boolean coming = false;
                for (Fleet.Vehicle bt : w.ops.boats) coming |= bt.waitFor == civ && (bt.boatJob == Ops.J_RESCUE || bt.boatJob == Ops.J_LOAD);
                if (z.dead && !coming && w.ops.rescued == 0) {
                    z = w.spawn(Entity.ZOMBIE, lx + 70, ly);
                    shore.keep(z);
                }
                if (!z.dead) z.stun = Math.max(z.stun, 1);
                step(w, shore);
            }
        }
        check(w.ops.rescued >= 1 && !civ.dead && World.hypot(civ.x - lx, civ.y - ly) > 200, "a boat came in and took them round to safety (" + w.ops.rescued + ")");
        z.dead = z.removed = true;
        // The command post: a truck parked at the police headquarters (and the army's at its base), sent out
        // with real officers driving it.
        for (int f = 0; f < 30 * 2; f++) w.update(1 / 30f);
        check(w.ops.police.parked() && w.ops.police.truck.kind == Fleet.K_COMMAND && w.ops.police.truck.crew.size() >= 2, "a command truck parked at headquarters, its crew waiting in it (" + w.ops.police.truck.crew.size() + ")");
        check(w.city.mainBase() == null || w.ops.army.parked() && w.ops.army.truck.kind == Fleet.K_ARMY_POST, "and the army's at its base");
        int cops0 = w.counts[Entity.COP];
        float[] p = driveSpot(w, w.ops.police.truck.x, w.ops.police.truck.y, 500, 1000, 3);
        Fleet.Vehicle cp = w.ops.placePost(p[0], p[1]);
        check(cp != null && (cp.state == Fleet.MUSTER || cp.state == Fleet.DRIVE && cp.crew.size() >= 2) && w.counts[Entity.COP] <= cops0, "sent out, crewed by officers already there (" + (cp != null ? cp.crew.size() : -1) + ")");
        boolean drove = false;
        for (int f = 0; f < 30 * 150 && !w.ops.police.up; f++) {
            w.update(1 / 30f);
            if (cp.state == Fleet.DRIVE && !cp.crew.isEmpty()) drove = true;
        }
        check(drove && w.ops.police.up, "they drove it there and set it up (" + cp.state + ")");
        float[] pn = w.city.findWalkable(w.ops.police.x + 40, w.ops.police.y), pf = driveSpot(w, w.ops.police.x, w.ops.police.y, 600, 1500, 9);
        Entity cop = w.spawnCop(0, pn[0], pn[1]), far = w.spawnCop(0, pf[0], pf[1]);
        check(w.ops.boosts(cop) && !w.ops.boosts(far), "it steadies the officers near it, not those far off");
        cop.reserve = 0;
        for (int f = 0; f < 45; f++) w.update(1 / 30f);
        check(cop.reserve > 0 && w.ops.police.resupplied > 0, "and hands out ammunition (" + cop.reserve + ")");
        cp.hp = 60;
        for (int k = 0; k < 24; k++) w.spawn(Entity.ZOMBIE, w.ops.police.x + (k % 6) * 4 - 10, w.ops.police.y + (k / 6) * 4 - 10);
        for (int f = 0; f < 30 * 15 && w.ops.police.truck != null; f++) w.update(1 / 30f);
        check(w.ops.police.lost == 1 && !w.ops.police.up && cp.broken, "the dead overrun it (" + w.ops.police.lost + ")");
        // Special forces.
        float[] q = w.city.randomStreetSpot(new java.util.Random(5));
        java.util.ArrayList<Entity> team = w.ops.callIn(q[0], q[1], "a test", true);
        check(team.size() == 4, "a special forces team on its way (" + team.size() + ")");
        int down = 0;
        for (int f = 0; f < 30 * 25 && down < 3; f++) {
            w.update(1 / 30f);
            if (f % 30 != 0) continue;
            int n = 0;
            for (Entity e : team) if (!e.dead && e.role == Entity.ROLE_SF && e.maxHp >= 150 && Dispatch.name(e).startsWith("Viper")) n++;
            down = Math.max(down, n);
        }
        check(down >= 3, "roped down on the spot (" + down + ")");
        // A Humvee's mounted gun.
        int shots = w.fleet.shotsFrom;
        Fleet.Vehicle hv = w.fleet.startReconAt(q[0] + 300, q[1]);
        for (int k = 0; k < 5; k++) w.spawn(Entity.ZOMBIE, hv.x + 60 + k * 5, hv.y + 10);
        for (int f = 0; f < 30 * 3; f++) w.update(1 / 30f);
        check(w.fleet.shotsFrom > shots, "a Humvee's gunner opens up (" + (w.fleet.shotsFrom - shots) + ")");
        check(Fleet.mountedGun(cp) == false && Fleet.mountedGun(b), "a boat carries guns, the command post doesn't");
        File f = File.createTempFile("zc26", ".dat");
        SaveGame.save(w, f);
        World l = SaveGame.load(f);
        f.delete();
        check(l.ops.police.lost == 1 && l.ops.rescued == w.ops.rescued && l.ops.boats.size() == w.ops.boats.size(), "kept in a save");
    }

    static void combatMedic() throws Exception {
        CityConfig c = cfg(CityConfig.SMALL, 21);
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

    static void fieldHospital() throws Exception {
        CityConfig c = cfg(0, CityConfig.MEDIUM, 77);
        World w = new World(c);
        w.populate(c);
        java.util.Random r = new java.util.Random(2);
        // The field hospital: set up once the army's in it; a badly hurt soldier goes there and is patched up.
        w.clearZombies();
        float[] far = w.city.randomWalkable(r);
        w.alert = 2;
        Field ob = World.class.getDeclaredField("outbreak");
        ob.setAccessible(true);
        WM fh = wm("fieldHospital");
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

    static void ambulancesAndRepair() throws Exception {
        CityConfig c = cfg(CityConfig.MEDIUM, 275);
        World w = new World(c);
        w.populate(c);
        for (int f = 0; f < 30 * 3; f++) w.update(1 / 30f);
        // An officer badly hurt a long walk from care (on a street), and a patrol car badly knocked about.
        java.util.Random r = new java.util.Random(4);
        float[] q0 = w.city.randomStreetSpot(r);
        Entity cop = w.spawnCop(0, q0[0], q0[1]);
        float far = 0, bx = q0[0], by = q0[1];
        for (int k = 0; k < 300; k++) {
            float[] s = w.city.randomStreetSpot(r);
            if (s == null || !w.city.roadNear(s[0], s[1], 2)) continue;
            cop.x = s[0];
            cop.y = s[1];
            float[] q = w.fleet.medevac.careFor(cop, s[0], s[1]);
            float d = q == null ? 0 : (float) Math.hypot(q[0] - s[0], q[1] - s[1]);
            if (d > far) {
                far = d;
                bx = s[0];
                by = s[1];
            }
        }
        cop.x = bx;
        cop.y = by;
        cop.task = Dispatch.T_NONE;
        Fleet.Vehicle car = null;
        for (Fleet.Vehicle v : w.fleet.vehicles)
            if (v.type == Fleet.CRUISER && v.patrol && v.state == Fleet.PATROL && !v.crew.isEmpty() && v.kind != Fleet.K_BIKE) car = v;
        check(cop != null && far > ArmedAI.WALK_TO_CARE && car != null, "a wounded officer far from care, and a patrol car");
        cop.hp = cop.maxHp * 0.3f;
        car.hp = car.maxHp * 0.3f;
        check(w.fleet.patrolFor(car.x, car.y) != car, "a badly damaged car takes no calls");
        boolean waited = false, inCare = false, mending = false;
        // (Till this officer is back - another wounded one coming back on duty doesn't count - and the car mended.)
        // ((28.8) Six minutes, not four: with 28.8's other dice the officer was still being patched up at four, 31 of 36.)
        for (int f = 0; f < 30 * 360 && !(inCare && cop.hp >= cop.maxHp && cop.task != Dispatch.T_HEAL && !cop.inCare && w.returnedToDuty > 0 && car.hp >= car.maxHp); f++) {
            w.update(1 / 30f);
            waited |= cop.waitPickup;
            inCare |= cop.inCare;
            mending |= w.fleet.status(car).contains("Repairing");
        }
        check(waited && w.fleet.medevac.pickups > 0, "an ambulance fetched them (" + w.fleet.medevac.pickups + ")");
        check(inCare && w.returnedToDuty >= 1 && cop.hp >= cop.maxHp && cop.task != Dispatch.T_HEAL && !cop.dead,
                "patched up in care and back on duty (" + (int) cop.hp + "/" + (int) cop.maxHp + ")");
        check(mending && car.hp >= car.maxHp && !car.broken && w.fleet.medevac.repaired > 0, "the car mended at the station");
        int armoured = 0;
        for (Fleet.Vehicle v : w.fleet.vehicles) if (v.kind == Fleet.K_ARMD_AMB) armoured++;
        check(armoured >= 2, "armoured ambulances at the base (" + armoured + ")");
    }

    static void volunteers() throws Exception {
        CityConfig c = cfg(CityConfig.TINY, 52);
        World w = new World(c);
        w.populate(c);
        w.update(1 / 30f);
        // Not before the emergency broadcast.
        w.outbreak = true;
        w.peakZombies = 10;
        w.alert = 1;
        w.recruiting.volunteers();
        int going = 0;
        for (Entity e : w.entities) if (e.volunteering) going++;
        check(going == 0, "nobody volunteers before the broadcast");
        // After it: a few at a time, to the nearest station or base with kit, and only so many.
        w.alert = 2;
        for (int k = 0; k < 40; k++) w.recruiting.volunteers();
        java.util.List<Entity> vols = new java.util.ArrayList<Entity>();
        for (Entity e : w.entities) if (!e.dead && e.volunteering && e.task == Dispatch.T_ENLIST) vols.add(e);
        check(!vols.isEmpty() && vols.size() <= Recruitment.MAX_VOLUNTEERS, "some sign up, no more than the cap (" + vols.size() + ")");
        boolean police = false, army = false;
        for (Entity e : vols) {
            check(e.type == Entity.CIVILIAN && !e.infected && !e.inmate && e.base == null && e.enlistAt != null, "able adults only");
            if (e.enlistAt.kind == City.FACILITY_POLICE) police = true;
            else army = true;
        }
        // Each at the station or base: they come out in uniform, the same person.
        java.util.Map<Entity, Integer> seeds = new java.util.HashMap<Entity, Integer>();
        for (Entity e : vols) {
            seeds.put(e, e.nameSeed);
            e.x = e.enlistAt.x;
            e.y = e.enlistAt.y;
        }
        for (int f = 0; f < 30 * 10; f++) {
            for (Entity e : vols) if (!e.removed) {
                e.x = e.enlistAt != null ? e.enlistAt.x : e.x;
                e.y = e.enlistAt != null ? e.enlistAt.y : e.y;
            }
            w.update(1 / 30f);
        }
        int aux = 0, sol = 0;
        for (Entity e : w.entities) {
            if (e.dead || e.role != Entity.ROLE_VOLUNTEER) continue;
            check(seeds.containsValue(e.nameSeed) && e.signedUpAt != null, "the same person, signed up at a station or base");
            if (e.type == Entity.COP) {
                aux++;
                check("Auxiliary".equals(Names.copRank(e)) && e.maxHp < 60 && e.isArmed(), "a police auxiliary: a pistol, less to them than an officer");
            } else {
                sol++;
                check("Volunteer".equals(Names.soldierRank(e)) && e.maxHp < 100 && e.grenades == 0, "an army volunteer: a rifle, no grenades");
            }
        }
        check(aux + sol >= 1 && (aux > 0 || !police) && (sol > 0 || !army), "they come out as auxiliaries and volunteers (" + aux + ", " + sol + ")");
        check(w.volunteersPolice + w.volunteersArmy == aux + sol, "counted (" + w.volunteersPolice + " + " + w.volunteersArmy + ")");
        // A fallen station or base takes nobody more. ((28.9) Anyone already on their way before they fell isn't new.)
        java.util.HashSet<Entity> onTheirWay = new java.util.HashSet<Entity>();
        for (Entity e : w.entities) if (!e.dead && e.volunteering && e.task == Dispatch.T_ENLIST) onTheirWay.add(e);
        for (City.Facility f : w.city.facilities) if (f.kind == City.FACILITY_POLICE || f.kind == City.FACILITY_BASE) f.fallen = true;
        for (Entity e : w.entities) if (!e.dead && e.type == Entity.CIVILIAN) e.asked = false;
        for (int k = 0; k < 10; k++) w.recruiting.volunteers();
        int more = 0;
        for (Entity e : w.entities) if (!e.dead && e.volunteering && e.task == Dispatch.T_ENLIST && !vols.contains(e) && !onTheirWay.contains(e)) more++;
        check(more == 0, "none once the stations and bases have fallen");
    }

    static void volunteersCapped() throws Exception {
        CityConfig c = cfg(CityConfig.MASSIVE, 280);
        World w = new World(c);
        w.populate(c);
        w.update(1 / 30f);
        // After the broadcast: volunteers sign up, are posted at the station or base they signed up at, and no
        // more than the cap ever do, however many are lost. Nor is each one lost made up with another recruit.
        w.outbreak = true;
        w.peakZombies = 10;
        int total = policeNow(w), most = total;
        for (int round = 0; round < 6; round++) {
            for (int k = 0; k < 10; k++) {
                w.alert = 2;
                w.recruiting.volunteers();
            }
            for (int f = 0; f < 30 * 20; f++) {
                w.alert = 2;
                w.warResult = 0;
                for (Entity e : w.entities)
                    if (!e.dead && e.volunteering && e.task == Dispatch.T_ENLIST && e.enlistAt != null) {
                        e.x = e.enlistAt.x;
                        e.y = e.enlistAt.y;
                    }
                w.update(1 / 30f);
                if (f % 30 == 0) most = Math.max(most, policeNow(w));
            }
            int vols = 0, posted = 0;
            for (Entity e : w.entities) {
                if (e.dead || e.role != Entity.ROLE_VOLUNTEER || e.signedUpAt == null) continue;
                vols++;
                if (e.task == Dispatch.T_POST && World.hypot(e.postX - e.signedUpAt.x, e.postY - e.signedUpAt.y) < e.signedUpAt.r + 20
                        && World.hypot(e.x - e.signedUpAt.x, e.y - e.signedUpAt.y) < e.signedUpAt.r + 60) posted++;
            }
            check(posted == vols, "volunteers posted at their station or base (" + posted + " of " + vols + ")");
            // Every one of them lost.
            for (Entity e : w.entities) if (e.role == Entity.ROLE_VOLUNTEER) e.dead = e.removed = true;
        }
        int ever = w.volunteersPolice + w.volunteersArmy;
        check(ever > 0 && ever <= Recruitment.MAX_VOLUNTEERS, "volunteers capped for the game (" + ever + ")");
        check(most <= total + Recruitment.MAX_VOLUNTEERS && w.recruits <= 4, "the police stay bounded (" + total + " -> most " + most + ", recruits " + w.recruits + ")");
    }

    static void experience() throws Exception {
        CityConfig c = cfg(CityConfig.SMALL, 2301);
        World w = new World(c);
        w.populate(c);
        float[] p = w.city.findWalkable(w.city.worldW() / 2, w.city.worldH() / 2);
        Entity s = w.spawnSoldier(Entity.ROLE_RIFLE, p[0], p[1]);
        float hp0 = s.maxHp;
        w.gainXp(s, 300);
        check(s.level == 4 && Integer.bitCount(s.perks) == 4, "300 experience: level 5, four perks (" + (s.level + 1) + ", " + World.perkList(s) + ")");
        check(!World.perk(s, World.P_TOUGH) || s.maxHp > hp0 * 1.15f, "tough means more health");
        java.io.File file = java.io.File.createTempFile("zcx", ".dat");
        SaveGame.save(w, file);
        World l = SaveGame.load(file);
        file.delete();
        boolean kept = false;
        for (Entity e : l.entities) if (e.nameSeed == s.nameSeed && e.type == Entity.SOLDIER) kept = e.level == s.level && e.perks == s.perks && Math.abs(e.xp - s.xp) < 0.01f;
        check(kept, "level, perks and experience kept in a save");
        // ((29.0) The levels past the perks: up to 15, each tougher and a better shot.)
        Entity v = w.spawnSoldier(Entity.ROLE_RIFLE, p[0] + 10, p[1]);
        w.gainXp(v, 700);
        float hp7 = v.maxHp, aim7 = World.levelAim(v);
        w.gainXp(v, 4500);
        check(v.level == 14 && World.LEVEL_XP.length == 15, "4,500 experience and more: level 15, the top (" + (v.level + 1) + ")");
        check(v.maxHp > hp7 * 1.5f && World.levelAim(v) > aim7 + 0.1f, "each level past 7 tougher and a better shot (" + (int) hp7 + " -> " + (int) v.maxHp + ")");
    }

    static void survivorBase() throws Exception {
        CityConfig c = cfg(CityConfig.LARGE, 3);
        World w = new World(c);
        w.populate(c);
        java.util.Random r = new java.util.Random(1);
        for (int i = 0; i < 6; i++) {
            float[] p = w.city.randomWalkable(r);
            w.spawn(Entity.ZOMBIE, p[0], p[1]);
        }
        w.recount();
        float[] p = null;
        for (int t = 0; t < 200; t++) {
            p = w.city.randomWalkableInTown(r);
            if (!w.zombieWithin(p[0], p[1], 600)) break;
        }
        for (int i = 0; i < 4; i++) w.armCivilian(w.spawn(Entity.CIVILIAN, p[0] + i * 6, p[1]), 30);
        // (Up to five minutes for the wall, its guards and a watchtower: how fast they go up turns on who happens to be
        // about.)
        for (int s = 0; s < 30 * 300 && !(!w.bases.isEmpty() && w.bases.get(0).walled && (w.bases.get(0).towersUp >= 1 || w.bases.get(0).towers.length == 0) && w.bases.get(0).guards >= 2
                && w.bases.get(0).members >= 4 && SurvivorBases.wallsUp(w.city, w.bases.get(0)) >= w.bases.get(0).ring.length - w.bases.get(0).gates.length); s++) {
            w.update(1 / 30f);
            w.evCount = 0;
            // (The outbreak kept going far off, but not at the base.)
            for (Entity e : w.entities)
                if (e.isZombie() && !w.bases.isEmpty() && Math.hypot(e.x - w.bases.get(0).cx(), e.y - w.bases.get(0).cy()) < 900) e.hp = 0;
        }
        check(!w.bases.isEmpty(), "a survivor base was founded");
        SurvivorBases.Base b = w.bases.get(0);
        check(b.walled && SurvivorBases.wallsUp(w.city, b) >= b.ring.length - b.gates.length, "its wall is up (" + SurvivorBases.wallsUp(w.city, b) + " of " + b.ring.length + ")");
        check(b.members >= 4 && b.guards >= 2, "people inside and guards on it (" + b.members + ", " + b.guards + ")");
        // (Where its corners leave room for one.)
        check(b.towersUp >= 1 || b.towers.length == 0, "a watchtower (" + b.towersUp + " of " + b.towers.length + " spots)");
        File f = File.createTempFile("zcs", ".dat");
        SaveGame.save(w, f);
        World l = SaveGame.load(f);
        f.delete();
        check(l.bases.size() == w.bases.size() && l.bases.get(0).name.equals(b.name) && l.bases.get(0).walled, "kept in a save");
    }

    static void settlement() throws Exception {
        CityConfig c = cfg(CityConfig.LARGE, 3);
        World w = new World(c);
        w.populate(c);
        // Two walled bases near each other.
        SurvivorBases.Base a = null, b = null;
        for (City.Building x : w.city.buildings) {
            if (x.collapsed || x.capacity == 0 || a != null) continue;
            SurvivorBases.Base pa = SurvivorBases.plan(w.city, x);
            if (pa == null) continue;
            for (City.Building y : w.city.buildings) {
                if (y == x || y.collapsed || y.capacity == 0) continue;
                SurvivorBases.Base pb = SurvivorBases.plan(w.city, y);
                if (pb == null) continue;
                int gx = Math.max(0, Math.max(pa.tx0, pb.tx0) - Math.min(pa.tx1, pb.tx1));
                if (gx >= 5 && gx <= 12 && Math.max(0, Math.max(pa.ty0, pb.ty0) - Math.min(pa.ty1, pb.ty1)) == 0
                        && Math.max(pa.tx1, pb.tx1) - Math.min(pa.tx0, pb.tx0) <= 44) {
                    a = w.restoreBase(x);
                    b = w.restoreBase(y);
                    break;
                }
            }
        }
        check(a != null && b != null, "two bases near each other");
        a.walled = b.walled = true;
        WM jb = wm("joinBases");
        jb.setAccessible(true);
        jb.invoke(w);
        check(w.bases.size() == 1 && w.bases.get(0).settlement, "they join into one settlement");
        File f = File.createTempFile("zcs", ".dat");
        SaveGame.save(w, f);
        World l = SaveGame.load(f);
        f.delete();
        SurvivorBases.Base m = w.bases.get(0), lm = l.bases.isEmpty() ? null : l.bases.get(0);
        check(lm != null && lm.settlement && lm.tx0 == m.tx0 && lm.tx1 == m.tx1, "kept in a save");
    }

    static void raidsAndConvoys() throws Exception {
        CityConfig c = cfg(CityConfig.SMALL, 275);
        World w = new World(c);
        w.populate(c);
        // Two bases a few blocks apart: survivors (a), raiders (b), each with armed guards.
        SurvivorBases.Base a = null, b = null;
        java.util.Random r = new java.util.Random(5);
        for (int t = 0; t < 6000 && b == null; t++) {
            City.Building x = w.city.buildings.get(r.nextInt(w.city.buildings.size()));
            if (x.capacity == 0 || !w.city.enterable(x) || x.doorX <= 0) continue;
            if (a != null && (Math.hypot(a.b.doorX - x.doorX, a.b.doorY - x.doorY) < 380 || Math.hypot(a.b.doorX - x.doorX, a.b.doorY - x.doorY) > 650)) continue;
            w.baseFoundedAt = -999;
            SurvivorBases.Base s = w.baseLife.foundBase(x, false);
            if (s == null) continue;
            if (a == null) a = s;
            else b = s;
        }
        check(a != null && b != null, "two bases");
        a.raiders = false;
        b.raiders = true;
        SurvivorBases.Base[] both = {a, b};
        for (SurvivorBases.Base s : both) {
            s.walled = true;
            for (int i = 0; i < 6; i++) {
                Entity e = w.spawn(Entity.CIVILIAN, s.b.doorX + i * 4, s.b.doorY);
                w.armCivilian(e, 60);
                w.baseLife.joinBase(e, s, true);
            }
        }
        a.stash = 100;
        b.raidCd = 9999;
        // A few of the dead a block or two out from the survivors' base, away from the raiders'.
        float dx = a.cx() - b.cx(), dy = a.cy() - b.cy(), l = (float) Math.hypot(dx, dy);
        float[] p = w.city.findWalkable(a.cx() + dx / l * 260, a.cy() + dy / l * 260);
        for (int i = 0; i < 4; i++) w.spawn(Entity.ZOMBIE, p[0] + i * 5, p[1]);
        w.recount();
        w.outbreak = true;
        for (int f = 0; f < 30 * 150 && a.territory == 0; f++) {
            w.update(1 / 30f);
            w.evCount = 0;
        }
        check(a.raids >= 1 && a.territory >= 1, "the survivors went out and cleared a block (raids " + a.raids + ", ground " + a.territory + ")");
        check(w.zombieCount() < 4, "the dead out there were cleared (" + w.zombieCount() + " left)");
        for (int f = 0; f < 30 * 120 && w.baseRaids.raidOf(a) != null; f++) {
            w.update(1 / 30f);
            w.evCount = 0;
        }
        check(w.baseRaids.raidOf(a) == null, "and came home");
        // A supply convoy for the survivors' base: the raiders go for it.
        a.raidCd = 9999;
        b.raidCd = 0;
        int bStash = (int) b.stash;
        check(w.convoys.supply(null, a, null, 120) != null, "a supply convoy sent");
        for (int f = 0; f < 30 * 400 && b.robbed == 0; f++) {
            w.update(1 / 30f);
            w.evCount = 0;
        }
        check(b.robbed >= 1 && b.stash > bStash, "the raiders held up a truck and took its load (" + b.robbed + ", " + bStash + " -> " + (int) b.stash + ")");
        File file = File.createTempFile("zcr", ".dat");
        SaveGame.save(w, file);
        World ld = SaveGame.load(file);
        file.delete();
        SurvivorBases.Base la = null, lb = null;
        for (SurvivorBases.Base s : ld.bases) {
            if (s.b.doorX == a.b.doorX && s.b.doorY == a.b.doorY) la = s;
            if (s.b.doorX == b.b.doorX && s.b.doorY == b.b.doorY) lb = s;
        }
        check(la != null && lb != null && lb.raiders && !la.raiders && la.territory == a.territory && lb.robbed == b.robbed, "kept in a save");
    }
}
