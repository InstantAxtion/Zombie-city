package com.instantaxtion.zombiesandbox;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.Landmarks.*;

/** ((22.6) Landmark designs, part 7 (see Landmarks): sports, leisure, animals and nature. */
final class LandmarkArt7 {
    private LandmarkArt7() {
    }

    static void register(List<Landmarks.Def> out) {
        baseball(out);
        basketball(out);
        velodrome(out);
        lido(out);
        climbing(out);
        drivingRange(out);
        bowls(out);
        equestrian(out);
        training(out);
        athletics(out);
        waterPark(out);
        trampoline(out);
        paintball(out);
        archery(out);
        boxing(out);
        yoga(out);
        roseGarden(out);
        butterfly(out);
        pettingFarm(out);
        zoo(out);
        aviary(out);
        meadow(out);
        waterfall(out);
        orchard(out);
        rockGarden(out);
    }

    // ------------------------------------------------------------------ shared bits

    static final float T = City.T;
    static final int[] SKIN = {0xFFE8C4A0, 0xFFC89870, 0xFF8E6040, 0xFFF0D0B0, 0xFF6E4A30};
    static final int[] KIT = {0xFFC83A30, 0xFF2E5EA8, 0xFFE8C030, 0xFF3A8A4A, 0xFFE87A2A, 0xFF8A4AA8, 0xFFF0F0EC, 0xFF30343A};

    /** A building laid out along the block's long side (u along it, v across), turned when the block is tall. */
    static int[] at(boolean along, int u, int v, int lu, int lv, int floors) {
        return along ? new int[]{u, v, lu, lv, floors} : new int[]{v, u, lv, lu, floors};
    }

    /** A planned building's footprint in the block's u/v frame (world units from the block's corner). */
    static float[] bu(int[] b, boolean along) {
        return along ? new float[]{b[0] * T, b[1] * T, (b[0] + b[2]) * T, (b[1] + b[3]) * T}
                : new float[]{b[1] * T, b[0] * T, (b[1] + b[3]) * T, (b[0] + b[2]) * T};
    }

    static int tiles(float a) {
        return Math.round(a / T);
    }

    /** Drawing in a rectangle's own frame: u along its long side, v across; a tall one is drawn transposed. */
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

        /** A rectangle with a soft shadow to the lower right. */
        void box(int col, float u0, float v0, float u1, float v1, float sh) {
            Landmarks.rect(c, p, 0x38000000, x(u0, v0) + sh, y(u0, v0) + sh, x(u1, v1) + sh, y(u1, v1) + sh);
            rect(col, u0, v0, u1, v1);
        }

        void oval(int col, float u0, float v0, float u1, float v1) {
            Landmarks.oval(c, p, col, x(u0, v0), y(u0, v0), x(u1, v1), y(u1, v1));
        }

        void roundRect(int col, float u0, float v0, float u1, float v1, float rad) {
            Landmarks.roundRect(c, p, col, x(u0, v0), y(u0, v0), x(u1, v1), y(u1, v1), rad);
        }

