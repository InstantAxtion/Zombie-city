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
        int g1 = AirbaseMap.clamp(Math.round(S * 0.1f), 3, 22);
        int rw = AirbaseMap.clamp(Math.round(S * 0.09f), 4, 12);
        int a0 = t1 + g1, a1 = a0 + rw;
        int g2 = AirbaseMap.clamp(Math.round(S * 0.14f), 5, 32);
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
    }

    static void stadium(List<Landmarks.Def> out) {
    }

    static void fanZone(List<Landmarks.Def> out) {
    }
}
