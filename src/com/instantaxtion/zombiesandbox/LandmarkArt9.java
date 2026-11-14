package com.instantaxtion.zombiesandbox;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.Landmarks.*;

/** ((22.8) Landmark designs, part 9 (see Landmarks): the buildings that give a neighbourhood its character. */
final class LandmarkArt9 {
    private LandmarkArt9() {
    }

    static void register(List<Landmarks.Def> out) {
        chinatownGate(out);
        dimSum(out);
        asianMarket(out);
        deliRow(out);
        gelato(out);
        operaCafe(out);
        galleryWarehouse(out);
        streetArt(out);
        artistLofts(out);
        jazzClub(out);
        vaudeville(out);
        comedyClub(out);
        stockExchange(out);
        bankHQ(out);
        corporateAtrium(out);
        fishMarket(out);
        chandlery(out);
        harbourPub(out);
        taqueria(out);
        bazaar(out);
        kebabRow(out);
        irishPub(out);
        beerGarden(out);
        noodleArcade(out);
        teaHouseGarden(out);
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
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(w);
            p.setColor(col);
            c.drawCircle(x(u, v), y(u, v), r, p);
            p.setStyle(Paint.Style.FILL);
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

        int[][] lp(int L, int S) {
            return new int[0][];
        }

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

    /** A hipped roof over the rectangle, its faces shaded for light from the top left; courses adds tile lines. */
    static void hip(F f, int col, float u0, float v0, float u1, float v1, boolean courses) {
        float h = Math.min(u1 - u0, v1 - v0) / 2, au = u0 + h, av = v0 + h, bu = u1 - h, bv = v1 - h;
        f.poly(City.lighten(col, 0.12f), u0, v0, u1, v0, bu, av, au, av);
        f.poly(col, u0, v0, u0, v1, au, bv, au, av);
        f.poly(City.darken(col, 0.82f), u1, v0, u1, v1, bu, bv, bu, av);
        f.poly(City.darken(col, 0.7f), u0, v1, u1, v1, bu, bv, au, bv);
        if (courses)
            for (float k = 3; k < h - 1; k += 3) f.frame(0x26000000, 0.5f, u0 + k, v0 + k, u1 - k, v1 - k);
        int hl = City.lighten(col, 0.3f);
        f.line(hl, 0.7f, u0, v0, au, av);
        f.line(hl, 0.7f, u1, v0, bu, av);
        f.line(City.darken(col, 0.6f), 0.7f, u0, v1, au, bv);
        f.line(City.darken(col, 0.6f), 0.7f, u1, v1, bu, bv);
        f.line(hl, 1f, au, av, bu, bv);
    }

    /** A gabled roof with its ridge along u; courses adds tile lines down the slopes. */
    static void gable(F f, int col, float u0, float v0, float u1, float v1, float step) {
        float m = (v0 + v1) / 2;
        f.rect(City.lighten(col, 0.1f), u0, v0, u1, m);
        f.rect(City.darken(col, 0.74f), u0, m, u1, v1);
        if (step > 0) {
            for (float v = v0 + step; v < m - 0.5f; v += step) f.line(0x22000000, 0.5f, u0, v, u1, v);
            for (float v = m + step; v < v1 - 0.5f; v += step) f.line(0x26000000, 0.5f, u0, v, u1, v);
        }
        f.line(City.lighten(col, 0.35f), 1f, u0, m, u1, m);
        f.frame(City.darken(col, 0.6f), 0.6f, u0, v0, u1, v1);
    }

    /** A gabled roof with its ridge along v. */
    static void gableV(F f, int col, float u0, float v0, float u1, float v1, float step) {
        float m = (u0 + u1) / 2;
        f.rect(City.lighten(col, 0.1f), u0, v0, m, v1);
        f.rect(City.darken(col, 0.78f), m, v0, u1, v1);
        if (step > 0) {
            for (float u = u0 + step; u < m - 0.5f; u += step) f.line(0x22000000, 0.5f, u, v0, u, v1);
            for (float u = m + step; u < u1 - 0.5f; u += step) f.line(0x26000000, 0.5f, u, v0, u, v1);
        }
        f.line(City.lighten(col, 0.35f), 1f, m, v0, m, v1);
        f.frame(City.darken(col, 0.6f), 0.6f, u0, v0, u1, v1);
    }

    /** A dome from above: gores shaded round from the lit top left. */
    static void dome(F f, float u, float v, float r, int col, int n) {
        f.circle(0x40000000, u + r * 0.25f, v + r * 0.3f, r);
        for (int k = 0; k < n; k++) {
            double a0 = k * 2 * Math.PI / n, a1 = (k + 1) * 2 * Math.PI / n, am = (a0 + a1) / 2;
            float sh = 1f + 0.3f * (float) Math.cos(am + 3 * Math.PI / 4);
            f.poly(Landmarks.shade(col, sh), u, v, u + (float) Math.cos(a0) * r, v + (float) Math.sin(a0) * r,
                    u + (float) Math.cos(a1) * r, v + (float) Math.sin(a1) * r);
        }
    }

    /**
     * A Chinese hipped roof of glazed tiles: rolls of tile running down every face, a heavy ridge with end ornaments
     * and the corners swept up and out past the eaves.
     */
    static void chRoof(F f, int col, int ridge, float u0, float v0, float u1, float v1) {
        float h = Math.min(u1 - u0, v1 - v0) / 2, au = u0 + h, av = v0 + h, bu = u1 - h, bv = v1 - h;
        float e = Math.min(3f, h * 0.35f);
        // Swept-up corner tips.
        f.poly(City.darken(col, 0.7f), u0, v0 + e, u0 - e * 0.9f, v0 - e * 0.9f, u0 + e, v0);
        f.poly(City.darken(col, 0.6f), u1 - e, v0, u1 + e * 0.9f, v0 - e * 0.9f, u1, v0 + e);
        f.poly(City.darken(col, 0.6f), u0, v1 - e, u0 - e * 0.9f, v1 + e * 0.9f, u0 + e, v1);
        f.poly(City.darken(col, 0.5f), u1 - e, v1, u1 + e * 0.9f, v1 + e * 0.9f, u1, v1 - e);
        f.poly(City.lighten(col, 0.12f), u0, v0, u1, v0, bu, av, au, av);
        f.poly(col, u0, v0, u0, v1, au, bv, au, av);
        f.poly(City.darken(col, 0.82f), u1, v0, u1, v1, bu, bv, bu, av);
        f.poly(City.darken(col, 0.7f), u0, v1, u1, v1, bu, bv, au, bv);
        // Tile rolls down each face.
        int dk = 0x30000000;
        float st = 2.2f;
        for (float u = u0 + st; u < u1 - 0.5f; u += st) {
            float vt = av - Math.max(0, Math.max(au - u, u - bu));
            if (vt > v0 + 0.4f) f.line(dk, 0.45f, u, v0 + 0.3f, u, vt);
            float vb = bv + Math.max(0, Math.max(au - u, u - bu));
            if (vb < v1 - 0.4f) f.line(dk, 0.45f, u, vb, u, v1 - 0.3f);
        }
        for (float v = v0 + st; v < v1 - 0.5f; v += st) {
            float ul = au - Math.max(0, Math.max(av - v, v - bv));
            if (ul > u0 + 0.4f) f.line(dk, 0.45f, u0 + 0.3f, v, ul, v);
            float ur = bu + Math.max(0, Math.max(av - v, v - bv));
            if (ur < u1 - 0.4f) f.line(dk, 0.45f, ur, v, u1 - 0.3f, v);
        }
        // Eave line and hips.
        f.frame(City.darken(col, 0.55f), 0.7f, u0, v0, u1, v1);
        f.line(ridge, 1f, u0, v0, au, av);
        f.line(ridge, 1f, u1, v0, bu, av);
        f.line(City.darken(ridge, 0.7f), 1f, u0, v1, au, bv);
        f.line(City.darken(ridge, 0.7f), 1f, u1, v1, bu, bv);
        f.line(0x40000000, 2f, au + 0.6f, av + 0.8f, bu + 0.6f, bv + 0.8f);
        f.line(ridge, 1.6f, au, av, bu, bv);
        f.circle(ridge, au, av, 1.3f);
        f.circle(ridge, bu, bv, 1.3f);
        f.circle(City.lighten(ridge, 0.3f), (au + bu) / 2, (av + bv) / 2, 1.1f);
    }

    /** A striped awning over the rectangle, stripes running across v (it projects along v towards v1). */
    static void awning(F f, float u0, float v0, float u1, float v1, int a, int b, float sw) {
        f.rect(0x38000000, u0 + 0.8f, v0 + 1.2f, u1 + 0.8f, v1 + 1.2f);
        int k = 0;
        for (float u = u0; u < u1 - 0.01f; u += sw, k++) f.rect(k % 2 == 0 ? a : b, u, v0, Math.min(u1, u + sw), v1);
        f.rect(0x28000000, u0, (v0 + v1) / 2, u1, v1);
        f.line(City.darken(a, 0.6f), 0.6f, u0, v1, u1, v1);
    }

    /** A round parasol with alternating gores. */
    static void parasol(F f, float u, float v, float r, int a, int b) {
        f.circle(0x38000000, u + r * 0.3f, v + r * 0.4f, r);
        for (int k = 0; k < 8; k++) {
            double a0 = k * Math.PI / 4, a1 = (k + 1) * Math.PI / 4;
            int col = k % 2 == 0 ? a : b;
            float sh = 1f + 0.18f * (float) Math.cos((a0 + a1) / 2 + 3 * Math.PI / 4);
            f.poly(Landmarks.shade(col, sh), u, v, u + (float) Math.cos(a0) * r, v + (float) Math.sin(a0) * r,
                    u + (float) Math.cos(a1) * r, v + (float) Math.sin(a1) * r);
        }
        f.circle(City.darken(a, 0.6f), u, v, 0.6f);
    }

    /** A small round café table with chairs round it. */
    static void cafeTable(F f, float u, float v, int top, int chair, int n) {
        for (int k = 0; k < n; k++) {
            double a = k * 2 * Math.PI / n + 0.4;
            f.circle(chair, u + (float) Math.cos(a) * 2.6f, v + (float) Math.sin(a) * 2.6f, 0.9f);
        }
        f.circle(0x30000000, u + 0.5f, v + 0.6f, 1.7f);
        f.circle(top, u, v, 1.6f);
        f.circle(City.lighten(top, 0.25f), u - 0.4f, v - 0.4f, 0.6f);
    }

    /** A paper lantern (red by default) seen from above. */
    static void lantern(F f, float u, float v, float r, int col) {
        f.circle(0x30000000, u + r * 0.4f, v + r * 0.6f, r);
        f.circle(City.darken(col, 0.75f), u, v, r);
        f.circle(col, u - r * 0.15f, v - r * 0.15f, r * 0.75f);
        f.circle(0xFFE8C050, u, v, r * 0.3f);
    }

    /** A string of hanging things from (u0,v0) to (u1,v1): the wire and a bead every step, colours in turn. */
    static void string(F f, float u0, float v0, float u1, float v1, int[] cols, float step, float s) {
        f.line(0x90303030, 0.35f, u0, v0, u1, v1);
        float len = (float) Math.hypot(u1 - u0, v1 - v0);
        int n = Math.max(1, (int) (len / step));
        for (int k = 1; k < n; k++) {
            float t = k / (float) n;
            f.circle(cols[k % cols.length], u0 + (u1 - u0) * t, v0 + (v1 - v0) * t, s);
        }
    }

    /** A rooftop plant box with its fan, and a shadow. */
    static void hvac(F f, float u, float v, float w, float h) {
        f.rect(0x40000000, u + 1, v + 1.4f, u + w + 1, v + h + 1.4f);
        f.rect(0xFF8E9298, u, v, u + w, v + h);
        f.rect(0xFFA8ACB2, u, v, u + w, v + 0.8f);
        float rr = Math.min(w, h) * 0.32f;
        f.circle(0xFF5A5E64, u + w - rr - 0.8f, v + h / 2, rr);
        f.circle(0xFF7A7E84, u + w - rr - 0.8f, v + h / 2, rr * 0.4f);
        for (float x = u + 1; x < u + w - 2 * rr - 1; x += 1.2f) f.line(0x30000000, 0.4f, x, v + 1.2f, x, v + h - 0.6f);
    }

    /** A wooden crate of produce: the box, its slats and the fruit heaped in it. */
    static void crate(F f, Random r, float u, float v, float w, float h, int box, int fruit) {
        f.rect(0x38000000, u + 0.6f, v + 0.8f, u + w + 0.6f, v + h + 0.8f);
        f.rect(box, u, v, u + w, v + h);
        f.rect(City.darken(fruit, 0.8f), u + 0.5f, v + 0.5f, u + w - 0.5f, v + h - 0.5f);
        for (int k = 0; k < 5; k++)
            f.circle(k % 2 == 0 ? fruit : City.lighten(fruit, 0.15f), u + 0.9f + r.nextFloat() * (w - 1.8f), v + 0.9f + r.nextFloat() * (h - 1.8f), Math.min(w, h) * 0.2f);
    }

    /** A picnic table with its two benches, lying along u (or across). */
    static void picnic(F f, float u, float v, boolean alongU, int wood) {
        float l = 4.5f, w = 1.6f;
        if (alongU) {
            f.rect(0x30000000, u - l + 0.8f, v - w - 2 + 1, u + l + 0.8f, v + w + 2 + 1);
            f.rect(City.darken(wood, 0.8f), u - l, v - w - 2, u + l, v - w - 1);
            f.rect(City.darken(wood, 0.8f), u - l, v + w + 1, u + l, v + w + 2);
            f.rect(wood, u - l, v - w, u + l, v + w);
            f.line(0x30000000, 0.4f, u - l, v, u + l, v);
        } else {
            f.rect(0x30000000, u - w - 2 + 0.8f, v - l + 1, u + w + 2 + 0.8f, v + l + 1);
            f.rect(City.darken(wood, 0.8f), u - w - 2, v - l, u - w - 1, v + l);
            f.rect(City.darken(wood, 0.8f), u + w + 1, v - l, u + w + 2, v + l);
            f.rect(wood, u - w, v - l, u + w, v + l);
            f.line(0x30000000, 0.4f, u, v - l, u, v + l);
        }
    }

    /** A potted shrub or small tree in a round tub; fruit adds dots of colour. */
    static void pot(F f, float u, float v, float r, int fruit) {
        f.circle(0x38000000, u + 0.8f, v + 1, r + 0.6f);
        f.circle(0xFF9A5A3A, u, v, r + 0.6f);
        f.circle(0xFF4E7A3A, u, v, r);
        f.circle(0xFF64944A, u - r * 0.3f, v - r * 0.3f, r * 0.5f);
        if (fruit != 0) {
            f.circle(fruit, u + r * 0.4f, v - r * 0.1f, 0.6f);
            f.circle(fruit, u - r * 0.3f, v + r * 0.4f, 0.6f);
            f.circle(fruit, u + r * 0.05f, v - r * 0.5f, 0.6f);
        }
    }

    /** A chimney stack with pots. */
    static void chimney(F f, float u, float v, int pots, int brick) {
        float w = 1.8f + pots * 1.8f;
        f.rect(0x48000000, u + 1, v + 1.4f, u + w + 1, v + 3.4f + 1.4f);
        f.rect(brick, u, v, u + w, v + 3.4f);
        f.rect(City.lighten(brick, 0.15f), u, v, u + w, v + 1);
        for (int k = 0; k < pots; k++) {
            f.circle(0xFFB86A48, u + 1.8f + k * 1.8f, v + 1.7f, 0.8f);
            f.circle(0xFF2A2020, u + 1.8f + k * 1.8f, v + 1.7f, 0.4f);
        }
    }

    /** A grid of glass panes (a skylight or a glazed roof) with glints. */
    static void glass(F f, float u0, float v0, float u1, float v1, int col, float step) {
        f.rect(City.darken(col, 0.6f), u0, v0, u1, v1);
        f.rect(col, u0 + 0.6f, v0 + 0.6f, u1 - 0.6f, v1 - 0.6f);
        f.poly(City.lighten(col, 0.15f), u0 + 0.6f, v0 + 0.6f, u0 + (u1 - u0) * 0.5f, v0 + 0.6f, u0 + 0.6f, v0 + (v1 - v0) * 0.6f);
        for (float u = u0 + step; u < u1 - 0.3f; u += step) f.line(0x50FFFFFF, 0.4f, u, v0 + 0.6f, u, v1 - 0.6f);
        for (float v = v0 + step; v < v1 - 0.3f; v += step) f.line(0x40000000, 0.4f, u0 + 0.6f, v, u1 - 0.6f, v);
    }

    /** A stone guardian lion on its plinth, facing along +v (dir 1) or -v (dir -1). */
    static void lion(F f, float u, float v, float dir) {
        f.rect(0x40000000, u - 3 + 1, v - 3 + 1.4f, u + 3 + 1, v + 3 + 1.4f);
        f.rect(0xFF8E8A82, u - 3, v - 3, u + 3, v + 3);
        f.rect(0xFFA8A49C, u - 2.6f, v - 2.6f, u + 2.6f, v + 2.6f);
        f.oval(0xFF6E6A62, u - 1.6f, v - 2.2f * dir - 0.1f, u + 1.6f, v + 1.4f * dir);
        f.oval(0xFF7E7A70, u - 1.6f, Math.min(v - 2.2f * dir, v + 1.4f * dir), u + 1.6f, Math.max(v - 2.2f * dir, v + 1.4f * dir));
        f.circle(0xFF6A665E, u, v + 1.4f * dir, 1.5f);
        for (int k = 0; k < 5; k++) f.circle(0xFF5A564E, u - 1.4f + k * 0.7f, v + 1.4f * dir - 1.2f * dir, 0.45f);
        f.circle(0xFF8E8A80, u - 1.2f, v - 0.6f * dir, 0.7f);
    }

    /** A chestnut tree: a broad canopy speckled with white flower candles. */
    static void chestnut(F f, Random r, float u, float v, float rad) {
        f.tree(u, v, rad, 0xFF3A6E34);
        for (int k = 0; k < 7; k++) {
            double a = r.nextFloat() * Math.PI * 2;
            float d = r.nextFloat() * rad * 0.7f;
            f.circle(0xFFEDE6D0, u - rad * 0.1f + (float) Math.cos(a) * d, v - rad * 0.1f + (float) Math.sin(a) * d, 0.55f);
        }
    }

    /** A blossom tree (plum or cherry): pink canopy, darker inside. */
    static void blossom(F f, float u, float v, float rad) {
        f.circle(0x38000000, u + rad * 0.35f, v + rad * 0.4f, rad);
        f.circle(0xFFB86A84, u, v, rad);
        f.circle(0xFFD88AA4, u - rad * 0.12f, v - rad * 0.12f, rad * 0.78f);
        f.circle(0xFFF0B8CA, u - rad * 0.35f, v - rad * 0.35f, rad * 0.35f);
        f.circle(0xFFFFFFFF, u + rad * 0.3f, v - rad * 0.2f, 0.5f);
        f.circle(0xFFFFFFFF, u - rad * 0.2f, v + rad * 0.35f, 0.5f);
    }

    // ------------------------------------------------------------------ 1 Chinatown Gate

    static void chinatownGate(List<Landmarks.Def> out) {
        // A street plaza through a paifang gate: red pillars, green-tiled roofs, stone lions, lantern strings overhead.
        D d = new D("Chinatown Gate", T_MONUMENT) {
            @Override
            int[][] lp(int L, int S) {
                int pw = Math.max(3, Math.min(5, L / 5)), ph = S >= 13 ? 3 : 2;
                return new int[][]{B(1, 1, pw, ph, 2), B(L - 1 - pw, 1, pw, ph, 2), B(1, S - 1 - ph, pw, ph, 2), B(L - 1 - pw, S - 1 - ph, pw, ph, 2)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float s0 = S * 0.34f, s1 = S * 0.66f, uc = L / 2;
                // Granite paving with a slab grid.
                f.rect(0xFFB2ACA2, 0, 0, L, S);
                for (float u = 4; u < L; u += 6) f.line(0x18000000, 0.4f, u, 0, u, S);
                for (float v = 4; v < S; v += 6) f.line(0x18000000, 0.4f, 0, v, L, v);
                f.specks(r, 40, 0x14000000, 0x14FFFFFF, 0, 0, L, S, 2.5f);
                // The street: red brick in courses, granite kerbs, a gold-ringed roundel each side of the gate.
                f.rect(0xFF9A4E3E, 0, s0, L, s1);
                for (float v = s0 + 2; v < s1; v += 2) f.line(0x22000000, 0.4f, 0, v, L, v);
                for (float u = 0, k = 0; u < L; u += 4, k++)
                    for (float v = s0 + (k % 2) * 2; v < s1; v += 4) f.line(0x1A000000, 0.4f, u, v, u, v + 2);
                f.rect(0xFFD2CCC0, 0, s0 - 1.2f, L, s0);
                f.rect(0xFFD2CCC0, 0, s1, L, s1 + 1.2f);
                float rr = (s1 - s0) * 0.32f;
                for (int k = -1; k <= 1; k += 2) {
                    float cu = uc + k * L * 0.27f, cv = S / 2;
                    f.circle(0xFFC8A040, cu, cv, rr);
                    f.circle(0xFFA8382C, cu, cv, rr - 1.2f);
                    f.circle(0xFFC8A040, cu, cv, rr * 0.45f);
                    for (int j = 0; j < 8; j++) {
                        double a = j * Math.PI / 4;
                        f.line(0xFFC8A040, 0.7f, cu + (float) Math.cos(a) * rr * 0.45f, cv + (float) Math.sin(a) * rr * 0.45f,
                                cu + (float) Math.cos(a) * (rr - 1.2f), cv + (float) Math.sin(a) * (rr - 1.2f));
                    }
                    f.circle(0xFF8A2A22, cu, cv, rr * 0.25f);
                }
                // Blossom trees in pits along both kerbs, red lantern posts between.
                float pe = q[0][2] * T + 4;
                for (float u = 10; u < L - 6; u += 22) {
                    if (Math.abs(u - uc) < 16 || u < pe || u > L - pe) continue;
                    for (int s = 0; s < 2; s++) {
                        float v = s == 0 ? s0 - 6 : s1 + 6;
                        f.rect(0xFF6E5A44, u - 3, v - 3, u + 3, v + 3);
                        blossom(f, u, v, 5);
                    }
                }
                for (float u = 21; u < L - 6; u += 22) {
                    if (Math.abs(u - uc) < 16) continue;
                    f.circle(0xFF8A2A22, u, s0 - 4, 1.1f);
                    lantern(f, u, s0 - 4, 1.4f, 0xFFD03A2A);
                    f.circle(0xFF8A2A22, u, s1 + 4, 1.1f);
                    lantern(f, u, s1 + 4, 1.4f, 0xFFD03A2A);
                }
                // Shop fronts: a red step and a pair of potted kumquats at each pavilion's door.
                for (int[] b : q) {
                    float u0 = b[0] * T, u1 = u0 + b[2] * T, v0 = b[1] * T, v1 = v0 + b[3] * T;
                    boolean top = v0 < S / 2;
                    float dv = top ? v1 : v0;
                    float sg = top ? 1 : -1;
                    float mu = (u0 + u1) / 2;
                    f.rect(0xFFB0402E, mu - 5, Math.min(dv, dv + sg * 3), mu + 5, Math.max(dv, dv + sg * 3));
                    pot(f, mu - 8, dv + sg * 3, 2.2f, 0xFFF0A030);
                    pot(f, mu + 8, dv + sg * 3, 2.2f, 0xFFF0A030);
                }
                // Lantern strings across the street.
                int[] lc = {0xFFD8382A, 0xFFE04030, 0xFFC83024};
                for (float u = 8; u < L - 4; u += 16) {
                    if (Math.abs(u - uc) < 16) continue;
                    f.line(0x90303030, 0.35f, u, s0 - 2, u + 4, s1 + 2);
                    for (int k = 1; k < 5; k++) {
                        float t = k / 5f;
                        lantern(f, u + 4 * t, s0 - 2 + (s1 - s0 + 4) * t, 1.9f, lc[k % 3]);
                    }
                }
                // The gate: its shadow, four red pillars on stone bases, the side roofs and the high middle roof.
                float g0 = S * 0.12f, g1 = S * 0.88f, gw = 9;
                f.poly(0x40000000, uc - gw + 4, g0 + 5, uc + gw + 6, g0 + 5, uc + gw + 6, g1 + 6, uc - gw + 4, g1 + 6);
                float[] pv = {g0 + 4, s0 + 2, s1 - 2, g1 - 4};
                for (float v : pv) {
                    f.rect(0xFFA8A296, uc - 3, v - 3, uc + 3, v + 3);
                    f.circle(0xFFB0302A, uc, v, 2);
                    f.circle(0xFFD04A3A, uc - 0.5f, v - 0.5f, 1);
                }
                f.rect(0xFFB0302A, uc - 1, g0 + 4, uc + 1, g1 - 4);
                chRoof(f, 0xFF3E8A62, 0xFFD8B040, uc - gw + 2, g0, uc + gw - 2, s0 + 1);
                chRoof(f, 0xFF3E8A62, 0xFFD8B040, uc - gw + 2, s1 - 1, uc + gw - 2, g1);
                f.rect(0x50000000, uc - gw + 1, s0 - 2, uc + gw + 2, s1 + 3);
                chRoof(f, 0xFF468E68, 0xFFE0B848, uc - gw - 1.5f, s0 - 4, uc + gw + 1.5f, s1 + 4);
                // Upper tier: a short top roof with a flaming pearl on its ridge.
                f.rect(0x50000000, uc - gw * 0.5f + 1, S / 2 - (s1 - s0) * 0.3f + 1.5f, uc + gw * 0.5f + 2, S / 2 + (s1 - s0) * 0.3f + 2);
                chRoof(f, 0xFF52A074, 0xFFE8C050, uc - gw * 0.55f, S / 2 - (s1 - s0) * 0.3f, uc + gw * 0.55f, S / 2 + (s1 - s0) * 0.3f);
                f.circle(0xFFE04030, uc, S / 2, 1.8f);
                f.circle(0xFFF4D870, uc - 0.4f, S / 2 - 0.4f, 0.8f);
                // The name board hung under the middle roof's front eave.
                f.rect(0xFFC8A040, uc + gw + 1.5f, S / 2 - 5, uc + gw + 3.4f, S / 2 + 5);
                f.rect(0xFF9A2A22, uc + gw + 1.9f, S / 2 - 4.4f, uc + gw + 3f, S / 2 + 4.4f);
                // Guardian lions either side of the street before the gate.
                lion(f, uc + gw + 7, s0 - 4, 1);
                lion(f, uc + gw + 7, s1 + 4, -1);
                lion(f, uc - gw - 7, s0 - 4, 1);
                lion(f, uc - gw - 7, s1 + 4, -1);
                f.sign(city, "CHINATOWN", uc, S / 2, 5, 0xFFF4D870);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                f.rect(0xFFA8302A, 0, 0, L, S);
                chRoof(f, part % 2 == 0 ? 0xFF3E8A62 : 0xFFB8502E, 0xFFD8B040, 1.4f, 1.4f, L - 1.4f, S - 1.4f);
                lantern(f, 0.8f, S - 0.8f, 1.3f, 0xFFD8382A);
                lantern(f, L - 0.8f, S - 0.8f, 1.3f, 0xFFD8382A);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.MID | Variants.OLD;
        d.minS = 10;
        d.minL = 14;
        d.base = City.SHOP;
        d.inside = Variants.I_SHELVES;
        d.roofCol = 0xFF3E8A62;
        out.add(d);
    }

    // ------------------------------------------------------------------ 2 Dim Sum Palace

    static void dimSum(List<Landmarks.Def> out) {
        // A red and gold banquet hall under a double-eaved green tile roof, its forecourt of lions, kumquats and a koi pool.
        D d = new D("Dim Sum Palace", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                return new int[][]{B(1, 1, L - 2, Math.max(4, S * 52 / 100), 3)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float fv = (q[0][1] + q[0][3]) * T, uc = L / 2;
                f.rect(0xFFBCB2A2, 0, 0, L, S);
                for (float u = 0, k = 0; u < L; u += 5, k++)
                    for (float v = fv, j = 0; v < S; v += 5, j++)
                        f.rect((k + j) % 2 == 0 ? 0xFFC4BAA8 : 0xFFB4AA98, u, v, u + 5, v + 5);
                f.rect(0xFF8A9A6A, 0, 0, L, T * 0.8f);
                // A red-carpeted walk from the doors with gold edging, lantern posts along it.
                f.rect(0xFFC8A040, uc - 7, fv, uc + 7, S);
                f.rect(0xFFA82A26, uc - 6, fv, uc + 6, S);
                for (float v = fv + 3; v < S; v += 3) f.line(0x22000000, 0.4f, uc - 6, v, uc + 6, v);
                for (float v = fv + 8; v < S - 3; v += 12) {
                    f.circle(0xFF3A2A22, uc - 10, v, 1);
                    lantern(f, uc - 10, v, 1.7f, 0xFFD8382A);
                    f.circle(0xFF3A2A22, uc + 10, v, 1);
                    lantern(f, uc + 10, v, 1.7f, 0xFFD8382A);
                }
                lion(f, uc - 15, fv + 5, 1);
                lion(f, uc + 15, fv + 5, 1);
                // A koi pool with lotus on one side, kumquat tubs in rows on the other.
                float pu0 = 6, pu1 = uc - 22, pv0 = fv + 6, pv1 = S - 6;
                if (pu1 - pu0 > 12 && pv1 - pv0 > 10) {
                    f.roundRect(0xFFA89E8A, pu0 - 2, pv0 - 2, pu1 + 2, pv1 + 2, 4);
                    f.roundRect(0xFF2E6A84, pu0, pv0, pu1, pv1, 3);
                    f.roundRect(0xFF3A7E98, pu0 + 2, pv0 + 2, pu1 - 2, pv1 - 2, 2);
                    for (int k = 0; k < 6; k++) {
                        float u = pu0 + 3 + r.nextFloat() * (pu1 - pu0 - 6), v = pv0 + 3 + r.nextFloat() * (pv1 - pv0 - 6);
                        f.circle(0xFF4E8A4A, u, v, 1.8f);
                        f.circle(0xFF66A456, u - 0.4f, v - 0.4f, 0.9f);
                        if (k % 2 == 0) f.circle(0xFFF0A8C0, u + 0.6f, v - 0.6f, 0.7f);
                    }
                    for (int k = 0; k < 5; k++) {
                        float u = pu0 + 4 + r.nextFloat() * (pu1 - pu0 - 8), v = pv0 + 3 + r.nextFloat() * (pv1 - pv0 - 6);
                        f.oval(k % 2 == 0 ? 0xFFF08A3A : 0xFFF4F0E8, u - 1.4f, v - 0.5f, u + 1.4f, v + 0.5f);
                    }
                    // A little stone bridge across it.
                    float bu = (pu0 + pu1) / 2;
                    f.rect(0x40000000, bu - 2, pv0 - 2, bu + 4, pv1 + 3);
                    f.rect(0xFFC8C0B0, bu - 3, pv0 - 3, bu + 3, pv1 + 3);
                    for (float v = pv0 - 2; v < pv1 + 3; v += 2) f.line(0x26000000, 0.4f, bu - 3, v, bu + 3, v);
                }
                for (float u = uc + 24; u < L - 4; u += 20)
                    for (float v = fv + 9; v < S - 3; v += 18) {
                        f.circle(0xFF8A7E6A, u, v, 6.5f);
                        f.circle(0xFF6E8A4A, u, v, 5.5f);
                        pot(f, u, v, 3f, 0xFFF0A030);
                    }
                f.sign(city, "DIM SUM", uc, fv + (S - fv) / 2, 5, 0xFFF4D870);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                // Red walls under a deep green lower eave, then the raised upper roof and its gold ridge.
                f.rect(0xFF9A2A24, 0, 0, L, S);
                chRoof(f, 0xFF2E7A56, 0xFFD8B040, 1, 1, L - 1, S - 1);
                float in = S * 0.22f;
                f.rect(0x50000000, in + 1, in + 1.5f, L - in + 1.5f, S - in + 2);
                f.rect(0xFFA8302A, in - 0.5f, in - 0.5f, L - in + 0.5f, S - in + 0.5f);
                for (float u = in + 3; u < L - in - 1; u += 5) f.rect(0xFFD8B040, u, S - in - 0.2f, u + 1.4f, S - in + 0.6f);
                chRoof(f, 0xFF3A8A62, 0xFFE0B848, in + 1, in + 1, L - in - 1, S - in - 1);
                // A gold board over the doors on the front eave and lanterns at the corners.
                f.rect(0xFFD8B040, L / 2 - 9, S - 3, L / 2 + 9, S);
                f.rect(0xFFA8302A, L / 2 - 8, S - 2.4f, L / 2 + 8, S - 0.6f);
                lantern(f, 2, S - 1, 1.8f, 0xFFD8382A);
                lantern(f, L - 2, S - 1, 1.8f, 0xFFD8382A);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.MID | Variants.OLD;
        d.minS = 10;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_RESTAURANT;
        d.roofCol = 0xFF2E7A56;
        out.add(d);
    }

    // ------------------------------------------------------------------ 3 Asian Supermarket

    static void asianMarket(List<Landmarks.Def> out) {
        // A big flat-roofed grocery with a red fascia, its front heaped with crates of fruit and greens under awnings.
        D d = new D("Asian Supermarket", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                return new int[][]{B(1, 1, L - 2, Math.max(4, S - 5), 1)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float fv = (q[0][1] + q[0][3]) * T;
                f.rect(0xFFB4B0A8, 0, 0, L, S);
                f.rect(0xFF7A7C80, 0, 0, L, T * 0.9f);
                for (float u = 6; u < L; u += 8) f.line(0x30FFFFFF, 0.5f, u, 3, u + 3, 3);
                // Forecourt paving and a tiled shopfront apron.
                for (float u = 0; u < L; u += 4) f.line(0x16000000, 0.4f, u, fv, u, S);
                f.rect(0xFFC8C2B4, 0, fv, L, fv + 4);
                // Awnings over the produce stands, crates heaped beneath.
                int[] fr = {0xFF4E9A3A, 0xFFE07A2A, 0xFFD43A4A, 0xFFE8C83A, 0xFF7AB04A, 0xFF8A3A6A, 0xFFF0A040, 0xFF5AA850};
                float aw = Math.min(54, L * 0.42f);
                awning(f, 4, fv, 4 + aw, fv + 5, 0xFFC8302A, 0xFFF0E8D8, 3);
                awning(f, L - 4 - aw, fv, L - 4, fv + 5, 0xFFC8302A, 0xFFF0E8D8, 3);
                int k = 0;
                for (int s = 0; s < 2; s++) {
                    float a = s == 0 ? 5 : L - 3 - aw;
                    for (float u = a; u + 5 < a + aw; u += 5.5f, k++) {
                        crate(f, r, u, fv + 5.6f, 5, 4, 0xFF9A7A50, fr[k % fr.length]);
                        crate(f, r, u, fv + 10, 5, 4, 0xFF9A7A50, fr[(k + 3) % fr.length]);
                    }
                }
                // Durian, melons and a sack stack by the doors, trolleys corralled, a few bikes.
                float uc = L / 2;
                f.rect(0xFF3A3C40, uc - 8, fv, uc + 8, fv + 1.2f);
                for (int j = 0; j < 4; j++) {
                    float u = uc - 6 + j * 4;
                    f.circle(0x30000000, u + 0.6f, fv + 7.6f, 1.6f);
                    f.circle(0xFF9AA04A, u, fv + 7, 1.6f);
                    for (int m = 0; m < 6; m++) f.circle(0xFF6E7430, u - 1 + (m % 3), fv + 6.2f + (m / 3) * 1.4f, 0.3f);
                }
                // The trolley corral in the middle of the forecourt.
                float tv = fv + 17;
                if (tv + 8 < S) {
                    f.rect(0xFF8A8E94, uc - 13, tv - 1, uc + 13, tv + 7);
                    f.rect(0xFFC4C0B6, uc - 12, tv, uc + 12, tv + 6);
                    for (float u = uc - 11; u < uc + 10.5f; u += 2.2f) {
                        f.rect(0xFF8E969E, u, tv + 0.8f, u + 1.8f, tv + 5.2f);
                        f.rect(0xFFC83A2A, u, tv + 0.8f, u + 1.8f, tv + 1.5f);
                    }
                }
                // Bamboo planters along the street edge, and a delivery van at the side door.
                for (float u = 6; u < L - 6; u += 18) {
                    if (Math.abs(u - uc) < 16) continue;
                    f.rect(0x38000000, u - 6 + 1, S - 7 + 1.2f, u + 6 + 1, S - 3 + 1.2f);
                    f.rect(0xFF6A6460, u - 6, S - 7, u + 6, S - 3);
                    for (int j = 0; j < 6; j++) {
                        float bu = u - 5 + j * 2;
                        f.circle(0xFF5A8A3A, bu, S - 5 + (j % 2) * 0.6f, 1.6f);
                        f.circle(0xFF7AAA4A, bu - 0.4f, S - 5.4f + (j % 2) * 0.6f, 0.8f);
                    }
                }
                if (S - fv > 34) {
                    float vu = L - 14, vv = fv + 26;
                    f.roundRect(0x48000000, vu - 9 + 1.5f, vv - 4 + 2, vu + 9 + 1.5f, vv + 4 + 2, 1.5f);
                    f.roundRect(0xFFE8E8E4, vu - 9, vv - 4, vu + 9, vv + 4, 1.5f);
                    f.rect(0xFF2A333C, vu + 5, vv - 3.2f, vu + 7.2f, vv + 3.2f);
                    f.rect(0xFFC8302A, vu - 8, vv - 1, vu + 4, vv + 1);
                }
                for (float u = 8; u < L - 4; u += 28) f.lamp(u, S - 1.5f);
                f.sign(city, "ASIA MART", L / 2, fv + 2, 4.5f, 0xFFF4D870);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                f.rect(0xFF8A8C90, 0, 0, L, S);
                f.rect(0xFFC4C6C4, 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                f.specks(r, 40, 0x14000000, 0x18FFFFFF, 2, 2, L - 2, S - 5, 1);
                // The red fascia along the front with gold lettering band.
                f.rect(0xFFB8282A, 0, S - 4, L, S);
                f.rect(0xFFE8C040, 3, S - 2.6f, L - 3, S - 1.6f);
                for (float u = 6; u < L - 4; u += 7) f.rect(0xFFB8282A, u, S - 2.6f, u + 2, S - 1.6f);
                // Skylight rows, the plant deck and ducts.
                for (float u = 7; u + 12 < L * 0.64f; u += 20)
                    for (float v = 6; v + 12 < S - 6; v += 20) {
                        f.rect(0x30000000, u + 1, v + 1.5f, u + 13, v + 13.5f);
                        glass(f, u, v, u + 12, v + 12, 0xFF8AAAC0, 3);
                    }
                float hu = L * 0.68f;
                f.rect(0xFFAEB0B0, hu, 4, L - 4, S - 8);
                for (float u = hu + 2, k = 0; u + 7 < L - 5; u += 9, k++) hvac(f, u, 6 + (k % 2) * 6, 7, 5);
                f.rect(0xFF8E9296, hu - 2, S * 0.4f, hu, S - 6);
                f.rect(0xFF8E9296, 6, S * 0.4f, hu, S * 0.4f + 1.6f);
                f.sign(city, "ASIA MART", L / 2, S / 2, 5, 0xFFF0E8D0);
            }
        };
        d.ground = City.LOT;
        d.districts = Variants.MID | Variants.SUB | Variants.DOWN;
        d.minS = 10;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_SHELVES;
        d.roofCol = 0xFFC4C6C4;
        out.add(d);
    }

    // ------------------------------------------------------------------ 4 Little Italy Deli Row

    static void deliRow(List<Landmarks.Def> out) {
        // A terrace of delis and cafés: each its own roof, striped awnings, tables with check cloths out on the cobbles.
        D d = new D("Little Italy Deli Row", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                return new int[][]{B(1, 1, L - 2, Math.max(4, Math.min(7, S / 2)), 3)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float bu0 = q[0][0] * T, bu1 = bu0 + q[0][2] * T, fv = (q[0][1] + q[0][3]) * T;
                // Back lane, then warm stone cobbles out front.
                f.rect(0xFF8A8478, 0, 0, L, S);
                f.rect(0xFFCDBFA4, 0, fv, L, S);
                for (float v = fv + 2.5f; v < S; v += 2.5f) f.line(0x18000000, 0.4f, 0, v, L, v);
                f.specks(r, 70, 0x16000000, 0x1CFFFFFF, 0, fv, L, S, 2);
                for (float u = 3; u < L - 3; u += 9) {
                    f.rect(0xFF5A5650, u, 3, u + 3, 7);
                    f.rect(0xFF3E6A3E, u + 4, 3, u + 7, 7);
                }
                int n = Math.max(3, Math.round((bu1 - bu0) / 40));
                float sw = (bu1 - bu0) / n;
                int[][] aw = {{0xFFB8302A, 0xFFF0E8DA}, {0xFF2E7040, 0xFFF0E8DA}, {0xFFE0D2B0, 0xFF7A4A2E}, {0xFF2E7040, 0xFFB8302A}};
                float dep = Math.min(6, (S - fv) * 0.18f);
                for (int k = 0; k < n; k++) {
                    float a = bu0 + k * sw, e = a + sw;
                    int[] c2 = aw[k % aw.length];
                    awning(f, a + 2, fv, e - 2, fv + dep, c2[0], c2[1], 2.4f);
                    // Tables in front of each shop: check cloths, chairs, a menu board.
                    for (float v = fv + dep + 7, row = 0; v < S - 12 && row < 3; v += 12, row++) {
                        for (float u = a + 7 + (row % 2) * 4; u < e - 5; u += 13) {
                            f.rect(0xFF4A3A2E, u - 2.8f, v - 0.5f, u - 1.8f, v + 0.5f);
                            f.rect(0xFF4A3A2E, u + 1.8f, v - 0.5f, u + 2.8f, v + 0.5f);
                            f.rect(0x30000000, u - 1.5f, v - 1.2f, u + 2.3f, v + 2.2f);
                            f.rect(0xFFF2EDE2, u - 1.8f, v - 1.8f, u + 1.8f, v + 1.8f);
                            for (int j = 0; j < 3; j++) {
                                f.rect(0xFFC0382E, u - 1.8f + j * 1.2f, v - 1.8f, u - 1.2f + j * 1.2f, v + 1.8f);
                                f.rect(0x70C0382E, u - 1.8f, v - 1.8f + j * 1.2f, u + 1.8f, v - 1.2f + j * 1.2f);
                            }
                        }
                    }
                    f.poly(0xFF3A2E26, e - 4, fv + dep + 1, e - 2, fv + dep + 1, e - 2.4f, fv + dep + 3, e - 3.6f, fv + dep + 3);
                    if (k < n - 1) pot(f, e, fv + dep + 2.5f, 2.2f, 0);
                }
                // Olive trees in tubs and a scooter at the end of the row; café lights strung across the front.
                pot(f, 4, S - 5, 3, 0xFF5A6A30);
                pot(f, L - 4, S - 5, 3, 0xFF5A6A30);
                f.roundRect(0x38000000, L - 18 + 0.8f, S - 5 + 1, L - 10 + 0.8f, S - 2.6f + 1, 1.2f);
                f.roundRect(0xFF7AB0A4, L - 18, S - 5, L - 10, S - 2.6f, 1.2f);
                f.circle(0xFF2A2A2A, L - 11, S - 3.8f, 0.9f);
                int[] bl = {0xFFF8E8A0, 0xFFF4D880};
                float tv = Math.min(S - 8, fv + dep + 7 + 3 * 12 - 4);
                for (float u = 10; u < L - 8; u += 16) pot(f, u, tv, 2.4f, 0);
                f.line(0xFF6A665E, 0.5f, 4, tv + 3.5f, L - 4, tv + 3.5f);
                for (float u = bu0 + 4; u < bu1 - 4; u += 24) string(f, u, fv + dep + 1, u + 12, tv - 1, bl, 3, 0.5f);
                for (float u = 20; u < L - 10; u += 40) f.lamp(u, tv + 6);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                int n = Math.max(3, Math.round(L / 40));
                float sw = L / n;
                int[] pal = {0xFFB8603E, 0xFFC8A070, 0xFFA85440, 0xFFD0B088};
                for (int k = 0; k < n; k++) {
                    float a = k * sw, e = a + sw;
                    int style = k % 3;
                    if (style == 0) {
                        // Terracotta pantiles, ridge along the row, a chimney.
                        gable(f, pal[k % 4], a, 0, e, S, 1.6f);
                        for (float u = a + 1.6f; u < e; u += 1.6f) f.line(0x18000000, 0.4f, u, 0, u, S);
                        chimney(f, e - 6, S * 0.2f, 2, 0xFF9A6A50);
                    } else if (style == 1) {
                        // A flat roof terrace: tiles, pots of herbs and a washing line.
                        f.rect(0xFF9A8A70, a, 0, e, S);
                        f.rect(0xFFC8B494, a + 1.2f, 1.2f, e - 1.2f, S - 1.2f);
                        for (float u = a + 3; u < e - 1; u += 3) f.line(0x1A000000, 0.4f, u, 1.2f, u, S - 1.2f);
                        for (float v = 3; v < S - 1; v += 3) f.line(0x1A000000, 0.4f, a + 1.2f, v, e - 1.2f, v);
                        for (float u = a + 4; u < e - 3; u += 5) pot(f, u, 3.5f, 1.5f, 0);
                        f.line(0xFF4A4A4A, 0.4f, a + 3, S * 0.6f, e - 3, S * 0.6f);
                        int[] cl = {0xFFF0F0F0, 0xFF5A7AB0, 0xFFE0C060, 0xFFC04A4A};
                        for (float u = a + 5, j = 0; u < e - 5; u += 4, j++)
                            f.rect(cl[(int) j % 4], u - 1.2f, S * 0.6f, u + 1.2f, S * 0.6f + 2.4f);
                        f.rect(0x40000000, e - 9 + 1, S - 8 + 1.2f, e - 4 + 1, S - 4 + 1.2f);
                        f.rect(0xFF6A5040, e - 9, S - 8, e - 4, S - 4);
                    } else {
                        // A hipped roof of old tiles with a skylight.
                        hip(f, pal[(k + 1) % 4], a, 0, e, S, true);
                        glass(f, a + sw * 0.3f, S * 0.6f, a + sw * 0.55f, S * 0.6f + 4, 0xFF8AAAC0, 2);
                    }
                    if (k > 0) f.rect(0xFF8A7A66, a - 0.6f, 0, a + 0.6f, S);
                }
                f.sign(city, "SALUMERIA", L / 2, S - 3, 4.5f, 0xFFF0E8D0);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.OLD | Variants.MID;
        d.minS = 10;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_CAFE;
        d.roofCol = 0xFFB8603E;
        out.add(d);
    }

    // ------------------------------------------------------------------ 5 Gelato Piazza

    static void gelato(List<Landmarks.Def> out) {
        // A pastel piazza: a gelateria with a giant cone on its roof, a café opposite, parasols and a little wall fountain.
        D d = new D("Gelato Piazza", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                int w = Math.max(4, Math.min(6, L * 3 / 10)), h = S >= 13 ? 4 : 3;
                return new int[][]{B(1, 1, w, h, 2), B(L - 1 - w, S - 1 - h, w, h, 3)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                // Travertine laid in a diagonal check of cream and peach.
                f.rect(0xFFE2D4BC, 0, 0, L, S);
                f.c.save();
                f.c.clipRect(f.x(0, 0), f.y(0, 0), f.x(L, S), f.y(L, S));
                float st = 9;
                for (float u = -S; u < L + S; u += st)
                    for (float v = 0, j = 0; v < S + st; v += st, j++)
                        if (((int) ((u + S) / st) + j) % 2 == 0)
                            f.poly(0xFFEAC8AC, u + v, v - st / 2, u + v + st / 2, v, u + v, v + st / 2, u + v - st / 2, v);
                f.c.restore();
                // The fountain in the middle: an octagonal basin, four spouting dolphins and a slim obelisk.
                float fu = L / 2, fv = S / 2, fr = Math.min(L, S) * 0.13f;
                for (int k = 0; k < 8; k++) {
                    double a0 = k * Math.PI / 4 + Math.PI / 8, a1 = a0 + Math.PI / 4;
                    f.poly(0xFFB4A68C, fu, fv, fu + (float) Math.cos(a0) * (fr + 2), fv + (float) Math.sin(a0) * (fr + 2),
                            fu + (float) Math.cos(a1) * (fr + 2), fv + (float) Math.sin(a1) * (fr + 2));
                    f.poly(0xFF4A90B0, fu, fv, fu + (float) Math.cos(a0) * fr, fv + (float) Math.sin(a0) * fr,
                            fu + (float) Math.cos(a1) * fr, fv + (float) Math.sin(a1) * fr);
                }
                f.circle(0xFF6AAACA, fu, fv, fr * 0.7f);
                for (int k = 0; k < 4; k++) {
                    double a = k * Math.PI / 2 + Math.PI / 4;
                    float du = (float) Math.cos(a), dv = (float) Math.sin(a);
                    f.line(0xA0FFFFFF, 0.8f, fu + du * 3, fv + dv * 3, fu + du * fr * 0.7f, fv + dv * fr * 0.7f);
                    f.circle(0x90FFFFFF, fu + du * fr * 0.72f, fv + dv * fr * 0.72f, 1);
                    f.circle(0xFF8A9A9A, fu + du * 3, fv + dv * 3, 1.3f);
                }
                f.rect(0x40000000, fu - 2 + 1, fv - 2 + 1.5f, fu + 2 + 4, fv + 2 + 4);
                f.rect(0xFFD8CCB4, fu - 2.2f, fv - 2.2f, fu + 2.2f, fv + 2.2f);
                f.poly(0xFFEDE4D0, fu - 2.2f, fv - 2.2f, fu + 2.2f, fv - 2.2f, fu, fv);
                f.poly(0xFFB8AC94, fu + 2.2f, fv - 2.2f, fu + 2.2f, fv + 2.2f, fu, fv);
                f.circle(0xFFC8A040, fu, fv, 0.6f);
                // Pastel parasols over tables outside both shops.
                int[][] pc = {{0xFFF2B8C4, 0xFFFFF4EC}, {0xFFAEE0C8, 0xFFFFF4EC}, {0xFFF4E09A, 0xFFFFF4EC}, {0xFFC8B8E8, 0xFFFFF4EC}};
                int k = 0;
                for (int i = 0; i < 2; i++) {
                    int[] b = q[i];
                    float u0 = b[0] * T, u1 = (b[0] + b[2]) * T, v0 = b[1] * T, v1 = (b[1] + b[3]) * T;
                    float row = i == 0 ? v1 + 13 : v0 - 13;
                    float st0 = i == 0 ? u0 + 8 : u0 - 28, st1 = i == 0 ? u1 + 30 : u1 - 6;
                    for (float u = st0; u < st1 && u < L - 8; u += 18, k++) {
                        float uu = u;
                        if (uu < 8 || Math.hypot(uu - fu, row - fv) < fr + 12) continue;
                        if (i == 0 && u < u1 + 2 && u > u1 - 10) continue;
                        cafeTable(f, uu - 3.5f, row + 3, 0xFFF4F0E8, 0xFF8AB0A0, 3);
                        cafeTable(f, uu + 3.5f, row - 2, 0xFFF4F0E8, 0xFF8AB0A0, 3);
                        int[] pp = pc[k % pc.length];
                        parasol(f, uu, row, 6.5f, pp[0], pp[1]);
                    }
                }
                // The gelato cart outside the gelateria: a tricycle box of coloured tubs under its own sunshade.
                int[] g0 = q[0];
                float cu = (g0[0] + g0[2]) * T + 8, cv = (g0[1] + g0[3]) * T + 6;
                f.rect(0x38000000, cu - 5 + 1, cv - 3 + 1.2f, cu + 5 + 1, cv + 3 + 1.2f);
                f.rect(0xFFF0E8DC, cu - 5, cv - 3, cu + 5, cv + 3);
                int[] tub = {0xFFF4C0C8, 0xFF9AD0A8, 0xFFF4E2A0, 0xFF7A4E34, 0xFFF8F4EE, 0xFFE87A6A};
                for (int j = 0; j < 6; j++)
                    f.rect(tub[j], cu - 4.4f + (j % 3) * 3, cv - 2.4f + (j / 3) * 2.6f, cu - 2f + (j % 3) * 3, cv - 0.2f + (j / 3) * 2.6f);
                f.circle(0xFF2A2A2A, cu - 5.5f, cv - 2.6f, 1);
                f.circle(0xFF2A2A2A, cu - 5.5f, cv + 2.6f, 1);
                parasol(f, cu + 3, cv - 3, 4.5f, 0xFFF2B8C4, 0xFFFFF4EC);
                // Lemon trees in tubs along the edges, benches, lamps.
                for (int i = 0; i < 4; i++) {
                    double a = i * Math.PI / 2;
                    float pu = fu + (float) Math.cos(a) * (fr + 9), pv = fv + (float) Math.sin(a) * (fr + 9);
                    pot(f, pu, pv, 3, 0xFFF0D840);
                    f.bench(fu + (float) Math.cos(a + Math.PI / 4) * (fr + 7), fv + (float) Math.sin(a + Math.PI / 4) * (fr + 7), i % 2 == 1);
                }
                pot(f, 6, S - 6, 3, 0xFFF0D840);
                pot(f, L - 6, 6, 3, 0xFFF0D840);
                f.lamp(4, 4);
                f.lamp(L - 4, S - 4);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                if (part == 0) {
                    // Pink stucco parapet, a flat roof, and the big rooftop cone with three scoops.
                    f.rect(0xFFD49AA4, 0, 0, L, S);
                    f.rect(0xFFE8D8C8, 1.4f, 1.4f, L - 1.4f, S - 1.4f);
                    float cu = L / 2, cv = S / 2, z = S / 16f;
                    f.poly(0x40000000, cu - 2 * z + 2, cv - 4 * z + 2, cu + 10 * z + 2, cv + 2, cu - 2 * z + 2, cv + 4 * z + 2);
                    f.poly(0xFFD0A060, cu - 2 * z, cv - 4 * z, cu + 10 * z, cv, cu - 2 * z, cv + 4 * z);
                    for (float t = 0; t < 10; t += 2.2f)
                        f.line(0x40804020, 0.4f, cu + (t - 2) * z, cv + (-4 + t * 0.4f) * z, cu + (t - 2) * z, cv + (4 - t * 0.4f) * z);
                    f.circle(0x40000000, cu - 3 * z + 1.5f, cv + z + 1.5f, 5 * z);
                    f.circle(0xFFF4C0C8, cu - 3 * z, cv - 2.5f * z, 3.3f * z);
                    f.circle(0xFF9AD0A8, cu - 3 * z, cv + 2.5f * z, 3.3f * z);
                    f.circle(0xFF7A4E34, cu - 6 * z, cv, 3.3f * z);
                    f.circle(0x50FFFFFF, cu - 7 * z, cv - z, 1.2f * z);
                    f.circle(0x50FFFFFF, cu - 4 * z, cv - 3.5f * z, z);
                    f.circle(0xFFD8283A, cu - 7.5f * z, cv - 0.5f * z, 0.9f * z);
                    hvac(f, L - 9, 3, 6, 4);
                    awning(f, 2, S - 0.2f, L - 2, S + 3.5f, 0xFFF2B8C4, 0xFFFFF4EC, 2.4f);
                    f.sign(city, "GELATO", L / 2, S - 2.5f, 4, 0xFF8A3A4A);
                } else {
                    // The café: a terracotta hipped roof, a chimney and an awning on the piazza side.
                    f.rect(0xFFC89A6A, 0, 0, L, S);
                    hip(f, 0xFFB8603E, 1, 1, L - 1, S - 1, true);
                    chimney(f, L * 0.7f, S * 0.25f, 2, 0xFF9A6A50);
                    awning(f, 2, -3.5f, L - 2, 0.2f, 0xFFAEE0C8, 0xFFFFF4EC, 2.4f);
                    f.sign(city, "CAFFÈ", L / 2, S / 2, 4, 0xFFF4E8D0);
                }
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.OLD | Variants.MID | Variants.DOWN;
        d.minS = 10;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_CAFE;
        d.roofCol = 0xFFE8D8C8;
        out.add(d);
    }

    // MORE

    // ------------------------------------------------------------------ 6 Opera Café

    /** A round advertising column: poster bands round a drum and a little domed cap. */
    static void adColumn(F f, float u, float v, float r) {
        f.circle(0x40000000, u + 1.2f, v + 1.6f, r + 0.5f);
        int[] pc = {0xFFC8A040, 0xFF8A3A3A, 0xFFE8E0D0, 0xFF3A5A7A, 0xFFD87A4A, 0xFF4A7A4A};
        for (int k = 0; k < 6; k++) {
            double a0 = k * Math.PI / 3, a1 = a0 + Math.PI / 3;
            f.poly(pc[k], u, v, u + (float) Math.cos(a0) * (r + 0.4f), v + (float) Math.sin(a0) * (r + 0.4f),
                    u + (float) Math.cos(a1) * (r + 0.4f), v + (float) Math.sin(a1) * (r + 0.4f));
        }
        f.circle(0xFF2E4A3A, u, v, r * 0.75f);
        f.circle(0xFF4A6E5A, u - r * 0.2f, v - r * 0.2f, r * 0.4f);
        f.circle(0xFFC8A040, u, v, r * 0.15f);
    }

    static void operaCafe(List<Landmarks.Def> out) {
        // An ornate corner café: a mansard roof with a copper corner dome, its terrace wrapping round under a green canopy.
        D d = new D("Opera Café", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                return new int[][]{B(1, 1, Math.max(6, L * 55 / 100), Math.max(4, S * 55 / 100), 3)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float bu1 = (q[0][0] + q[0][2]) * T, bv1 = (q[0][1] + q[0][3]) * T;
                // Pale limestone flags, a strip of darker paving round the building.
                f.rect(0xFFD2C8B4, 0, 0, L, S);
                for (float u = 0; u < L; u += 6) f.line(0x16000000, 0.4f, u, 0, u, S);
                for (float v = 0; v < S; v += 4) f.line(0x16000000, 0.4f, 0, v, L, v);
                f.rect(0xFFB8AC96, T - 2, T - 2, bu1 + 2, bv1 + 2);
                // The terrace: a raised timber deck along both street faces behind brass rails and box planters.
                float du = bu1 + 32, dv = bv1 + 32;
                f.rect(0xFF8A6A4A, bu1, T, Math.min(L - 3, du), dv);
                f.rect(0xFF8A6A4A, T, bv1, dv > S - 3 ? bu1 : du, Math.min(S - 3, dv));
                for (float u = bu1 + 2; u < Math.min(L - 3, du); u += 2.2f) f.line(0x1E000000, 0.4f, u, T, u, Math.min(S - 3, dv));
                for (float u = T + 2; u < bu1; u += 2.2f) f.line(0x1E000000, 0.4f, u, bv1, u, Math.min(S - 3, dv));
                float eu = Math.min(L - 3, du), ev = Math.min(S - 3, dv);
                f.line(0xFFC8A040, 0.8f, eu, T, eu, ev);
                f.line(0xFFC8A040, 0.8f, T, ev, eu, ev);
                for (float u = T + 4; u < eu - 2; u += 10) {
                    f.rect(0xFF4A4A44, u, ev - 2.4f, u + 6, ev);
                    f.bush(u + 3, ev - 1.2f, 1.6f);
                }
                for (float v = T + 4; v < ev - 4; v += 10) {
                    f.rect(0xFF4A4A44, eu - 2.4f, v, eu, v + 6);
                    f.bush(eu - 1.2f, v + 3, 1.6f);
                }
                // Marble-topped tables and bentwood chairs.
                for (float v = T + 7; v < ev - 5; v += 10)
                    for (float u = bu1 + 11; u < eu - 5; u += 10) cafeTable(f, u, v, 0xFFF0ECE4, 0xFF3A2A22, 3);
                for (float u = T + 7; u < bu1 + 1; u += 10)
                    for (float v = bv1 + 11; v < ev - 5; v += 10) cafeTable(f, u, v, 0xFFF0ECE4, 0xFF3A2A22, 3);
                // The canopy along both faces: bottle green with a gold valance.
                f.rect(0x38000000, bu1 + 1, T + 1, bu1 + 7, bv1 + 7.5f);
                f.rect(0x38000000, T + 1, bv1 + 1, bu1 + 1, bv1 + 7.5f);
                f.rect(0xFF2E5A40, bu1, T, bu1 + 6, bv1 + 6);
                f.rect(0xFF2E5A40, T, bv1, bu1, bv1 + 6);
                f.rect(0xFF3A6E50, bu1, T, bu1 + 2.5f, bv1 + 2.5f);
                f.rect(0xFF3A6E50, T, bv1, bu1, bv1 + 2.5f);
                for (float v = T + 1.5f; v < bv1 + 6; v += 3) f.circle(0xFFC8A040, bu1 + 6, v, 0.7f);
                for (float u = T + 1.5f; u < bu1 + 6; u += 3) f.circle(0xFFC8A040, u, bv1 + 6, 0.7f);
                // Beyond: plane trees, an advertising column, a lamp and a news kiosk.
                if (L - eu > 22) for (float v = 16; v < S - 22; v += 36) f.tree(eu + (L - eu) / 2, v, 7);
                if (S - ev > 22) for (float u = 16; u < eu - 10; u += 36) f.tree(u, ev + (S - ev) / 2, 7);
                adColumn(f, Math.min(L - 7, eu + 7), Math.min(S - 7, ev + 7), 3.4f);
                f.lamp(eu + 3, T * 0.6f);
                f.lamp(T * 0.6f, ev + 3);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S, m = Math.min(5f, S * 0.12f);
                // Mansard: slate slopes all round with dormers, a zinc flat top, cresting and chimneys.
                f.rect(0xFFD8CCB4, 0, 0, L, S);
                f.rect(0xFFB4A88E, 0.8f, 0.8f, L - 0.8f, S - 0.8f);
                f.poly(0xFF5E6672, 1.5f, 1.5f, L - 1.5f, 1.5f, L - 1.5f - m, 1.5f + m, 1.5f + m, 1.5f + m);
                f.poly(0xFF545C68, 1.5f, 1.5f, 1.5f + m, 1.5f + m, 1.5f + m, S - 1.5f - m, 1.5f, S - 1.5f);
                f.poly(0xFF3E4650, L - 1.5f, 1.5f, L - 1.5f, S - 1.5f, L - 1.5f - m, S - 1.5f - m, L - 1.5f - m, 1.5f + m);
                f.poly(0xFF363E48, 1.5f, S - 1.5f, L - 1.5f, S - 1.5f, L - 1.5f - m, S - 1.5f - m, 1.5f + m, S - 1.5f - m);
                f.rect(0xFF9AA4A8, 1.5f + m, 1.5f + m, L - 1.5f - m, S - 1.5f - m);
                for (float u = 3 + m; u < L - 2 - m; u += 2.5f) f.line(0x20000000, 0.4f, u, 1.5f + m, u, S - 1.5f - m);
                f.frame(0xFF3A3A3A, 0.6f, 1.5f + m, 1.5f + m, L - 1.5f - m, S - 1.5f - m);
                for (float u = 6; u < L - 10; u += 7) {
                    f.rect(0xFFE8E0CC, u, S - 1.5f - m * 0.8f, u + 2.4f, S - 2);
                    f.rect(0xFFE8E0CC, u, 2, u + 2.4f, 1.5f + m * 0.8f);
                }
                for (float v = 6; v < S - 10; v += 7) f.rect(0xFFE8E0CC, 2, v, 1.5f + m * 0.8f, v + 2.4f);
                chimney(f, L * 0.25f, S * 0.35f, 3, 0xFFA89A84);
                chimney(f, L * 0.5f, S * 0.35f, 3, 0xFFA89A84);
                hvac(f, 1.5f + m + 3, S * 0.6f, 6, 4);
                // The corner turret: a verdigris dome, ribs, a lantern and a gilded finial.
                float cr = Math.min(S, L) * 0.22f, cu = L - cr - 0.5f, cv = S - cr - 0.5f;
                f.rect(0xFFC8BCA4, cu - cr, cv - cr, L, S);
                dome(f, cu, cv, cr, 0xFF5E9A88, 12);
                for (int k = 0; k < 12; k++) {
                    double a = k * Math.PI / 6;
                    f.line(0x50FFFFFF, 0.4f, cu, cv, cu + (float) Math.cos(a) * cr, cv + (float) Math.sin(a) * cr);
                }
                f.circle(0xFF3E6A60, cu, cv, cr * 0.32f);
                f.circle(0xFF6EAA96, cu - 0.4f, cv - 0.4f, cr * 0.2f);
                f.circle(0xFFE0C050, cu, cv, 1);
                f.sign(city, "CAFÉ DE L'OPÉRA", L * 0.42f, S * 0.5f, 4.5f, 0xFFF0E4C0);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.OLD | Variants.MID;
        d.minS = 10;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_CAFE;
        d.roofCol = 0xFF5E6672;
        out.add(d);
    }

    // ------------------------------------------------------------------ 7 Gallery Warehouse

    static void galleryWarehouse(List<Landmarks.Def> out) {
        // An old brick warehouse turned gallery: a mural over its whole roof, north lights, and a yard of big sculptures.
        D d = new D("Gallery Warehouse", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                return new int[][]{B(1, 1, Math.max(6, L * 58 / 100), S - 2, 2)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float yu = (q[0][0] + q[0][2]) * T;
                // Old concrete with expansion joints and a strip of cobbles round the warehouse.
                f.rect(0xFFAEAAA2, 0, 0, L, S);
                for (float u = yu + 12; u < L; u += 16) f.line(0x22000000, 0.5f, u, 0, u, S);
                for (float v = 12; v < S; v += 16) f.line(0x22000000, 0.5f, yu, v, L, v);
                f.specks(r, 60, 0x16000000, 0x18FFFFFF, 0, 0, L, S, 1.6f);
                f.rect(0xFF8E867A, T - 3, T - 3, yu + 3, S - T + 3);
                for (float u = T - 2; u < yu + 3; u += 3) f.line(0x22000000, 0.4f, u, T - 3, u, S - T + 3);
                // Rails of the old siding still set in the yard.
                float rv = S - T * 0.6f;
                f.line(0xFF8A8A88, 0.8f, 0, rv - 2.2f, L, rv - 2.2f);
                f.line(0xFF8A8A88, 0.8f, 0, rv + 2.2f, L, rv + 2.2f);
                // Gravel beds and sculptures in the yard.
                float a = yu + 6, e = L - 8, mv = S / 2, w = e - a;
                if (w > 20) {
                    f.roundRect(0xFF9A948A, a - 1, 7, e + 1, S - 13, 6);
                    f.roundRect(0xFFC8C0B0, a, 8, e, S - 14, 5);
                    f.specks(r, 70, 0x22000000, 0x30FFFFFF, a + 4, 10, e - 4, S - 16, 1);
                    float z = Math.min(1.6f, Math.min(w, S) / 70f);
                    // A great red steel arc sweeping across the gravel.
                    float au = a + w * 0.22f;
                    f.curve(0x40000000, 4 * z, au + 2, S * 0.2f + 2, au + w * 0.35f + 2, mv + 2, au + 4, S * 0.78f - 12);
                    f.curve(0xFFB0402E, 4 * z, au, S * 0.2f, au + w * 0.35f, mv, au + 2, S * 0.78f - 14);
                    f.curve(0xFFD06048, z, au - 0.8f, S * 0.2f, au + w * 0.35f - 1.2f, mv, au + 1.2f, S * 0.78f - 14);
                    // A polished stone sphere.
                    float su = a + w * 0.68f, sv = S * 0.26f;
                    f.circle(0x40000000, su + 2.5f, sv + 3, 7 * z);
                    f.circle(0xFF5E6268, su, sv, 7 * z);
                    f.circle(0xFF8A9098, su - 2 * z, sv - 2 * z, 3.6f * z);
                    f.circle(0xFFC0C6CC, su - 3 * z, sv - 3 * z, 1.2f * z);
                    // Stacked tilted cubes.
                    float cu = a + w * 0.5f, cv = S * 0.62f;
                    f.turn(cu, cv, 20);
                    Landmarks.rect(f.c, f.p, 0x40000000, -5 * z, -5 * z, 8 * z, 8 * z);
                    Landmarks.rect(f.c, f.p, 0xFFC8A040, -6 * z, -6 * z, 6 * z, 6 * z);
                    Landmarks.rect(f.c, f.p, 0xFFE0C060, -6 * z, -6 * z, 6 * z, -2 * z);
                    f.c.restore();
                    f.turn(cu + 1, cv, 62);
                    Landmarks.rect(f.c, f.p, 0x40000000, -3 * z, -3 * z, 5 * z, 5 * z);
                    Landmarks.rect(f.c, f.p, 0xFF2A5A8A, -3.6f * z, -3.6f * z, 3.6f * z, 3.6f * z);
                    Landmarks.rect(f.c, f.p, 0xFF3A70A4, -3.6f * z, -3.6f * z, 3.6f * z, -1.2f * z);
                    f.c.restore();
                    // A steel ring on a white plinth and a bronze figure on another.
                    float ru = a + w * 0.82f, rv2 = S * 0.6f;
                    f.rect(0x40000000, ru - 6 * z + 1.5f, rv2 - 6 * z + 2, ru + 6 * z + 1.5f, rv2 + 6 * z + 2);
                    f.rect(0xFFE8E4DC, ru - 6 * z, rv2 - 6 * z, ru + 6 * z, rv2 + 6 * z);
                    f.ring(0x40000000, 1.6f * z, ru + 1.5f, rv2 + 2, 4 * z);
                    f.ring(0xFFB8BCC0, 1.6f * z, ru, rv2, 4 * z);
                    float bu = a + w * 0.4f, bv = S * 0.24f;
                    f.rect(0x40000000, bu - 4 + 1.5f, bv - 4 + 2, bu + 4 + 1.5f, bv + 4 + 2);
                    f.rect(0xFFE8E4DC, bu - 4, bv - 4, bu + 4, bv + 4);
                    f.oval(0xFF6A4A2A, bu - 1.6f, bv - 2.6f, bu + 1.6f, bv + 2.6f);
                    f.circle(0xFF7A5A32, bu, bv - 0.4f, 1.3f);
                    f.circle(0xFF9A7A4A, bu - 0.4f, bv - 0.8f, 0.5f);
                }
                // Silver birches in a row by the street, and benches.
                for (float v = 10; v < S - 10; v += 20) f.tree(L - 4, v, 4, 0xFF8AAA5A);
                f.bench(yu + 5, S - 12, false);
                f.lamp(yu + 4, 4);
                // A banner board at the door.
                f.rect(0xFF2A2A2A, yu + 1, mv - 6, yu + 3, mv + 6);
                f.rect(0xFFE84A3A, yu + 1.4f, mv - 5, yu + 2.6f, mv + 5);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                // Brick parapet and a pale painted roof deck.
                f.rect(0xFF8A4A36, 0, 0, L, S);
                for (float u = 1; u < L; u += 2) {
                    f.rect(0xFF9A5A42, u, 0.3f, u + 1, 1.4f);
                    f.rect(0xFF9A5A42, u, S - 1.4f, u + 1, S - 0.3f);
                }
                f.rect(0xFFE6E0D4, 2, 2, L - 2, S - 2);
                // The mural: bold bands, discs and a sweeping wave over the deck.
                float a = 3, e = L * 0.78f, v0 = 3, v1 = S - 3;
                f.c.save();
                f.c.clipRect(Math.min(f.x(a, v0), f.x(e, v1)), Math.min(f.y(a, v0), f.y(e, v1)), Math.max(f.x(a, v0), f.x(e, v1)), Math.max(f.y(a, v0), f.y(e, v1)));
                int[] mc = {0xFFC85A44, 0xFFE0AA48, 0xFF4A80B0, 0xFF5A9A6A, 0xFF7A5A98, 0xFFD88AA0, 0xFF2E2E40};
                float w = (e - a) / 7;
                for (int k = 0; k < 7; k++) f.poly(mc[k], a + k * w, v0, a + (k + 1) * w + 6, v0, a + (k + 1) * w - 6, v1, a + k * w - 12, v1);
                f.circle(0xFFF4F0E4, a + (e - a) * 0.3f, (v0 + v1) / 2, (v1 - v0) * 0.28f);
                f.circle(0xFFE0503A, a + (e - a) * 0.3f, (v0 + v1) / 2, (v1 - v0) * 0.18f);
                f.circle(0xFF2A2A40, a + (e - a) * 0.3f, (v0 + v1) / 2, (v1 - v0) * 0.07f);
                f.curve(0xFFF4F0E4, 3, a, v1 - 6, (a + e) / 2, v0 - 10, e, v1 - 4);
                f.curve(0xFF2A2A40, 1, a, v1 - 3.5f, (a + e) / 2, v0 - 7.5f, e, v1 - 1.5f);
                for (int k = 0; k < 10; k++) f.circle(0xFFF4F0E4, a + 6 + k * (e - a - 12) / 9, v0 + 4 + (k % 3) * 2, 1.2f);
                f.c.restore();
                f.frame(0x40000000, 0.6f, a, v0, e, v1);
                // North-light skylights at the far end and a goods hoist housing.
                for (float u = e + 3; u + 4 < L - 2; u += 6) {
                    f.rect(0xFF6A6E74, u, 3, u + 4, S - 3);
                    f.rect(0xFF8AAAC0, u + 2.4f, 3.5f, u + 4, S - 3.5f);
                    f.line(0x60FFFFFF, 0.4f, u + 3, 4, u + 3, S - 4);
                }
                f.rect(0x48000000, L * 0.5f + 1, S - 9 + 1.4f, L * 0.5f + 7 + 1, S - 3 + 1.4f);
                f.rect(0xFF7A3E2E, L * 0.5f, S - 9, L * 0.5f + 6, S - 3);
                f.sign(city, "GALLERY", L * 0.4f, S / 2, 5, 0xFFFFFFFF);
            }
        };
        d.ground = City.LOT;
        d.districts = Variants.IND | Variants.MID | Variants.OLD;
        d.minS = 10;
        d.minL = 13;
        d.base = City.SHOP;
        d.inside = Variants.I_SHOWROOM;
        d.roofCol = 0xFF8A4A36;
        out.add(d);
    }

    // ------------------------------------------------------------------ 8 Street Art Alley

    /** A food cart from above: a box body, a serving hatch side, wheels and a sunshade. */
    static void foodCart(F f, float u, float v, int body, int shadeA, int shadeB) {
        f.rect(0x40000000, u - 7 + 1, v - 4 + 1.4f, u + 7 + 1, v + 4 + 1.4f);
        f.rect(City.darken(body, 0.7f), u - 7, v - 4, u + 7, v + 4);
        f.rect(body, u - 6.5f, v - 3.5f, u + 6.5f, v + 3.5f);
        f.rect(0xFFD8D4CC, u - 6, v + 2, u + 6, v + 3.4f);
        for (float x = u - 5; x < u + 5.5f; x += 2.5f) f.circle(0xFFC88A4A, x, v + 2.7f, 0.5f);
        f.circle(0xFF2A2A2A, u - 5, v + 4.3f, 1.1f);
        f.circle(0xFF2A2A2A, u + 5, v + 4.3f, 1.1f);
        f.line(0xFF6A6A6A, 0.7f, u + 7, v - 1, u + 10, v - 1);
        f.rect(City.darken(body, 0.85f), u + 1, v - 3.5f, u + 6.5f, v + 1.5f);
        parasol(f, u - 2, v - 1, 5.5f, shadeA, shadeB);
    }

    static void streetArt(List<Landmarks.Def> out) {
        // A back-street yard given over to painters: graffiti walls, a mural over the tarmac, food carts and benches.
        D d = new D("Street Art Alley", T_PARK) {
            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S;
                f.rect(0xFF5A5A5C, 0, 0, L, S);
                f.specks(r, 120, 0x18000000, 0x14FFFFFF, 0, 0, L, S, 1.4f);
                // The ground mural: overlapping discs, ribbons of colour and black linework across the middle.
                float cu = L / 2, cv = S / 2, R = Math.min(L, S) * 0.36f;
                int[] mc = {0xFFC8604A, 0xFFD8A048, 0xFFD8C860, 0xFF6A9A5A, 0xFF4A7AA8, 0xFF7A5A98, 0xFFC87898};
                f.c.save();
                f.c.clipRect(Math.min(f.x(12, S * 0.18f), f.x(L - 12, S * 0.82f)), Math.min(f.y(12, S * 0.18f), f.y(L - 12, S * 0.82f)),
                        Math.max(f.x(12, S * 0.18f), f.x(L - 12, S * 0.82f)), Math.max(f.y(12, S * 0.18f), f.y(L - 12, S * 0.82f)));
                f.rect(0xFF3A4A6A, 12, S * 0.18f, L - 12, S * 0.82f);
                for (int k = 0; k < 9; k++) {
                    float u = 14 + r.nextFloat() * (L - 28), v = S * 0.2f + r.nextFloat() * S * 0.6f, rr = R * (0.25f + r.nextFloat() * 0.35f);
                    f.circle(mc[k % mc.length], u, v, rr);
                    f.circle(City.lighten(mc[k % mc.length], 0.15f), u - rr * 0.2f, v - rr * 0.2f, rr * 0.5f);
                }
                for (int k = 0; k < 4; k++) {
                    float v0 = S * (0.2f + k * 0.2f), v1 = S * (0.8f - k * 0.15f);
                    f.curve(mc[(k + 3) % mc.length], 4.5f, 12, v0, cu, S * (k % 2 == 0 ? 0.05f : 0.95f), L - 12, v1);
                    f.curve(0xFF1E1E22, 0.8f, 12, v0 + 2.6f, cu, S * (k % 2 == 0 ? 0.05f : 0.95f) + 2.6f, L - 12, v1 + 2.6f);
                }
                // A big painted eye in the middle.
                f.oval(0xFFF0EADA, cu - R * 0.45f, cv - R * 0.22f, cu + R * 0.45f, cv + R * 0.22f);
                f.circle(0xFF4A7AA8, cu, cv, R * 0.18f);
                f.circle(0xFF1E1E22, cu, cv, R * 0.08f);
                f.circle(0xFFFFFFFF, cu - R * 0.05f, cv - R * 0.05f, R * 0.03f);
                f.c.restore();
                // Zigzag and checker strips painted along the yard ends.
                for (float v = 4; v < S - 4; v += 6) {
                    f.poly(mc[(int) (v / 6) % mc.length], 4, v, 10, v + 3, 4, v + 6);
                    f.poly(mc[(int) (v / 6 + 3) % mc.length], L - 4, v, L - 10, v + 3, L - 4, v + 6);
                }
                for (float u = 16, k = 0; u < L - 16; u += 4, k++) {
                    f.rect(k % 2 == 0 ? 0xFFF4F0E4 : 0xFF2A2A2A, u, S * 0.14f, u + 4, S * 0.14f + 2);
                    f.rect(k % 2 == 1 ? 0xFFF4F0E4 : 0xFF2A2A2A, u, S * 0.86f - 2, u + 4, S * 0.86f);
                }
                // Walls along both long sides, every metre a different tag.
                for (int s = 0; s < 2; s++) {
                    float v0 = s == 0 ? 0 : S - 5, v1 = v0 + 5;
                    f.rect(0xFF7A6A5E, 0, v0, L, v1);
                    for (float u = 0; u < L; u += 5 + r.nextFloat() * 6) {
                        float w = 4 + r.nextFloat() * 8;
                        int col = mc[r.nextInt(mc.length)];
                        f.rect(col, u, v0 + 0.4f, Math.min(L, u + w), v1 - 0.4f);
                        f.line(r.nextBoolean() ? 0xFF1A1A1A : 0xFFF4F0E4, 0.6f, u + 1, v0 + 1 + r.nextFloat() * 2, Math.min(L, u + w) - 1, v0 + 1 + r.nextFloat() * 2);
                    }
                    f.line(0x50000000, 0.8f, 0, s == 0 ? v1 + 0.4f : v0 - 0.4f, L, s == 0 ? v1 + 0.4f : v0 - 0.4f);
                }
                // Food carts down one side, picnic tables down the other, string lights between.
                int[][] cc = {{0xFF3A7A8A, 0xFFE0C850, 0xFFF0ECE0}, {0xFFB8483A, 0xFFF0ECE0, 0xFF2A2A2A}, {0xFF5A9A5A, 0xFFE09A40, 0xFFF0ECE0}, {0xFF6A4A8A, 0xFFD07898, 0xFFF0ECE0}};
                int k = 0;
                for (float u = 16; u < L - 12; u += 30, k++) {
                    int[] c3 = cc[k % cc.length];
                    foodCart(f, u, 12, c3[0], c3[1], c3[2]);
                }
                for (float u = 22; u < L - 14; u += 30) picnic(f, u, S - 12, true, 0xFF8A6A4A);
                int[] lc = {0xFFF8E8A0, 0xFFF4C070, 0xFFF8F0D0};
                for (float u = 14; u < L - 8; u += 56) string(f, u, 5, u + 20, S - 5, lc, 4, 0.6f);
                // Spray cans, a stepladder and bins.
                for (int j = 0; j < 8; j++) f.circle(mc[j % mc.length], 8 + r.nextFloat() * (L - 16), S - 6 + r.nextFloat() * 1.5f, 0.6f);
                f.rect(0xFFB8B8B4, L - 14, 5, L - 12, 12);
                f.rect(0xFFB8B8B4, L - 10, 5, L - 8, 12);
                for (float v = 6; v < 12; v += 1.6f) f.line(0xFF8A8A86, 0.5f, L - 12, v, L - 10, v);
                f.rect(0xFF3A5A3A, 3, S - 9, 7, S - 5);
                f.rect(0xFF3A3A40, 3, 5, 7, 9);
            }
        };
        d.ground = City.LOT;
        d.districts = Variants.IND | Variants.MID | Variants.DOWN | Variants.UNI;
        d.minS = 9;
        d.minL = 12;
        out.add(d);
    }

    // ------------------------------------------------------------------ 9 Artist Lofts

    static void artistLofts(List<Landmarks.Def> out) {
        // Old factory lofts: sawtooth north-light roofs on one half, roof gardens on the other, a makers' yard between.
        D d = new D("Artist Lofts", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                if (S >= 13) {
                    int dep = Math.min(5, (S - 4) / 2);
                    return new int[][]{B(1, 1, L - 2, dep, 4), B(1, S - 1 - dep, L - 2, dep, 4)};
                }
                return new int[][]{B(1, 1, L - 2, Math.max(4, S - 5), 4)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float y0 = (q[0][1] + q[0][3]) * T, y1 = q.length > 1 ? q[1][1] * T : S;
                f.rect(0xFF8E867A, 0, 0, L, S);
                // Setts in the yard with old rails curving through.
                f.rect(0xFF9A9284, 0, y0, L, y1);
                for (float v = y0 + 2.5f; v < y1; v += 2.5f) f.line(0x1E000000, 0.4f, 0, v, L, v);
                f.specks(r, 60, 0x16000000, 0x16FFFFFF, 0, y0, L, y1, 1.8f);
                float mv = (y0 + y1) / 2;
                f.curve(0xFF6E6A66, 0.8f, 0, mv - 2, L * 0.5f, mv - 8, L, mv - 2);
                f.curve(0xFF6E6A66, 0.8f, 0, mv + 2, L * 0.5f, mv - 4, L, mv + 2);
                // Raised beds of veg and flowers, a kiln, a welded sculpture and bikes.
                int[] fl = {0xFF5A9A4A, 0xFFD87A4A, 0xFF8AB05A, 0xFFE0C04A};
                int k = 0;
                for (float u = 8; u < L * 0.55f; u += 16, k++) {
                    float v = mv + 2;
                    if (v + 8 > y1 - 2) v = y1 - 10;
                    f.rect(0x38000000, u + 1, v + 1.2f, u + 13, v + 8 + 1.2f);
                    f.rect(0xFF7A5A3A, u, v, u + 12, v + 8);
                    f.rect(0xFF5A4232, u + 1, v + 1, u + 11, v + 7);
                    for (float x = u + 2.5f; x < u + 11; x += 2.5f)
                        for (float y = v + 2.5f; y < v + 7; y += 2.5f) f.circle(fl[(k + (int) x) % 4], x, y, 1);
                }
                float su = L * 0.72f;
                f.circle(0x40000000, su + 1.5f, mv + 2, 5);
                f.ring(0xFF8A5A3A, 1.2f, su, mv, 5);
                f.line(0xFF8A5A3A, 1.2f, su - 5, mv, su + 5, mv);
                f.line(0xFF8A5A3A, 1.2f, su, mv - 5, su + 3, mv + 4);
                f.circle(0xFF6A3A2A, su, mv, 1.2f);
                for (float u = L - 22; u < L - 6; u += 3.2f) {
                    f.line(0xFF2A2A2A, 0.5f, u, y0 + 2, u, y0 + 8);
                    f.circle(0xFF2A2A2A, u, y0 + 2.5f, 1);
                    f.circle(0xFF2A2A2A, u, y0 + 7.5f, 1);
                }
                f.tree(4, mv, 5);
                f.tree(L - 4, mv + 4, 5);
                f.bench(L * 0.6f, y1 - 4, true);
                for (float u = 30; u < L - 10; u += 50) f.lamp(u, mv - 6);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                f.rect(0xFF8A4A36, 0, 0, L, S);
                float half = L * (part == 0 ? 0.55f : 0.45f);
                boolean sawFirst = part == 0;
                float s0 = sawFirst ? 1.5f : half, s1 = sawFirst ? half : L - 1.5f;
                float g0 = sawFirst ? half : 1.5f, g1 = sawFirst ? L - 1.5f : half;
                // Sawtooth: slate slopes and glazed north faces, teeth across the building.
                for (float u = s0; u + 6 <= s1 + 0.1f; u += 6) {
                    f.rect(0xFF4E545C, u, 1.5f, u + 4.4f, S - 1.5f);
                    f.rect(0xFF5E6670, u, 1.5f, u + 2, S - 1.5f);
                    f.rect(0xFF9ABACC, u + 4.4f, 1.5f, u + 6, S - 1.5f);
                    f.line(0x70FFFFFF, 0.4f, u + 5.2f, 2, u + 5.2f, S - 2);
                    f.line(0x50000000, 0.6f, u + 6, 1.5f, u + 6, S - 1.5f);
                }
                // Roof garden: decking, planters, little trees, a pergola, deck chairs.
                f.rect(0xFFA8845C, g0 + 0.8f, 1.5f, g1, S - 1.5f);
                for (float v = 3; v < S - 1.5f; v += 1.8f) f.line(0x22000000, 0.4f, g0 + 0.8f, v, g1, v);
                f.rect(0xFF6A7A4A, g0 + 1.5f, 2, g1 - 0.5f, 4.5f);
                for (float u = g0 + 3; u < g1 - 2; u += 3.5f) f.bush(u, 3.2f, 1.6f);
                float pu = (g0 + g1) / 2;
                f.rect(0x40000000, pu - 6 + 1, S * 0.45f + 1.4f, pu + 6 + 1, S - 3 + 1.4f);
                for (float u = pu - 6; u <= pu + 6; u += 2) f.line(0xFF6A4A30, 0.8f, u, S * 0.45f, u, S - 3);
                f.line(0xFF5A3A24, 1, pu - 6, S * 0.45f, pu + 6, S * 0.45f);
                f.line(0xFF5A3A24, 1, pu - 6, S - 3, pu + 6, S - 3);
                f.circle(0xFF5A8A3A, pu - 4, S * 0.5f, 1.6f);
                f.circle(0xFF6A9A4A, pu + 3, S - 4, 1.8f);
                pot(f, g0 + 3, S - 4, 2, 0xFFE04A4A);
                pot(f, g1 - 3, S * 0.55f, 2, 0);
                f.rect(0xFFE8D8B0, g1 - 7, S - 6, g1 - 4.5f, S - 2.5f);
                f.rect(0xFF3A6A8A, g1 - 4, S - 6, g1 - 1.5f, S - 2.5f);
                // Brick parapet coping and a water tank on stilts.
                f.frame(0xFF9A5A42, 1, 0.5f, 0.5f, L - 0.5f, S - 0.5f);
                float tu = sawFirst ? half - 2 : half + 2;
                f.circle(0x48000000, tu + 1.5f, S * 0.5f + 2, 3.4f);
                f.circle(0xFF6A4A30, tu, S * 0.5f, 3.4f);
                f.circle(0xFF8A6440, tu - 0.5f, S * 0.5f - 0.5f, 2.4f);
                if (part == 0) f.sign(city, "LOFTS", L / 2, S / 2, 4.5f, 0xFFF0E8D0);
            }
        };
        d.ground = City.LOT;
        d.districts = Variants.IND | Variants.MID | Variants.UNI;
        d.minS = 10;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFF4E545C;
        out.add(d);
    }

    // ------------------------------------------------------------------ 10 Jazz Club

    /** A grand piano from above, keyboard along v at u, its body curving away towards +u; lid open. */
    static void piano(F f, float u, float v, float s) {
        Path q = new Path();
        f.mv(q, u, v - 2.4f * s);
        f.ln(q, u, v + 2.4f * s);
        f.ln(q, u + 7.5f * s, v + 2.4f * s);
        f.qd(q, u + 9 * s, v + 2.4f * s, u + 8.6f * s, v + 0.6f * s);
        f.qd(q, u + 5 * s, v - 0.4f * s, u + 3.4f * s, v - 2.4f * s);
        f.ln(q, u, v - 2.4f * s);
        f.fill(q, 0xFF141416);
        f.rect(0xFFF4F0E8, u - 0.9f * s, v - 2.3f * s, u, v + 2.3f * s);
        for (float y = v - 2f * s; y < v + 2.2f * s; y += 0.7f * s) f.rect(0xFF141416, u - 0.9f * s, y, u - 0.4f * s, y + 0.3f * s);
        f.line(0xFF4A4A50, 0.5f, u + 0.6f * s, v - 2 * s, u + 7 * s, v + 2 * s);
        f.circle(0x40FFFFFF, u + 2 * s, v + 1 * s, 0.6f * s);
    }

    static void jazzClub(List<Landmarks.Def> out) {
        // A dark brick club with an art-deco front and a lit marquee, a walled courtyard with a little stage and a piano.
        D d = new D("Jazz Club", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                return new int[][]{B(1, 1, Math.max(5, L * 48 / 100), S - 2, 2)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float yu = (q[0][0] + q[0][2]) * T;
                // Brick pavers and an ivy-covered courtyard wall.
                f.rect(0xFF7A4A3A, 0, 0, L, S);
                for (float v = 0; v < S; v += 2.4f) f.line(0x22000000, 0.4f, 0, v, L, v);
                for (float v = 0, k = 0; v < S; v += 2.4f, k++)
                    for (float u = (k % 2) * 2.5f; u < L; u += 5) f.line(0x1A000000, 0.4f, u, v, u, v + 2.4f);
                f.rect(0xFF5A3A2E, yu + 4, 2, L - 2, 4);
                f.rect(0xFF5A3A2E, L - 4, 2, L - 2, S - 2);
                f.rect(0xFF5A3A2E, yu + 4, S - 4, L - 22, S - 2);
                for (int k = 0; k < 40; k++) {
                    float t = r.nextFloat();
                    if (k % 2 == 0) f.circle(0xFF3E6A34, yu + 4 + t * (L - yu - 6), 3 + r.nextFloat() * 1.4f, 1.3f + r.nextFloat());
                    else f.circle(0xFF4A7A3E, L - 3 + r.nextFloat() * 1.2f - 0.6f, 3 + t * (S - 6), 1.3f + r.nextFloat());
                }
                // The stage: timber boards against the far wall, a grand piano, a double bass on its side, a mic.
                float sv0 = S * 0.28f, sv1 = S * 0.72f, su0 = L - 22, su1 = L - 5;
                f.rect(0x40000000, su0 + 1, sv0 + 1.4f, su1 + 1, sv1 + 1.4f);
                f.rect(0xFF6A4A30, su0, sv0, su1, sv1);
                for (float v = sv0 + 2; v < sv1; v += 2) f.line(0x30000000, 0.4f, su0, v, su1, v);
                f.frame(0xFF3A2A1E, 0.6f, su0, sv0, su1, sv1);
                piano(f, su0 + 3, sv0 + (sv1 - sv0) * 0.3f, 1.6f);
                f.oval(0xFF8A4A22, su0 + 5, sv1 - 7, su0 + 13, sv1 - 3.4f);
                f.line(0xFF2A1A12, 0.6f, su0 + 9, sv1 - 6.6f, su0 + 16, sv1 - 5.2f);
                f.circle(0xFF2A2A2A, su0 + 3, sv1 - 4, 0.8f);
                f.circle(0xFFB8BCC0, su0 + 3, sv1 - 4, 0.4f);
                // Little round tables with candles before the stage.
                for (float u = yu + 14, k = 0; u < su0 - 6; u += 20, k++)
                    for (float v = 12 + (k % 2) * 9; v < S - 10; v += 20) {
                        for (int j = 0; j < 3; j++) {
                            double a = j * 2 * Math.PI / 3 + k;
                            f.circle(0xFF6A2A2A, u + (float) Math.cos(a) * 3.8f, v + (float) Math.sin(a) * 3.8f, 1.3f);
                            f.circle(0xFF8A3A3A, u + (float) Math.cos(a) * 3.8f - 0.3f, v + (float) Math.sin(a) * 3.8f - 0.3f, 0.7f);
                        }
                        f.circle(0x40000000, u + 0.7f, v + 0.9f, 2.6f);
                        f.circle(0xFF1E1E22, u, v, 2.6f);
                        f.circle(0xFFF8D870, u, v, 0.6f);
                        f.circle(0x40F8D870, u, v, 1.4f);
                    }
                // Warm lights strung over the courtyard.
                int[] lc = {0xFFF8D890, 0xFFF4C070};
                for (float v = 10; v < S - 6; v += 34) string(f, yu + 4, v, L - 4, v + 10, lc, 4, 0.6f);
                // The marquee over the street door, ringed with bulbs, and a gold-on-black poster frame.
                float mv = S / 2;
                f.rect(0x48000000, yu + 1, mv - 9 + 1.4f, yu + 6 + 1, mv + 9 + 1.4f);
                f.rect(0xFF1E1E22, yu, mv - 9, yu + 6, mv + 9);
                f.rect(0xFFC8A040, yu + 0.6f, mv - 8.4f, yu + 5.4f, mv + 8.4f);
                f.rect(0xFF2A1E2A, yu + 1.2f, mv - 7.8f, yu + 4.8f, mv + 7.8f);
                for (float v = mv - 8; v <= mv + 8; v += 2) {
                    f.circle(0xFFFFF0B0, yu + 5.6f, v, 0.45f);
                    f.circle(0xFFFFF0B0, yu + 0.4f, v, 0.45f);
                }
                f.lamp(yu + 3, 4);
                f.lamp(yu + 3, S - 4);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                // Charcoal roof inside a stepped deco parapet with a sunburst crest over the entrance side.
                f.rect(0xFF3A2A2A, 0, 0, L, S);
                f.rect(0xFF4A4448, 1.8f, 1.8f, L - 1.8f, S - 1.8f);
                f.specks(r, 30, 0x18000000, 0x12FFFFFF, 2, 2, L - 2, S - 2, 1);
                float cu = L * 0.62f, cv = S / 2, cr = Math.min(S * 0.32f, 9);
                f.circle(0x40000000, cu + 1, cv + 1.4f, cr + 1);
                f.circle(0xFF2A1E22, cu, cv, cr + 1);
                for (int k = 0; k < 16; k++) {
                    double a0 = k * Math.PI / 8, a1 = a0 + Math.PI / 8;
                    f.poly(k % 2 == 0 ? 0xFFC8A040 : 0xFF7A2A2A, cu, cv, cu + (float) Math.cos(a0) * cr, cv + (float) Math.sin(a0) * cr,
                            cu + (float) Math.cos(a1) * cr, cv + (float) Math.sin(a1) * cr);
                }
                f.circle(0xFF2A1E22, cu, cv, cr * 0.35f);
                f.circle(0xFFE8C860, cu, cv, cr * 0.2f);
                // Steps of the parapet at the corners.
                for (int i = 0; i < 4; i++) {
                    float u = i % 2 == 0 ? 0 : L - 4, v = i < 2 ? 0 : S - 4;
                    f.rect(0xFF5A3A3A, u, v, u + 4, v + 4);
                    f.rect(0xFF6A4848, u + 1, v + 1, u + 3, v + 3);
                }
                // Plant, a smoke vent and a dome light over the bar.
                f.frame(0xFFC8A040, 0.5f, 3, 3, L - 3, S - 3);
                hvac(f, 4, 4, 7, 5);
                hvac(f, 4, 11, 7, 5);
                // A glazed lantern over the dance floor and a water tank.
                float lu0 = L * 0.15f, lu1 = L * 0.45f, lv0 = S * 0.35f, lv1 = S * 0.65f;
                f.rect(0x48000000, lu0 + 1.5f, lv0 + 2, lu1 + 1.5f, lv1 + 2);
                f.rect(0xFF5A4A40, lu0, lv0, lu1, lv1);
                glass(f, lu0 + 1, lv0 + 1, lu1 - 1, (lv0 + lv1) / 2, 0xFF9AB4C4, 3);
                glass(f, lu0 + 1, (lv0 + lv1) / 2, lu1 - 1, lv1 - 1, 0xFF6A8494, 3);
                f.line(0xFFC8A040, 0.8f, lu0 + 1, (lv0 + lv1) / 2, lu1 - 1, (lv0 + lv1) / 2);
                f.circle(0x48000000, L - 8, 8.5f, 3.6f);
                f.circle(0xFF6A4A30, L - 9, 7.5f, 3.6f);
                f.circle(0xFF8A6440, L - 9.5f, 7, 2.6f);
                f.rect(0xFF6A6A6E, L * 0.3f, S * 0.65f, L * 0.3f + 3, S * 0.65f + 3);
                f.sign(city, "JAZZ", L / 2, S / 2, 5, 0xFFF0C860);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.OLD | Variants.MID;
        d.minS = 9;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_BAR;
        d.roofCol = 0xFF4A4448;
        out.add(d);
    }

    // ------------------------------------------------------------------ 11 Vaudeville Theatre

    static void vaudeville(List<Landmarks.Def> out) {
        // An old variety theatre: a domed and gilded front, the auditorium roof, a tall plain fly tower, a bulb-lit marquee.
        D d = new D("Vaudeville Theatre", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                return new int[][]{B(1, 1, L - 2, Math.max(5, S - 4), 4)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float fv = (q[0][1] + q[0][3]) * T, uc = L / 2;
                f.rect(0xFFB0A898, 0, 0, L, S);
                for (float u = 0; u < L; u += 5) f.line(0x16000000, 0.4f, u, 0, u, S);
                for (float v = 0; v < S; v += 5) f.line(0x16000000, 0.4f, 0, v, L, v);
                f.rect(0xFF8A847A, 0, 0, L, T * 0.8f);
                // Red carpet from the doors, brass stanchions and rope down each side.
                float cw = Math.min(16, L * 0.12f);
                f.rect(0xFF9A2A2A, uc - cw, fv, uc + cw, S);
                f.rect(0xFFB0383A, uc - cw + 1, fv, uc + cw - 1, S);
                for (float v = fv + 10; v < S - 1; v += 5) {
                    f.circle(0xFFC8A040, uc - cw - 2, v, 0.9f);
                    f.circle(0xFFC8A040, uc + cw + 2, v, 0.9f);
                }
                f.line(0xFF8A1A1A, 0.6f, uc - cw - 2, fv + 10, uc - cw - 2, S - 2);
                f.line(0xFF8A1A1A, 0.6f, uc + cw + 2, fv + 10, uc + cw + 2, S - 2);
                // The marquee: a deep canopy over the pavement, its edge ringed with bulbs and its top lettered.
                float mw = Math.min(L * 0.36f, 60), md = Math.min(9, (S - fv) * 0.6f);
                f.rect(0x50000000, uc - mw + 1.5f, fv + 1, uc + mw + 1.5f, fv + md + 2);
                f.rect(0xFF2A2A30, uc - mw, fv, uc + mw, fv + md);
                f.rect(0xFFC8A040, uc - mw + 0.8f, fv + 0.8f, uc + mw - 0.8f, fv + md - 0.8f);
                f.rect(0xFF3A2A3A, uc - mw + 1.8f, fv + 1.8f, uc + mw - 1.8f, fv + md - 1.8f);
                for (float u = uc - mw + 1.2f; u < uc + mw; u += 2) {
                    f.circle(0xFFFFF0B0, u, fv + md - 0.6f, 0.5f);
                    f.circle(0xFFFFF0B0, u, fv + 0.6f, 0.5f);
                }
                for (float v = fv + 2.6f; v < fv + md - 1; v += 2) {
                    f.circle(0xFFFFF0B0, uc - mw + 0.6f, v, 0.5f);
                    f.circle(0xFFFFF0B0, uc + mw - 0.6f, v, 0.5f);
                }
                // Poster cases, a ticket booth and lamps along the front.
                for (int s = -1; s <= 1; s += 2) {
                    for (float k = 0; k < 2; k++) {
                        float u = uc + s * (mw + 6 + k * 9);
                        if (u < 4 || u > L - 4) continue;
                        f.rect(0xFFC8A040, u - 3, fv + 0.2f, u + 3, fv + 1.8f);
                        f.rect(0xFFD87A5A, u - 2.4f, fv + 0.5f, u + 2.4f, fv + 1.5f);
                    }
                }
                float bu = uc + mw + 8, bv = fv + md + 6;
                if (bu + 6 < L && bv + 6 < S) {
                    f.rect(0x48000000, bu - 4 + 1, bv - 4 + 1.4f, bu + 4 + 1, bv + 4 + 1.4f);
                    f.rect(0xFF6A2A2A, bu - 4, bv - 4, bu + 4, bv + 4);
                    dome(f, bu, bv, 3.4f, 0xFFC8A040, 8);
                }
                for (float u = 8; u < L - 4; u += 36) f.lamp(u, S - 3);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                f.rect(0xFF8A6A50, 0, 0, L, S);
                f.rect(0xFF6E6A66, 1, 1, L - 1, S - 1);
                // The fly tower at the back: a high brick box with smoke vents (its own deeper shadow).
                float fu0 = L * 0.24f, fu1 = L * 0.76f, fv1 = S * 0.34f;
                f.rect(0x60000000, fu0 + 3, 1 + 4, fu1 + 4, fv1 + 5);
                f.rect(0xFF8A5040, fu0, 0.5f, fu1, fv1);
                f.rect(0xFF9A6050, fu0 + 1, 1.5f, fu1 - 1, fv1 - 1);
                for (float u = fu0 + 5; u < fu1 - 5; u += 8) {
                    f.rect(0xFF5A4A40, u, fv1 * 0.35f, u + 4, fv1 * 0.35f + 4);
                    f.rect(0xFF3A302A, u + 0.8f, fv1 * 0.35f + 0.8f, u + 3.2f, fv1 * 0.35f + 3.2f);
                }
                // Dressing-room wings either side, with plant.
                hvac(f, 4, 4, 6, 4);
                hvac(f, L - 11, 4, 6, 4);
                // The auditorium roof: green copper hip with a round ventilator.
                float av0 = fv1 + 1, av1 = S * 0.72f;
                hip(f, 0xFF5E9282, 3, av0, L - 3, av1, true);
                dome(f, L / 2, (av0 + av1) / 2, Math.min(6, (av1 - av0) * 0.25f), 0xFF7AAA98, 10);
                f.circle(0xFFC8A040, L / 2, (av0 + av1) / 2, 0.8f);
                // The front: a mansard band with corner domes and a gilded central gable.
                float fv0 = av1 + 1;
                f.rect(0xFFD8CCB0, 0.5f, fv0, L - 0.5f, S - 0.5f);
                f.rect(0xFF545C68, 2, fv0 + 1, L - 2, S - 2.5f);
                f.rect(0xFF6A727E, 2, fv0 + 1, L - 2, fv0 + (S - fv0) * 0.4f);
                for (float u = 6; u < L - 5; u += 6) f.rect(0xFFE0D8C4, u, S - 4.5f, u + 2, S - 2.5f);
                float cr = Math.min(6, (S - fv0) * 0.5f);
                f.rect(0xFFD8CCB0, 0.5f, S - 2 * cr - 1, 2 * cr + 1, S - 0.5f);
                f.rect(0xFFD8CCB0, L - 2 * cr - 1, S - 2 * cr - 1, L - 0.5f, S - 0.5f);
                dome(f, cr + 0.8f, S - cr - 0.8f, cr, 0xFF6EA090, 10);
                dome(f, L - cr - 0.8f, S - cr - 0.8f, cr, 0xFF6EA090, 10);
                f.circle(0xFFE0C050, cr + 0.8f, S - cr - 0.8f, 0.9f);
                f.circle(0xFFE0C050, L - cr - 0.8f, S - cr - 0.8f, 0.9f);
                float gw = L * 0.12f;
                f.rect(0x50000000, L / 2 - gw + 1.5f, fv0 + 1.5f, L / 2 + gw + 1.5f, S + 1);
                f.rect(0xFFE0D4B8, L / 2 - gw - 1, fv0, L / 2 + gw + 1, S);
                gableV(f, 0xFFB89A50, L / 2 - gw, fv0 + 0.6f, L / 2 + gw, S - 1.2f, 1.6f);
                f.circle(0xFFE8C860, L / 2, S - 1.5f, 1.4f);
                f.circle(0xFFE8C860, L / 2, fv0 + 1.5f, 1.1f);
                f.sign(city, "VAUDEVILLE", L / 2, (av0 + av1) / 2 + 8, 5, 0xFFF4E4B0);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.OLD;
        d.minS = 10;
        d.minL = 12;
        d.base = City.OFFICE;
        d.inside = Variants.I_SEATS;
        d.roofCol = 0xFF5E9282;
        out.add(d);
    }

    // ------------------------------------------------------------------ 12 Comedy Club & Bar

    /** A patio heater: a round steel hood with its glowing ring. */
    static void heater(F f, float u, float v) {
        f.circle(0x40000000, u + 0.8f, v + 1.1f, 2);
        f.circle(0xFF7A7E84, u, v, 2);
        f.circle(0xFFB0B4BA, u - 0.5f, v - 0.5f, 1);
        f.ring(0xA0F08040, 0.5f, u, v, 1.4f);
    }

    static void comedyClub(List<Landmarks.Def> out) {
        // A brick club with a rooftop bar of festoon lights and a painted spotlit mic, a roped queue and a heated yard.
        D d = new D("Comedy Club & Bar", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                return new int[][]{B(1, 1, L - 2, Math.max(4, S / 2), 2)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float fv = (q[0][1] + q[0][3]) * T;
                f.rect(0xFF6A6460, 0, 0, L, S);
                f.specks(r, 60, 0x18000000, 0x12FFFFFF, 0, 0, L, S, 1.6f);
                // The pavement before the door, then the yard laid with old setts.
                f.rect(0xFFA8A296, 0, fv, L, fv + 12);
                for (float u = 0; u < L; u += 4) f.line(0x18000000, 0.4f, u, fv, u, fv + 12);
                f.rect(0xFF8A8278, 0, fv + 12, L, S);
                for (float v = fv + 14; v < S; v += 2.5f) f.line(0x1E000000, 0.4f, 0, v, L, v);
                // Queue: brass posts and red rope along the front, poster frames, a doorman's lamp.
                float qu0 = L * 0.12f, qu1 = L * 0.5f;
                for (float u = qu0; u <= qu1; u += 6) {
                    f.circle(0x40000000, u + 0.5f, fv + 7.7f, 1);
                    f.circle(0xFFC8A040, u, fv + 7, 1);
                }
                f.line(0xFFA82A2A, 0.7f, qu0, fv + 7, qu1, fv + 7);
                for (float u = L * 0.55f; u < L - 6; u += 10) {
                    f.rect(0xFF1E1E22, u, fv + 0.2f, u + 6, fv + 2);
                    f.rect(r.nextBoolean() ? 0xFFE0B040 : 0xFFD05A4A, u + 0.6f, fv + 0.6f, u + 5.4f, fv + 1.6f);
                }
                // The yard: a brick wall, heaters, barrel tables, a food truck at the end.
                f.rect(0xFF8A4A36, 0, S - 3, L, S);
                for (float u = 0; u < L; u += 3) f.line(0x30000000, 0.4f, u, S - 3, u, S);
                float yv = (fv + 12 + S - 3) / 2;
                if (S - 3 - (fv + 12) > 14) {
                    for (float u = 12; u < L - 26; u += 20) {
                        f.circle(0x40000000, u + 0.8f, yv + 1, 2.6f);
                        f.circle(0xFF6A4A30, u, yv, 2.6f);
                        f.ring(0xFF3A3A3A, 0.4f, u, yv, 2.1f);
                        f.circle(0xFF3A3A3A, u - 4.5f, yv, 1);
                        f.circle(0xFF3A3A3A, u + 4.5f, yv, 1);
                        heater(f, u + 10, yv - 4);
                    }
                    float tu = L - 14;
                    f.rect(0x48000000, tu - 10 + 1.5f, yv - 5 + 2, tu + 10 + 1.5f, yv + 5 + 2);
                    f.roundRect(0xFF3A6A8A, tu - 10, yv - 5, tu + 10, yv + 5, 1.5f);
                    f.rect(0xFF2A333C, tu + 6, yv - 4, tu + 8.5f, yv + 4);
                    f.rect(0xFFE0E0D8, tu - 8, yv - 5, tu + 4, yv - 3.6f);
                    f.rect(0xFFF0C040, tu - 8, yv - 5.8f, tu + 4, yv - 5);
                }
                int[] lc = {0xFFF8E8A0, 0xFFF4C070};
                string(f, 2, fv + 12, L - 2, S - 3, lc, 4, 0.55f);
                string(f, 2, S - 3, L - 2, fv + 12, lc, 4, 0.55f);
                f.lamp(4, fv + 4);
                f.lamp(L - 4, fv + 4);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                // Brick parapet; one half a tarred roof with the club's painted mic and spotlight.
                f.rect(0xFF7A4232, 0, 0, L, S);
                f.rect(0xFF4E4A4A, 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                float h = L * 0.48f;
                float cu = h * 0.5f, cv = S / 2, sr = Math.min(h, S) * 0.36f;
                f.circle(0xFFE8D890, cu, cv, sr);
                f.circle(0xFFF4E8B0, cu - sr * 0.15f, cv - sr * 0.15f, sr * 0.7f);
                f.curve(0xFF2A2A2E, sr * 0.14f, cu - sr * 0.1f, cv + sr * 0.15f, cu + sr * 0.4f, cv + sr * 0.6f, cu + sr * 0.9f, cv + sr * 0.5f);
                f.circle(0xFF3A3A3E, cu - sr * 0.2f, cv - sr * 0.1f, sr * 0.3f);
                f.circle(0xFF6A6A70, cu - sr * 0.25f, cv - sr * 0.15f, sr * 0.22f);
                for (float k = -0.4f; k <= 0.41f; k += 0.2f) {
                    f.line(0x50000000, 0.4f, cu - sr * 0.47f, cv - sr * 0.1f + sr * k * 0.6f, cu + sr * 0.07f, cv - sr * 0.1f + sr * k * 0.6f);
                    f.line(0x50000000, 0.4f, cu - sr * 0.2f + sr * k * 0.6f, cv - sr * 0.37f, cu - sr * 0.2f + sr * k * 0.6f, cv + sr * 0.17f);
                }
                hvac(f, 3, 3, 6, 4);
                // The rooftop bar: decking, a bar counter, high tables, planters, festoons and parasols.
                f.rect(0xFF9A7A54, h, 1.5f, L - 1.5f, S - 1.5f);
                for (float v = 3, k = 0; v < S - 1.5f; v += 1.8f, k++) f.line(0x22000000, 0.4f, h, v, L - 1.5f, v);
                f.rect(0x40000000, h + 2 + 1, 3 + 1.2f, h + 6 + 1, S - 3 + 1.2f);
                f.rect(0xFF4A3424, h + 2, 3, h + 6, S - 3);
                f.rect(0xFF8A6A4A, h + 2.6f, 3.6f, h + 5.4f, S - 3.6f);
                for (float v = 5; v < S - 4; v += 3) f.circle(0xFF2A2A2A, h + 7.2f, v, 0.6f);
                for (float u = h + 14; u < L - 5; u += 14)
                    for (float v = 8; v < S - 6; v += 13) cafeTable(f, u, v, 0xFF2A2A2E, 0xFF5A5A60, 3);
                for (float u = h + 12; u < L - 4; u += 8) pot(f, u, S - 3, 1.5f, 0);
                parasol(f, L - 8, 7, 5, 0xFF2A4A5A, 0xFF3A5A6A);
                int[] lc = {0xFFF8E8A0, 0xFFF4C070};
                string(f, h + 1, 2, L - 2, S - 2, lc, 3, 0.5f);
                string(f, h + 1, S - 2, L - 2, 2, lc, 3, 0.5f);
                f.sign(city, "COMEDY", h * 0.5f, S - 4, 4, 0xFFF4E4B0);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.MID | Variants.UNI;
        d.minS = 10;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_BAR;
        d.roofCol = 0xFF4E4A4A;
        out.add(d);
    }

    // ------------------------------------------------------------------ 13 Stock Exchange

    /** A bronze bull from above, head towards +u. */
    static void bull(F f, float u, float v, float s) {
        f.oval(0x40000000, u - 6 * s + 1.5f, v - 2.6f * s + 2, u + 5 * s + 1.5f, v + 2.6f * s + 2);
        f.oval(0xFF5A4028, u - 6 * s, v - 2.6f * s, u + 4 * s, v + 2.6f * s);
        f.oval(0xFF7A5634, u - 4 * s, v - 2 * s, u + 2.5f * s, v + 0.6f * s);
        f.oval(0xFF4A3420, u + 3 * s, v - 1.6f * s, u + 6.5f * s, v + 1.6f * s);
        f.curve(0xFFB89A5A, 0.8f * s, u + 4 * s, v - 1.4f * s, u + 5.5f * s, v - 3.4f * s, u + 7.5f * s, v - 2.8f * s);
        f.curve(0xFFB89A5A, 0.8f * s, u + 4 * s, v + 1.4f * s, u + 5.5f * s, v + 3.4f * s, u + 7.5f * s, v + 2.8f * s);
        f.curve(0xFF4A3420, 0.6f * s, u - 6 * s, v, u - 8 * s, v - 1 * s, u - 7.5f * s, v - 3 * s);
    }

    static void stockExchange(List<Landmarks.Def> out) {
        // A classical exchange: a temple front of columns under a pediment, a glazed trading-floor roof, and a bull outside.
        D d = new D("Stock Exchange", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                return new int[][]{B(1, 1, L - 2, Math.max(5, S - 5), 5)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float fv = (q[0][1] + q[0][3]) * T, uc = L / 2;
                // Granite setts, a band of polished slabs before the steps.
                f.rect(0xFFB8B2A8, 0, 0, L, S);
                for (float u = 0; u < L; u += 8) f.line(0x18000000, 0.4f, u, fv, u, S);
                for (float v = fv; v < S; v += 8) f.line(0x18000000, 0.4f, 0, v, L, v);
                f.rect(0xFF9A948A, 0, 0, L, T * 0.8f);
                // The portico columns stand on the top step, the flight runs down to the square.
                float pw = L * 0.3f;
                float st = Math.min(12, (S - fv) * 0.42f);
                for (int k = 0; k < 6; k++) {
                    float v = fv + k * st / 6, w = k * 1.5f;
                    f.rect(k % 2 == 0 ? 0xFFDAD4C6 : 0xFFCCC6B8, uc - pw - 3 - w, v, uc + pw + 3 + w, v + st / 6);
                    f.line(0x30000000, 0.5f, uc - pw - 3 - w, v + st / 6, uc + pw + 3 + w, v + st / 6);
                }
                for (float u = uc - pw + 2; u <= uc + pw - 1; u += (2 * pw - 3) / 7) {
                    f.circle(0x40000000, u + 0.8f, fv + 2.4f, 2);
                    f.circle(0xFFE8E2D4, u, fv + 1.6f, 2);
                    f.circle(0xFFF8F4EA, u - 0.6f, fv + 1, 0.8f);
                }
                // Flagpoles, the charging bull, lamp standards and bollards.
                float bv = fv + st + (S - fv - st) / 2;
                if (S - fv - st > 14) {
                    f.rect(0xFF9A948A, uc + pw + 10, bv - 7, uc + pw + 38, bv + 7);
                    f.rect(0xFF8A847A, uc + pw + 11, bv - 6, uc + pw + 37, bv + 6);
                    bull(f, uc + pw + 24, bv, 1.7f);
                }
                for (int k = 0; k < 3; k++) {
                    float u = uc - pw - 30 + k * 8;
                    if (u < 3) continue;
                    f.circle(0x40000000, u + 0.6f, bv + 0.8f, 1);
                    f.circle(0xFFD0D0D0, u, bv, 0.9f);
                    int[] fc = {0xFF2A4A8A, 0xFFB83A3A, 0xFFE8E8E8};
                    f.poly(fc[k], u + 0.6f, bv - 0.6f, u + 6, bv - 1.4f, u + 6, bv + 1.2f, u + 0.6f, bv + 0.6f);
                }
                for (float u = 4; u < L - 2; u += 7) {
                    if (Math.abs(u - uc) < pw + 8) continue;
                    f.circle(0xFF3A3A3E, u, S - 3, 0.9f);
                }
                f.lamp(uc - pw - 6, fv + st);
                f.lamp(uc + pw + 6, fv + st);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                // Portland stone walls under a lead flat roof with a balustrade all round.
                f.rect(0xFFD2CAB8, 0, 0, L, S);
                f.rect(0xFF8E9094, 2.4f, 2.4f, L - 2.4f, S - 2.4f);
                for (float u = 2.6f; u < L - 2; u += 1.6f) {
                    f.circle(0xFFE6E0D2, u, 1.2f, 0.5f);
                    f.circle(0xFFE6E0D2, u, S - 1.2f, 0.5f);
                }
                for (float v = 2.6f; v < S - 2; v += 1.6f) {
                    f.circle(0xFFE6E0D2, 1.2f, v, 0.5f);
                    f.circle(0xFFE6E0D2, L - 1.2f, v, 0.5f);
                }
                // The trading floor: a great iron-ribbed glass roof in the middle.
                float gu0 = L * 0.22f, gu1 = L * 0.78f, gv0 = S * 0.2f, gv1 = S * 0.62f;
                f.rect(0xFF6E7074, gu0 - 1, gv0 - 1, gu1 + 1, gv1 + 1);
                glass(f, gu0, gv0, gu1, (gv0 + gv1) / 2, 0xFF9AB8C8, 3);
                glass(f, gu0, (gv0 + gv1) / 2, gu1, gv1, 0xFF7A98AA, 3);
                f.line(0xFF4A4C50, 1, gu0, (gv0 + gv1) / 2, gu1, (gv0 + gv1) / 2);
                for (float u = gu0 + 6; u < gu1; u += 6) f.line(0xFF4A4C50, 0.7f, u, gv0, u, gv1);
                // Corner pavilions with low hipped lead roofs.
                float cw = Math.min(10, L * 0.12f);
                hip(f, 0xFF6E747A, 2.4f, 2.4f, 2.4f + cw, 2.4f + cw, false);
                hip(f, 0xFF6E747A, L - 2.4f - cw, 2.4f, L - 2.4f, 2.4f + cw, false);
                // The temple front: a pediment roof over the portico, ridge running to the street.
                float pw = L * 0.3f, pv0 = S * 0.68f;
                f.rect(0x50000000, L / 2 - pw + 1.5f, pv0 + 1.5f, L / 2 + pw + 1.5f, S + 1.5f);
                f.rect(0xFFDAD2C0, L / 2 - pw, pv0, L / 2 + pw, S);
                gableV(f, 0xFF7A8088, L / 2 - pw + 1.5f, pv0 + 1, L / 2 + pw - 1.5f, S - 0.6f, 2);
                f.rect(0xFFE6DECC, L / 2 - pw, S - 1.6f, L / 2 + pw, S);
                f.circle(0xFFD8B040, L / 2, S - 1, 1.2f);
                // Flagpoles on the roof and a lift motor room.
                hvac(f, L - 12, S * 0.66f - 5, 7, 4);
                f.circle(0xFFE8E8E8, 6, S - 6, 0.8f);
                f.circle(0xFFE8E8E8, L - 6, S - 6, 0.8f);
                f.sign(city, "EXCHANGE", L / 2, (gv0 + gv1) / 2, 5, 0xFFF4EED8);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN;
        d.minS = 10;
        d.minL = 12;
        d.base = City.OFFICE;
        d.inside = Variants.I_OFFICE;
        d.roofCol = 0xFF8E9094;
        out.add(d);
    }

    // ------------------------------------------------------------------ 14 Bank Headquarters

    static void bankHQ(List<Landmarks.Def> out) {
        // A stepped granite tower with a roof garden on its setback, a granite plaza with a fountain grid and a sculpture.
        D d = new D("Bank Headquarters", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                int w = Math.max(6, Math.min(9, L / 2)), h = Math.max(6, Math.min(9, S - 3));
                return new int[][]{B(1, (S - h) / 2, w, h, 16)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float tu1 = (q[0][0] + q[0][2]) * T;
                // Polished granite paving in two tones.
                f.rect(0xFFA8A09A, 0, 0, L, S);
                for (float u = 0, k = 0; u < L; u += 8, k++)
                    for (float v = 0, j = 0; v < S; v += 8, j++)
                        if ((k + j) % 2 == 0) f.rect(0xFFB4ACA6, u, v, u + 8, v + 8);
                f.frame(0x30000000, 0.6f, T - 3, q[0][1] * T - 3, tu1 + 3, (q[0][1] + q[0][3]) * T + 3);
                // A grid of ground jets in a shallow dark pool.
                float pu0 = tu1 + 10, pu1 = L - 10, pv0 = S * 0.18f, pv1 = S * 0.45f;
                if (pu1 - pu0 > 16) {
                    f.rect(0xFF7A7470, pu0 - 1.5f, pv0 - 1.5f, pu1 + 1.5f, pv1 + 1.5f);
                    f.rect(0xFF2E4654, pu0, pv0, pu1, pv1);
                    f.rect(0xFF3A5868, pu0 + 1, pv0 + 1, pu1 - 1, pv1 - 1);
                    for (float u = pu0 + 4; u < pu1 - 2; u += 6)
                        for (float v = pv0 + 4; v < pv1 - 2; v += 6) {
                            f.circle(0x70FFFFFF, u, v, 1.4f);
                            f.circle(0xC0FFFFFF, u, v, 0.6f);
                        }
                }
                // The sculpture: a great polished steel ellipse twisting on a low black plinth.
                float su = (tu1 + L) / 2, sv = S * 0.72f, sr = Math.min(12, (L - tu1) * 0.2f);
                f.rect(0xFF2A2A2E, su - sr - 3, sv - sr * 0.6f - 3, su + sr + 3, sv + sr * 0.6f + 3);
                f.oval(0x40000000, su - sr + 2, sv - sr * 0.5f + 3, su + sr + 2, sv + sr * 0.5f + 3);
                f.oval(0xFF8A9098, su - sr, sv - sr * 0.5f, su + sr, sv + sr * 0.5f);
                f.oval(0xFFC0C6CE, su - sr * 0.9f, sv - sr * 0.45f, su + sr * 0.5f, sv + sr * 0.2f);
                f.oval(0xFF2A2A2E, su - sr * 0.45f, sv - sr * 0.18f, su + sr * 0.45f, sv + sr * 0.18f);
                f.oval(0xFFE8ECF0, su - sr * 0.7f, sv - sr * 0.35f, su - sr * 0.2f, sv - sr * 0.15f);
                // Plane trees in granite planters, benches and bollards on the street edges.
                for (float u = tu1 + 10; u < L - 6; u += 22) {
                    f.rect(0xFF8A827C, u - 5, S - 11, u + 5, S - 1);
                    f.tree(u, S - 6, 6);
                    f.rect(0xFF8A827C, u - 5, 1, u + 5, 11);
                    f.tree(u, 6, 6);
                }
                for (float v = 12; v < S - 12; v += 7) f.circle(0xFF3A3A3E, L - 2.5f, v, 0.9f);
                f.bench(su, sv + sr * 0.6f + 8, true);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                // Pink-grey granite: the base edge, the setback terrace, the upper tower and its crown.
                f.rect(0xFF8A7A76, 0, 0, L, S);
                f.rect(0xFFA89894, 0.8f, 0.8f, L - 0.8f, S - 0.8f);
                for (float u = 3; u < L - 1; u += 3) f.line(0x20000000, 0.5f, u, 0.8f, u, S - 0.8f);
                float m = Math.min(L, S) * 0.16f;
                // The roof garden on the setback: lawn, a clipped hedge border, small trees and paths.
                f.rect(0xFF6A9450, 2.4f, 2.4f, L - 2.4f, S - 2.4f);
                f.frame(0xFF4E7A3A, 1.6f, 3.2f, 3.2f, L - 3.2f, S - 3.2f);
                f.rect(0xFFD0C4B0, 2.4f, S / 2 - 1.2f, L - 2.4f, S / 2 + 1.2f);
                f.tree(2.4f + m * 0.5f, 2.4f + m * 0.5f, m * 0.4f);
                f.tree(L - 2.4f - m * 0.5f, S - 2.4f - m * 0.5f, m * 0.4f);
                f.bush(L - 2.4f - m * 0.5f, 2.4f + m * 0.5f, m * 0.3f);
                f.bush(2.4f + m * 0.5f, S - 2.4f - m * 0.5f, m * 0.3f);
                // The upper tower: a deep shadow, its granite frame and glazed panels, then the crown.
                float a = 2.4f + m, b = L - 2.4f - m, c0 = 2.4f + m, c1 = S - 2.4f - m;
                f.rect(0x60000000, a + 2.5f, c0 + 3.5f, b + 3, c1 + 4);
                f.rect(0xFF9A8A86, a, c0, b, c1);
                f.rect(0xFFB4A4A0, a + 1, c0 + 1, b - 1, c1 - 1);
                for (float u = a + 2.5f; u < b - 1; u += 2.5f) f.line(0x26000000, 0.5f, u, c0 + 1, u, c1 - 1);
                float n = Math.min(b - a, c1 - c0) * 0.2f;
                f.rect(0x50000000, a + n + 1.5f, c0 + n + 2, b - n + 1.5f, c1 - n + 2);
                f.rect(0xFF7A6E6A, a + n, c0 + n, b - n, c1 - n);
                f.rect(0xFF8E8280, a + n + 1, c0 + n + 1, b - n - 1, c1 - n - 1);
                hvac(f, a + n + 1.5f, c0 + n + 1.5f, Math.min(6, b - a - 2 * n - 3), 4);
                f.circle(0x40000000, b - n - 3, c1 - n - 2, 1.6f);
                f.circle(0xFFD0D0D4, b - n - 3.6f, c1 - n - 2.6f, 1.2f);
                f.line(0xFFD0D0D4, 0.5f, b - n - 3.6f, c1 - n - 2.6f, b - n - 3.6f, c1 - n - 8);
                f.sign(city, "BANK", L / 2, S / 2, 5, 0xFFF0E8D8);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN;
        d.minS = 10;
        d.minL = 14;
        d.base = City.OFFICE;
        d.inside = Variants.I_OFFICE;
        d.roofCol = 0xFFA89894;
        out.add(d);
    }

    // ------------------------------------------------------------------ 15 Corporate Atrium

    static int[][] atriumUV(int L, int S) {
        int tw = L >= 18 ? 5 : L >= 16 ? 4 : 3, dep = Math.max(5, S - 4);
        int aw = L - 2 - 2 * tw - 2;
        return new int[][]{B(1, 1, tw, dep, 12), B(2 + tw, 2, aw, dep - 2, 6), B(L - 1 - tw, 1, tw, dep, 14)};
    }

    static void corporateAtrium(List<Landmarks.Def> out) {
        // Two dark glass towers joined by a tall glazed atrium full of trees, glass links, a long reflecting pool.
        D d = new D("Corporate Atrium", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                return atriumUV(L, S);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float fv = (q[0][1] + q[0][3]) * T;
                // Pale stone plaza on a fine grid.
                f.rect(0xFFC8C4BC, 0, 0, L, S);
                for (float u = 0; u < L; u += 4) f.line(0x14000000, 0.4f, u, 0, u, S);
                for (float v = 0; v < S; v += 4) f.line(0x14000000, 0.4f, 0, v, L, v);
                // Glass links over the gaps between the towers and the atrium.
                for (int i = 0; i < 2; i++) {
                    float gu0 = (q[i][0] + q[i][2]) * T, gu1 = q[i + 1][0] * T;
                    float lv0 = (q[1][1]) * T + 6, lv1 = (q[1][1] + q[1][3]) * T - 6;
                    f.rect(0x40000000, gu0 + 1, lv0 + 2, gu1 + 1, lv1 + 2);
                    glass(f, gu0, lv0, gu1, lv1, 0xFF8AA8BC, 3);
                }
                // A long, narrow reflecting pool along the front, with a row of trees in grates.
                float pv0 = fv + 5, pv1 = Math.min(S - 6, pv0 + 6);
                f.rect(0xFF6A6A6E, 6, pv0 - 1, L - 6, pv1 + 1);
                f.rect(0xFF2A3E4E, 7, pv0, L - 7, pv1);
                f.rect(0x30FFFFFF, 9, pv0 + 1, L * 0.4f, pv0 + 2);
                for (float u = 10; u < L - 6; u += 18) {
                    float v = Math.min(S - 4, pv1 + 6);
                    f.frame(0xFF6A6A6E, 0.6f, u - 3, v - 3, u + 3, v + 3);
                    f.tree(u, v, 4.5f, 0xFF4A8A44);
                }
                // Bollards and a bike rack by the entrance, and a corporate logo stone.
                float uc = L / 2;
                f.rect(0xFF3A3C40, uc - 7, fv + 1, uc + 7, fv + 3.5f);
                f.rect(0xFFC8A040, uc - 6, fv + 1.6f, uc + 6, fv + 2.9f);
                for (float u = 3; u < L - 2; u += 6) f.circle(0xFF5A5C60, u, S - 1.5f, 0.8f);
                f.lamp(4, fv + 3);
                f.lamp(L - 4, fv + 3);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                if (part == 1) {
                    // The atrium: a triangulated glass roof, ridge lit, trees showing through its panes.
                    f.rect(0xFF5A6068, 0, 0, L, S);
                    f.rect(0xFF3E5A6A, 1, 1, L - 1, S - 1);
                    for (int k = 0; k < 7; k++) {
                        float u = 3 + r.nextFloat() * (L - 6), v = 3 + r.nextFloat() * (S - 6);
                        f.circle(0xFF3E6A44, u, v, 2.4f + r.nextFloat() * 1.6f);
                    }
                    f.rect(0x6090B4C8, 1, 1, L - 1, S / 2);
                    f.rect(0x40607888, 1, S / 2, L - 1, S - 1);
                    for (float u = 1; u < L; u += 4) {
                        f.line(0xFFB8C4CC, 0.4f, u, 1, u + 4, S / 2);
                        f.line(0xFFB8C4CC, 0.4f, u + 4, 1, u, S / 2);
                        f.line(0xFF8A98A4, 0.4f, u, S / 2, u + 4, S - 1);
                        f.line(0xFF8A98A4, 0.4f, u + 4, S / 2, u, S - 1);
                    }
                    f.line(0xFFE0E8EE, 1.2f, 1, S / 2, L - 1, S / 2);
                    f.frame(0xFF8A949E, 0.8f, 1, 1, L - 1, S - 1);
                } else {
                    // A tower: dark glass curtain wall, a recessed crown and its plant screen.
                    f.rect(0xFF2A3644, 0, 0, L, S);
                    f.rect(0xFF3A4C5E, 0.8f, 0.8f, L - 0.8f, S - 0.8f);
                    f.poly(0xFF4A6074, 0.8f, 0.8f, L - 0.8f, 0.8f, 0.8f, S * 0.5f);
                    for (float u = 2.5f; u < L - 1; u += 2.5f) f.line(0x30FFFFFF, 0.4f, u, 0.8f, u, S - 0.8f);
                    float m = Math.min(L, S) * 0.2f;
                    f.rect(0x50000000, m + 1.5f, m + 2, L - m + 1.5f, S - m + 2);
                    f.rect(0xFF5A626C, m, m, L - m, S - m);
                    for (float v = m + 1.5f; v < S - m; v += 1.5f) f.line(0x40000000, 0.5f, m, v, L - m, v);
                    hvac(f, m + 1, m + 2, Math.max(3, L - 2 * m - 2), 4);
                    f.circle(0xFFE04040, L / 2, S - m - 2, 0.7f);
                    if (part == 2) f.sign(city, "ATRIUM", L / 2, S * 0.75f, 4, 0xFFE0E8F0);
                }
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.MID;
        d.minS = 10;
        d.minL = 14;
        d.base = City.OFFICE;
        d.inside = Variants.I_OFFICE;
        d.roofCol = 0xFF3A4C5E;
        out.add(d);
    }

    // ------------------------------------------------------------------ 16 Fish Market Hall

    /** A stack of fish crates: blue plastic with silver fish, or white boxes of ice. */
    static void fishCrate(F f, Random r, float u, float v, boolean ice) {
        f.rect(0x38000000, u + 0.6f, v + 0.8f, u + 6.6f, v + 4.8f);
        if (ice) {
            f.rect(0xFFE8ECEE, u, v, u + 6, v + 4);
            f.rect(0xFFD0DCE4, u + 0.6f, v + 0.6f, u + 5.4f, v + 3.4f);
            for (int k = 0; k < 4; k++) f.rect(0xFFF8FCFF, u + 1 + r.nextFloat() * 3.4f, v + 0.9f + r.nextFloat() * 2.2f, u + 1.8f + r.nextFloat() * 3.4f, v + 1.6f + r.nextFloat() * 2.2f);
        } else {
            f.rect(0xFF2E5A8A, u, v, u + 6, v + 4);
            f.rect(0xFF3E6E9E, u + 0.5f, v + 0.5f, u + 5.5f, v + 3.5f);
            for (int k = 0; k < 3; k++) {
                float y = v + 1 + k * 0.9f;
                f.oval(0xFFB8C0C8, u + 0.9f + (k % 2) * 0.6f, y - 0.4f, u + 4.6f + (k % 2) * 0.6f, y + 0.4f);
            }
        }
    }

    static void fishMarket(List<Landmarks.Def> out) {
        // A barrel-roofed fish hall on a quay: crates of fish and ice, striped stalls, a moored trawler and the gulls.
        D d = new D("Fish Market Hall", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                return new int[][]{B(1, 1, L - 2, Math.max(4, S * 45 / 100), 2)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float fv = (q[0][1] + q[0][3]) * T;
                // Wet concrete quay, a dock basin along the front edge with timber fenders and bollards.
                f.rect(0xFF9C9A94, 0, 0, L, S);
                for (float u = 0; u < L; u += 12) f.line(0x1C000000, 0.5f, u, 0, u, S);
                f.specks(r, 60, 0x18000000, 0x14FFFFFF, 0, 0, L, S, 1.6f);
                float wv = S - Math.min(18, (S - fv) * 0.4f);
                f.rect(0xFF2E5A6E, 0, wv, L, S);
                f.rect(0xFF3A6A80, 0, wv + 3, L, S);
                for (int k = 0; k < 10; k++) {
                    float u = r.nextFloat() * (L - 10);
                    f.line(0x40FFFFFF, 0.4f, u, wv + 4 + r.nextFloat() * (S - wv - 5), u + 6, wv + 4 + r.nextFloat() * (S - wv - 5));
                }
                f.rect(0xFFB0ACA4, 0, wv - 2.4f, L, wv);
                for (float u = 4; u < L; u += 8) f.rect(0xFF5A4232, u, wv - 0.4f, u + 2, wv + 1.2f);
                for (float u = 10; u < L - 4; u += 26) {
                    f.circle(0x40000000, u + 0.6f, wv - 3.4f, 1.4f);
                    f.circle(0xFF2A2A2A, u, wv - 4, 1.4f);
                }
                // A trawler moored alongside: hull, wheelhouse, gantry and nets.
                float bu = L * 0.62f, bv = (wv + S) / 2 + 1, bl = Math.min(36, L * 0.3f), bw = Math.min(6, (S - wv) * 0.38f);
                Path h = new Path();
                f.mv(h, bu - bl / 2, bv - bw);
                f.ln(h, bu + bl / 2 - 6, bv - bw);
                f.qd(h, bu + bl / 2 + 2, bv, bu + bl / 2 - 6, bv + bw);
                f.ln(h, bu - bl / 2, bv + bw);
                f.ln(h, bu - bl / 2, bv - bw);
                f.fill(h, 0xFF2A4A6A);
                f.rect(0xFFE8E4DA, bu - bl / 2 + 1.5f, bv - bw + 1.4f, bu + bl / 2 - 6, bv + bw - 1.4f);
                f.rect(0x40000000, bu + 1.5f, bv - bw * 0.6f + 1.4f, bu + 9.5f, bv + bw * 0.6f + 1.4f);
                f.rect(0xFFF0EEE8, bu, bv - bw * 0.6f, bu + 8, bv + bw * 0.6f);
                f.rect(0xFF2A333C, bu + 6, bv - bw * 0.5f, bu + 7.5f, bv + bw * 0.5f);
                f.line(0xFFD86A2A, 1, bu - bl / 2 + 5, bv - bw, bu - bl / 2 + 5, bv + bw);
                f.oval(0xFF4A7A5A, bu - bl / 2 + 2, bv - bw * 0.6f, bu - 2, bv + bw * 0.6f);
                f.circle(0xFFE07030, bu - 5, bv - bw * 0.4f, 1);
                // Stalls under blue and white striped awnings along the hall front.
                float sw = 20;
                int k = 0;
                for (float u = 6; u + sw < L - 4; u += sw + 8, k++) {
                    awning(f, u, fv, u + sw, fv + 5, 0xFF2E5A8A, 0xFFF0F0EC, 2.5f);
                    f.rect(0xFF8A8E92, u, fv + 5.5f, u + sw, fv + 10);
                    f.rect(0xFFE8F0F4, u + 0.6f, fv + 6, u + sw - 0.6f, fv + 9.4f);
                    for (float x = u + 1.5f; x < u + sw - 2; x += 2.4f) {
                        f.oval(k % 2 == 0 ? 0xFFB8C0C8 : 0xFFD88A6A, x, fv + 6.5f, x + 2, fv + 8.9f);
                    }
                }
                // Crates heaped about the quay, a pallet truck, and gulls.
                float cv0 = fv + 14, cv1 = wv - 8;
                for (float u = 6; u < L - 10; u += 16)
                    for (float v = cv0; v < cv1; v += 10) {
                        fishCrate(f, r, u, v, (u + v) % 3 < 1.5f);
                        fishCrate(f, r, u + 6.5f, v, (u + v) % 3 >= 1.5f);
                        if (r.nextBoolean()) fishCrate(f, r, u + 3, v + 4.6f, true);
                    }
                for (int j = 0; j < 7; j++) {
                    float u = r.nextFloat() * L, v = r.nextFloat() * S;
                    f.line(0xFFF8F8F8, 0.6f, u - 1.6f, v - 0.6f, u, v);
                    f.line(0xFFF8F8F8, 0.6f, u, v, u + 1.6f, v - 0.6f);
                }
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                // A curved corrugated roof shaded across its span, iron ribs, a glazed ridge lantern.
                int[] band = {0xFFB8BEC2, 0xFFC8CED2, 0xFFD2D8DC, 0xFFC4CACE, 0xFFAEB4B8, 0xFF9AA0A4, 0xFF888E92};
                float bh = S / band.length;
                for (int k = 0; k < band.length; k++) f.rect(band[k], 0, k * bh, L, (k + 1) * bh + 0.2f);
                for (float u = 1.2f; u < L; u += 1.2f) f.line(0x14000000, 0.3f, u, 0, u, S);
                for (float u = 8; u < L - 2; u += 10) f.line(0xFF6A7074, 0.8f, u, 0, u, S);
                float lv0 = S * 0.4f, lv1 = S * 0.55f;
                f.rect(0x40000000, 2, lv0 + 1, L - 1, lv1 + 1.5f);
                glass(f, 2, lv0, L - 2, lv1, 0xFF8AAABA, 2.5f);
                f.frame(0xFF5A6064, 0.6f, 0, 0, L, S);
                // Louvred gable ends.
                for (float v = 2; v < S - 2; v += 1.5f) {
                    f.line(0xFF6A7074, 0.4f, 0.4f, v, 2.4f, v);
                    f.line(0xFF6A7074, 0.4f, L - 2.4f, v, L - 0.4f, v);
                }
                f.sign(city, "FISH MARKET", L / 2, S * 0.25f, 5, 0xFF1E3A5A);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.IND | Variants.OLD | Variants.MID;
        d.minS = 10;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_SHELVES;
        d.roofCol = 0xFFC4CACE;
        out.add(d);
    }

    // ------------------------------------------------------------------ 17 Chandlery & Sail Loft

    /** An anchor laid flat: shank along +u from (u,v), stock across its top, curved arms at the crown. */
    static void anchor(F f, float u, float v, float s, int col) {
        f.line(0x40000000, 1.4f * s, u + 1, v + 1.4f, u + 10 * s + 1, v + 1.4f);
        f.line(col, 1.4f * s, u, v, u + 10 * s, v);
        f.line(col, 1.2f * s, u + 1.2f * s, v - 4 * s, u + 1.2f * s, v + 4 * s);
        f.ring(col, 1 * s, u - 0.6f * s, v, 0.9f * s);
        f.curve(col, 1.3f * s, u + 7 * s, v - 4.5f * s, u + 11.5f * s, v, u + 7 * s, v + 4.5f * s);
        f.poly(col, u + 7 * s, v - 5.5f * s, u + 6 * s, v - 3.8f * s, u + 8.2f * s, v - 4 * s);
        f.poly(col, u + 7 * s, v + 5.5f * s, u + 6 * s, v + 3.8f * s, u + 8.2f * s, v + 4 * s);
    }

    /** A coil of rope. */
    static void rope(F f, float u, float v, float rad) {
        f.circle(0x38000000, u + 0.8f, v + 1, rad);
        f.circle(0xFFC8B080, u, v, rad);
        for (float k = rad - 0.8f; k > 0.6f; k -= 1.1f) f.ring(0xFF9A8458, 0.4f, u, v, k);
    }

    static void chandlery(List<Landmarks.Def> out) {
        // A weatherboarded chandlery and a long sail loft over a boatyard of rope coils, anchors, buoys and a sail spread out.
        D d = new D("Chandlery & Sail Loft", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                int sw = Math.max(4, L * 35 / 100), sd = Math.max(4, Math.min(6, S / 2));
                return new int[][]{B(1, 1, sw, sd, 2), B(sw + 2, 1, L - sw - 3, Math.max(3, Math.min(4, S / 3)), 3)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float yv = (q[0][1] + q[0][3]) * T, lv = (q[1][1] + q[1][3]) * T;
                // Gravel yard with tarred timber staging.
                f.rect(0xFFA89C88, 0, 0, L, S);
                f.specks(r, 160, 0x20000000, 0x24FFFFFF, 0, 0, L, S, 1.1f);
                f.rect(0xFF6A5A48, q[0][0] * T, yv, (q[0][0] + q[0][2]) * T, yv + 5);
                for (float u = q[0][0] * T; u < (q[0][0] + q[0][2]) * T; u += 2) f.line(0x30000000, 0.4f, u, yv, u, yv + 5);
                // A sail laid out under the loft for cutting: white panels with seams and a bolt rope.
                float su0 = q[1][0] * T + 4, su1 = L - 4, sv0 = lv + 4, sv1 = Math.min(S - 6, lv + 4 + (S - lv) * 0.5f);
                if (sv1 - sv0 > 10 && su1 - su0 > 20) {
                    f.poly(0x30000000, su0 + 1, sv1 + 1.4f, su1 + 1, sv1 + 1.4f, su0 + (su1 - su0) * 0.15f + 1, sv0 + 1.4f);
                    f.poly(0xFFF4F0E4, su0, sv1, su1, sv1, su0 + (su1 - su0) * 0.15f, sv0);
                    for (float t = 0.15f; t < 1; t += 0.15f) {
                        float u = su0 + (su1 - su0) * t;
                        float top = sv0 + (sv1 - sv0) * Math.max(0, (t - 0.15f) / 0.85f);
                        f.line(0x30000000, 0.4f, u, top, u, sv1);
                    }
                    f.line(0xFFC8B080, 0.7f, su0, sv1, su1, sv1);
                    f.circle(0xFF8A2A2A, su0 + (su1 - su0) * 0.4f, sv1 - 3, 1.4f);
                }
                // Rope coils, chain, anchors, buoys and lobster pots about the yard.
                float bv = Math.max(yv + 9, sv1 + 6);
                rope(f, 8, yv + 12, 3.4f);
                rope(f, 15, yv + 13, 2.6f);
                anchor(f, 22, Math.min(S - 8, yv + 14), 1.2f, 0xFF4A4E54);
                anchor(f, L * 0.6f, Math.min(S - 8, bv + 2), 1.4f, 0xFF3A3E44);
                for (int k = 0; k < 18; k++) f.ring(0xFF5A5650, 0.6f, 8 + k * 0.6f + r.nextFloat(), S - 6 + r.nextFloat() * 2, 0.7f);
                int[] bc = {0xFFE0602A, 0xFFD83A2A, 0xFFF0C040, 0xFFE0602A};
                for (int k = 0; k < 6; k++) {
                    float u = L - 30 + (k % 3) * 5, v = S - 10 + (k / 3) * 5;
                    f.circle(0x38000000, u + 0.6f, v + 0.8f, 2);
                    f.circle(bc[k % 4], u, v, 2);
                    f.circle(0x60FFFFFF, u - 0.6f, v - 0.6f, 0.7f);
                }
                for (int k = 0; k < 3; k++) {
                    float u = L - 12, v = S - 22 + k * 5.5f;
                    if (v < bv) continue;
                    f.rect(0x38000000, u + 0.6f, v + 0.8f, u + 7.6f, v + 5.3f);
                    f.rect(0xFF6A7A5A, u, v, u + 7, v + 4.5f);
                    for (float x = u + 1; x < u + 7; x += 1.2f) f.line(0x50000000, 0.3f, x, v, x, v + 4.5f);
                }
                // A dinghy upturned on trestles.
                float du = L * 0.35f, dv = S - 9;
                if (dv > bv) {
                    f.rect(0xFF6A5040, du - 6, dv - 4, du - 5, dv + 4);
                    f.rect(0xFF6A5040, du + 5, dv - 4, du + 6, dv + 4);
                    Path h = new Path();
                    f.mv(h, du - 9, dv - 2.8f);
                    f.qd(h, du + 4, dv - 3.4f, du + 10, dv);
                    f.qd(h, du + 4, dv + 3.4f, du - 9, dv + 2.8f);
                    f.ln(h, du - 9, dv - 2.8f);
                    f.fill(h, 0xFF2E5A7A);
                    f.line(0xFFE8E4DA, 0.7f, du - 9, dv, du + 10, dv);
                }
                f.lamp(4, 4 + yv);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                if (part == 0) {
                    // The shop: a slate gable over tarred weatherboard, a lantern and a ship's-bell sign.
                    f.rect(0xFF2A2A2A, 0, 0, L, S);
                    gable(f, 0xFF5A6068, 0.8f, 0.8f, L - 0.8f, S - 0.8f, 1.6f);
                    chimney(f, L * 0.7f, S * 0.18f, 2, 0xFF8A5A44);
                    f.rect(0xFFE8E4DA, 2, S - 2.4f, L - 2, S - 0.8f);
                    f.circle(0xFFC8A040, L / 2, S - 1.6f, 1.2f);
                    f.sign(city, "CHANDLER", L / 2, S * 0.3f, 4, 0xFFF0E8D0);
                } else {
                    // The sail loft: a long red pantile roof with a run of roof lights and a hoist beam at one end.
                    f.rect(0xFF3A2E26, 0, 0, L, S);
                    gable(f, 0xFFA8503A, 0.6f, 0.6f, L - 0.6f, S - 0.6f, 1.4f);
                    for (float u = 4; u < L - 6; u += 7) {
                        glass(f, u, 1.4f, u + 4, S / 2 - 0.8f, 0xFF9AB4C4, 2);
                    }
                    f.rect(0x48000000, L - 4 + 1, S / 2 - 0.8f + 1, L + 1, S / 2 + 0.8f + 1);
                    f.rect(0xFF5A4232, L - 4, S / 2 - 0.8f, L, S / 2 + 0.8f);
                    f.circle(0xFF2A2A2A, L - 0.8f, S / 2, 0.5f);
                    f.line(0xFF2A2A2A, 0.5f, L / 2, S / 2, L / 2 + 2, S / 2 - 2);
                    f.circle(0xFFC8A040, L / 2, S / 2, 0.7f);
                    f.sign(city, "SAIL LOFT", L / 2, S * 0.75f, 4, 0xFFF0E8D0);
                }
            }
        };
        d.ground = City.DIRT;
        d.districts = Variants.IND | Variants.OLD;
        d.minS = 10;
        d.minL = 13;
        d.base = City.SHOP;
        d.inside = Variants.I_SHELVES;
        d.roofCol = 0xFF5A6068;
        out.add(d);
    }

    // ------------------------------------------------------------------ 18 Harbour Pub

    /** A life ring on a post. */
    static void lifeRing(F f, float u, float v) {
        f.circle(0x38000000, u + 0.7f, v + 1, 2.2f);
        f.ring(0xFFE84A2A, 1.2f, u, v, 1.7f);
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2 + Math.PI / 4;
            f.circle(0xFFF4F0E8, u + (float) Math.cos(a) * 1.7f, v + (float) Math.sin(a) * 1.7f, 0.65f);
        }
    }

    static void harbourPub(List<Landmarks.Def> out) {
        // An old timber-framed pub with a sagging stone-tiled roof, its shingle garden fenced with rope and set with anchors.
        D d = new D("Harbour Pub", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                return new int[][]{B(1, 1, Math.max(5, L * 45 / 100), Math.max(4, Math.min(6, S / 2)), 2)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float pu1 = (q[0][0] + q[0][2]) * T, pv1 = (q[0][1] + q[0][3]) * T;
                // Shingle all over, flagstones round the pub door.
                f.rect(0xFFB4A88E, 0, 0, L, S);
                f.specks(r, 260, 0x26000000, 0x30FFFFFF, 0, 0, L, S, 1.1f);
                f.rect(0xFF8E8676, T - 3, pv1, pu1 + 3, pv1 + 8);
                for (float u = T - 3; u < pu1 + 3; u += 5) f.line(0x30000000, 0.5f, u, pv1, u, pv1 + 8);
                // Rope fence round the garden: stubby posts with the rope sagging between.
                float g0 = pu1 + 6, gv0 = 6;
                for (float u = g0; u < L - 4; u += 10) {
                    f.curve(0xFFC8B080, 0.8f, u, gv0, u + 5, gv0 + 1.6f, u + 10, gv0);
                    f.curve(0xFFC8B080, 0.8f, u, S - 5, u + 5, S - 3.4f, u + 10, S - 5);
                }
                for (float v = gv0; v < S - 5; v += 10) f.curve(0xFFC8B080, 0.8f, g0, v, g0 + 1.6f, v + 5, g0, v + 10);
                for (float u = g0; u < L - 3; u += 10) {
                    f.circle(0xFF4A3A2A, u, gv0, 1.2f);
                    f.circle(0xFF4A3A2A, u, S - 5, 1.2f);
                }
                for (float v = gv0; v < S - 4; v += 10) f.circle(0xFF4A3A2A, g0, v, 1.2f);
                // Picnic tables and barrel tables, two great anchors on display, life rings on posts.
                for (float u = g0 + 12; u < L - 10; u += 18)
                    for (float v = gv0 + 10; v < S - 12; v += 16) picnic(f, u, v, (int) (u + v) % 2 == 0, 0xFF8A6A4A);
                anchor(f, g0 + 4, S - 12, 1.6f, 0xFF3A3A3E);
                if (L - g0 > 40) anchor(f, L - 22, gv0 + 8, 1.4f, 0xFF5A4A3A);
                lifeRing(f, g0, (gv0 + S - 5) / 2);
                lifeRing(f, L - 4, gv0 + 2);
                for (int k = 0; k < 3; k++) {
                    float u = 6 + k * 8, v = Math.min(S - 8, pv1 + 16);
                    f.circle(0x40000000, u + 0.8f, v + 1, 2.4f);
                    f.circle(0xFF6A4A2A, u, v, 2.4f);
                    f.ring(0xFF2A2A2A, 0.5f, u, v, 1.9f);
                    f.circle(0xFF8A6440, u - 0.4f, v - 0.4f, 1);
                }
                // An old boat turned planter, full of flowers.
                float bu = 18, bv = S - 9;
                if (bv > pv1 + 22) {
                    Path h = new Path();
                    f.mv(h, bu - 10, bv - 3);
                    f.qd(h, bu + 6, bv - 4, bu + 11, bv);
                    f.qd(h, bu + 6, bv + 4, bu - 10, bv + 3);
                    f.ln(h, bu - 10, bv - 3);
                    f.fill(h, 0xFF6A8A9A);
                    f.oval(0xFF5A4A3A, bu - 8, bv - 2, bu + 8, bv + 2);
                    int[] fl = {0xFFE04A6A, 0xFFF0D040, 0xFFF4F0E8, 0xFFB04ACA};
                    for (int k = 0; k < 9; k++) f.circle(fl[k % 4], bu - 7 + k * 1.8f, bv - 1 + (k % 3), 0.8f);
                }
                f.lamp(pu1 + 3, 3);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                // Stone-slate roof, the ridge sagging with age, a lean-to at one end, two big chimneys.
                f.rect(0xFF3A3028, 0, 0, L, S);
                float m = S / 2;
                Path a = new Path();
                f.mv(a, 0.8f, 0.8f);
                f.ln(a, L - 0.8f, 0.8f);
                f.ln(a, L * 0.8f, m - 0.2f);
                f.qd(a, L * 0.5f, m + 1.2f, L * 0.08f, m - 0.2f);
                f.ln(a, 0.8f, 0.8f);
                f.fill(a, 0xFF8A8478);
                Path b = new Path();
                f.mv(b, 0.8f, S - 0.8f);
                f.ln(b, L * 0.8f, S - 0.8f);
                f.ln(b, L * 0.8f, m - 0.2f);
                f.qd(b, L * 0.5f, m + 1.2f, L * 0.08f, m - 0.2f);
                f.ln(b, 0.8f, S - 0.8f);
                f.fill(b, 0xFF6A665C);
                for (float v = 2; v < S - 1; v += 1.6f) f.line(0x22000000, 0.4f, 1, v, L * 0.79f, v);
                f.specks(r, 40, 0x20000000, 0x30C8D0A0, 1, 1, L * 0.8f, S - 1, 1);
                f.curve(0xFFA8A090, 1, L * 0.08f, m - 0.2f, L * 0.5f, m + 1.2f, L * 0.8f, m - 0.2f);
                // The lean-to: a low tarred roof.
                f.rect(0xFF2E2E30, L * 0.8f, 0.8f, L - 0.8f, S - 0.8f);
                for (float v = 2; v < S - 1; v += 2) f.line(0x30FFFFFF, 0.3f, L * 0.8f, v, L - 0.8f, v);
                chimney(f, L * 0.12f, m - 2, 2, 0xFF8A6A54);
                chimney(f, L * 0.62f, m - 2, 3, 0xFF8A6A54);
                f.sign(city, "THE ANCHOR", L * 0.4f, S - 3, 4, 0xFFF0E4C0);
            }
        };
        d.ground = City.SAND;
        d.districts = Variants.OLD | Variants.IND | Variants.MID;
        d.minS = 10;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_BAR;
        d.roofCol = 0xFF8A8478;
        out.add(d);
    }

    static void taqueria(List<Landmarks.Def> out) {
    }

    static void bazaar(List<Landmarks.Def> out) {
    }

    static void kebabRow(List<Landmarks.Def> out) {
    }

    static void irishPub(List<Landmarks.Def> out) {
    }

    static void beerGarden(List<Landmarks.Def> out) {
    }

    static void noodleArcade(List<Landmarks.Def> out) {
    }

    static void teaHouseGarden(List<Landmarks.Def> out) {
    }
}
