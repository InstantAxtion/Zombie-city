package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;

/**
 * (26.0) Convoys: a column of vehicles that travels together - escorts at the head and tail, trucks or buses between -
 * keeping its spacing, the lead waiting for anyone fallen behind. When the dead get in among it, it halts and its
 * gunners fight; once it's clear (or it's been held too long) it pushes on. Supply convoys bring ammunition to a safe
 * zone, a survivor base or a police station. Called by the city itself (a zone running low) or from the Events tool.
 * ((28.2) The evacuation convoys are gone: one in an old save turns round and leaves, empty - see dropEvacuations.)
 */
final class Convoys {
    /** ((28.2) 1 was an evacuation convoy, gone: see dropEvacuations.) */
    static final int SUPPLY = 0, EVAC = 1;
    static final int FORMING = 0, MOVING = 1, HALTED = 2, AT_TARGET = 3, LEAVING = 4, DONE = 5;
    /** What each vehicle in the queue to come on is: an escort, a supply truck. ((28.2) Buses and police escorts were the evacuation convoys': only in old saves.) */
    static final int V_ESCORT = 0, V_TRUCK = 1, V_BUS = 2, V_POLICE = 3;
    static final int MAX_COLUMNS = 3;
    static final int TRUCK_ROUNDS = 300;

    static final class Column {
        int purpose, state = FORMING, resume = MOVING;
        final ArrayList<Fleet.Vehicle> vs = new ArrayList<Fleet.Vehicle>();
        /** Still to come on, front to back. */
        final ArrayList<Integer> queue = new ArrayList<Integer>();
        float sx, sy, sa;
        Dispatch.SafeZone zone;
        SurvivorBases.Base base;
        City.Facility station;
        float tx, ty;
        /** ((28.2) aboard and carried were the evacuation convoys': kept so old saves read.) */
        int trucks, trucksStart, cargo, aboard, lost, carried;
        float stateT, age, quietT, saidCd, progressT, bestD = Float.MAX_VALUE;
        String place, from;
        Dispatch.Convoy tracker;
        /** ((33.0) Seconds left pushing through after a halt it couldn't hold: no halting again for the dead about meanwhile. */
        transient float pushT;
    }

    private final World w;
    final ArrayList<Column> columns = new ArrayList<Column>();
    private float autoCd = 30;
    int delivered, launched;

    Convoys(World w) {
        this.w = w;
    }

    /** Where a convoy comes from: the army's base (if it still stands), or the edge of the map nearest the target. */
    private boolean source(Column c) {
        City.Facility base = w.city.mainBase();
        if (c.purpose == SUPPLY && base != null && !base.fallen) {
            float[] p = w.city.nearestDrivable(base.gateX > 0 ? base.gateX : base.x, base.gateY > 0 ? base.gateY : base.y);
            // ((28.5) Only if there's a way by road from the base to it: a base with no road out (a radar station up a
            // track) had its convoy's lead vehicle sit at the gate for good, and the trucks behind it never came on.)
            if (p != null && !drivable(p[0], p[1], c.tx, c.ty)) p = null;
            if (p != null) {
                c.sx = p[0];
                c.sy = p[1];
                c.from = base.name != null ? base.name.trim() : "the base";
                c.sa = (float) Math.atan2(c.ty - p[1], c.tx - p[0]);
                return true;
            }
        }
        java.util.List<float[]> e = new java.util.ArrayList<float[]>(w.dispatch.entries(c.tx, c.ty));
        if (e.isEmpty()) return false;
        // ((28.5) The nearest way in that has a road on to it; none, and there's no convoy.)
        final float tx = c.tx, ty = c.ty;
        java.util.Collections.sort(e, new java.util.Comparator<float[]>() {
            @Override public int compare(float[] a, float[] b) {
                return Double.compare(World.hypot(a[0] - tx, a[1] - ty), World.hypot(b[0] - tx, b[1] - ty));
            }
        });
        float[] best = null;
        for (int i = 0; i < e.size() && i < 4 && best == null; i++) if (drivable(e.get(i)[0], e.get(i)[1], c.tx, c.ty)) best = e.get(i);
        if (best == null) return false;
        c.sx = best[0];
        c.sy = best[1];
        c.sa = (float) Math.atan2(c.ty - best[1], c.tx - best[0]);
        c.from = "outside the city";
        return true;
    }

