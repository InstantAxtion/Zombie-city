package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Sound: gunfire and other noise the dead hear and come to, and how loud the fighting has been where.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class Noise {
    private final World w;

    Noise(World w) {
        this.w = w;
    }

    /** ((26.0) A gunshot or a blast: heard by the dead further off than other noise, and kept in the battle heat. */
    void battleNoise(float x, float y, float radius) {
        noise(x, y, radius * World.BATTLE_CARRY);
        int bw = (int) Math.ceil(w.city.worldW() / World.BATTLE_CELL), bh = (int) Math.ceil(w.city.worldH() / World.BATTLE_CELL);
        if (w.battleHeat == null || w.battleW != bw || w.battleHeat.length != bw * bh) {
            w.battleW = bw;
            w.battleHeat = new float[bw * bh];
            w.battleX = new float[bw * bh];
            w.battleY = new float[bw * bh];
        }
        int i = Math.min(bh - 1, Math.max(0, (int) (y / World.BATTLE_CELL))) * bw + Math.min(bw - 1, Math.max(0, (int) (x / World.BATTLE_CELL)));
        float wt = radius / 200f;
        w.battleHeat[i] += wt;
        w.battleX[i] += x * wt;
        w.battleY[i] += y * wt;
        if (w.battleHeat[i] > w.battleMax) w.battleMax = w.battleHeat[i];
    }

    /** ((26.0) The battle heat dying away (every second or so: keep is what's left of it). */
    void coolBattle(float keep) {
        if (w.battleHeat == null) return;
        float max = 0;
        for (int i = 0; i < w.battleHeat.length; i++) {
            float h = w.battleHeat[i];
            if (h <= 0) continue;
            if (h < 0.05f) {
                w.battleHeat[i] = w.battleX[i] = w.battleY[i] = 0;
                continue;
            }
            w.battleHeat[i] = h * keep;
            w.battleX[i] *= keep;
            w.battleY[i] *= keep;
            if (h > max) max = h;
        }
        w.battleMax = max * keep;
        if (w.battleDrawn >= 10 && w.time - w.battleSaid > 75) {
            w.battleSaid = w.time;
            w.dispatch.say(Dispatch.WHO_INFO, null, "The fighting at " + w.city.placeName(w.battleDrawX, w.battleDrawY)
                    + " can be heard for streets around. The dead are following the noise in - about " + (w.battleDrawn / 5 * 5) + " of them.", w.battleDrawX, w.battleDrawY);
        }
        w.battleDrawn = 0;
    }

    void noise(float x, float y, float radius) {
        w.wilds.scareBirds(x, y, radius);
        // (10.32) How loud the fighting has been round here lately, for hordes further off to come to.
        if (w.fightHeat == null) w.fightHeat = new float[heatW() * heatW()];
        int hx = Math.min(heatW() - 1, Math.max(0, (int) (x / World.HEAT_CELL))), hy = Math.min(heatW() - 1, Math.max(0, (int) (y / World.HEAT_CELL)));
        w.fightHeat[hy * heatW() + hx] += radius / 200f;
        // ((17.5) And kept for good, for the after-action report's map.)
        if (w.battleMap == null || w.battleMap.length != w.fightHeat.length) w.battleMap = new float[w.fightHeat.length];
        w.battleMap[hy * heatW() + hx] += radius / 200f;
        boolean near = false;
        for (int i = 0; i < w.noiseRings.size() && !near; i++) {
            float[] r = w.noiseRings.get(i);
            if (r[3] < 0.5f && Math.abs(r[0] - x) < 40 && Math.abs(r[1] - y) < 40) near = true;
        }
        if (!near && w.noiseRings.size() < 60) w.noiseRings.add(new float[]{x, y, radius, 0});
        int cx0 = Math.max(0, (int) ((x - radius) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((x + radius) / World.CELL));
        int cy0 = Math.max(0, (int) ((y - radius) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((y + radius) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c], end = w.cellStart[c] + w.zCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o.dead || o.hp <= 0 || !o.isZombie()) continue;
                    float ddx = o.x - x, ddy = o.y - y;
                    if (ddx * ddx + ddy * ddy > radius * radius) continue;
                    // Already there and found nothing: it doesn't keep them hanging around.
                    if (ddx * ddx + ddy * ddy < 45 * 45 && o.memory <= 0) continue;
                    o.noiseX = x + w.rnd.nextFloat() * 30 - 15;
                    o.noiseY = y + w.rnd.nextFloat() * 30 - 15;
                    o.noiseTimer = 14;
                    // ((10.59) One of a horde heard it: the whole horde wakes and surges for it.)
                    Entity lead = o.leadsHorde ? o : o.hordeLeader;
                    if (lead != null && !lead.dead && lead.surgeCd <= 0 && w.surging.size() < 8 && !w.surging.contains(lead)) {
                        lead.surgeCd = 8;
                        lead.noiseX = x;
                        lead.noiseY = y;
                        w.surging.add(lead);
                    }
                }
                // People who hear it know something is wrong (once there's anything to be wrong about).
                if (w.zombies > 0)
                    for (int k = w.cellStart[c] + w.zCount[c], end = w.cellStart[c] + w.cellCount[c]; k < end; k++) {
                        Entity o = w.sorted[k];
                        if (o.dead || o.type != Entity.CIVILIAN || o.aware) continue;
                        float ddx = o.x - x, ddy = o.y - y;
                        if (ddx * ddx + ddy * ddy < radius * radius * 0.6f) o.aware = true;
                    }
            }
    }

    /**
     * Everyone (not just the dead) reacts to a gunshot: people nearby run from whatever is being shot at, armed units
     * that aren't busy go and see what it is, raiders come looking, and medics and firefighters keep their heads down.
     */
    void heardShot(Entity shooter, Entity target, float radius) {
        if (shooter == null || target == null) return;
        float x = shooter.x, y = shooter.y;
        // Shooting at the dead, the danger is where the bullets are going; at people, it's the gun itself.
        boolean atDead = target.isZombie();
        // ((10.45) Patrol cars on their rounds come to a fight they can hear.)
        if (atDead && (shooter.type == Entity.COP || shooter.type == Entity.SOLDIER || shooter.hasGun)
                && w.spatial.countZombiesNear(target.x, target.y, 90) >= 3)
            w.fleet.heardFight(x, y, radius, target.x, target.y);
        float dx = atDead ? target.x : x, dy = atDead ? target.y : y;
        int cx0 = Math.max(0, (int) ((x - radius) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((x + radius) / World.CELL));
        int cy0 = Math.max(0, (int) ((y - radius) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((y + radius) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c] + w.zCount[c], end = w.cellStart[c] + w.cellCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o == shooter || o.dead || o == w.controlled) continue;
                    float ox = o.x - x, oy = o.y - y, d2 = ox * ox + oy * oy;
                    if (d2 > radius * radius) continue;
                    switch (o.type) {
                        case Entity.CIVILIAN: {
                            // Their own group's gun, sheltering behind the walls, armed and in the fight: they hold.
                            if (o.leader == shooter || shooter.leader == o || o.militia || o.task == Dispatch.T_SHELTER
                                    || o.ride != null || o.aiming) break;
                            o.aware = true;
                            o.fear = Math.max(o.fear, d2 < 120 * 120 ? 40 : 25);
                            float run = d2 < 120 * 120 ? 6 : 3.5f;
                            if (o.fleeTimer < run) {
                                o.fleeTimer = run;
                                o.threatX = dx;
                                o.threatY = dy;
                                if (w.rnd.nextFloat() < 0.04f) w.emit(Sfx.SCREAM, o.x, o.y);
                            }
                            break;
                        }
                        case Entity.COP:
                        case Entity.SOLDIER: {
                            if (o.aiming || o.scout || o.rig != null || o.ride != null) break;
                            if (o.ammo <= 0 && o.reserve <= 0) break;
                            // (10.45) One of their own (or anyone with a gun) in a fight with several of them: go and
                            // help, not just look. On the way to a call that's gone quiet, they're needed here instead.
                            boolean ally = shooter.type == Entity.COP || shooter.type == Entity.SOLDIER || shooter.hasGun;
                            int pressing = atDead && ally ? w.spatial.countZombiesNear(target.x, target.y, 90) : 0;
                            if (o.task == Dispatch.T_RESPOND && pressing >= 3 && o.incident != null && o.incident.zombiesNear == 0
                                    && d2 < 200 * 200) {
                                Dispatch.Incident here = w.dispatch.sighting(target.x, target.y, null);
                                if (here != null && here != o.incident) {
                                    o.incident = here;
                                    o.orderField = null;
                                    o.onScene = false;
                                }
                                break;
                            }
                            // Off duty for a moment: shots fired, so go and look (and keep at it while the shooting
                            // goes on, longer when it sounds like someone's in trouble).
                            if (o.task != Dispatch.T_NONE) break;
                            if (o.fear > 0 && pressing < 3) break;
                            o.threatX = dx + w.rnd.nextFloat() * 20 - 10;
                            o.threatY = dy + w.rnd.nextFloat() * 20 - 10;
                            o.fear = pressing >= 3 ? 25 : 10;
                            if (pressing >= 3 && o.shareCd <= 0 && w.rnd.nextFloat() < 0.15f) {
                                o.shareCd = 20;
                                w.dispatch.chatter(o.type == Entity.SOLDIER ? Dispatch.WHO_MILITARY : Dispatch.WHO_POLICE, o,
                                        "Shots fired by " + w.city.placeName(dx, dy) + ". Moving to assist.", dx, dy);
                            }
                            break;
                        }
                        default:
                            break;
                    }
                }
            }
    }

    int heatW() {
        return (int) Math.ceil(w.city.worldW() / World.HEAT_CELL);
    }

    /** The loudest fighting within reach of (x, y): {x, y} of its square, or null if it's been quiet. */
    float[] loudestFight(float x, float y, float reach) {
        if (w.fightHeat == null) return null;
        int n = heatW();
        float best = 25;
        float[] at = null;
        for (int j = 0; j < n; j++)
            for (int i = 0; i < n; i++) {
                float v = w.fightHeat[j * n + i];
                if (v <= best) continue;
                float cx = (i + 0.5f) * World.HEAT_CELL, cy = (j + 0.5f) * World.HEAT_CELL;
                if (World.hypot(cx - x, cy - y) > reach) continue;
                best = v;
                at = new float[]{cx, cy};
            }
        return at;
    }
}
