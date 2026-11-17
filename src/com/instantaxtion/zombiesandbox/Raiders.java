package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Raider gangs.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class Raiders {
    private final World w;

    Raiders(World w) {
        this.w = w;
    }

    /**
     * (14.2) Once a second: a long outbreak turns some survivors bad. After six minutes, now and then (three at most a
     * game) an armed civilian and a few armed people near them form a gang that goes from shop to shop taking
     * everything, and shoots anyone there for the same. They're on their own once it's over.
     */
    void raiders() {
        int active = 0;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (!e.raider) continue;
            if (!w.outbreak || e.isZombie()) {
                // (It's over, or they've turned: no gang any more.)
                e.raider = false;
                e.raidShop = null;
                e.hostileT = 0;
                continue;
            }
            if (!e.dead) active++;
        }
        w.raidersActive = active;
        if (!w.outbreak || w.outbreakTime < 360 || w.gangsFormed >= World.MAX_GANGS || w.rnd.nextFloat() > 0.012f) return;
        Entity boss = null;
        for (int tries = 0; tries < 300 && boss == null; tries++) {
            Entity e = w.entities.get(w.rnd.nextInt(w.entities.size()));
            if (mayRaid(e)) boss = e;
        }
        if (boss == null) return;
        java.util.ArrayList<Entity> gang = new java.util.ArrayList<Entity>();
        for (int i = 0, n = w.spatial.gatherLiving(boss.x, boss.y, 220); i < n && gang.size() < 4; i++) {
            Entity o = w.gathered[i];
            if (o != boss && mayRaid(o)) gang.add(o);
        }
        if (gang.size() < 2) return;
        boss.raider = true;
        boss.leader = null;
        for (Entity o : gang) {
            o.raider = true;
            o.leader = boss;
        }
        w.gangsFormed++;
        String where = w.city.placeName(boss.x, boss.y);
        w.dispatch.say(Dispatch.WHO_INFO, null, "Info: An armed gang is looting the shops near " + where + ". Keep away from them.", boss.x, boss.y);
        w.stats.timeline("Looters on " + where, boss.x, boss.y);
    }

    /** Could join a raider gang: an armed civilian on their own, out on the street, with nothing else on. */
    boolean mayRaid(Entity e) {
        return !e.dead && e.type == Entity.CIVILIAN && e.hasGun && !e.raider && e.ammo + e.reserve > 0 && e != w.controlled
                && (e.leader == null || !e.leader.raider) && e.task == Dispatch.T_NONE && e.zone == null && !e.militia
                && e.holdout == null && e.building == null && !e.infected;
    }

    /**
     * (14.2) A raider's turn: the dead close by come first (the usual way: they're armed survivors too). Otherwise the
     * leader makes for the nearest shop with anything left and strips it, and the gang follows; at a shop, anyone else
     * there with a gun or after the same is shot at. True while busy with that.
     */
    boolean raiderStep(Entity e, float dt) {
        if (e.hostileT > 0) e.hostileT -= dt;
        if (w.spatial.nearest(e, 80, true, false) != null || e == w.controlled) return false;
        Entity boss = e.leader != null && e.leader.raider && !e.leader.dead ? e.leader : e;
        if (boss != e && e.leader != null && !e.leader.raider) e.leader = null;
        City.Building shop = boss.raidShop;
        // Anyone else at the shop, after the same: shot at.
        if (shop != null && World.hypot(shop.doorX - e.x, shop.doorY - e.y) < 160 && (e.ammo > 0 || e.reserve > 0)) {
            Entity v = victimNear(e);
            if (v != null) {
                float d = (float) World.hypot(v.x - e.x, v.y - e.y);
                if (e.hostileT <= 0 && boss.hostileT <= 0)
                    w.dispatch.say(Dispatch.WHO_911, null, "911: \"There's a gang shooting people at " + shop.name + "! They're taking everything!\"", v.x, v.y);
                e.hostileT = World.HOSTILE_TIME;
                w.combat.aimAndFire(e, v, d, World.RAID_REACH + 20, dt);
                return true;
            }
        }
        if (boss != e) {
            // Keeping up with the leader.
            float d = (float) World.hypot(boss.x - e.x, boss.y - e.y);
            if (d > 28) w.wilds.walkToSpot(e, boss.x + (e.nameSeed % 9), boss.y + (e.nameSeed / 9 % 9), e.runSpeed * 0.9f);
            else w.movement.steer(e, 0, 0, 0);
            return true;
        }
        if (shop == null || shop.collapsed || shop.stock <= 0) {
            shop = e.raidShop = raidTarget(e.x, e.y);
            if (shop == null) return false;
        }
        float dx = shop.doorX - e.x, dy = shop.doorY - e.y, d = (float) Math.sqrt(dx * dx + dy * dy);
        if (d > 12) {
            w.wilds.walkToSpot(e, shop.doorX, shop.doorY, e.runSpeed * 0.85f);
            return true;
        }
        // Stripped: everything on the shelves, rounds for the whole gang.
        shop.stock = 0;
        for (int i = 0, n = w.spatial.gatherLiving(e.x, e.y, 120); i < n; i++) {
            Entity o = w.gathered[i];
            if (o.raider && (o == e || o.leader == e) && o.hasGun) o.reserve = Math.max(o.reserve, 90);
        }
        w.dispatch.say(Dispatch.WHO_INFO, null, "Info: Looters have stripped " + shop.name + " bare.", shop.doorX, shop.doorY);
        e.raidShop = null;
        return true;
    }

    /** The nearest shop with something left to take: a supermarket or a pharmacy. */
    City.Building raidTarget(float x, float y) {
        City.Building best = null;
        float bd = 2200;
        for (int i = 0, n = w.city.buildings.size(); i < n; i++) {
            City.Building b = w.city.buildings.get(i);
            if (b.collapsed || b.stock <= 0 || b.name == null) continue;
            if (b.kind != City.MARKET && b.kind != City.PHARMACY) continue;
            float d = (float) World.hypot(b.doorX - x, b.doorY - y);
            if (d < bd) {
                bd = d;
                best = b;
            }
        }
        return best;
    }

    /** Someone at the shop a raider would shoot: anyone else armed, or on a supply run, close by and in sight. */
    Entity victimNear(Entity e) {
        Entity best = null;
        float bd = World.RAID_REACH;
        for (int i = 0, n = w.spatial.gatherLiving(e.x, e.y, World.RAID_REACH); i < n; i++) {
            Entity o = w.gathered[i];
            if (o == e || o.dead || o.raider || o.isZombie() || o.type == Entity.DOG) continue;
            if (!o.isArmed() && !o.hasGun && o.task != Dispatch.T_LOOT) continue;
            float d = (float) World.hypot(o.x - e.x, o.y - e.y);
            if (d < bd && w.city.los(e.x, e.y, o.x, o.y)) {
                bd = d;
                best = o;
            }
        }
        return best;
    }
}
