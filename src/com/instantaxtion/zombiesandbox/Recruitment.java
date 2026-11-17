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
        w.peakCops = Math.max(w.peakCops, w.counts[Entity.COP]);
        w.peakSoldiers = Math.max(w.peakSoldiers, w.counts[Entity.SOLDIER]);
        if (w.peakZombies < 5) return;
        w.policeDrive = drive(Entity.COP, City.FACILITY_POLICE, w.peakCops, w.policeDrive);
        w.armyDrive = drive(Entity.SOLDIER, City.FACILITY_BASE, w.peakSoldiers, w.armyDrive);
    }

    boolean drive(int type, int facilityKind, int peak, boolean active) {
        int enlisting = 0;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.task == Dispatch.T_ENLIST && e.enlistAt != null && e.enlistAt.kind == facilityKind) enlisting++;
        }
        int missing = peak - w.counts[type] - enlisting;
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
            int lost = peak - w.counts[type];
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
        e.dead = true;
        e.removed = true;
        for (int i = 0, n = w.entities.size(); i < n; i++) if (w.entities.get(i).leader == e) w.entities.get(i).leader = null;
        w.newRecruits.add(r);
        w.recruits++;
        String where = f.name;
        // Keep the radio readable when lots join at once.
        if (w.time - w.lastRecruitSay < 8) return;
        w.lastRecruitSay = w.time;
        if (type == Entity.COP)
            w.dispatch.chatter(Dispatch.WHO_POLICE, r, "New recruit, sworn in at " + where + ". Ready for duty.", r.x, r.y);
        else
            w.dispatch.chatter(Dispatch.WHO_MILITARY, r, "Volunteer reporting in at " + where + ", trained and armed.", r.x, r.y);
    }
}