    /**
     * ((28.5) Where a convoy pulls up for (x, y): the nearest road (it unloads within a hundred of it), else the nearest
     * anything that can be driven on. The nearest drivable could be a yard inside a survivor base's walls, with no way
     * in, and the convoy never set off.
     */
    private float[] roadNear(float x, float y) {
        City ci = w.city;
        int tx = (int) (x / City.T), ty = (int) (y / City.T);
        for (int r = 0; r <= 8; r++)
            for (int j = ty - r; j <= ty + r; j++)
                for (int i = tx - r; i <= tx + r; i++) {
                    if (Math.max(Math.abs(i - tx), Math.abs(j - ty)) != r || i < 0 || j < 0 || i >= ci.w || j >= ci.h) continue;
                    if (ci.tiles[j * ci.w + i] == City.ROAD) return new float[]{(i + 0.5f) * City.T, (j + 0.5f) * City.T};
                }
        return ci.nearestDrivable(x, y);
    }

    /** ((28.5) Whether a convoy's vehicle could find its way by road from (x, y) to (tx, ty). */
    private boolean drivable(float x, float y, float tx, float ty) {
        Fleet.Vehicle probe = w.fleet.make(Fleet.TRUCK);
        probe.x = x;
        probe.y = y;
        probe.angle = (float) Math.atan2(ty - y, tx - x);
        return w.fleet.routeTo(probe, tx, ty);
    }

    private boolean busy(Object target) {
        for (int i = 0; i < columns.size(); i++) {
            Column c = columns.get(i);
            if (c.state != DONE && (c.zone == target || c.base == target || c.station == target)) return true;
        }
        return false;
    }

    /** A supply convoy to a safe zone, survivor base or police station (one of them non-null). Null if it can't go. */
    Column supply(Dispatch.SafeZone z, SurvivorBases.Base b, City.Facility st, int rounds) {
        if (columns.size() >= MAX_COLUMNS) return null;
        Object target = z != null ? z : b != null ? b : st;
        if (target == null || busy(target)) return null;
        Column c = new Column();
        c.purpose = SUPPLY;
        c.zone = z;
        c.base = b;
        c.station = st;
        float[] t = z != null ? new float[]{z.x, z.y} : b != null ? new float[]{b.b.doorX, b.b.doorY}
                : new float[]{st.gateX > 0 ? st.gateX : st.x, st.gateY > 0 ? st.gateY : st.y};
        float[] road = roadNear(t[0], t[1]);
        if (road == null) return null;
        c.tx = road[0];
        c.ty = road[1];
        c.place = z != null ? z.place : b != null ? b.name : st.name != null ? st.name.trim() : w.city.placeName(t[0], t[1]);
        if (!source(c)) return null;
        c.trucks = c.trucksStart = Math.max(1, Math.min(3, (rounds + TRUCK_ROUNDS - 1) / TRUCK_ROUNDS));
        c.cargo = rounds;
        c.queue.add(V_ESCORT);
        for (int i = 0; i < c.trucks; i++) c.queue.add(V_TRUCK);
        c.queue.add(V_ESCORT);
        start(c, "SUPPLY CONVOY");
        w.dispatch.say(Dispatch.WHO_MILITARY, null, "Military: Supply convoy, " + c.trucks + (c.trucks == 1 ? " truck" : " trucks")
                + " and escort, leaving " + c.from + " for " + c.place + " with " + rounds + " rounds.", c.sx, c.sy);
        return c;
    }

    private void start(Column c, String label) {
        c.tracker = new Dispatch.Convoy();
        c.tracker.managed = true;
        c.tracker.type = Entity.SOLDIER;
        c.tracker.label = label;
        c.tracker.place = c.place;
        c.tracker.fromX = c.tracker.x = c.sx;
        c.tracker.fromY = c.tracker.y = c.sy;
        c.tracker.toX = c.tx;
        c.tracker.toY = c.ty;
        w.dispatch.convoys.add(c.tracker);
        columns.add(c);
        launched++;
        w.timeline(label.charAt(0) + label.substring(1).toLowerCase(java.util.Locale.ROOT) + " to " + c.place);
    }

    private boolean alive(Fleet.Vehicle v) {
        return v != null && !v.broken && !v.removedFromFleet;
    }

