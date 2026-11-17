package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * The dead: what one of them does each step - hunting, hordes and their leaders, roaming, clawing at cars and walls, biting - and the giant.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class ZombieAI {
    private final World w;

    ZombieAI(World w) {
        this.w = w;
    }

    void makeAlpha(Entity z) {
        z.alpha = true;
        z.maxHp *= 2.5f;
        z.hp = z.maxHp;
        z.mass *= 1.5f;
        w.alphas++;
    }

    /** The alpha's dead: its horde comes apart - stunned for a moment, then every one for itself for a while. */
    void alphaDown(Entity a) {
        int n = 0;
        for (int i = 0, m = w.entities.size(); i < m; i++) {
            Entity o = w.entities.get(i);
            if (o.hordeLeader != a || o.dead) continue;
            o.hordeLeader = null;
            o.stun = Math.max(o.stun, 1.5f + w.rnd.nextFloat() * 2);
            o.scatterT = 25 + w.rnd.nextFloat() * 10;
            o.noiseTimer = 0;
            o.memory = 0;
            pickRoam(o);
            n++;
        }
        w.alphasKilled++;
        w.emit(Sfx.ROAR, a.x, a.y);
        if (n >= 4) {
            w.dispatch.say(Dispatch.WHO_INFO, null, "The alpha leading the horde at " + w.city.placeName(a.x, a.y) + " is down. " + n + " of them scatter.", a.x, a.y);
            w.stats.timeline("An alpha is killed: its horde of " + n + " scatters", a.x, a.y);
        }
    }

    /** (10.59) Every one of the horde behind lead runs for where it heard the noise. */
    void surge(Entity lead) {
        float x = lead.noiseX, y = lead.noiseY;
        int n = 0;
        for (int i = 0, m = w.entities.size(); i < m; i++) {
            Entity o = w.entities.get(i);
            if (o.dead || (o != lead && o.hordeLeader != lead)) continue;
            o.noiseX = x + w.rnd.nextFloat() * 40 - 20;
            o.noiseY = y + w.rnd.nextFloat() * 40 - 20;
            o.noiseTimer = 16;
            o.surge = 12;
            n++;
        }
        if (n >= 15 && w.time - w.surgeSaid > 20) {
            w.surgeSaid = w.time;
            w.dispatch.say(Dispatch.WHO_INFO, null, "A horde of about " + (n / 5 * 5) + " heard the noise on " + w.city.placeName(x, y)
                    + " and is surging towards it!", x, y);
        }
    }

    /**
     * ((22.0) A sound only the dead pay attention to (an engine going by): those within radius turn towards it, and
     * a horde with them. Nothing else - no alarm for the living, no mark on the fighting map.
     */
    void engineLure(float x, float y, float radius) {
        if (w.hashSize < 0) return;
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
                    // (Something better to do - prey in sight, or a louder noise not long ago - comes first.)
                    if (o.memory > 0 || o.noiseTimer > 9) continue;
                    o.noiseX = x;
                    o.noiseY = y;
                    o.noiseTimer = 7;
                    Entity lead = o.leadsHorde ? o : o.hordeLeader;
                    if (lead != null && !lead.dead && lead.memory <= 0 && lead.noiseTimer <= 9) {
                        lead.noiseX = x;
                        lead.noiseY = y;
                        lead.noiseTimer = 7;
                    }
                }
            }
    }

    /**
     * ((26.0) One of the dead with nothing to do (or a horde's leader) hears a fight that's been going on a while, not
     * far off: it heads for the thick of it. Not from right across the map - a lone one from a few streets, a horde
     * from a bit further.
     */
    void battleDraw(Entity z) {
        float reach = z.leadsHorde ? 1000 : 600, need = z.leadsHorde ? World.BATTLE_DRAW_HORDE : World.BATTLE_DRAW;
        if (w.battleMax < need) return;
        int bh = w.battleHeat.length / w.battleW;
        int x0 = Math.max(0, (int) ((z.x - reach) / World.BATTLE_CELL)), x1 = Math.min(w.battleW - 1, (int) ((z.x + reach) / World.BATTLE_CELL));
        int y0 = Math.max(0, (int) ((z.y - reach) / World.BATTLE_CELL)), y1 = Math.min(bh - 1, (int) ((z.y + reach) / World.BATTLE_CELL));
        float best = 0;
        int at = -1;
        for (int cy = y0; cy <= y1; cy++)
            for (int cx = x0; cx <= x1; cx++) {
                int c = cy * w.battleW + cx;
                float h = w.battleHeat[c];
                if (h < need) continue;
                // (From where in the square the shooting was.)
                float px = w.battleX[c] / h - z.x, py = w.battleY[c] / h - z.y, d = (float) Math.sqrt(px * px + py * py);
                if (d > reach) continue;
                // (The louder the better, the nearer the better.)
                float score = h / (1 + d / 400);
                if (score > best) {
                    best = score;
                    at = cy * w.battleW + cx;
                }
            }
        if (at < 0) return;
        float h = w.battleHeat[at], tx = w.battleX[at] / h, ty = w.battleY[at] / h;
        if ((tx - z.x) * (tx - z.x) + (ty - z.y) * (ty - z.y) < 70 * 70) return;
        z.noiseX = tx + w.rnd.nextFloat() * 50 - 25;
        z.noiseY = ty + w.rnd.nextFloat() * 50 - 25;
        z.noiseTimer = 12;
        w.battleDrawn += z.leadsHorde ? Math.max(1, z.hordeSize) : 1;
        w.battleDrawX = tx;
        w.battleDrawY = ty;
    }

    void thinkZombie(Entity z, float dt) {
        if (z.type == Entity.GIANT) giantStep(z, dt);
        else if (z.blocked && !w.barriers.isEmpty()) bashBarrier(z, dt);
        z.chargeCd -= dt;
        if (z.surge > 0) z.surge -= dt;
        if (z.surgeCd > 0) z.surgeCd -= dt;
        if (z.charge > 0) {
            charging(z, dt);
            return;
        }
        tickTimers(z, dt);
        // Senses: they see a long way in front, but only hear or smell what is close behind them. Once they
        // have spotted someone they keep track of them.
        // ((12.1) Balance: a little keener all round, 140 and 55 before.)
        float sight = w.trait[World.TR_KEEN] ? 210 : w.trait[World.TR_DULL] ? 120 : 165, near = w.trait[World.TR_KEEN] ? 85 : w.trait[World.TR_DULL] ? 45 : 65;
        Entity t = w.spatial.nearest(z, sight, false, true);
        if (t != null && z.memory <= 0) {
            float ddx = t.x - z.x, ddy = t.y - z.y, d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
            float facing = ((float) Math.cos(z.angle) * ddx + (float) Math.sin(z.angle) * ddy) / d;
            if (d > near && facing < 0.1f && t.want < t.runSpeed * 0.9f) t = null;
        }
        if (chaseStep(z, t, dt)) return;
        // At the wall of a safe zone with people inside: claw at it.
        if (w.dispatch.wallOwner != null && clawWall(z, dt)) return;
        if (lastSeenStep(z)) return;
        if (searchStep(z)) return;
        if (lookAbout(z)) return;
        if (feedStep(z, dt)) return;
        // ((26.0) A car stopped with people in it close by: at the car, before any door. The dead used to walk past a
        // stopped car full of people to a house further off, so in a street of occupied houses nothing clawed at one.)
        Fleet.Vehicle car = carFor(z, 100);
        if (car != null && Math.abs(car.speed) < World.CLAW_SPEED) {
            goForCar(z, car);
            return;
        }
        if (doorStep(z, dt)) return;
        // Cars: chase the ones driving past and claw at the ones that stop.
        // ((26.0) The clawing itself, and the damage, is the vehicle's: see clawAt.)
        if (car != null) {
            goForCar(z, car);
            return;
        }
        if (noiseStep(z)) return;
        // Hunger draws the dead towards the living they can smell nearby.
        int dist = w.city.fieldAt(w.city.humanDist, z.x, z.y);
        if (dist < World.SCENT && w.movement.followField(z, w.city.humanDist, z.speed)) return;
        if (z.leadsHorde) {
            roam(z);
            return;
        }
        Entity lead = z.hordeLeader;
        if (lead != null) {
            hordeFollow(z, lead);
            return;
        }
        w.movement.wander(z, z.speed * 0.45f);
    }

    /** Its clocks run down, and now and then it groans. */
    private void tickTimers(final Entity z, final float dt) {
        z.noiseTimer -= dt;
        z.screamCd -= dt;
        z.feedTimer -= dt;
        z.memory -= dt;
        z.searchTimer -= dt;
        z.moanCd -= dt;
        if (z.scatterT > 0) z.scatterT -= dt;
        if (w.rnd.nextFloat() < dt * 0.03f) w.emit(z.big() ? Sfx.GROAN_DEEP : Sfx.GROAN, z.x, z.y);
    }

    /** Someone in sight: a charge or a lunge, the moan that brings the others, and after them - round by its own angle, over or through what's between - to bite. True when that was their step. */
    private boolean chaseStep(final Entity z, final Entity t, final float dt) {
        if (t != null) {
            if (w.rnd.nextFloat() < 0.2f) z.frenzy = w.spatial.countZombiesNear(t.x, t.y, 35) >= 3;
            // A brute lowers its head and charges.
            // ((22.6) A bruiser lunges: shorter and more often.)
            if (z.type == Entity.BRUISER && z.chargeCd <= 0) {
                float cdx = t.x - z.x, cdy = t.y - z.y, cd = (float) Math.sqrt(cdx * cdx + cdy * cdy) + 0.001f;
                if (cd > 22 && cd < 65 && w.city.los(z.x, z.y, t.x, t.y)) {
                    z.charge = 0.7f;
                    z.chargeCd = 7 + w.rnd.nextFloat() * 4;
                    z.chargeX = cdx / cd;
                    z.chargeY = cdy / cd;
                    w.emit(Sfx.GROAN, z.x, z.y);
                    return true;
                }
            }
            if (z.type == Entity.BRUTE && z.chargeCd <= 0) {
                float cdx = t.x - z.x, cdy = t.y - z.y, cd = (float) Math.sqrt(cdx * cdx + cdy * cdy) + 0.001f;
                if (cd > 35 && cd < 100 && w.city.los(z.x, z.y, t.x, t.y)) {
                    z.charge = 1.3f;
                    z.chargeCd = 12 + w.rnd.nextFloat() * 5;
                    z.chargeX = cdx / cd;
                    z.chargeY = cdy / cd;
                    w.emit(Sfx.GROAN_DEEP, z.x, z.y);
                    return true;
                }
            }
            z.lastX = t.x + t.vx * 0.8f;
            z.lastY = t.y + t.vy * 0.8f;
            z.trackX = t.vx;
            z.trackY = t.vy;
            // ((12.1) They don't give up as soon: ten seconds, seven before.)
            z.memory = w.trait[World.TR_RELENTLESS] ? 20 : 10;
            z.searchTimer = 0;
            // The moan: other zombies nearby hear it and come to join the hunt.
            if (z.moanCd <= 0) {
                z.moanCd = 4 + w.rnd.nextFloat() * 2;
                // ((21.0) An alpha roars, and its whole horde surges for what it's seen.)
                if (z.alpha && z.leadsHorde && z.surgeCd <= 0 && w.surging.size() < 8 && !w.surging.contains(z)) {
                    z.surgeCd = 14;
                    z.noiseX = t.x;
                    z.noiseY = t.y;
                    w.surging.add(z);
                    w.emit(Sfx.ROAR, z.x, z.y);
                }
                herd(z, t.x, t.y);
            }
            z.feedTimer = 0;
            float ddx = t.x - z.x, ddy = t.y - z.y;
            float d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
            float ax = ddx / d, ay = ddy / d;
            // ((17.1) After someone in a building, or from one: by the doors and through the rooms.)
            if (World.WALK_IN && d > z.radius + t.radius + 2 && w.indoors.indoorMove(z, t.x, t.y, World.chaseSpeed(z, d))) return true;
            // Each zombie comes at its prey from its own angle, so a crowd closes in from all sides.
            if (d > 12 && d < 100) {
                float a = z.flank * Math.min(1, (d - 12) / 40f) * 0.7f;
                float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
                float rx = ax * ca - ay * sa, ry = ax * sa + ay * ca;
                ax = rx;
                ay = ry;
                if (z.blocked) {
                    z.blocked = false;
                    z.flank = -z.flank * 0.5f;
                }
            }
            // Seen through a fence or across the highway barrier: the way round (by smell), not into it.
            if (d > 16 && !w.city.passLine(z.x, z.y, t.x, t.y)) {
                // Right up against a safe zone's wall with them on the other side: tear at it.
                if (w.dispatch.wallOwner != null && clawWall(z, dt)) return true;
                // ((13.4) Just the other side of a fence or a barricade: straight at it, to climb over (see
                // zombieClimb), rather than all the way round.)
                if (d < World.CLIMB_REACH && w.movement.climbable(z, t)) {
                    w.movement.steer(z, ddx / d, ddy / d, World.chaseSpeed(z, d));
                    return true;
                }
                if (w.movement.followField(z, w.city.humanDist, World.chaseSpeed(z, d))) return true;
            }
            if (t.aloft && d < 22) {
                // Up a fire tower: the stairs are steep and narrow, but they climb (runners quickest).
                z.climb += dt;
                w.movement.steer(z, ddx / d, ddy / d, z.speed * 0.3f);
                float need = z.type == Entity.RUNNER ? 4 : z.type == Entity.ZOMBIE_DOG ? 14 : 8;
                if (z.climb >= need && z.biteCd <= 0) bite(z, t, ddx / d, ddy / d);
                return true;
            }
            z.climb = 0;
            // ((12.1) They run down anyone they've spotted from well off, not just the last few steps: at a walk
            // they never caught anyone.)
            w.movement.steer(z, ax, ay, World.chaseSpeed(z, d));
            if (d < z.radius + t.radius + 2.5f && z.biteCd <= 0) bite(z, t, ddx / d, ddy / d);
            return true;
        }
        return false;
    }

    /** Lost sight of them: to where they were last seen, into the car or the building they got into, on along their trail. True when that was their step. */
    private boolean lastSeenStep(final Entity z) {
        // Lost sight of them: go to where they were last seen, then search around there for a while.
        if (z.memory > 0) {
            float ddx = z.lastX - z.x, ddy = z.lastY - z.y;
            float d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
            // ((26.0) They got into a car that's stopped there: at the car, not on past it along their trail.)
            if (d < 60) {
                Fleet.Vehicle car = carFor(z, 50);
                if (car != null && Math.abs(car.speed) < World.CLAW_SPEED) {
                    goForCar(z, car);
                    return true;
                }
            }
            City.Building hid = w.spatial.occupiedNear(z.lastX, z.lastY, 40);
            // ((17.1) Last seen going into a building (or in one): in after them.)
            if (World.WALK_IN && d >= 6 && (w.city.insideOf(z.lastX, z.lastY) != null || w.city.insideOf(z.x, z.y) != null)) {
                w.indoors.indoorMove(z, z.lastX, z.lastY, z.speed * 1.2f);
                return true;
            }
            if (hid != null) {
                // They got inside: on to the door, with the rest of the dead (below).
                z.memory = 0;
                z.path = null;
            } else if (d < 14 || (z.blocked && d < 40)) {
                z.blocked = false;
                // (10.39) On the trail: on the way they were going, a little further, before casting about.
                float tl = (float) Math.sqrt(z.trackX * z.trackX + z.trackY * z.trackY);
                float nx = z.lastX + z.trackX / (tl + 0.001f) * 80, ny = z.lastY + z.trackY / (tl + 0.001f) * 80;
                if (tl > 5 && !w.city.solidAt(nx, ny) && w.city.los(z.x, z.y, nx, ny)) {
                    z.lastX = nx;
                    z.lastY = ny;
                    z.trackX = z.trackY = 0;
                    z.memory = 3;
                    return true;
                }
                z.memory = 0;
                z.searchTimer = 5 + w.rnd.nextFloat() * 4;
            } else {
                if (!w.movement.followField(z, w.city.humanDist, z.speed * 1.2f)) zombieLure(z, z.lastX, z.lastY, z.speed * 1.25f);
                return true;
            }
        }
        return false;
    }

    /** Casting about where the trail went cold. True when that was their step. */
    private boolean searchStep(final Entity z) {
        if (z.searchTimer > 0) {
            if (z.wanderTimer <= 0) {
                z.wanderTimer = 0.8f + w.rnd.nextFloat();
                z.wanderAngle = w.rnd.nextFloat() * World.TAU;
            }
            w.movement.steer(z, (float) Math.cos(z.wanderAngle), (float) Math.sin(z.wanderAngle), z.speed * 0.7f);
            if (z.blocked) {
                z.blocked = false;
                z.wanderTimer = 0;
            }
            return true;
        }
        return false;
    }

    /** Every second or two with nothing to chase: mischief, the horde, a fight within earshot, a fresh body to feed on. True when that was their step. */
    private boolean lookAbout(final Entity z) {
        if (z.taskTimer <= 0) {
            z.taskTimer = 1.5f + w.rnd.nextFloat();
            if (z.noiseTimer <= 0) mischief(z);
            if (z.dead) return true;
            thinkHorde(z);
            // ((26.0) A fight that's been going on a while within earshot: off to it (a horde behind its leader).)
            if (z.noiseTimer <= 0 && z.hordeLeader == null && w.battleHeat != null) battleDraw(z);
            // Stop to feed on a fresh body.
            if (z.feedTimer <= 0 && z.noiseTimer <= 0 && w.rnd.nextFloat() < 0.4f) {
                World.Corpse c = freshCorpse(z.x, z.y, 45);
                if (c != null) {
                    z.feedTimer = 5 + w.rnd.nextFloat() * 7;
                    z.threatX = c.x + w.rnd.nextFloat() * 6 - 3;
                    z.threatY = c.y + w.rnd.nextFloat() * 6 - 3;
                }
            }
        }
        return false;
    }

    /** Feeding on a body. True when that was their step. */
    private boolean feedStep(final Entity z, final float dt) {
        if (z.feedTimer > 0) {
            float ddx = z.threatX - z.x, ddy = z.threatY - z.y;
            float d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
            if (d > 5 && !z.blocked) w.movement.steer(z, ddx / d, ddy / d, z.speed);
            else {
                w.movement.steer(z, 0, 0, 0);
                z.angle = World.turn(z.angle, (float) Math.atan2(ddy, ddx), dt * 4);
                if (w.gore && w.rnd.nextFloat() < dt * 1.5f) w.effects.bloodBurst(z.threatX, z.threatY, 2, 0, 0);
            }
            return true;
        }
        return false;
    }

    /** People shut in a building near by: to its door to batter it, or clawing at its walls with the crowd. True when that was their step. */
    private boolean doorStep(final Entity z, final float dt) {
        // People barricaded indoors: batter the door down.
        City.Building target = null;
        float best = 150 * 150;
        if (w.occGrid == null || w.occTimer <= 0) w.movement.buildOccGrid();
        int ocx = (int) (z.x / World.OCC_CELL), ocy = (int) (z.y / World.OCC_CELL);
        for (int gy = Math.max(0, ocy - 1); gy <= Math.min(w.occGH - 1, ocy + 1); gy++)
            for (int gx = Math.max(0, ocx - 1); gx <= Math.min(w.occGW - 1, ocx + 1); gx++) {
                java.util.ArrayList<City.Building> cell = w.occGrid[gy * w.occGW + gx];
                if (cell == null) continue;
                for (int i = 0, n = cell.size(); i < n; i++) {
                    City.Building b = cell.get(i);
                    if (b.occupants.isEmpty() && b.visitors.isEmpty()) continue;
                    if (b == z.ignoreB && w.time < z.ignoreUntil) continue;
                    float ddx = b.doorX - z.x, ddy = b.doorY - z.y, d2 = ddx * ddx + ddy * ddy;
                    if (d2 < best) {
                        best = d2;
                        target = b;
                    }
                }
            }
        if (target != null) {
            float ddx = target.doorX - z.x, ddy = target.doorY - z.y;
            float d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
            // ((10.58) A door that's held for a good while, with nobody to be seen: this one loses interest and
            // drifts off, and leaves the place be for a minute or two.)
            if (z.doorAt != target) {
                z.doorAt = target;
                z.doorTime = 0;
            }
            if (d < 70) z.doorTime += dt;
            if (z.doorTime > 25 + (z.nameSeed & 0xFF) / 10f && target.barricade > 20 && target.lurkers == 0) {
                z.ignoreB = target;
                z.ignoreUntil = w.time + 60 + w.rnd.nextFloat() * 60;
                z.doorTime = 0;
                z.noiseTimer = 0;
                return true;
            }
            // At the door, or pressed up behind the others at it: batter it.
            boolean atDoor = d < 14 || (d < 30 && (z.blocked || w.spatial.countZombiesNear(target.doorX, target.doorY, 16) >= 2));
            // Crowded out of the door, or up against the building's wall: claw at the walls and windows
            // (slowly weakening the barricade) all round it, rather than shoving at the back of the crowd.
            boolean atWall = !atDoor && (z.blocked || z.stuckTime > 0.6f) && z.x > target.x0 - 12 && z.x < target.x1 + 12
                    && z.y > target.y0 - 12 && z.y < target.y1 + 12;
            if (!atDoor && !atWall && d < 70 && w.spatial.countZombiesNear(target.doorX, target.doorY, 24) >= 4) {
                // Spread round the building instead.
                float a = (z.nameSeed & 0xFFF) / 4096f * World.TAU;
                float bx = (target.x0 + target.x1) / 2, by = (target.y0 + target.y1) / 2;
                float rx = (target.x1 - target.x0) / 2 + 9, ry = (target.y1 - target.y0) / 2 + 9;
                float px = bx + (float) Math.cos(a) * rx, py = by + (float) Math.sin(a) * ry;
                if (!w.city.solidAt(px, py)) {
                    zombieWalkTo(z, px, py, z.speed);
                    if (World.hypot(px - z.x, py - z.y) < 8) atWall = true;
                    else return true;
                }
            }
            if (atWall) {
                float wx = Math.max(target.x0, Math.min(target.x1, z.x)) - z.x, wy = Math.max(target.y0, Math.min(target.y1, z.y)) - z.y;
                w.movement.steer(z, 0, 0, 0);
                z.angle = World.turn(z.angle, (float) Math.atan2(wy, wx), dt * 4);
                target.barricade -= (z.type == Entity.GIANT ? 8 : z.type == Entity.BRUTE ? 2.4f : z.type == Entity.BRUISER ? 1.4f : 0.8f) * dt * World.doorWeakness(target);
                target.calmTimer = 0;
                if (w.rnd.nextFloat() < dt * 0.6f) w.emit(Sfx.THUD, z.x, z.y);
                z.stuckTime = 0;
                return true;
            }
            if (atDoor) {
                if (d > 9 && !z.blocked) w.movement.steer(z, ddx / d, ddy / d, z.speed * 0.5f);
                else w.movement.steer(z, 0, 0, 0);
                z.angle = World.turn(z.angle, (float) Math.atan2(ddy, ddx), dt * 4);
                // ((10.58) Doors hold longer: a minute or two against a handful, not seconds.)
                float dps = (z.type == Entity.GIANT ? 40 : z.type == Entity.BRUTE ? 10 : 4.5f) * (d < 14 ? 1 : 0.5f);
                // (A group's fortified door holds a lot longer.)
                if (target.holdout != null) dps *= 1 - target.holdout.fort * 0.006f;
                // ((11.5) A bank's or a storage unit's door holds out far longer than a glass shop front.)
                dps *= World.doorWeakness(target);
                target.barricade -= dps * dt;
                target.calmTimer = 0;
                if (target.barricade < 70 && World.isShop(target)) w.indoors.smash(target);
                if (w.rnd.nextFloat() < dt * 1.2f) w.emit(w.rnd.nextFloat() < 0.5f ? Sfx.POUND : Sfx.THUD, target.doorX, target.doorY);
                return true;
            }
            // Find the way round to the door (not straight into the wall): every zombie at this building
            // shares one route.
            World.DoorRoute route = w.movement.doorField(target);
            if (route != null && w.movement.followDoor(z, route, z.speed * 1.1f)) return true;
            w.movement.steer(z, ddx / d, ddy / d, z.speed);
            return true;
        }
        return false;
    }

    /** Off to a noise it heard. True when that was their step. */
    private boolean noiseStep(final Entity z) {
        if (z.noiseTimer > 0) {
            float ddx = z.noiseX - z.x, ddy = z.noiseY - z.y;
            float d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
            if (d < 25) z.noiseTimer = 0;
            else {
                // ((21.0) From inside a building, or to a noise inside one: by the rooms and the door. One of the dead
                // indoors that heard something outside used to walk at the wall between them and stay there - which is
                // where the last zombie of many an outbreak was.)
                if (World.WALK_IN && w.indoors.indoorMove(z, z.noiseX, z.noiseY, z.surge > 0 ? z.runSpeed * 0.85f : z.speed * 1.2f)) return true;
                // (Round the buildings in the way, not into their walls: the noise is often a fight indoors. (10.39)
                // All the dead drawn by one noise share the one route there.)
                zombieLure(z, z.noiseX, z.noiseY, z.surge > 0 ? z.runSpeed * 0.85f : z.speed * 1.2f);
                if (z.stuckTime > 3) {
                    z.noiseTimer = 0;
                    z.path = null;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * What an idle zombie gets up to around town: bumping into parked cars (setting off their alarms) and putting in
     * shop windows.
     */
    void mischief(Entity z) {
        for (int i = 0, n = w.fleet.vehicles.size(); i < n; i++) {
            Fleet.Vehicle v = w.fleet.vehicles.get(i);
            if (v.type != Fleet.CAR || !v.parked || v.broken || v.alarm > 0) continue;
            if (Math.abs(v.x - z.x) < 14 && Math.abs(v.y - z.y) < 14 && w.rnd.nextFloat() < 0.3f) {
                v.alarm = 8 + w.rnd.nextFloat() * 4;
                w.noise.noise(v.x, v.y, 180);
                return;
            }
        }
        for (int k = 0; k < 4; k++) {
            City.Building b = w.city.buildingAt(z.x + (k == 0 ? City.T : k == 1 ? -City.T : 0), z.y + (k == 2 ? City.T : k == 3 ? -City.T : 0));
            if (b == null || Math.abs(b.doorX - z.x) > 16 || Math.abs(b.doorY - z.y) > 16) continue;
            if (World.isShop(b) && !b.smashed && w.rnd.nextFloat() < 0.12f) {
                w.indoors.smash(b);
                return;
            }
        }
    }

    /** A hunting zombie's moan draws in idle zombies nearby that haven't found anyone yet. */
    void herd(Entity z, float x, float y) {
        float r = w.trait[World.TR_PACK] ? 170 : w.trait[World.TR_LONER] ? 40 : 90;
        int cx0 = Math.max(0, (int) ((z.x - r) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((z.x + r) / World.CELL));
        int cy0 = Math.max(0, (int) ((z.y - r) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((z.y + r) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c], end = w.cellStart[c] + w.zCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o == z || o.dead || !o.isZombie() || o.memory > 0 || o.noiseTimer > 0) continue;
                    if ((o.x - z.x) * (o.x - z.x) + (o.y - z.y) * (o.y - z.y) > r * r) continue;
                    o.noiseX = x + w.rnd.nextFloat() * 20 - 10;
                    o.noiseY = y + w.rnd.nextFloat() * 20 - 10;
                    o.noiseTimer = 6;
                }
            }
        if (w.rnd.nextFloat() < 0.3f) w.emit(z.big() ? Sfx.GROAN_DEEP : Sfx.GROAN, z.x, z.y);
    }

    /**
     * ((21.3) Every horde's numbers in one look through everyone, at most every fifteen steps: how many follow each leader
     * and how many of those are strung out behind.
     */
    void hordeTally() {
        if (w.tallyAt >= 0 && w.lodFrame - w.tallyAt < 15 && w.lodFrame >= w.tallyAt) return;
        w.tallyAt = w.lodFrame;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity o = w.entities.get(i);
            Entity z = o.hordeLeader;
            if (z == null || o.dead) continue;
            if (z.tallyFrame != w.tallyAt) {
                z.tallyFrame = w.tallyAt;
                z.tallyN = 0;
                z.tallyBehind = 0;
            }
            z.tallyN++;
            if ((o.x - z.x) * (o.x - z.x) + (o.y - z.y) * (o.y - z.y) > (o.roamX + 70) * (o.roamX + 70)) z.tallyBehind++;
        }
    }

    /** Hordes: idle zombies bunch up behind a leader who roams the city, and small hordes merge. */
    void thinkHorde(Entity z) {
        if (z.hordeLeader != null && (z.hordeLeader.dead || !z.hordeLeader.leadsHorde)) z.hordeLeader = null;
        if (z.leadsHorde) {
            // ((21.3) From the tally made for every horde at once (hordeTally): each leader used to go through everyone
            // in the city to count its own, every time it thought.)
            hordeTally();
            if (!z.leadSeen) {
                z.leadSeen = true;
                z.leadStart = w.lodFrame;
            }
            // (Until a tally made since they took the lead, their followers are as last known: a new leader isn't
            // counted as alone and made to give it up before anyone's had the chance to join.)
            boolean fresh = w.tallyAt > z.leadStart;
            int size = fresh ? 1 + (z.tallyFrame == w.tallyAt ? z.tallyN : 0) : Math.max(3, z.hordeSize);
            int behind = fresh && z.tallyFrame == w.tallyAt ? z.tallyBehind : 0;
            z.hordeSize = size;
            // ((21.0) A horde this big has an alpha at its head.)
            if (size >= World.ALPHA_HORDE && !z.alpha) makeAlpha(z);
            // (10.39) Too many strung out behind: it slows to let them catch up.
            z.hordeWait = behind * 3 > size;
            w.biggestHorde = Math.max(w.biggestHorde, size);
            Entity other = nearestHordeLeader(z, 140);
            if (size < 3 || (other != null && other.hordeSize >= size)) {
                z.leadsHorde = false;
                z.hordeAnnounced = false;
                if (other != null) join(z, other);
                return;
            }
            if (size >= 15 && !z.hordeAnnounced) {
                z.hordeAnnounced = true;
                City.District dist = w.city.districtOf(z.x, z.y);
                w.dispatch.say(Dispatch.WHO_INFO, null, "A horde of about " + (size / 5 * 5) + " zombies is moving through "
                        + (dist != null ? dist.name + " (" + w.city.placeName(z.x, z.y) + ")" : w.city.placeName(z.x, z.y)) + ".", z.x, z.y);
                if (size >= 25) w.dispatch.heavyContact(null, z.x, z.y, size);
            }
            return;
        }
        z.leadSeen = false;
        if (z.hordeLeader != null) return;
        // ((21.0) Scattered when their alpha fell: on their own for a while.)
        if (z.scatterT > 0) return;
        Entity lead = nearestHordeLeader(z, 170);
        if (lead != null) join(z, lead);
        else if (w.spatial.countZombiesNear(z.x, z.y, 70) >= 5) {
            z.leadsHorde = true;
            z.hordeSize = 1;
            pickRoam(z);
        }
    }

    /** A brute's charge: straight ahead, bowling people over, until it hits something. */
    void charging(Entity z, float dt) {
        z.charge -= dt;
        // ((22.6) A bruiser's lunge is lighter than a brute's charge.)
        boolean lunge = z.type == Entity.BRUISER;
        w.movement.steer(z, z.chargeX, z.chargeY, z.runSpeed * (lunge ? 1.6f : 2f));
        z.angle = (float) Math.atan2(z.chargeY, z.chargeX);
        int cx0 = Math.max(0, (int) ((z.x - 14) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((z.x + 14) / World.CELL));
        int cy0 = Math.max(0, (int) ((z.y - 14) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((z.y + 14) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c] + w.zCount[c], end = w.cellStart[c] + w.cellCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o.dead || o.stun > 0) continue;
                    float r = z.radius + o.radius + 2;
                    if ((o.x - z.x) * (o.x - z.x) + (o.y - z.y) * (o.y - z.y) > r * r) continue;
                    o.stun = lunge ? 0.5f : 0.9f;
                    o.hp -= lunge ? 7 : 12;
                    o.hurt = 1;
                    o.killedByZombie = true;
                    float kb = lunge ? 45 : 80;
                    o.knockX = z.chargeX * kb / o.mass;
                    o.knockY = z.chargeY * kb / o.mass;
                    w.movement.tryMove(o, z.chargeX * (lunge ? 5 : 9) / o.mass, z.chargeY * (lunge ? 5 : 9) / o.mass);
                    w.emit(Sfx.THUD, o.x, o.y);
                }
            }
        if (z.blocked) {
            // Slammed into something: barricades and cars take a beating; the brute is dazed for a moment.
            z.blocked = false;
            z.charge = 0;
            z.stun = lunge ? 0.4f : 0.6f;
            if (!w.barriers.isEmpty()) for (int k = 0; k < (lunge ? 2 : 6); k++) bashBarrier(z, 0.5f);
            Fleet.Vehicle car = carFor(z, 20);
            if (car != null) w.fleet.damage(car, lunge ? 12 : 40, false);
            if (!lunge) w.shake = Math.max(w.shake, 0.3f);
            w.emit(lunge ? Sfx.THUD : Sfx.CRASH, z.x, z.y);
        }
    }

    void join(Entity z, Entity lead) {
        z.hordeLeader = lead;
        // (10.39) A place in the horde: how far back behind the one in front, and how far to the side. The
        // bigger the horde, the longer its tail.
        z.roamX = 10 + w.rnd.nextFloat() * (20 + Math.min(60, lead.hordeSize * 2.5f));
        z.roamY = (w.rnd.nextFloat() - 0.5f) * (24 + Math.min(30, lead.hordeSize));
    }

    /**
     * (10.39) Walking with a horde: in its place behind the one in front (back along the way it's heading,
     * not a ring round it), and when that place is round a corner or the horde has pulled ahead, along the
     * trail the leader left, instead of straight into the wall between.
     */
    void hordeFollow(Entity z, Entity lead) {
        float hx = lead.mx, hy = lead.my, hl = (float) Math.sqrt(hx * hx + hy * hy);
        if (hl < 0.1f) {
            hx = (float) Math.cos(lead.angle);
            hy = (float) Math.sin(lead.angle);
        } else {
            hx /= hl;
            hy /= hl;
        }
        float sx = lead.x - hx * z.roamX - hy * z.roamY, sy = lead.y - hy * z.roamX + hx * z.roamY;
        if (w.city.solidAt(sx, sy)) {
            // (Their place is in a wall just now: straight behind instead.)
            sx = lead.x - hx * z.roamX;
            sy = lead.y - hy * z.roamX;
        }
        float gx = sx - z.x, gy = sy - z.y;
        float d = (float) Math.sqrt(gx * gx + gy * gy) + 0.001f;
        float ld = (float) World.hypot(lead.x - z.x, lead.y - z.y);
        if (ld > 420) {
            z.hordeLeader = null;
            return;
        }
        if ((d > 50 || z.blocked) && (ld > 60 || !w.city.los(z.x, z.y, sx, sy))) {
            // Catching up: the newest bit of the leader's trail they can see from here.
            float[] tr = lead.trail;
            if (tr != null) {
                for (int k = 0; k < lead.trailN; k++) {
                    int i = ((lead.trailHead - 1 - k) % World.TRAIL + World.TRAIL) % World.TRAIL;
                    float px = tr[i * 2], py = tr[i * 2 + 1];
                    float pd = (float) World.hypot(px - z.x, py - z.y);
                    if (pd < 220 && pd > 6 && w.city.los(z.x, z.y, px, py)) {
                        w.movement.steer(z, (px - z.x) / pd, (py - z.y) / pd, z.speed * 1.3f);
                        z.blocked = false;
                        return;
                    }
                }
            }
            zombieLure(z, lead.x, lead.y, z.speed * 1.3f);
            return;
        }
        if (d > 8) w.movement.steer(z, gx / d, gy / d, Math.min(z.speed * (d > 40 ? 1.3f : 1f), lead.want + d * 0.4f));
        else w.movement.steer(z, lead.mx, lead.my, lead.want);
    }

    /** A horde leader marks its way every twenty steps or so, for the stragglers to follow. */
    void markTrail(Entity z) {
        if (z.trail == null) z.trail = new float[World.TRAIL * 2];
        int last = ((z.trailHead - 1) % World.TRAIL + World.TRAIL) % World.TRAIL;
        if (z.trailN > 0 && World.hypot(z.trail[last * 2] - z.x, z.trail[last * 2 + 1] - z.y) < 22) return;
        z.trail[z.trailHead * 2] = z.x;
        z.trail[z.trailHead * 2 + 1] = z.y;
        z.trailHead = (z.trailHead + 1) % World.TRAIL;
        z.trailN = Math.min(World.TRAIL, z.trailN + 1);
    }

    Entity nearestHordeLeader(Entity z, float radius) {
        Entity best = null;
        float bd = radius * radius;
        int cx0 = Math.max(0, (int) ((z.x - radius) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((z.x + radius) / World.CELL));
        int cy0 = Math.max(0, (int) ((z.y - radius) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((z.y + radius) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c], end = w.cellStart[c] + w.zCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o == z || o.dead || !o.leadsHorde) continue;
                    float d = (o.x - z.x) * (o.x - z.x) + (o.y - z.y) * (o.y - z.y);
                    if (d < bd) {
                        bd = d;
                        best = o;
                    }
                }
            }
        return best;
    }

    void pickRoam(Entity z) {
        z.migrating = false;
        // ((14.5) A big horde, now and then: off across the whole city to the biggest crowd of the living.)
        float[] m = migrationTarget(z);
        if (m != null) {
            z.roamX = m[0];
            z.roamY = m[1];
            z.migrating = true;
            if (w.time - w.migrationSaid > 90) {
                w.migrationSaid = w.time;
                String where = w.city.placeName(m[0], m[1]);
                w.dispatch.say(Dispatch.WHO_INFO, null, "News: A horde of about " + Math.max(15, z.hordeSize / 5 * 5) + " is on the move across the city from "
                        + w.city.placeName(z.x, z.y) + ", heading for " + where + "!", z.x, z.y);
                w.stats.timeline("A horde on the march towards " + where, z.x, z.y);
            }
            return;
        }
        // (10.32) A horde hears a big fight a long way off, and goes to it.
        if (z.leadsHorde && w.rnd.nextFloat() < 0.6f) {
            float[] f = w.noise.loudestFight(z.x, z.y, 3200);
            if (f != null && World.hypot(f[0] - z.x, f[1] - z.y) > 300) {
                float[] p = w.city.findWalkable(f[0] + w.rnd.nextFloat() * 120 - 60, f[1] + w.rnd.nextFloat() * 120 - 60);
                if (p != null) {
                    z.roamX = p[0];
                    z.roamY = p[1];
                    if (z.hordeSize >= 8 && w.time > w.hordeHeatSaid) {
                        w.hordeHeatSaid = w.time + 60;
                        w.dispatch.say(Dispatch.WHO_INFO, null, "A horde of about " + Math.max(5, z.hordeSize / 5 * 5)
                                + " is heading for the sound of the fighting at " + w.city.placeName(p[0], p[1]) + ".", z.x, z.y);
                    }
                    return;
                }
            }
        }
        // ((11.0) Out in the country with nobody about, they make their way back towards the town, where the people
        // are, rather than wander the fields for ever.)
        int ztx = (int) (z.x / City.T), zty = (int) (z.y / City.T);
        boolean country = w.city.townX1 > w.city.townX0 && (ztx < w.city.townX0 || ztx >= w.city.townX1 || zty < w.city.townY0 || zty >= w.city.townY1);
        if (country || w.rnd.nextFloat() < 0.35f) {
            float[] t = country ? new float[]{(w.city.townX0 + w.city.townX1) / 2f * City.T, (w.city.townY0 + w.city.townY1) / 2f * City.T} : busiestNear(z.x, z.y);
            if (t != null) {
                float dx = t[0] - z.x, dy = t[1] - z.y, d = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
                float step = Math.min(d, 350 + w.rnd.nextFloat() * 300);
                float[] p = w.city.findWalkable(z.x + dx / d * step + w.rnd.nextFloat() * 80 - 40, z.y + dy / d * step + w.rnd.nextFloat() * 80 - 40);
                if (p != null && d > 60) {
                    z.roamX = p[0];
                    z.roamY = p[1];
                    return;
                }
            }
        }
        if (z.leadsHorde && w.city.settlements.size() > 1 && w.rnd.nextFloat() < 0.5f) {
            // Off down the lanes to another settlement.
            float[] here = null, there;
            float bd = Float.MAX_VALUE;
            for (float[] s : w.city.settlements) {
                float d = (s[0] - z.x) * (s[0] - z.x) + (s[1] - z.y) * (s[1] - z.y);
                if (d < bd) {
                    bd = d;
                    here = s;
                }
            }
            do there = w.city.settlements.get(w.rnd.nextInt(w.city.settlements.size()));
            while (there == here);
            float[] p = w.city.findWalkable(there[0] + w.rnd.nextFloat() * 60 - 30, there[1] + w.rnd.nextFloat() * 60 - 30);
            if (p != null) {
                z.roamX = p[0];
                z.roamY = p[1];
                if (z.hordeSize >= 8 && w.time > w.hordeLaneSaid) {
                    w.hordeLaneSaid = w.time + 45;
                    City.District d = w.city.districtOf(p[0], p[1]);
                    w.dispatch.say(Dispatch.WHO_INFO, null, "A horde of about " + Math.max(5, z.hordeSize / 5 * 5) + " is heading out along the lanes towards "
                            + (d != null ? d.name : w.city.placeName(p[0], p[1])) + ".", z.x, z.y);
                }
                return;
            }
        }
        // ((22.0) A horde with nothing to go after drifts on to the next district over, a few streets at a time, rather
        // than milling about the same blocks: over a game the dead spread across the city.)
        if (z.leadsHorde && w.rnd.nextFloat() < 0.35f) {
            City.District here = w.city.districtOf(z.x, z.y), best = null;
            float bestD = Float.MAX_VALUE;
            for (int tries = 0; tries < 8; tries++) {
                City.District d = w.city.districts.get(w.rnd.nextInt(w.city.districts.size()));
                if (d == here || !d.labelled()) continue;
                float dd = (float) World.hypot(d.cx - z.x, d.cy - z.y);
                if (dd < 600 || dd > 2500) continue;
                if (dd < bestD) {
                    bestD = dd;
                    best = d;
                }
            }
            float[] p = best == null ? null : w.city.findWalkable(best.cx + w.rnd.nextFloat() * 120 - 60, best.cy + w.rnd.nextFloat() * 120 - 60);
            if (p != null) {
                z.roamX = p[0];
                z.roamY = p[1];
                if (z.hordeSize >= 12 && w.time > w.hordeDriftSaid) {
                    w.hordeDriftSaid = w.time + 75;
                    w.dispatch.say(Dispatch.WHO_INFO, null, "A horde of about " + Math.max(10, z.hordeSize / 5 * 5) + " is drifting from "
                            + (here != null ? here.name : w.city.placeName(z.x, z.y)) + " towards " + best.name + ".", z.x, z.y);
                }
                return;
            }
        }
        float a = w.rnd.nextFloat() * World.TAU, r = 200 + w.rnd.nextFloat() * 400;
        float[] p = w.city.findWalkable(Math.max(20, Math.min(w.city.worldW() - 20, z.x + (float) Math.cos(a) * r)),
                Math.max(20, Math.min(w.city.worldH() - 20, z.y + (float) Math.sin(a) * r)));
        if (p == null) p = w.city.randomWalkable(w.rnd);
        z.roamX = p[0];
        z.roamY = p[1];
    }

    /** A horde leader shambles towards a far-off spot, or towards people it can smell. */
    void roam(Entity z) {
        markTrail(z);
        if (w.city.fieldAt(w.city.humanDist, z.x, z.y) < World.SCENT * 1.7f && w.movement.followField(z, w.city.humanDist, z.speed * 0.55f)) return;
        float ddx = z.roamX - z.x, ddy = z.roamY - z.y;
        float d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
        if (d < 30 || (z.blocked && z.path == null)) {
            z.blocked = false;
            pickRoam(z);
            return;
        }
        // ((10.32) A horde on the move goes round things, by the streets and lanes, not straight into them.)
        // ((10.59) A sleepy drift, until something wakes them: see surge.)
        // ((14.5) On a march across the city, a steadier pace.)
        float pace = z.speed * (z.hordeWait ? 0.22f : z.migrating ? 0.55f : 0.38f);
        if (z.leadsHorde && d > 150) zombieWalkTo(z, z.roamX, z.roamY, pace);
        else w.movement.steer(z, ddx / d, ddy / d, pace);
    }

    /**
     * (11.0) Roughly where the most people are within 1500 of (x, y): a sample of a few dozen of the living, the
     * nearest heavy cluster of them. Null if there's nobody.
     */
    float[] busiestNear(float x, float y) {
        int n = w.entities.size();
        if (n == 0) return null;
        float[] best = null;
        float bs = 0;
        for (int k = 0; k < 24; k++) {
            Entity o = w.entities.get(w.rnd.nextInt(n));
            if (o.dead || o.isZombie() || o.type == Entity.DOG) continue;
            float d = (float) World.hypot(o.x - x, o.y - y);
            if (d > 1500) continue;
            float score = 1 / (60 + d);
            if (score > bs) {
                bs = score;
                best = new float[]{o.x, o.y};
            }
        }
        return best;
    }

    /** A body that isn't about to get up, for a zombie to feed on. */
    World.Corpse freshCorpse(float x, float y, float radius) {
        for (int i = w.corpses.size() - 1; i >= 0; i--) {
            World.Corpse c = w.corpses.get(i);
            if (c.zombie || c.rise > 0 || c.age > 60) continue;
            if ((c.x - x) * (c.x - x) + (c.y - y) * (c.y - y) < radius * radius) return c;
        }
        return null;
    }

    /**
     * Up to a car: running after one driving past, and pressed up against one that's stopped (the clawing and the
     * damage are the vehicle's: see clawAt).
     */
    void goForCar(Entity z, Fleet.Vehicle car) {
        float ddx = car.x - z.x, ddy = car.y - z.y;
        float d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
        if (d < car.length() * 0.7f + z.radius + 2) {
            if (Math.abs(car.speed) < World.CLAW_SPEED) {
                w.movement.steer(z, 0, 0, 0);
                z.angle = (float) Math.atan2(ddy, ddx);
            } else w.movement.steer(z, ddx / d, ddy / d, z.runSpeed);
        } else w.movement.steer(z, ddx / d, ddy / d, d < 60 ? z.runSpeed : z.speed * 1.2f);
    }

    /**
     * ((26.0) The dead pressed up against a vehicle with people in it that's stopped or crawling - parked, at a light,
     * waiting at a scene, on standby with someone aboard, abandoned with people still inside - claw at it: it wears
     * down with how many are on it (brutes and giants far more), smokes, and breaks down or burns in the end. Worked
     * out here, for the vehicle, from whoever is touching it - whatever each of them is thinking. Before, only an idle
     * zombie with nothing better to do hurt one, so a car sat in a crowd of the dead for ever. Below a crawl they get
     * a window in, now and then, and drag someone out to them; not out of anything armoured. Called by Fleet a few
     * times a second for each vehicle, with the time since.
     */
    void clawAt(Fleet.Vehicle v, float dt) {
        if (Fleet.offRoad(v) || v.burnt || w.hashSize < 0 || w.zombies <= 0) return;
        if (Math.abs(v.speed) >= World.CLAW_SPEED || !v.occupied()) {
            v.breakT = 0;
            v.clawN = 0;
            return;
        }
        float reach = v.length() * 0.7f + 6;
        int cx0 = Math.max(0, (int) ((v.x - reach) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((v.x + reach) / World.CELL));
        int cy0 = Math.max(0, (int) ((v.y - reach) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((v.y + reach) / World.CELL));
        float weight = 0, bd = Float.MAX_VALUE;
        int n = 0;
        Entity first = null;
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c], end = w.cellStart[c] + w.zCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o.dead || o.hp <= 0 || !o.isZombie() || o.stun > 0 || o.vaultT > 0 || o.charge > 0) continue;
                    float ddx = o.x - v.x, ddy = o.y - v.y, d2 = ddx * ddx + ddy * ddy;
                    if (d2 > reach * reach) continue;
                    n++;
                    weight += o.type == Entity.GIANT ? 11 : o.type == Entity.BRUTE ? 2.5f : o.type == Entity.BRUISER ? 1.7f
                            : o.type == Entity.ZOMBIE_DOG ? 0.6f : 1;
                    if (d2 < bd) {
                        bd = d2;
                        first = o;
                    }
                }
            }
        v.clawN = n;
        if (n == 0) {
            v.breakT = Math.max(0, v.breakT - dt);
            return;
        }
        // (Only so many get their hands on it at once.)
        weight = Math.min(weight, 16);
        // Armour takes a lot of clawing (and the APC and riot van more again: see Fleet.damage); a car much less.
        w.fleet.damage(v, 4 * weight * (v.type == Fleet.TANK ? 0.15f : 1) * dt, false);
        if (w.rnd.nextFloat() < dt * (1 + n * 0.3f)) w.emit(w.rnd.nextFloat() < 0.5f ? Sfx.THUD : Sfx.POUND, v.x, v.y);
        if (n >= 4 && w.time - w.clawSaid > 90 && v.occupied() && !v.broken) {
            w.clawSaid = w.time;
            w.dispatch.say(Dispatch.WHO_911, null, "\"I'm stuck in my car on " + w.city.placeName(v.x, v.y) + " and they're all over it! "
                    + "They're beating on the glass!\"", v.x, v.y);
        }
        if (v.broken || Math.abs(v.speed) >= World.BREAK_SPEED || v.player != null || Fleet.armoured(v)) {
            v.breakT = 0;
            return;
        }
        v.breakT += dt * Math.min(4, weight);
        if (v.breakT < World.BREAK_IN || first == null) return;
        // The glass goes, and hands reach in.
        v.breakT = World.BREAK_IN * 0.35f;
        float ax = first.x - v.x, ay = first.y - v.y, al = (float) Math.sqrt(ax * ax + ay * ay) + 0.001f;
        float px = v.x + ax / al * 10, py = v.y + ay / al * 10;
        w.emit(Sfx.GLASS, v.x, v.y);
        for (int k = 0; k < 6; k++)
            w.effects.decal(px + w.rnd.nextFloat() * 8 - 4, py + w.rnd.nextFloat() * 8 - 4, 0.6f + w.rnd.nextFloat(), 0xCCCFE8F2, World.D_GLASS, w.rnd.nextFloat() * World.TAU);
        for (int k = 0; k < 6; k++)
            w.effects.particle(px, py, w.rnd.nextFloat() * 50 - 25, w.rnd.nextFloat() * 50 - 25, 0.4f, 0.7f, 0xFFCFE8F2, World.P_DOT);
        Entity e = w.fleet.dragOut(v, px, py);
        if (e == null) return;
        w.dragged++;
        e.stun = Math.max(e.stun, 0.6f);
        e.fleeTimer = 3;
        e.threatX = first.x;
        e.threatY = first.y;
        first.memory = w.trait[World.TR_RELENTLESS] ? 20 : 10;
        first.lastX = e.x;
        first.lastY = e.y;
        first.biteCd = 0;
        float bx = e.x - first.x, by = e.y - first.y, bl = (float) Math.sqrt(bx * bx + by * by) + 0.001f;
        bite(first, e, bx / bl, by / bl);
        w.stats.highlight("Dragged from the car!", e.x, e.y);
        if (w.time - w.dragSaid > 45) {
            w.dragSaid = w.time;
            w.dispatch.say(Dispatch.WHO_911, null, "\"They smashed the window and pulled someone out of a car on " + w.city.placeName(v.x, v.y)
                    + "! Oh God, they're on top of them!\"", v.x, v.y);
        }
    }

    Fleet.Vehicle carFor(Entity z, float radius) {
        // (10.49) Looked up on a grid of the cars worth chasing, made once a moment, instead of going through
        // every vehicle on the map for each of the dead.
        if (w.carGridTime != w.time) {
            w.carGridTime = w.time;
            int gw = w.city.w * City.T / World.CAR_CELL + 1, gh = w.city.h * City.T / World.CAR_CELL + 1;
            if (w.carHead == null || w.carHead.length != gw * gh) w.carHead = new int[gw * gh];
            java.util.Arrays.fill(w.carHead, -1);
            int n = w.fleet.vehicles.size();
            if (w.carNext.length < n) {
                w.carNext = new int[n * 2];
                w.carList = new Fleet.Vehicle[n * 2];
            }
            w.carCount = 0;
            for (int i = 0; i < n; i++) {
                Fleet.Vehicle v = w.fleet.vehicles.get(i);
                if (Fleet.offRoad(v) || !v.occupied()) continue;
                int cx = Math.max(0, Math.min(gw - 1, (int) (v.x / World.CAR_CELL))), cy = Math.max(0, Math.min(gh - 1, (int) (v.y / World.CAR_CELL)));
                w.carList[w.carCount] = v;
                w.carNext[w.carCount] = w.carHead[cy * gw + cx];
                w.carHead[cy * gw + cx] = w.carCount++;
            }
        }
        Fleet.Vehicle best = null;
        float bd = radius * radius;
        int gw = w.city.w * City.T / World.CAR_CELL + 1, gh = w.city.h * City.T / World.CAR_CELL + 1;
        int x0 = Math.max(0, (int) ((z.x - radius) / World.CAR_CELL)), x1 = Math.min(gw - 1, (int) ((z.x + radius) / World.CAR_CELL));
        int y0 = Math.max(0, (int) ((z.y - radius) / World.CAR_CELL)), y1 = Math.min(gh - 1, (int) ((z.y + radius) / World.CAR_CELL));
        for (int cy = y0; cy <= y1; cy++)
            for (int cx = x0; cx <= x1; cx++)
                for (int i = w.carHead[cy * gw + cx]; i >= 0; i = w.carNext[i]) {
                    Fleet.Vehicle v = w.carList[i];
                    float d = (v.x - z.x) * (v.x - z.x) + (v.y - z.y) * (v.y - z.y);
                    if (d < bd) {
                        bd = d;
                        best = v;
                    }
                }
        // (Not one on the far side of the barrier, or a fence: it would only press against it.)
        if (best != null && !w.city.passLine(z.x, z.y, best.x, best.y)) return null;
        return best;
    }

    void bite(Entity z, Entity t, float nx, float ny) {
        // ((17.1) Not through a wall.)
        if (World.WALK_IN && w.city.insideOf(z.x, z.y) != w.city.insideOf(t.x, t.y) && !w.city.los(z.x, z.y, t.x, t.y)) return;
        if (z.type == Entity.GIANT) {
            giantSwipe(z, t, nx, ny);
            return;
        }
        // Body armour stops a lot of bites, from any side.
        if (t.type == Entity.COP && t.role == Entity.ROLE_SWAT && w.rnd.nextFloat() < 0.45f) {
            z.biteCd = 0.8f;
            w.emit(Sfx.THUD, t.x, t.y);
            return;
        }
        // A riot shield turns away most bites from the front.
        if (t.type == Entity.COP && t.role == Entity.ROLE_RIOT) {
            float face = (float) Math.cos(t.angle) * -nx + (float) Math.sin(t.angle) * -ny;
            if (face > 0.35f && w.rnd.nextFloat() < 0.85f) {
                z.biteCd = 0.8f;
                z.stun = Math.max(z.stun, z.big() ? 0.2f : 0.5f);
                w.movement.tryMove(z, -nx * 5 / z.mass, -ny * 5 / z.mass);
                w.emit(Sfx.SHIELD, t.x, t.y);
                return;
            }
        }
        float dmg = z.type == Entity.BRUTE ? 20 : z.type == Entity.BRUISER ? 14 : z.type == Entity.RUNNER ? 8
                : z.type == Entity.ZOMBIE_DOG ? 7 : 17;
        z.biteCd = z.type == Entity.RUNNER || z.type == Entity.ZOMBIE_DOG ? 0.55f : 0.9f;
        z.biteAt = w.time;
        t.hp -= dmg;
        t.hurt = 1;
        t.killedByZombie = true;
        t.fleeTimer = 2f;
        t.threatX = z.x;
        t.threatY = z.y;
        w.effects.bloodBurst(t.x, t.y, 6, nx, ny);
        w.emit(Sfx.BITE, t.x, t.y);
        w.bites++;
        if (!w.bitBy.containsKey(t.nameSeed) && w.bitBy.size() < 20000) w.bitBy.put(t.nameSeed, z.nameSeed);
        float chance = (w.trait[World.TR_VIRULENT] ? 0.65f : w.trait[World.TR_WEAK_BITE] ? 0.22f : 0.4f) * World.BITE_INFECT * Director.INFECT[w.difficulty];
        if (!t.infected && t.type != Entity.DOG && w.rnd.nextFloat() < chance) {
            t.infected = true;
            t.infectTimer = w.war.turnTime(8 + w.rnd.nextFloat() * 10);
        }
        if (z.type == Entity.BRUTE) w.movement.tryMove(t, nx * 4, ny * 4);
        else if (z.type == Entity.BRUISER) w.movement.tryMove(t, nx * 2, ny * 2);
    }

    /** Shoves a zombie back and stuns it; armed units also hit it with their weapon. */
    void shove(Entity e, Entity z, boolean strong) {
        e.meleeCd = strong ? 1.2f : 2.2f;
        e.swingAt = w.time;
        float ddx = z.x - e.x, ddy = z.y - e.y;
        float d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
        // (10.45) Not a sure thing: a shove can glance off or be shrugged off, the more so from the untrained, against
        // a big one, or with more of them pressing in. (Only the one you're controlling shoves when you say.)
        // ((25.0) Nobody shoves a giant.)
        if (z.type == Entity.GIANT) {
            e.angle = (float) Math.atan2(ddy, ddx);
            w.emit(Sfx.THUD, z.x, z.y);
            return;
        }
        if (e != w.controlled) {
            float chance = strong ? 0.72f : 0.5f;
            if (z.type == Entity.BRUTE) chance -= 0.35f;
            else if (z.type == Entity.BRUISER) chance -= 0.18f;
            if (z.stun > 0) chance += 0.15f;
            chance -= 0.08f * Math.max(0, w.spatial.countZombiesNear(e.x, e.y, 22) - 1);
            if (e.hp < e.maxHp * 0.4f) chance -= 0.1f;
            if (w.rnd.nextFloat() > Math.max(0.12f, chance)) {
                w.movement.tryMove(z, ddx / d * 1.5f / z.mass, ddy / d * 1.5f / z.mass);
                e.angle = (float) Math.atan2(ddy, ddx);
                w.emit(Sfx.THUD, z.x, z.y);
                return;
            }
        }
        float push = (strong ? 11 : 7) / z.mass;
        w.movement.tryMove(z, ddx / d * push, ddy / d * push);
        z.stun = z.type == Entity.BRUTE ? 0.3f : z.type == Entity.BRUISER ? 0.5f : strong ? 0.9f : 0.6f;
        if (strong) {
            z.hp -= 8;
            z.hurt = 1;
        }
        e.angle = (float) Math.atan2(ddy, ddx);
        // ((13.4) A blow with a rifle butt or a baton has a sound of its own.)
        w.emit(strong ? Sfx.MELEE : Sfx.THUD, z.x, z.y);
    }

    /** ((25.0) A giant on the move, every step: barricades, wire and cars in its way give. */
    void giantStep(Entity z, float dt) {
        // (Hardly staggered by anything.)
        if (z.stun > 0.3f) z.stun = 0.3f;
        if (!w.barriers.isEmpty()) bashBarrier(z, dt);
        float reach = z.radius * 2.2f;
        for (int i = 0, n = w.fleet.vehicles.size(); i < n; i++) {
            Fleet.Vehicle v = w.fleet.vehicles.get(i);
            if (Fleet.offRoad(v)) continue;
            float dx = v.x - z.x, dy = v.y - z.y, r = reach + v.length() * 0.45f;
            if (Math.abs(dx) > r || Math.abs(dy) > r) continue;
            float d = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
            if (d > r) continue;
            // Shoved aside and turned about, crumpled as it goes: a car rolls over, a tank barely moves.
            float heavy = v.type == Fleet.TANK ? 0.15f : v.type == Fleet.TRUCK ? 0.5f : 1;
            float push = Math.min(4, (r - d) * 0.4f + 14 * dt) * heavy;
            float nx = v.x + dx / d * push, ny = v.y + dy / d * push;
            if (!w.city.solidAt(nx, ny)) {
                v.x = nx;
                v.y = ny;
            }
            v.speed *= 0.5f;
            v.angle += ((z.nameSeed & 1) == 0 ? 1 : -1) * dt * 2.2f * heavy;
            w.fleet.damage(v, (v.type == Fleet.TANK ? 8 : 55) * dt, false);
            if (w.rnd.nextFloat() < dt * 2) w.emit(Sfx.CRASH, v.x, v.y);
            if (w.rnd.nextFloat() < dt * 8)
                w.effects.particle(v.x, v.y, w.rnd.nextFloat() * 50 - 25, w.rnd.nextFloat() * 50 - 25, 0.4f, 0.7f, 0xFFCFE8F2, World.P_DOT);
        }
    }

    /** ((25.0) A giant's swipe: a heavy blow that throws whoever it hits back (no bite: nobody turns into one of these). */
    void giantSwipe(Entity z, Entity t, float nx, float ny) {
        z.biteCd = 1.7f;
        z.biteAt = w.time;
        boolean shield = t.type == Entity.COP && t.role == Entity.ROLE_RIOT
                && (float) Math.cos(t.angle) * -nx + (float) Math.sin(t.angle) * -ny > 0.35f;
        t.hp -= shield ? 12 : 32;
        t.hurt = 1;
        t.killedByZombie = true;
        t.fleeTimer = 3;
        t.threatX = z.x;
        t.threatY = z.y;
        float throwBy = (shield ? 5 : 10) / t.mass;
        for (int k = 0; k < 3; k++) w.movement.tryMove(t, nx * throwBy, ny * throwBy);
        t.knockX = nx * 200 / t.mass;
        t.knockY = ny * 200 / t.mass;
        t.stun = Math.max(t.stun, shield ? 0.4f : 0.9f);
        w.effects.bloodBurst(t.x, t.y, 5, nx, ny);
        w.emit(shield ? Sfx.SHIELD : Sfx.MELEE, t.x, t.y);
        if (w.rnd.nextFloat() < 0.4f) w.emit(Sfx.GROAN_DEEP, z.x, z.y);
    }

    /** ((25.0) A giant brought down. */
    void giantDown(Entity z) {
        w.giantsKilled++;
        w.dispatch.say(Dispatch.WHO_POLICE, null, "All units: the giant on " + w.city.placeName(z.x, z.y) + " is down. Repeat, it's down.", z.x, z.y, true);
        w.stats.timeline("A giant was brought down", z.x, z.y);
        w.noise.noise(z.x, z.y, 200);
    }

    void bashBarrier(Entity z, float dt) {
        for (int i = 0; i < w.barriers.size(); i++) {
            float[] b = w.barriers.get(i);
            float ddx = b[0] - z.x, ddy = b[1] - z.y;
            float reach = z.type == Entity.GIANT ? 15 + World.GIANT_REACH : 15;
            if (ddx * ddx + ddy * ddy > reach * reach) continue;
            // ((26.0) A crowd heaving at it together brings it down faster than each of them on its own would.)
            float pile = z.type == Entity.GIANT ? 1 : 1 + (World.pileBonus(w.spatial.countZombiesNear(b[0], b[1], 24)) - 1) * 0.5f;
            b[2] -= (z.type == Entity.GIANT ? World.GIANT_SMASH : z.type == Entity.BRUTE ? 30 : 6) * pile * dt;
            if (w.rnd.nextFloat() < dt * 1.5f) w.emit(Sfx.THUD, b[0], b[1]);
            if (b[2] <= 0) {
                w.sandbox.removeBarricade(b);
                for (int k = 0; k < 8; k++)
                    w.effects.particle(b[0], b[1], w.rnd.nextFloat() * 40 - 20, w.rnd.nextFloat() * 40 - 20, 0.6f, 1.2f, 0xFF7A5634, World.P_DEBRIS);
            }
            return;
        }
    }

    /** The shared route patch to (x, y) (spots within a few steps of each other share one), or null. */
    World.Lure lureField(float x, float y) {
        int tx = Math.max(0, Math.min(w.city.w - 1, (int) (x / City.T))), ty = Math.max(0, Math.min(w.city.h - 1, (int) (y / City.T)));
        int key = (ty / 3) * 65536 + tx / 3;
        World.Lure f = w.lures.get(key);
        if (f != null && w.time - f.made < 20) return f;
        if (w.pathBudget < 0.1f) return f;
        w.pathBudget -= 0.1f;
        if (w.city.solid[ty * w.city.w + tx]) {
            float[] p = w.city.findWalkable(x, y);
            if (p == null) return f;
            tx = (int) (p[0] / City.T);
            ty = (int) (p[1] / City.T);
        }
        if (f == null) f = new World.Lure();
        f.made = w.time;
        f.x0 = tx - World.LURE_R;
        f.y0 = ty - World.LURE_R;
        java.util.Arrays.fill(f.d, City.FAR);
        if (w.lureBuckets == null) w.lureBuckets = new int[9][World.LURE_S * World.LURE_S * 4];
        if (w.city.walkCost == null) w.city.computeWalkCost();
        int[] sizes = new int[9];
        int start = World.LURE_R * World.LURE_S + World.LURE_R;
        f.d[start] = 0;
        sizes[0] = 1;
        w.lureBuckets[0][0] = start;
        int pending = 1;
        for (int d = 0; pending > 0 && d < 900; d++) {
            int bi = d % 9;
            int[] bk = w.lureBuckets[bi];
            int size = sizes[bi];
            sizes[bi] = 0;
            pending -= size;
            for (int k = 0; k < size; k++) {
                int t = bk[k];
                if (f.d[t] != d) continue;
                int lx = t % World.LURE_S, ly = t / World.LURE_S;
                for (int q = 0; q < 4; q++) {
                    int nx = lx + (q == 0 ? -1 : q == 1 ? 1 : 0), ny = ly + (q == 2 ? -1 : q == 3 ? 1 : 0);
                    if (nx < 0 || ny < 0 || nx >= World.LURE_S || ny >= World.LURE_S) continue;
                    int wx = f.x0 + nx, wy = f.y0 + ny;
                    if (wx < 0 || wy < 0 || wx >= w.city.w || wy >= w.city.h) continue;
                    int wi = wy * w.city.w + wx;
                    if (w.city.solid[wi]) continue;
                    int nl = ny * World.LURE_S + nx, nd = d + w.city.walkCost[wi];
                    if (nd >= f.d[nl]) continue;
                    f.d[nl] = nd;
                    int b2 = nd % 9;
                    if (sizes[b2] >= w.lureBuckets[b2].length) continue;
                    w.lureBuckets[b2][sizes[b2]++] = nl;
                    pending++;
                }
            }
        }
        w.lures.put(key, f);
        w.luresMade++;
        return f;
    }

    /** One of the dead heading for a spot by the shared route there; by a route of its own when off it. */
    void zombieLure(Entity z, float x, float y, float speed) {
        float dx = x - z.x, dy = y - z.y;
        // (Close and in plain sight: straight there.)
        if (dx * dx + dy * dy < 40 * 40 && !z.blocked) {
            float d = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
            w.movement.steer(z, dx / d, dy / d, speed);
            return;
        }
        if (Math.abs(dx) < (World.LURE_R - 2) * City.T && Math.abs(dy) < (World.LURE_R - 2) * City.T) {
            World.Lure f = lureField(x, y);
            if (f != null && w.movement.followPatch(z, f.x0, f.y0, World.LURE_S, f.d, speed, true)) {
                z.blocked = false;
                z.path = null;
                return;
            }
        }
        zombieWalkTo(z, x, y, speed);
    }

    /**
     * (14.5) How far one of the dead has rotted, 0 to ROT_MAX: nothing for the first eight minutes after it rose, then
     * slowly more over the next twenty. That much slower on its feet, and that much more to each hit - a city that holds
     * out long enough gains the upper hand.
     */
    float rot(Entity z) {
        if (!z.isUndead()) return 0;
        float age = w.time - z.risen - World.ROT_START;
        return age <= 0 ? 0 : Math.min(1, age / World.ROT_SPAN) * World.ROT_MAX;
    }

    /**
     * (14.5) A big horde (fifteen or more) now and then sets off across the whole city for the biggest crowd of the
     * living - a safe zone full of people, as often as not. Announced on the news. The target, or null.
     */
    float[] migrationTarget(Entity z) {
        if (!z.leadsHorde || z.hordeSize < 15 || w.rnd.nextFloat() > 0.25f) return null;
        float bx = 0, by = 0, best = 0;
        for (int i = 0; i < w.dispatch.zones.size(); i++) {
            Dispatch.SafeZone s = w.dispatch.zones.get(i);
            if (s.removed || !s.open) continue;
            float d = (float) World.hypot(s.x - z.x, s.y - z.y);
            if (d > 1500 && s.sheltered > best) {
                best = s.sheltered;
                bx = s.x;
                by = s.y;
            }
        }
        // (Or the thickest crowd of people out on the streets, from a sample of them.)
        for (int k = 0, n = w.entities.size(); k < 40 && n > 0; k++) {
            Entity e = w.entities.get(w.rnd.nextInt(n));
            if (e.dead || e.isZombie() || World.hypot(e.x - z.x, e.y - z.y) < 1500) continue;
            int c = w.spatial.gatherLiving(e.x, e.y, 150);
            if (c > best) {
                best = c;
                bx = e.x;
                by = e.y;
            }
        }
        if (best < 12) return null;
        return w.city.findWalkable(bx, by);
    }

    /**
     * One of the dead heading for a spot (a noise, where it last saw someone): straight there when nothing is
     * in the way, otherwise round the buildings by a planned route instead of grinding into a wall.
     */
    void zombieWalkTo(Entity z, float x, float y, float speed) {
        float dx = x - z.x, dy = y - z.y, d = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
        boolean clear = !z.blocked && z.stuckTime < 0.5f && (d < 40 || w.city.los(z.x, z.y, x, y));
        if (clear && z.path == null) {
            w.movement.steer(z, dx / d, dy / d, speed);
            return;
        }
        if (z.path == null || Math.abs(z.planX - x) > 24 || Math.abs(z.planY - y) > 24) {
            if (w.pathBudget < 0.25f) {
                w.movement.steer(z, dx / d, dy / d, speed);
                return;
            }
            w.pathBudget -= 0.25f;
            float[] goal = w.city.solidAt(x, y) ? w.city.findWalkable(x, y) : new float[]{x, y};
            z.path = goal == null ? null : w.city.findPath(z.x, z.y, goal[0], goal[1], 6000);
            if (z.path == null) z.path = new int[0];
            z.pathIdx = 0;
            z.pathDest = null;
            z.planX = x;
            z.planY = y;
        }
        z.blocked = false;
        if (!w.movement.followPath(z, speed)) w.movement.steer(z, dx / d, dy / d, speed);
    }

    /**
     * Somewhere that means going back past the one they've just run from: right by where it was seen, or on
     * the far side of it, while that's still fresh in their mind.
     */
    boolean backTowardsIt(Entity e, float x, float y) {
        if (e.fear <= 0) return false;
        float tx = e.threatX - e.x, ty = e.threatY - e.y, td = (float) Math.sqrt(tx * tx + ty * ty) + 0.001f;
        float gx = x - e.x, gy = y - e.y, gd = (float) Math.sqrt(gx * gx + gy * gy) + 0.001f;
        if ((x - e.threatX) * (x - e.threatX) + (y - e.threatY) * (y - e.threatY) < 90 * 90) return true;
        // On ahead in its direction, and further than it is.
        return gd > td * 0.8f && (tx * gx + ty * gy) / (td * gd) > 0.6f;
    }

    /**
     * One of the dead at the wall (or a shut gate) of a safe zone with people in it: claws at it, a little
     * at a time, until the section gives. True while it's doing that.
     */
    boolean clawWall(Entity z, float dt) {
        int tx = (int) (z.x / City.T), ty = (int) (z.y / City.T);
        Dispatch.SafeZone zone = null;
        int seg = 0, tile = -1;
        float bd = Float.MAX_VALUE;
        for (int oy = -1; oy <= 1; oy++)
            for (int ox = -1; ox <= 1; ox++) {
                int x = tx + ox, y = ty + oy;
                if (x < 0 || y < 0 || x >= w.city.w || y >= w.city.h) continue;
                int t = y * w.city.w + x;
                Dispatch.SafeZone o = w.dispatch.wallOwner[t];
                if (o == null || o.removed || !w.city.solid[t]) continue;
                float cx = (x + 0.5f) * City.T - z.x, cy = (y + 0.5f) * City.T - z.y, d = cx * cx + cy * cy;
                if (d < bd) {
                    bd = d;
                    zone = o;
                    seg = w.dispatch.wallSeg[t];
                    tile = t;
                }
            }
        if (zone == null || zone.inside(z.x, z.y) || (zone.sheltered == 0 && zone.onSite == 0)) return false;
        float cx = (tile % w.city.w + 0.5f) * City.T - z.x, cy = (tile / w.city.w + 0.5f) * City.T - z.y;
        float d = (float) Math.sqrt(cx * cx + cy * cy) + 0.001f;
        if (d > City.T * 0.5f + z.radius + 3) w.movement.steer(z, cx / d, cy / d, z.speed * 0.6f);
        else w.movement.steer(z, 0, 0, 0);
        z.angle = World.turn(z.angle, (float) Math.atan2(cy, cx), dt * 4);
        // ((13.4) Now and then one gets a grip and climbs over, slowly, instead of clawing the wall down.)
        // ((26.0) Far more often, and quicker, with a crowd of them heaped up against the same stretch.)
        int pile = w.spatial.countZombiesNear(z.x, z.y, World.PILE_REACH);
        float bonus = World.pileBonus(pile);
        if (seg >= 0 && z.type != Entity.GIANT && d < City.T * 0.5f + z.radius + 6 && w.rnd.nextFloat() < dt * 0.06f * bonus * bonus) {
            z.mx = cx;
            z.my = cy;
            if (w.movement.tryVault(z, d + 3, true)) {
                float t = z.type == Entity.BRUTE ? World.CLIMB_BRUTE : z.type == Entity.RUNNER ? World.CLIMB_RUNNER : World.CLIMB_TIME;
                z.vaultDur = z.vaultT = t * 1.3f / (float) Math.sqrt(bonus);
                w.emit(Sfx.CLIMB, z.x, z.y);
                if (pile >= World.PILE_SAY) w.movement.pileUp(z.x, z.y, pile);
                return true;
            }
        }
        w.dispatch.damageWall(zone, seg, (z.type == Entity.GIANT ? 45 : z.type == Entity.BRUTE ? 12 : z.type == Entity.BRUISER ? 7 : 4) * (z.type == Entity.GIANT ? 1 : bonus) * dt);
        if (w.rnd.nextFloat() < dt * 0.8f) w.emit(Sfx.THUD, z.x, z.y);
        return true;
    }
}
