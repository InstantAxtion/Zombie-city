package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;

/**
 * ((18.0) The campaign: missions played one after another, each unlocking the next once it's won. In a mission you
 * are the commander, not a god: the city's police and army are what you start with, more of them cost money from a
 * budget that grows with funding and with how well it's going, and they come in from the stations and bases (or
 * the edge of town) and go where they're sent. Nothing is free: no spawning of the other side, no bombs from the
 * sky that aren't paid for, no clearing the streets with a button.
 */
final class Campaign {
    private Campaign() {
    }

    /** What winning a mission takes. */
    static final int G_CLEAR = 0, G_SURVIVE = 1;
    /** ((20.0) Hold out until dawn; get goalN people out of the city; save goalN of the wounded; destroy goalN of the dead. */
    static final int G_DAWN = 2, G_EVAC = 3, G_RESCUE = 4, G_KILLS = 5;
    static final String[] GOAL_NAMES = {"CLEAR THE CITY", "SURVIVE", "HOLD UNTIL DAWN", "EVACUATE", "SAVE THE WOUNDED", "BODY COUNT"};

    static final class Mission {
        final String title, brief;
        final int preset, size, cops, soldiers, foes;
        final long seed;
        final int budget, income, limit, goal, goalN;
        /** Two stars more: civilians lost no more than starCivilians, and police and soldiers lost no more than starLosses. */
        final int starCivilians, starLosses;
        /** ((20.0) The sky and the rules: the Time and Weather settings, the hour it starts (or -1), how hard, who flees, the power. */
        int time = Sky.TIME_DAY, weather = Sky.CLEAR + 1, difficulty = Director.NORMAL, exodus = CityConfig.EXODUS_OFF;
        float startClock = -1;
        boolean blackout;

        Mission sky(int time, int weather, float startClock) {
            this.time = time;
            this.weather = weather;
            this.startClock = startClock;
            return this;
        }

        Mission rules(int difficulty, int exodus, boolean blackout) {
            this.difficulty = difficulty;
            this.exodus = exodus;
            this.blackout = blackout;
            return this;
        }

        Mission(String title, String brief, int preset, int size, int cops, int soldiers, int foes, long seed,
                int budget, int income, int limit, int goal, int goalN, int starCivilians, int starLosses) {
            this.title = title;
            this.brief = brief;
            this.preset = preset;
            this.size = size;
            this.cops = cops;
            this.soldiers = soldiers;
            this.foes = foes;
            this.seed = seed;
            this.budget = budget;
            this.income = income;
            this.limit = limit;
            this.goal = goal;
            this.goalN = goalN;
            this.starCivilians = starCivilians;
            this.starLosses = starLosses;
        }
    }

