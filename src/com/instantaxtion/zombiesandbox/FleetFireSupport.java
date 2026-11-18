package com.instantaxtion.zombiesandbox;

import static com.instantaxtion.zombiesandbox.Fleet.*;

/**
 * ((27.5) Part of the vehicle code.) The army's fire support trucks: a mortar on the back of a truck, one or two in the
 * garages at each army base (two at a big one). When there's a big fight in reach - a pack of 25 or more of the dead with people fighting them -
 * one comes out of the gate, drives to a clear spot well behind the lines, sets up, and drops salvos (the same rounds
 * as a fire mission: Combat.shells) on the biggest pack with nobody living near it, a while between salvos; then it
 * goes home and back into the garage to restock. The dead getting close, it packs up and goes.
 */
final class FleetFireSupport {
    final Fleet fleet;
    final World w;
    final City city;
    /** Salvos fired, all told (for the tests and probes). */
    int salvos;

    static final float LOOK = 1600, REACH = 650, BACK = 320, COOLDOWN = 30, CLEAR = 150, RESTOCK = 20;
    static final int SALVOS = 6, PACK = 25, PACK_HERE = 15;

    /**
     * Per base: {trucks it has, ready again at}. Kept in the garages - off the map - until sent: parked out in the yard
     * they got in the way of the base's own trucks and people.
     */
    private final java.util.HashMap<City.Facility, float[]> garage = new java.util.HashMap<City.Facility, float[]>();
    private float lookCd;

    FleetFireSupport(Fleet fleet) {
        this.fleet = fleet;
        this.w = fleet.w;
        this.city = fleet.city;
    }

    /** {trucks base f has, ready again at}: one at an army base, two at a big one. */
    float[] garage(City.Facility f) {
        float[] g = garage.get(f);
        if (g == null) {
            g = new float[]{!city.cfg.nature() ? 0 : f.r >= 120 ? 2 : 1, 0};
            garage.put(f, g);
        }
        return g;
    }

    /** How many of army base f's are in, ready to go. */
    int ready(City.Facility f) {
        if (f.kind != City.FACILITY_BASE || f.fallen) return 0;
        float[] g = garage(f);
        if (g[1] > w.time) return 0;
        return Math.max(0, (int) g[0] - out(f));
    }

    /** How many of base f's are out (wrecks count until cleared). */
    private int out(City.Facility f) {
        int n = 0;
        float rad = Math.max(260, f.r * 2.5f);
        for (int i = 0, m = fleet.vehicles.size(); i < m; i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (v.kind == K_FIRE_SUPPORT && World.hypot(v.homeX - f.x, v.homeY - f.y) < rad) n++;
        }
        return n;
    }

    /** Every few seconds: a base with one in and a big fight in reach, and none of its own out already, sends it. */
    void tick(float dt) {
        if ((lookCd -= dt) > 0) return;
        lookCd = 3;
        if (w.zombies < PACK) return;
        for (int k = 0, n = city.facilities.size(); k < n; k++) {
            City.Facility f = city.facilities.get(k);
            if (f.kind != City.FACILITY_BASE || ready(f) <= 0 || out(f) > 0) continue;
            float[] pk = w.fieldworks.safePack(f.x, f.y, LOOK, PACK, CLEAR, 0);
            // (A fight: people with guns at it - a pack nobody is fighting is the mortars' and the air's.)
            if (pk == null || w.spatial.armedNear(pk[0], pk[1], 450) == 0) continue;
            int units = fleet.unitCount;
            Vehicle v = fleet.commandTrucks.parkPost(f, K_FIRE_SUPPORT);
            // (Not one of the numbered units: the others' call numbers stay as they were.)
            fleet.unitCount = units;
            if (v == null) continue;
            v.number = 0;
            v.depot = f;
            v.cargo = SALVOS;
            if (!goOut(v, pk)) {
                v.removedFromFleet = true;
                fleet.vehicles.remove(v);
            }
        }
    }

    /** Its line on the card. */
    String status(Vehicle v, String where) {
        String who = "Fire support truck";
        if (v.broken) return who + ": Wrecked";
        switch (v.state) {
            case DRIVE: return who + ": Moving up behind the lines" + (where != null ? " to " + where : "");
            case SCENE: return who + ": " + (v.cannonCd > COOLDOWN - 4 ? "Firing!" : "Set up, " + v.cargo + " salvos left") + (where != null ? " at " + where : "");
            default: return who + ": Back to base to restock";
        }
    }