    /** Once a frame: brings the next of each column on, watches over it, and moves it on through its stages. */
    void update(float dt) {
        autoCd -= dt;
        if (autoCd <= 0) {
            autoCd = 20;
            auto();
        }
        for (int i = columns.size() - 1; i >= 0; i--) {
            Column c = columns.get(i);
            c.age += dt;
            c.stateT += dt;
            c.saidCd -= dt;
            // The next one on, once the last has pulled clear.
            if (!c.queue.isEmpty() && c.state != DONE) {
                boolean clear = true;
                for (int k = 0; k < c.vs.size() && clear; k++) {
                    Fleet.Vehicle o = c.vs.get(k);
                    if (alive(o) && World.hypot(o.x - c.sx, o.y - c.sy) < 30) clear = false;
                }
                if (clear) {
                    Fleet.Vehicle v = w.fleet.convoyVehicle(c.queue.remove(0), c.sx, c.sy, c.sa, c);
                    if (v != null) c.vs.add(v);
                }
            }
            // Losses.
            int alive = 0, trucks = 0;
            for (int k = 0; k < c.vs.size(); k++) {
                Fleet.Vehicle v = c.vs.get(k);
                if (alive(v)) {
                    alive++;
                    if (v.convoyRole == V_TRUCK) trucks++;
                } else if (v.convoy == c) {
                    v.convoy = null;
                    c.lost++;
                    if (c.saidCd <= 0) {
                        c.saidCd = 8;
                        w.dispatch.say(Dispatch.WHO_MILITARY, null, "Convoy: We've lost a " + roleName(v.convoyRole) + " near "
                                        + w.city.placeName(v.x, v.y) + "! The rest keep going.", v.x, v.y);
                    }
                    // (Its passengers get out where it stopped.)
                    if (v.convoyRole == V_BUS && !v.riders.isEmpty()) {
                        for (int r = 0; r < v.riders.size(); r++) w.release(v.riders.get(r), v.x, v.y);
                        v.riders.clear();
                    }
                }
            }
            c.trucks = trucks;
            Fleet.Vehicle head = head(c);
            if (head != null) {
                c.tracker.x = head.x;
                c.tracker.y = head.y;
            }
            c.tracker.label = (c.state == LEAVING ? "CONVOY RETURNING" : "SUPPLY CONVOY")
                    + (c.state == HALTED ? " - CONTACT!" : c.state == AT_TARGET ? " - UNLOADING" : "")
                    + "  " + alive + (alive == 1 ? " vehicle" : " vehicles");
            if (alive == 0 && c.queue.isEmpty() && c.state != FORMING) {
                end(c, c.state == LEAVING ? null : "Military: The supply convoy to " + c.place + " has been wiped out.");
                columns.remove(i);
                continue;
            }
            if (c.state == FORMING && (c.queue.isEmpty() || c.stateT > 45)) {
                // (Those that couldn't get on after most of a minute aren't waited for.)
                c.queue.clear();
                c.state = MOVING;
                c.stateT = 0;
            }
            // Getting nowhere (no closer in a minute and a half, not held by a fight): it turns back.
            if (head != null && (c.state == MOVING || c.state == FORMING)) {
                float d = (float) World.hypot(head.x - c.tx, head.y - c.ty);
                if (d < c.bestD - 40) {
                    c.bestD = d;
                    c.progressT = 0;
                } else c.progressT += dt;
                if (c.progressT > 90) {
                    leave(c, "Military: The convoy can't get through to " + c.place + ". Turning back.");
                    continue;
                }
            }
            // The dead in among it: halt and fight.
            int threat = 0;
            for (int k = 0; k < c.vs.size(); k++) {
                Fleet.Vehicle v = c.vs.get(k);
                if (alive(v)) threat += w.countZombiesNear(v.x, v.y, 55);
            }
            // ((33.0) "Pushing through" is pushing through: for a quarter of a minute after a halt it couldn't hold, it drives
            // on whatever's round it. It halted again the very next moment, the dead still in among it, and sat in the middle
            // of them another forty seconds - over and over, never getting anywhere till it was torn apart.)
            c.pushT -= dt;
            if ((c.state == MOVING || c.state == LEAVING) && threat >= 2 && (c.pushT <= 0 || w.city.cfg.civic < 61)) {
                c.resume = c.state;
                c.state = HALTED;
                c.stateT = 0;
                c.quietT = 0;
                if (c.saidCd <= 0 && head != null) {
                    c.saidCd = 25;
                    w.dispatch.say(Dispatch.WHO_MILITARY, null, "Convoy: Contact at " + w.city.placeName(head.x, head.y) + "! Halting - gunners, clear them off.", head.x, head.y);
                }
            } else if (c.state == HALTED) {
                c.quietT = threat == 0 ? c.quietT + dt : 0;
                // Clear - or held long enough: they push through.
                if (c.quietT > 2.5f || c.stateT > 40) {
                    if (c.stateT > 40 && c.saidCd <= 0 && head != null) {
                        c.saidCd = 25;
                        w.dispatch.say(Dispatch.WHO_MILITARY, null, "Convoy: We can't hold here. Pushing through!", head.x, head.y);
                    }
                    if (c.stateT > 40) c.pushT = 15;
                    c.state = c.resume;
                    c.stateT = c.resume == MOVING ? 0 : c.stateT;
                }
            }
            if (c.state == MOVING && head != null && World.hypot(head.x - c.tx, head.y - c.ty) < 110) arrive(c);
            else if (c.state == MOVING && c.stateT > 300) {
                // (No way through at all: it turns back.)
                leave(c, "Convoy: There's no way through to " + c.place + ". Turning back.");
            } else if (c.state == AT_TARGET && c.stateT > 6) {
                leave(c, null);
            }
        }
    }

