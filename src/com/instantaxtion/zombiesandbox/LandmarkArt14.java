package com.instantaxtion.zombiesandbox;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.Landmarks.*;
import static com.instantaxtion.zombiesandbox.LandmarkArt12.F;
import static com.instantaxtion.zombiesandbox.LandmarkArt12.at;
import static com.instantaxtion.zombiesandbox.LandmarkArt12.bx;
import static com.instantaxtion.zombiesandbox.LandmarkArt12.pl;
import static com.instantaxtion.zombiesandbox.LandmarkArt12.person;
import static com.instantaxtion.zombiesandbox.LandmarkArt12.car;
import static com.instantaxtion.zombiesandbox.LandmarkArt12.CLOTHES;
import static com.instantaxtion.zombiesandbox.LandmarkArt13.paving;
import static com.instantaxtion.zombiesandbox.LandmarkArt13.pool;
import static com.instantaxtion.zombiesandbox.LandmarkArt13.palm;
import static com.instantaxtion.zombiesandbox.LandmarkArt13.plant;
import static com.instantaxtion.zombiesandbox.LandmarkArt13.crowd;

/**
 * ((26.0) Landmark designs, part 14 (see Landmarks): the Space Center's own - the launch pads with their rockets and
 * service towers, the Vehicle Assembly Building, the crawlerway, mission control, the labs and offices, the fuel farm
 * and the landing strip. None of them suits any district (districts 0), so no other map ever picks one:
 * City.spaceCenterPlaces puts each where it goes.
 */
final class LandmarkArt14 {
    private LandmarkArt14() {
    }

    static final float TT = City.T;

    static void register(List<Landmarks.Def> out) {
        int first = out.size();
        launchPad(out, "Launch Pad A", 0);
        launchPad(out, "Launch Pad B", 1);
        assemblyBuilding(out);
        crawlerway(out);
        missionControl(out);
        headquarters(out);
        propulsionLab(out);
        payloadProcessing(out);
        training(out);
        crewQuarters(out);
        visitorComplex(out);
        trackingStation(out);
        fuelFarm(out);
        landingStrip(out);
        // (Only ever placed by name, on the Space Center.)
        for (int i = first; i < out.size(); i++) out.get(i).districts = 0;
    }

    // ------------------------------------------------------------------ shared bits

    /** Florida scrub: the ground everything out at the space center stands in. */
    static final int SCRUB = 0xFF587840, SCRUB_DARK = 0xFF4C6A36, CONCRETE = 0xFFC8C4BA, CONCRETE_DARK = 0xFFA9A59C;

    /**
     * Drawing in a block turned so that its front (the side something comes in from) is at v = V: u runs along the
     * front (0..U), v from the back to the front (0..V). Shadows are cast in the world's own direction (to the lower
     * right), whichever way the block is turned.
     */
    static final class Q {
        final Canvas c;
        final Paint p;
        final float ox, oy, U, V;
        final int ux, uy, vx, vy;

        Q(Canvas c, Paint p, float x0, float y0, float x1, float y1, int front) {
            this.c = c;
            this.p = p;
            float W = x1 - x0, H = y1 - y0;
            switch (front) {
                case 2:
                    ux = -1; uy = 0; vx = 0; vy = -1; ox = x1; oy = y1; U = W; V = H;
                    break;
                case 1:
                    ux = 0; uy = -1; vx = 1; vy = 0; ox = x0; oy = y1; U = H; V = W;
                    break;
                case 0:
                    ux = 0; uy = 1; vx = -1; vy = 0; ox = x1; oy = y0; U = H; V = W;
                    break;
                default:
                    ux = 1; uy = 0; vx = 0; vy = 1; ox = x0; oy = y0; U = W; V = H;
                    break;
            }
        }

        /** The same frame as f's (u along the long side, v across, a tall block transposed). */
        Q(F f) {
            c = f.c;
            p = f.p;
            ox = f.ox;
            oy = f.oy;
            U = f.L;
            V = f.S;
            ux = f.t ? 0 : 1;
            uy = f.t ? 1 : 0;
            vx = f.t ? 1 : 0;
            vy = f.t ? 0 : 1;
        }

        float x(float u, float v) {
            return ox + u * ux + v * vx;
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

        float y(float u, float v) {
            return oy + u * uy + v * vy;
        }

        /** A world rectangle in this frame: {u0, v0, u1, v1}. */
        float[] inv(float wx0, float wy0, float wx1, float wy1) {
            float a0 = (wx0 - ox) * ux + (wy0 - oy) * uy, b0 = (wx0 - ox) * vx + (wy0 - oy) * vy;
            float a1 = (wx1 - ox) * ux + (wy1 - oy) * uy, b1 = (wx1 - ox) * vx + (wy1 - oy) * vy;
            return new float[]{Math.min(a0, a1), Math.min(b0, b1), Math.max(a0, a1), Math.max(b0, b1)};
        }

        void rect(int col, float u0, float v0, float u1, float v1) {
            float ax = x(u0, v0), ay = y(u0, v0), bx = x(u1, v1), by = y(u1, v1);
            Landmarks.rect(c, p, col, Math.min(ax, bx), Math.min(ay, by), Math.max(ax, bx), Math.max(ay, by));
        }

        void oval(int col, float u0, float v0, float u1, float v1) {
            float ax = x(u0, v0), ay = y(u0, v0), bx = x(u1, v1), by = y(u1, v1);
            Landmarks.oval(c, p, col, Math.min(ax, bx), Math.min(ay, by), Math.max(ax, bx), Math.max(ay, by));
        }

        void roundRect(int col, float u0, float v0, float u1, float v1, float r) {
            float ax = x(u0, v0), ay = y(u0, v0), bx = x(u1, v1), by = y(u1, v1);
            Landmarks.roundRect(c, p, col, Math.min(ax, bx), Math.min(ay, by), Math.max(ax, bx), Math.max(ay, by), r);
        }

        void frame(int col, float w, float u0, float v0, float u1, float v1) {
            float ax = x(u0, v0), ay = y(u0, v0), bx = x(u1, v1), by = y(u1, v1);
            Landmarks.frame(c, p, col, w, Math.min(ax, bx), Math.min(ay, by), Math.max(ax, bx), Math.max(ay, by));
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

        void tree(float u, float v, float r, int col) {
            Landmarks.tree(c, p, x(u, v), y(u, v), r, col);
        }

        void bush(float u, float v, float r) {
            Landmarks.bush(c, p, x(u, v), y(u, v), r);
        }

        void palm(float u, float v, float r) {
            Landmarks.circle(c, p, 0x30000000, x(u, v) + r * 0.5f, y(u, v) + r * 0.55f, r * 0.8f);
            City.desertTree(c, p, x(u, v), y(u, v), r, Country.TK_PALM, 1, u * 0.13f + v * 0.07f);
        }

        void person(float u, float v, int col) {
            Landmarks.circle(c, p, 0x30000000, x(u, v) + 0.8f, y(u, v) + 0.8f, 1.6f);
            Landmarks.circle(c, p, col, x(u, v), y(u, v), 1.6f);
            Landmarks.circle(c, p, 0xFF3A2A20, x(u, v), y(u, v), 0.85f);
        }

        /** The shadow of a box d high over u0..u1, v0..v1, cast to the world's lower right. */
        void sh(float u0, float v0, float u1, float v1, float d) {
            float ax = x(u0, v0), ay = y(u0, v0), bx = x(u1, v1), by = y(u1, v1);
            float x0 = Math.min(ax, bx), y0 = Math.min(ay, by), x1 = Math.max(ax, bx), y1 = Math.max(ay, by);
            Landmarks.poly(c, p, 0x38000000, x0, y0, x1, y0, x1 + d * 0.6f, y0 + d * 0.8f, x1 + d * 0.6f, y1 + d * 0.8f,
                    x0 + d * 0.6f, y1 + d * 0.8f, x0, y1);
        }

        /** The long shadow of something tall and round (radius r, len long) standing at u, v. */
        void tall(float u, float v, float r, float len, int col) {
            float X = x(u, v), Y = y(u, v);
            p.setStrokeCap(Paint.Cap.ROUND);
            Landmarks.line(c, p, col, r * 2, X, Y, X + len * 0.6f, Y + len * 0.8f);
            p.setStrokeCap(Paint.Cap.BUTT);
        }

        void sign(City city, String s, float u, float v, float size, int col) {
            Landmarks.sign(city, s, x(u, v), y(u, v), size, col);
        }
    }

    /**
     * Which side of the block (0 left, 1 right, 2 top, 3 bottom) has ground of kind t just outside it (within reach
     * tiles of the middle of that side), or -1.
     */
    static int front(City city, float x0, float y0, float x1, float y1, byte t, int reach) {
        if (city == null || city.tiles == null) return -1;
        int tx0 = Math.round(x0 / TT), ty0 = Math.round(y0 / TT), tx1 = Math.round(x1 / TT), ty1 = Math.round(y1 / TT);
        int mx = (tx0 + tx1) / 2, my = (ty0 + ty1) / 2;
        for (int d = 1; d <= reach; d++) {
            int[][] pts = {{tx0 - d, my}, {tx1 - 1 + d, my}, {mx, ty0 - d}, {mx, ty1 - 1 + d}};
            for (int k = 0; k < 4; k++) {
                int x = pts[k][0], y = pts[k][1];
                if (x >= 0 && y >= 0 && x < city.w && y < city.h && city.tiles[y * city.w + x] == t) return k;
            }
        }
        return -1;
    }

    /** Scrubby grass with darker patches. */
    static void scrub(Q f, Random r) {
        f.rect(SCRUB, 0, 0, f.U, f.V);
        int n = (int) (f.U * f.V / 900) + 2;
        for (int k = 0; k < n; k++) {
            float u = r.nextFloat() * f.U, v = r.nextFloat() * f.V, s = 6 + r.nextFloat() * 14;
            f.oval(r.nextBoolean() ? SCRUB_DARK : 0xFF66864A, u - s, v - s * 0.7f, u + s, v + s * 0.7f);
        }
    }

    /** Concrete in slabs, joints every step. */
    static void slabs(Q f, int col, float u0, float v0, float u1, float v1, float step) {
        f.rect(col, u0, v0, u1, v1);
        for (float u = u0 + step; u < u1; u += step) f.line(0x18000000, 0.5f, u, v0, u, v1);
        for (float v = v0 + step; v < v1; v += step) f.line(0x14000000, 0.5f, u0, v, u1, v);
    }

    /** A worker in a hi-vis vest. */
    static void worker(Q f, float u, float v) {
        f.person(u, v, (((int) (u * 7 + v * 3)) & 1) == 0 ? 0xFFF08A20 : 0xFFE8E040);
    }

    /**
     * A rocket standing on its pad, seen from right above: its long shadow, the fins, the boosters strapped on, the
     * white body with its interstage ring and the nose cone catching the light. kind 0 a heavy rocket with two
     * boosters, 1 a crew rocket with its capsule and escape tower, 2 a small display rocket.
     */
    static void rocket(Q f, float u, float v, float R, int kind, float shadow) {
        // The shadow: the body, the boosters, the fins, all thrown long across the ground.
        f.tall(u, v, R, shadow, 0x40000000);
        if (kind == 0) {
            f.tall(u, v - R * 1.45f, R * 0.55f, shadow * 0.8f, 0x30000000);
            f.tall(u, v + R * 1.45f, R * 0.55f, shadow * 0.8f, 0x30000000);
        }
        float fin = R * (kind == 2 ? 0.9f : 0.75f);
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4 + k * Math.PI / 2;
            float eu = u + (float) Math.cos(a) * (R + fin), ev = v + (float) Math.sin(a) * (R + fin);
            f.poly(kind == 2 ? 0xFFC83A30 : 0xFF2A2A30, u + (float) Math.cos(a - 0.4f) * R * 0.95f, v + (float) Math.sin(a - 0.4f) * R * 0.95f, eu, ev,
                    u + (float) Math.cos(a + 0.4f) * R * 0.95f, v + (float) Math.sin(a + 0.4f) * R * 0.95f);
        }
        if (kind == 0) {
            for (int s = -1; s <= 1; s += 2) {
                float bv = v + s * R * 1.45f, br = R * 0.55f;
                f.rect(0xFF6A6A70, u - R * 0.12f, Math.min(bv, v), u + R * 0.12f, Math.max(bv, v));
                f.circle(0xFF8E8C86, u, bv, br + 0.8f);
                f.circle(0xFFEDEBE4, u - 0.4f, bv - 0.4f, br);
                f.ring(0xFF2A2A30, Math.max(0.6f, br * 0.08f), u, bv, br * 0.7f);
                f.circle(0xFFF8F8F4, u - br * 0.25f, bv - br * 0.25f, br * 0.4f);
                f.circle(0xFFFFFFFF, u - br * 0.35f, bv - br * 0.35f, br * 0.15f);
            }
        }
        // The body: a grey rim, the white skin, the black interstage, then the nose cone.
        f.circle(0xFF8E8C86, u, v, R + 1);
        f.circle(0xFFF0EEE8, u - 0.5f, v - 0.5f, R);
        if (kind == 0) {
            // (The roll pattern: black quarters round the top of the first stage.)
            for (int k = 0; k < 4; k += 2) {
                double a0 = k * Math.PI / 2;
                Path q = new Path();
                q.moveTo(f.x(u, v), f.y(u, v));
                for (int j = 0; j <= 6; j++) {
                    double a = a0 + j * Math.PI / 12;
                    q.lineTo(f.x(u + (float) Math.cos(a) * R, v + (float) Math.sin(a) * R), f.y(u + (float) Math.cos(a) * R, v + (float) Math.sin(a) * R));
                }
                q.close();
                f.p.setStyle(Paint.Style.FILL);
                f.p.setColor(0xFF26262A);
                f.c.drawPath(q, f.p);
            }
        }
        f.ring(0xFF2A2A30, R * 0.12f, u, v, R * 0.78f);
        f.circle(0xFFE4E2DA, u, v, R * 0.66f);
        f.circle(0xFFF6F4EE, u - R * 0.12f, v - R * 0.12f, R * 0.48f);
        if (kind == 1) {
            // The crew capsule (dark heat shield ring), the escape tower and its red tip.
            f.circle(0xFF3A3C42, u, v, R * 0.5f);
            f.circle(0xFFD8D6D0, u - 0.3f, v - 0.3f, R * 0.38f);
            f.tall(u, v, R * 0.1f, R * 3.2f, 0x50000000);
            f.circle(0xFF5A5C62, u, v, R * 0.18f);
            f.circle(0xFFD83A2E, u, v, R * 0.09f);
        } else {
            f.circle(0xFFFFFFFF, u - R * 0.25f, v - R * 0.25f, R * 0.2f);
            f.circle(kind == 2 ? 0xFFC83A30 : 0xFF3A3C42, u, v, R * 0.08f);
        }
    }

