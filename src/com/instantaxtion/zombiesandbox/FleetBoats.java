package com.instantaxtion.zombiesandbox;

import static com.instantaxtion.zombiesandbox.Fleet.*;

import java.util.ArrayList;

/**
 * ((27.3) Part of the vehicle code, out of Fleet.) Boats: the marines' landing craft, evacuation boats, and the police's patrol boats (their work is in Ops).
 */
final class FleetBoats {
    final Fleet fleet;
    final World w;
    final City city;

    FleetBoats(Fleet fleet) {
        this.fleet = fleet;
        this.w = fleet.w;
        this.city = fleet.city;
    }

    /** ((26.0) A police boat on the water at (x, y), based at the landing (lx, ly). */
    Vehicle policeBoat(float x, float y, float lx, float ly, int number) {
        Vehicle v = fleet.make(BOAT);
        fleet.setKind(v, K_POLICE_BOAT);
        v.x = v.homeX = x;
        v.y = v.homeY = y;
        v.homeLandX = v.landX = lx;
        v.homeLandY = v.landY = ly;
        v.angle = (float) Math.atan2(y - ly, x - lx);
        v.turret = v.angle;
        v.boatNum = number;
        v.boatJob = Ops.J_IDLE;
        v.jobT = number * 2;
        v.state = WAIT;
        v.passengerType = Entity.COP;
        fleet.vehicles.add(v);
        return v;
    }

