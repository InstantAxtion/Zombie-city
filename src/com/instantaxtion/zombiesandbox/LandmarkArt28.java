package com.instantaxtion.zombiesandbox;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.Landmarks.*;

/**
 * ((28.0) Landmark designs, part 28 (see Landmarks): the Military Airbase's airfield (its apron, taxiway and runways),
 * Border Town's customs yards, and Stadium City's stadium and fan zones. None of them suits any district (districts 0),
 * so no other map ever picks one: AirbaseMap, BorderMap and StadiumMap put each where it goes.
 */
final class LandmarkArt28 {
    private LandmarkArt28() {
    }

    static final float TT = City.T;

    private static Landmarks.Def airfieldUp, airfieldDown;

    static void register(List<Landmarks.Def> out) {
        int first = out.size();
        airfieldUp = airfield(out, true);
        airfieldDown = airfield(out, false);
        customsYard(out);
        stadium(out);
        fanZone(out);
        for (int i = first; i < out.size(); i++) out.get(i).districts = 0;
    }

    /** The airfield design for a base along the top of the town (up) or the bottom. */
    static Landmarks.Def airfield(boolean up) {
        Landmarks.all();
        return up ? airfieldUp : airfieldDown;
    }

    /**
     * Drawing in a block's own frame: u along x (0..U), v across it from its front (0) to its back (V) - the front the
     * top of the block, or (flip) its bottom.
     */
    static final class G {
        final Canvas c;
        final Paint p;
        final float x0, y0, U, V;
        final boolean flip;

        G(Canvas c, Paint p, float x0, float y0, float x1, float y1, boolean flip) {
            this.c = c;
            this.p = p;
            this.x0 = x0;
            this.y0 = y0;
            U = x1 - x0;
            V = y1 - y0;
            this.flip = flip;
        }

        float x(float u) {
            return x0 + u;
        }

        float y(float v) {
            return flip ? y0 + V - v : y0 + v;
        }

        void rect(int col, float u0, float v0, float u1, float v1) {
            float ya = y(v0), yb = y(v1);
            Landmarks.rect(c, p, col, x(Math.min(u0, u1)), Math.min(ya, yb), x(Math.max(u0, u1)), Math.max(ya, yb));
        }

        void circle(int col, float u, float v, float r) {
            Landmarks.circle(c, p, col, x(u), y(v), r);
        }

        void line(int col, float w, float u0, float v0, float u1, float v1) {
            Landmarks.line(c, p, col, w, x(u0), y(v0), x(u1), y(v1));
        }

        void poly(int col, float... q) {
            float[] m = new float[q.length];
            for (int k = 0; k + 1 < q.length; k += 2) {
                m[k] = x(q[k]);
                m[k + 1] = y(q[k + 1]);
            }
            Landmarks.poly(c, p, col, m);
        }

        void sign(City city, String s, float u, float v, float size, int col) {
            Landmarks.sign(city, s, x(u), y(v), size, col);
        }
    }

    // ------------------------------------------------------------------ the airfield

    /** Where the taxiway links to the main runway, and the main runway to the second (fractions of the length). */
    static final float[] LINKS_A = {0.05f, 0.3f, 0.55f, 0.8f, 0.95f}, LINKS_B = {0.15f, 0.5f, 0.85f};

    /**
     * The airfield's rows, in tiles from its front (the apron's edge by the hangars) for an airfield S tiles deep:
     * {apron from, apron to, taxiway from, to, main runway from, to, second runway from, to (-1, -1 for none)}.
     */
    static int[] airfieldRows(int S) {
        int ad = AirbaseMap.clamp(Math.round(S * 0.18f), 5, 30);
        int tw = S >= 40 ? 4 : 3;
        int t0 = ad + 1, t1 = t0 + tw;
        int g1 = AirbaseMap.clamp(Math.round(S * 0.15f), 3, 80);
        int rw = AirbaseMap.clamp(Math.round(S * 0.055f), 4, 16);
        int a0 = t1 + g1, a1 = a0 + rw;
        int g2 = AirbaseMap.clamp(Math.round(S * 0.28f), 5, 120);
        int b0 = -1, b1 = -1;
        if (a1 + g2 + rw + 4 <= S && S >= 40) {
            b0 = a1 + g2;
            b1 = b0 + rw;
        }
        return new int[]{0, ad, t0, t1, a0, a1, b0, b1};
    }

