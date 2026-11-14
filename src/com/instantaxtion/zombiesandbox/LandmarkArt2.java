package com.instantaxtion.zombiesandbox;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.Landmarks.*;

/** ((22.4) Landmark designs, part 2 (see Landmarks): parks and green spaces. */
final class LandmarkArt2 {
    private LandmarkArt2() {
    }

    private static final float T = City.T;

    // ------------------------------------------------------------------ shared helpers

    /**
     * The block seen along its long side: u runs 0..1 along the long side, v 0..1 across the short one, so a design
     * laid out in u, v fits a wide block and a tall one (it is mirrored over the diagonal for a tall one).
     */
    private static final class Fr {
        final float x0, y0, w, h, L, S;
        final boolean wide;

        Fr(float x0, float y0, float x1, float y1) {
            this.x0 = x0;
            this.y0 = y0;
            w = x1 - x0;
            h = y1 - y0;
            wide = w >= h;
            L = wide ? w : h;
            S = wide ? h : w;
        }

        float x(float u, float v) {
            return wide ? x0 + u * w : x0 + v * w;
        }

        float y(float u, float v) {
            return wide ? y0 + v * h : y0 + u * h;
        }

        /** World extents of a size given along the long (lu) and short (lv) sides. */
        float ex(float lu, float lv) {
            return wide ? lu : lv;
        }

        float ey(float lu, float lv) {
            return wide ? lv : lu;
        }

        float[] pts(float... uv) {
            float[] o = new float[uv.length];
            for (int k = 0; k + 1 < uv.length; k += 2) {
                o[k] = x(uv[k], uv[k + 1]);
                o[k + 1] = y(uv[k], uv[k + 1]);
            }
            return o;
        }

        void box(Canvas c, Paint p, int col, float u0, float v0, float u1, float v1) {
            rect(c, p, col, x(u0, v0), y(u0, v0), x(u1, v1), y(u1, v1));
        }
    }

    /** A building given along the long (u) and short (v) sides, turned for a tall block. */
    private static int[] tb(int bw, int bh, int u, int v, int lu, int lv, int f) {
        return bw >= bh ? new int[]{u, v, lu, lv, f} : new int[]{v, u, lv, lu, f};
    }

    /** The world rectangle {x0, y0, x1, y1} of building i of a plan. */
    private static float[] part(int[][] pl, int i, float x0, float y0) {
        int[] q = pl[i];
        return new float[]{x0 + q[0] * T, y0 + q[1] * T, x0 + (q[0] + q[2]) * T, y0 + (q[1] + q[3]) * T};
    }

    private static boolean inside(float[] b, float m, float x, float y) {
        return x > b[0] - m && x < b[2] + m && y > b[1] - m && y < b[3] + m;
    }

    private static float[] jit(Random r, int n, float j) {
        float[] a = new float[n];
        for (int i = 0; i < n; i++) a[i] = 1 - j + 2 * j * r.nextFloat();
        return a;
    }

