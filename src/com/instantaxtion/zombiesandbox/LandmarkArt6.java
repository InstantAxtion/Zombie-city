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
        d.inside = Variants.I_SEATS;
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
                for (int k = 0; k < 8; k++) {
                    double an = k * Math.PI / 4 + Math.PI / 8;
                    float ca = (float) Math.cos(an), sa = (float) Math.sin(an);
                    if (k == 0) {
                        f.mv(q, cu + ca * 11, cv + sa * 11);
                        f.mv(w, cu + ca * 9, cv + sa * 9);
                    } else {
                        f.ln(q, cu + ca * 11, cv + sa * 11);
                        f.ln(w, cu + ca * 9, cv + sa * 9);
                    }
                }
                q.close();
                w.close();
                f.circle(0x38000000, cu + 2, cv + 2, 11);
                f.fill(q, 0xFFE8DCC4);
                f.fill(w, 0xFF4A94B8);
                f.circle(0xFF6AAED0, cu, cv, 6);
                f.circle(0xFFE8DCC4, cu, cv, 2.5f);
                f.circle(0xFFF4ECD8, cu - 0.6f, cv - 0.6f, 1.2f);
                for (int k = 0; k < 10; k++) {
                    double an = k * Math.PI / 5;
                    f.circle(0xA0FFFFFF, cu + (float) Math.cos(an) * 4.2f, cv + (float) Math.sin(an) * 4.2f, 0.6f);
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
                for (float u = pu + 6; u < L - 6; u += 22) {
                    for (float v = 12; v < S - 8; v += 22) {
                        float du = u - cu, dv = v - cv;
                        if (du * du + dv * dv < (R * 0.4f + 8) * (R * 0.4f + 8)) continue;
                        checkTable(f, u, v, 2.6f);
                        if (k++ % 3 == 0) f.parasol(u, v, 5.5f, 0xFFF0E6CC, 0xFF2E6A4A);
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
                if (iv1 - iv0 > 6) {
                    f.roundRect(0xFFE8DCC0, iu0 - 1, iv0 - 1, iu1 + 1, iv1 + 1, (iv1 - iv0) / 2 + 1);
                    f.roundRect(0xFF2E7EA8, iu0, iv0, iu1, iv1, (iv1 - iv0) / 2);
                    f.roundRect(0xFF4A9AC4, iu0 + 2, iv0 + 2, iu1 - 2, iv1 - 2, (iv1 - iv0) / 2 - 2);
                    float iv = (iv0 + iv1) / 2;
                    for (float u = iu0 + 6; u < iu1 - 4; u += 6) {
                        f.circle(0x80FFFFFF, u, iv, 1.8f);
                        f.circle(0xE0FFFFFF, u, iv, 0.9f);
                    }
                    f.circle(0xC0FFFFFF, cu, iv, 3);
                    f.circle(0xFFFFFFFF, cu, iv, 1.4f);
                } else {
                    f.roundRect(0xFF5E9446, iu0, iv0 - 3, iu1, iv1 + 3, 3);
                }
                // Palms round the lawns and red flower beds by the road.
                for (float u = 8; u < du0 - 4; u += 12) f.palm(u, (fv + S) / 2, 5);
                for (float u = L - 8; u > du1 + 4; u -= 12) f.palm(u, (fv + S) / 2, 5);
                for (float u = 8; u < L - 4; u += 22) {
                    if (u > du0 - 6 && u < du1 + 6) continue;
                    f.roundRect(0xFFB8283A, u - 4, S - 6, u + 4, S - 2, 2);
                }
                // The porte-cochère: a red carpet under a gilded canopy, a limousine waiting.
                f.rect(0xFFA0182A, cu - 5, fv, cu + 5, dv0 + 2);
                f.car(cu + 22, dv0 + 7, 0, 0xFF1E1E22, 8.5f);
                float pc0 = cu - 18, pc1 = cu + 18, pv1 = fv + 15;
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

    // @@MORE@@
}
