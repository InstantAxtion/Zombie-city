package com.instantaxtion.zombiesandbox;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.Landmarks.*;
/** ((22.8) Landmark designs, part 11 (see Landmarks): everyday services and community buildings. */
final class LandmarkArt11 {
    private LandmarkArt11() {
    }

    static final float TT = City.T;

    static void register(List<Landmarks.Def> out) {
        shelter(out);
        vet(out);
        branchLib(out);
        community(out);
        policeMuseum(out);
        radio(out);
        newspaper(out);
        exchange(out);
        busGarage(out);
        ambulance(out);
        coastGuard(out);
        bloodBank(out);
        studio(out);
        drivingSchool(out);
        storage(out);
        laundromat(out);
        carWash(out);
        gasStation(out);
        sorting(out);
        mortuary(out);
        pawn(out);
        iceFactory(out);
        fireTraining(out);
        recruiting(out);
        emergency(out);
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

    /** A plain flat roof filling f: a darker parapet and a deck. */
    static void deck(F f, int col) {
        f.rect(City.darken(col, 0.68f), 0, 0, f.L, f.S);
        f.rect(col, 1.4f, 1.4f, f.L - 1.4f, f.S - 1.4f);
        f.line(City.lighten(col, 0.18f), 0.5f, 1.4f, 1.7f, f.L - 1.4f, 1.7f);
    }

    /** A box standing on a roof or the ground: a soft shadow, a darker body and a lit top. */
    static void unit(F f, float u0, float v0, float u1, float v1, int col) {
        f.rect(0x36000000, u0 + 0.8f, v0 + 0.8f, u1 + 1.4f, v1 + 1.4f);
        f.rect(City.darken(col, 0.74f), u0, v0, u1, v1);
        f.rect(col, u0, v0, u1 - 0.7f, v1 - 0.7f);
        f.line(City.lighten(col, 0.25f), 0.5f, u0, v0 + 0.25f, u1 - 0.7f, v0 + 0.25f);
    }

    /** An air-conditioning unit with its round fan grille. */
    static void fan(F f, float u, float v, float r) {
        unit(f, u - r - 1, v - r - 1, u + r + 1, v + r + 1, 0xFFB4B8BC);
        f.circle(0xFF44484C, u - 0.3f, v - 0.3f, r);
        f.ring(0xFF8A8E92, 0.4f, u - 0.3f, v - 0.3f, r * 0.62f);
        f.line(0xFF8A8E92, 0.5f, u - 0.3f - r, v - 0.3f, u - 0.3f + r, v - 0.3f);
        f.line(0xFF8A8E92, 0.5f, u - 0.3f, v - 0.3f - r, u - 0.3f, v - 0.3f + r);
    }

    /** A round mushroom vent. */
    static void vent(F f, float u, float v, float r) {
        f.circle(0x3A000000, u + r * 0.5f, v + r * 0.5f, r);
        f.circle(0xFF6E7276, u, v, r);
        f.circle(0xFFA4A8AC, u - r * 0.22f, v - r * 0.22f, r * 0.55f);
    }

    /** A glazed rooflight with its frame and a glint. */
    static void glass(F f, float u0, float v0, float u1, float v1) {
        f.rect(0xFF4E5458, u0, v0, u1, v1);
        f.rect(0xFF7FA6BA, u0 + 0.6f, v0 + 0.6f, u1 - 0.6f, v1 - 0.6f);
        f.rect(0xFF9CC0D2, u0 + 0.6f, v0 + 0.6f, u1 - 0.6f, v0 + (v1 - v0) * 0.4f);
        f.line(0x70FFFFFF, 0.5f, u0 + 1.2f, v1 - 1.2f, u0 + (u1 - u0) * 0.4f, v0 + 1.2f);
    }

    /** A traffic cone. */
    static void cone(F f, float u, float v) {
        f.circle(0x34000000, u + 0.9f, v + 0.9f, 1.5f);
        f.rect(0xFFD8561A, u - 1.5f, v - 1.5f, u + 1.5f, v + 1.5f);
        f.circle(0xFFF07A2A, u, v, 1.1f);
        f.circle(0xFFF2F2EE, u, v, 0.65f);
        f.circle(0xFFF07A2A, u, v, 0.3f);
    }

    /** A fire hydrant. */
    static void hydrant(F f, float u, float v, int col) {
        f.circle(0x40000000, u + 1, v + 1, 1.7f);
        f.rect(City.darken(col, 0.7f), u - 2.2f, v - 0.6f, u + 2.2f, v + 0.6f);
        f.circle(City.darken(col, 0.8f), u, v, 1.6f);
        f.circle(col, u - 0.3f, v - 0.3f, 1.1f);
        f.circle(0xFFD8D0B8, u - 0.3f, v - 0.3f, 0.45f);
    }

    /** A box vehicle (van, lorry, bus) from above, its front at +u (along) or +v; returns nothing, cab marked. */
    static void veh(F f, float u, float v, float len, float wid, boolean alongU, boolean fwd, int body) {
        float a = alongU ? len / 2 : wid / 2, b = alongU ? wid / 2 : len / 2;
        f.roundRect(0x3A000000, u - a + 1.2f, v - b + 1.2f, u + a + 1.6f, v + b + 1.6f, 1.4f);
        f.roundRect(City.darken(body, 0.78f), u - a, v - b, u + a, v + b, 1.4f);
        f.roundRect(body, u - a + 0.4f, v - b + 0.4f, u + a - 0.8f, v + b - 0.8f, 1.2f);
        float s = fwd ? 1 : -1;
        if (alongU) {
            float fu = u + s * (a - 2.2f);
            f.rect(0xFF262C34, Math.min(fu, fu + s * 1.4f), v - b + 0.6f, Math.max(fu, fu + s * 1.4f), v + b - 0.6f);
        } else {
            float fv = v + s * (b - 2.2f);
            f.rect(0xFF262C34, u - a + 0.6f, Math.min(fv, fv + s * 1.4f), u + a - 0.6f, Math.max(fv, fv + s * 1.4f));
        }
    }

    /** A chain-link fence round a rectangle, with posts. */
    static void fence(F f, float u0, float v0, float u1, float v1, int col) {
        f.frame(col, 0.5f, u0, v0, u1, v1);
        float st = 8;
        for (float u = u0; u <= u1 + 0.1f; u += (u1 - u0) / Math.max(1, Math.round((u1 - u0) / st))) {
            f.circle(City.darken(col, 0.7f), u, v0, 0.6f);
            f.circle(City.darken(col, 0.7f), u, v1, 0.6f);
        }
        for (float v = v0; v <= v1 + 0.1f; v += (v1 - v0) / Math.max(1, Math.round((v1 - v0) / st))) {
            f.circle(City.darken(col, 0.7f), u0, v, 0.6f);
            f.circle(City.darken(col, 0.7f), u1, v, 0.6f);
        }
    }

    /** A grid of paving joints over a rectangle. */
    static void joints(F f, int col, float u0, float v0, float u1, float v1, float st) {
        for (float u = u0 + st; u < u1 - 0.5f; u += st) f.line(col, 0.4f, u, v0, u, v1);
        for (float v = v0 + st; v < v1 - 0.5f; v += st) f.line(col, 0.4f, u0, v, u1, v);
    }

    /** A person from above (head and shoulders). */
    static void person(F f, float u, float v, int shirt) {
        f.circle(0x30000000, u + 0.8f, v + 0.8f, 2.2f);
        f.oval(shirt, u - 2.4f, v - 1.4f, u + 2.4f, v + 1.4f);
        f.circle(0xFF5A3E2A, u, v, 1.1f);
    }

    /** A tent from above: a ridge along u, the lit and shaded slopes, and guy pegs. */
    static void tent(F f, float u0, float v0, float u1, float v1, int col) {
        f.rect(0x34000000, u0 + 1.5f, v0 + 1.5f, u1 + 2, v1 + 2);
        float m = (v0 + v1) / 2;
        f.rect(City.lighten(col, 0.08f), u0, v0, u1, m);
        f.rect(City.darken(col, 0.8f), u0, m, u1, v1);
        f.line(City.darken(col, 0.6f), 0.6f, u0, m, u1, m);
        for (float u = u0; u <= u1 + 0.1f; u += (u1 - u0) / 2) {
            f.line(0x50000000, 0.3f, u, v0, u, v0 - 2);
            f.line(0x50000000, 0.3f, u, v1, u, v1 + 2);
        }
    }

    /** A sub-frame over the rectangle u0..u1, v0..v1 of f (with its own long side). */
    static F sub(F f, float u0, float v0, float u1, float v1) {
        return new F(f.c, f.p, f.x(u0, v0), f.y(u0, v0), f.x(u1, v1), f.y(u1, v1));
    }

    /** A dog from above, facing +u (or -u). */
    static void dog(F f, float u, float v, int col, boolean right) {
        float s = right ? 1 : -1;
        f.oval(0x30000000, u - 2.4f + 0.8f, v - 1.1f + 0.8f, u + 2.4f + 0.8f, v + 1.1f + 0.8f);
        f.oval(col, u - 2.4f, v - 1.1f, u + 2.4f, v + 1.1f);
        f.circle(City.darken(col, 0.85f), u + s * 2.8f, v, 1.2f);
        f.circle(City.darken(col, 0.6f), u + s * 2.6f, v - 1, 0.5f);
        f.circle(City.darken(col, 0.6f), u + s * 2.6f, v + 1, 0.5f);
        f.line(col, 0.6f, u - s * 2.3f, v, u - s * 3.8f, v - 0.8f);
    }

    /** A horse from above, facing +u. */
    static void horse(F f, float u, float v, int col) {
        f.oval(0x30000000, u - 4.5f + 1.2f, v - 1.8f + 1.2f, u + 4.5f + 1.2f, v + 1.8f + 1.2f);
        f.oval(col, u - 4.5f, v - 1.8f, u + 4.5f, v + 1.8f);
        f.oval(col, u + 3.5f, v - 0.9f, u + 7.5f, v + 0.9f);
        f.line(City.darken(col, 0.6f), 0.8f, u + 3, v, u + 6, v);
        f.line(City.darken(col, 0.5f), 0.9f, u - 4.4f, v, u - 6.6f, v + 0.4f);
    }

    // ================================================================== 1 Animal Shelter

    static void shelter(List<Landmarks.Def> out) {
        // A brick office with a hipped roof beside a long kennel block; fenced concrete runs before the kennels with
        // dogs, bowls and beds, and a grassy exercise yard with a tyre and a ball.
        Landmarks.Def d = new Landmarks.Def("Animal Shelter", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh), a = Math.max(4, L / 3);
                return new int[][]{at(al, 1, 1, a, Math.min(S - 5, 6), 2), at(al, a + 3, 1, L - a - 4, 3, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] q = pl(this, x0, y0, x1, y1);
                float[] o = bx(f, q[0]), k = bx(f, q[1]);
                f.rect(0xFF789A56, 0, 0, L, S);
                // Gravel drive to the office and a path along the front.
                f.rect(0xFFC6BA9E, o[0] + 8, o[3], o[0] + 22, S);
                f.rect(0xFFC6BA9E, 0, S - 9, L, S - 3);
                joints(f, 0x16000000, 0, S - 9, L, S - 3, 6);
                f.bush(o[0] + 4, o[3] + 5, 3);
                f.bush(o[2] - 4, o[3] + 5, 3);
                // The runs: concrete pens behind chain link, one per kennel door.
                float rv1 = Math.min(S - 14, k[3] + 44);
                int n = Math.max(2, (int) ((k[2] - k[0]) / 15));
                float pw = (k[2] - k[0]) / n;
                f.rect(0xFFB8B4AA, k[0], k[3], k[2], rv1);
                int[] dc = {0xFFC89A5A, 0xFF3A3028, 0xFFE8E0D0, 0xFF8A6A4A, 0xFF5A4A3A};
                for (int i = 0; i < n; i++) {
                    float u0 = k[0] + i * pw;
                    f.line(0x22000000, 0.5f, u0 + pw / 2, k[3] + 2, u0 + pw / 2, rv1 - 2);
                    f.rect(0xFF6A5C4A, u0 + pw / 2 - 3, k[3], u0 + pw / 2 + 3, k[3] + 1.2f);
                    f.circle(0xFF3A6AA8, u0 + 3, rv1 - 4, 1.4f);
                    f.circle(0xFF8ABCE0, u0 + 3, rv1 - 4, 0.8f);
                    f.roundRect(0xFF9A5A4A, u0 + pw - 8, k[3] + 3, u0 + pw - 2, k[3] + 8, 1.5f);
                    f.roundRect(0xFFC07A62, u0 + pw - 7.2f, k[3] + 3.8f, u0 + pw - 2.8f, k[3] + 7.2f, 1.2f);
                    if (i % 3 != 2) dog(f, u0 + pw * 0.45f, k[3] + (rv1 - k[3]) * (i % 2 == 0 ? 0.55f : 0.75f), dc[i % 5], i % 2 == 0);
                    if (i > 0) f.line(0xFF8A8E90, 0.6f, u0, k[3], u0, rv1);
                }
                f.frame(0xFF7A7E80, 0.7f, k[0], k[3], k[2], rv1);
                // The exercise yard: a tyre, a ball, a ramp, a shade tree and a fence.
                float yv0 = rv1 + 3, yv1 = S - 11;
                if (yv1 - yv0 > 10) {
                    f.rect(0xFF86A862, k[0], yv0, k[2], yv1);
                    fence(f, k[0], yv0, k[2], yv1, 0xFF9A9EA0);
                    float tu = k[0] + 12, tv = (yv0 + yv1) / 2;
                    f.ring(0xFF2A2A2A, 1.6f, tu, tv, 3);
                    f.circle(0xFFE0D040, tu + 12, tv + 2, 1.2f);
                    f.rect(0xFF9A7A50, tu + 20, tv - 2, tu + 30, tv + 2);
                    for (float u = tu + 21; u < tu + 30; u += 2) f.line(0x30000000, 0.4f, u, tv - 2, u, tv + 2);
                    dog(f, tu + 40 < k[2] - 6 ? tu + 40 : tu + 6, tv + 3, 0xFFB08050, false);
                } else {
                    f.circle(0xFFE0D040, k[0] + 8, rv1 + 2, 1.2f);
                }
                f.tree(k[2] - 4, k[3] + 2, 8, 0xFF4A7A3A);
                f.tree(o[0] - 2 > 6 ? o[0] - 2 : 6, S - 16, 7);
                // A donation bin and a bench by the office door.
                unit(f, o[2] - 12, o[3] + 2, o[2] - 6, o[3] + 7, 0xFF3E7A9A);
                f.bench(o[0] + 28 < o[2] - 14 ? o[0] + 28 : o[0] + 4, o[3] + 4, true);
                f.lamp(o[0] + 7, S - 10);
                f.lamp(L - 6, S - 10);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                if (part == 0) {
                    f.rect(0xFF7A3E2E, 0, 0, L, S);
                    hip(f, 0xFFA65A3E, 1, 1, L - 1, S - 1, true);
                    unit(f, L * 0.3f - 2, S * 0.3f - 2, L * 0.3f + 2, S * 0.3f + 2, 0xFF8A5040);
                    vent(f, L * 0.7f, S * 0.65f, 1.4f);
                    f.sign(city, "ANIMAL SHELTER", L / 2, S / 2, 4.5f, 0xFFF4E8D0);
                } else {
                    f.rect(0xFF5E6A64, 0, 0, L, S);
                    gable(f, 0xFF8A9890, 0.5f, 0.5f, L - 0.5f, S - 0.5f);
                    for (float u = 2; u < L - 1; u += 2) f.line(0x20000000, 0.4f, u, 0.5f, u, S - 0.5f);
                    for (float u = 8; u < L - 6; u += 16) {
                        f.rect(0xB0D8E4E0, u, 1.5f, u + 5, S / 2 - 0.6f);
                        unit(f, u + 8, S / 2 - 1.5f, u + 11, S / 2 + 1.5f, 0xFF6E7A74);
                    }
                }
            }
        };
        d.minS = 10;
        d.minL = 13;
        d.ground = City.GRASS;
        d.base = City.OFFICE;
        d.inside = Variants.I_WARD;
        d.roofCol = 0xFFA65A3E;
        d.districts = Variants.SUB | Variants.IND | Variants.MID;
        out.add(d);
    }

