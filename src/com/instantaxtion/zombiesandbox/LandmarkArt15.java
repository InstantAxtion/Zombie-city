package com.instantaxtion.zombiesandbox;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.Landmarks.*;

/**
 * ((27.0) Landmark designs, part 15 (see Landmarks): Refinery Bay's own - the tank farms, the cracking plant with its
 * towers, pipe racks and flare stack, the loading racks on their rail siding, the depot, the tanker terminal, the pipes
 * out along the jetty and the tanker moored at it. None of them suits any district (districts 0), so no other map ever
 * picks one: City.refineryTown and refineryPlaces put each where it goes. The storage tanks and the tanker are
 * buildings to the game (City.Building.tank): hit hard enough they go up (World.tankGoesUp), and burnt paints what's left.
 */
final class LandmarkArt15 {
    private LandmarkArt15() {
    }

    static final float TT = City.T;
    static final int GRAVEL = 0xFFB4AD9C, GRAVEL_DARK = 0xFF9C9584, PAD = 0xFFC6C2B6, BUND = 0xFFA9A394, BUND_EDGE = 0xFF86806F,
            PIPE = 0xFF8C9096, PIPE_LIGHT = 0xFFC2C6CA, PIPE_YELLOW = 0xFFE0BC38, STEEL = 0xFF6E7378, SEA = 0xFF27587F;
    private static final int[] TANK_COLS = {0xFFF1F0EA, 0xFFF1F0EA, 0xFFE4E7EA, 0xFFF1F0EA, 0xFFD3DAD0, 0xFFE9E2CF};

    static void register(List<Landmarks.Def> out) {
        int first = out.size();
        tankFarm(out);
        crackingPlant(out);
        loadingRacks(out);
        depot(out);
        terminal(out);
        culvert(out);
        tanker(out);
        // (Only ever placed by name, on Refinery Bay.)
        for (int i = first; i < out.size(); i++) out.get(i).districts = 0;
    }

    // ------------------------------------------------------------------ shared bits

    /** The yard everything in the refinery stands on: gravel, oil stains, a concrete pad inside a kerb. */
    static void yard(Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
        rect(c, p, GRAVEL, x0, y0, x1, y1);
        int n = (int) ((x1 - x0) * (y1 - y0) / 3500);
        for (int k = 0; k < n; k++) {
            float x = x0 + r.nextFloat() * (x1 - x0), y = y0 + r.nextFloat() * (y1 - y0), s = 8 + r.nextFloat() * 26;
            oval(c, p, k % 3 == 0 ? 0x18201810 : 0x14FFFFFF, x - s, y - s * 0.6f, x + s, y + s * 0.6f);
        }
        frame(c, p, GRAVEL_DARK, 1.2f, x0 + 2, y0 + 2, x1 - 2, y1 - 2);
    }

    /** A run of pipes side by side on their sleepers, from (ax, ay) to (bx, by): n of them, one yellow. */
    static void pipes(Canvas c, Paint p, float ax, float ay, float bx, float by, int n) {
        pipes(c, p, ax, ay, bx, by, n, 1);
    }

    /** ...each s times as thick (a trunk line is 2 or more). */
    static void pipes(Canvas c, Paint p, float ax, float ay, float bx, float by, int n, float s) {
        float dx = bx - ax, dy = by - ay, len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return;
        float nx = -dy / len, ny = dx / len, pitch = 1.8f * s, half = n * pitch / 2;
        line(c, p, 0x38000000, n * pitch + 1.5f * s, ax + 1.5f * s, ay + 1.5f * s, bx + 1.5f * s, by + 1.5f * s);
        for (float t = 6 * s; t < len - 2; t += 14 * s) {
            float mx = ax + dx / len * t, my = ay + dy / len * t;
            line(c, p, STEEL, 1.6f * s, mx - nx * (half + 1.6f * s), my - ny * (half + 1.6f * s), mx + nx * (half + 1.6f * s), my + ny * (half + 1.6f * s));
        }
        for (int k = 0; k < n; k++) {
            float o = (k - (n - 1) / 2f) * pitch;
            line(c, p, k == n / 2 ? PIPE_YELLOW : k % 2 == 0 ? PIPE_LIGHT : PIPE, 1.3f * s, ax + nx * o, ay + ny * o, bx + nx * o, by + ny * o);
        }
    }

    /** A drum lying on its saddles from (ax, y) to (bx, y), radius r. */
    static void drum(Canvas c, Paint p, float ax, float bx, float y, float r, int col) {
        p.setStrokeCap(Paint.Cap.ROUND);
        line(c, p, 0x40000000, r * 2, ax + r * 0.5f, y + r * 0.6f, bx + r * 0.5f, y + r * 0.6f);
        line(c, p, City.darken(col, 0.72f), r * 2, ax, y, bx, y);
        line(c, p, col, r * 1.3f, ax, y - r * 0.2f, bx, y - r * 0.2f);
        line(c, p, City.lighten(col, 0.25f), r * 0.4f, ax, y - r * 0.45f, bx, y - r * 0.45f);
        p.setStrokeCap(Paint.Cap.BUTT);
        rect(c, p, STEEL, ax + (bx - ax) * 0.2f - 1, y - r - 1, ax + (bx - ax) * 0.2f + 1, y + r + 1);
        rect(c, p, STEEL, ax + (bx - ax) * 0.8f - 1, y - r - 1, ax + (bx - ax) * 0.8f + 1, y + r + 1);
    }

