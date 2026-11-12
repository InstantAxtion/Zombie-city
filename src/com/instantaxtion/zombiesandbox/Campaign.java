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
    static final int G_CLEAR = 0, G_SURVIVE = 1, G_ARREST = 2, G_BREAK = 3, G_HOLD = 4, G_HEIST = 5, G_PRISON = 6;

    static final class Mission {
        final String title, brief;
        final int mode, preset, size, cops, soldiers, foes;
        final long seed;
        final int budget, income, limit, goal, goalN;
        /** Two stars more: civilians lost no more than starCivilians, and police and soldiers lost no more than starLosses. */
        final int starCivilians, starLosses;

        Mission(String title, String brief, int mode, int preset, int size, int cops, int soldiers, int foes, long seed,
                int budget, int income, int limit, int goal, int goalN, int starCivilians, int starLosses) {
            this.title = title;
            this.brief = brief;
            this.mode = mode;
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

    private static final int Z = CityConfig.MODE_ZOMBIES, G = CityConfig.MODE_GANGS, A = CityConfig.MODE_ARMY, H = CityConfig.MODE_HEIST, P = CityConfig.MODE_PRISON;

    // (cops and soldiers: what the city starts with; foes: the zombies at the start, or the gang members or enemy
    // soldiers on the streets, with more of them to come in waves.)
    static final Mission[] MISSIONS = {
            new Mission("Patient Zero", "A handful of infected in a quiet town. The local police are all you have: put it down before it spreads.",
                    Z, 6, CityConfig.SMALL, 24, 0, 10, 41207, 250, 40, 600, G_CLEAR, 0, 40, 8),
            new Mission("The Long Night", "Forty of the dead in the suburbs and more getting up. You don't have to win - just keep the town standing until the army can get there.",
                    Z, 2, CityConfig.SMALL, 30, 8, 40, 88315, 320, 60, 480, G_SURVIVE, 0, 150, 10),
            new Mission("Corner Boys", "Street crews have taken the old town's corners. Arrest fifteen of them - the courts want them alive - or clear them off the streets.",
                    G, 5, CityConfig.SMALL, 20, 0, 40, 23977, 300, 50, 720, G_ARREST, 15, 40, 15),
            new Mission("Kingpins", "Two gangs run downtown from their hideouts. Break them: take their bosses down and the crews fall apart.",
                    G, 1, CityConfig.MEDIUM, 30, 10, 70, 51623, 400, 70, 900, G_BREAK, 2, 80, 40),
            new Mission("Hold the Line", "An enemy army is marching on the town. Don't let them take more than one of the stations, the base or City Hall before help arrives.",
                    A, 11, CityConfig.SMALL, 30, 30, 60, 70451, 600, 90, 600, G_HOLD, 1, 60, 60),
            new Mission("Liberation", "The enemy is dug in across the city. Drive every last one of them out.",
                    A, 0, CityConfig.MEDIUM, 40, 40, 100, 19288, 600, 100, 1200, G_CLEAR, 0, 60, 120),
            new Mission("Dead City", "Downtown is overrun: ninety of the dead and the numbers climbing. Clear the city.",
                    Z, 1, CityConfig.MEDIUM, 40, 20, 90, 64019, 700, 120, 1200, G_CLEAR, 0, 200, 20),
            // ((18.1) Heists.)
            new Mission("The Bank Job", "Three crews are going to hit downtown's banks and shops today. Stop them: box them in and talk them out, or catch the getaway car. Let no more than one get away.",
                    H, 1, CityConfig.SMALL, 12, 0, 3, 30517, 350, 60, 900, G_HEIST, 1, 10, 6),
            new Mission("Crime Wave", "Six crews, two at a time, all over the city. A negotiator saves lives; a roadblock of cars and officers stops a getaway. No more than two may escape.",
                    H, 0, CityConfig.MEDIUM, 12, 0, 6, 77141, 450, 80, 1500, G_HEIST, 2, 20, 12),
            // ((18.2) The prison break, on the Prison Island.)
            new Mission("Lockdown", "A riot is about to start in the island prison. The guards won't hold for long: be at the causeway when the gate goes, and let no more than five inmates get clear of the city.",
                    P, CityConfig.PRISON_ISLAND, CityConfig.SMALL, 20, 6, 30, 50923, 400, 60, 900, G_PRISON, 5, 40, 8),
            new Mission("Breakout", "The whole prison is going up at once. Hold the causeway with everything you've got; no more than five may escape.",
                    P, CityConfig.PRISON_ISLAND, CityConfig.MEDIUM, 16, 6, 90, 81277, 550, 90, 1200, G_PRISON, 5, 60, 15),
    };

    /**
     * Everyone a mission's city holds at most (the Max population setting doesn't apply): fewer against the dead,
     * where every one of them can be bitten and get up.
     */
    static int population(int id) {
        Mission m = get(id);
        return m != null && m.mode == Z ? 1800 : 5000;
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
        c.mode = m.mode;
        c.mission = id;
        c.normalize();
        return c;
    }

    // ---- Costs ----

    /** Each police officer by GameView.COP_ROLES order; -1 not to be had. */
    static final int[] COP_COST = {10, 15, 25, 20, 12, 12, 12, 15, 30};
    /** Each soldier by GameView.MIL_ROLES order. */
    static final int[] MIL_COST = {15, 60, 25, 25, 12, 25, 30, 20, 20, 20, 40};
    /** Car, police car, tank, barricade, crate, medkit, BearCat, recon Humvee. */
    static final int[] PLACE_COST = {-1, 30, 250, 4, 10, 10, 120, 40};
    /** Horde, panic, outbreak, supply drop, airstrike, city alarm, infect. */
    static final int[] EVENT_COST = {-1, -1, -1, 25, 150, 0, -1};

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
        int perKill = m.mode == G || m.mode == H || m.mode == P ? 4 : m.mode == A ? 5 : 1;
        return m.income * minutes + w.zombiesKilled * perKill + w.arrests * 10 + w.woundedSaved * 5 + w.freed * 2
                + w.enemyVehiclesLost * 40 + Integer.bitCount(w.gangBroken) * 150 + w.heistsStopped * 120;
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
        return m == null ? 0 : Math.max(0, (int) Math.ceil(m.limit - missionTime(w)));
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
            case G_ARREST: return "Arrest " + m.goalN + " gang members, or clear the streets, within " + time + ".";
            case G_BREAK: return "Break " + m.goalN + (m.goalN == 1 ? " gang" : " gangs") + " within " + time + ".";
            case G_PRISON: return "End the prison riot: no more than " + m.goalN + " inmates may escape.";
            case G_HEIST: return "Stop the heists: no more than " + m.goalN + (m.goalN == 1 ? " crew" : " crews") + " may get away.";
            case G_HOLD: return "Lose no more than " + m.goalN + (m.goalN == 1 ? " place" : " places") + " to the enemy for " + time + ".";
            default: return (m.mode == Z ? "Destroy every zombie" : m.mode == G ? "Break every gang" : m.mode == H ? "Stop every heist" : "Drive out the enemy") + " within " + time + ".";
        }
    }

    /** How the main objective's going, short: "7 / 15 arrested". */
    static String progress(World w) {
        Mission m = w.mission();
        if (m == null) return "";
        switch (m.goal) {
            case G_SURVIVE: return w.zombieCount() + " zombies left standing";
            case G_ARREST: return Math.min(w.arrests, m.goalN) + " / " + m.goalN + " arrested";
            case G_BREAK: return Math.min(Integer.bitCount(w.gangBroken), m.goalN) + " / " + m.goalN + " gangs broken";
            case G_HOLD: return taken(w) + " / " + m.goalN + " places lost";
            case G_PRISON: return w.recaptured + " recaptured, " + w.prisonEscaped + " / " + m.goalN + " escaped";
            case G_HEIST: return w.heistsStopped + " stopped, " + w.heistsEscaped + " / " + m.goalN + " got away";
            default: return w.zombieCount() + (m.mode == Z ? " zombies" : m.mode == G ? " gang members" : " enemy") + " left";
        }
    }

    static int taken(World w) {
        return w.checkpoints().length / 2;
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
        boolean won = false;
        switch (m.goal) {
            case G_SURVIVE:
                won = w.warResult == 1 || (out && w.warResult != 2);
                break;
            case G_ARREST:
                // (Or nobody left to arrest: the streets are clear all the same.)
                won = w.arrests >= m.goalN || w.warResult == 1;
                break;
            case G_BREAK:
                won = Integer.bitCount(w.gangBroken) >= m.goalN || w.warResult == 1;
                break;
            case G_PRISON:
                if (w.prisonEscaped > m.goalN) why = "Too many inmates escaped.";
                won = why == null && (w.warResult == 1 || out);
                break;
            case G_HEIST:
                // (Every job done, or the end of the shift, with no more than so many away: won.)
                if (w.heistsEscaped > m.goalN) why = "Too many crews got away.";
                won = why == null && (w.warResult == 1 || (w.hostileReserve <= 0 && w.heists.isEmpty() && w.heistsStarted > 0) || out);
                break;
            case G_HOLD:
                if (taken(w) > m.goalN) why = "The enemy has taken too much of the city.";
                won = why == null && (out || w.warResult == 1);
                break;
            default:
                won = w.warResult == 1;
                break;
        }
        if (!won && why == null) {
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
