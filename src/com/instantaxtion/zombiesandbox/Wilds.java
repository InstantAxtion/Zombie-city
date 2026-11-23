package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Out of town: wildlife, birds, hikers, rangers, lookout towers.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class Wilds {
    private final World w;

    Wilds(World w) {
        this.w = w;
    }

    /** Flocks of pigeons in parks and plazas. */
    void spawnBirds() {
        // (No animals since 10.18.)
        w.birds.clear();
        if (w.birds.isEmpty()) return;
        int flocks = Math.min(10, w.city.openAreas.size() + 3);
        for (int f = 0; f < flocks; f++) {
            float[] c;
            if (f < w.city.openAreas.size()) c = w.city.findWalkable(w.city.openAreas.get(f)[0], w.city.openAreas.get(f)[1]);
            else c = w.city.randomWalkable(w.rnd);
            if (c == null) continue;
            int n = 4 + w.rnd.nextInt(5);
            for (int i = 0; i < n; i++) {
                World.Bird b = new World.Bird();
                b.x = c[0] + w.rnd.nextFloat() * 30 - 15;
                b.y = c[1] + w.rnd.nextFloat() * 30 - 15;
                b.flap = w.rnd.nextFloat() * 6;
                w.birds.add(b);
            }
        }
    }

    /** Startles pigeons near a point into the air. */
    void scareBirds(float x, float y, float radius) {
        for (int i = 0, n = w.birds.size(); i < n; i++) {
            World.Bird b = w.birds.get(i);
            if (b.flying) continue;
            float ddx = b.x - x, ddy = b.y - y;
            if (ddx * ddx + ddy * ddy > radius * radius) continue;
            b.flying = true;
            float[] land = w.city.randomWalkable(w.rnd);
            float a = (float) Math.atan2(ddy, ddx) + (w.rnd.nextFloat() - 0.5f);
            // Fly away from the scare, landing somewhere open a few hundred units off.
            b.tx = Math.max(0, Math.min(w.city.worldW(), b.x + (float) Math.cos(a) * (200 + w.rnd.nextFloat() * 250)));
            b.ty = Math.max(0, Math.min(w.city.worldH(), b.y + (float) Math.sin(a) * (200 + w.rnd.nextFloat() * 250)));
            float[] p = w.city.findWalkable(b.tx, b.ty);
            if (p == null) p = land;
            b.tx = p[0] + w.rnd.nextFloat() * 20 - 10;
            b.ty = p[1] + w.rnd.nextFloat() * 20 - 10;
        }
    }

    void updateBirds(float dt) {
        for (int i = 0, n = w.birds.size(); i < n; i++) {
            World.Bird b = w.birds.get(i);
            b.flap += dt * (b.flying ? 18 : 2);
            if (b.flying) {
                float ddx = b.tx - b.x, ddy = b.ty - b.y;
                float d = (float) Math.sqrt(ddx * ddx + ddy * ddy);
                if (d < 3) {
                    b.flying = false;
                } else {
                    float sp = Math.min(d, 90 * dt);
                    b.x += ddx / d * sp;
                    b.y += ddy / d * sp;
                }
                continue;
            }
            // Peck about, and take off if anyone comes close.
            b.timer -= dt;
            if (b.timer <= 0) {
                b.timer = 0.3f + w.rnd.nextFloat() * 0.6f;
                if (w.rnd.nextFloat() < 0.3f) {
                    float nx = b.x + w.rnd.nextFloat() * 6 - 3, ny = b.y + w.rnd.nextFloat() * 6 - 3;
                    if (!w.city.solidAt(nx, ny)) {
                        b.x = nx;
                        b.y = ny;
                    }
                }
                int cx = Math.max(0, Math.min(w.gw - 1, (int) (b.x / World.CELL))), cy = Math.max(0, Math.min(w.gh - 1, (int) (b.y / World.CELL)));
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c], end = w.cellStart[c] + w.cellCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    float ddx = o.x - b.x, ddy = o.y - b.y;
                    if (ddx * ddx + ddy * ddy < 20 * 20) {
                        scareBirds(b.x, b.y, 40);
                        break;
                    }
                }
            }
        }
    }

    void spawnWildlife() {
        w.animals.clear();
        // (No animals since 10.18.)
        if (w.animals.isEmpty()) return;
        if (w.city.settlements.isEmpty() && w.city.townX0 <= 0) return;
        int herds = w.city.w * w.city.h / 2600;
        for (int h = 0; h < herds; h++) {
            float[] p = null;
            for (int t = 0; t < 30 && p == null; t++) {
                int tx = w.rnd.nextInt(w.city.w), ty = w.rnd.nextInt(w.city.h);
                if (tx >= w.city.townX0 && tx < w.city.townX1 && ty >= w.city.townY0 && ty < w.city.townY1) continue;
                if (w.city.tiles[ty * w.city.w + tx] != City.GRASS) continue;
                p = new float[]{tx * City.T + 8, ty * City.T + 8};
            }
            if (p == null) continue;
            boolean fox = w.rnd.nextFloat() < 0.35f;
            int n = fox ? 1 : 2 + w.rnd.nextInt(4);
            for (int k = 0; k < n; k++) {
                World.Animal a = new World.Animal();
                a.kind = fox ? 1 : 0;
                a.x = p[0] + w.rnd.nextFloat() * 20 - 10;
                a.y = p[1] + w.rnd.nextFloat() * 20 - 10;
                a.angle = w.rnd.nextFloat() * World.TAU;
                w.animals.add(a);
            }
        }
    }

    void updateWildlife(float dt) {
        w.wildlifeSaidCd -= dt;
        for (int i = w.animals.size() - 1; i >= 0; i--) {
            World.Animal a = w.animals.get(i);
            if (a.dead) {
                w.animals.remove(i);
                continue;
            }
            a.timer -= dt;
            a.flee -= dt;
            // Anything coming? The dead from a long way off, people closer.
            // (Nothing dead within a walk of here at all: no need to look.)
            Entity z = w.city.fieldAt(w.city.zombieDist, a.x, a.y) > 130 / City.T + 4 ? null : w.spatial.nearestZombie(a.x, a.y, 130);
            if (z != null) {
                float dx = a.x - z.x, dy = a.y - z.y, d = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
                if (d < 6) {
                    // Caught. A fox comes back as one of them.
                    a.dead = true;
                    if (a.kind == 1 && w.rnd.nextFloat() < 0.6f) w.population.spawn(Entity.ZOMBIE_DOG, a.x, a.y);
                    else w.effects.bloodBurst(a.x, a.y, 4, 0, 0);
                    continue;
                }
                if (a.flee <= 0 && a.kind == 0 && w.wildlifeSaidCd <= 0 && w.spatial.peopleNear(a.x, a.y, 260)) {
                    // Deer bolting: an early warning (if anyone is about to see them go).
                    w.wildlifeSaidCd = 60;
                    City.District dd = w.city.districtOf(a.x, a.y);
                    w.dispatch.say(Dispatch.WHO_INFO, null, "Deer are bolting out of the woods near " + (dd != null ? dd.name : w.city.placeName(a.x, a.y))
                            + ". Something is coming.", a.x, a.y);
                }
                a.flee = 3;
                a.vx = dx / d;
                a.vy = dy / d;
            } else if (a.flee <= 0 && w.rnd.nextFloat() < dt * 0.5f && w.spatial.peopleNear(a.x, a.y, 50)) {
                a.flee = 1.5f;
                a.angle = w.rnd.nextFloat() * World.TAU;
                a.vx = (float) Math.cos(a.angle);
                a.vy = (float) Math.sin(a.angle);
            }
            float speed;
            if (a.flee > 0) speed = a.kind == 0 ? 95 : 70;
            else {
                // Grazing: a few steps now and then.
                if (a.timer <= 0) {
                    a.timer = 2 + w.rnd.nextFloat() * 5;
                    float ang = w.rnd.nextFloat() * World.TAU;
                    a.vx = w.rnd.nextFloat() < 0.5f ? 0 : (float) Math.cos(ang);
                    a.vy = a.vx == 0 ? 0 : (float) Math.sin(ang);
                }
                speed = a.kind == 0 ? 8 : 12;
            }
            float nx = a.x + a.vx * speed * dt, ny = a.y + a.vy * speed * dt;
            if (!w.city.circleBlocked(nx, ny, 3) && nx > 4 && ny > 4 && nx < w.city.worldW() - 4 && ny < w.city.worldH() - 4) {
                a.x = nx;
                a.y = ny;
            } else {
                a.vx = -a.vx;
                a.vy = -a.vy;
            }
            if (a.vx != 0 || a.vy != 0) a.angle = (float) Math.atan2(a.vy, a.vx);
        }
    }

    /**
     * A fire lookout on duty: up in the cab, pacing round the windows. From up there they see the dead coming
     * a long way off and phone it in; the dead have to climb the stairs to get them. True while they're up there.
     */
    boolean lookout(Entity e, float dt) {
        float[] t = e.spot;
        if (t == null || e.jobStep < 0 || e.jobStep >= w.city.fireTowers.size()) {
            e.aloft = false;
            e.job = Entity.J_HOMEBODY;
            return false;
        }
        float dx = t[0] - e.x, dy = t[1] - e.y, d = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
        if (d > 16 && !e.aloft) {
            // (Knocked off somehow: back up the stairs.)
            walkToSpot(e, t[0], t[1], e.speed);
            return true;
        }
        e.aloft = true;
        e.jobTimer += dt;
        float a = e.jobTimer * 0.18f + (e.nameSeed & 7);
        float px = t[0] + (float) Math.cos(a) * 3.5f, py = t[1] + (float) Math.sin(a) * 3.5f;
        float qx = px - e.x, qy = py - e.y, qd = (float) Math.sqrt(qx * qx + qy * qy) + 0.001f;
        if (qd > 0.8f) w.movement.steer(e, qx / qd, qy / qd, Math.min(e.speed * 0.3f, qd * 3));
        else w.movement.steer(e, 0, 0, 0);
        // Binoculars out: anything moving on the hillsides.
        if (w.zombies > 0 && e.callCd <= 0 && w.rnd.nextFloat() < dt * 0.6f) {
            Entity z = w.spatial.nearestZombie(e.x, e.y, 520);
            if (z != null) {
                e.callCd = 50;
                e.aware = true;
                e.angle = (float) Math.atan2(z.y - e.y, z.x - e.x);
                w.dispatch.call(e, z);
                if (w.rnd.nextFloat() < 0.5f)
                    w.dispatch.say(Dispatch.WHO_911, null, "\"This is " + w.city.fireTowerNames.get(e.jobStep) + ". I can see "
                            + Math.max(1, w.spatial.countZombiesNear(z.x, z.y, 120)) + " of them down by " + w.city.placeName(z.x, z.y) + ".\"", z.x, z.y);
            }
        }
        return true;
    }

    /** Lookout shifts, and the rescue helicopter called out to anyone cut off out in the wilds. */
    void updateTowers(float dt) {
        w.wildsTick -= dt;
        if (w.wildsTick > 0 || w.towerKeeper == null) return;
        float step = 1 - w.wildsTick;
        w.wildsTick = 1;
        for (int k = 0; k < w.towerKeeper.length; k++) {
            Entity keep = w.towerKeeper[k];
            if (keep != null && (keep.dead || keep.type != Entity.CIVILIAN || keep.job != Entity.J_LOOKOUT)) {
                keep.aloft = false;
                w.towerKeeper[k] = keep = null;
            }
            Entity rel = w.towerRelief[k];
            if (rel != null && (rel.dead || rel.type != Entity.CIVILIAN || rel.job != Entity.J_RELIEF)) w.towerRelief[k] = rel = null;
            w.towerShift[k] -= step;
            // The next shift sets off from the ranger station (nobody comes up once the dead are about).
            if ((w.towerShift[k] <= 0 || keep == null) && rel == null && w.alert < 1) {
                w.towerShift[k] = 60;
                float[] from = w.rangerStation != null ? new float[]{w.rangerStation.doorX, w.rangerStation.doorY}
                        : w.city.trailheadX > 0 ? new float[]{w.city.trailheadX, w.city.trailheadY} : null;
                if (from == null) continue;
                Entity r = w.population.spawn(Entity.CIVILIAN, from[0] + w.rnd.nextFloat() * 10 - 5, from[1] + w.rnd.nextFloat() * 10 - 5);
                if (r == null) continue;
                r.job = Entity.J_RELIEF;
                r.jobStep = k;
                if (w.rangerStation != null) r.home = w.rangerStation;
                r.homeChecked = true;
                w.towerRelief[k] = r;
            }
        }
        w.rescueCd -= step;
        if (w.rescueCd <= 0) {
            w.rescueCd = 6;
            w.rescue.callRescue();
        }
    }

    /** The relief is at the top of the stairs: they take over, and the one coming off shift heads home. */
    void takeOverTower(Entity e, int k) {
        Entity old = w.towerKeeper[k];
        if (old != null && old != e) {
            old.aloft = false;
            old.job = Entity.J_OFFDUTY;
            old.spot = null;
            old.errand = null;
            old.errandTimer = 0;
        }
        w.towerRelief[k] = null;
        w.population.makeLookout(e, k);
        w.towerShift[k] = 240 + w.rnd.nextFloat() * 180;
    }

    /** Picks a trail walk starting near this person (or any, for those who drive out to it). */
    boolean pickHike(Entity e) {
        if (w.city.hikes.isEmpty()) return false;
        int best = -1;
        float bd = Float.MAX_VALUE;
        for (int i = 0; i < w.city.hikes.size(); i++) {
            float[] h = w.city.hikes.get(i);
            float d = (float) World.hypot(h[0] - e.x, h[1] - e.y) + w.rnd.nextFloat() * 400;
            if (d < bd) {
                bd = d;
                best = i;
            }
        }
        if (best < 0) return false;
        e.hike = w.city.hikes.get(best);
        e.hikeIdx = 0;
        e.hikeDir = 1;
        e.jobStep = 0;
        e.jobTimer = 0;
        return true;
    }

    /**
     * Along a trail: to where it starts, out along it to the end (a rest there to take in the view), and back.
     * False once they're back where they started.
     */
    boolean followHike(Entity e, float speed, float dt) {
        float[] h = e.hike;
        if (h == null) return false;
        int n = h.length / 2;
        if (e.jobStep == 0) {
            float sx = h[0] - e.x, sy = h[1] - e.y;
            if (sx * sx + sy * sy > 14 * 14) {
                walkToSpot(e, h[0], h[1], speed);
                return true;
            }
            e.jobStep = 1;
            e.hikeIdx = 0;
            e.hikeDir = 1;
        }
        if (e.jobStep == 2) {
            e.jobTimer -= dt;
            w.movement.steer(e, 0, 0, 0);
            if (e.jobTimer <= 0) {
                e.jobStep = 1;
                e.hikeDir = -1;
            }
            return true;
        }
        int k = Math.max(0, Math.min(n - 1, e.hikeIdx));
        // (Past the point aimed at counts too: on a bend the one before it may be left a little way off.)
        for (int j = 2; j >= 1; j--) {
            int a = k + e.hikeDir * j;
            if (a < 0 || a >= n) continue;
            float ax = h[a * 2] - e.x, ay = h[a * 2 + 1] - e.y;
            if (ax * ax + ay * ay < 12 * 12) {
                k = a;
                e.hikeIdx = k;
                break;
            }
        }
        float px = h[k * 2] - e.x, py = h[k * 2 + 1] - e.y;
        if (px * px + py * py < 9 * 9 || e.stuckTime > 2.5f) {
            e.stuckTime = 0;
            k += e.hikeDir;
            if (k >= n) {
                // The end: the view from the top, the falls, the lake shore.
                e.hikeIdx = n - 1;
                e.jobStep = 2;
                e.jobTimer = 12 + w.rnd.nextFloat() * 20;
                return true;
            }
            if (k < 0) return false;
            e.hikeIdx = k;
        }
        // Aim a couple of points on, so the walk follows the bends smoothly.
        int a = Math.max(0, Math.min(n - 1, k + e.hikeDir * 2));
        float tx = h[a * 2] - e.x, ty = h[a * 2 + 1] - e.y, td = (float) Math.sqrt(tx * tx + ty * ty) + 0.001f;
        w.movement.steer(e, tx / td, ty / td, speed);
        return true;
    }

    /** Walks to a spot by a planned route (round buildings and water), or straight there when it's close. */
    void walkToSpot(Entity e, float x, float y, float speed) {
        // ((17.1) Into, out of or through a building: by its doors.)
        if (World.WALK_IN && w.indoors.indoorMove(e, x, y, speed)) return;
        float dx = x - e.x, dy = y - e.y, d = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
        // ((12.0) Arriving: easing off over the last few steps and stopping on the spot, instead of running round and
        // round it at full speed; and a spot right up against a wall counts as reached once they're against it.)
        if (d < 3 || (e.blocked && d < 14)) {
            w.movement.steer(e, 0, 0, 0);
            e.blocked = false;
            return;
        }
        if (d < 26 && !e.blocked) {
            w.movement.steer(e, dx / d, dy / d, Math.min(speed, d * 3 + 3));
            return;
        }
        // (10.49) A spot that's crept a little (a cordon post as the zone shifts) keeps the way already planned
        // while it's still far off: replanned as it gets near.
        // ((31.0) Up to a tile and a half: a guard's place on a safe zone's wall shifts a little each time one comes or goes,
        // and every guard on every wall planned the whole way again, most steps - the biggest part of each step in an outbreak.)
        float slack = Math.max(e.task == Dispatch.T_GUARD ? City.T * 1.5f : 1, d * 0.12f);
        // ((14.9) A way that couldn't be found is looked for again every few seconds: it used to be left empty for
        // good, and they walked straight at the spot - into the wall of whatever building was in the way - for ever.)
        boolean retry = e.path != null && e.path.length == 0 && w.time >= e.pathRetry;
        if (e.path == null || retry || e.pathDest != null || Math.abs(e.planX - x) > slack || Math.abs(e.planY - y) > slack) {
            if (w.pathBudget < 0.25f) {
                steerClear(e, dx / d, dy / d, speed * 0.7f);
                return;
            }
            w.pathBudget -= 0.25f;
            e.path = w.city.findPath(e.x, e.y, x, y, 9000);
            // ((14.9) A spot inside a building or a wall: the nearest open ground to it instead.)
            if (e.path == null && w.city.solidAt(x, y)) {
                float[] p = w.city.findWalkable(x, y);
                if (p != null) e.path = w.city.findPath(e.x, e.y, p[0], p[1], 9000);
            }
            if (e.path == null) {
                e.path = new int[0];
                e.pathRetry = w.time + 3 + w.rnd.nextFloat() * 2;
            }
            e.pathIdx = 0;
            e.pathDest = null;
            e.planX = x;
            e.planY = y;
        }
        e.blocked = false;
        if (!w.movement.followPath(e, speed)) steerClear(e, dx / d, dy / d, speed);
    }

    /**
     * (14.9) Straight that way (dx, dy) - or, with a building or a wall just ahead, along it whichever way is nearest
     * to the right one (and keeps on the way they were already sliding), instead of pushing into it. People caught
     * in the seam where two buildings meet used to stand there running into the wall.
     */
    void steerClear(Entity e, float dx, float dy, float speed) {
        float ahead = e.radius + 5;
        if (!w.city.solidAt(e.x + dx * ahead, e.y + dy * ahead)) {
            w.movement.steer(e, dx, dy, speed);
            return;
        }
        int tx = City.fl(e.x / City.T), ty = City.fl(e.y / City.T);
        float best = -9, bx = dx, by = dy;
        for (int oy = -1; oy <= 1; oy++)
            for (int ox = -1; ox <= 1; ox++) {
                if (ox == 0 && oy == 0 || w.city.solidTile(tx + ox, ty + oy)) continue;
                if (ox != 0 && oy != 0 && (w.city.solidTile(tx + ox, ty) || w.city.solidTile(tx, ty + oy))) continue;
                float gx = (tx + ox + 0.5f) * City.T - e.x, gy = (ty + oy + 0.5f) * City.T - e.y;
                float gl = (float) Math.sqrt(gx * gx + gy * gy) + 0.001f;
                gx /= gl;
                gy /= gl;
                float score = gx * dx + gy * dy + 0.45f * (gx * e.mx + gy * e.my);
                if (score > best) {
                    best = score;
                    bx = gx;
                    by = gy;
                }
            }
        w.movement.steer(e, bx, by, speed);
    }

    /** A spot on a lake shore near home to fish from: {x, y, facing}. */
    float[] fishingSpot(Entity e) {
        float[] best = null;
        float bd = 1400;
        for (int i = 0; i < w.city.natureSpots.size(); i++) {
            float[] q = w.city.natureSpots.get(i);
            if (q[2] != City.NL_LAKE) continue;
            float d = (float) World.hypot(q[0] - e.x, q[1] - e.y);
            if (d > bd) continue;
            // Round the shore from the lake's middle, until it's dry land beside open water.
            float a = (e.nameSeed & 0xFFFF) / 65536f * World.TAU;
            for (int tries = 0; tries < 8; tries++, a += 0.8f)
                for (int r = 2; r < 18; r++) {
                    int tx = (int) (q[0] / City.T + Math.cos(a) * r), ty = (int) (q[1] / City.T + Math.sin(a) * r);
                    if (tx < 1 || ty < 1 || tx >= w.city.w - 1 || ty >= w.city.h - 1) break;
                    int t = ty * w.city.w + tx;
                    if (w.city.tiles[t] == City.WATER) continue;
                    if (w.city.solid[t]) break;
                    float fx = (tx + 0.5f) * City.T, fy = (ty + 0.5f) * City.T;
                    best = new float[]{fx, fy, (float) Math.atan2(q[1] - fy, q[0] - fx)};
                    bd = d;
                    break;
                }
        }
        return best;
    }

    /** A seat round a campfire (or at a picnic table) near home: {x, y, facing}. */
    float[] campfireSpot(Entity e, City.Building home) {
        float hx = home != null ? home.doorX : e.x, hy = home != null ? home.doorY : e.y;
        float[] best = null;
        float bd = 260;
        boolean picnic = (e.nameSeed & 3) == 0;
        for (float[] d : w.city.decor) {
            if ((int) d[0] != (picnic ? City.D_PICNIC : City.D_CAMPFIRE)) continue;
            float cx = (d[1] + d[3]) / 2, cy = (d[2] + d[4]) / 2, dist = (float) World.hypot(cx - hx, cy - hy);
            if (dist > bd) continue;
            bd = dist;
            float a = (e.nameSeed & 0xFFF) / 4096f * World.TAU;
            best = new float[]{cx + (float) Math.cos(a) * 10, cy + (float) Math.sin(a) * 10, a + (float) Math.PI};
        }
        return best;
    }

    /**
     * A spot in front of the ranger station: {@code dist} out from the door, {@code a} radians either side of
     * straight out, nudged back towards the door if that lands on something solid. Third value: facing out.
     */
    float[] stationSpot(float a, float dist) {
        City.Building b = w.rangerStation;
        float cx = (b.x0 + b.x1) / 2f, cy = (b.y0 + b.y1) / 2f;
        float hw = Math.max(1, (b.x1 - b.x0) / 2f), hh = Math.max(1, (b.y1 - b.y0) / 2f);
        float rx = (b.doorX - cx) / hw, ry = (b.doorY - cy) / hh;
        float nx = 0, ny = 0;
        if (Math.abs(rx) > Math.abs(ry)) nx = Math.signum(rx);
        else ny = ry == 0 ? 1 : Math.signum(ry);
        float out = (float) Math.atan2(ny, nx) + a;
        for (float d = dist; d >= 10; d -= 6) {
            float px = b.doorX + (float) Math.cos(out) * d, py = b.doorY + (float) Math.sin(out) * d;
            if (!w.city.solidAt(px, py) && (px < b.x0 - 4 || px > b.x1 + 4 || py < b.y0 - 4 || py > b.y1 + 4)) return new float[]{px, py, out};
        }
        return new float[]{b.doorX + nx * 12, b.doorY + ny * 12, out - a};
    }

    /** A park ranger on foot: walking the trails, or with the dead about, holding the station. */
    boolean rangerOnFoot(Entity e, float dt) {
        if (w.alert >= 1 && w.rangerStation != null) {
            // Holding the station: a fan out in front of the door, facing out (not lined up against the wall).
            float a = ((e.callsign * 0.618034f) % 1f - 0.5f) * 2.4f;
            float[] p = stationSpot(a, 30 + (e.callsign % 3) * 8);
            if (World.hypot(p[0] - e.x, p[1] - e.y) > 300) walkToSpot(e, p[0], p[1], e.speed);
            else {
                w.holdoutLife.standAt(e, p[0], p[1], 0.8f);
                if (e.want < 1) e.angle = World.turn(e.angle, p[2], dt * 3);
            }
            return true;
        }
        // One ranger always minds the station: on the porch, or out at the trailhead in front of it.
        if (w.rangerStation != null && !w.rangerStation.collapsed) {
            if (w.stationKeeper == null || w.stationKeeper.dead || w.stationKeeper.agency != 3 || w.stationKeeper.rig != null
                    || w.stationKeeper.task != Dispatch.T_NONE)
                w.stationKeeper = e;
            if (w.stationKeeper == e) {
                e.hike = null;
                int phase = (int) (w.time / 30) % 3;
                float[] p = phase == 0 ? stationSpot(0, 16) : phase == 1 ? stationSpot(0.9f, 34) : stationSpot(-0.7f, 46);
                if (World.hypot(p[0] - e.x, p[1] - e.y) > 300) walkToSpot(e, p[0], p[1], e.speed);
                else {
                    w.holdoutLife.standAt(e, p[0], p[1], 0.6f);
                    if (e.want < 1) e.angle = World.turn(e.angle, p[2], dt * 2);
                }
                return true;
            }
        }
        if (w.city.hikes.isEmpty()) return false;
        if (e.hike == null && !pickHike(e)) return false;
        if (followHike(e, e.speed * 0.8f, dt)) return true;
        e.hike = null;
        return true;
    }
}