    private static final int GRASS = 0xFF5E8448, GRASS_MOWN = 0xFF668C4E, CONCRETE = 0xFFA6A69E, CONCRETE_JOINT = 0xFF94948C,
            TAXI = 0xFF55575A, RUNWAY = 0xFF3E4042, WHITE = 0xFFF0F0EC, YELLOW = 0xFFE6C23A;

    static Landmarks.Def airfield(List<Landmarks.Def> out, final boolean up) {
        // The airfield: mown grass, the concrete apron with fighters and transports parked in rows, the taxiway and its
        // links, one runway or two with their markings, and the approach lights beyond the ends.
        Landmarks.Def d = new Landmarks.Def("Airfield", T_BUILDING) {
            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                G g = new G(c, p, x0, y0, x1, y1, up);
                float U = g.U, V = g.V;
                int bw = Math.round(U / TT), S = Math.round(V / TT), L = bw + 2;
                int[] rows = airfieldRows(S);
                g.rect(GRASS, 0, 0, U, V);
                for (float u = 0; u < U; u += 64) g.rect(GRASS_MOWN, u, 0, u + 32, V);
                // The apron.
                float ap = rows[1] * TT;
                g.rect(CONCRETE, 0, 0, U, ap);
                for (float u = 24; u < U; u += 24) g.line(CONCRETE_JOINT, 0.6f, u, 0, u, ap);
                for (float v = 24; v < ap; v += 24) g.line(CONCRETE_JOINT, 0.6f, 0, v, U, v);
                // The taxiway and its links.
                float t0 = rows[2] * TT, t1 = rows[3] * TT, tm = (t0 + t1) / 2;
                g.rect(TAXI, 2 * TT, t0, U - 2 * TT, t1);
                g.line(YELLOW, 1f, 2 * TT, tm, U - 2 * TT, tm);
                // (A lead-in from the apron every so often.)
                for (float u = 60; u < U - 60; u += 180) {
                    g.rect(TAXI, u - 10, ap, u + 10, t0 + 1);
                    g.line(YELLOW, 0.8f, u, ap, u, tm);
                }
                float a0 = rows[4] * TT, a1 = rows[5] * TT, ra0 = Math.round(L * 0.03f) * TT - TT, ra1 = U - ra0;
                for (float f : LINKS_A) {
                    float lu = (Math.round(bw * f) - 1) * TT;
                    g.rect(TAXI, lu, t1 - 1, lu + 3 * TT, a0 + 1);
                    g.line(YELLOW, 0.8f, lu + 1.5f * TT, tm, lu + 1.5f * TT, a0);
                }
                if (rows[6] >= 0) {
                    float b0 = rows[6] * TT;
                    for (float f : LINKS_B) {
                        float lu = (Math.round(bw * f) - 1) * TT;
                        g.rect(TAXI, lu, a1 - 1, lu + 3 * TT, b0 + 1);
                        g.line(YELLOW, 0.8f, lu + 1.5f * TT, a1, lu + 1.5f * TT, b0);
                    }
                }
                runway(city, g, ra0, a0, ra1, a1, r, "09", "27");
                if (rows[6] >= 0) {
                    float rb0 = Math.round(L * 0.12f) * TT - TT, rb1 = U - rb0;
                    runway(city, g, rb0, rows[6] * TT, rb1, rows[7] * TT, r, "09R", "27L");
                }
                // The aircraft on the apron: two big transports by the middle, rows of fighters either side.
                float mid = U / 2;
                float ts = Math.min(ap * 0.8f, 110);
                if (ap >= 5 * TT) {
                    transport(g, mid - ts * 0.6f, ap * 0.5f, ts);
                    transport(g, mid + ts * 0.6f, ap * 0.5f, ts);
                }
                float fs = Math.min(ap * 0.6f, 50);
                for (float u = 40; u < U - 40; u += fs * 1.6f) {
                    if (Math.abs(u - mid) < ts * 1.4f) continue;
                    if (r.nextFloat() < 0.3f) continue;
                    fighter(g, u, ap * 0.5f, fs);
                }
                // A windsock by the main runway.
                float wu = U * 0.5f, wv = a0 - Math.max(6, (a0 - t1) * 0.4f);
                g.circle(0xFF6A6A6A, wu, wv, 1.6f);
                g.poly(0xFFE8642A, wu, wv - 1, wu + 9, wv - 2, wu + 9, wv + 2, wu, wv + 1);
            }
        };
        d.ground = City.BASE;
        d.minS = 12;
        d.minL = 40;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_FACTORY;
        out.add(d);
        return d;
    }

    /** A runway with its edge lines, centre line, threshold bars, numbers, touchdown marks and approach lights. */
    static void runway(City city, G g, float u0, float v0, float u1, float v1, Random r, String n0, String n1) {
        float w = v1 - v0, cv = (v0 + v1) / 2;
        g.rect(0xFF8E8C88, u0 - 2, v0 - 2, u1 + 2, v1 + 2);
        g.rect(RUNWAY, u0, v0, u1, v1);
        g.line(WHITE, 1.2f, u0 + 2, v0 + 2, u1 - 2, v0 + 2);
        g.line(WHITE, 1.2f, u0 + 2, v1 - 2, u1 - 2, v1 - 2);
        for (float u = u0 + 90; u < u1 - 90; u += 40) g.rect(WHITE, u, cv - 0.9f, u + 22, cv + 0.9f);
        for (int end = 0; end < 2; end++) {
            float ub = end == 0 ? u0 + 4 : u1 - 4 - 26, dir = end == 0 ? 1 : -1;
            for (float v = v0 + 5; v < v1 - 5; v += 6) g.rect(WHITE, ub, v, ub + 26, v + 3.2f);
            float tz = end == 0 ? u0 + 110 : u1 - 110;
            for (int k = 0; k < 3; k++)
                for (int s = -1; s <= 1; s += 2) {
                    float uu = tz + dir * k * 36, vv = cv + s * w * 0.25f;
                    g.rect(WHITE, Math.min(uu, uu + dir * 18), vv - 1.6f, Math.max(uu, uu + dir * 18), vv + 1.6f);
                }
            for (int k = 0; k < 8; k++) {
                float uu = tz + dir * r.nextFloat() * 160;
                g.rect(0x26000000, uu, cv - w * 0.15f + r.nextFloat() * w * 0.3f, uu + dir * (10 + r.nextFloat() * 24), cv - w * 0.15f + r.nextFloat() * w * 0.3f + 1.4f);
            }
            // The approach lights out on the grass.
            float ue = end == 0 ? u0 : u1;
            for (int k = 1; k <= 5; k++) g.circle(0xFFF6F2C0, ue - dir * k * 9, cv, 1.4f);
        }
        float size = Math.max(6, Math.min(16, w / 4));
        g.sign(city, n0, u0 + 52, cv, size, WHITE);
        g.sign(city, n1, u1 - 52, cv, size, WHITE);
    }

    /** A fighter from above, nose to +u, s long. */
    static void fighter(G g, float u, float v, float s) {
        g.poly(0x38000000, u + s * 0.5f + 3, v + 3, u - s * 0.1f + 3, v - s * 0.45f + 3, u - s * 0.3f + 3, v + 3, u - s * 0.1f + 3, v + s * 0.45f + 3);
        g.poly(0xFF7C868E, u + s * 0.1f, v, u - s * 0.15f, v - s * 0.48f, u - s * 0.32f, v - s * 0.48f, u - s * 0.2f, v,
                u - s * 0.32f, v + s * 0.48f, u - s * 0.15f, v + s * 0.48f);
        g.poly(0xFF6C767E, u - s * 0.36f, v, u - s * 0.46f, v - s * 0.22f, u - s * 0.52f, v - s * 0.22f, u - s * 0.48f, v,
                u - s * 0.52f, v + s * 0.22f, u - s * 0.46f, v + s * 0.22f);
        g.rect(0xFF8A949C, u - s * 0.5f, v - s * 0.07f, u + s * 0.5f, v + s * 0.07f);
        g.rect(0xFF2A3A4C, u + s * 0.18f, v - s * 0.04f, u + s * 0.36f, v + s * 0.04f);
    }

    /** A four-engined transport from above, nose to +u, s long. */
    static void transport(G g, float u, float v, float s) {
        g.rect(0x38000000, u - s * 0.5f + 4, v - s * 0.07f + 4, u + s * 0.5f + 4, v + s * 0.07f + 4);
        g.poly(0x38000000, u + s * 0.05f + 4, v + 4, u - s * 0.12f + 4, v - s * 0.55f + 4, u - s * 0.2f + 4, v + 4, u - s * 0.12f + 4, v + s * 0.55f + 4);
        g.poly(0xFF8C9298, u + s * 0.06f, v - s * 0.06f, u - s * 0.06f, v - s * 0.56f, u - s * 0.16f, v - s * 0.56f, u - s * 0.1f, v,
                u - s * 0.16f, v + s * 0.56f, u - s * 0.06f, v + s * 0.56f, u + s * 0.06f, v + s * 0.06f);
        for (int k = -2; k <= 2; k++) {
            if (k == 0) continue;
            float ev = v + k * s * 0.17f;
            g.rect(0xFF5A6066, u - s * 0.01f + Math.abs(k) * -s * 0.025f, ev - s * 0.025f, u + s * 0.09f + Math.abs(k) * -s * 0.025f, ev + s * 0.025f);
        }
        g.rect(0xFF9AA0A6, u - s * 0.5f, v - s * 0.075f, u + s * 0.5f, v + s * 0.075f);
        g.poly(0xFF8C9298, u - s * 0.36f, v, u - s * 0.46f, v - s * 0.22f, u - s * 0.52f, v - s * 0.22f, u - s * 0.5f, v,
                u - s * 0.52f, v + s * 0.22f, u - s * 0.46f, v + s * 0.22f);
        g.rect(0xFF2A3A4C, u + s * 0.4f, v - s * 0.04f, u + s * 0.47f, v + s * 0.04f);
    }

    // ------------------------------------------------------------------ Border Town and Stadium City (below)

    static void customsYard(List<Landmarks.Def> out) {
        // A customs yard: the inspection shed along the back, the customs office at one end, the inspection lanes in
        // front with their booths and barriers, and lorries queued in them waiting to be searched.
        Landmarks.Def d = new Landmarks.Def("Customs Yard", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int sd = Math.max(3, Math.min(6, S / 3)), ol = Math.max(4, Math.min(8, L / 4));
                return new int[][]{LandmarkArt12.at(al, ol + 2, 0, L - ol - 2, sd, 1), LandmarkArt12.at(al, 0, 0, ol, Math.max(4, S - 3), 2)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                LandmarkArt12.F f = new LandmarkArt12.F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[][] pq = LandmarkArt12.pl(this, x0, y0, x1, y1);
                float[] shed = LandmarkArt12.bx(f, pq[0]);
                f.rect(0xFF5A5C60, 0, 0, L, S);
                float v0 = shed[3] + 4, u0 = shed[0], u1 = L - 2;
                int lanes = Math.max(2, Math.min(6, (int) ((u1 - u0) / 34)));
                float lw = (u1 - u0) / lanes;
                for (int k = 0; k <= lanes; k++) {
                    float u = u0 + k * lw;
                    f.rect(0xFFE8C23A, u - 1, v0, u + 1, S - 2);
                    // A booth on the island between lanes, a striped barrier across each.
                    if (k < lanes) {
                        f.rect(0xFF2E2E2E, u + 3, S * 0.55f - 1, u + lw - 3, S * 0.55f + 1);
                        for (float uu = u + 3; uu < u + lw - 3; uu += 6) f.rect(0xFFD8322A, uu, S * 0.55f - 1, uu + 3, S * 0.55f + 1);
                    }
                    f.roundRect(0xFFE6E2D8, u - 3, S * 0.55f - 6, u + 3, S * 0.55f + 2, 1);
                    f.rect(0xFF3A6A9A, u - 2, S * 0.55f - 5, u + 2, S * 0.55f - 2);
                }
                // Lorries waiting in the lanes, nose to the shed.
                int[] cabs = {0xFFC83A2A, 0xFF2E5FB0, 0xFFE8E4DA, 0xFF3C7A4E, 0xFFD4A21C};
                for (int k = 0; k < lanes; k++) {
                    if (r.nextFloat() < 0.25f) continue;
                    float u = u0 + (k + 0.5f) * lw, v = S * 0.55f + 6;
                    if (v + 34 > S) continue;
                    f.rect(0x38000000, u - 5, v + 2, u + 7, v + 36);
                    f.rect(cabs[r.nextInt(cabs.length)], u - 6, v, u + 6, v + 8);
                    f.rect(0xFF2A3A4A, u - 5, v + 1, u + 5, v + 3);
                    f.rect(r.nextBoolean() ? 0xFFB8BCC0 : 0xFF8A6A4A, u - 6.5f, v + 9, u + 6.5f, v + 34);
                }
                f.sign(city, "CUSTOMS", (u0 + u1) / 2, S - 6, Math.max(4, Math.min(8, S / 10)), 0xFFF2F2EE);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                LandmarkArt12.F f = new LandmarkArt12.F(c, p, x0, y0, x1, y1);
                if (part == 0) {
                    // The inspection shed: a long ribbed canopy.
                    f.rect(0xFF8C949C, 0, 0, f.L, f.S);
                    for (float u = 3; u < f.L; u += 4) f.line(0x22000000, 0.6f, u, 0, u, f.S);
                    f.rect(0xFFB0B8C0, 0, 0, f.L, f.S * 0.3f);
                    return;
                }
                Landmarks.flatRoof(c, p, x0, y0, x1, y1, 0xFF6E747A);
                f.circle(0xFFE8E8E8, f.L * 0.7f, f.S * 0.5f, Math.min(f.L, f.S) * 0.15f);
            }
        };
        d.ground = City.LOT;
        d.minS = 8;
        d.minL = 14;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_OFFICE;
        d.roofCol = 0xFF8C949C;
        out.add(d);
    }

    /** The home side's colours, seat and trim, from the city: every city its own club. */
    static int[] club(City city) {
        int[][] k = {{0xFFC8282A, 0xFFF2F2EE}, {0xFF1E4FA8, 0xFFF2F2EE}, {0xFF1E7A3E, 0xFFF2D24A}, {0xFF6A2A8A, 0xFFF2D24A},
                {0xFFE8C21E, 0xFF1E1E22}, {0xFFE8641E, 0xFF1E1E22}, {0xFF2A9AC8, 0xFFF2F2EE}, {0xFF8A1E2A, 0xFF4A9AD0}};
        return k[(int) Math.abs(city.cfg.seed % k.length)];
    }

    /** The stand thickness and the concourse round it, for a stadium of short side S tiles. */
    static int standT(int S) {
        return Math.max(3, Math.min(9, S / 5));
    }

    static void stadium(List<Landmarks.Def> out) {
        // The great stadium: four stands round the pitch (people go in, shelter, fight from them), the open corners of
        // the bowl with their tiers of seats, the floodlight masts, the big screen, and the concourse all round.
        Landmarks.Def d = new Landmarks.Def("Grand Stadium", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean al = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh), m = 2, t = standT(S);
                int hu = L / 2, hv = S / 2;
                return new int[][]{
                        LandmarkArt12.at(al, m + t, m, hu - 1 - (m + t), t, 4), LandmarkArt12.at(al, hu + 1, m, L - m - t - hu - 1, t, 4),
                        LandmarkArt12.at(al, m + t, S - m - t, hu - 1 - (m + t), t, 4), LandmarkArt12.at(al, hu + 1, S - m - t, L - m - t - hu - 1, t, 4),
                        LandmarkArt12.at(al, m, m + t, t, hv - 1 - (m + t), 3), LandmarkArt12.at(al, m, hv + 1, t, S - m - t - hv - 1, 3),
                        LandmarkArt12.at(al, L - m - t, m + t, t, hv - 1 - (m + t), 3), LandmarkArt12.at(al, L - m - t, hv + 1, t, S - m - t - hv - 1, 3)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                LandmarkArt12.F f = new LandmarkArt12.F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S, m = 2 * TT, t = standT(Math.round(S / TT)) * TT;
                int[] k = club(city);
                // The concourse: big pale slabs, a ring of trees and lamps.
                f.rect(0xFFC4BCA8, 0, 0, L, S);
                for (float u = 12; u < L; u += 24) f.line(0x18000000, 0.6f, u, 0, u, S);
                for (float v = 12; v < S; v += 24) f.line(0x18000000, 0.6f, 0, v, L, v);
                // The bowl: its outer wall, the tiers in the corners.
                f.roundRect(0xFF6A6E74, m - 3, m - 3, L - m + 3, S - m + 3, t * 1.2f);
                f.roundRect(0xFF8A8E94, m, m, L - m, S - m, t);
                for (int q = 0; q < 4; q++) {
                    float cu = q % 2 == 0 ? m + t : L - m - t, cv = q < 2 ? m + t : S - m - t;
                    for (int ring = 0; ring < 5; ring++) {
                        float rr = t * (1 - ring * 0.18f);
                        f.circle(ring % 2 == 0 ? k[0] : City.darken(k[0], 0.85f), cu, cv, rr);
                    }
                    f.circle(0xFF8A8E94, cu, cv, t * 0.12f);
                }
                // The pitch, mown in stripes, its lines.
                float pu0 = m + t + 4, pv0 = m + t + 4, pu1 = L - m - t - 4, pv1 = S - m - t - 4;
                f.rect(0xFF4E8A3A, m + t, m + t, L - m - t, S - m - t);
                for (float u = pu0; u < pu1; u += 24) f.rect(0xFF57953F, u, pv0, Math.min(pu1, u + 12), pv1);
                f.frame(0xFFF2F2EE, 1.2f, pu0, pv0, pu1, pv1);
                f.line(0xFFF2F2EE, 1.2f, (pu0 + pu1) / 2, pv0, (pu0 + pu1) / 2, pv1);
                f.ring(0xFFF2F2EE, 1.2f, (pu0 + pu1) / 2, (pv0 + pv1) / 2, Math.min(30, (pv1 - pv0) * 0.18f));
                float bh = (pv1 - pv0) * 0.5f, bd = Math.min(40, (pu1 - pu0) * 0.14f);
                f.frame(0xFFF2F2EE, 1.2f, pu0, (pv0 + pv1) / 2 - bh / 2, pu0 + bd, (pv0 + pv1) / 2 + bh / 2);
                f.frame(0xFFF2F2EE, 1.2f, pu1 - bd, (pv0 + pv1) / 2 - bh / 2, pu1, (pv0 + pv1) / 2 + bh / 2);
                f.rect(0xFFF2F2EE, pu0 - 2, (pv0 + pv1) / 2 - 6, pu0, (pv0 + pv1) / 2 + 6);
                f.rect(0xFFF2F2EE, pu1, (pv0 + pv1) / 2 - 6, pu1 + 2, (pv0 + pv1) / 2 + 6);
                // The floodlight masts on the corners of the bowl.
                for (int q = 0; q < 4; q++) {
                    float cu = q % 2 == 0 ? m + 4 : L - m - 4, cv = q < 2 ? m + 4 : S - m - 4;
                    f.rect(0x40000000, cu - 4, cv - 2, cu + 8, cv + 6);
                    f.rect(0xFF5A5E64, cu - 5, cv - 3, cu + 5, cv + 3);
                    for (float uu = cu - 4; uu < cu + 4; uu += 2.5f) f.rect(0xFFFFF6C8, uu, cv - 2, uu + 1.6f, cv + 2);
                }
                // Banners on the concourse in the club's colours, trees in their planters.
                for (float u = 20; u < L - 20; u += 46) {
                    f.rect(k[0], u, 3, u + 6, 6);
                    f.rect(k[1], u + 6, 3, u + 12, 6);
                }
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                // A stand from above: rows of seats in the club's colours rising away from the pitch, the roof's edge
                // over the back half.
                int[] k = club(city);
                boolean wide = x1 - x0 >= y1 - y0;
                f2(c, p, x0, y0, x1, y1, wide, k, part);
            }
        };
        d.ground = City.PLAZA;
        d.minS = 22;
        d.minL = 28;
        d.base = City.OFFICE;
        d.inside = Variants.I_SEATS;
        d.roofCol = 0xFF8A8E94;
        out.add(d);
    }

    /** A stand's seats and roof edge. */
    static void f2(Canvas c, Paint p, float x0, float y0, float x1, float y1, boolean wide, int[] k, int part) {
        Landmarks.rect(c, p, 0xFF5A5E64, x0, y0, x1, y1);
        float d = wide ? y1 - y0 : x1 - x0;
        // (Which side faces the pitch: the long stands' inner side, by their place in the plan.)
        boolean inLow = part == 0 || part == 1 || part == 4 || part == 5;
        for (float s = 1.5f; s < d * 0.62f; s += 2.6f) {
            int col = ((int) (s / 2.6f)) % 3 == 2 ? k[1] : k[0];
            float a = inLow ? d - s - 1.4f : s;
            if (wide) Landmarks.rect(c, p, col, x0 + 1, y0 + a, x1 - 1, y0 + a + 1.6f);
            else Landmarks.rect(c, p, col, x0 + a, y0 + 1, x0 + a + 1.6f, y1 - 1);
        }
        // The roof over the back rows: pale, ribbed.
        float r0 = inLow ? 0 : d * 0.62f, r1 = inLow ? d * 0.38f : d;
        if (wide) {
            Landmarks.rect(c, p, 0xFFDCDEE0, x0, y0 + r0, x1, y0 + r1);
            for (float x = x0 + 4; x < x1; x += 6) Landmarks.rect(c, p, 0x22000000, x, y0 + r0, x + 0.8f, y0 + r1);
        } else {
            Landmarks.rect(c, p, 0xFFDCDEE0, x0 + r0, y0, x0 + r1, y1);
            for (float y = y0 + 4; y < y1; y += 6) Landmarks.rect(c, p, 0x22000000, x0 + r0, y, x0 + r1, y + 0.8f);
        }
    }

    static void fanZone(List<Landmarks.Def> out) {
        // A fan zone: a great paved square with the big screen at one end, food and merchandise stalls under striped
        // awnings down the sides, flags strung over it, trees in planters and the crowd's benches.
        Landmarks.Def d = new Landmarks.Def("Fan Zone", T_PARK) {
            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                LandmarkArt12.F f = new LandmarkArt12.F(c, p, x0, y0, x1, y1);
                float L = f.L, S = f.S;
                int[] k = club(city);
                f.rect(0xFFCDBF9E, 0, 0, L, S);
                // Paving in a big chequer of two tones, a club-coloured band down the middle.
                for (float u = 0; u < L; u += 32)
                    for (float v = 0; v < S; v += 32) if (((int) (u / 32) + (int) (v / 32)) % 2 == 0) f.rect(0xFFC2B492, u, v, u + 32, v + 32);
                f.rect(City.lighten(k[0], 0.2f), 0, S / 2 - 3, L, S / 2 + 3);
                // The big screen on its frame at the far end, facing down the square.
                float sw = Math.min(S * 0.6f, 90);
                f.rect(0x40000000, L - 14, S / 2 - sw / 2 + 3, L - 4, S / 2 + sw / 2 + 3);
                f.rect(0xFF2A2C30, L - 16, S / 2 - sw / 2, L - 6, S / 2 + sw / 2);
                f.rect(0xFF3A6AA8, L - 15, S / 2 - sw / 2 + 2, L - 12, S / 2 + sw / 2 - 2);
                // Stalls down both long sides.
                for (float u = 10; u < L - 30; u += 26) {
                    for (int side = 0; side < 2; side++) {
                        float v = side == 0 ? 4 : S - 16;
                        f.rect(0x30000000, u + 2, v + 2, u + 20, v + 14);
                        for (float uu = u; uu < u + 18; uu += 3) f.rect((int) ((uu - u) / 3) % 2 == 0 ? k[0] : k[1], uu, v, uu + 3, v + 12);
                    }
                }
                // Planters with trees, and the crowd.
                for (float u = 40; u < L - 40; u += 60)
                    for (int side = 0; side < 2; side++) {
                        float v = side == 0 ? S * 0.3f : S * 0.7f;
                        f.circle(0xFF8A8478, u, v, 6);
                        f.tree(u, v, 5);
                    }
                for (int n = 0; n < (int) (L * S / 900); n++) {
                    float u = 20 + r.nextFloat() * (L - 50), v = 20 + r.nextFloat() * (S - 40);
                    LandmarkArt12.person(f, u, v, r.nextFloat() < 0.5f ? k[0] : LandmarkArt12.CLOTHES[r.nextInt(LandmarkArt12.CLOTHES.length)]);
                }
                // Flags strung across it.
                for (float u = 30; u < L; u += 50)
                    for (float v = 6; v < S - 6; v += 8) f.poly((int) (v / 8) % 2 == 0 ? k[0] : k[1], u - 2, v, u + 2, v, u, v + 3);
            }
        };
        d.ground = City.PLAZA;
        d.minS = 8;
        d.minL = 10;
        out.add(d);
    }
}
