package com.instantaxtion.zombiesandbox;

import static com.instantaxtion.zombiesandbox.Fleet.*;

import java.util.ArrayList;

/**
 * ((27.3) Part of the vehicle code, out of Fleet.) The player at the wheel.
 */
final class FleetPlayer {
    final Fleet fleet;
    final World w;
    final City city;

    FleetPlayer(Fleet fleet) {
        this.fleet = fleet;
        this.w = fleet.w;
        this.city = fleet.city;
    }

    // ------------------------------------------------------------------ the player driving

    /** The player gets behind the wheel (anyone else in it stays aboard). */
    void takeWheel(Vehicle v, Entity e) {
        v.player = e;
        v.parked = false;
        v.pulling = false;
        v.rescue = false;
        // ((14.7) A helicopter: lifting off from its pad, yours to fly.)
        v.state = v.type == HELI ? HOVER : DRIVE;
        v.field = null;
        v.alarm = 0;
        e.ride = v;
        v.riders.add(e);
        w.emit(Sfx.ENGINE, v.x, v.y);
    }

    /** The player gets out; the car stays where it is, and anyone riding with them gets out too. */
    void leaveWheel(Vehicle v, Entity e) {
        v.player = null;
        v.speed = 0;
        if (v.type == HELI) {
            // ((14.7) Down the rope where you are; it flies itself back to its pad.)
            v.state = FLY_OUT;
            v.homeX = v.baseX;
            v.homeY = v.baseY;
        } else {
            v.parked = true;
            v.state = ABANDONED;
        }
        v.riders.remove(e);
        float sx = v.x + (float) Math.cos(v.angle + 1.57f) * 10, sy = v.y + (float) Math.sin(v.angle + 1.57f) * 10;
        float[] p = city.findWalkable(sx, sy);
        if (p == null) p = new float[]{v.x, v.y};
        e.x = p[0];
        e.y = p[1];
        e.vx = e.vy = 0;
        e.dead = false;
        e.removed = false;
        e.ride = null;
        w.entities.add(e);
        for (int i = 0; i < v.riders.size(); i++) w.release(v.riders.get(i), sx, sy);
        v.riders.clear();
    }

    /**
     * ((27.5) The player lets go of the one at the wheel (stops controlling them, or takes over someone else) while
     * driving: the vehicle goes back to the game with them still in it, instead of standing there for good with nobody
     * steering. A police car goes back on patrol with its officers, an army truck takes its soldiers home to base, an
     * everyday car rejoins the traffic. A helicopter or a tank is left the way getting out leaves it.
     */
    void handBack(Vehicle v, Entity e) {
        if (v.player != e) return;
        boolean police = e.type == Entity.COP && v.type == CRUISER, army = e.type == Entity.SOLDIER && v.type == TRUCK;
        if (v.type == HELI || v.type == TANK || v.broken || !(police || army || v.type == CAR)) {
            leaveWheel(v, e);
            return;
        }
        // (Left off the road - on the grass, the pavement: a car a lane or two off is back on it; one well off it is
        // left there, and they get out.)
        float[] lane = city.nearestDrivable(v.x, v.y);
        int t = city.tileIndex(v.x, v.y);
        boolean onRoad = t >= 0 && (city.tiles[t] == City.ROAD || city.tiles[t] == City.DIRT || city.tiles[t] == City.LOT || city.tiles[t] == City.BASE);
        if (!onRoad) {
            if (lane == null || World.hypot(lane[0] - v.x, lane[1] - v.y) > City.T * 3) {
                leaveWheel(v, e);
                return;
            }
            v.x = lane[0];
            v.y = lane[1];
        }
        v.player = null;
        v.parked = false;
        v.pulling = false;
        v.field = null;
        v.incident = null;
        v.stuckTimer = 0;
        if (v.type == CAR) {
            // (They drive on as everyday traffic, still aboard.)
            v.state = CRUISE;
            if (!fleet.routing.newDestination(v)) v.stuckTimer = 5;
            return;
        }
        // Police or army: they're its crew now (and the others of theirs riding with them); anyone else gets out.
        v.riders.remove(e);
        fleet.board(v, e);
        for (Entity o : new ArrayList<Entity>(v.riders)) {
            v.riders.remove(o);
            if (o.type == e.type) fleet.board(v, o);
            else w.release(o, v.x, v.y);
        }
        v.passengerType = e.type;
        if (police) {
            v.patrol = true;
            v.state = PATROL;
            if (!fleet.routing.newDestination(v)) v.stuckTimer = 99;
            return;
        }
        // (Home to base, where they get out and are sent on again; with no way there, they get out here.)
        v.squad.clear();
        v.pickup = true;
        if (!fleet.service.headHome(v)) {
            v.pickup = false;
            fleet.crewOut(v, true);
            v.parked = true;
            v.state = ABANDONED;
        }
    }

