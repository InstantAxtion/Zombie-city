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
    static void dormer(F f, int col, float u, float v0, float v1, float w) {
        f.rect(0x40000000, u - w / 2 + 0.8f, Math.min(v0, v1) + 0.8f, u + w / 2 + 0.8f, Math.max(v0, v1) + 0.8f);
        gableV(f, col, u - w / 2, Math.min(v0, v1), u + w / 2, Math.max(v0, v1), false);
    }

    /** A white picket fence from (u0,v0) to (u1,v1). */
    static void picket(F f, int col, float u0, float v0, float u1, float v1) {
        f.line(0x40000000, 0.9f, u0 + 0.5f, v0 + 0.7f, u1 + 0.5f, v1 + 0.7f);
        f.line(col, 0.7f, u0, v0, u1, v1);
        float len = (float) Math.hypot(u1 - u0, v1 - v0);
        int n = Math.max(1, (int) (len / 2.2f));
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
        int n = (int) ((u1 - u0) * (v1 - v0) / 7) + 2;
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
                f.line(0xFF5A4028, 1.4f, u0 + 0.6f, v, u1 - 0.6f, v);
                for (float u = u0 + 1.2f; u < u1 - 0.8f; u += 1.8f) f.circle(cc[k % 4], u, v, 0.9f + r.nextFloat() * 0.3f);
            }
        } else {
            for (float u = u0 + 1.5f; u < u1 - 0.8f; u += 2.6f, k++) {
                f.line(0xFF5A4028, 1.4f, u, v0 + 0.6f, u, v1 - 0.6f);
                for (float v = v0 + 1.2f; v < v1 - 0.8f; v += 1.8f) f.circle(cc[k % 4], u, v, 0.9f + r.nextFloat() * 0.3f);
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
        f.rect(City.darken(col, 0.85f), u0, v0, u1, v1);
        for (float v = v0 + 1; v < v1; v += 1.8f)
            for (float u = u0 + 1 + ((int) ((v - v0) / 1.8f) % 2) * 0.9f; u < u1; u += 1.8f)
                f.circle(Landmarks.shade(col, 0.92f + r.nextFloat() * 0.16f), u, v, 0.75f);
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
        for (int k = 0; k < 5; k++) {
            double a = k * 1.3 + u * 0.1;
            f.circle(bloom, u + (float) Math.cos(a) * r * 0.5f, v + (float) Math.sin(a) * r * 0.5f, r * 0.32f);
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
        for (float u = u0 + 0.9f; u < u1 - 0.3f; u += 1.8f) {
            f.line(col, 1.2f, u, v0, u, v1);
            f.line(City.lighten(col, 0.18f), 0.4f, u - 0.3f, v0, u - 0.3f, v1);
        }
        for (float v = v0 + 3; v < v1; v += 3) f.line(0x22000000, 0.4f, u0, v, u1, v);
    }

    /** Barrel tiles running along u. */
    static void barrelTilesU(F f, int col, float u0, float v0, float u1, float v1) {
        f.rect(City.darken(col, 0.8f), u0, v0, u1, v1);
        for (float v = v0 + 0.9f; v < v1 - 0.3f; v += 1.8f) {
            f.line(col, 1.2f, u0, v, u1, v);
            f.line(City.lighten(col, 0.18f), 0.4f, u0, v - 0.3f, u1, v - 0.3f);
        }
        for (float u = u0 + 3; u < u1; u += 3) f.line(0x22000000, 0.4f, u, v0, u, v1);
    }

    /** A red Spanish-tile hipped roof: tiles run down each face. */
    static void tileHip(F f, int col, float u0, float v0, float u1, float v1) {
        float h = Math.min(u1 - u0, v1 - v0) / 2, au = u0 + h, av = v0 + h, bu = u1 - h, bv = v1 - h;
        hip(f, col, u0, v0, u1, v1, false);
        int ln = 0x38000000;
        for (float u = u0 + 1.6f; u < u1 - 0.5f; u += 1.6f) {
            float vt = Math.max(av, v0 + Math.min(u - u0, u1 - u)), vb = Math.min(bv, v1 - Math.min(u - u0, u1 - u));
            {
                f.line(ln, 0.45f, u, v0, u, Math.min(vt, av));
                f.line(ln, 0.45f, u, Math.max(vb, bv), u, v1);
            }
        }
        for (float v = v0 + 1.6f; v < v1 - 0.5f; v += 1.6f) {
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
        f.rect(City.lighten(col, 0.16f), u0, v0, u1, v0 + h * 0.18f);
        f.rect(City.lighten(col, 0.05f), u0, v0 + h * 0.18f, u1, v0 + h * 0.5f);
        f.rect(City.darken(col, 0.84f), u0, v0 + h * 0.5f, u1, v0 + h * 0.82f);
        f.rect(City.darken(col, 0.7f), u0, v0 + h * 0.82f, u1, v1);
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
                    f.oval(0xFF6A5038, hc - br * 1.6f, cv - br, hc + br * 1.6f, cv + br);
                    for (int k = 0; k < 12; k++) {
                        double a = k * Math.PI / 6;
                        f.circle(0xFF4E7A3E, hc + (float) Math.cos(a) * br * 1.25f, cv + (float) Math.sin(a) * br * 0.75f, 1.4f);
                        f.circle(k % 2 == 0 ? 0xFFD05868 : 0xFFF0E0E8, hc + (float) Math.cos(a) * br * 1.25f - 0.3f, cv + (float) Math.sin(a) * br * 0.75f - 0.3f, 0.8f);
                    }
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
                    cone(f, gu, gv, 7, 0xFFE8E4DA, 8);
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
                    dormer(f, sl, pw + (W - pw) * 0.35f, 0.5f, 6, 5);
                    float h2 = Math.min(W - pw, H - pw) / 2;
                    for (float u = pw + h2; u <= W - h2 + 0.1f; u += 1.4f) f.circle(0xFF26262A, u, h2, 0.45f);
                    chimney(f, pw + h2 - 1, h2 - 5, 2);
                    chimney(f, W - h2 - 4, h2 + 1.5f, 2);
                    // The round corner turret with its conical slate cap.
                    float tr = Math.min(7.5f, H * 0.24f);
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
                    if (w < 2) break;
                    a.add(B(u, 1, w, dv, 3));
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
                        f.rect(0x30000000, u + 0.5f, v + 0.7f, u + 6.5f, v + 5.7f);
                        f.rect(blanket[k], u, v, u + 6, v + 5);
                        f.line(0x60FFFFFF, 0.5f, u, v + 2.5f, u + 6, v + 2.5f);
                        f.line(0x60FFFFFF, 0.5f, u + 3, v, u + 3, v + 5);
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
                    f.tree(u1 - 4, top ? yb - 7 : ya + 7, 5.5f, 0xFF5A8A3A);
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

    /** An adobe flat roof: a rounded parapet, a mud deck, vigas poking out of one wall and canales. */
    static void adobeRoof(F f, Random r, float W, float H, boolean upper) {
        int mud = 0xFFC8946A;
        f.roundRect(City.darken(mud, 0.82f), 0, 0, W, H, 2.5f);
        f.roundRect(mud, 1.6f, 1.6f, W - 1.6f, H - 1.6f, 1.5f);
        f.rect(City.lighten(mud, 0.12f), 1.6f, 0.4f, W - 1.6f, 1.4f);
        f.specks(r, (int) (W * H / 25), 0x1C000000, 0x18FFFFFF, 2, 2, W - 2, H - 2, 0.8f);
        for (float u = 4; u < W - 2; u += 4) {
            f.circle(0x40000000, u + 0.4f, H - 0.4f, 0.9f);
            f.circle(0xFF5A3E28, u, H - 0.8f, 0.8f);
        }
        f.rect(0xFF8A5A3A, W - 1.5f, H * 0.3f, W + 1.5f, H * 0.3f + 1.2f);
        if (upper) {
            float u0 = W * 0.4f, v0 = 0, u1 = W, v1 = H * 0.62f;
            f.roundRect(0x40000000, u0 - 1.6f, v0, u1, v1 + 2.4f, 2);
            f.roundRect(City.darken(mud, 0.86f), u0, v0, u1, v1, 2);
            f.roundRect(City.lighten(mud, 0.06f), u0 + 1.4f, v0 + 1.4f, u1 - 1.4f, v1 - 1.4f, 1.2f);
            for (float u = u0 + 3; u < u1 - 2; u += 3.5f) f.circle(0xFF5A3E28, u, v1 + 0.6f, 0.7f);
            // A ladder up from the lower roof.
            float lu = u0 - 3;
            f.line(0xFF7A5A3A, 0.6f, lu, v1 - 6, lu, v1 + 3);
            f.line(0xFF7A5A3A, 0.6f, lu + 2, v1 - 6, lu + 2, v1 + 3);
            for (float v = v1 - 5; v < v1 + 3; v += 1.6f) f.line(0xFF7A5A3A, 0.5f, lu, v, lu + 2, v);
            f.circle(0x40000000, u0 + 4.5f, v0 + 5, 2.2f);
            f.circle(0xFFB48660, u0 + 4, v0 + 4.4f, 2.2f);
            f.circle(0xFF3A2A20, u0 + 4, v0 + 4.4f, 1);
        } else {
            f.circle(0x40000000, W * 0.25f + 0.6f, H * 0.4f + 0.8f, 2);
            f.circle(0xFFB48660, W * 0.25f, H * 0.4f, 2);
            f.circle(0xFF3A2A20, W * 0.25f, H * 0.4f, 0.9f);
        }
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
                f.specks(r, (int) (L * S / 30), 0x1C000000, 0x24FFFFFF, 0, 0, L, S, 0.9f);
                int[] w = q[q.length - 2];
                float cu0 = (w[0] + w[2]) * T, cu1 = q[q.length - 1][0] * T, cv0 = (q[0][1] + q[0][3]) * T, cv1 = S - T;
                // The courtyard: packed earth, a flagstone cross path and a round stone well in the middle.
                f.rect(0xFFCCAA78, cu0 - 4, cv0, cu1 + 4, S - 4);
                float cx = (cu0 + cu1) / 2, cy = (cv0 + cv1) / 2;
                for (float u = cu0; u < cu1; u += 5) {
                    f.rect(0xFFB8A488, u + 0.4f, cy - 2.4f, u + 4.6f, cy + 2.4f);
                }
                for (float v = cv0; v < S; v += 5) f.rect(0xFFB8A488, cx - 2.4f, v + 0.4f, cx + 2.4f, v + 4.6f);
                f.circle(0x40000000, cx + 1, cy + 1.4f, 5);
                f.circle(0xFF9A8A70, cx, cy, 5);
                f.circle(0xFF2A3A44, cx, cy, 3.2f);
                f.line(0xFF6A4A2E, 0.8f, cx - 5.5f, cy, cx + 5.5f, cy);
                // An horno (beehive oven), a ramada of poles and latillas with shade beneath, cacti and agave.
                float hu = cu1 - 7, hv = cv0 + 7;
                f.circle(0x40000000, hu + 1.5f, hv + 2, 4.5f);
                f.circle(0xFFC08A5E, hu, hv, 4.5f);
                f.circle(0xFFD09A6E, hu - 1.2f, hv - 1.2f, 2.5f);
                f.rect(0xFF2A2020, hu - 1.2f, hv + 3, hu + 1.2f, hv + 4.6f);
                float ru0 = cu0 + 2, rv0 = cv0 + 2;
                if (cu1 - cu0 > 60) {
                    f.rect(0x38000000, ru0 + 2, rv0 + 3, ru0 + 20, rv0 + 14);
                    for (float u = ru0; u <= ru0 + 18; u += 1.4f) f.line(0xFFA88A60, 0.6f, u, rv0, u, rv0 + 11);
                    f.line(0xFF6A4A2E, 1f, ru0, rv0 + 1, ru0 + 18, rv0 + 1);
                    f.line(0xFF6A4A2E, 1f, ru0, rv0 + 10, ru0 + 18, rv0 + 10);
                    bench(f.c, f.p, f.x(ru0 + 9, rv0 + 6), f.y(ru0 + 9, rv0 + 6), !f.t);
                }
                cactus(f, cu0 + 6, cv1 - 4, 2.4f, true);
                cactus(f, cu1 - 5, cv1 - 8, 1.8f, false);
                agave(f, cx + 12, cy + 9, 3.5f);
                agave(f, cx - 13, cy - 8, 3);
                for (int[] b : q) pot(f, b[0] * T + 3, (b[1] + b[3]) * T + 2.5f, 1.5f, 0xFFD04040);
                // The outer adobe wall round the compound with a gate on the far side; desert planting outside.
                int wall = 0xFFB88A60;
                f.roundRect(wall, 2, S - 4, cx - 6, S - 2, 1);
                f.roundRect(wall, cx + 6, S - 4, L - 2, S - 2, 1);
                f.rect(0xFF7A5232, cx - 6, S - 3.6f, cx + 6, S - 2.4f);
                for (float v = cv0; v < S - 4; v += 18) {
                    if (clear(q, 1.5f, v, 1)) cactus(f, 1.6f, v, 1.4f, false);
                }
                cactus(f, L - 8, S - 8, 2.2f, true);
                for (int k = 0; k < 4; k++) {
                    float u = 6 + k * (L - 12) / 3f;
                    if (clear(q, u, S - 1, 2)) f.circle(0xFF8A9A60, u, S - 0.8f, 1.6f);
                }
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                adobeRoof(f, r, f.L, f.S, part == 0 && f.L > 3 * TT && f.S >= 3 * TT);
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
                f.oval(0xFF8A7A58, cx - 14, cy - 10, cx + 14, cy + 10);
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
                for (int k = 0; k < (int) (L * S / 500); k++) {
                    float u = 6 + tr.nextFloat() * (L - 12), v = 6 + tr.nextFloat() * (S - 12);
                    if (!clear(q, u, v, 9) || Math.hypot(u - cx, (v - cy) * 1.3f) < 20) continue;
                    pine(f, u, v, 5 + tr.nextFloat() * 3, tr.nextBoolean() ? 0xFF2E5A34 : 0xFF3A6A3A);
                }
                for (float u = 5; u < L; u += 14) {
                    if (clear(q, u, 4, 7)) pine(f, u, 4, 5, 0xFF2E5A34);
                    if (clear(q, u + 7, S - 4, 7)) pine(f, u + 7, S - 4, 5, 0xFF3A6A3A);
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
                    for (float u = (v % 4 == 0 ? 1.5f : 3); u < W; u += 3) {
                        f.line(0x20000000, 0.3f, u, v - 2, u, v);
                        f.line(0x20000000, 0.3f, u, H - v, u, H - v + 2);
                    }
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
                int lu = Math.min(9, L - 7), lv = S >= 13 ? 5 : 4, u = L >= 16 ? 2 : 1;
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
                for (float u = 1; u < L; u += 3) {
                    f.line(0x30000000, 0.35f, u, t1 - 2.4f, u, t1);
                    f.line(0x30000000, 0.35f, u + 1.5f, t2 - 2.4f, u + 1.5f, t2);
                }
                for (float u = 16; u < L - 14; u += 22) olive(f, u + ((int) u % 3) * 2, (t1 + t2) / 2 + ((int) u % 2 == 0 ? 1.5f : -1), Math.min(7.5f, (t2 - t1) / 2 - 1));
                for (float v = t2 + 4; v < S - 2; v += 4.5f) {
                    f.line(0xFF7A6AA0, 2.6f, 12, v, L - 14, v);
                    for (float u = 13; u < L - 14; u += 2.5f) f.circle(0xFF9080C0, u, v - 0.4f, 0.6f);
                }
                float su = L - 8;
                for (float v = t1 - 3; v < S; v += 2) f.rect((int) (v / 2) % 2 == 0 ? 0xFFD0C4A8 : 0xFFC0B498, su - 3, v, su + 3, v + 2);
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
                    float ts = Math.min(H * 0.62f, 17), tu = W - ts - 0.5f;
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
                for (int i = 0; i + 1 < q.length; i++) {
                    float g0 = (q[i][1] + q[i][3]) * T + 6, g1 = q[i + 1][1] * T - 6;
                    f.rect(0xFF7A9A5A, 10, g0, L - 10, g1);
                    f.frame(0xFF8A8680, 1.2f, 10, g0, L - 10, g1);
                    for (float u = 22; u < L - 18; u += 34) {
                        f.rect(0xFF8A8680, u - 5, (g0 + g1) / 2 - 5, u + 5, (g0 + g1) / 2 + 5);
                        f.tree(u, (g0 + g1) / 2, 6.5f, 0xFF4A7A3E);
                    }
                }
                float pu = L * 0.5f + 10, pv = q.length > 1 ? (q[0][1] + q[0][3]) * T + 24 : S - 14;
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
                for (float v = 2.2f; v < H - 1.5f; v += 1.4f) f.line(0x16000000, 0.35f, 1.2f, v, W - 1.2f, v);
                f.specks(r, (int) (W * H / 30), 0x20000000, 0x1CFFFFFF, 1.5f, 1.5f, W - 1.5f, H - 1.5f, 0.8f);
                for (float u = 2; u < W - 3; u += 5) {
                    f.rect(0xFF7E7A74, u, 0, u + 3.4f, 1.2f);
                    f.rect(0xFF7E7A74, u + 2.5f, H - 1.2f, u + 5.9f, H);
                }
                // Lift and stair towers, plant rooms, a run of vent stacks.
                for (float u = W * 0.18f; u < W - 8; u += W * 0.32f) {
                    f.rect(0x50000000, u + 2, 3.5f, u + 10 + 2, H - 3.5f + 3);
                    f.rect(0xFF8A867E, u, 2.5f, u + 10, H - 3.5f);
                    f.rect(0xFFB0ACA4, u + 0.6f, 3.1f, u + 9.4f, H - 4.1f);
                    for (float v = 4.5f; v < H - 4.5f; v += 1.2f) f.line(0x18000000, 0.3f, u + 0.6f, v, u + 9.4f, v);
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
                    for (float u = u0 + 2.6f; u < u1 - 2; u += 1.6f) f.line(0xFF8A6A48, 0.3f, u, dv, u, dv + 7);
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
                for (float u = 16; u + 9 < split - 3; u += 13, k++) {
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
                    if (ru1 - hu1 > 10) {
                        f.tree((hu1 + ru1) / 2 + 2, (hv0 + hv1) / 2, 5.5f, 0xFF5A9040);
                        f.tree((pu1 + ru1) / 2 + 2, (pv0 + pv1) / 2, 5.5f, 0xFF4E8A3A);
                        for (int j = 0; j < 4; j++) f.circle(0xFFD04A30, (hu1 + ru1) / 2 + 2 + (j - 1.5f) * 2.2f, (hv0 + hv1) / 2 + (j % 2) * 2, 0.7f);
                    }
                }
                // The long communal table on the cross path, under a festoon of lights.
                float tu0 = split + 4, tu1 = Math.min(L - 14, tu0 + 26);
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
                for (int k = 0; k < (int) (W * H / 6); k++) {
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
    static void mobileHomes(List<Landmarks.Def> out) {}
    static void gatedEstate(List<Landmarks.Def> out) {}
    static void tudor(List<Landmarks.Def> out) {}
    static void mews(List<Landmarks.Def> out) {}
    static void tinyHouses(List<Landmarks.Def> out) {}
    static void riverLofts(List<Landmarks.Def> out) {}
    static void bungalowCourt(List<Landmarks.Def> out) {}
    static void farmstead(List<Landmarks.Def> out) {}
    static void machiya(List<Landmarks.Def> out) {}
    static void capeCod(List<Landmarks.Def> out) {}
    static void duplexRow(List<Landmarks.Def> out) {}
    static void luxuryCondos(List<Landmarks.Def> out) {}
    static void seniorApts(List<Landmarks.Def> out) {}
    static void workerCottages(List<Landmarks.Def> out) {}
    static void hacienda(List<Landmarks.Def> out) {}
}
