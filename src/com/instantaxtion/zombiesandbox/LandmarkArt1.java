package com.instantaxtion.zombiesandbox;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.Landmarks.*;

/** ((22.4) Landmark designs, part 1 (see Landmarks). */
final class LandmarkArt1 {
    private LandmarkArt1() {
    }

    static void register(List<Landmarks.Def> out) {
        // A paved square round a big three-tier fountain, with plane trees, benches and lamps.
        Landmarks.Def d = new Landmarks.Def("Fountain Square", T_PARK) {
            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2, m = Math.min(x1 - x0, y1 - y0);
                rect(c, p, 0xFFCFC6B2, x0, y0, x1, y1);
                // Paving in a radial pattern of light and dark rings.
                for (int k = 6; k >= 1; k--) circle(c, p, k % 2 == 0 ? 0xFFD9D0BC : 0xFFC4BAA4, cx, cy, m * 0.08f * k);
                for (int k = 0; k < 16; k++) {
                    double a = k * Math.PI / 8;
                    line(c, p, 0x30000000, 0.6f, cx, cy, cx + (float) Math.cos(a) * m * 0.48f, cy + (float) Math.sin(a) * m * 0.48f);
                }
                // The fountain: a stone basin, water, the tiers and the spray.
                circle(c, p, 0xFF8E8676, cx, cy, m * 0.2f);
                circle(c, p, 0xFF4A90B8, cx, cy, m * 0.18f);
                circle(c, p, 0xFF6AAAD0, cx, cy, m * 0.13f);
                circle(c, p, 0xFFB0A894, cx, cy, m * 0.08f);
                circle(c, p, 0xFF5AA0C8, cx, cy, m * 0.06f);
                circle(c, p, 0xFFC8C0AC, cx, cy, m * 0.025f);
                for (int k = 0; k < 12; k++) {
                    double a = k * Math.PI / 6;
                    circle(c, p, 0x90FFFFFF, cx + (float) Math.cos(a) * m * 0.1f, cy + (float) Math.sin(a) * m * 0.1f, 0.8f);
                }
                // Trees round the square, benches facing in, lamps on the corners.
                float in = 10;
                for (float t = x0 + in; t <= x1 - in + 0.1f; t += (x1 - x0 - 2 * in) / 4) {
                    tree(c, p, t, y0 + in, 5.5f);
                    tree(c, p, t, y1 - in, 5.5f);
                }
                for (float t = y0 + in + (y1 - y0 - 2 * in) / 4; t < y1 - in - 1; t += (y1 - y0 - 2 * in) / 4) {
                    tree(c, p, x0 + in, t, 5.5f);
                    tree(c, p, x1 - in, t, 5.5f);
                }
                for (int k = 0; k < 4; k++) {
                    double a = Math.PI / 4 + k * Math.PI / 2;
                    bench(c, p, cx + (float) Math.cos(a) * m * 0.3f, cy + (float) Math.sin(a) * m * 0.3f, k % 2 == 0);
                }
                lamp(c, p, x0 + 3, y0 + 3);
                lamp(c, p, x1 - 3, y0 + 3);
                lamp(c, p, x0 + 3, y1 - 3);
                lamp(c, p, x1 - 3, y1 - 3);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.MID | Variants.OLD;
        out.add(d);

        // A civic hall with a clock tower on its front and a lawn before it.
        d = new Landmarks.Def("Clock Tower Hall", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int hw = bw - 4, hh = Math.max(4, bh / 2);
                return new int[][]{{2, bh - hh - 1, hw, hh, 3}, {bw / 2 - 1, bh - hh - 4, 3, 3, 9}};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                rect(c, p, 0xFF5E9A48, x0, y0, x1, y1);
                float cx = (x0 + x1) / 2;
                rect(c, p, 0xFFD4CAB0, cx - 6, y0, cx + 6, y1);
                for (float y = y0 + 8; y < (y0 + y1) / 2; y += 14) {
                    bush(c, p, cx - 10, y, 3);
                    bush(c, p, cx + 10, y, 3);
                }
                tree(c, p, x0 + 10, y0 + 10, 7);
                tree(c, p, x1 - 10, y0 + 10, 7);
                lamp(c, p, cx - 7, y0 + 4);
                lamp(c, p, cx + 7, y0 + 4);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                if (part == 0) {
                    // Slate hipped roof with a lead ridge and dormers.
                    rect(c, p, 0xFF3E4650, x0, y0, x1, y1);
                    float mx = (x0 + x1) / 2, my = (y0 + y1) / 2;
                    if (x1 - x0 >= y1 - y0) {
                        float in = (y1 - y0) / 2;
                        poly(c, p, 0xFF5A6470, x0 + 1, y0 + 1, x1 - 1, y0 + 1, x1 - in, my, x0 + in, my);
                        poly(c, p, 0xFF4A535E, x0 + 1, y1 - 1, x1 - 1, y1 - 1, x1 - in, my, x0 + in, my);
                        line(c, p, 0xFF9AA2AA, 1.2f, x0 + in, my, x1 - in, my);
                    } else {
                        float in = (x1 - x0) / 2;
                        poly(c, p, 0xFF56606C, x0 + 1, y0 + 1, x0 + 1, y1 - 1, mx, y1 - in, mx, y0 + in);
                        poly(c, p, 0xFF4A535E, x1 - 1, y0 + 1, x1 - 1, y1 - 1, mx, y1 - in, mx, y0 + in);
                        poly(c, p, 0xFF5E6874, x0 + 1, y0 + 1, x1 - 1, y0 + 1, mx, y0 + in);
                        line(c, p, 0xFF9AA2AA, 1.2f, mx, y0 + in, mx, y1 - in);
                    }
                    for (float x = x0 + 8; x < x1 - 8; x += 12) rect(c, p, 0xFFD8D0C0, x, y1 - 6, x + 4, y1 - 3);
                } else {
                    // The tower: a stone square, the four clock faces and a copper spire.
                    float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2, s = (x1 - x0) / 2;
                    rect(c, p, 0xFFB8AE96, x0, y0, x1, y1);
                    rect(c, p, 0xFFCFC5AC, x0 + 2, y0 + 2, x1 - 2, y1 - 2);
                    poly(c, p, 0xFF4E9A84, x0 + 4, y0 + 4, x1 - 4, y0 + 4, cx, cy);
                    poly(c, p, 0xFF3E8270, x1 - 4, y0 + 4, x1 - 4, y1 - 4, cx, cy);
                    poly(c, p, 0xFF2E6A5A, x0 + 4, y1 - 4, x1 - 4, y1 - 4, cx, cy);
                    poly(c, p, 0xFF5AAE96, x0 + 4, y0 + 4, x0 + 4, y1 - 4, cx, cy);
                    circle(c, p, 0xFFE8C860, cx, cy, 1.4f);
                    circle(c, p, 0xFFF4F0E4, cx, y1 + 0.5f, s * 0.35f);
                    line(c, p, 0xFF202020, 0.6f, cx, y1 + 0.5f, cx, y1 + 0.5f - s * 0.25f);
                    line(c, p, 0xFF202020, 0.6f, cx, y1 + 0.5f, cx + s * 0.18f, y1 + 0.5f);
                }
            }
        };
        d.minS = 10;
        d.minL = 10;
        d.base = City.OFFICE;
        d.inside = Variants.I_OFFICE;
        d.districts = Variants.DOWN | Variants.MID | Variants.OLD;
        out.add(d);
    
        opera(out);
        artMuseum(out);
        naturalHistory(out);
        planetarium(out);
        grandHotel(out);
        concertHall(out);
        cathedral(out);
        townGate(out);
        arcade(out);
        library(out);
    }

    // ------------------------------------------------------------------ shared bits for the designs below

    static final float TT = City.T;

