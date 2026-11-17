package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Rooftop overwatch: marksmen and snipers up on roofs beside a fight.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class Overwatch {
    private final World w;

    Overwatch(World w) {
        this.w = w;
    }

    /** Down off the roof (or giving up on the way to it). */
    void comeDown(Entity e) {
        e.roof = null;
        e.onTower = false;
        e.roofCd = 12;
        w.overwatch.remove(e);
    }

    /** A building with a roof worth taking by a call at (x, y): three floors or more, standing, within reach. */
    City.Building roofBy(float x, float y) {
        City.Building best = null;
        float bd = World.ROOF_FIND;
        int tx = (int) (x / City.T), ty = (int) (y / City.T), r = (int) (World.ROOF_FIND / City.T) + 1;
        City.Building last = null;
        for (int j = Math.max(0, ty - r); j <= Math.min(w.city.h - 1, ty + r); j += 2)
            for (int i = Math.max(0, tx - r); i <= Math.min(w.city.w - 1, tx + r); i += 2) {
                int bi = w.city.buildingAt.get(j * w.city.w + i);
                if (bi < 0 || bi >= w.city.buildings.size()) continue;
                City.Building b = w.city.buildings.get(bi);
                if (b == last || b.collapsed || b.height < World.ROOF_HEIGHT || (b.maxHp > 0 && b.hp < b.maxHp * 0.4f)) continue;
                last = b;
                float cx = Math.max(b.x0, Math.min(b.x1, x)), cy = Math.max(b.y0, Math.min(b.y1, y));
                float d = (float) World.hypot(cx - x, cy - y);
                // (Not the one the fight is inside, or right up against.)
                if (d < 18 || d >= bd) continue;
                bd = d;
                best = b;
            }
        return best;
    }

    /**
     * A police marksman or an army sniper sent to a call takes the roof of a tall building beside it: up at the foot
     * of its wall nearest the fight, then on the roof (onTower, as up a base's watchtower) - seeing and shooting half
     * as far again, out of the dead's reach - until the call's over, the building's coming down, or they're hurt or
     * out of rounds. True while it has them busy.
     */
    boolean overwatchStep(Entity e, float range, float dt) {
        if (e.roofCd > 0) e.roofCd -= dt;
        Dispatch.Incident inc = e.incident;
        boolean call = e.task == Dispatch.T_RESPOND && inc != null && !inc.resolved;
        if (e.roof == null) {
            if (!call || e.roofCd > 0 || e.ride != null || e.rig != null || e.infected) return false;
            e.roofCd = 4;
            if (World.hypot(inc.x - e.x, inc.y - e.y) > 330) return false;
            City.Building b = roofBy(inc.x, inc.y);
            if (b == null) return false;
            float cx = Math.max(b.x0, Math.min(b.x1, inc.x)), cy = Math.max(b.y0, Math.min(b.y1, inc.y));
            float ox = inc.x - cx, oy = inc.y - cy, ol = (float) Math.sqrt(ox * ox + oy * oy) + 0.001f;
            float[] g = w.city.findWalkable(cx + ox / ol * 9, cy + oy / ol * 9);
            if (g == null || World.hypot(g[0] - cx, g[1] - cy) > 40) return false;
            e.roof = b;
            e.roofGX = g[0];
            e.roofGY = g[1];
            // (On the roof: a little in from the edge that looks down on the fight, each a step along from the last.)
            float in = 5 + (e.nameSeed & 3) * 3, along = ((e.nameSeed >> 2 & 3) - 1.5f) * 5;
            e.roofX = Math.max(b.x0 + 4, Math.min(b.x1 - 4, cx - ox / ol * in + oy / ol * along));
            e.roofY = Math.max(b.y0 + 4, Math.min(b.y1 - 4, cy - oy / ol * in - ox / ol * along));
        }
        City.Building b = e.roof;
        // (Taken somewhere else since - a lift, an order: no longer up there.)
        if (e.onTower && World.hypot(e.x - e.roofGX, e.y - e.roofGY) > 34) call = false;
        if (!call || b.collapsed || (b.maxHp > 0 && b.hp < b.maxHp * 0.4f) || e.infected || e.hp < e.maxHp * 0.35f
                || (e.ammo <= 0 && e.reserve <= 0) || World.hypot(inc.x - e.roofGX, inc.y - e.roofGY) > World.ROOF_FIND + 80) {
            comeDown(e);
            return false;
        }
        if (!e.onTower) {
            // (The dead on them before they're up: that first.)
            if (w.spatial.nearest(e, 45, true, false) != null) return false;
            float d = (float) World.hypot(e.roofGX - e.x, e.roofGY - e.y);
            if (d > 9 && !(e.blocked && d < 26)) {
                e.aiming = false;
                w.wilds.walkToSpot(e, e.roofGX, e.roofGY, e.runSpeed);
                return true;
            }
            e.onTower = true;
            e.onScene = true;
            if (!w.overwatch.contains(e)) w.overwatch.add(e);
            w.roofsTaken++;
            w.dispatch.chatter(e.type == Entity.SOLDIER ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, e,
                    "On the roof at " + inc.place + ". I have overwatch.", e.x, e.y);
        }
        w.movement.steer(e, 0, 0, 0);
        e.fear = 5;
        Entity t = w.combat.pickTarget(e, range * 1.5f);
        if (t != null) w.combat.aimAndFire(e, t, (float) World.hypot(t.x - e.x, t.y - e.y), range * 1.5f, dt);
        else {
            e.aiming = false;
            e.angle = (float) Math.atan2(inc.y - e.y, inc.x - e.x);
        }
        return true;
    }
}
