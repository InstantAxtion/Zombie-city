package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Who is where: the grid everyone is sorted into each step, and the questions asked of it (the nearest, how many near, who's in view).
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class Spatial {
    private final World w;

    Spatial(World w) {
        this.w = w;
    }

    /** Anyone standing just ahead of a car (so drivers can brake). */
    boolean personAhead(float x, float y, float angle) {
        float ax = x + (float) Math.cos(angle) * 16, ay = y + (float) Math.sin(angle) * 16;
        int cx0 = Math.max(0, (int) ((ax - 11) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((ax + 11) / World.CELL));
        int cy0 = Math.max(0, (int) ((ay - 11) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((ay + 11) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c] + w.zCount[c], end = w.cellStart[c] + w.cellCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o.isZombie()) continue;
                    float ddx = o.x - ax, ddy = o.y - ay;
                    if (ddx * ddx + ddy * ddy < 11 * 11) return true;
                }
            }
        return false;
    }

    /**
     * ((13.5) Everyone within (x0, y0)-(x1, y1), into out, from the grid of who's where: for the screen, which looked
     * at all thirty thousand people every time it was drawn to find the few hundred in view. With most of the map in
     * view it's quicker to go through everyone, and does.
     */
    void collectInView(float x0, float y0, float x1, float y1, ArrayList<Entity> out) {
        int cx0 = Math.max(0, (int) ((x0 - World.GRID_SLACK) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((x1 + World.GRID_SLACK) / World.CELL));
        int cy0 = Math.max(0, (int) ((y0 - World.GRID_SLACK) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((y1 + World.GRID_SLACK) / World.CELL));
        if (w.hashSize < 0 || (long) (cx1 - cx0 + 1) * (cy1 - cy0 + 1) * 4 > w.entities.size()) {
            for (int i = 0, n = w.entities.size(); i < n; i++) {
                Entity e = w.entities.get(i);
                if (e.x < x0 || e.x > x1 || e.y < y0 || e.y > y1) continue;
                out.add(e);
            }
            return;
        }
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c], end = k + w.cellCount[c]; k < end; k++) {
                    Entity e = w.sorted[k];
                    // (Not those gone since: indoors, into a car, or cleared away.)
                    if (e.dead || e.x < x0 || e.x > x1 || e.y < y0 || e.y > y1) continue;
                    out.add(e);
                }
            }
        // (And anyone come into the world since the grid was made: they're at the end of the list.)
        for (int i = w.hashSize, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.x < x0 || e.x > x1 || e.y < y0 || e.y > y1) continue;
            out.add(e);
        }
    }

    /** ((13.5) How many follow e (family, friends, a patrol), counted at most twice a second: for the cards. */
    int followers(Entity e) {
        if (e != w.followersOf || w.time - w.followersAt > 0.5f || w.time < w.followersAt) {
            w.followersOf = e;
            w.followersAt = w.time;
            int n = 0;
            for (int i = 0, m = w.entities.size(); i < m; i++) if (w.entities.get(i).leader == e) n++;
            w.followersN = n;
        }
        return w.followersN;
    }

    void buildHash() {
        int n = w.entities.size();
        if (w.sorted.length < n) {
            w.sorted = new Entity[n * 2];
            w.sortedX = new float[n * 2];
            w.sortedY = new float[n * 2];
            w.sortedR = new float[n * 2];
        }
        // ((12.0) Only the cells someone was in last time are emptied, and only those with someone in now are laid
        // out: an empty cell's count is nought, so where it starts never matters. Clearing and going through every
        // cell of the map each frame was most of the work on the big maps.)
        for (int k = 0; k < w.usedN; k++) {
            int c = w.usedCells[k];
            w.cellCount[c] = w.zCount[c] = w.aCount[c] = 0;
        }
        w.usedN = 0;
        w.hashFrame++;
        w.hashSize = n;
        if (w.hashCell.length < n) {
            w.hashCell = new int[n * 2];
            w.hashKind = new byte[n * 2];
            w.hashX = new float[n * 2];
            w.hashY = new float[n * 2];
            w.hashR = new float[n * 2];
        }
        // ((13.5) Counted, and later filled, in one array with each cell's three counts side by side: one place in
        // memory to go to for each of thirty thousand people, not three or four.)
        int[] cnt = w.hashCnt;
        for (int i = 0; i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.leader != null && !e.dead) e.leader.followerFrame = w.hashFrame;
            int c = cellOf(e);
            w.hashCell[i] = c;
            // ((13.3) What they are and where, kept side by side for the second pass below, which then doesn't need
            // to look at each of them again.)
            byte kind = e.isZombie() ? (byte) 0 : e.isArmed() ? (byte) 1 : (byte) 2;
            w.hashKind[i] = kind;
            w.hashX[i] = e.x;
            w.hashY[i] = e.y;
            w.hashR[i] = e.radius;
            int b = c * 3;
            if (cnt[b] + cnt[b + 1] + cnt[b + 2] == 0) {
                if (w.usedN == w.usedCells.length) w.usedCells = Arrays.copyOf(w.usedCells, w.usedN * 2);
                w.usedCells[w.usedN++] = c;
            }
            cnt[b + kind]++;
        }
        // Each cell lists its zombies first, then everyone else, so a search for one or the other only looks at
        // what it wants (a crowd of thousands doesn't slow down looking for the one zombie among them).
        // ((11.5) And among everyone else, the police and soldiers first: a search for them skips the crowd too.)
        int acc = 0;
        for (int k = 0; k < w.usedN; k++) {
            int c = w.usedCells[k], b = c * 3;
            int nz = cnt[b], na = cnt[b + 1], all = nz + na + cnt[b + 2];
            w.cellStart[c] = acc;
            w.zCount[c] = nz;
            w.aCount[c] = na;
            w.cellCount[c] = all;
            // (From here the same array holds where the next of each kind goes.)
            cnt[b] = acc;
            cnt[b + 1] = acc + nz;
            cnt[b + 2] = acc + nz + na;
            acc += all;
        }
        w.cellStart[w.cellCount.length] = acc;
        for (int i = 0; i < n; i++) {
            int at = cnt[w.hashCell[i] * 3 + w.hashKind[i]]++;
            w.sorted[at] = w.entities.get(i);
            w.sortedX[at] = w.hashX[i];
            w.sortedY[at] = w.hashY[i];
            w.sortedR[at] = w.hashR[i];
        }
        // (Emptied again for next time, only where anyone was.)
        for (int k = 0; k < w.usedN; k++) {
            int b = w.usedCells[k] * 3;
            cnt[b] = cnt[b + 1] = cnt[b + 2] = 0;
        }
    }

    int cellOf(Entity e) {
        int cx = Math.max(0, Math.min(w.gw - 1, (int) (e.x / World.CELL)));
        int cy = Math.max(0, Math.min(w.gh - 1, (int) (e.y / World.CELL)));
        return cy * w.gw + cx;
    }

    Entity nearest(Entity from, float radius, boolean wantZombie, boolean needLos) {
        float best = radius * radius;
        Entity res = null;
        float fx = from.x, fy = from.y, reach2 = (radius + World.GRID_SLACK) * (radius + World.GRID_SLACK);
        int cx0 = Math.max(0, (int) ((from.x - radius) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((from.x + radius) / World.CELL));
        int cy0 = Math.max(0, (int) ((from.y - radius) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((from.y + radius) / World.CELL));
        // ((27.4) The grid's arrays held here, not looked up again for every one in every cell; and a cell wholly out
        // of reach (the corners of the square round a long look) passed over without going through who's in it.
        // Everyone in a cell is inside it - but on the edge of the map, where those off it are kept in the edge cells -
        // so none of them could have been close enough: the same ones are found, in the same order.)
        final Entity[] sorted = w.sorted;
        final float[] sX = w.sortedX, sY = w.sortedY;
        final int[] start = w.cellStart, zc = w.zCount, cc = w.cellCount;
        final int gw = w.gw, gh = w.gh;
        final boolean fromZombie = from.isZombie();
        for (int cy = cy0; cy <= cy1; cy++) {
            float dy = 0;
            boolean edgeY = cy == 0 || cy == gh - 1;
            if (!edgeY) {
                float y0 = cy * World.CELL, y1 = (cy + 1) * World.CELL;
                dy = fy < y0 ? y0 - fy : fy > y1 ? fy - y1 : 0;
            }
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * gw + cx;
                int k = wantZombie ? start[c] : start[c] + zc[c], end = wantZombie ? start[c] + zc[c] : start[c] + cc[c];
                if (k >= end) continue;
                if (!edgeY && cx != 0 && cx != gw - 1) {
                    float x0 = cx * World.CELL, x1 = (cx + 1) * World.CELL;
                    float dx = fx < x0 ? x0 - fx : fx > x1 ? fx - x1 : 0;
                    if (dx * dx + dy * dy > reach2) continue;
                }
                for (; k < end; k++) {
                    // ((13.5) From where they were when the grid was made first (side by side in memory), with room for
                    // the few steps since: only those that could be close enough are looked at themselves.)
                    float px = sX[k] - fx, py = sY[k] - fy;
                    if (px * px + py * py > reach2) continue;
                    Entity o = sorted[k];
                    // ((12.0) Not one already finished off this moment, waiting to be cleared away.)
                    if (o.dead || o.hp <= 0 || o.isZombie() != wantZombie || o == from) continue;
                    // ((27.0) Up on a roof: out of the dead's reach, and their sight.)
                    if (!wantZombie && o.roof != null && o.onTower && fromZombie) continue;
                    float ddx = o.x - from.x, ddy = o.y - from.y, d2 = ddx * ddx + ddy * ddy;
                    if (d2 >= best) continue;
                    if (needLos) {
                        // (10.49) Seen or not is worked out after, nearest first: not for everyone nearer than the last.
                        if (w.nearN == w.nearD.length) {
                            w.nearD = Arrays.copyOf(w.nearD, w.nearN * 2);
                            w.nearE = Arrays.copyOf(w.nearE, w.nearN * 2);
                        }
                        w.nearD[w.nearN] = d2;
                        w.nearE[w.nearN++] = o;
                        continue;
                    }
                    best = d2;
                    res = o;
                }
            }
        }
        // ((27.4) Nearest first, in the same order as ever: the lists held here.)
        int nn = w.nearN;
        final float[] nd = w.nearD;
        final Entity[] ne = w.nearE;
        while (nn > 0) {
            int bi = 0;
            float bd = nd[0];
            for (int i = 1; i < nn; i++) if (nd[i] < bd) {
                bd = nd[i];
                bi = i;
            }
            Entity o = ne[bi];
            nd[bi] = nd[--nn];
            ne[bi] = ne[nn];
            if (w.city.los(from.x, from.y, o.x, o.y)) {
                res = o;
                break;
            }
        }
        while (nn > 0) ne[--nn] = null;
        w.nearN = 0;
        return res;
    }

    int gatherLiving(float x, float y, float r) {
        int n = 0;
        int cx0 = Math.max(0, (int) ((x - r) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((x + r) / World.CELL));
        int cy0 = Math.max(0, (int) ((y - r) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((y + r) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c] + w.zCount[c], end = w.cellStart[c] + w.cellCount[c]; k < end; k++) {
                    if (n == w.gathered.length) w.gathered = Arrays.copyOf(w.gathered, n * 2);
                    w.gathered[n++] = w.sorted[k];
                }
            }
        return n;
    }

    /** Any of the dead within radius? Quick when there's none within a walk of here (the usual case). */
    boolean zombieWithin(float x, float y, float radius) {
        if (radius / City.T + 3 < w.city.zombieLimit && w.city.fieldAt(w.city.zombieDist, x, y) > radius / City.T + 3) return false;
        return countZombiesNear(x, y, radius) > 0;
    }

    int countZombiesNear(float x, float y, float radius) {
        int count = 0;
        float reach2 = (radius + World.GRID_SLACK) * (radius + World.GRID_SLACK);
        int cx0 = Math.max(0, (int) ((x - radius) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((x + radius) / World.CELL));
        int cy0 = Math.max(0, (int) ((y - radius) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((y + radius) / World.CELL));
        // ((27.4) The grid's arrays held here.)
        final Entity[] sorted = w.sorted;
        final float[] sX = w.sortedX, sY = w.sortedY;
        final int[] start = w.cellStart, zc = w.zCount;
        final int gw = w.gw;
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * gw + cx;
                for (int k = start[c], end = start[c] + zc[c]; k < end; k++) {
                    // ((13.5) Only those that could be close enough looked at themselves (see nearest).)
                    float px = sX[k] - x, py = sY[k] - y;
                    if (px * px + py * py > reach2) continue;
                    Entity o = sorted[k];
                    if (o.dead || o.hp <= 0 || !o.isZombie()) continue;
                    float ddx = o.x - x, ddy = o.y - y;
                    if (ddx * ddx + ddy * ddy < radius * radius) count++;
                }
            }
        return count;
    }

    /** The city's police and soldiers (and anyone armed) round (x, y), from the grid, into gathered. */
    int gatherArmed(float x, float y, float r) {
        int n = 0;
        int cx0 = Math.max(0, (int) ((x - r) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((x + r) / World.CELL));
        int cy0 = Math.max(0, (int) ((y - r) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((y + r) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c] + w.zCount[c], end = k + w.aCount[c]; k < end; k++) {
                    if (n == w.gathered.length) w.gathered = Arrays.copyOf(w.gathered, n * 2);
                    w.gathered[n++] = w.sorted[k];
                }
            }
        return n;
    }

    boolean zombieAt(float x, float y, float r) {
        int cx0 = Math.max(0, (int) ((x - r) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((x + r) / World.CELL));
        int cy0 = Math.max(0, (int) ((y - r) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((y + r) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c], end = w.cellStart[c] + w.zCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o.dead || o.hp <= 0 || !o.isZombie()) continue;
                    float dx = o.x - x, dy = o.y - y;
                    if (dx * dx + dy * dy < r * r) return true;
                }
            }
        return false;
    }

    /** The dead round (x, y), from the grid, into gathered. */
    int gatherZ(float x, float y, float r) {
        int n = 0;
        int cx0 = Math.max(0, (int) ((x - r) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((x + r) / World.CELL));
        int cy0 = Math.max(0, (int) ((y - r) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((y + r) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c], end = w.cellStart[c] + w.zCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o.dead || o.hp <= 0 || !o.isZombie()) continue;
                    if ((o.x - x) * (o.x - x) + (o.y - y) * (o.y - y) > r * r) continue;
                    if (n == w.gathered.length) w.gathered = Arrays.copyOf(w.gathered, n * 2);
                    w.gathered[n++] = o;
                }
            }
        return n;
    }

    Entity nearestInReach(Entity e) {
        Entity z = nearest(e, e.radius + 9, true, false);
        if (z == null) return null;
        float ddx = z.x - e.x, ddy = z.y - e.y, reach = e.radius + z.radius + 3;
        return ddx * ddx + ddy * ddy < reach * reach ? z : null;
    }

    /** ((25.0) The nearest police officer to (x, y) within r, from the grid. */
    Entity nearestPolice(float x, float y, float r) {
        if (w.hashSize < 0) return null;
        Entity best = null;
        float bd = r * r;
        int cx0 = Math.max(0, (int) ((x - r) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((x + r) / World.CELL));
        int cy0 = Math.max(0, (int) ((y - r) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((y + r) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c] + w.zCount[c], end = w.cellStart[c] + w.cellCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o.dead || o.type != Entity.COP) continue;
                    float d = (o.x - x) * (o.x - x) + (o.y - y) * (o.y - y);
                    if (d < bd) {
                        bd = d;
                        best = o;
                    }
                }
            }
        return best;
    }

    Entity nearestArmed(Entity e, float radius) {
        Entity best = null;
        float bd = radius * radius;
        int cx0 = Math.max(0, (int) ((e.x - radius) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((e.x + radius) / World.CELL));
        int cy0 = Math.max(0, (int) ((e.y - radius) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((e.y + radius) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c] + w.zCount[c], end = k + w.aCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o.dead || !o.isArmed() || (o.ammo <= 0 && o.reserve <= 0)) continue;
                    float d = (o.x - e.x) * (o.x - e.x) + (o.y - e.y) * (o.y - e.y);
                    if (d < bd) {
                        bd = d;
                        best = o;
                    }
                }
            }
        return best;
    }

    /** The middle of the dead within r of (x, y), or null if there are none. */
    float[] packNear(float x, float y, float r) {
        float sx = 0, sy = 0;
        int n = 0;
        int cx0 = Math.max(0, (int) ((x - r) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((x + r) / World.CELL));
        int cy0 = Math.max(0, (int) ((y - r) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((y + r) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c], end = k + w.zCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o.dead || !o.isZombie() || (o.x - x) * (o.x - x) + (o.y - y) * (o.y - y) > r * r) continue;
                    sx += o.x;
                    sy += o.y;
                    n++;
                }
            }
        return n == 0 ? null : new float[]{sx / n, sy / n};
    }

    /** The most badly hurt police officer or soldier within a medic's reach (themselves too), or null. */
    Entity woundedNear(Entity e) {
        Entity best = e.hp < e.maxHp * 0.6f && !e.infected ? e : null;
        float worst = best != null ? e.hp / e.maxHp : 0.7f;
        float r = World.MEDIC_REACH;
        int cx0 = Math.max(0, (int) ((e.x - r) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((e.x + r) / World.CELL));
        int cy0 = Math.max(0, (int) ((e.y - r) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((e.y + r) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c] + w.zCount[c], end = k + w.aCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o == e || o.dead || o.removed || o.infected || o.ride != null || !o.isArmed()) continue;
                    float share = o.hp / o.maxHp;
                    if (share >= worst || World.hypot(o.x - e.x, o.y - e.y) > r) continue;
                    worst = share;
                    best = o;
                }
            }
        return best;
    }

    /** The middle of the dead within a radius ({x, y, count}); the spot itself if there are none. */
    float[] zombieCentre(float x, float y, float radius) {
        float sx = 0, sy = 0;
        int count = 0;
        int cx0 = Math.max(0, (int) ((x - radius) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((x + radius) / World.CELL));
        int cy0 = Math.max(0, (int) ((y - radius) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((y + radius) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c], end = w.cellStart[c] + w.zCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o.dead || o.hp <= 0 || !o.isZombie()) continue;
                    float ddx = o.x - x, ddy = o.y - y;
                    if (ddx * ddx + ddy * ddy < radius * radius) {
                        sx += o.x;
                        sy += o.y;
                        count++;
                    }
                }
            }
        return count == 0 ? new float[]{x, y, 0} : new float[]{sx / count, sy / count, count};
    }

    int armedNear(float x, float y, float r) {
        int n = 0;
        int cx0 = Math.max(0, (int) ((x - r) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((x + r) / World.CELL));
        int cy0 = Math.max(0, (int) ((y - r) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((y + r) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c] + w.zCount[c], end = k + w.aCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o.dead || !o.isArmed()) continue;
                    if ((o.x - x) * (o.x - x) + (o.y - y) * (o.y - y) <= r * r) n++;
                }
            }
        return n;
    }

    Entity nearestOfType(Entity e, int type, float radius) {
        Entity best = null;
        float bd = radius * radius;
        // ((10.31) The living off the grid, not a look through everyone.)
        boolean grid = !Entity.isZombieType(type);
        for (int i = 0, n = grid ? gatherLiving(e.x, e.y, radius) : w.entities.size(); i < n; i++) {
            Entity o = grid ? w.gathered[i] : w.entities.get(i);
            if (o.type != type || o.dead || o == e) continue;
            float d = (o.x - e.x) * (o.x - e.x) + (o.y - e.y) * (o.y - e.y);
            if (d < bd) {
                bd = d;
                best = o;
            }
        }
        return best;
    }

    /** Anyone who isn't a zombie within a radius (so soldiers don't grenade their own side). */
    boolean peopleNear(float x, float y, float radius) {
        int cx0 = Math.max(0, (int) ((x - radius) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((x + radius) / World.CELL));
        int cy0 = Math.max(0, (int) ((y - radius) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((y + radius) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c] + w.zCount[c], end = w.cellStart[c] + w.cellCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o.dead || o.isZombie()) continue;
                    if ((o.x - x) * (o.x - x) + (o.y - y) * (o.y - y) < radius * radius) return true;
                }
            }
        return false;
    }

    /** The nearest zombie to a point, ignoring walls (for the helicopter gunner). */
    Entity nearestZombie(float x, float y, float radius) {
        Entity best = null;
        float bd = radius * radius;
        // ((13.3) From the grid: it went through everyone in the city, for every lookout and every building's door.)
        int cx0 = Math.max(0, (int) ((x - radius) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((x + radius) / World.CELL));
        int cy0 = Math.max(0, (int) ((y - radius) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((y + radius) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c], end = k + w.zCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o.dead || o.hp <= 0 || !o.isZombie()) continue;
                    float d = (o.x - x) * (o.x - x) + (o.y - y) * (o.y - y);
                    if (d < bd) {
                        bd = d;
                        best = o;
                    }
                }
            }
        return best;
    }

    /** The nearest zombie a gunner on the ground can actually see. */
    Entity nearestVisibleZombie(float x, float y, float radius) {
        // ((24.0) From the grid, as nearestZombie, not everyone in the city for every gun on every step; and the line of
        // sight looked along for the nearest first, so only until one can be seen. A little beyond the radius in the
        // grid, for anyone who's moved since it was laid out.)
        float rr = radius * radius, pad = radius + 8;
        int cx0 = Math.max(0, (int) ((x - pad) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((x + pad) / World.CELL));
        int cy0 = Math.max(0, (int) ((y - pad) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((y + pad) / World.CELL));
        int m = 0;
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c], end = k + w.zCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o.dead || o.hp <= 0 || !o.isZombie()) continue;
                    float d = (o.x - x) * (o.x - x) + (o.y - y) * (o.y - y);
                    if (d >= rr) continue;
                    if (m == w.visZ.length) {
                        w.visZ = Arrays.copyOf(w.visZ, m * 2);
                        w.visD = Arrays.copyOf(w.visD, m * 2);
                    }
                    w.visZ[m] = o;
                    w.visD[m++] = d;
                }
            }
        Entity best = null;
        while (m > 0) {
            int bi = 0;
            for (int i = 1; i < m; i++) if (w.visD[i] < w.visD[bi]) bi = i;
            Entity o = w.visZ[bi];
            if (w.city.los(x, y, o.x, o.y)) {
                best = o;
                break;
            }
            w.visZ[bi] = w.visZ[--m];
            w.visD[bi] = w.visD[m];
        }
        for (int i = 0; i < w.visZ.length && w.visZ[i] != null; i++) w.visZ[i] = null;
        return best;
    }

    // ((13.3) These two from the grid of who's where, near (x, y): they went through all thirty thousand people for each
    // of the hundreds cut off by the dead, which held a Colossal game up for seconds at a time.)
    int countCiviliansNear(float x, float y, float r) {
        int n = 0;
        int cx0 = Math.max(0, (int) ((x - r) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((x + r) / World.CELL));
        int cy0 = Math.max(0, (int) ((y - r) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((y + r) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c] + w.zCount[c] + w.aCount[c], end = w.cellStart[c] + w.cellCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (!o.dead && o.type == Entity.CIVILIAN && Math.abs(o.x - x) < r && Math.abs(o.y - y) < r) n++;
                }
            }
        return n;
    }

    Entity nearestArmed(float x, float y, float r) {
        int cx0 = Math.max(0, (int) ((x - r) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((x + r) / World.CELL));
        int cy0 = Math.max(0, (int) ((y - r) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((y + r) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c] + w.zCount[c], end = k + w.aCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o.dead || !o.isArmed()) continue;
                    if ((o.x - x) * (o.x - x) + (o.y - y) * (o.y - y) < r * r) return o;
                }
            }
        return null;
    }

    /** A building with people inside (sheltering or visiting) whose door is within r of (x, y). */
    City.Building occupiedNear(float x, float y, float r) {
        if (w.occGrid == null || w.occTimer <= 0) w.movement.buildOccGrid();
        int ocx = (int) (x / World.OCC_CELL), ocy = (int) (y / World.OCC_CELL);
        for (int gy = Math.max(0, ocy - 1); gy <= Math.min(w.occGH - 1, ocy + 1); gy++)
            for (int gx = Math.max(0, ocx - 1); gx <= Math.min(w.occGW - 1, ocx + 1); gx++) {
                java.util.ArrayList<City.Building> cell = w.occGrid[gy * w.occGW + gx];
                if (cell == null) continue;
                for (int i = 0, n = cell.size(); i < n; i++) {
                    City.Building b = cell.get(i);
                    if ((b.doorX - x) * (b.doorX - x) + (b.doorY - y) * (b.doorY - y) < r * r
                            || (x > b.x0 - 8 && x < b.x1 + 8 && y > b.y0 - 8 && y < b.y1 + 8)) return b;
                }
            }
        return null;
    }
}
