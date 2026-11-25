package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Getting out: the danger map, evacuation, fleeing the city by road, boat and air, the airport, and coming home.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class Exodus {
    private final World w;

    Exodus(World w) {
        this.w = w;
    }

    /** Puts boats out on the sea, the river or the lake. */
    void launchBoats() {
        w.boats.clear();
        City c = w.city;
        Random r = new Random(c.cfg.seed * 13L + 5);
        if (c.waterKind == CityConfig.W_NONE) {
            // A rowing boat out on each lake in the country.
            for (int i = 0; i < c.natureSpots.size(); i++) {
                float[] q = c.natureSpots.get(i);
                if (q[2] != City.NL_LAKE) continue;
                for (int tries = 0; tries < 40; tries++) {
                    float x = q[0] + (r.nextFloat() - 0.5f) * 80, y = q[1] + (r.nextFloat() - 0.5f) * 80;
                    if (!navigable(x, y) || c.isBridge((int) (x / City.T), (int) (y / City.T))) continue;
                    w.boats.add(new float[]{x, y, r.nextBoolean() ? 1 : -1, r.nextFloat() - 0.5f, 6 + r.nextFloat() * 3, 0});
                    break;
                }
            }
            return;
        }
        int n = c.waterKind == CityConfig.W_LAKE ? 3 : Math.max(2, c.w / 50);
        for (int k = 0, tries = 0; k < n && tries < 400; tries++) {
            int tx = 2 + r.nextInt(c.w - 4), ty;
            if (c.seaY > 0) ty = c.seaY + 14 + r.nextInt(Math.max(1, c.h - c.seaY - 17));
            else ty = r.nextInt(c.h);
            if (ty < 0 || ty >= c.h || c.tiles[ty * c.w + tx] != City.WATER) continue;
            int kind = c.waterKind == CityConfig.W_LAKE ? 0 : c.waterKind == CityConfig.W_HARBOUR ? (r.nextBoolean() ? 3 : 1)
                    : 1 + r.nextInt(2);
            float dir = r.nextBoolean() ? 1 : -1;
            float dy = c.waterKind == CityConfig.W_LAKE ? (r.nextFloat() - 0.5f) : 0;
            float speed = kind == 0 ? 7 + r.nextFloat() * 4 : kind == 2 ? 10 + r.nextFloat() * 8 : 16 + r.nextFloat() * 14;
            w.boats.add(new float[]{(tx + 0.5f) * City.T, (ty + 0.5f) * City.T, dir, dy, speed, kind});
            k++;
        }
    }

    /** Water a boat can be on (under a bridge too). */
    boolean navigable(float x, float y) {
        int i = w.city.tileIndex(x, y);
        return i >= 0 && ((w.city.tiles[i] == City.WATER && !w.city.isFord(i)) || (w.city.bridge != null && w.city.bridge[i]));
    }

    void moveBoats(float dt) {
        for (int i = 0; i < w.boats.size(); i++) {
            float[] b = w.boats.get(i);
            float len = b[5] == 0 ? 10 : 16;
            float nx = b[0] + b[2] * b[4] * dt, ny = b[1] + b[3] * b[4] * dt;
            if (!navigable(nx + b[2] * len, ny + b[3] * len) || !navigable(nx, ny)) {
                // Turn about (and, on a lake, off on a new heading).
                b[2] = -b[2];
                b[3] = w.city.waterKind == CityConfig.W_LAKE || w.city.waterKind == CityConfig.W_NONE ? -b[3] + (w.rnd.nextFloat() - 0.5f) * 0.6f : 0;
                continue;
            }
            b[0] = nx;
            b[1] = ny;
        }
    }

    void updateDanger(float dt) {
        w.dangerTick -= dt;
        if (w.dangerTick > 0) return;
        w.dangerTick = 2;
        if (w.danger == null) {
            w.dgw = (w.city.w + World.DANGER_CELL - 1) / World.DANGER_CELL;
            w.dgh = (w.city.h + World.DANGER_CELL - 1) / World.DANGER_CELL;
            w.danger = new float[w.dgw * w.dgh];
            w.dangerDist = new int[w.dgw * w.dgh];
        }
        java.util.Arrays.fill(w.danger, 0);
        if (w.alert < 1) return;
        float cs = World.DANGER_CELL * City.T;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity z = w.entities.get(i);
            if (z.dead || !z.isZombie()) continue;
            int cx = Math.min(w.dgw - 1, Math.max(0, (int) (z.x / cs))), cy = Math.min(w.dgh - 1, Math.max(0, (int) (z.y / cs)));
            w.danger[cy * w.dgw + cx] += 1;
        }
        // A building with the dead shut inside counts too, once anyone knows.
        for (int i = 0, n = w.city.buildings.size(); i < n; i++) {
            City.Building b = w.city.buildings.get(i);
            if (b.lurkers <= 0 || !b.infestKnown) continue;
            int cx = Math.min(w.dgw - 1, (int) (b.doorX / cs)), cy = Math.min(w.dgh - 1, (int) (b.doorY / cs));
            w.danger[cy * w.dgw + cx] += b.lurkers * 0.5f;
        }
        // How many cells from the nearest bad one (a cell with 3 or more).
        int[] q = new int[w.dgw * w.dgh];
        int head = 0, tail = 0;
        for (int c = 0; c < w.dgw * w.dgh; c++) {
            if (w.danger[c] >= 3) {
                w.dangerDist[c] = 0;
                q[tail++] = c;
            } else w.dangerDist[c] = 999;
        }
        while (head < tail) {
            int c = q[head++], x = c % w.dgw, y = c / w.dgw;
            for (int k = 0; k < 4; k++) {
                int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = y + (k == 2 ? 1 : k == 3 ? -1 : 0);
                if (nx < 0 || ny < 0 || nx >= w.dgw || ny >= w.dgh) continue;
                int nc = ny * w.dgw + nx;
                if (w.dangerDist[nc] <= w.dangerDist[c] + 1) continue;
                w.dangerDist[nc] = w.dangerDist[c] + 1;
                q[tail++] = nc;
            }
        }
    }

    /** How many cells this spot is from ground the dead hold (0: in it; 999: nowhere near). */
    int dangerDistAt(float x, float y) {
        if (w.danger == null || w.alert < 1) return 999;
        float cs = World.DANGER_CELL * City.T;
        int cx = Math.min(w.dgw - 1, Math.max(0, (int) (x / cs))), cy = Math.min(w.dgh - 1, Math.max(0, (int) (y / cs)));
        return w.dangerDist[cy * w.dgw + cx];
    }

    /**
     * Nobody carries on with their day in the middle of it: someone out on the streets where the dead have
     * taken over (or next door to it) gets out, on foot, to the nearest part of town that's still quiet, and
     * keeps clear of it for a while. True while they're on their way (or waiting it out).
     */
    boolean evacuate(Entity e, float dt) {
        if (w.alert < 1 || w.danger == null || e.fleeTimer > 0) return false;
        if (e.task != Dispatch.T_NONE && e.task != Dispatch.T_EVACUATE) return false;
        int here = dangerDistAt(e.x, e.y);
        if (e.task == Dispatch.T_EVACUATE) {
            float d = (float) World.hypot(e.postX - e.x, e.postY - e.y);
            int there = dangerDistAt(e.postX, e.postY);
            if (there <= 1) {
                // The dead got there first: somewhere else.
                e.task = Dispatch.T_NONE;
            } else if (d > 30) {
                // (10.37) No way there on foot (over water, walled off): give it up for a while rather than push
                // against a wall towards it.
                if (e.path != null && e.path.length == 0 && e.pathDest == null && e.planX == e.postX && e.planY == e.postY && d > 60) {
                    e.task = Dispatch.T_NONE;
                    e.path = null;
                    e.taskTimer = 6;
                    e.goalCd = 0;
                    return false;
                }
                w.wilds.walkToSpot(e, e.postX, e.postY, e.fear > 0 ? e.runSpeed * 0.85f : e.speed * 1.6f);
                return true;
            } else {
                // Out: wait it out here a while before going back to anything.
                e.evacTime -= dt;
                if (e.evacTime <= 0) {
                    e.task = Dispatch.T_NONE;
                    return false;
                }
                w.holdoutLife.standAt(e, e.postX + (float) Math.sin(e.nameSeed + w.time * 0.1f) * 12, e.postY + (float) Math.cos(e.nameSeed * 1.3f + w.time * 0.1f) * 12, 0.4f);
                return true;
            }
        }
        if (here > 1) return false;
        // The quiet cell that's furthest from the trouble for the shortest walk.
        float cs = World.DANGER_CELL * City.T;
        int ex = (int) (e.x / cs), ey = (int) (e.y / cs);
        int best = -1;
        float bs = -Float.MAX_VALUE;
        for (int y = Math.max(0, ey - 8); y <= Math.min(w.dgh - 1, ey + 8); y++)
            for (int x = Math.max(0, ex - 8); x <= Math.min(w.dgw - 1, ex + 8); x++) {
                int c = y * w.dgw + x;
                if (w.dangerDist[c] < 3) continue;
                float score = Math.min(w.dangerDist[c], 6) * 3 - (float) World.hypot(x - ex, y - ey);
                if (score > bs) {
                    bs = score;
                    best = c;
                }
            }
        if (best < 0) return false;
        float[] p = w.city.findWalkable(((best % w.dgw) + 0.5f) * cs + (w.rnd.nextFloat() - 0.5f) * cs * 0.5f,
                ((best / w.dgw) + 0.5f) * cs + (w.rnd.nextFloat() - 0.5f) * cs * 0.5f);
        if (p == null) return false;
        e.task = Dispatch.T_EVACUATE;
        e.postX = p[0];
        e.postY = p[1];
        e.evacTime = 60 + w.rnd.nextFloat() * 60;
        e.paused = false;
        if (w.rnd.nextFloat() < 0.04f) w.dispatch.say(Dispatch.WHO_INFO, e, "They're all over " + w.city.placeName(e.x, e.y).split(" & ")[0]
                + ". I'm getting out of here.", e.x, e.y);
        w.wilds.walkToSpot(e, p[0], p[1], e.speed * 1.6f);
        return true;
    }

    /**
     * (15.10) Once a second: the dead at the terminal for long enough and the airport falls - no more flights; cleared
     * of them for a minute, and it opens again.
     */
    void airportLife() {
        float tx = w.city.apron[0], ty = w.city.apron[1] + 4 * City.T;
        int dead = w.spatial.countZombiesNear(tx, ty, 160);
        if (!w.airportClosed) {
            w.airportThreat = dead >= 6 ? w.airportThreat + 1 : Math.max(0, w.airportThreat - 2);
            if (w.airportThreat > 15) {
                w.airportClosed = true;
                w.airportClear = 0;
                w.banner("AIRPORT OVERRUN", "The dead have taken the airport. No more flights in or out.");
                w.stats.timeline("The airport fell to the dead", tx, ty);
                w.dispatch.sighting(tx, ty, null);
            }
        } else {
            w.airportClear = dead == 0 ? w.airportClear + 1 : 0;
            if (w.airportClear > 60) {
                w.airportClosed = false;
                w.airportThreat = 0;
                w.dispatch.say(Dispatch.WHO_INFO, null, "News: The airport has been cleared of the dead. Flights are running again.", tx, ty);
                w.stats.timeline("The airport was cleared and opened again", tx, ty);
            }
        }
    }

    /** (15.9) How many docks for evacuation boats on a map on the water (more on the bigger maps). */
    int addDocks() {
        if (w.city.waterKind == CityConfig.W_NONE || w.city.waterKind == CityConfig.W_LAKE) return 0;
        int want = Math.max(2, Math.min(5, (int) (w.city.worldW() / 2500)));
        java.util.Random r = new java.util.Random(w.city.cfg.seed * 17L + 3);
        int made = 0;
        for (int tries = 0; tries < want * 4 && made < want; tries++) {
            float[] q = w.city.randomWalkableInTown(r);
            if (q == null) continue;
            int[][] a = w.fleet.seaApproach(q[0], q[1]);
            if (a == null) continue;
            int land = a[1][0], water = a[1][1], w0 = w.city.w;
            float lx = (land % w0 + 0.5f) * City.T, ly = (land / w0 + 0.5f) * City.T;
            boolean close = false;
            for (World.EvacStop o : w.evacStops) if (o.dock && World.hypot(o.x - lx, o.y - ly) < 1500) close = true;
            if (close) continue;
            World.EvacStop st = new World.EvacStop();
            st.dock = true;
            st.route = a[0];
            st.bx = (water % w0 + 0.5f) * City.T;
            st.by = (water / w0 + 0.5f) * City.T;
            st.x = lx;
            st.y = ly;
            st.place = w.city.placeName(lx, ly) + " waterfront";
            w.evacStops.add(st);
            made++;
        }
        return made;
    }

    /** People are leaving the city right now (and the buses and trains are running). */
    boolean exodusOn() {
        return w.city.cfg.exodus() && w.exodusStarted && w.outbreak && w.alert >= 2 && !w.quarantined;
    }

    /** Leaving by bus and train is one of the ways people go. */
    boolean byTransit() {
        return w.city.cfg.exodus == CityConfig.EXODUS_TRANSIT || w.city.cfg.exodus == CityConfig.EXODUS_ALL;
    }

    /**
     * (10.47) With Fleeing citizens on: once the news breaks, people leave town - those out on the streets
     * make for the nearest way out of the city, those sitting it out indoors come out and go too when the
     * street outside is quiet - all but the few who won't leave their homes, survivor groups and anyone in a
     * safe zone. Once the outbreak is over and the city's recovering, they come home, a few at a time.
     */
    void updateExodus(float dt) {
        if (!w.city.cfg.exodus()) return;
        w.exodusTick -= dt;
        if (w.exodusTick > 0) return;
        w.exodusTick = 1;
        if (w.exits == null) findExits();
        if (w.exits.length == 0) return;
        // ((14.2) Quarantined: nobody new sets off, and anyone still on their way is turned back.)
        if (w.quarantined && w.outbreak) {
            w.war.turnBackLeavers();
            return;
        }
        if (!w.outbreak || w.alert < 2) {
            // (It's over: whoever was still on their way out turns round and goes home.)
            for (int i = 0, m = w.entities.size(); i < m; i++) {
                Entity e = w.entities.get(i);
                if (!e.dead && e.task == Dispatch.T_LEAVE) {
                    e.exitStop = -1;
                    e.task = e.home != null ? Dispatch.T_HOMECOMING : Dispatch.T_NONE;
                    e.path = null;
                    e.goalCd = 0;
                }
            }
            w.fleet.exodusOver();
            if (w.aftermath == World.AFTER_RECOVERY && w.afterTime > 30) homecoming();
            return;
        }
        if (!w.exodusStarted) {
            w.exodusStarted = true;
            w.stats.timeline("People are fleeing the city");
            if (byTransit()) {
                // (10.49) The buses and trains: where they pick up.
                if (w.evacStops.isEmpty()) {
                    int n = Math.max(4, Math.min(10, (int) (w.city.worldW() / 1300)));
                    for (float[] q : w.city.evacuationStops(n, new java.util.Random(w.city.cfg.seed * 13 + 5))) {
                        World.EvacStop st = new World.EvacStop();
                        st.bx = q[0];
                        st.by = q[1];
                        st.x = q[2];
                        st.y = q[3];
                        st.place = w.city.placeName(q[2], q[3]);
                        w.evacStops.add(st);
                    }
                }
                // ((15.10) And the airport, if there is one.)
                if (w.city.airport != null && w.city.gateArea != null) {
                    World.EvacStop st = new World.EvacStop();
                    st.airport = true;
                    st.x = st.bx = w.city.gateArea[0];
                    st.y = st.by = w.city.gateArea[1];
                    st.place = "the airport";
                    w.evacStops.add(st);
                    w.dispatch.say(Dispatch.WHO_INFO, null, "News: Evacuation flights are leaving from the airport.", st.x, st.y);
                }
                // ((15.9) And on the maps on the water, docks where boats take people off.)
                int docks = addDocks();
                if (docks > 0) {
                    StringBuilder dw = new StringBuilder();
                    for (World.EvacStop st : w.evacStops) if (st.dock) dw.append(dw.length() == 0 ? "" : ", ").append(st.place);
                    w.dispatch.say(Dispatch.WHO_INFO, null, "News: Boats are taking people off from the water's edge at " + dw + ".", w.city.worldW() / 2, w.city.worldH() / 2);
                    w.stats.timeline("Boats began taking people off by water");
                }
                StringBuilder where = new StringBuilder();
                for (int k = 0; k < w.evacStops.size() && k < 3; k++) where.append(k == 0 ? "" : ", ").append(w.evacStops.get(k).place);
                w.dispatch.say(Dispatch.WHO_INFO, null, "News: Evacuation buses are picking people up at " + where
                        + (w.evacStops.size() > 3 ? " and " + (w.evacStops.size() - 3) + " more stops" : "")
                        + (w.city.stationX >= 0 ? ". Trains are leaving from " + w.city.stationName + "." : "."), w.city.worldW() / 2, w.city.worldH() / 2);
            }
            if (w.city.cfg.exodus != CityConfig.EXODUS_TRANSIT)
                w.dispatch.say(Dispatch.WHO_INFO, null, "News: Thousands are leaving the city on foot. Authorities urge everyone leaving "
                        + "to keep to the main roads.", w.city.worldW() / 2, w.city.worldH() / 2);
        }
        // (A stop with the dead at it is closed: those waiting there go to another.)
        for (World.EvacStop st : w.evacStops) st.closed = w.spatial.countZombiesNear(st.x, st.y, 110) > 0 || (st.airport && w.airportClosed);
        // Out on the street: they decide to go (not all at once).
        for (int i = 0, m = w.entities.size(); i < m; i++) {
            Entity e = w.entities.get(i);
            if (e.exodusCd > 0) e.exodusCd -= 1;
            if (!mayFlee(e) || (e.task != Dispatch.T_NONE && e.task != Dispatch.T_EVACUATE) || w.rnd.nextFloat() > 0.2f) continue;
            startLeaving(e);
        }
        // Indoors: out of the door and away, while there's none of the dead outside it.
        ArrayList<Entity> out = null;
        ArrayList<City.Building> from = null;
        for (int i = 0, n = w.city.buildings.size(); i < n; i++) {
            City.Building b = w.city.buildings.get(i);
            if (b.occupants.isEmpty() && b.visitors.isEmpty()) continue;
            boolean group = false;
            for (int k = 0; k < w.holdouts.size(); k++) if (w.holdouts.get(k).b == b) group = true;
            if (group || b.collapsed || w.spatial.countZombiesNear(b.doorX, b.doorY, 140) > 0) continue;
            for (int pass = 0; pass < 2; pass++) {
                java.util.List<Entity> inside = pass == 0 ? b.occupants : b.visitors;
                for (int k = inside.size() - 1; k >= 0; k--) {
                    Entity e = inside.get(k);
                    if (e.type != Entity.CIVILIAN || e.infected || e == w.controlled || e.militia || e.holdout != null || World.stays(e)
                            || w.rnd.nextFloat() > 0.04f) continue;
                    inside.remove(k);
                    if (out == null) {
                        out = new ArrayList<Entity>();
                        from = new ArrayList<City.Building>();
                    }
                    out.add(e);
                    from.add(b);
                }
            }
        }
        if (out != null)
            for (int k = 0; k < out.size(); k++) {
                Entity e = out.get(k);
                w.indoors.leaveBuilding(e, from.get(k), false);
                e.leader = null;
                startLeaving(e);
            }
    }

    /** Someone on the street who might leave: an ordinary citizen on their own (families go with whoever leads them). */
    boolean mayFlee(Entity e) {
        return !e.dead && e.type == Entity.CIVILIAN && !e.infected && e != w.controlled && e.leader == null && !e.militia
                && e.holdout == null && !World.stays(e) && e.taskTimer <= 0 && e.exodusCd <= 0;
    }

    void startLeaving(Entity e) {
        // (10.49) By bus or train, if that's a way people go: the nearest stop still open, or the station.
        e.exitStop = -1;
        int how = w.city.cfg.exodus;
        if (byTransit()) {
            int bestStop = -1;
            float bd = Float.MAX_VALUE;
            for (int k = 0; k < w.evacStops.size(); k++) {
                World.EvacStop st = w.evacStops.get(k);
                float d = (float) World.hypot(st.x - e.x, st.y - e.y);
                if (!st.closed && d < bd) {
                    bd = d;
                    bestStop = k;
                }
            }
            float sx = 0, sy = 0;
            if (w.city.stationX >= 0) {
                float d = (float) World.hypot(w.city.stationX - e.x, w.city.stationY - e.y);
                if (d < bd && w.spatial.countZombiesNear(w.city.stationX, w.city.stationY, 150) == 0) {
                    bd = d;
                    bestStop = World.STOP_TRAIN;
                }
            }
            // (Both ways: on foot instead if the nearest stop is a long walk.)
            if (bestStop != -1 && (how == CityConfig.EXODUS_TRANSIT || bd < 1100)) {
                float[] p;
                if (bestStop == World.STOP_TRAIN) {
                    // On the platform, along the train: ((10.60) a row at a time, the next a step further back,
                    // rather than everyone on the same few spots.)
                    int row = Math.min(4, w.trainWaiting++ / 24);
                    float side = w.city.stationY < (w.city.railY0 + w.city.railRows / 2f) * City.T ? -1 : 1;
                    sx = w.city.stationX + (w.rnd.nextFloat() - 0.5f) * Fleet.TRAIN_LENGTH * 0.9f;
                    sy = w.city.stationY + side * (10 + row * 7 + w.rnd.nextFloat() * 3);
                    p = w.city.findWalkable(sx, sy);
                } else {
                    // ((10.60) Spread round the stop on the pavement, further out the more are waiting, instead of
                    // a crush of hundreds in one small square.)
                    World.EvacStop st = w.evacStops.get(bestStop);
                    p = waitSpot(st.x, st.y, st.waiting++);
                }
                if (p != null) {
                    e.exitStop = bestStop;
                    e.task = Dispatch.T_LEAVE;
                    e.postX = p[0];
                    e.postY = p[1];
                    e.evacTime = 0;
                    e.goalCd = 0;
                    e.paused = false;
                    return;
                }
            }
            if (how == CityConfig.EXODUS_TRANSIT) {
                // (Nowhere to get on: they stay, and look again in a while.)
                e.exodusCd = 25;
                return;
            }
        }
        float[] best = null;
        float bd = Float.MAX_VALUE;
        for (float[] x : w.exits) {
            float d = (float) World.hypot(x[0] - e.x, x[1] - e.y);
            if (d < bd) {
                bd = d;
                best = x;
            }
        }
        e.task = Dispatch.T_LEAVE;
        e.postX = best[0];
        e.postY = best[1];
        e.evacTime = 0;
        e.goalCd = 0;
        e.paused = false;
        if (w.rnd.nextFloat() < 0.02f)
            w.dispatch.say(Dispatch.WHO_INFO, e, w.rnd.nextBoolean() ? "We're getting out of the city while we still can." : "I'm not staying here to die. I'm leaving.", e.x, e.y);
    }

    /** On the way out of town (or home again afterwards). False if there's nothing to do after all. */
    boolean leaveStep(Entity e) {
        if (e.task == Dispatch.T_HOMECOMING) {
            City.Building h = e.home;
            if (h == null || h.collapsed || World.hypot(h.doorX - e.x, h.doorY - e.y) < 24 || World.unreachable(e, h.doorX, h.doorY)) {
                e.task = Dispatch.T_NONE;
                e.taskTimer = 2;
                return false;
            }
            w.wilds.walkToSpot(e, h.doorX, h.doorY, e.speed * 1.6f);
            return true;
        }
        if (e.task != Dispatch.T_LEAVE) return false;
        if (e.exitStop != -1) {
            // (10.49) To the bus stop or the platform, then wait there for the bus or the train.
            boolean shut = e.exitStop == World.STOP_TRAIN ? w.city.stationX < 0 : e.exitStop >= w.evacStops.size() || w.evacStops.get(e.exitStop).closed;
            if (shut || World.unreachable(e, e.postX, e.postY)) {
                e.task = Dispatch.T_NONE;
                startLeaving(e);
                if (e.task != Dispatch.T_LEAVE) e.exodusCd = 20;
                return e.task == Dispatch.T_LEAVE;
            }
            if (World.hypot(e.postX - e.x, e.postY - e.y) > 14) w.wilds.walkToSpot(e, e.postX, e.postY, e.fear > 0 ? e.runSpeed * 0.85f : e.speed * 1.4f);
            else w.holdoutLife.standAt(e, e.postX, e.postY, 0.3f);
            return true;
        }
        float m = City.T * 2.5f;
        if (World.hypot(e.postX - e.x, e.postY - e.y) < 40 || e.x < m || e.y < m || e.x > w.city.worldW() - m || e.y > w.city.worldH() - m) {
            goAway(e);
            return true;
        }
        if (World.unreachable(e, e.postX, e.postY)) {
            // (No way there on foot: another way out, and if there's none of those either, they stay.)
            e.evacTime++;
            float[] x = w.exits[w.rnd.nextInt(w.exits.length)];
            if (e.evacTime > 4) {
                e.task = Dispatch.T_NONE;
                e.taskTimer = 60;
                return false;
            }
            e.postX = x[0];
            e.postY = x[1];
            e.path = null;
        }
        w.wilds.walkToSpot(e, e.postX, e.postY, e.fear > 0 ? e.runSpeed * 0.85f : e.speed * 1.5f);
        return true;
    }

    /** (10.49) A bus or train of people fleeing the city has left the map: they're gone with it. */
    void droveOut(java.util.List<Entity> riders) {
        w.fledByTransit += riders.size();
        for (Entity e : riders) gone(e);
        riders.clear();
    }

    /**
     * (10.49) Aboard the bus or train at a stop (or the platform): those waiting for it there, with their families,
     * up to the room there is. Returns how many got on.
     */
    int boardEvac(Fleet.Vehicle v, int stop, float x, float y, float radius, int room) {
        int n = 0;
        // ((10.60) Those who got on leave room nearer the stop for the next ones.)
        if (stop == World.STOP_TRAIN) w.trainWaiting = Math.max(0, w.trainWaiting - room);
        else if (stop >= 0 && stop < w.evacStops.size()) w.evacStops.get(stop).waiting = Math.max(0, w.evacStops.get(stop).waiting - room);
        for (int i = 0, m = w.entities.size(); i < m && n < room; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || e.task != Dispatch.T_LEAVE || e.exitStop != stop || World.hypot(e.x - x, e.y - y) > radius) continue;
            for (int k = 0; k < m && n < room; k++) {
                Entity o = w.entities.get(k);
                if (o == e || o.dead || o.leader != e || o == w.controlled || World.hypot(o.x - e.x, o.y - e.y) > 80) continue;
                aboard(v, o);
                n++;
            }
            aboard(v, e);
            n++;
        }
        return n;
    }

    void aboard(Fleet.Vehicle v, Entity e) {
        e.task = Dispatch.T_NONE;
        e.exitStop = -1;
        e.dead = true;
        e.removed = true;
        e.vx = e.vy = 0;
        v.riders.add(e);
    }

    /** How many are on their way to (or waiting at) a stop, and how many are there now. */
    int waitingFor(int stop, float x, float y, boolean there) {
        float reach = stop == World.STOP_TRAIN ? Fleet.TRAIN_LENGTH * 0.6f : World.STOP_REACH + 6;
        int n = 0;
        for (int i = 0, m = w.entities.size(); i < m; i++) {
            Entity e = w.entities.get(i);
            if (!e.dead && e.task == Dispatch.T_LEAVE && e.exitStop == stop && (!there || World.hypot(e.x - x, e.y - y) < reach)) n++;
        }
        return n;
    }

    /**
     * (10.60) Somewhere to wait for the bus near (x, y), for the n-th person to arrive: off the road, on the
     * pavement, a square or the grass round the stop, the circle they're spread over growing as more come.
     */
    float[] waitSpot(float x, float y, int n) {
        float reach = Math.min(World.STOP_REACH, 14 + 5f * (float) Math.sqrt(n));
        for (int k = 0; k < 16; k++) {
            float r = reach * (float) Math.sqrt(w.rnd.nextFloat()), a = w.rnd.nextFloat() * (float) Math.PI * 2;
            float px = x + (float) Math.cos(a) * r, py = y + (float) Math.sin(a) * r;
            int tx = (int) (px / City.T), ty = (int) (py / City.T);
            if (tx < 0 || ty < 0 || tx >= w.city.w || ty >= w.city.h || w.city.solidTile(tx, ty)) continue;
            byte t = w.city.tiles[ty * w.city.w + tx];
            if (t != City.SIDEWALK && t != City.PLAZA && t != City.GRASS) continue;
            if (w.city.circleBlocked(px, py, 4f)) continue;
            return new float[]{px, py};
        }
        return w.city.findWalkable(x + w.rnd.nextFloat() * 24 - 12, y + w.rnd.nextFloat() * 24 - 12);
    }

    /** Out of the city: off the map until it's over, with the family that came with them. */
    void goAway(Entity e) {
        for (int i = 0, m = w.entities.size(); i < m; i++) {
            Entity o = w.entities.get(i);
            if (o != e && o.leader == e && !o.dead && o != w.controlled && World.hypot(o.x - e.x, o.y - e.y) < 140) gone(o);
        }
        gone(e);
    }

    void gone(Entity e) {
        e.dead = true;
        e.removed = true;
        e.vx = e.vy = 0;
        e.task = Dispatch.T_NONE;
        e.path = null;
        w.fled++;
        // (Out of it now: the war is between those still in the city.)
        w.startHumans = Math.max(1, w.startHumans - 1);
        // (Anyone bitten turns out on the road somewhere, and doesn't come back.)
        if (!e.infected) w.away.add(e);
    }

    /** The city's on its feet again: those who fled come home, a family or two at a time, each to their own home. */
    void homecoming() {
        if (w.away.isEmpty()) return;
        if ((w.exodusSaid & 1) == 0) {
            w.exodusSaid |= 1;
            w.banner("COMING HOME", (w.away.size() == 1 ? "1 person who fled the city is" : w.away.size() + " people who fled the city are") + " on their way back.");
            w.stats.timeline("Those who fled start coming home");
        }
        float[] x = w.exits[w.rnd.nextInt(w.exits.length)];
        int n = Math.min(w.away.size(), 2 + w.rnd.nextInt(4));
        for (int k = 0; k < n; k++) {
            Entity e = w.away.remove(0);
            float[] p = w.city.findWalkable(x[0] + w.rnd.nextFloat() * 40 - 20, x[1] + w.rnd.nextFloat() * 40 - 20);
            if (p == null) p = x;
            e.x = p[0];
            e.y = p[1];
            e.vx = e.vy = 0;
            e.dead = false;
            e.removed = false;
            e.fear = 0;
            e.fleeTimer = 0;
            e.task = e.leader == null ? Dispatch.T_HOMECOMING : Dispatch.T_NONE;
            e.goalCd = 0;
            w.addEntity(e);
            w.cameHome++;
        }
        if (w.away.isEmpty() && (w.exodusSaid & 2) == 0) {
            w.exodusSaid |= 2;
            w.dispatch.say(Dispatch.WHO_INFO, null, "News: The last of those who fled are back. " + (w.cameHome == 1 ? "1 person has" : w.cameHome + " people have") + " come home.",
                    w.city.worldW() / 2, w.city.worldH() / 2);
        }
    }

    /** The ways out of the map: where the roads (or the causeway) run off its edges. */
    void findExits() {
        java.util.Random r = new java.util.Random(w.city.cfg.seed * 31 + 7);
        ArrayList<float[]> list = new ArrayList<float[]>();
        for (int k = 0; k < 40 && list.size() < 10; k++) {
            float[] p = w.city.edgeRoad(r);
            if (p == null) break;
            boolean near = false;
            for (float[] q : list) if (World.hypot(q[0] - p[0], q[1] - p[1]) < 400) near = true;
            if (!near) list.add(p);
        }
        // (10.52) Over the beltway: the lanes that ran off the map's edges stop at it, so on foot it's out across
        // it, in the middle of each side (and a third and two thirds along on the bigger maps).
        if (w.city.beltTile != null)
            for (int side = 0; side < 4; side++)
                for (int f = 1; f <= 3; f++) {
                    if (f != 2 && w.city.w < 500) continue;
                    float along = (side < 2 ? w.city.worldW() : w.city.worldH()) * f / 4f, in = City.T * 1.5f;
                    float x = side < 2 ? along : side == 2 ? in : w.city.worldW() - in, y = side < 2 ? (side == 0 ? in : w.city.worldH() - in) : along;
                    float[] p = w.city.findWalkable(x, y);
                    if (p != null && (p[0] < City.T * 2.5f || p[1] < City.T * 2.5f || p[0] > w.city.worldW() - City.T * 2.5f || p[1] > w.city.worldH() - City.T * 2.5f))
                        list.add(p);
                }
        w.exits = list.toArray(new float[0][]);
    }
}
