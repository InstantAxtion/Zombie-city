package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Events: the giant's placing, air strikes, plane crashes, prison breaks, riots and the city alarm.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class Events {
    private final World w;

    Events(World w) {
        this.w = w;
    }

    /** ((25.0) Giants put down by hand: the first call about them. */
    void giantPlaced(Entity z) {
        w.giantsPlaced++;
        if (w.time - w.giantSaidAt < 20) return;
        w.giantSaidAt = w.time;
        w.dispatch.say(Dispatch.WHO_911, null, "\"There's something huge on " + w.city.placeName(z.x, z.y)
                + "! It's tearing through cars like paper!\"", z.x, z.y, true);
        w.dispatch.sighting(z.x, z.y, null);
    }

    /** ((25.0) Whole seconds until another airstrike can be called (0: ready). */
    int airstrikeWait() {
        return Math.max(0, (int) Math.ceil(w.airstrikeReady - w.time));
    }

    /** ((25.0) The sandbox's airstrike: free, but one every minute and a half. True if the jet's on its way. */
    boolean freeAirstrike(float x, float y) {
        int wait = airstrikeWait();
        if (wait > 0) {
            w.say("Airstrike not ready: " + wait + "s");
            return false;
        }
        airstrike(x, y);
        w.airstrikeReady = w.time + World.AIRSTRIKE_COOLDOWN;
        return true;
    }

    /** ((25.0) A passenger jet coming down on (x, y). False if there's nowhere there for it to come down. */
    boolean planeCrash(float x, float y) {
        float[] p = w.city.findWalkable(x, y);
        if (p == null) return false;
        w.fleet.sendCrashingPlane(p[0], p[1]);
        w.dispatch.say(Dispatch.WHO_INFO, null, "News: Air traffic control has lost contact with a passenger jet. It's coming down over "
                + w.city.placeName(p[0], p[1]) + "!", p[0], p[1], true);
        return true;
    }

    /** ((25.0) The jet hits the ground at (x, y) going along a: the fireball, the wreck, and those who walk out of it. */
    void planeCrashed(float x, float y, float a) {
        w.planeCrashes++;
        float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
        w.combat.explode(x, y, 75, 320);
        // Down the line it skidded along, and the wings' fuel going up a moment after.
        for (int k = 1; k <= 4; k++)
            w.combat.blastLater(x - ca * k * 26 + (w.rnd.nextFloat() - 0.5f) * 20, y - sa * k * 26 + (w.rnd.nextFloat() - 0.5f) * 20, 50, 160, 0.3f + k * 0.35f);
        for (int k = 0; k < 7; k++) w.effects.decal(x - ca * k * 14, y - sa * k * 14, 9 + w.rnd.nextFloat() * 6, 0xAA121212);
        if (w.crashes.size() >= 4) w.crashes.remove(0);
        w.crashes.add(new float[]{x, y, a, 0});
        // Those who walk out of it, some hurt, a few bitten; and the infected aboard.
        int alive = 0;
        for (int k = 0; k < 9; k++) {
            float ang = a + 1.57f * (k % 2 == 0 ? 1 : -1) + (w.rnd.nextFloat() - 0.5f) * 1.2f, r = 34 + w.rnd.nextFloat() * 22;
            Entity e = w.population.spawn(Entity.CIVILIAN, x + (float) Math.cos(ang) * r - ca * k * 6, y + (float) Math.sin(ang) * r - sa * k * 6);
            if (e == null) continue;
            alive++;
            e.hp *= 0.45f + w.rnd.nextFloat() * 0.45f;
            e.hurt = 1;
            e.home = null;
            e.fleeTimer = 4 + w.rnd.nextFloat() * 4;
            e.threatX = x;
            e.threatY = y;
            if (k % 3 == 1) {
                e.infected = true;
                e.infectTimer = w.war.turnTime(25 + w.rnd.nextFloat() * 30);
            }
        }
        int dead = 0;
        for (int k = 0; k < 5; k++) {
            float ang = w.rnd.nextFloat() * World.TAU, r = 26 + w.rnd.nextFloat() * 26;
            Entity z = w.population.spawn(Entity.ZOMBIE, x + (float) Math.cos(ang) * r, y + (float) Math.sin(ang) * r);
            if (z == null) continue;
            dead++;
            z.hp *= 0.7f;
            z.stun = 1.5f;
        }
        w.banner("PLANE CRASH", "A passenger jet has come down on " + w.city.placeName(x, y) + ". Survivors are walking out of the wreck - and not all of them are well.");
        w.stats.timeline("A passenger jet crashed", x, y);
        w.dispatch.say(Dispatch.WHO_911, null, "\"A plane just came down on " + w.city.placeName(x, y) + "! There's fire everywhere - people are walking out of it!\"", x, y, true);
        if (dead > 0) w.dispatch.sighting(x, y, null);
        boolean amb = w.fleet.sendAmbulance(x, y);
        w.dispatch.say(Dispatch.WHO_POLICE, null, "Dispatch: All units, aircraft down at " + w.city.placeName(x, y) + "."
                + (amb ? " Ambulance en route." : "") + (alive > 0 ? " Reports of " + alive + " survivors." : ""), x, y);
    }

    boolean prisonBreak() {
        if (w.city.prison == null || w.city.prisonGate == null) {
            w.say("No prison on this map");
            return false;
        }
        if (w.prisonFallen) {
            w.say("The prison's already open");
            return false;
        }
        w.prisonFallen = true;
        w.city.openPrisonGate();
        w.containment.routesChanged();
        int loose = 0;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (!e.inmate) continue;
            e.inmate = false;
            if (e.dead) continue;
            e.escapee = true;
            e.riotX = e.riotY = 0;
            loose++;
        }
        w.escaped += loose;
        float gx = (w.city.prisonGate[0] + 1.5f) * City.T, gy = w.city.prisonGate[1] * City.T;
        w.emit(Sfx.ALARM, gx, gy);
        w.banner("PRISON BREAK", loose + " inmates have broken out of the prison and are scattering into town.");
        w.dispatch.say(Dispatch.WHO_POLICE, null, "Police Command: Prison break at " + w.city.placeName(gx, gy) + "! " + loose
                + " inmates over the wall. All available units, round them up.", gx, gy, true);
        w.stats.timeline("A prison break: " + loose + " inmates loose", gx, gy);
        return true;
    }

    /** ((25.0) On the run: from the dead like anyone, from the police most of all, and away into town. */
    boolean escapeeStep(Entity e, float dt) {
        if (w.city.fieldAt(w.city.zombieDist, e.x, e.y) <= 100 / City.T + 6 && w.spatial.nearest(e, 90, true, false) != null) return false;
        Entity cop = w.spatial.nearestPolice(e.x, e.y, 75);
        if (cop != null) {
            float d = (float) World.hypot(e.x - cop.x, e.y - cop.y) + 0.001f;
            w.movement.flee(e, (e.x - cop.x) / d, (e.y - cop.y) / d, e.runSpeed * 0.85f);
            return true;
        }
        if ((e.riotX == 0 && e.riotY == 0) || World.hypot(e.riotX - e.x, e.riotY - e.y) < 14 || e.stuckTime > 4) {
            float[] p = w.city.randomWalkableInTown(w.rnd);
            e.riotX = p[0];
            e.riotY = p[1];
            e.stuckTime = 0;
        }
        w.wilds.walkToSpot(e, e.riotX, e.riotY, e.speed * 1.3f);
        return true;
    }

    /** ((25.0) An officer after an escaped inmate: runs them down and takes them in. False to get on as usual. */
    boolean collarStep(Entity e, float dt) {
        Entity s = e.collar;
        if (s.dead || s.removed || !s.escapee || World.hypot(s.x - e.x, s.y - e.y) > 700 || w.spatial.nearest(e, 100, true, false) != null) {
            e.collar = null;
            return false;
        }
        float d = (float) World.hypot(s.x - e.x, s.y - e.y);
        if (d < e.radius + s.radius + 5) {
            s.escapee = false;
            s.dead = true;
            s.removed = true;
            e.collar = null;
            w.recaptured++;
            int left = 0;
            for (int i = 0, n = w.entities.size(); i < n; i++) if (w.entities.get(i).escapee && !w.entities.get(i).dead) left++;
            w.dispatch.chatter(Dispatch.WHO_POLICE, e, "Escapee in custody on " + w.city.placeName(e.x, e.y) + "."
                    + (left > 0 ? " " + left + " still at large." : " That's the last of them."), e.x, e.y);
            if (left == 0) w.stats.timeline("The last escaped inmate was caught", e.x, e.y);
            return true;
        }
        w.wilds.walkToSpot(e, s.x, s.y, e.runSpeed);
        return true;
    }

    /** ((25.0) The crowd round (x, y) riots (topped up with a few more to make a crowd); the riot police are called. */
    boolean riot(float x, float y) {
        if (w.riot != null) {
            w.say("There's a riot on already, on " + w.city.placeName(w.riot[0], w.riot[1]));
            return false;
        }
        float[] p = w.city.findWalkable(x, y);
        if (p == null) return false;
        x = p[0];
        y = p[1];
        float until = w.time + World.RIOT_TIME;
        int n = 0;
        for (int i = 0, c = w.entities.size(); i < c; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || e.type != Entity.CIVILIAN || e.inmate || e.infected || e == w.controlled || e.zone != null) continue;
            if ((e.x - x) * (e.x - x) + (e.y - y) * (e.y - y) > 150 * 150) continue;
            rioter(e, x, y, until);
            n++;
        }
        for (int k = n; k < 16; k++) {
            double a = w.rnd.nextFloat() * Math.PI * 2;
            float r = 20 + w.rnd.nextFloat() * 60;
            Entity e = w.population.spawn(Entity.CIVILIAN, x + (float) Math.cos(a) * r, y + (float) Math.sin(a) * r);
            if (e == null) continue;
            rioter(e, x, y, until);
            n++;
        }
        if (n == 0) return false;
        w.riot = new float[]{x, y, until};
        w.riotCars = w.riotLoots = w.riotHeld = 0;
        w.riots++;
        w.emit(Sfx.PANIC, x, y);
        w.wilds.scareBirds(x, y, 200);
        w.dispatch.say(Dispatch.WHO_911, null, "\"There's a riot on " + w.city.placeName(x, y) + "! They're smashing windows and setting cars on fire!\"", x, y, true);
        w.stats.timeline("A riot broke out", x, y);
        callRiotPolice(x, y);
        return true;
    }

    void rioter(Entity e, float x, float y, float until) {
        e.riotUntil = until - w.rnd.nextFloat() * 15;
        e.riotCd = w.rnd.nextFloat();
        e.riotX = x;
        e.riotY = y;
        e.collar = null;
        e.fleeTimer = 0;
        e.building = null;
        if (e.task == Dispatch.T_HIDE) e.task = Dispatch.T_NONE;
    }

    /** ((25.0) Six riot officers to the riot: those about town first, the rest from the station. */
    void callRiotPolice(float x, float y) {
        w.riotSquad.clear();
        for (int pass = 0; pass < 2 && w.riotSquad.size() < 6; pass++)
            for (int i = 0, n = w.entities.size(); i < n && w.riotSquad.size() < 6; i++) {
                Entity e = w.entities.get(i);
                if (e.dead || e.type != Entity.COP || e == w.controlled || (pass == 0) != (e.role == Entity.ROLE_RIOT)) continue;
                if (e.task != Dispatch.T_NONE && e.task != Dispatch.T_PATROL) continue;
                if (World.hypot(e.x - x, e.y - y) > (pass == 0 ? 1500 : 450)) continue;
                w.riotSquad.add(e);
            }
        float[] at = Campaign.arrival(w, true, x, y);
        for (int k = w.riotSquad.size(); k < 6 && at != null; k++) {
            Entity e = w.population.spawnCop(Entity.ROLE_RIOT, at[0] + (k % 3) * 6, at[1] + (k / 3) * 6);
            if (e == null) break;
            e.outsider = true;
            w.riotSquad.add(e);
        }
        if (w.riotSquad.isEmpty()) {
            w.dispatch.say(Dispatch.WHO_POLICE, null, "Dispatch: No units free for the riot on " + w.city.placeName(x, y) + ".", x, y);
            return;
        }
        w.dispatch.order(w.riotSquad, x, y);
        w.dispatch.say(Dispatch.WHO_POLICE, w.riotSquad.get(0), "Riot squad responding to " + w.city.placeName(x, y) + ". Shields up, batons out.", x, y);
    }

    /** ((25.0) In the riot: at the police lines, looting the shops, surging through the street. False to get on as usual. */
    boolean riotStep(Entity e, float dt) {
        if (w.riot == null || w.time > e.riotUntil) {
            e.riotUntil = 0;
            e.collar = null;
            return false;
        }
        // (The dead turning up end it for them: they run like anyone.)
        if (w.city.fieldAt(w.city.zombieDist, e.x, e.y) <= 100 / City.T + 6 && w.spatial.nearest(e, 80, true, false) != null) {
            e.riotUntil = 0;
            e.collar = null;
            return false;
        }
        e.riotCd -= dt;
        if (e.riotCd <= 0) {
            e.riotCd = 3 + w.rnd.nextFloat() * 3;
            float roll = w.rnd.nextFloat();
            Entity cop = roll < 0.4f ? w.spatial.nearestPolice(e.x, e.y, 110) : null;
            e.collar = cop;
            if (cop == null) {
                City.Building shop = roll < 0.75f ? riotShop(w.riot[0], w.riot[1]) : null;
                if (shop != null) {
                    e.riotX = shop.doorX + w.rnd.nextFloat() * 16 - 8;
                    e.riotY = shop.doorY + w.rnd.nextFloat() * 16 - 8;
                } else {
                    e.riotX = w.riot[0] + w.rnd.nextFloat() * 110 - 55;
                    e.riotY = w.riot[1] + w.rnd.nextFloat() * 110 - 55;
                }
            }
            if (w.rnd.nextFloat() < 0.12f) w.emit(Sfx.SCREAM, e.x, e.y);
        }
        Entity c = e.collar;
        if (c != null && !c.dead) {
            float dx = c.x - e.x, dy = c.y - e.y, d = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
            if (d > e.radius + c.radius + 3) w.wilds.walkToSpot(e, c.x, c.y, e.runSpeed * 0.8f);
            else {
                w.movement.steer(e, 0, 0, 0);
                e.angle = (float) Math.atan2(dy, dx);
                if (e.meleeCd <= 0) {
                    // A blow at the line: a shield takes it; an officer without one is roughed up (never badly).
                    e.meleeCd = 1.4f + w.rnd.nextFloat();
                    e.swingAt = w.time;
                    boolean shield = c.role == Entity.ROLE_RIOT && (float) Math.cos(c.angle) * -dx / d + (float) Math.sin(c.angle) * -dy / d > 0.2f;
                    if (shield) w.emit(Sfx.SHIELD, c.x, c.y);
                    else {
                        if (c.hp > c.maxHp * 0.6f) c.hp -= 2;
                        c.hurt = 0.5f;
                        w.emit(Sfx.THUD, c.x, c.y);
                    }
                }
            }
            return true;
        }
        e.collar = null;
        w.wilds.walkToSpot(e, e.riotX, e.riotY, e.speed * 1.6f);
        return true;
    }

    /** ((25.0) A shop near the riot, not yet looted (or any shop near it), or null. */
    City.Building riotShop(float x, float y) {
        City.Building best = null;
        float bd = 170 * 170;
        for (int i = 0, n = w.city.buildings.size(); i < n; i++) {
            City.Building b = w.city.buildings.get(i);
            if (!World.isShop(b) || b.collapsed) continue;
            float d = (b.doorX - x) * (b.doorX - x) + (b.doorY - y) * (b.doorY - y) + (b.smashed ? 60 * 60 : 0);
            if (d < bd) {
                bd = d;
                best = b;
            }
        }
        return best;
    }

    /** ((25.0) Every step: the wrecks burning; once a second, the riot and the escapees. */
    void eventsStep(float dt) {
        for (int i = 0, n = w.crashes.size(); i < n; i++) {
            float[] c = w.crashes.get(i);
            c[3] += dt;
            if (c[3] > World.CRASH_SMOKE) continue;
            float ca = (float) Math.cos(c[2]), sa = (float) Math.sin(c[2]);
            float along = w.rnd.nextFloat() * 60 - 20, fx = c[0] - ca * along + (w.rnd.nextFloat() - 0.5f) * 14, fy = c[1] - sa * along + (w.rnd.nextFloat() - 0.5f) * 14;
            if (c[3] < World.CRASH_BURN && w.rnd.nextFloat() < dt * 30)
                w.effects.particle(fx, fy, w.rnd.nextFloat() * 10 - 5, -8 - w.rnd.nextFloat() * 12, 0.5f + w.rnd.nextFloat() * 0.5f, 2 + w.rnd.nextFloat() * 3,
                        w.rnd.nextBoolean() ? 0xFFFFB030 : 0xFFFF6A1A, World.P_FIRE);
            if (w.rnd.nextFloat() < dt * (c[3] < World.CRASH_BURN ? 10 : 3))
                w.effects.particle(fx, fy, w.rnd.nextFloat() * 8 - 4, -6 - w.rnd.nextFloat() * 8, 2 + w.rnd.nextFloat() * 1.5f, 5 + w.rnd.nextFloat() * 5, 0xFF3A3A3A, World.P_SMOKE);
        }
        if ((w.eventsTick -= dt) > 0) return;
        w.eventsTick = 1;
        // The fire burns anyone who stands in it.
        for (int i = 0, n = w.crashes.size(); i < n; i++) {
            float[] c = w.crashes.get(i);
            if (c[3] >= World.CRASH_BURN) continue;
            for (int k = 0, m = w.entities.size(); k < m; k++) {
                Entity e = w.entities.get(k);
                if (e.dead || Math.abs(e.x - c[0]) > 30 || Math.abs(e.y - c[1]) > 30) continue;
                if ((e.x - c[0]) * (e.x - c[0]) + (e.y - c[1]) * (e.y - c[1]) > 18 * 18) continue;
                e.hp -= 6;
                e.hurt = 1;
                e.killedByZombie = false;
            }
        }
        if (w.riot != null) riotTick();
        if (w.escaped > w.recaptured) roundUp();
    }

    /** ((25.0) Once a second in a riot: the looting, the cars set alight, the police lines; and the end of it. */
    void riotTick() {
        if (w.time > w.riot[2]) {
            endRiot();
            return;
        }
        int rioters = 0;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || e.riotUntil <= w.time) continue;
            rioters++;
            // A shop window put in, and the place stripped.
            if (w.rnd.nextFloat() < 0.5f) {
                City.Building b = riotShop(e.x, e.y);
                if (b != null && !b.smashed && World.hypot(b.doorX - e.x, b.doorY - e.y) < 20) {
                    w.indoors.smash(b);
                    if (b.stock > 0) b.stock = Math.max(0, b.stock - 60);
                    if (w.riotLoots++ == 0)
                        w.dispatch.say(Dispatch.WHO_911, null, "\"They're looting " + (b.name != null ? b.name : "the shops") + " on " + w.city.placeName(b.doorX, b.doorY) + "!\"", b.doorX, b.doorY);
                }
            }
            // A car or two set alight.
            if (w.riotCars < 2 && w.rnd.nextFloat() < 0.04f && burnCarNear(e.x, e.y)) {
                w.riotCars++;
                if (w.riotCars == 1) w.dispatch.say(Dispatch.WHO_911, null, "\"They've set a car on fire on " + w.city.placeName(e.x, e.y) + "!\"", e.x, e.y);
            }
            // At the police lines: shoved back by the shields, and some taken away or sent home.
            Entity cop = w.spatial.nearestPolice(e.x, e.y, e.radius + 9);
            if (cop != null) {
                float dx = e.x - cop.x, dy = e.y - cop.y, d = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
                w.movement.tryMove(e, dx / d * 7, dy / d * 7);
                e.stun = Math.max(e.stun, 0.8f);
                cop.swingAt = w.time;
                cop.angle = (float) Math.atan2(-dy, -dx);
                w.emit(Sfx.THUD, e.x, e.y);
                float roll = w.rnd.nextFloat();
                if (roll < 0.18f) {
                    e.riotUntil = 0;
                    e.dead = true;
                    e.removed = true;
                    w.riotHeld++;
                } else if (roll < 0.4f) {
                    e.riotUntil = 0;
                    e.collar = null;
                    e.fleeTimer = 4;
                    e.threatX = cop.x;
                    e.threatY = cop.y;
                }
            }
        }
        if (rioters == 0) endRiot();
    }

    /** ((25.0) A car near (x, y) set alight: a parked or abandoned one in the street, or one parked at the kerb. */
    boolean burnCarNear(float x, float y) {
        for (int i = 0, n = w.fleet.vehicles.size(); i < n; i++) {
            Fleet.Vehicle v = w.fleet.vehicles.get(i);
            if (v.type != Fleet.CAR || v.burnt || v.driver != null || !v.riders.isEmpty() || !(v.parked || v.broken || v.speed < 1)) continue;
            if (Math.abs(v.x - x) > 22 || Math.abs(v.y - y) > 22) continue;
            w.fleet.damage(v, v.maxHp * 1.6f, false);
            v.burnt = true;
            for (int k = 0; k < 12; k++)
                w.effects.particle(v.x, v.y, w.rnd.nextFloat() * 20 - 10, -10 - w.rnd.nextFloat() * 10, 0.6f, 2.5f, 0xFFFF8A2A, World.P_FIRE);
            return true;
        }
        int tx0 = (int) (x / City.T), ty0 = (int) (y / City.T);
        for (int ty = Math.max(0, ty0 - 1); ty <= Math.min(w.city.h - 1, ty0 + 1); ty++)
            for (int tx = Math.max(0, tx0 - 1); tx <= Math.min(w.city.w - 1, tx0 + 1); tx++) {
                int i = ty * w.city.w + tx;
                if (w.burned[i] || !w.city.burnParkedCar(i)) continue;
                w.burnTile(i);
                for (int k = 0; k < 12; k++)
                    w.effects.particle((tx + 0.5f) * City.T, (ty + 0.5f) * City.T, w.rnd.nextFloat() * 20 - 10, -10 - w.rnd.nextFloat() * 10, 0.6f, 2.5f, 0xFFFF8A2A, World.P_FIRE);
                return true;
            }
        return false;
    }

    /** ((25.0) The riot's over: everyone goes home, the riot squad back to its rounds. */
    void endRiot() {
        float x = w.riot[0], y = w.riot[1];
        w.riot = null;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.riotUntil <= 0) continue;
            e.riotUntil = 0;
            e.collar = null;
        }
        ArrayList<Entity> back = new ArrayList<Entity>();
        for (int i = 0; i < w.riotSquad.size(); i++) if (!w.riotSquad.get(i).dead) back.add(w.riotSquad.get(i));
        w.riotSquad.clear();
        if (!back.isEmpty()) w.dispatch.dismiss(back);
        w.dispatch.say(Dispatch.WHO_POLICE, null, "Police Command: The riot on " + w.city.placeName(x, y) + " has been dispersed."
                + (w.riotHeld > 0 ? " " + w.riotHeld + " arrested." : ""), x, y);
        w.stats.timeline("The riot was dispersed", x, y);
    }

    /** ((25.0) Once a second while inmates are loose: free officers nearby set off after them, one officer to each. */
    void roundUp() {
        java.util.HashSet<Entity> taken = new java.util.HashSet<Entity>();
        ArrayList<Entity> loose = new ArrayList<Entity>();
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.dead) continue;
            if (e.escapee) loose.add(e);
            else if (e.collar != null && e.type == Entity.COP) taken.add(e.collar);
        }
        if (loose.isEmpty()) {
            w.escaped = w.recaptured;
            return;
        }
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || e.type != Entity.COP || e.collar != null || e == w.controlled || e.role == Entity.ROLE_PRISON_GUARD || e.ride != null || e.rig != null) continue;
            if (e.task != Dispatch.T_NONE && e.task != Dispatch.T_PATROL) continue;
            Entity best = null;
            float bd = 500 * 500;
            for (int k = 0; k < loose.size(); k++) {
                Entity s = loose.get(k);
                if (taken.contains(s)) continue;
                float d = (s.x - e.x) * (s.x - e.x) + (s.y - e.y) * (s.y - e.y);
                if (d < bd) {
                    bd = d;
                    best = s;
                }
            }
            if (best == null) continue;
            e.collar = best;
            taken.add(best);
        }
    }

    /** A jet flies over and drops a line of bombs across (x, y). */
    void airstrike(float x, float y) {
        w.fleet.sendJet(x, y);
        w.dispatch.say(Dispatch.WHO_MILITARY, null, "Military: Airstrike inbound on " + w.city.placeName(x, y)
                + ". Everyone clear the area!", x, y);
    }

    /** The emergency sirens: everyone out on the street heads indoors or for a safe zone. */
    void cityAlarm() {
        if (w.blackout) {
            w.say("No power: the sirens are dead");
            return;
        }
        w.emit(Sfx.ALARM, w.city.worldW() / 2, w.city.worldH() / 2);
        w.alarmTime = 6;
        int sent = 0;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.type != Entity.CIVILIAN || e.task != Dispatch.T_NONE) continue;
            // ((12.4) Indoors only when the dead aren't about: in an outbreak, a safe zone or nothing.)
            City.Building b = w.indoors.apocalypse() ? null : w.civilianAI.shelterWithin(e, 250);
            if (b != null) {
                e.task = Dispatch.T_HIDE;
                e.building = b;
                sent++;
            } else if (w.dispatch.hasRoom()) {
                e.task = Dispatch.T_SEEK;
                sent++;
            }
        }
        w.dispatch.say(Dispatch.WHO_INFO, null, w.indoors.apocalypse() ? "City Hall: Emergency sirens are sounding. Get to a safe zone now!"
                        : "City Hall: Emergency sirens are sounding. Get indoors now!",
                w.city.worldW() / 2, w.city.worldH() / 2);
    }
}
