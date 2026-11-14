package com.instantaxtion.zombiesandbox;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.Landmarks.*;

/** ((22.8) Landmark designs, part 10 (see Landmarks): distinctive homes and residential blocks. */
final class LandmarkArt10 {
    private LandmarkArt10() {
    }

    static void register(List<Landmarks.Def> out) {
        victorianMansion(out);
        paintedLadies(out);
        midCentury(out);
        adobe(out);
        aFrames(out);
        hilltopVilla(out);
        shotgun(out);
        artDeco(out);
        brutalist(out);
        gardenCoop(out);
        mobileHomes(out);
        gatedEstate(out);
        tudor(out);
        mews(out);
        tinyHouses(out);
        riverLofts(out);
        bungalowCourt(out);
        farmstead(out);
        machiya(out);
        capeCod(out);
        duplexRow(out);
        luxuryCondos(out);
        seniorApts(out);
        workerCottages(out);
        hacienda(out);
    }

    // ------------------------------------------------------------------ shared bits

    static final float TT = City.T;

    /**
     * Drawing in a rectangle's own frame: u runs along its long side (0..L), v across it (0..S). A tall rectangle is
     * drawn transposed (x and y swapped), which keeps the light from the top left and the shadows to the lower right.
     */
    static final class F {
        final Canvas c;
        final Paint p;
        final float ox, oy, L, S;
        final boolean t;

        F(Canvas c, Paint p, float x0, float y0, float x1, float y1) {
            this.c = c;
            this.p = p;
            ox = x0;
            oy = y0;
            t = y1 - y0 > x1 - x0;
            L = t ? y1 - y0 : x1 - x0;
            S = t ? x1 - x0 : y1 - y0;
        }

        float x(float u, float v) {
            return ox + (t ? v : u);
        }

        float y(float u, float v) {
            return oy + (t ? u : v);
        }

        /** A direction in this frame (degrees from +u towards +v) as a world rotation. */
        float ang(float a) {
            return t ? 90 - a : a;
        }

        void rect(int col, float u0, float v0, float u1, float v1) {
            Landmarks.rect(c, p, col, x(u0, v0), y(u0, v0), x(u1, v1), y(u1, v1));
        }

        void oval(int col, float u0, float v0, float u1, float v1) {
            Landmarks.oval(c, p, col, x(u0, v0), y(u0, v0), x(u1, v1), y(u1, v1));
        }

        void roundRect(int col, float u0, float v0, float u1, float v1, float rad) {
            Landmarks.roundRect(c, p, col, x(u0, v0), y(u0, v0), x(u1, v1), y(u1, v1), rad);
        }

        void frame(int col, float w, float u0, float v0, float u1, float v1) {
            Landmarks.frame(c, p, col, w, x(u0, v0), y(u0, v0), x(u1, v1), y(u1, v1));
        }

        void circle(int col, float u, float v, float r) {
            Landmarks.circle(c, p, col, x(u, v), y(u, v), r);
        }

        void ring(int col, float w, float u, float v, float r) {
            LandmarkArt10.ring(c, p, col, w, x(u, v), y(u, v), r);
        }

        void line(int col, float w, float u0, float v0, float u1, float v1) {
            Landmarks.line(c, p, col, w, x(u0, v0), y(u0, v0), x(u1, v1), y(u1, v1));
        }

        void poly(int col, float... q) {
            float[] m = new float[q.length];
            for (int k = 0; k + 1 < q.length; k += 2) {
                m[k] = x(q[k], q[k + 1]);
                m[k + 1] = y(q[k], q[k + 1]);
            }
            Landmarks.poly(c, p, col, m);
        }

        void tree(float u, float v, float r, int col) {
            Landmarks.tree(c, p, x(u, v), y(u, v), r, col);
        }

        void tree(float u, float v, float r) {
            Landmarks.tree(c, p, x(u, v), y(u, v), r);
        }

        void bush(float u, float v, float r) {
            Landmarks.bush(c, p, x(u, v), y(u, v), r);
        }

        void lamp(float u, float v) {
            Landmarks.lamp(c, p, x(u, v), y(u, v));
        }

        void bench(float u, float v, boolean alongU) {
            Landmarks.bench(c, p, x(u, v), y(u, v), alongU != t);
        }

        void clip(float u0, float v0, float u1, float v1) {
            c.clipRect(Math.min(x(u0, v0), x(u1, v1)), Math.min(y(u0, v0), y(u1, v1)), Math.max(x(u0, v0), x(u1, v1)), Math.max(y(u0, v0), y(u1, v1)));
        }

        void mv(Path q, float u, float v) {
            q.moveTo(x(u, v), y(u, v));
        }

        void ln(Path q, float u, float v) {
            q.lineTo(x(u, v), y(u, v));
        }

        void qd(Path q, float cu, float cv, float u, float v) {
            q.quadTo(x(cu, cv), y(cu, cv), x(u, v), y(u, v));
        }

        void fill(Path q, int col) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(col);
            c.drawPath(q, p);
        }