    /** A satellite dish from above, aimed up and to the south: its pedestal's shadow, the bowl, the feed on its struts. */
    static void dish(Q f, float u, float v, float R) {
        f.tall(u, v, R * 0.25f, R * 1.4f, 0x30000000);
        f.oval(0x38000000, u - R + R * 0.5f, v - R * 0.85f + R * 0.7f, u + R + R * 0.5f, v + R * 0.85f + R * 0.7f);
        f.circle(0xFF7A7C80, u, v + R * 0.3f, R * 0.35f);
        f.oval(0xFF9A9C9E, u - R, v - R * 0.86f, u + R, v + R * 0.86f);
        f.oval(0xFFF2F2EE, u - R * 0.93f, v - R * 0.8f, u + R * 0.93f, v + R * 0.8f);
        f.oval(0xFFDCDCD6, u - R * 0.7f, v - R * 0.58f, u + R * 0.75f, v + R * 0.66f);
        f.oval(0xFFC8C8C2, u - R * 0.35f, v - R * 0.25f, u + R * 0.45f, v + R * 0.4f);
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4 + k * Math.PI / 2;
            f.line(0xFF6A6C70, Math.max(0.5f, R * 0.05f), u + (float) Math.cos(a) * R * 0.8f, v + (float) Math.sin(a) * R * 0.7f, u, v - R * 0.1f);
        }
        f.circle(0xFF4A4C50, u, v - R * 0.1f, R * 0.12f);
    }

