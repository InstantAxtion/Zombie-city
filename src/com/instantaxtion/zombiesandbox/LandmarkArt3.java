package com.instantaxtion.zombiesandbox;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.Landmarks.*;

/** ((22.4) Landmark designs, part 3 (see Landmarks): entertainment and commerce. */
final class LandmarkArt3 {
    private LandmarkArt3() {
    }

    // ------------------------------------------------------------------ shared bits

    /** The way shadows fall in the current frame (to the lower right on screen). */
    static float SX = 1, SY = 1;

    static final int[] CARS = {0xFFB83A30, 0xFF3A5A8A, 0xFFD8D4C8, 0xFF2A2C30, 0xFF7A8A90, 0xFF5A7A4A, 0xFFE0C060, 0xFF8A4A2A};

    static final int[] BRIGHT = {0xFFD8443A, 0xFFE8B83A, 0xFF3A7AC8, 0xFF4AA65A, 0xFFE07A30, 0xFFD060A0, 0xFF6A5AC0, 0xFF3AB0B0};

    /**
     * Starts a frame whose u axis runs along the rectangle's long side (turning the canvas a quarter if it is tall);
     * returns {long, short}. Ends with end().
     */
    static float[] begin(Canvas c, float x0, float y0, float x1, float y1) {
        float w = x1 - x0, h = y1 - y0;
        c.save();
        if (w >= h) {
            c.translate(x0, y0);
            SX = 1;
            SY = 1;
            return new float[]{w, h};
        }
        c.translate(x1, y0);
        c.rotate(90);
        SX = 1;
        SY = -1;
        return new float[]{h, w};
    }

    static void end(Canvas c) {
        c.restore();
        SX = 1;
        SY = 1;
    }

    /** A plan laid out with u along the long side ({u, v, lw, sw, floors}), turned to fit the block. */
    static int[][] orient(int[][] a, int bw, int bh) {
        if (bw >= bh) return a;
        int[][] o = new int[a.length][];
        for (int i = 0; i < a.length; i++) {
            int[] q = a[i];
            o[i] = new int[]{bw - q[1] - q[3], q[0], q[3], q[2], q[4]};
        }
        return o;
    }

    static void sh(Canvas c, Paint p, float x0, float y0, float x1, float y1, float d) {
        rect(c, p, 0x38000000, x0 + d * SX, y0 + d * SY, x1 + d * SX, y1 + d * SY);
    }

    static void shC(Canvas c, Paint p, float x, float y, float r, float d) {
        circle(c, p, 0x38000000, x + d * SX, y + d * SY, r);
    }

    /** A rectangle turned by ang (radians) round its centre; hl, hw are its half length and half width. */
    static void rbox(Canvas c, Paint p, int col, float cx, float cy, float hl, float hw, double ang) {
        float ca = (float) Math.cos(ang), sa = (float) Math.sin(ang);
        poly(c, p, col, cx + ca * hl - sa * hw, cy + sa * hl + ca * hw, cx + ca * hl + sa * hw, cy + sa * hl - ca * hw,
                cx - ca * hl + sa * hw, cy - sa * hl - ca * hw, cx - ca * hl - sa * hw, cy - sa * hl + ca * hw);
    }

    /** A parked car from above, nose towards ang; len scales a classic long car. */
    static void car(Canvas c, Paint p, float cx, float cy, double ang, int col, float len) {
        float ca = (float) Math.cos(ang), sa = (float) Math.sin(ang);
        rbox(c, p, 0x48000000, cx + 1.2f * SX, cy + 1.2f * SY, len + 0.3f, 2.5f, ang);
        rbox(c, p, City.darken(col, 0.8f), cx, cy, len, 2.3f, ang);
        rbox(c, p, col, cx, cy, len - 0.4f, 1.9f, ang);
        rbox(c, p, 0xFF2A3440, cx + ca * len * 0.3f, cy + sa * len * 0.3f, len * 0.16f, 1.8f, ang);
        rbox(c, p, City.lighten(col, 0.12f), cx - ca * len * 0.08f, cy - sa * len * 0.08f, len * 0.26f, 1.7f, ang);
        rbox(c, p, 0xFF2A3440, cx - ca * len * 0.45f, cy - sa * len * 0.45f, len * 0.08f, 1.6f, ang);
    }

    /** A round parasol of eight panels in two colours. */
    static void parasol(Canvas c, Paint p, float x, float y, float r, int c1, int c2) {
        shC(c, p, x, y, r, 1.5f);
        circle(c, p, c2, x, y, r);
        Path path = new Path();
        for (int k = 0; k < 8; k += 2) {
            double a0 = k * Math.PI / 4, a1 = a0 + Math.PI / 4;
            path.moveTo(x, y);
            path.lineTo(x + (float) Math.cos(a0) * r, y + (float) Math.sin(a0) * r);
            path.lineTo(x + (float) Math.cos(a1) * r, y + (float) Math.sin(a1) * r);
            path.close();
        }
        p.setColor(c1);
        c.drawPath(path, p);
        circle(c, p, 0x30000000, x + r * 0.3f, y + r * 0.3f, r * 0.55f);
        circle(c, p, 0xFFEDE6D6, x, y, r * 0.12f);
    }

    /** A picnic table with its benches, along u, with a parasol (c1 != 0) over it. */
    static void picnic(Canvas c, Paint p, float x, float y, int c1, int c2) {
        rect(c, p, 0x30000000, x - 5 + SX, y - 4.5f + SY, x + 5 + SX, y + 4.5f + SY);
        rect(c, p, 0xFF7A5A3A, x - 5, y - 4.5f, x + 5, y - 3);
        rect(c, p, 0xFF7A5A3A, x - 5, y + 3, x + 5, y + 4.5f);
        rect(c, p, 0xFF9A7448, x - 5, y - 2.2f, x + 5, y + 2.2f);
        line(c, p, 0x40000000, 0.4f, x - 5, y, x + 5, y);
        if (c1 != 0) parasol(c, p, x, y, 4.6f, c1, c2);
    }

