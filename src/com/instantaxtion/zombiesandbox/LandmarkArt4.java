package com.instantaxtion.zombiesandbox;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.Landmarks.*;

/** ((22.4) Landmark designs, part 4 (see Landmarks): towers, tech, campus and industrial heritage. */
final class LandmarkArt4 {
    private LandmarkArt4() {
    }

    private static final float U = City.T;

    // ------------------------------------------------------------------ shared helpers

    /**
     * Draws in a block's own axes: u runs along its long side and v across it, whichever way round the block is,
     * so one layout serves wide and tall blocks (a tall block is the wide layout mirrored on its diagonal).
     */
    static final class Ax {
        final Canvas c;
        final Paint p;
        final float ox, oy, L, S;
        final boolean tall;

        Ax(Canvas c, Paint p, float x0, float y0, float x1, float y1) {
            this.c = c;
            this.p = p;
            ox = x0;
            oy = y0;
            tall = y1 - y0 > x1 - x0;
            L = tall ? y1 - y0 : x1 - x0;
            S = tall ? x1 - x0 : y1 - y0;
        }

        int lt() {
            return Math.round(L / U);
        }

        int st() {
            return Math.round(S / U);
        }

        float x(float u, float v) {
            return ox + (tall ? v : u);
        }

        float y(float u, float v) {
            return oy + (tall ? u : v);
        }

        void R(int col, float u0, float v0, float u1, float v1) {
            rect(c, p, col, x(u0, v0), y(u0, v0), x(u1, v1), y(u1, v1));
        }

        void RR(int col, float u0, float v0, float u1, float v1, float rad) {
            roundRect(c, p, col, x(u0, v0), y(u0, v0), x(u1, v1), y(u1, v1), rad);
        }

        void O(int col, float u0, float v0, float u1, float v1) {
            oval(c, p, col, x(u0, v0), y(u0, v0), x(u1, v1), y(u1, v1));
        }

        void C(int col, float u, float v, float r) {
            circle(c, p, col, x(u, v), y(u, v), r);
        }

        void Ln(int col, float w, float u0, float v0, float u1, float v1) {
            line(c, p, col, w, x(u0, v0), y(u0, v0), x(u1, v1), y(u1, v1));
        }

        void P(int col, float... uv) {
            float[] q = new float[uv.length];
            for (int k = 0; k + 1 < uv.length; k += 2) {
                q[k] = x(uv[k], uv[k + 1]);
                q[k + 1] = y(uv[k], uv[k + 1]);
            }
            poly(c, p, col, q);
        }

        void tr(float u, float v, float r, int col) {
            Landmarks.tree(c, p, x(u, v), y(u, v), r, col);
        }

        void bu(float u, float v, float r) {
            Landmarks.bush(c, p, x(u, v), y(u, v), r);
        }

        void lm(float u, float v) {
            Landmarks.lamp(c, p, x(u, v), y(u, v));
        }

        /** A bench whose length runs along u (alongU) or along v. */
        void be(float u, float v, boolean alongU) {
            Landmarks.bench(c, p, x(u, v), y(u, v), alongU != tall);
        }

        /** A soft shadow of a box d high, cast to the lower right in world terms. */
        void sh(float u0, float v0, float u1, float v1, float d) {
            float a = x(u0, v0), b = y(u0, v0), e = x(u1, v1), f = y(u1, v1);
            rect(c, p, 0x40000000, a + d * 0.6f, b + d, e + d * 0.6f, f + d);
        }

        void track(float u0, float v0, float u1, float v1) {
            LandmarkArt4.track(c, p, x(u0, v0), y(u0, v0), x(u1, v1), y(u1, v1));
        }
    }

    /** A plan laid out for a wide block, turned for a tall one. */
    static int[][] tp(int[][] a, boolean tall) {
        if (!tall) return a;
        int[][] b = new int[a.length][];
        for (int i = 0; i < a.length; i++) b[i] = new int[]{a[i][1], a[i][0], a[i][3], a[i][2], a[i][4]};
        return b;
    }

    static int clampi(int v, int lo, int hi) {
        return v < lo ? lo : v > hi ? hi : v;
    }

    static int mix(int a, int b, float t) {
        t = t < 0 ? 0 : t > 1 ? 1 : t;
        int r = 0;
        for (int s = 0; s <= 24; s += 8) {
            int ca = (a >>> s) & 0xFF, cb = (b >>> s) & 0xFF;
            r |= ((int) (ca + (cb - ca) * t) & 0xFF) << s;
        }
        return r;
    }