    /** A radome: a white geodesic ball on its plinth. */
    static void radome(Q f, float u, float v, float R) {
        f.circle(0x38000000, u + R * 0.45f, v + R * 0.55f, R);
        f.circle(0xFF9A9C9E, u, v, R + 1);
        f.circle(0xFFE8E8E2, u, v, R);
        f.circle(0xFFF6F6F2, u - R * 0.25f, v - R * 0.25f, R * 0.6f);
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3;
            f.line(0x22000000, 0.5f, u, v, u + (float) Math.cos(a) * R, v + (float) Math.sin(a) * R);
        }
        f.ring(0x22000000, 0.5f, u, v, R * 0.55f);
        f.circle(0xFFFFFFFF, u - R * 0.4f, v - R * 0.4f, R * 0.15f);
    }

    /** A spherical tank from above: its shadow, the legs, the shaded ball. */
    static void sphere(Q f, float u, float v, float R, int col) {
        f.circle(0x38000000, u + R * 0.5f, v + R * 0.62f, R);
        f.circle(City.darken(col, 0.62f), u, v, R + 0.6f);
        f.circle(City.darken(col, 0.82f), u, v, R);
        f.circle(col, u - R * 0.15f, v - R * 0.15f, R * 0.78f);
        f.circle(City.lighten(col, 0.25f), u - R * 0.35f, v - R * 0.35f, R * 0.38f);
        f.circle(0xFFFFFFFF, u - R * 0.45f, v - R * 0.45f, R * 0.12f);
        f.rect(0xFF6A6C70, u - R * 0.15f, v - R * 1.02f, u + R * 0.15f, v - R * 0.7f);
    }

    /** A horizontal tank lying along u. */
    static void cylinder(Q f, float u0, float u1, float v, float R, int col) {
        f.rect(0x38000000, u0 + R * 0.5f, v - R + R * 0.6f, u1 + R * 0.5f, v + R + R * 0.6f);
        f.roundRect(City.darken(col, 0.7f), u0, v - R, u1, v + R, R);
        f.roundRect(col, u0 + 0.6f, v - R + 0.6f, u1 - 0.6f, v + R * 0.5f, R * 0.8f);
        f.rect(City.lighten(col, 0.25f), u0 + R * 0.6f, v - R * 0.55f, u1 - R * 0.6f, v - R * 0.3f);
        for (float u = u0 + R * 1.5f; u < u1 - R; u += R * 3) f.line(0x30000000, 0.5f, u, v - R, u, v + R);
    }

    /** A little parking lot of cars (kept small), across v0..v1 from u0 to u1. */
    static void parking(Q f, Random r, float u0, float v0, float u1, float v1) {
        f.rect(0xFF5A5C60, u0, v0, u1, v1);
        float mid = (v0 + v1) / 2;
        for (float u = u0 + 2; u < u1 - 6; u += 7) {
            f.line(0xFFE8E8E0, 0.5f, u, v0 + 1, u, mid - 2);
            f.line(0xFFE8E8E0, 0.5f, u, mid + 2, u, v1 - 1);
            if (r.nextFloat() < 0.55f) carQ(f, u + 3.5f, (v0 + mid) / 2, CLOTHES[r.nextInt(8)]);
            if (r.nextFloat() < 0.45f) carQ(f, u + 3.5f, (mid + v1) / 2, CLOTHES[r.nextInt(8)]);
        }
    }

    /** A car parked nose out across the lot (along v). */
    static void carQ(Q f, float u, float v, int col) {
        f.rect(0x38000000, u - 2 + 0.8f, v - 4 + 0.8f, u + 2 + 0.8f, v + 4 + 0.8f);
        f.roundRect(col, u - 2, v - 4, u + 2, v + 4, 1.2f);
        f.rect(0xFF2A3440, u - 1.6f, v - 2, u + 1.6f, v - 0.6f);
        f.rect(0xFF2A3440, u - 1.6f, v + 1.4f, u + 1.6f, v + 2.4f);
    }

    /** Flagpoles in a row along u, the flags blowing towards +u. */
    static void flags(Q f, float u, float v, int n) {
        int[] cols = {0xFF2A4A8A, 0xFFC8382E, 0xFFF0EEE8, 0xFF2A6A4A};
        for (int k = 0; k < n; k++) {
            float uu = u + k * 9;
            f.circle(0x40000000, uu + 1, v + 1, 1);
            f.circle(0xFF8A8E96, uu, v, 0.9f);
            f.rect(cols[k % cols.length], uu, v - 1.5f, uu + 7, v + 2.5f);
            f.rect(0x30FFFFFF, uu, v - 1.5f, uu + 7, v - 0.5f);
        }
    }

    /** A plain flat roof with a parapet, plant boxes and a sign, in the block's own frame. */
    static void officeRoof(F f, int col, Random r, City city, String sign, int signCol) {
        float L = f.L, S = f.S;
        f.rect(City.darken(col, 0.7f), 0, 0, L, S);
        f.rect(col, 1.6f, 1.6f, L - 1.6f, S - 1.6f);
        f.line(City.lighten(col, 0.2f), 0.8f, 1.6f, 1.6f, L - 1.6f, 1.6f);
        int n = Math.max(1, (int) (L / 26));
        for (int k = 0; k < n; k++) plant(f, L * (k + 0.5f) / n - 3, S * 0.2f + r.nextFloat() * S * 0.2f, 5);
        f.rect(0xFF6A6E74, L - 6, S - 6, L - 3, S - 3);
        if (sign != null && L > 30) f.sign(city, sign, L / 2, S * 0.68f, Math.min(9, Math.max(4, S / 4)), signCol);
    }

    // ------------------------------------------------------------------ the launch pads

    static void launchPad(List<Landmarks.Def> out, final String name, final int kind) {
        // An octagon of concrete out in the scrub, fenced round: the crawlerway's ramp up onto it from the front, the
        // mobile launcher deck on top with the rocket standing on it, the service tower beside it with its swing arms,
        // the flame trench running out behind black with soot, the water tower for the sound suppression, the fuel
        // spheres at the sides and the lightning masts with their wires round it all.
        Landmarks.Def d = new Landmarks.Def(name, T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                // The tower beside the rocket in the middle of the block, off to one side across it.
                int ts = Math.max(2, Math.min(6, S / 9)), gap = Math.max(2, Math.min(5, S / 10));
                return new int[][]{at(al, L / 2 - ts / 2, S / 2 - gap - ts, ts, ts, 14)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                int fr = front(city, x0, y0, x1, y1, City.PLAZA, 3);
                if (fr < 0) fr = (x1 - x0) >= (y1 - y0) ? 3 : 1;
                Q f = new Q(c, p, x0, y0, x1, y1, fr);
                float U = f.U, V = f.V;
                int[] tw = pl(this, x0, y0, x1, y1)[0];
                float[] t = f.inv(x0 + tw[0] * TT, y0 + tw[1] * TT, x0 + (tw[0] + tw[2]) * TT, y0 + (tw[1] + tw[3]) * TT);
                float[] mid = f.inv((x0 + x1) / 2, (y0 + y1) / 2, (x0 + x1) / 2, (y0 + y1) / 2);
                float cu = mid[0], cv = mid[1];
                scrub(f, r);
                // The octagon of concrete, and the fence round it.
                float R = Math.min(U, V) * 0.43f;
                f.ngon(0xFF8A8A84, cu, cv, R + 10, 8, Math.PI / 8);
                f.ngon(SCRUB, cu, cv, R + 9, 8, Math.PI / 8);
                f.ngon(CONCRETE_DARK, cu, cv, R + 1.5f, 8, Math.PI / 8);
                f.ngon(CONCRETE, cu, cv, R, 8, Math.PI / 8);
                for (float k = -R + 16; k < R; k += 16) {
                    f.line(0x12000000, 0.5f, cu + k, cv - R * 0.92f, cu + k, cv + R * 0.92f);
                    f.line(0x0E000000, 0.5f, cu - R * 0.92f, cv + k, cu + R * 0.92f, cv + k);
                }
                // The crawlerway's ramp up from the front (lanes as wide as the crawlerway's), lighter as it climbs.
                float cw = 8 * TT, lw = cw * 0.36f, gap = cw * 0.16f;
                for (int s = -1; s <= 1; s += 2) {
                    float a0 = Math.min(cu + s * gap / 2, cu + s * (gap / 2 + lw)), a1 = a0 + lw;
                    f.rect(0xFFB8B0A0, a0, cv, a1, V);
                    f.rect(0xFFD8D0C0, a0 + 0.8f, cv, a1 - 0.8f, V);
                    f.rect(0x18FFFFFF, a0 + 0.8f, cv, a1 - 0.8f, cv + R * 0.5f);
                    for (int k = 0; k < 2; k++) {
                        float tu = a0 + lw * (k == 0 ? 0.22f : 0.68f);
                        f.rect(0x1E000000, tu, cv, tu + lw * 0.12f, V);
                    }
                }
                f.rect(0xFF8AA664, cu - gap / 2, cv + R * 0.75f, cu + gap / 2, V);
                // The rocket's size, and the mobile launcher's deck under it.
                float rr = kind == 0 ? Math.max(14, Math.min(50, R * 0.15f)) : Math.max(11, Math.min(38, R * 0.115f));
                float mw = rr * (kind == 0 ? 3.4f : 3.8f), mh = mw * 0.84f;
                // The flame trench out the back: a broad slot, the deflector in it, soot fanned out from its mouth.
                float tw2 = mw * 0.42f, tEnd = Math.max(4, cv - R * 0.72f);
                for (int k = 0; k < 6; k++) {
                    float s = 0.5f + k * 0.3f;
                    f.oval(0x14000000, cu - tw2 * s, tEnd - 6 - k * 5, cu + tw2 * s, tEnd + 10 + k * 3);
                }
                f.rect(0xFF3A3632, cu - tw2 / 2 - 3, tEnd, cu + tw2 / 2 + 3, cv);
                f.rect(0xFF1E1A16, cu - tw2 / 2, tEnd + 2, cu + tw2 / 2, cv);
                f.poly(0xFF6A4E38, cu - tw2 / 2, cv - mh * 0.3f, cu + tw2 / 2, cv - mh * 0.3f, cu, cv - mh * 0.3f - tw2 * 0.9f);
                f.poly(0xFF8E6E50, cu - tw2 / 2, cv - mh * 0.3f, cu, cv - mh * 0.3f, cu, cv - mh * 0.3f - tw2 * 0.9f);
                for (float v = tEnd + 5; v < cv - mh * 0.3f - tw2; v += 6) f.line(0x24FFFFFF, 0.6f, cu - tw2 / 2 + 2, v, cu + tw2 / 2 - 2, v);
                // The fuel: a big white sphere of hydrogen on one side, the oxygen's pale blue one on the other, their
                // pipes running in under the deck.
                float sr = Math.max(10, Math.min(40, R * 0.12f));
                float lu = cu - R * 0.6f, ru = cu + R * 0.6f, sv = cv + R * 0.18f;
                f.line(0xFF8A8C90, 2.4f, lu, sv, cu - mw / 2, sv);
                f.line(0xFF8A8C90, 2.4f, ru, sv, cu + mw / 2, sv);
                f.line(0xFF6A6C70, 1.2f, lu, sv + 4, cu - mw / 2, sv + 4);
                f.line(0xFF6A6C70, 1.2f, ru, sv + 4, cu + mw / 2, sv + 4);
                sphere(f, lu, sv, sr, 0xFFF2F2EE);
                sphere(f, ru, sv, sr * 0.85f, 0xFFB8D0E0);
                // The lightning masts at the corners, and their thin shadows.
                float[][] masts = {{cu - R * 0.62f, cv - R * 0.62f}, {cu + R * 0.62f, cv - R * 0.62f}, {cu - R * 0.62f, cv + R * 0.62f}, {cu + R * 0.62f, cv + R * 0.62f}};
                for (float[] m : masts) f.tall(m[0], m[1], 1.4f, R * 0.8f, 0x28000000);
                for (float[] m : masts) {
                    f.circle(0xFF5A5C60, m[0], m[1], 2.6f);
                    f.circle(0xFFD83A2E, m[0], m[1], 1.1f);
                }
                // The water tower for the sound suppression, on its legs, its long shadow.
                float wu = cu + R * 0.62f, wv = cv - R * 0.25f, wr = Math.max(8, sr * 0.7f);
                f.tall(wu, wv, wr * 0.9f, R * 0.75f, 0x30000000);
                for (int k = 0; k < 4; k++) {
                    double a = Math.PI / 4 + k * Math.PI / 2;
                    f.circle(0xFF6A6C70, wu + (float) Math.cos(a) * wr * 0.85f, wv + (float) Math.sin(a) * wr * 0.85f, 1.4f);
                }
                f.circle(0xFF8A8C90, wu, wv, wr + 0.8f);
                f.circle(0xFFDCDCD6, wu - 0.4f, wv - 0.4f, wr);
                f.circle(0xFFF2F2EE, wu - wr * 0.3f, wv - wr * 0.3f, wr * 0.4f);
                // The deck: its shadow, the plates, the exhaust hole, the yellow pedestals.
                f.sh(cu - mw / 2, cv - mh / 2, cu + mw / 2, cv + mh / 2, 10);
                f.rect(0xFF55575C, cu - mw / 2, cv - mh / 2, cu + mw / 2, cv + mh / 2);
                f.rect(0xFF74767B, cu - mw / 2 + 1.5f, cv - mh / 2 + 1.5f, cu + mw / 2 - 1.5f, cv + mh / 2 - 1.5f);
                for (float k = -mw / 2 + 7; k < mw / 2; k += 7) f.line(0x22000000, 0.5f, cu + k, cv - mh / 2 + 1.5f, cu + k, cv + mh / 2 - 1.5f);
                f.rect(0xFF141416, cu - rr * 1.1f, cv - rr * 1.1f, cu + rr * 1.1f, cv + rr * 1.1f);
                for (int s = -1; s <= 1; s += 2)
                    for (int e = -1; e <= 1; e += 2) f.rect(0xFFE0C040, cu + s * (mw / 2 - 6) - 3, cv + e * (mh / 2 - 6) - 3, cu + s * (mw / 2 - 6) + 3, cv + e * (mh / 2 - 6) + 3);
                // The tower's swing arms across to the rocket (and the crew's white room at the end of one).
                float tu = (t[0] + t[2]) / 2, tv = (t[1] + t[3]) / 2, dx = cu - tu, dy = cv - tv, dl = (float) Math.hypot(dx, dy);
                if (dl > 1) {
                    float nx = dx / dl, ny = dy / dl, ts = (t[2] - t[0]) / 2;
                    for (int k = -1; k <= 1; k += 2) {
                        float ou = -ny * k * ts * 0.45f, ov = nx * k * ts * 0.45f;
                        float au = tu + nx * ts + ou, av = tv + ny * ts + ov, bu = cu - nx * rr * 0.9f + ou * 0.6f, bv = cv - ny * rr * 0.9f + ov * 0.6f;
                        f.line(0x30000000, 4, au + 3, av + 3, bu + 3, bv + 3);
                        f.line(0xFF7A2018, 4, au, av, bu, bv);
                        f.line(0xFFC8402E, 2.4f, au, av, bu, bv);
                    }
                    if (kind == 1) {
                        float wu2 = cu - nx * rr * 1.25f, wv2 = cv - ny * rr * 1.25f;
                        f.rect(0xFF8A8A8A, wu2 - 5, wv2 - 5, wu2 + 5, wv2 + 5);
                        f.rect(0xFFF6F6F2, wu2 - 4, wv2 - 4, wu2 + 4, wv2 + 4);
                    }
                }
                // The rocket.
                rocket(f, cu, cv, rr, kind, R * (kind == 0 ? 1.3f : 1.1f));
                // The pad's name on the concrete by the ramp, workers about.
                f.sign(city, name.toUpperCase(), cu, cv + R * 0.62f, Math.max(5, Math.min(12, R / 9)), 0xFF3A3A3A);
                for (int k = 0; k < 6; k++) worker(f, cu - mw / 2 - 8 + r.nextFloat() * (mw + 16), cv + mh / 2 + 6 + r.nextFloat() * 14);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                // The service tower from above: a square red lattice, braced, the crane on top with its boom.
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF5A1E18, 0, 0, L, S);
                f.rect(0xFFB83A2E, 0.8f, 0.8f, L - 0.8f, S - 0.8f);
                f.rect(0xFF3A1410, 2.4f, 2.4f, L - 2.4f, S - 2.4f);
                for (float u = 2.4f; u < L - 2; u += 4) {
                    f.line(0xFFD84A3A, 0.6f, u, 2.4f, u + 4, S - 2.4f);
                    f.line(0xFFD84A3A, 0.6f, u + 4, 2.4f, u, S - 2.4f);
                }
                f.frame(0xFFE85A48, 0.6f, L * 0.3f, S * 0.3f, L * 0.7f, S * 0.7f);
                // The hammerhead crane and the lightning mast on top.
                f.rect(0xFF2A2A30, L * 0.15f, S * 0.45f, L * 0.85f, S * 0.55f);
                f.rect(0xFFE8C040, L * 0.75f, S * 0.4f, L * 0.88f, S * 0.6f);
                f.circle(0xFF5A5C60, L / 2, S / 2, Math.min(L, S) * 0.12f);
                f.circle(0xFFD83A2E, L / 2, S / 2, Math.min(L, S) * 0.05f);
            }
        };
        d.ground = City.LOT;
        d.minS = 22;
        d.minL = 24;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_FACTORY;
        d.roofCol = 0xFFB83A2E;
        d.accent = 0xFFF0EEE8;
        out.add(d);
    }

    // ------------------------------------------------------------------ the Vehicle Assembly Building

    static void assemblyBuilding(List<Landmarks.Def> out) {
        // The giant hangar the rockets are stacked in: a vast pale box, the flag and the agency's emblem painted huge
        // across its roof, the high bays' great doors at both ends, the launch control centre beside it, the concrete
        // apron out to the crawlerway with a mobile launcher parked on it.
        Landmarks.Def d = new Landmarks.Def("Vehicle Assembly Building", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int mu = Math.max(3, L / 5), mv = Math.max(2, S / 8);
                int lu = L - 2 * mu, lv = S - mv - Math.max(4, S / 4);
                int cs = Math.max(2, Math.min(6, S / 7));
                return new int[][]{at(al, mu, mv, lu, lv, 24), at(al, mu + Math.max(1, lu / 6), mv + lv + 2, Math.max(3, lu / 3), Math.min(cs, S - mv - lv - 3), 4)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] pq = pl(this, x0, y0, x1, y1);
                float[] b = bx(f, pq[0]), lcc = bx(f, pq[1]);
                f.rect(SCRUB, 0, 0, L, S);
                // The apron round it, out to both ends.
                f.rect(CONCRETE_DARK, 2, 2, L - 2, S - 2);
                f.rect(CONCRETE, 3, 3, L - 3, S - 3);
                for (float u = 3 + 16; u < L - 3; u += 16) f.line(0x14000000, 0.5f, u, 3, u, S - 3);
                for (float v = 3 + 16; v < S - 3; v += 16) f.line(0x10000000, 0.5f, 3, v, L - 3, v);
                // Which end the crawlerway comes in at: its lanes up to the doors there.
                int fr = front(city, x0, y0, x1, y1, City.PLAZA, 3);
                boolean far = f.t ? fr == 3 : fr == 1;
                float bv0 = b[1], bv1 = b[3], cvm = (bv0 + bv1) / 2;
                float lw = Math.min((bv1 - bv0) * 0.18f, 36), gap = lw * 0.45f;
                float e0 = far ? b[2] : 0, e1 = far ? L : b[0];
                for (int s = -1; s <= 1; s += 2) {
                    float a0 = cvm + s * gap / 2, a1 = a0 + s * lw;
                    f.rect(0xFFD8D0C0, e0, Math.min(a0, a1), e1, Math.max(a0, a1));
                    for (float u = e0 + 2; u < e1; u += 3.2f) f.line(0x18000000, 0.5f, u, Math.min(a0, a1), u + 0.6f, Math.max(a0, a1));
                }
                f.rect(0xFF8AA664, e0, cvm - gap / 2, e1, cvm + gap / 2);
                // A mobile launcher waiting on the crawlerway in front of the doors: its deck and its tower's long shadow.
                float mw = Math.min((bv1 - bv0) * 0.5f, 110), mh = mw * 0.8f;
                float mu0 = far ? b[2] + 8 : b[0] - 8 - mw;
                if ((far ? L - b[2] : b[0]) > mw + 14) {
                    float mv0 = cvm - mh / 2;
                    f.rect(0x38000000, mu0 + 5, mv0 + 6, mu0 + mw + 5, mv0 + mh + 6);
                    f.rect(0xFF5E6064, mu0, mv0, mu0 + mw, mv0 + mh);
                    f.rect(0xFF7A7C80, mu0 + 1.2f, mv0 + 1.2f, mu0 + mw - 1.2f, mv0 + mh - 1.2f);
                    f.rect(0xFF1A1A1E, mu0 + mw * 0.42f, mv0 + mh * 0.38f, mu0 + mw * 0.62f, mv0 + mh * 0.62f);
                    float tw = mw * 0.24f, tu0 = mu0 + mw * 0.1f, tv0 = mv0 + mh * 0.1f;
                    f.poly(0x33000000, tu0, tv0, tu0 + tw, tv0, tu0 + tw + 70, tv0 + 90, tu0 + 70, tv0 + tw + 90, tu0, tv0 + tw);
                    f.rect(0xFF5A1E18, tu0, tv0, tu0 + tw, tv0 + tw);
                    f.rect(0xFFB83A2E, tu0 + 0.8f, tv0 + 0.8f, tu0 + tw - 0.8f, tv0 + tw - 0.8f);
                    f.line(0xFFD84A3A, 0.6f, tu0, tv0, tu0 + tw, tv0 + tw);
                    f.line(0xFFD84A3A, 0.6f, tu0 + tw, tv0, tu0, tv0 + tw);
                }
                // By the launch control centre: a few cars, flagpoles, palms.
                float pu0 = lcc[2] + 6, pu1 = Math.min(b[2], pu0 + 60);
                if (pu1 - pu0 > 20 && S - 4 - lcc[1] > 14) parking(qOf(f, c, p, x0, y0, x1, y1), r, pu0, lcc[1], pu1, Math.min(S - 4, lcc[1] + 18));
                for (float u = b[0] + 4; u < b[2]; u += 30) {
                    palm(f, u, 6, 5);
                }
                for (int k = 0; k < 6; k++) person(f, lcc[0] + r.nextFloat() * (lcc[2] - lcc[0]), lcc[3] + 3 + r.nextFloat() * 5, CLOTHES[r.nextInt(8)]);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                if (part == 1) {
                    // The launch control centre: a low block, its firing room's slanted windows along the front.
                    officeRoof(f, 0xFFDCDAD2, r, null, null, 0);
                    f.rect(0xFF4A6A8A, 2, S - 4, L - 2, S - 1.6f);
                    for (float u = 4; u < L - 2; u += 4) f.line(0xFF8AB0D0, 0.5f, u, S - 4, u, S - 1.6f);
                    f.sign(city, "LAUNCH CONTROL", L / 2, S * 0.45f, Math.min(6, S / 3), 0xFF3A4A5A);
                    return;
                }
                // The vast roof: a parapet, pale panels in a grid, rows of plant.
                f.rect(0xFF8E8C86, 0, 0, L, S);
                f.rect(0xFFE6E4DE, 2, 2, L - 2, S - 2);
                for (float u = 2 + 24; u < L - 2; u += 24) f.line(0x16000000, 0.6f, u, 2, u, S - 2);
                for (float v = 2 + 24; v < S - 2; v += 24) f.line(0x12000000, 0.6f, 2, v, L - 2, v);
                f.line(0xFFF8F8F4, 1, 2, 2.5f, L - 2, 2.5f);
                // The high bays' doors at each end: tall slots, panelled, in the dark of the bay.
                for (int e = 0; e < 2; e++) {
                    for (int k = 0; k < 2; k++) {
                        float v0 = S * (0.14f + k * 0.44f), v1 = v0 + S * 0.28f;
                        float u0 = e == 0 ? 0 : L - 10, u1 = e == 0 ? 10 : L;
                        f.rect(0xFF3A3C40, u0, v0, u1, v1);
                        for (float v = v0 + 2.5f; v < v1; v += 2.5f) f.line(0xFF6A6C70, 0.5f, u0, v, u1, v);
                        f.rect(0xFFB8B6B0, e == 0 ? 10 : L - 11, v0 - 1, e == 0 ? 11 : L - 10, v1 + 1);
                    }
                }
                // The flag painted across one half of the roof, the emblem on the other.
                // (The flag the right way up whichever way the building lies: its stripes across the map.)
                float ax = f.x(L * 0.1f, S * 0.18f), ay = f.y(L * 0.1f, S * 0.18f), bx2 = f.x(L * 0.52f, S * 0.82f), by2 = f.y(L * 0.52f, S * 0.82f);
                float wx0 = Math.min(ax, bx2), wy0 = Math.min(ay, by2), wx1 = Math.max(ax, bx2), wy1 = Math.max(ay, by2);
                float fh = Math.min(wy1 - wy0, (wx1 - wx0) * 0.6f), fw = Math.min(wx1 - wx0, fh * 1.9f);
                wy0 = (wy0 + wy1) / 2 - fh / 2;
                wx0 = (wx0 + wx1) / 2 - fw / 2;
                wx1 = wx0 + fw;
                wy1 = wy0 + fh;
                for (int k = 0; k < 13; k++)
                    Landmarks.rect(c, p, k % 2 == 0 ? 0xFFB8322A : 0xFFF4F2EC, wx0, wy0 + fh * k / 13f, wx1, wy0 + fh * (k + 1) / 13f);
                float cx1 = wx0 + fw * 0.4f, cy1 = wy0 + fh * 7 / 13f;
                Landmarks.rect(c, p, 0xFF22386E, wx0, wy0, cx1, cy1);
                for (float x = wx0 + 3; x < cx1 - 1; x += Math.max(3, (cx1 - wx0) / 6))
                    for (float y = wy0 + 3; y < cy1 - 1; y += Math.max(3, (cy1 - wy0) / 5)) Landmarks.circle(c, p, 0xFFF4F2EC, x, y, 0.9f);
                Landmarks.frame(c, p, 0x30000000, 0.6f, wx0, wy0, wx1, wy1);
                // The space center's own emblem: a gold-ringed dark blue disc, a ring of stars, a white rocket rising.
                float eu = L * 0.74f, ev = S / 2, er = Math.min(S * 0.33f, L * 0.16f);
                f.circle(0xFF8E8C86, eu, ev, er + 1.5f);
                f.circle(0xFFD8A830, eu, ev, er);
                f.circle(0xFF15254E, eu, ev, er * 0.88f);
                for (int k = 0; k < 12; k++) {
                    double a = k * Math.PI / 6;
                    f.circle(0xFFF4F2EC, eu + (float) Math.cos(a) * er * 0.72f, ev + (float) Math.sin(a) * er * 0.72f, Math.max(0.8f, er * 0.05f));
                }
                float rh = er * 0.5f, rw = er * 0.13f;
                f.poly(0xFFF4F2EC, eu - rh, ev - rw, eu + rh * 0.55f, ev - rw, eu + rh, ev, eu + rh * 0.55f, ev + rw, eu - rh, ev + rw);
                f.poly(0xFFC83A2E, eu - rh, ev - rw, eu - rh * 0.6f, ev - rw, eu - rh * 0.95f, ev - rw * 2.6f);
                f.poly(0xFFC83A2E, eu - rh, ev + rw, eu - rh * 0.6f, ev + rw, eu - rh * 0.95f, ev + rw * 2.6f);
                f.poly(0xFFE8A040, eu - rh, ev - rw * 0.6f, eu - rh * 1.35f, ev, eu - rh, ev + rw * 0.6f);
                // Plant along the long sides, and the name.
                for (float u = L * 0.1f; u < L * 0.9f; u += 18) {
                    plant(f, u, 4, 5);
                    plant(f, u, S - 9, 5);
                }
                f.sign(city, "VEHICLE ASSEMBLY BUILDING", L / 2, S - 12, Math.min(10, Math.max(5, S / 12)), 0xFF3A3A3A);
            }
        };
        d.ground = City.LOT;
        d.minS = 16;
        d.minL = 18;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_FACTORY;
        d.roofCol = 0xFFE6E4DE;
        d.accent = 0xFF1E3C82;
        out.add(d);
    }

    /** A Q drawing in exactly F's frame (u along the block's long side, v across it). */
    static Q qOf(F f, Canvas c, Paint p, float x0, float y0, float x1, float y1) {
        return new Q(f);
    }

    // ------------------------------------------------------------------ the crawlerway

    static void crawlerway(List<Landmarks.Def> out) {
        // The road the rockets go out to the pads on: two broad lanes of river rock with grass between, the crawler's
        // tread marks along them, and now and then the crawler itself, a great flat machine on its four tracks.
        Landmarks.Def d = new Landmarks.Def("Crawlerway", T_PARK) {
            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(SCRUB, 0, 0, L, S);
                float lw = S * 0.36f;
                for (int s = 0; s < 2; s++) {
                    float v0 = s == 0 ? S * 0.06f : S * 0.58f, v1 = v0 + lw;
                    f.rect(0xFFB8B0A0, 0, v0, L, v1);
                    f.rect(0xFFD8D0C0, 0, v0 + 0.8f, L, v1 - 0.8f);
                    for (float u = 1; u < L; u += 2.2f)
                        f.circle((((int) (u * 3.1f)) % 3) == 0 ? 0x26000000 : 0x30FFFFFF, u, v0 + 1.5f + ((u * 7.3f) % (lw - 3)), 0.5f);
                    // The tread marks: two pairs down each lane.
                    for (int k = 0; k < 2; k++) {
                        float tv = v0 + lw * (k == 0 ? 0.22f : 0.68f);
                        f.rect(0x1E000000, 0, tv, L, tv + lw * 0.12f);
                        for (float u = 0; u < L; u += 2) f.line(0x16000000, 0.4f, u, tv, u, tv + lw * 0.12f);
                    }
                }
                // Marker posts down the grass between.
                for (float u = 6; u < L; u += 24) f.circle(0xFFE8E040, u, S / 2, 0.8f);
                // The crawler, on a long stretch.
                if (L > 30 * TT && r.nextFloat() < 0.6f) {
                    float cu = L * (0.3f + r.nextFloat() * 0.4f), cs = S * 0.92f, h = cs * 0.9f;
                    float u0 = cu - h / 2, v0 = (S - cs) / 2;
                    f.rect(0x40000000, u0 + 6, v0 + 7, u0 + h + 6, v0 + cs + 7);
                    for (int a = 0; a < 2; a++)
                        for (int b = 0; b < 2; b++) {
                            float tu = u0 + a * (h - h * 0.3f), tv = v0 + b * (cs - cs * 0.22f);
                            f.rect(0xFF2A2A2E, tu, tv, tu + h * 0.3f, tv + cs * 0.22f);
                            for (float u = tu + 1; u < tu + h * 0.3f; u += 1.6f) f.line(0xFF4A4A50, 0.5f, u, tv, u, tv + cs * 0.22f);
                        }
                    f.rect(0xFF6E7076, u0 + h * 0.06f, v0 + cs * 0.12f, u0 + h * 0.94f, v0 + cs * 0.88f);
                    f.rect(0xFF8E9096, u0 + h * 0.08f, v0 + cs * 0.14f, u0 + h * 0.92f, v0 + cs * 0.86f);
                    for (float v = v0 + cs * 0.2f; v < v0 + cs * 0.8f; v += 4) f.line(0x22000000, 0.5f, u0 + h * 0.1f, v, u0 + h * 0.9f, v);
                    f.rect(0xFFE8C040, u0 + h * 0.08f, v0 + cs * 0.14f, u0 + h * 0.2f, v0 + cs * 0.26f);
                    f.rect(0xFFE8C040, u0 + h * 0.8f, v0 + cs * 0.74f, u0 + h * 0.92f, v0 + cs * 0.86f);
                    f.rect(0xFF3A5A7A, u0 + h * 0.09f, v0 + cs * 0.16f, u0 + h * 0.17f, v0 + cs * 0.22f);
                    f.rect(0xFF3A5A7A, u0 + h * 0.83f, v0 + cs * 0.78f, u0 + h * 0.91f, v0 + cs * 0.84f);
                }
            }
        };
        d.ground = City.PLAZA;
        d.minS = 4;
        d.minL = 6;
        out.add(d);
    }

    // ------------------------------------------------------------------ the tech area

    static void missionControl(List<Landmarks.Def> out) {
        // The control centre: a long block with a glass atrium down it and dishes on its roof, out on the lawn a row of
        // big dish antennas, flagpoles and a fountain by the door, a small car park.
        Landmarks.Def d = new Landmarks.Def("Mission Control", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int lu = Math.max(8, Math.min(40, L * 2 / 5)), lv = Math.max(5, Math.min(20, S / 2));
                return new int[][]{at(al, 2, 2, lu, lv, 5)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                Q q = qOf(f, c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] b = bx(f, pl(this, x0, y0, x1, y1)[0]);
                f.rect(0xFF74A458, 0, 0, L, S);
                // Paths, the plaza at the door, the fountain, flags.
                paving(f, 0xFFDCD6C8, b[0] - 3, b[3], b[2] + 3, b[3] + 22, 6);
                f.rect(0xFFDCD6C8, (b[0] + b[2]) / 2 - 4, b[3] + 22, (b[0] + b[2]) / 2 + 4, S);
                f.circle(0xFFB8B4AA, (b[0] + b[2]) / 2, b[3] + 12, 7);
                f.circle(0xFF5AAED0, (b[0] + b[2]) / 2, b[3] + 12, 5.5f);
                f.circle(0xFFDDF0F8, (b[0] + b[2]) / 2, b[3] + 12, 1.5f);
                flags(q, b[0] + 4, b[3] + 4, 4);
                float pu0 = b[0], pu1 = Math.min(b[2], b[0] + 56);
                if (S - (b[3] + 26) > 20) parking(q, r, pu0, S - 20, pu1, S - 4);
                // The dish array across the lawn.
                float du0 = b[2] + 16, du1 = L - 10;
                float dr = Math.max(9, Math.min(64, Math.min(S * 0.24f, (du1 - du0) / 5.5f)));
                int n = Math.max(1, Math.min(5, (int) ((du1 - du0) / (dr * 2.6f))));
                for (int k = 0; k < n; k++) {
                    float u = du0 + (k + 0.5f) * (du1 - du0) / n, v = S * (k % 2 == 0 ? 0.36f : 0.62f);
                    f.circle(0xFFB8B4AA, u, v, dr * 1.15f);
                    dish(q, u, v, dr);
                }
                for (int k = 0; k < 6; k++) f.tree(du0 - 8, 8 + k * (S - 16) / 5f, 5, 0xFF4A7E3C);
                for (int k = 0; k < 6; k++) person(f, b[0] + r.nextFloat() * (b[2] - b[0]), b[3] + 4 + r.nextFloat() * 16, CLOTHES[r.nextInt(8)]);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                officeRoof(f, 0xFFD8D4CA, r, null, null, 0);
                f.rect(0xFF4A7AA0, 3, S * 0.4f, L - 3, S * 0.6f);
                for (float u = 5; u < L - 3; u += 4) f.line(0xFF9AC4E0, 0.5f, u, S * 0.4f, u, S * 0.6f);
                Q q = qOf(f, c, p, x0, y0, x1, y1);
                for (int k = 0; k < 3; k++) dish(q, L * (0.2f + k * 0.3f), S * 0.22f, Math.min(5, S * 0.12f));
                f.sign(city, "MISSION CONTROL", L / 2, S * 0.8f, Math.min(9, Math.max(4, S / 5)), 0xFF2A3A5A);
            }
        };
        d.ground = City.GRASS;
        d.minS = 12;
        d.minL = 18;
        d.base = City.OFFICE;
        d.inside = Variants.I_OFFICE;
        d.roofCol = 0xFFD8D4CA;
        d.accent = 0xFF2A3A5A;
        out.add(d);
    }

    static void headquarters(List<Landmarks.Def> out) {
        // The headquarters: two office wings round a lawn with a fountain, flags out front, palms, a small car park.
        Landmarks.Def d = new Landmarks.Def("Space Center HQ", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int lv = Math.max(4, Math.min(10, S / 3)), lu = Math.max(6, Math.min(36, L / 3));
                int gap = Math.max(4, Math.min(30, L - 2 * lu - 6));
                return new int[][]{at(al, 2, 2, lu, lv, 7), at(al, 2 + lu + gap, 2, lu, lv, 7),
                        at(al, 2 + lu + 1, 2, Math.max(1, gap - 2), Math.max(3, lv / 2), 3)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                Q q = qOf(f, c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] pq = pl(this, x0, y0, x1, y1);
                float[] a = bx(f, pq[0]), b = bx(f, pq[1]);
                f.rect(0xFF74A458, 0, 0, L, S);
                paving(f, 0xFFE0DACC, a[0], a[3] + 2, b[2], a[3] + 12, 6);
                float cu = (a[2] + b[0]) / 2;
                f.rect(0xFFE0DACC, cu - 5, a[3], cu + 5, S);
                f.circle(0xFFB8B4AA, cu, a[3] + 28, 10);
                f.circle(0xFF5AAED0, cu, a[3] + 28, 8.5f);
                f.circle(0xFFDDF0F8, cu, a[3] + 28, 2);
                flags(q, cu - 22, a[3] + 44, 5);
                for (float u = 8; u < L - 4; u += 20) palm(f, u, S - 6, 6);
                if (S - a[3] > 60) parking(q, r, b[0], S - 22, Math.min(L - 4, b[0] + 60), S - 6);
                crowd(f, r, a[0], a[3], b[2], a[3] + 14, 8);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                if (part == 2) {
                    officeRoof(f, 0xFF8AA8C0, r, null, null, 0);
                    return;
                }
                officeRoof(f, 0xFFE2DED4, r, city, part == 0 ? "HEADQUARTERS" : null, 0xFF2A3A5A);
                float L = f.L, S = f.S;
                for (float u = 4; u < L - 4; u += 6) f.rect(0xFF6A8AA8, u, S - 3, u + 3.5f, S - 1.6f);
            }
        };
        d.ground = City.GRASS;
        d.minS = 12;
        d.minL = 20;
        d.base = City.OFFICE;
        d.inside = Variants.I_OFFICE;
        d.roofCol = 0xFFE2DED4;
        out.add(d);
    }

    static void propulsionLab(List<Landmarks.Def> out) {
        // The propulsion lab: the lab block with its rooftop vents, an engine test stand in a sooty pit behind a blast
        // wall, gas bottles in racks and tanks on a concrete yard.
        Landmarks.Def d = new Landmarks.Def("Propulsion Lab", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int lu = Math.max(6, Math.min(30, L / 3)), lv = Math.max(5, Math.min(16, S / 2));
                return new int[][]{at(al, 2, 2, lu, lv, 3), at(al, L - 2 - Math.max(3, lu / 3), S - 2 - 3, Math.max(3, lu / 3), 3, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                Q q = qOf(f, c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] pq = pl(this, x0, y0, x1, y1);
                float[] a = bx(f, pq[0]);
                f.rect(SCRUB, 0, 0, L, S);
                slabs(q, CONCRETE, 2, 2, L - 2, S - 2, 16);
                // The test stand: a pit, the blast wall, the stand's frame, the soot fanned out from it.
                float tu = a[2] + (L - a[2]) * 0.5f, tv = S * 0.42f, ts = Math.min(Math.min(S * 0.5f, (L - a[2]) * 0.5f), 130);
                for (int k = 0; k < 4; k++) f.oval(0x18000000, tu - ts * (0.6f + k * 0.2f), tv, tu + ts * (0.6f + k * 0.2f), tv + ts * (1.2f + k * 0.3f));
                f.rect(0xFF3A3632, tu - ts * 0.3f, tv - ts * 0.3f, tu + ts * 0.3f, tv + ts * 0.9f);
                f.rect(0xFF221E1A, tu - ts * 0.24f, tv, tu + ts * 0.24f, tv + ts * 0.85f);
                f.rect(0x38000000, tu - ts * 0.5f + 3, tv - ts * 0.6f + 4, tu + ts * 0.5f + 3, tv - ts * 0.42f + 4);
                f.rect(0xFF8A867C, tu - ts * 0.5f, tv - ts * 0.6f, tu + ts * 0.5f, tv - ts * 0.42f);
                f.rect(0x38000000, tu - ts * 0.18f + 6, tv - ts * 0.18f + 8, tu + ts * 0.18f + 6, tv + ts * 0.18f + 8);
                f.rect(0xFF4A4C52, tu - ts * 0.18f, tv - ts * 0.18f, tu + ts * 0.18f, tv + ts * 0.18f);
                f.frame(0xFFE0C040, 1, tu - ts * 0.18f, tv - ts * 0.18f, tu + ts * 0.18f, tv + ts * 0.18f);
                f.line(0xFFE0C040, 0.6f, tu - ts * 0.18f, tv - ts * 0.18f, tu + ts * 0.18f, tv + ts * 0.18f);
                f.circle(0xFF2A2A2E, tu, tv, ts * 0.08f);
                // Tanks and gas bottles.
                float gu = a[0], gv = a[3] + 8;
                float cr0 = Math.max(4.5f, Math.min(14, (S - gv) * 0.12f)), cl = cr0 * 5;
                for (int k = 0; k < 4 && gu + (cl + cr0 * 2) * k + cl < a[2]; k++)
                    cylinder(q, gu + (cl + cr0 * 2) * k, gu + (cl + cr0 * 2) * k + cl, gv + cr0 + 4, cr0, k == 1 ? 0xFFB8D0E0 : 0xFFE8E6E0);
                for (float u = a[0]; u < a[2] - 4; u += 3.5f) {
                    f.circle(0xFF2A6A4A, u + 1.5f, S - 8, 1.4f);
                    f.circle(0xFF9A9CA0, u + 1.5f, S - 4.5f, 1.4f);
                }
                for (int k = 0; k < 4; k++) worker(q, tu - ts + r.nextFloat() * ts * 2, tv + ts * 1.1f + r.nextFloat() * 8);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                if (part == 1) {
                    // The control bunker: thick concrete, a slit, earth banked on top.
                    f.rect(0xFF7A766C, 0, 0, L, S);
                    f.rect(0xFF8E9A6A, 1.5f, 1.5f, L - 1.5f, S - 1.5f);
                    f.rect(0xFF2A2A2E, 2, 0.5f, L - 2, 1.4f);
                    return;
                }
                officeRoof(f, 0xFFC8C4B8, r, city, "PROPULSION LAB", 0xFF5A3A2A);
                for (float u = 6; u < L - 6; u += 10) {
                    f.circle(0x38000000, u + 1, S * 0.32f + 1, 2.4f);
                    f.circle(0xFF9A9CA0, u, S * 0.32f, 2.4f);
                    f.circle(0xFF5A5C60, u, S * 0.32f, 1.2f);
                }
            }
        };
        d.ground = City.LOT;
        d.minS = 12;
        d.minL = 18;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_LAB;
        d.roofCol = 0xFFC8C4B8;
        out.add(d);
    }

    static void payloadProcessing(List<Landmarks.Def> out) {
        // The payload processing facility: a tall white clean-room hall with its high door, the airlock annex, a fenced
        // apron with the long payload transporter and its white canister.
        Landmarks.Def d = new Landmarks.Def("Payload Processing", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int lu = Math.max(6, Math.min(26, L / 3)), lv = Math.max(6, Math.min(20, S * 3 / 5));
                return new int[][]{at(al, 3, 2, lu, lv, 9), at(al, 3 + lu + 1, 3, Math.max(3, lu / 3), Math.max(3, lv / 2), 3)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] pq = pl(this, x0, y0, x1, y1);
                float[] a = bx(f, pq[0]), b = bx(f, pq[1]);
                f.rect(0xFF74A458, 0, 0, L, S);
                f.rect(CONCRETE_DARK, 2, 2, Math.max(b[2], a[2]) + 40, S - 2);
                f.rect(CONCRETE, 3, 3, Math.max(b[2], a[2]) + 39, S - 3);
                f.frame(0xFF8A8A84, 0.6f, 2, 2, Math.max(b[2], a[2]) + 40, S - 2);
                // The transporter out in front of the high door with its canister.
                float tu0 = a[0] + 4, tu1 = Math.min(a[2] + 30, L - 6), tv = a[3] + Math.min(16, (S - a[3]) / 2);
                if (tu1 - tu0 > 30 && S - a[3] > 14) {
                    f.rect(0x40000000, tu0 + 3, tv - 5 + 4, tu1 + 3, tv + 5 + 4);
                    f.rect(0xFF3A3C40, tu0, tv - 5, tu1, tv + 5);
                    for (float u = tu0 + 3; u < tu1 - 2; u += 6) {
                        f.rect(0xFF1A1A1E, u, tv - 6, u + 3, tv - 5);
                        f.rect(0xFF1A1A1E, u, tv + 5, u + 3, tv + 6);
                    }
                    f.roundRect(0xFFB8B6B0, tu0 + 8, tv - 4.4f, tu1 - 4, tv + 4.4f, 4);
                    f.roundRect(0xFFF6F4EE, tu0 + 8.5f, tv - 4, tu1 - 4.5f, tv + 1.5f, 3.5f);
                    f.rect(0xFFE0C040, tu0, tv - 5, tu0 + 6, tv + 5);
                    f.rect(0xFF2A4A6A, tu0 + 1, tv - 3.5f, tu0 + 4, tv + 3.5f);
                }
                for (float v = 8; v < S - 4; v += 22) f.tree(L - 8, v, 5, 0xFF4A7E3C);
                for (int k = 0; k < 4; k++) person(f, a[0] + r.nextFloat() * (a[2] - a[0]), a[3] + 2 + r.nextFloat() * 4, 0xFFF2F2EE);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                if (part == 1) {
                    officeRoof(f, 0xFFDCDAD4, r, null, null, 0);
                    return;
                }
                f.rect(0xFFA8A6A0, 0, 0, L, S);
                f.rect(0xFFF4F2EE, 1.6f, 1.6f, L - 1.6f, S - 1.6f);
                for (float u = 1.6f + 10; u < L - 2; u += 10) f.line(0x10000000, 0.5f, u, 1.6f, u, S - 1.6f);
                // The high door's slot on the front, the clean air plant along the back.
                f.rect(0xFF5A5C60, L * 0.3f, S - 4, L * 0.7f, S);
                for (float u = L * 0.3f + 2; u < L * 0.7f; u += 2.4f) f.line(0xFF8A8C90, 0.5f, u, S - 4, u, S);
                for (float u = 4; u < L - 8; u += 9) plant(f, u, 3.5f, 6);
                f.sign(city, "PAYLOAD PROCESSING", L / 2, S * 0.55f, Math.min(8, Math.max(4, S / 5)), 0xFF3A4A6A);
            }
        };
        d.ground = City.GRASS;
        d.minS = 12;
        d.minL = 18;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_LAB;
        d.roofCol = 0xFFF4F2EE;
        out.add(d);
    }

    static void training(List<Landmarks.Def> out) {
        // Astronaut training: the hall with the big pool under its skylight (a spacecraft mock-up on the bottom), the
        // round centrifuge house with its arm, a running track on the lawn.
        Landmarks.Def d = new Landmarks.Def("Astronaut Training", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int lu = Math.max(8, Math.min(30, L * 2 / 5)), lv = Math.max(6, Math.min(18, S * 3 / 5));
                return new int[][]{at(al, 2, 2, lu, lv, 4)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] a = bx(f, pl(this, x0, y0, x1, y1)[0]);
                f.rect(0xFF74A458, 0, 0, L, S);
                // The running track.
                float tu0 = a[2] + 14, tu1 = L - 10, tv0 = 8, tv1 = S - 8;
                if (tu1 - tu0 > 60 && tv1 - tv0 > 30) {
                    float rad = (tv1 - tv0) / 2;
                    float tk = Math.max(8, Math.min(18, rad * 0.14f));
                    f.roundRect(0xFFB85A3E, tu0, tv0, tu1, tv1, rad);
                    for (int k = 1; k < 4; k++) {
                        float in = k * tk / 4;
                        f.roundRect(0xC0F2EEE6, tu0 + in - 0.4f, tv0 + in - 0.4f, tu1 - in + 0.4f, tv1 - in + 0.4f, rad - in + 0.4f);
                        f.roundRect(0xFFB85A3E, tu0 + in + 0.4f, tv0 + in + 0.4f, tu1 - in - 0.4f, tv1 - in - 0.4f, rad - in - 0.4f);
                    }
                    f.roundRect(0xFFF2EEE6, tu0 + tk - 0.5f, tv0 + tk - 0.5f, tu1 - tk + 0.5f, tv1 - tk + 0.5f, rad - tk + 0.5f);
                    f.roundRect(0xFF74A458, tu0 + tk, tv0 + tk, tu1 - tk, tv1 - tk, rad - tk);
                    // The centrifuge house in the middle of it: a drum, its arm and capsule seen through the open roof.
                    float cu = (tu0 + tu1) / 2, cv = (tv0 + tv1) / 2, cr = Math.min(rad - tk - 10, 80);
                    if (cr > 8) {
                        f.circle(0x38000000, cu + 4, cv + 5, cr);
                        f.circle(0xFF8E8C86, cu, cv, cr);
                        f.circle(0xFFCFCBC2, cu, cv, cr - 1.5f);
                        f.circle(0xFF5A5C60, cu, cv, cr - 4);
                        f.line(0xFFE0C040, Math.max(2.4f, cr * 0.08f), cu - (cr - 6), cv, cu + (cr - 6), cv);
                        f.circle(0xFFF2F2EE, cu + cr - 4 - cr * 0.12f, cv, Math.max(3.4f, cr * 0.12f));
                        f.circle(0xFF8E8C86, cu - cr + 4 + cr * 0.1f, cv, Math.max(2.4f, cr * 0.09f));
                        f.circle(0xFF3A3C40, cu, cv, Math.max(3, cr * 0.1f));
                    }
                }
                paving(f, 0xFFDCD6C8, a[0], a[3] + 2, a[2], a[3] + 10, 6);
                crowd(f, r, a[0], a[3] + 2, a[2], a[3] + 10, 6);
                for (float v = 8; v < S - 4; v += 20) f.tree(a[2] + 6, v, 4.5f, 0xFF4A7E3C);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                officeRoof(f, 0xFFD0CCC2, r, null, null, 0);
                // The pool's skylight: the water seen through the glass, lane lines, the mock-up on the bottom.
                float u0 = L * 0.12f, u1 = L * 0.88f, v0 = S * 0.18f, v1 = S * 0.72f;
                f.rect(0xFF5A7A90, u0 - 1, v0 - 1, u1 + 1, v1 + 1);
                f.rect(0xFF3A9EC8, u0, v0, u1, v1);
                f.rect(0xFF2E86B0, u0 + 2, v0 + 2, u1 - 2, v1 - 2);
                f.rect(0xFFB8BCC0, (u0 + u1) / 2 - (u1 - u0) * 0.2f, (v0 + v1) / 2 - 2.4f, (u0 + u1) / 2 + (u1 - u0) * 0.2f, (v0 + v1) / 2 + 2.4f);
                f.circle(0xFFD8DCDE, (u0 + u1) / 2 - (u1 - u0) * 0.2f, (v0 + v1) / 2, 3.6f);
                for (float u = u0 + 6; u < u1; u += 6) f.line(0x40FFFFFF, 0.5f, u, v0, u, v1);
                f.sign(city, "ASTRONAUT TRAINING", L / 2, S * 0.86f, Math.min(7, Math.max(4, S / 6)), 0xFF2A3A5A);
            }
        };
        d.ground = City.GRASS;
        d.minS = 12;
        d.minL = 18;
        d.base = City.OFFICE;
        d.inside = Variants.I_GYM;
        d.roofCol = 0xFFD0CCC2;
        out.add(d);
    }

    static void crewQuarters(List<Landmarks.Def> out) {
        // The crew quarters: low blocks of rooms round a pool garden, palms, a barbecue terrace.
        Landmarks.Def d = new Landmarks.Def("Crew Quarters", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int lv = Math.max(3, Math.min(7, S / 4)), lu = Math.max(6, L - 4);
                return new int[][]{at(al, 2, 2, lu, lv, 3), at(al, 2, S - 2 - lv, Math.max(4, lu / 2), lv, 3)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] pq = pl(this, x0, y0, x1, y1);
                float[] a = bx(f, pq[0]), b = bx(f, pq[1]);
                f.rect(0xFF74A458, 0, 0, L, S);
                float gv0 = a[3] + 6, gv1 = b[1] - 6;
                if (gv1 - gv0 > 14) {
                    paving(f, 0xFFEDE4D2, a[0], gv0, a[2], gv1, 6);
                    float pu0 = a[0] + (a[2] - a[0]) * 0.55f, pu1 = Math.min(a[2] - 8, pu0 + 70);
                    if (pu1 - pu0 > 16) pool(f, pu0, gv0 + 4, pu1, gv1 - 4, 2);
                    for (float u = a[0] + 6; u < pu0 - 8; u += 14) LandmarkArt13.lounger(f, u, (gv0 + gv1) / 2, false, 0xFFF2F2EE);
                }
                for (float u = b[2] + 10; u < L - 6; u += 18) palm(f, u, S - 8, 6.5f);
                for (float u = 8; u < L - 4; u += 22) palm(f, u, gv0 - 2, 5);
                crowd(f, r, a[0], gv0, a[2], gv1, 6);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                LandmarkArt12.hip(f, 0xFFC27A54, 0, 0, f.L, f.S, true);
                if (part == 0 && f.L > 40) f.sign(city, "CREW QUARTERS", f.L / 2, f.S / 2, Math.min(7, Math.max(4, f.S / 3)), 0xFFF4EEE4);
            }
        };
        d.ground = City.GRASS;
        d.minS = 14;
        d.minL = 16;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOTEL;
        d.roofCol = 0xFFC27A54;
        out.add(d);
    }

    static void visitorComplex(List<Landmarks.Def> out) {
        // The visitor complex: the exhibit hall, the rocket garden of old rockets standing in a ring, a spaceplane on
        // show on its stand, crowds on the plaza, palms, a small car park.
        Landmarks.Def d = new Landmarks.Def("Visitor Complex", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int lu = Math.max(6, Math.min(28, L / 3)), lv = Math.max(5, Math.min(14, S / 2));
                return new int[][]{at(al, 2, S - 2 - lv, lu, lv, 3)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                Q q = qOf(f, c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] a = bx(f, pl(this, x0, y0, x1, y1)[0]);
                f.rect(0xFF74A458, 0, 0, L, S);
                paving(f, 0xFFE4DCCA, 4, 4, L - 4, S - 4, 8);
                // The rocket garden.
                float gu = a[2] + (L - a[2]) * 0.3f, gv = S * 0.42f, gr = Math.min(S * 0.36f, (L - a[2]) * 0.28f);
                f.circle(0xFF74A458, gu, gv, gr + 12);
                f.ring(0xFFD8D0BC, 3, gu, gv, gr + 6);
                int n = Math.max(3, Math.min(8, (int) (gr / 6)));
                for (int k = 0; k < n; k++) {
                    double ang = -Math.PI / 2 + k * 2 * Math.PI / n;
                    float ru = gu + (float) Math.cos(ang) * gr * 0.75f, rv = gv + (float) Math.sin(ang) * gr * 0.75f;
                    rocket(q, ru, rv, gr * (0.05f + (k % 3) * 0.02f), k % 3 == 0 ? 2 : k % 3 == 1 ? 1 : 0, gr * (0.4f + k % 3 * 0.15f));
                }
                rocket(q, gu, gv, gr * 0.12f, 0, gr * 0.9f);
                // The spaceplane on its stand: a white delta, black along its leading edges.
                float su = a[2] + (L - a[2]) * 0.75f, sv = S * 0.45f, sl = Math.min((L - a[2]) * 0.3f, S * 0.7f);
                if (sl > 30) {
                    f.poly(0x38000000, su - sl / 2 + 6, sv + 8, su + sl * 0.15f + 6, sv - sl * 0.32f + 8, su + sl / 2 + 6, sv - sl * 0.32f + 8, su + sl / 2 + 6, sv + sl * 0.32f + 8, su + sl * 0.15f + 6, sv + sl * 0.32f + 8);
                    f.poly(0xFF2A2A2E, su - sl / 2, sv, su + sl * 0.12f, sv - sl * 0.34f, su + sl / 2, sv - sl * 0.34f, su + sl / 2, sv + sl * 0.34f, su + sl * 0.12f, sv + sl * 0.34f);
                    f.poly(0xFFF0EEE8, su - sl / 2 + 3, sv, su + sl * 0.14f, sv - sl * 0.3f, su + sl / 2 - 1, sv - sl * 0.3f, su + sl / 2 - 1, sv + sl * 0.3f, su + sl * 0.14f, sv + sl * 0.3f);
                    f.roundRect(0xFFF6F4EE, su - sl * 0.48f, sv - sl * 0.08f, su + sl * 0.5f, sv + sl * 0.08f, sl * 0.08f);
                    f.rect(0xFFD8D6D0, su - sl * 0.05f, sv - sl * 0.07f, su + sl * 0.4f, sv + sl * 0.07f);
                    f.line(0xFFD8D6D0, 0.6f, su - sl * 0.05f, sv, su + sl * 0.4f, sv);
                    f.poly(0xFF2A2A2E, su - sl * 0.47f, sv - sl * 0.04f, su - sl * 0.4f, sv - sl * 0.07f, su - sl * 0.4f, sv + sl * 0.07f, su - sl * 0.47f, sv + sl * 0.04f);
                    f.rect(0xFF2A4A8A, su + sl * 0.18f, sv - sl * 0.24f, su + sl * 0.3f, sv - sl * 0.18f);
                    f.rect(0xFFC8382E, su + sl * 0.18f, sv + sl * 0.18f, su + sl * 0.3f, sv + sl * 0.24f);
                }
                if (a[1] > 30) parking(q, r, 6, 6, Math.min(a[2], 70), 22);
                for (float u = 8; u < L - 4; u += 24) palm(f, u, S - 4, 5.5f);
                crowd(f, r, a[2], 4, L - 4, S - 4, 30);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                // The exhibit hall: a curved roof, ribbed, and a glass front.
                f.rect(0xFF5A6A7A, 0, 0, L, S);
                f.rect(0xFFDCE4EA, 1, 1, L - 1, S * 0.35f);
                f.rect(0xFFC4CED6, 1, S * 0.35f, L - 1, S * 0.65f);
                f.rect(0xFFAAB4BE, 1, S * 0.65f, L - 1, S - 1);
                for (float u = 4; u < L - 1; u += 4) f.line(0x18000000, 0.5f, u, 1, u, S - 1);
                f.sign(city, "VISITOR COMPLEX", L / 2, S / 2, Math.min(7, Math.max(4, S / 4)), 0xFF1E3C82);
            }
        };
        d.ground = City.PLAZA;
        d.minS = 12;
        d.minL = 20;
        d.base = City.SHOP;
        d.inside = Variants.I_SHOWROOM;
        d.roofCol = 0xFFC4CED6;
        out.add(d);
    }

    static void trackingStation(List<Landmarks.Def> out) {
        // The tracking station: white radomes on their plinths, two big dishes, the operations hut, all inside a fence.
        Landmarks.Def d = new Landmarks.Def("Tracking Station", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, 3, 3, Math.max(5, Math.min(14, L / 6)), Math.max(4, Math.min(8, S / 3)), 2)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                Q q = qOf(f, c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] a = bx(f, pl(this, x0, y0, x1, y1)[0]);
                f.rect(0xFF74A458, 0, 0, L, S);
                f.frame(0xFF8A8A84, 0.6f, 3, 3, L - 3, S - 3);
                float u0 = a[2] + 14, span = L - 10 - u0;
                float R = Math.max(8, Math.min(60, Math.min(S * 0.24f, span / 6)));
                int n = Math.max(1, (int) (span / (R * 3)));
                for (int k = 0; k < n; k++) {
                    float u = u0 + (k + 0.5f) * span / n, v = S * (k % 2 == 0 ? 0.32f : 0.66f);
                    f.circle(0xFFB8B4AA, u, v, R * 1.2f);
                    if (k % 3 == 1) dish(q, u, v, R);
                    else radome(q, u, v, R * 0.85f);
                }
                f.rect(0xFFB8B4AA, a[0], a[3], a[2], S - 3);
                for (int k = 0; k < 3; k++) worker(q, a[0] + r.nextFloat() * (a[2] - a[0]), a[3] + 3 + r.nextFloat() * 6);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                officeRoof(f, 0xFFD0CCC2, r, city, "TRACKING", 0xFF2A3A5A);
                Q q = qOf(f, c, p, x0, y0, x1, y1);
                dish(q, f.L * 0.8f, f.S * 0.3f, Math.min(5, f.S * 0.15f));
            }
        };
        d.ground = City.GRASS;
        d.minS = 12;
        d.minL = 18;
        d.base = City.OFFICE;
        d.inside = Variants.I_OFFICE;
        d.roofCol = 0xFFD0CCC2;
        out.add(d);
    }

    static void fuelFarm(List<Landmarks.Def> out) {
        // The fuel farm: great spheres of liquid hydrogen and oxygen, rows of lying tanks of kerosene and gas, the pipe
        // racks between them, low berms round it all, the pump house.
        Landmarks.Def d = new Landmarks.Def("Fuel Farm", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, 2, S - 2 - Math.max(3, Math.min(6, S / 5)), Math.max(4, Math.min(10, L / 7)), Math.max(3, Math.min(6, S / 5)), 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                Q q = qOf(f, c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] a = bx(f, pl(this, x0, y0, x1, y1)[0]);
                scrub(q, r);
                f.roundRect(0xFF9AA070, 3, 3, L - 3, S - 3, 6);
                f.roundRect(CONCRETE, 6, 6, L - 6, S - 6, 4);
                // The pipe rack down the middle.
                float pv = S * 0.5f;
                f.rect(0x38000000, 6, pv - 2 + 2, L - 6, pv + 2 + 2);
                f.rect(0xFF8A8C90, 6, pv - 2, L - 6, pv + 2);
                for (int k = 0; k < 3; k++) f.line(k == 1 ? 0xFFE0C040 : 0xFFB8BABE, 0.8f, 6, pv - 1.2f + k * 1.2f, L - 6, pv - 1.2f + k * 1.2f);
                // Spheres along one side.
                int n = Math.max(1, Math.min(4, Math.round(L / (S * 0.75f))));
                float R = Math.max(8, Math.min(Math.min(S * 0.19f, 90), (L - 24) / (n * 2.5f)));
                for (int k = 0; k < n; k++) {
                    float u = n == 1 ? L / 2 : 12 + R * 1.1f + k * (L - 24 - 2.2f * R) / (n - 1);
                    f.circle(0xFF9AA070, u, S * 0.26f, R * 1.3f);
                    f.circle(CONCRETE_DARK, u, S * 0.26f, R * 1.18f);
                    f.line(0xFF8A8C90, 2, u, pv, u, S * 0.26f);
                    sphere(q, u, S * 0.26f, R, k % 2 == 0 ? 0xFFF2F2EE : 0xFFB8D0E0);
                }
                // Lying tanks along the other.
                float tr = Math.max(4, Math.min(26, S * 0.06f)), tl = tr * 7, tv = pv + 6 + tr * 1.6f;
                for (float u = a[2] + 12; u + tl < L - 8; u += tl + tr * 3) {
                    for (int k = 0; k < 2 && tv + tr * 2.6f * k + tr < S - 6; k++)
                        cylinder(q, u, u + tl, tv + tr * 2.6f * k, tr, k == 0 ? 0xFFE8E6E0 : 0xFFD8D6D0);
                    f.line(0xFF8A8C90, 1.4f, u + tl / 2, pv, u + tl / 2, tv - tr);
                }
                f.sign(city, "LH2", L * 0.2f, S * 0.08f + 4, 4, 0xFF3A3A3A);
                for (int k = 0; k < 3; k++) worker(q, a[2] + 4 + r.nextFloat() * 20, a[1] + r.nextFloat() * (a[3] - a[1]));
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                officeRoof(f, 0xFFB8B4AA, r, null, null, 0);
                f.rect(0xFFE0C040, 2, f.S - 3, f.L - 2, f.S - 1.6f);
            }
        };
        d.ground = City.LOT;
        d.minS = 14;
        d.minL = 20;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_FACTORY;
        d.roofCol = 0xFFB8B4AA;
        out.add(d);
    }

    // ------------------------------------------------------------------ the landing strip

    static void landingStrip(List<Landmarks.Def> out) {
        // The landing strip: a long, wide runway, grooved, its threshold bars, numbers, touchdown marks and centre line;
        // the taxiway beside it, the apron with the hangar and the little control tower, a spaceplane and two jets
        // parked out on it.
        Landmarks.Def d = new Landmarks.Def("Landing Strip", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int hv = Math.max(3, Math.min(8, S / 4)), hu = Math.max(4, Math.min(12, hv * 3 / 2));
                int u = L / 2 - hu;
                return new int[][]{at(al, u, S - 1 - hv, hu, hv, 4), at(al, u + hu + 3, S - 1 - Math.max(2, hv / 2), 2, Math.max(2, hv / 2), 6)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                Q q = qOf(f, c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] pq = pl(this, x0, y0, x1, y1);
                float[] hg = bx(f, pq[0]), tw = bx(f, pq[1]);
                f.rect(0xFF86A462, 0, 0, L, S);
                for (float u = 0; u < L; u += 40) f.rect(0xFF7E9C5A, u, 0, u + 20, S);
                // The runway.
                float rv0 = TT * 1.5f, rw = Math.max(48, Math.min(150, S * 0.34f)), rv1 = rv0 + rw, e = 12;
                f.rect(0xFF8E8C88, e - 2, rv0 - 2, L - e + 2, rv1 + 2);
                f.rect(0xFF6A6A6C, e, rv0, L - e, rv1);
                for (float u = e + 4; u < L - e; u += 3) f.line(0x0E000000, 0.5f, u, rv0, u, rv1);
                f.line(0xFFF2F2EE, 1.2f, e + 2, rv0 + 2, L - e - 2, rv0 + 2);
                f.line(0xFFF2F2EE, 1.2f, e + 2, rv1 - 2, L - e - 2, rv1 - 2);
                float cv = (rv0 + rv1) / 2;
                for (float u = e + 70; u < L - e - 70; u += 30) f.rect(0xFFF2F2EE, u, cv - 0.8f, u + 16, cv + 0.8f);
                for (int end = 0; end < 2; end++) {
                    float u0 = end == 0 ? e + 4 : L - e - 4 - 22;
                    // The threshold's piano keys.
                    for (float v = rv0 + 5; v < rv1 - 5; v += 5.5f) f.rect(0xFFF2F2EE, u0, v, u0 + 22, v + 3);
                    // Touchdown and aiming marks.
                    float dir = end == 0 ? 1 : -1, ub = end == 0 ? e + 70 : L - e - 70;
                    for (int k = 0; k < 4; k++) {
                        float uu = ub + dir * k * 30;
                        float n = 3 - k * 0.6f;
                        for (int s = -1; s <= 1; s += 2) {
                            for (int j = 0; j < Math.max(1, (int) n); j++) {
                                float vv = cv + s * (rw * 0.18f + j * 4.5f);
                                f.rect(0xFFF2F2EE, Math.min(uu, uu + dir * 14), vv - 1.2f, Math.max(uu, uu + dir * 14), vv + 1.2f);
                            }
                        }
                    }
                    for (int s = -1; s <= 1; s += 2) {
                        float vv = cv + s * rw * 0.28f, uu = ub + dir * 140;
                        f.rect(0xFFF2F2EE, Math.min(uu, uu + dir * 40), vv - 3, Math.max(uu, uu + dir * 40), vv + 3);
                    }
                    // The tyre marks where they touch down.
                    for (int k = 0; k < 10; k++) {
                        float uu = ub + dir * (20 + r.nextFloat() * 160);
                        f.rect(0x22000000, uu, cv - rw * 0.12f + r.nextFloat() * rw * 0.24f, uu + dir * (8 + r.nextFloat() * 20), cv - rw * 0.12f + r.nextFloat() * rw * 0.24f + 1.2f);
                    }
                }
                f.sign(city, "15", e + 40, cv, Math.max(6, rw / 6), 0xFFF2F2EE);
                f.sign(city, "33", L - e - 40, cv, Math.max(6, rw / 6), 0xFFF2F2EE);
                // The taxiway, its links to the runway, the apron.
                float tv0 = rv1 + Math.max(16, (S - rv1) * 0.22f), tv1 = tv0 + Math.max(16, Math.min(40, rw * 0.35f));
                f.rect(0xFF7A7A7C, e + 30, tv0, L - e - 30, tv1);
                f.line(0xFFE8C040, 0.8f, e + 30, (tv0 + tv1) / 2, L - e - 30, (tv0 + tv1) / 2);
                for (float u : new float[]{e + 30, L / 2 - 60, L / 2 + 60, L - e - 30 - (tv1 - tv0)}) {
                    f.rect(0xFF7A7A7C, u, rv1, u + (tv1 - tv0), tv0 + 1);
                    f.line(0xFFE8C040, 0.8f, u + (tv1 - tv0) / 2, rv1 + 2, u + (tv1 - tv0) / 2, tv0);
                }
                float au0 = hg[0] - 80, au1 = tw[2] + 60;
                f.rect(0xFF8A8A8C, au0, tv1, au1, S - 2);
                for (float u = au0 + 20; u < au1; u += 20) f.line(0x18000000, 0.5f, u, tv1, u, S - 2);
                // A spaceplane at the apron, and two jets.
                float su = au0 + 40, sv = (tv1 + S) / 2, sl = Math.min(60, (S - tv1) * 0.9f);
                if (sl > 24) {
                    f.poly(0x38000000, su - sl / 2 + 5, sv + 6, su + sl * 0.15f + 5, sv - sl * 0.32f + 6, su + sl / 2 + 5, sv - sl * 0.32f + 6, su + sl / 2 + 5, sv + sl * 0.32f + 6, su + sl * 0.15f + 5, sv + sl * 0.32f + 6);
                    f.poly(0xFF2A2A2E, su - sl / 2, sv, su + sl * 0.12f, sv - sl * 0.34f, su + sl / 2, sv - sl * 0.34f, su + sl / 2, sv + sl * 0.34f, su + sl * 0.12f, sv + sl * 0.34f);
                    f.poly(0xFFF0EEE8, su - sl / 2 + 3, sv, su + sl * 0.14f, sv - sl * 0.3f, su + sl / 2 - 1, sv - sl * 0.3f, su + sl / 2 - 1, sv + sl * 0.3f, su + sl * 0.14f, sv + sl * 0.3f);
                    f.roundRect(0xFFF6F4EE, su - sl * 0.48f, sv - sl * 0.08f, su + sl * 0.5f, sv + sl * 0.08f, sl * 0.08f);
                    f.rect(0xFFD8D6D0, su - sl * 0.05f, sv - sl * 0.07f, su + sl * 0.4f, sv + sl * 0.07f);
                }
                for (int k = 0; k < 2; k++) {
                    float ju = tw[2] + 16 + k * 22, jv = (tv1 + S) / 2;
                    if (ju + 10 > au1) break;
                    jet(q, ju, jv, 9);
                }
                for (int k = 0; k < 4; k++) worker(q, hg[0] - 20 + r.nextFloat() * 30, hg[1] - 6 + r.nextFloat() * 4);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                if (part == 1) {
                    // The control tower's cab: glass all round, the roof with its beacon.
                    f.rect(0xFF3A4A5A, 0, 0, L, S);
                    f.rect(0xFF7AA6C8, 1, 1, L - 1, S - 1);
                    f.rect(0xFF5A5C60, L * 0.25f, S * 0.25f, L * 0.75f, S * 0.75f);
                    f.circle(0xFFE84A3A, L / 2, S / 2, Math.min(L, S) * 0.12f);
                    return;
                }
                // The hangar: a curved roof, ribbed.
                f.rect(0xFF6A6E74, 0, 0, L, S);
                f.rect(0xFFC8CCD0, 1, 1, L - 1, S * 0.35f);
                f.rect(0xFFB0B4B8, 1, S * 0.35f, L - 1, S * 0.65f);
                f.rect(0xFF969AA0, 1, S * 0.65f, L - 1, S - 1);
                for (float u = 3; u < L - 1; u += 3) f.line(0x1C000000, 0.5f, u, 1, u, S - 1);
                f.sign(city, "LANDING FACILITY", L / 2, S / 2, Math.min(5, Math.max(3, S / 4)), 0xFF2A3A4A);
            }
        };
        d.ground = City.GRASS;
        d.minS = 12;
        d.minL = 60;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_FACTORY;
        d.roofCol = 0xFFB0B4B8;
        out.add(d);
    }

    /** A small trainer jet from above, nose to -u: swept wings, tail, canopy, its shadow. */
    static void jet(Q f, float u, float v, float s) {
        f.poly(0x38000000, u - s + 3, v + 4, u + s * 0.2f + 3, v - s * 0.7f + 4, u + s * 0.45f + 3, v - s * 0.7f + 4, u + s * 0.25f + 3, v + 4,
                u + s * 0.45f + 3, v + s * 0.7f + 4, u + s * 0.2f + 3, v + s * 0.7f + 4);
        f.poly(0xFFF2F2EE, u - s * 0.1f, v, u + s * 0.2f, v - s * 0.7f, u + s * 0.45f, v - s * 0.7f, u + s * 0.3f, v,
                u + s * 0.45f, v + s * 0.7f, u + s * 0.2f, v + s * 0.7f);
        f.poly(0xFFF2F2EE, u + s * 0.75f, v, u + s, v - s * 0.35f, u + s * 1.1f, v - s * 0.35f, u + s * 1.05f, v,
                u + s * 1.1f, v + s * 0.35f, u + s, v + s * 0.35f);
        f.roundRect(0xFFF6F6F2, u - s, v - s * 0.12f, u + s * 1.1f, v + s * 0.12f, s * 0.12f);
        f.roundRect(0xFF2A4A6A, u - s * 0.6f, v - s * 0.07f, u - s * 0.15f, v + s * 0.07f, s * 0.07f);
        f.rect(0xFF2A4A8A, u + s * 0.25f, v - s * 0.6f, u + s * 0.4f, v - s * 0.5f);
    }
}
