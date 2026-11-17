package com.instantaxtion.zombiesandbox;

/**
 * ((27.5) Officers in charge of a fight: an army commander, or a police sergeant or above. With a big pack of the dead
 * coming and several squads (or officers) round them, they spread them out along one line - at the narrowest spot
 * close behind them that can see the dead, so the fire overlaps - and hold them there; when the line is about to break
 * they pull everyone back together (to the same place the 27.4 fall-back uses), not one by one.
 */
final class Commanders {
    private final World w;
    /** When the last order to pull back went out on the radio (only the first in a while does). */
    private float retreatSaidAt = -99;
    /** Lines formed and retreats ordered, for the probes and the test. */
    int lines, retreats;
    private Entity[] buf = new Entity[16];

    static final float REACH = 260, SEE = 420, SLOT_S = 4;

    Commanders(World w) {
        this.w = w;
    }

    /** An officer who takes charge: an army commander, or a police officer of sergeant's rank or above. */
    static boolean leader(Entity e, boolean soldier) {
        if (soldier) return e.role == Entity.ROLE_COMMANDER;
        if (e.type != Entity.COP) return false;
        String[] l = Names.copLadder(e);
        int sgt = l.length - 1;
        for (int i = 0; i < l.length; i++)
            if (l[i].equals("Sergeant") || l[i].equals("Lieutenant")) {
                sgt = i;
                break;
            }
        return Names.copRankIndex(e) >= sgt;
    }

    /** One of c's men, free for a line: their own side, on foot, not on a post or held to some other job. */
    private boolean usable(Entity c, Entity o) {
        if (o == c || o.dead || o.type != c.type || o == w.controlled || World.riding(o)) return false;
        if (o.task != Dispatch.T_NONE && o.task != Dispatch.T_RESPOND) return false;
        if (o.cmdBy != null && o.cmdBy != c && o.lineT > w.time && !o.cmdBy.dead) return false;
        if (o.role == Entity.ROLE_COMMANDER || o.role == Entity.ROLE_PILOT || o.role == Entity.ROLE_MORTAR || o.role == Entity.ROLE_SNIPER
                || o.role == Entity.ROLE_SAR || o.role == Entity.ROLE_CORPSMAN) return false;
        return o.ammo > 0 || o.reserve > 0;
    }

    /** Every second and a half, for a leader: look at the fight round them and give their orders. */
    void lead(Entity c, boolean soldier, float dt) {
        if ((c.cmdCd -= dt) > 0) return;
        c.cmdCd = 1.5f;
        if (c == w.controlled || World.riding(c) || !leader(c, soldier)) {
            c.cmdSquads = 0;
            return;
        }
        float[] zc = w.spatial.zombieCentre(c.x, c.y, SEE);
        int pack = (int) zc[2];
        int n = 0;
        float gx = c.x, gy = c.y, stock = 0;
        for (int i = 0, m = w.spatial.gatherArmed(c.x, c.y, REACH); i < m; i++) {
            Entity o = w.gathered[i];
            if (!usable(c, o) || World.hypot(o.x - c.x, o.y - c.y) > REACH) continue;
            if (n == buf.length) buf = java.util.Arrays.copyOf(buf, n * 2);
            buf[n++] = o;
            gx += o.x;
            gy += o.y;
            stock += (o.ammo + o.reserve) / (float) Math.max(1, o.magSize + World.fullReserve(o));
        }
        gx /= n + 1;
        gy /= n + 1;
        stock = n > 0 ? stock / n : 0;
        c.cmdSquads = 0;
        if (n < 4 || pack < Math.max(10, n * 2)) {
            c.cmdLine = false;
            return;
        }
        // (Squads: the army counts its squads, the police their officers.)
        int squads = 0;
        sortBySquad(n);
        for (int i = 0; i < n; i++) if (!soldier || i == 0 || buf[i].squad != buf[i - 1].squad) squads++;
        c.cmdSquads = squads;
        // Pulled back just now: let them get there before forming up again.
        if (c.cmdRetreatUntil > w.time) return;
        // Far more of them coming than these guns can stop: pull back together now, before they're on them.
        float reach = World.hypot(zc[0] - gx, zc[1] - gy) < 260 ? 1 : 0;
        if (reach > 0 && pack > n * (soldier ? 10 : 4) * Math.min(1, stock + 0.3f)) {
            retreat(c, n, zc, soldier);
            return;
        }
        // The line about to break: far more of the dead on it than guns, or the guns running dry with them close.
        if (c.cmdLine) {
            int press = w.spatial.countZombiesNear(c.cmdAx, c.cmdAy, 90);
            boolean thin = n < c.cmdUnits / 2;
            if (press >= 6 && (press >= n * (soldier ? 1.1f : 0.8f) || stock < 0.25f || thin)) {
                retreat(c, n, zc, soldier);
                return;
            }
        }
        formLine(c, n, gx, gy, zc, soldier);
    }

    /** Sort the first n of buf by squad, then member, so each squad holds its own stretch of the line. */
    private void sortBySquad(int n) {
        for (int i = 1; i < n; i++) {
            Entity k = buf[i];
            int j = i - 1;
            while (j >= 0 && (buf[j].squad > k.squad || buf[j].squad == k.squad && buf[j].member > k.member)) {
                buf[j + 1] = buf[j];
                j--;
            }
            buf[j + 1] = k;
        }
    }

    /** How far the street is open from (x, y) along (px, py), in steps of 12 up to 120. */
    private float open(float x, float y, float px, float py) {
        float d = 0;
        while (d < 120) {
            float nx = x + px * (d + 12), ny = y + py * (d + 12);
            if (w.city.solidTile(City.fl(nx / City.T), City.fl(ny / City.T))) break;
            d += 12;
        }
        return d;
    }

