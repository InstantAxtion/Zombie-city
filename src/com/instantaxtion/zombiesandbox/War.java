package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * The war as a whole: the strain, the city's leaders and what they decide, alerts, supplies, escalation, the government, the Guard and militias.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class War {
    private final World w;

    War(World w) {
        this.w = w;
    }

    int strainTint() {
        return w.strainName == null ? 0 : World.STRAIN_TINTS[Math.abs(w.strainName.hashCode() % World.STRAIN_TINTS.length)];
    }

    /** Rolls how ready the city is (a hidden factor, revealed on the radio once the outbreak is under way). */
    void applyReadiness() {
        float r = w.rnd.nextFloat();
        w.readiness = r < 0.2f ? 1 : r < 0.35f ? 2 : r < 0.5f ? 3 : r < 0.62f ? 4 : 0;
        if (w.readiness == 1) {
            // (Caught unprepared: the towns around can spare only half as many, and the army is short too.)
            w.dispatch.policeReserve /= 2;
            w.dispatch.squadReserve = w.dispatch.squadReserve * 3 / 4;
            for (Entity e : w.entities) if (e.isArmed()) e.reserve /= 2;
        } else if (w.readiness == 3) {
            // ((16.9) A few more, not a fifth of everyone: a small share of people carry a gun.)
            for (Entity e : w.entities) if (e.type == Entity.CIVILIAN && !e.hasGun && w.rnd.nextFloat() < 0.06f) w.population.armCivilian(e, 12 + w.rnd.nextInt(3) * 6);
        } else if (w.readiness == 4) {
            if (w.dispatch.policeReserve > 0) w.dispatch.policeReserve += 40;
            if (w.dispatch.squadReserve > 0) w.dispatch.squadReserve += 24;
        }
    }

    void rollStrain() {
        int[][] pairs = {{World.TR_FAST_TURN, World.TR_SLOW_TURN}, {World.TR_VIRULENT, World.TR_WEAK_BITE}, {World.TR_TOUGH, World.TR_FRAIL}, {World.TR_KEEN, World.TR_DULL}};
        for (int[] p : pairs) if (w.rnd.nextFloat() < 0.55f) w.trait[p[w.rnd.nextInt(2)]] = true;
        if (w.rnd.nextFloat() < 0.3f) w.trait[World.TR_SWIFT] = true;
        if (w.rnd.nextFloat() < 0.25f) w.trait[World.TR_RESTLESS] = true;
        w.strainName = "Strain " + (char) ('A' + w.rnd.nextInt(26)) + "-" + (1 + w.rnd.nextInt(99));
        w.command = w.rnd.nextInt(World.CMD_COUNT);
        // ((12.4) A few more ways each strain is its own: rolled from the strain and the city, so nothing else that's
        // rolled after this comes out differently.)
        java.util.Random sr = new java.util.Random(w.strainName.hashCode() * 7919L + w.city.cfg.seed);
        int[][] more = {{World.TR_PACK, World.TR_LONER}, {World.TR_QUICK_RISE, World.TR_SLOW_RISE}};
        for (int[] p : more) if (sr.nextFloat() < 0.5f) w.trait[p[sr.nextInt(2)]] = true;
        if (sr.nextFloat() < 0.25f) w.trait[World.TR_RUNNERS] = true;
        if (sr.nextFloat() < 0.15f) w.trait[World.TR_BRUTES] = true;
        if (sr.nextFloat() < 0.3f) w.trait[World.TR_RELENTLESS] = true;
    }

    /** How much sooner (below 1) or later the army escalates. */
    float escalationPace() {
        return w.command == World.CMD_HAWK ? 0.7f : w.command == World.CMD_SLOW ? 1.45f : w.command == World.CMD_CAUTIOUS ? 1.15f : 1f;
    }

    /**
     * (11.5) The city's leaders weigh up how it's going every so often and act - each at most once, and which and
     * when depends on who they are and what the dead are doing: a curfew, sealing off the one district where it's
     * all happening, opening the gun stores, calling the Guard out early, or a convoy of ammunition to the police.
     */
    void leadersDecide(float dt) {
        if (!w.outbreak || w.alert < 1 || (w.decideCd -= dt) > 0) return;
        w.decideCd = 30 + w.rnd.nextFloat() * 30;
        int zs = w.zombieCount();
        if (zs < 6) return;
        float[] wt = new float[5];
        int[] bit = {World.DEC_CURFEW, World.DEC_QUARANTINE, World.DEC_GUNS, World.DEC_GUARD, World.DEC_CONVOY};
        wt[0] = w.command == World.CMD_CAUTIOUS ? 3 : w.command == World.CMD_ARMED ? 0.3f : 1;
        int worst = worstDistrict(), inWorst = 0;
        if (worst >= 0) {
            for (int i = 0, n = w.entities.size(); i < n; i++) {
                Entity e = w.entities.get(i);
                if (!e.dead && e.isZombie() && w.city.districtIndex(e.x, e.y) == worst) inWorst++;
            }
            // (Only worth it while most of the dead are in the one place.)
            if (inWorst >= 10 && inWorst > zs * 0.6f && (w.city.mainBase() != null || w.counts[Entity.SOLDIER] > 0))
                wt[1] = w.command == World.CMD_HAWK ? 3 : w.command == World.CMD_SLOW ? 0.4f : 1;
        }
        // ((16.9) No handing out guns: there are no gun stores to open.)
        if (!w.guardCalled && w.alert >= 2 && w.city.armory != null) wt[3] = w.command == World.CMD_CAUTIOUS || w.command == World.CMD_HAWK ? 2 : 0.6f;
        if (w.alert >= 2 && w.counts[Entity.COP] > 0) wt[4] = w.command == World.CMD_SLOW ? 0.3f : 0.8f;
        // (The slow ones mostly talk about it.)
        float act = w.command == World.CMD_SLOW ? 0.35f : 0.75f;
        if (w.rnd.nextFloat() > act) return;
        float total = 0;
        for (int k = 0; k < wt.length; k++) if ((w.decisions & bit[k]) != 0) wt[k] = 0; else total += wt[k];
        if (total <= 0) return;
        float pick = w.rnd.nextFloat() * total;
        int k = 0;
        while (k < wt.length - 1 && (pick -= wt[k]) > 0) k++;
        if (wt[k] <= 0) return;
        w.decisions |= bit[k];
        float mx = w.city.worldW() / 2f, my = w.city.worldH() / 2f;
        switch (bit[k]) {
            case World.DEC_CURFEW: {
                w.banner("CURFEW", "Everyone off the streets and into a safe zone until further notice.");
                w.dispatch.say(Dispatch.WHO_INFO, null, "News: The Mayor has declared a curfew. Keep away from the infected and make for a safe zone.", mx, my);
                for (int i = 0, n = w.entities.size(); i < n; i++) {
                    Entity e = w.entities.get(i);
                    if (!e.dead && e.type == Entity.CIVILIAN && !e.aware && w.rnd.nextFloat() < 0.8f) e.aware = true;
                }
                break;
            }
            case World.DEC_QUARANTINE: {
                if (sealDistrict(worst) > 0) w.escalation = Math.max(w.escalation, 1);
                break;
            }
            case World.DEC_GUARD:
                w.dispatch.say(Dispatch.WHO_INFO, null, "News: The governor isn't waiting: the National Guard is being called out now.", mx, my);
                callNationalGuard();
                break;
            case World.DEC_CONVOY:
                // An ammunition run to every police station.
                for (City.Facility f : w.city.facilities)
                    if (f.kind == City.FACILITY_POLICE && !f.fallen) f.ammo = Math.max(f.ammo, World.ARMOURY_POLICE);
                w.dispatch.say(Dispatch.WHO_POLICE, null, "Police Command: Ammunition trucks have reached every station. Officers, restock and get back out there.", mx, my);
                break;
        }
    }

    /** How long a bite takes to turn someone, for this strain. */
    /** ((29.2) The bitten turn sooner: six tenths of the time they took before (on top of the strain's own pace). */
    static final float TURN_SOONER = 0.6f;

    float turnTime(float base) {
        return base * TURN_SOONER * (w.trait[World.TR_FAST_TURN] ? 0.45f : w.trait[World.TR_SLOW_TURN] ? 1.8f : 1f);
    }

    void reveal(int t) {
        if (!w.trait[t] || w.traitKnown[t]) return;
        w.traitKnown[t] = true;
        w.dispatch.say(Dispatch.WHO_INFO, null, World.TRAIT_REVEALS[t], w.city.worldW() / 2, w.city.worldH() / 2);
    }

    /** Once a second during an outbreak: who is winning, turning points, and what's been learned about the strain. */
    void updateWar() {
        if (w.turned >= 3) {
            reveal(World.TR_FAST_TURN);
            reveal(World.TR_SLOW_TURN);
        }
        if (w.bites >= 10) {
            reveal(World.TR_VIRULENT);
            reveal(World.TR_WEAK_BITE);
        }
        if (w.zombiesKilled >= 15) {
            reveal(World.TR_TOUGH);
            reveal(World.TR_FRAIL);
        }
        if (w.turned >= 6) {
            reveal(World.TR_QUICK_RISE);
            reveal(World.TR_SLOW_RISE);
        }
        if (w.turned >= 12) {
            reveal(World.TR_RUNNERS);
            reveal(World.TR_BRUTES);
        }
        if (w.outbreakTime >= 45 && w.zombies >= 12) {
            reveal(World.TR_PACK);
            reveal(World.TR_LONER);
        }
        if (w.outbreakTime >= 70) reveal(World.TR_RELENTLESS);
        if (w.outbreakTime >= 20) reveal(World.TR_SWIFT);
        if (w.outbreakTime >= 25 && !w.readinessKnown) {
            w.readinessKnown = true;
            if (w.readiness > 0) w.dispatch.say(Dispatch.WHO_INFO, null, World.READINESS_NEWS[w.readiness], w.city.worldW() / 2, w.city.worldH() / 2);
        }
        if (w.outbreakTime >= 45) {
            reveal(World.TR_KEEN);
            reveal(World.TR_DULL);
        }
        // ((17.3) The fighting strength of each side. The police and soldiers counted whether on foot or riding in a car or
        // truck (half of them were left out); the unarmed only a little (twenty thousand of them outweighed the whole
        // army before, and the bar sat at 97 per cent); gang members and enemy soldiers counted at all (they weren't);
        // and the help still to come on each side, at half.)
        float hs = w.counts[Entity.COP] * 3 + w.counts[Entity.SOLDIER] * 4, zs = 0;
        hs += (w.dispatch.policeReserve * 3 + w.dispatch.squadReserve * 4) * 0.5f;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.dead) continue;
            switch (e.type) {
                case Entity.CIVILIAN: hs += e.hasGun ? 1.2f : 0.03f; break;
                case Entity.DOG: hs += 0.05f; break;
                case Entity.ZOMBIE: case Entity.ZOMBIE_DOG: zs += 1; break;
                case Entity.RUNNER: zs += 1.4f; break;
                case Entity.BRUTE: zs += 4; break;
                case Entity.BRUISER: zs += 2.2f; break;
                case Entity.GIANT: zs += 14; break;
                case Entity.SCREAMER: zs += 1.5f; break;
                case Entity.ARMOURED: zs += 2.5f; break;
            }
            if (e.infected) zs += 0.8f;
        }
        for (int i = 0, n = w.fleet.vehicles.size(); i < n; i++)
            if (w.fleet.vehicles.get(i).type == Fleet.TANK && !w.fleet.vehicles.get(i).broken) hs += 12;
        hs += w.hiding * 0.02f;
        zs += w.lurking;
        if (w.trait[World.TR_TOUGH]) zs *= 1.25f;
        if (w.trait[World.TR_FRAIL]) zs *= 0.8f;
        float b = hs + zs > 0 ? hs / (hs + zs) : 0.5f;
        // ((17.3) Straight to where it stands when the fighting starts, not eased up from halfway.)
        if (w.warFresh) {
            w.warFresh = false;
            w.warBalance = b;
        } else w.warBalance += (b - w.warBalance) * 0.25f;
        if (w.warBalance > 0.62f && w.warLead != 1) {
            if (w.warLead == -1) w.banner("THE TIDE IS TURNING", "People are fighting back. The dead are losing ground.");
            w.warLead = 1;
        } else if (w.warBalance < 0.38f && w.warLead != -1) {
            if (w.warLead == 1) w.banner("THE DEAD ARE WINNING", "The city is slipping away. Every survivor counts now.");
            else if (w.outbreakTime > 30) w.banner("THE CITY IS IN DANGER", "The dead outnumber the living.");
            w.warLead = -1;
        }
    }

    void updateAlert() {
        // A good while after the last of them is gone, the all-clear: people get back to their lives.
        if (w.alert > 0 && w.zombies == 0 && w.lurking == 0 && w.calmTime > 40) {
            w.alert = 0;
            for (int i = 0, n = w.entities.size(); i < n; i++) w.entities.get(i).aware = false;
            w.aftermathLife.calmDown();
            w.dispatch.say(Dispatch.WHO_INFO, null, "News: The all-clear has been given. Residents can go about their day.",
                    w.city.worldW() / 2, w.city.worldH() / 2);
            return;
        }
        if (!w.outbreak || w.zombies == 0) return;
        int was = w.alert;
        // (Nobody reports what nobody has seen: one of them alone out in the woods, chasing deer, is news to no one.)
        boolean seen = w.dispatch.calls > 0 || w.bites > 0;
        // (10.28) Slow to dawn on anyone: a few strange calls first, then the news, and only well into it the
        // emergency broadcast that sends everyone indoors.
        if (w.alert < 1 && (w.dispatch.calls >= 4 || w.bites >= 6 || (w.outbreakTime > 60 && seen))) {
            w.alert = 1;
            w.stats.timeline("It's on the news");
        }
        // ((11.5) The emergency broadcast goes out in good time, City Hall or no City Hall.)
        float t2 = 150;
        int z2 = 20, c2 = 14;
        if (w.alert < 2 && seen && (w.outbreakTime > t2 || w.zombies >= z2 || w.dispatch.calls >= c2)) {
            w.alert = 2;
            w.stats.timeline("Emergency broadcast: everyone to safety");
        }
        // ((11.7) What everyone learns: that it spreads by the bite.)
        if (!w.biteKnown && w.alert >= 1 && w.bites >= 8) {
            w.biteKnown = true;
            w.stats.timeline("Health officials: it spreads by the bite");
            w.dispatch.say(Dispatch.WHO_INFO, null, "News: Health officials say the illness spreads through bites. Anyone bitten should be kept apart and watched. Hospitals are asking people not to come in.",
                    w.city.worldW() / 2, w.city.worldH() / 2);
        }
        if (w.alert == was) return;
        String where = w.outbreakPlace != null ? w.outbreakPlace : "the city";
        if (w.alert == 1) {
            w.dispatch.say(Dispatch.WHO_INFO, null, "News: Reports of people attacking people near " + where
                    + ". Police urge residents to keep away.", w.city.worldW() / 2, w.city.worldH() / 2);
        } else {
            w.banner("EMERGENCY BROADCAST", "Get away from the infected and make for a safe zone. Do not approach them.");
            for (int i = 0, n = w.entities.size(); i < n; i++) {
                Entity e = w.entities.get(i);
                if (!e.dead && e.type == Entity.CIVILIAN) e.aware = true;
            }
        }
    }

    /** Broadcasts get out: the power's on and City Hall (if the city has one) is still running. */
    boolean broadcasting() {
        return !w.blackout;
    }

    /** Once a second: what the government buildings are doing, and whether they've fallen. */
    void government() {
        // ((11.5) The government buildings' work goes on whatever happens to the buildings: road crews board up
        // shelters through the outbreak; nobody tracks which office has fallen, and the jail has nobody to break out.)
        // ((16.9) Not boarding up shelters any more: nobody barricades buildings.)
    }

    /** How long the dead have held a building (it falls at 15). */
    float threat(City.Building b, float t) {
        boolean held = b.collapsed || w.spatial.countZombiesNear(b.doorX, b.doorY, 90) >= 5;
        return held ? t + 1 : Math.max(0, t - 2);
    }

    boolean lost(City.Building b, float t, boolean lost) {
        if (!lost) return t > 15;
        return !(t == 0 && !b.collapsed && w.spatial.countZombiesNear(b.doorX, b.doorY, 200) == 0 && w.spatial.peopleNear(b.doorX, b.doorY, 120));
    }

    /**
     * The jail: its inmates stay locked up unless the dead get in (some are bitten, the rest run), the power
     * fails (the cell doors open), or there are no police left to guard them.
     */
    void jail() {
        City.Building j = w.city.jail;
        if (j == null || w.jailBroken || w.inmates <= 0) return;
        w.jailThreat = threat(j, w.jailThreat);
        w.noGuards = w.outbreak && w.outbreakTime > 150 && w.peakCops > 0 && w.counts[Entity.COP] <= Math.max(1, w.peakCops / 8) ? w.noGuards + 1 : 0;
        String why = null;
        int bitten = 0;
        if (w.jailThreat > 10) {
            why = "The dead have got into " + j.name + ".";
            bitten = Math.max(1, w.inmates / 3);
        } else if (w.blackout) why = "With the power out, the cell doors at " + j.name + " have failed.";
        else if (w.noGuards > 30) why = "The last of the guards have abandoned " + j.name + ".";
        if (why == null) return;
        w.jailBroken = true;
        int n = w.inmates;
        w.inmates = 0;
        for (int i = 0; i < n; i++) {
            float[] p = w.city.findWalkable(j.doorX + w.rnd.nextFloat() * 30 - 15, j.doorY + w.rnd.nextFloat() * 30 - 15);
            if (p == null) p = new float[]{j.doorX, j.doorY};
            if (i < bitten) {
                Entity z = w.population.spawn(Entity.ZOMBIE, p[0], p[1]);
                if (z != null) w.turned++;
                continue;
            }
            // (10.50) No raiders: the inmates who get out are just people in prison orange, running for it.
            Entity r = w.population.spawn(Entity.CIVILIAN, p[0], p[1]);
            if (r == null) continue;
            r.body = 0xFFE07A2A;
        }
        w.banner("JAILBREAK", why + " " + (n - bitten) + " inmates are loose.");
        w.dispatch.say(Dispatch.WHO_POLICE, null, "Police Command: Jailbreak at " + j.name + ". " + (n - bitten)
                + " escaped inmates heading into town. Approach with caution.", j.doorX, j.doorY);
    }

    /** The power station falls if zombies hold it for long enough, and comes back once cleared. */
    void keyBuildings() {
        if (!w.powerChecked) {
            w.powerChecked = true;
            for (City.Building b : w.city.buildings) if (b.kind == City.POWER) w.powerStation = b;
        }
        if (w.powerStation != null) {
            City.Building p = w.powerStation;
            boolean held = p.collapsed || w.spatial.countZombiesNear(p.doorX, p.doorY, 90) >= 6;
            w.powerThreat = held ? w.powerThreat + 1 : Math.max(0, w.powerThreat - 2);
            if (!w.blackout && w.powerThreat > 20) {
                w.blackout = true;
                w.dispatch.say(Dispatch.WHO_INFO, null, "The power station has been overrun. The power is out across the city: no more sirens or broadcasts.",
                        p.doorX, p.doorY);
            } else if (w.blackout && !p.collapsed && w.powerThreat == 0 && w.spatial.countZombiesNear(p.doorX, p.doorY, 200) == 0
                    && w.spatial.peopleNear(p.doorX, p.doorY, 120)) {
                w.blackout = false;
                w.dispatch.say(Dispatch.WHO_INFO, null, "Engineers have the power station running again. The lights are back on.", p.doorX, p.doorY);
            }
        }
    }

    /**
     * When the army has nothing left to send and the war is going badly, the governor calls out the National
     * Guard (once): two trucks of guardsmen drive in from the edge of town to protect the civilians.
     */
    void callNationalGuard() {
        // (A Guard headquarters at the base: the governor doesn't wait so long.)
        City.Facility hq = w.city.mainBase();
        boolean local = hq != null && hq.baseType == City.BT_GUARD;
        // (The Mayor asks the governor; with City Hall gone, the governor takes a lot longer to hear. With the
        // town's own armory overrun, the Guard has to come from out of town, which takes longer still.)
        boolean hall = true;
        City.Building arm = w.city.armory;
        boolean muster = arm != null && !w.armoryLost && !arm.collapsed;
        float wait = local ? 50 : hall && !w.hallLost ? 70 : hall ? 200 : 90;
        if (arm != null && !muster) wait += 60;
        // (Since 10.10 the governor doesn't only wait for the army to run out: a city with no army base of its
        // own gets the Guard as soon as it's at war, and a big outbreak anywhere brings them out. If the first
        // call-up is wiped out and it's still going badly, a second follows.)
        boolean natural = false, second = false;
        if (w.city.cfg.nature() && w.outbreak) {
            boolean noArmy = hq == null;
            boolean big = w.zombieCount() >= Math.max(40, w.humanCount() / 8);
            natural = big || (noArmy && w.alert >= 2);
            if (w.guardCalled && w.guardWave == 1 && w.time - w.guardCalledAt > 240 && w.warBalance < 0.4f) {
                int left = 0;
                for (int i = 0, n = w.entities.size(); i < n && left == 0; i++) {
                    Entity e = w.entities.get(i);
                    if (!e.dead && e.type == Entity.SOLDIER && e.role == Entity.ROLE_GUARD) left++;
                }
                for (int i = 0; i < w.fleet.vehicles.size(); i++) if (w.fleet.vehicles.get(i).guardUnit) left++;
                second = left == 0;
            }
        }
        if (second) {
            w.guardCalled = false;
            muster = false;
        }
        if (w.guardCalled || !w.outbreak || w.outbreakTime < (natural ? Math.min(wait, 45) : wait)) return;
        if (!natural && !second && (w.warBalance > (local ? 0.55f : 0.45f) || (w.dispatch.squadReserve > 0 && !local) || w.readiness == 1)) return;
        w.guardCalled = true;
        w.guardWave++;
        w.guardCalledAt = w.time;
        float tx = w.city.worldW() / 2, ty = w.city.worldH() / 2;
        Dispatch.Incident to = null;
        if (!w.dispatch.zones.isEmpty()) {
            tx = w.dispatch.zones.get(0).x;
            ty = w.dispatch.zones.get(0).y;
        } else if (w.city.cfg.civic >= 61) {
            // ((33.0) Map version 61: with no safe zone to go to, to the fight most short of soldiers (Dispatch.armyShort),
            // not the middle of the map - they drove to an empty square downtown and stood there with the city burning.)
            to = w.dispatch.armyShort(tx, ty);
            if (to != null) {
                tx = to.x;
                ty = to.y;
            }
        }
        float[] from;
        if (muster) from = new float[]{arm.doorX, arm.doorY};
        else {
            from = w.city.edgeRoad(w.rnd);
            if (from == null) from = new float[]{20, w.city.worldH() / 2};
        }
        int trucks = muster ? 3 : 2;
        int before = w.fleet.vehicles.size();
        for (int k = 0; k < trucks; k++) w.fleet.send(Entity.SOLDIER, 6, from[0], from[1], tx, ty, to, null, w.city.placeName(tx, ty));
        for (int i = before; i < w.fleet.vehicles.size(); i++) {
            Fleet.Vehicle v = w.fleet.vehicles.get(i);
            v.guardUnit = true;
            // (Reservists take a little while to get to the armory and draw their kit.)
            if (muster) v.timer = 20 + (i - before) * 4;
            // (From out of town, one after another down the road rather than all on one spot.)
            else v.timer = 0.5f + (i - before) * 5;
            v.reinforcement = true;
            // (A Humvee leads the convoy.)
            if (w.city.cfg.nature() && i == before) {
                w.fleet.setKind(v, Fleet.K_HUMVEE);
                v.passengers = Fleet.HUMVEE_SEATS;
            }
        }
        String[] g = Country.guard();
        if (w.fleet.vehicles.size() > before)
            w.dispatch.trackConvoy(Entity.SOLDIER, Country.guard()[1].toUpperCase(), before, from[0], from[1], tx, ty, w.city.placeName(tx, ty));
        if (w.fleet.vehicles.size() > before)
            w.dispatch.say(Dispatch.WHO_MILITARY, null, g[2] + ": " + (hall && !w.hallLost ? "At the Mayor's request, I" : "I")
                    + "'m calling out the " + g[1] + ". " + (muster ? "Guardsmen are mustering at " + arm.name + "; " + World.count(trucks, "truck", "trucks")
                    + " will head for " : "With " + (arm != null ? arm.name + " overrun" : "no armory in town")
                    + ", " + World.count(trucks, "truck is", "trucks are") + " coming from out of town to ") + w.city.placeName(tx, ty) + ".", from[0], from[1]);
        else w.guardCalled = false;
    }

    /**
     * (13.9) Ammo trucks: every ten seconds, a fight where two or more soldiers are running low, a long way from the
     * base and with no army truck carrying rounds nearby, gets a truck from the base with a load of them, which waits
     * there while the soldiers fill up.
     */
    void fieldSupplies() {
        if (!w.outbreak || ((int) w.time) % 10 != 0) return;
        for (int i = 0; i < w.dispatch.incidents.size(); i++) {
            Dispatch.Incident inc = w.dispatch.incidents.get(i);
            if (inc.resolved || w.fleet.ammoTruckFor(inc)) continue;
            // (Those there, or just set off for the base: a soldier out of rounds is let go from the call at once.
            // ((16.5) Out to 450, not 350: this is looked at every ten seconds, and in ten a soldier who set off on foot
            // is 370 away - a squad that ran dry just after a look had gone by the next, and no truck came.))
            int low = 0;
            for (int k = 0, n = w.entities.size(); k < n && low < 2; k++) {
                Entity e = w.entities.get(k);
                if (e.dead || e.type != Entity.SOLDIER || e.ride != null || World.hypot(e.x - inc.x, e.y - inc.y) > 450) continue;
                float stock = (e.ammo + e.reserve) / (float) Math.max(1, e.magSize + World.fullReserve(e));
                if (stock < 0.35f) low++;
            }
            if (low < 2 || w.fleet.ammoTruckNear(inc.x, inc.y, World.TRUCK_REACH) != null) continue;
            City.Facility f = w.city.nearestFacility(City.FACILITY_BASE, inc.x, inc.y);
            if (f == null || f.fallen || f.ammo < Fleet.AMMO_KEEP + 400 || World.hypot(f.x - inc.x, f.y - inc.y) < World.AMMO_TRUCK_FROM
                    || w.fleet.ammoTrucksOut() >= Fleet.AMMO_TRUCKS_OUT) continue;
            Fleet.Vehicle v = w.fleet.sendAmmoTruck(f, inc);
            if (v == null) continue;
            w.dispatch.say(Dispatch.WHO_MILITARY, null, "Military: An ammo truck is leaving " + f.name + " for the squads at " + inc.place + ".", inc.x, inc.y);
            // (Those low and near, or already setting off for the base, wait for it instead.)
            for (int k = 0, n = w.entities.size(); k < n; k++) {
                Entity e = w.entities.get(k);
                if (e.dead || e.type != Entity.SOLDIER || e.ride != null || e.rig != null || World.hypot(e.x - inc.x, e.y - inc.y) > 600) continue;
                float stock = (e.ammo + e.reserve) / (float) Math.max(1, e.magSize + World.fullReserve(e));
                if (stock >= 0.35f || (e.task != Dispatch.T_RESUPPLY && e.task != Dispatch.T_NONE && e.task != Dispatch.T_RESPOND)) continue;
                w.dispatch.releaseForResupply(e);
                e.task = Dispatch.T_RESUPPLY;
                e.supplyTruck = v;
            }
        }
    }

    /** Supply trucks: the army keeps safe zones in ammunition, while its own armoury lasts. */
    void supplies() {
        if (!w.outbreak || ((int) w.time) % 15 != 0) return;
        for (int i = 0; i < w.dispatch.zones.size(); i++) {
            Dispatch.SafeZone z = w.dispatch.zones.get(i);
            if (z.removed || z.supplyComing || (z.ammo > 150 && z.food > z.foodMax * 0.35f)) continue;
            City.Facility best = null;
            float bd = Float.MAX_VALUE;
            for (City.Facility f : w.city.facilities) {
                if ((f.kind != City.FACILITY_BASE && f.kind != City.FACILITY_POLICE) || f.ammo < 600) continue;
                float d = (f.x - z.x) * (f.x - z.x) + (f.y - z.y) * (f.y - z.y);
                if (d < bd) {
                    bd = d;
                    best = f;
                }
            }
            if (best == null) continue;
            // ((26.0) A supply convoy, escorted, rather than a lone truck - or the truck, if no convoy can go now.)
            if (w.convoys.supply(z, null, null, 600) != null) {
                best.ammo -= 500;
                z.supplyComing = true;
            } else if (w.fleet.sendSupply(best, z, 500)) {
                best.ammo -= 500;
                z.supplyComing = true;
                w.dispatch.say(Dispatch.WHO_MILITARY, null, "Military: " + z.place + " is running low. A supply truck is leaving "
                        + best.name + ".", z.x, z.y);
            }
        }
    }

    void escalate() {
        if (!w.outbreak) return;
        boolean army = w.city.mainBase() != null || w.counts[Entity.SOLDIER] > 0;
        if (!army) return;
        // ((11.5) Sooner or later, depending on who's in charge.)
        float pace = escalationPace(), bias = w.command == World.CMD_HAWK ? 0.06f : w.command == World.CMD_SLOW ? -0.06f : 0;
        if (w.escalation == 0 && w.outbreakTime > 240 * pace && w.warBalance < 0.42f + bias) {
            int d = worstDistrict();
            if (d >= 0 && sealDistrict(d) > 0) w.escalation = 1;
            else w.escalation = 1;
        }
        // ((14.2) Then the whole city: every road out sealed, nobody leaves.)
        if (!w.quarantined && w.escalation >= 1 && w.outbreakTime > 330 * pace && w.warBalance < 0.36f + bias) quarantineCity();
        if (w.escalation == 1 && w.outbreakTime > 420 * pace && w.warBalance < 0.3f + bias) {
            w.escalation = 2;
            if (callAirStrike(8, true)) w.strikes = 1;
            w.strikeCd = 120 * pace;
        } else if (w.escalation >= 2 && w.firebombTime <= 0 && w.strikes < World.MAX_STRIKES && (w.strikeCd -= 1) <= 0) {
            // ((14.2) And again, while it keeps getting worse: the biggest crowd of the dead each time.)
            w.strikeCd = (90 + w.rnd.nextFloat() * 60) * pace;
            if (w.warBalance < 0.25f + bias && callAirStrike(12, false)) w.strikes++;
        }
    }

    /**
     * An air strike on the thickest crowd of the dead (at least min of them together), in 60 seconds: everyone there
     * is warned to get out. False if there's no crowd big enough.
     */
    boolean callAirStrike(int min, boolean first) {
        float bx = 0, by = 0;
        int best = 0;
        // ((29.7) Every so many of the dead, not every so many of everyone: a pack of a dozen in a city of thirty thousand
        // fell between the ones looked at, and no strike was called on it.)
        int every = Math.max(1, w.zombies / 300), seen = 0;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity z = w.entities.get(i);
            if (z.dead || !z.isZombie() || seen++ % every != 0) continue;
            int c = w.spatial.countZombiesNear(z.x, z.y, 120);
            if (c > best) {
                best = c;
                bx = z.x;
                by = z.y;
            }
        }
        if (best < min) return false;
        w.firebombX = bx;
        w.firebombY = by;
        w.firebombTime = 60;
        w.firebombPlace = w.city.placeName(bx, by);
        w.banner(first ? "AIR STRIKE IN 60 SECONDS" : "ANOTHER AIR STRIKE IN 60 SECONDS", "The army is going to bomb " + w.firebombPlace + ". Get everyone out!");
        w.dispatch.say(Dispatch.WHO_MILITARY, null, (first ? "Military: We're losing the city. In 60 seconds we bomb " : "Military: Another strike. In 60 seconds we bomb ")
                + w.firebombPlace + ". Anyone still there, get out now!", bx, by);
        w.stats.timeline("Air strike called on " + w.firebombPlace, bx, by);
        return true;
    }

    /** (14.2) The whole city sealed off: nobody leaves - those on their way out are turned back, the buses and trains stop. */
    void quarantineCity() {
        w.quarantined = true;
        if (w.exits == null) w.exodus.findExits();
        w.banner("CITY QUARANTINED", "The army has closed every road out. Nobody leaves, infected or not.");
        w.dispatch.say(Dispatch.WHO_MILITARY, null, "Military: The city is under quarantine. Every road out is closed and guarded. "
                + "Nobody leaves - go to a safe zone.", w.city.worldW() / 2, w.city.worldH() / 2);
        w.stats.timeline("The city is quarantined");
        turnBackLeavers();
        w.fleet.exodusOver();
    }

    /** (14.2) The outbreak's over: the roads out open again. */
    void liftQuarantine() {
        w.quarantined = false;
        w.dispatch.say(Dispatch.WHO_MILITARY, null, "Military: The quarantine is lifted. The roads out of the city are open again.",
                w.city.worldW() / 2, w.city.worldH() / 2);
    }

    /** Whoever was on their way out of the city turns round (and goes home, if they have one). */
    void turnBackLeavers() {
        for (int i = 0, m = w.entities.size(); i < m; i++) {
            Entity e = w.entities.get(i);
            if (!e.dead && e.task == Dispatch.T_LEAVE) {
                e.exitStop = -1;
                e.task = e.home != null ? Dispatch.T_HOMECOMING : Dispatch.T_NONE;
                e.path = null;
                e.goalCd = 0;
            }
        }
    }

    /** (14.2) The roads out of the city, for drawing the checkpoints while it's quarantined (null if not known). */
    float[][] exits() {
        return w.exits;
    }

    void updateFirebomb(float dt) {
        if (w.firebombTime <= 0) return;
        float before = w.firebombTime;
        w.firebombTime -= dt;
        // People in the target area run.
        if (((int) before) != ((int) w.firebombTime))
            for (int i = 0, n = w.entities.size(); i < n; i++) {
                Entity e = w.entities.get(i);
                if (e.dead || e.isZombie() || e == w.controlled) continue;
                if ((e.x - w.firebombX) * (e.x - w.firebombX) + (e.y - w.firebombY) * (e.y - w.firebombY) > 320 * 320) continue;
                e.fleeTimer = 3;
                e.threatX = w.firebombX;
                e.threatY = w.firebombY;
            }
        if (w.firebombTime > 0) return;
        w.firebombTime = 0;
        for (int k = 0; k < 14; k++) {
            float a = w.rnd.nextFloat() * World.TAU, r = (float) Math.sqrt(w.rnd.nextFloat()) * 230;
            float x = w.firebombX + (float) Math.cos(a) * r, y = w.firebombY + (float) Math.sin(a) * r;
            w.combat.blastLater(x, y, 70, 500, k * 0.25f);
        }
        w.emit(Sfx.JET, w.firebombX, w.firebombY);
        w.stats.highlight("Bombing of " + w.firebombPlace, w.firebombX, w.firebombY);
        w.dispatch.say(Dispatch.WHO_MILITARY, null, "Military: Bombs away on " + w.firebombPlace + ".", w.firebombX, w.firebombY);
    }

    int worstDistrict() {
        int[] z = new int[w.city.districts.size()];
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || !e.isZombie()) continue;
            int d = w.city.districtIndex(e.x, e.y);
            if (d >= 0) z[d]++;
        }
        int best = -1;
        for (int d = 0; d < z.length; d++) if (z[d] > 5 && (best < 0 || z[d] > z[best])) best = d;
        return best;
    }

    boolean checkpointAt(float x, float y) {
        for (int k = 0, n = w.city.checkpoints.size(); k < n; k++) {
            float[] cp = w.city.checkpoints.get(k);
            if (Math.abs(cp[0] - x) < City.T && Math.abs(cp[1] - y) < City.T) return true;
        }
        return false;
    }

    /** Quarantine: barricades across every road out of a district. */
    int sealDistrict(int d) {
        int placed = 0, w = this.w.city.w;
        for (int ty = 1; ty < this.w.city.h - 1 && placed < 70; ty++)
            for (int tx = 1; tx < w - 1 && placed < 70; tx++) {
                int i = ty * w + tx;
                if (this.w.city.tiles[i] != City.ROAD) continue;
                float x = tx * City.T + City.T / 2f, y = ty * City.T + City.T / 2f;
                if (this.w.city.districtIndex(x, y) != d) continue;
                boolean edge = false;
                int[] nb = {i - 1, i + 1, i - w, i + w};
                for (int j : nb) {
                    if (this.w.city.tiles[j] != City.ROAD) continue;
                    if (this.w.city.districtIndex((j % w) * City.T + 8, (j / w) * City.T + 8) != d) edge = true;
                }
                // ((21.2) Not across a checkpoint's own road: the army's already holding it there, and a wall put up
                // on top of the checkpoint left its guards standing in a fence.)
                if (edge && !checkpointAt(x, y) && this.w.sandbox.placeBarricade(x, y) != null) placed++;
            }
        if (placed > 0) {
            City.District dd = this.w.city.districts.get(d);
            this.w.banner("QUARANTINE", "The army has sealed off " + dd.name + ".");
            this.w.stats.timeline("District sealed off: " + dd.name, dd.cx, dd.cy);
            this.w.dispatch.say(Dispatch.WHO_MILITARY, null, "Military: We're sealing off " + dd.name + ". Every road out is blocked. "
                    + "Nobody in or out.", dd.cx, dd.cy);
        }
        return placed;
    }

    /** Armed residents band together and hold a building, with their neighbours sheltering inside. */
    void militias() {
        if (!w.outbreak || ((int) w.time) % 10 != 0) return;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || e.type != Entity.CIVILIAN || !e.hasGun || e.militia || e.ammo + e.reserve < 6 || e == w.controlled) continue;
            java.util.ArrayList<Entity> group = new java.util.ArrayList<Entity>();
            // ((16.8) From those in the grid round them, not everyone in the city for each: once the gun stores open
            // there are thousands with a gun, and this looked at tens of thousands for each one.)
            for (int k = 0, m = w.spatial.gatherLiving(e.x, e.y, 140); k < m && group.size() < 6; k++) {
                Entity o = w.gathered[k];
                if (o.dead || o.type != Entity.CIVILIAN || !o.hasGun || o.militia || o == w.controlled || o.ammo + o.reserve < 6) continue;
                if ((o.x - e.x) * (o.x - e.x) + (o.y - e.y) * (o.y - e.y) < 140 * 140) group.add(o);
            }
            if (group.size() < 3) continue;
            City.Building b = w.civilianAI.shelterWithin(e, 200);
            if (b == null) continue;
            for (int k = 0; k < group.size(); k++) {
                Entity o = group.get(k);
                o.militia = true;
                o.task = Dispatch.T_NONE;
                o.leader = null;
                float a = k * World.TAU / group.size();
                float[] p = w.city.findWalkable(b.doorX + (float) Math.cos(a) * 18, b.doorY + (float) Math.sin(a) * 18);
                o.postX = p != null ? p[0] : b.doorX;
                o.postY = p != null ? p[1] : b.doorY;
                // ((12.0) Never two on the same spot (moved off a wall, two of them could be given the same one, and
                // they shoved each other round it for good).)
                for (int q = 0; q < k; q++) {
                    Entity m = group.get(q);
                    if (Math.abs(m.postX - o.postX) < 5 && Math.abs(m.postY - o.postY) < 5) {
                        float[] alt = w.city.findWalkable(o.postX + (float) Math.cos(a) * 9, o.postY + (float) Math.sin(a) * 9);
                        if (alt != null) {
                            o.postX = alt[0] + (float) Math.cos(a) * 3;
                            o.postY = alt[1] + (float) Math.sin(a) * 3;
                        }
                        break;
                    }
                }
                o.building = b;
            }
            // Neighbours shelter behind them.
            for (int k = 0; k < n; k++) {
                Entity o = w.entities.get(k);
                if (o.dead || o.type != Entity.CIVILIAN || o.hasGun || o.task != Dispatch.T_NONE || w.indoors.apocalypse()) continue;
                if ((o.x - b.doorX) * (o.x - b.doorX) + (o.y - b.doorY) * (o.y - b.doorY) > 200 * 200) continue;
                o.task = Dispatch.T_HIDE;
                o.building = b;
            }
            w.dispatch.say(Dispatch.WHO_INFO, null, group.size() + " armed residents have taken over a building on "
                    + w.city.placeName(b.doorX, b.doorY) + " and are holding the street.", b.doorX, b.doorY);
            // ((25.0) And they set about walling the place in: a survivor base.)
            SurvivorBases.Base sb = w.baseLife.foundBase(b, true);
            if (sb != null) for (int k = 0; k < group.size(); k++) w.baseLife.joinBase(group.get(k), sb, true);
            else {
                World.Holdout h = b.holdout != null ? b.holdout : w.holdoutLife.found(b, false);
                if (h != null) for (int k = 0; k < group.size(); k++) group.get(k).holdout = h;
            }
            return;
        }
    }
}
