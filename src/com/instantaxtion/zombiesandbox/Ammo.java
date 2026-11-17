package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Ammunition running out: restocking, lending a magazine, scouting on with none.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class Ammo {
    private final World w;

    Ammo(World w) {
        this.w = w;
    }

    /**
     * ((22.0) Someone with plenty passes a magazine to a partner close by who's running low (a quarter of their
     * rounds or less), before they run dry - not only once they're out and have to come over for one.
     */
    void shareAmmo(Entity e) {
        int cx0 = Math.max(0, (int) ((e.x - 60) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((e.x + 60) / World.CELL));
        int cy0 = Math.max(0, (int) ((e.y - 60) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((e.y + 60) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c] + w.zCount[c], end = w.cellStart[c] + w.zCount[c] + w.aCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o == e || o.dead || o.type != e.type || !o.canShoot() || o.gunKind() != e.gunKind()) continue;
                    if (World.hypot(o.x - e.x, o.y - e.y) > 60) continue;
                    float theirs = (o.ammo + o.reserve) / (float) Math.max(1, o.magSize + World.fullReserve(o));
                    if (theirs > 0.25f) continue;
                    int give = Math.min(e.reserve - e.magSize, o.magSize);
                    if (give <= 0) return;
                    e.reserve -= give;
                    o.reserve += give;
                    o.outOfAmmoSaid = false;
                    w.magsShared++;
                    if (w.time > w.shareSaid) {
                        w.shareSaid = w.time + 30;
                        w.dispatch.chatter(e.type == Entity.SOLDIER ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, e,
                                Dispatch.name(o) + ", take a mag!", e.x, e.y);
                    }
                    return;
                }
            }
    }

    /** Takes a full load from an armoury, if it has any left. Returns false if it's empty. */
    boolean draw(City.Facility f, Entity e) {
        if (f.ammo <= 0) {
            if (!f.dryAnnounced) {
                f.dryAnnounced = true;
                w.dispatch.say(e.type == Entity.SOLDIER ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, null, f.name
                        + " has run out of ammunition.", f.x, f.y);
            }
            return false;
        }
        int take = Math.min(f.ammo, World.fullReserve(e) - e.reserve);
        f.ammo -= take;
        e.reserve += take;
        if (take > 0) w.history(e, "resupplied at " + f.name);
        return true;
    }

    /** The nearest armoury with rounds left that this one can draw from (their own service's first), or null. */
    City.Facility restockPoint(Entity e) {
        boolean soldier = e.type == Entity.SOLDIER;
        City.Facility best = null;
        float bd = Float.MAX_VALUE;
        for (City.Facility f : w.city.facilities) {
            if (f.ammo <= 0) continue;
            boolean own = f.kind == (soldier ? City.FACILITY_BASE : City.FACILITY_POLICE);
            if (!own && f.kind != (soldier ? City.FACILITY_POLICE : City.FACILITY_BASE)) continue;
            float d = (float) World.hypot(f.x - e.x, f.y - e.y) + (own ? 0 : 500);
            if (d < bd) {
                bd = d;
                best = f;
            }
        }
        City.Facility c = w.courtArmoury;
        if (!soldier && c != null && c.ammo > 0 && w.city.courthouse != null && !w.city.courthouse.collapsed
                && World.hypot(c.x - e.x, c.y - e.y) < bd) best = c;
        return best;
    }

    /** (10.60) Where this unit would go to restock (see Fleet's ammo runs), or null if there's nowhere with any. */
    City.Facility restockFor(Entity e) {
        return restockPoint(e);
    }

    /** (10.60) Everyone in this vehicle fills up from f. How many did. */
    int restockCrew(java.util.List<Entity> crew, City.Facility f) {
        int n = 0;
        for (int i = 0; i < crew.size(); i++) {
            Entity e = crew.get(i);
            if (!e.hasGun || !draw(f, e)) continue;
            int take = Math.min(e.magSize - e.ammo, e.reserve);
            e.ammo += take;
            e.reserve -= take;
            e.outOfAmmoSaid = false;
            n++;
        }
        return n;
    }

    /** Someone on the same side nearby with plenty of rounds to share. */
    Entity lenderFor(Entity e) {
        return lenderFor(e, 500);
    }

    /** (10.60) The same, no further than range. */
    Entity lenderFor(Entity e, float range) {
        Entity best = null;
        float bd = range * range;
        // ((13.3) Only those armed, from the grid, near them: it went through everyone in the city each time.)
        int cx0 = Math.max(0, (int) ((e.x - range) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((e.x + range) / World.CELL));
        int cy0 = Math.max(0, (int) ((e.y - range) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((e.y + range) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c] + w.zCount[c], end = k + w.aCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o == e || o.dead || o.removed || !o.isArmed() || o.scout || o.task == Dispatch.T_BORROW) continue;
                    if (!canLend(o)) continue;
                    // ((24.0) Rounds that fit their gun: a shotgun's officer took a rifleman's magazines.)
                    if (o.gunKind() != e.gunKind()) continue;
                    float d = (o.x - e.x) * (o.x - e.x) + (o.y - e.y) * (o.y - e.y);
                    if (d < bd) {
                        bd = d;
                        best = o;
                    }
                }
            }
        return best;
    }

    /**
     * ((13.3) Enough to share: two magazines in reserve, or a third of a full load. It used to take most of a full
     * load, so after the first few minutes of a fight hardly anyone could spare a round.)
     */
    boolean canLend(Entity o) {
        return o.reserve >= Math.max(1, o.magSize) && o.ammo + o.reserve >= Math.max(o.magSize * 2, World.fullReserve(o) / 3);
    }

    /**
     * No rounds and none to be had: they keep watch round their station or base, out of reach of the dead,
     * and radio in every group of them they see so the others know where they are.
     */
    void scout(Entity e, float dt) {
        e.aiming = false;
        Entity z = w.spatial.nearest(e, 220, true, true);
        if (z != null) {
            float dx = e.x - z.x, dy = e.y - z.y, d = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
            e.threatX = z.x;
            e.threatY = z.y;
            if (e.shareCd <= 0) {
                e.shareCd = 20;
                w.armedAI.shareSighting(e, z.x, z.y);
                int n = w.spatial.countZombiesNear(z.x, z.y, 120);
                w.dispatch.say(e.type == Entity.SOLDIER ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, e, "Eyes on " + n + " of them by "
                        + w.city.placeName(z.x, z.y) + ". I'm dry, can't engage.", z.x, z.y);
                w.dispatch.call(e, z);
            }
            if (d < 110) {
                w.movement.flee(e, dx / d, dy / d, e.runSpeed);
                return;
            }
            // Watching from a safe distance.
            w.movement.steer(e, 0, 0, 0);
            e.angle = World.turn(e.angle, (float) Math.atan2(-dy, -dx), dt * 4);
            return;
        }
        // A slow round of the streets near the station or base.
        City.Facility home = w.holdoutLife.supplyPoint(e);
        if (home == null) {
            w.movement.wander(e, e.speed * 0.6f);
            return;
        }
        float a = w.time * 0.05f + e.callsign * 1.7f + e.member;
        float r = home.r + 120 + (e.callsign % 3) * 40;
        float px = home.x + (float) Math.cos(a) * r, py = home.y + (float) Math.sin(a) * r;
        float[] p = w.city.solidAt(px, py) ? w.city.findWalkable(px, py) : new float[]{px, py};
        if (p == null) {
            w.movement.wander(e, e.speed * 0.6f);
            return;
        }
        w.wilds.walkToSpot(e, p[0], p[1], e.speed);
    }
}