    /** A building laid out along the block's long side (u along it, v across), turned when the block is tall. */
    static int[] at(boolean along, int u, int v, int lu, int lv, int floors) {
        return along ? new int[]{u, v, lu, lv, floors} : new int[]{v, u, lv, lu, floors};
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

        /** An outline circle. */
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

        /** A bench lying along u (or across it). */
        void bench(float u, float v, boolean alongU) {
            Landmarks.bench(c, p, x(u, v), y(u, v), alongU != t);
        }

        void clip(float u0, float v0, float u1, float v1) {
            c.clipRect(x(u0, v0), y(u0, v0), x(u1, v1), y(u1, v1));
        }

        void mv(Path q, float u, float v) {
            q.moveTo(x(u, v), y(u, v));
        }

        void qd(Path q, float cu, float cv, float u, float v) {
            q.quadTo(x(cu, cv), y(cu, cv), x(u, v), y(u, v));
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
            for (float k = 3; k < h - 1; k += 3) f.frame(0x26000000, 0.5f, u0 + k, v0 + k, u1 - k, v1 - k);
        int hl = City.lighten(col, 0.3f);
        f.line(hl, 0.7f, u0, v0, au, av);
        f.line(hl, 0.7f, u1, v0, bu, av);
        f.line(City.darken(col, 0.6f), 0.7f, u0, v1, au, bv);
        f.line(City.darken(col, 0.6f), 0.7f, u1, v1, bu, bv);
        f.line(hl, 1f, au, av, bu, bv);
    }

    /** A gabled roof with its ridge along u. */
    static void gable(F f, int col, float u0, float v0, float u1, float v1) {
        float m = (v0 + v1) / 2;
        f.rect(City.lighten(col, 0.1f), u0, v0, u1, m);
        f.rect(City.darken(col, 0.74f), u0, m, u1, v1);
        f.line(City.lighten(col, 0.35f), 1f, u0, m, u1, m);
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

    /** A flight of steps across v from v0 down to v1, widening as it goes. */
    static void steps(F f, float u0, float v0, float u1, float v1, int n, int col) {
        float h = (v1 - v0) / n;
        for (int k = 0; k < n; k++) {
            float v = v0 + k * h, w = k * 1.2f;
            f.rect(k % 2 == 0 ? col : City.darken(col, 0.92f), u0 - w, v, u1 + w, v + h);
            f.line(0x40000000, 0.6f, u0 - w, v + h, u1 + w, v + h);
        }
    }

    // ------------------------------------------------------------------ Opera House

    /** One white shell seen from above: a pointed lens, lit on its upper half, with ribs fanning to the tip. */
    static void shell(F f, float a, float b, float vc, float h, boolean mouth) {
        float cu = a + (b - a) * 0.42f;
        Path q = new Path();
        f.mv(q, a + 2, vc + 2);
        f.qd(q, cu + 2, vc - 2 * h + 2, b + 2, vc + 2);
        f.qd(q, cu + 2, vc + 2 * h + 2, a + 2, vc + 2);
        q.close();
        f.fill(q, 0x38000000);
        if (mouth) f.oval(0xFF7A6448, a - h * 0.18f, vc - h * 0.55f, a + h * 0.4f, vc + h * 0.55f);
        q = new Path();
        f.mv(q, a, vc);
        f.qd(q, cu, vc - 2 * h, b, vc);
        q.close();
        f.fill(q, 0xFFF7F5EF);
        q = new Path();
        f.mv(q, a, vc);
        f.qd(q, cu, vc + 2 * h, b, vc);
        q.close();
        f.fill(q, 0xFFD9D5CB);
        for (int j = 1; j <= 4; j++) {
            float s = j * 0.16f, pu = (1 - s) * (1 - s) * a + 2 * s * (1 - s) * cu + s * s * b, pv = 2 * h * 2 * s * (1 - s);
            f.line(0x22000000, 0.5f, b, vc, pu, vc - pv);
            f.line(0x26000000, 0.5f, b, vc, pu, vc + pv);
        }
        f.line(0x90FFFFFF, 0.7f, a + (b - a) * 0.05f, vc - 0.3f, b, vc - 0.3f);
        q = new Path();
        f.mv(q, a, vc);
        f.qd(q, cu, vc - 2 * h, b, vc);
        f.qd(q, cu, vc + 2 * h, a, vc);
        q.close();
        f.p.setStyle(Paint.Style.STROKE);
        f.p.setStrokeWidth(0.7f);
        f.p.setColor(0x60000000);
        f.c.drawPath(q, f.p);
        f.p.setStyle(Paint.Style.FILL);
    }

    static void shellRow(F f, float ua, float ub, float vc, float hw, int n) {
        float step = (ub - ua) / (n + 0.25f);
        for (int k = 0; k < n; k++) {
            float g = 1 - 0.13f * k, a = ua + k * step;
            shell(f, a, a + 1.9f * step * g, vc, hw * (1 - 0.1f * k), k == 0);
        }
    }

    static void opera(List<Landmarks.Def> out) {
        // Sail-like shells on a red granite podium, with a broad stepped forecourt before it.
        Landmarks.Def d = new Landmarks.Def("Opera House", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int l = Math.max(bw, bh), s = Math.min(bw, bh);
                return new int[][]{at(bw >= bh, 2, 1, l - 4, s - 5, 4)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, T = TT;
                int st = Math.round(S / T);
                float bu0 = 2 * T, bu1 = L - 2 * T, bv0 = T, bv1 = (st - 4) * T;
                f.rect(0xFFD6CFC0, 0, 0, L, S);
                for (float u = 8; u < L; u += 8) f.line(0x16000000, 0.5f, u, 0, u, S);
                for (float v = 8; v < S; v += 8) f.line(0x10000000, 0.5f, 0, v, L, v);
                // A promenade with a railing along the back.
                f.rect(0xFFC2BAA8, 0, 0, L, 5);
                f.line(0xFF5E6266, 0.8f, 0, 1.5f, L, 1.5f);
                for (float u = 4; u < L; u += 8) f.circle(0xFF5E6266, u, 1.5f, 0.6f);
                // The podium the shells stand on.
                f.rect(0x30000000, bu0 - 2, bv0 - 1, bu1 + 7, bv1 + 7);
                f.rect(0xFFA88E78, bu0 - 4, bv0 - 3, bu1 + 4, bv1 + 4);
                // The stepped forecourt.
                float s0 = bv1 + 4, s1 = S - 7, su0 = bu0 + (bu1 - bu0) * 0.12f, su1 = bu1 - (bu1 - bu0) * 0.12f;
                int n = Math.max(4, (int) ((s1 - s0) / 3));
                steps(f, su0, s0, su1, s1, n, 0xFFCDBFAC);
                // Avenues of trees down both ends, with benches.
                for (float v = bv0 + 6; v < S - 10; v += 18) {
                    f.tree(T * 0.75f, v, 5.5f);
                    f.tree(L - T * 0.75f, v, 5.5f);
                }
                f.bench(T * 0.75f, S - 12, false);
                f.bench(L - T * 0.75f, S - 12, false);
                // Lamps and bollards along the front.
                for (float u = 6; u < L - 3; u += 16) f.lamp(u, S - 3.5f);
                for (float u = su0; u <= su1 + 0.1f; u += (su1 - su0) / 6) f.circle(0xFF6A6460, u, S - 5.5f, 0.9f);
                // Flags at the head of the steps.
                for (int k = 0; k < 4; k++) {
                    float u = su0 + 6 + k * (su1 - su0 - 12) / 3;
                    f.circle(0xFF505050, u, s0 - 1.5f, 0.8f);
                    f.rect(k % 2 == 0 ? 0xFFB83A30 : 0xFFE8D8A0, u + 0.6f, s0 - 2.5f, u + 5, s0 - 0.5f);
                }
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF9A826C, 0, 0, L, S);
                f.rect(0xFFBEA890, 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                for (float u = 6; u < L - 2; u += 6) f.line(0x18000000, 0.5f, u, 1.5f, u, S - 1.5f);
                if (S >= 60) {
                    shellRow(f, L * 0.03f, L * 0.93f, S * 0.31f, S * 0.19f, 4);
                    shellRow(f, L * 0.14f, L * 0.86f, S * 0.73f, S * 0.16f, 3);
                } else {
                    shellRow(f, L * 0.04f, L * 0.95f, S * 0.5f, S * 0.32f, 4);
                }
                f.sign(city, "OPERA", L / 2, S / 2, 7, 0xFF7A2A20);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.MID;
        d.minS = 10;
        d.minL = 12;
        d.base = City.OFFICE;
        d.inside = Variants.I_SEATS;
        d.roofCol = 0xFFF0EEE8;
        out.add(d);
    }

    // ------------------------------------------------------------------ Art Museum

    static void plinth(F f, float u, float v, float s, int kind) {
        f.rect(0x40000000, u - s + 1.5f, v - s + 1.5f, u + s + 1.5f, v + s + 1.5f);
        f.rect(0xFFE8E4DA, u - s, v - s, u + s, v + s);
        f.frame(0xFFB0AA9C, 0.5f, u - s, v - s, u + s, v + s);
        switch (kind % 4) {
            case 0:
                f.ring(0x50000000, 1.8f, u + 1, v + 1, s * 0.6f);
                f.ring(0xFFC0402E, 1.8f, u, v, s * 0.6f);
                break;
            case 1:
                f.circle(0x50000000, u + 1.2f, v + 1.2f, s * 0.45f);
                f.circle(0xFF6E5028, u, v, s * 0.45f);
                f.circle(0xFF9A7A44, u - s * 0.12f, v - s * 0.12f, s * 0.2f);
                break;
            case 2:
                f.rect(0x50000000, u - s * 0.4f, v - s * 0.4f, u + s * 0.7f, v + s * 0.7f);
                f.rect(0xFF8E969E, u - s * 0.55f, v - s * 0.55f, u + s * 0.35f, v + s * 0.35f);
                f.rect(0xFFB8C0C8, u - s * 0.2f, v - s * 0.2f, u + s * 0.55f, v + s * 0.55f);
                break;
            default:
                f.circle(0xFF2E7AA8, u, v, s * 0.6f);
                f.circle(0xFFE8C040, u + s * 0.2f, v - s * 0.15f, s * 0.3f);
                f.circle(0xFFFFFFFF, u - s * 0.25f, v + s * 0.2f, s * 0.15f);
        }
    }

    static void artMuseum(List<Landmarks.Def> out) {
        // Stone gallery wings round a glass atrium, with a sculpture court and a reflecting pool before it.
        Landmarks.Def d = new Landmarks.Def("Art Museum", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int l = Math.max(bw, bh), s = Math.min(bw, bh);
                return new int[][]{at(bw >= bh, 1, 1, l - 2, Math.max(4, s - 6), 3)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, T = TT;
                int st = Math.round(S / T);
                float bv1 = (1 + Math.max(4, st - 6)) * T;
                f.rect(0xFFE0DACB, 0, 0, L, S);
                for (float u = 16; u < L; u += 16) f.line(0x1C000000, 0.5f, u, 0, u, S);
                for (float v = 16; v < S; v += 16) f.line(0x1C000000, 0.5f, 0, v, L, v);
                f.rect(0x30000000, T, bv1, L - T + 2, bv1 + 2);
                float cv0 = bv1 + 6, cv1 = S - 6, m = L / 2;
                // Two lawns either side of the entrance walk.
                float la0 = 6, la1 = m - 14, ra0 = m + 14, ra1 = L - 6;
                f.rect(0xFF8A9A70, la0 - 1, cv0 - 1, la1 + 1, cv1 + 1);
                f.rect(0xFF6FA052, la0, cv0, la1, cv1);
                f.rect(0xFF8A9A70, ra0 - 1, cv0 - 1, ra1 + 1, cv1 + 1);
                f.rect(0xFF6FA052, ra0, cv0, ra1, cv1);
                // The reflecting pool across the left lawn, the sculptures on plinths.
                float pv = (cv0 + cv1) / 2;
                f.rect(0xFFB8B2A4, la0 + 4, pv - 5, la1 - 4, pv + 5);
                f.rect(0xFF3F7FA8, la0 + 5, pv - 4, la1 - 5, pv + 4);
                f.rect(0xFF5A96BC, la0 + 5, pv - 4, la1 - 5, pv - 2.5f);
                int n = Math.max(1, (int) ((la1 - la0) / 32));
                for (int k = 0; k < n; k++) {
                    float u = la0 + (la1 - la0) * (k + 0.5f) / n;
                    plinth(f, u, cv0 + (pv - 5 - cv0) / 2, 3.5f, k);
                    if (cv1 - pv > 18) f.tree(u, cv1 - (cv1 - pv - 5) / 2, 4.5f, 0xFF7AA050);
                }
                n = Math.max(2, (int) ((ra1 - ra0) / 24));
                for (int k = 0; k < n; k++) {
                    float u = ra0 + (ra1 - ra0) * (k + 0.5f) / n;
                    plinth(f, u, k % 2 == 0 ? cv0 + (cv1 - cv0) * 0.3f : cv0 + (cv1 - cv0) * 0.7f, 4, k + 1);
                }
                // The entrance walk, benches facing the lawns, lamps.
                f.rect(0xFFEEE9DE, m - 9, bv1, m + 9, S);
                f.frame(0xFFC8C0B0, 0.6f, m - 9, bv1, m + 9, S);
                for (float v = cv0 + 6; v < cv1 - 4; v += 16) {
                    f.bench(m - 11, v, false);
                    f.bench(m + 11, v, false);
                }
                f.lamp(m - 10, S - 4);
                f.lamp(m + 10, S - 4);
                f.tree(4, S - 4, 4, 0xFF7AA050);
                f.tree(L - 4, S - 4, 4, 0xFF7AA050);
                f.sign(city, "ART MUSEUM", m, S - 8, 5, 0xFF303030);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFFA89E8C, 0, 0, L, S);
                f.rect(0xFFD5CEBE, 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                float au0 = L * 0.36f, au1 = L * 0.64f, av0 = S * 0.16f, av1 = S * 0.84f;
                // Rows of square rooflights over the galleries.
                for (int w = 0; w < 2; w++) {
                    float u0 = w == 0 ? 5 : au1 + 5, u1 = w == 0 ? au0 - 5 : L - 5;
                    for (float u = u0; u + 4 <= u1; u += 9)
                        for (float v = 5; v + 4 <= S - 5; v += 9) {
                            f.rect(0x30000000, u + 0.8f, v + 0.8f, u + 4.8f, v + 4.8f);
                            f.rect(0xFF8FB0C2, u, v, u + 4, v + 4);
                            f.rect(0xFFB8D0DC, u, v, u + 4, v + 1.2f);
                            f.frame(0xFF6E7E88, 0.4f, u, v, u + 4, v + 4);
                        }
                }
                // Plant screen on one wing.
                if (S > 50) {
                    f.rect(0xFF9A9A96, L - 16, S - 14, L - 4, S - 4);
                    f.circle(0xFF6E6E6A, L - 13, S - 9, 2.2f);
                    f.circle(0xFF6E6E6A, L - 7, S - 9, 2.2f);
                }
                // The glass atrium.
                f.rect(0x40000000, au0 + 2, av0 + 2, au1 + 3, av1 + 3);
                f.rect(0xFF7E888E, au0 - 1, av0 - 1, au1 + 1, av1 + 1);
                f.rect(0xFF86AEC4, au0, av0, au1, av1);
                float aw = au1 - au0, ah = av1 - av0;
                f.poly(0x26FFFFFF, au0, av0 + ah * 0.6f, au0 + aw * 0.5f, av0, au0 + aw * 0.75f, av0, au0, av0 + ah * 0.95f);
                f.poly(0x18FFFFFF, au0 + aw * 0.55f, av1, au1, av0 + ah * 0.35f, au1, av0 + ah * 0.55f, au0 + aw * 0.75f, av1);
                for (float u = au0 + 6; u < au1 - 1; u += 6) f.line(0xFF54646E, 0.6f, u, av0, u, av1);
                for (float v = av0 + 6; v < av1 - 1; v += 6) f.line(0xFF54646E, 0.6f, au0, v, au1, v);
                f.line(0xFFE0EAF0, 1.1f, (au0 + au1) / 2, av0, (au0 + au1) / 2, av1);
                f.frame(0xFF4A565E, 0.8f, au0, av0, au1, av1);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.MID | Variants.OLD | Variants.UNI;
        d.minS = 10;
        d.minL = 10;
        d.base = City.OFFICE;
        d.inside = Variants.I_SHOWROOM;
        d.roofCol = 0xFFD5CEBE;
        out.add(d);
    }

    // ------------------------------------------------------------------ Natural History Museum

    /** A topiary dinosaur (a long-necked one) clipped from hedge, fitted to the rectangle. */
    static void dino(F f, float u0, float v0, float u1, float v1) {
        float k = Math.min((u1 - u0) / 44, (v1 - v0) / 28);
        if (k < 0.45f) return;
        float cx = (u0 + u1) / 2 + 2 * k, cy = (v0 + v1) / 2 + 3 * k;
        int dk = 0xFF2E6A2E, md = 0xFF3F7E38, lt = 0xFF5E9E4A;
        float[] blob = new float[60];
        int n = 0;
        // Body, legs, neck, head, tail as (u, v, r).
        float[][] parts = {{cx, cy, 7 * k}, {cx - 5 * k, cy + 1, 5 * k}, {cx + 5 * k, cy + 1, 5 * k},
                {cx - 6 * k, cy + 6 * k, 2 * k}, {cx + 5 * k, cy + 6 * k, 2 * k}};
        for (float[] q : parts) {
            blob[n++] = q[0];
            blob[n++] = q[1];
            blob[n++] = q[2];
        }
        for (int i = 0; i < 6; i++) {
            float s = (i + 1) / 6f;
            blob[n++] = cx - 9 * k - s * 9 * k;
            blob[n++] = cy - 3 * k - s * 11 * k;
            blob[n++] = (3 - 1.4f * s) * k;
        }
        blob[n++] = cx - 20 * k;
        blob[n++] = cy - 15 * k;
        blob[n++] = 2.3f * k;
        for (int i = 0; i < 6; i++) {
            float s = (i + 1) / 6f;
            blob[n++] = cx + 9 * k + s * 12 * k;
            blob[n++] = cy + 1 * k + s * s * 5 * k;
            blob[n++] = (3.2f - 2.6f * s) * k;
        }
        for (int i = 0; i < n; i += 3) f.circle(0x40000000, blob[i] + 1.8f, blob[i + 1] + 1.8f, blob[i + 2]);
        for (int i = 0; i < n; i += 3) f.circle(dk, blob[i], blob[i + 1], blob[i + 2]);
        for (int i = 0; i < n; i += 3) f.circle(md, blob[i] - blob[i + 2] * 0.15f, blob[i + 1] - blob[i + 2] * 0.15f, blob[i + 2] * 0.75f);
        for (int i = 0; i < n; i += 3) f.circle(lt, blob[i] - blob[i + 2] * 0.4f, blob[i + 1] - blob[i + 2] * 0.4f, blob[i + 2] * 0.3f);
        f.circle(0xFF1E2A1E, cx - 20.8f * k, cy - 15.5f * k, 0.5f);
    }

    static void naturalHistory(List<Landmarks.Def> out) {
        // A long hall with a skylit ridge, end pavilions and a columned portico up a flight of steps; a topiary
        // dinosaur and an ammonite flowerbed on the lawns.
        Landmarks.Def d = new Landmarks.Def("Natural History Museum", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int l = Math.max(bw, bh), s = Math.min(bw, bh);
                return new int[][]{at(bw >= bh, 2, 1, l - 4, Math.max(4, s - 5), 3)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, T = TT;
                int st = Math.round(S / T);
                float bu0 = 2 * T, bu1 = L - 2 * T, bv0 = T, bv1 = (1 + Math.max(4, st - 5)) * T;
                f.rect(0xFF5E9A48, 0, 0, L, S);
                f.rect(0xFFCFC3A6, bu0 - 5, bv0 - 5, bu1 + 5, bv1 + 5);
                f.rect(0xFFCFC3A6, 0, S - 4, L, S);
                // The portico: a stone floor, two rows of column tops, then the steps.
                float pu0 = L * 0.32f, pu1 = L * 0.68f;
                f.rect(0xFFDCD0B4, pu0, bv1, pu1, bv1 + 10);
                f.rect(0x30000000, pu0, bv1, pu1, bv1 + 1.5f);
                int nc = Math.max(4, (int) ((pu1 - pu0 - 4) / 7));
                for (int row = 0; row < 2; row++)
                    for (int k = 0; k <= nc; k++) {
                        float u = pu0 + 3 + k * (pu1 - pu0 - 6) / nc, v = bv1 + 3 + row * 4.5f;
                        f.circle(0x40000000, u + 1, v + 1, 1.9f);
                        f.circle(0xFFEDE4CC, u, v, 1.8f);
                        f.circle(0xFFC4B898, u + 0.5f, v + 0.5f, 0.9f);
                    }
                steps(f, pu0, bv1 + 10, pu1, S - 4, Math.max(3, (int) ((S - 14 - bv1) / 3)), 0xFFD8CCB0);
                f.lamp(pu0 - 5, S - 6);
                f.lamp(pu1 + 5, S - 6);
                // The dinosaur on the one lawn, an ammonite bed on the other.
                dino(f, 4, bv1 + 7, pu0 - 16, S - 6);
                float ac = (pu1 + L) / 2 + 2, av = (bv1 + 5 + S - 4) / 2, R = Math.min((L - pu1) / 2 - 8, (S - 4 - bv1 - 5) / 2 - 3);
                if (R > 5) {
                    f.circle(0xFF8B6B4A, ac, av, R);
                    for (int i = 0; i < 20; i++) {
                        double a = i * 0.55;
                        float rr = R * 0.78f * (float) Math.exp(-0.11 * i);
                        f.circle(i % 2 == 0 ? 0xFFD08A3A : 0xFFE8C460, ac + (float) Math.cos(a) * rr * 0.75f,
                                av + (float) Math.sin(a) * rr * 0.75f, rr * 0.42f);
                    }
                    f.ring(0xFFB8AC90, 1.2f, ac, av, R);
                }
                // Cedars down the ends of the hall.
                for (float v = bv0 + 8; v < bv1; v += 22) {
                    f.tree(T * 0.9f, v, 6.5f, 0xFF2E5E36);
                    f.tree(L - T * 0.9f, v, 6.5f, 0xFF2E5E36);
                }
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFFB0926C, 0, 0, L, S);
                float pav = Math.min(L * 0.16f, S * 0.8f);
                gable(f, 0xFF5A6672, pav, 2, L - pav, S - 2);
                for (float u = pav + 4; u < L - pav; u += 5) {
                    f.line(0x22000000, 0.5f, u, 2, u, S * 0.42f);
                    f.line(0x22000000, 0.5f, u, S * 0.58f, u, S - 2);
                }
                // The glazed ridge.
                f.rect(0x40000000, pav + 3, S * 0.42f, L - pav - 3, S * 0.6f);
                f.rect(0xFF9CC2D4, pav + 3, S * 0.4f, L - pav - 3, S * 0.58f);
                f.rect(0xFFC4DCE6, pav + 3, S * 0.4f, L - pav - 3, S * 0.48f);
                for (float u = pav + 6; u < L - pav - 3; u += 4) f.line(0xFF4E5E68, 0.5f, u, S * 0.4f, u, S * 0.58f);
                f.line(0xFF4E5E68, 0.8f, pav + 3, S * 0.49f, L - pav - 3, S * 0.49f);
                // End pavilions with their own hipped roofs and finials.
                for (int e = 0; e < 2; e++) {
                    float u0 = e == 0 ? 0 : L - pav, u1 = u0 + pav;
                    f.rect(0xFFC4A880, u0, 0, u1, S);
                    hip(f, 0xFF4E5A66, u0 + 2, 2, u1 - 2, S - 2, false);
                    f.circle(0xFFD8B860, (u0 + u1) / 2, S / 2, 1.3f);
                }
                // The portico's pediment roof on the front.
                float qu0 = L * 0.4f, qu1 = L * 0.6f, qm = L / 2, qv = S * 0.64f;
                f.rect(0x40000000, qu0 + 2, qv, qu1 + 2, S);
                f.rect(0xFFC9AE88, qu0, qv, qu1, S);
                f.poly(0xFF6A7682, qu0 + 1.5f, qv + 1.5f, qm, qv + 1.5f, qm, S, qu0 + 1.5f, S);
                f.poly(0xFF4C5662, qm, qv + 1.5f, qu1 - 1.5f, qv + 1.5f, qu1 - 1.5f, S, qm, S);
                f.line(0xFFA8B2BA, 1, qm, qv + 1.5f, qm, S);
                f.sign(city, "MUSEUM", L / 2, S * 0.25f, 6, 0xFF3A2A1A);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.DOWN | Variants.MID | Variants.OLD | Variants.UNI;
        d.minS = 10;
        d.minL = 12;
        d.base = City.OFFICE;
        d.inside = Variants.I_SHOWROOM;
        d.roofCol = 0xFF5A6672;
        out.add(d);
    }

    // ------------------------------------------------------------------ Planetarium

    static void planetarium(List<Landmarks.Def> out) {
        // A big silver dome with an observatory turret beside it and a paved orrery: orbits, planets and stars.
        Landmarks.Def d = new Landmarks.Def("Planetarium", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int l = Math.max(bw, bh), s = Math.min(bw, bh), ds = Math.max(5, Math.min(s - 3, l - 6));
                boolean al = bw >= bh;
                return new int[][]{at(al, 1, (s - ds) / 2, ds, ds, 3), at(al, l - 3, 1, 2, 2, 4)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, T = TT;
                int st = Math.round(S / T), lt = Math.round(L / T), ds = Math.max(5, Math.min(st - 3, lt - 6));
                float du0 = T, du1 = (1 + ds) * T, dv0 = ((st - ds) / 2) * T, dv1 = dv0 + ds * T;
                float dc = (du0 + du1) / 2, dvc = (dv0 + dv1) / 2;
                f.rect(0xFFB4B4AE, 0, 0, L, S);
                for (float u = 12; u < L; u += 12) f.line(0x14000000, 0.5f, u, 0, u, S);
                c.save();
                f.clip(0, 0, L, S);
                f.circle(0xFFC8C6BE, dc, dvc, ds * T * 0.7f);
                f.ring(0xFF9A988E, 1.2f, dc, dvc, ds * T * 0.7f);
                c.restore();
                // The orrery: a night-blue disc, star dots, brass orbits and planets round a golden sun.
                float ou0 = du1 + T * 0.5f, ou1 = L - 3 * T - 4 - ou0 > S * 0.7f ? L - 3 * T - 4 : L - 2, oc = (ou0 + ou1) / 2, ov = S / 2;
                float R = Math.min((ou1 - ou0) / 2, S / 2) - 4;
                f.line(0xFFC8A860, 1, 0, ov, L, ov);
                if (R > 12) {
                    f.circle(0xFF9A988E, oc, ov, R + 2);
                    f.circle(0xFF343C52, oc, ov, R);
                    for (int k = 0; k < 26; k++) {
                        double a = r.nextDouble() * Math.PI * 2;
                        float rr = (float) Math.sqrt(r.nextDouble()) * R * 0.95f;
                        f.circle(0xC0FFFFFF, oc + (float) Math.cos(a) * rr, ov + (float) Math.sin(a) * rr, 0.35f + r.nextFloat() * 0.5f);
                    }
                    for (int k = 0; k < 24; k++) {
                        double a = k * Math.PI / 12;
                        float c0 = (float) Math.cos(a), s0 = (float) Math.sin(a), in = k % 2 == 0 ? 0.88f : 0.93f;
                        f.line(0xFFC8A860, 0.6f, oc + c0 * R * in, ov + s0 * R * in, oc + c0 * R, ov + s0 * R);
                    }
                    int[] pc = {0xFFB0A090, 0xFFE0C080, 0xFF4A8AC8, 0xFFC8583A, 0xFFD8B080, 0xFFE8D8A8};
                    for (int i = 1; i <= 5; i++) {
                        float rr = R * (0.18f + i * 0.14f);
                        f.ring(0xFFC8A860, 0.6f, oc, ov, rr);
                        double a = r.nextDouble() * Math.PI * 2;
                        float pu = oc + (float) Math.cos(a) * rr, pv = ov + (float) Math.sin(a) * rr, ps = 1.1f + (i == 4 ? 1.4f : i * 0.25f);
                        f.circle(0x50000000, pu + 0.6f, pv + 0.6f, ps);
                        f.circle(pc[i], pu, pv, ps);
                        if (i == 4) f.ring(0xFFE8D8A8, 0.5f, pu, pv, ps + 1.2f);
                    }
                    for (int k = 0; k < 8; k++) {
                        double a = k * Math.PI / 4;
                        f.line(0xFFE8B840, 0.8f, oc, ov, oc + (float) Math.cos(a) * R * 0.15f, ov + (float) Math.sin(a) * R * 0.15f);
                    }
                    f.circle(0xFFE8B840, oc, ov, R * 0.09f);
                    f.circle(0xFFF8E098, oc - R * 0.02f, ov - R * 0.02f, R * 0.05f);
                    for (int k = 0; k < 4; k++) {
                        double a = Math.PI / 4 + k * Math.PI / 2;
                        float bu = oc + (float) Math.cos(a) * (R + 7), bv = ov + (float) Math.sin(a) * (R + 7);
                        if (bu > ou0 - 4 && bu < L - 4 && bv > 5 && bv < S - 5) f.bench(bu, bv, k % 2 == 0);
                    }
                }
                f.tree(5, 5, 4.5f);
                f.tree(5, S - 5, 4.5f);
                f.tree(L - 6, S - 6, 5);
                f.lamp(du1 + 4, 4);
                f.lamp(du1 + 4, S - 4);
                f.lamp(L - 4, S / 2 - 8);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, cu = L / 2, cv = S / 2;
                if (part == 0) {
                    f.rect(0xFF8E9296, 0, 0, L, S);
                    f.rect(0xFFAEB2B4, 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                    for (int k = 0; k < 4; k++) {
                        float u = k % 2 == 0 ? 5 : L - 9, v = k < 2 ? 5 : S - 9;
                        f.rect(0x30000000, u + 1, v + 1, u + 5, v + 5);
                        f.rect(0xFF8A8E92, u, v, u + 4, v + 4);
                    }
                    float rad = Math.min(L, S) * 0.46f;
                    f.circle(0x48000000, cu + 3, cv + 3, rad);
                    f.circle(0xFF6E747C, cu, cv, rad);
                    dome(f, cu, cv, rad * 0.93f, 0xFFB8C0C8, 20);
                    for (int k = 0; k < 20; k++) {
                        double a = k * Math.PI / 10;
                        f.line(0x30000000, 0.5f, cu, cv, cu + (float) Math.cos(a) * rad * 0.93f, cv + (float) Math.sin(a) * rad * 0.93f);
                    }
                    f.ring(0x30000000, 0.6f, cu, cv, rad * 0.35f);
                    f.ring(0x30000000, 0.6f, cu, cv, rad * 0.64f);
                    f.ring(0x26FFFFFF, 0.6f, cu - 0.5f, cv - 0.5f, rad * 0.86f);
                    f.circle(0xFF7E848C, cu, cv, rad * 0.09f);
                    f.circle(0xFFDCE2E6, cu - 0.5f, cv - 0.5f, rad * 0.05f);
                    f.sign(city, "PLANETARIUM", cu, cv + rad * 0.5f, 5, 0xFF303848);
                } else {
                    f.rect(0xFF7A5A48, 0, 0, L, S);
                    f.rect(0xFF9A7660, 1.2f, 1.2f, L - 1.2f, S - 1.2f);
                    float rad = Math.min(L, S) * 0.42f;
                    f.circle(0x48000000, cu + 2, cv + 2, rad);
                    dome(f, cu, cv, rad, 0xFFDCDEE0, 12);
                    p.setStrokeCap(Paint.Cap.BUTT);
                    f.line(0xFF2A2E34, rad * 0.32f, cu, cv, cu - rad * 0.68f, cv - rad * 0.68f);
                    f.line(0xFF6A7078, rad * 0.12f, cu - rad * 0.1f, cv - rad * 0.1f, cu - rad * 0.55f, cv - rad * 0.55f);
                    f.circle(0xFF9AA0A6, cu, cv, rad * 0.14f);
                }
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.MID | Variants.UNI | Variants.PARK;
        d.minS = 10;
        d.minL = 12;
        d.base = City.OFFICE;
        d.inside = Variants.I_SEATS;
        d.roofCol = 0xFFB8C0C8;
        out.add(d);
    }

    // ------------------------------------------------------------------ Grand Hotel

    /** A mansard: slate slopes with dormers round a flat deck; returns the deck's inset. */
    static float mansard(F f, int deck) {
        float L = f.L, S = f.S, m = Math.min(7, S * 0.18f);
        f.rect(0xFF3A4048, 0, 0, L, S);
        f.poly(0xFF5E6670, 0, 0, L, 0, L - m, m, m, m);
        f.poly(0xFF545C66, 0, 0, 0, S, m, S - m, m, m);
        f.poly(0xFF464D56, L, 0, L, S, L - m, S - m, L - m, m);
        f.poly(0xFF3C434C, 0, S, L, S, L - m, S - m, m, S - m);
        for (float u = m + 5; u < L - m - 3; u += 12) {
            f.rect(0xFFDCD4C4, u - 1.6f, 1.2f, u + 1.6f, m - 1.4f);
            f.rect(0xFF8CA4B0, u - 1, 2, u + 1, m - 2);
            f.rect(0xFFDCD4C4, u - 1.6f, S - m + 1.4f, u + 1.6f, S - 1.2f);
            f.rect(0xFF6E8490, u - 1, S - m + 2, u + 1, S - 2);
        }
        for (float v = m + 5; v < S - m - 3; v += 12) {
            f.rect(0xFFDCD4C4, 1.2f, v - 1.6f, m - 1.4f, v + 1.6f);
            f.rect(0xFF8CA4B0, 2, v - 1, m - 2, v + 1);
            f.rect(0xFFDCD4C4, L - m + 1.4f, v - 1.6f, L - 1.2f, v + 1.6f);
            f.rect(0xFF6E8490, L - m + 2, v - 1, L - 2, v + 1);
        }
        f.rect(0xFFD8CCB0, m - 1, m - 1, L - m + 1, S - m + 1);
        f.rect(deck, m, m, L - m, S - m);
        return m;
    }

    static void grandHotel(List<Landmarks.Def> out) {
        // An L of mansard-roofed blocks with a rooftop pool, a canopied entrance on a carriage loop, and gardens.
        Landmarks.Def d = new Landmarks.Def("Grand Hotel", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int l = Math.max(bw, bh), s = Math.min(bw, bh), lv = s >= 13 ? 4 : 3, ww = s >= 13 ? 5 : 4;
                boolean al = bw >= bh;
                return new int[][]{at(al, 1, 1, l - 2, lv, 8), at(al, l - 1 - ww, 1 + lv, ww, s - 4 - lv, 7)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, T = TT;
                int st = Math.round(S / T), lt = Math.round(L / T), lv = st >= 13 ? 4 : 3, ww = st >= 13 ? 5 : 4;
                float mv1 = (1 + lv) * T, wu0 = (lt - 1 - ww) * T, wv1 = (st - 3) * T;
                f.rect(0xFF5E9A48, 0, 0, L, S);
                f.rect(0xFFD4CCBA, 0, 0, L, T - 3);
                f.rect(0xFFD4CCBA, wu0 - 6, mv1, L, S);
                for (float u = wu0 - 6; u < L; u += 8) f.line(0x18000000, 0.5f, u, wv1, u, S);
                // Café tables in front of the wing.
                for (float u = wu0 + 4; u < L - 6; u += 13) {
                    float v = (wv1 + S) / 2;
                    f.circle(0x30000000, u + 1.5f, v + 1.5f, 4);
                    f.circle(k(u) ? 0xFFE8E0D0 : 0xFF2E6A5A, u, v, 4);
                    f.circle(0xFFB8B0A0, u, v, 0.8f);
                }
                // The carriage loop up to the canopy, a fountain on its island, flags.
                float gu1 = wu0 - 6, cu = gu1 * 0.55f;
                f.rect(0xFF6E6E70, cu - 26, mv1, cu + 26, mv1 + 14);
                f.rect(0xFF6E6E70, cu - 26, mv1, cu - 17, S);
                f.rect(0xFF6E6E70, cu + 17, mv1, cu + 26, S);
                f.rect(0xFFD4CCBA, cu - 28, mv1 + 14, cu - 26, S);
                f.rect(0xFFD4CCBA, cu + 26, mv1 + 14, cu + 28, S);
                f.rect(0xFF9A9284, cu - 17, mv1 + 14, cu + 17, S);
                f.rect(0xFF6AA852, cu - 15, mv1 + 16, cu + 15, S - 2);
                float fv = (mv1 + 16 + S - 2) / 2, fr = Math.min(10, (S - mv1 - 18) / 2 - 1);
                if (fr > 3) {
                    f.circle(0xFFB8B0A0, cu, fv, fr);
                    f.circle(0xFF4A90B8, cu, fv, fr - 1.5f);
                    f.circle(0xFF6AAAD0, cu, fv, (fr - 1.5f) * 0.6f);
                    f.circle(0xFFD0C8B8, cu, fv, 1.2f);
                }
                for (int k = 0; k < 4; k++) {
                    f.bush(cu - 13 + k * 3, S - 4, 1.4f);
                    f.bush(cu + 13 - k * 3, S - 4, 1.4f);
                }
                f.rect(0x40000000, cu - 10, mv1, cu + 13, mv1 + 12);
                f.rect(0xFF7A2626, cu - 12, mv1, cu + 12, mv1 + 10);
                f.rect(0xFF9A3434, cu - 12, mv1, cu + 12, mv1 + 4);
                f.frame(0xFFD8B860, 0.6f, cu - 12, mv1, cu + 12, mv1 + 10);
                for (int k = 0; k < 3; k++) {
                    float u = cu - 8 + k * 8, v = mv1 + 16;
                    f.circle(0xFF505050, u, v, 0.8f);
                    f.rect(new int[]{0xFFB83A30, 0xFF2E4A8A, 0xFFE8C860}[k], u + 0.6f, v - 1, u + 5.5f, v + 1.8f);
                }
                // The garden: a parterre of hedges and beds, trees, benches.
                float g0 = 6, g1 = cu - 32;
                if (g1 - g0 > 20) {
                    float v0 = mv1 + 6, v1 = S - 6;
                    f.rect(0xFFD9CDB0, g0, v0, g1, v1);
                    f.frame(0xFF2E5E2E, 2.5f, g0 + 2, v0 + 2, g1 - 2, v1 - 2);
                    float gm = (g0 + g1) / 2, vm = (v0 + v1) / 2;
                    for (int q = 0; q < 4; q++) {
                        float a0 = q % 2 == 0 ? g0 + 5 : gm + 2.5f, a1 = q % 2 == 0 ? gm - 2.5f : g1 - 5;
                        float b0 = q < 2 ? v0 + 5 : vm + 2.5f, b1 = q < 2 ? vm - 2.5f : v1 - 5;
                        if (a1 - a0 < 4 || b1 - b0 < 4) continue;
                        f.rect(0xFF3E7A3A, a0, b0, a1, b1);
                        f.rect(q % 2 == 0 ? 0xFFC8506A : 0xFFE8C850, a0 + 1.5f, b0 + 1.5f, a1 - 1.5f, b1 - 1.5f);
                        f.rect(0xFF5A9A48, a0 + 3, b0 + 3, a1 - 3, b1 - 3);
                    }
                    f.circle(0xFFE8E4DA, gm, vm, 2.5f);
                }
                f.tree(cu - 34 > 8 ? 6 : 4, S - 5, 5);
                f.tree(gu1 - 4, S - 6, 5.5f);
                f.tree(gu1 - 4, mv1 + 8, 5);
                f.lamp(cu - 27, S - 3);
                f.lamp(cu + 27, S - 3);
                f.bench(gu1 - 10, (mv1 + S) / 2, false);
            }

            boolean k(float u) {
                return ((int) u / 13) % 2 == 0;
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                if (part == 0) {
                    float m = mansard(f, 0xFF9E978A);
                    f.rect(0x40000000, L * 0.3f + 2, S * 0.3f + 2, L * 0.3f + 14, S - m - 1);
                    f.rect(0xFFB8B0A0, L * 0.3f, S * 0.3f, L * 0.3f + 12, S - m - 3);
                    f.rect(0xFFCCC4B4, L * 0.3f + 1, S * 0.3f + 1, L * 0.3f + 11, S * 0.3f + 4);
                    for (int k = 0; k < 3; k++) {
                        float u = L * 0.5f + k * 9;
                        if (u > L - m - 8) break;
                        f.rect(0x30000000, u + 1, m + 3, u + 7, m + 9);
                        f.rect(0xFF8A8E92, u, m + 2, u + 6, m + 8);
                        f.circle(0xFF5E6266, u + 3, m + 5, 2);
                    }
                    f.circle(0x40000000, m + 8, S / 2 + 1, 4.5f);
                    f.circle(0xFF8A6A4A, m + 7, S / 2, 4.5f);
                    f.ring(0xFF5E4630, 0.6f, m + 7, S / 2, 3);
                    f.sign(city, "GRAND HOTEL", L * 0.62f, S / 2, 6, 0xFF7A2626);
                } else {
                    float m = mansard(f, 0xFFB48C62);
                    for (float v = m + 2; v < S - m; v += 2) f.line(0x22000000, 0.4f, m, v, L - m, v);
                    float pu0 = m + 4, pu1 = L - m - 4, pv0 = m + 4, pv1 = pv0 + Math.max(6, (S - 2 * m) * 0.38f);
                    f.rect(0xFFEAE6DC, pu0 - 1.5f, pv0 - 1.5f, pu1 + 1.5f, pv1 + 1.5f);
                    f.rect(0xFF3FA6C8, pu0, pv0, pu1, pv1);
                    f.rect(0xFF2F8EB4, pu0 + 2, pv0 + 2, pu1 - 2, pv1 - 1);
                    for (int k = 0; k < 4; k++) {
                        float u = pu0 + 3 + r.nextFloat() * (pu1 - pu0 - 8), v = pv0 + 2 + r.nextFloat() * (pv1 - pv0 - 4);
                        f.line(0x70FFFFFF, 0.5f, u, v, u + 4, v);
                    }
                    float lv = pv1 + 4;
                    for (float u = pu0; u + 3 <= pu1 && lv + 7 < S - m; u += 7) {
                        f.rect(0x30000000, u + 0.8f, lv + 0.8f, u + 3.8f, lv + 7.8f);
                        f.rect(0xFFF2F0EA, u, lv, u + 3, lv + 7);
                        f.rect(0xFFD8D2C4, u, lv, u + 3, lv + 1.8f);
                    }
                    for (float u = pu0 + 6.5f; u < pu1; u += 14) {
                        float v = lv + 7 + 3;
                        if (v + 3 > S - m) break;
                        f.circle(0x40000000, u + 1.5f, v + 1.5f, 4);
                        for (int k = 0; k < 8; k++) {
                            double a0 = k * Math.PI / 4, a1 = (k + 1) * Math.PI / 4;
                            f.poly(k % 2 == 0 ? 0xFFF0ECE0 : 0xFF2E8A84, u, v, u + (float) Math.cos(a0) * 4,
                                    v + (float) Math.sin(a0) * 4, u + (float) Math.cos(a1) * 4, v + (float) Math.sin(a1) * 4);
                        }
                    }
                    f.bush(m + 2.5f, m + 2.5f, 2);
                    f.bush(L - m - 2.5f, m + 2.5f, 2);
                }
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.DOWN | Variants.MID | Variants.OLD;
        d.minS = 10;
        d.minL = 12;
        d.base = City.OFFICE;
        d.inside = Variants.I_HOTEL;
        d.roofCol = 0xFF4E5560;
        out.add(d);
    }

    // ------------------------------------------------------------------ Concert Hall

    static void concertHall(List<Landmarks.Def> out) {
        // A modernist hall under folded zinc roof planes on a glass base, and a plaza with an outdoor band shell.
        Landmarks.Def d = new Landmarks.Def("Concert Hall", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int l = Math.max(bw, bh), s = Math.min(bw, bh);
                return new int[][]{at(bw >= bh, 1, 1, l - 1 - Math.max(5, l * 2 / 5), s - 2, 4)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, T = TT;
                int lt = Math.round(L / T);
                float hu1 = (lt - Math.max(5, lt * 2 / 5)) * T;
                f.rect(0xFFD2CCC0, 0, 0, L, S);
                c.save();
                f.clip(0, 0, L, S);
                for (float a = -S; a < L; a += 11) {
                    f.line(0x1C000000, 0.5f, a, 0, a + S, S);
                    f.line(0x12000000, 0.5f, a + S, 0, a, S);
                }
                c.restore();
                f.rect(0xFFB8B2A6, T - 4, T - 4, hu1 + 4, S - T + 4);
                // The band shell at the far end, the stage before it, rows of seats curving round.
                float pu0 = hu1 + 8, rs = Math.min(S * 0.24f, (L - pu0) * 0.32f), sc = L - 3 - rs * 0.85f, sv = S / 2;
                c.save();
                f.clip(pu0 - 2, 12, sc - rs * 0.75f, S - 12);
                for (float rr = rs * 0.95f; rr < sc - pu0 + 2; rr += 5.5f) {
                    f.ring(0x40000000, 2.2f, sc + 0.8f, sv + 0.8f, rr);
                    f.ring(0xFF8A6A48, 2.2f, sc, sv, rr);
                }
                c.restore();
                c.save();
                f.clip(0, 0, sc, S);
                f.circle(0x40000000, sc + 1.5f, sv + 1.5f, rs * 0.62f);
                f.circle(0xFF6A5444, sc, sv, rs * 0.62f);
                for (float u = sc - rs * 0.6f; u < sc; u += 2.2f) f.line(0x30000000, 0.4f, u, sv - rs * 0.6f, u, sv + rs * 0.6f);
                c.restore();
                c.save();
                f.clip(sc, 0, L, S);
                f.circle(0x48000000, sc + 2.5f, sv + 2.5f, rs);
                dome(f, sc, sv, rs, 0xFFE4E2DC, 14);
                for (int k = 0; k < 8; k++) {
                    double a = -Math.PI / 2 + k * Math.PI / 7;
                    f.line(0x30000000, 0.5f, sc, sv, sc + (float) Math.cos(a) * rs, sv + (float) Math.sin(a) * rs);
                }
                c.restore();
                f.line(0xFF8E8C86, 1, sc, sv - rs, sc, sv + rs);
                // Light masts, trees down the sides, lamps.
                f.circle(0xFF505458, pu0 + 2, 9, 1.5f);
                f.circle(0xFF505458, pu0 + 2, S - 9, 1.5f);
                for (float u = pu0 + 10; u < L - 6; u += 18) {
                    f.tree(u, 6, 4.5f);
                    f.tree(u, S - 6, 4.5f);
                }
                for (float v = T + 6; v < S - T; v += 20) f.lamp(hu1 + 5, v);
                f.sign(city, "STAGE", sc + rs * 0.5f, sv, 4, 0xFF303030);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, g = 3;
                // The glass base showing round the roof's edge.
                f.rect(0xFF5A6A74, 0, 0, L, S);
                f.rect(0xFF8AA8B8, 0.8f, 0.8f, L - 0.8f, S - 0.8f);
                for (float u = 4; u < L; u += 4) {
                    f.line(0xFF50606A, 0.4f, u, 0, u, g);
                    f.line(0xFF50606A, 0.4f, u, S - g, u, S);
                }
                // Folded metal planes: columns of jittered facets meeting on a crooked ridge.
                int nc = Math.max(3, Math.round(L / 34));
                float[] ut = new float[nc + 1], ub = new float[nc + 1], um = new float[nc + 1], vm = new float[nc + 1];
                for (int i = 0; i <= nc; i++) {
                    float base = g + (L - 2 * g) * i / nc, j = i > 0 && i < nc ? (L / nc) * 0.18f : 0;
                    ut[i] = base + (r.nextFloat() * 2 - 1) * j;
                    ub[i] = base + (r.nextFloat() * 2 - 1) * j;
                    um[i] = base + (r.nextFloat() * 2 - 1) * j;
                    vm[i] = S * (0.38f + 0.24f * r.nextFloat());
                }
                int[] zinc = {0xFFA8B0B8, 0xFF959DA6, 0xFFBCC2C8, 0xFF858D96};
                float v0 = g, v1 = S - g;
                for (int i = 0; i < nc; i++) {
                    int a = zinc[r.nextInt(4)], b = zinc[r.nextInt(4)];
                    f.poly(City.lighten(a, 0.12f), ut[i], v0, ut[i + 1], v0, um[i + 1], vm[i + 1]);
                    f.poly(a, ut[i], v0, um[i + 1], vm[i + 1], um[i], vm[i]);
                    f.poly(City.darken(b, 0.86f), um[i], vm[i], um[i + 1], vm[i + 1], ub[i + 1], v1);
                    f.poly(City.darken(b, 0.74f), um[i], vm[i], ub[i + 1], v1, ub[i], v1);
                }
                for (int i = 0; i < nc; i++) {
                    f.line(0xFF4E565E, 0.6f, ut[i], v0, um[i + 1], vm[i + 1]);
                    f.line(0xFF4E565E, 0.6f, um[i], vm[i], ub[i + 1], v1);
                    f.line(0xFFE0E4E8, 0.8f, um[i], vm[i], um[i + 1], vm[i + 1]);
                    if (i > 0) {
                        f.line(0xFF4E565E, 0.7f, ut[i], v0, um[i], vm[i]);
                        f.line(0xFF4E565E, 0.7f, um[i], vm[i], ub[i], v1);
                    }
                }
                f.frame(0xFF4A525A, 0.8f, g, g, L - g, S - g);
                // The fly tower rising out of it.
                float fu0 = L * 0.58f, fu1 = L * 0.78f, fv0 = S * 0.22f, fv1 = S * 0.62f;
                f.rect(0x48000000, fu0 + 3, fv0 + 3, fu1 + 4, fv1 + 4);
                f.rect(0xFF5E646A, fu0, fv0, fu1, fv1);
                f.rect(0xFF7E848A, fu0 + 1.2f, fv0 + 1.2f, fu1 - 1.2f, fv1 - 1.2f);
                for (float v = fv0 + 4; v < fv1 - 3; v += 3) f.line(0xFF5E646A, 0.5f, fu0 + 3, v, fu1 - 3, v);
                f.sign(city, "CONCERT HALL", L * 0.3f, S / 2, 5, 0xFF26303A);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.MID | Variants.UNI;
        d.minS = 10;
        d.minL = 12;
        d.base = City.OFFICE;
        d.inside = Variants.I_SEATS;
        d.roofCol = 0xFFA8B0B8;
        out.add(d);
    }

    // ------------------------------------------------------------------ Cathedral

    /** {nave width, tower depth, nave length, choir length} in tiles for a block l by s. */
    static int[] cathedralSizes(int l, int s) {
        int nw = s >= 13 ? 4 : 3, tw = 3, rem = l - 2 - tw - 3, ch = Math.max(2, rem / 3);
        return new int[]{nw, tw, rem - ch, ch};
    }

    static void cathedral(List<Landmarks.Def> out) {
        // A cross of nave, transept and apse behind twin west towers, lead roofs, a crossing spire, and a cloister.
        Landmarks.Def d = new Landmarks.Def("Cathedral", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int l = Math.max(bw, bh), s = Math.min(bw, bh);
                int[] z = cathedralSizes(l, s);
                int nw = z[0], tw = z[1], nv = z[2], ch = z[3];
                boolean al = bw >= bh;
                return new int[][]{at(al, 1, 2, tw, nw + 2, 6), at(al, 1 + tw, 3, nv, nw, 4),
                        at(al, 1 + tw + nv, 1, 3, nw + 4, 4), at(al, 4 + tw + nv, 3, ch, nw, 3)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, T = TT;
                int[] z = cathedralSizes(Math.round(L / T), Math.round(S / T));
                int nw = z[0], tw = z[1], nv = z[2];
                float nu0 = (1 + tw) * T, nu1 = (1 + tw + nv) * T, tu1 = nu1 + 3 * T, nv0 = 3 * T, nv1 = (3 + nw) * T;
                f.rect(0xFF5A9446, 0, 0, L, S);
                // Gravel walks round the walls, a paved parvis before the west front.
                int gr = 0xFFCFC2A6;
                f.rect(gr, T - 5, 2 * T - 5, nu0 + 2, (nw + 4) * T + 5);
                f.rect(gr, nu0, nv0 - 5, L - T + 5, nv1 + 5);
                f.rect(gr, nu1 - 5, T - 5, tu1 + 5, (nw + 5) * T + 5);
                f.rect(0xFFD8D0BC, 0, 2 * T - 5, T, (nw + 4) * T + 5);
                for (float v = 2 * T - 1; v < (nw + 4) * T + 5; v += 6) f.line(0x20000000, 0.5f, 0, v, T - 5, v);
                f.rect(0xFFD8D0BC, 0, (nw + 4) * T + 5, T, S);
                // Buttresses down the nave and choir.
                for (float u = nu0 + 6; u < L - T - 3; u += 12) {
                    if (u > nu1 - 3 && u < tu1 + 3) continue;
                    f.rect(0x40000000, u - 1, nv0 - 6, u + 2.5f, nv0);
                    f.rect(0xFF9C9282, u - 1.5f, nv0 - 6, u + 1.5f, nv0);
                    f.circle(0xFFB8AE9C, u, nv0 - 6, 1.6f);
                    f.rect(0x40000000, u - 1, nv1, u + 2.5f, nv1 + 5);
                    f.rect(0xFF9C9282, u - 1.5f, nv1, u + 1.5f, nv1 + 4);
                }
                // The churchyard on the far side: gravestones and yews.
                for (float u = nu0 + 3; u < nu1 - 6; u += 7)
                    for (float v = 4; v < nv0 - 12; v += 7) {
                        float ju = u + r.nextFloat() * 2, jv = v + r.nextFloat() * 1.5f;
                        f.rect(0x40000000, ju + 0.8f, jv + 0.8f, ju + 3.8f, jv + 2.8f);
                        f.rect(0xFF9A9890, ju, jv, ju + 3, jv + 2);
                        f.rect(0xFFB4B2AA, ju, jv, ju + 3, jv + 0.8f);
                    }
                f.tree(nu0 - 4 > 10 ? nu0 - 6 : nu0 + 4, 7, 5.5f, 0xFF2A5430);
                f.tree(nu1 - 4, 7, 5, 0xFF2A5430);
                f.tree(tu1 + 10, 8, 6);
                f.tree(L - 8, S - 8, 6.5f);
                if (L - tu1 > 30) f.tree(tu1 + 10, S - 10, 5.5f);
                // The cloister: a lead-roofed walk round a garth with crossing paths and a well.
                float cu0 = nu0 + 3, cu1 = nu1 - 3, cv0 = nv1 + 5, cv1 = S - 4, w = 7;
                if (cv1 - cv0 > 2 * w + 8 && cu1 - cu0 > 2 * w + 8) {
                    f.rect(0x40000000, cu0 + 2, cv0 + 2, cu1 + 2, cv1 + 2);
                    f.rect(0xFF6A7480, cu0, cv0, cu1, cv1);
                    f.rect(0xFF7C8692, cu0, cv0, cu1, cv0 + w / 2);
                    f.rect(0xFF7C8692, cu0, cv0, cu0 + w / 2, cv1);
                    f.rect(0xFF5A636E, cu0, cv1 - w / 2, cu1, cv1);
                    f.rect(0xFF5A636E, cu1 - w / 2, cv0, cu1, cv1);
                    float gu0 = cu0 + w, gu1 = cu1 - w, gv0 = cv0 + w, gv1 = cv1 - w, gm = (gu0 + gu1) / 2, vm = (gv0 + gv1) / 2;
                    f.rect(0xFFD8CFBC, gu0 - 1.5f, gv0 - 1.5f, gu1 + 1.5f, gv1 + 1.5f);
                    f.rect(0xFF6AA852, gu0, gv0, gu1, gv1);
                    f.rect(0x30000000, gu0, gv0, gu1, gv0 + 1.5f);
                    for (float u = gu0; u <= gu1 + 0.1f; u += (gu1 - gu0) / Math.max(2, Math.round((gu1 - gu0) / 5))) {
                        f.circle(0xFFE4DCCA, u, gv0 - 0.8f, 1);
                        f.circle(0xFFE4DCCA, u, gv1 + 0.8f, 1);
                    }
                    for (float v = gv0; v <= gv1 + 0.1f; v += (gv1 - gv0) / Math.max(2, Math.round((gv1 - gv0) / 5))) {
                        f.circle(0xFFE4DCCA, gu0 - 0.8f, v, 1);
                        f.circle(0xFFE4DCCA, gu1 + 0.8f, v, 1);
                    }
                    f.rect(0xFFD8CCB0, gm - 1.5f, gv0, gm + 1.5f, gv1);
                    f.rect(0xFFD8CCB0, gu0, vm - 1.5f, gu1, vm + 1.5f);
                    f.circle(0xFF8E8676, gm, vm, 3.5f);
                    f.circle(0xFF2F6A94, gm, vm, 2.2f);
                    for (int q = 0; q < 4; q++)
                        f.bush(q % 2 == 0 ? (gu0 + gm) / 2 : (gm + gu1) / 2, q < 2 ? (gv0 + vm) / 2 : (vm + gv1) / 2, 2.2f);
                }
                f.lamp(T - 2, 2 * T - 8);
                f.lamp(T - 2, (nw + 4) * T + 8);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int lead = 0xFF5E6A78;
                f.rect(0xFF8E8676, 0, 0, L, S);
                if (part == 0) {
                    // Twin towers with crenellated parapets and pyramid spires, a gable between them.
                    float q = Math.min(S, L * 0.42f), qv0 = (S - q) / 2;
                    gable(f, lead, q, 2, L - q, S - 2);
                    for (int k = 0; k < 2; k++) {
                        float u0 = k == 0 ? 0 : L - q;
                        f.rect(0x48000000, u0 + 2, qv0 + 2, u0 + q + 2, qv0 + q + 2);
                        f.rect(0xFF9E9484, u0, qv0, u0 + q, qv0 + q);
                        crenels(f, 0xFFB8AE9C, u0, qv0, u0 + q, qv0 + q, q / 11);
                        float a = q * 0.2f, cu = u0 + q / 2, cv = qv0 + q / 2;
                        f.poly(0xFF7E8A96, u0 + a, qv0 + a, u0 + q - a, qv0 + a, cu, cv);
                        f.poly(0xFF5A6572, u0 + q - a, qv0 + a, u0 + q - a, qv0 + q - a, cu, cv);
                        f.poly(0xFF4A5462, u0 + a, qv0 + q - a, u0 + q - a, qv0 + q - a, cu, cv);
                        f.poly(0xFF6E7A88, u0 + a, qv0 + a, u0 + a, qv0 + q - a, cu, cv);
                        f.circle(0xFFD8B860, cu, cv, 1.2f);
                        for (int j = 0; j < 4; j++)
                            f.circle(0xFFC8BEAA, j % 2 == 0 ? u0 + a * 0.5f : u0 + q - a * 0.5f, j < 2 ? qv0 + a * 0.5f : qv0 + q - a * 0.5f, a * 0.35f);
                    }
                    f.sign(city, "CATHEDRAL", L / 2, S / 2, 5, 0xFF2A2A2A);
                } else if (part == 1 || part == 2) {
                    gable(f, lead, 1, 1, L - 1, S - 1);
                    for (float u = 5; u < L - 2; u += 5) {
                        f.line(0x30000000, 0.5f, u, 1, u, S / 2 - 0.6f);
                        f.line(0x40000000, 0.5f, u, S / 2 + 0.6f, u, S - 1);
                    }
                    if (part == 2) {
                        // The crossing: an octagonal lantern with its spire.
                        float cu = L / 2, cv = S / 2, rad = S * 0.42f;
                        f.circle(0x48000000, cu + 2.5f, cv + 2.5f, rad);
                        float[] o = new float[16];
                        for (int k = 0; k < 8; k++) {
                            double a = Math.PI / 8 + k * Math.PI / 4;
                            o[2 * k] = cu + (float) Math.cos(a) * rad;
                            o[2 * k + 1] = cv + (float) Math.sin(a) * rad;
                        }
                        f.poly(0xFFA89E8C, o);
                        for (int k = 0; k < 8; k++) {
                            double a = Math.PI / 8 + k * Math.PI / 4, b = a + Math.PI / 4, am = (a + b) / 2;
                            float sh = 1f + 0.3f * (float) Math.cos(am + 3 * Math.PI / 4), ri = rad * 0.82f;
                            f.poly(Landmarks.shade(lead, sh), cu, cv, cu + (float) Math.cos(a) * ri, cv + (float) Math.sin(a) * ri,
                                    cu + (float) Math.cos(b) * ri, cv + (float) Math.sin(b) * ri);
                        }
                        f.circle(0xFFD8B860, cu, cv, 1.4f);
                    }
                } else {
                    // The apse: a rounded east end, its roof radiating from the centre.
                    float cu = L / 2, cv = S / 2;
                    int n = 12;
                    for (int k = 0; k < n; k++) {
                        double a = k * 2 * Math.PI / n, b = (k + 1) * 2 * Math.PI / n, am = (a + b) / 2;
                        float sh = 1f + 0.3f * (float) Math.cos(am + 3 * Math.PI / 4);
                        f.poly(Landmarks.shade(lead, sh), cu, cv, cu + (float) Math.cos(a) * (L / 2 - 1), cv + (float) Math.sin(a) * (S / 2 - 1),
                                cu + (float) Math.cos(b) * (L / 2 - 1), cv + (float) Math.sin(b) * (S / 2 - 1));
                    }
                    for (int k = 0; k < n; k++) {
                        double a = k * 2 * Math.PI / n;
                        f.line(0x40000000, 0.5f, cu, cv, cu + (float) Math.cos(a) * (L / 2 - 1), cv + (float) Math.sin(a) * (S / 2 - 1));
                    }
                    f.circle(0xFFD8B860, cu, cv, 1);
                }
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.DOWN | Variants.MID | Variants.OLD;
        d.minS = 10;
        d.minL = 14;
        d.base = City.OFFICE;
        d.inside = Variants.I_PEWS;
        d.roofCol = 0xFF5E6A78;
        out.add(d);
    }

    // ------------------------------------------------------------------ Old Town Gate

    static void cannon(F f, float u, float v, float dir) {
        f.rect(0x40000000, u - 2.5f, v - 3.5f * dir + 1, u + 4.5f, v + 3.5f * dir + 1);
        f.rect(0xFF2A2420, u - 3.6f, v - 2.5f, u - 2.2f, v + 0.5f);
        f.rect(0xFF2A2420, u + 2.2f, v - 2.5f, u + 3.6f, v + 0.5f);
        f.rect(0xFF6A4A2E, u - 2.2f, v - 3.5f, u + 2.2f, v + 3.5f);
        float b0 = v - 3 * dir, b1 = v + 8 * dir;
        f.poly(0xFF2E3034, u - 1.8f, b0, u + 1.8f, b0, u + 1.2f, b1, u - 1.2f, b1);
        f.line(0xFF6A6E74, 0.5f, u - 0.6f, b0, u - 0.4f, b1);
        f.circle(0xFF3A3C40, u, b1, 1.5f);
        f.circle(0xFF101010, u, b1, 0.7f);
        f.circle(0xFF3A3C40, u, b0 - 0.6f * dir, 1.1f);
    }

    static void townGate(List<Landmarks.Def> out) {
        // A fortified gatehouse between two round towers, stubs of the old town wall, cannons on the cobbles.
        Landmarks.Def d = new Landmarks.Def("Old Town Gate", T_MONUMENT) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int l = Math.max(bw, bh), s = Math.min(bw, bh), lu = Math.max(5, Math.min(8, l / 2)), lv = s >= 12 ? 4 : 3;
                return new int[][]{at(bw >= bh, (l - lu) / 2, (s - lv) / 2, lu, lv, 4)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, T = TT;
                int lt = Math.round(L / T), st = Math.round(S / T), lu = Math.max(5, Math.min(8, lt / 2)), lv = st >= 12 ? 4 : 3;
                float gu0 = ((lt - lu) / 2) * T, gu1 = gu0 + lu * T, gv0 = ((st - lv) / 2) * T, gv1 = gv0 + lv * T;
                float gc = (gu0 + gu1) / 2, gvc = (gv0 + gv1) / 2;
                // Cobbles: courses of setts with odd stones lighter and darker.
                f.rect(0xFFA89E8E, 0, 0, L, S);
                for (float v = 3; v < S; v += 3.5f) f.line(0x26000000, 0.5f, 0, v, L, v);
                for (int k = 0; k < 90; k++) {
                    float u = r.nextFloat() * (L - 3), v = r.nextFloat() * (S - 3);
                    f.rect(r.nextBoolean() ? 0x18FFFFFF : 0x1C000000, u, v, u + 3, v + 2.5f);
                }
                // The old road through the gate, worn with cart ruts.
                f.rect(0xFF8E8474, gc - 14, 0, gc + 14, S);
                for (float v = 2; v < S; v += 4) f.line(0x30000000, 0.5f, gc - 14, v, gc + 14, v);
                f.line(0x40000000, 1.5f, gc - 6, 0, gc - 6, S);
                f.line(0x40000000, 1.5f, gc + 6, 0, gc + 6, S);
                f.rect(0xFF7A7264, gc - 15, 0, gc - 14, S);
                f.rect(0xFF7A7264, gc + 14, 0, gc + 15, S);
                // Stubs of the town wall either side, crenellated, ending in broken masonry.
                for (int e = 0; e < 2; e++) {
                    float a = e == 0 ? Math.max(6, gu0 * 0.2f) : gu1, b = e == 0 ? gu0 : L - Math.max(6, (L - gu1) * 0.2f);
                    float w0 = gvc - 5, w1 = gvc + 5;
                    f.rect(0x48000000, a + 2, w0 + 3, b + 2, w1 + 3);
                    f.rect(0xFF8E826E, a, w0, b, w1);
                    f.rect(0xFFAEA28A, a, w0 + 2.5f, b, w1 - 1);
                    for (float u = a + 1; u < b - 2; u += 5) {
                        f.rect(0xFF7A6E5C, u, w0, u + 2.5f, w0 + 2.5f);
                        f.line(0x26000000, 0.4f, u + 2.5f, w0 + 2.5f, u + 2.5f, w1 - 1);
                    }
                    float end = e == 0 ? a : b, s = e == 0 ? -1 : 1;
                    f.poly(0xFF9A8E7A, end, w0, end + s * 3, w0 + 3, end + s * 1, w0 + 6, end + s * 4, w1, end, w1);
                    for (int k = 0; k < 7; k++)
                        f.circle(k % 2 == 0 ? 0xFF9A8E7A : 0xFF7E7262, end + s * (3 + r.nextFloat() * 10), w0 - 3 + r.nextFloat() * 16, 0.8f + r.nextFloat() * 1.4f);
                }
                // Cannons guarding the approach, a pile of shot.
                float fv = gv1 + Math.min(14, (S - gv1) * 0.4f);
                if (S - gv1 > 18) {
                    cannon(f, gc - 26, fv, 1);
                    cannon(f, gc + 26, fv, 1);
                    float pu = gc + 36, pv = fv + 2;
                    for (int k = 0; k < 3; k++) f.circle(0xFF2E3034, pu + k * 2.2f, pv, 1.1f);
                    f.circle(0xFF2E3034, pu + 1.1f, pv - 1.8f, 1.1f);
                    f.circle(0xFF3E4046, pu + 3.3f, pv - 1.8f, 1.1f);
                }
                if (gv0 > 22) cannon(f, gc - 26, gv0 - 14, -1);
                // Lanterns by the gate, lindens, benches, a plaque.
                f.lamp(gc - 17, gv1 + 3);
                f.lamp(gc + 17, gv1 + 3);
                f.lamp(gc - 17, gv0 - 3);
                f.lamp(gc + 17, gv0 - 3);
                f.tree(8, 8, 6);
                f.tree(L - 8, S - 8, 6);
                f.tree(L - 8, 8, 5.5f);
                f.tree(8, S - 8, 5.5f);
                f.bench(gc - 40, S - 6, true);
                f.bench(gc + 40, 6, true);
                f.rect(0x40000000, gc + 19, gv1 + 7, gc + 25, gv1 + 11);
                f.rect(0xFF6E6A62, gc + 18, gv1 + 6, gc + 24, gv1 + 10);
                f.rect(0xFFB08A3E, gc + 19, gv1 + 7, gc + 23, gv1 + 9);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF8C826E, 0, 0, L, S);
                // The gate block: a crenellated stone parapet round a red tiled roof.
                float tr = Math.min(S / 2 - 0.5f, L * 0.22f), bu0 = tr * 0.8f, bu1 = L - tr * 0.8f, bv0 = S * 0.08f, bv1 = S * 0.92f;
                f.rect(0xFFA49882, bu0, bv0, bu1, bv1);
                crenels(f, 0xFF8A7E6A, bu0, bv0, bu1, bv1, 2.2f);
                hip(f, 0xFFA0503A, bu0 + 4, bv0 + 4, bu1 - 4, bv1 - 4, true);
                f.line(0xFF505050, 0.6f, L / 2, S / 2, L / 2, S / 2 - 7);
                f.rect(0xFFC0302A, L / 2, S / 2 - 7, L / 2 + 5, S / 2 - 4);
                f.rect(0xFFF0E8D8, L / 2, S / 2 - 5.5f, L / 2 + 5, S / 2 - 5);
                // The flanking round towers with conical slate roofs.
                for (int e = 0; e < 2; e++) {
                    float u = e == 0 ? tr + 0.5f : L - tr - 0.5f, v = S / 2, rad = tr;
                    f.circle(0x48000000, u + 2.5f, v + 2.5f, rad);
                    f.circle(0xFF9A8E78, u, v, rad);
                    for (int k = 0; k < 16; k++) {
                        double a = k * Math.PI / 8;
                        f.circle(0xFF7E7262, u + (float) Math.cos(a) * rad * 0.9f, v + (float) Math.sin(a) * rad * 0.9f, rad * 0.07f);
                    }
                    dome(f, u, v, rad * 0.8f, 0xFF5A6470, 12);
                    for (int k = 0; k < 12; k++) {
                        double a = k * Math.PI / 6;
                        f.line(0x30000000, 0.4f, u, v, u + (float) Math.cos(a) * rad * 0.8f, v + (float) Math.sin(a) * rad * 0.8f);
                    }
                    f.circle(0xFFD8B860, u, v, 1.1f);
                }
                f.sign(city, "TOWN GATE", L / 2, S * 0.75f, 5, 0xFF2A2420);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.OLD | Variants.MID | Variants.DOWN;
        d.minS = 8;
        d.minL = 10;
        d.base = City.SHOP;
        d.inside = Variants.I_SHOWROOM;
        d.roofCol = 0xFFA0503A;
        out.add(d);
    }

    // ------------------------------------------------------------------ Covered Arcade

    static int[] arcadeSizes(int s) {
        return new int[]{Math.max(3, (s - 4 - (s - 10) / 2) / 2)};
    }

    static void arcade(List<Landmarks.Def> out) {
        // A Victorian shopping arcade: a glass barrel roof on iron ribs between two terraces of shops.
        Landmarks.Def d = new Landmarks.Def("Covered Arcade", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int l = Math.max(bw, bh), s = Math.min(bw, bh), tw = arcadeSizes(s)[0];
                boolean al = bw >= bh;
                return new int[][]{at(al, 1, 1, l - 2, tw, 3), at(al, 1, s - 1 - tw, l - 2, tw, 3)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, T = TT;
                int st = Math.round(S / T), tw = arcadeSizes(st)[0];
                float gv0 = (1 + tw) * T, gv1 = (st - 1 - tw) * T, vm = (gv0 + gv1) / 2, gw = gv1 - gv0;
                f.rect(0xFFCBC3B2, 0, 0, L, S);
                for (float u = 8; u < L; u += 8) {
                    f.line(0x18000000, 0.5f, u, 0, u, T);
                    f.line(0x18000000, 0.5f, u, S - T, u, S);
                }
                // The arcade floor: cream tiles with a red border and a mosaic down the middle.
                f.rect(0xFFE8E0CC, 0, gv0, L, gv1);
                f.rect(0xFF8A3A30, 0, gv0 + 2.5f, L, gv0 + 3.5f);
                f.rect(0xFF8A3A30, 0, gv1 - 3.5f, L, gv1 - 2.5f);
                for (float u = T + 6; u < L - T; u += 14) {
                    f.poly(0xFF3A5A6A, u, vm - 3, u + 3, vm, u, vm + 3, u - 3, vm);
                    f.circle(0xFFB89A50, u + 7, vm, 1.2f);
                }
                // Shop fronts: a dark glazed strip with coloured fascias, one per shop.
                int n = Math.max(3, Math.round((L - 2 * T) / 28));
                float uw = (L - 2 * T) / n;
                int[] fas = {0xFF2E4A3A, 0xFF7A2A2A, 0xFF2A3A5A, 0xFF5A4A2A, 0xFF4A2A4A};
                for (int i = 0; i < n; i++) {
                    float u0 = T + i * uw;
                    f.rect(fas[r.nextInt(fas.length)], u0 + 1, gv0, u0 + uw - 1, gv0 + 1.6f);
                    f.rect(fas[r.nextInt(fas.length)], u0 + 1, gv1 - 1.6f, u0 + uw - 1, gv1);
                }
                // The glass barrel roof: tinted glass shaded to its curve, iron ribs and purlins, a ridge.
                float u0 = T - 4, u1 = L - T + 4;
                f.rect(0x30000000, u0 + 2, gv0 + 2, u1 + 2, gv1 + 2);
                f.rect(0x60A8C8DA, u0, gv0, u1, gv1);
                f.rect(0x30203040, u0, gv0, u1, gv0 + gw * 0.16f);
                f.rect(0x40203040, u0, gv1 - gw * 0.2f, u1, gv1);
                f.rect(0x44FFFFFF, u0, gv0 + gw * 0.26f, u1, gv0 + gw * 0.38f);
                for (float u = u0; u <= u1 + 0.1f; u += (u1 - u0) / Math.round((u1 - u0) / 7)) f.line(0xFF2C3A34, 0.9f, u, gv0, u, gv1);
                for (int k = 1; k < 4; k++) f.line(0xFF2C3A34, 0.5f, u0, gv0 + gw * k / 4, u1, gv0 + gw * k / 4);
                f.line(0xFF2C3A34, 1.4f, u0, vm, u1, vm);
                f.line(0xFFD8E4E8, 0.5f, u0, vm - 1, u1, vm - 1);
                f.frame(0xFF2C3A34, 0.9f, u0, gv0, u1, gv1);
                // A glass dome over the middle crossing.
                if (L > 180) {
                    float rad = gw * 0.5f + 3;
                    f.circle(0x40000000, L / 2 + 2, vm + 2, rad);
                    dome(f, L / 2, vm, rad, 0xFF9EC0D0, 16);
                    for (int k = 0; k < 16; k++) {
                        double a = k * Math.PI / 8;
                        f.line(0xFF2C3A34, 0.6f, L / 2, vm, L / 2 + (float) Math.cos(a) * rad, vm + (float) Math.sin(a) * rad);
                    }
                    f.ring(0xFF2C3A34, 0.8f, L / 2, vm, rad);
                    f.ring(0xFF2C3A34, 0.5f, L / 2, vm, rad * 0.55f);
                    f.circle(0xFF2C3A34, L / 2, vm, 1.5f);
                }
                // Ornamental entrance arches at both ends.
                for (int e = 0; e < 2; e++) {
                    float a = e == 0 ? u0 : u1, s = e == 0 ? -1 : 1;
                    f.rect(0x40000000, Math.min(a, a + s * 4) + 1.5f, gv0 - 2 + 1.5f, Math.max(a, a + s * 4) + 1.5f, gv1 + 2 + 1.5f);
                    f.rect(0xFFD8CCB0, Math.min(a, a + s * 4), gv0 - 2, Math.max(a, a + s * 4), gv1 + 2);
                    f.rect(0xFFB8AA8C, Math.min(a, a + s * 4), gv0 - 2, Math.max(a, a + s * 4), gv0 + 1);
                    f.poly(0xFFE8DCC0, a + s * 4, vm - gw * 0.3f, a + s * 7, vm, a + s * 4, vm + gw * 0.3f);
                    f.circle(0xFFB89A50, a + s * 2, vm, 1.4f);
                }
                // The street round it: lamps and trees in pits, benches and a flower stall at the ends.
                for (float u = 12; u < L - 8; u += 30) {
                    f.tree(u, T / 2, 4.2f);
                    f.tree(u + 15, S - T / 2, 4.2f);
                    f.lamp(u + 15, T / 2);
                    f.lamp(u, S - T / 2);
                }
                f.bench(T / 2, vm - 10, false);
                f.bench(L - T / 2, vm + 10, false);
                f.rect(0x40000000, L - T / 2 - 3, vm - 8, L - T / 2 + 4, vm + 1);
                f.rect(0xFF2E6A4A, L - T / 2 - 4, vm - 9, L - T / 2 + 3, vm);
                for (int k = 0; k < 4; k++)
                    f.circle(new int[]{0xFFE85A6A, 0xFFF0D040, 0xFFF0F0F0, 0xFFC860C0}[k], L - T / 2 - 2.5f + (k % 2) * 4, vm - 7 + (k / 2) * 4, 1.3f);
                f.sign(city, "ARCADE", T / 2 + 6, vm, 4, 0xFF3A2A1A);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int n = Math.max(3, Math.round(L / 28));
                float uw = L / n;
                int[] cols = {0xFF5A626C, 0xFF9A5A44, 0xFF4E5662, 0xFF8A6A52};
                for (int i = 0; i < n; i++) {
                    float u0 = i * uw;
                    gable(f, cols[r.nextInt(cols.length)], u0, 0, u0 + uw, S);
                    float um = u0 + uw / 2;
                    for (int s = 0; s < 2; s++) {
                        float v = s == 0 ? S * 0.2f : S * 0.72f;
                        f.rect(0x40000000, um - 2.2f, v + 0.8f, um + 3.2f, v + 4.6f);
                        f.rect(0xFFD8D2C4, um - 3, v, um + 3, v + 4);
                        f.rect(s == 0 ? 0xFF8CA4B0 : 0xFF6E8490, um - 2, v + 0.8f, um + 2, v + 3.4f);
                    }
                }
                for (int i = 0; i <= n; i++) {
                    float u = Math.min(L - 1, Math.max(1, i * uw));
                    f.rect(0xFFA08C76, u - 1, 0, u + 1, S);
                    if (i > 0 && i < n) {
                        f.rect(0x40000000, u - 2, S * 0.5f - 1, u + 4, S * 0.5f + 3);
                        f.rect(0xFF8A5A44, u - 3, S * 0.5f - 2, u + 3, S * 0.5f + 2);
                        for (int k = 0; k < 3; k++) f.circle(0xFFB0704A, u - 2 + k * 2, S * 0.5f, 0.8f);
                    }
                }
                f.frame(0xFFCFC2A6, 1, 0.5f, 0.5f, L - 0.5f, S - 0.5f);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.MID | Variants.OLD;
        d.minS = 10;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_SHELVES;
        d.roofCol = 0xFF5A626C;
        out.add(d);
    }

    // ------------------------------------------------------------------ Public Library

    static void book(F f, float u, float v, int col, boolean alongU) {
        float a = alongU ? 6 : 3, b = alongU ? 3 : 6;
        f.rect(0x40000000, u - a + 1.2f, v - b + 1.2f, u + a + 1.2f, v + b + 1.2f);
        f.rect(col, u - a, v - b, u + a, v + b);
        if (alongU) {
            f.rect(0xFFF0E8D0, u - a + 1, v + b - 1.8f, u + a - 1, v + b);
            f.line(0x40000000, 0.3f, u - a + 1, v + b - 0.9f, u + a - 1, v + b - 0.9f);
            f.rect(City.darken(col, 0.7f), u - a, v - b, u - a + 1.5f, v + b);
            f.rect(0xFFD8B860, u - 2, v - 1.4f, u + 2, v - 0.6f);
        } else {
            f.rect(0xFFF0E8D0, u + a - 1.8f, v - b + 1, u + a, v + b - 1);
            f.line(0x40000000, 0.3f, u + a - 0.9f, v - b + 1, u + a - 0.9f, v + b - 1);
            f.rect(City.darken(col, 0.7f), u - a, v - b, u + a, v - b + 1.5f);
            f.rect(0xFFD8B860, u - 1.4f, v - 2, u - 0.6f, v + 2);
        }
    }

    static void library(List<Landmarks.Def> out) {
        // A classical library with a glazed reading-room rotunda, a portico onto a walled reading garden with an
        // open-book sculpture and book-shaped benches.
        Landmarks.Def d = new Landmarks.Def("Public Library", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int l = Math.max(bw, bh), s = Math.min(bw, bh);
                return new int[][]{at(bw >= bh, 1, 1, Math.max(5, l / 2), s - 2, 3)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, T = TT;
                int lt = Math.round(L / T);
                float bu1 = (1 + Math.max(5, lt / 2)) * T, vm = S / 2;
                f.rect(0xFF5E9A48, 0, 0, L, S);
                f.rect(0xFFD6CCB4, T - 5, T - 5, bu1 + 5, S - T + 5);
                // The portico: a floor, a row of columns, steps down to the garden walk.
                float sv0 = S * 0.28f, sv1 = S * 0.72f;
                f.rect(0xFFE2D8C0, bu1, sv0, bu1 + 7, sv1);
                for (float v = sv0 + 3; v <= sv1 - 2.9f; v += (sv1 - sv0 - 6) / Math.max(3, Math.round((sv1 - sv0 - 6) / 6))) {
                    f.circle(0x40000000, bu1 + 4, v + 1, 1.9f);
                    f.circle(0xFFF0E8D4, bu1 + 3, v, 1.8f);
                    f.circle(0xFFC8BCA0, bu1 + 3.5f, v + 0.5f, 0.9f);
                }
                for (int k = 0; k < 4; k++) {
                    float u = bu1 + 7 + k * 2;
                    f.rect(k % 2 == 0 ? 0xFFD8CCB0 : 0xFFCCC0A4, u, sv0 + 4 - k * 1.5f, u + 2, sv1 - 4 + k * 1.5f);
                    f.line(0x40000000, 0.5f, u + 2, sv0 + 4 - k * 1.5f, u + 2, sv1 - 4 + k * 1.5f);
                }
                // The walled reading garden.
                float gu0 = bu1 + 20, gu1 = L - 4, gv0 = 4, gv1 = S - 4;
                f.rect(0xFFD9CDB0, bu1 + 15, vm - 6, gu0 + 2, vm + 6);
                if (gu1 - gu0 > 30) {
                    float gm = (gu0 + gu1) / 2;
                    f.rect(0x30000000, gu0 + 1, gv0 + 1, gu1 + 1, gv1 + 1);
                    f.rect(0xFF2E5E2E, gu0, gv0, gu1, gv1);
                    f.rect(0xFF3E7234, gu0, gv0, gu1, gv0 + 1.5f);
                    f.rect(0xFF66A450, gu0 + 3, gv0 + 3, gu1 - 3, gv1 - 3);
                    // Beds in the four quarters.
                    int[] fl = {0xFFE85A6A, 0xFFF0D040, 0xFFF0F0F0, 0xFF9A6AD0, 0xFFF09040};
                    for (int q = 0; q < 4; q++) {
                        float a0 = q % 2 == 0 ? gu0 + 7 : gm + 9, a1 = q % 2 == 0 ? gm - 9 : gu1 - 7;
                        float b0 = q < 2 ? gv0 + 7 : vm + 9, b1 = q < 2 ? vm - 9 : gv1 - 7;
                        if (a1 - a0 < 6 || b1 - b0 < 5) continue;
                        f.roundRect(0xFF7A5A3A, a0, b0, a1, b1, 2);
                        f.roundRect(0xFF4A7E3A, a0 + 1, b0 + 1, a1 - 1, b1 - 1, 2);
                        for (int k = 0; k < 18; k++)
                            f.circle(fl[r.nextInt(fl.length)], a0 + 1.5f + r.nextFloat() * (a1 - a0 - 3), b0 + 1.5f + r.nextFloat() * (b1 - b0 - 3), 1);
                    }
                    f.rect(0xFFD9CDB0, gu0, vm - 4, gu1 - 3, vm + 4);
                    f.rect(0xFFD9CDB0, gm - 4, gv0 + 3, gm + 4, gv1 - 3);
                    // The open book on a round plinth at the crossing.
                    f.circle(0xFFB8B0A0, gm, vm, 8);
                    f.circle(0xFFD8D0C0, gm, vm, 7);
                    f.poly(0x40000000, gm - 5, vm - 3, gm + 6, vm - 3, gm + 6, vm + 5, gm - 5, vm + 5);
                    f.poly(0xFFF6F2E4, gm - 6, vm - 4, gm, vm - 3.2f, gm, vm + 4, gm - 6, vm + 3.2f);
                    f.poly(0xFFE0DACB, gm, vm - 3.2f, gm + 6, vm - 4, gm + 6, vm + 3.2f, gm, vm + 4);
                    for (int k = 0; k < 3; k++) {
                        float v = vm - 1.6f + k * 1.6f;
                        f.line(0x50000000, 0.3f, gm - 5, v, gm - 1, v + 0.3f);
                        f.line(0x50000000, 0.3f, gm + 1, v + 0.3f, gm + 5, v);
                    }
                    f.line(0xFF8A7A5A, 0.6f, gm, vm - 3.2f, gm, vm + 4);
                    // Book benches round the crossing, trees in the corners, lamps.
                    int[] bc = {0xFFA8382E, 0xFF2E4A7A, 0xFF3A6A3E, 0xFFB8862E};
                    float du = Math.min(16, (gu1 - gu0) / 4), dv = Math.min(14, (gv1 - gv0) / 4);
                    book(f, gm - du, vm - dv, bc[0], true);
                    book(f, gm + du, vm - dv, bc[1], true);
                    book(f, gm - du, vm + dv, bc[2], true);
                    book(f, gm + du, vm + dv, bc[3], true);
                    f.tree(gu0 + 8, gv0 + 8, 5.5f);
                    f.tree(gu1 - 8, gv0 + 8, 5.5f);
                    f.tree(gu0 + 8, gv1 - 8, 5.5f);
                    f.tree(gu1 - 8, gv1 - 8, 5.5f, 0xFF4E8A3A);
                    f.lamp(gm - 6, vm - 6);
                    f.lamp(gm + 6, vm + 6);
                }
                f.tree(bu1 + 10, 8, 5);
                f.tree(bu1 + 10, S - 8, 5);
                f.bench(bu1 + 10, sv0 - 4, true);
                f.bench(bu1 + 10, sv1 + 4, true);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, cu = L / 2, cv = S / 2;
                f.rect(0xFFB8AC90, 0, 0, L, S);
                f.rect(0xFFE0D6BE, 1, 1, L - 1, S - 1);
                for (float u = 3; u < L - 2; u += 3) {
                    f.circle(0xFFC8BCA0, u, 1.6f, 0.6f);
                    f.circle(0xFFC8BCA0, u, S - 1.6f, 0.6f);
                }
                hip(f, 0xFFB0603E, 3, 3, L - 3, S - 3, true);
                // The reading-room rotunda: a stone drum and a ribbed glass dome with a copper lantern.
                float rad = Math.min(L, S) * 0.3f;
                f.circle(0x48000000, cu + 3, cv + 3, rad + 2.5f);
                f.circle(0xFFD8CCB2, cu, cv, rad + 2.5f);
                f.ring(0xFFA89C80, 0.8f, cu, cv, rad + 2.5f);
                dome(f, cu, cv, rad, 0xFF9CB8C4, 16);
                for (int k = 0; k < 16; k++) {
                    double a = k * Math.PI / 8;
                    f.line(0xFF4A5A62, 0.6f, cu, cv, cu + (float) Math.cos(a) * rad, cv + (float) Math.sin(a) * rad);
                }
                f.ring(0xFF4A5A62, 0.5f, cu, cv, rad * 0.55f);
                f.circle(0xFF3E7A68, cu, cv, rad * 0.2f);
                f.circle(0xFF6AB098, cu - rad * 0.05f, cv - rad * 0.05f, rad * 0.11f);
                f.sign(city, "LIBRARY", cu, cv + rad + (S / 2 - rad) * 0.5f, 5, 0xFF3A2A1A);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.DOWN | Variants.MID | Variants.OLD | Variants.SUB | Variants.UNI;
        d.minS = 10;
        d.minL = 12;
        d.base = City.OFFICE;
        d.inside = Variants.I_LIBRARY;
        d.roofCol = 0xFFB0603E;
        out.add(d);
    }
}