        void stroke(Path q, int col, float w) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(w);
            p.setColor(col);
            c.drawPath(q, p);
            p.setStyle(Paint.Style.FILL);
        }

        /** A quadratic curve stroked from (u0,v0) via (cu,cv) to (u1,v1). */
        void curve(int col, float w, float u0, float v0, float cu, float cv, float u1, float v1) {
            Path q = new Path();
            mv(q, u0, v0);
            qd(q, cu, cv, u1, v1);
            stroke(q, col, w);
        }

        void sign(City city, String s, float u, float v, float size, int col) {
            Landmarks.sign(city, s, x(u, v), y(u, v), size, col);
        }

        /** Random specks over a rectangle (grit, gravel, wear). */
        void specks(Random r, int n, int a, int b, float u0, float v0, float u1, float v1, float s) {
            n = Math.min(n / 3 + 1, 60);
            for (int k = 0; k < n; k++) {
                float u = u0 + r.nextFloat() * (u1 - u0), v = v0 + r.nextFloat() * (v1 - v0);
                rect(r.nextBoolean() ? a : b, u, v, u + s, v + s * 0.8f);
            }
        }

        /** Starts drawing turned to frame angle a about (u,v); end with c.restore(). */
        void turn(float u, float v, float a) {
            c.save();
            c.translate(x(u, v), y(u, v));
            c.rotate(ang(a));
        }
    }

    /** A Def laid out in the frame of the block's long side: lp gives its buildings there, g paints its ground. */
    abstract static class D extends Landmarks.Def {
        D(String name, int type) {
            super(name, type);
        }

        abstract int[][] lp(int L, int S);

        abstract void g(City city, F f, int[][] q, Random r);

        void rf(City city, F f, int part, Random r) {
            flatRoof(f.c, f.p, f.x(0, 0), f.y(0, 0), f.x(f.L, f.S), f.y(f.L, f.S), roofCol);
        }

        @Override
        int[][] plan(int bw, int bh, Random r) {
            int[][] a = lp(Math.max(bw, bh), Math.min(bw, bh));
            if (bh <= bw) return a;
            int[][] b = new int[a.length][];
            for (int i = 0; i < a.length; i++) b[i] = new int[]{a[i][1], a[i][0], a[i][3], a[i][2], a[i][4]};
            return b;
        }

        @Override
        void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
            F f = new F(c, p, x0, y0, x1, y1);
            g(city, f, lp(Math.round(f.L / TT), Math.round(f.S / TT)), r);
            p.setStyle(Paint.Style.FILL);
        }

        @Override
        void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
            rf(city, new F(c, p, x0, y0, x1, y1), part, r);
            p.setStyle(Paint.Style.FILL);
        }
    }

    static int[] B(int u, int v, int lu, int lv, int fl) {
        return new int[]{u, v, lu, lv, fl};
    }

    static void ring(Canvas c, Paint p, int col, float w, float x, float y, float r) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(w);
        p.setColor(col);
        c.drawCircle(x, y, r, p);
        p.setStyle(Paint.Style.FILL);
    }

    /** A world offset (dx, dy) seen in a frame turned by deg (for shadows drawn inside a rotation). */
    static float[] off(float deg, float dx, float dy) {
        double a = Math.toRadians(-deg);
        return new float[]{(float) (dx * Math.cos(a) - dy * Math.sin(a)), (float) (dx * Math.sin(a) + dy * Math.cos(a))};
    }

    /** The world rotation the canvas is currently turned by (passed in by the caller who turned it). */
    static void shadowBox(Canvas c, Paint p, float deg, float l, float w, float rad, float dx, float dy, int col) {
        float[] o = off(deg, dx, dy);
        roundRect(c, p, col, -l + o[0], -w + o[1], l + o[0], w + o[1], rad);
    }

    /** A car from above. */
    static void car(F f, float u, float v, float a, int col) {
        float l = 6.5f, w = 3.2f, deg = f.ang(a);
        f.turn(u, v, a);
        Canvas c = f.c;
        Paint p = f.p;
        shadowBox(c, p, deg, l, w, 1.8f, 1, 1.4f, 0x48000000);
        roundRect(c, p, City.darken(col, 0.7f), -l, -w, l, w, 1.8f);
        roundRect(c, p, col, -l + 0.4f, -w + 0.4f, l - 0.4f, w - 0.4f, 1.6f);
        rect(c, p, 0xFF2A333C, 1.2f, -w + 0.8f, 3.2f, w - 0.8f);
        rect(c, p, 0xFF2A333C, -4.4f, -w + 0.9f, -3.2f, w - 0.9f);
        rect(c, p, City.lighten(col, 0.18f), -3.2f, -w + 0.8f, 1.2f, w - 0.8f);
        c.restore();
    }


    static int[][] arr(ArrayList<int[]> a) {
        return a.toArray(new int[a.size()][]);
    }

    /** A hipped roof over the rectangle, its faces shaded for light from the top left; courses adds tile lines. */
    static void hip(F f, int col, float u0, float v0, float u1, float v1, boolean courses) {
        float h = Math.min(u1 - u0, v1 - v0) / 2, au = u0 + h, av = v0 + h, bu = u1 - h, bv = v1 - h;
        f.poly(City.lighten(col, 0.12f), u0, v0, u1, v0, bu, av, au, av);
        f.poly(col, u0, v0, u0, v1, au, bv, au, av);
        f.poly(City.darken(col, 0.82f), u1, v0, u1, v1, bu, bv, bu, av);
        f.poly(City.darken(col, 0.7f), u0, v1, u1, v1, bu, bv, au, bv);
        if (courses)
            for (float k = 2.5f; k < h - 1; k += 2.5f) f.frame(0x22000000, 0.45f, u0 + k, v0 + k, u1 - k, v1 - k);
        int hl = City.lighten(col, 0.3f), dk = City.darken(col, 0.6f);
        f.line(hl, 0.7f, u0, v0, au, av);
        f.line(hl, 0.7f, u1, v0, bu, av);
        f.line(dk, 0.7f, u0, v1, au, bv);
        f.line(dk, 0.7f, u1, v1, bu, bv);
        if (bu - au > bv - av) f.line(hl, 1f, au, av, bu, bv);
        else f.line(hl, 1f, au, av, au, bv);
    }

    /** A gabled roof with its ridge along u, tile courses along it when courses. */
    static void gable(F f, int col, float u0, float v0, float u1, float v1, boolean courses) {
        float m = (v0 + v1) / 2;
        f.rect(City.lighten(col, 0.1f), u0, v0, u1, m);
        f.rect(City.darken(col, 0.74f), u0, m, u1, v1);
        if (courses)
            for (float k = 2.2f; k < (v1 - v0) / 2 - 0.5f; k += 2.2f) {
                f.line(0x26000000, 0.45f, u0, v0 + k, u1, v0 + k);
                f.line(0x26000000, 0.45f, u0, v1 - k, u1, v1 - k);
            }
        f.line(City.lighten(col, 0.35f), 1f, u0, m, u1, m);
        f.frame(City.darken(col, 0.6f), 0.6f, u0, v0, u1, v1);
    }

    /** A gabled roof with its ridge along v (across the frame). */
    static void gableV(F f, int col, float u0, float v0, float u1, float v1, boolean courses) {
        float m = (u0 + u1) / 2;
        f.rect(City.lighten(col, 0.04f), u0, v0, m, v1);
        f.rect(City.darken(col, 0.78f), m, v0, u1, v1);
        if (courses)
            for (float k = 2.2f; k < (u1 - u0) / 2 - 0.5f; k += 2.2f) {
                f.line(0x26000000, 0.45f, u0 + k, v0, u0 + k, v1);
                f.line(0x26000000, 0.45f, u1 - k, v0, u1 - k, v1);
            }
        f.line(City.lighten(col, 0.35f), 1f, m, v0, m, v1);
        f.frame(City.darken(col, 0.6f), 0.6f, u0, v0, u1, v1);
    }

    /** A flat roof with a parapet, a deck and a few specks of grit. */
    static void deck(F f, Random r, int col, float u0, float v0, float u1, float v1, float par) {
        f.rect(City.darken(col, 0.72f), u0, v0, u1, v1);
        f.rect(col, u0 + par, v0 + par, u1 - par, v1 - par);
        f.rect(City.lighten(col, 0.12f), u0, v0, u1, v0 + par * 0.5f);
        f.specks(r, (int) ((u1 - u0) * (v1 - v0) / 60), 0x16000000, 0x14FFFFFF, u0 + par, v0 + par, u1 - par, v1 - par, 0.7f);
    }

    /** A brick chimney stack with pots. */
    static void chimney(F f, float u, float v, int pots) {
        float w = 1.6f + pots * 1.7f;
        f.rect(0x48000000, u + 1, v + 1.4f, u + w + 1, v + 3.2f + 1.4f);
        f.rect(0xFF8A4A36, u, v, u + w, v + 3.2f);
        f.rect(0xFFA05A42, u, v, u + w, v + 0.9f);
        for (int k = 0; k < pots; k++) {
            f.circle(0xFFB86A48, u + 1.6f + k * 1.7f, v + 1.6f, 0.75f);
            f.circle(0xFF2A2020, u + 1.6f + k * 1.7f, v + 1.6f, 0.38f);
        }
    }

    /** A stuccoed or stone chimney, square with a cap. */
    static void stack(F f, float u, float v, float s, int col) {
        f.rect(0x48000000, u + 1, v + 1.4f, u + s + 1, v + s + 1.4f);
        f.rect(City.darken(col, 0.8f), u, v, u + s, v + s);
        f.rect(col, u + 0.5f, v + 0.5f, u + s - 0.5f, v + s - 0.5f);
        f.rect(0xFF3A3434, u + s * 0.3f, v + s * 0.3f, u + s * 0.7f, v + s * 0.7f);
    }

    /** A conical or faceted turret roof from above: gores shaded round from the lit top left, with a finial. */
    static void cone(F f, float u, float v, float r, int col, int n) {
        f.circle(0x40000000, u + r * 0.25f, v + r * 0.35f, r);
        for (int k = 0; k < n; k++) {
            double a0 = k * 2 * Math.PI / n, a1 = (k + 1) * 2 * Math.PI / n, am = (a0 + a1) / 2;
            float sh = 1f + 0.3f * (float) Math.cos(am + 3 * Math.PI / 4);
            f.poly(Landmarks.shade(col, sh), u, v, u + (float) Math.cos(a0) * r, v + (float) Math.sin(a0) * r,
                    u + (float) Math.cos(a1) * r, v + (float) Math.sin(a1) * r);
        }
        f.circle(0xFFD8B048, u, v, Math.max(0.7f, r * 0.12f));
    }

    /** A small gabled dormer on a roof face, its ridge running across v (out of the slope). */
    static void dormer(F f, int col, float u, float va, float vb, float w) {
        float len = Math.min(Math.abs(vb - va), w * 1.15f), v0 = vb > va ? va : va - len, v1 = v0 + len;
        f.rect(0x40000000, u - w / 2 + 0.8f, v0 + 0.8f, u + w / 2 + 0.8f, v1 + 0.8f);
        gableV(f, col, u - w / 2, v0, u + w / 2, v1, false);
        f.rect(0xFFE8E4DA, u - w / 2, vb > va ? v0 : v1 - 0.8f, u + w / 2, vb > va ? v0 + 0.8f : v1);
    }

    /** A white picket fence from (u0,v0) to (u1,v1). */
    static void picket(F f, int col, float u0, float v0, float u1, float v1) {
        f.line(0x40000000, 0.9f, u0 + 0.5f, v0 + 0.7f, u1 + 0.5f, v1 + 0.7f);
        f.line(col, 0.7f, u0, v0, u1, v1);
        float len = (float) Math.hypot(u1 - u0, v1 - v0);
        int n = Math.max(1, (int) (len / 5f));
        for (int k = 0; k <= n; k++) {
            float t = k / (float) n;
            f.circle(col, u0 + (u1 - u0) * t, v0 + (v1 - v0) * t, 0.55f);
        }
    }

    /** A rectangular pool with coping, deeper water to the far end and a glint. */
    static void pool(F f, float u0, float v0, float u1, float v1) {
        f.rect(0xFFE4DED0, u0 - 1.4f, v0 - 1.4f, u1 + 1.4f, v1 + 1.4f);
        f.rect(0xFF4AA8C8, u0, v0, u1, v1);
        f.rect(0xFF3A94B8, u0 + (u1 - u0) * 0.45f, v0, u1, v1);
        f.rect(0x30000000, u0, v0, u1, v0 + 1.2f);
        f.rect(0x30000000, u0, v0, u0 + 1.2f, v1);
        f.line(0x70FFFFFF, 0.5f, u0 + (u1 - u0) * 0.2f, v0 + (v1 - v0) * 0.5f, u0 + (u1 - u0) * 0.4f, v0 + (v1 - v0) * 0.45f);
        f.line(0x60FFFFFF, 0.5f, u0 + (u1 - u0) * 0.55f, v0 + (v1 - v0) * 0.7f, u0 + (u1 - u0) * 0.75f, v0 + (v1 - v0) * 0.66f);
    }

    /** A kidney-shaped pool in the box u0..u1, v0..v1 (wider along u). */
    static void kidney(F f, float u0, float v0, float u1, float v1) {
        for (int pass = 0; pass < 3; pass++) {
            float e = pass == 0 ? 1.6f : pass == 1 ? 0 : -1.4f;
            float a0 = u0 - e, b0 = v0 - e, a1 = u1 + e, b1 = v1 + e, w = a1 - a0, h = b1 - b0;
            Path q = new Path();
            f.mv(q, a0 + w * 0.08f, b0 + h * 0.45f);
            f.qd(q, a0, b0, a0 + w * 0.3f, b0 + h * 0.05f);
            f.qd(q, a0 + w * 0.5f, b0 + h * 0.35f, a0 + w * 0.72f, b0);
            f.qd(q, a1 + w * 0.05f, b0, a1, b0 + h * 0.5f);
            f.qd(q, a1, b1 + h * 0.05f, a0 + w * 0.6f, b1);
            f.qd(q, a0 - w * 0.05f, b1 + h * 0.05f, a0 + w * 0.08f, b0 + h * 0.45f);
            f.fill(q, pass == 0 ? 0xFFE8E2D4 : pass == 1 ? 0xFF54B4D0 : 0xFF3E9CC0);
        }
        f.line(0x70FFFFFF, 0.5f, u0 + (u1 - u0) * 0.3f, v0 + (v1 - v0) * 0.55f, u0 + (u1 - u0) * 0.5f, v0 + (v1 - v0) * 0.5f);
        f.rect(0xFFC8CCD0, u1 - 3, v1 - 1.5f, u1 - 2.4f, v1 + 1.5f);
    }

    /** A saguaro or barrel cactus from above. */
    static void cactus(F f, float u, float v, float r, boolean arms) {
        f.circle(0x40000000, u + r * 0.6f, v + r * 0.9f, r);
        if (arms) {
            f.circle(0xFF4A7A48, u - r * 1.1f, v + r * 0.2f, r * 0.6f);
            f.circle(0xFF4A7A48, u + r * 1.0f, v - r * 0.3f, r * 0.55f);
        }
        f.circle(0xFF46744A, u, v, r);
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4;
            f.line(0xFF5E9058, 0.35f, u, v, u + (float) Math.cos(a) * r, v + (float) Math.sin(a) * r);
        }
        f.circle(0xFF7AAA68, u - r * 0.25f, v - r * 0.25f, r * 0.3f);
        if (!arms) f.circle(0xFFE0A0B8, u, v, r * 0.25f);
    }

    /** A tall narrow cypress: a dark tight crown and a long shadow. */
    static void cypress(F f, float u, float v, float r) {
        f.poly(0x38000000, u - r * 0.6f, v, u + r * 0.6f, v, u + r * 2.2f, v + r * 3.4f, u + r * 1.4f, v + r * 3.6f);
        f.circle(0xFF24442A, u, v, r);
        f.circle(0xFF2E5634, u - r * 0.2f, v - r * 0.2f, r * 0.7f);
        f.circle(0xFF40703E, u - r * 0.35f, v - r * 0.35f, r * 0.3f);
    }

    /** A conifer from above: a shadow, then layered star-shaped tiers. */
    static void pine(F f, float u, float v, float r, int col) {
        f.circle(0x3A000000, u + r * 0.45f, v + r * 0.55f, r);
        for (int tier = 0; tier < 3; tier++) {
            float rr = r * (1 - tier * 0.28f);
            int cc = tier == 0 ? City.darken(col, 0.78f) : tier == 1 ? col : City.lighten(col, 0.15f);
            float[] pts = new float[32];
            for (int k = 0; k < 16; k++) {
                double a = k * Math.PI / 8 + tier * 0.3;
                float rad = k % 2 == 0 ? rr : rr * 0.62f;
                pts[2 * k] = u - tier * r * 0.06f + (float) Math.cos(a) * rad;
                pts[2 * k + 1] = v - tier * r * 0.06f + (float) Math.sin(a) * rad;
            }
            f.poly(cc, pts);
        }
        f.circle(City.lighten(col, 0.3f), u - r * 0.15f, v - r * 0.15f, r * 0.12f);
    }

    /** A palm from above: fronds radiating from the crown. */
    static void palm(F f, float u, float v, float r) {
        for (int k = 0; k < 9; k++) {
            double a = k * 2 * Math.PI / 9 + 0.2;
            float eu = u + (float) Math.cos(a) * r, ev = v + (float) Math.sin(a) * r;
            f.line(0x30000000, 1.6f, u + 3, v + 4, eu + 3, ev + 4);
        }
        for (int k = 0; k < 9; k++) {
            double a = k * 2 * Math.PI / 9 + 0.2;
            float eu = u + (float) Math.cos(a) * r, ev = v + (float) Math.sin(a) * r;
            int col = k % 2 == 0 ? 0xFF4E8240 : 0xFF5E9448;
            f.line(col, 1.7f, u, v, eu, ev);
            f.line(City.lighten(col, 0.2f), 0.5f, u, v, u + (eu - u) * 0.8f, v + (ev - v) * 0.8f);
        }
        f.circle(0xFF7A6040, u, v, 1.1f);
    }

    /** A grey-green olive tree. */
    static void olive(F f, float u, float v, float r) {
        f.tree(u, v, r, 0xFF7E8E62);
        f.circle(0xFFA0AE88, u + r * 0.2f, v - r * 0.3f, r * 0.2f);
    }

    /** Mowing stripes over a lawn. */
    static void lawn(F f, int col, float u0, float v0, float u1, float v1, float w) {
        f.rect(col, u0, v0, u1, v1);
        int k = 0;
        for (float u = u0; u < u1; u += w, k++)
            if (k % 2 == 0) f.rect(City.lighten(col, 0.06f), u, v0, Math.min(u1, u + w), v1);
    }

    /** A flower bed: soil with clumps of blooms in the given colours. */
    static void bed(F f, Random r, float u0, float v0, float u1, float v1, int... cols) {
        f.rect(0xFF6A5038, u0, v0, u1, v1);
        int n = Math.min(24, (int) ((u1 - u0) * (v1 - v0) / 16) + 2);
        for (int k = 0; k < n; k++) {
            float u = u0 + 1 + r.nextFloat() * (u1 - u0 - 2), v = v0 + 1 + r.nextFloat() * (v1 - v0 - 2);
            f.circle(0xFF4E7A3E, u, v, 1.1f);
            f.circle(cols[r.nextInt(cols.length)], u - 0.2f, v - 0.2f, 0.6f);
        }
    }

    /** Vegetable rows across a plot. */
    static void veg(F f, Random r, float u0, float v0, float u1, float v1, boolean alongU) {
        f.rect(0xFF6E5236, u0, v0, u1, v1);
        int[] cc = {0xFF5E9A48, 0xFF7AAA50, 0xFF4E8A50, 0xFF8AA048};
        int k = 0;
        if (alongU) {
            for (float v = v0 + 1.5f; v < v1 - 0.8f; v += 2.6f, k++) {
                f.line(cc[k % 4], 1.8f, u0 + 1, v, u1 - 1, v);
                for (float u = u0 + 2 + (k % 2) * 2; u < u1 - 1.5f; u += 5) f.circle(City.lighten(cc[k % 4], 0.15f), u, v - 0.3f, 0.9f);
            }
        } else {
            for (float u = u0 + 1.5f; u < u1 - 0.8f; u += 2.6f, k++) {
                f.line(cc[k % 4], 1.8f, u, v0 + 1, u, v1 - 1);
                for (float v = v0 + 2 + (k % 2) * 2; v < v1 - 1.5f; v += 5) f.circle(City.lighten(cc[k % 4], 0.15f), u - 0.3f, v, 0.9f);
            }
        }
    }

    /** A garden shed with a little gabled roof (ridge along u when alongU). */
    static void shed(F f, float u, float v, float lu, float lv, int col) {
        f.rect(0x48000000, u + 1.2f, v + 1.8f, u + lu + 1.2f, v + lv + 1.8f);
        if (lu >= lv) gable(f, col, u, v, u + lu, v + lv, false);
        else gableV(f, col, u, v, u + lu, v + lv, false);
    }

    /** A sun lounger. */
    static void lounger(F f, float u, float v, boolean alongU, int col) {
        if (alongU) {
            f.rect(0x30000000, u - 3 + 0.6f, v - 1.1f + 0.8f, u + 3 + 0.6f, v + 1.1f + 0.8f);
            f.rect(0xFFE8E8E4, u - 3, v - 1.1f, u + 3, v + 1.1f);
            f.rect(col, u - 2.6f, v - 0.8f, u + 2.6f, v + 0.8f);
            f.rect(City.lighten(col, 0.25f), u - 2.6f, v - 0.8f, u - 1.2f, v + 0.8f);
        } else {
            f.rect(0x30000000, u - 1.1f + 0.6f, v - 3 + 0.8f, u + 1.1f + 0.6f, v + 3 + 0.8f);
            f.rect(0xFFE8E8E4, u - 1.1f, v - 3, u + 1.1f, v + 3);
            f.rect(col, u - 0.8f, v - 2.6f, u + 0.8f, v + 2.6f);
            f.rect(City.lighten(col, 0.25f), u - 0.8f, v - 2.6f, u + 0.8f, v - 1.2f);
        }
    }

    /** A parasol from above: eight panels in two colours. */
    static void parasol(F f, float u, float v, float r, int a, int b) {
        f.circle(0x38000000, u + r * 0.4f, v + r * 0.5f, r);
        for (int k = 0; k < 8; k++) {
            double a0 = k * Math.PI / 4, a1 = (k + 1) * Math.PI / 4;
            f.poly(Landmarks.shade(k % 2 == 0 ? a : b, k < 4 ? 0.9f : 1.08f), u, v, u + (float) Math.cos(a0) * r, v + (float) Math.sin(a0) * r,
                    u + (float) Math.cos(a1) * r, v + (float) Math.sin(a1) * r);
        }
        f.circle(0xFFE8E0D0, u, v, 0.6f);
    }

    /** A round table and chairs. */
    static void table(F f, float u, float v, float r, int col) {
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2 + Math.PI / 4;
            f.rect(0xFF5A4A3A, u + (float) Math.cos(a) * (r + 1.3f) - 0.8f, v + (float) Math.sin(a) * (r + 1.3f) - 0.8f,
                    u + (float) Math.cos(a) * (r + 1.3f) + 0.8f, v + (float) Math.sin(a) * (r + 1.3f) + 0.8f);
        }
        f.circle(0x38000000, u + 0.6f, v + 0.8f, r);
        f.circle(col, u, v, r);
        f.circle(City.lighten(col, 0.2f), u - r * 0.3f, v - r * 0.3f, r * 0.35f);
    }

    /** A washing line between posts with clothes pegged on it. */
    static void washing(F f, float u0, float v, float u1) {
        f.line(0xFF8A8A8A, 0.3f, u0, v, u1, v);
        f.circle(0xFF6A6A6A, u0, v, 0.6f);
        f.circle(0xFF6A6A6A, u1, v, 0.6f);
        int[] cc = {0xFFE8E8E8, 0xFF4A6AA0, 0xFFD06050, 0xFFE0C860, 0xFF60A070, 0xFFE8A0B0};
        int k = 0;
        for (float u = u0 + 1.5f; u < u1 - 1.5f; u += 2.6f, k++) {
            f.rect(0x28000000, u + 0.4f, v + 0.6f, u + 1.8f + 0.4f, v + 1.8f + 0.6f);
            f.rect(cc[(k * 5 + (int) u0) % cc.length], u, v, u + 1.8f, v + 1.6f);
        }
    }

    /** A paved area with a grid of joints. */
    static void paving(F f, int col, float u0, float v0, float u1, float v1, float step) {
        f.rect(col, u0, v0, u1, v1);
        int j = City.darken(col, 0.88f);
        for (float u = u0 + step; u < u1; u += step) f.line(j, 0.35f, u, v0, u, v1);
        for (float v = v0 + step; v < v1; v += step) f.line(j, 0.35f, u0, v, u1, v);
    }

    /** Cobbles: a base and a scatter of rounded setts. */
    static void cobbles(F f, Random r, int col, float u0, float v0, float u1, float v1) {
        f.rect(col, u0, v0, u1, v1);
        int j = City.darken(col, 0.8f);
        boolean along = u1 - u0 >= v1 - v0;
        if (along) for (float v = v0 + 2.2f; v < v1; v += 2.2f) f.line(j, 0.4f, u0, v, u1, v);
        else for (float u = u0 + 2.2f; u < u1; u += 2.2f) f.line(j, 0.4f, u, v0, u, v1);
        f.specks(r, (int) ((u1 - u0) * (v1 - v0) / 8), City.lighten(col, 0.1f), City.darken(col, 0.88f), u0, v0, u1, v1, 1.6f);
    }

    /** A row of solar panels. */
    static void solar(F f, float u0, float v0, float u1, float v1) {
        f.rect(0x40000000, u0 + 0.8f, v0 + 1, u1 + 0.8f, v1 + 1);
        f.rect(0xFFB8BCC0, u0, v0, u1, v1);
        f.rect(0xFF24345A, u0 + 0.4f, v0 + 0.4f, u1 - 0.4f, v1 - 0.4f);
        for (float u = u0 + 3; u < u1 - 0.5f; u += 3) f.line(0xFF6A7A9A, 0.3f, u, v0 + 0.4f, u, v1 - 0.4f);
        f.line(0xFF6A7A9A, 0.3f, u0 + 0.4f, (v0 + v1) / 2, u1 - 0.4f, (v0 + v1) / 2);
        f.line(0x50FFFFFF, 0.4f, u0 + 0.6f, v0 + 0.8f, u0 + (u1 - u0) * 0.4f, v0 + 0.8f);
    }

    /** An air-conditioning box with a round fan. */
    static void acUnit(F f, float u, float v, float s) {
        f.rect(0x40000000, u - s + 0.8f, v - s + 1, u + s + 0.8f, v + s + 1);
        f.rect(0xFFB0B4B8, u - s, v - s, u + s, v + s);
        f.circle(0xFF5A5E62, u, v, s * 0.75f);
        f.circle(0xFF8A8E92, u, v, s * 0.25f);
    }

    /** A glass skylight. */
    static void skylight(F f, float u0, float v0, float u1, float v1) {
        f.rect(0xFFD8D8D4, u0 - 0.5f, v0 - 0.5f, u1 + 0.5f, v1 + 0.5f);
        f.rect(0xFF6E8AA0, u0, v0, u1, v1);
        f.rect(0xFF90AAC0, u0, v0, (u0 + u1) / 2, v1);
        f.line(0x80FFFFFF, 0.4f, u0 + 0.5f, v0 + 0.5f, u0 + (u1 - u0) * 0.4f, v0 + 0.5f);
    }

    /** A hen pecking about. */
    static void hen(F f, float u, float v, int col) {
        f.oval(0x30000000, u - 0.8f, v - 0.4f, u + 1.6f, v + 1.2f);
        f.oval(col, u - 1.2f, v - 0.8f, u + 1, v + 0.8f);
        f.circle(col, u + 1.2f, v - 0.4f, 0.6f);
        f.circle(0xFFD03A2A, u + 1.3f, v - 0.8f, 0.3f);
    }

    /** A terracotta pot with a plant. */
    static void pot(F f, float u, float v, float r, int bloom) {
        f.circle(0x40000000, u + 0.6f, v + 0.8f, r);
        f.circle(0xFFB0603E, u, v, r);
        f.circle(0xFF4E7A3E, u, v, r * 0.75f);
        if (bloom != 0) {
            f.circle(bloom, u - r * 0.25f, v - r * 0.2f, r * 0.3f);
            f.circle(bloom, u + r * 0.25f, v + r * 0.15f, r * 0.25f);
        }
    }

    /** A hydrangea or flowering shrub. */
    static void shrub(F f, float u, float v, float r, int bloom) {
        f.circle(0x30000000, u + 1, v + 1, r);
        f.circle(0xFF4A7A3C, u, v, r);
        for (int k = 0; k < 3; k++) {
            double a = k * 2.1 + u * 0.1;
            f.circle(bloom, u + (float) Math.cos(a) * r * 0.45f, v + (float) Math.sin(a) * r * 0.45f, r * 0.38f);
        }
        f.circle(City.lighten(bloom, 0.25f), u - r * 0.3f, v - r * 0.3f, r * 0.22f);
    }

    /** A stepping-stone path from a to b. */
    static void stones(F f, float u0, float v0, float u1, float v1, float r, int col) {
        float len = (float) Math.hypot(u1 - u0, v1 - v0);
        int n = Math.max(1, (int) (len / (r * 2.6f)));
        for (int k = 0; k <= n; k++) {
            float t = k / (float) n, u = u0 + (u1 - u0) * t, v = v0 + (v1 - v0) * t;
            f.circle(0x30000000, u + 0.4f, v + 0.5f, r);
            f.circle(col, u, v, r);
        }
    }

    /** A wheelie bin or two. */
    static void bins(F f, float u, float v) {
        f.rect(0x40000000, u + 0.5f, v + 0.7f, u + 2.8f + 0.5f, v + 2.4f + 0.7f);
        f.rect(0xFF3A5A3A, u, v, u + 2.4f, v + 2.4f);
        f.rect(0xFF4A4C50, u + 2.8f, v, u + 5.2f, v + 2.4f);
        f.line(0x40FFFFFF, 0.3f, u, v + 0.5f, u + 5.2f, v + 0.5f);
    }

    /** Barrel (Spanish) tiles: a red roof face with rows of half-round tiles running down it, along v. */
    static void barrelTiles(F f, int col, float u0, float v0, float u1, float v1) {
        f.rect(City.darken(col, 0.8f), u0, v0, u1, v1);
        for (float u = u0 + 1.3f; u < u1 - 0.3f; u += 2.6f) {
            f.line(col, 1.6f, u, v0, u, v1);
        }
        for (float v = v0 + 6; v < v1; v += 6) f.line(0x22000000, 0.4f, u0, v, u1, v);
    }

    /** Barrel tiles running along u. */
    static void barrelTilesU(F f, int col, float u0, float v0, float u1, float v1) {
        f.rect(City.darken(col, 0.8f), u0, v0, u1, v1);
        for (float v = v0 + 1.3f; v < v1 - 0.3f; v += 2.6f) {
            f.line(col, 1.6f, u0, v, u1, v);
        }
        for (float u = u0 + 6; u < u1; u += 6) f.line(0x22000000, 0.4f, u, v0, u, v1);
    }

    /** A red Spanish-tile hipped roof: tiles run down each face. */
    static void tileHip(F f, int col, float u0, float v0, float u1, float v1) {
        float h = Math.min(u1 - u0, v1 - v0) / 2, au = u0 + h, av = v0 + h, bu = u1 - h, bv = v1 - h;
        hip(f, col, u0, v0, u1, v1, false);
        int ln = 0x38000000;
        for (float u = u0 + 2.8f; u < u1 - 0.5f; u += 2.8f) {
            float vt = Math.max(av, v0 + Math.min(u - u0, u1 - u)), vb = Math.min(bv, v1 - Math.min(u - u0, u1 - u));
            {
                f.line(ln, 0.45f, u, v0, u, Math.min(vt, av));
                f.line(ln, 0.45f, u, Math.max(vb, bv), u, v1);
            }
        }
        for (float v = v0 + 2.8f; v < v1 - 0.5f; v += 2.8f) {
            float ut = Math.min(au, u0 + Math.min(v - v0, v1 - v)), ub = Math.max(bu, u1 - Math.min(v - v0, v1 - v));
            f.line(ln, 0.45f, u0, v, ut, v);
            f.line(ln, 0.45f, ub, v, u1, v);
        }
        int hl = City.lighten(col, 0.3f);
        f.line(hl, 1.1f, au, av, bu, bv);
        f.line(hl, 0.8f, u0, v0, au, av);
        f.line(hl, 0.8f, u1, v0, bu, av);
        f.line(City.darken(col, 0.6f), 0.8f, u0, v1, au, bv);
        f.line(City.darken(col, 0.6f), 0.8f, u1, v1, bu, bv);
    }

    /** A gambrel (barn) roof with its ridge along u: four bands, steep at the eaves. */
    static void gambrel(F f, int col, float u0, float v0, float u1, float v1) {
        float h = v1 - v0;
        f.rect(City.lighten(col, 0.24f), u0, v0, u1, v0 + h * 0.18f);
        f.rect(City.lighten(col, 0.08f), u0, v0 + h * 0.18f, u1, v0 + h * 0.5f);
        f.rect(City.darken(col, 0.82f), u0, v0 + h * 0.5f, u1, v0 + h * 0.82f);
        f.rect(City.darken(col, 0.6f), u0, v0 + h * 0.82f, u1, v1);
        for (float u = u0 + 2; u < u1; u += 2) f.line(0x1E000000, 0.4f, u, v0, u, v1);
        f.line(City.lighten(col, 0.35f), 1f, u0, v0 + h * 0.5f, u1, v0 + h * 0.5f);
        f.line(0x40000000, 0.5f, u0, v0 + h * 0.18f, u1, v0 + h * 0.18f);
        f.line(0x40000000, 0.5f, u0, v0 + h * 0.82f, u1, v0 + h * 0.82f);
        f.frame(City.darken(col, 0.55f), 0.6f, u0, v0, u1, v1);
    }

    /** True if (u,v) in the block frame is at least pad clear of every building in q. */
    static boolean clear(int[][] q, float u, float v, float pad) {
        for (int[] b : q) {
            float u0 = b[0] * TT - pad, v0 = b[1] * TT - pad, u1 = (b[0] + b[2]) * TT + pad, v1 = (b[1] + b[3]) * TT + pad;
            if (u > u0 && u < u1 && v > v0 && v < v1) return false;
        }
        return true;
    }

    /** An agave: a spiky rosette. */
    static void agave(F f, float u, float v, float r) {
        f.circle(0x30000000, u + r * 0.4f, v + r * 0.5f, r * 0.8f);
        for (int k = 0; k < 10; k++) {
            double a = k * Math.PI / 5;
            f.line(k % 2 == 0 ? 0xFF6A9A88 : 0xFF7EAE98, 1.1f, u, v, u + (float) Math.cos(a) * r, v + (float) Math.sin(a) * r);
        }
        f.circle(0xFF8ABAA4, u, v, r * 0.25f);
    }

    // ------------------------------------------------------------------ 1. Victorian Mansion

    static void victorianMansion(List<Landmarks.Def> out) {
        // A turreted Queen Anne mansion with a wraparound porch, a gravel carriage loop and formal gardens.
        D d = new D("Victorian Mansion", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                ArrayList<int[]> a = new ArrayList<int[]>();
                int lu = Math.min(10, L - 6), lv = Math.min(Math.min(7, S - 6), lu - 1);
                boolean ch = L >= 18;
                a.add(B(ch ? 2 : (L - lu) / 2, 2, lu, lv, 3));
                if (ch) a.add(B(L - 5, 2, 3, 4, 2));
                return arr(a);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                int[] m = q[0];
                float hu0 = m[0] * T, hu1 = (m[0] + m[2]) * T, hv0 = m[1] * T, hv1 = (m[1] + m[3]) * T, hc = (hu0 + hu1) / 2;
                int grass = 0xFF5E8E46, gravel = 0xFFCBBE9E;
                lawn(f, grass, 0, 0, L, S, 8);
                // A clipped yew hedge along the back and a gravel apron round the house.
                for (float u = 4; u < L - 2; u += 4.5f) f.bush(u, 3.5f, 2.8f);
                f.rect(gravel, hu0 - 5, hv0 - 4, hu1 + 5, hv1 + 5);
                f.specks(r, 50, 0x20000000, 0x30FFFFFF, hu0 - 5, hv0 - 4, hu1 + 5, hv1 + 5, 0.7f);
                // The carriage loop with a round rose bed and an urn fountain in it, a drive out to the gates.
                float fv0 = hv1 + 5, fv1 = S - 5, cv = (fv0 + fv1) / 2, rv = (fv1 - fv0) / 2, rw = Math.min((hu1 - hu0) * 0.42f, 30);
                f.rect(gravel, hc - 3.5f, cv, hc + 3.5f, S);
                if (q.length > 1) {
                    float cu = (q[1][0] + q[1][2] / 2f) * T, cb = (q[1][1] + q[1][3]) * T;
                    f.rect(gravel, cu - 5, cb, cu + 5, cv + 3);
                    f.rect(gravel, hc, cv - 3, cu + 5, cv + 3);
                }
                f.oval(gravel, hc - rw, cv - rv, hc + rw, cv + rv);
                f.oval(grass, hc - rw + 6, cv - rv + 6, hc + rw - 6, cv + rv - 6);
                float br = Math.min(rv - 8, rw - 9);
                if (br > 4) {
                    f.oval(0xFF6E9E56, hc - br * 1.6f, cv - br, hc + br * 1.6f, cv + br);
                    for (int k = 0; k < 12; k++) {
                        double a = k * Math.PI / 6;
                        shrub(f, hc + (float) Math.cos(a) * br * 1.3f, cv + (float) Math.sin(a) * br * 0.72f, 2, k % 2 == 0 ? 0xFFD05868 : 0xFFF0E0E8);
                    }
                    f.circle(0xFFB8B0A0, hc, cv, 4.4f);
                    f.circle(0xFF9A9284, hc, cv, 3.4f);
                    f.circle(0xFF5A9AC0, hc, cv, 2.6f);
                    f.circle(0xFFC8C0AC, hc - 0.3f, cv - 0.3f, 1.1f);
                }
                // Rose beds along the house front, either side of the steps.
                bed(f, r, hu0, hv1 + 1, hc - 6, hv1 + 4, 0xFFD05868, 0xFFE8A0B0, 0xFFF0E8E0);
                bed(f, r, hc + 6, hv1 + 1, hu1, hv1 + 4, 0xFFD05868, 0xFFE8A0B0, 0xFFF0E8E0);
                // Side gardens: a white gazebo on one side, a copper beech and a glasshouse on the other.
                float lw = hu0 - 5;
                if (lw > 22) {
                    float gu = lw / 2, gv = S * 0.5f;
                    stones(f, gu, gv + 7, gu, S - 8, 1.4f, 0xFFC8C0AC);
                    float[] o = new float[16];
                    for (int k = 0; k < 8; k++) {
                        double a = k * Math.PI / 4 + Math.PI / 8;
                        o[2 * k] = gu + (float) Math.cos(a) * 7.5f + 1.2f;
                        o[2 * k + 1] = gv + (float) Math.sin(a) * 7.5f + 1.8f;
                    }
                    f.poly(0x40000000, o);
                    cone(f, gu, gv, 8.5f, 0xFFE8E4DA, 8);
                } else if (lw > 10) f.tree(lw / 2, S * 0.45f, Math.min(8, lw / 2), 0xFF3E7A3A);
                float rw0 = (q.length > 1 ? q[1][0] * T : L) - (hu1 + 5);
                if (q.length == 1 && rw0 > 10) f.tree(hu1 + 5 + rw0 / 2, S * 0.4f, Math.min(9, rw0 / 2), 0xFF7A3A34);
                if (q.length > 1) {
                    float gu0 = q[1][0] * T, gv0 = (q[1][1] + q[1][3]) * T + 6;
                    if (cv - 8 - gv0 > 12) {
                        f.rect(0x40000000, gu0 + 1, gv0 + 1.6f, gu0 + 3 * T + 1, cv - 8 + 1.6f);
                        f.rect(0xFFE8E8E4, gu0, gv0, gu0 + 3 * T, cv - 8);
                        f.rect(0xFF9ABCC8, gu0 + 1, gv0 + 1, gu0 + 3 * T - 1, cv - 9);
                        for (float u = gu0 + 4; u < gu0 + 3 * T - 1; u += 4) f.line(0xFFE8E8E4, 0.5f, u, gv0, u, cv - 8);
                        f.line(0xFFE8E8E4, 0.8f, gu0, (gv0 + cv - 8) / 2, gu0 + 3 * T, (gv0 + cv - 8) / 2);
                    }
                }
                f.tree(7, S - 9, 6.5f, 0xFF3E7A3A);
                f.tree(L - 7, S - 9, 6.5f, 0xFF7A3A34);
                // Wrought-iron railings along the street front, stone gate piers with lamps.
                f.line(0xFF2A2A2E, 0.6f, 1, S - 2, hc - 6, S - 2);
                f.line(0xFF2A2A2E, 0.6f, hc + 6, S - 2, L - 1, S - 2);
                for (float u = 1; u < L - 1; u += 1.6f)
                    if (Math.abs(u - hc) > 6) f.circle(0xFF2A2A2E, u, S - 2, 0.45f);
                f.rect(0xFFB8B0A0, hc - 8, S - 4, hc - 5, S - 1);
                f.rect(0xFFB8B0A0, hc + 5, S - 4, hc + 8, S - 1);
                f.lamp(hc - 6.5f, S - 2.5f);
                f.lamp(hc + 6.5f, S - 2.5f);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float W = f.L, H = f.S;
                int sl = 0xFF5A5468;
                if (part == 0) {
                    float pw = 5;
                    // The porch roof (painted tin) round the front and one side, with white trim on its edge.
                    f.rect(0xFF6E7C76, 0, H - pw, W, H);
                    f.rect(0xFF6E7C76, 0, 0, pw, H);
                    for (float u = 1.5f; u < W; u += 1.5f) f.line(0x30000000, 0.35f, u, H - pw, u, H);
                    for (float v = 1.5f; v < H - pw; v += 1.5f) f.line(0x30000000, 0.35f, 0, v, pw, v);
                    f.line(0xFFE8E4DA, 0.8f, 0.4f, H - 0.4f, W, H - 0.4f);
                    f.line(0xFFE8E4DA, 0.8f, 0.4f, 0, 0.4f, H);
                    f.rect(0x40000000, pw, H - pw, W, H - pw + 1.2f);
                    // The slate main roof with fish-scale courses, a front cross gable and a rear dormer.
                    hip(f, sl, pw, 0, W, H - pw, true);
                    float gu0 = pw + (W - pw) * 0.55f, gu1 = pw + (W - pw) * 0.85f, gv0 = (H - pw) * 0.45f;
                    f.rect(0x40000000, gu1, gv0 + 1, gu1 + 1.2f, H - pw + 1.2f);
                    gableV(f, sl, gu0, gv0, gu1, H - pw + 1.5f, true);
                    f.poly(0xFFE8E4DA, gu0, H - pw + 1.5f, (gu0 + gu1) / 2, H - pw - 1, gu1, H - pw + 1.5f, gu1 - 0.8f, H - pw + 1.5f, (gu0 + gu1) / 2, H - pw, gu0 + 0.8f, H - pw + 1.5f);
                    float bu0 = pw + (W - pw) * 0.18f, bu1 = pw + (W - pw) * 0.4f;
                    f.rect(0x40000000, bu1, 0, bu1 + 1.2f, (H - pw) * 0.55f + 1);
                    gableV(f, sl, bu0, -0.5f, bu1, (H - pw) * 0.55f, true);
                    f.poly(0xFFE8E4DA, bu0, -0.5f, (bu0 + bu1) / 2, 2, bu1, -0.5f);
                    dormer(f, sl, pw + (W - pw) * 0.68f, 0.5f, 6, 5.5f);
                    float h2 = Math.min(W - pw, H - pw) / 2;
                    for (float u = pw + h2; u <= W - h2 + 0.1f; u += 1.4f) f.circle(0xFF26262A, u, h2, 0.45f);
                    chimney(f, pw + h2 - 1, h2 - 5, 2);
                    chimney(f, W - h2 - 4, h2 + 1.5f, 2);
                    // The round corner turret with its conical slate cap.
                    float tr = Math.min(10, H * 0.3f);
                    f.circle(0xFFC8B89A, pw + tr * 0.6f, H - pw - tr * 0.5f, tr + 0.8f);
                    cone(f, pw + tr * 0.6f, H - pw - tr * 0.5f, tr, 0xFF6A6280, 12);
                } else {
                    // The carriage house: a slate gable with a copper-capped cupola and weathervane.
                    gable(f, sl, 0, 0, W, H, true);
                    float cu = W / 2, cv = H / 2;
                    f.rect(0x40000000, cu - 3 + 1, cv - 3 + 1.4f, cu + 3 + 1, cv + 3 + 1.4f);
                    f.rect(0xFFE8E4DA, cu - 3, cv - 3, cu + 3, cv + 3);
                    hip(f, 0xFF4E8A78, cu - 2.4f, cv - 2.4f, cu + 2.4f, cv + 2.4f, false);
                    f.line(0xFF2A2A2A, 0.4f, cu - 2.2f, cv, cu + 2.2f, cv);
                    f.circle(0xFFD8B048, cu, cv, 0.6f);
                }
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.OLD | Variants.SUB;
        d.minS = 12;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFF5A5468;
        out.add(d);
    }

    // ------------------------------------------------------------------ 2. Painted Ladies

    static final int[] LADY = {0xFF9AB8CC, 0xFFE8B4B8, 0xFFE8D48A, 0xFFA8CCB0, 0xFFC0A8D0, 0xFFE8B890};
    static final int[] LADY_TRIM = {0xFF3A5A7A, 0xFF8A3A4A, 0xFF5A7A3A, 0xFF7A4A8A, 0xFFE8E4DA, 0xFF2A6A6A};

    static void paintedLadies(List<Landmarks.Def> out) {
        // A row of tall, colourful Victorian houses facing a sloping park, each with a gabled front and a bay.
        D d = new D("Painted Ladies", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                ArrayList<int[]> a = new ArrayList<int[]>();
                int dv = S >= 13 ? 5 : 4;
                for (int u = 1; u + 2 <= L - 1; ) {
                    int w = Math.min(6, L - 1 - u);
                    w -= w % 2;
                    if (w < 4) break;
                    int d = w > dv ? dv : w - 1;
                    a.add(B(u, 1 + dv - d, w, d, 3));
                    u += w + 1;
                }
                return arr(a);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float fv = (q[0][1] + q[0][3]) * T, sw = fv + 9;
                // Back yards behind the row, the pavement and stoops in front, the park beyond.
                f.rect(0xFF6E9450, 0, 0, L, fv);
                for (int[] b : q) {
                    float u0 = b[0] * T, u1 = (b[0] + b[2]) * T;
                    for (float u = u0; u <= u1 + 0.1f; u += T * 2) f.line(0xFF8A6A48, 0.7f, u, 0, u, b[1] * T);
                    f.line(0xFF8A6A48, 0.7f, u0, 1, u1, 1);
                    // Side passages between the houses.
                    f.rect(0xFFB0A898, u1, b[1] * T, u1 + T, fv);
                    bins(f, u1 + 2, fv - 4);
                }
                f.rect(0xFFC0B8AC, 0, fv, L, sw);
                for (float u = 4; u < L; u += 8) f.line(0x20000000, 0.4f, u, fv, u, sw);
                lawn(f, 0xFF6EA050, 0, sw + 1, L, S, 9);
                f.rect(0xFF9A9A92, 0, sw, L, sw + 1);
                for (int[] b : q) {
                    int n = b[2] / 2;
                    for (int k = 0; k < n; k++) {
                        float a = (b[0] + k * 2) * T, su = a + T * (k % 2 == 0 ? 1.4f : 0.6f);
                        for (int j = 0; j < 4; j++)
                            f.rect(j % 2 == 0 ? 0xFFD8D4CC : 0xFFC4C0B8, su - 3, fv + j * 1.6f, su + 3, fv + j * 1.6f + 1.6f);
                        f.line(0xFFE8E4DA, 0.5f, su - 3.3f, fv, su - 3.3f, fv + 6.4f);
                        f.line(0xFFE8E4DA, 0.5f, su + 3.3f, fv, su + 3.3f, fv + 6.4f);
                        pot(f, a + (k % 2 == 0 ? 4 : T * 2 - 4), fv + 2.5f, 1.4f, 0xFFD05868);
                    }
                }
                // The park: a diagonal path, a few big trees, picnic blankets and benches facing the houses.
                if (S - sw > 30) {
                    f.line(0xFFD8CCB0, 4, 0, S - 2, L, sw + 6);
                    for (float u = 10; u < L - 6; u += 26) {
                        float v = sw + 6 + (S - sw - 12) * ((u * 7) % 10) / 10f;
                        f.tree(u, Math.max(sw + 10, Math.min(S - 8, v)), 7, 0xFF3E7A3A);
                    }
                    int[] blanket = {0xFFD05050, 0xFF5070C0, 0xFFE0C050};
                    for (int k = 0; k < 3; k++) {
                        float u = L * (0.2f + k * 0.28f) + 6, v = sw + (S - sw) * (k % 2 == 0 ? 0.62f : 0.4f);
                        f.rect(0x30000000, u + 0.6f, v + 0.8f, u + 10.6f, v + 8.8f);
                        f.rect(blanket[k], u, v, u + 10, v + 8);
                        for (int j = 1; j < 4; j++) {
                            f.line(0x60FFFFFF, 0.5f, u, v + j * 2, u + 10, v + j * 2);
                            f.line(0x60FFFFFF, 0.5f, u + j * 2.5f, v, u + j * 2.5f, v + 8);
                        }
                        f.rect(0xFF8A6A40, u + 6.5f, v + 2.5f, u + 9, v + 5);
                    }
                    for (float u = 12; u < L - 8; u += 30) f.bench(u, sw + 4, true);
                } else {
                    for (float u = 10; u < L - 4; u += 22) f.tree(u, (sw + S) / 2, Math.min(6, (S - sw) / 2), 0xFF3E7A3A);
                }
                for (float u = 6; u < L; u += 24) f.lamp(u, sw + 1.5f);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float W = f.L, H = f.S;
                int n = Math.max(1, Math.round(W / (2 * TT)));
                float hw = W / n;
                for (int k = 0; k < n; k++) {
                    int ci = (part * 3 + k) % LADY.length;
                    int col = LADY[ci], trim = LADY_TRIM[ci];
                    float a = k * hw, e = a + hw, mid = (a + e) / 2;
                    // A dark slate gable running front to back.
                    gableV(f, 0xFF54505A, a, 0, e, H - 3.5f, true);
                    // The street gable end in the house colour with white bargeboards, and the bay window below it.
                    f.poly(col, a + 0.5f, H - 3.5f, mid, H - 3.5f - hw * 0.42f, e - 0.5f, H - 3.5f);
                    f.line(0xFFF0ECE4, 0.7f, a + 0.5f, H - 3.5f, mid, H - 3.5f - hw * 0.42f);
                    f.line(0xFFF0ECE4, 0.7f, mid, H - 3.5f - hw * 0.42f, e - 0.5f, H - 3.5f);
                    f.circle(trim, mid, H - 3.5f - hw * 0.18f, 1.3f);
                    f.circle(0xFFF0ECE4, mid, H - 3.5f - hw * 0.18f, 0.6f);
                    f.rect(col, a, H - 3.5f, e, H);
                    f.line(trim, 0.8f, a, H - 3.2f, e, H - 3.2f);
                    f.poly(City.darken(col, 0.85f), a + hw * 0.2f, H - 3.5f, e - hw * 0.45f, H - 3.5f, e - hw * 0.52f, H, a + hw * 0.27f, H);
                    f.line(0xFFF0ECE4, 0.5f, a, H - 0.3f, e, H - 0.3f);
                    f.rect(0xFFF0ECE4, a, 0, a + 0.8f, H);
                    chimney(f, e - 3.6f, H * 0.18f, 1);
                    if (k > 0) f.line(0xFF3A363E, 0.8f, a, 0, a, H - 3.5f);
                }
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.OLD | Variants.MID;
        d.minS = 10;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFF54505A;
        out.add(d);
    }

    // ------------------------------------------------------------------ 3. Mid-Century Modern Homes

    static void midCentury(List<Landmarks.Def> out) {
        // Low flat-roofed houses with butterfly and wide-eaved roofs, each with a kidney pool and a breeze-block wall.
        D d = new D("Mid-Century Modern Homes", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                ArrayList<int[]> a = new ArrayList<int[]>();
                int nu = Math.max(1, (L - 1) / 6), cw = (L - 1) / nu;
                boolean two = S >= 13;
                for (int i = 0; i < nu; i++) {
                    int u = 1 + i * cw;
                    a.add(B(u, 1, cw - 1, 3, 1));
                    if (two) a.add(B(u, S - 4, cw - 1, 3, 1));
                }
                return arr(a);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                boolean two = Math.round(S / T) >= 13;
                f.rect(0xFFC8B890, 0, 0, L, S);
                f.specks(r, (int) (L * S / 40), 0x18000000, 0x20FFFFFF, 0, 0, L, S, 0.8f);
                float ym = two ? S / 2 : S;
                int hi = 0;
                for (int[] b : q) {
                    boolean top = b[1] * T < S / 2;
                    float u0 = b[0] * T, u1 = (b[0] + b[2]) * T, hv0 = b[1] * T, hv1 = (b[1] + b[3]) * T;
                    // Yard: a lawn, a concrete patio off the house, the kidney pool with loungers and a lollipop tree.
                    float ya = top ? hv1 : (two ? ym : 0), yb = top ? ym : hv0;
                    float cu0 = u0 - T * 0.5f, cu1 = u1 + T * 0.5f;
                    f.rect(0xFF7AA052, cu0 + 1, ya + 1, cu1 - 1, yb - 1);
                    float pv0 = top ? hv1 : hv0 - 8, pv1 = top ? hv1 + 8 : hv0;
                    paving(f, 0xFFD8D4C8, u0 + 2, pv0, u1 - 6, pv1, 4);
                    float kh = Math.min(16, (yb - ya) - 14), kw = Math.min(30, (u1 - u0) * 0.55f);
                    if (kh > 7) {
                        float kv = top ? hv1 + 10 : hv0 - 10 - kh;
                        kidney(f, u0 + 6, kv, u0 + 6 + kw, kv + kh);
                        lounger(f, u0 + 9 + kw, kv + kh * 0.3f, false, 0xFFE07A40);
                        lounger(f, u0 + 13 + kw, kv + kh * 0.3f, false, 0xFF40A0A0);
                        parasol(f, u0 + 11 + kw, kv + kh * 0.8f, 3.5f, 0xFFE8E4D8, 0xFFE07A40);
                    }
                    // A desert bed of gravel, boulders and succulents on the far side of the yard, a palm or lollipop tree.
                    float du0 = u0 + 6 + kw + 18;
                    if (u1 + 4 - du0 > 12 && yb - ya > 20) {
                        float dv0 = top ? ya + (yb - ya) * 0.35f : ya + 2, dv1 = top ? yb - 2 : yb - (yb - ya) * 0.35f;
                        f.roundRect(0xFFD8C8A0, du0, dv0, u1 + 4, dv1, 3);
                        f.specks(r, 20, 0xFFC0B088, 0xFFE8DCC0, du0, dv0, u1 + 4, dv1, 0.8f);
                        f.circle(0x40000000, du0 + 5, dv0 + 5.6f, 2.4f);
                        f.circle(0xFF9A9288, du0 + 4.4f, dv0 + 4.8f, 2.4f);
                        f.circle(0xFFB0A89C, du0 + 3.8f, dv0 + 4.2f, 1);
                        cactus(f, u1 - 2, (dv0 + dv1) / 2, 1.8f, true);
                        agave(f, du0 + 6, dv1 - 4, 2.6f);
                    }
                    if (hi++ % 2 == 0) palm(f, u1 - 4, top ? yb - 7 : ya + 7, 7);
                    else f.tree(u1 - 4, top ? yb - 7 : ya + 7, 5.5f, 0xFF5A8A3A);
                    // A breeze-block screen wall on the yard's side, and a palm on the corner.
                    float wu = cu1 - 1;
                    f.rect(0xFFE8E4DA, wu - 1.5f, ya + 2, wu, yb - 2);
                    for (float v = ya + 3; v < yb - 3; v += 2.2f) f.circle(0xFF9A9284, wu - 0.75f, v, 0.5f);
                    agave(f, u0 + 3, top ? yb - 4 : ya + 4, 2.5f);
                    // The front: a short drive and a bright front door step.
                    float dv0 = top ? 0 : hv1, dv1 = top ? hv0 : S;
                    f.rect(0xFFB0ACA4, u1 - 14, dv0, u1 - 2, dv1);
                    f.rect(0xFFE8A030, u0 + 4, top ? hv0 - 2 : hv1, u0 + 8, top ? hv0 : hv1 + 2);
                }
                if (two) palm(f, L * 0.5f + 3, S / 2, 7);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float W = f.L, H = f.S;
                int kind = part % 3;
                int wood = 0xFF8A5A36;
                // Exposed roof beams running out past the walls.
                for (float u = 3; u < W - 2; u += 7) {
                    f.rect(0x40000000, u + 0.6f, H, u + 1.8f, H + 2.4f);
                    f.rect(0xFF6A4428, u, -1.6f, u + 1.2f, 0);
                    f.rect(0xFF6A4428, u, H, u + 1.2f, H + 1.6f);
                }
                if (kind == 0) {
                    // Butterfly roof: two shallow planes falling to a central valley gutter.
                    f.rect(wood, 0, 0, W, H);
                    f.rect(0xFFD8D2C4, 1, 1, W - 1, H / 2);
                    f.rect(0xFFEAE4D6, 1, H / 2, W - 1, H - 1);
                    for (float u = 3; u < W - 1; u += 3) {
                        f.line(0x18000000, 0.4f, u, 1, u, H / 2);
                        f.line(0x14000000, 0.4f, u, H / 2, u, H - 1);
                    }
                    f.line(0xFF7A746A, 1.2f, 1, H / 2, W - 1, H / 2);
                    skylight(f, W * 0.62f, H * 0.62f, W * 0.62f + 4, H * 0.62f + 4);
                    stack(f, W * 0.2f, H * 0.2f, 4, 0xFFB09A80);
                } else if (kind == 1) {
                    // Flat roof with deep timber eaves, white gravel and a stone chimney.
                    f.rect(wood, 0, 0, W, H);
                    f.rect(0xFF6A4428, 0, H - 1, W, H);
                    f.rect(0xFFE4E0D6, 2.5f, 2.5f, W - 2.5f, H - 2.5f);
                    f.specks(r, (int) (W * H / 10), 0x22000000, 0x40FFFFFF, 2.5f, 2.5f, W - 2.5f, H - 2.5f, 0.6f);
                    for (float u = 4; u < W; u += 5) f.rect(0xFF6A4428, u, H - 2.5f, u + 1, H);
                    stack(f, W * 0.7f, H * 0.3f, 5, 0xFF9A8A78);
                    acUnit(f, W * 0.3f, H * 0.5f, 2);
                } else {
                    // A low-pitched standing-seam roof in a period turquoise.
                    int col = 0xFF5A9A98;
                    f.rect(City.lighten(col, 0.1f), 0, 0, W, H / 2);
                    f.rect(City.darken(col, 0.8f), 0, H / 2, W, H);
                    for (float u = 2; u < W; u += 2) f.line(0x30000000, 0.4f, u, 0, u, H);
                    f.line(City.lighten(col, 0.35f), 1f, 0, H / 2, W, H / 2);
                    f.frame(wood, 0.8f, 0, 0, W, H);
                    stack(f, W * 0.15f, H * 0.35f, 4, 0xFFB09A80);
                }
            }
        };
        d.ground = City.SAND;
        d.districts = Variants.SUB | Variants.MID;
        d.minS = 9;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFFE4E0D6;
        out.add(d);
    }

    // ------------------------------------------------------------------ 4. Adobe Compound

    /** An adobe flat roof: a rounded parapet, a mud deck, vigas poking out of one wall, canales and roof clutter. */
    static void adobeRoof(F f, Random r, float W, float H, int part) {
        int mud = 0xFFC8946A;
        f.roundRect(City.darken(mud, 0.8f), 0, 0, W, H, 3);
        f.roundRect(mud, 1.8f, 1.8f, W - 1.8f, H - 1.8f, 2);
        f.rect(City.lighten(mud, 0.14f), 2, 0.5f, W - 2, 1.6f);
        f.rect(City.lighten(mud, 0.08f), 0.5f, 2, 1.6f, H - 2);
        f.specks(r, (int) (W * H / 12), 0x1C000000, 0x18FFFFFF, 2, 2, W - 2, H - 2, 1);
        for (float u = 4; u < W - 2; u += 4.5f) {
            f.rect(0x40000000, u + 0.4f, H, u + 1.6f, H + 2.4f);
            f.rect(0xFF5A3E28, u, H - 0.4f, u + 1.3f, H + 1.6f);
        }
        for (float v = H * 0.3f; v < H; v += H * 0.45f) {
            f.rect(0x40000000, W, v + 0.8f, W + 3, v + 2.4f);
            f.rect(0xFF8A5A3A, W - 1.5f, v, W + 2.5f, v + 1.4f);
        }
        float up = part == 0 ? 0.42f : part >= 2 ? 0.55f : 0;
        if (up > 0) {
            // A stepped upper storey with its own vigas, a kiva chimney and a ladder up from the lower roof.
            boolean along = W >= H;
            float u0 = along ? W * up : 0, v0 = 0, u1 = W, v1 = along ? H * 0.62f : H * (1 - up);
            f.roundRect(0x40000000, u0 - 2, v0, u1, v1 + 3, 2.5f);
            f.roundRect(City.darken(mud, 0.84f), u0, v0, u1, v1, 2.5f);
            f.roundRect(City.lighten(mud, 0.06f), u0 + 1.5f, v0 + 1.5f, u1 - 1.5f, v1 - 1.5f, 1.5f);
            for (float u = u0 + 3; u < u1 - 2; u += 4) f.rect(0xFF5A3E28, u, v1 - 0.4f, u + 1.2f, v1 + 1.4f);
            float lu = along ? u0 - 4 : u0 + 4, lv = v1 - 4;
            f.line(0xFF7A5A3A, 0.6f, lu, lv, lu, v1 + 4);
            f.line(0xFF7A5A3A, 0.6f, lu + 2.2f, lv, lu + 2.2f, v1 + 4);
            for (float v = lv + 1; v < v1 + 4; v += 1.6f) f.line(0xFF7A5A3A, 0.5f, lu, v, lu + 2.2f, v);
            f.circle(0x40000000, u1 - 5.4f, v0 + 5.6f, 2.4f);
            f.circle(0xFFB48660, u1 - 6, v0 + 5, 2.4f);
            f.circle(0xFF3A2A20, u1 - 6, v0 + 5, 1.1f);
        }
        // Lower roof clutter: a round chimney, a rain barrel, a rack of drying chiles.
        float cu = W * 0.18f, cv = H * 0.68f;
        f.circle(0x40000000, cu + 0.7f, cv + 1, 2.1f);
        f.circle(0xFFB48660, cu, cv, 2.1f);
        f.circle(0xFF3A2A20, cu, cv, 1);
        if (part % 2 == 1 || W < 3.5f * TT) {
            f.circle(0x40000000, W * 0.5f + 0.8f, H * 0.3f + 1, 2);
            f.circle(0xFF5A6A7A, W * 0.5f, H * 0.3f, 2);
            f.circle(0xFF3A4A5A, W * 0.5f, H * 0.3f, 1.3f);
        }
        float ru = W * 0.3f, rv = H - 6;
        f.line(0xFF6A4A30, 0.5f, ru, rv, ru + 8, rv);
        for (float u = ru + 0.8f; u < ru + 8; u += 1.4f) f.oval(0xFFC0302A, u - 0.5f, rv - 0.4f, u + 0.5f, rv + 2.2f);
    }

    static void adobe(List<Landmarks.Def> out) {
        // Earthen flat-roofed houses round a dusty courtyard: vigas, a stepped upper storey, a well, an horno and cacti.
        D d = new D("Adobe Compound", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                ArrayList<int[]> a = new ArrayList<int[]>();
                int bd = S >= 13 ? 4 : 3;
                if (L >= 18) {
                    int w0 = (L - 3) / 2;
                    a.add(B(1, 1, w0, bd, 2));
                    a.add(B(w0 + 2, 1, L - 3 - w0, bd, 1));
                } else a.add(B(1, 1, L - 2, bd, 2));
                int wv = S - bd - 3;
                a.add(B(1, bd + 2, 3, wv, 1));
                a.add(B(L - 4, bd + 2, 3, wv, 1));
                return arr(a);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                f.rect(0xFFD8BC8C, 0, 0, L, S);
                f.specks(r, (int) (L * S / 10), 0x1C000000, 0x24FFFFFF, 0, 0, L, S, 1.2f);
                int[] w = q[q.length - 2];
                float cu0 = (w[0] + w[2]) * T, cu1 = q[q.length - 1][0] * T, cv0 = (q[0][1] + q[0][3]) * T, cv1 = S - T;
                // The courtyard: packed earth, flagstone paths crossing at a round stone well.
                f.rect(0xFFCCAA78, cu0 - 4, cv0, cu1 + 4, S - 4);
                float cx = (cu0 + cu1) / 2, cy = (cv0 + cv1) / 2;
                for (float u = cu0; u < cu1; u += 7) {
                    f.roundRect(0xFFB8A488, u + 0.5f, cy - 3.5f, u + 6.5f, cy + 3.5f, 1.2f);
                }
                for (float v = cv0; v < S - 4; v += 7) f.roundRect(0xFFB8A488, cx - 3.5f, v + 0.5f, cx + 3.5f, v + 6.5f, 1.2f);
                f.circle(0x40000000, cx + 1.5f, cy + 2, 7);
                f.circle(0xFF9A8A70, cx, cy, 7);
                f.circle(0xFFB0A088, cx - 0.6f, cy - 0.6f, 5.6f);
                f.circle(0xFF2A3A44, cx, cy, 4.2f);
                f.rect(0x40000000, cx - 7, cy + 0.6f, cx + 8, cy + 2);
                f.line(0xFF6A4A2E, 1.2f, cx - 7.5f, cy, cx + 7.5f, cy);
                f.circle(0xFF6A4A2E, cx - 7.5f, cy, 1);
                f.circle(0xFF6A4A2E, cx + 7.5f, cy, 1);
                // A big mesquite throwing shade, an horno (beehive oven), a ramada with a bench, a chile patch.
                float mu = cu0 + (cu1 - cu0) * 0.25f, mv = cv0 + (cv1 - cv0) * 0.7f;
                if (cu1 - cu0 > 60) f.tree(mu, mv, Math.min(12, (cu1 - cu0) * 0.14f), 0xFF7A8A50);
                float hu = cu1 - 9, hv = cv0 + 9;
                f.circle(0x40000000, hu + 2, hv + 2.6f, 6);
                f.circle(0xFFC08A5E, hu, hv, 6);
                f.circle(0xFFD09A6E, hu - 1.6f, hv - 1.6f, 3.2f);
                f.rect(0xFF2A2020, hu - 1.6f, hv + 4, hu + 1.6f, hv + 6.2f);
                float ru0 = cu0 + 3, rv0 = cv0 + 3, rw = Math.min(26, (cu1 - cu0) * 0.35f);
                if (rw > 14) {
                    f.rect(0x38000000, ru0 + 3, rv0 + 4, ru0 + rw + 3, rv0 + 16);
                    for (float u = ru0; u <= ru0 + rw; u += 2) f.line(0xFFA88A60, 0.8f, u, rv0, u, rv0 + 12);
                    f.line(0xFF6A4A2E, 1.4f, ru0, rv0 + 1, ru0 + rw, rv0 + 1);
                    f.line(0xFF6A4A2E, 1.4f, ru0, rv0 + 11, ru0 + rw, rv0 + 11);
                }
                if (cv1 - cv0 > 50) veg(f, r, cu1 - 22, cv1 - 14, cu1 - 4, cv1 - 4, true);
                cactus(f, cu0 + 7, cv1 - 5, 3.2f, true);
                cactus(f, cx + 13, cy - 11, 2.6f, false);
                agave(f, cx + 14, cy + 11, 4.5f);
                agave(f, cx - 14, cy + 9, 3.8f);
                for (int[] b : q) {
                    pot(f, b[0] * T + 4, (b[1] + b[3]) * T + 3, 2, 0xFFD04040);
                    f.line(0xFFC0302A, 1.6f, (b[0] + b[2]) * T - 4, (b[1] + b[3]) * T + 0.5f, (b[0] + b[2]) * T - 4, (b[1] + b[3]) * T + 6);
                }
                // The outer adobe wall round the compound with a timber gate; desert planting outside it.
                int wall = 0xFFB88A60;
                f.roundRect(0x40000000, 3, S - 3.5f, L - 1, S - 0.5f, 1.5f);
                f.roundRect(wall, 2, S - 5, cx - 7, S - 1.6f, 1.6f);
                f.roundRect(wall, cx + 7, S - 5, L - 2, S - 1.6f, 1.6f);
                f.rect(City.lighten(wall, 0.15f), 3, S - 5, cx - 8, S - 4.2f);
                f.rect(City.lighten(wall, 0.15f), cx + 8, S - 5, L - 3, S - 4.2f);
                f.rect(0xFF7A5232, cx - 7, S - 4.6f, cx + 7, S - 2.2f);
                f.line(0xFF4A3222, 0.4f, cx, S - 4.6f, cx, S - 2.2f);
                for (float v = cv0 + 6; v < S - 8; v += 24) if (clear(q, 1.6f, v, 0)) cactus(f, 2, v, 1.8f, false);
                if (clear(q, L - 9, S - 9, 4)) cactus(f, L - 9, S - 9, 3, true);
                for (float u = 12; u < L - 8; u += 40) {
                    if (Math.abs(u - cx) < 16) continue;
                    f.oval(0x30000000, u - 2, S - 8.4f, u + 4, S - 5.6f);
                    f.oval(0xFF6A9A5A, u - 3, S - 9, u + 1, S - 6);
                    f.oval(0xFF6A9A5A, u, S - 10, u + 3.4f, S - 7.2f);
                    f.circle(0xFFE0C040, u + 1.6f, S - 10, 0.6f);
                }
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                adobeRoof(f, r, f.L, f.S, part);
            }
        };
        d.ground = City.SAND;
        d.districts = Variants.SUB | Variants.OLD;
        d.minS = 10;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFFC8946A;
        out.add(d);
    }

    // ------------------------------------------------------------------ 5. A-Frame Cabins

    static void aFrames(List<Landmarks.Def> out) {
        // Steep A-frame cabins among the pines, each with a front deck, round a clearing with a fire pit.
        D d = new D("A-Frame Cabins", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                ArrayList<int[]> a = new ArrayList<int[]>();
                int n = Math.max(2, (L - 1) / 4), span = n * 4 - 1, u0 = (L - span) / 2;
                for (int i = 0; i < n; i++) a.add(B(u0 + i * 4, i % 2 == 0 ? 1 : S - 6, 3, 4, 2));
                return arr(a);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                f.rect(0xFF5A6E3E, 0, 0, L, S);
                f.specks(r, (int) (L * S / 14), 0xFF6A5A3A, 0xFF4E6236, 0, 0, L, S, 0.9f);
                float cx = L / 2, cy = S / 2;
                // A clearing with the fire pit and log seats, gravel paths out to each deck.
                f.oval(0xFF8A7A58, cx - 18, cy - 13, cx + 18, cy + 13);
                f.specks(r, 30, 0x20000000, 0x20FFFFFF, cx - 12, cy - 8, cx + 12, cy + 8, 0.7f);
                for (int[] b : q) {
                    float bu = (b[0] + 1.5f) * T, front = (b[1] + b[3]) * T;
                    f.line(0xFF9A8A68, 3, bu, front + 6, cx, cy);
                }
                for (int k = 0; k < 10; k++) {
                    double a = k * Math.PI / 5;
                    f.circle(0xFF8A8478, cx + (float) Math.cos(a) * 3.6f, cy + (float) Math.sin(a) * 3.6f, 1.1f);
                }
                f.circle(0xFF2A2420, cx, cy, 2.8f);
                f.circle(0xFFE0782A, cx, cy, 1.4f);
                f.circle(0xFFF0C050, cx - 0.3f, cy - 0.3f, 0.6f);
                for (int k = 0; k < 3; k++) {
                    double a = k * 2 * Math.PI / 3 + 0.4;
                    float lu = cx + (float) Math.cos(a) * 8, lv = cy + (float) Math.sin(a) * 6;
                    f.turn(lu, lv, (float) Math.toDegrees(a) + 90);
                    Landmarks.rect(f.c, f.p, 0x40000000, -3.6f, -0.6f, 4.4f, 2.2f);
                    Landmarks.roundRect(f.c, f.p, 0xFF7A5634, -4, -1.2f, 4, 1.2f, 1.2f);
                    Landmarks.circle(f.c, f.p, 0xFFB08A60, 4, 0, 1.1f);
                    f.c.restore();
                }
                // Decks out front of each cabin: planks, two Adirondack chairs and a woodpile at the side.
                for (int i = 0; i < q.length; i++) {
                    int[] b = q[i];
                    float u0 = b[0] * T, u1 = (b[0] + b[2]) * T, fv = (b[1] + b[3]) * T;
                    f.rect(0x40000000, u0 + 1.5f, fv + 1.5f, u1 + 1.5f, fv + 9.5f);
                    f.rect(0xFFA07A50, u0, fv, u1, fv + 8);
                    for (float u = u0 + 1.6f; u < u1; u += 1.6f) f.line(0xFF7A5634, 0.35f, u, fv, u, fv + 8);
                    int[] chair = {0xFFB04030, 0xFF3A7A8A, 0xFFE0B040};
                    for (int k = 0; k < 2; k++) {
                        float su = u0 + 10 + k * 12, sv = fv + 4;
                        f.rect(0x30000000, su - 2 + 0.6f, sv - 2 + 0.8f, su + 2 + 0.6f, sv + 2 + 0.8f);
                        f.rect(chair[(i + k) % 3], su - 2, sv - 2, su + 2, sv + 2);
                        f.rect(City.darken(chair[(i + k) % 3], 0.75f), su - 2, sv - 2, su + 2, sv - 0.8f);
                    }
                    for (int k = 0; k < 4; k++) f.circle(0xFF8A6440, u1 + 2, b[1] * T + 6 + k * 2, 1);
                    f.circle(0xFFB89068, u1 + 2, b[1] * T + 6, 0.5f);
                }
                // A canoe pulled up by the path.
                if (L > 15 * T) {
                    f.turn(L - 14, cy + 2, 70);
                    Landmarks.oval(f.c, f.p, 0x40000000, -8, -1.4f, 9, 3.4f);
                    Landmarks.oval(f.c, f.p, 0xFFB04030, -9, -2.2f, 9, 2.2f);
                    Landmarks.oval(f.c, f.p, 0xFFD8C8A0, -7.5f, -1.3f, 7.5f, 1.3f);
                    Landmarks.line(f.c, f.p, 0xFF8A6440, 0.6f, 0, -1.4f, 0, 1.4f);
                    f.c.restore();
                }
                // Pines everywhere there is room.
                Random tr = new Random(7);
                for (int k = 0; k < (int) (L * S / 900); k++) {
                    float u = 6 + tr.nextFloat() * (L - 12), v = 6 + tr.nextFloat() * (S - 12);
                    if (!clear(q, u, v, 11) || Math.hypot(u - cx, (v - cy) * 1.3f) < 26) continue;
                    pine(f, u, v, 6.5f + tr.nextFloat() * 3.5f, tr.nextBoolean() ? 0xFF2E5A34 : 0xFF3A6A3A);
                }
                for (float u = 7; u < L; u += 22) {
                    if (clear(q, u, 6, 8)) pine(f, u, 6, 7, 0xFF2E5A34);
                    if (clear(q, u + 11, S - 6, 8)) pine(f, u + 11, S - 6, 7, 0xFF3A6A3A);
                }
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                // The A: two steep shingle slopes to the ground, a ridge cap, a stove flue and a glazed front gable.
                float W = f.L, H = f.S;
                int[] cols = {0xFF4E5A3E, 0xFF7A4A34, 0xFF5A4A40, 0xFF3E4E5A};
                int col = cols[part % cols.length];
                f.rect(City.lighten(col, 0.1f), 0, 0, W, H / 2);
                f.rect(City.darken(col, 0.72f), 0, H / 2, W, H);
                for (float v = 2; v < H / 2 - 0.5f; v += 2) {
                    f.line(0x30000000, 0.4f, 0, v, W, v);
                    f.line(0x30000000, 0.4f, 0, H - v, W, H - v);
                }
                f.rect(0xFF3A3434, 0, H / 2 - 0.8f, W, H / 2 + 0.8f);
                f.line(City.lighten(col, 0.3f), 0.4f, 0, H / 2 - 0.8f, W, H / 2 - 0.8f);
                f.rect(0xFFC8A070, W - 1.4f, 0, W, H);
                f.rect(0xFFC8A070, 0, 0, 1, H);
                f.circle(0x40000000, W * 0.35f + 0.8f, H * 0.3f + 1, 1.3f);
                f.circle(0xFF6A6E72, W * 0.35f, H * 0.3f, 1.3f);
                f.circle(0xFF2A2A2A, W * 0.35f, H * 0.3f, 0.6f);
                skylight(f, W * 0.55f, H * 0.62f, W * 0.55f + 5, H * 0.62f + 3);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.PARK | Variants.SUB;
        d.minS = 10;
        d.minL = 10;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFF4E5A3E;
        out.add(d);
    }

    // ------------------------------------------------------------------ 6. Hilltop Villa

    static void hilltopVilla(List<Landmarks.Def> out) {
        // A Mediterranean villa on the top terrace with a pool and pergola, olive and lavender terraces below, cypresses.
        D d = new D("Hilltop Villa", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                ArrayList<int[]> a = new ArrayList<int[]>();
                int lu = Math.min(9, L - 7), lv = Math.min(S >= 13 ? 5 : 4, lu - 1), u = L >= 16 ? 2 : 1;
                a.add(B(u, 1, lu, lv, 2));
                a.add(B(L - 4, 1, 3, 2, 1));
                return arr(a);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                int[] m = q[0];
                float hu0 = m[0] * T, hu1 = (m[0] + m[2]) * T, hv1 = (m[1] + m[3]) * T;
                float t1 = hv1 + Math.min(40, (S - hv1) * 0.45f), t2 = t1 + (S - t1) * 0.5f;
                // Top terrace: travertine paving, the pool before the villa, a vine pergola by the pool house.
                paving(f, 0xFFE0D4B8, 0, 0, L, t1, 6);
                float pu0 = hu0 + 6, pu1 = Math.min(hu1 + 10, L - 8), pv0 = hv1 + 6, pv1 = t1 - 6;
                if (pv1 - pv0 > 8) {
                    pool(f, pu0, pv0, pu1, pv1);
                    f.rect(0xFF2E7EA8, pu0, pv1 - 1.2f, pu1, pv1);
                    for (float u = pu0 + 6; u < pu1 - 4; u += 9) lounger(f, u, pv0 - 3.5f, true, 0xFFE8E4DA);
                }
                float gu0 = q[1][0] * T - 2, gv0 = (q[1][1] + q[1][3]) * T + 2, gu1 = L - 2, gv1 = Math.min(t1 - 4, gv0 + 22);
                f.rect(0x30000000, gu0 + 2, gv0 + 3, gu1 + 2, gv1 + 3);
                for (float u = gu0; u <= gu1; u += 3) f.line(0xFF8A6A48, 0.8f, u, gv0, u, gv1);
                f.line(0xFF6A4A30, 1.2f, gu0, gv0 + 1, gu1, gv0 + 1);
                f.line(0xFF6A4A30, 1.2f, gu0, gv1 - 1, gu1, gv1 - 1);
                for (int k = 0; k < 14; k++) {
                    float u = gu0 + (k * 37 % 100) / 100f * (gu1 - gu0), v = gv0 + (k * 61 % 100) / 100f * (gv1 - gv0);
                    f.circle(k % 3 == 0 ? 0xFF6A8A3A : 0xFF4E7A34, u, v, 2.4f);
                    if (k % 4 == 0) f.circle(0xFF6A3A6A, u + 0.8f, v + 0.6f, 0.8f);
                }
                table(f, (gu0 + gu1) / 2, (gv0 + gv1) / 2 + 2, 2.6f, 0xFFE8E4DA);
                for (int k = 0; k < 5; k++) pot(f, hu0 + 4 + k * (hu1 - hu0 - 8) / 4f, hv1 + 2.5f, 1.6f, k % 2 == 0 ? 0xFFD04878 : 0xFFE8E4DA);
                // Retaining walls with their drop shadows, terraces of olives then lavender, a stair down the side.
                f.rect(0xFF8AA858, 0, t1, L, t2);
                f.rect(0xFF8A9A58, 0, t2, L, S);
                f.rect(0x40000000, 0, t1, L, t1 + 4);
                f.rect(0x40000000, 0, t2, L, t2 + 4);
                f.rect(0xFFB8A888, 0, t1 - 2.4f, L, t1);
                f.rect(0xFFB8A888, 0, t2 - 2.4f, L, t2);
                for (float u = 1; u < L; u += 6) {
                    f.line(0x30000000, 0.35f, u, t1 - 2.4f, u, t1);
                    f.line(0x30000000, 0.35f, u + 3, t2 - 2.4f, u + 3, t2);
                }
                for (float u = 16; u < L - 14; u += 22) olive(f, u + ((int) u % 3) * 2, (t1 + t2) / 2 + ((int) u % 2 == 0 ? 1.5f : -1), Math.min(7.5f, (t2 - t1) / 2 - 1));
                for (float v = t2 + 4; v < S - 2; v += 4.5f) {
                    f.line(0xFF7A6AA0, 2.6f, 12, v, L - 14, v);
                    for (float u = 14 + (v % 9 < 4.5f ? 5 : 0); u < L - 14; u += 10) f.circle(0xFF9080C0, u, v - 0.4f, 1);
                }
                float su = L - 8;
                f.rect(0xFFD0C4A8, su - 3, t1 - 3, su + 3, S);
                for (float v = t1 - 1; v < S; v += 2.5f) f.line(0x30000000, 0.4f, su - 3, v, su + 3, v);
                // Cypresses down the edge of the drive and a pair framing the stair.
                for (float v = 8; v < S - 4; v += 16) cypress(f, 5, v, 3.8f);
                cypress(f, su - 6, t1 + 4, 2.6f);
                cypress(f, su + 6, t1 + 4, 2.6f);
                cypress(f, L - 3, t2 + 4, 2.6f);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float W = f.L, H = f.S;
                int tile = 0xFFB8603E;
                if (part == 0) {
                    // Terracotta hipped roof with a loggia roof on the front and a square belvedere tower at one end.
                    float lg = 4.5f;
                    f.rect(0xFFE8DCC4, 0, 0, W, H);
                    tileHip(f, tile, 0, 0, W, H - lg);
                    f.rect(0x40000000, 0, H - lg, W, H - lg + 1.4f);
                    barrelTiles(f, City.lighten(tile, 0.06f), 0, H - lg + 0.4f, W * 0.7f, H);
                    f.line(0xFFE8DCC4, 0.7f, 0, H - 0.3f, W * 0.7f, H - 0.3f);
                    float ts = Math.min(H * 0.8f, 26), tu = W - ts - 0.5f;
                    f.rect(0x48000000, tu + 1.5f, 1 + 2, tu + ts + 1.5f, 1 + ts + 2);
                    f.rect(0xFFE8DCC4, tu - 0.6f, 0.4f, tu + ts + 0.6f, 1.6f + ts);
                    tileHip(f, City.lighten(tile, 0.05f), tu, 1, tu + ts, 1 + ts);
                    stack(f, W * 0.25f, H * 0.18f, 3.6f, 0xFFE8DCC4);
                    stack(f, W * 0.48f, H * 0.4f, 3.6f, 0xFFE8DCC4);
                } else {
                    tileHip(f, tile, 0, 0, W, H);
                }
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.SUB | Variants.PARK;
        d.minS = 10;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFFB8603E;
        out.add(d);
    }

    // ------------------------------------------------------------------ 7. Shotgun Houses

    static final int[] SHOT = {0xFFA8D0C0, 0xFFE8D070, 0xFFC0A8D8, 0xFFE89A80, 0xFF8AB8D8, 0xFFB8D088};

    static void shotgun(List<Landmarks.Def> out) {
        // A row of long, narrow shotgun houses, one room wide, each with a deep front porch, a gable to the street and a yard.
        D d = new D("Shotgun Houses", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                ArrayList<int[]> a = new ArrayList<int[]>();
                int n = (L - 1) / 3, u0 = (L - (n * 3 - 1)) / 2, lv = Math.min(8, S - 4);
                for (int i = 0; i < n; i++) a.add(B(u0 + i * 3, 1, 2, lv, 1));
                return arr(a);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float fv = (q[0][1] + q[0][3]) * T;
                f.rect(0xFF6E9450, 0, 0, L, S);
                // Back yards, gravel side alleys, front yards behind a low iron fence, the brick banquette.
                for (int i = 0; i < q.length; i++) {
                    int[] b = q[i];
                    float u0 = b[0] * T, u1 = (b[0] + b[2]) * T;
                    f.rect(0xFFB8AC94, u1, 0, u1 + T, fv);
                    f.specks(r, 20, 0x22000000, 0x22FFFFFF, u1, 0, u1 + T, fv, 0.7f);
                    if (i % 2 == 0) washing(f, u0 + 2, 6, u1 + T - 2);
                    else shed(f, u0 + 4, 3, 10, 7, 0xFF7A7A72);
                    f.line(0xFF8A6A48, 0.7f, u1 + T, 0, u1 + T, b[1] * T);
                    // Front yard: a walk to the porch steps, a banana plant or crape myrtle.
                    f.rect(0xFFC8C0B0, u0 + 3, fv, u0 + 9, S - 6);
                    bed(f, r, u0 + 11, fv + 1, u1 - 1, fv + 5, 0xFFE06080, 0xFFE8C040, 0xFFF0F0F0, 0xFFE08040);
                    if (S - 6 - fv > 30) bed(f, r, u0 - T * 0.4f, S - 11, u0 + 2, S - 7, 0xFF9070D0, 0xFFF0F0F0);
                    for (int j = 0; j < 3; j++) f.rect(j % 2 == 0 ? 0xFFD4CCBC : 0xFFBCB4A4, u0 + 2, fv + j * 1.6f, u0 + 10, fv + j * 1.6f + 1.6f);
                    if (S - 6 - fv > 24) {
                        shrub(f, u0 + 13, S - 10, 2.6f, i % 2 == 0 ? 0xFFE8E0F0 : 0xFFD06080);
                        shrub(f, u0 - 1, S - 10, 2.4f, 0xFFE8C040);
                    }
                    if (S - 6 - fv > 18) {
                        if (i % 2 == 0) palm(f, u1 - 6, fv + 9, 6);
                        else f.tree(u1 - 6, fv + 9, 5.5f, 0xFFC86A90);
                    }
                    f.line(0xFF26262A, 0.6f, u0 - T * 0.5f, S - 6, u0 + 2.5f, S - 6);
                    f.line(0xFF26262A, 0.6f, u0 + 9.5f, S - 6, u1 + T * 0.5f, S - 6);
                    for (float u = u0 - T * 0.5f; u < u1 + T * 0.5f; u += 1.4f)
                        if (u < u0 + 2.5f || u > u0 + 9.5f) f.circle(0xFF26262A, u, S - 6, 0.4f);
                }
                f.rect(0xFFA0624A, 0, S - 5, L, S);
                for (float u = 0; u < L; u += 3) f.line(0x30000000, 0.35f, u, S - 5, u, S);
                f.line(0x30000000, 0.35f, 0, S - 2.5f, L, S - 2.5f);
                for (float u = 12; u < L; u += 40) f.tree(u, S - 4, 8, 0xFF3A6A34);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                // The roof runs front (high u in the house's frame) to back: a tin porch roof, then the gable end in colour.
                float W = f.L, H = f.S, pr = 7;
                int col = SHOT[part % SHOT.length];
                boolean camel = part % 3 == 2;
                int rc = part % 2 == 0 ? 0xFF6A6C70 : 0xFF7A5A4A;
                gable(f, rc, 0, 0, W - pr, H, true);
                if (camel) {
                    f.rect(0x40000000, W * 0.42f, 0, W * 0.42f + 1.4f, H);
                    gable(f, City.lighten(rc, 0.06f), 0, 0, W * 0.42f, H, true);
                }
                f.poly(col, W - pr, 0.4f, W - pr - H * 0.45f, H / 2, W - pr, H - 0.4f);
                f.line(0xFFF0ECE4, 0.7f, W - pr, 0.4f, W - pr - H * 0.45f, H / 2);
                f.line(0xFFF0ECE4, 0.7f, W - pr - H * 0.45f, H / 2, W - pr, H - 0.4f);
                f.circle(0xFFF0ECE4, W - pr - H * 0.18f, H / 2, 1.1f);
                // Porch roof: standing-seam tin, gingerbread brackets on its edge.
                f.rect(0x40000000, W - pr, 0, W - pr + 1.4f, H);
                int pc = City.lighten(col, 0.2f);
                f.rect(pc, W - pr, 0, W, H);
                for (float v = 1.6f; v < H; v += 1.6f) f.line(City.darken(pc, 0.85f), 0.4f, W - pr, v, W, v);
                f.rect(col, W - 1.2f, 0, W, H);
                for (float v = 1; v < H; v += 2) f.circle(0xFFF0ECE4, W - 0.6f, v, 0.5f);
                f.rect(col, 0, 0, W - pr, 0.8f);
                f.rect(City.darken(col, 0.8f), 0, H - 0.8f, W - pr, H);
                chimney(f, W * 0.28f, H / 2 - 1.6f, 1);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.OLD | Variants.SUB;
        d.minS = 10;
        d.minL = 10;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFF6A6C70;
        out.add(d);
    }

    // ------------------------------------------------------------------ 8. Art Deco Apartments

    static void artDeco(List<Landmarks.Def> out) {
        // A pastel Art Deco apartment block stepping up in tiers with rounded corners, speed-line bands and a central fin.
        D d = new D("Art Deco Apartments", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                ArrayList<int[]> a = new ArrayList<int[]>();
                int lu = Math.min(L - 4, 14), lv = Math.min(S - 5, 8);
                if (lu == lv) lv--;
                a.add(B((L - lu) / 2, 1, lu, lv, 7));
                return arr(a);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                int[] m = q[0];
                float hu0 = m[0] * T, hu1 = (m[0] + m[2]) * T, hv1 = (m[1] + m[3]) * T, cx = (hu0 + hu1) / 2;
                f.rect(0xFF8AB070, 0, 0, L, S);
                // Terrazzo forecourt: a sunburst of two colours with brass strips, a stepped fountain on the axis.
                f.rect(0xFFE8DCCC, 0, hv1, L, S);
                float cv = hv1 + (S - hv1) * 0.55f, rr = Math.min((S - hv1) * 0.5f, L * 0.4f);
                f.c.save();
                f.clip(0, hv1, L, S);
                for (int k = 0; k < 18; k++) {
                    double a0 = Math.PI + k * Math.PI / 18, a1 = Math.PI + (k + 1) * Math.PI / 18;
                    if (k % 2 == 0)
                        f.poly(0xFFE0B8B0, cx, S, cx + (float) Math.cos(a0) * L, S + (float) Math.sin(a0) * L, cx + (float) Math.cos(a1) * L, S + (float) Math.sin(a1) * L);
                }
                for (int k = 0; k <= 18; k += 3) {
                    double a = Math.PI + k * Math.PI / 18;
                    f.line(0xFFC8A050, 0.5f, cx, S, cx + (float) Math.cos(a) * L, S + (float) Math.sin(a) * L);
                }
                f.c.restore();
                f.rect(0xFFE8DCCC, 0, hv1, L, hv1 + 5);
                f.line(0xFFC8A050, 0.6f, 0, hv1 + 5, L, hv1 + 5);
                for (int k = 3; k >= 0; k--) f.circle(k % 2 == 0 ? 0xFFD8CCB8 : 0xFFB4DCC8, cx, cv, rr * 0.18f + k * 2.2f);
                f.circle(0xFF5AA8C8, cx, cv, rr * 0.18f);
                f.circle(0xFFC8A050, cx, cv, 1.6f);
                for (int k = 0; k < 8; k++) {
                    double a = k * Math.PI / 4;
                    f.circle(0x90FFFFFF, cx + (float) Math.cos(a) * rr * 0.1f, cv + (float) Math.sin(a) * rr * 0.1f, 0.6f);
                }
                // Royal palms in pairs along the front, clipped hedges in geometric beds to the sides.
                for (int s = -1; s <= 1; s += 2)
                    for (int k = 0; k < 3; k++) {
                        float u = cx + s * (18 + k * 26);
                        if (u > 6 && u < L - 6) palm(f, u, S - 9, 8);
                    }
                for (int s = 0; s < 2; s++) {
                    float u0 = s == 0 ? 2 : hu1 + 3, u1 = s == 0 ? hu0 - 3 : L - 2;
                    if (u1 - u0 < 8) continue;
                    float m0 = 6, m1 = hv1 - 4;
                    f.roundRect(0x30000000, u0 + 1, m0 + 1, u1 + 1, m1 + 1, 4);
                    f.roundRect(0xFF4E7A3E, u0, m0, u1, m1, 4);
                    f.roundRect(0xFF8AB070, u0 + 2.5f, m0 + 2.5f, u1 - 2.5f, m1 - 2.5f, 3);
                    for (float v = m0 + 9; v < m1 - 6; v += 16) {
                        f.circle(0xFFE8DCCC, (u0 + u1) / 2, v, Math.min(4, (u1 - u0) / 2 - 3));
                        shrub(f, (u0 + u1) / 2, v, Math.min(3, (u1 - u0) / 2 - 4), 0xFFE07088);
                    }
                }
                f.lamp(cx - 12, hv1 + 3);
                f.lamp(cx + 12, hv1 + 3);
                f.rect(0xFF3A8A80, cx - 8, hv1, cx + 8, hv1 + 1.5f);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float W = f.L, H = f.S;
                int pink = 0xFFE8C4B8, mint = 0xFFB4DCC8, cream = 0xFFF0E6D4, teal = 0xFF3A8A80;
                // A roof terrace round the edge, then three stepped, round-cornered tiers with shadows.
                paving(f, 0xFFD8CCB8, 0, 0, W, H, 3);
                f.frame(cream, 1.2f, 0.6f, 0.6f, W - 0.6f, H - 0.6f);
                for (float u = 3; u < W - 2; u += 3) f.rect(0xFFF4ECDC, u, 0, u + 1.2f, 1.2f);
                float[] in = {3.5f, 3.5f + H * 0.16f, 3.5f + H * 0.3f};
                int[] cols = {pink, cream, mint};
                for (int k = 0; k < 3; k++) {
                    float a = in[k], bu = W - in[k], bv = H - in[k] * (k == 0 ? 1 : 0.9f);
                    if (bu - a < 6 || bv - a < 5) break;
                    float rad = Math.min(4.5f - k, (bv - a) / 3);
                    f.roundRect(0x48000000, a + 1.2f, a + 1.6f, bu + 1.2f, bv + 1.6f, rad);
                    f.roundRect(City.darken(cols[k], 0.82f), a, a, bu, bv, rad);
                    f.roundRect(cols[k], a + 0.8f, a + 0.8f, bu - 0.8f, bv - 0.8f, rad);
                    f.roundRect(City.lighten(cols[k], 0.1f), a + 0.8f, a + 0.8f, bu - 0.8f, a + 2, rad * 0.5f);
                    // Speed lines round each tier's front.
                    for (int s = 0; s < 3; s++) f.line(teal, 0.4f, a + rad, bv - 1.4f - s * 0.9f, bu - rad, bv - 1.4f - s * 0.9f);
                    for (float u = a + rad + 2; u < bu - rad - 1; u += 3) f.line(City.darken(cols[k], 0.85f), 0.5f, u, a + 0.8f, u, a + 2.4f);
                    if (k == 0)
                        for (float u = a + 6; u < bu - 6; u += 6) {
                            f.circle(0xFF8AA8B8, u, a + 3.5f, 1.1f);
                            f.circle(0xFFC8D8E0, u - 0.3f, a + 3.2f, 0.4f);
                        }
                }
                // The central fin with its stepped crown and a gold finial, and a flagpole.
                float cu = W / 2, cv = H * 0.45f;
                f.rect(0x48000000, cu - 1.5f + 1.2f, cv - 6 + 1.6f, cu + 1.5f + 1.2f, cv + 6 + 1.6f);
                f.rect(cream, cu - 1.5f, cv - 6, cu + 1.5f, cv + 6);
                f.rect(teal, cu - 0.4f, cv - 6, cu + 0.4f, cv + 6);
                f.rect(cream, cu - 3.5f, cv - 2, cu + 3.5f, cv + 2);
                f.rect(teal, cu - 3.5f, cv - 0.3f, cu + 3.5f, cv + 0.3f);
                f.circle(0xFFD8B048, cu, cv, 1.2f);
                acUnit(f, 6, H - 6, 1.6f);
                acUnit(f, W - 6, H - 6, 1.6f);
                f.sign(city, "THE BELMAR", W / 2, H - 2.2f, 4, 0xFF3A8A80);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.MID | Variants.DOWN | Variants.OLD;
        d.minS = 10;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFFE8C4B8;
        out.add(d);
    }

    // ------------------------------------------------------------------ 9. Brutalist Estate

    static void brutalist(List<Landmarks.Def> out) {
        // Long raw-concrete slab blocks joined by walkways in the sky, over a hard landscaped deck with a play sculpture.
        D d = new D("Brutalist Estate", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                ArrayList<int[]> a = new ArrayList<int[]>();
                int m = Math.max(1, (S + 1) / 5), span = m * 5 - 3, v0 = (S - span) / 2;
                for (int i = 0; i < m; i++) a.add(B(i % 2 == 0 ? 1 : 3, v0 + i * 5, L - 4, 2, 9 + (i % 2) * 3));
                return arr(a);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                f.rect(0xFFA8A49C, 0, 0, L, S);
                for (float u = 0; u < L; u += 8) f.line(0x18000000, 0.4f, u, 0, u, S);
                for (float v = 0; v < S; v += 8) f.line(0x18000000, 0.4f, 0, v, L, v);
                // Lawns, a few trees in square concrete planters and a concrete play sculpture between the slabs.
                float pu = L * 0.5f + 10, pv = q.length > 1 ? (q[0][1] + q[0][3]) * T + 24 : S - 14;
                for (int i = 0; i + 1 < q.length; i++) {
                    float g0 = (q[i][1] + q[i][3]) * T + 6, g1 = q[i + 1][1] * T - 6;
                    f.rect(0xFF7A9A5A, 10, g0, L - 10, g1);
                    f.frame(0xFF8A8680, 1.2f, 10, g0, L - 10, g1);
                    for (float u = 22; u < L - 18; u += 34) {
                        if (i == 0 && Math.abs(u - pu) < 22) continue;
                        f.rect(0xFF8A8680, u - 5, (g0 + g1) / 2 - 5, u + 5, (g0 + g1) / 2 + 5);
                        f.tree(u, (g0 + g1) / 2, 6.5f, 0xFF4A7A3E);
                    }
                }
                if (q.length == 1) pv = Math.min(S - 12, (q[0][1] + q[0][3]) * T + 14);
                for (int k = 0; k < 3; k++) {
                    f.rect(0x40000000, pu - 6 + k * 4 + 1.5f, pv - 4 + 2, pu - 3 + k * 4 + 1.5f, pv + 4 + 2);
                    f.rect(0xFFB8B4AC, pu - 6 + k * 4, pv - 4, pu - 3 + k * 4, pv + 4);
                    f.rect(0xFFCCC8C0, pu - 6 + k * 4, pv - 4, pu - 3 + k * 4, pv - 3);
                }
                f.circle(0xFFD8B860, pu + 8, pv, 2.5f);
                // Bin stores and lamp standards along the slab feet.
                for (int[] b : q) {
                    float u0 = b[0] * T, u1 = (b[0] + b[2]) * T, v1 = (b[1] + b[3]) * T;
                    for (float u = u0 + 12; u < u1 - 6; u += 30) f.lamp(u, v1 + 3);
                    bins(f, u1 + 3, b[1] * T + 4);
                }
                // The streets in the sky: decks bridging the gaps between the slabs, casting shadows on the ground.
                for (int i = 0; i + 1 < q.length; i++) {
                    float v0 = (q[i][1] + q[i][3]) * T, v1 = q[i + 1][1] * T;
                    float lo = Math.max(q[i][0], q[i + 1][0]) * T, hi = Math.min(q[i][0] + q[i][2], q[i + 1][0] + q[i + 1][2]) * T;
                    for (int k = 1; k <= 2; k++) {
                        float u = lo + (hi - lo) * (k == 1 ? 0.25f : 0.72f);
                        f.rect(0x48000000, u - 3 + 9, v0 + 12, u + 3 + 9, v1 + 12);
                        f.rect(0xFF8E8A84, u - 3, v0, u + 3, v1);
                        f.rect(0xFFB0ACA4, u - 2.2f, v0, u + 2.2f, v1);
                        f.line(0xFF6A6660, 0.5f, u - 2.6f, v0, u - 2.6f, v1);
                        f.line(0xFF6A6660, 0.5f, u + 2.6f, v0, u + 2.6f, v1);
                        for (float v = v0 + 3; v < v1; v += 3) f.line(0x22000000, 0.3f, u - 2.2f, v, u + 2.2f, v);
                    }
                }
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float W = f.L, H = f.S;
                int con = 0xFF9A968E;
                // Board-marked concrete with balcony slabs stepping out along both faces.
                f.rect(City.darken(con, 0.75f), 0, 0, W, H);
                f.rect(con, 1.2f, 1.2f, W - 1.2f, H - 1.2f);
                for (float v = 2.6f; v < H - 1.5f; v += 2.2f) f.line(0x16000000, 0.35f, 1.2f, v, W - 1.2f, v);
                f.specks(r, (int) (W * H / 30), 0x20000000, 0x1CFFFFFF, 1.5f, 1.5f, W - 1.5f, H - 1.5f, 0.8f);
                for (float u = 2; u < W - 3; u += 8) {
                    f.rect(0xFF7E7A74, u, 0, u + 5, 1.2f);
                    f.rect(0xFF7E7A74, u + 4, H - 1.2f, u + 9, H);
                }
                // Lift and stair towers, plant rooms, a run of vent stacks.
                for (float u = W * 0.18f; u < W - 8; u += W * 0.32f) {
                    f.rect(0x50000000, u + 2, 3.5f, u + 10 + 2, H - 3.5f + 3);
                    f.rect(0xFF8A867E, u, 2.5f, u + 10, H - 3.5f);
                    f.rect(0xFFB0ACA4, u + 0.6f, 3.1f, u + 9.4f, H - 4.1f);
                    for (float v = 5; v < H - 4.5f; v += 2.4f) f.line(0x18000000, 0.3f, u + 0.6f, v, u + 9.4f, v);
                    f.rect(0xFF6A6660, u + 3, 2.5f, u + 7, 3.4f);
                }
                for (float u = W * 0.34f; u < W - 6; u += W * 0.32f) {
                    for (int k = 0; k < 3; k++) {
                        f.circle(0x40000000, u + k * 3 + 0.6f, H / 2 + 0.9f, 0.9f);
                        f.circle(0xFF7A7670, u + k * 3, H / 2, 0.9f);
                    }
                }
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.MID | Variants.DOWN | Variants.UNI;
        d.minS = 10;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFF9A968E;
        out.add(d);
    }

    // ------------------------------------------------------------------ 10. Garden Co-op

    static void gardenCoop(List<Landmarks.Def> out) {
        // Low timber homes with planted roofs round a shared garden of raised beds, a greenhouse, a pond and a long table.
        D d = new D("Garden Co-op", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                ArrayList<int[]> a = new ArrayList<int[]>();
                int n = (L - 1) / 4, u0 = (L - (n * 4 - 1)) / 2;
                for (int i = 0; i < n; i++) {
                    a.add(B(u0 + i * 4, 1, 3, 2, 1));
                    a.add(B(u0 + i * 4, S - 3, 3, 2, 1));
                }
                return arr(a);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                f.rect(0xFF6E9A4E, 0, 0, L, S);
                float g0 = 3 * T + 12, g1 = S - 3 * T - 12;
                // Each home's own deck to the garden side, with a pot or two.
                for (int[] b : q) {
                    boolean top = b[1] * T < S / 2;
                    float u0 = b[0] * T, u1 = (b[0] + b[2]) * T, dv = top ? (b[1] + b[3]) * T : b[1] * T - 7;
                    f.rect(0x30000000, u0 + 2, dv + 1.4f, u1 - 1, dv + 8.4f);
                    f.rect(0xFFB08A60, u0 + 1, dv, u1 - 2, dv + 7);
                    for (float u = u0 + 3.5f; u < u1 - 2; u += 3f) f.line(0xFF8A6A48, 0.3f, u, dv, u, dv + 7);
                    pot(f, u0 + 3, dv + 3.5f, 1.4f, 0xFFE0A030);
                }
                // A gravel loop round the shared garden.
                f.rect(0xFFC8BC9C, 6, g0 - 6, L - 6, g1 + 6);
                f.specks(r, (int) (L * (g1 - g0) / 30), 0x20000000, 0x30FFFFFF, 6, g0 - 6, L - 6, g1 + 6, 0.7f);
                f.rect(0xFF7AA858, 12, g0, L - 12, g1);
                float gh = g1 - g0, gm = (g0 + g1) / 2, split = 12 + (L - 24) * 0.55f;
                // A cross path; raised timber beds on one side, the greenhouse, pond, table and fruit trees on the other.
                f.rect(0xFFC8BC9C, 12, gm - 2.5f, L - 12, gm + 2.5f);
                f.rect(0xFFC8BC9C, split - 2.5f, g0, split + 2.5f, g1);
                Random rr = new Random(5);
                int k = 0;
                for (float u = 16; u + 9 < split - 3; u += 17, k++) {
                    for (int s = 0; s < 2; s++) {
                        float a = s == 0 ? g0 + 4 : gm + 5.5f, b = s == 0 ? gm - 5.5f : g1 - 4;
                        if (b - a < 6) continue;
                        f.rect(0x30000000, u + 0.5f, a + 0.5f, u + 10.5f, b + 1.7f);
                        f.rect(0xFF8A6440, u - 1, a - 1, u + 10, b + 1);
                        if ((k + s) % 3 == 2) bed(f, rr, u, a, u + 9, b, 0xFFE8C040, 0xFFE07030, 0xFFD04060, 0xFFF0F0E0);
                        else veg(f, rr, u, a, u + 9, b, false);
                    }
                }
                float ru0 = split + 6, ru1 = L - 16;
                if (ru1 - ru0 > 14) {
                    float hv0 = g0 + 3, hv1 = gm - 6, hu1 = Math.min(ru1, ru0 + 24);
                    f.rect(0x40000000, ru0 + 1.5f, hv0 + 2, hu1 + 1.5f, hv1 + 2);
                    f.rect(0xFFE8ECEC, ru0, hv0, hu1, hv1);
                    f.rect(0xFFA8C8C8, ru0 + 0.8f, hv0 + 0.8f, hu1 - 0.8f, (hv0 + hv1) / 2);
                    f.rect(0xFF90B4B8, ru0 + 0.8f, (hv0 + hv1) / 2, hu1 - 0.8f, hv1 - 0.8f);
                    for (float u = ru0 + 4; u < hu1; u += 4) f.line(0xFFE8ECEC, 0.5f, u, hv0, u, hv1);
                    f.line(0xFFF8FCFC, 0.9f, ru0, (hv0 + hv1) / 2, hu1, (hv0 + hv1) / 2);
                    float pv0 = gm + 6, pv1 = g1 - 4, pu1 = Math.min(ru1, ru0 + 22);
                    if (pv1 - pv0 > 8) {
                        pond(f.c, f.p, Math.min(f.x(ru0 + 2, pv0), f.x(pu1, pv1)), Math.min(f.y(ru0 + 2, pv0), f.y(pu1, pv1)),
                                Math.max(f.x(ru0 + 2, pv0), f.x(pu1, pv1)), Math.max(f.y(ru0 + 2, pv0), f.y(pu1, pv1)));
                        for (int j = 0; j < 5; j++) f.line(0xFF5A8A40, 0.6f, ru0 + 3 + j, pv1 - 1, ru0 + 2.5f + j * 1.3f, pv1 - 5);
                    }
                    // An orchard of little fruit trees filling the rest.
                    int j = 0;
                    for (float u = Math.max(hu1, pu1) + 9; u < L - 15; u += 14, j++)
                        for (int s = 0; s < 2; s++) {
                            float tv = s == 0 ? (hv0 + hv1) / 2 : (pv0 + pv1) / 2;
                            f.tree(u, tv, Math.min(5.5f, (hv1 - hv0) / 2), (j + s) % 2 == 0 ? 0xFF5A9040 : 0xFF4E8A3A);
                            for (int t = 0; t < 4; t++) f.circle((j + s) % 2 == 0 ? 0xFFD04A30 : 0xFFE0C040, u + (t - 1.5f) * 2, tv - 1 + (t % 2) * 2.2f, 0.7f);
                        }
                }
                // The long communal table on the cross path, under a festoon of lights.
                float tu0 = split + 6, tu1 = Math.min(L - 14, tu0 + 30);
                if (tu1 - tu0 > 12) {
                    f.rect(0x38000000, tu0 + 1, gm - 2 + 1.2f, tu1 + 1, gm + 2 + 1.2f);
                    f.rect(0xFF9A7650, tu0, gm - 2, tu1, gm + 2);
                    for (float s = tu0 + 2; s < tu1 - 1; s += 4) {
                        f.rect(0xFF6A5038, s, gm - 4, s + 2, gm - 2.8f);
                        f.rect(0xFF6A5038, s, gm + 2.8f, s + 2, gm + 4);
                    }
                    for (float s = tu0; s < tu1; s += 2.5f) f.circle(0xFFF8E8A0, s, gm + (float) Math.sin(s * 0.5f) * 1.2f, 0.45f);
                }
                f.tree(9, g0 - 2, 6, 0xFF4E8A3A);
                f.tree(L - 9, g1 + 2, 6, 0xFF4E8A3A);
                bench(f.c, f.p, f.x(split, g1 + 3), f.y(split, g1 + 3), !f.t);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float W = f.L, H = f.S;
                // A sedum roof in mottled greens and reds, a timber fascia, solar panels on every other home.
                f.rect(0xFF8A6440, 0, 0, W, H);
                f.rect(0xFF6E8A48, 1, 1, W - 1, H - 1);
                for (int k = 0; k < (int) (W * H / 60); k++) {
                    float u = 1.5f + r.nextFloat() * (W - 3), v = 1.5f + r.nextFloat() * (H - 3);
                    int c = r.nextInt(5);
                    f.circle(c == 0 ? 0xFF9A6A4A : c == 1 ? 0xFF8AA858 : c == 2 ? 0xFFA8B060 : c == 3 ? 0xFF5A7A3E : 0xFFC8A858, u, v, 0.7f + r.nextFloat() * 0.6f);
                }
                if ((part / 2) % 2 == 0) solar(f, 3, 2.5f, W - 3, H * 0.5f);
                skylight(f, W * 0.6f, H - 7, W * 0.6f + 5, H - 3.5f);
                f.circle(0x40000000, W - 4 + 0.6f, H / 2 + 0.8f, 1.2f);
                f.circle(0xFF8A8E92, W - 4, H / 2, 1.2f);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.SUB | Variants.MID | Variants.UNI;
        d.minS = 12;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFF6E8A48;
        out.add(d);
    }

    // ------------------------------------------------------------------ 11. Mobile Home Park

    static final int[] TRAILER = {0xFF5AA0A0, 0xFFC08A50, 0xFF7A8AB0, 0xFFB86A5A, 0xFF8AA060, 0xFFD0B060};

    static void mobileHomes(List<Landmarks.Def> out) {
        // Rows of mobile homes along a lane, each on its own little lot with an awning, a deck, chairs and flamingos.
        D d = new D("Mobile Home Park", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                ArrayList<int[]> a = new ArrayList<int[]>();
                boolean two = S >= 14;
                int n = (L - 1) / 4, u0 = (L - (n * 4 - 2)) / 2;
                int lv = two ? Math.min(5, (S - 5) / 2) : Math.min(5, S - 5);
                for (int i = 0; i < n; i++) {
                    a.add(B(u0 + i * 4, 1, 2, lv, 1));
                    if (two) a.add(B(u0 + i * 4 + (i % 2), S - 1 - lv, 2, lv, 1));
                }
                return arr(a);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                boolean two = Math.round(S / T) >= 14;
                f.rect(0xFF7A9A58, 0, 0, L, S);
                // The lane: worn asphalt with a centre crack line and gravel verges.
                float lv0 = (q[0][1] + q[0][3]) * T + 10, lv1 = two ? q[1][1] * T - 10 : S - 6;
                if (!two) lv0 = Math.max(lv0, lv1 - 24);
                else {
                    float lc = (lv0 + lv1) / 2;
                    lv0 = Math.max(lv0, lc - 11);
                    lv1 = Math.min(lv1, lc + 11);
                }
                f.rect(0xFFB8AE98, 0, lv0 - 3, L, lv1 + 3);
                f.rect(0xFF6E6C68, 0, lv0, L, lv1);
                f.specks(r, (int) (L * (lv1 - lv0) / 25), 0x20000000, 0x18FFFFFF, 0, lv0, L, lv1, 0.8f);
                for (float u = 4; u < L; u += 9) f.line(0xFFD8C890, 0.5f, u, (lv0 + lv1) / 2, u + 4, (lv0 + lv1) / 2);
                Random rr = new Random(3);
                for (int i = 0; i < q.length; i++) {
                    int[] b = q[i];
                    boolean top = b[1] * T < S / 2;
                    float u0 = b[0] * T, u1 = (b[0] + b[2]) * T, v0 = b[1] * T, v1 = (b[1] + b[3]) * T;
                    // A carport-style awning down the trailer's side, then the lot's bits and pieces.
                    float au0 = u1, au1 = u1 + T * 0.85f;
                    f.rect(0x40000000, au0 + 2, v0 + 6, au1 + 2, v1 - 2);
                    f.rect(0xFFD8D8D0, au0, v0 + 4, au1, v1 - 4);
                    for (float v = v0 + 6; v < v1 - 4; v += 2.4f) f.line(0xFFB0B0A8, 0.4f, au0, v, au1, v);
                    f.rect(TRAILER[i % TRAILER.length], au1 - 0.8f, v0 + 4, au1, v1 - 4);
                    float dv = top ? v1 : v0 - 6;
                    f.rect(0xFF9A7A58, u0 + 3, dv, u0 + 12, dv + 6);
                    for (float u = u0 + 4.5f; u < u0 + 12; u += 1.5f) f.line(0xFF7A5A3A, 0.3f, u, dv, u, dv + 6);
                    float yv = top ? Math.min(v1 + 8, lv0 - 6) : Math.max(v0 - 8, lv1 + 6);
                    int kind = rr.nextInt(4);
                    bed(f, rr, u0 - 1, top ? v1 + 1 : v0 - 3, u0 + 2.5f, top ? v1 + 3 : v0 - 1, 0xFFE0C040, 0xFFE06080);
                    if (kind == 0) {
                        for (int k = 0; k < 2; k++) {
                            float fu = u0 + 16 + k * 3.5f, fv2 = yv + k;
                            f.oval(0x30000000, fu - 1, fv2, fu + 2.4f, fv2 + 2);
                            f.oval(0xFFE87AA0, fu - 1.4f, fv2 - 1, fu + 1.4f, fv2 + 1);
                            f.circle(0xFFE87AA0, fu + 1.6f, fv2 - 1.4f, 0.7f);
                            f.line(0xFFE87AA0, 0.5f, fu + 0.8f, fv2 - 0.4f, fu + 1.6f, fv2 - 1.4f);
                        }
                    } else if (kind == 1) {
                        f.circle(0x40000000, u0 + 17.5f, yv + 0.7f, 1.6f);
                        f.circle(0xFF2A2A2A, u0 + 17, yv, 1.6f);
                        f.circle(0xFF5A5A5A, u0 + 16.6f, yv - 0.4f, 0.6f);
                    } else if (kind == 2) {
                        for (int k = 0; k < 2; k++) {
                            f.rect(0x30000000, u0 + 15 + k * 3.5f + 0.4f, yv - 1 + 0.6f, u0 + 17.4f + k * 3.5f + 0.4f, yv + 1.4f + 0.6f);
                            f.rect(k == 0 ? 0xFFE8E8E4 : 0xFF4A9A6A, u0 + 15 + k * 3.5f, yv - 1, u0 + 17.4f + k * 3.5f, yv + 1.4f);
                        }
                    } else {
                        f.rect(0x30000000, u0 + 14 + 0.8f, yv - 2.5f + 1, u0 + 20 + 0.8f, yv + 2.5f + 1);
                        f.rect(0xFF9A7A58, u0 + 14, yv - 1.5f, u0 + 20, yv + 1.5f);
                        f.rect(0xFF7A5A3A, u0 + 14, yv - 2.8f, u0 + 20, yv - 2);
                        f.rect(0xFF7A5A3A, u0 + 14, yv + 2, u0 + 20, yv + 2.8f);
                    }
                    if (bb(top, v0, v1, lv0, lv1, T)) f.tree(u0 + 8, top ? (v1 + lv0) / 2 + 4 : (lv1 + v0) / 2 - 4, 4.5f, 0xFF4E7A3A);
                    // A white propane bottle at the back and a satellite dish on a pole.
                    float bv = top ? v0 + 5 : v1 - 5;
                    f.roundRect(0x40000000, u0 - 4 + 0.6f, bv - 1.4f + 0.8f, u0 - 1 + 0.6f, bv + 1.4f + 0.8f, 1.4f);
                    f.roundRect(0xFFE8E8E4, u0 - 4, bv - 1.4f, u0 - 1, bv + 1.4f, 1.4f);
                }
                // Lot boundaries (chain-link) and a shade tree here and there.
                for (int i = 0; i < q.length; i++) {
                    int[] b = q[i];
                    boolean top = b[1] * T < S / 2;
                    float ub = b[0] * T - T * 0.5f - 1, va = top ? 0 : (b[1] - 1) * T + 4, vb = top ? (b[1] + b[3] + 0.5f) * T : S;
                    f.line(0x70A0A4A8, 0.5f, ub, va, ub, vb);
                }
                for (float u = 14; u < L - 6; u += 44) {
                    if (clear(q, u + 26, 6, 6)) f.tree(u + 26, 6, 7, 0xFF4E7A3A);
                }
                f.rect(0xFFE8E4D8, L - 20, lv1 + 2, L - 4, lv1 + 6);
                f.rect(0xFF3A6A9A, L - 19, lv1 + 2.6f, L - 5, lv1 + 5.4f);
            }

            boolean bb(boolean top, float v0, float v1, float lv0, float lv1, float T) {
                return top ? lv0 - v1 > 30 : v0 - lv1 > 30;
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                // A long tin roof with rounded ends, ribbed across, the coloured siding showing on the edges.
                float W = f.L, H = f.S;
                int col = TRAILER[part % TRAILER.length];
                f.roundRect(col, 0, 0, W, H, 2.5f);
                f.roundRect(0xFFD8D8D2, 0.9f, 0.9f, W - 0.9f, H - 0.9f, 2);
                f.rect(0xFFE6E6E0, 0.9f, 0.9f, W - 0.9f, H * 0.45f);
                for (float u = 3; u < W - 2; u += 1.6f) f.line(0x20000000, 0.35f, u, 1, u, H - 1);
                f.line(0x30000000, 0.5f, 1, H / 2, W - 1, H / 2);
                acUnit(f, W * 0.38f, H / 2, 2.2f);
                f.circle(0x40000000, W * 0.75f + 0.6f, H / 2 + 0.8f, 1);
                f.circle(0xFF8A8E92, W * 0.75f, H / 2, 1);
                if (part % 2 == 0) {
                    f.circle(0x40000000, W * 0.15f + 1, H * 0.3f + 1.4f, 2);
                    f.circle(0xFFE8E8E4, W * 0.15f, H * 0.3f, 2);
                    f.circle(0xFFB8B8B4, W * 0.15f + 0.5f, H * 0.3f + 0.5f, 1);
                }
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.SUB | Variants.IND;
        d.minS = 10;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFFD8D8D2;
        out.add(d);
    }

    // ------------------------------------------------------------------ 12. Gated Estate

    static void gatedEstate(List<Landmarks.Def> out) {
        // Big houses on sweeping lawns behind a stone wall, reached through a gatehouse and a private drive with a fountain.
        D d = new D("Gated Estate", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                ArrayList<int[]> a = new ArrayList<int[]>();
                int n = Math.max(1, (L - 2) / 6), cw = (L - 2) / n, lv = Math.min(S >= 13 ? 5 : 4, cw - 2);
                for (int i = 0; i < n; i++) a.add(B(2 + i * cw, S >= 13 ? 2 : 1, cw - 1, lv, 2));
                a.add(B(L / 2 + 1, S - 3, 2, 2, 1));
                return arr(a);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                lawn(f, 0xFF5E9A48, 0, 0, L, S, 10);
                int[] gh = q[q.length - 1];
                float gx = (gh[0] - 1) * T, gv0 = gh[1] * T, hv1 = (q[0][1] + q[0][3]) * T;
                float dv = (hv1 + gv0) / 2;
                // The drive: in through the gate, a fountain roundel, then a spur to each house's front.
                int pave = 0xFFD0C8B8;
                f.rect(pave, gx - 5, dv, gx + 5, S);
                f.rect(pave, q[0][0] * T + 8, dv - 4, (q[q.length - 2][0] + q[q.length - 2][2]) * T - 8, dv + 4);
                for (int i = 0; i + 1 < q.length; i++) {
                    float hc = (q[i][0] + q[i][2] / 2f) * T;
                    f.rect(pave, hc - 5, hv1, hc + 5, dv);
                    f.rect(pave, hc - 12, hv1, hc + 12, hv1 + 6);
                }
                float fr = Math.min(9, (gv0 - hv1) / 2 - 3);
                if (fr > 4) {
                    f.circle(pave, gx, dv, fr + 5);
                    f.circle(0xFF5E9A48, gx, dv, fr + 1);
                    f.circle(0xFFB8B0A0, gx, dv, fr - 1);
                    f.circle(0xFF5AA0C8, gx, dv, fr - 2.5f);
                    f.circle(0xFFC8C0B0, gx, dv, 1.6f);
                    for (int k = 0; k < 8; k++) {
                        double a = k * Math.PI / 4;
                        f.circle(0x90FFFFFF, gx + (float) Math.cos(a) * fr * 0.45f, dv + (float) Math.sin(a) * fr * 0.45f, 0.6f);
                    }
                }
                // Clipped hedges between the plots, topiary at the doors, a row of trees behind.
                for (int i = 0; i + 2 < q.length; i++) {
                    float hu = (q[i][0] + q[i][2]) * T + T * 0.5f;
                    for (float v = 4; v < dv - 8; v += 3.2f) f.bush(hu, v, 2.2f);
                }
                for (int i = 0; i + 1 < q.length; i++) {
                    float hc = (q[i][0] + q[i][2] / 2f) * T;
                    f.circle(0x40000000, hc - 14 + 1, hv1 + 3 + 1.4f, 2.4f);
                    f.circle(0xFF3E6A34, hc - 14, hv1 + 3, 2.4f);
                    f.circle(0x40000000, hc + 14 + 1, hv1 + 3 + 1.4f, 2.4f);
                    f.circle(0xFF3E6A34, hc + 14, hv1 + 3, 2.4f);
                    if (gv0 - hv1 > 40) {
                        f.tree(q[i][0] * T + 6, dv + 16, 6, 0xFF4E8A3A);
                        f.tree((q[i][0] + q[i][2]) * T - 4, dv + 18, 5, 0xFF7A3A34);
                    }
                }
                // The estate wall: stone with piers, broken only at the gate, where a barrier arm stands.
                int st = 0xFFB0A68E;
                f.rect(0x40000000, 0, 2.2f, L, 3.6f);
                f.rect(st, 0, 0, L, 2.4f);
                f.rect(st, 0, 0, 2.4f, S);
                f.rect(st, L - 2.4f, 0, L, S);
                f.rect(st, 0, S - 2.4f, gx - 10, S);
                f.rect(st, gx + 10, S - 2.4f, L, S);
                for (float u = 0; u < L; u += 16) {
                    if (Math.abs(u - gx) < 10) continue;
                    f.rect(0xFFC8BEA6, u, 0, u + 3.4f, 3.4f);
                    if (Math.abs(u - gx) > 12) f.rect(0xFFC8BEA6, u, S - 3.4f, u + 3.4f, S);
                }
                for (float v = 0; v < S; v += 16) {
                    f.rect(0xFFC8BEA6, 0, v, 3.4f, v + 3.4f);
                    f.rect(0xFFC8BEA6, L - 3.4f, v, L, v + 3.4f);
                }
                for (int s = -1; s <= 1; s += 2) {
                    f.tree(s < 0 ? 14 : L - 14, S - 16, 7, 0xFF4E8A3A);
                    for (float v = dv + 12; v < S - 10; v += 5) shrub(f, gx + s * 9, v, 1.8f, 0xFFF0F0F0);
                }
                f.line(0xFFD03A2A, 1, gx + 5, S - 6, gx - 4, S - 6);
                f.line(0xFFF0F0F0, 1, gx + 2, S - 6, gx - 1, S - 6);
                f.lamp(gx - 9, S - 4);
                f.lamp(gx + 9, S - 4);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float W = f.L, H = f.S;
                if (H > 2.5f * TT) {
                    // A grand house: a slate main block with two forward wings, a terrace roof between, chimneys and skylights.
                    int[] cols = {0xFF5A606A, 0xFF8A5A44, 0xFF6A645A};
                    int col = cols[part % 3];
                    float wing = Math.min(W * 0.3f, 22);
                    hip(f, col, wing - 2, 0, W - wing + 2, H * 0.72f, true);
                    f.rect(0xFFE0D8C8, wing, H * 0.72f, W - wing, H);
                    f.frame(0xFFB8B0A0, 0.8f, wing, H * 0.72f, W - wing, H);
                    f.rect(0x40000000, wing, H * 0.72f, W - wing, H * 0.72f + 1.4f);
                    for (float u = wing + 3; u < W - wing - 2; u += 6) pot(f, u, H - 2.2f, 1.2f, 0);
                    hip(f, col, 0, 0, wing, H, true);
                    hip(f, col, W - wing, 0, W, H, true);
                    stack(f, wing * 0.5f - 2, H * 0.3f, 4, 0xFFC8BEA6);
                    stack(f, W - wing * 0.5f - 2, H * 0.3f, 4, 0xFFC8BEA6);
                    skylight(f, W / 2 - 4, H * 0.18f, W / 2 + 4, H * 0.18f + 4);
                } else {
                    // The gatehouse: a pyramid roof with a lantern.
                    hip(f, 0xFF5A606A, 0, 0, W, H, true);
                    f.circle(0xFFD8B048, W / 2, H / 2, 1.2f);
                }
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.SUB;
        d.minS = 10;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFF5A606A;
        out.add(d);
    }

    // ------------------------------------------------------------------ 13. Tudor Cottages

    /** A thatched roof with its ridge along u: rounded eaves, straw texture, a patterned ridge and eyebrow dormers. */
    static void thatch(F f, Random r, float W, float H) {
        int st = 0xFFB89A5E;
        f.roundRect(City.darken(st, 0.7f), 0, 0, W, H, H * 0.3f);
        f.roundRect(City.lighten(st, 0.06f), 0.6f, 0.6f, W - 0.6f, H / 2 + 0.5f, H * 0.28f);
        f.roundRect(City.darken(st, 0.8f), 0.6f, H / 2 - 0.5f, W - 0.6f, H - 0.6f, H * 0.28f);
        f.rect(City.lighten(st, 0.06f), 0.6f + H * 0.28f, H / 2 - 1, W - 0.6f - H * 0.28f, H / 2);
        for (int k = 0; k < (int) (W * H / 30); k++) {
            float u = 1.5f + r.nextFloat() * (W - 3), v = 1.5f + r.nextFloat() * (H - 3);
            float dir = v < H / 2 ? -1.2f : 1.2f;
            f.line(r.nextBoolean() ? 0x22000000 : 0x24FFFFFF, 0.3f, u, v, u, v + dir);
        }
        for (int k = 1; k <= 4; k++) {
            float dv = k * (H / 2 - 3) / 5, inset = H * 0.28f * (1 - k / 5f) + 1;
            f.line(0x22000000, 0.6f, inset, dv + 1, W - inset, dv + 1);
            f.line(0x26000000, 0.6f, inset, H - dv - 1, W - inset, H - dv - 1);
        }
        f.line(0x40FFFFFF, 0.6f, H * 0.28f, 1.2f, W - H * 0.28f, 1.2f);
        // The ridge: a thick band with a scalloped edge on both sides.
        f.rect(0xFF8A6E40, H * 0.25f, H / 2 - 2, W - H * 0.25f, H / 2 + 2);
        for (float u = H * 0.25f + 1.2f; u < W - H * 0.25f - 1; u += 2.4f) {
            f.circle(0xFF8A6E40, u, H / 2 - 2, 1.2f);
            f.circle(0xFF7A5E34, u, H / 2 + 2, 1.2f);
        }
        f.line(0xFFA88A54, 0.6f, H * 0.25f, H / 2 - 1, W - H * 0.25f, H / 2 - 1);
        // Eyebrow dormers rising out of the front slope.
        for (float u = W * 0.3f; u < W - 4; u += W * 0.4f) {
            f.oval(0x40000000, u - 3.5f + 0.6f, H - 6 + 0.8f, u + 3.5f + 0.6f, H - 2 + 0.8f);
            f.oval(City.lighten(st, 0.02f), u - 3.5f, H - 6.5f, u + 3.5f, H - 2.5f);
            f.oval(0xFF3A3A3A, u - 1.8f, H - 3.6f, u + 1.8f, H - 2.6f);
        }
    }

    static void tudor(List<Landmarks.Def> out) {
        // Half-timbered cottages, some thatched and some tiled, in flower-crammed gardens along a winding lane.
        D d = new D("Tudor Cottages", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                ArrayList<int[]> a = new ArrayList<int[]>();
                for (int u = 1; u + 4 <= L - 1; u += 5) a.add(B(u, 1, 4, 3, 2));
                for (int u = 3; u + 4 <= L - 1; u += 5) a.add(B(u, S - 4, 4, 3, 2));
                return arr(a);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                f.rect(0xFF6E9A4E, 0, 0, L, S);
                // A winding lane between the two rows.
                float m = S / 2;
                Path lane = new Path();
                f.mv(lane, 0, m - 6);
                for (float u = 0; u < L; u += 20) f.qd(lane, u + 10, m - 6 + ((int) (u / 20) % 2 == 0 ? -4 : 4), u + 20, m - 6);
                f.ln(lane, L, m + 6);
                for (float u = L; u > 0; u -= 20) f.qd(lane, u - 10, m + 6 + ((int) (u / 20) % 2 == 0 ? 4 : -4), u - 20, m + 6);
                lane.close();
                f.fill(lane, 0xFFB8A888);
                f.specks(r, (int) (L / 2), 0x22000000, 0x24FFFFFF, 0, m - 6, L, m + 6, 0.8f);
                // Cottage gardens: dry-stone walls, a path to the door, beds of hollyhocks and roses, a bench.
                Random rr = new Random(9);
                for (int i = 0; i < q.length; i++) {
                    int[] b = q[i];
                    boolean top = b[1] * T < m;
                    float u0 = b[0] * T, u1 = (b[0] + b[2]) * T, v0 = b[1] * T, v1 = (b[1] + b[3]) * T;
                    float ga = top ? v1 : m + 7, gb = top ? m - 7 : v0;
                    float du = (u0 + u1) / 2;
                    if (gb - ga > 4) {
                        float ea = top ? Math.max(ga + 1, gb - 6) : ga + 1, eb = top ? gb - 1 : Math.min(gb - 1, ga + 6);
                        bed(f, rr, u0 - 4, ea, du - 3, eb, 0xFFE070A0, 0xFFF0E0F0, 0xFFC050A0, 0xFFE8D060, 0xFF9070C0);
                        bed(f, rr, du + 3, ea, u1 + 4, eb, 0xFFE07070, 0xFFF0E8E0, 0xFFE8A0B0, 0xFF8090D0);
                        if (gb - ga > 18) {
                            shrub(f, du - 10, (ga + gb) / 2, 3, 0xFFE8A0B0);
                            shrub(f, du + 11, (ga + gb) / 2 + 2, 2.6f, 0xFFF0F0F0);
                            if (i % 2 == 0) f.bench(du + 12, top ? ga + 4 : gb - 4, true);
                        }
                        f.rect(0xFFC8B898, du - 2.5f, ga, du + 2.5f, gb);
                        f.line(0xFF9A9080, 1.6f, u0 - 5, top ? gb : ga, du - 3, top ? gb : ga);
                        f.line(0xFF9A9080, 1.6f, du + 3, top ? gb : ga, u1 + 5, top ? gb : ga);
                    }
                    // Back gardens: a vegetable patch, an apple tree, a beehive or a well.
                    float ba = top ? 0 : v1, bb = top ? v0 : S;
                    if (bb - ba > 8) veg(f, rr, u0 + 2, ba + 2, u0 + 20, bb - 2, true);
                    if (i % 3 == 0) {
                        float wu = u1 + 8, wv = top ? v0 + 10 : v1 - 10;
                        if (clear(q, wu, wv, 5) && wu < L - 5) {
                            f.circle(0x40000000, wu + 1, wv + 1.4f, 4);
                            f.circle(0xFF9A9080, wu, wv, 4);
                            f.circle(0xFF2A3A44, wu, wv, 2.4f);
                            f.rect(0x40000000, wu - 4.5f + 1, wv - 1.5f + 1.5f, wu + 4.5f + 1, wv + 1.5f + 1.5f);
                            gable(f, 0xFF8A4A36, wu - 4.5f, wv - 1.5f, wu + 4.5f, wv + 1.5f, false);
                        }
                    } else if (i % 3 == 1) {
                        float hu = u1 + 7, hv = top ? v0 + 8 : v1 - 8;
                        if (clear(q, hu, hv, 4) && hu < L - 4)
                            for (int k = 0; k < 2; k++) {
                                f.rect(0x40000000, hu + 0.8f, hv - 2 + k * 6 + 1, hu + 4.8f, hv + 2 + k * 6 + 1);
                                f.rect(0xFFE8E4D8, hu, hv - 2 + k * 6, hu + 4, hv + 2 + k * 6);
                                f.line(0x40000000, 0.4f, hu, hv + k * 6, hu + 4, hv + k * 6);
                            }
                    }
                }
                for (float u = 10; u < L - 6; u += 34) {
                    float v = u % 68 < 34 ? 6 : S - 6;
                    if (clear(q, u, v, 7)) f.tree(u, v, 7, 0xFF4E8A3A);
                }
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float W = f.L, H = f.S;
                if (part % 2 == 0) thatch(f, r, W, H);
                else {
                    // Old clay tiles with moss, and a half-timbered gable wing jettied out at one end.
                    int col = 0xFF9A5A40;
                    gable(f, col, 0, 0, W, H, true);
                    for (int k = 0; k < 10; k++) {
                        float u = 2 + r.nextFloat() * (W - 4), v = 2 + r.nextFloat() * (H - 4);
                        f.circle(0x605A7A3A, u, v, 0.8f + r.nextFloat());
                    }
                    float wu = W - Math.min(14, W * 0.3f);
                    f.rect(0x40000000, wu - 1.4f, 0, wu, H);
                    gableV(f, City.darken(col, 0.95f), wu, 0, W, H, true);
                    f.rect(0xFFF0E8D8, wu, H - 2.2f, W, H);
                    for (float u = wu; u <= W; u += 2.6f) f.line(0xFF2A2420, 0.6f, u, H - 2.2f, u, H);
                    f.line(0xFF2A2420, 0.6f, wu, H - 2.2f, W, H - 2.2f);
                    f.line(0xFF2A2420, 0.5f, wu, H, wu + 2.6f, H - 2.2f);
                    f.line(0xFF2A2420, 0.5f, W, H, W - 2.6f, H - 2.2f);
                }
                chimney(f, part % 2 == 0 ? W * 0.12f : W * 0.15f, H / 2 - 1.6f, 2);
                if (part % 2 == 0) {
                    f.rect(0xFFF0E8D8, 0.6f + H * 0.3f, H - 1.6f, W - 0.6f - H * 0.3f, H);
                    for (float u = 0.6f + H * 0.3f; u <= W - H * 0.3f; u += 2.6f) f.line(0xFF2A2420, 0.6f, u, H - 1.6f, u, H);
                }
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.OLD | Variants.SUB | Variants.PARK;
        d.minS = 10;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFFB89A5E;
        out.add(d);
    }

    // ------------------------------------------------------------------ 14. Townhouse Mews

    static void mews(List<Landmarks.Def> out) {
        // A cobbled mews of converted stables and coach houses, painted doors and potted bays, an arch at each end.
        D d = new D("Townhouse Mews", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                ArrayList<int[]> a = new ArrayList<int[]>();
                int dep = S >= 14 ? 5 : 3;
                for (int row = 0; row < 2; row++) {
                    int v = row == 0 ? 1 : S - 1 - dep;
                    for (int u = 1; u + 2 <= L - 1; ) {
                        int w = Math.min(6, L - 1 - u);
                        w -= w % 2;
                        if (w < 4) break;
                        int d = w > dep ? dep : w - 1;
                        a.add(B(u, row == 0 ? v + dep - d : v, w, d, 2));
                        u += w + 1;
                    }
                }
                return arr(a);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                f.rect(0xFF6E9450, 0, 0, L, S);
                int dep = Math.round(S / T) >= 14 ? 5 : 3;
                float c0 = (dep + 1) * T, c1 = S - (dep + 1) * T;
                // Small back gardens behind each row, a cobbled lane between, with a gutter of flagstones down the middle.
                for (int[] b : q) {
                    boolean top = b[1] * T < S / 2;
                    float u0 = b[0] * T, u1 = (b[0] + b[2]) * T;
                    for (float u = u0; u <= u1 + 0.1f; u += 2 * T) f.line(0xFF7A6A58, 0.8f, u, top ? 0 : S - T, u, top ? T : S);
                    f.rect(0xFFB0A898, u1, top ? 0 : c1, u1 + T, top ? c0 : S);
                }
                cobbles(f, r, 0xFF8A8478, 0, c0, L, c1);
                f.rect(0xFFB0A898, 0, (c0 + c1) / 2 - 2, L, (c0 + c1) / 2 + 2);
                // Painted doors, window boxes and bay trees in pots along both fronts.
                int[] doors = {0xFF2A4A6A, 0xFF7A2A2A, 0xFF2A5A3A, 0xFFE0C040, 0xFF3A3A3A, 0xFF5A8AA0};
                int k = 0;
                for (int[] b : q) {
                    boolean top = b[1] * T < S / 2;
                    float fv = top ? (b[1] + b[3]) * T : b[1] * T;
                    float sg = top ? 1 : -1;
                    for (int h = 0; h < b[2] / 2; h++, k++) {
                        float a = (b[0] + h * 2) * T;
                        float du = a + (k % 2 == 0 ? 6 : 20);
                        f.rect(doors[k % doors.length], du - 4.5f, Math.min(fv, fv + sg * 1.6f), du + 4.5f, Math.max(fv, fv + sg * 1.6f));
                        f.line(0xFFE8E4DA, 0.4f, du, fv, du, fv + sg * 1.6f);
                        float wu = k % 2 == 0 ? a + 20 : a + 6;
                        f.rect(0xFF6A4A30, wu - 4, Math.min(fv, fv + sg * 1.4f), wu + 4, Math.max(fv, fv + sg * 1.4f));
                        for (float u = wu - 3.4f; u < wu + 4; u += 1.4f) f.circle(k % 3 == 0 ? 0xFFE04060 : 0xFFE8E0F0, u, fv + sg * 0.8f, 0.6f);
                        if (k % 2 == 1) pot(f, a + 1.8f, fv + sg * 3, 1.8f, 0);
                        if (k % 5 == 3) {
                            f.line(0xFF2A2A2A, 0.5f, a + 11, fv + sg * 3, a + 17, fv + sg * 3);
                            f.circle(0xFF2A2A2A, a + 11, fv + sg * 3, 1.3f);
                            f.circle(0xFF2A2A2A, a + 17, fv + sg * 3, 1.3f);
                        }
                    }
                }
                // Brick archways spanning the lane at each end, their shadows falling on the setts.
                for (int e = 0; e < 2; e++) {
                    float u0 = e == 0 ? 0 : L - 7, u1 = u0 + 7;
                    f.rect(0x48000000, u0 + 2, c0 + 3, u1 + 2, c1 + 3);
                    f.rect(0xFF8A4A36, u0, c0 - 2, u1, c1 + 2);
                    for (float v = c0; v < c1; v += 2) f.line(0x30000000, 0.35f, u0, v, u1, v);
                    f.rect(0xFFC8BEA6, u0, (c0 + c1) / 2 - 2, u1, (c0 + c1) / 2 + 2);
                    f.rect(0xFFA05A42, u0, c0 - 2, u1, c0 - 1);
                }
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                // Slate pitched roofs house by house, dormers to the lane, stacks on the party walls, a roof terrace or two.
                float W = f.L, H = f.S;
                int n = Math.max(1, Math.round(W / (2 * TT)));
                float hw = W / n;
                for (int k = 0; k < n; k++) {
                    float a = k * hw, e = a + hw;
                    int col = (k + part) % 3 == 1 ? 0xFF8A5040 : 0xFF54585E;
                    if ((k + part * 2) % 5 == 4) {
                        f.rect(0xFF54585E, a, 0, e, H);
                        f.rect(0xFFA07A54, a + 1.4f, 1.4f, e - 1.4f, H - 1.4f);
                        for (float u = a + 2.6f; u < e - 1.4f; u += 1.4f) f.line(0xFF7A5A3A, 0.3f, u, 1.4f, u, H - 1.4f);
                        pot(f, a + 4, 4, 2, 0xFFE070A0);
                        pot(f, e - 4, 4, 2, 0);
                        parasol(f, (a + e) / 2, H * 0.6f, 4, 0xFFE8E4D8, 0xFF3A6A8A);
                    } else {
                        gable(f, col, a, 0, e, H, true);
                        dormer(f, col, a + hw * 0.32f, H - 1, H / 2 + 1, 6.5f);
                        dormer(f, col, a + hw * 0.68f, 1, H / 2 - 1, 6.5f);
                    }
                    if (k > 0) chimney(f, a - 2.5f, H / 2 - 1.6f, 2);
                }
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.OLD | Variants.MID | Variants.DOWN;
        d.minS = 10;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFF54585E;
        out.add(d);
    }

    // ------------------------------------------------------------------ 15. Tiny House Village

    static final int[] TINY = {0xFFB8483A, 0xFF3A8A8A, 0xFFD8A838, 0xFF6A8A4A, 0xFF4A6A9A, 0xFF8A5A8A, 0xFFE07A40};

    static void tinyHouses(List<Landmarks.Def> out) {
        // Two rows of tiny houses, every one different, round a shared green with a fire circle, string lights and beds.
        D d = new D("Tiny House Village", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                ArrayList<int[]> a = new ArrayList<int[]>();
                int n = (L - 1) / 3, u0 = (L - (n * 3 - 1)) / 2;
                for (int i = 0; i < n; i++) {
                    a.add(B(u0 + i * 3, 1 + (i % 2), 2, 3, 1));
                    if (S >= 11) a.add(B(u0 + i * 3, S - 4 - ((i + 1) % 2), 2, 3, 1));
                }
                return arr(a);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                f.rect(0xFF769A52, 0, 0, L, S);
                f.specks(r, (int) (L * S / 40), 0x18000000, 0x18FFFFFF, 0, 0, L, S, 0.8f);
                float m = S / 2;
                // A gravel loop through the green; decks and porches off every house.
                f.rect(0xFFC8BC9C, 4, m - 4, L - 4, m + 4);
                f.specks(r, (int) (L / 3), 0x20000000, 0x30FFFFFF, 4, m - 4, L - 4, m + 4, 0.7f);
                for (int i = 0; i < q.length; i++) {
                    int[] b = q[i];
                    boolean top = b[1] * T < m;
                    float u0 = b[0] * T, u1 = (b[0] + b[2]) * T, fv = top ? (b[1] + b[3]) * T : b[1] * T;
                    float dv0 = top ? fv : fv - 6, dv1 = top ? fv + 6 : fv;
                    f.rect(0x30000000, u0 + 1, dv0 + 1.2f, u1 + 1, dv1 + 1.2f);
                    f.rect(i % 2 == 0 ? 0xFFB08A60 : 0xFF9A7A58, u0, dv0, u1, dv1);
                    for (float u = u0 + 1.5f; u < u1; u += 1.5f) f.line(0x30000000, 0.3f, u, dv0, u, dv1);
                    f.line(0xFFC8BC9C, 3, (u0 + u1) / 2, top ? dv1 : dv0, (u0 + u1) / 2, m);
                    pot(f, u1 - 3, (dv0 + dv1) / 2, 1.3f, TINY[(i + 3) % TINY.length]);
                    // Rain barrels and little raised beds round the backs.
                    float bv = top ? b[1] * T - 3 : (b[1] + b[3]) * T + 3;
                    if (bv > 3 && bv < S - 3) {
                        f.circle(0x40000000, u1 + 3.6f, bv + 0.8f, 2);
                        f.circle(0xFF3A5A7A, u1 + 3, bv, 2);
                        f.circle(0xFF5A7A9A, u1 + 2.6f, bv - 0.4f, 1);
                    }
                }
                // The fire circle with stump seats, raised beds, a picnic table, a hammock between two trees.
                float gv0 = 0, gv1 = S;
                for (int[] b : q) {
                    if (b[1] * TT < m) gv0 = Math.max(gv0, (b[1] + b[3]) * T + 7);
                    else gv1 = Math.min(gv1, b[1] * T - 7);
                }
                float gh = gv1 - gv0, cx = L * 0.5f, fr = Math.max(7, Math.min(12, gh / 2));
                f.circle(0xFF9A8A6A, cx, m, fr + 2);
                for (int k = 0; k < 10; k++) {
                    double a = k * Math.PI / 5;
                    float su = cx + (float) Math.cos(a) * fr, sv = m + (float) Math.sin(a) * fr;
                    f.circle(0x40000000, su + 0.5f, sv + 0.7f, 1.7f);
                    f.circle(0xFFA07A50, su, sv, 1.7f);
                    f.circle(0xFFC8A070, su, sv, 0.9f);
                }
                f.circle(0xFF7A7470, cx, m, 3.8f);
                f.circle(0xFF2A2420, cx, m, 2.8f);
                f.circle(0xFFE0782A, cx, m, 1.4f);
                f.circle(0xFFF0C050, cx - 0.3f, m - 0.3f, 0.6f);
                Random rr = new Random(4);
                int k = 0;
                for (float u = 12; u < L - 12; u += 26, k++) {
                    if (Math.abs(u - cx) < fr + 12) continue;
                    float bv = gh > 34 ? (k % 2 == 0 ? m - 12 : m + 12) : m;
                    if (gh > 34 && k % 3 == 1) {
                        f.rect(0x30000000, u - 5 + 0.8f, bv - 2 + 1.2f, u + 5 + 0.8f, bv + 2 + 1.2f);
                        f.rect(0xFF9A7A58, u - 5, bv - 1.6f, u + 5, bv + 1.6f);
                        f.rect(0xFF7A5A3A, u - 5, bv - 3.6f, u + 5, bv - 2.6f);
                        f.rect(0xFF7A5A3A, u - 5, bv + 2.6f, u + 5, bv + 3.6f);
                        continue;
                    }
                    f.rect(0x30000000, u - 6 + 0.8f, bv - 2.5f + 1, u + 6 + 0.8f, bv + 2.5f + 1);
                    f.rect(0xFF8A6440, u - 6.6f, bv - 3.1f, u + 6.6f, bv + 3.1f);
                    if (k % 2 == 0) veg(f, rr, u - 6, bv - 2.5f, u + 6, bv + 2.5f, true);
                    else bed(f, rr, u - 6, bv - 2.5f, u + 6, bv + 2.5f, 0xFFE8C040, 0xFFE07030, 0xFFF0F0E0);
                }
                for (float s = 6; s < L - 6; s += 2.4f) f.circle(0xFFF8E8A0, s, m - 5 + (float) Math.sin(s * 0.25f) * 1.2f, 0.45f);
                if (L > 15 * T) {
                    float hu = L - 24;
                    f.tree(hu - 9, m + 1, 5, 0xFF4E8A3A);
                    f.tree(hu + 9, m + 1, 5, 0xFF4E8A3A);
                    f.oval(0x30000000, hu - 5, m - 1, hu + 6, m + 3);
                    f.oval(0xFFD86A40, hu - 6, m - 2, hu + 6, m + 2);
                    f.line(0xFFE8D8B0, 0.4f, hu - 9, m + 1, hu + 9, m + 1);
                }
                for (float u = 8; u < L - 4; u += 30) {
                    if (clear(q, u, 5, 6)) f.tree(u, 5, 6, 0xFF4E8A3A);
                    if (clear(q, u + 15, S - 5, 6)) f.tree(u + 15, S - 5, 6, 0xFF5A9040);
                }
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float W = f.L, H = f.S;
                int col = TINY[(part * 5 + part / 2) % TINY.length];
                switch ((part * 3 + part / 2) % 4) {
                    case 0: {
                        // Mono-pitch metal roof falling across v.
                        f.rect(City.lighten(col, 0.12f), 0, 0, W, H);
                        for (float v = 0; v < H; v += H / 4) f.rect(Landmarks.shade(col, 1.12f - v / H * 0.35f), 0, v, W, v + H / 4);
                        for (float u = 1.6f; u < W; u += 1.6f) f.line(0x30000000, 0.35f, u, 0, u, H);
                        f.frame(City.darken(col, 0.6f), 0.6f, 0, 0, W, H);
                        solar(f, W * 0.15f, 2, W * 0.6f, H * 0.45f);
                        break;
                    }
                    case 1: {
                        gable(f, 0xFF8A6A4A, 0, 0, W, H, true);
                        f.rect(col, W - 1.4f, 0, W, H);
                        skylight(f, W * 0.3f, H * 0.12f, W * 0.3f + 4, H * 0.12f + 3);
                        break;
                    }
                    case 2: {
                        // A curved (barrel) roof.
                        int[] sh = {City.lighten(col, 0.25f), City.lighten(col, 0.12f), col, City.darken(col, 0.88f), City.darken(col, 0.75f)};
                        for (int k = 0; k < 5; k++) f.rect(sh[k], 0, H * k / 5, W, H * (k + 1) / 5);
                        for (float u = 2; u < W; u += 2) f.line(0x22000000, 0.35f, u, 0, u, H);
                        f.frame(City.darken(col, 0.55f), 0.6f, 0, 0, W, H);
                        break;
                    }
                    default: {
                        deck(f, r, 0xFF6E8A48, 0, 0, W, H, 1.2f);
                        for (int k = 0; k < 12; k++) f.circle(k % 3 == 0 ? 0xFFC8A858 : 0xFF8AA858, 2 + r.nextFloat() * (W - 4), 2 + r.nextFloat() * (H - 4), 0.9f);
                        f.rect(col, 0, 0, W, 1.2f);
                        skylight(f, W * 0.55f, H * 0.4f, W * 0.55f + 4, H * 0.4f + 4);
                    }
                }
                f.circle(0x40000000, W * 0.82f + 0.6f, H * 0.7f + 0.8f, 0.9f);
                f.circle(0xFF6A6E72, W * 0.82f, H * 0.7f, 0.9f);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.SUB | Variants.PARK | Variants.UNI;
        d.minS = 11;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFF3A8A8A;
        out.add(d);
    }

    // ------------------------------------------------------------------ 16. Riverside Lofts

    static void riverLofts(List<Landmarks.Def> out) {
        // An old brick mill on the river turned into lofts: slate roof with rooflights and cut-in terraces, its chimney,
        // the mill race and wheel, a towpath and a narrowboat at the jetty.
        D d = new D("Riverside Lofts", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                ArrayList<int[]> a = new ArrayList<int[]>();
                int lv = S >= 13 ? 5 : 4;
                a.add(B(1, 1, L - 6, lv, 5));
                a.add(B(L - 4, 1, 2, 3, 2));
                return arr(a);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float mv1 = (q[0][1] + q[0][3]) * T, rv0 = S - Math.max(2.4f * T, (S - mv1) * 0.45f), tp = rv0 - 12;
                // A cobbled mill yard with planters, then the towpath and the river.
                cobbles(f, r, 0xFF9A8E7E, 0, 0, L, tp);
                f.rect(0xFFB8A888, 0, tp, L, rv0);
                f.specks(r, (int) (L / 2), 0x22000000, 0x24FFFFFF, 0, tp, L, rv0, 0.7f);
                f.rect(0xFF8A7E6A, 0, rv0 - 1.6f, L, rv0);
                f.rect(0xFF3E6E8A, 0, rv0, L, S);
                f.rect(0xFF35607A, 0, rv0 + (S - rv0) * 0.4f, L, S);
                for (int k = 0; k < (int) (L / 10); k++) {
                    float u = r.nextFloat() * L, v = rv0 + 3 + r.nextFloat() * (S - rv0 - 5);
                    f.line(0x40FFFFFF, 0.4f, u, v, u + 4 + r.nextFloat() * 4, v);
                }
                // The mill race running under the building's end, with the old iron wheel turning in it.
                float ru = q[0][0] * T + q[0][2] * T * 0.62f;
                f.rect(0xFF7A7064, ru - 6, mv1, ru + 6, rv0);
                f.rect(0xFF3A6680, ru - 4.5f, mv1, ru + 4.5f, rv0 + 1);
                f.rect(0x40000000, ru - 7 + 2, mv1 + 1 + 3, ru + 7 + 2, mv1 + 12 + 3);
                f.rect(0xFF4A4440, ru - 7, mv1 + 1, ru + 7, mv1 + 12);
                for (float v = mv1 + 2; v < mv1 + 12; v += 1.6f) f.rect(0xFF7A6A5A, ru - 6.4f, v, ru + 6.4f, v + 0.8f);
                f.rect(0xFF2A2624, ru - 0.8f, mv1, ru + 0.8f, mv1 + 13);
                f.line(0xFF8A7A6A, 1.6f, ru - 9, tp + 4, ru + 9, tp + 4);
                // Planters, bike stands and benches in the yard; a jetty with a narrowboat on the water.
                for (float u = 10; u < ru - 14; u += 22) {
                    if (tp - mv1 < 14) break;
                    f.rect(0x40000000, u + 1, mv1 + 5 + 1.4f, u + 9 + 1, mv1 + 11 + 1.4f);
                    f.rect(0xFF5A5450, u, mv1 + 5, u + 9, mv1 + 11);
                    bed(f, r, u + 1, mv1 + 6, u + 8, mv1 + 10, 0xFFE0C040, 0xFFE06080, 0xFFF0F0F0);
                }
                for (int k = 0; k < 4; k++) f.line(0xFF3A3A3E, 0.6f, 4 + k * 3, mv1 + 2, 4 + k * 3, mv1 + 4);
                float ju = L * 0.25f;
                f.rect(0x40000000, ju + 1.5f, rv0, ju + 9.5f, rv0 + 8 + 2);
                f.rect(0xFF8A6A48, ju, rv0 - 1, ju + 8, rv0 + 8);
                for (float v = rv0; v < rv0 + 8; v += 1.6f) f.line(0xFF6A4A30, 0.35f, ju, v, ju + 8, v);
                float bv = rv0 + 8 + 4, bu0 = ju - 20, bu1 = Math.min(L - 4, ju + 46);
                if (S - bv > 3) {
                    f.roundRect(0x40000000, bu0 + 1, bv - 3 + 1.4f, bu1 + 1, bv + 3 + 1.4f, 3);
                    f.roundRect(0xFF2A4A3A, bu0, bv - 3, bu1, bv + 3, 3);
                    f.rect(0xFFB03A2E, bu0 + 6, bv - 2.2f, bu1 - 8, bv + 2.2f);
                    f.rect(0xFFE0C060, bu0 + 6, bv - 0.4f, bu1 - 8, bv + 0.4f);
                    for (float u = bu0 + 10; u < bu1 - 10; u += 8) f.rect(0xFF8AA0B0, u, bv - 1.4f, u + 3, bv - 0.6f);
                    f.circle(0xFF2A2A2A, bu1 - 13, bv + 1, 0.7f);
                    pot(f, bu0 + 14, bv + 1.2f, 1, 0xFFE06080);
                }
                for (float u = L - 10; u > ru + 12; u -= 6) {
                    for (int k = 0; k < 4; k++) f.line(0xFF6A8A40, 0.6f, u + k, rv0 + 3, u + k - 1.2f, rv0 - 1);
                }
                for (float u = 8; u < L; u += 34) if (Math.abs(u - ru) > 14 && Math.abs(u - ju - 4) > 10) f.tree(u, tp + 2, 6, 0xFF4E7A3A);
                f.lamp(ju - 6, tp + 4);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float W = f.L, H = f.S;
                if (part == 0) {
                    // A long slate gable with brick parapet ends, rows of rooflights, two cut-in terraces and a hoist lucam.
                    int sl = 0xFF525860;
                    gable(f, sl, 0, 0, W, H, true);
                    f.rect(0xFF8A4A36, 0, 0, 2.4f, H);
                    f.rect(0xFF8A4A36, W - 2.4f, 0, W, H);
                    f.rect(0xFFA05A42, 0, 0, 2.4f, 0.8f);
                    for (float u = 8; u < W - 6; u += 11) {
                        skylight(f, u, H * 0.18f, u + 3, H * 0.18f + 4);
                        skylight(f, u + 2, H * 0.62f, u + 5, H * 0.62f + 4);
                    }
                    for (int k = 0; k < 2; k++) {
                        float tu = W * (k == 0 ? 0.25f : 0.7f) - 6;
                        f.rect(0xFF3A3E44, tu - 0.8f, H / 2 + 0.8f, tu + 12.8f, H - 0.6f);
                        f.rect(0xFFA07A54, tu, H / 2 + 1.6f, tu + 12, H - 1.2f);
                        for (float u = tu + 1.4f; u < tu + 12; u += 1.4f) f.line(0xFF7A5A3A, 0.3f, u, H / 2 + 1.6f, u, H - 1.2f);
                        f.line(0xA0D8E8F0, 0.7f, tu, H - 1.4f, tu + 12, H - 1.4f);
                        pot(f, tu + 2.5f, H / 2 + 4, 1.5f, 0xFFE07090);
                        table(f, tu + 8, H * 0.76f, 1.5f, 0xFFE8E4DA);
                    }
                    float lu = W * 0.48f;
                    f.rect(0x48000000, lu + 1.2f, H / 2, lu + 8 + 1.2f, H + 1.4f);
                    gableV(f, sl, lu, H / 2 - 1, lu + 8, H, true);
                    f.rect(0xFF3A3A3A, lu + 3.5f, H - 1, lu + 4.5f, H + 1.5f);
                    f.sign(city, "THE OLD MILL", W * 0.3f, H / 2, 4, 0xFFF0E0C0);
                } else {
                    // The boiler house: a flat roof and the mill's round brick chimney with its iron bands.
                    deck(f, r, 0xFF6A6460, 0, 0, W, H, 1.2f);
                    float cu = W / 2, cv = H * 0.42f, cr = Math.min(W, H) * 0.36f;
                    f.poly(0x38000000, cu - cr * 0.7f, cv + cr * 0.7f, cu + cr * 0.7f, cv - cr * 0.7f, cu + cr * 0.7f + 10, cv - cr * 0.7f + 16, cu - cr * 0.7f + 10, cv + cr * 0.7f + 16);
                    f.circle(0x38000000, cu + 10, cv + 16, cr * 0.8f);
                    f.circle(0xFF7A4232, cu, cv, cr);
                    f.circle(0xFF9A5A44, cu - cr * 0.15f, cv - cr * 0.15f, cr * 0.8f);
                    f.ring(0xFF3A3434, 0.6f, cu, cv, cr * 0.85f);
                    f.circle(0xFF2A2222, cu, cv, cr * 0.5f);
                }
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.OLD | Variants.MID | Variants.IND;
        d.minS = 10;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFF525860;
        out.add(d);
    }

    // ------------------------------------------------------------------ 17. Courtyard Bungalows

    static void bungalowCourt(List<Landmarks.Def> out) {
        // Craftsman bungalows facing each other across a long garden walk, with a bigger bungalow closing the head of it.
        D d = new D("Courtyard Bungalows", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                ArrayList<int[]> a = new ArrayList<int[]>();
                boolean head = L >= 16;
                int end = head ? L - 6 : L - 1;
                int n = Math.max(1, end / 5), u0 = Math.max(1, (end - (n * 5 - 1)) / 2 + (head ? 0 : 0));
                for (int i = 0; i < n; i++) {
                    a.add(B(u0 + i * 5, 1, 4, 3, 1));
                    a.add(B(u0 + i * 5, S - 4, 4, 3, 1));
                }
                if (head) a.add(B(L - 5, (S - 5) / 2, 3, 5, 1));
                return arr(a);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT, m = S / 2;
                f.rect(0xFF7AA056, 0, 0, L, S);
                // The garden walk: brick paving down the middle, lawns either side edged with low hedges, citrus trees.
                float w0 = 4 * T + 8, w1 = S - 4 * T - 8;
                f.rect(0xFF8AB060, 0, w0, L, w1);
                float we = q.length % 2 == 1 ? q[q.length - 1][0] * T : L;
                f.rect(0xFFA0624A, 0, m - 5, we, m + 5);
                for (float u = 0; u < we; u += 5) f.line(0x26000000, 0.35f, u, m - 5, u, m + 5);
                for (float v = m - 5; v <= m + 5; v += 2.5f) f.line(0x26000000, 0.35f, 0, v, we, v);
                for (float u = 22; u < we - 24; u += 40) {
                    f.lamp(u, m - 7);
                    f.lamp(u + 20, m + 7);
                    f.bench(u + 10, m - 7.5f, true);
                }
                if (w1 - w0 > 24)
                    for (int s = 0; s < 2; s++) {
                        float hv = s == 0 ? w0 + 1.5f : w1 - 1.5f;
                        f.roundRect(0x30000000, 1, hv - 1.2f, L - 3, hv + 2.4f, 1.6f);
                        f.roundRect(0xFF4E8A40, 0, hv - 1.8f, L - 4, hv + 1.8f, 1.8f);
                        for (float u = 3; u < L - 5; u += 6) f.circle(0xFF62A050, u, hv - 0.6f, 0.9f);
                    }
                for (int[] b : q) {
                    if (b[2] < 4) continue;
                    boolean top = b[1] * T < m;
                    float u0 = b[0] * T, u1 = (b[0] + b[2]) * T, fv = top ? (b[1] + b[3]) * T : b[1] * T;
                    float pu = u0 + (u1 - u0) * 0.3f;
                    f.rect(0xFFD8CCB4, pu - 3.5f, top ? fv : m + 5, pu + 3.5f, top ? m - 5 : fv);
                    f.rect(0xFF9A8A70, pu - 5, top ? fv : fv - 2, pu + 5, top ? fv + 2 : fv);
                    bed(f, r, pu + 6, top ? fv + 0.5f : fv - 4, u1 - 1, top ? fv + 4 : fv - 0.5f, 0xFFE07040, 0xFFE8C040, 0xFFD04060, 0xFFF0F0E8);
                    float tv = top ? (fv + m - 5) / 2 : (fv + m + 5) / 2;
                    if (Math.abs(tv - m) > 12) {
                        float tu = u0 + (u1 - u0) * 0.72f;
                        f.tree(tu, tv, 6, 0xFF2E6A34);
                        for (int k = 0; k < 5; k++) f.circle(0xFFF0A030, tu + (float) Math.cos(k * 1.3) * 2.6f, tv + (float) Math.sin(k * 1.3) * 2.6f, 0.75f);
                    }
                }
                // The tiled fountain at the head of the walk (or at its end), and birds of paradise round it.
                float fu = q.length % 2 == 1 ? q[q.length - 1][0] * T - 12 : L - 14;
                for (int k = 0; k < 8; k++) {
                    double a = k * Math.PI / 4;
                    f.circle(k % 2 == 0 ? 0xFF3A7AA0 : 0xFFE8D8B0, fu + (float) Math.cos(a) * 6, m + (float) Math.sin(a) * 6, 1.8f);
                }
                f.circle(0xFFC8A060, fu, m, 6);
                f.circle(0xFF5AA8C8, fu, m, 4.8f);
                f.circle(0x90FFFFFF, fu - 1.5f, m - 1.5f, 0.7f);
                f.circle(0xFFE8D8B0, fu, m, 1.4f);
                agave(f, fu - 6, m - 9, 2.6f);
                agave(f, fu - 6, m + 9, 2.6f);
                palm(f, 8, m - 9, 7);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                // Low wide-eaved gables in shingle, a porch gable to the front, rafter tails and a river-stone chimney.
                float W = f.L, H = f.S;
                int[] cols = {0xFF6A5A48, 0xFF5A6A50, 0xFF7A5A40, 0xFF5A5A5E};
                int col = cols[(part / 2 + part) % 4];
                gable(f, col, 0, 0, W, H, true);
                for (float u = 1.5f; u < W; u += 2.2f) {
                    f.rect(0xFF4A3A2A, u, 0, u + 0.6f, 0.9f);
                    f.rect(0xFF4A3A2A, u, H - 0.9f, u + 0.6f, H);
                }
                // The porch gable sits over whichever long side faces the walk: front for even parts, back for odd.
                boolean front = part % 2 == 0;
                float pu0 = W * 0.12f, pu1 = W * 0.48f;
                if (W > 4.5f * TT) {
                    pu0 = W * 0.32f;
                    pu1 = W * 0.68f;
                    front = false;
                }
                float pv0 = front ? H * 0.42f : 0, pv1 = front ? H : H * 0.58f;
                f.rect(0x40000000, pu1, pv0 + 1, pu1 + 1.2f, pv1 + (front ? 0 : 1));
                gableV(f, City.lighten(col, 0.04f), pu0, pv0, pu1, pv1, true);
                f.rect(0xFFE8E0CC, pu0, front ? H - 1 : 0, pu1, front ? H : 1);
                // A river-stone chimney at one end.
                float cu = W - 5, cv = H / 2 - 2.5f;
                f.rect(0x40000000, cu + 1, cv + 1.4f, cu + 5, cv + 6.4f);
                f.rect(0xFF8A8478, cu, cv, cu + 4, cv + 5);
                for (int k = 0; k < 6; k++) f.circle(k % 2 == 0 ? 0xFFA8A090 : 0xFF6E685E, cu + 1 + (k % 2) * 2, cv + 1 + (k / 2) * 1.5f, 0.8f);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.SUB | Variants.MID;
        d.minS = 11;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFF6A5A48;
        out.add(d);
    }

    // ------------------------------------------------------------------ 18. Farmhouse & Barn

    static void farmstead(List<Landmarks.Def> out) {
        // A farmstead left inside the town: white farmhouse, red gambrel barn and silo, a hen run, fields and hay bales.
        D d = new D("Farmhouse & Barn", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                ArrayList<int[]> a = new ArrayList<int[]>();
                int hu = Math.min(6, L - 8);
                a.add(B(1, 1, hu, 3, 2));
                int bu = Math.min(7, L - hu - 3);
                a.add(B(L - 1 - bu, 1, bu, 4, 2));
                return arr(a);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                f.rect(0xFFA08A64, 0, 0, L, S);
                f.specks(r, (int) (L * S / 30), 0x20000000, 0x20FFFFFF, 0, 0, L, S, 0.9f);
                int[] h = q[0], b = q[1];
                float hv1 = (h[1] + h[3]) * T, bv1 = (b[1] + b[3]) * T, bu0 = b[0] * T, hu1 = (h[0] + h[2]) * T;
                float yard = Math.max(hv1, bv1) + 18;
                // The house's front garden with a picket fence and a kitchen plot; a lawn round the house.
                f.rect(0xFF7AA052, 0, 0, hu1 + 6, hv1 + 14);
                veg(f, r, 6, hv1 + 3, hu1 - 20, hv1 + 12, true);
                picket(f, 0xFFF0F0EC, 2, hv1 + 14, hu1 + 6, hv1 + 14);
                picket(f, 0xFFF0F0EC, hu1 + 6, 2, hu1 + 6, hv1 + 14);
                shrub(f, hu1 - 8, hv1 + 6, 3, 0xFFE8C0D0);
                // The silo standing beside the barn, with its long shadow.
                float su = bu0 - 9, sv = bv1 + 4, sr = 8.5f;
                f.poly(0x40000000, su - sr * 0.7f, sv + sr * 0.7f, su + sr * 0.7f, sv - sr * 0.7f, su + sr * 0.7f + 14, sv - sr * 0.7f + 24, su - sr * 0.7f + 14, sv + sr * 0.7f + 24);
                f.circle(0x40000000, su + 14, sv + 24, sr);
                f.circle(0xFF8A9096, su, sv, sr);
                for (int k = 0; k < 10; k++) {
                    double a0 = k * Math.PI / 5, a1 = (k + 1) * Math.PI / 5, am = (a0 + a1) / 2;
                    f.poly(Landmarks.shade(0xFFA8B0B6, 1f + 0.25f * (float) Math.cos(am + 3 * Math.PI / 4)), su, sv,
                            su + (float) Math.cos(a0) * sr * 0.92f, sv + (float) Math.sin(a0) * sr * 0.92f, su + (float) Math.cos(a1) * sr * 0.92f, sv + (float) Math.sin(a1) * sr * 0.92f);
                }
                f.circle(0xFFD8DCE0, su, sv, 1.4f);
                // The farmyard: a hen house and run with chickens, a tractor, hay bales and a water trough.
                float cu = hu1 + 14, cv = yard - 10;
                if (bu0 - 20 - cu > 16) {
                    f.rect(0xFF8A9A60, cu, yard - 14, cu + 24, yard + 6);
                    f.frame(0xC0D8D8D0, 0.4f, cu, yard - 14, cu + 24, yard + 6);
                    shed(f, cu + 1, yard - 13, 9, 6, 0xFF8A4A36);
                    int[] hc = {0xFFE8E0D0, 0xFF9A5A30, 0xFF2A2A2A, 0xFFC08040};
                    for (int k = 0; k < 7; k++) hen(f, cu + 6 + (k * 37 % 16), yard - 4 + (k * 23 % 9), hc[k % 4]);
                }
                f.rect(0xFF8A8E92, bu0 + 4, bv1 + 4, bu0 + 16, bv1 + 8);
                f.rect(0xFF5A8AA8, bu0 + 4.8f, bv1 + 4.8f, bu0 + 15.2f, bv1 + 7.2f);
                float tu = bu0 + 28, tvv = bv1 + 10;
                f.rect(0x40000000, tu - 5 + 1, tvv - 4 + 1.4f, tu + 6 + 1, tvv + 4 + 1.4f);
                f.rect(0xFF2A2A2A, tu - 5, tvv - 4.5f, tu - 1, tvv - 2.5f);
                f.rect(0xFF2A2A2A, tu - 5, tvv + 2.5f, tu - 1, tvv + 4.5f);
                f.rect(0xFF2A2A2A, tu + 3, tvv - 3.6f, tu + 5.4f, tvv - 2.4f);
                f.rect(0xFF2A2A2A, tu + 3, tvv + 2.4f, tu + 5.4f, tvv + 3.6f);
                f.rect(0xFF3A8A3A, tu - 4, tvv - 2.4f, tu + 6, tvv + 2.4f);
                f.rect(0xFF2A3A30, tu - 4, tvv - 2, tu - 0.5f, tvv + 2);
                f.rect(0xFFE0C040, tu + 4.6f, tvv - 1, tu + 6, tvv + 1);
                // The field: ploughed rows and a stack of round bales at its corner, a post-and-rail fence.
                float fv0 = yard + 10;
                if (S - fv0 > 16) {
                    boolean wide = L > 16 * T;
                    float z1 = wide ? L * 0.36f : L * 0.5f, z2 = wide ? L * 0.66f : L * 0.5f;
                    // Crops in ploughed rows, a strip of ripe wheat on wide plots, and a pasture with cows and a pond.
                    f.rect(0xFF7A5E3E, 4, fv0, z1, S - 4);
                    for (float v = fv0 + 2; v < S - 5; v += 3.6f) {
                        f.line(0xFF5E4630, 1, 4, v, z1, v);
                        f.line(0xFF6A9A48, 1.4f, 5, v - 1.2f, z1 - 1, v - 1.2f);
                    }
                    if (wide) {
                        f.rect(0xFFD8B860, z1, fv0, z2, S - 4);
                        for (float v = fv0 + 1.5f; v < S - 4; v += 1.8f) f.line(0xFFC8A850, 0.5f, z1, v, z2, v);
                    }
                    f.rect(0xFF7AA050, z2, fv0, L - 4, S - 4);
                    f.specks(r, (int) ((L - z2) * (S - fv0) / 30), 0xFF8AB060, 0xFF6A9048, z2, fv0, L - 4, S - 4, 1);
                    float pw = L - 4 - z2, ph = S - 4 - fv0;
                    if (pw > 30 && ph > 30) pond(f.c, f.p, Math.min(f.x(L - 4 - pw * 0.45f, S - 4 - ph * 0.45f), f.x(L - 10, S - 10)),
                            Math.min(f.y(L - 4 - pw * 0.45f, S - 4 - ph * 0.45f), f.y(L - 10, S - 10)),
                            Math.max(f.x(L - 4 - pw * 0.45f, S - 4 - ph * 0.45f), f.x(L - 10, S - 10)),
                            Math.max(f.y(L - 4 - pw * 0.45f, S - 4 - ph * 0.45f), f.y(L - 10, S - 10)));
                    cow(f, z2 + pw * 0.3f, fv0 + ph * 0.3f, 15);
                    if (ph > 30) cow(f, z2 + pw * 0.55f, fv0 + ph * 0.62f, -30);
                    f.line(0xFF8A6A48, 0.8f, z2, fv0, z2, S - 4);
                    f.line(0xFF8A6A48, 0.8f, z2, S - 4, L - 4, S - 4);
                    f.line(0xFF8A6A48, 0.8f, L - 4, fv0, L - 4, S - 4);
                    for (int k = 0; k < 3; k++) {
                        float hu = L - 16 - k * 10, hv = fv0 - 6;
                        f.circle(0x40000000, hu + 1.5f, hv + 2, 4.2f);
                        f.circle(0xFFC8A858, hu, hv, 4.2f);
                        f.ring(0xFFA88A40, 0.5f, hu, hv, 2.6f);
                        f.ring(0xFFA88A40, 0.4f, hu, hv, 1.2f);
                    }
                    f.line(0xFF8A6A48, 0.8f, 3, fv0 - 1, L - 3, fv0 - 1);
                    for (float u = 3; u < L; u += 6) f.rect(0xFF6A4A30, u - 0.6f, fv0 - 1.8f, u + 0.6f, fv0 - 0.2f);
                }
                f.tree(L - 6, 6, 7, 0xFF3E7A3A);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float W = f.L, H = f.S;
                if (part == 0) {
                    // The farmhouse: a grey standing-seam roof, a full-width front porch, dormers and a brick chimney.
                    int tin = 0xFF8A9096;
                    f.rect(City.lighten(tin, 0.12f), 0, 0, W, H * 0.4f);
                    f.rect(City.darken(tin, 0.8f), 0, H * 0.4f, W, H * 0.8f);
                    for (float u = 1.6f; u < W; u += 1.6f) f.line(0x30000000, 0.35f, u, 0, u, H * 0.8f);
                    f.line(City.lighten(tin, 0.35f), 1, 0, H * 0.4f, W, H * 0.4f);
                    f.rect(0x40000000, 0, H * 0.8f, W, H * 0.8f + 1.4f);
                    f.rect(0xFFA8B0B4, 0, H * 0.8f, W, H);
                    for (float u = 1.6f; u < W; u += 1.6f) f.line(0x26000000, 0.3f, u, H * 0.8f, u, H);
                    f.line(0xFFF0F0EC, 0.8f, 0, H - 0.4f, W, H - 0.4f);
                    for (float u = W * 0.25f; u < W - 4; u += W * 0.25f) dormer(f, tin, u, H * 0.78f, H * 0.42f, 6);
                    chimney(f, W * 0.12f, H * 0.4f - 1.6f, 2);
                    chimney(f, W * 0.82f, H * 0.4f - 1.6f, 2);
                } else {
                    // The barn: a red gambrel roof with louvred cupolas and a hay hood at the end.
                    gambrel(f, 0xFFA03A2E, 0, 0, W, H);
                    for (int k = 0; k < 2; k++) {
                        float cu = W * (k == 0 ? 0.33f : 0.66f), cv = H / 2;
                        f.rect(0x48000000, cu - 3.5f + 1.2f, cv - 3.5f + 1.8f, cu + 3.5f + 1.2f, cv + 3.5f + 1.8f);
                        f.rect(0xFFF0F0EC, cu - 3.5f, cv - 3.5f, cu + 3.5f, cv + 3.5f);
                        for (float t = cv - 2.8f; t < cv + 3; t += 1) f.line(0xFF9A9A94, 0.4f, cu - 3.5f, t, cu - 2.8f, t);
                        hip(f, 0xFF6A6E72, cu - 2.8f, cv - 2.8f, cu + 2.8f, cv + 2.8f, false);
                    }
                    f.circle(0xFF2A2A2A, W * 0.33f, H / 2, 0.5f);
                    f.poly(0xFF8A2E24, W, H * 0.3f, W - 3, H * 0.4f, W - 3, H * 0.6f, W, H * 0.7f);
                    f.sign(city, "FARM", W / 2, H * 0.25f, 4, 0xFFF0F0EC);
                }
            }
        };
        d.ground = City.DIRT;
        d.districts = Variants.SUB | Variants.PARK;
        d.minS = 12;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFFA03A2E;
        out.add(d);
    }

    /** A cow from above, heading at frame angle a. */
    static void cow(F f, float u, float v, float a) {
        f.turn(u, v, a);
        Canvas c = f.c;
        Paint p = f.p;
        Landmarks.oval(c, p, 0x40000000, -4, -1.6f, 6, 4.4f);
        Landmarks.oval(c, p, 0xFFF0ECE4, -5, -2.6f, 4, 2.6f);
        Landmarks.circle(c, p, 0xFF2A2624, -2, -1, 1.4f);
        Landmarks.circle(c, p, 0xFF2A2624, 1.5f, 1.2f, 1.1f);
        Landmarks.oval(c, p, 0xFFF0ECE4, 3.4f, -1.2f, 6.4f, 1.2f);
        Landmarks.circle(c, p, 0xFFD8A0A0, 6.2f, 0, 0.6f);
        Landmarks.line(c, p, 0xFF2A2624, 0.4f, -5, 0, -6.4f, 0.6f);
        c.restore();
    }

    // ------------------------------------------------------------------ 19. Japanese Machiya Row

    static void machiya(List<Landmarks.Def> out) {
        // Narrow, deep wooden townhouses on a stone lane: grey kawara roofs with a pent eave to the street, lattice
        // fronts and bamboo guards, and a tiny moss garden behind each one.
        D d = new D("Japanese Machiya Row", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                ArrayList<int[]> a = new ArrayList<int[]>();
                int n = (L - 1) / 3, u0 = (L - (n * 3 - 1)) / 2, lv = Math.min(6, S - 6);
                for (int i = 0; i < n; i++) a.add(B(u0 + i * 3, S - 3 - lv, 2, lv, 2));
                return arr(a);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float bv0 = q[0][1] * T, fv = (q[0][1] + q[0][3]) * T;
                f.rect(0xFF6A7A50, 0, 0, L, S);
                // The lane: big granite setts with a drain channel along the house fronts.
                f.rect(0xFFA8A49C, 0, fv, L, S);
                for (float v = fv + 3; v < S; v += 7) {
                    f.line(0x30000000, 0.4f, 0, v, L, v);
                    for (float u = ((int) (v / 7) % 2) * 7; u < L; u += 14) f.line(0x30000000, 0.4f, u, v, u, Math.min(S, v + 7));
                }
                f.rect(0xFF6A6660, 0, fv + 1.2f, L, fv + 2.6f);
                // Each house: a curved bamboo guard and indigo noren at the front; a moss garden, lantern and maple behind.
                Random rr = new Random(2);
                for (int i = 0; i < q.length; i++) {
                    int[] b = q[i];
                    float u0 = b[0] * T, u1 = (b[0] + b[2]) * T;
                    f.rect(0xFF9A7A4A, u0 + 1, fv, u1 - 1, fv + 1.2f);
                    for (float u = u0 + 2; u < u1 - 1; u += 2.4f) f.line(0xFF6A5030, 0.4f, u, fv, u, fv + 1.2f);
                    f.rect(0xFF2A3A6A, u0 + 6, fv, u0 + 14, fv + 1);
                    f.line(0xFFE8E4DA, 0.3f, u0 + 10, fv, u0 + 10, fv + 1);
                    // The gap between houses: a gravel path to the back garden.
                    f.rect(0xFFC0B8A8, u1, bv0 - 4, u1 + T, fv);
                    // The tsuboniwa: moss, raked white gravel, stepping stones, a stone lantern, a basin and a maple.
                    float g0 = 2, g1 = bv0 - 3;
                    if (g1 - g0 > 10) {
                        f.rect(0xFF5A7A40, u0 - T * 0.5f + 1, g0, u1 + T * 0.5f - 1, g1);
                        float ga = Math.max(g0 + (g1 - g0) * 0.5f, g1 - 18);
                        f.roundRect(0xFFE0DCD0, u0 + 2, ga, u1 + 2, g1 - 2, 2);
                        for (float v = ga + 1.2f; v < g1 - 2.5f; v += 1.2f) f.line(0x22000000, 0.3f, u0 + 3, v, u1 + 1, v);
                        f.circle(0x40000000, u0 + 9, (ga + g1) / 2 + 0.8f, 2.4f);
                        f.circle(0xFF8A867C, u0 + 8.4f, (ga + g1) / 2, 2.4f);
                        f.ring(0x22000000, 0.3f, u0 + 8.4f, (ga + g1) / 2, 3.6f);
                        stones(f, u1 - 2, g1 - 2, u1 - 2, g0 + 6, 1.3f, 0xFF9A968C);
                        f.line(0xFF9A7A4A, 0.8f, u1 + T * 0.5f, g0, u1 + T * 0.5f, g1 + 3);
                        for (int k = 0; k < 6; k++) f.circle(k % 2 == 0 ? 0xFF3E6A30 : 0xFF4E7A38, u0 - 3 + (k % 3) * 1.6f, g0 + 2 + (k / 3) * 2, 1.2f);
                        shrub(f, u0 + 2, ga - 3, 2.4f, 0xFFE070A0);
                        float lu = u0 + 5, lv = g0 + (g1 - g0) * 0.3f;
                        f.rect(0x40000000, lu - 2 + 1, lv - 2 + 1.4f, lu + 2 + 1, lv + 2 + 1.4f);
                        f.rect(0xFFA8A49A, lu - 2, lv - 2, lu + 2, lv + 2);
                        f.poly(0xFF8A867C, lu - 2, lv + 2, lu + 2, lv - 2, lu + 2, lv + 2);
                        f.circle(0xFF6A665E, lu, lv, 0.8f);
                        if (i % 2 == 0) f.tree(u1 - 3, g0 + (g1 - g0) * 0.35f, Math.min(6, (g1 - g0) / 2), 0xFFB8402E);
                        else {
                            for (int k = 0; k < 7; k++) {
                                float bu = u1 - 4 + (float) Math.cos(k * 2.1) * 3, bvv = g0 + (g1 - g0) * 0.3f + (float) Math.sin(k * 2.1) * 3;
                                f.circle(0x30000000, bu + 0.6f, bvv + 0.8f, 1.6f);
                                f.circle(k % 2 == 0 ? 0xFF6A9A48 : 0xFF7AAA50, bu, bvv, 1.6f);
                            }
                            f.circle(0xFF6A665E, u0 + 10, g1 - 6, 2);
                            f.circle(0xFF4A7A9A, u0 + 10, g1 - 6, 1.2f);
                        }
                    }
                    pot(f, u1 - 2, fv + 4, 1.4f, rr.nextBoolean() ? 0xFFE8E0F0 : 0);
                }
                // A willow at the lane's end and paper lanterns hung at a few doors.
                f.tree(6, S - 6, 7, 0xFF8AAA58);
                for (int i = 1; i < q.length; i += 2) {
                    float lu = (q[i][0] + q[i][2]) * T - 3;
                    f.circle(0x40000000, lu + 0.5f, fv + 3.4f, 1.3f);
                    f.circle(0xFFE04A30, lu, fv + 2.8f, 1.3f);
                    f.circle(0xFFF0A070, lu - 0.3f, fv + 2.5f, 0.5f);
                }
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                // In the house's own frame u runs from the back garden (0) to the street (W); the ridges run across it.
                float W = f.L, H = f.S;
                int kw = part % 3 == 1 ? 0xFF5A5E64 : part % 3 == 2 ? 0xFF464C54 : 0xFF4E545C;
                float pe = 4, fr = W * (part % 2 == 0 ? 0.55f : 0.42f);
                kawara(f, kw, 0, 0, fr, H);
                f.rect(0x48000000, fr - 1.4f, 0, fr, H);
                kawara(f, City.lighten(kw, 0.05f), fr, 0, W - pe, H);
                f.rect(0x48000000, W - pe, 0, W - pe + 1.2f, H);
                f.rect(0xFF5A6068, W - pe, 0, W, H);
                for (float v = 1.2f; v < H; v += 2.4f) f.line(0xFF6A7078, 0.6f, W - pe, v, W, v);
                f.rect(0xFF3A3E44, W - 0.8f, 0, W, H);
                f.rect(0xFF8A6A48, W - pe - 0.5f, 0, W - pe, H);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.OLD | Variants.MID;
        d.minS = 11;
        d.minL = 10;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFF4E545C;
        out.add(d);
    }

    /** A kawara roof with its ridge along v (across u): the tile rolls run down each slope, onigawara at the ridge ends. */
    static void kawara(F f, int col, float u0, float v0, float u1, float v1) {
        float m = (u0 + u1) / 2;
        f.rect(City.lighten(col, 0.08f), u0, v0, m, v1);
        f.rect(City.darken(col, 0.78f), m, v0, u1, v1);
        for (float v = v0 + 1.2f; v < v1 - 0.3f; v += 2.4f) {
            f.line(City.lighten(col, 0.2f), 0.8f, u0, v, m, v);
            f.line(City.darken(col, 0.9f), 0.8f, m, v, u1, v);
        }
        f.rect(0xFF3A3E44, m - 1.2f, v0, m + 1.2f, v1);
        f.line(0xFF6A7078, 0.5f, m - 0.6f, v0, m - 0.6f, v1);
        f.rect(0xFF2E3238, m - 1.8f, v0, m + 1.8f, v0 + 1.6f);
        f.rect(0xFF2E3238, m - 1.8f, v1 - 1.6f, m + 1.8f, v1);
        f.frame(City.darken(col, 0.6f), 0.5f, u0, v0, u1, v1);
    }

    // ------------------------------------------------------------------ 20. Cape Cod Cottages

    static void capeCod(List<Landmarks.Def> out) {
        // Weathered shingle cottages with steep roofs and big central chimneys, white picket fences, hydrangeas and shell paths.
        D d = new D("Cape Cod Cottages", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                ArrayList<int[]> a = new ArrayList<int[]>();
                int n = (L - 1) / 5, u0 = (L - (n * 5 - 1)) / 2;
                for (int i = 0; i < n; i++) {
                    a.add(B(u0 + i * 5, 1 + (i % 2), 4, 3, 2));
                    if (S >= 11) a.add(B(u0 + i * 5, S - 4 - ((i + 1) % 2), 4, 3, 2));
                }
                return arr(a);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT, m = S / 2;
                f.rect(0xFF96A86A, 0, 0, L, S);
                f.specks(r, (int) (L * S / 30), 0xFFB8B088, 0xFF86985A, 0, 0, L, S, 1);
                // A sandy lane down the middle with dune grass along it.
                f.rect(0xFFD8CCA8, 0, m - 6, L, m + 6);
                f.specks(r, (int) (L / 2), 0x18000000, 0x30FFFFFF, 0, m - 6, L, m + 6, 0.8f);
                for (float u = 3; u < L; u += 12) {
                    for (int k = 0; k < 4; k++) {
                        f.line(0xFF8A9A58, 0.5f, u, m - 6, u - 2 + k, m - 9);
                        f.line(0xFF8A9A58, 0.5f, u + 3, m + 6, u + 1 + k, m + 9);
                    }
                }
                Random rr = new Random(6);
                for (int i = 0; i < q.length; i++) {
                    int[] b = q[i];
                    boolean top = b[1] * T < m;
                    float u0 = b[0] * T, u1 = (b[0] + b[2]) * T, v0 = b[1] * T, v1 = (b[1] + b[3]) * T;
                    // The lot: a white picket fence round it with a rose arbour at the gate, a shell path to the door.
                    float la = u0 - T * 0.5f, lb = u1 + T * 0.5f, lc = top ? 2 : m + 8, ld = top ? m - 8 : S - 2;
                    float gate = (u0 + u1) / 2;
                    float fv = top ? ld : lc;
                    f.rect(0xFFF0ECE0, gate - 3, top ? v1 : fv, gate + 3, top ? fv : v0);
                    f.specks(rr, 14, 0xFFD8D0C0, 0xFFFFFFFF, gate - 3, top ? v1 : fv, gate + 3, top ? fv : v0, 0.7f);
                    picket(f, 0xFFF4F4F0, la, fv, gate - 4, fv);
                    picket(f, 0xFFF4F4F0, gate + 4, fv, lb, fv);
                    picket(f, 0xFFF4F4F0, la, lc, la, ld);
                    picket(f, 0xFFF4F4F0, lb, lc, lb, ld);
                    f.line(0xFFF4F4F0, 1, gate - 4.5f, fv, gate + 4.5f, fv);
                    for (int k = 0; k < 5; k++) f.circle(k % 2 == 0 ? 0xFFE06080 : 0xFF4E7A3E, gate - 4 + k * 2, fv, 0.9f);
                    // Hydrangeas along the house front; an Adirondack pair, a dinghy or buoys in the yard.
                    float hv = top ? v1 + 3 : v0 - 3;
                    for (float u = u0 + 3; u < u1 - 2; u += 9) if (Math.abs(u - gate) > 5) shrub(f, u, hv, 2.6f, i % 2 == 0 ? 0xFF7A90D8 : 0xFFD890C8);
                    float yv = top ? (v1 + fv) / 2 + 2 : (fv + v0) / 2 - 2;
                    if (Math.abs(fv - (top ? v1 : v0)) > 22) {
                        if (i % 3 == 0) {
                            f.turn(u1 - 2, yv, 20);
                            Landmarks.oval(f.c, f.p, 0x40000000, -5, -1.4f, 6, 3.4f);
                            Landmarks.oval(f.c, f.p, 0xFF3A6A9A, -6, -2.6f, 6, 2.6f);
                            Landmarks.oval(f.c, f.p, 0xFFE8E4DA, -5, -1.6f, 5, 1.6f);
                            f.c.restore();
                        } else if (i % 3 == 1) {
                            for (int k = 0; k < 2; k++) {
                                float su = u0 + 4 + k * 6;
                                f.rect(0x30000000, su - 2 + 0.6f, yv - 2 + 0.8f, su + 2 + 0.6f, yv + 2 + 0.8f);
                                f.rect(0xFFF0F0EC, su - 2, yv - 2, su + 2, yv + 2);
                                f.rect(0xFFD8D8D4, su - 2, yv - 2, su + 2, yv - 0.8f);
                            }
                        } else {
                            int[] bc = {0xFFE04030, 0xFFE0C040, 0xFF3A7AC0, 0xFF40A060};
                            for (int k = 0; k < 4; k++) f.circle(bc[k], u0 + 4 + k * 2.6f, yv, 1);
                        }
                    }
                }
                for (float u = 10; u < L - 6; u += 40) f.tree(u, m + ((int) u % 2 == 0 ? 9 : -9), 5.5f, 0xFF4E7A3A);
                // A flagpole on the green.
                f.circle(0xFFE8E8E8, L - 8, m, 1);
                f.rect(0xFF3A5AA0, L - 8, m - 0.5f, L - 3, m + 2.5f);
                f.rect(0xFFD03A30, L - 8, m + 2.5f, L - 3, m + 3.2f);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                // A steep gable in silvered cedar shingle, white trim, a big central brick chimney, dormers to the front.
                float W = f.L, H = f.S;
                int sh = part % 3 == 1 ? 0xFF8A7A66 : 0xFF8E8C86;
                f.rect(City.lighten(sh, 0.1f), 0, 0, W, H / 2);
                f.rect(City.darken(sh, 0.76f), 0, H / 2, W, H);
                for (float v = 1.8f; v < H / 2; v += 1.8f) {
                    f.line(0x30000000, 0.35f, 0, v, W, v);
                    f.line(0x30000000, 0.35f, 0, H - v, W, H - v);
                }
                f.line(City.lighten(sh, 0.3f), 1, 0, H / 2, W, H / 2);
                f.frame(0xFFF0F0EC, 0.8f, 0.4f, 0.4f, W - 0.4f, H - 0.4f);
                boolean frontLow = part % 2 == 1;
                for (int k = 0; k < 2; k++) {
                    float du = W * (k == 0 ? 0.25f : 0.75f);
                    float dl = Math.min(9, H * 0.22f), da = frontLow ? 1 : H - 1 - dl, db = da + dl;
                    f.rect(0x40000000, du - 4 + 0.8f, da + 0.8f, du + 4 + 0.8f, db + 0.8f);
                    gableV(f, sh, du - 4, da, du + 4, db, false);
                    f.rect(0xFFF0F0EC, du - 4, frontLow ? da : db - 1, du + 4, frontLow ? da + 1 : db);
                }
                f.rect(0x48000000, W / 2 - 3 + 1.2f, H / 2 - 3 + 1.6f, W / 2 + 3 + 1.2f, H / 2 + 3 + 1.6f);
                f.rect(0xFF8A4A36, W / 2 - 3, H / 2 - 3, W / 2 + 3, H / 2 + 3);
                f.rect(0xFFA05A42, W / 2 - 3, H / 2 - 3, W / 2 + 3, H / 2 - 2);
                f.circle(0xFF2A2020, W / 2 - 1.2f, H / 2, 0.8f);
                f.circle(0xFF2A2020, W / 2 + 1.2f, H / 2, 0.8f);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.SUB | Variants.OLD | Variants.PARK;
        d.minS = 11;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFF8E8C86;
        out.add(d);
    }

    // ------------------------------------------------------------------ 21. Duplex Row

    static void duplexRow(List<Landmarks.Def> out) {
        // Side-by-side duplexes, each a mirrored pair under one hipped roof, with split front lawns and fenced back yards.
        D d = new D("Duplex Row", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                ArrayList<int[]> a = new ArrayList<int[]>();
                int n = (L - 1) / 6, u0 = (L - (n * 6 - 1)) / 2, lv = S >= 13 ? 4 : 3, v = S - 1 - lv - 2;
                for (int i = 0; i < n; i++) a.add(B(u0 + i * 6, v, 5, lv, 2));
                return arr(a);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float bv0 = q[0][1] * T, fv = (q[0][1] + q[0][3]) * T;
                f.rect(0xFF74A050, 0, 0, L, S);
                f.rect(0xFFB8B4AC, 0, S - 4, L, S);
                Random rr = new Random(8);
                for (int i = 0; i < q.length; i++) {
                    int[] b = q[i];
                    float u0 = b[0] * T, u1 = (b[0] + b[2]) * T, mid = (u0 + u1) / 2;
                    // Front: a path and a mailbox for each half, one tidy bed, one bike on the lawn.
                    for (int s = 0; s < 2; s++) {
                        float pu = s == 0 ? u0 + 10 : u1 - 10;
                        f.rect(0xFFD0C8B8, pu - 2.5f, fv, pu + 2.5f, S - 4);
                        f.rect(0xFF3A3A3E, pu + (s == 0 ? -6 : 4), S - 6, pu + (s == 0 ? -4 : 6), S - 4.5f);
                    }
                    bed(f, rr, u0 + 2, fv + 2, mid - 4, fv + 6, 0xFFE06080, 0xFFE8C040, 0xFFF0F0F0);
                    if (S - 4 - fv > 18) {
                        f.line(0xFF2A6AB0, 0.6f, mid + 6, fv + 12, mid + 12, fv + 11);
                        f.ring(0xFF2A2A2A, 0.5f, mid + 6, fv + 12, 1.6f);
                        f.ring(0xFF2A2A2A, 0.5f, mid + 12, fv + 11, 1.6f);
                    }
                    f.tree(mid, S - 10, 4.5f, 0xFF4E8A3A);
                    // Back: the fence on the party line, a trampoline on one side, a patio and grill on the other.
                    f.line(0xFF9A7A58, 1, mid, 0, mid, bv0);
                    f.line(0xFF9A7A58, 1, u0 - T * 0.5f, 0, u0 - T * 0.5f, bv0);
                    if (bv0 > 30) {
                        float tv = bv0 * 0.45f, tr = Math.min(8, Math.min(mid - u0, bv0) * 0.3f);
                        f.circle(0x40000000, u0 + (mid - u0) / 2 + 2, tv + 3, tr);
                        f.circle(0xFF2A2A2E, u0 + (mid - u0) / 2, tv, tr);
                        f.ring(0xFF3A6AB0, 1.2f, u0 + (mid - u0) / 2, tv, tr);
                        paving(f, 0xFFC8C0B0, mid + 3, bv0 - 14, u1 - 2, bv0, 4);
                        table(f, mid + 12, bv0 - 7, 2.2f, 0xFFE8E4DA);
                        f.rect(0xFF2A2A2A, u1 - 8, bv0 - 12, u1 - 4, bv0 - 9);
                        washing(f, mid + 3, bv0 * 0.4f, u1 - 3);
                        f.tree(u0 + 7, 8, 5.5f, i % 2 == 0 ? 0xFF4E8A3A : 0xFF7A9A3A);
                        shed(f, u1 - 13, 3, 10, 7, 0xFF7A8A6A);
                        if (bv0 > 70) veg(f, rr, mid + 3, 14, mid + 18, bv0 * 0.4f - 4, false);
                    } else {
                        shed(f, u0 + 3, 3, 9, 6, 0xFF7A8A6A);
                        washing(f, mid + 3, 5, u1 - 3);
                    }
                }
                for (float u = 8; u < L; u += 30) f.lamp(u, S - 2);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                // One hipped roof, two halves: shingles of slightly different age, a party-wall parapet, mirrored porches.
                float W = f.L, H = f.S, m = W / 2;
                int[] cols = {0xFF6A6460, 0xFF5A6068, 0xFF7A6A58};
                int a = cols[part % 3], b = City.darken(a, 0.9f);
                hip(f, a, 0, 0, W, H, true);
                f.c.save();
                f.clip(m, 0, W, H);
                hip(f, b, 0, 0, W, H, true);
                f.c.restore();
                f.rect(0x40000000, m, 0, m + 1.2f, H);
                f.rect(0xFFB0A898, m - 0.8f, 0, m + 0.8f, H);
                chimney(f, m - 5.5f, H * 0.35f, 1);
                chimney(f, m + 2, H * 0.35f, 1);
                for (int s = 0; s < 2; s++) {
                    float pu = s == 0 ? W * 0.2f : W * 0.8f;
                    f.rect(0x40000000, pu - 4 + 1, H - 5 + 1, pu + 4 + 1, H + 1);
                    gableV(f, s == 0 ? a : b, pu - 4, H - 5, pu + 4, H, true);
                    f.rect(0xFFF0F0EC, pu - 4, H - 0.8f, pu + 4, H);
                }
                if (part % 2 == 0) solar(f, m + 3, 2, W - 4, H * 0.32f);
                else {
                    f.circle(0x40000000, W * 0.25f + 1, H * 0.25f + 1.4f, 1.8f);
                    f.circle(0xFFE8E8E4, W * 0.25f, H * 0.25f, 1.8f);
                }
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.SUB | Variants.MID;
        d.minS = 10;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFF6A6460;
        out.add(d);
    }

    // ------------------------------------------------------------------ 22. Luxury Condos

    static void luxuryCondos(List<Landmarks.Def> out) {
        // Two glass condo towers rising from a landscaped podium deck with an infinity pool, cabanas and palms.
        D d = new D("Luxury Condos", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                ArrayList<int[]> a = new ArrayList<int[]>();
                int t = S >= 13 ? 5 : 4;
                a.add(B(2, 1, t, t, 18));
                a.add(B(L - 2 - t, S - 1 - t, t, t, 24));
                return arr(a);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                // The podium: a raised deck of pale stone with a glass balustrade and a shadowed edge down to the street.
                f.rect(0xFF8AA870, 0, 0, L, S);
                f.rect(0x48000000, 4, 4, L - 1, S - 1);
                paving(f, 0xFFE4E0D8, 2, 2, L - 4, S - 4, 6);
                f.frame(0xC0B8D8E0, 0.8f, 2.4f, 2.4f, L - 4.4f, S - 4.4f);
                float a0 = (q[0][0] + q[0][2]) * T, b0 = q[1][0] * T, av = (q[0][1] + q[0][3]) * T, bvv = q[1][1] * T;
                // Two open corners of the deck: the pool terrace in one, a lawn with cabanas and a fire table in the other.
                float r1u0 = a0 + 8, r1u1 = L - 10, r1v0 = 9, r1v1 = bvv - 9;
                float r2u0 = 9, r2u1 = b0 - 8, r2v0 = av + 8, r2v1 = S - 10;
                float ph = Math.min(18, (r1v1 - r1v0) * 0.45f), pv0 = r1v1 - ph, pu0 = r1u0 + 4, pu1 = r1u1 - 4;
                if (pu1 - pu0 > 20 && ph > 8) {
                    pool(f, pu0, pv0, pu1, r1v1);
                    f.rect(0xFF2A4A5A, pu0, r1v1 + 1.4f, pu1, r1v1 + 3);
                    float dv0 = Math.max(r1v0, pv0 - 13);
                    f.rect(0xFFB08A60, pu0 - 2, dv0, pu1 + 2, pv0 - 2);
                    for (float u = pu0 - 1; u < pu1 + 2; u += 1.6f) f.line(0xFF8A6A48, 0.3f, u, dv0, u, pv0 - 2);
                    for (float u = pu0 + 4; u < pu1 - 3; u += 7) lounger(f, u, (dv0 + pv0 - 2) / 2, false, 0xFFF0F0EC);
                    if (pu1 - pu0 > 40) parasol(f, pu1 - 8, dv0 + 2, 4, 0xFFF0F0EC, 0xFF2A5A7A);
                }
                if (r2u1 - r2u0 > 20 && r2v1 - r2v0 > 16) {
                    f.roundRect(0xFF7AA058, r2u0, r2v0, r2u1, r2v1, 4);
                    f.specks(r, 30, 0xFF8AB068, 0xFF6A9048, r2u0 + 2, r2v0 + 2, r2u1 - 2, r2v1 - 2, 0.8f);
                    float cu = r2u0 + 9, cv = r2v0 + 9;
                    f.rect(0x40000000, cu - 5 + 1.4f, cv - 5 + 2, cu + 5 + 1.4f, cv + 5 + 2);
                    f.rect(0xFFF4F0E8, cu - 5, cv - 5, cu + 5, cv + 5);
                    f.line(0xFFD8D0C4, 0.5f, cu - 5, cv, cu + 5, cv);
                    f.line(0xFFD8D0C4, 0.5f, cu, cv - 5, cu, cv + 5);
                    float fu = (r2u0 + r2u1) / 2 + 6, fvv = (r2v0 + r2v1) / 2 + 2;
                    paving(f, 0xFFD0C8BC, fu - 10, fvv - 7, fu + 10, fvv + 7, 4);
                    f.rect(0x40000000, fu - 6 + 1, fvv - 3 + 1.4f, fu + 6 + 1, fvv + 3 + 1.4f);
                    f.rect(0xFF4A4A4E, fu - 6, fvv - 3, fu + 6, fvv + 3);
                    f.rect(0xFFE0782A, fu - 4, fvv - 0.6f, fu + 4, fvv + 0.6f);
                    for (int k = 0; k < 4; k++) f.rect(0xFF9A9088, fu - 7 + k * 4, fvv - 6.5f, fu - 5 + k * 4, fvv - 4.5f);
                    if (r2u1 - r2u0 > 50) f.tree(r2u1 - 9, r2v1 - 8, 6, 0xFF4E8A3A);
                    for (float u = r2u0 + 22; u < r2u1 - 16; u += 9) f.bush(u, r2v1 - 4, 2.6f);
                    for (float v = r2v0 + 22; v < r2v1 - 16; v += 9) f.bush(r2u0 + 4, v, 2.6f);
                    lounger(f, fu + 16, fvv - 2, false, 0xFFE8E0D0);
                    lounger(f, fu + 21, fvv - 2, false, 0xFFE8E0D0);
                }
                // A few palms in big planters round the deck edge.
                float[][] pp = {{8, S - 8}, {L - 8, 8}, {L - 8, S * 0.5f}, {L * 0.5f, S - 8}, {8, 8}};
                for (float[] pt : pp)
                    if (clear(q, pt[0], pt[1], 7)) {
                        f.rect(0xFF8A847A, pt[0] - 5, pt[1] - 5, pt[0] + 5, pt[1] + 5);
                        f.rect(0xFF5A7A40, pt[0] - 4, pt[1] - 4, pt[0] + 4, pt[1] + 4);
                        palm(f, pt[0], pt[1], 7);
                    }
                // Glazed lobby canopies at the tower feet.
                f.rect(0x40000000, q[0][0] * T + 1.5f, av + 1.5f, a0 + 1.5f, av + 6.5f);
                f.rect(0xC0A8C8D8, q[0][0] * T, av, a0, av + 5);
                f.rect(0x40000000, b0 + 1.5f, bvv - 5 + 1.5f, (q[1][0] + q[1][2]) * T + 1.5f, bvv + 1.5f);
                f.rect(0xC0A8C8D8, b0, bvv - 5, (q[1][0] + q[1][2]) * T, bvv);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float W = f.L, H = f.S;
                // Blue-green curtain glass with mullions and a sky reflection, a white crown, then the rooftop.
                f.rect(0xFFF0F0EC, 0, 0, W, H);
                f.rect(0xFF3E6A7E, 1, 1, W - 1, H - 1);
                f.poly(0xFF5A8AA0, 1, 1, W * 0.7f, 1, 1, H * 0.7f);
                f.poly(0xFF6A9AB0, 1, 1, W * 0.35f, 1, 1, H * 0.35f);
                for (float u = 3; u < W - 1; u += 3) f.line(0x40FFFFFF, 0.3f, u, 1, u, H - 1);
                for (float v = 3; v < H - 1; v += 3) f.line(0x30FFFFFF, 0.3f, 1, v, W - 1, v);
                f.rect(0xFFF0F0EC, 4, 4, W - 4, H - 4);
                f.rect(0xFFCFCBC3, 5, 5, W - 5, H - 5);
                f.rect(0x30000000, 5, 5, W - 5, 6);
                if (part == 0) {
                    // A sky garden with a rooftop pool.
                    f.rect(0xFFD8D4CC, 5, 5, W - 5, H - 5);
                    pool(f, 7, 7, W - 7, H * 0.42f);
                    f.rect(0xFF7AA058, 7, H * 0.55f, W * 0.55f, H - 7);
                    f.tree(W * 0.3f, H * 0.72f, 4, 0xFF4E8A3A);
                    parasol(f, W * 0.75f, H * 0.7f, 3.5f, 0xFFF0F0EC, 0xFF3A6A8A);
                } else {
                    // A helipad and plant room.
                    float cu = W * 0.42f, cv = H * 0.42f, rr = Math.min(W, H) * 0.3f;
                    f.circle(0xFF5A5E64, cu, cv, rr + 1);
                    f.circle(0xFF6A6E74, cu, cv, rr);
                    f.ring(0xFFE8E8E4, 0.8f, cu, cv, rr * 0.8f);
                    f.line(0xFFE8E8E4, 1, cu - rr * 0.3f, cv - rr * 0.4f, cu - rr * 0.3f, cv + rr * 0.4f);
                    f.line(0xFFE8E8E4, 1, cu + rr * 0.3f, cv - rr * 0.4f, cu + rr * 0.3f, cv + rr * 0.4f);
                    f.line(0xFFE8E8E4, 1, cu - rr * 0.3f, cv, cu + rr * 0.3f, cv);
                    f.rect(0x48000000, W - 14 + 1.2f, H - 12 + 1.6f, W - 5 + 1.2f, H - 5 + 1.6f);
                    f.rect(0xFFB0B4B8, W - 14, H - 12, W - 5, H - 5);
                    acUnit(f, W - 9.5f, H - 8.5f, 2);
                }
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.MID;
        d.minS = 10;
        d.minL = 13;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFF3E6A7E;
        out.add(d);
    }

    // ------------------------------------------------------------------ 23. Senior Apartments

    static void seniorApts(List<Landmarks.Def> out) {
        // An L-shaped block wrapped round a sheltered garden: a gazebo, raised planters, a looping path and plenty of benches.
        D d = new D("Senior Apartments", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                ArrayList<int[]> a = new ArrayList<int[]>();
                int lv = S >= 13 ? 4 : 3;
                a.add(B(1, 1, L - 2, lv, 3));
                a.add(B(1, lv + 2, 3, S - lv - 3, 3));
                return arr(a);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                f.rect(0xFF78A456, 0, 0, L, S);
                float g0u = 4 * T + 6, g0v = (q[0][1] + q[0][3]) * T + 6, g1u = L - 6, g1v = S - 6;
                float cx = (g0u + g1u) / 2, cy = (g0v + g1v) / 2, rx = (g1u - g0u) / 2 - 6, ry = (g1v - g0v) / 2 - 6;
                // The glazed link across the corner between the wings.
                float lv0 = (q[0][1] + q[0][3]) * T, lv1 = q[1][1] * T;
                f.rect(0x40000000, T + 4, lv0, 3 * T - 2, lv1 + 2);
                f.rect(0xFFB8D0D8, T + 2, lv0, 3 * T - 4, lv1);
                f.line(0xFFE8E8E4, 0.6f, 2 * T - 1, lv0, 2 * T - 1, lv1);
                // A wide looping path round the garden, benches along it, lamps.
                f.oval(0xFFD8CCB4, cx - rx - 4, cy - ry - 4, cx + rx + 4, cy + ry + 4);
                f.oval(0xFF84B060, cx - rx + 3, cy - ry + 3, cx + rx - 3, cy + ry - 3);
                f.rect(0xFFD8CCB4, 3 * T, cy - 3, cx - rx, cy + 3);
                int nb = (int) ((rx + ry) / 4);
                for (int k = 0; k < nb; k++) {
                    double a = k * 2 * Math.PI / nb;
                    if (Math.abs(a - Math.PI) < 0.35) continue;
                    shrub(f, cx + (float) Math.cos(a) * (rx - 7), cy + (float) Math.sin(a) * (ry - 7), 2.3f,
                            k % 3 == 0 ? 0xFFE8A0C0 : k % 3 == 1 ? 0xFFF0E8F0 : 0xFFB0A0E0);
                }
                for (int k = 0; k < 6; k++) {
                    double a = k * Math.PI / 3 + 0.5;
                    float bu = cx + (float) Math.cos(a) * (rx + 7), bv = cy + (float) Math.sin(a) * (ry + 7);
                    if (bu > g0u - 2 && bu < L - 4 && bv > g0v - 2 && bv < S - 4) f.bench(bu, bv, Math.abs(Math.sin(a)) > 0.7f);
                }
                // Raised planters at sitting height and a bird bath, the white gazebo in the middle.
                Random rr = new Random(1);
                for (int k = 0; k < 4; k++) {
                    double a = k * Math.PI / 2 + Math.PI / 4;
                    float pu = cx + (float) Math.cos(a) * rx * 0.62f, pv = cy + (float) Math.sin(a) * ry * 0.62f;
                    if (rx < 30 || ry < 20) break;
                    f.rect(0x40000000, pu - 7 + 1, pv - 4 + 1.4f, pu + 7 + 1, pv + 4 + 1.4f);
                    f.rect(0xFFA8987E, pu - 7, pv - 4, pu + 7, pv + 4);
                    bed(f, rr, pu - 6, pv - 3, pu + 6, pv + 3, 0xFFE06080, 0xFFE8C040, 0xFF9080D0, 0xFFF0F0F0);
                }
                float gr = Math.min(9, Math.min(rx, ry) * 0.45f);
                float[] o = new float[16];
                for (int k = 0; k < 8; k++) {
                    double a = k * Math.PI / 4 + Math.PI / 8;
                    o[2 * k] = cx + (float) Math.cos(a) * gr + 2;
                    o[2 * k + 1] = cy + (float) Math.sin(a) * gr + 3;
                }
                f.poly(0x40000000, o);
                cone(f, cx, cy, gr, 0xFFE8E4DA, 8);
                f.circle(0xFFB8B0A0, cx + rx * 0.7f, cy - ry * 0.05f, 2);
                f.circle(0xFF7AB0C8, cx + rx * 0.7f, cy - ry * 0.05f, 1.3f);
                // Trees at the open corners, a shuffleboard court along the long wing, the entrance canopy.
                f.tree(L - 9, S - 9, 7, 0xFF4E8A3A);
                f.tree(g0u + 4, S - 8, 6, 0xFF7A3A34);
                float su0 = Math.max(g0u + 20, L * 0.5f), su1 = Math.min(L - 10, su0 + 40);
                if (su1 - su0 > 24 && g0v > 0) {
                    f.rect(0xFF5A8A9A, su0, g0v - 5.5f, su1, g0v - 0.5f);
                    f.line(0xFFF0F0F0, 0.4f, su0 + 4, g0v - 3, su1 - 4, g0v - 3);
                    f.poly(0xFFF0F0F0, su0 + 2, g0v - 3, su0 + 6, g0v - 5, su0 + 6, g0v - 1);
                }
                f.rect(0x40000000, L - 18 + 1.5f, 0, L - 6 + 1.5f, T + 2);
                f.rect(0xFFE8E4DA, L - 18, 0, L - 6, T);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                // Low hipped roofs in brown concrete tile, dormers, rooflights over the corridors, plant on the flat.
                float W = f.L, H = f.S;
                int col = 0xFF8A6450;
                hip(f, col, 0, 0, W, H, true);
                for (float u = H * 0.6f; u < W - H * 0.6f; u += 20) {
                    skylight(f, u, H * 0.42f, u + 6, H * 0.58f);
                    dormer(f, col, u + 7, H - 1, H * 0.55f, 6.5f);
                }
                acUnit(f, W - H * 0.5f, H * 0.5f, 1.6f);
                if (part == 0) f.sign(city, "MAPLE COURT", W * 0.5f, H * 0.3f, 4, 0xFFF0E8D8);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.SUB | Variants.MID | Variants.OLD;
        d.minS = 10;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFF8A6450;
        out.add(d);
    }

    // ------------------------------------------------------------------ 24. Worker Cottages

    static void workerCottages(List<Landmarks.Def> out) {
        // Two terraces of two-up two-down cottages back to back, brick-walled yards with privies and coal sheds,
        // washing strung over a cobbled back alley.
        D d = new D("Worker Cottages", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                ArrayList<int[]> a = new ArrayList<int[]>();
                for (int row = 0; row < 2; row++) {
                    int v = row == 0 ? 1 : S - 3;
                    for (int u = 1; u + 4 <= L - 1; ) {
                        int rem = L - 1 - u, w = rem >= 14 ? 8 : rem >= 9 ? (rem - 1) / 2 : rem;
                        w -= w % 2;
                        if (w < 4) break;
                        a.add(B(u, v, w, 2, 2));
                        u += w + 1;
                    }
                }
                return arr(a);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT, m = S / 2;
                f.rect(0xFFA8A090, 0, 0, L, S);
                // The back alley: cobbles and a central gutter.
                cobbles(f, r, 0xFF7E786E, 0, m - 8, L, m + 8);
                f.rect(0xFF5A5650, 0, m - 0.8f, L, m + 0.8f);
                // Yards: blue-brick paving, brick walls between, a privy and a coal shed at the alley end, washing.
                for (int[] b : q) {
                    boolean top = b[1] * T < m;
                    float u0 = b[0] * T, u1 = (b[0] + b[2]) * T, ya = top ? (b[1] + b[3]) * T : m + 8, yb = top ? m - 8 : b[1] * T;
                    int houses = b[2] / 2;
                    for (int k = 0; k < houses; k++) {
                        float a = u0 + k * 2 * T, e = a + 2 * T;
                        paving(f, (k + b[0]) % 2 == 0 ? 0xFF6A6A72 : 0xFF74706C, a, ya, e, yb, 4);
                        f.rect(0xFF8A4A36, e - 0.9f, ya, e + 0.9f, yb);
                        if (yb - ya > 14) {
                            float pv = top ? yb - 7 : ya;
                            f.rect(0x48000000, a + 2 + 1, pv + 1.4f, a + 9 + 1, pv + 7 + 1.4f);
                            f.rect(0xFF5A5E64, a + 2, pv, a + 9, pv + 7);
                            f.rect(0xFF6A6E74, a + 2, pv, a + 9, pv + 2);
                            f.rect(0xFF6A5040, e - 9, pv + 1, e - 3, pv + 6);
                            f.rect(0xFF2A2420, e - 8, pv + 2, e - 4, pv + 5);
                            if (k % 2 == 0) washing(f, a + 3, (ya + yb) / 2, e - 3);
                            else {
                                f.oval(0x40000000, a + 12, (ya + yb) / 2 - 1, a + 20, (ya + yb) / 2 + 3);
                                f.oval(0xFF9AA0A6, a + 11, (ya + yb) / 2 - 2, a + 19, (ya + yb) / 2 + 2);
                                f.oval(0xFF6A7076, a + 12, (ya + yb) / 2 - 1.2f, a + 18, (ya + yb) / 2 + 1.2f);
                            }
                        }
                    }
                    f.rect(0xFF8A4A36, u0 - 0.9f, ya, u0 + 0.9f, yb);
                    f.rect(0xFF8A4A36, u0, top ? yb - 0.9f : ya, u1, top ? yb + 0.9f : ya + 0.9f);
                    // The front: a scrubbed step straight onto the pavement at each door.
                    float fv = top ? b[1] * T : (b[1] + b[3]) * T;
                    f.rect(0xFFB0A898, u0, top ? 0 : fv, u1, top ? fv : S);
                    for (int k = 0; k < houses; k++) {
                        float du = u0 + k * 2 * T + (k % 2 == 0 ? 6 : 2 * T - 6);
                        f.rect(0xFFE8E0D0, du - 3, top ? fv - 2 : fv, du + 3, top ? fv : fv + 2);
                    }
                }
                // Gaps between terraces lead through to the alley.
                for (int[] b : q) {
                    float u1 = (b[0] + b[2]) * T;
                    if (u1 < L - T) f.rect(0xFF9A9288, u1 + 1, b[1] * T < m ? 0 : m + 8, u1 + T - 1, b[1] * T < m ? m - 8 : S);
                }
                for (float u = 12; u < L - 6; u += 30) f.lamp(u, m - 6);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                // Welsh slate, a ridge of tiles, a stack of pots on every party wall, and the odd patched slate.
                float W = f.L, H = f.S;
                int n = Math.max(1, Math.round(W / (2 * TT)));
                float hw = W / n;
                gable(f, 0xFF4E545E, 0, 0, W, H, true);
                for (int k = 0; k < 6; k++) {
                    float u = 2 + r.nextFloat() * (W - 5), v = 2 + r.nextFloat() * (H - 5);
                    f.rect(r.nextBoolean() ? 0xFF5E6470 : 0xFF464C56, u, v, u + 2.2f, v + 1.6f);
                }
                f.line(0xFF8A4A36, 1.2f, 0, H / 2, W, H / 2);
                for (int k = 1; k < n; k++) {
                    f.line(0x50000000, 0.6f, k * hw, 0, k * hw, H);
                    chimney(f, k * hw - 3.4f, H / 2 - 1.6f, 3);
                }
                chimney(f, 0.5f, H / 2 - 1.6f, 2);
                chimney(f, W - 4.4f, H / 2 - 1.6f, 2);
                for (int k = 0; k < n; k++) if ((k + part) % 3 == 0) skylight(f, k * hw + hw * 0.4f, H * 0.62f, k * hw + hw * 0.4f + 3, H * 0.62f + 3);
            }
        };
        d.ground = City.LOT;
        d.districts = Variants.OLD | Variants.IND | Variants.MID;
        d.minS = 10;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFF4E545E;
        out.add(d);
    }

    // ------------------------------------------------------------------ 25. Spanish Colonial Hacienda

    static void hacienda(List<Landmarks.Def> out) {
        // Red-tiled wings round an arcaded courtyard with a tiled fountain, orange trees and bougainvillea, a bell gable.
        D d = new D("Spanish Colonial Hacienda", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                ArrayList<int[]> a = new ArrayList<int[]>();
                int dn = S >= 13 ? 4 : 3, ww = L >= 16 && S - dn - 4 != 4 ? 4 : 3;
                a.add(B(1, 1, L - 2, dn, 1));
                a.add(B(1, dn + 2, ww, S - dn - 4, 1));
                a.add(B(L - 1 - ww, dn + 2, ww, S - dn - 4, 1));
                return arr(a);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                f.rect(0xFFC8B48A, 0, 0, L, S);
                f.specks(r, (int) (L * S / 40), 0x18000000, 0x20FFFFFF, 0, 0, L, S, 0.9f);
                float cu0 = (q[1][0] + q[1][2]) * T, cu1 = q[2][0] * T, cv0 = (q[0][1] + q[0][3]) * T, cv1 = S - 2 * T;
                float cx = (cu0 + cu1) / 2, cy = (cv0 + cv1) / 2;
                // The courtyard: terracotta tiles laid on the diagonal, a walk under each arcade.
                f.rect(0xFFD8B48C, q[1][0] * T, cv0 - 2, (q[2][0] + q[2][2]) * T, cv1);
                f.c.save();
                f.clip(cu0, cv0, cu1, cv1);
                for (float k = cu0 - (cv1 - cv0); k < cu1; k += 7) {
                    f.line(0x30000000, 0.35f, k, cv0, k + (cv1 - cv0), cv1);
                    f.line(0x30000000, 0.35f, k, cv1, k + (cv1 - cv0), cv0);
                }
                f.c.restore();
                // Column bases along the arcade edges (the arcades are the lighter roof strips) and bougainvillea on them.
                for (float u = cu0 + 6; u < cu1 - 2; u += 9) {
                    f.rect(0xFFF0E8D8, u - 1.2f, cv0, u + 1.2f, cv0 + 2.2f);
                    if (((int) (u / 9)) % 2 == 0) shrub(f, u + 4.5f, cv0 + 1.5f, 2.2f, 0xFFD03A8A);
                }
                for (float v = cv0 + 6; v < cv1 - 2; v += 9) {
                    f.rect(0xFFF0E8D8, cu0, v - 1.2f, cu0 + 2.2f, v + 1.2f);
                    f.rect(0xFFF0E8D8, cu1 - 2.2f, v - 1.2f, cu1, v + 1.2f);
                }
                // The fountain: an octagonal basin of blue and yellow tiles with a tiered spout.
                float fr = Math.min(10, Math.min(cu1 - cu0, cv1 - cv0) * 0.2f);
                float[] o = new float[16], w = new float[16];
                for (int k = 0; k < 8; k++) {
                    double a = k * Math.PI / 4 + Math.PI / 8;
                    o[2 * k] = cx + (float) Math.cos(a) * (fr + 2);
                    o[2 * k + 1] = cy + (float) Math.sin(a) * (fr + 2);
                    w[2 * k] = cx + (float) Math.cos(a) * fr;
                    w[2 * k + 1] = cy + (float) Math.sin(a) * fr;
                }
                f.poly(0xFF3A6AA8, o);
                for (int k = 0; k < 8; k++) f.circle(0xFFE8C850, o[2 * k], o[2 * k + 1], 1);
                f.poly(0xFF5AA8C8, w);
                f.circle(0xFFE8DCC8, cx, cy, fr * 0.35f);
                f.circle(0xFF5AA8C8, cx, cy, fr * 0.22f);
                f.circle(0xFFE8DCC8, cx, cy, 0.9f);
                // Orange trees in square tiled planters round it, potted geraniums by the doors.
                float ou = Math.min(cu1 - cu0, 80) * 0.32f, ov = (cv1 - cv0) * 0.3f;
                for (int k = 0; k < 4; k++) {
                    float tu = cx + (k % 2 == 0 ? -ou : ou), tv = cy + (k < 2 ? -ov : ov);
                    if (ov < 9) tv = cy;
                    f.rect(0xFF3A6AA8, tu - 4, tv - 4, tu + 4, tv + 4);
                    f.rect(0xFF6A5038, tu - 3, tv - 3, tu + 3, tv + 3);
                    f.tree(tu, tv, 5, 0xFF2E6A34);
                    for (int j = 0; j < 5; j++) f.circle(0xFFF09030, tu + (float) Math.cos(j * 1.3) * 2.6f, tv + (float) Math.sin(j * 1.3) * 2.6f, 0.75f);
                }
                for (float u = cu0 + 4; u < cu1 - 2; u += 18) pot(f, u, cv1 - 3, 1.6f, 0xFFD03030);
                // The south wall with its great wooden gate; agave, olives and a palm outside.
                int wall = 0xFFE8DCC4;
                f.rect(0x40000000, 0, cv1 + 2, L, cv1 + 4);
                f.rect(wall, T, cv1, cx - 9, cv1 + 2.6f);
                f.rect(wall, cx + 9, cv1, L - T, cv1 + 2.6f);
                f.rect(0xFF6A4228, cx - 9, cv1 + 0.2f, cx + 9, cv1 + 2.4f);
                f.line(0xFF3A2414, 0.4f, cx, cv1, cx, cv1 + 2.6f);
                barrelTilesU(f, 0xFFB85A3A, cx - 11, cv1 - 1.5f, cx + 11, cv1 + 0.5f);
                f.rect(0xFFD8C8A8, cx - 6, cv1 + 2.6f, cx + 6, S);
                agave(f, cx - 18, S - 9, 3.5f);
                agave(f, cx + 18, S - 9, 3.5f);
                olive(f, 12, S - 12, 6);
                if (L > 15 * T) olive(f, L - 12, S - 12, 6);
                palm(f, L - 8, S - 7, 7);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                // Red barrel-tile roofs over white walls, a lower arcade roof on the courtyard side, chimneys and a bell gable.
                float W = f.L, H = f.S;
                int tile = 0xFFB85A3A;
                float ar = Math.min(8, H * 0.34f);
                boolean hi = part != 2;
                float m0 = hi ? 0 : ar, m1 = hi ? H - ar : H;
                f.rect(0xFFF0E8D8, 0, 0, W, H);
                float mid = (m0 + m1) / 2;
                barrelTiles(f, City.lighten(tile, 0.08f), 0, m0, W, mid);
                barrelTiles(f, City.darken(tile, 0.82f), 0, mid, W, m1);
                f.rect(0xFF8A3A24, 0, mid - 0.9f, W, mid + 0.9f);
                f.line(City.lighten(tile, 0.3f), 0.5f, 0, mid - 0.6f, W, mid - 0.6f);
                float a0 = hi ? m1 : 0, a1 = hi ? H : m0;
                f.rect(0x40000000, 0, hi ? a0 : a1 - 1.4f, W, hi ? a0 + 1.4f : a1);
                barrelTiles(f, City.lighten(tile, 0.2f), 0, a0, W, a1);
                for (float u = 4.5f; u < W - 2; u += 9) f.rect(0xFFF0E8D8, u - 1, hi ? H - 1.6f : 0, u + 1, hi ? H : 1.6f);
                f.rect(0xFFF0E8D8, 0, hi ? H - 0.8f : 0, W, hi ? H : 0.8f);
                stack(f, W * 0.2f, (m0 + mid) / 2 - 1.8f, 3.6f, 0xFFF0E8D8);
                if (part == 0) {
                    // The espadaña: a white bell gable with two bells.
                    float bu = W * 0.5f;
                    f.rect(0x48000000, bu - 10 + 2, mid - 2 + 3, bu + 10 + 2, mid + 2 + 3);
                    f.rect(0xFFF4ECDC, bu - 10, mid - 2, bu + 10, mid + 2);
                    f.poly(0xFFF4ECDC, bu - 5, mid - 2, bu, mid - 6, bu + 5, mid - 2);
                    f.rect(0xFFB85A3A, bu - 10.5f, mid - 2.6f, bu + 10.5f, mid - 1.8f);
                    f.circle(0xFF3A3434, bu - 4.5f, mid, 1.8f);
                    f.circle(0xFF3A3434, bu + 4.5f, mid, 1.8f);
                    f.circle(0xFFB88A30, bu - 4.5f, mid, 1.2f);
                    f.circle(0xFFB88A30, bu + 4.5f, mid, 1.2f);
                    f.circle(0xFFD8B048, bu, mid - 4.4f, 0.8f);
                    f.sign(city, "HACIENDA", W * 0.5f, H - ar / 2, 4, 0xFFF0E0C0);
                }
            }
        };
        d.ground = City.SAND;
        d.districts = Variants.SUB | Variants.OLD | Variants.PARK;
        d.minS = 11;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFFB85A3A;
        out.add(d);
    }
}
