package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.City.*;

/**
 * ((28.0) Stadium City: a city built round a great stadium complex at the far end of town from downtown - the stadium
 * itself, a second arena, the fan zones and training pitches, all round a long concourse - with the light rail straight
 * across town from Central Station downtown to Stadium Station at the complex, its trains stopping at both. Plazas and
 * the trains, not car parks. Everything it changes is for this map alone.
 */
final class StadiumMap extends CityPart {
    StadiumMap(City city) {
        super(city);
    }

    /** The complex inside its ring of avenues {x0, y0, x1, y1}; and where the trains stop for it (world x, or -1). */
    int[] complex;
    float stopX = -1;
    /** The complex's parts: blocks laid aside, each with what goes on it. */
    final List<int[]> parts = new ArrayList<int[]>();
    final List<String> kinds = new ArrayList<String>();

    static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    void layout(int bx0, int by0, int bx1, int by1, int in) {
        Random r = new Random(city.cfg.seed * 5381L + 28);
        int W = bx1 - bx0, H = by1 - by0;
        District d0 = city.districts.get(0);
        // The complex at the end of town away from downtown; the line through downtown's middle and the complex's.
        boolean east = d0.x < (bx0 + bx1) / 2f;
        int sw = clamp(Math.round(W * (0.4f + 0.06f * r.nextFloat())), 44, 360);
        int sh = clamp(Math.round(H * (0.56f + 0.08f * r.nextFloat())), 46, 300);
        int ry = clamp(Math.round(d0.y) - 1, by0 + sh / 2 + 8, by1 - sh / 2 - 11);
        int sy0 = ry + 1 - sh / 2, sy1 = sy0 + sh;
        int edge = clamp(Math.round(W * 0.07f), 8, 70);
        int sx0 = east ? bx1 - in - edge - sw : bx0 + in + edge, sx1 = sx0 + sw;
        complex = new int[]{sx0, sy0, sx1, sy1};
        city.railY0 = ry;
        city.railRows = 3;
        // The avenues round the complex, the stadium's boulevard down its middle (to the station), and the town round it.
        gen.carve(new Street(sx0 - 5, sy0 - 5, sx1 + 5, sy0, false, true));
        gen.carve(new Street(sx0 - 5, sy1, sx1 + 5, sy1 + 5, false, true));
        gen.carve(new Street(sx0 - 5, sy0 - 5, sx0, sy1 + 5, true, true));
        gen.carve(new Street(sx1, sy0 - 5, sx1 + 5, sy1 + 5, true, true));
        city.fill(sx0, sy0, sw, sh, SIDEWALK);
        int[][] regions = {{bx0 + in, by0 + in, sx0 - 5, by1 - in}, {sx1 + 5, by0 + in, bx1 - in, by1 - in},
                {sx0 - 5, by0 + in, sx1 + 5, sy0 - 5}, {sx0 - 5, sy1 + 5, sx1 + 5, by1 - in}};
        for (int[] g : regions) {
            if (g[1] < ry && g[3] > ry + 3) {
                split(g[0], g[1], g[2], ry);
                split(g[0], ry + 3, g[2], g[3]);
            } else split(g[0], g[1], g[2], g[3]);
        }
        // The line right across the map: level crossings where streets meet it from both sides.
        for (int y = ry; y < ry + 3; y++)
            for (int x = 0; x < city.w; x++) {
                int i = y * city.w + x;
                boolean above = city.roadDir[(ry - 1) * city.w + x] == 1, below = city.roadDir[(ry + 3) * city.w + x] == 1;
                boolean inside = x >= sx0 && x < sx1;
                if (above && below && !inside) {
                    city.tiles[i] = ROAD;
                    city.roadDir[i] = 1;
                } else {
                    city.tiles[i] = RAIL;
                    city.roadDir[i] = 0;
                    city.mainRoad[i] = false;
                }
            }
        roadGen.ensureCrossings(bx0, bx1, ry, 3);
        planComplex(r);
        for (District d : city.districts)
            if (d.x >= sx0 && d.x < sx1 && d.y >= sy0 && d.y < sy1) {
                d.name = "Stadium Quarter";
                break;
            }
    }

    private void split(int x0, int y0, int x1, int y1) {
        if (x1 - x0 >= 6 && y1 - y0 >= 6) gen.split(x0, y0, x1, y1, 1, -1, -1);
    }

