package com.instantaxtion.zombiesandbox;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.City.*;

/**
 * ((28.0) Canyon Town: a red-rock town up on a mesa and down on the canyon floor below it. A cliff runs right across
 * the map between the two, and the only ways down it are a few switchback roads. A deep gorge winds out of the
 * cliff across the mesa to the edge of the map, splitting the town up there in two; only a few bridges cross it.
 * Below the cliff the gorge's creek carries on as a dry sandy wash. Buttes and rock spires stand about the town.
 * The ground, the rock and the lie of the land are this map's own.
 */
final class CanyonMap extends RegionMap {
    CanyonMap(City city) {
        super(city);
    }

    /** Which way the gorge runs (along y if vertical), and whether its "a" runs backwards across the map. */
    boolean vertical, flip;
    int L;
    /** Where the cliff is (its foot, along a, for each b) and how thick it is. */
    float[] aF;
    int CW;
    /** The gorge's middle and half-width (across, b) for each a. */
    float[] gc, ghw;
    /** What each tile is: 0 canyon floor, 1 mesa, 2 cliff, 3 gorge, 4 wash. */
    byte[] zone;
    /** The buttes and spires: {x, y, rx, ry, height} in tiles. */
    final List<float[]> rocks = new ArrayList<float[]>();
    /** The switchbacks' roads and the bridges, for the test. */
    int switchbacks, bridges, homes;

    int idx(int a, int b) {
        int A = flip ? L - 1 - a : a;
        return vertical ? A * city.w + b : b * city.w + A;
    }

    /** The a and b of tile i. */
    int aOf(int i) {
        int A = vertical ? i / city.w : i % city.w;
        return flip ? L - 1 - A : A;
    }

    int bOf(int i) {
        return vertical ? i % city.w : i / city.w;
    }

