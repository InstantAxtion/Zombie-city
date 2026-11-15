package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

/**
 * (26.0) Special operations: the police's patrol boats on the water (patrolling, picking people up who are cornered at
 * the water's edge, taking officers across to calls they can't drive to), the police's mobile command post (out to the
 * worst of an outbreak, where it steadies and resupplies the officers round it and gets the calls near it seen to first,
 * until the dead overrun it), and the army's special forces (a team of four flown in to the hardest calls).
 */
final class Ops {
    private final World w;

    Ops(World w) {
        this.w = w;
    }

    /** Once a step (World.update). */
    void step(float dt) {
        if (!boats.isEmpty() && (boatTick -= dt) <= 0) {
            boatTick = 2;
            lookForStranded();
        }
        if ((tick -= dt) <= 0) {
            tick = 1;
            postTick();
            if ((sfTick -= 1) <= 0) {
                sfTick = 5;
                sfCheck();
            }
        }
    }

    private float tick, sfTick = 5;

    // ------------------------------------------------------------------ the police boats

    /** What a police boat is doing (Vehicle.boatJob). */
    static final int J_PATROL = 0, J_IDLE = 1, J_RESCUE = 2, J_LOAD = 3, J_DROP = 4, J_FERRY = 5, J_BOARD = 6;
    static final int BOAT_SEATS = 8;
    static final float BOAT_SPEED = 100;
    final ArrayList<Fleet.Vehicle> boats = new ArrayList<Fleet.Vehicle>();
    /** People taken off the shore to safety, and officers taken across the water, so far. */
    int rescued, ferried;
    /** The water the boats can get about on, and the land at its edge (two tiles in). */
    private BitSet water, shore;
    /** For the searches over the water: the way back (1-4) to where each tile was reached from, and a ring of tiles. */
    private byte[] back;
    private final int[] ring = new int[1 << 16];
    private float boatTick = 2;
    private float strandSaidAt = -99;

    private boolean wet(int t) {
        City c = w.city;
        return c.tiles[t] == City.WATER || (c.bridge != null && c.bridge[t]);
    }

    private boolean land(int t) {
        City c = w.city;
        return c.tiles[t] != City.WATER && (c.bridge == null || !c.bridge[t]) && !c.solid[t];
    }

    /**
     * The police boats, a few to a map on the water (the sea, the bay, the river or round the islands), each moored at
     * a landing of its own: one by a police station near the water, a pier, or a bit of the waterfront in town. Once,
     * when the city is made (or loaded: the boats aren't saved, they're simply back at their moorings).
     */
    void setupBoats() {
        City c = w.city;
        if (!boats.isEmpty() || c.waterKind == CityConfig.W_NONE || c.waterKind == CityConfig.W_LAKE || c.cfg.mission >= 0) return;
        int W = c.w, H = c.h;
        water = new BitSet(W * H);
        shore = new BitSet(W * H);
        int want = Math.max(2, Math.min(4, (int) (c.worldW() / 2200)));
        java.util.Random r = new java.util.Random(c.cfg.seed * 31L + 26);
        ArrayList<int[]> cands = new ArrayList<int[]>();
        // By the police stations first.
        for (City.Facility f : c.facilities) {
            if (f.kind != City.FACILITY_POLICE) continue;
            int[] s = shoreNear(f.x, f.y, 40, false);
            if (s != null) cands.add(s);
        }
        // Then the piers.
        ArrayList<int[]> piers = new ArrayList<int[]>();
        for (int t = W; t < W * (H - 1); t++) {
            if (c.tiles[t] != City.PIER) continue;
            int x = t % W;
            if (x == 0 || x == W - 1) continue;
            for (int j : new int[]{t - 1, t + 1, t - W, t + W})
                if (c.tiles[j] == City.WATER) {
                    piers.add(new int[]{j, t});
                    break;
                }
        }
        java.util.Collections.shuffle(piers, r);
        cands.addAll(piers);
        // Then the waterfront in town.
        for (int k = 0; k < 40; k++) {
            float[] q = c.randomWalkableInTown(r);
            if (q == null) continue;
            int[] s = shoreNear(q[0], q[1], 24, false);
            if (s != null) cands.add(s);
        }
        ArrayList<int[]> picked = new ArrayList<int[]>();
        for (int k = 0; k < cands.size() && picked.size() < want; k++) {
            int[] s = cands.get(k);
            float sx = tx(s[1]), sy = ty(s[1]);
            boolean close = false;
            for (int[] o : picked) if (World.hypot(tx(o[1]) - sx, ty(o[1]) - sy) < 1100) close = true;
            if (close) continue;
            // (Open water that goes somewhere: not a pond or a moat.)
            if (!water.get(s[0]) && flood(s[0]) < 400) continue;
            picked.add(s);
        }
        if (picked.isEmpty()) {
            water = shore = null;
            return;
        }
        // The land at the water's edge.
        for (int t = water.nextSetBit(0); t >= 0; t = water.nextSetBit(t + 1)) {
            int x = t % W, y = t / W;
            for (int dy = -2; dy <= 2; dy++)
                for (int dx = -2; dx <= 2; dx++) {
                    int xx = x + dx, yy = y + dy;
                    if (xx < 0 || yy < 0 || xx >= W || yy >= H) continue;
                    int j = yy * W + xx;
                    if (land(j)) shore.set(j);
                }
        }
        for (int[] s : picked) {
            float[] d = dock(s);
            boats.add(w.fleet.policeBoat(d[0], d[1], tx(s[1]), ty(s[1]), boats.size() + 1));
        }
    }

    /** Marks the water reachable from tile t as the boats', returning how much of it there is. */
    private int flood(int t0) {
        City c = w.city;
        int W = c.w, n = 0, head = 0, tail = 0, mask = ring.length - 1;
        BitSet seen = new BitSet(W * c.h);
        seen.set(t0);
        ring[tail++ & mask] = t0;
        ArrayList<Integer> got = new ArrayList<Integer>();
        while (head != tail) {
            int t = ring[head++ & mask];
            got.add(t);
            n++;
            int x = t % W;
            for (int k = 0; k < 4; k++) {
                int j = k == 0 ? (x > 0 ? t - 1 : -1) : k == 1 ? (x < W - 1 ? t + 1 : -1) : k == 2 ? t - W : t + W;
                if (j < 0 || j >= W * c.h || seen.get(j) || !wet(j)) continue;
                seen.set(j);
                if (tail - head >= ring.length) continue;
                ring[tail++ & mask] = j;
            }
        }
        if (n >= 400) for (int i = 0; i < got.size(); i++) water.set(got.get(i));
        return n;
    }

