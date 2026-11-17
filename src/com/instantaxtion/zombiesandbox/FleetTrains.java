package com.instantaxtion.zombiesandbox;

import static com.instantaxtion.zombiesandbox.Fleet.*;

import java.util.ArrayList;

/**
 * ((27.3) Part of the vehicle code, out of Fleet.) The railway: trains, the level crossings, and cars waiting at them.
 */
final class FleetTrains {
    final Fleet fleet;
    final World w;
    final City city;

    FleetTrains(Fleet fleet) {
        this.fleet = fleet;
        this.w = fleet.w;
        this.city = fleet.city;
    }

    /** A train is on the line near this car's crossing: wait. */
    boolean trainComing(Vehicle v) {
        if (city.railY0 < 0 || city.underRailBridge((int) (v.x / City.T))) return false;
        float ry = (city.railY0 + city.railRows / 2f) * City.T;
        float dy = ry - v.y;
        if (Math.abs(dy) > 60 || Math.abs(dy) < 26) return false;
        // Only if heading towards the tracks.
        if ((float) Math.sin(v.angle) * dy <= 0) return false;
        for (int i = 0; i < fleet.vehicles.size(); i++) {
            Vehicle t = fleet.vehicles.get(i);
            if (t.type != TRAIN) continue;
            float ahead = (v.x - t.x) * (float) Math.cos(t.angle);
            if (ahead > -TRAIN_LENGTH - 20 && ahead < 260) return true;
        }
        return false;
    }

    void spawnTrain() {
        Vehicle v = fleet.make(TRAIN);
        boolean east = w.rnd.nextBoolean();
        v.state = DRIVE;
        v.y = (city.railY0 + city.railRows / 2f) * City.T;
        v.x = east ? -20 : city.worldW() + 20;
        v.angle = east ? 0 : (float) Math.PI;
        v.speed = 110;
        fleet.vehicles.add(v);
    }

    /** A train is at, or about to reach, the level crossing at x: the barriers are down. */
    boolean crossingClosed(float x) {
        for (int i = 0; i < fleet.vehicles.size(); i++) {
            Vehicle t = fleet.vehicles.get(i);
            if (t.type != TRAIN) continue;
            float ahead = (x - t.x) * (float) Math.cos(t.angle);
            if (ahead > -TRAIN_LENGTH - 24 && ahead < 300) return true;
        }
        return false;
    }

    boolean updateTrain(Vehicle v, float dt) {
        float dir = (float) Math.cos(v.angle);
        if (v.state == WAIT) {
            // A short stop at the platform.
            v.speed = 0;
            v.timer -= dt;
            float mid = v.x - dir * TRAIN_LENGTH / 2;
            // (10.49) People leaving the city get on (a long stop, while there are more of them coming).
            if (w.exodusOn() && w.byTransit()) {
                v.checkCd -= dt;
                if (v.checkCd <= 0) {
                    v.checkCd = 0.5f;
                    int got = w.boardEvac(v, World.STOP_TRAIN, city.stationX, city.stationY, TRAIN_LENGTH * 0.7f, TRAIN_SEATS - v.riders.size());
                    if (!v.riders.isEmpty()) v.exodus = true;
                    if (got > 0 || w.waitingFor(World.STOP_TRAIN, city.stationX, city.stationY, true) > 0) v.timer = Math.max(v.timer, 4);
                    if (v.riders.size() >= TRAIN_SEATS) v.timer = 0;
                    if (v.timer <= 0 && v.riders.size() >= 10 && !v.said) {
                        v.said = true;
                        w.dispatch.say(Dispatch.WHO_INFO, null, "News: An evacuation train has left " + city.stationName + " with "
                                + v.riders.size() + " aboard.", v.x, v.y);
                    }
                }
            }
            if (v.timer <= 0 || w.countZombiesNear(mid, v.y, 80) > 2) {
                v.state = DRIVE;
                w.emit(Sfx.HORN, v.x, v.y);
            }
            return false;
        }
        float cruise = 110;
        if (!v.pad && city.stationX >= 0) {
            // Slow down for the station and stop with the train alongside the platform.
            float ahead = (city.stationX + dir * TRAIN_LENGTH / 2 - v.x) * dir;
            if (ahead < -4) v.pad = true;
            else if (ahead < 340) {
                boolean danger = w.countZombiesNear(city.stationX, v.y, 150) > 0;
                if (danger) {
                    v.pad = true;
                } else {
                    cruise = Math.max(5, ahead * 0.38f);
                    if (ahead < 3) {
                        v.state = WAIT;
                        v.pad = true;
                        v.speed = 0;
                        v.passengers = 0;
                        v.timer = 9;
                        return false;
                    }
                }
            }
        }
        v.speed += (cruise - v.speed) * Math.min(1, dt * (cruise < v.speed ? 1.2f : 0.35f));
        v.x += dir * v.speed * dt;
        // Anything on the line gets hit: zombies go flying, people are knocked aside, cars are wrecked.
        // (Not on the bridge: the highway runs underneath.)
        for (float k = 0; k < TRAIN_LENGTH; k += 16)
            if (!city.underRailBridge((int) ((v.x - dir * k) / City.T))) w.runOver(v.x - dir * k, v.y, 9, v.speed, v.angle);
        for (int i = 0; i < fleet.vehicles.size(); i++) {
            Vehicle o = fleet.vehicles.get(i);
            if (o == v || offRoad(o) || o.broken || city.underRailBridge((int) (o.x / City.T))) continue;
            float along = (o.x - v.x) * dir;
            if (along < 8 && along > -TRAIN_LENGTH && Math.abs(o.y - v.y) < 14) fleet.damage(o, 500, true);
        }
        if (v.soundCd <= 0) {
            v.soundCd = 4;
            w.emit(Sfx.HORN, v.x, v.y);
        }
        return v.x < -TRAIN_LENGTH - 40 || v.x > city.worldW() + TRAIN_LENGTH + 40;
    }
}
