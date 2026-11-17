package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Survivor bases: founding, building, manning and losing them, pickups, settlements, and their militia.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class BaseLife {
    private final World w;

    BaseLife(World w) {
        this.w = w;
    }

    /**
     * Survivors found a base round building b (see SurvivorBases): null if it won't do there - too many already, too
     * near a safe zone or another base, or a building that can't be walled round.
     */
    SurvivorBases.Base foundBase(City.Building b, boolean announce) {
        if (b == null || b.collapsed || w.bases.size() >= SurvivorBases.MAX_BASES || w.city.cfg.mission >= 0) return null;
        for (int i = 0; i < w.bases.size(); i++) if (w.bases.get(i).b == b) return w.bases.get(i);
        // (One at a time: a city's survivors don't all dig in at once.)
        if (w.time - w.baseFoundedAt < 45 && !w.bases.isEmpty()) return null;
        SurvivorBases.Base s = SurvivorBases.plan(w.city, b);
        if (s == null) return null;
        float cx = s.cx(), cy = s.cy();
        for (int i = 0; i < w.bases.size(); i++) {
            SurvivorBases.Base o = w.bases.get(i);
            if (s.tx1 + 4 >= o.tx0 && s.tx0 - 4 <= o.tx1 && s.ty1 + 4 >= o.ty0 && s.ty0 - 4 <= o.ty1) return null;
        }
        for (int i = 0; i < w.dispatch.zones.size(); i++) {
            Dispatch.SafeZone z = w.dispatch.zones.get(i);
            if (!z.removed && cx > z.left() - 200 && cx < z.right() + 200 && cy > z.top() - 200 && cy < z.bottom() + 200) return null;
        }
        s.name = SurvivorBases.nameFor(w.city, b, b.doorX > 0 ? (int) (b.doorX * 7 + b.doorY * 13) : w.bases.size());
        w.bases.add(s);
        w.basesFounded++;
        w.baseFoundedAt = w.time;
        if (announce)
            w.dispatch.say(Dispatch.WHO_INFO, null, "News: Survivors on " + w.city.placeName(b.doorX, b.doorY) + " are walling themselves in. They're calling it "
                    + s.name + ".", cx, cy);
        return s;
    }

    /** ((26.0) For loading a save: a settlement laid out over its ground again, in place of the base round its building. */
    void restoreSettlement(int bi, int x0, int y0, int x1, int y1, int room) {
        if (bi < 0 || bi >= w.city.buildings.size()) return;
        City.Building b = w.city.buildings.get(bi);
        for (int i = 0; i < w.bases.size(); i++) {
            SurvivorBases.Base old = w.bases.get(i);
            if (old.b != b) continue;
            SurvivorBases.Base m = SurvivorBases.planRect(w.city, b, x0, y0, x1, y1, 240);
            if (m == null) return;
            m.settlement = true;
            m.name = old.name;
            m.tier = old.tier;
            m.work = old.work;
            m.walled = old.walled;
            m.towersUp = old.towersUp;
            m.stash = old.stash;
            m.workshop = old.workshop;
            m.age = old.age;
            m.said = old.said;
            m.room = room;
            w.bases.set(i, m);
            return;
        }
    }

    /** (For loading a save: the base laid out round its building again, without the checks a new one has.) */
    SurvivorBases.Base restoreBase(City.Building b) {
        SurvivorBases.Base s = SurvivorBases.plan(w.city, b);
        if (s != null) w.bases.add(s);
        return s;
    }

    /** Once a second: each base counts its people, builds, mans its posts, works its gates and workshop - or falls. */
    void updateBases() {
        // (Armed bands with people to protect found bases of their own.)
        if (w.outbreak && ((int) w.time) % 5 == 2 && w.bases.size() < SurvivorBases.MAX_BASES) bandBases();
        if (w.outbreak && ((int) w.time) % 5 == 4) roamingPickups();
        if (((int) w.time) % 10 == 7) joinBases();
        for (int i = w.bases.size() - 1; i >= 0; i--) {
            SurvivorBases.Base s = w.bases.get(i);
            s.age += 1;
            s.saidCd -= 1;
            float cx = s.cx(), cy = s.cy();
            float half = Math.max(s.tx1 - s.tx0, s.ty1 - s.ty0) * City.T / 2f;
            // Who's here.
            s.members = s.guards = s.inside = s.engineers = 0;
            // (Everyone who belongs, wherever they are - on the way in counts too.)
            for (int k = 0, n = w.entities.size(); k < n; k++) {
                Entity e = w.entities.get(k);
                if (e.base != s || e.dead) continue;
                s.members++;
                if (e.militia) s.guards++;
                if (e.trade() == Entity.TR_ENGINEER) s.engineers++;
            }
            for (int k = 0, n = w.spatial.gatherZ(cx, cy, half + 8); k < n; k++) if (s.contains(w.gathered[k].x, w.gathered[k].y, -1)) s.inside++;
            boolean quiet = !w.spatial.zombieWithin(cx, cy, half + 90);
            s.quietFor = quiet ? s.quietFor + 1 : 0;
            if (s.members == 0 || s.b.collapsed || (s.inside > 0 && s.inside >= s.members * 2)) s.emptyFor += 1;
            else s.emptyFor = 0;
            if (s.emptyFor > 12) {
                loseBase(s);
                continue;
            }
            recruitToBase(s, quiet);
            buildBase(s, quiet);
            if (((int) w.time) % 5 == 1) basePickup(s);
            if (((int) w.time) % 3 == 0) manBase(s);
            baseGates(s);
            baseWorkshop(s);
        }
    }

    void loseBase(SurvivorBases.Base s) {
        s.fallen = true;
        w.bases.remove(s);
        w.basesLost++;
        // (The gates are left open; the wall stands, a ruin.)
        for (int g : s.gates) {
            float[] gb = SurvivorBases.barrierAt(w, g);
            if (gb != null) w.sandbox.removeBarricade(gb);
        }
        for (int k = 0, n = w.entities.size(); k < n; k++) {
            Entity e = w.entities.get(k);
            if (e.base == s) {
                e.base = null;
                e.onTower = false;
            }
        }
        w.dispatch.say(Dispatch.WHO_INFO, null, "News: We've lost contact with " + s.name + ".", s.cx(), s.cy());
    }

    /** An armed survivor leading a band of three or more, somewhere quiet: they make a stand at the nearest building. */
    void bandBases() {
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity l = w.entities.get(i);
            if (l.dead || l.type != Entity.CIVILIAN || !l.canShoot() || l.bandN < 3 || l.base != null || l == w.controlled || l.raider) continue;
            if (w.spatial.zombieWithin(l.x, l.y, 160)) continue;
            City.Building b = null;
            float bd = 220 * 220;
            for (int k = 0, m = w.city.buildings.size(); k < m; k++) {
                City.Building o = w.city.buildings.get(k);
                if (o.collapsed || o.capacity == 0 || !w.city.enterable(o)) continue;
                float d = (o.doorX - l.x) * (o.doorX - l.x) + (o.doorY - l.y) * (o.doorY - l.y);
                if (d < bd) {
                    bd = d;
                    b = o;
                }
            }
            SurvivorBases.Base s = foundBase(b, true);
            if (s == null) continue;
            joinBase(l, s, true);
            for (int k = 0, m = w.spatial.gatherLiving(l.x, l.y, 90); k < m; k++) {
                Entity o = w.gathered[k];
                if (o.leader == l && !o.dead) joinBase(o, s, false);
            }
            return;
        }
    }

    /** Someone becomes one of a base's people: the armed take a post on its wall, the rest go inside it. */
    void joinBase(Entity e, SurvivorBases.Base s, boolean armed) {
        e.base = s;
        e.leader = null;
        if (e.task == Dispatch.T_SEEK || e.task == Dispatch.T_HIDE) e.task = Dispatch.T_NONE;
        e.seekZone = null;
        if (armed && e.canShoot() && e.ammo + e.reserve >= 6) {
            e.militia = true;
            e.building = s.b;
            e.postX = s.b.doorX;
            e.postY = s.b.doorY;
        }
    }

    void recruitToBase(SurvivorBases.Base s, boolean quiet) {
        if (((int) w.time) % 3 != 0 || s.members >= s.room) return;
        float cx = s.cx(), cy = s.cy();
        int took = 0;
        for (int k = 0, n = w.spatial.gatherLiving(cx, cy, 300); k < n && took < 3; k++) {
            Entity e = w.gathered[k];
            if (e.dead || e.type != Entity.CIVILIAN || e.base != null || e == w.controlled || e.raider || e.infected || e.inmate
                    || (e.task != Dispatch.T_NONE && e.task != Dispatch.T_SEEK) || e.zone != null) continue;
            if (e.leader != null && e.leader.base != s) continue;
            // (Not someone already nearly at a safe zone's gate.)
            if (e.task == Dispatch.T_SEEK && e.seekZone != null && World.hypot(e.x - e.seekZone.x, e.y - e.seekZone.y) < World.hypot(e.x - cx, e.y - cy)) continue;
            joinBase(e, s, e.canShoot());
            took++;
        }
    }

    /** The wall going up, section by section while it's quiet; then sandbags, then concrete; damage made good. */
    void buildBase(SurvivorBases.Base s, boolean quiet) {
        if (!quiet || s.members == 0) return;
        // ((26.0) Engineers among them build a good deal faster.)
        s.work += 0.2f + Math.min(10, s.members) * 0.05f + Math.min(4, s.engineers) * 0.15f;
        // A section down (or not yet up) first.
        while (s.work >= SurvivorBases.SECTION_WORK) {
            int ni = SurvivorBases.nextSection(w.city, s, -1);
            if (ni < 0) break;
            int next = s.ring[ni];
            float nx = (next % w.city.w + 0.5f) * City.T, ny = (next / w.city.w + 0.5f) * City.T;
            // (Not under a car: it'd be shut in the wall for good.)
            float[] b = w.rides.vehicleAt(nx, ny) != null || w.rides.vehicleAt(nx - 6, ny) != null || w.rides.vehicleAt(nx + 6, ny) != null ? null : w.sandbox.placeBarricade(nx, ny, s.tier);
            if (b == null) {
                // (Someone standing there, or it can't be built on: the next one, and this one later - or never.)
                s.fails[ni]++;
                int nj = SurvivorBases.nextSection(w.city, s, ni);
                if (nj < 0) break;
                int t = s.ring[nj];
                if (w.sandbox.placeBarricade((t % w.city.w + 0.5f) * City.T, (t / w.city.w + 0.5f) * City.T, s.tier) == null) {
                    s.fails[nj]++;
                    break;
                }
            }
            s.work -= SurvivorBases.SECTION_WORK;
        }
        int up = SurvivorBases.wallsUp(w.city, s), want = s.ring.length - s.gates.length;
        if (!s.walled && up >= want) {
            s.walled = true;
            s.work = 0;
            if (s.said < 1) {
                s.said = 1;
                w.dispatch.say(Dispatch.WHO_INFO, null, "News: " + s.name + " has a wall all the way round now.", s.cx(), s.cy());
            }
        }
        if (!s.walled || up < want) return;
        // Patching what the dead have knocked about.
        for (int t : s.ring) {
            if (s.isGate(t)) continue;
            float[] b = SurvivorBases.barrierAt(w, t);
            if (b != null) b[2] = Math.min(World.BARRICADE_HP[Math.max(0, Math.min(2, (int) b[5]))], b[2] + 6);
        }
        // Towers, one every so often.
        int maxTowers = s.tier == 0 ? Math.min(2, s.towers.length) : s.towers.length;
        if (s.towersUp < maxTowers && s.quietFor > 0 && s.quietFor % 15 == 0) {
            s.towersUp++;
            if (s.towersUp == 1 && s.said < 2) {
                s.said = 2;
                w.dispatch.say(Dispatch.WHO_INFO, null, "News: " + s.name + " has a watchtower up. Their best shots are on it.", s.cx(), s.cy());
            }
        }
        // Then a stronger wall.
        if (s.tier < 2 && s.work >= (s.tier == 0 ? SurvivorBases.SANDBAG_WORK : SurvivorBases.CONCRETE_WORK)) {
            s.work = 0;
            s.tier++;
            for (int t : s.ring) {
                float[] b = SurvivorBases.barrierAt(w, t);
                if (b != null) World.setTier(b, s.tier);
            }
            w.dispatch.say(Dispatch.WHO_INFO, null, "News: " + s.name + " has rebuilt its wall in " + (s.tier == 1 ? "sandbags." : "concrete."), s.cx(), s.cy());
        }
    }

    /** The armed among them on the gates first, then up the towers, then round the door. */
    void manBase(SurvivorBases.Base s) {
        ArrayList<Entity> guards = new ArrayList<Entity>();
        for (int k = 0, n = w.spatial.gatherLiving(s.cx(), s.cy(), Math.max(s.tx1 - s.tx0, s.ty1 - s.ty0) * City.T + 260); k < n; k++) {
            Entity e = w.gathered[k];
            if (!e.dead && e.base == s && e.militia) guards.add(e);
        }
        ArrayList<float[]> spots = new ArrayList<float[]>();
        float cx = s.cx(), cy = s.cy();
        for (int g : s.gates) {
            float gx = (g % w.city.w + 0.5f) * City.T, gy = (g / w.city.w + 0.5f) * City.T;
            float dx = cx - gx, dy = cy - gy, l = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
            float[] p = w.city.findWalkable(gx + dx / l * City.T * 1.5f, gy + dy / l * City.T * 1.5f);
            if (p != null) spots.add(new float[]{p[0], p[1], 0});
        }
        for (int k = 0; k < s.towersUp && k < s.towers.length; k++) spots.add(new float[]{s.towers[k][0], s.towers[k][1], 1});
        for (int k = 0; k < guards.size(); k++) {
            Entity e = guards.get(k);
            e.building = s.b;
            if (k < spots.size()) {
                e.postX = spots.get(k)[0];
                e.postY = spots.get(k)[1];
                e.onTower = spots.get(k)[2] == 1;
            } else {
                float a = k * 2.4f;
                float[] p = w.city.findWalkable(s.b.doorX + (float) Math.cos(a) * 22, s.b.doorY + (float) Math.sin(a) * 22);
                e.postX = p != null ? p[0] : s.b.doorX;
                e.postY = p != null ? p[1] : s.b.doorY;
                e.onTower = false;
            }
        }
    }

    /** ((26.0) A walled base with rounds to spare and guards enough fits out an armed pickup, two of its guards aboard. */
    void basePickup(SurvivorBases.Base s) {
        if (!s.walled || s.stash < 80 || s.guards < 4 || w.fleet.pickupsOf(s) > 0 || w.fleet.pickups() >= 6 || s.gates.length == 0) return;
        int g = s.gates[0];
        float gx = (g % w.city.w + 0.5f) * City.T, gy = (g / w.city.w + 0.5f) * City.T;
        float dx = gx - s.cx(), dy = gy - s.cy(), l = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
        float[] spot = w.city.nearestDrivable(gx + dx / l * City.T * 2.5f, gy + dy / l * City.T * 2.5f);
        if (spot == null || s.contains(spot[0], spot[1], 0) || w.rides.vehicleAt(spot[0], spot[1]) != null) return;
        ArrayList<Entity> crew = new ArrayList<Entity>();
        for (int k = 0, n = w.entities.size(); k < n && crew.size() < 2; k++) {
            Entity e = w.entities.get(k);
            if (!e.dead && e.base == s && e.militia && e.ammo + e.reserve >= 10 && e != w.controlled) crew.add(e);
        }
        if (crew.size() < 2) return;
        for (Entity e : crew) {
            e.militia = false;
            e.onTower = false;
        }
        s.stash -= 60;
        Fleet.Vehicle v = w.fleet.makePickup(spot[0], spot[1], (float) Math.atan2(dy, dx), s, crew);
        crew.get(1).reserve += 40;
        w.dispatch.say(Dispatch.WHO_INFO, null, "News: " + s.name + " has bolted a gun on a pickup. It's out hunting the dead round their walls.", v.x, v.y);
    }

    /** ((26.0) Armed survivors on the move with no base take a parked car and bolt a gun in the back. */
    void roamingPickups() {
        if (w.fleet.pickups() >= 6) return;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity l = w.entities.get(i);
            if (l.dead || l.type != Entity.CIVILIAN || !l.canShoot() || l.base != null || l.militia || l.raider || l == w.controlled
                    || l.ammo + l.reserve < 12 || l.leader != null || w.spatial.zombieWithin(l.x, l.y, 90)) continue;
            Entity mate = null;
            for (int k = 0, m = w.spatial.gatherLiving(l.x, l.y, 50); k < m && mate == null; k++) {
                Entity o = w.gathered[k];
                if (o != l && !o.dead && o.type == Entity.CIVILIAN && o.base == null && !o.militia && o != w.controlled && !o.infected
                        && (o.leader == l || o.canShoot())) mate = o;
            }
            if (mate == null) continue;
            Fleet.Vehicle car = null;
            for (int k = 0, m = w.fleet.gather(l.x, l.y, 70); k < m && car == null; k++) {
                Fleet.Vehicle v = w.fleet.found[k];
                if (v.type == Fleet.CAR && v.parked && !v.broken && !v.burnt && v.kind == 0 && v.riders.isEmpty() && v.player == null
                        && v.evacStop < 0) car = v;
            }
            if (car == null) continue;
            ArrayList<Entity> crew = new ArrayList<Entity>();
            crew.add(mate);
            crew.add(l);
            mate.leader = null;
            w.fleet.takeAsPickup(car, crew);
            w.dispatch.say(Dispatch.WHO_INFO, null, "911: \"Some guys just bolted a rifle on a pickup and drove off shooting at those things!\"", car.x, car.y);
            return;
        }
    }

    /**
     * ((26.0) Two walled bases close together join up: one wall round the both of them - a settlement - the walls between
     * them taken down, their people and stores put together.
     */
    void joinBases() {
        for (int i = 0; i < w.bases.size(); i++)
            for (int j = i + 1; j < w.bases.size(); j++) {
                SurvivorBases.Base a = w.bases.get(i), b = w.bases.get(j);
                if (!a.walled || !b.walled || a.settlement || b.settlement) continue;
                int gapX = Math.max(0, Math.max(a.tx0, b.tx0) - Math.min(a.tx1, b.tx1)), gapY = Math.max(0, Math.max(a.ty0, b.ty0) - Math.min(a.ty1, b.ty1));
                if (gapX > 18 || gapY > 18) continue;
                int x0 = Math.min(a.tx0, b.tx0), y0 = Math.min(a.ty0, b.ty0), x1 = Math.max(a.tx1, b.tx1), y1 = Math.max(a.ty1, b.ty1);
                if (x1 - x0 > 46 || y1 - y0 > 46) continue;
                SurvivorBases.Base big = a.members >= b.members ? a : b, small = big == a ? b : a;
                SurvivorBases.Base m = SurvivorBases.planRect(w.city, big.b, x0, y0, x1, y1, 240);
                if (m == null) continue;
                // No zone in the way.
                boolean clash = false;
                for (int k = 0; k < w.dispatch.zones.size() && !clash; k++) {
                    Dispatch.SafeZone z = w.dispatch.zones.get(k);
                    clash = !z.removed && z.right() > x0 * City.T && z.left() < (x1 + 1) * City.T && z.bottom() > y0 * City.T && z.top() < (y1 + 1) * City.T;
                }
                if (clash) continue;
                joinInto(m, a, b);
                m.room = Math.min(60, a.room + b.room);
                String place = w.city.placeName(m.cx(), m.cy());
                int amp = place.indexOf(" & ");
                if (amp > 0) place = place.substring(0, amp);
                w.dispatch.say(Dispatch.WHO_INFO, null, "News: " + big.name + " and " + small.name + " have joined up. They're walling in the "
                        + place + " Settlement together.", m.cx(), m.cy());
                m.name = place + " Settlement";
                return;
            }
    }

    /** The settlement m takes the place of bases a and b. */
    void joinInto(SurvivorBases.Base m, SurvivorBases.Base a, SurvivorBases.Base b) {
        m.settlement = true;
        m.tier = Math.min(a.tier, b.tier);
        m.stash = a.stash + b.stash;
        m.age = Math.max(a.age, b.age);
        m.said = 1;
        // The old walls inside it come down (those on its own edge stay).
        java.util.HashSet<Integer> edge = new java.util.HashSet<Integer>();
        for (int t : m.ring) edge.add(t);
        SurvivorBases.Base[] old = {a, b};
        for (SurvivorBases.Base o : old)
            for (int t : o.ring) {
                if (edge.contains(t)) continue;
                float[] bar = SurvivorBases.barrierAt(w, t);
                if (bar != null) w.sandbox.removeBarricade(bar);
            }
        w.bases.remove(a);
        w.bases.remove(b);
        w.bases.add(m);
        for (int k = 0, n = w.entities.size(); k < n; k++) {
            Entity e = w.entities.get(k);
            if (e.base == a || e.base == b) e.base = m;
        }
        for (int k = 0; k < w.fleet.vehicles.size(); k++) {
            Fleet.Vehicle v = w.fleet.vehicles.get(k);
            if (v.pbase == a || v.pbase == b) v.pbase = m;
            if (v.ptarget == a || v.ptarget == b) v.ptarget = null;
        }
        a.fallen = b.fallen = true;
    }

    /** Shut when the dead come near (with nobody in the gateway), open again once they've been gone a while. */
    void baseGates(SurvivorBases.Base s) {
        for (int g : s.gates) {
            float gx = (g % w.city.w + 0.5f) * City.T, gy = (g / w.city.w + 0.5f) * City.T;
            boolean shut = w.city.tiles[g] == City.FENCE;
            boolean threat = w.spatial.zombieWithin(gx, gy, 110);
            if (threat && !shut) {
                float[] b = w.sandbox.placeBarricade(gx, gy, s.tier);
                if (b != null && s.saidCd <= 0) {
                    s.saidCd = 40;
                    w.dispatch.chatter(Dispatch.WHO_INFO, null, s.name + ": Shut the gate! They're coming!", gx, gy);
                }
            } else if (shut && !w.spatial.zombieWithin(gx, gy, 160) && s.quietFor >= 5) {
                float[] b = SurvivorBases.barrierAt(w, g);
                if (b != null) w.sandbox.removeBarricade(b);
            }
        }
    }

    /** Once the wall's up: the workshop makes rounds, and the armoury hands a gun to the unarmed among them. */
    void baseWorkshop(SurvivorBases.Base s) {
        if (!s.walled) return;
        s.workshop += 0.02f + Math.min(12, s.members) * 0.012f;
        if (s.workshop >= 1) {
            s.workshop -= 1;
            s.stash = Math.min(400, s.stash + 12);
            if (s.said < 3) {
                s.said = 3;
                w.dispatch.say(Dispatch.WHO_INFO, null, "News: " + s.name + " has a workshop going, making its own ammunition.", s.cx(), s.cy());
            }
        }
        if (s.stash < 20 || s.guards >= Math.min(10, Math.max(2, s.members / 2))) return;
        for (int k = 0, n = w.spatial.gatherLiving(s.cx(), s.cy(), Math.max(s.tx1 - s.tx0, s.ty1 - s.ty0) * City.T); k < n; k++) {
            Entity e = w.gathered[k];
            if (e.dead || e.base != s || e.militia || e.hasGun || e.type != Entity.CIVILIAN || e.infected) continue;
            e.hasGun = true;
            e.weapon = Entity.W_STD;
            e.magSize = 8;
            e.ammo = 8;
            e.reserve = 12;
            s.stash -= 20;
            joinBase(e, s, true);
            return;
        }
    }

    /**
     * One of a base's unarmed people: inside its wall and about the place - lending a hand where a section's going up -
     * or making their way back in. False when they should look to themselves (the dead right on them, or the base gone).
     */
    boolean baseMember(Entity e, float dt) {
        SurvivorBases.Base s = e.base;
        if (s == null || s.fallen) {
            e.base = null;
            return false;
        }
        if (e.task == Dispatch.T_SEEK || e.task == Dispatch.T_HIDE) e.task = Dispatch.T_NONE;
        float cx = s.cx(), cy = s.cy();
        if (w.spatial.zombieWithin(e.x, e.y, 70)) {
            // ((25.1) The dead outside the wall are no reason to run - out through the gate, as half of them did in a
            // siege. Back from the wall, towards the building; only one of them inside it sends them running.)
            boolean inWithThem = false;
            for (int k = 0, n = w.spatial.gatherZ(e.x, e.y, 70); k < n && !inWithThem; k++) inWithThem = s.contains(w.gathered[k].x, w.gathered[k].y, -1);
            if (inWithThem || !s.contains(e.x, e.y, -1)) return false;
            e.fleeTimer = 0;
            w.holdoutLife.standAt(e, s.b.doorX + (cx - s.b.doorX) * 0.3f, s.b.doorY + (cy - s.b.doorY) * 0.3f, 1.2f);
            return true;
        }
        if (!s.contains(e.x, e.y, -1)) {
            w.wilds.walkToSpot(e, s.b.doorX, s.b.doorY, e.speed * 1.4f);
            return true;
        }
        e.baseSpotT -= dt;
        if (e.baseSpotT <= 0) {
            e.baseSpotT = 4 + w.rnd.nextFloat() * 6;
            float px = -1, py = -1;
            // (Helping with the next stretch of wall, while there's one to build.)
            int ni = s.walled ? -1 : SurvivorBases.nextSection(w.city, s, -1);
            if (ni >= 0) {
                int t = s.ring[ni];
                float tx = (t % w.city.w + 0.5f) * City.T, ty = (t / w.city.w + 0.5f) * City.T;
                float dx = cx - tx, dy = cy - ty, l = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
                px = tx + dx / l * City.T;
                py = ty + dy / l * City.T;
            }
            if (px < 0) {
                px = (s.tx0 + 1.5f + w.rnd.nextFloat() * (s.tx1 - s.tx0 - 2)) * City.T;
                py = (s.ty0 + 1.5f + w.rnd.nextFloat() * (s.ty1 - s.ty0 - 2)) * City.T;
            }
            float[] p = w.city.findWalkable(px, py);
            if (p != null && s.contains(p[0], p[1], -1)) {
                e.baseSpotX = p[0];
                e.baseSpotY = p[1];
            } else {
                e.baseSpotX = s.b.doorX;
                e.baseSpotY = s.b.doorY;
            }
        }
        w.holdoutLife.standAt(e, e.baseSpotX, e.baseSpotY, 0.6f);
        return true;
    }
}