    /**
     * Where everything goes in the complex. The half with more room (one side of the line) has the station's concourse
     * along the line, the stadium in the middle, fan zones either side; the other half the arena, the training pitches
     * and another fan zone. Streets between them.
     */
    private void planComplex(Random r) {
        int sx0 = complex[0], sy0 = complex[1], sx1 = complex[2], sy1 = complex[3], ry = city.railY0;
        boolean topMain = ry - sy0 >= sy1 - (ry + 3);
        int my0 = topMain ? sy0 : ry + 3, my1 = topMain ? ry : sy1;
        int oy0 = topMain ? ry + 3 : sy0, oy1 = topMain ? sy1 : ry;
        int sw = sx1 - sx0;
        // The station concourse along the line.
        int pd = clamp((my1 - my0) / 6, 6, 12);
        int py0 = topMain ? my1 - pd : my0, py1 = py0 + pd;
        int cx = (sx0 + sx1) / 2;
        part(sx0, py0, sx1, py1, "Station");
        stopX = cx * T;
        // A street between the concourse and the rest of the main half.
        int sy = topMain ? py0 - 3 : py1;
        gen.carve(new Street(sx0, sy, sx1, sy + 3, false, false));
        int ma0 = topMain ? my0 : sy + 3, ma1 = topMain ? sy : my1;
        // The stadium in the middle of the main half, fan zones either side.
        int stw = clamp(Math.round(sw * 0.5f), 30, 78);
        int s0 = cx - stw / 2, s1 = s0 + stw;
        gen.carve(new Street(s0 - 3, ma0, s0, ma1, true, false));
        gen.carve(new Street(s1, ma0, s1 + 3, ma1, true, false));
        // (On the big maps the stadium by the station, and more of the complex beyond it.)
        int sth = Math.min(ma1 - ma0, Math.max(26, Math.round(stw * 0.78f)));
        int st0 = topMain ? ma1 - sth : ma0, st1 = st0 + sth;
        part(s0, st0, s1, st1, "Grand Stadium");
        if (ma1 - ma0 - sth >= 12) {
            int ey = topMain ? st0 - 3 : st1;
            gen.carve(new Street(s0, ey, s1, ey + 3, false, false));
            venues(s0, topMain ? ma0 : ey + 3, s1, topMain ? ey : ma1, new String[]{"Fan Zone", "Athletics Stadium", "Music Festival Ground"}, r);
        }
        venues(sx0, ma0, s0 - 3, ma1, new String[]{"Fan Zone", "Food Truck Plaza", "Velodrome", "Fan Zone"}, r);
        venues(s1 + 3, ma0, sx1, ma1, new String[]{r.nextBoolean() ? "Fan Zone" : "Food Truck Plaza", "Fan Zone", "Training Ground", "Music Festival Ground"}, r);
        // The other half: the arena, the pitches, a fan zone - in a seeded order.
        int aw = clamp(Math.round(sw * 0.3f), 18, 52), fw = clamp(Math.round(sw * 0.22f), 12, 44);
        int a0 = r.nextBoolean() ? sx0 : sx1 - aw;
        boolean arenaLeft = a0 == sx0;
        String arena = r.nextBoolean() ? "Basketball Arena" : "Ice Rink Arena";
        int b0 = arenaLeft ? sx0 + aw + 3 : sx0, b1 = arenaLeft ? sx1 : sx1 - aw - 3;
        gen.carve(new Street(arenaLeft ? sx0 + aw : sx1 - aw - 3, oy0, arenaLeft ? sx0 + aw + 3 : sx1 - aw, oy1, true, false));
        venues(a0, oy0, a0 + aw, oy1, new String[]{arena, "Fan Zone", "Tennis Club"}, r);
        boolean fanFirst = r.nextBoolean();
        int f0 = fanFirst ? b0 : b1 - fw, f1 = f0 + fw;
        if (b1 - b0 - fw - 3 >= 14) {
            gen.carve(new Street(fanFirst ? f1 : f0 - 3, oy0, fanFirst ? f1 + 3 : f0, oy1, true, false));
            venues(f0, oy0, f1, oy1, new String[]{"Fan Zone", "Food Truck Plaza"}, r);
            venues(fanFirst ? f1 + 3 : b0, oy0, fanFirst ? b1 : f0 - 3, oy1, new String[]{"Pitches", "Athletics Stadium", "Pitches", "Training Ground"}, r);
        } else venues(b0, oy0, b1, oy1, new String[]{"Pitches", "Fan Zone"}, r);
    }

    /**
     * A stretch of the complex cut by streets into parts no more than about sixty tiles a side, each the next of what's
     * on the menu (round again if it runs out).
     */
    private void venues(int x0, int y0, int x1, int y1, String[] menu, Random r) {
        venues(x0, y0, x1, y1, menu, new int[1]);
    }

    private void venues(int x0, int y0, int x1, int y1, String[] menu, int[] next) {
        int w = x1 - x0, h = y1 - y0;
        if (w > 64 && w >= h) {
            int at = x0 + w / 2 - 1;
            gen.carve(new Street(at, y0, at + 3, y1, true, false));
            venues(x0, y0, at, y1, menu, next);
            venues(at + 3, y0, x1, y1, menu, next);
            return;
        }
        if (h > 64) {
            int at = y0 + h / 2 - 1;
            gen.carve(new Street(x0, at, x1, at + 3, false, false));
            venues(x0, y0, x1, at, menu, next);
            venues(x0, at + 3, x1, y1, menu, next);
            return;
        }
        part(x0, y0, x1, y1, menu[next[0]++ % menu.length]);
    }

    private void part(int x0, int y0, int x1, int y1, String kind) {
        if (x1 - x0 < 6 || y1 - y0 < 6) return;
        int[] b = {x0, y0, x1, y1};
        city.blocks.add(b);
        parts.add(b);
        kinds.add(kind);
    }

