package com.instantaxtion.zombiesandbox;

import java.util.Arrays;

/**
 * (10.36) Walking routes for everyone on foot, the living and the dead: hierarchical pathfinding.
 *
 * The map is cut into 16x16-tile clusters. Where two clusters meet, each stretch of open ground across the border
 * is a gateway (a pair of tiles, one either side). Within a cluster, how far it is between each pair of its
 * gateways is worked out the first time it's needed. A long trip is planned across the clusters' gateways first
 * (a few hundred steps however far it is, instead of tens of thousands of tiles), then filled in tile by tile one
 * cluster at a time, and finally straightened: the corners cut wherever there's a clear, no worse way, so people
 * walk in lines, not staircases, and keep a step back from walls.
 *
 * Costs are the city's walking costs (pavements, crossings, the road only where they must), and a tile right up
 * against a wall or a building costs a little more, so routes don't scrape round corners. When the map changes
 * (a barricade, a wall going up, a building coming down) the clusters it touches are worked out again.
 */
final class Nav {
    /** ((13.6) For the tests: plan every trip from scratch, without the searches kept from busy goals.) */
    static boolean plainSearch;
    static final int CS = 16;
    private static final int FAR = Integer.MAX_VALUE / 4;

    private final City c;
    private final int w, h, cw, ch;

    // Gateways: a tile each, the cluster it's in, its partner across the border and the cost of the step over.
    private int nodes;
    private int[] nodeTile = new int[1024], nodeCluster = new int[1024], nodePartner = new int[1024], nodeCross = new int[1024];
    // ((13.6) Each gateway's tile x and y, and where it is in its cluster's list, so a search doesn't keep dividing
    // and looking them up; and once its cluster is worked out, the gateways it can reach across it and the cost of
    // each: the long trips spent most of their time going through every pair, most of them out of reach.)
    private int[] nodeX = new int[1024], nodeY = new int[1024], nodeSlot = new int[1024];
    private int[][] adjTo, adjCost, revTo, revCost;
    private int[][] clusterNodes;
    private int[] clusterCount;
    /** Within each cluster, the cost from each of its gateways to each other one (FAR if there's no way). */
    private int[][] intra;
    private boolean[] intraReady;
    private boolean entrancesDirty = true;

    Nav(City c) {
        this.c = c;
        this.w = c.w;
        this.h = c.h;
        cw = (w + CS - 1) / CS;
        ch = (h + CS - 1) / CS;
    }

    /** A tile's walkability changed: the cluster it's in (and, on a border, the gateways) need doing again. */
    void touch(int i) {
        if (intraReady == null) return;
        int x = i % w, y = i / w, k = (y / CS) * cw + x / CS;
        intraReady[k] = false;
        dropTrees();
        int lx = x % CS, ly = y % CS;
        if (lx == 0 || ly == 0 || lx == CS - 1 || ly == CS - 1) entrancesDirty = true;
    }

    private boolean open(int i) {
        return !c.solid[i] || c.gateTile(i);
    }

    /** What stepping onto tile i costs (x10, before the diagonal's extra), a little more right beside a wall. */
    private int cost(int i) {
        return cost(i, i % w, i / w);
    }

    /** As above, for tile i at (x, y), already known. */
    private int cost(int i, int x, int y) {
        int base = c.walkCost[i] * 10;
        if ((x > 0 && c.solid[i - 1]) || (x < w - 1 && c.solid[i + 1]) || (y > 0 && c.solid[i - w]) || (y < h - 1 && c.solid[i + w]))
            base += 6;
        return base;
    }

    // ------------------------------------------------------------------ the gateways