    /** A string of lights sagging from a to b. */
    static void festoon(Canvas c, Paint p, float ax, float ay, float bx, float by, float sag, int n, boolean multi) {
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
            float x = ax + (bx - ax) * t, y = ay + (by - ay) * t + s;
            circle(c, p, multi ? BRIGHT[k % BRIGHT.length] : 0xFFFFE9A0, x, y, 0.75f);
        }
    }

    /** A line of bunting flags from a to b. */
    static void bunting(Canvas c, Paint p, float ax, float ay, float bx, float by, int n) {
        line(c, p, 0xA0303030, 0.35f, ax, ay, bx, by);
        float dx = (bx - ax) / n, dy = (by - ay) / n, l = (float) Math.sqrt(dx * dx + dy * dy);
        float nx = -dy / l * 2.4f, ny = dx / l * 2.4f;
        if (ny < 0) {
            nx = -nx;
            ny = -ny;
        }
        for (int k = 0; k < n; k++) {
            float x = ax + dx * k, y = ay + dy * k;
            poly(c, p, BRIGHT[k % 6], x + dx * 0.1f, y + dy * 0.1f, x + dx * 0.8f, y + dy * 0.8f, x + dx * 0.45f + nx, y + dy * 0.45f + ny);
        }
    }

    /** A square pyramid tent (a market stall) seen from above. */
    static void tent(Canvas c, Paint p, float x0, float y0, float x1, float y1, int col) {
        float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
        sh(c, p, x0, y0, x1, y1, 1.8f);
        poly(c, p, City.lighten(col, 0.12f), x0, y0, x1, y0, cx, cy);
        poly(c, p, col, x0, y0, x0, y1, cx, cy);
        poly(c, p, City.darken(col, 0.82f), x1, y0, x1, y1, cx, cy);
        poly(c, p, City.darken(col, 0.7f), x0, y1, x1, y1, cx, cy);
    }

    /** A round tent striped in two colours (a ring of wedges round its peak). */
    static void roundTent(Canvas c, Paint p, float cx, float cy, float rx, float ry, int n, int c1, int c2, float shadow) {
        if (shadow > 0) oval(c, p, 0x38000000, cx - rx + shadow * SX, cy - ry + shadow * SY, cx + rx + shadow * SX, cy + ry + shadow * SY);
        for (int k = 0; k < n; k++) {
            double a0 = k * 2 * Math.PI / n, a1 = (k + 1) * 2 * Math.PI / n;
            int col = k % 2 == 0 ? c1 : c2;
            double mid = (a0 + a1) / 2;
            float lit = (float) Math.cos(mid - Math.PI * 1.25);
            col = lit > 0 ? City.lighten(col, lit * 0.1f) : City.darken(col, 1 + lit * 0.18f);
            poly(c, p, col, cx, cy, cx + (float) Math.cos(a0) * rx, cy + (float) Math.sin(a0) * ry,
                    cx + (float) Math.cos(mid) * rx * 1.02f, cy + (float) Math.sin(mid) * ry * 1.02f,
                    cx + (float) Math.cos(a1) * rx, cy + (float) Math.sin(a1) * ry);
        }
    }

    /** A candy-striped stall: an awning roof in stripes and a counter on its front (+v side). */
    static void booth(Canvas c, Paint p, float x0, float y0, float s, int col) {
        sh(c, p, x0, y0, x0 + s, y0 + s, 1.6f);
        rect(c, p, 0xFFF0EADC, x0, y0, x0 + s, y0 + s);
        for (int k = 0; k < 5; k += 2) rect(c, p, col, x0 + s * k / 5f, y0, x0 + s * (k + 1) / 5f, y0 + s);
        rect(c, p, 0x28000000, x0, y0 + s / 2, x0 + s, y0 + s);
        line(c, p, City.darken(col, 0.7f), 0.6f, x0, y0 + s / 2, x0 + s, y0 + s / 2);
        for (int k = 0; k < 5; k++) circle(c, p, k % 2 == 0 ? col : 0xFFF0EADC, x0 + s * (k + 0.5f) / 5f, y0 + s, s / 10f);
    }

    static Path wavePath(float u0, float u1, float v, float amp, float len, float phase, float bottom) {
        Path path = new Path();
        path.moveTo(u0, bottom);
        for (float u = u0; u <= u1 + 0.01f; u += 3) {
            path.lineTo(u, v + amp * (float) Math.sin((u + phase) * 2 * Math.PI / len));
        }
        path.lineTo(u1, v + amp * (float) Math.sin((u1 + phase) * 2 * Math.PI / len));
        path.lineTo(u1, bottom);
        path.close();
        return path;
    }

    static void strokeOval(Canvas c, Paint p, int col, float w, float x0, float y0, float x1, float y1) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(w);
        p.setColor(col);
        RF.set(x0, y0, x1, y1);
        c.drawOval(RF, p);
        p.setStyle(Paint.Style.FILL);
    }

    static void ring(Canvas c, Paint p, int col, float w, float x, float y, float r) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(w);
        p.setColor(col);
        c.drawCircle(x, y, r, p);
        p.setStyle(Paint.Style.FILL);
    }

    /** A hipped roof in col over the rectangle. */
    static void hipRoof(Canvas c, Paint p, float x0, float y0, float x1, float y1, int col) {
        rect(c, p, City.darken(col, 0.6f), x0, y0, x1, y1);
        float mx = (x0 + x1) / 2, my = (y0 + y1) / 2;
        if (x1 - x0 >= y1 - y0) {
            float in = (y1 - y0) / 2;
            poly(c, p, City.lighten(col, 0.08f), x0 + 1, y0 + 1, x1 - 1, y0 + 1, x1 - in, my, x0 + in, my);
            poly(c, p, City.darken(col, 0.8f), x0 + 1, y1 - 1, x1 - 1, y1 - 1, x1 - in, my, x0 + in, my);
            poly(c, p, col, x0 + 1, y0 + 1, x0 + 1, y1 - 1, x0 + in, my);
            poly(c, p, City.darken(col, 0.88f), x1 - 1, y0 + 1, x1 - 1, y1 - 1, x1 - in, my);
            line(c, p, City.lighten(col, 0.25f), 0.9f, x0 + in, my, x1 - in, my);
        } else {
            float in = (x1 - x0) / 2;
            poly(c, p, col, x0 + 1, y0 + 1, x0 + 1, y1 - 1, mx, y1 - in, mx, y0 + in);
            poly(c, p, City.darken(col, 0.85f), x1 - 1, y0 + 1, x1 - 1, y1 - 1, mx, y1 - in, mx, y0 + in);
            poly(c, p, City.lighten(col, 0.08f), x0 + 1, y0 + 1, x1 - 1, y0 + 1, mx, y0 + in);
            poly(c, p, City.darken(col, 0.75f), x0 + 1, y1 - 1, x1 - 1, y1 - 1, mx, y1 - in);
            line(c, p, City.lighten(col, 0.25f), 0.9f, mx, y0 + in, mx, y1 - in);
        }
    }

    /** A rooftop air-handling unit. */
    static void hvac(Canvas c, Paint p, float x, float y, float w, float h) {
        sh(c, p, x, y, x + w, y + h, 1.2f);
        rect(c, p, 0xFF9A9EA2, x, y, x + w, y + h);
        rect(c, p, 0xFFB4B8BC, x + 0.6f, y + 0.6f, x + w - 0.6f, y + h * 0.5f);
        circle(c, p, 0xFF5A5E62, x + w * 0.5f, y + h * 0.65f, Math.min(w, h) * 0.25f);
    }

    // ------------------------------------------------------------------ the designs

    static void register(List<Landmarks.Def> out) {
        funfair(out);
        bowling(out);
        driveIn(out);
        foodTrucks(out);
        aquarium(out);
        iceRink(out);
        goKarts(out);
        departmentStore(out);
        diner(out);
        nightMarket(out);
        tennis(out);
        circus(out);
        brewery(out);
    }

    // A fairground: a Ferris wheel, a carousel, a helter-skelter, stalls and bunting, and a dodgems hall.
    static void funfair(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Funfair", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int lt = Math.max(bw, bh), st = Math.min(bw, bh);
                return orient(new int[][]{{lt - 6, st - 5, 5, 4, 1}}, bw, bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                sign(city, "FUNFAIR", (x0 + x1) / 2, y0 + 10, 7, 0xFFE8B83A);
                float[] f = begin(c, x0, y0, x1, y1);
                float L = f[0], S = f[1], T = City.T;
                float hu = L - 6 * T, hv = S - 5 * T;
                rect(c, p, 0xFF5E9A48, 0, 0, L, S);
                roundRect(c, p, 0xFFC9B48E, 6, 6, L - 6, S - 6, 8);
                // Trodden patches in the sand.
                for (int k = 0; k < 14; k++) {
                    float x = 12 + r.nextFloat() * (L - 24), y = 12 + r.nextFloat() * (S - 24);
                    oval(c, p, 0x14000000, x, y, x + 8 + r.nextFloat() * 14, y + 4 + r.nextFloat() * 6);
                }
                // The Ferris wheel.
                float wr = Math.min(S * 0.34f, L * 0.24f), wx = 12 + wr, wy = Math.max(wr + 10, S * 0.42f);
                if (wy + wr + 8 > S) wy = S - wr - 8;
                ring(c, p, 0x30000000, 3, wx + 7 * SX, wy + 7 * SY, wr);
                line(c, p, 0xFF5A5E66, 2.2f, wx, wy, wx - wr * 0.5f, wy + wr + 3);
                line(c, p, 0xFF5A5E66, 2.2f, wx, wy, wx + wr * 0.5f, wy + wr + 3);
                rect(c, p, 0xFF8A6A4A, wx - wr * 0.6f, wy + wr + 1, wx + wr * 0.6f, wy + wr + 6);
                rect(c, p, 0xFFA8845A, wx - wr * 0.6f, wy + wr + 1, wx + wr * 0.6f, wy + wr + 2.5f);
                for (int k = 0; k < 16; k++) {
                    double a = k * Math.PI / 8;
                    line(c, p, 0xFFB8BCC4, 0.7f, wx, wy, wx + (float) Math.cos(a) * wr, wy + (float) Math.sin(a) * wr);
                }
                ring(c, p, 0xFFC83A32, 1.2f, wx, wy, wr * 0.55f);
                ring(c, p, 0xFFE8E4DC, 2.2f, wx, wy, wr);
                ring(c, p, 0xFFC83A32, 0.9f, wx, wy, wr - 2.5f);
                for (int k = 0; k < 12; k++) {
                    double a = k * Math.PI / 6 + 0.13;
                    float gx = wx + (float) Math.cos(a) * wr, gy = wy + (float) Math.sin(a) * wr + 1.5f;
                    float gr = Math.max(2.4f, wr * 0.085f);
                    roundRect(c, p, 0x40000000, gx - gr + SX, gy - gr * 0.8f + SY, gx + gr + SX, gy + gr * 0.8f + SY, 1.2f);
                    roundRect(c, p, City.darken(BRIGHT[k % 6], 0.7f), gx - gr, gy - gr * 0.8f, gx + gr, gy + gr * 0.8f, 1.2f);
                    roundRect(c, p, BRIGHT[k % 6], gx - gr + 0.6f, gy - gr * 0.8f + 0.6f, gx + gr - 0.6f, gy, 1);
                }
                circle(c, p, 0xFF3A3E46, wx, wy, 3.2f);
                circle(c, p, 0xFFE8B83A, wx, wy, 1.6f);
                // The carousel, over the dodgems hall.
                float uR = wx + wr + 10;
                float cr = Math.min(Math.min((hv - 16) / 2, (L - 8 - uR) / 2), Math.min(S * 0.2f, wr * 0.68f));
                float cx = uR + cr, cy = 8 + cr;
                if (cr > 10) {
                    circle(c, p, 0x38000000, cx + 3 * SX, cy + 3 * SY, cr + 1);
                    circle(c, p, 0xFFA89070, cx, cy, cr + 1.5f);
                    roundTent(c, p, cx, cy, cr, cr, 16, 0xFFD8443A, 0xFFF2E8D0, 0);
                    for (int k = 0; k < 16; k++) {
                        double a = (k + 0.5) * Math.PI / 8;
                        circle(c, p, k % 2 == 0 ? 0xFFE8B83A : 0xFFD8443A, cx + (float) Math.cos(a) * cr, cy + (float) Math.sin(a) * cr, 1.6f);
                    }
                    circle(c, p, 0xFFE8B83A, cx, cy, cr * 0.18f);
                    circle(c, p, 0xFFF8E8A0, cx - 0.6f, cy - 0.6f, cr * 0.08f);
                }
                // Stalls in the open ground.
                float su = cr > 10 ? cx + cr + 8 : uR;
                int col = 0;
                if (hv - 16 >= 12) {
                    float s = Math.min(16, hv - 18);
                    for (float u = su; u + s <= L - 8; u += s + 5) booth(c, p, u, 8, s, BRIGHT[col++ % 6]);
                }
                float bv = wy + wr + 12;
                if (S - 8 - bv >= 12) {
                    float s = Math.min(16, S - 10 - bv);
                    // A helter-skelter first.
                    float hr = s * 0.55f, hx = 10 + hr, hy = bv + s / 2;
                    circle(c, p, 0x40000000, hx + 4 * SX, hy + 4 * SY, hr);
                    for (int k = 5; k >= 1; k--) circle(c, p, k % 2 == 0 ? 0xFFE8B83A : 0xFFD8443A, hx, hy, hr * k / 5f);
                    circle(c, p, 0xFF3A7AC8, hx, hy, hr * 0.15f);
                    for (float u = hx + hr + 5; u + s <= hu - 6; u += s + 5) booth(c, p, u, bv, s, BRIGHT[col++ % 6]);
                }
                // Bunting zigzagging over the midway.
                float by = Math.min(hv - 4, S * 0.55f);
                bunting(c, p, wx + wr * 0.6f, wy - wr * 0.8f, L - 10, by * 0.5f, 10);
                bunting(c, p, wx + wr * 0.3f, wy + wr * 0.95f, hu - 4, by, 10);
                bunting(c, p, 8, 8, wx - wr * 0.4f, wy - wr * 0.95f, 5);
                // Lamps and a couple of trees on the grass corners.
                lamp(c, p, uR - 3, S * 0.5f);
                lamp(c, p, hu - 4, hv - 4);
                lamp(c, p, L - 10, hv - 4);
                tree(c, p, L - 6, 6, 5.5f);
                tree(c, p, 6, S - 6, 5.5f);
                end(c);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                float[] f = begin(c, x0, y0, x1, y1);
                float L = f[0], S = f[1];
                rect(c, p, 0xFF3E5A56, 0, 0, L, S);
                rect(c, p, 0xFF4E6E68, 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                for (float u = 4; u < L - 2; u += 3) line(c, p, 0x22000000, 0.6f, u, 2, u, S - 2);
                // A big skylight over the dodgem floor, cars inside.
                float gx0 = L * 0.18f, gx1 = L * 0.82f, gy0 = S * 0.22f, gy1 = S * 0.72f;
                rect(c, p, 0xFF2A3036, gx0, gy0, gx1, gy1);
                rect(c, p, 0xFF3A424A, gx0 + 1, gy0 + 1, gx1 - 1, gy1 - 1);
                for (int k = 0; k < 7; k++) {
                    float x = gx0 + 4 + r.nextFloat() * (gx1 - gx0 - 8), y = gy0 + 3 + r.nextFloat() * (gy1 - gy0 - 6);
                    rbox(c, p, BRIGHT[k % 8], x, y, 2.2f, 1.6f, r.nextFloat() * 6.3f);
                    circle(c, p, 0xFFFFF0A0, x, y, 0.5f);
                }
                for (float u = gx0 + 6; u < gx1 - 1; u += 6) line(c, p, 0x90B8C4CC, 0.5f, u, gy0, u, gy1);
                rect(c, p, 0x30FFFFFF, gx0, gy0, gx1, gy0 + (gy1 - gy0) * 0.3f);
                // Scalloped valance along the front.
                for (float u = 0; u < L - 0.5f; u += 4) poly(c, p, ((int) (u / 4)) % 2 == 0 ? 0xFFD8443A : 0xFFF2E8D0, u, S - 3, u + 4, S - 3, u + 2, S);
                rect(c, p, 0xFFE8B83A, 0, S - 4, L, S - 3);
                end(c);
                sign(city, "DODGEMS", (x0 + x1) / 2, (y0 + y1) / 2, 6, 0xFFE8B83A);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.PARK | Variants.SUB | Variants.MID;
        d.minS = 10;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_ARCADE;
        d.roofCol = 0xFF4E6E68;
        out.add(d);
    }

    // A long retro bowling alley with a giant pin on the roof, neon trim and a handful of parking bays.
    static void bowling(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Bowling Alley", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int lt = Math.max(bw, bh), st = Math.min(bw, bh);
                int h = Math.max(4, Math.min(7, st - 5));
                return orient(new int[][]{{1, 1, lt - 2, h, 1}}, bw, bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                float[] f = begin(c, x0, y0, x1, y1);
                float L = f[0], S = f[1], T = City.T;
                int h = Math.max(4, Math.min(7, (int) (S / T) - 5));
                float fy = (1 + h) * T;
                rect(c, p, 0xFF5E9A48, 0, 0, L, S);
                rect(c, p, 0xFFC8C2B4, 8, 8, L - 8, fy + 8);
                // Front walk with a neon-lit entrance canopy.
                rect(c, p, 0xFFD6D0C2, 8, fy, L - 8, fy + 10);
                for (float u = 12; u < L - 8; u += 8) line(c, p, 0x18000000, 0.5f, u, fy, u, fy + 10);
                float ex = L * 0.5f;
                rect(c, p, 0x40000000, ex - 14 + 2 * SX, fy - 1 + 2 * SY, ex + 14 + 2 * SX, fy + 9 + 2 * SY);
                rect(c, p, 0xFF2E2A3A, ex - 14, fy - 1, ex + 14, fy + 9);
                frame(c, p, 0xFFFF4FA0, 0.9f, ex - 13, fy, ex + 13, fy + 8);
                line(c, p, 0xFF4FE8FF, 0.7f, ex - 11, fy + 6, ex + 11, fy + 6);
                // A short row of parking bays, the rest lawn and trees.
                float py1 = S - 6, py0 = Math.max(fy + 14, py1 - 18);
                if (py0 - fy > 26) {
                    for (float u = 16; u < L - 10; u += 18) tree(c, p, u, (fy + 10 + py0) / 2, 6);
                }
                if (py1 - py0 >= 14) {
                    float bw = 11, n = Math.min(5, (int) ((L * 0.45f) / bw));
                    float pu0 = 10, pu1 = pu0 + n * bw;
                    rect(c, p, 0xFF5A5C60, pu0 - 2, py0, pu1 + 2, py1);
                    for (int k = 0; k <= n; k++) line(c, p, 0xFFE8E4D8, 0.6f, pu0 + k * bw, py1 - 14, pu0 + k * bw, py1 - 1);
                    int[] cc = {0xFFB83A30, 0xFF3A5A8A, 0xFFD8D4C8};
                    car(c, p, pu0 + bw * 0.5f, py1 - 7.5f, -Math.PI / 2, cc[0], 4.4f);
                    car(c, p, pu0 + bw * 2.5f, py1 - 7.5f, -Math.PI / 2, cc[1], 4.4f);
                    if (n >= 4) car(c, p, pu0 + bw * 3.5f, py1 - 7.5f, -Math.PI / 2, cc[2], 4.4f);
                    for (float u = pu1 + 8; u < L - 22; u += 9) bush(c, p, u, py1 - 7, 2.6f);
                    // The neon roadside pylon: a pin-shaped sign.
                    float sx = L - 14, sy = py1 - 6;
                    lamp(c, p, pu0 - 2, py0 + 2);
                    circle(c, p, 0x40000000, sx + 3 * SX, sy + 3 * SY, 4);
                    oval(c, p, 0xFFF4F0E8, sx - 3, sy - 6, sx + 3, sy + 4);
                    rect(c, p, 0xFFD8443A, sx - 2.6f, sy - 2.6f, sx + 2.6f, sy - 1.6f);
                    strokeOval(c, p, 0xFFFF4FA0, 0.6f, sx - 3.6f, sy - 6.6f, sx + 3.6f, sy + 4.6f);
                } else {
                    for (float u = 14; u < L - 10; u += 20) tree(c, p, u, S - 7, 5);
                }
                lamp(c, p, ex - 18, fy + 5);
                lamp(c, p, ex + 18, fy + 5);
                end(c);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                float[] f = begin(c, x0, y0, x1, y1);
                float L = f[0], S = f[1];
                rect(c, p, 0xFF4E4A58, 0, 0, L, S);
                rect(c, p, 0xFF6E6A7A, 2, 2, L - 2, S - 2);
                // Lane-like ribs, a nod to what is underneath.
                for (float v = 6; v < S - 4; v += 5) line(c, p, 0x18000000, 0.6f, 4, v, L - 4, v);
                // Neon trim round the parapet: pink and cyan.
                frame(c, p, 0xFFFF4FA0, 0.9f, 1, 1, L - 1, S - 1);
                frame(c, p, 0xFF4FE8FF, 0.6f, 2.6f, 2.6f, L - 2.6f, S - 2.6f);
                // The giant pin lying along the roof, and a ball beside it.
                float pl = Math.min(L * 0.55f, 120), pw = Math.min(S * 0.42f, pl * 0.34f);
                float pu = L * 0.5f - pl / 2, pv = S * 0.5f;
                oval(c, p, 0x50000000, pu + 3, pv - pw / 2 + 4, pu + pl * 0.62f + 3, pv + pw / 2 + 4);
                oval(c, p, 0x50000000, pu + pl * 0.6f + 3, pv - pw * 0.27f + 4, pu + pl + 3, pv + pw * 0.27f + 4);
                oval(c, p, 0xFFD8D2C6, pu, pv - pw / 2, pu + pl * 0.62f, pv + pw / 2);
                oval(c, p, 0xFFF4F0E8, pu + 1, pv - pw / 2 + 1, pu + pl * 0.6f, pv + pw * 0.2f);
                rect(c, p, 0xFFE4DED2, pu + pl * 0.5f, pv - pw * 0.16f, pu + pl * 0.7f, pv + pw * 0.16f);
                oval(c, p, 0xFFD8D2C6, pu + pl * 0.62f, pv - pw * 0.3f, pu + pl, pv + pw * 0.3f);
                oval(c, p, 0xFFF4F0E8, pu + pl * 0.64f, pv - pw * 0.28f, pu + pl * 0.98f, pv + pw * 0.08f);
                rect(c, p, 0xFFD8443A, pu + pl * 0.52f, pv - pw * 0.2f, pu + pl * 0.55f, pv + pw * 0.2f);
                rect(c, p, 0xFFD8443A, pu + pl * 0.57f, pv - pw * 0.2f, pu + pl * 0.6f, pv + pw * 0.2f);
                float br = pw * 0.42f, bx = pu - br - 6;
                if (bx - br > 4) {
                    circle(c, p, 0x50000000, bx + 3, pv + 4, br);
                    circle(c, p, 0xFF2E3A8A, bx, pv, br);
                    circle(c, p, 0xFF4A5AB0, bx - br * 0.25f, pv - br * 0.25f, br * 0.55f);
                    circle(c, p, 0xFF1A1E3A, bx + br * 0.2f, pv - br * 0.3f, br * 0.12f);
                    circle(c, p, 0xFF1A1E3A, bx + br * 0.45f, pv - br * 0.05f, br * 0.12f);
                    circle(c, p, 0xFF1A1E3A, bx + br * 0.15f, pv + br * 0.05f, br * 0.12f);
                }
                if (pu + pl + 16 < L - 4) {
                    hvac(c, p, L - 18, 6, 10, 7);
                    hvac(c, p, L - 18, S - 14, 10, 7);
                }
                end(c);
                sign(city, "BOWL", (x0 + x1) / 2, (y0 + y1) / 2, 7, 0xFFFF4FA0);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.MID | Variants.SUB;
        d.minS = 9;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_ARCADE;
        d.roofCol = 0xFF6E6A7A;
        out.add(d);
    }

    // A drive-in: a huge screen, arcs of car bays facing it with a few cars, and the projection and snack hut.
    static void driveIn(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Drive-in Cinema", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int lt = Math.max(bw, bh), st = Math.min(bw, bh);
                return orient(new int[][]{{lt - 5, st / 2 - 1, 3, 3, 1}}, bw, bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                float[] f = begin(c, x0, y0, x1, y1);
                float L = f[0], S = f[1];
                rect(c, p, 0xFF6A8A4A, 0, 0, L, S);
                rect(c, p, 0xFF6C6A66, 6, 6, L - 6, S - 6);
                for (int k = 0; k < 20; k++) {
                    float x = 8 + r.nextFloat() * (L - 16), y = 8 + r.nextFloat() * (S - 16);
                    circle(c, p, 0x10FFFFFF, x, y, 2 + r.nextFloat() * 4);
                }
                // The screen, on legs, with its long shadow on the grass.
                float sv0 = S * 0.12f, sv1 = S * 0.88f, su = 10;
                rect(c, p, 0x28000000, su + 3, sv0 + 6 * SY, su + 14, sv1 + 6 * SY);
                for (float v = sv0 + 4; v < sv1; v += 12) line(c, p, 0xFF4A4C50, 1.2f, su + 2, v, su + 10, v);
                rect(c, p, 0xFF3A3C40, su - 1, sv0 - 2, su + 3, sv1 + 2);
                rect(c, p, 0xFFF2F0EA, su + 3, sv0, su + 5, sv1);
                rect(c, p, 0xFF8A8E94, su - 1, sv0 - 2, su + 3, sv0 + 1);
                // Arcs of bays round the screen, each ramp a lighter band, with speaker posts.
                float cx = su - S * 0.25f, cy = S / 2;
                int rows = 0;
                for (float rr = S * 0.62f; cx + rr < L - 5 * City.T - 16; rr += 21) {
                    ring(c, p, 0xFF7E7C76, 9, cx, cy, rr);
                    ring(c, p, 0x40FFFFFF, 0.5f, cx, cy, rr - 4.5f);
                    double half = Math.asin(Math.min(1, (S / 2 - 12) / rr));
                    int n = (int) (2 * half * rr / 11);
                    for (int k = 0; k <= n; k++) {
                        double a = -half + 2 * half * k / Math.max(1, n);
                        float bx = cx + (float) Math.cos(a) * rr, by = cy + (float) Math.sin(a) * rr;
                        if (by < 10 || by > S - 10) continue;
                        float ox = (float) Math.cos(a) * 6, oy = (float) Math.sin(a) * 6;
                        circle(c, p, 0xFF3A3C40, bx + ox, by + oy, 0.8f);
                        line(c, p, 0x50FFFFFF, 0.4f, bx - (float) Math.sin(a) * 5.5f - ox * 0.7f, by + (float) Math.cos(a) * 5.5f - oy * 0.7f,
                                bx - (float) Math.sin(a) * 5.5f + ox * 0.5f, by + (float) Math.cos(a) * 5.5f + oy * 0.5f);
                        if (r.nextFloat() < 0.3f) car(c, p, bx, by, a + Math.PI, CARS[r.nextInt(CARS.length)], 4.4f);
                    }
                    rows++;
                    if (rows >= 6) break;
                }
                // Snack bar patio round the hut, entry lane and the ticket booth.
                float hu = L - 5 * City.T, hv = (float) (Math.floor(S / City.T / 2) - 1) * City.T;
                rect(c, p, 0xFFB8B0A0, hu - 8, hv - 8, hu + 3 * City.T + 8, hv + 3 * City.T + 8);
                for (int k = 0; k < 3; k++) picnic(c, p, hu - 2 + k * 18, hv - 14 > 8 ? hv - 4 : hv + 3 * City.T + 4, 0, 0);
                rect(c, p, 0xFF5E5C58, L - 30, S - 6, L - 18, S);
                rect(c, p, 0xFFE8E0C8, L - 24, S - 20, L - 18, S - 12);
                rect(c, p, 0xFFC83A32, L - 24, S - 20, L - 18, S - 18);
                tree(c, p, L - 8, 8, 5);
                tree(c, p, L - 8, S - 26, 5);
                end(c);
                sign(city, "DRIVE-IN", (x0 + x1) / 2, (y0 + y1) / 2, 7, 0xFFF2F0EA);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                float L = x1 - x0, S = y1 - y0;
                rect(c, p, 0xFF8A3A30, x0, y0, x1, y1);
                rect(c, p, 0xFFB04A3A, x0 + 1.5f, y0 + 1.5f, x1 - 1.5f, y1 - 1.5f);
                for (float x = x0 + 4; x < x1 - 2; x += 5) rect(c, p, 0xFFF2E8D0, x, y0 + 1.5f, x + 2.5f, y1 - 1.5f);
                rect(c, p, 0x30000000, x0 + 1.5f, (y0 + y1) / 2, x1 - 1.5f, y1 - 1.5f);
                // The projector box with its port and a popcorn bucket on top.
                roundRect(c, p, 0xFF3A3C40, x0 + L * 0.1f, y0 + S * 0.3f, x0 + L * 0.42f, y0 + S * 0.7f, 1.5f);
                circle(c, p, 0xFFF8F0B0, x0 + L * 0.12f, y0 + S * 0.5f, 1.4f);
                circle(c, p, 0xFFC83A32, x0 + L * 0.7f, y0 + S * 0.5f, S * 0.18f);
                circle(c, p, 0xFFF8F0D8, x0 + L * 0.7f, y0 + S * 0.5f, S * 0.13f);
                circle(c, p, 0xFFF0D890, x0 + L * 0.68f, y0 + S * 0.48f, S * 0.07f);
            }
        };
        d.ground = City.LOT;
        d.districts = Variants.SUB | Variants.IND;
        d.minS = 10;
        d.minL = 14;
        d.base = City.KIOSK;
        d.inside = Variants.I_CAFE;
        d.roofCol = 0xFFB04A3A;
        out.add(d);
    }

    // A paved plaza ringed with food trucks, picnic tables under parasols and festoon lights, with a small bar.
    static void foodTrucks(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Food Truck Plaza", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int lt = Math.max(bw, bh), st = Math.min(bw, bh);
                return orient(new int[][]{{lt - 4, st / 2 - 1, 3, 3, 1}}, bw, bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                float[] f = begin(c, x0, y0, x1, y1);
                float L = f[0], S = f[1], T = City.T;
                rect(c, p, 0xFFC8BCA6, 0, 0, L, S);
                for (float u = 0; u < L; u += 12) line(c, p, 0x14000000, 0.5f, u, 0, u, S);
                for (float v = 0; v < S; v += 12) line(c, p, 0x14000000, 0.5f, 0, v, L, v);
                // A herringbone band of red brick through the middle.
                rect(c, p, 0xFFB07A5A, 0, S / 2 - 6, L - 4 * T - 6, S / 2 + 6);
                for (float u = 2; u < L - 4 * T - 6; u += 6) line(c, p, 0x30000000, 0.5f, u, S / 2 - 6, u + 3, S / 2 + 6);
                // Trucks along both long sides, hatches facing in.
                float au = L - 4 * T - 10;
                int k = 0;
                for (float u = 8; u + 30 <= au; u += 36) {
                    truck(c, p, u, 6, 30, 11, BRIGHT[k % 8], BRIGHT[(k + 3) % 8], true);
                    k++;
                    if (u + 18 + 30 <= au) truck(c, p, u + 18, S - 17, 30, 11, BRIGHT[(k + 1) % 8], BRIGHT[(k + 4) % 8], false);
                    k++;
                }
                // Tables in the middle.
                int[][] pc = {{0xFFD8443A, 0xFFF2E8D0}, {0xFF3A7AC8, 0xFFF2E8D0}, {0xFF4AA65A, 0xFFF2E8D0}, {0xFFE8B83A, 0xFFD8443A}};
                int t = 0;
                float st = Math.max(22, Math.min(32, L / 10));
                for (float v = 36; v <= S - 36; v += st) {
                    for (float u = 18 + ((int) (v / st) % 2) * st * 0.45f; u < au - 6; u += st) {
                        if (Math.abs(v - S / 2) < 8) continue;
                        picnic(c, p, u, v, pc[t % 4][0], pc[t % 4][1]);
                        t++;
                    }
                }
                // Trees in planters and the light strings between the poles.
                for (float u = 10; u < au; u += 44) {
                    rect(c, p, 0xFF8A8478, u - 5, S / 2 - 5, u + 5, S / 2 + 5);
                    tree(c, p, u, S / 2, 5.5f);
                }
                for (float u = 10; u + 44 < au + 1; u += 44) {
                    festoon(c, p, u, 26, u + 44, S - 26, 8, 10, false);
                    festoon(c, p, u, S - 26, u + 44, 26, 8, 10, false);
                }
                for (float u = 10; u < au + 1; u += 44) {
                    lamp(c, p, u, 26);
                    lamp(c, p, u, S - 26);
                }
                // Bins and a bar patio round the kiosk.
                rect(c, p, 0xFF9A8E7A, L - 4 * T - 6, S / 2 - T - 10, L - 4, S / 2 + 2 * T + 10);
                circle(c, p, 0xFF3E5A3A, au - 2, 12, 2);
                circle(c, p, 0xFF3E5A3A, au - 2, S - 12, 2);
                end(c);
                sign(city, "STREET FOOD", (x0 + x1) / 2, (y0 + y1) / 2, 6, 0xFFE8B83A);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                // A shipping-container bar with a planted roof edge.
                rect(c, p, 0xFF2E6A6A, x0, y0, x1, y1);
                for (float x = x0 + 1; x < x1 - 1; x += 2.5f) rect(c, p, 0xFF3A8080, x, y0 + 1, x + 1.3f, y1 - 1);
                rect(c, p, 0xFFC8A040, x0, y0 + (y1 - y0) * 0.45f, x1, y0 + (y1 - y0) * 0.55f);
                for (float x = x0 + 4; x < x1 - 2; x += 7) bush(c, p, x, y0 + 3, 2.4f);
                hvac(c, p, x1 - 12, y1 - 11, 8, 7);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.MID | Variants.UNI;
        d.minS = 10;
        d.minL = 12;
        d.base = City.KIOSK;
        d.inside = Variants.I_BAR;
        d.roofCol = 0xFF3A8080;
        out.add(d);
    }

    /** A food truck along u: box, cab at the far end, a striped awning over its hatch (down: on the +v side). */
    static void truck(Canvas c, Paint p, float x, float y, float len, float wid, int col, int awn, boolean down) {
        sh(c, p, x, y, x + len, y + wid, 1.8f);
        roundRect(c, p, City.darken(col, 0.7f), x, y, x + len, y + wid, 1.5f);
        float cab = len * 0.24f;
        roundRect(c, p, City.darken(col, 0.85f), x + len - cab, y + 0.6f, x + len - 0.4f, y + wid - 0.6f, 1.5f);
        rect(c, p, 0xFF2A3440, x + len - cab * 0.5f, y + 1.2f, x + len - cab * 0.2f, y + wid - 1.2f);
        rect(c, p, col, x + 0.7f, y + 0.7f, x + len - cab - 0.5f, y + wid - 0.7f);
        rect(c, p, City.lighten(col, 0.2f), x + 0.7f, y + 0.7f, x + len - cab - 0.5f, y + 2);
        rect(c, p, 0xFFB4B8BC, x + len * 0.18f, y + wid * 0.35f, x + len * 0.32f, y + wid * 0.65f);
        circle(c, p, 0xFF6A6E72, x + len * 0.5f, y + wid * 0.5f, 1.3f);
        float a0 = down ? y + wid : y - 4, a1 = a0 + 4;
        rect(c, p, 0x30000000, x + 2, down ? a1 : a0 - 1, x + len - cab - 1, down ? a1 + 1 : a0);
        float aw = (len - cab - 3) / 6;
        for (int k = 0; k < 6; k++) rect(c, p, k % 2 == 0 ? awn : 0xFFF2EADA, x + 2 + k * aw, a0, x + 2 + (k + 1) * aw, a1);
    }

    // An aquarium: a long building with a wavy blue roof and glass rotunda ends, a seal pool and a fish sign.
    static void aquarium(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Aquarium", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int lt = Math.max(bw, bh), st = Math.min(bw, bh);
                return orient(new int[][]{{1, 1, lt - 2, aqH(st), 2}}, bw, bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                float[] f = begin(c, x0, y0, x1, y1);
                float L = f[0], S = f[1], T = City.T;
                float fy = (1 + aqH((int) (S / T))) * T;
                rect(c, p, 0xFF5E9A48, 0, 0, L, S);
                rect(c, p, 0xFFD4CEC0, 6, 6, L - 6, fy + 8);
                // Wavy blue paving along the front.
                p.setColor(0xFF9CB8C8);
                c.drawPath(wavePath(6, L - 6, fy + 5, 1.6f, 24, 0, fy + 10), p);
                // The seal pool: a rocky lagoon, a beach and seals basking.
                float pu0 = 12, pu1 = Math.max(L * 0.5f, 70), pv0 = fy + 14, pv1 = S - 8;
                if (pv1 - pv0 > 20) {
                    oval(c, p, 0xFF8E8676, pu0 - 3, pv0 - 3, pu1 + 3, pv1 + 3);
                    oval(c, p, 0xFF2E7AA0, pu0, pv0, pu1, pv1);
                    oval(c, p, 0xFF3E92B8, pu0 + 4, pv0 + 3, pu1 - 10, pv1 - 5);
                    float rk = (pv1 - pv0) * 0.3f;
                    oval(c, p, 0xFFD8C8A0, pu1 - rk * 2.2f, pv0 + 2, pu1 - 2, pv0 + rk * 1.6f);
                    circle(c, p, 0xFF8A8478, pu0 + (pu1 - pu0) * 0.4f, (pv0 + pv1) / 2, rk * 0.6f);
                    circle(c, p, 0xFFA09A8C, pu0 + (pu1 - pu0) * 0.4f - 1, (pv0 + pv1) / 2 - 1, rk * 0.35f);
                    float[][] seals = {{0.75f, 0.3f, 0.3f}, {0.85f, 0.25f, 1.2f}, {0.25f, 0.6f, 2.4f}, {0.55f, 0.75f, 0.9f}};
                    for (float[] s : seals) {
                        float x = pu0 + (pu1 - pu0) * s[0], y = pv0 + (pv1 - pv0) * s[1];
                        rbox(c, p, 0xFF4A4A4E, x, y, 3.2f, 1.5f, s[2]);
                        circle(c, p, 0xFF5E5E64, x + (float) Math.cos(s[2]) * 3, y + (float) Math.sin(s[2]) * 3, 1.4f);
                    }
                    for (int k = 0; k < 5; k++) line(c, p, 0x50FFFFFF, 0.5f, pu0 + 10 + k * 9, pv1 - 8 - k % 2 * 4, pu0 + 15 + k * 9, pv1 - 8 - k % 2 * 4);
                    for (float u = pu0; u <= pu1; u += 6) circle(c, p, 0xFF3A3C40, u, pv0 - 5, 0.6f);
                    line(c, p, 0xFF5A5C60, 0.6f, pu0, pv0 - 5, pu1, pv0 - 5);
                }
                // The fish sign on its plinth, trees and benches.
                float fx = Math.max(pu1 + 24, L * 0.72f), fyy = (fy + S) / 2 + 2;
                if (fx < L - 18 && S - fy > 30) {
                    rect(c, p, 0xFFB8B0A0, fx - 16, fyy - 9, fx + 16, fyy + 9);
                    fish(c, p, fx, fyy, 13, 0xFFE8783A);
                    for (float u = fx + 26; u < L - 6; u += 16) tree(c, p, u, fyy, 5.5f);
                    bench(c, p, fx - 24, fyy, false);
                } else {
                    for (float u = pu1 + 12; u < L - 6; u += 16) tree(c, p, u, (fy + S) / 2 + 4, 5);
                }
                end(c);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                float[] f = begin(c, x0, y0, x1, y1);
                float L = f[0], S = f[1], a = Math.min(S * 0.5f, L * 0.2f);
                rect(c, p, 0xFFD4CEC0, 0, 0, L, S);
                // Glass rotundas at the ends.
                oval(c, p, 0xFF5E7A8A, 0, 0, 2 * a, S);
                oval(c, p, 0xFF9CC4D4, 1.5f, 1.5f, 2 * a - 1.5f, S - 1.5f);
                oval(c, p, 0xFF5E7A8A, L - 2 * a, 0, L, S);
                oval(c, p, 0xFF8AB4C6, L - 2 * a + 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                for (int k = 0; k < 8; k++) {
                    double an = Math.PI / 2 + k * Math.PI / 7;
                    line(c, p, 0x806A8A9A, 0.5f, a, S / 2, a + (float) Math.cos(an) * a, S / 2 + (float) Math.sin(an) * S / 2);
                    line(c, p, 0x806A8A9A, 0.5f, L - a, S / 2, L - a - (float) Math.cos(an) * a, S / 2 + (float) Math.sin(an) * S / 2);
                }
                // The wave roof between them, darker bands to the front.
                rect(c, p, 0xFF1E4A78, a, 0, L - a, S);
                int[] cols = {0xFF4A90C8, 0xFF3A7CB8, 0xFF2E68A4, 0xFF245890};
                for (int k = 0; k < 4; k++) {
                    p.setColor(cols[k]);
                    c.drawPath(wavePath(a, L - a, S * (0.08f + 0.22f * k), S * 0.06f, Math.max(30, S * 1.1f), k * 9, S - (k == 3 ? 1.5f : 0)), p);
                    line(c, p, 0x50FFFFFF, 0.5f, a + 2, S * (0.08f + 0.22f * k) + 1, a + 6, S * (0.08f + 0.22f * k) + 1);
                }
                // A white crest line and the fish mural painted big.
                fish(c, p, L / 2, S * 0.5f, Math.min(S * 0.3f, L * 0.16f), 0xFFF0A040);
                end(c);
                sign(city, "AQUARIUM", (x0 + x1) / 2, (y0 + y1) / 2, 7, 0xFF9CD8F0);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.MID | Variants.PARK;
        d.minS = 10;
        d.minL = 14;
        d.base = City.OFFICE;
        d.inside = Variants.I_SEATS;
        d.roofCol = 0xFF3A7CB8;
        out.add(d);
    }

    static int aqH(int st) {
        return Math.max(4, Math.min(st - 5, Math.round(st * 0.55f)));
    }

    /** A cartoon fish facing -u, s its half length. */
    static void fish(Canvas c, Paint p, float x, float y, float s, int col) {
        poly(c, p, 0x40000000, x + s * 0.6f + 1.5f, y + 1.5f, x + s * 1.25f + 1.5f, y - s * 0.55f + 1.5f, x + s * 1.25f + 1.5f, y + s * 0.55f + 1.5f);
        oval(c, p, 0x40000000, x - s + 1.5f, y - s * 0.55f + 1.5f, x + s * 0.75f + 1.5f, y + s * 0.55f + 1.5f);
        poly(c, p, City.darken(col, 0.8f), x + s * 0.6f, y, x + s * 1.25f, y - s * 0.55f, x + s * 1.1f, y, x + s * 1.25f, y + s * 0.55f);
        oval(c, p, col, x - s, y - s * 0.55f, x + s * 0.75f, y + s * 0.55f);
        oval(c, p, City.lighten(col, 0.25f), x - s * 0.8f, y - s * 0.45f, x + s * 0.4f, y);
        poly(c, p, City.darken(col, 0.85f), x - s * 0.1f, y - s * 0.5f, x + s * 0.3f, y - s * 0.85f, x + s * 0.4f, y - s * 0.45f);
        line(c, p, 0xFFF4F0E8, s * 0.12f, x - s * 0.25f, y - s * 0.45f, x - s * 0.25f, y + s * 0.45f);
        line(c, p, 0xFFF4F0E8, s * 0.12f, x + s * 0.25f, y - s * 0.4f, x + s * 0.25f, y + s * 0.4f);
        circle(c, p, 0xFFF4F0E8, x - s * 0.6f, y - s * 0.1f, s * 0.14f);
        circle(c, p, 0xFF1A1A1A, x - s * 0.63f, y - s * 0.1f, s * 0.07f);
    }

    // An indoor arena with a barrel-vault roof and an open-air rink beside it.
    static void iceRink(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Ice Rink Arena", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int lt = Math.max(bw, bh), st = Math.min(bw, bh);
                return orient(new int[][]{{1, 1, Math.max(5, lt / 2), st - 2, 2}}, bw, bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                float[] f = begin(c, x0, y0, x1, y1);
                float L = f[0], S = f[1], T = City.T;
                float au = (1 + Math.max(5, (int) (L / T) / 2)) * T;
                rect(c, p, 0xFFCCC8C0, 0, 0, L, S);
                for (float u = 0; u < L; u += 10) line(c, p, 0x12000000, 0.5f, u, 0, u, S);
                // The outdoor rink: boards, ice, the markings and the scratches of skates.
                float ru0 = au + 12, ru1 = L - 10, rv0 = 12, rv1 = S - 22;
                float rad = Math.min(ru1 - ru0, rv1 - rv0) * 0.3f;
                roundRect(c, p, 0x38000000, ru0 + 2 * SX, rv0 + 2 * SY, ru1 + 2 * SX, rv1 + 2 * SY, rad);
                roundRect(c, p, 0xFF2E4E7E, ru0, rv0, ru1, rv1, rad);
                roundRect(c, p, 0xFFF0F2EE, ru0 + 2, rv0 + 2, ru1 - 2, rv1 - 2, rad - 2);
                roundRect(c, p, 0xFFE2ECF2, ru0 + 6, rv0 + 6, ru1 - 6, rv1 - 6, rad - 5);
                float rcx = (ru0 + ru1) / 2, rcy = (rv0 + rv1) / 2;
                boolean along = ru1 - ru0 > rv1 - rv0;
                if (along) {
                    line(c, p, 0xB0D04040, 1, rcx, rv0 + 2, rcx, rv1 - 2);
                    line(c, p, 0xA04060C0, 1, ru0 + (ru1 - ru0) * 0.33f, rv0 + 2, ru0 + (ru1 - ru0) * 0.33f, rv1 - 2);
                    line(c, p, 0xA04060C0, 1, ru0 + (ru1 - ru0) * 0.67f, rv0 + 2, ru0 + (ru1 - ru0) * 0.67f, rv1 - 2);
                } else {
                    line(c, p, 0xB0D04040, 1, ru0 + 2, rcy, ru1 - 2, rcy);
                    line(c, p, 0xA04060C0, 1, ru0 + 2, rv0 + (rv1 - rv0) * 0.33f, ru1 - 2, rv0 + (rv1 - rv0) * 0.33f);
                    line(c, p, 0xA04060C0, 1, ru0 + 2, rv0 + (rv1 - rv0) * 0.67f, ru1 - 2, rv0 + (rv1 - rv0) * 0.67f);
                }
                ring(c, p, 0xB0D04040, 0.7f, rcx, rcy, Math.min(ru1 - ru0, rv1 - rv0) * 0.15f);
                for (int k = 0; k < 9; k++) {
                    float w = (ru1 - ru0) * (0.35f + r.nextFloat() * 0.5f), h = (rv1 - rv0) * (0.3f + r.nextFloat() * 0.5f);
                    float ox = (r.nextFloat() - 0.5f) * 10, oy = (r.nextFloat() - 0.5f) * 8;
                    strokeOval(c, p, 0x50A8BCC8, 0.4f, rcx - w / 2 + ox, rcy - h / 2 + oy, rcx + w / 2 + ox, rcy + h / 2 + oy);
                }
                for (int k = 0; k < 7; k++) {
                    double a = r.nextFloat() * 6.28;
                    float sx = rcx + (float) Math.cos(a) * (ru1 - ru0) * 0.36f, sy = rcy + (float) Math.sin(a) * (rv1 - rv0) * 0.32f;
                    circle(c, p, 0x40000000, sx + 0.8f, sy + 0.8f, 1.6f);
                    circle(c, p, BRIGHT[k % 8], sx, sy, 1.5f);
                    circle(c, p, 0xFFE8C8A8, sx, sy, 0.7f);
                }
                // Benches, a hot chocolate hut, lamps and fir trees along the front.
                float bv = S - 12;
                for (float u = ru0 + 6; u < ru1 - 6; u += 18) bench(c, p, u, bv, true);
                rect(c, p, 0x38000000, ru1 - 18 + 2, bv - 6 + 2, ru1 - 4 + 2, bv + 6 + 2);
                rect(c, p, 0xFF7A4A30, ru1 - 18, bv - 6, ru1 - 4, bv + 6);
                poly(c, p, 0xFF9A6040, ru1 - 18, bv - 6, ru1 - 4, bv - 6, ru1 - 11, bv);
                poly(c, p, 0xFF6A3E26, ru1 - 18, bv + 6, ru1 - 4, bv + 6, ru1 - 11, bv);
                for (float u = ru0 + 12; u < ru1 - 10; u += 30) lamp(c, p, u, rv0 - 6);
                tree(c, p, au + 6, S - 8, 4.5f, 0xFF2E5E3E);
                tree(c, p, L - 5, 6, 4.5f, 0xFF2E5E3E);
                end(c);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                float[] f = begin(c, x0, y0, x1, y1);
                float L = f[0], S = f[1];
                // A vault across the short side: shaded bands so it reads as curved.
                rect(c, p, 0xFF6A7078, 0, 0, L, S);
                float[] sh = {0.78f, 0.88f, 0.97f, 1.04f, 1.0f, 0.92f, 0.84f, 0.76f, 0.68f};
                int base = 0xFFB8C0C8;
                for (int k = 0; k < sh.length; k++) {
                    rect(c, p, shade(base, sh[k]), 1.5f, 1.5f + (S - 3) * k / sh.length, L - 1.5f, 1.5f + (S - 3) * (k + 1) / sh.length);
                }
                for (float u = 6; u < L - 3; u += 7) line(c, p, 0x28000000, 0.6f, u, 1.5f, u, S - 1.5f);
                rect(c, p, 0xFFA8D4E8, 4, S * 0.3f, L - 4, S * 0.38f);
                for (float u = 8; u < L - 4; u += 7) line(c, p, 0x80789AAA, 0.5f, u, S * 0.3f, u, S * 0.38f);
                // Big snowflake roundel.
                float cx = L / 2, cy = S * 0.66f, rr = Math.min(S, L) * 0.14f;
                circle(c, p, 0xFF2E4E7E, cx, cy, rr);
                for (int k = 0; k < 6; k++) {
                    double a = k * Math.PI / 3;
                    line(c, p, 0xFFF4F8FA, 1, cx, cy, cx + (float) Math.cos(a) * rr * 0.8f, cy + (float) Math.sin(a) * rr * 0.8f);
                }
                circle(c, p, 0xFFF4F8FA, cx, cy, rr * 0.18f);
                hvac(c, p, 4, S - 10, 8, 6);
                hvac(c, p, L - 12, S - 10, 8, 6);
                end(c);
                sign(city, "ICE ARENA", (x0 + x1) / 2, (y0 + y1) / 2, 7, 0xFFA8D4E8);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.MID | Variants.SUB | Variants.PARK;
        d.minS = 10;
        d.minL = 14;
        d.base = City.OFFICE;
        d.inside = Variants.I_GYM;
        d.roofCol = 0xFFB8C0C8;
        out.add(d);
    }

    // A go-kart circuit: a twisty track with kerbs and tyre walls, karts, a pit garage and a grandstand.
    static void goKarts(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Go-Kart Track", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int lt = Math.max(bw, bh), st = Math.min(bw, bh);
                return orient(new int[][]{{lt - 4, 1, 3, Math.min(6, st - 6), 1}}, bw, bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                float[] f = begin(c, x0, y0, x1, y1);
                float L = f[0], S = f[1], T = City.T;
                float gu = L - 4 * T, gh = Math.min(6, (int) (S / T) - 6) * T;
                rect(c, p, 0xFF6A9A4E, 0, 0, L, S);
                for (int k = 0; k < 12; k++) {
                    float x = r.nextFloat() * L, y = r.nextFloat() * S;
                    circle(c, p, 0x12FFFFFF, x, y, 4 + r.nextFloat() * 6);
                }
                // The track, a closed spline through these points in the left part.
                float tu0 = 12, tu1 = gu - 16, tv0 = 12, tv1 = S - 12;
                float[] cp = {0.08f, 0.12f, 0.55f, 0.08f, 0.92f, 0.16f, 0.95f, 0.45f, 0.7f, 0.42f, 0.52f, 0.58f, 0.88f, 0.74f, 0.9f, 0.92f,
                        0.35f, 0.94f, 0.06f, 0.82f, 0.28f, 0.6f, 0.05f, 0.42f};
                int n = cp.length / 2, steps = 6, m = n * steps;
                float[] xs = new float[m], ys = new float[m];
                for (int i = 0; i < n; i++) {
                    for (int s = 0; s < steps; s++) {
                        float t = s / (float) steps;
                        float px = cr(cp[((i - 1 + n) % n) * 2], cp[i * 2], cp[((i + 1) % n) * 2], cp[((i + 2) % n) * 2], t);
                        float py = cr(cp[((i - 1 + n) % n) * 2 + 1], cp[i * 2 + 1], cp[((i + 1) % n) * 2 + 1], cp[((i + 2) % n) * 2 + 1], t);
                        xs[i * steps + s] = tu0 + px * (tu1 - tu0);
                        ys[i * steps + s] = tv0 + py * (tv1 - tv0);
                    }
                }
                float w = Math.max(9, Math.min(13, Math.min(tu1 - tu0, tv1 - tv0) * 0.08f));
                p.setStrokeCap(Paint.Cap.ROUND);
                trackPath(c, p, xs, ys, 2 * SX, 2 * SY, 0x30000000, w + 4);
                trackPath(c, p, xs, ys, 0, 0, 0xFFD8342A, w + 3);
                for (int i = 0; i < m; i += 2) {
                    int j = (i + 1) % m;
                    line(c, p, 0xFFF2F0EA, w + 3, xs[i], ys[i], xs[j], ys[j]);
                }
                trackPath(c, p, xs, ys, 0, 0, 0xFF4E5054, w);
                trackPath(c, p, xs, ys, 0, 0, 0x10FFFFFF, w * 0.3f);
                p.setStrokeCap(Paint.Cap.BUTT);
                // Start line: a chequer strip across the first straight.
                float sx0 = xs[steps / 2 + 1], sy0 = ys[steps / 2 + 1];
                for (int k = -2; k < 2; k++) {
                    rect(c, p, 0xFFF2F0EA, sx0 - 1.5f, sy0 + k * w / 4, sx0, sy0 + (k + 1) * w / 4);
                    rect(c, p, 0xFF1A1A1A, sx0, sy0 + k * w / 4, sx0 + 1.5f, sy0 + (k + 1) * w / 4);
                }
                // Tyre walls on the outside of the hairpins.
                int[] hp = {2, 3, 7, 9};
                for (int h : hp) {
                    float px = tu0 + cp[h * 2] * (tu1 - tu0), py = tv0 + cp[h * 2 + 1] * (tv1 - tv0);
                    float ox = px - (tu0 + tu1) / 2, oy = py - (tv0 + tv1) / 2, ol = (float) Math.sqrt(ox * ox + oy * oy) + 0.01f;
                    float bx = px + ox / ol * (w + 4), by = py + oy / ol * (w + 4);
                    for (int k = -3; k <= 3; k++) {
                        float tx = bx - oy / ol * k * 3, ty = by + ox / ol * k * 3;
                        circle(c, p, 0xFF1E1E20, tx, ty, 1.6f);
                        circle(c, p, k % 2 == 0 ? 0xFFD8342A : 0xFF3A3A3E, tx, ty, 0.7f);
                    }
                }
                // Karts.
                for (int k = 0; k < 6; k++) {
                    int i = (k * 13 + 5) % m, j = (i + 1) % m;
                    double a = Math.atan2(ys[j] - ys[i], xs[j] - xs[i]);
                    float ox = (k % 2 == 0 ? 1 : -1) * w * 0.2f;
                    float kx = xs[i] - (float) Math.sin(a) * ox, ky = ys[i] + (float) Math.cos(a) * ox;
                    rbox(c, p, 0x50000000, kx + 0.8f * SX, ky + 0.8f * SY, 2.8f, 1.9f, a);
                    rbox(c, p, 0xFF1A1A1A, kx, ky, 2.6f, 1.9f, a);
                    rbox(c, p, BRIGHT[k % 8], kx, ky, 2.2f, 1.2f, a);
                    circle(c, p, 0xFFF2F0EA, kx - (float) Math.cos(a) * 0.4f, ky - (float) Math.sin(a) * 0.4f, 0.9f);
                }
                // The pit apron in front of the garage and the grandstand below it.
                rect(c, p, 0xFF6A6C70, gu - 10, 10, gu - 1, gh + 22);
                for (float v = 14; v < gh + 20; v += 8) line(c, p, 0x60FFFFFF, 0.5f, gu - 9, v, gu - 2, v);
                float g0 = gh + 30, g1 = S - 8;
                if (g1 - g0 > 14) {
                    rect(c, p, 0x40000000, gu + 2 * SX, g0 + 2 * SY, L - 8 + 2 * SX, g1 + 2 * SY);
                    int rowsN = (int) ((L - 8 - gu) / 6);
                    for (int k = 0; k < rowsN; k++) {
                        float u = gu + k * 6;
                        rect(c, p, shade(0xFF9A9EA4, 0.85f + k * 0.06f), u, g0, u + 6, g1);
                        rect(c, p, k % 3 == 0 ? 0xFFD8342A : 0xFF3A6AB8, u + 1, g0 + 2, u + 3.2f, g1 - 2);
                        for (float v = g0 + 5; v < g1 - 2; v += 4) line(c, p, 0x50000000, 0.5f, u + 1, v, u + 3.2f, v);
                    }
                    rect(c, p, 0xFF5A5E64, L - 9, g0, L - 7, g1);
                }
                lamp(c, p, gu - 12, S / 2);
                end(c);
                sign(city, "KARTING", (x0 + x1) / 2, (y0 + y1) / 2, 7, 0xFFF2F0EA);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                float[] f = begin(c, x0, y0, x1, y1);
                float L = f[0], S = f[1];
                rect(c, p, 0xFF5A5E64, 0, 0, L, S);
                rect(c, p, 0xFF8A9098, 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                for (float v = 4; v < S - 2; v += 3) line(c, p, 0x20000000, 0.5f, 2, v, L - 2, v);
                // A chequered band across and the garage bays' roller-door hoods.
                for (int k = 0; k * 3 < L - 3; k++) {
                    rect(c, p, k % 2 == 0 ? 0xFF1A1A1A : 0xFFF2F0EA, 1.5f + k * 3, S * 0.4f, 1.5f + (k + 1) * 3, S * 0.4f + 3);
                    rect(c, p, k % 2 == 1 ? 0xFF1A1A1A : 0xFFF2F0EA, 1.5f + k * 3, S * 0.4f + 3, 1.5f + (k + 1) * 3, S * 0.4f + 6);
                }
                for (float u = 4; u < L - 6; u += 12) rect(c, p, 0xFFD8342A, u, S - 3, u + 8, S - 1);
                end(c);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.SUB | Variants.IND;
        d.minS = 12;
        d.minL = 14;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_SHOWROOM;
        d.roofCol = 0xFF8A9098;
        out.add(d);
    }

    static void trackPath(Canvas c, Paint p, float[] xs, float[] ys, float dx, float dy, int col, float w) {
        Path path = new Path();
        path.moveTo(xs[0] + dx, ys[0] + dy);
        for (int i = 1; i < xs.length; i++) path.lineTo(xs[i] + dx, ys[i] + dy);
        path.close();
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(w);
        p.setColor(col);
        c.drawPath(path, p);
        p.setStyle(Paint.Style.FILL);
    }

    static float cr(float p0, float p1, float p2, float p3, float t) {
        return 0.5f * (2 * p1 + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t + (-p0 + 3 * p1 - 3 * p2 + p3) * t * t * t);
    }

    // A grand corner department store: a mansard roof, a glass atrium dome, a rooftop café and striped awnings.
    static void departmentStore(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Department Store", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return new int[][]{{1, 1, bw - 2, bh - 2, 5}};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                float T = City.T;
                rect(c, p, 0xFFD2CCC0, x0, y0, x1, y1);
                for (float x = x0; x < x1; x += 12) line(c, p, 0x14000000, 0.5f, x, y0, x, y1);
                for (float y = y0; y < y1; y += 12) line(c, p, 0x14000000, 0.5f, x0, y, x1, y);
                // Awnings over the shop windows on all four sides.
                int ca = 0xFF2E5E46, cb = 0xFFEDE4CC;
                float bx0 = x0 + T, by0 = y0 + T, bx1 = x1 - T, by1 = y1 - T;
                for (float x = bx0 + 6; x + 18 <= bx1 - 6; x += 24) {
                    awning(c, p, x, by1, x + 18, by1 + 5, true, ca, cb);
                    awning(c, p, x, by0 - 5, x + 18, by0, true, ca, cb);
                }
                for (float y = by0 + 6; y + 18 <= by1 - 6; y += 24) {
                    awning(c, p, bx1, y, bx1 + 5, y + 18, false, ca, cb);
                    awning(c, p, bx0 - 5, y, bx0, y + 18, false, ca, cb);
                }
                // Lamps and small trees at the corners, flower tubs by the doors.
                lamp(c, p, x0 + 4, y0 + 4);
                lamp(c, p, x1 - 4, y0 + 4);
                lamp(c, p, x0 + 4, y1 - 4);
                lamp(c, p, x1 - 4, y1 - 4);
                float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
                bush(c, p, cx - 12, y1 - 3, 2.2f);
                bush(c, p, cx + 12, y1 - 3, 2.2f);
                bush(c, p, x1 - 3, cy - 12, 2.2f);
                bush(c, p, x1 - 3, cy + 12, 2.2f);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                float[] f = begin(c, x0, y0, x1, y1);
                float L = f[0], S = f[1], m = 7;
                // Mansard slopes with dormers, a stone cornice and a gravel deck.
                rect(c, p, 0xFFC8BEA8, 0, 0, L, S);
                rect(c, p, 0xFF4A5462, 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                poly(c, p, 0xFF5A6474, 1.5f, 1.5f, L - 1.5f, 1.5f, L - m, m, m, m);
                poly(c, p, 0xFF3E4652, 1.5f, S - 1.5f, L - 1.5f, S - 1.5f, L - m, S - m, m, S - m);
                poly(c, p, 0xFF525C6A, 1.5f, 1.5f, 1.5f, S - 1.5f, m, S - m, m, m);
                poly(c, p, 0xFF444E5A, L - 1.5f, 1.5f, L - 1.5f, S - 1.5f, L - m, S - m, L - m, m);
                for (float u = 12; u < L - 12; u += 10) {
                    rect(c, p, 0xFFD8D0BC, u, 2, u + 3.5f, 5);
                    rect(c, p, 0xFFD8D0BC, u, S - 5, u + 3.5f, S - 2);
                }
                for (float v = 12; v < S - 12; v += 10) {
                    rect(c, p, 0xFFD8D0BC, 2, v, 5, v + 3.5f);
                    rect(c, p, 0xFFD8D0BC, L - 5, v, L - 2, v + 3.5f);
                }
                rect(c, p, 0xFFA8A296, m, m, L - m, S - m);
                // The atrium dome.
                float dr = Math.min(S * 0.28f, L * 0.2f), dx = Math.max(m + dr + 6, L * 0.34f), dy = S * 0.5f;
                circle(c, p, 0x40000000, dx + 2 * SX, dy + 2 * SY, dr + 2);
                circle(c, p, 0xFFC8BEA8, dx, dy, dr + 2);
                circle(c, p, 0xFF7AA4B4, dx, dy, dr);
                circle(c, p, 0xFF9CC4D0, dx - dr * 0.12f, dy - dr * 0.12f, dr * 0.82f);
                circle(c, p, 0xFFB8DAE4, dx - dr * 0.35f, dy - dr * 0.35f, dr * 0.35f);
                ring(c, p, 0xB04E6470, 0.5f, dx, dy, dr * 0.66f);
                ring(c, p, 0xB04E6470, 0.5f, dx, dy, dr * 0.33f);
                for (int k = 0; k < 16; k++) {
                    double a = k * Math.PI / 8;
                    line(c, p, 0xC04E6470, 0.6f, dx, dy, dx + (float) Math.cos(a) * dr, dy + (float) Math.sin(a) * dr);
                }
                circle(c, p, 0xFFC8A048, dx, dy, dr * 0.12f);
                // The rooftop café terrace: a deck, tables, a glass rail and planters.
                float tu0 = Math.max(dx + dr + 10, L - m - 2 - 96), tu1 = L - m - 2, tv0 = m + 2, tv1 = S - m - 2;
                if (tu0 - (dx + dr) > 30) {
                    // A glazed ridge skylight over the store's central hall.
                    float g0 = dx + dr + 8, g1 = tu0 - 10, gv = S * 0.5f;
                    rect(c, p, 0x40000000, g0 + 2 * SX, gv - 7 + 2 * SY, g1 + 2 * SX, gv + 7 + 2 * SY);
                    rect(c, p, 0xFF8A8E90, g0, gv - 7, g1, gv + 7);
                    rect(c, p, 0xFF9CC4D0, g0 + 1, gv - 6, g1 - 1, gv);
                    rect(c, p, 0xFF7AA4B4, g0 + 1, gv, g1 - 1, gv + 6);
                    for (float u = g0 + 4; u < g1 - 1; u += 4) line(c, p, 0xA04E6470, 0.5f, u, gv - 6, u, gv + 6);
                }
                if (tu1 - tu0 > 24) {
                    rect(c, p, 0xFFA8805A, tu0, tv0, tu1, tv1);
                    for (float v = tv0 + 2.5f; v < tv1; v += 2.5f) line(c, p, 0x22000000, 0.4f, tu0, v, tu1, v);
                    frame(c, p, 0x90C8E4EC, 0.8f, tu0, tv0, tu1, tv1);
                    int k = 0;
                    for (float v = tv0 + 11; v < tv1 - 7; v += 22) {
                        for (float u = tu0 + 11; u < tu1 - 7; u += 22) {
                            parasol(c, p, u, v, 5, k % 2 == 0 ? 0xFF2E5E46 : 0xFFC8503A, 0xFFEDE4CC);
                            k++;
                        }
                    }
                    for (float v = tv0 + 4; v < tv1 - 2; v += 9) bush(c, p, tu1 - 2, v, 2.2f);
                } else {
                    hvac(c, p, L - m - 16, m + 4, 10, 8);
                }
                for (float v = m + 6; v < S - m - 8; v += 14) hvac(c, p, m + 3, v, 8, 7);
                // The corner rotunda with its own little dome and a flag.
                float rr = Math.min(12, S * 0.12f);
                circle(c, p, 0x40000000, L - rr - 1 + 2 * SX, S - rr - 1 + 2 * SY, rr);
                circle(c, p, 0xFFC8BEA8, L - rr - 1, S - rr - 1, rr);
                circle(c, p, 0xFF4E8A78, L - rr - 1, S - rr - 1, rr * 0.8f);
                circle(c, p, 0xFF6AA894, L - rr - 2, S - rr - 2, rr * 0.45f);
                circle(c, p, 0xFFE0C060, L - rr - 1, S - rr - 1, 1.2f);
                end(c);
                sign(city, "DEPARTMENT STORE", (x0 + x1) / 2, (y0 + y1) / 2, 7, 0xFFE0C060);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.MID;
        d.minS = 10;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_SHELVES;
        d.roofCol = 0xFF4A5462;
        out.add(d);
    }

    static void awning(Canvas c, Paint p, float x0, float y0, float x1, float y1, boolean across, int ca, int cb) {
        rect(c, p, 0x30000000, x0 + 1, y0 + 1.5f, x1 + 1, y1 + 1.5f);
        if (across) {
            for (int k = 0; k < 6; k++) rect(c, p, k % 2 == 0 ? ca : cb, x0 + (x1 - x0) * k / 6, y0, x0 + (x1 - x0) * (k + 1) / 6, y1);
        } else {
            for (int k = 0; k < 6; k++) rect(c, p, k % 2 == 0 ? ca : cb, x0, y0 + (y1 - y0) * k / 6, x1, y0 + (y1 - y0) * (k + 1) / 6);
        }
    }

    // A chrome 1950s diner with old gas pumps under a canopy, a neon pylon and classic cars.
    static void diner(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Retro Diner", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int lt = Math.max(bw, bh);
                return orient(new int[][]{{1, 1, dinerW(lt), 3, 1}}, bw, bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                float[] f = begin(c, x0, y0, x1, y1);
                float L = f[0], S = f[1], T = City.T;
                float du1 = (1 + dinerW((int) (L / T))) * T;
                float fv1 = Math.min(S - 18, 4 * T + 40);
                rect(c, p, 0xFF6E9A4E, 0, 0, L, S);
                rect(c, p, 0xFF5E5E5C, 6, 6, L - 6, fv1);
                for (int k = 0; k < 12; k++) {
                    float x = 8 + r.nextFloat() * (L - 16), y = 8 + r.nextFloat() * (fv1 - 14);
                    circle(c, p, 0x14000000, x, y, 1.5f + r.nextFloat() * 4);
                }
                // A checkerboard terrace along the diner's front.
                for (int k = 0; (k + 1) * 4 <= du1 - T; k++) {
                    for (int j = 0; j < 2; j++) rect(c, p, (k + j) % 2 == 0 ? 0xFFEFEAE0 : 0xFF2A2A2A, T + k * 4, 4 * T + 2 + j * 4, T + k * 4 + 4, 4 * T + 6 + j * 4);
                }
                // The gas canopy: pump islands peeking out from under a white roof with a red band.
                float cu0 = du1 + 18, cu1 = Math.min(L - 12, cu0 + 64), cv0 = 16, cv1 = Math.min(fv1 - 16, 16 + 38);
                if (cu1 - cu0 > 26) {
                    for (float u = cu0 + 8; u < cu1 - 4; u += 16) {
                        rect(c, p, 0xFFB8B4AC, u - 3, cv0 - 6, u + 3, cv1 + 6);
                        rect(c, p, 0xFFC8342A, u - 2, cv0 - 5, u + 2, cv0 - 1);
                        rect(c, p, 0xFFC8342A, u - 2, cv1 + 1, u + 2, cv1 + 5);
                        circle(c, p, 0xFFF2F0E8, u, cv0 - 3, 1);
                        circle(c, p, 0xFFF2F0E8, u, cv1 + 3, 1);
                    }
                    rect(c, p, 0x48000000, cu0 + 5 * SX, cv0 + 5 * SY, cu1 + 5 * SX, cv1 + 5 * SY);
                    rect(c, p, 0xFFE8E6E0, cu0, cv0, cu1, cv1);
                    rect(c, p, 0xFFF6F4F0, cu0 + 1, cv0 + 1, cu1 - 1, (cv0 + cv1) / 2);
                    rect(c, p, 0xFFC8342A, cu0, cv1 - 3, cu1, cv1);
                    rect(c, p, 0xFFC8342A, cu0, cv0, cu1, cv0 + 2);
                    circle(c, p, 0xFFC8342A, (cu0 + cu1) / 2, (cv0 + cv1) / 2, 5);
                    circle(c, p, 0xFFF6F4F0, (cu0 + cu1) / 2, (cv0 + cv1) / 2, 3.2f);
                    car(c, p, cu0 + 16, cv1 + 13, 0, 0xFF5AB8B0, 5.2f);
                }
                // Classic cars angle-parked in front of the diner.
                int[] cc = {0xFFE07A9A, 0xFFE8D8B0, 0xFFC83A32, 0xFF5AB8B0};
                float pv = fv1 - 10;
                for (int k = 0; k < 4; k++) {
                    float u = T + 8 + k * 16;
                    if (u + 8 > du1 + 8) break;
                    line(c, p, 0xB0F2F0E8, 0.6f, u - 6, pv - 7, u - 2, pv + 7);
                    if (k != 2) car(c, p, u + 2, pv, -Math.PI * 0.62, cc[k], 5.2f);
                }
                // The lawn by the road: a picnic table or two and trees.
                float lv = (fv1 + S) / 2;
                if (S - fv1 > 40) {
                    tree(c, p, 14, lv, 7);
                    tree(c, p, L * 0.7f, lv - 8, 6);
                    for (float u = 30; u < L - 30; u += 26) tree(c, p, u, S - 9, 4.5f);
                    rect(c, p, 0xFFB8B0A0, L * 0.22f, lv - 9, L * 0.6f, lv + 15);
                    for (float u = L * 0.22f + 4; u < L * 0.6f; u += 6) line(c, p, 0x14000000, 0.4f, u, lv - 9, u, lv + 15);
                    picnic(c, p, L * 0.3f, lv, 0xFFC83A32, 0xFFF2F0E8);
                    picnic(c, p, L * 0.5f, lv + 6, 0xFF5AB8B0, 0xFFF2F0E8);
                }
                for (float u = 10; u < L - 26; u += 12) bush(c, p, u, fv1 + 4, 2.6f);
                // The neon pylon on the corner, with its shadow.
                float sx = L - 14, sy = S - 13;
                rect(c, p, 0x40000000, sx - 9 + 6, sy - 4 + 6, sx + 9 + 6, sy + 4 + 6);
                roundRect(c, p, 0xFF2A2A3A, sx - 9, sy - 4, sx + 9, sy + 4, 4);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(0.9f);
                p.setColor(0xFFFF5AA8);
                RF.set(sx - 8, sy - 3, sx + 8, sy + 3);
                c.drawRoundRect(RF, 3, 3, p);
                p.setStyle(Paint.Style.FILL);
                line(c, p, 0xFF5AF0FF, 0.8f, sx - 5, sy, sx + 5, sy);
                circle(c, p, 0xFFFFE070, sx - 9, sy - 4, 1.6f);
                end(c);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                float[] f = begin(c, x0, y0, x1, y1);
                float L = f[0], S = f[1], rad = S * 0.45f;
                rect(c, p, 0xFF5E5E5C, 0, 0, L, S);
                roundRect(c, p, 0x40000000, 2, 2.5f, L + 1, S + 1, rad);
                roundRect(c, p, 0xFF8A9096, 0, 0, L, S, rad);
                roundRect(c, p, 0xFFC4CAD0, 1.2f, 1.2f, L - 1.2f, S - 1.2f, rad - 1);
                roundRect(c, p, 0xFFE4E8EC, 3, 2.5f, L - 3, S * 0.42f, rad * 0.6f);
                for (float u = rad; u < L - rad; u += 3) line(c, p, 0x18000000, 0.4f, u, 2, u, S - 4);
                // Red and turquoise bands along the front.
                roundRect(c, p, 0xFFC8342A, 2, S - 6, L - 2, S - 3, 1.5f);
                line(c, p, 0xFF4AC0B8, 0.8f, 4, S - 7, L - 4, S - 7);
                // Roof vents and the neon script box.
                for (float u = rad + 2; u < L - rad - 2; u += 14) {
                    circle(c, p, 0x40000000, u + 0.8f, S * 0.6f + 0.8f, 2);
                    circle(c, p, 0xFF9AA0A6, u, S * 0.6f, 2);
                    circle(c, p, 0xFF6A7076, u, S * 0.6f, 1);
                }
                float sw = Math.min(L * 0.4f, 34);
                roundRect(c, p, 0xFF2A2A3A, L / 2 - sw / 2, S * 0.25f, L / 2 + sw / 2, S * 0.55f, 2);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(0.8f);
                p.setColor(0xFFFF5AA8);
                RF.set(L / 2 - sw / 2 + 1.2f, S * 0.25f + 1.2f, L / 2 + sw / 2 - 1.2f, S * 0.55f - 1.2f);
                c.drawRoundRect(RF, 1.5f, 1.5f, p);
                p.setStyle(Paint.Style.FILL);
                for (float u = L / 2 - sw / 2 + 4; u < L / 2 + sw / 2 - 3; u += 4) line(c, p, 0xFF5AF0FF, 0.7f, u, S * 0.33f, u + 2, S * 0.47f);
                end(c);
                sign(city, "DINER", (x0 + x1) / 2, (y0 + y1) / 2, 7, 0xFFFF5AA8);
            }
        };
        d.ground = City.LOT;
        d.districts = Variants.SUB | Variants.MID | Variants.IND;
        d.minS = 8;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_RESTAURANT;
        d.roofCol = 0xFFC4CAD0;
        out.add(d);
    }

    static int dinerW(int lt) {
        return Math.max(5, Math.min(8, lt / 2 - 1));
    }

    // A night market: rows of tented stalls in many colours, lanterns strung over the walkways.
    static void nightMarket(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Night Market", T_PARK) {
            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                sign(city, "NIGHT MARKET", (x0 + x1) / 2, (y0 + y1) / 2, 7, 0xFFF0A040);
                float[] f = begin(c, x0, y0, x1, y1);
                float L = f[0], S = f[1];
                rect(c, p, 0xFF7E7A74, 0, 0, L, S);
                for (int k = 0; k < 16; k++) {
                    float x = r.nextFloat() * L, y = r.nextFloat() * S;
                    rect(c, p, 0x12000000, x, y, x + 6, y + 3);
                }
                int[] tc = {0xFFC83A32, 0xFFE8A030, 0xFF2E7A6A, 0xFF3A5AA8, 0xFF8A3A7A, 0xFFE0D8C0, 0xFF4A8A3A, 0xFFD06A3A};
                // Rows of stalls back to back, walkways between them.
                float sd = Math.max(13, Math.min(20, L / 18)), walk = sd * 1.3f, v = 8;
                int rows = 0;
                float[] walks = new float[8];
                int nw = 0;
                while (v + sd * 2 <= S - 6 && rows < 4) {
                    for (int side = 0; side < 2; side++) {
                        float sv = v + side * sd;
                        int slot = 0;
                        for (float u = 22; u + sd <= L - 8; u += sd + 1, slot++) {
                            if (slot % 6 == 5) continue;
                            int col = tc[r.nextInt(tc.length)];
                            if (r.nextInt(5) == 0) booth(c, p, u, sv, sd, col);
                            else tent(c, p, u, sv, u + sd, sv + sd, col);
                        }
                    }
                    v += sd * 2;
                    if (nw < walks.length) walks[nw++] = v + walk / 2;
                    v += walk;
                    rows++;
                }
                if (nw > 0 && walks[nw - 1] > S - 6) nw--;
                // Lanterns zigzag over the walkways; little tables and crowds of crates.
                for (int w = 0; w < nw; w++) {
                    float wv = walks[w];
                    for (float u = 22; u + sd * 2 <= L - 6; u += sd * 2) {
                        lanterns(c, p, u, wv - walk / 2 + 1, u + sd * 2, wv + walk / 2 - 1);
                    }
                    for (int k = 0; k < 4; k++) {
                        float u = 30 + r.nextFloat() * (L - 50);
                        rect(c, p, 0xFF8A6A48, u, wv - 1.5f, u + 3, wv + 1.5f);
                    }
                }
                // The entrance gate at the near end: red posts and a beam with a lantern.
                float gv0 = 10, gv1 = S - 10;
                rect(c, p, 0x40000000, 10, gv0 + 2, 16, gv1 + 2);
                rect(c, p, 0xFFB8302A, 8, gv0, 14, gv1);
                rect(c, p, 0xFFD8443A, 9, gv0, 12, gv1);
                rect(c, p, 0xFF2A2A2A, 6, gv0 - 2, 16, gv0 + 3);
                rect(c, p, 0xFF2A2A2A, 6, gv1 - 3, 16, gv1 + 2);
                for (float vv = gv0 + 10; vv < gv1 - 6; vv += 12) {
                    circle(c, p, 0x60FFB040, 11, vv, 2.6f);
                    circle(c, p, 0xFFE8302A, 11, vv, 1.6f);
                }
                end(c);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.OLD | Variants.MID;
        d.minS = 8;
        d.minL = 10;
        d.roofCol = 0xFFC83A32;
        out.add(d);
    }

    static void lanterns(Canvas c, Paint p, float ax, float ay, float bx, float by) {
        line(c, p, 0x80202020, 0.35f, ax, ay, bx, by);
        line(c, p, 0x80202020, 0.35f, ax, by, bx, ay);
        for (int k = 1; k < 4; k++) {
            float t = k / 4f;
            float x = ax + (bx - ax) * t;
            circle(c, p, k % 2 == 0 ? 0xFFE8302A : 0xFFF0A030, x, ay + (by - ay) * t, 1.4f);
            circle(c, p, k % 2 == 1 ? 0xFFE8302A : 0xFFF0A030, x, by + (ay - by) * t, 1.4f);
        }
    }

    // A tennis club: clay and hard courts behind fences, a clubhouse with a veranda and a viewing terrace.
    static void tennis(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Tennis Club", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int st = Math.min(bw, bh);
                return orient(new int[][]{{1, 1, 4, Math.min(4, st - 5), 2}}, bw, bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                float[] f = begin(c, x0, y0, x1, y1);
                float L = f[0], S = f[1], T = City.T;
                float ch = Math.min(4, (int) (S / T) - 5) * T;
                rect(c, p, 0xFF5E9A48, 0, 0, L, S);
                for (int k = 0; k < 10; k++) {
                    float x = r.nextFloat() * L, y = r.nextFloat() * S;
                    circle(c, p, 0x10FFFFFF, x, y, 5 + r.nextFloat() * 5);
                }
                // Veranda and terrace: a deck along the clubhouse and a paved terrace in front with parasols.
                rect(c, p, 0xFF9A7650, 5 * T, T - 2, 5 * T + 10, T + ch + 2);
                for (float v = T; v < T + ch; v += 2.5f) line(c, p, 0x22000000, 0.4f, 5 * T, v, 5 * T + 10, v);
                float tv0 = T + ch + 4, tv1 = S - 8;
                if (tv1 - tv0 > 12) {
                    rect(c, p, 0xFFD4CAB4, T - 4, tv0, 5 * T + 10, tv1);
                    for (float u = T - 4; u < 5 * T + 10; u += 6) line(c, p, 0x14000000, 0.4f, u, tv0, u, tv1);
                    int k = 0;
                    for (float v = tv0 + 10; v < tv1 - 5; v += 22) {
                        for (float u = T + 8; u < 5 * T + 4; u += 22) {
                            parasol(c, p, u, v, 5, k % 2 == 0 ? 0xFF2E6A4A : 0xFFE8E2D0, k % 2 == 0 ? 0xFFE8E2D0 : 0xFF2E6A4A);
                            k++;
                        }
                    }
                    for (float u = T - 2; u < 5 * T + 10; u += 7) bush(c, p, u, tv1 + 2, 2.2f);
                }
                // The courts.
                float cu0 = 5 * T + 16, cu1 = L - 6, cv0 = 6, cv1 = S - 6;
                float cw = 46, cl = Math.min(100, cv1 - cv0);
                int cols = Math.max(1, (int) ((cu1 - cu0 + 4) / (cw + 4)));
                int rowsN = Math.max(1, (int) ((cv1 - cv0 + 4) / (cl + 4)));
                rowsN = Math.min(rowsN, 2);
                float offU = cu0 + ((cu1 - cu0) - (cols * (cw + 4) - 4)) / 2;
                float offV = cv0 + ((cv1 - cv0) - (rowsN * (cl + 4) - 4)) / 2;
                for (int rr = 0; rr < rowsN; rr++) {
                    for (int cc = 0; cc < cols; cc++) {
                        float u = offU + cc * (cw + 4), v = offV + rr * (cl + 4);
                        court(c, p, u, v, u + cw, v + cl, (cc + rr) % 3 != 2);
                    }
                }
                frame(c, p, 0xFF2E4A36, 0.9f, offU - 1, offV - 1, offU + cols * (cw + 4) - 3, offV + rowsN * (cl + 4) - 3);
                tree(c, p, 6, S - 6, 5);
                end(c);
                sign(city, "TENNIS", (x0 + x1) / 2, (y0 + y1) / 2, 6, 0xFFF2F0EA);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                hipRoof(c, p, x0, y0, x1, y1, 0xFF3E6A50);
                float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
                rect(c, p, 0xFFE8E2D0, cx - 3, cy - 3, cx + 3, cy + 3);
                rect(c, p, 0xFFB8B0A0, cx - 3, cy + 1.5f, cx + 3, cy + 3);
                line(c, p, 0xFF3A3C40, 0.5f, cx + 6, y0 + 3, cx + 6, y0 + 8);
                poly(c, p, 0xFF2E6A4A, cx + 6, y0 + 3, cx + 10, y0 + 4.5f, cx + 6, y0 + 6);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.SUB | Variants.UNI | Variants.PARK;
        d.minS = 10;
        d.minL = 12;
        d.base = City.OFFICE;
        d.inside = Variants.I_CAFE;
        d.roofCol = 0xFF3E6A50;
        out.add(d);
    }

    /** A tennis court, long along v: clay (terracotta) or hard (green surround, blue court). */
    static void court(Canvas c, Paint p, float u0, float v0, float u1, float v1, boolean hard) {
        int sur = hard ? 0xFF4E7E5C : 0xFFB8603C, in = hard ? 0xFF3E6A9A : 0xFFC8704A;
        rect(c, p, sur, u0, v0, u1, v1);
        float w = u1 - u0, l = v1 - v0;
        float a0 = u0 + w * 0.14f, a1 = u1 - w * 0.14f, b0 = v0 + l * 0.12f, b1 = v1 - l * 0.12f;
        rect(c, p, in, a0, b0, a1, b1);
        int wl = 0xFFF2F0EA;
        frame(c, p, wl, 0.6f, a0, b0, a1, b1);
        float s0 = a0 + (a1 - a0) * 0.125f, s1 = a1 - (a1 - a0) * 0.125f, mid = (b0 + b1) / 2, cu = (a0 + a1) / 2;
        line(c, p, wl, 0.5f, s0, b0, s0, b1);
        line(c, p, wl, 0.5f, s1, b0, s1, b1);
        line(c, p, wl, 0.5f, s0, mid - (b1 - b0) * 0.27f, s1, mid - (b1 - b0) * 0.27f);
        line(c, p, wl, 0.5f, s0, mid + (b1 - b0) * 0.27f, s1, mid + (b1 - b0) * 0.27f);
        line(c, p, wl, 0.5f, cu, mid - (b1 - b0) * 0.27f, cu, mid + (b1 - b0) * 0.27f);
        // The net with its shadow and posts.
        line(c, p, 0x40000000, 1.2f, a0 - 2 + SX * 1.5f, mid + SY * 1.5f, a1 + 2 + SX * 1.5f, mid + SY * 1.5f);
        line(c, p, 0xFF2A2A2A, 0.9f, a0 - 2, mid, a1 + 2, mid);
        line(c, p, 0xFFF2F0EA, 0.4f, a0 - 2, mid - 0.5f, a1 + 2, mid - 0.5f);
        circle(c, p, 0xFF3A3C40, a0 - 2, mid, 0.9f);
        circle(c, p, 0xFF3A3C40, a1 + 2, mid, 0.9f);
        frame(c, p, 0xFF2E4A36, 0.7f, u0, v0, u1, v1);
    }

    // A circus: the striped big top with its poles and pennants, little tents, caravans and a ring of flags.
    static void circus(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Circus Big Top", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int lt = Math.max(bw, bh), st = Math.min(bw, bh);
                int[] t = topSize(lt, st);
                return orient(new int[][]{{t[0], t[1], t[2], t[3], 2}}, bw, bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                float[] f = begin(c, x0, y0, x1, y1);
                float L = f[0], S = f[1], T = City.T;
                int[] t = topSize((int) (L / T), (int) (S / T));
                float tu0 = t[0] * T, tv0 = t[1] * T, tu1 = tu0 + t[2] * T, tv1 = tv0 + t[3] * T;
                float tcx = (tu0 + tu1) / 2, tcy = (tv0 + tv1) / 2;
                rect(c, p, 0xFF6A9A4A, 0, 0, L, S);
                for (int k = 0; k < 16; k++) {
                    float x = r.nextFloat() * L, y = r.nextFloat() * S;
                    circle(c, p, 0x12FFFFFF, x, y, 3 + r.nextFloat() * 6);
                }
                // Sawdust round the tent and a path in from the near end.
                oval(c, p, 0xFFC8A870, tu0 - 10, tv0 - 10, tu1 + 10, tv1 + 10);
                rect(c, p, 0xFFC8A870, 0, tcy - 6, tu0, tcy + 6);
                // Guy ropes and pegs.
                for (int k = 0; k < 16; k++) {
                    double a = k * Math.PI / 8;
                    float rx = (tu1 - tu0) / 2, ry = (tv1 - tv0) / 2;
                    float ex = tcx + (float) Math.cos(a) * (rx + 8), ey = tcy + (float) Math.sin(a) * (ry + 8);
                    line(c, p, 0x80F0E8D8, 0.4f, tcx + (float) Math.cos(a) * rx * 0.95f, tcy + (float) Math.sin(a) * ry * 0.95f, ex, ey);
                    circle(c, p, 0xFF5A4A3A, ex, ey, 0.6f);
                }
                // The ring of flag poles.
                for (int k = 0; k < 20; k++) {
                    double a = k * Math.PI / 10;
                    float rx = (tu1 - tu0) / 2 + 13, ry = (tv1 - tv0) / 2 + 13;
                    float fx = tcx + (float) Math.cos(a) * rx, fy = tcy + (float) Math.sin(a) * ry;
                    if (fx < 3 || fy < 3 || fx > L - 3 || fy > S - 3) continue;
                    circle(c, p, 0x50000000, fx + SX, fy + SY, 0.8f);
                    circle(c, p, 0xFF4A4C50, fx, fy, 0.7f);
                    poly(c, p, BRIGHT[k % 6], fx, fy - 0.5f, fx + 4, fy + 0.6f, fx, fy + 1.6f);
                }
                // Small tents and caravans in the free end.
                float fu0 = tu1 + 18, fu1 = L - 6;
                if (fu1 - fu0 > 18) {
                    float rt = Math.min(10, (fu1 - fu0) * 0.3f);
                    roundTent(c, p, fu0 + rt, 6 + rt + 4, rt, rt, 12, 0xFF3A6AB8, 0xFFF2E8D0, 2);
                    circle(c, p, 0xFFE8B83A, fu0 + rt, 10 + rt, 1.2f);
                    if (fu1 - fu0 > 3 * rt + 6) {
                        roundTent(c, p, fu1 - rt, S - rt - 10, rt, rt, 12, 0xFFE8B83A, 0xFFC83A32, 2);
                        circle(c, p, 0xFF3A6AB8, fu1 - rt, S - rt - 10, 1.2f);
                    }
                    int[] vc = {0xFF2E8A7A, 0xFFC8503A, 0xFFE0C060, 0xFF6A4A9A};
                    int k = 0;
                    for (float v = 6 + 2 * rt + 12; v + 9 < S - 2 * rt - 14; v += 13) {
                        caravan(c, p, fu0 + 2, v, Math.min(26, fu1 - fu0 - 4), 8, vc[k % 4]);
                        k++;
                    }
                }
                // A ticket booth and light strings by the entrance.
                float bu = tu0 * 0.5f;
                if (tu0 > 26) {
                    rect(c, p, 0x40000000, bu - 4 + 1.5f * SX, tcy - 16 + 1.5f * SY, bu + 4 + 1.5f * SX, tcy - 8 + 1.5f * SY);
                    roundTent(c, p, bu, tcy - 12, 4.5f, 4.5f, 8, 0xFFC83A32, 0xFFF2E8D0, 0);
                    festoon(c, p, 4, tcy - 9, tu0 - 4, tcy - 9, 3, 8, true);
                    festoon(c, p, 4, tcy + 9, tu0 - 4, tcy + 9, 3, 8, true);
                }
                tree(c, p, 7, 7, 5);
                tree(c, p, 7, S - 7, 5);
                end(c);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2, rx = (x1 - x0) / 2, ry = (y1 - y0) / 2;
                c.save();
                c.clipRect(x0, y0, x1, y1);
                rect(c, p, 0xFF6A9A4A, x0, y0, x1, y1);
                oval(c, p, 0xFFC8A870, x0 - 10, y0 - 10, x1 + 10, y1 + 10);
                oval(c, p, 0x48000000, x0 + 4, y0 + 5, x1 + 4, y1 + 5);
                c.restore();
                oval(c, p, 0xFF8A2A24, x0, y0, x1, y1);
                roundTent(c, p, cx, cy, rx - 1, ry - 1, 24, 0xFFD8342A, 0xFFF4ECD8, 0);
                // Scalloped valance round the edge.
                for (int k = 0; k < 32; k++) {
                    double a = k * Math.PI / 16;
                    circle(c, p, k % 2 == 0 ? 0xFF2E5AA8 : 0xFFE8B83A, cx + (float) Math.cos(a) * (rx - 1.5f), cy + (float) Math.sin(a) * (ry - 1.5f), 1.8f);
                }
                strokeOval(c, p, 0x60000000, 0.6f, cx - rx * 0.55f, cy - ry * 0.55f, cx + rx * 0.55f, cy + ry * 0.55f);
                // The king poles with pennants.
                boolean along = rx >= ry;
                float d = Math.abs(rx - ry) * 0.5f + Math.min(rx, ry) * 0.12f;
                for (int k = -1; k <= 1; k += 2) {
                    float px = along ? cx + k * d : cx, py = along ? cy : cy + k * d;
                    circle(c, p, 0x50000000, px + 1.5f, py + 1.5f, 2.4f);
                    circle(c, p, 0xFFE8B83A, px, py, 2.2f);
                    circle(c, p, 0xFFF8E8A0, px - 0.6f, py - 0.6f, 0.9f);
                    poly(c, p, 0xFF2E5AA8, px, py - 0.8f, px + 7, py + 1, px, py + 1.6f);
                }
                sign(city, "CIRCUS", cx, cy, 7, 0xFFE8B83A);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.PARK | Variants.SUB;
        d.minS = 10;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_SEATS;
        d.roofCol = 0xFFD8342A;
        out.add(d);
    }

    /** The big top's footprint {u, v, w, h} in a block of lt by st tiles. */
    static int[] topSize(int lt, int st) {
        int h = Math.max(5, st - 4);
        int w = Math.max(h, Math.min(lt - 7, Math.round(h * 1.35f)));
        int u = Math.max(2, Math.min(lt - w - 5, (lt - w) / 2 - 2));
        return new int[]{u, (st - h) / 2, w, h};
    }

    static void caravan(Canvas c, Paint p, float x, float y, float len, float wid, int col) {
        sh(c, p, x, y, x + len, y + wid, 1.8f);
        line(c, p, 0xFF3A3C40, 0.8f, x - 4, y + wid / 2, x, y + wid / 2);
        roundRect(c, p, City.darken(col, 0.75f), x, y, x + len, y + wid, 2.5f);
        roundRect(c, p, col, x + 0.8f, y + 0.8f, x + len - 0.8f, y + wid - 0.8f, 2);
        rect(c, p, 0xFFF2E8D0, x + 1, y + wid * 0.42f, x + len - 1, y + wid * 0.58f);
        rect(c, p, City.lighten(col, 0.2f), x + len * 0.3f, y + 1.5f, x + len * 0.5f, y + wid * 0.35f);
        circle(c, p, 0xFF9AA0A6, x + len * 0.75f, y + wid * 0.25f, 1);
    }

    // A craft brewery: a brick brewhouse with copper kettles on its roof, steel tanks in the yard and a beer garden.
    static void brewery(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Craft Brewery", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int lt = Math.max(bw, bh), st = Math.min(bw, bh);
                return orient(new int[][]{{1, 1, brewW(lt), st - 2, 2}}, bw, bh);
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                float[] f = begin(c, x0, y0, x1, y1);
                float L = f[0], S = f[1], T = City.T;
                float bu1 = (1 + brewW((int) (L / T))) * T;
                rect(c, p, 0xFF9A8E7E, 0, 0, L, S);
                // Brick-paved yard.
                for (float v = 2; v < S * 0.5f; v += 4) line(c, p, 0x1C000000, 0.6f, bu1, v, L, v);
                for (float u = bu1 + 3; u < L; u += 12) line(c, p, 0x14000000, 0.5f, u, 0, u, S * 0.5f);
                // Fermenting tanks: tall steel cylinders with copper ones among them, pipes between.
                float yu0 = bu1 + 10, yu1 = L - 8, yv0 = 8, yv1 = S * 0.45f;
                float tr = Math.max(5, Math.min(10, (yv1 - yv0) / 4.6f));
                int rowsN = Math.max(1, Math.min(2, (int) ((yv1 - yv0 + 4) / (2 * tr + 4))));
                int colsN = Math.max(1, Math.min(4, (int) ((yu1 - yu0 + 4) / (2 * tr + 4))));
                for (int j = 0; j < rowsN; j++) {
                    float v = yv0 + tr + j * (2 * tr + 4);
                    rect(c, p, 0xFF8A9096, yu0 + tr, v - 1, yu0 + tr + (colsN - 1) * (2 * tr + 4), v + 1);
                    for (int i = 0; i < colsN; i++) {
                        float u = yu0 + tr + i * (2 * tr + 4);
                        boolean copper = (i + j) % 3 == 1;
                        circle(c, p, 0x48000000, u + tr * 0.9f * SX, v + tr * 0.9f * SY, tr);
                        int base = copper ? 0xFFB06A3A : 0xFFB4BAC0;
                        circle(c, p, City.darken(base, 0.75f), u, v, tr);
                        circle(c, p, base, u - tr * 0.08f, v - tr * 0.08f, tr * 0.85f);
                        circle(c, p, City.lighten(base, 0.3f), u - tr * 0.35f, v - tr * 0.35f, tr * 0.3f);
                        circle(c, p, City.darken(base, 0.6f), u, v, tr * 0.18f);
                    }
                }
                // Malt sacks on pallets by the brewhouse door.
                float mu = yu0, mv = yv0 + rowsN * (2 * tr + 4) + 2;
                if (mv + 8 < S * 0.5f) {
                    for (int i = 0; i < 3; i++) {
                        rect(c, p, 0xFF9A7A50, mu + i * 11, mv, mu + i * 11 + 9, mv + 7);
                        rect(c, p, 0xFFD8C8A0, mu + i * 11 + 1, mv + 1, mu + i * 11 + 4, mv + 6);
                        rect(c, p, 0xFFC8B890, mu + i * 11 + 5, mv + 1, mu + i * 11 + 8, mv + 6);
                    }
                }
                // The beer garden: gravel, long tables, trees, barrels and fairy lights.
                float gu0 = bu1 + 6, gu1 = L - 6, gv0 = S * 0.5f + 4, gv1 = S - 6;
                rect(c, p, 0xFFC8B898, gu0, gv0, gu1, gv1);
                for (int j = 0; j < 30; j++) circle(c, p, 0x18000000, gu0 + r.nextFloat() * (gu1 - gu0), gv0 + r.nextFloat() * (gv1 - gv0), 0.6f);
                frame(c, p, 0xFF6A5A44, 0.8f, gu0, gv0, gu1, gv1);
                for (float v = gv0 + 12; v < gv1 - 6; v += 24) {
                    for (float u = gu0 + 16; u + 9 < gu1 - 4; u += 30) longTable(c, p, u, v);
                }
                for (float v = gv0 + 24; v < gv1 - 8; v += 24) {
                    for (float u = gu0 + 31; u < gu1 - 10; u += 30) tree(c, p, u, v, 4.5f, 0xFF4A7A3A);
                }
                tree(c, p, gu0 + 4, gv1 - 4, 6, 0xFF4A7A3A);
                tree(c, p, gu1 - 4, gv0 + 4, 6, 0xFF4A7A3A);
                for (float u = gu0 + 22; u < gu1 - 10; u += 30) festoon(c, p, u, gv0 + 2, u + 20, gv1 - 2, 4, 9, false);
                festoon(c, p, gu0 + 2, gv0 + 3, gu1 - 2, gv0 + 3, 5, 18, false);
                festoon(c, p, gu0 + 2, gv1 - 3, gu1 - 2, gv1 - 3, -5, 18, false);
                for (int j = 0; j < 3; j++) {
                    float bx = bu1 + 4, by = S * 0.5f - 8 + j * 6;
                    circle(c, p, 0x40000000, bx + 1, by + 1, 2.4f);
                    circle(c, p, 0xFF7A5030, bx, by, 2.4f);
                    ring(c, p, 0xFF4A4C50, 0.4f, bx, by, 1.6f);
                }
                end(c);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                float[] f = begin(c, x0, y0, x1, y1);
                float L = f[0], S = f[1];
                // A brick parapet round a slate pitched roof, ridge along the long side.
                rect(c, p, 0xFF8A4A36, 0, 0, L, S);
                for (float v = 0.5f; v < S; v += 2.5f) line(c, p, 0x30000000, 0.3f, 0, v, L, v);
                rect(c, p, 0xFF4E5560, 2.5f, 2.5f, L - 2.5f, S / 2);
                rect(c, p, 0xFF3C424C, 2.5f, S / 2, L - 2.5f, S - 2.5f);
                for (float u = 5; u < L - 3; u += 4) {
                    line(c, p, 0x18000000, 0.4f, u, 2.5f, u, S / 2);
                    line(c, p, 0x18000000, 0.4f, u, S / 2, u, S - 2.5f);
                }
                line(c, p, 0xFF8A9098, 1, 2.5f, S / 2, L - 2.5f, S / 2);
                // Copper-green louvred vent along the ridge.
                float vw = Math.min(L * 0.3f, 30);
                rect(c, p, 0xFF3E8A74, L * 0.5f - vw / 2, S / 2 - 3, L * 0.5f + vw / 2, S / 2 + 3);
                for (float u = L * 0.5f - vw / 2 + 2; u < L * 0.5f + vw / 2; u += 2.5f) line(c, p, 0xFF2E6A5A, 0.5f, u, S / 2 - 3, u, S / 2 + 3);
                rect(c, p, 0xFF5AAE96, L * 0.5f - vw / 2, S / 2 - 3, L * 0.5f + vw / 2, S / 2 - 2);
                // Two copper kettle domes and a round brick chimney.
                float kr = Math.min(S * 0.17f, 9);
                for (int k = 0; k < 2; k++) {
                    float kx = L * (0.2f + 0.6f * k), ky = S * 0.25f;
                    circle(c, p, 0x50000000, kx + 2 * SX, ky + 2 * SY, kr);
                    circle(c, p, 0xFF8A4A26, kx, ky, kr);
                    circle(c, p, 0xFFC07A40, kx - kr * 0.1f, ky - kr * 0.1f, kr * 0.82f);
                    circle(c, p, 0xFFE8A868, kx - kr * 0.4f, ky - kr * 0.4f, kr * 0.28f);
                    circle(c, p, 0xFF8A4A26, kx, ky, kr * 0.25f);
                }
                float chx = L * 0.82f, chy = S * 0.74f, chr = Math.min(5, S * 0.1f);
                circle(c, p, 0x50000000, chx + 5 * SX, chy + 5 * SY, chr);
                circle(c, p, 0xFF7A3A2A, chx, chy, chr);
                circle(c, p, 0xFF9A5038, chx, chy, chr * 0.8f);
                circle(c, p, 0xFF1E1A18, chx, chy, chr * 0.5f);
                for (int k = 0; k < 3; k++) rect(c, p, 0xFFA8CCD8, L * 0.12f + k * 9, S * 0.62f, L * 0.12f + k * 9 + 5, S * 0.86f);
                end(c);
                sign(city, "BREWERY", (x0 + x1) / 2, (y0 + y1) / 2, 7, 0xFFE8A868);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.IND | Variants.OLD | Variants.MID;
        d.minS = 10;
        d.minL = 12;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_BAR;
        d.roofCol = 0xFF4E5560;
        out.add(d);
    }

    static int brewW(int lt) {
        return Math.max(4, Math.round(lt * 0.45f));
    }

    static void longTable(Canvas c, Paint p, float x, float y) {
        rect(c, p, 0x30000000, x - 9 + SX, y - 4 + SY, x + 9 + SX, y + 4 + SY);
        rect(c, p, 0xFF6A4A2E, x - 9, y - 4, x + 9, y - 2.6f);
        rect(c, p, 0xFF6A4A2E, x - 9, y + 2.6f, x + 9, y + 4);
        rect(c, p, 0xFF8A6440, x - 9, y - 1.8f, x + 9, y + 1.8f);
        circle(c, p, 0xFFE8B83A, x - 4, y, 0.8f);
        circle(c, p, 0xFFE8B83A, x + 3, y - 0.5f, 0.8f);
    }
}
