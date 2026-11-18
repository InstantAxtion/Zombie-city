package com.instantaxtion.zombiesandbox;

import static com.instantaxtion.zombiesandbox.Fleet.*;

import java.util.ArrayList;

/**
 * ((27.3) Part of the vehicle code, out of Fleet.) Aeroplanes: jets on an air strike, airliners taking people out, transports flying troops in, and the airliner that comes down.
 */
final class FleetAircraft {
    final Fleet fleet;
    final World w;
    final City city;

    FleetAircraft(Fleet fleet) {
        this.fleet = fleet;
        this.w = fleet.w;
        this.city = fleet.city;
    }

    /** A strike jet: comes in from the nearest edge, drops a stick of bombs across the target, flies on. */
    void sendJet(float tx, float ty) {
        Vehicle v = fleet.make(JET);
        float a = w.rnd.nextFloat() * (float) Math.PI * 2;
        v.angle = a;
        v.x = tx - (float) Math.cos(a) * 900;
        v.y = ty - (float) Math.sin(a) * 900;
        v.tx = tx;
        v.ty = ty;
        v.speed = 380;
        v.alt = 1;
        v.state = DRIVE;
        fleet.vehicles.add(v);
        w.emit(Sfx.JET, tx, ty);
    }

    /** (15.10) An airliner in to the airport to take people out (the evacuation stop k). */
    boolean sendAirliner(int k) {
        if (city.runway == null || w.airportClosed) return false;
        Vehicle v = fleet.make(JET);
        fleet.setKind(v, K_AIRLINER);
        v.evacStop = k;
        startApproach(v);
        fleet.vehicles.add(v);
        return true;
    }

    /**
     * (15.10) Soldiers flown in to the airport on transports, sixty to a plane, off and away to the call once they're
     * down. False if there's no airport, or it's fallen.
     */
    boolean flyInTroops(int count, Dispatch.Incident inc, String place) {
        if (city.runway == null || w.airportClosed) return false;
        for (int b = 0; count > 0; b++, count -= TRANSPORT_LOAD) {
            Vehicle v = fleet.make(JET);
            fleet.setKind(v, K_TRANSPORT);
            v.passengers = Math.min(TRANSPORT_LOAD, count);
            v.passengerType = Entity.SOLDIER;
            v.reinforcement = true;
            v.incident = inc;
            v.place = place;
            startApproach(v);
            v.x -= b * 900;
            fleet.vehicles.add(v);
        }
        return true;
    }

    /** (15.10) A plane lined up on the runway from far out to the west, at height. */
    void startApproach(Vehicle v) {
        float ry = (city.runway[1] + city.runway[3]) / 2;
        v.x = Math.min(-200, city.runway[0] - 1400);
        v.y = ry;
        v.angle = 0;
        v.alt = 1;
        v.speed = 200;
        v.busPhase = 0;
        v.timer = 0;
    }

    /**
     * (15.10) A plane at the airport: in on the approach, down onto the runway and slowing, along to its stand by the
     * terminal; there, people board (an airliner) or the troops get off (a transport); then back to the end of the
     * runway, the take-off run, and away - with everyone aboard counted as out of the city.
     */
    boolean updatePlane(Vehicle v, float dt) {
        float[] rw = city.runway, ap = city.apron;
        float ry = (rw[1] + rw[3]) / 2, len = rw[2] - rw[0];
        World.EvacStop st = v.evacStop >= 0 && v.evacStop < w.evacStops.size() ? w.evacStops.get(v.evacStop) : null;
        float gx, gy, speed;
        switch (v.busPhase) {
            case 0: // The approach, coming down to the threshold.
                gx = rw[0] + 30;
                gy = ry;
                speed = 200;
                v.alt = Math.min(1, Math.max(0, (gx - v.x) / 900));
                if (w.airportClosed) {
                    // (The airport has fallen: it goes round and away again.)
                    v.busPhase = 6;
                    return false;
                }
                break;
            case 1: // Down and slowing along the runway.
                gx = rw[0] + len * 0.45f;
                gy = ry;
                speed = Math.max(60, v.speed - dt * 50);
                v.alt = 0;
                break;
            case 2: // Taxiing to the stand.
                gx = ap[0];
                gy = ap[1];
                speed = 45;
                break;
            case 3: { // At the stand.
                v.speed = 0;
                v.timer += dt;
                v.checkCd -= dt;
                if (v.kind == K_TRANSPORT) {
                    if (v.passengers > 0 && v.timer > 2) {
                        v.tx = v.x;
                        v.ty = v.y;
                        fleet.crews.unload(v, true);
                        v.passengers = 0;
                    }
                    if (v.timer > 8) v.busPhase = 4;
                    return false;
                }
                if (v.checkCd > 0) return false;
                v.checkCd = 0.5f;
                if (st != null) w.boardEvac(v, v.evacStop, st.x, st.y, World.STOP_REACH + 40, PLANE_SEATS - v.riders.size());
                boolean full = v.riders.size() >= PLANE_SEATS, danger = w.countZombiesNear(v.x, v.y, 120) >= 2 || w.airportClosed;
                boolean nobody = st == null || w.waitingFor(v.evacStop, st.x, st.y, true) == 0;
                if (full || danger || (v.timer > 25 && nobody) || v.timer > 90) {
                    v.busPhase = 4;
                    if (st != null && st.bus == v) {
                        st.bus = null;
                        st.busCd = 10;
                    }
                    if (v.riders.size() >= 10)
                        w.dispatch.say(Dispatch.WHO_INFO, null, "News: An evacuation flight has taken off from the airport with " + v.riders.size() + " aboard.", v.x, v.y);
                }
                return false;
            }
            case 4: // Back to the end of the runway.
                gx = rw[0] + 30;
                gy = ry;
                speed = 50;
                break;
            case 5: // The take-off run.
                gx = rw[2] + 60;
                gy = ry;
                speed = Math.min(220, v.speed + dt * 70);
                v.alt = Math.min(1, Math.max(0, (v.x - (rw[0] + len * 0.55f)) / (len * 0.45f)));
                break;
            default: // Away to the east.
                gx = city.worldW() + 600;
                gy = ry - 300;
                speed = 220;
                v.alt = 1;
                break;
        }
        float dx = gx - v.x, dy = gy - v.y, d = (float) Math.sqrt(dx * dx + dy * dy);
        v.speed = speed;
        if (d < Math.max(6, speed * dt)) {
            if (v.busPhase == 6 || v.busPhase >= 7) {
                w.droveOut(v.riders);
                return true;
            }
            v.busPhase = v.busPhase == 0 ? 1 : v.busPhase == 1 ? 2 : v.busPhase == 2 ? 3 : v.busPhase == 4 ? 5 : 6;
            v.timer = 0;
            v.checkCd = 0;
            return false;
        }
        v.x += dx / d * speed * dt;
        v.y += dy / d * speed * dt;
        v.angle = turnTo(v.angle, (float) Math.atan2(dy, dx), dt * (v.alt > 0.5f ? 1.5f : 3));
        if (v.busPhase == 6 && v.x > city.worldW() + 500) {
            w.droveOut(v.riders);
            return true;
        }
        return false;
    }

