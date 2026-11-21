package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Survivor groups holding out in buildings: founding, supply runs, scavengers.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class Holdouts {
    private final World w;

    Holdouts(World w) {
        this.w = w;
    }

    /** People in a building become a group: named after their street. */
    World.Holdout found(City.Building b, boolean announce) {
        if (b.holdout != null || b.collapsed || b.capacity == 0 || w.holdouts.size() >= 8) return b.holdout;
        World.Holdout h = new World.Holdout();
        h.b = b;
        h.x = b.doorX;
        h.y = b.doorY;
        // Named after their street (the cross street if that's taken; failing that, what the building was).
        String place = w.city.placeName(b.doorX, b.doorY), other = null;
        int amp = place.indexOf(" & ");
        if (amp > 0) {
            other = place.substring(amp + 3);
            place = place.substring(0, amp);
        }
        h.name = "the " + place + " survivors";
        if (taken(h.name) && other != null) h.name = "the " + other + " survivors";
        if (taken(h.name) && b.typeName() != null) h.name = "the " + b.typeName() + " survivors";
        if (taken(h.name)) h.name = "the " + place + " " + (w.holdouts.size() + 1) + " survivors";
        b.holdout = h;
        h.count = b.occupants.size();
        h.stash = 10 * b.occupants.size();
        w.holdouts.add(h);
        if (announce)
            w.dispatch.say(Dispatch.WHO_INFO, null, "News: Survivors holed up on " + w.city.placeName(b.doorX, b.doorY)
                    + " have organised. They're calling themselves " + h.name + ".", b.doorX, b.doorY);
        return h;
    }

    boolean taken(String name) {
        for (World.Holdout o : w.holdouts) if (o.name.equalsIgnoreCase(name)) return true;
        return false;
    }

    int members(World.Holdout h) {
        int n = h.b.occupants.size();
        for (int i = 0, c = w.entities.size(); i < c; i++) {
            Entity e = w.entities.get(i);
            if (!e.dead && e.holdout == h) n++;
        }
        return n;
    }

    void updateHoldouts(float dt) {
        w.holdoutTimer -= dt;
        boolean tick = w.holdoutTimer <= 0;
        if (tick) w.holdoutTimer = 1;
        for (int i = w.holdouts.size() - 1; i >= 0; i--) {
            World.Holdout h = w.holdouts.get(i);
            City.Building b = h.b;
            int people = tick ? members(h) : 1;
            if (tick) h.count = people;
            if (b.collapsed || people == 0 || (b.lurkers > 0 && b.occupants.isEmpty())) {
                w.aftermathLife.disband(h);
                w.dispatch.say(Dispatch.WHO_INFO, null, "News: We've lost contact with " + h.name + ".", h.x, h.y);
                continue;
            }
            if (!tick) continue;
            boolean quiet = !b.fighting && !w.spatial.zombieWithin(b.doorX, b.doorY, 120);
            // Fortifying: barricades, then cars pushed across the street and sandbags, then a proper wall.
            if (quiet && h.fort < 100) {
                h.fort = Math.min(100, h.fort + 0.25f + Math.min(12, people) * 0.06f);
                int level = h.fort >= 100 ? 3 : h.fort >= 60 ? 2 : h.fort >= 25 ? 1 : 0;
                if (level > h.said) {
                    h.said = level;
                    w.dispatch.say(Dispatch.WHO_INFO, null, "News: " + World.cap(h.name) + (level == 1 ? " have dug in and are keeping watch."
                            : level == 2 ? " have pushed cars across the street and are filling sandbags." : " have walled off their block."), h.x, h.y);
                }
            }
            // ((16.9) No barricading the doors as they dig in.)
            // A group feeds whoever turns up (as long as there's room).
            h.recruitTimer -= 1;
            if (h.recruitTimer <= 0 && b.occupants.size() < b.capacity) {
                h.recruitTimer = 6;
                for (int k = 0, c = w.entities.size(); k < c; k++) {
                    Entity e = w.entities.get(k);
                    if (e.dead || e.type != Entity.CIVILIAN || e.task != Dispatch.T_NONE || e.leader != null || e.holdout != null
                            || e == w.controlled || e.militia || w.indoors.apocalypse()) continue;
                    if ((e.x - h.x) * (e.x - h.x) + (e.y - h.y) * (e.y - h.y) > 260 * 260) continue;
                    e.task = Dispatch.T_HIDE;
                    e.building = b;
                    break;
                }
            }
            // Guards on the door: anyone inside with a gun takes a turn.
            h.guardTimer -= 1;
            if (h.guardTimer <= 0 && quiet) {
                h.guardTimer = 8;
                int guards = 0;
                for (int k = 0, c = w.entities.size(); k < c; k++) {
                    Entity e = w.entities.get(k);
                    if (!e.dead && e.holdout == h && e.militia) guards++;
                }
                if (guards < 3)
                    for (int k = 0; k < b.occupants.size(); k++) {
                        Entity o = b.occupants.get(k);
                        if (o.type != Entity.CIVILIAN || o == w.controlled) continue;
                        if (!o.hasGun && h.stash >= 20) {
                            o.hasGun = true;
                            o.ammo = 0;
                            o.magSize = 8;
                            o.reserve = 20;
                            h.stash -= 20;
                        }
                        if (!o.hasGun || o.ammo + o.reserve < 6) continue;
                        b.occupants.remove(k);
                        w.indoors.leaveBuilding(o, b, false);
                        o.militia = true;
                        o.holdout = h;
                        o.building = b;
                        float a = w.rnd.nextFloat() * World.TAU;
                        float[] p = w.city.findWalkable(b.doorX + (float) Math.cos(a) * 16, b.doorY + (float) Math.sin(a) * 16);
                        o.postX = p != null ? p[0] : b.doorX;
                        o.postY = p != null ? p[1] : b.doorY;
                        break;
                    }
            }
            // Supply runs when things are quiet and the larder (or the ammo box) is getting low.
            h.runTimer -= 1;
            if (h.runTimer <= 0 && quiet && w.aftermath != World.AFTER_RECOVERY && (b.food < 20 || h.stash < 40) && b.occupants.size() >= 2) {
                h.runTimer = 70 + w.rnd.nextFloat() * 40;
                sendRun(h);
            }
        }
    }

    /** Two of the group go for supplies: someone with a gun, and someone to carry. */
    void sendRun(World.Holdout h) {
        City.Building b = h.b;
        City.Building target = null;
        float bd = 700 * 700;
        for (int i = 0, n = w.city.buildings.size(); i < n; i++) {
            City.Building o = w.city.buildings.get(i);
            if (o == b || o.collapsed || o.infestKnown || o.lurkers > 0 || o.holdout != null) continue;
            boolean useful = (b.food < 20 && o.food >= 10) || (h.stash < 40 && (o.kind == City.SHOP && o.stock > 0 || o.kind == City.MARKET && o.stock > 0));
            if (!useful) continue;
            float d = (o.doorX - b.doorX) * (o.doorX - b.doorX) + (o.doorY - b.doorY) * (o.doorY - b.doorY);
            if (d < bd && !w.spatial.zombieWithin(o.doorX, o.doorY, 80)) {
                bd = d;
                target = o;
            }
        }
        if (target == null) return;
        int sent = 0;
        // The armed first (a guard comes off the door if nobody inside has a gun).
        for (int k = b.occupants.size() - 1; k >= 0 && sent < 2; k--) {
            Entity o = b.occupants.get(k);
            if (o.type != Entity.CIVILIAN || o == w.controlled || o.infected) continue;
            if (sent == 0 && !o.hasGun && k > 0) continue;
            b.occupants.remove(k);
            w.indoors.leaveBuilding(o, b, false);
            o.holdout = h;
            o.scavenge = target;
            o.homeward = false;
            sent++;
        }
        if (sent > 0) {
            h.runs++;
            w.dispatch.say(Dispatch.WHO_INFO, null, "News: " + World.cap(h.name) + " have sent " + (sent == 1 ? "someone" : "two of their own")
                    + " out for supplies (" + (target.typeName() != null ? "a " + target.typeName().toLowerCase(java.util.Locale.ROOT) : "a building")
                    + " on " + w.city.placeName(target.doorX, target.doorY) + ").", h.x, h.y);
        }
    }

    City.Facility supplyPoint(Entity e) {
        City.Facility f = w.city.nearestFacility(e.type == Entity.SOLDIER ? City.FACILITY_BASE : City.FACILITY_POLICE, e.x, e.y);
        if (f == null) f = w.city.nearestFacility(e.type == Entity.SOLDIER ? City.FACILITY_POLICE : City.FACILITY_BASE, e.x, e.y);
        // Police can also restock from the courthouse's armoury, if it's nearer and has some left.
        City.Facility c = w.courtArmoury;
        if (e.type != Entity.SOLDIER && c != null && c.ammo > 0 && !w.city.courthouse.collapsed
                && (f == null || World.hypot(c.x - e.x, c.y - e.y) < World.hypot(f.x - e.x, f.y - e.y))) f = c;
        return f;
    }

    /** Stands on a spot (a guard post or an ordered position). */
    void standAt(Entity e, float x, float y, float speedFactor) {
        float ddx = x - e.x, ddy = y - e.y;
        float d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
        // ((29.2) Within a few yards of it and held up there by the others round them for a couple of seconds: there
        // already. Ranks on a little base's parade ground came out on top of each other, and the soldiers shoved at
        // each other for their places for the rest of the game.)
        if (d < 30 && e.vx * e.vx + e.vy * e.vy < 9) e.crowdT += 1 / 30f;
        else if (d >= 30) e.crowdT = 0;
        // ((12.0) Eased in, and up against something within a few steps of it counts as there: no shuffling to and
        // fro round a spot they can't quite reach.)
        if (d > 5 && !(e.blocked && d < 16) && e.crowdT < 2) w.movement.steer(e, ddx / d, ddy / d, Math.min((d > 30 ? e.runSpeed : e.speed) * speedFactor, d * 2.5f + 4));
        else {
            w.movement.steer(e, 0, 0, 0);
            e.blocked = false;
        }
    }

    /** The commander stays with the soldier who has the most of the army around them. */
    Entity biggestGroup(Entity e) {
        Entity best = null;
        int most = 0;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity o = w.entities.get(i);
            if (o == e || o.dead || o.type != Entity.SOLDIER || o.member != 1) continue;
            // ((21.3) Counted from the grid round them, not by going through everyone in the city for each squad: with a
            // few hundred squads in a big city that was millions of looks every couple of seconds per commander.)
            int near = 0;
            for (int k = 0, m = w.spatial.gatherArmed(o.x, o.y, 120); k < m; k++) {
                Entity q = w.gathered[k];
                if (q.type == Entity.SOLDIER && Math.abs(q.x - o.x) < 120 && Math.abs(q.y - o.y) < 120) near++;
            }
            if (near > most) {
                most = near;
                best = o;
            }
        }
        return best;
    }
}