    // (cops and soldiers: what the city starts with; foes: the zombies at the start.)
    static final Mission[] MISSIONS = {
            new Mission("Patient Zero", "A handful of infected in a quiet town. The local police are all you have: put it down before it spreads.",
                    6, CityConfig.SMALL, 24, 0, 10, 41207, 250, 40, 600, G_CLEAR, 0, 40, 8),
            new Mission("The Long Night", "Forty of the dead in the suburbs, more getting up, and the sun going down. You don't have to win - just keep the town standing until dawn.",
                    2, CityConfig.SMALL, 30, 8, 40, 88315, 320, 60, 480, G_DAWN, 0, 150, 10).sky(Sky.TIME_CYCLE, Sky.CLEAR + 1, 0.79f),
            new Mission("Dead City", "Downtown is overrun: ninety of the dead and the numbers climbing. Clear the city.",
                    1, CityConfig.MEDIUM, 40, 20, 90, 64019, 700, 120, 1200, G_CLEAR, 0, 200, 20),
            // ((20.0) Seven more, each with its own objective, hour and weather.)
            new Mission("Exodus", "The city can't be held. Buses and trains are running: keep the roads to them open and get four hundred and fifty people out before it falls.",
                    0, CityConfig.MEDIUM, 36, 12, 90, 51731, 450, 90, 900, G_EVAC, 450, 220, 14).rules(Director.NORMAL, CityConfig.EXODUS_ALL, false),
            new Mission("Whiteout", "Fog off the hills, and something moving in it. Nobody can see past the end of the street - them included. Clear the village.",
                    9, CityConfig.SMALL, 22, 6, 35, 77342, 300, 50, 780, G_CLEAR, 0, 60, 8).sky(Sky.TIME_DAY, Sky.FOG + 1, -1),
            new Mission("Triage", "The industrial district is a battlefield and the wounded are lying where they fell. Get to them before the dead do: bring twenty of them back alive.",
                    3, CityConfig.SMALL, 30, 16, 110, 30977, 420, 80, 900, G_RESCUE, 20, 200, 26),
            new Mission("Storm Front", "Night, a storm overhead, and thunder walking the dead through the streets. Keep the old town alive for ten minutes.",
                    5, CityConfig.SMALL, 34, 14, 60, 91853, 480, 90, 600, G_SURVIVE, 0, 180, 16).sky(Sky.TIME_NIGHT, Sky.STORM + 1, -1),
            new Mission("Body Count", "They keep coming in from the country. Command wants seven hundred of them destroyed before nightfall changes the odds.",
                    8, CityConfig.MEDIUM, 40, 30, 120, 12011, 700, 130, 1080, G_KILLS, 700, 260, 24).rules(Director.HARD, CityConfig.EXODUS_OFF, false),
            new Mission("Blackout", "The power station is gone and it's the middle of the night. No street lights, no mercy: bring flares, and clear the campus.",
                    7, CityConfig.SMALL, 32, 12, 45, 45589, 600, 100, 960, G_CLEAR, 0, 120, 14).sky(Sky.TIME_NIGHT, Sky.CLEAR + 1, -1).rules(Director.NORMAL, CityConfig.EXODUS_OFF, true),
            new Mission("Last Stand", "Everything that's left has fallen back on the base. The sun is setting, and every horde for miles knows where you are. Hold until dawn.",
                    11, CityConfig.MEDIUM, 30, 50, 60, 68111, 1000, 160, 600, G_DAWN, 0, 400, 40).sky(Sky.TIME_CYCLE, Sky.CLEAR + 1, 0.79f).rules(Director.HARD, CityConfig.EXODUS_OFF, false),
    };

    /**
     * Everyone a mission's city holds at most (the Max population setting doesn't apply): fewer against the dead,
     * where every one of them can be bitten and get up.
     */
    static int population(int id) {
        // ((20.0) The Military Town's garrison alone nearly fills the usual number, leaving no room for the dead.)
        Mission m = get(id);
        if (m != null && m.preset == 11) return 2800;
        return 1800;
    }

    static Mission get(int id) {
        return id >= 0 && id < MISSIONS.length ? MISSIONS[id] : null;
    }

    /** The city a mission is played on: always the same streets, forces and enemy. */
    static CityConfig config(int id) {
        Mission m = MISSIONS[id];
        CityConfig c = new CityConfig();
        c.v[CityConfig.OPT_PRESET] = m.preset;
        c.v[CityConfig.OPT_SIZE] = m.size;
        // (No help from outside: every reinforcement is one you pay for.)
        c.v[CityConfig.OPT_RESERVES] = 0;
        c.v[CityConfig.OPT_COUNTRY] = Country.USA;
        c.seed = m.seed;
        c.timeOfDay = m.time;
        c.weather = m.weather;
        c.difficulty = m.difficulty;
        c.exodus = m.exodus;
        c.mission = id;
        c.normalize();
        return c;
    }

    // ---- Costs ----

    /** Each police officer by GameView.COP_ROLES order; -1 not to be had. */
    static final int[] COP_COST = {10, 15, 25, 20, 12, 12, 12, 15};
    /** Each soldier by GameView.MIL_ROLES order. */
    static final int[] MIL_COST = {15, 60, 25, 25, 12, 25, 30, 20, 20, 20, 40};
    /** Car, police car, tank, barricade, crate, medkit, BearCat, recon Humvee. */
    static final int[] PLACE_COST = {-1, 30, 250, 4, 10, 10, 120, 40, 45};
    /** Horde, panic, outbreak, supply drop, airstrike, city alarm, infect, ((20.0) flare, mines, night/day, weather. */
    static final int[] EVENT_COST = {-1, -1, -1, 25, 150, 0, -1, 12, 35, -1, -1};

