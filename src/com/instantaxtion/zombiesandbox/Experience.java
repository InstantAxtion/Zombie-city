package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Experience: kills, levels, perks, bands of veterans.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class Experience {
    private final World w;

    Experience(World w) {
        this.w = w;
    }

    /** (10.59) A kill made: up a rank at the thresholds, a little tougher, and it gets said. */
    /**
     * ((15.1) A kill that saved a life: one of the dead put down with someone else alive right beside it, who it was
     * about to bite.)
     */
    void killMade(Entity e, Entity z) {
        killMade(e);
        if (e.isZombie() || z == null) return;
        // (Its own look at the grid, not gatherLiving's list: a kill can come in the middle of a loop over that.)
        int cx0 = Math.max(0, (int) ((z.x - 24) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((z.x + 24) / World.CELL));
        int cy0 = Math.max(0, (int) ((z.y - 24) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((z.y + 24) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c] + w.zCount[c], end = w.cellStart[c] + w.cellCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o == e || o == z || o.dead || o.isZombie() || o.type == Entity.DOG) continue;
                    float reach = z.radius + o.radius + 14; // (in reach of it, or just shoved off by it)
                    if ((o.x - z.x) * (o.x - z.x) + (o.y - z.y) * (o.y - z.y) > reach * reach) continue;
                    e.saved++;
                    gainXp(e, 20);
                    return;
                }
            }
    }

    /** Experience earned: a level at each threshold, each bringing the next perk for their kind. */
    void gainXp(Entity e, float amount) {
        if (e == null || e.dead || e.isUndead() || e.type == Entity.DOG) return;
        e.xp += amount;
        String rankWas = Names.rank(e);
        boolean heroWas = e.hero();
        while (e.level + 1 < World.LEVEL_XP.length && e.xp >= World.LEVEL_XP[e.level + 1]) {
            e.level++;
            int[] order = e.role == Entity.ROLE_CORPSMAN ? World.PERKS_MEDIC : e.type == Entity.SOLDIER ? World.PERKS_SOLDIER
                    : e.type == Entity.COP ? World.PERKS_COP : World.PERKS_CIVILIAN;
            // (Not all alike: each starts a little way along their kind's list.)
            int start = (e.nameSeed & 0x7FFFFFFF) % 2;
            int p = order[(e.level - 1 + start) % order.length];
            if (World.perk(e, p)) continue;
            e.perks |= p;
            if (p == World.P_TOUGH) {
                e.maxHp *= 1.2f;
                e.hp = Math.min(e.maxHp, e.hp + e.maxHp * 0.2f);
            } else if (p == World.P_FLEET) {
                e.speed *= 1.1f;
                e.runSpeed *= 1.1f;
            }
            if (e.level >= 2 && (e.type == Entity.COP || e.type == Entity.SOLDIER || e.canShoot()))
                w.stats.highlight(Names.person(e.nameSeed) + " reached level " + (e.level + 1) + ": " + World.perkName(p), e.x, e.y);
        }
        // ((26.0) A civilian good enough at it becomes a name the survivors know.)
        if (heroWas != e.hero() && e.hero()) {
            w.stats.highlight(Names.hero(e.nameSeed) + " is a survivor hero", e.x, e.y);
            w.dispatch.say(Dispatch.WHO_INFO, null, "News: Survivors are talking about " + Names.hero(e.nameSeed) + ", who has saved "
                    + e.saved + (e.saved == 1 ? " life" : " lives") + " and put down " + e.kills + " of the dead.", e.x, e.y);
        }
        // ((25.0) And up a rank for it: announced, and on the radio.)
        String rankNow = Names.rank(e);
        if (rankWas != null && rankNow != null && !rankWas.equals(rankNow)) {
            w.stats.highlight(Names.person(e.nameSeed) + " promoted to " + rankNow, e.x, e.y);
            w.dispatch.chatter(e.type == Entity.SOLDIER ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, e,
                    "Field promotion: " + rankNow + " " + Names.person(e.nameSeed) + ".", e.x, e.y);
        }
    }

    /**
     * ((23.0) Once a second, with the dead about: survivor bands and escorts. Someone with a gun - a civilian who
     * has one, or a police officer or soldier with nothing more pressing - gathers up to four frightened people
     * nearby, who stick with them.
     */
    void bands() {
        if (!w.outbreak) return;
        for (int i = 0, n = w.entities.size(); i < n; i++) w.entities.get(i).bandN = 0;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity f = w.entities.get(i);
            if (f.leader == null || f.dead) continue;
            // (An officer sent somewhere else lets their people go rather than take them along.)
            Entity l = f.leader;
            if ((l.type == Entity.COP || l.type == Entity.SOLDIER) && f.type == Entity.CIVILIAN
                    && (l.dead || l.rig != null || (l.task != Dispatch.T_NONE && l.task != Dispatch.T_PATROL))) {
                f.leader = null;
                continue;
            }
            l.bandN++;
        }
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity l = w.entities.get(i);
            int cap = l.hero() ? 8 : 4;
            if (l.dead || l.removed || l == w.controlled || l.lodZ > 25 || l.bandN >= cap) continue;
            boolean civ = l.type == Entity.CIVILIAN && l.canShoot() && l.leader == null;
            boolean officer = (l.type == Entity.COP || l.type == Entity.SOLDIER) && (l.task == Dispatch.T_NONE || l.task == Dispatch.T_PATROL)
                    && !l.aiming && l.rig == null;
            if (!civ && !officer) continue;
            int cx0 = Math.max(0, (int) ((l.x - 70) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((l.x + 70) / World.CELL));
            int cy0 = Math.max(0, (int) ((l.y - 70) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((l.y + 70) / World.CELL));
            for (int cy = cy0; cy <= cy1 && l.bandN < cap; cy++)
                for (int cx = cx0; cx <= cx1 && l.bandN < cap; cx++) {
                    int c = cy * w.gw + cx;
                    for (int k = w.cellStart[c] + w.zCount[c], end = w.cellStart[c] + w.cellCount[c]; k < end && l.bandN < cap; k++) {
                        Entity o = w.sorted[k];
                        if (o == l || o.dead || o.removed || o.type != Entity.CIVILIAN || o.leader != null || o.canShoot() || o.infected || o.base != null
                                || o == w.controlled || o.bandN > 0 || o.task != Dispatch.T_NONE || o.fleeTimer <= 0) continue;
                        if ((o.x - l.x) * (o.x - l.x) + (o.y - l.y) * (o.y - l.y) > 70 * 70) continue;
                        o.leader = l;
                        l.bandN++;
                        w.bandsFormed++;
                    }
                }
        }
    }

    Entity[] topUnits() {
        if (w.time - w.topAt > 2 || w.time < w.topAt) {
            w.topAt = w.time;
            java.util.Arrays.fill(w.topUnits, null);
            for (int i = 0, n = w.entities.size(); i < n; i++) {
                Entity e = w.entities.get(i);
                if (e.dead || e.isUndead() || e.xp < 1) continue;
                for (int k = 0; k < 3; k++) {
                    if (w.topUnits[k] == null || e.xp > w.topUnits[k].xp) {
                        for (int j = 2; j > k; j--) w.topUnits[j] = w.topUnits[j - 1];
                        w.topUnits[k] = e;
                        break;
                    }
                }
            }
        }
        return w.topUnits;
    }

    /** Once a second: time under fire counts, and field medics tend to those round them. */
    void veterans() {
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || e.removed || e.isUndead() || e.type == Entity.DOG) continue;
            boolean fighter = e.type == Entity.COP || e.type == Entity.SOLDIER || e.canShoot();
            if (fighter && e.lodZ < 12) gainXp(e, 0.5f);
            if (World.perk(e, World.P_MEDIC)) {
                int cx0 = Math.max(0, (int) ((e.x - 32) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((e.x + 32) / World.CELL));
                int cy0 = Math.max(0, (int) ((e.y - 32) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((e.y + 32) / World.CELL));
                for (int cy = cy0; cy <= cy1; cy++)
                    for (int cx = cx0; cx <= cx1; cx++) {
                        int c = cy * w.gw + cx;
                        for (int k = w.cellStart[c] + w.zCount[c], end = w.cellStart[c] + w.cellCount[c]; k < end; k++) {
                            Entity o = w.sorted[k];
                            if (o == e || o.dead || o.hp >= o.maxHp || o.type == Entity.DOG) continue;
                            if ((o.x - e.x) * (o.x - e.x) + (o.y - e.y) * (o.y - e.y) > 32 * 32) continue;
                            o.hp = Math.min(o.maxHp, o.hp + 2);
                        }
                    }
            }
        }
    }

    void killMade(Entity e) {
        int before = World.rank(e);
        e.kills++;
        if (!e.isZombie()) gainXp(e, 10);
        if (!e.isZombie()) w.killsBy[e.type == Entity.COP ? 0 : e.type == Entity.SOLDIER ? 1 : 2]++;
        int after = World.rank(e);
        if (after > before && !e.isZombie()) {
            e.maxHp *= 1.08f;
            e.hp = Math.min(e.maxHp, e.hp + e.maxHp * 0.25f);
            if (after >= 2) w.stats.highlight(Names.person(e.nameSeed) + " is now " + World.RANK_NAMES[after].toLowerCase(), e.x, e.y);
        }
    }
}