    private float tx(int t) {
        return (t % w.city.w + 0.5f) * City.T;
    }

    private float ty(int t) {
        return (t / w.city.w + 0.5f) * City.T;
    }

    /** Where a boat lies alongside landing s ({water tile, land tile}): in its water tile, close in to the land. */
    private float[] dock(int[] s) {
        float wx = tx(s[0]), wy = ty(s[0]), lx = tx(s[1]), ly = ty(s[1]);
        return new float[]{wx + (lx - wx) * 0.3f, wy + (ly - wy) * 0.3f};
    }

    /**
     * The nearest bit of shore to (x, y) within r tiles: {water tile, land tile beside it}, or null. Only the boats'
     * own water when mine is set (it can be got to); any open water otherwise (while they're being placed).
     */
    int[] shoreNear(float x, float y, int r, boolean mine) {
        City c = w.city;
        int W = c.w, H = c.h, cx = (int) (x / City.T), cy = (int) (y / City.T);
        for (int rad = 0; rad <= r; rad++) {
            int best = -1, bestW = -1, bd = Integer.MAX_VALUE;
            for (int dy = -rad; dy <= rad; dy++)
                for (int dx = -rad; dx <= rad; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != rad) continue;
                    int xx = cx + dx, yy = cy + dy;
                    if (xx < 1 || yy < 1 || xx >= W - 1 || yy >= H - 1) continue;
                    int t = yy * W + xx;
                    if (!land(t)) continue;
                    for (int j : new int[]{t - 1, t + 1, t - W, t + W}) {
                        if (mine ? !water.get(j) : c.tiles[j] != City.WATER) continue;
                        int d = dx * dx + dy * dy;
                        if (d < bd) {
                            bd = d;
                            best = t;
                            bestW = j;
                        }
                    }
                }
            if (best >= 0) return new int[]{bestW, best};
        }
        return null;
    }

    /**
     * A way over the water from tile a to tile b (tiles to steer for, straightened where the water's open), or null.
     * A search out from a over the boats' water until it reaches b.
     */
    private int[] waterPath(int a, int b) {
        City c = w.city;
        int W = c.w, N = W * c.h, mask = ring.length - 1;
        if (!water.get(a) || !water.get(b)) return null;
        if (back == null) back = new byte[N];
        else Arrays.fill(back, (byte) 0);
        int head = 0, tail = 0;
        back[a] = 5;
        ring[tail++ & mask] = a;
        boolean found = a == b;
        while (head != tail && !found) {
            int t = ring[head++ & mask], x = t % W;
            for (int k = 0; k < 4; k++) {
                int j = k == 0 ? (x > 0 ? t - 1 : -1) : k == 1 ? (x < W - 1 ? t + 1 : -1) : k == 2 ? t - W : t + W;
                if (j < 0 || j >= N || back[j] != 0 || !water.get(j)) continue;
                back[j] = (byte) (k + 1);
                if (j == b) {
                    found = true;
                    break;
                }
                if (tail - head >= ring.length) continue;
                ring[tail++ & mask] = j;
            }
        }
        if (!found) return null;
        ArrayList<Integer> rev = new ArrayList<Integer>();
        for (int t = b; ; ) {
            rev.add(t);
            int k = back[t];
            if (k == 5 || rev.size() > N) break;
            t = k == 1 ? t + 1 : k == 2 ? t - 1 : k == 3 ? t + W : t - W;
        }
        java.util.Collections.reverse(rev);
        // Straightened: on from each point to the furthest one ahead it can go straight to over open water.
        ArrayList<Integer> out = new ArrayList<Integer>();
        int i = 0;
        while (i < rev.size() - 1) {
            int far = i + 1;
            for (int j = Math.min(rev.size() - 1, i + 24); j > i + 1; j--)
                if (openWater(rev.get(i), rev.get(j))) {
                    far = j;
                    break;
                }
            out.add(rev.get(far));
            i = far;
        }
        int[] p = new int[out.size()];
        for (int k = 0; k < p.length; k++) p[k] = out.get(k);
        return p;
    }

    private boolean openWater(int a, int b) {
        float ax = tx(a), ay = ty(a), bx = tx(b), by = ty(b);
        float d = (float) World.hypot(bx - ax, by - ay);
        int n = (int) (d / 6) + 1;
        for (int k = 1; k < n; k++) {
            float f = k / (float) n;
            for (int s = -1; s <= 1; s += 2) {
                // (With a little room either side: the boat's not a point.)
                float px = ax + (bx - ax) * f - (by - ay) / d * 4 * s, py = ay + (by - ay) * f + (bx - ax) / d * 4 * s;
                int t = w.city.tileIndex(px, py);
                if (t < 0 || !water.get(t)) return false;
            }
        }
        return true;
    }

    /** Sets a boat off along the water to tile t, lying alongside at (fx, fy) at the end. False if it can't get there. */
    private boolean steerFor(Fleet.Vehicle v, int t, float fx, float fy) {
        int from = w.city.tileIndex(v.x, v.y);
        if (from < 0 || !water.get(from)) from = nearestWater(v.x, v.y);
        if (from < 0) return false;
        int[] p = waterPath(from, t);
        if (p == null) return false;
        v.boatPath = p;
        v.boatAt = 0;
        v.tx = fx;
        v.ty = fy;
        return true;
    }

    private int nearestWater(float x, float y) {
        int W = w.city.w, cx = (int) (x / City.T), cy = (int) (y / City.T);
        for (int r = 0; r <= 3; r++)
            for (int dy = -r; dy <= r; dy++)
                for (int dx = -r; dx <= r; dx++) {
                    int xx = cx + dx, yy = cy + dy;
                    if (xx < 0 || yy < 0 || xx >= W || yy >= w.city.h) continue;
                    if (water.get(yy * W + xx)) return yy * W + xx;
                }
        return -1;
    }

    /** Along its way over the water; true once it's alongside where it was going. */
    private boolean sail(Fleet.Vehicle v, float dt) {
        int[] p = v.boatPath;
        boolean last = p == null || v.boatAt >= p.length - 1;
        float gx = last ? v.tx : tx(p[v.boatAt]), gy = last ? v.ty : ty(p[v.boatAt]);
        float dx = gx - v.x, dy = gy - v.y, d = (float) Math.sqrt(dx * dx + dy * dy);
        if (last && d < 2.5f) {
            v.speed = 0;
            return true;
        }
        if (!last && d < 10) {
            v.boatAt++;
            return false;
        }
        float top = last ? Math.min(BOAT_SPEED, 12 + d * 1.6f) : BOAT_SPEED;
        v.speed = Math.min(top, v.speed + dt * 60);
        if (v.speed > top) v.speed = top;
        float step = Math.min(d, v.speed * dt);
        v.x += dx / d * step;
        v.y += dy / d * step;
        v.angle = turnTo(v.angle, (float) Math.atan2(dy, dx), dt * 3.5f);
        return false;
    }

    private static float turnTo(float from, float to, float amount) {
        float d = (float) Math.atan2(Math.sin(to - from), Math.cos(to - from));
        return from + Math.max(-amount, Math.min(amount, d));
    }

    /** A police boat's step (from Fleet.updateBoat). Never gone: true only if its water's been lost. */
    boolean boatStep(Fleet.Vehicle v, float dt) {
        if (water == null) return true;
        if (!boats.contains(v)) boats.add(v);
        // The officers aboard shoot from it.
        w.fleet.turret(v, dt);
        v.jobT += dt;
        switch (v.boatJob) {
            case J_PATROL:
                if (v.boatPath == null) patrol(v);
                else if (sail(v, dt)) {
                    v.boatJob = J_IDLE;
                    v.jobT = 0;
                }
                break;
            case J_IDLE:
                v.speed = 0;
                // (Bobbing on the water a little while, then on round.)
                if (v.jobT > 5 + (v.boatNum % 3) * 2) patrol(v);
                break;
            case J_RESCUE:
                if (sail(v, dt)) {
                    v.boatJob = J_LOAD;
                    v.jobT = 0;
                }
                break;
            case J_LOAD:
                takeAboard(v, dt);
                break;
            case J_FERRY:
                v.pickup = true;
                if (sail(v, dt)) {
                    v.boatJob = J_BOARD;
                    v.jobT = 0;
                    v.pickup = false;
                }
                break;
            case J_BOARD:
                officersAboard(v);
                break;
            case J_DROP:
                if (sail(v, dt)) ashore(v);
                break;
        }
        v.state = v.boatJob == J_IDLE ? Fleet.WAIT : v.boatJob == J_PATROL ? Fleet.PATROL : Fleet.DRIVE;
        return false;
    }

    /** Off round the water: somewhere out from its mooring, or home to it. */
    private void patrol(Fleet.Vehicle v) {
        v.boatJob = J_PATROL;
        v.jobT = 0;
        v.pickupFor = null;
        java.util.Random r = w.rnd;
        for (int k = 0; k < 10; k++) {
            float px = v.homeX + (r.nextFloat() - 0.5f) * 2600, py = v.homeY + (r.nextFloat() - 0.5f) * 2600;
            int t = w.city.tileIndex(px, py);
            if (t < 0 || !water.get(t)) continue;
            if (World.hypot(px - v.x, py - v.y) < 300) continue;
            if (steerFor(v, t, tx(t), ty(t))) return;
        }
        int home = w.city.tileIndex(v.homeX, v.homeY);
        if (home >= 0 && water.get(home) && steerFor(v, home, v.homeX, v.homeY)) return;
        v.boatJob = J_IDLE;
    }

    private boolean free(Fleet.Vehicle v) {
        return !v.removedFromFleet && (v.boatJob == J_PATROL || v.boatJob == J_IDLE) && v.riders.isEmpty() && v.crew.isEmpty();
    }

    /**
     * Every couple of seconds: anyone cornered at the water's edge by the dead - one of the living (not police or
     * soldiers) on the shore with the dead close - and the nearest boat that's free goes in for them.
     */
    private void lookForStranded() {
        boolean any = false;
        for (int i = 0; i < boats.size(); i++) if (free(boats.get(i))) any = true;
        if (!any || w.zombies == 0) return;
        City c = w.city;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            // (Not those holding out in a survivor base or sheltering in a safe zone: they're where they mean to be.)
            if (e.dead || e.type != Entity.CIVILIAN || e.ride != null || e.inmate || e.base != null || e.task == Dispatch.T_SHELTER) continue;
            int t = c.tileIndex(e.x, e.y);
            if (t < 0 || !shore.get(t)) continue;
            if (taken(e) || w.countZombiesNear(e.x, e.y, 90) == 0) continue;
            int[] s = shoreNear(e.x, e.y, 3, true);
            if (s == null) continue;
            Fleet.Vehicle best = null;
            float bd = 3200;
            for (int k = 0; k < boats.size(); k++) {
                Fleet.Vehicle b = boats.get(k);
                float d = (float) World.hypot(b.x - e.x, b.y - e.y);
                if (free(b) && d < bd) {
                    bd = d;
                    best = b;
                }
            }
            if (best == null) return;
            float[] d = dock(s);
            if (!steerFor(best, s[0], d[0], d[1])) continue;
            best.boatJob = J_RESCUE;
            best.jobT = 0;
            best.waitFor = e;
            best.landX = tx(s[1]);
            best.landY = ty(s[1]);
            best.place = c.placeName(e.x, e.y);
            if (w.time - strandSaidAt > 20) {
                strandSaidAt = w.time;
                w.dispatch.say(Dispatch.WHO_POLICE, null, "Marine " + best.boatNum + ": People trapped at the water's edge on " + best.place + ". Going in for them.", e.x, e.y);
            }
            any = false;
            for (int k = 0; k < boats.size(); k++) if (free(boats.get(k))) any = true;
            if (!any) return;
        }
    }

    private boolean taken(Entity e) {
        for (int k = 0; k < boats.size(); k++) {
            Fleet.Vehicle b = boats.get(k);
            if (b.waitFor == e && (b.boatJob == J_RESCUE || b.boatJob == J_LOAD)) return true;
            if (b.landX > 0 && (b.boatJob == J_RESCUE || b.boatJob == J_LOAD) && Math.abs(b.landX - e.x) < 60 && Math.abs(b.landY - e.y) < 60) return true;
        }
        return false;
    }

    /** Alongside the shore: everyone close by who isn't police or army climbs aboard, then away to safety. */
    private void takeAboard(Fleet.Vehicle v, float dt) {
        v.speed = 0;
        v.checkCd -= dt;
        if (v.checkCd > 0) return;
        v.checkCd = 0.25f;
        float lx = v.landX, ly = v.landY;
        for (int k = 0, m = w.gatherLiving(lx, ly, 120); k < m && v.riders.size() < BOAT_SEATS; k++) {
            Entity e = w.gathered[k];
            if (e.dead || e.removed || e.type != Entity.CIVILIAN || e.inmate || e.base != null || e.task == Dispatch.T_SHELTER) continue;
            float d = (float) World.hypot(e.x - v.x, e.y - v.y);
            if (d < 30) {
                // In over the side.
                e.dead = true;
                e.removed = true;
                e.ride = null;
                e.goneAt = w.time;
                v.riders.add(e);
            } else if (d < 120 && e.ride == null && shore.get(w.city.tileIndex(e.x, e.y))) {
                // Waved over: they run for it.
                e.ride = v;
                e.task = Dispatch.T_RIDE;
            }
        }
        boolean danger = w.countZombiesNear(lx, ly, 30) >= 3;
        if (v.riders.size() >= BOAT_SEATS || v.jobT > 9 || danger || (v.jobT > 4 && !v.riders.isEmpty() && nobodyComing(v))) {
            for (int k = 0, m = w.gatherLiving(lx, ly, 160); k < m; k++) if (w.gathered[k].ride == v) w.gathered[k].ride = null;
            v.waitFor = null;
            if (v.riders.isEmpty()) {
                patrol(v);
                return;
            }
            toSafety(v);
        }
    }

    private boolean nobodyComing(Fleet.Vehicle v) {
        for (int k = 0, m = w.gatherLiving(v.x, v.y, 120); k < m; k++) if (w.gathered[k].ride == v && !w.gathered[k].dead) return false;
        return true;
    }

    /** Off to a safe bit of shore with those aboard: a mooring with none of the dead near, or anywhere quiet along the water. */
    private void toSafety(Fleet.Vehicle v) {
        int[] best = null;
        float bd = Float.MAX_VALUE;
        for (int k = 0; k < boats.size(); k++) {
            Fleet.Vehicle b = boats.get(k);
            float d = (float) World.hypot(b.homeLandX - v.x, b.homeLandY - v.y);
            if (d < 200 || d >= bd || w.countZombiesNear(b.homeLandX, b.homeLandY, 220) > 0) continue;
            int[] s = shoreNear(b.homeLandX, b.homeLandY, 2, true);
            if (s != null) {
                bd = d;
                best = s;
            }
        }
        if (best == null)
            for (int k = 0; k < 24; k++) {
                float px = v.x + (w.rnd.nextFloat() - 0.5f) * 3000, py = v.y + (w.rnd.nextFloat() - 0.5f) * 3000;
                if (px < 0 || py < 0 || px >= w.city.worldW() || py >= w.city.worldH()) continue;
                int[] s = shoreNear(px, py, 6, true);
                if (s == null) continue;
                float d = (float) World.hypot(tx(s[1]) - v.x, ty(s[1]) - v.y);
                if (d < 300 || d >= bd || w.countZombiesNear(tx(s[1]), ty(s[1]), 260) > 0) continue;
                bd = d;
                best = s;
            }
        float[] dk = best != null ? dock(best) : null;
        if (best == null || !steerFor(v, best[0], dk[0], dk[1])) {
            // Nowhere to put them: back out on the water, and try again in a moment.
            v.boatJob = J_IDLE;
            v.jobT = 0;
            return;
        }
        v.boatJob = J_DROP;
        v.jobT = 0;
        v.landX = tx(best[1]);
        v.landY = ty(best[1]);
        v.place = w.city.placeName(v.landX, v.landY);
        w.dispatch.say(Dispatch.WHO_POLICE, null, "Marine " + v.boatNum + ": " + World.count(v.riders.size(), "survivor", "survivors")
                + " aboard. Taking them round to " + v.place + ".", v.x, v.y);
    }

    /** At the landing: everyone aboard goes ashore - the rescued safe, officers off to their call. */
    private void ashore(Fleet.Vehicle v) {
        int people = 0, officers = 0;
        for (int i = 0; i < v.riders.size(); i++) {
            Entity e = v.riders.get(i);
            if (!w.release(e, v.landX + (i % 3 - 1) * 5, v.landY + (i / 3) * 4)) continue;
            e.fleeTimer = 0;
            e.fear = 0;
            people++;
        }
        v.riders.clear();
        Dispatch.Incident inc = v.pickupFor;
        for (int i = 0; i < v.crew.size(); i++) {
            Entity e = v.crew.get(i);
            if (!w.release(e, v.landX + (i % 2 == 0 ? -5 : 5), v.landY + (i / 2) * 5)) continue;
            e.rig = null;
            e.rideCd = w.time + 20;
            if (inc != null && inc.ride) {
                // A lift (not a call): back to what they were doing.
                e.rideFrom = null;
                e.incident = null;
                e.task = inc.rideTask;
                e.zone = inc.rideZone;
                if (e.task == Dispatch.T_POST || e.task == Dispatch.T_CORDON) {
                    e.postX = inc.rideX;
                    e.postY = inc.rideY;
                }
                if (e.task == Dispatch.T_GUARD && (e.zone == null || e.zone.removed)) e.task = Dispatch.T_NONE;
                if (e.task == Dispatch.T_CORDON && w.zoneOfPost(e.postX, e.postY) == null) e.task = Dispatch.T_NONE;
            } else if (inc != null && !inc.resolved) {
                e.task = Dispatch.T_RESPOND;
                e.incident = inc;
                e.onScene = false;
            }
            officers++;
        }
        v.crew.clear();
        rescued += people;
        ferried += officers;
        if (people > 0)
            w.dispatch.say(Dispatch.WHO_POLICE, null, "Marine " + v.boatNum + ": " + World.count(people, "survivor", "survivors") + " put ashore safe at " + v.place + ".", v.x, v.y);
        if (officers > 0)
            w.dispatch.say(Dispatch.WHO_POLICE, null, "Marine " + v.boatNum + ": " + World.count(officers, "officer", "officers") + " ashore at " + v.place
                    + (inc != null && !inc.ride && !inc.resolved ? ", on their way to " + inc.place + "." : "."), v.x, v.y);
        v.pickupFor = null;
        v.waiting = null;
        v.boatJob = J_IDLE;
        v.jobT = 0;
    }

    /**
     * (From World.rideCheck.) Officers with a long way to go to a call over the water, with no road there (or only a long
     * way round): a police boat comes in to the shore near them, takes them across and puts them ashore near it. False
     * if there's no boat free, or no water in the way.
     */
    boolean boatLift(java.util.List<Entity> group, Dispatch.Incident inc) {
        if (boats.isEmpty() || group.isEmpty() || inc == null) return false;
        Entity a = group.get(0);
        float d = (float) World.hypot(inc.x - a.x, inc.y - a.y);
        if (d < 400) return false;
        City c = w.city;
        int n = (int) (d / City.T), wetN = 0;
        for (int k = 1; k < n; k++) {
            int t = c.tileIndex(a.x + (inc.x - a.x) * k / n, a.y + (inc.y - a.y) * k / n);
            if (t >= 0 && c.tiles[t] == City.WATER) wetN++;
        }
        if (wetN < 3) return false;
        boolean across = wetN >= n * 0.35f && d >= 600;
        if (!across && w.fleet.canDrive(a.x, a.y, inc.x, inc.y)) return false;
        int[] up = shoreNear(a.x, a.y, 20, true), down = shoreNear(inc.x, inc.y, 24, true);
        if (up == null || down == null || World.hypot(tx(up[1]) - tx(down[1]), ty(up[1]) - ty(down[1])) < 200) return false;
        Fleet.Vehicle best = null;
        float bd = 4000;
        for (int k = 0; k < boats.size(); k++) {
            Fleet.Vehicle b = boats.get(k);
            float bdist = (float) World.hypot(b.x - tx(up[0]), b.y - ty(up[0]));
            if (free(b) && bdist < bd) {
                bd = bdist;
                best = b;
            }
        }
        if (best == null) return false;
        float[] dk = dock(up);
        if (!steerFor(best, up[0], dk[0], dk[1])) return false;
        best.boatJob = J_FERRY;
        best.jobT = 0;
        best.pickupFor = inc;
        best.landX = tx(up[1]);
        best.landY = ty(up[1]);
        best.place = c.placeName(best.landX, best.landY);
        best.waiting = new ArrayList<Entity>();
        best.boatPath2 = down;
        for (Entity e : group) {
            if (best.waiting.size() >= BOAT_SEATS) break;
            e.task = Dispatch.T_BOARD;
            e.rig = best;
            w.fleet.note(e);
            best.waiting.add(e);
        }
        w.dispatch.say(Dispatch.WHO_POLICE, null, "Marine " + best.boatNum + ": Picking up " + World.count(best.waiting.size(), "officer", "officers") + " at " + best.place
                + " to take them across the water to " + inc.place + ".", best.landX, best.landY);
        return true;
    }

    /** Alongside for the officers: they climb in (or it gives up waiting), then across to the call. */
    private void officersAboard(Fleet.Vehicle v) {
        v.speed = 0;
        ArrayList<Entity> wait = v.waiting;
        int coming = 0;
        if (wait != null)
            for (int i = wait.size() - 1; i >= 0; i--) {
                Entity e = wait.get(i);
                if (e.dead && v.crew.contains(e)) {
                    wait.remove(i);
                    continue;
                }
                if (e.dead || e.removed || e.rig != v || e.task != Dispatch.T_BOARD) {
                    wait.remove(i);
                    continue;
                }
                if (World.hypot(e.x - v.x, e.y - v.y) < 34) {
                    w.fleet.board(v, e);
                    wait.remove(i);
                } else coming++;
            }
        Dispatch.Incident inc = v.pickupFor;
        boolean over = inc == null || (!inc.ride && inc.resolved);
        if ((coming == 0 && !v.crew.isEmpty()) || v.jobT > 30 || over) {
            if (wait != null)
                for (Entity e : wait)
                    if (e.rig == v) {
                        e.rig = null;
                        if (e.task == Dispatch.T_BOARD) e.task = Dispatch.T_NONE;
                    }
            v.waiting = null;
            int[] down = v.boatPath2;
            if (v.crew.isEmpty() || down == null) {
                if (!v.crew.isEmpty()) ashore(v);
                patrol(v);
                return;
            }
            float[] dk = dock(down);
            if (!steerFor(v, down[0], dk[0], dk[1])) {
                ashore(v);
                patrol(v);
                return;
            }
            v.boatJob = J_DROP;
            v.jobT = 0;
            v.landX = tx(down[1]);
            v.landY = ty(down[1]);
            v.place = w.city.placeName(v.landX, v.landY);
        }
    }

    /** ((26.0) The Place tool's police boat: out on the water nearest (x, y), or null if there's none. */
    Fleet.Vehicle placeBoat(float x, float y) {
        if (water == null) {
            City c = w.city;
            int[] s = shoreNear(x, y, 30, false);
            if (s == null) return null;
            water = new BitSet(c.w * c.h);
            shore = new BitSet(c.w * c.h);
            if (flood(s[0]) < 400) {
                water = shore = null;
                return null;
            }
            for (int t = water.nextSetBit(0); t >= 0; t = water.nextSetBit(t + 1)) {
                int tx0 = t % c.w, ty0 = t / c.w;
                for (int dy = -2; dy <= 2; dy++)
                    for (int dx = -2; dx <= 2; dx++) {
                        int xx = tx0 + dx, yy = ty0 + dy;
                        if (xx >= 0 && yy >= 0 && xx < c.w && yy < c.h && land(yy * c.w + xx)) shore.set(yy * c.w + xx);
                    }
            }
        }
        int[] s = shoreNear(x, y, 30, true);
        if (s == null) return null;
        float[] d = dock(s);
        Fleet.Vehicle v = w.fleet.policeBoat(d[0], d[1], tx(s[1]), ty(s[1]), boats.size() + 1);
        boats.add(v);
        return v;
    }

    /** ((26.0) Where those aboard a police boat would come ashore (for a save). */
    static float[] ashoreAt(Fleet.Vehicle v) {
        return new float[]{v.homeLandX, v.homeLandY};
    }

    // ------------------------------------------------------------------ the mobile command post

    /** The command post truck (one to a city), whether it's set up, and its tent. */
    Fleet.Vehicle post;
    boolean postUp;
    float postX, postY;
    /** Until another may set out; times it's been set up and overrun. */
    float postCd = 20;
    int postsSetUp, postsLost, resupplied;
    private float postHitSaidAt = -99, postFarFor, supplySaidAt = -99;
    static final float POST_BOOST = 200, POST_COORD = 400, POST_SUPPLY = 90;

    /** A police officer close enough to the command post set up to be steadied by it (a better shot, slower to break). */
    boolean boosts(Entity e) {
        if (!postUp || e.type != Entity.COP) return false;
        float dx = e.x - postX, dy = e.y - postY;
        return dx * dx + dy * dy < POST_BOOST * POST_BOOST;
    }

    /** A call close enough to the command post for it to run: units sent there first, and one more of them. */
    boolean coordinates(Dispatch.Incident inc) {
        // (Real calls only: not a post on an infection zone's cordon, nor a lift.)
        if (!postUp || inc.cordonPost >= 0 || inc.ride) return false;
        float dx = inc.x - postX, dy = inc.y - postY;
        return dx * dx + dy * dy < POST_COORD * POST_COORD;
    }

    int priority(Dispatch.Incident inc) {
        return coordinates(inc) ? 8 : 0;
    }

    /** The worst call going (the most of the dead at it), or null. */
    private Dispatch.Incident worst() {
        Dispatch.Incident best = null;
        for (int i = 0; i < w.dispatch.incidents.size(); i++) {
            Dispatch.Incident inc = w.dispatch.incidents.get(i);
            if (inc.resolved || inc.ride || inc.cordonPost >= 0) continue;
            if (best == null || inc.zombiesNear > best.zombiesNear) best = inc;
        }
        return best;
    }

    /** A spot on the road a little back from (x, y) towards (fx, fy), clear of the dead, for the command post. */
    private float[] postSpot(float x, float y, float fx, float fy) {
        City c = w.city;
        float d = (float) World.hypot(fx - x, fy - y) + 0.001f;
        float ux = (fx - x) / d, uy = (fy - y) / d;
        for (float back : new float[]{240, 320, 180, 400, 480}) {
            if (back > d) continue;
            float[] q = c.nearestDrivable(x + ux * back, y + uy * back);
            if (q == null || c.junctionIdAt(q[0], q[1]) >= 0) continue;
            if (w.countZombiesNear(q[0], q[1], 130) <= 1) return q;
        }
        return c.nearestDrivable(x + ux * Math.min(d, 300), y + uy * Math.min(d, 300));
    }

    /** Once a second: the command post sent out in an outbreak, set up, hit, resupplying, moving on - or lost. */
    private void postTick() {
        Fleet.Vehicle v = post;
        if (v != null && v.removedFromFleet) {
            post = null;
            postUp = false;
            return;
        }
        if (v != null && v.broken) {
            lose();
            return;
        }
        if (v == null) {
            if ((postCd -= 1) > 0) return;
            postCd = 10;
            if (!w.outbreak || w.alert < 1 || w.city.cfg.mission >= 0) return;
            Dispatch.Incident bad = worst();
            if (bad == null || bad.zombiesNear < 6) return;
            City.Facility st = null;
            float bd = 5000;
            for (City.Facility f : w.city.facilities) {
                if (f.kind != City.FACILITY_POLICE || f.fallen) continue;
                float d = (float) World.hypot(f.x - bad.x, f.y - bad.y);
                if (d < bd && w.countZombiesNear(f.gateX, f.gateY, 100) == 0) {
                    bd = d;
                    st = f;
                }
            }
            if (st == null) return;
            float[] spot = postSpot(bad.x, bad.y, st.gateX, st.gateY);
            if (spot == null) return;
            java.util.List<Entity> crew = w.fleet.standby(st, Entity.COP, 0, 2);
            ArrayList<Entity> aboard = new ArrayList<Entity>(crew);
            while (aboard.size() < 2) aboard.add(w.create(Entity.COP, st.gateX, st.gateY));
            Fleet.Vehicle cp = w.fleet.commandPost(st.gateX, st.gateY, spot[0], spot[1], aboard);
            if (cp == null) {
                postCd = 40;
                return;
            }
            post = cp;
            postUp = false;
            w.dispatch.say(Dispatch.WHO_POLICE, null, "Police Command: The mobile command post is on its way from " + st.name + " to set up near "
                    + bad.place + ".", st.gateX, st.gateY, true);
            return;
        }
        if (!postUp) return;
        // The dead at the tent or the truck: they tear into it.
        int n = w.countZombiesNear(postX, postY, 34) + w.countZombiesNear(v.x, v.y, 22);
        if (n > 0) {
            w.fleet.damage(v, n * 7, false);
            if (w.time - postHitSaidAt > 30) {
                postHitSaidAt = w.time;
                w.dispatch.say(Dispatch.WHO_POLICE, null, "Command post: The dead are on us at " + v.place + "! All units nearby, to the command post!", postX, postY, true);
                w.dispatch.sighting(postX, postY, null);
            }
            if (v.broken) {
                lose();
                return;
            }
        } else if (v.hp < v.maxHp && w.countZombiesNear(postX, postY, 140) == 0) v.hp = Math.min(v.maxHp, v.hp + 3);
        // Ammunition for the officers who come by.
        int got = 0;
        for (int k = 0, m = w.gatherArmed(postX, postY, POST_SUPPLY); k < m; k++) {
            Entity e = w.gathered[k];
            if (e.dead || e.type != Entity.COP || e.magSize <= 0) continue;
            if (World.hypot(e.x - postX, e.y - postY) > POST_SUPPLY) continue;
            int full = e.magSize * 5;
            if (e.reserve >= full) continue;
            e.reserve = Math.min(full, e.reserve + e.magSize * 2);
            e.outOfAmmoSaid = false;
            e.scout = false;
            if (e.task == Dispatch.T_RESUPPLY) e.task = Dispatch.T_NONE;
            got++;
        }
        if (got > 0) {
            resupplied += got;
            if (w.time - supplySaidAt > 60) {
                supplySaidAt = w.time;
                w.dispatch.say(Dispatch.WHO_POLICE, null, "Command post: Handing out magazines at " + v.place + ". " + World.count(got, "officer", "officers") + " topped up.", postX, postY);
            }
        }
        // The fighting's moved off a long way: it packs up and follows it.
        Dispatch.Incident bad = worst();
        boolean near = false;
        for (int i = 0; i < w.dispatch.incidents.size() && !near; i++) {
            Dispatch.Incident inc = w.dispatch.incidents.get(i);
            if (!inc.resolved && inc.zombiesNear > 0 && World.hypot(inc.x - postX, inc.y - postY) < 650) near = true;
        }
        postFarFor = near ? 0 : postFarFor + 1;
        if (postFarFor > 75 && bad != null && bad.zombiesNear >= 8 && World.hypot(bad.x - postX, bad.y - postY) > 800) {
            float[] spot = postSpot(bad.x, bad.y, postX, postY);
            if (spot != null && w.fleet.movePost(v, spot[0], spot[1])) {
                postUp = false;
                postFarFor = 0;
                for (int i = 0, m = w.entities.size(); i < m; i++) {
                    Entity e = w.entities.get(i);
                    if (!e.dead && e.type == Entity.COP && e.task == Dispatch.T_POST && World.hypot(e.postX - postX, e.postY - postY) < 30) e.task = Dispatch.T_NONE;
                }
                w.dispatch.say(Dispatch.WHO_POLICE, null, "Police Command: Command post packing up and moving to " + bad.place + ".", postX, postY);
            } else postFarFor = 40;
        }
    }

    /** (From Fleet.) The command post has got where it was going: it sets up - the tent out, its officers on watch by it. */
    void postArrived(Fleet.Vehicle v) {
        if (v != post && post != null && !post.removedFromFleet) return;
        post = v;
        postUp = true;
        postFarFor = 0;
        // The tent: beside the truck where there's room for all of it (not into a building), else behind or in front.
        float ca = (float) Math.cos(v.angle), sa = (float) Math.sin(v.angle);
        float[][] tries = {{-sa * 17, ca * 17}, {sa * 17, -ca * 17}, {-ca * 26, -sa * 26}, {ca * 26, sa * 26}};
        float[] q = null;
        for (int k = 0; k < tries.length && q == null; k++) {
            float px = v.x + tries[k][0], py = v.y + tries[k][1];
            boolean clear = true;
            for (int cx = -1; cx <= 1 && clear; cx++)
                for (int cy = -1; cy <= 1 && clear; cy++)
                    if (w.city.solidAt(px + ca * 13 * cx - sa * 9 * cy, py + sa * 13 * cx + ca * 9 * cy)) clear = false;
            if (clear) q = new float[]{px, py};
        }
        if (q == null) q = new float[]{v.x - ca * 26, v.y - sa * 26};
        postX = q[0];
        postY = q[1];
        v.place = w.city.placeName(v.x, v.y);
        ArrayList<Entity> crew = new ArrayList<Entity>(v.crew);
        w.fleet.crewOut(v, true);
        for (int i = 0; i < crew.size(); i++) {
            Entity e = crew.get(i);
            if (e.removed) continue;
            e.rig = null;
            e.task = Dispatch.T_POST;
            float[] p = w.city.findWalkable(postX + (i == 0 ? -12 : 12), postY + 8);
            e.postX = p != null ? p[0] : postX;
            e.postY = p != null ? p[1] : postY;
        }
        postsSetUp++;
        w.dispatch.say(Dispatch.WHO_POLICE, null, "Police Command: Mobile command post is set up on " + v.place
                + ". Units in the area, check in for orders and ammo.", postX, postY, true);
        w.timeline("The police set up a mobile command post on " + v.place, postX, postY);
    }

    /** The command post is overrun: lost, and the radio says so. */
    private void lose() {
        Fleet.Vehicle v = post;
        String where = v != null && v.place != null ? v.place : w.city.placeName(postX, postY);
        if (v != null && !v.broken) w.fleet.damage(v, v.maxHp * 2, false);
        for (int i = 0, m = w.entities.size(); i < m; i++) {
            Entity e = w.entities.get(i);
            if (!e.dead && e.type == Entity.COP && e.task == Dispatch.T_POST && World.hypot(e.postX - postX, e.postY - postY) < 30) e.task = Dispatch.T_NONE;
        }
        post = null;
        postUp = false;
        postCd = 180;
        postsLost++;
        w.dispatch.say(Dispatch.WHO_POLICE, null, "Police Command: We've lost the command post on " + where + ". It's overrun. All units, you're on your own out there.", postX, postY, true);
        w.timeline("The police command post on " + where + " was overrun", postX, postY);
    }

    /** ((26.0) The Place tool's command post: set up where it's put (or, with one out already, it moves there). */
    Fleet.Vehicle placePost(float x, float y) {
        float[] q = w.city.nearestDrivable(x, y);
        if (q == null) return null;
        if (post != null && !post.removedFromFleet && !post.broken) {
            if (!w.fleet.movePost(post, q[0], q[1])) return null;
            postUp = false;
            w.dispatch.say(Dispatch.WHO_POLICE, null, "Police Command: Command post packing up and moving to " + w.city.placeName(q[0], q[1]) + ".", q[0], q[1]);
            return post;
        }
        ArrayList<Entity> crew = new ArrayList<Entity>();
        for (int k = 0; k < 2; k++) crew.add(w.create(Entity.COP, q[0], q[1]));
        Fleet.Vehicle v = w.fleet.commandPost(q[0], q[1], q[0], q[1], crew);
        if (v == null) return null;
        post = v;
        postArrived(v);
        return v;
    }

    /** ((26.0) A command post set up in a saved game, put back. */
    void restorePost(float x, float y, float angle, float tentX, float tentY, float hp) {
        Fleet.Vehicle v = w.fleet.commandPost(x, y, x, y, null);
        if (v == null) return;
        v.angle = angle;
        v.hp = Math.max(1, Math.min(v.maxHp, hp));
        v.place = w.city.placeName(x, y);
        post = v;
        postUp = true;
        postX = tentX;
        postY = tentY;
    }

    // ------------------------------------------------------------------ special forces

    /** Teams flown in so far (by the army on its own, and put down by hand), and until the army sends another. */
    int sfCalls;
    private float sfCd = 60;
    private float giantSince = -1;

    /**
     * Every five seconds in an outbreak the army looks for a job for its special forces - a giant loose a while, a safe
     * zone falling to the dead, or a huge horde at a call - and with none of them already out, flies a team in.
     */
    private void sfCheck() {
        if (w.counts[Entity.GIANT] > 0) {
            if (giantSince < 0) giantSince = w.time;
        } else giantSince = -1;
        if ((sfCd -= 5) > 0 || !w.outbreak || w.alert < 1 || w.city.cfg.mission >= 0) return;
        int out = 0;
        Entity giant = null;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.dead) continue;
            if (e.role == Entity.ROLE_SF && e.type == Entity.SOLDIER) out++;
            else if (e.type == Entity.GIANT && giant == null) giant = e;
        }
        if (out >= 2) return;
        if (giant != null && giantSince >= 0 && w.time - giantSince > 15) {
            callIn(giant.x, giant.y, "a giant on the loose", false);
            return;
        }
        for (int i = 0; i < w.dispatch.zones.size(); i++) {
            Dispatch.SafeZone z = w.dispatch.zones.get(i);
            if (!z.removed && z.open && z.fallingBack) {
                callIn(z.x, z.y, "the " + z.place + " safe zone is falling", false);
                return;
            }
        }
        Dispatch.Incident bad = worst();
        if (bad != null && bad.zombiesNear >= 45) callIn(bad.x, bad.y, "a huge horde", false);
    }

    /**
     * Flies a special forces team in to (x, y): four in black with suppressed rifles, down the ropes from a helicopter
     * in from the nearest edge of the map. The team (still aboard), or an empty list if there's no room.
     */
    ArrayList<Entity> callIn(float x, float y, String why, boolean placed) {
        float ww = w.city.worldW(), hh = w.city.worldH();
        float dl = x, dr = ww - x, dt = y, db = hh - y, m = Math.min(Math.min(dl, dr), Math.min(dt, db));
        float ex = m == dl ? City.T : m == dr ? ww - City.T : x, ey = m == dt ? City.T : m == db ? hh - City.T : y;
        String place = w.city.placeName(x, y);
        Dispatch.Incident inc = null;
        for (int i = 0; i < w.dispatch.incidents.size(); i++) {
            Dispatch.Incident o = w.dispatch.incidents.get(i);
            if (!o.resolved && !o.ride && World.hypot(o.x - x, o.y - y) < 300) inc = o;
        }
        ArrayList<Entity> team = w.fleet.airlift(4, ex, ey, x, y, place, inc);
        if (team == null) return new ArrayList<Entity>();
        for (int i = 0; i < team.size(); i++) {
            Entity e = team.get(i);
            w.applyRole(e, Entity.ROLE_SF);
            e.member = i;
            e.level = Math.max(e.level, 4);
            if (!placed) e.outsider = true;
        }
        sfCalls++;
        sfCd = 240;
        w.dispatch.say(Dispatch.WHO_MILITARY, null, placed ? "Military: Special forces team inbound by helicopter to " + place + "."
                : "Military: Sending in special forces - " + why + " at " + place + ". Viper team inbound by helicopter.", x, y, true);
        if (!placed) w.timeline("Special forces were flown in: " + why, x, y);
        return team;
    }

    // ------------------------------------------------------------------ the cards

    /** A police boat's or the command post's line on the map and its card. */
    String status(Fleet.Vehicle v) {
        if (v.kind == Fleet.K_COMMAND) {
            if (v.broken) return "Command post: Overrun";
            if (v.state != Fleet.SCENE) return "Command post: On the way to set up" + (v.place != null ? " near " + v.place : "");
            if (v != post || !postUp) return "Command post: Packed up";
            int calls = 0;
            for (int i = 0; i < w.dispatch.incidents.size(); i++) if (!w.dispatch.incidents.get(i).resolved && coordinates(w.dispatch.incidents.get(i))) calls++;
            return "Command post: Set up on " + v.place + "  -  " + (int) (100 * v.hp / v.maxHp) + "%  -  running " + World.count(calls, "call", "calls")
                    + (resupplied > 0 ? ", " + resupplied + " resupplied" : "");
        }
        String who = "Marine " + v.boatNum;
        int aboard = 2 + v.crew.size();
        String crew = "  (" + aboard + " officers aboard" + (v.riders.isEmpty() ? ")" : ", " + v.riders.size() + " rescued)");
        switch (v.boatJob) {
            case J_RESCUE: return who + ": Going in for people at the water's edge" + (v.place != null ? " on " + v.place : "") + crew;
            case J_LOAD: return who + ": Taking people aboard (" + v.riders.size() + ")" + crew;
            case J_DROP: return who + ": " + (v.riders.isEmpty() ? "Taking officers across to " : "Taking them to safety at ") + v.place + crew;
            case J_FERRY: return who + ": Picking up officers at " + v.place + crew;
            case J_BOARD: return who + ": Officers boarding (" + v.crew.size() + "/" + (v.crew.size() + (v.waiting == null ? 0 : v.waiting.size())) + ")" + crew;
            case J_IDLE: return who + ": Holding on the water" + crew;
            default: return who + ": Patrolling the water" + crew;
        }
    }
}
