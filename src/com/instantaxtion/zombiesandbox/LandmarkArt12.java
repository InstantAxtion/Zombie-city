package com.instantaxtion.zombiesandbox;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.Landmarks.*;

/** ((22.8) Landmark designs, part 12 (see Landmarks): quirky attractions and oddities that make a map memorable. */
final class LandmarkArt12 {
    private LandmarkArt12() {
    }

    static final float TT = City.T;

    static void register(List<Landmarks.Def> out) {
        coaster(out);
        haunted(out);
        donut(out);
        ufo(out);
        pyramid(out);
        geodesic(out);
        colossus(out);
        iceCream(out);
        toyStore(out);
        waxMuseum(out);
        escapeRooms(out);
        laserTag(out);
        planetWalk(out);
        labyrinth(out);
        dinoPark(out);
        rocket(out);
        clockPlaza(out);
        chessPark(out);
        skyBridge(out);
        upsideDown(out);
        ropeCourse(out);
        balloons(out);
        festival(out);
        sandcastles(out);
        glassBridge(out);
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

        /** A line with round ends. */
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

        /** A stroked polyline through {u0, v0, u1, v1, ...}, round-capped; closed joins the ends. */
        void stroke(int col, float w, float[] q, boolean closed) {
            Path path = new Path();
            path.moveTo(x(q[0], q[1]), y(q[0], q[1]));
            for (int k = 2; k + 1 < q.length; k += 2) path.lineTo(x(q[k], q[k + 1]), y(q[k], q[k + 1]));
            if (closed) path.close();
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(w);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(col);
            c.drawPath(path, p);
            p.setStrokeCap(Paint.Cap.BUTT);
            p.setStyle(Paint.Style.FILL);
        }

        /** A stroked circular arc from angle a0 to a1 (radians). */
        void arc(int col, float w, float u, float v, float r, double a0, double a1) {
            int n = Math.max(3, (int) (Math.abs(a1 - a0) * r / 3));
            float[] q = new float[(n + 1) * 2];
            for (int k = 0; k <= n; k++) {
                double a = a0 + (a1 - a0) * k / n;
                q[2 * k] = u + (float) Math.cos(a) * r;
                q[2 * k + 1] = v + (float) Math.sin(a) * r;
            }
            stroke(col, w, q, false);
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
        void sh(float u0, float v0, float u1, float v1, float d) {
            rect(0x38000000, u0 + d * 0.7f, v0 + d * 0.7f, u1 + d, v1 + d);
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

    /** A smooth ball or dome: a shadow, the body, a darker rim and a highlight. */
    static void ball(F f, float u, float v, float r, int col) {
        f.circle(0x40000000, u + r * 0.3f, v + r * 0.35f, r);
        f.circle(City.darken(col, 0.75f), u, v, r);
        f.circle(col, u - r * 0.1f, v - r * 0.1f, r * 0.82f);
        f.circle(City.lighten(col, 0.22f), u - r * 0.3f, v - r * 0.3f, r * 0.4f);
        f.circle(City.lighten(col, 0.5f), u - r * 0.4f, v - r * 0.4f, r * 0.14f);
    }

    /** A person from above: shoulders and head. */
    static void person(F f, float u, float v, int col) {
        f.circle(0x30000000, u + 0.8f, v + 0.8f, 1.6f);
        f.circle(col, u, v, 1.6f);
        f.circle(0xFF3A2A20, u, v, 0.85f);
    }

    static final int[] CLOTHES = {0xFFB84A3E, 0xFF3E6AA0, 0xFFD8B040, 0xFF4E8A50, 0xFFE0E0D8, 0xFF8A4AA0, 0xFF2E2E34, 0xFFE07A40};

    /** A parasol from above: alternating wedges, a shadow and a finial. */
    static void parasol(F f, float u, float v, float r, int c1, int c2) {
        f.circle(0x38000000, u + r * 0.4f, v + r * 0.45f, r);
        int n = 8;
        for (int k = 0; k < n; k++) {
            double a0 = k * 2 * Math.PI / n, a1 = (k + 1) * 2 * Math.PI / n, am = (a0 + a1) / 2;
            float s = 1f + 0.12f * (float) Math.cos(am + 3 * Math.PI / 4);
            f.poly(Landmarks.shade(k % 2 == 0 ? c1 : c2, s), u, v, u + (float) Math.cos(a0) * r, v + (float) Math.sin(a0) * r,
                    u + (float) Math.cos(a1) * r, v + (float) Math.sin(a1) * r);
        }
        f.circle(0xFFE8E0D0, u, v, Math.max(0.6f, r * 0.12f));
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

    /** A small flower bed: a soil oval dotted with blooms. */
    static void flowers(F f, float u0, float v0, float u1, float v1, int c1, int c2) {
        f.oval(0xFF6A5040, u0, v0, u1, v1);
        f.oval(0xFF4E7E3C, u0 + 0.8f, v0 + 0.8f, u1 - 0.8f, v1 - 0.8f);
        int k = 0;
        for (float u = u0 + 2; u < u1 - 1.5f; u += 2.6f)
            for (float v = v0 + 2; v < v1 - 1.5f; v += 2.6f, k++)
                f.circle(k % 2 == 0 ? c1 : c2, u + (k % 3) * 0.4f, v, 0.85f);
    }

    static float clamp(float x, float lo, float hi) {
        return x < lo ? lo : x > hi ? hi : x;
    }

    // ================================================================== 1 Roller Coaster Park

    /** Smooths a closed loop of {u, v, h} points by corner cutting (Chaikin), n times. */
    static float[] chaikin(float[] q, int n) {
        for (int it = 0; it < n; it++) {
            int m = q.length / 3;
            float[] o = new float[m * 6];
            for (int k = 0; k < m; k++) {
                int a = k * 3, b = ((k + 1) % m) * 3;
                for (int j = 0; j < 3; j++) {
                    o[k * 6 + j] = q[a + j] * 0.75f + q[b + j] * 0.25f;
                    o[k * 6 + 3 + j] = q[a + j] * 0.25f + q[b + j] * 0.75f;
                }
            }
            q = o;
        }
        return q;
    }

    /** A coaster car on the track at point k of the loop q, pointing along it. */
    static void coasterCar(F f, float[] q, int k, int col) {
        int m = q.length / 3, a = ((k - 1 + m) % m) * 3, b = ((k + 1) % m) * 3, c0 = (k % m) * 3;
        float du = q[b] - q[a], dv = q[b + 1] - q[a + 1], l = (float) Math.sqrt(du * du + dv * dv);
        if (l < 0.01f) return;
        du /= l;
        dv /= l;
        float u = q[c0], v = q[c0 + 1], h = q[c0 + 2], hl = 3.4f, hw = 2.4f;
        float su = u + h * 0.45f, sv = v + h * 0.45f;
        f.poly(0x30000000, su - du * hl + dv * hw, sv - dv * hl - du * hw, su + du * hl + dv * hw, sv + dv * hl - du * hw,
                su + du * hl - dv * hw, sv + dv * hl + du * hw, su - du * hl - dv * hw, sv - dv * hl + du * hw);
        f.poly(City.darken(col, 0.75f), u - du * hl + dv * hw, v - dv * hl - du * hw, u + du * hl + dv * hw, v + dv * hl - du * hw,
                u + du * hl - dv * hw, v + dv * hl + du * hw, u - du * hl - dv * hw, v - dv * hl + du * hw);
        float i = 0.6f;
        f.poly(col, u - du * (hl - i) + dv * (hw - i), v - dv * (hl - i) - du * (hw - i), u + du * (hl - i) + dv * (hw - i),
                v + dv * (hl - i) - du * (hw - i), u + du * (hl - i) - dv * (hw - i), v + dv * (hl - i) + du * (hw - i),
                u - du * (hl - i) - dv * (hw - i), v - dv * (hl - i) + du * (hw - i));
        // Two rows of riders.
        for (int s = -1; s <= 1; s += 2) {
            float pu = u + du * s * 1.2f, pv = v + dv * s * 1.2f;
            f.circle(0xFF2A2A2E, pu + dv * 1f, pv - du * 1f, 0.8f);
            f.circle(0xFF2A2A2E, pu - dv * 1f, pv + du * 1f, 0.8f);
            f.circle(0xFFE0B890, pu + dv * 1f - du * 0.2f, pv - du * 1f - dv * 0.2f, 0.55f);
            f.circle(0xFFE0B890, pu - dv * 1f - du * 0.2f, pv + du * 1f - dv * 0.2f, 0.55f);
        }
    }

    static void coaster(List<Landmarks.Def> out) {
        // A steel coaster looping round a small park: a lift hill climbing out of the station, a vertical loop and
        // a camelback dipping over a pond, the train full of riders, a queue maze, a carousel and a gift shop.
        Landmarks.Def d = new Landmarks.Def("Roller Coaster Park", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, 2, S - 5, 5, 3, 1), at(al, L - 5, S - 5, 3, 3, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] pq = pl(this, x0, y0, x1, y1);
                float[] st = bx(f, pq[0]), sp = bx(f, pq[1]);
                f.rect(0xFF6E9A52, 0, 0, L, S);
                for (int k = 0; k < 40; k++) f.circle(0x14000000, r.nextFloat() * L, r.nextFloat() * S, 2 + r.nextFloat() * 3);
                // The paved midway along the front.
                float pv = st[1] - 7;
                f.rect(0xFFCFC3A8, 0, pv, L, S);
                for (float u = 4; u < L; u += 8) f.line(0x18000000, 0.5f, u, pv, u, S);
                f.line(0x30000000, 0.8f, 0, pv, L, pv);
                // The track's loop, from the station round the park.
                float vb = (st[1] + st[3]) / 2, top = 12, a1 = st[2];
                float[] cp = {
                        st[0] - 2, vb, 2,
                        a1 + 8, vb, 2,
                        a1 + 20, pv - 12, 26,
                        L - 26, pv - 16, 34,
                        L - 11, S * 0.42f, 30,
                        L - 14, top, 24,
                        L * 0.62f, top, 16,
                        L * 0.5f, S * 0.36f, 4,
                        L * 0.38f, top + 2, 14,
                        14, top, 12,
                        9, (top + vb) / 2, 8,
                        9, vb - 6, 3};
                float[] q = chaikin(cp, 3);
                int m = q.length / 3;
                // A pond under the camelback, trees and beds inside the loop.
                float cu = L * 0.5f, cv = S * 0.36f;
                pond(c, p, f.x(cu - 13, cv - 6), f.y(cu - 13, cv - 6), f.x(cu + 13, cv + 7), f.y(cu + 13, cv + 7));
                // Trees wherever the track leaves room: inside the loop and round the edge.
                for (float tu = 8; tu < L - 4; tu += 21)
                    for (float tv = 7; tv < pv - 4; tv += 19) {
                        float ju = tu + r.nextFloat() * 6 - 3, jv = tv + r.nextFloat() * 6 - 3, best = 1e9f;
                        for (int k = 0; k < m; k++) {
                            float du = q[3 * k] - ju, dv = q[3 * k + 1] - jv;
                            best = Math.min(best, du * du + dv * dv);
                        }
                        float pd = (ju - cu) * (ju - cu) / 4 + (jv - cv) * (jv - cv);
                        if (best > 160 && pd > 200 && !(ju > L * 0.2f && ju < L * 0.32f && jv < top + 14))
                            f.tree(ju, jv, 4.5f + r.nextFloat() * 2.5f, r.nextBoolean() ? 0xFF4A7E3C : 0xFF3E7034);
                    }
                f.bush(L * 0.4f, pv - 6, 2.6f);
                f.bush(L * 0.6f, pv - 5, 2.6f);
                flowers(f, 18, top + 12, 30, top + 20, 0xFFE05060, 0xFFF0D050);
                // The carousel on the midway between station and shop.
                float ku = (st[2] + sp[0]) / 2, kv = S - 9;
                if (sp[0] - st[2] > 26) {
                    float kr = Math.min(9, (sp[0] - st[2]) / 2 - 4);
                    f.circle(0x38000000, ku + 2, kv + 2, kr);
                    for (int k = 0; k < 12; k++) {
                        double a0 = k * Math.PI / 6, a2 = (k + 1) * Math.PI / 6;
                        f.poly(k % 2 == 0 ? 0xFFC8483C : 0xFFF0E6D0, ku, kv, ku + (float) Math.cos(a0) * kr, kv + (float) Math.sin(a0) * kr,
                                ku + (float) Math.cos(a2) * kr, kv + (float) Math.sin(a2) * kr);
                    }
                    f.ring(0xFFD8B040, 0.8f, ku, kv, kr - 0.4f);
                    f.circle(0xFFD8B040, ku, kv, 1.6f);
                    f.circle(0xFFF4E8B0, ku - 0.4f, kv - 0.4f, 0.7f);
                } else kv = S - 3;
                // The track's shadow on the ground, then supports, ties, rails.
                float[] s = new float[m * 2];
                for (int k = 0; k < m; k++) {
                    s[2 * k] = q[3 * k] + q[3 * k + 2] * 0.45f;
                    s[2 * k + 1] = q[3 * k + 1] + q[3 * k + 2] * 0.45f;
                }
                f.stroke(0x2C000000, 5f, s, true);
                float[] ra = new float[m * 2], rb = new float[m * 2], ce = new float[m * 2];
                for (int k = 0; k < m; k++) {
                    int a = ((k - 1 + m) % m) * 3, b = ((k + 1) % m) * 3;
                    float du = q[b] - q[a], dv = q[b + 1] - q[a + 1], l = (float) Math.sqrt(du * du + dv * dv);
                    du /= l;
                    dv /= l;
                    float u = q[3 * k], v = q[3 * k + 1];
                    ra[2 * k] = u + dv * 2.3f;
                    ra[2 * k + 1] = v - du * 2.3f;
                    rb[2 * k] = u - dv * 2.3f;
                    rb[2 * k + 1] = v + du * 2.3f;
                    ce[2 * k] = u;
                    ce[2 * k + 1] = v;
                    if (k % 3 == 0 && q[3 * k + 2] > 5) {
                        f.line(0x40000000, 1.4f, u, v, s[2 * k], s[2 * k + 1]);
                        f.rect(0xFF5A6068, s[2 * k] - 1.2f, s[2 * k + 1] - 1.2f, s[2 * k] + 1.2f, s[2 * k + 1] + 1.2f);
                        f.line(0xFF8A9098, 1.1f, u, v, s[2 * k], s[2 * k + 1]);
                    }
                }
                f.stroke(0xFF4A4E56, 3f, ce, true);
                for (int k = 0; k < m; k++) {
                    int b = ((k + 1) % m);
                    boolean lift = k >= 8 && k <= 18;
                    f.line(lift ? 0xFF3A3A40 : 0xFF6A6E76, 0.7f, ra[2 * k], ra[2 * k + 1], rb[2 * k], rb[2 * k + 1]);
                    f.line(0xFF6A6E76, 0.6f, (ra[2 * k] + ra[2 * b]) / 2, (ra[2 * k + 1] + ra[2 * b + 1]) / 2,
                            (rb[2 * k] + rb[2 * b]) / 2, (rb[2 * k + 1] + rb[2 * b + 1]) / 2);
                }
                f.stroke(0xFFC8402E, 1.3f, ra, true);
                f.stroke(0xFFC8402E, 1.3f, rb, true);
                for (int k = 9; k <= 18; k++) f.line(0xFFE8D060, 0.5f, ce[2 * k], ce[2 * k + 1], ce[2 * k + 2], ce[2 * k + 3]);
                // The vertical loop on the top straight: seen from above, a narrow ring and its long shadow.
                float lu = L * 0.26f, lv = top + 0.6f;
                f.oval(0x2A000000, lu - 4, lv + 9, lu + 30, lv + 21);
                f.oval(0xFF4A4E56, lu - 14, lv - 5, lu + 14, lv + 5);
                f.oval(0xFFC8402E, lu - 13, lv - 4.2f, lu + 13, lv + 4.2f);
                f.oval(0xFF4A4E56, lu - 11.6f, lv - 2.8f, lu + 11.6f, lv + 2.8f);
                f.oval(0xFF6E9A52, lu - 10.6f, lv - 1.9f, lu + 10.6f, lv + 1.9f);
                f.oval(0xFF5E8A46, lu - 10.6f, lv, lu + 10.6f, lv + 1.9f);
                f.circle(0xFFE8E4DC, lu - 12.4f, lv, 0.7f);
                f.circle(0xFFE8E4DC, lu + 12.4f, lv, 0.7f);
                // The train on the far straight.
                for (int k = 0; k < 4; k++) coasterCar(f, q, m * 52 / 96 - k * 2, k == 0 ? 0xFF2E5AA0 : 0xFF3E6AB0);
                // The queue maze in front of the station, with people waiting.
                float qa = st[0] + 2, qb = st[2] - 2, q0 = pv + 2, q1 = st[1] - 1.5f;
                for (float v = q0; v <= q1 + 0.1f; v += 2.5f) f.line(0xFF7A7E86, 0.5f, qa, v, qb, v);
                for (float u = qa; u <= qb; u += 6) f.circle(0xFF5A5E66, u, q0, 0.6f);
                for (int k = 0; k < 8; k++) person(f, qa + 3 + (k * 7.3f) % (qb - qa - 4), q0 + 1.2f + (k % 2) * 2.5f, CLOTHES[k % 8]);
                // Snack stalls along the midway between the carousel and the shop.
                for (float u = st[2] + 8; u + 10 < sp[0] - 4; u += 24) {
                    if (Math.abs(u + 5 - ku) < 16 && sp[0] - st[2] > 26) continue;
                    float v0 = st[1] + 2;
                    f.rect(0x38000000, u + 1.5f, v0 + 1.5f, u + 11.5f, v0 + 8.5f);
                    for (int k = 0; k < 5; k++) f.rect(k % 2 == 0 ? 0xFFE8B040 : 0xFFF2EADA, u + k * 2, v0, u + k * 2 + 2, v0 + 7);
                    f.line(0x40000000, 0.6f, u, v0 + 3.5f, u + 10, v0 + 3.5f);
                    f.rect(0xFF6E5034, u + 1, v0 + 7, u + 9, v0 + 8.2f);
                    person(f, u + 5, v0 + 11, CLOTHES[(int) u % 8]);
                    f.circle(0xFFE05050, u + 13, v0 + 2, 1.4f);
                    f.circle(0xFF50A0E0, u + 14.5f, v0 + 3.5f, 1.4f);
                    f.line(0x80FFFFFF, 0.3f, u + 13, v0 + 3.4f, u + 13.5f, v0 + 8);
                }
                // Lamps, benches, people on the midway.
                f.lamp(sp[0] - 4, pv + 3);
                f.lamp(st[2] + 4, pv + 3);
                f.bench(L - 6, pv - 4, true);
                for (int k = 0; k < 5; k++) person(f, st[2] + 6 + k * 6.5f, kv + (k % 2 == 0 ? -12 : 10), CLOTHES[(k + 3) % 8]);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                if (part == 0) {
                    // The station: a red canopy with a raised lantern over the track and a sign.
                    f.rect(0xFF7A3028, 0, 0, L, S);
                    gable(f, 0xFFB8483A, 1, 1, L - 1, S - 1);
                    for (float u = 6; u < L - 3; u += 6) f.line(0x30000000, 0.5f, u, 1, u, S - 1);
                    f.rect(0x40000000, L * 0.3f + 1.5f, S / 2 - 2 + 1.5f, L * 0.7f + 1.5f, S / 2 + 2 + 1.5f);
                    f.rect(0xFFE8E0D0, L * 0.3f, S / 2 - 2, L * 0.7f, S / 2 + 2);
                    f.rect(0xFFC8402E, L * 0.3f, S / 2 - 0.6f, L * 0.7f, S / 2 + 0.6f);
                    f.circle(0xFFE8C040, 3, 3, 1.1f);
                    f.circle(0xFFE8C040, L - 3, 3, 1.1f);
                    f.sign(city, "CYCLONE", L / 2, S - 4, 5, 0xFFF4E8C8);
                } else {
                    // The gift shop under a striped awning roof.
                    f.rect(0xFF3A5A7A, 0, 0, L, S);
                    float w = (S - 2) / 8;
                    for (int k = 0; k < 8; k++) f.rect(k % 2 == 0 ? 0xFF3E6EA8 : 0xFFE8E2D4, 1, 1 + k * w, L - 1, 1 + (k + 1) * w);
                    f.rect(0x30000000, 1, S / 2, L - 1, S - 1);
                    f.line(0xFFF0EAD8, 1f, 1, S / 2, L - 1, S / 2);
                    ball(f, L * 0.3f, S * 0.3f, 3, 0xFFE05050);
                    ball(f, L * 0.42f, S * 0.22f, 2.6f, 0xFFF0C040);
                    f.sign(city, "GIFTS", L / 2, S - 4, 4, 0xFFF4E8C8);
                }
            }
        };
        d.minS = 10;
        d.minL = 12;
        d.ground = City.GRASS;
        d.base = City.SHOP;
        d.inside = Variants.I_ARCADE;
        d.roofCol = 0xFFB8483A;
        d.districts = Variants.PARK | Variants.SUB | Variants.MID;
        out.add(d);
    }

    // ================================================================== 2 Haunted House

    /** A bare dead tree from above: a gnarled trunk and forking grey branches. */
    static void deadTree(F f, float u, float v, float r, Random rnd) {
        f.circle(0x30000000, u + 2, v + 2, 1.6f);
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3 + rnd.nextFloat() * 0.6f;
            float eu = u + (float) Math.cos(a) * r, ev = v + (float) Math.sin(a) * r;
            float mu = u + (float) Math.cos(a) * r * 0.5f, mv = v + (float) Math.sin(a) * r * 0.5f;
            f.line(0x26000000, 0.8f, mu + 2, mv + 2, eu + 2.5f, ev + 2.5f);
            f.line(0xFF4A4038, 1.1f, u, v, mu, mv);
            f.line(0xFF5A5048, 0.6f, mu, mv, eu, ev);
            double b = a + 0.7f;
            f.line(0xFF5A5048, 0.45f, mu, mv, mu + (float) Math.cos(b) * r * 0.4f, mv + (float) Math.sin(b) * r * 0.4f);
        }
        f.circle(0xFF3E342C, u, v, 1.5f);
    }

    /** A headstone, a little tilted, with its shadow. */
    static void headstone(F f, float u, float v, float tilt, int kind) {
        float w = 2.2f, h = 1.2f;
        f.poly(0x40000000, u - w + 1.2f, v - h + 1.6f + tilt, u + w + 1.2f, v - h + 1.6f - tilt, u + w + 1.2f, v + h + 1.6f - tilt,
                u - w + 1.2f, v + h + 1.6f + tilt);
        if (kind == 0) {
            f.poly(0xFF8A8A84, u - w, v - h + tilt, u + w, v - h - tilt, u + w, v + h - tilt, u - w, v + h + tilt);
            f.poly(0xFFA8A8A0, u - w, v - h + tilt, u + w, v - h - tilt, u + w, v - tilt, u - w, v + tilt);
        } else {
            f.rect(0xFF7E7E78, u - 0.6f, v - 2.6f, u + 0.6f, v + 2.6f);
            f.rect(0xFF9A9A92, u - 2, v - 1.4f, u + 2, v - 0.4f);
        }
        f.rect(0x30304020, u - w, v + h + 0.6f, u + w, v + h + 4.5f);
    }

    static void haunted(List<Landmarks.Def> out) {
        // A crooked Gothic mansion with a witch-hat turret and sagging slate roofs, a graveyard of leaning stones
        // under dead trees behind an iron fence, pumpkins, ground fog and a long roped queue up to the door.
        Landmarks.Def d = new Landmarks.Def("Haunted House", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int lu = Math.min(8, L - 6), lv = S - 6;
                return new int[][]{at(al, 2, 2, lu, lv, 3)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] h = bx(f, pl(this, x0, y0, x1, y1)[0]);
                f.rect(0xFF5E6646, 0, 0, L, S);
                for (int k = 0; k < 50; k++) {
                    float u = r.nextFloat() * L, v = r.nextFloat() * S, s = 2 + r.nextFloat() * 4;
                    f.oval(r.nextBoolean() ? 0x186A5A30 : 0x18000000, u - s, v - s * 0.6f, u + s, v + s * 0.6f);
                }
                // The gravel drive and the queue to the porch.
                float du = (h[0] + h[2]) / 2;
                f.rect(0xFF8A8270, du - 5, h[3], du + 5, S);
                for (int k = 0; k < 30; k++) f.circle(0x30000000, du - 4.5f + r.nextFloat() * 9, h[3] + r.nextFloat() * (S - h[3]), 0.5f);
                float qa = Math.max(h[0] + 2, du - 46), qb = du - 8, q0 = h[3] + 4, q1 = S - 3;
                f.rect(0xFF7E7666, qa - 2, q0 - 2.5f, qb + 2, q1 + 1.5f);
                for (float v = q0; v <= q1 + 0.1f; v += 4.5f) {
                    f.line(0xFF2A1A20, 0.6f, qa, v, qb, v);
                    f.circle(0xFF8A7A50, qa, v, 0.7f);
                    f.circle(0xFF8A7A50, qb, v, 0.7f);
                }
                int pi = 0;
                for (float v = q0 + 2.25f; v < q1; v += 4.5f)
                    for (float u = qa + 3 + (pi % 4) * 2; u < qb - 1; u += 6 + (pi % 5) * 2.5f) person(f, u, v, CLOTHES[pi++ % 8]);
                // The graveyard: iron railings, rows of leaning stones, an open grave and dead trees.
                float g0 = h[2] + 6, g1 = L - 3, gv0 = 4, gv1 = S - 4;
                f.rect(0xFF525A3E, g0, gv0, g1, gv1);
                float gm = (gv0 + gv1) / 2;
                f.rect(0xFF7A7464, g0, gm - 3, g1, gm + 3);
                float mu0 = g0 + 6, mu1 = Math.min(g1 - 6, mu0 + 26);
                boolean maus = g1 - g0 > 40;
                for (float u = g0 + 5; u < g1 - 3; u += 8)
                    for (float v = gv0 + 6; v < gv1 - 5; v += 11)
                        if (Math.abs(v - gm) > 6 && !(maus && u > mu0 - 3 && u < mu1 + 3 && v < gm))
                            headstone(f, u + ((int) v % 3), v, (r.nextFloat() - 0.5f) * 1.4f, r.nextInt(4) == 0 ? 1 : 0);
                if (maus) {
                    // A little stone mausoleum with a pediment roof.
                    float mv0 = gv0 + 4, mv1 = Math.min(gm - 6, mv0 + 18);
                    f.rect(0x48000000, mu0 + 2, mv0 + 2.5f, mu1 + 2.5f, mv1 + 3);
                    f.rect(0xFF8E8A80, mu0, mv0, mu1, mv1);
                    gable(f, 0xFF7A766E, mu0 + 1, mv0 + 1, mu1 - 1, mv1 - 1);
                    f.rect(0xFF2A2624, (mu0 + mu1) / 2 - 2.5f, mv1 - 0.5f, (mu0 + mu1) / 2 + 2.5f, mv1 + 1.5f);
                }
                float ou = g0 + (g1 - g0) * 0.5f, ov = gv1 - 7;
                f.rect(0xFF2A2018, ou - 2.5f, ov - 4, ou + 2.5f, ov + 4);
                f.rect(0xFF1A140E, ou - 1.8f, ov - 3.2f, ou + 1.8f, ov + 3.2f);
                f.oval(0xFF6A5438, ou + 3.5f, ov - 4, ou + 8, ov + 4);
                f.oval(0xFF7E6644, ou + 4, ov - 3, ou + 6.5f, ov + 1);
                f.frame(0xFF222226, 0.8f, g0, gv0, g1, gv1);
                for (float u = g0; u <= g1; u += 2.5f) {
                    f.circle(0xFF222226, u, gv0, 0.55f);
                    f.circle(0xFF222226, u, gv1, 0.55f);
                }
                for (float v = gv0; v <= gv1; v += 2.5f) {
                    f.circle(0xFF222226, g0, v, 0.55f);
                    f.circle(0xFF222226, g1, v, 0.55f);
                }
                f.rect(0xFF525A3E, g0 - 1, gv1 - 12, g0 + 1, gv1 - 6);
                deadTree(f, g1 - 7, gv0 + 7, 12, r);
                deadTree(f, g0 + 5, gv1 - 5, 10, r);
                deadTree(f, h[0] + 1, h[3] + 4, 9, r);
                deadTree(f, h[2] + 3, 6, 10, r);
                // Pumpkins by the drive, glowing.
                for (int k = 0; k < 5; k++) {
                    float u = du + (k % 2 == 0 ? 7 : -7), v = h[3] + 3 + k * 4;
                    if (v > S - 2) break;
                    f.circle(0x40000000, u + 0.8f, v + 0.8f, 1.6f);
                    f.circle(0xFFC86A20, u, v, 1.6f);
                    f.circle(0xFFE89040, u - 0.4f, v - 0.4f, 0.8f);
                    f.circle(0xFF3E6A2A, u, v - 1.4f, 0.4f);
                }
                // Ground fog drifting across.
                for (int k = 0; k < 7; k++) {
                    float u = r.nextFloat() * L, v = r.nextFloat() * S, w = 10 + r.nextFloat() * 14;
                    f.oval(0x22E8ECF0, u - w, v - 3, u + w, v + 3);
                    f.oval(0x1CE8ECF0, u - w * 0.6f, v - 4.5f, u + w * 0.5f, v + 1);
                }
                f.lamp(du - 6, S - 2);
                f.lamp(du + 6, S - 2);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF2E2A30, 0, 0, L, S);
                // A main range along u and a front wing, slates patched and sagging.
                gable(f, 0xFF4E4A58, 1, 1, L - 1, S * 0.62f);
                float wu0 = L * 0.36f, wu1 = L * 0.64f;
                gableV(f, 0xFF4A4654, wu0, S * 0.3f, wu1, S - 1);
                for (float u = 4; u < L - 2; u += 3) f.line(0x22000000, 0.4f, u, 1.5f, u, S * 0.6f);
                for (int k = 0; k < 7; k++) {
                    float u = 4 + r.nextFloat() * (L - 10), v = 2 + r.nextFloat() * (S * 0.55f);
                    f.rect(r.nextBoolean() ? 0xFF6A6474 : 0xFF241E26, u, v, u + 2.5f, v + 1.6f);
                }
                f.line(0xFF7A7484, 0.8f, 1, S * 0.31f, wu0, S * 0.33f);
                f.line(0xFF7A7484, 0.8f, wu1, S * 0.33f, L - 1, S * 0.3f);
                // Dormers with a dim yellow glow.
                for (float u = 6; u < L - 4; u += 9) {
                    if (u > wu0 - 3 && u < wu1 + 1) continue;
                    f.rect(0x40000000, u + 1, 2.5f, u + 5, 6.5f);
                    f.poly(0xFF3A3640, u, 5.5f, u + 2, 1.5f, u + 4, 5.5f);
                    f.rect(0xFFD8C060, u + 1.2f, 4, u + 2.8f, 5.5f);
                }
                // Crooked chimneys.
                for (int s = 0; s < 2; s++) {
                    float cu = s == 0 ? 4 : L - 6, cv = S * 0.15f;
                    f.poly(0x40000000, cu + 1, cv + 1, cu + 5, cv + 2, cu + 4.5f, cv + 5, cu + 0.5f, cv + 4);
                    f.poly(0xFF5A3A30, cu, cv, cu + 4, cv + 0.6f, cu + 3.6f, cv + 3.6f, cu - 0.4f, cv + 3);
                    f.circle(0xFF1A1414, cu + 1.8f, cv + 1.8f, 0.8f);
                }
                // The turret on the corner: a steep conical witch's hat.
                float tu = L - 0.25f * Math.min(L, S), tv = S - 0.25f * Math.min(L, S), tr = Math.min(L, S) * 0.22f;
                f.circle(0x50000000, tu + 2, tv + 2.5f, tr);
                f.circle(0xFF2A2630, tu, tv, tr);
                dome(f, tu, tv, tr - 0.8f, 0xFF4A3E56, 10);
                f.circle(0xFF7A6A40, tu, tv, 0.8f);
                f.line(0xFF7A6A40, 0.5f, tu - 2, tv - 0.5f, tu + 2, tv + 0.5f);
                // Bats.
                for (int k = 0; k < 3; k++) {
                    float u = L * (0.2f + k * 0.25f), v = S * (0.75f - k * 0.1f);
                    f.poly(0xFF141016, u - 2, v - 0.6f, u, v + 0.4f, u + 2, v - 0.6f, u + 1, v + 0.8f, u - 1, v + 0.8f);
                }
                f.sign(city, "HAUNTED HOUSE", (wu0 + wu1) / 2, S - 4, 4.5f, 0xFFD8E0B0);
            }
        };
        d.minS = 10;
        d.minL = 14;
        d.ground = City.GRASS;
        d.base = City.SHOP;
        d.inside = Variants.I_ARCADE;
        d.roofCol = 0xFF4E4A58;
        d.districts = Variants.OLD | Variants.SUB | Variants.PARK;
        out.add(d);
    }

    // ================================================================== 3 Giant Donut Drive-In

    /** A frosted donut from above, centred at (u, v), outer radius R; hole shows col underneath. */
    static void donutTop(F f, float u, float v, float R, int under, int icing, Random r) {
        float hr = R * 0.34f;
        f.circle(0x48000000, u + R * 0.18f, v + R * 0.22f, R);
        f.circle(0xFFA8743E, u, v, R);
        f.circle(0xFFC89458, u - R * 0.05f, v - R * 0.05f, R * 0.92f);
        int n = 28;
        float[] q = new float[n * 2];
        for (int k = 0; k < n; k++) {
            double a = k * 2 * Math.PI / n;
            float rr = R * (0.8f + 0.07f * (float) Math.sin(k * 2.3f) + (k % 2) * 0.04f);
            q[2 * k] = u + (float) Math.cos(a) * rr;
            q[2 * k + 1] = v + (float) Math.sin(a) * rr;
        }
        f.poly(City.darken(icing, 0.85f), q);
        f.circle(icing, u - R * 0.04f, v - R * 0.04f, R * 0.7f);
        f.oval(City.lighten(icing, 0.25f), u - R * 0.62f, v - R * 0.6f, u - R * 0.05f, v - R * 0.28f);
        int[] sp = {0xFFF4F0E0, 0xFF60B0E0, 0xFFF0D040, 0xFF70C060, 0xFFE05050, 0xFF9060C0};
        for (int k = 0; k < 40; k++) {
            double a = r.nextFloat() * 2 * Math.PI;
            float rr = hr + 2 + r.nextFloat() * (R * 0.72f - hr - 3);
            float su = u + (float) Math.cos(a) * rr, sv = v + (float) Math.sin(a) * rr;
            double b = r.nextFloat() * Math.PI;
            float l = R * 0.04f + 0.5f;
            f.rline(sp[k % sp.length], Math.max(0.6f, R * 0.025f), su - (float) Math.cos(b) * l, sv - (float) Math.sin(b) * l,
                    su + (float) Math.cos(b) * l, sv + (float) Math.sin(b) * l);
        }
        // The hole: dough rim, then whatever lies below, in shade.
        f.circle(0xFFB07C44, u, v, hr + 1.4f);
        f.circle(under, u, v, hr);
        f.circle(0x50000000, u - hr * 0.2f, v - hr * 0.2f, hr * 0.85f);
    }

    static void donut(List<Landmarks.Def> out) {
        // A small drive-in diner wearing a colossal pink-frosted donut on its roof, a drive-thru lane looping round
        // it past a menu board, a donut on a tall pole by the road, and picnic tables under parasols.
        Landmarks.Def d = new Landmarks.Def("Giant Donut Drive-In", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int lu = 5, lv = 5;
                return new int[][]{at(al, Math.max(3, (L - lu) / 2 - 1), (S - lv) / 2, lu, lv, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] h = bx(f, pl(this, x0, y0, x1, y1)[0]);
                f.rect(0xFF6E9A52, 0, 0, L, S);
                // The asphalt drive-thru loop round the diner.
                float m = 14;
                f.roundRect(0xFF4A4C50, h[0] - m, h[1] - m, h[2] + m, h[3] + m, 10);
                f.roundRect(0xFF56585C, h[0] - m + 1, h[1] - m + 1, h[2] + m - 1, h[3] + m - 1, 9);
                f.rect(0xFFD8D0BC, h[0] - 3, h[1] - 3, h[2] + 3, h[3] + 3);
                f.rect(0xFF4A4C50, h[0] - m, h[3] + 2, h[0] - 3, S);
                f.rect(0xFF56585C, h[0] - m + 1, h[3] + 2, h[0] - 4, S);
                for (float u = h[0] - 3; u < h[2] + 3; u += 7)
                    f.line(0xFFE8E0C0, 0.6f, u, h[1] - m / 2, u + 3.5f, h[1] - m / 2);
                for (float v = h[1]; v < h[3]; v += 7) {
                    f.line(0xFFE8E0C0, 0.6f, h[2] + m / 2, v, h[2] + m / 2, v + 3.5f);
                    f.line(0xFFE8E0C0, 0.6f, h[0] - m / 2, v, h[0] - m / 2, v + 3.5f);
                }
                // Arrows round the loop.
                f.poly(0xFFE8E0C0, h[2] + m / 2 - 2, h[3] - 4, h[2] + m / 2 + 2, h[3] - 4, h[2] + m / 2, h[3] - 1);
                f.poly(0xFFE8E0C0, h[0] + 4, h[1] - m / 2 - 2, h[0] + 4, h[1] - m / 2 + 2, h[0] + 1, h[1] - m / 2);
                // Menu board and the pick-up window.
                f.rect(0x40000000, h[2] + m + 2, h[1] + 1, h[2] + m + 4.5f, h[1] + 12);
                f.rect(0xFF2E2A2A, h[2] + m + 1, h[1], h[2] + m + 3.5f, h[1] + 11);
                for (int k = 0; k < 4; k++) f.rect(k % 2 == 0 ? 0xFFF0C040 : 0xFFE06A8A, h[2] + m + 1.5f, h[1] + 1 + k * 2.5f, h[2] + m + 3, h[1] + 2.8f + k * 2.5f);
                car(f, h[2] + m / 2, h[1] + 6, false, 0xFF6A8AB0);
                car(f, (h[0] + h[2]) / 2 + 6, h[1] - m / 2, true, 0xFFC8A040);
                // A donut on a tall pole by the road.
                float pu = L - 10, pv = S - 10;
                f.tall(pu, pv, 0.9f, 26);
                f.circle(0x30000000, pu + 18, pv + 18, 7);
                f.circle(0xFF7A7E86, pu, pv, 1.2f);
                donutTop(f, pu, pv - 3, 7.5f, 0x00000000, 0xFF7A4A30, r);
                // Picnic tables under parasols on the lawn.
                float tu0 = h[2] + m + 10;
                int nt = 0;
                for (float tu = tu0; tu < L - 8; tu += 42)
                    for (float tv = 12; tv < pv - 12; tv += 40) {
                        if (nt++ >= 4) break;
                        f.rect(0xFF8A6A48, tu - 4, tv - 1.6f, tu + 4, tv + 1.6f);
                        f.rect(0xFF6E5034, tu - 4, tv - 4, tu + 4, tv - 3);
                        f.rect(0xFF6E5034, tu - 4, tv + 3, tu + 4, tv + 4);
                        parasol(f, tu, tv, 5, 0xFFE07090, 0xFFF4ECE0);
                    }
                for (float tu = 10; tu < h[0] - m - 6; tu += 42)
                    for (float tv = 12; tv < S - 8; tv += 40) {
                        if (nt++ >= 6) break;
                        f.rect(0xFF8A6A48, tu - 4, tv - 1.6f, tu + 4, tv + 1.6f);
                        parasol(f, tu, tv, 5, 0xFF60A0C8, 0xFFF4ECE0);
                    }
                for (float tu = 7; tu < L - 4; tu += 30) {
                    if (tu > h[0] - m - 8 && tu < h[2] + m + 8) continue;
                    f.tree(tu + 9, S - 7, 6, 0xFF4A7E3C);
                    f.bush(tu + 2, 4, 2.6f);
                }
                f.bush(h[0] - m - 4, 4, 2.5f);
                f.bush(h[2] + m + 4, S - 4, 2.5f);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                // A flat white roof with a candy-pink fascia, vents, and the giant donut on top.
                f.rect(0xFFD86A8A, 0, 0, L, S);
                f.rect(0xFFE8E4DC, 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                for (float u = 1.5f; u < L - 1.5f; u += 4) f.rect(0xFFF0A0B8, u, 0, u + 2, 1.5f);
                f.rect(0xFF9A9EA4, 3, S - 6, 7, S - 3);
                f.rect(0xFF7A7E86, 3.5f, S - 5.5f, 6.5f, S - 3.5f);
                float R = Math.min(L, S) * 0.43f;
                donutTop(f, L / 2, S / 2, R, 0xFFE8E4DC, 0xFFE888A8, r);
                f.sign(city, "DONUTS", L / 2, S - 3, 4, 0xFFFFF0F4);
            }
        };
        d.minS = 10;
        d.minL = 12;
        d.ground = City.GRASS;
        d.base = City.SHOP;
        d.inside = Variants.I_CAFE;
        d.roofCol = 0xFFE8E4DC;
        d.districts = Variants.SUB | Variants.MID | Variants.IND;
        out.add(d);
    }

    // ================================================================== 4 UFO Diner

    static void ufo(List<Landmarks.Def> out) {
        // A flying saucer that has landed as a diner: a silver disc ringed with lights and a glass dome, on a
        // chequered patio, a crop circle mown into the lawn beside it, a little green man and fifties cars.
        Landmarks.Def d = new Landmarks.Def("UFO Diner", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int s = Math.min(S - 4, 7);
                return new int[][]{at(al, 2, (S - s) / 2, s, s, 2)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] h = bx(f, pl(this, x0, y0, x1, y1)[0]);
                f.rect(0xFF6E9A52, 0, 0, L, S);
                float cu = (h[0] + h[2]) / 2, cv = (h[1] + h[3]) / 2, R = (h[2] - h[0]) / 2;
                // A chequered patio round the saucer, black and white.
                float pr = R + 10;
                f.circle(0xFFD8D2C4, cu, cv, pr + 1);
                for (float u = cu - pr; u < cu + pr; u += 4)
                    for (float v = cv - pr; v < cv + pr; v += 4) {
                        float du = u + 2 - cu, dv = v + 2 - cv;
                        if (du * du + dv * dv < pr * pr && ((int) ((u - cu + 400) / 4) + (int) ((v - cv + 400) / 4)) % 2 == 0)
                            f.rect(0xFF3A3A40, u, v, u + 4, v + 4);
                    }
                f.ring(0xFFE06AA0, 1.2f, cu, cv, pr + 0.6f);
                // Scorch marks of the landing on the patio.
                for (int k = 0; k < 3; k++) {
                    double a = Math.PI / 6 + k * 2 * Math.PI / 3;
                    f.circle(0x40201810, cu + (float) Math.cos(a) * (R + 4), cv + (float) Math.sin(a) * (R + 4), 3.5f);
                }
                // The crop circle in a strip of wheat on the far side.
                float w0 = h[2] + pr - R + 4, w1 = L - 3;
                if (w1 - w0 > 30) {
                    f.rect(0xFFC8B068, w0, 3, w1, S - 3);
                    for (float u = w0 + 1; u < w1; u += 2) f.line(0x22604010, 0.5f, u, 3, u, S - 3);
                    float ku = (w0 + w1) / 2, kv = S / 2, kr = Math.min(w1 - w0, S - 6) * 0.42f;
                    f.circle(0xFFA89048, ku, kv, kr);
                    f.circle(0xFFC8B068, ku, kv, kr * 0.82f);
                    f.circle(0xFFA89048, ku, kv, kr * 0.62f);
                    f.circle(0xFFC8B068, ku, kv, kr * 0.3f);
                    for (int k = 0; k < 3; k++) {
                        double a = -Math.PI / 2 + k * 2 * Math.PI / 3;
                        float su = ku + (float) Math.cos(a) * kr * 0.46f, sv = kv + (float) Math.sin(a) * kr * 0.46f;
                        f.circle(0xFFA89048, su, sv, kr * 0.12f);
                        f.line(0xFFA89048, 1.4f, ku + (float) Math.cos(a) * kr * 0.62f, kv + (float) Math.sin(a) * kr * 0.62f,
                                ku + (float) Math.cos(a) * kr * 1.15f, kv + (float) Math.sin(a) * kr * 1.15f);
                        f.circle(0xFFA89048, ku + (float) Math.cos(a) * kr * 1.15f, kv + (float) Math.sin(a) * kr * 1.15f, kr * 0.08f);
                    }
                    f.frame(0xFF8A7A50, 0.6f, w0, 3, w1, S - 3);
                }
                // The little green man on a plinth, pointing at the sky.
                float au = cu + pr * 0.75f, av = cv + pr * 0.8f;
                f.rect(0x40000000, au - 2, av - 1, au + 5, av + 6);
                f.rect(0xFF9A9488, au - 3, av - 3, au + 3, av + 3);
                f.circle(0xFF5AB050, au, av, 1.5f);
                f.oval(0xFF6CC860, au - 1.6f, av - 2.8f, au + 1.6f, av - 0.6f);
                f.circle(0xFF101010, au - 0.6f, av - 1.8f, 0.4f);
                f.circle(0xFF101010, au + 0.6f, av - 1.8f, 0.4f);
                // Neon lamp posts, a fifties car and a sign.
                for (int k = 0; k < 4; k++) {
                    double a = Math.PI / 4 + k * Math.PI / 2;
                    float lu = cu + (float) Math.cos(a) * (pr + 5), lv = cv + (float) Math.sin(a) * (pr + 5);
                    f.circle(0x30000000, lu + 1, lv + 1, 1.8f);
                    f.circle(0xFF3A3C40, lu, lv, 1.5f);
                    f.circle(k % 2 == 0 ? 0xFF7AE0E8 : 0xFFF07AB8, lu, lv, 0.9f);
                }
                car(f, cu + 2, S - 5, true, 0xFF70C8C0);
                if (h[1] > 14) car(f, cu - 4, 6, true, 0xFFE07890);
                f.tree(4, 4, 5, 0xFF4A7E3C);
                f.tree(4, S - 4, 5, 0xFF4A7E3C);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, cu = L / 2, cv = S / 2, R = Math.min(L, S) / 2 + 3;
                // The low kitchen block under the disc.
                f.rect(0xFF4A4C54, 0, 0, L, S);
                f.rect(0xFF5E6068, 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                // The saucer: rim, panelled skirt, a ring of lights, the raised deck and the glass dome.
                f.circle(0x50000000, cu + 3, cv + 3.5f, R);
                f.circle(0xFF7E848C, cu, cv, R);
                int n = 24;
                for (int k = 0; k < n; k++) {
                    double a0 = k * 2 * Math.PI / n, a1 = (k + 1) * 2 * Math.PI / n, am = (a0 + a1) / 2;
                    float s = 1f + 0.32f * (float) Math.cos(am + 3 * Math.PI / 4);
                    float r0 = R - 1;
                    f.poly(Landmarks.shade(0xFFB0B6BE, s), cu, cv, cu + (float) Math.cos(a0) * r0, cv + (float) Math.sin(a0) * r0,
                            cu + (float) Math.cos(a1) * r0, cv + (float) Math.sin(a1) * r0);
                }
                for (int k = 0; k < n; k++) {
                    double a = k * 2 * Math.PI / n;
                    f.line(0x30000000, 0.5f, cu + (float) Math.cos(a) * R * 0.55f, cv + (float) Math.sin(a) * R * 0.55f,
                            cu + (float) Math.cos(a) * (R - 1), cv + (float) Math.sin(a) * (R - 1));
                }
                for (int k = 0; k < 16; k++) {
                    double a = k * Math.PI / 8;
                    float lu = cu + (float) Math.cos(a) * (R - 2.6f), lv = cv + (float) Math.sin(a) * (R - 2.6f);
                    f.circle(k % 2 == 0 ? 0xFFF0E070 : 0xFF80E8F0, lu, lv, 1.1f);
                    f.circle(0xC0FFFFFF, lu - 0.3f, lv - 0.3f, 0.4f);
                }
                f.ring(0xFF5A6068, 1f, cu, cv, R * 0.56f);
                dome(f, cu, cv, R * 0.54f, 0xFFC8CED6, 16);
                f.circle(0x40000000, cu + 1.5f, cv + 1.8f, R * 0.32f);
                f.circle(0xFF3A6A88, cu, cv, R * 0.32f);
                f.circle(0xFF5A90B0, cu - R * 0.04f, cv - R * 0.04f, R * 0.26f);
                f.oval(0xA0D8F0FF, cu - R * 0.24f, cv - R * 0.24f, cu - R * 0.02f, cv - R * 0.1f);
                f.circle(0xFFE04040, cu + R * 0.12f, cv + R * 0.12f, 0.9f);
                f.sign(city, "UFO DINER", cu, cv + R * 0.75f, 4, 0xFFE0FFF0);
            }
        };
        d.minS = 10;
        d.minL = 12;
        d.ground = City.GRASS;
        d.base = City.SHOP;
        d.inside = Variants.I_RESTAURANT;
        d.roofCol = 0xFFB0B6BE;
        d.districts = Variants.SUB | Variants.MID | Variants.IND;
        out.add(d);
    }

    // ================================================================== 5 Pyramid Building

    static void pyramid(List<Landmarks.Def> out) {
        // A glass pyramid office: four faceted faces of diamond-gridded glazing catching the light, set in a granite
        // court with triangular reflecting pools, fountain jets and three little pyramid skylights.
        Landmarks.Def d = new Landmarks.Def("Pyramid Building", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int s = Math.min(S - 4, 10);
                return new int[][]{at(al, (L - s) / 2, (S - s) / 2, s, s, 6)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] h = bx(f, pl(this, x0, y0, x1, y1)[0]);
                f.rect(0xFFC8C4BC, 0, 0, L, S);
                for (float u = 0; u < L; u += 8) f.line(0x18000000, 0.5f, u, 0, u, S);
                for (float v = 0; v < S; v += 8) f.line(0x14000000, 0.5f, 0, v, L, v);
                f.frame(0xFF9A968E, 2f, h[0] - 4, h[1] - 4, h[2] + 4, h[3] + 4);
                float cu = (h[0] + h[2]) / 2, cv = (h[1] + h[3]) / 2;
                // Triangular reflecting pools at the ends with fountain jets.
                for (int s = -1; s <= 1; s += 2) {
                    float e = s < 0 ? h[0] - 8 : h[2] + 8, far = s < 0 ? Math.max(6, e - 46) : Math.min(L - 6, e + 46);
                    if (Math.abs(far - e) < 18) continue;
                    float hw0 = Math.min(S / 2 - 5, 24);
                    f.poly(0xFF8A867E, e, cv - hw0, e, cv + hw0, far, cv);
                    float i = 2.2f;
                    f.poly(0xFF3E7690, e - s * i, cv - hw0 + i * 2.4f, e - s * i, cv + hw0 - i * 2.4f, far + s * i * 2.6f, cv);
                    f.poly(0xFF4E88A2, e - s * i, cv - hw0 + i * 2.4f, e - s * i, cv, far + s * i * 2.6f, cv);
                    float mu = (e + far) / 2;
                    for (int k = -2; k <= 2; k++) {
                        float ju = e + (far - e) * 0.2f, jv = cv + k * hw0 * 0.22f;
                        f.circle(0x70FFFFFF, ju, jv, 1.6f);
                        f.circle(0xD0FFFFFF, ju, jv, 0.7f);
                    }
                    // A round fountain beyond the pool where the court runs on.
                    float rest = s < 0 ? far : L - far;
                    if (rest > 40) {
                        float fu = s < 0 ? far / 2 : (far + L) / 2, fr = Math.min(16, rest * 0.3f);
                        f.circle(0xFF8A867E, fu, cv, fr + 2);
                        f.circle(0xFF3E7690, fu, cv, fr);
                        f.circle(0xFF4E88A2, fu - 1, cv - 1, fr * 0.8f);
                        f.circle(0xFF9A968E, fu, cv, fr * 0.3f);
                        for (int k = 0; k < 10; k++) {
                            double a = k * Math.PI / 5;
                            f.circle(0x90FFFFFF, fu + (float) Math.cos(a) * fr * 0.55f, cv + (float) Math.sin(a) * fr * 0.55f, 0.9f);
                        }
                        for (int k = 0; k < 4; k++) {
                            double a = Math.PI / 4 + k * Math.PI / 2;
                            f.bench(fu + (float) Math.cos(a) * (fr + 7), cv + (float) Math.sin(a) * (fr + 7), k % 2 == 0);
                        }
                    }
                    // A small pyramid skylight in the pool.
                    float pr = Math.min(7, Math.abs(far - e) * 0.12f + 3);
                    f.poly(0x40000000, mu, cv - pr + 2, mu + pr + 2, cv + 2, mu, cv + pr + 2, mu - pr + 2, cv + 2);
                    f.poly(0xFFB8D4E0, mu - pr, cv, mu, cv - pr, mu, cv);
                    f.poly(0xFF98B8C8, mu, cv - pr, mu + pr, cv, mu, cv);
                    f.poly(0xFF6E92A6, mu + pr, cv, mu, cv + pr, mu, cv);
                    f.poly(0xFF88AAC0, mu, cv + pr, mu - pr, cv, mu, cv);
                }
                // Clipped trees in square planters along the long sides, benches between.
                for (float u = 8; u < L - 6; u += 22) {
                    for (int s = 0; s < 2; s++) {
                        float v = s == 0 ? 5 : S - 5;
                        if (v > h[1] - 6 && v < h[3] + 6 && u > h[0] - 6 && u < h[2] + 6) continue;
                        f.rect(0xFF8A867E, u - 4, v - 4, u + 4, v + 4);
                        f.tree(u, v, 4, 0xFF4E8040);
                    }
                }
                f.lamp(h[0] - 7, h[1] - 7);
                f.lamp(h[2] + 7, h[1] - 7);
                f.lamp(h[0] - 7, h[3] + 7);
                f.lamp(h[2] + 7, h[3] + 7);
                for (int k = 0; k < 5; k++) person(f, 10 + k * (L - 20) / 4f, h[3] + 6 + (k % 2) * 3, CLOTHES[k]);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, cu = L / 2, cv = S / 2;
                f.rect(0xFF4A5058, 0, 0, L, S);
                float a = 1;
                // The four glass faces.
                int top = 0xFFA8C8DA, left = 0xFF8EB0C6, right = 0xFF5E8098, bot = 0xFF4A6A82;
                f.poly(top, a, a, L - a, a, cu, cv);
                f.poly(left, a, a, a, S - a, cu, cv);
                f.poly(right, L - a, a, L - a, S - a, cu, cv);
                f.poly(bot, a, S - a, L - a, S - a, cu, cv);
                // Sky reflections streaked across the upper faces.
                f.poly(0x40FFFFFF, a + 6, a, a + 14, a, cu - 2, cv - 2, cu - 5, cv - 5);
                f.poly(0x30FFFFFF, a, a + 10, a, a + 15, cu - 3, cv - 1, cu - 5, cv - 3);
                // Mullions: courses parallel to the base and ribs up to the apex.
                int n = 9;
                for (int k = 1; k < n; k++) {
                    float t = k / (float) n;
                    f.frame(0x50203038, 0.5f, a + (cu - a) * t, a + (cv - a) * t, L - a - (cu - a) * t, S - a - (cv - a) * t);
                }
                for (int k = 1; k < n; k++) {
                    float t = k / (float) n;
                    f.line(0x40203038, 0.4f, a + (L - 2 * a) * t, a, cu, cv);
                    f.line(0x40203038, 0.4f, a + (L - 2 * a) * t, S - a, cu, cv);
                    f.line(0x40203038, 0.4f, a, a + (S - 2 * a) * t, cu, cv);
                    f.line(0x40203038, 0.4f, L - a, a + (S - 2 * a) * t, cu, cv);
                }
                // The hip ribs, highlit on the lit side.
                f.line(0xFFE0ECF2, 1f, a, a, cu, cv);
                f.line(0xFFB8C8D2, 0.9f, L - a, a, cu, cv);
                f.line(0xFFB8C8D2, 0.9f, a, S - a, cu, cv);
                f.line(0xFF2E3A44, 0.9f, L - a, S - a, cu, cv);
                f.circle(0xFFE8E0C8, cu, cv, 1.2f);
                f.frame(0xFF3A4048, 1f, 0.5f, 0.5f, L - 0.5f, S - 0.5f);
            }
        };
        d.minS = 10;
        d.minL = 12;
        d.ground = City.PLAZA;
        d.base = City.OFFICE;
        d.inside = Variants.I_OFFICE;
        d.roofCol = 0xFF8EB0C6;
        d.districts = Variants.DOWN | Variants.MID;
        out.add(d);
    }

    // ================================================================== 6 Geodesic Homes

    /** A geodesic dome from above: rings of triangular panels shaded for light from the top left, a few glazed. */
    static void geoDome(F f, float u, float v, float R, int col, int glass) {
        f.circle(0x48000000, u + R * 0.25f, v + R * 0.3f, R);
        int rings = 4;
        for (int j = 0; j < rings; j++) {
            float r0 = R * (rings - j) / rings, r1 = R * (rings - j - 1) / rings;
            int n0 = 5 * (rings - j), n1 = Math.max(1, 5 * (rings - j - 1));
            for (int k = 0; k < n0; k++) {
                double a0 = k * 2 * Math.PI / n0, a1 = (k + 1) * 2 * Math.PI / n0;
                double b = (k + 0.5) * 2 * Math.PI / n0;
                float pu = u + (float) Math.cos(b) * r1, pv = v + (float) Math.sin(b) * r1;
                if (n1 == 1) {
                    pu = u;
                    pv = v;
                }
                float sh = 1f + (0.34f - j * 0.06f) * (float) Math.cos(b + 3 * Math.PI / 4);
                int cc = (k * 7 + j * 3) % 11 == 0 ? glass : col;
                f.poly(Landmarks.shade(cc, sh), u + (float) Math.cos(a0) * r0, v + (float) Math.sin(a0) * r0,
                        u + (float) Math.cos(a1) * r0, v + (float) Math.sin(a1) * r0, pu, pv);
                if (n1 > 1) {
                    double c1 = (k + 1.5) * 2 * Math.PI / n0;
                    float qu = u + (float) Math.cos(c1) * r1, qv = v + (float) Math.sin(c1) * r1;
                    float sh2 = 1f + (0.3f - j * 0.06f) * (float) Math.cos(a1 + 3 * Math.PI / 4);
                    f.poly(Landmarks.shade(col, sh2 * 1.04f), u + (float) Math.cos(a1) * r0, v + (float) Math.sin(a1) * r0, pu, pv, qu, qv);
                }
            }
        }
        // Struts: the ring lines and a few spokes.
        for (int j = 0; j < rings; j++) f.ring(0x50F0F0F0, 0.4f, u, v, R * (rings - j) / (float) rings);
        f.circle(0xFFE8E4D8, u, v, R * 0.08f + 0.4f);
    }

    static void geodesic(List<Landmarks.Def> out) {
        // A little commune of geodesic dome houses, white panelled and dotted with skylights, among curving paths,
        // round vegetable beds, a shared solar array and a swale pond.
        Landmarks.Def d = new Landmarks.Def("Geodesic Homes", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                List<int[]> l = new java.util.ArrayList<int[]>();
                for (int u = 1; u + 3 <= L - 1; u += 5) l.add(at(al, u, 1, 3, 3, 1));
                for (int u = 3; u + 3 <= L - 1; u += 5) l.add(at(al, u, S - 4, 3, 3, 1));
                return l.toArray(new int[0][]);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] q = pl(this, x0, y0, x1, y1);
                f.rect(0xFF74A058, 0, 0, L, S);
                for (int k = 0; k < 40; k++) f.circle(0x12000000, r.nextFloat() * L, r.nextFloat() * S, 2 + r.nextFloat() * 4);
                // A winding gravel path down the middle with spurs to every dome.
                float mv = S / 2;
                int n = 24;
                float[] pth = new float[(n + 1) * 2];
                for (int k = 0; k <= n; k++) {
                    pth[2 * k] = L * k / n;
                    pth[2 * k + 1] = mv + (float) Math.sin(k * 0.7f) * Math.min(6, S * 0.08f);
                }
                f.stroke(0xFFB0A488, 7, pth, false);
                f.stroke(0xFFC8BCA0, 5, pth, false);
                for (int[] b : q) {
                    float[] h = bx(f, b);
                    float bu = (h[0] + h[2]) / 2, bv = (h[1] + h[3]) / 2;
                    f.rline(0xFFC8BCA0, 3.5f, bu, bv, bu, mv);
                    // A round deck round each dome.
                    f.circle(0xFF9A7A58, bu, bv, (h[2] - h[0]) / 2 + 3);
                    f.circle(0xFFB08C66, bu, bv, (h[2] - h[0]) / 2 + 2);
                }
                // Round raised vegetable beds and a small pond along the path.
                int k = 0;
                for (float u = 10; u < L - 8; u += 26, k++) {
                    float v = mv + (k % 2 == 0 ? -10 : 10);
                    if (k % 3 == 2) {
                        pond(c, p, f.x(u - 7, v - 4), f.y(u - 7, v - 4), f.x(u + 7, v + 4), f.y(u + 7, v + 4));
                        continue;
                    }
                    f.circle(0x30000000, u + 1, v + 1, 5.5f);
                    f.circle(0xFF8A6A48, u, v, 5.5f);
                    f.circle(0xFF5A4030, u, v, 4.5f);
                    for (int j = 0; j < 7; j++) {
                        double a = j * 2 * Math.PI / 7;
                        f.circle(j % 2 == 0 ? 0xFF6AA850 : 0xFF88C060, u + (float) Math.cos(a) * 2.6f, v + (float) Math.sin(a) * 2.6f, 1.2f);
                    }
                    f.circle(0xFFD86040, u, v, 1);
                }
                // A shared solar array at the end of the path.
                float su = L - 12, sv = mv - 5;
                for (int j = 0; j < 3; j++) {
                    f.rect(0x40000000, su - 6 + 1.5f, sv - 6 + j * 4.5f + 1.5f, su + 6 + 1.5f, sv - 3 + j * 4.5f + 1.5f);
                    f.rect(0xFF2E4A6E, su - 6, sv - 6 + j * 4.5f, su + 6, sv - 3 + j * 4.5f);
                    for (float u = su - 6; u < su + 6; u += 3) f.line(0x60A0C0E0, 0.3f, u, sv - 6 + j * 4.5f, u, sv - 3 + j * 4.5f);
                }
                // Fruit trees round the edge.
                for (float u = 6; u < L - 4; u += 18) {
                    f.tree(u + 4, mv + (u % 36 < 18 ? -20 : 20), 4.5f, 0xFF5A8E44);
                }
                f.bench(L * 0.3f, mv + 5, true);
                f.lamp(L * 0.5f, mv - 5);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[] cols = {0xFFE4E0D6, 0xFFD8D0BC, 0xFFC8D0C8, 0xFFE0D4C4};
                geoDome(f, L / 2, S / 2, Math.min(L, S) / 2 + 1, cols[part % 4], 0xFF7AA8C4);
                // A round entrance porch and a stove flue.
                f.circle(0x40000000, L / 2 + S * 0.3f + 1, S / 2 + S * 0.3f + 1, 1.3f);
                f.circle(0xFF5A5E64, L / 2 + S * 0.3f, S / 2 + S * 0.3f, 1.3f);
                f.circle(0xFF1A1A1E, L / 2 + S * 0.3f, S / 2 + S * 0.3f, 0.6f);
            }
        };
        d.minS = 10;
        d.minL = 14;
        d.ground = City.GRASS;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFFE4E0D6;
        d.districts = Variants.SUB | Variants.PARK | Variants.UNI;
        out.add(d);
    }

    // ================================================================== 7 Giant Statue Park

    static void colossus(List<Landmarks.Def> out) {
        // A colossal patinated bronze figure with its arms flung wide, standing on a stone pedestal (a museum inside),
        // its cross-shaped shadow lying long across a star of paths, lawns and visitors craning up.
        Landmarks.Def d = new Landmarks.Def("Giant Statue Park", T_MONUMENT) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int s = S >= 12 ? 4 : 3;
                return new int[][]{at(al, (L - s) / 2, (S - s) / 2, s, s, 4)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] h = bx(f, pl(this, x0, y0, x1, y1)[0]);
                float cu = (h[0] + h[2]) / 2, cv = (h[1] + h[3]) / 2, ps = (h[2] - h[0]) / 2;
                f.rect(0xFF6A9A50, 0, 0, L, S);
                for (int k = 0; k < 30; k++) f.circle(0x12000000, r.nextFloat() * L, r.nextFloat() * S, 3 + r.nextFloat() * 3);
                // A star of paths from the corners and sides to the plaza.
                float[][] ends = {{0, 0}, {L, 0}, {0, S}, {L, S}, {cu, 0}, {cu, S}, {0, cv}, {L, cv}};
                for (float[] e : ends) f.rline(0xFFD0C6AE, 7, e[0], e[1], cu, cv);
                f.circle(0xFFB8AE96, cu, cv, ps + 15);
                f.circle(0xFFD8CEB6, cu, cv, ps + 13.5f);
                for (int k = 0; k < 16; k++) {
                    double a = k * Math.PI / 8;
                    f.line(0x20000000, 0.5f, cu + (float) Math.cos(a) * (ps + 3), cv + (float) Math.sin(a) * (ps + 3),
                            cu + (float) Math.cos(a) * (ps + 13.5f), cv + (float) Math.sin(a) * (ps + 13.5f));
                }
                f.ring(0x30000000, 0.5f, cu, cv, ps + 8);
                // Steps up to the pedestal.
                for (int k = 0; k < 3; k++) f.frame(k % 2 == 0 ? 0xFFC0B69E : 0xFFCCC2AA, 1.2f, h[0] - 1.6f - k * 1.2f, h[1] - 1.6f - k * 1.2f, h[2] + 1.6f + k * 1.2f, h[3] + 1.6f + k * 1.2f);
                // The figure's long shadow: body, head and outstretched arms cast to the lower right.
                float sl = 3.2f * ps, sd = sl * 0.7071f, aw = ps + 26;
                float bu = cu + sd, bv = cv + sd;
                f.rline(0x34000000, ps * 0.9f, cu + ps * 0.5f, cv + ps * 0.5f, bu, bv);
                f.circle(0x34000000, bu + 3, bv + 3, ps * 0.42f);
                float au = cu + sd * 0.72f, av = cv + sd * 0.72f;
                f.rline(0x34000000, ps * 0.32f, au - aw * 0.7071f, av + aw * 0.7071f, au + aw * 0.7071f, av - aw * 0.7071f);
                // Lawns' flower beds, trees round the ring, benches and visitors looking up.
                for (int k = 0; k < 4; k++) {
                    double a = Math.PI / 8 + k * Math.PI / 2 + Math.PI / 4 * (k % 2);
                    float du = (float) Math.cos(a), dv = (float) Math.sin(a), rr = ps + 24;
                    float tu = cu + du * rr, tv = cv + dv * rr;
                    if (tu > 6 && tu < L - 6 && tv > 6 && tv < S - 6) f.tree(tu, tv, 5.5f, 0xFF4A7E3C);
                }
                for (float u = 8; u < L - 6; u += 30) {
                    if (Math.abs(u - cu) < ps + 20) continue;
                    f.tree(u, 7, 5.5f, 0xFF3E7034);
                    f.tree(u + 12, S - 7, 5.5f, 0xFF4A7E3C);
                }
                double ad = Math.atan2(cv, cu);
                for (int k = 0; k < 8; k++) {
                    double a = (k % 2 == 0 ? ad / 2 : (ad + Math.PI / 2) / 2);
                    float du = (float) Math.cos(a) * ((k / 2) % 2 == 0 ? 1 : -1), dv = (float) Math.sin(a) * (k / 4 == 0 ? 1 : -1);
                    float rr = ps + 26, fu = cu + du * rr, fv = cv + dv * rr;
                    if (fu > 8 && fu < L - 8 && fv > 6 && fv < S - 6) flowers(f, fu - 5, fv - 3.5f, fu + 5, fv + 3.5f, 0xFFE06070, 0xFFF0E070);
                }
                for (int k = 0; k < 4; k++) {
                    double a = Math.PI / 4 + k * Math.PI / 2;
                    f.bench(cu + (float) Math.cos(a) * (ps + 11), cv + (float) Math.sin(a) * (ps + 11), k % 2 == 0);
                }
                for (int k = 0; k < 9; k++) {
                    double a = k * 0.7f + 0.3f;
                    float rr = ps + 5 + (k % 3) * 2.5f;
                    person(f, cu + (float) Math.cos(a) * rr, cv + (float) Math.sin(a) * rr, CLOTHES[k % 8]);
                }
                // A souvenir kiosk by the main path.
                float ku = cu - ps - 22, kv = cv + 10;
                if (ku > 8) {
                    f.rect(0x40000000, ku - 4, kv - 3, ku + 6, kv + 5);
                    f.rect(0xFF7A5A3A, ku - 5, kv - 4, ku + 5, kv + 4);
                    gable(f, 0xFF4E7A6A, ku - 5, kv - 4, ku + 5, kv + 4);
                }
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, cu = L / 2, cv = S / 2, s = Math.min(L, S);
                // The stepped granite pedestal.
                f.rect(0xFF8E887C, 0, 0, L, S);
                f.rect(0xFFA8A294, 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                f.rect(0xFFB8B2A4, 4, 4, L - 4, S - 4);
                f.line(0x30000000, 0.6f, 4, S - 4, L - 4, S - 4);
                f.line(0x30000000, 0.6f, L - 4, 4, L - 4, S - 4);
                int bronze = 0xFF5E9A86, dk = 0xFF3E7266, lt = 0xFF86C2AC;
                // Robes spreading at the feet, seen past the shoulders.
                f.oval(0x40000000, cu - s * 0.3f + 2, cv - s * 0.4f + 2, cu + s * 0.3f + 2, cv + s * 0.42f + 2);
                f.oval(dk, cu - s * 0.32f, cv - s * 0.42f, cu + s * 0.32f, cv + s * 0.42f);
                f.oval(bronze, cu - s * 0.28f, cv - s * 0.38f, cu + s * 0.26f, cv + s * 0.36f);
                for (int k = -2; k <= 2; k++) f.line(0x30183028, 0.6f, cu + k * s * 0.07f, cv - s * 0.34f, cu + k * s * 0.1f, cv + s * 0.38f);
                // Arms flung out wide past the pedestal, sleeves hanging.
                float aw = s / 2 + 24, at = s * 0.09f + 1.6f;
                f.rline(0x40000000, at * 2.2f, cu - aw + 3, cv + 3, cu + aw + 3, cv + 3);
                f.rline(dk, at * 2.2f, cu - aw, cv, cu + aw, cv);
                f.rline(bronze, at * 1.5f, cu - aw + 1, cv - at * 0.3f, cu + aw - 1, cv - at * 0.3f);
                f.line(lt, 0.7f, cu - aw + 2, cv - at * 0.75f, cu + aw - 2, cv - at * 0.75f);
                f.poly(dk, cu - aw * 0.75f, cv, cu - aw * 0.35f, cv, cu - aw * 0.45f, cv + at * 2.3f);
                f.poly(dk, cu + aw * 0.35f, cv, cu + aw * 0.75f, cv, cu + aw * 0.45f, cv + at * 2.3f);
                for (int sgn = -1; sgn <= 1; sgn += 2) {
                    f.circle(dk, cu + sgn * aw, cv, at * 1.05f);
                    f.circle(bronze, cu + sgn * aw - 0.3f, cv - 0.3f, at * 0.8f);
                    for (int k = -1; k <= 1; k++) f.line(dk, 0.5f, cu + sgn * aw, cv + k * at * 0.4f, cu + sgn * (aw + at * 1.1f), cv + k * at * 0.5f);
                }
                // Shoulders and the head, hair in coils, a laurel ring.
                f.oval(dk, cu - s * 0.28f, cv - s * 0.16f, cu + s * 0.28f, cv + s * 0.16f);
                f.oval(bronze, cu - s * 0.26f, cv - s * 0.15f, cu + s * 0.24f, cv + s * 0.12f);
                f.oval(lt, cu - s * 0.22f, cv - s * 0.13f, cu - s * 0.04f, cv - s * 0.06f);
                float hr = s * 0.15f;
                f.circle(0x50000000, cu + 1.2f, cv + 1.5f, hr);
                f.circle(dk, cu, cv, hr);
                f.circle(bronze, cu - 0.4f, cv - 0.4f, hr * 0.85f);
                for (int k = 0; k < 10; k++) {
                    double a = k * Math.PI / 5;
                    f.circle(k % 2 == 0 ? 0xFF4E8A76 : 0xFF6CAA94, cu + (float) Math.cos(a) * hr * 0.55f, cv + (float) Math.sin(a) * hr * 0.55f, hr * 0.22f);
                }
                f.ring(0xFFB8A050, 0.7f, cu, cv, hr * 0.85f);
                f.circle(lt, cu - hr * 0.35f, cv - hr * 0.35f, hr * 0.25f);
            }
        };
        d.minS = 10;
        d.minL = 12;
        d.ground = City.GRASS;
        d.base = City.OFFICE;
        d.inside = Variants.I_SHOWROOM;
        d.roofCol = 0xFFA8A294;
        d.districts = Variants.PARK | Variants.DOWN | Variants.OLD | Variants.MID;
        out.add(d);
    }

    // ================================================================== 8 Ice Cream Parlour

    /** A café table with two chairs under a parasol. */
    static void cafeTable(F f, float u, float v, int c1, int c2) {
        f.rect(0xFFB8B0A0, u - 3.8f, v - 1, u - 2.2f, v + 1);
        f.rect(0xFFB8B0A0, u + 2.2f, v - 1, u + 3.8f, v + 1);
        parasol(f, u, v, 4.2f, c1, c2);
    }

    static void iceCream(List<Landmarks.Def> out) {
        // A pastel parlour with a scalloped mint awning and a giant three-scoop cone on its roof, a pink and cream
        // chequered patio of parasol tables, an ice cream cart, planters and a queue of children at the hatch.
        Landmarks.Def d = new Landmarks.Def("Ice Cream Parlour", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int lu = Math.min(7, L - 6), lv = Math.min(5, S - 5);
                return new int[][]{at(al, 2, 2, lu, lv, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] h = bx(f, pl(this, x0, y0, x1, y1)[0]);
                f.rect(0xFF78A45C, 0, 0, L, S);
                // The chequered patio in front and to the side.
                float pu1 = L - 4, pv1 = S - 4;
                for (float u = h[0] - 4; u < pu1; u += 6)
                    for (float v = h[3]; v < pv1; v += 6)
                        f.rect(((int) ((u - h[0]) / 6 + 100) + (int) ((v - h[3]) / 6)) % 2 == 0 ? 0xFFF0D4D8 : 0xFFF4ECDC, u, v, Math.min(u + 6, pu1), Math.min(v + 6, pv1));
                for (float u = h[2]; u < pu1; u += 6)
                    for (float v = h[1] - 4; v < h[3]; v += 6)
                        f.rect(((int) ((u - h[2]) / 6) + (int) ((v - h[1]) / 6 + 100)) % 2 == 0 ? 0xFFF4ECDC : 0xFFF0D4D8, u, v, Math.min(u + 6, pu1), Math.min(v + 6, h[3]));
                f.frame(0xFFC8A8B0, 1f, h[0] - 4, h[1] - 4, pu1, pv1);
                // Low hedges in pastel planters round the patio edge.
                for (float u = h[0] - 2; u < pu1 - 3; u += 9) {
                    f.rect(0xFFB8E0D0, u, pv1 - 2.5f, u + 7, pv1 - 0.5f);
                    f.bush(u + 3.5f, pv1 - 1.5f, 1.8f);
                }
                // Tables under pink, mint and lemon parasols.
                int[] pc = {0xFFF0A0B4, 0xFFA0DCC8, 0xFFF4E09A};
                int k = 0;
                for (float u = h[0] + 8; u < pu1 - 6; u += 17)
                    for (float v = h[3] + 10; v < pv1 - 6; v += 15, k++) {
                        if (u < h[0] + 24 && v < h[3] + 14) continue;
                        cafeTable(f, u, v, pc[k % 3], 0xFFFAF6EE);
                    }
                for (float u = h[2] + 10; u < pu1 - 6; u += 17)
                    for (float v = h[1] + 6; v < h[3] - 4; v += 15, k++) cafeTable(f, u, v, pc[k % 3], 0xFFFAF6EE);
                // The queue at the hatch.
                for (int j = 0; j < 5; j++) person(f, h[0] + 8 + j * 4.5f, h[3] + 4 + (j % 2), CLOTHES[(j + 2) % 8]);
                // An ice cream cart with a striped umbrella.
                float cu = Math.min(pu1 - 10, h[2] + 16), cv = pv1 + 0.5f;
                if (cu > h[2] + 4) {
                    f.rect(0x40000000, cu - 5, S - 2.6f, cu + 7, S - 0.6f);
                    f.rect(0xFFF4ECDC, cu - 6, S - 4, cu + 6, S - 1);
                    f.rect(0xFFA0DCC8, cu - 6, S - 4, cu + 6, S - 3);
                    f.circle(0xFF3A3A40, cu - 4, S - 0.8f, 0.9f);
                    f.circle(0xFF3A3A40, cu + 4, S - 0.8f, 0.9f);
                }
                // Trees beyond the patio.
                f.tree(L - 6, 6, 5.5f, 0xFF4A7E3C);
                f.tree(h[0] - 1, S - 1, 4.5f, 0xFF4A7E3C);
                f.lamp(pu1 - 2, h[1] - 2);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                // A pink parapet round a cream deck, a scalloped mint and white awning along the front.
                f.rect(0xFFD88A9C, 0, 0, L, S);
                f.rect(0xFFF2E8DA, 1.5f, 1.5f, L - 1.5f, S - 6);
                float w = 4;
                for (float u = 0; u < L - 0.1f; u += w) {
                    int i = (int) (u / w);
                    f.rect(i % 2 == 0 ? 0xFF9AD8C4 : 0xFFF8F4EC, u, S - 6, Math.min(u + w, L), S - 1);
                    f.circle(i % 2 == 0 ? 0xFF9AD8C4 : 0xFFF8F4EC, u + w / 2, S - 1, w / 2);
                }
                f.line(0x30000000, 0.6f, 0, S - 6, L, S - 6);
                f.rect(0xFFB0B4B8, 3, 3, 8, 7);
                f.rect(0xFF9094A0, 3.6f, 3.6f, 7.4f, 6.4f);
                // The giant cone lying across the roof: waffle cone, three scoops, drips and a cherry.
                float cu = L * 0.5f, cv = (S - 6) * 0.5f + 1, sc = Math.min(L, S) / 80f * 1.0f + 0.25f;
                float tip = cu + 30 * sc, base = cu - 2 * sc, hw = 7.5f * sc;
                f.poly(0x40000000, base + 3, cv - hw + 3, base + 3, cv + hw + 3, tip + 3, cv + 3);
                f.poly(0xFFD4A060, base, cv - hw, base, cv + hw, tip, cv);
                f.poly(0xFFE4B878, base, cv - hw, base, cv, tip, cv);
                for (int k = 1; k < 6; k++) {
                    float t = k / 6f, uu = base + (tip - base) * t;
                    f.line(0x50805020, 0.6f, uu, cv - hw * (1 - t), base + (tip - base) * Math.min(1, t + 0.35f), cv + hw * (1 - Math.min(1, t + 0.35f)));
                    f.line(0x50805020, 0.6f, uu, cv + hw * (1 - t), base + (tip - base) * Math.min(1, t + 0.35f), cv - hw * (1 - Math.min(1, t + 0.35f)));
                }
                int[] sc3 = {0xFFF2A8BC, 0xFFA8E0C4, 0xFFF6EED8};
                float[] su = {base - 4 * sc, base - 12 * sc, base - 20 * sc};
                for (int k = 2; k >= 0; k--) {
                    float rr = (8.5f - k * 0.6f) * sc;
                    f.circle(0x40000000, su[k] + 2, cv + 2.5f, rr);
                    f.circle(City.darken(sc3[k], 0.85f), su[k], cv, rr);
                    f.circle(sc3[k], su[k] - rr * 0.1f, cv - rr * 0.12f, rr * 0.82f);
                    f.circle(City.lighten(sc3[k], 0.3f), su[k] - rr * 0.35f, cv - rr * 0.35f, rr * 0.3f);
                }
                for (int k = 0; k < 3; k++) f.roundRect(City.darken(sc3[0], 0.9f), base - 1 + k * 2.2f * sc, cv - hw * 0.9f + k * 4 * sc, base + 1.4f + k * 2.2f * sc, cv - hw * 0.9f + k * 4 * sc + 4 * sc, 1);
                float cy = su[2] - 7 * sc;
                f.circle(0x40000000, cy + 1, cv + 1.3f, 2.2f * sc);
                f.circle(0xFFC02830, cy, cv, 2.2f * sc);
                f.circle(0xFFF07070, cy - 0.6f * sc, cv - 0.6f * sc, 0.8f * sc);
                f.line(0xFF4E7A30, 0.6f, cy, cv, cy - 3 * sc, cv - 2.5f * sc);
                f.sign(city, "GELATO", L / 2, S - 3.5f, 4, 0xFF5A3A40);
            }
        };
        d.minS = 10;
        d.minL = 12;
        d.ground = City.GRASS;
        d.base = City.SHOP;
        d.inside = Variants.I_CAFE;
        d.roofCol = 0xFFF2E8DA;
        d.districts = Variants.SUB | Variants.MID | Variants.OLD | Variants.PARK;
        out.add(d);
    }

    // ================================================================== 9 Toy Store

    /** A toy brick from above: a coloured slab with a grid of studs. */
    static void brick(F f, float u0, float v0, float u1, float v1, int col, float up) {
        f.rect(0x48000000, u0 + up, v0 + up, u1 + up, v1 + up);
        f.rect(City.darken(col, 0.78f), u0, v0, u1, v1);
        f.rect(col, u0 + 0.6f, v0 + 0.6f, u1 - 0.8f, v1 - 0.8f);
        float st = 4.2f;
        for (float u = u0 + st / 2 + 0.2f; u < u1 - 1.5f; u += st)
            for (float v = v0 + st / 2 + 0.2f; v < v1 - 1.5f; v += st) {
                f.circle(City.darken(col, 0.72f), u + 0.35f, v + 0.35f, 1.4f);
                f.circle(City.lighten(col, 0.1f), u, v, 1.3f);
                f.circle(City.lighten(col, 0.35f), u - 0.45f, v - 0.45f, 0.45f);
            }
    }

    /** A letter block (a cube) from above: a cream top with a coloured square and its sides in shade. */
    static void letterBlock(F f, float u, float v, float s, int col) {
        f.rect(0x40000000, u + s * 0.25f, v + s * 0.3f, u + s * 1.25f, v + s * 1.3f);
        f.rect(0xFFD8CCB0, u, v, u + s, v + s);
        f.rect(0xFFF2E8D0, u, v, u + s * 0.9f, v + s * 0.9f);
        f.frame(col, Math.max(0.6f, s * 0.08f), u + s * 0.15f, v + s * 0.15f, u + s * 0.75f, v + s * 0.75f);
        f.rect(col, u + s * 0.32f, v + s * 0.32f, u + s * 0.58f, v + s * 0.58f);
    }

    static void toyStore(List<Landmarks.Def> out) {
        // A toy shop roofed like a pile of giant toy bricks in primary colours, a pinwheel spinning on the corner;
        // outside, giant letter blocks, a toy train running round its oval of track, hopscotch and balloons.
        Landmarks.Def d = new Landmarks.Def("Toy Store", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int lu = Math.min(L - 6, 10), lv = S - 5;
                return new int[][]{at(al, 1, 1, lu, lv, 2)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] h = bx(f, pl(this, x0, y0, x1, y1)[0]);
                f.rect(0xFFD4CCBC, 0, 0, L, S);
                for (float u = 0; u < L; u += 8) f.line(0x14000000, 0.5f, u, 0, u, S);
                for (float v = 0; v < S; v += 8) f.line(0x10000000, 0.5f, 0, v, L, v);
                // A rubber play surface beside the shop with a toy train on its track.
                float g0 = h[2] + 5, g1 = L - 4, gv0 = 5, gv1 = S - 5;
                if (g1 - g0 > 24) {
                    f.roundRect(0xFF5E9A6A, g0, gv0, g1, gv1, 6);
                    float tu0 = g0 + 5, tu1 = g1 - 5, tv0 = gv0 + 5, tv1 = gv1 - 5, rad = Math.min(tu1 - tu0, tv1 - tv0) / 2;
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(4);
                    p.setColor(0xFF8A6A48);
                    Landmarks.RF.set(f.x(tu0, tv0), f.y(tu0, tv0), f.x(tu1, tv1), f.y(tu1, tv1));
                    c.drawRoundRect(Landmarks.RF, rad, rad, p);
                    p.setStrokeWidth(0.7f);
                    p.setColor(0xFF9AA0A8);
                    Landmarks.RF.set(f.x(tu0 - 1.4f, tv0 - 1.4f), f.y(tu0 - 1.4f, tv0 - 1.4f), f.x(tu1 + 1.4f, tv1 + 1.4f), f.y(tu1 + 1.4f, tv1 + 1.4f));
                    c.drawRoundRect(Landmarks.RF, rad + 1.4f, rad + 1.4f, p);
                    Landmarks.RF.set(f.x(tu0 + 1.4f, tv0 + 1.4f), f.y(tu0 + 1.4f, tv0 + 1.4f), f.x(tu1 - 1.4f, tv1 - 1.4f), f.y(tu1 - 1.4f, tv1 - 1.4f));
                    c.drawRoundRect(Landmarks.RF, rad - 1.4f, rad - 1.4f, p);
                    p.setStyle(Paint.Style.FILL);
                    // The train: engine and three wagons along the near straight.
                    float tv = tv1;
                    int[] wc = {0xFFC83A30, 0xFF3A6AC0, 0xFFE8C030, 0xFF3E9A50};
                    for (int k = 0; k < 4; k++) {
                        float u = (tu0 + tu1) / 2 + 10 - k * 7;
                        f.rect(0x40000000, u - 2.5f, tv - 2, u + 3.8f, tv + 3.2f);
                        f.rect(wc[k], u - 3, tv - 2.4f, u + 3, tv + 2.4f);
                        if (k == 0) {
                            f.circle(0xFF2A2A2E, u + 1.5f, tv, 1.1f);
                            f.rect(0xFFF0E0C0, u - 2.5f, tv - 1.8f, u - 0.5f, tv + 1.8f);
                        } else f.rect(City.lighten(wc[k], 0.25f), u - 2.2f, tv - 1.6f, u + 2.2f, tv + 0.2f);
                    }
                    f.circle(0x50F0F0F0, (tu0 + tu1) / 2 + 15, tv - 2, 2);
                    f.circle(0x38F0F0F0, (tu0 + tu1) / 2 + 18, tv - 4, 2.6f);
                    // Letter blocks inside the oval.
                    float bu = (tu0 + tu1) / 2;
                    letterBlock(f, bu - 9, (tv0 + tv1) / 2 - 4, 7, 0xFFC83A30);
                    letterBlock(f, bu, (tv0 + tv1) / 2 - 5, 6, 0xFF3A6AC0);
                    letterBlock(f, bu - 3, (tv0 + tv1) / 2 + 1, 5, 0xFF3E9A50);
                }
                // Hopscotch painted on the forecourt.
                float hu = h[0] + 8, hv = h[3] + 6;
                for (int k = 0; k < 7; k++) {
                    float u = hu + k * 5;
                    if (k == 2 || k == 5) {
                        f.frame(0xFFF4F0E8, 0.7f, u, hv - 2.5f, u + 5, hv);
                        f.frame(0xFFF4F0E8, 0.7f, u, hv, u + 5, hv + 2.5f);
                    } else f.frame(0xFFF4F0E8, 0.7f, u, hv - 2.5f, u + 5, hv + 2.5f);
                }
                // Giant letter blocks tumbled across the forecourt.
                float fu = hu + 40;
                letterBlock(f, fu, h[3] + 4, 8, 0xFFE8A030);
                letterBlock(f, fu + 10, h[3] + 6, 7, 0xFF8A4AC0);
                // Balloons tied at the door.
                float du = (h[0] + h[2]) / 2;
                int[] bc = {0xFFE04848, 0xFF4890E0, 0xFFF0D040, 0xFF60C060, 0xFFE070C0};
                for (int k = 0; k < 5; k++) {
                    float u = du + 8 + (k % 3) * 3.2f - 3, v = h[3] + 3 + (k / 3) * 3.2f;
                    f.line(0x80FFFFFF, 0.3f, du + 8, h[3] + 9, u, v);
                    f.oval(0x30000000, u - 1, v, u + 3.5f, v + 3.4f);
                    f.oval(bc[k], u - 1.6f, v - 2, u + 1.6f, v + 1.8f);
                    f.circle(0xA0FFFFFF, u - 0.6f, v - 0.9f, 0.5f);
                }
                for (int k = 0; k < 5; k++) person(f, h[0] + 10 + k * 11, S - 4 - (k % 2) * 3, CLOTHES[(k * 3) % 8]);
                f.tree(4, S - 4, 4, 0xFF4A7E3C);
                f.lamp(du - 8, h[3] + 2);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF3A3E46, 0, 0, L, S);
                // Bricks laid in two courses, a few stacked a level higher.
                int[] cols = {0xFFC83A30, 0xFF3A6AC0, 0xFFE8C030, 0xFF3E9A50, 0xFFF0EAE0, 0xFFE07A30};
                float bw = 16.8f, half = S / 2;
                int k = 0;
                for (int row = 0; row < 2; row++) {
                    float v0 = row == 0 ? 0.5f : half, v1 = row == 0 ? half : S - 0.5f;
                    float off = row == 0 ? 0 : -bw / 2;
                    for (float u = 0.5f + off; u < L - 0.5f; u += bw, k++) {
                        float a = Math.max(0.5f, u), b = Math.min(L - 0.5f, u + bw);
                        if (b - a < 3) continue;
                        brick(f, a, v0, b, v1, cols[(k * 5 + row) % 6], 0);
                    }
                }
                brick(f, L * 0.18f, S * 0.18f, L * 0.18f + bw * 1.5f, S * 0.18f + 8.4f, 0xFF3A6AC0, 2.2f);
                brick(f, L * 0.58f, S * 0.52f, L * 0.58f + bw, S * 0.52f + 12.6f, 0xFFC83A30, 2.2f);
                brick(f, L * 0.6f, S * 0.6f, L * 0.6f + 8.4f, S * 0.6f + 8.4f, 0xFFE8C030, 4);
                // The pinwheel on the corner.
                float pu = L - 7, pv = 7;
                f.circle(0x40000000, pu + 2, pv + 2, 5.5f);
                int[] pw = {0xFFE04848, 0xFF4890E0, 0xFFF0D040, 0xFF60C060};
                for (int j = 0; j < 4; j++) {
                    double a = j * Math.PI / 2 + 0.4f;
                    float eu = pu + (float) Math.cos(a) * 5.5f, ev = pv + (float) Math.sin(a) * 5.5f;
                    float gu = pu + (float) Math.cos(a + 0.9f) * 4, gv = pv + (float) Math.sin(a + 0.9f) * 4;
                    f.poly(pw[j], pu, pv, eu, ev, gu, gv);
                }
                f.circle(0xFFF4F0E8, pu, pv, 0.9f);
                f.sign(city, "TOYS", L / 2, S - 4, 5, 0xFFFFFFFF);
            }
        };
        d.minS = 10;
        d.minL = 12;
        d.ground = City.PLAZA;
        d.base = City.SHOP;
        d.inside = Variants.I_SHELVES;
        d.roofCol = 0xFFC83A30;
        d.districts = Variants.MID | Variants.SUB | Variants.DOWN;
        out.add(d);
    }

    // ================================================================== 10 Wax Museum

    static void waxMuseum(List<Landmarks.Def> out) {
        // A plum-roofed showhouse with a gilded lantern and wax dripping down its cream parapet; out front, a red
        // carpet behind brass posts and velvet ropes, a walk of stars, wax celebrities on plinths, searchlights.
        Landmarks.Def d = new Landmarks.Def("Wax Museum", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int lu = Math.min(L - 4, 12), lv = S - 6;
                return new int[][]{at(al, (L - lu) / 2, 1, lu, lv, 3)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] h = bx(f, pl(this, x0, y0, x1, y1)[0]);
                f.rect(0xFF4A4648, 0, 0, L, S);
                float fv = h[3];
                // The forecourt in dark polished paving with the walk of stars.
                f.rect(0xFF3A3638, 0, fv, L, S);
                for (float u = 2; u < L - 6; u += 10) {
                    float v = S - 6;
                    f.rect(0xFF2E2A2C, u, v - 4, u + 8, v + 4);
                    f.frame(0xFF8A7A50, 0.5f, u, v - 4, u + 8, v + 4);
                    float su = u + 4, sv = v;
                    float[] q = new float[20];
                    for (int k = 0; k < 10; k++) {
                        double a = -Math.PI / 2 + k * Math.PI / 5;
                        float rr = k % 2 == 0 ? 3.2f : 1.3f;
                        q[2 * k] = su + (float) Math.cos(a) * rr;
                        q[2 * k + 1] = sv + (float) Math.sin(a) * rr;
                    }
                    f.poly(0xFFE89AA8, q);
                    f.circle(0xFFD8B860, su, sv, 0.6f);
                }
                // The red carpet up to the doors, brass posts and velvet ropes.
                float cu = (h[0] + h[2]) / 2;
                f.rect(0xFFA82830, cu - 7, fv, cu + 7, S);
                f.rect(0xFFB83840, cu - 6, fv, cu + 6, S);
                f.line(0xFFD8B860, 0.5f, cu - 7, fv, cu - 7, S);
                f.line(0xFFD8B860, 0.5f, cu + 7, fv, cu + 7, S);
                for (int s = -1; s <= 1; s += 2) {
                    float u = cu + s * 10;
                    for (float v = fv + 3; v < S - 1; v += 6) {
                        f.circle(0x40000000, u + 0.8f, v + 0.8f, 1.2f);
                        f.circle(0xFFD8B860, u, v, 1.1f);
                        if (v + 6 < S - 1) f.line(0xFF7A1A28, 0.8f, u, v + 1, u, v + 5);
                    }
                }
                // Wax figures on plinths each side, frozen mid-pose, with a little halo of light.
                for (int k = 0; k < 4; k++) {
                    float u = cu + (k < 2 ? -1 : 1) * (22 + (k % 2) * 18);
                    if (u < 6 || u > L - 6) continue;
                    float v = fv + 7;
                    f.circle(0x30FFF0C0, u, v, 6);
                    f.rect(0x40000000, u - 2.5f, v - 2, u + 4.5f, v + 4.5f);
                    f.rect(0xFFD8D0C0, u - 3.5f, v - 3.5f, u + 3.5f, v + 3.5f);
                    person(f, u, v, CLOTHES[(k * 3 + 1) % 8]);
                    f.line(CLOTHES[(k * 3 + 1) % 8], 0.9f, u, v, u + (k % 2 == 0 ? 2.6f : -2.4f), v - 2.4f);
                }
                // Searchlights at the corners, beams sweeping the sky.
                for (int s = 0; s < 2; s++) {
                    float u = s == 0 ? 6 : L - 6, v = S - 13;
                    float du = s == 0 ? -1 : 1;
                    f.poly(0x1CFFF8D8, u, v, u + du * 26 - 6, v - 30, u + du * 26 + 6, v - 30);
                    f.circle(0x40000000, u + 1, v + 1, 2.6f);
                    f.circle(0xFF5A5E66, u, v, 2.6f);
                    f.circle(0xFFF8F0C8, u - 0.3f, v - 0.3f, 1.6f);
                }
                // A queue along the rope and fans by the carpet.
                for (int k = 0; k < 6; k++) person(f, cu + 13 + (k % 2) * 2, fv + 5 + k * 5, CLOTHES[k % 8]);
                f.lamp(cu - 9, fv + 1.5f);
                f.lamp(cu + 9, fv + 1.5f);
                if (h[0] > 20) {
                    f.tree(8, 10, 5, 0xFF3E6E3A);
                    f.tree(L - 8, 10, 5, 0xFF3E6E3A);
                }
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                // Cream parapet with wax drips running down from its top edge.
                f.rect(0xFFE8DCC0, 0, 0, L, S);
                hip(f, 0xFF6A4A62, 3, 3, L - 3, S - 3, true);
                // A long skylight along the ridge.
                float m = S / 2;
                f.rect(0xFF4A3448, S / 2 - 1, m - 2.5f, L - S / 2 + 1, m + 2.5f);
                f.rect(0xFFB8CCD8, S / 2, m - 1.8f, L - S / 2, m + 1.8f);
                for (float u = S / 2 + 3; u < L - S / 2; u += 3) f.line(0xFF4A3448, 0.4f, u, m - 1.8f, u, m + 1.8f);
                // The gilded lantern in the middle.
                float lr = Math.min(S * 0.18f, 9);
                f.circle(0x50000000, L / 2 + 1.5f, m + 2, lr);
                f.circle(0xFF8A6A30, L / 2, m, lr);
                dome(f, L / 2, m, lr - 1, 0xFFD8B860, 12);
                f.circle(0xFFF4E8B0, L / 2, m, 1);
                // Drips: blobs and runs of melted wax over the edges.
                for (float u = 2; u < L - 2; u += 3 + r.nextFloat() * 4) {
                    float len = 1 + r.nextFloat() * 3;
                    f.roundRect(0xFFF4ECD8, u, 0, u + 1.6f, len + 1, 0.8f);
                    f.circle(0xFFF8F2E2, u + 0.8f, len + 0.8f, 1);
                }
                for (float v = 2; v < S - 2; v += 4 + r.nextFloat() * 4) {
                    float len = 1 + r.nextFloat() * 2.5f;
                    f.roundRect(0xFFF4ECD8, 0, v, len + 1, v + 1.6f, 0.8f);
                }
                f.frame(0xFFB8A888, 0.6f, 0.3f, 0.3f, L - 0.3f, S - 0.3f);
                f.sign(city, "WAX MUSEUM", L / 2, S - 4, 5, 0xFFF4E4C0);
            }
        };
        d.minS = 10;
        d.minL = 12;
        d.ground = City.PLAZA;
        d.base = City.OFFICE;
        d.inside = Variants.I_SHOWROOM;
        d.roofCol = 0xFF6A4A62;
        d.districts = Variants.DOWN | Variants.MID | Variants.OLD;
        out.add(d);
    }

    // ================================================================== 11 Escape Room Arcade

    /** A jigsaw piece outline filled: a square with knobs on two sides and sockets on the others. */
    static void jigsaw(F f, float u, float v, float s, int col, int knobs) {
        f.rect(col, u, v, u + s, v + s);
        float kr = s * 0.18f;
        if ((knobs & 1) != 0) f.circle(col, u + s / 2, v - kr * 0.6f, kr);
        if ((knobs & 2) != 0) f.circle(col, u + s + kr * 0.6f, v + s / 2, kr);
    }

    static void escapeRooms(List<Landmarks.Def> out) {
        // A puzzle box of a building: its roof a grid of themed rooms under coloured skylights, a padlock and a
        // jigsaw piece painted on top; outside, jigsaw paving, a giant padlock sculpture and a key-shaped flower bed.
        Landmarks.Def d = new Landmarks.Def("Escape Room Arcade", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int lu = Math.min(L - 5, 11), lv = S - 5;
                return new int[][]{at(al, 1, 1, lu, lv, 2)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] h = bx(f, pl(this, x0, y0, x1, y1)[0]);
                // Jigsaw paving: alternating squares with knobs.
                f.rect(0xFFB8B0A0, 0, 0, L, S);
                float s = 10;
                for (float u = 0; u < L; u += s)
                    for (float v = 0; v < S; v += s) {
                        int i = (int) (u / s) + (int) (v / s);
                        if (i % 2 == 0) {
                            f.rect(0xFFA8A090, u, v, u + s, v + s);
                            f.circle(0xFFA8A090, u + s, v + s / 2, 1.8f);
                            f.circle(0xFFA8A090, u + s / 2, v + s, 1.8f);
                        }
                    }
                // A strip of lawn and the key-shaped flower bed.
                float g0 = h[2] + 5, g1 = L - 4;
                if (g1 - g0 > 22) {
                    f.roundRect(0xFF6E9A52, g0, 4, g1, S - 4, 4);
                    float ku = (g0 + g1) / 2, kr = Math.min(g1 - g0, 30) * 0.28f, kv0 = 8 + kr;
                    f.circle(0xFF6A5040, ku, kv0, kr + 1.5f);
                    f.circle(0xFFE0B040, ku, kv0, kr);
                    f.circle(0xFF6E9A52, ku, kv0, kr * 0.45f);
                    float kv1 = Math.min(S - 10, kv0 + kr + 40);
                    f.rect(0xFF6A5040, ku - 3, kv0 + kr, ku + 3, kv1);
                    f.rect(0xFFE0B040, ku - 2, kv0 + kr, ku + 2, kv1);
                    for (int k = 0; k < 2; k++) {
                        float tv = kv1 - 4 - k * 7;
                        f.rect(0xFF6A5040, ku + 2, tv - 2.5f, ku + 8, tv + 2.5f);
                        f.rect(0xFFE0B040, ku + 2, tv - 1.5f, ku + 7, tv + 1.5f);
                    }
                    for (int k = 0; k < 12; k++) f.circle(0xFFF0D060, ku - kr + r.nextFloat() * kr * 2, kv0 - kr + r.nextFloat() * kr * 2, 0.6f);
                    f.tree(g1 - 5, S - 9, 4.5f, 0xFF4A7E3C);
                }
                // The giant padlock sculpture in front of the door.
                float pu = h[0] + 14, pv = h[3] + 10;
                if (pv + 6 < S) {
                    f.rect(0x40000000, pu - 4, pv - 1, pu + 8, pv + 8);
                    f.arc(0xFF8A8E96, 2f, pu, pv - 3, 4.2f, Math.PI, 2 * Math.PI);
                    f.rect(0xFFB8902E, pu - 6, pv - 3, pu + 6, pv + 6);
                    f.rect(0xFFD8B048, pu - 6, pv - 3, pu + 6, pv - 1);
                    f.circle(0xFF2A2A2E, pu, pv + 1, 1.3f);
                    f.rect(0xFF2A2A2E, pu - 0.5f, pv + 1, pu + 0.5f, pv + 3.8f);
                }
                // Benches, a bike rack and players milling about.
                float bu = (h[0] + h[2]) / 2 + 12;
                for (int k = 0; k < 4; k++) f.line(0xFF6A6E76, 0.7f, bu + k * 2.5f, h[3] + 4, bu + k * 2.5f, h[3] + 8);
                f.bench(h[2] - 10, S - 4, true);
                for (int k = 0; k < 5; k++) person(f, h[0] + 26 + k * 6, h[3] + 9 + (k % 2) * 3, CLOTHES[(k + 5) % 8]);
                f.lamp(h[0] + 4, h[3] + 4);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF2E3036, 0, 0, L, S);
                // A grid of rooms, each its own colour, walls dark between them, a skylight in each.
                int nu = Math.max(3, (int) (L / 26)), nv = 2;
                float cw = (L - 2) / nu, ch = (S - 2) / nv;
                int[] th = {0xFF7A4A3A, 0xFF3A5A4A, 0xFF4A4A6E, 0xFF6E5A3A, 0xFF5A3A5A, 0xFF3A5A6A};
                for (int i = 0; i < nu; i++)
                    for (int j = 0; j < nv; j++) {
                        float u0 = 1 + i * cw, v0 = 1 + j * ch;
                        int col = th[(i * 2 + j * 3) % th.length];
                        f.rect(col, u0 + 0.8f, v0 + 0.8f, u0 + cw - 0.8f, v0 + ch - 0.8f);
                        f.rect(City.lighten(col, 0.08f), u0 + 0.8f, v0 + 0.8f, u0 + cw - 0.8f, v0 + 2);
                        float su = u0 + cw / 2, sv = v0 + ch / 2;
                        f.rect(0x40000000, su - 3, sv - 2, su + 4.5f, sv + 3.5f);
                        f.rect(City.lighten(col, 0.45f), su - 3.5f, sv - 2.5f, su + 3.5f, sv + 2.5f);
                        f.line(City.darken(col, 0.7f), 0.4f, su, sv - 2.5f, su, sv + 2.5f);
                        // A door gap in one wall to the next room: a maze of rooms.
                        if (i < nu - 1) f.rect(0xFF8A7A5A, u0 + cw - 0.8f, v0 + ch * (0.25f + 0.5f * ((i + j) % 2)), u0 + cw + 0.8f, v0 + ch * (0.25f + 0.5f * ((i + j) % 2)) + 3);
                    }
                f.rect(0xFF8A7A5A, 1 + cw * 0.3f, S / 2 - 0.8f, 1 + cw * 0.3f + 3, S / 2 + 0.8f);
                // The padlock painted on one room's roof, a jigsaw piece on another.
                float pu = 1 + cw * (nu - 0.5f), pv = 1 + ch * 0.5f;
                f.arc(0xFFE8D060, 1.4f, pu - cw * 0.25f, pv - 1, 2.6f, Math.PI, 2 * Math.PI);
                f.rect(0xFFE8D060, pu - cw * 0.25f - 4, pv - 1, pu - cw * 0.25f + 4, pv + 4.5f);
                f.circle(0xFF2A2A2E, pu - cw * 0.25f, pv + 1.5f, 0.9f);
                float ju = 1 + cw * 0.15f, jv = 1 + ch * 1.2f;
                jigsaw(f, ju, jv, 6, 0xFF60B0D8, 3);
                // Rooftop units.
                f.rect(0x40000000, L * 0.42f + 1.5f, 2.5f, L * 0.42f + 9.5f, 8.5f);
                f.rect(0xFF9A9EA6, L * 0.42f, 1.5f, L * 0.42f + 8, 7.5f);
                f.circle(0xFF5A5E66, L * 0.42f + 4, 4.5f, 2.2f);
                f.sign(city, "ESCAPE ROOMS", L / 2, S - 4, 4.5f, 0xFFF0E8C8);
            }
        };
        d.minS = 10;
        d.minL = 12;
        d.ground = City.PLAZA;
        d.base = City.SHOP;
        d.inside = Variants.I_ARCADE;
        d.roofCol = 0xFF3A3C44;
        d.districts = Variants.DOWN | Variants.MID | Variants.UNI;
        out.add(d);
    }

    // ================================================================== 12 Laser Tag Arena

    /** An inflatable bunker from above: a puffy rounded block with seams. */
    static void bunker(F f, float u0, float v0, float u1, float v1, int col) {
        float rad = Math.min(u1 - u0, v1 - v0) / 2;
        f.roundRect(0x40000000, u0 + 2, v0 + 2.5f, u1 + 2, v1 + 2.5f, rad);
        f.roundRect(City.darken(col, 0.75f), u0, v0, u1, v1, rad);
        f.roundRect(col, u0 + 0.6f, v0 + 0.6f, u1 - 1, v1 - 1, rad * 0.8f);
        f.roundRect(City.lighten(col, 0.25f), u0 + 1.2f, v0 + 1.2f, u0 + (u1 - u0) * 0.5f, v0 + (v1 - v0) * 0.35f, rad * 0.4f);
    }

    static void laserTag(List<Landmarks.Def> out) {
        // A black box arena edged in green and magenta neon with a target logo on the roof; beside it an outdoor
        // battlefield netted in, strewn with inflatable bunkers and players in glowing vests.
        Landmarks.Def d = new Landmarks.Def("Laser Tag Arena", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, 1, 1, L - 7, S - 4, 2)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] h = bx(f, pl(this, x0, y0, x1, y1)[0]);
                f.rect(0xFF3E4044, 0, 0, L, S);
                for (int k = 0; k < 40; k++) f.circle(0x10FFFFFF, r.nextFloat() * L, r.nextFloat() * S, 1 + r.nextFloat() * 2);
                // Neon lines painted along the forecourt to the door.
                float fv = h[3];
                for (int k = 0; k < 3; k++) {
                    f.line(k == 1 ? 0xFFE040C0 : 0xFF50E070, 0.8f, 2, fv + 4 + k * 3, h[2], fv + 4 + k * 3);
                }
                float du = (h[0] + h[2]) / 2;
                for (float v = fv + 2; v < S - 1; v += 4) f.poly(0xFF50E070, du - 2, v + 2.5f, du + 2, v + 2.5f, du, v);
                // The outdoor battlefield: astroturf netted in on poles, bunkers and players.
                float a0 = h[2] + 6, a1 = L - 3, b0 = 3, b1 = S - 3;
                f.rect(0xFF3E7A46, a0, b0, a1, b1);
                for (float v = b0 + 4; v < b1; v += 8) f.rect(0x10FFFFFF, a0, v, a1, v + 4);
                f.line(0xC0F0F0F0, 0.6f, (a0 + a1) / 2, b0, (a0 + a1) / 2, b1);
                f.frame(0xFF1E1E22, 1.2f, a0, b0, a1, b1);
                for (float v = b0; v <= b1; v += 8) {
                    f.circle(0xFF1E1E22, a0, v, 0.9f);
                    f.circle(0xFF1E1E22, a1, v, 0.9f);
                }
                float w = a1 - a0;
                int[] bc = {0xFFE07A30, 0xFF3A7AD0};
                int k = 0;
                for (float v = b0 + 10; v < b1 - 8; v += 20, k++) {
                    bunker(f, a0 + w * 0.12f, v, a0 + w * 0.4f, v + 6, bc[k % 2]);
                    bunker(f, a0 + w * 0.58f, v + 8, a0 + w * 0.86f, v + 14, bc[(k + 1) % 2]);
                    f.circle(0x40000000, a0 + w * 0.5f + 1.5f, v + 4 + 1.5f, 3.4f);
                    f.circle(bc[k % 2], a0 + w * 0.5f, v + 4, 3.4f);
                    f.circle(City.lighten(bc[k % 2], 0.3f), a0 + w * 0.5f - 1, v + 3, 1.2f);
                }
                for (int j = 0; j < 6; j++) {
                    float u = a0 + w * (0.15f + (j % 3) * 0.33f), v = b0 + 6 + j * (b1 - b0 - 12) / 6f;
                    f.circle(j % 2 == 0 ? 0x5050E070 : 0x50E040C0, u, v, 3);
                    person(f, u, v, j % 2 == 0 ? 0xFF50E070 : 0xFFE040C0);
                }
                f.lamp(h[2] + 3, 3);
                f.lamp(h[2] + 3, S - 3);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF1E1E24, 0, 0, L, S);
                f.rect(0xFF2A2A32, 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                for (float u = 6; u < L - 2; u += 6) f.line(0x18FFFFFF, 0.4f, u, 1.5f, u, S - 1.5f);
                // Neon tubes round the parapet.
                f.frame(0xFF50E070, 0.8f, 0.8f, 0.8f, L - 0.8f, S - 0.8f);
                f.frame(0xFFE040C0, 0.6f, 2.4f, 2.4f, L - 2.4f, S - 2.4f);
                // The target logo with a lightning bolt.
                float cu = L * 0.5f, cv = S * 0.5f, R = Math.min(S, L) * 0.3f;
                f.circle(0xFF50E070, cu, cv, R);
                f.circle(0xFF2A2A32, cu, cv, R * 0.8f);
                f.circle(0xFFE040C0, cu, cv, R * 0.6f);
                f.circle(0xFF2A2A32, cu, cv, R * 0.4f);
                f.circle(0xFF50E070, cu, cv, R * 0.2f);
                f.line(0xFF50E070, 0.6f, cu - R * 1.2f, cv, cu + R * 1.2f, cv);
                f.line(0xFF50E070, 0.6f, cu, cv - R * 1.2f, cu, cv + R * 1.2f);
                // Extract fans and glowing strip skylights.
                for (int s = 0; s < 2; s++) {
                    float fu = s == 0 ? 8 : L - 8, fv = 8;
                    f.circle(0x40000000, fu + 1.5f, fv + 1.5f, 4);
                    f.circle(0xFF6A6E76, fu, fv, 4);
                    f.circle(0xFF3A3C42, fu, fv, 3.2f);
                    for (int k = 0; k < 4; k++) {
                        double a = k * Math.PI / 2 + 0.3f;
                        f.line(0xFF8A8E96, 0.8f, fu, fv, fu + (float) Math.cos(a) * 3, fv + (float) Math.sin(a) * 3);
                    }
                }
                for (float u = 6; u < L - 6; u += 12) f.rect(0xFF60D8E8, u, S - 7, u + 7, S - 5.5f);
                f.sign(city, "LASER TAG", cu, S - 3.5f, 4.5f, 0xFFB0FFC0);
            }
        };
        d.minS = 10;
        d.minL = 14;
        d.ground = City.LOT;
        d.base = City.SHOP;
        d.inside = Variants.I_ARCADE;
        d.roofCol = 0xFF2A2A32;
        d.districts = Variants.MID | Variants.IND | Variants.SUB;
        out.add(d);
    }
    static void planetWalk(List<Landmarks.Def> out) {}
    static void labyrinth(List<Landmarks.Def> out) {}
    static void dinoPark(List<Landmarks.Def> out) {}
    static void rocket(List<Landmarks.Def> out) {}
    static void clockPlaza(List<Landmarks.Def> out) {}
    static void chessPark(List<Landmarks.Def> out) {}
    static void skyBridge(List<Landmarks.Def> out) {}
    static void upsideDown(List<Landmarks.Def> out) {}
    static void ropeCourse(List<Landmarks.Def> out) {}
    static void balloons(List<Landmarks.Def> out) {}
    static void festival(List<Landmarks.Def> out) {}
    static void sandcastles(List<Landmarks.Def> out) {}
    static void glassBridge(List<Landmarks.Def> out) {}
}
