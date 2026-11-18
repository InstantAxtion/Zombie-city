package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Taking control of someone: the player's own unit, its options and actions, and what it picks up.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class PlayerControl {
    private final World w;

    PlayerControl(World w) {
        this.w = w;
    }

    /** The player's hands: walk or run with the stick; the attack button shoots, bites or shoves. */
    void thinkControlled(Entity e, float dt) {
        e.task = Dispatch.T_NONE;
        e.building = null;
        e.errand = null;
        e.aiming = false;
        float mag = Math.min(1, (float) Math.sqrt(w.joyX * w.joyX + w.joyY * w.joyY));
        if (mag < 0.12f) w.movement.steer(e, 0, 0, 0);
        else {
            // Push the stick all the way to run.
            float speed = mag > 0.85f ? e.runSpeed : e.speed * (0.35f + 0.65f * mag / 0.85f);
            w.movement.steer(e, w.joyX / mag, w.joyY / mag, speed);
        }
        // Walk over something to pick it up.
        World.Pickup p = nearestPlayerPickup(e);
        if (p != null) takePickup(e, p);
        if (!w.ctrlAttack) return;
        if (e.isUndead()) {
            Entity t = w.spatial.nearest(e, e.radius + 14, false, false);
            if (t == null) return;
            float ddx = t.x - e.x, ddy = t.y - e.y, d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
            e.angle = (float) Math.atan2(ddy, ddx);
            if (d < e.radius + t.radius + 4 && e.biteCd <= 0) w.zombieAI.bite(e, t, ddx / d, ddy / d);
        } else if (e.canShoot() && e.ammo + e.reserve > 0) {
            Entity t = aimTarget(e, 210);
            w.ctrlTarget = t;
            if (t != null) {
                float d = (float) World.hypot(t.x - e.x, t.y - e.y);
                w.combat.aimAndFire(e, t, d, 210, dt);
            }
        } else {
            Entity z = w.spatial.nearestInReach(e);
            if (z == null && e.melee > 0) z = w.spatial.nearest(e, e.radius + 12, true, false);
            if (z != null && e.meleeCd <= 0) {
                if (e.melee > 0) w.combat.swing(e, z);
                else w.zombieAI.shove(e, z, e.type == Entity.COP || e.type == Entity.SOLDIER);
            }
        }
    }

    World.Pickup nearestPlayerPickup(Entity e) {
        if (e.isZombie() || e.type == Entity.DOG) return null;
        for (int i = 0, n = w.pickups.size(); i < n; i++) {
            World.Pickup p = w.pickups.get(i);
            if (p.drop > 0 || p.lockUntil > w.time) continue;
            if ((p.x - e.x) * (p.x - e.x) + (p.y - e.y) * (p.y - e.y) < 10 * 10) return p;
        }
        return null;
    }

    /** The player picks something up: ammo for the gun they have, or swaps for what's lying there. */
    void takePickup(Entity e, World.Pickup p) {
        if (p.uses > 0) {
            // A supply crate: ammo (and a gun if they have none).
            if (!e.canShoot()) w.combat.setGun(e, Entity.W_PISTOL, 30);
            else e.reserve += 30;
            p.lockUntil = w.time + 4;
            if (--p.uses <= 0) w.pickups.remove(p);
            w.say("Ammo from the crate");
            return;
        }
        w.pickups.remove(p);
        if (p.melee > 0) {
            if (e.melee > 0) w.combat.dropMelee(e, e.melee);
            e.melee = p.melee;
            w.say("Picked up " + (p.melee == Entity.M_AXE ? "an axe" : "a " + Entity.MELEE_NAMES[p.melee].toLowerCase()));
            return;
        }
        int kind = p.weapon == Entity.W_STD ? Entity.W_PISTOL : p.weapon;
        if (e.canShoot() && e.gunKind() == kind) {
            e.reserve += p.rounds;
            w.say("+" + p.rounds + " rounds");
            return;
        }
        if (e.canShoot() && e.ammo + e.reserve > 0) {
            World.Pickup old = new World.Pickup();
            old.x = e.x;
            old.y = e.y;
            old.rounds = e.ammo + e.reserve;
            old.weapon = e.gunKind();
            old.lockUntil = w.time + 2;
            w.pickups.add(old);
        }
        w.combat.setGun(e, kind, Math.max(p.rounds, World.magFor(kind)));
        w.say("Picked up a " + Entity.WEAPON_NAMES[kind].toLowerCase());
    }

    /** What the player can do right now, besides moving and attacking. */
    int ctrlOptions(int[] out) {
        Entity e = w.controlled;
        if (e == null) return 0;
        int n = 0;
        if (w.controlledCar != null) {
            out[n++] = World.CA_EXIT_CAR;
            return n;
        }
        if (e.isZombie() || e.type == Entity.DOG) return 0;
        if (carToEnter(e) != null) out[n++] = World.CA_ENTER_CAR;
        // ((14.7) An officer or soldier leads a squad of their own; anyone else rallies people.)
        out[n++] = e.isArmed() ? World.CA_SQUAD : World.CA_RALLY;
        if (e.grenades > 0 && e.grenadeCd <= 0) out[n++] = World.CA_GRENADE;
        return n;
    }

    /** ((22.0) Whether the player can take this vehicle over and drive (or fly) it. */
    boolean canDrive(Fleet.Vehicle v) {
        boolean heli = v.type == Fleet.HELI && v.state == Fleet.IDLE && v.alt <= 0;
        return !v.broken && !v.burnt && v.player == null && v.block == null && (heli || !Fleet.offRoad(v))
                && v.type != Fleet.TRAIN && v.type != Fleet.BOAT && v.type != Fleet.JET;
    }

    /** ((22.0) e, the one the player now is, gets into the driver's seat of v. */
    void driveInto(Entity e, Fleet.Vehicle v) {
        w.fleet.takeWheel(v, e);
        w.controlledCar = v;
        e.dead = true;
        e.removed = true;
    }

    Fleet.Vehicle carToEnter(Entity e) {
        for (int i = 0, n = w.fleet.vehicles.size(); i < n; i++) {
            Fleet.Vehicle v = w.fleet.vehicles.get(i);
            // ((14.7) A helicopter too, sitting on its pad.)
            boolean heli = v.type == Fleet.HELI && v.state == Fleet.IDLE && v.alt <= 0;
            if ((Fleet.offRoad(v) && !heli) || v.broken || v.player != null || v.block != null) continue;
            float r = v.length() * 0.7f + 10;
            if ((v.x - e.x) * (v.x - e.x) + (v.y - e.y) * (v.y - e.y) < r * r) return v;
        }
        return null;
    }

    void ctrlAction(int a) {
        Entity e = w.controlled;
        if (e == null) return;
        switch (a) {
            case World.CA_ENTER_CAR: {
                Fleet.Vehicle v = carToEnter(e);
                if (v == null) return;
                w.fleet.takeWheel(v, e);
                w.controlledCar = v;
                e.dead = true;
                e.removed = true;
                // The group piles in too.
                for (int i = 0, n = w.entities.size(); i < n && v.riders.size() < Fleet.SEATS; i++) {
                    Entity o = w.entities.get(i);
                    if (o.dead || (o.leader != e && o.commandedBy != e) || o.type == Entity.DOG) continue;
                    if ((o.x - v.x) * (o.x - v.x) + (o.y - v.y) * (o.y - v.y) > 60 * 60) continue;
                    o.dead = true;
                    o.removed = true;
                    v.riders.add(o);
                }
                break;
            }
            case World.CA_EXIT_CAR: {
                Fleet.Vehicle v = w.controlledCar;
                if (v == null) return;
                w.fleet.leaveWheel(v, e);
                w.controlledCar = null;
                break;
            }
            case World.CA_RALLY: {
                int have = 0;
                for (int i = 0, n = w.entities.size(); i < n; i++) if (w.entities.get(i).leader == e) have++;
                int got = 0;
                for (int i = 0, n = w.entities.size(); i < n && have + got < 8; i++) {
                    Entity o = w.entities.get(i);
                    if (o.dead || o == e || o.leader != null || o.isArmed() || o.isZombie()) continue;
                    if (o.type != Entity.CIVILIAN && o.type != Entity.DOG) continue;
                    if ((o.x - e.x) * (o.x - e.x) + (o.y - e.y) * (o.y - e.y) > 110 * 110) continue;
                    if (w.civilianAI.hasFollowers(o)) continue;
                    o.leader = e;
                    o.task = Dispatch.T_NONE;
                    o.errand = null;
                    got++;
                }
                w.say(got == 0 ? (have > 0 ? have + " with you" : "Nobody near enough to join you")
                        : (have + got) + (have + got == 1 ? " person is" : " people are") + " with you");
                break;
            }
            case World.CA_SQUAD: {
                // ((14.7) Gather a squad (officers or soldiers, whichever you are) - or, with one, send them back.)
                int had = 0;
                for (int i = 0, n = w.entities.size(); i < n; i++) {
                    Entity o = w.entities.get(i);
                    if (o.commandedBy != e) continue;
                    o.commandedBy = null;
                    if (o.task == Dispatch.T_SQUAD) o.task = Dispatch.T_NONE;
                    had++;
                }
                if (had > 0) {
                    w.say("Squad dismissed: back to their orders");
                    break;
                }
                int got = 0;
                for (int i = 0, n = w.spatial.gatherLiving(e.x, e.y, World.SQUAD_REACH); i < n && got < World.SQUAD_MAX; i++) {
                    Entity o = w.gathered[i];
                    if (o == e || o.dead || o.type != e.type || o.rig != null || o.ride != null || o.role == Entity.ROLE_PILOT) continue;
                    if (o.task != Dispatch.T_NONE && o.task != Dispatch.T_RESPOND && o.task != Dispatch.T_POST && o.task != Dispatch.T_HOLD) continue;
                    w.dispatch.releaseForResupply(o);
                    o.task = Dispatch.T_SQUAD;
                    o.commandedBy = e;
                    o.squadSlot = got++;
                }
                w.say(got == 0 ? "Nobody near enough to follow you" : got + (got == 1 ? " follows you" : " follow you") + ": they shoot at what you shoot at");
                break;
            }
            case World.CA_GRENADE: {
                if (e.grenades <= 0 || e.grenadeCd > 0) return;
                Entity t = aimTarget(e, 170);
                float face = e.want > 1 ? (float) Math.atan2(e.my, e.mx) : e.angle;
                float tx = t != null ? t.x : e.x + (float) Math.cos(face) * 90, ty = t != null ? t.y : e.y + (float) Math.sin(face) * 90;
                w.combat.throwGrenade(e, tx, ty);
                break;
            }
        }
    }

    /** The last person in the building who isn't the player's character. */
    Entity popOccupant(City.Building b) {
        for (int i = b.occupants.size() - 1; i >= 0; i--)
            if (b.occupants.get(i) != w.controlled) return b.occupants.remove(i);
        return null;
    }

    /** Auto-aim for the player: the zombie most in line with where they're facing, that they can see. */
    Entity aimTarget(Entity e, float range) {
        float face = e.want > 1 ? (float) Math.atan2(e.my, e.mx) : e.angle;
        float fx = (float) Math.cos(face), fy = (float) Math.sin(face);
        Entity best = null;
        float bestScore = Float.MAX_VALUE;
        int cx0 = Math.max(0, (int) ((e.x - range) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((e.x + range) / World.CELL));
        int cy0 = Math.max(0, (int) ((e.y - range) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((e.y + range) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c], end = w.cellStart[c] + w.zCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o.dead || o.hp <= 0 || !o.isZombie()) continue;
                    float ddx = o.x - e.x, ddy = o.y - e.y, d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
                    if (d > range) continue;
                    float dot = (ddx * fx + ddy * fy) / d;
                    float score = d * (1.6f - dot);
                    if (score < bestScore && w.city.los(e.x, e.y, o.x, o.y)) {
                        bestScore = score;
                        best = o;
                    }
                }
            }
        return best;
    }
}