    static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    @Override
    boolean layStreets(int bx0, int by0, int bx1, int by1, int in) {
        gen.splitTop(bx0 + in, by0 + in, bx1 - in, by1 - in);
        L = city.w;
        Random rr = new Random(city.cfg.seed * 7331L + 24);
        vertical = rr.nextBoolean();
        flip = rr.nextBoolean();
        CW = Math.round(clamp(Math.round(L * 0.016f) + 10, L < 160 ? 11 : 12, 20));
        int legs = 1 + (CW - 6) / 5;
        int S = Math.round(clamp(L * 0.05f, 7, 14));
        byte dirA = (byte) (vertical ? 1 : 2), dirB = (byte) (vertical ? 2 : 1);
        int n = L * L;
        // Try a few lie-of-the-lands until the gorge has a street to bridge it and the cliff one to wind down.
        List<int[]> bridgeRuns = null, sites = null;
        for (int attempt = 0; attempt < 12; attempt++) {
            aF = new float[L];
            float base = L * (0.6f + rr.nextFloat() * 0.07f), A1 = L * 0.025f, A2 = L * 0.008f;
            float f1 = (float) (Math.PI * 2 / (L * (0.7f + rr.nextFloat() * 0.4f))), f2 = f1 * 2.7f, p1 = rr.nextFloat() * 6.3f, p2 = rr.nextFloat() * 6.3f;
            for (int b = 0; b < L; b++) aF[b] = base + A1 * (float) Math.sin(b * f1 + p1) + A2 * (float) Math.sin(b * f2 + p2);
            gc = new float[L];
            ghw = new float[L];
            float c0 = L * (0.4f + rr.nextFloat() * 0.2f), G1 = L * 0.06f, G2 = L * 0.02f;
            float g1 = (float) (Math.PI * 2 / (L * (0.6f + rr.nextFloat() * 0.4f))), g2 = g1 * 2.9f, q1 = rr.nextFloat() * 6.3f, q2 = rr.nextFloat() * 6.3f;
            float hw0 = clamp(L * 0.02f, 3, 9);
            for (int a = 0; a < L; a++) {
                gc[a] = clamp(c0 + G1 * (float) Math.sin(a * g1 + q1) + G2 * (float) Math.sin(a * g2 + q2), L * 0.25f, L * 0.75f);
                ghw[a] = hw0 + (float) Math.sin(a * g1 * 3 + q2);
            }
            // The bridges: where a straight street runs right across the gorge's course, up on the mesa.
            List<int[]> runs = new ArrayList<int[]>();
            int start = -1;
            for (int a = 0; a <= L; a++) {
                boolean ok = a < L && a > 3 && a < gorgeEnd(a) - CW - 3;
                if (ok) {
                    int e0 = Math.round(gc[a] - ghw[a]) - 1, e1 = Math.round(gc[a] + ghw[a]) + 1;
                    for (int b = e0; b <= e1 && ok; b++) {
                        int t = idx(a, b);
                        if (city.tiles[t] != ROAD || city.roadDir[t] != dirB) ok = false;
                    }
                }
                if (ok && start < 0) start = a;
                if (!ok && start >= 0) {
                    if (a - start >= 2 && a - start <= 6) runs.add(new int[]{start, a - start});
                    start = -1;
                }
            }
            // The switchbacks: where a straight street runs right down across the cliff, with room either side.
            List<int[]> cand = new ArrayList<int[]>();
            start = -1;
            for (int b = 0; b <= L; b++) {
                boolean ok = b < L;
                if (ok) {
                    int t0 = Math.round(aF[b]) - CW - 1, t1 = Math.round(aF[b]);
                    for (int a = t0; a <= t1 && ok; a++) {
                        if (a < 0 || a >= L) {
                            ok = false;
                            break;
                        }
                        int t = idx(a, b);
                        if (city.tiles[t] != ROAD || city.roadDir[t] != dirA) ok = false;
                    }
                }
                if (ok && start < 0) start = b;
                if (!ok && start >= 0) {
                    int wdt = b - start, bc = start + wdt / 2;
                    if (wdt >= 3 && wdt <= 6 && bc - S - 6 > 0 && bc + S + 6 < L) cand.add(new int[]{start, wdt});
                    start = -1;
                }
            }
            sites = new ArrayList<int[]>();
            int want = L >= 600 ? 3 : 2;
            java.util.Collections.shuffle(cand, rr);
            for (int[] s : cand) {
                if (sites.size() >= want) break;
                int bc = s[0] + s[1] / 2;
                float cg = gc[Math.max(0, Math.min(L - 1, Math.round(aF[bc]) - CW))];
                if (Math.abs(bc - cg) < S + ghw[0] + 8) continue;
                float aTop = -1e9f, aBot = 1e9f;
                for (int b = bc - S - 5; b <= bc + S + 5; b++) {
                    aTop = Math.max(aTop, aF[b] - CW);
                    aBot = Math.min(aF[b], aBot);
                }
                if (aBot - aTop < (legs - 1) * 5 + 4) continue;
                boolean clash = false;
                for (int[] o : sites) if (Math.abs(o[2] - bc) < 2 * S + 12) clash = true;
                // (One on each side of the gorge first.)
                for (int[] o : sites) if (sites.size() == 1 && (o[2] < cg) == (bc < cg) && cand.size() > 2) clash = true;
                if (clash) continue;
                sites.add(new int[]{s[0], s[1], bc, Math.round(aTop)});
            }
            java.util.Collections.shuffle(runs, rr);
            int bw = Math.max(2, Math.min(4, L / 200 + 1)), apart = Math.max(10, L / (bw + 2));
            bridgeRuns = new ArrayList<int[]>();
            for (int[] r : runs) {
                if (bridgeRuns.size() >= bw) break;
                boolean far = true;
                for (int[] o : bridgeRuns) if (Math.abs(o[0] - r[0]) < apart) far = false;
                if (far) bridgeRuns.add(r);
            }
            if (!bridgeRuns.isEmpty() && !sites.isEmpty()) break;
        }
        // The rock (cliff and gorge), the wash below, and what's kept as road through them.
        boolean[] gorge = new boolean[n], cliff = new boolean[n], wash = new boolean[n], keep = new boolean[n];
        zone = new byte[n];
        for (int a = 0; a < L; a++)
            for (int b = 0; b < L; b++) {
                int i = idx(a, b);
                float foot = aF[b];
                zone[i] = (byte) (a >= foot ? 0 : a >= foot - CW ? 2 : 1);
                if (zone[i] == 2) cliff[i] = true;
                float d = Math.abs(b + 0.5f - gc[a]);
                if (a < gorgeEnd(a)) {
                    if (d <= ghw[a]) {
                        gorge[i] = true;
                        zone[i] = 3;
                    }
                } else if (d <= Math.max(1.5f, ghw[a] * 0.55f)) {
                    wash[i] = true;
                    zone[i] = 4;
                }
            }
        if (city.bridge == null) city.bridge = new boolean[n];
        if (city.bridgeKind == null) city.bridgeKind = new byte[n];
        byte[] kinds = {BK_STEEL, BK_STONE, BK_CONCRETE, BK_STEEL};
        for (int k = 0; k < bridgeRuns.size(); k++) {
            int[] r = bridgeRuns.get(k);
            for (int a = r[0]; a < r[0] + r[1]; a++)
                for (int b = 0; b < L; b++) {
                    int i = idx(a, b);
                    if (!gorge[i]) continue;
                    keep[i] = true;
                    city.bridge[i] = true;
                    city.bridgeKind[i] = kinds[k % kinds.length];
                }
        }
        bridges = bridgeRuns.size();
        for (int[] s : sites)
            for (int b = s[0]; b < s[0] + s[1]; b++)
                for (int a = 0; a < L; a++) if (cliff[idx(a, b)]) keep[idx(a, b)] = true;
        // The blocks the rock runs through: cut back from it (what's left between, open ground).
        cutBlocks(gorge, vertical, rr);
        cutBlocks(cliff, !vertical, rr);
        cutBlocks(wash, vertical, rr);
        // The rock, the creek down the gorge, the sand of the wash.
        for (int i = 0; i < n; i++) {
            if ((gorge[i] || cliff[i]) && !keep[i]) {
                int a = aOf(i), b = bOf(i);
                boolean creek = gorge[i] && Math.abs(b + 0.5f - gc[a]) <= Math.max(0.8f, ghw[a] * 0.22f);
                clear(i, creek ? WATER : ROCK);
            } else if (wash[i] && city.tiles[i] != ROAD) clear(i, SAND);
        }
        // A road along the rim of the gorge and along the top and the foot of the cliff: the streets that ran on
        // into the rock end at it.
        rims();
        // The switchbacks: the straight street down the cliff made a road that winds down it, hairpin by hairpin.
        for (int[] s : sites) {
            int bc = s[2], aTop = s[3];
            float bm = bc + 0.5f;
            float aLast = aTop + 1.5f + (legs - 1) * 5;
            for (int b = s[0]; b < s[0] + s[1]; b++)
                for (int a = aTop + 4; a <= (int) aLast - 2; a++) clear(idx(a, b), ROCK);
            List<Float> pts = new ArrayList<Float>();
            addPt(pts, aTop - 1f, bm);
            int side = rr.nextBoolean() ? 1 : -1;
            float bNow = bm;
            for (int k = 0; k < legs; k++) {
                float al = aTop + 1.5f + k * 5;
                float E = k == legs - 1 ? bm : bm + side * S * (k % 2 == 0 ? 1 : -1);
                addPt(pts, al, bNow);
                addPt(pts, al, E);
                if (k < legs - 1) {
                    float dir = Math.signum(E - bNow);
                    for (int q = 1; q < 10; q++) {
                        double t = Math.PI * q / 10;
                        addPt(pts, al + 2.5f - 2.5f * (float) Math.cos(t), E + dir * 2.5f * (float) Math.sin(t));
                    }
                }
                bNow = E;
            }
            addPt(pts, aLast + 3f, bm);
            float[] line = new float[pts.size()];
            for (int k = 0; k < line.length; k += 2) {
                float A = flip ? L - pts.get(k) : pts.get(k);
                float B = pts.get(k + 1);
                line[k] = vertical ? B : A;
                line[k + 1] = vertical ? A : B;
            }
            landGen.curveRoad(line, false);
            switchbacks++;
        }
        dropRoadless(rr);
        buttes(rr);
        return true;
    }

