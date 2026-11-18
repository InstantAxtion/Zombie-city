package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Recruitment: enlisting, training, reserve pilots.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class Recruitment {
    private final World w;

    Recruitment(World w) {
        this.w = w;
    }

    /**
     * After the outbreak has cost the police or army people, they ask civilians to join up and fill the
     * gaps. Each person decides for themselves: some sign up, most don't.
     */
    void recruitment() {
        // ((28.0) The regulars only: volunteers lifted the peak, and every one of them lost was then made up with a
        // sworn-in recruit as if an officer had fallen - more police on foot out of the stations for each auxiliary down.)
        int volCops = 0, volSoldiers = 0;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || e.role != Entity.ROLE_VOLUNTEER) continue;
            if (e.type == Entity.COP) volCops++;
            else if (e.type == Entity.SOLDIER) volSoldiers++;
        }
        int cops = Math.max(0, w.counts[Entity.COP] - volCops), soldiers = Math.max(0, w.counts[Entity.SOLDIER] - volSoldiers);
        w.peakCops = Math.max(w.peakCops, cops);
        w.peakSoldiers = Math.max(w.peakSoldiers, soldiers);
        if (w.peakZombies < 5) return;
        w.policeDrive = drive(Entity.COP, City.FACILITY_POLICE, w.peakCops, cops, w.policeDrive);
        w.armyDrive = drive(Entity.SOLDIER, City.FACILITY_BASE, w.peakSoldiers, soldiers, w.armyDrive);
        volunteers();
    }

    // ------------------------------------------------------------------ ((27.5) volunteers

    /** Kit each station or base has for volunteers (vests and pistols, rifles and webbing): handed out once each. */
    private final java.util.HashMap<City.Facility, int[]> kitOut = new java.util.HashMap<City.Facility, int[]>();
    /**
     * No more than this many volunteers about at once, however many stations and bases there are; ((28.0) and no more
     * than this many sign up in the whole game: with the 27.5 cities' twenty-odd stations the kit alone let well over a
     * hundred through, a fresh one for each that fell.)
     */
    // ((28.0) A hundred and twenty: thirty-six was too few to notice in a city of twenty-odd stations.)
    static final int MAX_VOLUNTEERS = 120;

    private int kitFor(City.Facility f) {
        if (f.kind == City.FACILITY_BASE) return 10;
        // (The police headquarters - the first station - has more to hand out.)
        for (City.Facility o : w.city.facilities) if (o.kind == City.FACILITY_POLICE && !o.fed) return o == f ? 8 : 5;
        return 5;
    }

    /**
     * ((27.5) Once the emergency broadcast is out able
     * adults sign up: a few each time round, at the nearest police station or army base with kit left to hand out and
     * none of the dead at its door. Ex-soldiers and hunters most of all; never the bitten, inmates, or those already
     * holding out in a survivor base or a safe zone. They come out a little later as police auxiliaries or army
     * volunteers (see enlistWalk), and it stops when the kit's gone or the station or base falls.
     */
    /** (For comparing games with and without them: -Dzc.volunteers=off.) */
    static final boolean VOLUNTEERS = !"off".equals(System.getProperty("zc.volunteers"));

    void volunteers() {
        // ((28.0) Not once the city's fight is over, won or lost: they kept coming out of the stations for good after.)
        if (!VOLUNTEERS || w.alert < 2 || w.peakZombies < 5 || w.warResult != 0) return;
        int about = 0, enlisting = 0;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.dead) continue;
            if (e.role == Entity.ROLE_VOLUNTEER && (e.type == Entity.COP || e.type == Entity.SOLDIER)) about++;
            else if (e.volunteering && e.task == Dispatch.T_ENLIST) {
                about++;
                enlisting++;
            }
        }
        // (About at once, and ever: those signed up so far and those on their way to.)
        int ever = w.volunteersPolice + w.volunteersArmy + enlisting;
        if (about >= MAX_VOLUNTEERS || ever >= MAX_VOLUNTEERS) return;
        ArrayList<City.Facility> open = new ArrayList<City.Facility>();
        for (City.Facility f : w.city.facilities) {
            if ((f.kind != City.FACILITY_POLICE && f.kind != City.FACILITY_BASE) || f.fed || f.fallen) continue;
            int[] k = kitOut.get(f);
            if (k == null) kitOut.put(f, k = new int[2]);
            if (k[0] >= kitFor(f) || w.spatial.countZombiesNear(f.x, f.y, 220) > 0) continue;
            open.add(f);
            // The first time: the radio says so.
            if (k[1] == 0) {
                k[1] = 1;
                if (f.kind == City.FACILITY_POLICE)
                    w.dispatch.say(Dispatch.WHO_POLICE, null, "Police Command: " + f.name + " is taking volunteers. Able adults, report to the front desk "
                            + "for a vest and a sidearm.", f.x, f.y);
                else
                    w.dispatch.say(Dispatch.WHO_MILITARY, null, "Military: Volunteers wanted at the gate of " + f.name + ". Basic kit and a rifle for "
                            + "anyone who can use one.", f.x, f.y);
            }
        }
        if (open.isEmpty()) return;
        // (A few asked each time round: those not far from somewhere open, not in danger, with nothing better to do.)
        int asks = 6, room = MAX_VOLUNTEERS - Math.max(about, ever);
        for (int i = 0, n = w.entities.size(); i < n && asks > 0 && room > 0; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || e.type != Entity.CIVILIAN || e.asked || e.infected || e.inmate || e.fleeTimer > 0 || e == w.controlled) continue;
            if (e.base != null || e.zone != null || e.leader != null || e.pilotReserve) continue;
            if (e.task != Dispatch.T_NONE && e.task != Dispatch.T_SEEK) continue;
            City.Facility best = null;
            float bd = 900;
            for (int k = 0; k < open.size(); k++) {
                City.Facility f = open.get(k);
                float d = (float) World.hypot(e.x - f.x, e.y - f.y);
                if (d < bd) {
                    bd = d;
                    best = f;
                }
            }
            if (best == null || !w.city.reaches(w.city.field(best), e.x, e.y)) continue;
            asks--;
            e.asked = true;
            int tr = e.trade();
            float keen = willingness(e) * 0.5f + (tr == Entity.TR_VETERAN ? 0.45f : tr == Entity.TR_HUNTER ? 0.3f : 0);
            if (w.rnd.nextFloat() >= keen) {
                w.refused++;
                continue;
            }
            int[] k = kitOut.get(best);
            if (k[0] >= kitFor(best)) continue;
            k[0]++;
            room--;
            e.task = Dispatch.T_ENLIST;
            e.enlistAt = best;
            e.volunteering = true;
            e.taskTimer = 0;
            e.talkTimer = 1.5f;
        }
    }

    boolean drive(int type, int facilityKind, int peak, int now, boolean active) {
        int enlisting = 0;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            // ((28.0) Not the volunteers on their way: they aren't making up for anyone.)
            if (e.task == Dispatch.T_ENLIST && e.enlistAt != null && e.enlistAt.kind == facilityKind && !e.volunteering) enlisting++;
        }
        int missing = peak - now - enlisting;
        if (missing <= 0) return enlisting > 0 && active;
        // Recruiting only happens where it's safe.
        City.Facility office = null;
        for (City.Facility f : w.city.facilities)
            if (f.kind == facilityKind && w.spatial.countZombiesNear(f.x, f.y, 220) == 0) {
                office = f;
                break;
            }
        if (office == null) return active;
        if (!active) {
            int lost = peak - now;
            if (type == Entity.COP)
                w.dispatch.say(Dispatch.WHO_POLICE, null, "Police Command: We've lost " + lost + (lost == 1 ? " officer" : " officers")
                        + ". Any civilians willing to serve, report to " + office.name + " to be sworn in.", office.x, office.y);
            else
                w.dispatch.say(Dispatch.WHO_MILITARY, null, "Military: We're down " + lost + (lost == 1 ? " soldier" : " soldiers")
                        + ". Volunteers wanted at " + office.name + ". We'll train you and give you a rifle.", office.x, office.y);
        }
        // Ask a few people who are nearby and not in danger right now.
        int asks = Math.min(missing, 3) * 2;
        for (int i = 0, n = w.entities.size(); i < n && asks > 0 && missing > 0; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || e.type != Entity.CIVILIAN || e.asked || e.infected || e.fleeTimer > 0) continue;
            if (e.task != Dispatch.T_NONE && e.task != Dispatch.T_SEEK) continue;
            if (World.hypot(e.x - office.x, e.y - office.y) > 900 || !w.city.reaches(w.city.field(office), e.x, e.y)) continue;
            asks--;
            e.asked = true;
            if (w.rnd.nextFloat() < willingness(e)) {
                e.task = Dispatch.T_ENLIST;
                e.enlistAt = office;
                e.taskTimer = 0;
                e.talkTimer = 1.5f;
                missing--;
            } else {
                w.refused++;
            }
        }
        return true;
    }

    /**
     * (11.0) Once it's on the news, the city asks anyone with a pilot's licence to sign up as a reserve pilot for the
     * police, rescue and army helicopters: a couple for each of them do.
     */
    void reservePilots() {
        if (w.pilotsAsked || w.alert < 1) return;
        int helis = 0;
        for (int i = 0, n = w.fleet.vehicles.size(); i < n; i++) if (w.fleet.vehicles.get(i).type == Fleet.HELI && w.fleet.vehicles.get(i).padAt != null) helis++;
        if (helis == 0) return;
        w.pilotsAsked = true;
        int want = helis * 2, got = 0;
        for (int k = 0, n = w.entities.size(); k < n * 2 && got < want; k++) {
            Entity e = w.entities.get(w.rnd.nextInt(n));
            if (e.dead || e.type != Entity.CIVILIAN || e.infected || e.pilotReserve || e.leader != null || e.home == null) continue;
            e.pilotReserve = true;
            got++;
        }
        if (got > 0)
            w.dispatch.say(Dispatch.WHO_INFO, null, "News: The city asked anyone with a pilot's licence to sign up as a reserve pilot for its helicopters. "
                    + got + " have.", w.city.worldW() / 2, w.city.worldH() / 2);
    }

    /**
     * (14.9) An officer or soldier from the station or base whose helicopter has lost its pilot puts in to fly it:
     * the nearest one free (not its commander, a guard or anyone special), who goes to the pad and trains there.
     */
    Entity pilotApplicant(Fleet.Vehicle v) {
        boolean army = v.kind != Fleet.K_POLICE_HELI && v.kind != Fleet.K_RESCUE_HELI;
        int type = army ? Entity.SOLDIER : Entity.COP;
        Entity best = null;
        float bd = 1600;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || e.type != type || e.infected || e.pilotFor != null || e == w.controlled || e.rig != null || e.ride != null) continue;
            if (e.task != Dispatch.T_NONE && e.task != Dispatch.T_POST) continue;
            if (e.role != Entity.ROLE_RIFLE || e.agency != 0 || e.scout || e.hp < e.maxHp * 0.5f) continue;
            float d = (float) World.hypot(e.x - v.padAt[0], e.y - v.padAt[1]);
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        return best;
    }

    /** (11.0) The nearest reserve pilot still alive, called up to fly v (no pilot left); null if there are none. */
    Entity callReservePilot(Fleet.Vehicle v) {
        if (v.padAt == null) return null;
        // ((14.9) First, one of its own officers or soldiers puts in for it.)
        Entity own = pilotApplicant(v);
        if (own != null) {
            w.dispatch.releaseForResupply(own);
            own.pilotFor = v;
            own.task = Dispatch.T_TRAIN;
            own.trainT = 0;
            boolean army = own.type == Entity.SOLDIER;
            String air = v.kind == Fleet.K_RESCUE_HELI ? "the rescue helicopter" : v.kind == Fleet.K_POLICE_HELI ? "the police helicopter" : "Air " + Math.max(1, v.number);
            w.dispatch.say(army ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, own, (army ? Names.soldierRank(own) : Names.copRank(own)) + " " + Names.person(own.nameSeed)
                    + " has put in to fly " + air + ". Heading to the pad to train.", v.padAt[0], v.padAt[1]);
            return own;
        }
        Entity best = null;
        float bd = Float.MAX_VALUE;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || !e.pilotReserve || e.type != Entity.CIVILIAN || e.infected || e.pilotFor != null) continue;
            float d = (float) World.hypot(e.x - v.padAt[0], e.y - v.padAt[1]);
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        if (best == null) return null;
        best.pilotFor = v;
        best.task = Dispatch.T_ENLIST;
        best.enlistAt = null;
        best.goalCd = 0;
        String air = v.kind == Fleet.K_RESCUE_HELI ? "the rescue helicopter" : v.kind == Fleet.K_POLICE_HELI ? "the police helicopter" : "Air " + Math.max(1, v.number);
        w.dispatch.say(v.kind == Fleet.K_POLICE_HELI || v.kind == Fleet.K_RESCUE_HELI ? Dispatch.WHO_POLICE : Dispatch.WHO_MILITARY, null,
                "We've lost the pilot for " + air + ". Calling up reserve pilot " + Names.person(best.nameSeed) + ".", v.padAt[0], v.padAt[1]);
        return best;
    }

    /**
     * (14.9) An officer or soldier training to fly their station's helicopter: to the pad, then lessons there for a
     * while (they'll still shoot at the dead that come close), and then they're its pilot. False if it's off - the
     * helicopter's gone, or someone else has turned up to fly it - and they go back to their duties.
     */
    boolean trainStep(Entity e, float dt) {
        Fleet.Vehicle v = e.pilotFor;
        if (v == null || v.padAt == null || !w.fleet.vehicles.contains(v) || w.fleet.livePilot(v) != null) {
            e.pilotFor = null;
            e.task = Dispatch.T_NONE;
            return false;
        }
        Entity z = w.combat.pickTarget(e, 120);
        if (z != null && (e.ammo > 0 || e.reserve > 0)) return false;
        float px = v.padAt[0] + 22, py = v.padAt[1] + 6;
        if (World.hypot(px - e.x, py - e.y) > 16) {
            w.wilds.walkToSpot(e, px, py, e.runSpeed * 0.8f);
            return true;
        }
        w.movement.steer(e, 0, 0, 0);
        e.angle = World.turn(e.angle, (float) Math.atan2(v.y - e.y, v.x - e.x), dt * 3);
        e.trainT += dt;
        if (e.trainT < World.PILOT_TRAINING) return true;
        e.role = Entity.ROLE_PILOT;
        e.callsign = v.number;
        e.task = Dispatch.T_NONE;
        e.pilotFor = null;
        e.trainT = 0;
        v.flight.add(0, e);
        v.pilotComing = null;
        boolean army = e.type == Entity.SOLDIER;
        w.dispatch.say(army ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, e, (army ? Names.soldierRank(e) : Names.copRank(e)) + " " + Names.person(e.nameSeed)
                + ": Checked out on the helicopter. Ready to fly.", e.x, e.y);
        return true;
    }

    /** (11.0) A reserve pilot on the way to the pad; there, they're sworn in and become its pilot. False if it's off. */
    boolean pilotWalk(Entity e) {
        Fleet.Vehicle v = e.pilotFor;
        if (v == null || v.padAt == null || !w.fleet.vehicles.contains(v) || w.fleet.livePilot(v) != null) {
            e.pilotFor = null;
            e.task = Dispatch.T_NONE;
            return false;
        }
        float px = v.padAt[0] + 30, py = v.padAt[1];
        if (World.hypot(px - e.x, py - e.y) > 18) {
            w.wilds.walkToSpot(e, px, py, e.runSpeed * 0.8f);
            return true;
        }
        boolean army = v.kind != Fleet.K_POLICE_HELI && v.kind != Fleet.K_RESCUE_HELI;
        Entity p = w.population.make(army ? Entity.SOLDIER : Entity.COP, e.x, e.y, -1, 0);
        p.role = Entity.ROLE_PILOT;
        p.callsign = v.number;
        p.skin = e.skin;
        p.nameSeed = e.nameSeed;
        e.dead = true;
        e.removed = true;
        e.pilotFor = null;
        w.newRecruits.add(p);
        v.flight.add(0, p);
        v.pilotComing = null;
        w.dispatch.say(army ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, p, "Reserve pilot " + Names.person(p.nameSeed) + " at the pad. Ready to fly.", p.x, p.y);
        return true;
    }

    /** How likely someone is to sign up: gun owners are keen, people with family to look after and the hurt much less. */
    float willingness(Entity e) {
        float p = 0.22f;
        if (e.hasGun) p += 0.35f;
        if (e.leader != null) p -= 0.12f;
        for (int i = 0, n = w.entities.size(); i < n; i++)
            if (w.entities.get(i).leader == e && w.entities.get(i).type == Entity.CIVILIAN) {
                p -= 0.12f;
                break;
            }
        if (e.hp < e.maxHp * 0.6f) p -= 0.1f;
        return Math.max(0.03f, p);
    }

    /** Walks to the precinct or base, trains for a few seconds, then comes out in uniform. */
    void enlistWalk(Entity e, float dt) {
        City.Facility f = e.enlistAt;
        // ((27.5) The station or base fallen on the way: nowhere to sign up now.)
        if (f.fallen && e.volunteering) {
            e.task = Dispatch.T_NONE;
            e.enlistAt = null;
            e.volunteering = false;
            return;
        }
        float ddx = f.x - e.x, ddy = f.y - e.y;
        // ((12.0) Once there and training, a step or two further out doesn't send them back to the start: drifting
        // about on the edge of the yard, in and out, kept the clock going back to nothing and they never signed up.)
        boolean training = e.taskTimer < 0;
        if (ddx * ddx + ddy * ddy > f.r * f.r * (training ? 1.1f : 0.5f)) {
            e.taskTimer = 0;
            // ((16.10) Beyond the reach of its way on foot (see City.FACILITY_REACH): their own way there.)
            if (!w.movement.followField(e, w.city.field(f), e.speed * 1.4f)) w.wilds.walkToSpot(e, f.x, f.y, e.speed * 1.4f);
            return;
        }
        // Training: in a spot of their own in the yard.
        float a = (e.nameSeed & 0xFF) / 256f * World.TAU, tr = f.r * (0.2f + ((e.nameSeed >> 8) & 7) * 0.03f);
        float sx = f.x + (float) Math.cos(a) * tr, sy = f.y + (float) Math.sin(a) * tr;
        if (!w.city.solidAt(sx, sy)) w.holdoutLife.standAt(e, sx, sy, 0.4f);
        else w.movement.wander(e, e.speed * 0.3f);
        if (e.taskTimer > -6) return;
        int type = f.kind == City.FACILITY_BASE ? Entity.SOLDIER : Entity.COP;
        Entity r = w.population.make(type, e.x, e.y, -1, 0);
        r.skin = e.skin;
        r.angle = e.angle;
        if (e.volunteering) {
            // ((27.5) A volunteer: the same person (their name, their skill), in a vest or khaki, an auxiliary or a volunteer.)
            r.nameSeed = e.nameSeed;
            r.level = e.level;
            r.xp = e.xp;
            r.kills = e.kills;
            w.population.applyRole(r, Entity.ROLE_VOLUNTEER);
            r.signedUpAt = f;
            // ((28.0) And posted there: they hold the station or base they signed up at (fighting from it, and sent on
            // from it by a call like anyone posted). They had no orders at all, and walked off across the city on their
            // own - police on foot turning up anywhere, nobody had sent them.)
            float[] p = !w.city.solidAt(sx, sy) ? new float[]{sx, sy} : w.city.findWalkable(e.x, e.y);
            r.task = Dispatch.T_POST;
            r.postX = p != null ? p[0] : e.x;
            r.postY = p != null ? p[1] : e.y;
            if (type == Entity.COP) w.volunteersPolice++;
            else w.volunteersArmy++;
        }
        e.dead = true;
        e.removed = true;
        for (int i = 0, n = w.entities.size(); i < n; i++) if (w.entities.get(i).leader == e) w.entities.get(i).leader = null;
        w.newRecruits.add(r);
        if (!e.volunteering) w.recruits++;
        e.volunteering = false;
        String where = f.name;
        if (r.role == Entity.ROLE_VOLUNTEER) {
            if (w.time - w.lastRecruitSay < 8) return;
            w.lastRecruitSay = w.time;
            w.dispatch.chatter(type == Entity.COP ? Dispatch.WHO_POLICE : Dispatch.WHO_MILITARY, r, (type == Entity.COP ? "Auxiliary " : "Volunteer ")
                    + Names.person(r.nameSeed) + ", signed up at " + where + ". Ready to help.", r.x, r.y);
            return;
        }
        // Keep the radio readable when lots join at once.
        if (w.time - w.lastRecruitSay < 8) return;
        w.lastRecruitSay = w.time;
        if (type == Entity.COP)
            w.dispatch.chatter(Dispatch.WHO_POLICE, r, "New recruit, sworn in at " + where + ". Ready for duty.", r.x, r.y);
        else
            w.dispatch.chatter(Dispatch.WHO_MILITARY, r, "Volunteer reporting in at " + where + ", trained and armed.", r.x, r.y);
    }
}
