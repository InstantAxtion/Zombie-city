package com.instantaxtion.zombiesandbox;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.Landmarks.*;

/** ((22.6) Landmark designs, part 5 (see Landmarks): civic, heritage, religion and education. */
final class LandmarkArt5 {
    private LandmarkArt5() {
    }

    static final float TT = City.T;

    static void register(List<Landmarks.Def> out) {
        bathhouse(out);
        masonic(out);
        postOffice(out);
        customs(out);
        fireMuseum(out);
        basilica(out);
        mosque(out);
        stave(out);
        synagogue(out);
        conservatory(out);
        records(out);
        starFort(out);
        castle(out);
        ruins(out);
        arch(out);
        equestrian(out);
        pavilion(out);
        veterans(out);
        weather(out);
        lighthouse(out);
        windmill(out);
        foundry(out);
        cemetery(out);
        spa(out);
        embassy(out);
    }

    // ------------------------------------------------------------------ shared bits

    /** A building laid out along the block's long side (u along it, v across), turned when the block is tall. */
    static int[] at(boolean along, int u, int v, int lu, int lv, int floors) {
        return along ? new int[]{u, v, lu, lv, floors} : new int[]{v, u, lv, lu, floors};
    }

    /** The plan for the block drawn in x0..y1 (plan() must not use its Random). */
    static int[][] pl(Landmarks.Def d, float x0, float y0, float x1, float y1) {
        return d.plan(Math.round((x1 - x0) / TT), Math.round((y1 - y0) / TT), null);
    }

    /** A plan building's rectangle in f's (u, v) frame: {u0, v0, u1, v1}. */
    static float[] bx(F f, int[] q) {
        float a = q[0] * TT, b = q[1] * TT, w = q[2] * TT, h = q[3] * TT;
        return f.t ? new float[]{b, a, b + h, a + w} : new float[]{a, b, a + w, b + h};
    }

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

        /** A line with round ends (a soft stroke for long shadows, hoses, paths). */
        void rline(int col, float w, float u0, float v0, float u1, float v1) {
            p.setStrokeCap(Paint.Cap.ROUND);
            line(col, w, u0, v0, u1, v1);
            p.setStrokeCap(Paint.Cap.BUTT);
        }

        void poly(int col, float... q) {
            float[] m = new float[q.length];
            for (int k = 0; k + 1 < q.length; k += 2) {
                m[k] = x(q[k], q[k + 1]);
                m[k + 1] = y(q[k], q[k + 1]);
            }
            Landmarks.poly(c, p, col, m);
        }

        /** A regular polygon of n sides round (u, v), turned by a0. */
        void ngon(int col, float u, float v, float r, int n, double a0) {
            float[] q = new float[n * 2];
            for (int k = 0; k < n; k++) {
                double a = a0 + k * 2 * Math.PI / n;
                q[2 * k] = u + (float) Math.cos(a) * r;
                q[2 * k + 1] = v + (float) Math.sin(a) * r;
            }
            poly(col, q);
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

        /** The soft shadow of a box d high, cast to the lower right. */
        void sh(float[] b, float d) {
            rect(0x38000000, b[0] + d * 0.7f, b[1] + d * 0.7f, b[2] + d, b[3] + d);
        }

        /** The long shadow of a tall slender thing at (u, v). */
        void tall(float u, float v, float r, float len) {
            rline(0x33000000, r * 2, u, v, u + len, v + len);
        }

        void mv(Path q, float u, float v) {
            q.moveTo(x(u, v), y(u, v));
        }

        void qd(Path q, float cu, float cv, float u, float v) {
            q.quadTo(x(cu, cv), y(cu, cv), x(u, v), y(u, v));
        }

        void lt(Path q, float u, float v) {
            q.lineTo(x(u, v), y(u, v));
        }

        void fill(Path q, int col) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(col);
            c.drawPath(q, p);
        }