    private void buildEntrances() {
        // (10.49) The clusters already worked out keep it if their gateways are just where they were: one wall
        // going up on a border used to throw away every cluster on the map, and the next long trip stalled the game
        // working them all out again.
        int[][] oldNodes = clusterNodes, oldIntra = intra;
        boolean[] oldReady = intraReady;
        int[] oldTile = oldNodes == null ? null : Arrays.copyOf(nodeTile, nodes);
        nodes = 0;
        clusterCount = new int[cw * ch];
        clusterNodes = new int[cw * ch][];
        intra = new int[cw * ch][];
        intraReady = new boolean[cw * ch];
        adjTo = new int[nodeTile.length][];
        adjCost = new int[nodeTile.length][];
        revTo = new int[nodeTile.length][];
        revCost = new int[nodeTile.length][];
        for (int cy = 0; cy < ch; cy++)
            for (int cx = 0; cx < cw; cx++) {
                // The border on the right, then the one below.
                if (cx + 1 < cw) {
                    int x = (cx + 1) * CS - 1;
                    scanBorder(x, cy * CS, Math.min(h, (cy + 1) * CS), true);
                }
                if (cy + 1 < ch) {
                    int y = (cy + 1) * CS - 1;
                    scanBorder(y, cx * CS, Math.min(w, (cx + 1) * CS), false);
                }
            }
        // Each cluster's own list of its gateways.
        for (int k = 0; k < cw * ch; k++) clusterNodes[k] = new int[clusterCount[k]];
        int[] fill = new int[cw * ch];
        if (adjTo.length < nodes) {
            adjTo = new int[nodes][];
            adjCost = new int[nodes][];
            revTo = new int[nodes][];
            revCost = new int[nodes][];
        }
        for (int n = 0; n < nodes; n++) {
            int k = nodeCluster[n];
            nodeSlot[n] = fill[k];
            clusterNodes[k][fill[k]++] = n;
        }
        if (oldNodes != null)
            for (int k = 0; k < cw * ch; k++) {
                int[] was = oldNodes[k], now = clusterNodes[k];
                if (!oldReady[k] || was.length != now.length) continue;
                boolean same = true;
                for (int i = 0; i < now.length && same; i++) same = oldTile[was[i]] == nodeTile[now[i]];
                if (!same) continue;
                intra[k] = oldIntra[k];
                intraReady[k] = true;
                linkCluster(k);
            }
        entrancesDirty = false;
        warmAt = 0;
        dropTrees();
    }

    private int warmAt;

    /**
     * (10.49) A few more clusters worked out ahead of need (n of them at most), a little each moment, so a long
     * trip doesn't have to do dozens at once.
     */
    void warm(int n) {
        if (entrancesDirty || clusterNodes == null) buildEntrances();
        int all = cw * ch;
        for (int looked = 0; n > 0 && looked < all; looked++) {
            int k = warmAt;
            warmAt = (warmAt + 1) % all;
            if (intraReady[k]) continue;
            ensureIntra(k);
            n--;
        }
    }

    /** Along one border (at column or row `at`, from a0 to a1), a gateway in each stretch open on both sides. */
    private void scanBorder(int at, int a0, int a1, boolean vertical) {
        int run = -1;
        for (int a = a0; a <= a1; a++) {
            boolean ok = false;
            if (a < a1) {
                int p = vertical ? a * w + at : at * w + a, q = vertical ? p + 1 : p + w;
                ok = open(p) && open(q);
            }
            if (ok && run < 0) run = a;
            if (!ok && run >= 0) {
                int len = a - run;
                if (len <= 6) gateway(at, run + len / 2, vertical);
                else {
                    gateway(at, run + 1, vertical);
                    gateway(at, a - 2, vertical);
                }
                run = -1;
            }
        }
    }

    private void gateway(int at, int a, boolean vertical) {
        int p = vertical ? a * w + at : at * w + a, q = vertical ? p + 1 : p + w;
        int np = addNode(p), nq = addNode(q);
        nodePartner[np] = nq;
        nodePartner[nq] = np;
        nodeCross[np] = cost(q);
        nodeCross[nq] = cost(p);
    }

    private int addNode(int tile) {
        if (nodes == nodeTile.length) {
            int n = nodes * 2;
            nodeTile = Arrays.copyOf(nodeTile, n);
            nodeCluster = Arrays.copyOf(nodeCluster, n);
            nodePartner = Arrays.copyOf(nodePartner, n);
            nodeCross = Arrays.copyOf(nodeCross, n);
            nodeX = Arrays.copyOf(nodeX, n);
            nodeY = Arrays.copyOf(nodeY, n);
            nodeSlot = Arrays.copyOf(nodeSlot, n);
        }
        int x = tile % w, y = tile / w, k = (y / CS) * cw + x / CS;
        nodeTile[nodes] = tile;
        nodeX[nodes] = x;
        nodeY[nodes] = y;
        nodeCluster[nodes] = k;
        clusterCount[k]++;
        return nodes++;
    }

