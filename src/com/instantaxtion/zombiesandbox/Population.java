package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Building the city's people: who lives where, the police, the army and everyone else put on the map at the start, and anyone made or kitted out later.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class Population {
    private final World w;

    Population(World w) {
        this.w = w;
    }

    int[] shirts() { return w.city.cfg.variety() ? World.SHIRTS37 : World.SHIRTS; }

    int[] hair() { return w.city.cfg.variety() ? World.HAIR37 : World.HAIR; }

    int[] skins() { return w.city.cfg.variety() ? World.SKINS37 : World.SKINS; }

    /** The Build tool: an edit that's remembered in the city code. */
    boolean build(float x, float y, int kind) {
        int tx = (int) (x / City.T), ty = (int) (y / City.T);
        if (kind == City.ED_HOUSE || kind == City.ED_SHOP) {
            tx -= kind == City.ED_HOUSE ? 1 : 1;
            ty -= 1;
        }
        for (int i = 0, n = w.entities.size(); i < n && kind != City.ED_CLEAR && kind != City.ED_GRASS; i++) {
            Entity e = w.entities.get(i);
            // Not on top of anyone.
            if (Math.abs(e.x - (tx * City.T + 8)) < 12 && Math.abs(e.y - (ty * City.T + 8)) < 12
                    && (kind == City.ED_WALL || kind == City.ED_TREES)) return false;
        }
        int before = w.city.buildings.size();
        byte old = w.city.tiles[Math.max(0, Math.min(w.city.tiles.length - 1, ty * w.city.w + tx))];
        City.Building b = w.city.applyEdit(tx, ty, kind, true);
        boolean changed = b != null || w.city.tiles[Math.max(0, Math.min(w.city.tiles.length - 1, ty * w.city.w + tx))] != old || kind == City.ED_CLEAR;
        if (!changed) return false;
        if (b != null) {
            b.residents = kind == City.ED_HOUSE ? 3 : 0;
            b.capacity = Math.max(b.capacity, 3);
        }
        w.city.cfg.edits.add(new int[]{tx, ty, kind});
        // Old routes may run through what's changed.
        w.city.walkCost = null;
        for (City.Building d : w.doorPaths) d.field = null;
        w.doorPaths.clear();
        w.errandSpots = null;
        return true;
    }

    void populate(CityConfig cfg) {
        City.progress(0.82f, "Moving everyone in");
        scatterWeapons();
        stockArmouries();
        baseStrengths();
        // Every city with a police station has a SWAT team (two on the big maps).
        w.dispatch.swatTeams = w.city.nearestFacility(City.FACILITY_POLICE, 0, 0) == null ? 0 : w.city.w >= 300 ? 2 : 1;
        w.exodus.launchBoats();
        if (w.courtArmoury != null) w.courtArmoury.ammo = 1200 * World.AMMO_MORE;
        w.inmates = 0;
        // ((12.3) Room left for the police and soldiers, and for the dead: with the bigger forces of 12.2 a city
        // could start at its limit, and then no zombie could be put down or get up.)
        int[] force0 = World.forces(cfg, w.city);
        int people = Math.min(World.civiliansFor(cfg, w.city, force0), Math.max(w.maxEntities / 2, w.maxEntities * 4 / 5 - force0[0] - force0[1]));
        // ((17.0) In a gang war or an invasion, room kept for the other side as well: they don't come from the people.)
        w.popLadderNow = cfg.popLadder();
        residents(people);
        straysAndGuns(people);
        // Half the cops start at their precinct, the rest on patrol; soldiers start on base.
        City.Facility base = w.city.mainBase();
        int stations = 0;
        // ((16.9) The FBI field office isn't a precinct: it has its own agents (below).)
        for (City.Facility f : w.city.facilities) if (f.kind == City.FACILITY_POLICE && !f.fed) stations++;
        int[] force = World.forces(cfg, w.city);
        int cops = force[0], soldiers = force[1], satEach = force[2];
        policeOnTheBeat(cfg, stations, cops);
        marksmenAndAgents(cfg);
        mountedAndTraffic(cfg, stations);
        if (w.city.prison != null) populatePrison();
        w.dispatch.riotVans = cfg.nature() ? Math.min(3, stations) : 0;
        w.dispatch.bearcats = cfg.nature() ? Math.min(3, stations) : 0;
        w.dispatch.policeAir = cfg.nature() && stations > 0 ? 2 : 0;
        stationUnits();
        populateWilds(cops > 0);
        w.fleet.stationHelis();
        w.ops.setupBoats();
        w.fleet.stockStations();
        // Out of town: highway patrol on the highway, and (in the USA) sheriff's deputies on the country roads.
        // (About one patrol car to every fifty tiles of highway, four at the least.)
        if (w.city.hwyAxis >= 0) for (int i = 0, n = Math.max(4, (w.city.hwyAxis == 0 ? w.city.w : w.city.h) / 50); i < n; i++) w.fleet.startAgencyPatrol(1);
        if (w.city.country.ruralName != null && w.city.townX0 > 0) for (int i = 0; i < (w.city.w > 300 ? 3 : 2); i++) w.fleet.startAgencyPatrol(2);
        checkpointSoldiers(soldiers);
        int firstSoldier = w.entities.size();
        garrisons(base, soldiers, satEach);
        squadRoles(cfg, base, soldiers, firstSoldier);
        engineersAndMortars(firstSoldier);
        quickReactionForce();
        if (soldiers >= 5) {
            float[] p = base != null ? new float[]{base.x, base.y} : w.city.randomWalkable(w.rnd);
            spawnSoldier(Entity.ROLE_COMMANDER, p[0], p[1]);
        }
        // ((12.3) Trucks enough at each base for the soldiers there.)
        w.fleet.stockBases();
        w.fleet.ensureDozers();
        gateGuards();
        w.war.applyReadiness();
        w.posts.buildRoster();
        w.wilds.spawnBirds();
        w.wilds.spawnWildlife();
        // ((12.5) No civilians at the wheel: the streets carry only the police, the army and the buses and trains.)
        w.fleet.trafficTarget = 0;
        startingOutbreaks(cfg);
        startHikers();
        w.stats.recount();
        // ((13.3) The walking router's clusters all worked out while the city is still loading (a fraction of a second
        // even on the biggest map), not in the first moments of play, when everyone sets off at once.)
        w.city.warmPaths((w.city.w / Nav.CS + 1) * (w.city.h / Nav.CS + 1));
        // (And the ways on foot to the stations and bases: about a second on a Colossal map.)
        while (w.city.warmFacilityField()) {
        }
        outsideHelp();
    }

    /** A few stray dogs, and the few civilians who own a gun. */
    private void straysAndGuns(final int people) {
        // A few strays.
        spawnRandom(Entity.DOG, people / 60);
        // A few civilians own a gun.
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.type == Entity.CIVILIAN && w.rnd.nextFloat() < 0.07f) armCivilian(e, 12 + w.rnd.nextInt(3) * 6);
        }
    }

    /**
     * ((30.8) Map version 50: four officers to a patrol car, not two - every officer on patrol still in a car, in half as
     * many. With the maps shrunk twice and the police not, there were five hundred and more cars on the Default map, one
     * to every seventy tiles of road, and they jammed every street and heaped up at every call.
     */
    int roadCars(int cars) {
        // (Every officer, not five in six: nobody walks a beat now.)
        return w.city.cfg.civic >= 50 ? Math.max(1, (int) Math.ceil(cars / World.PATROL_SHARE / 2)) : cars;
    }

    /** The police: most out in patrol cars, the rest at their precincts. */
    private void policeOnTheBeat(final CityConfig cfg, final int stations, final int cops) {
        // Most officers are out in patrol cars, two to a car ((11.4) about five in six of them); the rest are at their
        // precinct.
        int cars = stations > 0 ? roadCars(Math.max(1, (int) (cops * World.PATROL_SHARE / 2))) : 0;
        w.fleet.patrolTarget = cars;
        int made = 0;
        City.Facility[] precincts = new City.Facility[Math.max(1, stations)];
        int pk = 0;
        for (City.Facility f : w.city.facilities) if (f.kind == City.FACILITY_POLICE && !f.fed) precincts[pk++] = f;
        // ((10.41) A small town's station out in the country runs two cars of its own; the rest are the city's.)
        int town = 0;
        for (City.Facility f : precincts) if (f != null && !f.satellite) town++;
        int ci = 0;
        // ((10.56) Counting who's aboard, not two to a car: a motorcycle has one, and the one short was never made,
        // so a big city had dozens fewer officers than it should.)
        int aboard = 0;
        for (City.Facility f : precincts)
            if (f != null && f.satellite && cfg.civic >= 19)
                for (int k = 0; k < 2 && ci < cars; k++, ci++) {
                    Fleet.Vehicle v = w.fleet.startPatrol(f);
                    if (v != null) {
                        made++;
                        aboard += v.crew.size();
                    }
                }
        for (int i = 0; ci < cars; i++, ci++) {
            City.Facility f = precincts[i % precincts.length];
            if (cfg.civic >= 19 && town > 0) while (f.satellite) f = precincts[++i % precincts.length];
            Fleet.Vehicle v = w.fleet.startPatrol(f);
            if (v != null) {
                made++;
                aboard += v.crew.size();
            }
        }
        int copsAtStations = stations > 0 ? cops - aboard : 0;
        // ((28.0) A country town's station keeps four of its own in (a car's crew seated, two more inside); the rest are at
        // the city's precincts. Each had an equal share before - and the 27.5 cities, a corner or two of them left as
        // country, have half a dozen such towns: a third of the force stood about in the hamlets out in the fields.)
        int sats = stations - town, satEach = cfg.civic >= 40 && town > 0 ? Math.min(4, copsAtStations / stations) : -1;
        int inTown = satEach >= 0 ? copsAtStations - sats * satEach : copsAtStations, shares = satEach >= 0 ? town : stations;
        int k = 0;
        for (City.Facility f : w.city.facilities) {
            if (f.kind != City.FACILITY_POLICE || f.fed) continue;
            int n = satEach >= 0 && f.satellite ? satEach : inTown / shares + (k++ < inTown % shares ? 1 : 0);
            for (int i = 0; i < n; i++) spawn(Entity.COP, f.x + w.rnd.nextFloat() * 40 - 20, f.y + w.rnd.nextFloat() * 40 - 20);
        }
        spawnRandom(Entity.COP, cops - copsAtStations - aboard);
    }

    /** A marksman at each precinct, and the FBI field office's own agents. */
    private void marksmenAndAgents(final CityConfig cfg) {
        // A marksman at each precinct (since 10.10).
        if (cfg.nature())
            for (City.Facility f : w.city.facilities)
                if (f.kind == City.FACILITY_POLICE && !f.fed) spawnCop(Entity.ROLE_MARKSMAN, f.x + w.rnd.nextFloat() * 20 - 10, f.y + w.rnd.nextFloat() * 20 - 10);
        // ((16.9) The FBI field office's own agents, there from the start, with their cars on the lot (see
        // Fleet.stockStations). More of them on the bigger maps.)
        for (City.Facility f : w.city.facilities) {
            if (!f.fed) continue;
            int agents = cfg.v[CityConfig.OPT_SIZE] >= CityConfig.VAST ? 14 : cfg.v[CityConfig.OPT_SIZE] >= CityConfig.COLOSSAL ? 12 : 10;
            for (int i = 0; i < agents; i++) {
                Entity a = spawn(Entity.COP, f.x + w.rnd.nextFloat() * 40 - 20, f.y + w.rnd.nextFloat() * 40 - 20);
                if (a != null) makeAgency(a, 4);
            }
        }
    }

    /** Mounted police out of the main precinct, and traffic officers at the busy junctions. */
    private void mountedAndTraffic(final CityConfig cfg, final int stations) {
        // ((15.0) Mounted police out of the main precinct, and traffic officers at the busy junctions near each one.)
        if (cfg.nature() && stations > 0) {
            City.Facility hq = null;
            for (City.Facility f : w.city.facilities) if (f.kind == City.FACILITY_POLICE && !f.fed && (hq == null || f.r > hq.r)) hq = f;
            int horses = Math.max(2, Math.min(4, stations + 1));
            for (int i = 0; i < horses; i++) {
                Entity m = spawn(Entity.COP, hq.x + w.rnd.nextFloat() * 30 - 15, hq.y + w.rnd.nextFloat() * 30 - 15);
                if (m != null) applyRole(m, Entity.ROLE_MOUNTED);
            }
            postTrafficOfficers();
        }
    }

    /** Soldiers on the country base's checkpoints. */
    private void checkpointSoldiers(final int soldiers) {
        // (10.30) Soldiers on the country base's checkpoints (first: a crowded city could fill up before them).
        if (soldiers > 0)
            for (float[] s : w.city.checkpointPosts) {
                Entity g = spawnSoldier(Entity.ROLE_GUARD, s[0], s[1]);
                if (g == null) continue;
                g.task = Dispatch.T_POST;
                g.postX = g.x;
                g.postY = g.y;
            }
    }

    /** The soldiers on their bases: the town's, and the garrison of the big one out in the country. */
    private void garrisons(final City.Facility base, final int soldiers, final int satEach) {
        if (base != null) {
            for (int i = 0; i < soldiers; i++)
                spawn(Entity.SOLDIER, base.x + w.rnd.nextFloat() * base.r - base.r / 2, base.y + w.rnd.nextFloat() * base.r - base.r / 2);
        } else {
            spawnRandom(Entity.SOLDIER, soldiers);
        }
        // (10.30) The big base out in the country has a garrison of its own, and soldiers on its checkpoints.
        for (City.Facility f : w.city.facilities) {
            if (f.kind != City.FACILITY_BASE || !f.satellite || f == base || soldiers <= 0 || f.qrf) continue;
            int n = satEach;
            for (int i = 0; i < n; i++) spawn(Entity.SOLDIER, f.x + w.rnd.nextFloat() * f.r - f.r / 2, f.y + w.rnd.nextFloat() * f.r - f.r / 2);
        }
    }

    /** ((28.7) The quick reaction force: its twenty soldiers. */
    static final int QRF_SOLDIERS = 20;

    /**
     * ((28.7) The quick reaction force's base gets its twenty, on top of the city's army: five to each of its four vans,
     * standing by in the yard. Riflemen, kitted out like any squad.
     */
    private void quickReactionForce() {
        for (City.Facility f : w.city.facilities) {
            if (f.kind != City.FACILITY_BASE || !f.qrf) continue;
            for (int i = 0; i < QRF_SOLDIERS; i++) {
                Entity e = spawn(Entity.SOLDIER, f.x + w.rnd.nextFloat() * f.r - f.r / 2, f.y + w.rnd.nextFloat() * f.r - f.r / 2);
                if (e != null && w.city.cfg.nature()) w.kitOut(e);
                // (Standing by in the yard, ready: on a post of their own, so nobody else's job takes them.)
                standBy(e, f, i);
            }
        }
    }

    /** Who carries what in each squad: machine gunners, snipers, corpsmen. */
    private void squadRoles(final CityConfig cfg, final City.Facility base, final int soldiers, final int firstSoldier) {
        // Every fourth soldier carries the squad's machine gun, big garrisons have snipers, and a
        // commander runs the show.
        int snipers = soldiers >= 10 ? 2 : 0;
        for (int i = firstSoldier, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.type != Entity.SOLDIER) continue;
            if (base != null && base.baseType == City.BT_SPECIAL) {
                // Special forces: more rounds and a grenade or two more each.
                e.reserve += e.reserve / 2;
                e.grenades += 1;
            }
            if (e.member == 4) applyRole(e, Entity.ROLE_GUNNER);
            else if (e.member == 2 && cfg.nature()) kitOut(e);
            else if (e.member == 3 && snipers > 0) {
                applyRole(e, Entity.ROLE_SNIPER);
                snipers--;
            } else if (e.member == 3 && cfg.nature()) applyRole(e, Entity.ROLE_CORPSMAN);
        }
    }

    /** Each base's combat engineers and its mortar teams in their pits. */
    private void engineersAndMortars(final int firstSoldier) {
        // ((14.0) Two riflemen at each base are its combat engineers.) ((28.7) Not the quick reaction force's.)
        for (City.Facility f : w.city.facilities) {
            if (f.kind != City.FACILITY_BASE || f.qrf) continue;
            int engineers = 0;
            for (int i = firstSoldier, n = w.entities.size(); i < n && engineers < 2; i++) {
                Entity e = w.entities.get(i);
                if (e.type != Entity.SOLDIER || e.role != Entity.ROLE_RIFLE || World.hypot(e.x - f.x, e.y - f.y) > f.r + 300) continue;
                applyRole(e, Entity.ROLE_ENGINEER);
                engineers++;
            }
            // ((14.0) And mortars. (14.9) Two or three of them, each dug into a sandbagged pit in the base and kept there,
            // not sent out with the squads: the one there was left with the riflemen, went off on the first truck and
            // never fired.)
            int here = 0;
            for (int i = firstSoldier, n = w.entities.size(); i < n; i++) {
                Entity e = w.entities.get(i);
                if (e.type == Entity.SOLDIER && World.hypot(e.x - f.x, e.y - f.y) < f.r + 300) here++;
            }
            int teams = Math.max(1, Math.min(3, 1 + here / 80));
            float toTown = (float) Math.atan2(w.city.worldH() / 2f - f.y, w.city.worldW() / 2f - f.x);
            for (int i = firstSoldier, n = w.entities.size(), dug = 0; i < n && dug < teams; i++) {
                Entity e = w.entities.get(i);
                if (e.type != Entity.SOLDIER || e.role != Entity.ROLE_RIFLE || World.hypot(e.x - f.x, e.y - f.y) > f.r + 300) continue;
                float a = toTown + (dug - (teams - 1) / 2f) * 0.45f;
                // (Out towards the edge on the town side, away from the crowd round the middle of the base.)
                float[] pit = w.city.findWalkable(f.x + (float) Math.cos(a) * f.r * 0.8f, f.y + (float) Math.sin(a) * f.r * 0.8f);
                if (pit == null) continue;
                applyRole(e, Entity.ROLE_MORTAR);
                e.x = pit[0];
                e.y = pit[1];
                e.task = Dispatch.T_POST;
                e.postX = pit[0];
                e.postY = pit[1];
                dug++;
            }
        }
    }

    /** Guards on the base gates and at the armory. */
    private void gateGuards() {
        // Guards on the base gates. ((11.3) Not the precinct doors: every officer is out on the beat.)
        for (City.Facility f : w.city.facilities) {
            int type = f.kind == City.FACILITY_BASE ? Entity.SOLDIER : -1;
            if (type < 0) continue;
            for (float[] post : f.posts) {
                Entity best = null;
                float bd = Float.MAX_VALUE;
                for (int i = 0, n = w.entities.size(); i < n; i++) {
                    Entity e = w.entities.get(i);
                    if (e.type != type || e.task != Dispatch.T_NONE) continue;
                    float d = (e.x - post[0]) * (e.x - post[0]) + (e.y - post[1]) * (e.y - post[1]);
                    if (d < bd && d < 400 * 400) {
                        bd = d;
                        best = e;
                    }
                }
                if (best == null) break;
                best.task = Dispatch.T_POST;
                best.postX = post[0];
                best.postY = post[1];
            }
        }
        // Guardsmen on duty at the armory: two on the gate, two at the drill hall door.
        if (w.city.armory != null && w.city.armoryGate != null) {
            float[][] spots = {{w.city.armoryGate[0] - 14, w.city.armoryGate[1]}, {w.city.armoryGate[0] + 14, w.city.armoryGate[1]},
                    {w.city.armory.doorX - 12, w.city.armory.doorY + 6}, {w.city.armory.doorX + 12, w.city.armory.doorY + 6}};
            for (float[] s : spots) {
                Entity g = spawnSoldier(Entity.ROLE_GUARD, s[0], s[1]);
                if (g == null) continue;
                g.task = Dispatch.T_POST;
                g.postX = g.x;
                g.postY = g.y;
            }
        }
    }

    /** The dead the game starts with, in a few small outbreaks. */
    private void startingOutbreaks(final CityConfig cfg) {
        // Zombies start in a few small outbreaks rather than spread evenly. ((17.0) Or, in a gang war or an invasion,
        // the gangs on their turf or the enemy's first squads coming in from the edge.)
        int left = cfg.zombies();
        while (left > 0) {
            // Some outbreaks start indoors: people sheltering inside, one of them bitten.
            if (left >= 3 && w.rnd.nextFloat() < 0.3f) {
                City.Building b = w.city.buildings.get(w.rnd.nextInt(w.city.buildings.size()));
                if (b.capacity > 0 && b.occupants.isEmpty()) {
                    w.death.indoorOutbreak(b, 2 + w.rnd.nextInt(3));
                    left -= 2;
                    continue;
                }
            }
            float[] p = w.city.randomWalkable(w.rnd);
            int group = Math.min(left, 1 + w.rnd.nextInt(6));
            for (int i = 0; i < group; i++)
                spawn(Entity.ZOMBIE, p[0] + w.rnd.nextFloat() * 30 - 15, p[1] + w.rnd.nextFloat() * 30 - 15);
            left -= group;
        }
    }

    /** How much help can come from outside. */
    private void outsideHelp() {
        // ((13.3) Help from outside: three quarters as many again as the police and soldiers the city starts with.)
        if (w.dispatch.policeReserve > 0 || w.dispatch.squadReserve > 0) {
            w.dispatch.policeReserve = Math.round(w.counts[Entity.COP] * Dispatch.OUTSIDE_SHARE);
            // ((26.1) Never next to none: a map with no base of its own (the Space Center, Theme Park Island) had three
            // soldiers coming from outside, or none. The army sends a proper force wherever the city is.)
            int floor = Math.max(150, Math.round(w.counts[Entity.COP] * 0.25f));
            w.dispatch.squadReserve = Math.max(floor, Math.round(w.counts[Entity.SOLDIER] * Dispatch.OUTSIDE_SHARE));
        }
    }

    /**
     * The game opens with people already out on the trails (since 10.18): spread along them, some on the
     * way out, some on the way back, rather than all setting off from home at once.
     */
    void startHikers() {
        if (w.city.hikes.isEmpty()) return;
        int want = Math.min(48, w.city.hikes.size() * 3 + 4);
        ArrayList<Entity> pool = new ArrayList<Entity>();
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || e.type != Entity.CIVILIAN || e.leader != null || e.rig != null || e.home == null || e.aloft
                    || e.job == Entity.J_LOOKOUT || e.job == Entity.J_RELIEF) continue;
            pool.add(e);
        }
        java.util.Collections.shuffle(pool, w.rnd);
        int made = 0;
        for (int i = 0; i < pool.size() && made < want; i++) {
            Entity e = pool.get(i);
            float[] h = w.city.hikes.get(w.rnd.nextInt(w.city.hikes.size()));
            int n = h.length / 2;
            if (n < 2) continue;
            int k = w.rnd.nextInt(n);
            float[] p = w.city.findWalkable(h[k * 2], h[k * 2 + 1]);
            if (p == null || World.hypot(p[0] - h[k * 2], p[1] - h[k * 2 + 1]) > 20) continue;
            e.job = Entity.J_HIKER;
            e.hike = h;
            e.hikeIdx = k;
            e.hikeDir = w.rnd.nextBoolean() ? 1 : -1;
            e.jobStep = 1;
            e.jobTimer = 0;
            e.x = p[0];
            e.y = p[1];
            e.markX = -1;
            made++;
            // (Often with a friend or two.)
            for (int f = 0, mates = w.rnd.nextFloat() < 0.45f ? 1 + w.rnd.nextInt(2) : 0; f < mates && i + 1 < pool.size() && made < want; f++) {
                Entity o = pool.get(++i);
                o.job = Entity.J_HIKER;
                o.hike = h;
                o.hikeIdx = k;
                o.hikeDir = e.hikeDir;
                o.jobStep = 1;
                o.jobTimer = 0;
                o.x = p[0] + w.rnd.nextFloat() * 8 - 4;
                o.y = p[1] + w.rnd.nextFloat() * 8 - 4;
                o.markX = -1;
                made++;
            }
        }
    }

    Entity follower(Entity leader, int type) {
        Entity f = spawn(type, leader.x + w.rnd.nextFloat() * 10 - 5, leader.y + w.rnd.nextFloat() * 10 - 5);
        if (f != null) f.leader = leader;
        return f;
    }

    void residents(int people) {
        ArrayList<City.Building> homes = new ArrayList<City.Building>();
        for (City.Building b : w.city.buildings) if (b.residents > 0) homes.add(b);
        java.util.Collections.shuffle(homes, w.rnd);
        int left = people;
        // Take a share of every household rather than whole households, so the whole city is lived in.
        float share = w.city.totalResidents > 0 ? Math.min(1f, people / (float) w.city.totalResidents) : 0;
        // ((22.4) On map version 36 the people don't depend on the homes: everyone is found one, a few more to each.)
        int passes = w.popLadderNow ? 60 : 3;
        for (int pass = 0; pass < passes && left > 0; pass++) {
            for (int h = 0; h < homes.size() && left > 0; h++) {
                City.Building b = homes.get(h);
                int n = pass == 0 ? Math.min(left, (int) (b.residents * share + w.rnd.nextFloat())) : Math.min(left, 1);
                if (n <= 0) continue;
                // The people living here, family by family (a flat or a house holds 1 to 5).
                while (n > 0) {
                    int size = Math.min(n, 1 + w.rnd.nextInt(b.kind == City.HOUSE ? 5 : 4));
                    n -= size;
                    boolean together = size >= 2 && w.rnd.nextFloat() < 0.3f;
                    boolean out = w.rnd.nextFloat() < 0.4f;
                    // ((10.56) Those out are out on the streets, not dotted about the fields.)
                    float[] p = out ? w.city.randomStreetSpot(w.rnd) : new float[]{b.doorX + w.rnd.nextFloat() * 16 - 8, b.doorY + w.rnd.nextFloat() * 16 - 8};
                    Entity leader = null;
                    for (int k = 0; k < size; k++) {
                        Entity e;
                        if (together && leader != null) e = follower(leader, Entity.CIVILIAN);
                        else {
                            float[] q = k == 0 || together ? p : w.rnd.nextFloat() < 0.4f ? w.city.randomStreetSpot(w.rnd)
                                    : new float[]{b.doorX + w.rnd.nextFloat() * 16 - 8, b.doorY + w.rnd.nextFloat() * 16 - 8};
                            e = spawn(Entity.CIVILIAN, q[0], q[1]);
                        }
                        left--;
                        if (e == null) continue;
                        e.home = b;
                        e.homeChecked = true;
                        if (leader == null) leader = e;
                    }
                    if (leader != null && w.rnd.nextFloat() < 0.1f) follower(leader, Entity.DOG);
                }
            }
        }
        // Everyone gets a life. Many start the day at work or at home, indoors; the rest are out doing
        // their thing, and those indoors come and go over the next few minutes.
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || e.type != Entity.CIVILIAN) continue;
            w.dailyLife.assignJob(e);
            if (e.leader != null) continue;
            boolean worker = e.job == Entity.J_WORKER || e.job == Entity.J_SHOPKEEPER || e.job == Entity.J_STUDENT;
            if (worker && e.work != null && w.rnd.nextFloat() < 0.55f) {
                e.x = e.work.doorX;
                e.y = e.work.doorY;
                e.lastErrand = e.work;
                w.indoors.goInside(e, e.work, 10 + w.rnd.nextFloat() * (e.job == Entity.J_SHOPKEEPER ? 400 : 260));
                continue;
            }
            City.Building b = e.home;
            if (b == null) continue;
            boolean near = (e.x - b.doorX) * (e.x - b.doorX) + (e.y - b.doorY) * (e.y - b.doorY) <= 20 * 20;
            if (e.job == Entity.J_HOMEBODY && w.rnd.nextFloat() < 0.8f) near = true;
            if (!near) continue;
            e.x = b.doorX;
            e.y = b.doorY;
            e.lastErrand = b;
            w.indoors.goInside(e, b, 5 + w.rnd.nextFloat() * (e.job == Entity.J_HOMEBODY ? 500 : 240));
        }
        // A city with nowhere to live (or more people than homes): the rest find a place nearby.
        if (left > 0) spawnRandom(Entity.CIVILIAN, left);
    }

    void armCivilian(Entity e, int rounds) {
        e.hasGun = true;
        e.magSize = 6;
        e.ammo = Math.min(6, rounds);
        e.reserve = rounds - e.ammo;
    }

    void spawnRandom(int type, int n) {
        for (int i = 0; i < n; i++) {
            // Where there's countryside, most people are in town. ((10.56) People and officers on the streets;
            // only a stray dog might be out in a field.)
            float[] p = type == Entity.CIVILIAN || type == Entity.COP ? w.city.randomStreetSpot(w.rnd)
                    : w.rnd.nextFloat() < 0.85f ? w.city.randomWalkableInTown(w.rnd) : w.city.randomWalkable(w.rnd);
            spawn(type, p[0], p[1]);
        }
    }

    Entity spawn(int type, float x, float y) {
        // (No animals since 10.18: no dogs, and the dead that were dogs come as runners instead.)
        if (type == Entity.DOG && !w.k9Spawning) return null;
        if (type == Entity.ZOMBIE_DOG) type = Entity.RUNNER;
        type = Entity.current(type);
        // (Help arriving from outside always gets out of its vehicles: the cap stretches for them.)
        if (w.entities.size() >= w.maxEntities + (w.arriving ? World.OVER_CAP : 0)) return null;
        float[] p = w.city.findWalkable(x, y);
        if (p == null) return null;
        Entity e = make(type, p[0], p[1], -1, 0);
        w.entities.add(e);
        // Counted straight away, so the numbers go up even while the game is paused.
        w.counts[type]++;
        if (e.isUndead() && w.patientZero == null) w.death.markPatientZero(e);
        w.fieldTimer = 0;
        return e;
    }

    /** A zombie to draw (the dead fighting inside a building), never part of the world. */
    Entity puppetZombie(int i) {
        Entity e = make(Entity.ZOMBIE, 0, 0, -1, 0);
        e.nameSeed = i * 7919 + 17;
        return e;
    }

    Entity make(int type, float x, float y, int origin, int originBody) {
        type = Entity.current(type);
        Entity e = new Entity();
        e.type = type;
        e.x = x;
        e.y = y;
        e.angle = w.rnd.nextFloat() * World.TAU;
        e.wanderAngle = e.angle;
        e.phase = w.rnd.nextFloat() * 10;
        e.origin = origin;
        e.nameSeed = w.rnd.nextInt();
        e.skin = skins()[w.rnd.nextInt(skins().length)];
        e.risen = w.time;
        if (type == Entity.COP) e.callsign = 11 + w.dispatch.copCount++;
        if (type == Entity.SOLDIER) {
            e.squad = w.dispatch.soldierCount / 4;
            e.member = w.dispatch.soldierCount % 4 + 1;
            w.dispatch.soldierCount++;
        }
        switch (type) {
            case Entity.CIVILIAN:
                e.hp = 30;
                e.radius = 3.6f;
                e.speed = 15 + w.rnd.nextFloat() * 6;
                e.runSpeed = 36 + w.rnd.nextFloat() * 10;
                e.body = shirts()[w.rnd.nextInt(shirts().length)];
                e.head = hair()[w.rnd.nextInt(hair().length)];
                // ((26.0) Hunters keep a rifle or a crossbow; ex-soldiers kept their service rifle.)
                int tr = e.trade();
                if (tr == Entity.TR_HUNTER) w.combat.setGun(e, (e.nameSeed & 1) == 0 ? Entity.W_HUNTING : Entity.W_CROSSBOW, 21);
                else if (tr == Entity.TR_VETERAN) w.combat.setGun(e, Entity.W_RIFLE, 90);
                break;
            case Entity.COP:
                // ((31.0) Map version 51: body armour under the uniform - a fifth tougher.)
                // ((32.3) Map version 56: heavier armour for holding a line - 100.)
                e.hp = w.city.cfg.civic >= 56 ? 100 : w.city.cfg.civic >= 51 ? 80 : 60;
                e.radius = 3.8f;
                e.speed = 22;
                e.runSpeed = 40;
                // A 15-round pistol and ((27.5) four) spare magazines: a duty belt's worth (three ran dry halfway through a fight
                // with a dozen of the dead).
                e.magSize = 15;
                e.reserve = 60 * World.AMMO_MORE;
                e.body = 0xFF23408E;
                e.head = 0xFF141C38;
                break;
            case Entity.SOLDIER:
                e.hp = 100;
                e.radius = 4.0f;
                e.speed = 24;
                e.runSpeed = 40;
                // A standard load: seven 30-round magazines.
                e.magSize = 30;
                e.reserve = 180 * World.AMMO_MORE;
                e.grenades = 2;
                e.body = 0xFF55623A;
                e.head = 0xFF3C4628;
                e.mass = 1.3f;
                break;
            case Entity.DOG:
                e.hp = 25;
                e.radius = 2.8f;
                e.speed = 18 + w.rnd.nextFloat() * 6;
                e.runSpeed = 55 + w.rnd.nextFloat() * 8;
                e.body = World.DOGS[w.rnd.nextInt(World.DOGS.length)];
                e.head = City.darken(e.body, 0.8f);
                e.skin = e.body;
                break;
            case Entity.ZOMBIE_DOG:
                e.hp = 22;
                e.radius = 2.8f;
                e.speed = 28;
                e.runSpeed = 60 + w.rnd.nextFloat() * 6;
                e.skin = 0xFF7A8058;
                break;
            case Entity.ZOMBIE:
                // (Tougher since 10.28: it takes a few hits, or one to the head.)
                // ((12.3) Less health again: 90, from 130 in 12.1 and 95 before that. (12.6) Slower: a 12 to 16
                // shamble and a 24 to 29 run, slower than anyone running from them (16 to 22 and 32 to 38 before).)
                e.hp = 90;
                e.radius = 3.8f;
                e.speed = 12 + w.rnd.nextFloat() * 4;
                e.runSpeed = 24 + w.rnd.nextFloat() * 5;
                e.skin = w.rnd.nextBoolean() ? 0xFF7C9A5E : 0xFF8FA173;
                break;
            case Entity.RUNNER:
                e.hp = 28;
                e.radius = 3.5f;
                // ((12.7) Half as fast again as an ordinary zombie, and no more: 33 to 39 and 56 to 62 before, nearly
                // twice as fast, and they ran down anyone.)
                e.speed = (12 + w.rnd.nextFloat() * 4) * World.RUNNER_PACE;
                e.runSpeed = (24 + w.rnd.nextFloat() * 5) * World.RUNNER_PACE;
                e.skin = 0xFF9DAA70;
                break;
            case Entity.BRUISER:
                // ((22.6) Between a runner and a brute: quicker than the shambling dead, tougher, a heavy bite and a
                // short lunge.)
                e.hp = 115;
                e.radius = 4.7f;
                e.speed = (12 + w.rnd.nextFloat() * 4) * 1.25f;
                e.runSpeed = (24 + w.rnd.nextFloat() * 5) * 1.25f;
                e.skin = 0xFF829858;
                e.mass = 1.9f;
                break;
            case Entity.ARMOURED:
                // ((27.0) A police officer or soldier come back in what they wore: body armour (most of a body shot
                // stopped) and a helmet that takes the first shot to the head.)
                e.hp = 150;
                e.radius = 4.0f;
                e.speed = 11 + w.rnd.nextFloat() * 3;
                e.runSpeed = 22 + w.rnd.nextFloat() * 4;
                e.skin = w.rnd.nextBoolean() ? 0xFF7C9A5E : 0xFF8FA173;
                e.mass = 1.5f;
                e.armored = true;
                e.helmet = true;
                break;
            case Entity.GIANT:
                // ((25.0) Only ever put down by hand: huge, slow, and it takes a squad's sustained fire, a tank or
                // explosives to bring down (no lucky shot to the head does it).)
                e.hp = World.GIANT_HP;
                e.radius = 6.8f;
                e.speed = 8 + w.rnd.nextFloat();
                e.runSpeed = 11;
                e.skin = 0xFF5C7448;
                e.mass = 14f;
                break;
            default:
                // (Toned down in 10.50: tough, not a tank.)
                e.hp = 170;
                e.radius = 6.2f;
                e.speed = 12;
                e.runSpeed = 18;
                e.skin = 0xFF6F8D55;
                e.mass = 3f;
                break;
        }
        if (e.isUndead()) {
            int base = originBody != 0 ? originBody
                    : type == Entity.BRUTE ? 0xFF4D3F4F : type == Entity.GIANT ? 0xFF3A3036 : type == Entity.BRUISER ? 0xFF6A3A34 : type == Entity.ZOMBIE_DOG ? World.DOGS[w.rnd.nextInt(World.DOGS.length)]
                    // ((27.0) The armoured dead: police blue or army olive. A screamer: a pale rag.)
                    : type == Entity.ARMOURED ? (origin == Entity.SOLDIER || (origin != Entity.COP && w.rnd.nextBoolean()) ? 0xFF55623A : 0xFF23408E)
                    : type == Entity.SCREAMER ? 0xFFB9B4A6
                    : shirts()[w.rnd.nextInt(shirts().length)];
            e.body = World.mix(base, 0xFF2F3A26, type == Entity.BRUTE || type == Entity.BRUISER || type == Entity.GIANT || type == Entity.ARMOURED ? 0.2f : 0.45f);
            // ((12.4) This strain's own look.)
            int tint = w.war.strainTint();
            if (tint != 0 && type != Entity.ZOMBIE_DOG) e.skin = World.mix(e.skin, tint, 0.45f);
            e.head = type == Entity.ZOMBIE_DOG ? City.darken(e.body, 0.8f) : e.skin;
            // ((27.0) The helmet, until it's shot off.)
            if (type == Entity.ARMOURED) e.head = City.darken(e.body, 0.62f);
            // The longer the outbreak goes on, the tougher new zombies get.
            if (w.mutation > 0) {
                e.hp *= 1 + 0.12f * w.mutation;
                e.speed *= 1 + 0.05f * w.mutation;
                e.runSpeed *= 1 + 0.05f * w.mutation;
            }
            // The strain.
            if (w.trait[World.TR_TOUGH]) e.hp *= 1.35f;
            if (w.trait[World.TR_FRAIL]) e.hp *= 0.75f;
            if (w.trait[World.TR_SWIFT]) {
                e.speed *= 1.12f;
                e.runSpeed *= 1.12f;
            }
        }
        if (type == Entity.COP || type == Entity.SOLDIER) e.hp *= World.ARMED_HP;
        // ((20.0) The dead are tougher on the harder settings.)
        if (Entity.isZombieType(type)) e.hp *= Director.Z_HP[w.difficulty];
        e.maxHp = e.hp;
        e.ammo = e.magSize;
        // ((11.4) Not every patrol officer carries the same: some have the shotgun from the car, some the patrol carbine.)
        if (type == Entity.COP) {
            int pick = (e.nameSeed >>> 3) % 8;
            if (pick == 0) w.combat.issue(e, Entity.W_SHOTGUN);
            else if (pick == 1) w.combat.issue(e, Entity.W_CARBINE);
        }
        e.flank = (w.rnd.nextFloat() - 0.5f) * 1.8f;
        return e;
    }

    /** Spawns a police officer: a regular cop, a riot officer with a shield, or a K9 handler with a dog. */
    Entity spawnCop(int role, float x, float y) {
        // ((22.0) A motorcycle officer: on the bike, out on patrol from the nearest road.)
        if (role == -4) return w.fleet.startBikeAt(x, y);
        Entity e = spawn(Entity.COP, x, y);
        if (e == null) return null;
        if (role == Entity.ROLE_RIOT) {
            applyRole(e, Entity.ROLE_RIOT);
        } else if (role == Entity.ROLE_SWAT || role == Entity.ROLE_MARKSMAN) {
            applyRole(e, role);
        } else if (role == Entity.ROLE_SAR) {
            applyRole(e, Entity.ROLE_SWAT);
            e.role = Entity.ROLE_SAR;
            e.body = 0xFFE06A20;
        } else if (role < 0) {
            // Highway patrol (-1), a sheriff's deputy (-2) or a park ranger (-3), on foot.
            if (role == -3) makeRanger(e);
            else {
                e.agency = -role;
                e.body = role == -1 || w.city.country.ruralShirt == 0 ? w.city.country.hpShirt : w.city.country.ruralShirt;
            }
        } else if (role == Entity.ROLE_K9) {
            // ((22.0) Back again: the only animals in the city now are the police dogs.)
            e.role = Entity.ROLE_K9;
            w.k9Spawning = true;
            Entity dog = spawn(Entity.DOG, x + 6, y + 4);
            w.k9Spawning = false;
            if (dog != null) {
                dog.leader = e;
                dog.body = 0xFF8B5A2B;
                dog.head = 0xFF2B2320;
                dog.hp = dog.maxHp = 45;
                dog.role = Entity.ROLE_K9;
            }
        }
        return e;
    }

    /**
     * A squad's second soldier is its grenadier; its fourth carries the machine gun. (Reserve squads arriving by
     * truck are made up the same way.) ((13.9) And its third is the combat medic again: gone in 10.50, back now.)
     */
    void kitOut(Entity e) {
        if (e.type != Entity.SOLDIER || e.role != Entity.ROLE_RIFLE) return;
        if (e.member == 4) applyRole(e, Entity.ROLE_GUNNER);
        else if (e.member == 3) applyRole(e, Entity.ROLE_CORPSMAN);
        else if (e.member == 2) applyRole(e, Entity.ROLE_GRENADIER);
    }

    /** Spawns a soldier with a particular job. */
    /**
     * (12.9) The special units, there from the start: each SWAT team and BearCat crew (six, the team's marksman among
     * them), each riot squad (six with shields) standing by at a police station, and two pairs of scouts at each army
     * base. They stay on their posts there until they're sent, and go out in their own vehicles - nobody appears from
     * nowhere when one is called.
     */
    void stationUnits() {
        ArrayList<City.Facility> pol = new ArrayList<City.Facility>(), bases = new ArrayList<City.Facility>();
        for (City.Facility f : w.city.facilities) {
            if (f.fallen) continue;
            if (f.kind == City.FACILITY_POLICE) pol.add(f);
            else if (f.kind == City.FACILITY_BASE && !f.qrf) bases.add(f);
        }
        // ((28.0) The city's precincts first: the country towns' stations came first in the list, and the SWAT teams and
        // riot squads all stood by out at the hamlets.)
        if (w.city.cfg.civic >= 40) {
            ArrayList<City.Facility> sorted = new ArrayList<City.Facility>();
            for (City.Facility f : pol) if (!f.satellite && !f.fed) sorted.add(f);
            for (City.Facility f : pol) if (f.satellite || f.fed) sorted.add(f);
            pol = sorted;
        }
        if (!pol.isEmpty()) {
            int have = 0, riot = 0;
            for (City.Facility f : pol) {
                have += w.fleet.standby(f, Entity.COP, Entity.ROLE_SWAT, 999).size();
                riot += w.fleet.standby(f, Entity.COP, Entity.ROLE_RIOT, 999).size();
            }
            int tactical = (w.dispatch.swatTeams + w.dispatch.bearcats) * 6;
            for (int k = have; k < tactical; k++) {
                City.Facility f = pol.get(k / 6 % pol.size());
                standBy(spawnCop(k % 6 == 5 && w.city.cfg.nature() ? Entity.ROLE_MARKSMAN : Entity.ROLE_SWAT, f.x, f.y), f, k);
            }
            for (int k = riot; k < w.dispatch.riotVans * 6; k++) {
                City.Facility f = pol.get((k / 6 + 1) % pol.size());
                standBy(spawnCop(Entity.ROLE_RIOT, f.x, f.y), f, k + 3);
            }
        }
        if (w.city.cfg.nature())
            for (City.Facility b : bases)
                for (int k = w.fleet.standby(b, Entity.SOLDIER, Entity.ROLE_RECON, 4).size(); k < 4; k++)
                    standBy(spawnSoldier(Entity.ROLE_RECON, b.x, b.y), b, k);
    }

    /** (12.9) On a post of their own in the station's or base's yard, waiting to be sent. */
    void standBy(Entity e, City.Facility f, int k) {
        standBy(e, f, k, true);
    }

    /** ((28.7) As above; put there (at the start), or, not, walking to it (back from a job). */
    void standBy(Entity e, City.Facility f, int k, boolean put) {
        if (e == null) return;
        float a = k * 2.39996f, r = Math.min(f.r * 0.6f, 14 + (k % 6) * 4);
        float[] p = w.city.findWalkable(f.x + (float) Math.cos(a) * r, f.y + (float) Math.sin(a) * r);
        if (p == null || World.hypot(p[0] - f.x, p[1] - f.y) > f.r + 100) p = new float[]{e.x, e.y};
        if (put) {
            e.x = p[0];
            e.y = p[1];
        }
        e.task = Dispatch.T_POST;
        e.postX = p[0];
        e.postY = p[1];
    }

    /**
     * ((22.0) A ready-made group, put down together round (x, y) in one tap: soldiers (police if cops) by role, the
     * soldiers all one squad.
     */
    ArrayList<Entity> spawnGroup(int[] roles, boolean cops, float x, float y) {
        ArrayList<Entity> out = new ArrayList<Entity>();
        int squad = -1;
        for (int i = 0; i < roles.length; i++) {
            double a = i * 2.4, r = 6 + i * 2.2;
            float px = x + (float) (Math.cos(a) * r), py = y + (float) (Math.sin(a) * r);
            Entity e = cops ? spawnCop(roles[i], px, py) : spawnSoldier(roles[i], px, py);
            if (e == null) continue;
            if (!cops && roles[i] != Entity.ROLE_COMMANDER) {
                if (squad < 0) squad = e.squad;
                e.squad = squad;
                e.member = out.size() + 1;
            }
            out.add(e);
        }
        return out;
    }

    Entity spawnSoldier(int role, float x, float y) {
        Entity e = spawn(Entity.SOLDIER, x, y);
        if (e == null) return null;
        applyRole(e, role);
        if (role == Entity.ROLE_COMMANDER) {
            // The commander isn't part of a squad.
            w.dispatch.soldierCount--;
            e.squad = -1;
        }
        return e;
    }

    /** Commanders carry a pistol and a radio, snipers a scoped rifle, gunners a belt-fed machine gun. */
    void applyRole(Entity e, int role) {
        e.role = role;
        switch (role) {
            case Entity.ROLE_MOUNTED:
                // ((15.0) On horseback: quicker, harder to bring down (the horse takes some of it), and a crowd
                // gives way to a horse rather than the other way round.)
                e.hp = e.maxHp = 160;
                e.speed *= 1.45f;
                e.runSpeed *= 1.55f;
                e.mass = 3.5f;
                break;
            case Entity.ROLE_TRAFFIC:
                break;
            case Entity.ROLE_VOLUNTEER:
                // ((27.5) A civilian signed up: no armour, fewer rounds, no grenades. Police auxiliaries in a hi-vis vest,
                // army volunteers in plain khaki with no helmet.)
                e.armored = false;
                e.grenades = 0;
                if (e.type == Entity.COP) {
                    e.hp = e.maxHp = 50;
                    e.reserve = 30 * World.AMMO_MORE;
                    e.body = 0xFFD9E021;
                    e.head = 0xFF2A2C30;
                } else {
                    e.hp = e.maxHp = 80;
                    e.reserve = 90 * World.AMMO_MORE;
                    e.body = 0xFF8C8A5E;
                    e.head = 0xFF5A4A3A;
                }
                break;
            case Entity.ROLE_MORTAR:
                // ((14.0) A carbine for close in, and the tube and its rounds on their back.)
                e.hp = e.maxHp = 100;
                e.magSize = 30;
                e.reserve = 90 * World.AMMO_MORE;
                e.grenades = World.MORTAR_ROUNDS;
                break;
            case Entity.ROLE_CORPSMAN:
                // ((13.9) A rifle, fewer spare rounds, no grenades: the bag is full of dressings.)
                e.hp = e.maxHp = 100;
                e.magSize = 30;
                e.reserve = 120 * World.AMMO_MORE;
                e.grenades = 0;
                break;
            case Entity.ROLE_RIOT:
                // A shield, a helmet and pads: slower, but bites from the front glance off.
                e.hp = e.maxHp = 90;
                e.speed = 18;
                e.runSpeed = 32;
                e.mass = 1.4f;
                break;
            case Entity.ROLE_GRENADIER:
                // A rifle with a grenade launcher slung under it, and a bandolier of rounds for it.
                e.hp = e.maxHp = 100;
                e.magSize = 30;
                e.reserve = 150 * World.AMMO_MORE;
                e.grenades = 4;
                break;
            case Entity.ROLE_MARKSMAN:
                // A scoped bolt-action rifle and a handful of rounds.
                e.hp = e.maxHp = 70;
                e.magSize = 5;
                e.reserve = 40 * World.AMMO_MORE;
                e.body = 0xFF2A3446;
                e.head = 0xFF1A2230;
                break;
            case Entity.ROLE_MARINE:
                // ((11.0) Marines, landed from the sea: well armed, in blue-grey camouflage.)
                e.hp = e.maxHp = 110;
                e.magSize = 30;
                e.reserve = 210 * World.AMMO_MORE;
                e.grenades = 1;
                e.body = 0xFF3E4E62;
                e.head = 0xFF2E3A4A;
                break;
            case Entity.ROLE_RECON:
                // ((11.0) Recon scouts: light kit, quick on their feet, in tan.)
                e.hp = e.maxHp = 90;
                e.magSize = 30;
                e.reserve = 150 * World.AMMO_MORE;
                e.runSpeed *= 1.1f;
                e.body = 0xFF8A7C56;
                e.head = 0xFF6E6244;
                break;
            case Entity.ROLE_SWAT:
                // Body armour, a helmet and a carbine.
                e.hp = e.maxHp = 130;
                e.magSize = 30;
                e.reserve = 150 * World.AMMO_MORE;
                e.grenades = 0;
                e.mass = 1.3f;
                e.body = 0xFF1E2228;
                e.head = 0xFF2C3038;
                break;
            case Entity.ROLE_GUARD:
                // Guardsmen: rifles and a little less training, in tan uniforms.
                e.hp = e.maxHp = 75;
                e.magSize = 30;
                e.reserve = 120 * World.AMMO_MORE;
                e.grenades = 1;
                e.body = 0xFF8C8260;
                e.head = 0xFF6A6248;
                break;
            case Entity.ROLE_COMMANDER:
                e.hp = e.maxHp = 130;
                e.magSize = 15;
                e.reserve = 60 * World.AMMO_MORE;
                e.grenades = 0;
                e.body = 0xFF4A5634;
                e.head = 0xFF8E1F1F;
                break;
            case Entity.ROLE_SNIPER:
                e.hp = e.maxHp = 80;
                e.magSize = 5;
                e.reserve = 45 * World.AMMO_MORE;
                e.grenades = 0;
                e.body = 0xFF434B2C;
                e.head = 0xFF363D22;
                break;
            case Entity.ROLE_SF:
                // ((26.0) Special forces: plate carriers and helmets in black, suppressed rifles, a couple of grenades.)
                e.hp = e.maxHp = 170;
                e.magSize = 30;
                e.reserve = 270 * World.AMMO_MORE;
                e.grenades = 2;
                e.mass = 1.3f;
                e.runSpeed *= 1.08f;
                e.body = 0xFF22262A;
                e.head = 0xFF121416;
                break;
            case Entity.ROLE_GUNNER:
                e.hp = e.maxHp = 130;
                e.magSize = 100;
                e.reserve = 200 * World.AMMO_MORE;
                e.grenades = 0;
                e.speed = 19;
                e.runSpeed = 31;
                e.mass = 1.6f;
                e.radius = 4.3f;
                break;
        }
        e.ammo = e.magSize;
        // ((11.4) Each kind of unit carries its own gun.)
        switch (role) {
            case Entity.ROLE_RIOT: w.combat.issue(e, Entity.W_SHOTGUN); break;
            case Entity.ROLE_SWAT: w.combat.issue(e, (e.nameSeed & 1) == 0 ? Entity.W_SMG : Entity.W_CARBINE); break;
            case Entity.ROLE_RECON: w.combat.issue(e, Entity.W_CARBINE); break;
            case Entity.ROLE_MARINE: if (e.member % 4 == 2) w.combat.issue(e, Entity.W_DMR); else e.weapon = Entity.W_STD; break;
            case Entity.ROLE_GUARD: if (e.member % 4 == 3) w.combat.issue(e, Entity.W_CARBINE); else e.weapon = Entity.W_STD; break;
            default: e.weapon = Entity.W_STD; break;
        }
    }

    /**
     * Reinforcements from outside arrive fresh, with extra magazines to hand round (the officers and soldiers
     * already fighting can borrow from them), and patch up anyone hurt on the way.
     */
    /** An officer from another force (10.20): highway patrol (1), the sheriff's (2), federal agents (4). */
    void makeAgency(Entity e, int agency) {
        e.agency = agency;
        if (agency == 4) {
            // Dark blue windbreakers.
            e.body = 0xFF1C2638;
            e.hp = e.maxHp = Math.max(e.maxHp, 70);
        } else e.body = agency == 1 || w.city.country.ruralShirt == 0 ? w.city.country.hpShirt : w.city.country.ruralShirt;
        // ((11.4) Troopers keep a carbine in the car, deputies a shotgun, the feds submachine guns.)
        int gun = agency == 1 ? Entity.W_CARBINE : agency == 2 ? Entity.W_SHOTGUN : agency == 4 ? Entity.W_SMG : Entity.W_STD;
        if (gun != Entity.W_STD && e.type == Entity.COP && e.role != Entity.ROLE_SWAT && e.role != Entity.ROLE_MARKSMAN) w.combat.issue(e, gun);
    }

    void reinforce(Entity e) {
        if (e.type == Entity.SOLDIER && e.role == Entity.ROLE_RIFLE) kitOut(e);
        e.reserve = Math.round(World.fullReserve(e) * 1.6f);
        e.ammo = e.magSize;
        e.hp = e.maxHp;
    }

    /** Makes a person or zombie without adding it to the city (used when loading a save). */
    Entity create(int type, float x, float y) {
        return make(type, x, y, -1, 0);
    }

    /** Recomputes counts and paths after a save has been loaded. */
    void afterLoad() {
        w.fieldTimer = 0;
        // ((15.6) The island prison's gate, open again if it had fallen.)
        if (w.prisonFallen) {
            w.city.openPrisonGate();
            w.containment.routesChanged();
        }
        // (Games saved before 10.18 may have dogs in them: there are no animals now.)
        for (Entity e : w.entities)
            if ((e.type == Entity.DOG && e.role != Entity.ROLE_K9) || e.type == Entity.ZOMBIE_DOG) {
                e.dead = true;
                e.removed = true;
            }
        findWilds();
        w.posts.buildRoster();
        // ((11.7) A saved game well into it already knows.)
        w.biteKnown = w.alert >= 1 && w.bites >= 8;
        w.exodus.launchBoats();
        w.wilds.spawnBirds();
        w.wilds.spawnWildlife();
        w.fleet.trafficTarget = 0;
        // ((12.5) A game saved with civilians driving about: they're out of their cars and on foot.)
        for (int i = w.fleet.vehicles.size() - 1; i >= 0; i--) {
            Fleet.Vehicle v = w.fleet.vehicles.get(i);
            // ((27.4) Nor a survivors' pickup or a convoy's bus, kept in the save: see VehicleSave.)
            if (v.type != Fleet.CAR || v.evacStop >= 0 || v.player != null || v.parked || v.kind == Fleet.K_PICKUP || v.convoy != null) continue;
            for (Entity r : v.riders) w.rides.release(r, v.x, v.y);
            v.riders.clear();
            v.removedFromFleet = true;
            w.fleet.vehicles.remove(i);
        }
        w.fleet.stationHelis();
        w.ops.setupBoats();
        // ((27.4) Not with the police cars kept in the save: they're all there already, crews and all.)
        if (!w.fleet.fromSave) w.fleet.stockStations();
        w.fleet.stockBases();
        w.fleet.ensureDozers();
        int cops = 0;
        for (Entity e : w.entities) if (!e.dead && e.type == Entity.COP) cops++;
        // ((11.4) Counting those aboard the patrol cars too.)
        for (Fleet.Vehicle v : w.fleet.vehicles) if (v.patrol && v.agency == 0) cops += v.crew.size();
        w.fleet.patrolTarget = roadCars(Math.max(1, (int) (cops * World.PATROL_SHARE / 2)));
        w.spatial.buildHash();
        w.city.computeFields(w.entities);
        w.stats.recount();
    }

    void erase(float x, float y, float r) {
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            float ddx = e.x - x, ddy = e.y - y;
            if (ddx * ddx + ddy * ddy < r * r) {
                e.dead = true;
                e.removed = true;
                e.hordeLeader = null;
                e.leadsHorde = false;
            }
        }
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.leader != null && e.leader.removed && e.leader.dead && !isAway(e.leader)) e.leader = null;
        }
        for (int i = w.corpses.size() - 1; i >= 0; i--) {
            World.Corpse c = w.corpses.get(i);
            float ddx = c.x - x, ddy = c.y - y;
            if (ddx * ddx + ddy * ddy < r * r) w.corpses.remove(i);
        }
        w.stats.recountLive();
        for (int i = 0; i < w.dcount; i++) {
            float ddx = w.dx[i] - x, ddy = w.dy[i] - y;
            if (ddx * ddx + ddy * ddy < r * r) w.dr[i] = 0;
        }
    }

    /** True if someone is only out of sight: hiding in a building or riding in a car. */
    boolean isAway(Entity e) {
        for (int i = 0, n = w.city.buildings.size(); i < n; i++) if (w.city.buildings.get(i).occupants.contains(e)) return true;
        for (int i = 0, n = w.fleet.vehicles.size(); i < n; i++) if (w.fleet.vehicles.get(i).riders.contains(e)) return true;
        return false;
    }

    void clearAll() {
        w.sandbox.clearBarricades();
        w.medkits.clear();
        w.entities.clear();
        Arrays.fill(w.lodWake, 0);
        w.corpses.clear();
        w.grenades.clear();
        w.explosions.clear();
        w.pickups.clear();
        w.pendingBlasts.clear();
        w.fleet.clear();
        for (City.Building b : w.city.buildings) {
            b.occupants.clear();
            // ((12.0) Everyone means everyone: those inside on errands, the dead shut in buildings, and those who'd
            // fled the city all used to come back afterwards.)
            b.visitors.clear();
            b.lurkers = 0;
            b.infestKnown = false;
            b.barricade = 100;
        }
        w.away.clear();
        w.stats.recountInsideNow();
        Arrays.fill(w.plife, 0);
        Arrays.fill(w.tlife, 0);
        w.dcount = 0;
        w.dnext = 0;
        w.outbreak = false;
        w.dispatch.clear();
        for (World.Holdout h : w.holdouts) h.b.holdout = null;
        w.holdouts.clear();
        w.stats.recount();
    }

    /** Every precinct and base starts with a limited supply of ammunition. */
    /** The army base's kind makes its own difference to the war (only if the city has reserves at all). */
    void baseStrengths() {
        City.Facility base = w.city.mainBase();
        if (base == null) return;
        // ((15.5) The Military Town's great base: a well-stocked armoury, three times the rounds.)
        if (w.city.cfg.bigGarrison()) base.ammo *= 3;
        switch (base.baseType) {
            case City.BT_AIR: if (w.dispatch.airSorties > 0) w.dispatch.airSorties++; break;
            case City.BT_ARMOUR: if (w.dispatch.tankReserve > 0) w.dispatch.tankReserve++; break;
            case City.BT_TRAINING: if (w.dispatch.squadReserve > 0) w.dispatch.squadReserve += 16; break;
            case City.BT_SPECIAL: base.ammo += base.ammo / 3; break;
            case City.BT_SUPPLY: base.ammo *= 2; break;
            case City.BT_RADAR: if (w.dispatch.airSorties > 0) w.dispatch.airSorties++; break;
            default: break;
        }
    }

    void stockArmouries() {
        for (City.Facility f : w.city.facilities) {
            if (f.kind == City.FACILITY_POLICE) f.ammo = World.ARMOURY_POLICE;
            else if (f.kind == City.FACILITY_BASE) f.ammo = World.ARMOURY_BASE;
        }
    }

    /** Bats lying about near homes, and axes at the fire stations. */
    void scatterWeapons() {
        int n = 0;
        for (City.Building b : w.city.buildings) {
            if (b.doorX == 0) continue;
            boolean fire = b.kind == City.FIRE_STATION;
            if (!fire && (b.kind != City.HOUSE || w.rnd.nextFloat() > 0.14f)) continue;
            for (int k = 0; k < (fire ? 2 : 1); k++) {
                float[] q = w.city.findWalkable(b.doorX + w.rnd.nextFloat() * 16 - 8, b.doorY + w.rnd.nextFloat() * 16 - 8);
                if (q == null) continue;
                World.Pickup p = new World.Pickup();
                p.x = q[0];
                p.y = q[1];
                // (A crowbar or a machete in some sheds and garages since 10.24.)
                int pick = ((int) b.doorX * 31 + (int) b.doorY) & 7;
                p.melee = fire ? Entity.M_AXE : pick == 0 ? Entity.M_MACHETE : pick < 3 ? Entity.M_CROWBAR : Entity.M_BAT;
                w.pickups.add(p);
            }
            if (++n > 80) break;
        }
    }

    /** (15.0) Two traffic officers at the busiest junctions with lights near each precinct (three at the main one). */
    void postTrafficOfficers() {
        int nj = w.city.junctions.size();
        w.trafficPost = new Entity[nj];
        w.junctionHeld = new float[nj];
        boolean first = true;
        for (City.Facility f : w.city.facilities) {
            if (f.kind != City.FACILITY_POLICE || f.satellite) continue;
            int want = first ? 3 : 2;
            first = false;
            for (int k = 0; k < want; k++) {
                int best = -1;
                float bd = 900;
                for (int id = 0; id < nj; id++) {
                    int[] j = w.city.junctions.get(id);
                    if (j[4] != City.J_LIGHTS || w.trafficPost[id] != null) continue;
                    float cx = (j[0] + j[2] + 1) / 2f * City.T, cy = (j[1] + j[3] + 1) / 2f * City.T;
                    float d = (float) World.hypot(cx - f.x, cy - f.y);
                    if (d < bd) {
                        bd = d;
                        best = id;
                    }
                }
                if (best < 0) break;
                int[] j = w.city.junctions.get(best);
                // On a corner of it, on the pavement.
                float[] spot = null;
                int[][] corners = {{j[0] - 1, j[1] - 1}, {j[2] + 1, j[3] + 1}, {j[2] + 1, j[1] - 1}, {j[0] - 1, j[3] + 1}};
                for (int[] q : corners) {
                    if (q[0] < 0 || q[1] < 0 || q[0] >= w.city.w || q[1] >= w.city.h || w.city.solidTile(q[0], q[1])) continue;
                    if (w.city.tiles[q[1] * w.city.w + q[0]] == City.ROAD) continue;
                    spot = new float[]{(q[0] + 0.5f) * City.T, (q[1] + 0.5f) * City.T};
                    break;
                }
                if (spot == null) {
                    w.trafficPost[best] = w.controlled; // (taken: no corner to stand on)
                    continue;
                }
                Entity t = spawn(Entity.COP, spot[0], spot[1]);
                if (t == null) break;
                applyRole(t, Entity.ROLE_TRAFFIC);
                t.task = Dispatch.T_POST;
                t.postX = spot[0];
                t.postY = spot[1];
                w.trafficPost[best] = t;
            }
        }
        for (int id = 0; id < nj; id++) if (w.trafficPost[id] != null && w.trafficPost[id].type != Entity.COP) w.trafficPost[id] = null;
    }

    /** (15.0) After loading a game: each traffic officer still on a corner back on the junction beside it. */
    void relinkTraffic() {
        w.trafficLooked = true;
        int nj = w.city.junctions.size();
        Entity[] posts = new Entity[nj];
        boolean any = false;
        for (Entity e : w.entities) {
            if (e.dead || e.type != Entity.COP || e.role != Entity.ROLE_TRAFFIC || e.task != Dispatch.T_POST) continue;
            int best = -1;
            float bd = City.T * 4;
            for (int id = 0; id < nj; id++) {
                int[] j = w.city.junctions.get(id);
                float d = (float) World.hypot((j[0] + j[2] + 1) / 2f * City.T - e.postX, (j[1] + j[3] + 1) / 2f * City.T - e.postY);
                if (d < bd) {
                    bd = d;
                    best = id;
                }
            }
            if (best >= 0) {
                posts[best] = e;
                any = true;
            }
        }
        if (any) {
            w.trafficPost = posts;
            w.junctionHeld = new float[nj];
        }
    }

    /** (15.6) The prison's inmates in their orange, out in the yard, and its guards with their shotguns. */
    void populatePrison() {
        int[] p = w.city.prison;
        int area = (p[2] - p[0]) * (p[3] - p[1]);
        // ((15.8) Up to 350 in the bigger prison.)
        int inmates = Math.max(30, Math.min(350, area / 9)), guards = Math.max(3, Math.min(10, inmates / 35));
        // ((32.3) A city with no civilians: an empty prison (its guards still on the walls).)
        int put = w.city.cfg.civic >= 56 && w.city.cfg.noCivilians() ? 0 : inmates;
        for (int i = 0; i < put; i++) {
            float[] q = w.prison.prisonYardSpot();
            if (q == null) break;
            Entity e = spawn(Entity.CIVILIAN, q[0], q[1]);
            if (e == null) break;
            e.inmate = true;
            e.body = 0xFFE07A2A;
            e.home = null;
            e.hasGun = false;
            e.ammo = e.reserve = 0;
            e.melee = 0;
        }
        // ((15.6) The guards: one on each of their posts (the gate, each watchtower, the barrier on the seafront), the
        // rest out in the yard.)
        java.util.List<float[]> posts = w.city.prisonPosts;
        for (int i = 0; i < guards + posts.size(); i++) {
            float[] q = i < posts.size() ? w.city.findWalkable(posts.get(i)[0], posts.get(i)[1]) : w.prison.prisonYardSpot();
            if (q == null) continue;
            Entity g = spawn(Entity.COP, q[0], q[1]);
            if (g == null) break;
            g.role = Entity.ROLE_PRISON_GUARD;
            // (Khaki and a brown hat, not police blue.)
            g.body = 0xFF8A7448;
            g.head = 0xFF5A4630;
            w.combat.setGun(g, Entity.W_SHOTGUN, 40);
            g.task = Dispatch.T_POST;
            g.postX = q[0];
            g.postY = q[1];
        }
        float[] f = w.city.ferry;
        if (f != null) w.ferryBoat = new float[]{f[0], f[1], 0, 1, 0, 1};
    }

    /** The rangers, their truck and the lookouts up the fire towers, at the start of a game. */
    void populateWilds(boolean rangers) {
        findWilds();
        int stations = 0;
        for (City.Facility f : w.city.facilities) if (f.kind == City.FACILITY_POLICE) stations++;
        w.rescueTeams = Math.max(1, Math.min(3, 1 + (w.city.w >= 400 ? 1 : 0) + (stations >= 2 ? 1 : 0)));
        if (!w.city.cfg.wilds()) return;
        if (w.rangerStation != null && rangers) {
            for (int i = 0; i < 2; i++) {
                Entity r = spawn(Entity.COP, w.rangerStation.doorX + w.rnd.nextFloat() * 20 - 10, w.rangerStation.doorY + w.rnd.nextFloat() * 20 - 10);
                if (r != null) makeRanger(r);
            }
            w.fleet.startRangerPatrol(w.rangerStation);
        }
        // (10.34) The outposts by the other mountains: two rangers and a truck each, and another rescue team.
        if (rangers)
            for (City.Building b : w.city.buildings) {
                if (b.name == null || !w.city.outpostNames.contains(b.name)) continue;
                for (int i = 0; i < 2; i++) {
                    Entity r = spawn(Entity.COP, b.doorX + w.rnd.nextFloat() * 20 - 10, b.doorY + w.rnd.nextFloat() * 20 - 10);
                    if (r != null) makeRanger(r);
                }
                w.fleet.startRangerPatrol(b);
                w.rescueTeams++;
            }
        for (int k = 0; k < w.city.fireTowers.size(); k++) {
            float[] t = w.city.fireTowers.get(k);
            Entity e = spawn(Entity.CIVILIAN, t[0], t[1]);
            if (e != null) makeLookout(e, k);
        }
    }

    /** The ranger station and the lookouts on duty (after a save is loaded too). */
    void findWilds() {
        w.rangerStation = null;
        if (w.city.rangerStationName != null)
            for (City.Building b : w.city.buildings) if (w.city.rangerStationName.equals(b.name)) w.rangerStation = b;
        int n = w.city.fireTowers.size();
        w.towerKeeper = new Entity[n];
        w.towerRelief = new Entity[n];
        w.towerShift = new float[n];
        for (int k = 0; k < n; k++) w.towerShift[k] = 200 + w.rnd.nextFloat() * 220;
        for (Entity e : w.entities) {
            if (e.dead || e.type != Entity.CIVILIAN || e.jobStep < 0 || e.jobStep >= n) continue;
            if (e.job == Entity.J_LOOKOUT) {
                w.towerKeeper[e.jobStep] = e;
                e.spot = w.city.fireTowers.get(e.jobStep);
            } else if (e.job == Entity.J_RELIEF) w.towerRelief[e.jobStep] = e;
        }
    }

    void makeRanger(Entity e) {
        e.agency = 3;
        e.body = World.RANGER_SHIRT;
        // ((11.4) Rangers carry a hunting rifle.)
        if (e.type == Entity.COP) w.combat.issue(e, Entity.W_HUNTING);
    }

    void makeLookout(Entity e, int k) {
        float[] t = w.city.fireTowers.get(k);
        e.job = Entity.J_LOOKOUT;
        e.jobStep = k;
        e.spot = t;
        e.x = t[0];
        e.y = t[1];
        e.aloft = true;
        if (w.rangerStation != null) e.home = w.rangerStation;
        e.homeChecked = true;
        w.towerKeeper[k] = e;
    }
}
