package com.instantaxtion.zombiesandbox;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.Landmarks.*;
import static com.instantaxtion.zombiesandbox.LandmarkArt12.F;
import static com.instantaxtion.zombiesandbox.LandmarkArt12.at;
import static com.instantaxtion.zombiesandbox.LandmarkArt12.bx;
import static com.instantaxtion.zombiesandbox.LandmarkArt12.pl;
import static com.instantaxtion.zombiesandbox.LandmarkArt12.person;
import static com.instantaxtion.zombiesandbox.LandmarkArt12.CLOTHES;
import static com.instantaxtion.zombiesandbox.LandmarkArt12.parasol;
import static com.instantaxtion.zombiesandbox.LandmarkArt12.flowers;
import static com.instantaxtion.zombiesandbox.LandmarkArt12.ball;
import static com.instantaxtion.zombiesandbox.LandmarkArt12.hip;

/**
 * ((25.0) Landmark designs, part 13 (see Landmarks): Theme Park Island's own - the park's rides and halls, the resort's
 * hotels and pool clubs, the staff town's clinic and housing. None of them suits any district (districts 0), so no
 * other map ever picks one: City.themeParkPlaces puts each where it goes.
 */
final class LandmarkArt13 {
    private LandmarkArt13() {
    }

    static final float TT = City.T;

    static void register(List<Landmarks.Def> out) {
        bigCoaster(out);
        ferrisWheel(out);
        castle(out);
        gate(out);
        logFlume(out);
        carousel(out);
        dropTower(out);
        teacups(out);
        swingRide(out);
        foodCourt(out);
        pirateShip(out);
        bumperCars(out);
        mouseCoaster(out);
        splashPad(out);
        midway(out);
        arcade(out);
        resortHotel(out);
        towerHotel(out);
        beachVillas(out);
        poolClub(out);
        surfLodge(out);
        staffClinic(out);
        staffHousing(out);
        // (Only ever placed by name, on Theme Park Island.)
        for (int i = out.size() - 23; i < out.size(); i++) out.get(i).districts = 0;
    }

    // ------------------------------------------------------------------ shared bits

    /** Park paving: warm slabs with faint joints every step. */
    static void paving(F f, int col, float u0, float v0, float u1, float v1, float step) {
        f.rect(col, u0, v0, u1, v1);
        for (float u = u0 + step; u < u1; u += step) f.line(0x14000000, 0.5f, u, v0, u, v1);
        for (float v = v0 + step; v < v1; v += step) f.line(0x10000000, 0.5f, u0, v, u1, v);
    }

    /** A swimming pool: coping, deep blue water lighter at the edge, lane glints. */
    static void pool(F f, float u0, float v0, float u1, float v1, float rad) {
        f.roundRect(0xFFE8E2D4, u0 - 2, v0 - 2, u1 + 2, v1 + 2, rad + 2);
        f.roundRect(0xFF5ABED8, u0, v0, u1, v1, rad);
        f.roundRect(0xFF3A9EC8, u0 + 2, v0 + 2, u1 - 1, v1 - 1, Math.max(0, rad - 1));
        for (float u = u0 + 4; u < u1 - 4; u += 7) f.line(0x60FFFFFF, 0.6f, u, v0 + 3 + (u % 5), u + 3, v0 + 3 + (u % 5));
        f.line(0x40FFFFFF, 0.8f, u0 + 1.5f, v0 + 1.5f, u1 - 3, v0 + 1.5f);
    }

    /** A sun lounger, its back up, along u or across. */
    static void lounger(F f, float u, float v, boolean alongU, int col) {
        if (alongU) {
            f.rect(0x30000000, u - 4 + 0.8f, v - 1.4f + 0.8f, u + 4 + 0.8f, v + 1.4f + 0.8f);
            f.rect(col, u - 4, v - 1.4f, u + 4, v + 1.4f);
            f.rect(City.darken(col, 0.8f), u + 1.5f, v - 1.4f, u + 4, v + 1.4f);
        } else {
            f.rect(0x30000000, u - 1.4f + 0.8f, v - 4 + 0.8f, u + 1.4f + 0.8f, v + 4 + 0.8f);
            f.rect(col, u - 1.4f, v - 4, u + 1.4f, v + 4);
            f.rect(City.darken(col, 0.8f), u - 1.4f, v + 1.5f, u + 1.4f, v + 4);
        }
    }

    /** A palm from above, with its shadow. */
    static void palm(F f, float u, float v, float r) {
        f.circle(0x30000000, u + r * 0.5f, v + r * 0.55f, r * 0.8f);
        City.desertTree(f.c, f.p, f.x(u, v), f.y(u, v), r, Country.TK_PALM, 1, u * 0.13f + v * 0.07f);
    }

    /** A roof's plant: a few grey boxes with shadows. */
    static void plant(F f, float u, float v, float s) {
        f.rect(0x38000000, u + 1, v + 1, u + s + 1, v + s * 0.7f + 1);
        f.rect(0xFFB8BCC0, u, v, u + s, v + s * 0.7f);
        f.circle(0xFF8A9096, u + s * 0.5f, v + s * 0.35f, s * 0.25f);
    }

    /** A flat roof with a parapet, a few plant boxes and a hatch. */
    static void roofDeck(F f, int col, Random r) {
        float L = f.L, S = f.S;
        f.rect(City.darken(col, 0.72f), 0, 0, L, S);
        f.rect(col, 1.6f, 1.6f, L - 1.6f, S - 1.6f);
        f.line(City.lighten(col, 0.2f), 0.8f, 1.6f, 1.6f, L - 1.6f, 1.6f);
        int n = Math.max(1, (int) (L / 28));
        for (int k = 0; k < n; k++) plant(f, L * (k + 0.5f) / n - 3, S * 0.25f + r.nextFloat() * S * 0.3f, 5);
        f.rect(0xFF6A6E74, L - 6, S - 6, L - 3, S - 3);
    }

    /** A little striped kiosk roof (snacks, tickets) over u0..u1, v0..v1. */
    static void stripes(F f, float u0, float v0, float u1, float v1, int c1, int c2) {
        f.rect(0x38000000, u0 + 1.2f, v0 + 1.2f, u1 + 1.2f, v1 + 1.2f);
        float w = Math.max(1.5f, (u1 - u0) / 6);
        int k = 0;
        for (float u = u0; u < u1 - 0.01f; u += w, k++) f.rect(k % 2 == 0 ? c1 : c2, u, v0, Math.min(u1, u + w), v1);
        f.line(0x40000000, 0.6f, u0, (v0 + v1) / 2, u1, (v0 + v1) / 2);
    }

    /** People scattered about an area, clear of the edges. */
    static void crowd(F f, Random r, float u0, float v0, float u1, float v1, int n) {
        // (Never thicker than a person to every twenty square tiles or so.)
        n = Math.min(n, (int) ((u1 - u0) * (v1 - v0) / 5000) + 1);
        for (int k = 0; k < n; k++)
            person(f, u0 + 2 + r.nextFloat() * Math.max(1, u1 - u0 - 4), v0 + 2 + r.nextFloat() * Math.max(1, v1 - v0 - 4), CLOTHES[r.nextInt(8)]);
    }