    /** ((25.0) A passenger jet in trouble: in low from far off, an engine on fire, down on (tx, ty). */
    void sendCrashingPlane(float tx, float ty) {
        Vehicle v = fleet.make(JET);
        fleet.setKind(v, K_CRASH);
        float a = w.rnd.nextFloat() * (float) Math.PI * 2;
        v.angle = a;
        v.x = tx - (float) Math.cos(a) * CRASH_RUN;
        v.y = ty - (float) Math.sin(a) * CRASH_RUN;
        v.tx = tx;
        v.ty = ty;
        v.speed = 210;
        v.alt = 0.6f;
        v.state = DRIVE;
        fleet.vehicles.add(v);
        w.emit(Sfx.JET, tx, ty);
    }

    boolean updateCrash(Vehicle v, float dt) {
        float dx = v.tx - v.x, dy = v.ty - v.y, d = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
        if (d < v.speed * dt + 2) {
            w.planeCrashed(v.tx, v.ty, v.angle);
            return true;
        }
        v.x += dx / d * v.speed * dt;
        v.y += dy / d * v.speed * dt;
        v.alt = Math.min(0.6f, d / CRASH_RUN * 0.6f);
        float ca = (float) Math.cos(v.angle), sa = (float) Math.sin(v.angle);
        // A wing (and its engine) on fire, and black smoke pouring off it.
        float ex = v.x - ca * 6 - sa * 30, ey = v.y - sa * 6 + ca * 30;
        if (w.rnd.nextFloat() < dt * 50) w.particle(ex, ey, -ca * 20, -sa * 20, 0.4f, 2.5f + w.rnd.nextFloat() * 2, 0xFFFF8A2A, World.P_FIRE);
        if (w.rnd.nextFloat() < dt * 40) w.particle(ex - ca * 10, ey - sa * 10, 0, 0, 2.5f, 5 + w.rnd.nextFloat() * 4, 0xFF2E2E2E, World.P_SMOKE);
        if ((v.soundCd -= dt) <= 0) {
            v.soundCd = 1.2f;
            w.emit(Sfx.JET, v.x, v.y);
        }
        return false;
    }

    boolean updateJet(Vehicle v, float dt) {
        if (v.kind == K_CRASH) return updateCrash(v, dt);
        if (v.kind == K_AIRLINER || v.kind == K_TRANSPORT) return updatePlane(v, dt);
        v.x += (float) Math.cos(v.angle) * v.speed * dt;
        v.y += (float) Math.sin(v.angle) * v.speed * dt;
        float d = (float) World.hypot(v.tx - v.x, v.ty - v.y);
        v.gunCd -= dt;
        boolean over = d < 120;
        if (over && v.gunCd <= 0) {
            v.gunCd = 0.12f;
            v.timer = 1;
            // The bombs take a moment to fall.
            w.blastLater(v.x, v.y, 55, 280, 0.8f);
        }
        if (w.rnd.nextFloat() < dt * 40)
            w.particle(v.x - (float) Math.cos(v.angle) * 14, v.y - (float) Math.sin(v.angle) * 14, 0, 0, 2.5f, 2.5f, 0xFFE8E8E8,
                    World.P_SMOKE);
        return World.hypot(v.x - v.tx, v.y - v.ty) > 1000 && v.timer > 0;
    }
}