    /** A smooth closed blob round (cx, cy): radii rx, ry scaled per spoke by k and grown by grow units. */
    private static Path blobPath(float cx, float cy, float rx, float ry, float[] k, float grow) {
        int n = k.length;
        float[] px = new float[n], py = new float[n];
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n;
            px[i] = cx + (float) Math.cos(a) * (rx * k[i] + grow);
            py[i] = cy + (float) Math.sin(a) * (ry * k[i] + grow);
        }
        Path path = new Path();
        path.moveTo((px[n - 1] + px[0]) / 2, (py[n - 1] + py[0]) / 2);
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            path.quadTo(px[i], py[i], (px[i] + px[j]) / 2, (py[i] + py[j]) / 2);
        }
        path.close();
        return path;
    }

    private static void blob(Canvas c, Paint p, int col, float cx, float cy, float rx, float ry, float[] k, float grow) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(col);
        c.drawPath(blobPath(cx, cy, rx, ry, k, grow), p);
    }

    private static void blobLine(Canvas c, Paint p, int col, float w, float cx, float cy, float rx, float ry, float[] k, float grow) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(w);
        p.setColor(col);
        c.drawPath(blobPath(cx, cy, rx, ry, k, grow), p);
        p.setStyle(Paint.Style.FILL);
    }

    /** A smooth open line through the points {x0, y0, x1, y1, ...} with round ends. */
    private static void trail(Canvas c, Paint p, int col, float w, float[] pts) {
        int n = pts.length / 2;
        Path path = new Path();
        path.moveTo(pts[0], pts[1]);
        if (n == 2) path.lineTo(pts[2], pts[3]);
        for (int i = 1; i < n - 1; i++) {
            float mx = (pts[2 * i] + pts[2 * i + 2]) / 2, my = (pts[2 * i + 1] + pts[2 * i + 3]) / 2;
            if (i == n - 2) path.quadTo(pts[2 * i], pts[2 * i + 1], pts[2 * i + 2], pts[2 * i + 3]);
            else path.quadTo(pts[2 * i], pts[2 * i + 1], mx, my);
        }
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(w);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(col);
        c.drawPath(path, p);
        p.setStrokeCap(Paint.Cap.BUTT);
        p.setStyle(Paint.Style.FILL);
    }

    /** Distance from (x, y) to the polyline through pts. */
    private static float dist(float[] pts, float x, float y) {
        float best = 1e9f;
        for (int i = 0; i + 3 < pts.length; i += 2) {
            float ax = pts[i], ay = pts[i + 1], bx = pts[i + 2], by = pts[i + 3];
            float dx = bx - ax, dy = by - ay, l2 = dx * dx + dy * dy;
            float t = l2 == 0 ? 0 : Math.max(0, Math.min(1, ((x - ax) * dx + (y - ay) * dy) / l2));
            float ex = ax + t * dx - x, ey = ay + t * dy - y;
            best = Math.min(best, (float) Math.sqrt(ex * ex + ey * ey));
        }
        return best;
    }

    private static void ring(Canvas c, Paint p, int col, float w, float x, float y, float rad) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(w);
        p.setColor(col);
        c.drawCircle(x, y, rad, p);
        p.setStyle(Paint.Style.FILL);
    }

    private static void curve(Canvas c, Paint p, int col, float w, float ax, float ay, float bx, float by, float ex, float ey) {
        Path path = new Path();
        path.moveTo(ax, ay);
        path.quadTo(bx, by, ex, ey);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(w);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(col);
        c.drawPath(path, p);
        p.setStrokeCap(Paint.Cap.BUTT);
        p.setStyle(Paint.Style.FILL);
    }

    /** A pitched roof with its ridge along the long side, tile courses and a lit and a shaded slope. */
    private static void gable(Canvas c, Paint p, float x0, float y0, float x1, float y1, int col) {
        rect(c, p, City.darken(col, 0.6f), x0, y0, x1, y1);
        if (x1 - x0 >= y1 - y0) {
            float my = (y0 + y1) / 2;
            rect(c, p, City.lighten(col, 0.1f), x0 + 0.8f, y0 + 0.8f, x1 - 0.8f, my);
            rect(c, p, City.darken(col, 0.8f), x0 + 0.8f, my, x1 - 0.8f, y1 - 0.8f);
            for (float y = y0 + 2.8f; y < my - 0.8f; y += 2.6f) line(c, p, 0x26000000, 0.5f, x0 + 0.8f, y, x1 - 0.8f, y);
            for (float y = my + 2.6f; y < y1 - 1; y += 2.6f) line(c, p, 0x26000000, 0.5f, x0 + 0.8f, y, x1 - 0.8f, y);
            line(c, p, City.lighten(col, 0.35f), 1.2f, x0 + 0.8f, my, x1 - 0.8f, my);
        } else {
            float mx = (x0 + x1) / 2;
            rect(c, p, City.lighten(col, 0.1f), x0 + 0.8f, y0 + 0.8f, mx, y1 - 0.8f);
            rect(c, p, City.darken(col, 0.8f), mx, y0 + 0.8f, x1 - 0.8f, y1 - 0.8f);
            for (float x = x0 + 2.8f; x < mx - 0.8f; x += 2.6f) line(c, p, 0x26000000, 0.5f, x, y0 + 0.8f, x, y1 - 0.8f);
            for (float x = mx + 2.6f; x < x1 - 1; x += 2.6f) line(c, p, 0x26000000, 0.5f, x, y0 + 0.8f, x, y1 - 0.8f);
            line(c, p, City.lighten(col, 0.35f), 1.2f, mx, y0 + 0.8f, mx, y1 - 0.8f);
        }
    }

    /** A hipped roof: four faces lit from the top left and a ridge. */
    private static void hip(Canvas c, Paint p, float x0, float y0, float x1, float y1, int col) {
        rect(c, p, City.darken(col, 0.6f), x0, y0, x1, y1);
        float mx = (x0 + x1) / 2, my = (y0 + y1) / 2, e = 0.8f;
        int lt = City.lighten(col, 0.14f), mid = col, dk = City.darken(col, 0.8f), dd = City.darken(col, 0.68f), ridge = City.lighten(col, 0.35f);
        if (x1 - x0 >= y1 - y0) {
            float in = (y1 - y0) / 2;
            poly(c, p, lt, x0 + e, y0 + e, x1 - e, y0 + e, x1 - in, my, x0 + in, my);
            poly(c, p, dd, x0 + e, y1 - e, x1 - e, y1 - e, x1 - in, my, x0 + in, my);
            poly(c, p, mid, x0 + e, y0 + e, x0 + in, my, x0 + e, y1 - e);
            poly(c, p, dk, x1 - e, y0 + e, x1 - in, my, x1 - e, y1 - e);
            line(c, p, ridge, 1.1f, x0 + in, my, x1 - in, my);
            line(c, p, City.lighten(col, 0.2f), 0.6f, x0 + e, y0 + e, x0 + in, my);
            line(c, p, City.lighten(col, 0.2f), 0.6f, x1 - e, y0 + e, x1 - in, my);
        } else {
            float in = (x1 - x0) / 2;
            poly(c, p, lt, x0 + e, y0 + e, x1 - e, y0 + e, mx, y0 + in);
            poly(c, p, dd, x0 + e, y1 - e, x1 - e, y1 - e, mx, y1 - in);
            poly(c, p, mid, x0 + e, y0 + e, mx, y0 + in, mx, y1 - in, x0 + e, y1 - e);
            poly(c, p, dk, x1 - e, y0 + e, mx, y0 + in, mx, y1 - in, x1 - e, y1 - e);
            line(c, p, ridge, 1.1f, mx, y0 + in, mx, y1 - in);
            line(c, p, City.lighten(col, 0.2f), 0.6f, x0 + e, y0 + e, mx, y0 + in);
            line(c, p, City.lighten(col, 0.2f), 0.6f, x1 - e, y0 + e, mx, y0 + in);
        }
    }

    /** A fence round a rectangle: rails and posts. */
    private static void fence(Canvas c, Paint p, int col, float x0, float y0, float x1, float y1, float step) {
        frame(c, p, 0x30000000, 0.9f, x0 + 0.6f, y0 + 0.8f, x1 + 0.6f, y1 + 0.8f);
        frame(c, p, col, 0.7f, x0, y0, x1, y1);
        int dk = City.darken(col, 0.7f);
        for (float x = x0; x <= x1 + 0.1f; x += (x1 - x0) / Math.max(1, Math.round((x1 - x0) / step))) {
            rect(c, p, dk, x - 0.7f, y0 - 0.7f, x + 0.7f, y0 + 0.7f);
            rect(c, p, dk, x - 0.7f, y1 - 0.7f, x + 0.7f, y1 + 0.7f);
        }
        for (float y = y0; y <= y1 + 0.1f; y += (y1 - y0) / Math.max(1, Math.round((y1 - y0) / step))) {
            rect(c, p, dk, x0 - 0.7f, y - 0.7f, x0 + 0.7f, y + 0.7f);
            rect(c, p, dk, x1 - 0.7f, y - 0.7f, x1 + 0.7f, y + 0.7f);
        }
    }

    private static void rock(Canvas c, Paint p, float x, float y, float rad, Random r) {
        float[] k = jit(r, 7, 0.22f);
        blob(c, p, 0x40000000, x + rad * 0.3f, y + rad * 0.35f, rad, rad * 0.85f, k, 0);
        blob(c, p, 0xFF64625C, x, y, rad, rad * 0.85f, k, 0);
        blob(c, p, 0xFF85837A, x - rad * 0.2f, y - rad * 0.2f, rad * 0.6f, rad * 0.5f, k, 0);
        circle(c, p, 0xFF5E7A44, x + rad * 0.3f, y - rad * 0.1f, rad * 0.22f);
    }

    /** A dog seen from above, heading at deg degrees. */
    private static void dog(Canvas c, Paint p, float x, float y, float deg, int col, float len) {
        float w = len * 0.42f;
        c.save();
        c.translate(x, y);
        c.rotate(deg);
        oval(c, p, 0x38000000, -len / 2 + 0.7f, -w / 2 + 0.9f, len / 2 + 0.7f, w / 2 + 0.9f);
        line(c, p, City.darken(col, 0.8f), 0.7f, -len / 2, 0, -len / 2 - len * 0.3f, -len * 0.12f);
        oval(c, p, col, -len / 2, -w / 2, len / 2, w / 2);
        circle(c, p, col, len / 2 + w * 0.2f, 0, w * 0.55f);
        circle(c, p, City.darken(col, 0.6f), len / 2 + w * 0.05f, -w * 0.42f, w * 0.22f);
        circle(c, p, City.darken(col, 0.6f), len / 2 + w * 0.05f, w * 0.42f, w * 0.22f);
        circle(c, p, 0xFF202020, len / 2 + w * 0.7f, 0, w * 0.13f);
        c.restore();
    }

    /** A rowing boat heading at deg degrees. */
    private static void boat(Canvas c, Paint p, float x, float y, float deg, float len, int col, boolean rower) {
        float w = len * 0.4f;
        c.save();
        c.translate(x, y);
        c.rotate(deg);
        if (rower) {
            line(c, p, 0xFF8A6E4A, 0.6f, -len * 0.05f, -w / 2 - len * 0.45f, len * 0.05f, w / 2 + len * 0.45f);
            line(c, p, 0x60FFFFFF, 0.5f, -len * 0.75f, -w * 0.3f, -len * 1.4f, -w * 0.8f);
            line(c, p, 0x60FFFFFF, 0.5f, -len * 0.75f, w * 0.3f, -len * 1.4f, w * 0.8f);
        }
        poly(c, p, 0x40000000, -len / 2 + 0.8f, -w / 2 + 1, len * 0.2f + 0.8f, -w / 2 + 1, len / 2 + 0.8f, 1, len * 0.2f + 0.8f, w / 2 + 1, -len / 2 + 0.8f, w / 2 + 1);
        poly(c, p, col, -len / 2, -w / 2, len * 0.2f, -w / 2, len / 2, 0, len * 0.2f, w / 2, -len / 2, w / 2);
        poly(c, p, 0xFF9A7A54, -len / 2 + 0.7f, -w / 2 + 0.7f, len * 0.18f, -w / 2 + 0.7f, len / 2 - 1.2f, 0, len * 0.18f, w / 2 - 0.7f, -len / 2 + 0.7f, w / 2 - 0.7f);
        rect(c, p, 0xFF6E5034, -len * 0.3f, -w / 2 + 0.7f, -len * 0.22f, w / 2 - 0.7f);
        rect(c, p, 0xFF6E5034, len * 0.05f, -w / 2 + 0.7f, len * 0.13f, w / 2 - 0.7f);
        if (rower) {
            circle(c, p, 0xFF3A5E9A, -len * 0.08f, 0, w * 0.32f);
            circle(c, p, 0xFFD8B090, -len * 0.08f, 0, w * 0.2f);
        }
        c.restore();
    }

    private static void duck(Canvas c, Paint p, float x, float y, float deg, boolean drake) {
        c.save();
        c.translate(x, y);
        c.rotate(deg);
        line(c, p, 0x50FFFFFF, 0.4f, -1.4f, -0.6f, -3.8f, -1.8f);
        line(c, p, 0x50FFFFFF, 0.4f, -1.4f, 0.6f, -3.8f, 1.8f);
        oval(c, p, drake ? 0xFF8A7A66 : 0xFF9A8264, -1.3f, -0.75f, 1.3f, 0.75f);
        circle(c, p, drake ? 0xFF2E6A3E : 0xFF7A6248, 1.2f, 0, 0.55f);
        rect(c, p, 0xFFE0A020, 1.6f, -0.2f, 2.2f, 0.2f);
        c.restore();
    }

    private static final int[] BRIGHT = {0xFFE0508A, 0xFF30B0C8, 0xFFE8C030, 0xFF58B048, 0xFF8A5AC8, 0xFFE0702E};

    // ------------------------------------------------------------------ the designs

    static void register(List<Landmarks.Def> out) {
        japaneseGarden(out);
        hedgeMaze(out);
        boatingLake(out);
        glasshouse(out);
        sculpturePark(out);
        skatePark(out);
        allotments(out);
        dogPark(out);
        splashPad(out);
        amphitheatre(out);
        warMemorial(out);
        arboretum(out);
        miniGolf(out);
    }

    // 1. A Japanese garden: koi pond with stepping stones, raked gravel, a red bridge over a stream, maples, tea house.
    private static void japaneseGarden(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Japanese Garden", T_PARK) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int bL = Math.max(bw, bh);
                return new int[][]{tb(bw, bh, bL - 5, 1, 4, 3, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                Fr f = new Fr(x0, y0, x1, y1);
                float S = f.S;
                float[] th = part(plan(Math.round(f.w / T), Math.round(f.h / T), null), 0, x0, y0);
                rect(c, p, 0xFF587F42, x0, y0, x1, y1);
                for (int k = 0; k < 36; k++) {
                    circle(c, p, r.nextBoolean() ? 0x1690C070 : 0x162A4A1E, x0 + r.nextFloat() * f.w, y0 + r.nextFloat() * f.h, 3 + r.nextFloat() * 7);
                }
                // Raked gravel with three rocks in rings.
                float gx0 = f.x(0.04f, 0.46f), gy0 = f.y(0.04f, 0.46f), gx1 = f.x(0.31f, 0.94f), gy1 = f.y(0.31f, 0.94f);
                rect(c, p, 0xFF6E6A60, gx0 - 1.2f, gy0 - 1.2f, gx1 + 1.2f, gy1 + 1.2f);
                int gravel = 0xFFDAD5C6, rake = 0xFFC2BCAA;
                rect(c, p, gravel, gx0, gy0, gx1, gy1);
                if (gx1 - gx0 >= gy1 - gy0) {
                    for (float y = gy0 + 1.6f; y < gy1; y += 2.2f) line(c, p, rake, 0.6f, gx0, y, gx1, y);
                } else {
                    for (float x = gx0 + 1.6f; x < gx1; x += 2.2f) line(c, p, rake, 0.6f, x, gy0, x, gy1);
                }
                float[][] rk = {{0.3f, 0.28f, 1.1f}, {0.7f, 0.55f, 0.8f}, {0.38f, 0.8f, 0.65f}};
                for (float[] q : rk) {
                    float rx = gx0 + (gx1 - gx0) * q[0], ry = gy0 + (gy1 - gy0) * q[1], rr = S * 0.035f * q[2] + 2;
                    c.save();
                    c.clipRect(gx0, gy0, gx1, gy1);
                    circle(c, p, gravel, rx, ry, rr + 8.5f);
                    for (int k = 1; k <= 3; k++) ring(c, p, rake, 0.6f, rx, ry, rr + k * 2.4f);
                    c.restore();
                    rock(c, p, rx, ry, rr, r);
                }
                // A stream from the far end into the pond, then the pond.
                float pcx = f.x(0.56f, 0.6f), pcy = f.y(0.56f, 0.6f), prx = f.ex(0.2f * f.L, 0.27f * S), pry = f.ey(0.2f * f.L, 0.27f * S);
                float[] k = jit(r, 12, 0.13f);
                float[] st = f.pts(0.64f, 0.66f, 0.76f, 0.74f, 0.86f, 0.64f, 1.02f, 0.72f);
                float sw = Math.max(6, S * 0.06f);
                trail(c, p, 0xFF7E7666, sw + 4, st);
                blob(c, p, 0xFF7E7666, pcx, pcy, prx, pry, k, 2.5f);
                trail(c, p, 0xFF3A7682, sw, st);
                blob(c, p, 0xFF3A7682, pcx, pcy, prx, pry, k, 0);
                blob(c, p, 0xFF2C6270, pcx, pcy, prx * 0.72f, pry * 0.72f, k, 0);
                blob(c, p, 0xFF24566A, pcx + 2, pcy + 1, prx * 0.4f, pry * 0.4f, k, 0);
                // Rim stones.
                for (int i = 0; i < 12; i++) {
                    double a = r.nextFloat() * Math.PI * 2;
                    int j = (int) ((a / (2 * Math.PI)) * k.length) % k.length;
                    float ex = pcx + (float) Math.cos(a) * (prx * k[j] + 1), ey = pcy + (float) Math.sin(a) * (pry * k[j] + 1);
                    oval(c, p, 0xFF8E8A80, ex - 2.2f, ey - 1.6f, ex + 2.2f, ey + 1.6f);
                    oval(c, p, 0xFFA8A498, ex - 1.8f, ey - 1.5f, ex + 1, ey + 0.6f);
                }
                // Lily pads and koi.
                for (int i = 0; i < 7; i++) {
                    double a = r.nextFloat() * Math.PI * 2;
                    float rr = 0.55f + r.nextFloat() * 0.3f, lx = pcx + (float) Math.cos(a) * prx * rr, ly = pcy + (float) Math.sin(a) * pry * rr;
                    circle(c, p, 0xFF4A8A3E, lx, ly, 1.6f + r.nextFloat());
                    if (i % 3 == 0) circle(c, p, 0xFFF0B8C8, lx + 0.4f, ly - 0.3f, 0.7f);
                }
                for (int i = 0; i < 8; i++) {
                    double a = r.nextFloat() * Math.PI * 2;
                    float rr = r.nextFloat() * 0.6f, kx = pcx + (float) Math.cos(a) * prx * rr, ky = pcy + (float) Math.sin(a) * pry * rr;
                    c.save();
                    c.translate(kx, ky);
                    c.rotate(r.nextFloat() * 360);
                    int kc = i % 3 == 0 ? 0xFFF2EEE6 : i % 3 == 1 ? 0xFFE8742A : 0xFFE0A030;
                    poly(c, p, kc, -1.6f, 0, -2.8f, -0.9f, -2.8f, 0.9f);
                    oval(c, p, kc, -2, -0.75f, 2, 0.75f);
                    if (i % 3 == 0) circle(c, p, 0xFFD8501E, 0.4f, 0, 0.5f);
                    c.restore();
                }
                // Stepping stones across the pond and up to the tea house.
                int n = 12;
                for (int i = 0; i <= n; i++) {
                    float t = (float) i / n, u = 0.34f + 0.42f * t, v = 0.62f - (float) Math.sin(t * Math.PI) * 0.1f;
                    float sx = f.x(u, v), sy = f.y(u, v), sr = 2 + (i % 2) * 0.5f;
                    oval(c, p, 0x40000000, sx - sr + 0.6f, sy - sr * 0.8f + 0.8f, sx + sr + 0.6f, sy + sr * 0.8f + 0.8f);
                    oval(c, p, 0xFFB0AA9C, sx - sr, sy - sr * 0.8f, sx + sr, sy + sr * 0.8f);
                }
                float sx0 = f.x(0.76f, 0.56f), sy0 = f.y(0.76f, 0.56f), dx = (th[0] + th[2]) / 2, dy = (th[1] + th[3]) / 2;
                float tx = f.wide ? dx : th[2] + 3, ty = f.wide ? th[3] + 3 : dy;
                for (int i = 1; i <= 5; i++) {
                    float t = i / 6f, sx = sx0 + (tx - sx0) * t, sy = sy0 + (ty - sy0) * t;
                    oval(c, p, 0x40000000, sx - 1.4f, sy - 1.2f, sx + 2.4f, sy + 2);
                    oval(c, p, 0xFFB0AA9C, sx - 2, sy - 1.6f, sx + 2, sy + 1.6f);
                }
                // The red arched bridge across the stream.
                float bx = (st[2] + st[4]) / 2, by = (st[3] + st[5]) / 2;
                float ang = (float) Math.toDegrees(Math.atan2(st[5] - st[3], st[4] - st[2])) + 90;
                float bl = sw + 10, bwid = 5.5f;
                c.save();
                c.translate(bx, by);
                c.rotate(ang);
                rect(c, p, 0x48000000, -bl / 2 + 1, -bwid / 2 + 1.4f, bl / 2 + 1, bwid / 2 + 1.4f);
                rect(c, p, 0xFF9A2820, -bl / 2, -bwid / 2, bl / 2, bwid / 2);
                rect(c, p, 0xFFB8362A, -bl / 2 + 1, -bwid / 2 + 0.8f, bl / 2 - 1, bwid / 2 - 0.8f);
                rect(c, p, 0xFFCC4A38, -bl * 0.2f, -bwid / 2 + 0.8f, bl * 0.2f, bwid / 2 - 0.8f);
                for (float t = -bl / 2 + 2; t < bl / 2 - 1; t += 1.8f) line(c, p, 0x30000000, 0.4f, t, -bwid / 2 + 0.8f, t, bwid / 2 - 0.8f);
                line(c, p, 0xFF7A1A14, 0.8f, -bl / 2, -bwid / 2 + 0.4f, bl / 2, -bwid / 2 + 0.4f);
                line(c, p, 0xFF7A1A14, 0.8f, -bl / 2, bwid / 2 - 0.4f, bl / 2, bwid / 2 - 0.4f);
                for (int i = -1; i <= 1; i++) {
                    circle(c, p, 0xFF3A2A20, i * (bl / 2 - 0.6f), -bwid / 2 + 0.4f, 0.8f);
                    circle(c, p, 0xFF3A2A20, i * (bl / 2 - 0.6f), bwid / 2 - 0.4f, 0.8f);
                }
                c.restore();
                // Rocks at the stream's head.
                rock(c, p, st[6] - 3, st[7] - sw * 0.7f, 3.2f, r);
                rock(c, p, st[6] - 5, st[7] + sw * 0.8f, 2.6f, r);
                // Stone lanterns and a water basin.
                float[][] lan = {{0.36f, 0.4f}, {0.8f, 0.9f}, {0.22f, 0.12f}};
                for (float[] q : lan) {
                    float lx = f.x(q[0], q[1]), ly = f.y(q[0], q[1]);
                    rect(c, p, 0x40000000, lx - 1.6f, ly - 1.2f, lx + 3.4f, ly + 3.8f);
                    rect(c, p, 0xFF85827A, lx - 2.5f, ly - 2.5f, lx + 2.5f, ly + 2.5f);
                    rect(c, p, 0xFFA6A398, lx - 1.6f, ly - 1.6f, lx + 1.6f, ly + 1.6f);
                    circle(c, p, 0xFFF0E0A0, lx, ly, 0.8f);
                }
                float wx = tx + f.ex(-10, 4), wy = ty + f.ey(-10, 4);
                circle(c, p, 0xFF7A776E, wx, wy, 3);
                circle(c, p, 0xFF3A7682, wx, wy, 1.8f);
                line(c, p, 0xFF9AAA5A, 0.8f, wx + 1, wy - 1, wx + 4, wy - 4);
                // Maples, pines and clipped azaleas, kept off the tea house.
                float[][] tr = {{0.1f, 0.2f, 0.11f, 0}, {0.25f, 0.1f, 0.08f, 1}, {0.42f, 0.16f, 0.09f, 2}, {0.56f, 0.12f, 0.08f, 0},
                        {0.94f, 0.48f, 0.09f, 2}, {0.92f, 0.9f, 0.08f, 1}, {0.64f, 0.93f, 0.07f, 0}, {0.42f, 0.93f, 0.06f, 1},
                        {0.08f, 0.38f, 0.06f, 2}, {0.7f, 0.42f, 0.05f, 3}, {0.4f, 0.4f, 0.05f, 3}, {0.85f, 0.8f, 0.05f, 3}};
                int[] tc = {0xFFB43A2A, 0xFFD06A2A, 0xFF2E5A3A, 0xFF4E8A40};
                for (float[] q : tr) {
                    float px = f.x(q[0], q[1]), py = f.y(q[0], q[1]), rad = Math.max(3, S * q[2] * 0.6f + 2);
                    if (inside(th, rad, px, py)) continue;
                    if (q[3] == 3) bush(c, p, px, py, rad * 0.8f);
                    else tree(c, p, px, py, rad, tc[(int) q[3]]);
                    if (q[3] == 2) {
                        circle(c, p, 0x30000000, px + rad * 0.2f, py + rad * 0.2f, rad * 0.5f);
                        circle(c, p, 0xFF3E6E46, px - rad * 0.3f, py - rad * 0.2f, rad * 0.4f);
                    }
                }
                // A bamboo fence round it all.
                frame(c, p, 0xFFA89060, 1.6f, x0 + 1.2f, y0 + 1.2f, x1 - 1.2f, y1 - 1.2f);
                frame(c, p, 0xFF7A6640, 0.5f, x0 + 2.2f, y0 + 2.2f, x1 - 2.2f, y1 - 2.2f);
                sign(city, "TEA HOUSE", (th[0] + th[2]) / 2, (th[1] + th[3]) / 2, 5, 0xFFF0E8D8);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                // Dark tiled hip-and-gable roof with a timber veranda edge and upturned corners.
                rect(c, p, 0xFF8A6A48, x0, y0, x1, y1);
                hip(c, p, x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0xFF4E4A46);
                float e = 2.5f;
                poly(c, p, 0xFF6E6A64, x0, y0, x0 + e, y0, x0, y0 + e);
                poly(c, p, 0xFF6E6A64, x1, y0, x1 - e, y0, x1, y0 + e);
                poly(c, p, 0xFF3A3632, x0, y1, x0 + e, y1, x0, y1 - e);
                poly(c, p, 0xFF3A3632, x1, y1, x1 - e, y1, x1, y1 - e);
                float mx = (x0 + x1) / 2, my = (y0 + y1) / 2, in = Math.min(x1 - x0, y1 - y0) / 2 - 1;
                boolean wide = x1 - x0 >= y1 - y0;
                circle(c, p, 0xFF2E2A26, wide ? x0 + in + 1 : mx, wide ? my : y0 + in + 1, 1.3f);
                circle(c, p, 0xFF2E2A26, wide ? x1 - in - 1 : mx, wide ? my : y1 - in - 1, 1.3f);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.PARK | Variants.SUB | Variants.MID | Variants.OLD | Variants.UNI;
        d.minS = 10;
        d.minL = 12;
        d.base = City.SHOP;
        d.inside = Variants.I_CAFE;
        d.roofCol = 0xFF4E4A46;
        out.add(d);
    }

    // 2. A hedge maze, a fresh one every time, with a gazebo in a little square at its heart.
    private static void hedgeMaze(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Hedge Maze", T_PARK) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return new int[][]{{bw / 2 - 1, bh / 2 - 1, 2, 2, 1}};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                float W = x1 - x0, H = y1 - y0;
                int bw = Math.round(W / T), bh = Math.round(H / T);
                float gx = x0 + (bw / 2) * T, gy = y0 + (bh / 2) * T;
                rect(c, p, 0xFF5E9248, x0, y0, x1, y1);
                float cell = Math.max(14f, Math.min(W, H) / 15f);
                int hc = Math.max(3, (int) ((Math.min(gx - x0, x1 - gx) - 6 - cell / 2) / cell));
                int hr = Math.max(3, (int) ((Math.min(gy - y0, y1 - gy) - 6 - cell / 2) / cell));
                int nc = 2 * hc + 1, nr = 2 * hr + 1;
                float mx0 = gx - nc * cell / 2, my0 = gy - nr * cell / 2, mx1 = mx0 + nc * cell, my1 = my0 + nr * cell;
                boolean[][] hw = new boolean[nr + 1][nc], vw = new boolean[nr][nc + 1];
                for (boolean[] a : hw) java.util.Arrays.fill(a, true);
                for (boolean[] a : vw) java.util.Arrays.fill(a, true);
                boolean[][] vis = new boolean[nr][nc];
                for (int i = hr - 1; i <= hr + 1; i++) {
                    for (int j = hc - 1; j <= hc + 1; j++) vis[i][j] = true;
                    vw[i][hc] = false;
                    vw[i][hc + 1] = false;
                }
                for (int j = hc - 1; j <= hc + 1; j++) {
                    hw[hr][j] = false;
                    hw[hr + 1][j] = false;
                }
                // Carve with a depth-first walk.
                int[] stack = new int[nr * nc], cand = new int[4];
                int sp = 0;
                stack[sp++] = 0;
                vis[0][0] = true;
                while (sp > 0) {
                    int cur = stack[sp - 1], i = cur / nc, j = cur % nc, n = 0;
                    if (i > 0 && !vis[i - 1][j]) cand[n++] = 0;
                    if (i < nr - 1 && !vis[i + 1][j]) cand[n++] = 1;
                    if (j > 0 && !vis[i][j - 1]) cand[n++] = 2;
                    if (j < nc - 1 && !vis[i][j + 1]) cand[n++] = 3;
                    if (n == 0) {
                        sp--;
                        continue;
                    }
                    int dir = cand[r.nextInt(n)], ni = i, nj = j;
                    if (dir == 0) {
                        hw[i][j] = false;
                        ni = i - 1;
                    } else if (dir == 1) {
                        hw[i + 1][j] = false;
                        ni = i + 1;
                    } else if (dir == 2) {
                        vw[i][j] = false;
                        nj = j - 1;
                    } else {
                        vw[i][j + 1] = false;
                        nj = j + 1;
                    }
                    vis[ni][nj] = true;
                    stack[sp++] = ni * nc + nj;
                }
                int side = r.nextInt(4);
                if (side == 0) hw[hr - 1][hc] = false;
                else if (side == 1) hw[hr + 2][hc] = false;
                else if (side == 2) vw[hr][hc - 1] = false;
                else vw[hr][hc + 2] = false;
                // A few loops so it isn't a perfect maze.
                for (int k = 0; k < nc * nr / 40; k++) {
                    int i = 1 + r.nextInt(nr - 2), j = 1 + r.nextInt(nc - 1);
                    vw[i][j] = false;
                }
                hw[0][hc] = false;
                hw[nr][hc] = false;
                // Gravel floor and the gazebo square.
                rect(c, p, 0xFFB8AC8A, mx0 - 4, my0 - 4, mx1 + 4, my1 + 4);
                rect(c, p, 0xFFCDC2A2, mx0, my0, mx1, my1);
                circle(c, p, 0xFFB8AE96, gx, gy, cell * 1.45f);
                circle(c, p, 0xFFDAD2BC, gx, gy, cell * 1.35f);
                for (int k = 0; k < 8; k++) {
                    double a = k * Math.PI / 4;
                    line(c, p, 0x22000000, 0.5f, gx, gy, gx + (float) Math.cos(a) * cell * 1.35f, gy + (float) Math.sin(a) * cell * 1.35f);
                }
                // Paths to the two gates.
                rect(c, p, 0xFFCDC2A2, mx0 + hc * cell, y0, mx0 + (hc + 1) * cell, my0);
                rect(c, p, 0xFFCDC2A2, mx0 + hc * cell, my1, mx0 + (hc + 1) * cell, y1);
                // The hedges, as long runs: shadow, body, lit top.
                float hwid = cell * 0.36f;
                int[] cols = {0x48000000, 0xFF2C5A2A, 0xFF3E7636, 0xFF52904A};
                float[] ws = {hwid, hwid, hwid * 0.62f, hwid * 0.22f};
                float[] off = {1.4f, 0, -0.35f, -0.8f};
                p.setStrokeCap(Paint.Cap.ROUND);
                for (int pass = 0; pass < 4; pass++) {
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(ws[pass]);
                    p.setColor(cols[pass]);
                    float ox = off[pass], oy = pass == 0 ? 1.8f : off[pass];
                    for (int i = 0; i <= nr; i++) {
                        int j = 0;
                        while (j < nc) {
                            if (!hw[i][j]) {
                                j++;
                                continue;
                            }
                            int e = j;
                            while (e < nc && hw[i][e]) e++;
                            c.drawLine(mx0 + j * cell + ox, my0 + i * cell + oy, mx0 + e * cell + ox, my0 + i * cell + oy, p);
                            j = e;
                        }
                    }
                    for (int j = 0; j <= nc; j++) {
                        int i = 0;
                        while (i < nr) {
                            if (!vw[i][j]) {
                                i++;
                                continue;
                            }
                            int e = i;
                            while (e < nr && vw[e][j]) e++;
                            c.drawLine(mx0 + j * cell + ox, my0 + i * cell + oy, mx0 + j * cell + ox, my0 + e * cell + oy, p);
                            i = e;
                        }
                    }
                }
                p.setStrokeCap(Paint.Cap.BUTT);
                p.setStyle(Paint.Style.FILL);
                // Topiary balls at the gates, trees and benches in the margins.
                float ex0 = mx0 + hc * cell, ex1 = mx0 + (hc + 1) * cell;
                if (my0 - y0 > 8) {
                    bush(c, p, ex0 - 3, my0 - 4, 3);
                    bush(c, p, ex1 + 3, my0 - 4, 3);
                }
                if (y1 - my1 > 8) {
                    bush(c, p, ex0 - 3, my1 + 4, 3);
                    bush(c, p, ex1 + 3, my1 + 4, 3);
                }
                float mxg = Math.min(mx0 - x0, my0 - y0);
                if (mxg > 10) {
                    tree(c, p, x0 + mxg / 2, y0 + mxg / 2, mxg * 0.4f);
                    tree(c, p, x1 - mxg / 2, y0 + mxg / 2, mxg * 0.4f, 0xFF4A7E3A);
                    tree(c, p, x0 + mxg / 2, y1 - mxg / 2, mxg * 0.4f, 0xFF4A7E3A);
                    tree(c, p, x1 - mxg / 2, y1 - mxg / 2, mxg * 0.4f);
                }
                if (my0 - y0 > 9) {
                    for (float x = x0 + 14; x < x1 - 14; x += 26) if (x < ex0 - 10 || x > ex1 + 10) bench(c, p, x, (y0 + my0) / 2 - 1, true);
                }
                if (y1 - my1 > 9) {
                    for (float x = x0 + 14; x < x1 - 14; x += 26) if (x < ex0 - 10 || x > ex1 + 10) bench(c, p, x, (y1 + my1) / 2 + 1, true);
                }
                if (mx0 - x0 > 9) {
                    for (float y = y0 + 20; y < y1 - 20; y += 30) {
                        bush(c, p, (x0 + mx0) / 2, y, 2.5f);
                        bush(c, p, (x1 + mx1) / 2, y, 2.5f);
                    }
                }
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                // An octagonal shingled gazebo roof with a finial.
                float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2, rad = Math.min(x1 - x0, y1 - y0) / 2;
                float[] px = new float[8], py = new float[8];
                for (int k = 0; k < 8; k++) {
                    double a = Math.PI / 8 + k * Math.PI / 4;
                    px[k] = cx + (float) Math.cos(a) * rad;
                    py[k] = cy + (float) Math.sin(a) * rad;
                }
                poly(c, p, 0xFFE8E4DA, px[0], py[0], px[1], py[1], px[2], py[2], px[3], py[3], px[4], py[4], px[5], py[5], px[6], py[6], px[7], py[7]);
                for (int k = 0; k < 8; k++) {
                    int j = (k + 1) % 8;
                    double mid = Math.PI / 8 + (k + 0.5) * Math.PI / 4;
                    float lit = (float) Math.cos(mid - Math.PI * 1.25);
                    int col = shade(0xFF6E7A70, 1 + lit * 0.22f);
                    poly(c, p, col, cx, cy, cx + (px[k] - cx) * 0.9f, cy + (py[k] - cy) * 0.9f, cx + (px[j] - cx) * 0.9f, cy + (py[j] - cy) * 0.9f);
                    line(c, p, 0xFFDAD6CC, 0.6f, cx, cy, px[k], py[k]);
                }
                circle(c, p, 0xFF3E5A46, cx, cy, 1.8f);
                circle(c, p, 0xFFE0C060, cx - 0.3f, cy - 0.3f, 0.9f);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.PARK | Variants.SUB | Variants.OLD | Variants.MID;
        d.minS = 9;
        d.minL = 9;
        d.base = City.KIOSK;
        d.inside = Variants.I_SEATS;
        d.roofCol = 0xFF6E7A70;
        out.add(d);
    }

    // 3. A boating lake: an irregular lake with an island, rowing boats, a boathouse and jetty, ducks and willows.
    private static void boatingLake(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Boating Lake", T_PARK) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int bS = Math.min(bw, bh);
                return new int[][]{tb(bw, bh, 1, bS - 4, 4, 3, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                Fr f = new Fr(x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] bh = part(plan(Math.round(f.w / T), Math.round(f.h / T), null), 0, x0, y0);
                rect(c, p, 0xFF62964A, x0, y0, x1, y1);
                for (int k = 0; k < 24; k++) circle(c, p, 0x1490C070, x0 + r.nextFloat() * f.w, y0 + r.nextFloat() * f.h, 4 + r.nextFloat() * 8);
                float cx = f.x(0.57f, 0.42f), cy = f.y(0.57f, 0.42f);
                float rx = f.ex(0.39f * L, 0.36f * S), ry = f.ey(0.39f * L, 0.36f * S);
                float[] k = jit(r, 14, 0.1f);
                // The path round the lake, a grass verge, the beach, then the water in depths.
                blob(c, p, 0xFFB8AC8A, cx, cy, rx, ry, k, 11.5f);
                blob(c, p, 0xFFD2C6A6, cx, cy, rx, ry, k, 10.5f);
                blob(c, p, 0xFF62964A, cx, cy, rx, ry, k, 5.5f);
                blob(c, p, 0xFFB8A57E, cx, cy, rx, ry, k, 1.8f);
                blob(c, p, 0xFF5E98AE, cx, cy, rx, ry, k, 0);
                blob(c, p, 0xFF467F9E, cx, cy, rx * 0.86f, ry * 0.84f, k, 0);
                blob(c, p, 0xFF3A6E90, cx + 2, cy + 1, rx * 0.62f, ry * 0.6f, k, 0);
                for (int i = 0; i < 10; i++) {
                    float wx = cx + (r.nextFloat() - 0.5f) * rx * 1.3f, wy = cy + (r.nextFloat() - 0.5f) * ry * 1.3f;
                    line(c, p, 0x40FFFFFF, 0.5f, wx, wy, wx + 3 + r.nextFloat() * 3, wy);
                }
                // Reeds in clumps on the shore.
                for (int i = 0; i < 6; i++) {
                    double a = r.nextFloat() * Math.PI * 2;
                    int j = (int) ((a / (2 * Math.PI)) * k.length) % k.length;
                    float ex = cx + (float) Math.cos(a) * rx * k[j], ey = cy + (float) Math.sin(a) * ry * k[j];
                    if (inside(bh, 10, ex, ey)) continue;
                    for (int q = 0; q < 7; q++) {
                        float qx = ex + (r.nextFloat() - 0.5f) * 6, qy = ey + (r.nextFloat() - 0.5f) * 6;
                        line(c, p, q % 3 == 0 ? 0xFFA89A5A : 0xFF5E8A3A, 0.6f, qx, qy, qx + (r.nextFloat() - 0.5f) * 1.5f, qy - 2.5f);
                    }
                }
                // An island with a willow and a duck house.
                float ix = f.x(0.72f, 0.36f), iy = f.y(0.72f, 0.36f), irx = f.ex(0.06f * L, 0.08f * S), iry = f.ey(0.06f * L, 0.08f * S);
                float[] ik = jit(r, 9, 0.15f);
                blob(c, p, 0xFFB8A57E, ix, iy, irx, iry, ik, 1.5f);
                blob(c, p, 0xFF5A8E44, ix, iy, irx, iry, ik, 0);
                tree(c, p, ix - irx * 0.2f, iy - iry * 0.1f, Math.min(irx, iry) * 0.8f, 0xFF8AAE4A);
                for (int q = 0; q < 10; q++) {
                    double a = q * Math.PI / 5;
                    float tr = Math.min(irx, iry) * 0.8f;
                    line(c, p, 0xFF9ABE5A, 0.5f, ix - irx * 0.2f, iy - iry * 0.1f, ix - irx * 0.2f + (float) Math.cos(a) * tr, iy - iry * 0.1f + (float) Math.sin(a) * tr);
                }
                rect(c, p, 0xFF8A5A3A, ix + irx * 0.3f, iy + iry * 0.2f, ix + irx * 0.3f + 3, iy + iry * 0.2f + 2.5f);
                // The jetty from the boathouse into the water, with boats tied up.
                float ju = (bh[0] + bh[2]) / 2, jv = (bh[1] + bh[3]) / 2, jl = 0.22f * S, jw = 7;
                float jx0, jy0, jx1, jy1;
                if (f.wide) {
                    jx0 = ju - jw / 2;
                    jx1 = ju + jw / 2;
                    jy1 = bh[1];
                    jy0 = bh[1] - jl;
                } else {
                    jy0 = jv - jw / 2;
                    jy1 = jv + jw / 2;
                    jx1 = bh[0];
                    jx0 = bh[0] - jl;
                }
                rect(c, p, 0x48000000, jx0 + 1, jy0 + 1.5f, jx1 + 1, jy1 + 1.5f);
                rect(c, p, 0xFF9A7A54, jx0, jy0, jx1, jy1);
                if (f.wide) {
                    for (float y = jy0 + 1.5f; y < jy1; y += 2) line(c, p, 0x40000000, 0.4f, jx0, y, jx1, y);
                } else {
                    for (float x = jx0 + 1.5f; x < jx1; x += 2) line(c, p, 0x40000000, 0.4f, x, jy0, x, jy1);
                }
                circle(c, p, 0xFF4A3A2A, jx0 + 0.8f, jy0 + 0.8f, 0.9f);
                circle(c, p, 0xFF4A3A2A, jx1 - 0.8f, jy0 + 0.8f, 0.9f);
                circle(c, p, 0xFF4A3A2A, jx0 + 0.8f, jy1 - 0.8f, 0.9f);
                circle(c, p, 0xFF4A3A2A, jx1 - 0.8f, jy1 - 0.8f, 0.9f);
                int[] bc = {0xFF2E5E8A, 0xFFB8402E, 0xFF3E7A4A, 0xFFE0C050, 0xFFE8E4DA};
                float bang = f.wide ? -90 : 180, bl = 9;
                if (f.wide) {
                    boat(c, p, jx0 - 3, jy0 + 6, bang, bl, bc[0], false);
                    boat(c, p, jx1 + 3, jy0 + 6, bang, bl, bc[1], false);
                    boat(c, p, jx1 + 3, jy0 + 6 + bl + 1, bang, bl, bc[2], false);
                } else {
                    boat(c, p, jx0 + 6, jy0 - 3, bang, bl, bc[0], false);
                    boat(c, p, jx0 + 6, jy1 + 3, bang, bl, bc[1], false);
                    boat(c, p, jx0 + 6 + bl + 1, jy1 + 3, bang, bl, bc[2], false);
                }
                // Boats out on the lake, a swan pedalo and ducks.
                for (int i = 0; i < 3; i++) {
                    double a = 0.6 + i * 2.0 + r.nextFloat() * 0.6;
                    float bx = cx + (float) Math.cos(a) * rx * 0.45f, by = cy + (float) Math.sin(a) * ry * 0.45f;
                    if (Math.abs(bx - ix) < irx + 8 && Math.abs(by - iy) < iry + 8) bx -= irx * 2 + 10;
                    boat(c, p, bx, by, r.nextFloat() * 360, bl, bc[(i + 2) % bc.length], true);
                }
                float sx = cx - rx * 0.25f, sy = cy + ry * 0.3f;
                oval(c, p, 0x40000000, sx - 4, sy - 2.2f, sx + 5, sy + 3.2f);
                oval(c, p, 0xFFF2F0EA, sx - 4.5f, sy - 3, sx + 4.5f, sy + 3);
                oval(c, p, 0xFFD8D4CA, sx - 2.5f, sy - 1.8f, sx + 2, sy + 1.8f);
                curve(c, p, 0xFFF2F0EA, 1.4f, sx + 3.5f, sy, sx + 6.5f, sy - 1, sx + 5.5f, sy - 3.5f);
                circle(c, p, 0xFFE0802A, sx + 5.8f, sy - 4, 0.7f);
                for (int i = 0; i < 7; i++) {
                    float gx = (i < 4 ? cx - rx * 0.55f : cx + rx * 0.3f) + (i % 4) * 4f, gy = (i < 4 ? cy - ry * 0.4f : cy + ry * 0.55f) + (i % 2) * 3;
                    duck(c, p, gx, gy, i < 4 ? 15 : 200, i % 2 == 0);
                }
                // Trees and willows around, benches and lamps on the path.
                float g = Math.max(22, (float) Math.sqrt(f.w * f.h / 60));
                int[] tc = {0xFF3E7A3A, 0xFF4A8640, 0xFF35683A, 0xFF8AAE4A};
                for (float ty = y0 + g * 0.4f; ty < y1 - 4; ty += g) {
                    for (float tx = x0 + g * 0.4f; tx < x1 - 4; tx += g) {
                        float px = tx + (r.nextFloat() - 0.5f) * g * 0.5f, py = ty + (r.nextFloat() - 0.5f) * g * 0.5f;
                        float qx = (px - cx) / (rx * 1.1f + 18), qy = (py - cy) / (ry * 1.1f + 18);
                        if (qx * qx + qy * qy < 1 || inside(bh, 9, px, py) || inside(new float[]{jx0, jy0, jx1, jy1}, 8, px, py)) continue;
                        float rad = 5 + r.nextFloat() * 3;
                        px = Math.max(x0 + 4, Math.min(x1 - 4, px));
                        py = Math.max(y0 + 4, Math.min(y1 - 4, py));
                        int sp = r.nextInt(tc.length);
                        tree(c, p, px, py, rad, tc[sp]);
                        if (sp == 3) for (int q = 0; q < 8; q++) {
                            double a = q * Math.PI / 4;
                            line(c, p, 0xFF9ABE5A, 0.5f, px, py, px + (float) Math.cos(a) * rad * 0.9f, py + (float) Math.sin(a) * rad * 0.9f);
                        }
                    }
                }
                for (int i = 0; i < 6; i++) {
                    double a = -Math.PI / 2 + i * Math.PI / 3 + 0.3;
                    int j = (int) ((((a % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI)) / (2 * Math.PI) * k.length) % k.length;
                    float ex = cx + (float) Math.cos(a) * (rx * k[j] + 16), ey = cy + (float) Math.sin(a) * (ry * k[j] + 16);
                    if (ex < x0 + 5 || ex > x1 - 5 || ey < y0 + 5 || ey > y1 - 5 || inside(bh, 6, ex, ey)) continue;
                    if (i % 2 == 0) bench(c, p, ex, ey, Math.abs(Math.sin(a)) > 0.7);
                    else lamp(c, p, ex, ey);
                }
                sign(city, "BOATS", (bh[0] + bh[2]) / 2, (bh[1] + bh[3]) / 2, 5, 0xFFF0E8D8);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                gable(c, p, x0, y0, x1, y1, 0xFF7A3E2E);
                float mx = (x0 + x1) / 2, my = (y0 + y1) / 2;
                rect(c, p, 0xFFE8E2D4, mx - 2.5f, my - 2.5f, mx + 2.5f, my + 2.5f);
                poly(c, p, 0xFF3E6E5A, mx - 2, my - 2, mx + 2, my - 2, mx, my);
                poly(c, p, 0xFF2E5A4A, mx - 2, my + 2, mx + 2, my + 2, mx, my);
                poly(c, p, 0xFF4E8270, mx - 2, my - 2, mx - 2, my + 2, mx, my);
                poly(c, p, 0xFF2A5244, mx + 2, my - 2, mx + 2, my + 2, mx, my);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.PARK | Variants.SUB | Variants.MID;
        d.minS = 11;
        d.minL = 14;
        d.base = City.SHOP;
        d.inside = Variants.I_CAFE;
        d.roofCol = 0xFF7A3E2E;
        out.add(d);
    }

    // 4. A Victorian palm house: glass wings and a dome over the palms, with formal bedding round it.
    private static void glasshouse(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Botanical Glasshouse", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int bL = Math.max(bw, bh), bS = Math.min(bw, bh), mu = bL >= 18 ? 3 : 2;
                return new int[][]{tb(bw, bh, mu, 2, bL - 2 * mu, bS - 4, 2)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                float[] b = part(plan(Math.round((x1 - x0) / T), Math.round((y1 - y0) / T), null), 0, x0, y0);
                float cx = (b[0] + b[2]) / 2, cy = (b[1] + b[3]) / 2;
                rect(c, p, 0xFF5E9246, x0, y0, x1, y1);
                rect(c, p, 0xFFC8BC9C, x0 + 1, y0 + 1, x1 - 1, y1 - 1);
                rect(c, p, 0xFF5E9246, x0 + 4, y0 + 4, x1 - 4, y1 - 4);
                float ap = 5;
                rect(c, p, 0xFFD8CDB0, b[0] - ap, b[1] - ap, b[2] + ap, b[3] + ap);
                // Cross paths from each side to the doors.
                float pw = 4;
                rect(c, p, 0xFFD8CDB0, cx - pw, y0, cx + pw, y1);
                rect(c, p, 0xFFD8CDB0, x0, cy - pw, x1, cy + pw);
                // Beds in the bands round the house: box-edged with stripes of bedding.
                int[] fl = {0xFFC83A3A, 0xFFE8C040, 0xFF8A5AB8, 0xFFF0EEE6, 0xFFE07AA0, 0xFFE0802E};
                float[][] bands = {{x0 + 4, y0 + 4, x1 - 4, b[1] - ap - 2}, {x0 + 4, b[3] + ap + 2, x1 - 4, y1 - 4},
                        {x0 + 4, b[1] - ap, b[0] - ap - 2, b[3] + ap}, {b[2] + ap + 2, b[1] - ap, x1 - 4, b[3] + ap}};
                for (int bi = 0; bi < 4; bi++) {
                    float[] q = bands[bi];
                    float bwid = q[2] - q[0], bhgt = q[3] - q[1];
                    if (bwid < 8 || bhgt < 8) continue;
                    boolean along = bwid >= bhgt;
                    float len = along ? bwid : bhgt, mid = along ? cx : cy, start = along ? q[0] : q[1];
                    int n = Math.max(2, Math.round(len / (3.2f * T)));
                    if (n % 2 == 1) n++;
                    float seg = len / n;
                    for (int s = 0; s < n; s++) {
                        float a0 = start + s * seg + 1.5f, a1 = start + (s + 1) * seg - 1.5f;
                        if (a1 > mid - pw - 1 && a0 < mid + pw + 1) {
                            if (s < n / 2) a1 = mid - pw - 1.5f;
                            else a0 = mid + pw + 1.5f;
                        }
                        if (a1 - a0 < 5) continue;
                        float bx0 = along ? a0 : q[0] + 1, by0 = along ? q[1] + 1 : a0, bx1 = along ? a1 : q[2] - 1, by1 = along ? q[3] - 1 : a1;
                        // A lily pool in the middle of the long bands, beds elsewhere.
                        rect(c, p, 0x30000000, bx0 + 0.6f, by0 + 0.8f, bx1 + 0.6f, by1 + 0.8f);
                        rect(c, p, 0xFF3A6A32, bx0, by0, bx1, by1);
                        rect(c, p, 0xFF6A5038, bx0 + 1.3f, by0 + 1.3f, bx1 - 1.3f, by1 - 1.3f);
                        float tw = along ? by1 - by0 - 2.6f : bx1 - bx0 - 2.6f;
                        int rows = Math.max(2, (int) (tw / 2.6f));
                        int c0 = r.nextInt(fl.length);
                        for (int k = 0; k < rows; k++) {
                            float t0 = 1.3f + tw * k / rows + 0.3f, t1 = 1.3f + tw * (k + 1) / rows - 0.3f;
                            int col = fl[(c0 + (k == 0 || k == rows - 1 ? 0 : 1 + (k % 2))) % fl.length];
                            if (along) rect(c, p, col, bx0 + 1.6f, by0 + t0, bx1 - 1.6f, by0 + t1);
                            else rect(c, p, col, bx0 + t0, by0 + 1.6f, bx0 + t1, by1 - 1.6f);
                        }
                        float tx = (bx0 + bx1) / 2, ty = (by0 + by1) / 2;
                        circle(c, p, 0x40000000, tx + 0.8f, ty + 1, 2);
                        circle(c, p, 0xFF2E5A2E, tx, ty, 2);
                        circle(c, p, 0xFF4E8A40, tx - 0.5f, ty - 0.5f, 1.1f);
                    }
                }
                // Lily pools on the long sides by the doors, urns at the house corners.
                boolean wide = b[2] - b[0] >= b[3] - b[1];
                float pr = Math.min(9, Math.max(4, (wide ? b[1] - y0 : b[0] - x0) * 0.25f));
                if (wide) {
                    pond(c, p, cx - pr * 1.6f, y0 + (b[1] - y0) * 0.5f - pr * 0.7f, cx + pr * 1.6f, y0 + (b[1] - y0) * 0.5f + pr * 0.7f);
                    pond(c, p, cx - pr * 1.6f, b[3] + (y1 - b[3]) * 0.5f - pr * 0.7f, cx + pr * 1.6f, b[3] + (y1 - b[3]) * 0.5f + pr * 0.7f);
                } else {
                    pond(c, p, x0 + (b[0] - x0) * 0.5f - pr * 0.7f, cy - pr * 1.6f, x0 + (b[0] - x0) * 0.5f + pr * 0.7f, cy + pr * 1.6f);
                    pond(c, p, b[2] + (x1 - b[2]) * 0.5f - pr * 0.7f, cy - pr * 1.6f, b[2] + (x1 - b[2]) * 0.5f + pr * 0.7f, cy + pr * 1.6f);
                }
                float[][] uc = {{b[0] - ap, b[1] - ap}, {b[2] + ap, b[1] - ap}, {b[0] - ap, b[3] + ap}, {b[2] + ap, b[3] + ap}};
                for (float[] u : uc) {
                    circle(c, p, 0x40000000, u[0] + 0.8f, u[1] + 1, 2.4f);
                    circle(c, p, 0xFFB8AE98, u[0], u[1], 2.4f);
                    circle(c, p, 0xFF4E8A40, u[0], u[1], 1.5f);
                    circle(c, p, 0xFFE07AA0, u[0] - 0.4f, u[1] - 0.4f, 0.6f);
                }
                lamp(c, p, cx - pw - 2, y0 + 3);
                lamp(c, p, cx + pw + 2, y1 - 3);
                tree(c, p, x0 + 6, y0 + 6, 5, 0xFF35683A);
                tree(c, p, x1 - 6, y0 + 6, 5, 0xFF35683A);
                tree(c, p, x0 + 6, y1 - 6, 5, 0xFF35683A);
                tree(c, p, x1 - 6, y1 - 6, 5, 0xFF35683A);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                float w = x1 - x0, h = y1 - y0, cx = (x0 + x1) / 2, cy = (y0 + y1) / 2, sh = Math.min(w, h);
                boolean horiz = w >= h;
                int iron = 0xFFF0F2EE, bar = 0xC8FFFFFF;
                rect(c, p, iron, x0, y0, x1, y1);
                rect(c, p, 0xFF30583A, x0 + 1, y0 + 1, x1 - 1, y1 - 1);
                // The plants under the glass.
                int[] pc = {0xFF3E7A3A, 0xFF4E8A40, 0xFF2E6A3A, 0xFF6A9A4A, 0xFF5A8A36};
                int n = Math.min(44, (int) (w * h / 80));
                for (int k = 0; k < n; k++) {
                    circle(c, p, pc[r.nextInt(pc.length)], x0 + 3 + r.nextFloat() * (w - 6), y0 + 3 + r.nextFloat() * (h - 6), 2.5f + r.nextFloat() * 3.5f);
                }
                int palms = Math.min(10, (int) (w * h / 500) + 3);
                for (int k = 0; k < palms; k++) {
                    float px = x0 + 6 + r.nextFloat() * (w - 12), py = y0 + 6 + r.nextFloat() * (h - 12), pl = 4 + r.nextFloat() * 3;
                    for (int q = 0; q < 7; q++) {
                        double a = q * Math.PI * 2 / 7 + k;
                        line(c, p, 0xFF6AAA50, 1.1f, px, py, px + (float) Math.cos(a) * pl, py + (float) Math.sin(a) * pl);
                    }
                    circle(c, p, 0xFF8A6A3A, px, py, 0.8f);
                }
                for (int k = 0; k < 10; k++) circle(c, p, k % 2 == 0 ? 0xFFE04A5A : 0xFFF0C040, x0 + 3 + r.nextFloat() * (w - 6), y0 + 3 + r.nextFloat() * (h - 6), 0.8f);
                // Glazing over it all.
                rect(c, p, 0x60CFE8E4, x0 + 1, y0 + 1, x1 - 1, y1 - 1);
                // Lean-to aisles round the edge: ribs square to the walls.
                float ab = sh * 0.17f;
                rect(c, p, 0x24FFFFFF, x0 + 1, y0 + 1, x1 - 1, y0 + ab);
                rect(c, p, 0x1C000000, x0 + 1, y1 - ab, x1 - 1, y1 - 1);
                for (float x = x0 + 3; x < x1 - 1; x += 3.2f) {
                    line(c, p, bar, 0.5f, x, y0 + 1, x, y0 + ab);
                    line(c, p, bar, 0.5f, x, y1 - ab, x, y1 - 1);
                }
                for (float y = y0 + ab + 3; y < y1 - ab; y += 3.2f) {
                    line(c, p, bar, 0.5f, x0 + 1, y, x0 + ab, y);
                    line(c, p, bar, 0.5f, x1 - ab, y, x1 - 1, y);
                }
                frame(c, p, iron, 1, x0 + ab, y0 + ab, x1 - ab, y1 - ab);
                // Barrel-vaulted wings along the long side (and across, for a squarish house).
                float bw2 = sh * 0.26f;
                for (int pass = 0; pass < 2; pass++) {
                    boolean hz = pass == 0 ? horiz : !horiz;
                    if (pass == 1 && Math.max(w, h) / sh > 1.3f) break;
                    float e = ab * 0.5f;
                    if (hz) {
                        rect(c, p, 0x30FFFFFF, x0 + e, cy - bw2, x1 - e, cy);
                        rect(c, p, 0x22000000, x0 + e, cy, x1 - e, cy + bw2);
                        for (float x = x0 + e; x <= x1 - e; x += 3.5f) line(c, p, bar, 0.6f, x, cy - bw2, x, cy + bw2);
                        line(c, p, iron, 0.8f, x0 + e, cy - bw2, x1 - e, cy - bw2);
                        line(c, p, iron, 0.8f, x0 + e, cy + bw2, x1 - e, cy + bw2);
                        line(c, p, iron, 1.2f, x0 + e, cy, x1 - e, cy);
                    } else {
                        rect(c, p, 0x30FFFFFF, cx - bw2, y0 + e, cx, y1 - e);
                        rect(c, p, 0x22000000, cx, y0 + e, cx + bw2, y1 - e);
                        for (float y = y0 + e; y <= y1 - e; y += 3.5f) line(c, p, bar, 0.6f, cx - bw2, y, cx + bw2, y);
                        line(c, p, iron, 0.8f, cx - bw2, y0 + e, cx - bw2, y1 - e);
                        line(c, p, iron, 0.8f, cx + bw2, y0 + e, cx + bw2, y1 - e);
                        line(c, p, iron, 1.2f, cx, y0 + e, cx, y1 - e);
                    }
                }
                // The central dome with its lantern.
                float rd = sh * 0.38f;
                circle(c, p, 0x30000000, cx + 1.5f, cy + 2, rd + 1);
                circle(c, p, iron, cx, cy, rd + 1.2f);
                circle(c, p, 0xFF2E5636, cx, cy, rd);
                for (int k = 0; k < 9; k++) {
                    double a = r.nextFloat() * Math.PI * 2;
                    float q = r.nextFloat() * rd * 0.7f;
                    circle(c, p, pc[k % pc.length], cx + (float) Math.cos(a) * q, cy + (float) Math.sin(a) * q, 2 + r.nextFloat() * 3);
                }
                circle(c, p, 0x60CFE8E4, cx, cy, rd);
                circle(c, p, 0x2C000000, cx + rd * 0.18f, cy + rd * 0.18f, rd * 0.82f);
                circle(c, p, 0x34FFFFFF, cx - rd * 0.22f, cy - rd * 0.22f, rd * 0.6f);
                for (int k = 0; k < 16; k++) {
                    double a = k * Math.PI / 8;
                    line(c, p, bar, 0.6f, cx + (float) Math.cos(a) * rd * 0.2f, cy + (float) Math.sin(a) * rd * 0.2f, cx + (float) Math.cos(a) * rd, cy + (float) Math.sin(a) * rd);
                }
                ring(c, p, bar, 0.6f, cx, cy, rd * 0.7f);
                ring(c, p, bar, 0.6f, cx, cy, rd * 0.42f);
                circle(c, p, iron, cx, cy, rd * 0.2f);
                circle(c, p, 0xFFB8C8C4, cx + 0.4f, cy + 0.4f, rd * 0.12f);
                circle(c, p, 0xFFE0C060, cx - 0.2f, cy - 0.2f, Math.max(0.8f, rd * 0.05f));
                sign(city, "PALM HOUSE", cx, cy, 6, 0xFFF0F0E8);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.PARK | Variants.UNI | Variants.OLD | Variants.MID;
        d.minS = 10;
        d.minL = 12;
        d.base = City.OFFICE;
        d.inside = Variants.I_SHOWROOM;
        d.roofCol = 0xFFD8E8E4;
        out.add(d);
    }

    /** A sculpture on its plinth: kind 0..8. */
    private static void sculpture(Canvas c, Paint p, int kind, float x, float y, float s, Random r) {
        float pl = s * 0.62f;
        rect(c, p, 0x40000000, x - pl + 1.5f, y - pl + 2, x + pl + 1.5f, y + pl + 2);
        rect(c, p, 0xFFA8A090, x - pl, y - pl, x + pl, y + pl);
        rect(c, p, 0xFFD2CCBC, x - pl, y - pl, x + pl - 0.9f, y + pl - 0.9f);
        switch (kind) {
            case 0: // a bronze ring
                ring(c, p, 0x48000000, s * 0.2f, x + 2, y + 2.5f, s * 0.42f);
                ring(c, p, 0xFF7A5A2E, s * 0.2f, x, y, s * 0.42f);
                ring(c, p, 0xFFC09858, s * 0.07f, x - 0.4f, y - 0.4f, s * 0.42f);
                break;
            case 1: { // a steel cube balanced on a corner
                float rr = s * 0.55f;
                float[] vx = new float[6], vy = new float[6];
                for (int k = 0; k < 6; k++) {
                    double a = -Math.PI / 2 + k * Math.PI / 3;
                    vx[k] = x + (float) Math.cos(a) * rr;
                    vy[k] = y + (float) Math.sin(a) * rr;
                }
                poly(c, p, 0x48000000, vx[0] + 2.5f, vy[0] + 3, vx[1] + 2.5f, vy[1] + 3, vx[2] + 2.5f, vy[2] + 3, vx[3] + 2.5f, vy[3] + 3, vx[4] + 2.5f, vy[4] + 3, vx[5] + 2.5f, vy[5] + 3);
                poly(c, p, 0xFFC0703C, x, y, vx[4], vy[4], vx[5], vy[5], vx[0], vy[0]);
                poly(c, p, 0xFF9A5228, x, y, vx[0], vy[0], vx[1], vy[1], vx[2], vy[2]);
                poly(c, p, 0xFF6E3A1E, x, y, vx[2], vy[2], vx[3], vy[3], vx[4], vy[4]);
                break;
            }
            case 2: // a marble arch
                curve(c, p, 0x48000000, s * 0.24f, x - s * 0.45f + 2, y + s * 0.35f + 2.5f, x + 2, y - s * 0.75f + 2.5f, x + s * 0.45f + 2, y + s * 0.35f + 2.5f);
                curve(c, p, 0xFFC8C2B6, s * 0.24f, x - s * 0.45f, y + s * 0.35f, x, y - s * 0.75f, x + s * 0.45f, y + s * 0.35f);
                curve(c, p, 0xFFF0ECE4, s * 0.1f, x - s * 0.47f, y + s * 0.3f, x - 0.5f, y - s * 0.8f, x + s * 0.4f, y + s * 0.3f);
                break;
            case 3: // a mirrored sphere
                circle(c, p, 0x48000000, x + 2, y + 2.5f, s * 0.45f);
                circle(c, p, 0xFF7E8890, x, y, s * 0.45f);
                circle(c, p, 0xFFB8C2CA, x - s * 0.08f, y - s * 0.08f, s * 0.36f);
                circle(c, p, 0xFF6A9A5A, x + s * 0.12f, y + s * 0.18f, s * 0.12f);
                circle(c, p, 0xFFF4F8FA, x - s * 0.18f, y - s * 0.18f, s * 0.1f);
                break;
            case 4: // stacked stones
                oval(c, p, 0x48000000, x - s * 0.4f + 2, y - s * 0.3f + 2.5f, x + s * 0.4f + 2, y + s * 0.3f + 2.5f);
                oval(c, p, 0xFF7A746A, x - s * 0.45f, y - s * 0.32f, x + s * 0.45f, y + s * 0.32f);
                oval(c, p, 0xFF9A9284, x - s * 0.3f, y - s * 0.24f, x + s * 0.32f, y + s * 0.2f);
                oval(c, p, 0xFFB8B0A0, x - s * 0.18f, y - s * 0.16f, x + s * 0.16f, y + s * 0.1f);
                oval(c, p, 0xFFD0C8B8, x - s * 0.08f, y - s * 0.1f, x + s * 0.06f, y);
                break;
            case 5: // a red zigzag
                poly(c, p, 0x48000000, x - s * 0.5f + 2, y + s * 0.4f + 3, x - s * 0.1f + 2, y - s * 0.1f + 3, x + s * 0.05f + 2, y + s * 0.15f + 3, x + s * 0.5f + 2, y - s * 0.45f + 3, x + s * 0.2f + 2, y + s * 0.3f + 3, x + 2, y + s * 0.05f + 3);
                poly(c, p, 0xFFC0362C, x - s * 0.5f, y + s * 0.4f, x - s * 0.1f, y - s * 0.1f, x + s * 0.05f, y + s * 0.15f, x + s * 0.5f, y - s * 0.45f, x + s * 0.2f, y + s * 0.3f, x, y + s * 0.05f);
                poly(c, p, 0xFFE0584A, x - s * 0.5f, y + s * 0.4f, x - s * 0.1f, y - s * 0.1f, x - s * 0.02f, y + s * 0.02f);
                break;
            case 6: // a reclining bronze figure with a hole through it
                oval(c, p, 0x48000000, x - s * 0.5f + 2, y - s * 0.25f + 2.5f, x + s * 0.5f + 2, y + s * 0.3f + 2.5f);
                oval(c, p, 0xFF3E5E4E, x - s * 0.55f, y - s * 0.2f, x + s * 0.1f, y + s * 0.3f);
                oval(c, p, 0xFF4A6E5A, x - s * 0.1f, y - s * 0.3f, x + s * 0.5f, y + s * 0.15f);
                circle(c, p, 0xFF6A9A82, x + s * 0.25f, y - s * 0.12f, s * 0.08f);
                circle(c, p, 0xFFD2CCBC, x - s * 0.08f, y, s * 0.1f);
                break;
            case 7: // a tall steel needle
                line(c, p, 0x48000000, s * 0.2f, x, y, x + s * 1.4f, y + s * 1.7f);
                circle(c, p, 0xFFB8C0C8, x, y, s * 0.16f);
                circle(c, p, 0xFFE8ECF0, x - s * 0.04f, y - s * 0.04f, s * 0.07f);
                break;
            default: { // a blue spiral
                Path path = new Path();
                for (int k = 0; k <= 24; k++) {
                    double a = k * 0.55;
                    float rr = s * 0.05f + s * 0.42f * k / 24;
                    float px = x + (float) Math.cos(a) * rr, py = y + (float) Math.sin(a) * rr;
                    if (k == 0) path.moveTo(px, py);
                    else path.lineTo(px, py);
                }
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(s * 0.12f);
                p.setColor(0x40000000);
                c.save();
                c.translate(1.5f, 2);
                c.drawPath(path, p);
                c.restore();
                p.setColor(0xFF2E5EA0);
                c.drawPath(path, p);
                p.setStyle(Paint.Style.FILL);
                break;
            }
        }
    }

    // 5. A sculpture park: a striped lawn, curving gravel paths and abstract works on plinths.
    private static void sculpturePark(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Sculpture Park", T_PARK) {
            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                Fr f = new Fr(x0, y0, x1, y1);
                rect(c, p, 0xFF64984C, x0, y0, x1, y1);
                // Mown stripes.
                for (int i = 0; ; i++) {
                    float a = i * 12;
                    if (a > f.L) break;
                    if (i % 2 == 0) {
                        if (f.wide) rect(c, p, 0x12FFFFFF, x0 + a, y0, Math.min(x1, x0 + a + 12), y1);
                        else rect(c, p, 0x12FFFFFF, x0, y0 + a, x1, Math.min(y1, y0 + a + 12));
                    }
                }
                float[] pa = f.pts(-0.05f, 0.55f, 0.2f, 0.46f, 0.38f, 0.6f, 0.55f, 0.47f, 0.75f, 0.54f, 1.05f, 0.42f);
                float[] pb = f.pts(0.38f, -0.05f, 0.44f, 0.22f, 0.55f, 0.47f);
                float[] pc = f.pts(0.62f, 1.05f, 0.7f, 0.78f, 0.75f, 0.54f);
                float w = Math.max(6, f.S * 0.035f);
                trail(c, p, 0xFFA89C80, w + 1.6f, pa);
                trail(c, p, 0xFFA89C80, w + 1.6f, pb);
                trail(c, p, 0xFFA89C80, w + 1.6f, pc);
                trail(c, p, 0xFFD2C6A8, w, pa);
                trail(c, p, 0xFFD2C6A8, w, pb);
                trail(c, p, 0xFFD2C6A8, w, pc);
                // A paved circle where the paths meet, with the big ring.
                float mx = f.x(0.55f, 0.47f), my = f.y(0.55f, 0.47f), mr = f.S * 0.12f;
                circle(c, p, 0xFFA89C80, mx, my, mr + 1);
                circle(c, p, 0xFFDAD0B8, mx, my, mr);
                ring(c, p, 0x20000000, 0.6f, mx, my, mr * 0.7f);
                sculpture(c, p, 0, mx, my, mr * 0.9f, r);
                for (int k = 0; k < 4; k++) {
                    double a = Math.PI / 4 + k * Math.PI / 2;
                    bench(c, p, mx + (float) Math.cos(a) * (mr + 5), my + (float) Math.sin(a) * (mr + 5), k % 2 == 1);
                }
                // The works round the lawn, each a different kind.
                float[][] spots = {{0.1f, 0.22f}, {0.27f, 0.2f}, {0.66f, 0.2f}, {0.88f, 0.2f}, {0.12f, 0.8f}, {0.32f, 0.84f}, {0.5f, 0.78f}, {0.9f, 0.78f}};
                int[] kinds = {1, 2, 3, 4, 5, 6, 7, 8};
                for (int i = kinds.length - 1; i > 0; i--) {
                    int j = r.nextInt(i + 1), t = kinds[i];
                    kinds[i] = kinds[j];
                    kinds[j] = t;
                }
                float s = Math.max(6, Math.min(12, f.S * 0.05f));
                for (int i = 0; i < spots.length; i++) {
                    float sx = f.x(spots[i][0], spots[i][1]), sy = f.y(spots[i][0], spots[i][1]);
                    if (dist(pa, sx, sy) < s + w || dist(pb, sx, sy) < s + w || dist(pc, sx, sy) < s + w) continue;
                    sculpture(c, p, kinds[i], sx, sy, s * (0.85f + r.nextFloat() * 0.35f), r);
                }
                // Clumps of trees on the edges, lamps by the paths.
                float[][] tr = {{0.03f, 0.04f}, {0.18f, 0.03f}, {0.5f, 0.04f}, {0.78f, 0.05f}, {0.97f, 0.06f}, {0.03f, 0.96f},
                        {0.22f, 0.97f}, {0.72f, 0.96f}, {0.97f, 0.95f}, {0.04f, 0.4f}, {0.96f, 0.6f}};
                int[] tc = {0xFF3E7A3A, 0xFF4A8640, 0xFF35683A, 0xFF6A8A3A};
                for (int i = 0; i < tr.length; i++) {
                    float tx = f.x(tr[i][0], tr[i][1]), ty = f.y(tr[i][0], tr[i][1]);
                    if (dist(pa, tx, ty) < w + 4 || dist(pb, tx, ty) < w + 4 || dist(pc, tx, ty) < w + 4) continue;
                    tree(c, p, tx, ty, 6 + r.nextFloat() * 3, tc[i % tc.length]);
                }
                for (int i = 1; i < 6; i += 2) lamp(c, p, pa[2 * i] + w, pa[2 * i + 1] - w);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.PARK | Variants.DOWN | Variants.MID | Variants.UNI;
        d.minS = 9;
        d.minL = 10;
        out.add(d);
    }

    // 6. A skate park: concrete bowls, quarter pipes, a funbox, rails, graffiti and spectator steps.
    private static void skatePark(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Skate Park", T_PARK) {
            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                Fr f = new Fr(x0, y0, x1, y1);
                float L = f.L, S = f.S;
                rect(c, p, 0xFFB2AEA6, x0, y0, x1, y1);
                for (float x = x0 + 16; x < x1; x += 16) line(c, p, 0x22000000, 0.5f, x, y0, x, y1);
                for (float y = y0 + 16; y < y1; y += 16) line(c, p, 0x22000000, 0.5f, x0, y, x1, y);
                for (int k = 0; k < 14; k++) circle(c, p, 0x10000000, x0 + r.nextFloat() * f.w, y0 + r.nextFloat() * f.h, 3 + r.nextFloat() * 6);
                // Spectator steps along one long edge.
                float sv0 = 0.84f;
                for (int t = 0; t < 3; t++) {
                    float v0 = sv0 + t * 0.05f, v1 = v0 + 0.05f;
                    f.box(c, p, 0xFFC8C4BA, 0.06f, v0, 0.94f, v1);
                    f.box(c, p, 0xFF8E8A82, 0.06f, v0, 0.94f, v0 + 0.008f);
                }
                // Benches and a few spectators on them.
                for (int i = 0; i < 5; i++) {
                    float u = 0.15f + i * 0.17f + r.nextFloat() * 0.05f, v = sv0 + 0.025f + (i % 3) * 0.05f;
                    circle(c, p, BRIGHT[i % BRIGHT.length], f.x(u, v), f.y(u, v), 1.6f);
                    circle(c, p, 0xFFD8B090, f.x(u, v), f.y(u, v), 0.9f);
                }
                // The big kidney bowl.
                float bx = f.x(0.27f, 0.42f), by = f.y(0.27f, 0.42f), brx = f.ex(0.2f * L, 0.3f * S), bry = f.ey(0.2f * L, 0.3f * S);
                float[] k = jit(r, 10, 0.16f);
                blob(c, p, 0x40000000, bx + 1.5f, by + 2, brx, bry, k, 2.5f);
                blob(c, p, 0xFFD6D4CE, bx, by, brx, bry, k, 2.5f);
                blob(c, p, 0xFF9C988F, bx, by, brx, bry, k, 0);
                blobLine(c, p, 0xFF3A6E9A, 1.2f, bx, by, brx, bry, k, -1.2f);
                blob(c, p, 0xFF8C887F, bx + 0.5f, by + 0.5f, brx * 0.78f, bry * 0.78f, k, 0);
                blob(c, p, 0xFF7E7A72, bx + 1, by + 1, brx * 0.5f, bry * 0.5f, k, 0);
                circle(c, p, 0xFF4A4844, bx + 1, by + 1, 1.4f);
                // A smaller round bowl.
                float b2x = f.x(0.58f, 0.62f), b2y = f.y(0.58f, 0.62f), b2r = Math.min(0.11f * L, 0.18f * S);
                circle(c, p, 0x40000000, b2x + 1.5f, b2y + 2, b2r + 2.5f);
                circle(c, p, 0xFFD6D4CE, b2x, b2y, b2r + 2.5f);
                circle(c, p, 0xFF9C988F, b2x, b2y, b2r);
                ring(c, p, 0xFFB8483A, 1, b2x, b2y, b2r - 1.2f);
                circle(c, p, 0xFF8A867E, b2x + 0.5f, b2y + 0.5f, b2r * 0.72f);
                circle(c, p, 0xFF7C786F, b2x + 1, b2y + 1, b2r * 0.42f);
                circle(c, p, 0xFF4A4844, b2x + 1, b2y + 1, 1.2f);
                // A quarter pipe along the far end: bands from the flat to the deck, coping and a railing.
                float qd = Math.max(10, L * 0.06f), qx0 = 0.96f * L - qd, dk = 4;
                for (int i = 0; i < 5; i++) {
                    float a0 = qx0 + qd * i / 5, a1 = qx0 + qd * (i + 1) / 5;
                    f.box(c, p, shade(0xFFA8A49C, 0.82f + i * 0.06f), a0 / L, 0.08f, a1 / L, 0.72f);
                }
                f.box(c, p, 0xFFE4E4E0, (qx0 + qd) / L, 0.08f, (qx0 + qd + 1.2f) / L, 0.72f);
                f.box(c, p, 0xFFC4C0B8, (qx0 + qd + 1.2f) / L, 0.08f, (qx0 + qd + 1.2f + dk) / L, 0.72f);
                f.box(c, p, 0xFF4A4C50, (qx0 + qd + dk) / L, 0.08f, (qx0 + qd + dk + 0.8f) / L, 0.72f);
                // A funbox with a rail on top.
                float fu = 0.6f, fv = 0.24f, fl = 0.13f * L, fw = 0.12f * S, fs = Math.min(fl, fw) * 0.3f;
                float fx = f.x(fu, fv), fy = f.y(fu, fv), hx = f.ex(fl, fw) / 2, hy = f.ey(fl, fw) / 2;
                rect(c, p, 0x40000000, fx - hx + 1.5f, fy - hy + 2, fx + hx + 1.5f, fy + hy + 2);
                poly(c, p, 0xFFC4C0B8, fx - hx, fy - hy, fx + hx, fy - hy, fx + hx - fs, fy - hy + fs, fx - hx + fs, fy - hy + fs);
                poly(c, p, 0xFF8E8A82, fx - hx, fy + hy, fx + hx, fy + hy, fx + hx - fs, fy + hy - fs, fx - hx + fs, fy + hy - fs);
                poly(c, p, 0xFFB4B0A8, fx - hx, fy - hy, fx - hx + fs, fy - hy + fs, fx - hx + fs, fy + hy - fs, fx - hx, fy + hy);
                poly(c, p, 0xFF9A968E, fx + hx, fy - hy, fx + hx - fs, fy - hy + fs, fx + hx - fs, fy + hy - fs, fx + hx, fy + hy);
                rect(c, p, 0xFFBCB8B0, fx - hx + fs, fy - hy + fs, fx + hx - fs, fy + hy - fs);
                if (hx >= hy) line(c, p, 0xFF3A3C40, 1.1f, fx - hx + fs, fy, fx + hx - fs, fy);
                else line(c, p, 0xFF3A3C40, 1.1f, fx, fy - hy + fs, fx, fy + hy - fs);
                // Rails and a ledge.
                float[][] rails = {{0.42f, 0.12f, 0.5f, 0.12f}, {0.72f, 0.42f, 0.8f, 0.42f}, {0.45f, 0.72f, 0.45f, 0.8f}};
                for (float[] q : rails) {
                    float ax = f.x(q[0], q[1]), ay = f.y(q[0], q[1]), ex = f.x(q[2], q[3]), ey = f.y(q[2], q[3]);
                    line(c, p, 0x40000000, 1.2f, ax + 1, ay + 1.5f, ex + 1, ey + 1.5f);
                    line(c, p, 0xFF5A5E64, 1.1f, ax, ay, ex, ey);
                    line(c, p, 0xFFB8BEC4, 0.4f, ax, ay - 0.3f, ex, ey - 0.3f);
                    circle(c, p, 0xFF3A3C40, ax, ay, 0.9f);
                    circle(c, p, 0xFF3A3C40, ex, ey, 0.9f);
                }
                float lx = f.x(0.66f, 0.1f), ly = f.y(0.66f, 0.1f), lex = f.ex(0.14f * L, 4), ley = f.ey(0.14f * L, 4);
                rect(c, p, 0x40000000, lx + 1.2f, ly + 1.6f, lx + lex + 1.2f, ly + ley + 1.6f);
                rect(c, p, 0xFFCAC6BC, lx, ly, lx + lex, ly + ley);
                frame(c, p, 0xFF6A6E74, 0.6f, lx, ly, lx + lex, ly + ley);
                // Graffiti tags over the walls and the floor.
                for (int i = 0; i < 12; i++) {
                    float gx, gy;
                    if (i < 4) {
                        gx = f.x((qx0 + qd * 0.5f) / L, 0.15f + i * 0.15f);
                        gy = f.y((qx0 + qd * 0.5f) / L, 0.15f + i * 0.15f);
                    } else if (i < 8) {
                        double a = r.nextFloat() * Math.PI * 2;
                        gx = bx + (float) Math.cos(a) * brx * 0.88f;
                        gy = by + (float) Math.sin(a) * bry * 0.88f;
                    } else {
                        gx = x0 + 8 + r.nextFloat() * (f.w - 16);
                        gy = y0 + 8 + r.nextFloat() * (f.h - 16);
                    }
                    int col = BRIGHT[r.nextInt(BRIGHT.length)] & 0xC8FFFFFF;
                    float gs = 3 + r.nextFloat() * 3;
                    if (i % 3 == 0) {
                        float[] gk = jit(r, 6, 0.3f);
                        blob(c, p, col, gx, gy, gs, gs * 0.6f, gk, 0);
                        blobLine(c, p, 0xC0202020, 0.5f, gx, gy, gs, gs * 0.6f, gk, 0);
                    }
                    float[] pts = new float[10];
                    for (int q = 0; q < 5; q++) {
                        pts[2 * q] = gx - gs + q * gs * 0.5f;
                        pts[2 * q + 1] = gy + (r.nextFloat() - 0.5f) * gs * 1.2f;
                    }
                    trail(c, p, i % 3 == 0 ? 0xD0F0F0F0 : col, 0.9f, pts);
                }
                // A chain-link fence and trees in planters at the corners.
                frame(c, p, 0xFF7A7E84, 0.8f, x0 + 1.5f, y0 + 1.5f, x1 - 1.5f, y1 - 1.5f);
                float[][] pl = {{x0 + 7, y0 + 7}, {x1 - 7, y1 - 7}, {x0 + 7, y1 - 7}};
                for (float[] q : pl) {
                    rect(c, p, 0xFF8A867E, q[0] - 5, q[1] - 5, q[0] + 5, q[1] + 5);
                    rect(c, p, 0xFF5A4A3A, q[0] - 4, q[1] - 4, q[0] + 4, q[1] + 4);
                    tree(c, p, q[0], q[1], 5, 0xFF4A8640);
                }
                lamp(c, p, f.x(0.03f, 0.5f), f.y(0.03f, 0.5f));
                lamp(c, p, f.x(0.5f, 0.03f), f.y(0.5f, 0.03f));
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.PARK | Variants.SUB | Variants.MID | Variants.IND;
        d.minS = 8;
        d.minL = 10;
        out.add(d);
    }

    /** One allotment's crop over its soil. */
    private static void crop(Canvas c, Paint p, int kind, float x0, float y0, float x1, float y1, Random r) {
        float w = x1 - x0, h = y1 - y0;
        boolean hz = w >= h;
        float len = hz ? w : h, th = hz ? h : w;
        int rows = Math.max(2, Math.min(5, (int) (th / 4.5f)));
        int per = Math.max(3, Math.min(8, (int) (len / 4.5f)));
        for (int i = 0; i < rows; i++) {
            float t = (i + 0.5f) / rows;
            float ax = hz ? x0 + 1 : x0 + w * t, ay = hz ? y0 + h * t : y0 + 1, bx = hz ? x1 - 1 : x0 + w * t, by = hz ? y0 + h * t : y1 - 1;
            switch (kind) {
                case 0: // cabbages
                    for (int j = 0; j < per; j++) {
                        float q = (j + 0.5f) / per, cx = ax + (bx - ax) * q, cy = ay + (by - ay) * q;
                        circle(c, p, 0xFF5E8E6A, cx, cy, 1.6f);
                        circle(c, p, 0xFF8ABA8A, cx - 0.3f, cy - 0.3f, 0.8f);
                    }
                    break;
                case 1: // carrots: feathery rows
                    line(c, p, 0xFF6AAA48, 1.6f, ax, ay, bx, by);
                    line(c, p, 0xFF8ACA5A, 0.5f, ax, ay - 0.3f, bx, by - 0.3f);
                    break;
                case 2: // beans up canes
                    line(c, p, 0xFF8A6A3A, 0.5f, ax, ay, bx, by);
                    for (int j = 0; j < per; j++) {
                        float q = (j + 0.5f) / per, cx = ax + (bx - ax) * q, cy = ay + (by - ay) * q;
                        oval(c, p, 0xFF3E7A36, cx - 1.3f, cy - 1.1f, cx + 1.3f, cy + 1.1f);
                        if (j % 2 == 0) circle(c, p, 0xFFD84A3A, cx + 0.5f, cy, 0.4f);
                    }
                    break;
                case 3: // potatoes in ridges
                    line(c, p, 0xFF7E5E40, 2.4f, ax, ay, bx, by);
                    for (int j = 0; j < per; j++) {
                        float q = (j + 0.5f) / per;
                        circle(c, p, 0xFF4E8A40, ax + (bx - ax) * q, ay + (by - ay) * q, 1.3f);
                    }
                    break;
                case 4: // sunflowers
                    if (i % 2 == 0) for (int j = 0; j < per; j += 2) {
                        float q = (j + 0.5f) / per, cx = ax + (bx - ax) * q, cy = ay + (by - ay) * q;
                        circle(c, p, 0x40000000, cx + 0.8f, cy + 1, 2.2f);
                        circle(c, p, 0xFFE8C030, cx, cy, 2.2f);
                        circle(c, p, 0xFF5A3A1E, cx, cy, 1);
                    }
                    break;
                case 5: // pumpkins sprawling
                    if (i % 2 == 0) for (int j = 0; j < per; j += 2) {
                        float q = (j + 0.5f) / per, cx = ax + (bx - ax) * q, cy = ay + (by - ay) * q;
                        circle(c, p, 0xFF4E8A40, cx - 1, cy - 0.6f, 2.4f);
                        circle(c, p, 0xFFE07A20, cx + 0.8f, cy + 0.6f, 1.4f);
                    }
                    break;
                case 6: // dug over: furrows
                    line(c, p, 0xFF4E3826, 0.8f, ax, ay, bx, by);
                    break;
                case 7: // strawberries
                    for (int j = 0; j < per; j++) {
                        float q = (j + 0.5f) / per, cx = ax + (bx - ax) * q, cy = ay + (by - ay) * q;
                        circle(c, p, 0xFF4E8A40, cx, cy, 1.2f);
                        if ((i + j) % 2 == 0) circle(c, p, 0xFFD02A2A, cx + 0.5f, cy + 0.4f, 0.5f);
                    }
                    break;
                default: // onions and leeks
                    for (int j = 0; j < per * 2; j++) {
                        float q = (j + 0.5f) / (per * 2), cx = ax + (bx - ax) * q, cy = ay + (by - ay) * q;
                        line(c, p, 0xFF7AA87A, 0.5f, cx - 0.6f, cy - 0.6f, cx + 0.6f, cy + 0.6f);
                    }
                    break;
            }
        }
        if (kind == 7) {
            // Netting over the strawberries.
            frame(c, p, 0x80FFFFFF, 0.4f, x0 + 0.5f, y0 + 0.5f, x1 - 0.5f, y1 - 0.5f);
            for (float x = x0 + 2.5f; x < x1; x += 2.5f) line(c, p, 0x50FFFFFF, 0.3f, x, y0, x, y1);
            for (float y = y0 + 2.5f; y < y1; y += 2.5f) line(c, p, 0x50FFFFFF, 0.3f, x0, y, x1, y);
        }
    }

    // 7. Community allotments: plots of every crop, a polytunnel, sheds, water butts and compost bins.
    private static void allotments(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Community Allotments", T_PARK) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int bL = Math.max(bw, bh), bS = Math.min(bw, bh), n = 0;
                int[][] o = new int[4][];
                for (int u = 1; u + 2 <= bL - 1 && n < 4; u += 5) {
                    o[n] = tb(bw, bh, u, n % 2 == 0 ? 1 : bS - 3, 2, 2, 1);
                    n++;
                }
                int[][] res = new int[n][];
                System.arraycopy(o, 0, res, 0, n);
                return res;
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                Fr f = new Fr(x0, y0, x1, y1);
                int[][] pl = plan(Math.round(f.w / T), Math.round(f.h / T), null);
                float[][] sheds = new float[pl.length][];
                for (int i = 0; i < pl.length; i++) sheds[i] = part(pl, i, x0, y0);
                rect(c, p, 0xFF6E9A50, x0, y0, x1, y1);
                for (int k = 0; k < 20; k++) circle(c, p, 0x14000000, x0 + r.nextFloat() * f.w, y0 + r.nextFloat() * f.h, 3 + r.nextFloat() * 5);
                // A wide central track along the long side.
                f.box(c, p, 0xFFA8946E, 0, 0.47f, 1, 0.53f);
                for (int k = 0; k < 10; k++) {
                    float u = r.nextFloat();
                    f.box(c, p, 0x20000000, u, 0.485f, u + 0.03f, 0.495f);
                }
                int nu = Math.max(2, Math.min(5, Math.round(f.L / 56))), nv = Math.max(1, Math.min(2, Math.round(f.S / 90)));
                float gap = 4;
                for (int half = 0; half < 2; half++) {
                    float va = half == 0 ? 0 : 0.53f, vb = half == 0 ? 0.47f : 1;
                    for (int iu = 0; iu < nu; iu++) {
                        for (int iv = 0; iv < nv; iv++) {
                            float u0 = (float) iu / nu, u1 = (float) (iu + 1) / nu, v0 = va + (vb - va) * iv / nv, v1 = va + (vb - va) * (iv + 1) / nv;
                            float px0 = f.x(u0, v0) + gap, py0 = f.y(u0, v0) + gap, px1 = f.x(u1, v1) - gap, py1 = f.y(u1, v1) - gap;
                            // Shed yards: grass, a water butt and a compost bin.
                            boolean yard = false;
                            for (float[] s : sheds) if (s[0] < px1 + 2 && s[2] > px0 - 2 && s[1] < py1 + 2 && s[3] > py0 - 2) yard = true;
                            if (yard) {
                                for (float[] s : sheds) {
                                    if (!(s[0] < px1 + 2 && s[2] > px0 - 2 && s[1] < py1 + 2 && s[3] > py0 - 2)) continue;
                                    float wx = s[2] + 3.5f, wy = s[1] + 3;
                                    if (wx > x1 - 3) wx = s[0] - 3.5f;
                                    circle(c, p, 0x40000000, wx + 0.8f, wy + 1, 2.4f);
                                    circle(c, p, 0xFF2E5A3A, wx, wy, 2.4f);
                                    circle(c, p, 0xFF3A6E8A, wx, wy, 1.6f);
                                    float cx = s[2] + 2, cy = s[3] - 6;
                                    if (cx > x1 - 7) cx = s[0] - 7;
                                    rect(c, p, 0xFF5A3E26, cx, cy, cx + 5, cy + 5);
                                    rect(c, p, 0xFF3E2A1A, cx + 0.8f, cy + 0.8f, cx + 4.2f, cy + 4.2f);
                                    for (int q = 0; q < 3; q++) circle(c, p, 0xFF6A8A3A, cx + 1.5f + q * 1.1f, cy + 2 + (q % 2), 0.6f);
                                }
                                continue;
                            }
                            rect(c, p, 0x38000000, px0 + 0.6f, py0 + 0.8f, px1 + 0.6f, py1 + 0.8f);
                            rect(c, p, 0xFF8A6E4A, px0, py0, px1, py1);
                            rect(c, p, 0xFF6A4E36, px0 + 0.8f, py0 + 0.8f, px1 - 0.8f, py1 - 0.8f);
                            int kind = r.nextInt(10);
                            if (kind == 9) {
                                // A polytunnel.
                                float in = 1.5f;
                                roundRect(c, p, 0x40000000, px0 + in + 1, py0 + in + 1.4f, px1 - in + 1, py1 - in + 1.4f, 3);
                                roundRect(c, p, 0xE8E8ECE4, px0 + in, py0 + in, px1 - in, py1 - in, 3);
                                boolean hz = px1 - px0 >= py1 - py0;
                                if (hz) {
                                    rect(c, p, 0x20000000, px0 + in, (py0 + py1) / 2, px1 - in, py1 - in);
                                    for (float x = px0 + in + 3; x < px1 - in; x += 3.5f) line(c, p, 0x30000000, 0.5f, x, py0 + in, x, py1 - in);
                                } else {
                                    rect(c, p, 0x20000000, (px0 + px1) / 2, py0 + in, px1 - in, py1 - in);
                                    for (float y = py0 + in + 3; y < py1 - in; y += 3.5f) line(c, p, 0x30000000, 0.5f, px0 + in, y, px1 - in, y);
                                }
                            } else {
                                crop(c, p, kind, px0 + 0.8f, py0 + 0.8f, px1 - 0.8f, py1 - 0.8f, r);
                            }
                            if (r.nextInt(3) == 0) {
                                circle(c, p, 0xFF2E5A3A, px1 - 1.5f, py0 + 1.5f, 1.8f);
                                circle(c, p, 0xFF3A6E8A, px1 - 1.5f, py0 + 1.5f, 1.1f);
                            }
                        }
                    }
                }
                // A scarecrow, a wheelbarrow and a hedge round it all.
                float sx = f.x(0.4f, 0.25f), sy = f.y(0.4f, 0.25f);
                line(c, p, 0xFF6E5034, 0.8f, sx - 3, sy, sx + 3, sy);
                circle(c, p, 0xFF4A6A9A, sx, sy, 1.5f);
                circle(c, p, 0xFFC8A858, sx, sy, 1);
                float wx = f.x(0.7f, 0.5f), wy = f.y(0.7f, 0.5f);
                rect(c, p, 0xFF3E6E9A, wx - 2, wy - 1.4f, wx + 2, wy + 1.4f);
                circle(c, p, 0xFF202020, wx + 2.6f, wy, 0.9f);
                frame(c, p, 0xFF3E6E34, 2.2f, x0 + 1.1f, y0 + 1.1f, x1 - 1.1f, y1 - 1.1f);
                sign(city, "ALLOTMENTS", f.x(0.5f, 0.5f), f.y(0.5f, 0.5f), 5, 0xFFF0E8D8);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                int[] cols = {0xFF4E6E4A, 0xFF4A5E7A, 0xFF6E5038, 0xFF8A4636};
                int col = cols[part % cols.length];
                // A felt mono-pitch roof with battens, higher at the top.
                rect(c, p, City.darken(col, 0.6f), x0, y0, x1, y1);
                rect(c, p, col, x0 + 0.8f, y0 + 0.8f, x1 - 0.8f, y1 - 0.8f);
                rect(c, p, City.lighten(col, 0.2f), x0 + 0.8f, y0 + 0.8f, x1 - 0.8f, y0 + 2.5f);
                for (float x = x0 + 4; x < x1 - 1; x += 4) line(c, p, City.darken(col, 0.75f), 0.6f, x, y0 + 1, x, y1 - 1);
                rect(c, p, 0x50FFFFFF, x0 + (x1 - x0) * 0.6f, y0 + (y1 - y0) * 0.45f, x0 + (x1 - x0) * 0.85f, y0 + (y1 - y0) * 0.7f);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.SUB | Variants.PARK | Variants.OLD;
        d.minS = 9;
        d.minL = 11;
        d.base = City.KIOSK;
        d.inside = Variants.I_SHELVES;
        d.roofCol = 0xFF4E6E4A;
        out.add(d);
    }

    // 8. A dog park: fenced runs (a small-dog pen too), an agility course, water bowls, benches, trees and dogs.
    private static void dogPark(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Dog Park", T_PARK) {
            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                Fr f = new Fr(x0, y0, x1, y1);
                float L = f.L, S = f.S;
                rect(c, p, 0xFF6A9C4E, x0, y0, x1, y1);
                rect(c, p, 0xFFC4B898, x0, y0, x1, y0 + 0);
                // Worn patches where the dogs run.
                for (int k = 0; k < 10; k++) {
                    float px = x0 + r.nextFloat() * f.w, py = y0 + r.nextFloat() * f.h;
                    blob(c, p, 0x40A08A5A, px, py, 6 + r.nextFloat() * 8, 4 + r.nextFloat() * 5, jit(r, 7, 0.25f), 0);
                }
                // A path round the outside.
                frame(c, p, 0xFFCCBF9E, 5, x0 + 2.5f, y0 + 2.5f, x1 - 2.5f, y1 - 2.5f);
                float fx0 = x0 + 6, fy0 = y0 + 6, fx1 = x1 - 6, fy1 = y1 - 6;
                // The small-dog pen at one end.
                float du = 0.72f;
                float dx = f.x(du, 0), dy = f.y(du, 0);
                rect(c, p, 0x20FFFFFF, f.wide ? dx : fx0, f.wide ? fy0 : dy, fx1, fy1);
                fence(c, p, 0xFF8A8E92, fx0, fy0, fx1, fy1, 8);
                if (f.wide) line(c, p, 0xFF8A8E92, 0.7f, dx, fy0, dx, fy1);
                else line(c, p, 0xFF8A8E92, 0.7f, fx0, dy, fx1, dy);
                // Double gates: gaps with the gate leaves.
                float gx = f.x(0.4f, 1), gy = f.y(0.4f, 1);
                if (f.wide) {
                    rect(c, p, 0xFFCCBF9E, gx - 4, fy1 - 1, gx + 4, fy1 + 1);
                    line(c, p, 0xFF5A5E62, 0.8f, gx - 4, fy1, gx - 1, fy1 - 3);
                } else {
                    rect(c, p, 0xFFCCBF9E, fx1 - 1, gy - 4, fx1 + 1, gy + 4);
                    line(c, p, 0xFF5A5E62, 0.8f, fx1, gy - 4, fx1 - 3, gy - 1);
                }
                // The agility course: a tunnel.
                float[] tun = f.pts(0.08f, 0.25f, 0.18f, 0.12f, 0.3f, 0.2f);
                trail(c, p, 0x40000000, 6.5f, new float[]{tun[0] + 1, tun[1] + 1.5f, tun[2] + 1, tun[3] + 1.5f, tun[4] + 1, tun[5] + 1.5f});
                trail(c, p, 0xFF2A4E8A, 6, tun);
                trail(c, p, 0xFF3E6EB0, 3.4f, tun);
                for (int i = 0; i < 9; i++) {
                    float t = i / 8f, a = (1 - t) * (1 - t), b = 2 * (1 - t) * t, e = t * t;
                    float qx = a * tun[0] + b * tun[2] + e * tun[4], qy = a * tun[1] + b * tun[3] + e * tun[5];
                    circle(c, p, 0x30000000, qx, qy, 1.2f);
                }
                circle(c, p, 0xFF1A2A40, tun[0], tun[1], 2.4f);
                circle(c, p, 0xFF1A2A40, tun[4], tun[5], 2.4f);
                // Jumps.
                for (int i = 0; i < 3; i++) {
                    float u = 0.12f + i * 0.17f, v = 0.48f + (i % 2) * 0.08f;
                    float jx = f.x(u, v), jy = f.y(u, v), ha = 5;
                    float ax = jx - f.ex(0, ha), ay = jy - f.ey(0, ha), bx = jx + f.ex(0, ha), by = jy + f.ey(0, ha);
                    line(c, p, 0x40000000, 1, ax + 1, ay + 1.4f, bx + 1, by + 1.4f);
                    line(c, p, 0xFFF0F0F0, 0.9f, ax, ay, bx, by);
                    for (int q = 0; q < 3; q++) {
                        float t0 = 0.15f + q * 0.3f, t1 = t0 + 0.15f;
                        line(c, p, 0xFFD03A2E, 0.9f, ax + (bx - ax) * t0, ay + (by - ay) * t0, ax + (bx - ax) * t1, ay + (by - ay) * t1);
                    }
                    rect(c, p, 0xFF2E5EA8, ax - 1, ay - 1, ax + 1, ay + 1);
                    rect(c, p, 0xFF2E5EA8, bx - 1, by - 1, bx + 1, by + 1);
                }
                // Weave poles.
                for (int i = 0; i < 8; i++) {
                    float u = 0.12f + i * 0.045f, v = 0.82f;
                    float wx = f.x(u, v), wy = f.y(u, v);
                    if (i == 0) {
                        float ex = f.x(u + 7 * 0.045f, v), ey = f.y(u + 7 * 0.045f, v);
                        line(c, p, 0xFF8A8E92, 0.6f, wx, wy, ex, ey);
                    }
                    circle(c, p, 0x40000000, wx + 0.6f, wy + 0.8f, 0.9f);
                    circle(c, p, i % 2 == 0 ? 0xFFE8C030 : 0xFF2E5EA8, wx, wy, 0.9f);
                }
                // An A-frame.
                float au = 0.5f, av = 0.2f, al = 0.12f * L, aw = 6;
                float ax = f.x(au, av), ay = f.y(au, av), ahx = f.ex(al, aw) / 2, ahy = f.ey(al, aw) / 2;
                rect(c, p, 0x40000000, ax - ahx + 1.5f, ay - ahy + 2, ax + ahx + 1.5f, ay + ahy + 2);
                if (f.wide) {
                    rect(c, p, 0xFF5A8AC0, ax - ahx, ay - ahy, ax, ay + ahy);
                    rect(c, p, 0xFF3E6A9A, ax, ay - ahy, ax + ahx, ay + ahy);
                    rect(c, p, 0xFFE8C030, ax - ahx, ay - ahy, ax - ahx * 0.7f, ay + ahy);
                    rect(c, p, 0xFFC8A020, ax + ahx * 0.7f, ay - ahy, ax + ahx, ay + ahy);
                } else {
                    rect(c, p, 0xFF5A8AC0, ax - ahx, ay - ahy, ax + ahx, ay);
                    rect(c, p, 0xFF3E6A9A, ax - ahx, ay, ax + ahx, ay + ahy);
                    rect(c, p, 0xFFE8C030, ax - ahx, ay - ahy, ax + ahx, ay - ahy * 0.7f);
                    rect(c, p, 0xFFC8A020, ax - ahx, ay + ahy * 0.7f, ax + ahx, ay + ahy);
                }
                // A tyre jump and a pause table.
                float tx = f.x(0.58f, 0.5f), ty = f.y(0.58f, 0.5f);
                ring(c, p, 0x40000000, 1.6f, tx + 1, ty + 1.4f, 3);
                ring(c, p, 0xFF202020, 1.6f, tx, ty, 3);
                ring(c, p, 0xFFE8C030, 0.5f, tx, ty, 3);
                float px = f.x(0.56f, 0.78f), py = f.y(0.56f, 0.78f);
                rect(c, p, 0x40000000, px - 3, py - 2.5f, px + 4.5f, py + 4.5f);
                rect(c, p, 0xFFD03A2E, px - 3.5f, py - 3.5f, px + 3.5f, py + 3.5f);
                rect(c, p, 0xFFE05A4A, px - 2.5f, py - 2.5f, px + 2.5f, py + 2.5f);
                // In the small pen: a shade sail, a little ramp, water bowls and toys.
                float su = (du + 1) / 2;
                float sx = f.x(su, 0.3f), sy = f.y(su, 0.3f), sr = Math.min(0.1f * L, 0.18f * S);
                poly(c, p, 0x30000000, sx - sr + 3, sy - sr * 0.6f + 4, sx + sr + 3, sy - sr * 0.3f + 4, sx + 3, sy + sr + 4);
                poly(c, p, 0xD8F0EEE6, sx - sr, sy - sr * 0.6f, sx + sr, sy - sr * 0.3f, sx, sy + sr);
                poly(c, p, 0x20000000, sx + sr, sy - sr * 0.3f, sx, sy + sr, sx + sr * 0.1f, sy);
                float rx2 = f.x(su, 0.72f), ry2 = f.y(su, 0.72f);
                rect(c, p, 0xFF9A7A54, rx2 - 4, ry2 - 2.5f, rx2 + 4, ry2 + 2.5f);
                rect(c, p, 0xFFB8946A, rx2 - 1.5f, ry2 - 2.5f, rx2 + 1.5f, ry2 + 2.5f);
                float[][] bowls = {{0.68f, 0.9f}, {0.76f, 0.9f}, {0.94f, 0.55f}};
                for (float[] q : bowls) {
                    float bx = f.x(q[0], q[1]), by = f.y(q[0], q[1]);
                    circle(c, p, 0xFF9AA0A6, bx, by, 1.8f);
                    circle(c, p, 0xFF4A8AC0, bx, by, 1.1f);
                }
                float tpx = f.x(0.7f, 0.86f), tpy = f.y(0.7f, 0.86f);
                rect(c, p, 0xFF5A5E64, tpx - 0.8f, tpy - 0.8f, tpx + 0.8f, tpy + 0.8f);
                for (int i = 0; i < 5; i++) circle(c, p, i % 2 == 0 ? 0xFFD8E040 : 0xFFE04A3A, x0 + 10 + r.nextFloat() * (f.w - 20), y0 + 10 + r.nextFloat() * (f.h - 20), 0.7f);
                // Trees with benches under them, bag bins.
                float[][] tr = {{0.05f, 0.08f}, {0.42f, 0.06f}, {0.66f, 0.94f}, {0.05f, 0.94f}, {0.95f, 0.1f}, {0.3f, 0.94f}};
                int[] tc = {0xFF3E7A3A, 0xFF4A8640, 0xFF35683A};
                for (int i = 0; i < tr.length; i++) {
                    float qx = f.x(tr[i][0], tr[i][1]), qy = f.y(tr[i][0], tr[i][1]);
                    tree(c, p, qx, qy, 7 + r.nextFloat() * 2, tc[i % tc.length]);
                }
                bench(c, p, f.x(0.3f, 0.06f), f.y(0.3f, 0.06f) + (f.wide ? 4 : 0), f.wide);
                bench(c, p, f.x(0.18f, 0.94f), f.y(0.18f, 0.94f) - (f.wide ? 4 : 0), f.wide);
                bench(c, p, f.x(0.86f, 0.94f), f.y(0.86f, 0.94f) - (f.wide ? 4 : 0), f.wide);
                rect(c, p, 0xFF2E6A3A, f.x(0.36f, 0.94f) - 1.5f, f.y(0.36f, 0.94f) - 1.5f, f.x(0.36f, 0.94f) + 1.5f, f.y(0.36f, 0.94f) + 1.5f);
                // The dogs.
                int[] dc = {0xFF8A5A30, 0xFF2A2420, 0xFFE8D8B8, 0xFFB88A50, 0xFF6A6460, 0xFFF0EEE8, 0xFFC09060};
                for (int i = 0; i < 7; i++) {
                    float u = i < 5 ? 0.06f + r.nextFloat() * 0.6f : du + 0.05f + r.nextFloat() * 0.2f, v = 0.1f + r.nextFloat() * 0.8f;
                    dog(c, p, f.x(u, v), f.y(u, v), r.nextFloat() * 360, dc[i], i < 5 ? 4.5f + r.nextFloat() * 1.5f : 3);
                }
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.PARK | Variants.SUB | Variants.MID;
        d.minS = 8;
        d.minL = 10;
        out.add(d);
    }

    // 9. A splash pad playground: jets and a tipping bucket, climbing frame, slides, swings, sandpit and a kiosk.
    private static void splashPad(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Splash Pad Playground", T_PARK) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int bL = Math.max(bw, bh), bS = Math.min(bw, bh);
                return new int[][]{tb(bw, bh, bL - 4, bS - 3, 3, 2, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                Fr f = new Fr(x0, y0, x1, y1);
                float L = f.L, S = f.S;
                float[] kb = part(plan(Math.round(f.w / T), Math.round(f.h / T), null), 0, x0, y0);
                rect(c, p, 0xFF66984C, x0, y0, x1, y1);
                frame(c, p, 0xFFCCC2A8, 5, x0 + 2.5f, y0 + 2.5f, x1 - 2.5f, y1 - 2.5f);
                // The splash pad.
                float sx = f.x(0.27f, 0.47f), sy = f.y(0.27f, 0.47f), sr = Math.min(0.22f * L, 0.38f * S);
                circle(c, p, 0x30000000, sx + 1, sy + 1.5f, sr + 1.5f);
                circle(c, p, 0xFFB8B4AA, sx, sy, sr + 1.5f);
                circle(c, p, 0xFFDADAD2, sx, sy, sr);
                int[] rc = {0xFF4AA0D8, 0xFFE8C040, 0xFF3EB0A0, 0xFFE07A5A};
                for (int k = 0; k < 4; k++) ring(c, p, rc[k], sr * 0.08f, sx, sy, sr * (0.85f - k * 0.18f));
                blob(c, p, 0x404AA0D8, sx + sr * 0.15f, sy - sr * 0.1f, sr * 0.6f, sr * 0.45f, jit(r, 8, 0.25f), 0);
                blob(c, p, 0x304AA0D8, sx - sr * 0.3f, sy + sr * 0.3f, sr * 0.4f, sr * 0.3f, jit(r, 8, 0.25f), 0);
                // Jets in a ring, each throwing spray.
                for (int k = 0; k < 10; k++) {
                    double a = k * Math.PI / 5;
                    float jx = sx + (float) Math.cos(a) * sr * 0.68f, jy = sy + (float) Math.sin(a) * sr * 0.68f;
                    circle(c, p, 0x80FFFFFF, jx, jy, 2.2f);
                    circle(c, p, 0xC0FFFFFF, jx, jy, 1.1f);
                    circle(c, p, 0xFF606A72, jx, jy, 0.4f);
                    for (int q = 0; q < 3; q++) circle(c, p, 0x90E8F4FC, jx + (r.nextFloat() - 0.5f) * 5, jy + (r.nextFloat() - 0.5f) * 5, 0.5f);
                }
                // The tipping bucket tower in the middle.
                float tw = sr * 0.22f;
                rect(c, p, 0x40000000, sx - tw + 1.5f, sy - tw + 2, sx + tw + 1.5f, sy + tw + 2);
                rect(c, p, 0xFF2E7AC0, sx - tw, sy - tw, sx + tw, sy + tw);
                rect(c, p, 0xFF4A96D8, sx - tw + 1, sy - tw + 1, sx + tw - 1, sy + tw - 1);
                circle(c, p, 0xFFE0602E, sx + tw * 0.4f, sy - tw * 0.4f, tw * 0.6f);
                circle(c, p, 0xFFF08A4A, sx + tw * 0.3f, sy - tw * 0.5f, tw * 0.35f);
                circle(c, p, 0x90FFFFFF, sx + tw * 0.9f, sy + tw * 0.2f, tw * 0.5f);
                // Safety surfacing for the play equipment.
                float pu0 = 0.53f, pv0 = 0.07f, pu1 = 0.95f, pv1 = 0.66f;
                float qx0 = f.x(pu0, pv0), qy0 = f.y(pu0, pv0), qx1 = f.x(pu1, pv1), qy1 = f.y(pu1, pv1);
                roundRect(c, p, 0xFF8A4A3E, qx0 - 1, qy0 - 1, qx1 + 1, qy1 + 1, 6);
                roundRect(c, p, 0xFFB45A4A, qx0, qy0, qx1, qy1, 5);
                blob(c, p, 0xFF4E8A5A, f.x(0.82f, 0.45f), f.y(0.82f, 0.45f), f.ex(0.08f * L, 0.12f * S), f.ey(0.08f * L, 0.12f * S), jit(r, 8, 0.2f), 0);
                blob(c, p, 0xFF3E7AB0, f.x(0.62f, 0.5f), f.y(0.62f, 0.5f), f.ex(0.05f * L, 0.09f * S), f.ey(0.05f * L, 0.09f * S), jit(r, 8, 0.2f), 0);
                // Climbing frame: three towers with pointed roofs, bridges, slides and a net.
                float[][] tp = {{0.62f, 0.2f}, {0.76f, 0.2f}, {0.76f, 0.4f}};
                float ts = Math.max(6, Math.min(9, S * 0.05f));
                for (int i = 0; i < 2; i++) {
                    float ax = f.x(tp[i][0], tp[i][1]), ay = f.y(tp[i][0], tp[i][1]), bx = f.x(tp[i + 1][0], tp[i + 1][1]), by = f.y(tp[i + 1][0], tp[i + 1][1]);
                    line(c, p, 0x40000000, 3.5f, ax + 1.5f, ay + 2, bx + 1.5f, by + 2);
                    line(c, p, 0xFF9A7A54, 3, ax, ay, bx, by);
                    line(c, p, 0xFF5A5E64, 0.4f, ax, ay, bx, by);
                }
                // Slides out from the towers.
                float s0x = f.x(tp[0][0], tp[0][1]), s0y = f.y(tp[0][0], tp[0][1]);
                float s1x = s0x - f.ex(0, -1) * 0 + (f.wide ? 0 : ts + 10), s1y = s0y + (f.wide ? ts + 10 : 0);
                line(c, p, 0x40000000, 3.6f, s0x + 1.5f, s0y + 2, s1x + 1.5f, s1y + 2);
                line(c, p, 0xFFE8C030, 3.6f, s0x, s0y, s1x, s1y);
                line(c, p, 0xFFF4DC70, 1.4f, s0x, s0y, s1x, s1y);
                float t2x = f.x(tp[2][0], tp[2][1]), t2y = f.y(tp[2][0], tp[2][1]);
                curve(c, p, 0x40000000, 3.4f, t2x + 1.5f, t2y + 2, t2x + f.ex(12, 2) + 1.5f, t2y + f.ey(12, 2) + 2, t2x + f.ex(8, 13) + 1.5f, t2y + f.ey(8, 13) + 2);
                curve(c, p, 0xFF2E7AC0, 3.4f, t2x, t2y, t2x + f.ex(12, 2), t2y + f.ey(12, 2), t2x + f.ex(8, 13), t2y + f.ey(8, 13));
                curve(c, p, 0xFF5AA0E0, 1.2f, t2x - 0.5f, t2y - 0.5f, t2x + f.ex(12, 2) - 0.5f, t2y + f.ey(12, 2) - 0.5f, t2x + f.ex(8, 13) - 0.5f, t2y + f.ey(8, 13) - 0.5f);
                int[] roofs = {0xFFC83A2E, 0xFF2E7AC0, 0xFFE8C030};
                for (int i = 0; i < 3; i++) {
                    float tx = f.x(tp[i][0], tp[i][1]), ty = f.y(tp[i][0], tp[i][1]), h = ts / 2;
                    rect(c, p, 0x40000000, tx - h + 2, ty - h + 2.5f, tx + h + 2, ty + h + 2.5f);
                    int rcol = roofs[i];
                    poly(c, p, City.lighten(rcol, 0.15f), tx - h, ty - h, tx + h, ty - h, tx, ty);
                    poly(c, p, rcol, tx - h, ty - h, tx - h, ty + h, tx, ty);
                    poly(c, p, City.darken(rcol, 0.8f), tx + h, ty - h, tx + h, ty + h, tx, ty);
                    poly(c, p, City.darken(rcol, 0.66f), tx - h, ty + h, tx + h, ty + h, tx, ty);
                    circle(c, p, 0xFFF0F0E8, tx, ty, 0.8f);
                }
                // A climbing net.
                float nx = f.x(0.6f, 0.42f), ny = f.y(0.6f, 0.42f), nh = ts * 0.7f;
                frame(c, p, 0xFF4A4C50, 0.7f, nx - nh, ny - nh, nx + nh, ny + nh);
                line(c, p, 0xFF4A4C50, 0.4f, nx - nh, ny - nh, nx + nh, ny + nh);
                line(c, p, 0xFF4A4C50, 0.4f, nx + nh, ny - nh, nx - nh, ny + nh);
                line(c, p, 0xFF4A4C50, 0.4f, nx, ny - nh, nx, ny + nh);
                line(c, p, 0xFF4A4C50, 0.4f, nx - nh, ny, nx + nh, ny);
                // Swings over a strip of rubber.
                float wu0 = 0.4f, wu1 = 0.62f, wv = 0.86f;
                float wx0 = f.x(wu0, wv), wy0 = f.y(wu0, wv), wx1 = f.x(wu1, wv), wy1 = f.y(wu1, wv);
                float ox = f.ex(0, 6), oy = f.ey(0, 6);
                rect(c, p, 0xFF4E6A8A, Math.min(wx0, wx1) - ox - 2, Math.min(wy0, wy1) - oy - 2, Math.max(wx0, wx1) + ox + 2, Math.max(wy0, wy1) + oy + 2);
                line(c, p, 0x40000000, 2.2f, wx0 + 2, wy0 + 3, wx1 + 2, wy1 + 3);
                line(c, p, 0xFF3A4048, 0.8f, wx0 - ox, wy0 - oy, wx0 + ox, wy0 + oy);
                line(c, p, 0xFF3A4048, 0.8f, wx1 - ox, wy1 - oy, wx1 + ox, wy1 + oy);
                line(c, p, 0xFFC83A2E, 2, wx0, wy0, wx1, wy1);
                for (int i = 1; i <= 3; i++) {
                    float t = i / 4f, kx = wx0 + (wx1 - wx0) * t, ky = wy0 + (wy1 - wy0) * t;
                    float ex = kx + ox * 0.5f * (i == 2 ? -1 : 1), ey = ky + oy * 0.5f * (i == 2 ? -1 : 1);
                    line(c, p, 0xFF8A8E92, 0.3f, kx, ky, ex, ey);
                    rect(c, p, 0xFF202020, ex - 1.4f, ey - 0.7f, ex + 1.4f, ey + 0.7f);
                }
                // A sandpit with toys.
                float bu = 0.08f, bv = 0.88f, bs = Math.min(0.12f * L, 0.18f * S);
                float bx0 = f.x(bu, bv) - bs / 2, by0 = f.y(bu, bv) - bs / 2;
                if (f.wide) {
                    bx0 = x0 + 8;
                    by0 = y1 - 8 - bs;
                } else {
                    bx0 = x1 - 8 - bs;
                    by0 = y0 + 8;
                }
                rect(c, p, 0x40000000, bx0 + 1, by0 + 1.5f, bx0 + bs + 1, by0 + bs + 1.5f);
                rect(c, p, 0xFF8A6A48, bx0, by0, bx0 + bs, by0 + bs);
                rect(c, p, 0xFFE4D4A4, bx0 + 1.5f, by0 + 1.5f, bx0 + bs - 1.5f, by0 + bs - 1.5f);
                circle(c, p, 0xFFD4C494, bx0 + bs * 0.35f, by0 + bs * 0.4f, bs * 0.15f);
                circle(c, p, 0xFFE04A3A, bx0 + bs * 0.65f, by0 + bs * 0.6f, 1.3f);
                circle(c, p, 0xFF3E8AE0, bx0 + bs * 0.3f, by0 + bs * 0.72f, 1);
                // Spring riders, benches, a picnic table and trees, kept off the kiosk.
                for (int i = 0; i < 3; i++) {
                    float u = 0.86f + (i % 2) * 0.05f, v = 0.12f + i * 0.13f;
                    float qx = f.x(u, v), qy = f.y(u, v);
                    oval(c, p, 0x40000000, qx - 1.5f, qy - 2, qx + 2.5f, qy + 3);
                    oval(c, p, BRIGHT[i + 1], qx - 2, qy - 2.5f, qx + 2, qy + 2.5f);
                }
                float[][] bn = {{0.27f, 0.06f}, {0.08f, 0.47f}, {0.45f, 0.47f}};
                for (int i = 0; i < bn.length; i++) {
                    float bxx = f.x(bn[i][0], bn[i][1]), byy = f.y(bn[i][0], bn[i][1]);
                    bench(c, p, bxx, byy, (i == 0) == f.wide);
                }
                float[][] tr = {{0.05f, 0.08f}, {0.46f, 0.1f}, {0.5f, 0.92f}, {0.05f, 0.62f}};
                for (int i = 0; i < tr.length; i++) {
                    float qx = f.x(tr[i][0], tr[i][1]), qy = f.y(tr[i][0], tr[i][1]);
                    if (!inside(kb, 6, qx, qy)) tree(c, p, qx, qy, 7, i % 2 == 0 ? 0xFF3E7A3A : 0xFF4A8640);
                }
                float pkx = (kb[0] + kb[2]) / 2 - f.ex(10, 0), pky = (kb[1] + kb[3]) / 2 - f.ey(10, 0);
                rect(c, p, 0x40000000, pkx - 3, pky - 2, pkx + 4.5f, pky + 4.5f);
                rect(c, p, 0xFF8A6A48, pkx - 3.5f, pky - 1.2f, pkx + 3.5f, pky + 1.2f);
                rect(c, p, 0xFF6E5034, pkx - 3.5f, pky - 3.5f, pkx + 3.5f, pky - 2.5f);
                rect(c, p, 0xFF6E5034, pkx - 3.5f, pky + 2.5f, pkx + 3.5f, pky + 3.5f);
                // A low bright fence round the play area.
                fence(c, p, 0xFF3E7AB0, x0 + 5.5f, y0 + 5.5f, x1 - 5.5f, y1 - 5.5f, 10);
                sign(city, "ICE CREAM", (kb[0] + kb[2]) / 2, (kb[1] + kb[3]) / 2, 5, 0xFFF0E8D8);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                flatRoof(c, p, x0, y0, x1, y1, 0xFFB8BCC0);
                rect(c, p, 0xFF8A9096, x0 + 4, y0 + 4, x0 + 8, y0 + 8);
                // A striped awning on the front edge.
                boolean hz = x1 - x0 >= y1 - y0;
                int k = 0;
                if (hz) {
                    for (float x = x0; x < x1 - 0.1f; x += 4, k++) rect(c, p, k % 2 == 0 ? 0xFFE04A6A : 0xFFF4F0E8, x, y1 - 5, Math.min(x1, x + 4), y1);
                } else {
                    for (float y = y0; y < y1 - 0.1f; y += 4, k++) rect(c, p, k % 2 == 0 ? 0xFFE04A6A : 0xFFF4F0E8, x1 - 5, y, x1, Math.min(y1, y + 4));
                }
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.PARK | Variants.SUB | Variants.MID;
        d.minS = 9;
        d.minL = 11;
        d.base = City.KIOSK;
        d.inside = Variants.I_CAFE;
        d.roofCol = 0xFFB8BCC0;
        out.add(d);
    }

    /** A filled ring sector between radii r0 and r1, angles a0..a1 (radians, y down). */
    private static void sector(Canvas c, Paint p, int col, float cx, float cy, float r0, float r1, float a0, float a1) {
        int n = Math.max(4, (int) (Math.abs(a1 - a0) * r1 / 3));
        Path path = new Path();
        for (int i = 0; i <= n; i++) {
            double a = a0 + (a1 - a0) * i / n;
            float px = cx + (float) Math.cos(a) * r1, py = cy + (float) Math.sin(a) * r1;
            if (i == 0) path.moveTo(px, py);
            else path.lineTo(px, py);
        }
        for (int i = n; i >= 0; i--) {
            double a = a0 + (a1 - a0) * i / n;
            path.lineTo(cx + (float) Math.cos(a) * r0, cy + (float) Math.sin(a) * r0);
        }
        path.close();
        p.setStyle(Paint.Style.FILL);
        p.setColor(col);
        c.drawPath(path, p);
    }

    // 10. A park amphitheatre: grass terraces with stone risers fanning round a stage under a shell.
    private static void amphitheatre(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Park Amphitheatre", T_PARK) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int sw = Math.max(4, Math.min(8, bw / 3));
                return new int[][]{{(bw - sw) / 2, 1, sw, 3, 1}};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                float[] sb = part(plan(Math.round((x1 - x0) / T), Math.round((y1 - y0) / T), null), 0, x0, y0);
                float cx = (sb[0] + sb[2]) / 2, fy = sb[3];
                rect(c, p, 0xFF66984C, x0, y0, x1, y1);
                float r0 = (sb[2] - sb[0]) * 0.42f;
                float rmax = Math.min(Math.min(cx - x0, x1 - cx), y1 - fy) - 7;
                float a0 = (float) (-0.1 * Math.PI), a1 = (float) (1.1 * Math.PI);
                // A gravel walk round the top, the terraces, then the paved orchestra.
                sector(c, p, 0xFFB8AC8A, cx, fy, rmax - 0.5f, rmax + 5.5f, a0, a1);
                sector(c, p, 0xFFD2C6A6, cx, fy, rmax, rmax + 5, a0, a1);
                int nt = Math.max(3, (int) ((rmax - r0) / 7));
                float tw = (rmax - r0) / nt;
                for (int t = nt - 1; t >= 0; t--) {
                    float ra = r0 + t * tw, rb = ra + tw;
                    sector(c, p, t % 2 == 0 ? 0xFF6E9E52 : 0xFF66964C, cx, fy, ra, rb, a0, a1);
                    sector(c, p, 0xFFC4BCA8, cx, fy, ra, ra + 1.8f, a0, a1);
                    sector(c, p, 0x30000000, cx, fy, ra + 1.8f, ra + 2.4f, a0, a1);
                }
                // Aisles of steps.
                float[] aisles = {(float) (0.25 * Math.PI), (float) (0.5 * Math.PI), (float) (0.75 * Math.PI)};
                for (float a : aisles) {
                    float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
                    line(c, p, 0xFFC4BCA8, 5, cx + ca * r0, fy + sa * r0, cx + ca * (rmax + 2), fy + sa * (rmax + 2));
                    for (float rr = r0 + 2; rr < rmax; rr += 2.4f) {
                        line(c, p, 0x38000000, 0.4f, cx + ca * rr - sa * 2.5f, fy + sa * rr + ca * 2.5f, cx + ca * rr + sa * 2.5f, fy + sa * rr - ca * 2.5f);
                    }
                }
                // People and picnic blankets dotted on the terraces.
                for (int i = 0; i < 14; i++) {
                    double a = 0.05 * Math.PI + r.nextFloat() * 0.9 * Math.PI;
                    float rr = r0 + tw * (0.5f + r.nextInt(nt));
                    float px = cx + (float) Math.cos(a) * rr, py = fy + (float) Math.sin(a) * rr;
                    boolean near = false;
                    for (float aa : aisles) if (Math.abs(a - aa) * rr < 5) near = true;
                    if (near) continue;
                    if (i % 4 == 0) rect(c, p, BRIGHT[i % BRIGHT.length], px - 3, py - 2, px + 3, py + 2);
                    circle(c, p, BRIGHT[(i + 2) % BRIGHT.length], px, py, 1.4f);
                    circle(c, p, 0xFFD8B090, px, py, 0.8f);
                }
                sector(c, p, 0xFFB0A68E, cx, fy, 0, r0 + 0.5f, a0, a1);
                sector(c, p, 0xFFD8CEB6, cx, fy, 0, r0, a0, a1);
                sector(c, p, 0xFFC8BEA6, cx, fy, 0, r0 * 0.6f, a0, a1);
                // Lamps on the walk, trees beyond.
                for (int i = 0; i <= 6; i++) {
                    double a = a0 + (a1 - a0) * i / 6;
                    lamp(c, p, cx + (float) Math.cos(a) * (rmax + 2.5f), fy + (float) Math.sin(a) * (rmax + 2.5f));
                }
                float g = Math.max(20, (float) Math.sqrt((x1 - x0) * (y1 - y0) / 50));
                int[] tc = {0xFF3E7A3A, 0xFF4A8640, 0xFF35683A};
                for (float ty = y0 + g * 0.45f; ty < y1 - 4; ty += g) {
                    for (float tx = x0 + g * 0.45f; tx < x1 - 4; tx += g) {
                        float px = tx + (r.nextFloat() - 0.5f) * g * 0.4f, py = ty + (r.nextFloat() - 0.5f) * g * 0.4f;
                        float dx = px - cx, dy = py - fy;
                        if ((dy > -rmax * 0.4f && dx * dx + dy * dy < (rmax + 13) * (rmax + 13)) || inside(sb, 10, px, py)) continue;
                        tree(c, p, px, py, 6 + r.nextFloat() * 3, tc[r.nextInt(tc.length)]);
                    }
                }
                // A path in from the bottom edge.
                rect(c, p, 0xFFD2C6A6, cx - 3, fy + rmax + 5, cx + 3, y1);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                float w = x1 - x0, h = y1 - y0, back = h * 0.38f, cx = (x0 + x1) / 2, by = y0 + back;
                // The stage floor showing under the front of the shell.
                rect(c, p, 0xFF7A5A3A, x0, by, x1, y1);
                for (float x = x0 + 2; x < x1; x += 2.5f) line(c, p, 0x30000000, 0.4f, x, by, x, y1);
                // The backstage block.
                flatRoof(c, p, x0, y0, x1, by + 1, 0xFF6A6E74);
                rect(c, p, 0xFF8A8E94, x0 + w * 0.15f, y0 + 3, x0 + w * 0.25f, y0 + back * 0.6f);
                // The shell: segments opening towards the audience, lit from the top left.
                float rx = w / 2 - 0.8f, ry = h - back - 0.6f;
                int n = 9;
                for (int i = 0; i < n; i++) {
                    double aa = Math.PI * i / n, ab = Math.PI * (i + 1) / n, am = (aa + ab) / 2;
                    float lit = (float) Math.cos(am - Math.PI * 0.85);
                    int col = shade(0xFFE2DACA, 1 + lit * 0.12f - (i % 2) * 0.05f);
                    poly(c, p, col, cx, by, cx + (float) Math.cos(aa) * rx, by + (float) Math.sin(aa) * ry, cx + (float) Math.cos(am) * rx * 1.01f, by + (float) Math.sin(am) * ry * 1.01f,
                            cx + (float) Math.cos(ab) * rx, by + (float) Math.sin(ab) * ry);
                }
                for (int i = 0; i <= n; i++) {
                    double a = Math.PI * i / n;
                    line(c, p, 0xFFF8F4EC, 0.6f, cx, by, cx + (float) Math.cos(a) * rx, by + (float) Math.sin(a) * ry);
                }
                for (int i = 0; i < n; i++) {
                    double aa = Math.PI * i / n, ab = Math.PI * (i + 1) / n;
                    line(c, p, 0xFF9A9284, 0.8f, cx + (float) Math.cos(aa) * rx, by + (float) Math.sin(aa) * ry, cx + (float) Math.cos(ab) * rx, by + (float) Math.sin(ab) * ry);
                }
                circle(c, p, 0xFFC8C0B0, cx, by, 2);
                sign(city, "STAGE", cx, y0 + back / 2, 5, 0xFFF0E8D8);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.PARK | Variants.UNI | Variants.MID | Variants.OLD;
        d.minS = 10;
        d.minL = 12;
        d.base = City.OFFICE;
        d.inside = Variants.I_SEATS;
        d.roofCol = 0xFFE2DACA;
        out.add(d);
    }

    // 11. A war memorial: an obelisk on a stepped plinth with wreaths, flags, yew hedges and a cypress avenue.
    private static void warMemorial(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("War Memorial", T_MONUMENT) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return new int[][]{{bw / 2 - 1, bh / 2 - 1, 2, 2, 12}};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                float[] ob = part(plan(Math.round((x1 - x0) / T), Math.round((y1 - y0) / T), null), 0, x0, y0);
                float cx = (ob[0] + ob[2]) / 2, cy = (ob[1] + ob[3]) / 2;
                float W = x1 - x0, H = y1 - y0, m = Math.min(W, H);
                rect(c, p, 0xFF5E9246, x0, y0, x1, y1);
                // Lawn panels framed by clipped yew, with rose borders.
                float pw = Math.max(8, m * 0.07f);
                float[][] qs = {{x0 + 4, y0 + 4, cx - pw - 2, cy - pw - 2}, {cx + pw + 2, y0 + 4, x1 - 4, cy - pw - 2},
                        {x0 + 4, cy + pw + 2, cx - pw - 2, y1 - 4}, {cx + pw + 2, cy + pw + 2, x1 - 4, y1 - 4}};
                for (float[] q : qs) {
                    frame(c, p, 0x40000000, 2.6f, q[0] + 0.8f, q[1] + 1, q[2] + 0.8f, q[3] + 1);
                    frame(c, p, 0xFF2A4E2A, 2.6f, q[0], q[1], q[2], q[3]);
                    frame(c, p, 0xFF3E6A38, 1, q[0] - 0.4f, q[1] - 0.4f, q[2] - 0.4f, q[3] - 0.4f);
                    rect(c, p, 0xFF68A050, q[0] + 1.3f, q[1] + 1.3f, q[2] - 1.3f, q[3] - 1.3f);
                    for (float x = q[0] + 4; x < q[2] - 3; x += 3.5f) {
                        circle(c, p, 0xFFB8283A, x, q[1] + 3, 0.9f);
                        circle(c, p, 0xFFB8283A, x, q[3] - 3, 0.9f);
                    }
                }
                // Paved cross walks and the round plaza.
                rect(c, p, 0xFFCFC6B2, cx - pw, y0, cx + pw, y1);
                rect(c, p, 0xFFCFC6B2, x0, cy - pw, x1, cy + pw);
                for (float y = y0 + 4; y < y1; y += 4) line(c, p, 0x18000000, 0.4f, cx - pw, y, cx + pw, y);
                for (float x = x0 + 4; x < x1; x += 4) line(c, p, 0x18000000, 0.4f, x, cy - pw, x, cy + pw);
                float pr = Math.min(m * 0.28f, Math.max(28, pw * 2.6f));
                circle(c, p, 0xFFB8AE98, cx, cy, pr + 1);
                circle(c, p, 0xFFD6CDB8, cx, cy, pr);
                ring(c, p, 0x20000000, 0.6f, cx, cy, pr * 0.8f);
                // The stepped plinth.
                float hs = (ob[2] - ob[0]) / 2;
                for (int k = 3; k >= 1; k--) {
                    float e = hs + k * 3.5f;
                    rect(c, p, 0x38000000, cx - e + 1.2f, cy - e + 1.6f, cx + e + 1.2f, cy + e + 1.6f);
                    rect(c, p, shade(0xFFB8B2A4, 1 - k * 0.05f + 0.15f), cx - e, cy - e, cx + e, cy + e);
                    rect(c, p, shade(0xFFB8B2A4, 0.85f), cx - e, cy + e - 0.8f, cx + e, cy + e);
                    rect(c, p, shade(0xFFB8B2A4, 0.9f), cx + e - 0.8f, cy - e, cx + e, cy + e);
                }
                // Poppy wreaths on the steps.
                float we = hs + 6;
                float[][] wr = {{cx, cy + we}, {cx - we, cy + 2}, {cx + we, cy - 2}, {cx + 3, cy - we}};
                for (int i = 0; i < wr.length; i++) {
                    float wx = wr[i][0], wy = wr[i][1], rr = 2.3f;
                    ring(c, p, 0xFF2E5A2E, 1.4f, wx, wy, rr);
                    for (int k = 0; k < 8; k++) {
                        double a = k * Math.PI / 4;
                        circle(c, p, 0xFFD02020, wx + (float) Math.cos(a) * rr, wy + (float) Math.sin(a) * rr, 0.75f);
                    }
                    rect(c, p, 0xFFD02020, wx - 0.5f, wy + rr, wx + 0.5f, wy + rr + 1.6f);
                }
                // Flags on poles round the plaza.
                int[][] flags = {{0xFFC8102E, 0xFFF4F4F4, 0xFF1A3A8A}, {0xFF1A3A8A, 0xFFF4F4F4, 0xFF1A3A8A}, {0xFF2E7A3A, 0xFFF4F4F4, 0xFFC8102E},
                        {0xFFC8102E, 0xFFC8102E, 0xFFF4F4F4}, {0xFF1A3A8A, 0xFFE8C030, 0xFF1A3A8A}, {0xFF202020, 0xFFC8102E, 0xFFE8C030}};
                int nf = 6;
                for (int i = 0; i < nf; i++) {
                    double a = Math.PI * 1.1 + i * Math.PI * 0.8 / (nf - 1);
                    float fx = cx + (float) Math.cos(a) * pr * 0.85f, fy = cy + (float) Math.sin(a) * pr * 0.85f;
                    float fl = 5.5f, fh = 3.6f;
                    poly(c, p, 0x30000000, fx + 1.5f, fy + 2, fx + fl + 1.5f, fy + 2.6f, fx + fl + 1.5f, fy + fh + 2.6f, fx + 1.5f, fy + fh + 2);
                    int[] fc = flags[i % flags.length];
                    for (int s = 0; s < 3; s++) {
                        float sa = fh * s / 3, sb2 = fh * (s + 1) / 3;
                        poly(c, p, fc[s], fx, fy - fh / 2 + sa, fx + fl, fy - fh / 2 + sa + 0.6f, fx + fl, fy - fh / 2 + sb2 + 0.6f, fx, fy - fh / 2 + sb2);
                    }
                    circle(c, p, 0xFF4A4C50, fx, fy, 0.9f);
                    circle(c, p, 0xFFE0C060, fx, fy, 0.45f);
                }
                // A cypress avenue along the main walk, benches and lamps.
                boolean tall = H > W;
                float step = 18;
                if (tall) {
                    for (float y = y0 + 9; y < y1 - 6; y += step) {
                        if (Math.abs(y - cy) < pr + 6) continue;
                        for (int s = -1; s <= 1; s += 2) {
                            float tx = cx + s * (pw + 5);
                            circle(c, p, 0x40000000, tx + 1.5f, y + 2, 3.2f);
                            circle(c, p, 0xFF1E3E26, tx, y, 3.2f);
                            circle(c, p, 0xFF2E5A36, tx - 0.8f, y - 0.8f, 2);
                        }
                    }
                } else {
                    for (float x = x0 + 9; x < x1 - 6; x += step) {
                        if (Math.abs(x - cx) < pr + 6) continue;
                        for (int s = -1; s <= 1; s += 2) {
                            float ty = cy + s * (pw + 5);
                            circle(c, p, 0x40000000, x + 1.5f, ty + 2, 3.2f);
                            circle(c, p, 0xFF1E3E26, x, ty, 3.2f);
                            circle(c, p, 0xFF2E5A36, x - 0.8f, ty - 0.8f, 2);
                        }
                    }
                }
                for (int k = 0; k < 4; k++) {
                    double a = Math.PI / 4 + k * Math.PI / 2;
                    bench(c, p, cx + (float) Math.cos(a) * (pr - 4), cy + (float) Math.sin(a) * (pr - 4), k % 2 == 0);
                    lamp(c, p, cx + (float) Math.cos(a) * (pr + 4), cy + (float) Math.sin(a) * (pr + 4));
                }
                tree(c, p, x0 + 10, y0 + 10, 7, 0xFF35683A);
                tree(c, p, x1 - 10, y0 + 10, 7, 0xFF35683A);
                tree(c, p, x0 + 10, y1 - 10, 7, 0xFF35683A);
                tree(c, p, x1 - 10, y1 - 10, 7, 0xFF35683A);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                // The obelisk from above: tapering faces to a gilt-tipped pyramidion.
                float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2, hs = (x1 - x0) / 2, in = hs * 0.42f, tip = hs * 0.2f;
                int stone = 0xFFD0CABC;
                poly(c, p, shade(stone, 1.08f), x0, y0, x1, y0, cx + in, cy - in, cx - in, cy - in);
                poly(c, p, shade(stone, 1.0f), x0, y0, x0, y1, cx - in, cy + in, cx - in, cy - in);
                poly(c, p, shade(stone, 0.8f), x1, y0, x1, y1, cx + in, cy + in, cx + in, cy - in);
                poly(c, p, shade(stone, 0.72f), x0, y1, x1, y1, cx + in, cy + in, cx - in, cy + in);
                frame(c, p, 0xFF8A8478, 0.6f, x0, y0, x1, y1);
                // Inscription bands on the faces.
                line(c, p, 0x50000000, 0.5f, cx - hs * 0.55f, y1 - hs * 0.25f, cx + hs * 0.55f, y1 - hs * 0.25f);
                line(c, p, 0x50000000, 0.5f, cx - hs * 0.45f, y1 - hs * 0.35f, cx + hs * 0.45f, y1 - hs * 0.35f);
                poly(c, p, shade(stone, 1.14f), cx - in, cy - in, cx + in, cy - in, cx, cy);
                poly(c, p, shade(stone, 1.02f), cx - in, cy - in, cx - in, cy + in, cx, cy);
                poly(c, p, shade(stone, 0.84f), cx + in, cy - in, cx + in, cy + in, cx, cy);
                poly(c, p, shade(stone, 0.76f), cx - in, cy + in, cx + in, cy + in, cx, cy);
                poly(c, p, 0xFFE8C860, cx - tip, cy - tip, cx + tip, cy - tip, cx, cy);
                poly(c, p, 0xFFD4B048, cx - tip, cy - tip, cx - tip, cy + tip, cx, cy);
                poly(c, p, 0xFFB08A30, cx + tip, cy - tip, cx + tip, cy + tip, cx, cy);
                poly(c, p, 0xFF9A7828, cx - tip, cy + tip, cx + tip, cy + tip, cx, cy);
                // A bronze wreath on the front.
                ring(c, p, 0xFF6A5A30, 0.8f, cx, y1 - hs * 0.6f, hs * 0.18f);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.DOWN | Variants.MID | Variants.OLD | Variants.PARK;
        d.minS = 9;
        d.minL = 9;
        d.base = City.OFFICE;
        d.inside = Variants.I_PEWS;
        d.roofCol = 0xFFD0CABC;
        out.add(d);
    }

    /** One arboretum tree: kind picks the species. */
    private static void specimen(Canvas c, Paint p, int kind, float x, float y, float rad, Random r) {
        switch (kind) {
            case 0: // oak
                tree(c, p, x, y, rad, 0xFF3E7A3A);
                break;
            case 1: // red maple
                tree(c, p, x, y, rad * 0.9f, 0xFFB8402A);
                break;
            case 2: // orange maple
                tree(c, p, x, y, rad * 0.85f, 0xFFD07A2A);
                break;
            case 3: // golden ginkgo
                tree(c, p, x, y, rad * 0.8f, 0xFFD8B030);
                break;
            case 4: // copper beech
                tree(c, p, x, y, rad * 1.05f, 0xFF6E3A38);
                break;
            case 5: // cherry blossom
                tree(c, p, x, y, rad * 0.85f, 0xFFE0A0B8);
                for (int k = 0; k < 5; k++) circle(c, p, 0xFFF8E8EE, x + (r.nextFloat() - 0.5f) * rad, y + (r.nextFloat() - 0.5f) * rad, 0.7f);
                break;
            case 6:
            case 7: { // conifers: a spiky star, dark green or blue
                int col = kind == 6 ? 0xFF234A30 : 0xFF3E6A6A;
                float rr = rad * (kind == 6 ? 0.9f : 1.1f);
                circle(c, p, 0x40000000, x + rr * 0.35f, y + rr * 0.4f, rr * 0.9f);
                for (int layer = 0; layer < 2; layer++) {
                    float lr = rr * (layer == 0 ? 1 : 0.6f);
                    int lc = layer == 0 ? col : City.lighten(col, 0.15f);
                    Path path = new Path();
                    int n = 9;
                    for (int i = 0; i < n * 2; i++) {
                        double a = i * Math.PI / n + layer * 0.3;
                        float q = i % 2 == 0 ? lr : lr * 0.62f;
                        float px = x + (float) Math.cos(a) * q, py = y + (float) Math.sin(a) * q;
                        if (i == 0) path.moveTo(px, py);
                        else path.lineTo(px, py);
                    }
                    path.close();
                    p.setStyle(Paint.Style.FILL);
                    p.setColor(lc);
                    c.drawPath(path, p);
                }
                circle(c, p, City.darken(col, 0.7f), x, y, rr * 0.15f);
                break;
            }
            case 8: { // cedar: flat layered tiers
                circle(c, p, 0x40000000, x + rad * 0.4f, y + rad * 0.45f, rad);
                oval(c, p, 0xFF3A5E54, x - rad, y - rad * 0.55f, x + rad * 0.6f, y + rad * 0.45f);
                oval(c, p, 0xFF4A7064, x - rad * 0.4f, y - rad * 0.2f, x + rad, y + rad * 0.8f);
                oval(c, p, 0xFF5A8274, x - rad * 0.6f, y - rad * 0.8f, x + rad * 0.5f, y + rad * 0.1f);
                break;
            }
            case 9: // silver birch
                tree(c, p, x, y, rad * 0.6f, 0xFF8AAE5A);
                circle(c, p, 0xFFF0EEE6, x + rad * 0.1f, y + rad * 0.1f, 0.8f);
                break;
            default: { // weeping willow
                tree(c, p, x, y, rad, 0xFF8AAE4A);
                for (int q = 0; q < 10; q++) {
                    double a = q * Math.PI / 5;
                    line(c, p, 0xFFA8C860, 0.5f, x, y, x + (float) Math.cos(a) * rad * 0.9f, y + (float) Math.sin(a) * rad * 0.9f);
                }
                break;
            }
        }
    }

    // 12. An arboretum: trees of many kinds along winding paths, a glade with a great tree, a visitor hut.
    private static void arboretum(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Arboretum", T_PARK) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return new int[][]{tb(bw, bh, 1, 1, 3, 2, 1)};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                Fr f = new Fr(x0, y0, x1, y1);
                float[] hut = part(plan(Math.round(f.w / T), Math.round(f.h / T), null), 0, x0, y0);
                rect(c, p, 0xFF5E9046, x0, y0, x1, y1);
                for (int k = 0; k < 30; k++) circle(c, p, r.nextBoolean() ? 0x14A0C070 : 0x14203A10, x0 + r.nextFloat() * f.w, y0 + r.nextFloat() * f.h, 4 + r.nextFloat() * 9);
                float hu = (f.wide ? (hut[0] + hut[2]) / 2 - x0 : (hut[1] + hut[3]) / 2 - y0) / f.L;
                float hv = (f.wide ? hut[3] - y0 + 6 : hut[2] - x0 + 6) / f.S;
                float[] pa = f.pts(hu, hv, 0.25f, 0.42f, 0.42f, 0.3f, 0.58f, 0.55f, 0.76f, 0.4f, 0.9f, 0.62f, 1.03f, 0.55f);
                float[] pb = f.pts(0.3f, 1.03f, 0.36f, 0.75f, 0.52f, 0.68f, 0.58f, 0.55f);
                float[] pc = f.pts(0.76f, 0.4f, 0.8f, 0.15f, 0.7f, -0.03f);
                float w = 6;
                trail(c, p, 0xFFA8946E, w + 1.6f, pa);
                trail(c, p, 0xFFA8946E, w + 1.6f, pb);
                trail(c, p, 0xFFA8946E, w + 1.6f, pc);
                trail(c, p, 0xFFC8B48C, w, pa);
                trail(c, p, 0xFFC8B48C, w, pb);
                trail(c, p, 0xFFC8B48C, w, pc);
                // A glade with a great cedar and benches round it.
                float gx = f.x(0.15f, 0.78f), gy = f.y(0.15f, 0.78f), gr = f.S * 0.13f;
                circle(c, p, 0x1AFFFFFF, gx, gy, gr + 6);
                specimen(c, p, 8, gx, gy, gr * 0.75f, r);
                bench(c, p, gx, gy - gr - 2, true);
                bench(c, p, gx + gr + 2, gy, false);
                // The collection, spaced on a jittered grid, off the paths.
                float g = Math.max(17, (float) Math.sqrt(f.w * f.h / 95));
                for (float ty = y0 + g * 0.5f; ty < y1 - 3; ty += g) {
                    for (float tx = x0 + g * 0.5f; tx < x1 - 3; tx += g) {
                        float px = tx + (r.nextFloat() - 0.5f) * g * 0.55f, py = ty + (r.nextFloat() - 0.5f) * g * 0.55f;
                        int kind = r.nextInt(11);
                        float rad = g * (0.3f + r.nextFloat() * 0.15f);
                        if (dist(pa, px, py) < rad + w * 0.6f || dist(pb, px, py) < rad + w * 0.6f || dist(pc, px, py) < rad + w * 0.6f) continue;
                        if (inside(hut, rad + 2, px, py)) continue;
                        float dx = px - gx, dy = py - gy;
                        if (dx * dx + dy * dy < (gr + 6 + rad) * (gr + 6 + rad)) continue;
                        specimen(c, p, kind, Math.max(x0 + 3, Math.min(x1 - 3, px)), Math.max(y0 + 3, Math.min(y1 - 3, py)), rad, r);
                        if (r.nextInt(4) == 0) rect(c, p, 0xFFF0EEE6, px + rad * 0.7f, py + rad * 0.7f, px + rad * 0.7f + 1.6f, py + rad * 0.7f + 1);
                    }
                }
                // Lamps and a notice board by the hut.
                for (int i = 2; i < pa.length / 2 - 1; i += 2) lamp(c, p, pa[2 * i] + 4, pa[2 * i + 1] + 4);
                float nx = f.wide ? hut[2] + 4 : hut[0] + 2, ny = f.wide ? hut[1] + 2 : hut[3] + 4;
                rect(c, p, 0xFF5A3E26, nx, ny, nx + 4, ny + 1.4f);
                frame(c, p, 0xFF3E8A3A, 2, x0 + 1, y0 + 1, x1 - 1, y1 - 1);
                sign(city, "ARBORETUM", (hut[0] + hut[2]) / 2, (hut[1] + hut[3]) / 2, 5, 0xFFF0E8D8);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                gable(c, p, x0, y0, x1, y1, 0xFF4E6A3E);
                float mx = (x0 + x1) / 2, my = (y0 + y1) / 2;
                rect(c, p, 0xFF5A4A40, mx + 3, my - 5, mx + 6, my - 2);
                rect(c, p, 0xFF3A2E28, mx + 3.6f, my - 4.4f, mx + 5.4f, my - 2.6f);
            }
        };
        d.ground = City.GRASS;
        d.districts = Variants.PARK | Variants.SUB | Variants.UNI | Variants.OLD;
        d.minS = 10;
        d.minL = 12;
        d.base = City.KIOSK;
        d.inside = Variants.I_OFFICE;
        d.roofCol = 0xFF4E6A3E;
        out.add(d);
    }

    /** A crazy-golf hole in its cell: shape and obstacle by number (1..9). */
    private static void golfHole(Canvas c, Paint p, int no, float x0, float y0, float x1, float y1, Random r) {
        float w = x1 - x0, h = y1 - y0;
        boolean hz = w >= h;
        float lw = Math.max(7, Math.min(13, Math.min(w, h) * 0.38f));
        // Segments of the lane as rects, from the tee to the cup.
        float[][] seg;
        float tx, ty, cx, cy;
        float m = lw / 2 + 2;
        int shape = no % 3;
        if (shape == 0 || Math.max(w, h) < lw * 2.2f) {
            if (hz) {
                seg = new float[][]{{x0 + 2, (y0 + y1) / 2 - lw / 2, x1 - 2, (y0 + y1) / 2 + lw / 2}};
                tx = x0 + m;
                ty = (y0 + y1) / 2;
                cx = x1 - m;
                cy = ty;
            } else {
                seg = new float[][]{{(x0 + x1) / 2 - lw / 2, y0 + 2, (x0 + x1) / 2 + lw / 2, y1 - 2}};
                tx = (x0 + x1) / 2;
                ty = y0 + m;
                cx = tx;
                cy = y1 - m;
            }
        } else if (shape == 1) { // L
            seg = new float[][]{{x0 + 2, y0 + 2, x0 + 2 + lw, y1 - 2}, {x0 + 2, y1 - 2 - lw, x1 - 2, y1 - 2}};
            tx = x0 + 2 + lw / 2;
            ty = y0 + m;
            cx = x1 - m;
            cy = y1 - 2 - lw / 2;
        } else { // dogleg
            float my = (y0 + y1) / 2, mx = (x0 + x1) / 2;
            if (hz) {
                seg = new float[][]{{x0 + 2, y0 + 2, mx + lw / 2, y0 + 2 + lw}, {mx - lw / 2, y0 + 2, mx + lw / 2, y1 - 2}, {mx - lw / 2, y1 - 2 - lw, x1 - 2, y1 - 2}};
                tx = x0 + m;
                ty = y0 + 2 + lw / 2;
                cx = x1 - m;
                cy = y1 - 2 - lw / 2;
            } else {
                seg = new float[][]{{x0 + 2, y0 + 2, x0 + 2 + lw, my + lw / 2}, {x0 + 2, my - lw / 2, x1 - 2, my + lw / 2}, {x1 - 2 - lw, my - lw / 2, x1 - 2, y1 - 2}};
                tx = x0 + 2 + lw / 2;
                ty = y0 + m;
                cx = x1 - 2 - lw / 2;
                cy = y1 - m;
            }
        }
        for (float[] s : seg) rect(c, p, 0x40000000, s[0] + 0.8f, s[1] + 1.2f, s[2] + 0.8f, s[3] + 1.2f);
        for (float[] s : seg) rect(c, p, 0xFF8A5A40, s[0] - 1.2f, s[1] - 1.2f, s[2] + 1.2f, s[3] + 1.2f);
        for (float[] s : seg) rect(c, p, 0xFF3A8A48, s[0], s[1], s[2], s[3]);
        for (float[] s : seg) rect(c, p, 0x12FFFFFF, s[0] + 1, s[1] + 1, (s[0] + s[2]) / 2, s[3] - 1);
        // Tee and cup.
        rect(c, p, 0xFF2A6A36, tx - 2.2f, ty - 2.2f, tx + 2.2f, ty + 2.2f);
        circle(c, p, 0xFFF4F4F4, tx, ty, 0.6f);
        circle(c, p, 0xFFE8F0E0, cx, cy, 1.5f);
        circle(c, p, 0xFF101010, cx, cy, 1.05f);
        line(c, p, 0xFFE8E8E8, 0.4f, cx, cy, cx + 0.6f, cy - 3.5f);
        poly(c, p, 0xFFE03030, cx + 0.6f, cy - 3.5f, cx + 3, cy - 3, cx + 0.6f, cy - 2.2f);
        // The obstacle, halfway along.
        float ox = (tx + cx) / 2, oy = (ty + cy) / 2;
        if (seg.length == 2) {
            ox = (seg[0][0] + seg[0][2]) / 2;
            oy = (seg[0][1] + seg[0][3]) / 2 + 2;
        } else if (seg.length == 3) {
            ox = (seg[1][0] + seg[1][2]) / 2;
            oy = (seg[1][1] + seg[1][3]) / 2;
        }
        float s = lw * 0.42f;
        switch (no) {
            case 1: { // windmill
                rect(c, p, 0x40000000, ox - s + 1.5f, oy - s + 2, ox + s + 1.5f, oy + s + 2);
                rect(c, p, 0xFFE8DCC8, ox - s, oy - s, ox + s, oy + s);
                poly(c, p, 0xFFB8402E, ox - s, oy - s, ox + s, oy - s, ox, oy);
                poly(c, p, 0xFF8A2E22, ox - s, oy + s, ox + s, oy + s, ox, oy);
                c.save();
                c.translate(ox, oy);
                c.rotate(25);
                float sl = s * 2.1f;
                for (int k = 0; k < 4; k++) {
                    c.rotate(90);
                    rect(c, p, 0xFF6E5034, -0.4f, -sl, 0.4f, 0);
                    rect(c, p, 0xD8F4F0E8, 0.4f, -sl, 1.8f, -sl * 0.3f);
                }
                c.restore();
                circle(c, p, 0xFF3A2A20, ox, oy, 0.9f);
                break;
            }
            case 2: { // castle
                rect(c, p, 0x40000000, ox - s + 1.5f, oy - s + 2, ox + s + 1.5f, oy + s + 2);
                rect(c, p, 0xFF8A8478, ox - s, oy - s, ox + s, oy + s);
                rect(c, p, 0xFFA8A296, ox - s + 1.4f, oy - s + 1.4f, ox + s - 1.4f, oy + s - 1.4f);
                for (int k = 0; k < 4; k++) {
                    float qx = k % 2 == 0 ? ox - s : ox + s, qy = k < 2 ? oy - s : oy + s;
                    circle(c, p, 0xFF7A7468, qx, qy, s * 0.42f);
                    circle(c, p, 0xFFB43A2E, qx, qy, s * 0.25f);
                }
                rect(c, p, 0xFF3A2E24, ox - 1.2f, oy - s, ox + 1.2f, oy + s);
                break;
            }
            case 3: // water hazard with a plank bridge
                if (hz || seg.length > 1) {
                    oval(c, p, 0xFF2E6A8A, ox - s * 1.1f, oy - lw / 2 - 0.6f, ox + s * 1.1f, oy + lw / 2 + 0.6f);
                    oval(c, p, 0xFF4A8EB0, ox - s * 0.8f, oy - lw / 2 + 0.6f, ox + s * 0.8f, oy + lw / 2 - 0.6f);
                    rect(c, p, 0xFF9A7A54, ox - s * 1.2f, oy - 1.2f, ox + s * 1.2f, oy + 1.2f);
                } else {
                    oval(c, p, 0xFF2E6A8A, ox - lw / 2 - 0.6f, oy - s * 1.1f, ox + lw / 2 + 0.6f, oy + s * 1.1f);
                    oval(c, p, 0xFF4A8EB0, ox - lw / 2 + 0.6f, oy - s * 0.8f, ox + lw / 2 - 0.6f, oy + s * 0.8f);
                    rect(c, p, 0xFF9A7A54, ox - 1.2f, oy - s * 1.2f, ox + 1.2f, oy + s * 1.2f);
                }
                break;
            case 4: // loop-the-loop
                ring(c, p, 0x40000000, 1.2f, ox + 1.2f, oy + 1.6f, s);
                ring(c, p, 0xFFB8BEC4, 1.2f, ox, oy, s);
                ring(c, p, 0xFFE8ECF0, 0.4f, ox - 0.3f, oy - 0.3f, s);
                break;
            case 5: // sand bunker
                oval(c, p, 0xFFC8B888, ox - s, oy - s * 0.7f, ox + s, oy + s * 0.7f);
                oval(c, p, 0xFFE4D4A4, ox - s + 0.6f, oy - s * 0.7f + 0.6f, ox + s - 0.6f, oy + s * 0.7f - 0.6f);
                break;
            case 6: // a pipe tunnel
                if (hz && seg.length == 1) {
                    rect(c, p, 0x40000000, ox - s * 1.4f + 1, oy - lw / 2 + 1.5f, ox + s * 1.4f + 1, oy + lw / 2 + 1.5f);
                    rect(c, p, 0xFF3A6E9A, ox - s * 1.4f, oy - lw / 2, ox + s * 1.4f, oy + lw / 2);
                    rect(c, p, 0xFF5A8EBA, ox - s * 1.4f, oy - lw / 2, ox + s * 1.4f, oy - lw / 6);
                } else {
                    rect(c, p, 0x40000000, ox - lw / 2 + 1, oy - s * 1.4f + 1.5f, ox + lw / 2 + 1, oy + s * 1.4f + 1.5f);
                    rect(c, p, 0xFF3A6E9A, ox - lw / 2, oy - s * 1.4f, ox + lw / 2, oy + s * 1.4f);
                    rect(c, p, 0xFF5A8EBA, ox - lw / 2, oy - s * 1.4f, ox - lw / 6, oy + s * 1.4f);
                }
                break;
            case 7: // log bumpers
                for (int k = -1; k <= 1; k++) {
                    float bx = ox + k * s * 0.9f, by = oy + (k == 0 ? -s * 0.5f : s * 0.5f);
                    circle(c, p, 0x40000000, bx + 0.8f, by + 1, 1.6f);
                    circle(c, p, 0xFF7A5232, bx, by, 1.6f);
                    ring(c, p, 0xFFA87A50, 0.4f, bx, by, 0.9f);
                }
                break;
            case 8: // volcano
                circle(c, p, 0x40000000, ox + 1.5f, oy + 2, s * 1.1f);
                circle(c, p, 0xFF6E5038, ox, oy, s * 1.1f);
                circle(c, p, 0xFF8A6A48, ox - s * 0.2f, oy - s * 0.2f, s * 0.7f);
                circle(c, p, 0xFFE0502A, ox, oy, s * 0.35f);
                circle(c, p, 0xFFF0A040, ox, oy, s * 0.18f);
                break;
            default: { // lighthouse
                circle(c, p, 0x40000000, ox + 2, oy + 2.5f, s * 0.75f);
                circle(c, p, 0xFFF0F0E8, ox, oy, s * 0.75f);
                ring(c, p, 0xFFD03A2E, s * 0.2f, ox, oy, s * 0.5f);
                circle(c, p, 0xFFF4E080, ox, oy, s * 0.22f);
                break;
            }
        }
        // The hole number on a little post by the tee.
        float px = tx + (hz ? -0.5f : lw / 2 + 2.5f), py = ty + (hz ? lw / 2 + 2.5f : -0.5f);
        rect(c, p, 0xFFF4F0E8, px - 1.6f, py - 1.2f, px + 1.6f, py + 1.2f);
        for (int k = 0; k < Math.min(no, 5); k++) circle(c, p, 0xFF202020, px - 1.1f + k * 0.55f, py - 0.4f + (k % 2) * 0.6f, 0.2f);
    }

    // 13. Mini golf: nine felt holes with a windmill, a castle, water and more, picnic tables and a kiosk.
    private static void miniGolf(List<Landmarks.Def> out) {
        Landmarks.Def d = new Landmarks.Def("Mini Golf", T_PARK) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                return new int[][]{{1, 1, 2, 2, 1}};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                Fr f = new Fr(x0, y0, x1, y1);
                float[] kb = part(plan(0, 0, null), 0, x0, y0);
                rect(c, p, 0xFFC8BEA6, x0, y0, x1, y1);
                for (float x = x0 + 8; x < x1; x += 8) line(c, p, 0x14000000, 0.4f, x, y0, x, y1);
                for (float y = y0 + 8; y < y1; y += 8) line(c, p, 0x14000000, 0.4f, x0, y, x1, y);
                boolean longish = f.L / f.S >= 1.5f;
                int nu = longish ? 5 : 4, nv = longish ? 2 : 3;
                float in = 3;
                int hole = 1;
                for (int iv = 0; iv < nv; iv++) {
                    for (int iu = 0; iu < nu; iu++) {
                        float u0 = (float) iu / nu, u1 = (float) (iu + 1) / nu, v0 = (float) iv / nv, v1 = (float) (iv + 1) / nv;
                        float cx0 = f.x(u0, v0) + in, cy0 = f.y(u0, v0) + in, cx1 = f.x(u1, v1) - in, cy1 = f.y(u1, v1) - in;
                        if (iu == 0 && iv == 0) {
                            // The kiosk's cell: a putter rack and the queue rail.
                            rect(c, p, 0xFF5A5E64, kb[2] + 2, kb[1], kb[2] + 3, kb[3]);
                            for (float y = kb[1] + 1; y < kb[3]; y += 2) circle(c, p, BRIGHT[(int) (y) % BRIGHT.length], kb[2] + 2.5f, y, 0.6f);
                            continue;
                        }
                        if (hole > 9) {
                            // Spare cells: picnic tables under umbrellas and planters.
                            float mx = (cx0 + cx1) / 2, my = (cy0 + cy1) / 2;
                            rect(c, p, 0x40000000, mx - 3, my - 2, mx + 4.5f, my + 4.5f);
                            rect(c, p, 0xFF8A6A48, mx - 3.5f, my - 1.5f, mx + 3.5f, my + 1.5f);
                            for (int k = 0; k < 8; k++) {
                                double a = k * Math.PI / 4;
                                poly(c, p, k % 2 == 0 ? 0xFFE04A3A : 0xFFF4F0E8, mx, my, mx + (float) Math.cos(a) * 5, my + (float) Math.sin(a) * 5,
                                        mx + (float) Math.cos(a + Math.PI / 4) * 5, my + (float) Math.sin(a + Math.PI / 4) * 5);
                            }
                            bush(c, p, cx0 + 3, cy1 - 3, 2.5f);
                            bush(c, p, cx1 - 3, cy0 + 3, 2.5f);
                            continue;
                        }
                        golfHole(c, p, hole, cx0, cy0, cx1, cy1, r);
                        hole++;
                    }
                }
                // Palms in pots at the corners and a low fence.
                float[][] pp = {{x1 - 5, y0 + 5}, {x0 + 5, y1 - 5}, {x1 - 5, y1 - 5}};
                for (float[] q : pp) {
                    circle(c, p, 0xFF8A5A40, q[0], q[1], 2.2f);
                    for (int k = 0; k < 6; k++) {
                        double a = k * Math.PI / 3 + 0.3;
                        line(c, p, 0xFF4E9A48, 1, q[0], q[1], q[0] + (float) Math.cos(a) * 4.5f, q[1] + (float) Math.sin(a) * 4.5f);
                    }
                }
                frame(c, p, 0xFFF0EEE6, 0.8f, x0 + 1, y0 + 1, x1 - 1, y1 - 1);
                sign(city, "MINI GOLF", (kb[0] + kb[2]) / 2, (kb[1] + kb[3]) / 2, 5, 0xFFF0E8D8);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                // A candy-striped hut roof with a big golf ball on top.
                rect(c, p, 0xFF7A2A22, x0, y0, x1, y1);
                int k = 0;
                for (float x = x0 + 1; x < x1 - 1; x += 3, k++) rect(c, p, k % 2 == 0 ? 0xFFE04A3A : 0xFFF4F0E8, x, y0 + 1, Math.min(x1 - 1, x + 3), y1 - 1);
                rect(c, p, 0x22000000, (x0 + x1) / 2, y0 + 1, x1 - 1, y1 - 1);
                float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
                circle(c, p, 0x40000000, cx + 1.5f, cy + 2, 4);
                circle(c, p, 0xFFF4F4F0, cx, cy, 4);
                circle(c, p, 0xFFD8D8D4, cx + 0.8f, cy + 0.8f, 3);
                circle(c, p, 0xFFFFFFFF, cx - 1.2f, cy - 1.2f, 1.4f);
                for (int i = 0; i < 5; i++) circle(c, p, 0x30000000, cx - 1.5f + (i % 3) * 1.5f, cy - 0.5f + (i / 3) * 1.5f, 0.4f);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.PARK | Variants.SUB | Variants.MID | Variants.DOWN;
        d.minS = 9;
        d.minL = 10;
        d.base = City.KIOSK;
        d.inside = Variants.I_CAFE;
        d.roofCol = 0xFFE04A3A;
        out.add(d);
    }
}
