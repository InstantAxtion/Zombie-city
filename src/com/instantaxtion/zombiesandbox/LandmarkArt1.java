package com.instantaxtion.zombiesandbox;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.Landmarks.*;

/** ((22.4) Landmark designs, part 1 (see Landmarks). */
final class LandmarkArt1 {
    private LandmarkArt1() {
    }

    static void register(List<Landmarks.Def> out) {
        // A paved square round a big three-tier fountain, with plane trees, benches and lamps.
        Landmarks.Def d = new Landmarks.Def("Fountain Square", T_PARK) {
            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2, m = Math.min(x1 - x0, y1 - y0);
                rect(c, p, 0xFFCFC6B2, x0, y0, x1, y1);
                // Paving in a radial pattern of light and dark rings.
                for (int k = 6; k >= 1; k--) circle(c, p, k % 2 == 0 ? 0xFFD9D0BC : 0xFFC4BAA4, cx, cy, m * 0.08f * k);
                for (int k = 0; k < 16; k++) {
                    double a = k * Math.PI / 8;
                    line(c, p, 0x30000000, 0.6f, cx, cy, cx + (float) Math.cos(a) * m * 0.48f, cy + (float) Math.sin(a) * m * 0.48f);
                }
                // The fountain: a stone basin, water, the tiers and the spray.
                circle(c, p, 0xFF8E8676, cx, cy, m * 0.2f);
                circle(c, p, 0xFF4A90B8, cx, cy, m * 0.18f);
                circle(c, p, 0xFF6AAAD0, cx, cy, m * 0.13f);
                circle(c, p, 0xFFB0A894, cx, cy, m * 0.08f);
                circle(c, p, 0xFF5AA0C8, cx, cy, m * 0.06f);
                circle(c, p, 0xFFC8C0AC, cx, cy, m * 0.025f);
                for (int k = 0; k < 12; k++) {
                    double a = k * Math.PI / 6;
                    circle(c, p, 0x90FFFFFF, cx + (float) Math.cos(a) * m * 0.1f, cy + (float) Math.sin(a) * m * 0.1f, 0.8f);
                }
                // Trees round the square, benches facing in, lamps on the corners.
                float in = 10;
                for (float t = x0 + in; t <= x1 - in + 0.1f; t += (x1 - x0 - 2 * in) / 4) {
                    tree(c, p, t, y0 + in, 5.5f);
                    tree(c, p, t, y1 - in, 5.5f);
                }
                for (float t = y0 + in + (y1 - y0 - 2 * in) / 4; t < y1 - in - 1; t += (y1 - y0 - 2 * in) / 4) {
                    tree(c, p, x0 + in, t, 5.5f);
                    tree(c, p, x1 - in, t, 5.5f);
                }
                for (int k = 0; k < 4; k++) {
                    double a = Math.PI / 4 + k * Math.PI / 2;
                    bench(c, p, cx + (float) Math.cos(a) * m * 0.3f, cy + (float) Math.sin(a) * m * 0.3f, k % 2 == 0);
                }
                lamp(c, p, x0 + 3, y0 + 3);
                lamp(c, p, x1 - 3, y0 + 3);
                lamp(c, p, x0 + 3, y1 - 3);
                lamp(c, p, x1 - 3, y1 - 3);
            }
        };
        d.ground = City.PLAZA;
        d.districts = Variants.DOWN | Variants.MID | Variants.OLD;
        out.add(d);

