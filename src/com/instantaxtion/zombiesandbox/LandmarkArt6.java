package com.instantaxtion.zombiesandbox;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.Landmarks.*;

/** ((22.6) Landmark designs, part 6 (see Landmarks): shops, food, hospitality and nightlife. */
final class LandmarkArt6 {
    private LandmarkArt6() {
    }

    static void register(List<Landmarks.Def> out) {
        foodHall(out);
        cinema(out);
        carShowroom(out);
        gardenCentre(out);
        hardware(out);
        motel(out);
        boutique(out);
        chapel(out);
        bakery(out);
        trattoria(out);
        sushi(out);
        beerHall(out);
        wineBar(out);
        nightclub(out);
        casino(out);
        arcade(out);
        recordRow(out);
        flowerMarket(out);
        antiques(out);
        furniture(out);
        petStore(out);
        burger(out);
        pizza(out);
        teaRoom(out);
        chocolate(out);
        // @@REG@@
    }

    // ------------------------------------------------------------------ shared bits

    static final float T = City.T;

    static final int[] CARS = {0xFFB83A30, 0xFF3A5A8A, 0xFFD8D4C8, 0xFF2A2C30, 0xFF7A8A90, 0xFF5A7A4A, 0xFFE0C060, 0xFF8A4A2A};

    static final int[] BRIGHT = {0xFFD8443A, 0xFFE8B83A, 0xFF3A7AC8, 0xFF4AA65A, 0xFFE07A30, 0xFFD060A0, 0xFF6A5AC0, 0xFF3AB0B0};

    static final int[] FLOWERS = {0xFFE04A5A, 0xFFF0C040, 0xFFF4F0F0, 0xFFE07AB0, 0xFF9A5AD0, 0xFFF08A3A, 0xFFD02A3A, 0xFFF8E0A0};

    /** A plan written with u along the block's long side ({u, v, lu, lv, floors}), turned (transposed) for a tall block. */
    static int[][] orient(int[][] a, boolean along) {
        if (along) return a;
        int[][] o = new int[a.length][];
        for (int i = 0; i < a.length; i++) o[i] = new int[]{a[i][1], a[i][0], a[i][3], a[i][2], a[i][4]};
        return o;
    }

    static int lt(F f) {
        return Math.round(f.L / T);
    }

    static int st(F f) {
        return Math.round(f.S / T);
    }

    /** A planned building's footprint in world units in the block's frame: {u0, v0, u1, v1}. */
    static float[] box(int[] q) {
        return new float[]{q[0] * T, q[1] * T, (q[0] + q[2]) * T, (q[1] + q[3]) * T};
    }

    /** Drawing in a rectangle's own frame: u along its long side, v across; a tall rectangle is drawn transposed. */
    static final class F {
        final Canvas c;
        final Paint p;
        final float ox, oy, L, S;
        final boolean t;

        F(Canvas c, Paint p, float x0, float y0, float x1, float y1) {
            this(c, p, x0, y0, x1, y1, y1 - y0 > x1 - x0);
        }

