package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;

/**
 * (26.0) Convoys: a column of vehicles that travels together - escorts at the head and tail, trucks or buses between -
 * keeping its spacing, the lead waiting for anyone fallen behind. When the dead get in among it, it halts and its
 * gunners fight; once it's clear (or it's been held too long) it pushes on. Supply convoys bring ammunition to a safe
 * zone, a survivor base or a police station; evacuation convoys take the people sheltering in a safe zone out of the
 * city. Called by the city itself (a zone running low, or full) or from the Events tool.
 */
final class Convoys {
    static final int SUPPLY = 0, EVAC = 1;
    static final int FORMING = 0, MOVING = 1, HALTED = 2, AT_TARGET = 3, LEAVING = 4, DONE = 5;
    /** What each vehicle in the queue to come on is: an escort, a supply truck, a bus. */
    static final int V_ESCORT = 0, V_TRUCK = 1, V_BUS = 2, V_POLICE = 3;
    static final int MAX_COLUMNS = 3;
    static final int TRUCK_ROUNDS = 300, BUS_SEATS = 36;

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
        int trucks, trucksStart, cargo, aboard, lost, carried;
        float stateT, age, quietT, saidCd, progressT, bestD = Float.MAX_VALUE;
        String place, from;
        Dispatch.Convoy tracker;
    }

    private final World w;
    final ArrayList<Column> columns = new ArrayList<Column>();
    private float autoCd = 30;
    int delivered, evacuatedBy, launched;

    Convoys(World w) {
        this.w = w;
    }

    /** Where a convoy comes from: the army's base (if it still stands), or the edge of the map nearest the target. */
    private boolean source(Column c) {
        City.Facility base = w.city.mainBase();
        if (c.purpose == SUPPLY && base != null && !base.fallen) {
            float[] p = w.city.nearestDrivable(base.gateX > 0 ? base.gateX : base.x, base.gateY > 0 ? base.gateY : base.y);
            if (p != null) {
                c.sx = p[0];
                c.sy = p[1];
                c.from = base.name != null ? base.name.trim() : "the base";
                c.sa = (float) Math.atan2(c.ty - p[1], c.tx - p[0]);
                return true;
            }
        }
        java.util.List<float[]> e = w.dispatch.entries(c.tx, c.ty);
        if (e.isEmpty()) return false;
        float[] best = e.get(0);
        float bd = Float.MAX_VALUE;
        for (float[] p : e) {
            float d = (float) World.hypot(p[0] - c.tx, p[1] - c.ty);
            if (d < bd) {
                bd = d;
                best = p;
            }
        }
        c.sx = best[0];
        c.sy = best[1];
        c.sa = (float) Math.atan2(c.ty - best[1], c.tx - best[0]);
        c.from = "outside the city";
        return true;
    }

    int running(int purpose) {
        int n = 0;
        for (int i = 0; i < columns.size(); i++) if (columns.get(i).purpose == purpose && columns.get(i).state != DONE) n++;
        return n;
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
        float[] road = w.city.nearestDrivable(t[0], t[1]);
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

    /** An evacuation convoy for the people sheltering in safe zone z. Null if it can't go. */
    Column evacuate(Dispatch.SafeZone z) {
        if (columns.size() >= MAX_COLUMNS || z == null || z.removed || busy(z) || z.sheltered <= 0) return null;
        Column c = new Column();
        c.purpose = EVAC;
        c.zone = z;
        float[] road = w.city.nearestDrivable(z.x, z.y);
        if (road == null) return null;
        c.tx = road[0];
        c.ty = road[1];
        c.place = z.place;
        if (!source(c)) return null;
        int buses = Math.max(1, Math.min(3, (z.sheltered + BUS_SEATS - 1) / BUS_SEATS));
        c.queue.add(V_POLICE);
        for (int i = 0; i < buses; i++) c.queue.add(V_BUS);
        c.queue.add(w.city.mainBase() != null ? V_ESCORT : V_POLICE);
        start(c, "EVACUATION CONVOY");
        w.dispatch.say(Dispatch.WHO_POLICE, null, "Dispatch: Evacuation convoy, " + buses + (buses == 1 ? " bus" : " buses")
                + " with an escort, on its way to " + c.place + " to take the people there out of the city.", c.sx, c.sy);
        return c;
    }

    private void start(Column c, String label) {
        c.tracker = new Dispatch.Convoy();
        c.tracker.managed = true;
        c.tracker.type = c.purpose == SUPPLY ? Entity.SOLDIER : Entity.COP;
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
                        w.dispatch.say(c.purpose == SUPPLY ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, null,
                                (c.purpose == SUPPLY ? "Convoy: " : "Evacuation convoy: ") + "We've lost a " + roleName(v.convoyRole) + " near "
                                        + w.city.placeName(v.x, v.y) + "! The rest keep going.", v.x, v.y);
                    }
                    // (Its passengers get out where it stopped.)
                    if (v.convoyRole == V_BUS && !v.riders.isEmpty()) {
                        for (int r = 0; r < v.riders.size(); r++) w.release(v.riders.get(r), v.x, v.y);
                        v.riders.clear();
                    }
                }
            }
            if (c.purpose == SUPPLY) c.trucks = trucks;
            Fleet.Vehicle head = head(c);
            if (head != null) {
                c.tracker.x = head.x;
                c.tracker.y = head.y;
            }
            c.tracker.label = (c.purpose == SUPPLY ? (c.state == LEAVING ? "CONVOY RETURNING" : "SUPPLY CONVOY") : "EVACUATION CONVOY")
                    + (c.state == HALTED ? " - CONTACT!" : c.state == AT_TARGET ? (c.purpose == EVAC ? " - LOADING" : " - UNLOADING") : "")
                    + "  " + alive + (alive == 1 ? " vehicle" : " vehicles") + (c.purpose == EVAC && c.aboard > 0 ? ", " + c.aboard + " aboard" : "");
            if (alive == 0 && c.queue.isEmpty() && c.state != FORMING) {
                end(c, c.state == LEAVING && c.purpose == EVAC ? null : (c.purpose == SUPPLY ? "Military: The supply convoy to " + c.place + " has been wiped out."
                        : "Dispatch: The evacuation convoy from " + c.place + " never made it out."));
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
                    leave(c, (c.purpose == SUPPLY ? "Military" : "Dispatch") + ": The convoy can't get through to " + c.place + ". Turning back.");
                    continue;
                }
            }
            // The dead in among it: halt and fight.
            int threat = 0;
            for (int k = 0; k < c.vs.size(); k++) {
                Fleet.Vehicle v = c.vs.get(k);
                if (alive(v)) threat += w.countZombiesNear(v.x, v.y, 55);
            }
            if ((c.state == MOVING || c.state == LEAVING) && threat >= 2) {
                c.resume = c.state;
                c.state = HALTED;
                c.stateT = 0;
                c.quietT = 0;
                if (c.saidCd <= 0 && head != null) {
                    c.saidCd = 25;
                    w.dispatch.say(c.purpose == SUPPLY ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, null, (c.purpose == SUPPLY ? "Convoy" : "Evacuation convoy")
                            + ": Contact at " + w.city.placeName(head.x, head.y) + "! Halting - gunners, clear them off.", head.x, head.y);
                }
            } else if (c.state == HALTED) {
                c.quietT = threat == 0 ? c.quietT + dt : 0;
                // Clear - or held long enough: they push through.
                if (c.quietT > 2.5f || c.stateT > 40) {
                    if (c.stateT > 40 && c.saidCd <= 0 && head != null) {
                        c.saidCd = 25;
                        w.dispatch.say(c.purpose == SUPPLY ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, null, "Convoy: We can't hold here. Pushing through!", head.x, head.y);
                    }
                    c.state = c.resume;
                    c.stateT = c.resume == MOVING ? 0 : c.stateT;
                }
            }
            if (c.state == MOVING && head != null && World.hypot(head.x - c.tx, head.y - c.ty) < 110) arrive(c);
            else if (c.state == MOVING && c.stateT > 300) {
                // (No way through at all: it turns back.)
                leave(c, "Convoy: There's no way through to " + c.place + ". Turning back.");
            } else if (c.state == AT_TARGET && c.stateT > (c.purpose == EVAC ? 10 : 6)) {
                if (c.purpose == EVAC) load(c);
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

    /** At the target: a supply convoy unloads what's left of its rounds; an evacuation convoy pulls up to load. */
    private void arrive(Column c) {
        c.state = AT_TARGET;
        c.stateT = 0;
        if (c.purpose == SUPPLY) {
            int rounds = c.trucksStart <= 0 ? 0 : Math.round(c.cargo * (float) c.trucks / c.trucksStart);
            if (c.zone != null && !c.zone.removed) {
                c.zone.ammo += rounds;
                c.zone.supplyComing = false;
            } else if (c.base != null && !c.base.fallen) c.base.stash += rounds;
            else if (c.station != null) c.station.ammo += rounds;
            delivered += rounds;
            w.dispatch.say(Dispatch.WHO_MILITARY, null, "Convoy: At " + c.place + ". " + rounds + " rounds delivered"
                    + (c.lost > 0 ? " - we lost " + c.lost + (c.lost == 1 ? " vehicle" : " vehicles") + " on the way." : "."), c.tx, c.ty);
        } else {
            w.dispatch.say(Dispatch.WHO_POLICE, null, "Evacuation convoy: At " + c.place + ". Get everyone aboard, quickly!", c.tx, c.ty);
        }
    }

    /** The people sheltering in the zone climb aboard the buses, as many as there are seats. */
    private void load(Column c) {
        Dispatch.SafeZone z = c.zone;
        if (z == null || z.removed) return;
        ArrayList<Fleet.Vehicle> buses = new ArrayList<Fleet.Vehicle>();
        for (int k = 0; k < c.vs.size(); k++) if (alive(c.vs.get(k)) && c.vs.get(k).convoyRole == V_BUS) buses.add(c.vs.get(k));
        if (buses.isEmpty()) return;
        int b = 0, n = 0;
        for (int i = 0, m = w.entities.size(); i < m; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || e.removed || e.task != Dispatch.T_SHELTER || e.zone != z || e.infected || e == w.controlled) continue;
            while (b < buses.size() && buses.get(b).riders.size() >= BUS_SEATS) b++;
            if (b >= buses.size()) break;
            Fleet.Vehicle bus = buses.get(b);
            e.task = Dispatch.T_NONE;
            e.zone = null;
            e.dead = true;
            e.removed = true;
            bus.riders.add(e);
            z.sheltered = Math.max(0, z.sheltered - 1);
            w.dispatch.sheltered = Math.max(0, w.dispatch.sheltered - 1);
            n++;
        }
        c.aboard = n;
        w.dispatch.say(Dispatch.WHO_POLICE, null, "Evacuation convoy: " + n + (n == 1 ? " person" : " people") + " aboard. Moving out!", c.tx, c.ty);
    }

    /** Away again: off the map by the nearest way out (an evacuation convoy) or back where it came from. */
    private void leave(Column c, String say) {
        c.state = LEAVING;
        c.stateT = 0;
        if (say != null) w.dispatch.say(c.purpose == SUPPLY ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, null, say, c.tx, c.ty);
        float ex = c.sx, ey = c.sy;
        if (c.purpose == EVAC) {
            java.util.List<float[]> out = w.dispatch.entries(c.tx, c.ty, true);
            float bd = Float.MAX_VALUE;
            for (float[] p : out) {
                float d = (float) World.hypot(p[0] - c.tx, p[1] - c.ty);
                if (d < bd) {
                    bd = d;
                    ex = p[0];
                    ey = p[1];
                }
            }
        }
        c.tracker.label = c.purpose == SUPPLY ? "CONVOY RETURNING" : "EVACUATION CONVOY";
        c.tracker.toX = ex;
        c.tracker.toY = ey;
        c.tracker.place = c.purpose == SUPPLY ? c.from : "out of the city";
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
        if (c.zone != null && c.purpose == SUPPLY) c.zone.supplyComing = false;
        if (say != null) w.dispatch.say(c.purpose == SUPPLY ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, null, say, c.tx, c.ty);
    }

    /** A vehicle of a column has gone off the map at the end: an evacuation bus's people count as got out. */
    void gone(Fleet.Vehicle v) {
        Column c = v.convoy;
        if (c == null) return;
        if (v.convoyRole == V_BUS && !v.riders.isEmpty()) {
            c.carried += v.riders.size();
            w.evacuated += v.riders.size();
            evacuatedBy += v.riders.size();
            v.riders.clear();
        }
        c.vs.remove(v);
        boolean left = false;
        for (int k = 0; k < c.vs.size(); k++) if (alive(c.vs.get(k))) left = true;
        if (!left && c.queue.isEmpty() && columns.contains(c)) {
            end(c, c.purpose == EVAC ? "Dispatch: The evacuation convoy from " + c.place + " is out of the city with " + c.carried
                    + (c.carried == 1 ? " person" : " people") + " aboard." : null);
            columns.remove(c);
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

    /** (The Events tool.) An evacuation convoy for the safe zone at (x, y). */
    boolean evacuateAt(float x, float y) {
        for (int i = 0; i < w.dispatch.zones.size(); i++) {
            Dispatch.SafeZone z = w.dispatch.zones.get(i);
            if (!z.removed && z.contains(x, y, 4)) return evacuate(z) != null;
        }
        return false;
    }

    /** The city's own convoys: ammunition to zones running low, and the people out of zones filling up. */
    private void auto() {
        if (!w.outbreak || w.city.cfg.mission >= 0) return;
        for (int i = 0; i < w.dispatch.zones.size() && columns.size() < MAX_COLUMNS; i++) {
            Dispatch.SafeZone z = w.dispatch.zones.get(i);
            if (z.removed || !z.open) continue;
            if (z.ammo < 120 && !z.supplyComing && supply(z, null, null, 600) != null) z.supplyComing = true;
            else if (z.sheltered >= Math.max(20, z.capacity * 0.7f) && w.outbreakTime > 90 && running(EVAC) == 0) evacuate(z);
        }
        // (A police station whose armoury's nearly empty.)
        for (City.Facility f : w.city.facilities) {
            if (columns.size() >= MAX_COLUMNS) break;
            if (f.kind == City.FACILITY_POLICE && !f.fallen && f.ammo < 150 && w.zombieCount() > 20) supply(null, null, f, 600);
        }
    }
}
