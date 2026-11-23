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
 * (27.3) What goes on a city block: lots and houses, shops, parks and plazas, the civic buildings, the service
 * buildings and the landmarks. Taken out of City.java as it stood; the city itself (its tiles, buildings and
 * everything the game asks of it) is `city`.
 */
final class CityBlocks extends CityPart {
    CityBlocks(City city) {
        super(city);
    }

    /** The kind of district at a tile. */
    /** ((22.8) The neighbourhood theme at a tile, or null. */
    Neighbourhoods.Theme themeAt(int tx, int ty) {
        if (city.districtAt == null || !city.cfg.neighbourhoods()) return null;
        tx = Math.max(0, Math.min(city.w - 1, tx));
        ty = Math.max(0, Math.min(city.h - 1, ty));
        return city.districts.get(city.districtAt.get(ty * city.w + tx)).theme;
    }

    // ------------------------------------------------------------------ the government quarter

    /**
     * The town's government: City Hall on its own square near the middle, the courthouse with the jail behind
     * it, the emergency call centre and the public works depot out in the industrial district. (Villages
     * just have a hall; small towns no call centre of their own.)
     */
    void government(boolean[] used) {
        int preset = city.cfg.preset();
        int hall = govBlock(used, 6, 6, -1, 0);
        if (hall < 0) return;
        cityHallSquare(takeBlock(used, hall));
        if (preset != 9) moreGovernment(used, hall, preset);
        // (Since 10.5, every town has its own National Guard armory, out towards the edge of town.)
        if (city.cfg.civic >= 2) {
            int arm = govBlock(used, 9, 7, hall, -2);
            if (arm < 0) arm = govBlock(used, 7, 6, hall, -2);
            if (arm >= 0) armoryCompound(takeBlock(used, arm));
        }
    }

    void moreGovernment(boolean[] used, int hall, int preset) {
        int court = govBlock(used, 11, 7, hall, 20);
        if (court >= 0) courtAndJail(takeBlock(used, court));
        if (preset != 6) {
            int calls = govBlock(used, 6, 6, hall, 30);
            if (calls >= 0) callCentreSite(takeBlock(used, calls));
        }
        int works = govBlock(used, 8, 7, hall, -1);
        if (works >= 0) worksYard(takeBlock(used, works));
    }

    /**
     * A free block big enough (either way round). Near the middle of town for City Hall (near = -1); about
     * {@code dist} tiles from block {@code near} otherwise; or (dist = -1) in the industrial district, as far
     * out as there is.
     */
    int govBlock(boolean[] used, int minLong, int minShort, int near, int dist) {
        float tx = city.w / 2f, ty = city.h / 2f;
        if (near >= 0) {
            int[] nb = city.blocks.get(near);
            tx = (nb[0] + nb[2]) / 2f;
            ty = (nb[1] + nb[3]) / 2f;
        }
        int best = -1;
        float bestScore = Float.MAX_VALUE;
        for (int k = 0; k < city.blocks.size(); k++) {
            int[] b = city.blocks.get(k);
            if (used[k]) continue;
            int iw = b[2] - b[0] - 2, ih = b[3] - b[1] - 2;
            if (Math.max(iw, ih) < minLong || Math.min(iw, ih) < minShort) continue;
            float cx = (b[0] + b[2]) / 2f, cy = (b[1] + b[3]) / 2f;
            float d = (float) World.hypot(cx - tx, cy - ty);
            boolean nearService = false;
            for (Facility f : city.facilities)
                if (World.hypot(f.x - cx * T, f.y - cy * T) < 14 * T) nearService = true;
            float score;
            if (near < 0) score = d;
            else if (dist == -2) score = -d - (city.districtType((int) cx, (int) cy) == DT_SUBURB ? 30 : 0);
            else if (dist < 0) score = (city.districtType((int) cx, (int) cy) == DT_INDUSTRIAL ? 0 : 400) - d;
            else score = Math.abs(d - dist);
            if (nearService) score += 60;
            score += city.rnd.nextFloat() * 3;
            if (score < bestScore) {
                bestScore = score;
                best = k;
            }
        }
        return best;
    }