    /** The costs between each pair of a cluster's gateways, worked out within the cluster. */
    private void ensureIntra(int k) {
        if (intraReady[k]) return;
        int[] list = clusterNodes[k];
        int n = list.length;
        int[] m = new int[n * n];
        Arrays.fill(m, FAR);
        int x0 = (k % cw) * CS, y0 = (k / cw) * CS, x1 = Math.min(w, x0 + CS), y1 = Math.min(h, y0 + CS);
        box(x0, y0, x1, y1);
        for (int a = 0; a < n; a++) {
            int[] d = local(nodeTile[list[a]], x0, y0, x1, y1);
            for (int b = 0; b < n; b++) {
                int t = nodeTile[list[b]];
                m[a * n + b] = d[(t / w - y0) * CS + (t % w - x0)];
            }
        }
        intra[k] = m;
        intraReady[k] = true;
        linkCluster(k);
    }

    /** Each of cluster k's gateways' reachable others across it, and the cost to each, from its worked-out costs. */
    private void linkCluster(int k) {
        int[] list = clusterNodes[k], m = intra[k];
        int n = list.length;
        for (int a = 0; a < n; a++) {
            int count = 0;
            for (int b = 0; b < n; b++) if (b != a && m[a * n + b] < FAR) count++;
            int[] to = new int[count], cost = new int[count];
            count = 0;
            for (int b = 0; b < n; b++)
                if (b != a && m[a * n + b] < FAR) {
                    to[count] = list[b];
                    cost[count++] = m[a * n + b];
                }
            adjTo[list[a]] = to;
            adjCost[list[a]] = cost;
            // (And the other way: the ones that can reach it, for the searches back from a goal.)
            count = 0;
            for (int b = 0; b < n; b++) if (b != a && m[b * n + a] < FAR) count++;
            to = new int[count];
            cost = new int[count];
            count = 0;
            for (int b = 0; b < n; b++)
                if (b != a && m[b * n + a] < FAR) {
                    to[count] = list[b];
                    cost[count++] = m[b * n + a];
                }
            revTo[list[a]] = to;
            revCost[list[a]] = cost;
        }
    }

    // Small searches inside one cluster (or a box of them).
    private int[] lDist = new int[CS * CS];
    private long[] lHeap = new long[256];
    /**
     * ((13.6) The cluster's tiles, open or not and what each costs, looked up once for all the searches from its
     * gateways (one each) instead of in every one of them, eight times a tile.)
     */
    private final boolean[] lOpen = new boolean[CS * CS];
    private final int[] lCost = new int[CS * CS];

    /** Fills in lOpen and lCost for the box (one cluster), for local to use. */
    private void box(int x0, int y0, int x1, int y1) {
        for (int y = y0; y < y1; y++)
            for (int x = x0; x < x1; x++) {
                int i = y * w + x, li = (y - y0) * CS + (x - x0);
                lOpen[li] = open(i);
                lCost[li] = lOpen[li] ? cost(i, x, y) : 0;
            }
    }

    /**
     * Costs from tile s to every tile of the box (x0..x1, y0..y1: one cluster), FAR where it can't get; box() must
     * have been called for it first.
     */
    private int[] local(int s, int x0, int y0, int x1, int y1) {
        int bw = CS;
        Arrays.fill(lDist, FAR);
        int size = 0;
        int si = (s / w - y0) * bw + (s % w - x0);
        lDist[si] = 0;
        lHeap[size++] = si;
        while (size > 0) {
            long top = lHeap[0];
            lHeap[0] = lHeap[--size];
            down(lHeap, size);
            int li = (int) (top & 0xFFFFFFFFL), g = (int) (top >>> 32);
            if (g > lDist[li]) continue;
            int lx = li % bw, ly = li / bw, tx = x0 + lx, ty = y0 + ly;
            for (int q = 0; q < 8; q++) {
                int dx = DX[q], dy = DY[q], nx = tx + dx, ny = ty + dy;
                if (nx < x0 || ny < y0 || nx >= x1 || ny >= y1) continue;
                int nli = li + dy * bw + dx;
                if (!lOpen[nli]) continue;
                if (dx != 0 && dy != 0 && (!lOpen[li + dx] || !lOpen[li + dy * bw])) continue;
                int ng = g + lCost[nli] * (dx != 0 && dy != 0 ? 14 : 10) / 10;
                if (ng >= lDist[nli]) continue;
                lDist[nli] = ng;
                if (size == lHeap.length) lHeap = Arrays.copyOf(lHeap, size * 2);
                lHeap[size] = ((long) ng << 32) | nli;
                up(lHeap, size++);
            }
        }
        return lDist;
    }

