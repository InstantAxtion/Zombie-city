package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * (27.5) Survivor bases going out. A walled base with guards to spare sends a group of them out to clear the dead off the
 * blocks round it: each block they clear is theirs (its territory, drawn faintly round it, grows), and they bring back
 * what rounds they find there. One base in four is a raider base: it takes in nobody but its own kind, and its groups
 * go after other people's supplies - holding up supply convoys and armed pickups on the roads near it, and raiding
 * other bases' stores - shooting anyone who stands in the way (and the police, the army and the other bases shoot back).
 * Everyone in a raid is the base's own: nobody turns up from nowhere.
 */
final class BaseRaids {
    static final int CLEAR = 0, AMBUSH = 1, STRIKE = 2;
    /** How many blocks' worth of ground a base can claim at most. */
    static final int MAX_TERRITORY = 10;

    static final class Raid {
        SurvivorBases.Base home;
        int kind;
        /** Where they're making for (the dead to clear, the vehicle to hold up, the other base's gate), and home again. */
        float tx, ty;
        final ArrayList<Entity> team = new ArrayList<Entity>();
        Fleet.Vehicle prey;
        SurvivorBases.Base target;
        /** Seconds out, seconds the place has been clear (or undefended), and whether they're on the way home. */
        float t, clearT;
        boolean homeward, over;
    }

    private final World w;
    final ArrayList<Raid> raids = new ArrayList<Raid>();
    /** Vehicles already held up (once each). */
    private final HashSet<Fleet.Vehicle> robbed = new HashSet<Fleet.Vehicle>();
    int sent, blocksCleared, vehiclesRobbed, basesStruck, lostOnRaids;

    BaseRaids(World w) {
        this.w = w;
    }