    /** One step out on the road; true when it's back in the garage. */
    boolean step(Vehicle v, float dt) {
        if (v.broken) return false;
        // (Stopped with no way home - or from a saved game: in.)
        if (v.state == WAIT) return inside(v);
        v.checkCd -= dt;
        if (v.state == SCENE) return firing(v, dt);
        // Driving out, or home.
        v.stuckTimer += dt;
        if (v.checkCd <= 0) {
            v.checkCd = 0.5f;
            float hx = (float) Math.cos(v.angle), hy = (float) Math.sin(v.angle);
            int ahead = w.countZombiesNear(v.x + hx * 80, v.y + hy * 80, 70) + w.countZombiesNear(v.x, v.y, 50);
            // (The dead on the road ahead: on the way out it sets up here if it can reach from here, else turns for home.)
            if (ahead >= 2 && v.state == DRIVE) {
                if (World.hypot(v.aimX - v.x, v.aimY - v.y) < REACH && w.countZombiesNear(v.x, v.y, 120) < 3) setUp(v);
                else goHome(v);
                return false;
            }
        }
        boolean moving = fleet.driving.driveStep(v, dt, 80, fleet.driving.carAhead(v) ? 0.5f : 1f);
        if (moving) fleet.driving.hit(v, w.runOver(v.x, v.y, 8, v.speed, v.angle));
        float d = (float) World.hypot(v.tx - v.x, v.ty - v.y);
        boolean there = d < 36 || (!moving && d < 120) || (v.stuckTimer > 20 && d < 250);
        if (there) {
            if (v.state == DRIVE) setUp(v);
            else return inside(v);
            return false;
        }
        if (moving && Math.abs(v.speed) > 12) v.stuckTimer = 0;
        else if (v.stuckTimer > 8) {
            v.stuckTimer = 0;
            if (!fleet.routing.route(v, v.tx, v.ty)) {
                if (v.state == DRIVE) setUp(v);
                else return inside(v);
            }
        }
        return false;
    }

    /** Off to the fight at the pack pk: to the spot behind the lines it fires from. False with no way there. */
    private boolean goOut(Vehicle v, float[] pk) {
        float[] spot = behind(pk[0], pk[1], v.homeX, v.homeY);
        if (spot == null) return false;
        v.aimX = pk[0];
        v.aimY = pk[1];
        v.tx = spot[0];
        v.ty = spot[1];
        v.place = city.placeName(spot[0], spot[1]);
        v.stuckTimer = 0;
        v.state = DRIVE;
        if (!fleet.routing.route(v, spot[0], spot[1])) return false;
        w.dispatch.say(Dispatch.WHO_MILITARY, null, (v.depot != null ? v.depot.name : "Base") + ": Fire support truck moving up to "
                + v.place + ". Mortar rounds on " + city.placeName(pk[0], pk[1]) + " shortly.", v.x, v.y);
        return true;
    }

    /** Back in its base's garage (off the map: true); the next one ready once it's restocked. */
    private boolean inside(Vehicle v) {
        City.Facility f = v.depot != null ? v.depot : fleet.armyUnits.yardOf(v.homeX, v.homeY);
        if (f != null) garage(f)[1] = w.time + RESTOCK;
        return true;
    }

    /** A road spot BACK or so from the pack (x, y) on the side towards (hx, hy), with none of the dead near it. */
    private float[] behind(float x, float y, float hx, float hy) {
        float dx = hx - x, dy = hy - y, dl = (float) Math.sqrt(dx * dx + dy * dy);
        if (dl < 1) return null;
        dx /= dl;
        dy /= dl;
        for (float back = Math.min(BACK, dl); back <= Math.min(REACH - 50, dl); back += 100) {
            float[] g = city.nearestDrivable(x + dx * back, y + dy * back);
            if (g == null || World.hypot(g[0] - x, g[1] - y) < 220 || World.hypot(g[0] - x, g[1] - y) > REACH - 30) continue;
            if (w.countZombiesNear(g[0], g[1], 160) == 0) return g;
        }
        // (Not far out: it fires from by the gate.)
        return dl < REACH - 30 ? new float[]{hx, hy} : null;
    }

    private void setUp(Vehicle v) {
        v.speed = 0;
        v.stuckTimer = 0;
        v.state = SCENE;
        v.timer = 0;
        v.cannonCd = 4;
        v.place = city.placeName(v.x, v.y);
    }

    private void goHome(Vehicle v) {
        v.stuckTimer = 0;
        v.tx = v.homeX;
        v.ty = v.homeY;
        v.state = RETURN;
        // (No way home by road: it's taken in where it is, next step.)
        if (!fleet.routing.route(v, v.homeX, v.homeY)) v.state = WAIT;
    }

    /** Set up: a salvo on the biggest safe pack in reach now and then; home when the dead come close, it's out, or there's nothing. */
    private boolean firing(Vehicle v, float dt) {
        v.speed = 0;
        v.timer += dt;
        v.cannonCd -= dt;
        if (v.checkCd > 0) return false;
        v.checkCd = 1;
        // The dead closing on it, rounds gone, or nothing to fire at for a while: pack up.
        if (w.countZombiesNear(v.x, v.y, 110) >= 2 || v.cargo <= 0 || v.timer > 45) {
            goHome(v);
            return false;
        }
        if (v.cannonCd > 0) return false;
        // (Not on top of itself: 140 out at the least. What's left of the pack once it's there counts: the fight goes
        // on while it drives.)
        float[] pk = w.fieldworks.safePack(v.x, v.y, REACH, PACK_HERE, CLEAR, 140);
        if (pk == null) return false;
        w.combat.shells(pk[0], pk[1], World.FIRE_DELAY);
        w.emit(Sfx.LAUNCHER, v.x, v.y);
        v.turret = (float) Math.atan2(pk[1] - v.y, pk[0] - v.x);
        v.cannonCd = COOLDOWN;
        v.timer = 0;
        v.cargo--;
        salvos++;
        w.fireMissions++;
        // (Only the first salvo of a set-up goes out on the radio.)
        if (v.cargo == SALVOS - 1)
            w.dispatch.say(Dispatch.WHO_MILITARY, null, "Fire support: rounds out on " + city.placeName(pk[0], pk[1]) + ", " + (int) pk[2]
                    + " of the dead there. Keep clear!", pk[0], pk[1]);
        return false;
    }
}
