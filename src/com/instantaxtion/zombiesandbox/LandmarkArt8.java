package com.instantaxtion.zombiesandbox;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.Landmarks.*;

/** ((22.6) Landmark designs, part 8 (see Landmarks): transport, industry, technology and homes. */
final class LandmarkArt8 {
    private LandmarkArt8() {
    }

    static void register(List<Landmarks.Def> out) {
        busTerminal(out);
        tramDepot(out);
        cableCar(out);
        monorail(out);
        heliport(out);
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
            LandmarkArt8.ring(c, p, col, w, x(u, v), y(u, v), r);
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

    /** A bus from above, centred on (u,v) heading at frame angle a (front to +). */
    static void bus(F f, float u, float v, float a, float len, int col) {
        float l = len / 2, w = 4.6f, deg = f.ang(a);
        f.turn(u, v, a);
        Canvas c = f.c;
        Paint p = f.p;
        shadowBox(c, p, deg, l, w, 2, 1.6f, 2.2f, 0x50000000);
        roundRect(c, p, City.darken(col, 0.65f), -l, -w, l, w, 2);
        roundRect(c, p, col, -l + 0.5f, -w + 0.5f, l - 0.5f, w - 0.5f, 1.8f);
        rect(c, p, 0xFF26303A, l - 3.2f, -w + 1, l - 0.9f, w - 1);
        rect(c, p, 0xFF2E3A46, -l + 0.8f, -w + 1.4f, -l + 1.8f, w - 1.4f);
        rect(c, p, 0xFF34404C, -l + 2.5f, -w + 0.3f, l - 4, -w + 1.1f);
        rect(c, p, 0xFF34404C, -l + 2.5f, w - 1.1f, l - 4, w - 0.3f);
        rect(c, p, 0xFFE4E6E8, -l * 0.35f, -2, l * 0.15f, 2);
        line(c, p, 0x40000000, 0.4f, -l * 0.1f, -2, -l * 0.1f, 2);
        rect(c, p, City.lighten(col, 0.25f), -l + 3, -w + 1.3f, -l * 0.45f, w - 1.3f);
        c.restore();
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

    /** A lorry: a cab and a box or tank behind it. */
    static void lorry(F f, float u, float v, float a, float len, int cab, int body, boolean tank) {
        float l = len / 2, w = 4.2f, deg = f.ang(a);
        f.turn(u, v, a);
        Canvas c = f.c;
        Paint p = f.p;
        shadowBox(c, p, deg, l, w, 1.5f, 1.5f, 2.2f, 0x50000000);
        roundRect(c, p, City.darken(cab, 0.7f), l - 7, -w, l, w, 1.5f);
        roundRect(c, p, cab, l - 6.6f, -w + 0.4f, l - 0.4f, w - 0.4f, 1.3f);
        rect(c, p, 0xFF26303A, l - 2.2f, -w + 0.9f, l - 0.8f, w - 0.9f);
        if (tank) {
            roundRect(c, p, City.darken(body, 0.72f), -l, -w + 0.3f, l - 8, w - 0.3f, w - 0.3f);
            rect(c, p, body, -l + 1, -w + 1.2f, l - 9, w - 1.2f);
            rect(c, p, City.lighten(body, 0.3f), -l + 1, -w + 1.6f, l - 9, -w + 2.6f);
            for (float t = -l + 4; t < l - 9; t += 6) circle(c, p, City.darken(body, 0.6f), t, 0, 1.1f);
        } else {
            rect(c, p, City.darken(body, 0.75f), -l, -w, l - 7.6f, w);
            rect(c, p, body, -l + 0.5f, -w + 0.5f, l - 8.1f, w - 0.5f);
            for (float t = -l + 3; t < l - 8; t += 3) line(c, p, 0x18000000, 0.4f, t, -w + 0.5f, t, w - 0.5f);
        }
        c.restore();
    }

    /** Railway track along u from u0 to u1 at v: ballast, sleepers and two rails. */
    static void track(F f, float u0, float u1, float v, float gauge) {
        float g = gauge / 2;
        f.rect(0xFF7A746C, u0, v - g - 2.5f, u1, v + g + 2.5f);
        for (float u = u0 + 1; u < u1; u += 3) f.rect(0xFF5A4636, u, v - g - 1.5f, u + 1.4f, v + g + 1.5f);
        f.line(0xFFB8BCC0, 0.8f, u0, v - g, u1, v - g);
        f.line(0xFFB8BCC0, 0.8f, u0, v + g, u1, v + g);
    }

    /** A curving track from (u0,v0) via (cu,cv) to (u1,v1), its rails offset across v. */
    static void curveTrack(F f, float u0, float v0, float cu, float cv, float u1, float v1, float gauge, boolean bed) {
        float g = gauge / 2;
        if (bed) f.curve(0xFF7A746C, gauge + 5, u0, v0, cu, cv, u1, v1);
        else {
            f.curve(0xFF5A4636, gauge + 3, u0, v0, cu, cv, u1, v1);
            f.curve(0xFF7A746C, gauge - 0.6f, u0, v0, cu, cv, u1, v1);
        }
    }

    static void rails(F f, float u0, float v0, float cu, float cv, float u1, float v1, float gauge) {
        float g = gauge / 2;
        f.curve(0xFFB8BCC0, 0.8f, u0, v0 - g, cu, cv - g, u1, v1 - g);
        f.curve(0xFFB8BCC0, 0.8f, u0, v0 + g, cu, cv + g, u1, v1 + g);
    }

    /** A metal sheet roof with ribs along u. */
    static void ribbed(F f, int col, float u0, float v0, float u1, float v1, float step) {
        f.rect(col, u0, v0, u1, v1);
        for (float v = v0 + step; v < v1; v += step) f.line(City.darken(col, 0.85f), 0.5f, u0, v, u1, v);
    }

    /** A roof plant box with a fan. */
    static void fanUnit(F f, float u, float v, float s) {
        f.rect(0x40000000, u - s + 1, v - s + 1.2f, u + s + 1, v + s + 1.2f);
        f.rect(0xFF9A9EA2, u - s, v - s, u + s, v + s);
        f.circle(0xFF4A4E52, u, v, s * 0.8f);
        f.line(0xFF7A7E82, 0.6f, u - s * 0.7f, v, u + s * 0.7f, v);
        f.line(0xFF7A7E82, 0.6f, u, v - s * 0.7f, u, v + s * 0.7f);
        f.circle(0xFFB0B4B8, u, v, s * 0.2f);
    }

    /** A barrel vault along u across v0..v1, shaded from the lit top. */
    static void vault(F f, int col, float u0, float v0, float u1, float v1) {
        float h = v1 - v0;
        int[] sh = {City.lighten(col, 0.2f), City.lighten(col, 0.1f), col, City.darken(col, 0.9f), City.darken(col, 0.78f), City.darken(col, 0.66f)};
        for (int k = 0; k < 6; k++) f.rect(sh[k], u0, v0 + h * k / 6, u1, v0 + h * (k + 1) / 6);
        f.line(City.lighten(col, 0.3f), 0.6f, u0, v0 + h * 0.36f, u1, v0 + h * 0.36f);
        f.frame(City.darken(col, 0.55f), 0.6f, u0, v0, u1, v1);
    }

    // ------------------------------------------------------------------ 1. Bus Terminal

    static void busTerminal(List<Landmarks.Def> out) {
        // A glazed terminal hall at one end, an island platform under a saw-tooth canopy with buses angled into it.
        D d = new D("Bus Terminal", T_BUILDING) {
            int tw(int L) {
                return Math.max(3, Math.min(5, L / 5));
            }

            @Override
            int[][] lp(int L, int S) {
                return new int[][]{B(1, 1, tw(L), S - 2, 2)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float te = (1 + q[0][2]) * T, vc = S / 2;
                f.rect(0xFF5C5E60, 0, 0, L, S);
                f.specks(r, 50, 0x16000000, 0x10FFFFFF, 0, 0, L, S, 1.6f);
                // Pavement round the terminal and along the edges.
                f.rect(0xFFC8C2B6, 0, 0, te + 6, S);
                f.rect(0xFFC8C2B6, te, 0, L, 5);
                f.rect(0xFFC8C2B6, te, S - 5, L, S);
                f.line(0xFF9A948A, 0.8f, te, 5, L, 5);
                f.line(0xFF9A948A, 0.8f, te, S - 5, L, S - 5);
                f.line(0xFF9A948A, 0.8f, te + 6, 5, te + 6, S - 5);
                for (float v = 10; v < S - 6; v += 18) {
                    f.tree(4, v, 3.6f, 0xFF4E8A44);
                }
                // The island platform, joined to the terminal by a walkway.
                float ua = te + 6, ub = L - 30, iw = 7;
                f.rect(0xFFD2CCC0, ua, vc - iw, ub, vc + iw);
                f.roundRect(0xFFD2CCC0, ub - 8, vc - iw, ub + 6, vc + iw, 6);
                f.frame(0xFFE8E0A0, 0.8f, ua, vc - iw, ub, vc + iw);
                // Buses angled nose-in along both sides; a saw-tooth kerb before each bay.
                float ang = 38, sp = 16, len = 32;
                float ca = (float) Math.cos(Math.toRadians(ang)), sa = (float) Math.sin(Math.toRadians(ang));
                int[] cols = {0xFFC8382E, 0xFF2E6AA8, 0xFFE2B428, 0xFF3A8A4E, 0xFFE8E4DC};
                int k = 0;
                for (float u = ua + 6; u + 6 < ub; u += sp, k++) {
                    for (int s = -1; s <= 1; s += 2) {
                        float ev = vc + s * iw;
                        // Bay lines.
                        f.line(0xFFE8E6DC, 0.7f, u - 2, ev, u - 2 - len * ca * 0.9f, ev + s * len * sa * 0.9f);
                        if (r.nextInt(5) < 4) {
                            float nu = u + 4, nv = ev + s * 1.5f;
                            bus(f, nu - ca * len / 2, nv + s * sa * len / 2, s < 0 ? ang : -ang, len, cols[(k * 2 + (s + 1) / 2) % cols.length]);
                        }
                    }
                }
                // Circulating lanes: dashes and arrows.
                for (float u = ua; u < L - 10; u += 12) {
                    f.rect(0xFFE8E6DC, u, 10, u + 6, 11);
                    f.rect(0xFFE8E6DC, u, S - 11, u + 6, S - 10);
                }
                f.poly(0xFFE8E6DC, ua + 30, 14, ua + 24, 12, ua + 24, 16);
                f.poly(0xFFE8E6DC, ua + 24, S - 14, ua + 30, S - 12, ua + 30, S - 16);
                // The saw-tooth canopy over the island: folded panels, glass in each fold, columns down the middle.
                float cv0 = vc - iw - 6, cv1 = vc + iw + 6;
                f.rect(0x38000000, ua + 2, cv0 + 3, ub + 3, cv1 + 3);
                for (float u = ua; u < ub; u += sp) {
                    float e = Math.min(ub, u + sp);
                    f.rect(0xFFE6E8EA, u, cv0, u + (e - u) * 0.55f, cv1);
                    f.rect(0xFFA8AEB4, u + (e - u) * 0.55f, cv0, e, cv1);
                    f.rect(0xFF8EB4C8, u + (e - u) * 0.55f, cv0 + 2, u + (e - u) * 0.7f, cv1 - 2);
                    f.line(0xFF7A8088, 0.6f, e, cv0, e, cv1);
                }
                f.frame(0xFF6A7078, 0.8f, ua, cv0, ub, cv1);
                for (float u = ua + sp / 2; u < ub; u += sp * 2) f.circle(0xFF5A6068, u, vc, 1);
                // A drivers' kiosk and bollards at the far end.
                f.rect(0x40000000, ub + 1, vc - 4 + 1.5f, ub + 9, vc + 4 + 1.5f);
                f.rect(0xFF7A8E9A, ub, vc - 4, ub + 8, vc + 4);
                f.rect(0xFF9AB0BE, ub + 1, vc - 3, ub + 7, vc + 3);
                for (float v = 8; v < S - 6; v += 6) f.circle(0xFFE0C040, L - 3, v, 0.9f);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                // The terminal: a long wave roof with a glazed spine and a clock over the entrance.
                float L = f.L, S = f.S;
                vault(f, 0xFFB4BAC0, 0, 0, L, S / 2);
                vault(f, 0xFFA0A8B0, 0, S / 2, L, S);
                f.rect(0xFF7AA4BE, 2, S / 2 - 2.5f, L - 2, S / 2 + 2.5f);
                for (float u = 4; u < L - 2; u += 4) f.line(0xFF5A7E96, 0.5f, u, S / 2 - 2.5f, u, S / 2 + 2.5f);
                f.line(0xC0FFFFFF, 0.5f, 2, S / 2 - 2, L - 2, S / 2 - 2);
                for (float u = 10; u < L - 6; u += 22) fanUnit(f, u, S * 0.2f, 3);
                f.circle(0xFF3A3E44, L * 0.5f, S * 0.8f, 4);
                f.circle(0xFFF4F0E4, L * 0.5f, S * 0.8f, 3.2f);
                f.line(0xFF202020, 0.6f, L * 0.5f, S * 0.8f, L * 0.5f, S * 0.8f - 2.4f);
                f.line(0xFF202020, 0.6f, L * 0.5f, S * 0.8f, L * 0.5f + 1.8f, S * 0.8f);
                f.sign(city, "BUS STATION", L / 2, S / 2, 7, 0xFF1E4A7A);
            }
        };
        d.ground = City.LOT;
        d.districts = Variants.DOWN | Variants.MID | Variants.IND;
        d.minS = 8;
        d.minL = 14;
        d.base = City.OFFICE;
        d.inside = Variants.I_CAFE;
        d.roofCol = 0xFFB4BAC0;
        out.add(d);
    }

    /** A tram from above: articulated sections with a pantograph. */
    static void tram(F f, float u, float v, float a, float len, int col) {
        float l = len / 2, w = 4.4f, deg = f.ang(a);
        f.turn(u, v, a);
        Canvas c = f.c;
        Paint p = f.p;
        shadowBox(c, p, deg, l, w, 3, 1.6f, 2.2f, 0x50000000);
        int n = 3;
        float sl = len / n;
        for (int k = 0; k < n; k++) {
            float a0 = -l + k * sl + (k > 0 ? 0.8f : 0), a1 = -l + (k + 1) * sl - (k < n - 1 ? 0.8f : 0);
            float rad = (k == 0 || k == n - 1) ? 3 : 0.8f;
            roundRect(c, p, City.darken(col, 0.65f), a0, -w, a1, w, rad);
            roundRect(c, p, col, a0 + 0.4f, -w + 0.4f, a1 - 0.4f, w - 0.4f, rad);
            rect(c, p, 0xFFEAE6DA, a0 + 1, -w + 1.4f, a1 - 1, w - 1.4f);
            if (k < n - 1) rect(c, p, 0xFF3A3A3C, a1, -w + 1, a1 + 1.6f, w - 1);
        }
        rect(c, p, 0xFF26303A, l - 2.4f, -w + 1, l - 0.8f, w - 1);
        rect(c, p, 0xFF26303A, -l + 0.8f, -w + 1, -l + 2.4f, w - 1);
        line(c, p, 0xFF3A3A3C, 0.6f, -2, -2.6f, 2, 2.6f);
        line(c, p, 0xFF3A3A3C, 0.6f, -2, 2.6f, 2, -2.6f);
        c.restore();
    }

    // ------------------------------------------------------------------ 2. Tram Depot

    static void tramDepot(List<Landmarks.Def> out) {
        // A single track off the street fans out over a ladder of points into two vaulted tram sheds.
        D d = new D("Tram Depot", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                int lv = (S - 3) / 2, lu = Math.max(lv + 1, Math.round(L * 0.42f));
                lu = Math.min(lu, L - 5);
                return new int[][]{B(L - 1 - lu, 1, lu, lv, 2), B(L - 1 - lu, S - 1 - lv, lu, lv, 2), B(1, 1, 3, 2, 1)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float su = q[0][0] * T;
                f.rect(0xFF8E8A82, 0, 0, L, S);
                f.specks(r, 70, 0x1A000000, 0x14FFFFFF, 0, 0, L, S, 1.5f);
                // Grass verges round the fan.
                f.rect(0xFF6E9450, 0, S - 6, su - 8, S);
                f.rect(0xFF6E9450, 4 * T + 6, 0, su - 8, 5);
                // The roads into the sheds: two per shed.
                float[] rv = new float[4];
                for (int s = 0; s < 2; s++) {
                    float v0 = q[s][1] * T, v1 = v0 + q[s][3] * T;
                    rv[s * 2] = v0 + (v1 - v0) * 0.28f;
                    rv[s * 2 + 1] = v0 + (v1 - v0) * 0.72f;
                }
                float lead = S / 2, fu = Math.max(su * 0.55f, su - 64);
                float g = 6;
                // Beds first, then sleepers on the straights, then all the rails.
                for (int k = 0; k < 4; k++) curveTrack(f, 0, lead, fu * 0.6f, lead, fu, rv[k], g, true);
                for (int k = 0; k < 4; k++) track(f, fu, L, rv[k], g);
                f.rect(0xFF7A746C, 0, lead - g / 2 - 2.5f, fu * 0.3f, lead + g / 2 + 2.5f);
                for (float u = 1; u < fu * 0.3f; u += 3) f.rect(0xFF5A4636, u, lead - g / 2 - 1.5f, u + 1.4f, lead + g / 2 + 1.5f);
                for (int k = 0; k < 4; k++) rails(f, 0, lead, fu * 0.6f, lead, fu, rv[k], g);
                // Point levers by the fan.
                for (int k = 0; k < 3; k++) {
                    float u = fu * (0.32f + k * 0.14f);
                    f.rect(0xFF3A3A3C, u, lead + 9 + k * 2, u + 3, lead + 11 + k * 2);
                    f.circle(0xFFD8B030, u + 1.5f, lead + 10 + k * 2, 0.8f);
                }
                // Trams standing at the shed doors (running in under the roof) and one coming in.
                int[] cols = {0xFFB8302A, 0xFF2A6A4A, 0xFFB8302A, 0xFFD89A28};
                for (int k = 0; k < 4; k++) {
                    if (k == 1) continue;
                    float tl = 46;
                    tram(f, su - tl / 2 + 16 + (k % 2) * 6, rv[k], 0, tl, cols[k]);
                }
                if (fu * 0.6f > 50) tram(f, 26, lead, 0, 44, 0xFFD89A28);
                // Overhead wires along every road, on poles.
                for (int k = 0; k < 4; k++) {
                    f.line(0xC0303032, 0.5f, fu, rv[k], su, rv[k]);
                    f.curve(0xC0303032, 0.5f, 0, lead, fu * 0.6f, lead, fu, rv[k]);
                }
                for (float u = 10; u < su - 4; u += 28) {
                    f.line(0x50000000, 1.2f, u + 2, 3, u + 2, S - 1);
                    f.circle(0xFF4A4A4C, u, 3, 1.3f);
                    f.circle(0xFF4A4A4C, u, S - 3, 1.3f);
                    f.line(0x90303032, 0.5f, u, 3, u, S - 3);
                }
                // A sand hopper and buffers inside the yard.
                f.circle(0x40000000, su - 6, S / 2 + 2, 4);
                f.circle(0xFF8A6A4A, su - 7, S / 2, 4);
                f.circle(0xFFC8A870, su - 7, S / 2, 2.4f);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                if (part == 2) {
                    // The substation: brick with a gravel roof and transformer fins.
                    f.rect(0xFF8A4A38, 0, 0, L, S);
                    f.rect(0xFF9C9488, 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                    f.specks(r, 20, 0x30000000, 0x30FFFFFF, 2, 2, L - 2, S - 2, 0.8f);
                    for (float u = 6; u < L - 4; u += 2.2f) f.rect(0xFF4A5056, u, S * 0.3f, u + 1, S * 0.7f);
                    f.sign(city, "DEPOT", L / 2, S / 2, 5, 0xFF2A2A2A);
                    return;
                }
                // A shed of two barrel vaults, each with a glazed crown, the gable ends showing.
                f.rect(0xFF4A4642, 0, 0, L, S);
                vault(f, 0xFF8E9298, 0, 0.8f, L, S / 2);
                vault(f, 0xFF8E9298, 0, S / 2, L, S - 0.8f);
                for (int k = 0; k < 2; k++) {
                    float v = S * (0.16f + k * 0.5f);
                    f.rect(0xFF9CC0D2, 3, v, L - 3, v + S * 0.08f);
                    for (float u = 5; u < L - 3; u += 4) f.line(0xFF6A8EA0, 0.5f, u, v, u, v + S * 0.08f);
                }
                f.rect(0xFF8A4A38, 0, 0, 2.2f, S);
                f.rect(0xFF8A4A38, L - 2.2f, 0, L, S);
                for (int k = 0; k < 3; k++) f.circle(0xFF5A5E64, L * (0.3f + k * 0.2f), S / 2, 1.6f);
            }
        };
        d.ground = City.LOT;
        d.districts = Variants.IND | Variants.MID | Variants.OLD;
        d.minS = 9;
        d.minL = 14;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_FACTORY;
        d.roofCol = 0xFF8E9298;
        out.add(d);
    }

    /** A gondola cabin hanging from a cable: its shadow far below, the hanger and the cabin. */
    static void gondola(F f, float u, float v, int col) {
        f.roundRect(0x40000000, u - 4 + 7, v - 3.5f + 10, u + 4 + 7, v + 3.5f + 10, 2);
        f.line(0xFF3A3A3C, 0.8f, u, v - 0.6f, u, v + 0.6f);
        f.roundRect(City.darken(col, 0.65f), u - 4, v - 3.5f, u + 4, v + 3.5f, 2);
        f.roundRect(col, u - 3.5f, v - 3, u + 3.5f, v + 3, 1.6f);
        f.rect(0xFF33414E, u - 3.5f, v - 1.6f, u + 3.5f, v + 1.6f);
        f.roundRect(City.lighten(col, 0.25f), u - 2.4f, v - 2, u + 2.4f, v + 2, 1);
        f.circle(0xFF3A3A3C, u, v, 0.8f);
    }

    // ------------------------------------------------------------------ 3. Cable Car Station

    static void cableCar(List<Landmarks.Def> out) {
        // The valley station of a gondola lift: cables leave over a lattice pylon and run off the block.
        D d = new D("Cable Car Station", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                int lv = Math.min(5, S - 5), lu = lv + 2;
                return new int[][]{B(1, (S - lv) / 2, lu, lv, 3), B(lu + 3, 1, 3, 2, 1)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float se = (q[0][0] + q[0][2]) * T, vc = (q[0][1] + q[0][3] / 2f) * T;
                f.rect(0xFF6E9A52, 0, 0, L, S);
                for (int k = 0; k < 40; k++) {
                    float u = r.nextFloat() * L, v = r.nextFloat() * S;
                    f.circle(r.nextBoolean() ? 0x1A000000 : 0x18FFFFFF, u, v, 2 + r.nextFloat() * 3);
                }
                // A paved forecourt round the station and a path to the street.
                float s0 = q[0][1] * T, s1 = s0 + q[0][3] * T;
                f.rect(0xFFC9C1B0, 4, s0 - 8, se + 4, s1 + 8);
                f.rect(0xFFC9C1B0, se - 30, s1 + 8, se - 18, S);
                f.frame(0xFF9E9684, 0.6f, 4, s0 - 8, se + 4, s1 + 8);
                // The queue lines.
                for (int k = 0; k < 3; k++) {
                    float v = s1 + 2 + k * 2;
                    f.line(0xFF4A5058, 0.5f, 10, v, se - 32, v);
                }
                // Rocks and pines on the slope.
                for (int k = 0; k < 7; k++) {
                    float u = se + 14 + r.nextFloat() * (L - se - 20), v = r.nextBoolean() ? 6 + r.nextFloat() * (vc - 26) : vc + 20 + r.nextFloat() * (S - vc - 26);
                    if (v < 4 || v > S - 4) continue;
                    if (k % 3 == 0) {
                        f.poly(0x40000000, u + 1, v + 1, u + 7, v - 1, u + 8, v + 5, u + 2, v + 6);
                        f.poly(0xFF9A948A, u - 1, v - 1, u + 5, v - 3, u + 6, v + 3, u, v + 4);
                        f.poly(0xFFB8B2A6, u - 1, v - 1, u + 5, v - 3, u + 3, v + 1);
                    } else f.tree(u, v, 4.5f + r.nextFloat() * 2, 0xFF2E5E3A);
                }
                for (float v = 6; v < S; v += 14) f.tree(3.5f, v, 3.5f, 0xFF2E5E3A);
                // The pylon: a lattice tower with a cross arm.
                float pu = se + (L - se) * 0.55f, cs = 7;
                f.rect(0x48000000, pu - 4 + 9, vc - 4 + 12, pu + 4 + 9, vc + 4 + 12);
                f.line(0x40000000, 2, pu + 9, vc - cs - 2 + 12, pu + 9, vc + cs + 2 + 12);
                f.rect(0xFF8A8E92, pu - 4, vc - 4, pu + 4, vc + 4);
                f.line(0xFF5A5E62, 0.6f, pu - 4, vc - 4, pu + 4, vc + 4);
                f.line(0xFF5A5E62, 0.6f, pu - 4, vc + 4, pu + 4, vc - 4);
                f.rect(0xFFB0B4B8, pu - 1.5f, vc - cs - 3, pu + 1.5f, vc + cs + 3);
                // The cables (up and down lines) with their shadows, and the cabins along them.
                for (int s = -1; s <= 1; s += 2) {
                    float v = vc + s * cs;
                    f.line(0x30000000, 0.8f, se, v + 8, L, v + 12);
                }
                for (int s = -1; s <= 1; s += 2) {
                    float v = vc + s * cs;
                    f.line(0xFF2A2A2C, 0.8f, se - 4, v, L, v);
                    f.circle(0xFF3A3A3C, pu, v, 1.6f);
                    f.circle(0xFFD0D0D0, pu, v, 0.7f);
                }
                int[] cols = {0xFFC8302A, 0xFFE8E4DA, 0xFF2E6AA8};
                int k = 0;
                for (float u = se + 22; u < L + 10; u += 40, k++) {
                    gondola(f, u, vc - cs, cols[k % 3]);
                    gondola(f, u + 20, vc + cs, cols[(k + 1) % 3]);
                }
                f.lamp(se + 6, s0 - 10);
                f.lamp(se + 6, s1 + 10);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                if (part == 1) {
                    // A timber ticket hut with a shingle gable.
                    f.rect(0xFF5A3E28, 0, 0, L, S);
                    f.rect(0xFF8A6440, 0.8f, 0.8f, L - 0.8f, S / 2);
                    f.rect(0xFF6E4E32, 0.8f, S / 2, L - 0.8f, S - 0.8f);
                    for (float u = 2; u < L; u += 2.5f) f.line(0x22000000, 0.4f, u, 0.8f, u, S - 0.8f);
                    f.line(0xFFA88060, 1, 0, S / 2, L, S / 2);
                    f.sign(city, "TICKETS", L / 2, S / 2, 4, 0xFFF0E0C0);
                    return;
                }
                // The station: a standing-seam gable along the line, open at the far end over the bullwheel.
                float open = Math.min(S * 0.9f, L * 0.4f);
                float ge = L - open;
                f.rect(0xFF3A3E44, 0, 0, L, S);
                ribbedAcross(f, 0xFF7A8490, 0.8f, 0.8f, ge, S / 2, 2.5f);
                ribbedAcross(f, 0xFF5E6772, 0.8f, S / 2, ge, S - 0.8f, 2.5f);
                f.line(0xFFA8B0B8, 1, 0.8f, S / 2, ge, S / 2);
                f.rect(0xFF6A6E74, ge, 0.8f, L - 0.8f, S - 0.8f);
                f.rect(0xFF8EB2C4, ge, 0.8f, ge + 2.5f, S - 0.8f);
                // The bullwheel the cable turns round.
                float wc = ge + open / 2 + 1, wr = Math.min(open, S) * 0.36f;
                f.circle(0x40000000, wc + 1.5f, S / 2 + 2, wr);
                f.ring(0xFF2A2A2C, 1.8f, wc, S / 2, wr);
                for (int k = 0; k < 6; k++) {
                    double a = k * Math.PI / 3;
                    f.line(0xFF4A4E54, 0.8f, wc, S / 2, wc + (float) Math.cos(a) * wr, S / 2 + (float) Math.sin(a) * wr);
                }
                f.circle(0xFFD8B030, wc, S / 2, 2);
                f.line(0xFF2A2A2C, 0.8f, wc, S / 2 - wr, L, S / 2 - wr);
                f.line(0xFF2A2A2C, 0.8f, wc, S / 2 + wr, L, S / 2 + wr);
                gondola(f, ge + 5, S / 2 - wr, 0xFFC8302A);
                f.sign(city, "CABLE CAR", ge / 2, S / 2, 6, 0xFFE8E4DA);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.PARK | Variants.MID | Variants.SUB;
        d.minS = 10;
        d.minL = 14;
        d.base = City.OFFICE;
        d.inside = Variants.I_CAFE;
        d.roofCol = 0xFF7A8490;
        out.add(d);
    }

    /** A sheet roof with its ribs across u. */
    static void ribbedAcross(F f, int col, float u0, float v0, float u1, float v1, float step) {
        f.rect(col, u0, v0, u1, v1);
        for (float u = u0 + step; u < u1; u += step) f.line(City.darken(col, 0.85f), 0.5f, u, v0, u, v1);
    }

    /** A monorail car on its beam: a rounded white body with a coloured band and dark windows. */
    static void monoTrain(F f, float u0, float u1, float v) {
        float w = 5.2f, n = Math.max(1, Math.round((u1 - u0) / 30)), cl = (u1 - u0) / n;
        for (int k = 0; k < n; k++) {
            float a = u0 + k * cl + 0.6f, b = u0 + (k + 1) * cl - 0.6f;
            float ra = k == 0 ? 5 : 1, rb = k == n - 1 ? 5 : 1;
            f.roundRect(0x48000000, a + 5, v - w + 8, b + 5, v + w + 8, 3);
            f.roundRect(0xFFB8BEC4, a, v - w, b, v + w, Math.max(ra, rb));
            f.roundRect(0xFFF2F4F6, a + 0.5f, v - w + 0.5f, b - 0.5f, v + w - 0.5f, Math.max(ra, rb));
            f.rect(0xFF2E7AB8, a + 3, v - w + 0.5f, b - 3, v - w + 1.6f);
            f.rect(0xFF2E7AB8, a + 3, v + w - 1.6f, b - 3, v + w - 0.5f);
            f.rect(0xFFD8DEE4, a + 4, v - 1.6f, b - 4, v + 1.6f);
            if (k == 0) f.roundRect(0xFF26323E, a + 0.8f, v - w + 1.6f, a + 4, v + w - 1.6f, 2);
            if (k == n - 1) f.roundRect(0xFF26323E, b - 4, v - w + 1.6f, b - 0.8f, v + w - 1.6f, 2);
        }
    }

    // ------------------------------------------------------------------ 4. Monorail Station

    static void monorail(List<Landmarks.Def> out) {
        // A concrete beam on T-piers strides over a plaza; the station straddles it halfway, a train pulling out.
        D d = new D("Monorail Station", T_BUILDING) {
            int bv(int S) {
                return Math.max(3, Math.round(S * 0.4f));
            }

            @Override
            int[][] lp(int L, int S) {
                int lu = Math.max(5, L / 3), lv = 4, v0 = bv(S) - 2;
                return new int[][]{B((L - lu) / 2, v0, lu, lv, 3), B(L - 4, S - 3, 3, 2, 1)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float bvc = bv(Math.round(S / T)) * T;
                float su0 = q[0][0] * T, su1 = su0 + q[0][2] * T;
                f.rect(0xFFD8D2C4, 0, 0, L, S);
                for (float u = 0; u < L; u += 10) for (float v = 0; v < S; v += 10)
                    if ((((int) (u / 10)) + ((int) (v / 10))) % 2 == 0) f.rect(0xFFCEC7B8, u, v, u + 10, v + 10);
                // A planted strip under the beam.
                f.rect(0xFF8A9A70, 0, bvc - 9, L, bvc + 9);
                f.rect(0xFF6E9A52, 0, bvc - 8, L, bvc + 8);
                for (float u = 6; u < L; u += 9) f.bush(u, bvc + ((int) (u / 9) % 2 == 0 ? -4 : 4), 2.6f);
                // Trees, benches and bike racks on the open plaza.
                float pv0 = bvc + 30, pv1 = S - 6;
                if (pv1 - pv0 > 12) {
                    for (float u = 12; u < L - 50; u += 26) {
                        f.rect(0xFF9A9284, u - 5, (pv0 + pv1) / 2 - 5, u + 5, (pv0 + pv1) / 2 + 5);
                        f.tree(u, (pv0 + pv1) / 2, 6, 0xFF4E8A44);
                        f.bench(u + 12, (pv0 + pv1) / 2, false);
                    }
                }
                for (float u = 8; u < L - 8; u += 26) {
                    f.tree(u, 6, 4.5f, 0xFF5A9048);
                }
                float bk = su0 - 8;
                for (int k = 0; k < 6; k++) {
                    float u = bk - k * 4;
                    f.line(0xFF8A9096, 0.6f, u, bvc + 14, u, bvc + 24);
                    f.line(k % 2 == 0 ? 0xFF2E5A8A : 0xFFB8302A, 1.2f, u + 1, bvc + 15, u + 1, bvc + 23);
                }
                // Piers, the beam's shadow, the beam, and the train.
                float sw = 4.2f;
                f.rect(0x30000000, 0, bvc - sw + 9, L, bvc + sw + 12);
                for (float u = 14; u < L; u += 40) {
                    if (u > su0 - 6 && u < su1 + 6) continue;
                    f.rect(0x40000000, u - 3 + 4, bvc - 3 + 5, u + 3 + 4, bvc + 3 + 5);
                    f.rect(0xFFB8B4AA, u - 3, bvc - 3, u + 3, bvc + 3);
                    f.rect(0xFFC8C4BA, u - 2, bvc - sw - 3, u + 2, bvc + sw + 3);
                }
                f.rect(0xFFA8A49A, 0, bvc - sw, L, bvc + sw);
                f.rect(0xFFC4C0B6, 0, bvc - sw, L, bvc - sw + 1.4f);
                f.rect(0xFF8E8A82, 0, bvc - 1.2f, L, bvc + 1.2f);
                for (float u = 0; u < L; u += 20) f.line(0x30000000, 0.5f, u, bvc - sw, u, bvc + sw);
                float tl = Math.min(64, su0 - 10);
                if (tl > 30) monoTrain(f, su0 - tl - 2, su0 - 2, bvc);
                // Lamps along the plaza.
                for (float u = 20; u < L; u += 40) f.lamp(u, bvc + 12);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                if (part == 1) {
                    // A glass lift and stair pavilion.
                    f.rect(0xFF5A646E, 0, 0, L, S);
                    f.rect(0xFF8CB0C4, 1, 1, L - 1, S - 1);
                    f.line(0xC0FFFFFF, 0.6f, 2, 2, L - 2, 2);
                    f.rect(0xFF4A545E, L * 0.6f, 2, L - 2, S - 2);
                    return;
                }
                // The station: a white elliptical canopy ribbed across, a glazed strip over the platforms each side.
                f.roundRect(0xFF8A9096, 0, 0, L, S, S * 0.45f);
                f.roundRect(0xFFF0F2F4, 0.8f, 0.8f, L - 0.8f, S - 0.8f, S * 0.42f);
                f.roundRect(0xFFDADEE2, 0.8f, S / 2, L - 0.8f, S - 0.8f, S * 0.3f);
                for (int s = 0; s < 2; s++) {
                    float v = s == 0 ? S * 0.22f : S * 0.66f;
                    f.rect(0xFF8EB6CC, S * 0.35f, v, L - S * 0.35f, v + S * 0.12f);
                }
                for (float u = S * 0.35f; u < L - S * 0.3f; u += 5) {
                    f.line(0xFFB4BAC0, 0.6f, u, 2, u, S - 2);
                }
                f.rect(0xFFB4B0A6, 0, S / 2 - 1.5f, L, S / 2 + 1.5f);
                f.line(0xFF8E8A82, 0.6f, 0, S / 2, L, S / 2);
                f.sign(city, "MONORAIL", L / 2, S * 0.3f, 6, 0xFF2E7AB8);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.MID;
        d.minS = 10;
        d.minL = 14;
        d.base = City.OFFICE;
        d.inside = Variants.I_SHOWROOM;
        d.roofCol = 0xFFF0F2F4;
        out.add(d);
    }

    /** A helicopter from above, rotor parked at an angle. */
    static void heli(F f, float u, float v, float a, int col, float rot) {
        float deg = f.ang(a);
        f.turn(u, v, a);
        Canvas c = f.c;
        Paint p = f.p;
        float[] o = off(deg, 3, 4);
        oval(c, p, 0x48000000, -9 + o[0], -4.5f + o[1], 9 + o[0], 4.5f + o[1]);
        line(c, p, 0x48000000, 2, -8 + o[0], o[1], -24 + o[0], o[1]);
        // Skids.
        line(c, p, 0xFF3A3A3C, 0.8f, -7, -5, 7, -5);
        line(c, p, 0xFF3A3A3C, 0.8f, -7, 5, 7, 5);
        line(c, p, City.darken(col, 0.6f), 2.2f, -6, 0, -23, 0);
        rect(c, p, City.darken(col, 0.6f), -25, -3.5f, -22, 3.5f);
        oval(c, p, City.darken(col, 0.6f), -9, -4.5f, 9, 4.5f);
        oval(c, p, col, -8.4f, -3.9f, 8.4f, 3.9f);
        oval(c, p, 0xFF2E3C4A, 2.5f, -3.4f, 8.6f, 3.4f);
        oval(c, p, 0x60FFFFFF, 4.5f, -2.2f, 7, -0.6f);
        rect(c, p, City.lighten(col, 0.3f), -6, -1.2f, 0, 1.2f);
        c.rotate(rot);
        for (int k = 0; k < 4; k++) {
            c.rotate(90);
            line(c, p, 0x30000000, 1.4f, 2 + o[0], o[1], 17 + o[0], o[1]);
            line(c, p, 0xFF2A2A2C, 1.2f, 1, 0, 16, 0);
        }
        circle(c, p, 0xFF4A4A4C, 0, 0, 1.6f);
        c.restore();
    }

    /** A helipad: a concrete disc, the yellow ring and a white H, edge lights. */
    static void pad(F f, float u, float v, float rad) {
        f.rect(0xFF8E8A82, u - rad - 2, v - rad - 2, u + rad + 2, v + rad + 2);
        f.rect(0xFFA8A49C, u - rad - 1, v - rad - 1, u + rad + 1, v + rad + 1);
        f.circle(0xFF6A6C70, u, v, rad);
        f.ring(0xFFE0C040, 1.4f, u, v, rad * 0.8f);
        float h = rad * 0.38f;
        f.rect(0xFFF0F0EC, u - h, v - h, u - h * 0.6f, v + h);
        f.rect(0xFFF0F0EC, u + h * 0.6f, v - h, u + h, v + h);
        f.rect(0xFFF0F0EC, u - h, v - h * 0.15f, u + h, v + h * 0.15f);
        for (int k = 0; k < 12; k++) {
            double a = k * Math.PI / 6;
            f.circle(0xFF7ADA6A, u + (float) Math.cos(a) * rad * 0.95f, v + (float) Math.sin(a) * rad * 0.95f, 0.8f);
        }
    }

    // ------------------------------------------------------------------ 5. Heliport

    static void heliport(List<Landmarks.Def> out) {
        // A row of pads on a concrete apron with grass between, a terminal and a vaulted hangar behind.
        D d = new D("Heliport", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                int tl = Math.max(4, (L - 3) / 2 - 1), hl = L - 3 - tl;
                hl = Math.max(4, hl);
                return new int[][]{B(1, 1, tl, 3, 2), B(tl + 2, 1, hl, 3, 2)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float av = 4 * T + 4;
                f.rect(0xFF739C55, 0, 0, L, S);
                for (int k = 0; k < 30; k++) f.circle(0x14000000, r.nextFloat() * L, r.nextFloat() * S, 2 + r.nextFloat() * 3);
                // The apron in front of the buildings and a taxi line.
                f.rect(0xFFA6A298, 0, 0, L, av);
                for (float u = 0; u < L; u += 16) f.line(0x18000000, 0.5f, u, 0, u, av);
                f.line(0xFFE0C040, 1, 6, av - 6, L - 6, av - 6);
                // Pads in a row (two rows on deep blocks), each with a helicopter on most.
                float free = S - av - 4;
                int rows = free > 150 ? 2 : 1;
                float rad = Math.min(26, Math.min(free / rows / 2 - 4, 28));
                int n = Math.max(1, (int) ((L - 8) / (rad * 2 + 14)));
                int[] cols = {0xFFC8302A, 0xFFE8E4DA, 0xFF2E4A7A, 0xFFE0A030};
                int k = 0;
                for (int ro = 0; ro < rows; ro++) {
                    float v = av + 2 + free * (ro + 0.5f) / rows;
                    for (int i = 0; i < n; i++, k++) {
                        float u = (L) * (i + 0.5f) / n;
                        f.rect(0xFFA6A298, u - 3, av - 6, u + 3, v);
                        pad(f, u, v, rad);
                        if (k % 3 != 2) heli(f, u, v, k % 2 == 0 ? -20 : 200, cols[k % cols.length], 20 + k * 17);
                    }
                }
                // A windsock on its mast, a fuel bowser, and edge lights along the apron.
                float wu = L - 8, wv = av + 8;
                f.circle(0xFF4A4A4C, wu, wv, 1.2f);
                f.poly(0x30000000, wu + 1, wv + 2, wu + 15, wv + 6, wu + 15, wv + 9, wu + 1, wv + 5);
                for (int j = 0; j < 4; j++) {
                    float a0 = j / 4f, a1 = (j + 1) / 4f;
                    f.poly(j % 2 == 0 ? 0xFFE86A28 : 0xFFF0F0EC, wu + 13 * a0, wv - 1.8f + 1.0f * a0 + 3 * a0, wu + 13 * a1, wv - 1.8f + 1.0f * a1 + 3 * a1,
                            wu + 13 * a1, wv + 1.8f - 1.0f * a1 + 3 * a1, wu + 13 * a0, wv + 1.8f - 1.0f * a0 + 3 * a0);
                }
                lorry(f, 20, av - 14, 0, 18, 0xFFC8302A, 0xFFD8D8D0, true);
                for (float u = 4; u < L; u += 12) f.circle(0xFF5A8AE0, u, av, 0.7f);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                if (part == 1) {
                    // The hangar: a shallow vault in ribbed steel, roof lights in pairs.
                    f.rect(0xFF4A4E52, 0, 0, L, S);
                    vault(f, 0xFFA4AAB0, 0.8f, 0.8f, L - 0.8f, S - 0.8f);
                    for (float u = 4; u < L - 2; u += 3) f.line(0x18000000, 0.5f, u, 1, u, S - 1);
                    for (float u = 8; u < L - 6; u += 16) f.rect(0xFFB8D0DC, u, S * 0.25f, u + 6, S * 0.4f);
                    f.sign(city, "HANGAR", L / 2, S / 2, 6, 0xFF2A2A2A);
                    return;
                }
                // The terminal: a flat roof with a glass control cab on top and a rooftop H.
                flatRoof(f.c, f.p, f.x(0, 0), f.y(0, 0), f.x(L, S), f.y(L, S), 0xFFC4C0B6);
                f.specks(r, 30, 0x18000000, 0x18FFFFFF, 2, 2, L - 2, S - 2, 1);
                float cu = L - S * 0.55f;
                f.rect(0x40000000, cu - S * 0.3f + 2, S * 0.2f + 2, cu + S * 0.3f + 2, S * 0.8f + 2);
                f.rect(0xFF3A4A58, cu - S * 0.3f, S * 0.2f, cu + S * 0.3f, S * 0.8f);
                f.rect(0xFF7AA6C0, cu - S * 0.25f, S * 0.25f, cu + S * 0.25f, S * 0.75f);
                f.rect(0xFFD8DCE0, cu - S * 0.15f, S * 0.35f, cu + S * 0.15f, S * 0.65f);
                fanUnit(f, S * 0.4f, S * 0.5f, 3);
                f.circle(0xFFD04040, cu, S * 0.5f, 0.9f);
                f.sign(city, "HELIPORT", L * 0.4f, S / 2, 6, 0xFF2A2A2A);
            }
        };
        d.ground = City.LOT;
        d.districts = Variants.DOWN | Variants.IND | Variants.MID;
        d.minS = 10;
        d.minL = 12;
        d.base = City.OFFICE;
        d.inside = Variants.I_OFFICE;
        d.roofCol = 0xFFC4C0B6;
        out.add(d);
    }
}