    // ================================================================== 2 Veterinary Clinic

    static void vet(List<Landmarks.Def> out) {
        // A crisp white clinic with a curved entrance canopy and a paw on the roof; a red stable for large animals with
        // a railed paddock and a horse, a pet relief garden and two parking bays.
        Landmarks.Def d = new Landmarks.Def("Veterinary Clinic", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh), cl = Math.max(5, L * 11 / 20);
                return new int[][]{at(al, 1, 1, cl, Math.min(S - 5, 6), 1), at(al, L - 4, 1, 3, 3, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] q = pl(this, x0, y0, x1, y1);
                float[] h = bx(f, q[0]), st = bx(f, q[1]);
                f.rect(0xFF74A052, 0, 0, L, S);
                // Paved forecourt and two bays.
                float mu = (h[0] + h[2]) / 2;
                f.rect(0xFFD2CCC0, h[0], h[3], h[2], h[3] + 14);
                joints(f, 0x14000000, h[0], h[3], h[2], h[3] + 14, 7);
                f.rect(0xFFD2CCC0, mu - 8, h[3] + 14, mu + 8, S);
                f.rect(0xFF5A5C60, h[0] + 2, h[3] + 16, mu - 10, S - 2);
                for (int i = 0; i < 3; i++) {
                    float u = h[0] + 2 + i * (mu - 12 - h[0]) / 2;
                    f.line(0xFFE8E8E0, 0.6f, u, h[3] + 17, u, S - 6);
                }
                if (S - 2 - h[3] - 16 > 14) {
                    car(f, h[0] + 2 + (mu - 12 - h[0]) * 0.25f, h[3] + 24, false, 0xFF4A6A8A);
                    car(f, h[0] + 2 + (mu - 12 - h[0]) * 0.75f, h[3] + 25, false, 0xFFB8B4AC);
                }
                // The canopy: a white swoosh on slim posts over the doors.
                f.roundRect(0x38000000, mu - 13, h[3] - 1, mu + 15, h[3] + 11, 6);
                f.roundRect(0xFFF2F2EE, mu - 14, h[3] - 2, mu + 14, h[3] + 9, 6);
                f.roundRect(0xFFDADAD4, mu - 14, h[3] + 5, mu + 14, h[3] + 9, 4);
                f.circle(0xFF5A9A8A, mu, h[3] + 3, 1.6f);
                // The paddock: white post and rail before the stable, a horse and a water trough.
                float pu0 = st[0] - 6, pv0 = st[3] + 4, pu1 = L - 4, pv1 = S - 5;
                if (pu0 < h[2] + 4) pu0 = h[2] + 4;
                f.rect(0xFF86AC5E, pu0, pv0, pu1, pv1);
                f.rect(0xFFAA9670, st[0] + 6, st[3], st[2] - 6, pv0 + 6);
                f.frame(0xFFF0EEE6, 1.2f, pu0, pv0, pu1, pv1);
                f.frame(0x40000000, 0.4f, pu0 + 1, pv0 + 1, pu1 + 1, pv1 + 1);
                if (pv1 - pv0 > 14) horse(f, (pu0 + pu1) / 2 - 3, (pv0 + pv1) / 2 + 2, 0xFF7A4E2E);
                f.rect(0xFF6A6E72, pu1 - 12, pv0 + 2, pu1 - 3, pv0 + 5);
                f.rect(0xFF6A9ABA, pu1 - 11.4f, pv0 + 2.6f, pu1 - 3.6f, pv0 + 4.4f);
                // The relief garden: bark chips, a post, a bin and a bench.
                float gu = h[2] - 26, gv = h[3] + 18;
                if (gv + 12 < S) {
                    f.roundRect(0xFF8A6A48, gu, gv, gu + 20, gv + 12, 3);
                    f.circle(0xFFC83A2A, gu + 6, gv + 6, 1.6f);
                    f.circle(0xFFE05A4A, gu + 5.6f, gv + 5.6f, 0.8f);
                    unit(f, gu + 14, gv + 3, gu + 17, gv + 6, 0xFF3A5A3A);
                }
                f.tree(L - 6, S - 4 < pv1 ? 6 : S - 4, 6);
                f.bush(h[0] + 3, h[3] + 3, 2.6f);
                f.bush(h[2] - 3, h[3] + 3, 2.6f);
                f.tree(4, S - 6, 6, 0xFF4E8040);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                if (part == 0) {
                    deck(f, 0xFFE6E6E0);
                    // A teal band along the front edge and a row of rooflights.
                    f.rect(0xFF4E8A80, 1.4f, S - 4, L - 1.4f, S - 1.4f);
                    for (float u = 6; u < L * 0.45f; u += 9) glass(f, u, 4, u + 5, 9);
                    fan(f, L - 8, 7, 2.4f);
                    fan(f, L - 16, 7, 2.4f);
                    vent(f, L - 7, S - 9, 1.3f);
                    // A paw print.
                    float pu = L * 0.62f, pv = S * 0.5f, s = Math.min(S, 40) / 40f;
                    f.oval(0xFF4E8A80, pu - 5 * s, pv - 2 * s, pu + 5 * s, pv + 5 * s);
                    for (int i = 0; i < 4; i++) f.circle(0xFF4E8A80, pu + (i - 1.5f) * 3.6f * s, pv - (i == 0 || i == 3 ? 4 : 6) * s, 1.6f * s);
                    f.sign(city, "VET CLINIC", L * 0.3f, S * 0.6f, 4.5f, 0xFF2E5A54);
                } else {
                    f.rect(0xFF6A2A22, 0, 0, L, S);
                    gable(f, 0xFF9A3A2E, 0.6f, 0.6f, L - 0.6f, S - 0.6f);
                    for (float u = 2; u < L - 1; u += 2.5f) f.line(0x1C000000, 0.4f, u, 0.6f, u, S - 0.6f);
                    unit(f, L / 2 - 3, S / 2 - 3, L / 2 + 3, S / 2 + 3, 0xFFE8E2D4);
                    f.rect(0xFF4A4A50, L / 2 - 2.4f, S / 2 - 2.4f, L / 2 + 1.7f, S / 2 + 1.7f);
                }
            }
        };
        d.minS = 10;
        d.minL = 13;
        d.ground = City.GRASS;
        d.base = City.OFFICE;
        d.inside = Variants.I_WARD;
        d.roofCol = 0xFFE6E6E0;
        d.districts = Variants.SUB | Variants.MID | Variants.PARK;
        out.add(d);
    }

    // ================================================================== 3 Branch Library

    static void branchLib(List<Landmarks.Def> out) {
        // A small modern library under a sedum roof with a long sawtooth clerestory; before it a reading lawn with a
        // curving path, grass steps, beanbags, a picnic blanket, a book return and a bike rack.
        Landmarks.Def d = new Landmarks.Def("Branch Library", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, 1, 1, Math.max(6, L * 3 / 5), Math.max(4, S / 2), 2)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] h = bx(f, pl(this, x0, y0, x1, y1)[0]);
                f.rect(0xFF72A250, 0, 0, L, S);
                // Mown stripes.
                for (float u = 0; u < L; u += 16) f.rect(0x0CFFFFFF, u, 0, u + 8, S);
                // Timber deck along the glazed front and a curving path to the corner.
                f.rect(0xFFB08A5E, h[0], h[3], h[2], h[3] + 7);
                for (float u = h[0] + 2; u < h[2]; u += 2.2f) f.line(0x22000000, 0.4f, u, h[3], u, h[3] + 7);
                Path pa = new Path();
                float du = h[0] + (h[2] - h[0]) * 0.3f;
                f.mv(pa, du - 5, h[3] + 7);
                f.qd(pa, du - 5, S - 10, du + 30, S);
                f.lt(pa, du + 42, S);
                f.qd(pa, du + 5, S - 14, du + 5, h[3] + 7);
                pa.close();
                f.fill(pa, 0xFFD6CCB4);
                // Grass steps facing a story-time spot.
                float au = h[2] + (L - h[2]) / 2, av = S * 0.55f;
                if (L - h[2] > 40) {
                    for (int i = 3; i >= 1; i--) {
                        f.oval(i % 2 == 0 ? 0xFF8A8478 : 0xFF6E9A4C, au - 6 * i - 4, av - 5 * i - 3, au + 6 * i + 4, av + 5 * i + 3);
                    }
                    f.circle(0xFFC8BCA0, au, av, 4);
                    f.tree(au, 10, 9, 0xFF3E7236);
                } else {
                    f.tree(L - 10, S * 0.35f, 8, 0xFF3E7236);
                }
                // Beanbags and a picnic blanket on the lawn.
                int[] bb = {0xFFD8603A, 0xFF3A7AB0, 0xFFE0B040, 0xFF8A4A9A};
                float lu = h[0] + (h[2] - h[0]) * 0.62f, lv = h[3] + 20;
                if (lv + 12 < S) {
                    for (int i = 0; i < 4; i++) {
                        float u = lu + (i % 2) * 11, v = lv + (i / 2) * 9;
                        f.circle(0x30000000, u + 1, v + 1, 3);
                        f.circle(bb[i], u, v, 3);
                        f.circle(City.lighten(bb[i], 0.2f), u - 0.8f, v - 0.8f, 1.3f);
                    }
                    f.rect(0xFFD84A4A, h[0] + 6, lv - 2, h[0] + 20, lv + 8);
                    for (float u = h[0] + 8; u < h[0] + 20; u += 4) f.rect(0xFFF2EEE6, u, lv - 2, u + 2, lv + 8);
                    person(f, h[0] + 12, lv + 3, 0xFF4A6A9A);
                }
                // A book return chute, a bike rack and lamps.
                unit(f, h[0] + 2, h[3] + 9, h[0] + 7, h[3] + 13, 0xFF3A6A8A);
                for (int i = 0; i < 4; i++) f.line(0xFF8A9096, 0.7f, h[2] - 14 + i * 3, h[3] + 9, h[2] - 14 + i * 3, h[3] + 14);
                f.lamp(du - 8, S - 14);
                f.lamp(du + 20, S - 5);
                f.bench(du + 14, S - 14, true);
                f.tree(L - 8, S - 8, 7);
                for (float u = h[0] + 6; u < h[2]; u += 14) f.bush(u, h[1] / 2, 2.4f);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF8A6A48, 0, 0, L, S);
                f.rect(0xFF6E8A48, 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                // Sedum patches in reds and greens.
                int[] sc = {0xFF86A050, 0xFF9A7A4A, 0xFF5E7E40, 0xFFA06A5A};
                for (int i = 0; i < (int) (L * S / 40); i++) {
                    float u = 3 + r.nextFloat() * (L - 6), v = 3 + r.nextFloat() * (S - 6);
                    f.circle(sc[i % 4], u, v, 1 + r.nextFloat() * 1.6f);
                }
                // The sawtooth clerestory: glazed slopes facing the lawn.
                float v0 = S * 0.42f, v1 = S * 0.62f;
                for (float u = 4; u + 10 < L - 3; u += 12) {
                    f.rect(0xFFDAD6CC, u, v0, u + 10, v1);
                    f.rect(0xFF7FA6BA, u, v1 - 2.5f, u + 10, v1);
                    f.rect(0x30000000, u + 10, v0 + 1, u + 11.5f, v1 + 1);
                }
                f.rect(0xFF5A5E62, 4, S - 7, L * 0.4f, S - 3);
                for (float u = 5; u < L * 0.4f - 1; u += 3) f.rect(0xFF2E3E5A, u, S - 6.4f, u + 2.4f, S - 3.6f);
                f.sign(city, "LIBRARY", L * 0.7f, S * 0.25f, 5, 0xFFF4EED8);
            }
        };
        d.minS = 10;
        d.minL = 12;
        d.ground = City.GRASS;
        d.base = City.OFFICE;
        d.inside = Variants.I_LIBRARY;
        d.roofCol = 0xFF6E8A48;
        d.districts = Variants.SUB | Variants.MID | Variants.PARK | Variants.UNI;
        out.add(d);
    }

    // ================================================================== 4 Community Centre

    static void community(List<Landmarks.Def> out) {
        // A hall under a pitched roof with solar panels, a painted basketball court beside it and a community garden
        // of raised beds, compost bins, a shed and picnic tables before the doors.
        Landmarks.Def d = new Landmarks.Def("Community Centre", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, 1, 1, Math.max(5, L * 9 / 20), Math.min(S - 5, 7), 2)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] h = bx(f, pl(this, x0, y0, x1, y1)[0]);
                f.rect(0xFF76A054, 0, 0, L, S);
                f.rect(0xFFCAC2B0, h[0], h[3], h[2], h[3] + 8);
                joints(f, 0x14000000, h[0], h[3], h[2], h[3] + 8, 8);
                // The basketball court.
                float cu0 = h[2] + 12, cv0 = 8, cu1 = L - 8, cv1 = S - 10;
                f.rect(0xFFB0AAA0, cu0 - 4, cv0 - 4, cu1 + 4, cv1 + 4);
                fence(f, cu0 - 4, cv0 - 4, cu1 + 4, cv1 + 4, 0xFF8A8E90);
                F g = sub(f, cu0, cv0, cu1, cv1);
                float gl = g.L, gs = g.S;
                g.rect(0xFF3E6E8E, 0, 0, gl, gs);
                g.rect(0xFFB0563A, 4, 4, gl - 4, gs - 4);
                int w = 0xFFF0EEE6;
                g.frame(w, 0.7f, 4, 4, gl - 4, gs - 4);
                g.line(w, 0.7f, gl / 2, 4, gl / 2, gs - 4);
                g.ring(w, 0.7f, gl / 2, gs / 2, gs * 0.14f);
                for (int s = 0; s < 2; s++) {
                    float e = s == 0 ? 4 : gl - 4, dir = s == 0 ? 1 : -1, kl = gl * 0.2f;
                    g.rect(0xFF3E6E8E, Math.min(e, e + dir * kl), gs / 2 - gs * 0.13f, Math.max(e, e + dir * kl), gs / 2 + gs * 0.13f);
                    g.frame(w, 0.6f, Math.min(e, e + dir * kl), gs / 2 - gs * 0.13f, Math.max(e, e + dir * kl), gs / 2 + gs * 0.13f);
                    g.ring(w, 0.6f, e, gs / 2, gs * 0.42f);
                    g.rect(0xFF2A2A2E, e - dir * 1.5f - 0.6f, gs / 2 - 3, e - dir * 1.5f + 0.6f, gs / 2 + 3);
                    g.ring(0xFFE06A2A, 0.6f, e + dir * 1.5f, gs / 2, 1.3f);
                }
                g.rect(0xFFB0563A, 0, 0, 4, 4);
                // The court lines' overspill outside the playing area is the fence apron; a ball.
                g.circle(0xFFD86A2A, gl * 0.3f, gs * 0.6f, 1.2f);
                // The garden: raised timber beds with crops before the hall.
                float gv0 = h[3] + 12, gv1 = S - 6;
                if (gv1 - gv0 > 10) {
                    int[] crop = {0xFF5E9A3E, 0xFF8AB04A, 0xFF4A7A34, 0xFFA0B85A};
                    int n = Math.max(2, (int) ((h[2] - h[0] - 10) / 20));
                    float bw = (h[2] - h[0] - 10) / n;
                    for (int i = 0; i < n; i++) {
                        float u0 = h[0] + 6 + i * bw, u1 = u0 + bw - 5;
                        unit(f, u0, gv0, u1, gv1, 0xFF8A6A44);
                        f.rect(0xFF5A4430, u0 + 1.2f, gv0 + 1.2f, u1 - 1.8f, gv1 - 1.8f);
                        for (float v = gv0 + 3; v < gv1 - 2; v += 3) {
                            for (float u = u0 + 3; u < u1 - 2; u += 3) f.circle(crop[(i + (int) v) % 4], u, v, 1.1f);
                        }
                    }
                }
                // Compost bins, a shed, picnic tables and trees.
                unit(f, 3, S - 14, 9, S - 8, 0xFF4A6A3A);
                float tu = h[2] - 6, tv = h[3] + 4 + (S - h[3]) / 2;
                f.rect(0x34000000, tu - 2, tv - 4, tu + 6, tv + 6);
                f.rect(0xFF8A6A44, tu - 4, tv - 6, tu + 4, tv + 4);
                f.rect(0xFF6E5034, tu - 6, tv - 6, tu - 4.5f, tv + 4);
                f.rect(0xFF6E5034, tu + 4.5f, tv - 6, tu + 6, tv + 4);
                f.tree(L - 6, S - 5, 6, 0xFF4A8040);
                f.tree(h[2] + 5, S - 5, 5);
                f.lamp(cu0 - 6, cv1 + 6);
                f.lamp(cu1 + 6, cv0 - 6 < 2 ? 2 : cv0 - 6);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF5A4E46, 0, 0, L, S);
                gable(f, 0xFF8A7A6A, 0.5f, 0.5f, L - 0.5f, S - 0.5f);
                for (float u = 3; u < L - 1; u += 3) f.line(0x1A000000, 0.4f, u, 0.5f, u, S - 0.5f);
                // Solar panels on the lower (sunny) slope.
                float pv0 = S / 2 + 2, pv1 = S - 3;
                for (float u = 4; u + 8 < L - 3; u += 9) {
                    f.rect(0xFF8A9096, u, pv0, u + 8, pv1);
                    f.rect(0xFF2A3A5E, u + 0.5f, pv0 + 0.5f, u + 7.5f, pv1 - 0.5f);
                    f.line(0x40FFFFFF, 0.3f, u + 4, pv0 + 0.5f, u + 4, pv1 - 0.5f);
                    for (float v = pv0 + 3; v < pv1 - 1; v += 3) f.line(0x30FFFFFF, 0.3f, u + 0.5f, v, u + 7.5f, v);
                }
                // Clerestory along the ridge and a little bell cote.
                for (float u = 5; u < L - 5; u += 7) glass(f, u, S / 2 - 4, u + 4, S / 2 - 1);
                f.sign(city, "COMMUNITY CENTRE", L / 2, S * 0.25f, 4.5f, 0xFFF6EED8);
            }
        };
        d.minS = 10;
        d.minL = 14;
        d.ground = City.GRASS;
        d.base = City.OFFICE;
        d.inside = Variants.I_SEATS;
        d.roofCol = 0xFF8A7A6A;
        d.districts = Variants.SUB | Variants.MID | Variants.PARK;
        out.add(d);
    }

    // ================================================================== 5 Police Museum

    static void policeMuseum(List<Landmarks.Def> out) {
        // An old stone police station with a slate roof and a blue lamp, its former cell block beside it; on the
        // cobbled forecourt a blue police box, a vintage black patrol car, an old motorcycle and a paddy wagon.
        Landmarks.Def d = new Landmarks.Def("Police Museum", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh), mw = Math.max(6, L * 11 / 20);
                return new int[][]{at(al, 1, 1, mw, Math.max(4, S / 2), 2), at(al, mw + 3, 1, L - mw - 4, Math.max(3, S / 2 - 1), 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] q = pl(this, x0, y0, x1, y1);
                float[] h = bx(f, q[0]), a = bx(f, q[1]);
                f.rect(0xFFA8A090, 0, 0, L, S);
                for (float v = 0; v < S; v += 4) {
                    float off = ((int) (v / 4)) % 2 == 0 ? 0 : 2;
                    f.line(0x1E000000, 0.4f, 0, v, L, v);
                    for (float u = off; u < L; u += 4) f.line(0x14000000, 0.4f, u, v, u, v + 4);
                }
                // Stone steps before the door, iron railings along the street with gaps.
                float mu = (h[0] + h[2]) / 2;
                steps(f, mu - 10, h[3], mu + 10, h[3] + 6, 3, 0xFFC4BCAA);
                f.line(0xFF2A2C30, 0.8f, 2, S - 3, mu - 14, S - 3);
                f.line(0xFF2A2C30, 0.8f, mu + 14, S - 3, L - 2, S - 3);
                for (float u = 3; u < L - 2; u += 3) if (Math.abs(u - mu) > 14) f.circle(0xFF2A2C30, u, S - 3, 0.5f);
                // The blue lamp on its post.
                f.tall(mu - 13, h[3] + 4, 0.8f, 8);
                f.circle(0xFF2A2C30, mu - 13, h[3] + 4, 1);
                f.circle(0xFF2A5AB0, mu - 13, h[3] + 4, 1.8f);
                f.circle(0xFF8AB0F0, mu - 13.4f, h[3] + 3.6f, 0.8f);
                // The police box.
                float bu = h[0] + 8, bv = h[3] + 12;
                if (bv + 8 < S - 4) {
                    f.rect(0x40000000, bu + 1.5f, bv + 1.5f, bu + 9.5f, bv + 9.5f);
                    f.rect(0xFF1A2E5A, bu, bv, bu + 8, bv + 8);
                    f.rect(0xFF2A4278, bu + 1, bv + 1, bu + 7, bv + 7);
                    f.rect(0xFF1A2E5A, bu + 2.2f, bv + 2.2f, bu + 5.8f, bv + 5.8f);
                    f.circle(0xFFE8F0FF, bu + 4, bv + 4, 1);
                }
                // The vintage patrol car, the motorcycle and the paddy wagon on display.
                float cv = Math.min(S - 12, h[3] + 18);
                car(f, mu + 4, cv, true, 0xFF1E1E22);
                f.rect(0xFFE8E8E8, mu + 2, cv - 0.8f, mu + 4, cv + 0.8f);
                f.circle(0xFF3A6ACA, mu + 3, cv, 0.7f);
                float mbu = mu + 18;
                if (mbu + 4 < a[0] - 2 || a[3] + 30 > S) {
                    f.line(0x30000000, 2, mbu + 1, cv + 1, mbu + 7, cv + 1);
                    f.line(0xFF2A2A2A, 1.6f, mbu, cv, mbu + 6, cv);
                    f.circle(0xFF6A6E72, mbu + 1.4f, cv, 0.9f);
                    f.line(0xFFB8BCC0, 0.5f, mbu + 5, cv - 1.8f, mbu + 5, cv + 1.8f);
                }
                float wv = a[3] + 12;
                if (wv + 6 < S - 4) {
                    veh(f, (a[0] + a[2]) / 2, wv, 16, 7, true, true, 0xFF22324A);
                    f.rect(0xFFE8E4D8, (a[0] + a[2]) / 2 - 6, wv - 1, (a[0] + a[2]) / 2 + 3, wv + 0.4f);
                }
                // A flagpole and two clipped bays.
                flag(f, h[0] + 4, h[3] + 8 > S - 10 ? S - 10 : h[3] + 8, 0xFF2A3E7A, 0xFFF0F0F0, 0xFF2A3E7A);
                f.bush(h[2] - 4, h[3] + 4, 2.4f);
                f.bush(mu - 16, h[3] + 3, 2.2f);
                f.bush(mu + 16, h[3] + 3, 2.2f);
                f.lamp(L - 4, S - 6);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                if (part == 0) {
                    f.rect(0xFF8A8270, 0, 0, L, S);
                    hip(f, 0xFF4E5662, 1.5f, 1.5f, L - 1.5f, S - 1.5f, true);
                    // Stone chimneys with pots.
                    for (int s = 0; s < 2; s++) {
                        float u = s == 0 ? L * 0.22f : L * 0.78f;
                        unit(f, u - 3, S * 0.3f - 2, u + 3, S * 0.3f + 2, 0xFFB0A68E);
                        f.circle(0xFF8A4A30, u - 1.4f, S * 0.3f - 0.3f, 0.9f);
                        f.circle(0xFF8A4A30, u + 1.2f, S * 0.3f - 0.3f, 0.9f);
                    }
                    // A pedimented entrance bay on the front.
                    float mu = L / 2;
                    f.poly(0xFFC8C0AC, mu - 8, S - 1.5f, mu + 8, S - 1.5f, mu, S - 8);
                    f.poly(0xFFA8A090, mu, S - 8, mu + 8, S - 1.5f, mu, S - 1.5f);
                    f.sign(city, "POLICE MUSEUM", mu, S * 0.45f, 4.5f, 0xFFE8EEF8);
                } else {
                    deck(f, 0xFF9A948A);
                    // The cell block's barred rooflights.
                    for (float u = 4; u + 5 < L - 3; u += 8) {
                        f.rect(0xFF3E4246, u, S / 2 - 3, u + 5, S / 2 + 3);
                        f.rect(0xFF8AA2AE, u + 0.6f, S / 2 - 2.4f, u + 4.4f, S / 2 + 2.4f);
                        for (float k = u + 1.4f; k < u + 4.4f; k += 1.2f) f.line(0xFF2A2C30, 0.4f, k, S / 2 - 2.4f, k, S / 2 + 2.4f);
                    }
                    vent(f, L - 4, 4, 1.2f);
                }
            }
        };
        d.minS = 10;
        d.minL = 14;
        d.ground = City.PLAZA;
        d.base = City.OFFICE;
        d.inside = Variants.I_SHOWROOM;
        d.roofCol = 0xFF4E5662;
        d.districts = Variants.OLD | Variants.MID | Variants.DOWN;
        out.add(d);
    }

    /** A satellite dish from above: a pale bowl tilted to the lower left, its feed arm and a shadow. */
    static void dish(F f, float u, float v, float r) {
        f.circle(0x38000000, u + r * 0.5f, v + r * 0.5f, r);
        f.circle(0xFF8A8E92, u, v, r);
        f.circle(0xFFE4E6E6, u - r * 0.08f, v - r * 0.08f, r * 0.86f);
        f.circle(0xFFCED2D4, u + r * 0.15f, v + r * 0.15f, r * 0.5f);
        f.line(0xFF5A5E62, Math.max(0.4f, r * 0.12f), u, v, u - r * 0.75f, v + r * 0.75f);
        f.circle(0xFF3A3E42, u - r * 0.75f, v + r * 0.75f, Math.max(0.5f, r * 0.15f));
    }

    /** A bus from above: roof, window bands, air-con pods; front at +u (along) or +v. */
    static void bus(F f, float u, float v, boolean alongU, boolean fwd, int col) {
        float len = 36, wid = 8.4f;
        veh(f, u, v, len, wid, alongU, fwd, col);
        float s = fwd ? 1 : -1;
        if (alongU) {
            f.rect(0xFF2E343C, u - len / 2 + 2, v - wid / 2 + 0.4f, u + len / 2 - 4, v - wid / 2 + 1.2f);
            f.rect(0xFFF2F2EE, u - len / 2 + 3, v - 1.6f, u + len / 2 - 5, v + 1.2f);
            unit(f, u - s * 6 - 3, v - 2, u - s * 6 + 3, v + 1.6f, 0xFFCCD0D2);
            unit(f, u + s * 6 - 3, v - 2, u + s * 6 + 3, v + 1.6f, 0xFFCCD0D2);
        } else {
            f.rect(0xFF2E343C, u - wid / 2 + 0.4f, v - len / 2 + 2, u - wid / 2 + 1.2f, v + len / 2 - 4);
            f.rect(0xFFF2F2EE, u - 1.6f, v - len / 2 + 3, u + 1.2f, v + len / 2 - 5);
            unit(f, u - 2, v - s * 6 - 3, u + 1.6f, v - s * 6 + 3, 0xFFCCD0D2);
            unit(f, u - 2, v + s * 6 - 3, u + 1.6f, v + s * 6 + 3, 0xFFCCD0D2);
        }
    }

    /** An ambulance: white box body, a chequered side band and a light bar over the cab. */
    static void ambo(F f, float u, float v, boolean alongU, boolean fwd) {
        float len = 18, wid = 7.4f, s = fwd ? 1 : -1;
        veh(f, u, v, len, wid, alongU, fwd, 0xFFF2F2EC);
        if (alongU) {
            for (float k = -len / 2 + 1; k < len / 2 - 4; k += 2) {
                int col = ((int) ((k + 20) / 2)) % 2 == 0 ? 0xFF3A8A4A : 0xFFE8D040;
                f.rect(col, u + k * s, v - wid / 2 + 0.2f, u + (k + 2) * s, v - wid / 2 + 1);
                f.rect(col, u + k * s, v + wid / 2 - 1.4f, u + (k + 2) * s, v + wid / 2 - 0.6f);
            }
            float fu = u + s * (len / 2 - 4.4f);
            f.rect(0xFFD03A2A, fu - 0.8f, v - 2.6f, fu + 0.8f, v - 0.2f);
            f.rect(0xFF3A6ADA, fu - 0.8f, v - 0.2f, fu + 0.8f, v + 2.2f);
            f.rect(0xFFD03A2A, u - s * 2 - 0.6f, v - 2.5f, u - s * 2 + 0.6f, v + 2.1f);
            f.rect(0xFFD03A2A, u - s * 2 - 2.5f, v - 0.6f, u - s * 2 + 2.5f, v + 0.6f);
        } else {
            for (float k = -len / 2 + 1; k < len / 2 - 4; k += 2) {
                int col = ((int) ((k + 20) / 2)) % 2 == 0 ? 0xFF3A8A4A : 0xFFE8D040;
                f.rect(col, u - wid / 2 + 0.2f, v + k * s, u - wid / 2 + 1, v + (k + 2) * s);
                f.rect(col, u + wid / 2 - 1.4f, v + k * s, u + wid / 2 - 0.6f, v + (k + 2) * s);
            }
            float fv = v + s * (len / 2 - 4.4f);
            f.rect(0xFFD03A2A, u - 2.6f, fv - 0.8f, u - 0.2f, fv + 0.8f);
            f.rect(0xFF3A6ADA, u - 0.2f, fv - 0.8f, u + 2.2f, fv + 0.8f);
            f.rect(0xFFD03A2A, u - 0.6f, v - s * 2 - 2.5f, u + 0.6f, v - s * 2 + 2.1f);
            f.rect(0xFFD03A2A, u - 2.5f, v - s * 2 - 0.6f, u + 2.5f, v - s * 2 + 0.6f);
        }
    }

    /** A star of life: a blue six-armed cross with a white staff. */
    static void starOfLife(F f, float u, float v, float r) {
        for (int k = 0; k < 3; k++) {
            double a = k * Math.PI / 3 + Math.PI / 2;
            float cx = (float) Math.cos(a), sy = (float) Math.sin(a), w = r * 0.28f;
            f.poly(0xFF2A5AB0, u + cx * r - sy * w, v + sy * r + cx * w, u + cx * r + sy * w, v + sy * r - cx * w,
                    u - cx * r + sy * w, v - sy * r - cx * w, u - cx * r - sy * w, v - sy * r + cx * w);
        }
        f.line(0xFFF2F2EE, r * 0.12f, u, v - r * 0.7f, u, v + r * 0.7f);
    }

    /** A wooden cable drum lying flat (seen end on). */
    static void drum(F f, float u, float v, float r) {
        f.circle(0x38000000, u + r * 0.4f, v + r * 0.4f, r);
        f.circle(0xFF8A6A44, u, v, r);
        f.ring(0xFF6A4E30, 0.5f, u, v, r * 0.7f);
        f.circle(0xFF2A2A2A, u, v, r * 0.45f);
        f.circle(0xFF8A6A44, u, v, r * 0.2f);
    }

    // ================================================================== 6 Radio Station

    static void radio(List<Landmarks.Def> out) {
        // A three-storey studio block with dishes and a red ON AIR lamp on its roof; a tall lattice mast stayed by
        // three sets of guy wires to concrete anchors, its long shadow over the grass, a transmitter hut and a cable
        // bridge, a dish farm, all behind a security fence.
        Landmarks.Def d = new Landmarks.Def("Radio Station", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, 1, 1, Math.max(5, L * 2 / 5), Math.max(4, S / 2), 3), at(al, L - 4, S - 4, 3, 3, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] q = pl(this, x0, y0, x1, y1);
                float[] s = bx(f, q[0]), h = bx(f, q[1]);
                f.rect(0xFF7C9C58, 0, 0, L, S);
                // Forecourt and drive.
                f.rect(0xFFC8C2B4, s[0], s[3], s[2], s[3] + 10);
                joints(f, 0x14000000, s[0], s[3], s[2], s[3] + 10, 8);
                f.rect(0xFF6A6C70, s[0] + 10, s[3] + 10, s[0] + 26, S);
                f.line(0xFFE8E0A0, 0.5f, s[0] + 18, s[3] + 12, s[0] + 18, S - 2);
                // A dish farm before the studio.
                float dv = s[3] + 22;
                if (dv + 8 < S - 2) for (float u = s[0] + 36; u < s[2] - 6; u += 18) {
                    f.rect(0xFFB8B2A4, u - 7, dv - 7, u + 7, dv + 7);
                    dish(f, u, dv, 5.5f);
                }
                // The mast compound.
                float mu = (s[2] + L) / 2 - 4, mv = S * 0.35f, R = Math.min(S * 0.3f, (L - s[2]) * 0.4f);
                f.rect(0xFFB4AC98, mu - 9, mv - 9, mu + 9, mv + 9);
                fence(f, mu - 9, mv - 9, mu + 9, mv + 9, 0xFF8A8E90);
                // The mast's long shadow, a lattice ladder down to the lower right.
                float len = Math.min(70, Math.min(L - mu, S - mv) - 4);
                f.line(0x30000000, 2.6f, mu, mv, mu + len, mv + len);
                for (float t = 4; t < len; t += 4) f.line(0x28000000, 0.5f, mu + t - 1.4f, mv + t + 1.4f, mu + t + 1.4f, mv + t - 1.4f);
                // The guy wires and their anchor blocks.
                for (int k = 0; k < 3; k++) {
                    double a = -Math.PI / 2 + k * 2 * Math.PI / 3;
                    float au = mu + (float) Math.cos(a) * R, av = mv + (float) Math.sin(a) * R;
                    for (int j = 1; j <= 2; j++) {
                        float tu = mu + (float) Math.cos(a) * 0.6f * j, tv = mv + (float) Math.sin(a) * 0.6f * j;
                        f.line(0xC0303438, 0.5f, tu, tv, au, av);
                    }
                    f.line(0x26000000, 0.4f, mu + len * 0.6f, mv + len * 0.6f, au, av);
                    unit(f, au - 2.4f, av - 2.4f, au + 2.4f, av + 2.4f, 0xFFB8B4AA);
                }
                // The mast itself: a red and white triangle, the beacon at its top.
                f.ngon(0xFFC8C4BC, mu, mv, 3.4f, 3, -Math.PI / 2);
                f.ngon(0xFFD03A2A, mu, mv, 2.4f, 3, -Math.PI / 2);
                f.ngon(0xFFF2F0EA, mu, mv, 1.4f, 3, -Math.PI / 2);
                f.circle(0xFFFF4A3A, mu, mv, 0.8f);
                // A cable bridge from the hut to the mast.
                float hu = (h[0] + h[2]) / 2;
                f.line(0xFF6A6E72, 1.6f, hu, h[1], hu, mv + 8);
                f.line(0xFF6A6E72, 1.6f, hu, mv + 8, mu + 6, mv + 8);
                for (float v = mv + 10; v < h[1]; v += 5) f.circle(0xFF4A4E52, hu, v, 0.8f);
                // A generator, a rose bed and trees.
                unit(f, h[0] - 20, h[1] + 4, h[0] - 6, h[1] + 12, 0xFF4E7A5A);
                f.tree(4 + 4, 8, 6);
                f.tree(L - 8, 8, 7, 0xFF3E7236);
                f.bush(s[0] + 4, s[3] + 14, 3);
                f.lamp(s[0] + 8, S - 3);
                f.lamp(s[0] + 28, S - 3);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                if (part == 0) {
                    deck(f, 0xFF9E9A92);
                    // A glazed stair core, dishes on stands, aircon and the ON AIR lamp.
                    unit(f, 4, 4, 14, 12, 0xFF8A8682);
                    glass(f, 5, 5, 12.6f, 10.6f);
                    dish(f, L - 10, 9, 5);
                    dish(f, L - 22, 8, 3.5f);
                    dish(f, L * 0.5f, S - 10, 4);
                    fan(f, 10, S - 10, 2.5f);
                    fan(f, 18, S - 10, 2.5f);
                    f.line(0xFF5A5E62, 0.8f, 20, 8, L - 26, 8);
                    f.rect(0xFF3A3A3E, L - 14, S - 7, L - 4, S - 3);
                    f.rect(0xFFE83A2A, L - 13.2f, S - 6.2f, L - 4.8f, S - 3.8f);
                    f.rect(0xFFFF8A6A, L - 12.6f, S - 5.8f, L - 9, S - 4.6f);
                    f.sign(city, "RADIO", L * 0.42f, S * 0.45f, 5, 0xFFF4EEDC);
                } else {
                    deck(f, 0xFF8E9290);
                    fan(f, L / 2, S / 2, 3);
                    vent(f, 5, 5, 1.2f);
                }
            }
        };
        d.minS = 10;
        d.minL = 14;
        d.ground = City.GRASS;
        d.base = City.OFFICE;
        d.inside = Variants.I_OFFICE;
        d.roofCol = 0xFF9E9A92;
        d.districts = Variants.MID | Variants.SUB | Variants.IND;
        out.add(d);
    }

    // ================================================================== 7 Newspaper Building

    static void newspaper(List<Landmarks.Def> out) {
        // An art deco office with a stepped clock tower on its front and the paper's name on the roof, joined to a
        // sawtooth press hall; delivery vans backed into the bays, bundles on pallets, reels of newsprint stacked
        // by the hall, and paper boxes by the front door.
        Landmarks.Def d = new Landmarks.Def("Newspaper Building", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh), ow = Math.max(4, L * 2 / 5);
                return new int[][]{at(al, 1, 1, ow, Math.min(S - 4, 8), 4), at(al, ow + 3, 1, L - ow - 4, Math.max(4, S * 11 / 20), 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] q = pl(this, x0, y0, x1, y1);
                float[] o = bx(f, q[0]), h = bx(f, q[1]);
                f.rect(0xFF6E7074, 0, 0, L, S);
                for (float u = 0; u < L; u += 24) f.line(0x10FFFFFF, 0.5f, u, 0, u, S);
                // Paved frontage before the office with paper boxes and planters.
                f.rect(0xFFCEC6B4, 0, o[3], o[2] + 8, S);
                joints(f, 0x16000000, 0, o[3], o[2] + 8, S, 8);
                float mu = (o[0] + o[2]) / 2;
                steps(f, mu - 9, o[3], mu + 9, o[3] + 4, 2, 0xFFDAD2C0);
                int[] bc = {0xFF2A5AA0, 0xFFC83A2A, 0xFFE0C040, 0xFF2A7A4A};
                for (int i = 0; i < 4; i++) unit(f, mu + 12 + i * 4.5f, o[3] + 2, mu + 15.5f + i * 4.5f, o[3] + 5.5f, bc[i]);
                f.tree(o[0] + 4, S - 6, 5);
                f.tree(o[2] - 2, S - 6, 5);
                // The loading bays: yellow dock bumpers, vans backed in, and pallets of bundles.
                int n = Math.max(2, (int) ((h[2] - h[0]) / 18));
                float bw = (h[2] - h[0]) / n;
                for (int i = 0; i < n; i++) {
                    float u = h[0] + bw * (i + 0.5f);
                    f.rect(0xFF3A3A3E, u - 5, h[3], u + 5, h[3] + 1.4f);
                    f.rect(0xFFE0B830, u - 6.4f, h[3], u - 5, h[3] + 2.4f);
                    f.rect(0xFFE0B830, u + 5, h[3], u + 6.4f, h[3] + 2.4f);
                    f.line(0xFFE8E2C8, 0.6f, u - bw / 2, h[3] + 3, u - bw / 2, h[3] + 26);
                    if (h[3] + 22 < S - 2 && i % 3 != 2) {
                        veh(f, u, h[3] + 12, 19, 8, false, true, i % 2 == 0 ? 0xFFF0EEE6 : 0xFF2A4A7A);
                        f.rect(0xFFC83A2A, u - 4, h[3] + 6, u + 3.4f, h[3] + 7.4f);
                    } else if (h[3] + 14 < S) {
                        for (int j = 0; j < 4; j++) unit(f, u - 4 + (j % 2) * 4.2f, h[3] + 5 + (j / 2) * 4.2f, u - 0.2f + (j % 2) * 4.2f, h[3] + 8.8f + (j / 2) * 4.2f, 0xFFE4DECE);
                    }
                }
                // Reels of newsprint stacked at the far end.
                float ru = h[2] + 6;
                if (ru + 6 < L) for (int i = 0; i < 4; i++) {
                    float v = h[1] + 6 + i * 9;
                    if (v + 5 > h[3]) break;
                    f.circle(0x38000000, ru + 1.4f, v + 1.4f, 4);
                    f.circle(0xFFE8E2D2, ru, v, 4);
                    f.ring(0x20000000, 0.4f, ru, v, 2.8f);
                    f.circle(0xFF8A6A48, ru, v, 1.2f);
                }
                f.lamp(h[0] - 8, S - 4);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                if (part == 0) {
                    deck(f, 0xFFC8BCA4);
                    // Deco setbacks: two stepped frames and a clock tower in the middle with a face on each side.
                    f.frame(0xFFB0A48C, 1.2f, 4, 4, L - 4, S - 4);
                    f.frame(0xFFDAD0BA, 0.6f, 7, 7, L - 7, S - 7);
                    float mu = L / 2, mv = S / 2;
                    unit(f, mu - 10, mv - 10, mu + 10, mv + 10, 0xFFD8CCB2);
                    unit(f, mu - 7.5f, mv - 7.5f, mu + 7.5f, mv + 7.5f, 0xFFE4DAC4);
                    hip(f, 0xFF4E7A6A, mu - 5.5f, mv - 5.5f, mu + 5.5f, mv + 5.5f, false);
                    f.circle(0xFFD8B048, mu, mv, 1);
                    for (int k = 0; k < 4; k++) {
                        float cu = mu + (k == 0 ? 10 : k == 1 ? -10 : 0), cv = mv + (k == 2 ? 10 : k == 3 ? -10 : 0);
                        f.circle(0x40000000, cu + 0.8f, cv + 0.8f, 3.4f);
                        f.circle(0xFF3A3A3A, cu, cv, 3.4f);
                        f.circle(0xFFF4F0E2, cu, cv, 2.8f);
                        f.line(0xFF1A1A1A, 0.5f, cu, cv, cu, cv - 2.2f);
                        f.line(0xFF1A1A1A, 0.5f, cu, cv, cu + 1.6f, cv + 0.6f);
                    }
                    fan(f, 10, 10, 2.4f);
                    fan(f, L - 10, 10, 2.4f);
                    f.sign(city, "THE DAILY", mu, S * 0.2f, 5, 0xFF2A2A2A);
                } else {
                    f.rect(0xFF6A6A6E, 0, 0, L, S);
                    // Sawtooth roof: glazed north lights and grey slopes.
                    float st = 10;
                    for (float u = 0; u + st <= L + 0.1f; u += st) {
                        f.rect(0xFF9A9A9C, u + 0.5f, 0.6f, u + st - 3, S - 0.6f);
                        f.rect(0xFF7A7C80, u + st * 0.5f, 0.6f, u + st - 3, S - 0.6f);
                        f.rect(0xFF8AAEC0, u + st - 3, 0.6f, u + st - 0.5f, S - 0.6f);
                        f.line(0x40000000, 0.5f, u + st - 0.5f, 0.6f, u + st - 0.5f, S - 0.6f);
                    }
                    for (float v = 8; v < S - 4; v += 16) vent(f, L / 2 - 2, v, 2);
                }
            }
        };
        d.minS = 10;
        d.minL = 14;
        d.ground = City.LOT;
        d.base = City.OFFICE;
        d.inside = Variants.I_FACTORY;
        d.roofCol = 0xFFC8BCA4;
        d.districts = Variants.DOWN | Variants.MID | Variants.IND;
        out.add(d);
    }

    // ================================================================== 8 Telephone Exchange

    static void exchange(List<Landmarks.Def> out) {
        // A blank brick block with no windows, its roof crowded with condensers, cable trays, a lift room and a
        // microwave mast with drum antennas; in a fenced yard a generator, a fuel tank, cable drums, manholes and
        // two engineers' vans.
        Landmarks.Def d = new Landmarks.Def("Telephone Exchange", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, 2, 2, L - 5, S - 6, 4)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] h = bx(f, pl(this, x0, y0, x1, y1)[0]);
                f.rect(0xFF8E8E8A, 0, 0, L, S);
                // Grass verges round the outside, the gravel yard within the fence.
                f.rect(0xFF7A9A58, 0, 0, L, 8);
                f.rect(0xFF7A9A58, 0, 0, 8, S);
                f.rect(0xFFA8A296, 10, 10, L - 4, S - 4);
                for (int i = 0; i < (int) (L * S / 120); i++) f.circle(0x18000000, 10 + r.nextFloat() * (L - 14), 10 + r.nextFloat() * (S - 14), 0.6f);
                fence(f, 10, 10, L - 4, S - 4, 0xFF7A7E80);
                f.frame(0x50202020, 0.3f, 9.2f, 9.2f, L - 3.2f, S - 3.2f);
                for (float u = 20; u < L - 10; u += 30) f.tree(u, 4, 4.5f, 0xFF4A7A3A);
                // The yard: generator, fuel tank, cable drums, manholes, vans.
                float yv = h[3] + 4, yv1 = S - 6;
                float gu = h[0] + 6;
                unit(f, gu, yv, gu + 26, Math.min(yv1, yv + 11), 0xFF4E6E5A);
                for (float u = gu + 3; u < gu + 22; u += 3) f.line(0x40000000, 0.5f, u, yv + 2, u, Math.min(yv1, yv + 11) - 3);
                f.roundRect(0x38000000, gu + 32, yv + 2, gu + 52, yv + 10, 4);
                f.roundRect(0xFFD8D4C8, gu + 30, yv, gu + 50, yv + 8, 4);
                f.rect(0xFFF2EEE4, gu + 32, yv + 1.2f, gu + 48, yv + 3);
                drum(f, gu + 60, yv + 5, 4);
                drum(f, gu + 70, yv + 6, 3.4f);
                float vu = h[2] - 14;
                if (vu > gu + 80) {
                    veh(f, vu, yv + 5 > yv1 - 4 ? yv1 - 4 : yv + 5, 15, 7, true, false, 0xFFF0EEE6);
                    f.rect(0xFF6A3A9A, vu - 4, yv + 3, vu + 4, yv + 4.2f);
                }
                if (h[2] - 34 > gu + 80) veh(f, h[2] - 34, yv + 5 > yv1 - 4 ? yv1 - 4 : yv + 5, 15, 7, true, false, 0xFFE8D040);
                for (float u = 30; u < L - 20; u += 46) {
                    f.circle(0xFF4A4A4C, u, S - 8, 2.4f);
                    f.ring(0xFF6E6E70, 0.4f, u, S - 8, 1.6f);
                }
                // Gate and ducts along the side.
                f.rect(0xFF8E8E8A, L - 4, h[1], L, h[1] + 20);
                f.line(0xFFE0B830, 1, L - 4, h[1], L - 4, h[1] + 20);
                f.rect(0xFF8A8E92, h[2] + 2, h[1] + 6, h[2] + 5, h[3] - 6);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                // Brick parapet with stone coping, a dark felt deck.
                f.rect(0xFF8A4A36, 0, 0, L, S);
                f.frame(0xFFC8BEA8, 0.8f, 0.6f, 0.6f, L - 0.6f, S - 0.6f);
                f.rect(0xFF4E4E50, 2.2f, 2.2f, L - 2.2f, S - 2.2f);
                for (float u = 12; u < L - 4; u += 24) f.line(0x18FFFFFF, 0.4f, u, 2.2f, u, S - 2.2f);
                // Cable trays crossing the deck.
                f.rect(0xFF8A9096, 6, S / 2 - 1, L - 6, S / 2 + 1);
                f.rect(0xFF8A9096, L * 0.35f - 1, 6, L * 0.35f + 1, S - 6);
                // Rows of condensers.
                for (float u = 8; u < L * 0.35f - 6; u += 9) {
                    fan(f, u, 8, 2.4f);
                    if (S > 30) fan(f, u, S - 8, 2.4f);
                }
                // Lift motor room and hatch.
                unit(f, L * 0.45f, 5, L * 0.45f + 14, S / 2 - 3, 0xFF9A5A44);
                unit(f, L * 0.45f + 18, 6, L * 0.45f + 24, 12, 0xFF6E7276);
                // The microwave mast: a lattice square with drum antennas.
                float mu = L * 0.78f, mv = S * 0.62f;
                f.tall(mu, mv, 2, 14);
                f.frame(0xFF9A9EA2, 0.8f, mu - 3, mv - 3, mu + 3, mv + 3);
                f.line(0xFF9A9EA2, 0.5f, mu - 3, mv - 3, mu + 3, mv + 3);
                f.line(0xFF9A9EA2, 0.5f, mu + 3, mv - 3, mu - 3, mv + 3);
                f.circle(0xFFE4E4E0, mu - 5, mv, 2.2f);
                f.circle(0xFFE4E4E0, mu + 1, mv - 5.4f, 2.6f);
                f.circle(0xFFE4E4E0, mu + 5, mv + 2, 2);
                f.line(0xFFE0E0DC, 1.2f, mu - 3, mv + 5, mu + 3, mv + 7);
                vent(f, L - 6, 6, 1.4f);
                vent(f, 6, S - 6, 1.4f);
            }
        };
        d.minS = 10;
        d.minL = 12;
        d.ground = City.LOT;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_RACKS;
        d.roofCol = 0xFF8A4A36;
        d.districts = Variants.DOWN | Variants.MID | Variants.IND | Variants.OLD;
        out.add(d);
    }

    // ================================================================== 9 Bus Garage

    static void busGarage(List<Landmarks.Def> out) {
        // A huge running shed under north-light roof bays with smoke vents; out in the yard a drive-through bus wash
        // gantry with big blue brushes and a bus in the spray, a fuel island and buses parked nose out.
        Landmarks.Def d = new Landmarks.Def("Bus Garage", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, 1, 1, Math.max(6, L * 3 / 5), S - 4, 2)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] h = bx(f, pl(this, x0, y0, x1, y1)[0]);
                f.rect(0xFF5E6064, 0, 0, L, S);
                for (int i = 0; i < 18; i++) f.circle(0x14000000, r.nextFloat() * L, r.nextFloat() * S, 2 + r.nextFloat() * 4);
                // A footway and planting along the front of the shed.
                f.rect(0xFFB8B2A6, 0, h[3] + 1, h[2] + 2, h[3] + 7);
                for (float u = 10; u < h[2]; u += 22) f.tree(u, S - 8 > h[3] + 14 ? S - 8 : h[3] + 12, 5, 0xFF4A7A3A);
                // Door aprons at the shed end, with yellow lane lines out into the yard.
                int lanes = Math.max(2, (int) ((h[3] - h[1]) / 18));
                float lw = (h[3] - h[1]) / lanes;
                for (int i = 0; i < lanes; i++) {
                    float v = h[1] + lw * (i + 0.5f);
                    f.rect(0xFFE0B830, h[2], v - lw / 2 + 1, h[2] + 1.4f, v - lw / 2 + 3);
                    f.line(0x50E8D060, 0.6f, h[2] + 2, v - lw / 2, L - 4, v - lw / 2);
                }
                // The yard: a wash gantry, a bus in it, a fuel island and parked buses.
                float yu0 = h[2] + 4, yu1 = L - 3;
                F g = sub(f, yu0, 3, yu1, S - 3);
                float gl = g.L, gs = g.S;
                // Wash gantry across the middle of the yard's long side.
                float wu = gl * 0.35f;
                g.rect(0x40FFFFFF, wu - 22, 2, wu + 22, gs - 2);
                g.rect(0x30A8D0E8, wu - 22, gs * 0.5f - 10, wu + 22, gs * 0.5f + 10);
                bus(g, wu, gs * 0.5f, true, true, 0xFFC83A2A);
                g.rect(0x50000000, wu - 3, gs * 0.5f - 11, wu + 5, gs * 0.5f + 13);
                g.rect(0xFF8A9096, wu - 4, gs * 0.5f - 12, wu + 4, gs * 0.5f - 10);
                g.rect(0xFF8A9096, wu - 4, gs * 0.5f + 10, wu + 4, gs * 0.5f + 12);
                g.rect(0xFF6E747A, wu - 4, gs * 0.5f - 12, wu - 2, gs * 0.5f + 12);
                g.rect(0xFF6E747A, wu + 2, gs * 0.5f - 12, wu + 4, gs * 0.5f + 12);
                for (int s = -1; s <= 1; s += 2) {
                    g.circle(0xFF2A5AA8, wu + 8, gs * 0.5f + s * 6.4f, 2.6f);
                    g.circle(0xFF5A8AD8, wu + 7.6f, gs * 0.5f + s * 6.4f - 0.4f, 1.4f);
                }
                for (int k = 0; k < 8; k++) g.circle(0x60FFFFFF, wu + 14 + (k % 4) * 2.4f, gs * 0.5f - 4 + (k / 4) * 8, 0.6f);
                // Buses parked across the far end, nose out, and a fuel island.
                float pu = gl * 0.72f;
                if (gs > 60) {
                    bus(g, pu, gs * 0.28f, true, false, 0xFFE0C040);
                    bus(g, pu, gs * 0.72f, true, false, 0xFFC83A2A);
                } else {
                    bus(g, pu + 4, gs * 0.5f, true, false, 0xFFE0C040);
                }
                if (gl > 110) {
                    float fu = gl - 10;
                    g.rect(0xFFB8B2A6, fu - 4, gs * 0.3f, fu + 4, gs * 0.7f);
                    for (float v = gs * 0.36f; v < gs * 0.68f; v += 9) unit(g, fu - 2, v, fu + 2, v + 4, 0xFFD8D4C8);
                }
                f.lamp(h[2] + 4, h[1] - 6 > 2 ? h[1] - 6 : 3);
                f.lamp(L - 4, S - 4);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF5A5E62, 0, 0, L, S);
                // North-light bays running across the shed, a smoke vent on each.
                float st = 14;
                for (float u = 1; u + st <= L; u += st) {
                    f.rect(0xFFA0A4A6, u, 1, u + st - 4, S - 1);
                    f.rect(0xFF868A8E, u + (st - 4) * 0.5f, 1, u + st - 4, S - 1);
                    f.rect(0xFF8CB0C2, u + st - 4, 1, u + st - 0.6f, S - 1);
                    for (float v = 3; v < S - 2; v += 3) f.line(0x28000000, 0.3f, u + st - 4, v, u + st - 0.6f, v);
                    unit(f, u + 2, S / 2 - 3, u + 6, S / 2 + 3, 0xFF6E7276);
                }
                f.sign(city, "BUS GARAGE", L / 2, 6, 5, 0xFFF4EEDC);
            }
        };
        d.minS = 10;
        d.minL = 15;
        d.ground = City.LOT;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_RACKS;
        d.roofCol = 0xFF8A8E90;
        d.districts = Variants.IND | Variants.MID;
        out.add(d);
    }

    // ================================================================== 10 Ambulance Station

    static void ambulance(List<Landmarks.Def> out) {
        // A low station with red roller doors and a star of life on its roof; ambulances stand nose out on a hatched
        // apron, a rapid response car beside them; the crew have a patio with a barbecue and a picnic table.
        Landmarks.Def d = new Landmarks.Def("Ambulance Station", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, 1, 1, Math.max(6, L * 13 / 20), Math.max(4, S * 9 / 20), 2)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] h = bx(f, pl(this, x0, y0, x1, y1)[0]);
                f.rect(0xFF76A054, 0, 0, L, S);
                // The apron, a driveway to the street with keep-clear hatching, and a lawn with a hedge.
                float av = Math.min(S - 4, h[3] + 30);
                f.rect(0xFF66686C, h[0] - 4, h[3], h[2] + 4, av);
                f.rect(0xFF66686C, h[0] + 6, av - 1, h[0] + 34, S);
                f.frame(0xFFE0C040, 0.8f, h[0] + 6, S - 6, h[0] + 34, S - 1);
                for (float u = h[0] + 8; u < h[0] + 31; u += 4) f.line(0xFFE0C040, 0.6f, u, S - 1, u + 4, S - 6);
                if (S - av > 12) {
                    for (float u = h[0] + 42; u < L - 6; u += 7) f.bush(u, S - 5, 3.4f);
                    f.tree(h[0] + 46, av + 6, 5.5f, 0xFF4A7A3A);
                    f.tree(h[2] - 10, av + 6, 5.5f, 0xFF4A7A3A);
                }
                int n = Math.max(2, Math.min(4, (int) ((h[2] - h[0]) / 26)));
                float bw = (h[2] - h[0]) / n;
                for (int i = 0; i < n; i++) {
                    float u = h[0] + bw * (i + 0.5f);
                    f.rect(0xFFB83A2A, u - 7, h[3], u + 7, h[3] + 1.2f);
                    f.line(0xFFE8E4D0, 0.6f, u - bw / 2, h[3] + 2, u - bw / 2, av - 2);
                    if (i % 4 != 3) ambo(f, u, h[3] + 13, false, true);
                }
                f.line(0xFFE8E4D0, 0.6f, h[2], h[3] + 2, h[2], av - 2);
                f.rect(0x2A3A80B0, h[2] - bw + 4, av - 10, h[2] - 4, av - 3);
                hydrant(f, h[2] + 6, av - 4, 0xFFC8A030);
                // A rapid response car and the crew patio round the side.
                float cu = h[2] + 14;
                if (cu + 10 < L) {
                    f.rect(0xFFCEC6B4, h[2] + 6, h[1], L - 4, h[1] + 34);
                    joints(f, 0x14000000, h[2] + 6, h[1], L - 4, h[1] + 34, 6);
                    f.rect(0x34000000, cu, h[1] + 8, cu + 12, h[1] + 16);
                    f.rect(0xFF8A6A44, cu - 2, h[1] + 6, cu + 10, h[1] + 14);
                    f.rect(0xFF6E5034, cu - 2, h[1] + 3.6f, cu + 10, h[1] + 5);
                    f.rect(0xFF6E5034, cu - 2, h[1] + 15, cu + 10, h[1] + 16.4f);
                    f.circle(0xFF2A2A2A, cu + 20, h[1] + 10, 3);
                    f.circle(0xFF4A4A4A, cu + 19.6f, h[1] + 9.6f, 2);
                    f.circle(0xFFC85A2A, cu + 20, h[1] + 10, 0.8f);
                    car(f, cu + 4, h[1] + 26, true, 0xFFF2F2EC);
                    f.rect(0xFFE0C040, cu - 1, h[1] + 25.4f, cu + 9, h[1] + 26.6f);
                    f.tree(L - 8, S - 8 > h[1] + 60 ? S - 8 : 8, 6);
                }
                f.tree(5, S - 6, 5);
                f.bush(h[0] + 3, h[1] - 4 > 3 ? h[1] - 4 : 3, 2.4f);
                f.lamp(h[0] - 2, S - 3);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                deck(f, 0xFFD8D4CC);
                // Roller door heads along the front, a big white disc with the star of life.
                for (float u = 6; u < L - 6; u += 20) f.rect(0xFF9A3A2E, u, S - 3, u + 14, S - 1.4f);
                float m = Math.min(S * 0.32f, 16);
                f.circle(0x30000000, L * 0.3f + 1, S * 0.45f + 1, m + 1);
                f.circle(0xFFF4F4F0, L * 0.3f, S * 0.45f, m + 1);
                starOfLife(f, L * 0.3f, S * 0.45f, m * 0.8f);
                fan(f, L * 0.62f, 8, 2.4f);
                fan(f, L * 0.62f + 8, 8, 2.4f);
                unit(f, L - 16, S * 0.4f, L - 6, S * 0.4f + 8, 0xFFB4B0A8);
                f.line(0xFF5A5E62, 0.8f, L - 11, S * 0.4f, L - 11, 3);
                f.circle(0xFF5A5E62, L - 11, 3, 1);
                f.sign(city, "AMBULANCE", L * 0.68f, S * 0.7f, 4.5f, 0xFF2A4A8A);
            }
        };
        d.minS = 10;
        d.minL = 14;
        d.ground = City.GRASS;
        d.base = City.OFFICE;
        d.inside = Variants.I_FIRE;
        d.roofCol = 0xFFD8D4CC;
        d.districts = Variants.MID | Variants.SUB | Variants.DOWN;
        out.add(d);
    }

    /** A boat on a road trailer, bow toward +u: hull, deck, tubes (RIB) or cabin, the trailer and its wheels. */
    static void boat(F f, float u, float v, float len, float wid, int hull, boolean rib) {
        float a = len / 2, b = wid / 2;
        f.line(0xFF3A3C40, 0.8f, u + a, v, u + a + 6, v);
        f.circle(0xFF3A3C40, u + a + 6, v, 0.9f);
        f.rect(0xFF1E1E20, u - 3, v - b - 1.2f, u + 3, v - b + 0.2f);
        f.rect(0xFF1E1E20, u - 3, v + b - 0.2f, u + 3, v + b + 1.2f);
        f.poly(0x3A000000, u - a + 1.5f, v - b + 1.5f, u + a * 0.5f + 1.5f, v - b + 1.5f, u + a + 1.5f, v + 1.5f, u + a * 0.5f + 1.5f, v + b + 1.5f, u - a + 1.5f, v + b + 1.5f);
        if (rib) {
            f.poly(0xFF4A4E52, u - a, v - b, u + a * 0.5f, v - b, u + a, v, u + a * 0.5f, v + b, u - a, v + b);
            f.poly(hull, u - a + 1.4f, v - b + 1.4f, u + a * 0.45f, v - b + 1.4f, u + a - 2, v, u + a * 0.45f, v + b - 1.4f, u - a + 1.4f, v + b - 1.4f);
            f.rect(0xFFDAD6CC, u - a * 0.3f, v - b * 0.4f, u + a * 0.1f, v + b * 0.4f);
            f.rect(0xFF2A2C30, u - a - 1, v - 1.2f, u - a + 1.4f, v + 1.2f);
        } else {
            f.poly(hull, u - a, v - b, u + a * 0.5f, v - b, u + a, v, u + a * 0.5f, v + b, u - a, v + b);
            f.poly(0xFFE8E4DA, u - a + 1, v - b + 1, u + a * 0.45f, v - b + 1, u + a - 2, v, u + a * 0.45f, v + b - 1, u - a + 1, v + b - 1);
            unit(f, u - a * 0.4f, v - b * 0.6f, u + a * 0.25f, v + b * 0.6f, 0xFFE87A2A);
            f.rect(0xFF2A3A4A, u + a * 0.15f, v - b * 0.5f, u + a * 0.22f, v + b * 0.5f);
            f.circle(0xFFE8E4DA, u - a * 0.1f, v, 1);
        }
    }

    // ================================================================== 11 Coast Guard Station

    static void coastGuard(List<Landmarks.Def> out) {
        // A white station house under a red roof and a slim lookout tower with a glazed watch room and radar; on the
        // yard orange RIBs and a lifeboat on trailers, a tow truck, a signal mast with flags, life rings and coiled
        // rope, with marram grass in the sand along the edge.
        Landmarks.Def d = new Landmarks.Def("Coast Guard Station", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh), mw = Math.max(5, L / 2);
                return new int[][]{at(al, 1, 1, mw, Math.max(4, S / 2), 2), at(al, mw + 3, 1, 2, 2, 5)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] q = pl(this, x0, y0, x1, y1);
                float[] h = bx(f, q[0]), t = bx(f, q[1]);
                f.rect(0xFFD8C8A0, 0, 0, L, S);
                for (int i = 0; i < 30; i++) f.circle(0x18806040, r.nextFloat() * L, r.nextFloat() * S, 1 + r.nextFloat() * 3);
                // Concrete hardstanding for the boats.
                float yv0 = h[3] + 4, yv1 = S - 6;
                f.rect(0xFFB4B0A6, h[0] - 4, h[3], L - 6, yv1);
                joints(f, 0x18000000, h[0] - 4, h[3], L - 6, yv1, 12);
                // Boats on trailers in a row along the yard.
                float rowv = (yv0 + yv1) / 2;
                float u = h[0] + 18;
                int k = 0;
                while (u + 24 < L - 8) {
                    boolean rib = k % 3 != 1;
                    float bl = rib ? 26 : 34, bwid = rib ? 10 : 12;
                    if (u + bl / 2 + 7 > L - 8) break;
                    boat(f, u, rowv + (k % 2 == 0 ? -1 : 2), bl, bwid, rib ? 0xFFE8722A : 0xFF1E3A6A, rib);
                    u += bl + 12;
                    k++;
                }
                // A tow truck by the station door.
                if (yv1 - yv0 > 40) veh(f, h[0] + 14, yv1 - 8, 18, 8, true, true, 0xFFF0EEE8);
                // Signal mast with a yardarm and flags beside the tower.
                float mu = t[2] + 14, mv = t[1] + 10;
                if (mu + 10 < L) {
                    f.tall(mu, mv, 0.8f, 18);
                    f.line(0xFF5A5E62, 0.8f, mu - 8, mv, mu + 8, mv);
                    int[] fc = {0xFFE8D040, 0xFFC83A2A, 0xFF2A4A9A, 0xFFF2F2EE};
                    for (int i = 0; i < 4; i++) f.rect(fc[i], mu - 7 + i * 4, mv + 0.6f, mu - 4.6f + i * 4, mv + 3.4f);
                    f.circle(0xFFE8E4DA, mu, mv, 1);
                }
                // Life rings on posts, a rope coil, a bench.
                for (int i = 0; i < 2; i++) {
                    float lu = h[2] + 6 + i * 10, lv = h[3] + 2;
                    f.ring(0xFFE8622A, 1.2f, lu, lv, 2);
                    f.circle(0xFFF2F2EE, lu + 2, lv, 0.6f);
                }
                f.ring(0xFFB8A070, 0.8f, h[0] + 4, h[3] + 4, 2);
                f.ring(0xFFB8A070, 0.8f, h[0] + 4, h[3] + 4, 1);
                // Marram grass along the back and side.
                for (float uu = 4; uu < L - 2; uu += 5) {
                    float vv = S - 3 + (float) Math.sin(uu) * 1.2f;
                    f.line(0xFF8A9A5A, 0.5f, uu, vv, uu - 1.5f, vv - 2.5f);
                    f.line(0xFF9AAA6A, 0.5f, uu, vv, uu + 1.4f, vv - 2.2f);
                }
                for (float vv = 6; vv < S - 6; vv += 6) {
                    f.line(0xFF8A9A5A, 0.5f, L - 3, vv, L - 5, vv - 2);
                    f.line(0xFF9AAA6A, 0.5f, L - 3, vv, L - 1.5f, vv - 2.2f);
                }
                f.lamp(h[0] - 2, h[3] + 2);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                if (part == 0) {
                    f.rect(0xFFE8E4DA, 0, 0, L, S);
                    hip(f, 0xFFB03A2E, 1.4f, 1.4f, L - 1.4f, S - 1.4f, true);
                    unit(f, L * 0.25f - 2.5f, S * 0.3f - 2, L * 0.25f + 2.5f, S * 0.3f + 2, 0xFFF0ECE2);
                    f.circle(0xFF5A5E62, L * 0.7f, S * 0.35f, 1);
                    f.line(0xFF5A5E62, 0.5f, L * 0.7f, S * 0.35f, L * 0.7f + 6, S * 0.35f);
                    f.sign(city, "COAST GUARD", L / 2, S * 0.6f, 4.5f, 0xFFF4F2EC);
                } else {
                    // The tower: white shaft, a glazed watch room and a rotating radar bar.
                    float cu = L / 2, cv = S / 2;
                    f.rect(0xFFD8D4CA, 0, 0, L, S);
                    f.rect(0xFFF2F0EA, 0.8f, 0.8f, L - 0.8f, S - 0.8f);
                    f.rect(0xFF3A4A5A, 2.4f, 2.4f, L - 2.4f, S - 2.4f);
                    f.rect(0xFF7AA8C0, 3, 3, L - 3, S * 0.45f);
                    f.rect(0xFFB03A2E, 5, 5, L - 5, S - 5);
                    f.rect(0xFFC84A3A, 5, 5, cu, cv);
                    f.circle(0xFF2A2A2A, cu, cv, 1.2f);
                    f.rect(0xFF2A2A2A, cu - 9, cv - 0.8f, cu + 9, cv + 0.8f);
                    f.rect(0x40000000, cu - 8, cv + 1.4f, cu + 10, cv + 2.6f);
                }
            }
        };
        d.minS = 10;
        d.minL = 14;
        d.ground = City.SAND;
        d.base = City.OFFICE;
        d.inside = Variants.I_OFFICE;
        d.roofCol = 0xFFB03A2E;
        d.districts = Variants.SUB | Variants.PARK | Variants.IND | Variants.MID;
        out.add(d);
    }

    // ================================================================== 12 Blood Bank & Clinic

    static void bloodBank(List<Landmarks.Def> out) {
        // A bright clinic with a red blood drop on its roof and a glass entrance canopy, joined by a link to a lab and
        // storage wing crowded with chillers; a bloodmobile parked on the forecourt, a courier van at the lab door,
        // flower beds in red and white, and benches for the donors.
        Landmarks.Def d = new Landmarks.Def("Blood Bank & Clinic", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh), cw = Math.max(5, L / 2);
                return new int[][]{at(al, 1, 1, cw, Math.max(4, S / 2), 2), at(al, cw + 3, 1, L - cw - 4, Math.max(3, S / 2 - 1), 2)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] q = pl(this, x0, y0, x1, y1);
                float[] h = bx(f, q[0]), b = bx(f, q[1]);
                f.rect(0xFF76A054, 0, 0, L, S);
                // A glazed link between the two wings.
                f.rect(0xFF8AA8B8, h[2], h[1] + 10, b[0], h[1] + 22);
                f.frame(0xFF6A7A84, 0.6f, h[2], h[1] + 10, b[0], h[1] + 22);
                // Forecourt and the bloodmobile.
                f.rect(0xFFD4CEC2, h[0], h[3], b[2], h[3] + 26 < S - 6 ? h[3] + 26 : S - 6);
                joints(f, 0x12000000, h[0], h[3], b[2], h[3] + 26 < S - 6 ? h[3] + 26 : S - 6, 8);
                float mu = (h[0] + h[2]) / 2;
                f.rect(0xFFD4CEC2, mu - 7, h[3], mu + 7, S);
                f.rect(0x6090B8CC, mu - 12, h[3], mu + 12, h[3] + 8);
                f.frame(0xFF7A8A94, 0.6f, mu - 12, h[3], mu + 12, h[3] + 8);
                float bv = h[3] + 16;
                if (bv + 5 < S - 6) {
                    bus(f, h[2] - 24, bv, true, true, 0xFFF4F2EE);
                    f.circle(0xFFC82A2A, h[2] - 30, bv - 1, 2.2f);
                    f.poly(0xFFC82A2A, h[2] - 32, bv - 2, h[2] - 28, bv - 2, h[2] - 30, bv - 5.4f);
                    f.rect(0xFFC82A2A, h[2] - 41, bv + 2.6f, h[2] - 9, bv + 3.4f);
                }
                // The courier van at the lab door.
                float lu = (b[0] + b[2]) / 2;
                if (b[3] + 14 < S - 4) {
                    veh(f, lu, b[3] + 10, 15, 7, false, true, 0xFFF4F2EE);
                    f.rect(0xFFC82A2A, lu - 3.5f, b[3] + 6, lu + 2.9f, b[3] + 7);
                }
                // Red and white flower beds and benches.
                float fv = S - 6;
                for (float u = 6; u < L - 6; u += 30) {
                    if (Math.abs(u + 10 - mu) < 18) continue;
                    f.roundRect(0xFF6A4E34, u, fv - 3, u + 20, fv + 3, 2);
                    for (float k = u + 2; k < u + 19; k += 2.4f) f.circle(((int) k) % 2 == 0 ? 0xFFD03A3A : 0xFFF2EEE8, k, fv + ((int) k % 3 - 1), 1);
                }
                f.bench(mu - 18, h[3] + 4, true);
                f.bench(mu + 18, h[3] + 4, true);
                f.tree(L - 6, S - 14 > b[3] + 20 ? S - 14 : 6, 6);
                f.tree(4, h[1] + 10, 4);
                f.lamp(mu - 9, S - 3);
                f.lamp(mu + 9, S - 3);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                if (part == 0) {
                    deck(f, 0xFFECEAE4);
                    // The drop: a red teardrop on a white disc.
                    float m = Math.min(S * 0.3f, 14), cu = L * 0.32f, cv = S * 0.52f;
                    f.circle(0x24000000, cu + 1, cv + 1, m + 1);
                    f.circle(0xFFFAFAF8, cu, cv, m + 1);
                    f.circle(0xFFC82A2A, cu, cv + m * 0.2f, m * 0.55f);
                    f.poly(0xFFC82A2A, cu - m * 0.5f, cv, cu + m * 0.5f, cv, cu, cv - m * 0.8f);
                    f.circle(0xFFE86A6A, cu - m * 0.2f, cv + m * 0.05f, m * 0.14f);
                    for (float u = L * 0.55f; u < L - 8; u += 10) glass(f, u, 4, u + 6, S / 2 - 2);
                    fan(f, L - 8, S - 8, 2.4f);
                    f.sign(city, "BLOOD BANK", L * 0.7f, S * 0.75f, 4.5f, 0xFFB02A2A);
                } else {
                    deck(f, 0xFFC4C2BC);
                    // Rows of chillers and two exhaust stacks.
                    for (float u = 5; u < L - 5; u += 7.5f) fan(f, u + 2, 7, 2.4f);
                    if (S > 30) for (float u = 5; u < L * 0.5f; u += 7.5f) fan(f, u + 2, S - 7, 2.4f);
                    vent(f, L - 8, S - 8, 2);
                    vent(f, L - 16, S - 8, 2);
                    unit(f, L * 0.6f, S / 2, L * 0.6f + 10, S / 2 + 6, 0xFF6E8A6E);
                }
            }
        };
        d.minS = 10;
        d.minL = 14;
        d.ground = City.GRASS;
        d.base = City.OFFICE;
        d.inside = Variants.I_WARD;
        d.roofCol = 0xFFECEAE4;
        d.districts = Variants.MID | Variants.DOWN | Variants.SUB | Variants.UNI;
        out.add(d);
    }

    // ================================================================== 13 Recording Studio

    static void studio(List<Landmarks.Def> out) {
        // A converted brick warehouse whose roof carries a quilted floating acoustic deck and big silenced ducts; a
        // courtyard laid as a giant vinyl record, a black tour bus, road cases being unloaded, a graffiti wall, potted
        // palms and a little lounge cabin with a planted roof.
        Landmarks.Def d = new Landmarks.Def("Recording Studio", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, 1, 1, Math.max(6, L * 3 / 5), Math.min(S - 4, 7), 2), at(al, L - 4, S - 4, 3, 3, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] q = pl(this, x0, y0, x1, y1);
                float[] h = bx(f, q[0]), g = bx(f, q[1]);
                f.rect(0xFF8A8478, 0, 0, L, S);
                joints(f, 0x16000000, 0, 0, L, S, 10);
                // A graffiti wall along the back of the yard beyond the warehouse.
                f.rect(0xFF9A5A44, h[2] + 4, 1, L - 1, 4);
                int[] gc = {0xFFE04A8A, 0xFF3AB0D0, 0xFFE8D040, 0xFF6AC04A, 0xFF8A4AC8};
                for (float u = h[2] + 6; u < L - 3; u += 5) f.oval(gc[(int) u % 5], u, 1.4f, u + 6, 3.6f);
                // The vinyl record courtyard.
                float ru = (h[2] + L) / 2 + 2, rv = (4 + g[1]) / 2 + 2, R = Math.min((L - h[2]) / 2 - 6, (g[1] - 4) / 2 - 2);
                if (R > 10) {
                    f.circle(0x30000000, ru + 1.5f, rv + 1.5f, R);
                    f.circle(0xFF1C1C1E, ru, rv, R);
                    for (float k = R * 0.45f; k < R - 1; k += 2) f.ring(0xFF2E2E32, 0.5f, ru, rv, k);
                    f.circle(0xFFC83A2A, ru, rv, R * 0.33f);
                    f.circle(0xFFE8D8B0, ru - R * 0.1f, rv - R * 0.1f, R * 0.08f);
                    f.circle(0xFF1C1C1E, ru, rv, 0.9f);
                    f.line(0x30FFFFFF, 1, ru - R * 0.7f, rv - R * 0.4f, ru - R * 0.4f, rv - R * 0.7f);
                }
                // The tour bus along the front, road cases behind it.
                float bv = h[3] + 12;
                if (bv + 6 < S - 2) {
                    bus(f, h[0] + 30, bv, true, true, 0xFF1E1E22);
                    f.rect(0xFF6A3A9A, h[0] + 14, bv + 3.2f, h[0] + 44, bv + 4);
                    for (int i = 0; i < 5; i++) {
                        float u = h[0] + 54 + (i % 3) * 6, v = bv - 3 + (i / 3) * 6;
                        if (u + 5 > h[2]) break;
                        unit(f, u, v, u + 5, v + 4, 0xFF2A2A2C);
                        f.rect(0xFFB8BCC0, u + 0.4f, v + 0.4f, u + 1.2f, v + 3);
                    }
                    person(f, h[0] + 52, bv + 8, 0xFF2A2A2A);
                }
                // A bench and ashtray by the stage door, potted palms.
                f.bench(h[2] + 8, h[3] - 10, false);
                for (int i = 0; i < 3; i++) {
                    float u = h[2] + 6, v = h[1] + 8 + i * 14;
                    if (v > h[3] - 18) break;
                    f.circle(0xFF8A5A3A, u, v, 3);
                    for (int k = 0; k < 6; k++) {
                        double a = k * Math.PI / 3;
                        f.line(0xFF4A8A3A, 1, u, v, u + (float) Math.cos(a) * 5, v + (float) Math.sin(a) * 5);
                    }
                    f.circle(0xFF5A9A4A, u, v, 1.2f);
                }
                f.lamp(L - 3, g[1] - 4);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                if (part == 0) {
                    // Brick parapet with a stepped gable end, a felt deck and the floating acoustic box.
                    f.rect(0xFF8A4232, 0, 0, L, S);
                    f.rect(0xFF54504C, 2, 2, L - 2, S - 2);
                    float a0 = L * 0.12f, a1 = L * 0.62f;
                    unit(f, a0, 4, a1, S - 4, 0xFF8E8A84);
                    for (float u = a0 + 1; u < a1 - 2; u += 4)
                        for (float v = 5; v < S - 5.5f; v += 4) {
                            f.rect(0xFF9E9A94, u, v, u + 3.4f, v + 3.4f);
                            f.circle(0xFF7A7670, u + 1.7f, v + 1.7f, 0.4f);
                        }
                    // Silenced ducts from the plant to the box.
                    float pu = L * 0.78f;
                    unit(f, pu - 6, 5, pu + 6, 15, 0xFFA8ACB0);
                    f.circle(0xFF44484C, pu, 10, 3);
                    f.rect(0x36000000, a1 + 1, 9, pu - 5, 13);
                    f.rect(0xFFB4B8BC, a1, 8, pu - 6, 11.6f);
                    for (float u = a1 + 3; u < pu - 6; u += 3) f.line(0x30000000, 0.4f, u, 8, u, 11.6f);
                    glass(f, pu - 5, S - 12, pu + 5, S - 5);
                    f.sign(city, "STUDIOS", (a0 + a1) / 2, S / 2, 5, 0xFFF4EEDC);
                } else {
                    f.rect(0xFF8A6A48, 0, 0, L, S);
                    f.rect(0xFF6E8A48, 1.4f, 1.4f, L - 1.4f, S - 1.4f);
                    for (int i = 0; i < 14; i++) f.circle(i % 2 == 0 ? 0xFF86A050 : 0xFF5E7E40, 3 + r.nextFloat() * (L - 6), 3 + r.nextFloat() * (S - 6), 1.2f);
                    glass(f, L / 2 - 3, S / 2 - 3, L / 2 + 3, S / 2 + 3);
                }
            }
        };
        d.minS = 10;
        d.minL = 14;
        d.ground = City.PLAZA;
        d.base = City.OFFICE;
        d.inside = Variants.I_SEATS;
        d.roofCol = 0xFF8A4232;
        d.districts = Variants.MID | Variants.IND | Variants.DOWN | Variants.OLD;
        out.add(d);
    }

    // ================================================================== 14 Driving School Track

    static void drivingSchool(List<Landmarks.Def> out) {
        // A practice circuit of painted roads on grass: a loop with a cross street, a mini roundabout, a zebra
        // crossing, stop lines and a traffic light; a coned slalom, parking bays and a parallel-parking box, two
        // learner cars with L plates and a small portable office.
        Landmarks.Def d = new Landmarks.Def("Driving School Track", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, 1, S - 4, 4, 3, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] o = bx(f, pl(this, x0, y0, x1, y1)[0]);
                f.rect(0xFF7AA454, 0, 0, L, S);
                int asp = 0xFF56585C, w = 0xFFF0EEE6, yl = 0xFFE8C840;
                float u0 = 6, v0 = 6, u1 = L - 6, v1 = o[1] - 8, rw = 13;
                // The loop.
                f.roundRect(asp, u0, v0, u1, v1, 18);
                f.roundRect(0xFF7AA454, u0 + rw, v0 + rw, u1 - rw, v1 - rw, 8);
                f.roundRect(0xFF86AE5E, u0 + rw + 2, v0 + rw + 2, u1 - rw - 2, v1 - rw - 2, 6);
                // Dashed centre line on the straights.
                float cm = rw / 2;
                for (float u = u0 + 20; u < u1 - 22; u += 8) {
                    f.line(w, 0.6f, u, v0 + cm, u + 4, v0 + cm);
                    f.line(w, 0.6f, u, v1 - cm, u + 4, v1 - cm);
                }
                for (float v = v0 + 20; v < v1 - 22; v += 8) {
                    f.line(w, 0.6f, u0 + cm, v, u0 + cm, v + 4);
                    f.line(w, 0.6f, u1 - cm, v, u1 - cm, v + 4);
                }
                // A cross street through the middle with a mini roundabout.
                float mu = (u0 + u1) / 2, mv = (v0 + v1) / 2;
                f.rect(asp, mu - rw / 2, v0 + rw - 1, mu + rw / 2, v1 - rw + 1);
                if (u1 - u0 > 120) {
                    f.rect(asp, u0 + rw - 1, mv - rw / 2, u1 - rw + 1, mv + rw / 2);
                    f.circle(asp, mu, mv, rw);
                    f.circle(w, mu, mv, 3.4f);
                    f.ring(0xFFB8B4AA, 0.6f, mu, mv, 3.4f);
                    for (float u = u0 + rw + 4; u < mu - rw - 2; u += 8) f.line(w, 0.6f, u, mv, u + 4, mv);
                    for (float u = mu + rw + 2; u < u1 - rw - 4; u += 8) f.line(w, 0.6f, u, mv, u + 4, mv);
                    // A slalom of cones in the infield.
                    for (int i = 0; i < 5; i++) cone(f, u0 + rw + 10 + i * 9, mv - rw / 2 - 8 - (i % 2) * 4);
                    // Parallel parking box.
                    f.frame(yl, 0.6f, mu + rw + 6, mv + rw / 2 + 4, mu + rw + 32, mv + rw / 2 + 12);
                    cone(f, mu + rw + 6, mv + rw / 2 + 8);
                    cone(f, mu + rw + 32, mv + rw / 2 + 8);
                }
                for (float v = v0 + rw + 4; v < mv - 6; v += 8) f.line(w, 0.6f, mu, v, mu, v + 4);
                for (float v = mv + 6; v < v1 - rw - 4; v += 8) f.line(w, 0.6f, mu, v, mu, v + 4);
                // Stop lines where the cross street meets the loop, and a traffic light.
                f.rect(w, mu - rw / 2, v0 + rw - 1, mu + rw / 2, v0 + rw + 0.4f);
                f.rect(w, mu - rw / 2, v1 - rw - 0.4f, mu + rw / 2, v1 - rw + 1);
                f.tall(mu + rw / 2 + 3, v0 + rw + 3, 0.6f, 6);
                f.rect(0xFF2A2A2C, mu + rw / 2 + 2, v0 + rw + 1, mu + rw / 2 + 4, v0 + rw + 6);
                f.circle(0xFFE83A2A, mu + rw / 2 + 3, v0 + rw + 2, 0.6f);
                f.circle(0xFF3AC84A, mu + rw / 2 + 3, v0 + rw + 4.6f, 0.6f);
                // A zebra crossing on the bottom straight.
                float zu = u0 + (u1 - u0) * 0.25f;
                for (float v = v1 - rw + 1; v < v1 - 1; v += 2.4f) f.rect(w, zu - 4, v, zu + 4, v + 1.2f);
                f.circle(0xFFE8A030, zu - 6, v1 + 2, 1);
                f.circle(0xFFE8A030, zu + 6, v1 + 2, 1);
                // Learner cars with L plates.
                car(f, u0 + (u1 - u0) * 0.62f, v0 + rw * 0.72f, true, 0xFFE8E4DA);
                f.rect(0xFFF2F2EE, u0 + (u1 - u0) * 0.62f + 4.4f, v0 + rw * 0.72f - 1, u0 + (u1 - u0) * 0.62f + 6.4f, v0 + rw * 0.72f + 1);
                f.rect(0xFFD02A2A, u0 + (u1 - u0) * 0.62f + 4.8f, v0 + rw * 0.72f - 0.8f, u0 + (u1 - u0) * 0.62f + 5.3f, v0 + rw * 0.72f + 0.8f);
                car(f, u1 - rw * 0.72f, v0 + (v1 - v0) * 0.6f, false, 0xFFC84A3A);
                f.rect(0xFFF2F2EE, u1 - rw * 0.72f - 1, v0 + (v1 - v0) * 0.6f + 4.4f, u1 - rw * 0.72f + 1, v0 + (v1 - v0) * 0.6f + 6.4f);
                // The front strip: a pad by the office with practice parking bays.
                f.rect(asp, o[2] + 8, o[1] - 2, L - 6, S - 4);
                for (float u = o[2] + 12; u < L - 10; u += 14) f.line(w, 0.6f, u, o[1], u, o[1] + 16);
                if (o[2] + 40 < L - 10) car(f, o[2] + 19, o[1] + 8, false, 0xFF4A7AB0);
                for (float u = o[2] + 14; u < L - 8; u += 10) cone(f, u, S - 7);
                f.rect(0xFFB8B2A4, o[0], o[3], o[2], o[3] + 4);
                f.bush(o[0] - 6 > 3 ? o[0] - 6 : 4, o[1] + 4, 3);
                f.tree(L - 4, S - 4, 4);
                f.lamp(o[2] + 4, o[1] - 4);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                // A portable cabin: ribbed steel roof, a sign.
                f.rect(0xFF8A9096, 0, 0, L, S);
                f.rect(0xFFB8BEC4, 0.8f, 0.8f, L - 0.8f, S - 0.8f);
                for (float u = 2; u < L - 1; u += 1.8f) f.line(0x22000000, 0.4f, u, 0.8f, u, S - 0.8f);
                fan(f, L - 6, S / 2, 2);
                f.rect(0xFFF2F2EE, 3, S / 2 - 3, 11, S / 2 + 3);
                f.rect(0xFFD02A2A, 5, S / 2 - 2, 6.4f, S / 2 + 2);
                f.rect(0xFFD02A2A, 5, S / 2 + 0.8f, 9, S / 2 + 2);
                f.sign(city, "DRIVING SCHOOL", L / 2, S / 2, 4, 0xFF2A2A2A);
            }
        };
        d.minS = 10;
        d.minL = 14;
        d.ground = City.GRASS;
        d.base = City.OFFICE;
        d.inside = Variants.I_CLASSROOM;
        d.roofCol = 0xFFB8BEC4;
        d.districts = Variants.SUB | Variants.IND | Variants.PARK;
        out.add(d);
    }

    // ================================================================== 15 Self-Storage Units

    static void storage(List<Landmarks.Def> out) {
        // Long rows of single-storey lock-ups with rainbow roller doors down both sides, a gated drive with a
        // barrier and keypad, a rental truck being loaded at an open unit, CCTV poles and a little office.
        Landmarks.Def d = new Landmarks.Def("Self-Storage Units", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int n = 0;
                for (int v = 1; v + 2 <= S - 3; v += 4) n++;
                int[][] q = new int[n + 1][];
                int i = 0;
                for (int v = 1; v + 2 <= S - 3; v += 4) {
                    boolean nearOffice = v + 2 > S - 4;
                    q[i++] = at(al, 1, v, nearOffice ? L - 8 : L - 3, 2, 1);
                }
                q[n] = at(al, L - 5, S - 4, 3, 3, 1);
                return q;
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] q = pl(this, x0, y0, x1, y1);
                f.rect(0xFF5E6064, 0, 0, L, S);
                for (int i = 0; i < 16; i++) f.circle(0x12000000, r.nextFloat() * L, r.nextFloat() * S, 3 + r.nextFloat() * 4);
                int[] dc = {0xFFD8562A, 0xFFE0B030, 0xFF3A7AB8, 0xFF4A9A4A, 0xFFB83A5A};
                for (int k = 0; k < q.length - 1; k++) {
                    float[] b = bx(f, q[k]);
                    int col = dc[k % 5];
                    int n = Math.max(2, (int) ((b[2] - b[0]) / 10));
                    float dw = (b[2] - b[0]) / n;
                    for (int i = 0; i < n; i++) {
                        float u = b[0] + i * dw;
                        boolean open = k == 0 && i == n / 2;
                        for (int s = 0; s < 2; s++) {
                            float va = s == 0 ? b[1] - 1.6f : b[3], vb = s == 0 ? b[1] : b[3] + 1.6f;
                            if (open && s == 1) {
                                f.rect(0xFF2A2A2C, u + 1, va, u + dw - 1, vb);
                                continue;
                            }
                            f.rect(col, u + 1, va, u + dw - 1, vb);
                            f.line(City.darken(col, 0.7f), 0.3f, u + 1, (va + vb) / 2, u + dw - 1, (va + vb) / 2);
                        }
                    }
                    // Kerb stones at the ends of the row.
                    f.rect(0xFFE0C040, b[2] + 1, b[1], b[2] + 2.4f, b[3]);
                    if (k == 0) {
                        float u = b[0] + (n / 2) * dw + dw / 2, v = b[3] + 10;
                        if (v + 4 < (k + 1 < q.length - 1 ? bx(f, q[k + 1])[1] : S)) {
                            veh(f, u, v, 20, 8, true, true, 0xFFF2EEE6);
                            f.rect(0xFFE8722A, u - 9, v - 3, u + 4, v - 2);
                            for (int j = 0; j < 3; j++) unit(f, u - 12 + j * 3.6f, b[3] + 2.4f, u - 9 + j * 3.6f, b[3] + 5.4f, 0xFFC8A878);
                        }
                    }
                }
                // Gate, barrier, keypad and CCTV.
                float[] o = bx(f, q[q.length - 1]);
                f.rect(0xFFB8B2A6, o[0] - 2, o[3] + 1, L, S);
                f.rect(0xFFE8E4DA, 4, S - 4, o[0] - 8, S - 2);
                for (float u = 6; u < o[0] - 8; u += 6) f.rect(0xFFC83A2A, u, S - 4, u + 3, S - 2);
                unit(f, o[0] - 8, S - 8, o[0] - 5, S - 5, 0xFF3A3C40);
                f.line(0xFF7A7E80, 0.6f, 0.6f, 0.6f, L - 0.6f, 0.6f);
                f.line(0xFF7A7E80, 0.6f, 0.6f, 0.6f, 0.6f, S - 0.6f);
                f.line(0xFF7A7E80, 0.6f, L - 0.6f, 0.6f, L - 0.6f, o[1] - 4);
                for (int s = 0; s < 2; s++) {
                    float cu = s == 0 ? 3 : L - 3, cv = 3;
                    f.tall(cu, cv, 0.6f, 10);
                    f.circle(0xFF3A3C40, cu, cv, 1.2f);
                    f.rect(0xFFE8E8E4, cu, cv - 0.6f, cu + 3, cv + 0.6f);
                }
                f.bush(o[2] + 1 < L - 3 ? o[2] + 1 : L - 4, o[1] - 6, 2.6f);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                boolean office = S > 2 * TT + 1;
                if (office) {
                    deck(f, 0xFFE4E0D4);
                    f.rect(0xFFE8722A, 1.4f, S - 5, L - 1.4f, S - 1.4f);
                    fan(f, L - 8, 8, 2.2f);
                    f.sign(city, "STORAGE", L / 2, S / 2, 4, 0xFF2A2A2A);
                    return;
                }
                int[] dc = {0xFFD8562A, 0xFFE0B030, 0xFF3A7AB8, 0xFF4A9A4A, 0xFFB83A5A};
                int col = dc[part % 5];
                f.rect(0xFF7A8086, 0, 0, L, S);
                gable(f, 0xFFB4BAC0, 0, 1.4f, L, S - 1.4f);
                for (float u = 1.5f; u < L; u += 1.8f) f.line(0x1E000000, 0.35f, u, 1.4f, u, S - 1.4f);
                // Coloured fascia over the doors on both long edges.
                f.rect(col, 0, 0, L, 1.4f);
                f.rect(City.darken(col, 0.8f), 0, S - 1.4f, L, S);
                for (float u = 10; u < L; u += 10) {
                    f.line(0x40000000, 0.4f, u, 0, u, 1.4f);
                    f.line(0x40000000, 0.4f, u, S - 1.4f, u, S);
                }
                for (float u = 14; u < L - 6; u += 28) vent(f, u, S / 2, 1.1f);
            }
        };
        d.minS = 10;
        d.minL = 14;
        d.ground = City.LOT;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_SHELVES;
        d.roofCol = 0xFFB4BAC0;
        d.districts = Variants.IND | Variants.SUB | Variants.MID;
        out.add(d);
    }

    static void laundromat(List<Landmarks.Def> out) {
    }

    static void carWash(List<Landmarks.Def> out) {
    }

    static void gasStation(List<Landmarks.Def> out) {
    }

    static void sorting(List<Landmarks.Def> out) {
    }

    static void mortuary(List<Landmarks.Def> out) {
    }

    static void pawn(List<Landmarks.Def> out) {
    }

    static void iceFactory(List<Landmarks.Def> out) {
    }

    static void fireTraining(List<Landmarks.Def> out) {
    }

    static void recruiting(List<Landmarks.Def> out) {
    }

    static void emergency(List<Landmarks.Def> out) {
    }

}
