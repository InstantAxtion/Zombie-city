package com.instantaxtion.zombiesandbox;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.Landmarks.*;

/**
 * ((28.0) Landmark designs of the water maps (see Landmarks): the bayou's bait-and-tackle shops, seafood shack and
 * airboat landing; the coast's lighthouse, fish market and coast guard station. None suits any district (districts 0),
 * so no other map ever picks one: SwampMap and LighthouseMap put each where it goes, by name.
 */
final class WaterLandmarks {
    private WaterLandmarks() {
    }

    static final float TT = City.T;

    static void register(List<Landmarks.Def> out) {
        int first = out.size();
        baitShop(out);
        seafoodShack(out);
        airboatLanding(out);
        lighthouse(out);
        fishMarket(out);
        coastGuard(out);
        // (Only ever placed by name.)
        for (int i = first; i < out.size(); i++) out.get(i).districts = 0;
    }

    // ------------------------------------------------------------------ bits

    /** Weathered planks over x0..y1, running along the long side. */
    static void deck(Canvas c, Paint p, float x0, float y0, float x1, float y1, int col) {
        rect(c, p, City.darken(col, 0.8f), x0, y0, x1, y1);
        rect(c, p, col, x0 + 0.8f, y0 + 0.8f, x1 - 0.8f, y1 - 0.8f);
        boolean wide = x1 - x0 >= y1 - y0;
        int seams = 0xFF5A4630;
        if (wide) for (float y = y0 + 3; y < y1 - 1; y += 3) line(c, p, seams, 0.4f, x0 + 1, y, x1 - 1, y);
        else for (float x = x0 + 3; x < x1 - 1; x += 3) line(c, p, seams, 0.4f, x, y0 + 1, x, y1 - 1);
    }

    /** A small boat from above, bow towards +x (or +y if down). */
    static void boat(Canvas c, Paint p, float x, float y, float len, boolean down, int hull) {
        float w = len * 0.38f;
        if (!down) {
            oval(c, p, 0x40000000, x + 1, y - w / 2 + 1.5f, x + len + 1, y + w / 2 + 1.5f);
            poly(c, p, hull, x, y - w / 2, x + len * 0.7f, y - w / 2, x + len, y, x + len * 0.7f, y + w / 2, x, y + w / 2);
            rect(c, p, 0xFFE8E4DA, x + 1.2f, y - w / 2 + 1.2f, x + len * 0.65f, y + w / 2 - 1.2f);
        } else {
            oval(c, p, 0x40000000, x - w / 2 + 1, y + 1.5f, x + w / 2 + 1, y + len + 1.5f);
            poly(c, p, hull, x - w / 2, y, x - w / 2, y + len * 0.7f, x, y + len, x + w / 2, y + len * 0.7f, x + w / 2, y);
            rect(c, p, 0xFFE8E4DA, x - w / 2 + 1.2f, y + 1.2f, x + w / 2 - 1.2f, y + len * 0.65f);
        }
    }

    /** An airboat: a flat hull, the seat up high and the great fan in its cage at the back. */
    static void airboat(Canvas c, Paint p, float x, float y, float len, int hull) {
        float w = len * 0.5f;
        roundRect(c, p, 0x40000000, x + 1.2f, y - w / 2 + 1.5f, x + len + 1.2f, y + w / 2 + 1.5f, 2);
        poly(c, p, hull, x, y - w / 2, x + len * 0.8f, y - w / 2, x + len, y - w / 4, x + len, y + w / 4, x + len * 0.8f, y + w / 2, x, y + w / 2);
        rect(c, p, 0xFFB8B4AA, x + len * 0.3f, y - w / 2 + 1, x + len * 0.75f, y + w / 2 - 1);
        rect(c, p, 0xFF2E2E30, x + len * 0.3f, y - 1, x + len * 0.42f, y + 1);
        // The fan: a ring of mesh, the blades inside.
        circle(c, p, 0xFF3A3C40, x + len * 0.12f, y, w * 0.55f);
        circle(c, p, 0xFF8A8C90, x + len * 0.12f, y, w * 0.47f);
        line(c, p, 0xFF2A2A2C, 1, x + len * 0.12f, y - w * 0.42f, x + len * 0.12f, y + w * 0.42f);
        circle(c, p, 0xFF2A2A2C, x + len * 0.12f, y, 0.9f);
    }

