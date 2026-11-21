package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Dying and what follows: deaths, corpses, the infection and who turns into what.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class Death {
    private final World w;

    Death(World w) {
        this.w = w;
    }

    /** Quietly bites the person nearest (x, y). Returns them, or null. */
    Entity infectAt(float x, float y) {
        Entity best = null;
        float bd = 30 * 30;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || e.isZombie() || e.infected || e.type == Entity.DOG) continue;
            float d = (e.x - x) * (e.x - x) + (e.y - y) * (e.y - y);
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        if (best == null) return null;
        best.infected = true;
        best.infectTimer = 25 + w.rnd.nextFloat() * 20;
        if (w.patientZero == null) markPatientZero(best);
        return best;
    }

    void markPatientZero(Entity e) {
        w.patientZero = e;
        w.patientZeroSeed = e.nameSeed;
        w.outbreakPlace = w.city.placeName(e.x, e.y);
        w.stats.timeline("First infection: " + Names.person(e.nameSeed) + " at " + w.outbreakPlace, e.x, e.y);
        w.stats.highlight("Patient zero: " + Names.person(e.nameSeed), e.x, e.y);
    }

    /** How many people the infection has passed through from patient zero, and how many generations deep. */
    int[] chainFromPatientZero() {
        if (w.patientZeroSeed == 0) return new int[]{0, 0};
        java.util.HashMap<Integer, java.util.ArrayList<Integer>> kids = new java.util.HashMap<Integer, java.util.ArrayList<Integer>>();
        for (java.util.Map.Entry<Integer, Integer> en : w.bitBy.entrySet()) {
            java.util.ArrayList<Integer> l = kids.get(en.getValue());
            if (l == null) kids.put(en.getValue(), l = new java.util.ArrayList<Integer>());
            l.add(en.getKey());
        }
        int people = 0, depth = 0;
        java.util.ArrayList<Integer> level = new java.util.ArrayList<Integer>();
        java.util.HashSet<Integer> seen = new java.util.HashSet<Integer>();
        level.add(w.patientZeroSeed);
        seen.add(w.patientZeroSeed);
        while (!level.isEmpty() && depth < 500) {
            java.util.ArrayList<Integer> next = new java.util.ArrayList<Integer>();
            for (int p : level) {
                java.util.ArrayList<Integer> l = kids.get(p);
                if (l == null) continue;
                for (int k : l) if (seen.add(k)) next.add(k);
            }
            if (next.isEmpty()) break;
            people += next.size();
            depth++;
            level = next;
        }
        return new int[]{people, depth};
    }

    /** People sheltering in a building, some of them bitten: when one turns, the building is lost. */
    void indoorOutbreak(City.Building b, int infected) {
        // ((16.14) Nobody indoors: the same people, some of them bitten, out in the street at the door.)
        int people = infected + 3 + w.rnd.nextInt(5);
        for (int i = 0; i < people; i++) {
            Entity e = w.population.spawn(Entity.CIVILIAN, b.doorX + w.rnd.nextFloat() * 24 - 12, b.doorY + w.rnd.nextFloat() * 24 - 12);
            if (e == null || i >= infected) continue;
            e.infected = true;
            e.infectTimer = 4 + w.rnd.nextFloat() * 14;
        }
    }

    void cleanup() {
        // ((22.1) Those passed over this step (calm, off screen, nothing near) can't have died in it of anything of
        // their own: they're looked at on every eighth step, with everyone else. Looking at all thirty thousand every
        // step, just in case, was a twentieth of the time.)
        boolean sweep = w.lodFrame % 8 == 0;
        for (int i = w.entities.size() - 1; i >= 0; i--) {
            if (!sweep && i < w.lodWake.length && w.lodWake[i] - w.lodFrame > 1 && w.lodSet[i] != w.lodFrame) continue;
            Entity e = w.entities.get(i);
            if (e.hp <= 0) e.dead = true;
            if (!e.dead) continue;
            int last = w.entities.size() - 1;
            w.entities.set(i, w.entities.get(last));
            w.entities.remove(last);
            if (last < w.lodWake.length) {
                w.lodWake[i] = 0;
                w.lodWake[last] = 0;
            }
            if (!e.removed) onDeath(e);
            else e.goneAt = w.time;
        }
    }

    void onDeath(Entity e) {
        if (e.alpha) w.zombieAI.alphaDown(e);
        if (e.type == Entity.GIANT) w.zombieAI.giantDown(e);
        if (e.type == Entity.COP) w.copsLost++;
        if (e.type == Entity.SOLDIER) w.soldiersLost++;
        if (e.kills > 0 && !e.isZombie()) {
            w.fallenHeroes.add(new int[]{e.nameSeed, e.type, e.role, e.kills});
            if (w.fallenHeroes.size() > 40) w.fallenHeroes.remove(0);
        }
        w.dispatch.onDeath(e);
        // ((18.2) The prison's inmates aren't counted with the townspeople.)
        if (e.type == Entity.CIVILIAN && !e.inmate) w.civiliansLost++;
        // Their gun stays where they fell for someone else to pick up.
        if (e.canShoot() && e.ammo + e.reserve > 0) {
            World.Pickup p = new World.Pickup();
            p.x = e.x + w.rnd.nextFloat() * 6 - 3;
            p.y = e.y + w.rnd.nextFloat() * 6 - 3;
            p.rounds = Math.min(60, e.ammo + e.reserve);
            p.weapon = e.gunKind();
            w.pickups.add(p);
            if (w.pickups.size() > 200) w.pickups.remove(0);
        }
        if (e.melee > 0) w.combat.dropMelee(e, e.melee);
        if (w.gore) w.effects.decal(e.x + w.rnd.nextFloat() * 4 - 2, e.y + w.rnd.nextFloat() * 4 - 2, e.radius * (1.4f + w.rnd.nextFloat()), 0x996E0A0A);
        if (e.isZombie()) w.zombiesKilled++;
        World.Corpse c = new World.Corpse();
        c.x = e.x;
        c.y = e.y;
        c.angle = w.rnd.nextFloat() * World.TAU;
        c.radius = e.radius;
        c.body = e.body;
        c.head = e.head;
        c.zombie = e.isUndead();
        // (For one of the dead, who they were: kept in case it gets up again.)
        c.origin = e.isUndead() ? e.origin : e.type;
        c.nameSeed = e.nameSeed;
        c.vx = e.knockX;
        c.vy = e.knockY;
        c.spin = (e.knockX != 0 || e.knockY != 0) ? (w.rnd.nextFloat() - 0.5f) * 12 : 0;
        boolean turns = !e.isZombie() && !e.gibbed && (e.killedByZombie || e.infected)
                && (e.type != Entity.DOG || w.rnd.nextFloat() < 0.6f);
        if (!turns && w.trait[World.TR_RESTLESS] && !e.isZombie() && !e.gibbed && e.type != Entity.DOG && w.rnd.nextFloat() < 0.35f) {
            turns = true;
            w.war.reveal(World.TR_RESTLESS);
        }
        c.rise = turns ? (2.5f + w.rnd.nextFloat() * 3f) * (w.trait[World.TR_QUICK_RISE] ? 0.35f : w.trait[World.TR_SLOW_RISE] ? 3f : 1f) : -1;
        c.riseType = e.type == Entity.DOG ? Entity.ZOMBIE_DOG : turnType(e.type);
        // ((27.0) Police and soldiers come back in their armour: soldiers, riot police and the tactical teams always,
        // an ordinary officer about half the time (which half goes by who they were, not by chance).)
        if (c.riseType == Entity.ZOMBIE && (e.type == Entity.SOLDIER || (e.type == Entity.COP
                && (e.role == Entity.ROLE_RIOT || e.role == Entity.ROLE_SWAT || (e.nameSeed & 1) == 0)))) c.riseType = Entity.ARMOURED;
        // ((29.2) Put down is put down: one dropped by body shots alone no longer gets up again (12.1 had it that only the
        // head stopped them for good, and the dead seemed to come back after dying).)
        if (e.gibbed) {
            w.effects.bloodBurst(e.x, e.y, 14, 0, 0);
            if (w.rnd.nextBoolean()) return;
        }
        w.corpses.add(c);
        if (w.corpses.size() > (w.quality == World.Q_FULL ? 200 : w.quality == World.Q_REDUCED ? 140 : 90)) {
            for (int i = 0; i < w.corpses.size(); i++)
                if (w.corpses.get(i).rise < 0) {
                    w.corpses.remove(i);
                    break;
                }
        }
    }

    /**
     * What someone comes back as. Nearly all are ordinary zombies; only now and then (about one in eleven since
     * 10.38) does one turn into one of the other kinds (a big soldier or cop is likelier to come back a brute).
     * (No crawlers since 10.38.)
     */
    int turnType(int was) {
        float roll = w.rnd.nextFloat();
        float brute = was == Entity.SOLDIER || was == Entity.COP ? 0.012f : 0.005f;
        float runners = w.trait[World.TR_RUNNERS] ? 0.22f : 0.05f;
        if (roll < runners) return Entity.RUNNER;
        roll -= runners;
        if (w.trait[World.TR_BRUTES]) brute *= 4;
        // (No more bloaters since 10.28, nor screamers and spitters since 10.50.)
        if (roll < brute) return Entity.BRUTE;
        roll -= brute;
        // ((22.6) And now and then a bruiser, likelier from the big and the trained.)
        float bruiser = (was == Entity.SOLDIER || was == Entity.COP ? 0.03f : 0.015f) * (w.trait[World.TR_BRUTES] ? 2 : 1);
        if (roll < bruiser) return Entity.BRUISER;
        return Entity.ZOMBIE;
    }

    /** ((27.0) An armoured zombie's helmet shot off: the next one to the head counts. */
    void loseHelmet(Entity t) {
        t.helmet = false;
        t.head = t.skin;
        for (int k = 0; k < 4; k++) w.effects.particle(t.x, t.y, w.rnd.nextFloat() * 60 - 30, w.rnd.nextFloat() * 60 - 30, 0.35f, 1f, 0xFFD8D8C8, World.P_DOT);
    }

    void updateCorpses(float dt) {
        for (int i = w.corpses.size() - 1; i >= 0; i--) {
            World.Corpse k = w.corpses.get(i);
            if (k.vx != 0 || k.vy != 0) {
                // Sliding to a stop; walls stop it dead.
                float nx = k.x + k.vx * dt, ny = k.y + k.vy * dt;
                if (w.city.circleBlocked(nx, ny, 3)) {
                    k.vx = k.vy = 0;
                } else {
                    k.x = nx;
                    k.y = ny;
                    k.angle += k.spin * dt;
                    float fr = Math.max(0, 1 - dt * 5);
                    k.vx *= fr;
                    k.vy *= fr;
                    k.spin *= fr;
                    if (Math.abs(k.vx) + Math.abs(k.vy) < 4) k.vx = k.vy = 0;
                }
            }
        }
        for (int i = w.corpses.size() - 1; i >= 0; i--) {
            World.Corpse c = w.corpses.get(i);
            c.age += dt;
            if (c.rise > 0) {
                c.rise -= dt;
                if (c.rise <= 0) {
                    w.corpses.remove(i);
                    if (w.entities.size() >= w.maxEntities) continue;
                    if (c.zombie) {
                        // ((12.1) One of the dead that was only shot in the body, getting back up: not one more of
                        // them, and not fresh, just not finished, and still hurt.)
                        Entity z = w.population.make(c.riseType, c.x, c.y, c.origin, 0);
                        z.body = c.body;
                        z.head = c.head;
                        z.armored = ((c.origin == Entity.COP || c.origin == Entity.SOLDIER) && w.rnd.nextFloat() < 0.75f) || z.type == Entity.ARMOURED;
                        // ((27.0) Its helmet only if it still had it when it fell.)
                        z.helmet = z.type == Entity.ARMOURED && c.head == City.darken(c.body, 0.62f);
                        z.angle = c.angle;
                        z.nameSeed = c.nameSeed;
                        z.hp = z.maxHp * 0.6f;
                        w.entities.add(z);
                        w.zombiesKilled = Math.max(0, w.zombiesKilled - 1);
                        w.emit(Sfx.GROAN, c.x, c.y);
                        continue;
                    }
                    Entity z = w.population.make(c.riseType, c.x, c.y, c.origin, c.body);
                    z.angle = c.angle;
                    // (10.32) A police officer or soldier comes back still in their body armour (and helmet).
                    z.armored = ((c.origin == Entity.COP || c.origin == Entity.SOLDIER) && w.rnd.nextFloat() < 0.75f) || z.type == Entity.ARMOURED;
                    // Same person, now one of them: who bit them gets the credit.
                    z.nameSeed = c.nameSeed;
                    Integer by = w.bitBy.get(c.nameSeed);
                    if (by != null) w.victims.put(by, w.victims.containsKey(by) ? w.victims.get(by) + 1 : 1);
                    // Freshly turned: fast and hungry for a while.
                    z.fresh = 45;
                    z.speed *= World.freshWalk(z);
                    z.runSpeed *= World.freshRun(z);
                    w.entities.add(z);
                    w.turned++;
                    w.stats.noteTurn(c.x, c.y);
                    w.effects.bloodBurst(c.x, c.y, 5, 0, 0);
                    w.emit(Sfx.GROAN, c.x, c.y);
                }
            } else if (c.age > World.CORPSE_LIFE) {
                // ((10.59) Cleared away after a minute, not two and a half: a big outbreak left the streets carpeted.)
                w.corpses.remove(i);
            }
        }
    }
}
