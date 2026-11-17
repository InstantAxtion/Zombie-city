package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Containment: cordons, infection zones, checkpoints, walls and the helicopters and drones watching them.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class Containment {
    private final World w;

    Containment(World w) {
        this.w = w;
    }

    /**
     * Once a second (acting every ten). In an outbreak the army's engineers wire the streets leading into a safe zone
     * they're at: a coil at a time, up to ten a zone, on the roads and pavements just outside its wall.
     */
    /**
     * ((22.0) A safe zone that has held for three minutes: two of its own guards take machine guns to sandbagged posts at
     * opposite inside corners of the wall. Nobody new: the guards it has.
     */
    void zonePosts(Dispatch.SafeZone z) {
        float[][] corners = {{(z.tx0 + 2.5f) * City.T, (z.ty0 + 2.5f) * City.T}, {(z.tx1 - 1.5f) * City.T, (z.ty1 - 1.5f) * City.T}};
        int c = 0;
        for (int i = 0, n = w.entities.size(); i < n && c < 2; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || e.zone != z || e.task != Dispatch.T_GUARD || !e.canShoot() || e.role == Entity.ROLE_GUNNER) continue;
            float[] q = w.city.findWalkable(corners[c][0], corners[c][1]);
            if (q == null || !z.inside(q[0], q[1])) {
                c++;
                continue;
            }
            if (e.type == Entity.SOLDIER) w.population.applyRole(e, Entity.ROLE_GUNNER);
            else e.reserve += 150;
            // (Holding the post, like a nest's crew: off the zone's own roster, which finds another for the wall.)
            e.task = Dispatch.T_HOLD;
            e.zone = null;
            z.guards--;
            e.postX = q[0];
            e.postY = q[1];
            for (int k = 0; k < 3; k++) {
                float[] b = w.sandbox.placeBarricade(q[0] + (k - 1) * City.T, q[1] + (c == 0 ? -City.T : City.T));
                if (b != null) World.setTier(b, 1);
            }
            c++;
        }
    }

    /**
     * (10.32) An army base with the dead all over it and next to no soldiers left holding it, for half a minute,
     * has fallen: command moves to the next base (the big one out in the country, if the town's goes).
     */
    void watchBases(float dt) {
        if (w.zombies == 0) return;
        for (City.Facility f : w.city.facilities) {
            if (f.kind != City.FACILITY_BASE || f.fallen) continue;
            int z = w.spatial.countZombiesNear(f.x, f.y, f.r * 1.3f), s = 0;
            for (int i = 0, n = w.spatial.gatherLiving(f.x, f.y, f.r * 1.3f); i < n; i++)
                if (w.gathered[i].type == Entity.SOLDIER && !w.gathered[i].dead) s++;
            if (z >= 12 && s <= 2) f.fallingFor += dt;
            else f.fallingFor = Math.max(0, f.fallingFor - dt);
            if (f.fallingFor < 30) continue;
            f.fallen = true;
            City.Facility next = w.city.mainBase();
            w.dispatch.say(Dispatch.WHO_MILITARY, null, "Military: " + f.name + " has been overrun."
                    + (next != null && next != f && !next.fallen ? " Command moves to " + next.name + "." : " There's nowhere left to fall back to."), f.x, f.y);
            w.stats.timeline("Army base overrun: " + f.name, f.x, f.y);
        }
    }

    /**
     * (10.32) The soldiers on the country base's checkpoints, and police at their roadblocks, look over everyone on
     * foot who comes past: someone bitten is mostly spotted and sent back the way they came.
     */
    void checkpointGuards() {
        if (w.zombies == 0) return;
        for (int k = 0; k < w.city.checkpoints.size(); k++) {
            float[] cp = w.city.checkpoints.get(k);
            boolean manned = false;
            for (int i = 0, n = w.spatial.gatherLiving(cp[0], cp[1], 70); i < n && !manned; i++)
                if (w.gathered[i].type == Entity.SOLDIER && !w.gathered[i].dead) manned = true;
            if (!manned) continue;
            screenAt(cp[0], cp[1], Dispatch.WHO_MILITARY, "Checkpoint");
        }
        for (int i = 0, n = w.fleet.vehicles.size(); i < n; i++) {
            Fleet.Vehicle v = w.fleet.vehicles.get(i);
            if (v.block != null && v.state == Fleet.BLOCK) screenAt(v.x, v.y, Dispatch.WHO_POLICE, "Roadblock");
        }
    }

    void screenAt(float x, float y, int who, String what) {
        for (int i = 0, n = w.spatial.gatherLiving(x, y, 55); i < n; i++) {
            Entity e = w.gathered[i];
            if (e.type != Entity.CIVILIAN || e.dead || !e.infected || e.screened) continue;
            e.screened = true;
            if (w.rnd.nextFloat() < 0.25f || hidesBite(e)) continue;
            w.turnedAway++;
            e.refused = true;
            e.task = Dispatch.T_NONE;
            e.talkTimer = 2;
            float dx = e.x - x, dy = e.y - y, d = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
            e.fleeTimer = 3;
            w.movement.flee(e, dx / d, dy / d, e.runSpeed);
            if (w.time > w.checkpointSaid) {
                w.checkpointSaid = w.time + 20;
                w.dispatch.say(who, null, what + ": Bite found on someone at " + w.city.placeName(x, y) + ". Turned away.", x, y);
            }
        }
    }

    /**
     * (14.2) Someone bitten who's out with their family (with a leader, or leading others): the family may hide it
     * rather than see them turned away. Then they're through the check, and nobody's watching them when they turn.
     */
    boolean hidesBite(Entity e) {
        boolean family = (e.leader != null && !e.leader.dead && e.leader.type == Entity.CIVILIAN) || (w.hashFrame > 0 && e.followerFrame == w.hashFrame);
        if (!family || w.rnd.nextFloat() >= World.HIDE_BITE) return false;
        e.concealed = true;
        w.bitesHidden++;
        return true;
    }

    boolean checkpoint(Entity e, Dispatch.SafeZone z) {
        if (!e.infected || e.screened) return false;
        e.screened = true;
        // The bite was missed (the army's medics miss fewer than the police).
        if (w.rnd.nextFloat() < (z.military ? 0.2f : 0.4f)) return false;
        // ((14.2) Or their family hides it: covered up, and they talk their way through.)
        if (hidesBite(e)) return false;
        String where = z.place;
        w.turnedAway++;
        e.refused = true;
        e.task = Dispatch.T_NONE;
        e.talkTimer = 2;
        if (z.checkCd <= 0) {
            z.checkCd = 20;
            w.dispatch.say(z.military ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, null, "Checkpoint: Bite found on someone at the " + where
                    + " gate. Turned away.", z.x, z.y);
        }
        return true;
    }

    void updateCordon(float dt) {
        w.cordonTick -= dt;
        if (w.cordonTick > 0) return;
        w.cordonTick = 4;
        if (w.alert < 1 || w.zombies == 0) {
            while (!w.infection.isEmpty()) liftZone(w.infection.get(0), null);
            return;
        }
        int clusters = findClusters();
        // Each bunch of the dead: the zone already round it moves with it (gradually), or a new one is declared.
        boolean[] matched = new boolean[World.MAX_ZONES + 1];
        for (int c = 0; c < clusters; c++) {
            World.InfectionZone best = null;
            int bi = -1;
            float bd = Float.MAX_VALUE;
            for (int i = 0; i < w.infection.size(); i++) {
                World.InfectionZone z = w.infection.get(i);
                float d = (float) World.hypot(z.x - w.clusterX[c], z.y - w.clusterY[c]);
                if (d < z.r + 220 && d < bd) {
                    bd = d;
                    best = z;
                    bi = i;
                }
            }
            if (best != null) {
                if (matched[bi]) continue;
                matched[bi] = true;
                // (Redrawn only when they've really moved or spread: a line forever being redrawn is never held.)
                if (bd > Math.max(180, best.r * 0.6f) || w.clusterR[c] > best.r + 140 || (w.clusterR[c] < best.r * 0.5f && best.quietFor == 0))
                    placeZone(best, w.clusterX[c], w.clusterY[c], Math.max(w.clusterR[c], best.r * 0.8f));
            } else if (w.clusterN[c] >= 6 && w.infection.size() < World.MAX_ZONES) {
                World.InfectionZone z = new World.InfectionZone();
                placeZone(z, w.clusterX[c], w.clusterY[c], w.clusterR[c]);
                w.infection.add(z);
                matched[w.infection.size() - 1] = true;
                w.dispatch.say(Dispatch.WHO_POLICE, null, "Dispatch: Infection zone declared round " + z.place
                        + ". Perimeter going up; nobody in or out.", z.x, z.y);
                w.dispatch.callout(z.x, z.y, "INFECTION ZONE", 0xFFFF5A3A);
            }
        }
        // Two that have run into each other become one (the bigger takes in the other).
        for (int i = 0; i < w.infection.size(); i++)
            for (int j = w.infection.size() - 1; j > i; j--) {
                World.InfectionZone a = w.infection.get(i), b = w.infection.get(j);
                float d = (float) World.hypot(a.x - b.x, a.y - b.y);
                if (d > Math.max(Math.max(a.r, b.r) * 0.8f, (a.r + b.r) * 0.6f)) continue;
                World.InfectionZone keep = a.r >= b.r ? a : b, gone = keep == a ? b : a;
                float far = d + gone.r;
                if (far > keep.r + 60) {
                    float nr = Math.min(640, (keep.r + far) * 0.5f), t = d > 1 ? (nr - keep.r) / d : 0;
                    placeZone(keep, keep.x + (gone.x - keep.x) * t, keep.y + (gone.y - keep.y) * t, nr);
                }
                keep.wasCritical |= gone.wasCritical;
                liftZone(gone, null);
                i = -1;
                break;
            }
        // How many of the dead are still inside each, and who's at risk there. A zone with none left for a while is
        // cleared and lifted.
        for (int i = w.infection.size() - 1; i >= 0; i--) {
            World.InfectionZone z = w.infection.get(i);
            z.zombies = w.spatial.countZombiesNear(z.x, z.y, z.r + 40);
            if (z.zombies == 0) {
                // (Gone out of it, not gone: the zone goes after the stragglers, and is only lifted once there are
                // none of them anywhere near.)
                float sx = 0, sy = 0;
                int k = 0;
                for (int j = 0, m = w.entities.size(); j < m; j++) {
                    Entity o = w.entities.get(j);
                    if (o.dead || !o.isZombie() || World.hypot(o.x - z.x, o.y - z.y) > z.r + 500) continue;
                    sx += o.x;
                    sy += o.y;
                    k++;
                }
                boolean taken = false;
                if (k > 0) for (World.InfectionZone o : w.infection) if (o != z && World.hypot(o.x - sx / k, o.y - sy / k) < o.r + 60) taken = true;
                if (k > 0 && !taken) {
                    placeZone(z, sx / k, sy / k, Math.max(160, z.r * 0.7f));
                    z.zombies = w.spatial.countZombiesNear(z.x, z.y, z.r + 40);
                }
            }
            if (z.zombies > 0) z.quietFor = 0;
            else z.quietFor += 4;
            if (z.quietFor >= 12) {
                liftZone(z, "Dispatch: " + z.place + " is clear of them. Lifting the infection zone.");
                continue;
            }
            int people = 0;
            for (int k = 0, n = w.spatial.gatherLiving(z.x, z.y, z.r); k < n; k++) if (w.gathered[k].type == Entity.CIVILIAN) people++;
            z.people = people;
            boolean shelter = false;
            for (Dispatch.SafeZone sz : w.dispatch.zones) if (!sz.removed && World.hypot(sz.x - z.x, sz.y - z.y) < z.r + sz.r + 150) shelter = true;
            z.priority = z.zombies + 0.3f * people + (shelter ? 15 : 0);
        }
        if (w.infection.isEmpty()) return;
        // The worst first: as many zones held as there are people for (about ten free units a perimeter); the rest
        // wait their turn, watched, until there's someone for them (or they're the worst).
        java.util.Collections.sort(w.infection, new java.util.Comparator<World.InfectionZone>() {
            @Override
            public int compare(World.InfectionZone a, World.InfectionZone b) {
                return Float.compare(World.rank(b), World.rank(a));
            }
        });
        int free = 0;
        for (int i = 0, m = w.entities.size(); i < m; i++) {
            Entity e = w.entities.get(i);
            if (!e.dead && w.controlled != e && (cordonCandidate(e) || e.task == Dispatch.T_CORDON)) free++;
        }
        int holdable = Math.max(1, free / 10);
        float top = World.rank(w.infection.get(0));
        for (int i = 0; i < w.infection.size(); i++) {
            World.InfectionZone z = w.infection.get(i);
            // (Held ones stay held a little longer than new ones take to qualify, so they don't flap.)
            z.critical = i == 0 || (i < holdable + (z.wasCritical ? 1 : 0) && World.rank(z) >= top * (z.wasCritical ? 0.2f : 0.3f));
            if (z.wasCritical && !z.critical)
                w.dispatch.say(Dispatch.WHO_POLICE, null, "Dispatch: " + z.place + " isn't the priority now. Units on that perimeter, move to "
                        + w.infection.get(0).place + ".", z.x, z.y);
            else if (!z.wasCritical && z.critical && i > 0)
                w.dispatch.say(Dispatch.WHO_POLICE, null, "Dispatch: Units free for " + z.place + ". Perimeter going up there too.", z.x, z.y);
            z.wasCritical = z.critical;
            if (!z.critical) {
                // (Waiting its turn: nobody on its perimeter, and its roads open again for now.)
                for (int p = 0; p < World.CORDON_POSTS; p++) closeCall(z.postCall, p);
                if (!z.checkpoints.isEmpty()) removeCheckpoints(z);
            } else if (z.checkpoints.isEmpty()) placeZone(z, z.x, z.y, z.r);
        }
        // Who's on which line now.
        for (World.InfectionZone z : w.infection) java.util.Arrays.fill(z.manned, 0);
        int unposted = 0;
        for (int i = 0, m = w.entities.size(); i < m; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || w.controlled == e) continue;
            if (e.task == Dispatch.T_CORDON) {
                World.InfectionZone z = zoneOfPost(e.postX, e.postY);
                int best = z == null || !z.critical ? -1 : postFor(z, e.postX, e.postY);
                if (best < 0) {
                    w.ops.jobs.release(e, z);
                    e.task = Dispatch.T_NONE;
                } else {
                    z.manned[best]++;
                    e.postX = z.post[best * 2];
                    e.postY = z.post[best * 2 + 1];
                }
            } else if (cordonCandidate(e)) unposted++;
        }
        // Half of the free units go on the lines (the most critical first); the rest stay free for 911 calls.
        int budget = unposted / 2;
        for (World.InfectionZone z : w.infection) {
            if (!z.critical) continue;
            for (int i = 0, m = w.entities.size(); i < m && budget > 0; i++) {
                Entity e = w.entities.get(i);
                if (e.dead || w.controlled == e || !cordonCandidate(e)) continue;
                if (World.hypot(e.x - z.x, e.y - z.y) > z.r + 1400) continue;
                int best = -1;
                float bd = Float.MAX_VALUE;
                for (int p = 0; p < World.CORDON_POSTS; p++) {
                    if (z.post[p * 2] < 0 || z.manned[p] >= 2) continue;
                    float d = (float) World.hypot(z.post[p * 2] - e.x, z.post[p * 2 + 1] - e.y) + z.manned[p] * 300;
                    if (d < bd) {
                        bd = d;
                        best = p;
                    }
                }
                if (best < 0) break;
                e.task = Dispatch.T_CORDON;
                e.postX = z.post[best * 2];
                e.postY = z.post[best * 2 + 1];
                // ((27.4) On the line round the zone: its, until the zone's lifted or the post goes.)
                w.ops.jobs.claim(e, z, Jobs.P_POST, "on the cordon");
                z.manned[best]++;
                budget--;
            }
            // Patrol cars on their rounds take crews out to posts nobody's on.
            int cars = 0;
            for (int p = 0; p < World.CORDON_POSTS && cars < 3; p++) {
                if (z.post[p * 2] < 0 || z.manned[p] > 0 || (z.postCall[p] != null && !z.postCall[p].resolved)) continue;
                Dispatch.Incident inc = new Dispatch.Incident();
                inc.x = z.post[p * 2];
                inc.y = z.post[p * 2 + 1];
                inc.place = "the perimeter on " + w.city.placeName(inc.x, inc.y);
                inc.cordonPost = p;
                Fleet.Vehicle v = null;
                float bd = Float.MAX_VALUE;
                for (int i = 0, m = w.fleet.vehicles.size(); i < m; i++) {
                    Fleet.Vehicle c = w.fleet.vehicles.get(i);
                    if (!c.patrol || c.agency != 0 || c.state != Fleet.PATROL || c.broken || c.parked || c.crew.isEmpty()) continue;
                    float d = (c.x - inc.x) * (c.x - inc.x) + (c.y - inc.y) * (c.y - inc.y);
                    if (d < bd) {
                        bd = d;
                        v = c;
                    }
                }
                if (v == null) break;
                if (w.fleet.respond(v, inc)) {
                    z.postCall[p] = inc;
                    cars++;
                }
            }
            manCheckpoints(z);
            // ((11.0) Held where it is a while, and the army has people: the engineers wall it in.)
            z.stableFor += 4;
            if (z.stableFor >= 40 && z.r <= 520 && w.counts[Entity.SOLDIER] > 0) buildWall(z);
        }
    }

    /**
     * The bunches of the dead on the map (into clusterX/Y/R/N): the thickest patch first, everything within a few
     * streets of it with it, and so on. Returns how many.
     */
    int findClusters() {
        int cell = 256, gw2 = (int) (w.city.worldW() / cell) + 1, gh2 = (int) (w.city.worldH() / cell) + 1;
        int[] count = new int[gw2 * gh2];
        for (int i = 0, m = w.entities.size(); i < m; i++) {
            Entity z = w.entities.get(i);
            if (z.dead || !z.isZombie()) continue;
            int c = Math.min(gh2 - 1, Math.max(0, (int) (z.y / cell))) * gw2 + Math.min(gw2 - 1, Math.max(0, (int) (z.x / cell)));
            count[c]++;
        }
        boolean[] taken = new boolean[w.entities.size()];
        int n = 0;
        while (n < w.clusterX.length) {
            int best = -1;
            for (int c = 0; c < count.length; c++) if (count[c] >= 2 && (best < 0 || count[c] > count[best])) best = c;
            if (best < 0) break;
            float bx = (best % gw2 + 0.5f) * cell, by = (best / gw2 + 0.5f) * cell;
            // Everything within reach of that patch: its centre, and how far the bulk of them has got.
            float sx = 0, sy = 0;
            int k = 0;
            for (int pass = 0; pass < 2; pass++) {
                float ox = pass == 0 ? bx : sx / Math.max(1, k), oy = pass == 0 ? by : sy / Math.max(1, k);
                sx = sy = 0;
                k = 0;
                for (int i = 0, m = w.entities.size(); i < m; i++) {
                    Entity z = w.entities.get(i);
                    if (z.dead || !z.isZombie() || taken[i]) continue;
                    if ((z.x - ox) * (z.x - ox) + (z.y - oy) * (z.y - oy) > 420 * 420) continue;
                    sx += z.x;
                    sy += z.y;
                    k++;
                }
            }
            if (k == 0) {
                count[best] = 0;
                continue;
            }
            float cx = sx / k, cy = sy / k;
            float[] dist = new float[k];
            int j = 0;
            for (int i = 0, m = w.entities.size(); i < m; i++) {
                Entity z = w.entities.get(i);
                if (z.dead || !z.isZombie() || taken[i]) continue;
                float d = (float) World.hypot(z.x - cx, z.y - cy);
                if (d > 460) continue;
                taken[i] = true;
                if (j < k) dist[j++] = d;
                int c = Math.min(gh2 - 1, Math.max(0, (int) (z.y / cell))) * gw2 + Math.min(gw2 - 1, Math.max(0, (int) (z.x / cell)));
                count[c]--;
            }
            java.util.Arrays.sort(dist, 0, j);
            float spread = j == 0 ? 0 : dist[Math.min(j - 1, (int) (j * 0.8f))];
            w.clusterX[n] = cx;
            w.clusterY[n] = cy;
            w.clusterR[n] = Math.min(640, Math.max(140, spread + 110));
            w.clusterN[n] = j;
            n++;
        }
        return n;
    }

    /** Puts a zone's ring round (cx, cy): its perimeter posts, and checkpoints on the roads that cross it. */
    void placeZone(World.InfectionZone z, float cx, float cy, float r) {
        // ((11.0) Moved or grown: any wall round where it was comes down, and goes up again once it's settled.)
        if (World.hypot(cx - z.x, cy - z.y) > 120 || Math.abs(r - z.r) > 100) {
            if (z.ring != null) tearDownWall(z);
            z.stableFor = 0;
        }
        // (It keeps its name unless it's moved right away from where it was.)
        if (z.place == null || World.hypot(cx - z.x, cy - z.y) > z.r) {
            z.place = w.city.placeName(cx, cy);
            // (Two on the same street: told apart by which end of it.)
            for (World.InfectionZone o : w.infection)
                if (o != z && z.place.equals(o.place)) {
                    float dx = cx - o.x, dy = cy - o.y;
                    z.place += Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? " East" : " West") : (dy > 0 ? " South" : " North");
                    break;
                }
        }
        z.x = cx;
        z.y = cy;
        z.r = r;
        for (int p = 0; p < World.CORDON_POSTS; p++) {
            float a = World.TAU * p / World.CORDON_POSTS, px = 0, py = 0;
            boolean ok = false;
            // Each post on open ground, as near the ring as it can be.
            for (int tr = 0; tr < 6 && !ok; tr++) {
                float rr = r + (tr % 2 == 0 ? tr : -tr) * 12, aa = a + (tr % 3 - 1) * 0.06f;
                px = cx + (float) Math.cos(aa) * rr;
                py = cy + (float) Math.sin(aa) * rr;
                ok = px > 20 && py > 20 && px < w.city.worldW() - 20 && py < w.city.worldH() - 20 && !w.city.solidAt(px, py);
            }
            z.post[p * 2] = ok ? px : -1;
            z.post[p * 2 + 1] = ok ? py : -1;
            closeCall(z.postCall, p);
        }
        // The army's checkpoints: where a road runs out across the ring, just outside it. (Whoever is already on
        // their way to one goes on to the new spot with the same number.)
        // (Those still on the roads in, near enough the new ring, stay where they are, and so do their guards.)
        ArrayList<float[]> oldCp = new ArrayList<float[]>(z.checkpoints), oldSpots = new ArrayList<float[]>(z.guardSpots);
        Entity[] oldGuards = z.guards.clone();
        Dispatch.Incident[] oldCalls = z.guardCall.clone();
        for (float[] cp : z.checkpoints) w.city.checkpoints.remove(cp);
        z.checkpoints.clear();
        z.guardSpots.clear();
        java.util.Arrays.fill(z.guards, null);
        java.util.Arrays.fill(z.guardCall, null);
        for (int c = 0; c < oldCp.size(); c++) {
            float[] cp = oldCp.get(c);
            if (Math.abs(World.hypot(cp[0] - cx, cp[1] - cy) - (r + 70)) > 110 || oldSpots.size() < c * 2 + 2) continue;
            int n = z.checkpoints.size();
            z.checkpoints.add(cp);
            w.city.checkpoints.add(cp);
            for (int sd = 0; sd < 2; sd++) {
                z.guardSpots.add(oldSpots.get(c * 2 + sd));
                z.guards[n * 2 + sd] = oldGuards[c * 2 + sd];
                z.guardCall[n * 2 + sd] = oldCalls[c * 2 + sd];
                oldGuards[c * 2 + sd] = null;
                oldCalls[c * 2 + sd] = null;
            }
        }
        // (The rest are taken down; whoever was going to them goes to the new ones in their place.)
        int moved = z.checkpoints.size() * 2;
        for (int k = 0; k < oldGuards.length; k++) {
            if (oldGuards[k] == null || moved >= z.guards.length) continue;
            z.guards[moved] = oldGuards[k];
            z.guardCall[moved] = oldCalls[k];
            oldGuards[k] = null;
            oldCalls[k] = null;
            moved++;
        }
        for (int k = 0; k < 32 && z.checkpoints.size() < World.MAX_CHECKPOINTS; k++) {
            float a = World.TAU * k / 32, ux = (float) Math.cos(a), uy = (float) Math.sin(a);
            float px = cx + ux * (r + 70), py = cy + uy * (r + 70);
            int tx = (int) (px / City.T), ty = (int) (py / City.T);
            if (tx < 2 || ty < 2 || tx >= w.city.w - 2 || ty >= w.city.h - 2) continue;
            int i = ty * w.city.w + tx;
            if (w.city.tiles[i] != City.ROAD || (w.city.oneWay != null && w.city.oneWay[i] != 0) || (w.city.slipWay != null && w.city.slipWay[i] != 0)
                    || w.city.junctionAt(tx, ty)) continue;
            int dir = w.city.roadDirAt(tx, ty);
            boolean radial = (dir == 1 && Math.abs(uy) > 0.7f) || (dir == 2 && Math.abs(ux) > 0.7f);
            if (!radial) continue;
            boolean near = false;
            for (float[] o : z.checkpoints) if (World.hypot(o[0] - px, o[1] - py) < 220) near = true;
            for (float[] o : w.city.checkpoints) if (World.hypot(o[0] - px, o[1] - py) < 120) near = true;
            if (near) continue;
            float ang = dir == 1 ? (uy > 0 ? 1.5708f : -1.5708f) : (ux > 0 ? 0 : 3.1416f);
            float[] cp = {(tx + 0.5f) * City.T, (ty + 0.5f) * City.T, ang};
            z.checkpoints.add(cp);
            w.city.checkpoints.add(cp);
            for (int sd = -1; sd <= 1; sd += 2) {
                float[] q = w.city.findWalkable(cp[0] - (float) Math.sin(ang) * sd * 2.4f * City.T, cp[1] + (float) Math.cos(ang) * sd * 2.4f * City.T);
                z.guardSpots.add(q != null ? q : new float[]{cp[0], cp[1]});
            }
        }
        for (int k = 0; k < z.guards.length; k++) {
            Entity g = z.guards[k];
            if (g == null) continue;
            if (k >= z.guardSpots.size() || g.dead && !World.riding(g)) {
                if (!g.dead && (g.task == Dispatch.T_POST || (g.task == Dispatch.T_RESPOND && g.incident == z.guardCall[k]))) {
                    g.task = Dispatch.T_NONE;
                    g.incident = null;
                }
                z.guards[k] = null;
                if (z.guardCall[k] != null) z.guardCall[k].resolved = true;
                z.guardCall[k] = null;
                continue;
            }
            float[] spot = z.guardSpots.get(k);
            if (g.task == Dispatch.T_POST) {
                g.postX = spot[0];
                g.postY = spot[1];
            }
            if (z.guardCall[k] != null) {
                z.guardCall[k].checkpointSpot = spot;
                z.guardCall[k].x = spot[0];
                z.guardCall[k].y = spot[1];
            }
        }
    }

    /**
     * Soldiers onto a critical zone's checkpoints: two to each, the nearest free ones walking if it's close, a squad
     * from the nearest base by truck if not.
     */
    void manCheckpoints(World.InfectionZone z) {
        if (z.guardSpots.isEmpty()) return;
        ArrayList<Entity> squad = null;
        Dispatch.Incident ride = null;
        boolean sent = false;
        for (int s = 0; s < z.guardSpots.size() && s < z.guards.length; s++) {
            // (A truck to each checkpoint, with the pair for it: they're spread right round the zone.)
            if (s % 2 == 0 && squad != null) {
                sendSquad(squad, ride);
                sent = true;
                squad = null;
            }
            Entity g = z.guards[s];
            // (Still on it, or on the way: walking, aboard the truck, or just dropped off.)
            if (g != null && (World.riding(g) || !g.dead && (g.task == Dispatch.T_BOARD || g.task == Dispatch.T_RESPOND
                    || (g.task == Dispatch.T_POST && World.hypot(g.postX - z.guardSpots.get(s)[0], g.postY - z.guardSpots.get(s)[1]) < 4)))) continue;
            z.guards[s] = null;
            float[] spot = z.guardSpots.get(s);
            Entity best = null;
            float bd = Float.MAX_VALUE;
            for (int i = 0, m = w.entities.size(); i < m; i++) {
                Entity e = w.entities.get(i);
                if (e.dead || e.type != Entity.SOLDIER || !cordonCandidate(e) || w.controlled == e) continue;
                float d = (float) World.hypot(e.x - spot[0], e.y - spot[1]);
                if (d < bd) {
                    bd = d;
                    best = e;
                }
            }
            if (best == null) continue;
            z.guards[s] = best;
            if (bd < 700) {
                best.task = Dispatch.T_POST;
                best.postX = spot[0];
                best.postY = spot[1];
                // ((27.4) On the checkpoint: the zone's, like those on its cordon.)
                w.ops.jobs.claim(best, z, Jobs.P_POST, "at a checkpoint");
                continue;
            }
            // A long way: they go by truck, the squad for this zone's checkpoints together.
            Dispatch.Incident inc = new Dispatch.Incident();
            inc.x = spot[0];
            inc.y = spot[1];
            inc.place = "the checkpoint on " + w.city.placeName(spot[0], spot[1]);
            inc.checkpointSpot = spot;
            z.guardCall[s] = inc;
            best.task = Dispatch.T_RESPOND;
            best.incident = inc;
            if (squad == null) {
                squad = new ArrayList<Entity>();
                ride = inc;
            }
            squad.add(best);
        }
        if (squad != null) {
            sendSquad(squad, ride);
            sent = true;
        }
        if (sent && w.time > z.saidAt) {
            z.saidAt = w.time + 45;
            w.dispatch.say(Dispatch.WHO_MILITARY, null, "Military: Closing the roads into the " + z.place + " infection zone. Checkpoints going up.", z.x, z.y);
        }
    }

    /** A checkpoint's squad on its way: by truck from the base, or whatever they can get in town, or on foot. */
    void sendSquad(ArrayList<Entity> squad, Dispatch.Incident ride) {
        City.Facility base = w.city.nearestFacility(City.FACILITY_BASE, squad.get(0).x, squad.get(0).y);
        boolean atBase = base != null && World.hypot(base.x - squad.get(0).x, base.y - squad.get(0).y) < base.r + 300;
        boolean byRoad = atBase && w.fleet.troopTruck(squad, base, ride) || w.fleet.mountUp(squad, ride);
        if (!byRoad) for (Entity e : squad) {
            // (No truck to be had: they walk.)
            e.task = Dispatch.T_POST;
            e.postX = e.incident.checkpointSpot[0];
            e.postY = e.incident.checkpointSpot[1];
            e.incident = null;
        }
    }

    /** The soldiers on a zone's checkpoints go back to what they were doing. */
    void standDownGuards(World.InfectionZone z) {
        for (int s = 0; s < z.guards.length; s++) {
            Entity g = z.guards[s];
            if (g != null && !g.dead && (g.task == Dispatch.T_POST || (g.task == Dispatch.T_RESPOND && g.incident == z.guardCall[s]))) {
                w.ops.jobs.release(g, z);
                g.task = Dispatch.T_NONE;
                g.incident = null;
            }
            z.guards[s] = null;
            if (z.guardCall[s] != null) z.guardCall[s].resolved = true;
            z.guardCall[s] = null;
        }
    }

    void removeCheckpoints(World.InfectionZone z) {
        standDownGuards(z);
        for (float[] cp : z.checkpoints) w.city.checkpoints.remove(cp);
        z.checkpoints.clear();
        z.guardSpots.clear();
    }

    void closeCall(Dispatch.Incident[] calls, int p) {
        if (calls[p] != null) calls[p].resolved = true;
        calls[p] = null;
    }

    /**
     * (11.0) A quarantine wall round an infection zone that's settled: the army's engineers put up a ring of fencing
     * and concrete barriers a little outside the perimeter, a third of the way round every few seconds, across yards,
     * parks and pavements; where a road runs through it there's a gate, held by the checkpoint and the posts. It comes
     * down when the zone is lifted, or moves.
     */
    void buildWall(World.InfectionZone z) {
        if (z.ring == null) {
            float rr = z.r + 36;
            java.util.ArrayList<Integer> ring = new java.util.ArrayList<Integer>();
            int steps = Math.max(48, (int) (rr * World.TAU / (City.T * 0.5f)));
            int last = -1;
            for (int k = 0; k <= steps; k++) {
                float a = World.TAU * k / steps;
                int tx = (int) ((z.x + (float) Math.cos(a) * rr) / City.T), ty = (int) ((z.y + (float) Math.sin(a) * rr) / City.T);
                if (tx < 2 || ty < 2 || tx >= w.city.w - 2 || ty >= w.city.h - 2) continue;
                int t = ty * w.city.w + tx;
                if (t == last) continue;
                // (No gaps at the corners: a step across as well as up or down.)
                if (last >= 0 && last % w.city.w != tx && last / w.city.w != ty) ring.add(last / w.city.w * w.city.w + tx);
                ring.add(t);
                last = t;
            }
            z.ring = new int[ring.size()];
            for (int k = 0; k < z.ring.length; k++) z.ring[k] = ring.get(k);
            z.ringAt = 0;
            w.dispatch.say(Dispatch.WHO_MILITARY, null, "Military: Engineers are walling off the infection zone at " + z.place
                    + ". Gates on the roads in, under guard.", z.x, z.y);
        }
        if (z.ringAt >= z.ring.length) return;
        int end = Math.min(z.ring.length, z.ringAt + z.ring.length / 3 + 1);
        for (; z.ringAt < end; z.ringAt++) {
            int t = z.ring[z.ringAt];
            byte k = w.city.tiles[t];
            if (k != City.GRASS && k != City.SIDEWALK && k != City.PLAZA && k != City.DIRT && k != City.SAND && k != City.TREE) continue;
            if (w.city.buildingAt.get(t) >= 0) continue;
            float cx = (t % w.city.w + 0.5f) * City.T, cy = (t / w.city.w + 0.5f) * City.T;
            if (w.spatial.gatherLiving(cx, cy, 9) > 0 || w.spatial.zombieWithin(cx, cy, 9)) continue;
            z.wall.add(new int[]{t, k});
            w.city.setLiveTile(t, City.FENCE);
        }
        routesChanged();
        if (z.ringAt >= z.ring.length)
            w.dispatch.say(Dispatch.WHO_MILITARY, null, "Military: The wall round " + z.place + " is up.", z.x, z.y);
    }

    /** (11.0) The quarantine wall comes down: every tile back to what it was. */
    void tearDownWall(World.InfectionZone z) {
        for (int[] w : z.wall) if (this.w.city.tiles[w[0]] == City.FENCE) this.w.city.setLiveTile(w[0], (byte) w[1]);
        boolean had = !z.wall.isEmpty();
        z.wall.clear();
        z.ring = null;
        z.ringAt = 0;
        if (had) routesChanged();
    }

    /**
     * (14.8) A police helicopter at a fight: it marks the dead it can see for the units on the ground (as a drone does,
     * from further), moves its hover over to the worst of them, and calls in any not yet reported.
     */
    void heliSpot(Fleet.Vehicle v, float dt) {
        v.heliLook -= dt;
        v.spotMove -= dt;
        if (v.heliLook > 0) return;
        v.heliLook = 0.5f;
        int seen = 0;
        int cx0 = Math.max(0, (int) ((v.x - World.HELI_SEE) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((v.x + World.HELI_SEE) / World.CELL));
        int cy0 = Math.max(0, (int) ((v.y - World.HELI_SEE) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((v.y + World.HELI_SEE) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c], end = k + w.zCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (!o.dead && (o.x - v.x) * (o.x - v.x) + (o.y - v.y) * (o.y - v.y) < World.HELI_SEE * World.HELI_SEE) {
                        o.spottedUntil = w.time + 1.6f;
                        seen++;
                    }
                }
            }
        // Over to where they're thickest, now and then: a few of them round the fight looked at.
        if (v.spotMove <= 0) {
            v.spotMove = 8;
            float bx = v.tx, by = v.ty;
            int best = w.spatial.countZombiesNear(v.tx, v.ty, 50) + 2;
            float r = 300;
            int dx0 = Math.max(0, (int) ((v.tx - r) / World.CELL)), dx1 = Math.min(w.gw - 1, (int) ((v.tx + r) / World.CELL));
            int dy0 = Math.max(0, (int) ((v.ty - r) / World.CELL)), dy1 = Math.min(w.gh - 1, (int) ((v.ty + r) / World.CELL));
            int looked = 0;
            for (int cy = dy0; cy <= dy1 && looked < 24; cy++)
                for (int cx = dx0; cx <= dx1 && looked < 24; cx++) {
                    int c = cy * w.gw + cx;
                    if (w.zCount[c] == 0) continue;
                    Entity o = w.sorted[w.cellStart[c]];
                    if (o.dead || (o.x - v.tx) * (o.x - v.tx) + (o.y - v.ty) * (o.y - v.ty) > r * r) continue;
                    looked++;
                    int n = w.spatial.countZombiesNear(o.x, o.y, 50);
                    if (n > best) {
                        best = n;
                        bx = o.x;
                        by = o.y;
                    }
                }
            v.tx = bx;
            v.ty = by;
        }
        if (seen == 0) return;
        boolean covered = false;
        for (int i = 0; i < w.dispatch.incidents.size() && !covered; i++) {
            Dispatch.Incident inc = w.dispatch.incidents.get(i);
            if (!inc.resolved && World.hypot(inc.x - v.x, inc.y - v.y) < 250) covered = true;
        }
        Entity z = w.spatial.nearestZombie(v.x, v.y, World.HELI_SEE);
        if (!covered && z != null) w.dispatch.sighting(z.x, z.y, null);
        if (w.time - v.spotSaid > 25 && z != null) {
            v.spotSaid = w.time;
            w.dispatch.say(Dispatch.WHO_POLICE, null, "Police helicopter: Eyes on " + seen + (seen == 1 ? " of the dead" : " of the dead") + " round "
                    + w.city.placeName(z.x, z.y) + ". Marking them for units on the ground.", z.x, z.y);
        }
    }

    /**
     * (14.8) A roadblock's crowd barriers: the dead that reach the line are held there, pushing, until enough of
     * them have pushed for long enough to send it over. The living get through (officers open a gap for them).
     */
    void barrierHold(Fleet.Vehicle v, float dt) {
        if (v.barHp <= 0) return;
        float mx = (v.barX0 + v.barX1) / 2, my = (v.barY0 + v.barY1) / 2;
        float half = (float) World.hypot(v.barX1 - v.barX0, v.barY1 - v.barY0) / 2 + 4;
        float tx = (v.barX1 - v.barX0) / (2 * half - 8 + 0.001f), ty = (v.barY1 - v.barY0) / (2 * half - 8 + 0.001f);
        int cx0 = Math.max(0, (int) ((mx - half) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((mx + half) / World.CELL));
        int cy0 = Math.max(0, (int) ((my - half) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((my + half) / World.CELL));
        int pushing = 0;
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c], end = k + w.zCount[c]; k < end; k++) {
                    Entity z = w.sorted[k];
                    if (z.dead || z.hp <= 0) continue;
                    float rx = z.x - mx, ry = z.y - my;
                    if (Math.abs(rx * tx + ry * ty) > half) continue;
                    // Which side, along the road: the zone's side is "in". Held on the line's outer edge.
                    float s = (rx * v.barNx + ry * v.barNy) * v.barIn;
                    float hold = -(z.radius + 2);
                    if (s > hold && s < 6) {
                        float push = hold - s;
                        z.x += v.barNx * v.barIn * push;
                        z.y += v.barNy * v.barIn * push;
                        z.vx = z.vy = 0;
                        pushing++;
                    }
                }
            }
        if (pushing == 0) return;
        v.barHp -= dt * pushing * (pushing > 6 ? 1.4f : 1);
        if (v.barHp <= 0) {
            v.barHp = 0;
            v.barDown = true;
            w.emit(Sfx.THUD, mx, my);
            w.dispatch.say(Dispatch.WHO_POLICE, null, "Roadblock on " + w.city.placeName(mx, my) + ": They've pushed the barriers over!", mx, my);
        }
    }

    /** (11.0) The ground has changed: the ways worked out over it are worked out again when next needed. */
    void routesChanged() {
        // ((13.3) Not all thrown away: the quarantine walls go up a stretch at a time, and each stretch had every
        // station's way on foot worked out again at once, holding a big game up for most of a second each time.)
        w.city.fieldsStale();
        for (City.Building d : w.doorPaths) d.field = null;
        w.doorPaths.clear();
        w.errandSpots = null;
    }

    void liftZone(World.InfectionZone z, String say) {
        if (z.ring != null) tearDownWall(z);
        for (int p = 0; p < World.CORDON_POSTS; p++) closeCall(z.postCall, p);
        removeCheckpoints(z);
        for (int i = 0, m = w.entities.size(); i < m; i++) {
            Entity e = w.entities.get(i);
            if (e.task == Dispatch.T_CORDON && zoneOfPost(e.postX, e.postY) == z) e.task = Dispatch.T_NONE;
            w.ops.jobs.release(e, z);
        }
        w.infection.remove(z);
        if (say != null) {
            w.dispatch.say(Dispatch.WHO_POLICE, null, say, z.x, z.y);
            w.dispatch.callout(z.x, z.y, "ZONE CLEARED", 0xFF7AD07A);
        }
    }

    /** The zone a perimeter post belongs to, or null. */
    World.InfectionZone zoneOfPost(float x, float y) {
        for (World.InfectionZone z : w.infection)
            for (int p = 0; p < World.CORDON_POSTS; p++)
                if (z.post[p * 2] >= 0 && Math.abs(z.post[p * 2] - x) < 200 && Math.abs(z.post[p * 2 + 1] - y) < 200
                        && World.hypot(z.post[p * 2] - x, z.post[p * 2 + 1] - y) < 200) return z;
        return null;
    }

    int postFor(World.InfectionZone z, float x, float y) {
        int best = -1;
        float bd = 200;
        for (int p = 0; p < World.CORDON_POSTS; p++) {
            if (z.post[p * 2] < 0) continue;
            float d = (float) World.hypot(z.post[p * 2] - x, z.post[p * 2 + 1] - y);
            if (d < bd) {
                bd = d;
                best = p;
            }
        }
        return best;
    }

    /** Police and soldiers with nothing to do and rounds to shoot (not the specialists). */
    boolean cordonCandidate(Entity e) {
        if (e.task != Dispatch.T_NONE || e.scout || e.aloft || e.ammo + e.reserve <= 0) return false;
        // ((27.4) Not one another job has.)
        if (w.ops.jobs.keeps(e, Jobs.P_POST)) return false;
        if (e.type == Entity.COP) return e.agency == 0 && e.role != Entity.ROLE_SAR && e.role != Entity.ROLE_SWAT;
        return e.type == Entity.SOLDIER && (e.role == Entity.ROLE_RIFLE || e.role == Entity.ROLE_GUNNER);
    }
}
