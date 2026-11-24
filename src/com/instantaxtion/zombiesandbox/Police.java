package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * The police's own ways: beats on foot, mounted and traffic officers, junctions held.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class Police {
    /** ((28.5) Reseeded for each step of a beat (policeBeat). */
    private final Random beatRnd = new Random();
    private final World w;

    Police(World w) {
        this.w = w;
    }

    /**
     * (15.0) The traffic officer working junction id, there and on duty, or null. They hold the other traffic for
     * anything with its lights on, so it goes straight through.
     */
    Entity trafficOfficer(int id) {
        if (w.trafficPost == null && !w.trafficLooked) w.population.relinkTraffic();
        if (w.trafficPost == null || id < 0 || id >= w.trafficPost.length) return null;
        Entity t = w.trafficPost[id];
        if (t == null) return null;
        if (t.dead || t.task != Dispatch.T_POST || World.hypot(t.x - t.postX, t.y - t.postY) > 20) {
            if (t.dead || t.task != Dispatch.T_POST) w.trafficPost[id] = null;
            return null;
        }
        return t;
    }

    /** (15.0) A traffic officer at junction id waves something with its lights on through: the rest wait. */
    void holdJunction(int id, Fleet.Vehicle v) {
        Entity t = trafficOfficer(id);
        if (t == null) return;
        w.junctionHeld[id] = w.time + 2.5f;
        t.waveUntil = w.time + 1.5f;
        t.angle = (float) Math.atan2(v.y - t.y, v.x - t.x);
    }

    /** (15.0) Is junction id being held for something coming through by its traffic officer? */
    boolean junctionHeldFor(int id) {
        return w.junctionHeld != null && id >= 0 && id < w.junctionHeld.length && w.junctionHeld[id] > w.time && trafficOfficer(id) != null;
    }

    /** (15.0) Marks the dead within r of (x, y) for the units nearby (as a drone or the police helicopter does). */
    void markSpotted(float x, float y, float r) {
        int cx0 = Math.max(0, (int) ((x - r) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((x + r) / World.CELL));
        int cy0 = Math.max(0, (int) ((y - r) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((y + r) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c], end = k + w.zCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (!o.dead && (o.x - x) * (o.x - x) + (o.y - y) * (o.y - y) < r * r) o.spottedUntil = w.time + 1.6f;
                }
            }
    }

    /**
     * (15.0) The mounted and traffic police: a traffic officer stays on their junction till the dead come near or
     * the city's being lost, then joins in like anyone; a mounted officer looks round from up on the horse
     * (marking the dead for those round them) and, with nothing doing, rides round the parks and open ground near
     * them. True while busy with that.
     */
    boolean specialPolice(Entity e, Entity threat, float dt) {
        if (e.role == Entity.ROLE_TRAFFIC && e.task == Dispatch.T_POST) {
            // (Through the emergency itself - that's when the lights and sirens are out - till the dead come near, or
            // the city's being lost.)
            if ((w.warBalance < 0.35f && w.time > 30) || w.spatial.countZombiesNear(e.x, e.y, 140) > 0) {
                e.task = Dispatch.T_NONE;
                return false;
            }
            if (World.hypot(e.postX - e.x, e.postY - e.y) > 4) w.wilds.walkToSpot(e, e.postX, e.postY, e.speed);
            else w.movement.steer(e, 0, 0, 0);
            return true;
        }
        if (e.role != Entity.ROLE_MOUNTED) return false;
        if (e.task != Dispatch.T_NONE || threat != null || e.leader != null) return false;
        if (e.patrolX == 0 || World.hypot(e.patrolX - e.x, e.patrolY - e.y) < 20 || e.blocked && w.rnd.nextFloat() < 0.02f) {
            // Somewhere green or open not far off: a park, a trail, a square.
            for (int k = 0; k < 40; k++) {
                float a = w.rnd.nextFloat() * World.TAU, d = 150 + w.rnd.nextFloat() * 550;
                float px = e.x + (float) Math.cos(a) * d, py = e.y + (float) Math.sin(a) * d;
                int t = w.city.tileIndex(px, py);
                if (t < 0 || w.city.solid[t]) continue;
                byte tile = w.city.tiles[t];
                // (Never the rubble of a fallen building: it's a building site once the rebuilding starts.)
                if (tile != City.GRASS && tile != City.TRAIL && tile != City.PLAZA) continue;
                e.patrolX = px;
                e.patrolY = py;
                break;
            }
        }
        if (e.patrolX == 0) return false;
        w.wilds.walkToSpot(e, e.patrolX, e.patrolY, e.speed);
        return true;
    }

    /**
     * (11.1) A police officer's beat: out of the precinct in pairs, along the streets round it from one spot to
     * the next, a few moments at each, then on. With the city under attack it's a tighter beat round the precinct,
     * and one in three stays guarding the precinct itself (see thinkArmed). False if there's no precinct to work
     * from (or this one is on guard).
     */
    boolean policeBeat(Entity e, float dt) {
        City.Facility st = w.city.nearestFacility(City.FACILITY_POLICE, e.x, e.y);
        if (st == null) return false;
        // ((11.4) Mostly car patrols: only one pair in four walks a beat. The rest head back in to the precinct to
        // crew a car (see Fleet.staffPatrols).)
        // ((31.0) Map version 50: no beats on foot at all - back to the precinct for a car (a lift there, from a long way
        // off), and out in it.)
        // ((32.0) Map version 53: nor anyone else - the K9 handlers, riot officers and the other agencies' officers still
        // walked theirs.)
        boolean noBeat = (w.city.cfg.civic >= 50 && e.agency == 0 && e.role == Entity.ROLE_RIFLE)
                || (w.city.cfg.civic >= 53 && e.role != Entity.ROLE_MOUNTED && e.role != Entity.ROLE_TRAFFIC);
        if (noBeat || (!World.footPatrol(e) && e.agency == 0 && e.role == Entity.ROLE_RIFLE && w.fleet.patrols.parkedAt(st) > 0
                && (w.city.cfg.civic < 50 || w.fleet.patrolCount < w.fleet.patrolTarget))) {
            float gx = st.gateX > 0 ? st.gateX : st.x, gy = st.gateY > 0 ? st.gateY : st.y;
            float bx = gx - e.x, by = gy - e.y;
            if (noBeat && bx * bx + by * by > World.RIDE_HOME * World.RIDE_HOME && e.rig == null && e.ride == null && (e.driveCd -= dt) <= 0) {
                e.driveCd = 6;
                java.util.ArrayList<Entity> group = new java.util.ArrayList<Entity>();
                group.add(e);
                for (int i = 0, n = w.spatial.gatherLiving(e.x, e.y, 150); i < n && group.size() < 4; i++) {
                    Entity o = w.gathered[i];
                    if (o != e && !o.dead && o.type == Entity.COP && o.task == Dispatch.T_NONE && o.rig == null && o.ride == null) group.add(o);
                }
                if (w.fleet.driveTo(group, null)) return true;
            }
            // ((31.0) Waiting for a car: each at a spot of their own round the yard, in two or three rings, not all on the
            // gate - a crowd of a hundred there, shoving, was a quarter of every step.)
            if (noBeat && bx * bx + by * by < (st.r + 200) * (st.r + 200)) {
                float a = (e.callsign * 2.39996f) % World.TAU, r = st.r * 0.55f + (e.callsign % 4) * 9;
                float px = st.x + (float) Math.cos(a) * r, py = st.y + (float) Math.sin(a) * r;
                if (!w.city.solidAt(px, py)) {
                    e.dutyFor = 0;
                    e.dutyHx = e.dutyHy = 0;
                    w.holdoutLife.standAt(e, px, py, 0.6f);
                    return true;
                }
            }
            e.dutyFor = 0;
            e.dutyHx = e.dutyHy = 0;
            if (bx * bx + by * by > 40 * 40) w.wilds.walkToSpot(e, gx, gy, e.speed);
            else w.movement.steer(e, 0, 0, 0);
            return true;
        }
        float reach = w.alert >= 1 ? 320 : 760;
        // ((11.2) Partners: the one with the higher callsign keeps to the other's side, a step behind.)
        Entity mate = beatMate(e, st);
        if (mate != null && mate.callsign < e.callsign) {
            float mx = mate.x - e.x, my = mate.y - e.y;
            if (mx * mx + my * my > 300 * 300) {
                w.wilds.walkToSpot(e, mate.x, mate.y, e.runSpeed * 0.8f);
                e.dutyFor = 1;
                return true;
            }
            // (Beside the lead as it walks - or just behind, where beside is a wall or the road.)
            e.dutyStep = mate.dutyStep;
            e.dutyHx = mate.dutyHx;
            e.dutyHy = mate.dutyHy;
            float fx = mate.x - 10 * mate.dutyHy - 4 * mate.dutyHx, fy = mate.y + 10 * mate.dutyHx - 4 * mate.dutyHy;
            int ft = w.city.tileIndex(fx, fy);
            if (ft < 0 || (w.city.tiles[ft] != City.SIDEWALK && w.city.tiles[ft] != City.PLAZA)) {
                fx = mate.x - 12 * mate.dutyHx;
                fy = mate.y - 12 * mate.dutyHy;
            }
            e.dutyX = fx;
            e.dutyY = fy;
            e.dutyFor = mate.dutyFor;
            float tx = fx - e.x, ty = fy - e.y, d2 = tx * tx + ty * ty;
            if (d2 < 5 * 5) {
                w.movement.steer(e, 0, 0, 0);
                if (e.dutyHx != 0 || e.dutyHy != 0) e.angle = World.turn(e.angle, (float) Math.atan2(e.dutyHy, e.dutyHx), dt * 2);
                return true;
            }
            if (d2 < 30 * 30 && !e.blocked) {
                float d = (float) Math.sqrt(d2);
                w.movement.steer(e, tx / d, ty / d, Math.min(e.speed, d * 2));
            } else w.wilds.walkToSpot(e, fx, fy, e.speed);
            return true;
        }
        if (mate != null) {
            // (The lead waits up for a partner who's fallen behind.)
            float mx = mate.x - e.x, my = mate.y - e.y;
            if (mx * mx + my * my > 90 * 90 && mx * mx + my * my < 300 * 300) {
                w.movement.steer(e, 0, 0, 0);
                return true;
            }
        }
        float side = mate != null ? 0 : (e.callsign & 1) == 0 ? 6 : -6;
        float tx = e.dutyX - e.x, ty = e.dutyY - e.y;
        boolean there = e.dutyFor > 0 && tx * tx + ty * ty < 22 * 22;
        e.dutyFor -= dt;
        if (there && e.dutyWait > 0) {
            // (Now and then a stop: a look up and down the street, a word at a shop door.)
            e.dutyWait -= dt;
            w.movement.steer(e, 0, 0, 0);
            if (e.dutyHx != 0 || e.dutyHy != 0) e.angle = World.turn(e.angle, (float) Math.atan2(e.dutyHy, e.dutyHx), dt * 2);
            return true;
        }
        if (e.dutyFor <= 0 || there || World.hypot(e.dutyX - st.x, e.dutyY - st.y) > reach + 60) {
            // ((11.2) Walked along the streets: on down the block, turning at a corner now and then and back round
            // towards the precinct at the end of the beat - not from one random spot to another. Partners keep
            // together: the next spot comes from their pair, the step and the spot they're both at.)
            e.dutyStep++;
            // ((28.5) One Random, seeded afresh each time - the same numbers as a new one, without making one.)
            Random r = beatRnd;
            r.setSeed((long) e.callsign / 2 * 7919 + e.dutyStep * 104729L + (long) st.x);
            boolean fresh = e.dutyFor < -30 || (e.dutyHx == 0 && e.dutyHy == 0);
            // (A fresh beat sets off from the precinct's door - or from wherever they are, if that's nowhere near it.)
            float gx = st.gateX > 0 ? st.gateX : st.x, gy = st.gateY > 0 ? st.gateY : st.y;
            if (fresh && World.hypot(e.x - gx, e.y - gy) > 200) {
                gx = e.x;
                gy = e.y;
            }
            float bx = fresh ? gx : e.dutyX - side, by = fresh ? gy : e.dutyY;
            float hx = e.dutyHx, hy = e.dutyHy;
            if (fresh) {
                int d0 = r.nextInt(4);
                hx = d0 == 0 ? 1 : d0 == 1 ? -1 : 0;
                hy = d0 == 2 ? 1 : d0 == 3 ? -1 : 0;
            }
            // The ways to go, best first: straight on (or a turn now and then), or round towards the precinct
            // when it's at the edge of the beat; back the way they came last of all.
            float[][] dirs = {{hx, hy}, {-hy, hx}, {hy, -hx}, {-hx, -hy}};
            if (r.nextFloat() < 0.3f) {
                float[] t0 = dirs[0];
                int k = r.nextBoolean() ? 1 : 2;
                dirs[0] = dirs[k];
                dirs[k] = t0;
            }
            if (World.hypot(bx - st.x, by - st.y) > reach * 0.75f) {
                final float sx = st.x - bx, sy = st.y - by;
                java.util.Arrays.sort(dirs, 0, 3, new java.util.Comparator<float[]>() {
                    public int compare(float[] a, float[] b) {
                        return Float.compare(b[0] * sx + b[1] * sy, a[0] * sx + a[1] * sy);
                    }
                });
            }
            float[] p = null;
            for (int k = 0; k < dirs.length && p == null; k++) {
                float step = 90 + r.nextFloat() * 90;
                float[] q = w.city.findWalkable(bx + dirs[k][0] * step, by + dirs[k][1] * step);
                int t = q == null ? -1 : w.city.tileIndex(q[0], q[1]);
                if (t < 0 || (w.city.tiles[t] != City.SIDEWALK && w.city.tiles[t] != City.PLAZA) || w.city.outOfTown(q[0], q[1])
                        || World.hypot(q[0] - st.x, q[1] - st.y) > reach + 40 || !openBetween(bx, by, q[0], q[1])) continue;
                p = q;
                hx = dirs[k][0];
                hy = dirs[k][1];
            }
            if (p == null) {
                // (Nowhere straight on along the street - in the precinct's yard, say: off to a spot of pavement on
                // the beat by whatever way gets there, and on along the street from there.)
                for (int k = 0; k < 10 && p == null; k++) {
                    double an = r.nextDouble() * World.TAU;
                    float d = 80 + r.nextFloat() * (reach - 80);
                    float[] q = w.city.findWalkable(st.x + (float) Math.cos(an) * d, st.y + (float) Math.sin(an) * d);
                    int t = q == null ? -1 : w.city.tileIndex(q[0], q[1]);
                    if (t >= 0 && (w.city.tiles[t] == City.SIDEWALK || w.city.tiles[t] == City.PLAZA) && !w.city.outOfTown(q[0], q[1])) {
                        p = q;
                        float hx2 = q[0] - bx, hy2 = q[1] - by;
                        boolean ew = Math.abs(hx2) > Math.abs(hy2);
                        hx = ew ? Math.signum(hx2) : 0;
                        hy = ew ? 0 : Math.signum(hy2);
                    }
                }
            }
            if (p == null) {
                // (Nowhere at all: start again from the precinct in a while.)
                e.dutyHx = e.dutyHy = 0;
                e.dutyFor = -31;
                return false;
            }
            e.dutyHx = hx;
            e.dutyHy = hy;
            e.dutyX = p[0] + side;
            e.dutyY = p[1];
            e.dutyFor = 60;
            e.dutyWait = r.nextFloat() < 0.25f ? 2 + r.nextFloat() * 4 : 0;
        }
        w.wilds.walkToSpot(e, e.dutyX, e.dutyY, e.speed * 0.7f);
        return true;
    }

    /**
     * (11.2) An officer's beat partner: kept while both are free and near enough; otherwise, now and then, the
     * nearest free officer from the same precinct who hasn't one.
     */
    Entity beatMate(Entity e, City.Facility st) {
        Entity m = e.beatMate;
        if (m != null && (m.dead || m.removed || m.beatMate != e || m.type != Entity.COP || m.task != Dispatch.T_NONE || m.rig != null
                || m.fear > 0 || Math.abs(m.x - e.x) > 900 || Math.abs(m.y - e.y) > 900)) {
            if (m.beatMate == e) m.beatMate = null;
            e.beatMate = m = null;
        }
        if (m != null) return m;
        e.mateCd -= 1 / 30f;
        if (e.mateCd > 0) return null;
        e.mateCd = 4 + w.rnd.nextFloat() * 2;
        float bd = 400 * 400;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity o = w.entities.get(i);
            if (o == e || o.dead || o.type != Entity.COP || o.beatMate != null || o.task != Dispatch.T_NONE || o.rig != null || o.agency != e.agency
                    || o.role != e.role || World.footPatrol(o) != World.footPatrol(e)) continue;
            float d = (o.x - e.x) * (o.x - e.x) + (o.y - e.y) * (o.y - e.y);
            if (d < bd && w.city.nearestFacility(City.FACILITY_POLICE, o.x, o.y) == st) {
                bd = d;
                m = o;
            }
        }
        if (m != null) {
            e.beatMate = m;
            m.beatMate = e;
        }
        return m;
    }

    /** (11.2) Nothing solid on the straight line between two points (checked every half tile). */
    boolean openBetween(float x0, float y0, float x1, float y1) {
        float d = (float) World.hypot(x1 - x0, y1 - y0);
        int n = Math.max(1, (int) (d / (City.T * 0.5f)));
        for (int i = 1; i < n; i++) {
            float x = x0 + (x1 - x0) * i / n, y = y0 + (y1 - y0) * i / n;
            if (w.city.solidTile(City.fl(x / City.T), City.fl(y / City.T))) return false;
        }
        return true;
    }
}