        void rrFrame(int col, float w, float u0, float v0, float u1, float v1, float rad) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(w);
            p.setColor(col);
            RF.set(x(u0, v0), y(u0, v0), x(u1, v1), y(u1, v1));
            c.drawRoundRect(RF, rad, rad, p);
            p.setStyle(Paint.Style.FILL);
        }

        void ovalFrame(int col, float w, float u0, float v0, float u1, float v1) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(w);
            p.setColor(col);
            RF.set(x(u0, v0), y(u0, v0), x(u1, v1), y(u1, v1));
            c.drawOval(RF, p);
            p.setStyle(Paint.Style.FILL);
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

        /** A polyline with round ends. */
        void pl(int col, float w, float... q) {
            Path pa = new Path();
            mv(pa, q[0], q[1]);
            for (int k = 2; k + 1 < q.length; k += 2) ln(pa, q[k], q[k + 1]);
            stroke(pa, col, w);
        }

        /** A quadratic curve with round ends. */
        void curve(int col, float w, float u0, float v0, float cu, float cv, float u1, float v1) {
            Path pa = new Path();
            mv(pa, u0, v0);
            qd(pa, cu, cv, u1, v1);
            stroke(pa, col, w);
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

        void pond(float u0, float v0, float u1, float v1) {
            Landmarks.pond(c, p, x(u0, v0), y(u0, v0), x(u1, v1), y(u1, v1));
        }

        void clip(float u0, float v0, float u1, float v1) {
            c.clipRect(x(u0, v0), y(u0, v0), x(u1, v1), y(u1, v1));
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

        /** A circular arc (degrees in the u/v frame) added to the path as quads. */
        void arc(Path q, float cu, float cv, float rad, float a0, float a1, int n, boolean move) {
            double s0 = Math.toRadians(a0), d = Math.toRadians(a1 - a0) / n;
            float su = cu + (float) Math.cos(s0) * rad, sv = cv + (float) Math.sin(s0) * rad;
            if (move) mv(q, su, sv);
            else ln(q, su, sv);
            float cr = (float) (rad / Math.cos(d / 2));
            for (int k = 0; k < n; k++) {
                double m = s0 + d * (k + 0.5), e = s0 + d * (k + 1);
                qd(q, cu + (float) Math.cos(m) * cr, cv + (float) Math.sin(m) * cr, cu + (float) Math.cos(e) * rad, cv + (float) Math.sin(e) * rad);
            }
        }

        void arcLine(int col, float w, float cu, float cv, float rad, float a0, float a1) {
            Path q = new Path();
            arc(q, cu, cv, rad, a0, a1, Math.max(2, (int) (Math.abs(a1 - a0) / 30)), true);
            stroke(q, col, w);
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
            p.setStrokeCap(Paint.Cap.ROUND);
            c.drawPath(q, p);
            p.setStrokeCap(Paint.Cap.BUTT);
            p.setStyle(Paint.Style.FILL);
        }

        void sign(City city, String s, float u, float v, float size, int col) {
            Landmarks.sign(city, s, x(u, v), y(u, v), size, col);
        }
    }

    /** A person from above: a shadow, the shoulders in their clothes and a head. */
    static void man(F f, float u, float v, int col) {
        int k = Math.abs((int) (u * 7 + v * 13)) % SKIN.length;
        f.circle(0x38000000, u + 0.8f, v + 0.9f, 2.1f);
        f.circle(col, u, v, 2.1f);
        f.circle(SKIN[k], u - 0.2f, v - 0.2f, 1.2f);
    }

    /** A soft-edged blob through n jittered radii (k from jit). */
    static void blob(F f, int col, float u, float v, float ru, float rv, float[] k) {
        int n = k.length;
        float[] pu = new float[n], pv = new float[n];
        for (int i = 0; i < n; i++) {
            double a = i * 2 * Math.PI / n;
            pu[i] = u + (float) Math.cos(a) * ru * k[i];
            pv[i] = v + (float) Math.sin(a) * rv * k[i];
        }
        Path q = new Path();
        f.mv(q, (pu[n - 1] + pu[0]) / 2, (pv[n - 1] + pv[0]) / 2);
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            f.qd(q, pu[i], pv[i], (pu[i] + pu[j]) / 2, (pv[i] + pv[j]) / 2);
        }
        q.close();
        f.fill(q, col);
    }

    static float[] jit(Random r, int n, float a) {
        float[] k = new float[n];
        for (int i = 0; i < n; i++) k[i] = 1 - a + 2 * a * r.nextFloat();
        return k;
    }

    /** An angular boulder: a shadow, the dark body, a lit face and a flat top. */
    static void rock(F f, float u, float v, float rad, int col, Random r) {
        int n = 5 + r.nextInt(3);
        float[] q = new float[2 * n], s = new float[2 * n], tp = new float[2 * n];
        double a0 = r.nextFloat() * Math.PI;
        for (int i = 0; i < n; i++) {
            double a = a0 + i * 2 * Math.PI / n;
            float k = rad * (0.75f + 0.4f * r.nextFloat());
            q[2 * i] = u + (float) Math.cos(a) * k;
            q[2 * i + 1] = v + (float) Math.sin(a) * k * 0.85f;
            s[2 * i] = q[2 * i] + rad * 0.35f;
            s[2 * i + 1] = q[2 * i + 1] + rad * 0.4f;
            tp[2 * i] = u - rad * 0.18f + (q[2 * i] - u) * 0.55f;
            tp[2 * i + 1] = v - rad * 0.18f + (q[2 * i + 1] - v) * 0.55f;
        }
        f.poly(0x40000000, s);
        f.poly(City.darken(col, 0.7f), q);
        float[] lit = new float[2 * n];
        for (int i = 0; i < n; i++) {
            lit[2 * i] = q[2 * i] * 0.85f + tp[2 * i] * 0.15f - rad * 0.05f;
            lit[2 * i + 1] = q[2 * i + 1] * 0.85f + tp[2 * i + 1] * 0.15f - rad * 0.05f;
        }
        f.poly(City.darken(col, 0.86f), lit);
        f.poly(col, tp);
        f.line(City.lighten(col, 0.25f), 0.5f, tp[0], tp[1], tp[2], tp[3]);
    }

    /** An animal from above, heading deg (u/v frame), sc its size; col its coat (0 for the usual). */
    static void animal(F f, float u, float v, float deg, int kind, int col, float sc) {
        Canvas c = f.c;
        Paint p = f.p;
        float x = f.x(u, v), y = f.y(u, v), a = f.t ? 90 - deg : deg;
        double ar = Math.toRadians(-a);
        float sx = 1.1f * sc, sy = 1.3f * sc;
        float lx = (float) (sx * Math.cos(ar) - sy * Math.sin(ar)), ly = (float) (sx * Math.sin(ar) + sy * Math.cos(ar));
        c.save();
        c.translate(x, y);
        c.rotate(a);
        float s = sc;
        switch (kind) {
            case 0: { // horse
                int b = col != 0 ? col : 0xFF7A4A2A;
                oval(c, p, 0x40000000, -4.5f * s + lx, -1.8f * s + ly, 4.5f * s + lx, 1.8f * s + ly);
                line(c, p, City.darken(b, 0.5f), 1.1f * s, -4.2f * s, 0, -6.8f * s, 0.4f * s);
                oval(c, p, b, -4.5f * s, -1.8f * s, 4.5f * s, 1.8f * s);
                oval(c, p, City.lighten(b, 0.12f), -3.5f * s, -1.3f * s, 3f * s, 0.6f * s);
                oval(c, p, b, 3.2f * s, -0.9f * s, 8.2f * s, 0.9f * s);
                line(c, p, City.darken(b, 0.45f), 0.7f * s, 3.2f * s, 0, 6.5f * s, 0);
                oval(c, p, City.darken(b, 0.8f), 6.8f * s, -0.7f * s, 8.8f * s, 0.7f * s);
                break;
            }
            case 1: { // cow
                oval(c, p, 0x40000000, -4 * s + lx, -2.2f * s + ly, 4 * s + lx, 2.2f * s + ly);
                oval(c, p, 0xFFF2F0EA, -4 * s, -2.2f * s, 4 * s, 2.2f * s);
                circle(c, p, 0xFF2A2A2A, -1.5f * s, -0.8f * s, 1.3f * s);
                circle(c, p, 0xFF2A2A2A, 1.6f * s, 1.0f * s, 1.0f * s);
                oval(c, p, 0xFF2A2A2A, 3.4f * s, -1.1f * s, 6.2f * s, 1.1f * s);
                oval(c, p, 0xFFE8B0A8, 5.4f * s, -0.7f * s, 6.6f * s, 0.7f * s);
                line(c, p, 0xFFD8D0B8, 0.5f * s, 4 * s, -1.2f * s, 4 * s, 1.2f * s);
                break;
            }
            case 2: { // sheep
                circle(c, p, 0x40000000, lx, ly, 2.4f * s);
                oval(c, p, 0xFFE8E2D2, -3 * s, -2 * s, 3 * s, 2 * s);
                circle(c, p, 0xFFF4F0E4, -1.2f * s, -0.7f * s, 1.1f * s);
                circle(c, p, 0xFFF4F0E4, 0.8f * s, -0.9f * s, 1f * s);
                circle(c, p, 0xFFF4F0E4, 0, 0.8f * s, 1f * s);
                oval(c, p, 0xFF2E2A28, 2.4f * s, -0.9f * s, 4.6f * s, 0.9f * s);
                break;
            }
            case 3: { // pig
                oval(c, p, 0x40000000, -3 * s + lx, -1.8f * s + ly, 3 * s + lx, 1.8f * s + ly);
                oval(c, p, 0xFFE8A8A0, -3 * s, -1.8f * s, 3 * s, 1.8f * s);
                oval(c, p, 0xFFF2C0B8, -2.2f * s, -1.2f * s, 2 * s, 0.3f * s);
                circle(c, p, 0xFFD88A84, 2.2f * s, -1.1f * s, 0.7f * s);
                circle(c, p, 0xFFD88A84, 2.2f * s, 1.1f * s, 0.7f * s);
                circle(c, p, 0xFFD08078, 3.3f * s, 0, 0.8f * s);
                if (col != 0) circle(c, p, 0xFF6A4A3A, -1 * s, 0.6f * s, 0.8f * s);
                break;
            }
            case 4: { // goat
                int b = col != 0 ? col : 0xFFD8CCB4;
                oval(c, p, 0x40000000, -2.6f * s + lx, -1.3f * s + ly, 2.6f * s + lx, 1.3f * s + ly);
                oval(c, p, b, -2.6f * s, -1.3f * s, 2.6f * s, 1.3f * s);
                oval(c, p, City.darken(b, 0.85f), 1.8f * s, -0.7f * s, 4 * s, 0.7f * s);
                line(c, p, 0xFF5A4A3A, 0.4f * s, 2.4f * s, -0.4f * s, 1.6f * s, -1f * s);
                line(c, p, 0xFF5A4A3A, 0.4f * s, 2.4f * s, 0.4f * s, 1.6f * s, 1f * s);
                break;
            }
            case 5: { // duck
                circle(c, p, 0x30000000, lx * 0.4f, ly * 0.4f, 1.3f * s);
                oval(c, p, col != 0 ? col : 0xFFF4F2EC, -1.5f * s, -0.85f * s, 1.3f * s, 0.85f * s);
                circle(c, p, col != 0 ? 0xFF2E6A3A : 0xFFF8F8F4, 1.3f * s, 0, 0.6f * s);
                rect(c, p, 0xFFE89A30, 1.8f * s, -0.25f * s, 2.4f * s, 0.25f * s);
                break;
            }
            case 6: { // elephant
                oval(c, p, 0x40000000, -7 * s + lx, -4.4f * s + ly, 6 * s + lx, 4.4f * s + ly);
                line(c, p, 0xFF5E5A58, 0.7f * s, -6.5f * s, 0, -8.5f * s, 0.5f * s);
                oval(c, p, 0xFF8E8884, -7 * s, -4.4f * s, 6 * s, 4.4f * s);
                oval(c, p, 0xFF9E9894, -5.5f * s, -3.2f * s, 4 * s, 1.5f * s);
                oval(c, p, 0xFF7E7874, 2.2f * s, -6.2f * s, 6 * s, -0.8f * s);
                oval(c, p, 0xFF7E7874, 2.2f * s, 0.8f * s, 6 * s, 6.2f * s);
                circle(c, p, 0xFF8A8480, 6 * s, 0, 2.6f * s);
                line(c, p, 0xFF7A7470, 1.3f * s, 8 * s, 0, 10.5f * s, 0.8f * s);
                line(c, p, 0xFFF0EADC, 0.5f * s, 7.6f * s, -1.2f * s, 9 * s, -1.4f * s);
                line(c, p, 0xFFF0EADC, 0.5f * s, 7.6f * s, 1.2f * s, 9 * s, 1.4f * s);
                break;
            }
            case 7: { // giraffe
                oval(c, p, 0x40000000, -4 * s + lx * 2, -2 * s + ly * 2, 4 * s + lx * 2, 2 * s + ly * 2);
                line(c, p, 0x30000000, 1.6f * s, 3 * s + lx * 3, ly * 3, 10 * s + lx * 4, ly * 4);
                oval(c, p, 0xFFD8A858, -4 * s, -2 * s, 4 * s, 2 * s);
                line(c, p, 0xFFD8A858, 1.6f * s, 3 * s, 0, 10 * s, 0);
                for (int i = 0; i < 6; i++) circle(c, p, 0xFF8A5A2A, (-3 + i * 1.2f) * s, ((i % 2) * 2 - 1) * 0.8f * s, 0.6f * s);
                circle(c, p, 0xFF8A5A2A, 5.5f * s, 0, 0.4f * s);
                circle(c, p, 0xFF8A5A2A, 8 * s, 0, 0.4f * s);
                oval(c, p, 0xFFC89848, 9.6f * s, -0.9f * s, 12.4f * s, 0.9f * s);
                circle(c, p, 0xFF5A3A1A, 10.2f * s, -0.7f * s, 0.35f * s);
                circle(c, p, 0xFF5A3A1A, 10.2f * s, 0.7f * s, 0.35f * s);
                break;
            }
            case 8: { // penguin
                circle(c, p, 0x30000000, lx * 0.4f, ly * 0.4f, 1.2f * s);
                oval(c, p, 0xFF22252A, -1.3f * s, -0.95f * s, 1.3f * s, 0.95f * s);
                oval(c, p, 0xFFF0F0EC, 0.1f * s, -0.6f * s, 1.25f * s, 0.6f * s);
                circle(c, p, 0xFF22252A, 1.1f * s, 0, 0.55f * s);
                rect(c, p, 0xFFE8A030, 1.5f * s, -0.15f * s, 2.1f * s, 0.15f * s);
                break;
            }
            case 9: { // rabbit
                circle(c, p, 0x30000000, lx * 0.4f, ly * 0.4f, 1.2f * s);
                oval(c, p, col != 0 ? col : 0xFFB8A894, -1.4f * s, -1f * s, 1.2f * s, 1f * s);
                circle(c, p, col != 0 ? col : 0xFFB8A894, 1.3f * s, 0, 0.7f * s);
                line(c, p, City.darken(col != 0 ? col : 0xFFB8A894, 0.8f), 0.4f * s, 1.2f * s, -0.3f * s, -0.2f * s, -0.6f * s);
                line(c, p, City.darken(col != 0 ? col : 0xFFB8A894, 0.8f), 0.4f * s, 1.2f * s, 0.3f * s, -0.2f * s, 0.6f * s);
                circle(c, p, 0xFFF8F8F4, -1.4f * s, 0, 0.4f * s);
                break;
            }
            case 10: { // flamingo
                circle(c, p, 0x30000000, lx, ly, 1.4f * s);
                oval(c, p, 0xFFF08A98, -1.6f * s, -1f * s, 1.4f * s, 1f * s);
                oval(c, p, 0xFFF4A4B0, -1.0f * s, -0.6f * s, 1f * s, 0.2f * s);
                line(c, p, 0xFFF08A98, 0.5f * s, 1.2f * s, 0, 2.4f * s, -1.2f * s);
                circle(c, p, 0xFFF4A0AC, 2.6f * s, -1.4f * s, 0.5f * s);
                line(c, p, 0xFF2A2A2A, 0.4f * s, 2.9f * s, -1.4f * s, 3.4f * s, -1.1f * s);
                break;
            }
            default: { // a perched or flying bird, wings out, in col
                int b = col != 0 ? col : 0xFFC83A30;
                poly(c, p, 0x30000000, -0.6f * s + lx * 0.5f, ly * 0.5f, 0.6f * s + lx * 0.5f, -2.2f * s + ly * 0.5f, 0.2f * s + lx * 0.5f, ly * 0.5f, 0.6f * s + lx * 0.5f, 2.2f * s + ly * 0.5f);
                poly(c, p, City.darken(b, 0.8f), -0.6f * s, 0, 0.6f * s, -2.2f * s, 0.2f * s, 0, 0.6f * s, 2.2f * s);
                oval(c, p, b, -1.4f * s, -0.5f * s, 1.2f * s, 0.5f * s);
                circle(c, p, City.lighten(b, 0.2f), 1.1f * s, 0, 0.45f * s);
                break;
            }
        }
        c.restore();
        p.setStyle(Paint.Style.FILL);
    }

    /** A rooftop plant box with a fan. */
    static void hvac(F f, float u, float v, float s) {
        f.box(0xFFB4B8BC, u, v, u + s * 1.6f, v + s, 1);
        f.circle(0xFF6A6E72, u + s * 0.5f, v + s * 0.5f, s * 0.35f);
        f.line(0xFF8A8E92, 0.4f, u + s * 1.05f, v + 1, u + s * 1.05f, v + s - 1);
        f.line(0xFF8A8E92, 0.4f, u + s * 1.3f, v + 1, u + s * 1.3f, v + s - 1);
    }

    /** A gabled roof with its ridge along u. */
    static void gable(F f, int col, float u0, float v0, float u1, float v1) {
        float m = (v0 + v1) / 2;
        f.rect(City.lighten(col, 0.1f), u0, v0, u1, m);
        f.rect(City.darken(col, 0.74f), u0, m, u1, v1);
        f.line(City.lighten(col, 0.35f), 1f, u0, m, u1, m);
        f.frame(City.darken(col, 0.6f), 0.6f, u0, v0, u1, v1);
    }

    /** A hipped roof, its faces shaded for light from the top left. */
    static void hip(F f, int col, float u0, float v0, float u1, float v1) {
        float h = Math.min(u1 - u0, v1 - v0) / 2, au = u0 + h, av = v0 + h, bu = u1 - h, bv = v1 - h;
        f.poly(City.lighten(col, 0.12f), u0, v0, u1, v0, bu, av, au, av);
        f.poly(col, u0, v0, u0, v1, au, bv, au, av);
        f.poly(City.darken(col, 0.82f), u1, v0, u1, v1, bu, bv, bu, av);
        f.poly(City.darken(col, 0.7f), u0, v1, u1, v1, bu, bv, au, bv);
        int hl = City.lighten(col, 0.3f);
        f.line(hl, 0.6f, u0, v0, au, av);
        f.line(hl, 0.6f, u1, v0, bu, av);
        f.line(City.darken(col, 0.6f), 0.6f, u0, v1, au, bv);
        f.line(City.darken(col, 0.6f), 0.6f, u1, v1, bu, bv);
        f.line(hl, 1f, au, av, bu, bv);
    }

    /** Ribbed sheet metal with the ribs across u. */
    static void tin(F f, int col, float u0, float v0, float u1, float v1, float step) {
        f.rect(col, u0, v0, u1, v1);
        for (float u = u0 + step; u < u1 - 0.5f; u += step) {
            f.line(City.darken(col, 0.82f), 0.6f, u, v0, u, v1);
            f.line(City.lighten(col, 0.12f), 0.4f, u - 0.6f, v0, u - 0.6f, v1);
        }
    }

    /** A post-and-rail fence round a rectangle. */
    static void fence(F f, int col, float u0, float v0, float u1, float v1, float step) {
        f.frame(0x30000000, 0.9f, u0 + 0.6f, v0 + 0.8f, u1 + 0.6f, v1 + 0.8f);
        f.frame(col, 0.8f, u0, v0, u1, v1);
        for (float u = u0; u <= u1 + 0.1f; u += step) {
            f.circle(City.darken(col, 0.7f), u, v0, 0.7f);
            f.circle(City.darken(col, 0.7f), u, v1, 0.7f);
        }
        for (float v = v0 + step; v < v1 - 0.1f; v += step) {
            f.circle(City.darken(col, 0.7f), u0, v, 0.7f);
            f.circle(City.darken(col, 0.7f), u1, v, 0.7f);
        }
    }

    /** A parasol from above: eight striped panels. */
    static void parasol(F f, float u, float v, float rad, int a, int b) {
        f.circle(0x34000000, u + rad * 0.35f, v + rad * 0.45f, rad);
        for (int k = 0; k < 8; k++) {
            double a0 = k * Math.PI / 4, a1 = (k + 1) * Math.PI / 4;
            int col = k % 2 == 0 ? a : b;
            if (k >= 2 && k <= 5) col = City.darken(col, 0.88f);
            f.poly(col, u, v, u + (float) Math.cos(a0) * rad, v + (float) Math.sin(a0) * rad, u + (float) Math.cos(a1) * rad, v + (float) Math.sin(a1) * rad);
        }
        f.circle(0xFFEEEEEE, u, v, 0.6f);
    }

    /** A point at fraction t round a stadium shape (long along u) inside the rectangle. */
    static float[] stad(float u0, float v0, float u1, float v1, float t) {
        float r = (v1 - v0) / 2, a = u0 + r, b = u1 - r, st = b - a, cv = (v0 + v1) / 2;
        float per = 2 * st + (float) (2 * Math.PI * r), d = ((t % 1) + 1) % 1 * per;
        if (d < st) return new float[]{a + d, v0};
        d -= st;
        if (d < Math.PI * r) {
            double an = -Math.PI / 2 + d / r;
            return new float[]{b + (float) Math.cos(an) * r, cv + (float) Math.sin(an) * r};
        }
        d -= Math.PI * r;
        if (d < st) return new float[]{b - d, v1};
        d -= st;
        double an = Math.PI / 2 + d / r;
        return new float[]{a + (float) Math.cos(an) * r, cv + (float) Math.sin(an) * r};
    }

    /** A cluster of flowers in one colour. */
    static void flowers(F f, Random r, float u, float v, float rad, int n, int col) {
        for (int i = 0; i < n; i++) {
            double a = r.nextFloat() * Math.PI * 2;
            float d = rad * (float) Math.sqrt(r.nextFloat());
            f.circle(col, u + (float) Math.cos(a) * d, v + (float) Math.sin(a) * d, 0.6f + r.nextFloat() * 0.5f);
        }
    }

    // ------------------------------------------------------------------ 1. Baseball Park

    static void baseball(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Baseball Park", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int l = Math.max(bw, bh), s = Math.min(bw, bh), h = l / 2;
                boolean a = bw >= bh;
                return new int[][]{at(a, 1, s - 3, h - 3, 2, 2), at(a, h + 2, s - 3, l - h - 3, 2, 2)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int s = tiles(S);
                float sv = (s - 3) * T, hu = L / 2, hv = sv - 15;
                f.rect(0xFFCBC3B0, 0, 0, L, S);
                for (float u = 8; u < L; u += 8) f.line(0x14000000, 0.5f, u, sv - 2, u, S);
                f.rect(0xFF4C8A3E, 0, 0, L, sv - 2);
                float R = Math.min((hv - 5) / 1.08f, (L / 2 - 5) / 0.72f);
                // Trees and floodlights beyond the fence.
                for (float u = 8; u < L; u += 15)
                    for (float v = 8; v < sv - 10; v += 15) {
                        float du = u - hu, dv = v - hv;
                        if (Math.sqrt(du * du + dv * dv) > R + 10 && r.nextFloat() < 0.7f) f.tree(u + r.nextFloat() * 4, v + r.nextFloat() * 4, 5 + r.nextFloat() * 2, 0xFF3A7436);
                    }
                // The outfield, mown in wedges.
                int n = 12;
                for (int k = 0; k < n; k++) {
                    double a0 = Math.toRadians(-135 + 90.0 * k / n), a1 = Math.toRadians(-135 + 90.0 * (k + 1) / n);
                    f.poly(k % 2 == 0 ? 0xFF66A84C : 0xFF5A9C44, hu, hv, hu + (float) Math.cos(a0) * R, hv + (float) Math.sin(a0) * R,
                            hu + (float) Math.cos(a1) * R, hv + (float) Math.sin(a1) * R);
                }
                f.arcLine(0xFFB08A5E, 6, hu, hv, R - 3, -135, -45);
                f.arcLine(0xFF2A4630, 2, hu, hv, R + 0.5f, -136, -44);
                f.arcLine(0xFFE8C840, 0.6f, hu, hv, R - 0.4f, -135, -45);
                // The infield skin, clipped at the backstop.
                float dd = R * 0.27f, mu = hu, mv = hv - 0.672f * dd;
                int sc = c.save();
                f.clip(0, 0, L, hv + dd * 0.25f);
                f.circle(0xFFBA8E5E, mu, mv, 1.06f * dd);
                c.restoreToCount(sc);
                float k7 = 0.7071f;
                float[] cu = {hu, hu + dd * k7, hu, hu - dd * k7}, cv = {hv, hv - dd * k7, hv - dd * 1.414f, hv - dd * k7};
                float gu = hu, gv = hv - dd * k7, sh = 0.8f;
                f.poly(0xFF62A44A, gu + (cu[0] - gu) * sh, gv + (cv[0] - gv) * sh, gu + (cu[1] - gu) * sh, gv + (cv[1] - gv) * sh,
                        gu + (cu[2] - gu) * sh, gv + (cv[2] - gv) * sh, gu + (cu[3] - gu) * sh, gv + (cv[3] - gv) * sh);
                f.circle(0xFFBA8E5E, hu, hv, dd * 0.17f);
                f.circle(0xFFC49866, mu, mv, dd * 0.11f);
                f.rect(0xFFF4F4F0, mu - 1, mv - 0.3f, mu + 1, mv + 0.3f);
                // Foul lines and the bases.
                f.line(0xFFF4F4F0, 0.7f, hu, hv, hu + (R - 6) * k7, hv - (R - 6) * k7);
                f.line(0xFFF4F4F0, 0.7f, hu, hv, hu - (R - 6) * k7, hv - (R - 6) * k7);
                for (int k = 1; k < 4; k++) f.poly(0xFFF4F4F0, cu[k], cv[k] - 1.4f, cu[k] + 1.4f, cv[k], cu[k], cv[k] + 1.4f, cu[k] - 1.4f, cv[k]);
                f.poly(0xFFF8F8F4, hu - 1.1f, hv - 1.1f, hu + 1.1f, hv - 1.1f, hu + 1.1f, hv, hu, hv + 1.1f, hu - 1.1f, hv);
                f.frame(0xC0FFFFFF, 0.4f, hu - 4.4f, hv - 2.2f, hu - 1.8f, hv + 2.2f);
                f.frame(0xC0FFFFFF, 0.4f, hu + 1.8f, hv - 2.2f, hu + 4.4f, hv + 2.2f);
                // The backstop net and the dugouts.
                f.arcLine(0xFF3A3E44, 1, hu, hv, 11, 25, 155);
                for (int sd = -1; sd <= 1; sd += 2) {
                    float a = hu + sd * 16, b = hu + sd * (16 + dd * 0.9f);
                    f.box(0xFF3E3A36, Math.min(a, b), sv - 8, Math.max(a, b), sv - 2, 1);
                    f.rect(0xFF2E5A9A, Math.min(a, b) + 1, sv - 7, Math.max(a, b) - 1, sv - 6);
                }
                // Players: fielders in white, the batter, catcher and umpire.
                int wh = 0xFFF2F2EE, gr = 0xFF7A7E86;
                man(f, mu, mv - 1, wh);
                man(f, hu, hv + 3, gr);
                man(f, hu, hv + 6.5f, 0xFF2A2A30);
                man(f, hu + 3.2f, hv, 0xFF2E4E8A);
                man(f, cu[1] + 2, cv[1] - 4, wh);
                man(f, cu[3] - 2, cv[3] - 4, wh);
                man(f, (cu[1] + cu[2]) / 2 + 4, (cv[1] + cv[2]) / 2 - 5, wh);
                man(f, (cu[3] + cu[2]) / 2 - 4, (cv[3] + cv[2]) / 2 - 5, wh);
                for (int k = 0; k < 3; k++) {
                    double a = Math.toRadians(-120 + 30 * k);
                    man(f, hu + (float) Math.cos(a) * R * 0.72f, hv + (float) Math.sin(a) * R * 0.72f, wh);
                }
                man(f, cu[1] + 1, cv[1] + 2, 0xFF2E4E8A);
                // Floodlight towers at the foul poles.
                for (int sd = -1; sd <= 1; sd += 2) {
                    float fu = hu + sd * (R + 4) * k7, fv = hv - (R + 4) * k7;
                    fu = Math.max(6, Math.min(L - 6, fu));
                    fv = Math.max(5, fv);
                    f.circle(0x40000000, fu + 3, fv + 3, 2.5f);
                    f.box(0xFF5A5E64, fu - 4, fv - 2, fu + 4, fv + 2, 2);
                    for (int i = 0; i < 4; i++) f.rect(0xFFF6F0C8, fu - 3.4f + i * 1.9f, fv - 1.3f, fu - 2.2f + i * 1.9f, fv + 1.3f);
                }
                // The scoreboard over the outfield wall.
                float sw = Math.min(L * 0.24f, 54), su = L - 5 - sw, svv = 3;
                f.box(0xFF2A2E2A, su, svv, su + sw, svv + 13, 2.5f);
                f.rect(0xFF1E4A2E, su + 1, svv + 1, su + sw - 1, svv + 12);
                f.rect(0xFF101418, su + sw * 0.62f, svv + 2, su + sw - 2, svv + 11);
                f.rect(0xFF3A6AA0, su + sw * 0.64f, svv + 3, su + sw - 3, svv + 10);
                for (int i = 0; i < 9; i++)
                    for (int j = 0; j < 2; j++)
                        f.rect(r.nextFloat() < 0.6f ? 0xFFF0D060 : 0xFF2A5A3A, su + 2.5f + i * (sw * 0.55f / 9), svv + 3 + j * 4.5f, su + 4 + i * (sw * 0.55f / 9), svv + 5.5f + j * 4.5f);
                // The entrance plaza between the stands: ticket booths and flags.
                for (int k = -1; k <= 1; k += 2) {
                    f.box(0xFFB83A30, hu + k * 18 - 3, sv + 4, hu + k * 18 + 3, sv + 10, 1);
                    f.rect(0xFFE8E0D0, hu + k * 18 - 3, sv + 4, hu + k * 18 + 3, sv + 5.5f);
                }
                for (int k = -2; k <= 2; k++) {
                    f.circle(0xFF505050, hu + k * 6, sv + 18, 0.7f);
                    f.rect(k % 2 == 0 ? 0xFF2E4E8A : 0xFFC83A30, hu + k * 6 + 0.5f, sv + 17, hu + k * 6 + 4, sv + 18.6f);
                }
                for (int i = 0; i < 8; i++) man(f, hu - 14 + r.nextFloat() * 28, sv + 11 + r.nextFloat() * 14, KIT[r.nextInt(KIT.length)]);
                f.lamp(4, S - 4);
                f.lamp(L - 4, S - 4);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, sd = S * 0.58f;
                f.rect(0xFF5E6268, 0, 0, L, S);
                int rows = (int) (sd / 2.6f);
                for (int i = 0; i < rows; i++)
                    f.rect(shade(0xFF2E5C9A, 0.8f + i * 0.05f), 0.5f, 0.6f + i * 2.6f, L - 0.5f, 0.6f + i * 2.6f + 2f);
                for (float u = 10; u < L - 4; u += 14) f.rect(0xFFB8B4AA, u - 0.8f, 0, u + 0.8f, sd);
                for (int i = 0; i < L / 2.5f; i++) {
                    float u = 1 + r.nextFloat() * (L - 2), v = 1 + r.nextInt(rows) * 2.6f;
                    f.circle(KIT[r.nextInt(KIT.length)], u, v + 0.4f, 0.8f);
                }
                // The cantilevered canopy over the back rows.
                f.rect(0xFFD4D6D8, 0, sd, L, S);
                for (float u = 4; u < L; u += 8) f.line(0xFFB0B4B8, 0.7f, u, sd, u, S);
                f.line(0xFFF4F6F8, 1.2f, 0, sd + 0.6f, L, sd + 0.6f);
                f.line(0xFF8A8E92, 1f, 0, S - 0.5f, L, S - 0.5f);
                if (part == 0) f.sign(city, "BALLPARK", L / 2, (sd + S) / 2, 5, 0xFF2E4E8A);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.MID | Variants.SUB | Variants.PARK | Variants.DOWN;
        d.minS = 12;
        d.minL = 14;
        d.base = City.OFFICE;
        d.inside = Variants.I_SEATS;
        d.roofCol = 0xFF2E5C9A;
        out.add(d);
    }

    // ------------------------------------------------------------------ 2. Basketball Arena

    /** A basketball court long along u. */
    static void court(F f, float u0, float v0, float u1, float v1, int surf, int key, Random r) {
        float w = u1 - u0, h = v1 - v0, cu = (u0 + u1) / 2, cv = (v0 + v1) / 2;
        f.rect(surf, u0, v0, u1, v1);
        int ln = 0xFFF2F2EE;
        float kl = w * 0.2f, kw = h * 0.33f, ar = h * 0.45f;
        f.rect(key, u0, cv - kw / 2, u0 + kl, cv + kw / 2);
        f.rect(key, u1 - kl, cv - kw / 2, u1, cv + kw / 2);
        f.circle(key, cu, cv, h * 0.12f);
        f.frame(ln, 0.6f, u0, v0, u1, v1);
        f.line(ln, 0.6f, cu, v0, cu, v1);
        f.ring(ln, 0.6f, cu, cv, h * 0.12f);
        f.frame(ln, 0.5f, u0, cv - kw / 2, u0 + kl, cv + kw / 2);
        f.frame(ln, 0.5f, u1 - kl, cv - kw / 2, u1, cv + kw / 2);
        float hb = w * 0.06f;
        f.arcLine(ln, 0.6f, u0 + hb, cv, Math.min(ar, w * 0.4f), -80, 80);
        f.arcLine(ln, 0.6f, u1 - hb, cv, Math.min(ar, w * 0.4f), 100, 260);
        f.arcLine(ln, 0.5f, u0 + kl, cv, kw / 2, -90, 90);
        f.arcLine(ln, 0.5f, u1 - kl, cv, kw / 2, 90, 270);
        // Backboards and hoops.
        f.rect(0xFFF8F8F8, u0 + hb * 0.5f - 0.5f, cv - 3, u0 + hb * 0.5f + 0.5f, cv + 3);
        f.rect(0xFFF8F8F8, u1 - hb * 0.5f - 0.5f, cv - 3, u1 - hb * 0.5f + 0.5f, cv + 3);
        f.ring(0xFFE0602A, 0.6f, u0 + hb, cv, 1.3f);
        f.ring(0xFFE0602A, 0.6f, u1 - hb, cv, 1.3f);
        for (int i = 0; i < 5; i++) man(f, u0 + w * (0.2f + r.nextFloat() * 0.6f), v0 + h * (0.15f + r.nextFloat() * 0.7f), i < 3 ? 0xFFC83A30 : 0xFFF0F0EC);
        f.circle(0xFFE07A2A, u0 + w * (0.3f + r.nextFloat() * 0.4f), cv + (r.nextFloat() - 0.5f) * h * 0.5f, 1);
    }

    static void basketball(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Basketball Arena", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int l = Math.max(bw, bh), s = Math.min(bw, bh);
                return new int[][]{at(bw >= bh, 1, 1, l - 7, s - 2, 3)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFFC8C2B6, 0, 0, L, S);
                for (float u = 8; u < L; u += 8) f.line(0x16000000, 0.5f, u, 0, u, S);
                for (float v = 8; v < S; v += 8) f.line(0x12000000, 0.5f, 0, v, L, v);
                float a1 = (tiles(L) - 6) * T;
                // Trees in planters round the arena.
                for (float u = 8; u < a1 - 4; u += 22) {
                    f.tree(u, 7, 4.5f, 0xFF4A8A40);
                    f.tree(u + 11, S - 7, 4.5f, 0xFF4A8A40);
                }
                // The outdoor courts in a fenced cage, along v.
                float cu0 = a1 + 8, cu1 = L - 6, cv0 = 8, cv1 = S - 8;
                f.rect(0xFF3E7A5A, cu0 - 3, cv0 - 3, cu1 + 3, cv1 + 3);
                int n = cv1 - cv0 >= 140 ? 2 : 1;
                float ch = (cv1 - cv0 - (n - 1) * 8) / n;
                for (int k = 0; k < n; k++) {
                    float a = cv0 + k * (ch + 8);
                    // Courts drawn long along v: use a turned frame.
                    F g = new F(c, p, f.x(cu0, a), f.y(cu0, a), f.x(cu1, a + ch), f.y(cu1, a + ch));
                    if (g.L < g.S) g = f;
                    court(g, 0, 0, g.L, g.S, 0xFF2E5E9A, 0xFFB84A3A, r);
                }
                fence(f, 0xFF6A7078, cu0 - 4, cv0 - 4, cu1 + 4, cv1 + 4, 8);
                f.bench(a1 + 4, S / 2 - 10, false);
                f.bench(a1 + 4, S / 2 + 10, false);
                // People queueing at the arena doors.
                for (int i = 0; i < 7; i++) man(f, a1 / 2 - 15 + i * 5, S - 6 + (i % 2), KIT[i % KIT.length]);
                f.lamp(a1 + 3, 4);
                f.lamp(a1 + 3, S - 4);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, cu = L / 2, cv = S / 2;
                f.rect(0xFF8A8E94, 0, 0, L, S);
                f.roundRect(0xFF5E6268, 1, 1, L - 1, S - 1, S * 0.3f);
                f.roundRect(0xFFC4C8CC, 2.5f, 2.5f, L - 2.5f, S - 2.5f, S * 0.28f);
                // Ribs from the crown to the rim, and rings.
                for (int k = 0; k < 24; k++) {
                    double a = k * Math.PI / 12;
                    float eu = cu + (float) Math.cos(a) * (L / 2 - 4), ev = cv + (float) Math.sin(a) * (S / 2 - 4);
                    f.line(0xFFA8ACB2, 0.7f, cu, cv, eu, ev);
                }
                for (int k = 1; k <= 3; k++) {
                    float fu = (L / 2 - 4) * k / 4f, fv = (S / 2 - 4) * k / 4f;
                    f.ovalFrame(0xFF9EA2A8, 0.6f, cu - fu, cv - fv, cu + fu, cv + fv);
                }
                f.oval(0x20FFFFFF, 4, 4, L * 0.55f, S * 0.5f);
                // The ball on the crown.
                float br = Math.min(L, S) * 0.17f;
                f.circle(0x40000000, cu + 2, cv + 2, br);
                f.circle(0xFFD86A2A, cu, cv, br);
                f.circle(0xFFE88A44, cu - br * 0.25f, cv - br * 0.25f, br * 0.5f);
                f.line(0xFF2A1A10, 0.8f, cu - br, cv, cu + br, cv);
                f.line(0xFF2A1A10, 0.8f, cu, cv - br, cu, cv + br);
                f.arcLine(0xFF2A1A10, 0.8f, cu - br * 1.3f, cv, br * 0.95f, -45, 45);
                f.arcLine(0xFF2A1A10, 0.8f, cu + br * 1.3f, cv, br * 0.95f, 135, 225);
                hvac(f, 4, 4, 5);
                hvac(f, L - 13, S - 9, 5);
                f.sign(city, "ARENA", cu, S - 6, 6, 0xFFD86A2A);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.MID | Variants.UNI;
        d.minS = 10;
        d.minL = 14;
        d.base = City.OFFICE;
        d.inside = Variants.I_SEATS;
        d.roofCol = 0xFFC4C8CC;
        out.add(d);
    }

    // ------------------------------------------------------------------ 3. Velodrome

    static void velodrome(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Velodrome", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int l = Math.max(bw, bh), s = Math.min(bw, bh);
                return new int[][]{at(bw >= bh, 1, 1, l - 2, s - 3, 3)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFFCEC8BC, 0, 0, L, S);
                for (float u = 12; u < L; u += 12) f.line(0x14000000, 0.5f, u, 0, u, S);
                // A red cycle path round the building.
                f.rrFrame(0xFFA8584A, 5, 6, 6, L - 6, S - 6, 10);
                f.rrFrame(0xC0FFFFFF, 0.4f, 6, 6, L - 6, S - 6, 10);
                // Bike racks and riders along the front.
                float fv = S - 2 * T + 6;
                for (float u = 16; u < L - 16; u += 30) {
                    for (int i = 0; i < 5; i++) {
                        f.line(0xFF5A5E64, 0.5f, u + i * 2.4f, fv + 3, u + i * 2.4f, fv + 9);
                        f.line(KIT[(i + (int) u) % KIT.length], 0.9f, u + i * 2.4f + 0.6f, fv + 4, u + i * 2.4f + 0.6f, fv + 8);
                    }
                    f.tree(u + 20, fv + 8, 4.5f, 0xFF4A8A40);
                }
                man(f, L * 0.3f, S - 4, 0xFFE8C030);
                man(f, L * 0.62f, S - 3.5f, 0xFF2E5EA8);
                f.lamp(3, 3);
                f.lamp(L - 3, 3);
                f.lamp(3, S - 3);
                f.lamp(L - 3, S - 3);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, cu = L / 2, cv = S / 2;
                f.rect(0xFFB8B2A6, 0, 0, L, S);
                for (float u = 6; u < L; u += 6) f.line(0x16000000, 0.5f, u, 0, u, S);
                // The roof shell: a stadium of standing-seam metal with ribs.
                f.roundRect(0xFF6E747A, 0.5f, 0.5f, L - 0.5f, S - 0.5f, S / 2);
                f.roundRect(0xFFB8C0C8, 2, 2, L - 2, S - 2, S / 2 - 1.5f);
                for (int k = 0; k < 40; k++) {
                    float[] a = stad(2, 2, L - 2, S - 2, k / 40f), b = stad(S * 0.17f, S * 0.17f, L - S * 0.17f, S * 0.83f, k / 40f);
                    f.line(0xFF98A0A8, 0.6f, a[0], a[1], b[0], b[1]);
                }
                f.rrFrame(0x30000000, 1.5f, 3, 3, L - 3, S - 3, S / 2 - 2.5f);
                // Through the glass crown: the banked wooden track.
                float g = S * 0.17f;
                f.roundRect(0xFF404448, g, g, L - g, S - g, S / 2 - g);
                int bands = 6;
                float tw = S * 0.13f;
                for (int i = 0; i < bands; i++) {
                    float in = g + 1 + tw * i / bands;
                    f.roundRect(shade(0xFFC08A54, 0.82f + i * 0.06f), in, in, L - in, S - in, Math.max(1, S / 2 - in));
                }
                float ti = g + 1 + tw;
                f.roundRect(0xFF3A78B8, ti, ti, L - ti, S - ti, Math.max(1, S / 2 - ti));
                float ii = ti + 1.6f;
                f.roundRect(0xFF5E8A72, ii, ii, L - ii, S - ii, Math.max(1, S / 2 - ii));
                f.rrFrame(0xFF1A1A1A, 0.4f, ti - 1, ti - 1, L - ti + 1, S - ti + 1, Math.max(1, S / 2 - ti + 1));
                f.rrFrame(0xFFC83A30, 0.4f, ti - 3, ti - 3, L - ti + 3, S - ti + 3, Math.max(1, S / 2 - ti + 3));
                f.rrFrame(0xFF2E5EA8, 0.4f, g + 1 + tw * 0.35f, g + 1 + tw * 0.35f, L - g - 1 - tw * 0.35f, S - g - 1 - tw * 0.35f, Math.max(1, S / 2 - g - 1 - tw * 0.35f));
                // Team pens in the infield.
                for (float u = cu - (L - 2 * ii) * 0.25f; u < cu + (L - 2 * ii) * 0.25f; u += 9)
                    f.box(KIT[(int) (u / 9) % KIT.length], u, cv - 2, u + 6, cv + 2, 0.6f);
                // Riders in a paceline and a breakaway.
                for (int i = 0; i < 9; i++) {
                    float t = 0.1f + i * 0.012f + (i > 6 ? 0.3f : 0);
                    float in = g + 1 + tw * (i > 6 ? 0.55f : 0.25f);
                    float[] q = stad(in, in, L - in, S - in, t);
                    f.circle(0x40000000, q[0] + 0.6f, q[1] + 0.6f, 1);
                    f.circle(KIT[i % KIT.length], q[0], q[1], 1);
                }
                // The glazing over it all.
                f.roundRect(0x30A8D0E8, g, g, L - g, S - g, S / 2 - g);
                for (float u = g + 6; u < L - g; u += 7) f.line(0x40FFFFFF, 0.5f, u, g, u, S - g);
                f.line(0x50FFFFFF, 0.6f, g, cv, L - g, cv);
                f.rrFrame(0xFF8A9298, 1.2f, g, g, L - g, S - g, S / 2 - g);
                f.oval(0x24FFFFFF, g + 4, g + 2, L * 0.4f, cv - 2);
                f.sign(city, "VELODROME", cu, S - 5, 5, 0xFF3A4048);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.MID | Variants.SUB | Variants.UNI | Variants.PARK;
        d.minS = 11;
        d.minL = 16;
        d.base = City.OFFICE;
        d.inside = Variants.I_SEATS;
        d.roofCol = 0xFFB8C0C8;
        out.add(d);
    }

    // ------------------------------------------------------------------ 4. Lido

    static void lido(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Lido", T_PARK) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int l = Math.max(bw, bh), s = Math.min(bw, bh);
                boolean a = bw >= bh;
                return new int[][]{at(a, 1, 1, 3, s - 2, 2), at(a, l - 3, 1, 2, 2, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFFE6DCC6, 0, 0, L, S);
                for (float u = 6; u < L; u += 6) f.line(0x10000000, 0.5f, u, 0, u, S);
                for (float v = 6; v < S; v += 6) f.line(0x10000000, 0.5f, 0, v, L, v);
                float pu0 = 5.5f * T, pu1 = L - 4.5f * T, pv0 = 1.6f * T, pv1 = S - 1.6f * T;
                // Sun terraces: stepped decks along the far end and the sides.
                float tu0 = pu1 + 12;
                for (int i = 0; i < 4; i++) {
                    float a = tu0 + i * (L - T - tu0) / 4f;
                    f.rect(shade(0xFFD8CCB0, 1 - i * 0.04f), a, 3 * T + 2, L - 3, S - 4);
                    f.line(0x30000000, 0.6f, a, 3 * T + 2, a, S - 4);
                }
                // The pool: cream coping, blue tiled edge, a deep end and a sunburst.
                f.roundRect(0x38000000, pu0 - 3, pv0 - 2, pu1 + 5, pv1 + 5, 10);
                f.roundRect(0xFFF2EAD8, pu0 - 4, pv0 - 4, pu1 + 4, pv1 + 4, 10);
                f.roundRect(0xFF2A6E96, pu0 - 1.2f, pv0 - 1.2f, pu1 + 1.2f, pv1 + 1.2f, 7);
                f.roundRect(0xFF6CC4E0, pu0, pv0, pu1, pv1, 6);
                f.roundRect(0xFF4AA6CE, pu0 + (pu1 - pu0) * 0.55f, pv0, pu1, pv1, 6);
                float du = pu0 + (pu1 - pu0) * 0.8f, dv = (pv0 + pv1) / 2, rr = Math.min(pu1 - pu0, pv1 - pv0) * 0.22f;
                for (int k = 0; k < 16; k++) {
                    double a0 = k * Math.PI / 8, a1 = a0 + Math.PI / 16;
                    f.poly(0x302A6E96, du, dv, du + (float) Math.cos(a0) * rr, dv + (float) Math.sin(a0) * rr, du + (float) Math.cos(a1) * rr, dv + (float) Math.sin(a1) * rr);
                }
                f.ring(0x402A6E96, 1, du, dv, rr * 0.4f);
                for (int i = 0; i < 26; i++) {
                    float u = pu0 + 3 + r.nextFloat() * (pu1 - pu0 - 6), v = pv0 + 3 + r.nextFloat() * (pv1 - pv0 - 6);
                    f.curve(0x60FFFFFF, 0.5f, u, v, u + 2, v - 1, u + 4, v);
                }
                // Steps into the shallow end, ladders at the corners.
                for (int i = 0; i < 3; i++) f.rect(shade(0xFF8AD0E8, 1.05f - i * 0.05f), pu0, dv - 10 + i * 0, pu0 + 2 + i * 2, dv + 10);
                for (int sd = 0; sd < 2; sd++) {
                    float lv = sd == 0 ? pv0 : pv1;
                    for (int k = 0; k < 2; k++) {
                        float lu = pu0 + (pu1 - pu0) * (0.35f + k * 0.4f);
                        f.line(0xFFD8DCE0, 0.6f, lu, lv - 1.5f, lu, lv + (sd == 0 ? 2 : -2));
                        f.line(0xFFD8DCE0, 0.6f, lu + 2.5f, lv - 1.5f, lu + 2.5f, lv + (sd == 0 ? 2 : -2));
                    }
                }
                // The diving stage: a stepped concrete tower with boards out over the deep end.
                float tu = pu1 + 1, tv = dv, th = Math.min(13, (pv1 - pv0) * 0.2f);
                // Three boards out over the water, the highest the longest, each with its shadow.
                float[] bl = {9, 14, 19}, bo = {-th * 0.62f, 0, th * 0.62f};
                for (int i = 0; i < 3; i++) {
                    float sh = 2 + i * 2.5f;
                    f.rect(0x30000000, tu - bl[i] + sh, tv + bo[i] - 1.2f + sh, tu + sh, tv + bo[i] + 1.2f + sh);
                }
                f.box(0xFFD8D0C0, tu - 2, tv - th, tu + 9, tv + th, 3);
                f.box(0xFFE8E0D0, tu - 1, tv - th * 0.75f, tu + 7, tv + th * 0.75f, 2);
                f.box(0xFFF4EEE2, tu, tv - th * 0.45f, tu + 5, tv + th * 0.45f, 1.5f);
                for (int i = 0; i < 3; i++) {
                    f.rect(i == 2 ? 0xFF2A6E96 : 0xFFF8F6F0, tu - bl[i], tv + bo[i] - 1.2f, tu + 1, tv + bo[i] + 1.2f);
                    f.line(0x40000000, 0.4f, tu - bl[i], tv + bo[i] + 1.2f, tu + 1, tv + bo[i] + 1.2f);
                }
                for (int k = 0; k < 4; k++) f.line(0xFFA89E8A, 0.4f, tu + 6 + k * 0.8f, tv + th * 0.5f, tu + 6 + k * 0.8f, tv + th - 1);
                man(f, tu + 3, tv - 2, 0xFF2A6E96);
                // Swimmers and a diver.
                for (int i = 0; i < 8; i++) {
                    float u = pu0 + 6 + r.nextFloat() * (pu1 - pu0) * 0.6f, v = pv0 + 6 + r.nextFloat() * (pv1 - pv0 - 12);
                    f.circle(0x50FFFFFF, u - 1, v, 2.4f);
                    f.circle(SKIN[i % SKIN.length], u, v, 1.2f);
                    if (i % 3 == 0) f.circle(KIT[i % KIT.length], u - 0.2f, v - 0.2f, 0.9f);
                }
                f.ring(0x80FFFFFF, 0.6f, du - 4, dv + 3, 2.5f);
                f.ring(0x60FFFFFF, 0.5f, du - 4, dv + 3, 4);
                // Loungers, towels and parasols on the terraces.
                int q = 0;
                for (float v = 3 * T + 10; v < S - 10; v += 19) {
                    for (float u = tu0 + 7; u < L - 9; u += 17) {
                        q++;
                        float ju = r.nextFloat() * 3, jv = r.nextFloat() * 3;
                        int kind = r.nextInt(4);
                        if (kind == 0) continue;
                        if (kind == 1) {
                            f.box(KIT[q % KIT.length], u + ju, v + jv, u + ju + 4, v + jv + 8, 0.5f);
                            man(f, u + ju + 2, v + jv + 3, KIT[(q + 3) % KIT.length]);
                        } else {
                            parasol(f, u + ju, v + jv, 5, 0xFFE8E2D4, q % 2 == 0 ? 0xFF3A86B0 : 0xFFE07A5A);
                            f.box(0xFFF8F8F4, u + ju + 4, v + jv + 3, u + ju + 11, v + jv + 6, 0.6f);
                            f.rect(0xFF3A86B0, u + ju + 4.5f, v + jv + 3.5f, u + ju + 7, v + jv + 5.5f);
                        }
                    }
                }
                for (float u = pu0 + 4; u < pu1 - 6; u += 13) {
                    f.box(KIT[(int) (u / 13) % KIT.length], u, pv0 - 13, u + 4, pv0 - 6, 0.5f);
                    if ((int) (u / 13) % 2 == 0) man(f, u + 2, pv0 - 9.5f, KIT[(int) (u / 13 + 2) % KIT.length]);
                    f.box(0xFFF8F8F4, u + 3, pv1 + 6, u + 10, pv1 + 9, 0.5f);
                }
                // Palms in tubs at the corners.
                float[][] pal = {{pu0 - 10, 6}, {pu0 - 10, S - 6}, {tu0 - 4, 6}};
                for (float[] pq : pal) {
                    f.box(0xFF4A90B0, pq[0] - 3, pq[1] - 3, pq[0] + 3, pq[1] + 3, 1);
                    for (int k = 0; k < 7; k++) {
                        double a = k * Math.PI * 2 / 7;
                        f.line(0xFF4E8A3E, 1.4f, pq[0], pq[1], pq[0] + (float) Math.cos(a) * 6, pq[1] + (float) Math.sin(a) * 6);
                    }
                    f.circle(0xFF6A5A3A, pq[0], pq[1], 1);
                }
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, cu = L / 2, cv = S / 2;
                if (part == 1) {
                    // The cafe kiosk under a striped awning.
                    for (int i = 0; i * 4 < L; i++) f.rect(i % 2 == 0 ? 0xFFE8E2D4 : 0xFF3A86B0, i * 4, 0, Math.min(L, i * 4 + 4), S);
                    f.rect(0x30000000, 0, S * 0.5f, L, S);
                    f.frame(0xFF2A6E96, 0.8f, 0, 0, L, S);
                    return;
                }
                // The art-deco pavilion: cream render, blue bands, porthole lights and a stepped tower.
                f.rect(0xFFD8CEB8, 0, 0, L, S);
                f.rect(0xFFF2EAD8, 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                f.frame(0xFF4A90B0, 1, 3, 3, L - 3, S - 3);
                for (float u = 9; u < L - 6; u += 9) {
                    f.circle(0xFFB8AE98, u, 7, 2.2f);
                    f.circle(0xFF9ACCE0, u, 7, 1.6f);
                    f.circle(0xFFB8AE98, u, S - 7, 2.2f);
                    f.circle(0xFF9ACCE0, u, S - 7, 1.6f);
                }
                for (int i = 0; i < 3; i++) {
                    float hw = S * 0.22f - i * S * 0.06f, hl = L * 0.22f - i * L * 0.05f;
                    f.box(shade(0xFFF6F0E2, 0.92f + i * 0.04f), cu - hl, cv - hw, cu + hl, cv + hw, 1.2f);
                }
                for (int k = -2; k <= 2; k++) f.line(0xFF4A90B0, 0.7f, cu + k * L * 0.05f, cv - S * 0.1f, cu + k * L * 0.05f, cv + S * 0.1f);
                f.circle(0xFF505050, cu, cv, 0.8f);
                f.rect(0xFF3A86B0, cu + 0.5f, cv - 1, cu + 5, cv + 1);
                f.sign(city, "LIDO", cu, S - 10, 6, 0xFF2A6E96);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.MID | Variants.OLD | Variants.SUB | Variants.PARK;
        d.minS = 10;
        d.minL = 14;
        d.base = City.SHOP;
        d.inside = Variants.I_BATH;
        d.roofCol = 0xFFF2EAD8;
        out.add(d);
    }
    // ------------------------------------------------------------------ 5. Climbing Centre

    /** A freestanding bouldering block: faceted concrete sides studded with holds, a flat top-out. */
    static void boulder(F f, float u, float v, float rad, Random r, int n) {
        float[] pu = new float[n], pv = new float[n], qu = new float[n], qv = new float[n];
        for (int i = 0; i < n; i++) {
            double a = i * 2 * Math.PI / n + r.nextFloat() * 0.3f;
            float k = rad * (0.8f + 0.3f * r.nextFloat());
            pu[i] = u + (float) Math.cos(a) * k;
            pv[i] = v + (float) Math.sin(a) * k;
            qu[i] = u - rad * 0.12f + (pu[i] - u) * 0.42f;
            qv[i] = v - rad * 0.12f + (pv[i] - v) * 0.42f;
        }
        // Crash mats round the foot.
        float[] mat = new float[2 * n];
        for (int i = 0; i < n; i++) {
            mat[2 * i] = u + (pu[i] - u) * 1.35f;
            mat[2 * i + 1] = v + (pv[i] - v) * 1.35f;
        }
        f.poly(0x38000000, shift(mat, 1.2f));
        f.poly(0xFF2F5FA0, mat);
        for (int i = 0; i < n; i++) f.line(0xFF244C84, 0.6f, pu[i], pv[i], mat[2 * i], mat[2 * i + 1]);
        float[] sh = new float[2 * n];
        for (int i = 0; i < n; i++) {
            sh[2 * i] = pu[i] + rad * 0.3f;
            sh[2 * i + 1] = pv[i] + rad * 0.35f;
        }
        f.poly(0x40000000, sh);
        int[] routes = {0xFFE8402A, 0xFFF0C020, 0xFF30A050, 0xFF2E7AD0, 0xFFE860B0, 0xFFF08A20, 0xFF8A4AC8, 0xFFF4F4F4};
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            double nm = Math.atan2((pv[i] + pv[j]) / 2 - v, (pu[i] + pu[j]) / 2 - u);
            float lt = 0.78f + 0.3f * (float) Math.cos(nm - Math.toRadians(225));
            f.poly(shade(0xFFA8A49C, lt), pu[i], pv[i], pu[j], pv[j], qu[j], qv[j], qu[i], qv[i]);
            int c1 = routes[r.nextInt(routes.length)], c2 = routes[r.nextInt(routes.length)];
            for (int k = 0; k < 6; k++) {
                float a = r.nextFloat(), b = 0.1f + r.nextFloat() * 0.8f;
                float hu = (pu[i] + (pu[j] - pu[i]) * a) * (1 - b) + (qu[i] + (qu[j] - qu[i]) * a) * b;
                float hv = (pv[i] + (pv[j] - pv[i]) * a) * (1 - b) + (qv[i] + (qv[j] - qv[i]) * a) * b;
                f.circle(0x50000000, hu + 0.3f, hv + 0.3f, 0.75f);
                f.circle(k % 3 == 0 ? c2 : c1, hu, hv, 0.5f + r.nextFloat() * 0.5f);
            }
        }
        float[] top = new float[2 * n];
        for (int i = 0; i < n; i++) {
            top[2 * i] = qu[i];
            top[2 * i + 1] = qv[i];
        }
        f.poly(0xFFC4C0B6, top);
        f.line(0xFFDCD8CE, 0.6f, qu[0], qv[0], qu[1], qv[1]);
    }

    static float[] shift(float[] q, float d) {
        float[] o = new float[q.length];
        for (int i = 0; i < q.length; i++) o[i] = q[i] + d;
        return o;
    }

    static void climbing(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Climbing Centre", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int l = Math.max(bw, bh), s = Math.min(bw, bh);
                return new int[][]{at(bw >= bh, 1, 1, l / 2 - 1, s - 2, 3)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float b1 = (tiles(L) / 2) * T;
                f.rect(0xFFC4BEB0, 0, 0, L, S);
                for (float u = 8; u < L; u += 8) f.line(0x12000000, 0.5f, u, 0, u, S);
                // The bark-chip safety ground of the bouldering park.
                float z0 = b1 + 14, z1 = L - 5, w0 = 5, w1 = S - 5;
                f.roundRect(0xFF6E5A44, z0 - 2, w0 - 2, z1 + 2, w1 + 2, 6);
                f.roundRect(0xFF8A6E50, z0, w0, z1, w1, 5);
                for (int i = 0; i < 40; i++) {
                    float a = z0 + 1 + r.nextFloat() * (z1 - z0 - 2), b = w0 + 1 + r.nextFloat() * (w1 - w0 - 2);
                    f.rect(r.nextBoolean() ? 0xFFA08262 : 0xFF6A523C, a, b, a + 1.2f, b + 0.6f);
                }
                // A long training wall facing the building, studded with holds.
                float wu = b1 + 5;
                f.rect(0x40000000, wu + 1.5f, 8, wu + 7.5f, S - 6);
                f.rect(0xFF8E8880, wu, 6, wu + 6, S - 8);
                f.rect(0xFFB4AEA4, wu, 6, wu + 1.5f, S - 8);
                for (float v = 9; v < S - 10; v += 3.4f) {
                    f.circle(KIT[(int) (v * 3) % 6], wu + 2.5f + r.nextFloat() * 3, v, 0.6f + r.nextFloat() * 0.4f);
                }
                f.rect(0xFF2F5FA0, wu - 6, 8, wu - 0.5f, S - 10);
                for (float v = 8 + 14; v < S - 10; v += 14) f.line(0xFF244C84, 0.5f, wu - 6, v, wu - 0.5f, v);
                // The big boulder and a smaller one.
                float zl = z1 - z0, zs = w1 - w0;
                float big = Math.min(zl, zs) * 0.28f;
                if (zl > zs * 1.5f) {
                    boulder(f, z0 + zl * 0.32f, w0 + zs * 0.5f, big, r, 8);
                    boulder(f, z0 + zl * 0.78f, w0 + zs * 0.4f, big * 0.6f, r, 6);
                } else {
                    boulder(f, z0 + zl * 0.5f, w0 + zs * 0.36f, big * 1.1f, r, 8);
                    boulder(f, z0 + zl * 0.45f, w0 + zs * 0.8f, Math.min(big * 0.6f, zs * 0.12f), r, 6);
                }
                // Climbers on the blocks, spotters and onlookers.
                man(f, z0 + zl * 0.32f + big * 0.7f, w0 + zs * 0.5f, 0xFFE8402A);
                man(f, z0 + zl * 0.5f - big * 0.2f, w0 + zs * 0.36f - big * 0.75f, 0xFF30A050);
                man(f, wu + 3, S / 2, 0xFFF0C020);
                man(f, wu - 3, S / 2 + 3, 0xFF2E7AD0);
                man(f, z0 + 4, w1 - 6, 0xFF8A4AC8);
                man(f, z1 - 6, w0 + 6, 0xFFF08A20);
                f.bench(b1 + 3, 6, false);
                f.bench(z1 - 10, w1 + 2.5f, true);
                f.tree(L - 6, 6, 5, 0xFF4A8A40);
                f.tree(L - 6, S - 6, 5, 0xFF4A8A40);
                f.lamp(b1 + 2, S - 4);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, m = S / 2;
                tin(f, 0xFF5A6A7A, 0, 0, L, m, 3);
                tin(f, 0xFF46545F, 0, m, L, S, 3);
                f.line(0xFF8A98A6, 1.2f, 0, m, L, m);
                for (float u = 8; u < L - 10; u += 20) {
                    f.rect(0xC0B8D8E8, u, 3, u + 8, m - 3);
                    f.rect(0xA08AB0C4, u, m + 3, u + 8, S - 3);
                }
                // A painted logo at the end: two peaks in a ring.
                float lc = L - Math.min(16, L * 0.18f), lr = Math.min(S * 0.32f, 11);
                f.circle(0x40000000, lc + 1, m + 1, lr + 1);
                f.circle(0xFFF2EFE6, lc, m, lr + 1);
                f.circle(0xFF2E7AD0, lc, m, lr);
                f.poly(0xFF8A96A4, lc - lr * 0.85f, m + lr * 0.5f, lc - lr * 0.25f, m - lr * 0.55f, lc + lr * 0.25f, m + lr * 0.5f);
                f.poly(0xFF6A7684, lc - lr * 0.2f, m + lr * 0.5f, lc + lr * 0.35f, m - lr * 0.3f, lc + lr * 0.85f, m + lr * 0.5f);
                f.poly(0xFFF8F8F4, lc - lr * 0.42f, m - lr * 0.24f, lc - lr * 0.25f, m - lr * 0.55f, lc - lr * 0.08f, m - lr * 0.24f);
                f.poly(0xFFF8F8F4, lc + lr * 0.22f, m - lr * 0.1f, lc + lr * 0.35f, m - lr * 0.3f, lc + lr * 0.48f, m - lr * 0.1f);
                f.rect(0xFF8A6E50, lc - lr * 0.86f, m + lr * 0.5f, lc + lr * 0.86f, m + lr * 0.62f);
                hvac(f, 4, m + 4, 5);
                f.frame(0xFF2E363E, 1, 0, 0, L, S);
                f.sign(city, "CLIMBING", L / 2, m, 5, 0xFFE8402A);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.MID | Variants.SUB | Variants.IND | Variants.UNI;
        d.minS = 9;
        d.minL = 14;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_GYM;
        d.roofCol = 0xFF5A6A7A;
        out.add(d);
    }

    // ------------------------------------------------------------------ 6. Golf Driving Range

    static void drivingRange(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Golf Driving Range", T_PARK) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int s = Math.min(bw, bh);
                return new int[][]{at(bw >= bh, 1, 1, 2, s - 2, 2)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF5A9A48, 0, 0, L, S);
                float r0 = 3 * T + 8;
                for (float u = r0; u < L; u += 12) f.rect(((int) ((u - r0) / 12)) % 2 == 0 ? 0xFF66A852 : 0xFF5C9E4A, u, 0, Math.min(L, u + 12), S);
                // The mats in front of the bays, a golfer at each.
                f.rect(0xFF9A9488, 3 * T, T, r0, S - T);
                int nb = (int) ((S - 2 * T) / 12);
                float bh = (S - 2 * T) / nb;
                for (int i = 0; i < nb; i++) {
                    float v = T + i * bh;
                    f.rect(0xFF3E7A3E, 3 * T + 1, v + 1, r0 - 1, v + bh - 1);
                    f.rect(0xFF2E5E30, 3 * T + 1, v + bh * 0.6f, r0 - 1, v + bh - 1);
                    if (i % 4 != 2) {
                        man(f, 3 * T + 3.5f, v + bh * 0.4f, KIT[i % KIT.length]);
                        f.line(0xFFB8BCC0, 0.4f, 3 * T + 4.5f, v + bh * 0.4f, 3 * T + 6.5f, v + bh * 0.4f + 1);
                        f.circle(0xFFFFFFFF, 3 * T + 6.8f, v + bh * 0.4f + 1.4f, 0.5f);
                    }
                    f.circle(0xFF2A2A2A, 3 * T + 2, v + bh * 0.8f, 1.2f);
                    f.circle(0xFFE8E8E4, 3 * T + 2, v + bh * 0.8f, 0.8f);
                }
                // Target greens with bunkers and flags.
                float rl = L - r0 - 8;
                int ng = Math.max(3, (int) (rl / 45));
                for (int k = 0; k < ng; k++) {
                    float gu = r0 + rl * (k + 0.6f) / ng, gv = S * (k % 2 == 0 ? 0.35f : 0.65f), gr = Math.min(S * 0.14f, 9 + k * 2);
                    if (ng > 3 && k == ng - 1) gv = S * 0.5f;
                    ellipse(f, 0xFF7EBE62, gu, gv, gr * 1.2f, gr);
                    f.ovalFrame(0xFF5AA04A, 0.8f, gu - gr * 1.2f, gv - gr, gu + gr * 1.2f, gv + gr);
                    f.ovalFrame(0xC0FFFFFF, 0.5f, gu - gr * 0.8f, gv - gr * 0.66f, gu + gr * 0.8f, gv + gr * 0.66f);
                    f.ovalFrame(0xC0FFFFFF, 0.5f, gu - gr * 0.4f, gv - gr * 0.33f, gu + gr * 0.4f, gv + gr * 0.33f);
                    f.oval(0xFFE4D6A8, gu + gr * 0.7f, gv + gr * 0.4f, gu + gr * 1.6f, gv + gr * 1.1f);
                    f.circle(0x40000000, gu + 0.6f, gv + 0.6f, 0.6f);
                    f.line(0xFFF0F0F0, 0.5f, gu, gv, gu, gv - 6);
                    f.poly(k % 2 == 0 ? 0xFFE84030 : 0xFFF0C020, gu, gv - 6, gu + 3.5f, gv - 5, gu, gv - 4);
                }
                // Yardage boards down both sides.
                for (int k = 1; k <= 4; k++) {
                    float u = r0 + rl * k / 4.6f;
                    for (int sd = 0; sd < 2; sd++) {
                        float v = sd == 0 ? 6 : S - 6;
                        f.box(0xFFF4F4F0, u - 3, v - 2, u + 3, v + 2, 0.8f);
                        f.circle(KIT[k], u, v, 1.2f);
                    }
                }
                // Balls everywhere, thickest near the bays.
                for (int i = 0; i < 160; i++) {
                    float q = r.nextFloat();
                    float u = r0 + 3 + q * q * (rl - 3), v = 3 + r.nextFloat() * (S - 6);
                    f.circle(0xFFF8F8F6, u, v, 0.45f);
                }
                // The ball picker with its cage and gang of rollers.
                float pu = r0 + rl * 0.55f, pv = S * 0.5f;
                f.box(0xFF6A6E70, pu - 9, pv - 5, pu - 4, pv + 5, 0.8f);
                for (int i = 0; i < 4; i++) f.line(0xFF4A4E50, 0.6f, pu - 8.5f + i * 1.3f, pv - 5, pu - 8.5f + i * 1.3f, pv + 5);
                f.box(0xFF3A7A3A, pu - 3, pv - 2.5f, pu + 4, pv + 2.5f, 1);
                f.frame(0xFFC0C4C8, 0.6f, pu - 2.5f, pv - 2, pu + 1.5f, pv + 2);
                f.circle(0xFF2A2A2A, pu + 2.8f, pv, 1.2f);
                // The tall netting round the range.
                for (float u = r0; u <= L - 1; u += 16) {
                    f.circle(0x50000000, u + 1.5f, 3.5f, 1.2f);
                    f.circle(0xFF4A4E52, u, 2, 1.1f);
                    f.circle(0x50000000, u + 1.5f, S - 0.5f, 1.2f);
                    f.circle(0xFF4A4E52, u, S - 2, 1.1f);
                }
                for (float v = 2; v <= S - 2; v += 16) f.circle(0xFF4A4E52, L - 2, v, 1.1f);
                f.line(0xB0D8DCE0, 0.6f, r0, 2, L - 2, 2);
                f.line(0xB0D8DCE0, 0.6f, r0, S - 2, L - 2, S - 2);
                f.line(0xB0D8DCE0, 0.6f, L - 2, 2, L - 2, S - 2);
                f.line(0x50D8DCE0, 3, r0, 3.5f, L - 3.5f, 3.5f);
                f.line(0x50D8DCE0, 3, r0, S - 3.5f, L - 3.5f, S - 3.5f);
                f.line(0x50D8DCE0, 3, L - 3.5f, 3.5f, L - 3.5f, S - 3.5f);
                f.tree(8, 6, 5, 0xFF3E7A3A);
                f.tree(8, S - 6, 5, 0xFF3E7A3A);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                // Turned so that u runs down the line of bays and v points at the range.
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF2E4A38, 0, 0, L, S);
                f.rect(0xFF3E5E48, 0, 0, L, S * 0.5f);
                f.rect(0xFF4A6E56, 0, S * 0.5f, L, S - 2);
                int nb = (int) (L / 12);
                for (int i = 1; i < nb; i++) f.line(0xFF2A3E30, 0.7f, i * L / nb, 0, i * L / nb, S);
                f.rect(0xFFF2F2EE, 0, S - 2, L, S);
                f.line(0xFF8ABE6E, 1.2f, 0, S * 0.5f, L, S * 0.5f);
                f.sign(city, "DRIVING RANGE", L / 2, S / 2, 5, 0xFFF2F2EE);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.SUB | Variants.PARK | Variants.MID;
        d.minS = 9;
        d.minL = 16;
        d.base = City.SHOP;
        d.inside = Variants.I_CAFE;
        d.roofCol = 0xFF3E5E48;
        out.add(d);
    }

    static void ellipse(F f, int col, float u, float v, float ru, float rv) {
        f.oval(col, u - ru, v - rv, u + ru, v + rv);
    }

    // ------------------------------------------------------------------ 7. Bowls & Croquet Club

    static void bowls(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Bowls & Croquet Club", T_PARK) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int l = Math.max(bw, bh);
                return new int[][]{at(bw >= bh, l / 2 - 2, 1, 4, 2, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, m = (tiles(L) / 2) * T;
                f.rect(0xFF5A9446, 0, 0, L, S);
                // Gravel walks and flower borders along the top.
                f.rect(0xFFD8CCB0, 0, 3 * T + 2, L, 3 * T + 8);
                f.rect(0xFFD8CCB0, m - 4, 3 * T, m + 4, S);
                f.rect(0xFFC8B898, m - 2 * T - 4, 3 * T, m + 2 * T + 4, 3 * T + 8);
                for (int sd = 0; sd < 2; sd++) {
                    float a = sd == 0 ? 6 : m + 2 * T + 6, b = sd == 0 ? m - 2 * T - 6 : L - 6;
                    if (b - a < 10) continue;
                    f.rect(0xFF6A5038, a, 8, b, 3 * T - 6);
                    flowers(f, r, (a + b) / 2, 14, Math.min((b - a) / 2, 12), (int) ((b - a) / 2), 0xFFE04A5A);
                    flowers(f, r, (a + b) / 2, 20, Math.min((b - a) / 2, 12), (int) ((b - a) / 3), 0xFFF0E0E8);
                    for (float u = a + 4; u < b - 3; u += 7) f.bush(u, 3 * T - 8, 2.2f);
                    f.bench((a + b) / 2, 3 * T - 1, true);
                }
                // The bowling green: a ditch, a bank and rinks.
                float g0 = 7, g1 = m - 8, gv0 = 3 * T + 13, gv1 = S - 7;
                f.rect(0xFF4A7A3A, g0 - 4, gv0 - 4, g1 + 4, gv1 + 4);
                f.rect(0xFFC4B898, g0 - 2, gv0 - 2, g1 + 2, gv1 + 2);
                f.rect(0xFF6CB25A, g0, gv0, g1, gv1);
                int nr = Math.max(3, (int) ((gv1 - gv0) / 16));
                float rh = (gv1 - gv0) / nr;
                for (int i = 0; i < nr; i++) {
                    if (i % 2 == 1) f.rect(0xFF64A852, g0, gv0 + i * rh, g1, gv0 + (i + 1) * rh);
                    float v = gv0 + (i + 0.5f) * rh;
                    f.circle(0xFFF2F2EE, g0 + 1, gv0 + i * rh, 0.5f);
                    f.rect(0xFF2A2A2A, g0 + 4, v - 1, g0 + 7, v + 1);
                    if (i % 2 == 0 && g1 - g0 > 30) {
                        float ju = g1 - 10 - r.nextFloat() * 8;
                        f.circle(0xFFF8F8F4, ju, v + (r.nextFloat() - 0.5f) * 4, 0.6f);
                        for (int k = 0; k < 4; k++) {
                            float bu = ju - 4 + r.nextFloat() * 8, bv = v + (r.nextFloat() - 0.5f) * rh * 0.6f;
                            f.circle(0x40000000, bu + 0.4f, bv + 0.4f, 1.2f);
                            f.circle(k % 2 == 0 ? 0xFF2A2420 : 0xFF6A3A2A, bu, bv, 1.2f);
                            f.circle(0x60FFFFFF, bu - 0.4f, bv - 0.4f, 0.4f);
                        }
                        man(f, g0 + 5.5f, v + 3, 0xFFF4F4F0);
                        man(f, g1 - 6, v - 3, 0xFFF4F4F0);
                    }
                }
                // The croquet lawn: chequer-mown, hoops, the peg and the four balls.
                float k0 = m + 8, k1 = L - 7;
                f.rect(0xFF4A7A3A, k0 - 3, gv0 - 3, k1 + 3, gv1 + 3);
                f.rect(0xFF72B85E, k0, gv0, k1, gv1);
                float cs = 10;
                for (float u = k0; u < k1; u += cs)
                    for (float v = gv0; v < gv1; v += cs)
                        if (((int) ((u - k0) / cs) + (int) ((v - gv0) / cs)) % 2 == 0) f.rect(0x14000000, u, v, Math.min(k1, u + cs), Math.min(gv1, v + cs));
                f.frame(0xFFF2F2EE, 0.6f, k0 + 2, gv0 + 2, k1 - 2, gv1 - 2);
                float kw = k1 - k0, kh = gv1 - gv0;
                float[][] hp = {{0.17f, 0.2f}, {0.83f, 0.2f}, {0.17f, 0.8f}, {0.83f, 0.8f}, {0.5f, 0.38f}, {0.5f, 0.62f}};
                for (float[] h : hp) {
                    float hu = k0 + kw * h[0], hv = gv0 + kh * h[1];
                    f.line(0x50000000, 0.8f, hu - 2 + 0.5f, hv + 0.6f, hu + 2 + 0.5f, hv + 0.6f);
                    f.line(0xFFF4F4F4, 0.8f, hu - 2, hv, hu + 2, hv);
                    f.circle(0xFFF4F4F4, hu - 2, hv, 0.6f);
                    f.circle(0xFFF4F4F4, hu + 2, hv, 0.6f);
                }
                float pu = k0 + kw / 2, pv = gv0 + kh / 2;
                f.circle(0x50000000, pu + 0.6f, pv + 0.6f, 1.2f);
                f.circle(0xFF2E5EA8, pu, pv, 1.2f);
                f.circle(0xFFC83A30, pu, pv, 0.85f);
                f.circle(0xFF2A2A2A, pu, pv, 0.55f);
                f.circle(0xFFE8C030, pu, pv, 0.3f);
                int[] bc = {0xFF2E5EA8, 0xFFC83A30, 0xFF2A2A2A, 0xFFE8C030};
                for (int k = 0; k < 4; k++) {
                    float bu = k0 + kw * (0.2f + r.nextFloat() * 0.6f), bv = gv0 + kh * (0.2f + r.nextFloat() * 0.6f);
                    f.circle(0x40000000, bu + 0.4f, bv + 0.4f, 1.1f);
                    f.circle(bc[k], bu, bv, 1.1f);
                }
                man(f, k0 + kw * 0.3f, gv0 + kh * 0.45f, 0xFFF4F4F0);
                f.line(0xFF6A4A2A, 0.6f, k0 + kw * 0.3f + 1, gv0 + kh * 0.45f + 1, k0 + kw * 0.3f + 4, gv0 + kh * 0.45f + 3);
                man(f, k1 - 5, gv1 - 6, 0xFFF4F4F0);
                // The veranda before the pavilion with tea tables.
                for (int k = -1; k <= 1; k += 2) {
                    float tu = m + k * T * 1.3f, tv = 3 * T + 5;
                    f.circle(0xFFF4F0E8, tu, tv, 2);
                    man(f, tu - 3, tv, 0xFFF4F4F0);
                    man(f, tu + 3, tv, 0xFFE8D8C0);
                }
                f.tree(4, S - 4, 4, 0xFF3E7A3A);
                f.tree(L - 4, S - 4, 4, 0xFF3E7A3A);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                hip(f, 0xFF9A5A44, 0, 0, L, S - 4);
                for (int i = 0; i * 3 < L; i++) f.rect(i % 2 == 0 ? 0xFF3E7A4A : 0xFFF2F0E8, i * 3, S - 4, Math.min(L, i * 3 + 3), S);
                f.line(0x40000000, 0.8f, 0, S - 4, L, S - 4);
                float cu = L / 2, cv = (S - 4) / 2;
                f.box(0xFFF2F0E8, cu - 3, cv - 3, cu + 3, cv + 3, 1.2f);
                f.poly(0xFF4E8A6A, cu - 3, cv - 3, cu + 3, cv - 3, cu, cv);
                f.poly(0xFF3A6E52, cu + 3, cv - 3, cu + 3, cv + 3, cu, cv);
                f.poly(0xFF2E5A42, cu - 3, cv + 3, cu + 3, cv + 3, cu, cv);
                f.poly(0xFF5A9A78, cu - 3, cv - 3, cu - 3, cv + 3, cu, cv);
                f.circle(0xFFE8C860, cu, cv, 0.7f);
                f.sign(city, "BOWLS CLUB", cu, cv, 4, 0xFF3E7A4A);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.OLD | Variants.SUB | Variants.PARK;
        d.minS = 10;
        d.minL = 14;
        d.base = City.SHOP;
        d.inside = Variants.I_CAFE;
        d.roofCol = 0xFF9A5A44;
        out.add(d);
    }

    // ------------------------------------------------------------------ 8. Equestrian Centre

    /** A show jump: two wings and striped poles, along u when across is false. */
    static void jump(F f, float u, float v, float w, boolean acrossV, int col) {
        float hu = acrossV ? 0 : w / 2, hv = acrossV ? w / 2 : 0;
        f.line(0x40000000, 1, u - hu + 1, v - hv + 1.2f, u + hu + 1, v + hv + 1.2f);
        f.line(0xFFF4F4F0, 1, u - hu, v - hv, u + hu, v + hv);
        int n = 6;
        for (int k = 0; k < n; k += 2) {
            float a = k / (float) n, b = (k + 1) / (float) n;
            f.line(col, 1, u - hu + 2 * hu * a, v - hv + 2 * hv * a, u - hu + 2 * hu * b, v - hv + 2 * hv * b);
        }
        for (int sd = -1; sd <= 1; sd += 2) {
            float wu = u + sd * (hu + (acrossV ? 0 : 1.5f)), wv = v + sd * (hv + (acrossV ? 1.5f : 0));
            f.box(0xFFF4F4F0, wu - 1.2f, wv - 1.2f, wu + 1.2f, wv + 1.2f, 0.6f);
            f.circle(col, wu, wv, 0.6f);
        }
    }

    static void equestrian(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Equestrian Centre", T_PARK) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int l = Math.max(bw, bh);
                boolean a = bw >= bh;
                return new int[][]{at(a, 1, 1, l / 2, 2, 1), at(a, l / 2 + 2, 1, l - l / 2 - 3, 2, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF64A04E, 0, 0, L, S);
                for (int i = 0; i < 40; i++) f.circle(0x10000000, r.nextFloat() * L, r.nextFloat() * S, 3 + r.nextFloat() * 5);
                // The cobbled yard before the stables.
                f.rect(0xFFA8A090, 0, 3 * T - 2, L, 4 * T);
                for (float v = 3 * T + 2; v < 4 * T - 1; v += 3) f.line(0x16000000, 0.5f, 0, v, L, v);
                for (float u = 3; u < L; u += 4) f.line(0x10000000, 0.5f, u, 3 * T - 2, u, 4 * T);
                float sl = (tiles(L) / 2) * T;
                for (float u = T + 6; u < T + sl - 6; u += 12) {
                    animal(f, u, 3 * T + 1.5f, 90, 0, (int) u % 3 == 0 ? 0xFF4A2E1E : 0xFFA06A38, 0.75f);
                }
                f.box(0xFF6A7078, sl + T + 4, 3 * T + 4, sl + T + 14, 3 * T + 8, 0.8f);
                f.rect(0xFF6A9AB8, sl + T + 5, 3 * T + 5, sl + T + 13, 3 * T + 7);
                for (int k = 0; k < 3; k++) {
                    float hu = L - 10 - k * 7;
                    f.box(0xFFD8C080, hu - 3, 3 * T + 3, hu + 3, 3 * T + 8, 0.8f);
                    f.line(0xFFB89A58, 0.5f, hu - 3, 3 * T + 5.5f, hu + 3, 3 * T + 5.5f);
                }
                // The sand arena with boards, letters and a course of jumps.
                float a0 = 6, a1 = L * 0.6f, b0 = 4 * T + 5, b1 = S - 6;
                f.rect(0x30000000, a0 + 1, b0 + 1.5f, a1 + 1.5f, b1 + 2);
                f.rect(0xFFD8C49A, a0, b0, a1, b1);
                for (int k = 1; k <= 3; k++) {
                    float in = k * 5;
                    f.rrFrame(0x18000000, 1.5f, a0 + in, b0 + in, a1 - in, b1 - in, 12);
                }
                f.frame(0xFFF4F4F0, 1.2f, a0, b0, a1, b1);
                float aw = a1 - a0, ah = b1 - b0;
                float[] lu = {0.5f, 0.5f, 0, 1, 0.25f, 0.75f, 0.25f, 0.75f};
                float[] lv = {0, 1, 0.5f, 0.5f, 0, 0, 1, 1};
                for (int k = 0; k < lu.length; k++) {
                    float mu = a0 + aw * lu[k], mv = b0 + ah * lv[k];
                    f.box(0xFFF4F4F0, mu - 1, mv - 1, mu + 1, mv + 1, 0.5f);
                }
                int[] jc = {0xFFC83A30, 0xFF2E5EA8, 0xFF3A8A4A, 0xFFE8A030};
                jump(f, a0 + aw * 0.25f, b0 + ah * 0.3f, Math.min(14, ah * 0.25f), true, jc[0]);
                jump(f, a0 + aw * 0.5f, b0 + ah * 0.22f, Math.min(14, aw * 0.15f), false, jc[1]);
                jump(f, a0 + aw * 0.75f, b0 + ah * 0.55f, Math.min(14, ah * 0.25f), true, jc[2]);
                jump(f, a0 + aw * 0.42f, b0 + ah * 0.75f, Math.min(14, aw * 0.15f), false, jc[3]);
                jump(f, a0 + aw * 0.47f, b0 + ah * 0.75f, Math.min(14, aw * 0.15f), false, jc[3]);
                animal(f, a0 + aw * 0.62f, b0 + ah * 0.38f, -30, 0, 0xFFE8E2D8, 1f);
                f.circle(0xFF2A2A30, a0 + aw * 0.62f, b0 + ah * 0.38f, 1.6f);
                f.circle(0xFF1E2A44, a0 + aw * 0.62f - 0.3f, b0 + ah * 0.38f - 0.3f, 1);
                man(f, a1 - 6, b1 - 5, 0xFF2E5A3A);
                // The paddocks: rails, horses, a field shelter and a trough.
                float p0 = a1 + 8, p1 = L - 6;
                int np = (b1 - b0) > 100 ? 2 : 1;
                float ph = (b1 - b0 - (np - 1) * 6) / np;
                for (int k = 0; k < np; k++) {
                    float q0 = b0 + k * (ph + 6), q1 = q0 + ph;
                    f.rect(k == 0 ? 0xFF72AE58 : 0xFF6AA652, p0, q0, p1, q1);
                    for (int i = 0; i < 12; i++) f.circle(0x14000000, p0 + r.nextFloat() * (p1 - p0), q0 + r.nextFloat() * (q1 - q0), 1 + r.nextFloat() * 2);
                    f.box(0xFF6A5038, p1 - 12, q0 + 2, p1 - 2, q0 + 9, 1.2f);
                    f.rect(0xFF8A6A48, p1 - 12, q0 + 2, p1 - 2, q0 + 5);
                    f.box(0xFF6A7078, p0 + 3, q1 - 6, p0 + 9, q1 - 3, 0.5f);
                    f.rect(0xFF6A9AB8, p0 + 3.5f, q1 - 5.5f, p0 + 8.5f, q1 - 3.5f);
                    int nh = 1 + (int) ((p1 - p0) * ph / 2500);
                    for (int i = 0; i < Math.min(3, nh); i++)
                        animal(f, p0 + 10 + r.nextFloat() * (p1 - p0 - 22), q0 + 10 + r.nextFloat() * (ph - 20), r.nextFloat() * 360, 0,
                                new int[]{0xFF9A5A2A, 0xFFD8D0C4, 0xFF4A3426, 0xFFB8803A}[(i + k) % 4], 0.95f);
                    fence(f, 0xFFF0EEE6, p0, q0, p1, q1, 8);
                }
                // A horsebox and the muck heap.
                f.box(0xFFE8E4DC, 4, 3 * T - 1, 16, 3 * T + 6, 1);
                f.rect(0xFF2E5A3A, 4, 3 * T + 3, 16, 3 * T + 4);
                f.tree(L - 4, 4 * T + 4, 4, 0xFF3E7A3A);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                if (part == 1) {
                    tin(f, 0xFF4A6A50, 0, 0, L, S, 3);
                    f.line(0xFF7A9A80, 1, 0, S / 2, L, S / 2);
                    for (float u = 6; u < L - 4; u += 14) f.rect(0x90D8E8E0, u, 2, u + 5, S - 2);
                    f.frame(0xFF2E3E32, 0.8f, 0, 0, L, S);
                    return;
                }
                gable(f, 0xFF8A4A3A, 0, 0, L, S);
                for (float u = 3; u < L; u += 3) f.line(0x18000000, 0.4f, u, 0, u, S);
                for (float u = 10; u < L - 6; u += 16) {
                    f.box(0xFFA8B4BC, u, 3, u + 4, 6, 0.5f);
                }
                float cu = L / 2, cv = S / 2;
                f.box(0xFFF2F0E8, cu - 3.5f, cv - 3.5f, cu + 3.5f, cv + 3.5f, 1.5f);
                f.poly(0xFF5A9A80, cu - 3.5f, cv - 3.5f, cu + 3.5f, cv - 3.5f, cu, cv);
                f.poly(0xFF3E7A64, cu + 3.5f, cv - 3.5f, cu + 3.5f, cv + 3.5f, cu, cv);
                f.poly(0xFF2E604E, cu - 3.5f, cv + 3.5f, cu + 3.5f, cv + 3.5f, cu, cv);
                f.poly(0xFF6AAA90, cu - 3.5f, cv - 3.5f, cu - 3.5f, cv + 3.5f, cu, cv);
                f.line(0xFF2A2A2A, 0.5f, cu - 2.5f, cv, cu + 2.5f, cv);
                f.poly(0xFF2A2A2A, cu + 2.5f, cv - 1, cu + 3.8f, cv, cu + 2.5f, cv + 1);
                f.sign(city, "STABLES", cu, cv + 4, 4, 0xFF8A4A3A);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.SUB | Variants.PARK;
        d.minS = 10;
        d.minL = 14;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_RACKS;
        d.roofCol = 0xFF8A4A3A;
        out.add(d);
    }

    // ------------------------------------------------------------------ 9. Training Ground

    /** A football pitch long along g's u, with training drills on it. */
    static void pitch(F g, Random r, int variant) {
        float L = g.L, S = g.S;
        int n = Math.max(6, (int) (L / 12));
        for (int i = 0; i < n; i++) g.rect(i % 2 == 0 ? 0xFF68AE50 : 0xFF5EA246, L * i / n, 0, L * (i + 1) / n, S);
        int w = 0xFFF2F2EE;
        float in = 3, cu = L / 2, cv = S / 2;
        g.frame(w, 0.6f, in, in, L - in, S - in);
        g.line(w, 0.6f, cu, in, cu, S - in);
        g.ring(w, 0.6f, cu, cv, S * 0.14f);
        g.circle(w, cu, cv, 0.7f);
        float pd = L * 0.15f, pw = S * 0.56f, sd = L * 0.055f, sw = S * 0.26f;
        for (int e = 0; e < 2; e++) {
            float a = e == 0 ? in : L - in, sg = e == 0 ? 1 : -1;
            g.frame(w, 0.6f, Math.min(a, a + sg * pd), cv - pw / 2, Math.max(a, a + sg * pd), cv + pw / 2);
            g.frame(w, 0.6f, Math.min(a, a + sg * sd), cv - sw / 2, Math.max(a, a + sg * sd), cv + sw / 2);
            g.circle(w, a + sg * pd * 0.7f, cv, 0.6f);
            g.arcLine(w, 0.6f, a + sg * pd * 0.7f, cv, S * 0.14f, e == 0 ? -53 : 127, e == 0 ? 53 : 233);
            // The goal and its net.
            float gw = S * 0.11f, gd = 3.5f;
            float b0 = Math.min(a, a - sg * gd), b1 = Math.max(a, a - sg * gd);
            g.rect(0x30FFFFFF, b0, cv - gw, b1, cv + gw);
            for (float v = cv - gw + 1.2f; v < cv + gw; v += 1.2f) g.line(0x60FFFFFF, 0.3f, b0, v, b1, v);
            g.line(0x40000000, 1, a + 0.6f, cv - gw + 0.8f, a + 0.6f, cv + gw + 0.8f);
            g.line(0xFFFAFAFA, 1, a, cv - gw, a, cv + gw);
        }
        // Drills: a cone slalom, an agility ladder, a wall of mannequins and players in bibs.
        float q = variant == 0 ? L * 0.25f : L * 0.72f;
        for (int k = 0; k < 7; k++) {
            float u = q - L * 0.12f + k * L * 0.04f, v = cv + S * 0.3f * (k % 2 == 0 ? 1 : 0.7f);
            g.circle(0x40000000, u + 0.4f, v + 0.4f, 0.9f);
            g.circle(0xFFF08A20, u, v, 0.9f);
            g.circle(0xFFF8C060, u - 0.2f, v - 0.2f, 0.35f);
        }
        float lu = q - L * 0.1f, lv = cv - S * 0.32f;
        g.line(0xFFE8D020, 0.5f, lu, lv, lu + L * 0.16f, lv);
        g.line(0xFFE8D020, 0.5f, lu, lv + 3, lu + L * 0.16f, lv + 3);
        for (float u = lu; u <= lu + L * 0.16f + 0.1f; u += 2.5f) g.line(0xFFE8D020, 0.5f, u, lv, u, lv + 3);
        float mu = variant == 0 ? L - in - pd - 4 : in + pd + 4;
        for (int k = 0; k < 4; k++) {
            float v = cv - 4.5f + k * 3;
            g.circle(0x40000000, mu + 0.6f, v + 0.7f, 1.3f);
            g.circle(0xFF2E5EA8, mu, v, 1.3f);
            g.circle(0xFF1E3E78, mu, v, 0.6f);
        }
        int bib = variant == 0 ? 0xFFF08A20 : 0xFFD8E830;
        for (int k = 0; k < 9; k++) {
            float u = L * (0.15f + r.nextFloat() * 0.7f), v = S * (0.2f + r.nextFloat() * 0.6f);
            man(g, u, v, k % 2 == 0 ? bib : 0xFF8A2A3A);
            if (k % 3 == 0) g.circle(0xFFF8F8F8, u + 2.5f, v + 1.5f, 0.8f);
        }
        man(g, cu, S - 1.5f, 0xFF2A2A30);
        for (int k = 0; k < 6; k++) g.circle(0xFFF8F8F8, in + 2 + k * 1.8f, S - 1.5f, 0.7f);
    }

    static void training(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Training Ground", T_PARK) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int s = Math.min(bw, bh);
                return new int[][]{at(bw >= bh, 1, s / 2 - 2, 3, 4, 2)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF55913F, 0, 0, L, S);
                float a0 = 4 * T + 10, a1 = L - 5, b0 = 6, b1 = S - 6;
                // A drive and car park by the clubhouse.
                f.rect(0xFF6A6E72, 0, S / 2 - 2 * T - 10, 4 * T + 4, S / 2 - 2 * T - 2);
                f.rect(0xFF6A6E72, 4 * T + 2, 4, 4 * T + 8, S - 4);
                for (float v = 8; v < S / 2 - 2 * T - 14; v += 9) {
                    f.box(KIT[(int) v % KIT.length], 6, v, 14, v + 5, 0.8f);
                }
                for (float v = S / 2 + 2 * T + 8; v < S - 8; v += 12) {
                    f.tree(10, v, 4.5f, 0xFF3E7A3A);
                    f.tree(26, v + 6, 4, 0xFF4A8640);
                }
                float al = a1 - a0, as = b1 - b0;
                int n = Math.max(1, Math.round(al / (as * 1.45f)));
                float gap = 8, pl = (al - gap * (n - 1)) / n;
                for (int k = 0; k < n; k++) {
                    float u0 = a0 + k * (pl + gap);
                    float wx0 = f.x(u0, b0), wy0 = f.y(u0, b0), wx1 = f.x(u0 + pl, b1), wy1 = f.y(u0 + pl, b1);
                    F g = new F(c, p, wx0, wy0, wx1, wy1);
                    f.rect(0xFF4A8438, u0 - 2, b0 - 2, u0 + pl + 2, b1 + 2);
                    pitch(g, r, k % 2);
                }
                // Dugout shelters down the side.
                for (int k = 0; k < n; k++) {
                    float u = a0 + k * (pl + gap) + pl / 2;
                    f.box(0xFF2A3A4A, u - 10, S - 4, u - 3, S - 1, 0.8f);
                    f.rect(0x90B8D8E8, u - 10, S - 4, u - 3, S - 3);
                    f.box(0xFF2A3A4A, u + 3, S - 4, u + 10, S - 1, 0.8f);
                    f.rect(0x90B8D8E8, u + 3, S - 4, u + 10, S - 3);
                }
                f.lamp(4 * T + 5, 3);
                f.lamp(4 * T + 5, S - 3);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                flatRoof(c, p, x0, y0, x1, y1, 0xFFD8D8D4);
                f.rect(0xFF8A2A3A, 0, S - 4, L, S - 1.5f);
                f.rect(0xFF7AB0D8, 0, S - 1.5f, L, S);
                int np = (int) ((L - 8) / 7);
                for (int i = 0; i < np; i++)
                    for (int j = 0; j < 2; j++) {
                        float u = 4 + i * 7, v = 4 + j * 9;
                        if (v + 7 > S - 6) continue;
                        f.box(0xFF2A3A5A, u, v, u + 6, v + 7.5f, 0.6f);
                        f.line(0xFF4A6A9A, 0.3f, u + 3, v, u + 3, v + 7.5f);
                        f.line(0xFF4A6A9A, 0.3f, u, v + 3.7f, u + 6, v + 3.7f);
                    }
                hvac(f, L - 12, S - 12, 5);
                f.sign(city, "TRAINING", L / 2, S / 2, 4, 0xFF8A2A3A);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.SUB | Variants.PARK | Variants.MID | Variants.IND;
        d.minS = 10;
        d.minL = 16;
        d.base = City.OFFICE;
        d.inside = Variants.I_GYM;
        d.roofCol = 0xFFD8D8D4;
        out.add(d);
    }

    // ------------------------------------------------------------------ 10. Athletics Stadium

    static void athletics(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Athletics Stadium", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int l = Math.max(bw, bh), s = Math.min(bw, bh);
                return new int[][]{at(bw >= bh, 2, s - 3, l - 4, 2, 2)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float sv = (tiles(S) - 3) * T;
                f.rect(0xFFC8C2B4, 0, 0, L, S);
                f.rect(0xFF4E8A40, 0, 0, L, sv - 1);
                float u0 = 5, u1 = L - 5, v0 = 4, v1 = sv - 4, h = v1 - v0, rad = h / 2;
                float lw = Math.min(3.2f, h * 0.028f);
                int lanes = 8;
                f.roundRect(0x30000000, u0 + 1, v0 + 1.5f, u1 + 2, v1 + 2, rad);
                f.roundRect(0xFF8A8A84, u0 - 1, v0 - 1, u1 + 1, v1 + 1, rad + 1);
                f.roundRect(0xFFB4553E, u0, v0, u1, v1, rad);
                for (int k = 1; k < lanes; k++) f.rrFrame(0xE0F4F0EC, 0.35f, u0 + k * lw, v0 + k * lw, u1 - k * lw, v1 - k * lw, rad - k * lw);
                float ii = lanes * lw;
                f.roundRect(0xFFF4F4F0, u0 + ii, v0 + ii, u1 - ii, v1 - ii, rad - ii);
                float ij = ii + 0.8f;
                f.roundRect(0xFF5E9E48, u0 + ij, v0 + ij, u1 - ij, v1 - ij, rad - ij);
                float iu0 = u0 + ij, iu1 = u1 - ij, iv0 = v0 + ij, iv1 = v1 - ij, ir = rad - ij;
                for (float u = iu0 + ir; u < iu1 - ir; u += 16) f.rect(0x10FFFFFF, u, iv0, u + 8, iv1);
                // Finish line, the stagger and the lane numbers on the home straight.
                float fu = u1 - rad - 4;
                f.rect(0xFFF4F4F0, fu, v1 - lanes * lw, fu + 1, v1);
                for (int k = 0; k < lanes; k++) {
                    float su = u0 + rad + 6 + k * 2.2f, lv = v1 - (k + 0.5f) * lw;
                    f.line(0xFFF4F4F0, 0.5f, su, lv - lw / 2, su, lv + lw / 2);
                    f.rect(0xFFE8E4DC, fu + 2, lv - 0.4f, fu + 3, lv + 0.4f);
                }
                // Runners on the back straight.
                for (int k = 0; k < 6; k++) {
                    float lv = v0 + (k + 1.5f) * lw, u = L * 0.45f + r.nextFloat() * L * 0.12f;
                    man(f, u, lv, KIT[k % KIT.length]);
                }
                // Field events in the infield: long jump, javelin, shot, discus cage and high jump.
                float cu = (iu0 + iu1) / 2, cv = (iv0 + iv1) / 2;
                float jl = Math.min(iu1 - iu0 - 2 * ir * 0.3f, L * 0.45f);
                f.rect(0xFFB4553E, cu - jl / 2, iv1 - 7, cu + jl / 2 - 14, iv1 - 4.5f);
                f.box(0xFFE4D2A0, cu + jl / 2 - 14, iv1 - 9, cu + jl / 2, iv1 - 2.5f, 0.6f);
                f.rect(0xFFF4F4F0, cu + jl / 2 - 15, iv1 - 7, cu + jl / 2 - 14, iv1 - 4.5f);
                man(f, cu - jl / 4, iv1 - 5.7f, 0xFFE8C030);
                f.rect(0xFFB4553E, cu - jl / 2, iv0 + 4, cu + jl / 6, iv0 + 6.5f);
                f.line(0xFFF4F4F0, 0.4f, cu + jl / 6, iv0 + 4, cu + jl / 6 + 30, iv0 + 1);
                f.line(0xFFF4F4F0, 0.4f, cu + jl / 6, iv0 + 6.5f, cu + jl / 6 + 30, iv0 + 9.5f);
                f.line(0xFFE8E0C0, 0.6f, cu + jl / 6 + 18, iv0 + 3.5f, cu + jl / 6 + 24, iv0 + 3.8f);
                // The shot circle in one end, the cage in the other.
                float eu = iu0 + ir * 0.75f;
                f.circle(0xFFB8B4AC, eu, cv, 3.5f);
                f.ring(0xFFF4F4F0, 0.5f, eu, cv, 3.5f);
                f.line(0xFFF4F4F0, 0.4f, eu, cv, eu + ir * 0.6f, cv - ir * 0.35f);
                f.line(0xFFF4F4F0, 0.4f, eu, cv, eu + ir * 0.6f, cv + ir * 0.35f);
                f.circle(0xFF3A3A3A, eu + ir * 0.4f, cv + 2, 0.8f);
                float hu = iu1 - ir * 0.75f;
                f.circle(0xFFB8B4AC, hu, cv, 3.5f);
                Path q = new Path();
                f.arc(q, hu, cv, 6, 220, 500, 8, true);
                f.stroke(q, 0xFF3A3E44, 0.8f);
                for (int k = 0; k < 8; k++) {
                    double a = Math.toRadians(220 + k * 40);
                    f.circle(0xFF2A2E34, hu + (float) Math.cos(a) * 6, cv + (float) Math.sin(a) * 6, 0.7f);
                }
                man(f, hu, cv, 0xFF2E5EA8);
                // High jump: a fan of track surface in the far bend with the mat and bar.
                float hj = iu1 - ir * 0.25f;
                f.rect(0xFF2E5EA8, hj - 3, cv - 8 - 9, hj + 3, cv - 8);
                f.line(0xFFF4F4F0, 0.6f, hj - 4, cv - 18, hj - 4, cv - 7);
                // The steeplechase water jump in the near bend.
                f.box(0xFF4A8AB8, iu0 + 2, cv + ir * 0.55f, iu0 + 10, cv + ir * 0.55f + 4, 0.5f);
                // Floodlight pylons at the corners.
                float[][] fl = {{3, 3}, {L - 3, 3}};
                for (float[] k : fl) {
                    f.circle(0x40000000, k[0] + 2, k[1] + 2, 2.5f);
                    f.circle(0xFF5A5E64, k[0], k[1], 2.2f);
                    f.circle(0xFFF4ECC0, k[0], k[1], 1.2f);
                }
                for (int i = 0; i < 10; i++) man(f, 4 + r.nextFloat() * (L - 8), S - 4 - r.nextFloat() * 4, KIT[r.nextInt(KIT.length)]);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                // The main stand: seats at the front, a white membrane roof on masts.
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, sd = S * 0.4f;
                f.rect(0xFF5E6268, 0, 0, L, S);
                for (int i = 0; i < 5; i++) f.rect(shade(0xFFC83A30, 0.8f + i * 0.06f), 0, i * sd / 5, L, (i + 0.8f) * sd / 5);
                for (int i = 0; i < L / 3; i++) f.circle(KIT[r.nextInt(KIT.length)], 1 + r.nextFloat() * (L - 2), r.nextInt(5) * sd / 5 + 1, 0.7f);
                int n = Math.max(3, (int) (L / 20));
                float w = L / n;
                for (int k = 0; k < n; k++) {
                    float a = k * w, b = a + w, m = a + w / 2;
                    f.poly(0xFFF2F2EE, a, sd, b, sd, m, S * 0.72f);
                    f.poly(0xFFD4D6D8, a, sd, m, S * 0.72f, a, S);
                    f.poly(0xFFC4C6C8, b, sd, b, S, m, S * 0.72f);
                    f.poly(0xFFB4B6B8, a, S, b, S, m, S * 0.72f);
                    f.line(0xFF8A8E92, 0.5f, m, S * 0.72f, a, sd);
                    f.line(0xFF8A8E92, 0.5f, m, S * 0.72f, b, sd);
                    f.circle(0x50000000, m + 1, S * 0.72f + 1, 1.4f);
                    f.circle(0xFF6A6E72, m, S * 0.72f, 1.2f);
                }
                f.line(0xFFFAFAFA, 1, 0, sd, L, sd);
                f.sign(city, "STADIUM", L / 2, S * 0.8f, 5, 0xFFC83A30);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.MID | Variants.SUB | Variants.UNI | Variants.PARK;
        d.minS = 12;
        d.minL = 16;
        d.base = City.OFFICE;
        d.inside = Variants.I_SEATS;
        d.roofCol = 0xFFF2F2EE;
        out.add(d);
    }

    // ------------------------------------------------------------------ 11. Water Park

    /** A smooth path through the points (quads through the midpoints). */
    static Path smooth(F f, float[] q) {
        Path pa = new Path();
        f.mv(pa, q[0], q[1]);
        int n = q.length / 2;
        for (int i = 1; i < n - 1; i++) f.qd(pa, q[2 * i], q[2 * i + 1], (q[2 * i] + q[2 * i + 2]) / 2, (q[2 * i + 1] + q[2 * i + 3]) / 2);
        f.ln(pa, q[2 * n - 2], q[2 * n - 1]);
        return pa;
    }

    static void slide(F f, float[] q, int col) {
        f.stroke(smooth(f, shift(q, 2.2f)), 0x40000000, 5);
        f.stroke(smooth(f, q), City.darken(col, 0.75f), 5.2f);
        f.stroke(smooth(f, q), col, 4.2f);
        f.stroke(smooth(f, q), City.lighten(col, 0.25f), 2.2f);
        f.stroke(smooth(f, q), 0x90E8F8FF, 0.8f);
    }

    static void waterPark(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Water Park", T_PARK) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return new int[][]{at(bw >= bh, 1, 1, 3, 3, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFFE2DACA, 0, 0, L, S);
                for (float u = 8; u < L; u += 8) f.line(0x10000000, 0.5f, u, 0, u, S);
                // The wave pool: a sandy beach shelving into deepening water.
                float w0 = L * 0.58f, w1 = L - 5;
                Path q = new Path();
                f.mv(q, w1, 5);
                f.ln(q, w0 + 14, 5);
                f.qd(q, w0 - 16, S / 2, w0 + 14, S - 5);
                f.ln(q, w1, S - 5);
                q.close();
                f.fill(q, 0xFFE8D8A8);
                Path w = new Path();
                f.mv(w, w1, 5);
                f.ln(w, w0 + 22, 5);
                f.qd(w, w0 - 4, S / 2, w0 + 22, S - 5);
                f.ln(w, w1, S - 5);
                w.close();
                f.fill(w, 0xFF8AD2E4);
                f.rect(0xFF4AA8D0, w0 + (w1 - w0) * 0.55f, 5, w1, S - 5);
                f.rect(0xFF3A94C0, w0 + (w1 - w0) * 0.8f, 5, w1, S - 5);
                for (int k = 0; k < 5; k++) {
                    float u = w0 + 20 + k * (w1 - w0 - 24) / 5f;
                    Path wv = new Path();
                    f.mv(wv, u, 7);
                    for (float v = 7; v < S - 9; v += 10) f.qd(wv, u + (((int) (v / 10)) % 2 == 0 ? 2 : -2), v + 5, u, v + 10);
                    f.stroke(wv, 0x80FFFFFF, 0.9f);
                }
                f.rect(0xFFD8DCE0, w1, 4, w1 + 2, S - 4);
                for (int i = 0; i < 10; i++) {
                    float u = w0 + 18 + r.nextFloat() * (w1 - w0 - 24), v = 10 + r.nextFloat() * (S - 20);
                    if (i % 3 == 0) {
                        f.ring(KIT[i % KIT.length], 1.2f, u, v, 2.2f);
                    }
                    f.circle(SKIN[i % SKIN.length], u, v, 1.2f);
                }
                // The lazy river round a palm island.
                float l0 = 6, l1 = w0 - 22, m0 = S * 0.48f, m1 = S - 6;
                if (l1 - l0 < m1 - m0 + 8) m0 = Math.max(S * 0.4f, m1 - (l1 - l0) + 8);
                float lr = Math.min(l1 - l0, m1 - m0) / 2;
                f.rrFrame(0x30000000, 12, l0 + 7, m0 + 7, l1 - 4, m1 - 4, lr - 5);
                f.rrFrame(0xFFC8C0B0, 12, l0 + 6, m0 + 6, l1 - 6, m1 - 6, lr - 6);
                f.rrFrame(0xFF4AA8D0, 9, l0 + 6, m0 + 6, l1 - 6, m1 - 6, lr - 6);
                f.rrFrame(0xFF6EC0E0, 4, l0 + 6, m0 + 6, l1 - 6, m1 - 6, lr - 6);
                f.roundRect(0xFF6AA850, l0 + 11, m0 + 11, l1 - 11, m1 - 11, Math.max(1, lr - 11));
                for (int k = 0; k < 9; k++) {
                    float[] pt = stad6(l0 + 6, m0 + 6, l1 - 6, m1 - 6, k / 9f + 0.03f);
                    f.circle(0x40000000, pt[0] + 0.6f, pt[1] + 0.6f, 2.2f);
                    f.ring(KIT[k % 6], 1.4f, pt[0], pt[1], 1.8f);
                    if (k % 2 == 0) f.circle(SKIN[k % SKIN.length], pt[0], pt[1], 1.1f);
                }
                float iu = (l0 + l1) / 2, iv = (m0 + m1) / 2;
                palm(f, iu - (l1 - l0) * 0.15f, iv, 7);
                palm(f, iu + (l1 - l0) * 0.15f, iv - 2, 6);
                // The slide tower and three twisting flumes into a splash pool.
                float tu = w0 - 26, tv = S * 0.2f, pu0 = 4 * T + 6, pu1 = pu0 + Math.min(40, (tu - pu0) * 0.7f), pv0 = S * 0.06f, pv1 = Math.min(m0 - 10, pv0 + 34);
                f.roundRect(0xFFF2EEE6, pu0 - 2, pv0 - 2, pu1 + 2, pv1 + 2, 5);
                f.roundRect(0xFF5AB8DC, pu0, pv0, pu1, pv1, 4);
                float hu = (tu - pu1), sv = pv1 - pv0;
                slide(f, new float[]{tu - 2, tv - 4, tu - hu * 0.2f, pv0 - 2, tu - hu * 0.6f, pv0 + 4, tu - hu * 0.5f, tv + 16, tu - hu * 0.9f, tv + 14, pu1 - 4, pv0 + sv * 0.3f}, 0xFFE8B030);
                slide(f, new float[]{tu - 2, tv + 2, tu - hu * 0.3f, tv + 20, tu + 8, tv + 24, tu - hu * 0.2f, m0 - 6, tu - hu * 0.85f, m0 - 8, pu1 - 4, pv1 - 4}, 0xFF3A7AC8);
                slide(f, new float[]{tu + 2, tv, tu + 14, tv - 10, tu + 18, tv + 10, tu + 6, tv + 14, tu - hu * 0.4f, tv + 2, pu1 - 4, pv0 + sv * 0.6f}, 0xFFD84A3A);
                f.box(0xFF8A8E92, tu - 6, tv - 6, tu + 6, tv + 6, 2.5f);
                f.rect(0xFFB4B8BC, tu - 5, tv - 5, tu + 5, tv + 5);
                for (int k = 0; k < 5; k++) f.line(0xFF6A6E72, 0.6f, tu - 5, tv - 4 + k * 2, tu + 5, tv - 4 + k * 2);
                f.rect(0xFF3A7AC8, tu - 5, tv - 5, tu + 5, tv - 3.5f);
                for (int k = 0; k < 4; k++) {
                    f.ring(0x80FFFFFF, 0.6f, pu1 - 6 - r.nextFloat() * 4, pv0 + 5 + r.nextFloat() * (sv - 10), 2 + r.nextFloat() * 2);
                }
                // Loungers, parasols and palms on the decks.
                for (float u = 6; u < l1 - 4; u += 16) {
                    float v = m0 - 7;
                    if (u > 4 * T + 2 || v > 4 * T + 4) {
                        f.box(0xFFF8F8F4, u, v - 2, u + 8, v + 1, 0.6f);
                        f.rect(KIT[(int) (u / 16) % 6], u + 0.5f, v - 1.5f, u + 3, v + 0.5f);
                    }
                }
                for (float v = 14; v < S - 14; v += 22) parasol(f, w0 - 8, v, 4.5f, 0xFFF8F4E8, (int) v % 3 == 0 ? 0xFFE8B030 : 0xFF3A7AC8);
                palm(f, L - 3, 3, 6);
                palm(f, w0 - 4, S - 4, 6);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF2E6E9A, 0, 0, L, S);
                for (int i = 0; i < 4; i++) {
                    float in = 2 + i * (S / 10);
                    f.frame(i % 2 == 0 ? 0xFF5AB0DC : 0xFFF2F2EE, 1.5f, in, in, L - in, S - in);
                }
                f.circle(0xFFE8B030, L / 2, S / 2, S * 0.12f);
                f.sign(city, "WATER PARK", L / 2, S / 2, 4, 0xFFF2F2EE);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.SUB | Variants.PARK | Variants.MID;
        d.minS = 11;
        d.minL = 15;
        d.base = City.SHOP;
        d.inside = Variants.I_BATH;
        d.roofCol = 0xFF2E6E9A;
        out.add(d);
    }

    /** A stadium point that also works for a shape taller than wide (it falls back to the long axis). */
    static float[] stad6(float u0, float v0, float u1, float v1, float t) {
        if (u1 - u0 >= v1 - v0) return stad(u0, v0, u1, v1, t);
        float[] q = stad(v0, u0, v1, u1, t);
        return new float[]{q[1], q[0]};
    }

    /** A palm tree from above: fronds round a trunk top. */
    static void palm(F f, float u, float v, float rad) {
        f.circle(0x30000000, u + rad * 0.5f, v + rad * 0.5f, rad * 0.8f);
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4 + 0.2f;
            float eu = u + (float) Math.cos(a) * rad, ev = v + (float) Math.sin(a) * rad;
            f.curve(k < 4 ? 0xFF3E7A36 : 0xFF4E8E42, 1.6f, u, v, (u + eu) / 2 - (float) Math.sin(a) * 1.2f, (v + ev) / 2 + (float) Math.cos(a) * 1.2f, eu, ev);
        }
        f.circle(0xFF7A6040, u, v, 1);
    }

    // ------------------------------------------------------------------ 12. Trampoline Park

    static void trampoline(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Trampoline Park", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int l = Math.max(bw, bh);
                return new int[][]{at(bw >= bh, 1, 1, l - 2, 3, 2)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFFD4CEC2, 0, 0, L, S);
                float v0 = 4 * T + 6, v1 = S - 5;
                f.roundRect(0xFF3E9A6A, 4, v0 - 2, L - 4, v1 + 1, 6);
                f.roundRect(0xFF48A876, 5, v0 - 1, L - 5, v1, 5);
                // A grid of sunken trampolines with padded frames.
                float g1 = L * 0.56f;
                int cols = Math.max(3, (int) ((g1 - 10) / 20)), rows = Math.max(2, (int) ((v1 - v0 - 6) / 20));
                float cw = (g1 - 10) / cols, ch = (v1 - v0 - 6) / rows;
                f.rect(0xFF2A62B8, 8, v0 + 2, 8 + cols * cw + 2, v0 + 4 + rows * ch);
                for (int i = 0; i < cols; i++)
                    for (int j = 0; j < rows; j++) {
                        float a = 9 + i * cw, b = v0 + 3 + j * ch;
                        f.rect((i + j) % 2 == 0 ? 0xFFF08A20 : 0xFF2E6AC8, a, b, a + cw, b + ch);
                        f.rect(0xFF1E2024, a + 2, b + 2, a + cw - 2, b + ch - 2);
                        f.rect(0x18FFFFFF, a + 2, b + 2, a + cw - 2, b + (ch - 4) * 0.4f);
                        f.frame(0x30FFFFFF, 0.3f, a + 3, b + 3, a + cw - 3, b + ch - 3);
                        if (r.nextFloat() < 0.45f) {
                            float pu = a + cw / 2, pv = b + ch / 2;
                            f.circle(0x40000000, pu + 2.5f, pv + 3, 1.8f);
                            f.circle(KIT[r.nextInt(KIT.length)], pu, pv, 2.3f);
                            f.circle(SKIN[r.nextInt(SKIN.length)], pu - 0.2f, pv - 0.2f, 1.3f);
                        }
                    }
                // The foam pit full of coloured cubes.
                float f0 = g1 + 6, f1 = Math.min(L - 30, g1 + (L - g1) * 0.55f);
                f.box(0xFF2A62B8, f0, v0 + 2, f1, v1 - 3, 1.5f);
                f.rect(0xFF3A3E44, f0 + 2, v0 + 4, f1 - 2, v1 - 5);
                int[] cube = {0xFFE84030, 0xFFF0C020, 0xFF30A050, 0xFF2E7AD0, 0xFFE860B0, 0xFFF08A20};
                f.rect(0xFF8A4A9A, f0 + 2, v0 + 4, f1 - 2, v1 - 5);
                float cs = Math.max(2.6f, (float) Math.sqrt((f1 - f0 - 6) * (v1 - v0 - 11) / 260f));
                for (float u = f0 + 3; u < f1 - 3; u += cs)
                    for (float v = v0 + 5; v < v1 - 6; v += cs) {
                        float ju = u + r.nextFloat() * 0.8f, jv = v + r.nextFloat() * 0.8f;
                        f.rect(cube[r.nextInt(cube.length)], ju, jv, ju + cs * 0.85f, jv + cs * 0.85f);
                    }
                man(f, (f0 + f1) / 2, (v0 + v1) / 2, 0xFFE8C030);
                f.rect(0xFF2E6AC8, f0 - 6, (v0 + v1) / 2 - 3, f0, (v0 + v1) / 2 + 3);
                // Round trampolines in safety nets.
                float ru = (f1 + L - 5) / 2, rr = Math.min((L - 5 - f1) / 2 - 3, (v1 - v0) / 4 - 2);
                int nr = rr > 6 ? 2 : 0;
                for (int k = 0; k < nr; k++) {
                    float rv = v0 + (v1 - v0) * (k == 0 ? 0.27f : 0.73f);
                    f.circle(0x38000000, ru + 2, rv + 2.5f, rr);
                    f.circle(0xFF2E6AC8, ru, rv, rr);
                    f.circle(0xFF1E2024, ru, rv, rr - 2);
                    f.ring(0xA0D8DCE0, 0.6f, ru, rv, rr - 0.6f);
                    for (int i = 0; i < 8; i++) {
                        double a = i * Math.PI / 4;
                        f.circle(0xFF3A3E44, ru + (float) Math.cos(a) * rr, rv + (float) Math.sin(a) * rr, 0.7f);
                    }
                    man(f, ru - 1, rv, KIT[k + 2]);
                }
                // Shade sails over the grid.
                f.poly(0x38FFFFFF, 10, v0, g1 * 0.55f, v0 + 4, 14, v1 - 4);
                f.poly(0x30FFF4E0, g1 * 0.5f, v0 + 2, g1, v0 + 6, g1 * 0.6f, v1 - 2);
                f.line(0xFF8A8E92, 0.4f, 10, v0, g1 * 0.55f, v0 + 4);
                // Parents on benches, a snack cart.
                for (float u = 12; u < L - 12; u += 24) f.bench(u, 4 * T + 2, true);
                f.box(0xFFF2F2EE, L - 18, 4 * T - 1, L - 8, 4 * T + 4, 1);
                for (int i = 0; i < 5; i++) f.rect(i % 2 == 0 ? 0xFFE84030 : 0xFFF2F2EE, L - 18 + i * 2, 4 * T - 1, L - 16 + i * 2, 4 * T + 1);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, cv = S / 2;
                flatRoof(c, p, x0, y0, x1, y1, 0xFFE4E2DC);
                // A row of bubble rooflights in bright colours.
                int[] cols = {0xFFE84030, 0xFFF0C020, 0xFF30A050, 0xFF2E7AD0, 0xFFE860B0, 0xFFF08A20};
                int n = Math.max(3, (int) ((L - 40) / 14));
                for (int k = 0; k < n; k++) {
                    float u = 8 + k * (L - 44) / Math.max(1, n - 1);
                    f.circle(0x40000000, u + 1.2f, cv + 1.5f, 4.5f);
                    f.circle(City.darken(cols[k % 6], 0.8f), u, cv, 4.5f);
                    f.circle(cols[k % 6], u - 0.4f, cv - 0.4f, 3.6f);
                    f.circle(0x70FFFFFF, u - 1.5f, cv - 1.5f, 1.4f);
                }
                // A big star on the end.
                float su = L - 18, sr = Math.min(14, S * 0.42f);
                float[] st = new float[20];
                for (int k = 0; k < 10; k++) {
                    double a = -Math.PI / 2 + k * Math.PI / 5;
                    float rr = k % 2 == 0 ? sr : sr * 0.45f;
                    st[2 * k] = su + (float) Math.cos(a) * rr;
                    st[2 * k + 1] = cv + (float) Math.sin(a) * rr;
                }
                f.poly(0x40000000, shift(st, 1.2f));
                f.poly(0xFFF0C020, st);
                f.circle(0xFFE84030, su, cv, sr * 0.3f);
                hvac(f, L - 40, 3, 4);
                f.sign(city, "JUMP!", su, cv, 5, 0xFFE84030);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.MID | Variants.SUB | Variants.IND;
        d.minS = 9;
        d.minL = 14;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_GYM;
        d.roofCol = 0xFFE4E2DC;
        out.add(d);
    }

    // ------------------------------------------------------------------ 13. Paintball Arena

    /** A paint splat: a lumpy blob and droplets flung round it. */
    static void splat(F f, Random r, float u, float v, float s, int col) {
        blob(f, col, u, v, s, s * (0.7f + r.nextFloat() * 0.4f), jit(r, 7, 0.35f));
        for (int k = 0; k < 5; k++) {
            double a = r.nextFloat() * Math.PI * 2;
            float d = s * (1.3f + r.nextFloat());
            f.circle(col, u + (float) Math.cos(a) * d, v + (float) Math.sin(a) * d, 0.3f + r.nextFloat() * 0.5f);
        }
    }

    /** One inflatable or scrap bunker. */
    static void bunker(F f, int kind, float u, float v, float s, int col) {
        int dk = City.darken(col, 0.7f), lt = City.lighten(col, 0.25f);
        switch (kind) {
            case 0: // can
                f.circle(0x40000000, u + 1.2f, v + 1.5f, s);
                f.circle(dk, u, v, s);
                f.circle(col, u - s * 0.12f, v - s * 0.12f, s * 0.8f);
                f.circle(lt, u - s * 0.35f, v - s * 0.35f, s * 0.25f);
                break;
            case 1: // snake
                f.roundRect(0x40000000, u - s * 2.2f + 1.2f, v - s * 0.5f + 1.5f, u + s * 2.2f + 1.2f, v + s * 0.5f + 1.5f, s * 0.5f);
                f.roundRect(dk, u - s * 2.2f, v - s * 0.5f, u + s * 2.2f, v + s * 0.5f, s * 0.5f);
                f.roundRect(col, u - s * 2.1f, v - s * 0.45f, u + s * 2.1f, v + s * 0.15f, s * 0.4f);
                for (float k = -1.5f; k <= 1.5f; k += 1) f.line(dk, 0.4f, u + k * s, v - s * 0.5f, u + k * s, v + s * 0.5f);
                break;
            case 2: // dorito
                f.poly(0x40000000, u - s + 1.2f, v + s * 0.7f + 1.5f, u + s + 1.2f, v + s * 0.7f + 1.5f, u + 1.2f, v - s * 0.9f + 1.5f);
                f.poly(dk, u - s, v + s * 0.7f, u + s, v + s * 0.7f, u, v - s * 0.9f);
                f.poly(col, u - s, v + s * 0.7f, u, v - s * 0.9f, u, v + s * 0.2f);
                f.poly(lt, u, v - s * 0.9f, u, v + s * 0.2f, u - s * 0.4f, v + s * 0.1f);
                break;
            case 3: // pyramid
                f.rect(0x40000000, u - s + 1.5f, v - s + 1.8f, u + s + 1.5f, v + s + 1.8f);
                f.poly(lt, u - s, v - s, u + s, v - s, u, v);
                f.poly(col, u - s, v - s, u - s, v + s, u, v);
                f.poly(City.darken(col, 0.82f), u + s, v - s, u + s, v + s, u, v);
                f.poly(dk, u - s, v + s, u + s, v + s, u, v);
                break;
            case 4: // tyre stack
                for (int k = 0; k < 3; k++) {
                    float tu = u + (k - 1) * s * 1.1f;
                    f.circle(0x40000000, tu + 1, v + 1.2f, s * 0.6f);
                    f.ring(0xFF1E1E20, s * 0.35f, tu, v, s * 0.45f);
                    f.ring(0xFF3A3A3E, 0.4f, tu - 0.3f, v - 0.3f, s * 0.45f);
                }
                break;
            case 5: // pallet wall
                f.box(0xFF9A7A50, u - s * 1.6f, v - s * 0.4f, u + s * 1.6f, v + s * 0.4f, 1.2f);
                for (float k = -1.2f; k <= 1.3f; k += 0.6f) f.line(0xFF6A4E30, 0.4f, u + k * s, v - s * 0.4f, u + k * s, v + s * 0.4f);
                f.line(0xFFB89A68, 0.5f, u - s * 1.6f, v - s * 0.3f, u + s * 1.6f, v - s * 0.3f);
                break;
            default: // drums
                for (int k = 0; k < 3; k++) {
                    float du = u + (k % 2) * s * 0.9f, dv = v + (k / 2) * s * 0.9f - s * 0.3f;
                    f.circle(0x40000000, du + 1, dv + 1.2f, s * 0.45f);
                    f.circle(0xFF3A5A7A, du, dv, s * 0.45f);
                    f.ring(0xFF2A3E52, 0.4f, du, dv, s * 0.3f);
                }
        }
    }

    static void paintball(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Paintball Arena", T_PARK) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return new int[][]{at(bw >= bh, 1, 1, 3, 2, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF8E7A5C, 0, 0, L, S);
                for (int i = 0; i < 30; i++) f.circle(r.nextBoolean() ? 0x18000000 : 0x14FFFFFF, r.nextFloat() * L, r.nextFloat() * S, 3 + r.nextFloat() * 6);
                // The safe zone by the hut: tables, a gear rack, a chrono station.
                float z = 4 * T + 4;
                f.rect(0xFFB0A898, 0, 3 * T + 4, z - 4, S);
                for (float v = 3 * T + 12; v < S - 12; v += 18) {
                    f.box(0xFF8A6A48, 8, v, 22, v + 6, 1);
                    man(f, 10, v - 2.5f, 0xFF4E6A3A);
                    for (int k = 0; k < 3; k++) f.circle(0xFF2A2A2E, 12 + k * 3, v + 3, 0.9f);
                }
                f.box(0xFF5A5E64, 4, 3 * T + 6, 6, S - 6, 0.6f);
                for (float v = 3 * T + 9; v < S - 8; v += 4) f.circle(0xFF2A2A2E, 5, v, 1.2f);
                // The field and its netting.
                float a0 = z + 2, a1 = L - 4, b0 = 4, b1 = S - 4, aw = a1 - a0, bh = b1 - b0;
                f.rect(0xFF9E8862, a0, b0, a1, b1);
                for (int i = 0; i < 26; i++) {
                    float u = a0 + r.nextFloat() * aw, v = b0 + r.nextFloat() * bh;
                    f.circle(0xFF6E8A48, u, v, 1 + r.nextFloat() * 1.5f);
                }
                f.line(0xFFF2F2EE, 0.5f, a0 + aw / 2, b0, a0 + aw / 2, b1);
                // A mirrored speedball layout on one side, scrapyard cover on the other.
                float[][] lay = {{0.12f, 0.25f, 1}, {0.14f, 0.75f, 0}, {0.26f, 0.5f, 3}, {0.3f, 0.15f, 2}, {0.36f, 0.82f, 1},
                        {0.42f, 0.38f, 0}, {0.45f, 0.65f, 2}, {0.24f, 0.88f, 4}, {0.38f, 0.1f, 5}};
                int[] cols = {0xFFD8402E, 0xFFF0C020, 0xFF2E6AC8, 0xFF2A2A2E};
                float s = Math.min(aw, bh) * 0.045f + 1.5f;
                for (int k = 0; k < lay.length; k++) {
                    int kind = (int) lay[k][2];
                    bunker(f, kind, a0 + aw * lay[k][0], b0 + bh * lay[k][1], s, cols[k % 4]);
                    bunker(f, kind == 3 ? 6 : kind, a0 + aw * (1 - lay[k][0]), b0 + bh * (1 - lay[k][1]), s, cols[(k + 2) % 4]);
                }
                bunker(f, 3, a0 + aw / 2, b0 + bh / 2, s * 1.2f, 0xFF2A2A2E);
                // Paint everywhere.
                int[] neon = {0xE0F040A0, 0xE060E040, 0xE0F0A020, 0xE040C0F0, 0xE0F0F040};
                for (int i = 0; i < 26; i++)
                    splat(f, r, a0 + 4 + r.nextFloat() * (aw - 8), b0 + 4 + r.nextFloat() * (bh - 8), 1 + r.nextFloat() * 1.8f, neon[r.nextInt(neon.length)]);
                // Players behind cover, masks on.
                for (int k = 0; k < 8; k++) {
                    float fu = k < 4 ? 0.1f + k * 0.1f : 0.9f - (k - 4) * 0.1f, fv = 0.2f + (k * 0.37f) % 0.65f;
                    float u = a0 + aw * fu, v = b0 + bh * fv;
                    f.circle(0x38000000, u + 0.8f, v + 0.9f, 2.1f);
                    f.circle(k < 4 ? 0xFF4E6A3A : 0xFF3A4E7A, u, v, 2.1f);
                    f.circle(0xFF5E7A44, u + 0.8f, v - 0.6f, 0.7f);
                    f.circle(0xFF1E1E22, u - 0.2f, v - 0.2f, 1.2f);
                    f.line(0xFF2A2A2E, 0.7f, u, v, u + (k < 4 ? 3 : -3), v + 0.5f);
                }
                // The flag stations at each end.
                for (int e = 0; e < 2; e++) {
                    float u = e == 0 ? a0 + 4 : a1 - 4, v = b0 + bh / 2;
                    f.circle(0x40000000, u + 0.6f, v + 0.6f, 0.9f);
                    f.circle(0xFF3A3A3A, u, v, 0.8f);
                    f.poly(e == 0 ? 0xFF4E9A3A : 0xFF3A6AC8, u, v - 0.5f, u + (e == 0 ? 5 : -5), v - 2, u, v - 3.5f);
                }
                // Netting round the field.
                f.frame(0x50E0E0E0, 3, a0 + 1, b0 + 1, a1 - 1, b1 - 1);
                f.frame(0xC0D8D8D8, 0.4f, a0, b0, a1, b1);
                for (float u = a0; u <= a1 + 0.1f; u += 14) {
                    f.circle(0xFF3A3A3A, u, b0, 0.9f);
                    f.circle(0xFF3A3A3A, u, b1, 0.9f);
                }
                for (float v = b0 + 14; v < b1; v += 14) {
                    f.circle(0xFF3A3A3A, a0, v, 0.9f);
                    f.circle(0xFF3A3A3A, a1, v, 0.9f);
                }
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                tin(f, 0xFF4E5A44, 0, 0, L, S, 2.6f);
                f.frame(0xFF2E3628, 0.8f, 0, 0, L, S);
                Random q = new Random(5);
                for (int i = 0; i < 4; i++) splat(f, q, 6 + q.nextFloat() * (L - 12), 5 + q.nextFloat() * (S - 10), 2 + q.nextFloat() * 1.5f, new int[]{0xE0F040A0, 0xE060E040, 0xE0F0A020}[i % 3]);
                f.sign(city, "PAINTBALL", L / 2, S / 2, 4, 0xFFF040A0);
            }
        };
        d.ground = City.DIRT;
        d.districts = Variants.IND | Variants.SUB | Variants.PARK;
        d.minS = 9;
        d.minL = 14;
        d.base = City.KIOSK;
        d.inside = Variants.I_RACKS;
        d.roofCol = 0xFF4E5A44;
        out.add(d);
    }

    // ------------------------------------------------------------------ 14. Archery Range

    static void archery(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Archery Range", T_PARK) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return new int[][]{at(bw >= bh, 1, 1, 2, 3, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF5A9846, 0, 0, L, S);
                float sl = 4 * T, tu = L - 2.2f * T;
                for (float u = sl; u < L; u += 10) f.rect(((int) ((u - sl) / 10)) % 2 == 0 ? 0xFF64A450 : 0xFF5C9C48, u, 0, Math.min(L, u + 10), S);
                // The waiting area behind the line: a striped gazebo, tackle boxes, benches.
                f.rect(0xFFC8BCA0, 3 * T + 4, 3, sl - 1, S - 3);
                int n = Math.max(3, (int) ((S - 12) / 22));
                float lane = (S - 8) / n;
                for (int k = 0; k <= n; k++) f.line(0x40FFFFFF, 0.4f, sl, 4 + k * lane, tu - 6, 4 + k * lane);
                float gv = 4 * T + 6;
                if (gv + 22 < S) {
                    for (int i = 0; i < 6; i++) f.rect(i % 2 == 0 ? 0xFF2E5A3A : 0xFFF0EEE6, 3, gv + i * 3.5f, 3 * T, gv + i * 3.5f + 3.5f);
                    f.rect(0x30000000, 3, gv + 21, 3 * T + 2, gv + 23);
                }
                for (int k = 0; k < n; k++) {
                    float v = 4 + (k + 0.5f) * lane;
                    f.box(KIT[k % KIT.length], 3 * T + 6, v - 4, 3 * T + 10, v - 1, 0.5f);
                }
                // The shooting line, archers at full draw.
                f.rect(0xFFF4F4F0, sl - 0.6f, 4, sl + 0.6f, S - 4);
                int dm = 0;
                for (float u = sl + 22; u < tu - 10; u += 22) {
                    f.line(0xA0F4F4F0, 0.4f, u, 4, u, S - 4);
                    for (int sd = 0; sd < 2; sd++) {
                        float v = sd == 0 ? 3 : S - 3;
                        f.box(0xFFF4F4F0, u - 2, v - 1.5f, u + 2, v + 1.5f, 0.5f);
                        f.rect(KIT[dm % 5], u - 1.2f, v - 0.8f, u + 1.2f, v + 0.8f);
                    }
                    dm++;
                }
                for (int k = 0; k < n; k++) {
                    float v = 4 + (k + 0.5f) * lane;
                    if (k % 3 == 2) continue;
                    float au = sl + 2.5f;
                    man(f, au, v, KIT[(k + 3) % KIT.length]);
                    f.arcLine(0xFF3A2A1A, 0.6f, au, v, 4, -55, 55);
                    f.line(0xFFE8E4DC, 0.3f, au + 2.3f, v - 3.3f, au, v);
                    f.line(0xFFE8E4DC, 0.3f, au + 2.3f, v + 3.3f, au, v);
                    f.line(0xFF6A5030, 0.4f, au, v, au + 5, v);
                    f.circle(0xFF2A2A2A, au - 1.5f, v + 1.8f, 0.8f);
                }
                // An arrow in flight.
                f.line(0xFF6A5030, 0.4f, L * 0.55f, 4 + lane * 0.5f, L * 0.55f + 5, 4 + lane * 0.5f);
                f.circle(0xFFE84030, L * 0.55f, 4 + lane * 0.5f, 0.4f);
                // Wind flags halfway down.
                for (int sd = 0; sd < 2; sd++) {
                    float u = (sl + tu) / 2, v = sd == 0 ? 2.5f : S - 2.5f;
                    f.circle(0xFF5A5A5A, u, v, 0.6f);
                    f.poly(0xFFF0C020, u, v - 0.3f, u + 4, v + 0.5f, u, v + 1.2f);
                }
                // The targets on their stands, arrows in the gold.
                for (int k = 0; k < n; k++) {
                    float v = 4 + (k + 0.5f) * lane, tr = Math.min(6.5f, lane * 0.32f);
                    f.line(0x40000000, 1, tu + 1, v + 1.5f, tu + 5, v + 2);
                    f.line(0xFF6A4A2A, 0.8f, tu - tr * 0.7f, v + tr + 1, tu + tr * 0.4f, v);
                    f.line(0xFF6A4A2A, 0.8f, tu + tr * 0.7f, v + tr + 1, tu - tr * 0.4f, v);
                    f.line(0xFF5A3A1A, 0.8f, tu + 4, v, tu, v);
                    f.circle(0x40000000, tu + 2, v + 2, tr + 0.5f);
                    f.circle(0xFFD8C890, tu, v, tr + 0.8f);
                    f.circle(0xFFF6F6F2, tu, v, tr);
                    f.circle(0xFF2A2A2E, tu, v, tr * 0.8f);
                    f.circle(0xFF3A8AD0, tu, v, tr * 0.6f);
                    f.circle(0xFFE04030, tu, v, tr * 0.4f);
                    f.circle(0xFFF0D030, tu, v, tr * 0.2f);
                    for (int a = 0; a < 3; a++) {
                        float au = tu + (r.nextFloat() - 0.5f) * tr * 0.8f, av = v + (r.nextFloat() - 0.5f) * tr * 0.8f;
                        f.line(0xFF4A3A2A, 0.35f, au, av, au - 1.5f, av + 0.8f);
                        f.circle(KIT[(k + a) % 6], au - 1.6f, av + 0.85f, 0.45f);
                    }
                    f.box(0xFFF4F4F0, tu - 3, v + tr + 2, tu + 3, v + tr + 4.5f, 0.4f);
                }
                // The earth backstop with netting along the top.
                f.rect(0xFF7A6248, L - T - 2, 2, L - 2, S - 2);
                f.rect(0xFF8E7656, L - T - 2, 2, L - T + 3, S - 2);
                f.rect(0xFF4E8A3E, L - 8, 2, L - 2, S - 2);
                for (float v = 4; v < S - 2; v += 6) f.bush(L - 5, v, 2.2f);
                f.line(0xA0D8DCE0, 1.5f, L - T - 4, 2, L - T - 4, S - 2);
                for (float v = 2; v <= S - 2; v += 16) f.circle(0xFF4A4E52, L - T - 4, v, 0.9f);
                f.tree(8, S - 8, 5, 0xFF3E7A3A);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                gable(f, 0xFF6A5A48, 0, 0, L, S);
                for (float u = 2.5f; u < L; u += 2.5f) f.line(0x22000000, 0.4f, u, 0, u, S);
                float cu = L / 2, cv = S / 2;
                f.circle(0xFFF6F6F2, cu, cv - S * 0.22f, 3.2f);
                f.circle(0xFFE04030, cu, cv - S * 0.22f, 2);
                f.circle(0xFFF0D030, cu, cv - S * 0.22f, 0.9f);
                f.sign(city, "ARCHERY", cu, cv, 4, 0xFF2E5A3A);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.SUB | Variants.PARK | Variants.OLD;
        d.minS = 8;
        d.minL = 16;
        d.base = City.KIOSK;
        d.inside = Variants.I_RACKS;
        d.roofCol = 0xFF6A5A48;
        out.add(d);
    }

    // ------------------------------------------------------------------ 15. Boxing Gym

    static void boxing(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Boxing Gym", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int l = Math.max(bw, bh), s = Math.min(bw, bh);
                return new int[][]{at(bw >= bh, 1, 1, l * 11 / 20, s - 2, 2)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float b1 = (1 + tiles(L) * 11 / 20) * T;
                f.rect(0xFF6A6C70, 0, 0, L, S);
                for (int i = 0; i < 30; i++) f.circle(0x10000000, r.nextFloat() * L, r.nextFloat() * S, 2 + r.nextFloat() * 5);
                // The rubber-tiled yard.
                float a0 = b1 + 8, a1 = L - 5, c0 = 5, c1 = S - 5;
                f.rect(0xFF34383E, a0, c0, a1, c1);
                for (float u = a0 + 8; u < a1; u += 8) f.line(0xFF2A2E34, 0.4f, u, c0, u, c1);
                for (float v = c0 + 8; v < c1; v += 8) f.line(0xFF2A2E34, 0.4f, a0, v, a1, v);
                float aw = a1 - a0, ah = c1 - c0;
                // A sled track of turf with lane lines.
                f.rect(0xFF3E8A4A, a0 + 2, c0 + 2, a1 - 2, c0 + 14);
                f.line(0xFFF2F2EE, 0.4f, a0 + 2, c0 + 8, a1 - 2, c0 + 8);
                for (float u = a0 + 12; u < a1 - 4; u += 12) f.line(0x80F2F2EE, 0.4f, u, c0 + 2, u, c0 + 14);
                f.box(0xFF3A3E44, a0 + aw * 0.6f, c0 + 3, a0 + aw * 0.6f + 6, c0 + 7, 0.8f);
                f.circle(0xFF1E1E22, a0 + aw * 0.6f + 3, c0 + 5, 1.5f);
                man(f, a0 + aw * 0.6f + 10, c0 + 5, 0xFFC83A30);
                f.line(0xFF2A2A2A, 0.4f, a0 + aw * 0.6f + 6, c0 + 5, a0 + aw * 0.6f + 9, c0 + 5);
                // The steel rig with hanging bags.
                float ru0 = a0 + 6, ru1 = a0 + Math.min(aw - 6, 46), rv0 = c0 + 22, rv1 = rv0 + 14;
                if (rv1 < c1 - 30) {
                    f.frame(0x40000000, 1.4f, ru0 + 1.2f, rv0 + 1.5f, ru1 + 1.2f, rv1 + 1.5f);
                    f.frame(0xFF8A9098, 1.2f, ru0, rv0, ru1, rv1);
                    for (float u = ru0; u <= ru1 + 0.1f; u += (ru1 - ru0) / 3) {
                        f.line(0xFF8A9098, 1, u, rv0, u, rv1);
                        f.box(0xFF5A6068, u - 1, rv0 - 1, u + 1, rv0 + 1, 0.5f);
                        f.box(0xFF5A6068, u - 1, rv1 - 1, u + 1, rv1 + 1, 0.5f);
                    }
                    for (int k = 0; k < 3; k++) {
                        float u = ru0 + (ru1 - ru0) * (k + 0.5f) / 3, v = (rv0 + rv1) / 2;
                        f.circle(0x50000000, u + 1.5f, v + 2, 2.5f);
                        f.circle(0xFFA8302A, u, v, 2.5f);
                        f.circle(0xFF2A2A2A, u, v, 1);
                        if (k != 1) man(f, u + 4, v + 1, k == 0 ? 0xFF2A2A2E : 0xFFE8C030);
                    }
                }
                // Tyres for flipping, battle ropes, kettlebells.
                float tv = c1 - 14;
                for (int k = 0; k < 2; k++) {
                    float u = a0 + 10 + k * 16;
                    f.circle(0x40000000, u + 1.5f, tv + 2, 6);
                    f.ring(0xFF1A1A1C, 3.2f, u, tv, 4.4f);
                    f.ring(0xFF3A3A3E, 0.5f, u - 0.4f, tv - 0.4f, 5.8f);
                    f.circle(0xFF4A4C50, u, tv, 2.6f);
                }
                man(f, a0 + 18, tv - 7, 0xFF2E5EA8);
                float ou = a1 - 8, ov = tv - 8;
                f.box(0xFF5A6068, ou - 1.5f, ov - 1.5f, ou + 1.5f, ov + 1.5f, 0.8f);
                Path q = new Path();
                f.mv(q, ou, ov - 1);
                for (int k = 0; k < 4; k++) f.qd(q, ou - 4 - k * 6, ov - 1 + (k % 2 == 0 ? -3 : 3), ou - 7 - k * 6, ov - 1);
                f.stroke(q, 0xFF1E1E22, 1);
                q = new Path();
                f.mv(q, ou, ov + 1);
                for (int k = 0; k < 4; k++) f.qd(q, ou - 4 - k * 6, ov + 1 + (k % 2 == 0 ? 3 : -3), ou - 7 - k * 6, ov + 1);
                f.stroke(q, 0xFF1E1E22, 1);
                man(f, ou - 29, ov, 0xFFF0F0EC);
                for (int k = 0; k < 6; k++) {
                    f.circle(0xFF1A1A1C, a1 - 6 - k * 3, c1 - 3, 1.1f);
                    f.circle(0xFF5A5C60, a1 - 6.4f - k * 3, c1 - 3.4f, 0.4f);
                }
                f.bench(b1 + 4, S / 2, false);
                f.box(0xFF2E6AC8, b1 + 2, S / 2 + 8, b1 + 6, S / 2 + 12, 0.6f);
                man(f, b1 + 4, 8, 0xFF2A2A2E);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                tin(f, 0xFF8A5A44, 0, 0, L, S, 2.4f);
                for (int i = 0; i < 6; i++) {
                    float a = r.nextFloat() * L;
                    f.rect(0x16000000, a, 0, Math.min(L, a + 2 + r.nextFloat() * 10), S);
                }
                f.frame(0xFF4A3024, 1.2f, 0, 0, L, S);
                // The skylight over the ring.
                float sk = Math.min(L * 0.48f, S - 12), su0 = L * 0.38f - sk / 2, sv0 = (S - sk) / 2, su1 = su0 + sk, sv1 = sv0 + sk;
                f.rect(0x40000000, su0 + 1.5f, sv0 + 1.5f, su1 + 2, sv1 + 2);
                f.rect(0xFF5A5E64, su0 - 1, sv0 - 1, su1 + 1, sv1 + 1);
                f.rect(0xFF3A3632, su0, sv0, su1, sv1);
                float rcu = (su0 + su1) / 2, rcv = (sv0 + sv1) / 2, rs = sk * 0.36f;
                f.rect(0xFF1E1E22, rcu - rs - 2, rcv - rs - 2, rcu + rs + 2, rcv + rs + 2);
                f.rect(0xFF2E5EA8, rcu - rs, rcv - rs, rcu + rs, rcv + rs);
                f.rect(0x18FFFFFF, rcu - rs, rcv - rs, rcu + rs, rcv);
                f.circle(0x30FFFFFF, rcu, rcv, rs * 0.3f);
                f.frame(0xFFC83A30, 0.6f, rcu - rs + 0.5f, rcv - rs + 0.5f, rcu + rs - 0.5f, rcv + rs - 0.5f);
                f.frame(0xFFF2F2EE, 0.6f, rcu - rs + 1.4f, rcv - rs + 1.4f, rcu + rs - 1.4f, rcv + rs - 1.4f);
                f.frame(0xFF2E3E9A, 0.6f, rcu - rs + 2.3f, rcv - rs + 2.3f, rcu + rs - 2.3f, rcv + rs - 2.3f);
                f.box(0xFFC83A30, rcu - rs - 1, rcv - rs - 1, rcu - rs + 1, rcv - rs + 1, 0.5f);
                f.box(0xFF2E5EA8, rcu + rs - 1, rcv + rs - 1, rcu + rs + 1, rcv + rs + 1, 0.5f);
                f.box(0xFFF2F2EE, rcu + rs - 1, rcv - rs - 1, rcu + rs + 1, rcv - rs + 1, 0.5f);
                f.box(0xFFF2F2EE, rcu - rs - 1, rcv + rs - 1, rcu - rs + 1, rcv + rs + 1, 0.5f);
                man(f, rcu - 2.5f, rcv - 1, 0xFFC83A30);
                f.circle(0xFFC83A30, rcu - 0.8f, rcv - 1.5f, 0.7f);
                man(f, rcu + 2.5f, rcv + 1, 0xFF2E5EA8);
                f.circle(0xFF2E5EA8, rcu + 0.8f, rcv + 1.5f, 0.7f);
                man(f, rcu + rs * 0.5f, rcv - rs * 0.6f, 0xFFF4F4F4);
                for (int k = 0; k < 6; k++) man(f, su0 + 2 + k * sk / 6, sv1 - 2, 0xFF5A5A60);
                f.rect(0x30A8D0E8, su0, sv0, su1, sv1);
                for (float u = su0 + sk / 4; u < su1 - 1; u += sk / 4) f.line(0xFF6A6E74, 0.6f, u, sv0, u, sv1);
                f.line(0xFF6A6E74, 0.6f, su0, rcv, su1, rcv);
                f.line(0x60FFFFFF, 0.6f, su0 + 1, sv0 + 1, su0 + sk * 0.3f, sv0 + 1);
                // A second light over the bag room.
                float bu0 = su1 + 6, bu1 = L - 6;
                if (bu1 - bu0 > 10) {
                    f.rect(0xFF5A5E64, bu0 - 1, S / 2 - 7, bu1 + 1, S / 2 + 7);
                    f.rect(0xFF3A3632, bu0, S / 2 - 6, bu1, S / 2 + 6);
                    for (float u = bu0 + 4; u < bu1 - 2; u += 7) {
                        f.circle(0xFFA8302A, u, S / 2 - 2.5f, 1.8f);
                        f.circle(0xFF2A2A2A, u + 3, S / 2 + 2.5f, 1.8f);
                    }
                    f.rect(0x30A8D0E8, bu0, S / 2 - 6, bu1, S / 2 + 6);
                }
                for (int k = 0; k < 3; k++) {
                    float u = 6 + k * 8;
                    f.circle(0x40000000, u + 1, S - 7, 2);
                    f.circle(0xFF9AA0A8, u, S - 8, 2);
                    f.circle(0xFF6A7078, u, S - 8, 1);
                }
                f.sign(city, "BOXING", rcu, sv1 + 3, 5, 0xFFC83A30);
            }
        };
        d.ground = City.LOT;
        d.districts = Variants.IND | Variants.MID | Variants.OLD | Variants.DOWN;
        d.minS = 9;
        d.minL = 14;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_GYM;
        d.roofCol = 0xFF8A5A44;
        out.add(d);
    }

    // ------------------------------------------------------------------ 16. Yoga Garden

    static void yoga(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Yoga Garden", T_PARK) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int s = Math.min(bw, bh);
                return new int[][]{at(bw >= bh, 1, s / 2 - 1, 3, 3, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, cv = S / 2;
                f.rect(0xFFDCD6C6, 0, 0, L, S);
                for (float v = 3; v < S; v += 3) f.line(0x0E000000, 0.5f, 0, v, L, v);
                // Bamboo screens along the long edges.
                for (int sd = 0; sd < 2; sd++) {
                    float v = sd == 0 ? 3 : S - 3;
                    f.rect(0xFF4E7A3A, 0, v - 3, L, v + 3);
                    for (float u = 1; u < L; u += 3.6f) {
                        float a = r.nextFloat() * 6.28f;
                        f.line(r.nextBoolean() ? 0xFF6A9E48 : 0xFF84B85A, 0.7f, u, v, u + (float) Math.cos(a) * 3, v + (float) Math.sin(a) * 3);
                    }
                }
                // The rill: a stone channel straight down the garden, with little weirs.
                float r0 = 4 * T + 10, r1 = L - 14;
                f.rect(0x30000000, r0 + 1, cv - 3, r1 + 1, cv + 4.5f);
                f.rect(0xFFB8B0A0, r0, cv - 3.5f, r1, cv + 3.5f);
                f.rect(0xFF3E7EA0, r0 + 1.5f, cv - 1.8f, r1, cv + 1.8f);
                f.rect(0xFF5A98B8, r0 + 1.5f, cv - 1.8f, r1, cv - 0.8f);
                for (float u = r0 + 16; u < r1 - 4; u += 16) {
                    f.line(0xC0FFFFFF, 0.6f, u, cv - 1.8f, u, cv + 1.8f);
                    f.line(0x60FFFFFF, 0.4f, u + 1.2f, cv - 1.4f, u + 1.2f, cv + 1.4f);
                }
                f.box(0xFF8E8676, r0 - 4, cv - 4, r0 + 1.5f, cv + 4, 1);
                f.circle(0xFF5A98B8, r0 - 1.2f, cv, 1.4f);
                // The lotus basin at the end.
                float bu = L - 12, br = Math.min(10, S * 0.12f);
                f.circle(0x30000000, bu + 1.5f, cv + 2, br + 2);
                f.circle(0xFFB8B0A0, bu, cv, br + 2);
                f.circle(0xFF3E7EA0, bu, cv, br);
                for (int k = 0; k < 6; k++) {
                    double a = k * 1.05f + 0.3f;
                    float pu = bu + (float) Math.cos(a) * br * 0.6f, pv = cv + (float) Math.sin(a) * br * 0.6f;
                    f.circle(0xFF5A9A48, pu, pv, 1.6f);
                    if (k % 2 == 0) f.circle(0xFFF2B8C8, pu + 0.5f, pv - 0.5f, 0.8f);
                }
                // Timber decks either side, mats in rows and people in poses.
                int[] mats = {0xFF8AB8C8, 0xFFC8A0C8, 0xFFE8B888, 0xFF9AC890, 0xFFE89A9A, 0xFFB8B0E0};
                float dl = Math.max(40, Math.min(64, (r1 - r0) / 2.2f));
                int dn = Math.max(1, (int) ((r1 - r0 + 8) / (dl + 8)));
                dl = (r1 - r0 - (dn - 1) * 8) / dn;
                int slots = 0;
                for (int sd = 0; sd < 2; sd++) {
                    float d0 = sd == 0 ? 10 : cv + 8, d1 = sd == 0 ? cv - 8 : S - 10;
                    slots += dn * ((int) ((dl - 4) / 12)) * ((int) ((d1 - d0 - 4) / 15));
                }
                float keep = Math.min(0.6f, 30f / Math.max(1, slots));
                for (int sd = 0; sd < 2; sd++) {
                    float d0 = sd == 0 ? 10 : cv + 8, d1 = sd == 0 ? cv - 8 : S - 10;
                    for (int k = 0; k < dn; k++) {
                        float u0 = r0 + k * (dl + 8), u1 = u0 + dl;
                        f.box(0xFFB48C62, u0, d0, u1, d1, 1.5f);
                        for (float v = d0 + 3.5f; v < d1; v += 3.5f) f.line(0x20000000, 0.4f, u0, v, u1, v);
                        f.frame(0xFF8A6A48, 0.6f, u0, d0, u1, d1);
                        int cols = (int) ((u1 - u0 - 4) / 12), rows = (int) ((d1 - d0 - 4) / 15);
                        for (int i = 0; i < cols; i++)
                            for (int j = 0; j < rows; j++) {
                                if (r.nextFloat() > keep) continue;
                                float mu = u0 + 4 + i * 12, mv = d0 + 3 + j * 15;
                                f.rect(mats[(i + j + k) % mats.length], mu, mv, mu + 5, mv + 10);
                                float pu = mu + 2.5f, pv = mv + 5;
                                int pose = (i + j * 2 + k) % 3;
                                int top = mats[(i + 3 + j) % mats.length];
                                if (pose == 0) {
                                    f.line(0xFF5A4A3A, 0.6f, pu - 3.5f, pv - 0.5f, pu + 3.5f, pv - 0.5f);
                                } else if (pose == 1) {
                                    f.line(0xFF5A4A3A, 0.6f, pu, pv, pu - 1.5f, pv + 4);
                                    f.line(0xFF5A4A3A, 0.6f, pu, pv, pu + 1.5f, pv + 4);
                                }
                                man(f, pu, pv, City.darken(top, 0.7f));
                                if (pose == 2) f.line(SKIN[(i + j) % SKIN.length], 0.6f, pu, pv - 2, pu, pv - 4.5f);
                            }
                    }
                }
                // Stepping stones over the rill, olive trees and lavender at the ends.
                for (int k = 0; k < dn - 1; k++) {
                    float u = r0 + (k + 1) * (dl + 8) - 4;
                    for (int j = -2; j <= 2; j++) f.oval(0xFFC8C0B0, u - 2, cv + j * 4 - 1.5f, u + 2, cv + j * 4 + 1.5f);
                }
                for (int sd = 0; sd < 2; sd++) {
                    float v = sd == 0 ? 14 : S - 14;
                    f.tree(4 * T + 4, v, 5, 0xFF8A9A6A);
                    f.tree(L - 8, v, 5, 0xFF8A9A6A);
                    for (float u = L - 24; u < L - 14; u += 3) f.circle(0xFF9A88C8, u, v + (sd == 0 ? 6 : -6), 1.4f);
                }
                f.bench(4 * T + 4, cv, false);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF6A5038, 0, 0, L, S);
                hip(f, 0xFF9A7A54, 1, 1, L - 1, S - 1);
                for (int k = 1; k < 5; k++) {
                    float in = k * Math.min(L, S) / 10;
                    f.frame(0x20000000, 0.4f, 1 + in, 1 + in, L - 1 - in, S - 1 - in);
                }
                f.circle(0xFF6A4A2A, L / 2, S / 2, 2);
                f.circle(0xFFD8B860, L / 2 - 0.4f, S / 2 - 0.4f, 1);
                f.sign(city, "YOGA", L / 2, S / 2 + 6, 4, 0xFF6A4A2A);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.MID | Variants.UNI | Variants.PARK | Variants.OLD;
        d.minS = 9;
        d.minL = 14;
        d.base = City.KIOSK;
        d.inside = Variants.I_GYM;
        d.roofCol = 0xFF9A7A54;
        out.add(d);
    }

    // ------------------------------------------------------------------ 17. Formal Rose Garden

    static final int[] ROSES = {0xFFD8304A, 0xFFF08AA8, 0xFFF6F0E8, 0xFFF0C850, 0xFFE86A3A, 0xFFB02040};

    static void rose(F f, float u, float v, int col) {
        f.circle(0xFF3E6A32, u, v, 1.5f);
        f.circle(col, u - 0.3f, v - 0.3f, 0.85f);
    }

    /** One parterre bed: roses massed in a pattern under a lattice of leaves, edged with box hedge. */
    static void parterre(F f, Random r, float u0, float v0, float u1, float v1, int kind) {
        int hd = 0xFF2E5A2A, leaf = 0xFF3A6630;
        f.rect(leaf, u0, v0, u1, v1);
        float w = u1 - u0, h = v1 - v0, cu = (u0 + u1) / 2, cv = (v0 + v1) / 2;
        int a = ROSES[kind % ROSES.length], b = ROSES[(kind + 2) % ROSES.length];
        if (kind % 3 == 0) {
            f.poly(a, u0, v0, u1, v0, cu, cv);
            f.poly(a, u0, v1, u1, v1, cu, cv);
            f.poly(b, u0, v0, u0, v1, cu, cv);
            f.poly(b, u1, v0, u1, v1, cu, cv);
        } else if (kind % 3 == 1) {
            float rr = Math.min(w, h) * 0.38f;
            f.rect(b, u0, v0, u1, v1);
            f.circle(a, cu, cv, rr);
            f.circle(0xFF6AA650, cu, cv, rr * 0.45f);
        } else {
            int rows = Math.max(3, (int) (h / 7));
            for (int j = 0; j < rows; j++) f.rect(j % 2 == 0 ? a : b, u0, v0 + j * h / rows, u1, v0 + (j + 1) * h / rows);
        }
        // The leaves between the blooms.
        for (float u = u0 + 2f; u < u1; u += 4f) f.line(leaf, 2f, u, v0, u, v1);
        for (float v = v0 + 2f; v < v1; v += 4f) f.line(leaf, 2f, u0, v, u1, v);
        if (kind % 3 == 0) {
            f.line(hd, 1.6f, u0, v0, u1, v1);
            f.line(hd, 1.6f, u0, v1, u1, v0);
            f.tree(cu, cv, 2.6f, 0xFF4A7A3A);
            f.circle(a, cu - 0.6f, cv - 0.6f, 1);
        } else if (kind % 3 == 1) {
            float rr = Math.min(w, h) * 0.38f;
            f.ring(hd, 1.6f, cu, cv, rr);
            f.ring(hd, 1.2f, cu, cv, rr * 0.45f);
            f.tree(cu, cv, 2.8f, 0xFF4A7A3A);
            f.circle(b, cu - 0.6f, cv - 0.6f, 1);
            f.circle(b, cu + 0.8f, cv + 0.4f, 0.8f);
        }
        f.frame(0x40000000, 2, u0 + 0.6f, v0 + 0.8f, u1 + 0.6f, v1 + 0.8f);
        f.frame(hd, 2.2f, u0, v0, u1, v1);
        f.frame(0xFF4A7A3A, 0.6f, u0 - 0.7f, v0 - 0.7f, u1 - 0.7f, v1 - 0.7f);
    }

    static void roseGarden(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Formal Rose Garden", T_PARK) {
            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, cu = L / 2, cv = S / 2;
                f.rect(0xFFDCCFB0, 0, 0, L, S);
                for (int i = 0; i < 40; i++) f.circle(0x10000000, r.nextFloat() * L, r.nextFloat() * S, 0.6f);
                // A clipped yew hedge round it all.
                f.frame(0xFF2A4A26, 5, 2.5f, 2.5f, L - 2.5f, S - 2.5f);
                f.frame(0xFF3A6232, 1.2f, 1.2f, 1.2f, L - 1.2f, S - 1.2f);
                // The beds: a grid either side of the main walk.
                float pw = 10, cw = 8, m = 10;
                int cols = Math.max(2, Math.round((L - 2 * m) / ((S - 2 * m - pw) / 2 + cw)));
                if (cols % 2 == 1) cols++;
                float bw = (L - 2 * m - (cols - 1) * cw) / cols, bh = (S - 2 * m - pw) / 2;
                for (int i = 0; i < cols; i++)
                    for (int j = 0; j < 2; j++) {
                        float u0 = m + i * (bw + cw), v0 = j == 0 ? m : cv + pw / 2;
                        parterre(f, r, u0, v0, u0 + bw, v0 + bh, i + j * 2 + (j == 1 ? 1 : 0));
                    }
                // The pergola down the main walk, roses climbing its posts.
                float rr = Math.min(S * 0.17f, 22);
                for (int sd = -1; sd <= 1; sd += 2) {
                    float a = sd < 0 ? m : cu + rr + 4, b = sd < 0 ? cu - rr - 4 : L - m;
                    for (int k = 0; k < 2; k++) {
                        float v = k == 0 ? cv - pw / 2 : cv + pw / 2;
                        f.line(0x40000000, 1.2f, a + 1, v + 1.4f, b + 1, v + 1.4f);
                        f.line(0xFF7A5A3A, 1.1f, a, v, b, v);
                    }
                    for (float u = a + 2; u < b - 1; u += 5) {
                        f.line(0x40000000, 1, u + 1, cv - pw / 2 - 1 + 1.4f, u + 1, cv + pw / 2 + 1 + 1.4f);
                        f.line(0xFF8A6A48, 0.9f, u, cv - pw / 2 - 1, u, cv + pw / 2 + 1);
                    }
                    for (float u = a + 4.5f; u < b - 1; u += 10) {
                        for (int k = 0; k < 2; k++) {
                            float v = k == 0 ? cv - pw / 2 : cv + pw / 2;
                            blob(f, 0xFF3E6A32, u, v, 2.6f, 2.2f, jit(r, 6, 0.3f));
                            f.circle(ROSES[(int) (u / 10 + k) % 3], u - 0.8f, v - 0.5f, 0.8f);
                            f.circle(ROSES[(int) (u / 10 + k) % 3], u + 0.9f, v + 0.6f, 0.7f);
                        }
                    }
                }
                // The central roundel: paving rings, a rose bed and a sundial.
                f.circle(0x30000000, cu + 1, cv + 1.5f, rr + 1);
                f.circle(0xFFE6DCC4, cu, cv, rr);
                f.ring(0xFFC8BCA0, 1, cu, cv, rr - 1);
                f.circle(0xFF6A4E36, cu, cv, rr * 0.62f);
                for (int i = 0; i < 14; i++) {
                    double an = i * Math.PI / 7;
                    rose(f, cu + (float) Math.cos(an) * rr * 0.48f, cv + (float) Math.sin(an) * rr * 0.48f, ROSES[i % 2 == 0 ? 0 : 1]);
                }
                f.ring(0xFF2E5A2A, 1.4f, cu, cv, rr * 0.62f);
                f.circle(0x40000000, cu + 1, cv + 1.2f, rr * 0.25f);
                f.circle(0xFFC8C0B0, cu, cv, rr * 0.25f);
                f.circle(0xFFB8A878, cu, cv, rr * 0.18f);
                for (int i = 0; i < 12; i++) {
                    double an = i * Math.PI / 6;
                    f.line(0xFF6A5A3A, 0.3f, cu + (float) Math.cos(an) * rr * 0.12f, cv + (float) Math.sin(an) * rr * 0.12f, cu + (float) Math.cos(an) * rr * 0.17f, cv + (float) Math.sin(an) * rr * 0.17f);
                }
                f.poly(0xFF5A4A2A, cu, cv - 0.4f, cu + rr * 0.15f, cv, cu, cv + 0.4f);
                f.poly(0x40000000, cu, cv + 0.4f, cu + rr * 0.15f, cv, cu + rr * 0.1f, cv + rr * 0.08f);
                // Topiary cones, urns and benches at the ends of the walk.
                for (int sd = 0; sd < 2; sd++) {
                    float u = sd == 0 ? m - 4 : L - m + 4;
                    f.box(0xFFB8B0A0, u - 2.5f, cv - 2.5f, u + 2.5f, cv + 2.5f, 1);
                    f.circle(0xFF3E6A32, u, cv, 1.8f);
                    f.circle(ROSES[0], u - 0.5f, cv - 0.5f, 0.7f);
                    for (int k = -1; k <= 1; k += 2) {
                        f.tree(u + (sd == 0 ? 6 : -6), cv + k * (pw / 2 + 3), 2.6f, 0xFF2E5A2A);
                    }
                }
                for (int i = 1; i < cols; i++) {
                    float u = m + i * (bw + cw) - cw / 2;
                    if (Math.abs(u - cu) < rr + 4) continue;
                    f.bench(u, 6, true);
                    f.bench(u, S - 6, true);
                }
                man(f, cu - rr - 8, cv + 1, 0xFF8A6A9A);
                man(f, cu - rr - 11, cv - 1, 0xFFC8A060);
                man(f, cu + rr + 14, cv + 2, 0xFF3A6A8A);
                f.lamp(cu - rr - 2, cv - rr * 0.7f);
                f.lamp(cu + rr + 2, cv + rr * 0.7f);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.OLD | Variants.MID | Variants.PARK | Variants.SUB;
        d.minS = 9;
        d.minL = 12;
        out.add(d);
    }

    // ------------------------------------------------------------------ 18. Butterfly House

    static void butterflyBug(F f, float u, float v, int col, float s) {
        f.oval(col, u - 1.6f * s, v - 1.1f * s, u - 0.1f * s, v + 0.2f * s);
        f.oval(col, u + 0.1f * s, v - 1.1f * s, u + 1.6f * s, v + 0.2f * s);
        f.oval(City.darken(col, 0.8f), u - 1.1f * s, v + 0.1f * s, u - 0.1f * s, v + 1f * s);
        f.oval(City.darken(col, 0.8f), u + 0.1f * s, v + 0.1f * s, u + 1.1f * s, v + 1f * s);
        f.line(0xFF2A2A2A, 0.35f * s, u, v - 1.1f * s, u, v + 0.9f * s);
    }

    static final int[] WINGS = {0xFFF08A20, 0xFF3A8AE0, 0xFFF0D030, 0xFFE04A8A, 0xFFF4F4F0, 0xFF60C0A0, 0xFFD83A2A};

    static void butterfly(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Butterfly House", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int l = Math.max(bw, bh), s = Math.min(bw, bh), q = Math.min(s - 3, l - 5);
                return new int[][]{at(bw >= bh, (l - q) / 2, (s - q) / 2, q, q, 2)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] pl = plan(tiles(x1 - x0), tiles(y1 - y0), r);
                float[] b = bu(pl[0], !f.t);
                f.rect(0xFF64A04E, 0, 0, L, S);
                // A gravel walk round the house and up to its door.
                f.rect(0xFFD8CCB0, b[0] - 8, b[1] - 8, b[2] + 8, b[3] + 8);
                f.rect(0xFFD8CCB0, (b[0] + b[2]) / 2 - 5, b[3], (b[0] + b[2]) / 2 + 5, S);
                f.rect(0xFF64A04E, b[0] - 3, b[1] - 3, b[2] + 3, b[3] + 3);
                // Butterfly borders: buddleia, lavender and flower drifts all round.
                int[] fl = {0xFF9A6AC8, 0xFFB088D8, 0xFFE8A0C0, 0xFFF0D060, 0xFFF4F0E8, 0xFFE87A4A};
                for (float u = 6; u < L - 4; u += 18) {
                    for (int sd = 0; sd < 2; sd++) {
                        float v = sd == 0 ? 5 : S - 5;
                        if (sd == 1 && Math.abs(u - (b[0] + b[2]) / 2) < 10) continue;
                        if (v > b[1] - 10 && v < b[3] + 10 && u > b[0] - 10 && u < b[2] + 10) continue;
                        blob(f, 0xFF4E8A3E, u, v, 5, 3.5f, jit(r, 6, 0.25f));
                        flowers(f, r, u, v, 3.5f, 5, fl[r.nextInt(fl.length)]);
                    }
                }
                float[][] sides = {{3, b[0] - 11}, {b[2] + 11, L - 3}};
                for (float[] sd : sides) {
                    float w = sd[1] - sd[0];
                    if (w < 14) continue;
                    int nb = Math.max(1, Math.round((S - 20) / (w * 1.3f)));
                    for (int k = 0; k < nb; k++) {
                        float u = (sd[0] + sd[1]) / 2, v = 12 + (S - 24) * (k + 0.5f) / nb, ru = w * 0.42f, rv = Math.min((S - 24) / nb * 0.42f, ru * 1.4f);
                        blob(f, 0xFF6A5040, u, v, ru + 1.5f, rv + 1.5f, jit(r, 7, 0.15f));
                        blob(f, 0xFF4E8A3E, u, v, ru, rv, jit(r, 7, 0.15f));
                        int ns = Math.max(3, Math.min(8, (int) (ru * rv / 70)));
                        for (int i2 = 0; i2 < ns; i2++) {
                            double an = i2 * 2.4f;
                            float dd = 0.62f * (float) Math.sqrt((i2 + 0.5f) / ns);
                            float cu = u + (float) Math.cos(an) * ru * dd, cv = v + (float) Math.sin(an) * rv * dd;
                            f.circle(0xFF3E7A36, cu, cv, Math.min(ru, rv) * 0.3f);
                            for (int k2 = 0; k2 < 3; k2++) f.oval(fl[k2 % 2], cu - 1.2f + (k2 - 1) * 2.2f, cv - 2.4f, cu + 1.2f + (k2 - 1) * 2.2f, cv + 1.2f);
                        }
                        for (int i2 = 0; i2 < ns / 2 + 1; i2++)
                            flowers(f, r, u + (r.nextFloat() - 0.5f) * ru, v + (r.nextFloat() - 0.5f) * rv, Math.min(ru, rv) * 0.25f, 7, fl[2 + (i2 + k) % 4]);
                    }
                }
                for (int k = 0; k < 9; k++) butterflyBug(f, 4 + r.nextFloat() * (L - 8), 4 + r.nextFloat() * (S - 8), WINGS[k % WINGS.length], 0.9f);
                f.bench((b[0] + b[2]) / 2 - 10, S - 8, false);
                f.bench((b[0] + b[2]) / 2 + 10, S - 8, false);
                f.lamp((b[0] + b[2]) / 2 - 7, b[3] + 5);
                f.lamp((b[0] + b[2]) / 2 + 7, b[3] + 5);
                for (int i = 0; i < 4; i++) man(f, (b[0] + b[2]) / 2 + (i - 1.5f) * 3, b[3] + 8 + i * 3, KIT[i]);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, cu = L / 2, cv = S / 2, R = Math.min(L, S) / 2 - 1.5f;
                // A stone plinth with planters in the corners.
                f.rect(0xFFB8B0A0, 0, 0, L, S);
                f.frame(0xFF8E8676, 1, 0.5f, 0.5f, L - 0.5f, S - 0.5f);
                for (int k = 0; k < 4; k++) {
                    float u = k % 2 == 0 ? 4 : L - 4, v = k / 2 == 0 ? 4 : S - 4;
                    f.circle(0xFF6A5040, u, v, 3);
                    f.bush(u, v, 2.4f);
                }
                f.box(0xFFC8E0E4, cu - 5, S - 5, cu + 5, S, 0.5f);
                // Inside the dome: jungle, a pool, a path and flowers.
                f.circle(0x40000000, cu + 2, cv + 2.5f, R);
                f.circle(0xFF2A5A2E, cu, cv, R);
                Path q = new Path();
                f.mv(q, cu, cv + R);
                f.qd(q, cu - R * 0.6f, cv + R * 0.2f, cu, cv);
                f.qd(q, cu + R * 0.6f, cv - R * 0.3f, cu - R * 0.2f, cv - R * 0.8f);
                f.stroke(q, 0xFFB09A78, 3);
                blob(f, 0xFF3A8AB0, cu + R * 0.35f, cv + R * 0.35f, R * 0.22f, R * 0.16f, jit(r, 7, 0.2f));
                int[] g = {0xFF3E7A36, 0xFF4E9A40, 0xFF2E6A30, 0xFF6AAA48, 0xFF387A50};
                for (int k = 0; k < 18; k++) {
                    double an = r.nextFloat() * Math.PI * 2;
                    float dd = R * (0.25f + 0.65f * r.nextFloat());
                    float u = cu + (float) Math.cos(an) * dd, v = cv + (float) Math.sin(an) * dd;
                    blob(f, g[k % g.length], u, v, R * 0.14f, R * 0.12f, jit(r, 6, 0.3f));
                }
                for (int k = 0; k < 4; k++) {
                    double an = k * Math.PI / 2 + 0.7f;
                    palm(f, cu + (float) Math.cos(an) * R * 0.6f, cv + (float) Math.sin(an) * R * 0.6f, R * 0.2f);
                }
                int[] fc = {0xFFE83A3A, 0xFFF08A20, 0xFFF060A0, 0xFFF0D030};
                for (int k = 0; k < 26; k++) {
                    double an = r.nextFloat() * Math.PI * 2;
                    float dd = R * 0.9f * (float) Math.sqrt(r.nextFloat());
                    f.circle(fc[k % 4], cu + (float) Math.cos(an) * dd, cv + (float) Math.sin(an) * dd, 0.8f);
                }
                for (int k = 0; k < 9; k++) {
                    double an = r.nextFloat() * Math.PI * 2;
                    float dd = R * 0.85f * (float) Math.sqrt(r.nextFloat());
                    butterflyBug(f, cu + (float) Math.cos(an) * dd, cv + (float) Math.sin(an) * dd, WINGS[k % WINGS.length], 0.9f);
                }
                // The geodesic glazing.
                f.circle(0x30D0ECF4, cu, cv, R);
                int rings = 4;
                float[][] pu = new float[rings + 1][], pv = new float[rings + 1][];
                for (int k = 1; k <= rings; k++) {
                    int n = 5 * k;
                    float rad = R * (float) Math.sin(k * Math.PI / 2 / rings);
                    pu[k] = new float[n];
                    pv[k] = new float[n];
                    for (int i = 0; i < n; i++) {
                        double an = i * 2 * Math.PI / n + (k % 2) * Math.PI / n;
                        pu[k][i] = cu + (float) Math.cos(an) * rad;
                        pv[k][i] = cv + (float) Math.sin(an) * rad;
                    }
                }
                int ln = 0x90E8F4F8;
                for (int k = 1; k <= rings; k++) {
                    int n = pu[k].length;
                    for (int i = 0; i < n; i++) {
                        f.line(ln, 0.45f, pu[k][i], pv[k][i], pu[k][(i + 1) % n], pv[k][(i + 1) % n]);
                        if (k == 1) f.line(ln, 0.45f, cu, cv, pu[k][i], pv[k][i]);
                        else {
                            int m = pu[k - 1].length;
                            int j = Math.round(i * m / (float) n) % m;
                            f.line(ln, 0.45f, pu[k][i], pv[k][i], pu[k - 1][j], pv[k - 1][j]);
                            int j2 = (int) Math.floor(i * m / (float) n + 0.5f + 0.5f) % m;
                            if (j2 != j) f.line(ln, 0.45f, pu[k][i], pv[k][i], pu[k - 1][j2], pv[k - 1][j2]);
                        }
                    }
                }
                f.ring(0xFF8A9298, 1.2f, cu, cv, R);
                f.oval(0x38FFFFFF, cu - R * 0.75f, cv - R * 0.8f, cu - R * 0.05f, cv - R * 0.25f);
                f.oval(0x24FFFFFF, cu - R * 0.6f, cv - R * 0.6f, cu + R * 0.1f, cv);
                f.sign(city, "BUTTERFLY HOUSE", cu, S - 3, 4, 0xFF2E6A30);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.PARK | Variants.MID | Variants.SUB | Variants.OLD;
        d.minS = 10;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_SHOWROOM;
        d.roofCol = 0xFFC8E0E4;
        out.add(d);
    }

    // ------------------------------------------------------------------ 19. Petting Farm

    static void pettingFarm(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Petting Farm", T_PARK) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return new int[][]{at(bw >= bh, 1, 1, 4, 3, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF6AA650, 0, 0, L, S);
                for (int i = 0; i < 30; i++) f.circle(0x12000000, r.nextFloat() * L, r.nextFloat() * S, 2 + r.nextFloat() * 4);
                // The farmyard: packed earth round the barn and down a track.
                f.rect(0xFFB09A74, 0, 0, 6 * T, 5 * T);
                f.rect(0xFFB09A74, 5 * T, S * 0.6f, L, S * 0.6f + 9);
                // Pens along the top: goats, sheep, pigs, rabbits.
                float a0 = 6 * T + 4, a1 = L - 4, b0 = 4, b1 = S * 0.6f - 4;
                int n = Math.max(2, Math.min(4, (int) ((a1 - a0) / 38)));
                float pw = (a1 - a0) / n;
                int[] kinds = {4, 2, 3, 9};
                for (int k = 0; k < n; k++) {
                    float u0 = a0 + k * pw + 2, u1 = u0 + pw - 4;
                    int kind = kinds[k % 4];
                    int gnd = kind == 3 ? 0xFF8A6A48 : kind == 4 ? 0xFFC8B070 : kind == 9 ? 0xFF7AAA5A : 0xFF72AE58;
                    f.rect(gnd, u0, b0, u1, b1);
                    for (int i = 0; i < 8; i++) f.circle(0x14000000, u0 + r.nextFloat() * (u1 - u0), b0 + r.nextFloat() * (b1 - b0), 1 + r.nextFloat() * 3);
                    if (kind == 3) {
                        blob(f, 0xFF6A5038, (u0 + u1) / 2, (b0 + b1) * 0.6f, (u1 - u0) * 0.25f, 6, jit(r, 6, 0.3f));
                        blob(f, 0xFF7A8A8A, (u0 + u1) / 2, (b0 + b1) * 0.6f, (u1 - u0) * 0.12f, 3, jit(r, 6, 0.3f));
                    }
                    if (kind == 9) {
                        f.box(0xFF9A7A50, u0 + 3, b0 + 3, u0 + 15, b0 + 10, 1);
                        f.rect(0xFF7A5A38, u0 + 3, b0 + 3, u0 + 15, b0 + 5);
                        f.frame(0x90E8E8E8, 0.4f, u0 + 3, b0 + 12, u1 - 3, b1 - 3);
                    }
                    // A shelter in the back corner and a trough.
                    f.box(0xFF8A6A48, u1 - 10, b0 + 2, u1 - 2, b0 + 9, 1);
                    f.rect(0xFFA8845A, u1 - 10, b0 + 2, u1 - 2, b0 + 5);
                    f.box(0xFF7A7E84, u0 + 3, b1 - 5, u0 + 10, b1 - 2.5f, 0.5f);
                    int na = kind == 9 ? 4 : 3;
                    for (int i = 0; i < na; i++) {
                        float au = u0 + 6 + r.nextFloat() * (u1 - u0 - 12), av = b0 + 12 + r.nextFloat() * (b1 - b0 - 20);
                        animal(f, au, av, r.nextFloat() * 360, kind, kind == 4 ? new int[]{0xFFE8E0D0, 0xFF6A4A30, 0xFFC8A070}[i % 3] : kind == 3 ? (i == 0 ? 1 : 0) : kind == 9 ? new int[]{0xFFF4F0E8, 0xFF8A7058, 0xFF4A4038, 0xFFC8A880}[i] : 0, kind == 9 ? 1.1f : 1.0f);
                    }
                    fence(f, 0xFF8A6A48, u0, b0, u1, b1, 6);
                    // A visitor leaning on the gate.
                    man(f, (u0 + u1) / 2, b1 + 3, KIT[(k + 2) % KIT.length]);
                }
                // The duck pond with a duck house and reeds.
                float q0 = 6 * T, q1 = Math.min(L * 0.62f, q0 + (S * 0.4f) * 2.2f), r0 = S * 0.6f + 14, r1 = S - 6;
                f.pond(q0, r0, q1, r1);
                for (int i = 0; i < 12; i++) {
                    float u = q0 + 2 + r.nextFloat() * 8, v = r0 + 2 + r.nextFloat() * (r1 - r0 - 4);
                    f.line(0xFF5A7A3A, 0.5f, u, v, u - 1, v - 3);
                }
                float du = (q0 + q1) / 2 + 6, dv = (r0 + r1) / 2;
                f.box(0xFF9A7A50, du - 3, dv - 3, du + 3, dv + 3, 1);
                f.poly(0xFFB8463A, du - 3.5f, dv - 3.5f, du + 3.5f, dv - 3.5f, du + 3.5f, dv);
                for (int i = 0; i < 6; i++)
                    animal(f, q0 + (q1 - q0) * (0.25f + r.nextFloat() * 0.55f), r0 + (r1 - r0) * (0.25f + r.nextFloat() * 0.5f), r.nextFloat() * 360, 5, i % 3 == 0 ? 0xFF8A6A48 : 0, 1.2f);
                // Round hay bales, a tractor with a hay-ride trailer, picnic tables and kids.
                for (int i = 0; i < 3; i++) {
                    float hu = 8 + i * 9, hv = 5 * T + 10;
                    f.circle(0x40000000, hu + 1.5f, hv + 1.5f, 4);
                    f.circle(0xFFD8B860, hu, hv, 4);
                    f.ring(0xFFB89A48, 0.5f, hu, hv, 2.5f);
                    f.circle(0xFFC8A850, hu, hv, 1);
                }
                float tu = 10, tv = S - 18;
                if (tv > 5 * T + 20) {
                    f.box(0xFF3A8A3A, tu, tv - 4, tu + 9, tv + 4, 1);
                    f.rect(0xFF2E6A2E, tu + 5, tv - 3, tu + 9, tv + 3);
                    f.rect(0xFF1E1E1E, tu + 1, tv - 5.5f, tu + 5, tv - 4);
                    f.rect(0xFF1E1E1E, tu + 1, tv + 4, tu + 5, tv + 5.5f);
                    f.box(0xFF9A7A50, tu + 11, tv - 5, tu + 28, tv + 5, 1);
                    for (int i = 0; i < 4; i++) f.rect(0xFFD8B860, tu + 12 + i * 4, tv - 4, tu + 15 + i * 4, tv + 4);
                    man(f, tu + 3, tv, 0xFF2E5EA8);
                    for (int i = 0; i < 3; i++) man(f, tu + 14 + i * 5, tv - 1 + (i % 2) * 2, KIT[i + 2]);
                }
                for (float u = q1 + 12; u < L - 10; u += 22) {
                    float v = (r0 + r1) / 2;
                    f.box(0xFF9A7A50, u - 5, v - 2, u + 5, v + 2, 1);
                    f.rect(0xFF7A5A38, u - 5, v - 4.5f, u + 5, v - 3.5f);
                    f.rect(0xFF7A5A38, u - 5, v + 3.5f, u + 5, v + 4.5f);
                    man(f, u - 2, v - 6, KIT[(int) u % KIT.length]);
                }
                f.tree(L - 6, S - 6, 6, 0xFF3E7A3A);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                // A red gambrel barn roof: steep lower slopes, shallow upper ones and a ridge vent.
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, q = S * 0.22f;
                f.rect(0xFF8A2E26, 0, 0, L, q);
                f.rect(0xFFB8463A, 0, q, L, S / 2);
                f.rect(0xFF9A3A30, 0, S / 2, L, S - q);
                f.rect(0xFF6E2420, 0, S - q, L, S);
                for (float u = 3; u < L; u += 3) f.line(0x18000000, 0.4f, u, 0, u, S);
                f.line(0xFFE8E0D0, 0.8f, 0, q, L, q);
                f.line(0xFFE8E0D0, 0.8f, 0, S - q, L, S - q);
                f.frame(0xFFF2EEE6, 1, 0.5f, 0.5f, L - 0.5f, S - 0.5f);
                f.box(0xFFF2EEE6, L / 2 - 4, S / 2 - 3, L / 2 + 4, S / 2 + 3, 1);
                gable(f, 0xFF8A2E26, L / 2 - 4, S / 2 - 3, L / 2 + 4, S / 2 + 3);
                f.line(0xFF2A2A2A, 0.5f, L / 2 - 3, S / 2, L / 2 + 3, S / 2);
                f.poly(0xFF2A2A2A, L / 2 + 2, S / 2 - 1, L / 2 + 4, S / 2, L / 2 + 2, S / 2 + 1);
                f.sign(city, "FARM", L / 2, S - q / 2, 4, 0xFFF2EEE6);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.SUB | Variants.PARK;
        d.minS = 10;
        d.minL = 14;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_SHELVES;
        d.roofCol = 0xFFB8463A;
        out.add(d);
    }

    // ------------------------------------------------------------------ 20. City Zoo

    /** An enclosure: its ground, a stone wall and a dry moat on the visitor side. */
    static void enclosure(F f, float u0, float v0, float u1, float v1, int gnd, Random r) {
        f.rect(0xFF8A8478, u0 - 2, v0 - 2, u1 + 2, v1 + 2);
        f.rect(gnd, u0, v0, u1, v1);
        for (int i = 0; i < 12; i++) f.circle(0x14000000, u0 + r.nextFloat() * (u1 - u0), v0 + r.nextFloat() * (v1 - v0), 2 + r.nextFloat() * 4);
        f.frame(0x30000000, 1.5f, u0 + 0.8f, v0 + 0.8f, u1 - 0.8f, v1 - 0.8f);
        f.frame(0xFFB0A898, 0.6f, u0 - 2, v0 - 2, u1 + 2, v1 + 2);
    }

    /** A flat-topped acacia. */
    static void acacia(F f, float u, float v, float rad) {
        f.oval(0x38000000, u - rad + rad * 0.4f, v - rad * 0.7f + rad * 0.5f, u + rad + rad * 0.4f, v + rad * 0.7f + rad * 0.5f);
        f.oval(0xFF5A7A3A, u - rad, v - rad * 0.7f, u + rad, v + rad * 0.7f);
        f.oval(0xFF6E8E48, u - rad * 0.8f, v - rad * 0.6f, u + rad * 0.5f, v + rad * 0.3f);
        f.oval(0xFF84A458, u - rad * 0.6f, v - rad * 0.5f, u - rad * 0.05f, v - rad * 0.1f);
    }

    static void zoo(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("City Zoo", T_PARK) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return new int[][]{at(bw >= bh, 1, 1, 3, 3, 2)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF5E9A48, 0, 0, L, S);
                float pu = L * 0.52f, pv = S * 0.44f, pw = 8;
                // The visitor paths: a cross and a loop.
                f.rect(0xFFE0D4B8, 0, pv - pw / 2, L, pv + pw / 2);
                f.rect(0xFFE0D4B8, pu - pw / 2, 0, pu + pw / 2, S);
                f.rrFrame(0xFFE0D4B8, pw * 0.8f, 3.5f, 3.5f, L - 3.5f, S - 3.5f, 10);
                // The elephant paddock: dusty ground, a bathing pool, logs and hay.
                float e0 = 10, e1 = pu - pw / 2 - 6, f0 = pv + pw / 2 + 6, f1 = S - 10;
                enclosure(f, e0, f0, e1, f1, 0xFFC8A878, r);
                float ew = e1 - e0, eh = f1 - f0;
                blob(f, 0xFF8A7A5A, e0 + ew * 0.72f, f0 + eh * 0.68f, ew * 0.2f + 2, eh * 0.2f + 2, jit(r, 7, 0.15f));
                blob(f, 0xFF5A8AA0, e0 + ew * 0.72f, f0 + eh * 0.68f, ew * 0.2f, eh * 0.2f, jit(r, 7, 0.15f));
                f.line(0x40000000, 2.4f, e0 + ew * 0.2f + 1, f0 + eh * 0.8f + 1.2f, e0 + ew * 0.45f + 1, f0 + eh * 0.85f + 1.2f);
                f.line(0xFF7A5A3A, 2.2f, e0 + ew * 0.2f, f0 + eh * 0.8f, e0 + ew * 0.45f, f0 + eh * 0.85f);
                f.box(0xFFD8B860, e0 + 4, f0 + 4, e0 + 12, f0 + 9, 0.8f);
                float es = Math.min(1.1f, Math.min(ew, eh) / 50);
                animal(f, e0 + ew * 0.35f, f0 + eh * 0.4f, 20, 6, 0, es);
                animal(f, e0 + ew * 0.6f, f0 + eh * 0.3f, 160, 6, 0, es * 0.9f);
                animal(f, e0 + ew * 0.5f, f0 + eh * 0.5f, 60, 6, 0, es * 0.55f);
                // The giraffe savanna: grass, acacias and a feeding platform.
                float g0 = pu + pw / 2 + 6, g1 = L - 10;
                enclosure(f, g0, f0, g1, f1, 0xFFA8AE60, r);
                float gw = g1 - g0;
                acacia(f, g0 + gw * 0.3f, f0 + eh * 0.3f, Math.min(10, gw * 0.15f));
                acacia(f, g0 + gw * 0.75f, f0 + eh * 0.72f, Math.min(9, gw * 0.13f));
                f.box(0xFF9A7A50, g0 - 6, f0 + eh * 0.45f, g0 + 3, f0 + eh * 0.45f + 10, 1);
                for (float v = f0 + eh * 0.45f + 1.5f; v < f0 + eh * 0.45f + 10; v += 2) f.line(0x30000000, 0.4f, g0 - 6, v, g0 + 3, v);
                man(f, g0 - 3, f0 + eh * 0.45f + 4, KIT[0]);
                float gs = Math.min(1f, gw / 50);
                animal(f, g0 + gw * 0.5f, f0 + eh * 0.42f, 200, 7, 0, gs);
                animal(f, g0 + gw * 0.62f, f0 + eh * 0.3f, 230, 7, 0, gs * 0.9f);
                animal(f, g0 + gw * 0.4f, f0 + eh * 0.75f, -30, 7, 0, gs * 0.6f);
                // The penguin pool: a blue lagoon with a white rocky shore.
                float k0 = g0, k1 = g1, h0 = 10, h1 = pv - pw / 2 - 6, kw = k1 - k0, kh = h1 - h0;
                enclosure(f, k0, h0, k1, h1, 0xFFD8DCDC, r);
                blob(f, 0xFF2E7AA8, k0 + kw * 0.58f, h0 + kh * 0.55f, kw * 0.34f, kh * 0.32f, jit(r, 8, 0.15f));
                blob(f, 0xFF4A9AC8, k0 + kw * 0.55f, h0 + kh * 0.5f, kw * 0.26f, kh * 0.22f, jit(r, 8, 0.15f));
                for (int i = 0; i < 4; i++) rock(f, k0 + 6 + r.nextFloat() * kw * 0.2f, h0 + 5 + r.nextFloat() * (kh - 10), 3, 0xFFE4E8EA, r);
                for (int i = 0; i < 9; i++) {
                    boolean swim = i % 3 == 0;
                    float u = swim ? k0 + kw * (0.45f + r.nextFloat() * 0.25f) : k0 + 4 + r.nextFloat() * kw * 0.25f;
                    float v = swim ? h0 + kh * (0.35f + r.nextFloat() * 0.3f) : h0 + 4 + r.nextFloat() * (kh - 8);
                    animal(f, u, v, swim ? r.nextFloat() * 360 : -90 + r.nextFloat() * 40, 8, 0, 1.3f);
                }
                f.rect(0x60A8D0E8, k0, h1 - 1, k1, h1 + 2);
                // Meerkats in a sandy pen beside the reptile house.
                float m0 = 4 * T + 6, m1 = pu - pw / 2 - 6;
                if (m1 - m0 > 16) {
                    enclosure(f, m0, 10, m1, h1, 0xFFDCC490, r);
                    for (int i = 0; i < 3; i++) {
                        float u = m0 + (m1 - m0) * (0.25f + i * 0.25f), v = 10 + (h1 - 10) * (0.35f + (i % 2) * 0.3f);
                        f.circle(0xFFC8AC78, u, v, 3);
                        f.circle(0xFF4A3A28, u, v, 1);
                    }
                    for (int i = 0; i < 6; i++)
                        animal(f, m0 + 4 + r.nextFloat() * (m1 - m0 - 8), 14 + r.nextFloat() * (h1 - 18), r.nextFloat() * 360, 9, 0xFFB8986A, 0.8f);
                }
                // Visitors, a kiosk umbrella and benches by the paths.
                for (int i = 0; i < 9; i++) {
                    boolean along = i % 2 == 0;
                    float u = along ? 8 + r.nextFloat() * (L - 16) : pu + (r.nextFloat() - 0.5f) * 4;
                    float v = along ? pv + (r.nextFloat() - 0.5f) * 4 : 8 + r.nextFloat() * (S - 16);
                    man(f, u, v, KIT[i % KIT.length]);
                }
                parasol(f, pu + pw / 2 + 4, pv - pw / 2 - 4, 4, 0xFFF0E8D8, 0xFF3A8A4A);
                f.bench(pu - 14, pv - pw / 2 - 2, true);
                f.bench(pu + 14, pv + pw / 2 + 2, true);
                f.tree(4, S - 4, 4, 0xFF3E7A3A);
                f.tree(L - 4, 4, 4, 0xFF3E7A3A);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                hip(f, 0xFF4E7A4A, 0, 0, L, S);
                f.box(0xC0B8D8E0, L / 2 - 6, S / 2 - 4, L / 2 + 6, S / 2 + 4, 0.8f);
                for (float u = L / 2 - 4; u < L / 2 + 6; u += 3) f.line(0xFF6A7A80, 0.4f, u, S / 2 - 4, u, S / 2 + 4);
                f.sign(city, "ZOO", L / 2, S - 6, 5, 0xFF4E7A4A);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.PARK | Variants.MID | Variants.SUB;
        d.minS = 12;
        d.minL = 14;
        d.base = City.SHOP;
        d.inside = Variants.I_LAB;
        d.roofCol = 0xFF4E7A4A;
        out.add(d);
    }

    // ------------------------------------------------------------------ 21. Bird Aviary

    /** A walk-through net dome with trees, perches and birds inside. */
    static void netDome(F f, Random r, float u, float v, float R, int birds) {
        f.circle(0x30000000, u + 2, v + 2.5f, R + 1);
        f.circle(0xFF8E8676, u, v, R + 1.2f);
        f.circle(0xFF6AA650, u, v, R);
        Path q = new Path();
        f.mv(q, u - R, v + R * 0.1f);
        f.qd(q, u, v - R * 0.5f, u + R, v + R * 0.2f);
        f.stroke(q, 0xFFD8CCB0, 3);
        f.pond(u + R * 0.15f, v + R * 0.25f, u + R * 0.55f, v + R * 0.55f);
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2 + 0.6f;
            f.tree(u + (float) Math.cos(a) * R * 0.55f, v + (float) Math.sin(a) * R * 0.55f, R * 0.2f, k % 2 == 0 ? 0xFF3E7A3A : 0xFF5A8A3A);
        }
        for (int k = 0; k < 3; k++) {
            double a = k * 2.1f;
            float pu = u + (float) Math.cos(a) * R * 0.3f, pv = v + (float) Math.sin(a) * R * 0.3f;
            f.line(0xFF6A4A2A, 0.8f, pu - 4, pv - 1, pu + 4, pv + 1);
            f.line(0xFF6A4A2A, 0.6f, pu, pv, pu + 1.5f, pv - 3);
        }
        int[] bc = {0xFFD83A2A, 0xFF2E7AD0, 0xFFF0C020, 0xFF30A050, 0xFFF08A20, 0xFFE8E8E4, 0xFF8A4AC8};
        for (int k = 0; k < birds; k++) {
            double a = r.nextFloat() * Math.PI * 2;
            float dd = R * 0.85f * (float) Math.sqrt(r.nextFloat());
            animal(f, u + (float) Math.cos(a) * dd, v + (float) Math.sin(a) * dd, r.nextFloat() * 360, 11, bc[k % bc.length], 1.1f);
        }
        man(f, u - R * 0.4f, v - R * 0.05f, 0xFF8A6A9A);
        // The netting: a veil, rings, spokes and masts.
        f.circle(0x24FFFFFF, u, v, R);
        for (int k = 1; k <= 4; k++) f.ring(0x70E8ECEE, 0.4f, u, v, R * (float) Math.sin(k * Math.PI / 8));
        for (int k = 0; k < 20; k++) {
            double a = k * Math.PI / 10;
            f.line(0x70E8ECEE, 0.4f, u, v, u + (float) Math.cos(a) * R, v + (float) Math.sin(a) * R);
        }
        for (int k = 0; k < 10; k++) {
            double a = k * Math.PI / 5;
            f.circle(0xFF4A4E52, u + (float) Math.cos(a) * R, v + (float) Math.sin(a) * R, 1);
        }
        f.circle(0x40000000, u + 1.5f, v + 2, 1.6f);
        f.circle(0xFF5A5E62, u, v, 1.5f);
        f.oval(0x20FFFFFF, u - R * 0.7f, v - R * 0.7f, u, v - R * 0.2f);
    }

    static void aviary(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Bird Aviary", T_PARK) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return new int[][]{at(bw >= bh, 1, 1, 2, 2, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF62A04C, 0, 0, L, S);
                for (int i = 0; i < 30; i++) f.circle(0x10000000, r.nextFloat() * L, r.nextFloat() * S, 2 + r.nextFloat() * 4);
                float R1 = Math.min(L * 0.3f, S * 0.42f), u1 = Math.max(R1 + 6, L * 0.34f), v1 = S * 0.52f;
                float R2 = Math.min(L * 0.2f, S * 0.27f), u2 = L - R2 - 8, v2 = R2 + 8;
                float lu = L - Math.min(L * 0.2f, S * 0.27f) - 8, lv = S - Math.min(S * 0.24f, 26) - 6;
                // The path linking the domes and the lagoon.
                f.stroke(smooth(f, new float[]{0, S - 8, u1 * 0.6f, S - 6, u1 + R1 * 0.8f, v1 + R1 * 0.4f, (u1 + u2) / 2, S * 0.5f, u2 - R2, v2 + R2 + 4, u2, v2 + R2 + 6, L, v2 + R2 + 4}), 0xFFD8CCB0, 6);
                f.stroke(smooth(f, new float[]{(u1 + u2) / 2, S * 0.5f, lu - 10, lv, lu, lv}), 0xFFD8CCB0, 5);
                // The flamingo lagoon.
                float lr = Math.min(L * 0.17f, S * 0.22f);
                blob(f, 0xFF8A7A5A, lu, lv, lr * 1.3f + 1.5f, lr * 0.8f + 1.5f, jit(r, 8, 0.12f));
                blob(f, 0xFF5AA0B8, lu, lv, lr * 1.3f, lr * 0.8f, jit(r, 8, 0.12f));
                blob(f, 0xFF7AB8C8, lu - lr * 0.2f, lv - lr * 0.1f, lr * 0.8f, lr * 0.45f, jit(r, 8, 0.12f));
                for (int k = 0; k < 9; k++)
                    animal(f, lu + (r.nextFloat() - 0.5f) * lr * 1.8f, lv + (r.nextFloat() - 0.5f) * lr * 0.9f, -60 + r.nextFloat() * 40, 10, 0, 1.4f);
                for (int k = 0; k < 8; k++) {
                    double a = k * Math.PI / 4;
                    f.line(0xFF5A7A3A, 0.5f, lu + (float) Math.cos(a) * lr * 1.3f, lv + (float) Math.sin(a) * lr * 0.8f, lu + (float) Math.cos(a) * lr * 1.4f, lv + (float) Math.sin(a) * lr * 0.8f - 3);
                }
                netDome(f, r, u1, v1, R1, 14);
                netDome(f, r, u2, v2, R2, 8);
                // Trees and benches between.
                f.tree(L * 0.62f, S * 0.85f, 6, 0xFF3E7A3A);
                f.tree(4 * T, 4, 4, 0xFF4A8640);
                f.bench((u1 + u2) / 2, S * 0.5f - 5, true);
                for (int i = 0; i < 5; i++) man(f, (u1 + u2) / 2 + (r.nextFloat() - 0.5f) * 30, S * 0.5f + (r.nextFloat() - 0.5f) * 4, KIT[i]);
                // Wild birds on the lawn.
                for (int k = 0; k < 4; k++) animal(f, r.nextFloat() * L, r.nextFloat() * S, r.nextFloat() * 360, 11, 0xFF6A6A70, 0.8f);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                hip(f, 0xFF6A7A5A, 0, 0, L, S);
                f.circle(0xFFF0C020, L / 2, S / 2, 1.6f);
                f.sign(city, "AVIARY", L / 2, S / 2, 3, 0xFF3A4A2A);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.PARK | Variants.MID | Variants.OLD | Variants.SUB;
        d.minS = 10;
        d.minL = 12;
        d.base = City.KIOSK;
        d.inside = Variants.I_CAFE;
        d.roofCol = 0xFF6A7A5A;
        out.add(d);
    }

    // ------------------------------------------------------------------ 22. Wildflower Meadow

    static void meadow(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Wildflower Meadow", T_PARK) {
            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF8CA85A, 0, 0, L, S);
                for (int i = 0; i < 26; i++) blob(f, r.nextBoolean() ? 0xFF98B262 : 0xFF7E9C50, r.nextFloat() * L, r.nextFloat() * S, 6 + r.nextFloat() * 10, 5 + r.nextFloat() * 8, jit(r, 6, 0.3f));
                for (int i = 0; i < 110; i++) {
                    float u = r.nextFloat() * L, v = r.nextFloat() * S;
                    f.line(r.nextBoolean() ? 0xFFA8BE6E : 0xFF6E8A44, 0.5f, u, v, u + (r.nextFloat() - 0.5f) * 2, v - 2.5f);
                }
                // Drifts of flowers: poppies, cornflowers, ox-eye daisies, buttercups, knapweed.
                int[] fc = {0xFFD8302A, 0xFF4A70D0, 0xFFF6F4EC, 0xFFF0D030, 0xFFA060B8};
                int nd = Math.max(12, Math.min(26, (int) (L * S / 2000)));
                for (int k = 0; k < nd; k++) {
                    float u = r.nextFloat() * L, v = r.nextFloat() * S;
                    flowers(f, r, u, v, 8 + r.nextFloat() * 8, 14, fc[k % fc.length]);
                }
                for (int i = 0; i < 50; i++) f.circle(i % 2 == 0 ? 0xFFC8B870 : fc[r.nextInt(fc.length)], r.nextFloat() * L, r.nextFloat() * S, 0.8f);
                // Mown paths: a loop and a cross path.
                float[] loop = {L * 0.08f, S * 0.5f, L * 0.1f, S * 0.12f, L * 0.5f, S * 0.18f, L * 0.9f, S * 0.1f, L * 0.92f, S * 0.5f,
                        L * 0.88f, S * 0.88f, L * 0.5f, S * 0.8f, L * 0.12f, S * 0.9f, L * 0.08f, S * 0.5f};
                float[] cross = {0, S * 0.62f, L * 0.3f, S * 0.55f, L * 0.55f, S * 0.45f, L, S * 0.38f};
                for (int k = 0; k < 2; k++) {
                    float[] q = k == 0 ? loop : cross;
                    f.stroke(smooth(f, shift(q, 0.8f)), 0x30000000, 10);
                    f.stroke(smooth(f, q), 0xFF6E9E48, 10);
                    f.stroke(smooth(f, q), 0xFF80B45A, 8);
                    f.stroke(smooth(f, q), 0x2CFFFFFF, 3);
                }
                // The bee yard: a mown square, five hives and a beekeeper.
                float hu = L * 0.78f, hv = S * 0.72f, hw = Math.min(22, L * 0.12f);
                f.rect(0xFF80B45A, hu - hw, hv - hw * 0.8f, hu + hw, hv + hw * 0.8f);
                fence(f, 0xFF8A6A48, hu - hw, hv - hw * 0.8f, hu + hw, hv + hw * 0.8f, 7);
                int[] hc = {0xFFF4F0E4, 0xFFE8D8A0, 0xFFB8D0E0, 0xFFF4F0E4, 0xFFD8C8E0};
                for (int k = 0; k < 5; k++) {
                    float u = hu - hw + 6 + (k % 3) * (2 * hw - 12) / 2f, v = hv - hw * 0.4f + (k / 3) * hw * 0.8f + (k / 3) * 0;
                    f.box(hc[k], u - 3, v - 3, u + 3, v + 3, 1.4f);
                    f.frame(City.darken(hc[k], 0.8f), 0.6f, u - 3, v - 3, u + 3, v + 3);
                    f.rect(City.darken(hc[k], 0.9f), u - 3, v - 0.4f, u + 3, v + 0.4f);
                    for (int b = 0; b < 3; b++) f.circle(0xFFE8B020, u + (r.nextFloat() - 0.5f) * 10, v + (r.nextFloat() - 0.5f) * 10, 0.4f);
                }
                man(f, hu + hw - 5, hv + hw * 0.5f, 0xFFF4F4F0);
                f.circle(0xFF3A3A3A, hu + hw - 5.2f, hv + hw * 0.5f - 0.2f, 1.25f);
                // Old oaks, an insect hotel and a bench where the paths meet.
                f.tree(L * 0.3f, S * 0.3f, Math.min(12, S * 0.1f), 0xFF4A7A36);
                f.tree(L * 0.62f, S * 0.88f, Math.min(10, S * 0.08f), 0xFF557E3A);
                f.tree(L * 0.95f, S * 0.05f, 7, 0xFF4A7A36);
                float iu = L * 0.45f, iv = S * 0.3f;
                f.box(0xFF8A6A48, iu - 4, iv - 2.5f, iu + 4, iv + 2.5f, 1);
                for (int k = 0; k < 6; k++) f.circle(0xFF3A2A1A, iu - 3 + k * 1.2f, iv + ((k % 2) - 0.5f) * 2, 0.5f);
                f.rect(0xFF6A5038, iu - 4.5f, iv - 3, iu + 4.5f, iv - 2);
                f.bench(L * 0.3f, S * 0.55f - 7, true);
                f.box(0xFF6A5038, L * 0.1f - 3, S * 0.5f - 9, L * 0.1f + 3, S * 0.5f - 7, 0.6f);
                man(f, L * 0.5f, S * 0.18f, 0xFFC86A4A);
                man(f, L * 0.52f, S * 0.2f + 3, 0xFF4A6A9A);
                for (int k = 0; k < 6; k++) butterflyBug(f, r.nextFloat() * L, r.nextFloat() * S, WINGS[k % WINGS.length], 0.8f);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.PARK | Variants.SUB | Variants.UNI | Variants.OLD;
        d.minS = 9;
        d.minL = 10;
        out.add(d);
    }

    // ------------------------------------------------------------------ 23. Waterfall Garden

    static void fern(F f, float u, float v, float rad, int col) {
        f.circle(0x30000000, u + rad * 0.3f, v + rad * 0.3f, rad * 0.7f);
        for (int k = 0; k < 7; k++) {
            double a = k * Math.PI * 2 / 7;
            f.curve(k % 2 == 0 ? col : City.lighten(col, 0.15f), 1.2f, u, v, u + (float) Math.cos(a + 0.3f) * rad * 0.6f, v + (float) Math.sin(a + 0.3f) * rad * 0.6f,
                    u + (float) Math.cos(a) * rad, v + (float) Math.sin(a) * rad);
        }
    }

    static void waterfall(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Waterfall Garden", T_PARK) {
            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, cv = S / 2;
                f.rect(0xFF4E8A40, 0, 0, L, S);
                for (int i = 0; i < 30; i++) blob(f, r.nextBoolean() ? 0xFF5A9A48 : 0xFF447E38, r.nextFloat() * L, r.nextFloat() * S, 5 + r.nextFloat() * 8, 4 + r.nextFloat() * 6, jit(r, 6, 0.3f));
                // The rock cliff at the head, the falls tumbling off it.
                float cl = L * 0.22f;
                for (int i = 0; i < 26; i++) {
                    float u = 2 + r.nextFloat() * cl, v = 2 + r.nextFloat() * (S - 4);
                    rock(f, u, v, 5 + r.nextFloat() * 6, r.nextBoolean() ? 0xFF8E8A84 : 0xFF7A766E, r);
                }
                // Pools stepping down the garden, joined by a stream with little falls.
                float[] pu = {cl + 10, L * 0.52f, L * 0.82f}, pv = {cv - S * 0.08f, cv + S * 0.12f, cv - S * 0.04f};
                float[] pr = {Math.min(S * 0.18f, 18), Math.min(S * 0.16f, 16), Math.min(S * 0.26f, 26)};
                float[] stream = {0, cv - 4, cl * 0.4f, cv + 3, cl * 0.8f, cv - 6, pu[0], pv[0], (pu[0] + pu[1]) / 2, pv[0] + 18, pu[1], pv[1], (pu[1] + pu[2]) / 2, pv[1] - 14, pu[2], pv[2]};
                f.stroke(smooth(f, stream), 0xFF6E6A62, 10);
                f.stroke(smooth(f, stream), 0xFF3E7EA8, 7);
                f.stroke(smooth(f, stream), 0xFF5A9AC4, 3);
                for (int k = 0; k < 3; k++) {
                    float[] k0 = jit(r, 8, 0.15f);
                    blob(f, 0xFF7A766E, pu[k], pv[k], pr[k] * 1.25f + 2, pr[k] + 2, k0);
                    blob(f, 0xFF2E6E98, pu[k], pv[k], pr[k] * 1.25f, pr[k], k0);
                    blob(f, 0xFF3E80AA, pu[k] - pr[k] * 0.1f, pv[k] - pr[k] * 0.1f, pr[k] * 0.9f, pr[k] * 0.7f, k0);
                    for (int i = 0; i < 5; i++) {
                        double a = r.nextFloat() * Math.PI * 2;
                        rock(f, pu[k] + (float) Math.cos(a) * pr[k] * 1.3f, pv[k] + (float) Math.sin(a) * pr[k] * 1.05f, 2.5f + r.nextFloat() * 2, 0xFF8A867E, r);
                    }
                    for (int i = 0; i < 3; i++) {
                        f.circle(0xFF5A9A48, pu[k] + (r.nextFloat() - 0.2f) * pr[k], pv[k] + (r.nextFloat() - 0.5f) * pr[k], 1.8f);
                    }
                }
                // White water: the falls off the cliff and the foam below each drop.
                f.stroke(smooth(f, new float[]{0, cv - 4, cl * 0.4f, cv + 3, cl * 0.8f, cv - 6, pu[0] - pr[0], pv[0]}), 0x90FFFFFF, 6);
                for (int k = 0; k < 6; k++) {
                    float v = cv - 6 + k * 2.4f;
                    f.curve(0xC0FFFFFF, 1.1f, cl * 0.55f, v, cl * 0.8f, v + 1, pu[0] - pr[0] * 0.9f, pv[0] + (k - 2.5f) * 2);
                }
                for (int k = 0; k < 3; k++) {
                    float fu = k == 0 ? pu[0] - pr[0] * 0.8f : (pu[k - 1] + pu[k]) / 2, fv = k == 0 ? pv[0] : (k == 1 ? pv[0] + 16 : pv[1] - 13);
                    for (int i = 0; i < 6; i++) f.circle(0x70FFFFFF, fu + (r.nextFloat() - 0.3f) * 6, fv + (r.nextFloat() - 0.5f) * 6, 1 + r.nextFloat() * 2);
                    if (k > 0) f.line(0xD0FFFFFF, 1, fu, fv - 3, fu, fv + 3);
                }
                // A winding path with wooden bridges over the stream.
                float[] path = {cl + 2, 4, pu[0] + pr[0] * 1.6f, pv[0] - pr[0] * 1.6f, pu[0] + 22, pv[0] + 30, pu[1] - 4, pv[1] + pr[1] + 16, pu[1] + 26, pv[1] - 4, pu[2] - 8, pv[2] - pr[2] - 12, L, 6};
                if (S > 150) path[13] = 8;
                f.stroke(smooth(f, path), 0xFFB8A888, 6);
                f.stroke(smooth(f, path), 0xFFCCBC9A, 4);
                float[][] br = {{(pu[0] + pu[1]) / 2 - 4, pv[0] + 17}, {(pu[1] + pu[2]) / 2 + 4, pv[1] - 13}};
                for (float[] b : br) {
                    f.rect(0x40000000, b[0] - 4 + 1.2f, b[1] - 8 + 1.5f, b[0] + 4 + 1.2f, b[1] + 8 + 1.5f);
                    f.rect(0xFF9A7A50, b[0] - 4, b[1] - 8, b[0] + 4, b[1] + 8);
                    for (float v = b[1] - 7; v < b[1] + 8; v += 2) f.line(0x40000000, 0.4f, b[0] - 4, v, b[0] + 4, v);
                    f.line(0xFF6A4A2A, 0.9f, b[0] - 4, b[1] - 8, b[0] - 4, b[1] + 8);
                    f.line(0xFF6A4A2A, 0.9f, b[0] + 4, b[1] - 8, b[0] + 4, b[1] + 8);
                }
                // Ferns, maples and pines round the water.
                for (int i = 0; i < 16; i++) {
                    float u = cl + r.nextFloat() * (L - cl - 4), v = 4 + r.nextFloat() * (S - 8);
                    fern(f, u, v, 3.5f + r.nextFloat() * 2, 0xFF3E8A3A);
                }
                f.tree(cl + 8, S - 10, 7, 0xFFB84A2A);
                f.tree(L * 0.66f, 9, 6, 0xFFC86A2A);
                f.tree(L - 8, S - 9, 7, 0xFF2E5A3A);
                f.tree(L * 0.4f, S - 7, 6, 0xFF2E5A3A);
                f.bench(pu[2] - pr[2] * 0.4f, pv[2] + pr[2] + 6, true);
                man(f, br[0][0] + 1, br[0][1] - 2, 0xFFC86A4A);
                man(f, pu[2] + 3, pv[2] + pr[2] + 6, 0xFF4A6A9A);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.PARK | Variants.OLD | Variants.MID | Variants.SUB;
        d.minS = 9;
        d.minL = 12;
        out.add(d);
    }

    // ------------------------------------------------------------------ 24. Community Orchard

    static void orchard(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Community Orchard", T_PARK) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return new int[][]{at(bw >= bh, 1, 1, 3, 2, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF7CA852, 0, 0, L, S);
                for (int i = 0; i < 50; i++) {
                    float u = r.nextFloat() * L, v = r.nextFloat() * S;
                    f.line(r.nextBoolean() ? 0xFF94BC64 : 0xFF6A9444, 0.5f, u, v, u + (r.nextFloat() - 0.5f) * 2, v - 2.5f);
                }
                // The working yard by the shed: the cider press, barrels and crates.
                f.rect(0xFFB8A684, 0, 0, 5 * T, 4 * T);
                float pu = 2.5f * T, pv = 3.3f * T;
                f.circle(0x40000000, pu + 1.2f, pv + 1.5f, 4);
                f.circle(0xFF8A6A48, pu, pv, 4);
                f.circle(0xFF6A4A2A, pu, pv, 2.8f);
                f.circle(0xFFC8A848, pu, pv, 2.2f);
                f.line(0xFF3A3A3A, 1, pu - 5, pv, pu + 5, pv);
                f.circle(0xFF3A3A3A, pu, pv, 0.9f);
                for (int k = 0; k < 3; k++) {
                    float bu = 4 * T + 2 + k * 5, bv = 3.2f * T;
                    f.circle(0x40000000, bu + 1, bv + 1.2f, 2.3f);
                    f.circle(0xFF8A5A30, bu, bv, 2.3f);
                    f.ring(0xFF4A4A4A, 0.4f, bu, bv, 1.6f);
                }
                // Mown strips between the rows of trees.
                float a0 = 6, gap = 27;
                int rows = Math.max(2, (int) ((S - 12) / gap) + 1);
                float rs = (S - 16) / (rows - 1);
                for (int j = 0; j < rows; j++) {
                    float v = 8 + j * rs;
                    if (j < rows - 1) f.rect(0xFF8CBA60, 0, v + rs / 2 - 3, L, v + rs / 2 + 3);
                }
                int[][] fruit = {{0xFF4A7E36, 0xFFD8302A}, {0xFF56863A, 0xFFE0C040}, {0xFF4A7A3A, 0xFF7A3A7A}, {0xFF52843A, 0xFF8AC040}};
                int cols = Math.max(3, (int) ((L - 12) / 27) + 1);
                float cs = (L - 16) / (cols - 1);
                int lad = 0;
                for (int j = 0; j < rows; j++)
                    for (int i = 0; i < cols; i++) {
                        float u = 8 + i * cs + (j % 2) * cs * 0.15f, v = 8 + j * rs;
                        if (u < 5 * T + 6 && v < 4 * T + 6) continue;
                        int kind = (i + j * 2) % 4;
                        float tr = Math.min(8, Math.min(cs, rs) * 0.36f);
                        for (int k = 0; k < 1; k++) f.circle(fruit[kind][1], u + (r.nextFloat() - 0.5f) * tr * 2.4f, v + tr + r.nextFloat() * 3, 0.6f);
                        f.tree(u, v, tr, fruit[kind][0]);
                        for (int k = 0; k < 4; k++) {
                            double a = r.nextFloat() * Math.PI * 2;
                            float dd = tr * 0.75f * (float) Math.sqrt(r.nextFloat());
                            f.circle(fruit[kind][1], u + (float) Math.cos(a) * dd, v + (float) Math.sin(a) * dd, 1.15f);
                        }
                        // Picking ladders and crates at a few trees.
                        if ((i * 7 + j * 3) % 5 == 1 && lad < 3) {
                            lad++;
                            float lu = u + tr * 0.4f, lv = v + tr * 0.3f, eu = lu + 9, ev = lv + 4;
                            f.line(0x40000000, 2.4f, lu + 1, lv + 1.5f, eu + 1, ev + 1.5f);
                            f.line(0xFFB8945A, 0.6f, lu, lv - 1, eu, ev - 1);
                            f.line(0xFFB8945A, 0.6f, lu, lv + 1, eu, ev + 1);
                            for (int k = 1; k < 6; k++) {
                                float t = k / 6f;
                                f.line(0xFFB8945A, 0.5f, lu + (eu - lu) * t, lv - 1 + (ev - lv) * t, lu + (eu - lu) * t, lv + 1 + (ev - lv) * t);
                            }
                            f.line(0xFF9A7A48, 0.6f, eu, ev, eu - 2, ev + 6);
                            man(f, lu + 3, lv + 1.5f, KIT[lad + 1]);
                            f.box(0xFF9A7A50, eu + 2, ev - 2, eu + 7, ev + 2, 0.8f);
                            for (int k = 0; k < 4; k++) f.circle(fruit[kind][1], eu + 3 + (k % 2) * 2.5f, ev - 0.8f + (k / 2) * 1.8f, 0.9f);
                            man(f, eu + 4, ev + 5, KIT[lad + 3]);
                        }
                    }
                // A wheelbarrow and a bench.
                f.box(0xFF3A7A4A, L - 16, S - 8, L - 9, S - 4, 0.8f);
                f.circle(0xFFD8302A, L - 13, S - 6, 1);
                f.circle(0xFFD8302A, L - 11, S - 6.5f, 1);
                f.circle(0xFF2A2A2A, L - 7.5f, S - 6, 1);
                f.bench(5 * T + 4, 2 * T, false);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                gable(f, 0xFF7A6448, 0, 0, L, S);
                for (float u = 2; u < L; u += 2) f.line(0x22000000, 0.4f, u, 0, u, S);
                for (int k = 0; k < 4; k++) blob(f, 0x806A9A3A, 4 + r.nextFloat() * (L - 8), 3 + r.nextFloat() * (S - 6), 2.5f, 1.8f, jit(r, 6, 0.3f));
                f.circle(0x40000000, L - 7, S / 2 - 3, 1.6f);
                f.circle(0xFF3A3A3A, L - 8, S / 2 - 4, 1.5f);
                f.circle(0xFF1A1A1A, L - 8, S / 2 - 4, 0.8f);
                f.sign(city, "CIDER", L / 2, S / 2, 4, 0xFFD8302A);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.SUB | Variants.PARK | Variants.OLD;
        d.minS = 9;
        d.minL = 12;
        d.base = City.KIOSK;
        d.inside = Variants.I_SHELVES;
        d.roofCol = 0xFF7A6448;
        out.add(d);
    }

    // ------------------------------------------------------------------ 25. Rock Garden

    static void cushion(F f, Random r, float u, float v, float rad, int col) {
        blob(f, 0x30000000, u + 0.6f, v + 0.8f, rad, rad * 0.85f, jit(r, 6, 0.2f));
        blob(f, 0xFF5A8A48, u, v, rad, rad * 0.85f, jit(r, 6, 0.2f));
        int n = (int) (rad * 1.6f);
        for (int k = 0; k < n; k++) {
            double a = r.nextFloat() * Math.PI * 2;
            float dd = rad * 0.75f * (float) Math.sqrt(r.nextFloat());
            f.circle(col, u + (float) Math.cos(a) * dd, v + (float) Math.sin(a) * dd, 0.8f);
        }
    }

    static void rockGarden(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Rock Garden", T_PARK) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return new int[][]{at(bw >= bh, 1, 1, 3, 2, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFFC4BFB4, 0, 0, L, S);
                for (int i = 0; i < 70; i++) {
                    int g = r.nextInt(3);
                    f.circle(g == 0 ? 0xFFA8A398 : g == 1 ? 0xFFD8D4CA : 0xFFB4AEA2, r.nextFloat() * L, r.nextFloat() * S, 0.5f + r.nextFloat() * 0.6f);
                }
                // A low dry-stone wall round the garden, a lawn strip by the alpine house.
                f.frame(0xFF8E887C, 3, 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                for (float u = 3; u < L; u += 5) f.line(0xFF6E685E, 0.4f, u, 0, u, 3);
                f.rect(0xFF6AA050, 3, 3, 5 * T, 3.6f * T);
                // A flagstone path winding through.
                float[] path = {5 * T, 2 * T, L * 0.4f, S * 0.3f, L * 0.45f, S * 0.7f, L * 0.75f, S * 0.75f, L * 0.8f, S * 0.3f, L - 3, S * 0.2f};
                Path pp = smooth(f, path);
                f.stroke(pp, 0xFFA8A296, 7);
                int np = path.length / 2;
                for (int i = 1; i < np - 1; i++) {
                    float au = i == 1 ? path[0] : (path[2 * i - 2] + path[2 * i]) / 2, av = i == 1 ? path[1] : (path[2 * i - 1] + path[2 * i + 1]) / 2;
                    float bu2 = i == np - 2 ? path[2 * np - 2] : (path[2 * i] + path[2 * i + 2]) / 2, bv2 = i == np - 2 ? path[2 * np - 1] : (path[2 * i + 1] + path[2 * i + 3]) / 2;
                    for (int k = 0; k < 6; k++) {
                        float t = (k + 0.5f) / 6, w0 = (1 - t) * (1 - t), w1 = 2 * t * (1 - t), w2 = t * t;
                        blob(f, 0xFFD8D0BE, w0 * au + w1 * path[2 * i] + w2 * bu2, w0 * av + w1 * path[2 * i + 1] + w2 * bv2, 2.6f, 2.2f, jit(r, 5, 0.25f));
                    }
                }
                // The crevice garden: slates on edge in parallel rows with plants between.
                float cu0 = L * 0.08f, cu1 = L * 0.34f, cv0 = S * 0.55f, cv1 = S - 8;
                for (float u = cu0; u < cu1; u += 3.2f) {
                    float jv = r.nextFloat() * 4;
                    f.line(0x40000000, 1.2f, u + 0.8f, cv0 + jv + 1, u + 2.8f, cv1 - jv + 1);
                    f.line(0xFF5E626A, 1.1f, u, cv0 + jv, u + 2, cv1 - jv);
                    f.line(0xFF8A8E96, 0.4f, u - 0.4f, cv0 + jv, u + 1.6f, cv1 - jv);
                    if (r.nextFloat() < 0.6f) {
                        float pv = cv0 + 4 + r.nextFloat() * (cv1 - cv0 - 8);
                        f.circle(0xFF5A8A48, u + 1.6f + (pv - cv0) / (cv1 - cv0) * 2, pv, 1.2f);
                        f.circle(new int[]{0xFFE070B0, 0xFFF4F4F0, 0xFF9A6AD0}[r.nextInt(3)], u + 1.6f + (pv - cv0) / (cv1 - cv0) * 2, pv, 0.6f);
                    }
                }
                // Boulders with alpine cushions tucked round them.
                int[] fc = {0xFFE070B0, 0xFF9A6AD0, 0xFFF4F4F0, 0xFFF0D040, 0xFF5A80E0, 0xFFE85A4A};
                float[][] rk = {{0.25f, 0.35f}, {0.55f, 0.18f}, {0.62f, 0.5f}, {0.9f, 0.55f}, {0.45f, 0.88f}, {0.85f, 0.88f}, {0.7f, 0.25f}, {0.36f, 0.62f}};
                for (int k = 0; k < rk.length; k++) {
                    float u = L * rk[k][0], v = S * rk[k][1], rad = Math.min(L, S) * (0.05f + 0.03f * (k % 3));
                    blob(f, 0xFFA8A296, u + rad * 0.9f, v + rad * 0.8f, rad * 1.6f, rad * 1.1f, jit(r, 6, 0.25f));
                    for (int i = 0; i < 6; i++) f.circle(0xFF8E887C, u + rad * (0.2f + r.nextFloat() * 1.6f), v + rad * (0.2f + r.nextFloat() * 1.4f), 0.8f + r.nextFloat() * 0.6f);
                    rock(f, u, v, rad, k % 2 == 0 ? 0xFFB4A898 : 0xFF9A9690, r);
                    if (k % 3 == 0) rock(f, u + rad * 1.3f, v + rad * 0.6f, rad * 0.5f, 0xFFA8A090, r);
                    for (int i = 0; i < 3; i++) {
                        double a = Math.PI * (0.2f + i * 0.5f) + r.nextFloat() * 0.4f;
                        cushion(f, r, u + (float) Math.cos(a) * rad * 1.45f, v + (float) Math.sin(a) * rad * 1.35f, 3f + r.nextFloat() * 2f, fc[(k + i) % fc.length]);
                    }
                }
                // Stone troughs of alpines and a few dwarf conifers.
                for (int k = 0; k < 3; k++) {
                    float u = 5 * T + 10 + k * 16, v = 2.4f * T;
                    if (u + 6 > L - 4) break;
                    f.box(0xFFA8A296, u - 6, v - 3.5f, u + 6, v + 3.5f, 1.2f);
                    f.rect(0xFF6A5A48, u - 4.8f, v - 2.3f, u + 4.8f, v + 2.3f);
                    cushion(f, r, u - 2, v, 2, fc[k]);
                    cushion(f, r, u + 2.5f, v + 0.5f, 1.6f, fc[k + 3]);
                }
                // A little alpine tarn.
                float tu = L * 0.78f, tv = S * 0.4f, tr = Math.min(L, S) * 0.07f;
                float[] tk = jit(r, 7, 0.2f);
                blob(f, 0xFF8E887C, tu, tv, tr * 1.4f + 1.5f, tr + 1.5f, tk);
                blob(f, 0xFF4A86A8, tu, tv, tr * 1.4f, tr, tk);
                blob(f, 0xFF6AA0C0, tu - tr * 0.2f, tv - tr * 0.2f, tr * 0.8f, tr * 0.5f, tk);
                f.tree(L * 0.95f, S * 0.08f, 4, 0xFF2E5A4A);
                f.tree(L * 0.15f, S * 0.18f + 3 * T, 3.5f, 0xFF2E5A4A);
                f.tree(L * 0.52f, S * 0.52f, 3, 0xFF3A6A5A);
                f.tree(L * 0.95f, S * 0.92f, 4.5f, 0xFF2E5A4A);
                f.bench(L * 0.6f, S * 0.75f + 7, true);
                man(f, L * 0.42f, S * 0.4f, 0xFF6A8AA8);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                // The alpine house: a glazed span roof over benches of pots, its vents open.
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, m = S / 2;
                f.rect(0xFF7A7468, 0, 0, L, S);
                f.rect(0xFFA89A80, 1, 1, L - 1, m - 2);
                f.rect(0xFFA89A80, 1, m + 2, L - 1, S - 1);
                int[] fc = {0xFFE070B0, 0xFF9A6AD0, 0xFFF4F4F0, 0xFFF0D040};
                for (float u = 3; u < L - 2; u += 3.2f)
                    for (int k = 0; k < 2; k++) {
                        float v = k == 0 ? m - 6 : m + 6;
                        f.circle(0xFFB8643A, u, v, 1.3f);
                        f.circle(0xFF5A8A48, u, v, 0.9f);
                        f.circle(fc[(int) (u + k) % 4], u, v, 0.4f);
                    }
                f.rect(0x48D8ECF0, 0, 0, L, m);
                f.rect(0x34B8D0D8, 0, m, L, S);
                for (float u = 0; u <= L; u += 4) f.line(0xFFE8ECEE, 0.5f, u, 0, u, S);
                f.line(0xFFF4F6F8, 1.2f, 0, m, L, m);
                f.frame(0xFFE8ECEE, 1, 0.5f, 0.5f, L - 0.5f, S - 0.5f);
                f.rect(0x60D8ECF0, L * 0.3f, 1, L * 0.3f + 4, m - 1);
                f.rect(0x40000000, L * 0.3f + 4, 1, L * 0.3f + 5, m - 1);
                f.sign(city, "ALPINES", L / 2, S - 3, 3, 0xFF3A6A4A);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.PARK | Variants.OLD | Variants.SUB | Variants.UNI;
        d.minS = 9;
        d.minL = 12;
        d.base = City.KIOSK;
        d.inside = Variants.I_SHELVES;
        d.roofCol = 0xFFD8ECF0;
        out.add(d);
    }
}
