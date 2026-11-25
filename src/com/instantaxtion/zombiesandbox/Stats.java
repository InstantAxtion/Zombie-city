package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Keeping count: the tallies, the history for the graphs and the replay, the timeline, highlights and the best units.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class Stats {
    private final World w;

    Stats(World w) {
        this.w = w;
    }

    /** The best zombie killers of this game, living or fallen: {name seed, type, role, kills, alive}. */
    /** ((12.0) The most kills by any one of the living, standing or fallen, without sorting everyone for it.) */
    int bestKills() {
        int best = 0;
        for (int i = 0, c = w.entities.size(); i < c; i++) {
            Entity e = w.entities.get(i);
            if (e.kills > best && !e.isZombie()) best = e.kills;
        }
        for (int[] h : w.fallenHeroes) best = Math.max(best, h[3]);
        return best;
    }

    /**
     * ((13.3) The living with the most kills, most first: out on the streets, or aboard a car, truck or helicopter
     * (indoors, they can't be watched, so not them). For the Top Fighters list.)
     */
    ArrayList<Entity> topFighters(int n) {
        ArrayList<Entity> all = new ArrayList<Entity>();
        java.util.IdentityHashMap<Entity, Boolean> seen = new java.util.IdentityHashMap<Entity, Boolean>();
        for (int i = 0, c = w.entities.size(); i < c; i++) {
            Entity e = w.entities.get(i);
            if (e.kills > 0 && !e.dead && !e.removed && !e.isZombie() && seen.put(e, Boolean.TRUE) == null) all.add(e);
        }
        for (int i = 0, c = w.fleet.vehicles.size(); i < c; i++) {
            Fleet.Vehicle v = w.fleet.vehicles.get(i);
            if (v.removedFromFleet) continue;
            for (int k = 0; k < v.crew.size(); k++) {
                Entity e = v.crew.get(k);
                if (e.kills > 0 && e.hp > 0 && !e.isZombie() && seen.put(e, Boolean.TRUE) == null) all.add(e);
            }
            for (int k = 0; k < v.riders.size(); k++) {
                Entity e = v.riders.get(k);
                if (e.kills > 0 && e.hp > 0 && !e.isZombie() && seen.put(e, Boolean.TRUE) == null) all.add(e);
            }
        }
        java.util.Collections.sort(all, new java.util.Comparator<Entity>() {
            @Override
            public int compare(Entity a, Entity b) {
                return b.kills - a.kills;
            }
        });
        while (all.size() > n) all.remove(all.size() - 1);
        return all;
    }

    ArrayList<int[]> heroes(int n) {
        ArrayList<int[]> all = new ArrayList<int[]>();
        for (int i = 0, c = w.entities.size(); i < c; i++) {
            Entity e = w.entities.get(i);
            if (e.kills > 0 && !e.isZombie()) all.add(new int[]{e.nameSeed, e.type, e.role, e.kills, 1});
        }
        for (int[] h : w.fallenHeroes) all.add(new int[]{h[0], h[1], h[2], h[3], 0});
        java.util.Collections.sort(all, new java.util.Comparator<int[]>() {
            @Override
            public int compare(int[] a, int[] b) {
                return b[3] - a[3];
            }
        });
        while (all.size() > n) all.remove(all.size() - 1);
        return all;
    }

    /** Records humans vs zombies; when the history fills, halves its resolution to keep going. */
    void sampleStats() {
        if (w.histCount == World.HISTORY) {
            for (int i = 0; i < World.HISTORY / 2; i++) {
                w.histHumans[i] = (w.histHumans[i * 2] + w.histHumans[i * 2 + 1]) / 2;
                w.histZombies[i] = (w.histZombies[i * 2] + w.histZombies[i * 2 + 1]) / 2;
                w.histArmed[i] = (w.histArmed[i * 2] + w.histArmed[i * 2 + 1]) / 2;
                w.histKills[i] = w.histKills[i * 2 + 1];
            }
            w.histCount = World.HISTORY / 2;
            w.statStep *= 2;
        }
        w.histHumans[w.histCount] = w.humans;
        w.histZombies[w.histCount] = w.zombies;
        w.histArmed[w.histCount] = w.counts[Entity.COP] + w.counts[Entity.SOLDIER];
        w.histKills[w.histCount] = w.zombiesKilled;
        w.histCount++;
    }

    /** Counts everyone right now (dead and removed people don't count), for when the game is paused. */
    void recountLive() {
        Arrays.fill(w.counts, 0);
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (!e.dead) w.counts[e.type]++;
        }
        w.fleet.countCrews(w.counts);
    }

    /** ((12.0) Counted again at the next recount, not up to half a second later: after clearing or loading.) */
    void recountInsideNow() {
        w.insideAt = -1;
        w.heldHumans = -1;
    }

    void recount() {
        Arrays.fill(w.counts, 0);
        w.bittenOut = 0;
        w.commanders.clear();
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            // ((10.56) Not those who've just gone indoors and not been cleared out of the list yet: they're
            // counted in their building below. At the start of a game that was everyone at home, twice.)
            if (e.removed) continue;
            w.counts[e.type]++;
            if (e.infected) w.bittenOut++;
            if (e.role == Entity.ROLE_COMMANDER && e.type == Entity.SOLDIER) w.commanders.add(e);
        }
        w.fleet.countCrews(w.counts);
        // ((12.0) Who's inside the buildings is counted a few times a second, not every step: on the big maps that's
        // tens of thousands of buildings, and it was one of the biggest costs of a quiet moment.)
        boolean fresh = w.time >= w.insideAt || w.time < w.insideAt - 1;
        if (fresh) scanInside();
        w.hiding = w.insideHiding;
        w.visiting = w.insideVisiting;
        int inCars = fleetCivilians();
        w.civilians = w.counts[Entity.CIVILIAN] + w.insideCivilians + inCars;
        w.riding = w.fleet.riderCount();
        int h = w.humanCount() + w.hiding + w.riding + w.visiting;
        // (Before deciding the city has fallen on a count that's a moment old - everyone who's just gone indoors
        // missing from it - it's counted again properly.)
        if (!fresh && w.outbreak && (h == 0 || h <= w.startHumans * World.FALLEN)) {
            scanInside();
            w.civilians = w.counts[Entity.CIVILIAN] + w.insideCivilians + inCars;
            w.hiding = w.insideHiding;
            w.visiting = w.insideVisiting;
            h = w.humanCount() + w.hiding + w.riding + w.visiting;
            fresh = true;
        }
        // ((12.5) Only the counts taken together with who's indoors: in between, everyone who'd gone in or come out
        // since was counted twice or not at all, and the number of people went up and down by a few every moment.)
        if (fresh || w.heldHumans < 0) {
            w.heldHumans = h;
            w.heldCivilians = w.civilians;
        } else {
            h = w.heldHumans;
            w.civilians = w.heldCivilians;
        }
        recountRest(h);
    }

    /** ((12.0) Civilians riding in cars.) */
    int fleetCivilians() {
        int c = 0;
        for (int i = 0, n = w.fleet.vehicles.size(); i < n; i++) {
            ArrayList<Entity> r = w.fleet.vehicles.get(i).riders;
            for (int k = 0; k < r.size(); k++) if (r.get(k).type == Entity.CIVILIAN) c++;
        }
        return c;
    }

    /** ((12.0) Counts who's inside the buildings: hiding, on errands, the dead shut in, the bitten.) */
    void scanInside() {
        {
            w.insideAt = w.time + 0.4f;
            int hid = 0, vis = 0, civ = 0, lurk = 0, bit = 0;
            for (int i = 0, n = w.city.buildings.size(); i < n; i++) {
                City.Building b = w.city.buildings.get(i);
                lurk += b.lurkers;
                int no = b.occupants.size(), nv = b.visitors.size();
                if (no == 0 && nv == 0) continue;
                hid += no;
                vis += nv;
                for (int k = 0; k < no; k++) {
                    Entity o = b.occupants.get(k);
                    if (o.type == Entity.CIVILIAN) civ++;
                    if (o.infected) bit++;
                }
                for (int k = 0; k < nv; k++) {
                    Entity o = b.visitors.get(k);
                    if (o.type == Entity.CIVILIAN) civ++;
                    if (o.infected) bit++;
                }
            }
            w.insideHiding = hid;
            w.insideVisiting = vis;
            w.insideCivilians = civ;
            w.insideLurking = lurk;
            w.insideBitten = bit;
        }
    }

    void recountRest(int h) {
        int z = w.zombieCount();
        w.peakZombies = Math.max(w.peakZombies, z);
        int rising = 0;
        for (int i = 0, n = w.corpses.size(); i < n; i++) if (w.corpses.get(i).rise > 0) rising++;
        w.lurking = w.insideLurking;
        if ((z > 0 || w.lurking > 0) && h > 0 && !w.outbreak && w.aftermath != World.AFTER_FALLEN) {
            if (w.aftermath == World.AFTER_RECOVERY) {
                // The dead are back while the city was getting on its feet again.
                w.aftermath = World.AFTER_NONE;
                w.warResult = 0;
                w.banner("THE DEAD ARE BACK", "Just as the city was getting back on its feet.");
            }
            w.outbreak = true;
            w.startHumans = h;
            w.warLead = 0;
            w.warBalance = 0.5f;
            w.warFresh = true;
        }
        w.startHumans = Math.max(w.startHumans, h);
        if (w.outbreak) {
            int bitten = 0, armed = w.counts[Entity.COP] + w.counts[Entity.SOLDIER];
            // ((13.1) Counted along with everyone else, not in a second pass through tens of thousands of people.)
            bitten = w.bittenOut + w.insideBitten;
            // Over only once it's stayed over for a few seconds.
            boolean clear = z == 0 && rising == 0 && w.lurking == 0 && bitten == 0 && h > 0;
            if (!clear) w.clearSince = -1;
            else if (w.clearSince < 0) w.clearSince = w.time;
            if (clear && w.time - w.clearSince >= 4) {
                w.outbreak = false;
                w.warResult = 1;
                timeline("The outbreak is over: the city survives");
                int t = (int) w.outbreakTime;
                w.banner(h < w.startHumans * 0.35f ? "THE CITY SURVIVES, BARELY" : "THE CITY SURVIVES", "The outbreak is over after "
                        + World.clock(t) + ". " + World.count(w.zombiesKilled, "zombie", "zombies") + " destroyed, "
                        + World.count(w.civiliansLost, "civilian", "civilians") + " lost.");
                w.aftermathLife.startAftermath(World.AFTER_RECOVERY);
                w.aftermathLife.calmDown();
                if (w.quarantined) w.war.liftQuarantine();
            } else if (h == 0 || (h <= w.startHumans * World.FALLEN && z > h) || (armed == 0 && h <= Math.max(5, w.startHumans / 12) && z > h)) {
                w.outbreak = false;
                w.warResult = 2;
                timeline("The city has fallen");
                w.banner("THE CITY HAS FALLEN", h == 0 ? "Nobody is left alive." : "Only " + h + (h == 1 ? " survivor is" : " survivors are")
                        + " left, hiding in the ruins.");
                if (h > 0) w.aftermathLife.startAftermath(World.AFTER_FALLEN);
            }
        }
        w.humans = h;
        w.zombies = z + rising + w.lurking;
    }

    /**
     * How well the city has done, as one number: the dead destroyed and a city saved with everyone still alive in
     * it, less those lost, and more for a harder game.
     */
    int score() {
        float s = w.zombiesKilled * 10 + w.alphasKilled * 150 + (w.warResult == 1 ? 1500 + w.humans * 2 : 0) - w.civiliansLost * 4 - (w.copsLost + w.soldiersLost) * 6;
        return Math.max(0, Math.round(s * Director.SCORE[w.difficulty]));
    }

    void recordReplay(float dt) {
        w.replayTimer -= dt;
        if (w.replayTimer > 0) return;
        w.replayTimer = w.replayStep;
        int z = 0, p = 0;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.dead) continue;
            if (e.isZombie()) z++;
            else if (e.type != Entity.DOG) p++;
        }
        int zs = Math.max(1, (z + World.REPLAY_SAMPLE - 1) / World.REPLAY_SAMPLE), ps = Math.max(1, (p + World.REPLAY_SAMPLE - 1) / World.REPLAY_SAMPLE);
        short[] dots = new short[(Math.min(z, World.REPLAY_SAMPLE) + Math.min(p, World.REPLAY_SAMPLE)) * 2 + 4];
        int k = 0, zi = 0, pi = 0, zn = 0, pn = 0;
        for (int pass = 0; pass < 2; pass++)
            for (int i = 0, n = w.entities.size(); i < n; i++) {
                Entity e = w.entities.get(i);
                if (e.dead || e.type == Entity.DOG || e.isZombie() != (pass == 0)) continue;
                if (pass == 0 ? zi++ % zs != 0 : pi++ % ps != 0) continue;
                if (k + 2 > dots.length) break;
                dots[k++] = (short) e.x;
                dots[k++] = (short) e.y;
                if (pass == 0) zn++;
                else pn++;
            }
        w.replayMeta.add(new float[]{w.time, z, p + w.hiding + w.visiting + w.riding, zn, pn});
        w.replayDots.add(k == dots.length ? dots : Arrays.copyOf(dots, k));
        if (w.replayMeta.size() >= World.REPLAY_FRAMES) {
            // A long game: keep every other snapshot and take them half as often from now on.
            for (int i = w.replayMeta.size() - 1; i > 0; i -= 2) {
                w.replayMeta.remove(i);
                w.replayDots.remove(i);
            }
            w.replayStep *= 2;
        }
    }

    void noteTurn(float x, float y) {
        if (w.turnEvents.size() < 6000) w.turnEvents.add(new float[]{w.time, x, y});
    }

    void highlight(String text, float x, float y) {
        if (w.highlightAt.size() >= 400) return;
        // Not the same thing twice in a row within a few seconds.
        int n = w.highlightAt.size();
        if (n > 0 && w.highlightText.get(n - 1).equals(text) && w.time - w.highlightAt.get(n - 1)[0] < 10) return;
        w.highlightAt.add(new float[]{w.time, x, y});
        w.highlightText.add(text);
    }

    void timeline(String text) {
        timeline(text, Float.NaN, Float.NaN);
    }

    /** (14.4) A moment that happened somewhere: the timeline can take you there. */
    void timeline(String text, float x, float y) {
        // ((33.0) Full, the oldest but the first moment makes way: after forty, nothing new went on it, and the end of a
        // long outbreak - the city falling, or saved - was never on the timeline.)
        if (w.timeline.size() >= 40) w.timeline.remove(1);
        World.Moment m = new World.Moment();
        m.time = w.time;
        m.text = text;
        m.x = x;
        m.y = y;
        w.timeline.add(m);
    }

    /** A hero's milestone, called out on the radio. */
    void killStreak(Entity e) {
        highlight(Names.person(e.nameSeed) + ": " + e.kills + " kills", e.x, e.y);
        String line = String.format(java.util.Locale.US, World.STREAK_LINES[w.rnd.nextInt(World.STREAK_LINES.length)], e.kills);
        if (e.isArmed()) w.dispatch.chatter(e.type == Entity.SOLDIER ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, e, line, e.x, e.y);
        else w.dispatch.say(Dispatch.WHO_INFO, null, Names.person(e.nameSeed) + ": \"" + line + "\"", e.x, e.y);
    }
}