    static void crate(Canvas c, Paint p, float x, float y, float s, int col) {
        rect(c, p, 0x40000000, x + 0.8f, y + 0.8f, x + s + 0.8f, y + s + 0.8f);
        rect(c, p, col, x, y, x + s, y + s);
        frame(c, p, City.darken(col, 0.7f), 0.4f, x + 0.4f, y + 0.4f, x + s - 0.4f, y + s - 0.4f);
    }

    static void reeds(Canvas c, Paint p, float x, float y, Random r) {
        for (int k = 0; k < 5; k++) {
            float dx = (r.nextFloat() - 0.5f) * 4, h = 2 + r.nextFloat() * 2;
            line(c, p, 0xFF6E7A3A, 0.5f, x + dx, y, x + dx + (r.nextFloat() - 0.5f), y - h);
        }
    }

    static Landmarks.Def def(String name, int base, int roof, byte ground, int minS, int minL, final int[][] rel, final Painter g) {
        Landmarks.Def d = new Landmarks.Def(name, T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                boolean wide = bw >= bh;
                int L = Math.max(bw, bh), S = Math.min(bw, bh);
                int[][] out = new int[rel.length][];
                for (int i = 0; i < rel.length; i++) {
                    // {u, v, lu, lv, floors}: tiles along the long side and across; a negative place counts from the far end.
                    int[] q = rel[i];
                    int lu = Math.min(q[2], L - 2), lv = Math.min(q[3], S - 2);
                    int u = q[0] >= 0 ? q[0] : L - lu + q[0], v = q[1] >= 0 ? q[1] : S - lv + q[1];
                    out[i] = wide ? new int[]{u, v, lu, lv, q[4]} : new int[]{v, u, lv, lu, q[4]};
                }
                return out;
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                g.paint(city, c, p, x0, y0, x1, y1, r, plan(Math.round((x1 - x0) / TT), Math.round((y1 - y0) / TT), null));
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                if (!g.roof(city, c, p, part, x0, y0, x1, y1)) flatRoof(c, p, x0, y0, x1, y1, roofCol);
            }
        };
        d.base = base;
        d.inside = base == City.SHOP ? Variants.I_SHELVES : Variants.I_OFFICE;
        d.roofCol = roof;
        d.ground = ground;
        d.minS = minS;
        d.minL = minL;
        return d;
    }

    abstract static class Painter {
        abstract void paint(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r, int[][] plan);

        boolean roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1) {
            return false;
        }
    }

    /** A plan building's rectangle in world units, from the block's top left. */
    static float[] box(float x0, float y0, int[] q) {
        return new float[]{x0 + q[0] * TT, y0 + q[1] * TT, x0 + (q[0] + q[2]) * TT, y0 + (q[1] + q[3]) * TT};
    }

    // ------------------------------------------------------------------ the bayou

    static void baitShop(List<Landmarks.Def> out) {
        out.add(def("Bait & Tackle", City.SHOP, 0xFF7A5A3A, City.DIRT, 8, 10, new int[][]{{1, 1, 6, 4, 1}}, new Painter() {
            @Override
            void paint(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r, int[][] plan) {
                rect(c, p, 0xFF8C7A5A, x0, y0, x1, y1);
                for (int k = 0; k < 40; k++)
                    circle(c, p, r.nextBoolean() ? 0xFF7A6A4C : 0xFFA08C68, x0 + r.nextFloat() * (x1 - x0), y0 + r.nextFloat() * (y1 - y0), 0.7f);
                float[] b = box(x0, y0, plan[0]);
                // The porch along the front, the bait tanks, coolers and a boat on its trailer in the yard.
                deck(c, p, b[0] - 2, b[3], b[2] + 2, b[3] + 6, 0xFF8A6A48);
                float yx = b[2] + 6, yy = y0 + 6;
                for (int k = 0; k < 3 && yx + 10 < x1; k++) {
                    roundRect(c, p, 0xFF3A6A8A, yx, yy + k * 7, yx + 10, yy + k * 7 + 5, 1.5f);
                    rect(c, p, 0xFF6A9AB8, yx + 1, yy + k * 7 + 1, yx + 9, yy + k * 7 + 4);
                }
                for (int k = 0; k < 4; k++) crate(c, p, b[0] + 2 + k * 5, b[3] + 8, 3.5f, k % 2 == 0 ? 0xFFE8E4DA : 0xFF2E6AB0);
                float tx = x0 + 6, ty = y1 - 12;
                if (ty > b[3] + 14) {
                    line(c, p, 0xFF3A3A3A, 1, tx, ty, tx + 26, ty);
                    boat(c, p, tx + 2, ty, 24, false, 0xFF2E5A3A);
                }
                sign(city, "BAIT & TACKLE", (b[0] + b[2]) / 2, b[3] + 3, 5, 0xFFF2E8C8);
            }

            @Override
            boolean roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1) {
                // A tin roof, rusted in streaks.
                rect(c, p, 0xFF8A8C88, x0, y0, x1, y1);
                for (float x = x0 + 1.5f; x < x1; x += 2.5f) line(c, p, 0xFF6E706C, 0.6f, x, y0, x, y1);
                rect(c, p, 0x509A5A30, x0 + (x1 - x0) * 0.6f, y0, x0 + (x1 - x0) * 0.75f, y1);
                return true;
            }
        }));
    }

    static void seafoodShack(List<Landmarks.Def> out) {
        out.add(def("Seafood Shack", City.SHOP, 0xFF9A3A2E, City.PLAZA, 8, 10, new int[][]{{1, 1, 5, 4, 1}}, new Painter() {
            @Override
            void paint(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r, int[][] plan) {
                rect(c, p, 0xFF5C6A3A, x0, y0, x1, y1);
                float[] b = box(x0, y0, plan[0]);
                // A deck out from the shack, with picnic tables under umbrellas.
                deck(c, p, b[2] + 1, y0 + 4, x1 - 3, y1 - 4, 0xFF9A7A54);
                for (float x = b[2] + 8; x < x1 - 8; x += 12)
                    for (float y = y0 + 10; y < y1 - 8; y += 12) {
                        rect(c, p, 0xFF6A4A30, x - 4, y - 1.5f, x + 4, y + 1.5f);
                        circle(c, p, r.nextBoolean() ? 0xFFD84A3A : 0xFFE8D84A, x, y, 3.5f);
                        circle(c, p, 0xFFF2F0E8, x, y, 0.8f);
                    }
                for (int k = 0; k < 6; k++) reeds(c, p, x0 + 2 + r.nextFloat() * (b[2] - x0), y1 - 2 - r.nextFloat() * 4, r);
                sign(city, "SEAFOOD", (b[0] + b[2]) / 2, b[3] + 4, 5, 0xFFF2E8C8);
            }
        }));
    }

    static void airboatLanding(List<Landmarks.Def> out) {
        out.add(def("Airboat Landing", City.SHOP, 0xFF5A6A4A, City.PLAZA, 10, 14, new int[][]{{1, 1, 4, 4, 1}}, new Painter() {
            @Override
            void paint(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r, int[][] plan) {
                rect(c, p, 0xFF5C6A3A, x0, y0, x1, y1);
                float[] b = box(x0, y0, plan[0]);
                // The slip: a run of bayou water cut in beside the ticket hut, the airboats tied up in it.
                boolean wide = x1 - x0 >= y1 - y0;
                float sx0 = wide ? b[2] + 6 : x0 + 4, sy0 = wide ? y0 + 4 : b[3] + 6;
                rect(c, p, 0xFF6A5A3A, sx0 - 1.5f, sy0 - 1.5f, x1 - 2.5f, y1 - 2.5f);
                rect(c, p, 0xFF44583C, sx0, sy0, x1 - 4, y1 - 4);
                rect(c, p, 0xFF3A4E34, sx0 + 2, sy0 + 2, x1 - 6, y1 - 6);
                deck(c, p, sx0, sy0 - 1, x1 - 4, sy0 + 3, 0xFF8A6A48);
                int[] hulls = {0xFF3A6A3A, 0xFF8A3A2E, 0xFF2E4A6A, 0xFFB8A040};
                int n = 0;
                for (float y = sy0 + 7; y < y1 - 9 && n < 6; y += 11)
                    for (float x = sx0 + 3; x + 22 < x1 - 5 && n < 6; x += 26) airboat(c, p, x, y, 20, hulls[n++ % hulls.length]);
                deck(c, p, b[0] - 1, b[3] + 1, b[2] + 1, b[3] + 6, 0xFF8A6A48);
                sign(city, "AIRBOAT TOURS", (b[0] + b[2]) / 2, b[3] + 3.5f, 4.5f, 0xFFF2E8C8);
            }
        }));
    }

    // ------------------------------------------------------------------ the coast

    static void lighthouse(List<Landmarks.Def> out) {
        Landmarks.Def d = def("Lighthouse", City.OFFICE, 0xFFE8E4DA, City.GRASS, 8, 10,
                new int[][]{{2, 2, 3, 3, 6}, {-1, -1, 4, 3, 1}}, new Painter() {
            @Override
            void paint(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r, int[][] plan) {
                rect(c, p, 0xFF587A46, x0, y0, x1, y1);
                for (int k = 0; k < 30; k++) circle(c, p, 0xFF4A6A3A, x0 + r.nextFloat() * (x1 - x0), y0 + r.nextFloat() * (y1 - y0), 0.9f);
                float[] t = box(x0, y0, plan[0]), h = box(x0, y0, plan[1]);
                float cx = (t[0] + t[2]) / 2, cy = (t[1] + t[3]) / 2;
                // The whitewashed wall round the yard, the path from the tower to the keeper's house.
                frame(c, p, 0xFFE0DCD0, 1.4f, x0 + 2, y0 + 2, x1 - 2, y1 - 2);
                line(c, p, 0xFFB8AE96, 3, cx, cy, (h[0] + h[2]) / 2, (h[1] + h[3]) / 2);
                circle(c, p, 0xFFB8AE96, cx, cy, (t[2] - t[0]) * 0.8f);
                // The long shadow of the tower.
                line(c, p, 0x38000000, (t[2] - t[0]) * 0.8f, cx, cy, cx + 30, cy + 34);
                for (int k = 0; k < 5; k++) Landmarks.bush(c, p, x0 + 5 + r.nextFloat() * (x1 - x0 - 10), y1 - 5 - r.nextFloat() * 6, 1.8f);
            }

            @Override
            boolean roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1) {
                if (part != 0) {
                    // The keeper's cottage: a red roof, ridged.
                    rect(c, p, 0xFF8A2E28, x0, y0, x1, y1);
                    rect(c, p, 0xFFA83A30, x0, y0, x1, (y0 + y1) / 2);
                    line(c, p, 0xFF6A221E, 0.8f, x0, (y0 + y1) / 2, x1, (y0 + y1) / 2);
                    rect(c, p, 0xFF6A6A6A, x1 - 5, y0 + 1, x1 - 3, y0 + 4);
                    return true;
                }
                // The tower from above: white and red bands stepping in, the gallery, the lantern's glass and its light.
                float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2, R = Math.min(x1 - x0, y1 - y0) / 2;
                circle(c, p, 0x50000000, cx + 3, cy + 3, R);
                circle(c, p, 0xFFF2F0E8, cx, cy, R);
                circle(c, p, 0xFFC0302A, cx, cy, R * 0.82f);
                circle(c, p, 0xFFF2F0E8, cx, cy, R * 0.64f);
                circle(c, p, 0xFF2A2A2C, cx, cy, R * 0.5f);
                circle(c, p, 0xFFF0E070, cx, cy, R * 0.36f);
                circle(c, p, 0xFFFFFFE0, cx - R * 0.08f, cy - R * 0.08f, R * 0.16f);
                circle(c, p, 0xFFC0302A, cx, cy, R * 0.1f);
                return true;
            }
        });
        d.inside = Variants.I_OFFICE;
        out.add(d);
    }

    static void fishMarket(List<Landmarks.Def> out) {
        out.add(def("Fish Market", City.SHOP, 0xFF6A7A86, City.PLAZA, 9, 12, new int[][]{{1, 1, 8, 5, 1}}, new Painter() {
            @Override
            void paint(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r, int[][] plan) {
                rect(c, p, 0xFFA8A49A, x0, y0, x1, y1);
                float[] b = box(x0, y0, plan[0]);
                // The stalls under their awnings, crates of ice and the fish boxes stacked by the door.
                for (float x = b[0]; x + 8 < b[2]; x += 10) {
                    rect(c, p, (int) (x / 10) % 2 == 0 ? 0xFF2E6AB0 : 0xFFF2F0E8, x, b[3] + 1, x + 8, b[3] + 5);
                    rect(c, p, 0xFF8A6A48, x + 1, b[3] + 5, x + 7, b[3] + 8);
                }
                for (int k = 0; k < 10; k++)
                    crate(c, p, x0 + 3 + r.nextFloat() * (x1 - x0 - 8), b[3] + 11 + r.nextFloat() * Math.max(1, y1 - b[3] - 16), 3,
                            r.nextBoolean() ? 0xFFE8E4DA : 0xFFD8A040);
                sign(city, "FISH MARKET", (b[0] + b[2]) / 2, b[3] + 3, 5, 0xFF1A2A3A);
            }

            @Override
            boolean roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1) {
                rect(c, p, 0xFF5A6A76, x0, y0, x1, y1);
                for (float x = x0 + 2; x < x1; x += 3) line(c, p, 0xFF7A8A96, 0.7f, x, y0, x, y1);
                return true;
            }
        }));
    }

    static void coastGuard(List<Landmarks.Def> out) {
        out.add(def("Coast Guard Station", City.OFFICE, 0xFFE8E4DA, City.PLAZA, 10, 14, new int[][]{{1, 1, 7, 5, 2}}, new Painter() {
            @Override
            void paint(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r, int[][] plan) {
                rect(c, p, 0xFFB8B4AA, x0, y0, x1, y1);
                float[] b = box(x0, y0, plan[0]);
                // The helipad and the flagpole; a lifeboat on its cradle.
                boolean wide = x1 - x0 >= y1 - y0;
                float hx = wide ? (b[2] + x1) / 2 : (x0 + x1) / 2, hy = wide ? (y0 + y1) / 2 : (b[3] + y1) / 2;
                float hr = Math.min(wide ? (x1 - b[2]) : (x1 - x0), wide ? (y1 - y0) : (y1 - b[3])) * 0.38f;
                if (hr > 6) {
                    circle(c, p, 0xFF5A5E62, hx, hy, hr);
                    circle(c, p, 0xFFE8E4DA, hx, hy, hr * 0.85f);
                    circle(c, p, 0xFF5A5E62, hx, hy, hr * 0.78f);
                    line(c, p, 0xFFE8E4DA, 1.6f, hx - hr * 0.3f, hy - hr * 0.4f, hx - hr * 0.3f, hy + hr * 0.4f);
                    line(c, p, 0xFFE8E4DA, 1.6f, hx + hr * 0.3f, hy - hr * 0.4f, hx + hr * 0.3f, hy + hr * 0.4f);
                    line(c, p, 0xFFE8E4DA, 1.6f, hx - hr * 0.3f, hy, hx + hr * 0.3f, hy);
                }
                circle(c, p, 0xFF8A8C90, b[0] + 2, b[3] + 5, 1);
                rect(c, p, 0xFFC0302A, b[0] + 2, b[3] + 4, b[0] + 8, b[3] + 6);
                boat(c, p, b[0] + 6, b[3] + 10, 22, false, 0xFFE07A2A);
            }

            @Override
            boolean roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1) {
                flatRoof(c, p, x0, y0, x1, y1, 0xFFE8E4DA);
                rect(c, p, 0xFFC0302A, x0 + 1.5f, y0 + 1.5f, x0 + (x1 - x0) * 0.2f, y1 - 1.5f);
                return true;
            }
        }));
    }
}
