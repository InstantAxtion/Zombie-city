package com.instantaxtion.zombiesandbox;

import static com.instantaxtion.zombiesandbox.Fleet.*;

import java.util.ArrayList;

/**
 * ((27.3) Part of the vehicle code, out of Fleet.) Ambulances.
 */
final class FleetAmbulances {
    final Fleet fleet;
    final World w;
    final City city;

    FleetAmbulances(Fleet fleet) {
        this.fleet = fleet;
        this.w = fleet.w;
        this.city = fleet.city;
    }

    /**
     * ((25.0) An ambulance out to a disaster: from the hospital (or in from the edge of town) with its lights going; at
     * the scene it waits a while patching up the hurt nearby, then goes back. False if it can't get there.
     */
    boolean sendAmbulance(float tx, float ty) {
        City.Facility h = city.nearestFacility(City.FACILITY_HOSPITAL, tx, ty);
        float[] start = h != null ? city.nearestDrivable(h.gateX, h.gateY) : null;
        if (start == null) {
            float[] e = city.edgeSpawn(tx, ty);
            start = e != null ? city.nearestDrivable(e[0], e[1]) : null;
        }
        float[] to = city.nearestDrivable(tx, ty);
        if (start == null || to == null) return false;
        Vehicle v = fleet.make(AMBULANCE);
        v.x = start[0];
        v.y = start[1];
        v.homeX = start[0];
        v.homeY = start[1];
        if (!fleet.routing.route(v, to[0], to[1])) return false;
        v.state = DRIVE;
        v.timer = 0;
        v.angle = (float) Math.atan2(to[1] - v.y, to[0] - v.x);
        fleet.vehicles.add(v);
        w.emit(Sfx.AMB_SIREN, v.x, v.y);
        return true;
    }

    boolean ambulanceStep(Vehicle v, float dt) {
        if (v.state == SCENE) {
            v.speed = 0;
            v.timer -= dt;
            if ((v.checkCd -= dt) <= 0) {
                v.checkCd = 1;
                for (int i = 0, n = w.entities.size(); i < n; i++) {
                    Entity e = w.entities.get(i);
                    if (e.dead || e.isZombie() || e.infected || Math.abs(e.x - v.x) > 70 || Math.abs(e.y - v.y) > 70) continue;
                    e.hp = Math.min(e.maxHp, e.hp + 8);
                }
            }
            if (v.timer <= 0 || w.countZombiesNear(v.x, v.y, 50) >= 3) {
                v.state = RETURN;
                v.stuckTimer = 0;
                return !fleet.routing.route(v, v.homeX, v.homeY);
            }
            return false;
        }
        v.timer += dt;
        boolean moving = fleet.driving.driveStep(v, dt, 45, 1);
        if (v.state == DRIVE && (!moving || v.timer > 90)) {
            v.state = SCENE;
            v.timer = 45;
            v.speed = 0;
            w.dispatch.chatter(Dispatch.WHO_POLICE, null, "EMS: On scene at " + city.placeName(v.x, v.y) + ", treating the injured.", v.x, v.y);
            return false;
        }
        return v.state == RETURN && (!moving || v.timer > 200);
    }
}
