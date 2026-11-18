package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Civilians once there's danger: deciding, fleeing, hiding, fighting back, making for a safe zone, warning each other.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class CivilianAI {
    private final World w;

    CivilianAI(World w) {
        this.w = w;
    }

    /**
     * ((22.0) Someone running from the dead with an armoured van parked as a shelter close by: for it, and in.
     * False if there's none with room.
     */
    boolean shelterStep(Entity e) {
        Fleet.Vehicle best = null;
        float bd = 150;
        for (int i = w.fleet.shelters.size() - 1; i >= 0; i--) {
            Fleet.Vehicle v = w.fleet.shelters.get(i);
            if (v.broken || v.burnt || !w.fleet.vehicles.contains(v)) {
                w.fleet.shelters.remove(i);
                continue;
            }
            if (v.riders.size() >= Fleet.SHELTER_SEATS || Math.abs(v.speed) > 3) continue;
            float d = (float) World.hypot(v.x - e.x, v.y - e.y);
            if (d < bd) {
                bd = d;
                best = v;
            }
        }
        if (best == null) return false;
        if (bd < best.length() * 0.6f + 8) {
            w.exodus.aboard(best, e);
            w.sheltered++;
            if (best.riders.size() == Fleet.SHELTER_SEATS)
                w.dispatch.say(Dispatch.WHO_POLICE, null, "Police: The armoured van at " + w.city.placeName(best.x, best.y) + " is full.", best.x, best.y);
            return true;
        }
        w.movement.steer(e, (best.x - e.x) / bd, (best.y - e.y) / bd, e.runSpeed);
        return true;
    }

    void thinkCivilian(Entity e, float dt) {
        if (e.fleeTimer > 0 && e != w.controlled && e.leader == null && !w.fleet.shelters.isEmpty() && shelterStep(e)) return;
        // ((12.4) No hiding indoors in an outbreak: a plan to is dropped.)
        if (w.indoors.apocalypse() && e.task == Dispatch.T_HIDE && e != w.controlled) {
            e.task = Dispatch.T_NONE;
            e.building = null;
        }
        if (e.job == Entity.J_LOOKOUT && w.wilds.lookout(e, dt)) return;
        if (e.scavenge != null || e.homeward) {
            thinkScavenger(e, dt);
            if (e.scavenge != null || e.homeward) return;
        }
        // ((27.5) Out with a raid from their base.)
        if (e.raid != null && w.baseRaids.member(e, dt)) return;
        if (e.militia) {
            w.armedAI.thinkMilitia(e, dt);
            if (e.militia) return;
        }
        // ((25.0) One of a survivor base's people.)
        if (e.base != null && w.baseLife.baseMember(e, dt)) return;
        // ((14.2) A raider gang: out to strip the shops, and they'll shoot whoever's there for the same.)
        if (e.raider && w.raiders.raiderStep(e, dt)) return;
        // ((25.0) In a riot, or on the run from a prison break.)
        if (e.riotUntil > 0 && e != w.controlled && w.events.riotStep(e, dt)) return;
        if (e.escapee && e != w.controlled && w.events.escapeeStep(e, dt)) return;
        if (inmateStep(e)) return;
        // ((10.31) Not even looking when the nearest of the dead is well out of reach: most of a big city, most
        // of the time.)
        boolean zNear = w.city.fieldAt(w.city.zombieDist, e.x, e.y) <= 100 / City.T + 6;
        Entity threat = zNear ? w.spatial.nearest(e, 100, true, true) : null;
        if (threat == null && zNear) threat = w.spatial.nearest(e, 26, true, false);
        float threatDist = threat != null ? noticed(e, threat) : Float.MAX_VALUE;

        if (w.rescue.waitForLift(e, threat, threatDist)) return;
        if (enlistStep(e, threat, threatDist, dt)) return;

        fightOff(e);

        // Waved down a car: run for it and jump in.
        if (w.rides.boardRide(e)) return;

        if (helpKin(e, threatDist)) return;

        // (10.37) The rest is the civilian's brain: what to do now, chosen by weighing up everything they could do
        // (see decideCivilian), held to for a while, then carried out.
        // Plans that can't happen any more are let go.
        boolean room = w.dispatch.hasRoom();
        // (One still going up counts: they queue at its gate for the wall to be finished.)
        letGoOfPlans(e, room, dt);
        int goal = decideCivilian(e, threat, threatDist, room, dt);
        if (goal != World.G_FIGHT) e.aiming = false;
        if (carryOut(e, goal, threat, threatDist, dt)) return;
        // The chosen thing had nothing to do after all (or it's an ordinary moment): get on with the day.
        if (goal != World.G_ROUTINE) e.goalCd = 0;
        // (Their plan changed under them - let into a zone, say: carry on with that, and think again next moment.)
        if (e.task == Dispatch.T_SHELTER && e.zone != null) {
            shelterLife(e, e.zone, dt);
            return;
        }
        if (e.fleeTimer > 0) {
            fleeStep(e, dt);
            return;
        }
        if (w.aftermathLife.cleanup(e, threat) || supplyRun(e, threat)) return;
        if (!w.dailyLife.routine(e, dt)) {
            // A moment between things (or nothing doing): stand about, or stroll a little, not drift for ever.
            if (e.aware && e.task == Dispatch.T_NONE) getToSafety(e, w.dailyLife.homeOf(e));
            else if (e.errandTimer > 0 && e.errandTimer < 6) w.movement.steer(e, 0, 0, 0);
            else w.movement.wander(e, e.speed * 0.6f);
        }
    }

    /** One of the dead seen: the scream, the alarm, the fright that stays with them, a call to 911. How far off it is. */
    private float noticed(final Entity e, final Entity threat) {
        if (e.fleeTimer <= 0) {
            if (w.rnd.nextFloat() < 0.3f) w.emit(Sfx.SCREAM, e.x, e.y);
            alarm(e, threat);
        }
        float ddx = threat.x - e.x, ddy = threat.y - e.y;
        float threatDist = (float) Math.sqrt(ddx * ddx + ddy * ddy);
        // A close call stays with you: the nearer it came, the longer they keep running.
        e.fleeTimer = Math.max(e.fleeTimer, threatDist < 50 ? 10 : 7);
        e.fear = 60;
        e.aware = true;
        e.threatX = threat.x;
        e.threatY = threat.y;
        // Some people call 911 when they see one.
        if (e.callCd <= 0 && threatDist > 20) {
            e.callCd = 45;
            if (w.rnd.nextFloat() < 0.35f) w.dispatch.call(e, threat);
        }
        return threatDist;
    }

    /** Locked up in the island prison: about the yard, and away from the dead if they're in there too. True when that was their step. */
    private boolean inmateStep(final Entity e) {
        // ((15.6) Locked up in the island prison: about the yard, and away from the dead if they're in there too.)
        if (e.inmate && !w.prisonFallen && e != w.controlled) {
            Entity z = w.spatial.nearest(e, 90, true, false);
            if (z != null) {
                float zd = (float) World.hypot(e.x - z.x, e.y - z.y) + 0.001f;
                w.movement.flee(e, (e.x - z.x) / zd, (e.y - z.y) / zd, e.runSpeed);
            } else w.movement.wander(e, e.speed * 0.5f);
            return true;
        }
        return false;
    }

    /** Called up or volunteering: on the way to the pad or to sign up - not with the dead close. True when that was their step. */
    private boolean enlistStep(final Entity e, final Entity threat, final float threatDist, final float dt) {
        // ((11.0) A reserve pilot called up: to the pad.)
        if (e.task == Dispatch.T_ENLIST && e.pilotFor != null) {
            if (threat != null && threatDist < 45) {
                w.movement.flee(e, (e.x - threat.x) / threatDist, (e.y - threat.y) / threatDist, e.runSpeed);
                return true;
            }
            if (w.recruiting.pilotWalk(e)) return true;
        }
        // Volunteers on their way to sign up.
        if (e.task == Dispatch.T_ENLIST) {
            if (threat != null && threatDist < 60 || e.enlistAt == null) {
                // Not now: run, and maybe try again later.
                e.task = Dispatch.T_NONE;
                e.enlistAt = null;
            } else {
                w.recruiting.enlistWalk(e, dt);
                return true;
            }
        }
        return false;
    }

    /** Grabbed: shoving the zombie off, or hitting it with something. */
    private void fightOff(final Entity e) {
        // Anyone grabbed can try to shove the zombie off (or hit it, with something to hit it with).
        if (e.meleeCd <= 0) {
            Entity z = w.spatial.nearestInReach(e);
            if (z != null) {
                if (e.melee > 0) w.combat.swing(e, z);
                else if (w.rnd.nextFloat() < 0.4f) w.zombieAI.shove(e, z, false);
                else e.meleeCd = 0.8f;
            }
        }
    }

    /** Family members pull zombies off each other. True when that was their step. */
    private boolean helpKin(final Entity e, final float threatDist) {
        // Family members pull zombies off each other.
        Entity kin = e.leader;
        if (kin != null && !kin.dead && e.meleeCd <= 0 && threatDist > 12) {
            Entity grab = w.spatial.nearestInReach(kin);
            if (grab != null) {
                float ddx = grab.x - e.x, ddy = grab.y - e.y;
                float d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
                if (d < 30) {
                    if (d < e.radius + grab.radius + 3) w.zombieAI.shove(e, grab, false);
                    else w.movement.steer(e, ddx / d, ddy / d, e.runSpeed);
                    return true;
                }
            }
        }
        return false;
    }

    /** Plans that can't happen any more are let go; the broadcasts heard; and not strolling back to where the dead were. */
    private void letGoOfPlans(final Entity e, final boolean room, final float dt) {
        if (e.task == Dispatch.T_SEEK && ((!room && !zoneGoingUp()) || e.refused || e.refusedUntil > w.time)) e.task = Dispatch.T_NONE;
        if (e.task == Dispatch.T_SHELTER && w.dispatch.zones.isEmpty()) e.task = Dispatch.T_NONE;
        // With the power out, nobody hears the broadcasts about safe zones any more.
        if (!e.heardZone && w.war.broadcasting() && w.readiness != 2 && w.rnd.nextFloat() < dt * 0.03f) e.heardZone = true;
        // Out of sight isn't out of mind: keep going until well clear of where it was last seen. ((10.51) Further
        // than before, and for longer: running a block and then strolling back the way they came got people killed.)
        if (e.fleeTimer <= 0 && e.fear > 15 && e.task == Dispatch.T_NONE && w.zombies > 0
                && (e.x - e.threatX) * (e.x - e.threatX) + (e.y - e.threatY) * (e.y - e.threatY) < 320 * 320) e.fleeTimer = 2.5f;
    }

    /** What they've decided to do, done. True when that was their step. */
    private boolean carryOut(final Entity e, final int goal, final Entity threat, final float threatDist, final float dt) {
        switch (goal) {
            case World.G_FIGHT:
                if (threat == null) break;
                if (!e.hasGun || e.ammo + e.reserve <= 0) {
                    brawlStep(e, threat, threatDist, dt);
                    return true;
                }
                fightStep(e, threat, threatDist, dt);
                return true;
            case World.G_FLEE:
                fleeStep(e, dt);
                return true;
            case World.G_HIDE:
                if (hideStep(e)) return true;
                break;
            case World.G_ZONE:
                if (seekZoneStep(e, threatDist)) return true;
                break;
            case World.G_SHELTER:
                if (e.task != Dispatch.T_SHELTER || e.zone == null) break;
                shelterLife(e, e.zone, dt);
                return true;
            case World.G_RESTOCK:
                if (marketRun(e, threat)) return true;
                break;
            case World.G_PICKUP:
                if (w.sandbox.pickupStep(e, threatDist)) return true;
                break;
            case World.G_HEAL:
                if (healSeeking(e, threat, dt)) return true;
                break;
            case World.G_EVAC:
                if (w.exodus.evacuate(e, dt)) return true;
                break;
            case World.G_LEAVE:
                if (w.exodus.leaveStep(e)) return true;
                break;
            case World.G_FOLLOW:
                if (followLeader(e)) return true;
                break;
            case World.G_PATROL:
                if (patrol(e)) return true;
                break;
            case World.G_SUPPLY:
                if (w.aftermathLife.cleanup(e, threat) || supplyRun(e, threat)) return true;
                break;
            default:
                break;
        }
        return false;
    }

    /**
     * Weighs up everything a civilian could be doing and picks one: running from the dead, standing and
     * shooting, hiding indoors, making for a safe zone, getting a gun, seeing to their wounds, getting out of a
     * part of town that's been taken over, sticking with their family, or just carrying on with their day. Each
     * gets a score from what's going on round them and the sort of person they are; what they're already doing
     * gets a bonus and is held to for a moment, so nobody dithers between two things. A threat coming close
     * makes them think again at once.
     */
    int decideCivilian(Entity e, Entity threat, float threatDist, boolean room, float dt) {
        e.goalCd -= dt;
        e.goalTime += dt;
        // (Thought again at once when a zombie gets close, or the one being fought is gone, but not every frame.)
        boolean urgent = threat != null && threatDist < 25 && e.goal != World.G_FLEE && e.goal != World.G_FIGHT && e.goalCd < 0.4f;
        // (10.39) ... and the moment one of the dead first comes into sight, whatever they were doing.
        boolean lost = (threat == null && e.goal == World.G_FIGHT) || (threat != null && (e.goal == World.G_ROUTINE || !e.sawThreat));
        e.sawThreat = threat != null;
        if (e.goalCd > 0 && !urgent && !lost) return e.goal;
        e.goalCd = 0.5f + w.rnd.nextFloat() * 0.4f;
        java.util.Arrays.fill(w.goalScore, -1);
        float brave = World.bravery(e), careful = World.caution(e);
        float near = threat == null ? 0 : Math.max(0, Math.min(1, 1 - threatDist / 100f));
        boolean armed = e.hasGun && (e.ammo > 0 || e.reserve > 0);
        // (New plans are only taken up by someone with none: a place in a zone, a patrol or a supply run stay theirs.)
        boolean idle = e.task == Dispatch.T_NONE;
        // Fight: armed, a target in range, and not right on top of them.
        if (armed && threat != null && threatDist > 14 && threatDist < 90 && e.task != Dispatch.T_SHELTER)
            w.goalScore[World.G_FIGHT] = 0.55f + 0.35f * brave + 0.15f * near;
        // ((14.8) Or fight back with whatever's to hand: someone with a bat or a crowbar already, the brave with
        // others round them doing the same, or anyone cornered with nowhere left to run. Not on their own against
        // a crowd, nor when badly hurt.)
        else if (!armed && threat != null && threatDist < 70 && e.task != Dispatch.T_SHELTER && e.type == Entity.CIVILIAN) {
            int mates = brawlersNear(e);
            int fighting = mates >> 8, with = mates & 0xFF;
            int pack = w.spatial.countZombiesNear(threat.x, threat.y, 40);
            boolean cornered = threatDist < 26 && (e.stamina < 0.3f || pack >= 2 && threatDist < 16);
            if (e.melee > 0 || with >= 3 || fighting > 0 || cornered || brave > World.BRAWL_BRAVE) {
                float s = 0.2f + 0.55f * brave + 0.12f * Math.min(3, with) + 0.12f * Math.min(3, fighting) + (e.melee > 0 ? 0.2f : 0)
                        + (cornered ? 0.35f : 0) + 0.15f * near - 0.14f * Math.max(0, pack - 1 - with) - (e.hp < e.maxHp * 0.4f ? 0.5f : 0);
                w.goalScore[World.G_FIGHT] = s;
            }
        }
        // Flee: from the dead in sight, or still shaken from the last one.
        if (threat != null) w.goalScore[World.G_FLEE] = 0.3f + 0.65f * near + (armed ? 0 : 0.15f) + 0.1f * (1 - brave);
        else if (e.fleeTimer > 0) w.goalScore[World.G_FLEE] = 0.25f + 0.3f * Math.min(1, e.fleeTimer / 7f);
        // Hide indoors: unarmed, a building with room close by, and the dead not already at its door.
        // ((12.0) Someone whose leader has gone indoors without them, or died, hides on their own: they used to wander
        // about for good, set on hiding and never going.)
        if (World.WALK_IN && !armed && !w.indoors.apocalypse() && ((threat != null && e.taskTimer <= 0 && idle) || e.task == Dispatch.T_HIDE) && threatDist > 30 && (e.leader == null || e.leader.dead)) {
            City.Building b = e.task == Dispatch.T_HIDE ? e.building : null;
            if (b == null || b.occupants.size() >= b.capacity || b.barricade <= 0 || b.collapsed) b = hidingPlace(e);
            if (b != null && w.spatial.countZombiesNear(b.doorX, b.doorY, 35) == 0) {
                float dd = (float) World.hypot(b.doorX - e.x, b.doorY - e.y);
                // (Not if the way to it runs past the dead.)
                float tx = threat != null ? threat.x - e.x : 0, ty = threat != null ? threat.y - e.y : 0;
                float toward = threat == null ? 0 : ((b.doorX - e.x) * tx + (b.doorY - e.y) * ty) / ((dd + 0.001f) * (threatDist + 0.001f));
                w.goalScore[World.G_HIDE] = 0.5f + 0.3f * careful + 0.25f * (1 - Math.min(1, dd / 220f)) - (toward > 0.5f ? 0.6f : 0)
                        - (w.readiness == 2 ? 0.25f : 0);
                e.hideCandidate = b;
            }
        }
        // A safe zone: living in one, on the way to one, or heading for one now it's on the news.
        if (e.task == Dispatch.T_SHELTER && e.zone != null) w.goalScore[World.G_SHELTER] = threat != null && threatDist < 28 ? 0.2f : 0.95f;
        // ((12.0) Including one still going up, for those already on their way: they queue at its gate (see
        // seekZoneStep). They used to keep the plan but stand about where they were until it opened.)
        if ((room || (e.task == Dispatch.T_SEEK && zoneGoingUp())) && !e.refused && e.refusedUntil <= w.time) {
            // (Once on the way, the way to the zone is the way out: kept to unless the dead are right there.)
            if (e.task == Dispatch.T_SEEK) w.goalScore[World.G_ZONE] = 0.8f;
            else if (idle && (e.fleeTimer > 0 || (w.war.broadcasting() && w.readiness != 2 && e.heardZone))) w.goalScore[World.G_ZONE] = 0.42f + 0.2f * careful;
            if (threat != null && threatDist < 28) w.goalScore[World.G_ZONE] = Math.min(w.goalScore[World.G_ZONE], 0.15f);
        }
        // ((16.9) No going for a gun: there are no gun stores. Those who own one have it already.)
        if (e.task == Dispatch.T_RESUPPLY) w.goalScore[World.G_RESTOCK] = 0.5f;
        // ... or out of rounds, when it's quiet: restock.
        // ((10.60) Not only when it's quiet: when the nearest of the dead is still a way off, too.)
        if (idle && e.hasGun && threatDist > 70 && e.ammo + e.reserve <= 2) w.goalScore[World.G_RESTOCK] = 0.45f;
        // ... or one lying in the street.
        // ((12.8) Not one somebody else has already picked up.)
        if (e.task == Dispatch.T_PICKUP) w.goalScore[World.G_PICKUP] = threatDist < 40 || e.pickup == null || !w.pickups.contains(e.pickup) ? -1 : 0.55f;
        // ((12.0) Or with their gun run dry: rounds lying there are as good as a walk to the shop.)
        else if (idle && (!e.hasGun || e.ammo + e.reserve <= 2) && threat == null) {
            World.Pickup p = w.sandbox.nearestPickup(e.x, e.y, 110);
            e.pickCandidate = p != null && w.spatial.countZombiesNear(p.x, p.y, 50) == 0 ? p : null;
            if (e.pickCandidate != null) w.goalScore[World.G_PICKUP] = 0.38f + 0.15f * brave;
        }
        // Hurt: a pharmacy, or a rest, once it's quiet.
        if (threat == null && (e.task == Dispatch.T_HEAL || (idle && e.hp < e.maxHp * 0.45f))) w.goalScore[World.G_HEAL] = 0.35f + 0.5f * (1 - e.hp / e.maxHp);
        // Out of a part of town that's been overrun.
        if (e.leader == null && w.alert >= 1 && w.danger != null && e.fleeTimer <= 0
                && (e.task == Dispatch.T_EVACUATE || (idle && e.taskTimer <= 0 && w.exodus.dangerDistAt(e.x, e.y) <= 1))) w.goalScore[World.G_EVAC] = 0.5f + 0.2f * careful;
        // Family and friends together.
        if (e.leader != null && !e.leader.dead) w.goalScore[World.G_FOLLOW] = 0.5f + (threat != null ? 0.15f : 0);
        if (e.task == Dispatch.T_PATROL) w.goalScore[World.G_PATROL] = 0.5f;
        // (10.47) Out of the city (or back home after): kept to, except to get away from one of the dead close by.
        if (e.task == Dispatch.T_LEAVE) w.goalScore[World.G_LEAVE] = 0.62f + 0.1f * careful;
        else if (e.task == Dispatch.T_HOMECOMING) w.goalScore[World.G_LEAVE] = 0.5f;
        if (e.task == Dispatch.T_CLEANUP || e.task == Dispatch.T_LOOT) w.goalScore[World.G_SUPPLY] = threat == null ? 0.48f : 0.1f;
        w.goalScore[World.G_ROUTINE] = 0.15f;
        // What they're already doing counts for more: they see it through unless something's clearly better.
        if (e.goal >= 0 && e.goal < World.G_COUNT && w.goalScore[e.goal] > 0) w.goalScore[e.goal] += 0.15f;
        int best = World.G_ROUTINE;
        for (int g = 0; g < World.G_COUNT; g++) if (w.goalScore[g] > w.goalScore[best]) best = g;
        switchGoal(e, best, threatDist);
        return best;
    }

    /** Moving on to a new goal: what the old one had them committed to is let go (fleeing and fighting are only interruptions). */
    void switchGoal(Entity e, int goal, float threatDist) {
        int was = e.goal;
        e.goal = goal;
        // ((12.0) The plan is made to match the goal even when the goal hasn't changed: a plan changed under them (a
        // hiding place given by a survivors' group and no good by now, a place in a zone lost when it filled) was
        // left as it was, and they wandered about "running to hide indoors", or set on a zone with no plan to get
        // there, back and forth on the spot for good.)
        // (Only for someone left with no plan, or a hiding place gone stale: a lift, a call-up or a rescue handed to
        // them by others while the goal stayed the same are theirs to keep.)
        if (goal == was && !(e.task == Dispatch.T_NONE || (goal == World.G_ROUTINE && e.task == Dispatch.T_HIDE))) return;
        if (goal != was) e.goalTime = 0;
        if (goal == World.G_FLEE || goal == World.G_FIGHT) return;
        switch (goal) {
            case World.G_HIDE:
                if (e.task != Dispatch.T_HIDE || e.building != e.hideCandidate) {
                    dropTask(e);
                    e.task = Dispatch.T_HIDE;
                    e.building = e.hideCandidate;
                }
                break;
            case World.G_ZONE:
                if (e.task != Dispatch.T_SEEK) {
                    dropTask(e);
                    e.task = Dispatch.T_SEEK;
                }
                break;
            case World.G_PICKUP:
                if (e.task != Dispatch.T_PICKUP) {
                    World.Pickup p = e.pickCandidate;
                    if (p != null && !p.claimed && w.pickups.contains(p)) {
                        dropTask(e);
                        if (p.uses == 0) p.claimed = true;
                        e.pickup = p;
                        e.task = Dispatch.T_PICKUP;
                    } else {
                        // ((12.8) Gone before they set off - someone else took it: not left meaning to fetch nothing.)
                        e.goal = World.G_ROUTINE;
                    }
                }
                break;
            case World.G_EVAC:
                if (e.task != Dispatch.T_EVACUATE) dropTask(e);
                break;
            case World.G_ROUTINE:
                // (Only their own plans are dropped: a lift, a patrol or a place in a zone stay theirs.)
                if (e.task == Dispatch.T_HIDE || e.task == Dispatch.T_PICKUP || e.task == Dispatch.T_RESUPPLY) dropTask(e);
                break;
            default:
                break;
        }
    }

    /** A safe zone being built that will take people when it opens. */
    boolean zoneGoingUp() {
        for (int i = 0; i < w.dispatch.zones.size(); i++) {
            Dispatch.SafeZone z = w.dispatch.zones.get(i);
            if (!z.open && !z.removed && !z.full) return true;
        }
        return false;
    }

    /** Lets go of the plan they had (the gun they'd claimed, the building they were going to). */
    void dropTask(Entity e) {
        if (e.task == Dispatch.T_PICKUP && e.pickup != null) e.pickup.claimed = false;
        if (e.task == Dispatch.T_HIDE || e.task == Dispatch.T_PICKUP || e.task == Dispatch.T_RESUPPLY || e.task == Dispatch.T_SEEK) {
            e.task = Dispatch.T_NONE;
            e.building = null;
            e.pickup = null;
        }
    }

    /** Somewhere to hide: home first if it's close, has room and is shut up, else the nearest with room. */
    City.Building hidingPlace(Entity e) {
        City.Building home = w.dailyLife.homeOf(e);
        if (home != null && home.inside < World.room(home) * 3 / 2 && home.barricade >= 40 && !home.collapsed && (!World.WALK_IN || w.city.enterable(home))
                && (home.doorX - e.x) * (home.doorX - e.x) + (home.doorY - e.y) * (home.doorY - e.y) < 220 * 220
                && w.spatial.countZombiesNear(home.doorX, home.doorY, 35) == 0) return home;
        return w.indoors.shelterNear(e);
    }

    /**
     * (14.8) Other civilians close by: those already fighting the dead (in the high byte) and all of them standing
     * with this one (in the low byte, the fighting ones included).
     */
    int brawlersNear(Entity e) {
        int fighting = 0, with = 0;
        for (int i = 0, m = w.spatial.gatherLiving(e.x, e.y, 45); i < m && with < 6; i++) {
            Entity o = w.gathered[i];
            if (o == e || o.dead || o.type != Entity.CIVILIAN) continue;
            if ((o.x - e.x) * (o.x - e.x) + (o.y - e.y) * (o.y - e.y) > 45 * 45) continue;
            with++;
            if (o.goal == World.G_FIGHT) fighting++;
        }
        return fighting << 8 | with;
    }

    /**
     * (14.8) Fighting back hand to hand: grab whatever's lying about (a bat, a length of pipe, a tool from the shed),
     * close in and swing; anyone near enough joins in (see brawlersNear). Each swing is the usual one (see swing).
     */
    void brawlStep(Entity e, Entity t, float d, float dt) {
        if (e.melee == 0) {
            float m = w.rnd.nextFloat();
            e.melee = m < 0.45f ? Entity.M_BAT : m < 0.85f ? Entity.M_CROWBAR : Entity.M_AXE;
            w.civBrawls++;
        }
        float reach = e.radius + t.radius + 4;
        float dx = t.x - e.x, dy = t.y - e.y, dd = d + 0.001f;
        e.aiming = false;
        e.angle = (float) Math.atan2(dy, dx);
        if (d > reach) w.movement.steer(e, dx / dd, dy / dd, e.runSpeed * 0.9f);
        else {
            w.movement.steer(e, 0, 0, 0);
            if (e.meleeCd <= 0) {
                boolean up = t.hp > 0;
                w.wildSwing = true;
                w.combat.swing(e, t);
                w.wildSwing = false;
                if (up && t.hp <= 0) w.civBrawlKills++;
            }
        }
    }

    void fightStep(Entity e, Entity threat, float threatDist, float dt) {
        // Armed civilians shoot, backing off if the zombie gets too close.
        w.combat.aimAndFire(e, threat, threatDist, 120, dt);
        if (threatDist < 38) {
            float ax = e.x - threat.x, ay = e.y - threat.y;
            w.movement.flee(e, ax / threatDist, ay / threatDist, e.speed * 1.2f);
        } else w.movement.steer(e, 0, 0, 0);
    }

    void fleeStep(Entity e, float dt) {
        e.paused = false;
        // ((10.60) Not with none of the dead left anywhere: running on, and shouting warnings that set others
        // running, kept whole streets in a panic long after the outbreak was over.)
        if (e.fleeTimer <= 0 && w.zombies > 0) e.fleeTimer = 1.5f;
        // (10.51) Shouting as they go: anyone about who hasn't seen it turns away instead of walking into it.
        e.warnCd -= dt;
        if (e.warnCd <= 0 && e.fear > 15 && w.zombies > 0) {
            e.warnCd = 1.2f + w.rnd.nextFloat() * 0.8f;
            warnOthers(e);
        }
        float ax = e.x - e.threatX, ay = e.y - e.threatY;
        float d = (float) Math.sqrt(ax * ax + ay * ay) + 0.001f;
        // ((15.9) Run as far as they can - to the water's edge, the end of a pier - with the dead well behind:
        // they stop there and get their breath back rather than keep pushing at the water.)
        if (e.blocked && d > 80 && e.stuckTime > 0.5f) {
            e.fleeTimer = 0;
            e.fear = Math.min(e.fear, 10);
            w.movement.steer(e, 0, 0, 0);
            return;
        }
        // ((12.5) No jumping into an abandoned car and driving off: civilians don't drive.)
        // Run towards a cop or soldier if one is nearby and not back where the zombies are.
        // ((10.34) Looked for every half second or so, not every frame, by everyone running.)
        e.protectorCd -= dt;
        if (((e.protector == null || e.protector.dead) && e.protectorCd <= 0) || w.rnd.nextFloat() < dt) {
            e.protector = w.spatial.nearestArmed(e, 180);
            e.protectorCd = 0.4f + w.rnd.nextFloat() * 0.4f;
        }
        Entity p = e.protector;
        if (p != null && !p.dead) {
            float px = p.x - e.x, py = p.y - e.y, pd = (float) Math.sqrt(px * px + py * py) + 0.001f;
            if (pd > 25 && (px * ax + py * ay) / (pd * d) > -0.2f) {
                w.movement.steer(e, ax / d * 0.4f + px / pd * 0.6f, ay / d * 0.4f + py / pd * 0.6f, e.runSpeed);
                float l = (float) Math.sqrt(e.mx * e.mx + e.my * e.my) + 0.001f;
                e.mx /= l;
                e.my /= l;
                return;
            }
        }
        // ((21.2) Away from all of them and through the gap, when there's more than one about.)
        if (World.TACTICS) {
            w.armedAI.sense(e);
            if (e.tN >= 2) {
                ax = e.escX * d;
                ay = e.escY * d;
            }
        }
        w.movement.flee(e, ax / d, ay / d, e.runSpeed);
    }

    /**
     * (10.51) A civilian running from the dead calls out: those within earshot who haven't seen them yet learn where
     * the danger is and keep away from it (the brave or the armed stay put and look; the rest get going).
     */
    void warnOthers(Entity e) {
        e.talkTimer = Math.max(e.talkTimer, 0.8f);
        int n = w.spatial.gatherLiving(e.x, e.y, 90);
        for (int i = 0; i < n; i++) {
            Entity o = w.gathered[i];
            if (o == e || o.dead || o.type != Entity.CIVILIAN || o == w.controlled || o.ride != null) continue;
            if ((o.x - e.x) * (o.x - e.x) + (o.y - e.y) * (o.y - e.y) > 90 * 90) continue;
            float od = (o.x - e.threatX) * (o.x - e.threatX) + (o.y - e.threatY) * (o.y - e.threatY);
            if (o.fleeTimer > 0 && o.fear >= 25) continue;
            o.aware = true;
            o.fear = Math.max(o.fear, 30);
            o.threatX = e.threatX;
            o.threatY = e.threatY;
            // Heading that way, or close to it: turn round and go.
            float hx = e.threatX - o.x, hy = e.threatY - o.y;
            boolean towards = o.vx * hx + o.vy * hy > 0;
            if (towards || od < 220 * 220) o.fleeTimer = Math.max(o.fleeTimer, o.hasGun ? 1 : 3.5f);
        }
    }

    boolean hideStep(Entity e) {
        City.Building b = e.building;
        // (Some other plan by now - a place in a zone, say: that's left alone.)
        if (e.task != Dispatch.T_HIDE) return false;
        // ((16.14) Nowhere to hide indoors: think again - run, make for a safe zone, or stand and fight.)
        if (!(World.WALK_IN && b != null && w.city.enterable(b))) {
            dropTask(e);
            e.goalCd = 0;
            return false;
        }
        if (b == null || b.occupants.size() + b.inside >= World.room(b) * 3 / 2 || b.barricade <= 0) {
            dropTask(e);
            return false;
        }
        // Too long getting there (the way's blocked, or it was further than it looked): something else, for now.
        if (e.goalTime > 25) {
            dropTask(e);
            e.taskTimer = 8;
            e.goalCd = 0;
            return false;
        }
        float ddx = b.doorX - e.x, ddy = b.doorY - e.y;
        float d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
        if (d < 8) w.indoors.enterBuilding(e, b);
        else if (!w.movement.walkTo(e, b, ddx, ddy, d, e.runSpeed) && e.blocked && w.rnd.nextFloat() < 0.1f) {
            // No way in from here: think again.
            dropTask(e);
            e.goalCd = 0;
            return false;
        }
        return true;
    }

    /**
     * ((12.0) A place in the crowd outside a gate: r out from (x, y) at angle a, or nearer it where that's inside a
     * wall or a building - where the gate was beside one, some stood pushing against it for good.)
     */
    void queueAt(Entity e, float x, float y, float a, float r, float speed) {
        float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
        while (r > 0 && w.city.solidAt(x + ca * r, y + sa * r)) r -= 3;
        r = Math.max(0, r);
        w.holdoutLife.standAt(e, x + ca * r, y + sa * r, speed);
    }

    /**
     * ((24.0) Those with them come in with them - checked at the gate the same, and counted against the room. They
     * used to walk in behind, uncounted and unchecked: zones over their room, bites that never went past a checkpoint.)
     */
    void admitFamily(Entity e, Dispatch.SafeZone z) {
        for (int i = 0, n = w.spatial.gatherLiving(e.x, e.y, 90); i < n; i++) {
            Entity o = w.gathered[i];
            if (o.dead || o.leader != e || o.type != Entity.CIVILIAN || o.task == Dispatch.T_SHELTER) continue;
            if (w.containment.checkpoint(o, z)) {
                o.leader = null;
                continue;
            }
            if (z.sheltered < z.capacity) w.dispatch.admitInside(o, z);
            else {
                o.leader = null;
                o.task = Dispatch.T_NONE;
            }
        }
    }

    /** ((24.0) How many are making for each zone, so the next to set out can pick one with places left for them. */
    void countHeading() {
        if (w.dispatch.zones.isEmpty()) return;
        for (int i = 0; i < w.dispatch.zones.size(); i++) w.dispatch.zones.get(i).heading = w.dispatch.zones.get(i).engineers = 0;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            // ((26.0) Engineers sheltering in a zone lend a hand with its wall.)
            if (!e.dead && e.task == Dispatch.T_SHELTER && e.zone != null && e.trade() == Entity.TR_ENGINEER) e.zone.engineers++;
            if (e.dead || e.task != Dispatch.T_SEEK || e.seekZone == null || e.seekZone.removed) continue;
            e.seekZone.heading++;
        }
    }

    /** On the way to a safe zone, queueing at its gate, and in. */
    boolean seekZoneStep(Entity e, float threatDist) {
        if (e.task != Dispatch.T_SEEK || threatDist <= 28) return false;
        e.paused = false;
        // ((12.0) Already in through a gate before it shut for being full: they're in, not shut inside the wall trying
        // to walk out through the closed gate to another zone, as they did for good.)
        for (int i = 0; i < w.dispatch.zones.size(); i++) {
            Dispatch.SafeZone q = w.dispatch.zones.get(i);
            if (q.open && !q.removed && q.inside(e.x, e.y)) {
                // ((12.0) While there's room. A wall put up round homes had their people coming out of their front
                // doors into it and taking a place each, two hundred and more in a zone with room for thirty; with
                // it full, they're inside the wall all the same, and stop looking for a place.)
                if (q.sheltered < q.capacity) {
                    w.dispatch.admitInside(e, q);
                    admitFamily(e, q);
                } else {
                    // ((24.0) For a while, not for good: it was never cleared, and they were shut out of every zone.)
                    e.task = Dispatch.T_NONE;
                    e.refusedUntil = w.time + 60;
                }
                return true;
            }
        }
        float speed = e.fleeTimer > 0 ? e.runSpeed : e.speed * 1.7f;
        // The nearest zone taking people (still going up, perhaps).
        Dispatch.SafeZone z = null, raw = null;
        float bd = Float.MAX_VALUE, rawD = Float.MAX_VALUE;
        for (int i = 0; i < w.dispatch.zones.size(); i++) {
            Dispatch.SafeZone q = w.dispatch.zones.get(i);
            if (q.removed || q.full || q.gateOut.length == 0) continue;
            float d = (q.x - e.x) * (q.x - e.x) + (q.y - e.y) * (q.y - e.y);
            // ((12.0) One that's open beats one still going up unless that's a good deal nearer, and one whose wall
            // hasn't been started after most of a minute isn't waited for while another has room: the crowd at its
            // gate stood there for good with an open zone across town half empty.)
            if (!q.open) d *= q.built <= 0 && q.age > 45 ? 25 : 2.5f;
            // ((24.0) One with more already on their way than it has places is passed over for one further off with room
            // - everyone made for the one zone, twice as many as it could take, while another stood half empty.)
            if (d < rawD) {
                rawD = d;
                raw = q;
            }
            if (q != e.seekZone && q.heading > q.capacity - q.sheltered + 8) d *= 6;
            if (d < bd) {
                bd = d;
                z = q;
            }
        }
        e.seekZone = z;
        // (The way to the gates leads to the nearest: making for another, they go by their own route.)
        boolean ownWay = z != raw;
        if (z == null) {
            e.task = Dispatch.T_NONE;
            getToSafety(e, w.dailyLife.homeOf(e));
            return true;
        }
        // In through the gate: they're in.
        if (z.open && z.inside(e.x, e.y)) {
            if (w.dispatch.admit(e, z)) {
                admitFamily(e, z);
                return true;
            }
            e.task = Dispatch.T_NONE;
            return true;
        }
        int g = z.gateNear(e.x, e.y);
        float[] out = z.gateOut[g], in = z.gateIn[g];
        float ox = out[0] - e.x, oy = out[1] - e.y, od = (float) Math.sqrt(ox * ox + oy * oy) + 0.001f;
        // ((12.0) Near enough to be in the queue: a little further out than the queue reaches, so the crowd's shoving
        // doesn't keep pushing someone out of it and walking back in again, over and over.)
        // ((12.0) Already through the gate's opening only when it's right there: from far off on the zone's far side
        // its inside was nearer than its outside too, and they set off in a straight line for it, into the first
        // building in the way, for good.)
        boolean atGate = od < 34 || (z.gateOpen[g] && od < 70 && World.hypot(in[0] - e.x, in[1] - e.y) < od);
        if (!atGate) {
            if (!z.open || ownWay || !w.movement.followField(e, w.dispatch.zoneField, speed)) w.wilds.walkToSpot(e, out[0], out[1], speed);
            return true;
        }
        if (!z.open) {
            // A queue outside the gate, waiting for the wall to be finished.
            float a = (e.nameSeed & 0xFF) / 256f * World.TAU;
            queueAt(e, out[0], out[1], a, 18, 0.5f);
            return true;
        }
        // At the gate: checked for bites, then through when it opens. ((12.0) And only while there's room: the
        // whole queue went through an open gate at once before the zone counted itself full, two hundred people
        // into a zone with room for thirty.)
        if (w.containment.checkpoint(e, z)) return true;
        if (z.gateOpen[g] && z.sheltered < z.capacity) {
            float ix = in[0] - e.x, iy = in[1] - e.y, id = (float) Math.sqrt(ix * ix + iy * iy) + 0.001f;
            w.movement.steer(e, ix / id, iy / id, e.speed * 1.2f);
        } else {
            // ((12.0) Waiting for it to open: each in their own place in a loose crowd out in front of it, not all
            // shoving for the one spot.)
            float gx = out[0] - in[0], gy = out[1] - in[1], gl = (float) Math.sqrt(gx * gx + gy * gy) + 0.001f;
            float a = (float) Math.atan2(gy, gx) + (((e.nameSeed >> 3) & 63) / 63f - 0.5f) * 2.2f, r = 6 + ((e.nameSeed >> 9) & 15);
            queueAt(e, out[0], out[1], a, r, 0.4f);
        }
        return true;
    }

    /**
     * Someone who knows what's happening drops whatever they were doing and gets somewhere safe, quickly:
     * home if it's near enough, a safe zone if one is taking people, any building with room, or the police.
     * Returns true while they're on their way.
     */
    boolean getToSafety(Entity e, City.Building home) {
        e.paused = false;
        e.errand = null;
        // ((12.4) Not indoors, home or anywhere else, while the dead are loose.)
        if (w.dispatch.hasRoom() && !e.refused && e.refusedUntil <= w.time && w.war.broadcasting()) {
            e.task = Dispatch.T_SEEK;
            return false;
        }
        if (e.taskTimer <= 0) e.taskTimer = 2;
        // Nowhere to hide yet: hurry to the police station, and keep moving.
        City.Facility police = w.city.nearestFacility(City.FACILITY_POLICE, e.x, e.y);
        if (police != null) {
            float pd = (float) World.hypot(police.x - e.x, police.y - e.y);
            // (10.28) Each to a spot of their own round the station, not all in a crowd at its door.
            float a = (e.nameSeed & 255) / 256f * World.TAU, rr = police.r * (1.3f + ((e.nameSeed >> 8) & 7) * 0.25f);
            float sx = police.x + (float) Math.cos(a) * rr, sy = police.y + (float) Math.sin(a) * rr;
            if (pd > rr + 60 && w.movement.followField(e, w.city.field(police), e.speed * 1.6f)) return true;
            if (!w.city.solidAt(sx, sy) && World.hypot(sx - e.x, sy - e.y) > 6) {
                w.wilds.walkToSpot(e, sx, sy, e.speed * 1.2f);
                return true;
            }
            w.movement.steer(e, 0, 0, 0);
            personalSpace(e);
            return true;
        }
        w.movement.wander(e, e.speed * 1.5f);
        return true;
    }

    /** The middle of the nearest town, hamlet or campsite. */
    float[] nearestSettlement(float x, float y) {
        float[] best = new float[]{w.city.worldW() / 2, w.city.worldH() / 2};
        float bd = Float.MAX_VALUE;
        for (float[] s : w.city.settlements) {
            float d = (s[0] - x) * (s[0] - x) + (s[1] - y) * (s[1] - y);
            if (d < bd) {
                bd = d;
                best = s;
            }
        }
        return best;
    }

    /** Family members stick with whoever leads them: keeping up, and running the same way. */
    boolean followLeader(Entity e) {
        Entity lead = e.leader;
        if (lead == null || lead.dead) return false;
        float ddx = lead.x - e.x, ddy = lead.y - e.y;
        float d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
        if (d > 300) {
            e.leader = null;
            return false;
        }
        if (e.fleeTimer > 0) {
            // Scared: run to them unless the zombie is right here.
            float tx = e.threatX - e.x, ty = e.threatY - e.y;
            if (d < 22 || tx * tx + ty * ty < 18 * 18) return false;
            w.movement.steer(e, ddx / d, ddy / d, e.runSpeed);
            return true;
        }
        // ((12.0) Alongside them, going the way they're actually going - not the way they'd like to: someone pushing
        // against a wall or a crowd wanted to go one way while standing still, and whoever was with them shuffled
        // back and forth round them for minutes. Close enough and not moving: stand with them.)
        float lsp = (float) Math.sqrt(lead.vx * lead.vx + lead.vy * lead.vy);
        // ((24.0) A wall between them (they've gone indoors, or round a corner): the way round, by the doors - not
        // straight at the wall. A third of families stood pressed against a building their leader had gone into.)
        if (d > 30 && (e.blocked || !w.city.los(e.x, e.y, lead.x, lead.y)))
            w.wilds.walkToSpot(e, lead.x, lead.y, d > 50 ? e.runSpeed : Math.max(e.speed * 1.3f, lead.want));
        else if (d > 18) w.movement.steer(e, ddx / d, ddy / d, d > 50 ? e.runSpeed : Math.max(e.speed * 1.3f, Math.min(lead.want, lsp + d)));
        else if (lsp > 3) w.movement.steer(e, lead.vx / lsp, lead.vy / lsp, lsp * (d > 10 ? 1.1f : 1f));
        else {
            w.movement.steer(e, 0, 0, 0);
            if (d > 2) e.angle = World.turn(e.angle, (float) Math.atan2(ddy, ddx), 0.08f);
        }
        return true;
    }

    City.Building shelterWithin(Entity e, float radius) {
        City.Building best = null;
        float bd = radius * radius;
        for (int i = 0, n = w.city.buildings.size(); i < n; i++) {
            City.Building b = w.city.buildings.get(i);
            if (b.capacity == 0 || b.collapsed || b.occupants.size() >= b.capacity) continue;
            float d = (b.doorX - e.x) * (b.doorX - e.x) + (b.doorY - e.y) * (b.doorY - e.y);
            if (d < bd) {
                bd = d;
                best = b;
            }
        }
        return best;
    }

    /**
     * During the outbreak survivors make their own plans: families go on supply runs to the shops, and
     * armed civilians band together into patrols.
     */
    void survivorPlans() {
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.type != Entity.CIVILIAN || e.task != Dispatch.T_NONE || e.leader != null || e.fleeTimer > 0) continue;
            if (!e.hasGun && w.rnd.nextFloat() < 0.004f && hasFollowers(e)) {
                City.Building shop = lootTarget(e.x, e.y);
                if (shop != null) {
                    e.task = Dispatch.T_LOOT;
                    e.building = shop;
                }
            } else if (e.hasGun && e.ammo + e.reserve > 6 && w.rnd.nextFloat() < 0.02f) {
                // Other armed people nearby fall in behind them.
                // ((13.6) Found from the grid, the cells within reach, not by going through everyone in the city for
                // each of them: with thousands armed in a big city it held the game up for a moment every second.)
                int joined = 0;
                float reach = 90 + World.GRID_SLACK;
                int cx0 = Math.max(0, (int) ((e.x - reach) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((e.x + reach) / World.CELL));
                int cy0 = Math.max(0, (int) ((e.y - reach) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((e.y + reach) / World.CELL));
                for (int cy = cy0; cy <= cy1 && joined < 3; cy++)
                    for (int cx = cx0; cx <= cx1 && joined < 3; cx++) {
                        int c = cy * w.gw + cx;
                        // (Past the dead and the police and soldiers, at the front of each cell.)
                        for (int k = w.cellStart[c] + w.zCount[c] + w.aCount[c], end = w.cellStart[c] + w.cellCount[c]; k < end && joined < 3; k++) {
                            Entity o = w.sorted[k];
                            if (o == e || o.dead || o.type != Entity.CIVILIAN || !o.hasGun || o.leader != null || o.task != Dispatch.T_NONE) continue;
                            if ((o.x - e.x) * (o.x - e.x) + (o.y - e.y) * (o.y - e.y) > 90 * 90) continue;
                            o.leader = e;
                            joined++;
                        }
                    }
                if (joined >= 1) {
                    e.task = Dispatch.T_PATROL;
                    e.talkTimer = 2;
                }
            }
        }
    }

    boolean hasFollowers(Entity e) {
        return e.followerFrame == w.hashFrame;
    }

    /** A supermarket or pharmacy with something left on the shelves. */
    City.Building lootTarget(float x, float y) {
        City.Building best = null;
        float bd = 800 * 800;
        for (int i = 0, n = w.city.buildings.size(); i < n; i++) {
            City.Building b = w.city.buildings.get(i);
            if (b.collapsed || b.stock <= 0 || (b.kind != City.MARKET && b.kind != City.PHARMACY)) continue;
            float d = (b.doorX - x) * (b.doorX - x) + (b.doorY - y) * (b.doorY - y);
            if (d < bd) {
                bd = d;
                best = b;
            }
        }
        return best;
    }

    /** A supply run: get to the shop, grab what's there, head off again. */
    boolean supplyRun(Entity e, Entity threat) {
        if (e.task != Dispatch.T_LOOT) return false;
        City.Building b = e.building;
        if (b == null || b.stock <= 0 || b.collapsed || (threat != null && e.fleeTimer > 0)) {
            e.task = Dispatch.T_NONE;
            e.building = null;
            return false;
        }
        float ddx = b.doorX - e.x, ddy = b.doorY - e.y;
        float d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
        if (d < 10) {
            // A market's hunting counter arms the group; a pharmacy patches them up.
            for (int i = 0, n = w.entities.size(); i < n; i++) {
                Entity o = w.entities.get(i);
                if (o != e && o.leader != e) continue;
                // ((16.9) A market no longer hands out guns: only those who own one carry one.)
                if (b.kind == City.PHARMACY && b.stock > 0) {
                    o.hp = o.maxHp;
                    b.stock--;
                }
            }
            e.task = Dispatch.T_NONE;
            e.building = null;
            e.talkTimer = 2;
            return true;
        }
        w.movement.walkTo(e, b, ddx, ddy, d, e.speed * 1.4f);
        return true;
    }

    /** A patrol of armed civilians goes looking for trouble nearby. */
    boolean patrol(Entity e) {
        if (e.task != Dispatch.T_PATROL) return false;
        if (!e.hasGun || e.ammo + e.reserve <= 0 || !hasFollowers(e) || w.recovering) {
            // Patrol over: everyone goes their own way.
            e.task = Dispatch.T_NONE;
            for (int i = 0, n = w.entities.size(); i < n; i++)
                if (w.entities.get(i).leader == e && w.entities.get(i).hasGun) w.entities.get(i).leader = null;
            return false;
        }
        if (w.city.fieldAt(w.city.zombieDist, e.x, e.y) < 25 && w.movement.followField(e, w.city.zombieDist, e.speed * 1.1f)) return true;
        w.movement.wander(e, e.speed);
        return true;
    }

    /** Word spreads: people near someone who spots a zombie start running too. */
    void alarm(Entity e, Entity threat) {
        int cx0 = Math.max(0, (int) ((e.x - 45) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((e.x + 45) / World.CELL));
        int cy0 = Math.max(0, (int) ((e.y - 45) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((e.y + 45) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c] + w.zCount[c] + w.aCount[c], end = w.cellStart[c] + w.cellCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o == e || o.dead || o.fleeTimer > 0 || (o.type != Entity.CIVILIAN && o.type != Entity.DOG)) continue;
                    if ((o.x - e.x) * (o.x - e.x) + (o.y - e.y) * (o.y - e.y) > 45 * 45) continue;
                    o.fleeTimer = 1.8f;
                    o.threatX = threat.x;
                    o.threatY = threat.y;
                }
            }
    }

    boolean marketRun(Entity e, Entity threat) {
        boolean far = threat == null || World.hypot(threat.x - e.x, threat.y - e.y) > 70;
        if (e.task == Dispatch.T_NONE && far && e.ammo + e.reserve <= 2 && e.taskTimer <= 0) {
            e.taskTimer = 3;
            City.Building best = null;
            // ((10.60) Further afield than before: a long walk for rounds beats standing about with an empty gun.)
            float bd = 2600 * 2600;
            for (int i = 0, n = w.city.buildings.size(); i < n; i++) {
                City.Building b = w.city.buildings.get(i);
                if (!(b.kind == City.MARKET || World.useOf(b) == Variants.U_ROUNDS) || b.collapsed || b.stock <= 0
                        || b.infestKnown) continue;
                float d = (b.doorX - e.x) * (b.doorX - e.x) + (b.doorY - e.y) * (b.doorY - e.y);
                if (d < bd) {
                    bd = d;
                    best = b;
                }
            }
            if (best != null) {
                e.task = Dispatch.T_RESUPPLY;
                e.building = best;
            } else if (e.hasGun && e.ammo + e.reserve <= 0 && e.reload <= 0) {
                // ((10.60) Nowhere left with any rounds: the empty gun goes, and they get by as everyone else does,
                // instead of hanging about as if they could still shoot.)
                e.hasGun = false;
                e.aiming = false;
                return false;
            }
        }
        if (e.task != Dispatch.T_RESUPPLY) return false;
        City.Building b = e.building;
        boolean close = threat != null && (threat.x - e.x) * (threat.x - e.x) + (threat.y - e.y) * (threat.y - e.y) < 25 * 25;
        if (b == null || b.stock <= 0 || b.collapsed || (threat != null && e.ammo > 0) || (!e.hasGun && close)) {
            e.task = Dispatch.T_NONE;
            e.building = null;
            return false;
        }
        float ddx = b.doorX - e.x, ddy = b.doorY - e.y;
        float d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
        if (d < 10) {
            int take = Math.min(36, b.stock);
            b.stock -= take;
            if (e.hasGun) e.reserve += take;
            else {
                w.population.armCivilian(e, take);
                // (10.24) Whatever's on the rack: shotguns most of all, hunting rifles, revolvers, or a pistol.
                float g = w.rnd.nextFloat();
                if (g < 0.4f) w.combat.setGun(e, Entity.W_SHOTGUN, take);
                else if (g < 0.58f) w.combat.setGun(e, Entity.W_HUNTING, Math.min(take, 20));
                else if (g < 0.72f) w.combat.setGun(e, Entity.W_REVOLVER, take);
                w.armedAtStores++;
            }
            e.reload = 1.5f;
            e.task = Dispatch.T_NONE;
            e.building = null;
            e.talkTimer = 2;
            return true;
        }
        // A route of their own rather than a whole map of distances for every shop: on the big maps those
        // filled the phone's memory within a minute of the outbreak.
        e.aiming = false;
        w.movement.walkTo(e, b, ddx, ddy, d, e.hasGun && e.ammo + e.reserve <= 0 ? e.runSpeed * 0.8f : e.speed * 1.4f);
        return true;
    }

    City.Building pharmacyNear(Entity e, float maxDist) {
        if (w.pharmacies == null) {
            w.pharmacies = new ArrayList<City.Building>();
            for (City.Building b : w.city.buildings) if (b.kind == City.PHARMACY) w.pharmacies.add(b);
        }
        City.Building best = null;
        float bd = maxDist * maxDist;
        for (int i = 0, n = w.pharmacies.size(); i < n; i++) {
            City.Building b = w.pharmacies.get(i);
            if (b.kind != City.PHARMACY || b.collapsed || b.stock <= 0) continue;
            float d = (b.doorX - e.x) * (b.doorX - e.x) + (b.doorY - e.y) * (b.doorY - e.y);
            if (d < bd) {
                bd = d;
                best = b;
            }
        }
        return best;
    }

    /**
     * Hurt people patch themselves up when nothing is chasing them: at a pharmacy if there's one with stock not far
     * off, or else by going home to rest. (No hospital care since 10.50.)
     */
    boolean healSeeking(Entity e, Entity threat, float dt) {
        boolean needs = e.hp < e.maxHp * 0.45f;
        if (e.task == Dispatch.T_NONE && needs && threat == null) e.task = Dispatch.T_HEAL;
        if (e.task != Dispatch.T_HEAL) return false;
        if (threat != null && e.fleeTimer > 0) {
            e.task = Dispatch.T_NONE;
            return false;
        }
        City.Building ph = pharmacyNear(e, 700);
        if (ph != null) {
            float ddx = ph.doorX - e.x, ddy = ph.doorY - e.y;
            float d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
            if (d < 10) {
                ph.stock--;
                e.hp = Math.min(e.maxHp, e.hp + e.maxHp * 0.5f);
                e.task = Dispatch.T_NONE;
                e.talkTimer = 1.5f;
                for (int k = 0; k < 6; k++)
                    w.effects.particle(e.x + w.rnd.nextFloat() * 6 - 3, e.y - 4, 0, -12, 0.6f, 1.2f, 0xFF63E06B, World.P_DOT);
                return true;
            }
            w.movement.walkTo(e, ph, ddx, ddy, d, e.speed * 1.3f);
            return true;
        }
        // Home to rest it off (or sitting it out where they are, with nowhere to go): back on their feet in a
        // minute or so.
        City.Building home = e.home;
        if (home != null && !home.collapsed) {
            float ddx = home.doorX - e.x, ddy = home.doorY - e.y;
            float d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
            if (d > 14) {
                w.movement.walkTo(e, home, ddx, ddy, d, e.speed);
                return true;
            }
        }
        e.hp = Math.min(e.maxHp, e.hp + 2.5f * dt);
        if (e.hp >= e.maxHp * 0.9f) e.task = Dispatch.T_NONE;
        w.movement.steer(e, 0, 0, 0);
        return true;
    }

    /** On a supply run: there, take what can be carried, and home again (fighting or running on the way). */
    void thinkScavenger(Entity e, float dt) {
        World.Holdout h = e.holdout;
        if (h == null || h.gone) {
            e.scavenge = null;
            e.homeward = false;
            return;
        }
        Entity z = w.spatial.nearest(e, 70, true, true);
        if (z != null) {
            float ddx = z.x - e.x, ddy = z.y - e.y, d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
            if (e.hasGun && e.ammo + e.reserve > 0) {
                w.combat.aimAndFire(e, z, d, 120, dt);
                if (d < 30) w.movement.flee(e, -ddx / d, -ddy / d, e.runSpeed);
                else w.movement.steer(e, 0, 0, 0);
                return;
            }
            if (d < 50) {
                w.movement.flee(e, -ddx / d, -ddy / d, e.runSpeed);
                return;
            }
        }
        e.aiming = false;
        City.Building goal = e.homeward ? h.b : e.scavenge;
        if (goal == null || goal.collapsed) {
            e.scavenge = null;
            e.homeward = true;
            goal = h.b;
        }
        float ddx = goal.doorX - e.x, ddy = goal.doorY - e.y, d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
        if (d > 12) {
            w.movement.walkTo(e, goal, ddx, ddy, d, e.speed * 1.5f);
            return;
        }
        if (!e.homeward) {
            // There: whatever food and ammunition one person can carry.
            int food = Math.min(goal.food, 12);
            goal.food -= food;
            e.carryFood += food;
            if ((goal.kind == City.SHOP || goal.kind == City.MARKET) && goal.stock > 0) {
                int ammo = Math.min(goal.stock, 40);
                goal.stock -= ammo;
                e.carryAmmo += ammo;
                goal.looted = true;
            }
            e.scavenge = null;
            e.homeward = true;
            return;
        }
        // Home.
        h.b.food += e.carryFood;
        h.stash += e.carryAmmo;
        if (e.carryFood + e.carryAmmo > 0)
            w.dispatch.say(Dispatch.WHO_INFO, null, "News: A supply run is back at " + h.name + " with "
                    + (e.carryFood > 0 ? World.count(e.carryFood, "day", "days") + " of food" : "") + (e.carryFood > 0 && e.carryAmmo > 0 ? " and " : "")
                    + (e.carryAmmo > 0 ? World.count(e.carryAmmo, "round", "rounds") : "") + ".", h.x, h.y);
        e.carryFood = e.carryAmmo = 0;
        e.homeward = false;
        if (!e.militia && !w.indoors.apocalypse()) {
            e.task = Dispatch.T_HIDE;
            e.building = h.b;
        }
    }

    /**
     * Life inside the wall: a tent to sleep in, meals at the food tent, the medical tent when hurt. Anyone who
     * strays outside (pushed out through a breach) makes for the gate again.
     */
    void shelterLife(Entity e, Dispatch.SafeZone z, float dt) {
        if (!z.inside(e.x, e.y)) {
            int g = z.gateNear(e.x, e.y);
            if (g < 0) return;
            float[] out = z.gateOut[g], in = z.gateIn[g];
            if (World.hypot(out[0] - e.x, out[1] - e.y) > 20 && !(z.gateOpen[g] && World.hypot(in[0] - e.x, in[1] - e.y) < 40))
                w.wilds.walkToSpot(e, out[0], out[1], e.speed * 1.3f);
            else if (z.gateOpen[g]) w.wilds.walkToSpot(e, in[0], in[1], e.speed);
            else w.holdoutLife.standAt(e, out[0], out[1], 0.4f);
            return;
        }
        e.jobTimer += dt;
        float tx, ty;
        boolean hurt = e.hp < e.maxHp * 0.85f && z.medX > 0;
        // (Each on their own clock: a meal now and then, the rest of the time by their tent.)
        int phase = ((int) ((e.jobTimer + (e.nameSeed & 0xFF)) / 30)) % 5;
        // ((12.0) The places in front of the tents a body's width apart, and more of them: three to five units apart
        // for people seven across, the crowd at the food tent shoved itself about and the ones at the back never
        // got anywhere near it.)
        if (hurt) {
            int q = e.nameSeed & 15;
            tx = z.medX + ((q % 8) - 3.5f) * 8;
            ty = z.medY + 12 + (q / 8) * 8;
        } else if (phase == 1 && z.food > 0 && z.foodX > 0) {
            // In the line at the food tent: rows in front of it.
            int q = (e.nameSeed >> 3) & 31;
            tx = z.foodX + ((q % 8) - 3.5f) * 8;
            ty = z.foodY + 13 + (q / 8) * 8;
        } else if ((phase == 2 || phase == 4) && strollSpot(e, z, (int) ((e.jobTimer + (e.nameSeed & 0xFF)) / 30))) {
            // (10.24) Out and about inside the wall: somewhere of their own, not all in one huddle.
            tx = e.spotX;
            ty = e.spotY;
        } else if (!z.tents.isEmpty()) {
            float[] t = z.tents.get((e.nameSeed & 0x7FFF) % z.tents.size());
            // Round their tent, each at their own spot.
            float a = ((e.nameSeed >> 4) & 63) / 64f * World.TAU, rr = 9 + ((e.nameSeed >> 10) & 15);
            tx = t[0] + (float) Math.cos(a) * rr;
            ty = t[1] + (float) Math.sin(a) * rr;
            if (w.city.solidAt(tx, ty)) {
                int side = (e.nameSeed >> 4) & 3;
                tx = t[0] + (side % 2 == 0 ? -7 : 7);
                ty = t[1] + (side < 2 ? 10 : -10);
            }
        } else {
            tx = z.x;
            ty = z.y;
        }
        float dx = tx - e.x, dy = ty - e.y, d = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
        if (d > 8) w.wilds.walkToSpot(e, tx, ty, e.speed);
        else {
            w.movement.steer(e, 0, 0, 0);
            personalSpace(e);
            // Patched up at the medical tent.
            if (hurt) e.hp = Math.min(e.maxHp, e.hp + 4 * dt);
        }
    }

    /** A spot of their own somewhere inside the zone for this stretch of the day (in goalX/goalY), or false. */
    boolean strollSpot(Entity e, Dispatch.SafeZone z, int cycle) {
        int hsh = e.nameSeed * 31 + cycle * 7919;
        hsh ^= hsh >>> 13;
        hsh *= 0x5bd1e995;
        hsh ^= hsh >>> 15;
        float m = 24, x0 = z.left() + m, x1 = z.right() - m, y0 = z.top() + m, y1 = z.bottom() - m;
        if (x1 <= x0 || y1 <= y0) return false;
        float x = x0 + ((hsh & 0xFFFF) / 65535f) * (x1 - x0), y = y0 + (((hsh >>> 16) & 0x7FFF) / 32767f) * (y1 - y0);
        if (w.city.solidAt(x, y) || !z.inside(x, y)) return false;
        e.spotX = x;
        e.spotY = y;
        return true;
    }

    /** Someone standing about steps away from anyone right on top of them (people keep a little apart). */
    void personalSpace(Entity e) {
        float r = 7, ax = 0, ay = 0;
        int cx0 = Math.max(0, (int) ((e.x - r) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((e.x + r) / World.CELL));
        int cy0 = Math.max(0, (int) ((e.y - r) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((e.y + r) / World.CELL));
        int n = 0;
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c] + w.zCount[c], end = w.cellStart[c] + w.cellCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o == e || o.dead) continue;
                    float dx = e.x - o.x, dy = e.y - o.y, d2 = dx * dx + dy * dy;
                    if (d2 >= r * r || d2 < 0.0001f) continue;
                    float d = (float) Math.sqrt(d2);
                    ax += dx / d * (r - d);
                    ay += dy / d * (r - d);
                    n++;
                }
            }
        if (n == 0) return;
        float l = (float) Math.sqrt(ax * ax + ay * ay) + 0.001f;
        w.movement.steer(e, ax / l, ay / l, e.speed * 0.5f);
    }

    void stampedeNews(float dt) {
        w.stampedeCd -= dt;
        if (w.stampedeCd > 0) return;
        w.stampedeCd = 6;
        if (w.trampled - w.trampledSaid >= 6) {
            // Where is the crush?
            Entity at = null;
            for (int i = 0, n = w.entities.size(); i < n && at == null; i++) {
                Entity e = w.entities.get(i);
                if (!e.dead && e.stun > 0 && e.type == Entity.CIVILIAN) at = e;
            }
            if (at != null)
                w.dispatch.say(Dispatch.WHO_INFO, null, "Stampede on " + w.city.placeName(at.x, at.y) + "! People are being trampled in the panic.",
                        at.x, at.y);
        }
        w.trampledSaid = w.trampled;
    }
}
