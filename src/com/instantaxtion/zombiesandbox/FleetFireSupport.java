package com.instantaxtion.zombiesandbox;

import static com.instantaxtion.zombiesandbox.Fleet.*;

/**
 * ((27.5) Part of the vehicle code.) The army's fire support trucks: a mortar on the back of a truck, one or two parked
 * at each base. When there's a big fight in reach - a pack of 25 or more of the dead with people fighting them - one
 * drives out to a clear spot well behind the lines, sets up, and drops salvos (the same rounds as a fire mission:
 * Combat.shells) on the biggest pack with nobody living near it, a while between salvos; then it comes home to
 * restock. The dead getting close, it packs up and goes.
 */
final class FleetFireSupport {
    final Fleet fleet;
    final World w;
    final City city;
    /** Salvos fired, all told (for the tests and probes). */
    int salvos;

    static final float LOOK = 1600, REACH = 650, BACK = 320, COOLDOWN = 30, CLEAR = 150;
    static final int SALVOS = 6, PACK = 25;
    /** What it's doing (in timer's place while it's out: see step). */
    static final int SETUP = 3;

    FleetFireSupport(Fleet fleet) {
        this.fleet = fleet;
        this.w = fleet.w;
        this.city = fleet.city;
    }

    /** One at each army base with soldiers at it, two at the bigger ones (game start, and a game loaded). */
    void stock() {
        if (!city.cfg.nature()) return;
        for (City.Facility f : city.facilities) {
            if (f.kind != City.FACILITY_BASE || f.fallen) continue;
            float rad = Math.max(260, f.r * 2.5f);
            int soldiers = 0, have = 0;
            for (int i = 0, n = w.entities.size(); i < n; i++) {
                Entity e = w.entities.get(i);
                if (!e.dead && e.type == Entity.SOLDIER && World.hypot(e.x - f.x, e.y - f.y) < rad) soldiers++;
            }
            if (soldiers == 0) continue;
            for (int i = 0, n = fleet.vehicles.size(); i < n; i++) {
                Vehicle v = fleet.vehicles.get(i);
                if (v.kind == K_FIRE_SUPPORT && World.hypot(v.homeX - f.x, v.homeY - f.y) < rad) have++;
            }
            for (int k = have, want = soldiers >= 40 ? 2 : 1; k < want; k++) {
                Vehicle v = fleet.commandTrucks.parkPost(f, K_FIRE_SUPPORT);
                if (v == null) break;
                v.depot = f;
                v.passengerType = Entity.SOLDIER;
                v.cargo = SALVOS;
            }
        }
    }

    /** Its line on the card. */
    String status(Vehicle v, String where) {
        String who = "Fire support truck";
        if (v.broken) return who + ": Wrecked";
        switch (v.state) {
            case WAIT: return who + ": Ready at " + (v.depot != null ? v.depot.name : "base") + ", " + v.cargo + " salvos";
            case DRIVE: return who + ": Moving up behind the lines" + (where != null ? " to " + where : "");
            case SCENE: return who + ": " + (v.cannonCd > COOLDOWN - 4 ? "Firing!" : "Set up, " + v.cargo + " salvos left") + (where != null ? " at " + where : "");
            default: return who + ": Back to base to restock";
        }
    }

    boolean step(Vehicle v, float dt) {
        if (v.broken) return false;
        v.checkCd -= dt;
        if (v.state == WAIT) {
            v.speed = 0;
            // (Restocked at home.)
            if (v.cargo < SALVOS && (v.timer += dt) > 20) {
                v.cargo = SALVOS;
                v.timer = 0;
            }
            if (v.checkCd > 0 || v.cargo <= 0 || !w.outbreak) return false;
            v.checkCd = 3 + (v.number & 3) * 0.25f;
            goOut(v);
            return false;
        }
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
            else {
                fleet.commandTrucks.parkHome(v, v.depot);
                v.timer = 0;
            }
            return false;
        }
        if (moving && Math.abs(v.speed) > 12) v.stuckTimer = 0;
        else if (v.stuckTimer > 8) {
            v.stuckTimer = 0;
            if (!fleet.routing.route(v, v.tx, v.ty)) {
                if (v.state == DRIVE) setUp(v);
                else fleet.commandTrucks.parkHome(v, v.depot);
            }
        }
        return false;
    }

    /** A big fight in reach of home: the spot behind the lines it fires from, and off it goes. */
    private void goOut(Vehicle v) {
        float[] pk = w.fieldworks.safePack(v.homeX, v.homeY, LOOK, PACK, CLEAR, 0);
        // (A fight: people with guns at it - a pack nobody is fighting is the mortars' and the air's.)
        if (pk == null || w.spatial.armedNear(pk[0], pk[1], 450) == 0) return;
        float[] spot = behind(pk[0], pk[1], v.homeX, v.homeY);
        if (spot == null) return;
        v.aimX = pk[0];
        v.aimY = pk[1];
        v.tx = spot[0];
        v.ty = spot[1];
        v.place = city.placeName(spot[0], spot[1]);
        v.stuckTimer = 0;
        v.state = DRIVE;
        if (!fleet.routing.route(v, spot[0], spot[1])) {
            v.state = WAIT;
            return;
        }
        w.dispatch.say(Dispatch.WHO_MILITARY, null, (v.depot != null ? v.depot.name : "Base") + ": Fire support truck moving up to "
                + v.place + ". Mortar rounds on " + city.placeName(pk[0], pk[1]) + " shortly.", v.x, v.y);
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
        // (Not far out: it fires from home.)
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
        if (!fleet.routing.route(v, v.homeX, v.homeY)) fleet.commandTrucks.parkHome(v, v.depot);
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
        // (Not on top of itself: 140 out at the least.)
        float[] pk = w.fieldworks.safePack(v.x, v.y, REACH, PACK, CLEAR, 140);
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