    /** Marks a block used and paves round it; returns its inside {x, y, w, h}. */
    int[] takeBlock(boolean[] used, int k) {
        used[k] = true;
        int[] b = city.blocks.get(k);
        city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], SIDEWALK);
        return new int[]{b[0] + 1, b[1] + 1, b[2] - b[0] - 2, b[3] - b[1] - 2};
    }

    /**
     * Tile rectangles in a block's own terms: u along its long side, v across. Returns {x, y, w, h}.
     */
    static int[] uv(int[] r, int u, int v, int du, int dv) {
        boolean wide = r[2] >= r[3];
        return wide ? new int[]{r[0] + u, r[1] + v, du, dv} : new int[]{r[0] + v, r[1] + u, dv, du};
    }

    void fillR(int[] q, byte t) {
        city.fill(q[0], q[1], q[2], q[3], t);
    }

    /** City Hall at the back of a paved square, with a fountain, flags and flower beds in front. */
    void cityHallSquare(int[] r) {
        int L = Math.max(r[2], r[3]), S = Math.min(r[2], r[3]);
        city.fill(r[0], r[1], r[2], r[3], PLAZA);
        // The hall faces down the long side of the square.
        int depth = Math.max(3, Math.min(S - 3, Math.round(S * 0.5f)));
        int inset = L >= 10 ? 1 : 0;
        int[] hall = uv(r, inset, 0, L - inset * 2, depth);
        // (Turned so it faces the square: long side along u, front at v = depth.)
        gen.govLot(hall[0], hall[1], hall[2], hall[3], CITY_HALL);
        int sq = S - depth;
        int[] square = uv(r, 0, depth, L, sq);
        float sx0 = square[0] * T, sy0 = square[1] * T, sx1 = (square[0] + square[2]) * T, sy1 = (square[1] + square[3]) * T;
        float mx = (sx0 + sx1) / 2, my = (sy0 + sy1) / 2;
        float fr = Math.min(sx1 - sx0, sy1 - sy0) * 0.32f;
        if (sq >= 3) city.addDecor(D_FOUNTAIN, mx - fr, my - fr, mx + fr, my + fr, 0);
        // Flags in front of the doors.
        boolean wide = r[2] >= r[3];
        if (wide) city.addDecor(D_FLAGS, mx - 16, sy0 + 9, mx + 16, sy0 + 15, 0);
        else city.addDecor(D_FLAGS, sx0 + 9, my - 16, sx0 + 15, my + 16, 1);
        // Flower beds either side of the fountain, and trees at the corners.
        if (L >= 9 && sq >= 3) {
            for (int side = -1; side <= 1; side += 2) {
                float off = (wide ? (sx1 - sx0) : (sy1 - sy0)) * 0.32f * side;
                float fx = wide ? mx + off : mx, fy = wide ? my : my + off;
                city.addDecor(D_FLOWERS, fx - 9, fy - 6, fx + 9, fy + 6, 0);
            }
        }
        int[][] corners = {uv(r, 0, S - 1, 1, 1), uv(r, L - 1, S - 1, 1, 1)};
        if (sq >= 3) for (int[] c : corners) city.tiles[c[1] * city.w + c[0]] = TREE;
        city.openAreas.add(new float[]{mx, my, 1});
    }

    /** The courthouse on its steps, and behind it the jail in a walled yard with a watchtower. */
    void courtAndJail(int[] r) {
        int L = Math.max(r[2], r[3]), S = Math.min(r[2], r[3]);
        city.fill(r[0], r[1], r[2], r[3], PLAZA);
        int cu = Math.max(4, Math.round(L * 0.4f));
        int cv = S >= 8 ? 1 : 0;
        int[] court = uv(r, 0, cv, cu - 1, S - cv * 2);
        gen.govLot(court[0], court[1], court[2], court[3], COURTHOUSE);
        // The jail compound: a wall all round with a gate facing the courthouse.
        int u0 = cu + 1, cl = L - u0;
        fillR(uv(r, u0, 0, cl, S), LOT);
        for (int u = u0; u < L; u++) {
            fillR(uv(r, u, 0, 1, 1), FENCE);
            fillR(uv(r, u, S - 1, 1, 1), FENCE);
        }
        for (int v = 0; v < S; v++) {
            fillR(uv(r, u0, v, 1, 1), FENCE);
            fillR(uv(r, L - 1, v, 1, 1), FENCE);
        }
        int gv = S / 2 - 1;
        fillR(uv(r, u0, gv, 1, 2), LOT);
        // Cell block at the far end, exercise yard in between.
        int inner = cl - 2;
        int jd = Math.min(inner, Math.max(3, Math.round(inner * 0.55f)));
        int[] cells = uv(r, L - 1 - jd, 1, jd, S - 2);
        gen.govLot(cells[0], cells[1], cells[2], cells[3], JAIL);
        int yl = inner - jd;
        if (yl >= 2) {
            int[] yard = uv(r, u0 + 1, 1, yl, S - 2);
            city.addDecor(D_YARD, yard[0] * T + 2, yard[1] * T + 2, (yard[0] + yard[2]) * T - 2, (yard[1] + yard[3]) * T - 2, 0);
            if (yl >= 4 && S >= 8) {
                int[] tw = uv(r, u0 + 1, 1, 2, 2);
                addFacilityLot(tw[0], tw[1], 2, 2, TOWER, 3);
            }
        }
    }

    /** The emergency call centre: a low block with a radio mast and the staff car park. */
    void callCentreSite(int[] r) {
        int L = Math.max(r[2], r[3]), S = Math.min(r[2], r[3]);
        city.fill(r[0], r[1], r[2], r[3], LOT);
        int bu = Math.max(4, Math.round(L * 0.6f));
        int cv = S >= 8 ? 1 : 0;
        int[] b = uv(r, 0, cv, bu, S - cv * 2);
        gen.govLot(b[0], b[1], b[2], b[3], CALL_CENTRE);
        int[] mast = uv(r, L - 3, 0, 3, 3);
        city.addDecor(D_MAST, mast[0] * T, mast[1] * T, (mast[0] + 3) * T, (mast[1] + 3) * T, 0);
        for (int v = 4; v < S - 1; v += 2)
            for (int u = bu + 1; u < L - 1; u++) {
                int[] q = uv(r, u, v, 1, 1);
                if (city.rnd.nextFloat() < 0.5f) {
                    city.tiles[q[1] * city.w + q[0]] = CAR;
                    city.carKind.set(q[1] * city.w + q[0], (byte) ((city.rnd.nextFloat() < 0.2f ? 1 : 0)));
                }
            }
    }

    /**
     * The National Guard armory: a fenced compound with the drill hall along the back, a parade ground with
     * the flags, and the Guard's trucks lined up by the gate.
     */
    void armoryCompound(int[] r) {
        int L = Math.max(r[2], r[3]), S = Math.min(r[2], r[3]);
        city.fill(r[0], r[1], r[2], r[3], BASE);
        for (int u = 0; u < L; u++) {
            fillR(uv(r, u, 0, 1, 1), FENCE);
            fillR(uv(r, u, S - 1, 1, 1), FENCE);
        }
        for (int v = 0; v < S; v++) {
            fillR(uv(r, 0, v, 1, 1), FENCE);
            fillR(uv(r, L - 1, v, 1, 1), FENCE);
        }
        // The gate, in the middle of the front.
        int gu = L / 2 - 1;
        fillR(uv(r, gu, S - 1, 3, 1), BASE);
        int[] gate = uv(r, gu + 1, S - 2, 1, 1);
        city.armoryGate = new float[]{(gate[0] + 0.5f) * T, (gate[1] + 0.5f) * T};
        int depth = Math.max(3, Math.min(6, S / 2 - 1));
        int[] hall = uv(r, 2, 1, L - 4, depth);
        gen.govLot(hall[0], hall[1], hall[2], hall[3], ARMORY);
        // Flags on the parade ground in front of the hall.
        int[] parade = uv(r, gu - 1, depth + 1, 5, 2);
        boolean wide = r[2] >= r[3];
        city.addDecor(D_FLAGS, parade[0] * T, parade[1] * T, (parade[0] + parade[2]) * T, (parade[1] + parade[3]) * T, wide ? 0 : 1);
        // A helipad and the motor pool on a big compound.
        int far = 0;
        if (L >= 20 && S >= 14) {
            int[] pad = uv(r, L - 5, depth + 2, 3, 3);
            fillR(pad, LOT);
            city.addDecor(D_HELIPAD, pad[0] * T + 2, pad[1] * T + 2, (pad[0] + 3) * T - 2, (pad[1] + 3) * T - 2, 0);
            int[] pool = uv(r, 2, depth + 2, 4, Math.min(5, S - depth - 6));
            addFacilityLot(pool[0], pool[1], pool[2], pool[3], BARRACKS, 1);
            far = 6;
        }
        // The trucks in rows, leaving the way to the gate clear.
        for (int tv = S - 3; tv > depth + 2; tv -= 3)
            for (int u = 2 + far; u < L - 2 - far; u += 2) {
                if (u >= gu - 1 && u <= gu + 3) continue;
                int[] q = uv(r, u, tv, 1, 1);
                if (city.tiles[q[1] * city.w + q[0]] != BASE) continue;
                city.tiles[q[1] * city.w + q[0]] = CAR;
                city.carKind.set(q[1] * city.w + q[0], (byte) (2));
            }
    }

    /** The public works depot: a fenced yard with the garage, a salt dome, gravel and the orange trucks. */
    void worksYard(int[] r) {
        int L = Math.max(r[2], r[3]), S = Math.min(r[2], r[3]);
        city.fill(r[0], r[1], r[2], r[3], LOT);
        for (int u = 0; u < L; u++) {
            fillR(uv(r, u, 0, 1, 1), FENCE);
            fillR(uv(r, u, S - 1, 1, 1), FENCE);
        }
        for (int v = 0; v < S; v++) {
            fillR(uv(r, 0, v, 1, 1), FENCE);
            fillR(uv(r, L - 1, v, 1, 1), FENCE);
        }
        // Gates at both ends.
        fillR(uv(r, 0, S / 2 - 1, 1, 2), LOT);
        fillR(uv(r, L - 1, S / 2 - 1, 1, 2), LOT);
        int depth = Math.max(3, Math.min(7, S / 3));
        int big = L >= 20 && S >= 14 ? 1 : 0;
        int[] shed = uv(r, 2, 1, L - 4 - big * 7, depth);
        gen.govLot(shed[0], shed[1], shed[2], shed[3], WORKS);
        // (A big yard has its offices and fuel pumps at the far end.)
        if (big == 1) {
            int[] office = uv(r, L - 8, 1, 5, Math.min(depth, 4));
            fillR(office, BUILDING);
            city.buildingLots.add(new int[]{office[0], office[1], office[2], office[3], 0xFF8A7F70, city.rnd.nextInt(100000), OFFICE, 2, 0xFFC8BCA8, -1});
            int[] pump = uv(r, L - 7, depth + 2, 1, 1);
            city.tiles[pump[1] * city.w + pump[0]] = PUMP;
        }
        int sd = S >= 12 ? 4 : 3;
        int[] salt = uv(r, 1, S - 1 - sd, sd, sd);
        city.addDecor(D_SALT, salt[0] * T, salt[1] * T, (salt[0] + sd) * T, (salt[1] + sd) * T, 0);
        int pl = Math.max(3, L / 5);
        int[] piles = uv(r, L - 1 - pl, S - 3, pl, 2);
        city.addDecor(D_PILES, piles[0] * T, piles[1] * T, (piles[0] + piles[2]) * T, (piles[1] + piles[3]) * T, 0);
        // The trucks, in rows in front of the garage.
        for (int tv = depth + 2; tv < S - 1 - sd && tv < depth + 9; tv += 3)
            for (int u = sd + 2; u < L - 2 - pl; u += 2) {
                int[] q = uv(r, u, tv, 1, 1);
                if (city.tiles[q[1] * city.w + q[0]] != LOT || city.rnd.nextFloat() < 0.25f) continue;
                city.tiles[q[1] * city.w + q[0]] = CAR;
                city.carKind.set(q[1] * city.w + q[0], (byte) (5));
            }
    }

    /** A precinct with police cruisers, or a hospital with ambulances, parked next to it. */
    boolean spareEngine;

    void serviceBuilding(int x, int y, int bw, int bh, int kind, int number) {
        city.fill(x, y, bw, bh, LOT);
        spareEngine = false;
        boolean wide = bw >= bh;
        int sw = wide ? Math.max(3, bw / 2 - 1) : bw - 2, sh = wide ? bh - 2 : Math.max(3, bh / 2 - 1);
        addFacilityLot(x + 1, y + 1, sw, sh, kind, kind == HOSPITAL ? 4 : kind == FIRE_STATION ? 2 : 3);
        int lx = wide ? x + sw + 2 : x, ly = wide ? y : y + sh + 2;
        int lw = wide ? bw - sw - 2 : bw, lh = wide ? bh : bh - sh - 2;
        for (int j = ly + 1; j < ly + lh - 1; j += 3)
            for (int i = lx + 1; i < lx + lw - 1; i++)
                if (city.rnd.nextFloat() < (kind == HOSPITAL ? 0.35f : kind == FIRE_STATION ? 0.2f : 0.6f)) {
                    city.tiles[j * city.w + i] = CAR;
                    // (At a fire station: the crew's own cars, and a spare engine at most. Its engine is the real
                    // one, out on the apron.)
                    boolean spare = kind == FIRE_STATION && !spareEngine && city.rnd.nextFloat() < 0.3f;
                    if (spare) spareEngine = true;
                    city.carKind.set(j * city.w + i, (byte) ((kind == HOSPITAL ? 3 : kind == FIRE_STATION ? (spare ? 4 : 0) : gen.fedLot ? 6 : 1)));
                }
        float cx = (lx + lw / 2f) * T, cy = (ly + lh / 2f) * T;
        float[] c = city.findWalkable(cx, cy);
        if (c == null) c = new float[]{cx, cy};
        if (kind == HOSPITAL) {
            // A helipad on the far end of the car park, and an ambulance bay by the doors.
            int px = wide ? lx + lw - 4 : lx + lw - 4, py = wide ? ly + lh - 4 : ly + lh - 4;
            if (lw >= 5 && lh >= 5) {
                city.fill(px, py, 3, 3, LOT);
                city.addDecor(D_HELIPAD, px * T + 2, py * T + 2, (px + 3) * T - 2, (py + 3) * T - 2, 0);
            }
            float bx0 = wide ? (x + sw + 1) * T + 1 : (x + 1) * T, by0 = wide ? (y + 1) * T : (y + sh + 1) * T + 1;
            if (wide) city.addDecor(D_BAY, bx0, by0, bx0 + T - 2, by0 + Math.min(sh, 4) * T, 1);
            else city.addDecor(D_BAY, bx0, by0, bx0 + Math.min(sw, 4) * T, by0 + T - 2, 0);
            city.facilities.add(new Facility(FACILITY_HOSPITAL, c[0], c[1], 64, c[0], c[1], city.country.hospital));
            return;
        }
        if (kind == FIRE_STATION) {
            int n = 1;
            for (Facility f : city.facilities) if (f.kind == FACILITY_FIRE) n++;
            city.facilities.add(new Facility(FACILITY_FIRE, c[0], c[1], 48, c[0], c[1], city.country.fireStation + n));
            return;
        }
        // (10.52) Number 0 is the police headquarters: where there's room, a second wing (the tactical unit's) and a
        // helipad on the far end of the car park.
        if (number == 0 && lw >= 9 && lh >= 9) {
            int px = lx + lw - 4, py = ly + lh - 4;
            city.fill(px, py, 3, 3, LOT);
            city.addDecor(D_HELIPAD, px * T + 2, py * T + 2, (px + 3) * T - 2, (py + 3) * T - 2, 0);
            int ww = wide ? Math.min(lw - 6, Math.max(4, lw / 2)) : Math.max(4, lw - 6), wh = wide ? Math.max(4, lh - 6) : Math.min(lh - 6, Math.max(4, lh / 2));
            if (ww >= 4 && wh >= 4) addFacilityLot(lx + 1, ly + 1, ww, wh, STATION, 2);
        }
        Facility station = new Facility(FACILITY_POLICE, c[0], c[1], number == 0 ? 80 : 64, c[0], c[1],
                number == 0 ? (city.country.policeStation.startsWith("Police") ? "Police Headquarters" : city.country.policeStation.trim() + " HQ") : city.country.policeStation + number);
        for (int k = -1; k <= 1; k += 2) {
            float[] p = city.findWalkable(c[0] + k * 18, c[1] + 10);
            if (p != null) station.posts.add(p);
        }
        city.facilities.add(station);
    }

    void addFacilityLot(int x, int y, int lw, int lh, int kind, int floors) {
        city.fill(x, y, lw, lh, BUILDING);
        int roof, wall;
        if (kind == STATION && gen.fedLot) {
            // ((16.9) The FBI field office: dark grey and glass.)
            roof = FED_ROOF;
            wall = 0xFF9AA3AD;
        } else if (kind == STATION) {
            roof = 0xFF2F4F86;
            wall = 0xFFD5D9DF;
        } else if (kind == HOSPITAL) {
            roof = 0xFFE9ECEF;
            wall = 0xFFE2E6EA;
        } else if (kind == FIRE_STATION) {
            roof = 0xFFA8322C;
            wall = 0xFFB8574A;
        } else if (kind == CHURCH || kind == SPIRE) {
            roof = 0xFF4A4550;
            wall = 0xFFB5AA98;
        } else if (kind == CRYPT) {
            roof = 0xFF8A8680;
            wall = 0xFF9E9A92;
        } else if (kind == SCHOOL) {
            roof = 0xFF6E6A62;
            wall = 0xFFA85A44;
        } else if (kind == MARKET) {
            roof = 0xFFB8BCC0;
            wall = 0xFFDADDE0;
        } else if (kind == KIOSK) {
            roof = 0xFFE8E8E8;
            wall = 0xFFD6DBE0;
        } else if (kind == TRAIN_STATION) {
            roof = 0xFF5E4A3A;
            wall = 0xFFC4A882;
        } else if (kind == SHOP) {
            int[] sr = city.cfg.variety() ? city.country.shopRoofs37 : city.country.shopRoofs, sw = city.cfg.variety() ? city.country.shopWalls37 : city.country.shopWalls;
            // ((22.8) The neighbourhood's own shopfronts.)
            Neighbourhoods.Theme th = themeAt(x + lw / 2, y + lh / 2);
            if (th != null) {
                sr = th.shopRoofs;
                sw = th.shopWalls;
            }
            roof = sr[city.rnd.nextInt(sr.length)];
            wall = sw[city.rnd.nextInt(sw.length)];
        } else if (kind == TOWER) {
            roof = 0xFF4B5536;
            wall = 0xFF6B7350;
        } else if (kind == MALL) {
            roof = 0xFFC8C4BC;
            wall = 0xFFE2DCD0;
        } else if (kind == STADIUM) {
            roof = 0xFF9EA2A8;
            wall = 0xFFB8BCC0;
        } else if (kind == POWER) {
            roof = 0xFF7C7F82;
            wall = 0xFF9A8A78;
        } else if (kind == BARN) {
            roof = 0xFF8E2E24;
            wall = 0xFFA83A2C;
        } else if (kind == SILO) {
            roof = 0xFFB8BCC0;
            wall = 0xFFC8CCD0;
        } else {
            roof = 0xFF5B6B3A;
            wall = 0xFF7C8456;
        }
        addVariantLot(x, y, lw, lh, roof, kind, floors, wall);
    }
    final List<float[]> lmAt = new ArrayList<float[]>();

    boolean landmark(int x, int y, int bw, int bh) {
        if (!city.cfg.landmarks() || gen.lmLeft <= 0) return false;
        if (gen.lmRnd == null) {
            gen.lmRnd = new Random(city.cfg.seed * 7919L + 35);
            gen.lmCount = new int[Landmarks.all().size()];
        }
        // (The chance keeps up with how many are still to place in the blocks left, so blocks too small for any
        // don't leave the city short.)
        float chance = gen.lmLeft / (float) Math.max(1, gen.lmOpen);
        gen.lmOpen--;
        if (gen.lmRnd.nextFloat() >= chance) return false;
        int dbit = 1 << Math.max(0, Math.min(6, city.districtType(x + bw / 2, y + bh / 2)));
        int s = Math.min(bw, bh), l = Math.max(bw, bh);
        List<Landmarks.Def> defs = Landmarks.all();
        Landmarks.Def pick = null;
        int least = Integer.MAX_VALUE, n = 0;
        // ((22.6) Cities made before (map versions 35 and 36) chose from the first fifty: they still do, so they
        // rebuild the same.)
        int designs = city.cfg.neighbourhoods() ? defs.size() : city.cfg.variety() ? Math.min(defs.size(), 150) : Math.min(defs.size(), 50);
        // ((22.8) The neighbourhood's own kinds first, more often than not.)
        Neighbourhoods.Theme th = themeAt(x + bw / 2, y + bh / 2);
        boolean wantFav = th != null && gen.lmRnd.nextFloat() < 0.6f;
        if (wantFav) {
            Landmarks.Def fav = null;
            int favLeast = Integer.MAX_VALUE, fn = 0;
            for (Landmarks.Def d : defs) {
                if (d.id >= designs || !th.favours(d.name) || s < d.minS || l < d.minL) continue;
                int cnt = gen.lmCount[d.id];
                if (cnt > 0) continue;
                if (cnt < favLeast) {
                    favLeast = cnt;
                    fn = 0;
                }
                if (cnt == favLeast && gen.lmRnd.nextInt(++fn) == 0) fav = d;
            }
            if (fav != null) pick = fav;
        }
        for (Landmarks.Def d : defs) {
            if (pick != null && wantFav) break;
            if (d.id >= designs) break;
            if ((d.districts & dbit) == 0 || s < d.minS || l < d.minL) continue;
            int cnt = gen.lmCount[d.id];
            if (cnt > 0) {
                boolean far = true;
                for (float[] a : lmAt)
                    if ((int) a[2] == d.id && Math.abs(a[0] - x) + Math.abs(a[1] - y) < 90) far = false;
                if (!far) continue;
            }
            if (cnt < least) {
                least = cnt;
                n = 0;
            }
            if (cnt == least && gen.lmRnd.nextInt(++n) == 0) pick = d;
        }
        if (pick == null) return false;
        placeLandmark(pick, x, y, bw, bh);
        gen.lmLeft--;
        return true;
    }

    /** Lays out a landmark on the inside of a block (bw by bh tiles from x, y). */
    void placeLandmark(Landmarks.Def pick, int x, int y, int bw, int bh) {
        Random r = new Random(gen.lmRnd.nextLong());
        int[][] plan = pick.plan(bw, bh, r);
        city.fill(x, y, bw, bh, pick.ground);
        int seed = gen.lmRnd.nextInt(1 << 22);
        city.decor.add(new float[]{D_LANDMARK, x * T, y * T, (x + bw) * T, (y + bh) * T, pick.id, seed});
        for (int i = 0; i < plan.length; i++) {
            int[] q = plan[i];
            int bx = x + q[0], by = y + q[1], qw = Math.min(q[2], x + bw - bx), qh = Math.min(q[3], y + bh - by);
            if (qw < 1 || qh < 1 || bx < x || by < y) continue;
            city.fill(bx, by, qw, qh, BUILDING);
            city.buildingLots.add(new int[]{bx, by, qw, qh, pick.roofCol, seed * 7 + i, pick.base, Math.max(1, q[4]),
                    darken(pick.roofCol, 0.8f), pick.var, pick.id, i});
        }
        if (pick.type == Landmarks.T_PARK || plan.length == 0)
            city.openAreas.add(new float[]{(x + bw / 2f) * T, (y + bh / 2f) * T, pick.ground == PLAZA ? 1 : 0});
        gen.lmCount[pick.id]++;
        lmAt.add(new float[]{x, y, pick.id});
    }

    /**
     * ((22.8) Strings over the streets of the neighbourhoods that have them: red lanterns in Chinatown, warm bulbs in
     * Little Italy and the theatres, coloured flags in the Arts District. Every few tiles along a street, side to side.
     */
    void placeStrings() {
        Random r = new Random(city.cfg.seed * 977L + 38);
        for (Street st : city.streets) {
            int len = st.vertical ? st.y1 - st.y0 : st.x1 - st.x0;
            if (len < 6 || st.width > 6) continue;
            for (int k = 2; k < len - 2; k += 3 + r.nextInt(2)) {
                int ax = st.vertical ? st.x0 + st.width / 2 : st.x0 + k, ay = st.vertical ? st.y0 + k : st.y0 + st.width / 2;
                if (ax < 0 || ay < 0 || ax >= city.w || ay >= city.h || city.tiles[ay * city.w + ax] != ROAD || city.roadDir[ay * city.w + ax] == 3) continue;
                Neighbourhoods.Theme th = themeAt(ax, ay);
                if (th == null || th.strings == 0) continue;
                float x0, y0, x1, y1;
                if (st.vertical) {
                    x0 = st.x0 * T - 6;
                    x1 = st.x1 * T + 6;
                    y0 = y1 = (st.y0 + k) * T + T / 2f;
                } else {
                    y0 = st.y0 * T - 6;
                    y1 = st.y1 * T + 6;
                    x0 = x1 = (st.x0 + k) * T + T / 2f;
                }
                city.decor.add(new float[]{D_STRINGS, x0, y0, x1, y1, th.strings, r.nextInt(1000)});
            }
        }
    }

    /** Fills the inside of one city block according to the map settings. */
    void block(int x, int y, int bw, int bh) {
        gen.curDistrict = city.districtType(x + bw / 2, y + bh / 2);
        float park = PARK_CHANCE[city.cfg.parks()];
        int style = city.cfg.style();
        switch (gen.curDistrict) {
            case DT_DOWNTOWN: style = CityConfig.STYLE_OFFICES; park *= 0.5f; break;
            case DT_MIDTOWN: style = city.rnd.nextFloat() < 0.75f ? CityConfig.STYLE_OFFICES : CityConfig.STYLE_HOUSES; break;
            case DT_OLDTOWN: style = CityConfig.STYLE_OFFICES; break;
            case DT_SUBURB: style = CityConfig.STYLE_HOUSES; break;
            case DT_INDUSTRIAL: style = CityConfig.STYLE_WAREHOUSES; park *= 0.3f; break;
            case DT_CAMPUS: style = CityConfig.STYLE_OFFICES; park = Math.max(park, 0.2f) * 1.6f; break;
            default: style = CityConfig.STYLE_HOUSES; park = Math.max(park, 0.15f) * 2.5f; break;
        }
        float plaza = style == CityConfig.STYLE_HOUSES ? 0.03f : 0.07f;
        if (city.cfg.parks() == 0) plaza *= 0.5f;
        float parking = style == CityConfig.STYLE_WAREHOUSES ? 0.15f : 0.05f;
        // ((22.4) Far fewer whole blocks of car park.)
        if (city.cfg.landmarks()) parking *= 0.35f;
        float open = city.cfg.density() == 0 ? 0.15f : 0f;
        if (city.cfg.density() == 2) {
            park *= 0.6f;
            plaza *= 0.6f;
        }
        float roll = city.rnd.nextFloat();
        float ax = (x + bw / 2f) * T, ay = (y + bh / 2f) * T;
        if (roll < park) {
            parkVariant(x, y, bw, bh);
            city.openAreas.add(new float[]{ax, ay, 0});
        } else if ((roll -= park) < plaza) {
            plaza(x, y, bw, bh);
            city.openAreas.add(new float[]{ax, ay, 1});
        } else if ((roll -= plaza) < parking) {
            parking(x, y, bw, bh);
            city.openAreas.add(new float[]{ax, ay, 2});
        } else if ((roll -= parking) < open) {
            boolean isPark = city.rnd.nextBoolean();
            if (isPark) park(x, y, bw, bh);
            else plaza(x, y, bw, bh);
            city.openAreas.add(new float[]{ax, ay, isPark ? 0 : 1});
        } else {
            // Zoning: an office core, apartments around it, shops along the main roads, houses further
            // out and an industrial district on one side of town.
            float cx = x + bw / 2f - city.w / 2f - gen.shiftX, cy = y + bh / 2f - city.h / 2f - gen.shiftY;
            float d = (float) Math.sqrt(cx * cx + cy * cy) / (city.w * 0.5f);
            float ang = (float) Math.atan2(cy, cx) - gen.industryAngle;
            while (ang > Math.PI) ang -= Math.PI * 2;
            while (ang < -Math.PI) ang += Math.PI * 2;
            boolean industrialZone = gen.curDistrict == DT_INDUSTRIAL;
            boolean mainRoad = city.onMainRoad(x - 1, y - 1, x + bw + 1, y + bh + 1);
            float r = city.rnd.nextFloat();
            // Shops line the main roads; side streets are mostly homes and offices.
            int shopChance = mainRoad ? city.cfg.shopShare() + 20 : city.cfg.shopShare() / 3;
            if (gen.curDistrict == DT_OLDTOWN) shopChance = Math.max(shopChance, 45);
            if (gen.curDistrict == DT_MIDTOWN && mainRoad) shopChance += 15;
            if (gen.curDistrict == DT_OLDTOWN && bw >= 10 && bh >= 10 && city.rnd.nextFloat() < 0.55f) {
                terraces(x, y, bw, bh);
                return;
            }
            if (gen.curDistrict == DT_CAMPUS && bw >= 12 && bh >= 12) {
                // Campus: halls on lawns.
                city.fill(x, y, bw, bh, GRASS);
                lots(x + 2, y + 2, bw - 4, bh - 4, 1);
                return;
            }
            if (style == CityConfig.STYLE_HOUSES && !mainRoad) shopChance = city.cfg.shopShare() / 6;
            if (style != CityConfig.STYLE_HOUSES && bw >= 12 && bh >= 12 && city.rnd.nextFloat() < 0.05f) constructionSite(x, y, bw, bh);
            else if (city.rnd.nextInt(100) < shopChance && bw >= 10 && bh >= 10 && style != CityConfig.STYLE_WAREHOUSES) shops(x, y, bw, bh);
            else if (style == CityConfig.STYLE_HOUSES && city.rnd.nextInt(100) < city.cfg.buildingMix()[0] / 2 && bw >= 10 && bh >= 10) {
                // An apartment block on the edge of the suburbs.
                city.fill(x, y, bw, bh, GRASS);
                addLot(x + 2, y + 2, bw - 4, bh - 4, APARTMENT);
            } else if (style == CityConfig.STYLE_HOUSES) houses(x, y, bw, bh);
            else if (style == CityConfig.STYLE_WAREHOUSES) warehouses(x, y, bw, bh);
            else if (city.cfg.density() == 0 && bw >= 10 && bh >= 10) {
                city.fill(x, y, bw, bh, GRASS);
                lots(x + 2, y + 2, bw - 4, bh - 4, 0);
            } else {
                lots(x, y, bw, bh, 0);
            }
        }
    }

    void lots(int x, int y, int lw, int lh, int depth) {
        int maxDepth = city.cfg.density() == 2 ? 2 : 3;
        if (depth < maxDepth && lw >= 13 && lw >= lh && city.rnd.nextFloat() < 0.75f) {
            int cut = 6 + city.rnd.nextInt(lw - 12);
            lots(x, y, cut, lh, depth + 1);
            lots(x + cut + 1, y, lw - cut - 1, lh, depth + 1);
            if (lh >= 4) city.addDecor(D_ALLEY, (x + cut) * T + 1, y * T, (x + cut + 1) * T - 1, (y + lh) * T, 0);
            return;
        }
        if (depth < maxDepth && lh >= 13 && city.rnd.nextFloat() < 0.75f) {
            int cut = 6 + city.rnd.nextInt(lh - 12);
            lots(x, y, lw, cut, depth + 1);
            lots(x, y + cut + 1, lw, lh - cut - 1, depth + 1);
            if (lw >= 4) city.addDecor(D_ALLEY, x * T, (y + cut) * T + 1, (x + lw) * T, (y + cut + 1) * T - 1, 0);
            return;
        }
        if (lw < 3 || lh < 3) return;
        addLot(x, y, lw, lh, officeKind(lw, lh));
    }

    /** Most lots are offices; some maps mix in apartment blocks, parking garages and pharmacies. */
    int officeKind(int lw, int lh) {
        int[] mix = city.cfg.buildingMix();
        int r = city.rnd.nextInt(100);
        if (r < mix[0] && lw >= 6 && lh >= 6) return APARTMENT;
        if ((r -= mix[0]) < mix[1] && lw >= 8 && lh >= 8) return GARAGE;
        if ((r -= mix[1]) < mix[2] && lw <= 8 && lh <= 8) return PHARMACY;
        return OFFICE;
    }

    void houses(int x, int y, int pw, int ph) {
        if ((pw >= 22 || ph >= 22) && Math.min(pw, ph) >= 14 && city.rnd.nextFloat() < 0.7f) {
            culDeSac(x, y, pw, ph);
            return;
        }
        city.fill(x, y, pw, ph, GRASS);
        // Houses set back from the pavement, each in its own garden.
        for (int cy = y + 1; cy + 4 <= y + ph; cy += 7)
            for (int cx = x + 1; cx + 4 <= x + pw; cx += 7) {
                int hw = Math.min(4 + city.rnd.nextInt(2), x + pw - cx);
                int hh = Math.min(4 + city.rnd.nextInt(2), y + ph - cy);
                if (hw >= 4 && hh >= 4 && city.rnd.nextFloat() > 0.1f) addLot(cx, cy, hw, hh, HOUSE);
            }
        for (int j = y; j < y + ph; j++)
            for (int i = x; i < x + pw; i++)
                if (city.tiles[j * city.w + i] == GRASS && city.rnd.nextFloat() < 0.1f && !city.hasNeighbor(i, j, TREE)
                        && !city.hasNeighbor(i, j, BUILDING))
                    city.tiles[j * city.w + i] = TREE;
    }

    /**
     * A big residential block with a dead-end lane running into it, a turning circle at the end and
     * houses along both sides.
     */
    void culDeSac(int x, int y, int pw, int ph) {
        city.fill(x, y, pw, ph, GRASS);
        boolean vertical = ph >= pw;
        int len = (vertical ? ph : pw) * 2 / 3;
        int mid = vertical ? x + pw / 2 - 1 : y + ph / 2 - 1;
        boolean fromStart = city.rnd.nextBoolean();
        // The lane (2 tiles wide), entering from one end of the block.
        for (int k = 0; k < len; k++)
            for (int t = 0; t < 2; t++) {
                int along = fromStart ? k : (vertical ? ph : pw) - 1 - k;
                int i = vertical ? mid + t : (x + along), j = vertical ? (y + along) : mid + t;
                city.tiles[j * city.w + i] = ROAD;
            }
        // Where the lane meets the street, the pavement is dropped.
        for (int t = 0; t < 2; t++) {
            int i = vertical ? mid + t : (fromStart ? x - 1 : x + pw), j = vertical ? (fromStart ? y - 1 : y + ph) : mid + t;
            if (i >= 0 && j >= 0 && i < city.w && j < city.h && city.tiles[j * city.w + i] == SIDEWALK) city.tiles[j * city.w + i] = ROAD;
        }
        // Turning circle.
        int endAlong = fromStart ? len - 1 : (vertical ? ph : pw) - len;
        for (int a = -1; a <= 1; a++)
            for (int b = -1; b <= 2; b++) {
                int i = vertical ? mid + b : x + endAlong + a, j = vertical ? y + endAlong + a : mid + b;
                if (i >= x && j >= y && i < x + pw && j < y + ph) city.tiles[j * city.w + i] = ROAD;
            }
        // Houses facing the lane on both sides, then more filling the rest of the block.
        for (int k = 1; k + 4 <= (vertical ? ph : pw) - 1; k += 6)
            for (int side = 0; side < 2; side++) {
                int hw = 4, hd = 4 + city.rnd.nextInt(2);
                int along = (vertical ? y : x) + k;
                int across = side == 0 ? mid - 2 - hd : mid + 4;
                int lx = vertical ? across : along, ly = vertical ? along : across;
                placeHouse(lx, ly, vertical ? hd : hw, vertical ? hw : hd, x, y, pw, ph);
            }
        for (int cy = y + 1; cy + 4 <= y + ph; cy += 6)
            for (int cx = x + 1; cx + 4 <= x + pw; cx += 6) placeHouse(cx, cy, 4, 4, x, y, pw, ph);
        for (int j = y; j < y + ph; j++)
            for (int i = x; i < x + pw; i++)
                if (city.tiles[j * city.w + i] == GRASS && city.rnd.nextFloat() < 0.1f && !city.hasNeighbor(i, j, TREE)
                        && !city.hasNeighbor(i, j, BUILDING) && !city.hasNeighbor(i, j, ROAD))
                    city.tiles[j * city.w + i] = TREE;
    }

    /** A house if the spot is inside the block, on grass, and has a garden's width from everything else. */
    void placeHouse(int lx, int ly, int lw, int lh, int x, int y, int pw, int ph) {
        if (lx < x || ly < y || lx + lw > x + pw || ly + lh > y + ph) return;
        for (int j = ly - 1; j <= ly + lh; j++)
            for (int i = lx - 1; i <= lx + lw; i++) {
                if (i < x || j < y || i >= x + pw || j >= y + ph) continue;
                byte t = city.tiles[j * city.w + i];
                boolean inside = i >= lx && j >= ly && i < lx + lw && j < ly + lh;
                if (t == BUILDING || (inside && t != GRASS)) return;
            }
        if (city.rnd.nextFloat() > 0.06f) addLot(lx, ly, lw, lh, HOUSE);
    }

    /** A station building facing the street, a platform along the tracks and a small car park. */
    void trainStation(int x, int y, int bw, int bh, boolean railBelow) {
        city.fill(x, y, bw, bh, PLAZA);
        city.stationX = (x + bw / 2f) * T;
        city.stationY = railBelow ? (y + bh - 0.5f) * T : (y + 0.5f) * T;
        city.stationName = "Central Station";
        int sw = Math.min(bw - 2, Math.max(12, bw * 2 / 3)), sh = Math.max(4, Math.min(8, bh - 3));
        int sx = x + (bw - sw) / 2, sy = railBelow ? y + 1 : y + bh - 1 - sh;
        addFacilityLot(sx, sy, sw, sh, TRAIN_STATION, 2);
        // Platform canopy along the tracks.
        int py = railBelow ? y + bh - 1 : y;
        city.addDecor(D_CANOPY, x * T + 4, py * T + 2, (x + bw) * T - 4, py * T + T - 2, 1);
        city.openAreas.add(new float[]{(x + bw / 2f) * T, (y + bh / 2f) * T, 1});
    }

    /**
     * An old-town block: narrow terraced buildings shoulder to shoulder around the edge, shops on the ground
     * floor, and a courtyard in the middle.
     */
    void terraces(int x, int y, int bw, int bh) {
        city.fill(x, y, bw, bh, PLAZA);
        int depth = bh >= 16 && bw >= 16 ? 6 : 4;
        for (int side = 0; side < 4; side++) {
            boolean horizontal = side < 2;
            int len = horizontal ? bw : bh - 2 * depth;
            int i = 0;
            boolean arch = !horizontal;
            while (i < len) {
                // An archway through the front and back rows into the courtyard.
                if (!arch && i >= len / 2 - 1) {
                    arch = true;
                    i += 2;
                    continue;
                }
                int uw = Math.min(3 + city.rnd.nextInt(3), len - i);
                if (uw < 3) break;
                int lx, ly, lw, lh;
                if (side == 0) { lx = x + i; ly = y; lw = uw; lh = depth; }
                else if (side == 1) { lx = x + i; ly = y + bh - depth; lw = uw; lh = depth; }
                else if (side == 2) { lx = x; ly = y + depth + i; lw = depth; lh = uw; }
                else { lx = x + bw - depth; ly = y + depth + i; lw = depth; lh = uw; }
                if (city.rnd.nextFloat() < 0.45f) addFacilityLot(lx, ly, lw, lh, SHOP, 2 + city.rnd.nextInt(2));
                else addLot(lx, ly, lw, lh, city.rnd.nextFloat() < 0.4f ? APARTMENT : OFFICE);
                i += uw;
            }
        }
        // A tree or two in the courtyard.
        int cx = x + bw / 2, cy = y + bh / 2;
        if (bw >= 14 && bh >= 14 && city.tiles[cy * city.w + cx] == PLAZA) city.tiles[cy * city.w + cx] = TREE;
    }

    void warehouses(int x, int y, int pw, int ph) {
        city.fill(x, y, pw, ph, LOT);
        int bx = x + 1, by = y + 1, bw = pw - 2, bh = ph - 2;
        if (bw < 3 || bh < 3) {
            parking(x, y, pw, ph);
            return;
        }
        if (bw >= 10 && city.rnd.nextBoolean()) {
            int cut = bw / 2 - 1;
            addLot(bx, by, cut, bh, WAREHOUSE);
            addLot(bx + cut + 2, by, bw - cut - 2, bh, WAREHOUSE);
        } else {
            addLot(bx, by, bw, bh, WAREHOUSE);
        }
        for (int j = y; j < y + ph; j++)
            for (int i = x; i < x + pw; i++)
                if (city.tiles[j * city.w + i] == LOT && city.rnd.nextFloat() < 0.12f && !city.hasNeighbor(i, j, CAR))
                    city.tiles[j * city.w + i] = CAR;
    }

    int oldWall() {
        int[] a = city.country.oldWalls != null ? city.country.oldWalls : OLD_WALLS;
        return a[city.rnd.nextInt(a.length)];
    }

    int oldRoof() {
        int[] a = city.country.oldRoofs != null ? city.country.oldRoofs : OLD_ROOFS;
        return a[city.rnd.nextInt(a.length)];
    }

    void addLot(int x, int y, int lw, int lh, int kind) {
        city.fill(x, y, lw, lh, BUILDING);
        int floors, roof, wall;
        int height = city.cfg.height();
        if (kind == HOUSE) {
            floors = 1 + city.rnd.nextInt(2);
            // ((22.9) Desert City: low adobe and stucco, a single storey.)
            if (city.desert) floors = 1;
            int[] hr = city.cfg.variety() ? city.country.houseRoofs37 : city.country.houseRoofs, hw = city.cfg.variety() ? city.country.houseWalls37 : city.country.houseWalls;
            roof = hr[city.rnd.nextInt(hr.length)];
            wall = hw[city.rnd.nextInt(hw.length)];
        } else if (kind == WAREHOUSE) {
            floors = 1 + city.rnd.nextInt(2) + (height >= 2 ? 1 : 0);
            int[] wr = city.cfg.neighbourhoods() ? WAREHOUSE_ROOFS38 : WAREHOUSE_ROOFS, ww = city.cfg.neighbourhoods() ? WAREHOUSE_WALLS38 : WAREHOUSE_WALLS;
            roof = wr[city.rnd.nextInt(wr.length)];
            wall = ww[city.rnd.nextInt(ww.length)];
        } else if (kind == APARTMENT) {
            floors = 3 + city.rnd.nextInt(4) + height * 2 + (city.cfg.towers() ? 6 : 0);
            roof = darken(ROOFS[city.rnd.nextInt(ROOFS.length)], 0.9f);
            int[] aw = city.cfg.variety() ? city.country.apartmentWalls37 : city.country.apartmentWalls;
            wall = aw[city.rnd.nextInt(aw.length)];
        } else if (kind == GARAGE) {
            floors = 3 + city.rnd.nextInt(2);
            roof = 0xFF6C6E70;
            wall = 0xFFA8A8A2;
        } else if (kind == PHARMACY) {
            floors = 1 + city.rnd.nextInt(2);
            roof = 0xFFE4E8E4;
            wall = 0xFFF0F2EE;
        } else {
            // Downtown (the middle of the map) gets the tallest towers; small lots stay low.
            float cx = x + lw / 2f - city.w / 2f - gen.shiftX, cy = y + lh / 2f - city.h / 2f - gen.shiftY;
            float dt = 1 - Math.min(1, (float) Math.sqrt(cx * cx + cy * cy) / (city.w * 0.55f));
            float r = city.rnd.nextFloat();
            switch (height) {
                case 0: floors = 1 + city.rnd.nextInt(3); break;
                case 1: floors = 2 + city.rnd.nextInt(3) + (int) (dt * dt * r * 9); break;
                case 2: floors = 3 + city.rnd.nextInt(4) + (int) (dt * dt * r * 14); break;
                default: floors = 4 + city.rnd.nextInt(5) + (int) (dt * r * 18); break;
            }
            // ((15.2) Downtown Towers: skyscrapers, the tallest at the heart of it.)
            if (city.cfg.towers() && lw * lh >= 36) floors = Math.max(floors, 8 + city.rnd.nextInt(8) + (int) (dt * r * 34));
            int min = Math.min(lw, lh);
            if (min <= 2) floors = Math.min(floors, 4);
            else if (min <= 3) floors = Math.min(floors, 8);
            else if (min <= 4) floors = Math.min(floors, 14);
            floors = Math.min(floors, 22);
            int[] roofs = city.country.officeRoofs != null ? city.country.officeRoofs : city.cfg.neighbourhoods() ? ROOFS38 : ROOFS;
            int[] walls = city.country.officeWalls != null ? city.country.officeWalls : city.cfg.neighbourhoods() ? WALLS38 : WALLS;
            roof = roofs[city.rnd.nextInt(roofs.length)];
            wall = walls[city.rnd.nextInt(walls.length)];
            if (gen.curDistrict == DT_DOWNTOWN) {
                // Downtown towers climb higher and many are clad in glass.
                floors = Math.min(24, floors + 2 + city.rnd.nextInt(3));
                if (city.rnd.nextFloat() < 0.55f) wall = city.cfg.neighbourhoods() ? GLASS_WALLS38[city.rnd.nextInt(GLASS_WALLS38.length)] : GLASS_WALLS[city.rnd.nextInt(GLASS_WALLS.length)];
            } else if (gen.curDistrict == DT_OLDTOWN || gen.curDistrict == DT_CAMPUS) {
                // Old brick and stone, a few storeys high.
                floors = 2 + city.rnd.nextInt(3);
                wall = oldWall();
                roof = oldRoof();
            } else if (gen.curDistrict == DT_SUBURB || gen.curDistrict == DT_PARKSIDE) {
                floors = Math.min(floors, 5);
            }
        }
        // (Paris keeps to its height limit outside La Défense.)
        if (kind == APARTMENT && gen.curDistrict == DT_OLDTOWN) {
            floors = Math.min(floors, 5);
            wall = oldWall();
            roof = oldRoof();
        }
        // ((22.8) The neighbourhood's own colours.)
        Neighbourhoods.Theme th = themeAt(x + lw / 2, y + lh / 2);
        if (th != null) {
            if (kind == HOUSE || kind == APARTMENT) {
                roof = Neighbourhoods.pick(th.roofs, city.rnd);
                wall = Neighbourhoods.pick(th.walls, city.rnd);
                if (kind == APARTMENT) roof = darken(roof, 0.9f);
            } else if (kind == OFFICE) {
                wall = Neighbourhoods.pick(th.officeWalls, city.rnd);
                roof = Neighbourhoods.pick(th.roofs, city.rnd);
            } else if (kind == WAREHOUSE) {
                wall = Neighbourhoods.pick(th.officeWalls, city.rnd);
                // ((27.5) Old warehouses made lofts: roof gardens on many of them.)
                if (th == Neighbourhoods.NB_THEMES[City.NB_LOFTS - City.NB_TRAILERS]) roof = Neighbourhoods.pick(th.roofs, city.rnd);
            }
        }
        addVariantLot(x, y, lw, lh, roof, kind, floors, wall);
    }

    /**
     * Records a lot, maybe as one of the country's and district's own kinds of building (a café, a factory,
     * a konbini...), which sets its roof, walls and height.
     */
    void addVariantLot(int x, int y, int lw, int lh, int roof, int kind, int floors, int wall) {
        int dist = city.districtType(x + lw / 2, y + lh / 2);
        int var = Variants.pick(kind, dist, lw, lh, city.rnd);
        Variants.V v = Variants.get(var);
        if (v != null && v.floorsMin > 0) floors = v.floorsMin + city.rnd.nextInt(Math.max(1, v.floorsMax - v.floorsMin + 1));
        // (One of the newer kinds instead, now and then: chosen with numbers of its own, so the rest of the
        // city is laid out exactly as before.)
        int swapped = Variants.swap(var, kind, dist, lw, lh);
        if (swapped != var) {
            var = swapped;
            v = Variants.get(var);
            if (v != null && v.floorsMin > 0) floors = v.floorsMin + Variants.extra.nextInt(Math.max(1, v.floorsMax - v.floorsMin + 1));
        }
        if (v != null) {
            // Small lots stay low whatever they are.
            int min = Math.min(lw, lh);
            if (min <= 5) floors = Math.min(floors, 6);
            else if (min <= 8) floors = Math.min(floors, 14);
            roof = v.roofCol;
            if (v.wall != 0) wall = v.wall;
        }
        city.buildingLots.add(new int[]{x, y, lw, lh, roof, city.rnd.nextInt(100000), kind, floors, wall, var});
    }

    /** A row of small shops around the edge of the block with a service lot behind. */
    void shops(int x, int y, int bw, int bh) {
        city.fill(x, y, bw, bh, LOT);
        int depth = bh >= 16 ? 6 : 4;
        for (int side = 0; side < 2; side++) {
            int sy = side == 0 ? y : y + bh - depth;
            int i = x;
            while (i < x + bw) {
                int uw = Math.min(3 + city.rnd.nextInt(3), x + bw - i);
                if (uw >= 3) addFacilityLot(i, sy, uw, depth, SHOP, 1 + (city.rnd.nextFloat() < 0.3f ? 1 : 0));
                i += uw;
            }
        }
        // Behind the shopfronts: offices and flats (a service yard with a few parked cars in the suburbs).
        int iy0 = y + depth + 1, iy1 = y + bh - depth - 1;
        if (iy1 - iy0 >= 8 && gen.curDistrict != DT_SUBURB && gen.curDistrict != DT_PARKSIDE) {
            city.fill(x, iy0, bw, iy1 - iy0, SIDEWALK);
            lots(x + 1, iy0 + 1, bw - 2, iy1 - iy0 - 2, 1);
            return;
        }
        for (int j = iy0; j < iy1; j++)
            for (int i = x + 1; i < x + bw - 1; i++)
                if (city.rnd.nextFloat() < 0.12f && !city.hasNeighbor(i, j, CAR)) city.tiles[j * city.w + i] = CAR;
    }

    /** A church with a steeple, and a churchyard with graves. */
    void church(int x, int y, int bw, int bh) {
        city.fill(x, y, bw, bh, GRASS);
        boolean wide = bw >= bh;
        int cw = wide ? Math.min(bw - 6, Math.max(8, bw / 2)) : Math.min(bw - 4, 8);
        int ch = wide ? Math.min(bh - 4, 8) : Math.min(bh - 6, Math.max(8, bh / 2));
        addFacilityLot(x + 2, y + 2, cw, ch, CHURCH, 3);
        if (wide) addFacilityLot(x + 2 + cw, y + 2 + ch / 2 - 1, 2, 2, SPIRE, 8);
        else addFacilityLot(x + 2 + cw / 2 - 1, y + 2 + ch, 2, 2, SPIRE, 8);
        graves(x, y, bw, bh, 0.5f);
    }

    void cemetery(int x, int y, int bw, int bh) {
        city.fill(x, y, bw, bh, GRASS);
        int cx = x + bw / 2, cy = y + bh / 2;
        for (int i = x; i < x + bw; i++) city.tiles[cy * city.w + i] = PLAZA;
        for (int j = y; j < y + bh; j++) city.tiles[j * city.w + cx] = PLAZA;
        if (bw >= 14 && bh >= 14) addFacilityLot(cx + 2, cy + 2, 3, 3, CRYPT, 1);
        graves(x, y, bw, bh, 0.8f);
        for (int j = y; j < y + bh; j++)
            for (int i = x; i < x + bw; i++)
                if ((i == x || i == x + bw - 1 || j == y || j == y + bh - 1) && city.tiles[j * city.w + i] == GRASS
                        && city.rnd.nextFloat() < 0.25f && !city.hasNeighbor(i, j, TREE)) city.tiles[j * city.w + i] = TREE;
    }

    /** Rows of headstones on the grass (they are decoration; people walk between them). */
    void graves(int x, int y, int bw, int bh, float density) {
        for (int j = y + 1; j < y + bh - 1; j++)
            for (int i = x + 1; i < x + bw - 1; i++) {
                if (city.tiles[j * city.w + i] != GRASS || city.rnd.nextFloat() > density) continue;
                for (int k = 0; k < 2; k++) {
                    float gx = i * T + 4 + k * 8, gy = j * T + 5;
                    city.addDecor(D_GRAVE, gx, gy, gx + 4, gy + 6, city.rnd.nextInt(3));
                }
            }
    }

    /** A school: the building, a basketball court on the tarmac and a sports field. */
    void school(int x, int y, int bw, int bh) {
        city.fill(x, y, bw, bh, LOT);
        boolean wide = bw >= bh;
        int sw = wide ? Math.max(4, bw / 2 - 1) : bw - 2, sh = wide ? bh - 2 : Math.max(4, bh / 2 - 1);
        addFacilityLot(x + 1, y + 1, sw, sh, SCHOOL, 2 + city.rnd.nextInt(2));
        int rx = wide ? x + sw + 2 : x + 1, ry = wide ? y + 1 : y + sh + 2;
        int rw = wide ? bw - sw - 3 : bw - 2, rh = wide ? bh - 2 : bh - sh - 3;
        if (rw < 3 || rh < 3) return;
        if (rw * rh >= 30) {
            city.fill(rx, ry, rw, rh, GRASS);
            city.addDecor(D_FIELD, rx * T + 3, ry * T + 3, (rx + rw) * T - 3, (ry + rh) * T - 3, 0);
        } else {
            city.addDecor(D_COURT, rx * T + 2, ry * T + 2, (rx + rw) * T - 2, (ry + rh) * T - 2, 0);
        }
    }

    /** A supermarket and its big car park. */
    void market(int x, int y, int bw, int bh) {
        city.fill(x, y, bw, bh, LOT);
        boolean wide = bw >= bh;
        int mw = wide ? bw - 2 : Math.max(4, bw / 2), mh = wide ? Math.max(4, bh / 2) : bh - 2;
        addFacilityLot(x + 1, y + 1, mw, mh, MARKET, 1);
        int px = wide ? x : x + mw + 2, py = wide ? y + mh + 2 : y;
        int pw = wide ? bw : bw - mw - 2, ph = wide ? bh - mh - 2 : bh;
        for (int j = py; j < py + ph; j++) {
            if ((j - py) % 3 != 1) continue;
            for (int i = px + 1; i < px + pw - 1; i++) if (city.rnd.nextFloat() < 0.5f) city.tiles[j * city.w + i] = CAR;
        }
    }

    /** A gas station: canopy over the pumps and a little shop. */
    void gasStation(int x, int y, int bw, int bh) {
        city.fill(x, y, bw, bh, LOT);
        addFacilityLot(x + bw - 5, y + 1, 4, 3, KIOSK, 1);
        int cx0 = x + 1, cy0 = y + 1, cx1 = x + bw - 6, cy1 = y + bh - 1;
        if (cx1 - cx0 < 2) {
            int j = y + bh / 2;
            city.tiles[j * city.w + x + 1] = PUMP;
            city.pumps.add(new float[]{(x + 1) * T + T / 2f, j * T + T / 2f, 1});
            return;
        }
        city.addDecor(D_CANOPY, cx0 * T + 2, cy0 * T + 2, cx1 * T - 2, cy1 * T - 2, 0);
        // Two islands of pumps under the canopy, well spaced (at least one).
        int placed = 0;
        for (int j = cy0 + 1; j < cy1 - 1 && placed < 4; j += 3)
            for (int i = cx0 + 1; i < cx1 - 1 && placed < 4; i += 3) {
                city.tiles[j * city.w + i] = PUMP;
                city.pumps.add(new float[]{i * T + T / 2f, j * T + T / 2f, 1});
                placed++;
            }
        if (placed == 0) {
            int i = (cx0 + cx1) / 2, j = (cy0 + cy1) / 2;
            city.tiles[j * city.w + i] = PUMP;
            city.pumps.add(new float[]{i * T + T / 2f, j * T + T / 2f, 1});
        }
    }

    /** Picks a kind of park by the map's taste. */
    void parkVariant(int x, int y, int bw, int bh) {
        int[] mix = city.cfg.parkMix();
        int total = 0;
        for (int m : mix) total += m;
        int r = city.rnd.nextInt(Math.max(1, total)), kind = 0;
        while (kind < mix.length - 1 && r >= mix[kind]) r -= mix[kind++];
        if (kind == 2 && (bw < 7 || bh < 6)) kind = 0;
        switch (kind) {
            case 1:
                park(x, y, bw, bh);
                // A playground in one corner, clear of the paths.
                int qw = Math.max(2, bw / 2 - 1), qh = Math.max(2, bh / 2 - 1);
                for (int j = y; j < y + qh; j++) for (int i = x; i < x + qw; i++) if (city.tiles[j * city.w + i] == TREE) city.tiles[j * city.w + i] = GRASS;
                city.addDecor(D_PLAYGROUND, x * T + 3, y * T + 3, (x + qw) * T - 2, (y + qh) * T - 2, 0);
                break;
            case 2:
                city.fill(x, y, bw, bh, GRASS);
                city.addDecor(D_FIELD, x * T + 4, y * T + 4, (x + bw) * T - 4, (y + bh) * T - 4, 0);
                break;
            case 3:
                city.fill(x, y, bw, bh, LOT);
                // Courts at their real size, as many as fit (each about 11 x 7 tiles with its run-off).
                boolean longX = bw >= bh;
                int courtsX = Math.max(1, Math.min(4, bw / (longX ? 12 : 8))), courtsY = Math.max(1, Math.min(4, bh / (longX ? 8 : 12)));
                float cw = bw * T / (float) courtsX, chh = bh * T / (float) courtsY;
                for (int a = 0; a < courtsX; a++)
                    for (int b = 0; b < courtsY; b++)
                        city.addDecor(D_COURT, x * T + a * cw + 4, y * T + b * chh + 4, x * T + (a + 1) * cw - 4,
                                y * T + (b + 1) * chh - 4, city.rnd.nextInt(2));
                break;
            case 4:
                city.fill(x, y, bw, bh, GRASS);
                city.addDecor(D_GARDEN, x * T + 4, y * T + 4, (x + bw) * T - 4, (y + bh) * T - 4, city.rnd.nextInt(3));
                break;
            case 5:
                city.fill(x, y, bw, bh, PLAZA);
                city.addDecor(D_SKATE, x * T + 6, y * T + 6, (x + bw) * T - 6, (y + bh) * T - 6, 0);
                break;
            default:
                park(x, y, bw, bh);
                if (bw >= 6 && bh >= 6 && city.rnd.nextFloat() < 0.45f) bandstand(x, y, bw, bh);
                break;
        }
    }

    /** A bandstand in the middle of the park, with flower beds around it. */
    void bandstand(int x, int y, int bw, int bh) {
        int cx = x + bw / 2, cy = y + bh / 2;
        for (int j = cy - 1; j <= cy + 1; j++)
            for (int i = cx - 1; i <= cx + 1; i++) city.tiles[j * city.w + i] = PLAZA;
        city.addDecor(D_BANDSTAND, (cx - 1) * T, (cy - 1) * T, (cx + 2) * T, (cy + 2) * T, 0);
        int[][] beds = {{x + 1, y + 1}, {x + bw - 3, y + 1}, {x + 1, y + bh - 3}, {x + bw - 3, y + bh - 3}};
        for (int[] b : beds) {
            boolean clear = true;
            for (int j = b[1]; j < b[1] + 2; j++)
                for (int i = b[0]; i < b[0] + 2; i++) if (city.tiles[j * city.w + i] != GRASS && city.tiles[j * city.w + i] != TREE) clear = false;
            if (!clear) continue;
            city.fill(b[0], b[1], 2, 2, GRASS);
            city.addDecor(D_FLOWERS, b[0] * T + 3, b[1] * T + 3, (b[0] + 2) * T - 3, (b[1] + 2) * T - 3, city.rnd.nextInt(3));
        }
    }

    /** A shopping mall in the middle of its car park. */
    void mall(int x, int y, int bw, int bh) {
        city.fill(x, y, bw, bh, LOT);
        int mw = Math.max(6, bw - 4), mh = Math.max(5, bh - 4);
        int mx = x + (bw - mw) / 2, my = y + (bh - mh) / 2;
        addFacilityLot(mx, my, mw, mh, MALL, 2 + city.rnd.nextInt(2));
        for (int j = y; j < y + bh; j++)
            for (int i = x; i < x + bw; i++)
                if (city.tiles[j * city.w + i] == LOT && (j - y) % 3 != 1 && city.rnd.nextFloat() < 0.3f && !city.hasNeighbor(i, j, CAR))
                    city.tiles[j * city.w + i] = CAR;
        city.openAreas.add(new float[]{(x + bw / 2f) * T, (y + bh / 2f) * T, 2});
    }

    /** A football stadium: stands on all four sides with gaps to walk in, the pitch in the middle. */
    void stadium(int x, int y, int bw, int bh) {
        city.fill(x, y, bw, bh, PLAZA);
        int t = 2, gap = 2;
        int mx = x + bw / 2 - 1, my = y + bh / 2 - 1;
        // Top and bottom stands (split by an entrance), then the ends.
        addFacilityLot(x, y, mx - x, t, STADIUM, 2);
        addFacilityLot(mx + gap, y, x + bw - mx - gap, t, STADIUM, 2);
        addFacilityLot(x, y + bh - t, mx - x, t, STADIUM, 2);
        addFacilityLot(mx + gap, y + bh - t, x + bw - mx - gap, t, STADIUM, 2);
        addFacilityLot(x, y + t, t, my - y - t, STADIUM, 2);
        addFacilityLot(x, my + gap, t, y + bh - t - my - gap, STADIUM, 2);
        addFacilityLot(x + bw - t, y + t, t, my - y - t, STADIUM, 2);
        addFacilityLot(x + bw - t, my + gap, t, y + bh - t - my - gap, STADIUM, 2);
        city.fill(x + t + 1, y + t + 1, bw - 2 * t - 2, bh - 2 * t - 2, GRASS);
        city.addDecor(D_FIELD, (x + t + 1) * T + 3, (y + t + 1) * T + 3, (x + bw - t - 1) * T - 3, (y + bh - t - 1) * T - 3, 0);
        city.addDecor(D_FLOODLIGHT, (x + t) * T, (y + t) * T, (x + bw - t) * T, (y + bh - t) * T, 0);
        city.openAreas.add(new float[]{(x + bw / 2f) * T, (y + bh / 2f) * T, 0});
    }

    /** A power station: turbine hall, chimneys, and a fenced yard of transformers. */
    void powerStation(int x, int y, int bw, int bh) {
        city.fill(x, y, bw, bh, LOT);
        boolean wide = bw >= bh;
        int hw = wide ? bw / 2 : bw - 2, hh = wide ? bh - 2 : bh / 2;
        addFacilityLot(x + 1, y + 1, hw, hh, POWER, 3);
        int yx = wide ? x + hw + 2 : x + 1, yy = wide ? y + 1 : y + hh + 2;
        int yw = wide ? bw - hw - 3 : bw - 2, yh = wide ? bh - 2 : bh - hh - 3;
        if (yw < 3 || yh < 3) return;
        for (int i = yx; i < yx + yw; i++) {
            city.tiles[yy * city.w + i] = FENCE;
            city.tiles[(yy + yh - 1) * city.w + i] = FENCE;
        }
        for (int j = yy; j < yy + yh; j++) {
            city.tiles[j * city.w + yx] = FENCE;
            city.tiles[j * city.w + yx + yw - 1] = FENCE;
        }
        city.addDecor(D_POWER, (yx + 1) * T, (yy + 1) * T, (yx + yw - 1) * T, (yy + yh - 1) * T, 0);
    }

    /** A building site: hoardings round the edge, a crane, foundations and piles of materials. */
    void constructionSite(int x, int y, int bw, int bh) {
        city.fill(x, y, bw, bh, LOT);
        for (int i = x; i < x + bw; i++) {
            if (Math.abs(i - (x + bw / 2)) > 1) city.tiles[y * city.w + i] = FENCE;
            city.tiles[(y + bh - 1) * city.w + i] = FENCE;
        }
        for (int j = y; j < y + bh; j++) {
            city.tiles[j * city.w + x] = FENCE;
            city.tiles[j * city.w + x + bw - 1] = FENCE;
        }
        city.addDecor(D_SITE, (x + 1) * T, (y + 1) * T, (x + bw - 1) * T, (y + bh - 1) * T, city.rnd.nextInt(4));
        // A half-built block in one corner.
        if (bw >= 16 && bh >= 16) addLot(x + bw - 8, y + bh - 8, 5, 5, OFFICE);
    }

    /**
     * Helipads for the police and the rescue service (10.20): the police helicopter beside the main police
     * station, the rescue helicopter beside the hospital (and another at the ranger station out in the wilds),
     * each on a clear patch of ground near the building.
     */
    void servicePads() {
        Facility hq = null, hospital = null;
        for (Facility f : city.facilities) {
            if (f.kind == FACILITY_POLICE && (hq == null || World.hypot(f.x - city.worldW() / 2, f.y - city.worldH() / 2)
                    < World.hypot(hq.x - city.worldW() / 2, hq.y - city.worldH() / 2))) hq = f;
            if (f.kind == FACILITY_HOSPITAL && hospital == null) hospital = f;
        }
        if (hq != null) addServicePad(hq.x, hq.y, PAD_POLICE);
        // ((31.0) Map version 51: a police helicopter at every third station besides, up to four in all.)
        if (city.cfg.civic >= 51) {
            int k = 0, extra = 0;
            for (Facility f : new java.util.ArrayList<Facility>(city.facilities)) {
                if (f.kind != FACILITY_POLICE || f == hq || f.fed) continue;
                if (k++ % 3 == 2 && extra < 3) {
                    addServicePad(f.x, f.y, PAD_POLICE);
                    extra++;
                }
            }
        }
        if (hospital != null) addServicePad(hospital.x, hospital.y, PAD_RESCUE);
        else if (hq != null) addServicePad(hq.x, hq.y, PAD_RESCUE);
        if (city.rangerLot != null) addServicePad(city.rangerLot[0], city.rangerLot[1], PAD_RESCUE);
        for (float[] o : city.outposts) addServicePad(o[0], o[1], PAD_RESCUE);
    }

    /** A pad on the nearest clear 4x4 tiles of open ground (car park, square or grass) to (x, y). */
    void addServicePad(float x, float y, int kind) {
        int cx = (int) (x / T), cy = (int) (y / T);
        for (int r = 2; r < 30; r++)
            for (int dy = -r; dy <= r; dy++)
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
                    int x0 = cx + dx - 2, y0 = cy + dy - 2;
                    if (!padFits(x0, y0)) continue;
                    for (int j = y0; j < y0 + 4 && !city.padsOnly; j++)
                        for (int i = x0; i < x0 + 4; i++) {
                            byte t = city.tiles[j * city.w + i];
                            if (t == CAR || t == TREE) city.tiles[j * city.w + i] = t == CAR ? LOT : GRASS;
                            city.solid[j * city.w + i] = false;
                        }
                    city.helipads.add(new float[]{(x0 + 2) * T, (y0 + 2) * T, kind});
                    return;
                }
    }

    boolean padFits(int x0, int y0) {
        if (x0 < 1 || y0 < 1 || x0 + 4 >= city.w || y0 + 4 >= city.h) return false;
        for (int j = y0; j < y0 + 4; j++)
            for (int i = x0; i < x0 + 4; i++) {
                byte t = city.tiles[j * city.w + i];
                if (t != LOT && t != PLAZA && t != GRASS && ((t != CAR && t != TREE) || city.padsOnly)) return false;
                if (city.buildingAt.get(j * city.w + i) >= 0) return false;
            }
        for (float[] p : city.helipads) if (World.hypot(p[0] - (x0 + 2) * T, p[1] - (y0 + 2) * T) < 70) return false;
        return true;
    }

    void park(int x, int y, int pw, int ph) {
        city.fill(x, y, pw, ph, GRASS);
        if (city.desert) {
            desertMap.desertPark(x, y, pw, ph);
            return;
        }
        int cx = x + pw / 2, cy = y + ph / 2;
        boolean organic = city.cfg.layout() == 2 || (city.cfg.layout() == 1 && city.rnd.nextBoolean());
        if (organic) {
            // Curved footpaths (drawn smoothly; the grass is walkable anyway).
            city.paths.add(new float[]{(x + pw / 2f) * T, (y + ph / 2f) * T, pw * T * 0.32f, ph * T * 0.3f, x * T, y * T, (x + pw) * T, (y + ph) * T});
        } else {
            for (int i = x; i < x + pw; i++) city.tiles[cy * city.w + i] = PLAZA;
            for (int j = y; j < y + ph; j++) city.tiles[j * city.w + cx] = PLAZA;
        }
        for (int j = y; j < y + ph; j++)
            for (int i = x; i < x + pw; i++)
                if (city.tiles[j * city.w + i] == GRASS && city.rnd.nextFloat() < 0.13f && !city.hasNeighbor(i, j, TREE))
                    city.tiles[j * city.w + i] = TREE;
        if (organic && city.cfg.civic >= 11) {
            // (Since 10.26) No trees standing in the middle of the footpaths.
            float pcx = x + pw / 2f, pcy = y + ph / 2f, rx = pw * 0.32f, ry = ph * 0.3f;
            for (int j = y; j < y + ph; j++)
                for (int i = x; i < x + pw; i++) {
                    if (city.tiles[j * city.w + i] != TREE) continue;
                    float dx = (i + 0.5f - pcx) / rx, dy = (j + 0.5f - pcy) / ry;
                    float ring = Math.abs((float) Math.sqrt(dx * dx + dy * dy) - 1) * Math.min(rx, ry);
                    boolean arm = (Math.abs(i + 0.5f - pcx) < 1 && Math.abs(dy) > 1) || (Math.abs(j + 0.5f - pcy) < 1 && Math.abs(dx) > 1);
                    if (ring < 1.2f || arm) city.tiles[j * city.w + i] = GRASS;
                }
        }
    }

    void plaza(int x, int y, int pw, int ph) {
        city.fill(x, y, pw, ph, PLAZA);
        if (pw >= 6 && ph >= 6) {
            int fx = x + pw / 2 - 1, fy = y + ph / 2 - 1;
            city.fill(fx, fy, 2, 2, STATUE);
            city.statues.add(new int[]{fx, fy});
        }
        int[][] corners = {{x + 1, y + 1}, {x + pw - 2, y + 1}, {x + 1, y + ph - 2}, {x + pw - 2, y + ph - 2}};
        for (int[] c : corners) if (city.tiles[c[1] * city.w + c[0]] == PLAZA) city.tiles[c[1] * city.w + c[0]] = TREE;
    }

    void parking(int x, int y, int pw, int ph) {
        city.fill(x, y, pw, ph, LOT);
        for (int j = y; j < y + ph; j++) {
            if ((j - y) % 3 != 1) continue;
            for (int i = x + 1; i < x + pw - 1; i++) if (city.rnd.nextFloat() < 0.55f) city.tiles[j * city.w + i] = CAR;
        }
    }
}
