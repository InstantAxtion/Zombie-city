package com.instantaxtion.zombiesandbox;

import static com.instantaxtion.zombiesandbox.Fleet.*;

import java.util.ArrayList;

/**
 * ((27.3) Part of the vehicle code, out of Fleet.) The mobile command post trucks, the police's and the army's (their decisions are in Ops).
 */
final class FleetCommand {
    final Fleet fleet;
    final World w;
    final City city;

    FleetCommand(Fleet fleet) {
        this.fleet = fleet;
        this.w = fleet.w;
        this.city = fleet.city;
    }

    // ------------------------------------------------------------------ (26.0) the command post and the police boats

    /**
     * ((26.0) The police's mobile command post truck, setting out from (sx, sy) for (tx, ty) with these officers aboard
     * (it sets up there: see Ops). Null if there's no way there by road.
     */
    Vehicle commandPost(float sx, float sy, float tx, float ty, java.util.List<Entity> crew, int kind) {
        float[] g = city.nearestDrivable(sx, sy);
        if (g == null) return null;
        Vehicle v = fleet.make(TRUCK);
        fleet.setKind(v, kind);
        v.x = v.homeX = g[0];
        v.y = v.homeY = g[1];
        v.angle = (float) Math.atan2(ty - g[1], tx - g[0]);
        v.number = ++fleet.unitCount;
        v.place = city.placeName(tx, ty);
        v.passengerType = kind == K_ARMY_POST ? Entity.SOLDIER : Entity.COP;
        if (World.hypot(tx - g[0], ty - g[1]) < 40) {
            v.state = SCENE;
            v.tx = g[0];
            v.ty = g[1];
        } else {
            v.state = DRIVE;
            if (!fleet.routing.route(v, tx, ty)) return null;
        }
        if (crew != null) for (Entity e : crew) fleet.crews.board(v, e);
        fleet.vehicles.add(v);
        return v;
    }

    /**
     * ((26.1) A command post truck parked in the yard at the station or base f, waiting to be needed (see Ops). Null if
     * there's no road by it.
     */
    Vehicle parkPost(City.Facility f, int kind) {
        float[] g = city.nearestDrivable(f.gateX > 0 ? f.gateX : f.x, f.gateY > 0 ? f.gateY : f.y);
        if (g == null) return null;
        float[] p = fleet.armyUnits.bay(g, f);
        if (p == null) p = g;
        Vehicle v = fleet.make(TRUCK);
        fleet.setKind(v, kind);
        v.x = p[0];
        v.y = p[1];
        v.homeX = g[0];
        v.homeY = g[1];
        v.angle = (float) Math.atan2(g[1] - p[1], g[0] - p[0]);
        v.number = ++fleet.unitCount;
        v.place = f.name;
        v.passengerType = kind == K_ARMY_POST ? Entity.SOLDIER : Entity.COP;
        v.state = WAIT;
        fleet.vehicles.add(v);
        return v;
    }

    /** ((26.1) A command post truck back home: into a bay in the yard, ((26.3) its crew staying in it, waiting.) */
    void parkHome(Vehicle v, City.Facility f) {
        if (f != null) {
            float[] p = fleet.armyUnits.bay(new float[]{v.x, v.y}, f);
            if (p != null && World.hypot(p[0] - v.x, p[1] - v.y) < City.T * 8) {
                v.x = p[0];
                v.y = p[1];
            }
        }
        v.speed = 0;
        v.state = WAIT;
        v.place = f != null ? f.name : city.placeName(v.x, v.y);
    }

    /** ((26.3) A command post truck with its crew already in it drives off to (tx, ty). False if there's no way. */
    boolean postGo(Vehicle v, float tx, float ty) {
        v.tx = tx;
        v.ty = ty;
        v.place = city.placeName(tx, ty);
        v.stuckTimer = 0;
        v.state = DRIVE;
        if (fleet.routing.route(v, tx, ty)) return true;
        v.state = WAIT;
        return false;
    }

    /** ((26.3) Climbs into the parked command post truck, to wait there. */
    void seat(Vehicle v, java.util.List<Entity> crew) {
        for (Entity e : crew) fleet.crews.board(v, e);
    }

