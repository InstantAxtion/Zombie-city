package com.instantaxtion.zombiesandbox;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A health check of a real game (tools/health.sh): two minutes of play on Metropolis and the Archipelago at the
 * Default size, the game's own map version, a small outbreak started ten seconds in, the dice fixed. It counts what
 * players notice going wrong - vehicles stuck, driven into each other or spinning on the spot, police standing about
 * far from any station, police numbers running away, slow responses, a slow simulation - and writes the numbers to a
 * text file. With a baseline it marks each OK, or WARN where it's got much worse.
 *
 *   Health run OUT_FILE            plays both maps, writes the numbers
 *   Health compare NOW BASELINE    prints OK / WARN for each number
 */
final class Health {
    static final float DT = 1 / 30f;
    static final int SECONDS = 120, OUTBREAK_AT = 10;

    public static void main(String[] args) throws Exception {
        if (args.length >= 3 && args[0].equals("compare")) {
            System.exit(compare(read(new File(args[1])), read(new File(args[2]))));
        }
        String out = args.length >= 2 ? args[1] : "health.txt";
        Map<String, String> all = new LinkedHashMap<String, String>();
        String only = System.getenv("HEALTH_MAP");
        if (only == null || only.equals("metropolis")) all.putAll(play("metropolis", CityConfig.METROPOLIS));
        if (only == null || only.equals("archipelago")) all.putAll(play("archipelago", CityConfig.ARCHIPELAGO));
        PrintWriter pw = new PrintWriter(new FileWriter(out));
        for (Map.Entry<String, String> e : all.entrySet()) pw.println(e.getKey() + " " + e.getValue());
        pw.close();
        for (Map.Entry<String, String> e : all.entrySet()) System.out.println(e.getKey() + " " + e.getValue());
    }