    /**
     * Where the line goes: the narrowest open spot (a choke point) a little way behind the group, between them and the
     * dead, that can still see them - and each unit's place on it, squads side by side, a second rank staggered behind
     * when the street is too narrow for them all.
     */
    private void formLine(Entity c, int n, float gx, float gy, float[] zc, boolean soldier) {
        float dx = zc[0] - gx, dy = zc[1] - gy, dl = (float) Math.sqrt(dx * dx + dy * dy);
        // (Only with the dead coming from one side: in the middle of them, a line has nothing to face.)
        if (dl < 120 || w.spatial.countZombiesNear(gx, gy, 70) >= 4) {
            c.cmdLine = false;
            return;
        }
        dx /= dl;
        dy /= dl;
        float px = -dy, py = dx;
        float ax = gx, ay = gy, al = 0, ar = 0, bestScore = Float.MAX_VALUE;
        // (Kept where it was while it still serves: no shuffling every order.)
        boolean keep = c.cmdLine && World.hypot(c.cmdAx - gx, c.cmdAy - gy) < 140 && World.hypot(c.cmdAx - zc[0], c.cmdAy - zc[1]) > 60;
        for (int k = keep ? -1 : 0; k < 4; k++) {
            float x = k < 0 ? c.cmdAx : gx - dx * 40 * k, y = k < 0 ? c.cmdAy : gy - dy * 40 * k;
            // (With the dead nearly on them, no time to move back.)
            if (k > 0 && dl < 150) break;
            if (w.city.solidTile(City.fl(x / City.T), City.fl(y / City.T))) continue;
            float l = open(x, y, -px, -py), r = open(x, y, px, py);
            float score = l + r + k * 18 + (w.city.los(x, y, zc[0], zc[1]) ? 0 : 80) + (k < 0 ? -30 : 0);
            if (score < bestScore) {
                bestScore = score;
                ax = x;
                ay = y;
                al = l;
                ar = r;
            }
        }
        if (bestScore == Float.MAX_VALUE) return;
        float width = al + ar;
        int perRank = Math.max(1, Math.min(n, (int) (width / 14) + 1));
        float spacing = perRank > 1 ? Math.min(24, width / (perRank - 1)) : 0;
        for (int i = 0; i < n; i++) {
            int rank = i / perRank, j = i % perRank, inRank = Math.min(perRank, n - rank * perRank);
            float off = (j - (inRank - 1) / 2f) * spacing + (rank % 2 == 1 ? spacing / 2 : 0);
            off = Math.max(-al, Math.min(ar, off + (ar - al) / 2));
            float sx = ax + px * off - dx * 16 * rank, sy = ay + py * off - dy * 16 * rank;
            float[] q = w.city.findWalkable(sx, sy);
            Entity o = buf[i];
            o.lineX = q != null ? q[0] : ax;
            o.lineY = q != null ? q[1] : ay;
            o.lineT = w.time + SLOT_S;
            o.cmdBy = c;
        }
        // The officer just behind the middle of it.
        float[] q = w.city.findWalkable(ax - dx * 40, ay - dy * 40);
        if (q != null) {
            c.lineX = q[0];
            c.lineY = q[1];
            c.lineT = w.time + SLOT_S;
            c.cmdBy = c;
        }
        if (!c.cmdLine) {
            lines++;
            c.cmdUnits = n;
            w.dispatch.chatter(soldier ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, c, soldier
                    ? "Squads, on line here! Spread out, overlapping fire, let them come to us."
                    : "Everyone on me, form a line! Hold them here.", c.x, c.y);
        }
        c.cmdLine = true;
        c.cmdAx = ax;
        c.cmdAy = ay;
    }

    /** The line won't hold: everyone back together to one place (see ArmedAI.rallyPoint), shooting as they go. */
    private void retreat(Entity c, int n, float[] zc, boolean soldier) {
        float[] r = w.armedAI.rallyPoint(c, zc[0], zc[1]);
        if (r == null) {
            float ax = c.x - zc[0], ay = c.y - zc[1], ad = (float) Math.sqrt(ax * ax + ay * ay) + 0.001f;
            r = w.city.findWalkable(c.x + ax / ad * 260, c.y + ay / ad * 260);
            if (r == null) return;
        }
        for (int i = 0; i <= n; i++) {
            Entity o = i == n ? c : buf[i];
            o.lineT = 0;
            o.rallyX = r[0];
            o.rallyY = r[1];
            o.rallyT = w.time + 10;
            o.goal = World.A_FALLBACK;
            o.goalCd = 1.5f;
            o.goalTime = 0;
        }
        retreats++;
        c.cmdLine = false;
        c.cmdRetreatUntil = w.time + 15;
        if (w.time - retreatSaidAt > 30) {
            retreatSaidAt = w.time;
            w.dispatch.say(soldier ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, c,
                    "The line won't hold! All units fall back together to " + w.city.placeName(r[0], r[1]) + "!", c.x, c.y);
        }
    }

    /** On a leader's card: how many squads (or officers) they have in hand just now; on a unit's, whose line they're on. */
    String describe(Entity e) {
        if (e.cmdSquads > 0) {
            boolean soldier = e.type == Entity.SOLDIER;
            return "Commanding: " + World.count(e.cmdSquads, soldier ? "squad" : "officer", soldier ? "squads" : "officers")
                    + (e.cmdLine ? ", holding a line" : e.cmdRetreatUntil > w.time ? ", pulling back" : "");
        }
        if (e.lineT > w.time && e.cmdBy != null && e.cmdBy != e && !e.cmdBy.dead) return "Holding the line for " + Dispatch.name(e.cmdBy);
        return null;
    }
}