    private Fleet.Vehicle head(Column c) {
        for (int k = 0; k < c.vs.size(); k++) if (alive(c.vs.get(k))) return c.vs.get(k);
        return null;
    }

    private static String roleName(int role) {
        return role == V_TRUCK ? "truck" : role == V_BUS ? "bus" : "escort";
    }

    /** At the target: a supply convoy unloads what's left of its rounds. */
    private void arrive(Column c) {
        c.state = AT_TARGET;
        c.stateT = 0;
        {
            int rounds = c.trucksStart <= 0 ? 0 : Math.round(c.cargo * (float) c.trucks / c.trucksStart);
            if (c.zone != null && !c.zone.removed) {
                c.zone.ammo += rounds;
                c.zone.supplyComing = false;
            } else if (c.base != null && !c.base.fallen) c.base.stash += rounds;
            else if (c.station != null) c.station.ammo += rounds;
            delivered += rounds;
            w.dispatch.say(Dispatch.WHO_MILITARY, null, "Convoy: At " + c.place + ". " + rounds + " rounds delivered"
                    + (c.lost > 0 ? " - we lost " + c.lost + (c.lost == 1 ? " vehicle" : " vehicles") + " on the way." : "."), c.tx, c.ty);
        }
    }

    /** Away again: back where it came from. */
    private void leave(Column c, String say) {
        c.state = LEAVING;
        c.stateT = 0;
        if (say != null) w.dispatch.say(Dispatch.WHO_MILITARY, null, say, c.tx, c.ty);
        float ex = c.sx, ey = c.sy;
        c.tracker.label = "CONVOY RETURNING";
        c.tracker.toX = ex;
        c.tracker.toY = ey;
        c.tracker.place = c.from;
        for (int k = 0; k < c.vs.size(); k++) {
            Fleet.Vehicle v = c.vs.get(k);
            if (!alive(v)) continue;
            v.homeX = ex;
            v.homeY = ey;
            v.convoyRouted = false;
        }
    }

    private void end(Column c, String say) {
        c.state = DONE;
        w.dispatch.convoys.remove(c.tracker);
        if (c.zone != null) c.zone.supplyComing = false;
        if (say != null) w.dispatch.say(Dispatch.WHO_MILITARY, null, say, c.tx, c.ty);
    }

    /** A vehicle of a column has gone off the map at the end, or in at the base. */
    void gone(Fleet.Vehicle v) {
        Column c = v.convoy;
        if (c == null) return;
        c.vs.remove(v);
        boolean left = false;
        for (int k = 0; k < c.vs.size(); k++) if (alive(c.vs.get(k))) left = true;
        if (!left && c.queue.isEmpty() && columns.contains(c)) {
            end(c, null);
            columns.remove(c);
        }
    }

    /**
     * ((28.2) A game saved with an evacuation convoy on the road: the people aboard get out where they are, and the
     * column turns round and goes back the way it came, empty, like a supply convoy that's delivered.)
     */
    void dropEvacuations() {
        for (int i = 0; i < columns.size(); i++) {
            Column c = columns.get(i);
            if (c.purpose == SUPPLY) continue;
            for (int k = 0; k < c.vs.size(); k++) {
                Fleet.Vehicle v = c.vs.get(k);
                for (int r = 0; r < v.riders.size(); r++) w.release(v.riders.get(r), v.x, v.y);
                v.riders.clear();
            }
            c.purpose = SUPPLY;
            c.queue.clear();
            c.zone = null;
            c.trucks = c.trucksStart = c.cargo = c.aboard = 0;
            if (c.from == null) c.from = "outside the city";
            if (c.state == FORMING || c.state == MOVING || c.state == HALTED || c.state == AT_TARGET) leave(c, null);
            else c.tracker.label = "CONVOY RETURNING";
        }
    }