    /** Two minutes of one map. */
    static Map<String, String> play(String name, int preset) {
        long wall = System.currentTimeMillis();
        World.testSeed = 2800 + preset;
        CityConfig c = new CityConfig();
        c.v[CityConfig.OPT_PRESET] = preset;
        c.v[CityConfig.OPT_SIZE] = CityConfig.GIGANTIC; // the Default size
        c.v[CityConfig.OPT_ZOMBIES] = 0;
        c.seed = 2800;
        c.normalize(); // (as the New Game screen does: the Archipelago comes at its one size)
        c.seed = 2800;
        World w = new World(c);
        w.populate(c);
        // A phone's view of the middle of town: what's on it is worked out in full, the rest the lighter way, as in a game.
        float cx = w.city.worldW() / 2f, cy = w.city.worldH() / 2f;
        w.viewX0 = cx - 450;
        w.viewY0 = cy - 250;
        w.viewX1 = cx + 450;
        w.viewY1 = cy + 250;

        int[] police = new int[3];
        police[0] = policeNow(w);
        int stuck = 0, spins = 0, idleMax = 0, idleSum = 0, idleSamples = 0, trips = 0;
        float overlapSecs = 0, tripSecs = 0;
        long simNanos = 0;
        Map<Fleet.Vehicle, float[]> at = new IdentityHashMap<Fleet.Vehicle, float[]>();
        Map<Fleet.Vehicle, float[]> turn = new IdentityHashMap<Fleet.Vehicle, float[]>();
        Map<Fleet.Vehicle, float[]> trip = new IdentityHashMap<Fleet.Vehicle, float[]>();
        final int N = 80; // samples of a spin's history: one every 3 steps, 8 s
        java.util.Random r = new java.util.Random(7);
        for (int f = 0; f < SECONDS * 30; f++) {
            if (f == OUTBREAK_AT * 30) {
                // A small outbreak in town: three packs of eight of the dead, one by the middle of the view.
                for (int pack = 0; pack < 3; pack++) {
                    float[] p = null;
                    for (int k = 0; k < 200 && p == null; k++) {
                        float[] q = w.city.randomWalkableInTown(r);
                        if (q != null && Math.hypot(q[0] - cx, q[1] - cy) < (pack == 0 ? 300 : w.city.worldW() / 4f)) p = q;
                    }
                    if (p == null) p = w.city.randomWalkableInTown(r);
                    for (int k = 0; k < 8; k++) {
                        float[] q = w.city.findWalkable(p[0] + (k % 3) * 8, p[1] + (k / 3) * 8);
                        if (q != null) w.spawn(Entity.ZOMBIE, q[0], q[1]);
                    }
                }
                w.recount();
            }
            long t0 = System.nanoTime();
            w.update(DT);
            simNanos += System.nanoTime() - t0;
            w.evCount = 0;
            if (f % 600 == 599 && System.getenv("HEALTH_DEBUG") != null)
                System.err.println(name + " t=" + (int) w.time + " zombies " + w.zombies + " humans " + w.humans + " outbreak " + w.outbreak + " alert " + w.alert
                        + " calls " + w.dispatch.incidents.size() + " killed " + w.zombiesKilled);
            if (f == 60 * 30 - 1) police[1] = policeNow(w);
            if (f == 120 * 30 - 1) police[2] = policeNow(w);
            List<Fleet.Vehicle> road = new ArrayList<Fleet.Vehicle>();
            for (int i = 0; i < w.fleet.vehicles.size(); i++) {
                Fleet.Vehicle v = w.fleet.vehicles.get(i);
                if (v.removedFromFleet || v.player != null || Fleet.offRoad(v) || Fleet.airborne(v)) continue;
                // Stuck: on its way somewhere, and not 40 further on in 30 seconds.
                boolean trying = !v.parked && !v.broken && (v.state == Fleet.DRIVE || v.state == Fleet.RETURN || v.state == Fleet.PATROL
                        || (v.type == Fleet.CAR && v.state == Fleet.WAIT));
                float[] a = at.get(v);
                if (a == null || !trying || Math.hypot(v.x - a[0], v.y - a[1]) > 40) at.put(v, a = new float[]{v.x, v.y, w.time, 0});
                if (trying && a[3] == 0 && w.time - a[2] > 30) {
                    a[3] = 1;
                    stuck++;
                }
                // A patrol car's trip to a call: from being sent to being there.
                if (v.type == Fleet.CRUISER && v.patrol) {
                    float[] tr = trip.get(v);
                    if (v.state == Fleet.DRIVE && v.incident != null) {
                        if (tr == null) trip.put(v, new float[]{w.time, System.identityHashCode(v.incident)});
                        else if (tr[1] != System.identityHashCode(v.incident)) tr[0] = w.time; // sent on to another
                    } else if (tr != null) {
                        if (v.state == Fleet.SCENE) {
                            trips++;
                            tripSecs += w.time - tr[0];
                        }
                        trip.remove(v);
                    }
                }
                int t = w.city.tileIndex(v.x, v.y);
                if (t >= 0 && (w.city.tiles[t] == City.ROAD || w.city.tiles[t] == City.CAR) && !v.parked) road.add(v);
                if (f % 3 == 0) spins += spin(turn, v, N);
            }
            // Bodies driven into each other: both on the road, overlapping by more than 30% of the smaller.
            if (f % 3 == 0) {
                for (int i = 0; i < road.size(); i++)
                    for (int j = i + 1; j < road.size(); j++) {
                        Fleet.Vehicle a = road.get(i), b = road.get(j);
                        float reach = a.length() + b.length();
                        if (Math.abs(a.x - b.x) > reach || Math.abs(a.y - b.y) > reach) continue;
                        if (overlap(a, b) > 0.3f) overlapSecs += 3 * DT;
                    }
            }
            // Police on foot far from any station with nothing to do.
            if (f % 150 == 149) {
                int idle = 0;
                for (Entity e : w.entities)
                    if (!e.dead && e.type == Entity.COP && e.agency != 3 && e.ride == null && e.task == Dispatch.T_NONE && stationDist(w, e) > 300) idle++;
                idleMax = Math.max(idleMax, idle);
                idleSum += idle;
                idleSamples++;
            }
        }
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put(name + ".vehicles_stuck_30s", "" + stuck);
        m.put(name + ".overlap_pair_seconds", String.format(java.util.Locale.ROOT, "%.1f", overlapSecs));
        m.put(name + ".spins", "" + spins);
        m.put(name + ".idle_police_far_max", "" + idleMax);
        m.put(name + ".idle_police_far_mean", String.format(java.util.Locale.ROOT, "%.1f", idleSamples == 0 ? 0f : idleSum / (float) idleSamples));
        m.put(name + ".police_0s", "" + police[0]);
        m.put(name + ".police_60s", "" + police[1]);
        m.put(name + ".police_120s", "" + police[2]);
        m.put(name + ".response_trips", "" + trips);
        m.put(name + ".response_trip_mean_s", String.format(java.util.Locale.ROOT, "%.1f", trips == 0 ? 0f : tripSecs / trips));
        m.put(name + ".sim_ms_per_step", String.format(java.util.Locale.ROOT, "%.2f", simNanos / 1e6 / (SECONDS * 30)));
        System.err.println(name + ": " + (System.currentTimeMillis() - wall) / 1000 + " s wall, " + w.fleet.vehicles.size() + " vehicles, "
                + w.entities.size() + " people and dead");
        return m;
    }