        void sign(City city, String s, float u, float v, float size, int col) {
            Landmarks.sign(city, s, x(u, v), y(u, v), size, col);
        }
    }

    /** A hipped roof over the rectangle, its faces shaded for light from the top left; courses adds tile lines. */
    static void hip(F f, int col, float u0, float v0, float u1, float v1, boolean courses) {
        float h = Math.min(u1 - u0, v1 - v0) / 2, au = u0 + h, av = v0 + h, bu = u1 - h, bv = v1 - h;
        f.poly(City.lighten(col, 0.12f), u0, v0, u1, v0, bu, av, au, av);
        f.poly(col, u0, v0, u0, v1, au, bv, au, av);
        f.poly(City.darken(col, 0.82f), u1, v0, u1, v1, bu, bv, bu, av);
        f.poly(City.darken(col, 0.7f), u0, v1, u1, v1, bu, bv, au, bv);
        if (courses)
            for (float k = 3; k < h - 1; k += 3) f.frame(0x22000000, 0.5f, u0 + k, v0 + k, u1 - k, v1 - k);
        int hl = City.lighten(col, 0.3f);
        f.line(hl, 0.7f, u0, v0, au, av);
        f.line(hl, 0.7f, u1, v0, bu, av);
        f.line(City.darken(col, 0.6f), 0.7f, u0, v1, au, bv);
        f.line(City.darken(col, 0.6f), 0.7f, u1, v1, bu, bv);
        f.line(hl, 1f, au, av, bu, bv);
    }

    /** A gabled roof with its ridge along u (lit slope on the low-v side). */
    static void gable(F f, int col, float u0, float v0, float u1, float v1) {
        float m = (v0 + v1) / 2;
        f.rect(City.lighten(col, 0.1f), u0, v0, u1, m);
        f.rect(City.darken(col, 0.74f), u0, m, u1, v1);
        f.line(City.lighten(col, 0.35f), 1f, u0, m, u1, m);
        f.frame(City.darken(col, 0.6f), 0.6f, u0, v0, u1, v1);
    }

    /** A gabled roof with its ridge along v (lit slope on the low-u side). */
    static void gableV(F f, int col, float u0, float v0, float u1, float v1) {
        float m = (u0 + u1) / 2;
        f.rect(City.lighten(col, 0.06f), u0, v0, m, v1);
        f.rect(City.darken(col, 0.8f), m, v0, u1, v1);
        f.line(City.lighten(col, 0.35f), 1f, m, v0, m, v1);
        f.frame(City.darken(col, 0.6f), 0.6f, u0, v0, u1, v1);
    }

    /** A dome from above: gores shaded round from the lit top left. */
    static void dome(F f, float u, float v, float r, int col, int n) {
        for (int k = 0; k < n; k++) {
            double a0 = k * 2 * Math.PI / n, a1 = (k + 1) * 2 * Math.PI / n, am = (a0 + a1) / 2;
            float sh = 1f + 0.3f * (float) Math.cos(am + 3 * Math.PI / 4);
            f.poly(Landmarks.shade(col, sh), u, v, u + (float) Math.cos(a0) * r, v + (float) Math.sin(a0) * r,
                    u + (float) Math.cos(a1) * r, v + (float) Math.sin(a1) * r);
        }
    }

    /** A smooth dome: a shadow, the body, a darker rim and a highlight. */
    static void ball(F f, float u, float v, float r, int col) {
        f.circle(0x40000000, u + r * 0.3f, v + r * 0.35f, r);
        f.circle(City.darken(col, 0.75f), u, v, r);
        f.circle(col, u - r * 0.1f, v - r * 0.1f, r * 0.82f);
        f.circle(City.lighten(col, 0.22f), u - r * 0.3f, v - r * 0.3f, r * 0.4f);
        f.circle(City.lighten(col, 0.5f), u - r * 0.4f, v - r * 0.4f, r * 0.14f);
    }

    /** Merlons round the edge of a rectangle (a crenellated parapet). */
    static void crenels(F f, int col, float u0, float v0, float u1, float v1, float s) {
        for (float u = u0; u + s <= u1 + 0.01f; u += 2 * s) {
            f.rect(col, u, v0, u + s, v0 + s);
            f.rect(col, u, v1 - s, u + s, v1);
        }
        for (float v = v0 + 2 * s; v + s <= v1 - s + 0.01f; v += 2 * s) {
            f.rect(col, u0, v, u0 + s, v + s);
            f.rect(col, u1 - s, v, u1, v + s);
        }
    }

    /** A flight of steps across v from v0 down to v1. */
    static void steps(F f, float u0, float v0, float u1, float v1, int n, int col) {
        float h = (v1 - v0) / n;
        for (int k = 0; k < n; k++) {
            float v = v0 + k * h;
            f.rect(k % 2 == 0 ? col : City.darken(col, 0.92f), u0, v, u1, v + h);
            f.line(0x40000000, 0.5f, u0, v + h, u1, v + h);
        }
    }

    /** A dark pointed conifer (yew, spruce) from above. */
    static void conifer(F f, float u, float v, float r, int col) {
        f.circle(0x40000000, u + r * 0.5f, v + r * 0.6f, r * 0.95f);
        int n = 7;
        float[] q = new float[n * 4];
        for (int k = 0; k < 2 * n; k++) {
            double a = k * Math.PI / n;
            float rr = k % 2 == 0 ? r : r * 0.62f;
            q[2 * k] = u + (float) Math.cos(a) * rr;
            q[2 * k + 1] = v + (float) Math.sin(a) * rr;
        }
        f.poly(City.darken(col, 0.78f), q);
        f.circle(col, u - r * 0.1f, v - r * 0.1f, r * 0.6f);
        f.circle(City.lighten(col, 0.15f), u - r * 0.25f, v - r * 0.25f, r * 0.28f);
    }

    /** A flagpole with its flag flying toward +u (three stripes; c3 0 for a plain one). */
    static void flag(F f, float u, float v, int c1, int c2, int c3) {
        f.circle(0x40000000, u + 1.2f, v + 1.2f, 1.1f);
        f.rect(0x30000000, u + 2, v + 2, u + 14, v + 8.5f);
        float w = 4;
        f.rect(c1, u, v - 0.5f, u + w, v + 6);
        f.rect(c2, u + w, v - 0.5f, u + 2 * w, v + 6);
        f.rect(c3 == 0 ? c2 : c3, u + 2 * w, v - 0.5f, u + 12, v + 6);
        f.line(0x30000000, 0.5f, u, v + 2.8f, u + 12, v + 2.8f);
        f.circle(0xFF6A6E72, u, v, 0.9f);
        f.circle(0xFFE0D8B0, u, v, 0.45f);
    }

    /** A car from above, along u or along v. */
    static void car(F f, float u, float v, boolean alongU, int col) {
        float a = alongU ? 5.5f : 2.7f, b = alongU ? 2.7f : 5.5f;
        f.roundRect(0x38000000, u - a + 1, v - b + 1, u + a + 1, v + b + 1, 1.5f);
        f.roundRect(col, u - a, v - b, u + a, v + b, 1.5f);
        if (alongU) {
            f.rect(0xFF2A3038, u + 1.4f, v - b + 0.6f, u + 3, v + b - 0.6f);
            f.rect(0xFF2A3038, u - 4, v - b + 0.7f, u - 3, v + b - 0.7f);
            f.rect(City.lighten(col, 0.15f), u - 3, v - b + 0.8f, u + 1.4f, v + b - 0.8f);
        } else {
            f.rect(0xFF2A3038, u - a + 0.6f, v + 1.4f, u + a - 0.6f, v + 3);
            f.rect(0xFF2A3038, u - a + 0.7f, v - 4, u + a - 0.7f, v - 3);
            f.rect(City.lighten(col, 0.15f), u - a + 0.8f, v - 3, u + a - 0.8f, v + 1.4f);
        }
    }

    static float clamp(float x, float lo, float hi) {
        return x < lo ? lo : x > hi ? hi : x;
    }

    // ================================================================== 1 Victorian Bathhouse

    static void bathhouse(List<Landmarks.Def> out) {
        // Red brick baths: slate pavilions at the ends, a glazed barrel vault over the pool hall, a boiler house with
        // its chimney and coal heap, and a tiled forecourt behind iron railings.
        Landmarks.Def d = new Landmarks.Def("Victorian Bathhouse", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, 1, 1, L - 6, S - 5, 2), at(al, L - 4, 1, 3, 3, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] q = pl(this, x0, y0, x1, y1);
                float[] h = bx(f, q[0]), b = bx(f, q[1]);
                f.rect(0xFFC9C1B0, 0, 0, L, S);
                for (float u = 7; u < L; u += 9) f.line(0x16000000, 0.5f, u, 0, u, S);
                for (float v = 7; v < S; v += 9) f.line(0x12000000, 0.5f, 0, v, L, v);
                // Lawns either side of the forecourt, with flower beds.
                float mu = (h[0] + h[2]) / 2, fv0 = h[3], fv1 = S - 5;
                f.rect(0xFF6A9A50, h[0], fv0 + 4, mu - 9, fv1 - 3);
                f.rect(0xFF6A9A50, mu + 9, fv0 + 4, h[2], fv1 - 3);
                f.frame(0xFFB0A890, 1f, h[0], fv0 + 4, mu - 9, fv1 - 3);
                f.frame(0xFFB0A890, 1f, mu + 9, fv0 + 4, h[2], fv1 - 3);
                for (float u = h[0] + 4; u < mu - 11; u += 6) {
                    f.circle(0xFFC84850, u, fv0 + 6.5f, 1.5f);
                    f.circle(0xFFE8C850, u + 3, fv0 + 6.5f, 1.3f);
                }
                for (float u = mu + 12; u < h[2] - 2; u += 6) {
                    f.circle(0xFFC84850, u, fv0 + 6.5f, 1.5f);
                    f.circle(0xFFE8C850, u + 3, fv0 + 6.5f, 1.3f);
                }
                // The tiled path, buff and red squares, to the door.
                for (float v = fv0; v < S - 1; v += 3.5f)
                    for (int k = 0; k < 4; k++) {
                        float u = mu - 7 + k * 3.5f;
                        f.rect(((int) ((v - fv0) / 3.5f) + k) % 2 == 0 ? 0xFFB05A44 : 0xFFE0D2B0, u, v, u + 3.5f, Math.min(v + 3.5f, S - 1));
                    }
                f.frame(0xFF7E7060, 0.8f, mu - 7, fv0, mu + 7, S - 1);
                // A drinking fountain on each lawn.
                float lv = (fv0 + 4 + fv1 - 3) / 2;
                if (fv1 - 3 - fv0 - 4 > 8) {
                    for (int s = -1; s <= 1; s += 2) {
                        float fu = mu + s * ((h[2] - h[0]) / 4 + 2);
                        f.circle(0x40000000, fu + 1.5f, lv + 1.5f, 4);
                        f.ngon(0xFF9A9284, fu, lv, 4, 8, Math.PI / 8);
                        f.circle(0xFF5A9AC0, fu, lv, 2.6f);
                        f.circle(0xFFB8B0A0, fu, lv, 1);
                    }
                }
                // Iron railings along the street with a gate.
                float rv = S - 3;
                f.line(0xFF2E3236, 0.9f, 0, rv, mu - 8, rv);
                f.line(0xFF2E3236, 0.9f, mu + 8, rv, b[2] + 2, rv);
                for (float u = 1; u < b[2] + 2; u += 3) if (Math.abs(u - mu) > 8) f.circle(0xFF2E3236, u, rv, 0.6f);
                f.rect(0xFF8A8070, mu - 9.5f, rv - 1.5f, mu - 7, rv + 1.5f);
                f.rect(0xFF8A8070, mu + 7, rv - 1.5f, mu + 9.5f, rv + 1.5f);
                f.lamp(mu - 8.2f, rv - 3.5f);
                f.lamp(mu + 8.2f, rv - 3.5f);
                // The coal yard by the boiler house: a heap, a cart, sacks.
                float cu = (b[0] + b[2]) / 2, cv = b[3] + 8;
                if (cv + 8 < rv) {
                    f.oval(0x40000000, cu - 9, cv - 3, cu + 6, cv + 7);
                    f.oval(0xFF26262A, cu - 11, cv - 5, cu + 4, cv + 5);
                    f.oval(0xFF3A3A40, cu - 9, cv - 4, cu, cv + 1);
                    for (int k = 0; k < 6; k++) f.circle(0xFF50505A, cu - 8 + k * 2, cv - 2 + (k % 3), 0.6f);
                    f.rect(0xFF6E5034, cu + 6, cv - 3, cu + 13, cv + 3);
                    f.rect(0xFF2A2A2E, cu + 7, cv - 2, cu + 12, cv + 2);
                    f.circle(0xFF3A2A1A, cu + 7, cv + 3.5f, 1.2f);
                    f.circle(0xFF3A2A1A, cu + 12, cv + 3.5f, 1.2f);
                }
                // Plane trees and benches.
                f.tree(L - 6, S - 8, 6, 0xFF4A7E3C);
                f.bench(h[0] + 8, rv - 3, true);
                f.bench(h[2] - 8, rv - 3, true);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                if (part == 0) {
                    f.rect(0xFF8E4E3A, 0, 0, L, S);
                    f.frame(0xFFC9906A, 0.8f, 0.6f, 0.6f, L - 0.6f, S - 0.6f);
                    float pw = Math.min(S * 0.7f, L * 0.24f);
                    hip(f, 0xFF56606C, 1.2f, 1.2f, pw, S - 1.2f, true);
                    hip(f, 0xFF56606C, L - pw, 1.2f, L - 1.2f, S - 1.2f, true);
                    // Brick chimney stacks with pots.
                    for (int s = 0; s < 2; s++) {
                        float cu = s == 0 ? pw * 0.5f : L - pw * 0.5f;
                        f.rect(0x40000000, cu - 2, 3.5f, cu + 4.5f, 7);
                        f.rect(0xFF7A3E2C, cu - 3, 2.5f, cu + 3, 5.5f);
                        f.circle(0xFFB06A48, cu - 1.5f, 4, 0.9f);
                        f.circle(0xFFB06A48, cu + 1.5f, 4, 0.9f);
                        f.circle(0xFF2A1A14, cu - 1.5f, 4, 0.4f);
                        f.circle(0xFF2A1A14, cu + 1.5f, 4, 0.4f);
                    }
                    // The glazed barrel vault over the pool hall.
                    float a = pw + 1.5f, e = L - pw - 1.5f, m = S / 2;
                    f.rect(0xFF3A4650, a - 0.8f, 1.2f, e + 0.8f, S - 1.2f);
                    f.rect(0xFFB2CCD4, a, 2, e, m * 0.6f);
                    f.rect(0xFF9CBAC6, a, m * 0.6f, e, m);
                    f.rect(0xFF7C9CAA, a, m, e, S - m * 0.6f);
                    f.rect(0xFF64828E, a, S - m * 0.6f, e, S - 2);
                    for (float u = a + 3; u < e - 0.5f; u += 3.5f) f.line(0xFF34424A, 0.6f, u, 2, u, S - 2);
                    f.line(0xFF34424A, 0.5f, a, m * 0.6f, e, m * 0.6f);
                    f.line(0xFF34424A, 0.5f, a, S - m * 0.6f, e, S - m * 0.6f);
                    // The ventilating lantern along the ridge.
                    f.rect(0x40000000, a + 3, m - 1.2f, e - 2, m + 3);
                    f.rect(0xFF3E4A52, a + 2, m - 2, e - 3, m + 2);
                    f.rect(0xFFD0E4EA, a + 2.5f, m - 1.5f, e - 3.5f, m - 0.2f);
                    for (float u = a + 4; u < e - 4; u += 2) f.line(0xFF2A343A, 0.4f, u, m + 0.2f, u, m + 1.7f);
                    f.circle(0xFFC8A040, a + 2, m, 1);
                    f.circle(0xFFC8A040, e - 3, m, 1);
                    f.sign(city, "PUBLIC BATHS", L / 2, S - 3, 5, 0xFFF0E2C0);
                } else {
                    f.rect(0xFF844634, 0, 0, L, S);
                    gable(f, 0xFF5A6068, 1, 1, L - 1, S - 1);
                    // The tall round boiler chimney.
                    float cu = L * 0.7f, cv = S * 0.35f;
                    f.tall(cu, cv, 3, 26);
                    f.circle(0xFF6E3626, cu, cv, 3.6f);
                    f.circle(0xFF9A5A44, cu - 0.4f, cv - 0.4f, 3);
                    f.ring(0xFF5A2A1E, 0.6f, cu, cv, 2.4f);
                    f.circle(0xFF1A1414, cu, cv, 1.8f);
                    f.circle(0x30C0C0C0, cu + 3, cv + 2, 3);
                    f.circle(0x24C0C0C0, cu + 6, cv + 5, 4);
                }
            }
        };
        d.minS = 10;
        d.minL = 12;
        d.ground = City.PLAZA;
        d.base = City.OFFICE;
        d.inside = Variants.I_BATH;
        d.roofCol = 0xFF8E4E3A;
        d.districts = Variants.OLD | Variants.MID | Variants.DOWN;
        out.add(d);
    }

    // ================================================================== 2 Masonic Lodge

    static void masonic(List<Landmarks.Def> out) {
        // A stern stone temple with a stepped pyramid lantern on its roof, a black and white chequered forecourt
        // flanked by twin bronze-topped pillars, and clipped hedges.
        Landmarks.Def d = new Landmarks.Def("Masonic Lodge", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int lv = S - 5, lu = Math.min(L - 4, Math.max(lv + 2, S + 1));
                return new int[][]{at(al, (L - lu) / 2, 1, lu, lv, 3)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] h = bx(f, pl(this, x0, y0, x1, y1)[0]);
                f.rect(0xFF5A8A48, 0, 0, L, S);
                for (float v = 4; v < S; v += 6) f.line(0x0C000000, 2.5f, 0, v, L, v);
                float mu = (h[0] + h[2]) / 2, cw = Math.min(26, (h[2] - h[0]) * 0.32f);
                // A stone border round the building.
                f.rect(0xFFB8B0A0, h[0] - 3, h[1] - 3, h[2] + 3, h[3] + 3);
                // The chequered forecourt.
                float v0 = h[3] + 2, v1 = S - 1, sq = 4;
                f.rect(0xFFA8A090, mu - cw - 2, v0 - 2, mu + cw + 2, v1);
                int nu = Math.max(2, Math.round(2 * cw / sq));
                sq = 2 * cw / nu;
                for (int i = 0; i < nu; i++)
                    for (int j = 0; v0 + j * sq < v1 - 0.5f; j++) {
                        float u = mu - cw + i * sq, v = v0 + j * sq;
                        f.rect((i + j) % 2 == 0 ? 0xFF1E1E22 : 0xFFEDEAE2, u, v, u + sq, Math.min(v + sq, v1));
                    }
                // The square and compasses inlaid in brass at its middle.
                float sv = (v0 + v1) / 2;
                if (v1 - v0 > 16) {
                    f.circle(0xFF2A2A30, mu, sv, 6.5f);
                    f.ring(0xFFC8A040, 0.8f, mu, sv, 6);
                    f.line(0xFFE0C060, 0.9f, mu, sv - 4.5f, mu - 3.5f, sv + 4);
                    f.line(0xFFE0C060, 0.9f, mu, sv - 4.5f, mu + 3.5f, sv + 4);
                    f.line(0xFFE0C060, 0.9f, mu - 4, sv - 1, mu, sv + 3);
                    f.line(0xFFE0C060, 0.9f, mu + 4, sv - 1, mu, sv + 3);
                    f.circle(0xFFE0C060, mu, sv - 4.5f, 0.8f);
                }
                // The twin pillars by the door, each with a bronze globe.
                for (int s = -1; s <= 1; s += 2) {
                    float pu = mu + s * (cw + 5), pv = h[3] + 5;
                    f.tall(pu, pv, 2.5f, 14);
                    f.circle(0xFF8E8676, pu, pv, 3.2f);
                    f.circle(0xFFC8BEA8, pu, pv, 2.6f);
                    ball(f, pu, pv, 1.8f, 0xFFA07838);
                }
                // Clipped hedges and topiary balls along both sides.
                for (int s = -1; s <= 1; s += 2) {
                    float hu = mu + s * (cw + 10);
                    float a = Math.min(hu, hu + s * 4), e = Math.max(hu, hu + s * 4);
                    f.rect(0x30000000, a + 1, v0 + 6, e + 1, v1 + 1);
                    f.rect(0xFF2E5A2C, a, v0 + 5, e, v1);
                    f.rect(0xFF3E6E38, a + 0.6f, v0 + 5.5f, e - 1, v1 - 0.5f);
                }
                for (int s = -1; s <= 1; s += 2) {
                    f.bush(h[0] - 6, h[1] + 4 + (s + 1) * (h[3] - h[1] - 8) / 2, 3.2f);
                    f.bush(h[2] + 6, h[1] + 4 + (s + 1) * (h[3] - h[1] - 8) / 2, 3.2f);
                }
                if (h[0] > 18) {
                    f.tree(h[0] / 2, S * 0.3f, 6, 0xFF3A6A36);
                    f.tree(L - h[0] / 2, S * 0.3f, 6, 0xFF3A6A36);
                    conifer(f, h[0] / 2, S - 8, 4, 0xFF2A4E2A);
                    conifer(f, L - h[0] / 2, S - 8, 4, 0xFF2A4E2A);
                }
                f.lamp(mu - cw - 4, S - 3);
                f.lamp(mu + cw + 4, S - 3);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                // Work in the block's frame: the front is high v of the block, i.e. high y (or high x if tall).
                f.rect(0xFFA49A86, 0, 0, L, S);
                f.rect(0xFF6E727A, 2.5f, 2.5f, L - 2.5f, S - 2.5f);
                for (float u = 1; u < L - 1; u += 2.2f) {
                    f.rect(0xFFCEC4AE, u, 0.4f, u + 1.1f, 1.6f);
                    f.rect(0xFF8A8270, u, S - 1.6f, u + 1.1f, S - 0.4f);
                }
                for (float u = 5; u < L - 5; u += 4) f.line(0x22000000, 0.5f, u, 2.5f, u, S - 2.5f);
                // The stepped pyramid lantern.
                float cu = L / 2, cv = S / 2, m = Math.min(L, S) * 0.36f;
                f.rect(0x40000000, cu - m + 3, cv - m + 3, cu + m + 3, cv + m + 3);
                int n = 4;
                for (int k = 0; k < n; k++) {
                    float s = m * (1 - k / (float) n);
                    int col = Landmarks.shade(0xFFC0B69E, 0.86f + k * 0.07f);
                    f.rect(City.darken(col, 0.8f), cu - s, cv - s, cu + s, cv + s);
                    f.rect(col, cu - s, cv - s, cu + s - 1.2f, cv + s - 1.2f);
                    f.line(City.lighten(col, 0.3f), 0.6f, cu - s, cv - s, cu + s, cv - s);
                }
                float s = m / n;
                f.rect(0xFF8EC0D0, cu - s * 0.8f, cv - s * 0.8f, cu + s * 0.8f, cv + s * 0.8f);
                f.poly(0xFFC8A040, cu, cv - s * 0.6f, cu + s * 0.55f, cv + s * 0.4f, cu - s * 0.55f, cv + s * 0.4f);
                f.circle(0xFFF0E8D0, cu, cv + s * 0.05f, s * 0.15f);
                // The portico at the front (towards the forecourt).
                float w = Math.min(L * 0.3f, 22);
                f.rect(0x40000000, cu - w + 2, S - 5, cu + w + 2, S);
                f.rect(0xFFB8AE98, cu - w, S - 5.5f, cu + w, S);
                f.rect(0xFFCFC6B0, cu - w, S - 5.5f, cu + w, S - 3);
                for (float u = cu - w + 2; u < cu + w - 1; u += 4) f.circle(0xFFE8E0CC, u, S - 1, 1);
                f.sign(city, "LODGE", cu, cv + m + 3, 4, 0xFF2A2A2A);
            }
        };
        d.minS = 10;
        d.minL = 10;
        d.ground = City.GRASS;
        d.base = City.OFFICE;
        d.inside = Variants.I_SEATS;
        d.roofCol = 0xFF6E727A;
        d.districts = Variants.OLD | Variants.MID | Variants.SUB;
        out.add(d);
    }

    // ================================================================== 3 Grand Post Office

    static void postOffice(List<Landmarks.Def> out) {
        // A Beaux-Arts post office: a limestone and slate mansard round a sawtooth sorting hall, a small copper dome
        // over the public hall, pillar boxes on the steps, and a yard of red mail vans by the depot.
        Landmarks.Def d = new Landmarks.Def("Grand Post Office", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, 1, 1, L - 6, S - 5, 3), at(al, L - 4, 1, 3, S - 7, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] q = pl(this, x0, y0, x1, y1);
                float[] h = bx(f, q[0]), g = bx(f, q[1]);
                f.rect(0xFFCCC6B8, 0, 0, L, S);
                // The van yard: asphalt with painted bays, in front of the depot.
                float yu0 = g[0] - 6, yu1 = L;
                f.rect(0xFF55575A, yu0, 0, yu1, S);
                for (float u = yu0 + 2; u < yu1; u += 13) f.line(0xFFE8E4D0, 0.6f, u, g[3] + 3, u, S - 3);
                f.line(0xFFE8C040, 0.8f, yu0 + 1, g[3] + 2.5f, yu1 - 1, g[3] + 2.5f);
                for (float u = yu0 + 8.5f; u + 4 < yu1; u += 13) {
                    float vv = g[3] + 12;
                    if (vv + 8 > S - 2) break;
                    f.roundRect(0x40000000, u - 3.5f + 1, vv - 7 + 1, u + 3.5f + 1, vv + 7 + 1, 1.2f);
                    f.roundRect(0xFFC0262A, u - 3.5f, vv - 7, u + 3.5f, vv + 7, 1.2f);
                    f.rect(0xFFE04040, u - 2.8f, vv - 4, u + 2.8f, vv + 6.3f);
                    f.rect(0xFF2A3038, u - 2.8f, vv - 6.3f, u + 2.8f, vv - 4.8f);
                    f.line(0xFFE8C040, 0.6f, u - 2.8f, vv + 1, u + 2.8f, vv + 1);
                }
                // The front plaza with granite steps up to the doors.
                float mu = (h[0] + h[2]) / 2;
                f.rect(0xFFD8D2C2, 0, h[3], yu0 - 1, S);
                for (float u = 4; u < yu0 - 2; u += 8) f.line(0x18000000, 0.5f, u, h[3], u, S);
                float sw = Math.min(20, (h[2] - h[0]) * 0.3f);
                steps(f, mu - sw, h[3], mu + sw, h[3] + 6, 4, 0xFFBFB6A4);
                // Pillar boxes, bollards and lamps.
                for (int s = -1; s <= 1; s += 2) {
                    float bu = mu + s * (sw + 5), bv = h[3] + 4;
                    f.circle(0x40000000, bu + 1, bv + 1, 2.4f);
                    f.circle(0xFFB01E22, bu, bv, 2.4f);
                    f.circle(0xFF1E1E22, bu, bv, 1.3f);
                    f.lamp(mu + s * (sw + 2), S - 4);
                }
                for (float u = 3; u < yu0 - 2; u += 6) f.circle(0xFF4A4C50, u, S - 1.5f, 0.9f);
                // Plane trees along the front and a flagpole.
                for (float u = 8; u < yu0 - 4; u += Math.max(16, (yu0 - 12) / 3)) {
                    if (Math.abs(u - mu) < sw + 6) continue;
                    f.tree(u, S - 7, 5, 0xFF4A7E3C);
                }
                flag(f, mu, S - 6, 0xFF2A3A8A, 0xFFF0F0F0, 0xFFC02020);
                // A side lane and bike rack between the building and the yard.
                for (float v = 4; v < h[3] - 2; v += 4) f.line(0xFF6A6C70, 0.6f, h[2] + 2, v, h[2] + 5, v);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                if (part == 0) {
                    // Limestone cornice, slate mansard, then the sorting hall's sawtooth glazing.
                    f.rect(0xFFD8CCB0, 0, 0, L, S);
                    f.rect(0xFF4E5560, 2.5f, 2.5f, L - 2.5f, S - 2.5f);
                    f.rect(0xFF656D78, 2.5f, 2.5f, L - 2.5f, 5);
                    f.rect(0xFF5A626C, 2.5f, 2.5f, 5, S - 2.5f);
                    f.rect(0xFF3E444C, L - 5, 2.5f, L - 2.5f, S - 2.5f);
                    f.rect(0xFF3A4048, 2.5f, S - 5, L - 2.5f, S - 2.5f);
                    for (float u = 6; u < L - 6; u += 6) {
                        f.rect(0xFFD8CCB0, u, S - 4.8f, u + 2.5f, S - 2.7f);
                        f.rect(0xFFD8CCB0, u, 2.7f, u + 2.5f, 4.8f);
                    }
                    f.rect(0xFF8E8A80, 6, 6, L - 6, S - 6);
                    for (float u = 7; u < L - 7; u += 6) f.line(0x18000000, 0.5f, u, 6, u, S - 6);
                    // Two long sorting-hall skylights, each a ridge of glass.
                    for (int s2 = 0; s2 < 2; s2++) {
                        float a = s2 == 0 ? 9 : S / 2 + 4, e = s2 == 0 ? S / 2 - 4 : S - 9;
                        if (e - a < 4) continue;
                        f.rect(0x40000000, 10, a + 1.5f, L - 8, e + 1.5f);
                        f.rect(0xFF3A444C, 9, a, L - 9, e);
                        f.rect(0xFFB0C8D2, 9.6f, a + 0.6f, L - 9.6f, (a + e) / 2);
                        f.rect(0xFF7E98A4, 9.6f, (a + e) / 2, L - 9.6f, e - 0.6f);
                        for (float u = 12; u < L - 10; u += 3) f.line(0xFF3A444C, 0.5f, u, a, u, e);
                    }
                    // The copper dome over the public hall, at the front.
                    float cu = L / 2, cv = S / 2, rr = Math.min(S * 0.24f, 10);
                    f.circle(0xFFD8CCB0, cu, cv, rr + 2);
                    f.circle(0x40000000, cu + 2, cv + 2, rr);
                    dome(f, cu, cv, rr, 0xFF5E9A86, 16);
                    f.circle(0xFFE8E0C8, cu, cv, rr * 0.3f);
                    f.circle(0xFFC8A040, cu, cv, rr * 0.13f);
                    f.sign(city, "POST OFFICE", cu, S - 3.5f, 5, 0xFF7A1A1A);
                } else {
                    flatRoof(c, p, x0, y0, x1, y1, 0xFF9A968C);
                    for (float v = 4; v < S - 3; v += 7) {
                        f.rect(0x40000000, 4, v + 1, L - 3, v + 4);
                        f.rect(0xFFB8B6B0, 3, v, L - 4, v + 3);
                        f.circle(0xFF6A6C70, L / 2, v + 1.5f, 1);
                    }
                }
            }
        };
        d.minS = 10;
        d.minL = 14;
        d.ground = City.PLAZA;
        d.base = City.OFFICE;
        d.inside = Variants.I_OFFICE;
        d.roofCol = 0xFF4E5560;
        d.districts = Variants.DOWN | Variants.MID | Variants.OLD;
        out.add(d);
    }

    // ================================================================== 4 Customs House

    static void customs(List<Landmarks.Def> out) {
        // A stone customs house on a quay: a hipped slate roof with dormers and a central stepped tower with an
        // octagonal copper cupola; behind it a dock basin with a moored tug, bollards, crates and a crane.
        Landmarks.Def d = new Landmarks.Def("Customs House", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, 2, 4, L - 4, S - 7, 3)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] h = bx(f, pl(this, x0, y0, x1, y1)[0]);
                // Cobbled quay.
                f.rect(0xFF9E968A, 0, 0, L, S);
                for (float v = 2; v < S; v += 3)
                    for (float u = (v % 6 < 3 ? 0 : 1.5f); u < L; u += 3) f.rect(0x14000000, u, v, u + 2.4f, v + 0.6f);
                // The dock basin along the back.
                float wv = 18;
                f.rect(0xFF2F5E78, 0, 0, L, wv);
                f.rect(0xFF3A6E8A, 0, 0, L, wv - 4);
                for (float u = 3; u < L; u += 11) f.line(0x30FFFFFF, 0.5f, u, 5 + (u % 7), u + 5, 5 + (u % 7));
                f.rect(0xFF6E665A, 0, wv - 1, L, wv + 1.5f);
                f.rect(0xFF5A5248, 0, wv + 1.5f, L, wv + 2);
                for (float u = 6; u < L; u += 14) {
                    f.circle(0x40000000, u + 0.8f, wv + 4.3f, 1.4f);
                    f.circle(0xFF2A2C30, u, wv + 3.5f, 1.4f);
                }
                // A tug moored at the quay.
                float tu = L * 0.28f, tv = wv - 7;
                f.oval(0x30000000, tu - 13, tv - 3.5f, tu + 15, tv + 6.5f);
                f.poly(0xFF2A2A30, tu - 12, tv - 4, tu + 8, tv - 4, tu + 14, tv, tu + 8, tv + 4, tu - 12, tv + 4);
                f.poly(0xFF9A2E26, tu - 11, tv - 3, tu + 7.5f, tv - 3, tu + 12.5f, tv, tu + 7.5f, tv + 3, tu - 11, tv + 3);
                f.rect(0xFFE8E2D4, tu - 6, tv - 2.2f, tu + 3, tv + 2.2f);
                f.rect(0xFF2A3038, tu + 1.5f, tv - 2, tu + 3, tv + 2);
                f.circle(0xFF1E1E22, tu - 3, tv, 1.6f);
                f.circle(0xFFE0B030, tu - 3, tv, 1.1f);
                for (int k = 0; k < 4; k++) f.circle(0xFF3A3A3A, tu - 11 + k * 6, tv + 4, 0.9f);
                f.line(0xFFD8D0B8, 0.4f, tu + 10, tv - 1, tu + 14, wv + 3.5f);
                // A dockside crane and crates.
                float cu = L * 0.72f;
                f.rect(0xFF3A3C40, cu - 3, wv + 3, cu + 3, wv + 9);
                f.circle(0xFFC09A30, cu, wv + 6, 2.5f);
                f.line(0xFFC09A30, 1.6f, cu, wv + 6, cu - 8, wv - 6);
                f.line(0x30000000, 1.6f, cu + 3, wv + 9, cu + 9, wv + 1);
                for (int k = 0; k < 4; k++) {
                    float u = cu + 6 + (k % 2) * 6, v = wv + 4 + (k / 2) * 6;
                    if (u + 5 > L - 1) continue;
                    f.rect(0x40000000, u + 1, v + 1, u + 6, v + 6);
                    f.rect(k % 3 == 0 ? 0xFF9A7A4E : 0xFF8A6A40, u, v, u + 5, v + 5);
                    f.line(0x50000000, 0.5f, u, v, u + 5, v + 5);
                    f.line(0x50000000, 0.5f, u + 5, v, u, v + 5);
                }
                // Barrels by the west end.
                for (int k = 0; k < 5; k++) {
                    float u = 5 + (k % 3) * 3.4f, v = wv + 5 + (k / 3) * 3.4f;
                    f.circle(0xFF5E4026, u, v, 1.6f);
                    f.ring(0xFF2E2016, 0.4f, u, v, 1.1f);
                }
                // The front: a granite terrace, steps and flagpoles.
                f.rect(0xFFC8C0AE, h[0] - 2, h[3], h[2] + 2, S - 2);
                float mu = (h[0] + h[2]) / 2, sw = Math.min(16, (h[2] - h[0]) * 0.22f);
                steps(f, mu - sw, h[3], mu + sw, h[3] + 5, 4, 0xFFB4AC9A);
                for (int k = -1; k <= 1; k++)
                    flag(f, mu + k * (sw + 8) - 4, S - 6, k == 0 ? 0xFF203A80 : 0xFF20683A, 0xFFF4F0E4, k == 0 ? 0xFFB02020 : 0xFFD0A020);
                for (float u = 3; u < L - 2; u += 5) f.circle(0xFF4A4C50, u, S - 1.5f, 0.8f);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFFD4CAB4, 0, 0, L, S);
                hip(f, 0xFF4C5866, 1.5f, 1.5f, L - 1.5f, S - 1.5f, true);
                // Dormers along both slopes.
                for (float u = 8; u < L - 8; u += 9) {
                    if (Math.abs(u - L / 2) < 12) continue;
                    f.rect(0xFFE0D8C4, u - 1.5f, S - 6, u + 1.5f, S - 3);
                    f.rect(0xFF2A3038, u - 0.8f, S - 4, u + 0.8f, S - 3);
                    f.rect(0xFFE0D8C4, u - 1.5f, 3, u + 1.5f, 6);
                }
                // A projecting pedimented portico at the front.
                float cu = L / 2, pw = Math.min(14, L * 0.2f);
                f.rect(0xFFDCD2BC, cu - pw, S - 7, cu + pw, S);
                f.poly(0xFFE8E0CC, cu - pw, S - 7, cu + pw, S - 7, cu, S - 3.5f);
                f.poly(0xFFB8AE98, cu - pw, S, cu + pw, S, cu, S - 3.5f);
                // The central tower: square stages and an octagonal copper cupola.
                float cv = S / 2, ts = Math.min(S * 0.34f, 15);
                f.rect(0x48000000, cu - ts + 4, cv - ts + 4, cu + ts + 4, cv + ts + 4);
                f.rect(0xFFB8AE98, cu - ts, cv - ts, cu + ts, cv + ts);
                f.rect(0xFFD4CAB4, cu - ts, cv - ts, cu + ts - 1.5f, cv + ts - 1.5f);
                for (int k = 0; k < 4; k++) {
                    float a = k < 2 ? -1 : 1, b = k % 2 == 0 ? -1 : 1;
                    f.circle(0xFFE8E0CC, cu + a * (ts - 1.8f), cv + b * (ts - 1.8f), 1.3f);
                }
                float s2 = ts * 0.7f;
                f.ngon(0xFF9A9080, cu, cv, s2 + 1, 8, Math.PI / 8);
                for (int k = 0; k < 8; k++) {
                    double a0 = Math.PI / 8 + k * Math.PI / 4, a1 = a0 + Math.PI / 4, am = (a0 + a1) / 2;
                    float sh = 1f + 0.28f * (float) Math.cos(am + 3 * Math.PI / 4);
                    f.poly(Landmarks.shade(0xFF5E9A86, sh), cu, cv, cu + (float) Math.cos(a0) * s2, cv + (float) Math.sin(a0) * s2,
                            cu + (float) Math.cos(a1) * s2, cv + (float) Math.sin(a1) * s2);
                }
                f.circle(0xFFC8A040, cu, cv, 1.3f);
                f.sign(city, "CUSTOM HOUSE", cu, S - 2, 5, 0xFF2A2A2A);
            }
        };
        d.minS = 11;
        d.minL = 12;
        d.ground = City.PLAZA;
        d.base = City.OFFICE;
        d.inside = Variants.I_OFFICE;
        d.roofCol = 0xFF4C5866;
        d.districts = Variants.DOWN | Variants.OLD | Variants.IND;
        out.add(d);
    }

    // ================================================================== 5 Fire Museum

    /** A vintage fire engine pointing toward -v (out of the bay): ladder truck or steam pumper. */
    static void engine(F f, float u, float v, int kind) {
        float w = 4.2f, l = kind == 0 ? 12 : 9;
        f.roundRect(0x40000000, u - w + 1.2f, v - l + 1.2f, u + w + 1.2f, v + l + 1.2f, 1.5f);
        for (int s = -1; s <= 1; s += 2) {
            f.rect(0xFF1A1A1C, u + s * w - 1.2f, v - l + 1.5f, u + s * w + 1.2f, v - l + 4.5f);
            f.rect(0xFF1A1A1C, u + s * w - 1, v + l - 4.5f, u + s * w + 1, v + l - 1.5f);
        }
        f.roundRect(0xFFB8221E, u - w, v - l, u + w, v + l, 1.5f);
        f.rect(0xFFD8342E, u - w + 0.7f, v - l + 0.7f, u - 0.2f, v + l - 0.7f);
        f.rect(0xFF2A2420, u - w + 0.8f, v - l + 2.5f, u + w - 0.8f, v - l + 4);
        if (kind == 0) {
            // Ladder down the middle.
            f.rect(0xFFC8C8C0, u - 1.8f, v - l + 5, u + 1.8f, v + l + 2);
            f.rect(0xFFB8221E, u - 1.1f, v - l + 5, u + 1.1f, v + l + 2);
            for (float t = v - l + 6; t < v + l + 2; t += 1.6f) f.line(0xFFE0E0D8, 0.5f, u - 1.8f, t, u + 1.8f, t);
        } else {
            // A brass boiler and a hose reel.
            ball(f, u, v + l - 4, 2.4f, 0xFFC89A38);
            f.circle(0xFF2A2A2A, u, v + l - 4, 0.7f);
            f.rect(0xFFE8E2D0, u - 2.6f, v - 2, u + 2.6f, v + 2);
            for (float t = -1.4f; t < 2; t += 1) f.line(0xFFB0A890, 0.4f, u - 2.6f, v + t, u + 2.6f, v + t);
        }
        f.circle(0xFFF0E0A0, u - w + 1, v - l + 0.6f, 0.6f);
        f.circle(0xFFF0E0A0, u + w - 1, v - l + 0.6f, 0.6f);
    }

    static void fireMuseum(List<Landmarks.Def> out) {
        // An old brick fire station with crow-stepped gables, a bell cupola and a tall hose-drying tower; three
        // vintage engines are drawn up on the apron before the red bay doors.
        Landmarks.Def d = new Landmarks.Def("Fire Museum", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, 1, 1, L - 5, S - 6, 2), at(al, L - 3, 1, 2, 2, 6)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] q = pl(this, x0, y0, x1, y1);
                float[] h = bx(f, q[0]), tw = bx(f, q[1]);
                f.rect(0xFFB4AEA2, 0, 0, L, S);
                for (float u = 8; u < L; u += 8) f.line(0x18000000, 0.5f, u, h[3], u, S);
                for (float v = h[3] + 8; v < S; v += 8) f.line(0x14000000, 0.5f, 0, v, L, v);
                // A lawn strip round the side of the tower.
                f.rect(0xFF6A9A50, tw[0] - 2, tw[3] + 3, L, h[3] - 1);
                // Red bay doors on the front and the engines before them.
                int n = Math.max(2, Math.min(4, (int) ((h[2] - h[0]) / 28)));
                float bwid = (h[2] - h[0]) / n;
                for (int k = 0; k < n; k++) {
                    float u = h[0] + bwid * (k + 0.5f);
                    f.rect(0xFF8A2A22, u - 6, h[3], u + 6, h[3] + 1.4f);
                    f.rect(0xFFE8E0C8, u - 6.6f, h[3], u - 6, h[3] + 1.4f);
                    f.rect(0xFFE8E0C8, u + 6, h[3], u + 6.6f, h[3] + 1.4f);
                    f.line(0x30FFFFFF, 0.5f, u - 6, h[3] + 2, u - 6, S - 3);
                    f.line(0x30FFFFFF, 0.5f, u + 6, h[3] + 2, u + 6, S - 3);
                    float ev = h[3] + 16;
                    if (ev + 14 < S - 1) {
                        f.rect(0x30FFFFFF, u - 9, ev - 14, u + 9, ev + 14);
                        engine(f, u, ev, k % 2);
                        // Brass stanchions and a red rope round each exhibit.
                        f.frame(0xFFA02828, 0.5f, u - 8, ev - 13, u + 8, ev + 13);
                        for (int j = 0; j < 4; j++) f.circle(0xFFD0A848, u + (j % 2 == 0 ? -8 : 8), ev + (j < 2 ? -13 : 13), 0.9f);
                    }
                }
                // A hydrant, a station bell on a plinth and a spotted dog.
                float hu = tw[0] + 6, hv = S - 6;
                f.circle(0x40000000, hu + 1, hv + 1, 1.8f);
                f.circle(0xFFC82A22, hu, hv, 1.8f);
                f.circle(0xFFE04A40, hu - 0.4f, hv - 0.4f, 0.8f);
                float bu = L - 7, bv = tw[3] + 8;
                if (bv + 5 < h[3] - 1) {
                    f.rect(0x40000000, bu - 3, bv - 3, bu + 5, bv + 5);
                    f.rect(0xFF8A8478, bu - 4, bv - 4, bu + 4, bv + 4);
                    ball(f, bu, bv, 2.8f, 0xFFB88A38);
                }
                f.oval(0xFFF4F0E8, hu + 5, hv - 1.5f, hu + 9.5f, hv + 0.5f);
                f.circle(0xFF1E1E1E, hu + 6, hv - 0.8f, 0.4f);
                f.circle(0xFF1E1E1E, hu + 8, hv - 0.2f, 0.4f);
                f.circle(0xFFF4F0E8, hu + 10, hv - 0.6f, 1);
                // A hose laid out to dry along the side.
                f.rline(0xFFD8CCAA, 1.2f, tw[2] + 1, tw[1] + 4, tw[2] + 1, h[3] - 4);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                if (part == 0) {
                    f.rect(0xFF8A3E2E, 0, 0, L, S);
                    gable(f, 0xFF6E6878, 3, 1, L - 3, S - 1);
                    for (float v = 3; v < S - 1; v += 2.2f) f.line(0x1C000000, 0.4f, 3, v, L - 3, v);
                    // Skylights and dormers on the slopes.
                    for (float u = 10; u < L - 10; u += 14) {
                        if (Math.abs(u - L / 2) < 8) continue;
                        f.rect(0xFF3A3A44, u - 3, 3, u + 3, 7);
                        f.rect(0xFFA0C0CC, u - 2.4f, 3.6f, u + 2.4f, 5.4f);
                        f.rect(0x40000000, u - 2, S - 7, u + 4, S - 2.5f);
                        f.rect(0xFFE8E0CC, u - 3, S - 8, u + 3, S - 3);
                        f.rect(0xFF5A5460, u - 3, S - 8, u + 3, S - 6);
                    }
                    // Crow-stepped gable ends.
                    for (int s = 0; s < 2; s++) {
                        float u0 = s == 0 ? 0 : L - 3.5f;
                        f.rect(0xFF9A4A36, u0, 0, u0 + 3.5f, S);
                        float st = S / 7;
                        for (int k = 0; k < 7; k++) {
                            float sh = (k < 4 ? k : 6 - k) * 0.25f;
                            f.rect(k < 4 ? 0xFFAA5A44 : 0xFF7A3626, u0 + sh, k * st, u0 + 3.5f - sh, (k + 1) * st - 0.4f);
                        }
                    }
                    // A white bell cupola on the ridge.
                    float cu = L / 2, cv = S / 2;
                    f.rect(0x40000000, cu - 3, cv - 3, cu + 6, cv + 6);
                    f.rect(0xFFE8E4DA, cu - 4, cv - 4, cu + 4, cv + 4);
                    hip(f, 0xFF4E6A5E, cu - 3.5f, cv - 3.5f, cu + 3.5f, cv + 3.5f, false);
                    f.circle(0xFFC8A040, cu, cv, 0.8f);
                    f.sign(city, "FIRE MUSEUM", cu, S - 3, 5, 0xFFF0E0C0);
                } else {
                    // The hose tower: a brick shaft with louvres and a pyramid cap with a weathervane.
                    f.rect(0xFF7E3828, 0, 0, L, S);
                    f.rect(0xFF9A4A36, 0.8f, 0.8f, L - 0.8f, S - 0.8f);
                    hip(f, 0xFF4A4650, 3, 3, L - 3, S - 3, false);
                    for (float t = 4; t < L - 3; t += 2) {
                        f.line(0xFF3A2A24, 0.6f, t, 1.2f, t, 2.6f);
                        f.line(0xFF3A2A24, 0.6f, t, S - 2.6f, t, S - 1.2f);
                    }
                    f.line(0xFF2A2A2A, 0.7f, L / 2 - 4, S / 2, L / 2 + 4, S / 2);
                    f.poly(0xFF2A2A2A, L / 2 + 4, S / 2 - 1.5f, L / 2 + 6, S / 2, L / 2 + 4, S / 2 + 1.5f);
                    f.circle(0xFFC8A040, L / 2, S / 2, 1);
                }
            }
        };
        d.minS = 11;
        d.minL = 12;
        d.ground = City.PLAZA;
        d.base = City.OFFICE;
        d.inside = Variants.I_FIRE;
        d.roofCol = 0xFF585260;
        d.districts = Variants.OLD | Variants.MID | Variants.SUB;
        out.add(d);
    }

    // ================================================================== 6 Domed Basilica

    static void basilica(List<Landmarks.Def> out) {
        // A Greek-cross basilica under a great ribbed lead dome on a windowed drum, with small corner domes and
        // terracotta arms; before it an oval piazza embraced by colonnades, with an obelisk and two fountains.
        Landmarks.Def d = new Landmarks.Def("Domed Basilica", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int s = Math.min(S - 2, L - 6);
                return new int[][]{at(al, L - 1 - s, (S - s) / 2, s, s, 4)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] h = bx(f, pl(this, x0, y0, x1, y1)[0]);
                f.rect(0xFFD4CAB6, 0, 0, L, S);
                for (float u = 5; u < L; u += 10) f.line(0x14000000, 0.5f, u, 0, u, S);
                // Lawns and cypresses round the church.
                f.rect(0xFF6A9450, h[0] - 4, 0, L, h[1] - 2);
                f.rect(0xFF6A9450, h[0] - 4, h[3] + 2, L, S);
                for (float u = h[0] + 4; u < L - 4; u += 20) {
                    if (h[1] > 12) conifer(f, u, h[1] / 2, Math.min(6, h[1] * 0.3f), 0xFF2E5230);
                    if (S - h[3] > 12) conifer(f, u + 6, (h[3] + S) / 2, Math.min(6, (S - h[3]) * 0.3f), 0xFF2E5230);
                }
                // The oval piazza.
                float pu1 = h[0] - 7, cu = pu1 / 2, cv = S / 2, ru = Math.max(18, pu1 / 2 - 3), rv = S * 0.44f;
                f.oval(0xFFC8BCA4, cu - ru, cv - rv, cu + ru, cv + rv);
                f.oval(0xFFDCD2BE, cu - ru + 6, cv - rv + 6, cu + ru - 6, cv + rv - 6);
                for (int k = 0; k < 16; k++) {
                    double a = k * Math.PI / 8;
                    f.line(0x30A09070, 0.8f, cu, cv, cu + (float) Math.cos(a) * (ru - 6), cv + (float) Math.sin(a) * (rv - 6));
                }
                f.oval(0xFFC0B49C, cu - (ru - 6) * 0.5f, cv - (rv - 6) * 0.5f, cu + (ru - 6) * 0.5f, cv + (rv - 6) * 0.5f);
                f.oval(0xFFDCD2BE, cu - (ru - 6) * 0.45f, cv - (rv - 6) * 0.45f, cu + (ru - 6) * 0.45f, cv + (rv - 6) * 0.45f);
                // The colonnades: two curved roofed arms with rows of columns, open toward the church and street.
                int nc = Math.max(40, (int) ((ru + rv) * 0.7f));
                for (int k = 0; k < nc; k++) {
                    double a = k * 2 * Math.PI / nc;
                    float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
                    if (Math.abs(ca) > 0.72f) continue;
                    float u = cu + ca * (ru - 3), v = cv + sa * (rv - 3);
                    f.circle(0x40000000, u + 1.6f, v + 1.6f, 3.4f);
                    f.circle(0xFFE6DCC6, u, v, 3.4f);
                    f.circle(0xFFB4A88E, u + ca * 1.6f, v + sa * 1.6f, 0.9f);
                    f.circle(0xFFB4A88E, u - ca * 1.6f, v - sa * 1.6f, 0.9f);
                }
                // The obelisk with its long shadow, and two fountains.
                f.tall(cu, cv, 1.6f, 34);
                f.rect(0xFFA89C86, cu - 3, cv - 3, cu + 3, cv + 3);
                f.rect(0xFFCFC4AC, cu - 2, cv - 2, cu + 2, cv + 2);
                f.poly(0xFFE8DEC8, cu - 2, cv - 2, cu + 2, cv - 2, cu, cv);
                f.poly(0xFFB0A48C, cu + 2, cv - 2, cu + 2, cv + 2, cu, cv);
                for (int s = -1; s <= 1; s += 2) {
                    float fv = cv + s * rv * 0.55f;
                    f.circle(0x40000000, cu + 1.5f, fv + 1.5f, 5);
                    f.circle(0xFFB8AC94, cu, fv, 5);
                    f.circle(0xFF5A9CC4, cu, fv, 4);
                    f.circle(0xFFB8AC94, cu, fv, 1.6f);
                    f.circle(0xB0FFFFFF, cu, fv, 0.9f);
                }
                // Broad steps up to the doors.
                float mv = (h[1] + h[3]) / 2, aw = (h[3] - h[1]) * 0.2f;
                for (int k = 0; k < 4; k++) {
                    float u = h[0] - 6 + k * 1.5f;
                    f.rect(k % 2 == 0 ? 0xFFCCC2AC : 0xFFBDB29C, u, mv - aw - 6 + k, u + 1.5f, mv + aw + 6 - k);
                }
                f.lamp(h[0] - 8, mv - aw - 8);
                f.lamp(h[0] - 8, mv + aw + 8);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, cu = L / 2, cv = S / 2, m = Math.min(L, S);
                float aw = m * 0.2f;
                f.rect(0xFFC9BEA6, 0, 0, L, S);
                f.rect(0xFF7C858C, 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                // The four terracotta arms, ridges running out from the crossing.
                int tc = 0xFFB0624A;
                f.poly(City.lighten(tc, 0.12f), cu - aw, 1, cu + aw, 1, cu, cv);
                f.poly(City.darken(tc, 0.85f), cu - aw, 1, cu, cv, cu - aw, cv);
                f.poly(City.darken(tc, 0.95f), cu + aw, 1, cu, cv, cu + aw, cv);
                f.rect(City.lighten(tc, 0.1f), cu - aw, 1, cu, cv);
                f.rect(City.darken(tc, 0.8f), cu, 1, cu + aw, cv);
                f.rect(City.lighten(tc, 0.1f), cu - aw, cv, cu, S - 1);
                f.rect(City.darken(tc, 0.8f), cu, cv, cu + aw, S - 1);
                f.rect(City.lighten(tc, 0.16f), 1, cv - aw, cu, cv);
                f.rect(City.darken(tc, 0.75f), 1, cv, cu, cv + aw);
                f.rect(City.lighten(tc, 0.16f), cu, cv - aw, L - 1, cv);
                f.rect(City.darken(tc, 0.75f), cu, cv, L - 1, cv + aw);
                for (float t = 3; t < m / 2; t += 2.5f) {
                    f.line(0x20000000, 0.4f, cu - aw, t, cu + aw, t);
                    f.line(0x20000000, 0.4f, cu - aw, S - t, cu + aw, S - t);
                    f.line(0x20000000, 0.4f, t, cv - aw, t, cv + aw);
                    f.line(0x20000000, 0.4f, L - t, cv - aw, L - t, cv + aw);
                }
                f.line(0xFFE0A080, 0.9f, cu, 1, cu, S - 1);
                f.line(0xFFE0A080, 0.9f, 1, cv, L - 1, cv);
                // Pediments on the arm ends.
                f.rect(0xFFE2D8C2, cu - aw, 0, cu + aw, 2);
                f.rect(0xFFE2D8C2, cu - aw, S - 2, cu + aw, S);
                f.rect(0xFFE2D8C2, 0, cv - aw, 2, cv + aw);
                f.rect(0xFFE2D8C2, L - 2, cv - aw, L, cv + aw);
                // Small lead domes in the four corners.
                float q = (m / 2 - aw) / 2 + 1.5f, rr = Math.max(2.5f, q * 0.6f);
                ball(f, q, q, rr, 0xFF8A949C);
                ball(f, L - q, q, rr, 0xFF8A949C);
                ball(f, q, S - q, rr, 0xFF8A949C);
                ball(f, L - q, S - q, rr, 0xFF8A949C);
                // The drum with its windows, the great ribbed dome and the lantern.
                float R = m * 0.3f;
                f.circle(0x50000000, cu + 4, cv + 4, R + 2);
                f.circle(0xFFD8CEB8, cu, cv, R + 2.5f);
                for (int k = 0; k < 16; k++) {
                    double a = k * Math.PI / 8;
                    f.circle(0xFF3A4048, cu + (float) Math.cos(a) * (R + 1.2f), cv + (float) Math.sin(a) * (R + 1.2f), 0.8f);
                }
                dome(f, cu, cv, R, 0xFF7E8C96, 16);
                for (int k = 0; k < 16; k++) {
                    double a = k * Math.PI / 8;
                    f.line(0xFFD0D4D0, 0.6f, cu + (float) Math.cos(a) * R * 0.25f, cv + (float) Math.sin(a) * R * 0.25f,
                            cu + (float) Math.cos(a) * R, cv + (float) Math.sin(a) * R);
                }
                f.circle(0xFFE0D6C0, cu, cv, R * 0.24f);
                f.circle(0xFF9AA2A8, cu - 0.3f, cv - 0.3f, R * 0.14f);
                f.circle(0xFFD8B048, cu, cv, R * 0.06f + 0.4f);
            }
        };
        d.minS = 10;
        d.minL = 14;
        d.ground = City.PLAZA;
        d.base = City.OFFICE;
        d.inside = Variants.I_PEWS;
        d.roofCol = 0xFF7E8C96;
        d.districts = Variants.OLD | Variants.DOWN | Variants.MID;
        out.add(d);
    }

    // ================================================================== 7 Mosque

    /** A minaret from above: a square base, the balcony ring and a slender pointed cap with a finial. */
    static void minaret(F f, float u, float v, float r) {
        f.rect(0xFFD8CEBA, u - r, v - r, u + r, v + r);
        f.circle(0xFFB8AE98, u, v, r * 0.85f);
        f.ring(0xFF6A6458, 0.5f, u, v, r * 0.8f);
        f.circle(0xFFEDE6D6, u, v, r * 0.6f);
        f.ngon(0xFF8A9EA4, u, v, r * 0.5f, 8, Math.PI / 8);
        dome(f, u, v, r * 0.45f, 0xFF5A8EA0, 8);
        f.circle(0xFFD8B048, u, v, 0.7f);
    }

    static void mosque(List<Landmarks.Def> out) {
        // A domed prayer hall in teal tile with semi-domes and corner domes; a marble courtyard with an arcade of
        // little domes on three sides, an octagonal ablution fountain, orange trees, and four slender minarets.
        Landmarks.Def d = new Landmarks.Def("Mosque", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int hw = Math.max(5, Math.round(L * 0.42f));
                return new int[][]{at(al, L - 1 - hw, 1, hw, S - 2, 3), at(al, L - 3 - hw, 1, 1, 1, 10),
                        at(al, L - 3 - hw, S - 2, 1, 1, 10), at(al, 1, 1, 1, 1, 10), at(al, 1, S - 2, 1, 1, 10)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] q = pl(this, x0, y0, x1, y1);
                float[] h = bx(f, q[0]);
                f.rect(0xFFD6CCB8, 0, 0, L, S);
                // The marble court with a lattice of star tiles.
                float cu0 = 6, cu1 = h[0] - 4, cv0 = 6, cv1 = S - 6;
                f.rect(0xFFEEE8DC, cu0, cv0, cu1, cv1);
                for (float u = cu0 + 9; u < cu1 - 3; u += 16)
                    for (float v = cv0 + 9; v < cv1 - 3; v += 16) {
                        f.rect(0xFFD8CCB4, u - 2, v - 2, u + 2, v + 2);
                        f.poly(0xFF8CB8BC, u, v - 2.8f, u + 2.8f, v, u, v + 2.8f, u - 2.8f, v);
                        f.circle(0xFFEEE8DC, u, v, 0.9f);
                    }
                // The arcade round three sides: a roofed walk with a row of small domes.
                float aw = 8;
                f.rect(0xFFCDC2AA, 0, 0, cu1 + 2, aw);
                f.rect(0xFFCDC2AA, 0, S - aw, cu1 + 2, S);
                f.rect(0xFFCDC2AA, 0, 0, aw, S);
                f.line(0x50000000, 1.2f, aw, aw, cu1 + 2, aw);
                f.line(0x50000000, 1.2f, aw, aw, aw, S - aw);
                for (float u = aw + 4; u < cu1 - 1; u += 8) {
                    ball(f, u, aw / 2, 2.8f, 0xFFE6DECC);
                    ball(f, u, S - aw / 2, 2.8f, 0xFFE6DECC);
                    f.circle(0xFFB8AC94, u - 4, aw + 0.8f, 0.9f);
                    f.circle(0xFFB8AC94, u - 4, S - aw - 0.8f, 0.9f);
                }
                for (float v = aw + 4; v < S - aw - 1; v += 8) {
                    if (Math.abs(v - S / 2) < 6) continue;
                    ball(f, aw / 2, v, 2.8f, 0xFFE6DECC);
                    f.circle(0xFFB8AC94, aw + 0.8f, v - 4, 0.9f);
                }
                // The portal into the court, on the outer side.
                f.rect(0xFF2E7480, 0, S / 2 - 6, aw + 1, S / 2 + 6);
                f.rect(0xFFE6DECC, 0.8f, S / 2 - 5, aw, S / 2 + 5);
                f.poly(0xFF3A8A96, 1.5f, S / 2 - 3.5f, aw - 1, S / 2 - 3.5f, aw - 1, S / 2 + 3.5f, 1.5f, S / 2 + 3.5f, -0.5f, S / 2);
                // The ablution fountain.
                float fu = (aw + cu1) / 2, fv = S / 2, fr = Math.min(10, Math.min(cu1 - aw, S) * 0.18f);
                f.circle(0x40000000, fu + 2, fv + 2, fr + 1);
                f.ngon(0xFFB8AE98, fu, fv, fr + 1.5f, 8, Math.PI / 8);
                f.ngon(0xFF4AA0B4, fu, fv, fr, 8, Math.PI / 8);
                f.ngon(0xFF6CC0D0, fu - 0.5f, fv - 0.5f, fr * 0.7f, 8, Math.PI / 8);
                ball(f, fu, fv, fr * 0.38f, 0xFF5E9AA8);
                for (int k = 0; k < 8; k++) {
                    double a = Math.PI / 8 + k * Math.PI / 4;
                    f.circle(0xFFE8E0CC, fu + (float) Math.cos(a) * fr * 0.62f, fv + (float) Math.sin(a) * fr * 0.62f, 0.7f);
                }
                // Orange trees in the court's corners.
                float tu0 = aw + 7, tu1 = cu1 - 5, tv0 = aw + 7, tv1 = S - aw - 7;
                if (tu1 - tu0 > 2 * fr + 8) {
                    for (int k = 0; k < 4; k++) {
                        float u = k % 2 == 0 ? tu0 : tu1, v = k < 2 ? tv0 : tv1;
                        f.circle(0xFFB8AC94, u, v, 4.2f);
                        f.tree(u, v, 3.6f, 0xFF3E7A36);
                        f.circle(0xFFE89030, u + 1, v - 1, 0.6f);
                        f.circle(0xFFE89030, u - 1.4f, v + 0.6f, 0.6f);
                    }
                }
                // Prayer-hall steps and the gap between court and hall.
                f.rect(0xFFDCD2C0, cu1 + 2, 0, h[0], S);
                for (int k = 0; k < 3; k++) f.line(0x30000000, 0.6f, h[0] - 1.5f - k * 1.5f, h[1] + 8, h[0] - 1.5f - k * 1.5f, h[3] - 8);
                f.sign(city, "MOSQUE", fu, S - aw - 3, 4, 0xFF2A4A50);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                if (part > 0) {
                    minaret(f, L / 2, S / 2, L / 2);
                    return;
                }
                float cu = L / 2, cv = S / 2, m = Math.min(L, S);
                f.rect(0xFFD2C8B2, 0, 0, L, S);
                f.rect(0xFFE6DECC, 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                crenels(f, 0xFFC4B8A0, 0, 0, L, S, 1.5f);
                int tl = 0xFF3E8C9A;
                // Semi-domes either side along the hall's long axis.
                float R = m * 0.34f;
                for (int s = -1; s <= 1; s += 2) {
                    float su = cu + s * R * 0.85f, sr = R * 0.58f;
                    f.circle(0x30000000, su + 2, cv + 2, sr);
                    dome(f, su, cv, sr, City.lighten(tl, 0.06f), 12);
                }
                // Little corner domes.
                float q = Math.min(m * 0.17f, 9);
                ball(f, q + 1.5f, q + 1.5f, q * 0.7f, tl);
                ball(f, L - q - 1.5f, q + 1.5f, q * 0.7f, tl);
                ball(f, q + 1.5f, S - q - 1.5f, q * 0.7f, tl);
                ball(f, L - q - 1.5f, S - q - 1.5f, q * 0.7f, tl);
                // The great dome on its drum.
                f.circle(0x50000000, cu + 4, cv + 4, R + 2);
                f.circle(0xFFD8CEB8, cu, cv, R + 2.2f);
                for (int k = 0; k < 20; k++) {
                    double a = k * Math.PI / 10;
                    f.circle(0xFF2A3E44, cu + (float) Math.cos(a) * (R + 1.1f), cv + (float) Math.sin(a) * (R + 1.1f), 0.6f);
                }
                dome(f, cu, cv, R, tl, 20);
                for (int k = 0; k < 10; k++) {
                    double a = k * Math.PI / 5;
                    f.line(0x50E8D8A0, 0.5f, cu + (float) Math.cos(a) * R * 0.2f, cv + (float) Math.sin(a) * R * 0.2f,
                            cu + (float) Math.cos(a) * R, cv + (float) Math.sin(a) * R);
                }
                f.circle(0xFFD8B048, cu, cv, 1.6f);
                // The crescent finial.
                f.circle(0xFFE8C860, cu, cv - 3, 1.5f);
                f.circle(tl, cu + 0.6f, cv - 3.4f, 1.2f);
            }
        };
        d.minS = 10;
        d.minL = 14;
        d.ground = City.PLAZA;
        d.base = City.OFFICE;
        d.inside = Variants.I_PEWS;
        d.roofCol = 0xFF3E8C9A;
        d.districts = Variants.MID | Variants.SUB | Variants.OLD | Variants.DOWN;
        out.add(d);
    }

    // ================================================================== 8 Stave Church

    /** Shingle courses: short ticks in rows along u over the rectangle. */
    static void shingles(F f, int col, float u0, float v0, float u1, float v1, float step) {
        int row = 0;
        for (float v = v0 + step; v < v1 - 0.3f; v += step, row++) {
            f.line(col, 0.4f, u0, v, u1, v);
            for (float u = u0 + (row % 2 == 0 ? 1 : 2.2f); u < u1; u += 2.4f) f.line(col, 0.35f, u, v - step, u, v);
        }
    }

    /** A carved dragon head jutting out from a gable end, toward direction (du, dv). */
    static void dragon(F f, float u, float v, float du, float dv) {
        f.rline(0xFF2A1E16, 1.1f, u, v, u + du * 3, v + dv * 3);
        f.rline(0xFF2A1E16, 0.8f, u + du * 3, v + dv * 3, u + du * 4.2f - dv * 1.2f, v + dv * 4.2f + du * 1.2f);
        f.circle(0xFF8A2A1E, u + du * 3.4f, v + dv * 3.4f, 0.5f);
    }

    /** A steep gable with strong light and shade (the ridge along u) and shingle courses. */
    static void sgable(F f, int col, float u0, float v0, float u1, float v1) {
        float m = (v0 + v1) / 2;
        f.rect(City.lighten(col, 0.2f), u0, v0, u1, m);
        f.rect(City.darken(col, 0.6f), u0, m, u1, v1);
        shingles(f, 0x30000000, u0, v0, u1, v1, 1.8f);
        f.line(City.lighten(col, 0.45f), 1, u0, m, u1, m);
        f.frame(0xFF1E1610, 0.7f, u0, v0, u1, v1);
    }

    static void stave(List<Landmarks.Def> out) {
        // A dark tarred stave church: stacked shingle roofs, a cross gable, a tiered tower with a spire and dragon
        // heads on every gable; a timber bell house, a fenced churchyard of spruce and birch, crosses and a rune stone.
        Landmarks.Def d = new Landmarks.Def("Stave Church", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int lu = Math.min(8, L - 5), lv = Math.min(6, S - 5);
                return new int[][]{at(al, Math.max(4, (L - lu) / 2 + 1), (S - lv) / 2, lu, lv, 3), at(al, 1, 1, 2, 2, 2)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] q = pl(this, x0, y0, x1, y1);
                float[] h = bx(f, q[0]), b = bx(f, q[1]);
                f.rect(0xFF5C7E46, 0, 0, L, S);
                for (int k = 0; k < 40; k++) {
                    float u = r.nextFloat() * L, v = r.nextFloat() * S;
                    f.circle(k % 2 == 0 ? 0xFF547640 : 0xFF66884E, u, v, 2 + r.nextFloat() * 3);
                }
                // Gravel paths: from the lych gate to the west door, and to the bell house.
                float mv = (h[1] + h[3]) / 2;
                f.rect(0xFFB0A688, 0, mv - 3, h[0], mv + 3);
                f.rect(0xFFB0A688, (b[0] + b[2]) / 2 - 2.5f, b[3], (b[0] + b[2]) / 2 + 2.5f, mv);
                f.rect(0xFFA49A7C, h[0] - 3, h[1] - 3, h[2] + 3, h[3] + 3);
                // The wooden fence round the churchyard, with a lych gate.
                float fi = 3;
                f.frame(0xFF5A4030, 1, fi, fi, L - fi, S - fi);
                for (float u = fi; u < L - fi; u += 5) {
                    f.circle(0xFF3A2A20, u, fi, 0.8f);
                    f.circle(0xFF3A2A20, u, S - fi, 0.8f);
                }
                for (float v = fi; v < S - fi; v += 5) {
                    f.circle(0xFF3A2A20, fi, v, 0.8f);
                    f.circle(0xFF3A2A20, L - fi, v, 0.8f);
                }
                f.rect(0xFFB0A688, 0, mv - 3, fi + 1, mv + 3);
                f.rect(0x40000000, fi - 2, mv - 5, fi + 6, mv + 7);
                gableV(f, 0xFF3E3026, fi - 3, mv - 6, fi + 4, mv + 6);
                // Graves: small wooden crosses and a few stones, in rows behind the church.
                for (float u = h[0] + 2; u < h[2] - 2; u += 6) {
                    for (int s = 0; s < 2; s++) {
                        float v = s == 0 ? h[1] - 8 : h[3] + 8;
                        if (v < fi + 4 || v > S - fi - 4) continue;
                        if (((int) u / 6 + s) % 3 == 0) {
                            f.rect(0x40000000, u - 1, v - 1.2f, u + 2.2f, v + 2.2f);
                            f.rect(0xFF8A8A84, u - 1.4f, v - 1.4f, u + 1.6f, v + 1.4f);
                        } else {
                            f.line(0xFF3A2A1E, 0.8f, u, v - 2, u, v + 2);
                            f.line(0xFF3A2A1E, 0.8f, u - 1.3f, v - 0.8f, u + 1.3f, v - 0.8f);
                        }
                    }
                }
                // A rune stone by the gate.
                f.oval(0x40000000, fi + 7, mv + 7, fi + 13, mv + 11);
                f.oval(0xFF8C8A80, fi + 6, mv + 6, fi + 11.5f, mv + 10);
                f.line(0xFFA8463A, 0.4f, fi + 7, mv + 8, fi + 10.5f, mv + 8);
                // Spruce at the back, birches at the front.
                for (float u = 10; u < L - 6; u += 11 + r.nextFloat() * 8) {
                    if (u > b[0] - 6 && u < b[2] + 6) continue;
                    conifer(f, u, fi + 6 + r.nextFloat() * 4, 4.5f + r.nextFloat() * 2.5f, 0xFF2C4A30);
                }
                for (float u = h[0] - 6; u < L - 8; u += 14 + r.nextFloat() * 10) {
                    float v = S - fi - 7 - r.nextFloat() * 4, rr = 3 + r.nextFloat() * 1.5f;
                    f.circle(0x40000000, u + 2, v + 2, rr);
                    f.circle(0xFF8AAA54, u, v, rr);
                    f.circle(0xFFA8C46A, u - 1, v - 1, rr * 0.5f);
                    f.circle(0xFFE8E4DC, u + rr * 0.2f, v + rr * 0.2f, 0.5f);
                }
                conifer(f, L - 8, mv, 5, 0xFF2C4A30);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int sk = 0xFF3A2C22;
                if (part == 1) {
                    // The bell house: a shingled pyramid over an open belfry.
                    f.rect(0xFF2A1E16, 0, 0, L, S);
                    hip(f, sk, 1, 1, L - 1, S - 1, false);
                    shingles(f, 0x30000000, 2, 2, L - 2, S - 2, 2);
                    dragon(f, 1, 1, -0.7f, -0.7f);
                    dragon(f, L - 1, S - 1, 0.7f, 0.7f);
                    f.circle(0xFF8A6A3A, L / 2, S / 2, 1);
                    return;
                }
                // The low skirt roof over the outer gallery.
                hip(f, City.darken(sk, 0.9f), 0, 0, L, S, false);
                shingles(f, 0x24000000, 1, 1, L - 1, S - 1, 2);
                // The nave and the lower chancel with its round apse.
                float nu0 = L * 0.1f, nu1 = L * 0.74f, nv0 = S * 0.2f, nv1 = S * 0.8f;
                f.rect(0x40000000, nu0 + 2, nv0 + 2, nu1 + 2, nv1 + 2);
                sgable(f, 0xFF4E3A28, nu0, nv0, nu1, nv1);
                float cu1 = L * 0.94f, cv0 = S * 0.32f, cv1 = S * 0.68f;
                f.ngon(0xFF3A2A1E, cu1 - (cv1 - cv0) / 2, S / 2, (cv1 - cv0) / 2, 8, Math.PI / 8);
                f.ngon(0xFF4A3828, cu1 - (cv1 - cv0) / 2 - 0.6f, S / 2 - 0.6f, (cv1 - cv0) / 2 - 1.2f, 8, Math.PI / 8);
                sgable(f, 0xFF483626, nu1, cv0, cu1 - (cv1 - cv0) / 2, cv1);
                // The cross gable through the crossing.
                float xu = L * 0.42f, xw = (nv1 - nv0) * 0.36f;
                f.rect(0x40000000, xu - xw + 2, S * 0.08f + 2, xu + xw + 2, S * 0.92f + 2);
                f.rect(0xFF6A5038, xu - xw, S * 0.08f, xu, S * 0.92f);
                f.rect(0xFF2E2218, xu, S * 0.08f, xu + xw, S * 0.92f);
                shingles(f, 0x30000000, xu - xw, S * 0.08f, xu + xw, S * 0.92f, 1.8f);
                f.line(0xFF9A7A58, 1, xu, S * 0.08f, xu, S * 0.92f);
                // The tiered tower: two shingled stages and a spire.
                float ts = Math.min(S, L) * 0.22f;
                for (int k = 0; k < 2; k++) {
                    float s = ts * (1 - k * 0.32f);
                    f.rect(0x48000000, xu - s + 2, S / 2 - s + 2, xu + s + 2, S / 2 + s + 2);
                    hip(f, Landmarks.shade(0xFF5A4430, 1 + k * 0.1f), xu - s, S / 2 - s, xu + s, S / 2 + s, false);
                    f.frame(0xFF1E1610, 0.7f, xu - s, S / 2 - s, xu + s, S / 2 + s);
                    dragon(f, xu - s, S / 2 - s, -0.7f, -0.7f);
                    dragon(f, xu + s, S / 2 - s, 0.7f, -0.7f);
                    dragon(f, xu - s, S / 2 + s, -0.7f, 0.7f);
                    dragon(f, xu + s, S / 2 + s, 0.7f, 0.7f);
                }
                float s3 = ts * 0.34f;
                f.tall(xu, S / 2, 1.5f, 12);
                hip(f, 0xFF5A4430, xu - s3, S / 2 - s3, xu + s3, S / 2 + s3, false);
                f.circle(0xFFC8A040, xu, S / 2, 0.7f);
                // Dragon heads on the gable ends.
                dragon(f, nu0, S / 2, -1, 0);
                dragon(f, xu, S * 0.08f, 0, -1);
                dragon(f, xu, S * 0.92f, 0, 1);
            }
        };
        d.minS = 10;
        d.minL = 12;
        d.ground = City.GRASS;
        d.base = City.OFFICE;
        d.inside = Variants.I_PEWS;
        d.roofCol = 0xFF3A2C22;
        d.districts = Variants.SUB | Variants.PARK | Variants.OLD;
        out.add(d);
    }

    // ================================================================== 9 Synagogue

    /** A Star of David: two triangles round (u, v). */
    static void hexagram(F f, int col, float u, float v, float r) {
        f.poly(col, u, v - r, u + r * 0.866f, v + r * 0.5f, u - r * 0.866f, v + r * 0.5f);
        f.poly(col, u, v + r, u + r * 0.866f, v - r * 0.5f, u - r * 0.866f, v - r * 0.5f);
    }

    static void synagogue(List<Landmarks.Def> out) {
        // A Moorish-revival synagogue: banded red and cream walls, a gold-ribbed copper dome on a drum, twin towers
        // with gilded onion domes on the front; a forecourt with a Star of David in blue tile, olive trees and a
        // walled memorial garden.
        Landmarks.Def d = new Landmarks.Def("Synagogue", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int lv = Math.min(S - 5, L - 8), lu = Math.min(L - 6, lv + 4);
                return new int[][]{at(al, (L - lu) / 2, 1, lu, lv, 3)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] h = bx(f, pl(this, x0, y0, x1, y1)[0]);
                f.rect(0xFF6A9050, 0, 0, L, S);
                // The forecourt in warm stone, a Star of David laid in blue tile at its centre.
                float mu = (h[0] + h[2]) / 2, v0 = h[3], v1 = S - 2;
                f.rect(0xFFD8CCB0, h[0] - 4, h[1] - 3, h[2] + 4, v1);
                for (float u = h[0]; u < h[2]; u += 6) f.line(0x16000000, 0.5f, u, v0, u, v1);
                float sv = (v0 + v1) / 2 + 1, sr = Math.min((v1 - v0) * 0.4f, (h[2] - h[0]) * 0.3f);
                if (sr > 6) {
                    f.circle(0xFFC4B696, mu, sv, sr + 3);
                    f.circle(0xFFE8DEC6, mu, sv, sr + 1.5f);
                    hexagram(f, 0xFF2E5C9A, mu, sv, sr);
                    hexagram(f, 0xFFE8DEC6, mu, sv, sr * 0.72f);
                    f.circle(0xFF4E7CBA, mu, sv, sr * 0.3f);
                }
                // Railings and gate piers along the street.
                f.line(0xFF2E3236, 0.9f, 0, S - 1.5f, mu - 8, S - 1.5f);
                f.line(0xFF2E3236, 0.9f, mu + 8, S - 1.5f, L, S - 1.5f);
                f.rect(0xFFB04A3A, mu - 10, S - 3, mu - 7, S);
                f.rect(0xFFB04A3A, mu + 7, S - 3, mu + 10, S);
                // Olive trees on the side lawns.
                for (float v = 10; v < S - 8; v += 26) {
                    if (h[0] > 14) f.tree(h[0] / 2 + (v % 3) - 1, v, 5.5f, 0xFF7A8E5A);
                    if (L - h[2] > 14) f.tree((h[2] + L) / 2 + 1, v + 9, 5.5f, 0xFF7A8E5A);
                }
                // A walled memorial garden on one side: a low wall, gravel, a stone and candles.
                float gu0 = 3, gu1 = h[0] - 6;
                if (gu1 - gu0 > 30) {
                    f.rect(0xFFB8AE98, gu0, S * 0.25f, gu1, S * 0.75f);
                    f.rect(0xFFD0C6AE, gu0 + 2, S * 0.25f + 2, gu1 - 2, S * 0.75f - 2);
                    float gu = (gu0 + gu1) / 2;
                    f.rect(0x40000000, gu - 4, S / 2 - 2, gu + 6, S / 2 + 4);
                    f.rect(0xFF5A5A5E, gu - 5, S / 2 - 3, gu + 5, S / 2 + 3);
                    for (int k = -2; k <= 2; k++) f.circle(0xFFF0D070, gu + k * 2, S / 2 + 4.5f, 0.6f);
                }
                f.bench(mu - 14, v0 + 5, true);
                f.bench(mu + 14, v0 + 5, true);
                f.lamp(mu - 10, S - 5);
                f.lamp(mu + 10, S - 5);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, cu = L / 2, cv = S * 0.45f, m = Math.min(L, S);
                // Banded walls (the parapet), a flat roof.
                f.rect(0xFFB04A3A, 0, 0, L, S);
                for (float t = 0; t < L; t += 3) f.rect(0xFFE6D8B8, t, 0, t + 1.5f, 1.4f);
                for (float t = 0; t < L; t += 3) f.rect(0xFFE6D8B8, t, S - 1.4f, t + 1.5f, S);
                for (float t = 0; t < S; t += 3) f.rect(0xFFE6D8B8, 0, t, 1.4f, t + 1.5f);
                for (float t = 0; t < S; t += 3) f.rect(0xFFE6D8B8, L - 1.4f, t, L, t + 1.5f);
                f.rect(0xFFB8AC96, 2, 2, L - 2, S - 2);
                f.rect(0xFFC8BCA6, 2, 2, L - 2, 3.5f);
                for (float t = 6; t < L - 4; t += 5) f.line(0x18000000, 0.5f, t, 2, t, S - 2);
                // The drum and the ribbed copper dome, a gilt star at the top.
                float R = m * 0.3f;
                f.circle(0x50000000, cu + 4, cv + 4, R + 2);
                f.circle(0xFFE6D8B8, cu, cv, R + 2.5f);
                for (int k = 0; k < 12; k++) {
                    double a = k * Math.PI / 6;
                    f.circle(0xFF3A4A6A, cu + (float) Math.cos(a) * (R + 1.2f), cv + (float) Math.sin(a) * (R + 1.2f), 0.9f);
                }
                dome(f, cu, cv, R, 0xFF5E9A86, 12);
                for (int k = 0; k < 12; k++) {
                    double a = k * Math.PI / 6;
                    f.line(0xFFD8B048, 0.7f, cu, cv, cu + (float) Math.cos(a) * R, cv + (float) Math.sin(a) * R);
                }
                f.circle(0xFFE6D8B8, cu, cv, R * 0.22f);
                hexagram(f, 0xFFD8B048, cu, cv, R * 0.18f);
                // Twin towers with gilded onion domes on the front corners.
                float ts = Math.min(m * 0.16f, 9);
                for (int s = -1; s <= 1; s += 2) {
                    float tu = s < 0 ? ts + 1 : L - ts - 1, tv = S - ts - 1;
                    f.rect(0x48000000, tu - ts + 3, tv - ts + 3, tu + ts + 3, tv + ts + 3);
                    f.rect(0xFFB04A3A, tu - ts, tv - ts, tu + ts, tv + ts);
                    f.frame(0xFFE6D8B8, 1, tu - ts + 1, tv - ts + 1, tu + ts - 1, tv + ts - 1);
                    f.ngon(0xFFE6D8B8, tu, tv, ts * 0.8f, 8, Math.PI / 8);
                    dome(f, tu, tv, ts * 0.65f, 0xFFD0A040, 10);
                    f.circle(0xFFF0D880, tu - ts * 0.2f, tv - ts * 0.2f, ts * 0.15f);
                    f.circle(0xFF8A6A20, tu, tv, 0.6f);
                }
                // Rose-window gable between them.
                f.rect(0xFFE6D8B8, cu - ts, S - 4, cu + ts, S);
                f.circle(0xFF3A4A6A, cu, S - 2, 1.5f);
            }
        };
        d.minS = 10;
        d.minL = 12;
        d.ground = City.GRASS;
        d.base = City.OFFICE;
        d.inside = Variants.I_PEWS;
        d.roofCol = 0xFFB04A3A;
        d.districts = Variants.MID | Variants.OLD | Variants.SUB | Variants.DOWN;
        out.add(d);
    }

    // ================================================================== 10 Music Conservatory

    static void conservatory(List<Landmarks.Def> out) {
        // Three terracotta-roofed wings round a garden: a striped bandstand in the middle, a path laid out like a
        // music stave with clipped "notes", and a zinc-roofed recital hall bulging from the back wing.
        Landmarks.Def d = new Landmarks.Def("Music Conservatory", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, 1, 1, L - 2, 3, 3), at(al, 1, 5, 3, S - 6, 2), at(al, L - 4, 5, 3, S - 6, 2)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] q = pl(this, x0, y0, x1, y1);
                float[] a = bx(f, q[0]), b = bx(f, q[1]), e = bx(f, q[2]);
                f.rect(0xFFCFC6B2, 0, 0, L, S);
                // The garden between the wings.
                float gu0 = b[2] + 4, gu1 = e[0] - 4, gv0 = a[3] + 4, gv1 = S - 3;
                f.rect(0xFF64984C, gu0, gv0, gu1, gv1);
                f.frame(0xFF3E6E34, 1.5f, gu0, gv0, gu1, gv1);
                float gu = (gu0 + gu1) / 2, gv = (gv0 + gv1) / 2;
                // The stave path: five lines across the garden with notes on them.
                float st = Math.min(2.6f, (gv1 - gv0) / 14);
                float sv0 = gv - 2 * st;
                f.rect(0xFFE8E0CC, gu0, sv0 - 3, gu1, sv0 + 4 * st + 3);
                for (int k = 0; k < 5; k++) f.line(0xFF3A3A3A, 0.5f, gu0, sv0 + k * st, gu1, sv0 + k * st);
                int[] notePos = {3, 1, 4, 2, 0, 3, 5, 2};
                float rad = Math.min(14, (gv1 - gv0) * 0.3f);
                for (int k = 0; k < notePos.length; k++) {
                    float u = gu0 + 6 + k * 7;
                    if (u > gu1 - 4) break;
                    if (Math.abs(u - gu) < rad + 2) continue;
                    float v = sv0 + notePos[k] * st * 0.5f + st;
                    f.oval(0xFF2E5A2C, u - 1.8f, v - 1.3f, u + 1.8f, v + 1.3f);
                    f.line(0xFF2E5A2C, 0.6f, u + 1.6f, v, u + 1.6f, v - 4 * st);
                }
                // The bandstand: an octagon with a striped tented roof.
                f.circle(0x40000000, gu + 2.5f, gv + 2.5f, rad);
                f.ngon(0xFFE8E2D4, gu, gv, rad + 1, 8, Math.PI / 8);
                for (int k = 0; k < 8; k++) {
                    double a0 = Math.PI / 8 + k * Math.PI / 4, a1 = a0 + Math.PI / 4;
                    f.poly(k % 2 == 0 ? 0xFFB83A34 : 0xFFF0EAE0, gu, gv, gu + (float) Math.cos(a0) * rad, gv + (float) Math.sin(a0) * rad,
                            gu + (float) Math.cos(a1) * rad, gv + (float) Math.sin(a1) * rad);
                }
                f.poly(0x30000000, gu, gv, gu + rad, gv, gu, gv + rad);
                f.circle(0xFFD8B048, gu, gv, 1.2f);
                // Benches, lamps and trees in the garden corners.
                f.tree(gu0 + 6, gv1 - 6, 5, 0xFF467E3A);
                f.tree(gu1 - 6, gv1 - 6, 5, 0xFF467E3A);
                f.bench(gu0 + 6, gv0 + 4, true);
                f.bench(gu1 - 6, gv0 + 4, true);
                f.lamp(gu0 + 2, gv0 + 2);
                f.lamp(gu1 - 2, gv0 + 2);
                // A busker's spot and cycle stands by the street.
                for (float u = gu0 + 4; u < gu1 - 2; u += 3) f.line(0xFF6A6C70, 0.5f, u, S - 2.6f, u, S - 0.6f);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int tc = 0xFFB86A4E;
                f.rect(0xFFE4D8C0, 0, 0, L, S);
                hip(f, tc, 1, 1, L - 1, S - 1, true);
                // Copper cresting on the ridge and skylights over the practice rooms.
                float h = Math.min(L, S) / 2;
                if (L - 2 * h > 4) f.line(0xFF5E9A86, 1.6f, h, S / 2, L - h, S / 2);
                for (float u = h + 3; u < L - h - 2; u += 8) {
                    f.rect(0xFF3A4048, u, 3, u + 3, 6);
                    f.rect(0xFF9EC0CC, u + 0.4f, 3.4f, u + 2.6f, 5.2f);
                }
                if (part == 0 && L > 40) {
                    // The recital hall: a zinc shell rising over the middle of the back wing.
                    float cu = L / 2, w = Math.min(L * 0.18f, 22);
                    Path q = new Path();
                    f.mv(q, cu - w * 0.6f + 3, S + 3);
                    f.qd(q, cu - w * 1.1f + 3, 3, cu + 3, 3);
                    f.qd(q, cu + w * 1.1f + 3, 3, cu + w * 0.6f + 3, S + 3);
                    q.close();
                    f.fill(q, 0x48000000);
                    q = new Path();
                    f.mv(q, cu - w * 0.6f, S);
                    f.qd(q, cu - w * 1.1f, 0, cu, 0);
                    f.qd(q, cu + w * 1.1f, 0, cu + w * 0.6f, S);
                    q.close();
                    f.fill(q, 0xFF98A2A8);
                    for (int k = -4; k <= 4; k++) f.line(0x38000000, 0.5f, cu + k * w * 0.15f, S, cu + k * w * 0.24f, S * 0.12f + Math.abs(k) * 0.6f);
                    f.rect(0xFFE4D8C0, cu - w * 0.6f, S - 2, cu + w * 0.6f, S);
                    f.sign(city, "CONSERVATORY", cu, S / 2, 5, 0xFF2A2A2A);
                }
            }
        };
        d.minS = 10;
        d.minL = 12;
        d.ground = City.PLAZA;
        d.base = City.OFFICE;
        d.inside = Variants.I_CLASSROOM;
        d.roofCol = 0xFFB86A4E;
        d.districts = Variants.UNI | Variants.MID | Variants.OLD;
        out.add(d);
    }

    // ================================================================== 11 Hall of Records

    static void records(List<Landmarks.Def> out) {
        // A monumental archive: a limestone block with a copper mansard edge, two glazed light courts and a coffered
        // saucer dome over the reading rotunda; a reflecting pool between planters in front, a loading dock behind.
        Landmarks.Def d = new Landmarks.Def("Hall of Records", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, 2, 2, L - 4, S - 5, 4)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] h = bx(f, pl(this, x0, y0, x1, y1)[0]);
                f.rect(0xFFBEB8AC, 0, 0, L, S);
                for (float u = 0; u < L; u += 16) f.rect(0xFFB0AA9E, u, 0, u + 2, S);
                for (float v = 0; v < S; v += 16) f.rect(0xFFB0AA9E, 0, v, L, v + 2);
                // The service lane and loading dock behind.
                f.rect(0xFF5E6064, h[0] + 8, 0, h[2] - 8, h[1]);
                f.line(0xFFE0C040, 0.7f, h[0] + 8, h[1] - 3, h[2] - 8, h[1] - 3);
                float tu = h[2] - 24;
                f.rect(0x40000000, tu - 9, h[1] - 14, tu + 3, h[1] - 0.5f);
                f.rect(0xFFE8E6E0, tu - 10, h[1] - 15, tu + 2, h[1] - 1.5f);
                f.rect(0xFF2A4A7A, tu - 10, h[1] - 19.5f, tu + 2, h[1] - 15);
                f.rect(0xFF2A3038, tu - 9, h[1] - 19, tu + 1, h[1] - 18);
                // The front: a reflecting pool between rows of planters, then steps.
                float mu = (h[0] + h[2]) / 2, pv0 = h[3] + 5, pv1 = S - 4, pw = (h[2] - h[0]) * 0.28f;
                if (pv1 - pv0 > 6) {
                    f.rect(0xFF8A8478, mu - pw - 1.5f, pv0 - 1.5f, mu + pw + 1.5f, pv1 + 1.5f);
                    f.rect(0xFF3E6A82, mu - pw, pv0, mu + pw, pv1);
                    f.rect(0xFF5888A0, mu - pw, pv0, mu + pw, pv0 + (pv1 - pv0) * 0.3f);
                    f.line(0x50FFFFFF, 0.6f, mu - pw * 0.6f, (pv0 + pv1) / 2, mu - pw * 0.2f, (pv0 + pv1) / 2);
                }
                for (int k = 0; k < 3; k++) f.rect(k % 2 == 0 ? 0xFFCCC6BA : 0xFFC0BAAE, h[0] + 6, h[3] + k * 1.6f, h[2] - 6, h[3] + (k + 1) * 1.6f);
                for (int s = -1; s <= 1; s += 2) {
                    for (float v = pv0 + 3; v < pv1; v += 18) {
                        float u = mu + s * (pw + 10);
                        f.rect(0x40000000, u - 3, v - 3, u + 5, v + 5);
                        f.rect(0xFF9A948A, u - 4, v - 4, u + 4, v + 4);
                        f.rect(0xFF5A4A3A, u - 3, v - 3, u + 3, v + 3);
                        f.tree(u, v, 4, 0xFF4A7E40);
                    }
                }
                // Planter rows down the sides as well.
                for (float v = 12; v < S - 6; v += 26) {
                    if (h[0] > 20) f.tree(h[0] / 2, v, 4.5f, 0xFF4A7E40);
                    if (L - h[2] > 20) f.tree((h[2] + L) / 2, v, 4.5f, 0xFF4A7E40);
                }
                for (float u = 3; u < L - 2; u += 5) f.circle(0xFF5A5C60, u, S - 1.5f, 1);
                flag(f, mu - pw - 4, S - 9, 0xFF2A3A8A, 0xFFF0F0F0, 0xFF2A3A8A);
                flag(f, mu + pw - 8, S - 9, 0xFF2A6A3A, 0xFFF0E0A0, 0);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, cu = L / 2, cv = S / 2;
                f.rect(0xFFD6CEBC, 0, 0, L, S);
                f.rect(0xFF5E9480, 2, 2, L - 2, S - 2);
                f.rect(0xFF74AA94, 2, 2, L - 2, 4);
                f.rect(0xFF4A7C6A, 2, S - 4, L - 2, S - 2);
                f.rect(0xFFC8C0AC, 5, 5, L - 5, S - 5);
                for (float u = 8; u < L - 6; u += 6) f.line(0x16000000, 0.5f, u, 5, u, S - 5);
                // Two glazed light courts.
                float R = Math.min(S * 0.26f, L * 0.14f);
                for (int s = -1; s <= 1; s += 2) {
                    float qu = cu + s * (L * 0.28f), qw = Math.min(L * 0.1f, 18), qh = S * 0.24f;
                    f.rect(0xFF7A746A, qu - qw - 1.5f, cv - qh - 1.5f, qu + qw + 1.5f, cv + qh + 1.5f);
                    f.rect(0xFF3A4248, qu - qw, cv - qh, qu + qw, cv + qh);
                    f.rect(0xFFA8C4CE, qu - qw + 0.6f, cv - qh + 0.6f, qu + qw - 0.6f, cv);
                    f.rect(0xFF86A4B0, qu - qw + 0.6f, cv, qu + qw - 0.6f, cv + qh - 0.6f);
                    for (float u = qu - qw + 4; u < qu + qw; u += 4) f.line(0xFF3A4248, 0.5f, u, cv - qh, u, cv + qh);
                    f.line(0xFF3A4248, 0.6f, qu - qw, cv, qu + qw, cv);
                }
                // Vents along the edges.
                for (float u = 10; u < L - 8; u += 12) {
                    if (Math.abs(u - cu) < R + 4) continue;
                    f.rect(0x40000000, u + 1, 7, u + 5, 10);
                    f.rect(0xFF9A9A98, u, 6, u + 3.5f, 9);
                }
                // The coffered saucer dome over the rotunda, an oculus at the top.
                f.circle(0x50000000, cu + 3, cv + 3, R + 1.5f);
                f.circle(0xFFB8B0A0, cu, cv, R + 1.5f);
                for (int k = 4; k >= 1; k--) {
                    float rr = R * k / 4.2f;
                    f.circle(Landmarks.shade(0xFFD8D0C0, 0.9f + k * 0.03f), cu - (4 - k) * 0.4f, cv - (4 - k) * 0.4f, rr);
                    f.ring(0x24000000, 0.5f, cu - (4 - k) * 0.4f, cv - (4 - k) * 0.4f, rr);
                }
                for (int k = 0; k < 16; k++) {
                    double a = k * Math.PI / 8;
                    f.line(0x20000000, 0.5f, cu + (float) Math.cos(a) * R * 0.3f, cv + (float) Math.sin(a) * R * 0.3f,
                            cu + (float) Math.cos(a) * R, cv + (float) Math.sin(a) * R);
                }
                f.circle(0xFF5A6A74, cu - 1.6f, cv - 1.6f, R * 0.16f);
                f.circle(0xFF9ABCCC, cu - 1.9f, cv - 1.9f, R * 0.1f);
                f.sign(city, "HALL OF RECORDS", cu, S - 3, 5, 0xFF2A2A2A);
            }
        };
        d.minS = 10;
        d.minL = 12;
        d.ground = City.PLAZA;
        d.base = City.OFFICE;
        d.inside = Variants.I_LIBRARY;
        d.roofCol = 0xFF5E9480;
        d.districts = Variants.DOWN | Variants.MID | Variants.UNI;
        out.add(d);
    }

    // ================================================================== 12 Star Fort

    /** The points of a star with n tips round (u, v): tips at radii (ru, rv), notches at ri of that, times sc. */
    static float[] star(float u, float v, float ru, float rv, int n, float ri, float sc) {
        float[] q = new float[n * 4];
        for (int k = 0; k < 2 * n; k++) {
            double a = -Math.PI / 2 + k * Math.PI / n;
            float rr = (k % 2 == 0 ? 1 : ri) * sc;
            q[2 * k] = u + (float) Math.cos(a) * ru * rr;
            q[2 * k + 1] = v + (float) Math.sin(a) * rv * rr;
        }
        return q;
    }

    static void starFort(List<Landmarks.Def> out) {
        // Star-shaped earthworks: a water-filled moat with ravelins, grassed ramparts with cannon on each bastion,
        // a sandy parade ground with an earth-covered powder magazine, and a timber bridge to the gate.
        Landmarks.Def d = new Landmarks.Def("Star Fort", T_MONUMENT) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, L / 2 - 2, S / 2 - 1, 4, 2, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, cu = L / 2, cv = S / 2 + S * 0.02f;
                f.rect(0xFF6E9450, 0, 0, L, S);
                for (float u = 0; u < L; u += 8) f.rect(0x0C000000, u, 0, u + 4, S);
                float rv = S * 0.47f, ru = Math.min(L * 0.47f, rv * 1.35f);
                int n = 5;
                float ri = 0.62f;
                // The moat.
                f.poly(0xFF4E6A3E, star(cu, cv, ru, rv, n, ri, 1.16f));
                f.poly(0xFF3E7896, star(cu, cv, ru, rv, n, ri, 1.12f));
                f.poly(0xFF4E88A6, star(cu, cv, ru, rv, n, ri, 1.04f));
                // Ravelins in the moat, before each curtain (not the gate's).
                for (int k = 0; k < n; k++) {
                    double a = -Math.PI / 2 + (2 * k + 1) * Math.PI / n;
                    if (Math.abs(a - Math.PI / 2) < 0.1) continue;
                    float tu = cu + (float) Math.cos(a) * ru * 0.98f, tv = cv + (float) Math.sin(a) * rv * 0.98f;
                    float bu0 = cu + (float) Math.cos(a - 0.3) * ru * 0.66f, bv0 = cv + (float) Math.sin(a - 0.3) * rv * 0.66f;
                    float bu1 = cu + (float) Math.cos(a + 0.3) * ru * 0.66f, bv1 = cv + (float) Math.sin(a + 0.3) * rv * 0.66f;
                    f.poly(0xFF4E7A3E, tu, tv, bu0, bv0, bu1, bv1);
                    f.poly(0xFF6E9A54, cu + (tu - cu) * 0.94f, cv + (tv - cv) * 0.94f, bu0, bv0, bu1, bv1);
                }
                // The ramparts: outer slope, the broad top, the inner slope, the parade ground.
                f.poly(0xFF4A743A, star(cu, cv, ru, rv, n, ri, 1));
                f.poly(0xFF78A45C, star(cu, cv, ru, rv, n, ri, 0.92f));
                f.poly(0xFF6A9450, star(cu, cv, ru, rv, n, ri, 0.82f));
                f.poly(0xFF5E8648, star(cu, cv, ru, rv, n, ri, 0.74f));
                float[] in = new float[n * 2];
                for (int k = 0; k < n; k++) {
                    double a = -Math.PI / 2 + (2 * k + 1) * Math.PI / n;
                    in[2 * k] = cu + (float) Math.cos(a) * ru * ri * 0.74f;
                    in[2 * k + 1] = cv + (float) Math.sin(a) * rv * ri * 0.74f;
                }
                f.poly(0xFFC8B48A, in);
                // A parapet line on the outer crest.
                float[] cr = star(cu, cv, ru, rv, n, ri, 0.96f);
                for (int k = 0; k < cr.length; k += 2) {
                    int j = (k + 2) % cr.length;
                    f.line(0xFF8E8A7A, 1.2f, cr[k], cr[k + 1], cr[j], cr[j + 1]);
                }
                // Cannon on each bastion tip, pointing out.
                for (int k = 0; k < n; k++) {
                    double a = -Math.PI / 2 + 2 * k * Math.PI / n;
                    float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
                    float u = cu + ca * ru * 0.8f, v = cv + sa * rv * 0.8f;
                    f.circle(0x40000000, u + 1, v + 1, 2.4f);
                    f.rect(0xFF6A4A2A, u - 1.8f, v - 1.8f, u + 1.8f, v + 1.8f);
                    f.rline(0xFF2A2C2E, 1.6f, u, v, u + ca * 5, v + sa * 5);
                    f.circle(0xFF1A1A1A, u + ca * 5, v + sa * 5, 0.5f);
                    f.circle(0xFF2A2C2E, u - ca * 3, v - sa * 3, 0.8f);
                    f.circle(0xFF2A2C2E, u - ca * 3 + 1.4f, v - sa * 3, 0.8f);
                }
                // The gate on the near curtain and the bridge out over the moat.
                float gv = cv + rv * ri * 0.74f;
                f.rect(0xFF6E5034, cu - 3.5f, gv - 2, cu + 3.5f, S);
                for (float v = gv; v < S; v += 2) f.line(0x40000000, 0.4f, cu - 3.5f, v, cu + 3.5f, v);
                f.line(0xFF4A3420, 0.7f, cu - 3.5f, gv, cu - 3.5f, S);
                f.line(0xFF4A3420, 0.7f, cu + 3.5f, gv, cu + 3.5f, S);
                f.rect(0xFF8E8678, cu - 6, gv - 3, cu - 3.5f, gv + 4);
                f.rect(0xFF8E8678, cu + 3.5f, gv - 3, cu + 6, gv + 4);
                // A path across the parade, a flagstaff.
                f.rect(0xFFB8A27A, cu - 2.5f, cv, cu + 2.5f, gv);
                flag(f, cu + 14, cv - 6, 0xFF2A3A8A, 0xFFF0F0F0, 0xFFB02020);
                // Trees on the glacis in the corners.
                f.tree(8, 8, 6, 0xFF3E6E36);
                f.tree(L - 8, 8, 6, 0xFF3E6E36);
                f.tree(8, S - 8, 5, 0xFF3E6E36);
                f.tree(L - 8, S - 8, 5, 0xFF3E6E36);
                if (L - 2 * ru > 40) {
                    f.tree(14, S / 2, 6, 0xFF4A7E3C);
                    f.tree(L - 14, S / 2, 6, 0xFF4A7E3C);
                }
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                // The powder magazine: a turfed barrel vault between stone gable walls.
                f.rect(0xFF8E8678, 0, 0, L, S);
                f.rect(0xFF8ABA64, 3, 0.8f, L - 3, S / 2);
                f.rect(0xFF5E8A44, 3, S / 2, L - 3, S - 0.8f);
                f.rect(0xFF7AAA58, 3, S * 0.3f, L - 3, S / 2);
                f.rect(0xFFA8A090, 0, 0, 3, S);
                f.rect(0xFF7E7668, L - 3, 0, L, S);
                for (float u = 8; u < L - 6; u += 10) {
                    f.circle(0xFF5A5A5A, u, S / 2, 1.2f);
                    f.circle(0xFF2A2A2A, u, S / 2, 0.6f);
                }
                f.sign(city, "STAR FORT", L / 2, S + 6, 5, 0xFF2A3A2A);
            }
        };
        d.minS = 10;
        d.minL = 10;
        d.ground = City.GRASS;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_RACKS;
        d.roofCol = 0xFF6E9A54;
        d.districts = Variants.PARK | Variants.SUB | Variants.OLD;
        out.add(d);
    }

    // ================================================================== 13 Castle Keep

    static void castle(List<Landmarks.Def> out) {
        // A square stone keep with corner turrets and twin lead roofs inside its battlements; a curtain wall with round
        // corner towers, a gatehouse of two towers over a moat bridge, and a bailey with a well, stables and hay.
        Landmarks.Def d = new Landmarks.Def("Castle Keep", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int k = Math.min(S - 6, 6);
                return new int[][]{at(al, L - 2 - k, (S - k) / 2, k, k, 5), at(al, 1, S / 2 - 3, 2, 2, 3), at(al, 1, S / 2 + 1, 2, 2, 3)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] q = pl(this, x0, y0, x1, y1);
                float[] k = bx(f, q[0]);
                float gv = S / 2 - TT / 2 * 0 - 0, wc = 2 * TT, wt = 9;
                gv = Math.round(S / TT) / 2 * TT;
                // The moat and banks outside the wall.
                f.rect(0xFF5E8A48, 0, 0, L, S);
                float m0 = 3, m1 = wc - wt / 2 - 2;
                f.frame(0xFF3E6E88, m1 - m0, (m0 + m1) / 2, (m0 + m1) / 2, L - (m0 + m1) / 2, S - (m0 + m1) / 2);
                // The bailey.
                float b0 = wc + wt / 2;
                f.rect(0xFF8E9464, b0, b0, L - b0, S - b0);
                for (int j = 0; j < 30; j++) {
                    float u = b0 + r.nextFloat() * (L - 2 * b0), v = b0 + r.nextFloat() * (S - 2 * b0);
                    f.circle(j % 2 == 0 ? 0xFF9C9A6A : 0xFF7E8A58, u, v, 2 + r.nextFloat() * 3);
                }
                // A beaten track from the gate to the keep door.
                f.rect(0xFFA8946A, b0 - 2, gv - 6, k[0], gv + 6);
                // The curtain wall: a thick band with a wall walk and merlons on the outer edge.
                float a0 = wc - wt / 2, a1 = wc + wt / 2;
                f.rect(0x40000000, a0 + 3, a0 + 3, L - a0 + 3, S - a0 + 3);
                f.frame(0xFF8A8478, wt, wc, wc, L - wc, S - wc);
                f.frame(0xFFB2AC9E, wt - 4, wc + 0.5f, wc + 0.5f, L - wc - 0.5f, S - wc - 0.5f);
                for (float u = a0; u < L - a0; u += 4) {
                    f.rect(0xFF9A9486, u, a0, u + 2, a0 + 1.6f);
                    f.rect(0xFF7A7468, u, S - a0 - 1.6f, u + 2, S - a0);
                }
                for (float v = a0; v < S - a0; v += 4) {
                    f.rect(0xFF9A9486, a0, v, a0 + 1.6f, v + 2);
                    f.rect(0xFF7A7468, L - a0 - 1.6f, v, L - a0, v + 2);
                }
                // The gate passage and the bridge over the moat.
                f.rect(0xFF6E6658, a0 - 1, gv - 6, a1 + 1, gv + 6);
                f.rect(0xFF6E5034, 0, gv - 5, a0, gv + 5);
                for (float u = 1; u < a0; u += 2) f.line(0x40000000, 0.4f, u, gv - 5, u, gv + 5);
                f.line(0xFF2A2A2A, 0.8f, a1, gv - 6, a1, gv + 6);
                // Round towers on the corners.
                float tr = 11;
                for (int j = 0; j < 4; j++) {
                    float u = j % 2 == 0 ? wc : L - wc, v = j < 2 ? wc : S - wc;
                    f.circle(0x48000000, u + 3, v + 3, tr);
                    f.circle(0xFF8A8478, u, v, tr);
                    f.circle(0xFFA8A294, u - 0.5f, v - 0.5f, tr - 2);
                    for (int t = 0; t < 12; t++) {
                        double a = t * Math.PI / 6;
                        f.circle(0xFF7A7468, u + (float) Math.cos(a) * (tr - 1), v + (float) Math.sin(a) * (tr - 1), 1.2f);
                    }
                    f.circle(0xFF6E6A60, u, v, 2);
                }
                // Lean-to stables along the inside of the far wall, with hay.
                float su0 = b0 + 2, su1 = k[0] - 6;
                if (su1 - su0 > 16) {
                    f.rect(0x40000000, su0 + 2, b0 + 2, su1 + 2, b0 + 13);
                    f.rect(0xFF8A6A40, su0, b0, su1, b0 + 11);
                    f.rect(0xFFA8844E, su0, b0, su1, b0 + 4);
                    for (float u = su0 + 2; u < su1; u += 2.5f) f.line(0x24000000, 0.4f, u, b0, u, b0 + 11);
                    for (int j = 0; j < 3; j++) {
                        float u = su0 + 5 + j * 6, v = S - b0 - 7;
                        f.circle(0x40000000, u + 1, v + 1, 2.8f);
                        f.circle(0xFFD8B860, u, v, 2.8f);
                        f.ring(0xFFB89838, 0.5f, u, v, 1.6f);
                    }
                }
                // The well.
                float wu = (b0 + k[0]) / 2 + 4, wv = S - b0 - 14;
                if (wv > gv + 10) {
                    f.circle(0x40000000, wu + 1.5f, wv + 1.5f, 4.5f);
                    f.circle(0xFF8A8478, wu, wv, 4.5f);
                    f.circle(0xFF1E2A30, wu, wv, 2.8f);
                    f.line(0xFF5A4030, 1, wu - 5, wv, wu + 5, wv);
                }
                // Banners at the gate.
                f.rect(0xFFA02828, a1 + 1, gv - 9, a1 + 3, gv - 6);
                f.rect(0xFFA02828, a1 + 1, gv + 6, a1 + 3, gv + 9);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                if (part > 0) {
                    // A gatehouse tower.
                    f.rect(0xFF7E786C, 0, 0, L, S);
                    f.rect(0xFFA8A294, 2, 2, L - 2, S - 2);
                    crenels(f, 0xFF8E887A, 0, 0, L, S, 2);
                    f.rect(0xFF5A4A36, L / 2 - 2, S / 2 - 2, L / 2 + 2, S / 2 + 2);
                    f.line(0xFF3A2A1E, 0.4f, L / 2 - 2, S / 2, L / 2 + 2, S / 2);
                    return;
                }
                // The keep: a walk inside the battlements round twin lead roofs, and four corner turrets.
                f.rect(0xFF7E786C, 0, 0, L, S);
                f.rect(0xFFAAA496, 2, 2, L - 2, S - 2);
                crenels(f, 0xFF928C7E, 0, 0, L, S, 2.2f);
                float in = 5;
                float mv = S / 2;
                gable(f, 0xFF6E747A, in, in, L - in, mv);
                gable(f, 0xFF6E747A, in, mv, L - in, S - in);
                float ts = Math.min(L, S) * 0.17f;
                for (int j = 0; j < 4; j++) {
                    float u = j % 2 == 0 ? 0 : L - 2 * ts, v = j < 2 ? 0 : S - 2 * ts;
                    f.rect(0x40000000, u + 2, v + 2, u + 2 * ts + 2, v + 2 * ts + 2);
                    f.rect(0xFF8A8478, u, v, u + 2 * ts, v + 2 * ts);
                    f.rect(0xFFB4AEA0, u + 1.5f, v + 1.5f, u + 2 * ts - 1.5f, v + 2 * ts - 1.5f);
                    crenels(f, 0xFF9A9486, u, v, u + 2 * ts, v + 2 * ts, 1.6f);
                }
                // A banner flying from the north-west turret.
                f.circle(0xFF3A3A3A, ts, ts, 0.9f);
                f.poly(0xFFB02828, ts, ts - 0.6f, ts + 9, ts + 1, ts, ts + 3);
                f.poly(0xFFE8C040, ts + 2, ts + 0.4f, ts + 5, ts + 1.2f, ts + 2, ts + 2);
                f.sign(city, "CASTLE", L / 2, S / 2, 5, 0xFF2A2A2A);
            }
        };
        d.minS = 10;
        d.minL = 12;
        d.ground = City.GRASS;
        d.base = City.OFFICE;
        d.inside = Variants.I_HOTEL;
        d.roofCol = 0xFF8A8478;
        d.districts = Variants.OLD | Variants.PARK | Variants.SUB;
        out.add(d);
    }

    // ================================================================== 14 Ancient Ruins

    static void ruins(List<Landmarks.Def> out) {
        // A temple's stepped platform with its colonnade half standing, the cella walls broken down, fallen drums;
        // beside it excavation trenches on a string grid, a part-uncovered mosaic, spoil heaps and the diggers' hut.
        Landmarks.Def d = new Landmarks.Def("Ancient Ruins", T_MONUMENT) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, 1, S - 3, 3, 2, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFFC8B68E, 0, 0, L, S);
                for (int k = 0; k < 40; k++) {
                    float u = r.nextFloat() * L, v = r.nextFloat() * S;
                    f.circle(k % 3 == 0 ? 0xFFA8A070 : k % 3 == 1 ? 0xFFD4C49C : 0xFFB8A880, u, v, 2 + r.nextFloat() * 4);
                }
                // The temple: a three-stepped stylobate.
                float tu0 = L * 0.4f, tu1 = L - 8, tv0 = 8, tv1 = S - 10;
                if (tu1 - tu0 > (tv1 - tv0) * 2.2f) tu0 = tu1 - (tv1 - tv0) * 2.2f;
                for (int k = 0; k < 3; k++) {
                    float e = (2 - k) * 2;
                    f.rect(0x30000000, tu0 - e + 1.5f, tv0 - e + 1.5f, tu1 + e + 1.5f, tv1 + e + 1.5f);
                    f.rect(Landmarks.shade(0xFFD8CCB0, 0.9f + k * 0.05f), tu0 - e, tv0 - e, tu1 + e, tv1 + e);
                }
                // Cracks and missing slabs.
                for (int k = 0; k < 6; k++) {
                    float u = tu0 + r.nextFloat() * (tu1 - tu0), v = tv0 + r.nextFloat() * (tv1 - tv0);
                    f.line(0x40000000, 0.5f, u, v, u + 3 - r.nextFloat() * 6, v + 3 - r.nextFloat() * 6);
                }
                f.rect(0xFFB8A880, tu1 - 10, tv1 - 5, tu1 - 2, tv1);
                // Scattered blocks in the cella and the base of the cult statue.
                for (int k = 0; k < 7; k++) {
                    float u = tu0 + (tu1 - tu0) * (0.3f + r.nextFloat() * 0.5f), v = tv0 + (tv1 - tv0) * (0.32f + r.nextFloat() * 0.36f);
                    f.rect(0x40000000, u + 1, v + 1, u + 5, v + 4);
                    f.rect(0xFFDCD2BA, u, v, u + 4, v + 3);
                }
                // The cella walls, broken down to stumps in places.
                float cu0 = tu0 + (tu1 - tu0) * 0.22f, cu1 = tu1 - (tu1 - tu0) * 0.12f, cv0 = tv0 + (tv1 - tv0) * 0.27f, cv1 = tv1 - (tv1 - tv0) * 0.27f;
                f.rect(0xFFC4B898, cu0, cv0, cu1, cv1);
                int wc = 0xFFE4DAC2;
                f.rect(0x40000000, cu0 + 1, cv0 + 1, cu1 + 1, cv0 + 3.5f);
                f.rect(wc, cu0, cv0, cu0 + (cu1 - cu0) * 0.6f, cv0 + 2.5f);
                f.rect(wc, cu0 + (cu1 - cu0) * 0.75f, cv0, cu1, cv0 + 2.5f);
                f.rect(wc, cu0, cv1 - 2.5f, cu0 + (cu1 - cu0) * 0.3f, cv1);
                f.rect(wc, cu0 + (cu1 - cu0) * 0.5f, cv1 - 2.5f, cu1, cv1);
                f.rect(wc, cu1 - 2.5f, cv0, cu1, cv1);
                f.rect(wc, cu0, cv0, cu0 + 2.5f, cv0 + (cv1 - cv0) * 0.3f);
                f.rect(wc, cu0, cv1 - (cv1 - cv0) * 0.25f, cu0 + 2.5f, cv1);
                // The peristyle: standing columns, broken stumps and empty bases.
                int nu = Math.max(6, Math.round((tu1 - tu0) / 9)), nv = Math.max(4, Math.round((tv1 - tv0) / 9));
                for (int i = 0; i < nu; i++)
                    for (int j = 0; j < nv; j++) {
                        if (i != 0 && i != nu - 1 && j != 0 && j != nv - 1) continue;
                        float u = tu0 + 3 + i * (tu1 - tu0 - 6) / (nu - 1), v = tv0 + 3 + j * (tv1 - tv0 - 6) / (nv - 1);
                        int st = r.nextInt(7) % 5;
                        f.rect(0xFFB8AC90, u - 2.8f, v - 2.8f, u + 2.8f, v + 2.8f);
                        if (st == 0) continue;
                        float hgt = st >= 3 ? 9 : 2.5f;
                        f.rline(0x38000000, 4.4f, u, v, u + hgt, v + hgt);
                        f.circle(0xFFC4B898, u, v, 2.4f);
                        f.circle(st >= 3 ? 0xFFF0E8D6 : 0xFFE0D6BE, u - 0.3f, v - 0.3f, 2.1f);
                        if (st >= 3) {
                            f.rect(0xFFF4EEE0, u - 2.4f, v - 2.4f, u + 2.4f, v + 2.4f);
                            f.circle(0xFFD8CCB0, u + 2.4f, v - 2.4f, 0.9f);
                            f.circle(0xFFD8CCB0, u - 2.4f, v + 2.4f, 0.9f);
                        }
                    }
                // A fallen column lying in drums down the slope, and a capital.
                float fu = tu0 - 6, fv = tv1 + 4;
                for (int k = 0; k < 4; k++) {
                    float u = fu - k * 5.2f, v = fv + k * 0.8f;
                    if (u < 4) break;
                    f.roundRect(0x40000000, u - 2.2f + 1, v - 2.4f + 1, u + 2.2f + 1, v + 2.4f + 1, 1);
                    f.roundRect(0xFFE0D6BE, u - 2.2f, v - 2.4f, u + 2.2f, v + 2.4f, 1);
                    f.line(0x30000000, 0.4f, u - 2.2f, v - 0.8f, u + 2.2f, v - 0.8f);
                    f.line(0x30000000, 0.4f, u - 2.2f, v + 0.8f, u + 2.2f, v + 0.8f);
                }
                f.rect(0x40000000, tu1 - 3, tv1 + 6, tu1 + 4, tv1 + 13);
                f.rect(0xFFE8DEC8, tu1 - 4, tv1 + 5, tu1 + 3, tv1 + 12);
                f.circle(0xFFC8BCA0, tu1 - 4, tv1 + 8.5f, 1.5f);
                f.circle(0xFFC8BCA0, tu1 + 3, tv1 + 8.5f, 1.5f);
                // Excavation trenches on a string grid, one with a mosaic floor.
                float eu1 = tu0 - 10;
                for (int k = 0; k < 2; k++) {
                    float a0 = 6, a1 = eu1, b0 = k == 0 ? 6 : S * 0.42f, b1 = k == 0 ? S * 0.36f : S - 3.5f * TT;
                    if (a1 - a0 < 14 || b1 - b0 < 10) continue;
                    f.rect(0xFFA8946C, a0 - 2, b0 - 2, a1 + 2, b1 + 2);
                    f.rect(0xFF7A6242, a0, b0, a1, b1);
                    f.rect(0xFF5E4A32, a0, b0, a1, b0 + 2);
                    f.rect(0xFF8A7250, a0 + 3, b0 + 3, a1 - 3, b1 - 3);
                    if (k == 1) {
                        float mu0 = a0 + 4, mv0 = b0 + 4, mu1 = a1 - 4, mv1 = b1 - 4;
                        mu1 = Math.min(mu1, mu0 + 40);
                        mv1 = Math.min(mv1, mv0 + 40);
                        f.rect(0xFFD8CCB0, mu0, mv0, mu1, mv1);
                        for (float u = mu0 + 1; u < mu1 - 4; u += 4.5f)
                            for (float v = mv0 + 1; v < mv1 - 4; v += 4.5f) {
                                if ((u - mu0) + (v - mv0) > (mu1 - mu0) * 1.1f) continue;
                                int ix = (int) ((u - mu0) / 4.5f), iy = (int) ((v - mv0) / 4.5f);
                                int col = (ix == iy || ix + iy == 6) ? 0xFF8A3A2A : (ix + iy) % 2 == 0 ? 0xFFE8DCC0 : 0xFF3A5A6A;
                                f.rect(col, u, v, u + 3.8f, v + 3.8f);
                            }
                        f.poly(0xFF8A7250, mu1, mv0 + (mu1 - mu0) * 0.3f, mu1, mv1, mu0 + (mu1 - mu0) * 0.3f, mv1);
                    }
                    for (float u = a0 + 8; u < a1; u += 8) f.line(0x90FFFFFF, 0.3f, u, b0, u, b1);
                    for (float v = b0 + 8; v < b1; v += 8) f.line(0x90FFFFFF, 0.3f, a0, v, a1, v);
                    f.circle(0xFFE0D8C0, a0 - 2, b0 - 2, 0.8f);
                    f.circle(0xFFE0D8C0, a1 + 2, b0 - 2, 0.8f);
                    f.circle(0xFFE0D8C0, a0 - 2, b1 + 2, 0.8f);
                    f.circle(0xFFE0D8C0, a1 + 2, b1 + 2, 0.8f);
                    if (k == 0) {
                        f.line(0xFFB89A60, 0.6f, a1 - 6, b0 + 2, a1 - 6, b1 - 2);
                        f.line(0xFFB89A60, 0.6f, a1 - 3, b0 + 2, a1 - 3, b1 - 2);
                        for (float v = b0 + 3; v < b1 - 2; v += 2) f.line(0xFFB89A60, 0.5f, a1 - 6, v, a1 - 3, v);
                    }
                }
                // Spoil heaps, a blue tarp, a wheelbarrow and a sieve.
                float hu = eu1 + 2, hv = S * 0.38f;
                f.oval(0x40000000, hu - 4, hv - 3, hu + 9, hv + 7);
                f.oval(0xFF9A8058, hu - 5, hv - 4, hu + 7, hv + 5);
                f.oval(0xFFAE9468, hu - 3, hv - 3, hu + 3, hv + 1);
                f.rect(0xFF3A6AA8, 3.5f * TT, S - 20, 3.5f * TT + 12, S - 13);
                f.line(0x40FFFFFF, 0.5f, 3.5f * TT, S - 17, 3.5f * TT + 12, S - 16);
                f.rect(0xFF3A6A3A, hu + 2, S - 16, hu + 8, S - 12);
                f.circle(0xFF1E1E1E, hu + 9, S - 14, 1);
                f.ring(0xFF6E5034, 0.8f, hu - 4, S - 14, 3);
                f.circle(0x806A6A6A, hu - 4, S - 14, 2.4f);
                // A few olive trees and a rope barrier round the temple.
                f.tree(L - 5, S - 5, 5, 0xFF7A8E5A);
                f.tree(tu0 - 4, 5, 4.5f, 0xFF7A8E5A);
                for (float u = tu0 - 6; u < tu1 + 6; u += 7) f.circle(0xFF5A4030, u, tv1 + 18 > S - 2 ? S - 2 : tv1 + 18, 0.8f);
                f.line(0xFFC8A060, 0.4f, tu0 - 6, Math.min(S - 2, tv1 + 18), tu1 + 6, Math.min(S - 2, tv1 + 18));
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                // The site hut: a white cabin with a ribbed roof.
                f.rect(0xFFB8B8B4, 0, 0, L, S);
                f.rect(0xFFE6E6E0, 1, 1, L - 1, S - 1);
                for (float u = 3; u < L - 1; u += 2.5f) f.line(0x24000000, 0.5f, u, 1, u, S - 1);
                f.rect(0xFF7A7A78, L * 0.7f, S * 0.3f, L * 0.7f + 4, S * 0.3f + 3);
                f.sign(city, "EXCAVATION", L / 2, S / 2, 4, 0xFF5A3A1A);
            }
        };
        d.minS = 10;
        d.minL = 12;
        d.ground = City.DIRT;
        d.base = City.KIOSK;
        d.inside = Variants.I_OFFICE;
        d.roofCol = 0xFFE6E6E0;
        d.districts = Variants.PARK | Variants.OLD | Variants.UNI;
        out.add(d);
    }

    // ================================================================== 15 Triumphal Arch

    /** A horse from above, facing +v (toward the viewer's lower edge in the frame), body length l. */
    static void horse(F f, float u, float v, float l, int col) {
        float w = l * 0.32f;
        f.oval(0x40000000, u - w / 2 + 1.2f, v - l / 2 + 1.2f, u + w / 2 + 1.2f, v + l / 2 + 1.2f);
        f.rline(City.darken(col, 0.85f), l * 0.08f, u, v - l / 2, u + l * 0.05f, v - l * 0.72f);
        f.oval(col, u - w / 2, v - l / 2, u + w / 2, v + l / 2);
        f.oval(col, u - w * 0.26f, v + l * 0.35f, u + w * 0.26f, v + l * 0.72f);
        f.oval(City.darken(col, 0.85f), u - w * 0.2f, v + l * 0.6f, u + w * 0.2f, v + l * 0.9f);
        f.oval(City.lighten(col, 0.2f), u - w * 0.3f, v - l * 0.3f, u, v + l * 0.2f);
    }

    static void arch(List<Landmarks.Def> out) {
        // A triumphal arch on a vast round plaza: radiating paving, rings of granite, avenues of clipped trees,
        // an eternal flame before it and a bronze quadriga driven by Victory on its attic.
        Landmarks.Def d = new Landmarks.Def("Triumphal Arch", T_MONUMENT) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int lu = Math.min(6, L - 6), lv = S >= 12 ? 3 : 2;
                return new int[][]{at(al, (L - lu) / 2, (S - lv) / 2, lu, lv, 5)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] h = bx(f, pl(this, x0, y0, x1, y1)[0]);
                float cu = (h[0] + h[2]) / 2, cv = (h[1] + h[3]) / 2, R = (float) Math.sqrt(L * L + S * S);
                f.rect(0xFFD8D0C0, 0, 0, L, S);
                int n = 32;
                for (int k = 0; k < n; k += 2) {
                    double a0 = k * 2 * Math.PI / n, a1 = (k + 1) * 2 * Math.PI / n;
                    f.poly(0xFFC6BCA8, cu, cv, cu + (float) Math.cos(a0) * R, cv + (float) Math.sin(a0) * R,
                            cu + (float) Math.cos(a1) * R, cv + (float) Math.sin(a1) * R);
                }
                float m = Math.min(L, S);
                // Rings of dark granite.
                for (int k = 1; k <= 4; k++) f.ring(0xFF8E887C, k == 2 ? 3 : 1, cu, cv, m * 0.12f * k + 6);
                f.ring(0xFF7A7468, 0.6f, cu, cv, m * 0.12f * 2 + 8);
                // Eight avenues out from the arch, edged with clipped trees.
                for (int k = 0; k < 8; k++) {
                    double a = k * Math.PI / 4;
                    float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
                    f.line(0xFFB4AA96, 6, cu + ca * m * 0.3f, cv + sa * m * 0.3f, cu + ca * R, cv + sa * R);
                    if (k % 2 == 1) {
                        for (float t = m * 0.36f; t < R; t += 18) {
                            float u = cu + ca * t, v = cv + sa * t;
                            if (u < 5 || v < 5 || u > L - 5 || v > S - 5) continue;
                            f.tree(u - sa * 7, v + ca * 7, 3.8f, 0xFF4A7E3C);
                            f.tree(u + sa * 7, v - ca * 7, 3.8f, 0xFF4A7E3C);
                        }
                    } else {
                        float u = cu + ca * m * 0.44f, v = cv + sa * m * 0.44f;
                        if (u > 3 && v > 3 && u < L - 3 && v < S - 3) {
                            f.lamp(u - sa * 5, v + ca * 5);
                            f.lamp(u + sa * 5, v - ca * 5);
                        }
                    }
                }
                // The eternal flame on its bronze disc, before the arch.
                float fv = h[3] + 7;
                f.circle(0xFF7A6A48, cu, fv, 3.4f);
                f.circle(0xFFA88A48, cu, fv, 2.6f);
                f.circle(0xFFE87A20, cu, fv, 1.6f);
                f.circle(0xFFF8D050, cu - 0.3f, fv - 0.3f, 0.8f);
                // Wreaths and two great flagpoles.
                f.circle(0xFF3E6E36, cu - 6, fv + 1, 1.6f);
                f.circle(0xFF3E6E36, cu + 6, fv + 1, 1.6f);
                flag(f, h[0] - 12, cv + 3, 0xFF203A80, 0xFFF4F0E4, 0xFFB02020);
                flag(f, h[2] + 6, cv + 3, 0xFF203A80, 0xFFF4F0E4, 0xFFB02020);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, cu = L / 2, cv = S / 2;
                // The attic: a cornice, inscription panels on both faces, reliefs on the ends.
                f.rect(0xFFB8AE98, 0, 0, L, S);
                f.rect(0xFFE2D8C4, 1, 1, L - 1, S - 1);
                f.rect(0xFFD2C8B2, 1, S / 2, L - 1, S - 1);
                f.frame(0xFFF0E8D8, 0.6f, 2, 2, L - 2, S - 2);
                for (int s = 0; s < 2; s++) {
                    float v = s == 0 ? 3 : S - 6;
                    f.rect(0xFF8A8270, cu - L * 0.3f, v, cu + L * 0.3f, v + 3);
                    for (float u = cu - L * 0.28f; u < cu + L * 0.28f; u += 2) f.rect(0xFFC8A848, u, v + 0.9f, u + 1.1f, v + 2.1f);
                }
                f.rect(0xFFC8BCA4, 2.5f, 4, 9, S - 4);
                f.rect(0xFFC8BCA4, L - 9, 4, L - 2.5f, S - 4);
                // Trophies on the corners.
                ball(f, 3.5f, 3.5f, 1.8f, 0xFF6E8A6A);
                ball(f, L - 3.5f, 3.5f, 1.8f, 0xFF6E8A6A);
                ball(f, 3.5f, S - 3.5f, 1.8f, 0xFF6E8A6A);
                ball(f, L - 3.5f, S - 3.5f, 1.8f, 0xFF6E8A6A);
                // The quadriga: four bronze horses abreast, the chariot and winged Victory.
                f.rect(0xFFA89E88, cu - 15, cv - 7, cu + 15, cv + 9);
                int bz = 0xFF5E7A62;
                for (int k = 0; k < 4; k++) horse(f, cu - 9 + k * 6, cv + 2.5f, 10, bz);
                f.roundRect(0x40000000, cu - 6 + 1, cv - 7 + 1, cu + 6 + 1, cv - 2 + 1, 1.5f);
                f.roundRect(City.darken(bz, 0.85f), cu - 6, cv - 7, cu + 6, cv - 2, 1.5f);
                f.poly(City.lighten(bz, 0.15f), cu, cv - 5, cu - 7, cv - 8.5f, cu - 4, cv - 3.5f);
                f.poly(City.lighten(bz, 0.15f), cu, cv - 5, cu + 7, cv - 8.5f, cu + 4, cv - 3.5f);
                f.circle(City.lighten(bz, 0.25f), cu, cv - 5, 1.6f);
                f.line(City.lighten(bz, 0.3f), 0.6f, cu, cv - 5, cu + 1, cv + 1);
                f.sign(city, "TRIUMPHAL ARCH", cu, S + 16, 5, 0xFF2A2A2A);
            }
        };
        d.minS = 10;
        d.minL = 10;
        d.ground = City.PLAZA;
        d.base = City.KIOSK;
        d.inside = Variants.I_SHOWROOM;
        d.roofCol = 0xFFE2D8C4;
        d.districts = Variants.DOWN | Variants.MID | Variants.OLD;
        out.add(d);
    }

    // ================================================================== (stubs to fill)

    static void equestrian(List<Landmarks.Def> out) {
    }

    static void pavilion(List<Landmarks.Def> out) {
    }

    static void veterans(List<Landmarks.Def> out) {
    }

    static void weather(List<Landmarks.Def> out) {
    }

    static void lighthouse(List<Landmarks.Def> out) {
    }

    static void windmill(List<Landmarks.Def> out) {
    }

    static void foundry(List<Landmarks.Def> out) {
    }

    static void cemetery(List<Landmarks.Def> out) {
    }

    static void spa(List<Landmarks.Def> out) {
    }

    static void embassy(List<Landmarks.Def> out) {
    }
}