    /** Driving: the stick says where to go (pull back to reverse); the button sounds the horn, or fires the cannon. */
    boolean updatePlayerVehicle(Vehicle v, float dt) {
        Entity e = v.player;
        if (v.type == HELI && !v.broken) return fleet.helis.playerHeli(v, dt);
        if (v.broken) {
            // Wrecked: out you get.
            if (v.riders.contains(e)) leaveWheel(v, e);
            else v.player = null;
            return false;
        }
        float jx = w.joyX, jy = w.joyY, mag = Math.min(1, (float) Math.sqrt(jx * jx + jy * jy));
        float max = v.type == TANK ? 45 : v.type == CAR || v.type == CRUISER ? 125 : 95;
        float target = 0;
        if (mag > 0.15f) {
            float want = (float) Math.atan2(jy, jx);
            float diff = want - v.angle;
            while (diff > Math.PI) diff -= Math.PI * 2;
            while (diff < -Math.PI) diff += Math.PI * 2;
            float grip = Math.min(1, Math.abs(v.speed) / 30 + 0.35f) * (v.type == TANK ? 1.4f : 2.6f);
            if (Math.abs(diff) > 2.4f) {
                // Stick pulled back: reverse, steering the tail round.
                float rd = diff > 0 ? diff - (float) Math.PI : diff + (float) Math.PI;
                v.angle -= Math.max(-dt * grip, Math.min(dt * grip, rd));
                target = -max * 0.35f * mag;
            } else {
                v.angle += Math.max(-dt * grip, Math.min(dt * grip, diff));
                target = max * mag * (0.35f + 0.65f * Math.max(0, (float) Math.cos(diff)));
            }
        }
        if (v.hp < v.maxHp * 0.3f) target *= 0.6f;
        v.speed += (target - v.speed) * Math.min(1, dt * (Math.abs(target) < Math.abs(v.speed) ? 2.5f : 1.4f));
        float nx = v.x + (float) Math.cos(v.angle) * v.speed * dt, ny = v.y + (float) Math.sin(v.angle) * v.speed * dt;
        float r = v.type == TANK ? 8 : 5.5f;
        // (A car pulled over against the kerb may start out touching something: let it drive away.)
        // ((10.34) On a bridge it's over the highway's barrier, not up against it.)
        if (!city.circleBlocked(v.x, v.y, r) && city.circleBlocked(nx, ny, r) && !(fleet.driving.onBridge(v) && city.overpassAt(nx, ny) >= 0)) {
            if (Math.abs(v.speed) > 35 && v.crashCd <= 0) {
                v.crashCd = 0.5f;
                fleet.damage(v, Math.abs(v.speed) * (v.type == TANK ? 0.03f : 0.25f), false);
                w.emit(Sfx.CRASH, v.x, v.y);
                w.shake = Math.max(w.shake, 0.4f);
            }
            v.speed *= -0.25f;
        } else {
            v.x = nx;
            v.y = ny;
        }
        if (Math.abs(v.speed) > 8) w.runOver(v.x, v.y, v.length() * 0.5f + 2, Math.abs(v.speed), v.angle);
        e.x = v.x;
        e.y = v.y;
        if (v.soundCd <= 0 && Math.abs(v.speed) > 20) {
            v.soundCd = 1.2f;
            w.emit(Sfx.ENGINE, v.x, v.y);
        }
        v.cannonCd -= dt;
        if (w.ctrlAttack) {
            if (v.type == TANK) {
                Entity z = w.nearestVisibleZombie(v.x, v.y, 260);
                if (z != null) {
                    v.turret = turnTo(v.turret, (float) Math.atan2(z.y - v.y, z.x - v.x), dt * 2.5f);
                    if (v.cannonCd <= 0) {
                        v.cannonCd = 2.5f;
                        w.tankShell(v.x + (float) Math.cos(v.turret) * 14, v.y + (float) Math.sin(v.turret) * 14, z.x, z.y);
                    }
                }
            } else if (v.soundCd <= 0.6f) {
                // The horn: every zombie for streets around comes to see.
                v.soundCd = 1.2f;
                w.emit(v.type == CRUISER ? Sfx.SIREN : Sfx.HORN, v.x, v.y);
                w.noise(v.x, v.y, 260);
            }
        }
        return false;
    }
}