    /** The kind of unit a mission ends on, from what's paid for (for the budget's sake). */
    static int income(World w) {
        Mission m = w.mission();
        return m == null ? 0 : m.income;
    }

    /** Money earned so far: funding each minute, and rewards for what's been done. */
    static int earned(World w) {
        Mission m = w.mission();
        if (m == null) return 0;
        int minutes = (int) (missionTime(w) / 60);
        return m.income * minutes + w.zombiesKilled + w.woundedSaved * 5;
    }

    static int budget(World w) {
        Mission m = w.mission();
        return m == null ? 0 : m.budget + earned(w) - w.spent;
    }

    /** Pays for something if there's the money; false (and nothing spent) if not. */
    static boolean spend(World w, int cost) {
        if (cost < 0 || budget(w) < cost) return false;
        w.spent += cost;
        return true;
    }

    static float missionTime(World w) {
        return Math.max(0, w.time - w.missionStart);
    }

    static int timeLeft(World w) {
        Mission m = w.mission();
        if (m == null) return 0;
        // ((20.0) Until dawn: what's left of the day, and then of the night.)
        if (m.goal == G_DAWN) return (int) (w.sky.night() ? w.sky.untilDawn() : w.sky.untilNight() + Sky.DAY_SECONDS * 0.44f);
        return Math.max(0, (int) Math.ceil(m.limit - missionTime(w)));
    }

    // ---- Running a mission ----

    /** A mission's first moments: the clock starts, veterans from the last missions report in, the briefing. */
    static void begin(World w, String roster) {
        Mission m = w.mission();
        if (m == null) return;
        w.missionStart = w.time;
        w.spent = 0;
        w.missionResult = 0;
        w.missionStars = 0;
        w.dispatch.policeReserve = 0;
        w.dispatch.squadReserve = 0;
        w.dispatch.tankReserve = 0;
        w.dispatch.swatTeams = 0;
        // ((20.0) The mission's hour, and its power cut.)
        if (m.startClock >= 0) {
            w.sky.clock = m.startClock;
            w.sky.resume();
        }
        if (m.blackout) w.blackout = true;
        trim(w, Entity.COP, m.cops);
        trim(w, Entity.SOLDIER, m.soldiers);
        int vets = applyVeterans(w, roster);
        w.dispatch.say(Dispatch.WHO_INFO, null, "Mission: " + m.title + ". " + goalText(w) + (vets > 0 ? " "
                + World.count(vets, "veteran is", "veterans are") + " back with you." : ""), w.city.worldW() / 2, w.city.worldH() / 2);
    }