    /** The complex's parts put up, and Central Station downtown on the line. */
    void places(boolean[] used) {
        if (!city.cfg.stadiumCity() || complex == null) return;
        if (gen.lmRnd == null) {
            gen.lmRnd = new Random(city.cfg.seed * 7919L + 35);
            gen.lmCount = new int[Landmarks.all().size()];
        }
        for (int k = 0; k < city.blocks.size(); k++) {
            int[] b = city.blocks.get(k);
            int j = parts.indexOf(b);
            if (j < 0) continue;
            used[k] = true;
            String kind = kinds.get(j);
            city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], SIDEWALK);
            int ix = b[0] + 1, iy = b[1] + 1, iw = b[2] - b[0] - 2, ih = b[3] - b[1] - 2;
            if (kind.equals("Station")) station(ix, iy, iw, ih);
            else if (kind.equals("Pitches")) pitches(ix, iy, iw, ih);
            else if (!gen.placeParcel(new int[]{ix, iy, ix + iw, iy + ih}, kind)) {
                if (kind.equals("Grand Stadium")) lotGen.stadium(ix, iy, iw, ih);
                else if (!gen.placeParcel(new int[]{ix, iy, ix + iw, iy + ih}, "Fan Zone")) lotGen.plaza(ix, iy, iw, ih);
            }
            if (kind.equals("Fan Zone") || kind.equals("Station")) city.openAreas.add(new float[]{(b[0] + b[2]) / 2f * T, (b[1] + b[3]) / 2f * T, 1});
        }
        // Central Station: on the line, nearest downtown.
        District d0 = city.districts.get(0);
        int best = -1;
        float bd = Float.MAX_VALUE;
        for (int k = 0; k < city.blocks.size(); k++) {
            int[] b = city.blocks.get(k);
            if (used[k] || (b[3] != city.railY0 && b[1] != city.railY0 + city.railRows) || b[2] - b[0] - 2 < 6 || b[3] - b[1] - 2 < 4) continue;
            if (b[0] < complex[2] + 5 && b[2] > complex[0] - 5) continue;
            float d = Math.abs((b[0] + b[2]) / 2f - d0.x);
            if (d < bd) {
                bd = d;
                best = k;
            }
        }
        if (best >= 0) {
            used[best] = true;
            int[] b = city.blocks.get(best);
            city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], SIDEWALK);
            lotGen.trainStation(b[0] + 1, b[1] + 1, b[2] - b[0] - 2, b[3] - b[1] - 2, b[3] == city.railY0);
        } else stopX = -1;
    }

    /** Stadium Station: the long platform concourse along the line, its canopy, the station building in the middle. */
    private void station(int x, int y, int w, int h) {
        city.fill(x, y, w, h, PLAZA);
        boolean railBelow = y + h + 1 == city.railY0;
        int py = railBelow ? y + h - 1 : y;
        city.addDecor(D_CANOPY, x * T + 4, py * T + 2, (x + w) * T - 4, py * T + T - 2, 1);
        int bw = Math.min(w - 4, clamp(w / 4, 10, 30)), bh = Math.max(3, Math.min(6, h - 3));
        int bx = x + (w - bw) / 2, by = railBelow ? y : y + h - bh;
        lotGen.addFacilityLot(bx, by, bw, bh, TRAIN_STATION, 2);
        city.lotNames.put(city.buildingLots.get(city.buildingLots.size() - 1), "Stadium Station");
        // Trees in planters along the concourse.
        for (int i = x + 3; i < x + w - 3; i += 8) {
            int j = railBelow ? y + h - 3 : y + 2;
            if (city.tiles[j * city.w + i] == PLAZA && (i < bx - 1 || i > bx + bw)) city.tiles[j * city.w + i] = TREE;
        }
    }

    /** The training pitches: as many full pitches as fit on the grass, a path round them. */
    private void pitches(int x, int y, int w, int h) {
        city.fill(x, y, w, h, GRASS);
        boolean wide = w >= h;
        int pl = 16, ps = 11;
        int nu = Math.max(1, ((wide ? w : h) - 1) / (pl + 2)), nv = Math.max(1, ((wide ? h : w) - 1) / (ps + 2));
        if ((wide ? w : h) < pl || (wide ? h : w) < ps) {
            city.addDecor(D_FIELD, x * T + 3, y * T + 3, (x + w) * T - 3, (y + h) * T - 3, 0);
            return;
        }
        float gu = ((wide ? w : h) - nu * pl) / (float) (nu + 1), gv = ((wide ? h : w) - nv * ps) / (float) (nv + 1);
        for (int a = 0; a < nu; a++)
            for (int b = 0; b < nv; b++) {
                float u0 = gu + a * (pl + gu), v0 = gv + b * (ps + gv);
                float fx0 = x + (wide ? u0 : v0), fy0 = y + (wide ? v0 : u0);
                city.addDecor(D_FIELD, fx0 * T, fy0 * T, (fx0 + (wide ? pl : ps)) * T, (fy0 + (wide ? ps : pl)) * T, 0);
            }
        city.openAreas.add(new float[]{(x + w / 2f) * T, (y + h / 2f) * T, 0});
    }
}