        /** A frame with the transposition given (false keeps u across the screen, for upright pictures). */
        F(Canvas c, Paint p, float x0, float y0, float x1, float y1, boolean t) {
            this.c = c;
            this.p = p;
            ox = x0;
            oy = y0;
            this.t = t;
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

        void sh(float u0, float v0, float u1, float v1, float d) {
            rect(0x38000000, u0 + d, v0 + d, u1 + d, v1 + d);
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

        /** An outline rounded rectangle (neon tube, kerb). */
        void rring(int col, float w, float u0, float v0, float u1, float v1, float rad) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(w);
            p.setColor(col);
            RF.set(x(u0, v0), y(u0, v0), x(u1, v1), y(u1, v1));
            c.drawRoundRect(RF, rad, rad, p);
            p.setStyle(Paint.Style.FILL);
        }

        /** A neon tube: a soft glow and the bright core. */
        void neon(int col, float u0, float v0, float u1, float v1, float rad) {
            rring((col & 0x00FFFFFF) | 0x50000000, 2.4f, u0, v0, u1, v1, rad);
            rring(col, 0.8f, u0, v0, u1, v1, rad);
        }

        void neonLine(int col, float u0, float v0, float u1, float v1) {
            line((col & 0x00FFFFFF) | 0x50000000, 2.4f, u0, v0, u1, v1);
            line(col, 0.8f, u0, v0, u1, v1);
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

        /** A parked car, nose towards angle a (in the u, v frame). */
        void car(float u, float v, double a, int col, float len) {
            carXY(c, p, x(u, v), y(u, v), t ? Math.PI / 2 - a : a, col, len);
        }

        void parasol(float u, float v, float r, int c1, int c2) {
            parasolXY(c, p, x(u, v), y(u, v), r, c1, c2);
        }

        void palm(float u, float v, float r) {
            float px = x(u, v), py = y(u, v);
            Landmarks.circle(c, p, 0x30000000, px + r * 0.45f, py + r * 0.55f, r * 0.85f);
            for (int k = 0; k < 9; k++) {
                double a = k * 2 * Math.PI / 9 + 0.3;
                float ex = px + (float) Math.cos(a) * r, ey = py + (float) Math.sin(a) * r;
                Landmarks.line(c, p, k % 2 == 0 ? 0xFF4A8A3A : 0xFF38702E, r * 0.34f, px, py, ex, ey);
                Landmarks.line(c, p, 0x50FFFFFF, 0.3f, px, py, (px + ex) / 2, (py + ey) / 2);
            }
            Landmarks.circle(c, p, 0xFF7A5A34, px, py, r * 0.18f);
        }

        void festoon(float u0, float v0, float u1, float v1, float sag, int n, boolean multi) {
            festoonXY(c, p, x(u0, v0), y(u0, v0), x(u1, v1), y(u1, v1), sag, n, multi);
        }

        void sign(City city, String s, float u, float v, float size, int col) {
            Landmarks.sign(city, s, x(u, v), y(u, v), size, col);
        }
    }

    static void rbox(Canvas c, Paint p, int col, float cx, float cy, float hl, float hw, double ang) {
        float ca = (float) Math.cos(ang), sa = (float) Math.sin(ang);
        poly(c, p, col, cx + ca * hl - sa * hw, cy + sa * hl + ca * hw, cx + ca * hl + sa * hw, cy + sa * hl - ca * hw,
                cx - ca * hl + sa * hw, cy - sa * hl - ca * hw, cx - ca * hl - sa * hw, cy - sa * hl + ca * hw);
    }

    static void carXY(Canvas c, Paint p, float cx, float cy, double ang, int col, float len) {
        float ca = (float) Math.cos(ang), sa = (float) Math.sin(ang);
        rbox(c, p, 0x48000000, cx + 1.2f, cy + 1.2f, len + 0.3f, 2.5f, ang);
        rbox(c, p, City.darken(col, 0.8f), cx, cy, len, 2.3f, ang);
        rbox(c, p, col, cx, cy, len - 0.4f, 1.9f, ang);
        rbox(c, p, 0xFF2A3440, cx + ca * len * 0.3f, cy + sa * len * 0.3f, len * 0.16f, 1.8f, ang);
        rbox(c, p, City.lighten(col, 0.12f), cx - ca * len * 0.08f, cy - sa * len * 0.08f, len * 0.26f, 1.7f, ang);
        rbox(c, p, 0xFF2A3440, cx - ca * len * 0.45f, cy - sa * len * 0.45f, len * 0.08f, 1.6f, ang);
    }

    static void parasolXY(Canvas c, Paint p, float x, float y, float r, int c1, int c2) {
        circle(c, p, 0x38000000, x + 1.5f, y + 1.5f, r);
        circle(c, p, c2, x, y, r);
        Path path = new Path();
        for (int k = 0; k < 8; k += 2) {
            double a0 = k * Math.PI / 4, a1 = a0 + Math.PI / 4;
            path.moveTo(x, y);
            path.lineTo(x + (float) Math.cos(a0) * r, y + (float) Math.sin(a0) * r);
            path.lineTo(x + (float) Math.cos(a1) * r, y + (float) Math.sin(a1) * r);
            path.close();
        }
        p.setStyle(Paint.Style.FILL);
        p.setColor(c1);
        c.drawPath(path, p);
        circle(c, p, 0x28000000, x + r * 0.3f, y + r * 0.3f, r * 0.55f);
        circle(c, p, 0xFFEDE6D6, x, y, r * 0.12f);
    }

    static void festoonXY(Canvas c, Paint p, float ax, float ay, float bx, float by, float sag, int n, boolean multi) {
        Path path = new Path();
        path.moveTo(ax, ay);
        path.quadTo((ax + bx) / 2, (ay + by) / 2 + 2 * sag, bx, by);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(0.35f);
        p.setColor(0x90202020);
        c.drawPath(path, p);
        p.setStyle(Paint.Style.FILL);
        for (int k = 1; k < n; k++) {
            float t = k / (float) n, s = 4 * t * (1 - t) * sag;
            circle(c, p, multi ? BRIGHT[k % BRIGHT.length] : 0xFFFFE9A0, ax + (bx - ax) * t, ay + (by - ay) * t + s, 0.75f);
        }
    }

    /** A striped awning over u0..u1, v0..v1, its stripes across u; drop is +1 if it slopes down towards v1, -1 towards v0. */
    static void awning(F f, float u0, float v0, float u1, float v1, int c1, int c2, float sw, int drop) {
        f.sh(u0, v0, u1, v1, 1.6f);
        f.rect(c1, u0, v0, u1, v1);
        for (float u = u0 + sw; u < u1; u += 2 * sw) f.rect(c2, u, v0, Math.min(u + sw, u1), v1);
        float m = (v0 + v1) / 2;
        if (drop > 0) f.rect(0x24000000, u0, m, u1, v1);
        else f.rect(0x22FFFFFF, u0, v0, u1, m);
        float e = drop > 0 ? v1 : v0;
        for (float u = u0 + sw / 2; u < u1; u += sw) f.circle(k2(c1, c2, u, u0, sw), u, e, sw * 0.42f);
        f.line(0x40000000, 0.4f, u0, e, u1, e);
    }

    private static int k2(int c1, int c2, float u, float u0, float sw) {
        return ((int) ((u - u0) / sw)) % 2 == 0 ? c1 : c2;
    }

    /** A round café table with chairs round it. */
    static void table(F f, float u, float v, float r, int top, int chair, int chairs) {
        for (int k = 0; k < chairs; k++) {
            double a = k * 2 * Math.PI / chairs + (chairs == 2 ? 0 : Math.PI / 4);
            float cu = u + (float) Math.cos(a) * (r + 1.3f), cv = v + (float) Math.sin(a) * (r + 1.3f);
            f.circle(0x30000000, cu + 0.5f, cv + 0.5f, 1.1f);
            f.circle(chair, cu, cv, 1.05f);
        }
        f.circle(0x34000000, u + 0.8f, v + 0.8f, r);
        f.circle(top, u, v, r);
        f.circle(City.lighten(top, 0.18f), u - r * 0.25f, v - r * 0.25f, r * 0.42f);
    }

    /** A rooftop plant unit with its fan. */
    static void unit(F f, float u, float v, float w, float h) {
        f.sh(u, v, u + w, v + h, 1.5f);
        f.rect(0xFF8E9296, u, v, u + w, v + h);
        f.rect(0xFFB4B8BC, u + 0.6f, v + 0.6f, u + w - 0.6f, v + h * 0.45f);
        float m = Math.min(w, h);
        f.circle(0xFF4E5256, u + w / 2, v + h / 2, m * 0.33f);
        f.circle(0xFF7A7E82, u + w / 2, v + h / 2, m * 0.13f);
    }

    static void grid(F f, int col, float w, float u0, float v0, float u1, float v1, float step) {
        for (float u = u0 + step; u < u1 - 0.5f; u += step) f.line(col, w, u, v0, u, v1);
        for (float v = v0 + step; v < v1 - 0.5f; v += step) f.line(col, w, u0, v, u1, v);
    }

    /** A hipped roof, faces shaded for light from the top left. */
    static void hip(F f, int col, float u0, float v0, float u1, float v1, boolean courses) {
        float h = Math.min(u1 - u0, v1 - v0) / 2, au = u0 + h, av = v0 + h, bu = u1 - h, bv = v1 - h;
        f.poly(City.lighten(col, 0.12f), u0, v0, u1, v0, bu, av, au, av);
        f.poly(col, u0, v0, u0, v1, au, bv, au, av);
        f.poly(City.darken(col, 0.82f), u1, v0, u1, v1, bu, bv, bu, av);
        f.poly(City.darken(col, 0.7f), u0, v1, u1, v1, bu, bv, au, bv);
        if (courses) for (float k = 3; k < h - 1; k += 3) f.frame(0x22000000, 0.5f, u0 + k, v0 + k, u1 - k, v1 - k);
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

    /** A terracotta pot with a plant in it. */
    static void pot(F f, float u, float v, float r, int plant) {
        f.circle(0x34000000, u + 0.8f, v + 0.8f, r);
        f.circle(0xFFB0603A, u, v, r);
        f.circle(0xFF5A4030, u, v, r * 0.75f);
        f.circle(plant, u, v, r * 0.62f);
        f.circle(City.lighten(plant, 0.2f), u - r * 0.2f, v - r * 0.2f, r * 0.28f);
    }

    /** A clipped hedge. */
    static void hedge(F f, float u0, float v0, float u1, float v1) {
        f.sh(u0, v0, u1, v1, 1.2f);
        f.roundRect(0xFF3E6E34, u0, v0, u1, v1, 1.2f);
        f.roundRect(0xFF4E8240, u0 + 0.5f, v0 + 0.5f, u1 - 0.8f, (v0 + v1) / 2, 1f);
    }

    /** A wheelie bin. */
    static void bin(F f, float u, float v, int col) {
        f.sh(u - 1.6f, v - 1.6f, u + 1.6f, v + 1.6f, 0.8f);
        f.rect(col, u - 1.6f, v - 1.6f, u + 1.6f, v + 1.6f);
        f.line(0x50000000, 0.5f, u - 1.6f, v - 0.6f, u + 1.6f, v - 0.6f);
    }

    /** Smoke puffs drifting to the lower right. */
    static void smoke(F f, float u, float v, float r, int col) {
        for (int k = 3; k >= 0; k--) f.circle((col & 0x00FFFFFF) | ((0x60 - k * 0x12) << 24), u + k * r * 0.9f, v + k * r * 0.5f, r * (0.7f + k * 0.25f));
    }

    // ------------------------------------------------------------------ 1 Covered Food Hall

    static int[][] hallUV(int L, int S) {
        int lv = Math.max(4, Math.min(S - 4, S * 6 / 10));
        return new int[][]{{1, 1, L - 2, lv, 2}};
    }

    static void foodHall(List<Landmarks.Def> out) {
        // A Victorian iron-and-glass market hall, with tables and food carts on the paved square in front.
        Landmarks.Def d = new Landmarks.Def("Covered Food Hall", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return orient(hallUV(Math.max(bw, bh), Math.min(bw, bh)), bw >= bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] b = box(hallUV(lt(f), st(f))[0]);
                f.rect(0xFFD8CFBE, 0, 0, L, S);
                grid(f, 0x16000000, 0.5f, 0, 0, L, S, 8);
                // The service lane behind: bins, crates and a delivery van.
                f.rect(0xFFB8B2A6, 0, 0, L, b[1] - 3);
                for (float u = 10; u < L - 30; u += 26) {
                    bin(f, u, 7, 0xFF3A6A4A);
                    bin(f, u + 4, 7, 0xFF3A6A4A);
                    f.sh(u + 8, 4.5f, u + 14, 10, 0.8f);
                    f.rect(0xFFA07A4A, u + 8, 4.5f, u + 14, 10);
                    f.frame(0xFF6A4A2A, 0.4f, u + 8, 4.5f, u + 14, 10);
                }
                f.car(L - 16, 7, 0, 0xFFE8E8E4, 6.5f);
                // Glass entrance canopies at the front and both ends.
                float cu = L / 2, fv = b[3];
                f.sh(cu - 16, fv, cu + 16, fv + 7, 1.8f);
                f.rect(0xA0A8CCD8, cu - 16, fv, cu + 16, fv + 7);
                for (float u = cu - 16; u <= cu + 16.1f; u += 8) f.line(0xFF3E4A48, 0.6f, u, fv, u, fv + 7);
                f.frame(0xFF3E4A48, 0.7f, cu - 16, fv, cu + 16, fv + 7);
                float mv = (b[1] + b[3]) / 2;
                f.rect(0xA0A8CCD8, b[0] - 6, mv - 8, b[0], mv + 8);
                f.rect(0xA0A8CCD8, b[2], mv - 8, b[2] + 6, mv + 8);
                // The square: rows of tables under parasols, a clear aisle to the door.
                int k = 0;
                for (float v = fv + 13; v < S - 8; v += 16) {
                    for (float u = 16; u < L - 12; u += 21) {
                        if (Math.abs(u - cu) < 18) continue;
                        table(f, u, v, 2.6f, 0xFFE8E2D4, 0xFF4A4440, 4);
                        if (k % 3 != 1) f.parasol(u, v, 5.2f, k % 2 == 0 ? 0xFF2E6A5A : 0xFFC8423A, 0xFFF2EDE0);
                        k++;
                    }
                }
                // Food carts with striped awnings at the corners of the square.
                if (S - fv > 34) {
                    float cv = S - 12;
                    for (int j = 0; j < 2; j++) {
                        float u = j == 0 ? 6 : L - 20;
                        f.sh(u, cv - 4, u + 14, cv + 4, 1.5f);
                        f.rect(0xFF8A8E92, u, cv - 4, u + 14, cv + 4);
                        awning(f, u - 1, cv - 5, u + 15, cv, j == 0 ? 0xFFE8B83A : 0xFF3A7AC8, 0xFFF4F0E6, 2, -1);
                        f.circle(0xFF2A2A2A, u + 3, cv + 4.5f, 1.4f);
                        f.circle(0xFF2A2A2A, u + 11, cv + 4.5f, 1.4f);
                    }
                }
                // Trees in planters and lamps, with festoon lights strung between them.
                f.rect(0xFF8A8478, 3, S - 9, 13, S - 1);
                f.tree(8, S - 5, 5.5f);
                f.rect(0xFF8A8478, L - 13, S - 9, L - 3, S - 1);
                f.tree(L - 8, S - 5, 5.5f);
                float lv = Math.min(S - 4, fv + 8);
                f.lamp(cu - 20, lv);
                f.lamp(cu + 20, lv);
                f.festoon(cu - 20, lv, 8, S - 5, 2, 8, false);
                f.festoon(cu + 20, lv, L - 8, S - 5, 2, 8, false);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int base = 0xFF6E8A82, n = 12;
                // A barrel vault in zinc, lit on its upper side.
                for (int k = 0; k < n; k++) {
                    float t = k / (float) (n - 1);
                    int col = t < 0.4f ? City.lighten(base, 0.28f * (1 - t / 0.4f)) : City.darken(base, 1 - 0.36f * (t - 0.4f) / 0.6f);
                    f.rect(col, 0, k * S / n, L, (k + 1) * S / n + 0.3f);
                }
                // The glazed crown with its bars.
                float g0 = S * 0.36f, g1 = S * 0.6f;
                f.rect(0xFF92BCC8, 3, g0, L - 3, g1);
                f.rect(0xFFB8D8E2, 3, g0, L - 3, (g0 + g1) / 2);
                for (float u = 3; u < L - 3; u += 3) f.line(0x40FFFFFF, 0.4f, u, g0, u, g1);
                f.line(0xFF3E4A48, 0.6f, 3, (g0 + g1) / 2, L - 3, (g0 + g1) / 2);
                // Iron ribs over the vault and the gable ends.
                for (float u = 7; u < L - 4; u += 11) {
                    f.line(0x40000000, 1.4f, u + 0.8f, 0, u + 0.8f, S);
                    f.line(0xFF34403E, 1.1f, u, 0, u, S);
                }
                f.rect(0xFF8E7A62, 0, 0, 3, S);
                f.rect(0xFF6E5E4A, L - 3, 0, L, S);
                f.frame(0xFF2E3836, 1f, 0, 0, L, S);
                // A clock lantern in the middle.
                float cu = L / 2, cv = S / 2, s = Math.min(6, S * 0.12f);
                f.sh(cu - s, cv - s, cu + s, cv + s, 2.5f);
                f.rect(0xFFE6E0D0, cu - s, cv - s, cu + s, cv + s);
                f.poly(0xFF5AA894, cu - s + 1, cv - s + 1, cu + s - 1, cv - s + 1, cu, cv);
                f.poly(0xFF448A78, cu + s - 1, cv - s + 1, cu + s - 1, cv + s - 1, cu, cv);
                f.poly(0xFF346E60, cu - s + 1, cv + s - 1, cu + s - 1, cv + s - 1, cu, cv);
                f.poly(0xFF62B4A0, cu - s + 1, cv - s + 1, cu - s + 1, cv + s - 1, cu, cv);
                f.circle(0xFFE8C860, cu, cv, 1.2f);
                f.sign(city, "FOOD HALL", cu, cv + s + 6, 7, 0xFF2E3836);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.MID | Variants.OLD;
        d.minS = 10;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_RESTAURANT;
        d.roofCol = 0xFF6E8A82;
        out.add(d);
    }

    // ------------------------------------------------------------------ 2 Multiplex Cinema

    static int[][] cinemaUV(int L, int S) {
        int lv = Math.max(5, Math.min(S - 4, S * 2 / 3));
        return new int[][]{{1, 1, L - 2, lv, 3}};
    }

    static void cinema(List<Landmarks.Def> out) {
        // A dark box of screens with a marquee and red carpet below and an open-air screen terrace on the roof.
        Landmarks.Def d = new Landmarks.Def("Multiplex Cinema", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return orient(cinemaUV(Math.max(bw, bh), Math.min(bw, bh)), bw >= bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] b = box(cinemaUV(lt(f), st(f))[0]);
                f.rect(0xFFCCC6BC, 0, 0, L, S);
                grid(f, 0x14000000, 0.5f, 0, 0, L, S, 10);
                f.rect(0xFF8E8A84, 0, 0, L, b[1] - 4);
                for (float u = 14; u < L - 10; u += 30) {
                    bin(f, u, 6, 0xFF4A4A50);
                    bin(f, u + 4, 6, 0xFF4A4A50);
                }
                float fv = b[3], cu = L * 0.32f;
                // Poster cases along the front wall.
                int k = 0;
                for (float u = 8; u < L - 6; u += 13) {
                    if (Math.abs(u - cu) < 24) continue;
                    f.rect(0xFF1E1E22, u - 3.5f, fv + 0.4f, u + 3.5f, fv + 2.8f);
                    f.rect(BRIGHT[k++ % BRIGHT.length], u - 2.8f, fv + 0.9f, u + 2.8f, fv + 2.3f);
                }
                // The red carpet out from the marquee, with queue barriers either side.
                f.rect(0xFFA01E2A, cu - 5, fv, cu + 5, S - 5);
                f.rect(0xFFB8303A, cu - 4, fv, cu + 4, S - 5);
                for (int j = -1; j <= 1; j += 2) {
                    float u = cu + j * 8;
                    for (float v = fv + 10; v < S - 6; v += 7) {
                        f.circle(0xFFD8C060, u, v, 0.9f);
                        if (v + 7 < S - 6) f.line(0xFF8A1A24, 0.7f, u, v, u, v + 7);
                    }
                }
                // The marquee canopy with its chasing bulbs.
                f.sh(cu - 20, fv, cu + 20, fv + 10, 2.5f);
                f.rect(0xFF24202A, cu - 20, fv, cu + 20, fv + 10);
                f.rect(0xFFB0202E, cu - 18.5f, fv + 1.5f, cu + 18.5f, fv + 8.5f);
                f.rect(0xFFF4E8C8, cu - 15, fv + 3.5f, cu + 15, fv + 6.5f);
                for (float u = cu - 13; u < cu + 14; u += 3) f.rect(0xFF2A2A2A, u, fv + 4.4f, u + 1.6f, fv + 5.6f);
                for (float u = cu - 19; u <= cu + 19.1f; u += 2.5f) f.circle(0xFFFFE8A0, u, fv + 9.3f, 0.55f);
                for (float v = fv + 1; v < fv + 9.5f; v += 2.5f) {
                    f.circle(0xFFFFE8A0, cu - 19.3f, v, 0.55f);
                    f.circle(0xFFFFE8A0, cu + 19.3f, v, 0.55f);
                }
                // A ticket booth and a popcorn cart on the forecourt.
                float bu = L * 0.66f, bv = Math.min(S - 14, fv + 14);
                f.circle(0x38000000, bu + 2, bv + 2, 6);
                f.circle(0xFF2A2630, bu, bv, 6);
                f.circle(0xFFB0202E, bu, bv, 4.8f);
                f.circle(0xFFE8C860, bu, bv, 1.6f);
                float pu = bu + 18;
                if (pu < L - 10) {
                    f.sh(pu - 4, bv - 3, pu + 4, bv + 3, 1);
                    f.rect(0xFFF2EEE6, pu - 4, bv - 3, pu + 4, bv + 3);
                    for (float u = pu - 3; u < pu + 4; u += 2) f.rect(0xFFD03A30, u, bv - 3, u + 1, bv + 3);
                    f.circle(0xFFF4E8B0, pu, bv, 1.8f);
                }
                // Trees and lamps along the road.
                for (float u = 10; u < L - 6; u += 26) {
                    if (Math.abs(u - cu) < 12) continue;
                    f.tree(u, S - 6, 5);
                }
                f.lamp(cu - 12, S - 3);
                f.lamp(cu + 12, S - 3);
                f.sign(city, "CINEMA", cu, fv + 5, 6, 0xFFFFE8A0);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF34363C, 0, 0, L, S);
                f.rect(0xFF4C4E54, 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                float tu = L * 0.5f;
                // The auditoria rise above the foyer as two slabs, with their plant.
                float a0 = 4, a1 = tu - 4, am = (a0 + a1) / 2;
                for (int j = 0; j < 2; j++) {
                    float u0 = j == 0 ? a0 : am + 1.5f, u1 = j == 0 ? am - 1.5f : a1;
                    f.sh(u0, 4, u1, S - 8, 2.5f);
                    f.rect(0xFF5E6066, u0, 4, u1, S - 8);
                    f.rect(0xFF686A70, u0 + 1, 5, u1 - 1, S * 0.35f);
                    for (float u = u0 + 4; u < u1 - 1; u += 4) f.line(0x18000000, 0.4f, u, 4, u, S - 8);
                    unit(f, u0 + 3, S - 16, 6, 5);
                }
                f.circle(0xFF7E8288, a0 + 3, S - 4, 1.6f);
                // The screen terrace: a deck, beanbag rows, a projector beam and the screen.
                f.rect(0xFF8E6E4C, tu, 2.5f, L - 2.5f, S - 2.5f);
                for (float v = 4.5f; v < S - 2.5f; v += 2.4f) f.line(0x22000000, 0.4f, tu, v, L - 2.5f, v);
                f.frame(0xFFD8D8D8, 0.8f, tu, 2.5f, L - 2.5f, S - 2.5f);
                float su = L - 7, v0 = S * 0.14f, v1 = S * 0.86f;
                f.poly(0x1CFFF8D8, tu + 5, S / 2 - 1.5f, su - 2, v0 + 1, su - 2, v1 - 1, tu + 5, S / 2 + 1.5f);
                int[] bag = {0xFFB8323E, 0xFF2E3A5A, 0xFFB8323E, 0xFFE0B040};
                int k = 0;
                f.rect(0xFF5E9A4A, su - 14, v0, su - 4, v1);
                for (float v = v0 + 2; v < v1; v += 3) f.line(0x18FFFFFF, 1.2f, su - 14, v, su - 4, v);
                for (float u = tu + 12; u < su - 18; u += 10) {
                    for (float v = v0 + 3; v < v1 - 2; v += 8f) {
                        if (Math.abs(v - S / 2) < 4) continue;
                        f.roundRect(0x30000000, u - 2, v - 1.8f, u + 3.6f, v + 3.2f, 1.8f);
                        f.roundRect(bag[k++ % bag.length], u - 2.8f, v - 2.6f, u + 2.8f, v + 2.6f, 1.8f);
                        f.circle(0x30FFFFFF, u - 1, v - 1, 1.2f);
                    }
                }
                f.sh(su - 2, v0, su + 2.5f, v1, 4);
                f.rect(0xFF1E1E22, su - 2, v0 - 1, su + 2.5f, v1 + 1);
                f.rect(0xFFF6F6F2, su - 3.6f, v0, su - 2, v1);
                f.rect(0xFF3A3A40, su + 0.5f, v0 - 3, su + 2, v0 - 1);
                f.rect(0xFF3A3A40, su + 0.5f, v1 + 1, su + 2, v1 + 3);
                f.sh(tu + 2, S / 2 - 3, tu + 6, S / 2 + 3, 1.2f);
                f.rect(0xFF2A2A30, tu + 2, S / 2 - 3, tu + 6, S / 2 + 3);
                f.circle(0xFFFFF0C0, tu + 6, S / 2, 0.9f);
                f.palm(tu + 4, 6, 4);
                f.palm(tu + 4, S - 6, 4);
                f.festoon(tu + 1, 3, L - 3, 3, 1.5f, 10, true);
                f.festoon(tu + 1, S - 3, L - 3, S - 3, -1.5f, 10, true);
                // A red neon band along the front.
                f.neonLine(0xFFFF4A5A, 3, S - 1, tu - 3, S - 1);
                f.sign(city, "MULTIPLEX", tu / 2, S / 2, 7, 0xFFFF4A5A);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.MID | Variants.SUB;
        d.minS = 10;
        d.minL = 14;
        d.base = City.OFFICE;
        d.inside = Variants.I_SEATS;
        d.roofCol = 0xFF4C4E54;
        out.add(d);
    }

    // ------------------------------------------------------------------ 3 Car Showroom

    static int[][] showUV(int L, int S) {
        int lv = Math.max(4, Math.min(S - 5, S / 2)), lu = Math.max(lv + 1, L * 55 / 100), ws = L - lu - 3;
        if (ws >= 3) return new int[][]{{1, 1, lu, lv, 1}, {lu + 2, 1, ws, Math.min(4, lv), 1}};
        return new int[][]{{1, 1, lu, lv, 1}};
    }

    static void carShowroom(List<Landmarks.Def> out) {
        // A glass box full of new cars, a forecourt of more on show and a turntable, and a service workshop.
        Landmarks.Def d = new Landmarks.Def("Car Showroom", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return orient(showUV(Math.max(bw, bh), Math.min(bw, bh)), bw >= bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] pl = showUV(lt(f), st(f));
                float[] b = box(pl[0]);
                f.rect(0xFFE2E0DA, 0, 0, L, S);
                grid(f, 0x12000000, 0.5f, 0, 0, L, S, 12);
                f.rect(0xFFD2D0CA, 0, b[3] + 2, L, b[3] + 4);
                // The workshop: its apron, roller doors, a car waiting and a stack of tyres.
                if (pl.length > 1) {
                    float[] w = box(pl[1]);
                    f.rect(0xFF8A8A88, w[0] - 4, w[3], L - 2, w[3] + 26);
                    for (int j = 0; j < 2; j++) {
                        float u = w[0] + (w[2] - w[0]) * (0.28f + 0.44f * j);
                        f.rect(0xFFB8BCC0, u - 6, w[3] - 0.5f, u + 6, w[3] + 1.5f);
                        f.line(0xFF6A6E72, 0.4f, u - 6, w[3] + 0.5f, u + 6, w[3] + 0.5f);
                    }
                    f.car(w[0] + (w[2] - w[0]) * 0.28f, w[3] + 11, Math.PI / 2, 0xFF7A8A90, 5.2f);
                    float tu = w[2] - 4, tv = w[3] + 20;
                    for (int j = 0; j < 3; j++) {
                        f.circle(0x30000000, tu - j * 5 + 0.8f, tv + 0.8f, 2.2f);
                        f.circle(0xFF2A2A2A, tu - j * 5, tv, 2.2f);
                        f.circle(0xFF6A6A6A, tu - j * 5, tv, 1f);
                    }
                }
                // A turntable with the star car.
                float fv = b[3], tv = (fv + S) / 2, turn = Math.min(12, (S - fv) / 2 - 3);
                float tu = b[0] + turn + 6;
                f.circle(0x30000000, tu + 1.5f, tv + 1.5f, turn);
                f.circle(0xFF9AA0A6, tu, tv, turn);
                f.circle(0xFFC4CAD0, tu, tv, turn - 1.2f);
                f.ring(0x30000000, 0.5f, tu, tv, turn * 0.6f);
                f.car(tu, tv, -0.5, 0xFFD03028, 5.6f);
                // A row of cars on show, each with a price board.
                int k = 0;
                for (float u = tu + turn + 14; u < b[2] - 4; u += 17) {
                    f.car(u, tv, -Math.PI * 0.32f, CARS[(k + 1) % CARS.length], 5.2f);
                    f.rect(0xFFF4F4F0, u + 3, tv - 7, u + 7, tv - 5);
                    f.rect(0xFFE8B83A, u + 3, tv - 7, u + 4, tv - 5);
                    k++;
                }
                // Flags along the road and trees at the ends.
                for (float u = 8; u < L - 6; u += 11) {
                    f.circle(0xFF4A4C50, u, S - 3, 0.7f);
                    f.poly(BRIGHT[(int) (u / 11) % 4 == 0 ? 0 : (int) (u / 11) % BRIGHT.length], u + 0.5f, S - 3.5f, u + 5, S - 2.6f, u + 0.5f, S - 1.8f);
                }
                f.tree(7, b[1] + 6, 4.5f);
                f.lamp(b[0] - 7, fv + 6);
                f.sign(city, "MOTORS", (b[0] + b[2]) / 2, fv + 3, 6, 0xFF1E9AAE);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                if (part == 0) {
                    // Seen through the glass: the polished floor and the cars on it.
                    f.rect(0xFFDCDFE2, 0, 0, L, S);
                    grid(f, 0x16000000, 0.5f, 0, 0, L, S, 8);
                    int n = Math.max(2, (int) (L / 28));
                    for (int k = 0; k < n; k++) {
                        float u = (k + 0.5f) * L / n, v = S * 0.55f + (k % 2 == 0 ? -S * 0.12f : S * 0.14f);
                        if (k == n / 2) {
                            f.circle(0xFFB8BEC4, u, v, 8);
                            f.circle(0xFFCDD2D8, u, v, 7);
                        }
                        f.car(u, v, k % 2 == 0 ? 0.55 : -0.55, CARS[(k * 3 + 2) % CARS.length], 5.4f);
                    }
                    // A reception desk, plants and a lounge corner.
                    f.sh(4, S - 12, 16, S - 8, 1);
                    f.roundRect(0xFFF4F4F2, 4, S - 12, 16, S - 8, 2);
                    f.rect(0xFF1E9AAE, 4, S - 9, 16, S - 8);
                    pot(f, 3.5f, 9, 2.4f, 0xFF4E8A40);
                    pot(f, L - 4, S - 4, 2.4f, 0xFF4E8A40);
                    f.roundRect(0xFF3A3C40, L - 16, 4, L - 4, 8, 1.5f);
                    f.circle(0xFFE8E4DC, L - 10, 12, 2.4f);
                    f.rect(0x58A0CCE8, 0, 0, L, S);
                    for (float u = 12; u < L - 2; u += 12) f.line(0x90FFFFFF, 0.6f, u, 0, u, S);
                    f.line(0x90FFFFFF, 0.6f, 0, S / 2, L, S / 2);
                    c.save();
                    c.clipRect(x0, y0, x1, y1);
                    for (float u = -S; u < L; u += 22) f.line(0x34FFFFFF, 3f, u, S, u + S * 0.6f, 0);
                    c.restore();
                    // A white fascia band along the back with the brand stripe.
                    f.sh(0, 0, L, 6, 1.5f);
                    f.rect(0xFFF0F0EE, 0, 0, L, 6);
                    f.rect(0xFF1E9AAE, 0, 4.5f, L, 6);
                    f.frame(0xFF6A7076, 1.2f, 0, 0, L, S);
                } else {
                    // The workshop: a ribbed metal roof with a lantern of roof lights.
                    gable(f, 0xFF9AA2A8, 0, 0, L, S);
                    for (float u = 2; u < L; u += 2.5f) f.line(0x22000000, 0.4f, u, 0, u, S);
                    for (float u = 4; u < L - 6; u += 10) f.rect(0x90C8E0EC, u, S / 2 - 4, u + 5, S / 2 - 1);
                    f.rect(0xFF1E9AAE, 0, S - 1.5f, L, S);
                }
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.SUB | Variants.MID | Variants.IND;
        d.minS = 9;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_SHOWROOM;
        d.roofCol = 0xFFDCDFE2;
        out.add(d);
    }

    // ------------------------------------------------------------------ 4 Garden Centre

    static int[][] gardenUV(int L, int S) {
        int lu = Math.max(4, L * 3 / 10), lv = Math.max(4, S * 55 / 100), gu = lu + 2, gl = L - lu - 3, glv = Math.max(3, (S - 2) / 2);
        if (gl >= 3) return new int[][]{{1, 1, lu, lv, 1}, {gu, 1, gl, glv, 1}};
        return new int[][]{{1, 1, lu, lv, 1}};
    }

    static void gardenCentre(List<Landmarks.Def> out) {
        // A shop, a big glasshouse full of plants, and a yard of plant tables, pots, young trees and compost.
        Landmarks.Def d = new Landmarks.Def("Garden Centre", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return orient(gardenUV(Math.max(bw, bh), Math.min(bw, bh)), bw >= bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] pl = gardenUV(lt(f), st(f));
                float[] sb = box(pl[0]);
                f.rect(0xFFD0C4A8, 0, 0, L, S);
                for (int k = 0; k < 40; k++) f.circle(0x14000000, r.nextFloat() * L, r.nextFloat() * S, 0.6f + r.nextFloat());
                // A box hedge round the yard.
                hedge(f, 0, 0, L, 2.5f);
                hedge(f, 0, 2.5f, 2.5f, S - 2);
                hedge(f, L - 2.5f, 2.5f, L, S - 2);
                // Pallets of compost bags beside the shop.
                float pv = sb[3] + 4;
                for (int j = 0; j < 3 && pv + 6 < S - 14; j++) {
                    float u = sb[0] + j * 10;
                    f.sh(u, pv, u + 8, pv + 6, 1);
                    f.rect(0xFFB8925A, u, pv, u + 8, pv + 6);
                    for (int q = 0; q < 2; q++) {
                        f.rect(j == 1 ? 0xFF3E6A3A : 0xFF6A4A30, u + 0.5f + q * 3.8f, pv + 0.5f, u + 3.7f + q * 3.8f, pv + 5.5f);
                        f.line(0x40FFFFFF, 0.4f, u + 0.5f + q * 3.8f, pv + 2, u + 3.7f + q * 3.8f, pv + 2);
                    }
                }
                // Rows of terracotta pots and planted urns in front of the shop.
                for (float v = Math.max(pv + 10, S - 22); v < S - 6; v += 7) {
                    for (float u = sb[0] + 2; u < sb[2]; u += 6) pot(f, u, v, 2.2f, ((int) (u + v)) % 3 == 0 ? FLOWERS[(int) (u * 7 + v) % FLOWERS.length] : 0xFF4E8A40);
                }
                if (pl.length < 2) return;
                float[] g = box(pl[1]);
                // Plant tables under a shade net, packed with flowering pots.
                float t0 = g[3] + 8, t1 = S - 12;
                int row = 0;
                for (float v = t0; v + 6 <= t1; v += 11) {
                    float u0 = g[0], u1 = L - 8;
                    f.sh(u0, v, u1, v + 6, 1.2f);
                    f.rect(0xFF7A6A52, u0, v, u1, v + 6);
                    f.rect(0xFF5A6A48, u0 + 0.6f, v + 0.6f, u1 - 0.6f, v + 5.4f);
                    for (float u = u0 + 1.8f; u < u1 - 1; u += 2.6f) {
                        f.circle(0xFF3E6A34, u, v + 1.8f, 1.15f);
                        f.circle(0xFF3E6A34, u, v + 4.2f, 1.15f);
                        int col = FLOWERS[(row * 3 + (int) ((u - u0) / 14)) % FLOWERS.length];
                        f.circle(col, u - 0.2f, v + 1.6f, 0.75f);
                        f.circle(col, u - 0.2f, v + 4f, 0.75f);
                    }
                    row++;
                }
                if (row > 0) {
                    float nu = (g[0] + L - 8) / 2;
                    f.rect(0x3A10301A, nu, t0 - 3, L - 5, t0 + (row - 1) * 11 + 9);
                    grid(f, 0x22102010, 0.4f, nu, t0 - 3, L - 5, t0 + (row - 1) * 11 + 9, 4);
                }
                // Young trees for sale along the front, and a little display pond with a fountain.
                for (float u = g[0] + 4; u < L - 20; u += 10) f.tree(u, S - 6, 3.6f, (int) u % 20 < 10 ? 0xFF4E8A3A : 0xFF8AA03A);
                pond(f.c, f.p, f.x(L - 18, S - 11), f.y(L - 18, S - 11), f.x(L - 5, S - 3), f.y(L - 5, S - 3));
                f.circle(0xC0FFFFFF, L - 11.5f, S - 7, 1.2f);
                // Trolleys parked by the door.
                for (float u = sb[2] + 3; u < g[0] - 3; u += 3) f.rect(0xFF2E7A3A, u, sb[3] - 8, u + 1.6f, sb[3] - 2);
                f.sign(city, "GARDEN CENTRE", (sb[0] + sb[2]) / 2, sb[3] + 2, 6, 0xFF2E6A2E);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                if (part == 0) {
                    // The shop: a green pitched roof with a timber-framed lantern.
                    gable(f, 0xFF4E7A4A, 0, 0, L, S);
                    for (float u = 2.5f; u < L; u += 2.5f) f.line(0x1C000000, 0.4f, u, 0, u, S);
                    f.sh(L * 0.2f, S / 2 - 3, L * 0.8f, S / 2 + 3, 1.2f);
                    f.rect(0xFF8A6A44, L * 0.2f, S / 2 - 3, L * 0.8f, S / 2 + 3);
                    f.rect(0xFFB0D4DC, L * 0.2f + 1, S / 2 - 2, L * 0.8f - 1, S / 2);
                    f.rect(0xFF88B0BA, L * 0.2f + 1, S / 2, L * 0.8f - 1, S / 2 + 2);
                    for (float u = L * 0.2f + 4; u < L * 0.8f - 2; u += 4) f.line(0xFF8A6A44, 0.5f, u, S / 2 - 3, u, S / 2 + 3);
                } else {
                    // The glasshouse: plants seen through ridge-and-furrow glazing.
                    f.rect(0xFF6A9A6A, 0, 0, L, S);
                    for (int k = 0; k < 60; k++) {
                        float u = r.nextFloat() * L, v = r.nextFloat() * S;
                        f.circle(k % 4 == 0 ? FLOWERS[k % FLOWERS.length] : (k % 2 == 0 ? 0xFF3E7A3A : 0xFF5A9A4A), u, v, 1 + r.nextFloat() * 2.5f);
                    }
                    int n = Math.max(2, Math.round(L / 14));
                    float bw = L / n;
                    for (int k = 0; k < n; k++) {
                        float u0 = k * bw, um = u0 + bw / 2;
                        f.rect(0x58E8F4F4, u0, 0, um, S);
                        f.rect(0x48C0D8DC, um, 0, u0 + bw, S);
                        f.line(0xFFF4F4F0, 0.8f, um, 0, um, S);
                        f.line(0xFFC8CCCC, 0.6f, u0, 0, u0, S);
                        for (float v = 4; v < S; v += 4) f.line(0x70FFFFFF, 0.35f, u0, v, u0 + bw, v);
                        if (k % 2 == 0) f.rect(0x40203020, um - 3, S * 0.3f, um, S * 0.3f + 4);
                    }
                    f.frame(0xFFE8ECEC, 1.2f, 0, 0, L, S);
                }
            }
        };
        d.ground = City.DIRT;
        d.districts = Variants.SUB | Variants.PARK | Variants.MID;
        d.minS = 9;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_SHELVES;
        d.roofCol = 0xFF4E7A4A;
        out.add(d);
    }

    // ------------------------------------------------------------------ 5 Hardware Superstore

    static int[][] hardwareUV(int L, int S) {
        int lv = Math.max(5, Math.min(S - 5, S * 6 / 10)), lu = Math.max(lv + 1, L * 6 / 10);
        return new int[][]{{1, 1, lu, lv, 1}};
    }

    static void hardware(List<Landmarks.Def> out) {
        // A big orange-banded shed with a timber yard: racks of planks, bricks, sand, sheds on show and a forklift.
        Landmarks.Def d = new Landmarks.Def("Hardware Superstore", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return orient(hardwareUV(Math.max(bw, bh), Math.min(bw, bh)), bw >= bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] b = box(hardwareUV(lt(f), st(f))[0]);
                f.rect(0xFF6A6A68, 0, 0, L, S);
                for (int k = 0; k < 24; k++) f.circle(0x12000000, r.nextFloat() * L, r.nextFloat() * S, 1 + r.nextFloat() * 3);
                // A pavement along the store front and a crossing out to a handful of bays.
                float fv = b[3], cu = (b[0] + b[2]) / 2;
                f.rect(0xFFBEB8AC, b[0] - 4, fv, b[2] + 4, fv + 7);
                for (float u = cu - 6; u < cu + 6; u += 2.4f) f.rect(0xFFF2F0E8, u, fv + 7, u + 1.2f, fv + 15);
                for (int j = 0; j < 4; j++) {
                    float u = b[0] + 4 + j * 13;
                    if (u + 13 > cu - 8) break;
                    f.line(0xC0F2F0E8, 0.7f, u, fv + 10, u, S - 3);
                    if (j % 2 == 0) f.car(u + 6.5f, (fv + 10 + S - 3) / 2, Math.PI / 2, CARS[j + 3], 5.2f);
                }
                // A trolley bay.
                for (float u = cu + 12; u < cu + 30 && u < b[2]; u += 2.2f) f.rect(0xFFE07030, u, fv + 10, u + 1, fv + 18);
                f.frame(0xFF9A9A98, 0.6f, cu + 11, fv + 9, cu + 31, fv + 19);
                // The entrance canopy in the house orange.
                f.sh(cu - 10, fv, cu + 10, fv + 6, 2);
                f.rect(0xFFE0702E, cu - 10, fv, cu + 10, fv + 6);
                f.rect(0xFFF08A44, cu - 10, fv, cu + 10, fv + 2.5f);
                // The timber yard: a chain fence, a covered rack and the open stacks.
                float y0u = b[2] + 6, y1u = L - 3;
                if (y1u - y0u > 20) {
                    f.rect(0xFF8A8478, y0u, 3, y1u, S - 3);
                    f.frame(0xFFB8BCC0, 0.6f, y0u, 3, y1u, S - 3);
                    float rv = Math.min(S * 0.45f, 50);
                    for (float v = 6; v < rv - 2; v += 6) {
                        for (int q = 0; q < 3; q++) f.rect(q % 2 == 0 ? 0xFFCCA46A : 0xFFB88E58, y0u + 3, v + q * 1.4f, y1u - 3, v + q * 1.4f + 1.2f);
                    }
                    f.sh(y0u + 1, 4, y1u - 1, rv, 2.5f);
                    f.rect(0xE0789088, y0u + 1, 4, y1u - 1, rv);
                    for (float u = y0u + 3; u < y1u - 1; u += 2.5f) f.line(0x24000000, 0.4f, u, 4, u, rv);
                    f.rect(0x30FFFFFF, y0u + 1, 4, y1u - 1, 6);
                    // Open stacks: planks, bricks and bagged sand.
                    float sw = (y1u - y0u - 8) / 2;
                    int qq = 0;
                    for (float v = rv + 20; v + 12 < S - 5; v += 15)
                    for (int q2 = 0; q2 < 2; q2++, qq++) {
                        int q = qq % 3;
                        float u0 = y0u + 3 + q2 * (sw + 2), u1 = u0 + sw;
                        if (q == 2) {
                            f.sh(u0, v, u1, v + 10, 1.2f);
                            f.rect(0xFFB8B4AC, u0, v, u1, v + 10);
                            for (float u = u0 + 2.5f; u < u1; u += 2.5f) f.line(0x30000000, 0.4f, u, v, u, v + 10);
                            f.rect(0x40FFFFFF, u0, v, u1, v + 2);
                            continue;
                        }
                        f.sh(u0, v, u1, v + 10, 1.2f);
                        if (q == 0) {
                            f.rect(0xFFC89E64, u0, v, u1, v + 10);
                            for (float w = v + 1.2f; w < v + 10; w += 1.6f) f.line(0x30000000, 0.4f, u0, w, u1, w);
                        } else {
                            for (float u = u0; u + 4 <= u1 + 0.1f; u += 4.5f) {
                                f.rect(0xFFA44C36, u, v, u + 4, v + 4.6f);
                                f.rect(0xFFD8CCAA, u, v + 5.4f, u + 4, v + 10);
                                f.line(0x30000000, 0.4f, u, v + 2.3f, u + 4, v + 2.3f);
                            }
                        }
                    }
                    // The forklift.
                    float fu = y0u + 8, fvv = rv + 9;
                    f.sh(fu, fvv - 3, fu + 7, fvv + 3, 1);
                    f.rect(0xFFE8B830, fu, fvv - 3, fu + 7, fvv + 3);
                    f.rect(0xFF2A2A2A, fu + 2, fvv - 2, fu + 5, fvv + 2);
                    f.line(0xFF505050, 0.8f, fu + 7, fvv - 2, fu + 11, fvv - 2);
                    f.line(0xFF505050, 0.8f, fu + 7, fvv + 2, fu + 11, fvv + 2);
                }
                // Two garden sheds on show by the road, and the pylon sign.
                float shu = b[2] - 24;
                for (int j = 0; j < 2; j++) {
                    float u = shu + j * 12;
                    F g = new F(c, p, f.x(u, S - 13), f.y(u, S - 13), f.x(u + 9, S - 5), f.y(u + 9, S - 5));
                    g.sh(0, 0, g.L, g.S, 1.2f);
                    gable(g, j == 0 ? 0xFF6A8A5A : 0xFF8A6A4A, 0, 0, g.L, g.S);
                }
                f.sh(4, S - 10, 14, S - 6, 3);
                f.rect(0xFF3A3A3A, 4, S - 10, 14, S - 6);
                f.rect(0xFFE0702E, 4.6f, S - 9.4f, 13.4f, S - 6.6f);
                f.sign(city, "HARDWARE", cu, fv + 3, 6, 0xFFE0702E);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF8E908E, 0, 0, L, S);
                f.rect(0xFFC4C6C2, 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                for (float u = 8; u < L - 2; u += 8) f.line(0x14000000, 0.5f, u, 1.5f, u, S - 1.5f);
                // Rows of roof lights and the plant down the spine.
                for (float v = S * 0.18f; v < S * 0.8f; v += S * 0.3f) {
                    for (float u = 8; u < L - 12; u += 14) {
                        f.rect(0xFF9A9C9A, u - 0.6f, v - 0.6f, u + 6.6f, v + 4.6f);
                        f.rect(0xFFA8C8D8, u, v, u + 6, v + 4);
                        f.rect(0x60FFFFFF, u, v, u + 6, v + 1.6f);
                    }
                }
                for (float u = 10; u < L - 14; u += 22) unit(f, u, S * 0.5f - 1, 8, 6);
                // The orange fascia along the front.
                f.rect(0xFFE0702E, 0, S - 3.5f, L, S);
                f.rect(0xFFF08A44, 0, S - 3.5f, L, S - 2.5f);
                f.sign(city, "HARDWARE", L / 2, S * 0.5f, 7, 0xFFE0702E);
            }
        };
        d.ground = City.LOT;
        d.districts = Variants.SUB | Variants.IND | Variants.MID;
        d.minS = 10;
        d.minL = 14;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_SHELVES;
        d.roofCol = 0xFFC4C6C2;
        out.add(d);
    }

    // ------------------------------------------------------------------ 6 Motel

    static int[][] motelUV(int L, int S) {
        return new int[][]{{1, 1, L - 6, 3, 2}, {L - 4, 1, 3, S - 2, 2}, {1, S - 4, 4, 2, 1}};
    }

    /** A kidney shape in the box (u0, v0)-(u1, v1). */
    static Path kidney(F f, float u0, float v0, float u1, float v1) {
        float w = u1 - u0, h = v1 - v0;
        Path q = new Path();
        f.mv(q, u0 + 0.12f * w, v0 + 0.15f * h);
        f.qd(q, u0 + 0.5f * w, v0 - 0.08f * h, u1 - 0.12f * w, v0 + 0.12f * h);
        f.qd(q, u1 + 0.06f * w, v0 + 0.5f * h, u1 - 0.15f * w, v1 - 0.08f * h);
        f.qd(q, u0 + 0.66f * w, v1 + 0.04f * h, u0 + 0.52f * w, v0 + 0.66f * h);
        f.qd(q, u0 + 0.4f * w, v1 + 0.1f * h, u0 + 0.12f * w, v1 - 0.12f * h);
        f.qd(q, u0 - 0.06f * w, v0 + 0.5f * h, u0 + 0.12f * w, v0 + 0.15f * h);
        q.close();
        return q;
    }

    static void motel(List<Landmarks.Def> out) {
        // A 1960s roadside motel: two wings of rooms in an L round a kidney pool, the office and a neon pylon sign.
        Landmarks.Def d = new Landmarks.Def("Motel", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return orient(motelUV(Math.max(bw, bh), Math.min(bw, bh)), bw >= bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] pl = motelUV(lt(f), st(f));
                float[] a = box(pl[0]), b = box(pl[1]), o = box(pl[2]);
                f.rect(0xFF6E6E6A, 0, 0, L, S);
                // A lawn behind the wings and along the road.
                f.rect(0xFF6A9A4E, 0, 0, L, a[1] - 3);
                f.rect(0xFF6A9A4E, b[2] + 3, 0, L, S);
                // Covered walkways along the room fronts, with the doors and their numbers.
                f.rect(0xFFD4CCBA, a[0], a[3], a[2], a[3] + 7);
                f.rect(0xFFD4CCBA, b[0] - 7, a[3], b[0], b[3]);
                int[] dc = {0xFF2AA0A8, 0xFFE07A3A, 0xFFE8C040};
                int k = 0;
                for (float u = a[0] + 5; u < a[2] - 3; u += 10, k++) {
                    f.rect(dc[k % 3], u - 2, a[3], u + 2, a[3] + 1.2f);
                    f.rect(0xFF9AC4D4, u + 3, a[3], u + 6.5f, a[3] + 0.8f);
                    if (k % 3 == 1) {
                        f.circle(0xFFE8E4DC, u + 4.5f, a[3] + 3.5f, 1.2f);
                        f.circle(0xFFE8E4DC, u + 7.5f, a[3] + 3.5f, 1.2f);
                    }
                }
                for (float v = a[3] + 5; v < b[3] - 3; v += 10, k++) {
                    f.rect(dc[k % 3], b[0] - 1.2f, v - 2, b[0], v + 2);
                    f.rect(0xFF9AC4D4, b[0] - 0.8f, v + 3, b[0], v + 6.5f);
                }
                f.line(0xFF8A8478, 0.7f, a[0], a[3] + 7, a[2], a[3] + 7);
                f.line(0xFF8A8478, 0.7f, b[0] - 7, a[3] + 7, b[0] - 7, b[3]);
                // The ice machine and drinks in the breezeway.
                f.rect(0xFFE8ECEE, a[2] + 3, a[1] + 4, a[2] + 9, a[1] + 10);
                f.rect(0xFFD03A30, a[2] + 3, a[1] + 14, a[2] + 9, a[1] + 20);
                // Three cars nosed in by the office.
                for (int j = 0; j < 3; j++) {
                    float u = a[0] + 8 + j * 15;
                    if (u + 8 > o[2] + 10) break;
                    f.line(0xC0F2F0E8, 0.6f, u - 7.5f, a[3] + 9, u - 7.5f, a[3] + 24);
                    if (j != 1) f.car(u, a[3] + 16, -Math.PI / 2, j == 0 ? 0xFF5AB8B0 : 0xFFE8D8B0, 5.2f);
                }
                // The pool deck with its kidney pool, loungers and parasols.
                float d0 = o[2] + 10, d1 = b[0] - 14, e0 = a[3] + 12, e1 = S - 8;
                if (d1 - d0 > 30 && e1 - e0 > 24) {
                    f.sh(d0, e0, d1, e1, 1);
                    f.rect(0xFFE8E0CC, d0, e0, d1, e1);
                    grid(f, 0x14000000, 0.4f, d0, e0, d1, e1, 5);
                    float pu0 = d0 + 6, pu1 = d1 - 6, pv0 = e0 + 9, pv1 = e1 - 5;
                    F g = new F(c, p, f.x(pu0, pv0), f.y(pu0, pv0), f.x(pu1, pv1), f.y(pu1, pv1));
                    Path q = kidney(g, 0, 0, g.L, g.S);
                    g.stroke(q, 0xFFF4F0E6, 3.5f);
                    g.fill(q, 0xFF3FB4D0);
                    q = kidney(g, 2, 2, g.L - 2, g.S - 2);
                    g.fill(q, 0xFF5AC8E0);
                    for (int j = 0; j < 4; j++) f.line(0x60FFFFFF, 0.5f, pu0 + (pu1 - pu0) * (0.2f + j * 0.18f), pv0 + 4 + j % 2 * 3, pu0 + (pu1 - pu0) * (0.28f + j * 0.18f), pv0 + 4 + j % 2 * 3);
                    f.rect(0xFFB8BCC0, pu1 - 6, pv0 - 1, pu1 - 5, pv0 + 3);
                    for (float u = d0 + 6; u < d1 - 6; u += 9) {
                        f.rect(0x30000000, u - 1.4f, e0 + 1.4f, u + 2.6f, e0 + 8.4f);
                        f.rect(0xFFF4F4F0, u - 2, e0 + 1, u + 2, e0 + 8);
                        f.rect(0xFF2AA0A8, u - 1.5f, e0 + 1.5f, u + 1.5f, e0 + 3.5f);
                    }
                    f.parasol(d0 + 4, e1 - 4, 4, 0xFFE07A3A, 0xFFF4F0E6);
                    f.palm(d1 + 6, e0 + 2, 6);
                    f.palm(d0 - 4, e1 + 2, 5);
                }
                // The office's car port, and the neon pylon by the road.
                f.rect(0xFFD4CCBA, o[0], o[1] - 4, o[2], o[1]);
                float su = o[2] + 6, sv = S - 5;
                f.rect(0x40000000, su - 2, sv - 7 + 8, su + 18, sv + 1 + 8);
                f.line(0xFF5A5C60, 1.2f, su + 8, sv, su + 8, sv + 4);
                f.roundRect(0xFF1E5A5E, su - 2, sv - 7, su + 18, sv + 1, 3);
                f.neon(0xFFFF5A9A, su - 1, sv - 6, su + 17, sv, 2.5f);
                f.neonLine(0xFFFFE070, su + 2, sv - 3, su + 12, sv - 3);
                f.poly(0xFFFFE070, su + 12, sv - 5.5f, su + 16, sv - 3, su + 12, sv - 0.5f);
                f.rect(0xFFFF3A3A, su + 2, sv + 1, su + 12, sv + 2.6f);
                f.sign(city, "MOTEL", su + 8, sv - 3, 6, 0xFFFF5A9A);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                if (part < 2) {
                    // A low gable of brown shingles, a vent and a cooler for each room.
                    gable(f, 0xFF8A6448, 0, 0, L, S);
                    for (float v = 2; v < S; v += 2) f.line(0x16000000, 0.4f, 0, v, L, v);
                    for (float u = 10; u < L - 2; u += 10) f.line(0x30000000, 0.5f, u, 0, u, S);
                    for (float u = 5; u < L - 2; u += 10) {
                        f.circle(0xFF4A4C50, u, S * 0.3f, 0.9f);
                        f.sh(u - 2.5f, S * 0.62f, u + 2.5f, S * 0.62f + 4, 0.8f);
                        f.rect(0xFFB8BCC0, u - 2.5f, S * 0.62f, u + 2.5f, S * 0.62f + 4);
                        f.line(0xFF7A7E82, 0.4f, u - 2, S * 0.62f + 2, u + 2, S * 0.62f + 2);
                    }
                    f.rect(0xFFF2EEE4, 0, S - 1.2f, L, S);
                    f.rect(0xFFF2EEE4, 0, 0, L, 1.2f);
                } else {
                    // The office: a swooping butterfly roof in turquoise.
                    f.rect(0xFF2A8A90, 0, 0, L, S / 2);
                    f.rect(0xFF1E6A70, 0, S / 2, L, S);
                    f.rect(0xFF4AB0B4, 0, 0, L, 2);
                    f.line(0xFF12484C, 1f, 0, S / 2, L, S / 2);
                    f.rect(0xFFF2EEE4, L * 0.7f, S * 0.2f, L * 0.8f, S * 0.45f);
                    f.sign(city, "OFFICE", L / 2, S / 2, 5, 0xFFFFFFFF);
                }
            }
        };
        d.ground = City.LOT;
        d.districts = Variants.SUB | Variants.IND | Variants.MID;
        d.minS = 10;
        d.minL = 13;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOTEL;
        d.roofCol = 0xFF8A6448;
        out.add(d);
    }

    // ------------------------------------------------------------------ 7 Boutique Hotel

    static int[][] boutiqueUV(int L, int S) {
        int lu = Math.max(5, L / 2), al = L - lu - 4;
        if (al >= 3) return new int[][]{{1, 1, lu, S - 2, 5}, {lu + 3, 1, al, Math.max(3, S * 4 / 10), 3}};
        return new int[][]{{1, 1, lu, S - 2, 5}};
    }

    static void boutique(List<Landmarks.Def> out) {
        // A slim townhouse hotel with a roof garden and plunge pool, and a tiled courtyard with olives and a fountain.
        Landmarks.Def d = new Landmarks.Def("Boutique Hotel", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return orient(boutiqueUV(Math.max(bw, bh), Math.min(bw, bh)), bw >= bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] pl = boutiqueUV(lt(f), st(f));
                float[] m = box(pl[0]);
                f.rect(0xFFD2B898, 0, 0, L, S);
                // Terracotta tiles laid on the diagonal.
                c.save();
                c.clipRect(x0, y0, x1, y1);
                for (float u = -S; u < L; u += 6) {
                    f.line(0x1C000000, 0.5f, u, 0, u + S, S);
                    f.line(0x1C000000, 0.5f, u + S, 0, u, S);
                }
                c.restore();
                // Bamboo along the back and a border hedge to the road.
                for (float u = m[2] + 6; u < L - 3; u += 4.5f) {
                    f.circle(0xFF4E8A3A, u, 4, 2.6f);
                    f.circle(0xFF6AAA4A, u - 0.8f, 3.2f, 1.2f);
                }
                hedge(f, m[2] + 4, S - 5, L - 3, S - 2);
                // The front door: a black canopy with clipped bay trees in pots.
                float fu = m[2], fv = (m[1] + m[3]) / 2;
                f.sh(fu, fv - 8, fu + 6, fv + 8, 1.5f);
                f.rect(0xFF1E1E22, fu, fv - 8, fu + 6, fv + 8);
                f.rect(0xFF3A3A40, fu, fv - 8, fu + 6, fv - 6.5f);
                f.rect(0xFF2E2A3A, fu + 6, fv - 3, fu + 16, fv + 3);
                pot(f, fu + 9, fv - 7, 2.6f, 0xFF2E6A2E);
                pot(f, fu + 9, fv + 7, 2.6f, 0xFF2E6A2E);
                // The courtyard: a round fountain, olive trees in square planters, tables and loungers.
                float cu0 = m[2] + 18, cu1 = L - 4, cv0 = pl.length > 1 ? box(pl[1])[3] + 5 : 12, cv1 = S - 8;
                float ccu = (cu0 + cu1) / 2, ccv = (cv0 + cv1) / 2, rad = Math.min(10, Math.min(cu1 - cu0, cv1 - cv0) / 4);
                if (rad > 4) {
                    f.circle(0x34000000, ccu + 1.5f, ccv + 1.5f, rad + 1.5f);
                    f.circle(0xFFE8E0D0, ccu, ccv, rad + 1.5f);
                    f.circle(0xFF3F8AB0, ccu, ccv, rad);
                    f.circle(0xFF5AA4C8, ccu, ccv, rad * 0.7f);
                    f.circle(0xFFE8E0D0, ccu, ccv, rad * 0.25f);
                    for (int j = 0; j < 8; j++) {
                        double an = j * Math.PI / 4;
                        f.circle(0xA0FFFFFF, ccu + (float) Math.cos(an) * rad * 0.45f, ccv + (float) Math.sin(an) * rad * 0.45f, 0.6f);
                    }
                }
                float[][] olives = {{cu0 + 6, cv0 + 6}, {cu1 - 6, cv0 + 6}, {cu0 + 6, cv1 - 4}, {cu1 - 6, cv1 - 4}};
                for (float[] ol : olives) {
                    f.sh(ol[0] - 4, ol[1] - 4, ol[0] + 4, ol[1] + 4, 1);
                    f.rect(0xFFA8A090, ol[0] - 4, ol[1] - 4, ol[0] + 4, ol[1] + 4);
                    f.tree(ol[0], ol[1], 4.6f, 0xFF7A9060);
                }
                if (cu1 - cu0 > 50) {
                    table(f, ccu - rad - 12, ccv, 2.2f, 0xFFF4F0E6, 0xFF2A2A2A, 3);
                    table(f, ccu + rad + 12, ccv, 2.2f, 0xFFF4F0E6, 0xFF2A2A2A, 3);
                }
                f.festoon(cu0, cv0, cu1, cv1, 2, 12, false);
                f.festoon(cu1, cv0, cu0, cv1, 2, 12, false);
                // A side garden along the hotel's flank with a bench.
                f.rect(0xFF6A9A4E, m[0] - 12, m[1], m[0] - 3, m[3]);
                for (float v = m[1] + 6; v < m[3] - 4; v += 14) f.bush(m[0] - 7.5f, v, 3);
                f.sign(city, "HOTEL", fu + 3, fv, 5, 0xFFE8C860);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                if (part == 0) {
                    // A stone parapet round a timber deck.
                    f.rect(0xFFB8AC98, 0, 0, L, S);
                    f.rect(0xFF9A7A58, 2, 2, L - 2, S - 2);
                    for (float v = 4; v < S - 2; v += 2.2f) f.line(0x20000000, 0.4f, 2, v, L - 2, v);
                    // Planter beds round the edge.
                    f.rect(0xFF5A4636, 2, 2, L - 2, 5.5f);
                    f.rect(0xFF5A4636, 2, S - 5.5f, L - 2, S - 2);
                    for (float u = 4; u < L - 3; u += 3.5f) {
                        f.circle(0xFF4E8A40, u, 3.8f, 1.6f);
                        f.circle(0xFF4E8A40, u + 1.7f, S - 3.8f, 1.6f);
                        if (((int) u) % 3 == 0) f.circle(FLOWERS[((int) u) % FLOWERS.length], u, 3.4f, 0.7f);
                    }
                    // The plunge pool with loungers.
                    float pu0 = L * 0.55f, pu1 = L - 6, pv0 = S * 0.3f, pv1 = S * 0.62f;
                    f.rect(0xFFE8E4DA, pu0 - 1.5f, pv0 - 1.5f, pu1 + 1.5f, pv1 + 1.5f);
                    f.rect(0xFF3FB0CC, pu0, pv0, pu1, pv1);
                    f.rect(0xFF62C6DC, pu0 + 1, pv0 + 1, pu1 - 1, (pv0 + pv1) / 2);
                    for (float u = pu0 + 1; u < pu1 - 4; u += 7) {
                        f.rect(0x30000000, u + 0.6f, pv1 + 3.6f, u + 4.6f, pv1 + 11.6f);
                        f.rect(0xFFF4F2EC, u, pv1 + 3, u + 4, pv1 + 11);
                    }
                    // A pergola with a daybed, and the lift overrun.
                    float gu0 = 5, gu1 = L * 0.45f, gv0 = S * 0.3f, gv1 = S * 0.7f;
                    f.roundRect(0xFFE8DCC8, gu0 + 2, gv0 + 2, gu1 - 2, gv1 - 2, 2);
                    f.rect(0xFF6A8AA0, gu0 + 3, gv0 + 3, gu1 - 3, gv0 + 5);
                    for (float u = gu0; u <= gu1 + 0.1f; u += (gu1 - gu0) / 5) f.line(0xFF5A4030, 1f, u, gv0, u, gv1);
                    f.line(0xFF4A3424, 1.2f, gu0, gv0, gu1, gv0);
                    f.line(0xFF4A3424, 1.2f, gu0, gv1, gu1, gv1);
                    for (float u = gu0 + 2; u < gu1; u += 5) f.circle(0xFF5A9A48, u, gv0 + (u * 7) % 4, 1.5f);
                    f.sh(L * 0.48f, S * 0.75f, L * 0.48f + 9, S * 0.75f + 7, 2);
                    f.rect(0xFF8A8478, L * 0.48f, S * 0.75f, L * 0.48f + 9, S * 0.75f + 7);
                    f.rect(0xFFA09A8E, L * 0.48f + 1, S * 0.75f + 1, L * 0.48f + 8, S * 0.75f + 3);
                    pot(f, 7, S * 0.18f + 1, 2.4f, 0xFF7A9060);
                    pot(f, L - 7, S * 0.82f - 1, 2.4f, 0xFF7A9060);
                } else {
                    // The annex: a sedum roof with roof lights.
                    f.rect(0xFF8A8478, 0, 0, L, S);
                    f.rect(0xFF7A8A4A, 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                    for (int k = 0; k < 50; k++)
                        f.circle(k % 3 == 0 ? 0xFFA05A4A : (k % 3 == 1 ? 0xFF92A256 : 0xFF5E7A3A), 2 + r.nextFloat() * (L - 4), 2 + r.nextFloat() * (S - 4), 0.8f + r.nextFloat() * 1.4f);
                    for (float u = 6; u < L - 8; u += 12) {
                        f.rect(0xFF9A9A94, u - 0.5f, S / 2 - 3.5f, u + 6.5f, S / 2 + 3.5f);
                        f.rect(0xFFA8CCD8, u, S / 2 - 3, u + 6, S / 2 + 3);
                        f.rect(0x60FFFFFF, u, S / 2 - 3, u + 6, S / 2 - 1);
                    }
                }
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.OLD | Variants.MID;
        d.minS = 10;
        d.minL = 13;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOTEL;
        d.roofCol = 0xFF9A7A58;
        out.add(d);
    }

    // ------------------------------------------------------------------ 8 Wedding Chapel Gardens

    static int[][] chapelUV(int L, int S) {
        int lc = Math.max(5, L * 4 / 10);
        return new int[][]{{1, S / 2 - 2, lc, 4, 1}};
    }

    static void chapel(List<Landmarks.Def> out) {
        // A little white chapel with a bell tower, looking down an aisle of chairs to a floral arch in a rose garden.
        Landmarks.Def d = new Landmarks.Def("Wedding Chapel Gardens", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return orient(chapelUV(Math.max(bw, bh), Math.min(bw, bh)), bw >= bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] b = box(chapelUV(lt(f), st(f))[0]);
                // Striped lawn, hedged all round.
                f.rect(0xFF6AA050, 0, 0, L, S);
                for (float u = 0; u < L; u += 16) f.rect(0xFF74AA58, u, 0, u + 8, S);
                hedge(f, 0, 0, L, 3);
                hedge(f, 0, S - 3, L, S);
                hedge(f, 0, 3, 3, S - 3);
                hedge(f, L - 3, 3, L, S - 3);
                float cv = S / 2, au = L - 12;
                // Gravel round the chapel and the aisle: a white runner strewn with petals.
                f.rect(0xFFE0D8C4, b[0] - 4, b[1] - 4, b[2] + 6, b[3] + 4);
                f.rect(0xFFF6F4EE, b[2], cv - 3, au, cv + 3);
                for (int k = 0; k < 30; k++) f.circle(k % 2 == 0 ? 0xFFF0A0B8 : 0xFFE06A8A, b[2] + 4 + r.nextFloat() * (au - b[2] - 8), cv - 2.5f + r.nextFloat() * 5, 0.5f);
                // Rows of white chairs either side of the aisle.
                float r0 = b[2] + 14, r1 = au - 12;
                for (float u = r0; u < r1; u += 6) {
                    for (int sd = -1; sd <= 1; sd += 2) {
                        for (int j = 0; j < 4; j++) {
                            float v = cv + sd * (6 + j * 4);
                            if (Math.abs(v - cv) > S / 2 - 10) break;
                            f.rect(0x30000000, u - 1.1f, v - 1.1f, u + 2.1f, v + 2.1f);
                            f.rect(0xFFF8F8F4, u - 1.5f, v - 1.5f, u + 1.5f, v + 1.5f);
                            f.rect(0xFFD8D4CC, u + 0.8f, v - 1.5f, u + 1.5f, v + 1.5f);
                        }
                    }
                }
                // Lanterns along the aisle.
                for (float u = r0 - 4; u < r1; u += 18) {
                    f.lamp(u, cv - 4);
                    f.lamp(u, cv + 4);
                }
                // The floral arch at the end of the aisle.
                f.sh(au - 1.5f, cv - 10, au + 1.5f, cv + 10, 1.5f);
                f.rect(0xFFF4F2EC, au - 1.2f, cv - 10, au + 1.2f, cv + 10);
                for (float v = cv - 10; v <= cv + 10; v += 2) {
                    f.circle(0xFF5A9A48, au + (v % 4 == 0 ? 0.8f : -0.8f), v, 1.6f);
                    f.circle(((int) v) % 3 == 0 ? 0xFFF4F4F4 : 0xFFF08AAA, au, v, 1f);
                }
                // Rose beds along the long sides.
                for (int sd = 0; sd < 2; sd++) {
                    float v0 = sd == 0 ? 5 : S - 11, v1 = v0 + 6;
                    for (float u = b[2] + 8; u < L - 30; u += 26) {
                        f.roundRect(0xFF6A4E38, u, v0, u + 20, v1, 2);
                        for (float q = u + 2; q < u + 19; q += 2.6f) {
                            f.circle(0xFF3E6E34, q, v0 + 3, 1.5f);
                            f.circle(FLOWERS[((int) q / 3) % 4 == 0 ? 2 : ((int) q / 3) % 2 == 0 ? 0 : 3], q - 0.3f, v0 + 2.7f, 0.9f);
                        }
                    }
                }
                // A white gazebo in one corner and a lily pond in the other.
                float gu = L - 18, gv = 18;
                if (S > 120) {
                    Path q = new Path();
                    for (int k = 0; k < 8; k++) {
                        double an = k * Math.PI / 4 + Math.PI / 8;
                        float pu = gu + (float) Math.cos(an) * 10, pv = gv + (float) Math.sin(an) * 10;
                        if (k == 0) f.mv(q, pu, pv);
                        else f.ln(q, pu, pv);
                    }
                    q.close();
                    f.circle(0x38000000, gu + 2, gv + 2, 10);
                    f.fill(q, 0xFFF4F2EC);
                    for (int k = 0; k < 8; k++) {
                        double an = k * Math.PI / 4 + Math.PI / 8;
                        f.line(0xFFC8C4BC, 0.6f, gu, gv, gu + (float) Math.cos(an) * 10, gv + (float) Math.sin(an) * 10);
                    }
                    f.circle(0xFFE8C860, gu, gv, 1.2f);
                    pond(c, p, f.x(L - 30, S - 26), f.y(L - 30, S - 26), f.x(L - 6, S - 12), f.y(L - 6, S - 12));
                    for (int k = 0; k < 4; k++) f.circle(0xFF5A9A48, L - 26 + k * 5, S - 21 + (k % 2) * 3, 1.4f);
                }
                // Cypresses at the chapel door.
                f.tree(b[2] + 4, b[1] - 3, 3.2f, 0xFF2E5A34);
                f.tree(b[2] + 4, b[3] + 3, 3.2f, 0xFF2E5A34);
                f.sign(city, "CHAPEL", (b[0] + b[2]) / 2, b[3] + 6, 5, 0xFF8A6A9A);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                // A slate nave roof with stained glass glinting at the eaves.
                gable(f, 0xFF5E6672, 0, 0, L, S);
                for (float u = 2; u < L; u += 2.4f) f.line(0x1A000000, 0.4f, u, 0, u, S);
                int[] gl = {0xFF3A6AC8, 0xFFD84A4A, 0xFFE8C040, 0xFF4AA65A};
                for (float u = 6; u < L - S; u += 8) {
                    f.rect(gl[(int) (u / 8) % 4], u, 0, u + 2.5f, 1.2f);
                    f.rect(gl[(int) (u / 8 + 2) % 4], u, S - 1.2f, u + 2.5f, S);
                }
                // The white bell tower at the garden end, with a spire and a gold cross.
                float t = S * 0.56f, tu0 = L - t, tv0 = (S - t) / 2, tu1 = L, tv1 = tv0 + t;
                f.sh(tu0, tv0, tu1, tv1, 2.5f);
                f.rect(0xFFF4F2EC, tu0, tv0, tu1, tv1);
                float mu = (tu0 + tu1) / 2, mv = (tv0 + tv1) / 2, s = t / 2 - 1.5f;
                f.poly(0xFF8E96A0, mu - s, mv - s, mu + s, mv - s, mu, mv);
                f.poly(0xFF6E7680, mu + s, mv - s, mu + s, mv + s, mu, mv);
                f.poly(0xFF545C66, mu - s, mv + s, mu + s, mv + s, mu, mv);
                f.poly(0xFF9AA2AC, mu - s, mv - s, mu - s, mv + s, mu, mv);
                f.line(0xFFE8C860, 0.8f, mu - 2.5f, mv, mu + 2.5f, mv);
                f.line(0xFFE8C860, 0.8f, mu, mv - 1.5f, mu, mv + 1.5f);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.SUB | Variants.OLD | Variants.PARK;
        d.minS = 10;
        d.minL = 13;
        d.base = City.OFFICE;
        d.inside = Variants.I_PEWS;
        d.roofCol = 0xFF5E6672;
        out.add(d);
    }

    // ------------------------------------------------------------------ 9 Bakery Row

    static int[][] bakeryUV(int L, int S) {
        if (S >= 12) return new int[][]{{1, 1, L - 2, 3, 2}, {1, S - 4, L - 2, 3, 2}};
        return new int[][]{{1, 1, L - 2, 4, 2}};
    }

    static final int[] SHOPROOF = {0xFFA0563A, 0xFF5A6068, 0xFF8A4A3A, 0xFF6E7880, 0xFFB86A44, 0xFF4E5A64, 0xFF9A7A5A};
    static final int[][] AWN = {{0xFFC83A32, 0xFFF4F0E6}, {0xFF2E7A4A, 0xFFF4F0E6}, {0xFF3A6AB0, 0xFFF4F0E6}, {0xFFE8B83A, 0xFFF4F0E6},
            {0xFFD87AA0, 0xFF6A4030}, {0xFF6A4030, 0xFFE8D8B0}};

    static int shops(float len) {
        return Math.max(3, Math.round(len / 56));
    }

    static void bakery(List<Landmarks.Def> out) {
        // Terraces of little bakeries and cafés facing a cobbled lane, each with its own roof and striped awning.
        Landmarks.Def d = new Landmarks.Def("Bakery Row", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return orient(bakeryUV(Math.max(bw, bh), Math.min(bw, bh)), bw >= bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] pl = bakeryUV(lt(f), st(f));
                float[] a = box(pl[0]);
                float l0 = a[3], l1 = pl.length > 1 ? box(pl[1])[1] : S;
                // Back yards behind the terraces.
                f.rect(0xFF8A9A6A, 0, 0, L, a[1]);
                if (pl.length > 1) f.rect(0xFF8A9A6A, 0, l1 + 3 * T, L, S);
                // The cobbled lane with a pavement each side.
                f.rect(0xFFB4AA96, 0, l0, L, l1);
                for (float v = l0 + 8; v < l1 - 7; v += 3.2f) {
                    float off = ((int) ((v - l0) / 3.2f)) % 2 == 0 ? 0 : 1.6f;
                    for (float u = off; u < L; u += 3.2f) f.circle(0x18000000, u, v, 0.9f);
                }
                f.rect(0xFFD6CEBE, 0, l0, L, l0 + 7);
                if (pl.length > 1) f.rect(0xFFD6CEBE, 0, l1 - 7, L, l1);
                // The shop fronts: an awning each, café tables, chalk boards and bread carts.
                for (int row = 0; row < pl.length; row++) {
                    float[] b = box(pl[row]);
                    int n = shops(b[2] - b[0]);
                    float w = (b[2] - b[0]) / n;
                    for (int k = 0; k < n; k++) {
                        int[] ac = AWN[(k * 2 + row * 3) % AWN.length];
                        float u0 = b[0] + k * w + 1.5f, u1 = b[0] + (k + 1) * w - 1.5f;
                        float tv;
                        if (row == 0) {
                            awning(f, u0, b[3], u1, b[3] + 6, ac[0], ac[1], 2, 1);
                            tv = b[3] + 11;
                        } else {
                            awning(f, u0, b[1] - 6, u1, b[1], ac[0], ac[1], 2, -1);
                            tv = b[1] - 11;
                        }
                        if ((k + row) % 3 != 2) {
                            table(f, u0 + w * 0.3f, tv, 1.8f, 0xFFF4F0E6, 0xFF3A3430, 2);
                            table(f, u0 + w * 0.65f, tv, 1.8f, 0xFFF4F0E6, 0xFF3A3430, 2);
                        } else {
                            f.sh(u0 + 4, tv - 3, u0 + 14, tv + 3, 1);
                            f.rect(0xFF8A5A34, u0 + 4, tv - 3, u0 + 14, tv + 3);
                            for (float u = u0 + 5.5f; u < u0 + 13; u += 2.4f) {
                                f.oval(0xFFD8A050, u - 1, tv - 2.2f, u + 1, tv - 0.2f);
                                f.oval(0xFFC88A3A, u - 1, tv + 0.4f, u + 1, tv + 2.4f);
                            }
                        }
                        f.rect(0xFF2A2A2A, u1 - 3, tv - 1, u1 - 1, tv + 1);
                    }
                }
                // Down the middle: planters with small trees, benches and lamps; or bikes on a single row.
                float mv = (l0 + l1) / 2;
                if (l1 - l0 > 40) {
                    for (float u = 20; u < L - 10; u += 36) {
                        f.rect(0xFF8A8478, u - 5, mv - 5, u + 5, mv + 5);
                        f.tree(u, mv, 5, 0xFF5A8A3A);
                        f.bench(u + 13, mv, true);
                        f.lamp(u + 22, mv);
                    }
                } else {
                    for (float u = 16; u < L - 10; u += 40) {
                        f.tree(u, S - 7, 4.5f);
                        f.lamp(u + 20, S - 4);
                    }
                }
                for (int k = 0; k < 2; k++) {
                    float u = L * (0.3f + 0.4f * k), v = l0 + 16;
                    f.ring(0xFF2A2A2A, 0.6f, u - 2.6f, v, 1.8f);
                    f.ring(0xFF2A2A2A, 0.6f, u + 2.6f, v, 1.8f);
                    f.line(BRIGHT[k + 2], 0.8f, u - 2.6f, v, u + 2.6f, v);
                }
                f.sign(city, "BAKERY ROW", L / 2, mv, 6, 0xFF8A4A2A);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int n = shops(L);
                float w = L / n;
                for (int k = 0; k < n; k++) {
                    float u0 = k * w, u1 = u0 + w;
                    int col = SHOPROOF[(k * 3 + part * 2) % SHOPROOF.length];
                    gable(f, col, u0, 0, u1, S);
                    for (float v = 2; v < S; v += 2) f.line(0x18000000, 0.4f, u0, v, u1, v);
                    // A dormer or roof light on some, and a chimney stack on the party wall.
                    if (k % 2 == 0) {
                        f.sh(u0 + w * 0.35f, S * 0.6f, u0 + w * 0.65f, S - 2, 1);
                        f.rect(0xFFF0ECE2, u0 + w * 0.35f, S * 0.6f, u0 + w * 0.65f, S - 2);
                        f.rect(0xFF8AB0C0, u0 + w * 0.4f, S - 4, u0 + w * 0.6f, S - 2);
                    } else {
                        f.rect(0xFF9AC0CE, u0 + w * 0.4f, S * 0.15f, u0 + w * 0.6f, S * 0.38f);
                        f.frame(0xFFE8E8E4, 0.5f, u0 + w * 0.4f, S * 0.15f, u0 + w * 0.6f, S * 0.38f);
                    }
                    if (k > 0) {
                        f.rect(0xFFB8AC98, u0 - 1, 0, u0 + 1, S);
                        f.sh(u0 - 2.5f, S / 2 - 3, u0 + 2.5f, S / 2 + 1, 2);
                        f.rect(0xFF9A5A44, u0 - 2.5f, S / 2 - 3, u0 + 2.5f, S / 2 + 1);
                        f.circle(0xFFB86A44, u0 - 1.2f, S / 2 - 1, 0.9f);
                        f.circle(0xFFB86A44, u0 + 1.2f, S / 2 - 1, 0.9f);
                        if (k % 2 == 1) smoke(f, u0 + 1, S / 2 - 3, 1.4f, 0xFFE8E4DC);
                    }
                }
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.OLD | Variants.MID | Variants.SUB;
        d.minS = 9;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_CAFE;
        d.roofCol = 0xFFA0563A;
        out.add(d);
    }

    // ------------------------------------------------------------------ 10 Trattoria Piazza

    static int[][] trattoriaUV(int L, int S) {
        return new int[][]{{1, 1, Math.max(4, L * 4 / 10), S - 2, 2}};
    }

    /** A square table with a red-check cloth and its chairs. */
    static void checkTable(F f, float u, float v, float s) {
        f.circle(0xFF4A3A30, u - s - 1.2f, v, 1.1f);
        f.circle(0xFF4A3A30, u + s + 1.2f, v, 1.1f);
        f.circle(0xFF4A3A30, u, v - s - 1.2f, 1.1f);
        f.circle(0xFF4A3A30, u, v + s + 1.2f, 1.1f);
        f.rect(0x34000000, u - s + 0.8f, v - s + 0.8f, u + s + 0.8f, v + s + 0.8f);
        f.rect(0xFFF6F2EA, u - s, v - s, u + s, v + s);
        float q = s * 2 / 4;
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++)
                if ((i + j) % 2 == 0) f.rect(0xFFC8342A, u - s + i * q, v - s + j * q, u - s + (i + 1) * q, v - s + (j + 1) * q);
        f.circle(0xFFF4F0E8, u, v, s * 0.35f);
    }

    static void trattoria(List<Landmarks.Def> out) {
        // A family trattoria under a terracotta roof and a vine pergola, facing a piazza with a fountain and tables.
        Landmarks.Def d = new Landmarks.Def("Trattoria Piazza", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return orient(trattoriaUV(Math.max(bw, bh), Math.min(bw, bh)), bw >= bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] b = box(trattoriaUV(lt(f), st(f))[0]);
                f.rect(0xFFD8C8A8, 0, 0, L, S);
                float pu = b[2] + 18, cu = (pu + L) / 2, cv = S / 2, R = Math.min(L - pu, S) * 0.42f;
                // Stone setts in fans round the fountain.
                f.circle(0xFFCDBC9A, cu, cv, R);
                for (float rr = R; rr > 6; rr -= 7) f.ring(0x22000000, 0.6f, cu, cv, rr);
                for (int k = 0; k < 16; k++) {
                    double an = k * Math.PI / 8;
                    f.line(0x18000000, 0.5f, cu + (float) Math.cos(an) * 12, cv + (float) Math.sin(an) * 12, cu + (float) Math.cos(an) * R, cv + (float) Math.sin(an) * R);
                }
                f.ring(0xFFB8A886, 2, cu, cv, R);
                // The octagonal fountain.
                Path q = new Path(), w = new Path();
                float fr = Math.max(11, R * 0.3f), sc = fr / 11;
                for (int k = 0; k < 8; k++) {
                    double an = k * Math.PI / 4 + Math.PI / 8;
                    float ca = (float) Math.cos(an), sa = (float) Math.sin(an);
                    if (k == 0) {
                        f.mv(q, cu + ca * fr, cv + sa * fr);
                        f.mv(w, cu + ca * (fr - 2), cv + sa * (fr - 2));
                    } else {
                        f.ln(q, cu + ca * fr, cv + sa * fr);
                        f.ln(w, cu + ca * (fr - 2), cv + sa * (fr - 2));
                    }
                }
                q.close();
                w.close();
                f.circle(0x38000000, cu + 2, cv + 2, fr);
                f.fill(q, 0xFFE8DCC4);
                f.fill(w, 0xFF4A94B8);
                f.circle(0xFF6AAED0, cu, cv, 6 * sc);
                f.circle(0xFFE8DCC4, cu, cv, 2.5f * sc);
                f.circle(0xFFF4ECD8, cu - 0.6f, cv - 0.6f, 1.2f * sc);
                for (int k = 0; k < 10; k++) {
                    double an = k * Math.PI / 5;
                    f.circle(0xA0FFFFFF, cu + (float) Math.cos(an) * 4.2f * sc, cv + (float) Math.sin(an) * 4.2f * sc, 0.6f);
                }
                // A vine pergola along the trattoria's front with tables under it.
                float g0 = b[2], g1 = b[2] + 15;
                for (float v = b[1] + 6; v < b[3] - 4; v += 16) checkTable(f, g0 + 8, v, 2.6f);
                for (float v = b[1]; v <= b[3] + 0.1f; v += 8) f.line(0xFF6A4A30, 1f, g0, v, g1, v);
                f.line(0xFF5A3A24, 1.4f, g1, b[1], g1, b[3]);
                for (int k = 0; k < 40; k++) {
                    float u = g0 + r.nextFloat() * (g1 - g0), v = b[1] + r.nextFloat() * (b[3] - b[1]);
                    f.circle(k % 2 == 0 ? 0xFF4E8A3A : 0xFF6AA04A, u, v, 1.3f + r.nextFloat());
                    if (k % 7 == 0) f.circle(0xFF6A3A6A, u + 0.8f, v + 0.8f, 0.7f);
                }
                for (float v = b[1]; v <= b[3] + 0.1f; v += 16) f.circle(0xFF5A3A24, g1, v, 1);
                // Tables on the piazza round the fountain, some under cream umbrellas.
                int k = 0;
                for (float u = pu + 8; u < L - 6; u += 27) {
                    for (float v = 12; v < S - 8; v += 26) {
                        float du = u - cu, dv = v - cv;
                        if (du * du + dv * dv < (R * 0.5f + 8) * (R * 0.5f + 8)) continue;
                        checkTable(f, u, v, 2.6f);
                        if (k++ % 2 == 0) f.parasol(u, v, 6f, 0xFFF2EAD4, 0xFFE2D6B8);
                    }
                }
                // Olive trees in pots at the corners, a Vespa by the door and a lamp.
                pot(f, L - 6, 6, 3.4f, 0xFF7A9060);
                f.tree(L - 6, 6, 4, 0xFF7A9060);
                pot(f, L - 6, S - 6, 3.4f, 0xFF7A9060);
                f.tree(L - 6, S - 6, 4, 0xFF7A9060);
                float vu = g1 + 6, vv = S - 7;
                f.oval(0x40000000, vu - 3.5f, vv - 1.2f, vu + 5, vv + 2.6f);
                f.oval(0xFF8ACCB8, vu - 4, vv - 1.6f, vu + 4, vv + 1.6f);
                f.oval(0xFF4A3A30, vu - 1, vv - 1, vu + 2.4f, vv + 1);
                f.line(0xFF2A2A2A, 0.6f, vu - 3.6f, vv - 2.4f, vu - 3.6f, vv + 2.4f);
                f.lamp(pu - 2, 5);
                f.sign(city, "TRATTORIA", (b[0] + b[2]) / 2, (b[1] + b[3]) / 2, 6, 0xFF8A2A20);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                // A terracotta hip roof with pantile courses, a chimney and a roof light.
                hip(f, 0xFFB8603A, 0, 0, L, S, true);
                for (float u = 2; u < L; u += 2.2f) {
                    float dv = Math.min(Math.min(u, L - u), S / 2);
                    f.line(0x14000000, 0.4f, u, Math.min(dv, S / 2), u, S - Math.min(dv, S / 2));
                }
                float cu = L * 0.7f, cv = S * 0.3f;
                f.sh(cu - 3, cv - 3, cu + 3, cv + 3, 2.5f);
                f.rect(0xFFD8CCB4, cu - 3, cv - 3, cu + 3, cv + 3);
                f.rect(0xFFB86A44, cu - 3.5f, cv - 3.5f, cu + 3.5f, cv - 1.5f);
                f.circle(0xFF3A3430, cu, cv, 1.6f);
                smoke(f, cu + 1, cv, 2, 0xFFE8E4DC);
                f.rect(0xFF8AB0C0, L * 0.3f, S * 0.62f, L * 0.42f, S * 0.78f);
                f.frame(0xFFE8E4DC, 0.5f, L * 0.3f, S * 0.62f, L * 0.42f, S * 0.78f);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.OLD | Variants.MID | Variants.DOWN;
        d.minS = 9;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_RESTAURANT;
        d.roofCol = 0xFFB8603A;
        out.add(d);
    }

    // ------------------------------------------------------------------ 11 Sushi Pavilion

    static int[][] sushiUV(int L, int S) {
        int lu = Math.max(5, L * 45 / 100), lv = Math.max(4, S * 45 / 100);
        return new int[][]{{Math.max(2, L / 8), Math.max(2, (S - lv) / 2), lu, lv, 1}};
    }

    /** An organic blob (a pond) filling most of the box. */
    static Path blob(F f, float u0, float v0, float u1, float v1) {
        float w = u1 - u0, h = v1 - v0;
        Path q = new Path();
        f.mv(q, u0 + 0.1f * w, v0 + 0.3f * h);
        f.qd(q, u0 + 0.2f * w, v0 - 0.05f * h, u0 + 0.55f * w, v0 + 0.1f * h);
        f.qd(q, u1 + 0.05f * w, v0 + 0.05f * h, u1 - 0.05f * w, v0 + 0.55f * h);
        f.qd(q, u1 - 0.1f * w, v1 + 0.05f * h, u0 + 0.5f * w, v1 - 0.1f * h);
        f.qd(q, u0 - 0.05f * w, v1 + 0.02f * h, u0 + 0.1f * w, v0 + 0.3f * h);
        q.close();
        return q;
    }

    static void koi(F f, float u, float v, double a, int col) {
        float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
        f.circle(col, u, v, 1.3f);
        f.circle(col, u - ca * 1.5f, v - sa * 1.5f, 0.95f);
        f.circle(col, u - ca * 2.6f, v - sa * 2.6f, 0.55f);
        f.circle(0xFFF4F0E8, u + ca * 0.4f, v + sa * 0.4f, 0.55f);
    }

    static void sushi(List<Landmarks.Def> out) {
        // A dark-roofed timber pavilion on a deck, with a koi pond, a red bridge, maples and raked gravel.
        Landmarks.Def d = new Landmarks.Def("Sushi Pavilion", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return orient(sushiUV(Math.max(bw, bh), Math.min(bw, bh)), bw >= bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] b = box(sushiUV(lt(f), st(f))[0]);
                // Raked gravel.
                f.rect(0xFFDAD6CA, 0, 0, L, S);
                for (float v = 3; v < S; v += 2.6f) f.line(0x18000000, 0.4f, 0, v, L, v);
                // A bamboo fence along the back with bamboo behind it.
                for (float u = 3; u < L - 2; u += 4.2f) {
                    f.circle(0xFF5E9A44, u, 2.6f, 2.4f);
                    f.circle(0xFF7AB858, u - 0.6f, 2f, 1f);
                }
                f.line(0xFF9A8A5A, 1.2f, 0, 5.5f, L, 5.5f);
                for (float u = 2; u < L; u += 6) f.circle(0xFF6A5A3A, u, 5.5f, 0.8f);
                // The deck (engawa) round the pavilion.
                f.sh(b[0] - 6, b[1] - 6, b[2] + 6, b[3] + 6, 1.2f);
                f.rect(0xFF9A7450, b[0] - 6, b[1] - 6, b[2] + 6, b[3] + 6);
                for (float u = b[0] - 4; u < b[2] + 6; u += 2.2f) f.line(0x22000000, 0.4f, u, b[1] - 6, u, b[3] + 6);
                f.frame(0xFF5A4028, 0.7f, b[0] - 6, b[1] - 6, b[2] + 6, b[3] + 6);
                // Stepping stones from the road to the noren curtain.
                float cu = (b[0] + b[2]) / 2;
                for (float v = b[3] + 10; v < S - 2; v += 6) {
                    float off = ((int) v % 2 == 0) ? 1.5f : -1.5f;
                    f.oval(0x30000000, cu + off - 2.6f, v - 1.4f, cu + off + 3.4f, v + 2.6f);
                    f.oval(0xFF9A9890, cu + off - 3, v - 2, cu + off + 3, v + 2);
                }
                f.rect(0xFF2A3A5A, cu - 5, b[3] + 0.2f, cu + 5, b[3] + 2.2f);
                f.rect(0xFFF4F0E8, cu - 0.6f, b[3] + 0.2f, cu + 0.6f, b[3] + 2.2f);
                // The koi pond with a red bridge.
                float pu0 = b[2] + 14, pu1 = L - 8, pv0 = 12, pv1 = S - 10;
                if (pu1 - pu0 > 24) {
                    F f0 = f;
                    f = new F(c, p, f0.x(pu0, pv0), f0.y(pu0, pv0), f0.x(pu1, pv1), f0.y(pu1, pv1));
                    pu0 = 0;
                    pv0 = 0;
                    pu1 = f.L;
                    pv1 = f.S;
                    Path q = blob(f, pu0, pv0, pu1, pv1);
                    f.stroke(q, 0xFF8A8678, 3);
                    f.fill(q, 0xFF2E5E6E);
                    q = blob(f, pu0 + 4, pv0 + 4, pu1 - 4, pv1 - 4);
                    f.fill(q, 0xFF3A7484);
                    float[][] kp = {{0.3f, 0.35f, 0.4f}, {0.55f, 0.6f, 2.5f}, {0.7f, 0.3f, 1.2f}, {0.4f, 0.7f, 4f}, {0.8f, 0.6f, 5.2f}};
                    int[] kc = {0xFFF07A30, 0xFFF4F0E8, 0xFFD83A2A, 0xFFF0A040, 0xFFF4F0E8};
                    for (int k = 0; k < kp.length; k++) koi(f, pu0 + (pu1 - pu0) * kp[k][0], pv0 + (pv1 - pv0) * kp[k][1], kp[k][2], kc[k]);
                    for (int k = 0; k < 4; k++) f.circle(0xFF4E8A40, pu0 + (pu1 - pu0) * (0.2f + k * 0.2f), pv0 + (pv1 - pv0) * (k % 2 == 0 ? 0.8f : 0.2f), 1.8f);
                    float bu = pu0 + (pu1 - pu0) * 0.5f;
                    f.sh(bu - 4, pv0 + 2, bu + 4, pv1 - 2, 1.5f);
                    f.rect(0xFFB8402E, bu - 4, pv0 + 2, bu + 4, pv1 - 2);
                    for (float v = pv0 + 4; v < pv1 - 2; v += 2.4f) f.line(0x30000000, 0.4f, bu - 4, v, bu + 4, v);
                    f.line(0xFF7A2418, 1f, bu - 4, pv0 + 2, bu - 4, pv1 - 2);
                    f.line(0xFF7A2418, 1f, bu + 4, pv0 + 2, bu + 4, pv1 - 2);
                    f.rect(0x30FFFFFF, bu - 3.5f, (pv0 + pv1) / 2 - 4, bu + 3.5f, (pv0 + pv1) / 2);
                    f = f0;
                }
                // Rocks with raked rings, maples, pines and stone lanterns.
                float ru = b[0] / 2 + 2, rv = S - 12;
                if (ru > 8) {
                    for (int k = 3; k >= 1; k--) f.circle(k % 2 == 0 ? 0xFFD0CCC0 : 0xFFDAD6CA, ru, rv, 2 + k * 2.5f);
                    f.circle(0x40000000, ru + 1, rv + 1, 2.8f);
                    f.circle(0xFF7A7870, ru, rv, 2.8f);
                    f.circle(0xFF9A988E, ru - 0.8f, rv - 0.8f, 1.2f);
                }
                f.tree(b[0] - 2, b[1] - 9, 5.5f, 0xFFB8402E);
                f.tree(L - 6, S - 6, 5, 0xFFC8582E);
                f.tree(b[2] + 8, S - 6, 4.5f, 0xFF2E5A3A);
                f.tree(L - 6, 12, 4.5f, 0xFF2E5A3A);
                for (int k = 0; k < 2; k++) {
                    float u = k == 0 ? b[0] - 10 : b[2] + 10, v = b[3] + 6;
                    f.rect(0x40000000, u - 1.4f, v - 1.4f, u + 2.8f, v + 2.8f);
                    f.rect(0xFF8A8880, u - 2, v - 2, u + 2, v + 2);
                    f.circle(0xFFF4E6A8, u, v, 0.9f);
                }
                f.sign(city, "SUSHI", cu, b[3] + 5, 5, 0xFFB8402E);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                // A dark tiled hip roof with flared corners and a raised gable over the middle.
                float e = 2;
                f.rect(0x40000000, 2, 2, L + 2, S + 2);
                hip(f, 0xFF4A5058, -e, -e, L + e, S + e, false);
                for (float u = 2; u < L; u += 2) {
                    float dv = Math.min(Math.min(u + e, L + e - u), S / 2);
                    f.line(0x22000000, 0.4f, u, -e + 0.5f, u, -e + dv * 0.6f);
                    f.line(0x30000000, 0.4f, u, S + e - 0.5f, u, S + e - dv * 0.6f);
                }
                f.poly(0xFF6A727C, -e - 2, -e - 2, -e + 4, -e, -e, -e + 4);
                f.poly(0xFF3A4048, L + e + 2, S + e + 2, L + e - 4, S + e, L + e, S + e - 4);
                f.poly(0xFF5A626C, L + e + 2, -e - 2, L + e - 4, -e, L + e, -e + 4);
                f.poly(0xFF4A5058, -e - 2, S + e + 2, -e + 4, S + e, -e, S + e - 4);
                float iu0 = L * 0.24f, iu1 = L * 0.76f, iv0 = S * 0.26f, iv1 = S * 0.74f;
                f.sh(iu0, iv0, iu1, iv1, 2);
                gable(f, 0xFF525A64, iu0, iv0, iu1, iv1);
                for (float u = iu0 + 2; u < iu1; u += 2) f.line(0x24000000, 0.4f, u, iv0, u, iv1);
                f.rect(0xFFD8D0B8, iu0 - 1, iv0, iu0 + 1, iv1);
                f.rect(0xFFC0B8A0, iu1 - 1, iv0, iu1 + 1, iv1);
                f.line(0xFF2A2E34, 1.6f, iu0, S / 2, iu1, S / 2);
                f.circle(0xFF2A2E34, iu0, S / 2, 1.8f);
                f.circle(0xFF2A2E34, iu1, S / 2, 1.8f);
                f.circle(0xFFB8402E, L / 2, S / 2, 1);
            }
        };
        d.ground = City.SAND;
        d.districts = Variants.DOWN | Variants.MID | Variants.PARK;
        d.minS = 9;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_RESTAURANT;
        d.roofCol = 0xFF4A5058;
        out.add(d);
    }

    // ------------------------------------------------------------------ 12 Beer Hall

    static int[][] beerUV(int L, int S) {
        return new int[][]{{1, 1, L - 2, Math.max(4, Math.min(S - 5, S * 4 / 10)), 2}};
    }

    static void beerHall(List<Landmarks.Def> out) {
        // A long timber beer hall with dormers and a bell turret, a chestnut-shaded beer garden and a maypole.
        Landmarks.Def d = new Landmarks.Def("Beer Hall", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return orient(beerUV(Math.max(bw, bh), Math.min(bw, bh)), bw >= bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] b = box(beerUV(lt(f), st(f))[0]);
                f.rect(0xFFCDBE9A, 0, 0, L, S);
                for (int k = 0; k < 50; k++) f.circle(0x16000000, r.nextFloat() * L, r.nextFloat() * S, 0.5f + r.nextFloat() * 0.8f);
                f.rect(0xFF8A9A6A, 0, 0, L, b[1] - 3);
                // Barrels and crates at the back door.
                for (int k = 0; k < 4; k++) {
                    f.circle(0x34000000, 9 + k * 6 + 0.8f, 7.8f, 2.6f);
                    f.circle(0xFF8A5A34, 9 + k * 6, 7, 2.6f);
                    f.ring(0xFF3A3A3A, 0.4f, 9 + k * 6, 7, 1.8f);
                }
                // A low fence along the road with a gate.
                float gu = L / 2;
                f.line(0xFF7A5A3A, 1.2f, 2, S - 2.5f, gu - 6, S - 2.5f);
                f.line(0xFF7A5A3A, 1.2f, gu + 6, S - 2.5f, L - 2, S - 2.5f);
                for (float u = 2; u < L - 1; u += 5) if (Math.abs(u - gu) > 6) f.circle(0xFF5A3A24, u, S - 2.5f, 0.8f);
                // Long trestle tables in rows under the chestnuts.
                float g0 = b[3] + 8, g1 = S - 8;
                float tl = 22;
                for (float v = g0 + 4; v < g1 - 2; v += 15) {
                    for (float u = 10; u + tl < L - 8; u += tl + 12) {
                        f.rect(0x30000000, u + 1, v - 4 + 1, u + tl + 1, v + 4 + 1);
                        f.rect(0xFF6A4A2E, u, v - 4.2f, u + tl, v - 2.8f);
                        f.rect(0xFF6A4A2E, u, v + 2.8f, u + tl, v + 4.2f);
                        f.rect(0xFFA07A4E, u, v - 2, u + tl, v + 2);
                        f.line(0x30000000, 0.4f, u, v, u + tl, v);
                        for (float q = u + 3; q < u + tl - 1; q += 5) {
                            f.circle(0xFFF4E6B0, q, v - 1, 0.7f);
                            f.circle(0xFFE8B83A, q + 2, v + 1, 0.7f);
                        }
                    }
                }
                for (float v = g0 + 11; v < g1; v += 30) {
                    for (float u = 26; u < L - 10; u += 34) f.tree(u + ((int) v % 2) * 4, v, 8, 0xFF3E7034);
                }
                // The maypole: blue and white with garlands.
                float mu = L - 16, mv = Math.min(g1 - 10, g0 + 14);
                if (S - b[3] > 60) {
                    f.circle(0x30000000, mu + 2, mv + 2, 8);
                    f.ring(0xFF3E7A34, 1.4f, mu, mv, 8);
                    for (int k = 0; k < 12; k++) {
                        double a = k * Math.PI / 6;
                        f.line(k % 2 == 0 ? 0xFF3A6AC8 : 0xFFF4F4F0, 0.6f, mu, mv, mu + (float) Math.cos(a) * 8, mv + (float) Math.sin(a) * 8);
                    }
                    f.circle(0xFF3A6AC8, mu, mv, 2.2f);
                    f.circle(0xFFF4F4F0, mu, mv, 1.2f);
                    f.circle(0xFFE8C860, mu, mv, 0.6f);
                }
                // The serving kiosk by the gate.
                float ku = 8, kv = S - 18;
                F k = new F(c, p, f.x(ku, kv), f.y(ku, kv), f.x(ku + 16, kv + 10), f.y(ku + 16, kv + 10));
                k.sh(0, 0, k.L, k.S, 1.6f);
                gable(k, 0xFF7A4A30, 0, 0, k.L, k.S);
                f.festoon(26, g0, L - 26, g0, 2.5f, 14, false);
                f.festoon(26, g1, L - 26, g1, 2.5f, 14, false);
                f.sign(city, "BIERHALLE", L / 2, b[3] + 4, 6, 0xFF3A6AC8);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                // A steep red-brown tiled gable with timber bargeboards.
                gable(f, 0xFF8A4A34, 0, 0, L, S);
                for (float v = 2; v < S; v += 2) f.line(0x1A000000, 0.4f, 0, v, L, v);
                f.rect(0xFF4A2E1E, 0, 0, 2, S);
                f.rect(0xFF4A2E1E, L - 2, 0, L, S);
                // Dormers down both slopes.
                for (float u = 10; u < L - 8; u += 16) {
                    if (Math.abs(u - L / 2) < 8) continue;
                    f.sh(u - 3, 2, u + 3, S * 0.3f, 1);
                    f.rect(0xFF9A5A40, u - 3, 2, u + 3, S * 0.3f);
                    f.rect(0xFFF0E8D8, u - 2, 2, u + 2, 3.2f);
                    f.sh(u - 3, S * 0.7f, u + 3, S - 2, 1);
                    f.rect(0xFF6A3828, u - 3, S * 0.7f, u + 3, S - 2);
                    f.rect(0xFFE0D8C8, u - 2, S - 3.2f, u + 2, S - 2);
                }
                // The bell turret with an onion dome.
                float cu = L / 2, cv = S / 2, s = Math.min(6, S * 0.18f);
                f.sh(cu - s, cv - s, cu + s, cv + s, 2.5f);
                f.rect(0xFFF0E8D8, cu - s, cv - s, cu + s, cv + s);
                f.circle(0xFF2E7A6A, cu, cv, s * 0.85f);
                f.circle(0xFF4A9A88, cu - s * 0.2f, cv - s * 0.2f, s * 0.5f);
                f.circle(0xFFE8C860, cu, cv, 0.9f);
                f.rect(0xFF6A6A6A, L * 0.2f, cv - 2.5f, L * 0.2f + 4, cv + 2.5f);
                f.rect(0xFF6A6A6A, L * 0.8f, cv - 2.5f, L * 0.8f + 4, cv + 2.5f);
            }
        };
        d.ground = City.DIRT;
        d.districts = Variants.OLD | Variants.MID | Variants.UNI | Variants.PARK;
        d.minS = 10;
        d.minL = 13;
        d.base = City.SHOP;
        d.inside = Variants.I_BAR;
        d.roofCol = 0xFF8A4A34;
        out.add(d);
    }

    // ------------------------------------------------------------------ 13 Wine Bar Courtyard

    static int[][] wineUV(int L, int S) {
        return new int[][]{{1, 1, Math.max(4, L * 35 / 100), S - 2, 2}};
    }

    static void wineBar(List<Landmarks.Def> out) {
        // An old stone wine bar beside a walled courtyard: a vine pergola over barrel tables, and rows of vines.
        Landmarks.Def d = new Landmarks.Def("Wine Bar Courtyard", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return orient(wineUV(Math.max(bw, bh), Math.min(bw, bh)), bw >= bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] b = box(wineUV(lt(f), st(f))[0]);
                f.rect(0xFFC8BC9E, 0, 0, L, S);
                float u0 = b[2] + 4, u1 = L - 3, v0 = 3, v1 = S - 3;
                // Flagstones of mixed sizes.
                f.rect(0xFFD6C8A8, u0, v0, u1, v1);
                for (float v = v0; v < v1; v += 7) {
                    float u = u0 + (((int) (v / 7)) % 2) * 3;
                    while (u < u1) {
                        float w = 5 + r.nextFloat() * 5;
                        f.line(0x26000000, 0.5f, u, v, u, Math.min(v + 7, v1));
                        u += w;
                    }
                    f.line(0x26000000, 0.5f, u0, v, u1, v);
                }
                // The vineyard rows at the far end.
                float vu = u0 + (u1 - u0) * 0.62f;
                f.rect(0xFF8A7050, vu, v0, u1, v1);
                for (float u = vu + 5; u < u1 - 2; u += 7) {
                    f.line(0xFF5A4030, 0.6f, u, v0 + 4, u, v1 - 4);
                    for (float v = v0 + 5; v < v1 - 4; v += 3.4f) {
                        f.circle(0xFF4E8A3A, u + ((int) v % 2 == 0 ? 0.7f : -0.7f), v, 1.6f);
                        if (((int) (v * 3 + u)) % 5 == 0) f.circle(0xFF5A2A5A, u + 1, v + 0.8f, 0.7f);
                    }
                }
                // Barrel tables with stools under a pergola of vines.
                float pu1 = vu - 4;
                for (float v = v0 + 10; v < v1 - 6; v += 14) {
                    for (float u = u0 + 9; u < pu1 - 4; u += 14) {
                        for (int k = 0; k < 3; k++) {
                            double a = k * 2 * Math.PI / 3 + 0.4;
                            f.circle(0xFF3A2A20, u + (float) Math.cos(a) * 4.2f, v + (float) Math.sin(a) * 4.2f, 1.1f);
                        }
                        f.circle(0x38000000, u + 1, v + 1, 2.8f);
                        f.circle(0xFF8A5A34, u, v, 2.8f);
                        f.ring(0xFF4A4A4A, 0.4f, u, v, 2.1f);
                        f.circle(0xFF7A1A2A, u - 0.6f, v - 0.6f, 0.6f);
                    }
                }
                for (float u = u0 + 2; u <= pu1; u += 9) f.line(0xFF5A3A24, 1.1f, u, v0 + 2, u, v1 - 2);
                f.line(0xFF4A2E1E, 1.4f, u0 + 2, v0 + 4, pu1, v0 + 4);
                f.line(0xFF4A2E1E, 1.4f, u0 + 2, v1 - 4, pu1, v1 - 4);
                for (int k = 0; k < 70; k++) {
                    float u = u0 + 2 + r.nextFloat() * (pu1 - u0 - 2), v = v0 + 2 + r.nextFloat() * (v1 - v0 - 4);
                    float near = Math.min(Math.abs(v - v0 - 4), Math.abs(v - v1 + 4));
                    if (near > 8 && k % 3 != 0) continue;
                    f.circle(k % 2 == 0 ? 0xFF4E8A3A : 0xFF6AA04A, u, v, 1.4f + r.nextFloat());
                    if (k % 5 == 0) f.circle(0xFF5A2A5A, u + 0.8f, v + 0.8f, 0.8f);
                }
                // The stone well and an olive tree.
                float wu = (u0 + pu1) / 2, wv = (v0 + v1) / 2;
                f.circle(0x38000000, wu + 1.5f, wv + 1.5f, 4.5f);
                f.circle(0xFFB8A888, wu, wv, 4.5f);
                f.circle(0xFF2E4A5A, wu, wv, 3);
                f.line(0xFF5A3A24, 0.8f, wu - 4.5f, wv, wu + 4.5f, wv);
                f.tree(u1 - 6, v1 - 6, 5, 0xFF7A9060);
                // The courtyard wall with its gate.
                int wc = 0xFFB0A080;
                float gv = (v0 + v1) / 2;
                f.line(wc, 2.5f, u0, v0, u1, v0);
                f.line(wc, 2.5f, u0, v1, (u0 + u1) / 2 - 6, v1);
                f.line(wc, 2.5f, (u0 + u1) / 2 + 6, v1, u1, v1);
                f.line(wc, 2.5f, u1, v0, u1, v1);
                f.line(0x60FFFFFF, 0.6f, u0, v0 - 0.8f, u1, v0 - 0.8f);
                f.circle(0xFFE8B850, (u0 + u1) / 2 - 7, v1, 1);
                f.circle(0xFFE8B850, (u0 + u1) / 2 + 7, v1, 1);
                f.lamp(b[2] + 2, gv);
                f.sign(city, "WINE BAR", (b[0] + b[2]) / 2, b[3] - 8, 5, 0xFF7A1A2A);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                // Roman tiles: rows of half-round tiles down the two slopes, ivy creeping over one end.
                f.rect(0xFFB0603E, 0, 0, L, S / 2);
                f.rect(0xFF8A4A30, 0, S / 2, L, S);
                for (float u = 1.2f; u < L; u += 2.6f) {
                    f.line(0x40FFE0C0, 1f, u, 0, u, S / 2);
                    f.line(0x30000000, 1f, u + 1.3f, 0, u + 1.3f, S);
                }
                for (float v = 3; v < S; v += 3) f.line(0x18000000, 0.4f, 0, v, L, v);
                f.line(0xFFC8805A, 1.6f, 0, S / 2, L, S / 2);
                f.frame(0xFF6A3A24, 0.6f, 0, 0, L, S);
                for (int k = 0; k < 24; k++) {
                    float u = r.nextFloat() * L * 0.25f, v = r.nextFloat() * S;
                    f.circle(k % 2 == 0 ? 0xFF3E6E30 : 0xFF528A3A, u, v, 1.2f + r.nextFloat() * 1.6f);
                }
                float cu = L * 0.7f, cv = S * 0.35f;
                f.sh(cu - 3, cv - 3, cu + 3, cv + 3, 2.5f);
                f.rect(0xFFC8B898, cu - 3, cv - 3, cu + 3, cv + 3);
                f.rect(0xFF8A4A30, cu - 3.6f, cv - 1.2f, cu + 3.6f, cv + 1.2f);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.OLD | Variants.MID | Variants.DOWN;
        d.minS = 9;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_BAR;
        d.roofCol = 0xFFB0603E;
        out.add(d);
    }

    // ------------------------------------------------------------------ 14 Nightclub

    static int[][] clubUV(int L, int S) {
        return new int[][]{{2, 1, L - 4, Math.max(4, Math.min(S - 5, S * 6 / 10)), 2}};
    }

    static void nightclub(List<Landmarks.Def> out) {
        // A windowless black box edged in neon, a roped zigzag queue to the door, searchlights and a smoking deck.
        Landmarks.Def d = new Landmarks.Def("Nightclub", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return orient(clubUV(Math.max(bw, bh), Math.min(bw, bh)), bw >= bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] b = box(clubUV(lt(f), st(f))[0]);
                f.rect(0xFF2C2C32, 0, 0, L, S);
                for (int k = 0; k < 30; k++) f.circle(0x10FFFFFF, r.nextFloat() * L, r.nextFloat() * S, 0.5f + r.nextFloat() * 2);
                float fv = b[3], du = b[0] + (b[2] - b[0]) * 0.62f;
                f.rect(0xFF3E3E46, 0, fv, L, fv + 8);
                // Searchlight beams sweeping across the forecourt.
                f.poly(0x22FFFFFF, 6, S - 4, du - 10, fv + 2, du - 30, fv + 2);
                f.poly(0x22FFFFFF, L - 6, S - 4, du + 10, fv + 2, du + 30, fv + 2);
                f.circle(0xFF8A8A90, 6, S - 4, 2.4f);
                f.circle(0xFFF4F4E8, 6, S - 4, 1.3f);
                f.circle(0xFF8A8A90, L - 6, S - 4, 2.4f);
                f.circle(0xFFF4F4E8, L - 6, S - 4, 1.3f);
                // The red carpet and the door canopy.
                f.rect(0xFFA0182A, du - 4, fv, du + 4, fv + 14);
                f.sh(du - 9, fv, du + 9, fv + 6, 1.8f);
                f.rect(0xFF101014, du - 9, fv, du + 9, fv + 6);
                f.neon(0xFFFF3AC8, du - 8.5f, fv + 0.5f, du + 8.5f, fv + 5.5f, 1);
                // The roped zigzag queue.
                float q0 = du - 10, lanes = Math.min(4, (S - fv - 18) / 6);
                float qu0 = b[0] + 6;
                for (int k = 0; k <= lanes; k++) {
                    float v = fv + 12 + k * 6;
                    float a0 = k == 0 ? qu0 + 8 : qu0, a1 = k == lanes ? q0 - 8 : q0;
                    for (float u = a0; u <= a1 + 0.1f; u += (a1 - a0) / Math.max(1, Math.round((a1 - a0) / 9))) f.circle(0xFFD8D8E0, u, v, 0.9f);
                    f.line(0xFFB8182E, 0.8f, a0, v, a1, v);
                }
                for (int k = 0; k < lanes * 4; k++) {
                    float v = fv + 15 + (k / 4) * 6, u = qu0 + 6 + (k % 4) * ((q0 - qu0 - 12) / 3) + ((k * 7) % 3);
                    f.circle(0x40000000, u + 0.8f, v + 0.8f, 1.6f);
                    f.circle(BRIGHT[(k * 3) % BRIGHT.length], u, v, 1.5f);
                    f.circle(0xFF2A1A14, u, v, 0.9f);
                }
                // The bouncer at the rope.
                f.circle(0xFF101010, du - 6, fv + 9, 1.8f);
                f.circle(0xFF3A2A20, du - 6, fv + 9, 1f);
                // A smoking deck at the end with neon palms.
                float su0 = b[2] + 2, su1 = L - 2;
                if (su1 - su0 > 10) {
                    f.rect(0xFF4A3A30, su0, b[1] + 6, su1, b[3] - 6);
                    for (float v = b[1] + 8; v < b[3] - 6; v += 2.2f) f.line(0x30000000, 0.4f, su0, v, su1, v);
                    f.neon(0xFF3AF0FF, su0 + 0.5f, b[1] + 6.5f, su1 - 0.5f, b[3] - 6.5f, 1);
                    for (float v = b[1] + 14; v < b[3] - 10; v += 14) f.circle(0xFF8A8A90, (su0 + su1) / 2, v, 1.8f);
                }
                f.palm(b[0] - 7, fv + 4, 5);
                f.palm(b[2] + 7, fv + 4, 5);
                // A taxi at the kerb and neon arrows painted on the ground.
                f.car(L - 30, S - 5, 0, 0xFFF0C030, 5.2f);
                for (int k = 0; k < 3; k++) {
                    float u = du + 14 + k * 7;
                    f.neonLine(0xFFFF3AC8, u, fv + 12, u + 3, fv + 10);
                    f.neonLine(0xFFFF3AC8, u + 3, fv + 10, u, fv + 8);
                }
                f.sign(city, "CLUB", du, fv + 3, 6, 0xFFFF3AC8);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF2A2A30, 0, 0, L, S);
                f.rect(0xFF17171B, 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                for (float u = 6; u < L - 2; u += 6) f.line(0x0CFFFFFF, 0.5f, u, 1.5f, u, S - 1.5f);
                // A dot-matrix display panel showing a wave.
                float pu0 = L * 0.12f, pu1 = L * 0.62f, pv0 = S * 0.22f, pv1 = S * 0.78f;
                f.rect(0xFF0A0A0E, pu0, pv0, pu1, pv1);
                for (float u = pu0 + 2; u < pu1 - 1; u += 2.6f) {
                    float t = (u - pu0) / (pu1 - pu0);
                    float wv = (pv0 + pv1) / 2 + (float) Math.sin(t * Math.PI * 3) * (pv1 - pv0) * 0.3f;
                    for (float v = pv0 + 2; v < pv1 - 1; v += 2.6f) {
                        float dv = Math.abs(v - wv);
                        int col = dv < 2.6f ? 0xFFFF3AC8 : (dv < 5.2f ? 0xFF3AF0FF : 0xFF26262E);
                        f.circle(col, u, v, 0.75f);
                    }
                }
                // Big extract fans.
                for (int k = 0; k < 2; k++) {
                    float u = L * (0.74f + 0.14f * k), v = k == 0 ? S * 0.3f : S * 0.68f, rr = Math.min(6, S * 0.16f);
                    f.circle(0x50000000, u + 1.5f, v + 1.5f, rr);
                    f.circle(0xFF46464E, u, v, rr);
                    f.circle(0xFF24242A, u, v, rr - 1);
                    for (int j = 0; j < 4; j++) {
                        double a = j * Math.PI / 2 + 0.4;
                        f.line(0xFF5A5A62, 1.2f, u, v, u + (float) Math.cos(a) * (rr - 1.4f), v + (float) Math.sin(a) * (rr - 1.4f));
                    }
                    f.circle(0xFF6A6A72, u, v, 1);
                }
                // Neon round the edge.
                f.neon(0xFFFF3AC8, 2.5f, 2.5f, L - 2.5f, S - 2.5f, 2);
                f.neonLine(0xFF3AF0FF, L * 0.68f, 5, L * 0.68f, S - 5);
                f.sign(city, "NIGHTCLUB", L * 0.37f, S / 2, 7, 0xFFFF3AC8);
            }
        };
        d.ground = City.LOT;
        d.districts = Variants.DOWN | Variants.MID | Variants.IND;
        d.minS = 10;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_BAR;
        d.roofCol = 0xFF17171B;
        out.add(d);
    }

    // ------------------------------------------------------------------ 15 Casino

    static int[][] casinoUV(int L, int S) {
        return new int[][]{{1, 1, L - 2, Math.max(4, Math.min(S - 6, S * 55 / 100)), 4}};
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

    static void casino(List<Landmarks.Def> out) {
        // A gilded palace of chance: a gold dome and turrets, a porte-cochère over the drive and a dancing fountain.
        Landmarks.Def d = new Landmarks.Def("Casino", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return orient(casinoUV(Math.max(bw, bh), Math.min(bw, bh)), bw >= bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] b = box(casinoUV(lt(f), st(f))[0]);
                f.rect(0xFF5E9446, 0, 0, L, S);
                f.rect(0xFFD8CCB0, 0, 0, L, b[1] - 3);
                float fv = b[3], cu = L / 2;
                // The drive loop round the fountain island.
                float du0 = L * 0.16f, du1 = L * 0.84f, dv0 = fv + 4, dv1 = S - 3;
                f.roundRect(0xFF4A4A4E, du0, dv0, du1, dv1, (dv1 - dv0) / 2);
                f.rring(0xFFE8DCC0, 1, du0, dv0, du1, dv1, (dv1 - dv0) / 2);
                float iu0 = du0 + 14, iu1 = du1 - 14, iv0 = dv0 + 12, iv1 = dv1 - 12;
                float iv = (iv0 + iv1) / 2, ih = Math.max(4, (iv1 - iv0) / 2);
                f.roundRect(0xFF6AA050, iu0, iv - ih, iu1, iv + ih, ih);
                for (float u = iu0 + ih; u < iu1 - ih; u += 7) {
                    if (Math.abs(u - cu) < ih + 4) continue;
                    f.circle(0xFFB8283A, u, iv, 2);
                    f.circle(0xFFE84A5A, u - 0.5f, iv - 0.5f, 0.9f);
                }
                float fr = Math.min(ih + 3, 16);
                f.circle(0x38000000, cu + 1.5f, iv + 1.5f, fr);
                f.circle(0xFFE8DCC0, cu, iv, fr);
                f.circle(0xFF2E7EA8, cu, iv, fr - 1.5f);
                f.circle(0xFF4A9AC4, cu, iv, fr - 3.5f);
                for (int k = 0; k < 12; k++) {
                    double an = k * Math.PI / 6;
                    float ju = cu + (float) Math.cos(an) * (fr - 4), jv = iv + (float) Math.sin(an) * (fr - 4);
                    f.circle(0x80FFFFFF, ju, jv, 1.4f);
                    f.circle(0xF0FFFFFF, ju, jv, 0.6f);
                }
                f.circle(0x90FFFFFF, cu, iv, fr * 0.35f);
                f.circle(0xFFFFFFFF, cu, iv, fr * 0.15f);
                // Palms round the lawns and red flower beds by the road.
                for (float u = 8; u < du0 - 4; u += 12) f.palm(u, (fv + S) / 2, 5);
                for (float u = L - 8; u > du1 + 4; u -= 12) f.palm(u, (fv + S) / 2, 5);
                for (float u = 8; u < L - 4; u += 22) {
                    if (u > du0 - 6 && u < du1 + 6) continue;
                    f.roundRect(0xFFB8283A, u - 4, S - 6, u + 4, S - 2, 2);
                }
                // The porte-cochère: a red carpet under a gilded canopy, a limousine waiting.
                f.rect(0xFFA0182A, cu - 5, fv, cu + 5, dv0 + 2);
                f.car(cu + 30, dv0 + 6, 0, 0xFF1E1E22, 8.5f);
                float pc0 = cu - 24, pc1 = cu + 24, pv1 = fv + 18;
                f.rect(0x48000000, pc0 + 4, fv + 4, pc1 + 4, pv1 + 4);
                f.rect(0xFF8A6A2A, pc0, fv, pc1, pv1);
                f.rect(0xFFD8B050, pc0 + 1, fv, pc1 - 1, pv1 - 1);
                f.rect(0xFFF0D070, pc0 + 1, fv, pc1 - 1, fv + 4);
                for (float u = pc0 + 3; u < pc1 - 2; u += 3) f.circle(0xFFFFF4C0, u, pv1 - 1.8f, 0.5f);
                f.circle(0xFFF4ECD8, pc0 + 2, pv1 - 2, 1.4f);
                f.circle(0xFFF4ECD8, pc1 - 2, pv1 - 2, 1.4f);
                for (float u = 6; u < L - 2; u += 30) f.lamp(u, b[1] - 6);
                f.sign(city, "CASINO", cu, fv + 8, 7, 0xFF8A1A2A);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                // Gilded tiers stepping up to a gold dome, with turrets on the corners.
                int[] g = {0xFF9A7A30, 0xFFC8A040, 0xFFB08A36, 0xFFDDB850};
                for (int k = 0; k < 4; k++) {
                    float in = k * Math.min(S, L) * 0.08f;
                    if (k > 0) f.sh(in, in, L - in, S - in, 1.5f);
                    f.rect(g[k], in, in, L - in, S - in);
                    f.rect(City.lighten(g[k], 0.18f), in, in, L - in, in + 1.2f);
                    f.rect(City.darken(g[k], 0.8f), in, S - in - 1.2f, L - in, S - in);
                }
                float in = Math.min(S, L) * 0.08f;
                for (float u = in * 2; u < L - in * 2; u += 5) {
                    f.circle(0xFFFFF0B0, u, in * 0.5f, 0.6f);
                    f.circle(0xFFFFF0B0, u, S - in * 0.5f, 0.6f);
                }
                // Red glass skylights along the upper tier.
                for (float u = in * 3 + 2; u < L - in * 3 - 6; u += 10) {
                    if (Math.abs(u + 3 - L / 2) < S * 0.36f) continue;
                    f.rect(0xFF8A1A2A, u, S / 2 - 3, u + 6, S / 2 + 3);
                    f.rect(0xFFC0384A, u, S / 2 - 3, u + 6, S / 2 - 1);
                }
                float rr = S * 0.3f;
                f.circle(0x50000000, L / 2 + 3, S / 2 + 3, rr + 1);
                f.circle(0xFF8A6A2A, L / 2, S / 2, rr + 1);
                dome(f, L / 2, S / 2, rr, 0xFFD8B050, 16);
                f.circle(0xFFF8E8A8, L / 2 - rr * 0.35f, S / 2 - rr * 0.35f, rr * 0.18f);
                f.circle(0xFFB8283A, L / 2, S / 2, rr * 0.12f);
                float t = Math.min(6, S * 0.12f);
                float[][] cs = {{t, t}, {L - t, t}, {t, S - t}, {L - t, S - t}};
                for (float[] q : cs) {
                    f.circle(0x50000000, q[0] + 1.5f, q[1] + 1.5f, t);
                    f.circle(0xFFF2E8D4, q[0], q[1], t);
                    dome(f, q[0], q[1], t - 1, 0xFFD8B050, 8);
                }
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.DOWN | Variants.MID;
        d.minS = 10;
        d.minL = 14;
        d.base = City.OFFICE;
        d.inside = Variants.I_ARCADE;
        d.roofCol = 0xFFC8A040;
        out.add(d);
    }

    // ------------------------------------------------------------------ 16 Retro Arcade

    static int[][] arcadeUV(int L, int S) {
        return new int[][]{{1, 1, L - 2, Math.max(4, Math.min(S - 4, S * 6 / 10)), 1}};
    }

    static final String[] INVADER = {"00100000100", "00010001000", "00111111100", "01101110110", "11111111111", "10111111101",
            "10100000101", "00011011000"};

    static void arcade(List<Landmarks.Def> out) {
        // A games arcade whose roof is a giant pixel-art screen: a chomping yellow hero, a ghost and an invader.
        Landmarks.Def d = new Landmarks.Def("Retro Arcade", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return orient(arcadeUV(Math.max(bw, bh), Math.min(bw, bh)), bw >= bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] b = box(arcadeUV(lt(f), st(f))[0]);
                f.rect(0xFFB8B4AE, 0, 0, L, S);
                grid(f, 0x14000000, 0.5f, 0, 0, L, S, 8);
                float fv = b[3], cu = L / 2;
                // A black-and-white checkerboard entrance.
                float q = 5;
                for (int i = 0; i < 8; i++)
                    for (int j = 0; j * q < S - fv - 6 && j < 6; j++)
                        f.rect((i + j) % 2 == 0 ? 0xFF1E1E22 : 0xFFF2F0EA, cu - 4 * q + i * q, fv + j * q, cu - 3 * q + i * q, fv + (j + 1) * q);
                f.sh(cu - 12, fv, cu + 12, fv + 5, 1.5f);
                f.rect(0xFF2A1E4A, cu - 12, fv, cu + 12, fv + 5);
                f.neon(0xFF3AF0FF, cu - 11.5f, fv + 0.5f, cu + 11.5f, fv + 4.5f, 1);
                // A claw machine and a kiddie ride outside, neon palms and a bike rack.
                float ku = cu + 26, kv = fv + 8;
                if (ku + 10 < L) {
                    f.sh(ku, kv, ku + 7, kv + 7, 1.2f);
                    f.rect(0xFFD84A9A, ku, kv, ku + 7, kv + 7);
                    f.rect(0xC0B8E8F8, ku + 1, kv + 1, ku + 6, kv + 5);
                    for (int k = 0; k < 4; k++) f.circle(BRIGHT[k], ku + 2 + (k % 2) * 2.5f, kv + 2 + (k / 2) * 2, 0.8f);
                    f.sh(ku + 12, kv, ku + 20, kv + 5, 1);
                    f.roundRect(0xFFE8B83A, ku + 12, kv, ku + 20, kv + 5, 2);
                    f.circle(0xFF3A7AC8, ku + 15, kv + 2.5f, 1.3f);
                }
                float bu = cu - 34;
                for (int k = 0; k < 4 && bu > 6; k++) {
                    float u = bu - k * 5;
                    f.line(0xFF3A3A3A, 0.5f, u, fv + 6, u, fv + 12);
                    f.ring(0xFF2A2A2A, 0.5f, u, fv + 7, 1.4f);
                    f.ring(0xFF2A2A2A, 0.5f, u, fv + 11, 1.4f);
                }
                for (int k = 0; k < 2; k++) {
                    float u = k == 0 ? 10 : L - 10, v = S - 9;
                    f.palm(u, v, 6);
                    f.ring(0xFFFF3AC8, 0.6f, u, v, 2);
                }
                f.sign(city, "ARCADE", cu, fv + 2.5f, 5, 0xFF3AF0FF);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int bg = 0xFF241A42;
                f.rect(0xFF14102A, 0, 0, L, S);
                f.rect(bg, 2, 2, L - 2, S - 2);
                for (int k = 0; k < 30; k++) f.circle(0x80FFFFFF, 4 + r.nextFloat() * (L - 8), 4 + r.nextFloat() * (S - 8), 0.4f);
                float m = S / 2, R = S * 0.24f;
                // The yellow hero chomping along a line of pellets.
                float pu = Math.max(L * 0.16f, R + 9);
                for (float u = pu + R + 4; u < L * 0.52f - R; u += 6) f.rect(0xFFF4E6C8, u - 1, m - 1, u + 1, m + 1);
                f.circle(0x40000000, pu + 1.5f, m + 1.5f, R);
                f.circle(0xFFF4D030, pu, m, R);
                f.poly(bg, pu, m, pu + R * 1.2f, m - R * 0.75f, pu + R * 1.2f, m + R * 0.75f);
                f.circle(0xFF1E1E22, pu, m - R * 0.5f, R * 0.12f);
                // The ghost.
                float gu = L * 0.52f, gr = R * 0.9f;
                f.circle(0xFFE84A6A, gu, m - gr * 0.2f, gr);
                f.rect(0xFFE84A6A, gu - gr, m - gr * 0.2f, gu + gr, m + gr);
                for (int k = 0; k < 3; k++) {
                    float a = gu - gr + k * gr * 2 / 3;
                    f.poly(bg, a, m + gr + 0.2f, a + gr / 3, m + gr * 0.62f, a + gr * 2 / 3, m + gr + 0.2f);
                }
                for (int k = -1; k <= 1; k += 2) {
                    f.circle(0xFFF4F4F4, gu + k * gr * 0.38f, m - gr * 0.25f, gr * 0.28f);
                    f.circle(0xFF2A4AC8, gu + k * gr * 0.38f + gr * 0.1f, m - gr * 0.2f, gr * 0.13f);
                }
                // The pixel invader.
                float px = Math.min(S * 0.55f / 8, L * 0.22f / 11), iu = L * 0.82f - 5.5f * px, iv = m - 4 * px;
                for (int j = 0; j < 8; j++)
                    for (int i = 0; i < 11; i++)
                        if (INVADER[j].charAt(i) == '1') {
                            f.rect(0xFF2A8A2A, iu + i * px + 0.4f, iv + j * px + 0.4f, iu + (i + 1) * px + 0.4f, iv + (j + 1) * px + 0.4f);
                            f.rect(0xFF5AF05A, iu + i * px, iv + j * px, iu + (i + 1) * px - 0.3f, iv + (j + 1) * px - 0.3f);
                        }
                // Neon tubes round the edge, a score bar of blocks.
                f.neon(0xFFFF3AC8, 3, 3, L - 3, S - 3, 2);
                f.neon(0xFF3AF0FF, 6, 6, L - 6, S - 6, 1.5f);
                for (int k = 0; k < 6; k++) f.rect(BRIGHT[k], 9 + k * 4, 8.5f, 12 + k * 4, 10.5f);
                f.sign(city, "ARCADE", L / 2, S - 9, 6, 0xFF3AF0FF);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.MID | Variants.SUB | Variants.UNI;
        d.minS = 9;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_ARCADE;
        d.roofCol = 0xFF241A42;
        out.add(d);
    }

    // ------------------------------------------------------------------ 17 Record & Comic Row

    static int[][] recordUV(int L, int S) {
        int n = L >= 14 ? 3 : 2, w = (L - 2 - (n - 1)) / n, lv = Math.max(4, Math.min(S - 4, S * 6 / 10));
        int[][] a = new int[n][];
        for (int k = 0; k < n; k++) a[k] = new int[]{1 + k * (w + 1), 1, w, lv, 2};
        return a;
    }

    static void recordRow(List<Landmarks.Def> out) {
        // A short row of shops with roofs painted as their own signs: a giant vinyl record, a comic POW and a cassette.
        Landmarks.Def d = new Landmarks.Def("Record & Comic Row", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return orient(recordUV(Math.max(bw, bh), Math.min(bw, bh)), bw >= bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] pl = recordUV(lt(f), st(f));
                f.rect(0xFFC4BEB2, 0, 0, L, S);
                grid(f, 0x14000000, 0.5f, 0, 0, L, S, 8);
                f.rect(0xFF8A9070, 0, 0, L, T - 3);
                for (float u = 8; u < L - 4; u += 20) bin(f, u, 6, 0xFF4A4A50);
                for (int k = 0; k < pl.length; k++) {
                    float[] b = box(pl[k]);
                    // Alleys between the shops, splashed with mural paint.
                    if (k > 0) {
                        float a0 = box(pl[k - 1])[2], a1 = b[0];
                        f.rect(0xFF9A948A, a0, b[1], a1, b[3]);
                        for (int j = 0; j < 6; j++) {
                            float v = b[1] + 6 + j * (b[3] - b[1] - 12) / 5;
                            f.circle(BRIGHT[(j + k * 3) % BRIGHT.length], a0 + 2 + (j % 2) * (a1 - a0 - 4), v, 2.2f);
                        }
                    }
                    float fv = b[3], cu = (b[0] + b[2]) / 2;
                    // Each shop's pavement display.
                    if (k == 0) {
                        for (int j = 0; j < 3; j++) {
                            float u = b[0] + 6 + j * 9;
                            if (u + 7 > b[2]) break;
                            f.sh(u, fv + 4, u + 7, fv + 10, 1);
                            f.rect(0xFF8A6A44, u, fv + 4, u + 7, fv + 10);
                            for (float q = u + 1; q < u + 7; q += 1.2f) f.line(BRIGHT[(int) (q * 3) % BRIGHT.length], 0.5f, q, fv + 4.6f, q, fv + 9.4f);
                        }
                    } else if (k == 1) {
                        for (int j = 0; j < 2; j++) {
                            float u = b[0] + 8 + j * 14;
                            if (u + 4 > b[2]) break;
                            f.circle(0x34000000, u + 1, fv + 9, 4);
                            for (int s = 0; s < 6; s++) {
                                double a0 = s * Math.PI / 3, a1 = a0 + Math.PI / 3;
                                f.poly(BRIGHT[(s + j) % BRIGHT.length], u, fv + 8, u + (float) Math.cos(a0) * 4, fv + 8 + (float) Math.sin(a0) * 4,
                                        u + (float) Math.cos(a1) * 4, fv + 8 + (float) Math.sin(a1) * 4);
                            }
                            f.circle(0xFF3A3A3A, u, fv + 8, 0.8f);
                        }
                    } else {
                        f.sh(cu - 6, fv + 4, cu + 6, fv + 8, 1);
                        f.rect(0xFF3A3A40, cu - 6, fv + 4, cu + 6, fv + 8);
                        for (float q = cu - 5; q < cu + 5; q += 2) f.rect(BRIGHT[(int) (q + 50) % BRIGHT.length], q, fv + 4.6f, q + 1.4f, fv + 7.4f);
                    }
                    f.rect(0xFF2A2A2A, cu - 3, fv, cu + 3, fv + 1.2f);
                    f.tree(cu, S - 7, 4.5f);
                }
                // A poster pillar, planters, a bench, a bike and lamps along the kerb.
                float lastFv = box(pl[0])[3];
                if (S - lastFv > 40) {
                    float pu = L * 0.62f, pv = (lastFv + S) / 2 - 2;
                    f.circle(0x40000000, pu + 1.5f, pv + 1.5f, 4.5f);
                    f.circle(0xFF2E5A3A, pu, pv, 4.5f);
                    for (int j = 0; j < 8; j++) {
                        double a = j * Math.PI / 4;
                        f.circle(BRIGHT[j], pu + (float) Math.cos(a) * 3.6f, pv + (float) Math.sin(a) * 3.6f, 1.1f);
                    }
                    f.circle(0xFF3E6A4A, pu, pv, 2.4f);
                    for (float u = 12; u < L * 0.5f; u += 16) {
                        f.rect(0xFF8A8478, u - 5, pv - 2.5f, u + 5, pv + 2.5f);
                        for (float q = u - 4; q < u + 5; q += 2.2f) f.circle(FLOWERS[(int) (q * 3) % FLOWERS.length], q, pv, 1.2f);
                    }
                }
                f.bench(L / 2, S - 15, true);
                f.lamp(10, S - 4);
                f.lamp(L - 10, S - 4);
                float bu = L * 0.3f, bv = S - 15;
                f.ring(0xFF2A2A2A, 0.6f, bu - 2.6f, bv, 1.8f);
                f.ring(0xFF2A2A2A, 0.6f, bu + 2.6f, bv, 1.8f);
                f.line(0xFFD8443A, 0.8f, bu - 2.6f, bv, bu + 2.6f, bv);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, cu = L / 2, cv = S / 2, R = Math.min(L, S) * 0.42f;
                if (part == 0) {
                    // RECORDS: a giant vinyl disc on a black roof.
                    f.rect(0xFF3A3A40, 0, 0, L, S);
                    f.rect(0xFF2A2A30, 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                    f.circle(0x50000000, cu + 2, cv + 2, R);
                    f.circle(0xFF121214, cu, cv, R);
                    for (float rr = R - 1.5f; rr > R * 0.38f; rr -= 1.6f) f.ring(0xFF26262A, 0.4f, cu, cv, rr);
                    f.poly(0x26FFFFFF, cu, cv, cu - R * 0.9f, cv - R * 0.42f, cu - R * 0.42f, cv - R * 0.9f);
                    f.poly(0x1AFFFFFF, cu, cv, cu + R * 0.9f, cv + R * 0.42f, cu + R * 0.42f, cv + R * 0.9f);
                    f.circle(0xFFD8443A, cu, cv, R * 0.33f);
                    f.circle(0xFFE8B83A, cu, cv, R * 0.2f);
                    f.circle(0xFFF2F0EA, cu, cv, R * 0.05f);
                    f.sign(city, "RECORDS", cu, cv, 5, 0xFFD8443A);
                } else if (part == 1) {
                    // COMICS: halftone yellow with a red starburst and a speech bubble.
                    f.rect(0xFFB89020, 0, 0, L, S);
                    f.rect(0xFFF0C838, 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                    for (float u = 4; u < L - 2; u += 4)
                        for (float v = 4 + ((int) (u / 4) % 2) * 2; v < S - 2; v += 4) f.circle(0x30E07020, u, v, 0.9f);
                    float[] st = new float[32], in = new float[32];
                    for (int k = 0; k < 16; k++) {
                        double a = k * Math.PI / 8;
                        float rr = k % 2 == 0 ? R : R * 0.6f;
                        st[2 * k] = cu + (float) Math.cos(a) * rr;
                        st[2 * k + 1] = cv + (float) Math.sin(a) * rr;
                        in[2 * k] = cu + (float) Math.cos(a) * rr * 0.7f;
                        in[2 * k + 1] = cv + (float) Math.sin(a) * rr * 0.7f;
                    }
                    float[] sh = st.clone();
                    for (int k = 0; k < 32; k++) sh[k] += 2;
                    f.poly(0x40000000, sh);
                    f.poly(0xFFD8302A, st);
                    f.poly(0xFFF8E060, in);
                    float ex = f.x(cu, cv), ey = f.y(cu, cv);
                    line(c, p, 0xFF1E1E22, R * 0.14f, ex, ey - R * 0.32f, ex, ey + R * 0.08f);
                    circle(c, p, 0xFF1E1E22, ex, ey + R * 0.25f, R * 0.08f);
                    float bu = Math.min(L - 7, cu + R * 0.9f), bv = Math.max(6, cv - R * 0.9f);
                    f.poly(0xFF1E1E22, bu - 3, bv + 2, bu - 6, bv + 7, bu, bv + 3);
                    f.oval(0xFF1E1E22, bu - 6.6f, bv - 4.6f, bu + 6.6f, bv + 4.6f);
                    f.oval(0xFFFFFFFF, bu - 6, bv - 4, bu + 6, bv + 4);
                    f.sign(city, "COMICS", cu, cv, 5, 0xFFD8302A);
                } else {
                    // TAPES: a big cassette on a teal roof.
                    f.rect(0xFF1E6A6A, 0, 0, L, S);
                    f.rect(0xFF2E8A8A, 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                    boolean wide = L >= S;
                    float hw = wide ? Math.min(L * 0.42f, S * 0.6f) : L * 0.42f, hh = hw * 0.62f;
                    f.roundRect(0x50000000, cu - hw + 2, cv - hh + 2, cu + hw + 2, cv + hh + 2, 2);
                    f.roundRect(0xFF2A2A2E, cu - hw, cv - hh, cu + hw, cv + hh, 2);
                    f.rect(0xFFF2E8D0, cu - hw * 0.85f, cv - hh * 0.8f, cu + hw * 0.85f, cv + hh * 0.3f);
                    f.rect(0xFFE07A30, cu - hw * 0.85f, cv - hh * 0.8f, cu + hw * 0.85f, cv - hh * 0.55f);
                    f.rect(0xFF3A3A40, cu - hw * 0.5f, cv - hh * 0.35f, cu + hw * 0.5f, cv + hh * 0.2f);
                    for (int k = -1; k <= 1; k += 2) {
                        float ru = cu + k * hw * 0.32f, rv = cv - hh * 0.08f, rr = hh * 0.22f;
                        f.circle(0xFFF2F0EA, ru, rv, rr);
                        f.circle(0xFF3A3A40, ru, rv, rr * 0.5f);
                    }
                    f.poly(0xFF4A4A50, cu - hw * 0.55f, cv + hh, cu - hw * 0.45f, cv + hh * 0.6f, cu + hw * 0.45f, cv + hh * 0.6f, cu + hw * 0.55f, cv + hh);
                    f.sign(city, "TAPES", cu, cv, 5, 0xFFE07A30);
                }
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.MID | Variants.UNI | Variants.OLD;
        d.minS = 9;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_SHELVES;
        d.roofCol = 0xFF2A2A30;
        out.add(d);
    }

    // ------------------------------------------------------------------ 18 Flower Market

    static int[][] flowerUV(int L, int S) {
        return new int[][]{{1, 1, Math.max(4, L * 28 / 100), S - 2, 1}};
    }

    static void flowerMarket(List<Landmarks.Def> out) {
        // An iron market hall and rows of flower stalls under striped canopies, buckets of blooms at their fronts.
        Landmarks.Def d = new Landmarks.Def("Flower Market", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return orient(flowerUV(Math.max(bw, bh), Math.min(bw, bh)), bw >= bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] b = box(flowerUV(lt(f), st(f))[0]);
                f.rect(0xFFCBC3B2, 0, 0, L, S);
                grid(f, 0x12000000, 0.5f, 0, 0, L, S, 8);
                // Rows of stalls with buckets of flowers in front.
                float u0 = b[2] + 8, u1 = L - 4, sw = 22, sd = 12;
                int[][] can = {{0xFF2E7A4A, 0xFFF4F0E6}, {0xFFC83A5A, 0xFFF4F0E6}, {0xFF3A6AB0, 0xFFF4F0E6}, {0xFFE8B83A, 0xFFF4F0E6}};
                int n = Math.max(1, (int) ((u1 - u0) / (sw + 2))), k = 0;
                float gap = (u1 - u0 - n * sw) / Math.max(1, n - 1);
                for (float v = 6; v + sd + 6 < S - 4; v += 32) {
                    for (int i = 0; i < n; i++, k++) {
                        float a = u0 + i * (sw + gap);
                        int[] cc = can[(k + (int) (v / 30)) % can.length];
                        // Buckets in two rows before the canopy.
                        f.rect(0xFF7A8088, a + 0.5f, v + sd - 5, a + sw - 0.5f, v + sd + 4);
                        for (int j = 0; j < 5; j++) {
                            float bu = a + 2.5f + j * 4.2f;
                            for (int rw = 0; rw < 2; rw++)
                                f.circle(FLOWERS[(k * 3 + j + rw * 5) % FLOWERS.length], bu, v + sd - 2.6f + rw * 4.2f, 1.75f);
                        }
                        f.sh(a, v, a + sw, v + sd - 5, 1.6f);
                        f.rect(cc[0], a, v, a + sw, v + sd - 5);
                        for (float q = a + 2.75f; q < a + sw; q += 5.5f) f.rect(cc[1], q, v, Math.min(q + 2.75f, a + sw), v + sd - 5);
                        f.rect(0x26000000, a, v + (sd - 5) / 2, a + sw, v + sd - 5);
                    }
                }
                // A flower cart, lamps and benches down the main aisle.
                float cv = S - 8, cu = u0 + 4;
                f.sh(cu, cv - 3, cu + 12, cv + 3, 1);
                f.rect(0xFF6A4A30, cu, cv - 3, cu + 12, cv + 3);
                for (int j = 0; j < 8; j++) f.circle(FLOWERS[j], cu + 1.5f + (j % 4) * 3, cv - 1.5f + (j / 4) * 3, 1.3f);
                f.circle(0xFF2A2A2A, cu + 3, cv + 3.6f, 1.4f);
                f.circle(0xFF2A2A2A, cu + 9, cv + 3.6f, 1.4f);
                for (float u = u0 + 30; u < L - 6; u += 34) f.lamp(u, S - 4);
                // Hanging baskets by the hall doors.
                for (float v = b[1] + 10; v < b[3] - 6; v += 22) {
                    f.circle(0xFF4E8A40, b[2] + 3, v, 2.2f);
                    f.circle(0xFFE07AB0, b[2] + 3, v, 1.1f);
                }
                f.sign(city, "FLOWER MARKET", (u0 + u1) / 2, S - 3, 6, 0xFFC83A5A);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                // A copper-green market hall with a long glass lantern and wrought iron crestings.
                gable(f, 0xFF5A9A84, 0, 0, L, S);
                for (float u = 2.5f; u < L; u += 2.5f) f.line(0x1C000000, 0.5f, u, 0, u, S);
                f.sh(3, S / 2 - S * 0.16f, L - 3, S / 2 + S * 0.16f, 1.5f);
                f.rect(0xFFB8DCE4, 3, S / 2 - S * 0.16f, L - 3, S / 2);
                f.rect(0xFF8ABCC8, 3, S / 2, L - 3, S / 2 + S * 0.16f);
                for (int k = 0; k < 24; k++) f.circle(FLOWERS[k % FLOWERS.length] & 0x80FFFFFF, 4 + r.nextFloat() * (L - 8), S / 2 - S * 0.14f + r.nextFloat() * S * 0.28f, 1.2f);
                for (float u = 3; u < L - 3; u += 4) f.line(0xFF2E4A40, 0.5f, u, S / 2 - S * 0.16f, u, S / 2 + S * 0.16f);
                f.line(0xFF2E4A40, 1, 3, S / 2, L - 3, S / 2);
                for (float u = 4; u < L - 3; u += 3) f.circle(0xFF2E4A40, u, S / 2, 0.6f);
                f.frame(0xFF2E4A40, 0.8f, 0, 0, L, S);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.OLD | Variants.MID | Variants.DOWN;
        d.minS = 9;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_SHELVES;
        d.roofCol = 0xFF5A9A84;
        out.add(d);
    }

    // ------------------------------------------------------------------ 19 Antiques Emporium

    static int[][] antiqueUV(int L, int S) {
        int lv = Math.max(4, Math.min(S - 4, S * 6 / 10)), lu = Math.max(lv + 1, L * 55 / 100);
        return new int[][]{{1, 1, lu, lv, 2}};
    }

    /** One curio in the antiques yard, centred on (u, v). */
    static void curio(F f, int kind, float u, float v) {
        switch (kind % 10) {
            case 0: // a clawfoot bath
                f.roundRect(0x38000000, u - 4, v - 1.6f, u + 5, v + 3, 2);
                f.roundRect(0xFFF2F0EA, u - 4.5f, v - 2.2f, u + 4.5f, v + 2.2f, 2);
                f.roundRect(0xFFC8D4D8, u - 3.5f, v - 1.4f, u + 3.5f, v + 1.4f, 1.4f);
                break;
            case 1: // a statue on a plinth
                f.rect(0x38000000, u - 2, v - 2, u + 3.5f, v + 3.5f);
                f.rect(0xFFB8B4AA, u - 2.5f, v - 2.5f, u + 2.5f, v + 2.5f);
                f.circle(0xFFDAD6CC, u, v, 1.6f);
                f.circle(0xFFEAE6DC, u - 0.5f, v - 0.5f, 0.7f);
                break;
            case 2: // a cartwheel
                f.ring(0xFF5A3A24, 0.8f, u, v, 3.2f);
                for (int k = 0; k < 6; k++) {
                    double a = k * Math.PI / 3;
                    f.line(0xFF7A5A3A, 0.4f, u, v, u + (float) Math.cos(a) * 3.2f, v + (float) Math.sin(a) * 3.2f);
                }
                f.circle(0xFF3A2A1A, u, v, 0.8f);
                break;
            case 3: // urns
                for (int k = 0; k < 2; k++) {
                    f.circle(0x38000000, u - 2 + k * 4 + 0.8f, v + 0.8f, 1.8f);
                    f.circle(k == 0 ? 0xFF7A8A6A : 0xFFA8603A, u - 2 + k * 4, v, 1.8f);
                    f.circle(0xFF3A3A30, u - 2 + k * 4, v, 0.8f);
                }
                break;
            case 4: // a red phone box
                f.rect(0x40000000, u - 1.5f, v - 1.5f, u + 3.5f, v + 3.5f);
                f.rect(0xFFC8282A, u - 2.2f, v - 2.2f, u + 2.2f, v + 2.2f);
                f.rect(0xFFE04040, u - 1.4f, v - 1.4f, u + 1.4f, v + 1.4f);
                f.circle(0xFFF4E8B0, u, v, 0.5f);
                break;
            case 5: // a wardrobe on its back and a mirror
                f.rect(0x38000000, u - 3, v - 2, u + 4, v + 3);
                f.rect(0xFF6A4228, u - 3.5f, v - 2.5f, u + 3.5f, v + 2.5f);
                f.line(0xFF4A2A18, 0.4f, u, v - 2.5f, u, v + 2.5f);
                f.oval(0xFFB8D0DC, u - 2.8f, v - 1.8f, u - 0.6f, v + 1.8f);
                break;
            case 6: // dining chairs
                for (int k = 0; k < 3; k++) {
                    f.rect(0x30000000, u - 4 + k * 3 + 0.6f, v - 1 + 0.6f, u - 2 + k * 3 + 0.6f, v + 1 + 0.6f);
                    f.rect(0xFF8A5A34, u - 4 + k * 3, v - 1, u - 2 + k * 3, v + 1);
                    f.rect(0xFF5A3A24, u - 4 + k * 3, v - 1.6f, u - 2 + k * 3, v - 1);
                }
                break;
            case 7: // a birdbath
                f.circle(0x38000000, u + 1, v + 1, 2.4f);
                f.circle(0xFFC8C0B0, u, v, 2.4f);
                f.circle(0xFF6A9AB0, u, v, 1.6f);
                break;
            case 8: // an old street lamp lying down
                f.line(0xFF2A3A2A, 0.9f, u - 4, v + 1, u + 3, v - 1);
                f.circle(0xFF2A3A2A, u + 3.5f, v - 1.2f, 1.4f);
                f.circle(0xFFF4E8B0, u + 3.5f, v - 1.2f, 0.6f);
                break;
            default: // a stack of crates
                f.rect(0x38000000, u - 2.5f, v - 2.5f, u + 3.5f, v + 3.5f);
                f.rect(0xFFB08A58, u - 3, v - 3, u + 3, v + 3);
                f.frame(0xFF7A5A34, 0.4f, u - 3, v - 3, u + 3, v + 3);
                f.line(0xFF7A5A34, 0.4f, u - 3, v - 3, u + 3, v + 3);
                break;
        }
    }

    static void antiques(List<Landmarks.Def> out) {
        // An old brick warehouse with a rusting roof, and a yard crammed with curios: baths, statues, wheels, a phone box.
        Landmarks.Def d = new Landmarks.Def("Antiques Emporium", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return orient(antiqueUV(Math.max(bw, bh), Math.min(bw, bh)), bw >= bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] b = box(antiqueUV(lt(f), st(f))[0]);
                // Worn setts in front, a dirt yard beside, weeds in the cracks.
                f.rect(0xFFA8A090, 0, 0, L, S);
                for (float v = b[3]; v < S; v += 3) f.line(0x1A000000, 0.4f, 0, v, b[2] + 4, v);
                float yu0 = b[2] + 4, yu1 = L - 2;
                f.rect(0xFFB09A78, yu0, 2, yu1, S - 2);
                for (int k = 0; k < 30; k++) f.circle(k % 3 == 0 ? 0x405A8A3A : 0x14000000, r.nextFloat() * L, r.nextFloat() * S, 0.6f + r.nextFloat());
                // A picket fence round the yard with a gate on the street.
                float gu = (yu0 + yu1) / 2;
                f.line(0xFFD8CCB0, 0.8f, yu0, 2, yu1, 2);
                f.line(0xFFD8CCB0, 0.8f, yu1, 2, yu1, S - 2);
                f.line(0xFFD8CCB0, 0.8f, yu0, S - 2, gu - 5, S - 2);
                f.line(0xFFD8CCB0, 0.8f, gu + 5, S - 2, yu1, S - 2);
                for (float v = 4; v < S - 2; v += 3) f.circle(0xFFB8AC90, yu1, v, 0.5f);
                // The curios, packed in on a rough grid.
                int k = 0;
                for (float v = 9; v < S - 8; v += 12) {
                    for (float u = yu0 + 8; u < yu1 - 5; u += 12) {
                        k++;
                        if (Math.abs(u - gu) < 6 && v > S - 30) continue;
                        float ju = (r.nextFloat() - 0.5f) * 6, jv = (r.nextFloat() - 0.5f) * 6;
                        int kind = r.nextInt(14);
                        if (kind >= 10) {
                            for (int j = 0; j < 3; j++) f.circle(0x705A8A3A, u + ju + j * 1.6f, v + jv + (j % 2) * 1.4f, 1.6f);
                            continue;
                        }
                        curio(f, kind, u + ju, v + jv);
                    }
                }
                // More on the setts by the door, an old rusting car, and bunting.
                for (int j = 0; j < 4; j++) {
                    float u = b[0] + 8 + j * 14;
                    if (u > b[2] - 6) break;
                    curio(f, j * 3 + 1, u, b[3] + 7);
                }
                if (S - b[3] > 40) f.car(b[0] + 18, S - 12, 0.15, 0xFF8A6A4A, 5.6f);
                f.line(0x60000000, 0.3f, yu0, 4, yu1, S * 0.5f);
                for (int j = 1; j < 10; j++) {
                    float t = j / 10f, u = yu0 + (yu1 - yu0) * t, v = 4 + (S * 0.5f - 4) * t;
                    f.poly(BRIGHT[j % BRIGHT.length], u - 1, v, u + 1, v, u, v + 2.2f);
                }
                f.sign(city, "ANTIQUES", (b[0] + b[2]) / 2, b[3] + 2, 6, 0xFF6A4228);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                // Twin gables of weathered corrugated iron with rust and patched roof lights, brick parapets.
                gable(f, 0xFF8A8478, 0, 0, L, S / 2);
                gable(f, 0xFF8A8478, 0, S / 2, L, S);
                for (float u = 1.5f; u < L; u += 1.8f) f.line(0x1C000000, 0.4f, u, 0, u, S);
                for (int k = 0; k < 18; k++) {
                    float u = r.nextFloat() * (L - 10), v = r.nextFloat() * (S - 8);
                    f.oval(0x50A0502A, u, v, u + 4 + r.nextFloat() * 8, v + 2 + r.nextFloat() * 4);
                }
                for (float u = 8; u < L - 8; u += 14) {
                    f.rect(0xFFA8C4CC, u, S * 0.1f, u + 5, S * 0.22f);
                    f.rect(((int) u) % 3 == 0 ? 0xFF3A3A3A : 0xFFB8CCD2, u, S * 0.6f, u + 5, S * 0.72f);
                }
                f.rect(0xFF9A4A34, 0, 0, L, 1.5f);
                f.rect(0xFF7A3A28, 0, S - 1.5f, L, S);
                f.rect(0xFF8A4230, 0, 0, 1.5f, S);
                f.rect(0xFF7A3A28, L - 1.5f, 0, L, S);
                f.rect(0x40F4F0E6, L * 0.25f, S * 0.32f, L * 0.75f, S * 0.44f);
                float cu = L * 0.86f, cv = S * 0.75f;
                f.sh(cu - 3, cv - 3, cu + 3, cv + 3, 3);
                f.rect(0xFF9A4A34, cu - 3, cv - 3, cu + 3, cv + 3);
                f.circle(0xFF2A2A2A, cu, cv, 1.6f);
            }
        };
        d.ground = City.DIRT;
        d.districts = Variants.OLD | Variants.IND | Variants.MID;
        d.minS = 9;
        d.minL = 12;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_SHELVES;
        d.roofCol = 0xFF8A8478;
        out.add(d);
    }

    // ------------------------------------------------------------------ 20 Furniture Warehouse

    static int[][] furnitureUV(int L, int S) {
        return new int[][]{{1, 1, L - 5, Math.max(5, Math.min(S - 4, S * 65 / 100)), 2}};
    }

    /** A delivery lorry backed in, nose towards +u (or -u when flip). */
    static void lorry(F f, float u, float v, boolean flip, int col) {
        float s = flip ? -1 : 1;
        float a = u, b = u + s * 22;
        f.rect(0x40000000, Math.min(a, b) + 1.5f, v - 3.5f + 1.5f, Math.max(a, b) + 1.5f, v + 3.5f + 1.5f);
        f.rect(0xFFE8E8E4, Math.min(a, b), v - 3.5f, Math.max(a, b), v + 3.5f);
        f.rect(col, Math.min(a, b), v + 1.8f, Math.max(a, b), v + 3.5f);
        float c0 = b + s * 1, c1 = b + s * 7;
        f.rect(col, Math.min(c0, c1), v - 3, Math.max(c0, c1), v + 3);
        f.rect(0xFF2A3440, Math.min(c1 - s * 2, c1), v - 2.6f, Math.max(c1 - s * 2, c1), v + 2.6f);
    }

    static void furniture(List<Landmarks.Def> out) {
        // A huge navy-and-mustard furniture shed: a loading dock with lorries, and garden furniture laid out to try.
        Landmarks.Def d = new Landmarks.Def("Furniture Warehouse", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return orient(furnitureUV(Math.max(bw, bh), Math.min(bw, bh)), bw >= bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] b = box(furnitureUV(lt(f), st(f))[0]);
                f.rect(0xFFB4B0A8, 0, 0, L, S);
                grid(f, 0x12000000, 0.5f, 0, 0, L, S, 12);
                // The loading dock at the end: an apron, two lorries backed in and a third waiting.
                float du = b[2];
                f.rect(0xFF6A6A68, du, 2, L - 2, b[3]);
                for (int k = 0; k < 3; k++) {
                    float v = b[1] + 10 + k * 16;
                    if (v + 6 > b[3]) break;
                    f.rect(0xFF2A2A2E, du - 0.5f, v - 5, du + 1.5f, v + 5);
                    f.rect(0xFFE0B030, du + 1.5f, v - 5, du + 2.2f, v + 5);
                    if (k < 2 && L - du > 30) lorry(f, du + 2, v, false, 0xFF2A3E6A);
                    else if (k < 2) lorry(f, du + 2, v, false, 0xFF2A3E6A);
                }
                // A glass entrance and a mustard canopy.
                float fv = b[3], cu = (b[0] + b[2]) / 2;
                f.sh(cu - 14, fv, cu + 14, fv + 7, 2);
                f.rect(0xFFE0B030, cu - 14, fv, cu + 14, fv + 7);
                f.rect(0xFFF0C848, cu - 14, fv, cu + 14, fv + 2.5f);
                // Garden furniture on show: lounge sets, a dining set under a parasol, deckchairs.
                float sv = fv + 16;
                if (S - fv > 30) {
                    float u = b[0] + 8;
                    // A corner sofa round a coffee table.
                    f.sh(u, sv - 6, u + 16, sv + 6, 1);
                    f.rect(0xFF6A6460, u, sv - 6, u + 16, sv - 2);
                    f.rect(0xFF6A6460, u, sv - 2, u + 4, sv + 6);
                    for (float q = u + 1; q < u + 15; q += 4.6f) f.roundRect(0xFFE8E0CC, q, sv - 5.4f, q + 4, sv - 2.6f, 1);
                    f.roundRect(0xFFE8E0CC, u + 0.6f, sv - 1.6f, u + 3.4f, sv + 5.4f, 1);
                    f.rect(0xFF8A6A44, u + 7, sv + 0.5f, u + 13, sv + 4.5f);
                    // A dining set.
                    float du2 = u + 30;
                    if (du2 + 10 < cu - 16) {
                        table(f, du2, sv, 3.4f, 0xFFF2EEE6, 0xFF4A4A4A, 6);
                        f.parasol(du2, sv, 6, 0xFFE0B030, 0xFFF4F0E6);
                    }
                    // Deckchairs in a row.
                    for (int k = 0; k < 4; k++) {
                        float q = cu + 18 + k * 7;
                        if (q + 3 > b[2]) break;
                        f.rect(0x30000000, q - 1.5f, sv - 3.5f, q + 3.5f, sv + 5.5f);
                        f.rect(0xFF8A6A44, q - 2, sv - 4, q + 3, sv + 5);
                        for (float z = q - 1.5f; z < q + 2.5f; z += 1.2f) f.line(k % 2 == 0 ? 0xFF2A3E6A : 0xFFE0B030, 0.6f, z, sv - 3.5f, z, sv + 4.5f);
                    }
                }
                // A couple of bays, flags and planters along the road.
                for (int k = 0; k < 3; k++) {
                    float u = 6 + k * 13;
                    f.line(0xC0F2F0E8, 0.6f, u, S - 18, u, S - 3);
                }
                f.car(12.5f, S - 10.5f, Math.PI / 2, 0xFF7A8A90, 5.2f);
                for (float u = 50; u < L - 6; u += 14) {
                    f.circle(0xFF4A4C50, u, S - 4, 0.7f);
                    f.rect((int) (u / 14) % 2 == 0 ? 0xFF2A3E6A : 0xFFE0B030, u + 0.5f, S - 5, u + 5, S - 3.4f);
                }
                f.sign(city, "FURNITURE", cu, fv + 3.5f, 6, 0xFF2A3E6A);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                // A navy box with a mustard band, rows of roof lights, and a big mustard roof sign.
                f.rect(0xFFE0B030, 0, 0, L, S);
                f.rect(0xFF2A3E6A, 2.5f, 2.5f, L - 2.5f, S - 2.5f);
                for (float u = 8; u < L - 3; u += 8) f.line(0x16FFFFFF, 0.5f, u, 2.5f, u, S - 2.5f);
                for (float v = S * 0.15f; v < S * 0.9f; v += S * 0.24f) {
                    for (float u = 8; u < L - 10; u += 12) {
                        if (u > L * 0.3f && u < L * 0.72f && v > S * 0.3f && v < S * 0.7f) continue;
                        f.rect(0xFF1E2E52, u - 0.6f, v - 0.6f, u + 5.6f, v + 3.6f);
                        f.rect(0xFFA8C8DC, u, v, u + 5, v + 3);
                    }
                }
                float su0 = L * 0.32f, su1 = L * 0.7f, sv0 = S * 0.36f, sv1 = S * 0.64f;
                f.sh(su0, sv0, su1, sv1, 1.5f);
                f.roundRect(0xFFE0B030, su0, sv0, su1, sv1, 3);
                // An armchair silhouette on the sign.
                float cu = (su0 + su1) / 2, cv = (sv0 + sv1) / 2, s = Math.min(su1 - su0, sv1 - sv0) * 0.32f;
                f.roundRect(0xFF2A3E6A, cu - s, cv - s, cu + s, cv - s * 0.45f, 1.5f);
                f.roundRect(0xFF2A3E6A, cu - s, cv - s, cu - s * 0.6f, cv + s, 1.5f);
                f.roundRect(0xFF2A3E6A, cu + s * 0.6f, cv - s, cu + s, cv + s, 1.5f);
                f.roundRect(0xFF3A5288, cu - s * 0.55f, cv - s * 0.4f, cu + s * 0.55f, cv + s, 1.5f);
                unit(f, L * 0.12f, S * 0.42f, 7, 6);
                unit(f, L * 0.8f, S * 0.42f, 7, 6);
                f.sign(city, "FURNITURE", cu, sv1 + 6, 7, 0xFFE0B030);
            }
        };
        d.ground = City.LOT;
        d.districts = Variants.SUB | Variants.IND | Variants.MID;
        d.minS = 10;
        d.minL = 14;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_SHOWROOM;
        d.roofCol = 0xFF2A3E6A;
        out.add(d);
    }

    // ------------------------------------------------------------------ 21 Pet Superstore

    static int[][] petUV(int L, int S) {
        int lv = Math.max(4, Math.min(S - 4, S * 6 / 10)), lu = Math.max(lv + 1, L * 6 / 10);
        return new int[][]{{1, 1, lu, lv, 1}};
    }

    static void dog(F f, float u, float v, boolean right, int col) {
        float s = right ? 1 : -1;
        f.oval(0x30000000, u - 2.2f + 0.8f, v - 1.2f + 0.8f, u + 2.2f + 0.8f, v + 1.2f + 0.8f);
        f.oval(col, u - 2.2f, v - 1.2f, u + 2.2f, v + 1.2f);
        f.circle(col, u + s * 2.6f, v - 0.3f, 1.2f);
        f.circle(City.darken(col, 0.7f), u + s * 2.4f, v - 1.2f, 0.5f);
        f.line(col, 0.5f, u - s * 2.2f, v, u - s * 3.6f, v - 1);
    }

    static void paw(F f, float u, float v, float s, int col) {
        f.oval(col, u - s, v - s * 0.4f, u + s, v + s);
        f.circle(col, u - s * 1.1f, v - s * 0.9f, s * 0.42f);
        f.circle(col, u - s * 0.4f, v - s * 1.45f, s * 0.42f);
        f.circle(col, u + s * 0.4f, v - s * 1.45f, s * 0.42f);
        f.circle(col, u + s * 1.1f, v - s * 0.9f, s * 0.42f);
    }

    static void petStore(List<Landmarks.Def> out) {
        // A pet superstore with a giant paw on the roof, a paw-print path, a dog wash and a fenced agility run.
        Landmarks.Def d = new Landmarks.Def("Pet Superstore", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return orient(petUV(Math.max(bw, bh), Math.min(bw, bh)), bw >= bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] b = box(petUV(lt(f), st(f))[0]);
                f.rect(0xFFC8C2B6, 0, 0, L, S);
                grid(f, 0x12000000, 0.5f, 0, 0, L, S, 10);
                float fv = b[3], cu = (b[0] + b[2]) / 2;
                // The entrance and a trail of paw prints out to the road.
                f.sh(cu - 10, fv, cu + 10, fv + 6, 1.5f);
                f.rect(0xFFD8783A, cu - 10, fv, cu + 10, fv + 6);
                f.rect(0xFFE8945A, cu - 10, fv, cu + 10, fv + 2);
                for (float v = fv + 10; v < S - 4; v += 6) paw(f, cu + (((int) (v / 6)) % 2 == 0 ? -2 : 2), v, 1.1f, 0xFF8A5A3A);
                // The dog wash under a blue canopy.
                float wu = b[0] + 8, wv = fv + 10;
                if (S - fv > 34 && cu - wu > 30) {
                    f.rect(0xFFD8E4EC, wu, wv, wu + 20, wv + 12);
                    for (int k = 0; k < 2; k++) {
                        f.rect(0xFFB8C4CC, wu + 2 + k * 9, wv + 2, wu + 9 + k * 9, wv + 10);
                        f.rect(0xFF8AB8D8, wu + 3 + k * 9, wv + 3, wu + 8 + k * 9, wv + 9);
                    }
                    dog(f, wu + 5.5f, wv + 6, true, 0xFFE8D8B0);
                    f.sh(wu, wv - 2, wu + 20, wv + 3, 1.5f);
                    f.rect(0xFF3A7AC8, wu, wv - 2, wu + 20, wv + 3);
                    f.rect(0x30FFFFFF, wu, wv - 2, wu + 20, wv);
                }
                // The agility run beside the store: turf, hurdles, a tunnel, weave poles and dogs.
                float a0 = b[2] + 6, a1 = L - 4, v0 = 4, v1 = S - 4;
                if (a1 - a0 > 24) {
                    f.rect(0xFF5AAE4A, a0, v0, a1, v1);
                    for (float u = a0; u < a1; u += 10) f.rect(0xFF62B852, u, v0, Math.min(u + 5, a1), v1);
                    f.frame(0xFFB8BCC0, 0.7f, a0, v0, a1, v1);
                    for (float u = a0 + 4; u < a1; u += 5) {
                        f.circle(0xFF8A8E92, u, v0, 0.5f);
                        f.circle(0xFF8A8E92, u, v1, 0.5f);
                    }
                    float am = (a0 + a1) / 2, aw = Math.min(10, (a1 - a0) / 2 - 4);
                    for (float v = v0 + 14; v < v1 * 0.45f; v += 14) {
                        for (float q = am - aw; q < am + aw; q += 2) f.line(((int) ((q - am) / 2)) % 2 == 0 ? 0xFFD8302A : 0xFFF4F4F0, 0.9f, q, v, q + 2, v);
                        f.circle(0xFF3A3A3A, am - aw, v, 0.8f);
                        f.circle(0xFF3A3A3A, am + aw, v, 0.8f);
                    }
                    float tv = v1 * 0.55f;
                    f.roundRect(0x30000000, am - aw + 1, tv - 3 + 1, am + aw + 1, tv + 3 + 1, 3);
                    f.roundRect(0xFF3A7AC8, am - aw, tv - 3, am + aw, tv + 3, 3);
                    for (float q = am - aw + 2; q < am + aw; q += 2.5f) f.line(0xFF2A5A98, 0.5f, q, tv - 3, q, tv + 3);
                    for (float v = tv + 12; v < v1 - 6; v += 5) f.circle(((int) v / 5) % 2 == 0 ? 0xFFE8B83A : 0xFFD8302A, am + (((int) v / 5) % 2 == 0 ? -1.5f : 1.5f), v, 0.9f);
                    dog(f, am - aw * 0.5f, v0 + 8, true, 0xFF8A5A34);
                    dog(f, am + aw * 0.6f, tv + 8, false, 0xFF2A2A2A);
                    f.circle(0xFF8AB8D8, a1 - 5, v1 - 5, 1.8f);
                    f.ring(0xFFB8BCC0, 0.6f, a1 - 5, v1 - 5, 1.8f);
                    f.circle(0xFFD8302A, a0 - 3, v1 - 6, 1.6f);
                    f.circle(0xFFF07060, a0 - 3.4f, v1 - 6.4f, 0.6f);
                }
                f.tree(6, S - 8, 5);
                f.bench(cu - 18, S - 6, true);
                f.sign(city, "PETS", cu, fv + 3, 6, 0xFFD8783A);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                // A cream roof with an orange border, a giant paw print and a bone.
                f.rect(0xFFD8783A, 0, 0, L, S);
                f.rect(0xFFEADCC0, 2.5f, 2.5f, L - 2.5f, S - 2.5f);
                for (float u = 8; u < L - 3; u += 8) f.line(0x10000000, 0.5f, u, 2.5f, u, S - 2.5f);
                float s = S * 0.17f, pu = L * 0.32f, pv = S * 0.56f;
                F g = new F(c, p, f.x(0, 0), f.y(0, 0), f.x(L, S), f.y(L, S));
                paw(g, pu + 1.5f, pv + 1.5f, s, 0x30000000);
                paw(g, pu, pv, s, 0xFFB85A2A);
                float bu = L * 0.7f, bv = S * 0.5f, bl = Math.min(L * 0.16f, 22), br = S * 0.07f;
                f.rect(0x30000000, bu - bl + 1.5f, bv - br * 0.7f + 1.5f, bu + bl + 1.5f, bv + br * 0.7f + 1.5f);
                f.rect(0xFFF8F6F0, bu - bl, bv - br * 0.7f, bu + bl, bv + br * 0.7f);
                for (int k = -1; k <= 1; k += 2) {
                    f.circle(0xFFF8F6F0, bu + k * bl, bv - br, br);
                    f.circle(0xFFF8F6F0, bu + k * bl, bv + br, br);
                }
                for (float u = 8; u < L - 6; u += 14) {
                    f.circle(0xFFB8B4AA, u, 6.5f, 2.2f);
                    f.circle(0xFFA8CCE0, u, 6.5f, 1.6f);
                }
                unit(f, L - 14, S - 12, 8, 6);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.SUB | Variants.MID;
        d.minS = 10;
        d.minL = 13;
        d.base = City.SHOP;
        d.inside = Variants.I_SHELVES;
        d.roofCol = 0xFFEADCC0;
        out.add(d);
    }

    // ------------------------------------------------------------------ 22 Burger Drive-Thru

    static int[][] burgerUV(int L, int S) {
        int lu = Math.max(5, L * 45 / 100), lv = Math.max(3, S * 35 / 100);
        return new int[][]{{(L - lu) / 2, (S - lv) / 2 - (S >= 12 ? 1 : 0), lu, lv, 1}};
    }

    static void burger(List<Landmarks.Def> out) {
        // A burger joint ringed by its drive-thru lane: menu boards, a pickup window, a queue of cars and a big sign.
        Landmarks.Def d = new Landmarks.Def("Burger Drive-Thru", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return orient(burgerUV(Math.max(bw, bh), Math.min(bw, bh)), bw >= bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] b = box(burgerUV(lt(f), st(f))[0]);
                f.rect(0xFF6A9A4E, 0, 0, L, S);
                // The lane: a ring of tarmac round a kerbed pavement, joined to the road at the front.
                float lw = 15, li = 6;
                float o0 = b[0] - li - lw, o1 = b[2] + li + lw, w0 = b[1] - li - lw, w1 = b[3] + li + lw;
                f.rect(0xFF55555A, o0 + 2, w1 - 6, o0 + lw + 2, S);
                f.rect(0xFF55555A, o1 - lw - 2, w1 - 6, o1 - 2, S);
                f.roundRect(0xFF55555A, o0, w0, o1, w1, 12);
                f.roundRect(0xFFE8E4DA, b[0] - li - 1, b[1] - li - 1, b[2] + li + 1, b[3] + li + 1, 4);
                f.roundRect(0xFFCCC6BA, b[0] - li, b[1] - li, b[2] + li, b[3] + li, 3);
                // Arrows round the lane (in at the left, out at the right) and the dashed edge.
                for (float v = w1 - 10; v > w0 + 14; v -= 16) {
                    float u = o0 + lw / 2 + 0.5f;
                    f.line(0xFFF2F0E8, 0.8f, u, v, u - 2.5f, v + 3);
                    f.line(0xFFF2F0E8, 0.8f, u, v, u + 2.5f, v + 3);
                }
                for (float v = w0 + 16; v < w1 - 8; v += 16) {
                    float u = o1 - lw / 2 - 0.5f;
                    f.line(0xFFF2F0E8, 0.8f, u, v, u - 2.5f, v - 3);
                    f.line(0xFFF2F0E8, 0.8f, u, v, u + 2.5f, v - 3);
                }
                f.rring(0x80F2F0E8, 0.5f, o0 + 1.5f, w0 + 1.5f, o1 - 1.5f, w1 - 1.5f, 11);
                // Menu boards at the top corner, the speaker post and the pickup window.
                float mu = b[0] - li + 2, mv = b[1] - li + 1;
                for (int k = 0; k < 2; k++) {
                    float u = mu + k * 13;
                    f.sh(u, mv, u + 11, mv + 3.5f, 1.2f);
                    f.rect(0xFF1E1E22, u, mv, u + 11, mv + 3.5f);
                    for (int j = 0; j < 4; j++) f.rect(BRIGHT[(j + k * 2) % BRIGHT.length], u + 1 + j * 2.5f, mv + 0.8f, u + 3 + j * 2.5f, mv + 2.7f);
                }
                f.circle(0xFF3A3A3A, mu + 28, mv + 2, 1.4f);
                f.circle(0xFFD8302A, mu + 28, mv + 2, 0.6f);
                float wv = (b[1] + b[3]) / 2;
                f.rect(0xFFE8B83A, b[2], wv - 5, b[2] + 2, wv + 5);
                f.rect(0xFF9AC4D4, b[2], wv - 3.5f, b[2] + 1.2f, wv + 3.5f);
                f.sh(b[2], wv - 7, b[2] + 6, wv + 7, 1);
                f.rect(0xE0D8302A, b[2], wv - 7, b[2] + 6, wv + 7);
                // Cars queueing round the lane.
                f.car(o0 + lw / 2, (w0 + w1) / 2 + 10, -Math.PI / 2, CARS[1], 5.2f);
                f.car((o0 + o1) / 2, w0 + lw / 2, 0, CARS[2], 5.2f);
                f.car(o1 - lw / 2, wv, Math.PI / 2, CARS[0], 5.2f);
                f.car(o0 + lw / 2, w1 - 4, -Math.PI / 2, CARS[6], 5.2f);
                // Picnic tables, a slide, bins and the sign on the lawns outside the lane.
                float[][] pt = {{o0 / 2, 14}, {(o1 + L) / 2, 14}};
                for (float[] q : pt) {
                    if (q[0] < 8 || q[0] > L - 8) continue;
                    f.rect(0x30000000, q[0] - 5 + 1, q[1] - 4.5f + 1, q[0] + 5 + 1, q[1] + 4.5f + 1);
                    f.rect(0xFFB8442E, q[0] - 5, q[1] - 4.5f, q[0] + 5, q[1] + 4.5f);
                    f.rect(0xFFD8603A, q[0] - 5, q[1] - 2.2f, q[0] + 5, q[1] + 2.2f);
                    f.parasol(q[0], q[1], 4.6f, 0xFFE8B83A, 0xFFD8302A);
                }
                if (o0 > 18) {
                    float su = o0 / 2, sv = S * 0.5f;
                    f.rect(0xFFD8C8A0, su - 6, sv - 8, su + 6, sv + 8);
                    f.roundRect(0x30000000, su - 1, sv - 5, su + 3, sv + 7, 1.5f);
                    f.roundRect(0xFF3A7AC8, su - 2, sv - 6, su + 2, sv + 6, 1.5f);
                    f.rect(0xFFE8B83A, su - 2, sv - 6, su + 2, sv - 3);
                }
                float pu = L - 10, pv = S - 10;
                f.circle(0x40000000, pu + 5, pv + 5, 7);
                f.circle(0xFFD8302A, pu, pv, 7);
                f.circle(0xFFE8B83A, pu, pv, 5.4f);
                f.oval(0xFFC8862E, pu - 4, pv - 3.6f, pu + 4, pv - 0.4f);
                f.rect(0xFF4AA65A, pu - 4.2f, pv - 0.6f, pu + 4.2f, pv + 0.6f);
                f.rect(0xFF6A3A1E, pu - 4, pv + 0.6f, pu + 4, pv + 2);
                f.roundRect(0xFFC8862E, pu - 4, pv + 2, pu + 4, pv + 3.6f, 1);
                bin(f, b[0] - li + 3, b[3] + li - 3, 0xFF8A2A20);
                f.sign(city, "BURGERS", (b[0] + b[2]) / 2, b[3] + 3, 6, 0xFFD8302A);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                // A red mansard band round a flat roof with a painted burger and kitchen extract fans.
                f.rect(0xFF9A1E1A, 0, 0, L, S);
                f.rect(0xFFD8302A, 0.8f, 0.8f, L - 0.8f, S - 0.8f);
                for (float u = 2; u < L; u += 2.5f) {
                    f.line(0x30000000, 0.4f, u, 0.8f, u, 4);
                    f.line(0x30000000, 0.4f, u, S - 4, u, S - 0.8f);
                }
                f.rect(0xFFE8B83A, 4, 4, L - 4, 5);
                f.rect(0xFFD8D4CC, 5, 5, L - 5, S - 5);
                unit(f, L * 0.72f, S * 0.25f, 7, 6);
                f.circle(0xFF8A8E92, L * 0.78f, S * 0.72f, 2.4f);
                f.circle(0xFF5A5E62, L * 0.78f, S * 0.72f, 1.2f);
                float bx = x0 + (f.t ? S * 0.5f : L * 0.36f), by = y0 + (f.t ? L * 0.4f : S * 0.5f);
                f = new F(c, p, bx, by, bx + 1, by + 1, false);
                float cu = 0, cv = 0, w = Math.min(L * 0.22f, S * 0.32f);
                f.oval(0x30000000, cu - w + 1.5f, cv - w * 0.75f + 1.5f, cu + w + 1.5f, cv + w * 0.8f + 1.5f);
                f.oval(0xFFC8862E, cu - w, cv - w * 0.75f, cu + w, cv - w * 0.05f);
                for (int k = 0; k < 5; k++) f.oval(0xFFF4ECD0, cu - w * 0.6f + k * w * 0.3f - 0.8f, cv - w * 0.55f + (k % 2) * w * 0.15f, cu - w * 0.6f + k * w * 0.3f + 0.8f, cv - w * 0.55f + (k % 2) * w * 0.15f + 0.5f);
                f.roundRect(0xFF4AA65A, cu - w * 1.05f, cv - w * 0.12f, cu + w * 1.05f, cv + w * 0.1f, 1);
                f.roundRect(0xFFE8C03A, cu - w * 1.02f, cv + w * 0.08f, cu + w * 1.02f, cv + w * 0.22f, 1);
                f.roundRect(0xFF5A2E18, cu - w, cv + w * 0.2f, cu + w, cv + w * 0.48f, 2);
                f.roundRect(0xFFB8762A, cu - w, cv + w * 0.5f, cu + w, cv + w * 0.8f, 3);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.SUB | Variants.IND | Variants.MID;
        d.minS = 10;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_RESTAURANT;
        d.roofCol = 0xFFD8302A;
        out.add(d);
    }

    // ------------------------------------------------------------------ 23 Pizza Parlour

    static int[][] pizzaUV(int L, int S) {
        int lv = Math.max(4, Math.min(S - 5, S / 2)), lu = Math.max(lv + 1, L / 2);
        return new int[][]{{1, 1, lu, lv, 1}};
    }

    static void pizzaDisc(F f, float u, float v, float R) {
        f.circle(0x40000000, u + 2, v + 2, R);
        f.circle(0xFFC8862E, u, v, R);
        f.circle(0xFFD84A2A, u, v, R * 0.84f);
        for (int k = 0; k < 9; k++) {
            double a = k * 2.4;
            float rr = R * (0.2f + 0.55f * ((k * 37) % 10) / 10f);
            f.circle(0xFFF4E2A0, u + (float) Math.cos(a) * rr, v + (float) Math.sin(a) * rr, R * 0.14f);
        }
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3 + 0.5;
            f.circle(0xFF9A2A1E, u + (float) Math.cos(a) * R * 0.52f, v + (float) Math.sin(a) * R * 0.52f, R * 0.12f);
        }
        for (int k = 0; k < 3; k++) {
            double a = k * 2.1 + 1;
            f.circle(0xFF3A8A3A, u + (float) Math.cos(a) * R * 0.28f, v + (float) Math.sin(a) * R * 0.28f, R * 0.07f);
        }
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 4;
            f.line(0x40000000, 0.4f, u - (float) Math.cos(a) * R, v - (float) Math.sin(a) * R, u + (float) Math.cos(a) * R, v + (float) Math.sin(a) * R);
        }
    }

    static void pizza(List<Landmarks.Def> out) {
        // A corner pizzeria: a wood-fired oven dome and smoking chimney on the roof, a tricolore awning over the terrace.
        Landmarks.Def d = new Landmarks.Def("Pizza Parlour", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return orient(pizzaUV(Math.max(bw, bh), Math.min(bw, bh)), bw >= bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] b = box(pizzaUV(lt(f), st(f))[0]);
                f.rect(0xFFC4BEB2, 0, 0, L, S);
                grid(f, 0x12000000, 0.5f, 0, 0, L, S, 8);
                float fv = b[3];
                // A terracotta-tiled terrace in front, under a green, white and red awning.
                f.rect(0xFFC8805A, b[0] - 4, fv, b[2] + 4, S - 5);
                grid(f, 0x22000000, 0.5f, b[0] - 4, fv, b[2] + 4, S - 5, 6);
                int[] tri = {0xFF2E8A3A, 0xFFF4F0E6, 0xFFC8342A};
                f.sh(b[0], fv, b[2], fv + 7, 1.6f);
                int k = 0;
                for (float u = b[0]; u < b[2]; u += 3, k++) f.rect(tri[k % 3], u, fv, Math.min(u + 3, b[2]), fv + 7);
                f.rect(0x24000000, b[0], fv + 3.5f, b[2], fv + 7);
                for (float u = b[0] + 1.5f; u < b[2]; u += 3) f.circle(tri[((int) ((u - b[0]) / 3)) % 3], u, fv + 7, 1.3f);
                // Wooden tables, each with a pizza, and herb planters.
                for (float v = fv + 16; v < S - 10; v += 18) {
                    for (float u = b[0] + 8; u < b[2] - 4; u += 18) {
                        f.circle(0xFF3A2A20, u - 5, v, 1.1f);
                        f.circle(0xFF3A2A20, u + 5, v, 1.1f);
                        f.rect(0x30000000, u - 3.4f, v - 3.4f, u + 4.2f, v + 4.2f);
                        f.rect(0xFF9A6A3E, u - 3.6f, v - 3.6f, u + 3.6f, v + 3.6f);
                        f.circle(0xFFC8862E, u, v, 2.2f);
                        f.circle(0xFFD84A2A, u, v, 1.7f);
                        f.circle(0xFFF4E2A0, u - 0.5f, v - 0.4f, 0.6f);
                    }
                }
                for (float u = b[0]; u < b[2]; u += 18) {
                    f.rect(0xFF7A6A5A, u, S - 9, u + 10, S - 5);
                    for (float q = u + 1.5f; q < u + 10; q += 2.4f) f.circle(0xFF4E8A40, q, S - 7, 1.4f);
                }
                // Delivery scooters lined up along the side, and the oven's woodpile.
                float su = b[2] + 10;
                for (int j = 0; j < 4; j++) {
                    float v = b[1] + 8 + j * 9;
                    if (v > S - 10 || su + 8 > L - 2) break;
                    f.oval(0x40000000, su - 3, v - 1.2f, su + 6, v + 2.6f);
                    f.oval(0xFFC8342A, su - 3.5f, v - 1.5f, su + 3.5f, v + 1.5f);
                    f.rect(0xFFF4F0E6, su - 4.5f, v - 2, su - 1, v + 2);
                    f.rect(0xFFC8342A, su - 4.2f, v - 0.4f, su - 1.3f, v + 0.4f);
                    f.line(0xFF2A2A2A, 0.6f, su + 3, v - 2.4f, su + 3, v + 2.4f);
                }
                float wu = b[2] + 4, wv = b[1] + 4;
                for (int j = 0; j < 3; j++)
                    for (int q = 0; q < 2; q++) {
                        f.circle(0xFF8A5A34, wu + q * 2.6f, wv + j * 2.6f, 1.3f);
                        f.circle(0xFFC8A070, wu + q * 2.6f, wv + j * 2.6f, 0.7f);
                    }
                // A garden terrace beside: lawn, tables under green parasols, festoon lights.
                float g0 = b[2] + 18, g1 = L - 4;
                if (g1 - g0 > 24) {
                    f.rect(0xFF6E9E52, g0, 4, g1, S - 5);
                    for (float u = g0; u < g1; u += 8) f.rect(0xFF78A85C, u, 4, Math.min(u + 4, g1), S - 5);
                    hedge(f, g0, S - 9, g1, S - 5);
                    int kk = 0;
                    for (float v = 16; v < S - 16; v += 24)
                        for (float u = g0 + 12; u < g1 - 6; u += 24, kk++) {
                            table(f, u, v, 2.8f, 0xFFF4F0E6, 0xFF3A2A20, 4);
                            if (kk % 2 == 0) f.parasol(u, v, 5.5f, 0xFF2E8A3A, 0xFFF4F0E6);
                        }
                    f.festoon(g0, 4, g1, S - 9, 2, 16, false);
                }
                f.lamp(b[2] + 6, S - 6);
                f.sign(city, "PIZZA", (b[0] + b[2]) / 2, fv + 3.5f, 6, 0xFFC8342A);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                // A pantiled gable, the brick oven dome breaking through at one end with its chimney.
                gable(f, 0xFFB8603A, 0, 0, L, S);
                for (float u = 1.5f; u < L; u += 2.4f) f.line(0x22000000, 0.6f, u, 0, u, S);
                float orr = Math.min(S * 0.24f, L * 0.16f), ou = L - orr - 6, ov = S / 2;
                f.circle(0x48000000, ou + 2, ov + 2, orr);
                f.circle(0xFF8A3A24, ou, ov, orr);
                for (float rr = orr * 0.8f; rr > 1; rr -= orr * 0.22f) f.ring(0x40000000, 0.5f, ou, ov, rr);
                f.circle(0x30FFFFFF, ou - orr * 0.35f, ov - orr * 0.35f, orr * 0.4f);
                float ch = Math.max(3, S * 0.09f);
                f.circle(0x50000000, ou + 3, ov + 3, ch);
                f.circle(0xFF9A4A30, ou, ov, ch);
                f.circle(0xFF1E1A18, ou, ov, ch * 0.55f);
                smoke(f, ou + 2, ov - 1, 2.6f, 0xFFDCD8D0);
                // A pizza sign board on a stand.
                float R = Math.min(S * 0.27f, L * 0.18f), pu = Math.min(L * 0.32f, ou - orr - R - 6);
                if (pu > R + 2) pizzaDisc(f, pu, S / 2, R);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.MID | Variants.OLD | Variants.SUB | Variants.UNI;
        d.minS = 9;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_KITCHEN;
        d.roofCol = 0xFFB8603A;
        out.add(d);
    }

    // ------------------------------------------------------------------ 24 Tea Room Garden

    static int[][] teaUV(int L, int S) {
        return new int[][]{{1, 1, Math.max(5, L * 4 / 10), Math.max(3, Math.min(4, S * 35 / 100)), 1}};
    }

    static void teaRoom(List<Landmarks.Def> out) {
        // A thatched cottage tea room and its lawn: tables with cake stands, box hedges, a rose arch and a sundial.
        Landmarks.Def d = new Landmarks.Def("Tea Room Garden", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return orient(teaUV(Math.max(bw, bh), Math.min(bw, bh)), bw >= bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] b = box(teaUV(lt(f), st(f))[0]);
                // A striped lawn inside a white picket fence.
                f.rect(0xFF72A656, 0, 0, L, S);
                for (float v = 0; v < S; v += 12) f.rect(0xFF7CB060, 0, v, L, v + 6);
                float gu = (b[0] + b[2]) / 2;
                f.line(0xFFF4F2EC, 0.8f, 1.5f, S - 1.5f, gu - 5, S - 1.5f);
                f.line(0xFFF4F2EC, 0.8f, gu + 5, S - 1.5f, L - 1.5f, S - 1.5f);
                f.line(0xFFF4F2EC, 0.8f, 1.5f, 1.5f, 1.5f, S - 1.5f);
                f.line(0xFFF4F2EC, 0.8f, L - 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                for (float u = 3; u < L - 1; u += 3) if (Math.abs(u - gu) > 5) f.circle(0xFFDAD8D0, u, S - 1.5f, 0.45f);
                // Gravel paths: gate to door, and a loop round the sundial.
                f.rect(0xFFE0D4B8, gu - 3, b[3], gu + 3, S - 1.5f);
                float cu = (b[2] + L) / 2 + 4, cv = (b[3] + S) / 2 + 4, rr = Math.min(L - b[2], S - b[3]) * 0.22f;
                if (rr > 10) {
                    f.ring(0xFFE0D4B8, 5, cu, cv, rr);
                    f.line(0xFFE0D4B8, 5, gu + 3, cv, cu - rr, cv);
                    f.circle(0xFF6A4E38, cu, cv, rr * 0.55f);
                    for (int k = 0; k < 12; k++) {
                        double a = k * Math.PI / 6;
                        f.circle(FLOWERS[k % FLOWERS.length], cu + (float) Math.cos(a) * rr * 0.4f, cv + (float) Math.sin(a) * rr * 0.4f, 1.3f);
                    }
                    f.circle(0x40000000, cu + 0.8f, cv + 0.8f, 2.4f);
                    f.circle(0xFFC8C0B0, cu, cv, 2.4f);
                    f.line(0xFF8A7A40, 0.7f, cu, cv, cu + 1.8f, cv - 1.8f);
                }
                // Cottage borders along the front of the tea room.
                for (float u = b[0]; u < b[2]; u += 2.4f) {
                    if (Math.abs(u - gu) < 4) continue;
                    f.circle(0xFF4E8A40, u, b[3] + 2.5f, 1.6f);
                    f.circle(FLOWERS[((int) (u * 1.7f)) % FLOWERS.length], u - 0.3f, b[3] + 2.2f, 0.9f);
                }
                // A rose arch over the path near the gate.
                float av = S - 10;
                f.sh(gu - 5, av - 1.2f, gu + 5, av + 1.2f, 1);
                f.rect(0xFFF4F2EC, gu - 5, av - 1.2f, gu + 5, av + 1.2f);
                for (float u = gu - 5; u <= gu + 5; u += 2) {
                    f.circle(0xFF4E8A40, u, av + ((int) u % 2 == 0 ? 0.6f : -0.6f), 1.3f);
                    f.circle(0xFFE86A8A, u, av, 0.8f);
                }
                // Box hedges in the corners, a birdbath and croquet hoops.
                hedge(f, b[2] + 6, 4, b[2] + 22, 9);
                hedge(f, L - 22, 4, L - 5, 9);
                hedge(f, 4, b[3] + 8, 9, S - 14);
                f.circle(0x40000000, L - 11, S * 0.38f + 1, 2.6f);
                f.circle(0xFFC8C0B0, L - 12, S * 0.38f, 2.6f);
                f.circle(0xFF8AB8D0, L - 12, S * 0.38f, 1.8f);
                for (int k = 0; k < 3; k++) {
                    float u = b[0] + 14 + k * 10, v = S - 16 - (k % 2) * 6;
                    f.line(0xFFF4F4F0, 0.5f, u - 1.4f, v, u - 1.4f, v - 1.4f);
                    f.line(0xFFF4F4F0, 0.5f, u + 1.4f, v, u + 1.4f, v - 1.4f);
                    f.line(0xFFF4F4F0, 0.5f, u - 1.4f, v - 1.4f, u + 1.4f, v - 1.4f);
                }
                f.circle(0xFFC8342A, b[0] + 20, S - 13, 0.9f);
                f.circle(0xFF3A6AC8, b[0] + 32, S - 20, 0.9f);
                // Tea tables with pastel cloths, cake stands and parasols.
                int[] cloth = {0xFFF4C8D4, 0xFFC8E8D4, 0xFFC8DCF0, 0xFFF4E4B8};
                int k = 0;
                for (float v = b[3] + 16; v < S - 12; v += 22) {
                    for (float u = b[2] + 12; u < L - 10; u += 24) {
                        float du = u - cu, dv = v - cv;
                        if (rr > 10 && du * du + dv * dv < (rr + 10) * (rr + 10)) continue;
                        table(f, u, v, 3.8f, cloth[k % 4], 0xFFF4F2EC, 3);
                        f.circle(0xFFF8F8F8, u, v, 2f);
                        f.circle(0xFFE89AB0, u, v, 1.3f);
                        f.circle(0xFFF8F8F8, u, v, 0.4f);
                        k++;
                    }
                }
                for (float v = 14; v < b[3] + 4 && b[2] + 30 < L - 30; v += 40) f.parasol(b[2] + 34, v + 4, 5, 0xFFF4C8D4, 0xFFF8F4EC);
                f.tree(L - 10, b[3] + 10, 7, 0xFF4E8A3A);
                f.sign(city, "TEA ROOM", gu, b[3] + 5, 5, 0xFF8A5A6A);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                // Thatch: soft rounded eaves, combed straw, a patterned ridge, eyebrow dormers and a chimney.
                f.roundRect(0x40000000, 1.5f, 2, L + 2, S + 2, S * 0.35f);
                f.roundRect(0xFF9A7A44, -1, -1, L + 1, S + 1, S * 0.35f);
                f.roundRect(0xFFCCA866, 0, 0, L, S / 2 + 1, S * 0.3f);
                f.roundRect(0xFFAE8A4E, 0, S / 2 - 1, L, S, S * 0.3f);
                for (int k = 0; k < 70; k++) {
                    float u = 2 + r.nextFloat() * (L - 4), v = 2 + r.nextFloat() * (S - 4);
                    float dv = v < S / 2 ? -1.8f : 1.8f;
                    f.line(v < S / 2 ? 0x30FFF0C0 : 0x24000000, 0.4f, u, v, u, v + dv);
                }
                f.rect(0xFF8A6A3A, S * 0.3f, S / 2 - 2, L - S * 0.3f, S / 2 + 2);
                for (float u = S * 0.3f; u < L - S * 0.3f - 2; u += 4) {
                    f.poly(0xFFAE8A4E, u, S / 2 - 2, u + 2, S / 2 - 4, u + 4, S / 2 - 2);
                    f.poly(0xFF7A5A30, u, S / 2 + 2, u + 2, S / 2 + 4, u + 4, S / 2 + 2);
                }
                for (float u = L * 0.25f; u < L * 0.8f; u += L * 0.28f) {
                    f.oval(0xFF9A7A44, u - 4, S - 9, u + 4, S - 3);
                    f.oval(0xFFB8945A, u - 3.4f, S - 8.6f, u + 3.4f, S - 5);
                    f.rect(0xFFE8E4D8, u - 2, S - 5.5f, u + 2, S - 4);
                }
                f.sh(L * 0.82f - 3, S / 2 - 3, L * 0.82f + 3, S / 2 + 3, 2.5f);
                f.rect(0xFFA0503A, L * 0.82f - 3, S / 2 - 3, L * 0.82f + 3, S / 2 + 3);
                f.circle(0xFF5A3A2A, L * 0.82f, S / 2, 1.2f);
                smoke(f, L * 0.82f + 1, S / 2 - 1, 1.6f, 0xFFE8E4DC);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.OLD | Variants.SUB | Variants.PARK;
        d.minS = 9;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_CAFE;
        d.roofCol = 0xFFCCA866;
        out.add(d);
    }

    // ------------------------------------------------------------------ 25 Chocolate Factory Shop

    static int[][] chocUV(int L, int S) {
        int lv = Math.max(4, Math.min(S - 5, S / 2)), lu = Math.max(lv + 1, L * 58 / 100), sl = L - lu - 3;
        if (sl >= 3) return new int[][]{{1, 1, lu, lv, 3}, {lu + 2, 1, sl, Math.max(3, lv * 6 / 10), 1}};
        return new int[][]{{1, 1, lu, lv, 3}};
    }

    static void lolly(F f, float u, float v, float r, int c1, int c2) {
        f.line(0x40000000, 0.8f, u, v, u + r * 1.6f, v + r * 1.8f);
        f.circle(0x40000000, u + 1.2f, v + 1.2f, r);
        f.circle(c1, u, v, r);
        for (float rr = r * 0.75f; rr > 0.8f; rr -= r * 0.3f) f.ring(c2, r * 0.13f, u, v, rr);
        f.circle(0x50FFFFFF, u - r * 0.35f, v - r * 0.35f, r * 0.25f);
    }

    static void chocolate(List<Landmarks.Def> out) {
        // A brick chocolate works with sawtooth roofs and two tall chimneys, a candy-striped shop and a chocolate river.
        Landmarks.Def d = new Landmarks.Def("Chocolate Factory Shop", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return orient(chocUV(Math.max(bw, bh), Math.min(bw, bh)), bw >= bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] pl = chocUV(lt(f), st(f));
                float[] b = box(pl[0]);
                f.rect(0xFF74AA58, 0, 0, L, S);
                f.rect(0xFF9A948A, 0, 0, b[2] + 6, b[1] - 3);
                f.car(b[2] - 14, 7, Math.PI, 0xFF6A3A8A, 6.5f);
                float fv = b[3];
                f.rect(0xFFB8AE9E, b[0] - 4, fv, b[2] + 4, fv + 6);
                // The chocolate river winding across the lawn, with a candy-striped bridge.
                float rv = (fv + 6 + S) / 2 + 2, amp = Math.min(8, (S - fv) * 0.12f);
                Path q = new Path();
                f.mv(q, 0, rv - 5);
                for (int k = 0; k < 4; k++) {
                    float u0 = k * L / 4, u1 = (k + 1) * L / 4;
                    f.qd(q, (u0 + u1) / 2, rv - 5 + (k % 2 == 0 ? -amp : amp) * 2, u1, rv - 5);
                }
                f.ln(q, L, rv + 5);
                for (int k = 3; k >= 0; k--) {
                    float u0 = (k + 1) * L / 4, u1 = k * L / 4;
                    f.qd(q, (u0 + u1) / 2, rv + 5 + (k % 2 == 0 ? -amp : amp) * 2, u1, rv + 5);
                }
                q.close();
                f.stroke(q, 0xFF8A6A44, 2.5f);
                f.fill(q, 0xFF5A3420);
                for (int k = 0; k < 8; k++) {
                    float u = L * (k + 0.5f) / 8, t = (k + 0.5f) / 8 * 4;
                    float off = (float) Math.sin(t * Math.PI) * amp * ((int) t % 2 == 0 ? -1 : 1);
                    f.line(0x50C89A70, 0.6f, u - 4, rv + off * 0.3f, u + 4, rv + off * 0.3f);
                }
                float bu = L * 0.25f;
                f.sh(bu - 4, rv - 12, bu + 4, rv + 12, 1.5f);
                for (float v = rv - 12; v < rv + 12; v += 3) f.rect(((int) ((v - rv) / 3 + 10)) % 2 == 0 ? 0xFFF08AB0 : 0xFFF8F4F0, bu - 4, v, bu + 4, v + 3);
                // A pink path from the road to the shop, lollipop trees and gumdrop bushes.
                float su = pl.length > 1 ? (box(pl[1])[0] + box(pl[1])[2]) / 2 : L * 0.8f, sv1 = pl.length > 1 ? box(pl[1])[3] : fv;
                f.rect(0xFFF4C8D8, su - 4, sv1, su + 4, S);
                for (float v = sv1 + 2; v < S; v += 4) f.rect(0xFFF8E0EA, su - 4, v, su + 4, v + 1.5f);
                int[] lc = {0xFFE84A8A, 0xFF4AB8E8, 0xFFF0C030, 0xFF8A5AD0};
                int k = 0;
                for (float u = 10; u < L - 6; u += 26, k++) {
                    if (Math.abs(u - su) < 10 || Math.abs(u - bu) < 8) continue;
                    lolly(f, u, S - 9, 4.5f, lc[k % 4], 0xFFF8F4F0);
                    lolly(f, u + 9, fv + 14, 3.6f, lc[(k + 2) % 4], 0xFFF8F4F0);
                }
                for (int j = 0; j < 10; j++) {
                    float u = 6 + r.nextFloat() * (L - 12), v = fv + 8 + r.nextFloat() * (S - fv - 14);
                    if (Math.abs(v - rv) < 10 || Math.abs(u - su) < 8) continue;
                    f.circle(0x30000000, u + 0.8f, v + 0.8f, 2);
                    f.circle(lc[j % 4], u, v, 2);
                    f.circle(0x60FFFFFF, u - 0.6f, v - 0.6f, 0.7f);
                }
                // The shop's fun entrance: a striped canopy with giant lollipops either side.
                if (pl.length > 1) {
                    float[] s = box(pl[1]);
                    float cu = (s[0] + s[2]) / 2;
                    f.sh(cu - 9, s[3], cu + 9, s[3] + 7, 1.6f);
                    for (float u = cu - 9; u < cu + 9; u += 3) f.rect(((int) ((u - cu) / 3 + 10)) % 2 == 0 ? 0xFFE84A8A : 0xFFF8F4F0, u, s[3], u + 3, s[3] + 7);
                    f.rect(0x24000000, cu - 9, s[3] + 3.5f, cu + 9, s[3] + 7);
                    lolly(f, cu - 12, s[3] + 8, 3.6f, 0xFF4AB8E8, 0xFFF8F4F0);
                    lolly(f, cu + 12, s[3] + 8, 3.6f, 0xFFF0C030, 0xFFE84A8A);
                    f.sign(city, "CHOCOLATE SHOP", cu, s[3] + 4, 5, 0xFFE84A8A);
                }
                f.sign(city, "CHOCOLATE WORKS", (b[0] + b[2]) / 2, fv + 3, 6, 0xFF5A3420);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                if (part == 0) {
                    // Sawtooth roofs: brick slopes and north-light glazing, two chimneys and the pipework.
                    float t = Math.max(8, S / Math.max(3, Math.round(S / 12f)));
                    for (float v = 0; v < S - 0.5f; v += t) {
                        float m = v + t * 0.68f;
                        f.rect(0xFF9A4E3A, 0, v, L, m);
                        f.rect(0xFFB0604A, 0, v, L, v + (m - v) * 0.4f);
                        f.rect(0xFFA8C8D4, 0, m, L, Math.min(v + t, S));
                        for (float u = 4; u < L; u += 4) f.line(0x50FFFFFF, 0.4f, u, m, u, Math.min(v + t, S));
                        f.line(0x40000000, 0.8f, 0, m, L, m);
                    }
                    f.frame(0xFF6A3426, 1.2f, 0, 0, L, S);
                    f.line(0xFF8A8E92, 1.6f, L * 0.18f, S * 0.32f, L * 0.62f, S * 0.32f);
                    f.line(0xFF8A8E92, 1.6f, L * 0.62f, S * 0.32f, L * 0.62f, S * 0.7f);
                    f.line(0xFFB8BCC0, 0.5f, L * 0.18f, S * 0.32f - 0.5f, L * 0.62f, S * 0.32f - 0.5f);
                    unit(f, L * 0.4f, S * 0.55f, 8, 6);
                    int[] band = {0xFFE84A8A, 0xFF8A5AD0};
                    for (int k = 0; k < 2; k++) {
                        float cu = k == 0 ? L * 0.14f : L * 0.82f, cv = k == 0 ? S * 0.3f : S * 0.68f, cr = Math.max(4.5f, S * 0.09f);
                        f.circle(0x50000000, cu + cr * 0.8f, cv + cr * 0.8f, cr);
                        f.circle(0xFF8A3A2A, cu, cv, cr);
                        f.ring(band[k], cr * 0.25f, cu, cv, cr * 0.82f);
                        f.circle(0xFF6A2A1E, cu, cv, cr * 0.62f);
                        f.circle(0xFF1E1410, cu, cv, cr * 0.42f);
                        smoke(f, cu + 2, cv + 1, cr * 0.6f, k == 0 ? 0xFFC8A890 : 0xFFE8C8D8);
                    }
                    f.sign(city, "CHOCOLATE", L / 2, S / 2, 7, 0xFFE84A8A);
                } else {
                    // The shop: a candy-striped roof with a giant swirl lollipop.
                    f.rect(0xFFF8F4F0, 0, 0, L, S);
                    c.save();
                    c.clipRect(x0, y0, x1, y1);
                    for (float u = -S; u < L; u += 6) f.poly(0xFFE84A8A, u, 0, u + 3, 0, u + 3 + S, S, u + S, S);
                    c.restore();
                    f.frame(0xFFB8306A, 1.2f, 0, 0, L, S);
                    lolly(f, L / 2, S / 2, Math.min(L, S) * 0.3f, 0xFF8A5AD0, 0xFFF0C030);
                }
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.IND | Variants.MID | Variants.SUB;
        d.minS = 10;
        d.minL = 13;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_FACTORY;
        d.roofCol = 0xFF9A4E3A;
        out.add(d);
    }

    // @@MORE@@
}