    /** ((26.1) The crew walk over to the command post truck and climb in; then it drives them to (tx, ty). */
    void musterPost(Vehicle v, java.util.List<Entity> crew, float tx, float ty) {
        v.state = MUSTER;
        v.timer = 0;
        v.coming = -1;
        v.speed = 0;
        v.crewWanted = crew.size();
        v.tx = tx;
        v.ty = ty;
        v.place = city.placeName(tx, ty);
        for (Entity e : crew) {
            e.rig = v;
            fleet.crews.note(e);
            e.task = Dispatch.T_BOARD;
        }
    }

    /** ((26.0) The command post: parked, its crew climbing in, driving there, then set up (Ops takes it from there). */
    boolean commandPostStep(Vehicle v, float dt) {
        if (v.state == WAIT) {
            v.speed = 0;
            return false;
        }
        if (v.state == MUSTER) {
            v.speed = 0;
            float was = v.timer;
            v.timer += dt;
            if (v.coming < 0 || (int) v.timer != (int) was) v.coming = fleet.service.comingTo(v);
            boolean ready = v.crew.size() >= v.crewWanted || (!v.crew.isEmpty() && v.coming == 0) || (v.timer > 60 && !v.crew.isEmpty());
            if (ready) {
                fleet.service.dropCrew(v);
                v.stuckTimer = 0;
                v.state = DRIVE;
                if (!fleet.routing.route(v, v.tx, v.ty)) {
                    // (No way there: they get out again.)
                    fleet.crews.crewOut(v, true);
                    v.state = WAIT;
                    w.ops.postUnmanned(v);
                }
            } else if (v.timer > 90 || (v.timer > 3 && v.coming == 0 && v.crew.isEmpty())) {
                fleet.service.dropCrew(v);
                v.state = WAIT;
                w.ops.postUnmanned(v);
            }
            return false;
        }
        if (v.state == SCENE) {
            v.speed = Math.max(0, v.speed - dt * 200);
            return false;
        }
        v.stuckTimer += dt;
        // ((26.6) Eyes on the road: the dead on the road ahead, round the truck, or where it's going - it turns off for
        // somewhere clear instead of driving on into them (it drove straight through a crowd of them and was torn apart).)
        v.checkCd -= dt;
        if (v.checkCd <= 0) {
            v.checkCd = 0.4f;
            float hx = (float) Math.cos(v.angle), hy = (float) Math.sin(v.angle);
            int ahead = w.countZombiesNear(v.x + hx * 70, v.y + hy * 70, 70) + w.countZombiesNear(v.x + hx * 150, v.y + hy * 150, 60)
                    + w.countZombiesNear(v.x, v.y, 45);
            boolean there = World.hypot(v.tx - v.x, v.ty - v.y) < 500 && w.countZombiesNear(v.tx, v.ty, 160) >= 2;
            if ((ahead >= 2 || there) && w.ops.postBlocked(v, v.x + hx * 100, v.y + hy * 100)) return false;
        }
        boolean moving = fleet.driving.driveStep(v, dt, 90, fleet.driving.carAhead(v) ? 0.5f : 1f);
        if (moving) fleet.driving.hit(v, w.runOver(v.x, v.y, 8, v.speed, v.angle));
        float d = (float) World.hypot(v.tx - v.x, v.ty - v.y);
        if (d < 36 || (!moving && d < 160) || (v.stuckTimer > 20 && d < 320)) {
            v.speed = 0;
            v.stuckTimer = 0;
            v.state = SCENE;
            w.ops.postArrived(v);
            return false;
        }
        if (moving && Math.abs(v.speed) > 12) v.stuckTimer = 0;
        else if (v.stuckTimer > 6) {
            // Held up, or lost its way: a new route there, or (with none) it sets up where it is.
            v.stuckTimer = 0;
            if (!fleet.routing.route(v, v.tx, v.ty)) {
                v.speed = 0;
                v.state = SCENE;
                w.ops.postArrived(v);
            }
        }
        return false;
    }

    /** ((26.0) The command post packs up and drives on to (tx, ty). False if there's no way there. */
    boolean movePost(Vehicle v, float tx, float ty) {
        v.state = DRIVE;
        v.stuckTimer = 0;
        v.place = city.placeName(tx, ty);
        if (fleet.routing.route(v, tx, ty)) return true;
        v.state = SCENE;
        return false;
    }
}
