package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * The sandbox's tools: barricades, crates, medkits, hordes, panic, clearing things away.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class Sandbox {
    private final World w;

    Sandbox(World w) {
        this.w = w;
    }

    boolean pickupStep(Entity e, float threatDist) {
        World.Pickup p = e.pickup;
        if (e.task != Dispatch.T_PICKUP) return false;
        if (p == null || !w.pickups.contains(p) || threatDist < 40) {
            w.civilianAI.dropTask(e);
            return false;
        }
        float ddx = p.x - e.x, ddy = p.y - e.y;
        float d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
        if (d < 6 && p.drop <= 0) {
            if (e.hasGun && (p.weapon == Entity.W_STD || p.weapon == e.gunKind())) {
                // ((12.0) Rounds for the gun they have: loaded up, not their gun swapped for a six-shot one.)
                e.reserve += p.rounds;
                if (e.ammo <= 0 && e.reload <= 0) e.reload = 1.6f;
            } else {
                w.population.armCivilian(e, p.rounds);
                if (p.weapon != Entity.W_STD) w.combat.setGun(e, p.weapon, p.rounds);
            }
            if (p.uses > 0) {
                if (--p.uses <= 0) w.pickups.remove(p);
            } else w.pickups.remove(p);
            e.task = Dispatch.T_NONE;
            e.pickup = null;
        } else if (d > 40) w.wilds.walkToSpot(e, p.x, p.y, e.speed * 1.6f);
        else w.movement.steer(e, ddx / d, ddy / d, e.speed * 1.6f);
        return true;
    }

    void updateMedkits(float dt) {
        for (int i = w.medkits.size() - 1; i >= 0; i--) {
            float[] m = w.medkits.get(i);
            for (int k = 0, n = w.entities.size(); k < n && m[2] > 0; k++) {
                Entity e = w.entities.get(k);
                if (e.dead || e.isZombie()) continue;
                if ((e.x - m[0]) * (e.x - m[0]) + (e.y - m[1]) * (e.y - m[1]) > 40 * 40) continue;
                if (e.hp < e.maxHp) {
                    e.hp = Math.min(e.maxHp, e.hp + 15 * dt);
                    m[2] -= dt * 0.5f;
                }
            }
            if (m[2] <= 0) w.medkits.remove(i);
        }
    }

    float[] placeMedkit(float x, float y) {
        float[] p = w.city.findWalkable(x, y);
        if (p == null) return null;
        float[] m = {p[0], p[1], 12};
        w.medkits.add(m);
        return m;
    }

    /** ((22.0) A barricade of a kind (0 wood, 1 sandbags, 2 concrete) on the tile at (x, y), or null. */
    float[] placeBarricade(float x, float y, int tier) {
        float[] b = placeBarricade(x, y);
        if (b != null) World.setTier(b, tier);
        return b;
    }

    float[] placeBarricade(float x, float y) {
        int tx = City.fl(x / City.T), ty = City.fl(y / City.T);
        if (tx < 0 || ty < 0 || tx >= w.city.w || ty >= w.city.h) return null;
        int i = ty * w.city.w + tx;
        byte t = w.city.tiles[i];
        if (w.city.solid[i] || t == City.RAIL) return null;
        // Not on top of someone. ((16.8) Those in the grid's cells round it - the living and the dead - not a look
        // through everyone: sealing off a district put up seventy, each looking at tens of thousands.)
        float bx = tx * City.T + City.T / 2f, by = ty * City.T + City.T / 2f, reach = City.T + World.GRID_SLACK;
        if (w.hashSize < 0) w.spatial.buildHash();
        int gx0 = Math.max(0, (int) ((bx - reach) / World.CELL)), gx1 = Math.min(w.gw - 1, (int) ((bx + reach) / World.CELL));
        int gy0 = Math.max(0, (int) ((by - reach) / World.CELL)), gy1 = Math.min(w.gh - 1, (int) ((by + reach) / World.CELL));
        for (int gy = gy0; gy <= gy1; gy++)
            for (int gx = gx0; gx <= gx1; gx++)
                for (int k = w.cellStart[gy * w.gw + gx], kend = k + w.cellCount[gy * w.gw + gx]; k < kend; k++) {
                    Entity e = w.sorted[k];
                    if (Math.abs(e.x - bx) < City.T / 2f + e.radius && Math.abs(e.y - by) < City.T / 2f + e.radius) return null;
                }
        float[] b = {tx * City.T + City.T / 2f, ty * City.T + City.T / 2f, 220, i, t, 0};
        w.city.tiles[i] = City.FENCE;
        w.city.solid[i] = true;
        w.city.solidChanged(i);
        w.barriers.add(b);
        w.fieldTimer = 0;
        return b;
    }

    void removeBarricade(float[] b) {
        if (!w.barriers.remove(b)) return;
        int i = (int) b[3];
        w.city.tiles[i] = (byte) b[4];
        w.city.solid[i] = false;
        w.city.solidChanged(i);
        w.fieldTimer = 0;
    }

    /** A crate of guns and ammo; with a drop time it comes down by parachute. */
    World.Pickup placeCrate(float x, float y, float drop) {
        float[] p = w.city.findWalkable(x, y);
        if (p == null) return null;
        World.Pickup c = new World.Pickup();
        c.x = p[0];
        c.y = p[1];
        c.rounds = 30;
        c.uses = 6;
        c.drop = drop;
        w.pickups.add(c);
        return c;
    }

    void supplyDrop(float x, float y) {
        World.Pickup c = placeCrate(x, y, 3.5f);
        if (c != null)
            w.dispatch.say(Dispatch.WHO_MILITARY, null, "Military: Supply drop coming down at " + w.city.placeName(c.x, c.y)
                    + ". Weapons and ammo for anyone who needs them.", c.x, c.y);
    }

    /** A big horde walks in from the nearest edge of the map, heading for (x, y). Returns who came. */
    ArrayList<Entity> hordeEvent(float x, float y) {
        return hordeFrom(x, y, -1, true);
    }

    /**
     * ((22.0) The mega horde: two hundred and fifty to three hundred and fifty of the dead in from the nearest edge,
     * all one horde behind an alpha, heading for (x, y).
     */
    ArrayList<Entity> megaHorde(float x, float y) {
        // ((29.2) Four to five and a half hundred now (250 to 350 before).)
        ArrayList<Entity> out = hordeFrom(x, y, 400 + w.rnd.nextInt(150), false);
        if (out.isEmpty()) return out;
        Entity lead = out.get(0);
        if (!lead.alpha) w.zombieAI.makeAlpha(lead);
        w.dispatch.say(Dispatch.WHO_INFO, null, "A mega horde of " + out.size() + " is pouring into town from "
                + w.city.placeName(lead.x, lead.y) + "!", lead.x, lead.y, true);
        w.stats.timeline("A mega horde of " + out.size() + " comes into town", lead.x, lead.y);
        w.megaHordes++;
        return out;
    }

    /** ((20.0) A horde of n (25 to 40 if n is negative) in from the nearest edge, heading for (x, y); said on the radio or not. */
    ArrayList<Entity> hordeFrom(float x, float y, int count, boolean announce) {
        ArrayList<Entity> out = new ArrayList<Entity>();
        // ((29.2) From somewhere out of sight a few streets off, not the edge of the map: walking in from the edge, a horde
        // met the patrols on the beltway and the highway and was shot down there, three in four of it, before it ever got
        // into town.)
        float[] edge = approach(x, y);
        if (edge == null) edge = w.city.edgeSpawn(x, y);
        if (edge == null) return out;
        Entity lead = null;
        // ((29.2) Bigger: 90 to 130 (25 to 40 before), to stand a chance against a city's police and army at full strength.)
        int n = count < 0 ? 90 + w.rnd.nextInt(41) : count;
        // ((22.0) Spread out to fit: a big horde in one small blob was hundreds standing on each other.)
        float spread = 50 * (float) Math.max(1, Math.sqrt(n / 35.0));
        for (int i = 0; i < n; i++) {
            float roll = w.rnd.nextFloat();
            int type = roll < 0.12f ? Entity.RUNNER : roll < 0.14f ? Entity.BRUTE : roll < 0.2f ? Entity.ZOMBIE_DOG
                    : roll < 0.25f ? Entity.BRUISER : Entity.ZOMBIE;
            Entity z = w.population.spawn(type, edge[0] + w.rnd.nextFloat() * spread - spread / 2, edge[1] + w.rnd.nextFloat() * spread - spread / 2);
            if (z == null) continue;
            out.add(z);
            z.noiseX = x + w.rnd.nextFloat() * 40 - 20;
            z.noiseY = y + w.rnd.nextFloat() * 40 - 20;
            z.noiseTimer = 90;
            if (lead == null) {
                lead = z;
                z.leadsHorde = true;
                z.roamX = x;
                z.roamY = y;
            } else w.zombieAI.join(z, lead);
        }
        // ((21.0) Pointed out on the screen for its first half minute.)
        if (lead != null && out.size() >= 5) {
            if (w.incoming.size() >= 4) w.incoming.remove(0);
            w.incoming.add(new Object[]{lead, 30f, out.size()});
        }
        if (announce && !out.isEmpty())
            w.dispatch.say(Dispatch.WHO_INFO, null, (out.size() >= 25 ? "A huge horde of " : "A horde of ") + out.size() + " is coming into town from "
                    + w.city.placeName(edge[0], edge[1]) + "!", edge[0], edge[1]);
        return out;
    }

    /** ((29.2) Where a horde for (x, y) turns up: 450 to 750 off, on open ground, out of sight, no guns within 200. */
    float[] approach(float x, float y) {
        for (int k = 0; k < 40; k++) {
            double a = w.rnd.nextFloat() * Math.PI * 2;
            float r = 450 + w.rnd.nextFloat() * 300;
            float[] p = w.city.findWalkable(x + (float) Math.cos(a) * r, y + (float) Math.sin(a) * r);
            if (p == null || w.city.insideOf(p[0], p[1]) != null || w.fleet.routing.inView(p[0], p[1]) || w.armedNear(p[0], p[1], 200) > 0) continue;
            if (p[0] < 60 || p[1] < 60 || p[0] > w.city.worldW() - 60 || p[1] > w.city.worldH() - 60) continue;
            return p;
        }
        return null;
    }

    /** Panic: everyone near a point screams and runs. */
    void panic(float x, float y) {
        w.emit(Sfx.PANIC, x, y);
        int n = 0;
        for (int i = 0, c = w.entities.size(); i < c; i++) {
            Entity e = w.entities.get(i);
            if (e.type != Entity.CIVILIAN && e.type != Entity.DOG) continue;
            if ((e.x - x) * (e.x - x) + (e.y - y) * (e.y - y) > 320 * 320) continue;
            e.fleeTimer = 5 + w.rnd.nextFloat() * 3;
            e.threatX = x;
            e.threatY = y;
            if (w.rnd.nextFloat() < 0.2f) w.emit(Sfx.SCREAM, e.x, e.y);
            n++;
        }
        w.wilds.scareBirds(x, y, 320);
        if (n > 0) w.dispatch.say(Dispatch.WHO_911, null, "\"Everyone's running! Something's happening on " + w.city.placeName(x, y) + "!\"", x, y);
    }

    /** An outbreak inside the building nearest a point. Returns false if there isn't one close by. */
    boolean indoorOutbreakAt(float x, float y) {
        City.Building best = null;
        float bd = 160 * 160;
        for (int i = 0, n = w.city.buildings.size(); i < n; i++) {
            City.Building b = w.city.buildings.get(i);
            if (b.capacity == 0 || b.collapsed) continue;
            float cx = (b.x0 + b.x1) / 2, cy = (b.y0 + b.y1) / 2;
            float d = (cx - x) * (cx - x) + (cy - y) * (cy - y);
            if (d < bd) {
                bd = d;
                best = b;
            }
        }
        if (best == null) return false;
        w.death.indoorOutbreak(best, 2);
        w.dispatch.say(Dispatch.WHO_911, null, "\"Someone's been bitten inside " + (best.name != null ? best.name : "a building on "
                + w.city.placeName(best.doorX, best.doorY)) + ". People are trapped in there!\"", best.doorX, best.doorY);
        return true;
    }

    void clearZombies() {
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.isZombie()) {
                e.dead = true;
                e.removed = true;
            }
        }
        for (int i = w.corpses.size() - 1; i >= 0; i--) if (w.corpses.get(i).rise > 0) w.corpses.get(i).rise = -1;
        w.stats.recountInsideNow();
        // ((12.0) And the dead shut inside buildings: they stayed, still counted, and came bursting out later.)
        for (int i = 0, n = w.city.buildings.size(); i < n; i++) {
            City.Building b = w.city.buildings.get(i);
            b.lurkers = 0;
            b.infestKnown = false;
        }
        w.stats.recountLive();
    }

    void clearBodies() {
        w.corpses.clear();
        for (int i = 0; i < w.dcount; i++) if (w.dkind[i] == World.D_SPLAT && (w.dcol[i] & 0xFF0000) > 0x500000) w.dr[i] = 0;
    }

    void clearWrecks() {
        for (int i = w.fleet.vehicles.size() - 1; i >= 0; i--)
            if (w.fleet.vehicles.get(i).broken && w.fleet.vehicles.get(i).riders.isEmpty()) w.fleet.discard(w.fleet.vehicles.get(i));
        w.pendingBlasts.clear();
    }

    void clearBarricades() {
        while (!w.barriers.isEmpty()) removeBarricade(w.barriers.get(w.barriers.size() - 1));
        w.defences.clear();
    }

    World.Pickup nearestPickup(float x, float y, float radius) {
        World.Pickup best = null;
        float bd = radius * radius;
        // ((16.8) By where they are, in squares of PICK_CELL, sorted out once a step (or when one comes or goes): only
        // the squares round about are looked at. Thousands asked each second, and went through every one.)
        if (w.pickupsAt != w.lodFrame || w.pickupsN != w.pickups.size()) indexPickups();
        int cx0 = Math.max(0, (int) ((x - radius) / World.PICK_CELL)), cx1 = Math.min(w.pickGW - 1, (int) ((x + radius) / World.PICK_CELL));
        int cy0 = Math.max(0, (int) ((y - radius) / World.PICK_CELL)), cy1 = Math.min(w.pickGH - 1, (int) ((y + radius) / World.PICK_CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.pickGW + cx;
                for (int k = w.pickStart[c]; k < w.pickStart[c + 1]; k++) {
                    World.Pickup p = w.pickups.get(w.pickIdx[k]);
                    if (p.claimed || p.drop > 0 || p.melee > 0) continue;
                    float d = (p.x - x) * (p.x - x) + (p.y - y) * (p.y - y);
                    if (d < bd) {
                        bd = d;
                        best = p;
                    }
                }
            }
        return best;
    }

    void indexPickups() {
        w.pickupsAt = w.lodFrame;
        int n = w.pickupsN = w.pickups.size();
        if (w.pickStart == null) {
            w.pickGW = (int) (w.city.worldW() / World.PICK_CELL) + 1;
            w.pickGH = (int) (w.city.worldH() / World.PICK_CELL) + 1;
            w.pickStart = new int[w.pickGW * w.pickGH + 1];
        }
        if (w.pickIdx.length < n) {
            w.pickIdx = new int[n * 2];
            w.pickCellOf = new int[n * 2];
        }
        // (A counting sort: a few thousand squares to clear, a few dozen things to place.)
        Arrays.fill(w.pickStart, 0);
        for (int i = 0; i < n; i++) {
            World.Pickup p = w.pickups.get(i);
            int c = Math.max(0, Math.min(w.pickGH - 1, (int) (p.y / World.PICK_CELL))) * w.pickGW + Math.max(0, Math.min(w.pickGW - 1, (int) (p.x / World.PICK_CELL)));
            w.pickCellOf[i] = c;
            w.pickStart[c + 1]++;
        }
        for (int c = 0; c + 1 < w.pickStart.length; c++) w.pickStart[c + 1] += w.pickStart[c];
        if (w.pickCur == null || w.pickCur.length != w.pickStart.length) w.pickCur = new int[w.pickStart.length];
        System.arraycopy(w.pickStart, 0, w.pickCur, 0, w.pickStart.length);
        for (int i = 0; i < n; i++) w.pickIdx[w.pickCur[w.pickCellOf[i]]++] = i;
    }

    void updatePickups(float dt) {
        for (int i = w.pickups.size() - 1; i >= 0; i--) {
            World.Pickup p = w.pickups.get(i);
            p.age += dt;
            if (p.drop > 0) {
                p.drop -= dt;
                if (p.drop <= 0) {
                    w.emit(Sfx.THUD, p.x, p.y);
                    for (int k = 0; k < 8; k++)
                        w.effects.particle(p.x, p.y, w.rnd.nextFloat() * 30 - 15, w.rnd.nextFloat() * 30 - 15, 0.8f, 2, 0xFFB8AE98, World.P_SMOKE);
                }
            }
            // (10.38) A gun left lying about is gone after a minute (picked up by someone off screen); a bat or an
            // axe after three, a supply crate after ten.
            if (p.age > (p.uses > 0 ? 600 : p.melee > 0 ? 180 : 60)) w.pickups.remove(i);
        }
    }
}
