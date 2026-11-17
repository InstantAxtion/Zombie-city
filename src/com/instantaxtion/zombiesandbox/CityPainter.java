package com.instantaxtion.zombiesandbox;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.City.*;

/**
 * (27.3) Paints the map: the ground picture and the sharp close-ups (render), every roof (drawBuilding), every
 * decoration (drawDecor), roads, water, terrain and the rest. Taken out of City.java as it stood; the city itself
 * (its tiles, buildings and everything the game asks of it) is `city`.
 */
final class CityPainter extends CityPart {
    CityPainter(City city) {
        super(city);
    }

    /**
     * What a street is surfaced with (cities since 10.24): cobblestones in the old town, concrete slabs in the
     * industrial district, rough chip-seal on the roads out in the country, tarmac everywhere else (and on every
     * main road and bridge).
     */
    int roadSurface(int x, int y) {
        if (city.cfg.civic < 9) return SURF_ASPHALT;
        int i = y * city.w + x;
        if (city.mainRoad[i] || (city.bridge != null && city.bridge[i])) return SURF_ASPHALT;
        // (10.29) The frontage road and the slip roads by the highway are tarmac, not country chip-seal.
        if (city.cfg.civic >= 13 && city.hwyAxis >= 0) {
            int a = (city.hwyAxis == 0 ? y : x) - city.hwyAt;
            if (a >= -8 && a <= 14) return SURF_ASPHALT;
        }
        boolean inTown = x >= city.townX0 && x < city.townX1 && y >= city.townY0 && y < city.townY1;
        if (!inTown) return SURF_CHIPSEAL;
        int dt = city.districtType(x, y);
        if (dt == DT_OLDTOWN) return SURF_COBBLE;
        if (dt == DT_INDUSTRIAL) return SURF_CONCRETE;
        return SURF_ASPHALT;
    }

    // ------------------------------------------------------------------ close-ups

    /** Randomness for drawing, seeded from each tile's position (see tileSeed). */
    final Random prnd = new Random();

    long tileSeed(int x, int y, int pass) {
        return (x * 73856093L) ^ (y * 19349663L) ^ (pass * 83492791L) ^ (city.cfg.seed * 2654435761L);
    }

    /** The tiles to draw: just the close-up's (with its margin) while drawing one, otherwise the whole map. */
    int gx0() { return city.regionOnly ? city.rx0 : 0; }
    int gx1() { return city.regionOnly ? city.rx1 : city.w; }
    int gy1() { return city.regionOnly ? city.ry1 : city.h; }

    /** Draws what stands inside a base (see baseLayout). */
    void drawBaseDecor(Canvas c, Paint p) {
        for (float[] d : city.baseDecor) {
            int kind = (int) d[0];
            float x0 = d[1], y0 = d[2], x1 = d[3], y1 = d[4], w = x1 - x0, h = y1 - y0, cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
            switch (kind) {
                case BD_RUNWAY: {
                    p.setColor(0xFF3A3C3E);
                    c.drawRect(x0, y0, x1, y1, p);
                    p.setColor(0xFFE8E8E8);
                    for (float x = x0 + 14; x < x1 - 14; x += 14) c.drawRect(x, cy - 0.6f, x + 7, cy + 0.6f, p);
                    for (int k = 0; k < 4; k++) {
                        float ty = y0 + 3 + k * (h - 6) / 4f;
                        c.drawRect(x0 + 2, ty, x0 + 9, ty + (h - 6) / 8f, p);
                        c.drawRect(x1 - 9, ty, x1 - 2, ty + (h - 6) / 8f, p);
                    }
                    p.setColor(0xFFE8D24A);
                    c.drawRect(x0, y0, x1, y0 + 0.7f, p);
                    c.drawRect(x0, y1 - 0.7f, x1, y1, p);
                    break;
                }
                case BD_HANGAR: {
                    // An arched roof: ribbed, lighter along its crown.
                    p.setColor(0x60000000);
                    c.drawRect(x0 + 2, y0 + 2, x1 + 2, y1 + 2, p);
                    p.setColor(0xFF7C848A);
                    c.drawRect(x0, y0, x1, y1, p);
                    p.setColor(0xFF9AA2A8);
                    c.drawRect(x0, cy - h * 0.18f, x1, cy + h * 0.18f, p);
                    p.setColor(0xFF6A7076);
                    for (float x = x0 + 4; x < x1; x += 5) c.drawRect(x, y0, x + 0.6f, y1, p);
                    p.setColor(0xFF2A2E32);
                    c.drawRect(x0 + w * 0.15f, y1 - 1.5f, x1 - w * 0.15f, y1, p);
                    break;
                }
                case BD_AIRLINER: {
                    // ((15.10) An airliner at its stand: white fuselage, swept wings, a coloured tail.)
                    drawAirliner(c, p, cx, cy, Math.min(w, h * 1.6f) / 9f, (float) -Math.PI / 2, 0xFF2E6EB8);
                    break;
                }
                case BD_JET: {
                    p.setColor(0xFF6E7A82);
                    c.drawRect(x0, cy - 1.2f, x1, cy + 1.2f, p);
                    c.drawRect(cx - 1.5f, y0, cx + 1.5f, y1, p);
                    c.drawRect(x0, cy - 3f, x0 + 2.5f, cy + 3f, p);
                    p.setColor(0xFF3A4A5A);
                    c.drawRect(x1 - 3, cy - 0.8f, x1 - 1, cy + 0.8f, p);
                    break;
                }
                case BD_HARDSTAND: {
                    p.setColor(0xFF8E9088);
                    c.drawRect(x0, y0, x1, y1, p);
                    p.setColor(0xFF7E8078);
                    for (float x = x0 + 10; x < x1; x += 10) c.drawRect(x, y0, x + 0.5f, y1, p);
                    for (float y = y0 + 10; y < y1; y += 10) c.drawRect(x0, y, x1, y + 0.5f, p);
                    break;
                }
                case BD_TANK: {
                    p.setColor(0x50000000);
                    c.drawRect(x0 + 1.5f, y0 + 1.5f, x1 + 1.5f, y1 + 1.5f, p);
                    p.setColor(0xFF2E3320);
                    c.drawRect(x0, y0, x0 + 2.5f, y1, p);
                    c.drawRect(x1 - 2.5f, y0, x1, y1, p);
                    p.setColor(0xFF4F5A33);
                    c.drawRect(x0 + 2, y0 + 1, x1 - 2, y1 - 1, p);
                    p.setColor(0xFF5F6B3D);
                    c.drawCircle(cx, cy + 1, 3.5f, p);
                    p.setColor(0xFF3A4226);
                    c.drawRect(cx - 0.8f, y0 - 6, cx + 0.8f, cy, p);
                    break;
                }
                case BD_SHED: {
                    p.setColor(0x60000000);
                    c.drawRect(x0 + 2, y0 + 2, x1 + 2, y1 + 2, p);
                    p.setColor(0xFF6E7660);
                    c.drawRect(x0, y0, x1, y1, p);
                    p.setColor(0xFF7E8670);
                    for (float y = y0 + 3; y < y1; y += 6) c.drawRect(x0, y, x1, y + 2.5f, p);
                    p.setColor(0xFF2A2E32);
                    for (float x = x0 + 4; x + 8 < x1; x += 12) c.drawRect(x, y1 - 1.5f, x + 8, y1, p);
                    break;
                }
                case BD_RANGE: {
                    p.setColor(0xFF9A8A62);
                    c.drawRect(x0, y0, x1, y1, p);
                    p.setColor(0xFFC8BC98);
                    int lanes = Math.max(3, (int) (h / 6));
                    for (int k = 1; k < lanes; k++) c.drawRect(x0, y0 + k * h / lanes - 0.3f, x1, y0 + k * h / lanes + 0.3f, p);
                    for (int k = 0; k < lanes; k++) {
                        float ty = y0 + (k + 0.5f) * h / lanes;
                        p.setColor(0xFFF2F2F2);
                        c.drawCircle(x1 - 4, ty, 2f, p);
                        p.setColor(0xFFD02A2A);
                        c.drawCircle(x1 - 4, ty, 0.9f, p);
                    }
                    p.setColor(0xFF5A6670);
                    c.drawRect(x0, y0, x0 + 4, y1, p);
                    p.setColor(0xFF7A6A4A);
                    c.drawRect(x1 - 1.5f, y0, x1, y1, p);
                    break;
                }
                case BD_OBSTACLE: {
                    p.setColor(0xFF8A7A52);
                    c.drawRect(x0, y0, x1, y1, p);
                    p.setColor(0xFF6A4A2A);
                    for (float x = x0 + 4; x < x1 - 2; x += 8) c.drawRect(x, y0 + 2, x + 1.5f, y1 - 2, p);
                    p.setColor(0xFF4A4E52);
                    for (float x = x0 + 8; x < x1 - 2; x += 16) {
                        c.drawRect(x, cy - 3, x + 4, cy + 3, p);
                    }
                    break;
                }
                case BD_RADAR: {
                    p.setColor(0x60000000);
                    c.drawCircle(cx + 2, cy + 2, w / 2, p);
                    p.setColor(0xFFE8ECEE);
                    c.drawCircle(cx, cy, w / 2, p);
                    p.setColor(0xFFC8CED2);
                    for (int k = 1; k < 4; k++) c.drawCircle(cx + w * 0.1f, cy + w * 0.1f, w / 2 * (1 - k * 0.22f), p);
                    p.setColor(0xFFFFFFFF);
                    c.drawCircle(cx - w * 0.15f, cy - w * 0.15f, w * 0.12f, p);
                    break;
                }
                case BD_DISH: {
                    p.setColor(0xFF6A7076);
                    c.drawRect(cx - 3, cy - 3, cx + 3, cy + 3, p);
                    p.setColor(0xFFD8DCE0);
                    c.drawCircle(cx, cy, w / 2, p);
                    p.setColor(0xFFB8BEC4);
                    c.drawCircle(cx + 1, cy + 1, w / 2 * 0.7f, p);
                    p.setColor(0xFF4A4E52);
                    c.drawCircle(cx, cy, 1.5f, p);
                    break;
                }
                case BD_MAST: {
                    p.setColor(0xFFC8302A);
                    c.drawRect(x0, y0, x1, y1, p);
                    p.setColor(0xFFF2F2F2);
                    c.drawRect(x0 + w * 0.25f, y0 + h * 0.25f, x1 - w * 0.25f, y1 - h * 0.25f, p);
                    break;
                }
                case BD_SANDBAGS: {
                    p.setColor(0xFF8E7A50);
                    c.drawRect(x0, y0, x1, y0 + 3, p);
                    c.drawRect(x0, y0, x0 + 3, y1, p);
                    c.drawRect(x1 - 3, y0, x1, y1, p);
                    p.setColor(0xFFC4AA78);
                    for (float x = x0; x < x1; x += 3) c.drawRect(x + 0.3f, y0 + 0.3f, x + 2.7f, y0 + 2.7f, p);
                    break;
                }
                case BD_KILLHOUSE: {
                    // Walls without a roof: rooms and doorways seen from above.
                    p.setColor(0xFF9A9488);
                    c.drawRect(x0, y0, x1, y1, p);
                    p.setColor(0xFF5A5650);
                    float t = 1.4f;
                    c.drawRect(x0, y0, x1, y0 + t, p);
                    c.drawRect(x0, y1 - t, x1, y1, p);
                    c.drawRect(x0, y0, x0 + t, y1, p);
                    c.drawRect(x1 - t, y0, x1, y1, p);
                    for (float x = x0 + 14; x < x1 - 6; x += 14) {
                        c.drawRect(x, y0, x + t, cy - 3, p);
                        c.drawRect(x, cy + 3, x + t, y1, p);
                    }
                    c.drawRect(x0, cy - t / 2, cx - 3, cy + t / 2, p);
                    c.drawRect(cx + 3, cy - t / 2, x1, cy + t / 2, p);
                    break;
                }
                case BD_FUEL: {
                    p.setColor(0x60000000);
                    c.drawCircle(cx + 2, cy + 2, w / 2, p);
                    p.setColor(0xFFD8DCD0);
                    c.drawCircle(cx, cy, w / 2, p);
                    p.setColor(0xFFB8BCB0);
                    c.drawCircle(cx, cy, w / 2 - 2, p);
                    p.setColor(0xFF8A8E84);
                    c.drawCircle(cx, cy, 1.6f, p);
                    break;
                }
                case BD_CONTAINER: {
                    int[] cols = {0xFFB03A2E, 0xFF2E5FB0, 0xFF3C7A4E, 0xFFD4A21C, 0xFF6A6E74};
                    p.setColor(cols[(int) d[5] % cols.length]);
                    c.drawRect(x0, y0, x1, y1, p);
                    p.setColor(0x30000000);
                    for (float x = x0 + 1.5f; x < x1; x += 2) c.drawRect(x, y0, x + 0.5f, y1, p);
                    break;
                }
                case BD_FLAG: {
                    p.setColor(0xFFE8E8E8);
                    c.drawCircle(cx, cy, 2.5f, p);
                    p.setColor(0xFF4A4E52);
                    c.drawCircle(cx, cy, 1f, p);
                    p.setColor(0xFFD02A2A);
                    c.drawRect(cx, cy - 6, cx + 8, cy - 1.5f, p);
                    break;
                }
                default:
                    break;
            }
        }
    }

    /**
     * (15.6) The prison wall: two tiles of grey concrete standing high (a long shadow to the lower right, a pale top),
     * coils of razor wire all along it, watchtowers on it, and the steel gate - yellow and black, shut, or its two
     * leaves swung back once the prison has fallen. And the barrier where the causeway meets the seafront.
     */
    void drawPrisonWall(Canvas c, Paint p) {
        int x0 = city.prison[0], y0 = city.prison[1], x1 = city.prison[2], y1 = city.prison[3];
        int g0 = city.prisonGate[0], g1 = city.prisonGate[2];
        boolean open = city.tiles[city.prisonGate[1] * city.w + g0] != ROCK;
        float wx0 = x0 * T, wy0 = y0 * T, wx1 = x1 * T, wy1 = y1 * T, th = PRISON_WALL * T;
        // The shadow it throws.
        p.setColor(0x55000000);
        c.drawRect(wx0 + 6, wy1, wx1 + 6, wy1 + 6, p);
        c.drawRect(wx1, wy0 + 6, wx1 + 6, wy1, p);
        c.drawRect(wx0 + th, wy0 + th, wx1 - th, wy0 + th + 5, p);
        c.drawRect(wx0 + th, wy0 + th, wx0 + th + 5, wy1 - th, p);
        // The wall itself: sides, then its pale top.
        for (int pass = 0; pass < 2; pass++) {
            p.setColor(pass == 0 ? 0xFF8A8880 : 0xFFC4C2BA);
            float in = pass == 0 ? 0 : 2.5f;
            c.drawRect(wx0 + in, wy0 + in, wx1 - in, wy0 + th - in, p);
            c.drawRect(wx0 + in, wy1 - th + in, wx1 - in, wy1 - in, p);
            c.drawRect(wx0 + in, wy0 + in, wx0 + th - in, wy1 - in, p);
            c.drawRect(wx1 - th + in, wy0 + in, wx1 - in, wy1 - in, p);
        }
        // Razor wire along the middle of the top, all the way round.
        p.setColor(0xFFE8ECF0);
        float mid = th / 2;
        for (float x = wx0 + 4; x < wx1 - 4; x += 4) {
            if (x > g0 * T - 2 && x < g1 * T + 2) continue;
            c.drawCircle(x, wy0 + mid, 1.5f, p);
            c.drawCircle(x, wy1 - mid, 1.5f, p);
        }
        for (float y = wy0 + 4; y < wy1 - 4; y += 4) {
            c.drawCircle(wx0 + mid, y, 1.5f, p);
            c.drawCircle(wx1 - mid, y, 1.5f, p);
        }
        p.setColor(0xFFC4C2BA);
        for (float x = wx0 + 4; x < wx1 - 4; x += 4) {
            if (x > g0 * T - 2 && x < g1 * T + 2) continue;
            c.drawCircle(x, wy0 + mid, 0.8f, p);
            c.drawCircle(x, wy1 - mid, 0.8f, p);
        }
        for (float y = wy0 + 4; y < wy1 - 4; y += 4) {
            c.drawCircle(wx0 + mid, y, 0.8f, p);
            c.drawCircle(wx1 - mid, y, 0.8f, p);
        }
        // The gate.
        float gx0 = g0 * T, gx1 = g1 * T, gy0 = city.prisonGate[1] * T, gy1 = city.prisonGate[3] * T;
        if (!open) {
            p.setColor(0xFF2A2C30);
            c.drawRect(gx0, gy0, gx1, gy1, p);
            for (float x = gx0; x < gx1; x += 6) {
                p.setColor(((int) ((x - gx0) / 6)) % 2 == 0 ? 0xFFE8C020 : 0xFF1E1E1E);
                c.drawRect(x, gy0 + 2, Math.min(gx1, x + 6), gy0 + 6, p);
                c.drawRect(x, gy1 - 6, Math.min(gx1, x + 6), gy1 - 2, p);
            }
            p.setColor(0xFF5A6068);
            c.drawRect((gx0 + gx1) / 2 - 0.6f, gy0, (gx0 + gx1) / 2 + 0.6f, gy1, p);
        } else {
            // Its two leaves swung back inside the wall.
            p.setColor(0xFF2A2C30);
            c.drawRect(gx0 - 1, gy1, gx0 + 2.5f, gy1 + 22, p);
            c.drawRect(gx1 - 2.5f, gy1, gx1 + 1, gy1 + 22, p);
            p.setColor(0xFFE8C020);
            c.drawRect(gx0 - 1, gy1 + 4, gx0 + 2.5f, gy1 + 8, p);
            c.drawRect(gx1 - 2.5f, gy1 + 4, gx1 + 1, gy1 + 8, p);
        }
        // The watchtowers: a square cabin up on the wall, a dark roof, glass all round, a searchlight.
        for (float[] t : city.prisonTowers) {
            float s = 15;
            p.setColor(0x60000000);
            c.drawRect(t[0] - s + 6, t[1] - s + 6, t[0] + s + 6, t[1] + s + 6, p);
            p.setColor(0xFF5E5C56);
            c.drawRect(t[0] - s, t[1] - s, t[0] + s, t[1] + s, p);
            p.setColor(0xFF8FB8D0);
            c.drawRect(t[0] - s + 2, t[1] - s + 2, t[0] + s - 2, t[1] + s - 2, p);
            p.setColor(0xFF3A3C40);
            c.drawRect(t[0] - s + 4, t[1] - s + 4, t[0] + s - 4, t[1] + s - 4, p);
            p.setColor(0xFFFFF2B0);
            c.drawCircle(t[0], t[1], 2.4f, p);
        }
        // The barrier where the causeway meets the seafront: a red and white arm across the road, and its booth.
        if (city.prisonBarrier != null) {
            float bx0 = city.prisonBarrier[0], by = city.prisonBarrier[1], bx1 = city.prisonBarrier[2];
            for (float x = bx0; x < bx1; x += 5) {
                p.setColor(((int) ((x - bx0) / 5)) % 2 == 0 ? 0xFFE03A2A : 0xFFF2F2F2);
                c.drawRect(x, by - 1.2f, Math.min(bx1, x + 5), by + 1.2f, p);
            }
            p.setColor(0xFF4A4E52);
            c.drawCircle(bx0, by, 2, p);
            p.setColor(0x50000000);
            c.drawRect(bx0 - 13 + 2, by - 6 + 2, bx0 - 3 + 2, by + 4 + 2, p);
            p.setColor(0xFFB8B4AA);
            c.drawRect(bx0 - 13, by - 6, bx0 - 3, by + 4, p);
            p.setColor(0xFF8FB8D0);
            c.drawRect(bx0 - 11, by - 4, bx0 - 5, by + 2, p);
        }
    }

    /** The colour of a tile of open water: darker the further it is from the shore. */
    int waterColor(int x, int y) {
        int base = city.waterKind == CityConfig.W_LAKE || city.waterKind == CityConfig.W_NONE ? 0xFF2F6E80 : 0xFF2A6390;
        int land = 9;
        for (int r = 1; r <= 8 && land == 9; r++)
            for (int k = -r; k <= r && land == 9; k += r)
                for (int[] d : new int[][]{{k, -r}, {k, r}, {-r, k}, {r, k}}) {
                    int nx = x + d[0], ny = y + d[1];
                    if (nx >= 0 && ny >= 0 && nx < city.w && ny < city.h && city.tiles[ny * city.w + nx] != WATER) {
                        land = r;
                        break;
                    }
                }
        // A stream you can wade is shallow: the bed shows through.
        if (city.isFord(y * city.w + x)) return lighten(base, 0.14f);
        return darken(base, 1 - Math.min(0.22f, (land - 1) * 0.028f));
    }

    void drawRubble(final Building b) {
        city.paintMap(b.x0, b.y0, b.x1, b.y1, new MapPainter() {
            @Override
            public void paint(Canvas c) {
                drawRubble(c, b);
            }
        });
        city.changed(b.x0, b.y0, b.x1, b.y1);
    }

