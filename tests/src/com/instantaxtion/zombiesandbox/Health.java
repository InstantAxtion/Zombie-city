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
    static final int SECONDS = Integer.getInteger("health.seconds", 120), OUTBREAK_AT = 10;
    /** ((29.2) How many of the dead in each of the three packs (8 for the check; more to look at a big fight). */
    static final int PACK = Integer.getInteger("health.pack", 8);

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
        // ((29.2) Junction waits over 20 s; trips out of a station's grounds (from setting off to 60 from where it stood);
        // how much further than the straight line a trip to a call drives; seconds on a building's ground; people
        // shoved aside by something driving into them.)
        int longHolds = 0, exits = 0, slowExits = 0, detours = 0, pushed = 0;
        float exitSecs = 0, wallSecs = 0, detourSum = 0;
        Map<Fleet.Vehicle, float[]> hold = new IdentityHashMap<Fleet.Vehicle, float[]>();
        Map<Fleet.Vehicle, float[]> leave = new IdentityHashMap<Fleet.Vehicle, float[]>();
        Map<Fleet.Vehicle, float[]> route = new IdentityHashMap<Fleet.Vehicle, float[]>();
        Map<Fleet.Vehicle, Boolean> was = new IdentityHashMap<Fleet.Vehicle, Boolean>();
        Map<String, Integer> shoveWhy = new java.util.TreeMap<String, Integer>();
        Map<Entity, float[]> foot = new IdentityHashMap<Entity, float[]>();
        Map<Fleet.Vehicle, Float> board = new IdentityHashMap<Fleet.Vehicle, Float>();
        int footStuck = 0, idleNear = 0, boardWaits = 0, towardDead = 0, calmNearDead = 0, bunched = 0;
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
                    for (int k = 0; k < PACK; k++) {
                        float[] q = w.city.findWalkable(p[0] + (k % 5) * 8, p[1] + (k / 5) * 8);
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
                    if (System.getenv("HEALTH_STUCK") != null) {
                        System.err.println(describe(w, v, "STUCK"));
                        int jn = w.city.junctionIdAt(v.x, v.y);
                        if (jn >= 0) System.err.println("   junction type " + w.city.junctions.get(jn)[4] + " box " + java.util.Arrays.toString(w.city.junctions.get(jn)));
                        for (Fleet.Vehicle o : w.fleet.vehicles)
                            if (o != v && !Fleet.airborne(o) && Math.hypot(o.x - v.x, o.y - v.y) < 60)
                                System.err.println("   near " + (int) o.x + "," + (int) o.y + " ang " + String.format("%.2f", o.angle % 6.283f) + " sp " + (int) o.speed + " " + w.fleet.status(o)
                                        + " queued " + o.queued + " held " + o.held + " still " + (int) o.stillT + " jn " + w.city.junctionIdAt(o.x, o.y) + " giveWay " + o.giveWayT + " back " + o.backT);
                        System.err.println("   self ang " + String.format("%.2f", v.angle % 6.283f) + " giveWay " + v.giveWayT + " back " + v.backT);
                    }
                }
                // Held at a junction for more than 20 s on end.
                float[] hh = hold.get(v);
                if (v.held && !v.parked) {
                    if (hh == null) hold.put(v, hh = new float[]{w.time, 0});
                    if (hh[1] == 0 && w.time - hh[0] > 20) {
                        hh[1] = 1;
                        longHolds++;
                    }
                } else hold.remove(v);
                // Out of the yard: parked (or waiting) one moment, setting off the next, and how long till 60 from there.
                boolean still = v.parked || v.state == Fleet.IDLE || v.state == Fleet.STANDBY || v.state == Fleet.WAIT || v.state == Fleet.MUSTER;
                Boolean before = was.put(v, still);
                float[] lv = leave.get(v);
                if (before != null && before && !still && v.type != Fleet.CAR && lv == null) leave.put(v, lv = new float[]{w.time, v.x, v.y});
                if (lv != null && System.getenv("HEALTH_EXITS") != null && (int) ((w.time - lv[0]) * 30) == 8 * 30 && Math.hypot(v.x - lv[1], v.y - lv[2]) < 30)
                    System.err.println("AT8S " + describe(w, v, ""));
                if (lv != null) {
                    if (Math.hypot(v.x - lv[1], v.y - lv[2]) > 60) {
                        exits++;
                        exitSecs += w.time - lv[0];
                        if (w.time - lv[0] > 15) {
                            slowExits++;
                            if (System.getenv("HEALTH_EXITS") != null) System.err.println("SLOWEXIT " + (int) (w.time - lv[0]) + "s from " + (int) lv[1] + "," + (int) lv[2] + " " + describe(w, v, ""));
                        }
                        leave.remove(v);
                    } else if (still || w.time - lv[0] > 90) {
                        if (!still) {
                            exits++;
                            slowExits++;
                            if (System.getenv("HEALTH_EXITS") != null) System.err.println("NOEXIT 90s from " + (int) lv[1] + "," + (int) lv[2] + " " + describe(w, v, ""));
                            exitSecs += 90;
                        }
                        leave.remove(v);
                    }
                }
                // On a building's ground (not driving: through a wall).
                if (!v.parked && Math.abs(v.speed) > 1) {
                    int bt = w.city.tileIndex(v.x, v.y);
                    if (bt >= 0 && w.city.tiles[bt] == City.BUILDING) wallSecs += DT;
                }
                // A patrol car's trip to a call: from being sent to being there.
                if (v.type == Fleet.CRUISER && v.patrol) {
                    float[] ro = route.get(v);
                    if (v.state == Fleet.DRIVE && v.incident != null) {
                        if (ro == null) route.put(v, ro = new float[]{v.x, v.y, v.x, v.y, 0});
                        ro[4] += (float) Math.hypot(v.x - ro[2], v.y - ro[3]);
                        ro[2] = v.x;
                        ro[3] = v.y;
                    } else if (ro != null) {
                        float straight = (float) Math.hypot(v.x - ro[0], v.y - ro[1]);
                        if (v.state == Fleet.SCENE && straight > 150) {
                            float ratio = ro[4] / straight;
                            detourSum += ratio;
                            if (ratio > 2.2f) detours++;
                        }
                        route.remove(v);
                    }
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
                if (System.getenv("HEALTH_SHOVE") != null && Math.abs(v.speed) > 20) {
                    for (Entity e : w.entities) {
                        if (e.dead || e.isZombie() || e.ride != null || e.removed) continue;
                        if (Math.abs(e.x - v.x) > 12 || Math.abs(e.y - v.y) > 12) continue;
                        if (Math.hypot(e.x - v.x, e.y - v.y) < 9 + e.radius) {
                            int et = w.city.tileIndex(e.x, e.y);
                            String k = w.fleet.status(v).replaceAll(" to .*| at .*| on .*|\\(.*", "") + " | state " + v.state + " | person on tile " + w.city.tiles[et] + (w.city.junctionIdAt(e.x, e.y) >= 0 ? " (junction)" : "") + " | " + (e.fleeTimer > 0 ? "fleeing" : "calm") + " " + e.type;
                            shoveWhy.merge(k, 1, Integer::sum);
                        }
                    }
                }
                int t = w.city.tileIndex(v.x, v.y);
                if (t >= 0 && (w.city.tiles[t] == City.ROAD || w.city.tiles[t] == City.CAR) && !v.parked) road.add(v);
                if (f % 3 == 0) {
                    int sp = spin(turn, v, N);
                    spins += sp;
                    if (sp > 0 && System.getenv("HEALTH_STUCK") != null) System.err.println(describe(w, v, "SPIN"));
                }
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
            // ((29.2) Units on foot: stuck (trying to go somewhere, under 15 further on in 20 s), idle with the dead close
            // (no task, a gun with rounds, one of the dead within 120, no shot in 10 s), and vehicles waiting over 30 s for
            // their people to board.)
            if (f % 30 == 29) {
                for (Entity e : w.entities) {
                    if (e.dead || e.removed || e.ride != null || !(e.type == Entity.COP || e.type == Entity.SOLDIER)) continue;
                    float[] fp = foot.get(e);
                    boolean trying = e.want > 2 && e.building == null && e.task != Dispatch.T_POST && e.task != Dispatch.T_HOLD && e.task != Dispatch.T_GUARD;
                    if (fp == null || !trying || Math.hypot(e.x - fp[0], e.y - fp[1]) > 15) foot.put(e, fp = new float[]{e.x, e.y, w.time, 0});
                    if (trying && fp[3] == 0 && w.time - fp[2] > 20) {
                        fp[3] = 1;
                        footStuck++;
                        if (System.getenv("HEALTH_FOOT") != null) System.err.println("FOOTSTUCK t" + (int) w.time + " " + footDesc(w, e));
                    }
                    if (e.task == Dispatch.T_NONE && e.ammo + e.reserve > 0 && w.time - e.shotAt > 10 && w.countZombiesNear(e.x, e.y, 120) > 0) {
                        idleNear++;
                        if (System.getenv("HEALTH_FOOT") != null && w.rnd.nextFloat() < 0.05f) System.err.println("IDLENEAR t" + (int) w.time + " " + footDesc(w, e));
                    }
                }
                for (Fleet.Vehicle v : w.fleet.vehicles) {
                    if (v.removedFromFleet || (v.state != Fleet.MUSTER && v.state != Fleet.PICKUP)) {
                        board.remove(v);
                        continue;
                    }
                    Float since = board.get(v);
                    if (since == null) board.put(v, since = w.time);
                    if (w.time - since > 30 && w.time - since <= 31) {
                        boardWaits++;
                        if (System.getenv("HEALTH_FOOT") != null) System.err.println("BOARDWAIT t" + (int) w.time + " " + describe(w, v, "") + " crew " + v.crew.size() + "/" + v.crewWanted + " coming " + v.coming);
                    }
                }
            }
            // ((29.2) Civilians, every second: running towards the dead, calm with one in sight close by, packed in a crowd
            // with the dead near.)
            if (f % 30 == 15) {
                for (Entity e : w.entities) {
                    if (e.dead || e.removed || e.type != Entity.CIVILIAN || e.ride != null || e.building != null || e.isArmed()) continue;
                    if (w.countZombiesNear(e.x, e.y, 80) == 0) continue;
                    Entity z = null;
                    float zd = 1e9f;
                    for (int k = 0; k < w.entities.size(); k++) {
                        Entity o2 = w.entities.get(k);
                        if (o2.dead || !o2.isZombie() || Math.abs(o2.x - e.x) > 80 || Math.abs(o2.y - e.y) > 80) continue;
                        float d = (float) Math.hypot(o2.x - e.x, o2.y - e.y);
                        if (d < zd) {
                            zd = d;
                            z = o2;
                        }
                    }
                    if (z == null) continue;
                    float sp = (float) Math.hypot(e.vx, e.vy);
                    if (e.fleeTimer > 0 && sp > 10 && zd < 60 && ((z.x - e.x) * e.vx + (z.y - e.y) * e.vy) / (zd * sp) > 0.5f) {
                        towardDead++;
                        if (System.getenv("HEALTH_CIV") != null && w.rnd.nextFloat() < 0.1f) System.err.println("TOWARD " + civDesc(w, e, z, zd));
                    }
                    if (e.fleeTimer <= 0 && zd < 40 && w.city.los(e.x, e.y, z.x, z.y)) {
                        calmNearDead++;
                        if (System.getenv("HEALTH_CIV") != null && w.rnd.nextFloat() < 0.1f) System.err.println("CALM " + civDesc(w, e, z, zd));
                    }
                    if (e.fleeTimer > 0) {
                        int crowd = 0;
                        for (int k = w.gatherLiving(e.x, e.y, 15) - 1; k >= 0; k--) if (w.gathered[k].type == Entity.CIVILIAN && Math.hypot(w.gathered[k].x - e.x, w.gathered[k].y - e.y) < 15) crowd++;
                        if (crowd >= 9) bunched++;
                    }
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
        m.put(name + ".junction_holds_20s", "" + longHolds);
        m.put(name + ".station_exits", "" + exits);
        m.put(name + ".station_exit_mean_s", String.format(java.util.Locale.ROOT, "%.1f", exits == 0 ? 0f : exitSecs / exits));
        m.put(name + ".station_exits_over_15s", "" + slowExits);
        m.put(name + ".call_detours_over_2x", "" + detours);
        m.put(name + ".call_route_ratio_mean", String.format(java.util.Locale.ROOT, "%.2f", trips == 0 ? 0f : detourSum / Math.max(1, trips)));
        m.put(name + ".seconds_on_building_ground", String.format(java.util.Locale.ROOT, "%.1f", wallSecs));
        m.put(name + ".people_shoved_by_vehicles", "" + w.peopleShoved);
        m.put(name + ".units_on_foot_stuck_20s", "" + footStuck);
        m.put(name + ".unit_seconds_idle_near_dead", "" + idleNear);
        m.put(name + ".boarding_waits_30s", "" + boardWaits);
        m.put(name + ".civilian_seconds_running_at_dead", "" + towardDead);
        m.put(name + ".civilian_seconds_calm_near_dead", "" + calmNearDead);
        m.put(name + ".civilian_seconds_bunched_fleeing", "" + bunched);
        m.put(name + ".civilians_lost", "" + w.civiliansLost);
        if (!shoveWhy.isEmpty()) {
            List<Map.Entry<String, Integer>> l = new ArrayList<Map.Entry<String, Integer>>(shoveWhy.entrySet());
            l.sort((x, y) -> y.getValue() - x.getValue());
            for (int i = 0; i < Math.min(20, l.size()); i++) System.err.println("SHOVE " + l.get(i).getValue() + "  " + l.get(i).getKey());
        }
        m.put(name + ".sim_ms_per_step", String.format(java.util.Locale.ROOT, "%.2f", simNanos / 1e6 / (SECONDS * 30)));
        System.err.println(name + ": " + (System.currentTimeMillis() - wall) / 1000 + " s wall, " + w.fleet.vehicles.size() + " vehicles, "
                + w.entities.size() + " people and dead");
        return m;
    }

    static String footDesc(World w, Entity e) {
        int t = w.city.tileIndex(e.x, e.y);
        Entity z = null;
        float zd = 1e9f;
        for (Entity o : w.entities) {
            if (o.dead || !o.isZombie()) continue;
            float d = (float) Math.hypot(o.x - e.x, o.y - e.y);
            if (d < zd) {
                zd = d;
                z = o;
            }
        }
        return (e.type == Entity.COP ? "cop" : "soldier") + " role " + e.role + " task " + e.task + " at " + (int) e.x + "," + (int) e.y + " tile " + (t >= 0 ? w.city.tiles[t] : -1)
                + " want " + (int) e.want + " mx " + String.format("%.1f,%.1f", e.mx, e.my) + " ammo " + e.ammo + "+" + e.reserve + " reload " + String.format("%.1f", e.reload)
                + " lastShot " + (int) (w.time - e.shotAt) + "s rig " + (e.rig != null) + " path " + (e.path == null ? "none" : e.path.length) + " blocked " + e.blocked
                + " inc " + (e.incident != null) + " nearestDead " + (int) zd + (z != null && w.city.los(e.x, e.y, z.x, z.y) ? " (seen)" : " (no los)") + " inside " + (e.building != null)
                + " goal " + e.goal + " squad " + e.squad + "/" + e.member + " post " + (int) e.postX + "," + (int) e.postY
                + baseInfo(w, e) + " leader " + (e.leader != null) + " pathDest " + (e.pathDest != null ? (int) e.pathDest.doorX + "," + (int) e.pathDest.doorY : "-")
;
    }

    static String civDesc(World w, Entity e, Entity z, float zd) {
        int t = w.city.tileIndex(e.x, e.y);
        return "civ task " + e.task + " flee " + String.format("%.1f", e.fleeTimer) + " at " + (int) e.x + "," + (int) e.y + " tile " + w.city.tiles[t] + " dead " + (int) zd
                + " leader " + (e.leader != null) + " home " + (e.home != null) + " v " + (int) e.vx + "," + (int) e.vy + " toDead " + (int) (z.x - e.x) + "," + (int) (z.y - e.y)
                + " threat " + (int) (e.threatX - e.x) + "," + (int) (e.threatY - e.y) + " stun " + String.format("%.1f", e.stun) + " pathDest " + (e.pathDest != null) + " zones " + w.dispatch.zones.size();
    }

    static String baseInfo(World w, Entity e) {
        City.Facility b = w.city.nearestFacility(City.FACILITY_BASE, e.x, e.y);
        return b == null ? "" : " base " + b.name + " at " + (int) b.x + "," + (int) b.y + " r " + (int) b.r + " d " + (int) Math.hypot(b.x - e.x, b.y - e.y);
    }

    static String routeTest(World w, Fleet.Vehicle v) {
        Fleet.Route f = v.field;
        float tx = v.tx, ty = v.ty;
        w.fleet.routing.urgent = true;
        float[] d = w.city.nearestDrivable(w.city.worldW() / 2, w.city.worldH() / 2);
        int st = v.state;
        v.state = Fleet.DRIVE;
        boolean ok = w.fleet.routing.route(v, d[0], d[1]);
        float[] e = w.city.nearestDrivable(v.x + (float) Math.cos(v.angle) * 500, v.y + (float) Math.sin(v.angle) * 500);
        boolean ahead = e != null && w.fleet.routing.route(v, e[0], e[1]);
        w.fleet.routing.mayTurnBack = true;
        boolean turn = w.fleet.routing.route(v, d[0], d[1]);
        w.fleet.routing.mayTurnBack = false;
        v.state = st;
        w.fleet.routing.urgent = false;
        String s = ok + " ahead500 " + ahead + " withTurn " + turn + " angle " + String.format("%.2f", v.angle) + " dir " + w.city.roadDir[w.city.tileIndex(v.x, v.y)];
        v.field = f;
        v.tx = tx;
        v.ty = ty;
        return s;
    }

    /** ((29.2) One vehicle, for the stuck and spin logs (HEALTH_STUCK=1). */
    static String describe(World w, Fleet.Vehicle v, String what) {
        int t = w.city.tileIndex(v.x, v.y);
        Fleet.Vehicle near = null;
        float nd = 1e9f;
        for (Fleet.Vehicle o : w.fleet.vehicles) {
            if (o == v || o.removedFromFleet || Fleet.airborne(o)) continue;
            float d = (float) Math.hypot(o.x - v.x, o.y - v.y);
            if (d < nd) {
                nd = d;
                near = o;
            }
        }
        return what + " t" + (int) w.time + " " + w.fleet.status(v) + " type " + v.type + " kind " + v.kind + " state " + v.state + " at " + (int) v.x + "," + (int) v.y
                + " tile " + (t >= 0 ? w.city.tiles[t] : -1) + " jn " + w.city.junctionIdAt(v.x, v.y) + " speed " + (int) v.speed + " held " + v.held
                + " queued " + v.queued + " qheld " + v.queuedHeld + " field " + (v.field != null) + " pathT " + v.pathT + " still " + (int) v.stillT
                + " giveWay " + (int) v.giveWayT + " back " + (int) v.backT + " to " + (int) v.tx + "," + (int) v.ty
                + " agency " + v.agency + " destCd " + String.format("%.1f", v.destCd) + " orbited " + v.orbited + " belt " + w.city.onBelt(v.x, v.y) + " hwy " + w.city.onHighway(v.x, v.y)
                + " pick " + (v.destCd <= 0 ? w.fleet.routing.newDestination(v) + " -> " + (int) v.tx + "," + (int) v.ty : "cd")
                + " toCentre " + routeTest(w, v)
                + (near == null ? "" : " | nearest " + (int) nd + ": " + w.fleet.status(near) + " st " + near.state + " sp " + (int) near.speed + " parked " + near.parked + " broken " + near.broken);
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
