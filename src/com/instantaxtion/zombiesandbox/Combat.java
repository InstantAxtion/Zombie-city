package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Fighting: picking a target, aiming and firing every kind of weapon, shells, grenades and explosions, and what they do to buildings.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class Combat {
    private final World w;

    Combat(World w) {
        this.w = w;
    }

    /** Explosions waiting to go off. */
    void updateBlasts(float dt) {
        for (int i = w.pendingBlasts.size() - 1; i >= 0; i--) {
            float[] b = w.pendingBlasts.get(i);
            b[4] -= dt;
            if (b[4] <= 0) {
                w.pendingBlasts.remove(i);
                explode(b[0], b[1], b[2], b[3]);
            }
        }
    }

    /** An explosion that goes off after a delay (a car's fuel tank, a gas pump). */
    void blastLater(float x, float y, float radius, float damage, float delay) {
        w.pendingBlasts.add(new float[]{x, y, radius, damage, delay});
    }

    /** Sandbags or a wall right round someone: they're dug in. */
    /**
     * ((24.0) How long a reload takes, the one rule everywhere: a machine gun's belt 4 s, a rifle 2.2, a pistol or
     * shotgun 1.6 (quicker for the police units trained for it). It was set four ways in four places.)
     */
    float reloadTime(Entity e) {
        if (e.type == Entity.SOLDIER) return e.role == Entity.ROLE_GUNNER ? 4f : 2.2f;
        float[] dc = World.doctrine(e);
        return 1.6f * (dc != null ? dc[3] : 1);
    }

    /** ((24.0) The reload counting down (faster with the Quick perk), and the magazine filled from the spare rounds at the end. */
    void reloadTick(Entity e, float dt) {
        if (e.reload <= 0) return;
        e.reload -= dt * (World.perk(e, World.P_QUICK) ? 1.35f : 1);
        if (e.reload <= 0) {
            int take = Math.max(0, Math.min(e.magSize - e.ammo, e.reserve));
            e.ammo += take;
            e.reserve -= take;
        }
    }

    /** The barrage's eight shells, the first after delay + 2.2 seconds. */
    void shells(float x, float y, float delay) {
        for (int k = 0; k < 8; k++) {
            double a = w.rnd.nextFloat() * Math.PI * 2;
            float r = (float) Math.sqrt(w.rnd.nextFloat()) * 62;
            blastLater(x + (float) Math.cos(a) * r, y + (float) Math.sin(a) * r, 46, 210, delay + 2.2f + k * 0.45f + w.rnd.nextFloat() * 0.2f);
        }
    }

    /** (11.4) Issues an officer or soldier their own gun, full and with the spare rounds that go with it. */
    void issue(Entity e, int kind) {
        e.weapon = kind;
        e.magSize = World.magFor(kind);
        e.ammo = e.magSize;
        int spare = World.spareFor(kind);
        if (spare >= 0) e.reserve = spare;
    }

    /** Puts a particular gun in someone's hands. */
    void setGun(Entity e, int kind, int rounds) {
        if (!e.isArmed()) e.hasGun = true;
        e.weapon = kind;
        e.magSize = World.magFor(kind);
        e.ammo = Math.min(e.magSize, rounds);
        e.reserve = Math.max(0, rounds - e.ammo);
        e.reload = 0;
    }

    /** A bat or an axe: a hard blow that stuns, and sometimes splits a skull. */
    void swing(Entity e, Entity z) {
        // (A machete cuts like an axe; a crowbar hits like a bat, only harder.)
        boolean axe = e.melee == Entity.M_AXE || e.melee == Entity.M_MACHETE;
        if (e.melee == Entity.M_CROWBAR) z.hp -= 8;
        e.meleeCd = axe ? 0.8f : 0.6f;
        e.swingAt = w.time;
        float ddx = z.x - e.x, ddy = z.y - e.y, d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
        e.angle = (float) Math.atan2(ddy, ddx);
        boolean wasUp = z.hp > 0;
        // ((14.8) A civilian wading in with something they've just picked up lands weaker blows, further apart.)
        boolean wild = w.wildSwing;
        if (wild) e.meleeCd += 0.6f;
        if (z.isUndead() && !z.big() && w.rnd.nextFloat() < (wild ? 0.03f : axe ? 0.25f : 0.1f)) z.hp = 0;
        else z.hp -= (axe ? 36 : 22) * (wild ? 0.55f : 1);
        z.hurt = 1;
        z.stun = Math.max(z.stun, z.big() ? 0.2f : axe ? 0.4f : 0.8f);
        w.movement.tryMove(z, ddx / d * (axe ? 5 : 9) / z.mass, ddy / d * (axe ? 5 : 9) / z.mass);
        w.effects.bloodBurst(z.x, z.y, 3, ddx / d, ddy / d);
        w.emit(Sfx.MELEE, z.x, z.y);
        if (wasUp && z.hp <= 0 && z.isZombie()) w.experience.killMade(e, z);
    }

    /** Shotgun pellets catch whoever is standing next to the target too. */
    void pellets(Entity e, Entity t, float dmg) {
        int cx0 = Math.max(0, (int) ((t.x - 16) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((t.x + 16) / World.CELL));
        int cy0 = Math.max(0, (int) ((t.y - 16) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((t.y + 16) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c], end = w.cellStart[c] + w.zCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o == t || o.dead || o.hp <= 0) continue;
                    if ((o.x - t.x) * (o.x - t.x) + (o.y - t.y) * (o.y - t.y) > 16 * 16) continue;
                    o.hp -= dmg * 0.4f * World.GUN_DAMAGE;
                    o.hurt = 1;
                    if (o.hp <= 0) {
                        o.bodyShot = true;
                        w.experience.killMade(e, o);
                    }
                }
            }
    }

    void dropMelee(Entity e, int kind) {
        World.Pickup p = new World.Pickup();
        p.x = e.x + w.rnd.nextFloat() * 6 - 3;
        p.y = e.y + w.rnd.nextFloat() * 6 - 3;
        p.melee = kind;
        p.lockUntil = w.time + 2;
        w.pickups.add(p);
    }

    /** Aims and shoots. Returns false if the shot is blocked by someone standing in the way. */
    boolean aimAndFire(Entity e, Entity t, float d, float range, float dt) {
        e.aiming = true;
        // ((17.6) Trading shots with someone who can shoot back: cover counts.)
        if (t.isArmed() || t.hasGun) {
            e.coverFromX = t.x;
            e.coverFromY = t.y;
            e.lastAimT = w.time;
        }
        e.angle = World.turn(e.angle, (float) Math.atan2(t.y - e.y, t.x - e.x), dt * 14);
        if (e.ammo <= 0) {
            if (e.reload <= 0 && e.reserve > 0) {
                e.reload = reloadTime(e);
                w.emit(Sfx.RELOAD, e.x, e.y);
            } else if (e.reload <= 0 && e.reserve <= 0 && w.rnd.nextFloat() < dt * 1.5f) {
                // ((13.4) Nothing left: the click of a trigger on an empty gun.)
                w.emit(Sfx.DRY_FIRE, e.x, e.y);
            }
            return true;
        }
        // ((12.8) Only once the gun is on it: a moment to bring it round, not a shot off to the side.)
        float off = Math.abs(World.angleDiff(e.angle, (float) Math.atan2(t.y - e.y, t.x - e.x)));
        if (off > 0.45f) return true;
        // ((24.0) No shots through walls: a target picked without a look (the walk back to resupply, the break for the
        // gap) behind a building was shot dead all the same.)
        if (e.reload <= 0 && e.cooldown <= 0 && d > 18 && !w.city.los(e.x, e.y, t.x, t.y)) return false;
        // ((12.8) As many shots as the gun fires in the time that's gone by, with what's left over carried to the
        // next: before, the time after each shot started again from nothing every step, so a quick-firing gun lost a
        // quarter of its rate at 30 frames a second, and at the faster game speeds most of it.)
        for (int shots = 0; e.reload <= 0 && e.cooldown <= 0 && e.ammo > 0 && shots < 6 && t.hp > 0; shots++) {
            if (friendInLine(e, t, d)) {
                e.cooldown = 0.15f;
                return false;
            }
            if (holdFire(e, t, d, range)) {
                e.cooldown = 0.25f;
                return true;
            }
            float carry = Math.max(e.cooldown, -dt);
            fire(e, t, d, range);
            e.cooldown += carry;
        }
        return true;
    }

    /**
     * The zombie most worth shooting: close ones first, but ones attacking someone, runners and screamers
     * jump the queue, and crawlers wait. Only targets in sight count.
     */
    Entity pickTarget(Entity e, float range) {
        float best = Float.MAX_VALUE, blockedBest = Float.MAX_VALUE;
        Entity res = null, blocked = null;
        // ((27.4) The others fighting alongside: what threatens any of them counts, and one of them reloading with the
        // dead coming gets covered first.)
        int nm = e.raider ? 0 : mates(e, MATE_R);
        int cx0 = Math.max(0, (int) ((e.x - range) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((e.x + range) / World.CELL));
        int cy0 = Math.max(0, (int) ((e.y - range) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((e.y + range) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c], end = w.cellStart[c] + w.zCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o.dead || o.hp <= 0 || !o.isZombie()) continue;
                    float ddx = o.x - e.x, ddy = o.y - e.y, d2 = ddx * ddx + ddy * ddy;
                    if (d2 > range * range) continue;
                    float score = (float) Math.sqrt(d2);
                    // The same senses as the dead: they see a long way in front, but only notice what's close behind
                    // them (unless they're already on alert).
                    // ((14.3) Unless a drone overhead has it marked: then they know where it is.)
                    if (e.fear <= 0 && score > 60 && o.spottedUntil <= w.time) {
                        float facing = ((float) Math.cos(e.angle) * ddx + (float) Math.sin(e.angle) * ddy) / score;
                        if (facing < -0.2f) continue;
                    }
                    if (o.biteCd > 0) score -= 60;
                    if (nm > 0) score = groupScore(e, o, score, nm);
                    // ((21.0) The alpha first, for anyone with a scope; the rest of them know to go for it too.)
                    if (o.alpha) score -= e.role == Entity.ROLE_SNIPER || e.role == Entity.ROLE_MARKSMAN || e.weapon == Entity.W_DMR ? 200 : 45;
                    if (o.type == Entity.RUNNER) score -= 25;
                    // ((25.0) A giant: everyone's priority, the scopes and launchers most of all.)
                    if (o.type == Entity.GIANT) score -= e.role == Entity.ROLE_SNIPER || e.role == Entity.ROLE_MARKSMAN || e.role == Entity.ROLE_GRENADIER
                            || e.weapon == Entity.W_DMR ? 180 : 50;
                    // ((21.0) Fire discipline: one that others are already shooting at counts as further off, so a line
                    // of guns works along the front of a crowd instead of all killing the same one twice over. Not the
                    // big ones, which take all the fire they can get.)
                    // (Only out past sixty or so: one that's nearly on them gets every gun there is.)
                    if (score > 65 && o.aimT > w.time && !o.alpha && !o.big()) score += Math.min(3, o.aimN - (o == e.aimPick ? 1 : 0)) * 12;
                    // (10.27) A clear shot: not one with a friend standing in the way, when there's another to take.
                    if (score < best && w.city.los(e.x, e.y, o.x, o.y)) {
                        if (!friendInLine(e, o, (float) Math.sqrt(d2) + 0.001f)) {
                            best = score;
                            res = o;
                        } else if (World.TACTICS && score < blockedBest && (o.biteCd > 0 || w.spatial.peopleNear(o.x, o.y, 12))) {
                            // ((21.2) One of the dead on someone, with them in the way: kept, in case there's nothing
                            // else, to work round for an angle on it rather than stand and watch.)
                            blockedBest = score;
                            blocked = o;
                        }
                    }
                }
            }
        // ((21.2) Nothing clear to shoot, and one of the dead on someone with them in the way: that's the one. The shot
        // isn't taken through them (aimAndFire won't): they step round for an angle on it.)
        if (res == null && blocked != null) res = blocked;
        if (res != null) {
            if (res.aimT <= w.time) res.aimN = 0;
            // ((24.0) Their own aim on it doesn't count against it: a soldier kept switching between two of the dead,
            // each looking further off for being aimed at.)
            if (res != e.aimPick || res.aimN == 0) res.aimN++;
            res.aimT = w.time + 0.5f;
        }
        e.aimPick = res;
        // ((14.2) A raider who's been shooting people: the police, soldiers and other armed survivors shoot back, once
        // there's none of the dead closer.)
        if (w.raidersActive > 0 && !e.raider && (e.isArmed() || e.hasGun))
            for (int cy = cy0; cy <= cy1; cy++)
                for (int cx = cx0; cx <= cx1; cx++) {
                    int c = cy * w.gw + cx;
                    for (int k = w.cellStart[c] + w.zCount[c], end = w.cellStart[c] + w.cellCount[c]; k < end; k++) {
                        Entity o = w.sorted[k];
                        if (!o.raider || o.dead || o.hp <= 0 || o.hostileT <= 0) continue;
                        float ddx = o.x - e.x, ddy = o.y - e.y, d2 = ddx * ddx + ddy * ddy;
                        if (d2 > range * range) continue;
                        float score = (float) Math.sqrt(d2) + 40;
                        if (score < best && w.city.los(e.x, e.y, o.x, o.y) && !friendInLine(e, o, (float) Math.sqrt(d2) + 0.001f)) {
                            best = score;
                            res = o;
                        }
                    }
                }
        return res;
    }

    /** ((27.4) How far round them others count as fighting alongside them. */
    static final float MATE_R = 90;
    /** (Those others, found by mates.) */
    private final Entity[] mateBuf = new Entity[12];

    /** ((27.4) The living with guns fighting alongside e (within r), into mateBuf; how many. */
    int mates(Entity e, float r) {
        int n = 0;
        for (int i = 0, m = w.spatial.gatherArmed(e.x, e.y, r); i < m && n < mateBuf.length; i++) {
            Entity o = w.gathered[i];
            if (o == e || o.dead || o.raider || o.isZombie() || !(o.isArmed() || o.hasGun)) continue;
            float dx = o.x - e.x, dy = o.y - e.y;
            if (dx * dx + dy * dy > r * r) continue;
            mateBuf[n++] = o;
        }
        return n;
    }

    /**
     * ((27.4) Fighting together: one of the dead reaching any of them is as pressing as one reaching this one; one
     * closing on someone who's reloading is taken first (covering them); one coming straight at them is taken before
     * one shambling past; and one that's all but down, with someone else already on it, is left to them.
     */
    private float groupScore(Entity e, Entity o, float score, int nm) {
        for (int i = 0; i < nm; i++) {
            Entity m = mateBuf[i];
            float dx = o.x - m.x, dy = o.y - m.y, dm = (float) Math.sqrt(dx * dx + dy * dy);
            if (dm + 12 < score) score = dm + 12;
            if (m.reload > 0 && dm < 55) score -= 40;
        }
        float ex = e.x - o.x, ey = e.y - o.y, ed = (float) Math.sqrt(ex * ex + ey * ey) + 0.001f;
        if (ed < 110 && (o.vx * ex + o.vy * ey) / ed > o.speed * 0.6f) score -= 15;
        if (o.hp <= 8 && o.aimT > w.time && o.aimN >= 1 && o != e.aimPick && ed > 30) score += 35;
        return score;
    }

    /** ((27.4) The i'th of those mates found. */
    Entity mateAt(int i) {
        return mateBuf[i];
    }

    /** ((27.4) Someone alongside e (within r) reloading now. */
    boolean mateReloading(Entity e, float r) {
        for (int i = 0, n = mates(e, r); i < n; i++) if (mateBuf[i].reload > 0) return true;
        return false;
    }

    /** ((27.4) Someone alongside e (within r) with rounds in their gun, ready to cover a reload. */
    boolean mateCovering(Entity e, float r) {
        for (int i = 0, n = mates(e, r); i < n; i++) if (mateBuf[i].reload <= 0 && mateBuf[i].ammo > 2) return true;
        return false;
    }

    /**
     * How likely a hit is to be a stopping shot to the head. Police and soldiers are trained to aim for it,
     * and at close range they usually manage; a frightened civilian rarely does. Harder at range and on a
     * zombie that's running.
     */
    float headshotChance(Entity e, Entity t, float d, float range) {
        // ((12.4) The more of them someone has put down, the surer their aim at the head: a third as likely again for
        // each rank, up to more than twice as likely for the elite. Nobody has to learn it first now: the 11.7 body
        // shots until word got round are gone.)
        float ranked = 1 + 0.3f * World.rank(e);
        float base;
        if (e.role == Entity.ROLE_SNIPER) return 0.45f * World.TRAINED_HEADSHOT * ranked;
        if (e.role == Entity.ROLE_MARKSMAN) return 0.5f * World.TRAINED_HEADSHOT * ranked;
        if (e.weapon == Entity.W_SHOTGUN && d < 45) return 0.3f * World.TRAINED_HEADSHOT * ranked;
        // ((27.5) Aimed pistol fire at close quarters is what police train for: the closer it gets, the surer the shot to the
        // head (at 1 to 3 in 100 a hit, a pair of officers could empty everything they carried into a dozen of them).)
        if (e.type == Entity.COP) base = (e.role == Entity.ROLE_SWAT ? 0.42f : 0.3f) * (d < 70 ? POLICE_HEAD_CLOSE : POLICE_HEAD_FAR);
        else if (e.type == Entity.SOLDIER)
            base = e.role == Entity.ROLE_GUNNER ? 0.2f : e.role == Entity.ROLE_GUARD ? 0.3f : e.role == Entity.ROLE_COMMANDER ? 0.3f : 0.36f;
        else return World.HEADSHOT * ranked;
        float f = Math.max(0.35f, 1.25f - d / range) * ranked;
        // ((16.9) Police: their training (see doctrine).)
        if (e.type == Entity.COP) f *= World.doctrine(e)[1];
        if (t.type == Entity.RUNNER) f *= 0.6f;
        // A clearer shot from the high ground.
        if (w.city.elev != null && w.city.elevation(e.x, e.y) > w.city.elevation(t.x, t.y) + 3) f *= 1.2f;
        return base * f * World.TRAINED_HEADSHOT;
    }

    /**
     * Police and soldiers don't waste rounds on a zombie that's a long way off and not after anyone: they
     * wait for a better shot (or for it to come to them).
     */
    boolean holdFire(Entity e, Entity t, float d, float range) {
        // ((27.4) Someone armed but untrained doesn't spray at the dead a street away either: they wait until it's close, or
        // on someone.)
        if (e.type != Entity.COP && e.type != Entity.SOLDIER)
            return e.hasGun && !e.raider && e != w.controlled && t.isUndead() && e.weapon != Entity.W_HUNTING && e.weapon != Entity.W_CROSSBOW
                    && d > 85 && t.biteCd <= 0 && !w.spatial.peopleNear(t.x, t.y, 45);
        if (e.role == Entity.ROLE_SNIPER || e.role == Entity.ROLE_MARKSMAN || e == w.controlled || !t.isUndead()) return false;
        // ((11.7) Before anyone knows what this is, an officer facing one of them sees a violent suspect, not a monster:
        // they shout warnings and hold fire until it's right on them or on someone else.)
        if (w.alert == 0 && e.type == Entity.COP && e.role != Entity.ROLE_SWAT && d > 34 && !w.spatial.peopleNear(t.x, t.y, 22)) return true;
        // ((27.4) An officer with a pistol waits until they're closer still: rounds fired a street off rarely find a
        // head, and an empty magazine as the first of them arrives gets officers bitten. Not for a runner: that one is
        // coming anyway.)
        float effective = e.type == Entity.COP ? (e.role == Entity.ROLE_SWAT ? 150 : t.type == Entity.RUNNER ? 110 : 85) : 165;
        // ((14.3) Further off on one a drone has marked.)
        if (t.spottedUntil > w.time) effective *= 1.3f;
        return d > effective && t.biteCd <= 0 && !w.spatial.peopleNear(t.x, t.y, 45);
    }

    /** Is a person (not the target) standing between the shooter and the target? */
    boolean friendInLine(Entity e, Entity t, float d) {
        float nx = (t.x - e.x) / d, ny = (t.y - e.y) / d;
        int cx0 = Math.max(0, (int) ((Math.min(e.x, t.x) - 4) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((Math.max(e.x, t.x) + 4) / World.CELL));
        int cy0 = Math.max(0, (int) ((Math.min(e.y, t.y) - 4) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((Math.max(e.y, t.y) + 4) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c] + w.zCount[c], end = w.cellStart[c] + w.cellCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o == e || o == t || o.dead || o.isZombie()) continue;
                    float ox = o.x - e.x, oy = o.y - e.y;
                    float along = ox * nx + oy * ny;
                    if (along < 4 || along > d - 3) continue;
                    // (10.27) Police and soldiers fire past the one right in front (the front rank kneels), and
                    // only someone square in the bullet's path is in the way, not anyone near the line.
                    if (along < 16 && o.isArmed() && e.isArmed()) continue;
                    if (Math.abs(ox * -ny + oy * nx) < o.radius * 0.85f) return true;
                }
            }
        return false;
    }

    /** (What handling worked out for the shot being fired: see fire.) */
    private float shotDmg, shotAccuracy;

    /** ((27.5) A police service round's damage (a civilian's pistol does 10), and how long a body hit checks one of the dead. */
    static float POLICE_PISTOL = 22, POLICE_STAGGER = 0.15f, POLICE_SMG = 14, POLICE_CARBINE = 24;
    /** ((27.5) How much surer police are of a shot to the head, close in (under 70) and further off. */
    static float POLICE_HEAD_CLOSE = 4.2f, POLICE_HEAD_FAR = 1.8f;

    void fire(Entity e, Entity t, float d, float range) {
        e.shotAt = w.time;
        boolean soldier = e.type == Entity.SOLDIER;
        e.ammo--;
        w.shotsFired++;
        handling(e, soldier, d);
        float dmg = shotDmg, accuracy = shotAccuracy;
        if (e.weapon == Entity.W_SHOTGUN && d < 60) pellets(e, t, dmg);
        accuracy = trainedAim(e, soldier, accuracy);
        report(e, t);
        float ca = (float) Math.cos(e.angle), sa = (float) Math.sin(e.angle);
        float mx = e.x + ca * e.radius * (soldier ? 2.5f : 2f) - sa * e.radius * 0.3f;
        float my = e.y + sa * e.radius * (soldier ? 2.5f : 2f) + ca * e.radius * 0.3f;
        boolean scoped = e.role == Entity.ROLE_SNIPER || e.role == Entity.ROLE_MARKSMAN || e.weapon == Entity.W_DMR;
        float hit = accuracy * (1 - (scoped ? 0.1f : 0.4f) * d / range) + (t.type == Entity.BRUTE ? 0.1f : t.type == Entity.GIANT ? 0.2f : 0);
        // ((17.6) Behind cover, half as easy to hit; pinned down by fire, a worse shot.)
        // ((18.4) A police drone overhead marks them: the city's shots at them go true, and cover doesn't hide them.)
        boolean marked = !e.isZombie() && t.isZombie() && w.fieldworks.droneOver(t.x, t.y);
        if (marked) hit *= 1.25f;
        if (World.COVER && !t.isUndead() && !marked && w.armedAI.inCover(t, e.x, e.y)) hit *= 0.5f;
        if (World.COVER) hit *= 1 - 0.45f * e.suppress;
        if (w.rnd.nextFloat() < hit) hits(e, t, d, range, dmg, mx, my);
        else misses(e, t, d, dmg, mx, my);
        muzzle(e, soldier, mx, my);
    }

    /** The shot's damage and how true it flies (left in shotDmg and shotAccuracy), and how long to the next: by who fires it, and the gun. */
    private void handling(final Entity e, final boolean soldier, final float d) {
        float dmg, accuracy = e.hasGun ? 0.5f : 0.7f;
        if ((soldier && e.role == Entity.ROLE_SNIPER) || e.role == Entity.ROLE_MARKSMAN) {
            // ((12.8) A scoped rifle worked quickly by someone trained on it: 1.0 and 1.4 s, from 1.4 and 1.6.)
            e.cooldown = e.role == Entity.ROLE_MARKSMAN ? 1.0f : 1.4f;
            dmg = e.role == Entity.ROLE_MARKSMAN ? 60 : 75;
            accuracy = e.role == Entity.ROLE_MARKSMAN ? 0.95f : 0.97f;
        } else if (soldier && e.role == Entity.ROLE_GUNNER) {
            // ((12.8) In bursts of six or so with a breath between, the way a machine gun is fired, not one long
            // stream.)
            e.burst++;
            e.cooldown = e.burst % 6 == 0 ? 0.35f : 0.075f;
            // ((24.0) A heavier round than a rifle's: at 10 a nest fired 160 rounds in ten seconds and dropped nothing.)
            dmg = 22;
            accuracy = 0.55f;
        } else if (soldier && e.role == Entity.ROLE_COMMANDER) {
            e.cooldown = 0.35f;
            dmg = 12;
        } else if (soldier && e.role == Entity.ROLE_SF) {
            // ((26.0) Special forces: quick, aimed pairs, nearly every one on target.)
            e.burst++;
            e.cooldown = e.burst % 2 == 0 ? 0.3f : 0.12f;
            dmg = 20;
            accuracy = 0.9f;
        } else if (soldier) {
            // Aimed single shots at range; short bursts only when they're close.
            // ((12.8) Quicker aimed shots (0.3 s, from 0.38) and three-round bursts at the rifle's real rate (0.07 s
            // apart, 0.35 s between bursts, from 0.1 and 0.45).)
            if (d > 70) {
                e.burst = 0;
                e.cooldown = 0.3f;
            } else {
                e.burst++;
                e.cooldown = e.burst % 3 == 0 ? 0.35f : 0.07f;
            }
            dmg = 16;
        } else {
            // ((12.8) A service pistol in steady aimed fire, about two and a half a second (0.4 s, from 0.55); a
            // frightened civilian with one, 0.6 (from 0.8).)
            e.cooldown = e.hasGun ? 0.6f : 0.4f;
            // ((27.5) A police officer's service pistol: a full-power duty round, not a civilian's (22, from 10): a body hit
            // used to take one of the dead 2 of its 90, and a pair of officers could empty every magazine they had into a
            // dozen of them and put down five.)
            dmg = e.type == Entity.COP ? POLICE_PISTOL : 10;
        }
        // Picked-up guns.
        if (e.weapon == Entity.W_SHOTGUN) {
            // ((12.8) As fast as the pump can be worked: 0.7 s, from 0.9.)
            e.cooldown = 0.7f;
            dmg = d < 45 ? 34 : d < 80 ? 20 : 9;
            accuracy = 0.92f;
        } else if (e.weapon == Entity.W_RIFLE && !soldier) {
            e.burst++;
            e.cooldown = e.burst % 3 == 0 ? 0.45f : 0.11f;
            dmg = 16;
            accuracy = 0.55f;
        } else if (e.weapon == Entity.W_PISTOL) {
            e.cooldown = 0.4f;
            // ((27.5) An officer issued one carries the duty round in it too.)
            dmg = e.type == Entity.COP ? POLICE_PISTOL : 10;
            accuracy = 0.7f;
        } else if (e.weapon == Entity.W_HUNTING) {
            // A bolt-action deer rifle: slow, but it hits hard and true a long way off.
            e.cooldown = 1.3f;
            dmg = 55;
            accuracy = 0.85f;
        } else if (e.weapon == Entity.W_CROSSBOW) {
            // ((26.0) A bolt: slow to load, hits hard, and quiet.)
            e.cooldown = 0.9f;
            dmg = 48;
            accuracy = 0.88f;
        } else if (e.weapon == Entity.W_REVOLVER) {
            e.cooldown = 0.55f;
            dmg = 18;
            accuracy = 0.68f;
        } else if (e.weapon == Entity.W_SMG) {
            // ((11.4) A submachine gun: quick four-round bursts of pistol rounds, best up close.)
            e.burst++;
            e.cooldown = e.burst % 4 == 0 ? 0.4f : 0.08f;
            dmg = 9;
            accuracy = d < 90 ? 0.66f : 0.5f;
        } else if (e.weapon == Entity.W_CARBINE) {
            // ((11.4) A carbine: steady aimed shots, further and harder than a pistol.)
            e.cooldown = 0.25f;
            dmg = 15;
            accuracy = 0.72f;
        } else if (e.weapon == Entity.W_DMR) {
            // ((11.4) A marksman rifle: a scoped semi-automatic, between a rifle and a sniper's.)
            e.cooldown = 0.6f;
            dmg = 40;
            accuracy = 0.9f;
        }
        // ((27.5) Police long guns carry duty rounds too: the feds' submachine guns (14, from 9) and the patrol and troopers'
        // carbines (24, from 15) - a soldier's or a civilian's are as they were.)
        if (e.type == Entity.COP && e.weapon == Entity.W_SMG) dmg = POLICE_SMG;
        else if (e.type == Entity.COP && e.weapon == Entity.W_CARBINE) dmg = POLICE_CARBINE;
        shotDmg = dmg;
        shotAccuracy = accuracy;
    }

    /** How true the shot flies once training, a commander or command post near by, and the shooter's own eye are counted in. */
    private float trainedAim(final Entity e, final boolean soldier, final float base) {
        float accuracy = base;
        // ((16.9) Police: as well as they've been trained (see doctrine).)
        float[] dc = World.doctrine(e);
        if (dc != null) {
            accuracy *= dc[0];
            e.cooldown *= dc[2];
        }
        // Soldiers near their commander fight better.
        if (soldier && e.role != Entity.ROLE_COMMANDER && !w.commanders.isEmpty() && w.armedAI.commanded(e)) accuracy += 0.12f;
        // ((26.0) And police (or, (26.1), soldiers) near their side's mobile command post.)
        else if (w.ops.boosts(e)) accuracy += 0.1f;
        // (10.20) Some shoot better than others: each one anywhere from as before to 15% better, for good.
        accuracy = Math.min(0.99f, accuracy * (1 + World.aimBonus(e) + 0.05f * World.rank(e) + (World.perk(e, World.P_STEADY) ? 0.08f : 0) + World.levelAim(e)));
        return accuracy;
    }

    /** The shot going off: the reload when that was the last round, the noise of it, the spent case and the smoke. */
    private void report(final Entity e, final Entity t) {
        if (e.ammo <= 0 && e.reserve > 0) {
            e.reload = reloadTime(e);
            w.emit(Sfx.RELOAD, e.x, e.y);
        }
        // (10.45) How far the shot carries depends on the gun: a pistol's crack, a shotgun's boom, a rifle's
        // report, a machine gun's chatter.
        float loud = World.gunLoudness(e);
        if (w.rnd.nextFloat() < (e.role == Entity.ROLE_GUNNER ? 0.12f : 0.25f) * (0.6f + loud / 550f)) w.noise.battleNoise(e.x, e.y, loud);
        if (e.alertCd <= 0) {
            e.alertCd = 1.2f;
            w.noise.heardShot(e, t, loud * 1.12f);
        }
        // A spent case flies out of the side of the gun.
        float ea = e.angle + 1.57f + (w.rnd.nextFloat() - 0.5f) * 0.6f;
        if (w.rnd.nextFloat() < 0.7f)
            w.effects.particle(e.x, e.y, (float) Math.cos(ea) * (25 + w.rnd.nextFloat() * 20), (float) Math.sin(ea) * (25 + w.rnd.nextFloat() * 20),
                    2.5f, 0.45f, 0xFFD9B44A, World.P_CASING);
        // Gunsmoke: it hangs over a long firefight.
        if (w.rnd.nextFloat() < (e.role == Entity.ROLE_GUNNER ? 0.18f : 0.35f)) w.effects.haze(e.x + (float) Math.cos(e.angle) * 6, e.y + (float) Math.sin(e.angle) * 6);
    }

    /** A hit: the head (the only thing that really stops one of the dead) or the body, the kill, the blood. */
    private void hits(final Entity e, final Entity t, final float d, final float range, final float dmg, final float mx, final float my) {
        boolean wasUp = t.hp > 0;
        // Only a hit to the head really stops a zombie; body shots just wear it down.
        boolean headshot = t.isUndead() && w.rnd.nextFloat() < headshotChance(e, t, d, range) * (t.armored ? 0.6f : 1) * (t.type == Entity.BRUTE ? 0.4f : t.type == Entity.BRUISER ? 0.7f : 1)
                * (t.spottedUntil > w.time && e.isArmed() ? World.SPOTTED_AIM : 1);
        if (headshot && t.type == Entity.GIANT) {
            // ((25.0) A shot to a giant's head hurts it, and no more.)
            t.hp -= dmg * World.GUN_DAMAGE * 8;
            if (wasUp && t.hp <= 0) t.bodyShot = true;
        } else if (headshot && t.helmet) {
            // ((27.0) The helmet takes it, and comes off.)
            w.death.loseHelmet(t);
            t.hp -= dmg * World.GUN_DAMAGE * 0.5f;
            if (wasUp && t.hp <= 0) t.bodyShot = true;
        } else if (headshot) {
            t.hp = 0;
            t.gibbed = w.rnd.nextFloat() < 0.3f;
            for (int k = 0; k < 6; k++)
                w.effects.particle(t.x, t.y, w.rnd.nextFloat() * 70 - 35, w.rnd.nextFloat() * 70 - 35, 0.4f, 1.1f, 0xFF7A1010, World.P_DOT);
            if (w.gore) w.effects.decal(t.x + w.rnd.nextFloat() * 6 - 3, t.y + w.rnd.nextFloat() * 6 - 3, 2.5f + w.rnd.nextFloat() * 2, 0xAA5A0808);
        } else {
            // ((10.32) Body armour on one of the dead takes most of a body shot.)
            t.hp -= t.isUndead() ? dmg * World.GUN_DAMAGE * Director.GUN[w.difficulty] * (t.armored ? 0.5f : 1) * (1 + w.zombieAI.rot(t)) : dmg * (t.armored ? 0.65f : 1);
            if (wasUp && t.hp <= 0) t.bodyShot = true;
            // ((27.5) Stopping power: a police round in the body checks one of the dead for a moment - not the big ones,
            // and not through armour.)
            if (e.type == Entity.COP && t.isUndead() && !t.big() && !t.armored && t.hp > 0) t.stun = Math.max(t.stun, POLICE_STAGGER);
        }
        t.hurt = 1;
        if (wasUp && t.hp <= 0 && t.isZombie()) {
            w.experience.killMade(e, t);
            if (e.kills == 10 || e.kills == 25 || e.kills == 50 || e.kills == 100) w.stats.killStreak(e);
        }
        t.killedByZombie = false;
        float nx = (t.x - e.x) / d, ny = (t.y - e.y) / d;
        w.effects.bloodBurst(t.x, t.y, 4, nx, ny);
        w.effects.tracer(mx, my, t.x + w.rnd.nextFloat() * 2 - 1, t.y + w.rnd.nextFloat() * 2 - 1);
        w.movement.tryMove(t, nx * 1.2f / t.mass, ny * 1.2f / t.mass);
    }

    /** A miss: heads kept down, and where the round goes instead - a wall, a car, the ground. */
    private void misses(final Entity e, final Entity t, final float d, final float dmg, final float mx, final float my) {
        // ((17.6) Close enough to keep their head down: a machine gun's bursts most of all.)
        if (World.COVER && !t.isUndead()) t.suppress = Math.min(1, t.suppress + (e.role == Entity.ROLE_GUNNER ? 0.3f : 0.12f));
        float a = (float) Math.atan2(t.y - my, t.x - mx) + (w.rnd.nextBoolean() ? 1 : -1) * (0.05f + w.rnd.nextFloat() * 0.1f);
        float len = d + 20 + w.rnd.nextFloat() * 40;
        float ex = mx, ey = my;
        Fleet.Vehicle car = null;
        boolean hitWall = false;
        for (float s = 0; s < len && car == null; s += 4) {
            ex = mx + (float) Math.cos(a) * s;
            ey = my + (float) Math.sin(a) * s;
            if (w.city.solidAt(ex, ey)) {
                for (int i = 0; i < 3; i++)
                    w.effects.particle(ex, ey, w.rnd.nextFloat() * 60 - 30, w.rnd.nextFloat() * 60 - 30, 0.2f, 0.7f, 0xFFFFD27A, World.P_DOT);
                w.effects.impactPuff(ex - (float) Math.cos(a) * 2, ey - (float) Math.sin(a) * 2, 0xFFB8B0A0);
                bulletHole(ex, ey, e.x, e.y);
                hitWall = true;
                // ((13.4) Now and then the whine of one coming off the wall.)
                if (w.rnd.nextFloat() < 0.2f) w.emit(Sfx.RICOCHET, ex, ey);
                break;
            }
            if (s > 12 && ((int) s) % 8 == 0) car = w.rides.vehicleAt(ex, ey);
        }
        // Stray rounds put holes in cars too.
        if (car != null) w.fleet.damage(car, dmg * 0.4f, false);
        // A miss kicks up a little puff of dust where it lands.
        else if (!hitWall) w.effects.impactPuff(ex, ey, 0xFF8E8676);
        w.effects.tracer(mx, my, ex, ey);
    }

    /** The flash and the bang. */
    private void muzzle(final Entity e, final boolean soldier, final float mx, final float my) {
        // ((26.0) A crossbow makes no flash and no bang.)
        if (e.weapon == Entity.W_CROSSBOW) return;
        w.effects.particle(mx, my, 0, 0, 0.06f, soldier ? 3.2f : 2.6f, 0xFFFFE9A0, World.P_FLASH);
        int gunSound = e.weapon == Entity.W_SHOTGUN ? Sfx.SHOTGUN : e.weapon == Entity.W_HUNTING ? Sfx.HUNTING : e.weapon == Entity.W_REVOLVER ? Sfx.REVOLVER
                : e.weapon == Entity.W_SMG ? Sfx.SMG : e.weapon == Entity.W_CARBINE ? Sfx.CARBINE : e.weapon == Entity.W_DMR ? Sfx.DMR : -1;
        if (gunSound >= 0) w.emit(gunSound, e.x, e.y);
        // ((22.0) More of them with a sound of their own: SWAT and recon carbines, grenadiers' and Marines' bursts.)
        else w.emit(e.role == Entity.ROLE_MARKSMAN ? Sfx.SNIPER : e.role == Entity.ROLE_SWAT || e.role == Entity.ROLE_SAR || e.role == Entity.ROLE_RECON ? Sfx.CARBINE
                : !soldier || e.role == Entity.ROLE_COMMANDER ? Sfx.PISTOL : e.role == Entity.ROLE_SNIPER ? Sfx.SNIPER
                : e.role == Entity.ROLE_GUNNER ? Sfx.MG : e.role == Entity.ROLE_GRENADIER || e.role == Entity.ROLE_MARINE ? Sfx.BURST : Sfx.RIFLE, e.x, e.y);
    }

    void throwGrenade(Entity e, float x, float y) {
        e.grenades--;
        e.grenadeCd = 10;
        World.Grenade g = new World.Grenade();
        g.sx = e.x;
        g.sy = e.y;
        g.tx = x + w.rnd.nextFloat() * 16 - 8;
        g.ty = y + w.rnd.nextFloat() * 16 - 8;
        g.dur = 0.9f;
        g.owner = e;
        w.grenades.add(g);
    }

    void blast(Entity o, float x, float y, float radius, float damage) {
        if (o.dead) return;
        float ddx = o.x - x, ddy = o.y - y;
        float d = (float) Math.sqrt(ddx * ddx + ddy * ddy);
        if (d > radius) return;
        // ((17.2) A wall between them and the blast: it takes it.)
        if (World.WALK_IN && w.city.insideOf(o.x, o.y) != w.city.insideOf(x, y) && !w.city.los(x, y, o.x, o.y)) return;
        float f = 1 - d / radius;
        boolean wasUp = o.hp > 0;
        o.hp -= damage * (0.25f + 0.75f * f);
        if (wasUp && o.hp <= 0 && o.isZombie() && w.blastBy != null && !w.blastBy.isZombie()) w.experience.killMade(w.blastBy, o);
        o.hurt = 1;
        o.killedByZombie = false;
        if (o.hp <= 0 && f > 0.4f) o.gibbed = true;
        if (d > 0.01f) w.movement.tryMove(o, ddx / d * 16 * f / o.mass, ddy / d * 16 * f / o.mass);
        if (d > 0.01f) {
            o.knockX = ddx / d * 180 * f / o.mass;
            o.knockY = ddy / d * 180 * f / o.mass;
        }
        if (!o.isZombie()) {
            o.fleeTimer = 3;
            o.threatX = x;
            o.threatY = y;
        }
    }

    /** A tank round: a flash at the muzzle and a blast where it lands. */
    void tankShell(float fx, float fy, float tx, float ty) {
        w.effects.particle(fx, fy, 0, 0, 0.12f, 7, 0xFFFFE9A0, World.P_FLASH);
        w.emit(Sfx.CANNON, fx, fy);
        for (int i = 0; i < 8; i++)
            w.effects.particle(fx, fy, w.rnd.nextFloat() * 30 - 15, w.rnd.nextFloat() * 30 - 15, 1.2f, 4, 0xFF8A8478, World.P_SMOKE);
        w.effects.tracer(fx, fy, tx, ty);
        w.shotsFired++;
        explode(tx, ty, 50, 230);
    }

    /** One burst from the helicopter's door gun. */
    void airShot(float fx, float fy, Entity t) {
        w.shotsFired++;
        if (w.rnd.nextFloat() < 0.75f) {
            t.hp -= 22;
            t.hurt = 1;
            w.effects.bloodBurst(t.x, t.y, 4, 0, 0);
            w.effects.tracer(fx, fy, t.x, t.y);
        } else {
            w.effects.tracer(fx, fy, t.x + w.rnd.nextFloat() * 24 - 12, t.y + w.rnd.nextFloat() * 24 - 12);
        }
        w.emit(Sfx.RIFLE, fx, fy);
    }

    /** A marksman's careful shot from above (the police helicopter): most of them drop the zombie for good. */
    void marksmanShot(float fx, float fy, Entity t) {
        w.shotsFired++;
        if (w.rnd.nextFloat() < 0.85f && !w.spatial.peopleNear(t.x, t.y, 10)) {
            t.hp -= !t.big() && w.rnd.nextFloat() < 0.55f ? t.hp + 1 : 40;
            t.hurt = 1;
            t.killedByZombie = false;
            w.effects.bloodBurst(t.x, t.y, 5, 0, 0);
            w.effects.tracer(fx, fy, t.x, t.y);
        } else {
            w.effects.tracer(fx, fy, t.x + w.rnd.nextFloat() * 20 - 10, t.y + w.rnd.nextFloat() * 20 - 10);
        }
        w.emit(Sfx.SNIPER, fx, fy);
    }

    /**
     * A vehicle's roof gun (an APC's, a Humvee's) firing at a zombie, if it has a clear line and nobody living is
     * standing next to it. Returns false if it held fire.
     */
    /** (14.6) An autocannon round bursting at (x, y): the dead within r of it hurt too (dmg each, less further out). */
    void cannonSplash(float x, float y, float r, float dmg) {
        int cx0 = Math.max(0, (int) ((x - r) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((x + r) / World.CELL));
        int cy0 = Math.max(0, (int) ((y - r) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((y + r) / World.CELL));
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c], end = k + w.zCount[c]; k < end; k++) {
                    Entity o = w.sorted[k];
                    if (o.dead || o.hp <= 0 || !o.isZombie()) continue;
                    float d = (float) World.hypot(o.x - x, o.y - y);
                    if (d > r) continue;
                    o.hp -= dmg * (1 - 0.5f * d / r) * (o.armored ? 0.6f : 1);
                    o.hurt = 1;
                    o.killedByZombie = false;
                }
            }
        w.effects.particle(x, y, 0, 0, 0.1f, 5, 0xFFFFD27A, World.P_FLASH);
        for (int i = 0; i < 4; i++) {
            float a = w.rnd.nextFloat() * World.TAU, s = 30 + w.rnd.nextFloat() * 60;
            w.effects.particle(x, y, (float) Math.cos(a) * s, (float) Math.sin(a) * s, 0.4f + w.rnd.nextFloat() * 0.3f, 0.6f + w.rnd.nextFloat() * 0.6f, 0xFF4A4640, World.P_DEBRIS);
        }
    }

    boolean turretShot(float fx, float fy, Entity t, float dmg) {
        if (!w.city.los(fx, fy, t.x, t.y) || w.spatial.peopleNear(t.x, t.y, 16)) return false;
        w.shotsFired++;
        if (w.rnd.nextFloat() < 0.7f) {
            boolean head = !t.big() && w.rnd.nextFloat() < 0.15f;
            t.hp -= head ? t.hp + 1 : dmg;
            t.hurt = 1;
            t.killedByZombie = false;
            w.effects.bloodBurst(t.x, t.y, 4, 0, 0);
            w.effects.tracer(fx, fy, t.x, t.y);
        } else {
            w.effects.tracer(fx, fy, t.x + w.rnd.nextFloat() * 24 - 12, t.y + w.rnd.nextFloat() * 24 - 12);
        }
        w.effects.particle(fx, fy, 0, 0, 0.06f, 3.4f, 0xFFFFE9A0, World.P_FLASH);
        w.emit(Sfx.MG, fx, fy);
        // ((10.45) A heavy machine gun is heard a long way off.)
        if (w.rnd.nextFloat() < 0.2f) w.noise.battleNoise(fx, fy, 360);
        return true;
    }

    void explode(float x, float y, float radius, float damage) {
        // ((13.5) From the grid as it stands, with room for the steps since it was made, and anyone come into the world
        // since: the whole grid was made again for every grenade, rocket and shell, which in a big fight on a big map
        // was a fifth of the time.)
        float reach = radius + World.GRID_SLACK;
        int cx0 = Math.max(0, (int) ((x - reach) / World.CELL)), cx1 = Math.min(w.gw - 1, (int) ((x + reach) / World.CELL));
        int cy0 = Math.max(0, (int) ((y - reach) / World.CELL)), cy1 = Math.min(w.gh - 1, (int) ((y + reach) / World.CELL));
        if (w.hashSize < 0) w.spatial.buildHash();
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                int c = cy * w.gw + cx;
                for (int k = w.cellStart[c], end = w.cellStart[c] + w.cellCount[c]; k < end; k++) blast(w.sorted[k], x, y, radius, damage);
            }
        for (int i = w.hashSize, n = w.entities.size(); i < n; i++) blast(w.entities.get(i), x, y, radius, damage);
        for (int i = w.corpses.size() - 1; i >= 0; i--) {
            World.Corpse c = w.corpses.get(i);
            float ddx = c.x - x, ddy = c.y - y;
            if (ddx * ddx + ddy * ddy < radius * radius * 0.5f) {
                w.corpses.remove(i);
                w.effects.bloodBurst(c.x, c.y, 6, 0, 0);
            }
        }
        // Vehicles caught in the blast.
        for (int i = 0, n = w.fleet.vehicles.size(); i < n; i++) {
            Fleet.Vehicle v = w.fleet.vehicles.get(i);
            if (Fleet.offRoad(v)) continue;
            float d = (float) World.hypot(v.x - x, v.y - y);
            // ((17.6) Not a helicopter up in the air: a blast on the ground doesn't reach it.)
            if (d < radius && !(v.type == Fleet.HELI && v.alt > 0.3f)) w.fleet.damage(v, damage * (1.1f - d / radius), d < radius * 0.5f);
        }
        // ((22.0) Parked cars close to the blast burn out, and some of them go up in turn a moment later: a chain
        // down a row of them.)
        int pr = (int) (radius * 0.6f / City.T) + 1, burnt = 0;
        int ptx = (int) (x / City.T), pty = (int) (y / City.T);
        for (int ty = Math.max(0, pty - pr); ty <= Math.min(w.city.h - 1, pty + pr) && burnt < 6; ty++)
            for (int tx = Math.max(0, ptx - pr); tx <= Math.min(w.city.w - 1, ptx + pr) && burnt < 6; tx++) {
                float cxw = (tx + 0.5f) * City.T, cyw = (ty + 0.5f) * City.T;
                if (World.hypot(cxw - x, cyw - y) > radius * 0.6f) continue;
                if (!w.city.burnParkedCar(ty * w.city.w + tx)) continue;
                burnt++;
                w.carsChained++;
                w.city.charTile(tx, ty);
                if (w.rnd.nextFloat() < 0.45f) blastLater(cxw, cyw, 42, 140, 1.5f + w.rnd.nextFloat() * 2.5f);
            }
        // Buildings: scorched walls, and enough of a hit brings one down.
        int bx0 = Math.max(0, (int) ((x - radius) / City.T)), bx1 = Math.min(w.city.w - 1, (int) ((x + radius) / City.T));
        int by0 = Math.max(0, (int) ((y - radius) / City.T)), by1 = Math.min(w.city.h - 1, (int) ((y + radius) / City.T));
        for (int ty = by0; ty <= by1; ty++)
            for (int tx = bx0; tx <= bx1; tx++) {
                int bi = w.city.buildingAt.get(ty * w.city.w + tx);
                if (bi < 0) continue;
                City.Building b = w.city.buildings.get(bi);
                if (b.collapsed || w.hitBuildings.contains(b)) continue;
                w.hitBuildings.add(b);
            }
        for (int i = 0; i < w.hitBuildings.size(); i++) blastBuilding(w.hitBuildings.get(i), x, y, radius, damage);
        w.hitBuildings.clear();

        World.Explosion ex = new World.Explosion();
        ex.x = x;
        ex.y = y;
        ex.r = radius;
        ex.life = ex.max = 0.45f;
        w.explosions.add(ex);
        w.emit(Sfx.EXPLOSION, x, y);
        // Parked cars in the blast catch fire; gas pumps go up a moment later.
        int tx0 = Math.max(0, (int) ((x - radius) / City.T)), tx1 = Math.min(w.city.w - 1, (int) ((x + radius) / City.T));
        int ty0 = Math.max(0, (int) ((y - radius) / City.T)), ty1 = Math.min(w.city.h - 1, (int) ((y + radius) / City.T));
        for (int ty = ty0; ty <= ty1; ty++)
            for (int tx = tx0; tx <= tx1; tx++) {
                int i = ty * w.city.w + tx;
                byte t = w.city.tiles[i];
                if ((t != City.CAR && t != City.PUMP) || w.burned[i]) continue;
                float cx = tx * City.T + City.T / 2f, cy = ty * City.T + City.T / 2f;
                if ((cx - x) * (cx - x) + (cy - y) * (cy - y) > radius * radius * 0.8f) continue;
                if (t == City.CAR && w.rnd.nextFloat() > 0.6f) continue;
                w.burnTile(i);
                if (t == City.PUMP) w.pendingBlasts.add(new float[]{cx, cy, 90, 380, 0.3f + w.rnd.nextFloat() * 0.5f});
                else if (w.rnd.nextFloat() < 0.25f) w.pendingBlasts.add(new float[]{cx, cy, 45, 150, 1.5f + w.rnd.nextFloat() * 3});
            }
        w.noise.battleNoise(x, y, 450);
        for (int i = 0; i < 40; i++) {
            float a = w.rnd.nextFloat() * World.TAU, s = 30 + w.rnd.nextFloat() * radius * 2.2f;
            w.effects.particle(x, y, (float) Math.cos(a) * s, (float) Math.sin(a) * s, 0.3f + w.rnd.nextFloat() * 0.4f,
                    2 + w.rnd.nextFloat() * 3, w.rnd.nextBoolean() ? 0xFFFFB030 : 0xFFFF6A1A, World.P_FIRE);
        }
        // Chunks of debris thrown out. ((14.4) More of them, some bigger, and glowing embers among them.)
        for (int i = 0; i < 30; i++) {
            float a = w.rnd.nextFloat() * World.TAU, s = 60 + w.rnd.nextFloat() * radius * 2.8f;
            boolean ember = i % 5 == 0;
            w.effects.particle(x, y, (float) Math.cos(a) * s, (float) Math.sin(a) * s, 0.8f + w.rnd.nextFloat() * 0.8f,
                    (ember ? 0.6f : 0.8f) + w.rnd.nextFloat() * (i % 4 == 0 ? 2.2f : 1.2f), ember ? 0xFFFF9A30 : w.rnd.nextBoolean() ? 0xFF3A3632 : 0xFF6A645C, World.P_DEBRIS);
        }
        for (int i = 0; i < 22; i++) {
            float a = w.rnd.nextFloat() * World.TAU, s = 5 + w.rnd.nextFloat() * radius * 0.8f;
            w.effects.particle(x, y, (float) Math.cos(a) * s, (float) Math.sin(a) * s, 1.2f + w.rnd.nextFloat() * 1.3f,
                    5 + w.rnd.nextFloat() * 6, 0xFF3A3A3A, World.P_SMOKE);
        }
        w.effects.decal(x, y, radius * 0.55f, 0xAA121212);
        w.shake = Math.min(1.5f, w.shake + radius / 60f);
    }

    /** A bullet hole where a stray shot hit a wall. */
    void bulletHole(float x, float y, float fromX, float fromY) {
        City.Building b = w.city.buildingAt(x, y);
        if (b == null) return;
        int side = World.wallSide(b, fromX, fromY);
        b.mark(side, World.along(b, side, x, y), 2 + w.rnd.nextFloat() * Math.min(14, b.height - 3), 0.5f + w.rnd.nextFloat() * 0.3f);
    }

    void blastBuilding(City.Building b, float x, float y, float radius, float damage) {
        float nx = Math.max(b.x0, Math.min(x, b.x1)), ny = Math.max(b.y0, Math.min(y, b.y1));
        float d = (float) World.hypot(nx - x, ny - y);
        if (d > radius) return;
        float f = 1 - d / radius;
        int side = World.wallSide(b, x, y);
        b.mark(side, World.along(b, side, nx, ny), 1 + w.rnd.nextFloat() * 8, 3 + f * 5);
        b.barricade -= damage * f * 0.3f;
        b.hp -= damage * f * (w.heavy ? 2.5f : 1);
        if (b.hp > 0) return;
        collapseBuilding(b);
    }

    /** Brings a building down (blasted or burnt out): some inside get out, rubble and dust everywhere. */
    void collapseBuilding(City.Building b) {
        if (b.collapsed) return;
        // Down it comes.
        int inside = b.occupants.size();
        for (int i = 0; i < inside; i++) {
            Entity o = b.occupants.get(i);
            if (w.rnd.nextFloat() < 0.5f || !w.rides.release(o, b.doorX, b.doorY)) {
                if (o.type == Entity.CIVILIAN) w.civiliansLost++;
            } else {
                o.hp *= 0.4f;
                o.fleeTimer = 3;
                o.threatX = (b.x0 + b.x1) / 2;
                o.threatY = (b.y0 + b.y1) / 2;
            }
        }
        float cx = (b.x0 + b.x1) / 2, cy = (b.y0 + b.y1) / 2;
        float h = b.height;
        w.city.collapse(b);
        w.collapsedCount++;
        w.stats.highlight("A building collapsed", cx, cy);
        w.dispatch.say(Dispatch.WHO_INFO, null, "A building collapsed on " + w.city.placeName(cx, cy) + "!"
                + (inside > 0 ? " People were trapped inside." : ""), cx, cy);
        // Anyone standing next to it gets hit by falling debris.
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity o = w.entities.get(i);
            if (o.x > b.x0 - 12 && o.x < b.x1 + 12 && o.y > b.y0 - 12 && o.y < b.y1 + 12) {
                o.hp -= 40 + h * 0.5f;
                o.hurt = 1;
            }
        }
        int dust = (int) Math.min(60, 12 + (b.x1 - b.x0) * (b.y1 - b.y0) / 200);
        for (int i = 0; i < dust; i++)
            w.effects.particle(b.x0 + w.rnd.nextFloat() * (b.x1 - b.x0), b.y0 + w.rnd.nextFloat() * (b.y1 - b.y0), w.rnd.nextFloat() * 40 - 20,
                    w.rnd.nextFloat() * 40 - 20, 2.5f + w.rnd.nextFloat() * 2, 6 + w.rnd.nextFloat() * 6, 0xFF9A948A, World.P_SMOKE);
        w.emit(Sfx.COLLAPSE, cx, cy);
        w.emit(Sfx.DEBRIS, cx + (w.rnd.nextFloat() - 0.5f) * 20, cy + (w.rnd.nextFloat() - 0.5f) * 20);
        w.shake = Math.min(2f, w.shake + 1f);
        w.noise.noise(cx, cy, 400);
        // ((18.4) Heavy destruction: debris thrown wide, and the buildings next to it hit hard enough to come down too.)
        if (w.heavy && w.collapseChain < 3) {
            w.collapseChain++;
            float reach = 26 + h * 0.3f;
            for (int i = 0, n = w.entities.size(); i < n; i++) {
                Entity o = w.entities.get(i);
                if (o.dead || o.ride != null) continue;
                if (o.x > b.x0 - reach && o.x < b.x1 + reach && o.y > b.y0 - reach && o.y < b.y1 + reach && w.rnd.nextFloat() < 0.5f) {
                    o.hp -= 15 + w.rnd.nextFloat() * 30;
                    o.hurt = 1;
                }
            }
            for (int i = 0; i < 20; i++)
                w.effects.particle(cx, cy, w.rnd.nextFloat() * 160 - 80, w.rnd.nextFloat() * 160 - 80, 1.2f + w.rnd.nextFloat(), 2 + w.rnd.nextFloat() * 3, 0xFF6E6860, World.P_DOT);
            java.util.ArrayList<City.Building> near = new java.util.ArrayList<City.Building>();
            for (int i = 0, n = w.city.buildings.size(); i < n; i++) {
                City.Building o = w.city.buildings.get(i);
                if (o == b || o.collapsed) continue;
                if (o.x1 > b.x0 - 20 && o.x0 < b.x1 + 20 && o.y1 > b.y0 - 20 && o.y0 < b.y1 + 20) near.add(o);
            }
            for (City.Building o : near) blastBuilding(o, cx, cy, Math.max(b.x1 - b.x0, b.y1 - b.y0) + 30, 60 + h * 2);
            w.collapseChain--;
        }
    }

    void updateGrenades(float dt) {
        for (int i = w.grenades.size() - 1; i >= 0; i--) {
            World.Grenade g = w.grenades.get(i);
            g.t += dt;
            if (g.t >= g.dur) {
                w.grenades.remove(i);
                // ((10.62) Smaller and weaker than before (48 across, 140): it kills what's right on it and hurts
                // the rest, rather than clearing a street.)
                // ((14.0) A mortar shell: a little bigger.)
                // ((15.3) The dead it kills count as the thrower's kills.)
                w.blastBy = g.owner;
                if (g.shell) explode(g.tx, g.ty, World.MORTAR_BLAST, World.MORTAR_DAMAGE);
                else explode(g.tx, g.ty, 36, 110);
                w.blastBy = null;
            }
        }
    }
}