    /** Whether the base founded round building b is a raider base: one in four, by the city and the building. */
    boolean raiderBase(City.Building b) {
        int i = w.city.buildings.indexOf(b);
        long h = (w.city.cfg.seed * 0x9E3779B97F4A7C15L) ^ (i * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 29;
        return (h & 3) == 0;
    }

    /** How far a base's ground reaches from its middle. */
    static float territoryR(SurvivorBases.Base s) {
        float half = Math.max(s.tx1 - s.tx0, s.ty1 - s.ty0) * City.T / 2f;
        return half + 60 + 45 * s.territory;
    }

    Raid raidOf(SurvivorBases.Base s) {
        for (int i = 0; i < raids.size(); i++) if (raids.get(i).home == s) return raids.get(i);
        return null;
    }

    // ------------------------------------------------------------------ once a second

    void update() {
        for (int i = raids.size() - 1; i >= 0; i--) {
            Raid r = raids.get(i);
            if (r.home.fallen || !w.bases.contains(r.home)) end(r, false);
            else step(r);
            if (r.over) raids.remove(i);
        }
        for (int i = 0; i < w.bases.size(); i++) {
            SurvivorBases.Base s = w.bases.get(i);
            s.raidCd -= 1;
            if (s.raidCd > 0 || raidOf(s) != null || !s.walled || s.quietFor < 8 || s.inside > 0) continue;
            s.raidCd = 20;
            send(s);
        }
    }

    /** A base with guards to spare picks a job for a group of them, and they go. */
    private void send(SurvivorBases.Base s) {
        ArrayList<Entity> fit = new ArrayList<Entity>();
        float cx = s.cx(), cy = s.cy();
        for (int k = 0, n = w.entities.size(); k < n; k++) {
            Entity e = w.entities.get(k);
            if (e.dead || e.base != s || !e.militia || e.raid != null || e == w.controlled || e.infected) continue;
            if (e.hp < e.maxHp * 0.7f || e.ammo + e.reserve < 14) continue;
            fit.add(e);
        }
        // (Some always stay on the wall: two; a raider base leaves one, or none if it has only three.)
        int size = Math.min(6, fit.size() - (s.raiders ? (fit.size() > 3 ? 1 : 0) : 2));
        if (size < 3) return;
        Raid r = new Raid();
        r.home = s;
        boolean ok = false;
        if (s.raiders) {
            // Raiders: a vehicle with something worth taking on the roads nearby; now and then another base's store.
            Fleet.Vehicle v = prey(s);
            if (v != null) {
                r.kind = AMBUSH;
                r.prey = v;
                r.tx = v.x;
                r.ty = v.y;
                ok = true;
            } else if (s.raids % 2 == 1) {
                SurvivorBases.Base t = strikeTarget(s);
                if (t != null) {
                    r.kind = STRIKE;
                    r.target = t;
                    float[] g = gateOutside(t);
                    r.tx = g[0];
                    r.ty = g[1];
                    ok = true;
                }
            }
        }
        if (!ok) {
            float[] spot = clearTarget(s, size);
            if (spot == null) return;
            r.kind = CLEAR;
            r.tx = spot[0];
            r.ty = spot[1];
        }
        // The fittest go: those nearest the gate first.
        for (int q = 0; q < size; q++) {
            Entity best = null;
            float bd = Float.MAX_VALUE;
            for (int k = 0; k < fit.size(); k++) {
                Entity e = fit.get(k);
                float d = (float) World.hypot(e.x - r.tx, e.y - r.ty);
                if (d < bd) {
                    bd = d;
                    best = e;
                }
            }
            fit.remove(best);
            best.raid = r;
            best.onTower = false;
            r.team.add(best);
        }
        raids.add(r);
        s.raids++;
        sent++;
        // (Their posts: the next ones' to man.)
        w.baseLife.manBase(s);
        if (s.raids == 1 || r.kind != CLEAR)
            w.dispatch.chatter(Dispatch.WHO_INFO, null, r.kind == CLEAR ? s.name + ": Going out to clear the street. Cover us from the wall."
                    : r.kind == AMBUSH ? "911: \"There's armed men from " + s.name + " heading for the road - they're after that truck!\""
                    : "911: \"A gang from " + s.name + " is heading for " + r.target.name + "! They're armed!\"", r.tx, r.ty);
        s.raidCd = r.kind == STRIKE ? 300 : r.kind == AMBUSH ? 150 : 70;
    }

    /** The nearest bunch of the dead on the base's ground or just past it that this many could take on; else open ground at its edge. */
    private float[] clearTarget(SurvivorBases.Base s, int size) {
        float cx = s.cx(), cy = s.cy(), reach = territoryR(s) + 260;
        float[] best = null;
        float bd = Float.MAX_VALUE;
        int n = w.spatial.gatherZ(cx, cy, reach), looked = 0;
        Entity[] got = new Entity[Math.min(n, 40)];
        for (int k = 0; k < got.length; k++) got[k] = w.gathered[k * Math.max(1, n / got.length)];
        for (Entity z : got) {
            if (z == null || z.dead || s.contains(z.x, z.y, 1)) continue;
            if (++looked > 30) break;
            int c = w.countZombiesNear(z.x, z.y, 60);
            if (c > size * 5) continue;
            float d = (float) World.hypot(z.x - cx, z.y - cy);
            if (d < bd) {
                bd = d;
                best = new float[]{z.x, z.y};
            }
        }
        if (best != null) return best;
        // Nothing to clear: they walk the edge of their ground and take the next block in.
        if (s.territory >= MAX_TERRITORY) return null;
        float a = s.raids * 2.4f, d = territoryR(s) + 50;
        return w.city.findWalkable(cx + (float) Math.cos(a) * d, cy + (float) Math.sin(a) * d);
    }

    /** A vehicle near a raider base carrying something: a supply convoy's truck, an ammo truck, another base's pickup. */
    private Fleet.Vehicle prey(SurvivorBases.Base s) {
        float cx = s.cx(), cy = s.cy();
        Fleet.Vehicle best = null;
        float bd = 900;
        for (int i = 0; i < w.fleet.vehicles.size(); i++) {
            Fleet.Vehicle v = w.fleet.vehicles.get(i);
            if (v.broken || v.removedFromFleet || robbed.contains(v) || Fleet.offRoad(v)) continue;
            boolean worth = (v.convoy != null && v.convoyRole == Convoys.V_TRUCK && v.convoy.purpose == Convoys.SUPPLY && v.convoy.cargo > 0
                    && v.convoy.state < Convoys.AT_TARGET)
                    || (v.ammoTruck && v.cargo > 0) || (v.kind == Fleet.K_PICKUP && v.pbase != s);
            if (!worth) continue;
            float d = (float) World.hypot(v.x - cx, v.y - cy);
            if (d < bd) {
                bd = d;
                best = v;
            }
        }
        return best;
    }

    /** Another base worth raiding: walled, not raiders themselves, rounds in store, not too far. */
    private SurvivorBases.Base strikeTarget(SurvivorBases.Base s) {
        SurvivorBases.Base best = null;
        float bd = 1400;
        for (int i = 0; i < w.bases.size(); i++) {
            SurvivorBases.Base o = w.bases.get(i);
            if (o == s || o.raiders || o.fallen || !o.walled || o.stash < 60) continue;
            float d = (float) World.hypot(o.cx() - s.cx(), o.cy() - s.cy());
            if (d < bd) {
                bd = d;
                best = o;
            }
        }
        return best;
    }

    /** A spot just outside a base's first gate. */
    private float[] gateOutside(SurvivorBases.Base t) {
        int g = t.gates.length > 0 ? t.gates[0] : -1;
        float gx = g >= 0 ? (g % w.city.w + 0.5f) * City.T : t.b.doorX, gy = g >= 0 ? (g / w.city.w + 0.5f) * City.T : t.b.doorY;
        float dx = gx - t.cx(), dy = gy - t.cy(), l = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
        float[] p = w.city.findWalkable(gx + dx / l * City.T * 2.5f, gy + dy / l * City.T * 2.5f);
        return p != null ? p : new float[]{gx, gy};
    }

    /** A raid, once a second: who's still out, whether the job's done, and home again. */
    private void step(Raid r) {
        r.t += 1;
        for (int k = r.team.size() - 1; k >= 0; k--) {
            Entity e = r.team.get(k);
            if (e.dead || e.raid != r || e.base != r.home) {
                if (e.dead && !e.removed) {
                    lostOnRaids++;
                }
                r.team.remove(k);
            }
        }
        if (r.team.isEmpty()) {
            end(r, false);
            return;
        }
        float cx = 0, cy = 0;
        for (Entity e : r.team) {
            cx += e.x;
            cy += e.y;
        }
        cx /= r.team.size();
        cy /= r.team.size();
        SurvivorBases.Base s = r.home;
        if (r.homeward) {
            if (s.contains(cx, cy, 0)) end(r, true);
            else if (r.t > 260) end(r, true);
            return;
        }
        if (r.t > 150) {
            goHome(r);
            return;
        }
        float near = (float) World.hypot(cx - r.tx, cy - r.ty);
        if (r.kind == CLEAR) {
            if (near < 70 && w.countZombiesNear(r.tx, r.ty, 130) == 0) r.clearT += 1;
            else r.clearT = 0;
            if (r.clearT >= 4) {
                s.territory = Math.min(MAX_TERRITORY, s.territory + 1);
                blocksCleared++;
                // (And what they find there: a few boxes of rounds for the store.)
                int found = 10 + w.rnd.nextInt(16);
                s.stash = Math.min(400, s.stash + found);
                if (s.territory == 1 || s.territory % 4 == 0)
                    w.dispatch.chatter(Dispatch.WHO_INFO, null, s.name + ": Street's clear. Bringing back " + found + " rounds.", r.tx, r.ty);
                goHome(r);
            }
        } else if (r.kind == AMBUSH) {
            Fleet.Vehicle v = r.prey;
            if (v == null || v.removedFromFleet || robbed.contains(v)) {
                goHome(r);
                return;
            }
            Convoys.Column c = v.convoy;
            // (Unloaded already, or gone home: too late.)
            if (c != null && c.state >= Convoys.AT_TARGET) {
                goHome(r);
                return;
            }
            float vd = (float) World.hypot(v.x - cx, v.y - cy);
            if (c != null && vd > 200) {
                // Ahead of a moving convoy: across the road it's taking, halfway between it and where it's going.
                float[] p = w.city.findWalkable((v.x + c.tx) / 2, (v.y + c.ty) / 2);
                r.tx = p != null ? p[0] : v.x;
                r.ty = p != null ? p[1] : v.y;
            } else {
                r.tx = v.x;
                r.ty = v.y;
            }
            if (World.hypot(v.x - s.cx(), v.y - s.cy()) > 1300 || (vd > 400 && r.t > 90)) {
                goHome(r);
                return;
            }
            // Close enough: they shoot it up till it's theirs (a convoy under fire halts, its escort fighting back).
            int shooting = 0;
            for (Entity e : r.team) {
                if (World.hypot(v.x - e.x, v.y - e.y) > 130 || e.ammo + e.reserve <= 0 || !w.city.los(e.x, e.y, v.x, v.y)) continue;
                shooting++;
                hostile(e);
                w.effects.tracer(e.x, e.y, v.x + w.rnd.nextFloat() * 6 - 3, v.y + w.rnd.nextFloat() * 6 - 3);
                if (e.ammo > 0) e.ammo--;
                else e.reserve--;
                w.fleet.damage(v, 8, false);
            }
            if (shooting > 0 && c != null && (c.state == Convoys.MOVING || c.state == Convoys.HALTED)) {
                if (c.state == Convoys.MOVING) {
                    c.resume = Convoys.MOVING;
                    c.state = Convoys.HALTED;
                    c.stateT = 0;
                }
                c.quietT = 0;
            }
            if (shooting > 0) {
                w.emit(Sfx.RIFLE, v.x, v.y);
                w.noise(v.x, v.y, 200);
            }
            if (v.broken || v.hp < v.maxHp * 0.55f) rob(r, v);
        } else {
            SurvivorBases.Base t = r.target;
            if (t == null || t.fallen || !w.bases.contains(t)) {
                goHome(r);
                return;
            }
            // Undefended (nobody of theirs with a gun near the gate) for a few seconds: in they go, and out with the store.
            int defenders = 0;
            for (int k = 0, n = w.spatial.gatherLiving(r.tx, r.ty, 150); k < n; k++) {
                Entity o = w.gathered[k];
                if (!o.dead && o.base == t && o.canShoot() && o.ammo + o.reserve > 0) defenders++;
            }
            if (near < 60 && defenders == 0) r.clearT += 1;
            else r.clearT = 0;
            if (r.clearT >= 4) {
                int took = (int) Math.min(t.stash, 120);
                t.stash -= took;
                s.stash = Math.min(600, s.stash + took);
                s.struck++;
                basesStruck++;
                w.dispatch.say(Dispatch.WHO_INFO, null, "News: Raiders from " + s.name + " have broken into " + t.name + "'s store and made off with "
                        + took + " rounds.", t.cx(), t.cy());
                if (basesStruck == 1) w.stats.timeline("Raiders from " + s.name + " robbed " + t.name, t.cx(), t.cy());
                goHome(r);
            }
        }
    }

    /** A vehicle held up: its load is theirs. */
    private void rob(Raid r, Fleet.Vehicle v) {
        robbed.add(v);
        int took;
        String what;
        if (v.convoy != null && v.convoyRole == Convoys.V_TRUCK) {
            took = v.convoy.cargo / Math.max(1, v.convoy.trucksStart);
            v.convoy.cargo -= took;
            what = "a supply convoy's truck";
        } else if (v.ammoTruck) {
            took = v.cargo / 2;
            v.cargo -= took;
            what = "an army ammo truck";
        } else {
            took = 40;
            what = (v.pbase != null ? v.pbase.name + "'s" : "a survivors'") + " armed pickup";
        }
        SurvivorBases.Base s = r.home;
        s.stash = Math.min(600, s.stash + took);
        s.robbed++;
        vehiclesRobbed++;
        w.dispatch.say(Dispatch.WHO_INFO, null, "News: Raiders from " + s.name + " have held up " + what + " near " + w.city.placeName(v.x, v.y)
                + " and taken " + took + " rounds.", v.x, v.y);
        if (vehiclesRobbed == 1) w.stats.timeline("Raiders from " + s.name + " held up " + what, v.x, v.y);
        goHome(r);
    }

    private void hostile(Entity e) {
        e.raider = true;
        e.hostileT = World.HOSTILE_TIME;
    }

    private void goHome(Raid r) {
        r.homeward = true;
        r.clearT = 0;
        r.tx = r.home.b.doorX;
        r.ty = r.home.b.doorY;
    }

    /** The raid's over: everyone still out is back on the base's books, on the wall again. */
    private void end(Raid r, boolean home) {
        r.over = true;
        for (Entity e : r.team) {
            if (e.raid == r) e.raid = null;
            // (Back in: topped up from the store.)
            int want = home ? Math.min((int) r.home.stash, Math.max(0, 40 - e.ammo - e.reserve)) : 0;
            e.reserve += want;
            r.home.stash -= want;
            // (A raider's gun is cold again once they're home: nobody's shooting at them for what they did.)
            if (r.kind != CLEAR) e.raider = false;
        }
        r.team.clear();
        if (home && !r.home.fallen) w.baseLife.manBase(r.home);
    }

    // ------------------------------------------------------------------ each of them, every step

    /**
     * One of a raid: the dead close by first, the people in the way next (raiders only), and otherwise on with the
     * group to where they're going. False when they're no longer on it (back to their base's ordinary day).
     */
    boolean member(Entity e, float dt) {
        Raid r = e.raid;
        if (r == null || r.over) {
            e.raid = null;
            return false;
        }
        // Hurt or out of rounds: back to the base on their own.
        if (e.hp < e.maxHp * 0.45f || e.ammo + e.reserve <= 2) {
            r.team.remove(e);
            e.raid = null;
            if (r.kind != CLEAR) e.raider = false;
            e.postX = r.home.b.doorX;
            e.postY = r.home.b.doorY;
            return false;
        }
        if (e.hostileT > 0) e.hostileT -= dt;
        float reach = e.weapon == Entity.W_HUNTING ? 220 : 140;
        Entity t = w.combat.pickTarget(e, reach);
        if (t == null && !r.homeward && r.kind == STRIKE && World.hypot(r.tx - e.x, r.ty - e.y) < 220) t = defenderNear(e);
        if (t != null) {
            float d = (float) World.hypot(t.x - e.x, t.y - e.y);
            if (d < 14 && t.isZombie()) {
                float ax = e.x - t.x, ay = e.y - t.y;
                w.movement.flee(e, ax / (d + 0.01f), ay / (d + 0.01f), e.runSpeed);
            } else w.movement.steer(e, 0, 0, 0);
            if (!t.isZombie()) hostile(e);
            w.combat.aimAndFire(e, t, d, reach, dt);
            return true;
        }
        // Spread round the spot, each to their own place in it.
        int i = Math.max(0, r.team.indexOf(e));
        float a = i * 1.9f, off = r.homeward ? 0 : 14 + (i % 3) * 6;
        float gx = r.tx + (float) Math.cos(a) * off, gy = r.ty + (float) Math.sin(a) * off;
        if (World.hypot(gx - e.x, gy - e.y) > 10) w.wilds.walkToSpot(e, gx, gy, e.runSpeed * (r.kind == AMBUSH && !r.homeward ? 0.95f : 0.75f));
        else w.movement.steer(e, 0, 0, 0);
        return true;
    }

    /** Someone in the way of a raider: armed, not one of their own, in sight (the other base's guards, a convoy's escort). */
    private Entity defenderNear(Entity e) {
        Entity best = null;
        float bd = 160;
        for (int k = 0, n = w.spatial.gatherLiving(e.x, e.y, 160); k < n; k++) {
            Entity o = w.gathered[k];
            if (o.dead || o == e || o.base == e.base || o.raider || o.isZombie() || !o.canShoot()) continue;
            float d = (float) World.hypot(o.x - e.x, o.y - e.y);
            if (d < bd && w.city.los(e.x, e.y, o.x, o.y)) {
                bd = d;
                best = o;
            }
        }
        return best;
    }
}
