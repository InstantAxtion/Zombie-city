package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Police, soldiers and armed civilians: what one of them does each step - deciding, holding, falling back, cover, squads, medics, mortars, mopping up.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class ArmedAI {
    private final World w;

    ArmedAI(World w) {
        this.w = w;
    }

    boolean commanded(Entity e) {
        for (int i = 0, n = w.commanders.size(); i < n; i++) {
            Entity c = w.commanders.get(i);
            if (c != e && (c.x - e.x) * (c.x - e.x) + (c.y - e.y) * (c.y - e.y) < 170 * 170) return true;
        }
        return false;
    }

    /**
     * A few times a second: the dead within SENSE_R of someone - how many, from how many of eight sides, how near the
     * nearest - and the best way out: of sixteen directions, the open one with the fewest of them (the near ones
     * counting for more) along it, holding to the last way out unless another is clearly better.
     */
    void sense(Entity e) {
        if (w.time < e.senseAt) return;
        e.senseAt = w.time + 0.2f + (e.nameSeed & 3) * 0.02f;
        float r = World.SENSE_R;
        java.util.Arrays.fill(w.cone, 0);
        int n = 0, sides = 0;
        float near = 999;
        int cx0 = Math.max(0, (int) ((e.x - r) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((e.x + r) / World.CELL));
        int cy0 = Math.max(0, (int) ((e.y - r) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((e.y + r) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c], end = w.cellStart[c] + w.zCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o.dead || o.hp <= 0 || !o.isZombie()) continue;
                    float dx = o.x - e.x, dy = o.y - e.y, d2 = dx * dx + dy * dy;
                    if (d2 > r * r) continue;
                    float d = (float) Math.sqrt(d2) + 0.001f;
                    n++;
                    near = Math.min(near, d);
                    int sec = ((int) Math.floor((Math.atan2(dy, dx) + Math.PI) / (Math.PI / 4))) & 7;
                    sides |= 1 << sec;
                    float wgt = 1 + 2 * (r - d) / r;
                    for (int q = 0; q < World.DIRS; q++) {
                        float dot = (dx * World.DIR_X[q] + dy * World.DIR_Y[q]) / d;
                        if (dot > 0.35f) w.cone[q] += wgt * (dot - 0.35f) / 0.65f;
                    }
                }
            }
        e.tN = n;
        e.tSides = Integer.bitCount(sides);
        e.tNear = near;
        if (n == 0) return;
        int best = -1;
        float bs = -Float.MAX_VALUE;
        for (int q = 0; q < World.DIRS; q++) {
            float score = -w.cone[q];
            if (w.city.circleBlocked(e.x + World.DIR_X[q] * 14, e.y + World.DIR_Y[q] * 14, e.radius)) score -= 100;
            else if (w.city.circleBlocked(e.x + World.DIR_X[q] * 32, e.y + World.DIR_Y[q] * 32, e.radius)) score -= 6;
            if (q == e.escK) score += 0.6f;
            if (score > bs) {
                bs = score;
                best = q;
            }
        }
        e.escK = best;
        e.escX = World.DIR_X[best];
        e.escY = World.DIR_Y[best];
    }

    /** Which way to step back from a target: by the open side when there are several of the dead about. */
    float awayX(Entity e, float ddx, float d) {
        if (!World.TACTICS) return -ddx / d;
        sense(e);
        return e.tN >= 3 ? e.escX * 0.75f - ddx / d * 0.25f : -ddx / d;
    }

    float awayY(Entity e, float ddy, float d) {
        if (!World.TACTICS) return -ddy / d;
        return e.tN >= 3 ? e.escY * 0.75f - ddy / d * 0.25f : -ddy / d;
    }

    /**
     * A police officer or soldier in a fight. Being surrounded - the dead closing in from five sides or more - or about
     * to be overrun: they run for the open side before the ring closes, shooting as they go, and keep going until
     * they have room again (the old way was to stand and shoot until it was on them, then step back from the nearest
     * into the rest). With a lull, they reload a gun that's getting low, rather than run dry with the dead at arm's
     * length. True when this took care of their step.
     */
    boolean breakOut(Entity e, Entity t, float d, float range, float dt) {
        if (e.ride != null || e.rig != null) return false;
        sense(e);
        // A lull, with a gun getting low: reload now, not with the dead on top of them.
        // ((27.4) Not all at once: with someone alongside already reloading, the rest keep their guns up a moment.)
        if (e.reload <= 0 && e.reserve > 0 && e.ammo > 0 && e.ammo <= Math.max(2, e.magSize * 0.4f) && e.tNear > 85
                && (e.ammo <= 2 || !w.combat.mateReloading(e, Combat.MATE_R))) {
            e.reload = w.combat.reloadTime(e);
            w.emit(Sfx.RELOAD, e.x, e.y);
        }
        // (Behind sandbags or a safe zone's wall, and guards on a gate: they hold.)
        if (e.task == Dispatch.T_GUARD || dugIn(e)) return false;
        boolean surrounded = e.tN >= 6 && e.tSides >= 5 && e.tNear < 75;
        boolean overrun = e.tN >= 14 && e.tNear < 55;
        if (surrounded || overrun) e.breakT = 2.2f;
        if (e.breakT <= 0) return false;
        e.breakT -= dt;
        // Room again: back to the fight.
        if (e.tNear > 70 && !surrounded && !overrun) {
            e.breakT = 0;
            return false;
        }
        if (surrounded || overrun) {
            if (e.breakCount++ == 0) w.breakouts++;
            if (w.time - w.breakSaidAt > 12) {
                w.breakSaidAt = w.time;
                w.dispatch.chatter(e.type == Entity.SOLDIER ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, e,
                        "They're all round us! Fall back - this way!", e.x, e.y);
            }
        }
        w.movement.steer(e, e.escX, e.escY, e.runSpeed);
        float l = (float) Math.sqrt(e.mx * e.mx + e.my * e.my) + 0.001f;
        e.mx /= l;
        e.my /= l;
        // Shooting as they go: whatever's in the way first.
        Entity ahead = null;
        float ad = Float.MAX_VALUE;
        for (int i = 0, n = w.spatial.gatherZ(e.x, e.y, 60); i < n; i++) {
            Entity o = w.gathered[i];
            float ox = o.x - e.x, oy = o.y - e.y, od = (float) Math.sqrt(ox * ox + oy * oy) + 0.001f;
            if ((ox * e.escX + oy * e.escY) / od < 0.5f) continue;
            if (od < ad) {
                ad = od;
                ahead = o;
            }
        }
        if (ahead != null) w.combat.aimAndFire(e, ahead, ad, range, dt);
        else if (d < range) w.combat.aimAndFire(e, t, d, range, dt);
        return true;
    }

    boolean wallsRound(float x, float y) {
        int walls = 0;
        for (int q = 0; q < World.DIRS; q += 2) if (w.city.circleBlocked(x + World.DIR_X[q] * 17, y + World.DIR_Y[q] * 17, 3)) walls++;
        return walls >= 4;
    }

    /**
     * ((24.0) Holding a post that's dug in - sandbags round it - and on it. Judged by the post, not where they stand:
     * a gunner who'd stepped a little way out of the horseshoe no longer counted as behind it, broke and ran through
     * the horde, and both of a nest's crew died that way.)
     */
    boolean dugIn(Entity e) {
        if (e.task != Dispatch.T_HOLD || w.defences.nests <= 0) return false;
        if (Math.abs(e.x - e.postX) > 24 || Math.abs(e.y - e.postY) > 24) return false;
        return wallsRound(e.postX, e.postY);
    }

    /** Every two and a half minutes of an outbreak: where things stand, in a line on the radio. */
    void sitrep() {
        if (!w.outbreak || w.outbreakTime < 150 || ((int) w.outbreakTime) % 150 != 0) return;
        int hordes = 0, biggest = 0, alphaN = 0;
        Entity big = null;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || !e.isZombie()) continue;
            if (e.alpha) alphaN++;
            if (e.leadsHorde && e.hordeSize >= 8) {
                hordes++;
                if (e.hordeSize > biggest) {
                    biggest = e.hordeSize;
                    big = e;
                }
            }
        }
        String text = "Situation: " + w.zombieCount() + " of the dead"
                + (hordes > 0 ? ", " + World.count(hordes, "horde", "hordes") + " (the biggest about " + biggest / 5 * 5 + " at " + w.city.placeName(big.x, big.y) + ")" : "")
                + (alphaN > 0 ? ", " + World.count(alphaN, "alpha", "alphas") : "") + ". " + (w.counts[Entity.COP] + w.counts[Entity.SOLDIER])
                + " police and soldiers standing, " + w.humans + " people alive.";
        w.dispatch.say(Dispatch.WHO_INFO, null, text, big != null ? big.x : w.city.worldW() / 2, big != null ? big.y : w.city.worldH() / 2);
    }

    /**
     * Once a second (acting every five). One of the dead standing inside something solid - knocked into a wall by a
     * blast, turned where they fell - for four looks running is wedged there, where nothing can see or reach it: out
     * to the nearest open ground. (Not anyone climbing or vaulting, who's only passing through.)
     */
    void unstick() {
        if ((w.unstickCd -= 1) > 0) return;
        w.unstickCd = 5;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || !e.isZombie() || e.ride != null || e.vaultT > 0) {
                e.inSolid = 0;
                continue;
            }
            int ti = w.city.tileIndex(e.x, e.y);
            // (Solid there, that is: not the floor of a building they've walked into.)
            if (ti < 0 || !w.city.solid[ti] || !w.city.solidAt(e.x, e.y)) {
                e.inSolid = 0;
                continue;
            }
            if (++e.inSolid < 4) continue;
            e.inSolid = 0;
            float[] out = w.city.findWalkable(e.x, e.y);
            if (out == null) continue;
            e.x = out[0];
            e.y = out[1];
            w.unstuck++;
        }
    }

    /**
     * Once a second. With only a handful of the dead left in an outbreak, they're hunted: spotters call out where each
     * one is every few seconds - so nobody has to stumble on the last of them - and a unit is sent after any with
     * nobody near.
     */
    void mopUp() {
        int z = w.zombieCount();
        if (!w.outbreak || z == 0 || z > World.MOP_UP || w.outbreakTime < 120) {
            w.mopping = false;
            if (w.lastDead.length > 0) w.lastDead = new float[0];
            return;
        }
        if (!w.mopping) {
            w.mopping = true;
            w.mopCd = 0;
            w.dispatch.say(Dispatch.WHO_INFO, null, "Only " + z + " of the dead left. Spotters are calling out where they are.", w.city.worldW() / 2, w.city.worldH() / 2);
        }
        float[] at = new float[z * 2];
        int n = 0;
        boolean call = (w.mopCd -= 1) <= 0;
        if (call) w.mopCd = 6;
        for (int i = 0, m = w.entities.size(); i < m && n < z; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || e.hp <= 0 || !e.isZombie()) continue;
            at[n * 2] = e.x;
            at[n * 2 + 1] = e.y;
            n++;
            e.spottedUntil = Math.max(e.spottedUntil, w.time + 3);
            // (One wedged inside a wall - knocked there by a blast, or turned there - where nobody can see or reach it:
            // out into the open.)
            if (w.city.solidAt(e.x, e.y) && e.ride == null && e.vaultT <= 0) {
                float[] out = w.city.findWalkable(e.x, e.y);
                if (out != null) {
                    e.x = out[0];
                    e.y = out[1];
                    at[n * 2 - 2] = e.x;
                    at[n * 2 - 1] = e.y;
                }
            }
            // (One that hasn't stirred from inside a building for forty seconds of this - shut in a back room, or just
            // standing there - is driven out through its door.)
            if (World.hypot(e.x - e.mopX, e.y - e.mopY) > 12) {
                e.mopX = e.x;
                e.mopY = e.y;
                e.mopT = 0;
            } else if (++e.mopT >= 40) {
                e.mopT = 0;
                City.Building in = w.city.insideOf(e.x, e.y);
                if (in != null && in.doorX > 0) {
                    float cx = (in.x0 + in.x1) / 2, cy = (in.y0 + in.y1) / 2;
                    float dx = in.doorX - cx, dy = in.doorY - cy, dl = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
                    float[] out = null;
                    for (float step = 0; step <= 40 && out == null; step += 10) {
                        float ox = in.doorX + dx / dl * step, oy = in.doorY + dy / dl * step;
                        if (w.city.insideOf(ox, oy) == null && !w.city.solidAt(ox, oy)) out = w.city.findWalkable(ox, oy);
                    }
                    if (out != null) {
                        e.x = out[0];
                        e.y = out[1];
                        e.path = null;
                        w.flushed++;
                    }
                }
            }
            if (!call) continue;
            Entity hunter = w.spatial.nearestArmed(e.x, e.y, 260);
            if (hunter == null) {
                w.dispatch.sighting(e.x, e.y, null);
                continue;
            }
            // It hears them and comes for them: out of a building by its door, round a corner into their sights.
            if (e.noiseTimer <= 0) {
                e.noiseX = hunter.x;
                e.noiseY = hunter.y;
                e.noiseTimer = 8;
            }
            // And with no shot at it from where they are - it's indoors, or round the back - one of them who isn't
            // under orders to stay put goes in after it. (The end of an outbreak used to be a wait outside a building.)
            if (!w.city.los(hunter.x, hunter.y, e.x, e.y)) {
                Entity goer = null;
                float gd = Float.MAX_VALUE;
                for (int k = 0, m2 = w.spatial.gatherArmed(e.x, e.y, 320); k < m2; k++) {
                    Entity o = w.gathered[k];
                    if (o.dead || o == w.controlled || o.ride != null || (o.type != Entity.COP && o.type != Entity.SOLDIER)) continue;
                    if (o.task == Dispatch.T_HOLD || o.task == Dispatch.T_MOVE || o.task == Dispatch.T_SQUAD || o.ammo + o.reserve <= 0) continue;
                    float d = (o.x - e.x) * (o.x - e.x) + (o.y - e.y) * (o.y - e.y);
                    if (d < gd) {
                        gd = d;
                        goer = o;
                    }
                }
                if (goer != null) {
                    w.sweepers.clear();
                    w.sweepers.add(goer);
                    w.dispatch.order(w.sweepers, e.x, e.y);
                    goer.sweepT = 40;
                }
            }
        }
        // (Those sent in go back to their duties once it's done, or after a while.)
        for (int i = 0, m = w.entities.size(); i < m; i++) {
            Entity o = w.entities.get(i);
            if (o.sweepT <= 0) continue;
            o.sweepT -= 1;
            if (o.sweepT <= 0 && o.task == Dispatch.T_MOVE && !o.dead) {
                w.sweepers.clear();
                w.sweepers.add(o);
                w.dispatch.dismiss(w.sweepers);
            }
        }
        w.lastDead = n * 2 == at.length ? at : Arrays.copyOf(at, n * 2);
    }

    /** Is something solid just in front of them (a car, a wall, a tree, the corner of a building) towards (fx, fy)? */
    boolean inCover(Entity e, float fx, float fy) {
        float dx = fx - e.x, dy = fy - e.y, d = (float) Math.sqrt(dx * dx + dy * dy);
        if (d < 30) return false;
        float ux = dx / d, uy = dy / d;
        for (float st = 5; st <= 13; st += 4) {
            float px = e.x + ux * st, py = e.y + uy * st;
            if (w.city.solidAt(px, py)) return true;
            int ti = w.city.tileIndex(px, py);
            if (ti >= 0 && (w.city.tiles[ti] == City.CAR || w.city.tiles[ti] == City.TREE)) return true;
        }
        return false;
    }

    /**
     * Someone trading shots with an armed enemy: every couple of seconds, unless they're in cover already, a spot close
     * by with something solid between them and the enemy and still a line to shoot along - and there they go, firing
     * as they move. Pinned down by fire they keep their heads down: to cover at once, or slower without it. Close in
     * (a fight at arm's length) there's no time for it.
     */
    void coverStep(Entity e, float dt) {
        if (e.suppress > 0) e.suppress = Math.max(0, e.suppress - dt * 0.35f);
        if (e == w.controlled || e.ride != null || e.isUndead() || w.time - e.lastAimT > 0.6f) {
            e.coverX = -1;
            return;
        }
        float fx = e.coverFromX, fy = e.coverFromY;
        float d = (float) World.hypot(fx - e.x, fy - e.y);
        if (d < 50) {
            e.coverX = -1;
            return;
        }
        e.coverT -= dt;
        if (e.suppress > 0.6f && e.coverX < 0 && e.coverT > 0.5f) e.coverT = 0.5f;
        if (e.coverT <= 0) {
            e.coverT = 2 + w.rnd.nextFloat();
            if (!inCover(e, fx, fy)) {
                e.coverX = -1;
                float best = Float.MAX_VALUE;
                float px = -(fy - e.y) / d * 7, py = (fx - e.x) / d * 7;
                for (int k = 0; k < 20; k++) {
                    float a = w.rnd.nextFloat() * World.TAU, r = 8 + w.rnd.nextFloat() * 38;
                    float cx = e.x + (float) Math.cos(a) * r, cy = e.y + (float) Math.sin(a) * r;
                    // (Not towards them.)
                    if ((cx - e.x) * (fx - e.x) + (cy - e.y) * (fy - e.y) > r * d * 0.6f) continue;
                    if (w.city.circleBlocked(cx, cy, e.radius)) continue;
                    // (A line to shoot along from there - or from a step to either side, leaning out round a corner.)
                    if (!w.city.los(cx, cy, fx, fy) && !w.city.los(cx + px, cy + py, fx, fy) && !w.city.los(cx - px, cy - py, fx, fy)) continue;
                    Entity probe = w.coverProbe;
                    probe.x = cx;
                    probe.y = cy;
                    if (!inCover(probe, fx, fy)) continue;
                    if (r < best) {
                        best = r;
                        e.coverX = cx;
                        e.coverY = cy;
                    }
                }
            }
        }
        if (e.coverX >= 0) {
            float cx = e.coverX - e.x, cy = e.coverY - e.y, cd = (float) Math.sqrt(cx * cx + cy * cy);
            if (cd > 2.5f) {
                e.mx = cx / cd;
                e.my = cy / cd;
                e.want = Math.max(e.want, e.suppress > 0.5f ? e.runSpeed * 0.8f : e.speed);
            } else e.want = 0;
        } else if (e.suppress > 0.6f) e.want *= 0.4f;
    }

    /**
     * (14.2) Radios for backup when outnumbered (four or more of the dead close, over two and a half to each gun there),
     * or badly hurt with two or more on them. Looked at every few seconds whatever they're doing: before, only while
     * they had rounds and one of the dead in their sights - so those who'd run dry, were falling back, or had lost
     * sight of them never called - and only with six or more round them, so an officer on their own against four or
     * five never did.
     */
    void maybeCallBackup(Entity e) {
        if (e.callCd > 0 || e.role == Entity.ROLE_PILOT || e.task == Dispatch.T_BOARD || e == w.controlled || e.rig != null) return;
        e.callCd = 3;
        int near = w.spatial.countZombiesNear(e.x, e.y, 110);
        if (near < 2) return;
        int guns = Math.max(1, w.spatial.armedNear(e.x, e.y, 130));
        boolean outnumbered = near >= 4 && near > guns * 2.5f;
        boolean hurt = e.hp < e.maxHp * 0.4f && near >= 2;
        if (!outnumbered && !hurt) return;
        e.callCd = 35;
        w.dispatch.unitBackup(e, near);
    }

    /**
     * (14.5) Once the outbreak is on the news and the army is in it, it sets up a tent hospital behind the lines: by
     * the base (or else the police headquarters), where wounded police and soldiers go to be patched up. Never the
     * bitten.
     */
    void fieldHospital() {
        if (!Float.isNaN(w.hospitalX) || w.alert < 2 || !w.outbreak) return;
        City.Facility f = w.city.mainBase();
        if (f == null || f.fallen) f = w.city.nearestFacility(City.FACILITY_POLICE, w.city.worldW() / 2, w.city.worldH() / 2);
        if (f == null) return;
        float[] p = w.city.findWalkable(f.gateX > 0 ? f.gateX + (f.x - f.gateX) * 0.4f : f.x, f.gateY > 0 ? f.gateY + (f.y - f.gateY) * 0.4f : f.y + f.r);
        if (p == null) return;
        w.hospitalX = p[0];
        w.hospitalY = p[1];
        w.dispatch.say(Dispatch.WHO_MILITARY, null, "Military: A field hospital is up at " + f.name + ". Wounded units, fall back there to be patched up.", p[0], p[1]);
        w.stats.timeline("Field hospital set up at " + f.name, p[0], p[1]);
    }

    /**
     * (14.5) Badly hurt and out of the fight: back to the field hospital (if it's in reach), a few seconds there, and
     * out good as new. True while busy with it. ((27.5) Wherever there's care: the command post's aid station, the
     * field hospital, a hospital, or else their own station or base (FleetMedevac.careFor). Close by and clear of the
     * dead, they walk there; a long way off or in the thick of it, they wait where they are for an ambulance (the army's
     * armoured one for soldiers) to fetch them. Patched up there a little at a time - their card says how far - and
     * back to duty with full magazines.)
     */
    boolean hospitalStep(Entity e, float dt) {
        if (e.medCd > 0) e.medCd -= dt;
        if (e.infected || e == w.controlled || e.ride != null) {
            if (e.task == Dispatch.T_HEAL && e.ride == null) endHeal(e);
            return false;
        }
        if (e.task != Dispatch.T_HEAL) {
            e.waitPickup = e.inCare = false;
            if (e.hp >= e.maxHp * WOUNDED || e.medCd > 0 || (e.task != Dispatch.T_NONE && e.task != Dispatch.T_RESPOND && e.task != Dispatch.T_POST
                    && e.task != Dispatch.T_CORDON && e.task != Dispatch.T_GUARD)) return false;
            float[] care = w.fleet.medevac.careFor(e, e.x, e.y);
            if (care == null) return false;
            float d = (float) World.hypot(care[0] - e.x, care[1] - e.y);
            // ((27.5) In the thick of it, they no longer just fight on till they drop: they call for a ride out and keep
            // fighting where they are till it comes.)
            boolean fight = w.spatial.nearest(e, 90, true, false) != null;
            w.dispatch.releaseForResupply(e);
            // (Off their car's crew: it goes on without them.)
            if (e.rig != null) e.rig.squad.remove(e);
            e.rig = null;
            e.task = Dispatch.T_HEAL;
            e.hospitalT = 0;
            e.pickupT = 0;
            e.waitPickup = fight || d > WALK_TO_CARE || w.countZombiesNear(e.x, e.y, 200) >= 3;
            if (w.time - e.medSaid > 30) {
                e.medSaid = w.time;
                Ops.Post aid = w.ops.aidFor(e, w.hospitalX, w.hospitalY);
                w.dispatch.chatter(e.type == Entity.SOLDIER ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, e,
                        e.waitPickup ? "I'm hit bad. Need a medevac on " + w.city.placeName(e.x, e.y) + "."
                                : aid != null ? "I'm hit bad. Falling back to the command post." : "I'm hit bad. Falling back to get patched up.", e.x, e.y);
            }
            if (fight) return false;
        }
        // (Patched up on the way by a medic: back to it.)
        if (!e.inCare && e.hp >= e.maxHp * 0.95f) {
            endHeal(e);
            return false;
        }
        if (e.waitPickup) {
            e.pickupT += dt;
            Fleet.Vehicle v = e.casevac;
            // (Nobody came: they make their own way, if it's within walking distance at all.)
            if (v == null && e.pickupT > PICKUP_WAIT) {
                e.waitPickup = false;
                float[] care = w.fleet.medevac.careFor(e, e.x, e.y);
                if (care == null || World.hypot(care[0] - e.x, care[1] - e.y) > World.HOSPITAL_REACH) {
                    endHeal(e);
                    e.medCd = 60;
                    return false;
                }
            } else {
                // (The ambulance is here: over to it and in.)
                if (v != null && v.medPhase == FleetMedevac.MP_LOAD && World.hypot(v.x - e.x, v.y - e.y) < 220) {
                    e.aiming = false;
                    w.wilds.walkToSpot(e, v.x, v.y, e.runSpeed * 0.8f);
                    return true;
                }
                if (w.spatial.nearest(e, 90, true, false) != null) return false;
                w.movement.steer(e, 0, 0, 0);
                e.aiming = false;
                return true;
            }
        }
        Ops.Post aid = w.ops.aidFor(e, w.hospitalX, w.hospitalY);
        float[] care = w.fleet.medevac.careFor(e, e.x, e.y);
        if (care == null) {
            endHeal(e);
            return false;
        }
        if (!e.inCare && w.spatial.nearest(e, 50, true, false) != null) return false;
        float hospitalX = care[0], hospitalY = care[1];
        float dx = hospitalX - e.x, dy = hospitalY - e.y, d = (float) Math.sqrt(dx * dx + dy * dy);
        if (d > 40 && !(e.blocked && d < 60)) {
            e.aiming = false;
            e.inCare = false;
            w.wilds.walkToSpot(e, hospitalX, hospitalY, e.runSpeed * 0.8f);
            return true;
        }
        w.movement.steer(e, 0, 0, 0);
        e.aiming = false;
        e.inCare = true;
        e.hospitalT += dt;
        e.hp = Math.min(e.maxHp, e.hp + e.maxHp * dt / CARE_TIME);
        if (e.hp >= e.maxHp) {
            e.hp = e.maxHp;
            // (Back to duty, magazines full.)
            if (e.magSize > 0) {
                e.reserve = Math.max(e.reserve, e.magSize * 5);
                e.ammo = Math.max(e.ammo, e.magSize);
                e.outOfAmmoSaid = false;
            }
            endHeal(e);
            w.treated++;
            w.returnedToDuty++;
            if (aid != null) aid.treated++;
        }
        return true;
    }

    /** ((27.5) How far they'd walk to care before waiting for a ride instead, and how long they wait for one. */
    static final float WALK_TO_CARE = 600, PICKUP_WAIT = 90;
    /** ((27.5) Badly hurt, and the seconds it takes to patch them up. */
    static final float WOUNDED = 0.35f, CARE_TIME = 30;

    private void endHeal(Entity e) {
        if (e.task == Dispatch.T_HEAL) e.task = Dispatch.T_NONE;
        e.hospitalT = 0;
        e.waitPickup = e.inCare = false;
        e.casevac = null;
    }

    /**
     * (14.7) One of your squad: in a wedge behind you as you move, firing at whatever you're firing at, or at any of
     * the dead coming at them. Let go (back to their own orders) once you stop playing that officer or soldier.
     */
    boolean squadStep(Entity e, float dt) {
        Entity c = e.commandedBy;
        if (c == null || c != w.controlled || (c.dead && w.controlledCar == null)) {
            e.commandedBy = null;
            e.task = Dispatch.T_NONE;
            return false;
        }
        // (You've driven off without them: they look after themselves till you're back.)
        if (w.controlledCar != null) return false;
        Entity t = null;
        if (w.ctrlAttack && w.ctrlTarget != null && !w.ctrlTarget.dead && w.ctrlTarget.hp > 0
                && World.hypot(w.ctrlTarget.x - e.x, w.ctrlTarget.y - e.y) < 230 && w.city.los(e.x, e.y, w.ctrlTarget.x, w.ctrlTarget.y)) t = w.ctrlTarget;
        if (t == null && (e.ammo > 0 || e.reserve > 0)) {
            Entity z = w.combat.pickTarget(e, 200);
            if (z != null && (w.ctrlAttack || World.hypot(z.x - e.x, z.y - e.y) < 140)) t = z;
        }
        // Their place: a wedge behind you, the way you're facing.
        float hx = (float) Math.cos(c.angle), hy = (float) Math.sin(c.angle);
        int m = e.squadSlot + 1;
        float back = 14 + (m / 2) * 12, side = (m % 2 == 0 ? 1 : -1) * (9 + (m / 2) * 8);
        float sx = c.x - hx * back - hy * side, sy = c.y - hy * back + hx * side;
        float dx = sx - e.x, dy = sy - e.y, sd = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
        if (t != null) {
            float d = (float) World.hypot(t.x - e.x, t.y - e.y);
            w.combat.aimAndFire(e, t, d, 230, dt);
            // (Keeping up while they shoot, if you've moved off.)
            if (sd > 50) w.movement.steer(e, dx / sd, dy / sd, e.speed);
            else w.movement.steer(e, 0, 0, 0);
            return true;
        }
        e.aiming = false;
        if (sd > 6 && !w.city.solidAt(sx, sy)) {
            float pace = Math.min(e.runSpeed, c.want + sd * 0.9f);
            if (e.blocked || (sd > 40 && !w.city.los(e.x, e.y, sx, sy))) w.wilds.walkToSpot(e, sx, sy, pace);
            else w.movement.steer(e, dx / sd, dy / sd, pace);
        } else {
            w.movement.steer(e, 0, 0, 0);
            e.angle = World.turn(e.angle, c.angle, dt * 3);
        }
        return true;
    }

    /**
     * (14.0) A mortar team: with none of the dead close, at the biggest pack of them at a call a few streets off, a
     * shell every few seconds. Never with police or soldiers near where it lands; near civilians only when the dead
     * there far outnumber them - and a shell can land a little off. True when it fired.
     * ((14.9) From its pit at the base, out to further; at the thickest of the dead round a call that's clear of our
     * own people (the middle of the pack used to be ruled out whenever a soldier was anywhere near it, which at a
     * call was nearly always); and each new fire mission called in.)
     * ((15.4) Only the rounds they carry, no more from the base: once they're all fired, the team leaves its pit
     * and goes to fight with the rest.)
     */
    boolean mortarStep(Entity e, float dt) {
        if (e.grenades <= 0) {
            if (e.task == Dispatch.T_POST) {
                e.task = Dispatch.T_NONE;
                w.dispatch.chatter(Dispatch.WHO_MILITARY, e, "Mortars: That's our last round. Leaving the tube, joining the fight.", e.x, e.y);
                w.stats.timeline("A mortar team fired its last round and joined the fight", e.x, e.y);
            }
            return false;
        }
        if (e.grenadeCd > 0 || w.spatial.nearest(e, 90, true, false) != null) return false;
        float bestX = 0, bestY = 0;
        int best = World.MORTAR_PACK - 1;
        Dispatch.Incident at = null;
        for (int i = 0; i < w.dispatch.incidents.size(); i++) {
            Dispatch.Incident inc = w.dispatch.incidents.get(i);
            if (inc.resolved) continue;
            float d = (float) World.hypot(inc.x - e.x, inc.y - e.y);
            if (d < World.MORTAR_MIN - 200 || d > World.MORTAR_MAX + 200) continue;
            // (Where they're thickest round the call: the middle of them, and a few of them looked at too.)
            float[] mid = w.spatial.packNear(inc.x, inc.y, 160);
            if (mid == null) continue;
            for (int k = 0; k < 6; k++) {
                float cx = mid[0], cy = mid[1];
                if (k > 0) {
                    Entity z = w.spatial.nearestZombie(mid[0] + (w.rnd.nextFloat() * 2 - 1) * 140, mid[1] + (w.rnd.nextFloat() * 2 - 1) * 140, 60);
                    if (z == null) continue;
                    cx = z.x;
                    cy = z.y;
                }
                float ad = (float) World.hypot(cx - e.x, cy - e.y);
                if (ad < World.MORTAR_MIN || ad > World.MORTAR_MAX) continue;
                int n = w.spatial.countZombiesNear(cx, cy, World.MORTAR_BLAST);
                if (n <= best) continue;
                if (w.spatial.armedNear(cx, cy, World.MORTAR_BLAST + 15) > 0) continue;
                if (w.spatial.countCiviliansNear(cx, cy, World.MORTAR_BLAST + 10) * 3 > n) continue;
                best = n;
                bestX = cx;
                bestY = cy;
                at = inc;
            }
        }
        e.grenadeCd = 1.5f;
        if (at == null) return false;
        w.movement.steer(e, 0, 0, 0);
        e.aiming = false;
        e.angle = (float) Math.atan2(bestY - e.y, bestX - e.x);
        e.grenades--;
        w.mortarShots++;
        e.grenadeCd = World.MORTAR_EVERY;
        float d = (float) World.hypot(bestX - e.x, bestY - e.y);
        World.Grenade g = new World.Grenade();
        g.sx = e.x;
        g.sy = e.y;
        // (Not quite where it's aimed: further off, further out.)
        float spread = 8 + d * 0.03f;
        g.tx = bestX + (w.rnd.nextFloat() * 2 - 1) * spread;
        g.ty = bestY + (w.rnd.nextFloat() * 2 - 1) * spread;
        g.dur = 2.2f + d / 450;
        g.shell = true;
        g.owner = e;
        g.aimX = bestX;
        g.aimY = bestY;
        w.grenades.add(g);
        w.emit(Sfx.LAUNCHER, e.x, e.y);
        w.noise.noise(e.x, e.y, 160);
        // ((14.9) Plain to see where it's fired from: a flash at the tube and a cloud of smoke going up.)
        float fx = e.x + (float) Math.cos(e.angle) * 4, fy = e.y + (float) Math.sin(e.angle) * 4;
        w.effects.particle(fx, fy, 0, 0, 0.14f, 6, 0xFFFFE9A0, World.P_FLASH);
        for (int k = 0; k < 7; k++)
            w.effects.particle(fx + (w.rnd.nextFloat() - 0.5f) * 4, fy + (w.rnd.nextFloat() - 0.5f) * 4, (w.rnd.nextFloat() - 0.5f) * 10, -6 - w.rnd.nextFloat() * 10,
                    1.6f + w.rnd.nextFloat(), 4 + w.rnd.nextFloat() * 3, 0xFFB8B4AA, World.P_SMOKE);
        String place = w.city.placeName(bestX, bestY);
        if (w.time - e.lastShot > 30) {
            w.dispatch.say(Dispatch.WHO_MILITARY, e, "Mortars: Fire mission, " + best + " of the dead on " + place + ". Rounds on the way.", e.x, e.y);
            w.stats.timeline("Mortars fired on " + place, bestX, bestY);
        } else w.dispatch.chatter(Dispatch.WHO_MILITARY, e, "Shot out! " + e.grenades + " rounds left.", e.x, e.y);
        e.lastShot = w.time;
        return true;
    }

    /**
     * (13.9) A combat medic: with none of the dead close, to the most badly hurt police officer or soldier nearby
     * (or themselves), and a few seconds kneeling over them puts half their strength back. A bite can't be treated:
     * the infected are passed over. True while busy with it.
     */
    boolean medicStep(Entity e, float dt) {
        if (w.spatial.nearest(e, 70, true, false) != null) {
            e.patient = null;
            return false;
        }
        Entity p = e.patient;
        if (p != null && (p.dead || p.removed || p.infected || p.hp >= p.maxHp * 0.95f || p.ride != null
                || World.hypot(p.x - e.x, p.y - e.y) > World.MEDIC_REACH * 1.5f)) p = e.patient = null;
        if (p == null) {
            if ((e.medicCd -= dt) > 0) return false;
            e.medicCd = 1.5f;
            p = w.spatial.woundedNear(e);
            if (p == null) return false;
            e.patient = p;
            e.treatT = 0;
        }
        float dx = p.x - e.x, dy = p.y - e.y, d = (float) Math.sqrt(dx * dx + dy * dy);
        if (p != e && d > 14) {
            w.wilds.walkToSpot(e, p.x, p.y, e.runSpeed);
            return true;
        }
        w.movement.steer(e, 0, 0, 0);
        e.aiming = false;
        if (p != e) e.angle = World.turn(e.angle, (float) Math.atan2(dy, dx), 0.3f);
        e.treatT += dt;
        if (e.treatT >= World.MEDIC_TIME) {
            p.hp = Math.min(p.maxHp, p.hp + p.maxHp * 0.5f);
            // ((15.1) Someone else patched up counts as a life saved.)
            if (p != e) {
                e.saved++;
                w.experience.gainXp(e, 20);
            }
            w.dispatch.chatter(Dispatch.WHO_MILITARY, e, p == e ? "Patching myself up." : "Medic here. " + Dispatch.name(p) + ", you're good to go.", e.x, e.y);
            e.patient = null;
            e.treatT = 0;
            e.medicCd = 3;
        }
        return true;
    }

    void thinkArmed(Entity e, float dt) {
        boolean soldier = e.type == Entity.SOLDIER;
        float range = shotRange(e, soldier);
        if (e.role == Entity.ROLE_COMMANDER && soldier) command(e, dt);
        // ((15.0) Up on a horse, a mounted officer sees the dead round them - whatever they're doing - and marks them
        // for the units nearby.)
        if (!soldier && e.role == Entity.ROLE_MOUNTED && (e.lookT -= dt) <= 0) {
            e.lookT = 1;
            w.police.markSpotted(e.x, e.y, World.MOUNT_SEE);
        }
        // The rescue team brings people in to the helicopter.
        if (e.role == Entity.ROLE_SAR && e.task == Dispatch.T_RESCUE && w.rescue.rescuer(e, dt)) return;
        if (e.meleeCd <= 0) {
            Entity z = w.spatial.nearestInReach(e);
            if (z != null) w.zombieAI.shove(e, z, true);
        }
        // ((25.0) After an escaped inmate.)
        if (!soldier && e.collar != null && e != w.controlled && w.events.collarStep(e, dt)) return;
        // ((13.9) A combat medic with nothing to shoot at patches up the wounded nearby.)
        if (soldier && e.role == Entity.ROLE_CORPSMAN && e != w.controlled && medicStep(e, dt)) return;
        // ((14.7) In the squad of the one you're playing: with them, and shooting at what they shoot at.)
        if (e.task == Dispatch.T_SQUAD && squadStep(e, dt)) return;
        // ((14.9) Put in to fly the station's helicopter: to the pad, and training there.)
        if (e.task == Dispatch.T_TRAIN && w.recruiting.trainStep(e, dt)) return;
        // ((14.2) In over their heads: radio for backup - out of rounds, falling back or hurt included.)
        maybeCallBackup(e);
        // ((14.5) Badly hurt and out of the fight: to the field hospital.)
        if (hospitalStep(e, dt)) return;
        // ((27.0) A marksman or sniper at a call: up on a roof beside it.)
        if ((e.role == Entity.ROLE_MARKSMAN && !soldier || e.role == Entity.ROLE_SNIPER && soldier) && e != w.controlled) {
            if (w.rooftops.overwatchStep(e, range, dt)) return;
        } else if (e.roof != null) w.rooftops.comeDown(e);
        // ((14.0) A mortar team shells a pack of the dead a few streets off.)
        if (soldier && e.role == Entity.ROLE_MORTAR && e != w.controlled && mortarStep(e, dt)) return;
        // ((15.0) Traffic officers on their junctions; mounted officers looking round and riding the parks.)
        if (!soldier && (e.role == Entity.ROLE_MOUNTED || e.role == Entity.ROLE_TRAFFIC) && e != w.controlled
                && w.police.specialPolice(e, w.spatial.nearest(e, 150, true, false), dt)) return;
        if (rideToFarCall(e, soldier, dt)) return;
        boolean dry = e.ammo <= 0 && e.reserve <= 0 && e.reload <= 0;
        float stock = (e.ammo + e.reserve) / (float) Math.max(1, e.magSize + World.fullReserve(e));
        e.lowCd -= dt;
        if (stock > 0.55f && e.reserve >= e.magSize * 2 && (e.magCd -= dt) <= 0) {
            e.magCd = 2;
            w.ammo.shareAmmo(e);
        }
        if (e.scout && !dry) {
            e.scout = false;
            w.dispatch.say(soldier ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, e, "Got some rounds. Back in the fight.", e.x, e.y);
        }
        // Out of ammo: head back to the precinct or base to resupply.
        if (dry && e.task != Dispatch.T_RESUPPLY && restockOnTheSpot(e)) dry = false;
        seekRounds(e, soldier, dry);
        restockWhenLow(e, soldier, dry, stock);
        if (borrowStep(e)) return;
        if (e.scout && dry) {
            w.ammo.scout(e, dt);
            return;
        }
        if (truckResupplyStep(e, soldier, dt)) return;
        if (resupplyStep(e, range, dt)) return;
        topUp(e, dt);

        Entity t = e.ammo > 0 || e.reserve > 0 ? w.combat.pickTarget(e, range) : null;
        e.shareCd -= dt;
        if (t != null && t.isZombie()) {
            // Remember where it was, to go and look if it slips out of sight.
            e.threatX = t.x;
            e.threatY = t.y;
            e.fear = 20;
            // Call it in: nearby units that can't see anything come and help.
            if (e.shareCd <= 0) {
                e.shareCd = 2.5f;
                shareSighting(e, t.x, t.y);
            }
        }
        // (10.37) What to do about it: fight, pull back hurt, fall back from more than they can hold, or get on with
        // their orders. Weighed up and held to for a moment (see decideArmed).
        int goal = decideArmed(e, t, soldier, stock, dt);
        if (goal == World.A_FALLBACK) {
            fallBack(e, t, range, soldier, dt);
            return;
        }
        if (woundedStep(e, t, soldier, range, goal, dt)) return;
        if (e.hp >= e.maxHp * 0.6f) e.retreatSaid = false;
        if (cordonStep(e, t, dt)) return;
        if (postStep(e, t)) return;
        boolean stationary = e.task == Dispatch.T_POST || e.task == Dispatch.T_HOLD
                || (e.task == Dispatch.T_CORDON && World.hypot(e.postX - e.x, e.postY - e.y) < 40);
        if (engageStep(e, t, soldier, stock, range, stationary, dt)) return;
        e.aiming = false;
        if (e.reload <= 0 && e.ammo < e.magSize / 2 && e.reserve > 0) {
            e.reload = w.combat.reloadTime(e);
            w.emit(Sfx.RELOAD, e.x, e.y);
        }
        if (stationary) {
            w.holdoutLife.standAt(e, e.postX, e.postY, 1f);
            return;
        }
        if (moveOrderStep(e, t, range, dt)) return;
        if (e.task == Dispatch.T_GUARD && e.zone != null) {
            w.posts.holdPost(e, e.zone, 1f);
            return;
        }
        if (e.task == Dispatch.T_RESPOND && e.incident != null && e.incident.ride) {
            // ((12.3) Dropped off by the truck that drove them out: back to what they were sent to do.)
            w.rides.afterRide(e, e.incident);
            return;
        }
        takeDroppedPost(e);
        if (respondStep(e, soldier, dt)) return;
        if (guardJoinsZone(e, soldier)) return;
        // ((11.1) In peacetime, soldiers on base are on duty, not loafing about.)
        if (soldier && e.task == Dispatch.T_NONE && w.alert == 0 && e.fear <= 0 && w.posts.soldierDuty(e, dt)) return;
        if (squadFormation(e, soldier, dt)) return;
        if (searchStep(e, soldier, dt)) return;
        int dist = w.city.fieldAt(w.city.zombieDist, e.x, e.y);
        int hunt = soldier ? 200 : 40;
        // ((13.3) The dead a long way off: soldiers ride out to the nearest call there, rather than walk across town.)
        if (soldier && e.task == Dispatch.T_NONE && (e.ammo > 0 || e.reserve > 0) && dist >= World.RIDE_TILES && dist < hunt && w.rides.rideOut(e, dt)) return;
        if ((e.ammo > 0 || e.reserve > 0) && dist < hunt && w.movement.followField(e, w.city.zombieDist, e.speed * 1.3f)) return;
        if (e.agency == 3 && e.task == Dispatch.T_NONE && e.rig == null && w.wilds.rangerOnFoot(e, dt)) return;
        // ((11.1) Officers with no call walk a beat.)
        if (!soldier && e.task == Dispatch.T_NONE && e.rig == null && e.fear <= 0 && w.police.policeBeat(e, dt)) return;
        if (guardPrecinct(e, soldier, dt)) return;
        if (backToBase(e, soldier, dt)) return;
        w.movement.wander(e, e.speed * 0.6f);
    }

    /** How far they'll take a shot: by what they are, and by the gun. */
    private float shotRange(final Entity e, final boolean soldier) {
        float range = soldier ? (e.role == Entity.ROLE_SNIPER ? 380 : e.role == Entity.ROLE_COMMANDER ? 180
                : e.role == Entity.ROLE_CORPSMAN ? 200 : 230)
                : e.role == Entity.ROLE_SWAT ? 210 : e.role == Entity.ROLE_MARKSMAN ? 320 : e.role == Entity.ROLE_MOUNTED ? 200 : 160;
        // ((11.4) How far they'll take a shot depends on the gun too.)
        switch (e.weapon) {
            case Entity.W_SHOTGUN: range = Math.min(range, 130); break;
            case Entity.W_SMG: range = Math.min(range, 170); break;
            case Entity.W_CARBINE: range = Math.max(range, 220); break;
            case Entity.W_HUNTING: range = Math.max(range, 280); break;
            case Entity.W_CROSSBOW: range = Math.max(range, 200); break;
            case Entity.W_DMR: range = Math.max(range, 300); break;
        }
        return range;
    }

    /** Out of rounds with an armoury or a stocked safe zone right where they stand: restocked on the spot (true), or not. */
    private boolean restockOnTheSpot(final Entity e) {
        boolean dry = true;
        City.Facility f = w.ammo.restockPoint(e);
        if (f == null) f = w.holdoutLife.supplyPoint(e);
        if (f != null && (f.x - e.x) * (f.x - e.x) + (f.y - e.y) * (f.y - e.y) < f.r * f.r * 2 && w.ammo.draw(f, e)) {
            // Already at the armoury (guards on a post): restock on the spot.
            e.reload = 2;
            dry = false;
        }
        // Guarding a safe zone that has ammo: restock there.
        if (dry && e.zone != null && !e.zone.removed && e.zone.ammo > 0
                && (e.zone.x - e.x) * (e.zone.x - e.x) + (e.zone.y - e.y) * (e.zone.y - e.y) < e.zone.r * e.zone.r * 1.5f) {
            int take = Math.min(e.zone.ammo, World.fullReserve(e));
            e.zone.ammo -= take;
            e.reserve = take;
            e.reload = 2;
            dry = false;
        }
        return !dry;
    }

    /** On foot to a call a long way off: a car or truck to take them and whoever else nearby is going. True when that was their step. */
    private boolean rideToFarCall(final Entity e, final boolean soldier, final float dt) {
        // ((11.0) On foot to a call a long way off, with nothing to fight here: a car or truck to take them, if
        // there's one (with whoever else nearby is going there too).)
        if (e.task == Dispatch.T_RESPOND && e.incident != null && !e.incident.resolved && !e.onScene && e.rig == null
                && e.ride == null && e.agency != 3 && e.role != Entity.ROLE_PILOT && (e.driveCd -= dt) <= 0) {
            e.driveCd = 8;
            Dispatch.Incident inc = e.incident;
            // ((13.3) Soldiers from 500 away, not 700.)
            if (World.hypot(inc.x - e.x, inc.y - e.y) > (soldier ? 500 : 700) && w.spatial.nearest(e, 100, true, false) == null) {
                java.util.ArrayList<Entity> group = new java.util.ArrayList<Entity>();
                group.add(e);
                int n = w.spatial.gatherLiving(e.x, e.y, 150), seats = e.type == Entity.COP ? 4 : 8;
                for (int i = 0; i < n && group.size() < seats; i++) {
                    Entity o = w.gathered[i];
                    if (o != e && !o.dead && o.type == e.type && o.task == Dispatch.T_RESPOND && o.incident == inc && o.rig == null && o.ride == null) group.add(o);
                }
                if (w.fleet.driveTo(group, inc)) {
                    for (Entity o : group) o.driveCd = 8;
                    if (w.time > w.rideSaidAt) {
                        w.rideSaidAt = w.time + 25;
                        w.dispatch.say(e.type == Entity.SOLDIER ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, e,
                                "Long way to " + inc.place + " on foot. Grabbing a " + (e.type == Entity.SOLDIER ? "truck." : "car."), e.x, e.y);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /** Out of rounds: a magazine off a partner, the ammo truck, the armoury, or (with none to be had) keeping watch. */
    private void seekRounds(final Entity e, final boolean soldier, final boolean dry) {
        if (dry && e.task != Dispatch.T_RESUPPLY && e.task != Dispatch.T_BORROW && e.task != Dispatch.T_BOARD && !e.scout) {
            City.Facility f = w.ammo.restockPoint(e);
            // ((10.60) A partner close by with plenty: a magazine off them first, rather than a long walk back.)
            // ((13.3) Whenever there's one about, not only when the station is a long way off.)
            Entity mate = w.ammo.lenderFor(e, 240);
            Fleet.Vehicle truck = null;
            if (mate != null) {
                w.dispatch.releaseForResupply(e);
                e.task = Dispatch.T_BORROW;
                e.protector = mate;
                e.taskTimer = 20;
                if (!e.outOfAmmoSaid) {
                    e.outOfAmmoSaid = true;
                    w.dispatch.chatter(soldier ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, e, "I'm dry! " + Dispatch.name(mate) + ", throw me a mag!", e.x, e.y);
                }
            } else if (soldier && ((truck = w.fleet.ammoTruckNear(e.x, e.y, World.TRUCK_REACH)) != null || (truck = w.fleet.ammoTruckComing(e.x, e.y, 600)) != null)) {
                // ((13.9) An army truck with spare rounds near: there, not all the way back to the base.)
                w.dispatch.releaseForResupply(e);
                e.task = Dispatch.T_RESUPPLY;
                e.supplyTruck = truck;
                if (!e.outOfAmmoSaid) {
                    e.outOfAmmoSaid = true;
                    w.dispatch.chatter(Dispatch.WHO_MILITARY, e, "I'm dry! Back to the truck for rounds.", e.x, e.y);
                }
            } else if (f != null) {
                if (!e.outOfAmmoSaid) {
                    e.outOfAmmoSaid = true;
                    w.dispatch.chatter(soldier ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, e, "I'm out of ammo! Heading back to " + f.name + " to resupply.", e.x, e.y);
                }
                w.dispatch.releaseForResupply(e);
                e.task = Dispatch.T_RESUPPLY;
            } else {
                // Nothing left in any armoury: a mag off someone who has plenty, or keep watch instead.
                Entity lender = w.ammo.lenderFor(e);
                w.dispatch.releaseForResupply(e);
                if (lender != null) {
                    e.task = Dispatch.T_BORROW;
                    e.protector = lender;
                    e.taskTimer = 30;
                    w.dispatch.chatter(soldier ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, e, "I'm dry and the armouries are empty. "
                            + Dispatch.name(lender) + ", can you spare a few rounds?", e.x, e.y);
                } else {
                    e.scout = true;
                    City.Facility home = w.holdoutLife.supplyPoint(e);
                    w.dispatch.chatter(soldier ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, e, "Out of ammo, and there's none to be had. Falling back"
                            + (home != null ? " to " + home.name : "") + " to keep watch. I'll call in what I see.", e.x, e.y);
                }
            }
        }
    }

    /** Running low: off to restock before it's too late. */
    private void restockWhenLow(final Entity e, final boolean soldier, final boolean dry, final float stock) {
        // Running low: back to restock before it's too late (when there's somewhere to get it and nothing on top of them).
        if (!dry && stock < 0.22f && e.lowCd <= 0 && (e.task == Dispatch.T_NONE || e.task == Dispatch.T_RESPOND)) {
            e.lowCd = 25;
            City.Facility f = w.ammo.restockPoint(e);
            // ((13.3) A partner right there with plenty: a magazine or two off them, and stay in the fight.)
            Entity mate = w.ammo.lenderFor(e, 120);
            Fleet.Vehicle truck = soldier && mate == null && w.spatial.nearest(e, 70, true, false) == null ? w.fleet.ammoTruckNear(e.x, e.y, World.TRUCK_REACH) : null;
            if (mate != null) {
                e.task = Dispatch.T_BORROW;
                e.protector = mate;
                e.taskTimer = 12;
                w.dispatch.chatter(soldier ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, e, "Running low. " + Dispatch.name(mate) + ", spare a mag?", e.x, e.y);
            } else if (truck != null) {
                w.dispatch.chatter(Dispatch.WHO_MILITARY, e, "Running low. Back to the truck for rounds.", e.x, e.y);
                w.dispatch.releaseForResupply(e);
                e.task = Dispatch.T_RESUPPLY;
                e.supplyTruck = truck;
            } else if (f != null && World.hypot(f.x - e.x, f.y - e.y) < 1600 && w.spatial.nearest(e, 70, true, false) == null) {
                w.dispatch.chatter(soldier ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, e, "Running low. Heading back to " + f.name + " to restock.", e.x, e.y);
                w.dispatch.releaseForResupply(e);
                e.task = Dispatch.T_RESUPPLY;
            }
        }
    }

    /** Over to a partner for a magazine. True when that was their step. */
    private boolean borrowStep(final Entity e) {
        if (e.task == Dispatch.T_BORROW) {
            Entity l = e.protector;
            // ((10.60) And not chasing a partner who's drawn away: back to the station after all.)
            if (l == null || l.dead || l.removed || l.reserve <= 0 || World.hypot(l.x - e.x, l.y - e.y) > 600 || e.taskTimer <= 0) {
                e.task = Dispatch.T_NONE;
                e.protector = null;
                if (l != null && !l.dead && w.ammo.restockPoint(e) != null) {
                    w.dispatch.releaseForResupply(e);
                    e.task = Dispatch.T_RESUPPLY;
                }
            } else {
                float lx = l.x - e.x, ly = l.y - e.y, ld = (float) Math.sqrt(lx * lx + ly * ly) + 0.001f;
                if (ld > 16) {
                    e.aiming = false;
                    Entity t = w.spatial.nearest(e, 24, true, false);
                    if (t != null) {
                        // ((10.60) Round the one in the way, still towards them.)
                        float tx = e.x - t.x, ty = e.y - t.y, td = (float) Math.sqrt(tx * tx + ty * ty) + 0.001f;
                        float fx = tx / td + 0.8f * lx / ld, fy = ty / td + 0.8f * ly / ld, fd = (float) Math.sqrt(fx * fx + fy * fy) + 0.001f;
                        w.movement.flee(e, fx / fd, fy / fd, e.runSpeed);
                    } else w.wilds.walkToSpot(e, l.x, l.y, e.runSpeed);
                    return true;
                }
                // Shared out: half of what they can spare.
                // ((13.3) Half of what they have beyond a third of a load, two magazines at least where they have them.)
                int spare = Math.max(e.magSize * 2, (l.ammo + l.reserve - World.fullReserve(l) / 3) / 2);
                spare = Math.min(spare, l.reserve);
                // ((24.0) No more than they can carry.)
                spare = Math.max(0, Math.min(spare, Math.max(e.magSize, World.fullReserve(e) - e.reserve)));
                l.reserve -= spare;
                e.reserve += spare;
                e.reload = 2;
                e.talkTimer = l.talkTimer = 1.5f;
                e.task = Dispatch.T_NONE;
                e.protector = null;
                e.outOfAmmoSaid = false;
            }
        }
        return false;
    }

    /** To an army truck with spare rounds, or waiting where it's coming to. True when that was their step. */
    private boolean truckResupplyStep(final Entity e, final boolean soldier, final float dt) {
        // ((13.9) On the way back to the base, and an army truck with rounds has pulled up near: to it instead.)
        if (soldier && e.task == Dispatch.T_RESUPPLY && e.supplyTruck == null && (e.driveCd -= dt) <= 0) {
            e.driveCd = 2;
            e.supplyTruck = w.fleet.ammoTruckNear(e.x, e.y, World.TRUCK_REACH);
        }
        if (e.task == Dispatch.T_RESUPPLY && e.supplyTruck != null) {
            Fleet.Vehicle v = e.supplyTruck;
            // ((16.7) Still on its way by what it's doing, not how fast it's going this moment: held at a junction or in
            // a queue it was taken for there, and they set off to wherever it had stopped - and when it drove on, back
            // again - and on a long drive the waiting ran out before it came.)
            boolean coming = v.ammoTruck && v.state == Fleet.DRIVE;
            if (v.cargo <= 0 || v.broken || v.burnt || v.removedFromFleet || v.state == Fleet.RETURN || (v.speed > 3 && !coming)) {
                // (Gone, or emptied before they got there: the base after all.)
                e.supplyTruck = null;
            } else if (coming) {
                // (On its way: wait for it where it's going, out of the fight.)
                float wx = v.tx - e.x, wy = v.ty - e.y;
                e.aiming = false;
                if (wx * wx + wy * wy > 60 * 60) w.wilds.walkToSpot(e, v.tx, v.ty, e.speed);
                else w.movement.steer(e, 0, 0, 0);
                return true;
            } else {
                float tx = v.x - e.x, ty = v.y - e.y, td = (float) Math.sqrt(tx * tx + ty * ty);
                if (td < 18) {
                    int take = Math.min(v.cargo, Math.max(0, World.fullReserve(e) - e.reserve));
                    v.cargo -= take;
                    e.reserve += take;
                    int top = Math.min(e.magSize - e.ammo, e.reserve);
                    e.ammo += top;
                    e.reserve -= top;
                    e.reload = 1.5f;
                    e.task = Dispatch.T_NONE;
                    e.supplyTruck = null;
                    e.outOfAmmoSaid = false;
                    return true;
                }
                e.aiming = false;
                Entity t = w.spatial.nearest(e, 24, true, false);
                if (t != null) {
                    float ax = e.x - t.x, ay = e.y - t.y, ad = (float) Math.sqrt(ax * ax + ay * ay) + 0.001f;
                    float fx = ax / ad + 0.8f * tx / (td + 0.001f), fy = ay / ad + 0.8f * ty / (td + 0.001f), fd = (float) Math.sqrt(fx * fx + fy * fy) + 0.001f;
                    w.movement.flee(e, fx / fd, fy / fd, e.runSpeed);
                } else w.wilds.walkToSpot(e, v.x, v.y, e.runSpeed);
                return true;
            }
        }
        return false;
    }

    /** Back to the armoury to restock, shooting back on the way. True when that was their step. */
    private boolean resupplyStep(final Entity e, final float range, final float dt) {
        if (e.task == Dispatch.T_RESUPPLY) {
            City.Facility f = w.ammo.restockPoint(e);
            if (f == null) f = w.holdoutLife.supplyPoint(e);
            if (f == null) {
                e.task = Dispatch.T_NONE;
            } else {
                float ddx = f.x - e.x, ddy = f.y - e.y;
                if (ddx * ddx + ddy * ddy < f.r * f.r) {
                    if (w.ammo.draw(f, e)) {
                        // ((12.0) Topped up: the rounds still in the gun stay in it - they used to be thrown away.)
                        int top = Math.min(e.magSize - e.ammo, e.reserve);
                        e.ammo += top;
                        e.reserve -= top;
                    }
                    e.task = Dispatch.T_NONE;
                    e.outOfAmmoSaid = false;
                } else {
                    e.aiming = false;
                    Entity t = w.spatial.nearest(e, 50, true, false);
                    float tx = 0, ty = 0, d = 99;
                    if (t != null) {
                        tx = e.x - t.x;
                        ty = e.y - t.y;
                        d = (float) Math.sqrt(tx * tx + ty * ty) + 0.001f;
                    }
                    // ((10.60) Away from the dead only when one is right on them, and even then round towards the
                    // station: running straight away from whatever was behind them used to take them further and
                    // further from it, gun up and empty, with the dead following all the way.)
                    if (t != null && d < 24) {
                        float hx = ddx, hy = ddy, hd = (float) Math.sqrt(hx * hx + hy * hy) + 0.001f;
                        float fx = tx / d + 0.8f * hx / hd, fy = ty / d + 0.8f * hy / hd, fd = (float) Math.sqrt(fx * fx + fy * fy);
                        if (fd < 0.3f) {
                            // (The dead right between them and the station: round them.)
                            fx = -ty / d;
                            fy = tx / d;
                            fd = 1;
                        }
                        w.movement.flee(e, fx / fd, fy / fd, e.runSpeed);
                    } else if (!w.movement.followField(e, w.city.field(f), e.runSpeed)) {
                        // ((16.10) Beyond its way on foot: their own way there, not giving up on it.)
                        if (World.hypot(f.x - e.x, f.y - e.y) > f.r) w.wilds.walkToSpot(e, f.x, f.y, e.runSpeed);
                        else e.task = Dispatch.T_NONE;
                    }
                    // (10.37) Rounds still in the gun: shooting back on the way.
                    if (t != null && (e.ammo > 0 || e.reserve > 0)) w.combat.aimAndFire(e, t, d, range, dt);
                    return true;
                }
            }
        }
        return false;
    }

    /** Spare rounds topped up quietly: from a supply crate passed, or standing at a supply point. */
    private void topUp(final Entity e, final float dt) {
        // A supply crate tops up anyone passing.
        if (e.reserve < World.fullReserve(e) && !w.pickups.isEmpty() && w.rnd.nextFloat() < dt * 2)
            for (int i = 0; i < w.pickups.size(); i++) {
                World.Pickup p = w.pickups.get(i);
                if (p.uses > 0 && p.drop <= 0 && (p.x - e.x) * (p.x - e.x) + (p.y - e.y) * (p.y - e.y) < 30 * 30)
                    e.reserve = World.fullReserve(e);
            }
        // Standing at a supply point quietly tops up spare ammo. ((16.6) But never the armoury's last half: with
        // everyone short at the start (a city caught unprepared), the hundreds standing at the base filled up in
        // the first second and emptied it, before a shot was fired - and with it empty, no ammo truck could go out
        // to a fight. The last half is for those who come back from one.)
        if (e.reserve < World.fullReserve(e) && w.rnd.nextFloat() < dt) {
            City.Facility f = w.holdoutLife.supplyPoint(e);
            int keep = f == null ? 0 : f.kind == City.FACILITY_BASE ? 4500 : f.kind == City.FACILITY_POLICE ? 1500 : 0;
            if (f != null && (f.x - e.x) * (f.x - e.x) + (f.y - e.y) * (f.y - e.y) < f.r * f.r * 1.5f && f.ammo > keep) {
                int take = Math.min(f.ammo - keep, Math.min(e.magSize, World.fullReserve(e) - e.reserve));
                f.ammo -= take;
                e.reserve += take;
            }
        }
    }

    /** Badly hurt: pulling back while the others cover. True when that was their step. */
    private boolean woundedStep(final Entity e, final Entity t, final boolean soldier, final float range, final int goal, final float dt) {
        // Badly hurt: pull back while the others cover.
        if (goal == World.A_WOUNDED && t != null) {
            if (!e.retreatSaid) {
                e.retreatSaid = true;
                w.dispatch.chatter(soldier ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, e, "I'm hit! Falling back!", e.x, e.y);
            }
            float ddx = t.x - e.x, ddy = t.y - e.y, d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
            w.movement.flee(e, -ddx / d, -ddy / d, e.runSpeed);
            if (d < 120) w.combat.aimAndFire(e, t, d, range, dt);
            return true;
        }
        return false;
    }

    /** On an infection zone's cordon: to their place on the line, facing in. True when that was their step. */
    private boolean cordonStep(final Entity e, final Entity t, final float dt) {
        World.InfectionZone myZone = e.task == Dispatch.T_CORDON ? w.containment.zoneOfPost(e.postX, e.postY) : null;
        if (e.task == Dispatch.T_CORDON) {
            if (myZone == null || !myZone.critical) {
                e.task = Dispatch.T_NONE;
            } else {
                float pd = (float) World.hypot(e.postX - e.x, e.postY - e.y);
                if (pd > 30 && (t == null || World.hypot(t.x - e.x, t.y - e.y) > 90)) {
                    w.wilds.walkToSpot(e, e.postX, e.postY, e.runSpeed * 0.9f);
                    return true;
                }
                if (t == null) {
                    // On the line: facing in, towards where the dead are.
                    w.holdoutLife.standAt(e, e.postX, e.postY, 0.6f);
                    if (e.want < 1) e.angle = World.turn(e.angle, (float) Math.atan2(myZone.y - e.y, myZone.x - e.x), dt * 3);
                    return true;
                }
            }
        }
        return false;
    }

    /** Posted somewhere: getting there, and back to it after a fight. True when that was their step. */
    private boolean postStep(final Entity e, final Entity t) {
        // (10.46) Posted somewhere (a checkpoint on the road into an infection zone): get there first, and back to
        // it after a fight.
        if (e.task == Dispatch.T_POST && e.postX > 0) {
            float pd = (float) World.hypot(e.postX - e.x, e.postY - e.y);
            if (pd > 24 && (t == null || World.hypot(t.x - e.x, t.y - e.y) > 90)) {
                w.wilds.walkToSpot(e, e.postX, e.postY, pd > 200 ? w.rides.transit(e, pd) : e.speed * 1.3f);
                return true;
            }
        }
        return false;
    }

    /** One of the dead to shoot: keeping their distance, firing, and a grenade into a crowd of them. True when that was their step. */
    private boolean engageStep(final Entity e, final Entity t, final boolean soldier, final float stock, final float range, final boolean stationary, final float dt) {
        if (t != null && e.task != Dispatch.T_MOVE) {
            float ddx = t.x - e.x, ddy = t.y - e.y;
            float d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
            // ((21.2) Reading the fight: how many, from how many sides, and which way is open.)
            if (World.TACTICS && e != w.controlled && breakOut(e, t, d, range, dt)) return true;
            // ((16.9) Each kind of police officer keeps their own distance (see doctrine).)
            float[] dc = World.doctrine(e);
            float keep = soldier ? (e.role == Entity.ROLE_COMMANDER ? 90 : e.role == Entity.ROLE_SNIPER ? 60 : 40) : dc != null ? dc[4] : 50;
            // (10.28) Soldiers sent in close right up; police on a perimeter keep their distance.
            if (soldier && e.task == Dispatch.T_RESPOND && e.role != Entity.ROLE_COMMANDER && e.role != Entity.ROLE_SNIPER) keep = 26;
            else if (!soldier && e.task == Dispatch.T_RESPOND && e.incident != null && e.incident.zombiesNear >= 4) keep = Math.max(keep, keep * 1.4f);
            // Low on rounds: they don't let them get close.
            if (stock < 0.5f) keep *= 1 + (0.5f - stock) * 3;
            Dispatch.SafeZone guarding = e.task == Dispatch.T_GUARD ? e.zone : null;
            boolean swarmed = w.spatial.countZombiesNear(e.x, e.y, 40) >= 3;
            boolean pressed = d < keep || (e.reload > 0 && d < 70) || swarmed;
            if (stationary) {
                // ((11.5) Nobody is rooted to a gate or a checkpoint: with the dead coming at them they step back
                // (a fair way, not across town), shooting, and take the post again once it's clear.)
                float pd = (float) World.hypot(e.x - e.postX, e.y - e.postY);
                // ((24.0) Dug in behind sandbags, they stay behind them and fight from there.)
                if (dugIn(e)) {
                    if (pd > 3) w.holdoutLife.standAt(e, e.postX, e.postY, 0.5f);
                    else w.movement.steer(e, 0, 0, 0);
                } else if ((d < keep * 0.8f || swarmed || (e.reload > 0 && d < 45)) && pd < 120)
                    w.movement.flee(e, awayX(e, ddx, d), awayY(e, ddy, d), World.backOff(e, swarmed));
                else if (d > keep * 1.4f && !swarmed) w.holdoutLife.standAt(e, e.postX, e.postY, 0.5f);
                else w.movement.steer(e, 0, 0, 0);
            } else if (pressed && (guarding == null || World.inside(e, guarding, 1f)))
                // Too close, reloading or outnumbered: fall back while shooting. ((21.2) Away from all of them, by the
                // open side - not straight back from the nearest into the rest.) ((27.4) Reloading, with no aim to keep
                // steady, they open the gap properly: backing off at a walk, one of the dead running at them caught them
                // with an empty gun.)
                w.movement.flee(e, awayX(e, ddx, d), awayY(e, ddy, d), e.reload > 0 && d < 70 ? Math.max(World.backOff(e, swarmed), e.runSpeed * 0.85f) : World.backOff(e, swarmed));
            else if (pressed && guarding != null)
                // ((11.5) On the gate outside the walls and the dead are on them: back inside, not stood there.)
                w.wilds.walkToSpot(e, guarding.x, guarding.y, e.runSpeed * 0.9f);
            else if (guarding != null) w.posts.holdPost(e, guarding, 0.5f);
            else if (!soldier && (e.role == Entity.ROLE_SWAT || e.role == Entity.ROLE_RIOT) && d > keep + 20 && !swarmed && stock >= 0.4f
                    && w.spatial.countZombiesNear(t.x, t.y, 60) < 5)
                // ((16.9) SWAT and riot police push in on them, steadily, rather than wait for them to come.)
                w.movement.steer(e, ddx / d, ddy / d, e.speed * (e.role == Entity.ROLE_SWAT ? 0.6f : 0.4f));
            else if (e.task == Dispatch.T_NONE && e.member % 3 == 2 && d > keep + 25) {
                // Flankers work round to the side for a clear angle, instead of all firing down one line.
                float side = e.callsign % 2 == 0 ? 1 : -1;
                w.movement.steer(e, -ddy / d * side, ddx / d * side, e.speed * 0.5f);
            } else w.movement.steer(e, 0, 0, 0);
            if (!w.combat.aimAndFire(e, t, d, range, dt) && !stationary && guarding == null) {
                // Someone in the line of fire: step sideways for a clear shot.
                float side = (e.callsign + e.member) % 2 == 0 ? 1 : -1;
                w.movement.steer(e, -ddy / d * side, ddx / d * side, e.speed);
            }
            // ((27.4) Getting low, with the nearest of them not on top of them and someone alongside with a full gun to
            // cover them (and not reloading too): change the magazine now, not when it clicks empty with the dead close.)
            if (e.reload <= 0 && e.reserve > 0 && e.ammo > 0 && e.ammo <= Math.max(2, e.magSize * 0.3f) && d > 60
                    && w.combat.mateCovering(e, 60) && !w.combat.mateReloading(e, Combat.MATE_R)) {
                e.reload = w.combat.reloadTime(e);
                w.emit(Sfx.RELOAD, e.x, e.y);
            }
            boolean launcher = soldier && e.role == Entity.ROLE_GRENADIER;
            if (soldier && (e.role == Entity.ROLE_RIFLE || launcher) && e.grenades > 0 && e.grenadeCd <= 0 && d > (launcher ? 60 : 70)
                    && d < (launcher ? 280 : 200) && (w.spatial.countZombiesNear(t.x, t.y, 40) >= (launcher ? 5 : 7) || (launcher && t.type == Entity.GIANT)) && !w.spatial.peopleNear(t.x, t.y, 60)) {
                w.combat.throwGrenade(e, t.x, t.y);
                // (A launcher reloads quicker than a man can pull another pin.)
                if (launcher) {
                    e.grenadeCd = 7;
                    w.emit(Sfx.LAUNCHER, e.x, e.y);
                }
            }
            return true;
        }
        return false;
    }

    /** Ordered somewhere: on the way there, shooting what's close. True when that was their step. */
    private boolean moveOrderStep(final Entity e, final Entity t, final float range, final float dt) {
        if (e.task == Dispatch.T_MOVE) {
            float ddx = e.postX - e.x, ddy = e.postY - e.y;
            float d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
            if (d < 20) {
                e.task = Dispatch.T_HOLD;
                e.orderField = null;
            } else if (e.orderField == null || !w.movement.followField(e, e.orderField, e.runSpeed)) {
                w.movement.steer(e, ddx / d, ddy / d, e.runSpeed);
            }
            // ((21.2) On the way, they shoot what's close: under orders to move, they used to walk past the dead -
            // and past people being bitten - without firing a shot.)
            if (World.TACTICS && t != null) {
                float tx = t.x - e.x, ty = t.y - e.y, td = (float) Math.sqrt(tx * tx + ty * ty) + 0.001f;
                if (td < 95) w.combat.aimAndFire(e, t, td, range, dt);
            }
            return true;
        }
        return false;
    }

    /** Dropped off for a cordon or a checkpoint: the post is theirs. */
    private void takeDroppedPost(final Entity e) {
        if (e.task == Dispatch.T_RESPOND && e.incident != null && e.incident.cordonPost >= 0) {
            // Dropped off on the cordon: take the post.
            Dispatch.Incident inc = e.incident;
            e.incident = null;
            e.task = !inc.resolved && w.containment.zoneOfPost(inc.x, inc.y) != null ? Dispatch.T_CORDON : Dispatch.T_NONE;
            e.postX = inc.x;
            e.postY = inc.y;
        }
        if (e.task == Dispatch.T_RESPOND && e.incident != null && e.incident.checkpointSpot != null) {
            // (10.46) Dropped off for a checkpoint on the road into an infection zone: on to the spot by the barrier.
            Dispatch.Incident inc = e.incident;
            e.incident = null;
            if (inc.resolved) e.task = Dispatch.T_NONE;
            else {
                // (Each to their own spot: the truck brought the whole squad.)
                float[] spot = inc.checkpointSpot;
                for (World.InfectionZone z : w.infection)
                    for (int k = 0; k < z.guards.length && k < z.guardSpots.size(); k++) if (z.guards[k] == e) spot = z.guardSpots.get(k);
                e.task = Dispatch.T_POST;
                e.postX = spot[0];
                e.postY = spot[1];
            }
        }
    }

    /** Answering a call: getting there, and the perimeter or cordon round it. True when that was their step. */
    private boolean respondStep(final Entity e, final boolean soldier, final float dt) {
        if (e.task == Dispatch.T_RESPOND && e.incident != null) {
            Dispatch.Incident inc = e.incident;
            float ddx = inc.x - e.x, ddy = inc.y - e.y;
            float d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
            // (10.28) Police contain a real outbreak: a perimeter round it, outside the infected ground, facing in,
            // shooting whatever comes out. (SWAT and the riot squad still go in.) The military goes in after them.
            if (!soldier && inc.zombiesNear >= 4 && e.role != Entity.ROLE_SWAT && e.role != Entity.ROLE_RIOT) {
                float pr = 100 + Math.min(90, inc.zombiesNear * 3);
                // ((12.0) Their place on the ring is picked once, from the side they came in on: worked out afresh
                // from where they stood each moment, a third of a turn to one side, it moved on ahead of them as they
                // walked, and they went round and round the perimeter for good.)
                if (e.ringOf != inc) {
                    e.ringOf = inc;
                    e.ringA = (float) Math.atan2(e.y - inc.y, e.x - inc.x) + ((e.member % 3) - 1) * 0.35f;
                }
                float ang = e.ringA;
                float px = inc.x + (float) Math.cos(ang) * pr, py = inc.y + (float) Math.sin(ang) * pr;
                // ((12.0) Once there, they keep to their place on the ring unless pushed well back: right at the edge,
                // with the ring growing and shrinking as the count of the dead changed, they went from the call's
                // route to their spot and back every moment - two ways at once - and stood there for good.)
                if (d > pr + (e.onScene ? 160 : 80)) {
                    w.movement.headFor(e, inc.field, inc.x, inc.y, d, w.rides.transit(e, d));
                    return true;
                }
                if (!e.onScene) {
                    e.onScene = true;
                    w.dispatch.onScene(e, inc);
                }
                // ((27.5) The dead close by but round a corner or just out of pistol range: they stood on their
                // spot on the ring and never fired a shot. They step in until they can see them, no nearer.)
                int zd = w.city.fieldAt(w.city.zombieDist, e.x, e.y);
                boolean closeIn = zd < 16 && (e.ammo > 0 || e.reserve > 0);
                if (closeIn && zd > 3 && w.movement.followField(e, w.city.zombieDist, e.speed)) return true;
                if (!closeIn && !w.city.solidAt(px, py) && World.hypot(px - e.x, py - e.y) > 6) {
                    w.wilds.walkToSpot(e, px, py, e.speed * 1.4f);
                    return true;
                }
                w.movement.steer(e, 0, 0, 0);
                w.civilianAI.personalSpace(e);
                if (e.want < 1) e.angle = World.turn(e.angle, (float) Math.atan2(inc.y - e.y, inc.x - e.x), dt * 3);
                return true;
            }
            if (soldier && e.onScene && w.city.fieldAt(w.city.zombieDist, e.x, e.y) < 30
                    && w.movement.followField(e, w.city.zombieDist, e.runSpeed * 0.8f)) return true;
            if (d > 40) {
                w.movement.headFor(e, inc.field, inc.x, inc.y, d, w.rides.transit(e, d));
                return true;
            }
            if (!e.onScene) {
                e.onScene = true;
                w.dispatch.onScene(e, inc);
            }
            // ((27.5) On scene with the dead a street off and nothing in sight: they go to them (it was only 10
            // tiles, and they stood at the cordon while the fight went on just out of range).
            if (w.city.fieldAt(w.city.zombieDist, e.x, e.y) < 16 && (e.ammo > 0 || e.reserve > 0)
                    && w.movement.followField(e, w.city.zombieDist, e.speed * 1.2f)) return true;
            if (!soldier) {
                // Police throw a cordon round the scene: spread out in a ring, facing outwards.
                float a = (e.callsign * 2.4f + e.member * 1.7f) % World.TAU, r = 38;
                float px = inc.x + (float) Math.cos(a) * r, py = inc.y + (float) Math.sin(a) * r;
                if (!w.city.solidAt(px, py)) {
                    w.holdoutLife.standAt(e, px, py, 0.8f);
                    if (e.want < 1) e.angle = World.turn(e.angle, a, dt * 3);
                    return true;
                }
            }
            w.movement.wander(e, e.speed * 0.5f);
            return true;
        }
        return false;
    }

    /** The National Guard reinforce the nearest safe zone. True when that was their step. */
    private boolean guardJoinsZone(final Entity e, final boolean soldier) {
        // The National Guard's job is protecting civilians: they reinforce the nearest safe zone.
        if (soldier && e.role == Entity.ROLE_GUARD && e.task == Dispatch.T_NONE && e.taskTimer <= 0) {
            e.taskTimer = 3;
            Dispatch.SafeZone best = null;
            float bd = Float.MAX_VALUE;
            for (int i = 0; i < w.dispatch.zones.size(); i++) {
                Dispatch.SafeZone z = w.dispatch.zones.get(i);
                float d = (z.x - e.x) * (z.x - e.x) + (z.y - e.y) * (z.y - e.y);
                if (d < bd && z.guards < z.wantGuards + 6) {
                    bd = d;
                    best = z;
                }
            }
            if (best != null) {
                e.task = Dispatch.T_GUARD;
                e.zone = best;
                best.guards++;
                return true;
            }
        }
        return false;
    }

    /** Soldiers keep their squad together: up to the leader, and a wedge behind them on the move. True when that was their step. */
    private boolean squadFormation(final Entity e, final boolean soldier, final float dt) {
        // Soldiers keep their squad together.
        if (soldier && e.task == Dispatch.T_NONE) {
            if (e.taskTimer <= 0) {
                e.taskTimer = 2;
                e.leader = e.role == Entity.ROLE_COMMANDER ? w.holdoutLife.biggestGroup(e) : squadLead(e);
            }
            Entity lead = e.leader;
            if (lead != null && !lead.dead && lead != e) {
                float lx = lead.x - e.x, ly = lead.y - e.y, ld = (float) Math.sqrt(lx * lx + ly * ly) + 0.001f;
                if (ld > 80) {
                    // ((12.0) The way to them, round whatever's between: straight at them from across town, they ran
                    // into the side of the first building and stayed there.)
                    if (e.blocked || (ld > 120 && !w.city.los(e.x, e.y, lead.x, lead.y))) w.wilds.walkToSpot(e, lead.x, lead.y, e.runSpeed);
                    else w.movement.steer(e, lx / ld, ly / ld, e.runSpeed);
                    return true;
                }
                // On the move with the squad: keep a wedge behind the leader. ((11.6) Lined up on the way the leader
                // is actually going, eased round over a second or so and held while they pause - not the way they
                // wish to go this very moment, which swung to and fro and sent the wedge swapping sides.)
                float lsp = (float) Math.sqrt(lead.vx * lead.vx + lead.vy * lead.vy);
                if (lsp > 4) {
                    float k = e.formHx == 0 && e.formHy == 0 ? 1 : Math.min(1, dt * 2.5f);
                    e.formHx += (lead.vx / lsp - e.formHx) * k;
                    e.formHy += (lead.vy / lsp - e.formHy) * k;
                }
                if (lsp > 4 && e.fear <= 0 && (e.formHx != 0 || e.formHy != 0)) {
                    float hx = e.formHx, hy = e.formHy, hl = (float) Math.sqrt(hx * hx + hy * hy) + 0.001f;
                    hx /= hl;
                    hy /= hl;
                    int m = e.member + 1;
                    float back = 12 + (m / 2) * 12, side = (m % 2 == 0 ? 1 : -1) * (8 + (m / 2) * 9);
                    float sx = lead.x - hx * back - hy * side, sy = lead.y - hy * back + hx * side;
                    float dx = sx - e.x, dy = sy - e.y, sd = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
                    if (sd > 5 && !w.city.solidAt(sx, sy)) {
                        // ((12.0) Round the building in the way, not into it.)
                        if (e.blocked || (sd > 30 && !w.city.los(e.x, e.y, sx, sy))) w.wilds.walkToSpot(e, sx, sy, Math.min(e.runSpeed, lead.want + sd * 0.8f));
                        else w.movement.steer(e, dx / sd, dy / sd, Math.min(e.runSpeed, lead.want + sd * 0.8f));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Going to check where the last one was seen. True when that was their step. */
    private boolean searchStep(final Entity e, final boolean soldier, final float dt) {
        // Go and check where the last one was seen.
        if (e.task == Dispatch.T_NONE && e.fear > 0 && (e.ammo > 0 || e.reserve > 0)) {
            float lx = e.threatX - e.x, ly = e.threatY - e.y, ld = (float) Math.sqrt(lx * lx + ly * ly) + 0.001f;
            if (ld < 20 || (e.goal == World.A_SEARCH && e.goalTime > 15 && e.blocked)) e.fear = 0;
            else {
                // Bounding overwatch: half the squad moves up while the other half covers, then they swap.
                boolean cover = soldier && e.leader != null && ld > 60 && ((int) (w.time / 3) + e.member) % 2 == 0;
                if (cover) {
                    w.movement.steer(e, 0, 0, 0);
                    e.angle = World.turn(e.angle, (float) Math.atan2(ly, lx), dt * 4);
                } else if (ld > 30 && (e.blocked || !w.city.los(e.x, e.y, e.threatX, e.threatY))) {
                    // ((12.0) The way there, round whatever's between: straight at it, they walked into the side of
                    // a building and stayed there, the squad with them.)
                    w.wilds.walkToSpot(e, e.threatX, e.threatY, e.speed * 1.2f);
                } else {
                    w.movement.steer(e, lx / ld, ly / ld, e.speed * 1.2f);
                }
                return true;
            }
        }
        return false;
    }

    /** The city under attack: officers with no call guard their precinct. True when that was their step. */
    private boolean guardPrecinct(final Entity e, final boolean soldier, final float dt) {
        if (!soldier && w.alert >= 1 && e.task == Dispatch.T_NONE && e.rig == null) {
            // The city is under attack: officers with no call guard their precinct, spread round it facing out,
            // instead of milling about.
            City.Facility st = w.city.nearestFacility(City.FACILITY_POLICE, e.x, e.y);
            if (st != null) {
                float sx = st.x - e.x, sy = st.y - e.y;
                if (sx * sx + sy * sy > 500 * 500) {
                    if (w.movement.followField(e, w.city.field(st), e.speed)) return true;
                    // ((16.10) Beyond its way on foot: their own way there.)
                    w.wilds.walkToSpot(e, st.x, st.y, e.speed);
                    return true;
                } else {
                    // ((12.0) In two or three rings when there are many of them, not all shoulder to shoulder on one.)
                    float a = (e.callsign * 2.39996f) % World.TAU, r = st.r * 0.95f + 10 + (e.callsign % 3) * 11;
                    float px = st.x + (float) Math.cos(a) * r, py = st.y + (float) Math.sin(a) * r;
                    if (!w.city.solidAt(px, py)) {
                        w.holdoutLife.standAt(e, px, py, 0.7f);
                        if (e.want < 1) e.angle = World.turn(e.angle, a, dt * 2);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** With nothing to fight, soldiers drift back to base and fall in. True when that was their step. */
    private boolean backToBase(final Entity e, final boolean soldier, final float dt) {
        if (soldier) {
            // With nothing to fight, soldiers drift back to base.
            City.Facility base = w.city.nearestFacility(City.FACILITY_BASE, e.x, e.y);
            if (base != null) {
                float bx = base.x - e.x, by = base.y - e.y;
                // ((13.3) A long way from base: a truck back, not the walk.)
                if (bx * bx + by * by > World.RIDE_HOME * World.RIDE_HOME && w.rides.rideOut(e, dt)) return true;
                if (bx * bx + by * by > base.r * base.r * 2.2f && w.movement.followField(e, w.city.field(base), e.speed * 0.8f)) return true;
                // ((16.10) Beyond its way on foot: their own way back.)
                if (bx * bx + by * by > base.r * base.r * 6) {
                    w.wilds.walkToSpot(e, base.x, base.y, e.speed * 0.8f);
                    return true;
                }
                // ((11.1) And once there, fall in: squads in ranks on the parade ground, ready to go.)
                if (w.alert >= 1 && bx * bx + by * by < base.r * base.r * 2.2f) {
                    int rank = e.squad % 4, file = e.member % 8, blk = (e.squad / 4) % 3;
                    // ((12.0) And every fourth squad in a block of its own beside the others, not on their places.)
                    float px = base.x + (file - 3.5f) * 9 + (blk == 0 ? 0 : blk == 1 ? -78 : 78), py = base.y + (rank - 1.5f) * 16;
                    // ((12.0) Their place on a building: the same ranks a little further down or up the base, rather
                    // than wandering about it.)
                    for (int k = 1; k <= 4 && w.city.solidAt(px, py); k++) py += (k % 2 == 1 ? 1 : -1) * k * 48;
                    if (!w.city.solidAt(px, py)) {
                        w.holdoutLife.standAt(e, px, py, 0.6f);
                        if (e.want < 1) e.angle = World.turn(e.angle, (float) (Math.PI / 2), dt * 3);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * What an officer or soldier does about what's in front of them: shoot it, pull back hurt, fall back from more
     * of the dead than they and the others round them can hold (towards the others, or somewhere to restock), go
     * and look where the last one was seen, or get on with their orders. Each is scored from the odds round them,
     * their wounds, their rounds, their orders and their nerve; what they're doing counts for more, and is held to
     * for a moment. A target coming into sight or going out of it makes them think again at once.
     */
    int decideArmed(Entity e, Entity t, boolean soldier, float stock, float dt) {
        e.goalCd -= dt;
        e.goalTime += dt;
        boolean fighting = e.goal == World.A_FIGHT || e.goal == World.A_WOUNDED || e.goal == World.A_FALLBACK;
        if (e.goalCd > 0 && (t != null) == fighting) return e.goal;
        e.goalCd = 0.4f + w.rnd.nextFloat() * 0.3f;
        java.util.Arrays.fill(w.armedScore, -1);
        boolean free = e.task == Dispatch.T_NONE || e.task == Dispatch.T_RESPOND;
        boolean posted = e.task == Dispatch.T_POST || e.task == Dispatch.T_HOLD || e.task == Dispatch.T_GUARD || e.task == Dispatch.T_CORDON;
        float hp = e.hp / e.maxHp;
        w.armedScore[World.A_DUTY] = 0.3f;
        if (t != null) {
            if (e.task != Dispatch.T_MOVE) w.armedScore[World.A_FIGHT] = 0.7f;
            if (hp < 0.35f && free) w.armedScore[World.A_WOUNDED] = 0.9f + (0.35f - hp);
            // Outnumbered: how many of the dead are on them, against how many guns there are here.
            if (free && e.role != Entity.ROLE_PILOT) {
                int near = w.spatial.countZombiesNear(e.x, e.y, 110);
                if (near >= 6) {
                    float odds = near / (float) Math.max(1, w.spatial.armedNear(e.x, e.y, 130));
                    // (Soldiers sent in close are meant to be there: it takes a lot more to turn them.)
                    // ((16.9) Each kind of police officer's nerve: see doctrine.)
                    float nerve = soldier ? (e.task == Dispatch.T_RESPOND ? 7 : 5) : World.doctrine(e) != null ? World.doctrine(e)[5] : 4;
                    // ((10.59) A veteran stands longer.)
                    nerve += World.rank(e) * 0.75f + (World.perk(e, World.P_NERVE) ? 2 : 0) + (w.ops.boosts(e) ? 2 : 0);
                    if (odds >= nerve - (e.goal == World.A_FALLBACK ? 1.5f : 0))
                        w.armedScore[World.A_FALLBACK] = 0.6f + 0.05f * (odds - nerve) + (stock < 0.4f ? 0.15f : 0) + 0.15f * (World.caution(e) - 0.5f)
                                + (e.kills > 10 ? -0.05f : 0);
                }
            }
        } else if (e.task == Dispatch.T_NONE && e.fear > 0 && (e.ammo > 0 || e.reserve > 0)) w.armedScore[World.A_SEARCH] = 0.5f;
        // ((27.4) About to be overrun: far more of the dead than there are guns here, and the guns running low - time to
        // go, together, while they still can, not fight on until the last magazine. (Soldiers sent in close take a good
        // deal more.)
        if (t != null && free && e.role != Entity.ROLE_PILOT && e.goal != World.A_FALLBACK && stock < 0.3f) {
            int near = w.spatial.countZombiesNear(e.x, e.y, 110);
            float pressure = near / (float) Math.max(1, w.spatial.armedNear(e.x, e.y, 130));
            float limit = soldier ? (e.task == Dispatch.T_RESPOND ? 9 : 7) : 3.5f + World.rank(e) * 0.5f + (w.ops.boosts(e) ? 1.5f : 0);
            if (near >= 6 && pressure >= limit) w.armedScore[World.A_FALLBACK] = Math.max(w.armedScore[World.A_FALLBACK], 0.9f);
        }
        // ((27.4) The others alongside are falling back to somewhere: they go with them, while the dead are close.)
        if (t != null && free && e.rallyT > w.time && e.role != Entity.ROLE_PILOT && w.spatial.countZombiesNear(e.x, e.y, 120) >= 3)
            w.armedScore[World.A_FALLBACK] = Math.max(w.armedScore[World.A_FALLBACK], 0.8f);
        if (posted && t != null) w.armedScore[World.A_FALLBACK] = -1;
        if (e.goal >= 0 && e.goal < World.A_COUNT && w.armedScore[e.goal] > 0) w.armedScore[e.goal] += 0.15f;
        int best = World.A_DUTY;
        for (int g = 0; g < World.A_COUNT; g++) if (w.armedScore[g] > w.armedScore[best]) best = g;
        if (best != e.goal) {
            e.goalTime = 0;
            // ((27.4) Not each on their own: the ones alongside fall back with them, to the same place.)
            if (best == World.A_FALLBACK && e.rallyT <= w.time && t != null) fallBackTogether(e, t, soldier);
            else if (best == World.A_FALLBACK && w.rnd.nextFloat() < 0.35f)
                w.dispatch.chatter(soldier ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, e,
                        w.rnd.nextBoolean() ? "Too many of them! Falling back!" : "We can't hold here! Pull back to the others!", e.x, e.y);
        }
        e.goal = best;
        return best;
    }

    /**
     * Falling back from a crowd of the dead: away from the thick of them, towards the nearest others with guns who
     * aren't in the middle of it (or, low on rounds, towards somewhere to restock), turning to shoot the closest
     * when they get near.
     */
    void fallBack(Entity e, Entity t, float range, boolean soldier, float dt) {
        float[] c = w.spatial.zombieCentre(e.x, e.y, 110);
        float ax = e.x - c[0], ay = e.y - c[1], ad = (float) Math.sqrt(ax * ax + ay * ay);
        if (ad < 1) {
            ax = e.x - t.x;
            ay = e.y - t.y;
            ad = (float) Math.sqrt(ax * ax + ay * ay) + 0.001f;
        }
        ax /= ad;
        ay /= ad;
        // Somewhere to fall back to: others holding (not in the middle of it), else a restock point.
        float gx = 0, gy = 0;
        e.protectorCd -= dt;
        if (e.protectorCd <= 0 || e.protector == null || e.protector.dead) {
            e.protectorCd = 0.6f;
            e.protector = null;
            float bd = 360 * 360;
            for (int k = 0; k < 6; k++) {
                // (A few places back along the way out, nearest first.)
                float px = e.x + ax * (70 + k * 50), py = e.y + ay * (70 + k * 50);
                Entity o = w.spatial.nearestArmed(px, py, 90);
                if (o != null && o != e && w.spatial.countZombiesNear(o.x, o.y, 60) < 3) {
                    float d = (o.x - e.x) * (o.x - e.x) + (o.y - e.y) * (o.y - e.y);
                    if (d < bd) {
                        bd = d;
                        e.protector = o;
                    }
                    break;
                }
            }
        }
        if (e.protector != null && !e.protector.dead) {
            float px = e.protector.x - e.x, py = e.protector.y - e.y, pd = (float) Math.sqrt(px * px + py * py) + 0.001f;
            if (pd > 30 && (px * ax + py * ay) / pd > -0.3f) {
                gx = px / pd;
                gy = py / pd;
            }
        }
        // ((22.0) Overwhelmed, with nobody holding behind them: back to their own station or base, where there are walls
        // and others, not just a few steps from the dead - and with more of a will to get there.)
        boolean overrun = e.protector == null && w.spatial.countZombiesNear(e.x, e.y, 110) >= 8;
        if ((e.protector == null || e.protector.dead) && (overrun || e.ammo + e.reserve < World.fullReserve(e) * 0.4f)) {
            City.Facility f = w.ammo.restockPoint(e);
            if (f != null) {
                float fx = f.x - e.x, fy = f.y - e.y, fd = (float) Math.sqrt(fx * fx + fy * fy) + 0.001f;
                if ((fx * ax + fy * ay) / fd > (overrun ? -0.6f : -0.3f)) {
                    gx = fx / fd * (overrun ? 1.6f : 1);
                    gy = fy / fd * (overrun ? 1.6f : 1);
                    if (overrun && !e.fallbackSaid && fd > 200) {
                        e.fallbackSaid = true;
                        w.fallbacks++;
                        w.dispatch.chatter(soldier ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, e, "Too many of them! Falling back to "
                                + f.name.trim() + "!", e.x, e.y);
                    }
                }
            }
        }
        if (!overrun) e.fallbackSaid = false;
        // ((27.4) Falling back together: all of them for the same place (the command post, a safe zone, their vehicle,
        // the station), the two nearest the dead hanging back to cover the rest.)
        boolean rear = false;
        if (e.rallyT > w.time) {
            float rx = e.rallyX - e.x, ry = e.rallyY - e.y, rd = (float) Math.sqrt(rx * rx + ry * ry) + 0.001f;
            if (rd < 40) e.rallyT = 0;
            else if ((rx * ax + ry * ay) / rd > -0.5f) {
                gx = rx / rd * 1.4f;
                gy = ry / rd * 1.4f;
            }
            rear = rearGuard(e, c);
        }
        float tx = t.x - e.x, ty = t.y - e.y, td = (float) Math.sqrt(tx * tx + ty * ty) + 0.001f;
        float mx = ax * 0.6f + gx * 0.4f, my = ay * 0.6f + gy * 0.4f, ml = (float) Math.sqrt(mx * mx + my * my) + 0.001f;
        // Turning to fire: when one gets close, or they've opened a gap and can afford to. ((27.4) The rear guard keeps
        // firing as it goes, a little slower; the rest run for it.)
        boolean shoot = rear ? td < range && e.reload <= 0 : td < 45 || (e.rallyT <= w.time && td > 70 && td < range && e.reload <= 0);
        w.movement.flee(e, mx / ml, my / ml, e.runSpeed * (rear ? 0.7f : shoot ? 0.75f : 0.95f));
        if (shoot) w.combat.aimAndFire(e, t, td, range, dt);
        else e.aiming = false;
    }

    /** ((27.4) Groups that have fallen back together, and when it was last said on the radio. */
    int groupFallbacks;
    float groupFallbackSaidAt = -99;

    /**
     * ((27.4) Too many of the dead for them: e and the others alongside (of their own, free to go, not dug in) fall back
     * together to one place - their side's command post, a safe zone, their vehicle or the station - rather than each
     * running alone.
     */
    void fallBackTogether(Entity e, Entity t, boolean soldier) {
        float[] c = w.spatial.zombieCentre(e.x, e.y, 120);
        float[] r = rallyPoint(e, c[0], c[1]);
        if (r == null) {
            float ax = e.x - c[0], ay = e.y - c[1], ad = (float) Math.sqrt(ax * ax + ay * ay);
            if (ad < 1) {
                ax = e.x - t.x;
                ay = e.y - t.y;
                ad = (float) Math.sqrt(ax * ax + ay * ay) + 0.001f;
            }
            r = w.city.findWalkable(e.x + ax / ad * 260, e.y + ay / ad * 260);
            if (r == null) return;
        }
        int n = 0;
        for (int i = 0, m = w.combat.mates(e, Combat.MATE_R); i <= m; i++) {
            Entity o = i == m ? e : w.combat.mateAt(i);
            if (o.type != e.type || o == w.controlled || (o.task != Dispatch.T_NONE && o.task != Dispatch.T_RESPOND) || dugIn(o)) continue;
            o.rallyX = r[0];
            o.rallyY = r[1];
            o.rallyT = w.time + 9;
            if (o.histN == 0 || w.time - o.histT[(o.histN - 1) % Entity.HIST] > 20) w.history(o, "fell back to " + w.city.placeName(r[0], r[1]));
            if (o != e) {
                o.goal = World.A_FALLBACK;
                o.goalCd = 1.5f;
                o.goalTime = 0;
                n++;
            }
        }
        groupFallbacks++;
        if (w.time - groupFallbackSaidAt > 10) {
            groupFallbackSaidAt = w.time;
            w.dispatch.chatter(soldier ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, e, (n > 0 ? "On me! Fall back together to " : "Falling back to ")
                    + w.city.placeName(r[0], r[1]) + "!", e.x, e.y);
        }
    }

    /**
     * ((27.4) Where a group falling back makes for: the nearest of their side's command post (set up), an open safe zone,
     * their own vehicle and their station or base - not one on the far side of the dead (cx, cy) - or null.
     */
    float[] rallyPoint(Entity e, float cx, float cy) {
        float best = 900 * 900, bx = 0, by = 0;
        boolean got = false;
        Ops.Post p = e.type == Entity.COP ? w.ops.police : e.type == Entity.SOLDIER ? w.ops.army : null;
        for (int k = 0; k < 4; k++) {
            float x, y;
            if (k == 0) {
                if (p == null || !p.up) continue;
                x = p.x;
                y = p.y;
            } else if (k == 1) {
                Dispatch.SafeZone z = null;
                float zd = Float.MAX_VALUE;
                for (int i = 0; i < w.dispatch.zones.size(); i++) {
                    Dispatch.SafeZone o = w.dispatch.zones.get(i);
                    if (o.removed || !o.open || o.fallingBack) continue;
                    float d = (o.x - e.x) * (o.x - e.x) + (o.y - e.y) * (o.y - e.y);
                    if (d < zd) {
                        zd = d;
                        z = o;
                    }
                }
                if (z == null) continue;
                x = z.x;
                y = z.y;
            } else if (k == 2) {
                if (e.rig == null || e.rig.broken || e.rig.removedFromFleet) continue;
                x = e.rig.x;
                y = e.rig.y;
            } else {
                City.Facility f = w.ammo.restockPoint(e);
                if (f == null) continue;
                x = f.x;
                y = f.y;
            }
            float dx = x - e.x, dy = y - e.y, d2 = dx * dx + dy * dy;
            if (d2 < 60 * 60 || d2 >= best) continue;
            // (Not through them: away from the dead, or at least not towards them.)
            float zx = cx - e.x, zy = cy - e.y, zl = (float) Math.sqrt(zx * zx + zy * zy) + 0.001f, dl = (float) Math.sqrt(d2);
            if ((dx * zx + dy * zy) / (dl * zl) > 0.3f || w.spatial.countZombiesNear(x, y, 80) >= 3) continue;
            best = d2;
            bx = x;
            by = y;
            got = true;
        }
        return got ? new float[]{bx, by} : null;
    }

    /** ((27.4) One of the two of a group falling back nearest the dead (at c): they cover the others. */
    boolean rearGuard(Entity e, float[] c) {
        float mine = (e.x - c[0]) * (e.x - c[0]) + (e.y - c[1]) * (e.y - c[1]);
        int closer = 0;
        for (int i = 0, m = w.combat.mates(e, Combat.MATE_R); i < m && closer < 2; i++) {
            Entity o = w.combat.mateAt(i);
            if (o.type != e.type || o.rallyT <= w.time) continue;
            if ((o.x - c[0]) * (o.x - c[0]) + (o.y - c[1]) * (o.y - c[1]) < mine) closer++;
        }
        return closer < 2;
    }

    /** Radios a sighting: other police and soldiers nearby with nothing in view come to look. */
    /** Police and soldiers on their feet within r of (x, y). */
    /**
     * Whether police and soldiers have their guns out (10.24): only in an emergency, when they're on a call,
     * aiming, guarding, or once the whole city knows about the outbreak (until it is over). On an ordinary day the rifle stays slung and the pistol
     * holstered.
     */
    boolean weaponsOut(Entity e) {
        if (e.aiming || e.reload > 0 || (w.alert >= 2 && !w.recovering)) return true;
        int t = e.task;
        return t == Dispatch.T_RESPOND || t == Dispatch.T_GUARD || e.onScene;
    }

    /**
     * (13.8) The busiest fights right now, the most of the dead first (at most max): open calls with the dead about,
     * and safe zones with the dead at the wall. Two spots almost on top of each other count as one.
     */
    java.util.List<World.HotSpot> hotSpots(int max) {
        java.util.ArrayList<World.HotSpot> out = new java.util.ArrayList<World.HotSpot>();
        if (w.hashSize < 0) w.spatial.buildHash();
        for (int i = 0; i < w.dispatch.incidents.size(); i++) {
            Dispatch.Incident inc = w.dispatch.incidents.get(i);
            if (inc.resolved) continue;
            addHot(out, inc.x, inc.y, inc.place, World.HOT_REACH, false);
        }
        for (int i = 0; i < w.dispatch.zones.size(); i++) {
            Dispatch.SafeZone z = w.dispatch.zones.get(i);
            if (z.removed) continue;
            addHot(out, z.x, z.y, z.place, z.r + 60, true);
        }
        java.util.Collections.sort(out, new java.util.Comparator<World.HotSpot>() {
            public int compare(World.HotSpot a, World.HotSpot b) {
                return a.dead != b.dead ? b.dead - a.dead : b.armed - a.armed;
            }
        });
        while (out.size() > max) out.remove(out.size() - 1);
        return out;
    }

    void addHot(java.util.List<World.HotSpot> out, float x, float y, String place, float r, boolean zone) {
        int dead = w.spatial.countZombiesNear(x, y, r);
        if (dead <= 0) return;
        World.HotSpot h = new World.HotSpot();
        h.x = x;
        h.y = y;
        h.place = place != null ? place : w.city.placeName(x, y);
        h.dead = dead;
        h.armed = w.spatial.armedNear(x, y, r);
        h.zone = zone;
        for (int i = 0; i < out.size(); i++) {
            World.HotSpot o = out.get(i);
            if (Math.abs(o.x - x) < World.HOT_REACH && Math.abs(o.y - y) < World.HOT_REACH) {
                // (The same fight: the busier of the two, a safe zone's name over a call's.)
                if (dead > o.dead || (zone && !o.zone)) out.set(i, h);
                return;
            }
        }
        out.add(h);
    }

    void shareSighting(Entity e, float x, float y) {
        // ((10.51) Over the radio, and to the other service too: further than before, the call board hears of it,
        // and when the police and the army are both about, one calls the other in.)
        float r = 260;
        boolean soldier = e.type == Entity.SOLDIER;
        if (w.spatial.countZombiesNear(x, y, 90) >= 2) w.dispatch.sighting(x, y, null);
        boolean otherSide = false;
        int cx0 = Math.max(0, (int) ((e.x - r) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((e.x + r) / World.CELL));
        int cy0 = Math.max(0, (int) ((e.y - r) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((e.y + r) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c] + w.zCount[c], end = k + w.aCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o == e || o.dead || !o.isArmed() || o.aiming || o.fear > 0 || o.task != Dispatch.T_NONE) continue;
                    if ((o.x - e.x) * (o.x - e.x) + (o.y - e.y) * (o.y - e.y) > r * r) continue;
                    o.threatX = x;
                    o.threatY = y;
                    o.fear = 12;
                    if ((o.type == Entity.SOLDIER) != soldier && (o.type == Entity.COP || o.type == Entity.SOLDIER)) otherSide = true;
                }
            }
        if (otherSide && w.time > w.jointSaid) {
            w.jointSaid = w.time + 25;
            String where = w.city.placeName(x, y);
            w.dispatch.say(soldier ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, e, soldier
                    ? "Army to police units: we have the dead at " + where + ". Hold the perimeter, we're moving in."
                    : "Police to army: the dead at " + where + ", more than we can hold. Requesting your support.", x, y);
        }
    }

    /** A militia member: hold the post, shoot what comes, and give up once the bullets are gone. */
    void thinkMilitia(Entity e, float dt) {
        // ((25.0) A base's guard tops up from its workshop's store.)
        if (e.base != null && e.ammo + e.reserve <= 6 && e.base.stash >= 10) {
            e.base.stash -= 10;
            e.reserve += 10;
        }
        if (e.holdout != null && e.ammo + e.reserve <= 4 && e.holdout.stash > 0) {
            // Back to the group's stash for more.
            int take = Math.min(e.holdout.stash, 30);
            e.holdout.stash -= take;
            e.reserve += take;
        }
        if (e.ammo + e.reserve <= 0 && e.reload <= 0 || e.building == null || e.building.collapsed) {
            e.militia = false;
            // (One of a group: out of bullets, they go inside with the others.)
            if (!w.indoors.apocalypse() && e.holdout != null && e.building != null && !e.building.collapsed) {
                e.task = Dispatch.T_HIDE;
                e.fleeTimer = 0;
            }
            return;
        }
        // ((12.0) Reloading is seen to with everyone else's (see think): done here as well, they reloaded twice as fast.)
        e.fear = 5;
        // ((25.0) Up a watchtower: they see and shoot further.)
        float reach = (e.weapon == Entity.W_HUNTING ? 260 : 150) * (e.onTower ? 1.5f : 1);
        Entity t = w.combat.pickTarget(e, reach);
        if (t != null) {
            float d = (float) World.hypot(t.x - e.x, t.y - e.y);
            if (d < 14) {
                float ax = e.x - t.x, ay = e.y - t.y;
                w.movement.flee(e, ax / (d + 0.01f), ay / (d + 0.01f), e.runSpeed);
            } else w.movement.steer(e, 0, 0, 0);
            w.combat.aimAndFire(e, t, d, e.onTower ? 225 : 150, dt);
            return;
        }
        w.holdoutLife.standAt(e, e.postX, e.postY, 1);
    }

    /** The commander calls in armour or air support when the fighting around them gets heavy. */
    void command(Entity e, float dt) {
        e.screamCd -= dt;
        if (e.screamCd > 0) return;
        e.screamCd = 3;
        int near = w.spatial.countZombiesNear(e.x, e.y, 220);
        if (near < 12) return;
        e.screamCd = 45;
        w.dispatch.heavyContact(e, e.x, e.y, near);
    }

    Entity squadLead(Entity e) {
        if (w.lodFrame - w.squadRosterAt >= 30 || w.lodFrame < w.squadRosterAt) {
            w.squadRosterAt = w.lodFrame;
            for (ArrayList<Entity> l : w.squadRoster.values()) l.clear();
            for (int i = 0, n = w.entities.size(); i < n; i++) {
                Entity o = w.entities.get(i);
                if (o.dead || o.type != Entity.SOLDIER) continue;
                ArrayList<Entity> l = w.squadRoster.get(o.squad);
                if (l == null) w.squadRoster.put(o.squad, l = new ArrayList<Entity>());
                l.add(o);
            }
        }
        ArrayList<Entity> squad = w.squadRoster.get(e.squad);
        if (squad == null) return null;
        Entity best = null;
        for (int i = 0, n = squad.size(); i < n; i++) {
            Entity o = squad.get(i);
            if (o == e || o.dead || o.type != Entity.SOLDIER || o.squad != e.squad || o.member >= e.member) continue;
            if (o.task != Dispatch.T_NONE) continue;
            if (best == null || o.member < best.member) best = o;
        }
        return best;
    }
}