    /** A sphere of gas on its legs, radius r, in a bund. */
    static void sphere(Canvas c, Paint p, float x, float y, float r) {
        roundRect(c, p, BUND_EDGE, x - r * 1.45f, y - r * 1.45f, x + r * 1.45f, y + r * 1.45f, 6);
        roundRect(c, p, BUND, x - r * 1.45f + 2.2f, y - r * 1.45f + 2.2f, x + r * 1.45f - 2.2f, y + r * 1.45f - 2.2f, 5);
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3;
            circle(c, p, STEEL, x + (float) Math.cos(a) * r * 0.95f, y + (float) Math.sin(a) * r * 0.95f, 2);
        }
        circle(c, p, 0x48000000, x + r * 0.3f, y + r * 0.36f, r);
        circle(c, p, 0xFFA9AEB4, x, y, r);
        circle(c, p, 0xFFDFE1E4, x - r * 0.12f, y - r * 0.12f, r * 0.84f);
        circle(c, p, 0xFFF6F7F8, x - r * 0.32f, y - r * 0.32f, r * 0.4f);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1);
        p.setColor(0x30000000);
        c.drawCircle(x, y, r * 0.6f, p);
        p.setStyle(Paint.Style.FILL);
        line(c, p, 0xFF7A7E84, 1.4f, x, y, x + r, y - r);
        circle(c, p, 0xFF5E6268, x, y, 2.4f);
    }

    /** The colour of the tank whose look (City.Building.look: the first int from its seed) is h. */
    static int tankCol(int h) {
        return TANK_COLS[(h >>> 4) % TANK_COLS.length];
    }

    /**
     * A round tank from above, radius r: its shell, lighter roof, seams, the stair up its side, the gauger's platform
     * and walkway and the vent. h is its look, for its colour and which way round it stands; rf a rectangle to use
     * (the map's painters and the view each bring their own: they draw on different threads).
     */
    static void tankTop(Canvas c, Paint p, RectF rf, float x, float y, float r, int h) {
        int col = tankCol(h);
        p.setStyle(Paint.Style.FILL);
        circle(c, p, City.darken(col, 0.62f), x, y, r);
        circle(c, p, City.darken(col, 0.86f), x, y, r - 1.4f);
        circle(c, p, col, x - r * 0.06f, y - r * 0.06f, r * 0.88f);
        circle(c, p, City.lighten(col, 0.1f), x - r * 0.26f, y - r * 0.26f, r * 0.42f);
        float a0 = ((h >>> 8) & 1023) / 1023f * 6.28f;
        for (int k = 0; k < 8; k++) {
            double a = a0 + k * Math.PI / 4;
            line(c, p, 0x1E000000, 0.6f, x, y, x + (float) Math.cos(a) * (r - 1.6f), y + (float) Math.sin(a) * (r - 1.6f));
        }
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(0.7f);
        p.setColor(0x26000000);
        c.drawCircle(x, y, r * 0.55f, p);
        p.setStrokeWidth(Math.max(1.8f, r * 0.06f));
        p.setColor(0xFF4E5258);
        float sa = (float) Math.toDegrees(a0) + 20;
        rf.set(x - r + 0.9f, y - r + 0.9f, x + r - 0.9f, y + r - 0.9f);
        c.drawArc(rf, sa, 70, false, p);
        p.setStyle(Paint.Style.FILL);
        double ea = Math.toRadians(sa + 70);
        float ex = x + (float) Math.cos(ea) * (r - 1.5f), ey = y + (float) Math.sin(ea) * (r - 1.5f), pw = Math.max(1.8f, r * 0.07f);
        rect(c, p, 0xFF5E6268, ex - pw, ey - pw, ex + pw, ey + pw);
        line(c, p, 0xFF7A7E84, 1.1f, ex, ey, x, y);
        circle(c, p, 0xFF5E6268, x, y, pw);
        circle(c, p, 0xFF9CA0A6, x, y, pw * 0.5f);
        // Rust streaked in from the rim on the odd one.
        if ((h & 3) == 0) {
            double a = a0 + 2.4;
            line(c, p, 0x309A5A2A, r * 0.08f, x + (float) Math.cos(a) * r * 0.5f, y + (float) Math.sin(a) * r * 0.5f,
                    x + (float) Math.cos(a) * (r - 1), y + (float) Math.sin(a) * (r - 1));
        }
    }

    /** A process column from above (a tall tower): its long shadow, the shell, platforms and the pipe up it. */
    static void column(Canvas c, Paint p, float x, float y, float r, float tall, int col) {
        line(c, p, 0x40000000, r * 2, x + r * 0.4f, y + r * 0.4f, x + tall * 0.55f, y + tall * 0.7f);
        circle(c, p, 0x40000000, x + tall * 0.55f, y + tall * 0.7f, r);
        circle(c, p, STEEL, x, y, r + 1.6f);
        circle(c, p, City.darken(col, 0.8f), x, y, r);
        circle(c, p, col, x - r * 0.15f, y - r * 0.15f, r * 0.75f);
        circle(c, p, 0xFF4E5258, x, y, r * 0.25f);
        rect(c, p, 0xFF5E6268, x + r, y - 1.2f, x + r + 3, y + 1.2f);
    }

    /** The flare stack at (x, y), from above: its guys, its long shadow and the tip (the view burns a flame over it). */
    static void flareStack(Canvas c, Paint p, float x, float y, float tall) {
        line(c, p, 0x40000000, 5, x + 2, y + 2, x + tall * 0.55f, y + tall * 0.7f);
        for (int k = 0; k < 3; k++) {
            double a = k * 2.094 + 0.5;
            float gx = x + (float) Math.cos(a) * 30, gy = y + (float) Math.sin(a) * 30;
            line(c, p, 0xFF5E6268, 0.9f, x, y, gx, gy);
            rect(c, p, 0xFF6E6860, gx - 2, gy - 2, gx + 2, gy + 2);
        }
        circle(c, p, 0xFF3E4248, x, y, 7.5f);
        circle(c, p, 0xFF8A8E94, x, y, 5.6f);
        circle(c, p, 0xFF2A2C30, x, y, 3.2f);
        circle(c, p, 0xFFE8742A, x, y, 2);
    }

    // ------------------------------------------------------------------ the tank farm

    /** How big across (tiles) the tanks on a bw by bh parcel are, and the gap between them. */
    private static int tankSize(int bw, int bh) {
        int s = Math.min(bw, bh);
        return s >= 40 ? 7 : s >= 22 ? 6 : 5;
    }

    static void tankFarm(List<Landmarks.Def> out) {
        // Rows of round storage tanks, each in its bund, the pipes from every one to the manifold between the rows.
        Landmarks.Def d = new Landmarks.Def("Tank Farm", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int ts = tankSize(bw, bh), gap = 4, m = 3;
                int nx = (bw - 2 * m + gap) / (ts + gap), ny = (bh - 2 * m + gap) / (ts + gap);
                if (nx < 1 || ny < 1) return new int[0][];
                int ox = (bw - (nx * (ts + gap) - gap)) / 2, oy = (bh - (ny * (ts + gap) - gap)) / 2;
                int[][] q = new int[nx * ny][];
                for (int j = 0; j < ny; j++)
                    for (int i = 0; i < nx; i++) q[j * nx + i] = new int[]{ox + i * (ts + gap), oy + j * (ts + gap), ts, ts, 2};
                return q;
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                yard(c, p, x0, y0, x1, y1, r);
                int[][] q = plan(Math.round((x1 - x0) / TT), Math.round((y1 - y0) / TT), null);
                if (q.length == 0) return;
                // The bunds: a low wall round every tank, the ground inside it darker.
                for (int[] t : q) {
                    float bx0 = x0 + (t[0] - 1.4f) * TT, by0 = y0 + (t[1] - 1.4f) * TT, bx1 = x0 + (t[0] + t[2] + 1.4f) * TT, by1 = y0 + (t[1] + t[3] + 1.4f) * TT;
                    roundRect(c, p, 0x30000000, bx0 + 2, by0 + 2, bx1 + 2, by1 + 2, 5);
                    roundRect(c, p, BUND_EDGE, bx0, by0, bx1, by1, 5);
                    roundRect(c, p, BUND, bx0 + 2.2f, by0 + 2.2f, bx1 - 2.2f, by1 - 2.2f, 4);
                }
                // The manifold along under each row, and a line down to it from every tank; the header up one side.
                float hx = x0 + (q[0][0] - 2.4f) * TT;
                float top = y0 + q[0][1] * TT, bot = top;
                for (int[] t : q) {
                    float my = y0 + (t[1] + t[3] + 2f) * TT, cx = x0 + (t[0] + t[2] / 2f) * TT;
                    bot = Math.max(bot, my);
                    line(c, p, PIPE, 1.6f, cx, y0 + (t[1] + t[3]) * TT, cx, my);
                    circle(c, p, 0xFFC0392B, cx, my - 5, 1.6f);
                }
                int lastY = -1;
                for (int[] t : q) {
                    if (t[1] == lastY) continue;
                    lastY = t[1];
                    float my = y0 + (t[1] + t[3] + 2f) * TT;
                    pipes(c, p, hx, my, x1 - 8, my, 3);
                }
                pipes(c, p, hx, top, hx, bot, 4);
                // Fire hydrants and a foam cannon by the kerb.
                for (int k = 0; k < 4; k++) {
                    float fx = x0 + 8 + r.nextFloat() * (x1 - x0 - 16), fy = k % 2 == 0 ? y0 + 6 : y1 - 6;
                    circle(c, p, 0xFFC0392B, fx, fy, 1.5f);
                }
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                rect(c, p, BUND, x0, y0, x1, y1);
                float tx = (x0 + x1) / 2, ty = (y0 + y1) / 2, rad = (x1 - x0) / 2 - 0.5f;
                circle(c, p, 0x48000000, tx + rad * 0.16f + 1.5f, ty + rad * 0.2f + 1.5f, rad);
                tankTop(c, p, new RectF(), tx, ty, rad, r.nextInt());
            }
        };
        d.ground = City.LOT;
        d.minS = 11;
        d.minL = 11;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_FACTORY;
        d.roofCol = 0xFFE9E8E2;
        out.add(d);
    }

    // ------------------------------------------------------------------ the cracking plant

    /** Where the cracking plant on this parcel has its flare stack (world units), for the view's flame: {x, y}. */
    static List<float[]> flaresOf(float x0, float y0, float x1, float y1) {
        List<float[]> l = new ArrayList<float[]>();
        l.add(new float[]{x1 - 58, y0 + 58});
        if (x1 - x0 > 90 * TT) l.add(new float[]{x0 + 58, y0 + 58});
        return l;
    }

    static void crackingPlant(List<Landmarks.Def> out) {
        // The heart of the refinery: the control building, a compressor house, and out in the open the distillation
        // columns and the cat cracker in a maze of pipe racks, the furnaces with their stacks, cooling towers steaming,
        // and the flare stack burning in its corner.
        Landmarks.Def d = new Landmarks.Def("Cracking Plant", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int cw = Math.max(5, Math.min(10, bw / 5)), ch = Math.max(4, Math.min(7, bh / 5));
                return new int[][]{{2, bh - 2 - ch, cw, ch, 2}, {bw - 2 - cw, bh - 2 - ch, cw, ch, 1}};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                yard(c, p, x0, y0, x1, y1, r);
                float w = x1 - x0;
                int[][] q = plan(Math.round(w / TT), Math.round((y1 - y0) / TT), null);
                float by = y0 + (q[0][1] - 1) * TT;
                rect(c, p, PAD, x0 + 10, y0 + 10, x1 - 10, by - 6);
                frame(c, p, 0xFF9A968A, 1, x0 + 10, y0 + 10, x1 - 10, by - 6);
                // The pipe racks: the main one right across, and one down between each pair of units.
                float ry = y0 + 10 + (by - y0 - 16) * 0.5f;
                int nv = Math.max(2, Math.round(w / (12 * TT)));
                float cw = (w - 40) / nv;
                List<float[]> fl = flaresOf(x0, y0, x1, y1);
                for (int k = 0; k <= nv; k++) pipes(c, p, x0 + 20 + k * cw, y0 + 18, x0 + 20 + k * cw, by - 12, 5, 1.8f);
                pipes(c, p, x0 + 12, ry, x1 - 12, ry, 7, 2.2f);
                // A unit in every bay between the racks, above the main rack and below it.
                for (int k = 0; k < nv; k++)
                    for (int row = 0; row < 2; row++) {
                        float ux0 = x0 + 20 + k * cw + 16, ux1 = x0 + 20 + (k + 1) * cw - 16;
                        float uy0 = row == 0 ? y0 + 20 : ry + 22, uy1 = row == 0 ? ry - 22 : by - 14;
                        if (ux1 - ux0 < 40 || uy1 - uy0 < 40) continue;
                        boolean flare = false;
                        for (float[] f : fl) if (f[0] > ux0 - 20 && f[0] < ux1 + 20 && f[1] > uy0 - 20 && f[1] < uy1 + 20) flare = true;
                        if (flare) continue;
                        unit(c, p, ux0, uy0, ux1, uy1, r, (k * 2 + row * 3 + (int) (x0 / TT)) % 5);
                    }
                // The flare stack(s), in a corner inside a ring of bare, scorched gravel.
                for (float[] f : fl) {
                    circle(c, p, GRAVEL_DARK, f[0], f[1], 46);
                    circle(c, p, 0xFF8A8474, f[0], f[1], 42);
                    circle(c, p, 0x30201810, f[0], f[1], 22);
                    pipes(c, p, f[0], f[1] + 8, f[0], ry, 2, 1.6f);
                    flareStack(c, p, f[0], f[1], 120);
                }
                sign(city, "REFINERY", (x0 + x1) / 2, by + TT * 1.2f, 7, 0xFF3A3A3A);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                flatRoof(c, p, x0, y0, x1, y1, part == 0 ? 0xFFB9B4A8 : 0xFF7E8A94);
                if (part == 0) {
                    for (float x = x0 + 5; x < x1 - 6; x += 9) rect(c, p, 0xFF8A8E94, x, y0 + 4, x + 5, y0 + 8);
                    rect(c, p, 0xFFE0BC38, x0 + 2, y1 - 3.4f, x1 - 2, y1 - 2.2f);
                } else {
                    for (float x = x0 + 4; x < x1 - 3; x += 5) line(c, p, 0x30000000, 0.7f, x, y0 + 2, x, y1 - 2);
                    circle(c, p, 0xFF5E6268, (x0 + x1) / 2, (y0 + y1) / 2, 2.6f);
                }
            }
        };
        d.ground = City.LOT;
        d.minS = 16;
        d.minL = 22;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_FACTORY;
        d.roofCol = 0xFFB9B4A8;
        out.add(d);
    }

    /**
     * One process unit in its bay (x0..y1): 0 distillation columns with their drums, 1 furnaces and their stacks, 2 a
     * bank of heat exchangers and drums, 3 cooling towers, 4 the cat cracker in its frame.
     */
    static void unit(Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r, int kind) {
        float w = x1 - x0, h = y1 - y0, mx = (x0 + x1) / 2, my = (y0 + y1) / 2;
        rect(c, p, 0xFFBAB6AA, x0, y0, x1, y1);
        frame(c, p, 0xFFA29E92, 0.8f, x0, y0, x1, y1);
        switch (kind) {
            case 0: {
                int n = Math.max(2, Math.min(4, (int) (w / 46)));
                for (int k = 0; k < n; k++) {
                    float cx = x0 + w * (k + 0.5f) / n, cy = y0 + 26 + (k % 2) * 18, rad = 11 + (k * 7 % 3) * 3.5f;
                    line(c, p, PIPE, 2.4f, cx, cy + rad, cx, cy + rad + 26);
                    if (cy + rad + 40 < y1 - 8) drum(c, p, cx - 22, cx + 22, cy + rad + 30, 8, 0xFFD6D8DA);
                    column(c, p, cx, cy, rad, 80 + (k * 5 % 3) * 30, k % 2 == 0 ? 0xFFC4C8CC : 0xFFDADCDE);
                }
                break;
            }
            case 1: {
                int n = Math.max(1, Math.min(3, (int) (w / 70)));
                for (int k = 0; k < n; k++) {
                    float fx = x0 + w * (k + 0.5f) / n, fy = my;
                    float fw = Math.min(40, w / n * 0.42f), fh = Math.min(32, h * 0.36f);
                    rect(c, p, 0x40000000, fx - fw + 4, fy - fh + 5, fx + fw + 4, fy + fh + 5);
                    rect(c, p, 0xFF6A6460, fx - fw, fy - fh, fx + fw, fy + fh);
                    rect(c, p, 0xFF8E8882, fx - fw + 3, fy - fh + 3, fx + fw - 3, fy + fh - 3);
                    for (float x = fx - fw + 6; x < fx + fw - 6; x += 7) rect(c, p, 0xFFE0802E, x, fy + fh - 6, x + 3.5f, fy + fh - 4);
                    line(c, p, 0xFF5E5852, 1, fx - fw + 3, fy, fx + fw - 3, fy);
                    column(c, p, fx + fw * 0.45f, fy - fh * 0.3f, 7.5f, 100, 0xFF9A9EA2);
                }
                break;
            }
            case 2: {
                float y = y0 + 18;
                for (int row = 0; y + 10 < y1 - 28 && row < 6; row++, y += 22)
                    for (float x = x0 + 14; x + 52 < x1 - 10; x += 62) drum(c, p, x, x + 50, y, 7.5f, row % 2 == 0 ? 0xFFD6D8DA : 0xFFB9C4CC);
                rect(c, p, PAD, x0 + 8, y1 - 20, x1 - 8, y1 - 8);
                for (float x = x0 + 12; x + 9 < x1 - 10; x += 14) {
                    rect(c, p, 0xFF2E5FB0, x, y1 - 18, x + 8, y1 - 11);
                    circle(c, p, 0xFF8A8E94, x + 9.5f, y1 - 14.5f, 2.2f);
                }
                break;
            }
            case 3: {
                int n = Math.max(1, Math.min(3, (int) (w / 60)));
                float rad = Math.min(34, Math.min(w / n * 0.42f, h * 0.4f));
                for (int k = 0; k < n; k++) {
                    float tx = x0 + w * (k + 0.5f) / n, ty = my;
                    circle(c, p, 0x40000000, tx + 6, ty + 7, rad);
                    circle(c, p, 0xFFA8A49A, tx, ty, rad);
                    circle(c, p, 0xFFCFCBC0, tx, ty, rad * 0.8f);
                    circle(c, p, 0xFF4A4E52, tx, ty, rad * 0.55f);
                    circle(c, p, 0x78FFFFFF, tx + 3, ty - 3, rad * 0.62f);
                    circle(c, p, 0x58FFFFFF, tx + rad * 0.6f, ty - rad * 0.6f, rad * 0.5f);
                    circle(c, p, 0x38FFFFFF, tx + rad * 1.1f, ty - rad * 1.1f, rad * 0.36f);
                }
                break;
            }
            default: {
                float s = Math.min(Math.min(w, h) * 0.4f, 56);
                rect(c, p, 0x40000000, mx - s + 6, my - s + 7, mx + s + 6, my + s + 7);
                rect(c, p, 0xFF7C8086, mx - s, my - s, mx + s, my + s);
                rect(c, p, 0xFF9CA0A6, mx - s + 3, my - s + 3, mx + s - 3, my + s - 3);
                for (float t = -s + 8; t < s - 4; t += 9) {
                    line(c, p, 0xFF6E7278, 0.8f, mx + t, my - s + 3, mx + t, my + s - 3);
                    line(c, p, 0xFF6E7278, 0.8f, mx - s + 3, my + t, mx + s - 3, my + t);
                }
                column(c, p, mx - s * 0.3f, my - s * 0.2f, s * 0.36f, 110, 0xFFD0D3D6);
                column(c, p, mx + s * 0.45f, my + s * 0.35f, s * 0.24f, 80, 0xFFB9BDC2);
                line(c, p, 0xFFB8BCC0, 4, mx - s * 0.3f, my - s * 0.2f, mx + s * 0.45f, my + s * 0.35f);
                break;
            }
        }
    }

    // ------------------------------------------------------------------ the loading racks

    static void loadingRacks(List<Landmarks.Def> out) {
        // The rail siding: strings of tank cars under the loading gantries, the road tankers' bays beside them, a few
        // tanks of product, the dispatch office.
        Landmarks.Def d = new Landmarks.Def("Loading Racks", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int ow = Math.max(5, Math.min(9, bw / 6)), oh = Math.max(4, Math.min(6, bh / 6));
                return new int[][]{{2, bh - 2 - oh, ow, oh, 1}};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                yard(c, p, x0, y0, x1, y1, r);
                float w = x1 - x0;
                int[][] q = plan(Math.round(w / TT), Math.round((y1 - y0) / TT), null);
                float lim = y0 + (q[0][1] - 1) * TT;
                int[] cars = {0xFF2E3034, 0xFF3A3C40, 0xFFE8E6E0, 0xFF8A2A24, 0xFF2E3034, 0xFFC8CCD0};
                // The sidings across the top: ballast, sleepers, rails, and strings of tank cars standing on them.
                float pitch = 30;
                int tracks = Math.max(2, Math.min(7, (int) ((lim - y0) * 0.5f / pitch)));
                for (int k = 0; k < tracks; k++) {
                    float ty = y0 + 26 + k * pitch;
                    rect(c, p, 0xFF8E8676, x0 + 6, ty - 9, x1 - 6, ty + 9);
                    for (float x = x0 + 8; x < x1 - 8; x += 7) rect(c, p, 0xFF5E5448, x, ty - 7, x + 2.4f, ty + 7);
                    line(c, p, 0xFFB8BCC0, 1.2f, x0 + 6, ty - 4.5f, x1 - 6, ty - 4.5f);
                    line(c, p, 0xFFB8BCC0, 1.2f, x0 + 6, ty + 4.5f, x1 - 6, ty + 4.5f);
                    float x = x0 + 14 + r.nextFloat() * 50;
                    while (x + 50 < x1 - 12) {
                        if (r.nextInt(7) == 0) {
                            x += 80;
                            continue;
                        }
                        int col = cars[r.nextInt(cars.length)];
                        roundRect(c, p, 0x40000000, x + 3, ty - 6 + 3.5f, x + 48 + 3, ty + 6 + 3.5f, 6);
                        rect(c, p, 0xFF2A2C30, x + 2, ty - 5.5f, x + 46, ty + 5.5f);
                        roundRect(c, p, City.darken(col, 0.7f), x, ty - 6.5f, x + 48, ty + 6.5f, 6.5f);
                        roundRect(c, p, col, x + 1.5f, ty - 5, x + 46.5f, ty + 2.5f, 5);
                        roundRect(c, p, City.lighten(col, 0.22f), x + 4, ty - 4, x + 44, ty - 1.5f, 2);
                        rect(c, p, 0xFF8A8E94, x + 21.5f, ty - 3, x + 26.5f, ty + 3);
                        x += 52;
                    }
                }
                // The gantry between the first two tracks: a walkway with a loading arm down to every car.
                float gy = y0 + 26 + pitch / 2;
                rect(c, p, 0x38000000, x0 + 20, gy - 2 + 4, x1 - 20, gy + 2 + 4);
                rect(c, p, 0xFFE0BC38, x0 + 20, gy - 2.6f, x1 - 20, gy + 2.6f);
                line(c, p, 0xFF8A7A20, 0.7f, x0 + 20, gy, x1 - 20, gy);
                for (float x = x0 + 30; x < x1 - 26; x += 26) {
                    line(c, p, STEEL, 1.8f, x, gy, x, gy - pitch / 2 + 5);
                    line(c, p, STEEL, 1.8f, x + 8, gy, x + 8, gy + pitch / 2 - 5);
                    rect(c, p, STEEL, x - 2, gy - 4, x + 10, gy + 4);
                }
                // Below: the header, product tanks at one end, and the road tankers' bays, a canopy over each pair.
                float ry = y0 + 26 + tracks * pitch + 4;
                pipes(c, p, x0 + 12, ry, x1 - 12, ry, 4, 1.8f);
                float by0 = ry + 22;
                if (by0 + 84 < lim) {
                    int bays = Math.max(2, Math.min(14, (int) ((w - 60) / 62)));
                    float span = (w - 60) / bays;
                    for (int k = 0; k < bays; k++) {
                        float bx = x0 + 30 + k * span + 6, bw = span - 12;
                        rect(c, p, 0xFF8C8A84, bx, by0, bx + bw, by0 + 78);
                        line(c, p, 0xFFE8E4D8, 1, bx + bw / 2, by0 + 2, bx + bw / 2, by0 + 76);
                        for (int lane = 0; lane < 2; lane++) {
                            if (r.nextInt(3) == 0) continue;
                            float tx = bx + bw * (lane == 0 ? 0.25f : 0.75f) - 6, ty = by0 + 8 + r.nextFloat() * 8;
                            rect(c, p, 0x40000000, tx + 2.5f, ty + 3, tx + 12 + 2.5f, ty + 52 + 3);
                            roundRect(c, p, 0xFFC8CCD0, tx, ty, tx + 12, ty + 38, 5);
                            roundRect(c, p, 0xFFF2F3F4, tx + 1.6f, ty + 1.5f, tx + 8.5f, ty + 36.5f, 3.5f);
                            rect(c, p, r.nextBoolean() ? 0xFFC0392B : 0xFF2E5FB0, tx + 0.5f, ty + 40, tx + 11.5f, ty + 52);
                            rect(c, p, 0xFF2A3A4A, tx + 1.5f, ty + 47, tx + 10.5f, ty + 50);
                        }
                        rect(c, p, 0x38000000, bx - 2 + 4, by0 + 22 + 5, bx + bw + 2 + 4, by0 + 44 + 5);
                        rect(c, p, 0xE6E4E6E8, bx - 2, by0 + 22, bx + bw + 2, by0 + 44);
                        frame(c, p, 0xFF9A9EA2, 1, bx - 2, by0 + 22, bx + bw + 2, by0 + 44);
                        line(c, p, 0xFFC0392B, 2, bx - 2, by0 + 43, bx + bw + 2, by0 + 43);
                    }
                    pipes(c, p, x0 + 20, by0 + 33, x1 - 20, by0 + 33, 2, 1.4f);
                }
                // Stacks of drums and a few lying tanks of product in what's left of the yard.
                float py = by0 + 100;
                for (float x = x0 + (q[0][0] + q[0][2] + 2) * TT; x + 70 < x1 - 14 && py + 26 < y1 - 10; x += 84)
                    for (int k = 0; k < 2 && py + k * 20 + 8 < y1 - 10; k++) drum(c, p, x, x + 62, py + k * 20, 8, k == 0 ? 0xFFE8E6E0 : 0xFFD0D6D0);
                sign(city, "LOADING", x1 - 70, lim - TT, 6, 0xFF3A3A3A);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                flatRoof(c, p, x0, y0, x1, y1, 0xFF9AA4AC);
                rect(c, p, 0xFF7A848C, x0 + 4, y0 + 4, x0 + 9, y0 + 8);
            }
        };
        d.ground = City.LOT;
        d.minS = 14;
        d.minL = 18;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_FACTORY;
        d.roofCol = 0xFF9AA4AC;
        out.add(d);
    }

    // ------------------------------------------------------------------ the depot

    static void depot(List<Landmarks.Def> out) {
        // Warehouses and workshops: long sheds with ridged roofs, drums and pipe stacked in the yard between them.
        Landmarks.Def d = new Landmarks.Def("Refinery Depot", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int ow = Math.max(5, Math.min(10, bw / 4)), oh = Math.max(4, Math.min(6, bh / 4));
                int n = Math.max(1, Math.min(5, (bw - 3) / 14)), sw = (bw - 4 - (n - 1) * 3) / n;
                int sh = Math.max(5, Math.min(Math.min(bh * 3 / 5, 22), bh - oh - 5));
                int[][] q = new int[n + 1][];
                for (int k = 0; k < n; k++) q[k] = new int[]{2 + k * (sw + 3), 2, sw, sh, 1};
                q[n] = new int[]{bw - 2 - ow, bh - 2 - oh, ow, oh, 2};
                return q;
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                yard(c, p, x0, y0, x1, y1, r);
                int[][] q = plan(Math.round((x1 - x0) / TT), Math.round((y1 - y0) / TT), null);
                float yd = y0 + (q[0][1] + q[0][3] + 1.5f) * TT, lim = x0 + (q[q.length - 1][0] - 1) * TT;
                // Drums on pallets, lengths of pipe, a skip or two.
                int[] drums = {0xFF2E5FB0, 0xFFC0392B, 0xFF3A3C40, 0xFFE0BC38};
                for (float x = x0 + 10; x + 12 < lim; x += 16 + r.nextFloat() * 10) {
                    float y = yd + r.nextFloat() * Math.max(1, y1 - yd - 22);
                    int kind = r.nextInt(3);
                    if (kind == 0) {
                        rect(c, p, 0xFF8A7250, x - 0.5f, y - 0.5f, x + 9.5f, y + 9.5f);
                        int col = drums[r.nextInt(drums.length)];
                        for (int k = 0; k < 9; k++) circle(c, p, k % 4 == 0 ? City.lighten(col, 0.2f) : col, x + 1.5f + (k % 3) * 3, y + 1.5f + (k / 3) * 3, 1.3f);
                    } else if (kind == 1) {
                        for (int k = 0; k < 5; k++) line(c, p, k % 2 == 0 ? PIPE_LIGHT : PIPE, 1.3f, x, y + k * 1.5f, x + 13, y + k * 1.5f);
                    } else {
                        rect(c, p, 0x40000000, x + 1.5f, y + 1.5f, x + 11.5f, y + 7.5f);
                        rect(c, p, 0xFF3C7A4E, x, y, x + 10, y + 6);
                        rect(c, p, 0xFF2A5636, x + 1, y + 1, x + 9, y + 5);
                    }
                }
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                // (The office is the last and the smallest of them.)
                boolean office = part > 0 && (y1 - y0) <= 6.5f * TT && (x1 - x0) <= 10.5f * TT;
                if (office) {
                    flatRoof(c, p, x0, y0, x1, y1, 0xFFB9B4A8);
                    rect(c, p, 0xFF8A8E94, x0 + 4, y0 + 4, x0 + 10, y0 + 8);
                    return;
                }
                int col = part % 2 == 0 ? 0xFF8E99A2 : 0xFFA09A8C;
                rect(c, p, City.darken(col, 0.7f), x0, y0, x1, y1);
                float mx = (x0 + x1) / 2;
                rect(c, p, col, x0 + 1, y0 + 1, mx, y1 - 1);
                rect(c, p, City.darken(col, 0.86f), mx, y0 + 1, x1 - 1, y1 - 1);
                for (float y = y0 + 4; y < y1 - 2; y += 4) line(c, p, 0x22000000, 0.5f, x0 + 1, y, x1 - 1, y);
                line(c, p, City.lighten(col, 0.2f), 1, mx, y0 + 1, mx, y1 - 1);
                for (float y = y0 + 8; y < y1 - 8; y += 16) rect(c, p, 0xB0C8DCE8, x0 + 4, y, mx - 3, y + 3);
            }
        };
        d.ground = City.LOT;
        d.minS = 14;
        d.minL = 18;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_FACTORY;
        d.roofCol = 0xFF8E99A2;
        out.add(d);
    }

    // ------------------------------------------------------------------ the tanker terminal

    static void terminal(List<Landmarks.Def> out) {
        // On the waterfront: the pump house and the terminal office, a row of tanks' worth of manifold, and the big
        // lines running down the middle of it to the culvert under the seafront road and out along the jetty.
        Landmarks.Def d = new Landmarks.Def("Tanker Terminal", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int pw = Math.max(5, Math.min(9, bw / 6)), ph = Math.max(4, Math.min(7, bh / 5));
                // (Either side of the trunk lines; on a narrow parcel, as far apart as it allows.)
                return new int[][]{{Math.max(1, bw / 2 - 5 - pw), bh - 2 - ph, pw, ph, 1}, {Math.min(bw - 1 - pw, bw / 2 + 5), bh - 2 - ph, pw, ph, 2}};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                yard(c, p, x0, y0, x1, y1, r);
                float w = x1 - x0, mx = (x0 + x1) / 2;
                int[][] q = plan(Math.round(w / TT), Math.round((y1 - y0) / TT), null);
                float by = y0 + (q[0][1] - 1) * TT;
                // Spheres of gas either side of the trunk lines, a line from each to the header below them.
                float R = 30, pitch = R * 3.3f;
                int rows = Math.max(1, Math.min(3, (int) ((by - y0 - 70) / pitch)));
                int n = Math.max(1, Math.min(5, (int) ((w / 2 - 50) / pitch)));
                float hy = y0 + 24 + rows * pitch;
                for (int row = 0; row < rows; row++)
                    for (int side = -1; side <= 1; side += 2)
                        for (int k = 0; k < n; k++) {
                            float sx = mx + side * (62 + k * pitch), sy = y0 + 20 + R * 1.5f + row * pitch;
                            line(c, p, PIPE, 2.4f, sx + R * 1.2f * side * -1, sy, sx + R * 1.6f * side * -1, sy);
                            sphere(c, p, sx, sy, R);
                        }
                pipes(c, p, mx - (62 + (n - 1) * pitch) - 20, hy, mx + 62 + (n - 1) * pitch + 20, hy, 3, 1.8f);
                // The pump slab across the trunk lines; then the lines themselves, top to bottom, over everything.
                float py = hy + 22;
                if (py + 22 < by - 4) {
                    rect(c, p, PAD, mx - 110, py, mx + 110, py + 22);
                    frame(c, p, 0xFF9A968A, 0.8f, mx - 110, py, mx + 110, py + 22);
                    for (float x = mx - 102; x + 16 < mx + 106; x += 22) {
                        if (Math.abs(x + 7 - mx) < 26) continue;
                        rect(c, p, 0x40000000, x + 1.5f, py + 6.5f, x + 15.5f, py + 17.5f);
                        rect(c, p, 0xFF2E5FB0, x, py + 5, x + 14, py + 16);
                        rect(c, p, 0xFF4C7CC8, x + 1.5f, py + 6.5f, x + 9, py + 11);
                        circle(c, p, 0xFF8A8E94, x + 17, py + 10.5f, 3.2f);
                    }
                }
                pipes(c, p, mx, y0, mx, y1, 7, 2.6f);
                sign(city, "TERMINAL", mx + w * 0.3f, y1 - TT * 0.8f, 6, 0xFF3A3A3A);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                flatRoof(c, p, x0, y0, x1, y1, part == 0 ? 0xFF7E8A94 : 0xFFB9B4A8);
                if (part == 0) for (float x = x0 + 4; x < x1 - 3; x += 5) line(c, p, 0x30000000, 0.7f, x, y0 + 2, x, y1 - 2);
                else rect(c, p, 0xFF8A8E94, x0 + 4, y0 + 4, x0 + 10, y0 + 8);
            }
        };
        d.ground = City.LOT;
        d.minS = 14;
        d.minL = 20;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_FACTORY;
        d.roofCol = 0xFF7E8A94;
        out.add(d);
    }

    // ------------------------------------------------------------------ the pipes out to the jetty

    static void culvert(List<Landmarks.Def> out) {
        // Painted over the ground as it is (no ground of its own): the trunk lines from the terminal, in a grated
        // trench across the seafront road and the quay, then on their trestle down the middle of the jetty to the
        // loading arms at its end.
        Landmarks.Def d = new Landmarks.Def("Pipe Culvert", T_PARK) {
            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                float mx = (x0 + x1) / 2;
                rect(c, p, 0xFF4A4C50, mx - 13, y0, mx + 13, y1 - 40);
                pipes(c, p, mx, y0, mx, y1 - 30, 5, 2.4f);
                for (float y = y0 + 4; y < y1 - 44; y += 5) line(c, p, 0x48202224, 0.7f, mx - 13, y, mx + 13, y);
                frame(c, p, 0xFFE0BC38, 1.2f, mx - 13, y0, mx + 13, y1 - 40);
                // The manifold at the end of the jetty, and the loading arms reaching over to the tanker.
                rect(c, p, 0xFF5E6268, mx - 16, y1 - 44, mx + 16, y1 - 14);
                rect(c, p, 0xFF8A8E94, mx - 13, y1 - 41, mx + 13, y1 - 17);
                for (int k = 0; k < 4; k++) {
                    float ay = y1 - 90 - k * 46;
                    if (ay < y0 + 150) break;
                    rect(c, p, 0xFF5E6268, mx + 10, ay - 6, mx + 22, ay + 6);
                    line(c, p, 0xFFE0802E, 3, mx + 16, ay, mx + 44, ay - 9);
                    line(c, p, 0xFFE0802E, 3, mx + 44, ay - 9, mx + 66, ay);
                    circle(c, p, 0xFF3A3C40, mx + 44, ay - 9, 3);
                }
                // Bollards down both sides of it.
                for (float y = y0 + 160; y < y1 - 10; y += 48) {
                    circle(c, p, 0xFF3A3C40, mx - 20, y, 2.4f);
                    circle(c, p, 0xFF3A3C40, mx + 20, y, 2.4f);
                }
            }
        };
        d.ground = City.LOT;
        out.add(d);
    }

    // ------------------------------------------------------------------ the tanker

    static void tanker(List<Landmarks.Def> out) {
        // The tanker moored at the jetty: one building, all of its parcel (which lies in the water). Its bow to the
        // top; a red deck crossed by pipes and catwalk, the manifold amidships, the white bridge and the funnel aft.
        Landmarks.Def d = new Landmarks.Def("Oil Tanker", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return new int[][]{{0, 0, bw, bh, 1}};
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                hull(c, p, x0, y0, x1, y1, false);
            }
        };
        d.ground = City.LOT;
        d.minS = 4;
        d.minL = 10;
        d.base = City.WAREHOUSE;
        d.inside = Variants.I_FACTORY;
        d.roofCol = 0xFF8A2A24;
        out.add(d);
    }

    private static Path hullPath(float x0, float y0, float x1, float y1, float in) {
        float w = x1 - x0, mx = (x0 + x1) / 2, half = w / 2 - in, bow = w * 0.95f, st = w * 0.3f;
        Path h = new Path();
        h.moveTo(mx, y0 + in);
        // (The bow: a pointed curve out to the full beam; the stern: rounded. Straight lines only: a few to each.)
        for (int k = 1; k <= 6; k++) {
            float t = k / 6f;
            h.lineTo(mx + half * (float) Math.sin(t * Math.PI / 2), y0 + in + (bow - in) * t * t);
        }
        h.lineTo(mx + half, y1 - in - st);
        for (int k = 1; k <= 6; k++) {
            double a = k * Math.PI / 12;
            h.lineTo(mx + half * (float) Math.cos(a), y1 - in - st + st * (float) Math.sin(a));
        }
        for (int k = 5; k >= 0; k--) {
            double a = k * Math.PI / 12;
            h.lineTo(mx - half * (float) Math.cos(a), y1 - in - st + st * (float) Math.sin(a));
        }
        for (int k = 6; k >= 1; k--) {
            float t = k / 6f;
            h.lineTo(mx - half * (float) Math.sin(t * Math.PI / 2), y0 + in + (bow - in) * t * t);
        }
        h.close();
        return h;
    }

    static void hull(Canvas c, Paint p, float x0, float y0, float x1, float y1, boolean burnt) {
        float w = x1 - x0, mx = (x0 + x1) / 2;
        rect(c, p, SEA, x0, y0, x1, y1);
        p.setStyle(Paint.Style.FILL);
        p.setColor(burnt ? 0xFF1C1A18 : 0xFF2A2C30);
        c.drawPath(hullPath(x0, y0, x1, y1, 0.5f), p);
        p.setColor(burnt ? 0xFF2E2622 : 0xFF9A3A2E);
        c.drawPath(hullPath(x0, y0, x1, y1, 3.5f), p);
        if (burnt) return;
        float top = y0 + w * 1.05f, aft = y1 - w * 1.1f;
        // The deck: tank hatches both sides, the pipes and catwalk down the middle, the manifold across amidships.
        for (float y = top; y + 12 < aft; y += 18)
            for (int s = -1; s <= 1; s += 2) {
                float hx = mx + s * w * 0.24f;
                rect(c, p, 0xFF85302A, hx - w * 0.14f, y, hx + w * 0.14f, y + 13);
                circle(c, p, 0xFFB8BCC0, hx, y + 6.5f, 2.2f);
                circle(c, p, 0xFF5E6268, hx, y + 6.5f, 1.1f);
            }
        pipes(c, p, mx, top - 6, mx, aft, 3);
        line(c, p, 0xFFD8DADC, 1.2f, mx - 4.5f, top - 6, mx - 4.5f, aft);
        float my = (top + aft) / 2;
        rect(c, p, 0xFF5E6268, x0 + 5, my - 3, x1 - 5, my + 3);
        for (float x = x0 + 8; x < x1 - 8; x += 5) circle(c, p, 0xFFE0802E, x, my, 1.3f);
        // The fo'c'sle: winches and the anchor chains.
        circle(c, p, 0xFF5E6268, mx - 4, y0 + w * 0.7f, 2);
        circle(c, p, 0xFF5E6268, mx + 4, y0 + w * 0.7f, 2);
        line(c, p, 0xFF3A3C40, 0.8f, mx, y0 + w * 0.3f, mx, y0 + w * 0.7f);
        // Aft: the bridge right across, its wings, the funnel, the lifeboats.
        rect(c, p, 0x40000000, x0 + 5, aft + 3, x1 - 1, aft + w * 0.6f + 3);
        rect(c, p, 0xFFDADCDE, x0 + 4, aft, x1 - 4, aft + w * 0.6f);
        rect(c, p, 0xFFF4F5F6, x0 + 6, aft + 2, x1 - 6, aft + w * 0.6f - 2);
        rect(c, p, 0xFF2A3A4A, x0 + 5, aft + 0.8f, x1 - 5, aft + 2.6f);
        rect(c, p, 0xFF2A2C30, mx - 4, aft + w * 0.66f, mx + 4, aft + w * 0.66f + 8);
        rect(c, p, 0xFFE0802E, mx - 4, aft + w * 0.66f, mx + 4, aft + w * 0.66f + 2.5f);
        circle(c, p, 0xFF101010, mx, aft + w * 0.66f + 5, 2);
        roundRect(c, p, 0xFFE0802E, x0 + 5, aft + w * 0.66f, x0 + 9, aft + w * 0.66f + 9, 2);
        roundRect(c, p, 0xFFE0802E, x1 - 9, aft + w * 0.66f, x1 - 5, aft + w * 0.66f + 9, 2);
    }

    // ------------------------------------------------------------------ what's left of one that's gone up

    /** A tank or the tanker burnt out, over its footprint: the shell torn open and blackened, soot all round it. */
    static void burnt(Canvas c, Paint p, City.Building b) {
        Random r = new Random(b.seed * 31L + 7);
        if (b.tank == 2) {
            hull(c, p, b.x0, b.y0, b.x1, b.y1, true);
            // Decks burnt through to the tanks, her back broken; oil on the water round her.
            float mx = (b.x0 + b.x1) / 2, w = b.x1 - b.x0;
            for (int k = 0; k < 14; k++) {
                float x = b.x0 + 5 + r.nextFloat() * (w - 10), y = b.y0 + w + r.nextFloat() * (b.y1 - b.y0 - w * 1.4f), s = 2 + r.nextFloat() * 5;
                oval(c, p, k % 3 == 0 ? 0xFF4A3A30 : 0xFF0E0D0C, x - s, y - s * 0.7f, x + s, y + s * 0.7f);
            }
            line(c, p, 0xFF0A0A0A, 2.4f, b.x0 + 3, (b.y0 + b.y1) / 2 - 4, b.x1 - 3, (b.y0 + b.y1) / 2 + 5);
            for (int k = 0; k < 6; k++) {
                float y = b.y0 + r.nextFloat() * (b.y1 - b.y0), s = 4 + r.nextFloat() * 6;
                oval(c, p, 0x50101014, (k % 2 == 0 ? b.x0 : b.x1) - s, y - s * 0.5f, (k % 2 == 0 ? b.x0 : b.x1) + s, y + s * 0.5f);
            }
            rect(c, p, 0xFF3A3632, mx - 6, b.y1 - w * 1.05f, mx + 6, b.y1 - w * 0.7f);
            return;
        }
        float cx = (b.x0 + b.x1) / 2, cy = (b.y0 + b.y1) / 2, rad = (b.x1 - b.x0) / 2 - 0.5f;
        rect(c, p, 0xFF5A554C, b.x0, b.y0, b.x1, b.y1);
        circle(c, p, 0xFF2A2724, cx, cy, rad + 1.5f);
        circle(c, p, 0xFF141312, cx, cy, rad - 1.5f);
        circle(c, p, 0xFF221E1A, cx + rad * 0.15f, cy + rad * 0.1f, rad * 0.6f);
        // The shell: what stands of it, a broken ring, the plates peeled outward.
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2.2f);
        p.setColor(0xFF4A4038);
        RF.set(cx - rad, cy - rad, cx + rad, cy + rad);
        float a = r.nextFloat() * 360;
        for (int k = 0; k < 4; k++) {
            float sweep = 40 + r.nextFloat() * 45;
            c.drawArc(RF, a, sweep, false, p);
            a += sweep + 12 + r.nextFloat() * 30;
        }
        p.setStyle(Paint.Style.FILL);
        for (int k = 0; k < 7; k++) {
            double an = r.nextFloat() * 6.28;
            float d0 = rad * (0.75f + r.nextFloat() * 0.2f), d1 = rad * (0.95f + r.nextFloat() * 0.35f);
            line(c, p, k % 2 == 0 ? 0xFF5A4A3C : 0xFF36302A, 1.6f, cx + (float) Math.cos(an) * d0, cy + (float) Math.sin(an) * d0,
                    cx + (float) Math.cos(an) * d1, cy + (float) Math.sin(an) * d1);
        }
        // The roof, fallen in, in pieces on the floor of it.
        for (int k = 0; k < 6; k++) {
            float x = cx + (r.nextFloat() - 0.5f) * rad * 1.1f, y = cy + (r.nextFloat() - 0.5f) * rad * 1.1f, s = 2 + r.nextFloat() * rad * 0.22f;
            rect(c, p, k % 2 == 0 ? 0xFF3E3832 : 0xFF2C2824, x - s, y - s * 0.6f, x + s, y + s * 0.6f);
        }
        circle(c, p, 0x38000000, cx, cy, rad * 0.35f);
    }
}