    private static final int[] DX = {1, -1, 0, 0, 1, -1, 1, -1}, DY = {0, 0, 1, -1, 1, 1, -1, -1};

    // ------------------------------------------------------------------ tile A*, in a box

    private int[] aG, aStamp;
    /** ((16.4) Which way each tile was reached from, as one of the eight directions: a byte, not a whole tile index. */
    private byte[] aFrom;
    private long[] aHeap = new long[4096];
    private int aRun;

    /** A tile route from s to g (start excluded) inside the box, or null within maxExpand tiles searched. */
    private int[] astar(int s, int g, int x0, int y0, int x1, int y1, int maxExpand) {
        // ((16.10) Its working arrays cover only the box searched, not the whole map: two whole-map arrays and a third
        // were 25 MB on the Large map, for searches that never leave a few clusters (see path()). Each tile's place
        // in them is worked out from where it is in the box.)
        // (The start and the goal always inside it.)
        x0 = Math.min(x0, Math.min(s % w, g % w));
        y0 = Math.min(y0, Math.min(s / w, g / w));
        x1 = Math.max(x1, Math.max(s % w, g % w) + 1);
        y1 = Math.max(y1, Math.max(s / w, g / w) + 1);
        int bw = x1 - x0, bh = y1 - y0, area = bw * bh;
        if (aG == null || aG.length < area) {
            aG = new int[area];
            aFrom = new byte[area];
            aStamp = new int[area];
        }
        int run = ++aRun;
        if (s == g) return new int[0];
        int gx = g % w, gy = g / w, size = 0;
        int ls = (s / w - y0) * bw + (s % w - x0);
        aStamp[ls] = run;
        aG[ls] = 0;
        aHeap[size++] = ((long) heur(s % w, s / w, gx, gy) << 32) | s;
        int expanded = 0;
        while (size > 0) {
            long top = aHeap[0];
            aHeap[0] = aHeap[--size];
            down(aHeap, size);
            int t = (int) (top & 0xFFFFFFFFL), f = (int) (top >>> 32), tx = t % w, ty = t / w;
            int lt = (ty - y0) * bw + (tx - x0);
            if (f - heur(tx, ty, gx, gy) > aG[lt]) continue;
            if (t == g) {
                int n = 0;
                for (int k = t; k != s; ) {
                    int q = aFrom[(k / w - y0) * bw + (k % w - x0)];
                    k -= DX[q] + DY[q] * w;
                    n++;
                }
                int[] path = new int[n];
                for (int k = t, i = n - 1; k != s; ) {
                    path[i--] = k;
                    int q = aFrom[(k / w - y0) * bw + (k % w - x0)];
                    k -= DX[q] + DY[q] * w;
                }
                return path;
            }
            if (++expanded > maxExpand) return null;
            for (int q = 0; q < 8; q++) {
                int dx = DX[q], dy = DY[q], nx = tx + dx, ny = ty + dy;
                if (nx < x0 || ny < y0 || nx >= x1 || ny >= y1) continue;
                int nb = ny * w + nx, ln = (ny - y0) * bw + (nx - x0);
                if (!open(nb) && nb != g) continue;
                if (dx != 0 && dy != 0 && (!open(ty * w + nx) || !open(ny * w + tx))) continue;
                int ng = aG[lt] + cost(nb, nx, ny) * (dx != 0 && dy != 0 ? 14 : 10) / 10;
                if (aStamp[ln] == run && ng >= aG[ln]) continue;
                aStamp[ln] = run;
                aG[ln] = ng;
                aFrom[ln] = (byte) q;
                if (size == aHeap.length) aHeap = Arrays.copyOf(aHeap, size * 2);
                aHeap[size] = ((long) (ng + heur(nx, ny, gx, gy)) << 32) | nb;
                up(aHeap, size++);
            }
        }
        return null;
    }

