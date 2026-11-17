package com.instantaxtion.zombiesandbox;

import static com.instantaxtion.zombiesandbox.Fleet.*;

import java.util.ArrayList;

/**
 * ((27.3) Part of the vehicle code, out of Fleet.) The evacuation buses.
 */
final class FleetEvac {
    final Fleet fleet;
    final World w;
    final City city;

    FleetEvac(Fleet fleet) {
        this.fleet = fleet;
        this.w = fleet.w;
        this.city = fleet.city;
    }

    /**
     * (10.49) It's over: an evacuation bus not yet on its way out lets its people off to walk home, and goes
     * back to being an ordinary bus. (Those already leaving go, and come home with everyone else.)
     */
    void exodusOver() {
        for (int i = 0; i < fleet.vehicles.size(); i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (v.evacStop < 0 || v.busPhase >= 2) continue;
            v.evacStop = -1;
            ArrayList<Entity> out = new ArrayList<Entity>(v.riders);
            fleet.service.dropRiders(v, false);
            for (Entity e : out) if (!e.dead && e.home != null) e.task = Dispatch.T_HOMECOMING;
            v.state = CRUISE;
            if (!fleet.routing.newDestination(v)) v.stuckTimer = 5;
        }
    }

    /** Evacuation buses: one to each stop where people are waiting (when it hasn't one already). */
    void sendEvacBuses() {
        if (!w.exodusOn() || !w.byTransit()) return;
        for (int k = 0; k < w.evacStops.size(); k++) {
            World.EvacStop st = w.evacStops.get(k);
            if (st.bus != null && (st.bus.removedFromFleet || st.bus.broken || st.bus.evacStop != k)) st.bus = null;
            st.busCd -= 2;
            if (st.airport && !st.closed && st.busCd <= 0) st.bus = null;
            if (st.bus != null || st.closed || st.busCd > 0 || w.waitingFor(k, st.x, st.y, false) < 3) continue;
            // ((15.10) The airport: an evacuation flight.)
            if (st.airport) {
                // (A second flight in at once when there's a crowd waiting.)
                int planes = 0;
                for (Vehicle o : fleet.vehicles) if (o.type == JET && o.evacStop == k) planes++;
                int waiting = w.waitingFor(k, st.x, st.y, false);
                if (planes < (waiting > PLANE_SEATS * 2 ? 2 : 1) && waiting >= 8 && fleet.aircraft.sendAirliner(k)) {
                    for (Vehicle o : fleet.vehicles) if (o.type == JET && o.evacStop == k) st.bus = o;
                    st.busCd = 20;
                }
                continue;
            }
            // ((15.9) A dock: a boat in from offshore.)
            if (st.dock) {
                Vehicle b = fleet.make(BOAT);
                b.boatPath = st.route;
                b.boatAt = 0;
                b.x = (st.route[0] % city.w + 0.5f) * City.T;
                b.y = (st.route[0] / city.w + 0.5f) * City.T;
                b.evacStop = k;
                b.busPhase = 0;
                b.state = DRIVE;
                fleet.vehicles.add(b);
                st.bus = b;
                st.busCd = 6;
                continue;
            }
            // In from the edge of the map, the nearest way in to the stop.
            float[] in = fleet.routing.exitNear(st.bx, st.by);
            if (in == null) continue;
            Vehicle v = fleet.make(CAR);
            v.model = M_BUS;
            v.color = city.country.busColor;
            v.x = in[0];
            v.y = in[1];
            v.state = CRUISE;
            v.evacStop = k;
            v.busPhase = 0;
            v.angle = (float) Math.atan2(st.by - in[1], st.bx - in[0]);
            if (!fleet.routing.route(v, st.bx, st.by)) {
                st.busCd = 20;
                continue;
            }
            fleet.vehicles.add(v);
            st.bus = v;
            st.busCd = 6;
            // ((23.0) A big crowd waiting: a convoy - a second and third bus behind the first, and a police car with them
            // to hold the stop while they load.)
            int waiting = w.waitingFor(k, st.x, st.y, false);
            int extra = waiting > 80 ? 2 : waiting > 40 ? 1 : 0;
            float ca = (float) Math.cos(v.angle), sa = (float) Math.sin(v.angle);
            for (int j = 1; j <= extra; j++) {
                Vehicle b = fleet.make(CAR);
                b.model = M_BUS;
                b.color = city.country.busColor;
                b.x = in[0] - ca * 34 * j;
                b.y = in[1] - sa * 34 * j;
                b.state = CRUISE;
                b.evacStop = k;
                b.busPhase = 0;
                b.angle = v.angle;
                if (fleet.routing.route(b, st.bx, st.by)) fleet.vehicles.add(b);
            }
            if (extra > 0) {
                fleet.service.send(Entity.COP, 2, in[0] - ca * 34 * (extra + 1), in[1] - sa * 34 * (extra + 1), st.bx, st.by, null, null, st.place);
                w.dispatch.say(Dispatch.WHO_POLICE, null, "Evacuation convoy of " + (extra + 1) + " buses on its way to " + st.place + ", police escort with it.", st.bx, st.by);
            }
        }
    }

