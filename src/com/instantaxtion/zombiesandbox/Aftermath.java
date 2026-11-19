package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * After the war: calming down, recovery and rebuilding, restocking, the city coming back to life - or fallen.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class Aftermath {
    private final World w;

    Aftermath(World w) {
        this.w = w;
    }

    /** Once a second: in an outbreak with people sheltering, every few minutes a plane drops supplies over the busiest zone. */
    void cargoFlights() {
        if (w.cargoPlane != null || !w.outbreak || w.zombies == 0 || w.dispatch.zones.isEmpty()) return;
        if ((w.cargoCd -= 1) > 0) return;
        Dispatch.SafeZone best = null;
        for (Dispatch.SafeZone z : w.dispatch.zones) if (best == null || z.sheltered > best.sheltered) best = z;
        if (best == null || best.sheltered < 5) {
            w.cargoCd = 30;
            return;
        }
        w.cargoCd = 200 + w.rnd.nextFloat() * 90;
        double a = w.rnd.nextDouble() * Math.PI * 2;
        float dx = (float) Math.cos(a), dy = (float) Math.sin(a), far = w.city.worldW() + w.city.worldH();
        w.cargoPlane = new float[]{best.x - dx * far * 0.5f, best.y - dy * far * 0.5f, dx, dy, 3, 0};
        w.cargoTarget = new float[]{best.x, best.y};
        w.dispatch.say(Dispatch.WHO_MILITARY, null, "Air Force: Cargo plane inbound, dropping supplies over " + w.city.placeName(best.x, best.y) + ".", best.x, best.y);
    }

    /** The plane flies on; over its zone, it drops its crates under their parachutes. */
    void moveCargoPlane(float dt) {
        float[] p = w.cargoPlane;
        p[0] += p[2] * 260 * dt;
        p[1] += p[3] * 260 * dt;
        p[5] -= dt;
        float along = (p[0] - w.cargoTarget[0]) * p[2] + (p[1] - w.cargoTarget[1]) * p[3];
        if (p[4] > 0 && along > -90 && p[5] <= 0) {
            float ox = (w.rnd.nextFloat() - 0.5f) * 50, oy = (w.rnd.nextFloat() - 0.5f) * 50;
            if (p[4] == 3) w.sandbox.supplyDrop(p[0] + ox, p[1] + oy);
            else w.sandbox.placeCrate(p[0] + ox, p[1] + oy, 3.5f);
            p[4]--;
            p[5] = 0.6f;
        }
        float m = 600;
        if (along > 0 && (p[0] < -m || p[1] < -m || p[0] > w.city.worldW() + m || p[1] > w.city.worldH() + m)) w.cargoPlane = null;
    }

    void cityLife() {
        w.war.keyBuildings();
        w.experience.veterans();
        w.experience.bands();
        cargoFlights();
        w.civilianAI.countHeading();
        w.baseLife.updateBases();
        if (w.city.prison != null) {
            w.prison.prisonLife();
        }
        if (w.city.apron != null) w.exodus.airportLife();
        w.recruiting.reservePilots();
        if (((int) w.time) % 5 == 0) w.fleet.stationHelis();
        w.war.government();
        w.war.updateAlert();
        w.war.callNationalGuard();
        w.war.escalate();
        w.war.leadersDecide(1);
        w.war.supplies();
        w.war.fieldSupplies();
        w.raiders.raiders();
        if (World.WALK_IN) w.indoors.countInside();
        w.director.tick(w);
        w.armedAI.mopUp();
        w.fieldworks.engineersWire();
        w.fieldworks.engineersFortify();
        w.armedAI.sitrep();
        w.armedAI.unstick();
        for (int i = w.incoming.size() - 1; i >= 0; i--) {
            Object[] h = w.incoming.get(i);
            float left = (Float) h[1] - 1;
            h[1] = left;
            if (left <= 0 || ((Entity) h[0]).dead) w.incoming.remove(i);
        }
        w.armedAI.fieldHospital();
        w.war.militias();
        if (w.outbreak) w.war.updateWar();
        int z = w.zombieCount();
        if (z > 0) {
            w.outbreakTime += 1;
            w.calmTime = 0;
            w.restockSent = 0;
            w.restockSaid = false;
            if (w.recovering) {
                w.recovering = false;
                w.dispatch.say(Dispatch.WHO_INFO, null, "The infected are back. Recovery efforts suspended.",
                        w.city.worldW() / 2, w.city.worldH() / 2);
            }
            int stage = Math.min(5, (int) (w.outbreakTime / 180));
            if (stage > w.mutation) {
                w.mutation = stage;
                String[] lines = {"", "New infections are turning faster and hitting harder.",
                        "Doctors say the virus is changing. The newly infected are tougher.",
                        "The infected are getting stronger and quicker.",
                        "Reports of infected shrugging off gunshots.",
                        "The virus has fully evolved. Expect the worst."};
                w.dispatch.say(Dispatch.WHO_INFO, null, "Health officials: " + lines[stage] + " (stage " + stage + ")",
                        w.city.worldW() / 2, w.city.worldH() / 2);
            }
            w.civilianAI.survivorPlans();
            return;
        }
        if (w.peakZombies < 5 || w.humans == 0) return;
        w.calmTime += 1;
        if (!w.recovering && w.calmTime > 30) {
            w.recovering = true;
            w.dispatch.say(Dispatch.WHO_INFO, null, "No infected seen for a while. The city is starting to recover.",
                    w.city.worldW() / 2, w.city.worldH() / 2);
        }
        if (w.recovering) recover();
    }

    /** Clean-up crews, repairs, wrecks towed away, safe zones closing and people going home. */
    void recover() {
        restockTown();
        // Volunteers clear away the bodies.
        int crews = 0;
        for (int i = 0, n = w.entities.size(); i < n; i++) if (w.entities.get(i).task == Dispatch.T_CLEANUP) crews++;
        for (int i = 0, n = w.entities.size(); i < n && crews < 8; i++) {
            Entity e = w.entities.get(i);
            if (e.type != Entity.CIVILIAN || e.task != Dispatch.T_NONE || e.leader != null || w.rnd.nextFloat() > 0.2f) continue;
            World.Corpse c = freshCorpseAny(e.x, e.y, 400);
            if (c == null) break;
            e.task = Dispatch.T_CLEANUP;
            e.threatX = c.x;
            e.threatY = c.y;
            e.postX = w.time;
            e.taskTimer = 0;
            crews++;
        }
        // Blood fades, buildings are patched up, wrecks towed away.
        for (int i = 0; i < w.dcount; i++) {
            int a = (w.dcol[i] >>> 24) & 0xFF;
            if (a > 0 && w.dkind[i] == World.D_SPLAT && (w.dcol[i] & 0xFF0000) > 0x500000) w.dcol[i] = (Math.max(0, a - 4) << 24) | (w.dcol[i] & 0xFFFFFF);
        }
        for (int i = 0, n = w.city.buildings.size(); i < n; i++) {
            City.Building b = w.city.buildings.get(i);
            if (b.collapsed) continue;
            b.hp = Math.min(b.maxHp, b.hp + 3);
            if (b.markCount > 0 && w.rnd.nextFloat() < 0.05f) b.markCount--;
        }
        if (w.rnd.nextFloat() < 0.07f)
            for (int i = 0; i < w.fleet.vehicles.size(); i++)
                if (w.fleet.vehicles.get(i).broken && w.fleet.vehicles.get(i).riders.isEmpty()) {
                    w.fleet.discard(w.fleet.vehicles.get(i));
                    break;
                }
        // After a while the safe zones close and everyone goes home.
        if (w.calmTime > 120 && !w.dispatch.zones.isEmpty() && w.rnd.nextFloat() < 0.04f) w.dispatch.closeZone(w.dispatch.zones.get(0));
    }

    /**
     * Once it's over, trucks come in from outside to refill what the outbreak used up: ammo for the police
     * and the army, guns and ammo for the gun stores, food for the supermarkets and shops. One every few seconds,
     * worst-off first.
     */
    void restockTown() {
        if (w.restockSent >= World.RESTOCK_TRUCKS || ((int) w.time) % 4 != 0) return;
        Object best = null;
        float worst = 0.75f;
        for (City.Facility f : w.city.facilities) {
            int full = f.kind == City.FACILITY_POLICE ? World.ARMOURY_POLICE : f.kind == City.FACILITY_BASE ? World.ARMOURY_BASE : 0;
            if (full == 0 || restockBound(f)) continue;
            float left = f.ammo / (float) full;
            if (left < worst) {
                worst = left;
                best = f;
            }
        }
        for (int i = 0, n = w.city.buildings.size(); i < n; i++) {
            City.Building b = w.city.buildings.get(i);
            if (b.collapsed) continue;
            float left;
            if (b.kind == City.MARKET) left = Math.min(b.stock / 240f, b.food / 400f);
            else if (b.kind == City.SHOP) left = b.food / (b.shopType == 2 ? 120f : 40f) + 0.2f;
            else continue;
            if (left < worst && !restockBound(b)) {
                worst = left;
                best = b;
            }
        }
        if (best == null) return;
        City.Building b = best instanceof City.Building ? (City.Building) best : null;
        City.Facility f = b == null ? (City.Facility) best : null;
        float tx = b != null ? b.doorX : f.x, ty = b != null ? b.doorY : f.y;
        java.util.List<float[]> from = w.dispatch.entries(tx, ty);
        if (from.isEmpty()) return;
        float[] p = from.get(0);
        w.restockSent++;
        if (!w.fleet.sendRestock(p[0], p[1], b, f)) return;
        if (!w.restockSaid) {
            w.restockSaid = true;
            w.dispatch.say(Dispatch.WHO_INFO, null, "News: Supply trucks are on their way in from " + w.dispatch.sideOfMap(p[0], p[1])
                    + " to restock the stores, the gun shops and the armouries.", p[0], p[1]);
        }
    }

    /** Whether a supply truck is already on its way to this store or armoury. */
    boolean restockBound(Object o) {
        for (int i = 0; i < w.fleet.vehicles.size(); i++) {
            Fleet.Vehicle v = w.fleet.vehicles.get(i);
            if (v.restock && (v.restockShop == o || v.restockFac == o)) return true;
        }
        return false;
    }

    /** A supply truck has pulled up: the shelves and the armoury are full again. */
    void restocked(Fleet.Vehicle v) {
        City.Building b = v.restockShop;
        City.Facility f = v.restockFac;
        String what;
        if (f != null) {
            f.ammo = Math.max(f.ammo, f.kind == City.FACILITY_POLICE ? World.ARMOURY_POLICE : World.ARMOURY_BASE);
            what = "ammunition";
        } else if (b != null && !b.collapsed) {
            if (b.kind == City.MARKET) {
                b.stock = Math.max(b.stock, 240);
                b.food = Math.max(b.food, 400);
                what = "food";
            } else {
                b.food = Math.max(b.food, b.shopType == 2 ? 120 : 40);
                what = "food";
            }
            b.smashed = false;
            b.looted = false;
        } else return;
        w.stats.highlight("Restocked", v.x, v.y);
        if (w.rnd.nextFloat() < 0.4f)
            w.dispatch.say(Dispatch.WHO_INFO, null, "Supply truck: " + what + " delivered to " + v.place + ".", v.x, v.y);
    }

    World.Corpse freshCorpseAny(float x, float y, float radius) {
        World.Corpse best = null;
        float bd = radius * radius;
        for (int i = 0, n = w.corpses.size(); i < n; i++) {
            World.Corpse c = w.corpses.get(i);
            if (c.rise > 0 || c.age < 0) continue;
            float d = (c.x - x) * (c.x - x) + (c.y - y) * (c.y - y);
            if (d < bd) {
                bd = d;
                best = c;
            }
        }
        if (best != null) best.age = -1000; // claimed
        return best;
    }

    /** Clean-up crew: walk to a body, spend a moment with it, and it's gone. */
    boolean cleanup(Entity e, Entity threat) {
        if (e.task != Dispatch.T_CLEANUP) return false;
        if (threat != null) {
            e.task = Dispatch.T_NONE;
            return false;
        }
        float ddx = e.threatX - e.x, ddy = e.threatY - e.y;
        float d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
        // Can't get to it: someone else will deal with it later.
        boolean giveUp = w.time - e.postX > 45;
        if (d > 6 && !giveUp) {
            e.taskTimer = 0;
            if (!e.blocked) w.movement.steer(e, ddx / d, ddy / d, e.speed);
            else {
                e.blocked = false;
                w.movement.wander(e, e.speed);
            }
            return true;
        }
        w.movement.steer(e, 0, 0, 0);
        if (e.taskTimer < -3 || giveUp) {
            for (int i = w.corpses.size() - 1; i >= 0; i--) {
                World.Corpse c = w.corpses.get(i);
                if (Math.abs(c.x - e.threatX) < 1 && Math.abs(c.y - e.threatY) < 1) w.corpses.remove(i);
            }
            for (int i = 0; i < w.dcount; i++)
                if ((w.dx[i] - e.x) * (w.dx[i] - e.x) + (w.dy[i] - e.y) * (w.dy[i] - e.y) < 20 * 20 && w.dkind[i] == World.D_SPLAT) w.dr[i] = 0;
            e.task = Dispatch.T_NONE;
        }
        return true;
    }

    /**
     * (10.60) The outbreak's over: nobody keeps running from what isn't there. Those out in the streets stop fleeing,
     * and anyone on their way to hide somewhere goes back to their day (those already inside come out when it's been
     * quiet a while, as ever).
     */
    void calmDown() {
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || e.type != Entity.CIVILIAN) continue;
            e.fear = 0;
            e.fleeTimer = 0;
            e.goalCd = 0;
            if (e.task == Dispatch.T_HIDE || e.task == Dispatch.T_SEEK) w.civilianAI.dropTask(e);
            // (Out in a street that's quiet now: back to their day, not home to hide.)
            if (e.building == null) {
                e.aware = false;
                e.emerged = w.time;
            }
        }
    }

    void startAftermath(int kind) {
        w.aftermath = kind;
        w.rebuildCd = 120;
        w.afterTime = 0;
        w.afterTick = 0;
        w.flareCd = 150 + w.rnd.nextFloat() * 150;
        w.repaired = w.cleared = w.towed = 0;
        w.afterSaid = 0;
    }

    void updateAftermath(float dt) {
        if (w.aftermath == World.AFTER_NONE) return;
        w.afterTime += dt;
        w.afterTick -= dt;
        if (w.afterTick > 0) return;
        w.afterTick = 1;
        if (w.aftermath == World.AFTER_RECOVERY) recovery();
        else fallen();
    }

    void recovery() {
        recoveryStep((int) w.afterTime);
        // (The public works crews make it go twice as fast.)
        if (w.city.worksDepot != null && !w.city.worksDepot.collapsed) {
            if (w.afterTime > 20 && (w.afterSaid & 64) == 0) {
                w.afterSaid |= 64;
                w.dispatch.say(Dispatch.WHO_INFO, null, "News: " + w.city.worksDepot.name + " has every truck out clearing the streets.",
                        w.city.worksDepot.doorX, w.city.worksDepot.doorY);
            }
            recoveryStep((int) w.afterTime + 1);
        }
        recoveryRest();
    }

    /** The clean-up work done this second (bodies, glass, wrecks and repairs). */
    void recoveryStep(int t) {
        if (t >= 20 && (w.afterSaid & 1) == 0) {
            w.afterSaid |= 1;
            w.banner("RECOVERY", "Survivors are coming out. Clean-up crews are on the streets.");
        }
        // Bodies collected (the ones that won't get up).
        if (t % 3 == 0)
            for (int i = 0; i < w.corpses.size(); i++)
                if (w.corpses.get(i).rise < 0) {
                    w.corpses.remove(i);
                    w.cleared++;
                    break;
                }
        // Blood and glass washed away, a little at a time.
        for (int k = 0; k < 4 && w.dcount > 0; k++) {
            int i = w.rnd.nextInt(w.dcount);
            if (w.dr[i] > 0 && w.dkind[i] != World.D_SKID) w.dr[i] = 0;
        }
        // Wrecks towed (never in front of you).
        if (t % 10 == 0) {
            for (int i = w.fleet.vehicles.size() - 1; i >= 0; i--) {
                Fleet.Vehicle v = w.fleet.vehicles.get(i);
                if ((v.broken || v.burnt) && v.riders.isEmpty() && v.player == null && !w.fleet.inView(v.x, v.y)) {
                    w.fleet.discard(v);
                    w.towed++;
                    break;
                }
            }
        }
        // Repairs: damaged buildings patched up, smashed shops boarded and reopened.
        if (t % 8 == 0) {
            for (int i = 0, n = w.city.buildings.size(); i < n; i++) {
                City.Building b = w.city.buildings.get(i);
                if (b.collapsed) continue;
                if (b.hp < b.maxHp || (b.smashed && w.afterTime > 60)) {
                    b.hp = Math.min(b.maxHp, b.hp + b.maxHp * 0.34f);
                    if (w.afterTime > 60 && b.smashed) {
                        b.smashed = false;
                        b.looted = false;
                        if (b.kind == City.MARKET || b.kind == City.SHOP) b.stock = Math.max(b.stock, 40);
                        b.food = Math.max(b.food, 30);
                    }
                    if (b.hp >= b.maxHp) b.markCount = 0;
                    w.repaired++;
                    break;
                }
            }
        }
    }

    void rebuilding() {
        City.Building site = null;
        for (int i = 0, n = w.city.buildings.size(); i < n; i++) {
            City.Building b = w.city.buildings.get(i);
            if (b.rebuildT <= 0) continue;
            site = b;
            b.rebuildT -= 1;
            if (b.rebuildT > 0 || !b.collapsed) break;
            if (peopleOn(b)) {
                // ((18.3) Someone standing there holds the work up; a stream of people just passing through only for a
                // while: then the crew shoos them off and builds.)
                java.util.HashSet<Entity> now = new java.util.HashSet<Entity>();
                for (int k = 0, m = w.spatial.gatherLiving((b.x0 + b.x1) / 2, (b.y0 + b.y1) / 2, Math.max(b.x1 - b.x0, b.y1 - b.y0)); k < m; k++) {
                    Entity e = w.gathered[k];
                    if (!e.dead && e.x > b.x0 - e.radius && e.x < b.x1 + e.radius && e.y > b.y0 - e.radius && e.y < b.y1 + e.radius) now.add(e);
                }
                boolean standing = false;
                for (Entity e : now) standing |= w.siteSeen.contains(e);
                w.siteSeen.clear();
                w.siteSeen.addAll(now);
                w.siteWait = standing ? 0 : w.siteWait + 1;
                if (standing || w.siteWait < 10 || now.isEmpty()) {
                    b.rebuildT = 2;
                    break;
                }
                for (Entity e : now) {
                    float[] q = w.city.findWalkable(b.doorX, b.doorY + 12);
                    if (q != null) {
                        e.x = q[0];
                        e.y = q[1];
                    }
                }
                if (peopleOn(b)) {
                    b.rebuildT = 2;
                    break;
                }
            }
            w.siteWait = 0;
            w.siteSeen.clear();
            if (w.city.rebuild(b)) {
                w.rebuilt++;
                // (Any bodies left in the rubble taken away first.)
                for (int k = w.corpses.size() - 1; k >= 0; k--) {
                    World.Corpse c = w.corpses.get(k);
                    if (c.x > b.x0 && c.x < b.x1 && c.y > b.y0 && c.y < b.y1) w.corpses.remove(k);
                }
                w.collapsedCount = Math.max(0, w.collapsedCount - 1);
                if (b.name != null) w.dispatch.say(Dispatch.WHO_INFO, null, "News: " + b.name + " has been rebuilt.", b.doorX, b.doorY);
            }
            break;
        }
        if (site != null || (w.rebuildCd -= 1) > 0) return;
        w.rebuildCd = World.REBUILD_GAP;
        for (int i = 0, n = w.city.buildings.size(); i < n; i++) {
            City.Building b = w.city.buildings.get(i);
            if (!b.collapsed) continue;
            b.rebuildT = World.REBUILD_TIME;
            if (w.rebuilt == 0) w.dispatch.say(Dispatch.WHO_INFO, null, "News: Work has started on rebuilding what fell, beginning on "
                    + w.city.placeName(b.doorX, b.doorY) + ".", b.doorX, b.doorY);
            return;
        }
    }

    /** Anyone (or any vehicle) on a building's footprint. */
    boolean peopleOn(City.Building b) {
        for (int i = 0, n = w.spatial.gatherLiving((b.x0 + b.x1) / 2, (b.y0 + b.y1) / 2, Math.max(b.x1 - b.x0, b.y1 - b.y0)); i < n; i++) {
            Entity e = w.gathered[i];
            if (!e.dead && e.x > b.x0 - e.radius && e.x < b.x1 + e.radius && e.y > b.y0 - e.radius && e.y < b.y1 + e.radius) return true;
        }
        for (int i = 0, n = w.fleet.vehicles.size(); i < n; i++) {
            Fleet.Vehicle v = w.fleet.vehicles.get(i);
            if (!v.removedFromFleet && v.x > b.x0 - 10 && v.x < b.x1 + 10 && v.y > b.y0 - 10 && v.y < b.y1 + 10) return true;
        }
        return false;
    }

    void recoveryRest() {
        int t = (int) w.afterTime;
        rebuilding();
        // Survivor groups take their barricades down and go home.
        if (w.afterTime > 90)
            for (int i = 0; i < w.holdouts.size(); i++) {
                World.Holdout h = w.holdouts.get(i);
                h.fort = Math.max(0, h.fort - 1.5f);
                if (h.fort <= 0) {
                    w.dispatch.say(Dispatch.WHO_INFO, null, "News: " + World.cap(h.name) + " have taken down their barricades and gone home.", h.x, h.y);
                    disband(h);
                    break;
                }
            }
        if (t >= 45 && (w.afterSaid & 2) == 0 && (w.afterSaid |= 2) != 0)
            w.dispatch.say(Dispatch.WHO_INFO, null, "News: Clean-up crews are out across the city. Shops are boarding up their windows and getting ready to reopen.",
                    w.city.worldW() / 2, w.city.worldH() / 2);
        // But it isn't always over. (Twice at most.)
        w.flareCd -= 1;
        if (w.flareCd <= 0 && w.flareUps < 2) {
            w.flareCd = 150 + w.rnd.nextFloat() * 200;
            if (w.rnd.nextFloat() < 0.45f) {
                w.flareUps++;
                float[] edge = w.city.edgeRoad(w.rnd);
                if (edge != null) {
                    int n = 5 + w.rnd.nextInt(8);
                    Entity lead = null;
                    for (int k = 0; k < n; k++) {
                        Entity z = w.population.spawn(w.death.turnType(Entity.CIVILIAN), edge[0] + w.rnd.nextFloat() * 30 - 15, edge[1] + w.rnd.nextFloat() * 30 - 15);
                        if (z == null) continue;
                        if (lead == null) {
                            lead = z;
                            z.leadsHorde = true;
                            z.roamX = w.city.worldW() / 2;
                            z.roamY = w.city.worldH() / 2;
                        } else w.zombieAI.join(z, lead);
                    }
                    w.dispatch.say(Dispatch.WHO_INFO, null, "News: Reports of the dead coming in from " + w.city.placeName(edge[0], edge[1])
                            + ". Residents, get clear!", edge[0], edge[1]);
                }
            }
        }
    }

    void fallen() {
        int t = (int) w.afterTime;
        if (t >= 15 && (w.afterSaid & 4) == 0 && (w.afterSaid |= 4) != 0)
            w.dispatch.say(Dispatch.WHO_INFO, null, "News: The city has fallen. If you're still out there, stay hidden and stay quiet.",
                    w.city.worldW() / 2, w.city.worldH() / 2);
        boolean army = w.city.mainBase() != null;
        if (!army || w.retakeSent) return;
        if (t >= 100 && !w.retakeWarned) {
            w.retakeWarned = true;
            w.dispatch.say(Dispatch.WHO_MILITARY, null, "Military: We're putting together a force to take the city back. Hold on.",
                    w.city.worldW() / 2, w.city.worldH() / 2);
        }
        if (t >= 160) {
            w.retakeSent = true;
            // One last force (there's no other): three trucks from the edge of town, for the heart of it.
            float tx = w.city.worldW() / 2, ty = w.city.worldH() / 2;
            if (!w.holdouts.isEmpty()) {
                tx = w.holdouts.get(0).x;
                ty = w.holdouts.get(0).y;
            }
            float[] edge = w.city.edgeRoad(w.rnd);
            if (edge == null) return;
            for (int k = 0; k < 3; k++) w.fleet.send(Entity.SOLDIER, 6, edge[0], edge[1], tx, ty, null, null, w.city.placeName(tx, ty));
            w.aftermath = World.AFTER_NONE;
            w.warResult = 0;
            w.outbreak = true;
            w.startHumans = w.humans;
            w.warLead = 1;
            w.warBalance = 0.3f;
            w.banner("RETAKING THE CITY", "The army's last force is on its way in. Survivors, this is your chance.");
        }
    }

    void disband(World.Holdout h) {
        w.holdouts.remove(h);
        h.gone = true;
        h.b.holdout = null;
        for (int k = 0, c = w.entities.size(); k < c; k++) {
            Entity e = w.entities.get(k);
            if (e.holdout == h) {
                e.holdout = null;
                e.militia = false;
                e.scavenge = null;
                e.homeward = false;
            }
        }
    }
}
