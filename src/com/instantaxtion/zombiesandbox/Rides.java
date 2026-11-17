package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Lifts: getting in and out of vehicles, calling one, being driven to a job and what happens after.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class Rides {
    private final World w;

    Rides(World w) {
        this.w = w;
    }

    /**
     * A crew member walking back to their fire engine or patrol car. Returns true while they're on their way
     * (or just got in); false if there's nothing to get back to.
     */
    boolean boardStep(Entity e) {
        Fleet.Vehicle v = e.rig;
        if (v == null || v.broken || v.parked || !w.fleet.vehicles.contains(v)) {
            e.rig = null;
            e.task = Dispatch.T_NONE;
            return false;
        }
        float ddx = v.x - e.x, ddy = v.y - e.y, d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
        // ((11.7) A car or truck on its way to pick them up: they wait where they are, watching for it, and only run
        // to it once it's close - not off towards it as it drives on to where they were.)
        if (v.state == Fleet.DRIVE && v.pickup && d > 140) {
            w.movement.steer(e, 0, 0, 0);
            e.paused = false;
            if (e.want < 1) e.angle = World.turn(e.angle, (float) Math.atan2(ddy, ddx), 0.1f);
            return true;
        }
        // (10.34) Climbing in from a little further off, at a run, and round whatever's in the way.
        if (d < 20) {
            w.fleet.board(v, e);
            return true;
        }
        e.paused = false;
        e.aiming = false;
        // ((13.3) Flat out, a little quicker than their usual run: the truck is waiting.)
        if (d > 48) {
            w.wilds.walkToSpot(e, v.x, v.y, e.runSpeed * 1.1f);
            return true;
        }
        w.movement.steer(e, ddx / d, ddy / d, e.runSpeed * 1.1f);
        if (e.blocked) {
            e.blocked = false;
            e.unstick = 0.5f;
            e.unstickAngle = (float) Math.atan2(ddy, ddx) + (w.rnd.nextBoolean() ? 1.3f : -1.3f);
        }
        return true;
    }

    /** Running to a car that stopped for them, and climbing in. Returns true while that is happening. */
    boolean boardRide(Entity e) {
        Fleet.Vehicle v = e.ride;
        if (v == null) return false;
        if (v.broken || v.parked || !w.fleet.vehicles.contains(v) || v.riders.size() >= Fleet.SEATS) {
            e.ride = null;
            if (e.task == Dispatch.T_RIDE) e.task = Dispatch.T_NONE;
            return false;
        }
        float ddx = v.x - e.x, ddy = v.y - e.y;
        float d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
        if (d > 150) {
            e.ride = null;
            if (e.task == Dispatch.T_RIDE) e.task = Dispatch.T_NONE;
            return false;
        }
        if (d < v.length() + 5 && v.speed < 20) {
            e.dead = true;
            e.removed = true;
            w.fleet.boarded(v, e);
            return true;
        }
        // ((13.3) The car waits longer for them, and they run to it round whatever's in the way, flat out.)
        v.waitTimer = Math.max(v.waitTimer, 3f);
        if (d > 40 && (e.blocked || !w.city.los(e.x, e.y, v.x, v.y))) w.wilds.walkToSpot(e, v.x, v.y, e.runSpeed * 1.1f);
        else w.movement.steer(e, ddx / d, ddy / d, e.runSpeed * 1.1f);
        return true;
    }

    /**
     * ((13.3) Soldiers with nothing to do, a long way from the dead or from base: a truck for them and the rest of
     * their squad about them, to the nearest call where the dead are, or back to base with no call to go to. True
     * once one is on its way; they wait for it then (see boardStep).
     */
    boolean rideOut(Entity e, float dt) {
        if (e.rig != null || e.ride != null || e == w.controlled || (e.driveCd -= dt) > 0) return false;
        // ((16.9) Asked again sooner when there wasn't one: every five seconds, not ten.)
        e.driveCd = 5;
        if (w.spatial.nearest(e, 100, true, false) != null) return false;
        Dispatch.Incident inc = null;
        float bd = 4000;
        for (int i = 0, n = w.dispatch.incidents.size(); i < n; i++) {
            Dispatch.Incident q = w.dispatch.incidents.get(i);
            if (q.resolved) continue;
            float d = (float) World.hypot(q.x - e.x, q.y - e.y);
            if (d < bd && d > World.RIDE_HOME * 0.7f) {
                bd = d;
                inc = q;
            }
        }
        java.util.ArrayList<Entity> group = new java.util.ArrayList<Entity>();
        group.add(e);
        int n = w.spatial.gatherLiving(e.x, e.y, 150);
        for (int i = 0; i < n && group.size() < 8; i++) {
            Entity o = w.gathered[i];
            if (o != e && !o.dead && o.type == Entity.SOLDIER && o.task == Dispatch.T_NONE && o.rig == null && o.ride == null && o != w.controlled) group.add(o);
        }
        if (!w.fleet.driveTo(group, inc)) return false;
        for (Entity o : group) o.driveCd = 10;
        if (w.time > w.rideSaidAt) {
            w.rideSaidAt = w.time + 25;
            w.dispatch.say(Dispatch.WHO_MILITARY, e, inc != null ? "Long walk to " + inc.place + ". Getting a truck." : "Long walk back to base. Getting a truck.", e.x, e.y);
        }
        return true;
    }

    /**
     * (11.7) How fast an officer or soldier goes to where they're needed: a long way off, with none of the dead
     * about, flat out - a sprint, for as long as their breath lasts (then a jog until they have it back); nearer,
     * or with the dead close, at a steady run.
     */
    /** (12.3) Back to what they were doing before the lift (there, or it fell through): their task, zone or post. */
    void afterRide(Entity e, Dispatch.Incident inc) {
        e.rideFrom = null;
        e.incident = null;
        e.onScene = false;
        e.task = inc.rideTask;
        e.zone = inc.rideZone;
        if (e.task == Dispatch.T_POST || e.task == Dispatch.T_CORDON) {
            e.postX = inc.rideX;
            e.postY = inc.rideY;
        }
        if (e.task == Dispatch.T_GUARD && (e.zone == null || e.zone.removed)) {
            e.task = Dispatch.T_NONE;
            e.zone = null;
        }
        // ((26.0) A perimeter post the infection zone has moved off (or that's no longer held) while they rode: not
        // back to it. The dead drawn to the fighting move a zone more than they did.)
        if (e.task == Dispatch.T_CORDON) {
            World.InfectionZone z = w.containment.zoneOfPost(e.postX, e.postY);
            if (z == null || !z.critical) e.task = Dispatch.T_NONE;
        }
        e.rideCd = w.time + 20;
    }

    /**
     * (12.3) Where a soldier on foot is headed, for rideCheck: the safe zone they guard, their post or place on the
     * cordon, the call they're answering, where the dead were last seen, or (with nothing to fight) back to base.
     * Null if they're not going anywhere in particular.
     */
    float[] rideTarget(Entity e) {
        switch (e.task) {
            case Dispatch.T_GUARD:
                return e.zone != null && !e.zone.removed ? new float[]{e.zone.x, e.zone.y} : null;
            case Dispatch.T_POST:
            case Dispatch.T_CORDON:
                return e.postX > 0 ? new float[]{e.postX, e.postY} : null;
            case Dispatch.T_RESPOND:
                return e.incident != null && !e.incident.resolved && !e.incident.ride ? new float[]{e.incident.x, e.incident.y} : null;
            case Dispatch.T_NONE:
                if (e.role == Entity.ROLE_GUARD || w.posts.onDuty(e)) return null;
                // ((24.3) An officer with nothing on walks their beat; only hunting the dead far off do they drive.)
                if (e.type == Entity.COP) {
                    int czd = w.city.fieldAt(w.city.zombieDist, e.x, e.y);
                    if (czd >= 200 || czd < 40 || e.fear > 0) return null;
                    Dispatch.Incident cn = w.dispatch.neediest(e.x, e.y, null);
                    return cn != null ? new float[]{cn.x, cn.y} : null;
                }
                if (e.fear > 0) return new float[]{e.threatX, e.threatY};
                int zd = w.city.fieldAt(w.city.zombieDist, e.x, e.y);
                if (zd < 200) {
                    // Hunting the dead a long way off: the worst of the fighting nearest them.
                    if (zd < 40) return null;
                    Dispatch.Incident n = w.dispatch.neediest(e.x, e.y, null);
                    return n != null ? new float[]{n.x, n.y} : null;
                }
                City.Facility base = w.city.nearestFacility(City.FACILITY_BASE, e.x, e.y);
                return base != null && !base.fallen ? new float[]{base.x, base.y} : null;
            default:
                return null;
        }
    }

    void rideCheck(float dt) {
        w.rideTimer -= dt;
        if (w.rideTimer > 0) return;
        w.rideTimer = 2;
        w.rideGroups.clear();
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            // ((24.3) The police too: they walked to every call they weren't sent to by car, right across town.)
            if (e.dead || (e.type != Entity.SOLDIER && e.type != Entity.COP) || e.rig != null || e == w.controlled || e.aloft || e.rideCd > w.time) continue;
            if (e.role == Entity.ROLE_PILOT || e.role == Entity.ROLE_SAR || e.role == Entity.ROLE_K9 || e.task >= 32) continue;
            // ((27.4) Not a crew's or a roof's: they're where their job wants them.)
            if (w.ops.jobs.keeps(e, Jobs.P_CREW)) continue;
            float[] to = rideTarget(e);
            // ((24.3) From a shorter walk than before (500).)
            if (to == null || World.hypot(to[0] - e.x, to[1] - e.y) < World.RIDE_FROM || w.spatial.countZombiesNear(e.x, e.y, 120) > 0) continue;
            // ((21.4) Two lifts in a row that got them nowhere (a post the trucks can't reach, on an island map): they
            // walk it for a few minutes. Each lift took a truck out, and the trucks never stopped coming.)
            if (e.rideWasted >= 2 && w.time - e.rideAt < 240) continue;
            // (Grouped by what they're doing and where it is, not by squad: a squad gets split up, and a truck for
            // each one on their own soon used up every truck there was.)
            int key = ((e.type * 64 + e.task) * 64 + (int) (to[0] / 400)) * 64 + (int) (to[1] / 400);
            ArrayList<Entity> g = w.rideGroups.get(key);
            if (g == null) w.rideGroups.put(key, g = new ArrayList<Entity>());
            g.add(e);
        }
        for (ArrayList<Entity> g : w.rideGroups.values()) {
            Entity a = g.get(0);
            float[] to = rideTarget(a);
            if (to == null) continue;
            boolean cops = a.type == Entity.COP;
            int seats = cops ? Fleet.SEATS : 10;
            ArrayList<Entity> group = new ArrayList<Entity>();
            // ((24.3) Gathered from further round (350, from 250), so more of them go together in fewer vehicles.)
            for (Entity e : g) {
                float[] t = e == a ? to : rideTarget(e);
                if (t == null) continue;
                if (group.size() < seats && World.hypot(e.x - a.x, e.y - a.y) < 350 && World.hypot(t[0] - to[0], t[1] - to[1]) < 300
                        && e.zone == a.zone && e.incident == a.incident) group.add(e);
            }
            // ((24.3) Seats to spare on the way to a call: those standing about near them with nothing on come along.)
            if (a.task == Dispatch.T_RESPOND && a.incident != null && !a.incident.resolved && group.size() < seats)
                for (int k = 0, m = w.spatial.gatherArmed(a.x, a.y, 160); k < m && group.size() < seats; k++) {
                    Entity o = w.gathered[k];
                    if (o.dead || o.type != a.type || o.task != Dispatch.T_NONE || o.rig != null || o == w.controlled || group.contains(o)
                            || o.role == Entity.ROLE_GUARD || o.role == Entity.ROLE_PILOT || o.role == Entity.ROLE_SAR || o.role == Entity.ROLE_K9
                            || w.posts.onDuty(o) || o.ammo + o.reserve <= 0 || o.hp < o.maxHp * 0.5f || o.rideCd > w.time) continue;
                    // ((27.4) Nor one on a job of its own.)
                    if (!w.ops.jobs.take(o, Jobs.P_POST)) continue;
                    o.task = Dispatch.T_RESPOND;
                    o.incident = a.incident;
                    o.onScene = false;
                    group.add(o);
                }
            // ((24.3) One soldier on their own isn't worth a truck to themselves for a few blocks: they walk it (or go
            // with others heading the same way), and only take one for a long way.)
            if (!cops && group.size() == 1 && World.hypot(to[0] - a.x, to[1] - a.y) < 900) continue;
            Dispatch.Incident inc, hunt = null;
            if (a.task == Dispatch.T_NONE && a.fear <= 0 && w.city.fieldAt(w.city.zombieDist, a.x, a.y) < 200) {
                hunt = w.dispatch.neediest(a.x, a.y, null);
                if (hunt != null && World.hypot(hunt.x - to[0], hunt.y - to[1]) > 1) hunt = null;
            }
            if (a.task == Dispatch.T_RESPOND) inc = a.incident;
            else if (hunt != null) inc = hunt;
            else {
                inc = new Dispatch.Incident();
                inc.x = to[0];
                inc.y = to[1];
                inc.place = a.task == Dispatch.T_GUARD ? a.zone.place : w.city.placeName(to[0], to[1]);
                inc.ride = true;
                inc.rideTask = a.task;
                inc.rideZone = a.zone;
                inc.rideX = a.postX;
                inc.rideY = a.postY;
            }
            City.Facility base = cops ? null : w.city.nearestFacility(City.FACILITY_BASE, a.x, a.y);
            boolean atBase = base != null && World.hypot(base.x - a.x, base.y - a.y) < Math.max(200, base.r * 2.5f);
            // ((26.0) Police with water between them and the call, and no quick way round by road: a police boat.)
            boolean ok = cops ? w.ops.boatLift(group, inc) || w.fleet.driveTo(group, inc) : (atBase && w.fleet.troopTruck(group, base, inc)) || w.fleet.mountUp(group, inc) || w.fleet.driveTo(group, inc);
            for (Entity e : group) {
                e.rideCd = w.time + (ok ? 25 : 8);
                if (ok && inc.ride) e.rideFrom = inc;
                if (ok) {
                    float left = (float) World.hypot(inc.x - e.x, inc.y - e.y);
                    e.rideWasted = w.time - e.rideAt < 240 && left > e.rideLeft * 0.6f ? e.rideWasted + 1 : 0;
                    e.rideAt = w.time;
                    e.rideLeft = left;
                }
            }
            if (ok && w.time > w.squadRideSaidAt && inc.ride && !cops) {
                w.squadRideSaidAt = w.time + 25;
                w.dispatch.chatter(Dispatch.WHO_MILITARY, a, Dispatch.SQUADS[Math.max(0, a.squad) % Dispatch.SQUADS.length] + " squad, mounting up for "
                        + inc.place + ".", a.x, a.y);
            }
        }
    }

    float transit(Entity e, float d) {
        if (d > 260 && e.stamina > 0.2f && w.spatial.countZombiesNear(e.x, e.y, 70) == 0) return e.runSpeed * 1.25f;
        return e.runSpeed * 0.95f;
    }

    /** A moving vehicle hits zombies in its way and nudges people aside. */
    float runOver(float x, float y, float r, float speed, float angle) {
        if (speed < 20) return 0;
        float impact = 0;
        int cx0 = Math.max(0, (int) ((x - r - 8) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((x + r + 8) / World.CELL));
        int cy0 = Math.max(0, (int) ((y - r - 8) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((y + r + 8) / World.CELL));
        float fx = (float) Math.cos(angle), fy = (float) Math.sin(angle);
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c], end = w.cellStart[c] + w.cellCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o.dead) continue;
                    float ddx = o.x - x, ddy = o.y - y, reach = r + o.radius;
                    if (ddx * ddx + ddy * ddy > reach * reach) continue;
                    // Push sideways out of the lane.
                    float side = ddx * -fy + ddy * fx >= 0 ? 1 : -1;
                    w.movement.tryMove(o, -fy * side * 6 + fx * 3, fx * side * 6 + fy * 3);
                    if (!o.isZombie() || o.stun > 0) continue;
                    o.hp -= speed * (o.type == Entity.GIANT ? 0.12f : o.type == Entity.BRUTE ? 0.3f : 0.9f);
                    // ((12.1) Knocked flat, not finished: one hit by a car may get up again too.)
                    if (o.hp <= 0) o.bodyShot = true;
                    o.hurt = 1;
                    o.stun = 1f;
                    o.knockX = fx * speed * 0.9f / o.mass;
                    o.knockY = fy * speed * 0.9f / o.mass;
                    o.killedByZombie = false;
                    w.effects.bloodBurst(o.x, o.y, 8, fx, fy);
                    w.emit(Sfx.THUD, o.x, o.y);
                    // Hitting them dents the car: a brute is like hitting a wall.
                    impact += o.type == Entity.GIANT ? 70 : o.type == Entity.BRUTE ? 28 : 6;
                }
            }
        return impact;
    }

    /** A civilian waves the car down: the nearest person running for their life who can reach it. */
    boolean hail(Fleet.Vehicle v) {
        if (v.speed > 60) return false;
        Entity best = null;
        float bd = 70 * 70;
        int cx0 = Math.max(0, (int) ((v.x - 70) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((v.x + 70) / World.CELL));
        int cy0 = Math.max(0, (int) ((v.y - 70) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((v.y + 70) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c] + w.zCount[c], end = w.cellStart[c] + w.cellCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o.dead || o.type != Entity.CIVILIAN || o.ride != null || o.infected) continue;
                    if (o.task != Dispatch.T_NONE && o.task != Dispatch.T_SEEK) continue;
                    if (o.fleeTimer <= 0 && o.task != Dispatch.T_SEEK) continue;
                    float d = (o.x - v.x) * (o.x - v.x) + (o.y - v.y) * (o.y - v.y);
                    if (d < bd && w.spatial.countZombiesNear(o.x, o.y, 14) == 0) {
                        bd = d;
                        best = o;
                    }
                }
            }
        if (best == null) return false;
        callRide(best, v);
        // Their family comes too.
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity o = w.entities.get(i);
            if (!o.dead && o.ride == null && (o.leader == best || (best.leader != null && (o == best.leader || o.leader == best.leader))))
                callRide(o, v);
        }
        return true;
    }

    void callRide(Entity e, Fleet.Vehicle v) {
        e.ride = v;
        if (e.type == Entity.CIVILIAN) e.task = Dispatch.T_RIDE;
        e.talkTimer = 1.5f;
    }

    /** Puts someone who was in a car (or indoors) back on the street. Returns false if there's no room. */
    boolean release(Entity o, float x, float y) {
        // (Someone already alive getting out of a car or a building is never turned away by the cap; only well
        // past it.)
        if (w.entities.size() >= w.maxEntities + World.OVER_CAP) return false;
        float[] p = w.city.findWalkable(x, y);
        if (p == null) return false;
        o.x = p[0];
        o.y = p[1];
        o.vx = o.vy = 0;
        o.dead = false;
        o.removed = false;
        o.ride = null;
        // ((24.0) The bite didn't wait while they rode: a bitten officer driven across town turned only on getting out.)
        if (o.infected && o.goneAt >= 0) o.infectTimer -= Math.max(0, w.time - o.goneAt);
        o.goneAt = -1;
        // ((14.7) Your squad, out with you, carries on following you.)
        o.task = o.commandedBy != null && o.commandedBy == w.controlled ? Dispatch.T_SQUAD : Dispatch.T_NONE;
        o.taskTimer = 3;
        w.entities.add(o);
        return true;
    }

    /** A car (not the helicopter) at a point, or null. */
    Fleet.Vehicle vehicleAt(float x, float y) {
        for (int i = 0, n = w.fleet.gather(x, y, 8); i < n; i++) {
            Fleet.Vehicle v = w.fleet.found[i];
            if (Fleet.offRoad(v)) continue;
            float dx = v.x - x, dy = v.y - y;
            if (dx * dx + dy * dy < 7 * 7) return v;
        }
        return null;
    }
}