    static void ring(Canvas c, Paint p, int col, float w, float x, float y, float r) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(w);
        p.setColor(col);
        c.drawCircle(x, y, r, p);
        p.setStyle(Paint.Style.FILL);
    }

    static void ovalRing(Canvas c, Paint p, int col, float w, float x0, float y0, float x1, float y1) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(w);
        p.setColor(col);
        RF.set(x0, y0, x1, y1);
        c.drawOval(RF, p);
        p.setStyle(Paint.Style.FILL);
    }

    static void hex(Canvas c, Paint p, int col, float x, float y, float r) {
        float h = r * 0.866f;
        poly(c, p, col, x + r, y, x + r / 2, y + h, x - r / 2, y + h, x - r, y, x - r / 2, y - h, x + r / 2, y - h);
    }

    /** A pitched roof with its ridge along the long side, the slope facing the light paler, and tile courses. */
    static void gable(Canvas c, Paint p, float x0, float y0, float x1, float y1, int col, float course) {
        rect(c, p, City.darken(col, 0.6f), x0, y0, x1, y1);
        if (x1 - x0 >= y1 - y0) {
            float my = (y0 + y1) / 2;
            rect(c, p, City.lighten(col, 0.12f), x0 + 0.8f, y0 + 0.8f, x1 - 0.8f, my);
            rect(c, p, City.darken(col, 0.8f), x0 + 0.8f, my, x1 - 0.8f, y1 - 0.8f);
            if (course > 0) for (float y = y0 + course; y < y1 - 1; y += course) line(c, p, 0x24000000, 0.4f, x0 + 1, y, x1 - 1, y);
            line(c, p, City.lighten(col, 0.35f), 1.2f, x0 + 0.8f, my, x1 - 0.8f, my);
        } else {
            float mx = (x0 + x1) / 2;
            rect(c, p, City.lighten(col, 0.12f), x0 + 0.8f, y0 + 0.8f, mx, y1 - 0.8f);
            rect(c, p, City.darken(col, 0.8f), mx, y0 + 0.8f, x1 - 0.8f, y1 - 0.8f);
            if (course > 0) for (float x = x0 + course; x < x1 - 1; x += course) line(c, p, 0x24000000, 0.4f, x, y0 + 1, x, y1 - 1);
            line(c, p, City.lighten(col, 0.35f), 1.2f, mx, y0 + 0.8f, mx, y1 - 0.8f);
        }
    }

    /** A hipped roof: four shaded facets meeting on a short ridge. */
    static void hip(Canvas c, Paint p, float x0, float y0, float x1, float y1, int col) {
        float w = x1 - x0, h = y1 - y0, mx = (x0 + x1) / 2, my = (y0 + y1) / 2;
        rect(c, p, City.darken(col, 0.6f), x0, y0, x1, y1);
        if (w >= h) {
            float in = h / 2;
            poly(c, p, City.lighten(col, 0.14f), x0, y0, x1, y0, x1 - in, my, x0 + in, my);
            poly(c, p, City.darken(col, 0.76f), x0, y1, x1, y1, x1 - in, my, x0 + in, my);
            poly(c, p, City.lighten(col, 0.05f), x0, y0, x0 + in, my, x0, y1);
            poly(c, p, City.darken(col, 0.88f), x1, y0, x1 - in, my, x1, y1);
            line(c, p, City.lighten(col, 0.3f), 1, x0 + in, my, x1 - in, my);
        } else {
            float in = w / 2;
            poly(c, p, City.lighten(col, 0.14f), x0, y0, x0, y1, mx, y1 - in, mx, y0 + in);
            poly(c, p, City.darken(col, 0.8f), x1, y0, x1, y1, mx, y1 - in, mx, y0 + in);
            poly(c, p, City.lighten(col, 0.08f), x0, y0, x1, y0, mx, y0 + in);
            poly(c, p, City.darken(col, 0.72f), x0, y1, x1, y1, mx, y1 - in);
            line(c, p, City.lighten(col, 0.3f), 1, mx, y0 + in, mx, y1 - in);
        }
    }

    /** A railway track on ballast with sleepers, from a to b. */
    static void track(Canvas c, Paint p, float ax, float ay, float bx, float by) {
        float dx = bx - ax, dy = by - ay, len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return;
        float ux = dx / len, uy = dy / len, nx = -uy, ny = ux;
        line(c, p, 0xFFA29C92, 10, ax, ay, bx, by);
        for (float t = 1.5f; t < len; t += 3.6f) {
            float px = ax + ux * t, py = ay + uy * t;
            line(c, p, 0xFF5C4836, 1.5f, px + nx * 4.3f, py + ny * 4.3f, px - nx * 4.3f, py - ny * 4.3f);
        }
        for (int s = -1; s <= 1; s += 2) {
            float ox = nx * 2.5f * s, oy = ny * 2.5f * s;
            line(c, p, 0xFF5E5C5A, 0.9f, ax + ox, ay + oy, bx + ox, by + oy);
            line(c, p, 0xFFC4C4C8, 0.35f, ax + ox, ay + oy, bx + ox, by + oy);
        }
    }

    /** A steam locomotive and tender from above, centred at x, y and pointing along deg. */
    static void loco(Canvas c, Paint p, float cx, float cy, float deg, float len, int col) {
        float wd = len * 0.19f, hl = len / 2, hw = wd / 2;
        double a = Math.toRadians(deg);
        float sx = (float) (1.5 * Math.cos(a) + 2.4 * Math.sin(a)), sy = (float) (-1.5 * Math.sin(a) + 2.4 * Math.cos(a));
        c.save();
        c.translate(cx, cy);
        c.rotate(deg);
        roundRect(c, p, 0x50000000, -hl + sx, -hw + sy, hl + sx, hw + sy, 2);
        // Tender with its coal.
        float t1 = -hl + len * 0.3f;
        roundRect(c, p, 0xFF1C1C1C, -hl, -hw, t1, hw, 1.5f);
        rect(c, p, City.darken(col, 0.8f), -hl + 0.8f, -hw + 0.8f, t1 - 0.8f, hw - 0.8f);
        rect(c, p, 0xFF151515, -hl + 2.2f, -hw + 1.6f, t1 - 3, hw - 1.6f);
        circle(c, p, 0xFF3A3A3A, -hl + len * 0.1f, -hw * 0.2f, 0.8f);
        circle(c, p, 0xFF3A3A3A, -hl + len * 0.17f, hw * 0.25f, 0.7f);
        // Running plate, cab, boiler, smokebox, dome and chimney.
        rect(c, p, 0xFF262626, t1 + 0.6f, -hw, hl - len * 0.04f, hw);
        float c0 = t1 + 0.8f, c1 = -hl + len * 0.5f;
        rect(c, p, City.darken(col, 0.65f), c0, -hw, c1, hw);
        rect(c, p, City.lighten(col, 0.12f), c0 + 0.8f, -hw + 0.8f, c1 - 0.8f, hw - 0.8f);
        line(c, p, 0x50FFFFFF, 0.6f, c0 + 1, -hw + 1.4f, c1 - 1, -hw + 1.4f);
        float b0 = c1, b1 = hl - len * 0.06f, bw = wd * 0.34f;
        rect(c, p, col, b0, -bw, b1, bw);
        rect(c, p, City.lighten(col, 0.25f), b0, -bw, b1, -bw * 0.3f);
        for (float x = b0 + len * 0.07f; x < b1 - len * 0.1f; x += len * 0.08f) line(c, p, 0xFFC09838, 0.6f, x, -bw, x, bw);
        roundRect(c, p, 0xFF1E1E1E, b1 - len * 0.1f, -bw, b1, bw, 1.5f);
        circle(c, p, 0xFF0E0E0E, b1 - len * 0.05f, 0, bw * 0.55f);
        circle(c, p, 0xFF3A3A3A, b1 - len * 0.05f, 0, bw * 0.3f);
        circle(c, p, 0xFFC8A040, (b0 + b1) / 2 - len * 0.02f, 0, bw * 0.6f);
        circle(c, p, 0xFFF0D890, (b0 + b1) / 2 - len * 0.02f - 0.4f, -0.4f, bw * 0.22f);
        rect(c, p, 0xFFB02A20, hl - len * 0.04f, -hw, hl, hw);
        circle(c, p, 0xFF222222, hl, -hw * 0.6f, 0.9f);
        circle(c, p, 0xFF222222, hl, hw * 0.6f, 0.9f);
        c.restore();
    }

    /** An old passenger carriage from above. */
    static void carriage(Canvas c, Paint p, float cx, float cy, float deg, float len, int col) {
        float hl = len / 2, hw = len * 0.1f;
        double a = Math.toRadians(deg);
        float sx = (float) (1.5 * Math.cos(a) + 2.4 * Math.sin(a)), sy = (float) (-1.5 * Math.sin(a) + 2.4 * Math.cos(a));
        c.save();
        c.translate(cx, cy);
        c.rotate(deg);
        roundRect(c, p, 0x50000000, -hl + sx, -hw + sy, hl + sx, hw + sy, 2);
        roundRect(c, p, City.darken(col, 0.7f), -hl, -hw, hl, hw, 2);
        roundRect(c, p, 0xFF8E8C88, -hl + 1, -hw + 0.8f, hl - 1, hw - 0.8f, 2);
        rect(c, p, 0xFFAEACA6, -hl + 3, -hw * 0.32f, hl - 3, hw * 0.32f);
        line(c, p, col, 0.8f, -hl + 1, -hw + 0.6f, hl - 1, -hw + 0.6f);
        for (float x = -hl + 5; x < hl - 3; x += len / 6) circle(c, p, 0xFF5A5854, x, 0, 0.8f);
        rect(c, p, 0xFF2A2A2A, -hl - 1, -hw * 0.4f, -hl, hw * 0.4f);
        rect(c, p, 0xFF2A2A2A, hl, -hw * 0.4f, hl + 1, hw * 0.4f);
        c.restore();
    }

    /** One wind turbine blade (or its shadow) from the hub along deg. */
    static void blade(Canvas c, Paint p, int col, float hx, float hy, float R, float deg) {
        double a = Math.toRadians(deg);
        float dx = (float) Math.cos(a), dy = (float) Math.sin(a), nx = -dy, ny = dx, ch = Math.max(1.2f, R * 0.075f);
        poly(c, p, col,
                hx + dx * R * 0.06f + nx * ch * 0.5f, hy + dy * R * 0.06f + ny * ch * 0.5f,
                hx + dx * R * 0.22f + nx * ch, hy + dy * R * 0.22f + ny * ch,
                hx + dx * R + nx * ch * 0.18f, hy + dy * R + ny * ch * 0.18f,
                hx + dx * R - nx * ch * 0.12f, hy + dy * R - ny * ch * 0.12f,
                hx + dx * R * 0.22f - nx * ch * 0.45f, hy + dy * R * 0.22f - ny * ch * 0.45f,
                hx + dx * R * 0.06f - nx * ch * 0.4f, hy + dy * R * 0.06f - ny * ch * 0.4f);
    }

    static void turbine(Canvas c, Paint p, float x, float y, float R, float ang) {
        circle(c, p, 0xFFB4AC94, x, y, Math.max(5, R * 0.17f));
        circle(c, p, 0xFF9C9480, x, y, Math.max(3.5f, R * 0.11f));
        float sx = R * 0.32f, sy = R * 0.52f;
        for (int k = 0; k < 3; k++) blade(c, p, 0x34000000, x + sx, y + sy, R, ang + k * 120);
        line(c, p, 0x30000000, 2.6f, x, y, x + sx, y + sy);
        circle(c, p, 0xFFE6E6E4, x, y, 2.5f);
        // Nacelle facing into a north-westerly wind.
        c.save();
        c.translate(x, y);
        c.rotate(-135);
        roundRect(c, p, 0x40000000, -7 + 1, -2.4f + 1.4f, 3 + 1, 2.4f + 1.4f, 1.5f);
        roundRect(c, p, 0xFFD6D8D8, -7, -2.4f, 3, 2.4f, 1.5f);
        rect(c, p, 0xFFF2F2F0, -6, -2.4f, 2, -0.6f);
        c.restore();
        for (int k = 0; k < 3; k++) {
            float d = ang + k * 120;
            blade(c, p, 0xFFF4F4F2, x, y, R, d);
            double a = Math.toRadians(d);
            line(c, p, 0xFFC8483A, Math.max(0.8f, R * 0.04f), x + (float) Math.cos(a) * R * 0.86f, y + (float) Math.sin(a) * R * 0.86f,
                    x + (float) Math.cos(a) * R * 0.97f, y + (float) Math.sin(a) * R * 0.97f);
        }
        circle(c, p, 0xFFBEC2C4, x, y, 2.2f);
        circle(c, p, 0xFFF8F8F8, x - 0.6f, y - 0.6f, 1.1f);
    }

    // ------------------------------------------------------------------ the designs

    static void register(List<Landmarks.Def> out) {
        // 1. A glass office tower on a granite plaza: curtain-wall grid, a stepped crown and a helipad on top.
        Landmarks.Def d = new Landmarks.Def("Glass Skyscraper", T_BUILDING) {
            int[][] lp(int L, int S) {
                int tw = clampi(Math.round(S * 0.46f), 4, 8);
                int tl = Math.max(tw, Math.min(L - 6, Math.round(tw * 1.25f)));
                return new int[][]{{(L - tl) / 2, (S - tw) / 2, tl, tw, 40}};
            }

            @Override
            int[][] plan(int bw, int bh, Random r) {
                int[][] q = lp(Math.max(bw, bh), Math.min(bw, bh));
                q[0][4] = 32 + r.nextInt(17);
                return tp(q, bh > bw);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                Ax a = new Ax(c, p, x0, y0, x1, y1);
                int[] q = lp(a.lt(), a.st())[0];
                float u0 = q[0] * U, v0 = q[1] * U, u1 = u0 + q[2] * U, v1 = v0 + q[3] * U, um = (u0 + u1) / 2;
                rect(c, p, 0xFFC8C4BC, x0, y0, x1, y1);
                for (float x = x0 + 8; x < x1; x += 8) line(c, p, 0x1C000000, 0.5f, x, y0, x, y1);
                for (float y = y0 + 8; y < y1; y += 8) line(c, p, 0x1C000000, 0.5f, x0, y, x1, y);
                // A dark granite apron round the tower.
                a.R(0xFF8C8882, u0 - 12, v0 - 12, u1 + 12, v1 + 12);
                a.R(0xFFAAA69E, u0 - 10, v0 - 10, u1 + 10, v1 + 10);
                a.R(0xFF74726E, u0 - 2, v0 - 2, u1 + 2, v1 + 2);
                // Glass canopies over the two entrances.
                for (int s = 0; s < 2; s++) {
                    float vv = s == 0 ? v0 - 9 : v1;
                    a.R(0x40000000, um - 14, vv + 1.5f, um + 16.5f, vv + 10.5f);
                    a.R(0xFF9CC0D4, um - 15, vv, um + 15, vv + 9);
                    for (float t = um - 15; t <= um + 15.1f; t += 5) a.Ln(0xFF6E8EA2, 0.5f, t, vv, t, vv + 9);
                    a.Ln(0x80FFFFFF, 0.6f, um - 15, vv + 0.5f, um + 15, vv + 0.5f);
                }
                // Planters and street trees along the long sides.
                for (int s = 0; s < 2; s++) {
                    float sv0 = s == 0 ? 3 : v1 + 13, sv1 = s == 0 ? v0 - 13 : a.S - 3;
                    if (sv1 - sv0 < 9) continue;
                    float vc = (sv0 + sv1) / 2;
                    int k = 0;
                    for (float t = 8; t + 16 < a.L - 6; t += 26, k++) {
                        if (Math.abs(t + 8 - um) < 22) continue;
                        if (k % 2 == 0) {
                            a.RR(0xFF7E786E, t, vc - 4.5f, t + 16, vc + 4.5f, 2);
                            a.RR(0xFF5A4834, t + 1.2f, vc - 3.3f, t + 14.8f, vc + 3.3f, 1.5f);
                            a.bu(t + 4, vc, 2.6f);
                            a.bu(t + 8, vc - 0.5f, 2.8f);
                            a.bu(t + 12, vc, 2.6f);
                        } else {
                            a.R(0xFF68686A, t + 4, vc - 4, t + 12, vc + 4);
                            a.tr(t + 8, vc, 6, 0xFF4A8A44);
                        }
                    }
                }
                // A linear fountain at one end, benches and trees at the other.
                float e0 = 4, e1 = u0 - 14;
                if (e1 - e0 > 10) {
                    float eu = (e0 + e1) / 2, va = v0 + 4, vb = v1 - 4;
                    a.R(0xFF8A867E, eu - 6, va - 2, eu + 6, vb + 2);
                    a.R(0xFF3E7EA6, eu - 4.5f, va, eu + 4.5f, vb);
                    a.R(0xFF5A9CC4, eu - 3, va + 1.5f, eu + 3, vb - 1.5f);
                    for (float t = va + 4; t < vb - 2; t += 6) a.C(0xB0FFFFFF, eu, t, 1);
                    if (e1 - e0 > 24) {
                        a.be(eu - 10, (va + vb) / 2, false);
                        a.be(eu + 10, (va + vb) / 2, false);
                    }
                }
                float f0 = u1 + 14, f1 = a.L - 4;
                if (f1 - f0 > 10) {
                    float fu = (f0 + f1) / 2;
                    a.tr(fu, v0 + 6, 6, 0xFF3E7A3A);
                    a.tr(fu, v1 - 6, 6, 0xFF3E7A3A);
                    a.be(fu, (v0 + v1) / 2, false);
                }
                lamp(c, p, x0 + 4, y0 + 4);
                lamp(c, p, x1 - 4, y0 + 4);
                lamp(c, p, x0 + 4, y1 - 4);
                lamp(c, p, x1 - 4, y1 - 4);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                float w = x1 - x0, h = y1 - y0, m = Math.min(w, h), cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
                rect(c, p, 0xFF20374C, x0, y0, x1, y1);
                rect(c, p, 0xFF3A6284, x0 + 1, y0 + 1, x1 - 1, y1 - 1);
                for (float x = x0 + 3.2f; x < x1 - 1; x += 3.2f) line(c, p, 0xFF294660, 0.45f, x, y0 + 1, x, y1 - 1);
                for (float y = y0 + 4; y < y1 - 1; y += 4) line(c, p, 0xFF294660, 0.45f, x0 + 1, y, x1 - 1, y);
                poly(c, p, 0x26FFFFFF, x0 + 1, y0 + h * 0.5f, x0 + w * 0.5f, y0 + 1, x0 + w * 0.7f, y0 + 1, x0 + 1, y0 + h * 0.7f);
                // The stepped crown: three set-backs, the last a plant deck.
                float st = m * 0.1f;
                int[] tc = {0xFF4A7698, 0xFF5D8AAC, 0xFF9A9EA2};
                for (int k = 1; k <= 3; k++) {
                    float i = k * st;
                    rect(c, p, 0x60000000, x0 + i + 1.2f, y0 + i + 1.4f, x1 - i + 1.2f, y1 - i + 1.4f);
                    rect(c, p, tc[k - 1], x0 + i, y0 + i, x1 - i, y1 - i);
                    if (k < 3) for (float x = x0 + i + 3.2f; x < x1 - i - 0.5f; x += 3.2f) line(c, p, 0x24FFFFFF, 0.4f, x, y0 + i, x, y1 - i);
                    line(c, p, 0x90FFFFFF, 0.6f, x0 + i, y0 + i, x1 - i, y0 + i);
                    line(c, p, 0x90FFFFFF, 0.6f, x0 + i, y0 + i, x0 + i, y1 - i);
                }
                float i3 = 3 * st;
                // Window-cleaning gantry and plant on the deck.
                rect(c, p, 0xFFD8B030, x0 + i3 + 1, y0 + i3 + 1, x0 + i3 + 4, y0 + i3 + 3.5f);
                line(c, p, 0xFFD8B030, 0.8f, x0 + i3 + 2.5f, y0 + i3 + 2, x0 + st * 1.4f, y0 + st * 1.4f);
                rect(c, p, 0xFF7A7E82, x1 - i3 - 6, y1 - i3 - 5, x1 - i3 - 1, y1 - i3 - 1);
                circle(c, p, 0xFF4A4E52, x1 - i3 - 3.5f, y1 - i3 - 3, 1.5f);
                // The helipad.
                float hr = Math.max(4, m / 2 - i3 - 1.5f);
                circle(c, p, 0x50000000, cx + 0.8f, cy + 1.2f, hr);
                circle(c, p, 0xFF3C4044, cx, cy, hr);
                ring(c, p, 0xFFE8C030, Math.max(0.6f, hr * 0.07f), cx, cy, hr * 0.84f);
                rect(c, p, 0xFFF4F4F0, cx - hr * 0.36f, cy - hr * 0.45f, cx - hr * 0.22f, cy + hr * 0.45f);
                rect(c, p, 0xFFF4F4F0, cx + hr * 0.22f, cy - hr * 0.45f, cx + hr * 0.36f, cy + hr * 0.45f);
                rect(c, p, 0xFFF4F4F0, cx - hr * 0.22f, cy - hr * 0.07f, cx + hr * 0.22f, cy + hr * 0.07f);
                for (int k = 0; k < 8; k++) {
                    double a = k * Math.PI / 4 + Math.PI / 8;
                    circle(c, p, k % 2 == 0 ? 0xFF70E070 : 0xFFF8F0B0, cx + (float) Math.cos(a) * hr * 0.97f, cy + (float) Math.sin(a) * hr * 0.97f, 0.7f);
                }
                circle(c, p, 0xFFE03020, x0 + st * 0.5f, y0 + st * 0.5f, 0.9f);
                circle(c, p, 0xFFE03020, x1 - st * 0.5f, y1 - st * 0.5f, 0.9f);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.MID;
        d.minS = 10;
        d.minL = 10;
        d.base = City.OFFICE;
        d.inside = Variants.I_OFFICE;
        d.roofCol = 0xFF3A6284;
        out.add(d);

        // 2. A tower whose floor plates turn as it rises, beside a long reflecting pool.
        d = new Landmarks.Def("Twisted Tower", T_BUILDING) {
            int[][] lp(int L, int S) {
                int s = clampi(Math.round(S * 0.45f), 4, 8);
                int du = Math.max(1, Math.round(L * 0.42f - s / 2f));
                if (du + s > L - 3) du = Math.max(1, L - 3 - s);
                return new int[][]{{du, (S - s) / 2, s, s, 36}};
            }

            @Override
            int[][] plan(int bw, int bh, Random r) {
                int[][] q = lp(Math.max(bw, bh), Math.min(bw, bh));
                q[0][4] = 30 + r.nextInt(14);
                return tp(q, bh > bw);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                Ax a = new Ax(c, p, x0, y0, x1, y1);
                int[] q = lp(a.lt(), a.st())[0];
                float u0 = q[0] * U, v0 = q[1] * U, u1 = u0 + q[2] * U, v1 = v0 + q[3] * U, vm = (v0 + v1) / 2;
                rect(c, p, 0xFFD2CCBF, x0, y0, x1, y1);
                for (float x = x0 + 12; x < x1; x += 12) line(c, p, 0x18000000, 0.5f, x, y0, x, y1);
                for (float y = y0 + 12; y < y1; y += 12) line(c, p, 0x18000000, 0.5f, x0, y, x1, y);
                // A lawn with trees before the tower, crossed by the entrance walk.
                float lu1 = u0 - 1.1f * U;
                if (lu1 > 0.6f * U + 10) {
                    a.R(0xFF6A9A52, 0.6f * U, 0.6f * U, lu1, a.S - 0.6f * U);
                    a.R(0xFF74A45A, 0.6f * U + 2, 0.6f * U + 2, lu1 - 2, a.S - 0.6f * U - 2);
                    int k = 0;
                    for (float u = 0.6f * U + 9; u < lu1 - 6; u += 20, k++)
                        for (float v = 0.6f * U + 9 + (k % 2) * 10; v < a.S - 0.6f * U - 6; v += 20)
                            if (Math.abs(v - vm) > 12) a.tr(u, v, 5.5f, k % 2 == 0 ? 0xFF3E7A3A : 0xFF4A8640);
                }
                a.R(0xFFBEB6A6, 0, vm - 6, u0, vm + 6);
                a.lm(u0 - 10, vm - 8);
                a.lm(u0 - 10, vm + 8);
                // The podium round the tower's foot.
                a.R(0xFF9E9A92, u0 - 8, v0 - 8, u1 + 8, v1 + 8);
                a.R(0xFFB6B2AA, u0 - 6, v0 - 6, u1 + 6, v1 + 6);
                a.R(0xFF6E6C68, u0, v0, u1, v1);
                // The reflecting pool, with the tower's twist mirrored in it and a row of jets.
                float pu0 = u1 + 1.2f * U, pu1 = a.L - 0.9f * U, pv0 = 0.9f * U, pv1 = a.S - 0.9f * U;
                if (pu1 - pu0 > 10) {
                    a.R(0xFFB4AEA0, pu0 - 2.5f, pv0 - 2.5f, pu1 + 2.5f, pv1 + 2.5f);
                    a.R(0xFF1E485E, pu0, pv0, pu1, pv1);
                    a.R(0xFF2A5C78, pu0 + 1.5f, pv0 + 1.5f, pu1 - 1.5f, pv1 - 1.5f);
                    float rx = a.x((pu0 + pu1) / 2, (pv0 + pv1) / 2), ry = a.y((pu0 + pu1) / 2, (pv0 + pv1) / 2);
                    float rs = Math.min(pu1 - pu0, pv1 - pv0) * 0.62f;
                    for (int k = 0; k < 6; k++) {
                        float s = rs * (1 - k * 0.12f) / 2;
                        c.save();
                        c.translate(rx, ry);
                        c.rotate(-k * 8);
                        frame(c, p, 0x34FFFFFF, 0.6f, -s, -s, s, s);
                        c.restore();
                    }
                    for (float t = pv0 + 5; t < pv1 - 3; t += 8) {
                        a.C(0x60FFFFFF, pu0 + 3.5f, t, 1.4f);
                        a.C(0xC0FFFFFF, pu0 + 3.5f, t, 0.6f);
                    }
                    for (int k = 0; k < 5; k++) {
                        float uu = pu0 + 6 + r.nextFloat() * (pu1 - pu0 - 12), vv = pv0 + 4 + r.nextFloat() * (pv1 - pv0 - 8);
                        a.Ln(0x40FFFFFF, 0.5f, uu, vv, uu + 4, vv);
                    }
                    a.be(pu0 - 7, pv0 + (pv1 - pv0) * 0.3f, false);
                    a.be(pu0 - 7, pv0 + (pv1 - pv0) * 0.7f, false);
                }
                // Clipped hedges along the podium's long sides.
                for (float u = u0 - 4; u < u1 + 4; u += 7) {
                    if (v0 - 13 > 2) a.bu(u, v0 - 12, 2.5f);
                    if (v1 + 13 < a.S - 2) a.bu(u, v1 + 12, 2.5f);
                }
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2, w = Math.min(x1 - x0, y1 - y0);
                int n = 9;
                float hs = w / 2;
                for (int k = 0; k < n; k++) {
                    float ang = k * 8f;
                    double rad = Math.toRadians(ang);
                    hs = w * 0.98f * (1 - k * 0.075f) / (float) (Math.cos(rad) + Math.sin(rad)) / 2;
                    float ox = (float) (1.0 * Math.cos(rad) + 1.5 * Math.sin(rad)), oy = (float) (-1.0 * Math.sin(rad) + 1.5 * Math.cos(rad));
                    c.save();
                    c.translate(cx, cy);
                    c.rotate(ang);
                    roundRect(c, p, 0x55000000, -hs + ox, -hs + oy, hs + ox, hs + oy, hs * 0.22f);
                    roundRect(c, p, 0xFFE2E4E4, -hs, -hs, hs, hs, hs * 0.22f);
                    roundRect(c, p, mix(0xFF26506C, 0xFF7EAECA, k / (float) (n - 1)), -hs + 1.3f, -hs + 1.3f, hs - 1.3f, hs - 1.3f, hs * 0.2f);
                    line(c, p, 0x50FFFFFF, 0.6f, -hs + hs * 0.3f, -hs + 2.2f, hs * 0.4f, -hs + 2.2f);
                    c.restore();
                }
                // The crown: a small sky garden round a spire.
                circle(c, p, 0xFFE2E4E4, cx, cy, hs * 0.62f);
                circle(c, p, 0xFF5E8E52, cx, cy, hs * 0.5f);
                circle(c, p, 0xFF72A464, cx - hs * 0.12f, cy - hs * 0.12f, hs * 0.3f);
                circle(c, p, 0xFFB8BCC0, cx, cy, hs * 0.18f);
                circle(c, p, 0xFFF0F0F0, cx - 0.3f, cy - 0.3f, hs * 0.08f);
                circle(c, p, 0xFFE03020, cx, cy, 0.7f);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.MID;
        d.minS = 10;
        d.minL = 12;
        d.base = City.OFFICE;
        d.inside = Variants.I_OFFICE;
        d.roofCol = 0xFF4A7898;
        out.add(d);

        // 3. A slender TV tower with a round observation pod and antenna, a membrane-roofed pavilion at its foot.
        d = new Landmarks.Def("TV Tower", T_BUILDING) {
            int[][] lp(int L, int S) {
                int ts = S >= 13 ? 4 : 3;
                int du = Math.max(2, Math.round(L * 0.36f - ts / 2f)), dv = (S - ts) / 2;
                int pu = du + ts + 1, pw = L - 2 - pu, ph = Math.min(S - 4, 4), pv = (S - ph) / 2;
                if (pw >= 3) return new int[][]{{du, dv, ts, ts, 64}, {pu, pv, pw, ph, 2}};
                return new int[][]{{du, dv, ts, ts, 64}};
            }

            @Override
            int[][] plan(int bw, int bh, Random r) {
                return tp(lp(Math.max(bw, bh), Math.min(bw, bh)), bh > bw);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                Ax a = new Ax(c, p, x0, y0, x1, y1);
                int[][] q = lp(a.lt(), a.st());
                float ts = q[0][2] * U, tu = q[0][0] * U + ts / 2, tv = q[0][1] * U + ts / 2;
                rect(c, p, 0xFF64984E, x0, y0, x1, y1);
                rect(c, p, 0xFF6EA256, x0 + 3, y0 + 3, x1 - 3, y1 - 3);
                // Paths out to the streets.
                a.R(0xFFCFC8B8, 0, tv - 5, tu, tv + 5);
                a.R(0xFFCFC8B8, tu - 5, 0, tu + 5, a.S);
                // The pavilion's forecourt.
                if (q.length > 1) {
                    float pu0 = q[1][0] * U, pv0 = q[1][1] * U, pu1 = pu0 + q[1][2] * U, pv1 = pv0 + q[1][3] * U;
                    a.R(0xFFCFC8B8, tu, tv - 6, pu0, tv + 6);
                    a.R(0xFFC6BEAC, pu0 - 6, pv0 - 7, Math.min(a.L - 3, pu1 + 6), pv1 + 7);
                    for (float u = pu0; u <= pu1 + 0.1f; u += (pu1 - pu0) / 4) {
                        if (pv0 - 9 > 2) a.C(0xFF5A5A5C, u, pv0 - 4, 0.8f);
                        int fc = ((int) (u / 7)) % 3 == 0 ? 0xFFC8402E : ((int) (u / 7)) % 3 == 1 ? 0xFF2E6AB0 : 0xFFE8C040;
                        if (pv0 - 9 > 2) a.R(fc, u + 0.8f, pv0 - 5.5f, u + 5, pv0 - 2.5f);
                    }
                }
                // The round plaza at the tower's foot with radial paving.
                float rp = Math.min(a.S * 0.46f, ts * 1.25f);
                a.C(0xFFB8B0A0, tu, tv, rp + 2);
                a.C(0xFFD6D0C2, tu, tv, rp);
                a.C(0xFFC8C0B0, tu, tv, rp * 0.72f);
                a.C(0xFFD6D0C2, tu, tv, rp * 0.62f);
                for (int k = 0; k < 16; k++) {
                    double an = k * Math.PI / 8;
                    a.Ln(0x22000000, 0.5f, tu + (float) Math.cos(an) * rp * 0.62f, tv + (float) Math.sin(an) * rp * 0.62f,
                            tu + (float) Math.cos(an) * rp, tv + (float) Math.sin(an) * rp);
                }
                // Three concrete buttresses splay out from the shaft.
                for (int k = 0; k < 3; k++) {
                    double an = Math.PI / 2 + k * Math.PI * 2 / 3;
                    float dx = (float) Math.cos(an), dy = (float) Math.sin(an), l = ts * 0.78f;
                    a.P(0xFFA6A49E, tu - dy * 4, tv + dx * 4, tu + dx * l, tv + dy * l, tu + dy * 4, tv - dx * 4);
                    a.P(0x30000000, tu + dx * l, tv + dy * l, tu + dy * 4, tv - dx * 4, tu + dx * l * 0.5f + dy * 2, tv + dy * l * 0.5f - dx * 2);
                }
                // A ring of trees, broken by the paths.
                for (int k = 0; k < 12; k++) {
                    double an = k * Math.PI / 6 + Math.PI / 12;
                    float uu = tu + (float) Math.cos(an) * (rp + 9), vv = tv + (float) Math.sin(an) * (rp + 9);
                    if (uu < 7 || vv < 7 || vv > a.S - 7 || uu > a.L - 7) continue;
                    if (q.length > 1 && uu > q[1][0] * U - 8) continue;
                    a.tr(uu, vv, 5, k % 2 == 0 ? 0xFF3E7A3A : 0xFF4E8A44);
                }
                for (int k = 0; k < 4; k++) {
                    double an = Math.PI / 4 + k * Math.PI / 2;
                    a.be(tu + (float) Math.cos(an) * rp * 0.82f, tv + (float) Math.sin(an) * rp * 0.82f, k % 2 == 0);
                }
                a.lm(3, tv - 8);
                a.lm(3, tv + 8);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2, w = Math.min(x1 - x0, y1 - y0);
                if (part == 0) {
                    // Long shadow of the shaft, then the pod: window band, deck, antenna rings and beacon.
                    line(c, p, 0x2A000000, w * 0.2f, cx, cy, cx + w * 1.1f, cy + w * 1.8f);
                    circle(c, p, 0x40000000, cx + w * 0.22f, cy + w * 0.36f, w * 0.56f);
                    circle(c, p, 0xFF5A626C, cx, cy, w * 0.57f);
                    circle(c, p, 0xFF2C4454, cx, cy, w * 0.53f);
                    for (int k = 0; k < 28; k++) {
                        double an = k * Math.PI * 2 / 28;
                        float ca = (float) Math.cos(an), sa = (float) Math.sin(an);
                        line(c, p, 0xFF6E8696, 0.45f, cx + ca * w * 0.4f, cy + sa * w * 0.4f, cx + ca * w * 0.53f, cy + sa * w * 0.53f);
                    }
                    ovalRing(c, p, 0x50FFFFFF, 0.8f, cx - w * 0.5f, cy - w * 0.5f, cx + w * 0.44f, cy + w * 0.44f);
                    circle(c, p, 0xFFC8CCD0, cx, cy, w * 0.4f);
                    circle(c, p, 0xFFDADDE0, cx - w * 0.03f, cy - w * 0.03f, w * 0.33f);
                    circle(c, p, 0xFFA6ACB2, cx, cy, w * 0.24f);
                    for (int k = 0; k < 3; k++) {
                        double an = k * Math.PI * 2 / 3 + 0.4;
                        circle(c, p, 0xFFF4F4F4, cx + (float) Math.cos(an) * w * 0.31f, cy + (float) Math.sin(an) * w * 0.31f, w * 0.035f + 0.4f);
                    }
                    circle(c, p, 0xFFC03A30, cx, cy, w * 0.15f);
                    circle(c, p, 0xFFF2F2F0, cx, cy, w * 0.115f);
                    circle(c, p, 0xFFC03A30, cx, cy, w * 0.08f);
                    circle(c, p, 0xFFF2F2F0, cx, cy, w * 0.05f);
                    circle(c, p, 0xFFFF4030, cx, cy, w * 0.028f);
                } else {
                    // The pavilion: a white ribbed membrane roof with a glazed spine.
                    float w2 = x1 - x0, h2 = y1 - y0;
                    roundRect(c, p, 0xFFB8BCBE, x0, y0, x1, y1, 5);
                    roundRect(c, p, 0xFFEFF0EE, x0 + 1, y0 + 1, x1 - 1, y1 - 1, 4.5f);
                    Path path = new Path();
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(0.7f);
                    p.setColor(0xFFC4C8CA);
                    if (w2 >= h2) {
                        for (float t = x0 + 6; t < x1 - 4; t += 7) {
                            path.reset();
                            path.moveTo(t, y0 + 1.5f);
                            path.quadTo(t + 4, cy, t, y1 - 1.5f);
                            c.drawPath(path, p);
                        }
                        p.setStyle(Paint.Style.FILL);
                        rect(c, p, 0xFF9EC4D6, x0 + 3, cy - h2 * 0.08f, x1 - 3, cy + h2 * 0.08f);
                        line(c, p, 0x80FFFFFF, 0.5f, x0 + 3, cy - h2 * 0.04f, x1 - 3, cy - h2 * 0.04f);
                    } else {
                        for (float t = y0 + 6; t < y1 - 4; t += 7) {
                            path.reset();
                            path.moveTo(x0 + 1.5f, t);
                            path.quadTo(cx, t + 4, x1 - 1.5f, t);
                            c.drawPath(path, p);
                        }
                        p.setStyle(Paint.Style.FILL);
                        rect(c, p, 0xFF9EC4D6, cx - w2 * 0.08f, y0 + 3, cx + w2 * 0.08f, y1 - 3);
                        line(c, p, 0x80FFFFFF, 0.5f, cx - w2 * 0.04f, y0 + 3, cx - w2 * 0.04f, y1 - 3);
                    }
                    sign(city, "TV TOWER", cx, cy, 6, 0xFF404850);
                }
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.DOWN | Variants.MID | Variants.PARK;
        d.minS = 10;
        d.minL = 12;
        d.base = City.OFFICE;
        d.inside = Variants.I_OFFICE;
        d.roofCol = 0xFFC8CCD0;
        out.add(d);

        // 4. A science centre: a geodesic sphere, a hall under a honeycomb roof, and an outdoor science playground.
        d = new Landmarks.Def("Science Centre", T_BUILDING) {
            int[][] lp(int L, int S) {
                int sp = clampi(Math.round(S * 0.45f), 4, 7), sv = (S - sp) / 2;
                int hu = sp + 2, hw = L - 1 - hu, hh = Math.max(4, Math.round(S * 0.42f));
                return new int[][]{{1, sv, sp, sp, 6}, {hu, 1, hw, hh, 3}};
            }

            @Override
            int[][] plan(int bw, int bh, Random r) {
                return tp(lp(Math.max(bw, bh), Math.min(bw, bh)), bh > bw);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                Ax a = new Ax(c, p, x0, y0, x1, y1);
                int[][] q = lp(a.lt(), a.st());
                float sp = q[0][2] * U, su = q[0][0] * U + sp / 2, sv = q[0][1] * U + sp / 2;
                float hu = q[1][0] * U, hv1 = (q[1][1] + q[1][3]) * U;
                rect(c, p, 0xFFD3CFC5, x0, y0, x1, y1);
                for (float x = x0 + 10; x < x1; x += 10) line(c, p, 0x14000000, 0.5f, x, y0, x, y1);
                for (float y = y0 + 10; y < y1; y += 10) line(c, p, 0x14000000, 0.5f, x0, y, x1, y);
                // Lawns with trees either side of the sphere.
                float lu1 = hu - 0.6f * U;
                float[][] lawns = {{0.5f * U, 0.5f * U, lu1, sv - sp / 2 - 0.6f * U}, {0.5f * U, sv + sp / 2 + 0.6f * U, lu1, a.S - 0.5f * U}};
                for (float[] l : lawns) {
                    if (l[3] - l[1] < 10) continue;
                    a.RR(0xFF6A9C52, l[0], l[1], l[2], l[3], 4);
                    for (float u = l[0] + 7; u < l[2] - 4; u += 17) a.tr(u, (l[1] + l[3]) / 2, Math.min(6, (l[3] - l[1]) * 0.4f), 0xFF44803E);
                }
                // The forecourt ring round the sphere.
                a.C(0xFFA8A296, su, sv, sp * 0.5f + 9);
                a.C(0xFFC4BEB2, su, sv, sp * 0.5f + 7);
                for (int k = 0; k < 24; k++) {
                    double an = k * Math.PI / 12;
                    a.C(0xFF8E8A80, su + (float) Math.cos(an) * (sp * 0.5f + 5), sv + (float) Math.sin(an) * (sp * 0.5f + 5), 0.7f);
                }
                // The science playground: a grid of exhibits on coloured rubber.
                float pu0 = hu - 4, pu1 = a.L - 0.5f * U, pv0 = hv1 + 0.6f * U, pv1 = a.S - 0.5f * U;
                if (pv1 - pv0 > 16 && pu1 - pu0 > 16) {
                    a.RR(0xFF3A8478, pu0, pv0, pu1, pv1, 5);
                    a.RR(0xFF4C9C8E, pu0 + 2, pv0 + 2, pu1 - 2, pv1 - 2, 4);
                    float ph = pv1 - pv0 - 4, cs = Math.min(ph, 3.6f * U);
                    int cols = Math.max(1, (int) ((pu1 - pu0 - 4) / cs)), rows = Math.max(1, (int) (ph / cs));
                    float cw = (pu1 - pu0 - 4) / cols, chh = ph / rows;
                    int item = 0;
                    for (int j = 0; j < rows; j++)
                        for (int i = 0; i < cols; i++, item++) {
                            float cu = pu0 + 2 + cw * (i + 0.5f), cv = pv0 + 2 + chh * (j + 0.5f), s = Math.min(cw, chh) * 0.42f;
                            switch (item % 5) {
                                case 0: // a giant sundial
                                    a.C(0xFFD87E3A, cu, cv, s);
                                    a.C(0xFFE8E2D4, cu, cv, s * 0.88f);
                                    for (int k = 0; k < 12; k++) {
                                        double an = k * Math.PI / 6;
                                        a.Ln(0xFF6A6458, 0.5f, cu + (float) Math.cos(an) * s * 0.6f, cv + (float) Math.sin(an) * s * 0.6f,
                                                cu + (float) Math.cos(an) * s * 0.85f, cv + (float) Math.sin(an) * s * 0.85f);
                                    }
                                    a.P(0x50000000, cu, cv, cu + s * 0.7f, cv + s * 0.5f, cu + s * 0.1f, cv + 1.5f);
                                    a.P(0xFF4A4A4E, cu - 1, cv, cu + 1, cv, cu, cv - s * 0.8f);
                                    break;
                                case 1: // a rope-net climbing pyramid
                                    a.R(0xFFD87E3A, cu - s, cv - s, cu + s, cv + s);
                                    a.R(0xFFE8A050, cu - s + 1.5f, cv - s + 1.5f, cu + s - 1.5f, cv + s - 1.5f);
                                    a.Ln(0xFF3A3A3E, 0.6f, cu - s, cv - s, cu + s, cv + s);
                                    a.Ln(0xFF3A3A3E, 0.6f, cu - s, cv + s, cu + s, cv - s);
                                    for (float f = 0.33f; f < 0.9f; f += 0.33f) {
                                        float e = s * f;
                                        a.Ln(0xFF3A3A3E, 0.4f, cu - e, cv - e, cu + e, cv - e);
                                        a.Ln(0xFF3A3A3E, 0.4f, cu - e, cv + e, cu + e, cv + e);
                                        a.Ln(0xFF3A3A3E, 0.4f, cu - e, cv - e, cu - e, cv + e);
                                        a.Ln(0xFF3A3A3E, 0.4f, cu + e, cv - e, cu + e, cv + e);
                                    }
                                    a.C(0xFFC03A30, cu, cv, 1.2f);
                                    break;
                                case 2: { // the planets in a row
                                    float[] ps = {0.8f, 1.2f, 1.3f, 1f, 2.6f, 2.2f, 1.7f, 1.6f};
                                    int[] pc = {0xFFA09A90, 0xFFE0C080, 0xFF4A80C0, 0xFFC0603A, 0xFFD8B080, 0xFFE0D0A0, 0xFF90D0D8, 0xFF5070C8};
                                    a.C(0xFFF0C030, cu - s * 1.1f, cv, s * 0.45f);
                                    float uu = cu - s * 0.55f;
                                    for (int k = 0; k < 8; k++) {
                                        a.C(pc[k], uu, cv + (k % 2 == 0 ? -1 : 1) * s * 0.15f, ps[k] * Math.max(0.6f, s / 10));
                                        uu += s * 0.24f;
                                    }
                                    break;
                                }
                                case 3: // whisper dishes facing each other
                                    for (int k = -1; k <= 1; k += 2) {
                                        a.C(0x40000000, cu + k * s * 0.7f + 1, cv + 1.5f, s * 0.38f);
                                        a.C(0xFFB8BEC4, cu + k * s * 0.7f, cv, s * 0.38f);
                                        a.C(0xFFE4E8EC, cu + k * s * 0.7f, cv, s * 0.28f);
                                        a.C(0xFF8A9096, cu + k * s * 0.7f, cv, s * 0.08f);
                                    }
                                    a.Ln(0x40FFFFFF, 0.5f, cu - s * 0.3f, cv, cu + s * 0.3f, cv);
                                    break;
                                default: // water play
                                    a.C(0xFFE0DACC, cu, cv, s);
                                    a.C(0xFF4C98C8, cu, cv, s * 0.85f);
                                    a.C(0xFF70B4DC, cu, cv, s * 0.55f);
                                    for (int k = 0; k < 6; k++) {
                                        double an = k * Math.PI / 3;
                                        a.C(0xD0FFFFFF, cu + (float) Math.cos(an) * s * 0.4f, cv + (float) Math.sin(an) * s * 0.4f, 0.8f);
                                    }
                                    break;
                            }
                        }
                }
                // Bike racks by the hall door.
                for (int k = 0; k < 5; k++) a.Ln(0xFF606468, 0.7f, hu - 6, 0.6f * U + k * 3, hu - 2, 0.6f * U + k * 3);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
                if (part == 0) {
                    // The geodesic sphere: a triangulated dome, each facet shaded by its slope to the light.
                    float R = Math.min(x1 - x0, y1 - y0) * 0.5f;
                    circle(c, p, 0x50000000, cx + R * 0.14f, cy + R * 0.2f, R);
                    circle(c, p, 0xFF3A444E, cx, cy, R);
                    int n = 4;
                    float[] px = new float[3], py = new float[3];
                    for (int s = 0; s < 6; s++)
                        for (int i = 0; i < n; i++)
                            for (int j = 0; i + j < n; j++)
                                for (int up = 0; up < 2; up++) {
                                    if (up == 1 && i + j + 2 > n) continue;
                                    int[][] tri = up == 0 ? new int[][]{{i, j}, {i + 1, j}, {i, j + 1}} : new int[][]{{i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
                                    float mx = 0, my = 0;
                                    for (int k = 0; k < 3; k++) {
                                        int ri = tri[k][0] + tri[k][1];
                                        float t = ri == 0 ? 0 : tri[k][1] / (float) ri;
                                        double an = (s + t) * Math.PI / 3 - Math.PI / 2;
                                        float rad = R * 0.97f * (float) Math.sin(ri / (float) n * Math.PI / 2);
                                        px[k] = cx + (float) Math.cos(an) * rad;
                                        py[k] = cy + (float) Math.sin(an) * rad;
                                        mx += px[k] / 3;
                                        my += py[k] / 3;
                                    }
                                    float nx = (mx - cx) / R, ny = (my - cy) / R, nz = (float) Math.sqrt(Math.max(0, 1 - nx * nx - ny * ny));
                                    float b = -0.45f * nx - 0.55f * ny + 0.7f * nz;
                                    int col = mix(0xFF3E5868, 0xFFD8E8F0, b);
                                    if (r.nextInt(6) == 0) col = mix(col, 0xFFF4F2EE, 0.55f);
                                    float sh = 0.86f;
                                    poly(c, p, col, mx + (px[0] - mx) * sh, my + (py[0] - my) * sh, mx + (px[1] - mx) * sh, my + (py[1] - my) * sh,
                                            mx + (px[2] - mx) * sh, my + (py[2] - my) * sh);
                                }
                    circle(c, p, 0x30FFFFFF, cx - R * 0.35f, cy - R * 0.38f, R * 0.25f);
                } else {
                    // The hall: a honeycomb of frosted, clear and solar panels.
                    rect(c, p, 0xFF606A72, x0, y0, x1, y1);
                    rect(c, p, 0xFF98A4AA, x0 + 1.5f, y0 + 1.5f, x1 - 1.5f, y1 - 1.5f);
                    int sc = c.save();
                    c.clipRect(x0 + 2.5f, y0 + 2.5f, x1 - 2.5f, y1 - 2.5f);
                    float hr = 5.5f, dx = hr * 1.5f, dy = hr * 1.732f;
                    int col = 0;
                    for (float x = x0 + 2; x < x1 + hr; x += dx, col++)
                        for (float y = y0 + 2 + ((col & 1) == 1 ? dy / 2 : 0); y < y1 + hr; y += dy) {
                            int k = r.nextInt(10);
                            int cc = k < 5 ? 0xFFE4EAEC : k < 8 ? 0xFF8CC2CA : 0xFF2C4A66;
                            hex(c, p, cc, x, y, hr * 0.86f);
                            if (k < 5) hex(c, p, 0xFFF2F6F6, x - 0.6f, y - 0.6f, hr * 0.45f);
                        }
                    c.restoreToCount(sc);
                    sign(city, "SCIENCE CENTRE", cx, cy, 7, 0xFF20343C);
                }
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.UNI | Variants.MID | Variants.PARK;
        d.minS = 10;
        d.minL = 12;
        d.base = City.OFFICE;
        d.inside = Variants.I_LAB;
        d.roofCol = 0xFFA8B4BA;
        out.add(d);

        // 5. Old stone college ranges round a striped lawn, with a gatehouse bell tower.
        d = new Landmarks.Def("University Quad", T_BUILDING) {
            int[][] lp(int L, int S) {
                int tu = (L - 3) / 2;
                if (S >= 13)
                    return new int[][]{{1, 1, tu - 2, 3, 3}, {tu, 1, 3, 3, 10}, {tu + 4, 1, L - 5 - tu, 3, 3},
                            {1, S - 3, L - 2, 2, 3}, {1, 5, 2, S - 9, 3}, {L - 3, 5, 2, S - 9, 3}};
                return new int[][]{{1, 1, tu - 2, 3, 3}, {tu, 1, 3, 3, 10}, {tu + 4, 1, L - 5 - tu, 3, 3},
                        {1, 5, 2, S - 6, 3}, {L - 3, 5, 2, S - 6, 3}};
            }

            @Override
            int[][] plan(int bw, int bh, Random r) {
                return tp(lp(Math.max(bw, bh), Math.min(bw, bh)), bh > bw);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                Ax a = new Ax(c, p, x0, y0, x1, y1);
                int lt = a.lt(), st = a.st();
                boolean four = st >= 13;
                int tu = (lt - 3) / 2;
                rect(c, p, 0xFF63954A, x0, y0, x1, y1);
                // Flagstones round the ranges.
                a.R(0xFFC9BFA6, 0.55f * U, 0.55f * U, a.L - 0.55f * U, four ? a.S - 0.55f * U : a.S);
                for (float u = 0.55f * U + 6; u < a.L - 0.55f * U; u += 6) a.Ln(0x18000000, 0.5f, u, 0.55f * U, u, a.S - 0.55f * U);
                // The cloister walk and the lawn in mown stripes.
                float wu0 = 3 * U + 1, wu1 = a.L - 3 * U - 1, wv0 = 4 * U + 1, wv1 = four ? a.S - 3 * U - 1 : a.S;
                a.R(0xFFD8CEB2, wu0, wv0, wu1, wv1);
                float lu0 = wu0 + 5, lu1 = wu1 - 5, lv0 = wv0 + 5, lv1 = four ? wv1 - 5 : a.S - 7;
                for (int i = 0; ; i++) {
                    float s0 = lu0 + i * 7;
                    if (s0 >= lu1) break;
                    a.R(i % 2 == 0 ? 0xFF5E9A48 : 0xFF6AA654, s0, lv0, Math.min(lu1, s0 + 7), lv1);
                }
                float cu = (lu0 + lu1) / 2, cv = (lv0 + lv1) / 2;
                a.Ln(0xFFD8CEB2, 3, lu0, lv0, lu1, lv1);
                a.Ln(0xFFD8CEB2, 3, lu0, lv1, lu1, lv0);
                a.Ln(0xFFD8CEB2, 3, cu, lv0, cu, lv1);
                a.Ln(0xFFD8CEB2, 3, lu0, cv, lu1, cv);
                // Through the gatehouse to the street.
                a.R(0xFFD8CEB2, tu * U + 10, 0, tu * U + 38, wv0);
                // The founder's statue.
                a.C(0x40000000, cu + 1.5f, cv + 2, 7.5f);
                a.C(0xFFA89E86, cu, cv, 7.5f);
                a.C(0xFFCFC5AE, cu, cv, 6);
                a.R(0xFF9C9480, cu - 3, cv - 3, cu + 3, cv + 3);
                a.C(0xFF4E6250, cu - 0.4f, cv - 0.4f, 2.2f);
                a.C(0xFF7A9478, cu - 1, cv - 1, 1);
                // Old trees in the lawn's quarters, benches on the walk.
                if (lu1 - lu0 > 50 && lv1 - lv0 > 40) {
                    a.tr(lu0 + (cu - lu0) * 0.4f, lv0 + (cv - lv0) * 0.4f, 6, 0xFF3E7A3A);
                    a.tr(lu1 - (lu1 - cu) * 0.4f, lv1 - (lv1 - cv) * 0.4f, 6, 0xFF4A8040);
                }
                a.be(lu0 + 12, wv0 + 2.5f, true);
                a.be(lu1 - 12, wv0 + 2.5f, true);
                if (four) {
                    a.be(lu0 + 12, wv1 - 2.5f, true);
                    a.be(lu1 - 12, wv1 - 2.5f, true);
                } else {
                    // An open side: iron railings with a gate.
                    a.Ln(0xFF2E2E30, 0.8f, wu0, a.S - 2, cu - 6, a.S - 2);
                    a.Ln(0xFF2E2E30, 0.8f, cu + 6, a.S - 2, wu1, a.S - 2);
                    for (float u = wu0; u < wu1; u += 4) if (Math.abs(u - cu) > 6) a.C(0xFF2E2E30, u, a.S - 2, 0.6f);
                }
                a.lm(wu0 + 2, wv0 + 2);
                a.lm(wu1 - 2, wv0 + 2);
                // Trees in the margins outside.
                a.tr(0.5f * U, 0.5f * U, 7, 0xFF3E7A3A);
                a.tr(a.L - 0.5f * U, 0.5f * U, 7, 0xFF3E7A3A);
                if (four) {
                    a.tr(0.5f * U, a.S - 0.5f * U, 7, 0xFF487E3E);
                    a.tr(a.L - 0.5f * U, a.S - 0.5f * U, 7, 0xFF487E3E);
                }
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                float w = x1 - x0, h = y1 - y0, cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
                if (part == 1) {
                    // The bell tower: crenellated parapet, pinnacles and an octagonal lead lantern.
                    rect(c, p, 0xFFA89C82, x0, y0, x1, y1);
                    rect(c, p, 0xFF868C90, x0 + 3, y0 + 3, x1 - 3, y1 - 3);
                    for (float t = x0; t < x1 - 1; t += 4) {
                        rect(c, p, 0xFFCFC4A8, t, y0, t + 2.2f, y0 + 2.2f);
                        rect(c, p, 0xFFB8AC90, t, y1 - 2.2f, t + 2.2f, y1);
                    }
                    for (float t = y0; t < y1 - 1; t += 4) {
                        rect(c, p, 0xFFCFC4A8, x0, t, x0 + 2.2f, t + 2.2f);
                        rect(c, p, 0xFFB8AC90, x1 - 2.2f, t, x1, t + 2.2f);
                    }
                    float[][] cs = {{x0, y0}, {x1 - 5, y0}, {x0, y1 - 5}, {x1 - 5, y1 - 5}};
                    for (float[] q : cs) {
                        rect(c, p, 0xFFBDB196, q[0], q[1], q[0] + 5, q[1] + 5);
                        poly(c, p, 0xFFD8CEB4, q[0], q[1], q[0] + 5, q[1], q[0] + 2.5f, q[1] + 2.5f);
                        poly(c, p, 0xFF968A70, q[0], q[1] + 5, q[0] + 5, q[1] + 5, q[0] + 2.5f, q[1] + 2.5f);
                        circle(c, p, 0xFFE0D6BC, q[0] + 2.5f, q[1] + 2.5f, 0.8f);
                    }
                    float R = Math.min(w, h) * 0.24f;
                    for (int k = 0; k < 8; k++) {
                        double a0 = k * Math.PI / 4 + Math.PI / 8, a1 = a0 + Math.PI / 4, am = (a0 + a1) / 2;
                        float lit = (float) (0.5 + 0.5 * Math.cos(am - Math.toRadians(225)));
                        poly(c, p, mix(0xFF3E6A5C, 0xFF7EAE98, lit), cx, cy, cx + (float) Math.cos(a0) * R, cy + (float) Math.sin(a0) * R,
                                cx + (float) Math.cos(a1) * R, cy + (float) Math.sin(a1) * R);
                    }
                    circle(c, p, 0xFFE0C050, cx, cy, 1.4f);
                    line(c, p, 0xFF404040, 0.6f, x1 - 7, y0 + 7, x1 - 7, y0 + 1);
                    rect(c, p, 0xFF2A4A8A, x1 - 7, y0 + 1, x1 - 3, y0 + 3.5f);
                    sign(city, "UNIVERSITY", cx, cy, 6, 0xFF3A3020);
                } else {
                    // A stone-slated range with chimney stacks on the ridge and dormers on the quad side.
                    gable(c, p, x0, y0, x1, y1, 0xFF7E766A, 2.6f);
                    boolean wide = w >= h;
                    float len = wide ? w : h;
                    for (float t = 10; t < len - 6; t += 30) {
                        float sx = wide ? x0 + t : cx, sy = wide ? cy : y0 + t;
                        rect(c, p, 0x40000000, sx - 2.5f + 1, sy - 2 + 1.5f, sx + 2.5f + 1, sy + 2 + 1.5f);
                        rect(c, p, 0xFFB4A88E, sx - 2.5f, sy - 2, sx + 2.5f, sy + 2);
                        circle(c, p, 0xFFA05A3A, sx - 1.2f, sy, 0.9f);
                        circle(c, p, 0xFFA05A3A, sx + 1.2f, sy, 0.9f);
                    }
                    for (float t = 20; t < len - 8; t += 30) {
                        if (wide) gable(c, p, x0 + t - 3, y1 - 6, x0 + t + 3, y1 - 0.5f, 0xFF8A8274, 0);
                        else gable(c, p, x1 - 6, y0 + t - 3, x1 - 0.5f, y0 + t + 3, 0xFF8A8274, 0);
                    }
                }
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.UNI | Variants.OLD;
        d.minS = 10;
        d.minL = 12;
        d.base = City.OFFICE;
        d.inside = Variants.I_CLASSROOM;
        d.roofCol = 0xFF7E766A;
        out.add(d);

        // 6. A stone watermill on its race, a wheel turning beside it, the millpond above and a cobbled yard.
        d = new Landmarks.Def("Old Water Mill", T_BUILDING) {
            int[][] lp(int L, int S) {
                int mu = Math.round(L * 0.42f), mw = Math.max(4, Math.round(L * 0.28f)), mh = Math.max(3, Math.min(S - 6, 5));
                if (mu + mw > L - 1) mu = L - 1 - mw;
                return new int[][]{{mu, 3, mw, mh, 3}, {1, S - 4, 3, 3, 2}};
            }

            @Override
            int[][] plan(int bw, int bh, Random r) {
                return tp(lp(Math.max(bw, bh), Math.min(bw, bh)), bh > bw);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                Ax a = new Ax(c, p, x0, y0, x1, y1);
                int[][] q = lp(a.lt(), a.st());
                float mu0 = q[0][0] * U, mv0 = q[0][1] * U, mu1 = mu0 + q[0][2] * U, mv1 = mv0 + q[0][3] * U;
                float cu0 = q[1][0] * U, cv0 = q[1][1] * U, cu1 = cu0 + 3 * U, cv1 = cv0 + 3 * U;
                rect(c, p, 0xFF6C984A, x0, y0, x1, y1);
                for (int k = 0; k < 30; k++) {
                    float u = r.nextFloat() * a.L, v = r.nextFloat() * a.S;
                    a.C(0x20305020, u, v, 2 + r.nextFloat() * 3);
                }
                float rv0 = 0.9f * U, rv1 = 2.3f * U, rvm = (rv0 + rv1) / 2;
                float pu1 = Math.max(3 * U, mu0 - 1.0f * U), pv1 = Math.min(a.S * 0.55f, cv0 - 6);
                // The cobbled yard and the cart track to the street.
                float yu0 = mu0 - 0.8f * U, yv0 = rv1 + 3, yu1 = Math.min(a.L - 0.3f * U, mu1 + 0.9f * U), yv1 = Math.min(a.S, mv1 + 1.0f * U);
                a.R(0xFF9A8F80, yu0, yv0, yu1, yv1);
                a.R(0xFF9A8F80, (mu0 + mu1) / 2 - 8, yv1 - 2, (mu0 + mu1) / 2 + 8, a.S);
                for (float v = yv0 + 2.6f; v < yv1; v += 2.6f) a.Ln(0x22000000, 0.4f, yu0, v, yu1, v);
                for (int k = 0; k < 50; k++) {
                    float u = yu0 + r.nextFloat() * (yu1 - yu0 - 2), v = yv0 + r.nextFloat() * (yv1 - yv0 - 2);
                    a.R(r.nextBoolean() ? 0xFFAEA494 : 0xFF887E70, u, v, u + 1.8f, v + 1.5f);
                }
                a.Ln(0x30000000, 1.2f, (mu0 + mu1) / 2 - 4, yv1, (mu0 + mu1) / 2 - 4, a.S);
                a.Ln(0x30000000, 1.2f, (mu0 + mu1) / 2 + 4, yv1, (mu0 + mu1) / 2 + 4, a.S);
                // Garden path to the miller's cottage and its vegetable rows.
                a.R(0xFFB4A88E, cu1, cv0 + U, yu0 + 2, cv0 + U + 6);
                float gu0 = cu1 + 4, gu1 = Math.min(yu0 - 4, gu0 + 2.5f * U);
                if (gu1 - gu0 > 12) {
                    a.R(0xFF6E5236, gu0, cv0 + U + 9, gu1, a.S - 4);
                    for (float v = cv0 + U + 11; v < a.S - 5; v += 3.5f) a.Ln(0xFF5A9A40, 1.6f, gu0 + 1, v, gu1 - 1, v);
                }
                // The mill race: stone banks, dark water, foam below the wheel.
                a.R(0xFF8C8272, pu1 * 0.5f, rv0 - 1.8f, a.L, rv1 + 1.8f);
                a.R(0xFF2E5A74, pu1 * 0.5f, rv0, a.L, rv1);
                a.R(0xFF3C6E8A, pu1 * 0.5f, rv0 + 2, a.L, rv1 - 2);
                float wu = mu0 + 1.6f * U;
                for (int k = 0; k < 8; k++) {
                    float u = wu + 1.4f * U + k * 6 + r.nextFloat() * 3;
                    if (u > a.L - 3) break;
                    a.Ln(0x90FFFFFF, 0.6f, u, rv0 + 2 + r.nextFloat() * (rv1 - rv0 - 4), u + 3, rv0 + 2 + r.nextFloat() * (rv1 - rv0 - 4));
                }
                // The millpond with reeds and lily pads, and the sluice where the race leaves it.
                pond(c, p, a.x(0.4f * U, 0.4f * U), a.y(0.4f * U, 0.4f * U), a.x(pu1, pv1), a.y(pu1, pv1));
                for (int k = 0; k < 6; k++) {
                    float u = 0.4f * U + (pu1 - 0.4f * U) * (0.25f + r.nextFloat() * 0.5f), v = 0.4f * U + (pv1 - 0.4f * U) * (0.3f + r.nextFloat() * 0.45f);
                    a.C(0xFF4E8A3A, u, v, 1.6f);
                }
                for (int k = 0; k < 10; k++) {
                    double an = Math.PI * 0.6 + k * 0.17;
                    float u = (0.4f * U + pu1) / 2 + (float) Math.cos(an) * (pu1 - 0.4f * U) * 0.5f, v = (0.4f * U + pv1) / 2 + (float) Math.sin(an) * (pv1 - 0.4f * U) * 0.5f;
                    a.Ln(0xFF5A7A36, 0.6f, u, v, u - 1.5f, v - 3);
                    a.Ln(0xFF6E8A3E, 0.6f, u, v, u + 1, v - 3.2f);
                }
                float slu = pu1 + 2;
                a.R(0xFF5A4630, slu, rv0 - 2.5f, slu + 3, rv1 + 2.5f);
                a.R(0xFF7A6040, slu + 0.6f, rv0 - 2, slu + 2.4f, rv1 + 2);
                // The water wheel: rim, spokes and paddles, its axle into the mill wall.
                float wr = 1.3f * U, wv = (rv1 - rv0) / 2 + 3.5f;
                a.O(0x50000000, wu - wr + 1.5f, rvm - wv + 2.2f, wu + wr + 1.5f, rvm + wv + 2.2f);
                a.Ln(0xFF3A3A3A, 2.2f, wu, rvm, wu, mv0 + 1);
                a.O(0xFF3E2A1A, wu - wr, rvm - wv, wu + wr, rvm + wv);
                a.O(0xFF2E5A74, wu - wr + 2.4f, rvm - wv + 2.2f, wu + wr - 2.4f, rvm + wv - 2.2f);
                for (int k = 0; k < 8; k++) {
                    double an = k * Math.PI / 4 + 0.2;
                    a.Ln(0xFF6E5236, 1.1f, wu, rvm, wu + (float) Math.cos(an) * (wr - 1.5f), rvm + (float) Math.sin(an) * (wv - 1.5f));
                }
                for (int k = 0; k < 20; k++) {
                    double an = k * Math.PI / 10;
                    float ca = (float) Math.cos(an), sa = (float) Math.sin(an);
                    a.Ln(0xFF8A6A44, 1.3f, wu + ca * (wr - 2.6f), rvm + sa * (wv - 2.4f), wu + ca * wr, rvm + sa * wv);
                }
                a.C(0xFF2A2A2A, wu, rvm, 2.2f);
                a.C(0xFF6A6A6A, wu - 0.5f, rvm - 0.5f, 1);
                // A plank footbridge downstream.
                float fb = a.L - 1.3f * U;
                if (fb > wu + wr + 8) {
                    a.R(0x40000000, fb + 1, rv0 - 2.5f, fb + 9, rv1 + 3.5f);
                    a.R(0xFF8A6A44, fb, rv0 - 3, fb + 8, rv1 + 3);
                    for (float v = rv0 - 3 + 2; v < rv1 + 3; v += 2) a.Ln(0xFF5E4630, 0.4f, fb, v, fb + 8, v);
                }
                // Millstones leaning on the wall, sacks by the door, a cart.
                float su = mu1 + 4;
                if (su + 8 < a.L) {
                    a.C(0xFF8A8478, su + 2, mv1 - 6, 4);
                    a.C(0xFF6A6458, su + 2, mv1 - 6, 1.2f);
                    a.C(0xFF948E82, su + 3, mv1 - 15, 3.5f);
                    a.C(0xFF6A6458, su + 3, mv1 - 15, 1);
                }
                for (int k = 0; k < 4; k++) a.RR(0xFFD8C8A0, mu0 + 6 + k * 4.5f, mv1 + 2, mu0 + 9.5f + k * 4.5f, mv1 + 5.5f, 1.2f);
                a.R(0xFF7A5A3A, mu1 - 14, mv1 + 3, mu1 - 4, mv1 + 9);
                a.R(0xFF2A2A2A, mu1 - 12, mv1 + 2, mu1 - 10, mv1 + 10);
                a.Ln(0xFF5A4030, 0.8f, mu1 - 14, mv1 + 6, mu1 - 20, mv1 + 6);
                // Willows by the pond, oaks round the edge.
                a.tr(pu1 - 2, pv1 + 2, 7, 0xFF8AAA48);
                a.tr(0.6f * U, pv1 + 4, 6, 0xFF7E9E44);
                a.tr(a.L - 0.7f * U, a.S - 0.8f * U, 7, 0xFF3E7A3A);
                if (a.L - mu1 > 2.5f * U) a.tr(a.L - 0.8f * U, mv0 + 1.5f * U, 6.5f, 0xFF467E3C);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                float w = x1 - x0, h = y1 - y0, cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
                if (part == 0) {
                    gable(c, p, x0, y0, x1, y1, 0xFF505A64, 2.2f);
                    // The lucam (hoist loft) over the yard and a stone chimney.
                    if (w >= h) {
                        rect(c, p, 0x50000000, cx - 4 + 1, y1 - 9 + 1.5f, cx + 4 + 1, y1 + 2.5f);
                        gable(c, p, cx - 4, y1 - 9, cx + 4, y1 + 1, 0xFF58626C, 2);
                        rect(c, p, 0xFFA89E8C, x0 + 3, cy - 3, x0 + 8, cy + 3);
                        circle(c, p, 0xFF3A3A3A, x0 + 5.5f, cy, 1.4f);
                    } else {
                        rect(c, p, 0x50000000, x1 - 9 + 1, cy - 4 + 1.5f, x1 + 2, cy + 5.5f);
                        gable(c, p, x1 - 9, cy - 4, x1 + 1, cy + 4, 0xFF58626C, 2);
                        rect(c, p, 0xFFA89E8C, cx - 3, y0 + 3, cx + 3, y0 + 8);
                        circle(c, p, 0xFF3A3A3A, cx, y0 + 5.5f, 1.4f);
                    }
                    sign(city, "WATER MILL", cx, cy, 6, 0xFFE8E0D0);
                } else {
                    gable(c, p, x0, y0, x1, y1, 0xFF9A5A44, 1.8f);
                    float sx = x0 + w * 0.25f, sy = y0 + h * 0.5f;
                    if (h > w) {
                        sx = x0 + w * 0.5f;
                        sy = y0 + h * 0.25f;
                    }
                    rect(c, p, 0xFFB0A490, sx - 2.5f, sy - 2.5f, sx + 2.5f, sy + 2.5f);
                    circle(c, p, 0xFF3A3A3A, sx, sy, 1.2f);
                    circle(c, p, 0x50D8D8D8, sx + 3, sy - 3, 2.2f);
                    circle(c, p, 0x38D8D8D8, sx + 6, sy - 5, 2.8f);
                }
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.OLD | Variants.SUB | Variants.PARK;
        d.minS = 10;
        d.minL = 12;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_FACTORY;
        d.roofCol = 0xFF505A64;
        out.add(d);

        // 7. A railway museum: an engine shed with smoke vents, a turntable, and old engines and carriages on display.
        d = new Landmarks.Def("Railway Museum", T_BUILDING) {
            int[][] lp(int L, int S) {
                int sw = Math.min(S - 3, Math.max(4, Math.round(L * 0.35f)));
                return new int[][]{{1, 1, sw, S - 2, 3}};
            }

            @Override
            int[][] plan(int bw, int bh, Random r) {
                return tp(lp(Math.max(bw, bh), Math.min(bw, bh)), bh > bw);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                Ax a = new Ax(c, p, x0, y0, x1, y1);
                int[][] q = lp(a.lt(), a.st());
                float se = (1 + q[0][2]) * U;
                float deg = a.tall ? 90 : 0;
                rect(c, p, 0xFF8A8276, x0, y0, x1, y1);
                for (int k = 0; k < 60; k++) {
                    float u = r.nextFloat() * a.L, v = r.nextFloat() * a.S;
                    a.R(r.nextBoolean() ? 0x26000000 : 0x1EFFFFFF, u, v, u + 1.5f, v + 1.2f);
                }
                // Grass strips at the edges.
                a.R(0xFF6E9450, se, 0, a.L, 3);
                a.R(0xFF6E9450, se, a.S - 3, a.L, a.S);
                float tr = Math.min(Math.min(a.S * 0.27f, 2.7f * U), (a.L - se - 1.4f * U) / 2);
                float tcu = se + 0.9f * U + tr, tcv = a.S / 2;
                // Tracks from the shed doors to the turntable.
                float sp = Math.min(2.0f * U, (a.S - 3 * U) / 3);
                for (int k = -1; k <= 1; k++) {
                    float dv = tcv + k * sp, du = se + 0.3f * U;
                    double an = Math.atan2(dv - tcv, du - tcu);
                    float ru = tcu + (float) Math.cos(an) * tr, rv = tcv + (float) Math.sin(an) * tr;
                    a.track(se - 4, dv, du, dv);
                    a.track(du, dv, ru, rv);
                }
                // The exit road beyond the turntable.
                float ex = tcu + tr, ee = a.L - 0.3f * U;
                if (ee - ex > 1.2f * U) {
                    a.track(ex, tcv, ee, tcv);
                    a.R(0xFFB03A28, ee - 2, tcv - 5, ee, tcv + 5);
                }
                // The turntable pit, its ring rail, and the bridge with an engine on it.
                a.C(0xFF6A6460, tcu, tcv, tr + 2.5f);
                a.C(0xFF3E3A36, tcu, tcv, tr);
                a.C(0xFF4A4540, tcu, tcv, tr * 0.9f);
                ring(c, p, 0xFFA8A8AC, 0.8f, a.x(tcu, tcv), a.y(tcu, tcv), tr * 0.94f);
                float bx = a.x(tcu, tcv), by = a.y(tcu, tcv), bdeg = 24 + deg;
                c.save();
                c.translate(bx, by);
                c.rotate(bdeg);
                rect(c, p, 0x50000000, -tr + 1.5f, -5 + 2, tr + 1.5f, 5 + 2);
                rect(c, p, 0xFF4E4A44, -tr, -5.5f, tr, 5.5f);
                rect(c, p, 0xFF6A645C, -tr, -5.5f, tr, -4);
                rect(c, p, 0xFF6A645C, -tr, 4, tr, 5.5f);
                for (float t = -tr + 2; t < tr; t += 3.6f) line(c, p, 0xFF5C4836, 1.4f, t, -4, t, 4);
                line(c, p, 0xFFC4C4C8, 0.7f, -tr, -2.5f, tr, -2.5f);
                line(c, p, 0xFFC4C4C8, 0.7f, -tr, 2.5f, tr, 2.5f);
                c.restore();
                circle(c, p, 0xFF5A5650, bx, by, 2);
                loco(c, p, bx, by, bdeg, Math.min(tr * 1.8f, 52), 0xFF2E5A3A);
                // Sidings along both long edges: an engine and carriages on one, carriages on the other.
                float lv = 0.85f * U;
                int[] cc = {0xFF6E2A2A, 0xFF3A5A40, 0xFF6E2A2A};
                for (int s = 0; s < 2; s++) {
                    float v = s == 0 ? lv : a.S - lv;
                    if (Math.abs(v - tcv) < tr + 8) continue;
                    float u0 = se + 0.4f * U, u1 = a.L - 0.3f * U;
                    a.track(u0, v, u1, v);
                    a.R(0xFFB03A28, u1 - 2, v - 5, u1, v + 5);
                    float u = u0 + 3;
                    int k = 0;
                    if (s == 0 && u + 50 < u1 - 2) {
                        loco(c, p, a.x(u + 25, v), a.y(u + 25, v), deg + 180, 50, 0xFF1E1E22);
                        u += 52;
                    }
                    while (u + 44 < u1 - 2) {
                        carriage(c, p, a.x(u + 22, v), a.y(u + 22, v), deg, 44, cc[(k + s) % 3]);
                        u += 46;
                        k++;
                    }
                }
                // The water crane by the shed, a coal stage and a signal.
                float wcu = se + 0.5f * U, wcv = tcv + sp * 0.5f;
                a.C(0x40000000, wcu + 1, wcv + 1.5f, 2.2f);
                a.C(0xFF2E3A44, wcu, wcv, 2);
                a.Ln(0xFF2E3A44, 1.2f, wcu, wcv, wcu + 7, wcv - 4);
                a.C(0xFF2E3A44, wcu + 7, wcv - 4, 1);
                if (ee - ex > 2.5f * U) {
                    float gu = ex + 1.2f * U;
                    a.R(0xFF5A5450, gu, tcv + 8, gu + 16, tcv + 18);
                    a.O(0xFF1C1C1C, gu + 1, tcv + 9, gu + 15, tcv + 17);
                    a.O(0xFF333333, gu + 4, tcv + 10.5f, gu + 9, tcv + 14);
                }
                a.C(0xFF404040, tcu + tr + 6, tcv - 9, 1.2f);
                a.C(0xFFC8302A, tcu + tr + 6, tcv - 12, 1.6f);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                // The engine shed: pitched spans along the tracks, glazing strips, and a smoke vent on each ridge.
                Ax ra = new Ax(c, p, x0, y0, x1, y1);
                rect(c, p, 0xFF3A3430, x0, y0, x1, y1);
                int n = Math.max(2, Math.round(ra.L / 44));
                float span = ra.L / n;
                for (int i = 0; i < n; i++) {
                    float s0 = i * span, s1 = s0 + span, sm = (s0 + s1) / 2;
                    ra.R(0xFF7A6E62, s0 + 0.8f, 0.8f, sm, ra.S - 0.8f);
                    ra.R(0xFF5C5248, sm, 0.8f, s1 - 0.8f, ra.S - 0.8f);
                    ra.R(0xFF9AB2BE, s0 + span * 0.12f, 2, s0 + span * 0.26f, ra.S - 2);
                    ra.R(0xFF7A929E, sm + span * 0.24f, 2, sm + span * 0.38f, ra.S - 2);
                    for (float v = 6; v < ra.S - 2; v += 6) {
                        ra.Ln(0xFF5A6A72, 0.4f, s0 + span * 0.12f, v, s0 + span * 0.26f, v);
                        ra.Ln(0xFF4A5A62, 0.4f, sm + span * 0.24f, v, sm + span * 0.38f, v);
                    }
                    for (int k = 0; k < 3; k++) {
                        float v = ra.S * (0.2f + k * 0.3f);
                        ra.O(0x38000000, sm - 7, v - 6, sm + 7, v + 6);
                    }
                    ra.R(0x50000000, sm - 2.2f + 1, 3 + 1.4f, sm + 2.2f + 1, ra.S - 3 + 1.4f);
                    ra.R(0xFF2C2824, sm - 2.4f, 3, sm + 2.4f, ra.S - 3);
                    for (float v = 4.5f; v < ra.S - 3.5f; v += 2.6f) ra.Ln(0xFF56504A, 0.5f, sm - 2.4f, v, sm + 2.4f, v);
                    if (i > 0) ra.Ln(0xFF262220, 1.2f, s0, 0.8f, s0, ra.S - 0.8f);
                }
                sign(city, "RAILWAY MUSEUM", (x0 + x1) / 2, (y0 + y1) / 2, 7, 0xFFE8DCC0);
            }
        };
        d.ground = City.LOT;
        d.districts = Variants.IND | Variants.OLD | Variants.MID;
        d.minS = 10;
        d.minL = 13;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_SHOWROOM;
        d.roofCol = 0xFF6A5E54;
        out.add(d);

        // 8. A five-tiered pagoda with upturned eaves in red and gold, a walled temple courtyard, gate and lanterns.
        d = new Landmarks.Def("Pagoda Temple", T_BUILDING) {
            int[][] lp(int L, int S) {
                int ps = clampi(Math.round(S * 0.44f), 4, 7), pv = (S - ps) / 2;
                int pu = Math.round(L * 0.55f - ps / 2f);
                int hu = pu + ps + 1, hw = L - 1 - hu;
                if (hw >= 4) return new int[][]{{pu, pv, ps, ps, 12}, {hu, 2, hw, S - 4, 3}};
                return new int[][]{{L - ps - 2, pv, ps, ps, 12}};
            }

            @Override
            int[][] plan(int bw, int bh, Random r) {
                return tp(lp(Math.max(bw, bh), Math.min(bw, bh)), bh > bw);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                Ax a = new Ax(c, p, x0, y0, x1, y1);
                int[][] q = lp(a.lt(), a.st());
                float pu0 = q[0][0] * U, pv0 = q[0][1] * U, pu1 = pu0 + q[0][2] * U, pv1 = pv0 + q[0][3] * U;
                float cv = a.S / 2;
                rect(c, p, 0xFFD9D1BE, x0, y0, x1, y1);
                // Raked gravel either side of the walk.
                for (float v = 6; v < a.S - 5; v += 3.5f) {
                    if (Math.abs(v - cv) < 10) continue;
                    a.Ln(0x1C000000, 0.5f, 6, v, pu0 - 6, v);
                }
                // The stone platform round the pagoda and the processional walk.
                a.R(0xFF9E968A, pu0 - 7, pv0 - 7, pu1 + 7, pv1 + 7);
                a.R(0xFFB8AE9C, pu0 - 5, pv0 - 5, pu1 + 5, pv1 + 5);
                a.R(0xFFA8A090, 0, cv - 7, pu0 - 5, cv + 7);
                for (float u = 4; u < pu0 - 5; u += 6) a.Ln(0x22000000, 0.5f, u, cv - 7, u, cv + 7);
                if (q.length > 1) a.R(0xFFA8A090, pu1 + 5, cv - 6, q[1][0] * U, cv + 6);
                // A koi pond and cherry trees in the front corners.
                float fu1 = pu0 - 10;
                if (fu1 > 40 && cv - 10 > 30) {
                    pond(c, p, a.x(8, 6), a.y(8, 6), a.x(Math.min(fu1, 8 + 34), cv - 14), a.y(Math.min(fu1, 8 + 34), cv - 14));
                    a.C(0xFFE07030, 20, (cv - 8) / 2, 1.2f);
                    a.C(0xFFF0F0E0, 26, (cv - 8) / 2 + 3, 1.1f);
                }
                a.tr(10, a.S - 10, 7, 0xFFE4A0B4);
                if (fu1 > 50) a.tr(fu1 - 6, a.S - 12, 6.5f, 0xFFE8B0C0);
                a.tr(pu1 + 4, 8, 6, 0xFF2E5A3A);
                a.tr(pu1 + 4, a.S - 8, 6, 0xFF2E5A3A);
                // Stone lanterns down the walk, red paper lanterns strung beside it.
                for (float u = 2.4f * U; u < pu0 - 0.8f * U; u += 1.3f * U) {
                    for (int s = -1; s <= 1; s += 2) {
                        float v = cv + s * 12;
                        a.R(0x40000000, u - 3 + 1, v - 3 + 1.5f, u + 3 + 1, v + 3 + 1.5f);
                        a.R(0xFF8E8A82, u - 3, v - 3, u + 3, v + 3);
                        a.R(0xFFC2BEB4, u - 2, v - 2, u + 2, v + 2);
                        a.C(0xFFF0D070, u, v, 0.9f);
                    }
                }
                for (int s = -1; s <= 1; s += 2) {
                    float v = cv + s * 19;
                    if (v < 4 || v > a.S - 4) continue;
                    a.Ln(0xFF3A3A3A, 0.3f, 1.4f * U, v, pu0 - 8, v);
                    for (float u = 1.4f * U + 4; u < pu0 - 9; u += 7) {
                        a.C(0xFFD0382A, u, v, 1.8f);
                        a.C(0xFFF0B040, u - 0.5f, v - 0.5f, 0.6f);
                    }
                }
                // The incense burner before the pagoda.
                float iu = pu0 - 12;
                a.C(0x40000000, iu + 1, cv + 1.5f, 4.5f);
                a.C(0xFF6A5A3A, iu, cv, 4.5f);
                a.C(0xFFB0A898, iu, cv, 3);
                a.C(0x40FFFFFF, iu + 2, cv - 3, 2.5f);
                a.C(0x30FFFFFF, iu + 4, cv - 6, 3);
                // The red temple wall with its tiled cap, broken by the gate.
                int wc = 0xFF9C3A2C, cap = 0xFF4A3A36;
                a.R(wc, 0, 0, a.L, 4);
                a.R(wc, 0, a.S - 4, a.L, a.S);
                a.R(wc, a.L - 4, 0, a.L, a.S);
                a.R(wc, 0, 0, 4, cv - 10);
                a.R(wc, 0, cv + 10, 4, a.S);
                a.Ln(cap, 1.4f, 2, 2, a.L - 2, 2);
                a.Ln(cap, 1.4f, 2, a.S - 2, a.L - 2, a.S - 2);
                a.Ln(cap, 1.4f, a.L - 2, 2, a.L - 2, a.S - 2);
                a.Ln(cap, 1.4f, 2, 2, 2, cv - 10);
                a.Ln(cap, 1.4f, 2, cv + 10, 2, a.S - 2);
                // The gate: two red posts and a tiled beam with upturned gold ends.
                float gu = 1.1f * U;
                a.R(0x40000000, gu - 3 + 1.5f, cv - 18 + 2, gu + 3 + 1.5f, cv + 18 + 2);
                a.C(0xFFB03A28, gu, cv - 12, 2.4f);
                a.C(0xFFB03A28, gu, cv + 12, 2.4f);
                a.R(0xFFB03A28, gu - 1.5f, cv - 15, gu + 1.5f, cv + 15);
                a.R(0xFF3A3432, gu - 3, cv - 17, gu + 3, cv + 17);
                a.Ln(0xFF5A504C, 0.8f, gu, cv - 17, gu, cv + 17);
                a.C(0xFFE0B040, gu, cv - 18, 1.4f);
                a.C(0xFFE0B040, gu, cv + 18, 1.4f);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2, w = Math.min(x1 - x0, y1 - y0);
                if (part == 0) {
                    int n = 5;
                    float h0 = w / 2 * 0.9f, hk = h0;
                    Path path = new Path();
                    for (int k = 0; k < n; k++) {
                        hk = h0 * (1 - k * 0.17f);
                        float e = hk * 0.17f, t = hk + e, in = hk * 0.84f;
                        // Shadow of the tier on the one below.
                        rect(c, p, 0x50000000, cx - hk + 1.2f, cy - hk + 1.8f, cx + hk + 1.2f, cy + hk + 1.8f);
                        int[] fc = {0xFFA23C2C, 0xFF8E3428, 0xFF6E2820, 0xFF5E221C};
                        float[][] f = {{-t, -t, 0, -in, t, -t}, {-t, -t, -in, 0, -t, t}, {t, -t, in, 0, t, t}, {-t, t, 0, in, t, t}};
                        int[] order = {0, 1, 2, 3};
                        for (int o : order) {
                            path.reset();
                            path.moveTo(cx + f[o][0], cy + f[o][1]);
                            path.quadTo(cx + f[o][2], cy + f[o][3], cx + f[o][4], cy + f[o][5]);
                            path.lineTo(cx, cy);
                            path.close();
                            p.setStyle(Paint.Style.FILL);
                            p.setColor(fc[o == 0 ? 0 : o == 1 ? 1 : o == 2 ? 2 : 3]);
                            c.drawPath(path, p);
                        }
                        // Gold eaves trim, hip ridges and bells at the upturned corners.
                        path.reset();
                        path.moveTo(cx - t, cy - t);
                        path.quadTo(cx, cy - in, cx + t, cy - t);
                        path.quadTo(cx + in, cy, cx + t, cy + t);
                        path.quadTo(cx, cy + in, cx - t, cy + t);
                        path.quadTo(cx - in, cy, cx - t, cy - t);
                        p.setStyle(Paint.Style.STROKE);
                        p.setStrokeWidth(Math.max(0.6f, hk * 0.05f));
                        p.setColor(0xFFD8B048);
                        c.drawPath(path, p);
                        p.setStyle(Paint.Style.FILL);
                        for (int s = 0; s < 4; s++) {
                            float sx = (s & 1) == 0 ? -1 : 1, sy = (s & 2) == 0 ? -1 : 1;
                            line(c, p, 0xFF4A1A14, Math.max(0.6f, hk * 0.06f), cx + sx * t, cy + sy * t, cx + sx * t * 0.45f, cy + sy * t * 0.45f);
                            circle(c, p, 0xFFE8C050, cx + sx * t, cy + sy * t, Math.max(0.7f, hk * 0.06f));
                        }
                    }
                    // The finial: stacked gold rings and a jewel.
                    for (int k = 0; k < 4; k++) {
                        circle(c, p, k % 2 == 0 ? 0xFFC89A30 : 0xFFE8C860, cx - k * 0.3f, cy - k * 0.3f, hk * (0.36f - k * 0.07f));
                    }
                    circle(c, p, 0xFFFFF0B0, cx - 1.2f, cy - 1.2f, Math.max(0.6f, hk * 0.06f));
                } else {
                    // The main hall: green glazed tiles, red eaves, gold ridge ornaments.
                    hip(c, p, x0, y0, x1, y1, 0xFF56645A);
                    frame(c, p, 0xFFA03828, 1.4f, x0 + 0.7f, y0 + 0.7f, x1 - 0.7f, y1 - 0.7f);
                    float ww = x1 - x0, hh = y1 - y0;
                    if (ww >= hh) {
                        line(c, p, 0xFFD8B048, 1.4f, x0 + hh / 2, cy, x1 - hh / 2, cy);
                        circle(c, p, 0xFFE8C050, x0 + hh / 2, cy, 1.8f);
                        circle(c, p, 0xFFE8C050, x1 - hh / 2, cy, 1.8f);
                    } else {
                        line(c, p, 0xFFD8B048, 1.4f, cx, y0 + ww / 2, cx, y1 - ww / 2);
                        circle(c, p, 0xFFE8C050, cx, y0 + ww / 2, 1.8f);
                        circle(c, p, 0xFFE8C050, cx, y1 - ww / 2, 1.8f);
                    }
                    sign(city, "TEMPLE", cx, cy, 6, 0xFFF0D890);
                }
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.OLD | Variants.MID | Variants.PARK;
        d.minS = 10;
        d.minL = 12;
        d.base = City.OFFICE;
        d.inside = Variants.I_PEWS;
        d.roofCol = 0xFF8E3428;
        out.add(d);

        // 9. A perimeter block of flats round a green courtyard with a playground; every wing's roof is different.
        d = new Landmarks.Def("Courtyard Apartments", T_BUILDING) {
            int[][] lp(int L, int S) {
                if (S >= 13)
                    return new int[][]{{1, 1, L - 2, 3, 6}, {1, S - 4, L - 2, 3, 5}, {1, 5, 3, S - 10, 7}, {L - 4, 5, 3, S - 10, 5}};
                return new int[][]{{1, 1, L - 2, 3, 6}, {1, 5, 3, S - 6, 5}, {L - 4, 5, 3, S - 6, 7}};
            }

            @Override
            int[][] plan(int bw, int bh, Random r) {
                return tp(lp(Math.max(bw, bh), Math.min(bw, bh)), bh > bw);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                Ax a = new Ax(c, p, x0, y0, x1, y1);
                int lt = a.lt(), st = a.st();
                boolean four = st >= 13;
                rect(c, p, 0xFFC2BEB6, x0, y0, x1, y1);
                for (float x = x0 + 8; x < x1; x += 8) line(c, p, 0x16000000, 0.5f, x, y0, x, y1);
                for (float y = y0 + 8; y < y1; y += 8) line(c, p, 0x16000000, 0.5f, x0, y, x1, y);
                // Front gardens along the street.
                a.R(0xFF5E8E48, 3, 2, a.L - 3, 0.75f * U);
                for (float u = 8; u < a.L - 6; u += 14) a.bu(u, 0.45f * U, 3);
                if (four) {
                    a.R(0xFF5E8E48, 3, a.S - 0.75f * U, a.L - 3, a.S - 2);
                    for (float u = 15; u < a.L - 6; u += 14) a.bu(u, a.S - 0.45f * U, 3);
                }
                // The courtyard: lawn, a loop path, trees and benches on one side, the playground on the other.
                float cu0 = 4 * U + 2, cu1 = (lt - 4) * U - 2, cv0 = 4 * U + 2, cv1 = four ? (st - 4) * U - 2 : a.S - 3;
                a.R(0xFF6A9C50, cu0, cv0, cu1, cv1);
                float pu0 = cu0 + 5, pv0 = cv0 + 5, pu1 = cu1 - 5, pv1 = cv1 - 5;
                frame(c, p, 0xFFD2C8B0, 3.2f, Math.min(a.x(pu0, pv0), a.x(pu1, pv1)), Math.min(a.y(pu0, pv0), a.y(pu1, pv1)),
                        Math.max(a.x(pu0, pv0), a.x(pu1, pv1)), Math.max(a.y(pu0, pv0), a.y(pu1, pv1)));
                float cw = cu1 - cu0, split = cu0 + cw * 0.45f;
                if (cw > 100) split = cu0 + cw * 0.5f;
                // Playground.
                float gu0 = split + 4, gu1 = pu1 - 4, gv0 = pv0 + 4, gv1 = pv1 - 4;
                if (gu1 - gu0 > 14 && gv1 - gv0 > 14) {
                    a.R(0xFFC87A50, gu0, gv0, gu1, gv1);
                    a.R(0xFFD48A5E, gu0 + 1, gv0 + 1, gu1 - 1, gv1 - 1);
                    float gw = gu1 - gu0, gh = gv1 - gv0;
                    // Sandpit.
                    a.R(0xFF8A6E4E, gu0 + 2, gv0 + 2, gu0 + gw * 0.45f, gv0 + gh * 0.45f);
                    a.R(0xFFE6D29C, gu0 + 3, gv0 + 3, gu0 + gw * 0.45f - 1, gv0 + gh * 0.45f - 1);
                    a.C(0xFFD03A30, gu0 + gw * 0.25f, gv0 + gh * 0.25f, 1.2f);
                    // Slide.
                    float su = gu0 + gw * 0.62f, sv = gv0 + gh * 0.12f;
                    a.R(0x40000000, su + 1, sv + 1.5f, su + 6, sv + gh * 0.4f + 1.5f);
                    a.R(0xFF3A7AC0, su, sv, su + 5, sv + gh * 0.12f);
                    a.R(0xFFE8C040, su + 1, sv + gh * 0.12f, su + 4, sv + gh * 0.4f);
                    // Swings.
                    float wv = gv0 + gh * 0.72f;
                    a.Ln(0xFF4A4A50, 1.1f, gu0 + 3, wv, gu0 + gw * 0.55f, wv);
                    a.C(0xFF4A4A50, gu0 + 3, wv, 1.2f);
                    a.C(0xFF4A4A50, gu0 + gw * 0.55f, wv, 1.2f);
                    a.R(0xFFC03A30, gu0 + gw * 0.18f, wv + 1.5f, gu0 + gw * 0.18f + 3.5f, wv + 3);
                    a.R(0xFF30A050, gu0 + gw * 0.38f, wv + 1.5f, gu0 + gw * 0.38f + 3.5f, wv + 3);
                    // Climbing dome.
                    float du = gu0 + gw * 0.8f, dv = gv0 + gh * 0.72f, dr = Math.min(gw, gh) * 0.16f;
                    ring(c, p, 0xFF3A3A40, 0.7f, a.x(du, dv), a.y(du, dv), dr);
                    ring(c, p, 0xFF3A3A40, 0.6f, a.x(du, dv), a.y(du, dv), dr * 0.55f);
                    a.Ln(0xFF3A3A40, 0.5f, du - dr, dv, du + dr, dv);
                    a.Ln(0xFF3A3A40, 0.5f, du, dv - dr, du, dv + dr);
                }
                // Trees, benches and a picnic table.
                float tu = (pu0 + split) / 2;
                if (split - pu0 > 14) {
                    a.tr(tu, pv0 + (pv1 - pv0) * 0.3f, Math.min(7, (split - pu0) * 0.35f), 0xFF3E7A3A);
                    if (pv1 - pv0 > 40) a.tr(tu, pv0 + (pv1 - pv0) * 0.75f, Math.min(6, (split - pu0) * 0.3f), 0xFF4A8640);
                    a.be(tu, pv1 + 0.5f, true);
                    a.be(tu, pv0 - 0.5f, true);
                }
                a.R(0xFF8A6A44, split - 3, (pv0 + pv1) / 2 - 4, split + 3, (pv0 + pv1) / 2 + 4);
                // Bike racks in the passages.
                for (int k = 0; k < 4; k++) a.Ln(0xFF5A5E62, 0.7f, 1.2f * U + k * 3, 4 * U + 3, 1.2f * U + k * 3, 4 * U + 8);
                a.lm(cu0 + 2, cv0 + 2);
                a.lm(cu1 - 2, cv0 + 2);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                Ax ra = new Ax(c, p, x0, y0, x1, y1);
                float L = ra.L, S = ra.S;
                rect(c, p, 0xFF847E76, x0, y0, x1, y1);
                switch (part % 4) {
                    case 0: { // a roof garden
                        ra.R(0xFF7A9C5A, 1.3f, 1.3f, L - 1.3f, S - 1.3f);
                        for (int k = 0; k < (int) (L / 6); k++) {
                            float u = 3 + r.nextFloat() * (L - 6), v = 3 + r.nextFloat() * (S - 6);
                            ra.C(r.nextBoolean() ? 0xFF8EAE60 : 0xFF9A9A50, u, v, 1.5f + r.nextFloat() * 2);
                        }
                        ra.R(0xFFB08A60, 1.3f, S / 2 - 2.5f, L - 1.3f, S / 2 + 2.5f);
                        for (float u = 3; u < L - 1.5f; u += 3.2f) ra.Ln(0xFF8A6A44, 0.4f, u, S / 2 - 2.5f, u, S / 2 + 2.5f);
                        for (float u = 10; u < L - 8; u += 22) {
                            ra.R(0xFF8E8070, u - 4, 2.5f, u + 4, 7.5f);
                            ra.bu(u - 1.5f, 5, 2.2f);
                            ra.bu(u + 1.8f, 5, 2);
                            ra.tr(u + 11, S - 6, 3.6f, 0xFF4E8A44);
                        }
                        break;
                    }
                    case 1: { // solar panels
                        ra.R(0xFFB4B4B0, 1.3f, 1.3f, L - 1.3f, S - 1.3f);
                        for (float v = 3; v + 4.5f < S - 2; v += 7) {
                            for (float u = 4; u + 14 < L - 3; u += 17) {
                                ra.R(0x40000000, u + 0.8f, v + 4.4f, u + 14.8f, v + 5.6f);
                                ra.R(0xFF243A56, u, v, u + 14, v + 4.4f);
                                ra.Ln(0xFF7E9AB8, 0.5f, u, v + 0.25f, u + 14, v + 0.25f);
                                for (float t = u + 3.5f; t < u + 14; t += 3.5f) ra.Ln(0xFF3A5272, 0.35f, t, v, t, v + 4.4f);
                            }
                        }
                        break;
                    }
                    case 2: { // private terraces with parasols
                        ra.R(0xFFB08660, 1.3f, 1.3f, L - 1.3f, S - 1.3f);
                        for (float v = 3.5f; v < S - 1.5f; v += 2.4f) ra.Ln(0x1E000000, 0.4f, 1.3f, v, L - 1.3f, v);
                        int[] pc = {0xFFE8E0D0, 0xFFC84A3A, 0xFF3A7AA0, 0xFFE0B040};
                        int k = 0;
                        for (float u = 1.3f; u < L - 4; u += 24, k++) {
                            if (u > 2) ra.Ln(0xFF6E6A64, 1, u, 1.3f, u, S - 1.3f);
                            float pu = u + 12, pv = S * 0.45f;
                            ra.R(0xFF7E7266, u + 3, S - 6, u + 10, S - 2.5f);
                            ra.bu(u + 6.5f, S - 4.3f, 1.8f);
                            ra.C(0x40000000, pu + 1, pv + 1.5f, 5);
                            ra.C(pc[k % 4], pu, pv, 5);
                            ra.C(City.lighten(pc[k % 4], 0.25f), pu - 1, pv - 1, 2.5f);
                            ra.C(0xFF5A5A5A, pu, pv, 0.6f);
                        }
                        ra.Ln(0x90C8E0E8, 1, 1.3f, S - 1.5f, L - 1.3f, S - 1.5f);
                        break;
                    }
                    default: { // gravel with skylights and plant
                        ra.R(0xFFBDBAB2, 1.3f, 1.3f, L - 1.3f, S - 1.3f);
                        for (int k = 0; k < (int) (L / 3); k++) {
                            float u = 2 + r.nextFloat() * (L - 4), v = 2 + r.nextFloat() * (S - 4);
                            ra.R(0x20000000, u, v, u + 0.8f, v + 0.8f);
                        }
                        for (float u = 6; u + 8 < L - 4; u += 16) {
                            ra.R(0xFF7E848A, u - 0.8f, S / 2 - 4.8f, u + 8.8f, S / 2 + 4.8f);
                            ra.R(0xFF8DB4C8, u, S / 2 - 4, u + 8, S / 2 + 4);
                            ra.Ln(0x70FFFFFF, 0.6f, u + 1, S / 2 - 3, u + 4, S / 2 - 3);
                        }
                        ra.R(0xFF9A9EA2, L - 12, 3, L - 4, 9);
                        ra.C(0xFF4A4E52, L - 8, 6, 2);
                        break;
                    }
                }
                // A stair and lift core on every roof.
                float cu = L * 0.5f + (part % 2 == 0 ? -L * 0.18f : L * 0.18f);
                ra.R(0x50000000, cu - 4 + 1, 2 + 1.5f, cu + 4 + 1, 9 + 1.5f);
                ra.R(0xFF9C968E, cu - 4, 2, cu + 4, 9);
                ra.R(0xFFB4AEA6, cu - 3, 3, cu + 3, 8);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.MID | Variants.SUB | Variants.OLD;
        d.minS = 10;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOME;
        d.roofCol = 0xFF8A847C;
        out.add(d);

        // 10. A space-age mushroom water tower on a landscaped mound with a spiral path.
        d = new Landmarks.Def("Water Tower", T_MONUMENT) {
            int[][] lp(int L, int S) {
                int ts = S >= 12 ? 4 : 3;
                return new int[][]{{(L - ts) / 2, (S - ts) / 2, ts, ts, 18}};
            }

            @Override
            int[][] plan(int bw, int bh, Random r) {
                return tp(lp(Math.max(bw, bh), Math.min(bw, bh)), bh > bw);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2, rx = (x1 - x0) / 2, ry = (y1 - y0) / 2;
                int ts = Math.round(Math.min(x1 - x0, y1 - y0) / U) >= 12 ? 4 : 3;
                float tw = ts * U;
                rect(c, p, 0xFF5C8E44, x0, y0, x1, y1);
                // The mound: stacked contours, lighter toward the top-left lit face.
                int[] mc = {0xFF64984A, 0xFF6CA050, 0xFF75A856, 0xFF7EB05C, 0xFF86B862};
                for (int k = 0; k < 5; k++) {
                    float f = 0.95f - k * 0.15f, o = -k * 1.2f;
                    oval(c, p, mc[k], cx - rx * f + o, cy - ry * f + o, cx + rx * f + o, cy + ry * f + o);
                    ovalRing(c, p, 0x18000000, 0.5f, cx - rx * f + o, cy - ry * f + o, cx + rx * f + o, cy + ry * f + o);
                }
                // The spiral path up to the tower, and the steps down to the street.
                int m = 44;
                float[] sx = new float[m + 1], sy = new float[m + 1];
                float fin = tw * 0.66f / Math.min(rx, ry);
                for (int i = 0; i <= m; i++) {
                    float t = i / (float) m;
                    double an = Math.PI * 0.5 + t * Math.PI * 2.6;
                    float f = 0.9f + (fin - 0.9f) * t;
                    sx[i] = cx + (float) Math.cos(an) * rx * f;
                    sy[i] = cy + (float) Math.sin(an) * ry * f;
                }
                line(c, p, 0xFFC2B494, 6, sx[0], sy[0], sx[0], y1);
                p.setStrokeCap(Paint.Cap.ROUND);
                for (int i = 0; i < m; i++) line(c, p, 0xFFA8987A, 5.4f, sx[i], sy[i], sx[i + 1], sy[i + 1]);
                for (int i = 0; i < m; i++) line(c, p, 0xFFD8CCAA, 4, sx[i], sy[i], sx[i + 1], sy[i + 1]);
                p.setStrokeCap(Paint.Cap.BUTT);
                for (float y = sy[0] + 3; y < y1; y += 2.5f) line(c, p, 0x30000000, 0.4f, sx[0] - 3, y, sx[0] + 3, y);
                // Flower beds and shrubs between the turns of the path.
                int[] fl = {0xFFE0A030, 0xFFD05A6A, 0xFFF0E070, 0xFF9A6AC0};
                for (int k = 0; k < 26; k++) {
                    double an = r.nextFloat() * Math.PI * 2;
                    float f = 0.35f + r.nextFloat() * 0.6f;
                    float px = cx + (float) Math.cos(an) * rx * f, py = cy + (float) Math.sin(an) * ry * f;
                    boolean ok = true;
                    for (int i = 0; i <= m && ok; i++) if (Math.abs(sx[i] - px) < 6.5f && Math.abs(sy[i] - py) < 6.5f) ok = false;
                    if (!ok || Math.abs(px - cx) < tw * 0.62f && Math.abs(py - cy) < tw * 0.62f) continue;
                    if (k % 3 == 0) bush(c, p, px, py, 2.6f);
                    else {
                        circle(c, p, 0xFF4E7E3A, px, py, 2.4f);
                        circle(c, p, fl[k % 4], px - 0.6f, py - 0.6f, 1.2f);
                        circle(c, p, fl[(k + 1) % 4], px + 1, py + 0.6f, 0.9f);
                    }
                }
                // Trees at the foot of the mound.
                float[][] tc = {{x0 + 9, y0 + 9}, {x1 - 9, y0 + 9}, {x0 + 9, y1 - 9}, {x1 - 9, y1 - 9}};
                for (float[] t : tc) tree(c, p, t[0], t[1], 7, 0xFF3E7A3A);
                if (y1 - y0 > x1 - x0 + 40) {
                    tree(c, p, x0 + 8, cy, 6, 0xFF4A8640);
                    tree(c, p, x1 - 8, cy, 6, 0xFF4A8640);
                } else if (x1 - x0 > y1 - y0 + 40) {
                    tree(c, p, cx, y0 + 8, 6, 0xFF4A8640);
                }
                // The concrete apron and the long shadow of the tank and stalk.
                circle(c, p, 0xFFB4AE9E, cx, cy, tw * 0.62f);
                circle(c, p, 0xFFC8C2B2, cx, cy, tw * 0.55f);
                line(c, p, 0x38000000, tw * 0.2f, cx, cy, cx + tw * 0.5f, cy + tw * 0.85f);
                circle(c, p, 0x38000000, cx + tw * 0.55f, cy + tw * 0.9f, tw * 0.58f);
                for (int k = 0; k < 4; k++) {
                    double an = Math.PI / 4 + k * Math.PI / 2;
                    bench(c, p, cx + (float) Math.cos(an) * tw * 0.47f, cy + (float) Math.sin(an) * tw * 0.47f, k % 2 == 1);
                }
                lamp(c, p, sx[m / 3], sy[m / 3]);
                lamp(c, p, sx[2 * m / 3], sy[2 * m / 3]);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2, w = Math.min(x1 - x0, y1 - y0), rt = w * 0.6f;
                // The fluted tank: wedges shaded by the light from the top-left.
                int n = 24;
                for (int k = 0; k < n; k++) {
                    double a0 = k * Math.PI * 2 / n, a1 = (k + 1) * Math.PI * 2 / n, am = (a0 + a1) / 2;
                    float lit = (float) (0.5 + 0.5 * Math.cos(am - Math.toRadians(225)));
                    int col = mix(0xFF9AA4A2, 0xFFF4F2EA, lit);
                    if (k % 2 == 0) col = City.darken(col, 0.94f);
                    poly(c, p, col, cx, cy, cx + (float) Math.cos(a0) * rt, cy + (float) Math.sin(a0) * rt,
                            cx + (float) Math.cos(a1) * rt, cy + (float) Math.sin(a1) * rt);
                }
                ring(c, p, 0xFF7E8886, 1, cx, cy, rt - 0.5f);
                ring(c, p, 0xFF3AA0A0, Math.max(1.2f, rt * 0.08f), cx, cy, rt * 0.74f);
                circle(c, p, 0xFFE6E4DC, cx, cy, rt * 0.42f);
                circle(c, p, 0xFFF4F2EC, cx - rt * 0.05f, cy - rt * 0.05f, rt * 0.33f);
                ring(c, p, 0xFF6A7270, 0.4f, cx, cy, rt * 0.5f);
                for (int k = 0; k < 16; k++) {
                    double an = k * Math.PI / 8;
                    circle(c, p, 0xFF6A7270, cx + (float) Math.cos(an) * rt * 0.5f, cy + (float) Math.sin(an) * rt * 0.5f, 0.5f);
                }
                rect(c, p, 0xFF8A9290, cx + rt * 0.1f, cy - rt * 0.08f, cx + rt * 0.26f, cy + rt * 0.08f);
                line(c, p, 0xFF5A6260, 0.6f, cx + rt * 0.26f, cy, cx + rt * 0.95f, cy);
                circle(c, p, 0xFFB8C0BE, cx, cy, rt * 0.1f);
                circle(c, p, 0xFFE03020, cx, cy, Math.max(0.8f, rt * 0.05f));
                circle(c, p, 0x40FFFFFF, cx - rt * 0.38f, cy - rt * 0.38f, rt * 0.22f);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.SUB | Variants.IND | Variants.PARK;
        d.minS = 8;
        d.minL = 8;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_FACTORY;
        d.roofCol = 0xFFE6E4DC;
        out.add(d);

        // 11. A renewable energy farm: rows of solar tables, wind turbines, an inverter house and a fenced yard.
        d = new Landmarks.Def("Wind & Solar Farm", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return tp(new int[][]{{1, 1, 3, 2, 1}}, bh > bw);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                Ax a = new Ax(c, p, x0, y0, x1, y1);
                rect(c, p, 0xFF8EA05E, x0, y0, x1, y1);
                for (int k = 0; k < 24; k++) {
                    float u = r.nextFloat() * a.L, v = r.nextFloat() * a.S;
                    a.C(r.nextBoolean() ? 0x18000000 : 0x18FFFF80, u, v, 3 + r.nextFloat() * 4);
                }
                float tz = Math.min(5.2f * U, a.S * 0.5f), vs1 = a.S - tz;
                // The service track, with a spur to the inverter house.
                a.R(0xFFC2B48E, 0.4f * U, vs1 - 0.3f * U, a.L - 0.4f * U, vs1 + 0.3f * U);
                a.R(0xFFC2B48E, 0.8f * U, 3 * U, 1.4f * U, vs1);
                // Rows of solar tables, each with its cells and a shadow.
                float su0 = 5.2f * U, su1 = a.L - 0.7f * U;
                for (float v = 0.8f * U; v + 6 < vs1 - 0.6f * U; v += 11) {
                    for (float u = su0; u < su1 - 6; u += 34) {
                        float ue = Math.min(su1, u + 32);
                        a.R(0x48000000, u + 1, v + 6, ue + 1, v + 7.8f);
                        a.R(0xFFA8ACB0, u - 0.5f, v - 0.5f, ue + 0.5f, v + 6.5f);
                        a.R(0xFF22364C, u, v, ue, v + 6);
                        a.Ln(0xFF6E8EB0, 0.6f, u, v + 0.3f, ue, v + 0.3f);
                        a.Ln(0xFF33506C, 0.4f, u, v + 3, ue, v + 3);
                        for (float t = u + 5.3f; t < ue - 0.5f; t += 5.3f) a.Ln(0xFF3A5878, 0.4f, t, v, t, v + 6);
                    }
                }
                // The transformer yard beside the inverter house.
                float yv1 = Math.min(vs1 - 0.6f * U, 6.5f * U);
                if (yv1 - 3.6f * U > 14) {
                    a.R(0xFFA8A49A, 1.8f * U, 3.6f * U, 4.8f * U, yv1);
                    frame(c, p, 0xFF6E7276, 0.6f, Math.min(a.x(1.8f * U, 3.6f * U), a.x(4.8f * U, yv1)), Math.min(a.y(1.8f * U, 3.6f * U), a.y(4.8f * U, yv1)),
                            Math.max(a.x(1.8f * U, 3.6f * U), a.x(4.8f * U, yv1)), Math.max(a.y(1.8f * U, 3.6f * U), a.y(4.8f * U, yv1)));
                    for (int k = 0; k < 2; k++) {
                        float u = 2.2f * U + k * 1.3f * U, v = 3.6f * U + 4;
                        a.R(0x40000000, u + 1, v + 1.5f, u + 14, v + 11.5f);
                        a.R(0xFF7C8488, u, v, u + 13, v + 10);
                        for (float t = u + 2; t < u + 12; t += 2) a.Ln(0xFF5C6266, 0.5f, t, v + 1, t, v + 9);
                        a.C(0xFFB8BCC0, u + 4, v + 5, 1.2f);
                        a.C(0xFFB8BCC0, u + 9, v + 5, 1.2f);
                    }
                }
                // The wind turbines.
                int n = Math.max(1, Math.round(a.L / (4.6f * U)));
                float sp = a.L / n, rb = Math.min(tz * 0.46f, sp * 0.46f);
                for (int i = 0; i < n; i++) {
                    float u = (i + 0.5f) * sp, v = vs1 + tz / 2 + 0.15f * U;
                    turbine(c, p, a.x(u, v), a.y(u, v), rb, 15 + i * 37 + r.nextInt(30));
                }
                // The perimeter fence.
                frame(c, p, 0xFF7E8286, 0.6f, x0 + 2, y0 + 2, x1 - 2, y1 - 2);
                for (float x = x0 + 2; x <= x1 - 2; x += 14) {
                    rect(c, p, 0xFF5A5E62, x - 0.6f, y0 + 1.4f, x + 0.6f, y0 + 2.6f);
                    rect(c, p, 0xFF5A5E62, x - 0.6f, y1 - 2.6f, x + 0.6f, y1 - 1.4f);
                }
                for (float y = y0 + 2; y <= y1 - 2; y += 14) {
                    rect(c, p, 0xFF5A5E62, x0 + 1.4f, y - 0.6f, x0 + 2.6f, y + 0.6f);
                    rect(c, p, 0xFF5A5E62, x1 - 2.6f, y - 0.6f, x1 - 1.4f, y + 0.6f);
                }
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
                flatRoof(c, p, x0, y0, x1, y1, 0xFFC4C8C6);
                Ax ra = new Ax(c, p, x0, y0, x1, y1);
                for (int k = 0; k < 3; k++) {
                    float u = 4 + k * (ra.L - 8) / 3;
                    ra.R(0x40000000, u + 1, 4 + 1.5f, u + 9, ra.S - 4 + 1.5f);
                    ra.R(0xFF8E9496, u, 4, u + 8, ra.S - 4);
                    for (float v = 6; v < ra.S - 5; v += 2.2f) ra.Ln(0xFF6A7072, 0.5f, u + 1, v, u + 7, v);
                }
                sign(city, "SOLAR", cx, cy, 5, 0xFF2A4058);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.IND;
        d.minS = 10;
        d.minL = 12;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_FACTORY;
        d.roofCol = 0xFFC4C8C6;
        out.add(d);

        // 12. A data centre: windowless halls under rows of cooling fans, generators and fuel tanks behind a double fence.
        d = new Landmarks.Def("Data Centre", T_BUILDING) {
            int[][] lp(int L, int S) {
                int hw = L - 5;
                if (S >= 12) {
                    int h1 = (S - 3) / 2;
                    return new int[][]{{1, 1, hw, h1, 3}, {1, 2 + h1, hw, h1, 3}, {L - 3, S - 2, 2, 1, 1}};
                }
                return new int[][]{{1, 1, hw, S - 4, 3}, {L - 3, S - 2, 2, 1, 1}};
            }

            @Override
            int[][] plan(int bw, int bh, Random r) {
                return tp(lp(Math.max(bw, bh), Math.min(bw, bh)), bh > bw);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                Ax a = new Ax(c, p, x0, y0, x1, y1);
                int lt = a.lt(), st = a.st();
                rect(c, p, 0xFF5C5E60, x0, y0, x1, y1);
                // A grass verge outside the fence.
                a.R(0xFF6E9450, 0, 0, a.L, 2.5f);
                a.R(0xFF6E9450, 0, 0, 2.5f, a.S);
                a.R(0xFF6E9450, a.L - 2.5f, 0, a.L, a.S);
                a.R(0xFF6E9450, 0, a.S - 2.5f, (lt - 3.4f) * U, a.S);
                // The service yard: generators in containers and fuel tanks in a bund.
                float yu0 = (lt - 4) * U + 0.5f * U, yu1 = a.L - 0.6f * U, yv0 = 0.7f * U, yv1 = (st - 2.6f) * U;
                a.R(0xFF9A9C9C, yu0 - 2, yv0 - 2, yu1 + 2, yv1 + 2);
                int k = 0;
                for (float v = yv0 + 1; v + 12 < yv1; v += 17, k++) {
                    if (k % 3 == 2) {
                        a.R(0xFF7E807E, yu0, v - 1, yu1, v + 13);
                        a.R(0xFFA8AAA6, yu0 + 1.2f, v + 0.2f, yu1 - 1.2f, v + 11.8f);
                        for (int s = 0; s < 2; s++) {
                            float tv = v + 1.2f + s * 5.6f;
                            a.RR(0x40000000, yu0 + 3, tv + 1, yu1 - 2, tv + 5.5f, 2.2f);
                            a.RR(0xFFDCDCD4, yu0 + 2.5f, tv, yu1 - 2.5f, tv + 4.5f, 2.2f);
                            a.Ln(0xFFFFFFFF, 0.6f, yu0 + 4, tv + 1.2f, yu1 - 4, tv + 1.2f);
                        }
                    } else {
                        a.R(0x48000000, yu0 + 1, v + 1.5f, yu1 + 1, v + 13.5f);
                        a.R(0xFF3E5A48, yu0, v, yu1, v + 12);
                        a.R(0xFF4E6E58, yu0 + 1, v + 1, yu1 - 1, v + 11);
                        for (float t = yu0 + (yu1 - yu0) * 0.55f; t < yu1 - 1.5f; t += 2) a.Ln(0xFF2E4436, 0.6f, t, v + 1.5f, t, v + 10.5f);
                        a.C(0xFF262626, yu0 + 6, v + 4, 1.8f);
                        a.C(0xFF262626, yu0 + 6, v + 8, 1.8f);
                        a.C(0xFF555555, yu0 + 6, v + 4, 0.8f);
                        a.C(0xFF555555, yu0 + 6, v + 8, 0.8f);
                    }
                }
                // Lane markings, cable trays between the halls, the gate barrier.
                for (float u = 1.2f * U; u < (lt - 4) * U; u += 10) a.Ln(0xC0E8E8E0, 0.6f, u, a.S - 1.4f * U, u + 5, a.S - 1.4f * U);
                if (st >= 12) {
                    float mv = (1 + (st - 3) / 2) * U + 0.5f * U;
                    a.R(0xFF8A8C8E, 1.4f * U, mv - 1.5f, (lt - 4) * U - 6, mv + 1.5f);
                    for (float u = 1.4f * U; u < (lt - 4) * U - 6; u += 4) a.Ln(0xFF6A6C6E, 0.4f, u, mv - 1.5f, u, mv + 1.5f);
                }
                float gbu = (lt - 3.4f) * U;
                a.Ln(0xFFE8E8E8, 1.2f, gbu, a.S - 1, gbu, a.S - 0.9f * U);
                for (int s = 0; s < 3; s++) a.Ln(0xFFC8302A, 1.3f, gbu, a.S - 3 - s * 4.5f, gbu, a.S - 5 - s * 4.5f);
                // The double security fence and its posts, with cameras at the corners.
                float g0 = a.x(gbu, a.S), g1 = a.y(gbu, a.S);
                for (int s = 0; s < 2; s++) {
                    float in = 2.5f + s * 3;
                    frame(c, p, s == 0 ? 0xFF9EA2A6 : 0xFF7E8286, 0.6f, x0 + in, y0 + in, x1 - in, y1 - in);
                }
                for (float u = 2.5f; u < a.L; u += 12) {
                    a.R(0xFF46494C, u - 0.6f, 1.9f, u + 0.6f, 3.1f);
                    a.R(0xFF46494C, u - 0.6f, a.S - 3.1f, u + 0.6f, a.S - 1.9f);
                }
                for (float v = 2.5f; v < a.S; v += 12) {
                    a.R(0xFF46494C, 1.9f, v - 0.6f, 3.1f, v + 0.6f);
                    a.R(0xFF46494C, a.L - 3.1f, v - 0.6f, a.L - 1.9f, v + 0.6f);
                }
                // The gate gap where the drive meets the street.
                a.R(0xFF5C5E60, gbu, a.S - 6, a.L - 0.4f * U, a.S);
                circle(c, p, 0xFF1E1E1E, x0 + 5.5f, y0 + 5.5f, 1.2f);
                circle(c, p, 0xFF1E1E1E, x1 - 5.5f, y0 + 5.5f, 1.2f);
                circle(c, p, 0xFF1E1E1E, x0 + 5.5f, y1 - 5.5f, 1.2f);
                lamp(c, p, g0 - (a.tall ? 0 : 8), g1 - (a.tall ? 8 : 0));
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                Ax ra = new Ax(c, p, x0, y0, x1, y1);
                float L = ra.L, S = ra.S;
                rect(c, p, 0xFF7A7E82, x0, y0, x1, y1);
                ra.R(0xFFCDD0D2, 1.4f, 1.4f, L - 1.4f, S - 1.4f);
                if (S < 20) {
                    // The gatehouse.
                    ra.R(0xFFE4E6E6, 2.5f, 2.5f, L - 2.5f, S - 2.5f);
                    ra.C(0xFF7A7E82, L * 0.3f, S / 2, 1.5f);
                    return;
                }
                float uw = 14, uh = 8;
                int rows = Math.max(1, Math.min(3, (int) ((S - 6) / 13)));
                float gap = (S - rows * uh) / (rows + 1);
                float step = Math.max(uw + 2.5f, (L - 7) / 10);
                for (int row = 0; row < rows; row++) {
                    float v = gap + row * (uh + gap);
                    for (float u = 4; u + uw < L - 3; u += step) {
                        ra.R(0x48000000, u + 1, v + 1.4f, u + uw + 1, v + uh + 1.4f);
                        ra.R(0xFF84888C, u, v, u + uw, v + uh);
                        ra.R(0xFFA2A6AA, u + 0.6f, v + 0.6f, u + uw - 0.6f, v + uh - 0.6f);
                        for (int f = 0; f < 2; f++) {
                            float fu = u + uw * (f == 0 ? 0.27f : 0.73f), fv = v + uh / 2, fr = uh * 0.38f;
                            ra.C(0xFF2C3034, fu, fv, fr);
                            ra.Ln(0xFF6A7076, 0.6f, fu - fr * 0.8f, fv - fr * 0.3f, fu + fr * 0.8f, fv + fr * 0.3f);
                            ra.Ln(0xFF6A7076, 0.6f, fu - fr * 0.3f, fv + fr * 0.8f, fu + fr * 0.3f, fv - fr * 0.8f);
                            ra.C(0xFF9EA4AA, fu, fv, fr * 0.22f);
                        }
                    }
                }
                // Walkway and roof hatch.
                ra.Ln(0xFFE8C840, 0.6f, 2.5f, S - 2.5f, L - 2.5f, S - 2.5f);
                ra.R(0xFF8A8E92, 2.5f, 2.5f, 7, 7);
            }
        };
        d.ground = City.LOT;
        d.districts = Variants.IND | Variants.UNI;
        d.minS = 10;
        d.minL = 12;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_RACKS;
        d.roofCol = 0xFFCDD0D2;
        out.add(d);
    }
}