    void drawRubble(Canvas c, Building b) {
        Paint p = new Paint();
        p.setAntiAlias(true);
        Random r = new Random(b.seed * 31L + 7);
        p.setColor(0xFF6E6860);
        c.drawRect(b.x0, b.y0, b.x1, b.y1, p);
        // Broken slabs and bricks, with the old wall colour mixed in.
        int n = (int) ((b.x1 - b.x0) * (b.y1 - b.y0) / 30);
        for (int k = 0; k < n; k++) {
            float x = b.x0 + r.nextFloat() * (b.x1 - b.x0), y = b.y0 + r.nextFloat() * (b.y1 - b.y0);
            float s = 1 + r.nextFloat() * 3.5f;
            int roll = r.nextInt(4);
            p.setColor(roll == 0 ? darken(b.wall, 0.8f) : roll == 1 ? darken(b.roof, 0.7f) : roll == 2 ? 0xFF8C867C : 0xFF4E4A45);
            c.drawRect(x - s, y - s * 0.6f, x + s, y + s * 0.6f, p);
        }
        p.setColor(0x60101010);
        for (int k = 0; k < n / 6; k++)
            c.drawCircle(b.x0 + r.nextFloat() * (b.x1 - b.x0), b.y0 + r.nextFloat() * (b.y1 - b.y0), 2 + r.nextFloat() * 4, p);
        // Stumps of the outer walls.
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2f);
        p.setColor(darken(b.wall, 0.6f));
        c.drawLine(b.x0 + 1, b.y0 + 1, b.x0 + (b.x1 - b.x0) * (0.2f + r.nextFloat() * 0.3f), b.y0 + 1, p);
        c.drawLine(b.x1 - 1, b.y1 - 1, b.x1 - (b.x1 - b.x0) * (0.2f + r.nextFloat() * 0.3f), b.y1 - 1, p);
        c.drawLine(b.x0 + 1, b.y1 - 1, b.x0 + 1, b.y1 - (b.y1 - b.y0) * (0.2f + r.nextFloat() * 0.4f), p);
        p.setStyle(Paint.Style.FILL);
    }

    void render(Canvas c) {
        Paint p = new Paint();
        p.setAntiAlias(true);
        c.drawColor(0xFF1B1C1F);
        // (Drawing just one region of the map, at a higher resolution: only the tiles in and around it.)
        int X0 = city.regionOnly ? city.rx0 : 0, Y0 = city.regionOnly ? city.ry0 : 0, X1 = city.regionOnly ? city.rx1 : city.w, Y1 = city.regionOnly ? city.ry1 : city.h;

        // (The ground tiles are drawn with square edges, so no seams show between them when the map is drawn
        // smaller than full size.)
        p.setAntiAlias(false);
        paintTiles(c, p, X0, Y0, X1, Y1);
        paintGroundTexture(c, p, X0, Y0, X1, Y1);
        paintRoadsAndWater(c, p, X0, Y0, X1, Y1);
        paintPathsAndDecor(c, p, X0, Y0, X1, Y1);
        paintBuildings(c, p, X0, Y0, X1, Y1);
        paintStreetThings(c, p, X0, Y0, X1, Y1);
    }

    /**
     * ((27.3) A step of render.) Every tile in its plain colour: road, pavement, grass, water and the rest
     * (square-edged, so no seams show).
     */
    private void paintTiles(Canvas c, Paint p, int X0, int Y0, int X1, int Y1) {
        // ((22.8) Neighbourhoods paved in their own colour.)
        boolean paved = city.cfg.neighbourhoods() && city.districtAt != null && Neighbourhoods.any(city.districts);
        for (int y = Y0; y < Y1; y++) {
            // ((22.0) A run of tiles the same colour along a row goes down as one rectangle, not one each: the same
            // picture, from a tenth of the drawing.)
            int runCol = 0, runX = -1;
            for (int x = X0; x < X1; x++) {
                byte t = city.tiles[y * city.w + x];
                int col;
                boolean real = city.drawnRealistic;
                switch (t) {
                    case SIDEWALK: col = city.country.pavement != 0 ? city.country.pavement : real ? 0xFF96938C : 0xFF8F8D87; break;
                    case GRASS: case TREE: col = city.desert ? grassColour(x, y) : city.country.grass != 0 ? city.country.grass : real ? 0xFF4F6F36 : 0xFF4C7837; break;
                    case PLAZA: case STATUE: col = city.country.plaza != 0 ? city.country.plaza : real ? 0xFFB2A58A : 0xFFB3A487; break;
                    case LOT: col = real ? 0xFF46474A : 0xFF48494D; break;
                    case BUILDING: col = city.country.pavement != 0 ? city.country.pavement : real ? 0xFF96938C : 0xFF8F8D87; break;
                    case BASE: case FENCE: col = 0xFF6A6E5E; break;
                    case DIRT: col = real ? 0xFF8A7252 : 0xFF8C7456; break;
                    case WATER: col = waterColor(x, y); break;
                    case SAND: col = sandColor(); break;
                    case PIER: col = 0xFF8C6C4A; break;
                    case ROCK: col = 0xFF7E796E; break;
                    case TRAIL: col = city.desert ? grassColour(x, y) : city.country.grass != 0 ? city.country.grass : real ? 0xFF4F6F36 : 0xFF4C7837; break;
                    default: col = real ? 0xFF38393C : 0xFF3A3D43; break;
                }
                if (paved && (t == SIDEWALK || t == BUILDING || t == PLAZA)) {
                    Neighbourhoods.Theme th = city.districts.get(city.districtAt.get(y * city.w + x)).theme;
                    if (th != null && th.paving != 0) col = t == PLAZA ? mix(th.paving, col, 0.5f) : th.paving;
                }
                if (t == TREE && city.isPlazaTree(x, y)) col = 0xFFB3A487;
                // (10.40) Lanes and drives are drawn smoothly over the ground afterwards (drawLanes).
                if (t == DIRT || (t == LOT && city.drive != null && city.drive[y * city.w + x])) col = laneGround(x, y);
                if (t == CAR && isLotCar(x, y)) col = 0xFF48494D;
                if (t == CAR && city.carKind.get(y * city.w + x) == 2) col = 0xFF6A6E5E;
                if (city.slipTile != null && city.slipTile[y * city.w + x] && (t == ROAD || t == CAR))
                    col = city.desert ? grassColour(x, y) : city.country.grass != 0 ? city.country.grass : real ? 0xFF4F6F36 : 0xFF4C7837;
                else if (city.curveTile != null && city.curveTile[y * city.w + x] && (t == ROAD || t == CAR))
                    col = city.country.pavement != 0 ? city.country.pavement : real ? 0xFF96938C : 0xFF8F8D87;
                else if (t == ROAD || (t == CAR && col != 0xFF48494D && col != 0xFF6A6E5E)) {
                    int sf = roadSurface(x, y);
                    if (sf == SURF_COBBLE) col = 0xFF5F5A55;
                    else if (sf == SURF_CONCRETE) col = 0xFF77777A;
                    else if (sf == SURF_CHIPSEAL) col = 0xFF4A4946;
                }
                if (runX >= 0 && col != runCol) {
                    p.setColor(runCol);
                    // (A hair over each edge, so no seams show between tiles when the map is drawn smaller.)
                    c.drawRect(runX * T, y * T, x * T + 0.75f, y * T + T + 0.75f, p);
                    runX = -1;
                }
                if (runX < 0) {
                    runX = x;
                    runCol = col;
                }
            }
            if (runX >= 0) {
                p.setColor(runCol);
                c.drawRect(runX * T, y * T, X1 * T + 0.75f, y * T + T + 0.75f, p);
            }
        }
        p.setAntiAlias(true);
    }

    /**
     * ((27.3) A step of render.) The texture over the ground: speckles, cracks, tufts, ripples - each tile's own
     * from its place on the map.
     */
    private void paintGroundTexture(Canvas c, Paint p, int X0, int Y0, int X1, int Y1) {
        // Ground texture.
        p.setStrokeWidth(1f);
        for (int y = Y0; y < Y1; y++) {
            for (int x = X0; x < X1; x++) {
                byte t = city.tiles[y * city.w + x];
                float fx = x * T, fy = y * T;
                // Each tile's own randomness, so it looks the same however much of the map is drawn.
                prnd.setSeed(tileSeed(x, y, 1));
                if (t == SIDEWALK) {
                    // Paving slabs, a few of them newer or stained.
                    float roll = prnd.nextFloat();
                    if (roll < 0.08f) {
                        p.setColor(roll < 0.04f ? 0xFF999791 : 0xFF84827C);
                        c.drawRect(fx + (roll < 0.06f ? 0 : T / 2f), fy, fx + (roll < 0.06f ? T / 2f : T), fy + T, p);
                    }
                    p.setColor(0xFF7E7C77);
                    c.drawLine(fx, fy, fx + T, fy, p);
                    c.drawLine(fx, fy, fx, fy + T, p);
                    c.drawLine(fx + T / 2f, fy, fx + T / 2f, fy + T, p);
                } else if (t == ROCK) {
                    // Bare rock: boulders with a lit top, cracks and scree.
                    for (int k = 0; k < 3; k++) {
                        float bx = fx + 2 + prnd.nextFloat() * (T - 4), by = fy + 2 + prnd.nextFloat() * (T - 4), br = 1.5f + prnd.nextFloat() * 3;
                        p.setColor(0xFF5E5A52);
                        c.drawCircle(bx + 0.8f, by + 0.8f, br, p);
                        p.setColor(prnd.nextBoolean() ? 0xFF97918A : 0xFF8A857C);
                        c.drawCircle(bx, by, br, p);
                    }
                    p.setColor(0xFF5A564E);
                    float cx0 = fx + prnd.nextFloat() * T, cy0 = fy + prnd.nextFloat() * T;
                    c.drawLine(cx0, cy0, cx0 + prnd.nextFloat() * 6 - 3, cy0 + prnd.nextFloat() * 6 - 3, p);
                } else if ((t == GRASS || t == TREE || t == TRAIL) && city.desert && !lushAt(x, y)) {
                    // ((22.9) Sand: grit and pebbles, now and then a dry tuft or a stone.)
                    for (int k = 0; k < 5; k++) {
                        p.setColor(prnd.nextBoolean() ? 0xFFB8986A : 0xFFE6D2A2);
                        float sx = fx + prnd.nextFloat() * T, sy = fy + prnd.nextFloat() * T;
                        if (city.fine) c.drawCircle(sx, sy, 0.8f, p);
                    }
                    if (prnd.nextFloat() < 0.1f) {
                        p.setColor(0xFF9A8A58);
                        float gx = fx + prnd.nextFloat() * T, gy = fy + prnd.nextFloat() * T;
                        c.drawLine(gx, gy, gx - 1.5f, gy - 2, p);
                        c.drawLine(gx, gy, gx + 1.5f, gy - 2, p);
                        c.drawLine(gx, gy, gx, gy - 2.5f, p);
                    }
                    if (prnd.nextFloat() < 0.04f) {
                        float bx = fx + 3 + prnd.nextFloat() * (T - 6), by = fy + 3 + prnd.nextFloat() * (T - 6);
                        p.setColor(0x40000000);
                        c.drawCircle(bx + 0.6f, by + 0.6f, 1.6f, p);
                        p.setColor(0xFFA08A70);
                        c.drawCircle(bx, by, 1.5f, p);
                    }
                    // The green of watered ground beside spills over its edge in soft scallops.
                    for (int k = 0; k < 4; k++) {
                        int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = y + (k == 2 ? 1 : k == 3 ? -1 : 0);
                        if (nx < 0 || ny < 0 || nx >= city.w || ny >= city.h || !lushAt(nx, ny)) continue;
                        byte o = city.tiles[ny * city.w + nx];
                        if (o != GRASS && o != TREE && o != TRAIL) continue;
                        p.setColor(grassColour(nx, ny));
                        float ex = fx + T / 2f + (nx - x) * T / 2f, ey = fy + T / 2f + (ny - y) * T / 2f;
                        float along = (prnd.nextFloat() - 0.5f) * T * 0.6f;
                        c.drawCircle(ex + (ny != y ? along : 0), ey + (nx != x ? along : 0), 4 + prnd.nextFloat() * 5, p);
                    }
                } else if (t == GRASS || t == TREE || t == TRAIL) {
                    for (int k = 0; k < 6; k++) {
                        p.setColor(prnd.nextBoolean() ? 0xFF578A3F : 0xFF426B30);
                        float sx = fx + prnd.nextFloat() * T, sy = fy + prnd.nextFloat() * T;
                        if (city.fine) c.drawCircle(sx, sy, 0.9f, p);
                    }
                    // Tufts and the odd wildflower.
                    if (prnd.nextFloat() < 0.35f) {
                        p.setColor(0xFF3A6128);
                        float gx = fx + prnd.nextFloat() * T, gy = fy + prnd.nextFloat() * T;
                        if (city.fine) {
                            c.drawLine(gx, gy, gx - 1, gy - 2, p);
                            c.drawLine(gx, gy, gx + 1, gy - 2, p);
                        }
                    }
                    if (prnd.nextFloat() < 0.05f) {
                        int[] flowers = {0xFFF2E86B, 0xFFF2F2F2, 0xFFE87BB0, 0xFFB08AE8};
                        p.setColor(flowers[prnd.nextInt(flowers.length)]);
                        for (int k = 0; k < 3; k++)
                            c.drawCircle(fx + prnd.nextFloat() * T, fy + prnd.nextFloat() * T, 0.8f, p);
                    }
                } else if (t == DIRT && false) {
                    // Packed earth: wheel ruts and pebbles. (Since 10.40 drawn smoothly in drawLanes.)
                    p.setColor(0xFF7A6446);
                    boolean vert = (y > 0 && city.tiles[(y - 1) * city.w + x] == DIRT) && (y < city.h - 1 && city.tiles[(y + 1) * city.w + x] == DIRT);
                    if (vert) {
                        c.drawRect(fx + 4, fy, fx + 6, fy + T, p);
                        c.drawRect(fx + 10, fy, fx + 12, fy + T, p);
                    } else {
                        c.drawRect(fx, fy + 4, fx + T, fy + 6, p);
                        c.drawRect(fx, fy + 10, fx + T, fy + 12, p);
                    }
                    for (int k = 0; k < 4; k++) {
                        p.setColor(prnd.nextBoolean() ? 0xFFA08A68 : 0xFF6E5A40);
                        c.drawCircle(fx + prnd.nextFloat() * T, fy + prnd.nextFloat() * T, 0.7f, p);
                    }
                } else if (t == PLAZA) {
                    p.setColor(0xFFA29376);
                    c.drawLine(fx, fy, fx + T, fy, p);
                    c.drawLine(fx, fy, fx, fy + T, p);
                } else if ((t == ROAD || t == CAR) && city.curveTile != null && city.curveTile[y * city.w + x]) {
                    // (Under a curved street: drawn smoothly afterwards.)
                } else if ((t == ROAD || t == CAR) && roadSurface(x, y) != SURF_ASPHALT) {
                    int sf = roadSurface(x, y);
                    if (sf == SURF_COBBLE) {
                        // Setts in courses, each row offset by half a stone.
                        p.setColor(0xFF4A4642);
                        for (int r = 0; r < 4; r++) {
                            float ry = fy + r * T / 4f;
                            c.drawLine(fx, ry, fx + T, ry, p);
                            float off = (r + y * 4) % 2 == 0 ? 0 : T / 6f;
                            for (float sx = fx + off; sx < fx + T; sx += T / 3f) c.drawLine(sx, ry, sx, ry + T / 4f, p);
                        }
                        p.setColor(0x18FFFFFF);
                        for (int k = 0; k < 3; k++) c.drawCircle(fx + prnd.nextFloat() * T, fy + prnd.nextFloat() * T, 1.2f, p);
                    } else if (sf == SURF_CONCRETE) {
                        // Slabs with tar-filled joints and the odd stain.
                        p.setColor(0xFF5C5C60);
                        if (x % 2 == 0) c.drawLine(fx, fy, fx, fy + T, p);
                        if (y % 2 == 0) c.drawLine(fx, fy, fx + T, fy, p);
                        if (prnd.nextFloat() < 0.15f) {
                            p.setColor(0x22000000);
                            c.drawCircle(fx + prnd.nextFloat() * T, fy + prnd.nextFloat() * T, 2 + prnd.nextFloat() * 3, p);
                        }
                    } else {
                        // Chip-seal: loose grey and brown stone chips, a worn edge.
                        for (int k = 0; k < 7; k++) {
                            p.setColor(prnd.nextBoolean() ? 0xFF65625C : 0xFF3A3936);
                            c.drawCircle(fx + prnd.nextFloat() * T, fy + prnd.nextFloat() * T, 0.7f, p);
                        }
                    }
                } else if (t == ROAD || t == CAR) {
                    if (prnd.nextFloat() < 0.25f) {
                        p.setColor(0x22000000);
                        c.drawCircle(fx + prnd.nextFloat() * T, fy + prnd.nextFloat() * T, 1 + prnd.nextFloat() * 3, p);
                    }
                    float roll = prnd.nextFloat();
                    if (roll < 0.03f) {
                        // Patched asphalt.
                        p.setColor(0xFF33363B);
                        float pw = 5 + prnd.nextFloat() * 8, ph = 4 + prnd.nextFloat() * 6;
                        float px = fx + prnd.nextFloat() * (T - pw), py = fy + prnd.nextFloat() * (T - ph);
                        c.drawRect(px, py, px + pw, py + ph, p);
                    } else if (roll < 0.07f) {
                        // Cracks.
                        p.setColor(0xFF2A2C30);
                        float cx0 = fx + prnd.nextFloat() * T, cy0 = fy + prnd.nextFloat() * T;
                        float cx1 = cx0 + prnd.nextFloat() * 8 - 4, cy1 = cy0 + prnd.nextFloat() * 8 - 4;
                        c.drawLine(cx0, cy0, cx1, cy1, p);
                        c.drawLine(cx1, cy1, cx1 + prnd.nextFloat() * 6 - 3, cy1 + prnd.nextFloat() * 6 - 3, p);
                    } else if (roll < 0.085f && t == ROAD) {
                        // Manhole cover.
                        float mx = fx + 4 + prnd.nextFloat() * (T - 8), my = fy + 4 + prnd.nextFloat() * (T - 8);
                        p.setColor(0xFF26282B);
                        c.drawCircle(mx, my, 2.6f, p);
                        p.setColor(0xFF4A4C50);
                        c.drawCircle(mx, my, 2f, p);
                        p.setColor(0xFF34363A);
                        c.drawLine(mx - 1.5f, my, mx + 1.5f, my, p);
                        c.drawLine(mx, my - 1.5f, mx, my + 1.5f, p);
                    }
                }
            }
        }
    }

    /**
     * ((27.3) A step of render.) Lanes, curves, road markings, the highway, the railway, kerbs, then water and the
     * lie of the land over them.
     */
    private void paintRoadsAndWater(Canvas c, Paint p, int X0, int Y0, int X1, int Y1) {
        if (city.drawnRealistic) drawRealGround(c, p);
        drawLanes(c, p, X0, Y0, X1, Y1);
        drawCurves(c, p);
        drawRoadMarkings(c, p);
        drawRoadPatches(c, p);
        drawHighway(c, p);
        drawCheckpoints(c, p);
        drawRail(c, p);
        drawStreetFurniture(c, p);
        drawParkingLines(c, p);
        if (city.drawnRealistic) drawKerbs(c, p);
        if (city.waterKind != CityConfig.W_NONE || city.elev != null) drawWater(c, p, X0, Y0, X1, Y1);
        // ((22.1) River City's banks rounded off even without the lie of the land.)
        if (city.waterKind == CityConfig.W_RIVER && city.elev == null) roundBanks(c, p, X0, Y0, X1, Y1);
        if (city.riverC != null) smoothRiver(c, p, X0, Y0, X1, Y1);
        drawTerrain(c, p, X0, Y0, X1, Y1);
        roundCorners(c, p);
        drawBaseDetails(c, p);
    }

    /**
     * ((27.3) A step of render.) Footpaths, and whatever lies on the ground (courts, gardens, graves, crops...).
     */
    private void paintPathsAndDecor(Canvas c, Paint p, int X0, int Y0, int X1, int Y1) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        for (float[] pa : city.paths) {
            // (Kept inside its own park since 10.26: the paths used to run on over the pavement and the road.)
            boolean clip = pa.length >= 8;
            if (clip) {
                c.save();
                c.clipRect(pa[4] + 2, pa[5] + 2, pa[6] - 2, pa[7] - 2);
            }
            p.setStrokeWidth(8f);
            p.setColor(0xFFB3A487);
            c.drawOval(new RectF(pa[0] - pa[2], pa[1] - pa[3], pa[0] + pa[2], pa[1] + pa[3]), p);
            c.drawLine(pa[0], pa[1] - pa[3] * 1.7f, pa[0], pa[1] - pa[3], p);
            c.drawLine(pa[0], pa[1] + pa[3], pa[0], pa[1] + pa[3] * 1.7f, p);
            c.drawLine(pa[0] - pa[2] * 1.6f, pa[1], pa[0] - pa[2], pa[1], p);
            c.drawLine(pa[0] + pa[2], pa[1], pa[0] + pa[2] * 1.6f, pa[1], p);
            if (clip) c.restore();
        }
        p.setStyle(Paint.Style.FILL);

        for (float[] d : city.decor) if (!city.offRegion(d[1], d[2], d[3], d[4])) drawGroundDecor(c, p, d);
    }

    /**
     * ((27.3) A step of render.) The buildings: their shadows, every roof (drawBuilding), and the light and shade on
     * the roofs' edges.
     */
    private void paintBuildings(Canvas c, Paint p, int X0, int Y0, int X1, int Y1) {
        // Building shadows (longer for taller buildings), then roofs. The roof art is drawn at the
        // footprint and GameView lifts it to the building's height.
        Path shadow = new Path();
        if (city.drawnRealistic) {
            // Soft light: ambient darkening hugging each building, and a pale penumbra past the shadow's edge.
            for (Building b : city.buildings) {
                if (city.offRegion(b.x0, b.y0, b.x1, b.y1)) continue;
                for (int k = 3; k >= 1; k--) {
                    p.setColor(0x16000000);
                    c.drawRect(b.x0 - k * 1.6f, b.y0 - k * 1.6f, b.x1 + k * 1.6f, b.y1 + k * 1.6f, p);
                }
                float sx = b.height * 0.28f * 1.12f + 2, sy = b.height * 0.38f * 1.12f + 2;
                shadow.reset();
                shadow.moveTo(b.x0 - 1, b.y0 - 1);
                shadow.lineTo(b.x1 + 1, b.y0 - 1);
                shadow.lineTo(b.x1 + sx, b.y0 + sy);
                shadow.lineTo(b.x1 + sx, b.y1 + sy);
                shadow.lineTo(b.x0 + sx, b.y1 + sy);
                shadow.lineTo(b.x0 - 1, b.y1 + 1);
                shadow.close();
                p.setColor(0x22000000);
                c.drawPath(shadow, p);
            }
        }
        p.setColor(city.drawnRealistic ? 0x46000000 : 0x50000000);
        for (Building b : city.buildings) {
            if (city.offRegion(b.x0, b.y0, b.x1, b.y1)) continue;
            float sx = b.height * 0.28f, sy = b.height * 0.38f;
            shadow.reset();
            shadow.moveTo(b.x0, b.y0);
            shadow.lineTo(b.x1, b.y0);
            shadow.lineTo(b.x1 + sx, b.y0 + sy);
            shadow.lineTo(b.x1 + sx, b.y1 + sy);
            shadow.lineTo(b.x0 + sx, b.y1 + sy);
            shadow.lineTo(b.x0, b.y1);
            shadow.close();
            c.drawPath(shadow, p);
        }
        for (int[] b : city.buildingLots) if (!city.offRegion(b[0] * T, b[1] * T, (b[0] + b[2]) * T, (b[1] + b[3]) * T)) drawBuilding(c, p, b);
        if (city.drawnRealistic)
            for (int[] b : city.buildingLots) if (!city.offRegion(b[0] * T, b[1] * T, (b[0] + b[2]) * T, (b[1] + b[3]) * T)) weatherRoof(c, p, b);
        // ((11.0) Every roof edge catches the light from the north-west and falls into shade on the far side, so
        // the roofs stand up off the ground.)
        p.setStyle(Paint.Style.STROKE);
        for (int[] b : city.buildingLots) {
            float x0 = b[0] * T, y0 = b[1] * T, x1 = (b[0] + b[2]) * T, y1 = (b[1] + b[3]) * T;
            if (city.offRegion(x0, y0, x1, y1)) continue;
            p.setStrokeWidth(1.2f);
            p.setColor(0x34FFFFFF);
            c.drawLine(x0 + 0.6f, y0 + 0.6f, x1 - 0.6f, y0 + 0.6f, p);
            c.drawLine(x0 + 0.6f, y0 + 0.6f, x0 + 0.6f, y1 - 0.6f, p);
            p.setStrokeWidth(1.4f);
            p.setColor(0x40000000);
            c.drawLine(x0 + 0.7f, y1 - 0.7f, x1 - 0.7f, y1 - 0.7f, p);
            c.drawLine(x1 - 0.7f, y0 + 0.7f, x1 - 0.7f, y1 - 0.7f, p);
        }
        p.setStyle(Paint.Style.FILL);
    }

    /**
     * ((27.3) A step of render.) What stands in the street: statues, parked cars, pumps and their canopies, lamps,
     * and the street trees.
     */
    private void paintStreetThings(Canvas c, Paint p, int X0, int Y0, int X1, int Y1) {
        for (int[] f : city.statues) {
            // A statue on a stepped stone plinth.
            float cx = (f[0] + 1) * T, cy = (f[1] + 1) * T;
            p.setColor(0x50000000);
            c.drawRect(cx - 12, cy - 10, cx + 16, cy + 16, p);
            p.setColor(0xFF9C968A);
            c.drawRect(cx - 14, cy - 14, cx + 14, cy + 14, p);
            p.setColor(0xFFB2AC9F);
            c.drawRect(cx - 10, cy - 10, cx + 10, cy + 10, p);
            p.setColor(0xFF5E6A5C);
            c.drawCircle(cx, cy, 5, p);
            p.setColor(0xFF7D8B7A);
            c.drawCircle(cx - 1.2f, cy - 1.2f, 2.6f, p);
        }

        for (int y = Y0; y < Y1; y++)
            for (int x = X0; x < X1; x++)
                if (city.tiles[y * city.w + x] == CAR) drawCar(c, p, x, y);

        for (float[] pu : city.pumps) {
            p.setColor(0x50000000);
            c.drawRect(pu[0] - 2.5f, pu[1] - 3, pu[0] + 4.5f, pu[1] + 5, p);
            p.setColor(0xFFD83A3A);
            c.drawRect(pu[0] - 3.5f, pu[1] - 4.5f, pu[0] + 3.5f, pu[1] + 4.5f, p);
            p.setColor(0xFFEEEEEE);
            c.drawRect(pu[0] - 2.5f, pu[1] - 3.5f, pu[0] + 2.5f, pu[1] - 0.5f, p);
            p.setColor(0xFF222222);
            c.drawRect(pu[0] + 3.5f, pu[1] - 1, pu[0] + 5, pu[1] + 3, p);
        }
        // Gas station canopies are drawn over the pumps, see-through so the pumps show.
        for (float[] d : city.decor) {
            if (d[0] != D_CANOPY) continue;
            p.setColor(0x50000000);
            c.drawRect(d[1] + 4, d[2] + 5, d[3] + 4, d[4] + 5, p);
            p.setColor(0x70F2F2F2);
            c.drawRect(d[1], d[2], d[3], d[4], p);
            p.setColor(0xFFD83A3A);
            c.drawRect(d[1], d[2], d[3], d[2] + 3, p);
            c.drawRect(d[1], d[4] - 3, d[3], d[4], p);
            c.drawRect(d[1], d[2], d[1] + 3, d[4], p);
            c.drawRect(d[3] - 3, d[2], d[3], d[4], p);
        }

        for (float[] l : city.lamps) {
            p.setColor(0x50000000);
            c.drawCircle(l[0] + 1.5f, l[1] + 2f, 2f, p);
            p.setColor(0xFF2E3034);
            c.drawCircle(l[0], l[1], 2f, p);
            p.setColor(0xFFE8E4D0);
            c.drawCircle(l[0], l[1], 1.1f, p);
        }

        for (int y = Y0; y < Y1; y++)
            for (int x = X0; x < X1; x++)
                if (city.tiles[y * city.w + x] == TREE) {
                    float cx = x * T + T / 2f, cy = y * T + T / 2f;
                    prnd.setSeed(tileSeed(x, y, 2));
                    if (city.elev != null && (x < city.townX0 || y < city.townY0 || x >= city.townX1 || y >= city.townY1)) {
                        // (Woods in the country grow where they like, not in rows.)
                        cx += (prnd.nextFloat() - 0.5f) * 7;
                        cy += (prnd.nextFloat() - 0.5f) * 7;
                    }
                    float r = 7.5f + prnd.nextFloat() * 2f;
                    byte kind = city.treeKindAt(y * city.w + x);
                    if (kind == Country.TK_PINE) r *= 0.82f;
                    // ((22.9) A saguaro and a bush of scrub are smaller than a tree.)
                    if (kind == Country.TK_CACTUS) r *= 0.6f;
                    else if (kind == Country.TK_SCRUB) r *= 0.55f;
                    p.setColor(0x50000000);
                    c.drawCircle(cx + TREE_HEIGHT * 0.28f, cy + TREE_HEIGHT * 0.38f, r, p);
                    p.setColor(0xFF4A3524);
                    c.drawCircle(cx, cy, 2f, p);
                    if (!city.regionOnly) {
                        city.trees.add(new float[]{cx, cy, r, kind});
                        if (city.canopies()) canopy(c, p, cx, cy, r, kind);
                    }
                }

        // Street trees along the pavement, their canopies over people's heads.
        if (city.cfg.parks() > 0)
            for (int y = Math.max(1, Y0); y < Math.min(city.h - 1, Y1); y++)
                for (int x = Math.max(1, X0); x < Math.min(city.w - 1, X1); x++) {
                    if (city.tiles[y * city.w + x] != SIDEWALK || ((x * 7 + y * 13) % (city.cfg.parks() >= 2 ? 5 : 8)) != 0) continue;
                    if (city.isBridge(x, y) || (city.doorway != null && city.doorway[y * city.w + x])) continue;
                    boolean nextToRoad = city.tiles[y * city.w + x - 1] == ROAD || city.tiles[y * city.w + x + 1] == ROAD
                            || city.tiles[(y - 1) * city.w + x] == ROAD || city.tiles[(y + 1) * city.w + x] == ROAD;
                    if (!nextToRoad || city.hasNeighborDir(x, y) || city.hasNeighbor(x, y, CAR)) continue;
                    boolean lamp = false;
                    for (float[] l : city.lamps) if (Math.abs(l[0] - (x * T + T / 2f)) < T * 2 && Math.abs(l[1] - (y * T + T / 2f)) < T * 2) lamp = true;
                    if (lamp) continue;
                    float cx = x * T + T / 2f, cy = y * T + T / 2f, r = 6f + ((x * 3 + y) % 3);
                    p.setColor(0x40000000);
                    c.drawCircle(cx + TREE_HEIGHT * 0.28f, cy + TREE_HEIGHT * 0.38f, r, p);
                    p.setColor(0xFF6B5A48);
                    c.drawRect(cx - 3.5f, cy - 3.5f, cx + 3.5f, cy + 3.5f, p);
                    p.setColor(0xFF4A3524);
                    c.drawCircle(cx, cy, 1.6f, p);
                    if (!city.regionOnly) {
                        // ((22.9) Palms along the desert's streets.)
                        // ((25.0) And on Theme Park Island.)
                        boolean palm = city.desert || (city.themePark && city.island(x, y) == 0) || (city.cfg.archipelago() && city.cfg.distinctIslands() && city.island(x, y) >= 1);
                        if (palm) city.trees.add(new float[]{cx, cy, r, Country.TK_PALM});
                        else city.trees.add(new float[]{cx, cy, r});
                        if (city.canopies()) canopy(c, p, cx, cy, r, palm ? Country.TK_PALM : Country.TK_BROAD);
                    }
                }
    }

    void canopy(Canvas c, Paint p, float x, float y, float r, int kind) {
        if (kind >= Country.TK_PALM) {
            desertTree(c, p, x, y, r, kind, 1, x * 0.13f + y * 0.07f);
            return;
        }
        int dark, light;
        switch (kind) {
            case Country.TK_PINE: dark = 0xFF1C3E26; light = 0xFF2C5C36; break;
            default: dark = 0xFF2C5A22; light = 0xFF3B742D; break;
        }
        p.setColor(dark);
        c.drawCircle(x, y, r, p);
        p.setColor(light);
        c.drawCircle(x - 1.5f, y - 1.5f, r * 0.62f, p);
    }

    // ------------------------------------------------------------------ realistic graphics

    /** The ground a lane or drive runs over: the grass, or the sand or plaza it's beside. */
    int laneGround(int x, int y) {
        int sand = 0, plaza = 0;
        for (int k = 0; k < 4; k++) {
            int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = y + (k == 2 ? 1 : k == 3 ? -1 : 0);
            if (nx < 0 || ny < 0 || nx >= city.w || ny >= city.h) continue;
            byte o = city.tiles[ny * city.w + nx];
            if (o == SAND) sand++;
            else if (o == PLAZA) plaza++;
        }
        if (sand > 0) return sandColor();
        if (plaza > 0) return city.country.plaza != 0 ? city.country.plaza : city.drawnRealistic ? 0xFFB2A58A : 0xFFB3A487;
        if (city.desert) return grassColour(x, y);
        return city.country.grass != 0 ? city.country.grass : city.drawnRealistic ? 0xFF4F6F36 : 0xFF4C7837;
    }

    /** 1 for a lane (dirt), 2 for a drive (a home's own way out), 0 for anything else. */
    int laneKind(int x, int y) {
        if (x < 0 || y < 0 || x >= city.w || y >= city.h) return 0;
        int i = y * city.w + x;
        if (city.tiles[i] == DIRT) return 1;
        if (city.tiles[i] == LOT && city.drive != null && city.drive[i]) return 2;
        return 0;
    }

    /** Paved ground a lane or drive runs out onto (it stops square at its edge, with a little gravel spilt on it). */
    boolean pavedEdge(int x, int y) {
        if (x < 0 || y < 0 || x >= city.w || y >= city.h) return false;
        byte t = city.tiles[y * city.w + x];
        return t == ROAD || t == CAR || t == SIDEWALK || t == PLAZA || t == BASE || (t == LOT && (city.drive == null || !city.drive[y * city.w + x]));
    }

    /**
     * (10.40) Lanes and drives drawn as smooth bands, not tile by tile: each tile a round patch joined to its
     * neighbours (straight on, round the bends and across the diagonal steps, so there's no staircase), with a
     * soft verge, wheel ruts along a lane, and a square end where it meets a road, the pavement or a car park.
     */
    void drawLanes(Canvas c, Paint p, int X0, int Y0, int X1, int Y1) {
        boolean real = city.drawnRealistic;
        int dirt = real ? 0xFF8A7252 : 0xFF8C7456, tar = real ? 0xFF46474A : 0xFF48494D;
        int x0 = Math.max(0, X0 - 1), y0 = Math.max(0, Y0 - 1), x1 = Math.min(city.w, X1 + 1), y1 = Math.min(city.h, Y1 + 1);
        p.setAntiAlias(true);
        for (int pass = 0; pass < 3; pass++) {
            for (int y = y0; y < y1; y++)
                for (int x = x0; x < x1; x++) {
                    int kind = laneKind(x, y);
                    if (kind == 0) continue;
                    int surface = kind == 1 ? dirt : tar;
                    float half = pass == 0 ? T * 0.72f : T * 0.6f;
                    int col = pass == 0 ? mix(surface, laneGround(x, y), 0.55f) : pass == 1 ? surface : kind == 1 ? 0xFF7A6446 : 0xFF3E3F42;
                    if (pass == 2 && kind == 2) continue;
                    float cx = x * T + T / 2f, cy = y * T + T / 2f;
                    p.setColor(col);
                    if (pass < 2) {
                        p.setStyle(Paint.Style.FILL);
                        c.drawCircle(cx, cy, half, p);
                    }
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(pass == 2 ? 1.6f : half * 2);
                    for (int k = 0; k < 4; k++) {
                        int dx = k == 0 ? 1 : k == 1 ? 0 : k == 2 ? 1 : -1, dy = k == 0 ? 0 : 1;
                        int ok = laneKind(x + dx, y + dy);
                        boolean diag = dx != 0 && dy != 0;
                        if (ok == kind) {
                            // (A diagonal only across a step, where the two tiles beside it aren't lane too.)
                            if (diag && (laneKind(x + dx, y) == kind || laneKind(x, y + dy) == kind)) continue;
                            if (pass == 2 && diag) continue;
                            // (Ruts run along the lane, not across it: only where it carries on beyond these two.)
                            if (pass == 2 && laneKind(x - dx, y - dy) != kind && laneKind(x + 2 * dx, y + 2 * dy) != kind) continue;
                            c.drawLine(cx, cy, cx + dx * T, cy + dy * T, p);
                        }
                    }
                    // Out to the edge of the road, pavement or car park it meets: square there.
                    if (pass == 1)
                        for (int k = 0; k < 4; k++) {
                            int dx = k == 0 ? 1 : k == 1 ? -1 : 0, dy = k == 2 ? 1 : k == 3 ? -1 : 0;
                            if (!pavedEdge(x + dx, y + dy) && laneKind(x + dx, y + dy) == 0) continue;
                            if (laneKind(x + dx, y + dy) != 0 && laneKind(x + dx, y + dy) != kind) {
                                // (A drive onto a lane, or a lane onto a drive: the two meet halfway.)
                            } else if (laneKind(x + dx, y + dy) == kind) continue;
                            p.setStyle(Paint.Style.FILL);
                            float ex = cx + dx * T / 2f, ey = cy + dy * T / 2f;
                            c.drawRect(Math.min(cx - (dy != 0 ? half : 0), ex), Math.min(cy - (dx != 0 ? half : 0), ey),
                                    Math.max(cx + (dy != 0 ? half : 0), ex), Math.max(cy + (dx != 0 ? half : 0), ey), p);
                            if (kind == 1 && pavedEdge(x + dx, y + dy)) {
                                // Gravel the wheels have carried out onto the tarmac.
                                p.setColor(0x996E5A40);
                                for (int g = 0; g < 4; g++) {
                                    float gx = ex + dx * (1 + g % 2 * 2) + (dy != 0 ? (g - 1.5f) * 3 : 0);
                                    float gy = ey + dy * (1 + g % 2 * 2) + (dx != 0 ? (g - 1.5f) * 3 : 0);
                                    c.drawCircle(gx, gy, 0.7f, p);
                                }
                                p.setColor(col);
                            }
                        }
                }
        }
        // Pebbles and tufts on the dirt.
        Random r = new Random(city.cfg.seed * 31 + x0 * 7919L + y0);
        p.setStyle(Paint.Style.FILL);
        for (int y = y0; y < y1; y++)
            for (int x = x0; x < x1; x++) {
                if (laneKind(x, y) != 1) continue;
                for (int k = 0; k < 3; k++) {
                    p.setColor(r.nextBoolean() ? 0xFFA08A68 : 0xFF6E5A40);
                    c.drawCircle(x * T + T * (0.2f + r.nextFloat() * 0.6f), y * T + T * (0.2f + r.nextFloat() * 0.6f), 0.7f, p);
                }
            }
        p.setStyle(Paint.Style.FILL);
        p.setStrokeWidth(1);
        p.setAntiAlias(false);
    }

    static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return 0xFF000000 | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    /**
     * Realistic ground: grainy asphalt with darker tyre tracks and the odd oil stain, paving slabs that
     * vary in tone, and grass with lighter and darker patches.
     */
    void drawRealGround(Canvas c, Paint p) {
        // (Each tile's own randomness, so a close-up draws just its own tiles and still matches the whole map.)
        Random r = prnd;
        for (int y = Math.max(0, city.gy0()); y < Math.min(city.h, gy1()); y++)
            for (int x = Math.max(0, gx0()); x < Math.min(city.w, gx1()); x++) {
                r.setSeed(tileSeed(x, y, 11));
                byte t = city.tiles[y * city.w + x];
                float fx = x * T, fy = y * T;
                if ((t == ROAD || t == CAR) && roadSurface(x, y) == SURF_ASPHALT && (city.curveTile == null || !city.curveTile[y * city.w + x])) {
                    for (int k = 0; k < 5; k++) {
                        p.setColor(r.nextBoolean() ? 0x14FFFFFF : 0x1A000000);
                        float gx = fx + r.nextFloat() * T, gy = fy + r.nextFloat() * T;
                        if (city.fine) c.drawRect(gx, gy, gx + 0.8f, gy + 0.8f, p);
                    }
                    // Patchy tarmac: lighter worn areas and darker fresh ones.
                    if (r.nextFloat() < 0.2f) {
                        p.setColor(0x08000000);
                        float bx = fx + r.nextFloat() * T, by = fy + r.nextFloat() * T, br = 5 + r.nextFloat() * 7;
                        c.drawCircle(bx, by, br, p);
                        c.drawCircle(bx, by, br * 0.6f, p);
                    }
                    if (r.nextFloat() < 0.025f) {
                        p.setColor(0x22101010);
                        c.drawCircle(fx + 4 + r.nextFloat() * 8, fy + 4 + r.nextFloat() * 8, 2 + r.nextFloat() * 2.5f, p);
                    }
                } else if (t == SIDEWALK) {
                    // Each half-slab a slightly different tone, with the odd crack.
                    for (int k = 0; k < 2; k++) {
                        int tone = r.nextInt(3);
                        if (tone == 0) continue;
                        p.setColor(tone == 1 ? 0x10FFFFFF : 0x12000000);
                        c.drawRect(fx + k * T / 2f + 0.5f, fy + 0.5f, fx + (k + 1) * T / 2f - 0.5f, fy + T - 0.5f, p);
                    }
                    if (r.nextFloat() < 0.06f) {
                        p.setColor(0x33000000);
                        p.setStrokeWidth(0.5f);
                        float cx = fx + r.nextFloat() * T, cy = fy + r.nextFloat() * T;
                        c.drawLine(cx, cy, cx + r.nextFloat() * 6 - 3, cy + r.nextFloat() * 6 - 3, p);
                    }
                } else if ((t == GRASS || t == TREE) && city.desert && !lushAt(x, y)) {
                    // ((22.9) Wind-blown sand: soft light and dark drifts.)
                    if (r.nextFloat() < 0.35f) {
                        p.setColor(r.nextBoolean() ? 0x14FFF4D8 : 0x10603A10);
                        c.drawCircle(fx + r.nextFloat() * T, fy + r.nextFloat() * T, 4 + r.nextFloat() * 8, p);
                    }
                } else if (t == GRASS || t == TREE) {
                    // Mown lawn stripes.
                    p.setColor((x / 2) % 2 == 0 ? 0x0CFFFFFF : 0x08000000);
                    c.drawRect(fx, fy, fx + T, fy + T, p);
                    if (r.nextFloat() < 0.3f) {
                        p.setColor(r.nextBoolean() ? 0x1A8AC060 : 0x18203A10);
                        c.drawCircle(fx + r.nextFloat() * T, fy + r.nextFloat() * T, 4 + r.nextFloat() * 7, p);
                    }
                    for (int k = 0; k < 4; k++) {
                        p.setColor(r.nextBoolean() ? 0x30A8D080 : 0x30284A18);
                        float gx = fx + r.nextFloat() * T, gy = fy + r.nextFloat() * T;
                        if (city.fine) c.drawRect(gx, gy, gx + 0.6f, gy + 1.6f, p);
                    }
                } else if (t == LOT || t == BASE) {
                    for (int k = 0; k < 3; k++) {
                        p.setColor(r.nextBoolean() ? 0x10FFFFFF : 0x14000000);
                        float gx = fx + r.nextFloat() * T, gy = fy + r.nextFloat() * T;
                        if (city.fine) c.drawRect(gx, gy, gx + 0.8f, gy + 0.8f, p);
                    }
                }
            }
    }

    /**
     * Water, sand and piers: deeper water further from the shore, ripples, surf on the beach, a stone edge
     * on the quays and embankments, plank decks, and bridges with their parapets and shadows on the water.
     */
    void drawWater(Canvas c, Paint p, int X0, int Y0, int X1, int Y1) {
        p.setStyle(Paint.Style.FILL);
        // (Square edges, so the tiles of open water don't show seams.)
        p.setAntiAlias(false);
        for (int y = Y0; y < Y1; y++)
            for (int x = X0; x < X1; x++) {
                byte t = city.tiles[y * city.w + x];
                float fx = x * T, fy = y * T;
                prnd.setSeed(tileSeed(x, y, 7));
                if (t == WATER) {
                    // Ripples.
                    for (int k = 0; k < 2; k++) {
                        if (prnd.nextFloat() < 0.5f) continue;
                        p.setColor(0x30C8E4F0);
                        float rx = fx + prnd.nextFloat() * (T - 6), ry = fy + 2 + prnd.nextFloat() * (T - 4);
                        c.drawRect(rx, ry, rx + 3 + prnd.nextFloat() * 4, ry + 0.7f, p);
                    }
                    // The water's edge: surf on sand, a lighter shallow band elsewhere.
                    for (int[] d : new int[][]{{0, -1}, {0, 1}, {-1, 0}, {1, 0}}) {
                        int nx = x + d[0], ny = y + d[1];
                        if (nx < 0 || ny < 0 || nx >= city.w || ny >= city.h) continue;
                        byte o = city.tiles[ny * city.w + nx];
                        if (o == WATER || (city.bridge != null && city.bridge[ny * city.w + nx])) continue;
                        boolean surf = o == SAND;
                        p.setColor(surf ? 0x90F2F6F8 : 0x4078B0C8);
                        float bw = surf ? 3 : 2.5f;
                        if (d[1] == -1) c.drawRect(fx, fy, fx + T, fy + bw, p);
                        else if (d[1] == 1) c.drawRect(fx, fy + T - bw, fx + T, fy + T, p);
                        else if (d[0] == -1) c.drawRect(fx, fy, fx + bw, fy + T, p);
                        else c.drawRect(fx + T - bw, fy, fx + T, fy + T, p);
                        if (surf) {
                            p.setColor(0x50F2F6F8);
                            if (d[1] == -1) c.drawRect(fx + prnd.nextFloat() * 6, fy + 5, fx + 6 + prnd.nextFloat() * 10, fy + 6, p);
                        }
                    }
                } else if (t == SAND) {
                    for (int k = 0; k < 6; k++) {
                        p.setColor(prnd.nextBoolean() ? 0xFFC8B686 : 0xFFE8DAB2);
                        c.drawCircle(fx + prnd.nextFloat() * T, fy + prnd.nextFloat() * T, 0.7f, p);
                    }
                    // Wet sand by the water.
                    if (y + 1 < city.h && city.tiles[(y + 1) * city.w + x] == WATER) {
                        p.setColor(0x40806A40);
                        c.drawRect(fx, fy + T * 0.55f, fx + T, fy + T, p);
                    }
                } else if (t == PIER) {
                    boolean along = (y > 0 && city.tiles[(y - 1) * city.w + x] == PIER) && (y < city.h - 1 && city.tiles[(y + 1) * city.w + x] == PIER);
                    p.setColor(0xFF6E5236);
                    for (int k = 1; k < 4; k++) {
                        if (along) c.drawRect(fx, fy + k * T / 4f, fx + T, fy + k * T / 4f + 0.7f, p);
                        else c.drawRect(fx + k * T / 4f, fy, fx + k * T / 4f + 0.7f, fy + T, p);
                    }
                    // Posts and a rail along the edge over the water.
                    for (int[] d : new int[][]{{-1, 0}, {1, 0}, {0, 1}, {0, -1}}) {
                        int nx = x + d[0], ny = y + d[1];
                        if (nx < 0 || ny < 0 || nx >= city.w || ny >= city.h || city.tiles[ny * city.w + nx] != WATER) continue;
                        p.setColor(0x40000000);
                        if (d[0] != 0) c.drawRect(d[0] < 0 ? fx - 3 : fx + T, fy, d[0] < 0 ? fx : fx + T + 3, fy + T, p);
                        else c.drawRect(fx, d[1] < 0 ? fy - 3 : fy + T, fx + T, d[1] < 0 ? fy : fy + T + 3, p);
                        p.setColor(0xFF4E3A26);
                        if (d[0] != 0) c.drawRect(d[0] < 0 ? fx : fx + T - 1.2f, fy, d[0] < 0 ? fx + 1.2f : fx + T, fy + T, p);
                        else c.drawRect(fx, d[1] < 0 ? fy : fy + T - 1.2f, fx + T, d[1] < 0 ? fy + 1.2f : fy + T, p);
                    }
                } else if (t != WATER && t != BUILDING) {
                    boolean br = city.bridge != null && city.bridge[y * city.w + x];
                    int bk = br && city.bridgeKind != null ? city.bridgeKind[y * city.w + x] : 0;
                    if (bk == BK_WOOD) {
                        // ((22.1) A wooden footbridge: planks across the deck.)
                        p.setColor(0xFF9A7448);
                        c.drawRect(fx, fy, fx + T, fy + T, p);
                        p.setColor(0xFF6E5030);
                        boolean ns = (x > 0 && city.bridge[y * city.w + x - 1]) || (x < city.w - 1 && city.bridge[y * city.w + x + 1]);
                        for (int k = 1; k < 6; k++) {
                            if (ns) c.drawRect(fx, fy + k * T / 6f, fx + T, fy + k * T / 6f + 0.6f, p);
                            else c.drawRect(fx + k * T / 6f, fy, fx + k * T / 6f + 0.6f, fy + T, p);
                        }
                    }
                    // Stone edges on quays and embankments; parapets on bridges.
                    for (int[] d : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
                        int nx = x + d[0], ny = y + d[1];
                        if (nx < 0 || ny < 0 || nx >= city.w || ny >= city.h || city.tiles[ny * city.w + nx] != WATER) continue;
                        if (t == SAND) continue;
                        // (Streams and lakes out in the country have grassy banks, not stone.)
                        if ((city.waterKind == CityConfig.W_NONE || city.waterKind == CityConfig.W_RIVER) && !br) continue;
                        // ((22.1) Each of River City's bridges its own: heavy stone, steel girders, plain concrete, timber.)
                        float th = bk == BK_STONE ? 3.4f : bk == BK_STEEL ? 3.6f : bk == BK_WOOD ? 1.4f : br ? 2.2f : 1.6f;
                        p.setColor(0x50000000);
                        float sh = bk == BK_STEEL ? 6 : 4;
                        if (d[0] != 0) c.drawRect(d[0] < 0 ? fx - sh : fx + T, fy, d[0] < 0 ? fx : fx + T + sh, fy + T, p);
                        else c.drawRect(fx, d[1] < 0 ? fy - sh : fy + T, fx + T, d[1] < 0 ? fy : fy + T + sh, p);
                        p.setColor(bk == BK_STONE ? 0xFFC9B48E : bk == BK_STEEL ? 0xFF5C7894 : bk == BK_WOOD ? 0xFF5E4428 : br ? 0xFFC8C4BA : 0xFFA8A49A);
                        if (d[0] != 0) c.drawRect(d[0] < 0 ? fx : fx + T - th, fy, d[0] < 0 ? fx + th : fx + T, fy + T, p);
                        else c.drawRect(fx, d[1] < 0 ? fy : fy + T - th, fx + T, d[1] < 0 ? fy + th : fy + T, p);
                        if (bk == BK_STEEL) {
                            // The girders' posts, and the cross-bracing between them, out over the water.
                            p.setColor(0xFF2E3A48);
                            for (int k = 0; k < 4; k++) {
                                float o = k * T / 4f;
                                if (d[0] != 0) c.drawRect(d[0] < 0 ? fx - 5 : fx + T, fy + o, d[0] < 0 ? fx : fx + T + 5, fy + o + 1.6f, p);
                                else c.drawRect(fx + o, d[1] < 0 ? fy - 5 : fy + T, fx + o + 1.6f, d[1] < 0 ? fy : fy + T + 5, p);
                            }
                            // A dark rail along the outer edge of the girder.
                            if (d[0] != 0) c.drawRect(d[0] < 0 ? fx - 5 : fx + T + 3.6f, fy, d[0] < 0 ? fx - 3.6f : fx + T + 5, fy + T, p);
                            else c.drawRect(fx, d[1] < 0 ? fy - 5 : fy + T + 3.6f, fx + T, d[1] < 0 ? fy - 3.6f : fy + T + 5, p);
                        } else if (bk == BK_STONE) {
                            // Stone blocks along the parapet.
                            p.setColor(0xFF9E8A68);
                            for (int k = 1; k < 4; k++) {
                                float o = k * T / 4f;
                                if (d[0] != 0) c.drawRect(d[0] < 0 ? fx : fx + T - th, fy + o, d[0] < 0 ? fx + th : fx + T, fy + o + 0.6f, p);
                                else c.drawRect(fx + o, d[1] < 0 ? fy : fy + T - th, fx + o + 0.6f, d[1] < 0 ? fy + th : fy + T, p);
                            }
                        }
                    }
                }
            }
        p.setAntiAlias(true);
    }

    /** A raised kerb where pavement meets road: a shadow on the road side and a lit edge on top. */
    void drawKerbs(Canvas c, Paint p) {
        for (int y = Math.max(1, city.gy0()); y < Math.min(city.h - 1, gy1()); y++)
            for (int x = Math.max(1, gx0()); x < Math.min(city.w - 1, gx1()); x++) {
                byte t = city.tiles[y * city.w + x];
                if (t != SIDEWALK && t != PLAZA) continue;
                float fx = x * T, fy = y * T;
                if (kerbRoad(y * city.w + x + 1)) {
                    p.setColor(0x40000000);
                    c.drawRect(fx + T, fy, fx + T + 1.2f, fy + T, p);
                    p.setColor(0x30FFFFFF);
                    c.drawRect(fx + T - 1, fy, fx + T, fy + T, p);
                }
                if (kerbRoad(y * city.w + x - 1)) {
                    p.setColor(0x28000000);
                    c.drawRect(fx - 1, fy, fx, fy + T, p);
                    p.setColor(0x30FFFFFF);
                    c.drawRect(fx, fy, fx + 1, fy + T, p);
                }
                if (kerbRoad((y + 1) * city.w + x)) {
                    p.setColor(0x40000000);
                    c.drawRect(fx, fy + T, fx + T, fy + T + 1.2f, p);
                    p.setColor(0x30FFFFFF);
                    c.drawRect(fx, fy + T - 1, fx + T, fy + T, p);
                }
                if (kerbRoad((y - 1) * city.w + x)) {
                    p.setColor(0x28000000);
                    c.drawRect(fx, fy - 1, fx + T, fy, p);
                    p.setColor(0x30FFFFFF);
                    c.drawRect(fx, fy, fx + T, fy + 1, p);
                }
            }
    }

    /**
     * Road a kerb is drawn against: not a curved street's tiles, which are drawn as pavement under the street's
     * own smooth kerb (10.44: straight kerb lines showed through across the pavement where it joins the grid).
     */
    boolean kerbRoad(int i) {
        return city.tiles[i] == ROAD && (city.curveTile == null || !city.curveTile[i]);
    }

    /** Weathering on roofs: grime speckles, streaks, a lit parapet on the sunny edges and a dark one opposite. */
    void weatherRoof(Canvas c, Paint p, int[] b) {
        float x0 = b[0] * T, y0 = b[1] * T, x1 = (b[0] + b[2]) * T, y1 = (b[1] + b[3]) * T;
        int kind = b[6];
        if (kind == SILO || kind == SPIRE) return;
        Random r = new Random(b[5] * 13L + 5);
        int n = (int) ((x1 - x0) * (y1 - y0) / 40);
        for (int k = 0; k < n; k++) {
            p.setColor(r.nextInt(3) == 0 ? 0x14FFFFFF : 0x16000000);
            float gx = x0 + 2 + r.nextFloat() * (x1 - x0 - 4), gy = y0 + 2 + r.nextFloat() * (y1 - y0 - 4);
            if (city.fine) c.drawRect(gx, gy, gx + 0.9f, gy + 0.9f, p);
        }
        // A few rain streaks and stains.
        for (int k = 0; k < 1 + n / 60; k++) {
            p.setColor(0x12000000);
            float gx = x0 + 3 + r.nextFloat() * (x1 - x0 - 6), gy = y0 + 3 + r.nextFloat() * (y1 - y0 - 6);
            c.drawCircle(gx, gy, 2 + r.nextFloat() * 4, p);
        }
        p.setColor(0x38FFFFFF);
        c.drawRect(x0, y0, x1, y0 + 0.9f, p);
        c.drawRect(x0, y0, x0 + 0.9f, y1, p);
        p.setColor(0x40000000);
        c.drawRect(x0, y1 - 0.9f, x1, y1, p);
        c.drawRect(x1 - 0.9f, y0, x1, y1, p);
    }

    /**
     * Curved streets (10.25): a kerb and a smooth band of tarmac along each centre line, and turning circles,
     * over the tiles they were laid on (which are drawn as pavement, so no steps show at the edges).
     */
    void drawCurves(Canvas c, Paint p) {
        if (city.curves.isEmpty()) return;
        if (!curvesTrimmed) trimCurves();
        boolean real = city.drawnRealistic;
        int road = real ? 0xFF38393C : 0xFF3A3D43, kerb = 0xFF7E7C77;
        Path path = new Path();
        p.setStyle(Paint.Style.STROKE);
        for (int pass = 0; pass < 2; pass++) {
            for (float[] l : city.curves) {
                if (l.length == 3) {
                    // A turning circle.
                    p.setStyle(Paint.Style.FILL);
                    p.setColor(pass == 0 ? kerb : road);
                    c.drawCircle(l[0] * T, l[1] * T, (-l[2] + (pass == 0 ? 0.12f : 0)) * T, p);
                    p.setStyle(Paint.Style.STROKE);
                    continue;
                }
                // (The kerb stops short of the street it joins, so it doesn't cross it.)
                int a = 0, b = l.length / 2 - 1;
                if (pass == 0) {
                    while (a < b && !onCurveTile(l[a * 2], l[a * 2 + 1])) a++;
                    while (b > a && !onCurveTile(l[b * 2], l[b * 2 + 1])) b--;
                }
                if (b - a < 1) continue;
                path.reset();
                path.moveTo(l[a * 2] * T, l[a * 2 + 1] * T);
                for (int k = a + 1; k <= b; k++) path.lineTo(l[k * 2] * T, l[k * 2 + 1] * T);
                p.setStrokeCap(Paint.Cap.BUTT);
                p.setStrokeWidth(pass == 0 ? 3.35f * T : 3.1f * T);
                p.setColor(pass == 0 ? kerb : road);
                c.drawPath(path, p);
                if (pass == 1) cornerPatches(c, p, l, road);
            }
        }
        // A faint worn line down the middle.
        p.setStrokeWidth(0.5f);
        p.setColor(0x40E8E2C8);
        for (float[] l : city.curves) {
            if (l.length == 3) continue;
            path.reset();
            path.moveTo(l[0] * T, l[1] * T);
            for (int k = 1; k < l.length / 2; k++) path.lineTo(l[k * 2] * T, l[k * 2 + 1] * T);
            c.drawPath(path, p);
        }
        p.setStyle(Paint.Style.FILL);
        p.setStrokeCap(Paint.Cap.BUTT);
    }

    boolean curvesTrimmed;

    /**
     * (10.47) Only what's still road is drawn as a curved street: the crescents, winding streets and greens are
     * laid out before the sea, the river or the harbour is cut in (and before some buildings), so a curve could
     * be left drawn across the water or over a building with no road under it. Each line is cut down to its
     * stretches over road; a turning circle goes if its middle isn't road.
     */
    void trimCurves() {
        curvesTrimmed = true;
        List<float[]> kept = new ArrayList<float[]>();
        for (float[] l : city.curves) {
            if (l.length == 3) {
                if (roadUnder(l[0], l[1])) kept.add(l);
                continue;
            }
            int n = l.length / 2, start = -1;
            for (int k = 0; k <= n; k++) {
                boolean ok = k < n && roadUnder(l[k * 2], l[k * 2 + 1]);
                if (ok && start < 0) start = k;
                if (!ok && start >= 0) {
                    if (start == 0 && k == n) kept.add(l);
                    else if (k - start >= 4) kept.add(java.util.Arrays.copyOfRange(l, start * 2, k * 2));
                    start = -1;
                }
            }
        }
        city.curves.clear();
        city.curves.addAll(kept);
    }

    boolean roadUnder(float x, float y) {
        int tx = City.fl(x), ty = City.fl(y);
        return tx >= 0 && ty >= 0 && tx < city.w && ty < city.h && city.tiles[ty * city.w + tx] == ROAD;
    }

    /**
     * (10.49) A curved road that comes in at the end of a straight one, where the straight road turns away (an L,
     * nothing beyond): the curve stops at the middle of the straight road, which left a notch of grass in the
     * corner. The tarmac is carried on to the straight road's far side, so the corner is filled.
     */
    void cornerPatches(Canvas c, Paint p, float[] l, int road) {
        int n = l.length / 2;
        if (n < 4) return;
        for (int end = 0; end < 2; end++) {
            int k = end == 0 ? 0 : n - 1, k2 = end == 0 ? 2 : n - 3;
            float x = l[k * 2], y = l[k * 2 + 1], dx = x - l[k2 * 2], dy = y - l[k2 * 2 + 1], d = (float) World.hypot(dx, dy);
            if (d < 0.01f) continue;
            dx /= d;
            dy /= d;
            int tx = City.fl(x), ty = City.fl(y);
            if (tx < 0 || ty < 0 || tx >= city.w || ty >= city.h || city.tiles[ty * city.w + tx] != ROAD) continue;
            int bx = City.fl(x + dx * 2.2f), by = City.fl(y + dy * 2.2f);
            if (bx >= 0 && by >= 0 && bx < city.w && by < city.h && city.tiles[by * city.w + bx] == ROAD) continue;
            // (A straight road turning away to one side there, not the curve just stopping in the grass.)
            boolean side = false;
            for (int sd = -1; sd <= 1; sd += 2) {
                int sx = City.fl(x - dy * sd * 2.4f), sy = City.fl(y + dx * sd * 2.4f);
                if (sx >= 0 && sy >= 0 && sx < city.w && sy < city.h && city.tiles[sy * city.w + sx] == ROAD && !onCurveTile(sx + 0.5f, sy + 0.5f)) side = true;
            }
            if (!side) continue;
            // A square of tarmac from the end of the curve to the straight road's far edge.
            float ex = x + dx * 1.6f, ey = y + dy * 1.6f, nx = -dy * 1.55f, ny = dx * 1.55f;
            Path q = new Path();
            q.moveTo((x + nx) * T, (y + ny) * T);
            q.lineTo((ex + nx) * T, (ey + ny) * T);
            q.lineTo((ex - nx) * T, (ey - ny) * T);
            q.lineTo((x - nx) * T, (y - ny) * T);
            q.close();
            p.setStyle(Paint.Style.FILL);
            p.setColor(road);
            c.drawPath(q, p);
            // (Back to drawing the curves' lines.)
            p.setStyle(Paint.Style.STROKE);
        }
    }

    boolean onCurveTile(float x, float y) {
        int tx = City.fl(x), ty = City.fl(y);
        return city.curveTile != null && tx >= 0 && ty >= 0 && tx < city.w && ty < city.h && city.curveTile[ty * city.w + tx];
    }

    /** Rounds the outer corners of every block's sidewalk where two roads meet. */
    /** Rounds off the block corners where two streets meet: wide kerbs on big blocks, tight on small ones. */
    void roundCorners(Canvas c, Paint p) {
        Path path = new Path();
        java.util.HashSet<Integer> done = new java.util.HashSet<Integer>();
        for (int[] b : city.blocks) {
            int bw = b[2] - b[0], bh = b[3] - b[1];
            float r = Math.min(bw, bh) >= 8 ? T * 2.2f : T * 1.1f;
            int[][] corners = {{b[0], b[1], 1, 1}, {b[2], b[1], -1, 1}, {b[0], b[3], 1, -1}, {b[2], b[3], -1, -1}};
            for (int[] k : corners) {
                // Only where roads meet on both sides of the corner.
                int ox = k[2] > 0 ? k[0] - 1 : k[0], oy = k[3] > 0 ? k[1] - 1 : k[1];
                int ix = k[2] > 0 ? k[0] : k[0] - 1, iy = k[3] > 0 ? k[1] : k[1] - 1;
                if (!city.paved(ox, iy) || !city.paved(ix, oy)) continue;
                if (ix < 0 || iy < 0 || ix >= city.w || iy >= city.h || city.tiles[iy * city.w + ix] != SIDEWALK) continue;
                done.add((iy * city.w + ix) * 4 + (k[2] > 0 ? 0 : 1) + (k[3] > 0 ? 0 : 2));
                float cx = k[0] * T, cy = k[1] * T;
                path.reset();
                path.moveTo(cx, cy);
                path.lineTo(cx + k[2] * r, cy);
                path.quadTo(cx, cy, cx, cy + k[3] * r);
                path.close();
                p.setStyle(Paint.Style.FILL);
                p.setColor(0xFF3A3D43);
                c.drawPath(path, p);
                // The kerb.
                path.reset();
                path.moveTo(cx + k[2] * r, cy);
                path.quadTo(cx, cy, cx, cy + k[3] * r);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1.2f);
                p.setColor(0xFF7E7C77);
                c.drawPath(path, p);
                p.setStyle(Paint.Style.FILL);
            }
        }
        // ((11.1) And every other corner of land that juts into a junction - a lot, a park, a quay, a compound, a
        // block changed after the streets were laid - not just the street blocks' own: rounded off the same way.)
        if (city.cfg.civic < 27) return;
        for (int iy = Math.max(1, city.gy0()); iy < Math.min(city.h - 1, gy1()); iy++)
            for (int ix = Math.max(1, gx0()); ix < Math.min(city.w - 1, gx1()); ix++) {
                byte t = city.tiles[iy * city.w + ix];
                if (t != SIDEWALK && t != GRASS && t != PLAZA && t != LOT && t != TREE && t != SAND) continue;
                for (int q = 0; q < 4; q++) {
                    int dx = (q & 1) == 0 ? -1 : 1, dy = (q & 2) == 0 ? -1 : 1;
                    if (!city.paved(ix + dx, iy) || !city.paved(ix, iy + dy) || !city.paved(ix + dx, iy + dy)) continue;
                    if (city.paved(ix - dx, iy) || city.paved(ix, iy - dy)) continue;
                    if (done.contains((iy * city.w + ix) * 4 + (dx < 0 ? 0 : 1) + (dy < 0 ? 0 : 2))) continue;
                    if (city.hwyTile(ix + dx, iy + dy) || (city.bridge != null && city.bridge[iy * city.w + ix]))
                        continue;
                    // The corner point, and the curve cut from it back into the land.
                    float cx = (dx < 0 ? ix : ix + 1) * T, cy = (dy < 0 ? iy : iy + 1) * T, r = T * 0.9f;
                    path.reset();
                    path.moveTo(cx, cy);
                    path.lineTo(cx - dx * r, cy);
                    path.quadTo(cx, cy, cx, cy - dy * r);
                    path.close();
                    p.setStyle(Paint.Style.FILL);
                    p.setColor(roadSurfaceColour(ix + dx, iy + dy));
                    c.drawPath(path, p);
                    path.reset();
                    path.moveTo(cx - dx * r, cy);
                    path.quadTo(cx, cy, cx, cy - dy * r);
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(1.2f);
                    p.setColor(0xFF7E7C77);
                    c.drawPath(path, p);
                    p.setStyle(Paint.Style.FILL);
                }
            }
    }

    /** (11.1) The colour of the road surface at a tile, as the ground is first painted. */
    int roadSurfaceColour(int x, int y) {
        int sf = roadSurface(x, y);
        if (sf == SURF_COBBLE) return 0xFF5F5A55;
        if (sf == SURF_CONCRETE) return 0xFF77777A;
        if (sf == SURF_CHIPSEAL) return 0xFF4A4946;
        return city.drawnRealistic ? 0xFF38393C : 0xFF3A3D43;
    }

    void drawBaseDetails(Canvas c, Paint p) {
        // Concrete slabs, then chain-link fences with posts.
        p.setStrokeWidth(1f);
        for (int y = Math.max(0, city.gy0()); y < Math.min(city.h, gy1()); y++)
            for (int x = Math.max(0, gx0()); x < Math.min(city.w, gx1()); x++) {
                byte t = city.tiles[y * city.w + x];
                if (t != BASE && t != FENCE && !(t == CAR && city.carKind.get(y * city.w + x) == 2)) continue;
                p.setColor(0xFF62665A);
                c.drawLine(x * T, y * T, x * T + T, y * T, p);
                c.drawLine(x * T, y * T, x * T, y * T + T, p);
            }
        for (int y = Math.max(0, city.gy0()); y < Math.min(city.h, gy1()); y++)
            for (int x = Math.max(0, gx0()); x < Math.min(city.w, gx1()); x++) {
                if (city.tiles[y * city.w + x] != FENCE) continue;
                float cx = x * T + T / 2f, cy = y * T + T / 2f;
                boolean hor = (x > 0 && city.tiles[y * city.w + x - 1] == FENCE) || (x < city.w - 1 && city.tiles[y * city.w + x + 1] == FENCE);
                boolean ver = (y > 0 && city.tiles[(y - 1) * city.w + x] == FENCE) || (y < city.h - 1 && city.tiles[(y + 1) * city.w + x] == FENCE);
                p.setColor(0x60000000);
                p.setStrokeWidth(2.4f);
                if (hor) c.drawLine(x * T, cy + 1.5f, x * T + T, cy + 1.5f, p);
                if (ver) c.drawLine(cx + 1.5f, y * T, cx + 1.5f, y * T + T, p);
                p.setColor(0xFFB9BEC2);
                p.setStrokeWidth(1.4f);
                if (hor) c.drawLine(x * T, cy, x * T + T, cy, p);
                if (ver) c.drawLine(cx, y * T, cx, y * T + T, p);
                p.setColor(0xFF4A4E52);
                c.drawCircle(cx, cy, 1.6f, p);
            }
        drawBaseDecor(c, p);
        if (city.prison != null) drawPrisonWall(c, p);
        // ((14.9) A big white star in a ring painted on each base's parade ground, its name across the ground by it,
        // and the flag: plain to see as the army's from any height.)
        for (Facility f : city.facilities) {
            if (f.kind != FACILITY_BASE) continue;
            float sr = Math.min(30, f.r * 0.24f);
            // (On open concrete clear of the buildings.)
            float sx = f.x, sy = f.y;
            // (The side away from town: the mortar pits are on the town side, and the garrison mills round the middle.)
            float away = (float) Math.atan2(f.y - city.worldH() / 2f, f.x - city.worldW() / 2f);
            search:
            for (float dist = f.r; dist < f.r * 2.4f; dist += 8)
                for (int k = 0; k < 16; k++) {
                    float a = away + ((k + 1) / 2) * (k % 2 == 0 ? 1 : -1) * (float) Math.PI / 8;
                    float qx = f.x + (float) Math.cos(a) * dist, qy = f.y + (float) Math.sin(a) * dist;
                    boolean open = true;
                    for (float ox = -sr - 8; ox <= sr + 8 && open; ox += T / 2f)
                        for (float oy = -sr - 14; oy <= sr + 18 && open; oy += T / 2f) {
                            int tx = fl((qx + ox) / T), ty = fl((qy + oy) / T);
                            if (tx < 0 || ty < 0 || tx >= city.w || ty >= city.h || city.tiles[ty * city.w + tx] != BASE) open = false;
                        }
                    // (Clear of the base's own things - domes, tanks, the runway - and its helicopter pads.)
                    for (float[] d : city.baseDecor)
                        if (qx + sr + 6 > d[1] && qx - sr - 6 < d[3] && qy + sr + 18 > d[2] && qy - sr - 6 < d[4]) open = false;
                    for (float[] hp : city.helipads) if (World.hypot(hp[0] - qx, hp[1] - qy) < sr + 30) open = false;
                    if (open) {
                        sx = qx;
                        sy = qy;
                        break search;
                    }
                }
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2.2f);
            p.setColor(0xD8E8E6DA);
            c.drawCircle(sx, sy, sr, p);
            p.setStyle(Paint.Style.FILL);
            android.graphics.Path star = new android.graphics.Path();
            for (int k = 0; k < 10; k++) {
                float a = (float) (-Math.PI / 2 + k * Math.PI / 5), rr = k % 2 == 0 ? sr * 0.88f : sr * 0.36f;
                if (k == 0) star.moveTo(sx + (float) Math.cos(a) * rr, sy + (float) Math.sin(a) * rr);
                else star.lineTo(sx + (float) Math.cos(a) * rr, sy + (float) Math.sin(a) * rr);
            }
            star.close();
            p.setColor(0xD8E8E6DA);
            c.drawPath(star, p);
            city.sign(city.country.sign("ARMY"), sx, sy + sr + 9, 8, 0xFFE8E6DA);
            // The flagpole by the star, and the flag.
            float fx = sx + sr + 8, fy = sy - sr * 0.4f;
            p.setColor(0x55000000);
            c.drawCircle(fx + 0.8f, fy + 0.8f, 1.8f, p);
            p.setColor(0xFFD8D8D8);
            c.drawCircle(fx, fy, 1.5f, p);
            p.setColor(0xFF3E5A2A);
            c.drawRect(fx + 1, fy - 1.5f, fx + 9, fy + 3.5f, p);
            p.setColor(0xFFE8C547);
            c.drawCircle(fx + 5, fy + 1, 1.2f, p);
        }
        for (float[] hp : city.helipads) {
            p.setColor(0xFF4A4E46);
            c.drawCircle(hp[0], hp[1], 22, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1.8f);
            // (Yellow for the army, blue for the police, red for the rescue service.)
            int kind = padKind(hp);
            p.setColor(kind == PAD_POLICE ? 0xFF4A8AE8 : kind == PAD_RESCUE ? 0xFFE0453A : 0xFFE8D24A);
            c.drawCircle(hp[0], hp[1], 18, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFEEEEEE);
            c.drawRect(hp[0] - 7, hp[1] - 9, hp[0] - 4, hp[1] + 9, p);
            c.drawRect(hp[0] + 4, hp[1] - 9, hp[0] + 7, hp[1] + 9, p);
            c.drawRect(hp[0] - 5, hp[1] - 1.5f, hp[0] + 5, hp[1] + 1.5f, p);
        }
        for (float[] t : city.tents) {
            p.setColor(0x50000000);
            c.drawRect(t[0] - 9, t[1] - 6, t[0] + 11, t[1] + 8, p);
            p.setColor(0xFF5E6B3E);
            c.drawRect(t[0] - 10, t[1] - 7, t[0] + 10, t[1] + 7, p);
            p.setColor(0xFF6F7D4A);
            c.drawRect(t[0] - 10, t[1] - 7, t[0] + 10, t[1] - 0.5f, p);
            p.setColor(0xFF465030);
            c.drawRect(t[0] - 10, t[1] - 0.6f, t[0] + 10, t[1] + 0.6f, p);
        }
    }

    /** Courts, sports fields, playgrounds, gardens, graves and skateparks. */
    /**
     * The biggest rectangle of the given shape (long side / short side) that fits in the box, no longer than
     * maxLen, centred, along the box's long side. Returns {x0, y0, x1, y1, 1 if it runs across}.
     */
    static float[] fit(float x0, float y0, float x1, float y1, float ratio, float maxLen) {
        float bw = x1 - x0, bh = y1 - y0;
        boolean wide = bw >= bh;
        float lng = wide ? bw : bh, shrt = wide ? bh : bw;
        float L = Math.min(maxLen, Math.min(lng, shrt * ratio)), S = L / ratio;
        float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
        return wide ? new float[]{cx - L / 2, cy - S / 2, cx + L / 2, cy + S / 2, 1}
                : new float[]{cx - S / 2, cy - L / 2, cx + S / 2, cy + L / 2, 0};
    }

    /** A point on a court: a along its length (0..1), b across it (0..1). */
    static float cxAt(float[] ct, float a, float b) {
        return ct[4] > 0 ? ct[0] + (ct[2] - ct[0]) * a : ct[0] + (ct[2] - ct[0]) * b;
    }

    static float cyAt(float[] ct, float a, float b) {
        return ct[4] > 0 ? ct[1] + (ct[3] - ct[1]) * b : ct[1] + (ct[3] - ct[1]) * a;
    }

    static void line(Canvas c, Paint p, float[] ct, float a0, float b0, float a1, float b1) {
        c.drawLine(cxAt(ct, a0, b0), cyAt(ct, a0, b0), cxAt(ct, a1, b1), cyAt(ct, a1, b1), p);
    }

    static void box(Canvas c, Paint p, float[] ct, float a0, float b0, float a1, float b1) {
        float xa = cxAt(ct, a0, b0), ya = cyAt(ct, a0, b0), xb = cxAt(ct, a1, b1), yb = cyAt(ct, a1, b1);
        c.drawRect(Math.min(xa, xb), Math.min(ya, yb), Math.max(xa, xb), Math.max(ya, yb), p);
    }

    static float lenOf(float[] ct) {
        return ct[4] > 0 ? ct[2] - ct[0] : ct[3] - ct[1];
    }

    /** A basketball court: keys, free-throw circles, three-point arcs (inside the court), centre circle. */
    static void basketball(Canvas c, Paint p, float[] ct) {
        float L = lenOf(ct);
        p.setColor(0xFFB0663A);
        c.drawRect(ct[0], ct[1], ct[2], ct[3], p);
        p.setColor(0xFF8A4A2A);
        // The painted keys.
        box(c, p, ct, 0, 0.5f - 4.9f / 30, 5.8f / 28, 0.5f + 4.9f / 30);
        box(c, p, ct, 1 - 5.8f / 28, 0.5f - 4.9f / 30, 1, 0.5f + 4.9f / 30);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.1f);
        p.setColor(0xEEFFFFFF);
        c.drawRect(ct[0], ct[1], ct[2], ct[3], p);
        line(c, p, ct, 0.5f, 0, 0.5f, 1);
        c.drawCircle(cxAt(ct, 0.5f, 0.5f), cyAt(ct, 0.5f, 0.5f), L * 1.8f / 28, p);
        box(c, p, ct, 0, 0.5f - 4.9f / 30, 5.8f / 28, 0.5f + 4.9f / 30);
        box(c, p, ct, 1 - 5.8f / 28, 0.5f - 4.9f / 30, 1, 0.5f + 4.9f / 30);
        c.drawCircle(cxAt(ct, 5.8f / 28, 0.5f), cyAt(ct, 5.8f / 28, 0.5f), L * 1.8f / 28, p);
        c.drawCircle(cxAt(ct, 1 - 5.8f / 28, 0.5f), cyAt(ct, 1 - 5.8f / 28, 0.5f), L * 1.8f / 28, p);
        // Three-point lines: arcs round each basket, kept inside the court.
        c.save();
        c.clipRect(ct[0], ct[1], ct[2], ct[3]);
        c.drawCircle(cxAt(ct, 1.575f / 28, 0.5f), cyAt(ct, 1.575f / 28, 0.5f), L * 6.75f / 28, p);
        c.drawCircle(cxAt(ct, 1 - 1.575f / 28, 0.5f), cyAt(ct, 1 - 1.575f / 28, 0.5f), L * 6.75f / 28, p);
        c.restore();
        p.setStyle(Paint.Style.FILL);
        // Hoops.
        p.setColor(0xFFE0702E);
        c.drawCircle(cxAt(ct, 1.575f / 28, 0.5f), cyAt(ct, 1.575f / 28, 0.5f), 1.4f, p);
        c.drawCircle(cxAt(ct, 1 - 1.575f / 28, 0.5f), cyAt(ct, 1 - 1.575f / 28, 0.5f), 1.4f, p);
    }

    /** A tennis court: doubles and singles sidelines, service boxes, the net. */
    static void tennis(Canvas c, Paint p, float[] ct) {
        p.setColor(0xFF4E946A);
        c.drawRect(ct[0], ct[1], ct[2], ct[3], p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1f);
        p.setColor(0xEEFFFFFF);
        c.drawRect(ct[0], ct[1], ct[2], ct[3], p);
        float alley = (1 - 8.23f / 10.97f) / 2, svc = 6.4f / 23.77f;
        line(c, p, ct, 0, alley, 1, alley);
        line(c, p, ct, 0, 1 - alley, 1, 1 - alley);
        line(c, p, ct, 0.5f - svc, alley, 0.5f - svc, 1 - alley);
        line(c, p, ct, 0.5f + svc, alley, 0.5f + svc, 1 - alley);
        line(c, p, ct, 0.5f - svc, 0.5f, 0.5f + svc, 0.5f);
        p.setStrokeWidth(1.8f);
        p.setColor(0xFF2A2A2A);
        line(c, p, ct, 0.5f, -0.04f, 0.5f, 1.04f);
        p.setStyle(Paint.Style.FILL);
    }

    /** A football pitch: halfway line, centre circle, penalty and goal areas, penalty arcs, goals. */
    static void football(Canvas c, Paint p, float[] ct) {
        float L = lenOf(ct);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.3f);
        p.setColor(0xEEFFFFFF);
        c.drawRect(ct[0], ct[1], ct[2], ct[3], p);
        line(c, p, ct, 0.5f, 0, 0.5f, 1);
        c.drawCircle(cxAt(ct, 0.5f, 0.5f), cyAt(ct, 0.5f, 0.5f), L * 9.15f / 105, p);
        float pb = 16.5f / 105, pw = 40.3f / 68 / 2, gb = 5.5f / 105, gw = 18.3f / 68 / 2;
        box(c, p, ct, 0, 0.5f - pw, pb, 0.5f + pw);
        box(c, p, ct, 1 - pb, 0.5f - pw, 1, 0.5f + pw);
        box(c, p, ct, 0, 0.5f - gw, gb, 0.5f + gw);
        box(c, p, ct, 1 - gb, 0.5f - gw, 1, 0.5f + gw);
        // The "D" on each penalty area: the part of the circle round the spot outside the box.
        for (int end = 0; end < 2; end++) {
            float spot = end == 0 ? 11f / 105 : 1 - 11f / 105;
            c.save();
            float ea = end == 0 ? pb : 1 - pb;
            float xa = cxAt(ct, ea, 0), ya = cyAt(ct, ea, 0), xb = cxAt(ct, end == 0 ? 0.5f : 0.5f, 1), yb = cyAt(ct, 0.5f, 1);
            c.clipRect(Math.min(xa, xb), Math.min(ya, yb), Math.max(xa, xb), Math.max(ya, yb));
            c.drawCircle(cxAt(ct, spot, 0.5f), cyAt(ct, spot, 0.5f), L * 9.15f / 105, p);
            c.restore();
        }
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xEEFFFFFF);
        c.drawCircle(cxAt(ct, 11f / 105, 0.5f), cyAt(ct, 11f / 105, 0.5f), 1.1f, p);
        c.drawCircle(cxAt(ct, 1 - 11f / 105, 0.5f), cyAt(ct, 1 - 11f / 105, 0.5f), 1.1f, p);
        c.drawCircle(cxAt(ct, 0.5f, 0.5f), cyAt(ct, 0.5f, 0.5f), 1.1f, p);
        // Goals.
        p.setColor(0xFFF2F2F2);
        box(c, p, ct, -0.012f, 0.5f - 3.66f / 68, 0, 0.5f + 3.66f / 68);
        box(c, p, ct, 1, 0.5f - 3.66f / 68, 1.012f, 0.5f + 3.66f / 68);
    }

    /**
     * (10.26) Fields, crops, playgrounds, gardens, courts and the like are painted on the ground only where it's
     * still open ground: a lane, a stream, a driveway, a helipad or a tree that ended up on one shows as itself
     * instead of the field being drawn over it (or it over the field).
     */
    void drawGroundDecor(Canvas c, Paint p, float[] d) {
        int kind = (int) d[0];
        boolean ground = kind == D_CROPS || kind == D_FIELD || kind == D_PLAYGROUND || kind == D_GARDEN || kind == D_COURT
                || kind == D_SKATE || kind == D_FLOWERS || kind == D_GRAVE;
        if (!ground) {
            drawDecor(c, p, d);
            return;
        }
        int tx0 = Math.max(0, (int) (d[1] / T)), ty0 = Math.max(0, (int) (d[2] / T));
        int tx1 = Math.min(city.w - 1, (int) ((d[3] - 0.01f) / T)), ty1 = Math.min(city.h - 1, (int) ((d[4] - 0.01f) / T));
        boolean clear = true;
        for (int y = ty0; y <= ty1 && clear; y++)
            for (int x = tx0; x <= tx1 && clear; x++) if (!openGround(city.tiles[y * city.w + x])) clear = false;
        if (clear) {
            drawDecor(c, p, d);
            return;
        }
        // Row by row, in runs of open ground.
        for (int y = ty0; y <= ty1; y++)
            for (int x = tx0; x <= tx1; ) {
                if (!openGround(city.tiles[y * city.w + x])) {
                    x++;
                    continue;
                }
                int x0 = x;
                while (x <= tx1 && openGround(city.tiles[y * city.w + x])) x++;
                c.save();
                c.clipRect(Math.max(d[1], x0 * T), Math.max(d[2], y * T), Math.min(d[3], x * T), Math.min(d[4], (y + 1) * T));
                drawDecor(c, p, d);
                c.restore();
            }
    }

    static boolean openGround(byte t) {
        return t == GRASS || t == PLAZA || t == LOT || t == SAND;
    }

    void drawDecor(Canvas c, Paint p, float[] d) {
        int kind = (int) d[0], variant = (int) d[5];
        float x0 = d[1], y0 = d[2], x1 = d[3], y1 = d[4], cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
        float dw = x1 - x0, dh = y1 - y0;
        boolean wide = dw >= dh;
        p.setStyle(Paint.Style.FILL);
        if (kind == D_RANGE) {
            drawTestRange(c, p, x0, y0, x1, y1, (int) d[6]);
            return;
        }
        if (kind == D_STRINGS) {
            // ((22.8) A wire across the street, sagging a little, hung with lanterns, bulbs or flags.)
            float ax = d[1], ay = d[2], bx = d[3], by = d[4], len = (float) Math.hypot(bx - ax, by - ay);
            float nx = -(by - ay) / len, ny = (bx - ax) / len, sag = len * 0.06f;
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1f);
            p.setColor(0xC0202020);
            Path wire = new Path();
            wire.moveTo(ax, ay);
            wire.quadTo((ax + bx) / 2 + nx * sag, (ay + by) / 2 + ny * sag, bx, by);
            c.drawPath(wire, p);
            p.setStyle(Paint.Style.FILL);
            int n = Math.max(3, (int) (len / (variant == 1 ? 11 : 9)));
            for (int k = 1; k < n; k++) {
                float t = k / (float) n, s2 = 4 * t * (1 - t);
                float px = ax + (bx - ax) * t + nx * sag * s2, py = ay + (by - ay) * t + ny * sag * s2;
                if (variant == 1) {
                    p.setColor(0x50000000);
                    c.drawCircle(px + 1.4f, py + 2f, 3.6f, p);
                    p.setColor(0xFFC8281E);
                    c.drawCircle(px, py, 3.5f, p);
                    p.setColor(0xFFE8B030);
                    c.drawRect(px - 1.2f, py - 4.2f, px + 1.2f, py - 3f, p);
                    c.drawRect(px - 1.2f, py + 3f, px + 1.2f, py + 4.2f, p);
                    p.setColor(0xFFF06A50);
                    c.drawCircle(px - 1.1f, py - 1.1f, 1.2f, p);
                } else if (variant == 2) {
                    p.setColor(0x60F8E8A0);
                    c.drawCircle(px, py, 3f, p);
                    p.setColor(0xFFFFF0B8);
                    c.drawCircle(px, py, 1.5f, p);
                } else {
                    int[] fl = {0xFFE04A3A, 0xFFE8C040, 0xFF3AA0D0, 0xFF60B050, 0xFFB060C0, 0xFFF08A30};
                    p.setColor(fl[(k + (int) d[6]) % fl.length]);
                    Path f = new Path();
                    float dx = (bx - ax) / len, dy = (by - ay) / len;
                    f.moveTo(px - dx * 2.8f, py - dy * 2.8f);
                    f.lineTo(px + dx * 2.8f, py + dy * 2.8f);
                    f.lineTo(px + nx * 5f, py + ny * 5f);
                    f.close();
                    c.drawPath(f, p);
                }
            }
            return;
        }
        if (kind == D_LANDMARK) {
            Landmarks.Def ld = Landmarks.get(variant);
            if (ld != null) {
                int sc = c.save();
                c.clipRect(x0, y0, x1, y1);
                ld.ground(city, c, p, x0, y0, x1, y1, new Random(d.length > 6 ? (long) d[6] : 1));
                c.restoreToCount(sc);
                p.setStyle(Paint.Style.FILL);
            }
            return;
        }
        switch (kind) {
            case D_COURT:
                decorCourt(c, p, variant, x0, y0, x1, y1);
                break;
            case D_FIELD:
                decorField(c, p, x0, y0, x1, y1, dw, dh, wide);
                break;
            case D_PLAYGROUND:
                decorPlayground(c, p, x0, y0, x1, y1, dw, dh);
                break;
            case D_GARDEN:
                decorGarden(c, p, variant, x0, y0, x1, y1, dw, dh, wide);
                break;
            case D_GRAVE:
                decorGrave(c, p, variant, x0, y0, x1, y1, cx);
                break;
            case D_SKATE:
                decorSkate(c, p, x0, y0, x1, y1, dw, dh);
                break;
            case D_ALLEY:
                decorAlley(c, p, x0, y0, x1, y1, dw, dh, wide);
                break;
            case D_SITE:
                decorSite(c, p, variant, x0, y0, x1, y1, dw, dh);
                break;
            case D_POWER:
                decorPower(c, p, x0, y0, x1, y1);
                break;
            case D_HELIPAD:
                decorHelipad(c, p, cx, cy, dw, dh);
                break;
            case D_BAY:
                decorBay(c, p, x0, y0, x1, y1, dw, dh, wide);
                break;
            case D_BANDSTAND:
                decorBandstand(c, p, cx, cy, dw, dh);
                break;
            case D_FLOWERS:
                decorFlowers(c, p, variant, x0, y0, x1, y1, dw, dh);
                break;
            case D_CROPS:
                decorCrops(c, p, variant, x0, y0, x1, y1, wide);
                break;
            case D_ROUNDABOUT:
                decorRoundabout(c, p, variant, cx, cy);
                break;
            case D_FOUNTAIN:
                decorFountain(c, p, cx, cy, dw, dh);
                break;
            case D_FLAGS:
                decorFlags(c, p, variant, x0, y0, cx, cy, dw, dh);
                break;
            case D_YARD:
                decorYard(c, p, x0, y0, x1, y1, cx, cy, dw, dh, wide);
                break;
            case D_MAST:
                decorMast(c, p, x0, y0, x1, y1, cx, cy);
                break;
            case D_SALT:
                decorSalt(c, p, cx, cy, dw, dh);
                break;
            case D_PILES:
                decorPiles(c, p, x0, y0, cx, cy, dw, dh, wide);
                break;
            case D_UMBRELLA:
                decorUmbrella(c, p, variant, cx, cy, dw);
                break;
            case D_LIFEGUARD:
                decorLifeguard(c, p, x0, y0, x1, y1, dh);
                break;
            case D_BOAT:
                decorBoat(c, p, variant, x0, cy, dw);
                break;
            case D_SHIP:
                decorShip(c, p, variant, x0, y0, x1, y1, cx, dw);
                break;
            case D_CRANE:
                decorCrane(c, p, x0, y0, x1, y1, cx, cy);
                break;
            case D_CONTAINERS:
                decorContainers(c, p, variant, x0, y0, x1, y1, dw, dh);
                break;
            case D_FLOODLIGHT:
                decorFloodlight(c, p, x0, y0, x1, y1);
                break;
            case D_TENT:
                decorTent(c, p, variant, cx, cy);
                break;
            case D_CAMPFIRE:
                decorCampfire(c, p, cx, cy);
                break;
            case D_PICNIC:
                decorPicnic(c, p, variant, x0, y0, x1, y1, cx, cy);
                break;
            case D_FALLS:
                decorFalls(c, p, variant, x0, y0, x1, y1, cx, cy);
                break;
            case D_SIGN:
                decorSign(c, p, cx, cy);
                break;
            case D_FIRETOWER:
                decorFiretower(c, p, cx, cy);
                break;
            case D_LOOKOUT:
                decorLookout(c, p, x0, y0, x1, y1, cx, cy);
                break;
            default:
                break;
        }
    }

    /** ((27.3) drawDecor's D_COURT, as it was drawn there.) */
    private void decorCourt(Canvas c, Paint p, int variant, float x0, float y0, float x1, float y1) {
        // The surface round the court, then a court of the right shape in the middle of it.
        p.setColor(variant == 0 ? 0xFF6E6E70 : 0xFF3F6A50);
        c.drawRect(x0, y0, x1, y1, p);
        float[] ct = fit(x0 + 3, y0 + 3, x1 - 3, y1 - 3, variant == 0 ? 28f / 15f : 23.77f / 10.97f, variant == 0 ? 160 : 140);
        if (variant == 0) basketball(c, p, ct);
        else tennis(c, p, ct);
    }

    /** ((27.3) drawDecor's D_FIELD, as it was drawn there.) */
    private void decorField(Canvas c, Paint p, float x0, float y0, float x1, float y1, float dw, float dh, boolean wide) {
        int stripes = 8;
        for (int i = 0; i < stripes; i++) {
            p.setColor(i % 2 == 0 ? 0xFF4F8A3A : 0xFF5A9644);
            if (wide) c.drawRect(x0 + dw * i / stripes, y0, x0 + dw * (i + 1) / stripes, y1, p);
            else c.drawRect(x0, y0 + dh * i / stripes, x1, y0 + dh * (i + 1) / stripes, p);
        }
        float[] ct = fit(x0 + 4, y0 + 4, x1 - 4, y1 - 4, 105f / 68f, 560);
        football(c, p, ct);
    }

    /** ((27.3) drawDecor's D_PLAYGROUND, as it was drawn there.) */
    private void decorPlayground(Canvas c, Paint p, float x0, float y0, float x1, float y1, float dw, float dh) {
        p.setColor(0xFFD9C38E);
        c.drawRoundRect(new RectF(x0, y0, x1, y1), 6, 6, p);
        p.setColor(0x40000000);
        c.drawRect(x0 + dw * 0.15f + 1.5f, y0 + dh * 0.2f + 1.5f, x0 + dw * 0.35f + 1.5f, y0 + dh * 0.7f + 1.5f, p);
        p.setColor(0xFFE0483A);
        c.drawRect(x0 + dw * 0.15f, y0 + dh * 0.2f, x0 + dw * 0.35f, y0 + dh * 0.7f, p);
        p.setColor(0xFFE8C53A);
        c.drawRect(x0 + dw * 0.18f, y0 + dh * 0.55f, x0 + dw * 0.32f, y0 + dh * 0.85f, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.5f);
        p.setColor(0xFF3A6FB0);
        c.drawLine(x0 + dw * 0.5f, y0 + dh * 0.25f, x0 + dw * 0.85f, y0 + dh * 0.25f, p);
        p.setColor(0xFF555555);
        c.drawLine(x0 + dw * 0.6f, y0 + dh * 0.25f, x0 + dw * 0.6f, y0 + dh * 0.45f, p);
        c.drawLine(x0 + dw * 0.75f, y0 + dh * 0.25f, x0 + dw * 0.75f, y0 + dh * 0.45f, p);
        p.setStyle(Paint.Style.FILL);
        int[] cols = {0xFF4FB06A, 0xFFE0483A, 0xFF3A6FB0, 0xFFE8C53A};
        for (int i = 0; i < 4; i++) {
            p.setColor(cols[i]);
            c.drawCircle(x0 + dw * (0.55f + 0.1f * i), y0 + dh * 0.72f, 2.2f, p);
        }
    }

    /** ((27.3) drawDecor's D_GARDEN, as it was drawn there.) */
    private void decorGarden(Canvas c, Paint p, int variant, float x0, float y0, float x1, float y1, float dw, float dh, boolean wide) {
        int rows = (int) ((wide ? dh : dw) / 6);
        for (int i = 0; i < rows; i++) {
            float a = (wide ? y0 : x0) + i * 6;
            p.setColor(0xFF6B4A30);
            if (wide) c.drawRect(x0, a, x1, a + 4, p);
            else c.drawRect(a, y0, a + 4, y1, p);
            int crop = (i + variant) % 3;
            p.setColor(crop == 0 ? 0xFF6FB04A : crop == 1 ? 0xFF3E8A34 : 0xFFB0A03A);
            float len = wide ? dw : dh;
            for (float t = 2; t < len - 2; t += 4) {
                if (wide) c.drawCircle(x0 + t, a + 2, 1.5f, p);
                else c.drawCircle(a + 2, y0 + t, 1.5f, p);
            }
        }
    }

    /** ((27.3) drawDecor's D_GRAVE, as it was drawn there.) */
    private void decorGrave(Canvas c, Paint p, int variant, float x0, float y0, float x1, float y1, float cx) {
        p.setColor(0x50000000);
        c.drawRect(x0 + 1, y0 + 1.5f, x1 + 1, y1 + 1.5f, p);
        p.setColor(variant == 0 ? 0xFFA8A49C : variant == 1 ? 0xFF8E8A84 : 0xFFBDB8AE);
        c.drawRect(x0, y0, x1, y1, p);
        if (variant == 2) {
            p.setColor(0xFF7A766E);
            c.drawRect(cx - 0.5f, y0 + 0.5f, cx + 0.5f, y1 - 1, p);
            c.drawRect(x0 + 0.5f, y0 + 2, x1 - 0.5f, y0 + 3, p);
        }
    }

    /** ((27.3) drawDecor's D_SKATE, as it was drawn there.) */
    private void decorSkate(Canvas c, Paint p, float x0, float y0, float x1, float y1, float dw, float dh) {
        p.setColor(0xFFBDB6A4);
        c.drawRect(x0, y0, x1, y1, p);
        RectF bowl = new RectF(x0 + dw * 0.1f, y0 + dh * 0.15f, x0 + dw * 0.5f, y0 + dh * 0.75f);
        p.setColor(0xFF8E887A);
        c.drawRoundRect(bowl, 12, 12, p);
        bowl.inset(4, 4);
        p.setColor(0xFF77715F);
        c.drawRoundRect(bowl, 10, 10, p);
        p.setColor(0xFF9A9384);
        c.drawRect(x0 + dw * 0.6f, y0 + dh * 0.2f, x0 + dw * 0.9f, y0 + dh * 0.4f, p);
        p.setColor(0xFFA9A293);
        c.drawRect(x0 + dw * 0.6f, y0 + dh * 0.2f, x0 + dw * 0.9f, y0 + dh * 0.26f, p);
        p.setStrokeWidth(1.4f);
        p.setColor(0xFF555555);
        c.drawLine(x0 + dw * 0.6f, y0 + dh * 0.65f, x0 + dw * 0.9f, y0 + dh * 0.65f, p);
    }

    /** ((27.3) drawDecor's D_ALLEY, as it was drawn there.) */
    private void decorAlley(Canvas c, Paint p, float x0, float y0, float x1, float y1, float dw, float dh, boolean wide) {
        // A narrow back alley: worn tarmac, bins and a dumpster or two.
        Random r = new Random((long) (x0 * 31 + y0 * 17));
        p.setColor(0xFF5A5854);
        c.drawRect(x0, y0, x1, y1, p);
        float len = wide ? dw : dh;
        for (float t = 6 + r.nextFloat() * 20; t < len - 8; t += 22 + r.nextFloat() * 30) {
            float px = wide ? x0 + t : x0 + 1, py = wide ? y0 + 1 : y0 + t;
            float sx = wide ? 9 : dw - 2, sy = wide ? dh - 2 : 9;
            if (r.nextBoolean()) {
                p.setColor(0x50000000);
                c.drawRect(px + 1, py + 1.5f, px + sx + 1, py + sy + 1.5f, p);
                p.setColor(r.nextBoolean() ? 0xFF2F6B45 : 0xFF3A5A8A);
                c.drawRect(px, py, px + sx, py + sy, p);
                p.setColor(0x33FFFFFF);
                c.drawRect(px, py, px + sx, py + 1.2f, p);
            } else {
                p.setColor(0xFF22262A);
                c.drawCircle(px + (wide ? 3 : sx / 2), py + (wide ? sy / 2 : 3), 2.3f, p);
                p.setColor(0xFF1A1A1A);
                c.drawCircle(px + (wide ? 7 : sx / 2 + 2), py + (wide ? sy / 2 + 1 : 7), 1.8f, p);
            }
        }
    }

    /** ((27.3) drawDecor's D_SITE, as it was drawn there.) */
    private void decorSite(Canvas c, Paint p, int variant, float x0, float y0, float x1, float y1, float dw, float dh) {
        // Dug-out foundations, rebar, stacked materials and a tower crane.
        Random r = new Random((long) (x0 * 13 + y0 * 7));
        p.setColor(0xFF8A6E4E);
        c.drawRect(x0, y0, x1, y1, p);
        for (int k = 0; k < 40; k++) {
            p.setColor(r.nextBoolean() ? 0xFF7A5E40 : 0xFF9A7E5A);
            c.drawCircle(x0 + r.nextFloat() * dw, y0 + r.nextFloat() * dh, 1 + r.nextFloat() * 2, p);
        }
        float fx0 = x0 + dw * 0.1f, fy0 = y0 + dh * 0.12f, fx1 = x0 + dw * 0.6f, fy1 = y0 + dh * 0.55f;
        p.setColor(0xFF9E9A92);
        c.drawRect(fx0, fy0, fx1, fy1, p);
        p.setColor(0xFF6E6A62);
        c.drawRect(fx0 + 2, fy0 + 2, fx1 - 2, fy1 - 2, p);
        p.setColor(0xFFB06A3A);
        p.setStrokeWidth(0.5f);
        for (float gx = fx0 + 4; gx < fx1 - 2; gx += 5) c.drawLine(gx, fy0 + 2, gx, fy1 - 2, p);
        for (float gy = fy0 + 4; gy < fy1 - 2; gy += 5) c.drawLine(fx0 + 2, gy, fx1 - 2, gy, p);
        // Stacks of planks, pipes and bricks.
        for (int k = 0; k < 3; k++) {
            float mx = x0 + dw * (0.12f + k * 0.18f), my = y0 + dh * 0.75f;
            p.setColor(k == 0 ? 0xFFC8A060 : k == 1 ? 0xFF8A8E94 : 0xFFA8543A);
            c.drawRect(mx, my, mx + 10, my + 6, p);
            p.setColor(0x33000000);
            for (float l = my + 1.5f; l < my + 6; l += 1.5f) c.drawLine(mx, l, mx + 10, l, p);
        }
        // Portable cabin.
        p.setColor(0xFFE8C547);
        c.drawRect(x1 - 18, y0 + 3, x1 - 3, y0 + 10, p);
        p.setColor(0xFF4A6E8A);
        c.drawRect(x1 - 15, y0 + 5, x1 - 11, y0 + 8, p);
        // The crane: a mast with a long jib, and its shadow on the ground.
        // The jib swings over the site (never out over the street or the next block).
        float mx = fx1 - 4, my = fy1 - 4, ang = (float) Math.PI + 0.25f + (variant % 4) * 0.35f;
        float jx = (float) Math.cos(ang), jy = (float) Math.sin(ang);
        float jl = 0.92f * Math.min((mx - x0) / Math.max(0.05f, -jx), (my - y0) / Math.max(0.05f, -jy));
        float cj = Math.min(jl * 0.25f, Math.min((x1 - mx) / Math.max(0.05f, -jx), (y1 - my) / Math.max(0.05f, -jy)));
        p.setStrokeWidth(3f);
        p.setColor(0x40000000);
        c.drawLine(mx + 10 - jx * cj, my + 14 - jy * cj, mx + 10 + jx * jl, my + 14 + jy * jl, p);
        p.setColor(0xFFE8B830);
        c.drawRect(mx - 3, my - 3, mx + 3, my + 3, p);
        p.setStrokeWidth(2.2f);
        c.drawLine(mx - jx * cj, my - jy * cj, mx + jx * jl, my + jy * jl, p);
        p.setColor(0xFF6A6A6A);
        c.drawRect(mx - jx * cj - 3, my - jy * cj - 3, mx - jx * cj + 3, my - jy * cj + 3, p);
        p.setStrokeWidth(1f);
    }

    /** ((27.3) drawDecor's D_POWER, as it was drawn there.) */
    private void decorPower(Canvas c, Paint p, float x0, float y0, float x1, float y1) {
        // Rows of transformers and a pylon.
        p.setColor(0xFF9A968C);
        c.drawRect(x0, y0, x1, y1, p);
        p.setColor(0xFF8A867C);
        for (float gy = y0 + 2; gy < y1; gy += 4) c.drawRect(x0, gy, x1, gy + 0.6f, p);
        for (float ty = y0 + 5; ty < y1 - 8; ty += 14)
            for (float tx = x0 + 5; tx < x1 - 8; tx += 14) {
                p.setColor(0x50000000);
                c.drawRect(tx + 1.5f, ty + 2, tx + 9.5f, ty + 10, p);
                p.setColor(0xFF5E6A5A);
                c.drawRect(tx, ty, tx + 8, ty + 8, p);
                p.setColor(0xFF7E8A78);
                for (float fx = tx + 1; fx < tx + 8; fx += 2) c.drawRect(fx, ty + 1, fx + 0.8f, ty + 7, p);
                p.setColor(0xFFD8D8D0);
                c.drawCircle(tx + 2, ty + 1, 0.9f, p);
                c.drawCircle(tx + 6, ty + 1, 0.9f, p);
            }
        p.setColor(0xFF3A3A3A);
        p.setStrokeWidth(0.5f);
        c.drawLine(x0, y0 + 3, x1, y0 + 3, p);
        c.drawLine(x0, y1 - 3, x1, y1 - 3, p);
        p.setStrokeWidth(1f);
    }

    /** ((27.3) drawDecor's D_HELIPAD, as it was drawn there.) */
    private void decorHelipad(Canvas c, Paint p, float cx, float cy, float dw, float dh) {
        p.setColor(0xFF3A3C40);
        c.drawCircle(cx, cy, Math.min(dw, dh) / 2, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.5f);
        p.setColor(0xFFE8E8E8);
        c.drawCircle(cx, cy, Math.min(dw, dh) / 2 - 2.5f, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFD83A3A);
        float hs = Math.min(dw, dh) * 0.22f;
        c.drawRect(cx - hs, cy - hs, cx - hs * 0.55f, cy + hs, p);
        c.drawRect(cx + hs * 0.55f, cy - hs, cx + hs, cy + hs, p);
        c.drawRect(cx - hs, cy - hs * 0.2f, cx + hs, cy + hs * 0.2f, p);
    }

    /** ((27.3) drawDecor's D_BAY, as it was drawn there.) */
    private void decorBay(Canvas c, Paint p, float x0, float y0, float x1, float y1, float dw, float dh, boolean wide) {
        // Yellow hatched ambulance bay.
        p.setColor(0xFFE8C547);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1f);
        c.drawRect(x0 + 0.5f, y0 + 0.5f, x1 - 0.5f, y1 - 0.5f, p);
        p.setStyle(Paint.Style.FILL);
        float len = wide ? dw : dh;
        for (float t = 3; t < len - 2; t += 5) {
            if (wide) c.drawLine(x0 + t, y0 + 1, x0 + t + 3, y1 - 1, p);
            else c.drawLine(x0 + 1, y0 + t, x1 - 1, y0 + t + 3, p);
        }
    }

    /** ((27.3) drawDecor's D_BANDSTAND, as it was drawn there.) */
    private void decorBandstand(Canvas c, Paint p, float cx, float cy, float dw, float dh) {
        // An eight-sided bandstand with a green roof.
        float rr = Math.min(dw, dh) * 0.42f;
        Path oct = new Path();
        for (int k = 0; k < 8; k++) {
            double a = Math.PI / 8 + k * Math.PI / 4;
            float px = cx + (float) Math.cos(a) * rr, py = cy + (float) Math.sin(a) * rr;
            if (k == 0) oct.moveTo(px, py);
            else oct.lineTo(px, py);
        }
        oct.close();
        c.save();
        c.translate(3, 4);
        p.setColor(0x50000000);
        c.drawPath(oct, p);
        c.restore();
        p.setColor(0xFFE8E4DA);
        c.drawPath(oct, p);
        c.save();
        c.translate(cx, cy);
        c.scale(0.82f, 0.82f);
        c.translate(-cx, -cy);
        p.setColor(0xFF3F7A5A);
        c.drawPath(oct, p);
        c.restore();
        p.setColor(0xFF5A9A74);
        c.drawCircle(cx, cy, rr * 0.3f, p);
        p.setColor(0xFFE8C547);
        c.drawCircle(cx, cy, 1.5f, p);
    }

    /** ((27.3) drawDecor's D_FLOWERS, as it was drawn there.) */
    private void decorFlowers(Canvas c, Paint p, int variant, float x0, float y0, float x1, float y1, float dw, float dh) {
        Random r = new Random((long) (x0 * 7 + y0 * 3));
        p.setColor(0xFF6B4A30);
        c.drawRoundRect(new RectF(x0, y0, x1, y1), 4, 4, p);
        int[][] sets = {{0xFFE84A5A, 0xFFF2E86B}, {0xFFB08AE8, 0xFFF2F2F2}, {0xFFE87BB0, 0xFFE8A040}};
        for (int k = 0; k < (int) (dw * dh / 5); k++) {
            p.setColor(r.nextInt(3) == 0 ? 0xFF3E8A34 : sets[variant % 3][r.nextInt(2)]);
            c.drawCircle(x0 + 2 + r.nextFloat() * (dw - 4), y0 + 2 + r.nextFloat() * (dh - 4), 1.1f, p);
        }
    }

    /** ((27.3) drawDecor's D_CROPS, as it was drawn there.) */
    private void decorCrops(Canvas c, Paint p, int variant, float x0, float y0, float x1, float y1, boolean wide) {
        // Ploughed rows: wheat, cabbages or bare earth.
        int base = variant == 0 ? 0xFFC8A850 : variant == 1 ? 0xFF4E8A38 : 0xFF7A5A3A;
        int row = variant == 0 ? 0xFFB09040 : variant == 1 ? 0xFF3A6E2A : 0xFF6A4A2E;
        p.setColor(base);
        c.drawRect(x0, y0, x1, y1, p);
        p.setColor(row);
        boolean rowsAcross = variant == 1 ? !wide : wide;
        if (rowsAcross) for (float gy = y0 + 1; gy < y1; gy += 3.5f) c.drawRect(x0, gy, x1, gy + 1.2f, p);
        else for (float gx = x0 + 1; gx < x1; gx += 3.5f) c.drawRect(gx, y0, gx + 1.2f, y1, p);
        if (variant == 1) {
            p.setColor(0xFF6FB04A);
            for (float gy = y0 + 2; gy < y1 - 1; gy += 3.5f)
                for (float gx = x0 + 2; gx < x1 - 1; gx += 3.5f) c.drawCircle(gx, gy, 1.1f, p);
        }
    }

    /** ((27.3) drawDecor's D_ROUNDABOUT, as it was drawn there.) */
    private void decorRoundabout(Canvas c, Paint p, int variant, float cx, float cy) {
        // Paint the corners of the island back to road, then the round island with a kerb and flowers.
        float ir = variant * T / 2f + (variant == 1 ? 2 : 4);
        float ix0 = cx - variant * T / 2f, iy0 = cy - variant * T / 2f;
        p.setColor(0xFF3A3D43);
        c.drawRect(ix0, iy0, ix0 + variant * T, iy0 + variant * T, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1f);
        p.setColor(0xB0E8E8E8);
        c.drawCircle(cx, cy, ir + 5, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFB8B4AC);
        c.drawCircle(cx, cy, ir, p);
        p.setColor(0xFF4C7837);
        c.drawCircle(cx, cy, ir - 1.5f, p);
        p.setColor(0xFF578A3F);
        c.drawCircle(cx, cy, ir * 0.6f, p);
        int[] fl = {0xFFE84A5A, 0xFFF2E86B, 0xFFF2F2F2};
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4;
            p.setColor(fl[k % 3]);
            c.drawCircle(cx + (float) Math.cos(a) * ir * 0.72f, cy + (float) Math.sin(a) * ir * 0.72f, 1.2f, p);
        }
    }

    /** ((27.3) drawDecor's D_FOUNTAIN, as it was drawn there.) */
    private void decorFountain(Canvas c, Paint p, float cx, float cy, float dw, float dh) {
        // A round basin with a jet in the middle.
        float rr = Math.min(dw, dh) / 2;
        p.setColor(0x50000000);
        c.drawCircle(cx + 2, cy + 3, rr, p);
        p.setColor(0xFFD8D2C4);
        c.drawCircle(cx, cy, rr, p);
        p.setColor(0xFF4A86A8);
        c.drawCircle(cx, cy, rr - 2.2f, p);
        p.setColor(0xFF6FA8C8);
        c.drawCircle(cx - rr * 0.2f, cy - rr * 0.2f, rr * 0.45f, p);
        p.setColor(0xFFD8D2C4);
        c.drawCircle(cx, cy, Math.max(2, rr * 0.22f), p);
        p.setColor(0xFFF2F8FF);
        c.drawCircle(cx, cy, Math.max(1, rr * 0.1f), p);
    }

    /** ((27.3) drawDecor's D_FLAGS, as it was drawn there.) */
    private void decorFlags(Canvas c, Paint p, int variant, float x0, float y0, float cx, float cy, float dw, float dh) {
        // Three flagpoles: the country's, the region's and the city's.
        int[] cols = {city.country.shieldColor, 0xFFD8B040, 0xFFE8E8E8};
        for (int k = 0; k < 3; k++) {
            float fx = variant == 0 ? x0 + dw * (k + 0.5f) / 3 : cx, fy = variant == 0 ? cy : y0 + dh * (k + 0.5f) / 3;
            p.setColor(0x50000000);
            c.drawCircle(fx + 1, fy + 1, 1.6f, p);
            p.setColor(0xFFB8BCC0);
            c.drawCircle(fx, fy, 1.3f, p);
            p.setColor(cols[k]);
            c.drawRect(fx + 0.5f, fy - 5.5f, fx + 7, fy - 1.5f, p);
            p.setColor(0x30000000);
            c.drawRect(fx + 4, fy - 5.5f, fx + 7, fy - 1.5f, p);
        }
    }

    /** ((27.3) drawDecor's D_YARD, as it was drawn there.) */
    private void decorYard(Canvas c, Paint p, float x0, float y0, float x1, float y1, float cx, float cy, float dw, float dh, boolean wide) {
        // Exercise yard: bare concrete, a painted court and benches.
        p.setColor(0xFF8A8A86);
        c.drawRect(x0, y0, x1, y1, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(0.8f);
        p.setColor(0xB0E8E4D0);
        c.drawRect(x0 + 3, y0 + 3, x1 - 3, y1 - 3, p);
        if (wide) c.drawLine(cx, y0 + 3, cx, y1 - 3, p);
        else c.drawLine(x0 + 3, cy, x1 - 3, cy, p);
        c.drawCircle(cx, cy, Math.min(dw, dh) * 0.15f, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF5A5E62);
        if (wide) c.drawRect(x0 + 4, y1 - 2.5f, x0 + 14, y1 - 1, p);
        else c.drawRect(x1 - 2.5f, y0 + 4, x1 - 1, y0 + 14, p);
    }

    /** ((27.3) drawDecor's D_MAST, as it was drawn there.) */
    private void decorMast(Canvas c, Paint p, float x0, float y0, float x1, float y1, float cx, float cy) {
        // A lattice radio mast on a concrete pad, with its guy wires.
        p.setColor(0xFF7A7C80);
        c.drawRect(x0 + 2, y0 + 2, x1 - 2, y1 - 2, p);
        p.setStrokeWidth(0.6f);
        p.setColor(0xFFB8BCC0);
        c.drawLine(cx, cy, x0 + 3, y0 + 3, p);
        c.drawLine(cx, cy, x1 - 3, y0 + 3, p);
        c.drawLine(cx, cy, cx, y1 - 3, p);
        p.setColor(0x60000000);
        c.drawRect(cx - 2, cy - 1, cx + 14, cy + 2, p);
        p.setColor(0xFFC8302A);
        c.drawRect(cx - 2.5f, cy - 2.5f, cx + 2.5f, cy + 2.5f, p);
        p.setColor(0xFFF2F2F2);
        c.drawRect(cx - 1.4f, cy - 1.4f, cx + 1.4f, cy + 1.4f, p);
        p.setColor(0xFFC8302A);
        c.drawCircle(cx, cy, 0.8f, p);
    }

    /** ((27.3) drawDecor's D_SALT, as it was drawn there.) */
    private void decorSalt(Canvas c, Paint p, float cx, float cy, float dw, float dh) {
        // Salt dome: a round shed with a dark opening.
        float rr = Math.min(dw, dh) / 2 - 1;
        p.setColor(0x50000000);
        c.drawCircle(cx + 2, cy + 3, rr, p);
        p.setColor(0xFF9A9488);
        c.drawCircle(cx, cy, rr, p);
        p.setColor(0xFFB8B2A4);
        c.drawCircle(cx - rr * 0.2f, cy - rr * 0.2f, rr * 0.6f, p);
        p.setColor(0xFF4A4642);
        c.drawRect(cx - 3, cy + rr - 4, cx + 3, cy + rr, p);
    }

    /** ((27.3) drawDecor's D_PILES, as it was drawn there.) */
    private void decorPiles(Canvas c, Paint p, float x0, float y0, float cx, float cy, float dw, float dh, boolean wide) {
        // Heaps of gravel and sand.
        int[] cols = {0xFF8C8478, 0xFFC8B080, 0xFF6E6A64};
        for (int k = 0; k < 3; k++) {
            float px = wide ? x0 + dw * (k + 0.5f) / 3 : cx, py = wide ? cy : y0 + dh * (k + 0.5f) / 3;
            float pr = Math.min(wide ? dw / 3 : dw, wide ? dh : dh / 3) * 0.45f;
            p.setColor(darken(cols[k], 0.75f));
            c.drawCircle(px + 1, py + 1, pr, p);
            p.setColor(cols[k]);
            c.drawCircle(px, py, pr, p);
            p.setColor(lighten(cols[k], 0.15f));
            c.drawCircle(px - pr * 0.25f, py - pr * 0.25f, pr * 0.45f, p);
        }
    }

    /** ((27.3) drawDecor's D_UMBRELLA, as it was drawn there.) */
    private void decorUmbrella(Canvas c, Paint p, int variant, float cx, float cy, float dw) {
        // A beach umbrella over a towel.
        int[][] cols = {{0xFFE84A4A, 0xFFF2F2F2}, {0xFF3A8AD8, 0xFFF2E86B}, {0xFF3FA860, 0xFFF2F2F2}, {0xFFF2A030, 0xFF3A6AB0},
                {0xFFE07AA0, 0xFFF2F2F2}};
        int[] cc = cols[variant % cols.length];
        p.setColor(cc[1]);
        c.drawRect(cx + 1, cy + 1, cx + 5, cy + 9, p);
        p.setColor(0x40000000);
        c.drawCircle(cx + 2, cy + 3, dw * 0.42f, p);
        float rr = dw * 0.42f;
        for (int k = 0; k < 8; k++) {
            p.setColor(cc[k % 2]);
            c.drawArc(new RectF(cx - rr, cy - rr, cx + rr, cy + rr), k * 45, 45, true, p);
        }
        p.setColor(0xFF6A5A48);
        c.drawCircle(cx, cy, 0.9f, p);
    }

    /** ((27.3) drawDecor's D_LIFEGUARD, as it was drawn there.) */
    private void decorLifeguard(Canvas c, Paint p, float x0, float y0, float x1, float y1, float dh) {
        // A lifeguard hut on stilts, with its flag.
        p.setColor(0x50000000);
        c.drawRect(x0 + 3, y0 + 3, x1 + 3, y1 + 3, p);
        p.setColor(0xFFF2F2F2);
        c.drawRect(x0, y0, x1, y1, p);
        p.setColor(0xFFD83A3A);
        c.drawRect(x0, y0, x1, y0 + dh * 0.35f, p);
        p.setColor(0xFFF2D23A);
        c.drawRect(x1 - 2, y0 - 7, x1 + 6, y0 - 3, p);
        p.setColor(0xFFD83A3A);
        c.drawRect(x1 + 2, y0 - 7, x1 + 6, y0 - 3, p);
    }

    /** ((27.3) drawDecor's D_BOAT, as it was drawn there.) */
    private void decorBoat(Canvas c, Paint p, int variant, float x0, float cy, float dw) {
        // 0 a rowing boat, 1 a motor boat, 2 a yacht with its sail.
        float bl = variant == 0 ? Math.min(dw, 18) : dw, bh2 = variant == 0 ? 6 : 9;
        float bx0 = x0, by0 = cy - bh2 / 2, bx1 = x0 + bl, by1 = cy + bh2 / 2;
        p.setColor(0x30000000);
        c.drawOval(new RectF(bx0 + 2, by0 + 3, bx1 + 2, by1 + 3), p);
        p.setColor(0x40F2F6F8);
        c.drawRect(bx0 - 6, cy - 0.6f, bx0, cy + 0.6f, p);
        Path hull = new Path();
        hull.moveTo(bx0, by0);
        hull.lineTo(bx1 - bh2 * 0.6f, by0);
        hull.lineTo(bx1, cy);
        hull.lineTo(bx1 - bh2 * 0.6f, by1);
        hull.lineTo(bx0, by1);
        hull.close();
        p.setColor(variant == 0 ? 0xFF9A6A3E : variant == 1 ? 0xFFF2F2F2 : 0xFFE8E4DA);
        c.drawPath(hull, p);
        if (variant == 0) {
            p.setColor(0xFF6E4A2A);
            c.drawRect(bx0 + bl * 0.35f, by0, bx0 + bl * 0.42f, by1, p);
        } else if (variant == 1) {
            p.setColor(0xFF2A4A6A);
            c.drawRect(bx0 + bl * 0.35f, by0 + 2, bx0 + bl * 0.6f, by1 - 2, p);
            p.setColor(0xFFD83A3A);
            c.drawRect(bx0, by0, bx1 - bh2 * 0.6f, by0 + 1, p);
        } else {
            p.setColor(0xFFF8F8F4);
            Path sail = new Path();
            sail.moveTo(bx0 + bl * 0.45f, cy);
            sail.lineTo(bx0 + bl * 0.45f, cy - 14);
            sail.lineTo(bx0 + bl * 0.15f, cy);
            sail.close();
            c.drawPath(sail, p);
            p.setColor(0xFF6A6A6A);
            c.drawRect(bx0 + bl * 0.44f, cy - 14, bx0 + bl * 0.47f, cy, p);
        }
    }

    /** ((27.3) drawDecor's D_SHIP, as it was drawn there.) */
    private void decorShip(Canvas c, Paint p, int variant, float x0, float y0, float x1, float y1, float cx, float dw) {
        // A cargo ship moored alongside: hull, stacked containers, the bridge at the stern.
        int[] hulls = {0xFF8A2A24, 0xFF24405E, 0xFF2E4A36, 0xFF3A3A3E};
        RectF hr = new RectF(x0, y0, x1, y1);
        p.setColor(0x40000000);
        c.drawRoundRect(new RectF(x0 + 3, y0 + 4, x1 + 3, y1 + 4), 6, 6, p);
        Path hull = new Path();
        hull.moveTo(x0, y0 + dw * 0.8f);
        hull.lineTo(cx, y0);
        hull.lineTo(x1, y0 + dw * 0.8f);
        hull.lineTo(x1, y1 - 2);
        hull.lineTo(x0, y1 - 2);
        hull.close();
        p.setColor(hulls[variant % hulls.length]);
        c.drawPath(hull, p);
        p.setColor(0xFF5A5E62);
        c.drawRect(x0 + 2, y0 + dw * 0.8f, x1 - 2, y1 - 4, p);
        int[] box = {0xFFB03A2E, 0xFF2E5FB0, 0xFF3C7A4E, 0xFFD4A21C, 0xFF6A6E74, 0xFFE07A2A};
        Random r = new Random(variant * 31L + (long) x0);
        float top = y0 + dw * 0.9f, bottom = y1 - dw * 0.45f - 4;
        for (float yy = top; yy < bottom - 4; yy += 6)
            for (float xx = x0 + 3; xx < x1 - 4; xx += (dw - 6) / 3) {
                p.setColor(box[r.nextInt(box.length)]);
                c.drawRect(xx, yy, xx + (dw - 6) / 3 - 1, yy + 5, p);
            }
        p.setColor(0xFFF2F2F2);
        c.drawRect(x0 + 3, y1 - dw * 0.45f - 2, x1 - 3, y1 - 6, p);
        p.setColor(0xFF2A3A4A);
        c.drawRect(x0 + 4, y1 - dw * 0.45f, x1 - 4, y1 - dw * 0.45f + 2, p);
        p.setColor(0xFF3A3A3A);
        c.drawCircle(cx, y1 - 9, 2, p);
    }

    /** ((27.3) drawDecor's D_CRANE, as it was drawn there.) */
    private void decorCrane(Canvas c, Paint p, float x0, float y0, float x1, float y1, float cx, float cy) {
        // A dockside gantry crane: legs on rails, and its boom reaching out over the water.
        p.setColor(0x40000000);
        c.drawRect(x0 + 6, y0 + 6, x1 + 6, y1 + 6, p);
        p.setColor(0xFF5A5E62);
        c.drawRect(x0, y0 - 1, x1, y0 + 1, p);
        c.drawRect(x0, y1 - 1, x1, y1 + 1, p);
        p.setColor(0xFFE0A030);
        c.drawRect(x0, y0, x0 + 3, y1, p);
        c.drawRect(x1 - 3, y0, x1, y1, p);
        c.drawRect(x0, cy - 2, x1, cy + 2, p);
        c.drawRect(cx - 1.5f, y0 - 2 * T, cx + 1.5f, y1, p);
        p.setColor(0xFFC88A20);
        c.drawRect(cx - 4, cy - 4, cx + 4, cy + 4, p);
    }

    /** ((27.3) drawDecor's D_CONTAINERS, as it was drawn there.) */
    private void decorContainers(Canvas c, Paint p, int variant, float x0, float y0, float x1, float y1, float dw, float dh) {
        // Stacked shipping containers in rows.
        int[] box = {0xFFB03A2E, 0xFF2E5FB0, 0xFF3C7A4E, 0xFFD4A21C, 0xFF6A6E74, 0xFFE07A2A, 0xFF8A3A6A};
        Random r = new Random(variant * 977L + (long) (x0 * 3 + y0));
        boolean along = dw >= dh;
        float cl = 15, cw = 6;
        for (float a = along ? y0 : x0; a + cw <= (along ? y1 : x1); a += cw + 1)
            for (float b = along ? x0 : y0; b + cl <= (along ? x1 : y1); b += cl + 1.5f) {
                if (r.nextFloat() < 0.12f) continue;
                int col = box[r.nextInt(box.length)];
                float rx0 = along ? b : a, ry0 = along ? a : b, rx1 = along ? b + cl : a + cw, ry1 = along ? a + cw : b + cl;
                p.setColor(0x40000000);
                c.drawRect(rx0 + 1.5f, ry0 + 2, rx1 + 1.5f, ry1 + 2, p);
                p.setColor(col);
                c.drawRect(rx0, ry0, rx1, ry1, p);
                p.setColor(0x30000000);
                if (along) for (float xx = rx0 + 1.5f; xx < rx1; xx += 2) c.drawRect(xx, ry0, xx + 0.5f, ry1, p);
                else for (float yy = ry0 + 1.5f; yy < ry1; yy += 2) c.drawRect(rx0, yy, rx1, yy + 0.5f, p);
            }
    }

    /** ((27.3) drawDecor's D_FLOODLIGHT, as it was drawn there.) */
    private void decorFloodlight(Canvas c, Paint p, float x0, float y0, float x1, float y1) {
        float[][] corners = {{x0, y0}, {x1, y0}, {x0, y1}, {x1, y1}};
        for (float[] k : corners) {
            p.setColor(0x50000000);
            c.drawRect(k[0] - 2, k[1] - 2, k[0] + 5, k[1] + 5, p);
            p.setColor(0xFF4A4C50);
            c.drawRect(k[0] - 3.5f, k[1] - 3.5f, k[0] + 3.5f, k[1] + 3.5f, p);
            p.setColor(0xFFF2F0DA);
            c.drawRect(k[0] - 2.5f, k[1] - 2.5f, k[0] + 2.5f, k[1] - 0.5f, p);
        }
    }

    /** ((27.3) drawDecor's D_TENT, as it was drawn there.) */
    private void decorTent(Canvas c, Paint p, int variant, float cx, float cy) {
        // A ridge tent from above: its fly sheet, the ridge line and the guy ropes.
        int[] fly = {0xFFD86A2A, 0xFF2E7A4E, 0xFF2E5FA8, 0xFFC8A22A, 0xFF8A3A3A, 0xFF5A6A7A};
        int col = fly[(variant / 2) % fly.length];
        boolean along = (variant & 1) == 1;
        float hx = along ? 7 : 5.5f, hy = along ? 5.5f : 7;
        p.setColor(0x40000000);
        c.drawRect(cx - hx + 2, cy - hy + 2.5f, cx + hx + 2, cy + hy + 2.5f, p);
        p.setColor(darken(col, 0.8f));
        c.drawRect(cx - hx, cy - hy, cx + hx, cy + hy, p);
        p.setColor(col);
        if (along) c.drawRect(cx - hx, cy - hy, cx + hx, cy, p);
        else c.drawRect(cx - hx, cy - hy, cx, cy + hy, p);
        p.setColor(lighten(col, 0.3f));
        if (along) c.drawRect(cx - hx, cy - 0.5f, cx + hx, cy + 0.5f, p);
        else c.drawRect(cx - 0.5f, cy - hy, cx + 0.5f, cy + hy, p);
        p.setColor(0x80E8E4D8);
        p.setStrokeWidth(0.4f);
        c.drawLine(cx - hx, cy - hy, cx - hx - 2, cy - hy - 2, p);
        c.drawLine(cx + hx, cy + hy, cx + hx + 2, cy + hy + 2, p);
    }

    /** ((27.3) drawDecor's D_CAMPFIRE, as it was drawn there.) */
    private void decorCampfire(Canvas c, Paint p, float cx, float cy) {
        // A ring of stones round the ashes, a log to sit on.
        p.setColor(0xFF8A8478);
        c.drawCircle(cx, cy, 3.6f, p);
        p.setColor(0xFF2E2A26);
        c.drawCircle(cx, cy, 2.4f, p);
        p.setColor(0xFFE0762A);
        c.drawCircle(cx, cy, 1.2f, p);
        p.setColor(0xFF6A4A2E);
        c.drawRect(cx - 4, cy + 5, cx + 4, cy + 6.6f, p);
    }

    /** ((27.3) drawDecor's D_PICNIC, as it was drawn there.) */
    private void decorPicnic(Canvas c, Paint p, int variant, float x0, float y0, float x1, float y1, float cx, float cy) {
        boolean along = variant == 1;
        p.setColor(0x40000000);
        c.drawRect(x0 + 2, y0 + 2, x1 + 2, y1 + 2, p);
        p.setColor(0xFF7A5434);
        if (along) {
            c.drawRect(x0, cy - 2.5f, x1, cy + 2.5f, p);
            p.setColor(0xFF64442A);
            c.drawRect(x0 + 1, cy - 5, x1 - 1, cy - 3.6f, p);
            c.drawRect(x0 + 1, cy + 3.6f, x1 - 1, cy + 5, p);
        } else {
            c.drawRect(cx - 2.5f, y0, cx + 2.5f, y1, p);
            p.setColor(0xFF64442A);
            c.drawRect(cx - 5, y0 + 1, cx - 3.6f, y1 - 1, p);
            c.drawRect(cx + 3.6f, y0 + 1, cx + 5, y1 - 1, p);
        }
    }

    /** ((27.3) drawDecor's D_FALLS, as it was drawn there.) */
    private void decorFalls(Canvas c, Paint p, int variant, float x0, float y0, float x1, float y1, float cx, float cy) {
        // White water tumbling over the lip of the rock, spray at the foot.
        boolean across = variant == 1;
        p.setColor(0xFFE8F2F6);
        if (across) c.drawRect(cx - 3, y0 + 3, cx + 3, y1 - 3, p);
        else c.drawRect(x0 + 3, cy - 3, x1 - 3, cy + 3, p);
        p.setColor(0xFFB8D8E4);
        for (int k = 0; k < 5; k++) {
            float a = -8 + k * 4;
            if (across) c.drawRect(cx - 3 + k * 1.4f, y0 + 4, cx - 2.4f + k * 1.4f, y1 - 4, p);
            else c.drawRect(x0 + 4, cy - 3 + k * 1.4f, x1 - 4, cy - 2.4f + k * 1.4f, p);
            p.setColor(0x90F4FAFC);
            c.drawCircle(cx + (across ? 0 : a) + 2, cy + (across ? a : 0) + 2, 2.2f, p);
            p.setColor(0xFFB8D8E4);
        }
    }

    /** ((27.3) drawDecor's D_SIGN, as it was drawn there.) */
    private void decorSign(Canvas c, Paint p, float cx, float cy) {
        // The trailhead: a wooden board on two posts.
        p.setColor(0x50000000);
        c.drawRect(cx - 5, cy - 1, cx + 7, cy + 4, p);
        p.setColor(0xFF6E4A2A);
        c.drawRect(cx - 6, cy - 3, cx + 6, cy + 2, p);
        p.setColor(0xFFE8D8A8);
        c.drawRect(cx - 4.5f, cy - 1.6f, cx + 4.5f, cy - 1, p);
        c.drawRect(cx - 4.5f, cy, cx + 2, cy + 0.6f, p);
    }

    /** ((27.3) drawDecor's D_FIRETOWER, as it was drawn there.) */
    private void decorFiretower(Canvas c, Paint p, float cx, float cy) {
        // A fire lookout: four steel legs splayed out under a little glass-walled cab, a stair zigzagging up.
        p.setColor(0x40000000);
        Path sh = new Path();
        sh.moveTo(cx - 12, cy - 12);
        sh.lineTo(cx + 12, cy - 12);
        sh.lineTo(cx + 40, cy + 30);
        sh.lineTo(cx + 22, cy + 46);
        sh.close();
        c.drawPath(sh, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.6f);
        p.setColor(0xFF5A5E62);
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sy = -1; sy <= 1; sy += 2) c.drawLine(cx + sx * 13, cy + sy * 13, cx + sx * 5, cy + sy * 5, p);
        p.setStrokeWidth(0.7f);
        p.setColor(0xFF7A7E82);
        c.drawLine(cx - 9, cy - 9, cx + 9, cy + 9, p);
        c.drawLine(cx + 9, cy - 9, cx - 9, cy + 9, p);
        // The stair.
        p.setColor(0xFF6E5236);
        for (int k = 0; k < 4; k++) c.drawLine(cx + 6, cy - 8 + k * 4, cx + 11, cy - 6 + k * 4, p);
        p.setStyle(Paint.Style.FILL);
        // The cab: windows all round and a pyramid roof.
        p.setColor(0xFF8FB8C8);
        c.drawRect(cx - 7, cy - 7, cx + 7, cy + 7, p);
        p.setColor(0xFF6E4A32);
        Path roof = new Path();
        roof.moveTo(cx - 6, cy - 6);
        roof.lineTo(cx + 6, cy - 6);
        roof.lineTo(cx, cy);
        roof.close();
        c.drawPath(roof, p);
        p.setColor(0xFF5A3C28);
        roof.reset();
        roof.moveTo(cx + 6, cy - 6);
        roof.lineTo(cx + 6, cy + 6);
        roof.lineTo(cx, cy);
        roof.close();
        c.drawPath(roof, p);
        roof.reset();
        roof.moveTo(cx - 6, cy + 6);
        roof.lineTo(cx + 6, cy + 6);
        roof.lineTo(cx, cy);
        roof.close();
        c.drawPath(roof, p);
        p.setColor(0xFF7E5840);
        roof.reset();
        roof.moveTo(cx - 6, cy - 6);
        roof.lineTo(cx - 6, cy + 6);
        roof.lineTo(cx, cy);
        roof.close();
        c.drawPath(roof, p);
    }

    /** ((27.3) drawDecor's D_LOOKOUT, as it was drawn there.) */
    private void decorLookout(Canvas c, Paint p, float x0, float y0, float x1, float y1, float cx, float cy) {
        // A timber platform with a rail and a bench, looking out.
        p.setColor(0x50000000);
        c.drawRect(x0 + 3, y0 + 4, x1 + 3, y1 + 4, p);
        p.setColor(0xFF8A6A44);
        c.drawRect(x0, y0, x1, y1, p);
        p.setColor(0xFF6E5234);
        for (float yy = y0 + 2; yy < y1; yy += 3) c.drawRect(x0, yy, x1, yy + 0.6f, p);
        p.setColor(0xFF4E3A24);
        c.drawRect(x0, y0, x1, y0 + 1.4f, p);
        c.drawRect(x0, y0, x0 + 1.4f, y1, p);
        c.drawRect(x1 - 1.4f, y0, x1, y1, p);
        p.setColor(0xFF5A4028);
        c.drawRect(cx - 5, cy + 2, cx + 5, cy + 4.5f, p);
    }

    // ------------------------------------------------------------------ drawing the lie of the land

    /** Light and shade on the slopes, snow on the peaks: a picture one pixel to a tile, drawn smoothed. */
    Bitmap shade;

    Bitmap shadeMap() {
        if (shade != null) return shade;
        float top = 0;
        for (int q = 0; q < city.elev.length; q++) top = Math.max(top, city.elev.get(q));
        // (Only real mountains get snow.)
        // (Snow lower down in the north and the Alps; none in the tropics or the desert.)
        float snow = !city.desert && city.country.snowLine > 0 && top > 55 ? top * city.country.snowLine : Float.MAX_VALUE;
        int[] px = new int[city.w * city.h];
        for (int y = 0; y < city.h; y++)
            for (int x = 0; x < city.w; x++) {
                int i = y * city.w + x;
                if (city.tiles[i] == WATER) continue;
                float gx = (city.elev.get(y * city.w + Math.min(city.w - 1, x + 1)) - city.elev.get(y * city.w + Math.max(0, x - 1))) / 2;
                float gy = (city.elev.get(Math.min(city.h - 1, y + 1) * city.w + x) - city.elev.get(Math.max(0, y - 1) * city.w + x)) / 2;
                // Lit from the north-west, like the buildings' shadows.
                float lit = (gx + gy) * 0.707f * (city.cfg.civic >= 9 ? 0.11f : 0.075f);
                float white = Math.min(0.32f, Math.max(0, lit)), black = Math.min(0.5f, Math.max(0, -lit));
                if (city.elev.get(i) > snow) white = Math.max(white, Math.min(1, (city.elev.get(i) - snow) / (top - snow) * 2.5f) * 0.7f);
                float a = Math.min(0.75f, white + black);
                if (a < 0.01f) continue;
                // (Snow and sunny slopes a little warm; shade a little blue.)
                boolean light = white >= black;
                px[i] = ((int) (a * (light ? 255 : 150)) << 24) | (light ? 0xFFFCF8 : 0x0A1428);
            }
        shade = Bitmap.createBitmap(px, city.w, city.h, Bitmap.Config.ARGB_8888);
        return shade;
    }

    /** A tile's ground colour for rounding the banks, or 0 if it isn't open ground. */
    int bankColor(int x, int y) {
        if (x < 0 || y < 0 || x >= city.w || y >= city.h) return 0;
        byte t = city.tiles[y * city.w + x];
        if (city.bridge != null && city.bridge[y * city.w + x]) return 0;
        if (t == GRASS || t == TREE || t == TRAIL) return city.desert ? grassColour(x, y) : city.country.grass != 0 ? city.country.grass : city.drawnRealistic ? 0xFF4F6F36 : 0xFF4C7837;
        if (t == ROCK) return 0xFF7E796E;
        if (t == SAND) return sandColor();
        return 0;
    }

    /** Sand: a beach's. */
    int sandColor() {
        return 0xFFDCCB9E;
    }

    /**
     * ((22.4) River City's banks as smooth curves, not steps a tile at a time: along each bank, the water's edge
     * tiles are painted as the bank, then the water drawn back over them to the true line, with a pale shallow edge
     * and a dark line of mud along it. Not where a road, pavement or bridge meets the water (those keep their edge).
     */
    void smoothRiver(Canvas c, Paint p, int X0, int Y0, int X1, int Y1) {
        boolean across = city.riverAcross;
        int L = city.riverC.length, B = across ? city.h : city.w;
        int a0 = Math.max(0, (across ? X0 : Y0) - 1), a1 = Math.min(L, (across ? X1 : Y1) + 1);
        Path path = new Path();
        p.setStyle(Paint.Style.FILL);
        for (int a = a0; a < a1; a++) {
            for (int side = 0; side < 2; side++) {
                boolean top = side == 0;
                int e = top ? city.riverEdge0[a] : city.riverEdge1[a];
                float s0 = city.bankAt(a, top), s1 = city.bankAt(a + 1, top);
                // The tiles at the bank: from the land beside the edge to the far side of the true line.
                int z0 = Math.max(0, Math.min(e, (int) Math.floor(Math.min(s0, s1))) - 1);
                int z1 = Math.min(B, Math.max(e + 1, (int) Math.ceil(Math.max(s0, s1))) + 1);
                int land = top ? e - 1 : e + 1;
                if (land < 0 || land >= B) continue;
                boolean ok = true;
                for (int b = Math.max(0, z0 - 1); b <= Math.min(B - 1, z1) && ok; b++) {
                    int t = across ? b * city.w + a : a * city.w + b;
                    byte ty = city.tiles[t];
                    if ((city.bridge != null && city.bridge[t]) || !(ty == WATER || ty == GRASS || ty == TREE || ty == TRAIL || ty == SAND)) ok = false;
                }
                if (!ok) continue;
                int lx = across ? a : land, ly = across ? land : a;
                int lc = bankColor(lx, ly);
                if (lc == 0) continue;
                int wx = across ? a : e, wy = across ? e : a;
                int wc = waterColor(wx, wy);
                float u0 = a * T, u1 = (a + 1) * T;
                int sc = c.save();
                if (across) c.clipRect(u0, z0 * T, u1, z1 * T);
                else c.clipRect(z0 * T, u0, z1 * T, u1);
                // Edge water tiles become bank; then the water back to the true line.
                for (int b = z0; b < z1; b++) {
                    int t = across ? b * city.w + a : a * city.w + b;
                    if (city.tiles[t] != WATER) continue;
                    p.setColor(lc);
                    if (across) c.drawRect(u0, b * T, u1, (b + 1) * T, p);
                    else c.drawRect(b * T, u0, (b + 1) * T, u1, p);
                }
                float far = top ? z1 * T + T : z0 * T - T;
                path.reset();
                if (across) {
                    path.moveTo(u0, s0 * T);
                    path.lineTo(u1, s1 * T);
                    path.lineTo(u1, far);
                    path.lineTo(u0, far);
                } else {
                    path.moveTo(s0 * T, u0);
                    path.lineTo(s1 * T, u1);
                    path.lineTo(far, u1);
                    path.lineTo(far, u0);
                }
                path.close();
                p.setColor(wc);
                c.drawPath(path, p);
                // The shallows, the line of the bank and a little mud.
                float in = top ? 2.2f : -2.2f;
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(3f);
                p.setColor(0x4078B0C8);
                if (across) c.drawLine(u0, s0 * T + in, u1, s1 * T + in, p);
                else c.drawLine(s0 * T + in, u0, s1 * T + in, u1, p);
                p.setStrokeWidth(1.4f);
                p.setColor(0x705A4A30);
                if (across) c.drawLine(u0, s0 * T, u1, s1 * T, p);
                else c.drawLine(s0 * T, u0, s1 * T, u1, p);
                p.setStyle(Paint.Style.FILL);
                c.restoreToCount(sc);
            }
        }
    }

    /** Streams and lakes with rounded banks instead of square steps. */
    void roundBanks(Canvas c, Paint p, int X0, int Y0, int X1, int Y1) {
        p.setStyle(Paint.Style.FILL);
        for (int y = Y0; y < Y1; y++)
            for (int x = X0; x < X1; x++) {
                boolean wt = city.wet(x, y);
                float cx = x * T + T / 2f, cy = y * T + T / 2f;
                for (int dy = -1; dy <= 1; dy += 2)
                    for (int dx = -1; dx <= 1; dx += 2) {
                        float qx = dx < 0 ? x * T : cx, qy = dy < 0 ? y * T : cy;
                        if (wt && !city.wet(x + dx, y) && !city.wet(x, y + dy)) {
                            // An outer corner of the water: the bank rounds it off.
                            int land = bankColor(x + dx, y);
                            if (land == 0) land = bankColor(x, y + dy);
                            if (land == 0) continue;
                            p.setColor(land);
                            c.drawRect(qx, qy, qx + T / 2f, qy + T / 2f, p);
                            p.setColor(waterColor(x, y));
                            c.drawCircle(cx, cy, T / 2f, p);
                        } else if (!wt && bankColor(x, y) != 0 && city.wet(x + dx, y) && city.wet(x, y + dy) && city.wet(x + dx, y + dy)) {
                            // An inner corner: the water fills it in a curve.
                            p.setColor(waterColor(x + dx, y));
                            c.drawCircle(dx < 0 ? x * T : x * T + T, dy < 0 ? y * T : y * T + T, T / 2f, p);
                        }
                    }
            }
    }

    /** The trails as smooth curves through their tiles, and posts where they cross a road or the railway. */
    void drawTrailLines(Canvas c, Paint p, int X0, int Y0, int X1, int Y1) {
        float rx0 = (X0 - 2) * T, ry0 = (Y0 - 2) * T, rx1 = (X1 + 2) * T, ry1 = (Y1 + 2) * T;
        Path path = new Path();
        for (int pass = 0; pass < 2; pass++) {
            p.setStrokeWidth(pass == 0 ? 5.5f : 2.6f);
            p.setColor(pass == 0 ? 0xFF8C7454 : 0xFFAE9670);
            for (float[] l : city.trailLines) {
                float lx0 = Float.MAX_VALUE, ly0 = Float.MAX_VALUE, lx1 = -Float.MAX_VALUE, ly1 = -Float.MAX_VALUE;
                for (int k = 0; k < l.length; k += 2) {
                    lx0 = Math.min(lx0, l[k]);
                    lx1 = Math.max(lx1, l[k]);
                    ly0 = Math.min(ly0, l[k + 1]);
                    ly1 = Math.max(ly1, l[k + 1]);
                }
                if (lx1 < rx0 || lx0 > rx1 || ly1 < ry0 || ly0 > ry1) continue;
                // Through the midpoints, curving at each tile: no stair-steps.
                path.reset();
                path.moveTo(l[0], l[1]);
                int n = l.length / 2;
                for (int k = 1; k < n - 1; k++)
                    path.quadTo(l[k * 2], l[k * 2 + 1], (l[k * 2] + l[k * 2 + 2]) / 2, (l[k * 2 + 1] + l[k * 2 + 3]) / 2);
                path.lineTo(l[(n - 1) * 2], l[(n - 1) * 2 + 1]);
                c.drawPath(path, p);
            }
        }
        p.setStyle(Paint.Style.FILL);
        for (float[] x : city.trailCrossings) {
            if (x[0] < rx0 || x[0] > rx1 || x[1] < ry0 || x[1] > ry1) continue;
            if (x[2] == RAIL) {
                // Boards laid between the rails.
                p.setColor(0xFF7A5A3A);
                c.drawRect(x[0] - 6, x[1] - 6, x[0] + 6, x[1] + 6, p);
                p.setColor(0xFF5A4028);
                for (int k = -1; k <= 1; k++) c.drawRect(x[0] - 6, x[1] + k * 4 - 0.4f, x[0] + 6, x[1] + k * 4 + 0.4f, p);
            } else {
                // A crossing: white bars across the road.
                p.setColor(0xD8E8E8E0);
                for (int k = -1; k <= 1; k++) c.drawRect(x[0] + k * 4 - 1.2f, x[1] - 6, x[0] + k * 4 + 1.2f, x[1] + 6, p);
            }
        }
        p.setStyle(Paint.Style.STROKE);
    }

    /** Hill shading, contour lines out in the country, trails and their footbridges. */
    void drawTerrain(Canvas c, Paint p, int X0, int Y0, int X1, int Y1) {
        if (city.elev == null) return;
        if (city.waterKind == CityConfig.W_NONE || city.waterKind == CityConfig.W_RIVER) roundBanks(c, p, X0, Y0, X1, Y1);
        // Stones in the fords, where people wade across.
        if (city.ford != null)
            for (int y = Math.max(0, Y0); y < Math.min(city.h, Y1); y++)
                for (int x = Math.max(0, X0); x < Math.min(city.w, X1); x++) {
                    if (!city.isFord(y * city.w + x) || (city.bridge != null && city.bridge[y * city.w + x])) continue;
                    prnd.setSeed(tileSeed(x, y, 7));
                    for (int k = 0; k < 3; k++) {
                        float sx = x * T + 2 + prnd.nextFloat() * (T - 4), sy = y * T + 2 + prnd.nextFloat() * (T - 4);
                        p.setColor(0x60202830);
                        c.drawCircle(sx + 0.6f, sy + 0.8f, 1.4f, p);
                        p.setColor(0xFF8A9088);
                        c.drawCircle(sx, sy, 1.3f, p);
                    }
                }
        // Trails: a worn earth path winding along (or, on older maps, joining up from tile to tile).
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        if (!city.trailLines.isEmpty()) drawTrailLines(c, p, X0, Y0, X1, Y1);
        for (int pass = 0; pass < 2 && city.trailLines.isEmpty(); pass++) {
            p.setStrokeWidth(pass == 0 ? 5.5f : 2.6f);
            p.setColor(pass == 0 ? 0xFF8C7454 : 0xFFAE9670);
            for (int y = Math.max(0, Y0 - 1); y < Math.min(city.h, Y1 + 1); y++)
                for (int x = Math.max(0, X0 - 1); x < Math.min(city.w, X1 + 1); x++) {
                    if (city.tiles[y * city.w + x] != TRAIL) continue;
                    float fx = x * T + T / 2f, fy = y * T + T / 2f;
                    boolean any = false;
                    for (int[] d : new int[][]{{1, 0}, {0, 1}, {-1, 0}, {0, -1}}) {
                        int nx = x + d[0], ny = y + d[1];
                        if (nx < 0 || ny < 0 || nx >= city.w || ny >= city.h) continue;
                        byte o = city.tiles[ny * city.w + nx];
                        if (o == TRAIL) any = true;
                        if ((o == TRAIL && (d[0] > 0 || d[1] > 0)) || o == LOT || o == DIRT)
                            c.drawLine(fx, fy, fx + d[0] * T, fy + d[1] * T, p);
                    }
                    if (!any) c.drawCircle(fx, fy, 1, p);
                }
        }
        p.setStyle(Paint.Style.FILL);
        // Footbridges over the streams.
        for (int y = Y0; y < Y1; y++)
            for (int x = X0; x < X1; x++) {
                int i = y * city.w + x;
                if (city.tiles[i] != TRAIL || city.bridge == null || !city.bridge[i]) continue;
                float fx = x * T, fy = y * T;
                p.setColor(waterColor(x, y));
                c.drawRect(fx, fy, fx + T, fy + T, p);
                boolean ns = (y > 0 && city.tiles[i - city.w] == TRAIL) || (y < city.h - 1 && city.tiles[i + city.w] == TRAIL);
                p.setColor(0x50000000);
                if (ns) c.drawRect(fx + 5, fy, fx + 13, fy + T, p);
                else c.drawRect(fx, fy + 5, fx + T, fy + 13, p);
                p.setColor(0xFF8C6C4A);
                if (ns) c.drawRect(fx + 4, fy, fx + 12, fy + T, p);
                else c.drawRect(fx, fy + 4, fx + T, fy + 12, p);
                p.setColor(0xFF6E5236);
                for (int k = 1; k < 4; k++) {
                    if (ns) c.drawRect(fx + 4, fy + k * 4, fx + 12, fy + k * 4 + 0.6f, p);
                    else c.drawRect(fx + k * 4, fy + 4, fx + k * 4 + 0.6f, fy + 12, p);
                }
            }
        // Light and shade.
        Paint sp = new Paint();
        sp.setFilterBitmap(true);
        int sx0 = Math.max(0, X0), sy0 = Math.max(0, Y0), sx1 = Math.min(city.w, X1), sy1 = Math.min(city.h, Y1);
        Rect src = new Rect();
        src.set(sx0, sy0, sx1, sy1);
        c.drawBitmap(shadeMap(), src, new RectF(sx0 * T, sy0 * T, sx1 * T, sy1 * T), sp);
        // Contours out in the country, every 4 (a darker one every 20), traced through the tile centres.
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.BUTT);
        for (int y = Math.max(0, Y0 - 1); y < Math.min(city.h - 1, Y1); y++)
            for (int x = Math.max(0, X0 - 1); x < Math.min(city.w - 1, X1); x++) {
                if (x >= city.townX0 - 2 && x < city.townX1 + 2 && y >= city.townY0 - 2 && y < city.townY1 + 2) continue;
                int i = y * city.w + x;
                if (city.tiles[i] == WATER || city.tiles[i + 1] == WATER || city.tiles[i + city.w] == WATER || city.tiles[i + city.w + 1] == WATER) continue;
                float a = city.elev.get(i), b = city.elev.get(i + 1), cc = city.elev.get(i + city.w + 1), d = city.elev.get(i + city.w);
                float lo = Math.min(Math.min(a, b), Math.min(cc, d)), hi = Math.max(Math.max(a, b), Math.max(cc, d));
                for (float lv = (float) Math.ceil(lo / 4) * 4; lv < hi; lv += 4) {
                    boolean major = Math.round(lv) % 20 == 0;
                    p.setStrokeWidth(major ? 1.3f : 0.8f);
                    p.setColor(major ? 0x46302010 : 0x26302010);
                    float ox = x * T + T / 2f, oy = y * T + T / 2f;
                    // Where the level crosses each edge of the cell.
                    float[] pts = new float[8];
                    int n = 0;
                    if ((a < lv) != (b < lv)) { pts[n++] = ox + T * (lv - a) / (b - a); pts[n++] = oy; }
                    if ((b < lv) != (cc < lv)) { pts[n++] = ox + T; pts[n++] = oy + T * (lv - b) / (cc - b); }
                    if ((d < lv) != (cc < lv) && n < 8) { pts[n++] = ox + T * (lv - d) / (cc - d); pts[n++] = oy + T; }
                    if ((a < lv) != (d < lv) && n < 8) { pts[n++] = ox; pts[n++] = oy + T * (lv - a) / (d - a); }
                    if (n >= 4) c.drawLine(pts[0], pts[1], pts[2], pts[3], p);
                    if (n >= 8) c.drawLine(pts[4], pts[5], pts[6], pts[7], p);
                }
            }
        p.setStyle(Paint.Style.FILL);
    }

    boolean isLotCar(int x, int y) {
        return (x > 0 && city.tiles[y * city.w + x - 1] == LOT) || (x < city.w - 1 && city.tiles[y * city.w + x + 1] == LOT)
                || (y > 0 && city.tiles[(y - 1) * city.w + x] == LOT) || (y < city.h - 1 && city.tiles[(y + 1) * city.w + x] == LOT);
    }

    /** Gravel, sleepers and rails, carried across the level crossings with barrier markings. */
    void drawRail(Canvas c, Paint p) {
        if (city.railY0 < 0) return;
        float y0 = city.railY0 * T, y1 = (city.railY0 + city.railRows) * T, cy = (y0 + y1) / 2;
        for (int x = city.regionOnly ? city.rx0 : 0; x < (city.regionOnly ? city.rx1 : city.w); x++) {
            prnd.setSeed(tileSeed(x, 0, 4));
            float fx = x * T;
            boolean crossing = city.tiles[city.railY0 * city.w + x] == ROAD;
            if (!crossing) {
                p.setColor(0xFF6E665C);
                c.drawRect(fx, y0 + 6, fx + T, y1 - 6, p);
                for (int k = 0; k < 10; k++) {
                    p.setColor(prnd.nextBoolean() ? 0xFF7E766A : 0xFF5E574E);
                    c.drawCircle(fx + prnd.nextFloat() * T, y0 + 6 + prnd.nextFloat() * (y1 - y0 - 12), 0.8f, p);
                }
                p.setColor(0xFF4A3A2C);
                for (float sx = fx + 1; sx < fx + T; sx += 4) c.drawRect(sx, cy - 8, sx + 2, cy + 8, p);
            } else {
                // Level crossing: yellow boxes and stop lines either side.
                p.setColor(0x55E8C547);
                c.drawRect(fx, y0 + 4, fx + T, y0 + 6, p);
                c.drawRect(fx, y1 - 6, fx + T, y1 - 4, p);
            }
        }
        p.setColor(0xFFB8BCC0);
        c.drawRect(0, cy - 6, city.w * T, cy - 4.8f, p);
        c.drawRect(0, cy + 4.8f, city.w * T, cy + 6, p);
    }

    /** Benches, bins, fire hydrants, bus stops and traffic lights along the pavements. */
    void drawStreetFurniture(Canvas c, Paint p) {
        for (int y = Math.max(1, city.regionOnly ? city.ry0 : 0); y < Math.min(city.h - 1, city.regionOnly ? city.ry1 : city.h); y++)
            for (int x = Math.max(1, city.regionOnly ? city.rx0 : 0); x < Math.min(city.w - 1, city.regionOnly ? city.rx1 : city.w); x++) {
                prnd.setSeed(tileSeed(x, y, 5));
                if (city.tiles[y * city.w + x] != SIDEWALK) continue;
                // Which side faces the road?
                int dx = 0, dy = 0;
                if (city.paved(x - 1, y)) dx = -1;
                else if (city.paved(x + 1, y)) dx = 1;
                else if (city.paved(x, y - 1)) dy = -1;
                else if (city.paved(x, y + 1)) dy = 1;
                if (dx == 0 && dy == 0) continue;
                boolean corner = (city.paved(x - 1, y) || city.paved(x + 1, y)) && (city.paved(x, y - 1) || city.paved(x, y + 1));
                float cx = x * T + T / 2f + dx * 5, cy = y * T + T / 2f + dy * 5;
                float roll = prnd.nextFloat();
                if (corner) {
                    if (roll < 0.35f && city.onMainRoad(x - 2, y - 2, x + 2, y + 2)) {
                        // Traffic light.
                        p.setColor(0xFF2A2C30);
                        c.drawRect(cx - 1.5f, cy - 3, cx + 1.5f, cy + 3, p);
                        p.setColor(0xFFE0302A);
                        c.drawCircle(cx, cy - 1.8f, 0.8f, p);
                        p.setColor(0xFF3AD06A);
                        c.drawCircle(cx, cy + 1.8f, 0.8f, p);
                    }
                    continue;
                }
                if (roll < 0.035f) {
                    // Bench, parallel to the kerb.
                    p.setColor(0xFF7A5634);
                    if (dx != 0) c.drawRect(cx - 1.2f, cy - 4, cx + 1.2f, cy + 4, p);
                    else c.drawRect(cx - 4, cy - 1.2f, cx + 4, cy + 1.2f, p);
                } else if (roll < 0.06f) {
                    // Litter bin.
                    p.setColor(0xFF3C4A3C);
                    c.drawCircle(cx, cy, 1.6f, p);
                    p.setColor(0xFF232A23);
                    c.drawCircle(cx, cy, 0.9f, p);
                } else if (roll < 0.075f) {
                    // Fire hydrant.
                    p.setColor(0xFFD8302A);
                    c.drawCircle(cx, cy, 1.4f, p);
                    p.setColor(0xFFF0C040);
                    c.drawCircle(cx, cy, 0.6f, p);
                } else if (roll < 0.085f && city.onMainRoad(x - 2, y - 2, x + 2, y + 2)) {
                    // Bus stop shelter.
                    p.setColor(0xAA9CC3D9);
                    if (dx != 0) c.drawRect(cx - 2, cy - 6, cx + 2, cy + 6, p);
                    else c.drawRect(cx - 6, cy - 2, cx + 6, cy + 2, p);
                    p.setColor(0xFF3A5A8A);
                    c.drawCircle(cx + dx * 3 + (dy != 0 ? 7 : 0), cy + dy * 3 + (dx != 0 ? 7 : 0), 1.2f, p);
                }
            }
    }

    /**
     * Turn lanes on the avenues: just before each junction, the lane nearest the middle is for turning across
     * the traffic (left, or right where they drive on the left) and the kerb lane is for going straight on or
     * turning off. The arrows are painted on whichever side of the road the country drives on.
     */
    void turnLanes(Canvas c, Paint p, Street st, int n) {
        float drive = 1;
        float mid = st.vertical ? st.x0 + st.width / 2f : st.y0 + st.width / 2f;
        p.setColor(0xE0ECECEC);
        p.setStrokeWidth(1.4f);
        for (int k = 2; k < n - 2; k++) {
            int cx = st.vertical ? st.x0 + st.width / 2 : st.x0 + k, cy = st.vertical ? st.y0 + k : st.y0 + st.width / 2;
            if (!city.paved(cx, cy) || city.junctionAt(cx, cy) || city.crosswalk(cx, cy)) continue;
            for (int s = -1; s <= 1; s += 2) {
                // Heading towards a crossing and junction just ahead?
                int ax = st.vertical ? cx : cx + s, ay = st.vertical ? cy + s : cy;
                int bx = st.vertical ? cx : cx + 2 * s, by = st.vertical ? cy + 2 * s : cy;
                if (!city.crosswalk(ax, ay) || !city.junctionAt(bx, by)) continue;
                float hx = st.vertical ? 0 : s, hy = st.vertical ? s : 0;
                // The right-hand side of someone driving that way.
                float rx = -hy, ry = hx;
                float along = (st.vertical ? cy : cx) + 0.5f;
                // ((11.1) The lanes where traffic drives them: the inside one turns across, the outside one goes
                // straight on or turns off, a middle one (on a boulevard) straight on.)
                int per = st.width >= 7 ? 3 : 2;
                float lw = st.width / 2f / per;
                for (int lane = 0; lane < per; lane++) {
                    float off = (lane + 0.5f) * lw * drive;
                    float lx = st.vertical ? mid + rx * off : along, ly = st.vertical ? along : mid + ry * off;
                    float px = lx * T, py = ly * T;
                    if (lane == per - 1) {
                        arrow(c, p, px, py, hx, hy, 0);
                        arrow(c, p, px, py, hx, hy, drive);
                    } else if (lane == 0) {
                        arrow(c, p, px, py, hx, hy, -drive);
                    } else arrow(c, p, px, py, hx, hy, 0);
                }
            }
        }
    }

    /**
     * Where each street meets a junction: a stop line across the lanes coming in, with STOP
     * painted before stop junctions and a dashed give-way line at roundabouts.
     */
    void stopLines(Canvas c, Paint p, Street st, int n) {
        float drive = 1;
        for (int k = 1; k < n - 1; k++) {
            int cx = st.vertical ? st.x0 + st.width / 2 : st.x0 + k, cy = st.vertical ? st.y0 + k : st.y0 + st.width / 2;
            if (!city.paved(cx, cy) || city.junctionAt(cx, cy) || city.crosswalk(cx, cy)) continue;
            for (int s = -1; s <= 1; s += 2) {
                int ax = st.vertical ? cx : cx + s, ay = st.vertical ? cy + s : cy;
                int bx = st.vertical ? cx : cx + 2 * s, by = st.vertical ? cy + 2 * s : cy;
                if (!city.crosswalk(ax, ay) || !city.junctionAt(bx, by)) continue;
                int id = city.junctionId.get(by * city.w + bx);
                int type = city.junctions.get(id)[4];
                if (type == J_BEND) continue;
                float hx = st.vertical ? 0 : s, hy = st.vertical ? s : 0, rx = -hy, ry = hx;
                // The edge of this tile nearest the junction, and the half of the road coming in.
                float ex = (cx + 0.5f) * T + hx * T / 2, ey = (cy + 0.5f) * T + hy * T / 2;
                float mid = st.width * T / 2f;
                float sx = st.vertical ? st.x0 * T + mid : ex, sy = st.vertical ? ey : st.y0 * T + mid;
                float ox = rx * drive, oy = ry * drive;
                p.setColor(0xE8EEEEEE);
                if (type == J_ROUNDABOUT) {
                    p.setStrokeWidth(1.2f);
                    for (float q = 1; q < mid - 1; q += 3)
                        c.drawLine(sx + ox * q, sy + oy * q, sx + ox * (q + 1.6f), sy + oy * (q + 1.6f), p);
                    continue;
                }
                p.setStrokeWidth(1.8f);
                c.drawLine(sx + ox * 0.8f, sy + oy * 0.8f, sx + ox * (mid - 1), sy + oy * (mid - 1), p);
                if (type == J_STOP && st.width == 3) {
                    String word = "STOP";
                    float lx = sx + ox * mid * 0.5f - hx * 9, ly = sy + oy * mid * 0.5f - hy * 9;
                    c.save();
                    c.translate(lx, ly);
                    // Read by the driver coming in.
                    c.rotate((float) Math.toDegrees(Math.atan2(hy, hx)) + 90);
                    p.setTextAlign(Paint.Align.CENTER);
                    p.setTextSize(5.5f);
                    p.setFakeBoldText(true);
                    c.drawText(word, 0, 2, p);
                    p.setFakeBoldText(false);
                    c.restore();
                }
            }
        }
    }

    /** A painted arrow at (x, y) pointing along (hx, hy); turn 0 is straight on, +1 bends right, -1 left. */
    static void arrow(Canvas c, Paint p, float x, float y, float hx, float hy, float turn) {
        float rx = -hy, ry = hx;
        if (turn == 0) {
            c.drawLine(x - hx * 5, y - hy * 5, x + hx * 3, y + hy * 3, p);
            c.drawLine(x + hx * 5, y + hy * 5, x + hx * 2.5f - rx * 1.8f, y + hy * 2.5f - ry * 1.8f, p);
            c.drawLine(x + hx * 5, y + hy * 5, x + hx * 2.5f + rx * 1.8f, y + hy * 2.5f + ry * 1.8f, p);
            c.drawLine(x + hx * 5, y + hy * 5, x + hx * 3, y + hy * 3, p);
            return;
        }
        float ex = x + hx * 1 + rx * turn * 3.5f, ey = y + hy * 1 + ry * turn * 3.5f;
        c.drawLine(x - hx * 5, y - hy * 5, x + hx * 1, y + hy * 1, p);
        c.drawLine(x + hx * 1, y + hy * 1, ex, ey, p);
        float tx = rx * turn, ty = ry * turn;
        c.drawLine(ex + tx * 1.5f, ey + ty * 1.5f, ex - hx * 1.8f, ey - hy * 1.8f, p);
        c.drawLine(ex + tx * 1.5f, ey + ty * 1.5f, ex + hx * 1.8f, ey + hy * 1.8f, p);
    }

    /** A rectangle on the highway, in tiles along it (k) and across it from its first row (a). */
    void hwyRect(Canvas c, Paint p, float k0, float k1, float a0, float a1) {
        if (city.hwyAxis == 0) c.drawRect(k0 * T, (city.hwyAt + a0) * T, k1 * T, (city.hwyAt + a1) * T, p);
        else c.drawRect((city.hwyAt + a0) * T, k0 * T, (city.hwyAt + a1) * T, k1 * T, p);
    }

    /**
     * The highway: smoother, darker asphalt, white edge lines, dashed lane lines, a concrete barrier down the
     * middle, stop lines at the lights, green signs before each junction and the route shield at each end.
     */
    void drawHighway(Canvas c, Paint p) {
        if (city.hwyAxis < 0) return;
        int len = city.hwyAxis == 0 ? city.w : city.h;
        for (int k = 0; k < len; k++) {
            int x = city.hwyAxis == 0 ? k : city.hwyAt, y = city.hwyAxis == 0 ? city.hwyAt : k;
            int cx = city.hwyAxis == 0 ? k : city.hwyAt + 1, cy = city.hwyAxis == 0 ? city.hwyAt + 1 : k;
            boolean junc = city.junctionAt(cx, cy) || city.junctionAt(city.hwyAxis == 0 ? k : city.hwyAt + 5, city.hwyAxis == 0 ? city.hwyAt + 5 : k);
            boolean rail = city.tiles[(city.hwyAxis == 0 ? city.hwyAt : y) * city.w + (city.hwyAxis == 0 ? x : city.hwyAt)] == RAIL || (city.railY0 >= 0 && city.hwyAxis == 1 && k >= city.railY0 && k < city.railY0 + city.railRows);
            p.setColor(0xFF303236);
            hwyRect(c, p, k, k + 1, 0, 3);
            hwyRect(c, p, k, k + 1, 4, 7);
            // (10.29) Where a slip road joins, the edge is open: no shoulder, a broken line instead.
            boolean slipLo = hwySlip(k, -1), slipHi = hwySlip(k, 7);
            // Gravel shoulders.
            p.setColor(0xFF6A665E);
            if (!slipLo) hwyRect(c, p, k, k + 1, -0.35f, 0);
            if (!slipHi) hwyRect(c, p, k, k + 1, 7, 7.35f);
            if (rail) continue;
            if (!junc) {
                p.setColor(0xF0ECECEC);
                if (!slipLo) hwyRect(c, p, k, k + 1, 0.12f, 0.24f);
                else if (k % 2 == 0) hwyRect(c, p, k + 0.1f, k + 0.9f, 0.1f, 0.3f);
                if (!slipHi) hwyRect(c, p, k, k + 1, 6.76f, 6.88f);
                else if (k % 2 == 0) hwyRect(c, p, k + 0.1f, k + 0.9f, 6.7f, 6.9f);
                // The inside edge: yellow next to the barrier.
                p.setColor(0xF0E8C440);
                hwyRect(c, p, k, k + 1, 2.76f, 2.88f);
                hwyRect(c, p, k, k + 1, 4.12f, 4.24f);
                if (k % 3 == 0) {
                    p.setColor(0xE0ECECEC);
                    hwyRect(c, p, k + 0.2f, k + 1.4f, 1.44f, 1.56f);
                    hwyRect(c, p, k + 0.2f, k + 1.4f, 5.44f, 5.56f);
                }
                // The concrete barrier.
                if (city.tiles[(city.hwyAxis == 0 ? city.hwyAt + 3 : k) * city.w + (city.hwyAxis == 0 ? k : city.hwyAt + 3)] == FENCE) {
                    p.setColor(0xFF7A776F);
                    hwyRect(c, p, k, k + 1, 3.2f, 3.8f);
                    p.setColor(0xFFC4C0B6);
                    hwyRect(c, p, k, k + 1, 3.32f, 3.68f);
                    p.setColor(0xFF9A968C);
                    hwyRect(c, p, k, k + 0.06f, 3.32f, 3.68f);
                } else {
                    p.setColor(0xFF303236);
                    hwyRect(c, p, k, k + 1, 3, 4);
                }
            } else {
                p.setColor(0xFF303236);
                hwyRect(c, p, k, k + 1, 3, 4);
            }
        }
        // Stop lines at the lights, and the signs: a green board before each junction, both ways.
        for (int[] ex : city.hwyExits) {
            if (ex.length > 3) {
                // (10.29) An interchange: just the sign, a good way before the way off.
                boolean lo = ex[2] > 0;
                float sk = ex[3] - ex[4] * 12 - 1, sa = lo ? -1.6f : 7.6f;
                p.setColor(0x60000000);
                hwyRect(c, p, sk + 0.15f, sk + 2.15f, sa + 0.15f, sa + 1.25f);
                p.setColor(0xFF1E6A3A);
                hwyRect(c, p, sk, sk + 2, sa, sa + 1.1f);
                p.setColor(0xFFF2F2F2);
                hwyRect(c, p, sk + 0.15f, sk + 1.85f, sa + 0.1f, sa + 0.16f);
                hwyRect(c, p, sk + 0.15f, sk + 1.85f, sa + 0.94f, sa + 1f);
                continue;
            }
            int k0 = ex[0], k1 = ex[0] + ex[1];
            int jk0 = k0, jk1 = k1;
            while (jk0 > 0 && city.junctionAt(city.hwyAxis == 0 ? jk0 - 1 : city.hwyAt + 1, city.hwyAxis == 0 ? city.hwyAt + 1 : jk0 - 1)) jk0--;
            while (jk1 < len - 1 && city.junctionAt(city.hwyAxis == 0 ? jk1 : city.hwyAt + 1, city.hwyAxis == 0 ? city.hwyAt + 1 : jk1)) jk1++;
            p.setColor(0xF0F2F2F2);
            for (int car = 0; car < 2; car++) {
                // Traffic on carriageway 0 comes from the high end (it heads to lower k); keeping left flips it.
                boolean fromHigh = (car == 0);
                if (city.hwyAxis == 1) fromHigh = !fromHigh;
                float a0 = car == 0 ? 0.2f : 4.2f, a1 = car == 0 ? 2.8f : 6.8f;
                float lineK = fromHigh ? jk1 + 0.15f : jk0 - 0.35f;
                hwyRect(c, p, lineK, lineK + 0.2f, a0, a1);
                // The sign, on the verge, a good way back.
                float sk = fromHigh ? jk1 + 9 : jk0 - 11, sa = car == 0 ? -1.6f : 7.6f;
                p.setColor(0x60000000);
                hwyRect(c, p, sk + 0.15f, sk + 2.15f, sa + 0.15f, sa + 1.25f);
                p.setColor(0xFF1E6A3A);
                hwyRect(c, p, sk, sk + 2, sa, sa + 1.1f);
                p.setColor(0xFFF2F2F2);
                hwyRect(c, p, sk + 0.15f, sk + 1.85f, sa + 0.1f, sa + 0.16f);
                hwyRect(c, p, sk + 0.15f, sk + 1.85f, sa + 0.94f, sa + 1f);
                p.setColor(0xF0F2F2F2);
            }
        }
        // The route shield at each end of the highway.
        for (int end = 0; end < 2; end++) {
            float k = end == 0 ? 3 : len - 5, a = end == 0 ? 7.6f : -1.8f;
            float sx = city.hwyAxis == 0 ? k * T : (city.hwyAt + a) * T, sy = city.hwyAxis == 0 ? (city.hwyAt + a) * T : k * T;
            p.setColor(city.country.shieldColor);
            c.drawRect(sx, sy, sx + 26, sy + 14, p);
            if (city.country.id == Country.USA) {
                p.setColor(0xFFB02020);
                c.drawRect(sx, sy, sx + 26, sy + 4, p);
            }
            p.setColor(0xFFF2F2F2);
            p.setTextSize(7);
            p.setTextAlign(Paint.Align.CENTER);
            p.setFakeBoldText(true);
            c.drawText(city.hwyShield, sx + 13, sy + 11.5f, p);
            p.setFakeBoldText(false);
            p.setTextAlign(Paint.Align.LEFT);
        }
    }

    /** (10.30) The army's checkpoints: a hut, sandbags either side and a striped barrier across the road. */
    void drawCheckpoints(Canvas c, Paint p) {
        for (float[] cp : city.checkpoints) {
            c.save();
            c.translate(cp[0], cp[1]);
            c.rotate((float) Math.toDegrees(cp[2]));
            // (Along the road is x, across it y; the road is about 3 tiles wide.)
            float hw = 1.6f * T;
            // Sandbag walls on both verges, either side of the barrier.
            for (int sd = -1; sd <= 1; sd += 2)
                for (int end = -1; end <= 1; end += 2) {
                    float x0 = end < 0 ? -30 : 6, x1 = end < 0 ? -6 : 30;
                    float y0 = sd * (hw + 2), y1 = sd * (hw + 11);
                    p.setColor(0x50000000);
                    c.drawRect(x0 + 2, Math.min(y0, y1) + 2, x1 + 2, Math.max(y0, y1) + 2, p);
                    p.setColor(0xFF6E6248);
                    c.drawRect(x0, Math.min(y0, y1), x1, Math.max(y0, y1), p);
                    p.setColor(0xFFA8986E);
                    for (float bx = x0 + 0.8f; bx < x1 - 1; bx += 6)
                        for (float by = Math.min(y0, y1) + 0.8f; by < Math.max(y0, y1) - 1; by += 4.5f)
                            c.drawRect(bx, by, Math.min(x1 - 0.8f, bx + 5), Math.min(Math.max(y0, y1) - 0.8f, by + 3.6f), p);
                }
            // The guard hut, on one side, behind its sandbags.
            float hy = hw + 14;
            p.setColor(0x60000000);
            c.drawRect(-9, hy + 3, 15, hy + 23, p);
            p.setColor(0xFF59614A);
            c.drawRect(-12, hy, 12, hy + 20, p);
            p.setColor(0xFF424A36);
            c.drawRect(-12, hy, 12, hy + 4, p);
            c.drawRect(-12, hy + 16, 12, hy + 20, p);
            p.setColor(0xFF9AB4C4);
            c.drawRect(-8, hy + 6, -2, hy + 9, p);
            c.drawRect(2, hy + 6, 8, hy + 9, p);
            // A sign on the other side: the army's, white on green.
            float sy = -(hw + 17);
            p.setColor(0xFF2A2A2A);
            c.drawRect(-1, sy, 1, sy + 4, p);
            p.setColor(0xFF2F5A2A);
            c.drawRect(-12, sy - 8, 12, sy, p);
            p.setColor(0xFFF0F0F0);
            c.drawRect(-10, sy - 6, 10, sy - 5, p);
            c.drawRect(-10, sy - 3, 6, sy - 2, p);
            // The barrier: a red and white pole across the road, on its post.
            p.setColor(0xFF2A2A2A);
            c.drawRect(-2.5f, hw - 1, 2.5f, hw + 4, p);
            for (int k = 0; k < 8; k++) {
                p.setColor(k % 2 == 0 ? 0xFFD03028 : 0xFFF0F0F0);
                float a0 = hw - (k + 1) * (2 * hw - 2) / 8f, a1 = hw - k * (2 * hw - 2) / 8f;
                c.drawRect(-1.3f, a0, 1.3f, a1, p);
            }
            // Stop lines either side.
            p.setColor(0xD0ECECEC);
            c.drawRect(-36, -hw + 1, -34.5f, hw - 1, p);
            c.drawRect(34.5f, -hw + 1, 36, hw - 1, p);
            c.restore();
        }
    }

    /** A slip road beside the highway at k, a (across it): its edge is open there. */
    boolean hwySlip(int k, int a) {
        int x = city.hwyAxis == 0 ? k : city.hwyAt + a, y = city.hwyAxis == 0 ? city.hwyAt + a : k;
        return city.cfg.civic >= 13 && city.curveTile != null && x >= 0 && y >= 0 && x < city.w && y < city.h && city.curveTile[y * city.w + x];
    }

    /** (12.4) A well-mixed number from a few others: the same every time for the same ones. */
    static int scramble(int a, int b, int c) {
        int h = a * 0x9E3779B1 ^ b * 0x85EBCA6B ^ c * 0xC2B2AE35;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        h *= 0x297A2D39;
        return h ^ (h >>> 15);
    }

    /**
     * (12.4) Here and there out in the middle of a street: a drift of sand blown across it, or grass and weeds come up
     * through the cracks. Just for looks (cars and people go over them). How many, and how much sand to grass,
     * differs from one city to the next; where they are is the same every time for the same city.
     */
    void drawRoadPatches(Canvas c, Paint p) {
        int seed = (int) city.cfg.seed;
        float rate = 0.008f + ((scramble(seed, 11, 3) >>> 8) & 1023) / 1023f * 0.022f;
        float sandShare = ((scramble(seed, 5, 17) >>> 8) & 1023) / 1023f;
        for (Street st : city.streets) {
            if (city.offRegion((st.x0 - 2) * T, (st.y0 - 2) * T, (st.x1 + st.width + 2) * T, (st.y1 + st.width + 2) * T)) continue;
            int n = st.vertical ? st.y1 - st.y0 : st.x1 - st.x0;
            int dir = st.vertical ? 1 : 2;
            for (int k = 0; k < n; k++) {
                int x = st.vertical ? st.x0 : st.x0 + k, y = st.vertical ? st.y0 + k : st.y0;
                int hsh = scramble(seed, x * 7919 + 13, y * 104729 + 7);
                if (((hsh >>> 4) & 0xFFFF) / 65535f >= rate) continue;
                int mx = st.vertical ? st.x0 + st.width / 2 : x, my = st.vertical ? y : st.y0 + st.width / 2;
                if (mx < 0 || my < 0 || mx >= city.w || my >= city.h || city.roadDir[my * city.w + mx] != dir || !city.paved(mx, my)) continue;
                boolean whole = true;
                for (int q = 0; q < st.width && whole; q++) {
                    int qx = st.vertical ? st.x0 + q : x, qy = st.vertical ? y : st.y0 + q;
                    if (qx < 0 || qy < 0 || qx >= city.w || qy >= city.h || city.tiles[qy * city.w + qx] != ROAD) whole = false;
                }
                if (!whole) continue;
                float span = st.width * T;
                // Near the middle of the road: the crown, where the wheels don't wear it away.
                float across = (st.vertical ? st.x0 : st.y0) * T + span * (0.5f + (((hsh >>> 20) & 255) / 255f - 0.5f) * 0.3f);
                float along = (st.vertical ? y : x) * T + T * (((hsh >>> 12) & 255) / 255f);
                float cx = st.vertical ? across : along, cy = st.vertical ? along : across;
                boolean sand = ((hsh >>> 28) & 15) / 15f < sandShare;
                int blobs = 3 + ((hsh >>> 2) & 3);
                float size = 3 + ((hsh >>> 24) & 7) * 0.7f;
                for (int b = 0; b < blobs; b++) {
                    int hb = scramble(hsh, b, 31);
                    float ox = ((hb & 255) / 255f - 0.5f) * size * 2.2f, oy = (((hb >>> 8) & 255) / 255f - 0.5f) * size * 1.4f;
                    if (st.vertical) {
                        float t = ox;
                        ox = oy;
                        oy = t;
                    }
                    float rx = size * (0.5f + ((hb >>> 16) & 255) / 255f * 0.6f), ry = rx * (0.55f + ((hb >>> 24) & 127) / 127f * 0.35f);
                    if (sand) p.setColor(b == 0 ? 0x90C9B387 : 0x70D8C49A);
                    else p.setColor(b % 2 == 0 ? 0xA0607A3A : 0x907A9446);
                    c.drawOval(new android.graphics.RectF(cx + ox - rx, cy + oy - ry, cx + ox + rx, cy + oy + ry), p);
                }
                if (!sand) {
                    // Tufts sticking up out of it.
                    p.setColor(0xC04E6A2C);
                    for (int t = 0; t < 4; t++) {
                        int ht = scramble(hsh, t, 97);
                        float tx = cx + ((ht & 255) / 255f - 0.5f) * size * 1.8f, ty = cy + (((ht >>> 8) & 255) / 255f - 0.5f) * size * 1.4f;
                        c.drawRect(tx - 0.5f, ty - 1.4f, tx + 0.5f, ty + 1.4f, p);
                    }
                }
            }
        }
    }

    void drawRoadMarkings(Canvas c, Paint p) {
        p.setStrokeWidth(1.2f);
        for (Street st : city.streets) {
            if (city.offRegion((st.x0 - 2) * T, (st.y0 - 2) * T, (st.x1 + st.width + 2) * T, (st.y1 + st.width + 2) * T)) continue;
            int n = st.vertical ? st.y1 - st.y0 : st.x1 - st.x0;
            int dir = st.vertical ? 1 : 2;
            for (int k = 0; k < n; k++) {
                int x = st.vertical ? st.x0 : st.x0 + k, y = st.vertical ? st.y0 + k : st.y0;
                // Only between junctions.
                int mx = st.vertical ? st.x0 + st.width / 2 : x, my = st.vertical ? y : st.y0 + st.width / 2;
                if (mx < 0 || my < 0 || mx >= city.w || my >= city.h || city.roadDir[my * city.w + mx] != dir || !city.paved(mx, my)) continue;
                // (10.28) Only where the street is still road all the way across: a stretch that went back to
                // grass, or was cut short, gets no lines painted on the grass.
                boolean whole = true;
                for (int q = 0; q < st.width && whole; q++) {
                    int qx = st.vertical ? st.x0 + q : x, qy = st.vertical ? y : st.y0 + q;
                    if (qx < 0 || qy < 0 || qx >= city.w || qy >= city.h || (city.tiles[qy * city.w + qx] != ROAD && city.tiles[qy * city.w + qx] != CAR)) whole = false;
                }
                if (!whole) continue;
                float base = (st.vertical ? st.x0 : st.y0) * T, span = st.width * T;
                // Wear: darker tracks where the wheels run, two to each lane.
                int lanes = st.width >= 7 ? 6 : st.width >= 5 ? 4 : 2;
                p.setColor(0x14000000);
                for (int ln = 0; ln < lanes; ln++) {
                    float lc = base + span * (ln + 0.5f) / lanes;
                    for (int side = -1; side <= 1; side += 2) {
                        float wc = lc + side * 3.2f;
                        if (st.vertical) c.drawRect(wc - 1.2f, y * T, wc + 1.2f, y * T + T, p);
                        else c.drawRect(x * T, wc - 1.2f, x * T + T, wc + 1.2f, p);
                    }
                }
                // Solid white edge lines along the kerbs (not across a side road or a driveway).
                p.setColor(0xB8E4E4E0);
                for (int side = 0; side < 2; side++) {
                    int ox = st.vertical ? (side == 0 ? st.x0 - 1 : st.x1) : x, oy = st.vertical ? y : (side == 0 ? st.y0 - 1 : st.y1);
                    if (city.paved(ox, oy) || city.tiles[Math.max(0, Math.min(city.h - 1, oy)) * city.w + Math.max(0, Math.min(city.w - 1, ox))] == DIRT) continue;
                    float e = side == 0 ? base + 1.6f : base + span - 2.4f;
                    if (st.vertical) c.drawRect(e, y * T, e + 0.8f, y * T + T, p);
                    else c.drawRect(x * T, e, x * T + T, e + 0.8f, p);
                }
                p.setStrokeWidth(1.5f);
                if (st.width >= 5) {
                    // ((11.1) Two lanes each way (three on a boulevard), evenly split, dashed between them, and the
                    // centre line down the middle.)
                    int per = st.width >= 7 ? 3 : 2;
                    float mid = base + span / 2, lw = span / 2 / per;
                    p.setColor(0xCCE8E8E8);
                    for (int ln = 1; ln < per; ln++)
                        for (int sd = -1; sd <= 1; sd += 2) {
                            float l = mid + sd * ln * lw;
                            if (st.vertical) c.drawLine(l, y * T + 4, l, y * T + 10, p);
                            else c.drawLine(x * T + 4, l, x * T + 10, l, p);
                        }
                    if (!city.country.ringRoad.equals(st.name)) {
                        p.setColor(city.country.centreLine);
                        p.setStrokeWidth(1f);
                        for (int sd = -1; sd <= 1; sd += 2) {
                            float l = mid + sd * 1.1f;
                            if (st.vertical) c.drawLine(l, y * T, l, y * T + T, p);
                            else c.drawLine(x * T, l, x * T + T, l, p);
                        }
                    }
                } else if (st.width == 3 && !city.country.ringRoad.equals(st.name)) {
                    p.setColor(city.country.centreLine);
                    if (st.vertical) c.drawLine((st.x0 + 1.5f) * T, y * T + 2, (st.x0 + 1.5f) * T, y * T + 12, p);
                    else c.drawLine(x * T + 2, (st.y0 + 1.5f) * T, x * T + 12, (st.y0 + 1.5f) * T, p);
                }
                p.setStrokeWidth(1.2f);
            }
            if (st.width >= 5) turnLanes(c, p, st, n);
            stopLines(c, p, st, n);
            // Zebra crossings where the street meets a junction.
            p.setColor(0xCCE8E8E8);
            for (int k = 1; k < n; k++) {
                int ax = st.vertical ? st.x0 : st.x0 + k - 1, ay = st.vertical ? st.y0 + k - 1 : st.y0;
                int bx = st.vertical ? st.x0 : st.x0 + k, by = st.vertical ? st.y0 + k : st.y0;
                boolean ja = city.junctionAt(ax, ay), jb = city.junctionAt(bx, by);
                if (ja == jb || bendAt(ja ? ax : bx, ja ? ay : by)) continue;
                // The crossing sits on the non-junction tile, against the junction.
                int cx = ja ? bx : ax, cy = ja ? by : ay;
                if (!city.paved(cx, cy)) continue;
                for (int o = 0; o < st.width; o++) {
                    int tx = st.vertical ? st.x0 + o : cx, ty = st.vertical ? cy : st.y0 + o;
                    if (!city.paved(tx, ty)) continue;
                    // Bold zebra bars, as wide as the gaps between them.
                    for (float q = 1.5f; q + 3 <= T; q += 6) {
                        if (st.vertical) {
                            float yy = ja ? ty * T + 1.5f : ty * T + T - 11.5f;
                            c.drawRect(tx * T + q, yy, tx * T + q + 3.2f, yy + 10, p);
                        } else {
                            float xx = ja ? tx * T + 1.5f : tx * T + T - 11.5f;
                            c.drawRect(xx, ty * T + q, xx + 10, ty * T + q + 3.2f, p);
                        }
                    }
                }
            }
        }
    }

    void drawParkingLines(Canvas c, Paint p) {
        p.setColor(0xAAE0E0E0);
        p.setStrokeWidth(1f);
        for (int y = Math.max(0, city.gy0()); y < Math.min(city.h, gy1()); y++)
            for (int x = Math.max(0, gx0()); x < Math.min(city.w, gx1()); x++) {
                byte t = city.tiles[y * city.w + x];
                if ((t == LOT || (t == CAR && isLotCar(x, y))) && y > 0 && y < city.h - 1) {
                    boolean row = city.tiles[(y - 1) * city.w + x] == LOT || city.tiles[(y + 1) * city.w + x] == LOT;
                    if (row && (t == CAR || rowHasCars(x, y))) c.drawLine(x * T, y * T + 2, x * T, y * T + T - 2, p);
                }
            }
    }

    boolean rowHasCars(int x, int y) {
        return (x > 0 && city.tiles[y * city.w + x - 1] == CAR) || (x < city.w - 1 && city.tiles[y * city.w + x + 1] == CAR);
    }

    void drawBuilding(Canvas c, Paint p, int[] b) {
        if (b.length > 11 && b[10] >= 0) {
            Landmarks.Def ld = Landmarks.get(b[10]);
            if (ld != null) {
                p.setStyle(Paint.Style.FILL);
                ld.roof(city, c, p, b[11], b[0] * T, b[1] * T, (b[0] + b[2]) * T, (b[1] + b[3]) * T, new Random(b[5]));
                p.setStyle(Paint.Style.FILL);
                return;
            }
        }
        float x0 = b[0] * T, y0 = b[1] * T, x1 = (b[0] + b[2]) * T, y1 = (b[1] + b[3]) * T;
        int roof = b[4], kind = b[6];
        Random r = new Random(b[5]);
        float bw = x1 - x0, bh = y1 - y0;
        int bi = city.buildingAt.get(b[1] * city.w + b[0]);
        Building own = bi >= 0 && bi < city.buildings.size() ? city.buildings.get(bi) : null;
        if (own != null && own.kind == JAIL && own.name != null && own.name.equals("Airport Terminal")) {
            roofAirportTerminal(c, p, x0, y0, x1, y1, bw);
            return;
        }
        if (own != null && own.kind == JAIL && city.prison != null && own.name != null && own.name.equals("Gatehouse")) {
            roofGatehouse(c, p, x0, y0, x1, y1);
            return;
        }
        if (own != null && own.kind == JAIL && city.prison != null && own.name != null && (own.name.startsWith("Cell Block") || own.name.equals("Prison Admin"))) {
            roofCellBlock(c, p, x0, y0, x1, y1, bw, own);
            return;
        }
        if (own != null && own.name != null && (own.name.equals(city.rangerStationName) || city.outpostNames.contains(own.name))) {
            roofRangerStation(c, p, x0, y0, x1, y1, bw);
            return;
        }
        if (b.length > 9 && b[9] >= 0 && Roofs.drawVariant(city, c, p, b, Variants.get(b[9]))) return;
        if (kind == HOUSE) {
            roofHouse(c, p, b, x0, y0, x1, y1, roof, r, bw, bh);
            return;
        }
        if (kind == STATION && roof == FED_ROOF) {
            roofFedOffice(c, p, x0, y0, x1, y1, bw, bh);
            return;
        }
        if (kind == STATION) {
            roofStation(c, p, x0, y0, x1, y1, bw, bh);
            return;
        }
        if (kind == FIRE_STATION || kind == MARKET || kind == SCHOOL || kind == KIOSK) {
            roofCivic(c, p, x0, y0, x1, y1, roof, kind, bw);
            return;
        }
        if (kind == CHURCH || kind == CRYPT) {
            roofChurch(c, p, x0, y0, x1, y1, roof, bw, bh);
            return;
        }
        if (kind == TRAIN_STATION) {
            roofTrainStation(c, p, x0, y0, x1, y1, roof);
            return;
        }
        if (kind == MALL) {
            roofMall(c, p, x0, y0, x1, y1, roof, bw, bh);
            return;
        }
        if (kind == STADIUM) {
            roofStadium(c, p, x0, y0, x1, y1, bw, bh);
            return;
        }
        if (kind == POWER) {
            roofPower(c, p, x0, y0, x1, y1, roof, bw, bh);
            return;
        }
        if (kind == BARN) {
            roofBarn(c, p, x0, y0, x1, y1, roof, bw, bh);
            return;
        }
        if (kind == SILO) {
            roofSilo(c, p, x0, y0, x1, y1, roof, bw);
            return;
        }
        if (kind == APARTMENT) {
            roofApartment(c, p, x0, y0, x1, y1, roof, r, bw);
            return;
        }
        if (kind == GARAGE) {
            roofGarage(c, p, x0, y0, x1, y1, roof, r);
            return;
        }
        if (kind == PHARMACY) {
            roofPharmacy(c, p, x0, y0, x1, y1, roof, bw, bh);
            return;
        }
        if (kind == SPIRE) {
            roofSpire(c, p, x0, y0, x1, y1);
            return;
        }
        if (kind == SHOP) {
            roofShop(c, p, b, x0, y0, x1, y1, roof, bw);
            return;
        }
        if (kind == HOSPITAL) {
            roofHospital(c, p, x0, y0, x1, y1, roof, bw, bh);
            return;
        }
        if (kind == TOWER) {
            p.setColor(darken(roof, 0.7f));
            c.drawRect(x0, y0, x1, y1, p);
            p.setColor(roof);
            c.drawRect(x0 + 2, y0 + 2, x1 - 2, y1 - 2, p);
            p.setColor(0xFF2C3220);
            c.drawRect(x0 + 6, y0 + 6, x1 - 6, y1 - 6, p);
            return;
        }
        if (kind == WAREHOUSE || kind == BARRACKS) {
            roofWarehouse(c, p, x0, y0, x1, y1, roof, bw, bh);
            return;
        }
        p.setColor(darken(roof, 0.7f));
        c.drawRect(x0, y0, x1, y1, p);
        p.setColor(roof);
        c.drawRect(x0 + 2, y0 + 2, x1 - 2, y1 - 2, p);
        p.setColor(lighten(roof, 0.08f));
        c.drawRect(x0 + 5, y0 + 5, x1 - 5, y1 - 5, p);

        // Roof details on the lower half: solar panels, a rooftop garden or a row of skylights.
        int deco = bw >= 40 && bh >= 40 ? new Random(b[5] * 7L + 3).nextInt(5) : 4;
        float dx0 = x0 + 7, dy0 = (y0 + y1) / 2 + 2, dx1 = Math.min(x1 - 7, x0 + 7 + 70), dy1 = Math.min(y1 - 7, dy0 + 44);
        if (deco == 0) {
            drawSolar(c, p, dx0, dy0, dx1, dy1);
        } else if (deco == 1) {
            p.setColor(0xFF6B5238);
            c.drawRect(dx0, dy0, dx1, dy1, p);
            p.setColor(0xFF4F8A3A);
            c.drawRect(dx0 + 1.5f, dy0 + 1.5f, dx1 - 1.5f, dy1 - 1.5f, p);
            Random g = new Random(b[5]);
            for (int k = 0; k < (int) ((dx1 - dx0) * (dy1 - dy0) / 60); k++) {
                float gx = dx0 + 3 + g.nextFloat() * Math.max(1, dx1 - dx0 - 6), gy = dy0 + 3 + g.nextFloat() * Math.max(1, dy1 - dy0 - 6);
                p.setColor(g.nextInt(4) == 0 ? 0xFFE07AA0 : g.nextBoolean() ? 0xFF3B742D : 0xFF6AA84F);
                c.drawCircle(gx, gy, 1.5f + g.nextFloat() * 1.5f, p);
            }
            p.setColor(0xFFB8A888);
            c.drawRect(dx0 + 2, (dy0 + dy1) / 2 - 1, dx1 - 2, (dy0 + dy1) / 2 + 1, p);
        } else if (deco == 2) {
            for (float sx = dx0 + 2; sx + 8 < dx1; sx += 12) {
                p.setColor(darken(roof, 0.6f));
                c.drawRect(sx, dy0 + 2, sx + 8, dy1 - 2, p);
                p.setColor(0xCC9CC3D9);
                c.drawRect(sx + 1, dy0 + 3, sx + 7, dy1 - 3, p);
                p.setColor(0x66FFFFFF);
                c.drawRect(sx + 1, dy0 + 3, sx + 2.5f, dy1 - 3, p);
            }
        }
        float unitsH = deco < 3 ? bh / 2 : bh;
        int units = 1 + r.nextInt(Math.max(1, (int) (bw * bh / 1500f)) + 1);
        // ((11.1) In a tidy row with their duct, not dotted about at random.)
        Roofs.hvac(c, p, x0 + 3, y0 + 3, x1 - 3, y0 + unitsH - 3, r, units + 1);
        if (bw >= 64 && bh >= 64 && b[7] >= 8 && r.nextFloat() < 0.35f) {
            float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
            p.setColor(0xFF3F4347);
            c.drawCircle(cx, cy, 16, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1.5f);
            p.setColor(0xFFE8D24A);
            c.drawCircle(cx, cy, 13, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFEEEEEE);
            c.drawRect(cx - 6, cy - 7, cx - 3.5f, cy + 7, p);
            c.drawRect(cx + 3.5f, cy - 7, cx + 6, cy + 7, p);
            c.drawRect(cx - 4, cy - 1.2f, cx + 4, cy + 1.2f, p);
        }
    }

    /** ((27.3) A roof of drawBuilding's, as it was drawn there: own != null && own.kind == JAIL && own.name != null && own.name.equals("Airport Terminal").) */
    private void roofAirportTerminal(Canvas c, Paint p, float x0, float y0, float x1, float y1, float bw) {
        // ((15.10) The terminal: a long pale roof, a glass front along the apron side, and its name.)
        p.setColor(0xFFC8CCD0);
        c.drawRect(x0, y0, x1, y1, p);
        p.setColor(0xFFE2E6EA);
        c.drawRect(x0 + 1.5f, y0 + 1.5f, x1 - 1.5f, y1 - 1.5f, p);
        p.setColor(0xFF6FA0C8);
        c.drawRect(x0 + 2, y0 + 2, x1 - 2, y0 + 6, p);
        city.sign("AIRPORT", (x0 + x1) / 2, (y0 + y1) / 2 + 2, Math.min(10f, bw / 6f), 0xFF2A3A4A);
    }

    /** ((27.3) A roof of drawBuilding's, as it was drawn there: own != null && own.kind == JAIL && city.prison != null && own.name != null && own.name.equals("Gatehouse").) */
    private void roofGatehouse(Canvas c, Paint p, float x0, float y0, float x1, float y1) {
        // ((15.6) A gatehouse: a squat concrete box with a dark slit window round the top and a light on the roof.)
        p.setColor(0xFF6E6C66);
        c.drawRect(x0, y0, x1, y1, p);
        p.setColor(0xFF8E8C86);
        c.drawRect(x0 + 1.5f, y0 + 1.5f, x1 - 1.5f, y1 - 1.5f, p);
        p.setColor(0xFF2A2C30);
        c.drawRect(x0 + 3, y0 + 3, x1 - 3, y0 + 5, p);
        p.setColor(0xFFFFE08A);
        c.drawCircle((x0 + x1) / 2, (y0 + y1) / 2 + 2, 1.6f, p);
    }

    /** ((27.3) A roof of drawBuilding's, as it was drawn there: own != null && own.kind == JAIL && city.prison != null && own.name != null && (own.name.startsWith("Cell Block") || own.name.equals("Prison Admin")).) */
    private void roofCellBlock(Canvas c, Paint p, float x0, float y0, float x1, float y1, float bw, Building own) {
        // ((15.6) The island prison's blocks: bare concrete roofs, rows of vents, the block's name painted on.)
        p.setColor(0xFF8E8C86);
        c.drawRect(x0, y0, x1, y1, p);
        p.setColor(0xFFA6A49C);
        c.drawRect(x0 + 1.5f, y0 + 1.5f, x1 - 1.5f, y1 - 1.5f, p);
        p.setColor(0xFF6E6C66);
        for (float vx = x0 + 6; vx < x1 - 6; vx += 10) c.drawRect(vx, y0 + 4, vx + 4, y0 + 7, p);
        String sign = own.name.toUpperCase();
        city.sign(sign, (x0 + x1) / 2, (y0 + y1) / 2 + 2, Math.min(9f, bw / (sign.length() * 0.75f)), 0xFFF2F0E8);
    }

    /** ((27.3) A roof of drawBuilding's, as it was drawn there: own != null && own.name != null && (own.name.equals(city.rangerStationName) || city.outpostNames.contains(own.name)).) */
    private void roofRangerStation(Canvas c, Paint p, float x0, float y0, float x1, float y1, float bw) {
        // The ranger station: a green metal roof, a brown trim and the sign on top.
        p.setColor(0xFF3A4A30);
        c.drawRect(x0, y0, x1, y1, p);
        p.setColor(0xFF4E6240);
        c.drawRect(x0 + 1.5f, y0 + 1.5f, x1 - 1.5f, (y0 + y1) / 2, p);
        p.setColor(0xFF465A3A);
        c.drawRect(x0 + 1.5f, (y0 + y1) / 2, x1 - 1.5f, y1 - 1.5f, p);
        p.setColor(0xFF6E4A2A);
        c.drawRect(x0, y1 - 3, x1, y1, p);
        String sign = city.country.sign("RANGERS");
        city.sign(sign, (x0 + x1) / 2, (y0 + y1) / 2, Math.min(9f, bw / (sign.length() * 0.72f)), 0xFFE8D8A8);
    }

    /** ((27.3) A roof of drawBuilding's, as it was drawn there: kind == HOUSE.) */
    private void roofHouse(Canvas c, Paint p, int[] b, float x0, float y0, float x1, float y1, int roof, Random r, float bw, float bh) {
        // Pitched roof: two slopes meeting at a ridge along the long side.
        p.setColor(darken(roof, 0.75f));
        c.drawRect(x0, y0, x1, y1, p);
        if (bw >= bh) {
            p.setColor(lighten(roof, 0.12f));
            c.drawRect(x0 + 1, y0 + 1, x1 - 1, (y0 + y1) / 2, p);
            p.setColor(roof);
            c.drawRect(x0 + 1, (y0 + y1) / 2, x1 - 1, y1 - 1, p);
            p.setColor(darken(roof, 0.6f));
            c.drawRect(x0 + 1, (y0 + y1) / 2 - 0.6f, x1 - 1, (y0 + y1) / 2 + 0.6f, p);
        } else {
            p.setColor(lighten(roof, 0.12f));
            c.drawRect(x0 + 1, y0 + 1, (x0 + x1) / 2, y1 - 1, p);
            p.setColor(roof);
            c.drawRect((x0 + x1) / 2, y0 + 1, x1 - 1, y1 - 1, p);
            p.setColor(darken(roof, 0.6f));
            c.drawRect((x0 + x1) / 2 - 0.6f, y0 + 1, (x0 + x1) / 2 + 0.6f, y1 - 1, p);
        }
        if (r.nextBoolean()) {
            p.setColor(0xFF6A5A50);
            c.drawRect(x0 + bw * 0.7f, y0 + 3, x0 + bw * 0.7f + 4, y0 + 7, p);
        }
        // Some houses have solar panels on the sunny slope.
        if (new Random(b[5] * 7L + 3).nextInt(4) == 0) {
            float px0 = x0 + 4, py0 = y0 + 3, px1, py1;
            if (bw >= bh) {
                px1 = x0 + bw * 0.55f;
                py1 = (y0 + y1) / 2 - 1.5f;
            } else {
                px1 = (x0 + x1) / 2 - 1.5f;
                py1 = y0 + bh * 0.55f;
            }
            drawSolar(c, p, px0, py0, px1, py1);
        }
    }

    /** ((27.3) A roof of drawBuilding's, as it was drawn there: kind == STATION && roof == FED_ROOF.) */
    private void roofFedOffice(Canvas c, Paint p, float x0, float y0, float x1, float y1, float bw, float bh) {
        // ((26.5) The FBI field office, nothing like a precinct from above: a charcoal roof inside a pale parapet,
        // a long glass atrium across it, a mast and dishes in one corner, and the letters painted big in gold.)
        p.setColor(0xFF7A828E);
        c.drawRect(x0, y0, x1, y1, p);
        p.setColor(0xFF2A2E35);
        c.drawRect(x0 + 2, y0 + 2, x1 - 2, y1 - 2, p);
        boolean wide = bw >= bh;
        // (Along the long side's edge, so the letters have the rest.)
        float gx0 = wide ? x0 + 4 : x1 - 4 - Math.min(9, bw * 0.18f), gx1 = x1 - 4;
        float gy0 = wide ? y1 - 4 - Math.min(9, bh * 0.18f) : y0 + 4, gy1 = y1 - 4;
        p.setColor(0xFF35577A);
        c.drawRect(gx0, gy0, gx1, gy1, p);
        p.setColor(0xFF6E9CC4);
        if (wide) for (float gx = gx0 + 4; gx < gx1; gx += 6) c.drawRect(gx, gy0, gx + 0.8f, gy1, p);
        else for (float gy = gy0 + 4; gy < gy1; gy += 6) c.drawRect(gx0, gy, gx1, gy + 0.8f, p);
        // The mast and two dishes, top left.
        float dx = x0 + Math.min(10, bw * 0.15f), dy = y0 + Math.min(10, bh * 0.15f);
        p.setColor(0xFF9AA2AC);
        c.drawRect(dx - 1, dy - 1, dx + 1, dy + 9, p);
        p.setColor(0xFFE8EBEF);
        c.drawCircle(dx + 7, dy + 2, 3.2f, p);
        c.drawCircle(dx + 5, dy + 9, 2.4f, p);
        p.setColor(0xFF5A626C);
        c.drawCircle(dx + 7, dy + 2, 0.9f, p);
        c.drawCircle(dx + 5, dy + 9, 0.7f, p);
        // The red warning light on the mast.
        p.setColor(0xFFE0402E);
        c.drawCircle(dx, dy - 1.5f, 1.2f, p);
        String word = Dispatch.federal(city.country).equals("FBI") ? "FBI" : "POLICE";
        float roomW = (wide ? x1 : gx0) - x0 - 6, roomH = (wide ? gy0 : y1) - y0 - 6;
        float size = Math.min(Math.min(word.length() > 3 ? 14f : 26f, roomH * 0.6f), roomW / (word.length() * 0.72f + 0.3f));
        float tx = (x0 + (wide ? x1 : gx0)) / 2, ty = (y0 + (wide ? gy0 : y1)) / 2 + size * 0.36f;
        city.label(word, tx, ty, size, 0xFFE8C547);
    }

    /** ((27.3) A roof of drawBuilding's, as it was drawn there: kind == STATION.) */
    private void roofStation(Canvas c, Paint p, float x0, float y0, float x1, float y1, float bw, float bh) {
        // ((14.9) Unmistakably the police from above: a deep navy roof, a band of blue and white checks round
        // the parapet, a big gold badge painted on it, and the blue lamp over the door. It used to be a plain blue
        // roof much like a shop's.)
        p.setColor(0xFFE8EAEE);
        c.drawRect(x0, y0, x1, y1, p);
        float band = Math.max(3, Math.min(5, Math.min(bw, bh) / 12));
        int cols = Math.max(4, Math.round(bw / band)), rows = Math.max(4, Math.round(bh / band));
        float cw = bw / cols, ch = bh / rows;
        for (int i = 0; i < cols; i++) {
            p.setColor(i % 2 == 0 ? 0xFF1E4FA8 : 0xFFF4F6F8);
            c.drawRect(x0 + i * cw, y0, x0 + (i + 1) * cw, y0 + band, p);
            p.setColor(i % 2 == 1 ? 0xFF1E4FA8 : 0xFFF4F6F8);
            c.drawRect(x0 + i * cw, y1 - band, x0 + (i + 1) * cw, y1, p);
        }
        for (int j = 0; j < rows; j++) {
            p.setColor(j % 2 == 0 ? 0xFF1E4FA8 : 0xFFF4F6F8);
            c.drawRect(x0, y0 + j * ch, x0 + band, y0 + (j + 1) * ch, p);
            p.setColor(j % 2 == 1 ? 0xFF1E4FA8 : 0xFFF4F6F8);
            c.drawRect(x1 - band, y0 + j * ch, x1, y0 + (j + 1) * ch, p);
        }
        p.setColor(0xFF1A2B4E);
        c.drawRect(x0 + band, y0 + band, x1 - band, y1 - band, p);
        p.setColor(0xFF22365F);
        c.drawRect(x0 + band + 3, y0 + band + 3, x1 - band - 3, y1 - band - 3, p);
        // The badge: a gold star in a ring.
        float bx = (x0 + x1) / 2, by = (y0 + y1) / 2, br = Math.min(16, Math.min(bw, bh) * 0.22f);
        if (bh > 34) by -= br * 0.55f;
        p.setColor(0xFFE8C547);
        c.drawCircle(bx, by, br, p);
        p.setColor(0xFF22365F);
        c.drawCircle(bx, by, br * 0.8f, p);
        android.graphics.Path star = new android.graphics.Path();
        for (int k = 0; k < 12; k++) {
            float a = (float) (-Math.PI / 2 + k * Math.PI / 6), rr = k % 2 == 0 ? br * 0.72f : br * 0.36f;
            if (k == 0) star.moveTo(bx + (float) Math.cos(a) * rr, by + (float) Math.sin(a) * rr);
            else star.lineTo(bx + (float) Math.cos(a) * rr, by + (float) Math.sin(a) * rr);
        }
        star.close();
        p.setColor(0xFFE8C547);
        c.drawPath(star, p);
        float signY = bh > 34 ? by + br + Math.min(10f, bw / 5.2f) * 0.9f : (y0 + y1) / 2;
        city.sign(city.country.sign("POLICE"), bx, signY, Math.min(10f, bw / 5.2f), 0xFFFFFFFF);
        // The blue lamp over the door (on the side facing down the map).
        p.setColor(0x553D8BFF);
        c.drawCircle(bx, y1 + 1, 6, p);
        p.setColor(0xFF3D8BFF);
        c.drawCircle(bx, y1 + 1, 2.6f, p);
    }

    /** ((27.3) A roof of drawBuilding's, as it was drawn there: kind == FIRE_STATION || kind == MARKET || kind == SCHOOL || kind == KIOSK.) */
    private void roofCivic(Canvas c, Paint p, float x0, float y0, float x1, float y1, int roof, int kind, float bw) {
        p.setColor(darken(roof, 0.7f));
        c.drawRect(x0, y0, x1, y1, p);
        p.setColor(roof);
        c.drawRect(x0 + 2, y0 + 2, x1 - 2, y1 - 2, p);
        if (kind == MARKET || kind == SCHOOL) {
            p.setColor(0xCC9CC3D9);
            for (float sx = x0 + 10; sx < x1 - 10; sx += 18)
                for (float sy = y0 + 10; sy < y1 - 10; sy += 18) c.drawRect(sx - 3, sy - 2, sx + 3, sy + 2, p);
        }
        // ((10.62) No sign on the fire stations: there's no fire service in the game any more.)
        if (kind == FIRE_STATION) return;
        String label = city.country.sign(kind == MARKET ? "MARKET" : kind == SCHOOL ? "SCHOOL" : "GAS");
        p.setColor(kind == FIRE_STATION ? 0xFFFFFFFF : kind == KIOSK ? 0xFFD83A3A : 0xFF3A3A3A);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(Math.min(10f, bw / (label.length() * 0.75f)));
        city.sign(label, (x0 + x1) / 2, (y0 + y1) / 2, Math.min(10f, bw / (label.length() * 0.75f)),
                kind == FIRE_STATION ? 0xFFFFFFFF : kind == KIOSK ? 0xFFD83A3A : 0xFF3A3A3A);
    }

    /** ((27.3) A roof of drawBuilding's, as it was drawn there: kind == CHURCH || kind == CRYPT.) */
    private void roofChurch(Canvas c, Paint p, float x0, float y0, float x1, float y1, int roof, float bw, float bh) {
        // Pitched roof with a cross.
        p.setColor(darken(roof, 0.7f));
        c.drawRect(x0, y0, x1, y1, p);
        boolean alongX = bw >= bh;
        p.setColor(lighten(roof, 0.1f));
        if (alongX) c.drawRect(x0 + 1, y0 + 1, x1 - 1, (y0 + y1) / 2, p);
        else c.drawRect(x0 + 1, y0 + 1, (x0 + x1) / 2, y1 - 1, p);
        p.setColor(roof);
        if (alongX) c.drawRect(x0 + 1, (y0 + y1) / 2, x1 - 1, y1 - 1, p);
        else c.drawRect((x0 + x1) / 2, y0 + 1, x1 - 1, y1 - 1, p);
        p.setColor(0xFFE8D24A);
        float cxx = (x0 + x1) / 2, cyy = (y0 + y1) / 2;
        c.drawRect(cxx - 1, cyy - 5, cxx + 1, cyy + 5, p);
        c.drawRect(cxx - 3.5f, cyy - 2.5f, cxx + 3.5f, cyy - 0.8f, p);
    }

    /** ((27.3) A roof of drawBuilding's, as it was drawn there: kind == TRAIN_STATION.) */
    private void roofTrainStation(Canvas c, Paint p, float x0, float y0, float x1, float y1, int roof) {
        // Long pitched roof with a clock.
        p.setColor(darken(roof, 0.75f));
        c.drawRect(x0, y0, x1, y1, p);
        p.setColor(lighten(roof, 0.1f));
        c.drawRect(x0 + 1, y0 + 1, x1 - 1, (y0 + y1) / 2, p);
        p.setColor(roof);
        c.drawRect(x0 + 1, (y0 + y1) / 2, x1 - 1, y1 - 1, p);
        p.setColor(0xFFF2EEE0);
        c.drawCircle((x0 + x1) / 2, (y0 + y1) / 2, 4, p);
        p.setColor(0xFF2A2A2A);
        p.setStrokeWidth(0.8f);
        c.drawLine((x0 + x1) / 2, (y0 + y1) / 2, (x0 + x1) / 2, (y0 + y1) / 2 - 3, p);
        c.drawLine((x0 + x1) / 2, (y0 + y1) / 2, (x0 + x1) / 2 + 2, (y0 + y1) / 2, p);
    }

    /** ((27.3) A roof of drawBuilding's, as it was drawn there: kind == MALL.) */
    private void roofMall(Canvas c, Paint p, float x0, float y0, float x1, float y1, int roof, float bw, float bh) {
        // Big flat roof with a glass atrium down the middle and plant rooms.
        p.setColor(darken(roof, 0.7f));
        c.drawRect(x0, y0, x1, y1, p);
        p.setColor(roof);
        c.drawRect(x0 + 2, y0 + 2, x1 - 2, y1 - 2, p);
        boolean along = bw >= bh;
        float ax0 = along ? x0 + 8 : (x0 + x1) / 2 - 7, ay0 = along ? (y0 + y1) / 2 - 7 : y0 + 8;
        float ax1 = along ? x1 - 8 : (x0 + x1) / 2 + 7, ay1 = along ? (y0 + y1) / 2 + 7 : y1 - 8;
        p.setColor(0xFF6E8CA0);
        c.drawRect(ax0, ay0, ax1, ay1, p);
        p.setColor(0xFF9CC3D9);
        c.drawRect(ax0 + 1, ay0 + 1, ax1 - 1, ay1 - 1, p);
        p.setColor(0xFF6E8CA0);
        p.setStrokeWidth(0.8f);
        if (along) for (float gx = ax0 + 6; gx < ax1; gx += 6) c.drawLine(gx, ay0, gx, ay1, p);
        else for (float gy = ay0 + 6; gy < ay1; gy += 6) c.drawLine(ax0, gy, ax1, gy, p);
        c.drawLine(along ? ax0 : (ax0 + ax1) / 2, along ? (ay0 + ay1) / 2 : ay0, along ? ax1 : (ax0 + ax1) / 2,
                along ? (ay0 + ay1) / 2 : ay1, p);
        p.setColor(0x66FFFFFF);
        c.drawRect(ax0 + 1, ay0 + 1, along ? ax1 - 1 : ax0 + 3, along ? ay0 + 3 : ay1 - 1, p);
        p.setColor(0xFFA7ABAF);
        c.drawRect(x0 + 6, y0 + 6, x0 + 16, y0 + 13, p);
        c.drawRect(x1 - 16, y1 - 13, x1 - 6, y1 - 6, p);
        p.setColor(0xFF7D8185);
        c.drawCircle(x0 + 11, y0 + 9.5f, 2.5f, p);
        c.drawCircle(x1 - 11, y1 - 9.5f, 2.5f, p);
    }

    /** ((27.3) A roof of drawBuilding's, as it was drawn there: kind == STADIUM.) */
    private void roofStadium(Canvas c, Paint p, float x0, float y0, float x1, float y1, float bw, float bh) {
        // Tiered seating in club colours.
        p.setColor(0xFF6E7278);
        c.drawRect(x0, y0, x1, y1, p);
        boolean along = bw >= bh;
        int[] seats = {0xFFB83A30, 0xFFE8E8E8};
        int rows = (int) ((along ? bh : bw) / 3);
        for (int k = 0; k < rows; k++) {
            p.setColor(darken(seats[k % 2], 0.8f + 0.2f * k / Math.max(1, rows)));
            if (along) c.drawRect(x0 + 1, y0 + 1 + k * 3, x1 - 1, y0 + 3 + k * 3, p);
            else c.drawRect(x0 + 1 + k * 3, y0 + 1, x0 + 3 + k * 3, y1 - 1, p);
        }
    }

    /** ((27.3) A roof of drawBuilding's, as it was drawn there: kind == POWER.) */
    private void roofPower(Canvas c, Paint p, float x0, float y0, float x1, float y1, int roof, float bw, float bh) {
        p.setColor(darken(roof, 0.7f));
        c.drawRect(x0, y0, x1, y1, p);
        p.setColor(roof);
        c.drawRect(x0 + 2, y0 + 2, x1 - 2, y1 - 2, p);
        p.setColor(darken(roof, 0.85f));
        p.setStrokeWidth(1f);
        for (float x = x0 + 5; x < x1 - 3; x += 4) c.drawLine(x, y0 + 2, x, y1 - 2, p);
        // Two tall chimneys, red and white.
        for (int k = 0; k < 2; k++) {
            float cx = x0 + bw * (0.3f + k * 0.4f), cy = y0 + bh * 0.35f;
            p.setColor(0x60000000);
            c.drawCircle(cx + 3, cy + 4, 5, p);
            p.setColor(0xFFE8E8E8);
            c.drawCircle(cx, cy, 5, p);
            p.setColor(0xFFC8302A);
            c.drawCircle(cx, cy, 3.6f, p);
            p.setColor(0xFF2A2A2A);
            c.drawCircle(cx, cy, 2.2f, p);
        }
    }

    /** ((27.3) A roof of drawBuilding's, as it was drawn there: kind == BARN.) */
    private void roofBarn(Canvas c, Paint p, float x0, float y0, float x1, float y1, int roof, float bw, float bh) {
        p.setColor(darken(roof, 0.7f));
        c.drawRect(x0, y0, x1, y1, p);
        boolean along = bw >= bh;
        p.setColor(lighten(roof, 0.1f));
        if (along) c.drawRect(x0 + 1, y0 + 1, x1 - 1, (y0 + y1) / 2, p);
        else c.drawRect(x0 + 1, y0 + 1, (x0 + x1) / 2, y1 - 1, p);
        p.setColor(roof);
        if (along) c.drawRect(x0 + 1, (y0 + y1) / 2, x1 - 1, y1 - 1, p);
        else c.drawRect((x0 + x1) / 2, y0 + 1, x1 - 1, y1 - 1, p);
        p.setColor(0xFFE8E0D0);
        if (along) c.drawRect(x0 + 1, (y0 + y1) / 2 - 0.7f, x1 - 1, (y0 + y1) / 2 + 0.7f, p);
        else c.drawRect((x0 + x1) / 2 - 0.7f, y0 + 1, (x0 + x1) / 2 + 0.7f, y1 - 1, p);
    }

    /** ((27.3) A roof of drawBuilding's, as it was drawn there: kind == SILO.) */
    private void roofSilo(Canvas c, Paint p, float x0, float y0, float x1, float y1, int roof, float bw) {
        p.setColor(0xFF4C7837);
        c.drawRect(x0, y0, x1, y1, p);
        float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
        p.setColor(darken(roof, 0.7f));
        c.drawCircle(cx, cy, bw / 2 - 0.5f, p);
        p.setColor(roof);
        c.drawCircle(cx, cy, bw / 2 - 1.5f, p);
        p.setColor(lighten(roof, 0.2f));
        c.drawCircle(cx - 1.5f, cy - 1.5f, bw / 4, p);
    }

    /** ((27.3) A roof of drawBuilding's, as it was drawn there: kind == APARTMENT.) */
    private void roofApartment(Canvas c, Paint p, float x0, float y0, float x1, float y1, int roof, Random r, float bw) {
        // Flat roof with a rooftop water tank, stairwell hut and washing lines.
        p.setColor(darken(roof, 0.75f));
        c.drawRect(x0, y0, x1, y1, p);
        p.setColor(roof);
        c.drawRect(x0 + 2, y0 + 2, x1 - 2, y1 - 2, p);
        p.setColor(darken(roof, 0.85f));
        for (float gy = y0 + 4; gy < y1 - 3; gy += 3) c.drawRect(x0 + 2, gy, x1 - 2, gy + 0.4f, p);
        p.setColor(0xFF7A5A3C);
        float tx = x0 + 5 + r.nextFloat() * Math.max(1, bw - 16);
        c.drawCircle(tx + 3, y0 + 7, 3.2f, p);
        p.setColor(0xFF9A7A58);
        c.drawCircle(tx + 3, y0 + 7, 2.2f, p);
        p.setColor(0xFFB8B4AC);
        c.drawRect(x1 - 11, y1 - 11, x1 - 4, y1 - 5, p);
        p.setColor(0xFF6A6A70);
        c.drawRect(x1 - 9, y1 - 7, x1 - 6, y1 - 5, p);
        p.setStrokeWidth(0.4f);
        p.setColor(0xFFDADADA);
        c.drawLine(x0 + 4, (y0 + y1) / 2, x0 + bw * 0.55f, (y0 + y1) / 2, p);
        int[] cloth = {0xFFE05050, 0xFF5080E0, 0xFFF0F0F0, 0xFFE0C050};
        for (int k = 0; k < 4; k++) {
            p.setColor(cloth[k]);
            float lx = x0 + 6 + k * (bw * 0.5f - 6) / 4;
            c.drawRect(lx, (y0 + y1) / 2, lx + 2, (y0 + y1) / 2 + 2.5f, p);
        }
    }

    /** ((27.3) A roof of drawBuilding's, as it was drawn there: kind == GARAGE.) */
    private void roofGarage(Canvas c, Paint p, float x0, float y0, float x1, float y1, int roof, Random r) {
        // Parking garage: the top deck is a car park with a ramp.
        p.setColor(0xFF8E8E88);
        c.drawRect(x0, y0, x1, y1, p);
        p.setColor(roof);
        c.drawRect(x0 + 1.5f, y0 + 1.5f, x1 - 1.5f, y1 - 1.5f, p);
        p.setColor(0xFFE8E8E0);
        p.setStrokeWidth(0.6f);
        for (float gx = x0 + 4; gx < x1 - 6; gx += 8) {
            c.drawLine(gx, y0 + 2, gx, y0 + 12, p);
            c.drawLine(gx, y1 - 12, gx, y1 - 2, p);
        }
        p.setColor(0xFF55575A);
        c.drawRect(x1 - 10, y0 + 14, x1 - 2, y1 - 14, p);
        p.setColor(0xFFE8C547);
        for (float yy = y0 + 16; yy < y1 - 16; yy += 5) c.drawRect(x1 - 7, yy, x1 - 5, yy + 2, p);
        for (float gx = x0 + 4; gx < x1 - 8; gx += 8) {
            if (r.nextFloat() < 0.45f) continue;
            p.setColor(CAR_COLORS[r.nextInt(CAR_COLORS.length)]);
            c.drawRect(gx + 1.2f, y0 + 3, gx + 6.8f, y0 + 11, p);
            if (r.nextFloat() < 0.5f) {
                p.setColor(CAR_COLORS[r.nextInt(CAR_COLORS.length)]);
                c.drawRect(gx + 1.2f, y1 - 11, gx + 6.8f, y1 - 3, p);
            }
        }
    }

    /** ((27.3) A roof of drawBuilding's, as it was drawn there: kind == PHARMACY.) */
    private void roofPharmacy(Canvas c, Paint p, float x0, float y0, float x1, float y1, int roof, float bw, float bh) {
        p.setColor(0xFFB8BEB8);
        c.drawRect(x0, y0, x1, y1, p);
        p.setColor(roof);
        c.drawRect(x0 + 1.5f, y0 + 1.5f, x1 - 1.5f, y1 - 1.5f, p);
        float cxx = (x0 + x1) / 2, cyy = (y0 + y1) / 2, a = Math.min(bw, bh) * 0.22f;
        p.setColor(0xFF2FA84F);
        c.drawRect(cxx - a, cyy - a / 3, cxx + a, cyy + a / 3, p);
        c.drawRect(cxx - a / 3, cyy - a, cxx + a / 3, cyy + a, p);
    }

    /** ((27.3) A roof of drawBuilding's, as it was drawn there: kind == SPIRE.) */
    private void roofSpire(Canvas c, Paint p, float x0, float y0, float x1, float y1) {
        p.setColor(0xFF3A3640);
        c.drawRect(x0, y0, x1, y1, p);
        p.setColor(0xFF524C58);
        Path spire = new Path();
        spire.moveTo((x0 + x1) / 2, y0 + 1);
        spire.lineTo(x1 - 1, (y0 + y1) / 2);
        spire.lineTo((x0 + x1) / 2, y1 - 1);
        spire.lineTo(x0 + 1, (y0 + y1) / 2);
        spire.close();
        c.drawPath(spire, p);
    }

    /** ((27.3) A roof of drawBuilding's, as it was drawn there: kind == SHOP.) */
    private void roofShop(Canvas c, Paint p, int[] b, float x0, float y0, float x1, float y1, int roof, float bw) {
        p.setColor(darken(roof, 0.7f));
        c.drawRect(x0, y0, x1, y1, p);
        p.setColor(roof);
        c.drawRect(x0 + 1.5f, y0 + 1.5f, x1 - 1.5f, y1 - 1.5f, p);
        Random rr = new Random(b[5]);
        p.setColor(0xFFA7ABAF);
        float ux = x0 + 4 + rr.nextFloat() * Math.max(1, bw - 12);
        c.drawRect(ux, y0 + 4, ux + 5, y0 + 8, p);
    }

    /** ((27.3) A roof of drawBuilding's, as it was drawn there: kind == HOSPITAL.) */
    private void roofHospital(Canvas c, Paint p, float x0, float y0, float x1, float y1, int roof, float bw, float bh) {
        p.setColor(0xFFBFC5CB);
        c.drawRect(x0, y0, x1, y1, p);
        p.setColor(roof);
        c.drawRect(x0 + 2, y0 + 2, x1 - 2, y1 - 2, p);
        float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2, s = Math.min(Math.min(bw, bh) * 0.3f, 14);
        p.setColor(0xFFD83A3A);
        c.drawRect(cx - s, cy - s * 0.33f, cx + s, cy + s * 0.33f, p);
        c.drawRect(cx - s * 0.33f, cy - s, cx + s * 0.33f, cy + s, p);
    }

    /** ((27.3) A roof of drawBuilding's, as it was drawn there: kind == WAREHOUSE || kind == BARRACKS.) */
    private void roofWarehouse(Canvas c, Paint p, float x0, float y0, float x1, float y1, int roof, float bw, float bh) {
        p.setColor(darken(roof, 0.7f));
        c.drawRect(x0, y0, x1, y1, p);
        p.setColor(roof);
        c.drawRect(x0 + 2, y0 + 2, x1 - 2, y1 - 2, p);
        p.setColor(darken(roof, 0.88f));
        p.setStrokeWidth(1f);
        if (bw >= bh) for (float x = x0 + 5; x < x1 - 3; x += 4) c.drawLine(x, y0 + 2, x, y1 - 2, p);
        else for (float y = y0 + 5; y < y1 - 3; y += 4) c.drawLine(x0 + 2, y, x1 - 2, y, p);
        p.setColor(0xCC9CC3D9);
        int lights = 1 + (int) (Math.max(bw, bh) / 40);
        for (int i = 0; i < lights; i++) {
            float t = (i + 0.5f) / lights;
            float cx = bw >= bh ? x0 + bw * t : (x0 + x1) / 2, cy = bw >= bh ? (y0 + y1) / 2 : y0 + bh * t;
            c.drawRect(cx - 4, cy - 3, cx + 4, cy + 3, p);
        }
    }

    /** A grid of dark blue solar panels. */
    static void drawSolar(Canvas c, Paint p, float x0, float y0, float x1, float y1) {
        if (x1 - x0 < 4 || y1 - y0 < 4) return;
        p.setColor(0xFF8A8E94);
        c.drawRect(x0, y0, x1, y1, p);
        float cw = 5, ch = 4;
        for (float y = y0 + 0.6f; y + ch <= y1; y += ch + 0.6f)
            for (float x = x0 + 0.6f; x + cw <= x1; x += cw + 0.6f) {
                p.setColor(0xFF1F3A66);
                c.drawRect(x, y, x + cw, y + ch, p);
                p.setColor(0xFF3A5C8E);
                c.drawRect(x, y, x + cw, y + 0.8f, p);
            }
    }

    void drawCar(Canvas c, Paint p, int x, int y) {
        drawCar(c, p, x, y, prnd);
    }

    void drawCar(Canvas c, Paint p, int x, int y, Random prnd) {
        prnd.setSeed(tileSeed(x, y, 3));
        boolean vertical;
        if (isLotCar(x, y)) vertical = true;
        else vertical = city.roadDir[y * city.w + x] == 1;
        float cx = x * T + T / 2f, cy = y * T + T / 2f;
        byte kind = city.carKind.get(y * city.w + x);
        if (kind == 2 || kind == 5) vertical = true;
        float hl = kind == 2 || kind == 5 ? 7.8f : 7.5f, hw = kind == 2 || kind == 5 ? 4.8f : 4.2f;
        RectF rect = vertical ? new RectF(cx - hw, cy - hl, cx + hw, cy + hl) : new RectF(cx - hl, cy - hw, cx + hl, cy + hw);
        if (kind == 4) vertical = true;
        if (kind == 7) {
            // ((22.0) Burnt out: a blackened shell, the glass gone.)
            p.setColor(0x55000000);
            rect.offset(1.5f, 1.5f);
            c.drawRoundRect(rect, 2.5f, 2.5f, p);
            rect.offset(-1.5f, -1.5f);
            p.setColor(0xFF2B2724);
            c.drawRoundRect(rect, 2.5f, 2.5f, p);
            p.setColor(0xFF151312);
            if (vertical) c.drawRect(cx - hw + 1, cy - 4.5f, cx + hw - 1, cy + 5f, p);
            else c.drawRect(cx - 5f, cy - hw + 1, cx + 4.5f, cy + hw - 1, p);
            p.setColor(0xFF5A3A28);
            c.drawCircle(cx + prnd.nextFloat() * 4 - 2, cy + prnd.nextFloat() * 4 - 2, 1.4f, p);
            return;
        }
        int color = kind == 6 ? 0xFF15171A : kind == 4 ? 0xFFC8302A : kind == 1 ? city.country.cruiserBody : kind == 2 ? 0xFF4F5A33 : kind == 3 ? 0xFFF2F2F2
                : kind == 5 ? 0xFFE8862A
                : CAR_COLORS[prnd.nextInt(CAR_COLORS.length)];
        p.setColor(0x55000000);
        rect.offset(1.5f, 1.5f);
        c.drawRoundRect(rect, 2.5f, 2.5f, p);
        rect.offset(-1.5f, -1.5f);
        p.setColor(color);
        c.drawRoundRect(rect, 2.5f, 2.5f, p);
        p.setColor(0xFF1E2A33);
        if (vertical) {
            c.drawRect(cx - hw + 1, cy - 4.5f, cx + hw - 1, cy - 1.5f, p);
            c.drawRect(cx - hw + 1, cy + 3f, cx + hw - 1, cy + 5f, p);
        } else {
            c.drawRect(cx + 1.5f, cy - hw + 1, cx + 4.5f, cy + hw - 1, p);
            c.drawRect(cx - 5f, cy - hw + 1, cx - 3f, cy + hw - 1, p);
        }
        p.setColor(lighten(color, 0.15f));
        if (vertical) c.drawRect(cx - hw + 1.5f, cy - 1f, cx + hw - 1.5f, cy + 2.5f, p);
        else c.drawRect(cx - 2.5f, cy - hw + 1.5f, cx + 1f, cy + hw - 1.5f, p);
        if (kind == 1) {
            // Police cruiser in the country's colours: its doors (or a stripe) and a red/blue light bar.
            p.setColor(city.country.cruiserDoor != city.country.cruiserBody ? city.country.cruiserDoor
                    : city.country.cruiserChecks ? city.country.checkA : city.country.cruiserRoof != 0 ? city.country.cruiserRoof : 0xFFEDEDED);
            if (vertical) {
                c.drawRect(cx - hw, cy - 1.5f, cx - hw + 1.3f, cy + 3f, p);
                c.drawRect(cx + hw - 1.3f, cy - 1.5f, cx + hw, cy + 3f, p);
            } else {
                c.drawRect(cx - 3f, cy - hw, cx + 1.5f, cy - hw + 1.3f, p);
                c.drawRect(cx - 3f, cy + hw - 1.3f, cx + 1.5f, cy + hw, p);
            }
            p.setColor(0xFFE0302A);
            if (vertical) c.drawRect(cx - hw + 1, cy - 0.8f, cx, cy + 0.6f, p);
            else c.drawRect(cx - 0.8f, cy - hw + 1, cx + 0.6f, cy, p);
            p.setColor(0xFF2F6BFF);
            if (vertical) c.drawRect(cx, cy - 0.8f, cx + hw - 1, cy + 0.6f, p);
            else c.drawRect(cx - 0.8f, cy, cx + 0.6f, cy + hw - 1, p);
        } else if (kind == 5) {
            // Works truck: a tipper body behind the cab, and an amber beacon.
            p.setColor(0xFF8A8E92);
            c.drawRect(cx - hw + 1, cy + 0.5f, cx + hw - 1, cy + hl - 1, p);
            p.setColor(0xFF6A6E72);
            for (float yy = cy + 2; yy < cy + hl - 1; yy += 2) c.drawRect(cx - hw + 1, yy, cx + hw - 1, yy + 0.5f, p);
            p.setColor(0xFFF2B02A);
            c.drawCircle(cx, cy - hl + 4, 1.1f, p);
        } else if (kind == 4) {
            // Fire truck: ladder along the top.
            p.setColor(0xFFD8D8D8);
            c.drawRect(cx - 1.5f, cy - hl + 2, cx - 0.7f, cy + hl - 1, p);
            c.drawRect(cx + 0.7f, cy - hl + 2, cx + 1.5f, cy + hl - 1, p);
            for (float yy = cy - hl + 3; yy < cy + hl - 1; yy += 2.5f) c.drawRect(cx - 1.5f, yy, cx + 1.5f, yy + 0.5f, p);
        } else if (kind == 3) {
            // Ambulance: red stripe and cross.
            p.setColor(0xFFD83A3A);
            if (vertical) c.drawRect(cx - hw, cy + 3.5f, cx + hw, cy + 4.6f, p);
            else c.drawRect(cx - 4.6f, cy - hw, cx - 3.5f, cy + hw, p);
            c.drawRect(cx - 0.6f, cy - 2, cx + 0.6f, cy + 2, p);
            c.drawRect(cx - 2, cy - 0.6f, cx + 2, cy + 0.6f, p);
        } else if (kind == 2) {
            // Army truck: canvas-covered cargo bed.
            p.setColor(0xFF5F6B40);
            c.drawRect(cx - hw + 0.8f, cy - 1.5f, cx + hw - 0.8f, cy + hl - 0.8f, p);
            p.setColor(0xFF4A5530);
            for (float yy = cy; yy < cy + hl - 1; yy += 2.2f) c.drawRect(cx - hw + 0.8f, yy, cx + hw - 0.8f, yy + 0.5f, p);
        }
    }

    /** Part of a bend: two roads meeting at a corner, where nobody stops. */
    boolean bendAt(int x, int y) {
        if (!city.junctionAt(x, y)) return false;
        int id = city.junctionId.get(y * city.w + x);
        return id >= 0 && city.junctions.get(id)[4] == J_BEND;
    }

    /** The test range's ground: dust and tracks, firing points by the gate, targets far off, craters and wrecks. */
    void drawTestRange(Canvas c, Paint p, float x0, float y0, float x1, float y1, int seed) {
        Random r = new Random(seed);
        float rw = x1 - x0, rh = y1 - y0;
        p.setStyle(Paint.Style.FILL);
        // Churned-up dirt, darker in the impact area.
        for (int k = 0; k < (int) (rw * rh / 14000); k++) {
            float px = x0 + r.nextFloat() * rw, py = y0 + r.nextFloat() * rh, pr = 10 + r.nextFloat() * 26;
            p.setColor(r.nextBoolean() ? 0x30704A28 : 0x22A07A48);
            c.drawCircle(px, py, pr, p);
        }
        float iy0 = y0 + rh * 0.08f, iy1 = y0 + rh * 0.62f;
        p.setColor(0x2A5A3A1E);
        c.drawOval(new RectF(x0 + rw * 0.1f, iy0, x1 - rw * 0.1f, iy1), p);
        // Vehicle tracks winding across.
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2.2f);
        for (int k = 0; k < 4; k++) {
            Path tr = new Path();
            float sx = x0 + r.nextFloat() * rw;
            tr.moveTo(sx, y1);
            float mx = x0 + r.nextFloat() * rw;
            tr.quadTo(x0 + r.nextFloat() * rw, y0 + rh * 0.75f, mx, y0 + rh * 0.5f);
            tr.quadTo(x0 + r.nextFloat() * rw, y0 + rh * 0.25f, x0 + r.nextFloat() * rw, y0);
            p.setColor(0x40604028);
            c.drawPath(tr, p);
            c.save();
            c.translate(4, 0);
            c.drawPath(tr, p);
            c.restore();
        }
        // Berms across the range.
        p.setStrokeWidth(5f);
        p.setStrokeCap(Paint.Cap.ROUND);
        for (int k = 0; k < 3; k++) {
            float by = y0 + rh * (0.3f + k * 0.18f), bx = x0 + rw * (0.15f + r.nextFloat() * 0.3f);
            p.setColor(0x60886038);
            c.drawLine(bx, by, bx + rw * 0.3f, by, p);
            p.setColor(0x40E8D0A0);
            p.setStrokeWidth(2f);
            c.drawLine(bx, by - 1.5f, bx + rw * 0.3f, by - 1.5f, p);
            p.setStrokeWidth(5f);
        }
        p.setStrokeCap(Paint.Cap.BUTT);
        p.setStyle(Paint.Style.FILL);
        // Craters in the impact area: a dark bowl, a pale thrown-up rim.
        int craters = Math.max(6, (int) (rw * rh / 26000));
        for (int k = 0; k < craters; k++) {
            float cx = x0 + rw * (0.12f + r.nextFloat() * 0.76f), cy = iy0 + r.nextFloat() * (iy1 - iy0), cr = 3 + r.nextFloat() * 6;
            p.setColor(0x50D8C090);
            c.drawCircle(cx, cy, cr * 1.5f, p);
            p.setColor(0xFF5A4028);
            c.drawCircle(cx, cy, cr, p);
            p.setColor(0xFF3A2A1A);
            c.drawCircle(cx + cr * 0.15f, cy + cr * 0.2f, cr * 0.55f, p);
        }
        // Burnt-out hulks used as targets.
        for (int k = 0; k < 4; k++) {
            float hx = x0 + rw * (0.16f + k * 0.21f + r.nextFloat() * 0.06f), hy = iy0 + (iy1 - iy0) * (0.3f + r.nextFloat() * 0.4f);
            c.save();
            c.translate(hx, hy);
            c.rotate(r.nextFloat() * 360);
            c.translate(-hx, -hy);
            p.setColor(0x50000000);
            c.drawRect(hx - 6, hy - 9, hx + 8, hy + 11, p);
            p.setColor(0xFF3A3830);
            c.drawRect(hx - 7, hy - 10, hx + 7, hy + 10, p);
            p.setColor(0xFF4E4A3A);
            c.drawCircle(hx, hy, 4.5f, p);
            p.setColor(0xFF2A2822);
            c.drawRect(hx - 1, hy - 16, hx + 1, hy - 3, p);
            c.restore();
        }
        // A row of targets along the far end, each on its post: white boards with red rings.
        int targets = Math.max(4, (int) (rw / 48));
        for (int k = 0; k < targets; k++) {
            float tx = x0 + rw * (k + 0.5f) / targets, ty = y0 + 8;
            p.setColor(0xFF7A5A3A);
            c.drawRect(tx - 6, ty + 2, tx + 6, ty + 4, p);
            p.setColor(0xFFF2EEE4);
            c.drawRect(tx - 4, ty - 4, tx + 4, ty + 3, p);
            p.setColor(0xFFC8302A);
            c.drawCircle(tx, ty - 0.5f, 2.6f, p);
            p.setColor(0xFFF2EEE4);
            c.drawCircle(tx, ty - 0.5f, 1.6f, p);
            p.setColor(0xFFC8302A);
            c.drawCircle(tx, ty - 0.5f, 0.7f, p);
        }
        // Firing points by the gate: sandbagged bays under a line of red flags.
        float fy = y1 - 16;
        int bays = Math.max(3, (int) (rw / 100));
        for (int k = 0; k < bays; k++) {
            float fx = x0 + rw * (k + 0.5f) / bays;
            if (Math.abs(fx - (x0 + x1) / 2) < 18) continue;
            p.setColor(0xFFB8A070);
            c.drawRect(fx - 8, fy - 3, fx + 8, fy + 3, p);
            p.setColor(0xFF8A7650);
            for (int q = -2; q <= 2; q++) c.drawCircle(fx + q * 3.4f, fy - 3, 1.8f, p);
            p.setColor(0xFF6A5A3A);
            c.drawRect(fx - 0.6f, fy + 4, fx + 0.6f, fy + 11, p);
            p.setColor(0xFFE0302A);
            c.drawRect(fx + 0.6f, fy + 4, fx + 5, fy + 7, p);
        }
        // A warning sign on the gate side.
        p.setColor(0xFFE8C020);
        c.drawRect((x0 + x1) / 2 + 14, y1 - 8, (x0 + x1) / 2 + 26, y1 - 2, p);
        p.setColor(0xFF2A2A2A);
        c.drawRect((x0 + x1) / 2 + 15, y1 - 6, (x0 + x1) / 2 + 25, y1 - 5, p);
    }

    /** ((22.9) Desert City: ground left green (by water, in the parks). */
    boolean lushAt(int x, int y) {
        return city.lush != null && city.lush[y * city.w + x];
    }

    /** The colour of open ground (grass, or in the desert sand unless watered). */
    int grassColour(int x, int y) {
        if (city.desert && !lushAt(x, y)) {
            // (Broad, soft swells of lighter and darker sand, in a few steps so a row still goes down in long runs.)
            float n = (float) (Math.sin(x * 0.043f + y * 0.017f) * Math.sin(y * 0.051f - x * 0.011f + 1.7f));
            int step = Math.round(n * 2);
            int base = city.drawnRealistic ? 0xFFCFB07A : 0xFFD2B47E;
            return step == 0 ? base : step > 0 ? lighten(base, 0.025f * step) : darken(base, 1 + 0.025f * step);
        }
        if (city.desert) return city.drawnRealistic ? 0xFF5E8A3C : 0xFF5A8E3A;
        return city.country.grass != 0 ? city.country.grass : city.drawnRealistic ? 0xFF4F6F36 : 0xFF4C7837;
    }
}