    /** Turned more than 300 degrees inside 40 of where it was, in under 8 s: one spin (then not again for 8 s). */
    static int spin(Map<Fleet.Vehicle, float[]> hist, Fleet.Vehicle v, int n) {
        // [0] count, [1] unwrapped angle, [2] last raw angle, [3] cooldown, then x, y, angle per sample.
        float[] h = hist.get(v);
        if (h == null) {
            hist.put(v, h = new float[4 + n * 3]);
            h[1] = h[2] = v.angle;
        }
        float da = v.angle - h[2];
        while (da > Math.PI) da -= 2 * Math.PI;
        while (da < -Math.PI) da += 2 * Math.PI;
        h[1] += da;
        h[2] = v.angle;
        int c = (int) h[0], at = 4 + (c % n) * 3;
        h[at] = v.x;
        h[at + 1] = v.y;
        h[at + 2] = h[1];
        h[0] = ++c;
        if (h[3] > 0) {
            h[3]--;
            return 0;
        }
        float minX = v.x, maxX = v.x, minY = v.y, maxY = v.y;
        for (int k = 1; k < Math.min(c, n); k++) {
            int b = 4 + ((c - 1 - k) % n) * 3;
            minX = Math.min(minX, h[b]);
            maxX = Math.max(maxX, h[b]);
            minY = Math.min(minY, h[b + 1]);
            maxY = Math.max(maxY, h[b + 1]);
            if (maxX - minX > 80 || maxY - minY > 80) break;
            if (Math.abs(h[1] - h[b + 2]) > Math.toRadians(300)) {
                h[3] = n;
                return 1;
            }
        }
        return 0;
    }

    /** How much of the smaller of two vehicles' bodies (rectangles as drawn) the other covers, 0 to 1. */
    static float overlap(Fleet.Vehicle a, Fleet.Vehicle b) {
        float[][] pa = body(a), pb = body(b);
        List<float[]> poly = new ArrayList<float[]>();
        for (float[] p : pa) poly.add(p);
        // Clip a's rectangle by each edge of b's (both wound the same way).
        for (int i = 0; i < 4 && !poly.isEmpty(); i++) {
            float[] e0 = pb[i], e1 = pb[(i + 1) % 4];
            List<float[]> next = new ArrayList<float[]>();
            for (int k = 0; k < poly.size(); k++) {
                float[] p = poly.get(k), q = poly.get((k + 1) % poly.size());
                float sp = side(e0, e1, p), sq = side(e0, e1, q);
                if (sp >= 0) next.add(p);
                if ((sp >= 0) != (sq >= 0)) {
                    float t = sp / (sp - sq);
                    next.add(new float[]{p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t});
                }
            }
            poly = next;
        }
        float area = 0;
        for (int k = 0; k < poly.size(); k++) {
            float[] p = poly.get(k), q = poly.get((k + 1) % poly.size());
            area += p[0] * q[1] - q[0] * p[1];
        }
        area = Math.abs(area) / 2;
        float sa = 4 * a.length() * a.halfWidth(), sb = 4 * b.length() * b.halfWidth();
        return area / Math.min(sa, sb);
    }