    @Override
    public String toString() {
        return "bridges " + bridges + " switchbacks " + switchbacks + " rocks " + rocks.size() + " homes " + homes;
    }

    /**
     * The rim roads, smooth curves: along both rims of the gorge down to the top of the cliff, along the top of the
     * cliff either side of the gorge, and along its foot.
     */
    void rims() {
        for (int s = -1; s <= 1; s += 2) {
            List<Float> pts = new ArrayList<Float>();
            for (int a = 0; a < L; a += 3) {
                float b = gc[a] + s * (ghw[a] + 2.1f);
                int bi = Math.max(0, Math.min(L - 1, Math.round(b)));
                float top = aF[bi] - CW - 2.1f;
                if (a + 0.5f >= top) {
                    addPt(pts, top, b);
                    break;
                }
                addPt(pts, a + 0.5f, b);
            }
            lay(pts);
        }
        for (int s = -1; s <= 1; s += 2) {
            List<Float> pts = new ArrayList<Float>();
            for (int b = s < 0 ? 0 : L - 1; b >= 0 && b < L; b -= s * 3) {
                int a = Math.max(0, Math.min(L - 1, Math.round(aF[b] - CW - 2.1f)));
                float edge = gc[a] + s * (ghw[a] + 2.1f);
                if (s < 0 ? b + 0.5f >= edge : b + 0.5f <= edge) {
                    addPt(pts, aF[Math.max(0, Math.min(L - 1, Math.round(edge)))] - CW - 2.1f, edge);
                    break;
                }
                addPt(pts, aF[b] - CW - 2.1f, b + 0.5f);
            }
            lay(pts);
        }
        List<Float> foot = new ArrayList<Float>();
        for (int b = 0; b < L; b += 3) addPt(foot, aF[b] + 1.9f, b + 0.5f);
        addPt(foot, aF[L - 1] + 1.9f, L - 0.5f);
        lay(foot);
    }

