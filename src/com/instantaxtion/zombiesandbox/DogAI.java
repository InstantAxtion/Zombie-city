package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Dogs: what a dog does each step, and what it can smell.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class DogAI {
    private final World w;

    DogAI(World w) {
        this.w = w;
    }

    /**
     * ((22.0) A police dog's nose, every second and a half: the dead hiding nearby - lying still, or shut in a building
     * - are flushed out, and anyone carrying a bite it can smell is pointed out and kept out of the safe zones.
     */
    void sniff(Entity dog, float dt) {
        Entity handler = dog.leader;
        if (handler == null || handler.dead || (dog.sniffT -= dt) > 0) return;
        dog.sniffT = 1.5f;
        int found = 0, bitten = 0;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity o = w.entities.get(i);
            if (o.dead || Math.abs(o.x - dog.x) > 140 || Math.abs(o.y - dog.y) > 140) continue;
            if (o.isZombie() && o.inSolid > 0 && World.hypot(o.x - dog.x, o.y - dog.y) < 140) {
                o.noiseX = dog.x;
                o.noiseY = dog.y;
                o.noiseTimer = 8;
                found++;
            } else if (o.type == Entity.CIVILIAN && o.infected && !o.sniffed && World.hypot(o.x - dog.x, o.y - dog.y) < 90) {
                o.sniffed = true;
                o.screened = true;
                o.refused = true;
                bitten++;
            }
        }
        // Shut in a building close by: the dog scratches at the door and they come out.
        City.Building b = null;
        for (int k = 0; k < 5 && b == null; k++) {
            City.Building o = w.city.buildingAt(dog.x + (k == 1 ? 40 : k == 2 ? -40 : 0), dog.y + (k == 3 ? 40 : k == 4 ? -40 : 0));
            if (o != null && o.lurkers > 0 && !o.collapsed) b = o;
        }
        if (b != null && b.lurkers > 0) {
            b.lurkers--;
            w.population.spawn(Entity.ZOMBIE, b.doorX, b.doorY);
            found++;
        }
        if (found + bitten == 0) return;
        w.emit(Sfx.BARK, dog.x, dog.y);
        if (w.time > w.sniffSaid) {
            w.sniffSaid = w.time + 25;
            w.dispatch.say(Dispatch.WHO_POLICE, handler, "K9 has a scent at " + w.city.placeName(dog.x, dog.y) + ": "
                    + (found > 0 ? World.count(found, "of the dead", "of the dead") + " flushed out" : "")
                    + (found > 0 && bitten > 0 ? ", and " : "") + (bitten > 0 ? World.count(bitten, "bite", "bites") + " found" : "") + ".", dog.x, dog.y);
        }
        w.k9Finds += found + bitten;
    }

    void thinkDog(Entity e, float dt) {
        if (e.role == Entity.ROLE_K9) sniff(e, dt);
        if (w.rides.boardRide(e)) return;
        Entity threat = w.spatial.nearest(e, 80, true, true);
        Entity owner = e.leader;
        boolean hasOwner = owner != null && !owner.dead;
        if (threat != null) {
            float ddx = threat.x - e.x, ddy = threat.y - e.y;
            float d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
            if (w.rnd.nextFloat() < dt * 2.5f) w.emit(Sfx.BARK, e.x, e.y);
            boolean guarding = hasOwner && World.hypot(owner.x - threat.x, owner.y - threat.y) < 50;
            // Police dogs go in hard.
            boolean k9 = e.role == Entity.ROLE_K9 && hasOwner;
            if (e.hp > e.maxHp * (k9 ? 0.25f : 0.4f) && (guarding || d < 20 || (k9 && d < 70))) {
                w.movement.steer(e, ddx / d, ddy / d, e.runSpeed);
                if (d < e.radius + threat.radius + 2.5f && e.meleeCd <= 0) {
                    e.meleeCd = 0.7f;
                    threat.hp -= 6;
                    threat.hurt = 1;
                    threat.stun = Math.max(threat.stun, threat.type == Entity.BRUTE ? 0.1f : 0.5f);
                    w.movement.tryMove(threat, ddx / d * 3 / threat.mass, ddy / d * 3 / threat.mass);
                    w.emit(Sfx.BARK, e.x, e.y);
                }
                return;
            }
            w.movement.flee(e, -ddx / d, -ddy / d, e.runSpeed);
            return;
        }
        if (hasOwner) {
            float ddx = owner.x - e.x, ddy = owner.y - e.y;
            float d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
            if (d > 14) {
                w.movement.steer(e, ddx / d, ddy / d, d > 40 ? e.runSpeed : Math.max(e.speed * 1.3f, owner.want * 1.1f));
                return;
            }
            if (owner.want < 1) {
                w.movement.wander(e, e.speed * 0.3f);
                return;
            }
            w.movement.steer(e, owner.mx, owner.my, owner.want);
            return;
        }
        w.movement.wander(e, e.speed * 0.6f);
    }
}