        // A civic hall with a clock tower on its front and a lawn before it.
        d = new Landmarks.Def("Clock Tower Hall", T_BUILDING) {
            @Override
            int[][] plan(int bw, int bh, Random r) {
                int hw = bw - 4, hh = Math.max(4, bh / 2);
                return new int[][]{{2, bh - hh - 1, hw, hh, 3}, {bw / 2 - 1, bh - hh - 4, 3, 3, 9}};
            }

            @Override
            void ground(City city, Canvas c, Paint p, float x0, float y0, float x1, float y1, Random r) {
                rect(c, p, 0xFF5E9A48, x0, y0, x1, y1);
                float cx = (x0 + x1) / 2;
                rect(c, p, 0xFFD4CAB0, cx - 6, y0, cx + 6, y1);
                for (float y = y0 + 8; y < (y0 + y1) / 2; y += 14) {
                    bush(c, p, cx - 10, y, 3);
                    bush(c, p, cx + 10, y, 3);
                }
                tree(c, p, x0 + 10, y0 + 10, 7);
                tree(c, p, x1 - 10, y0 + 10, 7);
                lamp(c, p, cx - 7, y0 + 4);
                lamp(c, p, cx + 7, y0 + 4);
            }

            @Override
            void roof(City city, Canvas c, Paint p, int part, float x0, float y0, float x1, float y1, Random r) {
                if (part == 0) {
                    // Slate hipped roof with a lead ridge and dormers.
                    rect(c, p, 0xFF3E4650, x0, y0, x1, y1);
                    float mx = (x0 + x1) / 2, my = (y0 + y1) / 2;
                    if (x1 - x0 >= y1 - y0) {
                        float in = (y1 - y0) / 2;
                        poly(c, p, 0xFF5A6470, x0 + 1, y0 + 1, x1 - 1, y0 + 1, x1 - in, my, x0 + in, my);
                        poly(c, p, 0xFF4A535E, x0 + 1, y1 - 1, x1 - 1, y1 - 1, x1 - in, my, x0 + in, my);
                        line(c, p, 0xFF9AA2AA, 1.2f, x0 + in, my, x1 - in, my);
                    } else {
                        float in = (x1 - x0) / 2;
                        poly(c, p, 0xFF56606C, x0 + 1, y0 + 1, x0 + 1, y1 - 1, mx, y1 - in, mx, y0 + in);
                        poly(c, p, 0xFF4A535E, x1 - 1, y0 + 1, x1 - 1, y1 - 1, mx, y1 - in, mx, y0 + in);
                        poly(c, p, 0xFF5E6874, x0 + 1, y0 + 1, x1 - 1, y0 + 1, mx, y0 + in);
                        line(c, p, 0xFF9AA2AA, 1.2f, mx, y0 + in, mx, y1 - in);
                    }
                    for (float x = x0 + 8; x < x1 - 8; x += 12) rect(c, p, 0xFFD8D0C0, x, y1 - 6, x + 4, y1 - 3);
                } else {
                    // The tower: a stone square, the four clock faces and a copper spire.
                    float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2, s = (x1 - x0) / 2;
                    rect(c, p, 0xFFB8AE96, x0, y0, x1, y1);
                    rect(c, p, 0xFFCFC5AC, x0 + 2, y0 + 2, x1 - 2, y1 - 2);
                    poly(c, p, 0xFF4E9A84, x0 + 4, y0 + 4, x1 - 4, y0 + 4, cx, cy);
                    poly(c, p, 0xFF3E8270, x1 - 4, y0 + 4, x1 - 4, y1 - 4, cx, cy);
                    poly(c, p, 0xFF2E6A5A, x0 + 4, y1 - 4, x1 - 4, y1 - 4, cx, cy);
                    poly(c, p, 0xFF5AAE96, x0 + 4, y0 + 4, x0 + 4, y1 - 4, cx, cy);
                    circle(c, p, 0xFFE8C860, cx, cy, 1.4f);
                    circle(c, p, 0xFFF4F0E4, cx, y1 + 0.5f, s * 0.35f);
                    line(c, p, 0xFF202020, 0.6f, cx, y1 + 0.5f, cx, y1 + 0.5f - s * 0.25f);
                    line(c, p, 0xFF202020, 0.6f, cx, y1 + 0.5f, cx + s * 0.18f, y1 + 0.5f);
                }
            }
        };
        d.minS = 10;
        d.minL = 10;
        d.base = City.OFFICE;
        d.inside = Variants.I_OFFICE;
        d.districts = Variants.DOWN | Variants.MID | Variants.OLD;
        out.add(d);
    }
}
