package com.instantaxtion.zombiesandbox;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ((22.4) One-of-a-kind places: buildings, landmarks and parks with their own look, a block each. A new city puts a
 * selection of them where it would otherwise have had a car park or a plain block (City.landmark). Each is a Def:
 * the ground of its block, the buildings on it (ordinary buildings to the game: people go in, shelter, fight) and how
 * its ground and roofs are painted into the map. The designs live in LandmarkArt1..4.
 */
final class Landmarks {
    private Landmarks() {
    }

    static final int T_PARK = 0, T_BUILDING = 1, T_MONUMENT = 2;

    abstract static class Def {
        final String name;
        final int type;
        /** Where it can go: Variants district bits (Variants.DOWN, MID, OLD, SUB, IND, UNI, PARK; TOWN, ANY). */
        int districts = Variants.TOWN;
        /** The block's inside must be at least minS tiles on its short side and minL on its long side. */
        int minS = 8, minL = 8;
        /** The tile the whole block is laid with (City.GRASS, PLAZA, LOT, DIRT or SAND). */
        byte ground = City.GRASS;
        /** What its buildings are to the game (City.OFFICE, SHOP, WAREHOUSE, APARTMENT or KIOSK), and inside. */
        int base = City.OFFICE, inside = Variants.I_OFFICE;
        int roofCol = 0xFF8A8A8A, accent = 0xFFD0B040;
        /** Set when registered. */
        int id, var = -1;

        Def(String name, int type) {
            this.name = name;
            this.type = type;
        }

        /**
         * Its buildings, as {dx, dy, w, h, floors} in tiles from the top left of the block's inside (bw by bh tiles:
         * keep them inside it, at least 1 tile apart, with open ground round each so there's a way to the door).
         * None for a park.
         */
        int[][] plan(int bw, int bh, Random r) {
            return new int[0][];
        }

        /** Paints the whole block's ground (world units; a tile is City.T = 16). Drawn before any roofs. */
        void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
        }

        /** Paints the roof of building number part (as in plan) over its footprint. */
        void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
            Landmarks.flatRoof(c, p, x0, y0, x1, y1, roofCol);
        }
    }

    private static List<Def> all;

    static synchronized List<Def> all() {
        if (all == null) {
            List<Def> l = new ArrayList<Def>();
            LandmarkArt1.register(l);
            LandmarkArt2.register(l);
            LandmarkArt3.register(l);
            LandmarkArt4.register(l);
            for (int i = 0; i < l.size(); i++) l.get(i).id = i;
            all = l;
        }
        return all;
    }

    static Def get(int id) {
        List<Def> l = all();
        return id >= 0 && id < l.size() ? l.get(id) : null;
    }

    // ------------------------------------------------------------------ drawing helpers for the designs

    static final RectF RF = new RectF();

    static void rect(Canvas c, Paint p, int col, float x0, float y0, float x1, float y1) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(col);
        c.drawRect(x0, y0, x1, y1, p);
    }

    static void circle(Canvas c, Paint p, int col, float x, float y, float r) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(col);
        c.drawCircle(x, y, r, p);
    }

    static void oval(Canvas c, Paint p, int col, float x0, float y0, float x1, float y1) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(col);
        RF.set(x0, y0, x1, y1);
        c.drawOval(RF, p);
    }

    static void roundRect(Canvas c, Paint p, int col, float x0, float y0, float x1, float y1, float rad) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(col);
        RF.set(x0, y0, x1, y1);
        c.drawRoundRect(RF, rad, rad, p);
    }

    static void line(Canvas c, Paint p, int col, float w, float ax, float ay, float bx, float by) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(w);
        p.setColor(col);
        c.drawLine(ax, ay, bx, by, p);
        p.setStyle(Paint.Style.FILL);
    }

    /** An outline (stroke) rectangle. */
    static void frame(Canvas c, Paint p, int col, float w, float x0, float y0, float x1, float y1) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(w);
        p.setColor(col);
        c.drawRect(x0, y0, x1, y1, p);
        p.setStyle(Paint.Style.FILL);
    }

    /** A filled polygon through the points {x0, y0, x1, y1, ...}. */
    static void poly(Canvas c, Paint p, int col, float... pts) {
        Path path = new Path();
        path.moveTo(pts[0], pts[1]);
        for (int k = 2; k + 1 < pts.length; k += 2) path.lineTo(pts[k], pts[k + 1]);
        path.close();
        p.setStyle(Paint.Style.FILL);
        p.setColor(col);
        c.drawPath(path, p);
    }

    /** A tree from above: a shadow, the canopy, and a lit side. */
    static void tree(Canvas c, Paint p, float x, float y, float r, int col) {
        circle(c, p, 0x40000000, x + r * 0.35f, y + r * 0.4f, r);
        circle(c, p, City.darken(col, 0.8f), x, y, r);
        circle(c, p, col, x - r * 0.12f, y - r * 0.12f, r * 0.8f);
        circle(c, p, City.lighten(col, 0.18f), x - r * 0.35f, y - r * 0.35f, r * 0.35f);
    }

    static void tree(Canvas c, Paint p, float x, float y, float r) {
        tree(c, p, x, y, r, 0xFF3E7A3A);
    }

    static void bush(Canvas c, Paint p, float x, float y, float r) {
        circle(c, p, 0x30000000, x + 1, y + 1, r);
        circle(c, p, 0xFF4E8A40, x, y, r);
        circle(c, p, 0xFF62A050, x - r * 0.3f, y - r * 0.3f, r * 0.45f);
    }

    static void bench(Canvas c, Paint p, float x, float y, boolean across) {
        if (across) {
            rect(c, p, 0xFF6E5034, x - 4, y - 1.2f, x + 4, y + 1.2f);
            rect(c, p, 0xFF4A3420, x - 4, y + 0.6f, x + 4, y + 1.2f);
        } else {
            rect(c, p, 0xFF6E5034, x - 1.2f, y - 4, x + 1.2f, y + 4);
            rect(c, p, 0xFF4A3420, x + 0.6f, y - 4, x + 1.2f, y + 4);
        }
    }

    static void lamp(Canvas c, Paint p, float x, float y) {
        circle(c, p, 0x40000000, x + 1, y + 1, 1.4f);
        circle(c, p, 0xFF3A3C40, x, y, 1.3f);
        circle(c, p, 0xFFF4E6A8, x, y, 0.7f);
    }

    /** Still water with a rim and a lighter shallow edge. */
    static void pond(Canvas c, Paint p, float x0, float y0, float x1, float y1) {
        oval(c, p, 0xFF8A7A5A, x0 - 1.5f, y0 - 1.5f, x1 + 1.5f, y1 + 1.5f);
        oval(c, p, 0xFF3F7FA8, x0, y0, x1, y1);
        oval(c, p, 0xFF2F6A94, x0 + (x1 - x0) * 0.12f, y0 + (y1 - y0) * 0.12f, x1 - (x1 - x0) * 0.12f, y1 - (y1 - y0) * 0.12f);
        p.setColor(0x50FFFFFF);
        c.drawRect(x0 + (x1 - x0) * 0.3f, y0 + (y1 - y0) * 0.35f, x0 + (x1 - x0) * 0.45f, y0 + (y1 - y0) * 0.35f + 0.6f, p);
    }

    /** A plain flat roof: a parapet and a lighter deck. */
    static void flatRoof(Canvas c, Paint p, float x0, float y0, float x1, float y1, int col) {
        rect(c, p, City.darken(col, 0.7f), x0, y0, x1, y1);
        rect(c, p, col, x0 + 1.5f, y0 + 1.5f, x1 - 1.5f, y1 - 1.5f);
    }

    /** A name painted on the roof or ground (shown as a sign label, the same as the other buildings'). */
    static void sign(City city, String text, float x, float y, float size, int col) {
        city.sign(text, x, y, size, col);
    }

    static int shade(int col, float f) {
        return f < 1 ? City.darken(col, f) : City.lighten(col, f - 1);
    }
}