    static float side(float[] e0, float[] e1, float[] p) {
        return (e1[0] - e0[0]) * (p[1] - e0[1]) - (e1[1] - e0[1]) * (p[0] - e0[0]);
    }

    /** The four corners of a vehicle's body, counter-clockwise (in the map's y-down sense, consistently). */
    static float[][] body(Fleet.Vehicle v) {
        float c = (float) Math.cos(v.angle), s = (float) Math.sin(v.angle), l = v.length(), hw = v.halfWidth();
        float[][] p = new float[4][];
        float[][] k = {{l, hw}, {-l, hw}, {-l, -hw}, {l, -hw}};
        for (int i = 0; i < 4; i++) p[i] = new float[]{v.x + k[i][0] * c - k[i][1] * s, v.y + k[i][0] * s + k[i][1] * c};
        // (Wound so each edge has the inside on its left: side() >= 0 inside.)
        if (side(p[0], p[1], p[2]) < 0) {
            float[] t = p[1];
            p[1] = p[3];
            p[3] = t;
        }
        return p;
    }

    static int policeNow(World w) {
        int n = 0;
        for (Entity e : w.entities) if (!e.dead && e.type == Entity.COP) n++;
        for (Fleet.Vehicle v : w.fleet.vehicles) {
            for (Entity e : v.crew) if (e.type == Entity.COP) n++;
            for (Entity e : v.riders) if (e.type == Entity.COP) n++;
        }
        return n;
    }

    static float stationDist(World w, Entity e) {
        float best = Float.MAX_VALUE;
        for (City.Facility f : w.city.facilities)
            if (f.kind == City.FACILITY_POLICE) best = Math.min(best, (float) Math.hypot(e.x - f.x, e.y - f.y));
        return best;
    }

    // ------------------------------------------------------------------ comparing with the baseline

    static Map<String, Float> read(File f) throws Exception {
        Map<String, Float> m = new LinkedHashMap<String, Float>();
        java.io.BufferedReader in = new java.io.BufferedReader(new java.io.FileReader(f));
        for (String line; (line = in.readLine()) != null; ) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] kv = line.split("\\s+");
            if (kv.length >= 2) m.put(kv[0], Float.parseFloat(kv[1]));
        }
        in.close();
        return m;
    }

    /**
     * Worse by more than half, and by more than a set margin (so a count of 1 against 0 isn't a warning): higher is
     * worse for everything but the police numbers, which warn moving either way by a third. Returns how many warned.
     */
    static int compare(Map<String, Float> now, Map<String, Float> base) {
        int warn = 0;
        for (Map.Entry<String, Float> e : now.entrySet()) {
            String k = e.getKey();
            float v = e.getValue();
            Float b = base.get(k);
            String verdict;
            if (b == null) verdict = "NEW ";
            else if (k.contains(".police_")) verdict = v < b * 0.67f - 5 || v > b * 1.5f + 5 ? "WARN" : "OK  ";
            else if (k.endsWith(".response_trips")) verdict = v < b * 0.5f - 3 ? "WARN" : "OK  ";
            else verdict = v > b * 1.5f + margin(k) ? "WARN" : "OK  ";
            if (verdict.startsWith("WARN")) warn++;
            System.out.println(String.format(java.util.Locale.ROOT, "%s %-42s %8s   (baseline %s)", verdict, k, fmt(v), b == null ? "-" : fmt(b)));
        }
        System.out.println(warn == 0 ? "health: OK" : "health: " + warn + " WARN");
        return 0;
    }

    static float margin(String k) {
        if (k.endsWith("vehicles_stuck_30s")) return 3;
        if (k.endsWith("overlap_pair_seconds")) return 5;
        if (k.endsWith("spins")) return 3;
        if (k.contains("idle_police")) return 3;
        if (k.endsWith("response_trip_mean_s")) return 5;
        if (k.endsWith("sim_ms_per_step")) return 1;
        return 0;
    }

    static String fmt(float v) {
        return v == Math.round(v) ? "" + (int) v : String.format(java.util.Locale.ROOT, "%.2f", v);
    }
}