    private static int heur(int x, int y, int gx, int gy) {
        int dx = Math.abs(x - gx), dy = Math.abs(y - gy);
        return 10 * Math.max(dx, dy) + 4 * Math.min(dx, dy);
    }

    // ------------------------------------------------------------------ the whole route

    /**
     * A walking route from (sx, sy) to (tx, ty): the tiles to head for in turn (the corners of a straightened
     * route; the last one the destination), or null if there's no way there.
     */
    int[] path(float sx, float sy, float tx, float ty) {
        int s = c.tileIndex(sx, sy), g = c.tileIndex(tx, ty);
        if (s < 0 || g < 0) return null;
        if (s == g) return new int[]{g};
        int x0 = s % w, y0 = s / w, gx = g % w, gy = g / w;
        int far = Math.max(Math.abs(x0 - gx), Math.abs(y0 - gy));
        int[] tiles = null;
        // Close by: straight to it, in a box round the two of them.
        if (far <= CS * 2) {
            int m = CS;
            tiles = astar(s, g, Math.max(0, Math.min(x0, gx) - m), Math.max(0, Math.min(y0, gy) - m),
                    Math.min(w, Math.max(x0, gx) + m + 1), Math.min(h, Math.max(y0, gy) + m + 1), 6000);
        }
        if (tiles == null) tiles = hierarchical(s, g);
        // (Gave up on the clusters: a long plain search, as a last resort.)
        // ((16.10) In a box round the two of them, a long way out on every side, not the whole map: no search that stops
        // at thirty thousand tiles gets further than that.)
        if (tiles == null && far <= CS * 6) {
            int m = 160;
            tiles = astar(s, g, Math.max(0, Math.min(x0, gx) - m), Math.max(0, Math.min(y0, gy) - m),
                    Math.min(w, Math.max(x0, gx) + m + 1), Math.min(h, Math.max(y0, gy) + m + 1), 30000);
        }
        if (tiles == null) return null;
        return straighten(s, tiles);
    }

    private int[] hG, hFrom, hStamp, hDone;
    private int hRun, hSize, goalX, goalY;
    private long[] hHeap = new long[1024];

    private int[] hierarchical(int s, int g) {
        if (entrancesDirty || clusterNodes == null) buildEntrances();
        int ks = clusterOf(s), kg = clusterOf(g);
        GoalTree tree = plainSearch ? null : treeFor(g);
        if (tree != null) return fromTree(tree, s, ks);
        ensureIntra(ks);
        ensureIntra(kg);
        // From the start to each gateway of its cluster, and from each of the goal's cluster's to the goal.
        int[] toStart = costsInCluster(s, ks), toGoal = costsInCluster(g, kg);
        if (hG == null || hG.length < nodes + 2) {
            int n = Math.max(nodes + 2, 1024) * 2;
            hG = new int[n];
            hFrom = new int[n];
            hStamp = new int[n];
            hDone = new int[n];
        }
        int run = ++hRun;
        hSize = 0;
        int START = nodes, GOAL = nodes + 1;
        goalX = g % w;
        goalY = g / w;
        hStamp[START] = run;
        hG[START] = 0;
        hFrom[START] = -1;
        int[] ls = clusterNodes[ks];
        for (int a = 0; a < ls.length; a++) if (toStart[a] < FAR) relax(START, ls[a], toStart[a], run);
        int expanded = 0;
        while (hSize > 0) {
            long top = hHeap[0];
            hHeap[0] = hHeap[--hSize];
            down(hHeap, hSize);
            int u = (int) (top & 0xFFFFFFFFL);
            if (u == GOAL) break;
            // (Already done by a cheaper way: the heap keeps the dearer entries it was pushed with before.)
            if (hDone[u] == run) continue;
            hDone[u] = run;
            if (++expanded > 60000) return null;
            int k = nodeCluster[u], gu = hG[u];
            // Over the border.
            relax(u, nodePartner[u], gu + nodeCross[u], run);
            // Across its own cluster.
            ensureIntra(k);
            int[] to = adjTo[u], co = adjCost[u];
            for (int b = 0; b < to.length; b++) relax(u, to[b], gu + co[b], run);
            // And on to the goal itself.
            if (k == kg) {
                int d = toGoal[nodeSlot[u]];
                if (d < FAR) relax(u, GOAL, gu + d, run);
            }
        }
        if (hStamp[GOAL] != run) return null;
        // The gateways in order, then filled in tile by tile.
        int count = 0;
        for (int n = hFrom[GOAL]; n != START; n = hFrom[n]) count++;
        int[] seq = new int[count];
        for (int n = hFrom[GOAL], i = count - 1; n != START; n = hFrom[n]) seq[i--] = n;
        return fill(s, g, seq, count, null);
    }

