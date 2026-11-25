package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Buildings from the inside: going in and out, moving about indoors, sheltering in them, the dead breaking in and bursting out.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class Indoors {
    private final World w;

    Indoors(World w) {
        this.w = w;
    }

    /** Goes indoors (out of the simulation) for a while, with the family. */
    void goInside(Entity e, City.Building b, float stay) {
        e.errand = null;
        // ((16.14) Nobody goes into a building any more: at the door a moment, then on to the next thing.)
        e.errandTimer = 3 + w.rnd.nextFloat() * 10;
        // ((17.1) In through the door on their own feet, the family with them.)
        if (World.WALK_IN && !(apocalypse() && e.type == Entity.CIVILIAN) && w.city.enterable(b) && b.inside < World.room(b)) {
            beginStay(e, b, stay, false);
            if (e.followerFrame == w.hashFrame)
                for (int i = 0, n = w.spatial.gatherLiving(b.doorX, b.doorY, 120); i < n; i++) {
                    Entity o = w.gathered[i];
                    if (o.dead || o.leader != e || o.type != Entity.CIVILIAN) continue;
                    beginStay(o, b, stay, false);
                }
        }
    }

    /** (17.1) How many are in each building right now (once a second): for room to spare, and for the roofs drawn. */
    void countInside() {
        for (int i = 0, n = w.occupied.size(); i < n; i++) w.occupied.get(i).inside = 0;
        w.occupied.clear();
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.dead) continue;
            City.Building b = w.city.insideOf(e.x, e.y);
            // (Those on their way in count too: the room is theirs.)
            if (b == null) b = e.stayIn;
            if (b == null) continue;
            if (b.inside++ == 0) w.occupied.add(b);
        }
    }

    void beginStay(Entity e, City.Building b, float stay, boolean hide) {
        if (!w.city.enterable(b) || b.inside >= World.room(b) + (hide ? World.room(b) / 2 : 0)) return;
        if (b.inside++ == 0) w.occupied.add(b);
        e.stayIn = b;
        e.stayT = stay;
        e.stayWalk = 0;
        e.stayHide = hide;
        e.inSpotT = 0;
        e.errand = null;
        e.errandTimer = stay + 60;
    }

    /** The way to a spot, in or out of a building or through one: true if this took care of the step. */
    boolean indoorMove(Entity e, float tx, float ty, float speed) {
        City.Building from = w.city.insideOf(e.x, e.y), to = w.city.insideOf(tx, ty);
        if (from == null && to == null) return false;
        e.inT = w.time;
        if (from != null && from == to) {
            followRoute(e, from, tx, ty, speed);
            return true;
        }
        if (from != null) {
            // Out: through the rooms to the entrance, then out of the door.
            if (World.hypot(e.x - from.inX, e.y - from.inY) < 6 || World.hypot(e.x - from.gapX, e.y - from.gapY) < 7
                    || e.routeGiveUp && e.routeB == from && e.routeTx == from.inX && e.routeTy == from.inY) {
                float ox = from.doorX - e.x, oy = from.doorY - e.y, od = (float) Math.sqrt(ox * ox + oy * oy) + 0.001f;
                w.movement.steer(e, ox / od, oy / od, speed);
            } else followRoute(e, from, from.inX, from.inY, speed);
            return true;
        }
        // In: to the door, through the doorway, then through the rooms.
        float dd = (float) World.hypot(e.x - to.doorX, e.y - to.doorY);
        float gd = (float) World.hypot(e.x - to.gapX, e.y - to.gapY);
        if (dd > 9 && gd > 9) {
            w.wilds.walkToSpot(e, to.doorX, to.doorY, speed);
            return true;
        }
        float ix = to.inX - e.x, iy = to.inY - e.y, id = (float) Math.sqrt(ix * ix + iy * iy) + 0.001f;
        w.movement.steer(e, ix / id, iy / id, speed);
        return true;
    }

    /** Through a building's rooms, doorway by doorway, to a spot in it. */
    void followRoute(Entity e, City.Building b, float tx, float ty, float speed) {
        if (e.route == null || e.routeB != b || Math.abs(e.routeTx - tx) > 3 || Math.abs(e.routeTy - ty) > 3 || (e.blocked && w.rnd.nextFloat() < 0.05f)) {
            e.route = clearOfWalls(b, w.city.roomRoute(b, e.x, e.y, tx, ty), e.radius);
            e.routeI = 0;
            e.routeB = b;
            e.routeTx = tx;
            e.routeTy = ty;
            e.blocked = false;
            e.routeSeen = -1;
            e.routeGiveUp = false;
        }
        float[] r = e.route;
        // ((27.5) A doorway's middle can sit right against its frame: up to it, or pressed against the wall beside
        // it, they're through.)
        float near = e.blocked ? 7 : 4;
        while (e.routeI < r.length / 2 - 1 && World.hypot(r[e.routeI * 2] - e.x, r[e.routeI * 2 + 1] - e.y) < near) e.routeI++;
        float px = r[e.routeI * 2], py = r[e.routeI * 2 + 1];
        float dx = px - e.x, dy = py - e.y, d = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
        boolean last = e.routeI >= r.length / 2 - 1;
        if (last && (d < 2.5f || e.routeGiveUp)) {
            w.movement.steer(e, 0, 0, 0);
            return;
        }
        // ((27.5) Getting no closer for two seconds - the spot taken, a corner, someone in the doorway - they settle
        // for near enough, or go on to the next doorway, or step aside and find another way, instead of walking into
        // the wall for good.)
        if (e.routeSeen != e.routeI || d < e.routeBest - 1) {
            e.routeSeen = e.routeI;
            e.routeBest = d;
            e.routeProgT = w.time;
        } else if (w.time - e.routeProgT > 2) {
            e.routeProgT = w.time;
            e.routeBest = d;
            if (last && d < 12) {
                e.routeGiveUp = true;
                w.movement.steer(e, 0, 0, 0);
                return;
            }
            if (!last && d < 9) e.routeI++;
            else {
                e.route = clearOfWalls(b, w.city.roomRoute(b, e.x, e.y, tx, ty), e.radius);
                e.routeI = 0;
                e.routeSeen = -1;
                e.unstick = 0.4f;
                e.unstickAngle = (float) Math.atan2(dy, dx) + (w.rnd.nextBoolean() ? 1.4f : -1.4f);
            }
            return;
        }
        w.movement.steer(e, dx / d, dy / d, last ? Math.min(speed, d * 3 + 2) : speed);
    }

    private static final float[] NUDGE_X = {1, -1, 0, 0, 0.7f, -0.7f, 0.7f, -0.7f}, NUDGE_Y = {0, 0, 1, -1, 0.7f, 0.7f, -0.7f, -0.7f};

    /**
     * ((27.5) A doorway's point can sit where someone can't stand - hard by the end of the wall beside it, where
     * another wall meets it - and they pressed into that corner for good, a step from the gap. Each point on the
     * way is moved to the nearest place clear of the walls, on the same side of them.
     */
    float[] clearOfWalls(City.Building b, float[] r, float radius) {
        float rr = radius + 0.5f;
        for (int k = 0; k + 1 < r.length; k += 2) {
            float x = r[k], y = r[k + 1];
            if (!w.city.wallHit(b, x, y, rr)) continue;
            search:
            for (float step = 1.5f; step <= 7.5f; step += 1.5f)
                for (int j = 0; j < NUDGE_X.length; j++) {
                    float nx = x + NUDGE_X[j] * step, ny = y + NUDGE_Y[j] * step;
                    if (w.city.wallHit(b, nx, ny, rr) || w.city.wallCross(b, x, y, nx, ny) || w.city.insideOf(nx, ny) != b) continue;
                    r[k] = nx;
                    r[k + 1] = ny;
                    break search;
                }
        }
        return r;
    }

    /** Somewhere to be in a building: one of the places people sit or stand, or anywhere in one of its rooms. */
    void pickSpot(Entity e, City.Building b) {
        Interiors.Plan p = w.city.planOf(b);
        if (p == null) return;
        float[] best = null;
        if (!p.spots.isEmpty()) {
            if (e.stayHide) {
                // Hiding: as far from the door as there is.
                float bd = -1;
                for (int k = 0; k < 6; k++) {
                    float[] sp = p.spots.get(w.rnd.nextInt(p.spots.size()));
                    float d = (float) World.hypot(sp[0] - b.inX, sp[1] - b.inY);
                    if (d > bd) {
                        bd = d;
                        best = sp;
                    }
                }
            } else best = p.spots.get(w.rnd.nextInt(p.spots.size()));
        }
        if (best != null) {
            e.inSpotX = best[0] + w.rnd.nextFloat() * 2 - 1;
            e.inSpotY = best[1] + w.rnd.nextFloat() * 2 - 1;
            e.inSpotA = best[3];
        } else {
            float[] room = p.rooms.get(w.rnd.nextInt(p.rooms.size()));
            e.inSpotX = room[0] + 5 + w.rnd.nextFloat() * Math.max(1, room[2] - room[0] - 10);
            e.inSpotY = room[1] + 5 + w.rnd.nextFloat() * Math.max(1, room[3] - room[1] - 10);
            e.inSpotA = w.rnd.nextFloat() * World.TAU;
        }
        e.inSpotT = e.stayHide ? 40 : 8 + w.rnd.nextFloat() * 22;
    }

    /**
     * A civilian spending time in a building: from place to place in it, sitting, standing, browsing. The dead in
     * the building with them: out, now. The dead outside: they stay in, away from the door, until it's clear.
     * False when they're done here (or must get out), for the rest of think to take over.
     */
    boolean indoorVisit(Entity e, City.Building b, float dt) {
        e.stayT -= dt;
        Entity z = w.spatial.nearest(e, 90, true, true);
        if (z != null && w.city.insideOf(z.x, z.y) == b) {
            // In here with them: get out.
            e.stayIn = null;
            e.goal = World.G_FLEE;
            e.fleeTimer = Math.max(e.fleeTimer, 4);
            e.threatX = z.x;
            e.threatY = z.y;
            return false;
        }
        if (e.stayT <= 0) {
            // The dead about outside: wait it out a while longer.
            if (w.spatial.zombieWithin(b.doorX, b.doorY, 110) && e.stayT > -120) e.stayT = 6;
            else {
                e.stayIn = null;
                e.stayHide = false;
                e.errandTimer = 2 + w.rnd.nextFloat() * 6;
                return false;
            }
        }
        // (Seen, or known to be about - shots, screams - while they're in here: they're hiding now.)
        boolean danger = z != null || ((e.fleeTimer > 0 || e.fear > 15) && w.spatial.countZombiesNear(e.x, e.y, 150) > 0);
        if (danger) e.goal = World.G_HIDE;
        if (danger && !e.stayHide) {
            // Seen through the door: back from it, and stay put.
            e.stayHide = true;
            e.inSpotT = 0;
            e.stayT = Math.max(e.stayT, 20);
        }
        e.inSpotT -= dt;
        if (e.inSpotT <= 0) pickSpot(e, b);
        float d = (float) World.hypot(e.inSpotX - e.x, e.inSpotY - e.y);
        if (d < 2.5f || e.routeGiveUp && e.routeB == b && e.routeTx == e.inSpotX && e.routeTy == e.inSpotY) {
            w.movement.steer(e, 0, 0, 0);
            // ((27.5) Still squeezed past by those finding their way round them.)
            e.inT = w.time;
            e.angle = World.turn(e.angle, e.inSpotA, dt * 4);
        } else indoorMove(e, e.inSpotX, e.inSpotY, e.stayHide ? e.runSpeed * 0.6f : e.speed * 0.7f);
        return true;
    }

    /**
     * (17.1) Who's in which building, as they walk about: on their way in, spending time in one, or there by mistake (on
     * a chase, or shoved in) and on their way out. True when this took care of their step.
     */
    boolean indoorThink(Entity e, float dt) {
        City.Building in = w.city.insideOf(e.x, e.y);
        if (in != null) {
            if (e.type == Entity.CIVILIAN && e.stayIn == in && e.stayT > -200) {
                if (indoorVisit(e, in, dt)) return true;
            }
            // Chasing someone in here, or after someone who's in here: that's their business (thinkZombie, thinkHostile).
            if (e.isUndead() && e.memory > 0) return false;
            if (e.isArmed() && e.aiming) return false;
            // ((21.0) Armed, with one of the dead in their sights from where they stand: they fight. Before, only someone
            // already aiming got that far - and nobody indoors ever started to, so police and soldiers inside a building
            // never fired a shot, and walked out past whatever was in there with them.)
            if (e.isArmed() && e != w.controlled) {
                if ((e.indoorLook -= dt) <= 0) {
                    e.indoorLook = 0.25f;
                    e.indoorSees = (e.ammo > 0 || e.reserve > 0 || e.reload > 0) && w.combat.pickTarget(e, 150) != null;
                }
                if (e.indoorSees) return false;
                // Sent to a spot in here: to it through the rooms, and they stay.
                if ((e.task == Dispatch.T_MOVE || e.task == Dispatch.T_HOLD) && w.city.insideOf(e.postX, e.postY) == in) {
                    if (World.hypot(e.postX - e.x, e.postY - e.y) > 8) indoorMove(e, e.postX, e.postY, e.speed * 1.3f);
                    else w.movement.steer(e, 0, 0, 0);
                    return true;
                }
            }
            // ((27.5) With family who've gone in here: with them, not out of the door and straight back in after
            // them, over and over, for good.)
            if (e.type == Entity.CIVILIAN && e.leader != null && !e.leader.dead && e.fleeTimer <= 0 && w.city.insideOf(e.leader.x, e.leader.y) == in) {
                if (World.hypot(e.leader.x - e.x, e.leader.y - e.y) > 14) indoorMove(e, e.leader.x, e.leader.y, e.speed);
                else {
                    w.movement.steer(e, 0, 0, 0);
                    e.inT = w.time;
                }
                return true;
            }
            // Nothing to do in here: out.
            e.aiming = false;
            indoorMove(e, in.doorX, in.doorY, e.fleeTimer > 0 || e.isUndead() ? e.runSpeed * 0.8f : e.speed);
            return true;
        }
        // ((21.0) Police or soldiers ordered to a spot inside a building: to its door and in, shooting what they see on
        // the way. (An order into a building used to end at its wall.))
        if (e.isArmed() && e.task == Dispatch.T_MOVE && e != w.controlled && e.ride == null && (e.type == Entity.COP || e.type == Entity.SOLDIER)) {
            City.Building to = w.city.insideOf(e.postX, e.postY);
            if (to != null && w.city.enterable(to)) {
                if ((e.indoorLook -= dt) <= 0) {
                    e.indoorLook = 0.25f;
                    e.indoorSees = (e.ammo > 0 || e.reserve > 0 || e.reload > 0) && w.combat.pickTarget(e, 150) != null;
                }
                if (!e.indoorSees && !e.aiming) {
                    indoorMove(e, e.postX, e.postY, e.speed * 1.3f);
                    return true;
                }
            }
        }
        if (e.stayIn != null && e.type == Entity.CIVILIAN) {
            // On the way in: given up if it's taking too long, or the dead are close (unless that's why they're going in).
            e.stayWalk += dt;
            boolean danger = !e.stayHide && w.spatial.zombieWithin(e.x, e.y, 90);
            if (e.stayWalk > 70 || danger || e.stayIn.collapsed || !w.city.enterable(e.stayIn)) {
                e.stayIn = null;
                e.stayHide = false;
                e.errandTimer = 3 + w.rnd.nextFloat() * 5;
                return false;
            }
            indoorMove(e, e.stayIn.inX, e.stayIn.inY, e.stayHide ? e.runSpeed : e.speed);
            return true;
        }
        return false;
    }

    /**
     * ((16.14) Everyone in a building - sheltering, on an errand, or the dead shut in - out in the street at its door: for
     * a game saved before nobody went indoors.
     */
    void emptyAllBuildings() {
        for (int i = 0, n = w.city.buildings.size(); i < n; i++) {
            City.Building b = w.city.buildings.get(i);
            while (!b.occupants.isEmpty()) {
                Entity o = b.occupants.remove(b.occupants.size() - 1);
                if (!w.rides.release(o, b.doorX + w.rnd.nextFloat() * 16 - 8, b.doorY + w.rnd.nextFloat() * 16 - 8)) continue;
            }
            while (!b.visitors.isEmpty()) {
                Entity o = b.visitors.remove(b.visitors.size() - 1);
                w.rides.release(o, b.doorX + w.rnd.nextFloat() * 16 - 8, b.doorY + w.rnd.nextFloat() * 16 - 8);
            }
            for (int k = 0; k < b.lurkers; k++) w.population.spawn(Entity.ZOMBIE, b.doorX + w.rnd.nextFloat() * 16 - 8, b.doorY + w.rnd.nextFloat() * 16 - 8);
            b.lurkers = 0;
            b.fighting = false;
        }
        w.stats.recountInsideNow();
    }

    /** A building with room whose door is close and not swarmed. */
    City.Building shelterNear(Entity e) {
        return shelterNear(e, 80);
    }

    City.Building shelterNear(Entity e, float range) {
        City.Building best = null;
        float bd = range * range;
        for (int i = 0, n = w.city.buildings.size(); i < n; i++) {
            City.Building b = w.city.buildings.get(i);
            if (b.capacity == 0 || b.occupants.size() >= b.capacity || b.barricade < 40) continue;
            if (World.WALK_IN && (!w.city.enterable(b) || b.inside >= World.room(b) * 3 / 2)) continue;
            float ddx = b.doorX - e.x, ddy = b.doorY - e.y, d2 = ddx * ddx + ddy * ddy;
            if (d2 < bd && w.spatial.countZombiesNear(b.doorX, b.doorY, 35) == 0 && !w.zombieAI.backTowardsIt(e, b.doorX, b.doorY)) {
                bd = d2;
                best = b;
            }
        }
        return best;
    }

    void enterBuilding(Entity e, City.Building b) {
        // ((16.14) Nobody vanishes in. (17.1) They walk in, and hide there.)
        if (World.WALK_IN && e != w.controlled) {
            beginStay(e, b, 35 + w.rnd.nextFloat() * 45, true);
            if (e.task == Dispatch.T_HIDE) e.task = Dispatch.T_NONE;
        }
    }

    void updateBuildings(float dt) {
        w.headingTimer -= dt;
        if (w.headingTimer <= 0) {
            // Recount who's on their way where (people change their minds, get scared or are bitten on the way).
            w.headingTimer = 2;
            for (int i = 0, n = w.city.buildings.size(); i < n; i++) w.city.buildings.get(i).heading = 0;
            for (int i = 0, n = w.entities.size(); i < n; i++) {
                Entity e = w.entities.get(i);
                if (!e.dead && e.errand != null) e.errand.heading++;
            }
        }
        float frameDt = dt;
        int bmask = 3;
        for (int i = 0, n = w.city.buildings.size(); i < n; i++) {
            // ((13.1) A quarter of the buildings each frame, four frames' worth, for everything (not only the visitors,
            // as since 10.31): tens of thousands of them on the biggest maps, nearly all with nothing going on.)
            if (((i + w.lodFrame) & bmask) != 0) continue;
            dt = frameDt * (bmask + 1);
            City.Building b = w.city.buildings.get(i);
            // The last shot's flash in the windows fades whatever happens next (it used to stay lit for good
            // when the fight ended, or everyone inside was lost, right after a shot).
            if (b.flash > 0) b.flash = Math.max(0, b.flash - dt);
            if (b.lurkers > 0 && !b.collapsed && b.occupants.isEmpty()) {
                // Zombies inside burst out when someone comes close, or when they get restless.
                boolean near = w.spatial.peopleNear(b.doorX, b.doorY, 32);
                if ((near && w.rnd.nextFloat() < dt * 1.2f) || w.rnd.nextFloat() < dt / 100f) burstOut(b);
            }
        }
    }

    /** Zombies shut inside a building smash their way out. */
    void burstOut(City.Building b) {
        int n = b.lurkers;
        b.lurkers = 0;
        for (int k = 0; k < n; k++) {
            float[] p = w.city.findWalkable(b.doorX + w.rnd.nextFloat() * 12 - 6, b.doorY + w.rnd.nextFloat() * 12 - 6);
            if (p == null) break;
            Entity z = w.population.spawn(w.death.turnType(Entity.CIVILIAN), p[0], p[1]);
            if (z != null) z.fresh = 0;
        }
        w.emit(Sfx.THUD, b.doorX, b.doorY);
        w.emit(Sfx.GROAN, b.doorX, b.doorY);
        for (int k = 0; k < 12; k++)
            w.effects.particle(b.doorX, b.doorY, w.rnd.nextFloat() * 80 - 40, w.rnd.nextFloat() * 80 - 40, 0.5f, 1.2f, 0xFF8A6238, World.P_DEBRIS);
        if (World.isShop(b)) smash(b);
        b.infestKnown = true;
        w.dispatch.say(Dispatch.WHO_INFO, null, n + (n == 1 ? " zombie burst" : " zombies burst") + " out of "
                + (b.name != null ? b.name : "a building") + " on " + w.city.placeName(b.doorX, b.doorY) + "!", b.doorX, b.doorY);
    }

    /** Shop windows put in: glass all over the pavement. */
    void smash(City.Building b) {
        if (b.smashed) return;
        b.smashed = true;
        w.emit(Sfx.GLASS, b.doorX, b.doorY);
        for (int k = 0; k < 10; k++) {
            float gx = b.doorX + w.rnd.nextFloat() * 20 - 10, gy = b.doorY + w.rnd.nextFloat() * 8 - 4;
            w.effects.decal(gx, gy, 0.8f + w.rnd.nextFloat() * 1.2f, 0xCCCFE8F2, World.D_GLASS, w.rnd.nextFloat() * World.TAU);
        }
        for (int k = 0; k < 8; k++)
            w.effects.particle(b.doorX, b.doorY, w.rnd.nextFloat() * 60 - 30, w.rnd.nextFloat() * 60 - 30, 0.4f, 0.8f, 0xFFCFE8F2, World.P_DOT);
        w.emit(Sfx.CRASH, b.doorX, b.doorY);
    }

    /** (12.4) The dead are loose (or have taken the city): nobody shelters indoors. */
    boolean apocalypse() {
        return w.outbreak || w.aftermath == World.AFTER_FALLEN;
    }

    void leaveBuilding(Entity o, City.Building b, boolean panic) {
        // (Never refused: they were already one of the population before they went in.)
        float[] p = w.city.findWalkable(b.doorX + w.rnd.nextFloat() * 10 - 5, b.doorY + w.rnd.nextFloat() * 10 - 5);
        if (p == null) p = new float[]{b.doorX, b.doorY};
        o.x = p[0];
        o.y = p[1];
        o.vx = o.vy = 0;
        o.dead = false;
        o.removed = false;
        o.task = Dispatch.T_NONE;
        o.taskTimer = 4;
        if (panic) {
            o.fleeTimer = 3;
            o.threatX = b.doorX + (b.doorX - (b.x0 + b.x1) / 2);
            o.threatY = b.doorY + (b.doorY - (b.y0 + b.y1) / 2);
        }
        w.addEntity(o);
    }
}
