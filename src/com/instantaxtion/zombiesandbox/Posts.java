package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Posts kept manned: the roster, who stands where, and what a unit on duty is doing.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class Posts {
    private final World w;

    Posts(World w) {
        this.w = w;
    }

    /**
     * (11.5) The base gates, the armory and the country checkpoints, and whoever is on each now (worked out again from
     * where everyone stands after a load).
     */
    void buildRoster() {
        w.rosterPosts.clear();
        w.rosterHolders.clear();
        for (City.Facility f : w.city.facilities)
            if (f.kind == City.FACILITY_BASE) for (float[] p : f.posts) w.rosterPosts.add(new float[]{p[0], p[1]});
        for (float[] p : w.city.checkpointPosts) w.rosterPosts.add(new float[]{p[0], p[1]});
        if (w.city.armory != null && w.city.armoryGate != null) {
            float[] g = w.city.armoryGate;
            w.rosterPosts.add(new float[]{g[0] - 14, g[1]});
            w.rosterPosts.add(new float[]{g[0] + 14, g[1]});
            w.rosterPosts.add(new float[]{w.city.armory.doorX - 12, w.city.armory.doorY + 6});
            w.rosterPosts.add(new float[]{w.city.armory.doorX + 12, w.city.armory.doorY + 6});
        }
        for (float[] p : w.rosterPosts) {
            Entity best = null;
            float bd = 14;
            for (int i = 0, n = w.entities.size(); i < n; i++) {
                Entity e = w.entities.get(i);
                if (e.dead || e.type != Entity.SOLDIER || e.task != Dispatch.T_POST) continue;
                float d = (float) World.hypot(e.postX - p[0], e.postY - p[1]);
                if (d < bd) {
                    bd = d;
                    best = e;
                }
            }
            if (best != null) {
                // (The holder's post is the roster's spot from now on, so they can be matched up again.)
                p[0] = best.postX;
                p[1] = best.postY;
            }
            w.rosterHolders.add(best);
        }
    }

    /**
     * (11.5) A guard killed on a gate or a checkpoint is replaced: once the dead have gone from the post (nobody is
     * sent to stand among them), the nearest free soldier with rounds and their health goes and takes it. One who has
     * been called away is not replaced - it's only manned again when they come back or someone dies.
     */
    void staffPosts() {
        if (w.city.cfg != null && !w.city.cfg.nature()) return;
        for (int k = 0; k < w.rosterPosts.size(); k++) {
            float[] p = w.rosterPosts.get(k);
            Entity h = w.rosterHolders.get(k);
            if (h != null && !h.dead && !h.removed) continue;
            boolean died = h != null;
            if (w.spatial.countZombiesNear(p[0], p[1], 110) > 0) continue;
            City.Facility base = w.city.nearestFacility(City.FACILITY_BASE, p[0], p[1]);
            if (base != null && base.fallen && World.hypot(base.x - p[0], base.y - p[1]) < base.r * 1.6f) continue;
            Entity best = null;
            float bd = 700;
            for (int i = 0, n = w.entities.size(); i < n; i++) {
                Entity e = w.entities.get(i);
                if (e.dead || e.type != Entity.SOLDIER || e.task != Dispatch.T_NONE || e.rig != null || e.ride != null) continue;
                if (e.role == Entity.ROLE_COMMANDER || e.role == Entity.ROLE_PILOT || e.role == Entity.ROLE_SAR) continue;
                if (e.ammo + e.reserve <= 0 || e.hp < e.maxHp * 0.5f) continue;
                float d = (float) World.hypot(e.x - p[0], e.y - p[1]) - (e.role == Entity.ROLE_GUARD ? 150 : 0);
                if (d < bd) {
                    bd = d;
                    best = e;
                }
            }
            if (best == null) continue;
            best.task = Dispatch.T_POST;
            best.postX = p[0];
            best.postY = p[1];
            w.rosterHolders.set(k, best);
            if (died)
                w.dispatch.say(Dispatch.WHO_MILITARY, null, "Military: " + Dispatch.name(best) + ", take the post at "
                        + (base != null && base.name != null && World.hypot(base.x - p[0], base.y - p[1]) < base.r * 2 ? base.name.trim()
                        : String.valueOf(w.city.placeName(p[0], p[1]))) + ". We lost the last one there.", p[0], p[1]);
        }
    }

    /**
     * (11.3) What an officer or soldier with no call to answer is doing, for the card: walking the beat, on sentry,
     * on the perimeter, PT, the motor pool, formed up; or null for nothing in particular.
     */
    String dutyText(Entity e) {
        if (e.type == Entity.COP) {
            City.Facility st = w.city.nearestFacility(City.FACILITY_POLICE, e.x, e.y);
            if (e.dutyFor > 0) {
                String street = w.city.placeName(e.x, e.y);
                boolean stopped = Math.abs(e.vx) + Math.abs(e.vy) < 3;
                return (stopped ? "Keeping an eye on " : "Walking the beat on ") + (street != null ? street : "the streets")
                        + (e.beatMate != null && !e.beatMate.dead ? " with " + Names.person(e.beatMate.nameSeed) : "");
            }
            return null;
        }
        if (e.type != Entity.SOLDIER) return null;
        City.Facility base = w.city.nearestFacility(City.FACILITY_BASE, e.x, e.y);
        if (base == null || base.fallen || World.hypot(base.x - e.x, base.y - e.y) > base.r * 2.4f) return null;
        if (w.alert >= 1) return "Formed up on the parade ground";
        switch (soldierDutyOf(e, base)) {
            case 0: return "On sentry at " + (base.name != null ? base.name.trim() : "the base");
            case 1: return "Walking the perimeter fence";
            case 2: return "Drill on the parade ground";
            default: return e.role == Entity.ROLE_COMMANDER ? "Running the motor pool" : "Standing by at the motor pool";
        }
    }

    /**
     * (11.1) Soldiers on base in peacetime: each has a duty by squad and place in it - sentry at one of the posts,
     * a pair walking the perimeter fence, drill in ranks on the parade ground, or standing by at the motor pool. False if
     * they're nowhere near a base (they make their way back first).
     */
    boolean soldierDuty(Entity e, float dt) {
        City.Facility base = w.city.nearestFacility(City.FACILITY_BASE, e.x, e.y);
        if (base == null || base.fallen) return false;
        float bx = base.x - e.x, by = base.y - e.y;
        if (bx * bx + by * by > base.r * base.r * 6) return false;
        int duty = soldierDutyOf(e, base);
        if (duty == 0) {
            // ((11.3) Sentry: one each side of a post, standing still and facing out - not milling round it.)
            int slot = e.squad * 8 + e.member;
            float[] p = base.posts.get((slot >> 1) % base.posts.size());
            float out = (float) Math.atan2(p[1] - base.y, p[0] - base.x);
            float side = (slot & 1) == 0 ? 6 : -6;
            float px = p[0] - (float) Math.sin(out) * side, py = p[1] + (float) Math.cos(out) * side;
            if (w.city.solidAt(px, py)) {
                px = p[0];
                py = p[1];
            }
            holdPost(e, px, py, out, dt);
            return true;
        }
        if (duty == 1) {
            // ((11.3) The perimeter: a pair walking one stretch of the fence, end to end and back, with a pause
            // at each end - not round and round the base.)
            float r = base.r * 0.82f;
            int sideNo = (e.squad + (e.member >> 1)) % 4;
            boolean along = sideNo < 2;
            float fixed = (sideNo % 2 == 0 ? -r : r);
            float end = (e.dutyStep & 1) == 0 ? -r * 0.8f : r * 0.8f;
            float off = (e.member & 1) == 0 ? 4 : -4;
            float tx = base.x + (along ? end + off : fixed), ty = base.y + (along ? fixed : end + off);
            float[] q = w.city.solidAt(tx, ty) ? w.city.findWalkable(tx, ty) : new float[]{tx, ty};
            if (q == null) return false;
            float dx = q[0] - e.x, dy = q[1] - e.y;
            // ((33.0) No nearer the end of the stretch for six seconds (the fence line blocked): that's the end of it.)
            boolean stuckEnd = false;
            if (w.city.cfg.civic >= 61) {
                if (Math.abs(e.postForX - q[0]) > 20 || Math.abs(e.postForY - q[1]) > 20) {
                    e.postForX = q[0];
                    e.postForY = q[1];
                    e.postBest = Float.MAX_VALUE;
                    e.postTryT = 0;
                }
                float pd = (float) Math.sqrt(dx * dx + dy * dy);
                if (pd < e.postBest - 4) {
                    e.postBest = pd;
                    e.postTryT = 0;
                } else e.postTryT += dt;
                stuckEnd = e.postTryT > 6;
            }
            if (dx * dx + dy * dy < 10 * 10 || stuckEnd) {
                // At the end of the stretch: a look round, then back the other way.
                if (e.dutyWait <= 0) e.dutyWait = 4 + w.rnd.nextFloat() * 4;
                e.dutyWait -= dt;
                w.movement.steer(e, 0, 0, 0);
                e.angle = World.turn(e.angle, (float) Math.atan2(along ? fixed : 0, along ? 0 : fixed), dt * 1.5f);
                if (e.dutyWait <= 0) e.dutyStep++;
                return true;
            }
            w.wilds.walkToSpot(e, q[0], q[1], e.speed * 0.7f);
            return true;
        }
        if (duty == 2) {
            // ((11.3) Drill: the squad in ranks on the parade ground, all facing the same way (no more laps round
            // in circles).)
            // ((12.0) Each squad its own block of ranks: every third squad used to be given the same places, and they
            // shoved each other round them for as long as the drill went on.)
            int blk = (e.squad / 3) % 3;
            float px = base.x + ((e.member % 4) - 1.5f) * 9 + (blk == 0 ? 0 : blk == 1 ? -44 : 44), py = base.y - 20 + (e.squad % 3) * 26 + (e.member / 4) * 9;
            if (w.city.solidAt(px, py)) {
                px = base.x + ((e.member % 4) - 1.5f) * 9;
                py = base.y + (e.member / 4) * 9;
            }
            holdPost(e, px, py, (float) (-Math.PI / 2), dt);
            return true;
        }
        // Standing by at the motor pool, by the gate, ready to roll.
        // ((12.0) A line of their own for each squad, not every squad on the same few spots.)
        float gx = base.gateX + ((e.member % 4) - 1.5f) * 10 + ((e.squad / 4) % 2 == 0 ? 0 : 46), gy = base.gateY - 24 - (e.member / 4) * 10 - (e.squad % 4) * 22;
        if (w.city.solidAt(gx, gy)) {
            gx = base.x + ((e.member % 4) - 1.5f) * 10;
            gy = base.y + 30;
        }
        holdPost(e, gx, gy, (float) Math.atan2(base.gateY - gy, base.gateX - gx), dt);
        return true;
    }

    /** (12.0) Someone on their way to a safe zone who's reached the gate and is waiting at it. */
    boolean queueingAtGate(Entity e) {
        for (int i = 0; i < w.dispatch.zones.size(); i++) {
            Dispatch.SafeZone z = w.dispatch.zones.get(i);
            if (z.removed || z.gateOut.length == 0) continue;
            int g = z.gateNear(e.x, e.y);
            if (World.hypot(z.gateOut[g][0] - e.x, z.gateOut[g][1] - e.y) < 34 && !z.inside(e.x, e.y)) return true;
        }
        return false;
    }

    /** (12.0) What a guard post at (x, y) is: a base's gate, the armory, a checkpoint, or null. */
    String postName(float x, float y) {
        City.Facility base = w.city.nearestFacility(City.FACILITY_BASE, x, y);
        if (base != null && World.hypot(base.x - x, base.y - y) < base.r * 2 && base.name != null) return "the gate of " + base.name.trim();
        if (w.city.armory != null && World.hypot(w.city.armory.doorX - x, w.city.armory.doorY - y) < 120) return "the armory";
        for (float[] p : w.city.checkpointPosts) if (World.hypot(p[0] - x, p[1] - y) < 40) return "a checkpoint on " + w.city.placeName(x, y);
        for (int i = 0; i < w.infection.size(); i++) {
            World.InfectionZone z = w.infection.get(i);
            if (World.hypot(z.x - x, z.y - y) < z.r + 120) return "the checkpoint into the infection zone";
        }
        return null;
    }

    /** (11.3) On the beat, or on duty on base, while the city is quiet. */
    boolean onDuty(Entity e) {
        if (w.alert != 0) return false;
        if (e.type == Entity.COP) return e.dutyFor > 0;
        if (e.type != Entity.SOLDIER) return false;
        City.Facility base = w.city.nearestFacility(City.FACILITY_BASE, e.x, e.y);
        return base != null && !base.fallen && World.hypot(base.x - e.x, base.y - e.y) < base.r * 2.4f;
    }

    /**
     * (11.3) Which duty a soldier on base has: 0 sentry (only as many as the posts have places for), 1 the
     * perimeter, 2 drill, 3 the motor pool (commanders run it).
     */
    int soldierDutyOf(Entity e, City.Facility base) {
        if (e.role == Entity.ROLE_COMMANDER) return 3;
        int duty = (e.squad * 3 + (e.member >> 1)) % 4;
        if (duty == 0 && (base.posts.isEmpty() || ((e.squad * 8 + e.member) >> 1) >= base.posts.size())) duty = 1;
        return duty;
    }

    /**
     * (11.3) Walks to a spot and, once there, stands still facing one way: a little give before stepping back onto
     * it, so nobody shuffles and spins on the spot.
     */
    void holdPost(Entity e, float x, float y, float face, float dt) {
        // ((33.0) A post in a wall or inside a building: the nearest open ground to it. And one they get no nearer to
        // for six seconds - the far side of a fence, a corner the others fill - is wherever they've got to: a base's
        // motor pool and fence line had dozens walking on the spot, all game, for places they could never reach.)
        if (w.city.cfg.civic >= 61) {
            if (w.city.solidAt(x, y) || w.city.insideOf(x, y) != null) {
                float[] q = w.city.findWalkable(x, y);
                if (q != null) {
                    x = q[0];
                    y = q[1];
                }
            }
            if (Math.abs(e.postForX - x) > 20 || Math.abs(e.postForY - y) > 20) {
                e.postForX = x;
                e.postForY = y;
                e.postBest = Float.MAX_VALUE;
                e.postTryT = 0;
            }
            float pd = (float) World.hypot(x - e.x, y - e.y);
            if (pd < e.postBest - 4) {
                e.postBest = pd;
                e.postTryT = 0;
            } else e.postTryT += dt;
            if (e.postTryT > 6 && pd < 300) {
                x = e.x;
                y = e.y;
            }
        }
        float dx = x - e.x, dy = y - e.y, d2 = dx * dx + dy * dy;
        // ((29.4) Within a few yards and held up there by the others for a couple of seconds: there. Places worked out
        // from squad numbers (the motor pool's lines, the drill ranks) can fall on top of each other on a little base,
        // and the soldiers shoved at each other for them all game.)
        boolean idle = e.task == Dispatch.T_NONE;
        if (idle && d2 < 30 * 30 && e.vx * e.vx + e.vy * e.vy < 9) e.crowdT += 1 / 30f;
        else if (!idle || d2 >= 30 * 30) e.crowdT = 0;
        if (e.crowdT >= 2 && d2 < 30 * 30) {
            e.dutyFor = 0;
            w.movement.steer(e, 0, 0, 0);
            e.angle = World.turn(e.angle, face, dt * 2);
            return;
        }
        if (d2 > 9 * 9 || (e.dutyFor > 0 && d2 > 3 * 3)) {
            e.dutyFor = 1;
            if (d2 > 30 * 30) w.wilds.walkToSpot(e, x, y, e.speed * 0.8f);
            else {
                float d = (float) Math.sqrt(d2);
                w.movement.steer(e, dx / d, dy / d, Math.min(e.speed * 0.8f, d * 2));
            }
            return;
        }
        e.dutyFor = 0;
        w.movement.steer(e, 0, 0, 0);
        e.angle = World.turn(e.angle, face, dt * 2);
    }

    void holdPost(Entity e, Dispatch.SafeZone z, float speedFactor) {
        float run = e.runSpeed * speedFactor, walk = e.speed * speedFactor;
        int gates = z.gateTiles.length;
        if (gates == 0) {
            w.holdoutLife.standAt(e, z.x, z.y, speedFactor);
            return;
        }
        if (z.field == null) {
            // The way there from anywhere in town: to the gates (and, while the wall is going up, the middle).
            float[] xs = new float[gates + 1], ys = new float[gates + 1];
            for (int g = 0; g < gates; g++) {
                xs[g] = z.gateOut[g][0];
                ys[g] = z.gateOut[g][1];
            }
            xs[gates] = z.x;
            ys[gates] = z.y;
            // ((16.8) Out to a walk's distance, not the whole map, and kept small: on the Large map it was a search of
            // every street in town - over a tenth of a second, the longest step of the game - and 13 MB kept for
            // each zone. A guard further off than that finds their own way, as anyone does.)
            z.field = w.city.walkFieldFrom(xs, ys, gates + 1, World.ZONE_REACH);
        }
        float far = Math.max(Math.abs(e.x - z.x) - (z.right() - z.left()) / 2, Math.abs(e.y - z.y) - (z.bottom() - z.top()) / 2);
        // ((12.0) Through the wall one way or the other: the way planned from the other side is no good now. A guard
        // in through the nearer gate kept the route to their post planned from outside - back out, round the wall
        // and in at the far gate - and went out, and in, and out again, for good.)
        boolean ins = z.inside(e.x, e.y);
        if (ins != e.wasInside) {
            e.wasInside = ins;
            e.path = null;
        }
        if (far > 160 && w.movement.followField(e, z.field, e.runSpeed * speedFactor)) return;
        if (!z.open) {
            // Building the wall: at the stretch going up now.
            int k = Math.min(z.wall.length - 1, (int) (z.built * z.wall.length) + e.slot * 2);
            if (z.wall.length == 0 || k < 0) {
                w.holdoutLife.standAt(e, z.x, z.y, speedFactor);
                return;
            }
            float[] p = z.innerOf(z.wall[k], w.city.w);
            if (z.contains(e.x, e.y, 1) || World.hypot(p[0] - e.x, p[1] - e.y) < 200) w.wilds.walkToSpot(e, p[0], p[1], run);
            else if (z.field == null || !w.movement.followField(e, z.field, run)) w.wilds.walkToSpot(e, p[0], p[1], run);
            return;
        }
        if (!z.inside(e.x, e.y)) {
            // In through the nearest gate. ((12.0) A gate's own guards in through their own gate: in through the
            // nearer one, with the inside cut in two by buildings, the way to their post led back out, round, and
            // in at the other - and in at the nearer one again, for good.)
            int g = e.slot < gates * 2 ? e.slot / 2 : z.gateNear(e.x, e.y);
            float[] out = z.gateOut[g], in = z.gateIn[g];
            // ((12.0) Within reach of the inside once nearer it than the gate is long: there was a stretch in the
            // gateway out of reach of both, and a guard there walked out, then in, then out again, for good.)
            if (World.hypot(out[0] - e.x, out[1] - e.y) > 20 && !(z.gateOpen[g] && World.hypot(in[0] - e.x, in[1] - e.y) < 40)) {
                w.wilds.walkToSpot(e, out[0], out[1], run);
                return;
            }
            float ix = in[0] - e.x, iy = in[1] - e.y, id = (float) Math.sqrt(ix * ix + iy * iy) + 0.001f;
            if (z.gateOpen[g]) w.movement.steer(e, ix / id, iy / id, walk);
            else w.holdoutLife.standAt(e, out[0], out[1], speedFactor);
            return;
        }
        float px, py, face;
        if (e.slot < gates * 2) {
            // On the gate: one each side, just inside it, facing the street.
            int g = e.slot / 2;
            float[] out = z.gateOut[g], in = z.gateIn[g];
            float dx = out[0] - in[0], dy = out[1] - in[1], l = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
            float side = e.slot % 2 == 0 ? -1 : 1;
            // ((12.0) A step further in than the gate's inner end: that's right on the edge of what counts as inside
            // the wall, and a guard there was inside one moment - off to their post - and outside the next - off
            // round to the gate - back and forth on the spot for good.)
            // (And nearer the middle where the side of the gate is a building's wall: a gate in a narrow alley had
            // its guards' places inside the buildings either side, and they stood pushing at them.)
            float off = 16;
            px = in[0] - dx / l * 10 - dy / l * off * side;
            py = in[1] - dy / l * 10 + dx / l * off * side;
            while (off > 0 && w.city.solidAt(px, py)) {
                off -= 4;
                px = in[0] - dx / l * 10 - dy / l * off * side;
                py = in[1] - dy / l * 10 + dx / l * off * side;
            }
            face = (float) Math.atan2(dy, dx);
        } else {
            // ((Every other guard on the wall) to the nearest breach within a walk, not one with a horde at it.)
            float[] breach = z.breaches > 0 && e.slot % 2 == 0 ? w.dispatch.nearestBreach(z, e.x, e.y, 480, 5) : null;
            if (breach != null) {
                // Covering a breach (and mending it once it's clear), then back to their post.
                px = breach[0];
                py = breach[1];
                face = (float) Math.atan2(breach[1] - z.y, breach[0] - z.x);
            } else {
                // Spaced out along the wall.
                int n = Math.max(1, z.guards - gates * 2), k = e.slot - gates * 2;
                int idx = Math.max(0, Math.min(z.wall.length - 1, (int) ((k + 0.5f) / n * z.wall.length)));
                if (z.wall.length == 0) {
                    w.holdoutLife.standAt(e, z.x, z.y, speedFactor);
                    return;
                }
                float[] p = z.innerOf(z.wall[idx], w.city.w);
                px = p[0];
                py = p[1];
                face = (float) Math.atan2(p[1] - z.y, p[0] - z.x);
            }
        }
        float ddx = px - e.x, ddy = py - e.y, d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
        if (d > 40) w.wilds.walkToSpot(e, px, py, run);
        else if (d > 4 && !(e.blocked && d < 16)) w.movement.steer(e, ddx / d, ddy / d, Math.min(d > 20 ? run : walk, d * 2.5f + 4));
        else {
            w.movement.steer(e, 0, 0, 0);
            if (!e.aiming) e.angle = World.turn(e.angle, face, 0.1f);
        }
    }
}
