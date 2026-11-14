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
        fuelDepot(out);
        recycling(out);
        waterWorks(out);
        sawmill(out);
        glassworks(out);
        printingWorks(out);
        filmStudios(out);
        radioTelescope(out);
        researchCampus(out);
        robotics(out);
        coldStore(out);
        brickKilns(out);
        gasometer(out);
        georgianCrescent(out);
        ecoVillage(out);
        penthouseTower(out);
        studentHalls(out);
        retirementVillage(out);
        treehouseHotel(out);
        brownstoneRow(out);
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
                float ang = 52, sp = 17, len = 30;
                float ca = (float) Math.cos(Math.toRadians(ang)), sa = (float) Math.sin(Math.toRadians(ang));
                int[] cols = {0xFFC8382E, 0xFF2E6AA8, 0xFFE2B428, 0xFF3A8A4E, 0xFFE8E4DC};
                int k = 0;
                for (float u = ua + 20; u + 6 < ub; u += sp, k++) {
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
                float cv0 = vc - iw - 3, cv1 = vc + iw + 3;
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
        line(c, p, 0xFF3A3A3C, 0.5f, -l + sl * 0.5f - 1.5f, 0, -l + sl * 0.5f + 1.5f, 0);
        line(c, p, 0xFF3A3A3C, 0.5f, -l + sl * 0.5f, -1.8f, -l + sl * 0.5f, 1.8f);
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
                    float tl = Math.min(46, su - fu - 4);
                    if (tl > 26) tram(f, su - tl / 2 - 2 - (k % 2) * 4, rv[k], 0, tl, cols[k]);
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
                float Lr = q[0][2] * T, Sr = q[0][3] * T, open = Math.min(Sr * 0.9f, Lr * 0.4f);
                float pu = se + (L - se) * 0.55f, cs = Math.min(open, Sr) * 0.36f;
                f.rect(0x48000000, pu - 6 + 9, vc - 6 + 12, pu + 6 + 9, vc + 6 + 12);
                f.rect(0x40000000, pu - 2 + 9, vc - cs - 4 + 12, pu + 2 + 9, vc + cs + 4 + 12);
                f.rect(0xFF6A6E72, pu - 6, vc - 6, pu + 6, vc + 6);
                f.frame(0xFF9A9EA2, 1, pu - 5.5f, vc - 5.5f, pu + 5.5f, vc + 5.5f);
                f.line(0xFF9A9EA2, 0.6f, pu - 5.5f, vc - 5.5f, pu + 5.5f, vc + 5.5f);
                f.line(0xFF9A9EA2, 0.6f, pu - 5.5f, vc + 5.5f, pu + 5.5f, vc - 5.5f);
                f.rect(0xFF8A8E92, pu - 2, vc - cs - 4, pu + 2, vc + cs + 4);
                f.rect(0xFFB8BCC0, pu - 2, vc - cs - 4, pu - 0.8f, vc + cs + 4);
                f.rect(0xFFC8302A, pu - 2, vc - cs - 4, pu + 2, vc - cs - 2.5f);
                f.rect(0xFFC8302A, pu - 2, vc + cs + 2.5f, pu + 2, vc + cs + 4);
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

    /** A round storage tank from above: floating-roof (open) or cone-roofed. */
    static void tank(F f, float u, float v, float rad, int col, boolean cone) {
        f.circle(0x48000000, u + rad * 0.12f + 2, v + rad * 0.16f + 3, rad);
        f.circle(City.darken(col, 0.62f), u, v, rad);
        f.circle(col, u, v, rad - 1.2f);
        if (cone) {
            int n = 16;
            for (int k = 0; k < n; k++) {
                double a0 = k * 2 * Math.PI / n, a1 = (k + 1) * 2 * Math.PI / n, am = (a0 + a1) / 2;
                float sh = 1f + 0.22f * (float) Math.cos(am + 3 * Math.PI / 4);
                f.poly(Landmarks.shade(col, sh), u, v, u + (float) Math.cos(a0) * (rad - 1.2f), v + (float) Math.sin(a0) * (rad - 1.2f),
                        u + (float) Math.cos(a1) * (rad - 1.2f), v + (float) Math.sin(a1) * (rad - 1.2f));
            }
            f.circle(City.darken(col, 0.8f), u, v, 2);
            f.circle(0xFF6A6E72, u + rad * 0.4f, v - rad * 0.3f, 1.4f);
        } else {
            f.circle(City.darken(col, 0.82f), u, v, rad - 3);
            f.ring(City.darken(col, 0.7f), 0.6f, u, v, rad - 4.5f);
            for (int k = 0; k < 8; k++) {
                double a = k * Math.PI / 4;
                f.line(0x18000000, 0.5f, u, v, u + (float) Math.cos(a) * (rad - 4.5f), v + (float) Math.sin(a) * (rad - 4.5f));
            }
            f.line(0xFF5A5E62, 1.2f, u, v, u + rad - 1.5f, v - 0.5f);
            for (float t = 2; t < rad - 3; t += 2) f.line(0xFF8A8E92, 0.5f, u + t, v - 1.2f, u + t, v + 0.2f);
            f.circle(0xFF4A4E52, u, v, 1.5f);
        }
        // The stair spiralling up the shell.
        f.line(0xFF5A5E62, 1, u - rad * 0.7f, v + rad * 0.7f, u - rad * 0.95f, v + rad * 0.2f);
    }

    // ------------------------------------------------------------------ 6. Fuel Depot

    static void fuelDepot(List<Landmarks.Def> out) {
        // Big round tanks, each in its walled bund, colour-coded pipework running out to a road-tanker gantry.
        D d = new D("Fuel Depot", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                return new int[][]{B(1, S - 3, 3, 2, 1), B(L - 4, S - 3, 3, 2, 1)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float H = (q[0][1] - 1) * T - 4;
                f.rect(0xFF9A968C, 0, 0, L, S);
                f.specks(r, 70, 0x1C000000, 0x16FFFFFF, 0, 0, L, S, 1.4f);
                // The road along the front and the fence round it all.
                f.rect(0xFF5E6062, 0, q[0][1] * T - 10, L, S);
                for (float u = 4; u < L; u += 14) f.rect(0xFFE0D8C0, u, q[0][1] * T - 5.5f, u + 7, q[0][1] * T - 4.5f);
                f.frame(0xFF6A7076, 0.7f, 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                // The bunds, evenly along the block, a tank in each.
                float b = Math.min(H - 6, 110);
                int n = Math.max(1, (int) ((L - 6) / (b + 8) + 0.5f));
                b = Math.min(b, (L - 6) / n - 8);
                float gap = (L - n * b) / (n + 1);
                int[] cols = {0xFFE8E6DE, 0xFFE8E6DE, 0xFF9AAE8E, 0xFFE8E6DE};
                float pv = H + 6;
                for (int k = 0; k < n; k++) {
                    float u0 = gap + k * (b + gap), v0 = 4 + (H - 2 - b) / 2, u1 = u0 + b, v1 = v0 + b;
                    f.rect(0xFFB0A890, u0, v0, u1, v1);
                    f.specks(r, 16, 0x20000000, 0x20FFFFFF, u0 + 2, v0 + 2, u1 - 2, v1 - 2, 1.2f);
                    f.rect(0x30000000, u0 + 2.5f, v0 + 2.5f, u1, v0 + 4);
                    f.frame(0xFFCEC8BA, 2.5f, u0, v0, u1, v1);
                    f.line(0xFF8E887A, 0.5f, u0 - 1.2f, v1 + 1.2f, u1 + 1.2f, v1 + 1.2f);
                    float cu = (u0 + u1) / 2, cv = (v0 + v1) / 2, rad = b / 2 - 7;
                    // The pipe from the tank out to the manifold.
                    f.line(0xFF5A5E62, 2.2f, cu + rad * 0.3f, cv, cu + rad * 0.3f, pv);
                    f.line(0xFFD8B030, 1.4f, cu + rad * 0.3f, cv, cu + rad * 0.3f, pv);
                    tank(f, cu, cv, rad, cols[k % 4], k % 3 == 1);
                    f.rect(0xFF6A6E72, u0 + 4, v1 - 1.5f, u0 + 9, v1 + 1.5f);
                }
                // The manifold: three coloured pipes on sleepers along the front of the bunds.
                for (float u = 4; u < L - 4; u += 8) f.rect(0xFF6A645A, u, pv - 3.5f, u + 1.4f, pv + 3.5f);
                f.line(0x40000000, 1.6f, 4, pv + 3, L - 4, pv + 3);
                f.line(0xFFD8B030, 1.4f, 4, pv - 2.2f, L - 4, pv - 2.2f);
                f.line(0xFF8A9096, 1.4f, 4, pv, L - 4, pv);
                f.line(0xFFB8402E, 1.4f, 4, pv + 2.2f, L - 4, pv + 2.2f);
                // Pipes into the gantry, its canopy and a tanker under it.
                float gu = (q[1][0] - 4) * T, gv = q[1][1] * T;
                f.line(0xFF8A9096, 1.4f, gu + 30, pv, gu + 30, gv);
                if (gu > q[0][0] * T + 4 * T) {
                    lorry(f, gu, gv + 14, 180, 34, 0xFFE8E4DA, 0xFFD8D8D0, true);
                    f.rect(0x38000000, gu - 18 + 2, gv + 6 + 3, gu + 18 + 2, gv + 22 + 3);
                    f.rect(0xC0A8AEB4, gu - 18, gv + 6, gu + 18, gv + 22);
                    for (float u = gu - 18; u <= gu + 18; u += 6) f.line(0xFF7A8088, 0.5f, u, gv + 6, u, gv + 22);
                    f.frame(0xFF6A7078, 0.8f, gu - 18, gv + 6, gu + 18, gv + 22);
                }
                f.rect(0xFFB8302A, 6, S - 4, 9, S - 2);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                if (part == 0) {
                    flatRoof(f.c, f.p, f.x(0, 0), f.y(0, 0), f.x(L, S), f.y(L, S), 0xFFC8C2B4);
                    fanUnit(f, L * 0.7f, S * 0.5f, 3.5f);
                    f.sign(city, "FUEL DEPOT", L / 2, S / 2, 5, 0xFFB8302A);
                    return;
                }
                // The pump house: ribbed steel, roof vents and a pump skid beside its louvres.
                ribbedAcross(f, 0xFF8A949C, 0, 0, L, S, 2.2f);
                f.frame(0xFF5A646C, 1, 0, 0, L, S);
                for (int k = 0; k < 3; k++) f.circle(0xFF5A646C, L * (0.25f + k * 0.25f), S / 2, 2.2f);
                for (int k = 0; k < 3; k++) f.circle(0xFFA8B0B8, L * (0.25f + k * 0.25f), S / 2, 1.2f);
            }
        };
        d.ground = City.LOT;
        d.districts = Variants.IND;
        d.minS = 10;
        d.minL = 14;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_FACTORY;
        d.roofCol = 0xFF8A949C;
        out.add(d);
    }

    /** A heap of loose material: overlapping lumps in shades of one colour. */
    static void heap(F f, Random r, float u0, float v0, float u1, float v1, int col) {
        float cu = (u0 + u1) / 2, cv = (v0 + v1) / 2, ru = (u1 - u0) / 2, rv = (v1 - v0) / 2;
        f.oval(0x38000000, u0 + 1.5f, v0 + 2, u1 + 1.5f, v1 + 2);
        f.oval(City.darken(col, 0.75f), u0, v0, u1, v1);
        for (int k = 0; k < 14; k++) {
            double a = r.nextFloat() * Math.PI * 2;
            float d = (float) Math.sqrt(r.nextFloat()) * 0.65f;
            float u = cu + (float) Math.cos(a) * ru * d, v = cv + (float) Math.sin(a) * rv * d;
            float s = Math.min(ru, rv) * (0.18f + r.nextFloat() * 0.2f);
            f.circle(Landmarks.shade(col, 0.82f + r.nextFloat() * 0.4f), u, v, s);
        }
        f.circle(City.lighten(col, 0.2f), cu - ru * 0.2f, cv - rv * 0.2f, Math.min(ru, rv) * 0.25f);
    }

    /** An open skip from above with its load. */
    static void skip(F f, Random r, float u, float v, float a, int col, int load) {
        f.turn(u, v, a);
        Canvas c = f.c;
        Paint p = f.p;
        float[] o = off(f.ang(a), 1.5f, 2);
        rect(c, p, 0x48000000, -9 + o[0], -5 + o[1], 9 + o[0], 5 + o[1]);
        rect(c, p, City.darken(col, 0.7f), -9, -5, 9, 5);
        rect(c, p, col, -8.4f, -4.4f, 8.4f, 4.4f);
        rect(c, p, City.darken(load, 0.7f), -7, -3.4f, 7, 3.4f);
        for (int k = 0; k < 8; k++)
            rect(c, p, Landmarks.shade(load, 0.8f + r.nextFloat() * 0.4f), -6.5f + r.nextFloat() * 10, -3 + r.nextFloat() * 4, -4 + r.nextFloat() * 10, -1 + r.nextFloat() * 4);
        c.restore();
    }

    // ------------------------------------------------------------------ 7. Recycling Centre

    static void recycling(List<Landmarks.Def> out) {
        // A sorting shed, a row of colour-coded bays piled high, skips, and bales stacked off the conveyor.
        D d = new D("Recycling Centre", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                int lv = Math.max(3, S / 2 - 2), lu = Math.max(lv + 1, L / 2);
                return new int[][]{B(1, 1, lu, lv, 2), B(L - 3, 1, 2, 2, 1)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float se = (q[0][0] + q[0][2]) * T, sv = (q[0][1] + q[0][3]) * T;
                f.rect(0xFF8E8A84, 0, 0, L, S);
                f.specks(r, 70, 0x1E000000, 0x14FFFFFF, 0, 0, L, S, 1.6f);
                // Yellow lanes and a weighbridge by the gate.
                f.line(0xFFE0C040, 1, 4, sv + 8, L - 4, sv + 8);
                f.rect(0xFF6A6C6E, L - 3 * T - 2, 3 * T + 4, L - 4, 3 * T + 24);
                f.frame(0xFFE0C040, 0.8f, L - 3 * T - 2, 3 * T + 4, L - 4, 3 * T + 24);
                // The bays along the far side: concrete walls, a coloured board and a heap in each.
                int[] cols = {0xFF4E8A4A, 0xFFA8845A, 0xFF4A78B0, 0xFF9AA0A6, 0xFFD8C890, 0xFF8A5A3A, 0xFFE8E4DA};
                float bv0 = sv + 14, bv1 = S - 3, bw = Math.max(26, Math.min(36, (L - 8) / 6));
                int n = (int) ((L - 8) / bw);
                float u0 = (L - n * bw) / 2;
                for (int k = 0; k < n; k++) {
                    float a = u0 + k * bw, b = a + bw;
                    f.rect(0xFF7A766E, a, bv0, b, bv1);
                    heap(f, r, a + 3, bv0 + (bv1 - bv0) * 0.25f, b - 3, bv1 - 3, cols[k % cols.length]);
                    f.rect(cols[k % cols.length], a + 2, bv1 - 1.6f, b - 2, bv1);
                }
                for (int k = 0; k <= n; k++) {
                    float a = u0 + k * bw;
                    f.rect(0x40000000, a + 1, bv0 + 1, a + 3.5f, bv1 + 1);
                    f.rect(0xFFC8C2B6, a - 1.2f, bv0, a + 1.2f, bv1);
                }
                f.rect(0xFFC8C2B6, u0 - 1.2f, bv1 - 1.5f, u0 + n * bw + 1.2f, bv1 + 1);
                // Skips in a row beside the shed.
                int[] sk = {0xFFE0B020, 0xFF3A7A4A, 0xFFE0B020, 0xFF2E5A8A};
                int[] ld = {0xFF8A7A6A, 0xFF6A6A6A, 0xFFA87A4A, 0xFF5A8AC0};
                int j = 0;
                for (float v = 14; v + 8 < sv; v += 16, j++) {
                    float u = se + 16;
                    if (u + 12 < (q[1][0]) * T) skip(f, r, u, v, 0, sk[j % 4], ld[j % 4]);
                    if (u + 34 < (q[1][0]) * T - 4) skip(f, r, u + 24, v, 0, sk[(j + 1) % 4], ld[(j + 2) % 4]);
                }
                // The conveyor out of the shed to a stack of bales.
                float cu = se - 2, cv = sv + 4;
                float bu = Math.min(L - 40, se + 30);
                if (q[0][2] * T > 50) {
                    float ba = q[0][0] * T + 6;
                    for (int k = 0; k < 5; k++) {
                        float a = ba + k * 9;
                        if (a + 8 > se - 20) break;
                        int bc = k % 3 == 0 ? 0xFFA8845A : k % 3 == 1 ? 0xFF4A78B0 : 0xFFB0B4B8;
                        f.rect(0x40000000, a + 1, cv - 2 + 1.5f, a + 8 + 1, cv + 5 + 1.5f);
                        f.rect(bc, a, cv - 2, a + 8, cv + 5);
                        f.line(0x50000000, 0.5f, a, cv + 0.5f, a + 8, cv + 0.5f);
                        f.line(0x50000000, 0.5f, a, cv + 2.5f, a + 8, cv + 2.5f);
                    }
                }
                f.line(0x40000000, 4, se - 18 + 1, cv + 1.5f, se - 4 + 1, cv + 1.5f);
                f.rect(0xFF3A3C3E, se - 18, cv - 1.8f, se - 4, cv + 1.8f);
                for (float u = se - 17; u < se - 4; u += 2) f.line(0xFF5A5C5E, 0.4f, u, cv - 1.8f, u, cv + 1.8f);
                lorry(f, bu + 20, sv + 4, 0, 26, 0xFF2E7A4A, 0xFF6A8A5A, false);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                if (part == 1) {
                    flatRoof(f.c, f.p, f.x(0, 0), f.y(0, 0), f.x(L, S), f.y(L, S), 0xFF3E7A4A);
                    f.sign(city, "WEIGH", L / 2, S / 2, 4, 0xFFF0F0E0);
                    return;
                }
                // The sorting shed: green cladding, a ridge vent, the recycling arrows painted on the roof.
                f.rect(0xFF2E5A3A, 0, 0, L, S);
                ribbedAcross(f, 0xFF4A8A5A, 0.8f, 0.8f, L - 0.8f, S / 2, 2.4f);
                ribbedAcross(f, 0xFF3A7048, 0.8f, S / 2, L - 0.8f, S - 0.8f, 2.4f);
                f.rect(0xFF2A3A30, 4, S / 2 - 1.5f, L - 4, S / 2 + 1.5f);
                for (float u = 5; u < L - 4; u += 3) f.line(0xFF5A6A60, 0.5f, u, S / 2 - 1.5f, u, S / 2 + 1.5f);
                float cu = L * 0.3f, cv = S * 0.72f, rr = Math.min(S * 0.18f, 9);
                for (int k = 0; k < 3; k++) {
                    double a = k * 2 * Math.PI / 3 - Math.PI / 2;
                    float pu = cu + (float) Math.cos(a) * rr, pv = cv + (float) Math.sin(a) * rr;
                    double b = a + Math.PI / 2 + 0.5;
                    f.line(0xFFE8F0E0, 1.4f, pu, pv, pu + (float) Math.cos(b) * rr * 1.1f, pv + (float) Math.sin(b) * rr * 1.1f);
                    f.circle(0xFFE8F0E0, pu + (float) Math.cos(b) * rr * 1.1f, pv + (float) Math.sin(b) * rr * 1.1f, 1.4f);
                }
                for (float u = L * 0.55f; u < L - 6; u += 12) f.rect(0xFFB8D0C0, u, S * 0.15f, u + 5, S * 0.32f);
                f.sign(city, "RECYCLING", L / 2, S * 0.3f, 6, 0xFFF0F0E0);
            }
        };
        d.ground = City.LOT;
        d.districts = Variants.IND | Variants.SUB;
        d.minS = 10;
        d.minL = 14;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_FACTORY;
        d.roofCol = 0xFF4A8A5A;
        out.add(d);
    }

    /** A round clarifier: a concrete rim, the weir channel, settling water and a bridge arm turning about the middle. */
    static void clarifier(F f, float u, float v, float rad, float arm) {
        f.circle(0x40000000, u + 2, v + 3, rad + 2);
        f.circle(0xFFB8B4AA, u, v, rad + 2);
        f.circle(0xFF8E8A80, u, v, rad);
        f.circle(0xFF5E7A66, u, v, rad - 1.5f);
        f.ring(0xFF7A9A84, 2.4f, u, v, rad - 4);
        f.ring(0xFFC8C4BA, 0.6f, u, v, rad - 5.6f);
        f.circle(0xFF4E6A58, u, v, rad * 0.62f);
        f.ring(0x30FFFFFF, 0.5f, u, v, rad * 0.45f);
        f.ring(0x30FFFFFF, 0.5f, u, v, rad * 0.78f);
        f.circle(0xFF6A8A74, u, v, rad * 0.2f);
        f.ring(0xFFB8B4AA, 1.2f, u, v, rad * 0.2f);
        // The bridge: a walkway with rails, its wake on the water and the drive at the rim.
        f.turn(u, v, arm);
        Canvas c = f.c;
        Paint p = f.p;
        float[] o = off(f.ang(arm), 2, 3);
        rect(c, p, 0x40000000, o[0], -2 + o[1], rad + o[0], 2 + o[1]);
        rect(c, p, 0xFF5A7A68, rad * 0.25f, 2, rad - 2, 5);
        rect(c, p, 0xFFA8ACB0, 0, -2, rad + 1, 2);
        line(c, p, 0xFFD8B030, 0.6f, 0, -2, rad + 1, -2);
        line(c, p, 0xFFD8B030, 0.6f, 0, 2, rad + 1, 2);
        for (float t = 3; t < rad; t += 3) line(c, p, 0xFF8A8E92, 0.4f, t, -2, t, 2);
        rect(c, p, 0xFF3E6A8A, rad - 1, -3, rad + 3, 3);
        c.restore();
        f.circle(0xFFD0D2D4, u, v, 3);
        f.circle(0xFF8A8E92, u, v, 1.6f);
    }

    // ------------------------------------------------------------------ 8. Water Treatment Works

    static void waterWorks(List<Landmarks.Def> out) {
        // Round clarifiers with turning bridges, a bank of filter beds and an aeration lane, on mown grass.
        D d = new D("Water Treatment Works", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                return new int[][]{B(1, 1, 4, 3, 2)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                f.rect(0xFF74A058, 0, 0, L, S);
                for (float v = 0; v < S; v += 12) f.rect(0x0C000000, 0, v, L, v + 6);
                float bu = (q[0][0] + q[0][2]) * T + 8, bv = (q[0][1] + q[0][3]) * T;
                float cv1 = S * 0.56f;
                // Service roads.
                f.rect(0xFFB0AA9C, 0, cv1, L, cv1 + 8);
                f.rect(0xFFB0AA9C, bu - 10, 0, bu - 4, cv1);
                // Clarifiers along the top.
                float avail = L - bu - 4;
                float rad = Math.min(cv1 / 2 - 5, 70);
                int n = Math.max(1, (int) (avail / (rad * 2 + 8)));
                rad = Math.min(rad, avail / n / 2 - 5);
                for (int k = 0; k < n; k++) {
                    float u = bu + avail * (k + 0.5f) / n;
                    clarifier(f, u, cv1 / 2, rad, 30 + k * 110 + r.nextInt(40));
                }
                // Pipes from the clarifiers into the beds.
                for (int k = 0; k < n; k++) {
                    float u = bu + avail * (k + 0.5f) / n;
                    f.line(0xFF5A7A9A, 2, u, cv1 / 2 + rad + 2, u, cv1 + 12);
                }
                // An aeration lane by the building: a long tank of bubbling water.
                float av0 = bv + 6, av1 = cv1 - 6;
                if (av1 - av0 > 16) {
                    f.rect(0x40000000, 6, av0 + 2, bu - 12, av1 + 2);
                    f.rect(0xFFB8B4AA, 4, av0, bu - 14, av1);
                    f.rect(0xFF6A8E7C, 6, av0 + 2, bu - 16, av1 - 2);
                    for (int k = 0; k < 40; k++)
                        f.circle(0x70FFFFFF, 7 + r.nextFloat() * (bu - 24), av0 + 3 + r.nextFloat() * (av1 - av0 - 6), 0.6f + r.nextFloat() * 0.8f);
                }
                // Filter beds below: a grid of tanks, sand in some, water on others, walkways between.
                float fv0 = cv1 + 14, fv1 = S - 6;
                int rows = Math.max(1, (int) ((fv1 - fv0) / 34));
                int cols = Math.max(2, (int) ((L - 12) / 34));
                float cw = (L - 12) / cols, rh = (fv1 - fv0) / rows;
                f.rect(0x40000000, 6 + 2, fv0 + 2, L - 6 + 2, fv1 + 2);
                f.rect(0xFFC4C0B6, 6, fv0, L - 6, fv1);
                for (int i = 0; i < cols; i++)
                    for (int j = 0; j < rows; j++) {
                        float a = 6 + i * cw + 2, b = 6 + (i + 1) * cw - 2, c0 = fv0 + j * rh + 2, c1 = fv0 + (j + 1) * rh - 2;
                        int s = (i * 3 + j * 5 + r.nextInt(2)) % 4;
                        if (s == 0) {
                            f.rect(0xFFC8B48A, a, c0, b, c1);
                            for (float v = c0 + 3; v < c1; v += 3) f.line(0x22000000, 0.5f, a, v, b, v);
                        } else {
                            f.rect(0xFF3E6E8E, a, c0, b, c1);
                            f.rect(0xFF4E80A0, a, c0, b, c0 + 2);
                            f.line(0x50FFFFFF, 0.5f, a + 3, (c0 + c1) / 2, b - 3, (c0 + c1) / 2);
                        }
                        f.rect(0xFF8A8E92, (a + b) / 2 - 1, c0 - 2, (a + b) / 2 + 1, c1 + 2);
                    }
                for (float u = 10; u < L; u += 40) f.tree(u, S - 2, 3.5f, 0xFF4E8A44);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                // The control building: pale brick parapet, a gravel roof with plant and a pipe gallery skylight.
                float L = f.L, S = f.S;
                flatRoof(f.c, f.p, f.x(0, 0), f.y(0, 0), f.x(L, S), f.y(L, S), 0xFFB8B0A0);
                f.specks(r, 30, 0x20000000, 0x20FFFFFF, 2, 2, L - 2, S - 2, 0.9f);
                f.rect(0xFF8EB4C8, 5, S * 0.4f, L - 5, S * 0.6f);
                for (float u = 7; u < L - 5; u += 3) f.line(0xFF6A8EA0, 0.5f, u, S * 0.4f, u, S * 0.6f);
                fanUnit(f, L * 0.2f, S * 0.22f, 3);
                fanUnit(f, L * 0.8f, S * 0.78f, 3);
                f.sign(city, "WATER WORKS", L / 2, S / 2, 5, 0xFF2E5A7A);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.IND | Variants.SUB | Variants.PARK;
        d.minS = 10;
        d.minL = 14;
        d.base = City.OFFICE;
        d.inside = Variants.I_LAB;
        d.roofCol = 0xFFB8B0A0;
        out.add(d);
    }

    /** A decked pile of logs lying along v. */
    static void logPile(F f, Random r, float u0, float v0, float u1, float v1) {
        f.rect(0x40000000, u0 + 2, v0 + 2, u1 + 2, v1 + 2);
        float u = u0;
        while (u < u1 - 2) {
            float d = 3.2f + r.nextFloat() * 1.6f;
            if (u + d > u1) break;
            float s = r.nextFloat() * 3;
            int col = Landmarks.shade(0xFF7A5A3A, 0.85f + r.nextFloat() * 0.3f);
            f.roundRect(City.darken(col, 0.7f), u, v0 + s, u + d, v1 - 3 + s, d / 2);
            f.roundRect(col, u + 0.3f, v0 + s + 0.3f, u + d - 0.3f, v1 - 3 + s - 0.3f, d / 2);
            f.line(City.lighten(col, 0.2f), 0.5f, u + d * 0.3f, v0 + s + 2, u + d * 0.3f, v1 - 5 + s);
            f.circle(0xFFD8B888, u + d / 2, v1 - 3 + s - d / 2, d / 2 - 0.4f);
            f.ring(0xFFB89868, 0.3f, u + d / 2, v1 - 3 + s - d / 2, d / 4);
            u += d + 0.2f;
        }
    }

    /** A stack of sawn timber with its stickers across. */
    static void timberStack(F f, float u0, float v0, float u1, float v1, int col) {
        f.rect(0x44000000, u0 + 1.5f, v0 + 2, u1 + 1.5f, v1 + 2);
        f.rect(City.darken(col, 0.8f), u0, v0, u1, v1);
        for (float u = u0 + 0.4f; u < u1 - 0.4f; u += 2.2f) f.rect(col, u, v0 + 0.4f, Math.min(u1 - 0.4f, u + 1.8f), v1 - 0.4f);
        for (float v = v0 + 3; v < v1 - 1; v += (v1 - v0 - 4) / 3) f.rect(0xFF8A6A4A, u0, v, u1, v + 0.9f);
    }

    // ------------------------------------------------------------------ 9. Sawmill

    static void sawmill(List<Landmarks.Def> out) {
        // A rusty mill shed with the saw line open at one end, decks of logs, timber stacked to dry, a sawdust heap.
        D d = new D("Sawmill", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                int lu = Math.max(5, L / 2), lv = 3;
                return new int[][]{B(1, 1, lu, lv, 2), B(L - 4, 1, 3, 2, 1)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float se = (q[0][0] + q[0][2]) * T, sv = (q[0][1] + q[0][3]) * T;
                f.rect(0xFF9A8466, 0, 0, L, S);
                f.specks(r, 90, 0x22000000, 0x1CFFFFFF, 0, 0, L, S, 1.6f);
                for (int k = 0; k < 10; k++) f.circle(0x14000000, r.nextFloat() * L, r.nextFloat() * S, 3 + r.nextFloat() * 5);
                // Track ruts from the gate.
                f.line(0x26000000, 2, 0, sv + 14, L, sv + 14);
                f.line(0x26000000, 2, 0, sv + 22, L, sv + 22);
                // Decks of logs across the far side.
                float lv0 = sv + 30, lv1 = S - 6;
                float ou = 6;
                int k = 0;
                while (ou < L * 0.55f && lv1 - lv0 > 20) {
                    float w = 26 + r.nextInt(10);
                    if (ou + w > L * 0.58f) break;
                    logPile(f, r, ou, lv0, ou + w, lv1);
                    ou += w + 8;
                    k++;
                }
                // Timber stacked to dry in rows on the other half.
                for (float u = Math.max(ou + 6, L * 0.6f); u + 14 < L - 4; u += 20) {
                    for (float v = lv0 - 6; v + 18 < S - 4; v += 26) timberStack(f, u, v, u + 14, v + 20, 0xFFE0C898);
                }
                // The sawdust heap off the end of the shed, the blower pipe to it.
                float hu = se + 18, hv = sv / 2 + 4;
                if (hu + 16 < (q[1][0]) * T - 4) {
                    f.oval(0x38000000, hu - 14 + 2, hv - 12 + 3, hu + 14 + 2, hv + 12 + 3);
                    f.oval(0xFFB89A68, hu - 14, hv - 12, hu + 14, hv + 12);
                    f.oval(0xFFCCAE7C, hu - 10, hv - 9, hu + 9, hv + 8);
                    f.oval(0xFFDCC090, hu - 6, hv - 6, hu + 4, hv + 3);
                    f.line(0xFF8A8E92, 2, se - 2, hv - 4, hu - 2, hv - 4);
                }
                // A log loader with its grapple.
                float lu = Math.min(ou + 10, L * 0.5f), lvv = sv + 18;
                f.turn(lu, lvv, 0);
                Canvas c = f.c;
                Paint p = f.p;
                float[] o = off(f.ang(0), 2, 2.5f);
                rect(c, p, 0x48000000, -8 + o[0], -5 + o[1], 8 + o[0], 5 + o[1]);
                rect(c, p, 0xFF2A2A2A, -7, -5.5f, -3, 5.5f);
                rect(c, p, 0xFF2A2A2A, 3, -5.5f, 7, 5.5f);
                rect(c, p, 0xFFD8A020, -8, -4, 8, 4);
                rect(c, p, 0xFF2E3A46, 1, -3, 5, 3);
                line(c, p, 0xFF4A4A4A, 1.4f, -8, 0, -16, 0);
                rect(c, p, 0xFF3A3A3A, -19, -4, -15, 4);
                c.restore();
                f.lamp(se + 4, sv + 6);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                if (part == 1) {
                    // The drying kiln: insulated panels and a stubby stack.
                    flatRoof(f.c, f.p, f.x(0, 0), f.y(0, 0), f.x(L, S), f.y(L, S), 0xFFA8AAA6);
                    for (float u = 4; u < L - 2; u += 4) f.line(0x26000000, 0.5f, u, 1.5f, u, S - 1.5f);
                    f.circle(0x40000000, L * 0.75f + 1.5f, S / 2 + 2, 3);
                    f.circle(0xFF5A5450, L * 0.75f, S / 2, 3);
                    f.circle(0xFF222222, L * 0.75f, S / 2, 1.8f);
                    return;
                }
                // The mill: a rusty corrugated gable along the saw line, open at the near end over the head saw.
                float open = Math.min(S * 0.9f, L * 0.22f);
                f.rect(0xFF4A3426, 0, 0, L, S);
                ribbedAcross(f, 0xFF9A5E3A, open, 0.8f, L - 0.8f, S / 2, 2.2f);
                ribbedAcross(f, 0xFF7A4A2E, open, S / 2, L - 0.8f, S - 0.8f, 2.2f);
                for (int k = 0; k < 6; k++) {
                    float u = open + r.nextFloat() * (L - open - 8), v = 2 + r.nextFloat() * (S - 6);
                    f.rect(0x30602010, u, v, u + 5 + r.nextFloat() * 6, v + 2);
                }
                f.line(0xFFB8805A, 1, open, S / 2, L - 0.8f, S / 2);
                for (float u = open + 6; u < L - 4; u += 14) f.rect(0xFFB8C8CC, u, S * 0.2f, u + 4, S * 0.35f);
                // The open bay: the floor, the log on the carriage and the big toothed blade.
                f.rect(0xFF6A5E50, 0.8f, 0.8f, open, S - 0.8f);
                f.roundRect(0xFF7A5A3A, 1.5f, S * 0.3f, open - 1, S * 0.45f, 2);
                float bu = open * 0.55f, bvv = S * 0.62f, br = Math.min(open * 0.35f, S * 0.22f);
                f.circle(0x40000000, bu + 1, bvv + 1, br);
                f.circle(0xFFC8CCD0, bu, bvv, br);
                for (int k = 0; k < 16; k++) {
                    double a = k * Math.PI / 8;
                    f.circle(0xFF8A8E92, bu + (float) Math.cos(a) * br, bvv + (float) Math.sin(a) * br, 0.7f);
                }
                f.circle(0xFF5A5E62, bu, bvv, br * 0.25f);
                f.sign(city, "SAWMILL", (L + open) / 2, S / 2, 6, 0xFFF0E0C0);
            }
        };
        d.ground = City.DIRT;
        d.districts = Variants.IND | Variants.SUB;
        d.minS = 10;
        d.minL = 14;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_FACTORY;
        d.roofCol = 0xFF9A5E3A;
        out.add(d);
    }

    /** A round brick chimney from above, hot inside. */
    static void stack(F f, float u, float v, float rad, int brick, boolean hot) {
        f.circle(0x48000000, u + rad * 0.5f + 2, v + rad * 0.7f + 3, rad);
        f.circle(City.darken(brick, 0.7f), u, v, rad);
        f.circle(brick, u - rad * 0.08f, v - rad * 0.08f, rad * 0.86f);
        f.ring(City.darken(brick, 0.6f), 0.5f, u, v, rad * 0.7f);
        f.circle(0xFF1C1814, u, v, rad * 0.55f);
        if (hot) {
            f.circle(0xFF8A2A10, u, v, rad * 0.4f);
            f.circle(0xFFE07020, u, v, rad * 0.25f);
            f.circle(0xFFFFC040, u, v, rad * 0.12f);
        }
    }

    // ------------------------------------------------------------------ 10. Glassworks

    static void glassworks(List<Landmarks.Def> out) {
        // A furnace hall with tall stacks and glowing vents, an old brick glass cone, cullet heaps and pallets of bottles.
        D d = new D("Glassworks", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                int cs = Math.min(5, Math.max(4, S / 3));
                int lv = Math.max(3, S - cs - 6);
                lv = Math.min(lv, 5);
                return new int[][]{B(1, 1, L - cs - 3, lv, 2), B(L - cs - 1, 1, cs, cs, 3)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float hv = (q[0][1] + q[0][3]) * T, cv = (q[1][1] + q[1][3]) * T;
                f.rect(0xFF8A847C, 0, 0, L, S);
                // Setts in the yard.
                for (float v = 0; v < S; v += 4)
                    for (float u = ((int) (v / 4) % 2) * 3; u < L; u += 6) f.rect(0x14000000, u, v, u + 5.4f, v + 3.4f);
                float yv = Math.max(hv, cv) + 8;
                // Cullet heaps: crushed glass, green, brown and clear, glinting.
                int[] gc = {0xFF5A9A6A, 0xFF9A6A3A, 0xFFB8D0D4};
                float hu = 8;
                for (int k = 0; k < 3 && hu + 28 < L * 0.6f; k++) {
                    float w = 24, v0 = yv + 4, v1 = Math.min(S - 6, v0 + 26);
                    if (v1 - v0 < 14) break;
                    f.rect(0xFF6A6660, hu - 3, v0 - 3, hu + w + 3, v1 + 3);
                    heap(f, r, hu, v0, hu + w, v1, gc[k]);
                    for (int j = 0; j < 8; j++) f.circle(0xC0FFFFFF, hu + 4 + r.nextFloat() * (w - 8), v0 + 4 + r.nextFloat() * (v1 - v0 - 8), 0.5f);
                    hu += w + 10;
                }
                // Sand silo on legs.
                float su = hu + 12, sv = yv + 16;
                if (su + 10 < L * 0.7f && sv + 10 < S) {
                    f.circle(0x48000000, su + 4, sv + 5, 9);
                    f.circle(0xFFB8BCC0, su, sv, 9);
                    f.circle(0xFFD0D4D8, su - 1.5f, sv - 1.5f, 6.5f);
                    f.circle(0xFF8A8E92, su, sv, 2);
                }
                // Pallets of bottles waiting for the lorry, shrink-wrapped.
                float pu0 = Math.max(su + 16, L * 0.6f);
                for (float u = pu0; u + 10 < L - 4 && u < pu0 + 3 * 13; u += 13)
                    for (float v = yv + 2; v + 10 < S - 4 && v < yv + 2 + 3 * 13; v += 13) {
                        f.rect(0x40000000, u + 1.2f, v + 1.5f, u + 11.2f, v + 11.5f);
                        f.rect(0xFFA88A5A, u, v, u + 10, v + 10);
                        for (float a = u + 1.2f; a < u + 9.5f; a += 2.1f)
                            for (float b = v + 1.2f; b < v + 9.5f; b += 2.1f)
                                f.circle(((int) ((a + b) * 3)) % 3 == 0 ? 0xFF3E7A4A : 0xFF5A9A6A, a + 0.8f, b + 0.8f, 0.85f);
                        f.rect(0x30FFFFFF, u, v, u + 10, v + 3);
                    }
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                if (part == 1) {
                    // The glass cone: a brick base, the tapering cone in rings and the open top glowing.
                    float cx = L / 2, cy = S / 2, rad = Math.min(L, S) / 2;
                    f.rect(0xFF6E5A4A, 0, 0, L, S);
                    f.rect(0xFF8A7058, 1, 1, L - 1, S - 1);
                    int[] rc = {0xFF7A3E2A, 0xFF8A4A32, 0xFF9A563A, 0xFFA86244, 0xFF9A563A};
                    for (int k = 0; k < 5; k++) {
                        float rr = rad * (0.96f - k * 0.13f);
                        f.circle(rc[k], cx - k * 0.4f, cy - k * 0.4f, rr);
                        f.ring(0x30000000, 0.5f, cx - k * 0.4f, cy - k * 0.4f, rr);
                    }
                    f.circle(0x50000000, cx + rad * 0.2f, cy + rad * 0.25f, rad * 0.3f);
                    stack(f, cx - 2, cy - 2, rad * 0.3f, 0xFFA86244, true);
                    f.sign(city, "GLASS CONE", cx, cy + rad * 0.6f, 4, 0xFFF0E0C0);
                    return;
                }
                // The furnace hall: dark slate gable with a glowing louvred lantern along the ridge and two stacks.
                f.rect(0xFF2E2E34, 0, 0, L, S);
                f.rect(0xFF5A5E6A, 0.8f, 0.8f, L - 0.8f, S / 2);
                f.rect(0xFF464A54, 0.8f, S / 2, L - 0.8f, S - 0.8f);
                for (float v = 3; v < S - 1; v += 3) f.line(0x1E000000, 0.4f, 0.8f, v, L - 0.8f, v);
                float lw = Math.max(3, S * 0.12f);
                f.rect(0x50000000, 4, S / 2 - lw + 1.5f, L * 0.62f + 1.5f, S / 2 + lw + 1.5f);
                f.rect(0xFF3A3436, 4, S / 2 - lw, L * 0.62f, S / 2 + lw);
                for (float u = 5; u < L * 0.62f - 1; u += 2.4f) {
                    f.rect(0xFFE07A28, u, S / 2 - lw + 1, u + 1.2f, S / 2 + lw - 1);
                    f.rect(0xFFFFC060, u + 0.2f, S / 2 - 0.6f, u + 1, S / 2 + 0.6f);
                }
                float sr = Math.min(S * 0.2f, 8);
                stack(f, L * 0.74f, S * 0.32f, sr, 0xFF9A563A, true);
                stack(f, L * 0.88f, S * 0.66f, sr * 0.85f, 0xFF9A563A, true);
                f.sign(city, "GLASSWORKS", L * 0.33f, S * 0.22f, 6, 0xFFF0E0C0);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.IND | Variants.OLD;
        d.minS = 10;
        d.minL = 14;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_FACTORY;
        d.roofCol = 0xFF5A5E6A;
        out.add(d);
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

    /** A brick chimney stack with pots. */
    static void chimney(F f, float u, float v, int pots) {
        float w = 1.8f + pots * 1.8f;
        f.rect(0x48000000, u + 1, v + 1.4f, u + w + 1, v + 3.4f + 1.4f);
        f.rect(0xFF8A4A36, u, v, u + w, v + 3.4f);
        f.rect(0xFFA05A42, u, v, u + w, v + 1);
        for (int k = 0; k < pots; k++) {
            f.circle(0xFFB86A48, u + 1.8f + k * 1.8f, v + 1.7f, 0.8f);
            f.circle(0xFF2A2020, u + 1.8f + k * 1.8f, v + 1.7f, 0.4f);
        }
    }

    /** A rooftop turbine ventilator. */
    static void turbine(F f, float u, float v, float rad) {
        f.circle(0x40000000, u + 1, v + 1.4f, rad);
        f.circle(0xFF9AA0A6, u, v, rad);
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4;
            f.line(0xFF6A7076, 0.5f, u, v, u + (float) Math.cos(a) * rad, v + (float) Math.sin(a + 0.6) * rad);
        }
        f.circle(0xFFC8CCD0, u - rad * 0.2f, v - rad * 0.2f, rad * 0.35f);
    }

    // ------------------------------------------------------------------ 11. Printing Works

    static void printingWorks(List<Landmarks.Def> out) {
        // A red-brick Victorian front with a clock lantern, the big press hall behind, paper reels and the dispatch dock.
        D d = new D("Printing Works", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                int lvp = S - 6, lo = Math.max(4, L / 2);
                return new int[][]{B(1, 1, L - 6, lvp, 2), B(1, S - 4, lo, 3, 3)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float pe = (q[0][0] + q[0][2]) * T, pv = (q[0][1] + q[0][3]) * T, oe = (q[1][0] + q[1][2]) * T, ov = q[1][1] * T;
                f.rect(0xFF7E7A74, 0, 0, L, S);
                f.specks(r, 50, 0x1A000000, 0x12FFFFFF, 0, 0, L, S, 1.5f);
                // A strip of setts along the front of the old building, railings and a lamp.
                f.rect(0xFFB4AA98, 0, ov - 6, oe + 6, S);
                for (float u = 2; u < oe + 4; u += 3) f.line(0x16000000, 0.5f, u, ov - 6, u, S);
                f.line(0xFF2A2A2C, 0.8f, 2, S - 2, oe + 4, S - 2);
                for (float u = 3; u < oe + 4; u += 2.5f) f.circle(0xFF2A2A2C, u, S - 2, 0.5f);
                // The reel store: giant paper reels standing on end beside the press hall.
                float ru0 = pe + 6, ru1 = L - 4;
                for (float u = ru0 + 6; u + 5 < ru1 && u < ru0 + 6 + 4 * 11; u += 11)
                    for (float v = 8; v + 5 < pv - 4 && v < 8 + 6 * 11; v += 11) {
                        f.circle(0x48000000, u + 1.5f, v + 2, 5);
                        f.circle(0xFFD8D2C2, u, v, 5);
                        f.circle(0xFFEEEAE0, u - 0.6f, v - 0.6f, 4);
                        f.ring(0x20000000, 0.4f, u, v, 2.8f);
                        f.circle(0xFF8A6A4A, u, v, 1.2f);
                    }
                // The dispatch dock: bay doors, vans backed up, bundles of papers on pallets.
                float dv = pv + 2;
                f.rect(0xFFE0C040, oe + 10, dv, L - 4, dv + 1);
                int[] vc = {0xFFE8E4DA, 0xFF2E5A8A};
                int k = 0;
                for (float u = oe + 20; u + 10 < L - 4 && k < 2; u += 22, k++) {
                    car(f, u, dv + 14, -90, vc[k]);
                }
                for (float u = oe + 12; u + 8 < L - 6 && u < oe + 60; u += 10) {
                    float v = S - 14;
                    f.rect(0x40000000, u + 1, v + 1.5f, u + 9, v + 9.5f);
                    f.rect(0xFFA88A5A, u, v, u + 8, v + 8);
                    for (float a = v + 0.5f; a < v + 8; a += 2) f.rect(0xFFE8E4DC, u + 0.6f, a, u + 7.4f, a + 1.6f);
                    f.line(0xFF3A3A3A, 0.4f, u + 4, v, u + 4, v + 8);
                }
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                if (part == 1) {
                    // The Victorian front: a slate hip with a ventilating lantern, chimneys and a clock turret.
                    f.rect(0xFF8A4632, 0, 0, L, S);
                    hip(f, 0xFF5A6070, 1, 1, L - 1, S - 1, true);
                    float lu0 = L * 0.25f, lu1 = L * 0.6f;
                    f.rect(0x50000000, lu0 + 1, S / 2 - 3 + 1.5f, lu1 + 1, S / 2 + 3 + 1.5f);
                    f.rect(0xFF3E4450, lu0, S / 2 - 3, lu1, S / 2 + 3);
                    f.rect(0xFF6A7080, lu0, S / 2 - 3, lu1, S / 2 - 1);
                    for (float u = lu0 + 1.5f; u < lu1 - 1; u += 2) f.line(0xFF2A2E36, 0.5f, u, S / 2 - 1, u, S / 2 + 3);
                    chimney(f, 3, 3, 2);
                    chimney(f, L - 9, S - 7, 3);
                    float cu = L * 0.8f, cv = S / 2;
                    f.rect(0x50000000, cu - 4 + 1.5f, cv - 4 + 2, cu + 4 + 1.5f, cv + 4 + 2);
                    f.rect(0xFFC8B89A, cu - 4, cv - 4, cu + 4, cv + 4);
                    f.poly(0xFF5A8A7A, cu - 3.4f, cv - 3.4f, cu + 3.4f, cv - 3.4f, cu, cv);
                    f.poly(0xFF3E6A5A, cu + 3.4f, cv - 3.4f, cu + 3.4f, cv + 3.4f, cu, cv);
                    f.poly(0xFF2E5A4A, cu - 3.4f, cv + 3.4f, cu + 3.4f, cv + 3.4f, cu, cv);
                    f.poly(0xFF6A9A8A, cu - 3.4f, cv - 3.4f, cu - 3.4f, cv + 3.4f, cu, cv);
                    f.circle(0xFFE0C060, cu, cv, 0.8f);
                    f.sign(city, "PRINTING WORKS", L * 0.42f, S / 2, 6, 0xFFF0E0C0);
                    return;
                }
                // The press hall: a grey membrane roof with rows of turbine vents and a long ridge light.
                flatRoof(f.c, f.p, f.x(0, 0), f.y(0, 0), f.x(L, S), f.y(L, S), 0xFF9EA2A6);
                for (float u = 3; u < L - 2; u += 8) f.line(0x14000000, 0.5f, u, 1.5f, u, S - 1.5f);
                f.rect(0xFF8EB4C8, 4, S / 2 - 2.5f, L - 4, S / 2 + 2.5f);
                for (float u = 6; u < L - 4; u += 3) f.line(0xFF6A8EA0, 0.5f, u, S / 2 - 2.5f, u, S / 2 + 2.5f);
                for (float u = 10; u < L - 6; u += 14) {
                    turbine(f, u, S * 0.22f, 2.6f);
                    turbine(f, u + 7, S * 0.78f, 2.6f);
                }
                fanUnit(f, L - 10, S * 0.22f, 3.5f);
                f.sign(city, "PRESS HALL", L / 2, S * 0.3f, 6, 0xFF2A2A2A);
            }
        };
        d.ground = City.LOT;
        d.districts = Variants.IND | Variants.OLD | Variants.MID;
        d.minS = 10;
        d.minL = 13;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_FACTORY;
        d.roofCol = 0xFF9EA2A6;
        out.add(d);
    }

    /** A big seven-segment digit painted on a roof. */
    static void digit(F f, int n, float u, float v, float h, int col) {
        int[] seg = {0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07, 0x7F, 0x6F};
        int m = seg[n % 10];
        float w = h * 0.55f, t = h * 0.14f, hh = h / 2;
        if ((m & 1) != 0) f.rect(col, u, v, u + w, v + t);
        if ((m & 2) != 0) f.rect(col, u + w - t, v, u + w, v + hh);
        if ((m & 4) != 0) f.rect(col, u + w - t, v + hh, u + w, v + h);
        if ((m & 8) != 0) f.rect(col, u, v + h - t, u + w, v + h);
        if ((m & 16) != 0) f.rect(col, u, v + hh, u + t, v + h);
        if ((m & 32) != 0) f.rect(col, u, v, u + t, v + hh);
        if ((m & 64) != 0) f.rect(col, u, v + hh - t / 2, u + w, v + hh + t / 2);
    }

    // ------------------------------------------------------------------ 12. Film Studios

    static void filmStudios(List<Landmarks.Def> out) {
        // Two numbered sound stages, a backlot street of false-front facades propped from behind, a water tank, trailers.
        D d = new D("Film Studios", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                int lv = Math.max(3, S / 2 - 2), a = (L - 3) / 2;
                return new int[][]{B(1, 1, a, lv, 4), B(a + 2, 1, L - a - 3, lv, 4)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float sv = (q[0][1] + q[0][3]) * T;
                f.rect(0xFF8A8680, 0, 0, L, S);
                f.specks(r, 40, 0x18000000, 0x12FFFFFF, 0, 0, L, S, 1.5f);
                // Trailers and lighting carts along the stage fronts.
                for (float u = 8; u + 20 < L * 0.5f; u += 26) {
                    f.roundRect(0x40000000, u + 1.5f, sv + 5, u + 21.5f, sv + 14, 2);
                    f.roundRect(0xFFE8E6E0, u, sv + 3, u + 20, sv + 12, 2);
                    f.rect(0xFFB8B4AC, u + 3, sv + 6, u + 8, sv + 9);
                    f.circle(0xFF9AA0A6, u + 15, sv + 7.5f, 1.6f);
                }
                // The backlot: a street of facades facing each other, a period car and lamps.
                float b0 = sv + 18, b1 = S - 3, st0 = b0 + (b1 - b0) * 0.36f, st1 = b0 + (b1 - b0) * 0.64f;
                if (b1 - b0 > 40) {
                    float u0 = L * 0.06f, u1 = L * 0.94f;
                    f.rect(0xFFB8A888, u0, st0, u1, st1);
                    f.rect(0xFFC8BC9C, u0, st0, u1, st0 + 2.5f);
                    f.rect(0xFFC8BC9C, u0, st1 - 2.5f, u1, st1);
                    int[] fc = {0xFFC8A070, 0xFF8A5A4A, 0xFFE0D8C0, 0xFF6A8A9A, 0xFFB86A50, 0xFFD8C090, 0xFF7A6A8A};
                    for (int s = 0; s < 2; s++) {
                        float u = u0;
                        int k = s * 3;
                        while (u < u1 - 10) {
                            float w = Math.min(u1 - u, 14 + r.nextInt(10));
                            int col = fc[k % fc.length];
                            float fv = s == 0 ? st0 - 2 : st1, bv = s == 0 ? st0 - 12 : st1 + 12;
                            // The facade board, its cornice and the braces propping it from behind.
                            f.rect(0x40000000, u + 1, fv + 1, u + w, fv + 3);
                            f.rect(col, u + 0.3f, fv, u + w - 0.3f, fv + 2);
                            f.rect(City.lighten(col, 0.25f), u + 0.3f, s == 0 ? fv + 1.4f : fv, u + w - 0.3f, s == 0 ? fv + 2 : fv + 0.6f);
                            for (float t = u + 3; t < u + w - 1; t += 5) f.line(0xFF8A7050, 0.6f, t, fv + (s == 0 ? 0 : 2), t + 2, bv);
                            u += w + 0.6f;
                            k++;
                        }
                    }
                    car(f, L * 0.4f, (st0 + st1) / 2, 0, 0xFF2A2A2A);
                    f.oval(0x50000000, L * 0.64f, st0 + 3, L * 0.7f, st0 + 7);
                    for (float u = u0 + 10; u < u1; u += 34) {
                        f.lamp(u, st0 + 3.5f);
                        f.lamp(u + 17, st1 - 3.5f);
                    }
                }
                // The studio water tank on its legs.
                float wu = L - 18, wv = sv + 10;
                f.circle(0x40000000, wu + 8, wv + 10, 8);
                for (int k = 0; k < 4; k++) {
                    double a = Math.PI / 4 + k * Math.PI / 2;
                    f.line(0xFF5A5450, 1, wu, wv, wu + (float) Math.cos(a) * 9, wv + (float) Math.sin(a) * 9);
                }
                f.circle(0xFF6A5A48, wu, wv, 8);
                f.circle(0xFF8A7458, wu - 0.8f, wv - 0.8f, 6.6f);
                f.poly(0xFF9A8464, wu - 6, wv - 2, wu, wv - 6, wu, wv);
                f.circle(0xFF5A4A38, wu, wv, 1.2f);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                // A sound stage: a vast windowless box, its roof crossed by catwalks, AC plant and its number.
                float L = f.L, S = f.S;
                flatRoof(f.c, f.p, f.x(0, 0), f.y(0, 0), f.x(L, S), f.y(L, S), 0xFFC8C4BA);
                for (float u = 6; u < L - 2; u += 6) f.line(0x16000000, 0.5f, u, 1.5f, u, S - 1.5f);
                f.rect(0xFF8A8E92, 1.5f, S * 0.5f - 0.8f, L - 1.5f, S * 0.5f + 0.8f);
                float h = Math.min(S * 0.55f, L * 0.4f);
                float du = L * 0.18f + h * 0.27f, dv = S / 2;
                F w = new F(f.c, f.p, f.x(du, dv), f.y(du, dv), f.x(du, dv) + 1, f.y(du, dv));
                digit(w, part + 1, -h * 0.27f, -h / 2, h, 0xFFE0C040);
                for (float u = L * 0.55f; u < L - 6; u += 10) {
                    fanUnit(f, u, S * 0.25f, 3);
                    fanUnit(f, u + 5, S * 0.75f, 3);
                }
                f.sign(city, "STAGE " + (part + 1), L / 2, S / 2, 6, 0xFF2A2A2A);
            }
        };
        d.ground = City.LOT;
        d.districts = Variants.IND | Variants.MID | Variants.SUB;
        d.minS = 12;
        d.minL = 14;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_SHOWROOM;
        d.roofCol = 0xFFC8C4BA;
        out.add(d);
    }

    // ------------------------------------------------------------------ 13. Radio Telescope

    static void radioTelescope(List<Landmarks.Def> out) {
        // A great steerable dish on its azimuth rail, the feed on a tripod over it, a row of small dishes and the control hut.
        D d = new D("Radio Telescope", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                return new int[][]{B(1, S - 3, 3, 2, 2)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                f.rect(0xFF7AA05C, 0, 0, L, S);
                for (int k = 0; k < 30; k++) f.circle(0x12000000, r.nextFloat() * L, r.nextFloat() * S, 3 + r.nextFloat() * 4);
                float rad = Math.min(S, L) * 0.4f, cu = Math.min(L - rad - 8, Math.max(rad + 8, L * 0.6f)), cv = S / 2;
                if (L > 1.6f * S) cu = L - rad - 10;
                // The concrete apron and the circular rail the mount runs round.
                f.circle(0xFFB4B0A6, cu, cv, rad * 0.75f);
                f.ring(0xFF8A8680, 2.4f, cu, cv, rad * 0.6f);
                f.ring(0xFFC8CCD0, 0.6f, cu, cv, rad * 0.6f - 0.8f);
                f.ring(0xFFC8CCD0, 0.6f, cu, cv, rad * 0.6f + 0.8f);
                // The path to the control hut.
                float hu = (q[0][0] + q[0][2] / 2f) * T, hv = q[0][1] * T;
                f.curve(0xFFC8C0AE, 5, hu, hv, hu, cv, cu - rad * 0.7f, cv);
                // The dish's shadow, then the dish: a pale bowl lit on its far side, panel rings and ribs.
                f.circle(0x40000000, cu + rad * 0.3f, cv + rad * 0.38f, rad);
                f.circle(0xFF9AA0A6, cu, cv, rad);
                f.circle(0xFFC4C8CC, cu, cv, rad - 1.5f);
                f.circle(0xFFD4D8DC, cu + rad * 0.08f, cv + rad * 0.08f, rad * 0.8f);
                f.circle(0xFFE2E6E8, cu + rad * 0.16f, cv + rad * 0.16f, rad * 0.5f);
                f.circle(0xFFB0B6BC, cu - rad * 0.06f, cv - rad * 0.06f, rad * 0.3f);
                for (int k = 1; k < 5; k++) f.ring(0x26000000, 0.5f, cu, cv, rad * k / 5f);
                for (int k = 0; k < 24; k++) {
                    double a = k * Math.PI / 12;
                    f.line(0x22000000, 0.5f, cu + (float) Math.cos(a) * rad * 0.2f, cv + (float) Math.sin(a) * rad * 0.2f,
                            cu + (float) Math.cos(a) * (rad - 1.5f), cv + (float) Math.sin(a) * (rad - 1.5f));
                }
                f.ring(0xFF7A8086, 1.2f, cu, cv, rad - 0.6f);
                // The feed: three legs from the rim to the receiver cabin, with their shadows on the bowl.
                for (int k = 0; k < 3; k++) {
                    double a = -Math.PI / 2 + k * 2 * Math.PI / 3;
                    float eu = cu + (float) Math.cos(a) * (rad - 1), ev = cv + (float) Math.sin(a) * (rad - 1);
                    f.line(0x30000000, 1.2f, eu, ev, cu + rad * 0.12f, cv + rad * 0.15f);
                }
                for (int k = 0; k < 3; k++) {
                    double a = -Math.PI / 2 + k * 2 * Math.PI / 3;
                    float eu = cu + (float) Math.cos(a) * (rad - 1), ev = cv + (float) Math.sin(a) * (rad - 1);
                    f.line(0xFF5A6066, 1.4f, eu, ev, cu, cv);
                    f.line(0xFF8A9096, 0.5f, eu, ev, cu, cv);
                }
                f.rect(0x50000000, cu - 4 + 3, cv - 4 + 4, cu + 4 + 3, cv + 4 + 4);
                f.rect(0xFF6A7076, cu - 4, cv - 4, cu + 4, cv + 4);
                f.rect(0xFFE8E6E0, cu - 3.2f, cv - 3.2f, cu + 3.2f, cv + 3.2f);
                f.circle(0xFFB0B4B8, cu, cv, 1.6f);
                // A row of small dishes on the other side.
                float du = 14;
                for (float v = 14; v + 10 < (q[0][1] - 1) * T; v += 24) {
                    if (du + 10 > cu - rad - 4) break;
                    f.circle(0x40000000, du + 3, v + 4, 9);
                    f.circle(0xFF9AA0A6, du, v, 9);
                    f.circle(0xFFD8DCE0, du + 0.8f, v + 0.8f, 7.5f);
                    f.circle(0xFFE8ECEE, du + 2, v + 2, 4);
                    f.line(0xFF5A6066, 0.8f, du - 6, v - 6, du, v);
                    f.circle(0xFF5A6066, du, v, 1.2f);
                }
                // The perimeter fence.
                f.frame(0xFF6A7076, 0.6f, 2, 2, L - 2, S - 2);
                for (float u = 2; u < L; u += 10) {
                    f.circle(0xFF6A7076, u, 2, 0.6f);
                    f.circle(0xFF6A7076, u, S - 2, 0.6f);
                }
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                // The control building: a white roof with antennas and a little weather mast.
                float L = f.L, S = f.S;
                flatRoof(f.c, f.p, f.x(0, 0), f.y(0, 0), f.x(L, S), f.y(L, S), 0xFFE0DED8);
                f.circle(0x40000000, L * 0.3f + 1.5f, S / 2 + 2, 4);
                f.circle(0xFFB8BCC0, L * 0.3f, S / 2, 4);
                f.circle(0xFFE8ECEE, L * 0.3f + 0.6f, S / 2 + 0.6f, 3);
                f.line(0xFF4A4E52, 0.6f, L * 0.7f, S * 0.3f, L * 0.85f, S * 0.3f);
                f.line(0xFF4A4E52, 0.6f, L * 0.7f, S * 0.7f, L * 0.85f, S * 0.7f);
                f.circle(0xFFD04040, L * 0.78f, S / 2, 1);
                f.sign(city, "OBSERVATORY", L / 2, S / 2, 4, 0xFF2A2A2A);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.PARK | Variants.UNI | Variants.SUB;
        d.minS = 10;
        d.minL = 12;
        d.base = City.OFFICE;
        d.inside = Variants.I_LAB;
        d.roofCol = 0xFFE0DED8;
        out.add(d);
    }

    /** A solar array: dark blue panels in a grid. */
    static void solar(F f, float u0, float v0, float u1, float v1) {
        f.rect(0xFF8A9096, u0, v0, u1, v1);
        for (float u = u0 + 0.6f; u + 4 < u1; u += 4.6f)
            for (float v = v0 + 0.6f; v + 2.6f < v1; v += 3.2f) {
                f.rect(0xFF24384E, u, v, u + 4, v + 2.6f);
                f.rect(0xFF3A5672, u, v, u + 4, v + 0.7f);
            }
    }

    // ------------------------------------------------------------------ 14. Research Campus

    static void researchCampus(List<Landmarks.Def> out) {
        // Three crisp lab buildings tied by glazed walkways, set in lawns with curving paths, a pond and young trees.
        D d = new D("Research Campus", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                int half = S / 2, a = L / 2 - 2, lv = Math.max(3, half - 2);
                int cw = Math.max(4, L / 3), ch = Math.max(3, S - half - 2);
                return new int[][]{B(1, 1, a, lv, 4), B(a + 2, 1, L - a - 3, lv, 3), B(L - cw - 1, lv + 2, cw, Math.min(ch, S - lv - 3), 3)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                f.rect(0xFF78A85A, 0, 0, L, S);
                for (float u = 0; u < L; u += 14) f.rect(0x0C000000, u, 0, u + 7, S);
                float av1 = (q[0][1] + q[0][3]) * T, cu0 = q[2][0] * T, cv0 = q[2][1] * T, cv1 = cv0 + q[2][3] * T;
                // Paths: a plaza strip along the labs, a curving path across the lawn.
                f.rect(0xFFDCD6C8, 0, av1, L, av1 + 8);
                f.curve(0xFFDCD6C8, 6, 0, S - 10, cu0 * 0.5f, S - 10, cu0 * 0.6f, av1 + 6);
                f.curve(0xFFDCD6C8, 5, cu0 * 0.3f, av1 + 6, cu0 * 0.4f, S - 4, cu0 - 4, (cv0 + cv1) / 2);
                // A pond with a boardwalk on the lawn.
                float pu0 = cu0 * 0.12f, pu1 = cu0 * 0.5f, pv0 = av1 + 16, pv1 = Math.min(S - 18, pv0 + 40);
                if (pu1 - pu0 > 24 && pv1 - pv0 > 16) {
                    pond(f.c, f.p, f.x(pu0, pv0), f.y(pu0, pv0), f.x(pu1, pv1), f.y(pu1, pv1));
                    f.rect(0xFF9A7A52, (pu0 + pu1) / 2 - 2, pv0 - 2, (pu0 + pu1) / 2 + 2, (pv0 + pv1) / 2);
                }
                // The glazed walkways between the buildings.
                float gu0 = (q[0][0] + q[0][2]) * T, gu1 = q[1][0] * T, gv = (q[0][1] + q[0][3] / 2f) * T;
                f.rect(0x40000000, gu0, gv - 4 + 3, gu1 + 2, gv + 4 + 3);
                f.rect(0xFF8EB6CC, gu0, gv - 4, gu1, gv + 4);
                f.frame(0xFFE8ECEE, 0.8f, gu0, gv - 4, gu1, gv + 4);
                for (float u = gu0 + 2; u < gu1; u += 3) f.line(0xFFD0DCE4, 0.4f, u, gv - 4, u, gv + 4);
                float wu = (q[2][0] + q[2][2] / 2f) * T, wv0 = (q[1][1] + q[1][3]) * T;
                f.rect(0x40000000, wu - 4 + 2, wv0, wu + 4 + 2, cv0 + 3);
                f.rect(0xFF8EB6CC, wu - 4, wv0, wu + 4, cv0);
                f.frame(0xFFE8ECEE, 0.8f, wu - 4, wv0, wu + 4, cv0);
                // Young trees in the lawns, benches by the paths.
                for (int k = 0; k < 7; k++) {
                    float u = 6 + r.nextFloat() * (cu0 - 14), v = av1 + 14 + r.nextFloat() * (S - av1 - 20);
                    if (u > pu0 - 6 && u < pu1 + 6 && v > pv0 - 6 && v < pv1 + 6) continue;
                    f.tree(u, v, 4 + r.nextFloat() * 2, k % 3 == 0 ? 0xFF8AA048 : 0xFF4E8A44);
                }
                for (float u = 12; u < L - 10; u += 40) f.bench(u, av1 + 4, true);
                for (float u = 6; u < L; u += 30) f.lamp(u, av1 + 9);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                // A white membrane roof with a glass atrium, solar arrays and a green-roof patch.
                f.rect(0xFF9EA4AA, 0, 0, L, S);
                f.rect(0xFFF0F0EC, 1, 1, L - 1, S - 1);
                for (float u = 5; u < L - 1; u += 5) f.line(0x10000000, 0.5f, u, 1, u, S - 1);
                float au = L * (part == 1 ? 0.62f : 0.32f);
                f.rect(0xFF7AA8C2, au - S * 0.18f, S * 0.22f, au + S * 0.18f, S * 0.78f);
                for (float v = S * 0.22f + 3; v < S * 0.78f; v += 3) f.line(0xFFB8D4E2, 0.5f, au - S * 0.18f, v, au + S * 0.18f, v);
                f.line(0xC0FFFFFF, 0.6f, au - S * 0.18f, S * 0.22f, au + S * 0.18f, S * 0.22f);
                float s0 = part == 1 ? 3 : au + S * 0.25f, s1 = part == 1 ? au - S * 0.25f : L - 3;
                if (s1 - s0 > 10) solar(f, s0, 3, s1, S * 0.45f);
                if (s1 - s0 > 10) {
                    f.rect(0xFF6E9A4A, s0, S * 0.55f, s1, S - 3);
                    f.specks(r, 20, 0xFF5A8A3A, 0xFF8AB060, s0, S * 0.55f, s1, S - 3, 1.4f);
                }
                fanUnit(f, part == 1 ? L - 6 : 6, S / 2, 3);
                if (part == 0) f.sign(city, "RESEARCH", L / 2, S / 2, 6, 0xFF2E6A8A);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.UNI | Variants.MID | Variants.SUB;
        d.minS = 10;
        d.minL = 14;
        d.base = City.OFFICE;
        d.inside = Variants.I_LAB;
        d.roofCol = 0xFFF0F0EC;
        out.add(d);
    }

    /** An industrial robot arm on its plinth, from above. */
    static void robotArm(F f, float u, float v, float a, float len) {
        f.rect(0x40000000, u - 4 + 1.5f, v - 4 + 2, u + 4 + 1.5f, v + 4 + 2);
        f.rect(0xFF6A6E72, u - 4, v - 4, u + 4, v + 4);
        f.circle(0xFF3A3E42, u, v, 3.2f);
        double r1 = Math.toRadians(a), r2 = Math.toRadians(a + 70);
        float eu = u + (float) Math.cos(r1) * len * 0.55f, ev = v + (float) Math.sin(r1) * len * 0.55f;
        float tu = eu + (float) Math.cos(r2) * len * 0.45f, tv = ev + (float) Math.sin(r2) * len * 0.45f;
        f.line(0x40000000, 3, u + 2, v + 3, eu + 2, ev + 3);
        f.line(0x40000000, 2.4f, eu + 2, ev + 3, tu + 2, tv + 3);
        f.line(0xFFE07A20, 3, u, v, eu, ev);
        f.line(0xFFF09A40, 1, u, v, eu, ev);
        f.line(0xFFE07A20, 2.4f, eu, ev, tu, tv);
        f.circle(0xFF3A3E42, eu, ev, 1.8f);
        f.circle(0xFFE07A20, u, v, 2.2f);
        f.circle(0xFF5A5E62, tu, tv, 1.4f);
    }

    /** A small wheeled robot (an AGV) with its sensor dome. */
    static void agv(F f, float u, float v, float a, int col) {
        f.turn(u, v, a);
        Canvas c = f.c;
        Paint p = f.p;
        float[] o = off(f.ang(a), 1, 1.4f);
        roundRect(c, p, 0x48000000, -5 + o[0], -3.5f + o[1], 5 + o[0], 3.5f + o[1], 1.5f);
        roundRect(c, p, City.darken(col, 0.7f), -5, -3.5f, 5, 3.5f, 1.5f);
        roundRect(c, p, col, -4.5f, -3, 4.5f, 3, 1.2f);
        rect(c, p, 0xFF2A2E32, 3.2f, -2.4f, 4.5f, 2.4f);
        circle(c, p, 0xFF2A2E32, -1, 0, 1.4f);
        circle(c, p, 0xFF5ADA8A, -1, 0, 0.6f);
        c.restore();
    }

    // ------------------------------------------------------------------ 15. Robotics Factory

    static void robotics(List<Landmarks.Def> out) {
        // Long white assembly halls under a grid of skylights, and a test yard where arms and rovers are put through their paces.
        D d = new D("Robotics Factory", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                int lv = Math.max(3, S / 2 - 1);
                int ow = Math.max(3, L / 4);
                return new int[][]{B(1, 1, L - 2, lv, 2), B(L - ow - 1, lv + 2, ow, Math.min(3, S - lv - 3), 3)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float hv = (q[0][1] + q[0][3]) * T, ou = q[1][0] * T;
                f.rect(0xFF9A9C9E, 0, 0, L, S);
                // The test yard: pale epoxy with a grid, hatched safety zones and a painted loop for the rovers.
                float y0 = hv + 6, y1 = S - 4, x0 = 4, x1 = ou - 6;
                f.rect(0xFFC8CCCE, x0, y0, x1, y1);
                for (float u = x0 + 10; u < x1; u += 10) f.line(0x18000000, 0.5f, u, y0, u, y1);
                for (float v = y0 + 10; v < y1; v += 10) f.line(0x18000000, 0.5f, x0, v, x1, v);
                f.frame(0xFFE0C040, 1.2f, x0, y0, x1, y1);
                float lu0 = x0 + 8, lu1 = x1 - 8, lv0 = y0 + 8, lv1 = y1 - 8;
                if (lv1 - lv0 > 14) {
                    f.p.setStyle(Paint.Style.STROKE);
                    f.p.setStrokeWidth(1.4f);
                    f.p.setColor(0xFF2E7AB8);
                    Landmarks.RF.set(Math.min(f.x(lu0, lv0), f.x(lu1, lv1)), Math.min(f.y(lu0, lv0), f.y(lu1, lv1)),
                            Math.max(f.x(lu0, lv0), f.x(lu1, lv1)), Math.max(f.y(lu0, lv0), f.y(lu1, lv1)));
                    f.c.drawRoundRect(Landmarks.RF, 10, 10, f.p);
                    f.p.setStyle(Paint.Style.FILL);
                }
                // Robot arms in a fenced cell, rovers on the loop, cones and a charging pad.
                float cu = x0 + (x1 - x0) * 0.3f, cvv = (y0 + y1) / 2;
                if (y1 - y0 > 30) {
                    f.rect(0xFFD8C040, cu - 18, cvv - 12, cu + 18, cvv + 12);
                    for (float u = cu - 16; u < cu + 18; u += 4) f.line(0xFF2A2A2A, 1, u, cvv - 12, u + 3, cvv - 10);
                    f.rect(0xFFC0C4C6, cu - 16, cvv - 10, cu + 16, cvv + 10);
                    f.frame(0xFF6A6E72, 0.6f, cu - 18, cvv - 12, cu + 18, cvv + 12);
                    robotArm(f, cu - 8, cvv, 20, 16);
                    robotArm(f, cu + 8, cvv, 200, 14);
                    f.rect(0xFF5A7A9A, cu - 3, cvv - 3, cu + 3, cvv + 3);
                }
                agv(f, lu1 - 4, (lv0 + lv1) / 2, 90, 0xFFE8E6E0);
                agv(f, (lu0 + lu1) * 0.6f, lv0, 0, 0xFFE07A20);
                agv(f, (lu0 + lu1) * 0.5f, lv1, 180, 0xFF2E7AB8);
                for (int k = 0; k < 5; k++) {
                    float u = x0 + (x1 - x0) * (0.55f + k * 0.07f), v = cvv + (k % 2 == 0 ? -5 : 5);
                    f.circle(0x40000000, u + 0.8f, v + 1, 1.6f);
                    f.circle(0xFFE86A28, u, v, 1.6f);
                    f.circle(0xFFF0F0F0, u, v, 0.6f);
                }
                // A walking robot on four legs, mid-stride.
                float du = x0 + (x1 - x0) * 0.85f, dv = cvv;
                f.roundRect(0x40000000, du - 4 + 1.5f, dv - 2 + 2, du + 4 + 1.5f, dv + 2 + 2, 1.5f);
                f.line(0xFF2A2A2A, 0.8f, du - 3, dv - 1.6f, du - 4.5f, dv - 4);
                f.line(0xFF2A2A2A, 0.8f, du + 3, dv - 1.6f, du + 4, dv - 3.6f);
                f.line(0xFF2A2A2A, 0.8f, du - 3, dv + 1.6f, du - 2.4f, dv + 4);
                f.line(0xFF2A2A2A, 0.8f, du + 3, dv + 1.6f, du + 4.8f, dv + 3.8f);
                f.roundRect(0xFFE0C040, du - 4, dv - 2, du + 4, dv + 2, 1.5f);
                f.rect(0xFF2A2A2A, du + 3, dv - 1, du + 5, dv + 1);
                f.rect(0xFF5A6A7A, 2, hv + 1, ou - 4, hv + 3);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                if (part == 1) {
                    // The design office: a dark glass box with a planted roof terrace.
                    f.rect(0xFF2E3A46, 0, 0, L, S);
                    f.rect(0xFF4A5A6A, 1, 1, L - 1, S - 1);
                    f.rect(0xFFA88A62, L * 0.45f, 2, L - 2, S - 2);
                    for (float u = L * 0.45f + 2; u < L - 2; u += 2) f.line(0x30000000, 0.4f, u, 2, u, S - 2);
                    f.bush(L * 0.6f, S * 0.35f, 2.2f);
                    f.bush(L * 0.85f, S * 0.65f, 2.2f);
                    return;
                }
                // The assembly hall: brilliant white, a regular grid of rooflights and a band of plant.
                f.rect(0xFFB8BCC0, 0, 0, L, S);
                f.rect(0xFFF6F6F4, 1, 1, L - 1, S - 1);
                for (float u = 6; u + 6 < L - 4; u += 12)
                    for (float v = 5; v + 4 < S - 8; v += 10) {
                        f.rect(0x30000000, u + 0.8f, v + 0.8f, u + 6.8f, v + 4.8f);
                        f.rect(0xFF9CC4DA, u, v, u + 6, v + 4);
                        f.rect(0xFFC8E0EC, u, v, u + 6, v + 1.2f);
                    }
                f.rect(0xFFDADCDE, 2, S - 7, L - 2, S - 2);
                for (float u = 8; u < L - 6; u += 12) fanUnit(f, u, S - 4.5f, 2.2f);
                f.sign(city, "ROBOTICS", L / 2, S / 2, 7, 0xFF2E7AB8);
            }
        };
        d.ground = City.LOT;
        d.districts = Variants.IND | Variants.UNI;
        d.minS = 10;
        d.minL = 14;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_FACTORY;
        d.roofCol = 0xFFF6F6F4;
        out.add(d);
    }

    /** A refrigerated trailer backed up to a dock, its cab pointing away (front at +). */
    static void reefer(F f, float u, float v, float a, float len, int cab) {
        float l = len / 2, w = 4.6f;
        f.turn(u, v, a);
        Canvas c = f.c;
        Paint p = f.p;
        shadowBox(c, p, f.ang(a), l, w, 1, 1.8f, 2.4f, 0x50000000);
        rect(c, p, 0xFFB8BCC0, -l, -w, l - 9, w);
        rect(c, p, 0xFFF2F2F0, -l + 0.5f, -w + 0.5f, l - 9.5f, w - 0.5f);
        for (float t = -l + 3; t < l - 10; t += 3) line(c, p, 0x14000000, 0.4f, t, -w + 0.5f, t, w - 0.5f);
        rect(c, p, 0xFF5A6066, l - 12, -w + 1, l - 9.5f, w - 1);
        rect(c, p, 0xFF2A2A2A, l - 9.5f, -1, l - 7.5f, 1);
        roundRect(c, p, City.darken(cab, 0.7f), l - 7.5f, -w, l, w, 1.5f);
        roundRect(c, p, cab, l - 7.1f, -w + 0.4f, l - 0.4f, w - 0.4f, 1.3f);
        rect(c, p, 0xFF26303A, l - 2.4f, -w + 0.9f, l - 0.8f, w - 0.9f);
        c.restore();
    }

    /** A horizontal cylinder tank on saddles. */
    static void bullet(F f, float u0, float v0, float u1, float v1, int col) {
        float h = v1 - v0;
        f.roundRect(0x48000000, u0 + 1.5f, v0 + 2, u1 + 1.5f, v1 + 2, h / 2);
        f.roundRect(City.darken(col, 0.7f), u0, v0, u1, v1, h / 2);
        f.roundRect(col, u0 + 0.5f, v0 + 0.4f, u1 - 0.5f, v1 - h * 0.35f, h / 3);
        f.rect(City.lighten(col, 0.3f), u0 + h / 2, v0 + h * 0.2f, u1 - h / 2, v0 + h * 0.32f);
    }

    // ------------------------------------------------------------------ 16. Cold Store

    static void coldStore(List<Landmarks.Def> out) {
        // A vast white insulated box crowned with condenser banks, refrigerated trailers at its docks, an engine room with ammonia tanks.
        D d = new D("Cold Store", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                int lv = S - 6;
                return new int[][]{B(1, 1, L - 2, lv, 3), B(L - 4, S - 3, 3, 2, 1)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float cv = (q[0][1] + q[0][3]) * T, eu = q[1][0] * T;
                f.rect(0xFFA4A6A6, 0, 0, L, S);
                for (float u = 0; u < L; u += 16) f.line(0x16000000, 0.5f, u, 0, u, S);
                for (float v = 0; v < S; v += 16) f.line(0x16000000, 0.5f, 0, v, L, v);
                // The docks along the front: levellers, bumpers, guide lines, and trailers backed onto some.
                int n = (int) ((eu - 16) / 18);
                for (int k = 0; k < n; k++) {
                    float u = 10 + k * 18 + 9;
                    f.rect(0xFF3A3C3E, u - 6, cv, u + 6, cv + 3);
                    f.rect(0xFF2A2A2A, u - 7, cv, u - 6, cv + 1.6f);
                    f.rect(0xFF2A2A2A, u + 6, cv, u + 7, cv + 1.6f);
                    f.line(0xFFE0C040, 0.8f, u - 8, cv + 4, u - 8, cv + 40);
                    if (k % 2 == 0 && k < 6) reefer(f, u, cv + 3 + 19, 90, 38, k % 4 == 0 ? 0xFF2E5A8A : 0xFFB8302A);
                }
                // Ammonia tanks beside the engine room, fenced.
                float tv0 = q[1][1] * T - 2, tu0 = eu - 34;
                if (tu0 > n * 18 * 0.5f) {
                    f.rect(0xFF8A8C8C, tu0 - 2, tv0 - 2, eu - 4, S - 4);
                    bullet(f, tu0, tv0 + 2, eu - 6, tv0 + 9, 0xFFE8E6E0);
                    bullet(f, tu0, tv0 + 13, eu - 6, tv0 + 20, 0xFFE8E6E0);
                    f.frame(0xFF5A6066, 0.6f, tu0 - 2, tv0 - 2, eu - 4, S - 4);
                }
                f.line(0xFF8A9096, 1.6f, eu + 8, cv, eu + 8, q[1][1] * T);
                f.line(0xFFB8BCC0, 1, eu + 12, cv, eu + 12, q[1][1] * T);
                for (float u = 4; u < L; u += 10) f.circle(0xFFE0C040, u, S - 2, 0.8f);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                if (part == 1) {
                    // The engine room: a plain box with louvres and a vent stack.
                    flatRoof(f.c, f.p, f.x(0, 0), f.y(0, 0), f.x(L, S), f.y(L, S), 0xFFB8BAB8);
                    for (float u = 4; u < L * 0.6f; u += 2) f.line(0xFF6A6E72, 0.6f, u, S * 0.3f, u, S * 0.7f);
                    f.circle(0x40000000, L * 0.8f + 1, S / 2 + 1.4f, 2.2f);
                    f.circle(0xFF8A8E92, L * 0.8f, S / 2, 2.2f);
                    f.circle(0xFF3A3E42, L * 0.8f, S / 2, 1.2f);
                    return;
                }
                // The store: insulated white panels, banks of condensers with twin fans, the pipe runs to them.
                f.rect(0xFFC8CCCE, 0, 0, L, S);
                f.rect(0xFFF4F6F6, 1.2f, 1.2f, L - 1.2f, S - 1.2f);
                for (float u = 4; u < L - 1; u += 4) f.line(0x12000000, 0.5f, u, 1.2f, u, S - 1.2f);
                float cv = S * 0.32f;
                f.line(0xFFA8AEB4, 1.4f, 4, cv + 8, L - 4, cv + 8);
                f.line(0xFF8A9096, 1, 4, cv + 10, L - 4, cv + 10);
                for (float u = 8; u + 14 < L - 4; u += 20) {
                    f.rect(0x40000000, u + 1.5f, cv - 5 + 2, u + 15.5f, cv + 5 + 2);
                    f.rect(0xFF8A9096, u, cv - 5, u + 14, cv + 5);
                    f.rect(0xFFB0B6BC, u + 0.6f, cv - 4.4f, u + 13.4f, cv + 4.4f);
                    for (int k = 0; k < 2; k++) {
                        float fu = u + 3.6f + k * 6.8f;
                        f.circle(0xFF3A3E42, fu, cv, 3);
                        f.line(0xFF7A8086, 0.5f, fu - 2.6f, cv, fu + 2.6f, cv);
                        f.line(0xFF7A8086, 0.5f, fu, cv - 2.6f, fu, cv + 2.6f);
                        f.circle(0xFF9AA0A6, fu, cv, 0.8f);
                    }
                    f.line(0xFFA8AEB4, 0.8f, u + 7, cv + 5, u + 7, cv + 8);
                }
                for (float u = 10; u < L - 6; u += 30) {
                    f.rect(0xFFDCDEE0, u, S * 0.66f, u + 6, S * 0.66f + 5);
                    f.frame(0xFF9AA0A6, 0.5f, u, S * 0.66f, u + 6, S * 0.66f + 5);
                }
                f.sign(city, "COLD STORE", L / 2, S * 0.7f, 7, 0xFF2E6A9A);
            }
        };
        d.ground = City.LOT;
        d.districts = Variants.IND;
        d.minS = 10;
        d.minL = 14;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_RACKS;
        d.roofCol = 0xFFF4F6F6;
        out.add(d);
    }

    /** A brick hack: a long low stack of bricks drying under a board. */
    static void bricks(F f, float u0, float v0, float u1, float v1) {
        f.rect(0x40000000, u0 + 1, v0 + 1.4f, u1 + 1, v1 + 1.4f);
        f.rect(0xFF8A3E2A, u0, v0, u1, v1);
        for (float u = u0 + 0.4f; u < u1 - 0.4f; u += 1.6f) f.rect(0xFFB0563A, u, v0 + 0.4f, Math.min(u1 - 0.4f, u + 1.2f), v1 - 0.4f);
        f.line(0x30000000, 0.4f, u0, (v0 + v1) / 2, u1, (v0 + v1) / 2);
    }

    // ------------------------------------------------------------------ 17. Brick Kilns

    static void brickKilns(List<Landmarks.Def> out) {
        // Bottle kilns in a row with the drying shed beside them, hacks of new bricks, and the terraced clay pit they dig.
        D d = new D("Brick Kilns", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                int ks = S >= 13 ? 4 : 3;
                int[][] a = {B(1, 1, ks, ks, 3), B(ks + 2, 1, ks, ks, 3), B(2 * ks + 3, 1, L - 2 * ks - 4, 2, 1)};
                if (L - 2 * ks - 4 < 3) return new int[][]{a[0], a[1]};
                return a;
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float kv = (q[0][1] + q[0][3]) * T;
                f.rect(0xFF8E7058, 0, 0, L, S);
                f.specks(r, 80, 0x22000000, 0x18FFFFFF, 0, 0, L, S, 1.6f);
                // Brick hacks drying beside the shed.
                if (q.length > 2) {
                    float u0 = q[2][0] * T, sv1 = (q[2][1] + q[2][3]) * T;
                    for (float v = sv1 + 4; v + 3 < kv; v += 6) bricks(f, u0 + 2, v, L - 6, v + 3);
                }
                // The clay pit: terraces stepping down to standing water, a ramp down one side.
                float p0 = kv + 10, p1 = S - 4, pu0 = 6, pu1 = L - 6;
                if (p1 - p0 > 24) {
                    int[] tc = {0xFFA8764E, 0xFF96663E, 0xFF845834, 0xFF724C2C};
                    for (int k = 0; k < 4; k++) {
                        float in = k * Math.min(p1 - p0, pu1 - pu0) * 0.09f;
                        f.roundRect(tc[k], pu0 + in, p0 + in, pu1 - in, p1 - in, 10);
                        f.roundRect(0x30000000, pu0 + in, p0 + in, pu1 - in, p0 + in + 1.5f, 4);
                    }
                    float in = 4 * Math.min(p1 - p0, pu1 - pu0) * 0.09f;
                    f.roundRect(0xFF5A7A80, pu0 + in, p0 + in, pu1 - in, p1 - in, 8);
                    f.roundRect(0xFF6A8E92, pu0 + in + 2, p0 + in + 1, pu1 - in - 2, p0 + in + 3, 2);
                    f.poly(0xFFB08A60, pu1 - 2, p0, pu1 - 14, p0, pu1 - in - 6, p1 - in - 2, pu1 - in + 4, p1 - in - 2);
                    // A digger working the face.
                    float du = pu0 + (pu1 - pu0) * 0.3f, dv = p0 + in * 0.6f;
                    f.rect(0x48000000, du - 5 + 1.5f, dv - 4 + 2, du + 5 + 1.5f, dv + 4 + 2);
                    f.rect(0xFF2A2A2A, du - 5, dv - 4.5f, du + 5, dv - 2.5f);
                    f.rect(0xFF2A2A2A, du - 5, dv + 2.5f, du + 5, dv + 4.5f);
                    f.rect(0xFFD8A020, du - 4, dv - 3, du + 4, dv + 3);
                    f.line(0xFFD8A020, 1.6f, du, dv, du - 3, dv + 10);
                    f.rect(0xFF3A3A3A, du - 6, dv + 9, du, dv + 12);
                }
                // Coal heap by the kilns.
                float cu = q[1][0] * T + q[1][2] * T + 8;
                if (q.length == 2 && cu + 16 < L) heap(f, r, cu, 8, cu + 16, 22, 0xFF2E2E30);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                if (part == 2) {
                    // The drying shed: a long pantiled roof on brick piers.
                    f.rect(0xFF5A3A2A, 0, 0, L, S);
                    f.rect(0xFFB0664A, 0.6f, 0.6f, L - 0.6f, S / 2);
                    f.rect(0xFF8E4E36, 0.6f, S / 2, L - 0.6f, S - 0.6f);
                    for (float u = 1.6f; u < L; u += 1.8f) {
                        f.line(0x30000000, 0.5f, u, 0.6f, u, S - 0.6f);
                    }
                    f.line(0xFFC88A6A, 1, 0, S / 2, L, S / 2);
                    return;
                }
                // A bottle kiln: the brick hovel rising and narrowing to its open neck.
                float cx = L / 2, cy = S / 2, rad = Math.min(L, S) / 2 - 0.5f;
                f.rect(0xFF7A6050, 0, 0, L, S);
                f.specks(r, 12, 0x30000000, 0x20FFFFFF, 0, 0, L, S, 1);
                f.circle(0x50000000, cx + rad * 0.3f, cy + rad * 0.4f, rad);
                int[] sh = {0xFF7A3626, 0xFF8A4230, 0xFF9A4E38, 0xFFA85A42, 0xFFB4664C, 0xFFBC7056};
                for (int k = 0; k < 6; k++) {
                    float rr = rad * (1 - k * 0.12f), o = k * rad * 0.035f;
                    f.circle(sh[k], cx - o, cy - o, rr);
                }
                for (int k = 1; k < 4; k++) f.ring(0x28000000, 0.5f, cx - k * rad * 0.04f, cy - k * rad * 0.04f, rad * (1 - k * 0.22f));
                // Iron bands, and the neck.
                f.ring(0xFF3A2A22, 0.7f, cx, cy, rad * 0.94f);
                f.circle(0xFF5A2A1E, cx - rad * 0.22f, cy - rad * 0.22f, rad * 0.3f);
                f.circle(0xFF1A1412, cx - rad * 0.22f, cy - rad * 0.22f, rad * 0.2f);
                f.circle(0x50A0A0A0, cx - rad * 0.1f, cy - rad * 0.36f, rad * 0.12f);
                if (part == 0) f.sign(city, "BRICKWORKS", cx, cy + rad * 0.55f, 4, 0xFFF0E0C0);
            }
        };
        d.ground = City.DIRT;
        d.districts = Variants.IND | Variants.OLD;
        d.minS = 10;
        d.minL = 14;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_FACTORY;
        d.roofCol = 0xFFA85A42;
        out.add(d);
    }

    // ------------------------------------------------------------------ 18. Gasometer

    static void gasometer(List<Landmarks.Def> out) {
        // A Victorian gas holder: the domed bell inside a ring of cast-iron columns tied by lattice girders, on rough grass.
        D d = new D("Gasometer", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                return new int[][]{B(1, S - 3, 3, 2, 1), B(L - 5, S - 3, 4, 2, 2)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                f.rect(0xFF7E9A5E, 0, 0, L, S);
                for (int k = 0; k < 50; k++) f.circle(r.nextBoolean() ? 0x16000000 : 0x14FFFFFF, r.nextFloat() * L, r.nextFloat() * S, 2 + r.nextFloat() * 4);
                float rad = Math.min(S * 0.4f, (S - 3 * T - 12) / 2);
                rad = Math.min(rad, L / 2 - 10);
                float cu = L / 2, cv = rad + 10;
                // A brick wall round the site and a cinder road to the buildings.
                f.frame(0xFF8A4A36, 2, 1, 1, L - 1, S - 1);
                f.rect(0xFF6A6460, 0, S - 3 * T - 6, L, S - 3 * T + 2);
                // The holder's tank wall and the water seal.
                f.circle(0x40000000, cu + 4, cv + 5, rad + 2);
                f.circle(0xFF6A6460, cu, cv, rad + 2);
                f.circle(0xFF3E5A5E, cu, cv, rad);
                // The bell: lifts telescoped inside each other, a shallow crown of riveted plates.
                f.circle(0xFF5A5E62, cu, cv, rad - 2);
                f.circle(0xFF6E7276, cu, cv, rad - 5);
                int n = 24;
                for (int k = 0; k < n; k++) {
                    double a0 = k * 2 * Math.PI / n, a1 = (k + 1) * 2 * Math.PI / n, am = (a0 + a1) / 2;
                    float sh = 1f + 0.16f * (float) Math.cos(am + 3 * Math.PI / 4);
                    float rr = rad - 7;
                    f.poly(Landmarks.shade(0xFF7E8286, sh), cu, cv, cu + (float) Math.cos(a0) * rr, cv + (float) Math.sin(a0) * rr,
                            cu + (float) Math.cos(a1) * rr, cv + (float) Math.sin(a1) * rr);
                }
                for (int k = 1; k < 4; k++) f.ring(0x22000000, 0.5f, cu, cv, (rad - 7) * k / 4f);
                f.circle(0xFF6A6E72, cu, cv, 3);
                f.circle(0xFF9AA0A6, cu - 0.8f, cv - 0.8f, 1.6f);
                // The guide frame: columns round the outside, a double ring of girders with lattice between.
                int m = 16;
                float fr = rad + 4;
                f.ring(0x30000000, 2.6f, cu + 8, cv + 11, fr);
                for (int k = 0; k < m; k++) {
                    double a = k * 2 * Math.PI / m;
                    float pu = cu + (float) Math.cos(a) * fr, pv = cv + (float) Math.sin(a) * fr;
                    f.line(0x30000000, 2.2f, pu, pv, pu + 8, pv + 11);
                }
                f.ring(0xFF3E5A4A, 0.9f, cu, cv, fr - 1.6f);
                f.ring(0xFF3E5A4A, 0.9f, cu, cv, fr + 1.6f);
                int z = m * 4;
                for (int k = 0; k < z; k++) {
                    double a0 = k * 2 * Math.PI / z, a1 = (k + 1) * 2 * Math.PI / z;
                    float r0 = k % 2 == 0 ? fr - 1.6f : fr + 1.6f, r1 = k % 2 == 0 ? fr + 1.6f : fr - 1.6f;
                    f.line(0xFF4E6A5A, 0.5f, cu + (float) Math.cos(a0) * r0, cv + (float) Math.sin(a0) * r0, cu + (float) Math.cos(a1) * r1, cv + (float) Math.sin(a1) * r1);
                }
                for (int k = 0; k < m; k++) {
                    double a = k * 2 * Math.PI / m;
                    float pu = cu + (float) Math.cos(a) * fr, pv = cv + (float) Math.sin(a) * fr;
                    f.circle(0xFF2E4A3A, pu, pv, 2.2f);
                    f.circle(0xFF5E7A6A, pu - 0.5f, pv - 0.5f, 1.2f);
                    // Radial struts into the bell's guide rollers.
                    f.line(0xFF3E5A4A, 0.6f, pu, pv, cu + (float) Math.cos(a) * (rad - 2), cv + (float) Math.sin(a) * (rad - 2));
                }
                // The gas main to the valve house.
                float vu = (q[0][0] + q[0][2] / 2f) * T, vv = q[0][1] * T;
                f.line(0xFF4A4A4C, 2.4f, cu - rad * 0.7f, cv + rad * 0.7f, vu, vv);
                f.line(0xFFD8B030, 1, cu - rad * 0.7f, cv + rad * 0.7f, vu, vv);
                for (int k = 0; k < 4; k++) f.tree(L - 8 - k * 9, S - 3 * T - 14 + (k % 2) * 4, 4, 0xFF5A7A3A);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                // Brick buildings with slate roofs (the office has a lantern).
                f.rect(0xFF8A4A36, 0, 0, L, S);
                hip(f, 0xFF5A6070, 1, 1, L - 1, S - 1, true);
                if (part == 1) {
                    f.rect(0xFF3E4450, L * 0.4f, S / 2 - 1.5f, L * 0.6f, S / 2 + 1.5f);
                    chimney(f, L * 0.15f, S * 0.3f, 2);
                    f.sign(city, "GAS WORKS", L / 2, S / 2, 4, 0xFFF0E0C0);
                } else {
                    f.circle(0xFF8A8E92, L / 2, S / 2, 1.6f);
                }
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.IND | Variants.OLD;
        d.minS = 10;
        d.minL = 12;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_FACTORY;
        d.roofCol = 0xFF5A6070;
        out.add(d);
    }

    // ------------------------------------------------------------------ 19. Georgian Crescent

    static void georgianCrescent(List<Landmarks.Def> out) {
        // A sweeping crescent of stone townhouses stepping round a private railed garden, a cobbled carriageway between.
        D d = new D("Georgian Crescent", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                int n = Math.max(3, (L - 1) / 5);
                int w = (L - 2 - (n - 1)) / n, extra = (L - 2 - (n - 1)) - w * n;
                int dep = (S >= 12 && w >= 4) ? 3 : 2;
                int mo = Math.max(2, Math.min(4, S / 4));
                int[][] a = new int[n][];
                int u = 1 + extra / 2;
                for (int i = 0; i < n; i++) {
                    float t = (i - (n - 1) / 2f) / ((n - 1) / 2f);
                    int off = Math.round(t * t * mo);
                    a[i] = B(u, 1 + off, w, dep, 4);
                    u += w + 1;
                }
                return a;
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                f.rect(0xFF74A058, 0, 0, L, S);
                int n = q.length;
                // The mews lane behind.
                f.rect(0xFFB8AE9A, 0, 0, L, T - 3);
                // The carriageway sweeping along the fronts.
                float fe = (q[0][1] + q[0][3]) * T, fm = (q[n / 2][1] + q[n / 2][3]) * T;
                if (n % 2 == 0) fm = Math.min(fm, (q[n / 2 - 1][1] + q[n / 2 - 1][3]) * T);
                float ve = fe + 10, vm = fm + 10, cvv = 2 * vm - ve;
                f.curve(0xFFD8D0BC, 26, -10, ve - 2, L / 2, cvv - 2, L + 10, ve - 2);
                f.curve(0xFF8E887E, 14, -10, ve + 4, L / 2, cvv + 4, L + 10, ve + 4);
                for (int k = 0; k < 2; k++) f.curve(0x14000000, 0.5f, -10, ve + 1 + k * 6, L / 2, cvv + 1 + k * 6, L + 10, ve + 1 + k * 6);
                // The garden: a great lawn inside railings, gravel walks, trees round the edge and an urn in the middle.
                float g0 = ve + 14, gm = vm + 14;
                float gv1 = S - 4, gu0 = 6, gu1 = L - 6;
                Path pth = new Path();
                f.mv(pth, gu0, g0 + 4);
                f.qd(pth, L / 2, 2 * gm - g0, gu1, g0 + 4);
                f.ln(pth, gu1, gv1);
                f.ln(pth, gu0, gv1);
                pth.close();
                f.fill(pth, 0xFF6E9C50);
                f.stroke(pth, 0xFF2A2A2C, 0.8f);
                float gcv = (gm + gv1) / 2 + 4;
                f.line(0xFFD8CCB0, 3, L / 2, gm + 6, L / 2, gv1);
                f.oval(0xFFD8CCB0, L / 2 - 12, gcv - 8, L / 2 + 12, gcv + 8);
                f.oval(0xFF6E9C50, L / 2 - 9, gcv - 5, L / 2 + 9, gcv + 5);
                f.circle(0xFFC8C0AC, L / 2, gcv, 2.6f);
                f.circle(0xFFE0D8C4, L / 2 - 0.6f, gcv - 0.6f, 1.4f);
                for (int k = 0; k < 6; k++) {
                    float u = gu0 + 8 + (gu1 - gu0 - 16) * k / 5f;
                    float t = (u - gu0) / (gu1 - gu0);
                    float ev = (1 - t) * (1 - t) * (g0 + 4) + 2 * t * (1 - t) * (2 * gm - g0) + t * t * (g0 + 4);
                    if (gv1 - ev > 20) f.tree(u, gv1 - 8, 5.5f, 0xFF3E7A3A);
                    if (gcv - ev > 16 && Math.abs(u - L / 2) > 8) f.tree(u, ev + 9, 4.5f, 0xFF4E8A44);
                }
                for (int k = 0; k < 4; k++) {
                    float bu = L / 2 + (k % 2 == 0 ? -1 : 1) * 24, bv = gcv + (k < 2 ? -1 : 1) * Math.min(14, (gv1 - gm) * 0.22f);
                    if (bu - 12 < gu0 + 4) continue;
                    f.roundRect(0xFFD8CCB0, bu - 12, bv - 4.5f, bu + 12, bv + 4.5f, 4);
                    f.roundRect(0xFF5A8A44, bu - 10.5f, bv - 3, bu + 10.5f, bv + 3, 3);
                    for (float t = bu - 8; t <= bu + 8; t += 2.6f)
                        f.circle(((int) (t + k)) % 2 == 0 ? 0xFFE07A9A : 0xFFF0E0A0, t, bv + (((int) t) % 2 == 0 ? -1 : 1), 0.8f);
                }
                f.bench(L / 2 - 16, gcv, false);
                f.bench(L / 2 + 16, gcv, false);
                // Area railings and steps before each house.
                for (int[] b : q) {
                    float u0 = b[0] * T, u1 = u0 + b[2] * T, v = (b[1] + b[3]) * T;
                    f.line(0xFF2A2A2C, 0.6f, u0, v + 3, u1, v + 3);
                    for (float u = u0 + 8; u < u1; u += 16) f.rect(0xFFD8D0BC, u - 2.5f, v, u + 2.5f, v + 3);
                }
                for (int k = 0; k <= 4; k++) {
                    float u = L * k / 4;
                    float t = u / L, ev = (1 - t) * (1 - t) * ve + 2 * t * (1 - t) * cvv + t * t * ve;
                    f.lamp(u + (k == 0 ? 3 : k == 4 ? -3 : 0), ev - 9);
                }
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                // A terrace of houses: a stone parapet to the front, slate pitches, chimney stacks on each party wall.
                float L = f.L, S = f.S;
                f.rect(0xFFC8BC9C, 0, 0, L, S);
                f.rect(0xFF5E6672, 1, 1.5f, L - 1, S * 0.5f);
                f.rect(0xFF4A515C, 1, S * 0.5f, L - 1, S - 4);
                for (float v = 3; v < S - 4; v += 2.2f) f.line(0x1A000000, 0.4f, 1, v, L - 1, v);
                f.line(0xFF8A929E, 1, 1, S * 0.5f, L - 1, S * 0.5f);
                f.rect(0xFFE0D6BC, 0, S - 4, L, S);
                f.rect(0xFFB8AC8C, 0, S - 4, L, S - 3.2f);
                for (float u = 1.5f; u < L; u += 2.5f) f.circle(0xFFC8BC9C, u, S - 2, 0.6f);
                int houses = Math.max(2, Math.round(L / 16));
                float hw = L / houses;
                for (int k = 0; k < houses; k++) {
                    float u = k * hw;
                    if (k > 0) {
                        f.line(0xFFB8AC8C, 1, u, 1.5f, u, S - 4);
                        chimney(f, u - 3, S * 0.5f - 1.7f, 3);
                    }
                    f.rect(0xFF3A3E46, u + hw * 0.3f, S * 0.66f, u + hw * 0.7f, S * 0.66f + 2.6f);
                    f.rect(0xFFE8E0CC, u + hw * 0.3f, S * 0.66f, u + hw * 0.7f, S * 0.66f + 0.7f);
                }
                if (part == 0) f.sign(city, "THE CRESCENT", L / 2, S / 2, 5, 0xFFF0E8D0);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.OLD | Variants.MID | Variants.SUB;
        d.minS = 11;
        d.minL = 14;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFF5E6672;
        out.add(d);
    }

    /** A small wind turbine: the tower, nacelle and three blades, with its shadow. */
    static void turbineMast(F f, float u, float v, float bl, float rot) {
        f.circle(0x40000000, u + 6, v + 8, 2);
        f.circle(0xFFD8DADC, u, v, 2.2f);
        for (int k = 0; k < 3; k++) {
            double a = Math.toRadians(rot + k * 120);
            float eu = u + (float) Math.cos(a) * bl, ev = v + (float) Math.sin(a) * bl * 0.25f;
            f.line(0x30000000, 1.6f, u + 6, v + 8, eu + 6, ev + 8);
        }
        f.rect(0xFFB8BCC0, u - 1.5f, v - 4, u + 1.5f, v + 3);
        for (int k = 0; k < 3; k++) {
            double a = Math.toRadians(rot + k * 120);
            float eu = u + (float) Math.cos(a) * bl, ev = v - 4 + (float) Math.sin(a) * bl * 0.25f;
            f.line(0xFFF0F2F4, 1.6f, u, v - 4, eu, ev);
            f.line(0xFFB8BCC0, 0.5f, u, v - 3.4f, eu, ev + 0.5f);
        }
        f.circle(0xFFE8EAEC, u, v - 4, 1.4f);
    }

    /** A raised vegetable bed in a timber frame. */
    static void vegBed(F f, Random r, float u0, float v0, float u1, float v1) {
        f.rect(0x38000000, u0 + 1, v0 + 1.2f, u1 + 1, v1 + 1.2f);
        f.rect(0xFF8A6A44, u0, v0, u1, v1);
        f.rect(0xFF5A4030, u0 + 1, v0 + 1, u1 - 1, v1 - 1);
        int kind = r.nextInt(3);
        for (float v = v0 + 2.5f; v < v1 - 1.5f; v += 3)
            for (float u = u0 + 2.5f; u < u1 - 1.5f; u += 3) {
                if (kind == 0) f.circle(0xFF6AA848, u, v, 1.2f);
                else if (kind == 1) f.circle(0xFF8AB858, u, v, 0.8f);
                else {
                    f.circle(0xFF4E8A3A, u, v, 1.1f);
                    if (r.nextInt(3) == 0) f.circle(0xFFD84A2A, u + 0.5f, v + 0.4f, 0.5f);
                }
            }
    }

    // ------------------------------------------------------------------ 20. Eco Village

    static void ecoVillage(List<Landmarks.Def> out) {
        // Timber homes with turf roofs in two loose rows round a shared green of ponds, veg beds, a polytunnel and a turbine.
        D d = new D("Eco Village", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                int n = Math.max(2, (L - 1) / 4);
                int sp = (L - 1) / n;
                int[][] a = new int[n * 2][];
                for (int i = 0; i < n; i++) {
                    a[i] = B(1 + i * sp, 1 + (i % 2), sp - 1, 2, 2);
                    a[n + i] = B(1 + i * sp, S - 3 - ((i + 1) % 2), sp - 1, 2, 2);
                }
                return a;
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                f.rect(0xFF7AA45A, 0, 0, L, S);
                for (int k = 0; k < 40; k++) f.circle(r.nextBoolean() ? 0x14000000 : 0x16FFFFFF, r.nextFloat() * L, r.nextFloat() * S, 2 + r.nextFloat() * 4);
                float m0 = 4 * T, m1 = S - 4 * T, mv = (m0 + m1) / 2;
                // A winding gravel path through the middle, with stubs to each door.
                Path pth = new Path();
                f.mv(pth, 0, mv);
                f.qd(pth, L * 0.25f, m0 - 4, L * 0.5f, mv);
                f.qd(pth, L * 0.75f, m1 + 4, L, mv);
                f.stroke(pth, 0xFFC8BC9C, 5);
                int n = q.length / 2;
                for (int i = 0; i < q.length; i++) {
                    float u = (q[i][0] + q[i][2] / 2f) * T, v0 = (i < n ? q[i][1] + q[i][3] : q[i][1]) * T;
                    f.line(0xFFC8BC9C, 2.5f, u, v0, u, i < n ? v0 + 10 : v0 - 10);
                }
                // Ponds, veg beds and a polytunnel in the middle strip.
                float pu = L * 0.18f;
                if (m1 - m0 > 20) {
                    pond(f.c, f.p, f.x(pu - 14, mv + 2), f.y(pu - 14, mv + 2), f.x(pu + 12, m1 - 2), f.y(pu + 12, m1 - 2));
                    f.oval(0xFF5A9A48, pu + 6, mv + 4, pu + 10, mv + 7);
                    for (int k = 0; k < 5; k++) f.line(0xFF5A7A3A, 0.6f, pu - 12 + k * 1.5f, mv + 4, pu - 13 + k * 1.5f, mv);
                }
                float bu = L * 0.42f;
                for (int k = 0; k < 3; k++) {
                    float u = bu + k * 13;
                    if (u + 10 > L * 0.7f) break;
                    vegBed(f, r, u, m0 + 2, u + 10, Math.min(m0 + 2 + 20, mv - 3));
                }
                if (m1 - mv > 14) {
                    float tu = L * 0.5f;
                    f.rect(0x40000000, tu + 1.5f, mv + 4 + 2, tu + 28 + 1.5f, m1 - 2 + 2);
                    vault(f, 0xFFE8ECE8, tu, mv + 4, tu + 28, m1 - 2);
                    for (float u = tu + 4; u < tu + 28; u += 4) f.line(0x30000000, 0.5f, u, mv + 4, u, m1 - 2);
                }
                // Compost bins and hens.
                float cu = L * 0.78f;
                for (int k = 0; k < 3; k++) {
                    f.rect(0xFF6A4E34, cu + k * 6, m0 + 2, cu + k * 6 + 5, m0 + 7);
                    f.rect(0xFF4A3424, cu + k * 6 + 0.8f, m0 + 2.8f, cu + k * 6 + 4.2f, m0 + 6.2f);
                }
                for (int k = 0; k < 5; k++) {
                    float u = cu + r.nextFloat() * 16, v = m0 + 12 + r.nextFloat() * 8;
                    f.circle(0xFFF0EEE6, u, v, 1);
                    f.circle(0xFFC83A2A, u + 0.8f, v - 0.4f, 0.4f);
                }
                // The community turbine on the green and fruit trees.
                turbineMast(f, L * 0.88f, mv + 4, 16, 30);
                for (int k = 0; k < 4; k++) f.tree(L * 0.92f - k * 10 + (k % 2) * 3, m1 - 4 - (k % 2) * 6, 3.6f, 0xFF6A9A3E);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                // A turf roof edged in timber, a strip of solar slates and a round rooflight.
                float L = f.L, S = f.S;
                f.rect(0xFF8A6A44, 0, 0, L, S);
                int g = (part % 3 == 0) ? 0xFF6E9A48 : part % 3 == 1 ? 0xFF7AA454 : 0xFF8AA458;
                f.roundRect(g, 1, 1, L - 1, S - 1, 3);
                f.specks(r, 30, City.darken(g, 0.82f), City.lighten(g, 0.15f), 2, 2, L - 2, S - 2, 1.4f);
                for (int k = 0; k < 4; k++) f.circle(r.nextBoolean() ? 0xFFE0D060 : 0xFFE8E8F0, 3 + r.nextFloat() * (L - 6), 3 + r.nextFloat() * (S - 6), 0.6f);
                float s0 = L * 0.15f, s1 = L * 0.6f;
                f.rect(0xFF8A9096, s0, S * 0.58f, s1, S - 3);
                for (float u = s0 + 0.5f; u + 3 < s1; u += 3.5f) f.rect(0xFF2A3E56, u, S * 0.58f + 0.5f, u + 3, S - 3.5f);
                f.circle(0xFF8A9096, L * 0.8f, S * 0.4f, 2.4f);
                f.circle(0xFFA8CCE0, L * 0.8f, S * 0.4f, 1.6f);
                f.circle(0xFF7A5A40, L * 0.82f, S * 0.75f, 1.4f);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.SUB | Variants.PARK;
        d.minS = 11;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFF6E9A48;
        out.add(d);
    }

    /** A palm from above: a star of fronds. */
    static void palm(F f, float u, float v, float rad) {
        for (int k = 0; k < 7; k++) {
            double a = k * 2 * Math.PI / 7 + 0.3;
            f.line(0x38000000, 1.8f, u + 3, v + 4, u + 3 + (float) Math.cos(a) * rad, v + 4 + (float) Math.sin(a) * rad);
        }
        for (int k = 0; k < 7; k++) {
            double a = k * 2 * Math.PI / 7 + 0.3;
            float eu = u + (float) Math.cos(a) * rad, ev = v + (float) Math.sin(a) * rad;
            f.line(0xFF3E7A3A, 2, u, v, eu, ev);
            f.line(0xFF6AA858, 0.6f, u, v, eu, ev);
        }
        f.circle(0xFF7A6040, u, v, 1.4f);
    }

    // ------------------------------------------------------------------ 21. Penthouse Tower

    static void penthouseTower(List<Landmarks.Def> out) {
        // A slender luxury tower: an infinity pool and sun deck on the roof, a glass penthouse, a palm-lined forecourt with a drop-off loop.
        D d = new D("Penthouse Tower", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                int tv = Math.max(5, Math.min(7, S - 5)), tu = tv + 2;
                return new int[][]{B((L - tu) / 2, 2, tu, tv, 32)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                float tu0 = q[0][0] * T, tu1 = tu0 + q[0][2] * T, tv1 = (q[0][1] + q[0][3]) * T;
                f.rect(0xFF4A4C50, 0, 0, L, S);
                for (float u = 0; u < L; u += 8) f.line(0x18FFFFFF, 0.5f, u, 0, u, S);
                for (float v = 0; v < S; v += 8) f.line(0x10FFFFFF, 0.5f, 0, v, L, v);
                // Lawns and palms down the sides.
                f.rect(0xFF6E9A52, 0, 0, tu0 - 10, S);
                f.rect(0xFF6E9A52, tu1 + 10, 0, L, S);
                for (float v = 10; v < S - 4; v += 22) {
                    if (tu0 - 10 > 12) palm(f, (tu0 - 10) / 2, v, 7);
                    if (L - tu1 - 10 > 12) palm(f, (L + tu1 + 10) / 2, v + 11, 7);
                }
                // Reflecting pools with fountain jets along the tower sides.
                for (int s = 0; s < 2; s++) {
                    float a = s == 0 ? tu0 - 9 : tu1 + 3, b = a + 6;
                    f.rect(0xFF8A8C90, a - 1, 6, b + 1, tv1);
                    f.rect(0xFF2E6A8E, a, 7, b, tv1 - 1);
                    for (float v = 12; v < tv1 - 4; v += 8) f.circle(0xC0FFFFFF, (a + b) / 2, v, 1);
                }
                // The drop-off loop round a round fountain before the door.
                float cu = L / 2, cv = (tv1 + S) / 2 + 2, rr = Math.min((S - tv1) / 2 - 3, 26);
                if (rr > 10) {
                    f.circle(0xFF6A6C70, cu, cv, rr);
                    f.circle(0xFFC8C0B0, cu, cv, rr - 9);
                    f.circle(0xFF8A8C90, cu, cv, rr - 11);
                    f.circle(0xFF3E7AA0, cu, cv, rr - 12);
                    f.circle(0xFF6AAAD0, cu, cv, (rr - 12) * 0.5f);
                    f.circle(0xD0FFFFFF, cu, cv, 1.4f);
                    f.rect(0xFF6A6C70, cu - 6, cv + rr - 4, cu + 6, S);
                    car(f, cu + rr - 4.5f, cv, 90, 0xFF1A1A1E);
                }
                // The entrance canopy.
                f.rect(0x40000000, cu - 10 + 2, tv1 + 2, cu + 10 + 2, tv1 + 12 + 2);
                f.rect(0xFFC8B070, cu - 10, tv1, cu + 10, tv1 + 12);
                f.rect(0xFFE0CC90, cu - 10, tv1, cu + 10, tv1 + 2);
                f.lamp(cu - 12, tv1 + 14);
                f.lamp(cu + 12, tv1 + 14);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                // A glass parapet round a teak deck; the infinity pool runs the length of one side.
                f.rect(0xFF2A3440, 0, 0, L, S);
                f.rect(0xFF6A8CA4, 0.6f, 0.6f, L - 0.6f, S - 0.6f);
                f.rect(0xFFB08A60, 2, 2, L - 2, S - 2);
                for (float v = 3.5f; v < S - 2; v += 2) f.line(0x22000000, 0.4f, 2, v, L - 2, v);
                float pv0 = S * 0.62f, pv1 = S - 2;
                f.rect(0xFF8EC8DA, 3, pv0 - 1, L - 2, pv1);
                f.rect(0xFF2E8AB8, 4, pv0, L - 2, pv1 - 1.2f);
                f.rect(0xFF4AAAD4, 4, pv0, L - 2, pv0 + 2);
                f.line(0xFF1E5A7A, 1.2f, 4, pv1 - 0.6f, L - 2, pv1 - 0.6f);
                for (int k = 0; k < 3; k++) f.line(0x60FFFFFF, 0.5f, 8 + k * 14, pv0 + 4, 14 + k * 14, pv0 + 4);
                // Loungers along the pool, two parasols.
                for (float u = 6; u + 3 < L * 0.6f; u += 6) {
                    f.rect(0x30000000, u + 0.8f, pv0 - 9 + 0.8f, u + 3.8f, pv0 - 2.5f + 0.8f);
                    f.rect(0xFFF2F0EA, u, pv0 - 9, u + 3, pv0 - 2.5f);
                    f.rect(0xFFD8D4CC, u, pv0 - 9, u + 3, pv0 - 7.5f);
                }
                for (int k = 0; k < 2; k++) {
                    float u = 9 + k * 12, v = pv0 - 13;
                    f.circle(0x40000000, u + 1.5f, v + 2, 4);
                    for (int j = 0; j < 8; j++) {
                        double a0 = j * Math.PI / 4, a1 = (j + 1) * Math.PI / 4;
                        f.poly(j % 2 == 0 ? 0xFFF0ECE0 : 0xFFD8D0BC, u, v, u + (float) Math.cos(a0) * 4, v + (float) Math.sin(a0) * 4, u + (float) Math.cos(a1) * 4, v + (float) Math.sin(a1) * 4);
                    }
                }
                // The penthouse: a dark glass pavilion with a green roof, and a hot tub beside it.
                float hu0 = L * 0.64f, hu1 = L - 3, hv0 = 3, hv1 = pv0 - 3;
                f.rect(0x50000000, hu0 + 1.5f, hv0 + 2, hu1 + 1.5f, hv1 + 2);
                f.rect(0xFF1E2A36, hu0, hv0, hu1, hv1);
                f.rect(0xFF6E9A52, hu0 + 1.5f, hv0 + 1.5f, hu1 - 1.5f, hv1 - 1.5f);
                f.specks(r, 12, 0xFF5A8A3E, 0xFF8AB060, hu0 + 2, hv0 + 2, hu1 - 2, hv1 - 2, 1);
                f.circle(0xFFB0B4B8, hu0 - 6, hv0 + 6, 4);
                f.circle(0xFF5AC0D8, hu0 - 6, hv0 + 6, 3);
                f.bush(4.5f, 4.5f, 2.6f);
                f.bush(L * 0.55f, 4.5f, 2.6f);
                f.sign(city, "THE PINNACLE", L * 0.35f, S * 0.35f, 5, 0xFF2A2A2A);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.MID;
        d.minS = 10;
        d.minL = 10;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFFB08A60;
        out.add(d);
    }

    /** A bike shed: a curved canopy over a row of parked bikes. */
    static void bikeShed(F f, float u0, float v0, float u1, float v1) {
        f.rect(0x40000000, u0 + 1.5f, v0 + 2, u1 + 1.5f, v1 + 2);
        for (float u = u0 + 2; u < u1 - 1; u += 3) {
            f.line(0xFF2A2A2C, 0.6f, u, v0 + 1, u, v1 - 1);
            f.circle(0xFF2A2A2C, u, v0 + 1.6f, 0.9f);
            f.circle(0xFF2A2A2C, u, v1 - 1.6f, 0.9f);
        }
        f.rect(0xA0B8C8D0, u0, v0, u1, v1);
        f.rect(0xC0D8E4EA, u0, v0, u1, v0 + (v1 - v0) * 0.35f);
        f.frame(0xFF5A6066, 0.6f, u0, v0, u1, v1);
    }

    // ------------------------------------------------------------------ 22. Student Halls

    static void studentHalls(List<Landmarks.Def> out) {
        // Four blocks with bright stair towers round a courtyard lawn, bike sheds in the corners, a ping-pong table and picnic benches.
        D d = new D("Student Halls", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                int dd = S >= 12 ? 3 : 2, w = L >= 16 ? 3 : 2;
                int wh = S - 2 * dd - 4;
                return new int[][]{B(1, 1, L - 2, dd, 5), B(1, S - 1 - dd, L - 2, dd, 5), B(1, dd + 2, w, wh, 4), B(L - 1 - w, dd + 2, w, wh, 4)};
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                f.rect(0xFFCAC4B8, 0, 0, L, S);
                float c0 = (q[2][0] + q[2][2]) * T + 4, c1 = q[3][0] * T - 4, v0 = (q[0][1] + q[0][3]) * T + 4, v1 = q[1][1] * T - 4;
                // The lawn with diagonal paths.
                f.rect(0xFF74A458, c0, v0, c1, v1);
                f.line(0xFFD8D0C0, 4, c0, v0, c1, v1);
                f.line(0xFFD8D0C0, 4, c0, v1, c1, v0);
                float cu = (c0 + c1) / 2, cv = (v0 + v1) / 2;
                f.circle(0xFFD8D0C0, cu, cv, Math.min(10, (v1 - v0) * 0.3f));
                f.tree(cu, cv, Math.min(8, (v1 - v0) * 0.24f), 0xFF4E8A44);
                // Ping-pong table and picnic benches either side.
                float tu = c0 + (c1 - c0) * 0.25f;
                f.rect(0x40000000, tu - 5 + 1, cv - 3 + 1.4f, tu + 5 + 1, cv + 3 + 1.4f);
                f.rect(0xFF2E6A4A, tu - 5, cv - 3, tu + 5, cv + 3);
                f.line(0xFFF0F0F0, 0.4f, tu - 5, cv, tu + 5, cv);
                f.line(0xFFE0E0E0, 0.8f, tu, cv - 3.6f, tu, cv + 3.6f);
                float pu = c0 + (c1 - c0) * 0.75f;
                for (int k = -1; k <= 1; k += 2) {
                    float v = cv + k * 2;
                    f.rect(0x40000000, pu - 5 + 1, v - 2.5f + 1.2f, pu + 5 + 1, v + 2.5f + 1.2f);
                    f.rect(0xFF9A7A50, pu - 4, v - 2.5f, pu + 4, v - 1.6f);
                    f.rect(0xFFB08E60, pu - 5, v - 1.2f, pu + 5, v + 1.2f);
                    f.rect(0xFF9A7A50, pu - 4, v + 1.6f, pu + 4, v + 2.5f);
                }
                // Bike sheds in two corners, bins in another.
                float bw = Math.min(30, (c1 - c0) * 0.3f);
                bikeShed(f, c0 + 2, v0 + 2, c0 + 2 + bw, v0 + 9);
                bikeShed(f, c1 - 2 - bw, v1 - 9, c1 - 2, v1 - 2);
                for (int k = 0; k < 3; k++) {
                    int col = k == 0 ? 0xFF3A6A3A : k == 1 ? 0xFF2E4A7A : 0xFF4A4A4C;
                    f.rect(0x40000000, c1 - 18 + k * 5 + 0.8f, v0 + 2 + 1, c1 - 14 + k * 5 + 0.8f, v0 + 6 + 1);
                    f.rect(col, c1 - 18 + k * 5, v0 + 2, c1 - 14 + k * 5, v0 + 6);
                }
                for (float v = v0 + 8; v < v1 - 4; v += 20) f.lamp(c0 + 1, v);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                int[] acc = {0xFFE07A28, 0xFF2AA0A0, 0xFF8A5AB0, 0xFF9AC040};
                int ac = acc[part % 4];
                // A grey flat roof with a coloured stair tower, plant, and gravel.
                flatRoof(f.c, f.p, f.x(0, 0), f.y(0, 0), f.x(L, S), f.y(L, S), 0xFFA4A6A8);
                f.specks(r, (int) (L * S / 40), 0x1A000000, 0x1AFFFFFF, 2, 2, L - 2, S - 2, 0.8f);
                float su = L * 0.5f, sw = Math.min(8, S * 0.7f);
                f.rect(0x50000000, su - sw / 2 + 1.5f, S / 2 - sw / 2 + 2, su + sw / 2 + 1.5f, S / 2 + sw / 2 + 2);
                f.rect(City.darken(ac, 0.75f), su - sw / 2, S / 2 - sw / 2, su + sw / 2, S / 2 + sw / 2);
                f.rect(ac, su - sw / 2 + 0.8f, S / 2 - sw / 2 + 0.8f, su + sw / 2 - 0.8f, S / 2 + sw / 2 - 0.8f);
                if (L > 40) {
                    fanUnit(f, L * 0.18f, S / 2, Math.min(3, S * 0.25f));
                    fanUnit(f, L * 0.82f, S / 2, Math.min(3, S * 0.25f));
                    for (float u = L * 0.25f; u < L * 0.42f; u += 4) f.rect(0xFFD8DADC, u, S * 0.3f, u + 3, S * 0.7f);
                }
                if (part == 0) f.sign(city, "STUDENT HALLS", L / 2, S / 2, 5, 0xFF2A2A2A);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.UNI | Variants.MID;
        d.minS = 10;
        d.minL = 13;
        d.base = City.APARTMENT;
        d.inside = Variants.I_DORM;
        d.roofCol = 0xFFA4A6A8;
        out.add(d);
    }

    // ------------------------------------------------------------------ 23. Retirement Village

    static void retirementVillage(List<Landmarks.Def> out) {
        // A row of tidy bungalows, a community hall with a porch, and a manicured bowling green, with flower beds and benches.
        D d = new D("Retirement Village", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                int n = Math.max(2, (L - 1) / 4);
                int sp = (L - 1) / n;
                int[][] a = new int[n + 1][];
                for (int i = 0; i < n; i++) a[i] = B(1 + i * sp, 1, sp - 1, 2, 1);
                int hl = Math.max(4, Math.min(6, L / 3));
                a[n] = B(1, S - 4, hl, 3, 1);
                return a;
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                int n = q.length - 1;
                f.rect(0xFF7AA45A, 0, 0, L, S);
                float he = (q[n][0] + q[n][2]) * T, hv = q[n][1] * T;
                // A paved walk in front of the bungalows, front paths, flower borders.
                float wv = 3 * T + 6;
                f.rect(0xFFD8D0BC, 0, wv - 3, L, wv + 3);
                for (int i = 0; i < n; i++) {
                    float u0 = q[i][0] * T, u1 = u0 + q[i][2] * T, u = (u0 + u1) / 2;
                    f.rect(0xFFD8D0BC, u - 2, 3 * T, u + 2, wv);
                    for (float t = u0 + 2; t < u1 - 2; t += 2.6f)
                        if (Math.abs(t - u) > 4) {
                            f.circle(0xFF4E7A3A, t, 3 * T + 2.5f, 1.3f);
                            int fc = ((int) t * 7) % 3 == 0 ? 0xFFE05A7A : ((int) t * 7) % 3 == 1 ? 0xFFF0D040 : 0xFFB070D0;
                            f.circle(fc, t, 3 * T + 2.2f, 0.7f);
                        }
                }
                // The bowling green: a perfect square of striped turf in its ditch and bank.
                float gu0 = he + 12, gu1 = L - 6, gv0 = wv + 10, gv1 = S - 6;
                float side = Math.min(gu1 - gu0, gv1 - gv0);
                float gcu = (gu0 + gu1) / 2, gcv = (gv0 + gv1) / 2;
                float a0 = gcu - side / 2, a1 = gcu + side / 2, b0 = gcv - side / 2, b1 = gcv + side / 2;
                if (side > 30) {
                    f.rect(0xFFB8B0A0, a0 - 4, b0 - 4, a1 + 4, b1 + 4);
                    f.rect(0xFF9A8E7A, a0 - 2, b0 - 2, a1 + 2, b1 + 2);
                    f.rect(0xFF5E9E48, a0, b0, a1, b1);
                    float st = side / 6;
                    for (int k = 0; k < 6; k += 2) f.rect(0xFF68AA52, a0 + k * st, b0, a0 + (k + 1) * st, b1);
                    for (int k = 1; k < 6; k++) f.circle(0xFFF0F0F0, a0 + k * st, b0 - 1, 0.5f);
                    for (int k = 0; k < 6; k++) {
                        float u = a0 + st * 1.5f + r.nextFloat() * st * 3, v = b0 + side * 0.3f + r.nextFloat() * side * 0.4f;
                        f.circle(0x40000000, u + 0.5f, v + 0.6f, 1);
                        f.circle(k % 2 == 0 ? 0xFF2A2A2E : 0xFF6A2A2A, u, v, 1);
                    }
                    f.circle(0xFFF0F0F0, gcu, b0 + side * 0.25f, 0.6f);
                    f.rect(0xFFF0EEE6, a0 + 2, b1 - 3, a0 + 4, b1 - 1);
                    f.bench(a0 - 8, gcv, false);
                    f.bench(gcu, b1 + 7, true);
                }
                // Gardens between: a pergola with roses, a sundial in a rose bed, trees.
                float mu = he / 2, mv = (wv + hv) / 2;
                if (hv - wv > 24) {
                    f.circle(0xFFD8D0BC, mu, mv, 9);
                    f.circle(0xFF5A8A44, mu, mv, 6.5f);
                    for (int k = 0; k < 10; k++) {
                        double a = k * Math.PI / 5;
                        f.circle(0xFFD04A5A, mu + (float) Math.cos(a) * 5, mv + (float) Math.sin(a) * 5, 0.9f);
                    }
                    f.circle(0xFFC8C0AC, mu, mv, 2);
                    f.line(0xFF5A5A5A, 0.5f, mu, mv, mu + 1.5f, mv - 1.5f);
                }
                f.rect(0xFFD8D0BC, (q[n][0] + q[n][2] / 2f) * T - 3, wv, (q[n][0] + q[n][2] / 2f) * T + 3, hv);
                f.tree(4, wv + 12, 5, 0xFF4E8A44);
                f.tree(L - 6, wv + 6, 4, 0xFF8AA048);
                f.lamp(he + 4, hv - 4);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                boolean hall = S > 40;
                int col = hall ? 0xFF6E7A5A : (part % 2 == 0 ? 0xFFA8583E : 0xFF9A6A4A);
                // A hipped tile roof (the hall's in green slate, with a clock and a porch gable).
                hip(f, col, 0, 0, L, S, true);
                if (hall) {
                    f.rect(0xFFE8E0CC, L / 2 - 5, S - 4, L / 2 + 5, S);
                    f.poly(City.lighten(col, 0.1f), L / 2 - 5, S - 4, L / 2, S - 4, L / 2, S);
                    f.circle(0xFFF4F0E4, L / 2, S * 0.3f, 2.2f);
                    f.sign(city, "COMMUNITY HALL", L / 2, S / 2, 4, 0xFFF0E8D0);
                } else {
                    chimney(f, L * 0.7f, S * 0.3f, 1);
                }
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.SUB | Variants.TOWN;
        d.minS = 10;
        d.minL = 13;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFFA8583E;
        out.add(d);
    }

    /** A rope bridge from (u0,v0) to (u1,v1): planks between two ropes, its shadow far below. */
    static void ropeBridge(F f, float u0, float v0, float u1, float v1) {
        float du = u1 - u0, dv = v1 - v0, len = (float) Math.sqrt(du * du + dv * dv);
        if (len < 1) return;
        float nu = -dv / len * 2, nv = du / len * 2;
        f.line(0x30000000, 4, u0 + 6, v0 + 8, u1 + 6, v1 + 8);
        int k = (int) (len / 2.2f);
        for (int i = 0; i <= k; i++) {
            float t = i / (float) k, u = u0 + du * t, v = v0 + dv * t;
            f.line(0xFF9A7A50, 1.2f, u - nu, v - nv, u + nu, v + nv);
        }
        f.line(0xFFC8B080, 0.5f, u0 - nu, v0 - nv, u1 - nu, v1 - nv);
        f.line(0xFFC8B080, 0.5f, u0 + nu, v0 + nv, u1 + nu, v1 + nv);
    }

    // ------------------------------------------------------------------ 24. Treehouse Hotel

    static void treehouseHotel(List<Landmarks.Def> out) {
        // Timber cabins built up into big old trees, linked by rope bridges, a lodge, a fire pit in a clearing.
        D d = new D("Treehouse Hotel", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                java.util.ArrayList<int[]> a = new java.util.ArrayList<int[]>();
                a.add(B(1, S - 4, 4, 3, 1));
                int k = 0;
                for (int u = 2; u + 2 <= L - 1; u += 4, k++) {
                    a.add(B(u, 1 + (k % 2), 2, 2, 2));
                    int v2 = S / 2 + 1;
                    if (u > 6 && v2 + 2 <= S - 2) a.add(B(u, v2, 2, 2, 2));
                }
                return a.toArray(new int[a.size()][]);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                f.rect(0xFF4E6E3A, 0, 0, L, S);
                for (int k = 0; k < 60; k++)
                    f.circle(r.nextInt(3) == 0 ? 0x228A6A3A : r.nextBoolean() ? 0x1A000000 : 0x146AA040, r.nextFloat() * L, r.nextFloat() * S, 2 + r.nextFloat() * 4);
                // A mulch trail from the lodge.
                float lu = (q[0][0] + q[0][2]) * T, lv = q[0][1] * T + 20;
                f.curve(0xFF8A6A48, 5, lu, lv, L * 0.5f, S * 0.9f, L - 4, S * 0.55f);
                // The fire pit clearing.
                float fu = (lu + L) / 2, fv = S - 18;
                if (fv > S / 2 + 2 * T && fu + 14 < L) {
                    f.circle(0xFF9A8460, fu, fv, 12);
                    f.circle(0xFF6A6460, fu, fv, 4);
                    f.circle(0xFF2A2420, fu, fv, 3);
                    f.circle(0xFFE07A28, fu, fv, 1.6f);
                    for (int k = 0; k < 4; k++) {
                        double a = k * Math.PI / 2 + 0.4;
                        float bu = fu + (float) Math.cos(a) * 8, bv = fv + (float) Math.sin(a) * 8;
                        f.roundRect(0xFF6A4A30, bu - 3, bv - 1.2f, bu + 3, bv + 1.2f, 1.2f);
                    }
                }
                // The trees each cabin is built in: a great canopy spreading past the deck, and its deck.
                for (int i = 1; i < q.length; i++) {
                    float cu = (q[i][0] + 1) * T, cv = (q[i][1] + 1) * T;
                    f.tree(cu - 4, cv - 3, 22, i % 3 == 0 ? 0xFF3A6A34 : 0xFF2E5E30);
                }
                for (int i = 1; i < q.length; i++) {
                    float u0 = q[i][0] * T - 3, v0 = q[i][1] * T - 3;
                    f.rect(0x40000000, u0 + 4, v0 + 5, u0 + 38 + 4, v0 + 38 + 5);
                    f.rect(0xFF8A6A44, u0, v0, u0 + 38, v0 + 38);
                    for (float u = u0 + 2; u < u0 + 38; u += 2.4f) f.line(0x26000000, 0.4f, u, v0, u, v0 + 38);
                    f.frame(0xFF5A4028, 0.8f, u0, v0, u0 + 38, v0 + 38);
                }
                // Rope bridges between neighbouring decks.
                for (int i = 1; i < q.length; i++)
                    for (int j = i + 1; j < q.length; j++) {
                        float ui = (q[i][0] + 1) * T, vi = (q[i][1] + 1) * T, uj = (q[j][0] + 1) * T, vj = (q[j][1] + 1) * T;
                        float dd = Math.abs(ui - uj) + Math.abs(vi - vj);
                        if (dd > 6.5f * T) continue;
                        float du = uj - ui, dv = vj - vi;
                        float s = Math.max(Math.abs(du), Math.abs(dv));
                        float e = 19 / s;
                        ropeBridge(f, ui + du * e, vi + dv * e, uj - du * e, vj - dv * e);
                    }
                // Other trees and ferns round the edges.
                for (int k = 0; k < 10; k++) {
                    float u = r.nextFloat() * L, v = r.nextFloat() * S;
                    boolean clear = true;
                    for (int i = 0; i < q.length; i++)
                        if (u > q[i][0] * T - 8 && u < (q[i][0] + q[i][2]) * T + 8 && v > q[i][1] * T - 8 && v < (q[i][1] + q[i][3]) * T + 8) clear = false;
                    if (clear) f.tree(u, v, 7 + r.nextFloat() * 5, 0xFF2E5A2E);
                }
                for (int k = 0; k < 12; k++) f.bush(r.nextFloat() * L, r.nextFloat() * S, 2);
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                float L = f.L, S = f.S;
                if (part == 0) {
                    // The lodge: a big shingled hip with a stone chimney and a turf patch.
                    hip(f, 0xFF7A5A3A, 0, 0, L, S, true);
                    f.rect(0x48000000, L * 0.7f + 1, S * 0.2f + 1.5f, L * 0.7f + 6, S * 0.2f + 6.5f);
                    f.rect(0xFF8A8478, L * 0.7f, S * 0.2f, L * 0.7f + 5, S * 0.2f + 5);
                    f.circle(0xFF2A2420, L * 0.7f + 2.5f, S * 0.2f + 2.5f, 1.2f);
                    f.sign(city, "TREEHOUSE HOTEL", L / 2, S / 2, 5, 0xFFF0E0C0);
                    return;
                }
                // A cabin: a cedar-shingle pyramid roof, a stovepipe, and leaves of its tree falling over a corner.
                hip(f, part % 2 == 0 ? 0xFF8A5E3A : 0xFF6E5A3E, 0, 0, L, S, true);
                f.circle(0xFF3A3A3A, L * 0.3f, S * 0.65f, 1.2f);
                f.c.save();
                f.c.clipRect(f.x(0, 0), f.y(0, 0), f.x(L, S), f.y(L, S));
                float cu = part % 2 == 0 ? L : 0, cv = part % 3 == 0 ? 0 : S;
                f.circle(0x40000000, cu + 2, cv + 3, L * 0.4f);
                f.circle(0xFF2E5E30, cu, cv, L * 0.38f);
                f.circle(0xFF3E7A3A, cu - 2, cv - 2, L * 0.28f);
                f.circle(0xFF5A9A4A, cu - 4, cv - 4, L * 0.12f);
                f.c.restore();
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.PARK | Variants.SUB;
        d.minS = 10;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOTEL;
        d.roofCol = 0xFF8A5E3A;
        out.add(d);
    }

    // ------------------------------------------------------------------ 25. Brownstone Row

    static void brownstoneRow(List<Landmarks.Def> out) {
        // Rows of brownstone townhouses with high stoops to the street and long fenced back gardens behind.
        D d = new D("Brownstone Row", T_BUILDING) {
            @Override
            int[][] lp(int L, int S) {
                java.util.ArrayList<int[]> a = new java.util.ArrayList<int[]>();
                boolean two = S >= 13;
                int dep = 3;
                int n = Math.max(1, (L - 2 + 1) / 7);
                int w = (L - 2 - (n - 1)) / n;
                for (int i = 0; i < n; i++) {
                    int u = 1 + i * (w + 1);
                    a.add(B(u, S - 1 - dep, w, dep, 4));
                    if (two) a.add(B(u, 1, w, dep, 4));
                }
                return a.toArray(new int[a.size()][]);
            }

            @Override
            void g(City city, F f, int[][] q, Random r) {
                float L = f.L, S = f.S, T = TT;
                boolean two = Math.round(S / T) >= 13;
                f.rect(0xFF6E9450, 0, 0, L, S);
                int[] gc = {0xFF6E9A50, 0xFFB8A890, 0xFF7AA45A, 0xFFA89880, 0xFF6A9048};
                // Back gardens: a strip behind each house, a fence on every party line, a tree or a patio in each.
                for (int[] b : q) {
                    boolean front = b[1] > 1;
                    float u0 = b[0] * T, u1 = u0 + b[2] * T;
                    float ga = front ? (two ? S / 2 : 0) : b[1] * T + b[3] * T, gb = front ? b[1] * T : S / 2;
                    int houses = Math.max(1, b[2]);
                    float hw = (u1 - u0) / houses;
                    for (int k = 0; k < houses; k++) {
                        float a = u0 + k * hw, e = a + hw;
                        int c = gc[(int) ((a * 7 + ga) / 16) % gc.length];
                        f.rect(c, a, ga, e, gb);
                        if (c == 0xFFB8A890 || c == 0xFFA89880)
                            for (float v = ga + 3; v < gb; v += 3) f.line(0x18000000, 0.4f, a, v, e, v);
                        float tv = front ? ga + (gb - ga) * 0.4f : gb - (gb - ga) * 0.4f;
                        if ((k + b[0]) % 3 == 0 && gb - ga > 20) f.tree((a + e) / 2, tv, Math.min(6, hw * 0.4f), 0xFF3E7A3A);
                        else if ((k + b[0]) % 3 == 1 && gb - ga > 14) {
                            f.rect(0x40000000, a + 3 + 1, tv - 3 + 1.2f, a + 9 + 1, tv + 3 + 1.2f);
                            f.rect(0xFF8A6A48, a + 3, tv - 3, a + 9, tv + 3);
                        }
                        f.line(0xFF8A6A48, 0.8f, e, ga, e, gb);
                    }
                    f.line(0xFF8A6A48, 0.8f, u0, ga, u0, gb);
                    f.line(0xFF8A6A48, 0.8f, u0, front ? ga : gb, u1, front ? ga : gb);
                }
                // Stoops: a flight of brownstone steps out of each front door, railings, bins and a street tree pit.
                for (int[] b : q) {
                    boolean front = b[1] > 1;
                    float u0 = b[0] * T, u1 = u0 + b[2] * T;
                    float fv = front ? (b[1] + b[3]) * T : b[1] * T;
                    float sg = front ? 1 : -1;
                    f.rect(0xFFB8B0A4, u0, front ? fv : 0, u1, front ? S : fv);
                    int houses = Math.max(1, b[2]);
                    float hw = (u1 - u0) / houses;
                    for (int k = 0; k < houses; k++) {
                        float a = u0 + k * hw, su = a + hw * 0.62f;
                        for (int j = 0; j < 4; j++) {
                            float v = fv + sg * j * 2;
                            f.rect(j % 2 == 0 ? 0xFF7A4A34 : 0xFF6A3E2C, su - 2.5f, Math.min(v, v + sg * 2), su + 2.5f, Math.max(v, v + sg * 2));
                        }
                        f.line(0xFF1A1A1A, 0.5f, su - 2.8f, fv, su - 2.8f, fv + sg * 8);
                        f.line(0xFF1A1A1A, 0.5f, su + 2.8f, fv, su + 2.8f, fv + sg * 8);
                        f.line(0xFF1A1A1A, 0.5f, a + 1, fv + sg * 5, su - 3.5f, fv + sg * 5);
                        if (k % 2 == 0) {
                            f.circle(0xFF3A5A3A, a + 3, fv + sg * 2.5f, 1.3f);
                            f.circle(0xFF4A4A4C, a + 5.5f, fv + sg * 2.5f, 1.3f);
                        }
                    }
                }
            }

            @Override
            void rf(City city, F f, int part, Random r) {
                // Flat tar roofs house by house, a heavy cornice on both faces, hatches, chimneys and the odd water tank.
                float L = f.L, S = f.S;
                int houses = Math.max(1, Math.round(L / TT));
                float hw = L / houses;
                int[] tc = {0xFF4A4644, 0xFF5A524C, 0xFF3E3C3C, 0xFF56504A};
                for (int k = 0; k < houses; k++) {
                    float a = k * hw, e = a + hw;
                    f.rect(0xFF6A3E2C, a, 0, e, S);
                    f.rect(tc[(k + part) % 4], a + 0.8f, 3, e - 0.8f, S - 3);
                    f.specks(r, 6, 0x22000000, 0x18FFFFFF, a + 1, 3, e - 1, S - 3, 0.8f);
                    f.rect(0xFFA89E8C, a + hw * 0.3f, S * 0.4f, a + hw * 0.55f, S * 0.4f + 3);
                    f.frame(0xFF6A6458, 0.4f, a + hw * 0.3f, S * 0.4f, a + hw * 0.55f, S * 0.4f + 3);
                    chimney(f, e - 4.5f, S * 0.65f, 1);
                    if ((k + part) % 4 == 1) {
                        f.circle(0x48000000, a + hw / 2 + 2, S * 0.24f + 3, 4);
                        f.circle(0xFF6A4A30, a + hw / 2, S * 0.24f, 4);
                        f.circle(0xFF8A6440, a + hw / 2 - 0.5f, S * 0.24f - 0.5f, 3);
                        f.poly(0xFF5A3A24, a + hw / 2 - 4, S * 0.24f, a + hw / 2, S * 0.24f - 4, a + hw / 2 + 4, S * 0.24f);
                    }
                    if (k > 0) f.rect(0xFF7A4A34, a - 0.6f, 0, a + 0.6f, S);
                }
                f.rect(0xFF3E2A20, 0, 0, L, 2.4f);
                f.rect(0xFF3E2A20, 0, S - 2.4f, L, S);
                for (float u = 1; u < L; u += 2) {
                    f.rect(0xFF5A3E2E, u, 0.6f, u + 1, 1.8f);
                    f.rect(0xFF5A3E2E, u, S - 1.8f, u + 1, S - 0.6f);
                }
                if (part == 0) f.sign(city, "BROWNSTONES", L / 2, S / 2, 5, 0xFFF0E0C0);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.OLD | Variants.MID;
        d.minS = 10;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFF4A4644;
        out.add(d);
    }
}