    /**
     * The city's force cut down to the mission's: the patrol cars and army trucks beyond a few parked (their crews put
     * out on foot), then whoever's left over beyond the number taken off the map.
     */
    static void trim(World w, int type, int cap) {
        // (Most of the police start out in patrol cars, two to a car, and aren't on the street at all: the cars past
        // the few kept go, their crews put out on foot to be counted with the rest.)
        int kept = 0, cars = 0, maxCars = Math.max(1, cap / 6);
        ArrayList<Fleet.Vehicle> vs = new ArrayList<Fleet.Vehicle>(w.fleet.vehicles);
        for (Fleet.Vehicle v : vs) {
            int aboard = 0;
            for (Entity e : v.crew) if (e.type == type) aboard++;
            for (Entity e : v.riders) if (e.type == type) aboard++;
            if (v.passengerType == type) aboard += v.passengers;
            if (aboard == 0) continue;
            if (cars < maxCars && kept + aboard <= cap) {
                cars++;
                kept += aboard;
                continue;
            }
            if (v.passengerType == type) v.passengers = 0;
            for (Entity e : new ArrayList<Entity>(v.riders)) w.release(e, v.x, v.y);
            v.riders.clear();
            w.fleet.discard(v);
        }
        if (type == Entity.COP) w.fleet.patrolTarget = Math.min(w.fleet.patrolTarget, cars);
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.type != type || e.dead || e.removed) continue;
            if (kept < cap) {
                kept++;
                continue;
            }
            e.dead = true;
            e.removed = true;
            for (int b = 0, nb = w.city.buildings.size(); b < nb; b++) w.city.buildings.get(b).occupants.remove(e);
        }
        // (And anyone of theirs indoors, out of sight.)
        for (int b = 0, nb = w.city.buildings.size(); b < nb; b++) {
            java.util.List<Entity> occ = w.city.buildings.get(b).occupants;
            for (int k = occ.size() - 1; k >= 0; k--) {
                Entity e = occ.get(k);
                if (e.type != type || e.dead) continue;
                if (kept < cap) {
                    kept++;
                    continue;
                }
                e.dead = true;
                e.removed = true;
                occ.remove(k);
            }
        }
        w.recountLive();
    }

    /** What has to be done, as one line. */
    static String goalText(World w) {
        Mission m = w.mission();
        if (m == null) return "";
        String time = World.clock(m.limit);
        switch (m.goal) {
            case G_SURVIVE: return "Keep the city standing for " + time + ".";
            case G_DAWN: return "Keep the city standing until dawn.";
            case G_EVAC: return "Get " + m.goalN + " people out of the city within " + time + ".";
            case G_RESCUE: return "Save " + m.goalN + " of the wounded within " + time + ".";
            case G_KILLS: return "Destroy " + m.goalN + " of the dead within " + time + ".";
            default: return "Destroy every zombie within " + time + ".";
        }
    }

    /** ((20.0) How far along a counted objective is, or -1 if this mission's isn't counted. */
    static int count(World w) {
        Mission m = w.mission();
        if (m == null) return -1;
        return m.goal == G_EVAC ? w.fled : m.goal == G_RESCUE ? w.woundedSaved : m.goal == G_KILLS ? w.zombiesKilled : -1;
    }

    /** How the main objective's going, short. */
    static String progress(World w) {
        Mission m = w.mission();
        if (m == null) return "";
        int n = count(w);
        if (n >= 0) return Math.min(n, m.goalN) + " / " + m.goalN + (m.goal == G_EVAC ? " out" : m.goal == G_RESCUE ? " saved" : " destroyed");
        if (m.goal == G_DAWN) return w.sky.night() ? "Dawn in " + World.clock((int) w.sky.untilDawn()) : "Night falls in " + World.clock((int) w.sky.untilNight());
        return w.zombieCount() + (m.goal == G_SURVIVE ? " zombies left standing" : " zombies left");
    }

    static int lost(World w) {
        return w.copsLost + w.soldiersLost;
    }

    /** Checked once a second: won, lost, or still going. */
    static void tick(World w) {
        Mission m = w.mission();
        if (m == null || w.missionResult != 0) return;
        float t = missionTime(w);
        boolean out = t >= m.limit;
        String why = null;
        boolean won;
        switch (m.goal) {
            case G_SURVIVE: won = w.warResult == 1 || (out && w.warResult != 2); break;
            // (Dawn after a night that fell during the mission; there's no clock to run out but the sun.)
            case G_DAWN:
                won = w.warResult == 1 || (w.warResult != 2 && w.nightsSurvived > 0);
                out = false;
                break;
            case G_EVAC: case G_RESCUE: case G_KILLS: won = count(w) >= m.goalN; break;
            default: won = w.warResult == 1;
        }
        // (A counted objective isn't lost with the city cleared early: there's nobody left to count, so it's won.)
        if (!won && w.warResult == 1 && m.goal != G_CLEAR) won = m.goal == G_KILLS || m.goal == G_RESCUE || m.goal == G_EVAC;
        if (!won) {
            if (w.warResult == 2) why = "The city has fallen.";
            else if (out) why = "Out of time.";
            else if (t > 20 && armed(w) == 0 && budget(w) < cheapest()) why = "Your forces are gone, and there's no money for more.";
        }
        if (won) finish(w, 1, null);
        else if (why != null) finish(w, 2, why);
    }

    static int armed(World w) {
        return w.counts[Entity.COP] + w.counts[Entity.SOLDIER];
    }

    static int cheapest() {
        int c = Integer.MAX_VALUE;
        for (int v : COP_COST) if (v >= 0) c = Math.min(c, v);
        for (int v : MIL_COST) if (v >= 0) c = Math.min(c, v);
        return c;
    }

    static boolean starCivilians(World w) {
        Mission m = w.mission();
        return m != null && w.civiliansLost <= m.starCivilians;
    }

    static boolean starLosses(World w) {
        Mission m = w.mission();
        return m != null && lost(w) <= m.starLosses;
    }

    static void finish(World w, int result, String why) {
        Mission m = w.mission();
        w.missionResult = result;
        w.missionStars = result == 1 ? 1 + (starCivilians(w) ? 1 : 0) + (starLosses(w) ? 1 : 0) : 0;
        w.missionWhy = why;
        String stars = result == 1 ? stars(w.missionStars) + "  " : "";
        w.timeline(result == 1 ? "Mission complete: " + m.title : "Mission failed: " + m.title);
        w.banner(result == 1 ? "MISSION COMPLETE" : "MISSION FAILED", result == 1 ? stars + "In " + World.clock((int) missionTime(w))
                + ", " + World.count(lost(w), "officer or soldier", "officers and soldiers") + " lost." : why);
    }

    static String stars(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) sb.append(i < n ? '★' : '☆');
        return sb.toString();
    }

    // ---- Veterans ----

    /** The ranked police and soldiers still standing, best first, as the roster for the next mission. */
    static String veterans(World w) {
        ArrayList<Entity> best = new ArrayList<Entity>();
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || e.removed || e.hp <= 0 || (e.type != Entity.COP && e.type != Entity.SOLDIER)) continue;
            if (World.rank(e) < 1) continue;
            best.add(e);
        }
        java.util.Collections.sort(best, new java.util.Comparator<Entity>() {
            @Override
            public int compare(Entity a, Entity b) {
                return b.kills - a.kills;
            }
        });
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(MAX_VETERANS, best.size()); i++) {
            Entity e = best.get(i);
            sb.append(e.type).append(',').append(e.kills).append(',').append(e.nameSeed).append(';');
        }
        return sb.toString();
    }

    static final int MAX_VETERANS = 10;

    static int rosterSize(String roster) {
        if (roster == null || roster.isEmpty()) return 0;
        int n = 0;
        for (String s : roster.split(";")) if (s.split(",").length == 3) n++;
        return n;
    }

    /** Veterans take the place of the first of the city's own of their kind: their name, and their kills. */
    static int applyVeterans(World w, String roster) {
        if (roster == null || roster.isEmpty()) return 0;
        int done = 0;
        java.util.HashSet<Entity> used = new java.util.HashSet<Entity>();
        for (String s : roster.split(";")) {
            String[] p = s.split(",");
            if (p.length != 3) continue;
            int type, kills, seed;
            try {
                type = Integer.parseInt(p[0]);
                kills = Integer.parseInt(p[1]);
                seed = Integer.parseInt(p[2]);
            } catch (NumberFormatException ex) {
                continue;
            }
            for (int i = 0, n = w.entities.size(); i < n; i++) {
                Entity e = w.entities.get(i);
                if (e.type != type || e.dead || used.contains(e) || e.kills > 0) continue;
                used.add(e);
                e.nameSeed = seed;
                e.kills = kills;
                done++;
                break;
            }
        }
        return done;
    }

    // ---- Commanding ----

    /**
     * Where new units come in for a spot: the nearest police station (for police) or base (for soldiers), or the edge
     * of town if that's nearer or there's none.
     */
    static float[] arrival(World w, boolean police, float x, float y) {
        City.Facility f = w.city.nearestFacility(police ? City.FACILITY_POLICE : City.FACILITY_BASE, x, y);
        if (f == null && !police) f = w.city.nearestFacility(City.FACILITY_POLICE, x, y);
        float[] in = f != null ? w.city.findWalkable(f.gateX, f.gateY) : null;
        float[] edge = edgeNear(w, x, y);
        if (in == null) return edge;
        if (edge != null && World.hypot(edge[0] - x, edge[1] - y) < World.hypot(in[0] - x, in[1] - y) * 0.7f) return edge;
        return in;
    }

    private static float[] edgeNear(World w, float x, float y) {
        float ww = w.city.worldW(), hh = w.city.worldH();
        float dl = x, dr = ww - x, dt = y, db = hh - y;
        float m = Math.min(Math.min(dl, dr), Math.min(dt, db));
        float ex = m == dl ? 0 : m == dr ? ww : x, ey = m == dt ? 0 : m == db ? hh : y;
        return w.city.edgeSpawn(ex, ey);
    }
}