    /**
     * The tiles from s through the gateways seq[0..count) to g, filled in one cluster at a time; by a kept search's
     * stretches from each gateway on to the next, when the route comes from one (the same for everyone after).
     */
    private int[] fill(int s, int g, int[] seq, int count, GoalTree tree) {
        IntList out = new IntList();
        int at = s;
        for (int i = 0; i <= count; i++) {
            int next = i < count ? nodeTile[seq[i]] : g;
            if (next == at) continue;
            // A step over a border is one tile; anything else is a walk inside one cluster.
            int ax = at % w, ay = at / w, bx = next % w, by = next / w;
            if (Math.abs(ax - bx) + Math.abs(ay - by) == 1 && clusterOf(at) != clusterOf(next)) {
                out.add(next);
            } else {
                int k = clusterOf(next);
                int cx0 = (k % cw) * CS, cy0 = (k / cw) * CS;
                int[] part = tree != null && i > 0 ? tree.seg[seq[i - 1]] : null;
                if (part == null) {
                    part = astar(at, next, Math.max(0, cx0 - 1), Math.max(0, cy0 - 1), Math.min(w, cx0 + CS + 1), Math.min(h, cy0 + CS + 1), 3000);
                    if (part != null && tree != null && i > 0) tree.seg[seq[i - 1]] = part;
                }
                if (part == null) {
                    // The map's changed since: work the cluster out again next time, and fall back for now.
                    intraReady[k] = false;
                    dropTrees();
                    return null;
                }
                for (int t : part) out.add(t);
            }
            at = next;
        }
        return out.toArray();
    }

    // ------------------------------------------------------------------ searches back from a busy goal

    /**
     * ((13.6) Where crowds head for one place (everyone called to an outbreak, a rally point, a cordon) the same
     * long trip across the map was planned from scratch for each of them, a few thousand gateways looked at every
     * time. Now once a goal's been asked for twice lately, the search is run backwards from it instead and kept: each
     * new trip there only carries it on as far as its own start, and most find it already there. The routes are
     * just as short (it's the same search, the other way round), and the map changing throws them all away.)
     */
    private static final class GoalTree {
        int goal, used;
        int[] dist, next;
        boolean[] done;
        int[][] seg;
        long[] heap = new long[1024];
        int size;
    }

    private static final int TREES = 4;
    private final GoalTree[] trees = new GoalTree[TREES];
    private final int[] recentGoals = new int[48];
    private int recentAt, treeClock;

    private void dropTrees() {
        for (int i = 0; i < TREES; i++) trees[i] = null;
    }