    /** An evacuation bus: in to its stop, waits while people get on, then out of the city with them. */
    boolean updateEvacBus(Vehicle v, float dt) {
        World.EvacStop st = v.evacStop < w.evacStops.size() ? w.evacStops.get(v.evacStop) : null;
        if (v.busPhase == 0) {
            if (st == null || st.closed) {
                leaveWithBus(v, st);
                return false;
            }
            v.stuckTimer += dt;
            boolean going = fleet.driving.driveStep(v, dt, 75, fleet.driving.carAhead(v) ? 0.4f : 1f);
            if (going) fleet.driving.hit(v, w.runOver(v.x, v.y, 9, v.speed, v.angle));
            // ((24.0) The route running out short of the stop isn't arriving: it opened its doors wherever it was and
            // the people at the stop never reached it. Planned again from there, a few times.)
            if (!going && World.hypot(st.bx - v.x, st.by - v.y) > 60 && v.jamTries < 3 && fleet.routing.route(v, st.bx, st.by)) {
                v.jamTries++;
                return false;
            }
            if (!going || World.hypot(st.bx - v.x, st.by - v.y) < 28) {
                v.jamTries = 0;
                v.busPhase = 1;
                v.timer = 0;
                v.checkCd = 0;
            } else if (v.stuckTimer > 14) {
                v.stuckTimer = 0;
                if (!fleet.routing.route(v, st.bx, st.by)) leaveWithBus(v, st);
            }
            return false;
        }
        if (v.busPhase == 1) {
            v.speed = Math.max(0, v.speed - dt * 200);
            v.timer += dt;
            v.checkCd -= dt;
            if (v.checkCd > 0) return false;
            v.checkCd = 0.5f;
            if (st != null) w.boardEvac(v, v.evacStop, st.x, st.y, World.STOP_REACH + 8, BUS_SEATS - v.riders.size());
            boolean full = v.riders.size() >= BUS_SEATS, danger = w.countZombiesNear(v.x, v.y, 60) >= 2;
            boolean nobody = st == null || w.waitingFor(v.evacStop, st.x, st.y, true) == 0;
            // ((13.3) Longer at the stop for those still coming.)
            if (full || danger || (v.timer > 20 && nobody) || v.timer > 70) leaveWithBus(v, st);
            return false;
        }
        // On the way out.
        v.stuckTimer += dt;
        boolean going = fleet.driving.driveStep(v, dt, 85, fleet.driving.carAhead(v) ? 0.4f : 1f);
        if (going) fleet.driving.hit(v, w.runOver(v.x, v.y, 9, v.speed, v.angle));
        float edge = 4 * City.T;
        boolean atEdge = v.x < edge || v.y < edge || v.x > city.worldW() - edge || v.y > city.worldH() - edge;
        if (!going || atEdge) {
            if (atEdge || !fleet.routing.inView(v.x, v.y)) {
                w.droveOut(v.riders);
                return true;
            }
            if (!exodusReroute(v)) v.stuckTimer = Math.max(v.stuckTimer, 5);
        }
        if (v.stuckTimer > 12) {
            v.stuckTimer = 0;
            exodusReroute(v);
        }
        // (Stuck for good somewhere nobody's watching: it gets out of town another way.)
        if (v.stillT > 30 && !fleet.routing.inView(v.x, v.y)) {
            w.droveOut(v.riders);
            return true;
        }
        return false;
    }

    void leaveWithBus(Vehicle v, World.EvacStop st) {
        if (st != null && st.bus == v) {
            st.bus = null;
            st.busCd = 4;
        }
        v.busPhase = 2;
        v.exodus = true;
        v.fleeing = true;
        v.stuckTimer = 0;
        float[] out = fleet.routing.exitNear(v.x, v.y);
        if (out == null || !fleet.routing.route(v, out[0], out[1])) exodusReroute(v);
        if (v.riders.size() >= 8 && st != null && w.rnd.nextFloat() < 0.5f)
            w.dispatch.say(Dispatch.WHO_INFO, null, "News: An evacuation bus has left " + st.place + " with " + v.riders.size() + " aboard.", v.x, v.y);
    }

    /** (10.48) Stuck, or gone round in a circle, on the way out of the city: another way out (the nearest that's different). */
    boolean exodusReroute(Vehicle v) {
        v.destCd = 3;
        float[] exit = fleet.routing.exitNear(v.x + (w.rnd.nextFloat() - 0.5f) * 1600, v.y + (w.rnd.nextFloat() - 0.5f) * 1600);
        return exit != null && fleet.routing.route(v, exit[0], exit[1]);
    }
}