    /**
     * (11.0) Marines by boat (on the maps with water): landing craft come in off the sea or the river to the shore
     * nearest the fighting, eight Marines to a boat, put them ashore, and pull back out. False if there's no way in by
     * water.
     */
    /**
     * ((15.9) From marineLanding, for the evacuation boats too.) The way in by water to the bit of open shore nearest
     * (tx, ty) that has a way on foot to it: {the route of water tiles from just offshore, {the land tile, the last
     * water tile}}, or null if there's none (no sea that reaches the edge of the map).
     */
    int[][] seaApproach(float tx, float ty) {
        int w0 = city.w, h0 = city.h, n = w0 * h0;
        int[] dist = new int[n];
        java.util.Arrays.fill(dist, Integer.MAX_VALUE);
        int[] q = new int[n];
        int head = 0, tail = 0;
        for (int x = 0; x < w0; x++)
            for (int y : new int[]{0, h0 - 1}) {
                int t = y * w0 + x;
                if (city.tiles[t] == City.WATER && dist[t] == Integer.MAX_VALUE) {
                    dist[t] = 0;
                    q[tail++] = t;
                }
            }
        for (int y = 0; y < h0; y++)
            for (int x : new int[]{0, w0 - 1}) {
                int t = y * w0 + x;
                if (city.tiles[t] == City.WATER && dist[t] == Integer.MAX_VALUE) {
                    dist[t] = 0;
                    q[tail++] = t;
                }
            }
        if (tail == 0) return null;
        while (head < tail) {
            int c = q[head++], x = c % w0, y = c / w0;
            int[] nb = {x > 0 ? c - 1 : -1, x < w0 - 1 ? c + 1 : -1, y > 0 ? c - w0 : -1, y < h0 - 1 ? c + w0 : -1};
            for (int j : nb)
                // (Under the bridges too.)
                if (j >= 0 && dist[j] == Integer.MAX_VALUE && (city.tiles[j] == City.WATER || (city.bridge != null && city.bridge[j]))) {
                    dist[j] = dist[c] + 1;
                    q[tail++] = j;
                }
        }
        // The landing: the water beside a bit of open shore nearest the fighting.
        // ((15.9) Looked for further and further out until there's one (a call deep inland on a big island had none
        // within reach, and no Marines came); never a pier or a jetty, nor the prison island; and only a shore with
        // a way on foot from it to the fighting - they were put ashore where they couldn't get anywhere.)
        int best = -1, land = -1;
        int tx0 = (int) (tx / City.T), ty0 = (int) (ty / City.T);
        float[] goal = city.findWalkable(tx, ty);
        for (int reach : new int[]{100, 250, Math.max(w0, h0)}) {
            java.util.ArrayList<long[]> cands = new java.util.ArrayList<long[]>();
            for (int y = Math.max(1, ty0 - reach); y < Math.min(h0 - 1, ty0 + reach); y++)
                for (int x = Math.max(1, tx0 - reach); x < Math.min(w0 - 1, tx0 + reach); x++) {
                    int t = y * w0 + x;
                    if (dist[t] == Integer.MAX_VALUE || city.tiles[t] != City.WATER) continue;
                    for (int j : new int[]{t - 1, t + 1, t - w0, t + w0}) {
                        byte jt = city.tiles[j];
                        if (jt == City.WATER || jt == City.PIER || city.solidTile(j % w0, j / w0) || (city.bridge != null && city.bridge[j])) continue;
                        if (w.inPrison((j % w0 + 0.5f) * City.T, (j / w0 + 0.5f) * City.T)) continue;
                        long dx = j % w0 - tx0, dy = j / w0 - ty0;
                        cands.add(new long[]{dx * dx + dy * dy, t, j});
                    }
                }
            if (cands.isEmpty()) continue;
            java.util.Collections.sort(cands, new java.util.Comparator<long[]>() {
                @Override
                public int compare(long[] a, long[] b) {
                    return Long.compare(a[0], b[0]);
                }
            });
            // (The nearest few, until one has a way on foot to the call; spread out so it's not the same bit of
            // beach tried eight times over.)
            int tried = 0;
            long lastD = -1;
            for (int k = 0; k < cands.size() && tried < 8 && best < 0; k++) {
                long[] cd = cands.get(k);
                if (lastD >= 0 && cd[0] - lastD < 36) continue;
                lastD = cd[0];
                tried++;
                int j = (int) cd[2];
                float lx = (j % w0 + 0.5f) * City.T, ly = (j / w0 + 0.5f) * City.T;
                if (goal == null || city.findPath(lx, ly, goal[0], goal[1], 60000) != null) {
                    best = (int) cd[1];
                    land = j;
                }
            }
            if (best >= 0) break;
        }
        if (best < 0) return null;
        // The way in: from the shore back out along the falling distances, then turned round.
        java.util.ArrayList<Integer> path = new java.util.ArrayList<Integer>();
        for (int c = best; ; ) {
            path.add(c);
            if (dist[c] == 0) break;
            int x = c % w0, y = c / w0, next = -1;
            for (int j : new int[]{x > 0 ? c - 1 : -1, x < w0 - 1 ? c + 1 : -1, y > 0 ? c - w0 : -1, y < h0 - 1 ? c + w0 : -1})
                if (j >= 0 && dist[j] == dist[c] - 1) next = j;
            if (next < 0) break;
            c = next;
        }
        java.util.Collections.reverse(path);
        // ((15.9) In from just offshore, not from the far edge of the map: on the big maps the boats were minutes
        // out at sea before they got anywhere near.)
        int skip = Math.max(0, path.size() - MARINE_RUN);
        int[] route = new int[path.size() - skip];
        for (int k = 0; k < route.length; k++) route[k] = path.get(k + skip);
        return new int[][]{route, {land, best}};
    }

    boolean marineLanding(int count, float tx, float ty, String place, Dispatch.Incident inc) {
        int[][] a = seaApproach(tx, ty);
        if (a == null) return false;
        int w0 = city.w;
        int[] route = a[0];
        int land = a[1][0];
        int boats = (count + 7) / 8;
        for (int b = 0; b < boats; b++) {
            Vehicle v = fleet.make(BOAT);
            v.boatPath = route;
            v.boatAt = 0;
            v.x = (route[0] % w0 + 0.5f) * City.T;
            v.y = (route[0] / w0 + 0.5f) * City.T;
            // (One behind the other, a few seconds apart.)
            v.timer = -b * 4f;
            v.tx = (land % w0 + 0.5f) * City.T;
            v.ty = (land / w0 + 0.5f) * City.T;
            v.passengers = Math.min(8, count - b * 8);
            v.passengerType = Entity.SOLDIER;
            v.incident = inc;
            v.place = place;
            v.reinforcement = true;
            v.state = DRIVE;
            fleet.vehicles.add(v);
        }
        return true;
    }

