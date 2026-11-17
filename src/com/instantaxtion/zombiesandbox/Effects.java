package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Effects: particles, blood, tracers, decals, marks and gunsmoke.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class Effects {
    private final World w;

    Effects(World w) {
        this.w = w;
    }

    void mark(float x, float y, float angle, byte kind, int side) {
        int i = w.mnext;
        w.mnext = (w.mnext + 1) % World.MAXM;
        if (w.mcount < World.MAXM) w.mcount++;
        w.mx[i] = x;
        w.my[i] = y;
        w.mang[i] = angle;
        w.mkind[i] = kind;
        w.mside[i] = (byte) side;
        w.mborn[i] = w.time;
    }

    /** A puff of gunsmoke where someone fired. */
    void haze(float x, float y) {
        int i = w.hnext;
        w.hnext = (w.hnext + 1) % World.MAXH;
        w.hx[i] = x + w.rnd.nextFloat() * 6 - 3;
        w.hy[i] = y + w.rnd.nextFloat() * 6 - 3;
        w.hsize[i] = 4 + w.rnd.nextFloat() * 3;
        w.hborn[i] = w.time;
    }

    /**
     * Footprints in sand, dirt and mud (and wet ones just out of a stream), a trail of blood behind the
     * wounded, and drag marks behind the crawling dead. Only where the camera is, a few times a second.
     */
    void updateMarks(float dt) {
        w.marksTick -= dt;
        if (w.marksTick > 0) return;
        float step = 0.1f - w.marksTick;
        w.marksTick = 0.1f;
        if (w.viewX1 < 0) return;
        float x0 = w.viewX0 - 60, x1 = w.viewX1 + 60, y0 = w.viewY0 - 60, y1 = w.viewY1 + 60;
        // ((16.8) Only those in the grid's cells round the screen, not a look at all tens of thousands for the few in
        // view.)
        if (w.hashSize < 0) return;
        int gx0 = Math.max(0, (int) ((x0 - World.GRID_SLACK) / World.CELL)), gx1 = Math.min(w.gw - 1, (int) ((x1 + World.GRID_SLACK) / World.CELL));
        int gy0 = Math.max(0, (int) ((y0 - World.GRID_SLACK) / World.CELL)), gy1 = Math.min(w.gh - 1, (int) ((y1 + World.GRID_SLACK) / World.CELL));
        for (int gy = gy0; gy <= gy1; gy++)
          for (int gx = gx0; gx <= gx1; gx++)
           for (int k = w.cellStart[gy * w.gw + gx], kend = k + w.cellCount[gy * w.gw + gx]; k < kend; k++) {
            Entity e = w.sorted[k];
            if (e.dead || e.aloft || e.x < x0 || e.x > x1 || e.y < y0 || e.y > y1) continue;
            if (e.wet > 0) e.wet -= step;
            if (e.markX < 0) {
                e.markX = e.x;
                e.markY = e.y;
                continue;
            }
            float ddx = e.x - e.markX, ddy = e.y - e.markY, d2 = ddx * ddx + ddy * ddy;
            int ti = w.city.tileIndex(e.x, e.y);
            byte t = ti >= 0 && ti < w.city.tiles.length ? w.city.tiles[ti] : 0;
            if (t == City.WATER) {
                if (w.city.isFord(ti)) e.wet = 4;
                e.markX = e.x;
                e.markY = e.y;
                continue;
            }
            boolean dog = e.type == Entity.DOG || e.type == Entity.ZOMBIE_DOG;
            float stride = dog ? 4 : 5.5f;
            if (d2 < stride * stride) continue;
            float ang = (float) Math.atan2(ddy, ddx);
            boolean soft = t == City.SAND || t == City.DIRT || e.wet > 0;
            boolean crawling = e.isUndead() && e.hp < e.maxHp * 0.25f;
            if (soft) {
                e.markLeft = !e.markLeft;
                float side = e.markLeft ? 1.3f : -1.3f;
                mark(e.x - (float) Math.sin(ang) * side, e.y + (float) Math.cos(ang) * side, ang, dog ? World.M_PAW : World.M_FOOT, e.markLeft ? 1 : 0);
            }
            if (w.gore && crawling) mark((e.x + e.markX) / 2, (e.y + e.markY) / 2, ang, World.M_DRAG, 0);
            else if (w.gore && !e.isZombie() && (e.infected || e.hp < e.maxHp * 0.5f) && w.rnd.nextFloat() < (e.hp < e.maxHp * 0.25f ? 0.9f : 0.5f))
                mark(e.x + w.rnd.nextFloat() * 2 - 1, e.y + w.rnd.nextFloat() * 2 - 1, w.rnd.nextFloat() * World.TAU, World.M_DRIP, e.hp < e.maxHp * 0.25f ? 1 : 0);
            e.markX = e.x;
            e.markY = e.y;
        }
    }

    /** (10.59) How much of a splat is left to see (1 fresh, 0 gone). Tyre marks and glass too. */
    float decalFade(int i) {
        float a = w.dage[i];
        return a <= World.DECAL_FADE ? 1 : Math.max(0, 1 - (a - World.DECAL_FADE) / (World.DECAL_LIFE - World.DECAL_FADE));
    }

    void particle(float x, float y, float vx, float vy, float life, float size, int color, byte type) {
        if (w.quality > World.Q_FULL && ((w.pnext * 7 + 3) % 10) < (w.quality == World.Q_REDUCED ? 4 : 7)) {
            w.pnext = (w.pnext + 1) % World.MAXP; // (skips a slot so the pattern moves on)
            return;
        }
        int i = w.pnext;
        w.pnext = (w.pnext + 1) % World.MAXP;
        w.px[i] = x;
        w.py[i] = y;
        w.pvx[i] = vx;
        w.pvy[i] = vy;
        w.plife[i] = w.pmax[i] = life;
        w.psize[i] = size;
        w.pcol[i] = color;
        w.ptype[i] = type;
    }

    void bloodBurst(float x, float y, int n, float nx, float ny) {
        if (!w.gore) return;
        for (int i = 0; i < n; i++) {
            float a = w.rnd.nextFloat() * World.TAU, s = 10 + w.rnd.nextFloat() * 40;
            particle(x, y, (float) Math.cos(a) * s + nx * 40, (float) Math.sin(a) * s + ny * 40,
                    0.3f + w.rnd.nextFloat() * 0.3f, 0.8f + w.rnd.nextFloat() * 0.8f, 0xFF8E1010, World.P_DOT);
        }
        if (w.rnd.nextFloat() < 0.5f)
            decal(x + nx * 4 + w.rnd.nextFloat() * 4 - 2, y + ny * 4 + w.rnd.nextFloat() * 4 - 2, 1.5f + w.rnd.nextFloat() * 2.5f, 0x996E0A0A);
    }

    /** A small puff of dust or plaster where a bullet strikes. */
    void impactPuff(float x, float y, int color) {
        for (int k = 0; k < 2; k++)
            particle(x, y, w.rnd.nextFloat() * 16 - 8, w.rnd.nextFloat() * 16 - 8, 0.35f + w.rnd.nextFloat() * 0.2f,
                    1.6f + w.rnd.nextFloat(), color, World.P_SMOKE);
    }

    void tracer(float x0, float y0, float x1, float y1) {
        int i = w.tnext;
        w.tnext = (w.tnext + 1) % World.MAXT;
        w.tx0[i] = x0;
        w.ty0[i] = y0;
        w.tx1[i] = x1;
        w.ty1[i] = y1;
        w.tlife[i] = World.TRACER_LIFE;
    }

    void decal(float x, float y, float r, int color) {
        decal(x, y, r, color, World.D_SPLAT, 0);
    }

    void decal(float x, float y, float r, int color, byte kind, float angle) {
        int i = w.dnext;
        w.dnext = (w.dnext + 1) % World.MAXD;
        if (w.dcount < World.MAXD) w.dcount++;
        w.dx[i] = x;
        w.dy[i] = y;
        w.dr[i] = r;
        w.dcol[i] = color;
        w.dkind[i] = kind;
        w.dang[i] = angle;
        w.dage[i] = 0;
    }

    /** Rubber left on the road by a car braking hard or skidding in a crash. */
    void skid(float x, float y, float angle, float length) {
        float sx = (float) -Math.sin(angle) * 2.8f, sy = (float) Math.cos(angle) * 2.8f;
        decal(x + sx, y + sy, length, 0x55101010, World.D_SKID, angle);
        decal(x - sx, y - sy, length, 0x55101010, World.D_SKID, angle);
    }

    void updateEffects(float dt) {
        updateMarks(dt);
        for (int i = 0; i < w.dcount; i++) {
            if (w.dr[i] <= 0) continue;
            w.dage[i] += dt;
            if (w.dage[i] >= World.DECAL_LIFE) w.dr[i] = 0;
        }
        for (int i = 0; i < World.MAXP; i++) {
            if (w.plife[i] <= 0) continue;
            w.plife[i] -= dt;
            // A spent case comes to rest on the ground and stays there.
            if (w.plife[i] <= 0 && w.ptype[i] == World.P_CASING) mark(w.px[i], w.py[i], w.rnd.nextFloat() * World.TAU, World.M_CASING, 0);
            w.px[i] += w.pvx[i] * dt;
            w.py[i] += w.pvy[i] * dt;
            float drag = w.ptype[i] == World.P_SMOKE ? 1.5f : w.ptype[i] == World.P_CASING ? 9f : w.ptype[i] == World.P_DEBRIS ? 2.5f : 5f;
            w.pvx[i] -= w.pvx[i] * Math.min(1, drag * dt);
            w.pvy[i] -= w.pvy[i] * Math.min(1, drag * dt);
            if (w.ptype[i] == World.P_SMOKE) w.psize[i] += dt * 6;
        }
        for (int i = 0; i < World.MAXT; i++) if (w.tlife[i] > 0) w.tlife[i] -= dt;
        for (int i = w.explosions.size() - 1; i >= 0; i--) {
            World.Explosion e = w.explosions.get(i);
            e.life -= dt;
            if (e.life <= 0) w.explosions.remove(i);
        }
    }
}