    /**
     * A ride's plot: paving, four lawns round a round paved apron (radius R) at (cu, cv) with paths between them out
     * to each side, trees, bushes and flower beds on the lawns, lamps round the apron.
     */
    static void plot(F f, Random r, float cu, float cv, float R) {
        float L = f.L, S = f.S;
        paving(f, 0xFFE0D4BA, 0, 0, L, S, 8);
        float pw = Math.max(8, Math.min(14, S * 0.06f)), e = 5;
        float[][] lawns = {{e, e, cu - pw, cv - pw}, {cu + pw, e, L - e, cv - pw}, {e, cv + pw, cu - pw, S - e}, {cu + pw, cv + pw, L - e, S - e}};
        for (float[] l : lawns) {
            if (l[2] - l[0] < 8 || l[3] - l[1] < 8) continue;
            f.roundRect(0xFF8A9A6A, l[0] - 1, l[1] - 1, l[2] + 1, l[3] + 1, 7);
            f.roundRect(0xFF6EA052, l[0], l[1], l[2], l[3], 6);
        }
        f.circle(0xFFC8BA9C, cu, cv, R + 10);
        f.circle(0xFFE8DEC8, cu, cv, R + 8.5f);
        int[][] bed = {{0xFFE05060, 0xFFF0D050}, {0xFF8050D0, 0xFFF2F2F2}, {0xFFF08040, 0xFFF8E080}};
        for (float u = 14; u < L - 8; u += 22)
            for (float v = 14; v < S - 8; v += 22) {
                float ju = u + (r.nextFloat() - 0.5f) * 8, jv = v + (r.nextFloat() - 0.5f) * 8;
                if (Math.abs(ju - cu) < pw + 8 || Math.abs(jv - cv) < pw + 8) continue;
                if (Math.hypot(ju - cu, jv - cv) < R + 18 || ju < 11 || jv < 11 || ju > L - 11 || jv > S - 11) continue;
                int k = r.nextInt(7);
                if (k < 4) f.tree(ju, jv, 5.5f + r.nextFloat() * 2.5f, k == 0 ? 0xFF3E7034 : k == 1 ? 0xFF5A8A3E : 0xFF4A7E3C);
                else if (k == 4) {
                    int[] b = bed[r.nextInt(bed.length)];
                    flowers(f, ju - 6, jv - 3.5f, ju + 6, jv + 3.5f, b[0], b[1]);
                } else f.bush(ju, jv, 2.8f);
            }
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4 + k * Math.PI / 2;
            f.lamp(cu + (float) Math.cos(a) * (R + 9), cv + (float) Math.sin(a) * (R + 9));
        }
        if (cu - R - 14 > pw) f.bench(cu - R - 14, cv - pw + 2, true);
        if (cu + R + 14 < L - pw) f.bench(cu + R + 14, cv + pw - 2, true);
    }

    /** Point k of the closed track q (u, v, height triples), wrapped. */
    static float tq(float[] q, int k, int j) {
        int m = q.length / 3;
        return q[((k % m + m) % m) * 3 + j];
    }

    /**
     * A steel coaster track along the closed loop q (u, v, height): its shadow on the ground, supports where it is up
     * high, ties, a spine and two rails; the lift hill's chain between points lift0 and lift1.
     */
    static void track(F f, float[] q, int rail, int spine, float gauge, int lift0, int lift1) {
        int m = q.length / 3;
        float[] s = new float[m * 2], ce = new float[m * 2], ra = new float[m * 2], rb = new float[m * 2];
        for (int k = 0; k < m; k++) {
            float u = q[3 * k], v = q[3 * k + 1], hgt = q[3 * k + 2];
            s[2 * k] = u + hgt * 0.45f;
            s[2 * k + 1] = v + hgt * 0.45f;
            float du = tq(q, k + 1, 0) - tq(q, k - 1, 0), dv = tq(q, k + 1, 1) - tq(q, k - 1, 1), l = (float) Math.sqrt(du * du + dv * dv);
            if (l < 0.001f) l = 1;
            du /= l;
            dv /= l;
            ce[2 * k] = u;
            ce[2 * k + 1] = v;
            ra[2 * k] = u + dv * gauge;
            ra[2 * k + 1] = v - du * gauge;
            rb[2 * k] = u - dv * gauge;
            rb[2 * k + 1] = v + du * gauge;
        }
        f.stroke(0x2A000000, gauge * 2.2f, s, true);
        for (int k = 0; k < m; k += 3) {
            if (q[3 * k + 2] < 4) continue;
            f.line(0x38000000, 1.2f, ce[2 * k], ce[2 * k + 1], s[2 * k], s[2 * k + 1]);
            f.rect(0xFF5A6068, s[2 * k] - 1.1f, s[2 * k + 1] - 1.1f, s[2 * k] + 1.1f, s[2 * k + 1] + 1.1f);
            f.line(0xFF9AA0A8, 1f, ce[2 * k], ce[2 * k + 1], s[2 * k], s[2 * k + 1]);
        }
        f.stroke(spine, gauge * 1.1f, ce, true);
        for (int k = 0; k < m; k++) f.line(0xFF5A5E66, 0.6f, ra[2 * k], ra[2 * k + 1], rb[2 * k], rb[2 * k + 1]);
        f.stroke(rail, Math.max(0.9f, gauge * 0.5f), ra, true);
        f.stroke(rail, Math.max(0.9f, gauge * 0.5f), rb, true);
        for (int k = lift0; k < lift1; k++)
            f.line(0xFFE8D060, 0.5f, tq(q, k, 0), tq(q, k, 1), tq(q, k + 1, 0), tq(q, k + 1, 1));
    }

    /** A coaster train of n cars at point k of the closed track q, nose along the track. */
    static void train(F f, float[] q, int k, int n, int col, float size) {
        int m = q.length / 3;
        // (Cars a fixed distance apart along the track, however finely it's cut.)
        float step = size * 2.4f;
        int at = k;
        for (int c = 0; c < n; c++) {
            float u = tq(q, at, 0), v = tq(q, at, 1);
            float du = tq(q, at + 1, 0) - tq(q, at - 1, 0), dv = tq(q, at + 1, 1) - tq(q, at - 1, 1), l = (float) Math.sqrt(du * du + dv * dv);
            if (l < 0.001f) return;
            du /= l;
            dv /= l;
            float hl = size, hw = size * 0.72f, hg = tq(q, at, 2);
            float su = u + hg * 0.45f, sv = v + hg * 0.45f;
            f.poly(0x30000000, su - du * hl + dv * hw, sv - dv * hl - du * hw, su + du * hl + dv * hw, sv + dv * hl - du * hw,
                    su + du * hl - dv * hw, sv + dv * hl + du * hw, su - du * hl - dv * hw, sv - dv * hl + du * hw);
            f.poly(c == 0 ? City.darken(col, 0.7f) : col, u - du * hl + dv * hw, v - dv * hl - du * hw, u + du * hl + dv * hw, v + dv * hl - du * hw,
                    u + du * hl - dv * hw, v + dv * hl + du * hw, u - du * hl - dv * hw, v - dv * hl + du * hw);
            f.circle(0xFF2A2A2E, u + dv * hw * 0.45f, v - du * hw * 0.45f, hw * 0.33f);
            f.circle(0xFF2A2A2E, u - dv * hw * 0.45f, v + du * hw * 0.45f, hw * 0.33f);
            // The next car back: walk back along the track by step.
            float walked = 0;
            int guard = 0;
            while (walked < step && guard++ < m) {
                float a0 = tq(q, at, 0) - tq(q, at - 1, 0), a1 = tq(q, at, 1) - tq(q, at - 1, 1);
                walked += (float) Math.sqrt(a0 * a0 + a1 * a1);
                at--;
            }
        }
    }

    /** A vertical loop on the track at (u, v), its axis along the track direction (du, dv): a thin ring seen from above. */
    static void loop(F f, float u, float v, float du, float dv, float r, int rail) {
        float l = (float) Math.sqrt(du * du + dv * dv);
        du /= l;
        dv /= l;
        int n = 24;
        float[] ring = new float[n * 2], sh = new float[n * 2];
        for (int k = 0; k < n; k++) {
            double a = k * Math.PI * 2 / n;
            float along = (float) Math.cos(a) * r, side = (float) Math.sin(a) * r * 0.22f, up = (float) (1 - Math.cos(a + Math.PI)) * r * 0.5f;
            ring[2 * k] = u + du * along - dv * side;
            ring[2 * k + 1] = v + dv * along + du * side;
            sh[2 * k] = ring[2 * k] + up * 0.9f;
            sh[2 * k + 1] = ring[2 * k + 1] + up * 0.9f;
        }
        float k = Math.max(1, r / 14);
        f.stroke(0x30000000, 2.6f * k, sh, true);
        f.stroke(0xFF4A4E56, 3.2f * k, ring, true);
        f.stroke(rail, 1.2f * k, ring, true);
    }

    /** A fence of posts and a rail round a rectangle. */
    static void fence(F f, float u0, float v0, float u1, float v1) {
        f.frame(0xFF6A6E76, 0.7f, u0, v0, u1, v1);
        for (float u = u0; u <= u1; u += 4) {
            f.circle(0xFF4A4E56, u, v0, 0.6f);
            f.circle(0xFF4A4E56, u, v1, 0.6f);
        }
        for (float v = v0; v <= v1; v += 4) {
            f.circle(0xFF4A4E56, u0, v, 0.6f);
            f.circle(0xFF4A4E56, u1, v, 0.6f);
        }
    }

    /** A queue maze: rows of rails with people in them. */
    static void queue(F f, float u0, float v0, float u1, float v1, int seed) {
        f.rect(0xFFC8BCA0, u0, v0, u1, v1);
        for (float v = v0 + 2.5f; v < v1 - 0.5f; v += 2.5f) f.line(0xFF7A7E86, 0.5f, u0 + ((int) (v / 2.5f) % 2 == 0 ? 0 : 2.5f), v, u1 - ((int) (v / 2.5f) % 2 == 0 ? 2.5f : 0), v);
        f.frame(0xFF6A6E76, 0.6f, u0, v0, u1, v1);
        Random r = new Random(seed);
        int n = (int) ((u1 - u0) * (v1 - v0) / 40);
        for (int k = 0; k < n; k++) {
            float u = u0 + 2 + r.nextFloat() * (u1 - u0 - 4), v = v0 + 1.25f + 2.5f * r.nextInt(Math.max(1, (int) ((v1 - v0) / 2.5f)));
            person(f, u, v, CLOTHES[r.nextInt(8)]);
        }
    }

    /** Planted beds and trees round the edge of a ride's plot, leaving the middle and the paths clear. */
    static void edging(F f, Random r, float inset) {
        float L = f.L, S = f.S;
        for (float u = 6; u < L - 4; u += 11 + r.nextFloat() * 4) {
            f.bush(u, inset, 2.2f + r.nextFloat());
            f.bush(u + 4, S - inset, 2.2f + r.nextFloat());
        }
    }

    /** A turning ride's round platform: a ring of planks round a hub, light from the top left. */
    static void platform(F f, float u, float v, float r, int col) {
        f.circle(0x38000000, u + 2, v + 2.4f, r);
        f.circle(City.darken(col, 0.75f), u, v, r);
        f.circle(col, u - 0.4f, v - 0.4f, r - 1.2f);
        f.ring(City.lighten(col, 0.2f), 0.6f, u - 0.4f, v - 0.4f, r - 2.4f);
    }

    // ================================================================== the park

    static void bigCoaster(List<Landmarks.Def> out) {
        // The park's great coaster: a long loop of red track round the whole plot, a lift hill climbing out of the
        // station, two dips into the middle, two vertical loops and a helix, over a rocky mountain and its lake.
        Landmarks.Def d = new Landmarks.Def("Thunder Mountain Coaster", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, 3, S - 6, Math.max(5, L / 7), 4, 1), at(al, L - 7, S - 6, 4, 4, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] pq = pl(this, x0, y0, x1, y1);
                float[] st = bx(f, pq[0]), sp = bx(f, pq[1]);
                f.rect(0xFF6E9E50, 0, 0, L, S);
                for (int k = 0; k < (int) (L * S / 400); k++) f.circle(0x12000000, r.nextFloat() * L, r.nextFloat() * S, 3 + r.nextFloat() * 5);
                // The midway along the front.
                float pv = st[1] - 9, g = Math.max(1, S / 420);
                paving(f, 0xFFDCCCA8, 0, pv, L, S, 8);
                f.line(0x30000000, 0.8f, 0, pv, L, pv);
                // The mountain in the middle: rock in layers, lit from the top left.
                float mu = L * 0.52f, mv = S * 0.42f, mr = Math.min(L * 0.2f, S * 0.24f);
                f.oval(0x40000000, mu - mr + 6, mv - mr * 0.8f + 8, mu + mr + 10, mv + mr * 0.8f + 10);
                int[] rock = {0xFF8A5A3A, 0xFFA06A44, 0xFFB87E52, 0xFFCC9464, 0xFFDCAA78};
                for (int k = 0; k < rock.length; k++) {
                    float sc = 1 - k * 0.18f, ou = -k * mr * 0.06f, ov = -k * mr * 0.07f;
                    float[] pts = new float[18];
                    for (int j = 0; j < 9; j++) {
                        double a = j * Math.PI * 2 / 9 + k * 0.3;
                        float rr = mr * sc * (0.82f + 0.18f * (float) Math.sin(j * 2.3 + k));
                        pts[2 * j] = mu + ou + (float) Math.cos(a) * rr * 1.25f;
                        pts[2 * j + 1] = mv + ov + (float) Math.sin(a) * rr * 0.9f;
                    }
                    f.poly(rock[k], pts);
                }
                f.circle(0xFFEAD0A8, mu - mr * 0.3f, mv - mr * 0.32f, mr * 0.12f);
                // A lake beside it, and trees.
                float lu = L * 0.22f, lv = S * 0.4f, lr = Math.min(L * 0.1f, S * 0.18f);
                pond(c, p, f.x(lu - lr * 1.3f, lv - lr), f.y(lu - lr * 1.3f, lv - lr), f.x(lu + lr * 1.3f, lv + lr), f.y(lu + lr * 1.3f, lv + lr));
                // The track.
                float vb = (st[1] + st[3]) / 2, top = 10, a1 = st[2];
                float hb = Math.min(40, S * 0.12f + 10);
                float[] cp = {
                        st[0] - 6, vb, 2,
                        a1 + 10, vb, 2,
                        L * 0.55f, pv - 6, hb * 0.5f,
                        L - 16, pv - 10, hb,
                        L - 11, S * 0.45f, hb * 0.9f,
                        L - 16, top + 2, hb * 0.7f,
                        L * 0.82f, top, hb * 0.5f,
                        L * 0.72f, S * 0.5f, 8,
                        L * 0.62f, top + 4, hb * 0.45f,
                        L * 0.45f, top, hb * 0.6f,
                        L * 0.36f, S * 0.62f, 6,
                        L * 0.28f, top + 2, hb * 0.4f,
                        14, top + 2, hb * 0.5f,
                        10, S * 0.45f, hb * 0.3f,
                        L * 0.16f, pv - 14 * g, 10,
                        L * 0.36f, pv - 34 * g, hb * 0.45f,
                        L * 0.56f, S * 0.66f, hb * 0.35f,
                        L * 0.74f, pv - 22 * g, 8,
                        L * 0.6f, pv - 8 * g, 5,
                        L * 0.3f, pv - 6 * g, 4,
                        10, vb - 8, 6};
                float[] q = LandmarkArt12.chaikin(cp, 4);
                int m = q.length / 3;
                // Trees wherever the track leaves room.
                for (float tu = 8; tu < L - 6; tu += 18 * g)
                    for (float tv = 8; tv < pv - 6; tv += 17 * g) {
                        float ju = tu + (r.nextFloat() - 0.5f) * 14 * g, jv = tv + (r.nextFloat() - 0.5f) * 14 * g, bd = 1e9f;
                        if (r.nextFloat() < 0.3f) continue;
                        for (int k = 0; k < m; k++) {
                            float du = q[3 * k] - ju, dv = q[3 * k + 1] - jv;
                            bd = Math.min(bd, du * du + dv * dv);
                        }
                        float md = ((ju - mu) * (ju - mu)) / 1.6f + (jv - mv) * (jv - mv), ld = (ju - lu) * (ju - lu) / 1.7f + (jv - lv) * (jv - lv);
                        if (bd > 120 * g * g && md > mr * mr * 1.3f && ld > lr * lr * 1.6f) f.tree(ju, jv, (4.5f + r.nextFloat() * 3) * (0.6f + 0.4f * g), r.nextBoolean() ? 0xFF4A7E3C : 0xFF3E7034);
                    }
                // A helix inside the right end: rings of track one over another.
                float hu = L * 0.8f, hv = S * 0.5f, hr = Math.min(16, S * 0.12f) * g;
                if (L > 120) {
                    for (int k = 0; k < 3; k++) f.ring(0x26000000, 2.4f * g, hu + (4 + k * 3) * g, hv + (5 + k * 3) * g, hr);
                    for (int k = 2; k >= 0; k--) {
                        f.ring(0xFF4A4E56, 2.8f * g, hu, hv, hr - k * 0.6f * g);
                        f.ring(0xFFC8402E, 0.9f * g, hu, hv, hr - (k * 0.6f - 1.4f) * g);
                        f.ring(0xFFC8402E, 0.9f * g, hu, hv, hr - (k * 0.6f + 1.4f) * g);
                    }
                }
                track(f, q, 0xFFC8402E, 0xFF4A4E56, 2.4f * g, m * 2 / 16, m * 5 / 16);
                // Two vertical loops on the long straights.
                int k1 = m * 9 / 16, k2 = m * 15 / 32;
                for (int kk : new int[]{k1, k2}) {
                    float du = tq(q, kk + 1, 0) - tq(q, kk - 1, 0), dv = tq(q, kk + 1, 1) - tq(q, kk - 1, 1);
                    loop(f, tq(q, kk, 0), tq(q, kk, 1), du, dv, Math.min(14, S * 0.09f + 4) * g, 0xFFC8402E);
                }
                train(f, q, m * 3 / 16, 6, 0xFF2E5AA0, 3.2f * g);
                train(f, q, m * 13 / 16, 6, 0xFFE0B030, 3.2f * g);
                // The queue maze in front of the station, snack stalls and people on the midway.
                queue(f, st[0], pv + 2, st[2], st[1] - 1.5f, 7);
                for (float u = st[2] + 10; u + 12 < sp[0] - 6; u += 30) {
                    stripes(f, u, pv + 3, u + 10, pv + 9, 0xFFE8B040, 0xFFF2EADA);
                    parasol(f, u + 16, pv + 6, 3.5f, 0xFFD84040, 0xFFF2EADA);
                }
                crowd(f, r, st[2], pv, L, S, (int) (L / 6));
                for (float u = 10; u < L; u += 26) f.lamp(u, S - 2.5f);
                f.sign(city, "THUNDER MOUNTAIN", mu, mv + mr + 8, Math.min(14, L / 16), 0xFFF2E6C8);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                if (part == 0) {
                    // The station: a long pitched roof, red and cream.
                    hip(f, 0xFFB8483A, 0, 0, f.L, f.S, false);
                    f.line(0xFFF2E6C8, 1f, 2, f.S / 2, f.L - 2, f.S / 2);
                } else {
                    hip(f, 0xFF3E7A9A, 0, 0, f.L, f.S, false);
                    f.circle(0xFFE8C040, f.L / 2, f.S / 2, 1.6f);
                }
            }
        };
        d.ground = City.GRASS;
        d.minS = 16;
        d.minL = 20;
        d.base = City.SHOP;
        d.inside = Variants.I_ARCADE;
        d.roofCol = 0xFFB8483A;
        out.add(d);
    }

    static void ferrisWheel(List<Landmarks.Def> out) {
        // A great wheel on a paved plaza: its long shadow across the ground, the A-frame legs, the rim and spokes, and a
        // ring of coloured gondolas; flower beds, a ticket booth and the queue.
        Landmarks.Def d = new Landmarks.Def("Ferris Wheel", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, L - 5, S - 4, 3, 3, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                paving(f, 0xFFDCD0B4, 0, 0, L, S, 8);
                float cu = Math.min(L * 0.45f, L - S * 0.5f - 2), cv = S * 0.46f, R = Math.min(S * 0.42f, L * 0.4f);
                if (cu < R + 4) cu = R + 4;
                plot(f, r, cu, cv, R + 6);
                f.circle(0xFFC8B898, cu, cv, R + 4);
                f.ring(0xFFAA9A7A, 1f, cu, cv, R + 4);
                // The shadow, long to the lower right.
                float so = R * 0.55f;
                f.ring(0x30000000, 3f, cu + so, cv + so * 1.1f, R);
                for (int k = 0; k < 8; k++) {
                    double a = k * Math.PI / 8;
                    f.line(0x22000000, 1f, cu + so + (float) Math.cos(a) * R, cv + so * 1.1f + (float) Math.sin(a) * R,
                            cu + so - (float) Math.cos(a) * R, cv + so * 1.1f - (float) Math.sin(a) * R);
                }
                // The legs: an A-frame each side down to the ground.
                f.line(0xFF7A8088, 2.4f, cu, cv, cu - R * 0.5f, cv + R * 1.05f);
                f.line(0xFF7A8088, 2.4f, cu, cv, cu + R * 0.5f, cv + R * 1.05f);
                f.line(0xFF9AA0A8, 1.2f, cu - R * 0.25f, cv + R * 0.52f, cu + R * 0.25f, cv + R * 0.52f);
                // Spokes and the rim.
                int n = 16;
                for (int k = 0; k < n; k++) {
                    double a = k * Math.PI * 2 / n;
                    f.line(0xFFD8DCE0, 0.8f, cu, cv, cu + (float) Math.cos(a) * R, cv + (float) Math.sin(a) * R);
                }
                f.ring(0xFF5A6068, 3f, cu, cv, R);
                f.ring(0xFFF2F2EE, 1.4f, cu, cv, R);
                f.ring(0xFFF2F2EE, 0.8f, cu, cv, R * 0.62f);
                f.ring(0xFFE8C040, 0.6f, cu, cv, R + 1.6f);
                // The gondolas round the rim.
                int[] cab = {0xFFD84040, 0xFF3A8AD0, 0xFFE8B030, 0xFF40A860, 0xFFC050B0, 0xFFE87830};
                float gr = Math.max(2.2f, R * 0.09f);
                for (int k = 0; k < n; k++) {
                    double a = k * Math.PI * 2 / n + Math.PI / n;
                    float gu = cu + (float) Math.cos(a) * R, gv = cv + (float) Math.sin(a) * R;
                    f.circle(0x40000000, gu + 1.2f, gv + 1.6f, gr);
                    f.circle(City.darken(cab[k % cab.length], 0.75f), gu, gv, gr);
                    f.circle(cab[k % cab.length], gu - 0.3f, gv - 0.3f, gr * 0.75f);
                    f.circle(0x70FFFFFF, gu - gr * 0.35f, gv - gr * 0.35f, gr * 0.28f);
                }
                // The hub.
                f.circle(0xFF4A4E56, cu, cv, Math.max(2.5f, R * 0.09f));
                f.circle(0xFFE8C040, cu - 0.4f, cv - 0.4f, Math.max(1.4f, R * 0.05f));
                // Beds, the queue, people.
                float bu = cu + R + 10;
                if (bu + 14 < L - 6) {
                    flowers(f, bu, 6, Math.min(L - 6, bu + 20), 16, 0xFFE05060, 0xFFF0D050);
                    queue(f, bu, S * 0.5f, Math.min(L - 8, bu + 24), S - 10, 11);
                }
                flowers(f, 4, 4, 18, 12, 0xFFC050B0, 0xFFF2F2F2);
                flowers(f, 4, S - 12, 18, S - 4, 0xFF4A90E0, 0xFFF0D050);
                crowd(f, r, 0, 0, L, S, (int) (L * S / 900) + 3);
                for (float u = 8; u < L; u += 24) f.lamp(u, 2.5f);
                f.sign(city, "FERRIS WHEEL", cu, cv + R + 9, Math.min(12, S / 9), 0xFFF2E6C8);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                stripes(f, 0, 0, f.L, f.S, 0xFFD84040, 0xFFF2EADA);
            }
        };
        d.ground = City.PLAZA;
        d.minS = 14;
        d.minL = 14;
        d.base = City.KIOSK;
        d.inside = Variants.I_SHELVES;
        d.roofCol = 0xFFD84040;
        out.add(d);
    }

    static void castle(List<Landmarks.Def> out) {
        // The park's heart: a fairytale castle on a lawn inside its moat, round towers with pointed blue roofs, a
        // bridge to the gate, flower beds and lamps round the plaza.
        Landmarks.Def d = new Landmarks.Def("Fairytale Castle", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int m = Math.min(bw, bh), s = Math.max(6, Math.min(m - 8, Math.round(m * 0.5f)));
                return new int[][]{{(bw - s) / 2, (bh - s) / 2, s, s, 3}};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] pq = pl(this, x0, y0, x1, y1);
                float[] k = bx(f, pq[0]);
                float cu = (k[0] + k[2]) / 2, cv = (k[1] + k[3]) / 2, hs = (k[2] - k[0]) / 2, mr = Math.min(hs * 1.45f + 6, S / 2 - 6);
                plot(f, r, cu, cv, mr + 4);
                // Lawn and the moat round the keep.
                f.circle(0xFF6EA052, cu, cv, mr + 4);
                f.circle(0xFF8A7A5A, cu, cv, mr + 0.8f);
                f.circle(0xFF3F7FA8, cu, cv, mr);
                f.circle(0xFF2F6A94, cu, cv, mr - 2);
                f.ring(0x40FFFFFF, 0.6f, cu, cv, mr - 3.5f);
                f.circle(0xFF7AAA5C, cu, cv, Math.max(hs + 2, mr - 6));
                // The bridge over the moat, out to the plaza in front.
                f.rect(0x38000000, cu - 6 + 1.5f, cv + hs + 1.5f, cu + 6 + 1.5f, cv + mr + 6 + 1.5f);
                f.rect(0xFFB8A888, cu - 6, cv + hs, cu + 6, cv + mr + 6);
                for (float v = cv + hs + 1; v < cv + mr + 6; v += 2.4f) f.line(0x30000000, 0.5f, cu - 6, v, cu + 6, v);
                f.line(0xFF7A6A50, 0.8f, cu - 6, cv + hs, cu - 6, cv + mr + 6);
                f.line(0xFF7A6A50, 0.8f, cu + 6, cv + hs, cu + 6, cv + mr + 6);
                crowd(f, r, 0, 0, L, S, 30);
                f.sign(city, "FAIRYTALE CASTLE", cu, Math.min(S - 6, cv + mr + 12), Math.min(12, S / 8), 0xFFF2E6C8);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, t = Math.max(8, L * 0.24f);
                // Walls round a courtyard, battlements, and the keep.
                f.rect(0xFFB0A89E, 0, 0, L, S);
                f.rect(0xFFDCD4CA, 2, 2, L - 2, S - 2);
                for (float u = 3; u < L - 2; u += 4) {
                    f.rect(0xFFA0988E, u, 0.5f, u + 2, 2);
                    f.rect(0xFFA0988E, u, S - 2, u + 2, S - 0.5f);
                    f.rect(0xFFA0988E, 0.5f, u, 2, u + 2);
                    f.rect(0xFFA0988E, L - 2, u, L - 0.5f, u + 2);
                }
                f.rect(0xFFC4BCB0, t * 0.55f, t * 0.55f, L - t * 0.55f, S - t * 0.55f);
                hip(f, 0xFF4A70C0, L * 0.28f, S * 0.28f, L * 0.72f, S * 0.72f, true);
                float[][] tw = {{t / 2, t / 2}, {L - t / 2, t / 2}, {t / 2, S - t / 2}, {L - t / 2, S - t / 2}};
                for (float[] w : tw) cone(f, w[0], w[1], t / 2 + 0.5f, 0xFF3A62B8);
                cone(f, L / 2, S - t * 0.3f, t * 0.32f, 0xFF3A62B8);
                cone(f, L / 2, S / 2, Math.max(4, L * 0.13f), 0xFFC8508A);
                cone(f, L * 0.36f, S * 0.36f, Math.max(2.5f, L * 0.06f), 0xFFC8508A);
                cone(f, L * 0.64f, S * 0.36f, Math.max(2.5f, L * 0.06f), 0xFFC8508A);
            }
        };
        d.ground = City.PLAZA;
        d.minS = 14;
        d.minL = 14;
        d.base = City.OFFICE;
        d.inside = Variants.I_SEATS;
        d.roofCol = 0xFF4A70C0;
        out.add(d);
    }

    /** A pointed tower roof from above: gores shaded round a finial. */
    static void cone(F f, float u, float v, float r, int col) {
        f.circle(0x40000000, u + r * 0.4f, v + r * 0.5f, r);
        LandmarkArt12.dome(f, u, v, r, col, 8);
        f.ring(City.darken(col, 0.6f), 0.5f, u, v, r);
        f.circle(0xFFE8C040, u, v, Math.max(0.6f, r * 0.15f));
    }

    static void gate(List<Landmarks.Def> out) {
        // The main gate: two ticket halls either side of the way in, turnstiles and an arch with the park's name across
        // it, flags along the front, a plaza inside with a fountain and flower beds.
        Landmarks.Def d = new Landmarks.Def("Park Gate", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int g = Math.max(4, L / 5), hl = Math.max(3, (L - g) / 2 - 2);
                return new int[][]{at(al, 2, S - 5, hl, 3, 2), at(al, L - 2 - hl, S - 5, hl, 3, 2)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] pq = pl(this, x0, y0, x1, y1);
                float[] a = bx(f, pq[0]), b = bx(f, pq[1]);
                float fu = L / 2, fv = Math.max(14, a[1] * 0.45f), fr = Math.max(6, Math.min(26, a[1] * 0.28f));
                plot(f, r, fu, fv, fr);
                // The way in between the halls: red paving out to the street, turnstiles under the arch.
                f.rect(0xFFC86A5A, a[2] + 2, a[1] - 6, b[0] - 2, S);
                for (float v = a[1] - 4; v < S; v += 4) f.line(0x20000000, 0.5f, a[2] + 2, v, b[0] - 2, v);
                float gv = a[1] + 6;
                for (float u = a[2] + 5; u < b[0] - 3; u += 5) {
                    f.rect(0xFF8A9098, u - 0.6f, gv - 3, u + 0.6f, gv + 3);
                    f.circle(0xFFC8CCD0, u, gv, 1.2f);
                }
                f.rect(0x40000000, a[2] + 1, a[1] + 1, b[0] + 2, a[1] + 5);
                f.rect(0xFF3A6AB0, a[2] - 1, a[1], b[0] + 1, a[1] + 3.5f);
                f.rect(0xFFE8C040, a[2] - 1, a[1] + 1.4f, b[0] + 1, a[1] + 2.2f);
                // The fountain.
                f.circle(0xFFC8BCA0, fu, fv, fr);
                f.circle(0xFF4A9AC8, fu, fv, fr - 1.5f);
                f.ring(0x70FFFFFF, 0.6f, fu, fv, fr * 0.6f);
                f.circle(0xFFE8E4DC, fu, fv, fr * 0.22f);
                for (int k = 0; k < 8; k++) {
                    double an = k * Math.PI / 4;
                    f.circle(0x90FFFFFF, fu + (float) Math.cos(an) * fr * 0.4f, fv + (float) Math.sin(an) * fr * 0.4f, 0.9f);
                }
                int[] flag = {0xFFD84040, 0xFF3A8AD0, 0xFFE8B030, 0xFF40A860};
                for (float u = 6; u < L - 4; u += 10) {
                    if (u > a[2] && u < b[0]) continue;
                    f.tall(u, S - 2, 0.5f, 8);
                    f.circle(0xFF5A5E66, u, S - 2, 0.8f);
                    f.rect(flag[(int) (u / 10) % 4], u + 0.8f, S - 5, u + 6, S - 2.2f);
                }
                crowd(f, r, a[2], a[1] - 10, b[0], S, 14);
                crowd(f, r, 0, 0, L, a[1] - 4, 20);
                f.sign(city, "ADVENTURE PARK", L / 2, a[1] - 3, Math.min(13, L / 10), 0xFFF2E6C8);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                LandmarkArt12.gable(f, 0xFFC8483C, 0, 0, L, S);
                for (float u = 4; u < L - 2; u += 8) f.line(0x30FFFFFF, 0.6f, u, 0, u, S);
                cone(f, part == 0 ? L - S / 2 : S / 2, S / 2, S * 0.38f, 0xFF3A6AB0);
            }
        };
        d.ground = City.PLAZA;
        d.minS = 8;
        d.minL = 10;
        d.base = City.SHOP;
        d.inside = Variants.I_SHELVES;
        d.roofCol = 0xFFC8483C;
        out.add(d);
    }

    static void logFlume(List<Landmarks.Def> out) {
        // A log flume: a channel of white water winding round the plot, a lift up to a high trough, the big drop into a
        // splash pool, logs full of riders, the station with its queue, all among rocks and pines.
        Landmarks.Def d = new Landmarks.Def("Log Flume", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, 2, S - 5, 5, 3, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] pq = pl(this, x0, y0, x1, y1);
                float[] st = bx(f, pq[0]);
                f.rect(0xFF5E8E48, 0, 0, L, S);
                for (int k = 0; k < (int) (L * S / 900); k++) {
                    float u = r.nextFloat() * L, v = r.nextFloat() * S;
                    f.circle(0x22000000, u + 1, v + 1, 2 + r.nextFloat() * 3);
                    f.circle(r.nextBoolean() ? 0xFF8A8478 : 0xFF9A9488, u, v, 2 + r.nextFloat() * 3);
                }
                float vb = (st[1] + st[3]) / 2;
                float[] cp = {
                        st[0] - 4, vb, 1,
                        st[2] + 10, vb, 1,
                        L * 0.6f, S - 12, 1,
                        L - 12, S * 0.7f, 1,
                        L - 13, S * 0.35f, 6,
                        L - 20, 12, 16,
                        L * 0.55f, 12, 16,
                        L * 0.42f, S * 0.34f, 2,
                        L * 0.3f, 13, 2,
                        12, 14, 1,
                        12, vb - 8, 1};
                float[] q = LandmarkArt12.chaikin(cp, 4);
                int m = q.length / 3;
                // Pines and boulders inside the loop and round it.
                float pu = L * 0.44f, pvv = S * 0.36f, pr = Math.max(10, Math.min(L * 0.09f, S * 0.12f));
                for (float tu = 10; tu < L - 8; tu += 20)
                    for (float tv = 10; tv < S - 8; tv += 18) {
                        float ju = tu + r.nextFloat() * 8 - 4, jv = tv + r.nextFloat() * 8 - 4, bd = 1e9f;
                        for (int k = 0; k < m; k++) {
                            float du = q[3 * k] - ju, dv = q[3 * k + 1] - jv;
                            bd = Math.min(bd, du * du + dv * dv);
                        }
                        if (bd > 160 && (ju - pu) * (ju - pu) + (jv - pvv) * (jv - pvv) > (pr + 10) * (pr + 10) && r.nextFloat() < 0.75f)
                            f.tree(ju, jv, 4.5f + r.nextFloat() * 2.5f, r.nextBoolean() ? 0xFF2E5E36 : 0xFF3A6E3E);
                    }
                // The splash pool where the drop comes down, foaming.
                f.oval(0xFF8A7A5A, pu - pr * 1.4f - 2, pvv - pr - 2, pu + pr * 1.4f + 2, pvv + pr + 2);
                f.oval(0xFF4A9ACB, pu - pr * 1.4f, pvv - pr, pu + pr * 1.4f, pvv + pr);
                f.oval(0xFF6AB4DC, pu - pr * 0.8f, pvv - pr * 0.6f, pu + pr * 0.6f, pvv + pr * 0.5f);
                for (int k = 0; k < 24; k++) f.circle(0xB0FFFFFF, pu - pr * 0.7f + r.nextFloat() * pr * 1.2f, pvv - pr * 0.5f + r.nextFloat() * pr, 0.8f + r.nextFloat() * 1.8f);
                // The channel: shadow, concrete walls, water with ripples, supports where it's up high.
                float[] s = new float[m * 2], ce = new float[m * 2];
                for (int k = 0; k < m; k++) {
                    ce[2 * k] = q[3 * k];
                    ce[2 * k + 1] = q[3 * k + 1];
                    s[2 * k] = q[3 * k] + q[3 * k + 2] * 0.45f;
                    s[2 * k + 1] = q[3 * k + 1] + q[3 * k + 2] * 0.45f;
                }
                f.stroke(0x30000000, 11, s, true);
                for (int k = 0; k < m; k += 3)
                    if (q[3 * k + 2] > 4) f.line(0xFF7A746A, 1.6f, ce[2 * k], ce[2 * k + 1], s[2 * k], s[2 * k + 1]);
                f.stroke(0xFFB8B0A0, 11, ce, true);
                f.stroke(0xFF3A8EC0, 7.5f, ce, true);
                f.stroke(0x50FFFFFF, 1.4f, ce, true);
                // The lift: a chain ramp up to the high trough.
                for (int k = m * 4 / 11; k < m * 5 / 11; k++) {
                    float du = tq(q, k + 1, 0) - tq(q, k - 1, 0), dv = tq(q, k + 1, 1) - tq(q, k - 1, 1), l = (float) Math.sqrt(du * du + dv * dv) + 0.001f;
                    f.line(0xFF4A4E56, 0.8f, tq(q, k, 0) + dv / l * 3.5f, tq(q, k, 1) - du / l * 3.5f, tq(q, k, 0) - dv / l * 3.5f, tq(q, k, 1) + du / l * 3.5f);
                }
                // Logs with riders.
                for (int j = 0; j < 6; j++) {
                    int k = (j * m / 6 + m / 9) % m;
                    float du = tq(q, k + 1, 0) - tq(q, k - 1, 0), dv = tq(q, k + 1, 1) - tq(q, k - 1, 1), l = (float) Math.sqrt(du * du + dv * dv);
                    if (l < 0.01f) continue;
                    du /= l;
                    dv /= l;
                    float u = tq(q, k, 0), v = tq(q, k, 1), hl = 6, hw = 2.6f;
                    f.poly(0xFF6A4228, u - du * hl + dv * hw, v - dv * hl - du * hw, u + du * hl + dv * hw, v + dv * hl - du * hw,
                            u + du * hl - dv * hw, v + dv * hl + du * hw, u - du * hl - dv * hw, v - dv * hl + du * hw);
                    f.line(0xFF9A7048, 0.6f, u - du * hl, v - dv * hl, u + du * hl, v + dv * hl);
                    person(f, u + du * 2.2f, v + dv * 2.2f, CLOTHES[j % 8]);
                    person(f, u - du * 2.2f, v - dv * 2.2f, CLOTHES[(j + 3) % 8]);
                }
                queue(f, st[0], Math.max(st[1] - 22, 2), st[2], st[1] - 2, 5);
                f.sign(city, "LOG FLUME", pu, Math.min(S - 6, pvv + pr + 10), Math.min(10, S / 8), 0xFFF2E6C8);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                LandmarkArt12.gable(f, 0xFF7A5232, 0, 0, f.L, f.S);
                for (float u = 2; u < f.L; u += 2.5f) f.line(0x20000000, 0.4f, u, 0, u, f.S);
            }
        };
        d.ground = City.GRASS;
        d.minS = 13;
        d.minL = 16;
        d.base = City.KIOSK;
        d.inside = Variants.I_SHELVES;
        d.roofCol = 0xFF7A5232;
        out.add(d);
    }

    static void carousel(List<Landmarks.Def> out) {
        // A carousel under its striped canopy, horses round the edge, in the middle of a little garden.
        Landmarks.Def d = new Landmarks.Def("Carousel", T_PARK) {
            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, cu = L / 2, cv = S / 2, R = Math.min(S * 0.3f, 80);
                plot(f, r, cu, cv, R);
                platform(f, cu, cv, R + 2, 0xFFB89060);
                int n = Math.max(12, (int) (R * 0.5f));
                for (int k = 0; k < n; k++) {
                    double a = k * Math.PI * 2 / n;
                    float u = cu + (float) Math.cos(a) * (R - 2), v = cv + (float) Math.sin(a) * (R - 2);
                    f.circle(k % 2 == 0 ? 0xFFF2EEE6 : 0xFF8A5A3A, u, v, 1.6f);
                    f.circle(0xFFE8C040, u, v, 0.5f);
                }
                int g = 16;
                float cr = R - 5;
                f.circle(0x40000000, cu + 4, cv + 5, cr);
                for (int k = 0; k < g; k++) {
                    double a0 = k * Math.PI * 2 / g, a1 = (k + 1) * Math.PI * 2 / g, am = (a0 + a1) / 2;
                    float sh = 1f + 0.18f * (float) Math.cos(am + 3 * Math.PI / 4);
                    f.poly(Landmarks.shade(k % 2 == 0 ? 0xFFC8343C : 0xFFF2E6D0, sh), cu, cv, cu + (float) Math.cos(a0) * cr, cv + (float) Math.sin(a0) * cr,
                            cu + (float) Math.cos(a1) * cr, cv + (float) Math.sin(a1) * cr);
                }
                for (int k = 0; k < g; k++) {
                    double a = k * Math.PI * 2 / g;
                    f.circle(0xFFE8C040, cu + (float) Math.cos(a) * cr, cv + (float) Math.sin(a) * cr, 1.2f);
                }
                f.ring(0xFFD8A830, 0.8f, cu, cv, cr * 0.55f);
                f.circle(0xFFD8A830, cu, cv, cr * 0.16f);
                f.circle(0xFFF4E0A0, cu - 0.6f, cv - 0.6f, cr * 0.08f);
                crowd(f, r, 0, 0, L, S, 20);
                f.sign(city, "CAROUSEL", cu, Math.min(S - 6, cv + R + 14), Math.min(9, S / 9), 0xFFF2E6C8);
            }
        };
        d.ground = City.PLAZA;
        d.minS = 8;
        d.minL = 8;
        out.add(d);
    }

    static void dropTower(List<Landmarks.Def> out) {
        // A drop tower: a tall lattice mast with a ring of seats round its foot, its shadow far across the garden; a
        // fenced landing zone and the queue.
        Landmarks.Def d = new Landmarks.Def("Drop Tower", T_PARK) {
            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, cu = L * 0.42f, cv = S * 0.42f, R = Math.min(S * 0.2f, 52);
                plot(f, r, cu, cv, R + 4);
                // The long shadow of the mast, and of the gondola ring at the top.
                float len = Math.min(R * 3, Math.min(L - cu, S - cv) - 6);
                f.rline(0x34000000, 6, cu, cv, cu + len, cv + len);
                f.ring(0x26000000, 3, cu + len, cv + len, R * 0.45f);
                // The landing zone, fenced.
                f.circle(0xFF8A9098, cu, cv, R + 4);
                f.circle(0xFF9AA0A8, cu, cv, R + 2.5f);
                for (int k = 0; k < 32; k++) {
                    double a = k * Math.PI / 16;
                    f.circle(0xFF4A4E56, cu + (float) Math.cos(a) * (R + 4), cv + (float) Math.sin(a) * (R + 4), 0.7f);
                }
                f.ring(0xFF5A5E66, 0.8f, cu, cv, R + 4);
                float sr = R * 0.5f;
                f.ring(0xFF3A6AB0, 5, cu, cv, sr);
                int n = Math.max(10, (int) (sr * 0.8f));
                for (int k = 0; k < n; k++) {
                    double a = k * Math.PI * 2 / n;
                    float u = cu + (float) Math.cos(a) * sr, v = cv + (float) Math.sin(a) * sr;
                    f.circle(0xFFE8B030, u, v, 2f);
                    person(f, u + (float) Math.cos(a) * 2.4f, v + (float) Math.sin(a) * 2.4f, CLOTHES[k % 8]);
                }
                // The mast from above: its top, lit, and the cap with a red light.
                float ms = Math.max(4, R * 0.18f);
                f.rect(0xFF5A6068, cu - ms, cv - ms, cu + ms, cv + ms);
                f.frame(0xFFC8CED4, 0.7f, cu - ms, cv - ms, cu + ms, cv + ms);
                f.line(0xFFC8CED4, 0.6f, cu - ms, cv - ms, cu + ms, cv + ms);
                f.line(0xFFC8CED4, 0.6f, cu + ms, cv - ms, cu - ms, cv + ms);
                f.circle(0xFFD84040, cu, cv, 1.8f);
                f.circle(0xFFF8A0A0, cu - 0.5f, cv - 0.5f, 0.7f);
                float qu = cu + R + 14;
                if (qu + 26 < L - 4) queue(f, qu, Math.max(4, cv - R), Math.min(L - 6, qu + 40), cv - 12, 13);
                crowd(f, r, 0, 0, L, S, 16);
                f.sign(city, "DROP TOWER", cu, Math.min(S - 6, cv + R + 14), Math.min(10, S / 8), 0xFFF2E6C8);
            }
        };
        d.ground = City.PLAZA;
        d.minS = 8;
        d.minL = 10;
        out.add(d);
    }

    static void teacups(List<Landmarks.Def> out) {
        // The teacups: a big turntable carrying three smaller ones, each with its spinning cups in pastel colours.
        Landmarks.Def d = new Landmarks.Def("Teacups", T_PARK) {
            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, cu = L / 2, cv = S / 2, R = Math.min(S * 0.32f, 84);
                plot(f, r, cu, cv, R);
                f.circle(0x30000000, cu + 3, cv + 3, R + 2);
                f.circle(0xFFE8A8BC, cu, cv, R + 2);
                f.circle(0xFFF6E6EC, cu, cv, R);
                for (int k = 0; k < 24; k++) {
                    double a = k * Math.PI / 12;
                    f.line(0x18000000, 0.5f, cu, cv, cu + (float) Math.cos(a) * R, cv + (float) Math.sin(a) * R);
                }
                int[] cup = {0xFFE07090, 0xFF70B8E0, 0xFFF0C850, 0xFF90D090, 0xFFB890E0, 0xFFF09060};
                for (int j = 0; j < 3; j++) {
                    double a = j * Math.PI * 2 / 3 + 0.4;
                    float su = cu + (float) Math.cos(a) * R * 0.5f, sv = cv + (float) Math.sin(a) * R * 0.5f, sr = R * 0.43f;
                    f.circle(0xFFDCA8BA, su, sv, sr);
                    f.ring(0xFFFFFFFF, 0.7f, su, sv, sr - 1);
                    for (int k = 0; k < 4; k++) {
                        double b = k * Math.PI / 2 + j;
                        float u = su + (float) Math.cos(b) * sr * 0.55f, v = sv + (float) Math.sin(b) * sr * 0.55f, cr = Math.max(2.6f, sr * 0.3f);
                        int col = cup[(j * 4 + k) % cup.length];
                        f.circle(0x40000000, u + 1.2f, v + 1.5f, cr);
                        f.circle(City.darken(col, 0.8f), u, v, cr);
                        f.circle(0xFFF8F4F0, u, v, cr * 0.74f);
                        f.circle(col, u, v, cr * 0.56f);
                        f.circle(0xFFF8F4F0, u, v, cr * 0.2f);
                        f.rect(City.darken(col, 0.8f), u + cr * 0.9f, v - cr * 0.25f, u + cr * 1.3f, v + cr * 0.25f);
                        person(f, u - cr * 0.35f, v, CLOTHES[(j + k) % 8]);
                        person(f, u + cr * 0.3f, v + cr * 0.2f, CLOTHES[(j + k + 3) % 8]);
                    }
                }
                f.circle(0xFFE07090, cu, cv, R * 0.1f);
                f.circle(0xFFF8E080, cu, cv, R * 0.05f);
                crowd(f, r, 0, 0, L, S, 18);
                f.sign(city, "TEACUPS", cu, Math.min(S - 6, cv + R + 14), Math.min(9, S / 9), 0xFFF2E6C8);
            }
        };
        d.ground = City.PLAZA;
        d.minS = 8;
        d.minL = 8;
        out.add(d);
    }

    static void swingRide(List<Landmarks.Def> out) {
        // Chair swings flying out from a tall central tower under its painted crown, riders in every chair.
        Landmarks.Def d = new Landmarks.Def("Swing Ride", T_PARK) {
            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, cu = L / 2, cv = S / 2, R = Math.min(S * 0.33f, 88);
                plot(f, r, cu, cv, R + 2);
                f.circle(0xFFB8C8A0, cu, cv, R + 3);
                f.ring(0xFF7A8A60, 1f, cu, cv, R + 3);
                f.circle(0x22000000, cu + R * 0.4f, cv + R * 0.45f, R * 0.85f);
                int n = Math.max(14, (int) (R * 0.45f));
                for (int k = 0; k < n; k++) {
                    double a = k * Math.PI * 2 / n + 0.2;
                    float u = cu + (float) Math.cos(a) * R * 0.92f, v = cv + (float) Math.sin(a) * R * 0.92f;
                    f.line(0xFF9AA0A8, 0.5f, cu + (float) Math.cos(a - 0.15) * R * 0.48f, cv + (float) Math.sin(a - 0.15) * R * 0.48f, u, v);
                    f.circle(0x30000000, u + 2, v + 2.5f, 2f);
                    f.circle(0xFF3A6AB0, u, v, 2f);
                    person(f, u, v, CLOTHES[k % 8]);
                }
                float cr = R * 0.48f;
                f.circle(0x40000000, cu + 5, cv + 6, cr);
                int g = 12;
                for (int k = 0; k < g; k++) {
                    double a0 = k * Math.PI * 2 / g, a1 = (k + 1) * Math.PI * 2 / g, am = (a0 + a1) / 2;
                    float sh = 1f + 0.2f * (float) Math.cos(am + 3 * Math.PI / 4);
                    f.poly(Landmarks.shade(k % 2 == 0 ? 0xFF2E8A7A : 0xFFE8C040, sh), cu, cv, cu + (float) Math.cos(a0) * cr, cv + (float) Math.sin(a0) * cr,
                            cu + (float) Math.cos(a1) * cr, cv + (float) Math.sin(a1) * cr);
                }
                for (int k = 0; k < g; k++) {
                    double a = k * Math.PI * 2 / g;
                    f.circle(0xFFF8F0D0, cu + (float) Math.cos(a) * cr, cv + (float) Math.sin(a) * cr, 1.1f);
                }
                f.circle(0xFFC8343C, cu, cv, cr * 0.18f);
                crowd(f, r, 0, 0, L, S, 16);
                f.sign(city, "SWING RIDE", cu, Math.min(S - 6, cv + R + 14), Math.min(9, S / 9), 0xFFF2E6C8);
            }
        };
        d.ground = City.PLAZA;
        d.minS = 8;
        d.minL = 8;
        out.add(d);
    }

    static void foodCourt(List<Landmarks.Def> out) {
        // A food hall with a glazed roof, its terrace of tables under parasols, food trucks along the side.
        Landmarks.Def d = new Landmarks.Def("Food Court", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, 2, 2, Math.max(5, Math.min(L - 8, L / 2)), Math.max(4, Math.min(S - 6, S / 2)), 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                paving(f, 0xFFE2D6BE, 0, 0, L, S, 8);
                float[] h = bx(f, pl(this, x0, y0, x1, y1)[0]);
                int[] pc = {0xFFD84040, 0xFF3A8AD0, 0xFF40A860, 0xFFE8B030};
                int k = 0;
                for (float u = 16; u < L - 10; u += 40)
                    for (float v = 16; v < S - 10; v += 36, k++) {
                        if (u > h[0] - 12 && u < h[2] + 12 && v > h[1] - 12 && v < h[3] + 12) continue;
                        f.circle(0x30000000, u + 1, v + 1, 3.6f);
                        f.circle(0xFFF2F2EE, u, v, 3.6f);
                        for (int s = 0; s < 4; s++) {
                            double a = s * Math.PI / 2;
                            f.circle(0xFF8A7A6A, u + (float) Math.cos(a) * 5.4f, v + (float) Math.sin(a) * 5.4f, 1.3f);
                        }
                        parasol(f, u, v, 7.5f, pc[k % 4], 0xFFF2EADA);
                    }
                // Food trucks along the far end, and planters.
                for (float v = 6; v + 26 < S - 4; v += 34) {
                    float u = L - 18;
                    if (u < h[2] + 8) break;
                    int col = pc[(int) (v / 34) % 4];
                    f.roundRect(0x38000000, u + 1.5f, v + 1.5f, u + 13.5f, v + 27.5f, 2);
                    f.roundRect(col, u, v, u + 12, v + 26, 2);
                    f.rect(0xFFF2F2EE, u + 2, v + 2, u + 10, v + 18);
                    f.rect(0xFF2A3038, u + 2, v + 21, u + 10, v + 24);
                    f.rect(City.darken(col, 0.75f), u - 3, v + 4, u, v + 16);
                }
                for (float u = 8; u < L - 8; u += 30) f.bush(u, S - 5, 3);
                crowd(f, r, 0, 0, L, S, 40);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFFE8E0D0, 0, 0, L, S);
                f.rect(0xFF8AB8C8, 2.5f, 2.5f, L - 2.5f, S - 2.5f);
                for (float u = 2.5f + 8; u < L - 2.5f; u += 8) f.line(0xFFE8E0D0, 0.8f, u, 2.5f, u, S - 2.5f);
                f.rect(0xFFE8E0D0, 2.5f, S / 2 - 1, L - 2.5f, S / 2 + 1);
                f.rect(0x30FFFFFF, 2.5f, 2.5f, L - 2.5f, S * 0.3f);
                f.sign(city, "FOOD COURT", L / 2, S / 2, Math.min(10, S / 3), 0xFFF2E6C8);
            }
        };
        d.ground = City.PLAZA;
        d.minS = 9;
        d.minL = 10;
        d.base = City.SHOP;
        d.inside = Variants.I_RESTAURANT;
        d.roofCol = 0xFF8AB8C8;
        out.add(d);
    }

    static void pirateShip(List<Landmarks.Def> out) {
        // The swinging pirate ship: a galleon hung from its A-frames over a sunken pit, riders along both sides, a black
        // flag on the mast, in a garden.
        Landmarks.Def d = new Landmarks.Def("Pirate Ship", T_PARK) {
            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, cu = L / 2, cv = S / 2, R = Math.min(Math.min(S * 0.36f, L * 0.3f), 92);
                plot(f, r, cu, cv, R + 2);
                float hl = R * 0.95f, hw = R * 0.27f;
                f.roundRect(0xFF6A6458, cu - hl - 6, cv - hw - 6, cu + hl + 6, cv + hw + 6, 8);
                f.roundRect(0xFF4E483E, cu - hl - 3, cv - hw - 3, cu + hl + 3, cv + hw + 3, 6);
                f.line(0x30000000, 3, cu + 4, cv - hw - 12 + 5, cu + 4, cv + hw + 12 + 5);
                for (float s = -1; s <= 1; s += 2) {
                    f.line(0xFF8A9098, 2.6f, cu - 10 * s, cv - hw - 12, cu, cv);
                    f.line(0xFF8A9098, 2.6f, cu - 10 * s, cv + hw + 12, cu, cv);
                    f.rect(0xFF5A6068, cu - 10 * s - 1.5f, cv - hw - 13.5f, cu - 10 * s + 1.5f, cv - hw - 10.5f);
                    f.rect(0xFF5A6068, cu - 10 * s - 1.5f, cv + hw + 10.5f, cu - 10 * s + 1.5f, cv + hw + 13.5f);
                }
                float o = hl * 0.14f;
                f.poly(0x38000000, cu - hl + o + 5, cv + 5, cu - hl * 0.6f + o + 5, cv - hw + 5, cu + hl * 0.6f + o + 5, cv - hw + 5,
                        cu + hl + o + 5, cv + 5, cu + hl * 0.6f + o + 5, cv + hw + 5, cu - hl * 0.6f + o + 5, cv + hw + 5);
                f.poly(0xFF5A3420, cu - hl + o, cv, cu - hl * 0.6f + o, cv - hw, cu + hl * 0.6f + o, cv - hw, cu + hl + o, cv,
                        cu + hl * 0.6f + o, cv + hw, cu - hl * 0.6f + o, cv + hw);
                f.poly(0xFFA87848, cu - hl * 0.86f + o, cv, cu - hl * 0.56f + o, cv - hw * 0.72f, cu + hl * 0.56f + o, cv - hw * 0.72f,
                        cu + hl * 0.86f + o, cv, cu + hl * 0.56f + o, cv + hw * 0.72f, cu - hl * 0.56f + o, cv + hw * 0.72f);
                f.line(0xFFD8B040, 0.8f, cu - hl + o, cv, cu - hl * 0.6f + o, cv - hw);
                f.line(0xFFD8B040, 0.8f, cu + hl + o, cv, cu + hl * 0.6f + o, cv - hw);
                for (float u = cu - hl * 0.5f + o; u < cu + hl * 0.5f + o; u += 6) {
                    f.line(0xFF7A5232, 0.7f, u, cv - hw * 0.72f, u, cv + hw * 0.72f);
                    person(f, u + 3, cv - hw * 0.42f, CLOTHES[(int) u % 8]);
                    person(f, u + 3, cv + hw * 0.42f, CLOTHES[((int) u + 3) % 8]);
                }
                f.circle(0xFF4A3A2A, cu + o, cv, 2.2f);
                f.rect(0xFFF2EAD8, cu + o - 1.6f, cv - hw * 0.95f, cu + o + 1.6f, cv + hw * 0.95f);
                f.rect(0xFF1E1E22, cu + o + 1.6f, cv - hw * 0.95f, cu + o + 8, cv - hw * 0.95f + 4.5f);
                f.circle(0xFFF2F2EE, cu + o + 4.6f, cv - hw * 0.95f + 2.2f, 1f);
                f.circle(0xFF5A6068, cu, cv, 1.8f);
                crowd(f, r, 0, 0, L, S, 14);
                f.sign(city, "PIRATE SHIP", cu, Math.min(S - 6, cv + hw + 22), Math.min(10, S / 8), 0xFFF2E6C8);
            }
        };
        d.ground = City.PLAZA;
        d.minS = 9;
        d.minL = 12;
        out.add(d);
    }

    static void bumperCars(List<Landmarks.Def> out) {
        // The dodgems: a big open-sided pavilion with a striped canopy over the floor (the building, so people can
        // shelter in it), the queue and a candy-floss stall in front, a garden round it.
        Landmarks.Def d = new Landmarks.Def("Bumper Cars", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int lu = Math.max(6, Math.min(L - 6, Math.round(L * 0.55f))), lv = Math.max(5, Math.min(S - 6, Math.round(S * 0.5f)));
                return new int[][]{at(al, (L - lu) / 2, 2, lu, lv, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] h = bx(f, pl(this, x0, y0, x1, y1)[0]);
                plot(f, r, L / 2, (h[1] + h[3]) / 2, 1);
                paving(f, 0xFFE0D4BA, h[0] - 10, h[1] - 10, h[2] + 10, h[3] + 10, 8);
                float qv = h[3] + 12;
                if (qv + 14 < S - 4) queue(f, h[0], qv, Math.min(h[2] - 20, h[0] + 60), qv + 14, 3);
                if (qv + 12 < S - 4) stripes(f, h[2] - 16, qv, h[2] - 4, qv + 10, 0xFFF090C0, 0xFFF8F4F0);
                crowd(f, r, 0, h[3], L, S, 14);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, m = S / 2;
                int k = 0;
                for (float u = 0; u < L - 0.01f; u += 10, k++) {
                    int col = k % 2 == 0 ? 0xFFD8343C : 0xFFF2C840;
                    f.rect(City.lighten(col, 0.06f), u, 0, Math.min(L, u + 10), m);
                    f.rect(City.darken(col, 0.8f), u, m, Math.min(L, u + 10), S);
                }
                f.line(0xFFF8F4E8, 1.4f, 0, m, L, m);
                for (float u = 2.5f; u < L; u += 5) {
                    f.circle(0xFFF8F4E8, u, 0.8f, 1.6f);
                    f.circle(0xFFE8E0D0, u, S - 0.8f, 1.6f);
                }
                f.sign(city, "BUMPER CARS", L / 2, m, Math.min(10, S / 3), 0xFFFFFFFF);
            }
        };
        d.ground = City.PLAZA;
        d.minS = 9;
        d.minL = 10;
        d.base = City.SHOP;
        d.inside = Variants.I_ARCADE;
        d.roofCol = 0xFFD8343C;
        out.add(d);
    }

    static void mouseCoaster(List<Landmarks.Def> out) {
        // A wild-mouse coaster: tight hairpins stacked back and forth over a compact plot, little single cars, a lift
        // up one side and its station.
        Landmarks.Def d = new Landmarks.Def("Spinning Coaster", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, L - 6, S - 4, 4, 3, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                paving(f, 0xFFD4CAB2, 0, 0, L, S, 8);
                f.roundRect(0xFF8A9A6A, 4, 4, L - 4, S - 20, 8);
                f.roundRect(0xFF6EA052, 5, 5, L - 5, S - 21, 7);
                float[] st = bx(f, pl(this, x0, y0, x1, y1)[0]);
                int rows = Math.max(3, Math.min(6, (int) ((S - 40) / 36)));
                float u0 = 16, u1 = L - 16, top = 16, gap = (S - 44) / (float) rows;
                float[] cp = new float[(rows * 2 + 6) * 3];
                int n = 0;
                float hgt = 22;
                cp[n++] = st[0] - 2; cp[n++] = S - 12; cp[n++] = 2;
                cp[n++] = u1 + 6; cp[n++] = S - 12; cp[n++] = 4;
                for (int k = 0; k <= rows; k++) {
                    float v = top + (rows - k) * gap;
                    boolean right = k % 2 == 0;
                    cp[n++] = right ? u1 : u0; cp[n++] = v; cp[n++] = hgt - k * 2;
                    cp[n++] = right ? u0 : u1; cp[n++] = v; cp[n++] = hgt - k * 2 - 1;
                }
                cp[n++] = 8; cp[n++] = S - 12; cp[n++] = 2;
                float[] q = LandmarkArt12.chaikin(java.util.Arrays.copyOf(cp, n), 3);
                int m = q.length / 3;
                for (float u = 26; u < L - 20; u += 34)
                    for (int k = 0; k < rows; k++) f.tree(u + (k % 2) * 14, top + (k + 0.5f) * gap, 4.5f, 0xFF4A7E3C);
                track(f, q, 0xFF40A0C8, 0xFF5A5E66, 2.2f, 0, m / 10);
                for (int k = 0; k < 4; k++) train(f, q, (k * m / 4 + m / 7) % m, 1, k % 2 == 0 ? 0xFFE8B030 : 0xFFD84060, 3.4f);
                crowd(f, r, 0, S - 18, st[0], S, 10);
                f.sign(city, "WILD MOUSE", L / 2, S - 26, Math.min(9, S / 8), 0xFFF2E6C8);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                hip(f, 0xFF3A8AB0, 0, 0, f.L, f.S, false);
            }
        };
        d.ground = City.PLAZA;
        d.minS = 12;
        d.minL = 14;
        d.base = City.KIOSK;
        d.inside = Variants.I_SHELVES;
        d.roofCol = 0xFF3A8AB0;
        out.add(d);
    }

    static void splashPad(List<Landmarks.Def> out) {
        // A water playground: a round blue splash floor with rings of fountain jets, a big tipping bucket on its tower in
        // the middle, loungers and parasols for the parents round it.
        Landmarks.Def d = new Landmarks.Def("Splash Pad", T_PARK) {
            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, cu = L / 2, cv = S / 2, R = Math.min(S * 0.34f, 90);
                plot(f, r, cu, cv, R + 4);
                f.circle(0xFF5AA8CC, cu, cv, R + 1.5f);
                f.circle(0xFF8ED0E8, cu, cv, R);
                for (float rr = 14; rr < R - 4; rr += 13) {
                    int n = (int) (rr * 0.45f);
                    for (int k = 0; k < n; k++) {
                        double a = k * Math.PI * 2 / n + rr;
                        float u = cu + (float) Math.cos(a) * rr, v = cv + (float) Math.sin(a) * rr;
                        f.circle(0xB0FFFFFF, u, v, 2.6f);
                        f.circle(0xFF3A8EC0, u, v, 0.8f);
                    }
                }
                f.rect(0x38000000, cu - 6, cv - 6, cu + 10, cv + 10);
                f.rect(0xFFE8B030, cu - 8, cv - 8, cu + 8, cv + 8);
                f.rect(0xFFD84040, cu - 5.5f, cv - 5.5f, cu + 5.5f, cv + 5.5f);
                ball(f, cu, cv, 4, 0xFF3A8AD0);
                for (int k = 0; k < 6; k++) {
                    double a = k * Math.PI / 3 + 0.5;
                    lounger(f, cu + (float) Math.cos(a) * (R + 6), cv + (float) Math.sin(a) * (R + 6), false, 0xFFF2F2EE);
                }
                crowd(f, r, cu - R * 0.7f, cv - R * 0.7f, cu + R * 0.7f, cv + R * 0.7f, 20);
            }
        };
        d.ground = City.PLAZA;
        d.minS = 8;
        d.minL = 8;
        out.add(d);
    }

    static void midway(List<Landmarks.Def> out) {
        // Midway games: two rows of striped booths facing a lane, prizes hanging, people playing, trees at the ends.
        Landmarks.Def d = new Landmarks.Def("Midway Games", T_PARK) {
            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                paving(f, 0xFFDCD0B6, 0, 0, L, S, 8);
                int[][] cols = {{0xFFD84040, 0xFFF2EADA}, {0xFF3A8AD0, 0xFFF2EADA}, {0xFFE8B030, 0xFFD84040}, {0xFF40A860, 0xFFF2EADA}, {0xFFC050B0, 0xFFF2EADA}};
                float bd = Math.min(22, S * 0.22f), bwid = 34;
                int k = 0;
                for (float u = 18; u + bwid < L - 16; u += bwid + 4, k++) {
                    int[] cc = cols[k % cols.length];
                    stripes(f, u, 6, u + bwid, 6 + bd, cc[0], cc[1]);
                    stripes(f, u, S - 6 - bd, u + bwid, S - 6, cols[(k + 2) % cols.length][0], cc[1]);
                    f.rect(0xFF8A6A48, u, 6 + bd, u + bwid, 8 + bd);
                    f.rect(0xFF8A6A48, u, S - 8 - bd, u + bwid, S - 6 - bd);
                    for (int j = 0; j < 5; j++) {
                        f.circle(CLOTHES[(k + j) % 8], u + 4 + j * 6.5f, 11 + bd, 1.8f);
                        if (j % 2 == 0) person(f, u + 5 + j * 6, S - 12 - bd, CLOTHES[(k + j + 1) % 8]);
                    }
                }
                f.tree(8, S / 2, 6, 0xFF4A7E3C);
                f.tree(L - 8, S / 2, 6, 0xFF4A7E3C);
                // A planted strip down the middle of the lane: trees, bushes and lamps, a high striker at one end.
                if (S > 90)
                    for (float u = 36; u < L - 30; u += 44) {
                        f.roundRect(0xFF6EA052, u - 12, S / 2 - 5, u + 12, S / 2 + 5, 4);
                        f.tree(u, S / 2, 6, 0xFF4A7E3C);
                        f.bush(u - 9, S / 2, 2.4f);
                        f.bush(u + 9, S / 2, 2.4f);
                        f.lamp(u + 22, S / 2);
                    }
                f.rect(0xFF5A5E66, 22, S / 2 - 12, 24, S / 2 + 4);
                f.rect(0xFFD84040, 21, S / 2 - 14, 25, S / 2 - 11);
                crowd(f, r, 16, S * 0.35f, L - 16, S * 0.65f, 18);
            }
        };
        d.ground = City.PLAZA;
        d.minS = 6;
        d.minL = 6;
        out.add(d);
    }

    static void arcade(List<Landmarks.Def> out) {
        // The arcade hall: a big shed with a neon-trimmed roof and skylights, planters and benches on its forecourt.
        Landmarks.Def d = new Landmarks.Def("Arcade Hall", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, 2, 2, Math.max(4, L - 4), Math.max(3, Math.min(S - 6, S * 2 / 3)), 2)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                paving(f, 0xFFD8CCB4, 0, 0, L, S, 8);
                float[] h = bx(f, pl(this, x0, y0, x1, y1)[0]);
                for (float u = 14; u < L - 10; u += 30) {
                    f.bush(u, (h[3] + S) / 2, 3.5f);
                    f.bench(u + 12, (h[3] + S) / 2, true);
                }
                crowd(f, r, 0, h[3], L, S, 10);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF3A3A48, 0, 0, L, S);
                f.rect(0xFF4E4E5E, 2, 2, L - 2, S - 2);
                f.frame(0xFFF070B0, 1.2f, 1.2f, 1.2f, L - 1.2f, S - 1.2f);
                f.frame(0xFF40D0D0, 0.8f, 3.4f, 3.4f, L - 3.4f, S - 3.4f);
                for (float u = 14; u < L - 18; u += 36) {
                    f.rect(0xFF8AB8D0, u, S * 0.3f, u + 18, S * 0.7f);
                    f.rect(0x40FFFFFF, u, S * 0.3f, u + 18, S * 0.42f);
                    f.frame(0xFF2A2A34, 0.6f, u, S * 0.3f, u + 18, S * 0.7f);
                }
                f.sign(city, "ARCADE", L / 2, S / 2, Math.min(11, S / 3), 0xFFF8E070);
            }
        };
        d.ground = City.PLAZA;
        d.minS = 7;
        d.minL = 8;
        d.base = City.SHOP;
        d.inside = Variants.I_ARCADE;
        d.roofCol = 0xFF4E4E5E;
        out.add(d);
    }

    // ================================================================== the resort

    static void resortHotel(List<Landmarks.Def> out) {
        // A beach hotel: an L of white guest wings with balconies, the lobby in the corner, a lagoon pool with an island
        // of palms in the garden, loungers and parasols round it.
        Landmarks.Def d = new Landmarks.Def("Resort Hotel", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int t = Math.max(3, Math.min(5, S / 3));
                return new int[][]{at(al, 1, 1, L - 2, t, 6), at(al, 1, t + 2, t, Math.max(2, S - t - 3), 5)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF74A85A, 0, 0, L, S);
                int[][] pq = pl(this, x0, y0, x1, y1);
                float[] a = bx(f, pq[0]), b = bx(f, pq[1]);
                float gu0 = b[2] + 4, gv0 = a[3] + 4, gu1 = L - 4, gv1 = S - 4;
                paving(f, 0xFFEDE4D2, gu0 - 2, gv0 - 2, gu1 + 2, gv1 + 2, 6);
                // A lagoon pool, its edge curving.
                float pu = (gu0 + gu1) / 2, pv = (gv0 + gv1) / 2, ru = (gu1 - gu0) * 0.38f, rv = (gv1 - gv0) * 0.34f;
                if (ru > 5 && rv > 4) {
                    f.oval(0xFFE8E2D4, pu - ru - 2, pv - rv - 2, pu + ru + 2, pv + rv + 2);
                    f.oval(0xFF5ABED8, pu - ru, pv - rv, pu + ru, pv + rv);
                    f.oval(0xFF3A9EC8, pu - ru * 0.8f, pv - rv * 0.7f, pu + ru * 0.75f, pv + rv * 0.75f);
                    f.oval(0xFF5ABED8, pu + ru * 0.35f, pv - rv * 0.9f, pu + ru * 1.2f, pv + rv * 0.1f);
                    f.circle(0xFFE6D6A8, pu - ru * 0.2f, pv, Math.min(ru, rv) * 0.28f);
                    palm(f, pu - ru * 0.2f, pv, Math.min(6, Math.min(ru, rv) * 0.35f));
                    for (float u = pu - ru + 3; u < pu + ru - 2; u += 6) lounger(f, u, pv + rv + 5, false, 0xFFF2F2EE);
                    parasol(f, pu - ru - 3, pv - rv, 4, 0xFF2E8A9A, 0xFFF2EADA);
                    parasol(f, pu + ru + 2, pv + rv, 4, 0xFF2E8A9A, 0xFFF2EADA);
                }
                for (float u = 8; u < L - 4; u += 22) palm(f, u, S - 6, 7.5f);
                for (float v = gv0 + 6; v < S - 12; v += 22) palm(f, L - 6, v, 7.5f);
                crowd(f, r, gu0, gv0, gu1, gv1, (int) (L * S / 1500) + 2);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFFB8B4AC, 0, 0, L, S);
                f.rect(0xFFF2EEE6, 1.4f, 1.4f, L - 1.4f, S - 1.4f);
                // Balcony rails along the edges, a terracotta band, the plant.
                for (float u = 3; u < L - 2; u += 3) {
                    f.line(0xFFC8C4BC, 0.5f, u, 0.4f, u, 1.4f);
                    f.line(0xFFC8C4BC, 0.5f, u, S - 1.4f, u, S - 0.4f);
                }
                f.rect(0xFFC87A5A, 1.4f, S / 2 - 1, L - 1.4f, S / 2 + 1);
                for (float u = 6; u < L - 8; u += 22) plant(f, u, S * 0.18f, 4.5f);
                if (part == 0 && L > 40) {
                    f.roundRect(0xFF5ABED8, L * 0.7f, S * 0.58f, L * 0.7f + 14, S - 3, 2);
                    f.sign(city, "RESORT HOTEL", L * 0.35f, S / 2, Math.min(9, S / 2.4f), 0xFF2E6A7A);
                }
            }
        };
        d.ground = City.GRASS;
        d.minS = 10;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOTEL;
        d.roofCol = 0xFFF2EEE6;
        out.add(d);
    }

    static void towerHotel(List<Landmarks.Def> out) {
        // A tall hotel tower on a pool deck: the slab of the tower with its rooftop helipad and plant, a long pool with
        // loungers and cabanas along it, palms round the deck.
        Landmarks.Def d = new Landmarks.Def("Tower Hotel", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int lu = Math.max(5, Math.min(L - 4, L * 3 / 5)), lv = Math.max(4, Math.min(S - 6, S / 2));
                return new int[][]{at(al, 2, 1, lu, lv, 14)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] t = bx(f, pl(this, x0, y0, x1, y1)[0]);
                paving(f, 0xFFE8DCC4, 0, 0, L, S, 8);
                f.rect(0xFF74A85A, 0, S - 10, L, S);
                float pv0 = t[3] + 26, pv1 = S - 22, pu0 = 24, pu1 = Math.min(L - 24, pu0 + Math.max(60, L * 0.6f));
                if (pv1 - pv0 > 14 && pu1 - pu0 > 30) {
                    pool(f, pu0, pv0, pu1, pv1, 3);
                    for (float u = pu0 + 4; u < pu1 - 4; u += 10) {
                        lounger(f, u, pv0 - 9, false, 0xFFF2F2EE);
                        lounger(f, u + 3, pv1 + 8, false, 0xFFF2F2EE);
                    }
                    for (float u = pu1 + 14; u < L - 12; u += 20) parasol(f, u, (pv0 + pv1) / 2, 6, 0xFF2E8A9A, 0xFFF2EADA);
                }
                for (float u = 8; u < L - 4; u += 22) palm(f, u, S - 5, 7);
                for (float v = t[1] + 6; v < t[3]; v += 22) palm(f, L - 8, v, 7);
                crowd(f, r, 0, t[3], L, S, 16);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF5A8A9A, 0, 0, L, S);
                f.rect(0xFFE4EEF0, 2, 2, L - 2, S - 2);
                f.rect(0xFFCCDCE0, 2, S * 0.55f, L - 2, S - 2);
                f.rect(0xFF8A9096, L * 0.42f, S * 0.25f, L * 0.78f, S * 0.75f);
                f.rect(0xFFB0B6BC, L * 0.42f + 1.5f, S * 0.25f + 1.5f, L * 0.78f - 1.5f, S * 0.75f - 1.5f);
                for (float u = L * 0.45f; u < L * 0.75f; u += 10) plant(f, u, S * 0.35f, 6);
                float hr = Math.min(S * 0.32f, L * 0.16f);
                f.circle(0xFF5A6068, L * 0.2f, S / 2, hr);
                f.circle(0xFF7A8088, L * 0.2f, S / 2, hr - 1.5f);
                f.ring(0xFFF2F2EE, 1f, L * 0.2f, S / 2, hr - 3);
                f.rect(0xFFF2F2EE, L * 0.2f - hr * 0.35f, S / 2 - hr * 0.45f, L * 0.2f - hr * 0.2f, S / 2 + hr * 0.45f);
                f.rect(0xFFF2F2EE, L * 0.2f + hr * 0.2f, S / 2 - hr * 0.45f, L * 0.2f + hr * 0.35f, S / 2 + hr * 0.45f);
                f.rect(0xFFF2F2EE, L * 0.2f - hr * 0.2f, S / 2 - hr * 0.07f, L * 0.2f + hr * 0.2f, S / 2 + hr * 0.07f);
                f.sign(city, "TOWER HOTEL", L * 0.6f, S - 6, Math.min(9, S / 5), 0xFF2E6A7A);
            }
        };
        d.ground = City.PLAZA;
        d.minS = 9;
        d.minL = 10;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOTEL;
        d.roofCol = 0xFFE4EEF0;
        out.add(d);
    }

    static void beachVillas(List<Landmarks.Def> out) {
        // Beach villas: rows of thatched bungalows, each with its timber deck and plunge pool, either side of a sandy
        // path through the palms.
        Landmarks.Def d = new Landmarks.Def("Beach Villas", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int n = Math.max(1, (L - 1) / 6);
                boolean two = S >= 15;
                int[][] o = new int[n * (two ? 2 : 1)][];
                int k = 0;
                for (int j = 0; j < n; j++) {
                    o[k++] = at(al, 1 + j * 6 + (L - 1 - n * 6) / 2 + 1, 1, 3, 3, 1);
                    if (two) o[k++] = at(al, 1 + j * 6 + (L - 1 - n * 6) / 2 + 1, S - 4, 3, 3, 1);
                }
                return o;
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                f.rect(0xFF7AAE5E, 0, 0, L, S);
                int[][] pq = pl(this, x0, y0, x1, y1);
                boolean two = pq.length > 1 && bx(f, pq[1])[1] > S / 2;
                float pv0 = two ? S / 2 - 14 : S - 30, pv1 = two ? S / 2 + 14 : S - 6;
                f.rect(0xFFE6D6A8, 0, pv0, L, pv1);
                for (int k = 0; k < (int) (L / 2); k++) f.circle(0x14000000, r.nextFloat() * L, pv0 + r.nextFloat() * (pv1 - pv0), 0.8f);
                for (int[] q : pq) {
                    float[] b = bx(f, q);
                    boolean top = b[1] < S / 2;
                    float dv0 = top ? b[3] : b[1] - 30, dv1 = top ? b[3] + 30 : b[1];
                    f.rect(0x30000000, b[0] - 6 + 1.5f, dv0 + 1.5f, b[2] + 6 + 1.5f, dv1 + 1.5f);
                    f.rect(0xFFB08A60, b[0] - 6, dv0, b[2] + 6, dv1);
                    for (float u = b[0] - 6; u < b[2] + 6; u += 3) f.line(0x26000000, 0.5f, u, dv0, u, dv1);
                    pool(f, b[0] - 2, top ? dv0 + 8 : dv0 + 4, b[2] + 2, top ? dv1 - 4 : dv1 - 8, 2);
                    lounger(f, b[2] + 10, (dv0 + dv1) / 2, false, 0xFFF2F2EE);
                    palm(f, b[0] - 12, top ? b[1] + 10 : b[3] - 10, 8);
                }
                for (float u = 20; u < L - 10; u += 46) parasol(f, u, (pv0 + pv1) / 2, 7, 0xFF2E8A9A, 0xFFF2EADA);
                crowd(f, r, 0, pv0, L, pv1, 6);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                hip(f, 0xFFC8A060, 0, 0, f.L, f.S, true);
                for (int k = 0; k < 20; k++) {
                    float u = r.nextFloat() * f.L, v = r.nextFloat() * f.S;
                    f.line(0x30000000, 0.3f, u, v, u + 2, v + 0.6f);
                }
                f.circle(0xFF8A6A3A, f.L / 2, f.S / 2, 1.4f);
            }
        };
        d.ground = City.GRASS;
        d.minS = 10;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_HOTEL;
        d.roofCol = 0xFFC8A060;
        out.add(d);
    }

    static void poolClub(List<Landmarks.Def> out) {
        // A pool club: a big free-form pool and a lap pool with water slides coiling down into them, cabanas, rows of
        // loungers and the clubhouse with its bar.
        Landmarks.Def d = new Landmarks.Def("Pool Club", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, 1, 1, Math.max(4, L / 4), Math.max(3, S - 2), 2)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] h = bx(f, pl(this, x0, y0, x1, y1)[0]);
                paving(f, 0xFFEFE6D4, 0, 0, L, S, 8);
                float u0 = h[2] + 24, u1 = L - 18, v0 = 22, v1 = S - 34;
                if (u1 - u0 > 40 && v1 - v0 > 24) {
                    float mid = u0 + (u1 - u0) * 0.55f;
                    pool(f, u0, v0, mid, v1, 14);
                    pool(f, mid + 14, v0, u1, v0 + (v1 - v0) * 0.45f, 2);
                    for (float v = v0 + 6; v < v0 + (v1 - v0) * 0.45f - 2; v += 6) f.line(0x60FFFFFF, 0.5f, mid + 16, v, u1 - 2, v);
                    // Slides: a tower by the big pool, two tubes coiling down into it.
                    float tu = mid - 8, tv = v0 - 12;
                    f.rect(0x40000000, tu - 5, tv - 4, tu + 9, tv + 10);
                    f.rect(0xFF8A9098, tu - 7, tv - 6, tu + 7, tv + 8);
                    f.arc(0xFFE8B030, 4, tu - 14, tv + 16, 14, -0.6, 2.6);
                    f.arc(0xFFD84040, 4, tu - 4, tv + 24, 18, -1.4, 2.0);
                    for (float u = u0 + 4; u < u1 - 4; u += 10) lounger(f, u, v1 + 14, false, 0xFFF2F2EE);
                    for (float u = u0 + 8; u < u1 - 8; u += 34) parasol(f, u, S - 8, 6, 0xFF2E8A9A, 0xFFF2EADA);
                    crowd(f, r, u0, v0, u1, v1, 30);
                }
                for (float v = 12; v < S - 10; v += 26) {
                    f.rect(0x38000000, h[2] + 6, v + 1.5f, h[2] + 18, v + 13.5f);
                    stripes(f, h[2] + 5, v, h[2] + 17, v + 12, 0xFFF2EADA, 0xFF2E8A9A);
                }
                palm(f, L - 8, S - 8, 7);
                palm(f, L - 8, 8, 7);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                LandmarkArt12.gable(f, 0xFF2E8A9A, 0, 0, f.L, f.S);
                f.sign(city, "POOL CLUB", f.L / 2, f.S / 2, Math.min(9, f.S / 3), 0xFFF8F4EC);
            }
        };
        d.ground = City.PLAZA;
        d.minS = 10;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_BAR;
        d.roofCol = 0xFF2E8A9A;
        out.add(d);
    }

    static void surfLodge(List<Landmarks.Def> out) {
        // A surf lodge: a low timber lodge with a deck, racks of bright surfboards, a hammock between palms, a small pool.
        Landmarks.Def d = new Landmarks.Def("Surf Lodge", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, 1, 1, Math.max(4, L / 2), Math.max(3, S / 2 - 1), 2)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] h = bx(f, pl(this, x0, y0, x1, y1)[0]);
                f.rect(0xFFE6D6A8, 0, 0, L, S);
                for (int k = 0; k < (int) (L * S / 120); k++) f.circle(0x16000000, r.nextFloat() * L, r.nextFloat() * S, 0.7f);
                f.rect(0x30000000, h[0] + 1.5f, h[3] + 1.5f, h[2] + 1.5f, h[3] + 21.5f);
                f.rect(0xFFB08A60, h[0], h[3], h[2], h[3] + 20);
                for (float u = h[0]; u < h[2]; u += 3) f.line(0x28000000, 0.5f, u, h[3], u, h[3] + 20);
                float pu0 = h[2] + 20, pu1 = Math.min(L - 16, pu0 + 90);
                if (pu1 - pu0 > 30) pool(f, pu0, 16, pu1, Math.min(S - 30, h[3] - 4), 4);
                int[] board = {0xFFE8B030, 0xFFD84040, 0xFF3A8AD0, 0xFF40A860, 0xFFF2F2EE, 0xFFF08040};
                for (int k = 0; k < 10; k++) {
                    float u = h[0] + 8 + k * 7, v = h[3] + 28;
                    if (u > L - 8 || v + 20 > S) break;
                    f.oval(0x30000000, u - 1, v + 1.5f, u + 4.5f, v + 21.5f);
                    f.oval(board[k % board.length], u - 2.5f, v, u + 2.5f, v + 20);
                    f.line(0x50FFFFFF, 0.4f, u, v + 2, u, v + 18);
                }
                palm(f, L - 12, S - 12, 8);
                palm(f, L * 0.62f, S - 10, 7.5f);
                f.line(0xFFE8D8B0, 1.4f, L * 0.62f, S - 10, L - 12, S - 12);
                crowd(f, r, 0, h[3], L, S, 6);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                LandmarkArt12.gable(f, 0xFF8A6A48, 0, 0, f.L, f.S);
                for (float u = 3; u < f.L; u += 3) f.line(0x22000000, 0.5f, u, 0, u, f.S);
                f.sign(city, "SURF LODGE", f.L / 2, f.S / 2, Math.min(8, f.S / 3), 0xFFF8E070);
            }
        };
        d.ground = City.SAND;
        d.minS = 8;
        d.minL = 9;
        d.base = City.SHOP;
        d.inside = Variants.I_CAFE;
        d.roofCol = 0xFF8A6A48;
        out.add(d);
    }

    // ================================================================== the staff town

    static void staffClinic(List<Landmarks.Def> out) {
        // The staff clinic: a low white building with a red cross on the roof, an ambulance in its bay, a small garden.
        Landmarks.Def d = new Landmarks.Def("Staff Clinic", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                return new int[][]{at(al, 1, 1, Math.max(5, Math.min(L * 2 / 3, 12)), Math.max(4, Math.min(S - 5, 9)), 2)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] h = bx(f, pl(this, x0, y0, x1, y1)[0]);
                f.rect(0xFF74A05A, 0, 0, L, S);
                f.rect(0xFFB8B4AC, 0, h[3] + 4, Math.min(L, h[2] + 56), Math.min(S, h[3] + 38));
                // The ambulance bay, and the ambulance.
                float au = h[0] + 8, av = h[3] + 10;
                f.frame(0xFFE8D040, 1f, au - 4, av - 4, au + 40, av + 20);
                f.roundRect(0x38000000, au + 1.5f, av + 1.5f, au + 31.5f, av + 15.5f, 2);
                f.roundRect(0xFFF2F2EE, au, av, au + 30, av + 14, 2);
                f.rect(0xFFD83A3A, au, av + 6, au + 30, av + 8);
                f.rect(0xFF2A3038, au + 24, av + 1.5f, au + 27, av + 12.5f);
                f.rect(0xFF3A6AD8, au + 14, av + 5, au + 18, av + 9);
                for (float u = h[2] + 70; u < L - 10; u += 40)
                    for (float v = 14; v < S - 10; v += 40) f.tree(u + (v % 3) * 4, v, 6.5f);
                for (float v = h[3] + 52; v < S - 10; v += 40)
                    for (float u = 14; u < h[2] + 60; u += 40) f.tree(u, v + (u % 3) * 4, 6.5f);
                float gu = h[2] + 8;
                if (L - gu > 20) {
                    f.tree((gu + L) / 2, h[1] + 14, 7);
                    flowers(f, gu, h[3] - 20, Math.min(L - 6, gu + 24), h[3] - 8, 0xFFF2F2F2, 0xFFE05060);
                    f.bench(gu + 10, h[3] - 2, true);
                }
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, m = Math.min(L, S) * 0.22f;
                f.rect(0xFFB8BCC0, 0, 0, L, S);
                f.rect(0xFFF2F2EE, 2, 2, L - 2, S - 2);
                f.circle(0xFFF8F8F4, L / 2, S / 2, m * 1.5f);
                f.rect(0xFFD83A3A, L / 2 - m, S / 2 - m * 0.33f, L / 2 + m, S / 2 + m * 0.33f);
                f.rect(0xFFD83A3A, L / 2 - m * 0.33f, S / 2 - m, L / 2 + m * 0.33f, S / 2 + m);
                plant(f, 5, 5, 7);
                plant(f, L - 14, 5, 7);
                f.sign(city, "STAFF CLINIC", L / 2, S - 6, Math.min(8, S / 5), 0xFF8A2A2A);
            }
        };
        d.ground = City.GRASS;
        d.minS = 8;
        d.minL = 9;
        d.base = City.OFFICE;
        d.inside = Variants.I_WARD;
        d.roofCol = 0xFFF2F2EE;
        out.add(d);
    }

    static void staffHousing(List<Landmarks.Def> out) {
        // Staff housing: two plain dormitory blocks round a courtyard, a basketball court, washing lines, bikes.
        Landmarks.Def d = new Landmarks.Def("Staff Housing", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int t = Math.max(3, Math.min(4, (S - 3) / 3));
                return new int[][]{at(al, 1, 1, L - 2, t, 3), at(al, 1, S - 1 - t, L - 2, t, 3)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] pq = pl(this, x0, y0, x1, y1);
                float[] a = bx(f, pq[0]), b = bx(f, pq[1]);
                f.rect(0xFF7A9E5E, 0, 0, L, S);
                float v0 = a[3] + 6, v1 = b[1] - 6, cl = Math.min(L * 0.4f, 150), cw = Math.min(v1 - v0, 84);
                if (cw > 30) {
                    float cv = (v0 + v1) / 2;
                    f.rect(0xFFB06A4A, 12, cv - cw / 2, 12 + cl, cv + cw / 2);
                    f.frame(0xFFF2F2EE, 0.8f, 16, cv - cw / 2 + 4, 8 + cl, cv + cw / 2 - 4);
                    f.line(0xFFF2F2EE, 0.8f, 12 + cl / 2, cv - cw / 2 + 4, 12 + cl / 2, cv + cw / 2 - 4);
                    f.ring(0xFFF2F2EE, 0.8f, 12 + cl / 2, cv, cw * 0.15f);
                    f.circle(0xFFD87030, 20, cv, 1.6f);
                    f.circle(0xFFD87030, 4 + cl, cv, 1.6f);
                }
                float lu0 = cl + 30, lu1 = Math.min(L - 14, lu0 + 70);
                for (float v = v0 + 8, n = 0; v < v1 - 6 && n < 4; v += 12, n++) {
                    f.line(0xFFE8E8E8, 0.4f, lu0, v, lu1, v);
                    for (float u = lu0 + 3; u < lu1 - 4; u += 9)
                        f.rect(CLOTHES[(int) (u + v) % 8], u, v - 0.4f, u + 4, v + 3);
                }
                for (float u = lu1 + 24; u < L - 12; u += 34) f.tree(u, (v0 + v1) / 2 + ((int) u % 3 - 1) * 10, 6.5f);
                for (int k = 0; k < 6; k++) {
                    float u = 10 + k * 5;
                    f.line(0xFF3A3E46, 0.8f, u, a[3] + 1, u, a[3] + 4);
                }
                for (float u = cl + 30; u < L - 10; u += 40) f.tree(u, v1 - 2, 6);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                F f = new F(c, p, x0, y0, x1, y1);
                roofDeck(f, 0xFFA8A49C, r);
                for (float u = 12; u < f.L - 10; u += 24) f.rect(0xFF5A6A7A, u, f.S * 0.62f, u + 10, f.S * 0.62f + 3);
            }
        };
        d.ground = City.GRASS;
        d.minS = 10;
        d.minL = 12;
        d.base = City.APARTMENT;
        d.inside = Variants.I_DORM;
        d.roofCol = 0xFFA8A49C;
        out.add(d);
    }
}