    /** The kept search back from goal g, started now if it's been a goal lately; null to plan the usual way. */
    private GoalTree treeFor(int g) {
        treeClock++;
        for (GoalTree t : trees)
            if (t != null && t.goal == g) {
                t.used = treeClock;
                return t;
            }
        boolean again = false;
        for (int r : recentGoals) if (r == g + 1) again = true;
        recentGoals[recentAt] = g + 1;
        recentAt = (recentAt + 1) % recentGoals.length;
        if (!again) return null;
        int slot = 0;
        for (int i = 0; i < TREES; i++) {
            if (trees[i] == null) {
                slot = i;
                break;
            }
            if (trees[i].used < trees[slot].used) slot = i;
        }
        // ((22.1) The one it replaces lends its arrays, rather than new ones the size of the whole network each time:
        // in an outbreak that was most of the memory the game used and threw away.)
        GoalTree old = trees[slot];
        GoalTree t = old != null && old.dist.length == nodes ? old : new GoalTree();
        t.goal = g;
        t.used = treeClock;
        t.size = 0;
        if (t == old) {
            Arrays.fill(t.next, 0);
            Arrays.fill(t.done, false);
            Arrays.fill(t.seg, null);
        } else {
            t.dist = new int[nodes];
            t.next = new int[nodes];
            t.done = new boolean[nodes];
            t.seg = new int[nodes][];
        }
        Arrays.fill(t.dist, FAR);
        int kg = clusterOf(g);
        ensureIntra(kg);
        int[] toGoal = costsInCluster(g, kg), lg = clusterNodes[kg];
        for (int a = 0; a < lg.length; a++) if (toGoal[a] < FAR) treeRelax(t, lg[a], toGoal[a], -1);
        trees[slot] = t;
        return t;
    }

    private static void treeRelax(GoalTree t, int n, int d, int via) {
        if (t.done[n] || d >= t.dist[n]) return;
        t.dist[n] = d;
        t.next[n] = via;
        if (t.size == t.heap.length) t.heap = Arrays.copyOf(t.heap, t.size * 2);
        t.heap[t.size] = ((long) d << 32) | n;
        up(t.heap, t.size++);
    }

    /** The route from s (in cluster ks) by the kept search back from its goal, carried on as far as s needs. */
    private int[] fromTree(GoalTree t, int s, int ks) {
        ensureIntra(ks);
        int[] toStart = costsInCluster(s, ks), ls = clusterNodes[ks];
        int best = FAR, bestNode = -1;
        for (int a = 0; a < ls.length; a++) {
            int n = ls[a];
            if (t.done[n] && toStart[a] < FAR && toStart[a] + t.dist[n] < best) {
                best = toStart[a] + t.dist[n];
                bestNode = n;
            }
        }
        int expanded = 0;
        while (t.size > 0 && (int) (t.heap[0] >>> 32) < best) {
            long top = t.heap[0];
            t.heap[0] = t.heap[--t.size];
            down(t.heap, t.size);
            int v = (int) (top & 0xFFFFFFFFL), d = (int) (top >>> 32);
            if (t.done[v] || d > t.dist[v]) continue;
            t.done[v] = true;
            if (++expanded > 60000) return null;
            int k = nodeCluster[v];
            if (k == ks) {
                int a = nodeSlot[v];
                if (toStart[a] < FAR && toStart[a] + d < best) {
                    best = toStart[a] + d;
                    bestNode = v;
                }
            }
            // Back over the border, and back across its own cluster.
            int p = nodePartner[v];
            treeRelax(t, p, d + nodeCross[p], v);
            ensureIntra(k);
            int[] from = revTo[v], co = revCost[v];
            for (int b = 0; b < from.length; b++) treeRelax(t, from[b], d + co[b], v);
        }
        if (bestNode < 0) return null;
        int count = 0;
        for (int n = bestNode; n >= 0; n = t.next[n]) count++;
        int[] seq = new int[count];
        count = 0;
        for (int n = bestNode; n >= 0; n = t.next[n]) seq[count++] = n;
        return fill(s, t.goal, seq, count, t);
    }

    /** The estimate on from gateway n (or the goal, nothing) to the goal. */
    private int h(int n) {
        if (n >= nodes) return 0;
        return heur(nodeX[n], nodeY[n], goalX, goalY);
    }

    private void relax(int from, int to, int g, int run) {
        if (hStamp[to] == run && (g >= hG[to] || hDone[to] == run)) return;
        hStamp[to] = run;
        hG[to] = g;
        hFrom[to] = from;
        if (hSize == hHeap.length) hHeap = Arrays.copyOf(hHeap, hSize * 2);
        hHeap[hSize] = ((long) (g + h(to)) << 32) | to;
        up(hHeap, hSize++);
    }