    /**
     * One vehicle of a column, each step: to the target (or the way out), keeping its place - stopping short of the one
     * ahead, the lead waiting for the rest - and stopped while the column's halted. True once it's off the map.
     */
    boolean step(Fleet.Vehicle v, float dt) {
        Column c = v.convoy;
        w.fleet.convoyGun(v, dt);
        if (c.state == HALTED || c.state == AT_TARGET || c.state == DONE) {
            v.speed = Math.max(0, v.speed - dt * 220);
            return false;
        }
        float gx = c.state == LEAVING ? v.homeX : c.tx, gy = c.state == LEAVING ? v.homeY : c.ty;
        if (!v.convoyRouted) {
            v.routeCd -= dt;
            if (v.routeCd > 0) {
                v.speed = Math.max(0, v.speed - dt * 150);
                return false;
            }
            v.routeCd = 1.5f;
            v.convoyRouted = w.fleet.routeTo(v, gx, gy);
            if (!v.convoyRouted) return false;
        }
        int idx = c.vs.indexOf(v);
        float max = v.convoyRole == V_BUS ? 60 : 72;
        // Keeping its place behind the one ahead.
        Fleet.Vehicle ahead = null;
        for (int k = idx - 1; k >= 0 && ahead == null; k--) if (alive(c.vs.get(k))) ahead = c.vs.get(k);
        if (ahead != null) {
            float d = (float) World.hypot(ahead.x - v.x, ahead.y - v.y);
            if (d < 24) {
                v.speed = Math.max(0, v.speed - dt * 200);
                return false;
            }
            if (d < 46) max = Math.min(max, Math.max(10, ahead.speed));
        } else {
            // The lead: waiting for the rest when they've fallen behind.
            Fleet.Vehicle behind = null;
            for (int k = idx + 1; k < c.vs.size() && behind == null; k++) if (alive(c.vs.get(k))) behind = c.vs.get(k);
            if (behind != null && World.hypot(behind.x - v.x, behind.y - v.y) > 110) max = 6;
            if (!c.queue.isEmpty()) max = Math.min(max, 25);
        }
        boolean going = w.fleet.convoyDrive(v, dt, max);
        if (c.state == LEAVING) {
            float left = (float) World.hypot(v.x - gx, v.y - gy);
            // Off the map, or in at the base's gate (or out of road at the very end of the way).
            if (left < 50 || (!going && left < 160)) {
                gone(v);
                return true;
            }
            if (!going) v.convoyRouted = false;
            return false;
        }
        if (!going && c.state != LEAVING) v.convoyRouted = false;
        return false;
    }

    /** (The Events tool.) A supply convoy to whatever's at (x, y): a safe zone, a survivor base, a police station. */
    boolean supplyAt(float x, float y) {
        for (int i = 0; i < w.dispatch.zones.size(); i++) {
            Dispatch.SafeZone z = w.dispatch.zones.get(i);
            if (!z.removed && z.contains(x, y, 4)) return supply(z, null, null, 600) != null && (z.supplyComing = true);
        }
        for (int i = 0; i < w.bases.size(); i++) {
            SurvivorBases.Base b = w.bases.get(i);
            if (b.contains(x, y, 4)) return supply(null, b, null, 400) != null;
        }
        City.Facility best = null;
        float bd = 260;
        for (City.Facility f : w.city.facilities) {
            if (f.kind != City.FACILITY_POLICE || f.fallen) continue;
            float d = (float) World.hypot(f.x - x, f.y - y);
            if (d < bd) {
                bd = d;
                best = f;
            }
        }
        return best != null && supply(null, null, best, 600) != null;
    }

    /** The city's own convoys: ammunition to zones and stations running low. */
    private void auto() {
        if (!w.outbreak) return;
        for (int i = 0; i < w.dispatch.zones.size() && columns.size() < MAX_COLUMNS; i++) {
            Dispatch.SafeZone z = w.dispatch.zones.get(i);
            if (z.removed || !z.open) continue;
            if (z.ammo < 120 && !z.supplyComing && supply(z, null, null, 600) != null) z.supplyComing = true;
        }
        // (A police station whose armoury's nearly empty.)
        for (City.Facility f : w.city.facilities) {
            if (columns.size() >= MAX_COLUMNS) break;
            if (f.kind == City.FACILITY_POLICE && !f.fallen && f.ammo < 150 && w.zombieCount() > 20) supply(null, null, f, 600);
        }
    }
}