    /** A smooth road along points {a, b, ...}. */
    void lay(List<Float> pts) {
        if (pts.size() < 4) return;
        float[] line = new float[pts.size()];
        for (int k = 0; k < line.length; k += 2) {
            float A = flip ? L - pts.get(k) : pts.get(k);
            float B = pts.get(k + 1);
            line[k] = Math.max(0.5f, Math.min(L - 0.5f, vertical ? B : A));
            line[k + 1] = Math.max(0.5f, Math.min(L - 0.5f, vertical ? A : B));
        }
        line = CityGenerator.chaikin(line);
        landGen.curveRoad(line, false);
    }

    void addPt(List<Float> pts, float a, float b) {
        pts.add(a);
        pts.add(b);
    }

    /** Where the gorge ends (the foot of the cliff, at its middle). */
    float gorgeEnd(int a) {
        int b = Math.max(0, Math.min(L - 1, Math.round(gc[a])));
        return aF[b];
    }

    /** A butte or a cluster of spires on a few blocks of the canyon floor and the mesa, standing in open ground. */
    void buttes(Random rr) {
        int want = L < 160 ? 0 : Math.max(1, Math.min(5, L / 180 + 1));
        List<int[]> cands = new ArrayList<int[]>();
        for (int[] b : city.blocks) {
            if (b.length > 4 || b[2] - b[0] < 14 || b[3] - b[1] < 14) continue;
            int cz = zone[((b[1] + b[3]) / 2) * city.w + (b[0] + b[2]) / 2];
            if (cz == 0 || cz == 1) cands.add(b);
        }
        java.util.Collections.shuffle(cands, rr);
        for (int k = 0; k < Math.min(want, cands.size()); k++) {
            int[] b = cands.get(k);
            city.blocks.remove(b);
            city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], SIDEWALK);
            city.fill(b[0] + 1, b[1] + 1, b[2] - b[0] - 2, b[3] - b[1] - 2, GRASS);
            float cx = (b[0] + b[2]) / 2f, cy = (b[1] + b[3]) / 2f, rx = (b[2] - b[0]) / 2f - 4, ry = (b[3] - b[1]) / 2f - 4;
            if (k % 2 == 0) {
                // A butte: steep sides, a flat top.
                rockBlob(cx, cy, rx * 0.8f, ry * 0.8f, 9 + rr.nextFloat() * 5, rr);
            } else {
                // Spires.
                int m = 3 + rr.nextInt(4);
                for (int s = 0; s < m; s++) {
                    float sx = cx + (rr.nextFloat() * 2 - 1) * rx * 0.8f, sy = cy + (rr.nextFloat() * 2 - 1) * ry * 0.8f;
                    rockBlob(sx, sy, 1.2f + rr.nextFloat() * 1.2f, 1.2f + rr.nextFloat() * 1.2f, 6 + rr.nextFloat() * 8, rr);
                }
            }
            for (int y = b[1] + 2; y < b[3] - 2; y++)
                for (int x = b[0] + 2; x < b[2] - 2; x++)
                    if (city.tiles[y * city.w + x] == GRASS && rr.nextFloat() < 0.04f && !city.hasNeighbor(x, y, ROCK)) city.tiles[y * city.w + x] = TREE;
            city.openAreas.add(new float[]{cx * T, (b[3] - 2) * T, 0});
        }
    }

    void rockBlob(float cx, float cy, float rx, float ry, float hgt, Random rr) {
        for (int y = (int) (cy - ry - 1); y <= (int) (cy + ry + 1); y++)
            for (int x = (int) (cx - rx - 1); x <= (int) (cx + rx + 1); x++) {
                if (x < 1 || y < 1 || x >= city.w - 1 || y >= city.h - 1) continue;
                float dx = (x + 0.5f - cx) / rx, dy = (y + 0.5f - cy) / ry;
                if (dx * dx + dy * dy > 1 || city.tiles[y * city.w + x] != GRASS) continue;
                city.tiles[y * city.w + x] = ROCK;
            }
        rocks.add(new float[]{cx, cy, rx, ry, hgt});
    }

    @Override
    void finish() {
        Random r = new Random(city.cfg.seed * 919L + 24);
        // Spires along the rim of the gorge and the foot of the cliff, where there's open ground.
        int want = Math.max(4, L / 40);
        for (int tries = 0; tries < want * 40 && want > 0; tries++) {
            int x = 3 + r.nextInt(city.w - 6), y = 3 + r.nextInt(city.h - 6);
            if (!open(x - 2, y - 2, 5, 5)) continue;
            boolean byRock = false;
            for (int dy = -7; dy <= 7; dy++) for (int dx = -7; dx <= 7; dx++) if (tile(x + dx, y + dy) == ROCK) byRock = true;
            if (!byRock) continue;
            rockBlob(x + 0.5f, y + 0.5f, 1.1f, 1.1f, 5 + r.nextFloat() * 7, r);
            want--;
        }
        // Adobe homes on the ground left open along the rim roads.
        homes = roadsideHomes(L / 6, r, new int[]{0xFFB8683E, 0xFFA05A3A, 0xFFC88A5A, 0xFF8A4A30}, new int[]{0xFFD8B088, 0xFFE0C49C, 0xFFC89870});
        // The town's parks watered green (an empty block of red sand looks like nothing at all).
        lawn = new boolean[city.w * city.h];
        for (int[] b : city.blocks) {
            int open = 0, built = 0, all = 0;
            for (int y = b[1] + 1; y < b[3] - 1; y++)
                for (int x = b[0] + 1; x < b[2] - 1; x++) {
                    byte t = city.tiles[y * city.w + x];
                    all++;
                    if (t == GRASS || t == TREE) open++;
                    if (t == BUILDING) built++;
                }
            if (built > 0 || open * 10 < all * 6) continue;
            for (int y = b[1] + 1; y < b[3] - 1; y++)
                for (int x = b[0] + 1; x < b[2] - 1; x++) lawn[y * city.w + x] = true;
        }
        // Junipers, scrub and the odd cactus for trees.
        for (int i = 0; i < city.w * city.h; i++) {
            if (city.tiles[i] != TREE) continue;
            float f = r.nextFloat();
            city.setTreeKind(i, f < 0.45f ? Country.TK_PINE : f < 0.85f ? Country.TK_SCRUB : Country.TK_CACTUS);
        }
        // The lie of the land: the mesa high, the cliff falling to the floor, the gorge deep.
        if (city.elev == null) return;
        float H = 22;
        for (int i = 0; i < city.w * city.h; i++) {
            int a = aOf(i), b = bOf(i);
            float e = city.elev.get(i) * 0.35f, foot = aF[b];
            if (a < foot - CW) e += H;
            else if (a < foot) e += H * (foot - a) / CW;
            if (zone[i] == 3 && (city.bridge == null || !city.bridge[i])) {
                float d = Math.abs(b + 0.5f - gc[a]) / Math.max(1, ghw[a]);
                float hereH = a < foot - CW ? H : H * Math.max(0, foot - a) / CW;
                e = Math.min(e, hereH - (hereH + 8) * (1 - d * d * d));
            }
            if (zone[i] == 4) e -= 1.5f;
            city.elev.set(i, Math.min(50, e));
        }
        for (float[] k : rocks)
            for (int y = (int) (k[1] - k[3] - 1); y <= (int) (k[1] + k[3] + 1); y++)
                for (int x = (int) (k[0] - k[2] - 1); x <= (int) (k[0] + k[2] + 1); x++) {
                    if (x < 0 || y < 0 || x >= city.w || y >= city.h || city.tiles[y * city.w + x] != ROCK) continue;
                    city.elev.set(y * city.w + x, Math.min(54, city.elev.get(y * city.w + x) + k[4]));
                }
    }

    // ------------------------------------------------------------------ painting

    boolean[] lawn;

    int zoneAt(int x, int y) {
        return zone == null ? 0 : zone[y * city.w + x];
    }

    @Override
    int ground(int x, int y) {
        float n = (float) (Math.sin(x * 0.047f + y * 0.019f) * Math.sin(y * 0.053f - x * 0.013f + 1.3f));
        int step = Math.round(n * 2);
        if (lawn != null && lawn[y * city.w + x]) return mix(0xFF6A7E3C, step > 0 ? 0xFF8A9A50 : 0xFF4A5E2A, Math.abs(step) * 0.2f);
        int z = zoneAt(x, y);
        int base = z == 1 || z == 2 ? 0xFFC8946A : z == 4 ? 0xFFD8B486 : 0xFFB8704A;
        return mix(base, step > 0 ? 0xFFE8C8A0 : 0xFF7A3A22, Math.abs(step) * 0.06f);
    }

    @Override
    int rock(int x, int y) {
        int z = zoneAt(x, y);
        float band = (float) Math.sin(y * 0.9f + x * 0.31f);
        int base = z == 3 ? 0xFF7A3C28 : z == 2 ? 0xFF9A4E32 : 0xFFA8583A;
        return mix(base, 0xFF5E2A1A, band > 0.6f ? 0.15f : 0);
    }

    @Override
    boolean texture(Canvas c, Paint p, byte t, int x, int y, float fx, float fy, Random r) {
        if ((t == GRASS || t == TREE) && lawn != null && lawn[y * city.w + x]) return false;
        if (t == GRASS || t == TREE || t == TRAIL) {
            // Red grit and pebbles, a dry tuft now and then.
            for (int k = 0; k < 4; k++) {
                p.setColor(r.nextBoolean() ? 0xFF9A5A38 : 0xFFDCAA80);
                if (city.fine) c.drawCircle(fx + r.nextFloat() * T, fy + r.nextFloat() * T, 0.8f, p);
            }
            if (r.nextFloat() < 0.08f) {
                p.setColor(0xFF8A7A48);
                float gx = fx + r.nextFloat() * T, gy = fy + r.nextFloat() * T;
                c.drawLine(gx, gy, gx - 1.5f, gy - 2, p);
                c.drawLine(gx, gy, gx + 1.5f, gy - 2, p);
            }
            if (r.nextFloat() < 0.05f) {
                float bx = fx + 3 + r.nextFloat() * (T - 6), by = fy + 3 + r.nextFloat() * (T - 6);
                p.setColor(0x40000000);
                c.drawCircle(bx + 0.6f, by + 0.6f, 1.7f, p);
                p.setColor(0xFF9A4A30);
                c.drawCircle(bx, by, 1.6f, p);
            }
            return true;
        }
        if (t == ROCK) {
            // Layered red rock: bands of strata, cracks, a lit boulder or two.
            int base = rock(x, y);
            for (int k = 0; k < 4; k++) {
                float sy = fy + k * T / 4f + (float) Math.sin(x * 0.7f + k) * 1.5f;
                p.setColor(k % 2 == 0 ? mix(base, 0xFFE0A070, 0.18f) : mix(base, 0xFF401808, 0.2f));
                c.drawRect(fx, sy, fx + T, sy + 1.2f, p);
            }
            if (r.nextFloat() < 0.5f) {
                float bx = fx + 2 + r.nextFloat() * (T - 4), by = fy + 2 + r.nextFloat() * (T - 4), br = 1.5f + r.nextFloat() * 2.5f;
                p.setColor(0x50000000);
                c.drawCircle(bx + 0.8f, by + 0.8f, br, p);
                p.setColor(mix(base, 0xFFE8B080, 0.3f));
                c.drawCircle(bx, by, br, p);
            }
            p.setColor(0xFF4A1C10);
            float cx0 = fx + r.nextFloat() * T, cy0 = fy + r.nextFloat() * T;
            c.drawLine(cx0, cy0, cx0 + r.nextFloat() * 6 - 3, cy0 + r.nextFloat() * 6 - 3, p);
            return true;
        }
        return false;
    }

    final RectF rf = new RectF();

    @Override
    void paintOver(Canvas c, Paint p, int X0, int Y0, int X1, int Y1) {
        // The buttes and spires standing up: a long shadow, then rings of strata up to a flat top.
        for (float[] k : rocks) {
            if (k[0] + k[2] + 8 < X0 || k[0] - k[2] - 2 > X1 || k[1] + k[3] + 8 < Y0 || k[1] - k[3] - 2 > Y1) continue;
            float cx = k[0] * T, cy = k[1] * T, rx = k[2] * T, ry = k[3] * T, hgt = k[4];
            float sh = hgt * 1.4f;
            p.setColor(0x38000000);
            for (int q = 0; q < 4; q++) {
                float o = sh * q / 3f;
                rf.set(cx - rx + o * 0.7f, cy - ry + o, cx + rx + o * 0.7f, cy + ry + o);
                c.drawOval(rf, p);
            }
            int layers = 5;
            for (int q = 0; q < layers; q++) {
                float f = 1 - q * 0.08f, lift = hgt * q / layers;
                p.setColor(q % 2 == 0 ? 0xFF8E4028 : 0xFFA8583A);
                rf.set(cx - rx * f - lift * 0.3f, cy - ry * f - lift * 0.5f, cx + rx * f - lift * 0.3f, cy + ry * f - lift * 0.5f);
                c.drawOval(rf, p);
            }
            float f = 1 - layers * 0.08f, lift = hgt;
            p.setColor(0xFFD08A5C);
            rf.set(cx - rx * f - lift * 0.3f, cy - ry * f - lift * 0.5f, cx + rx * f - lift * 0.3f, cy + ry * f - lift * 0.5f);
            c.drawOval(rf, p);
            p.setColor(0xFFE0A274);
            rf.set(cx - rx * f * 0.7f - lift * 0.3f - rx * 0.1f, cy - ry * f * 0.7f - lift * 0.5f - ry * 0.1f, cx + rx * f * 0.5f - lift * 0.3f, cy + ry * f * 0.5f - lift * 0.5f);
            c.drawOval(rf, p);
        }
        // The cliff's face darker the further down it, its shadow across the foot; parapets along the bridges.
        for (int y = Math.max(1, Y0); y < Math.min(city.h - 1, Y1); y++)
            for (int x = Math.max(1, X0); x < Math.min(city.w - 1, X1); x++) {
                int i = y * city.w + x, z = zone[i];
                float fx = x * T, fy = y * T;
                if (z == 2 || z == 0) {
                    float down = aOf(i) - (aF[bOf(i)] - CW);
                    float f = z == 2 ? down / CW * 0.45f : Math.max(0, 0.3f - (down - CW) * 0.08f);
                    if (f > 0.01f && city.tiles[i] != ROAD && (city.curveTile == null || !city.curveTile[i])) {
                        p.setColor((int) (f * 255) << 24 | 0x1A0804);
                        c.drawRect(fx, fy, fx + T, fy + T, p);
                    }
                }
                if (city.bridge != null && city.bridge[i] && z == 3) {
                    for (int k = 0; k < 4; k++) {
                        int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = y + (k == 2 ? 1 : k == 3 ? -1 : 0);
                        byte o = city.tiles[ny * city.w + nx];
                        if (o == ROAD) continue;
                        p.setColor(0x60000000);
                        if (k == 0) c.drawRect(fx + T, fy + 3, fx + T + 5, fy + T + 3, p);
                        else if (k == 1) c.drawRect(fx - 2, fy + 3, fx, fy + T + 3, p);
                        else if (k == 2) c.drawRect(fx + 3, fy + T, fx + T + 3, fy + T + 6, p);
                        p.setColor(0xFFC8B8A4);
                        if (k == 0) c.drawRect(fx + T - 2, fy, fx + T, fy + T, p);
                        else if (k == 1) c.drawRect(fx, fy, fx + 2, fy + T, p);
                        else if (k == 2) c.drawRect(fx, fy + T - 2, fx + T, fy + T, p);
                        else c.drawRect(fx, fy, fx + T, fy + 2, p);
                    }
                }
            }
        // The rim of the mesa: a lit edge where the cliff drops away.
        p.setColor(0x60F8D8B0);
        for (int y = Math.max(1, Y0); y < Math.min(city.h - 1, Y1); y++)
            for (int x = Math.max(1, X0); x < Math.min(city.w - 1, X1); x++) {
                int z = zoneAt(x, y);
                if (z != 1 || city.tiles[y * city.w + x] == ROAD) continue;
                for (int k = 0; k < 4; k++) {
                    int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = y + (k == 2 ? 1 : k == 3 ? -1 : 0);
                    if (city.tiles[ny * city.w + nx] != ROCK) continue;
                    float fx = x * T, fy = y * T;
                    if (k == 0) c.drawRect(fx + T - 2, fy, fx + T, fy + T, p);
                    else if (k == 1) c.drawRect(fx, fy, fx + 2, fy + T, p);
                    else if (k == 2) c.drawRect(fx, fy + T - 2, fx + T, fy + T, p);
                    else c.drawRect(fx, fy, fx + T, fy + 2, p);
                }
            }
    }
}