    private int clusterOf(int t) {
        return ((t / w) / CS) * cw + (t % w) / CS;
    }

    /** From tile t to each gateway of its cluster k (FAR where there's no way within the cluster). */
    private int[] costsInCluster(int t, int k) {
        int x0 = (k % cw) * CS, y0 = (k / cw) * CS, x1 = Math.min(w, x0 + CS), y1 = Math.min(h, y0 + CS);
        box(x0, y0, x1, y1);
        int[] d = local(t, x0, y0, x1, y1);
        int[] list = clusterNodes[k];
        int[] out = new int[list.length];
        for (int a = 0; a < list.length; a++) {
            int n = nodeTile[list[a]];
            out[a] = d[(n / w - y0) * CS + (n % w - x0)];
        }
        return out;
    }

    // ------------------------------------------------------------------ straightening

    /**
     * The route's corners only: from each point, on to the furthest one further along that can be walked to in a
     * straight line over ground no dearer than the route took (so a shortcut never cuts across the road where the
     * route kept to the pavement), and not brushing a wall.
     */
    private int[] straighten(int s, int[] tiles) {
        int n = tiles.length;
        if (n <= 2) return tiles;
        IntList out = new IntList();
        int from = s, i = -1;
        while (i < n - 1) {
            int best = i + 1, worst = c.walkCost[tiles[i + 1]];
            for (int j = i + 1; j < n && j <= i + 48; j++) {
                worst = Math.max(worst, c.walkCost[tiles[j]]);
                if (j > i + 1 && !clearLine(from, tiles[j], worst)) break;
                best = j;
            }
            out.add(tiles[best]);
            from = tiles[best];
            i = best;
        }
        return out.toArray();
    }

    /** A straight walk between the middles of two tiles crosses nothing solid nor anything dearer than maxCost. */
    private boolean clearLine(int a, int b, int maxCost) {
        float ax = a % w + 0.5f, ay = a / w + 0.5f, bx = b % w + 0.5f, by = b / w + 0.5f;
        float dx = bx - ax, dy = by - ay;
        int steps = (int) (Math.max(Math.abs(dx), Math.abs(dy)) * 3) + 1;
        for (int k = 1; k < steps; k++) {
            float px = ax + dx * k / steps, py = ay + dy * k / steps;
            // (A person's width: the line and a little either side of it.)
            for (int q = 0; q < 3; q++) {
                float ox = q == 0 ? 0 : (q == 1 ? 0.3f : -0.3f) * -dy / (Math.abs(dx) + Math.abs(dy) + 0.001f);
                float oy = q == 0 ? 0 : (q == 1 ? 0.3f : -0.3f) * dx / (Math.abs(dx) + Math.abs(dy) + 0.001f);
                int x = (int) (px + ox), y = (int) (py + oy);
                if (x < 0 || y < 0 || x >= w || y >= h) return false;
                int i = y * w + x;
                if (!open(i) || c.walkCost[i] > maxCost) return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ bits

    private static void up(long[] heap, int i) {
        long v = heap[i];
        while (i > 0) {
            int p = (i - 1) >> 1;
            if (heap[p] <= v) break;
            heap[i] = heap[p];
            i = p;
        }
        heap[i] = v;
    }

    /** Restores the heap after its top was replaced by the last item; returns size unchanged. */
    private static int down(long[] heap, int size) {
        if (size == 0) return 0;
        long v = heap[0];
        int i = 0;
        while (true) {
            int ch = 2 * i + 1;
            if (ch >= size) break;
            if (ch + 1 < size && heap[ch + 1] < heap[ch]) ch++;
            if (heap[ch] >= v) break;
            heap[i] = heap[ch];
            i = ch;
        }
        heap[i] = v;
        return size;
    }

    private static final class IntList {
        int[] a = new int[64];
        int n;

        void add(int v) {
            if (n == a.length) a = Arrays.copyOf(a, n * 2);
            a[n++] = v;
        }

        int[] toArray() {
            return Arrays.copyOf(a, n);
        }
    }
}