    /** (15.9) An evacuation boat: in along the water to its dock, people aboard, then back out to sea with them. */
    boolean updateEvacBoat(Vehicle v, float dt) {
        World.EvacStop st = v.evacStop < w.evacStops.size() ? w.evacStops.get(v.evacStop) : null;
        int[] path = v.boatPath;
        if (v.busPhase == 1) {
            v.speed = 0;
            v.timer += dt;
            v.checkCd -= dt;
            if (v.checkCd > 0) return false;
            v.checkCd = 0.5f;
            if (st != null) w.boardEvac(v, v.evacStop, st.x, st.y, World.STOP_REACH + 16, BOAT_SEATS - v.riders.size());
            boolean full = v.riders.size() >= BOAT_SEATS, danger = st == null || w.countZombiesNear(st.x, st.y, 50) >= 2;
            boolean nobody = st == null || w.waitingFor(v.evacStop, st.x, st.y, true) == 0;
            if (full || danger || (v.timer > 20 && nobody) || v.timer > 70) {
                v.busPhase = 2;
                v.boatAt = path.length - 1;
                if (st != null && st.bus == v) {
                    st.bus = null;
                    st.busCd = 4;
                }
                if (v.riders.size() >= 6 && st != null && w.rnd.nextFloat() < 0.6f)
                    w.dispatch.say(Dispatch.WHO_INFO, null, "News: A boat has left " + st.place + " with " + v.riders.size() + " aboard.", v.x, v.y);
            }
            return false;
        }
        boolean in = v.busPhase == 0;
        if (in && (st == null || st.closed)) {
            v.busPhase = 2;
            v.boatAt = Math.max(0, Math.min(path.length - 1, v.boatAt));
            in = false;
        }
        if (in ? v.boatAt >= path.length : v.boatAt < 0) {
            if (in) {
                v.busPhase = 1;
                v.timer = 0;
                v.checkCd = 0;
                return false;
            }
            // Out at sea: away with everyone aboard.
            w.droveOut(v.riders);
            return true;
        }
        int t = path[v.boatAt];
        float gx = (t % city.w + 0.5f) * City.T, gy = (t / city.w + 0.5f) * City.T;
        float dx = gx - v.x, dy = gy - v.y, d = (float) Math.sqrt(dx * dx + dy * dy);
        v.speed = Math.min(v.speed + dt * 50, 110 * World.VEHICLE_SPEED);
        if (d < 4) v.boatAt += in ? 1 : -1;
        else {
            float step = Math.min(d, v.speed * dt);
            v.x += dx / d * step;
            v.y += dy / d * step;
            v.angle = turnTo(v.angle, (float) Math.atan2(dy, dx), dt * 3);
        }
        return false;
    }

    boolean updateBoat(Vehicle v, float dt) {
        if (v.kind == K_POLICE_BOAT) return w.ops.boatStep(v, dt);
        if (v.evacStop >= 0) return updateEvacBoat(v, dt);
        v.timer += dt;
        if (v.timer < 0) return false;
        if (v.state == DRIVE) {
            int w0 = city.w;
            if (v.boatAt >= v.boatPath.length) {
                v.speed = 0;
                // Ashore: everyone out on the bank.
                float sx = v.x, sy = v.y;
                v.x = v.tx;
                v.y = v.ty;
                fleet.crews.unload(v, true);
                v.x = sx;
                v.y = sy;
                v.passengers = 0;
                v.state = SCENE;
                v.timer = 0;
                return false;
            }
            int t = v.boatPath[v.boatAt];
            float gx = (t % w0 + 0.5f) * City.T, gy = (t / w0 + 0.5f) * City.T;
            float dx = gx - v.x, dy = gy - v.y, d = (float) Math.sqrt(dx * dx + dy * dy);
            v.speed = Math.min(v.speed + dt * 60, 160 * World.VEHICLE_SPEED);
            if (d < 4) v.boatAt++;
            else {
                float step = Math.min(d, v.speed * dt);
                v.x += dx / d * step;
                v.y += dy / d * step;
                v.angle = turnTo(v.angle, (float) Math.atan2(dy, dx), dt * 3);
            }
            return false;
        }
        // Covering the landing a while, then away.
        v.speed = 0;
        return v.timer > 25;
    }
}
