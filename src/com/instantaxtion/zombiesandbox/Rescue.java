package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Rescue: people cut off calling for a lift, and the crews who go for them.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class Rescue {
    private final World w;

    Rescue(World w) {
        this.w = w;
    }

    /** Someone cut off by the dead out in the wilds (or up a tower), with no help nearby, for the rescue helicopter. */
    void callRescue() {
        if (!w.city.cfg.wilds() || w.alert < 1) return;
        // (A rescue helicopter on its pad, with its pilot and team: none ready, no rescue.)
        Fleet.Vehicle ready = w.fleet.readyHeli(Fleet.K_RESCUE_HELI, w.city.worldW() / 2, w.city.worldH() / 2);
        if (ready == null) return;
        Entity best = null;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || e.type != Entity.CIVILIAN || e.leader != null || e.task == Dispatch.T_RESCUE) continue;
            int near = w.spatial.countZombiesNear(e.x, e.y, 170);
            if (near == 0 || (!e.aloft && near > 12)) continue;
            Entity help = w.spatial.nearestArmed(e.x, e.y, 260);
            // (A helicopter crew standing by its pad isn't help on the ground: they're the ones who'd come.)
            if (help != null && !(help.task == Dispatch.T_POST && (help.role == Entity.ROLE_SAR || help.role == Entity.ROLE_PILOT))) continue;
            // (In town, only a group cut off together is worth a helicopter.)
            if (w.city.inTown(e.x, e.y) && !e.aloft && w.spatial.countCiviliansNear(e.x, e.y, 60) < 3) continue;
            boolean covered = false;
            for (int k = 0; k < w.fleet.vehicles.size() && !covered; k++) {
                Fleet.Vehicle v = w.fleet.vehicles.get(k);
                if (v.kind == Fleet.K_RESCUE_HELI && World.hypot(v.tx - e.x, v.ty - e.y) < 300) covered = true;
            }
            if (covered) continue;
            best = e;
            if (e.aloft) break;
        }
        if (best == null) return;
        String place = best.aloft && best.jobStep >= 0 && best.jobStep < w.city.fireTowerNames.size() ? w.city.fireTowerNames.get(best.jobStep)
                : w.city.placeName(best.x, best.y);
        // (Hovering beside the tower, not on top of it.)
        float tx = best.x + (best.aloft ? 30 : 0), ty = best.y + (best.aloft ? 20 : 0);
        ready = w.fleet.readyHeli(Fleet.K_RESCUE_HELI, tx, ty);
        if (!w.fleet.launchHeli(ready, tx, ty, place, null, 0)) return;
        w.dispatch.say(Dispatch.WHO_POLICE, null, "Dispatch: Rescue " + ready.number + ", people cut off at " + place
                + ". Crew to the helicopter, get them out.", tx, ty);
        w.rescueCd = 20;
    }

    /**
     * The rescue helicopter in the hover. Dropping off: lowers its team (1 once they're all down, and it can go).
     * Picking up: winches up everyone waiting at the landing spot (a lookout straight off the tower too), then the
     * team (2 once they're all aboard).
     */
    int rescueStep(Fleet.Vehicle v, float dt) {
        v.timer += dt;
        if (!v.pickup) {
            if (v.team == null) {
                if (World.hypot(v.x - v.tx, v.y - v.ty) > 16) return 0;
                v.team = new ArrayList<Entity>();
                // (Since 10.20 the team is the one that boarded at the pad: out of the door and down the line.)
                ArrayList<Entity> aboard = new ArrayList<Entity>();
                for (Entity c : v.crew) if (c.role == Entity.ROLE_SAR) aboard.add(c);
                for (int i = 0; i < (v.padAt != null ? aboard.size() : 3); i++) {
                    Entity s;
                    if (v.padAt != null) {
                        s = aboard.get(i);
                        v.crew.remove(s);
                        if (!w.rides.release(s, v.tx + (i - 1) * 6, v.ty)) continue;
                    } else {
                        s = w.population.spawn(Entity.COP, v.tx + (i - 1) * 6, v.ty);
                        if (s == null) continue;
                        w.population.applyRole(s, Entity.ROLE_SWAT);
                        s.role = Entity.ROLE_SAR;
                        s.body = 0xFFE06A20;
                    }
                    s.rig = v;
                    w.fleet.note(s);
                    s.task = Dispatch.T_RESCUE;
                    s.roping = 2.5f + i * 1.2f;
                    s.member = i;
                    s.callsign = v.number;
                    v.team.add(s);
                }
                return 0;
            }
            // (Anyone stranded up a tower goes straight up on the winch while the team ropes down.)
            hoistFromTower(v);
            for (int i = 0; i < v.team.size(); i++) if (!v.team.get(i).dead && v.team.get(i).roping > 0) return 0;
            v.timer = 0;
            return 1;
        }
        // Picking up: whoever is waiting at the landing spot goes up on the winch.
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || e.type != Entity.CIVILIAN) continue;
            boolean waiting = (e.task == Dispatch.T_RESCUE || (e.leader != null && e.leader.role == Entity.ROLE_SAR)) && World.hypot(e.x - v.tx, e.y - v.ty) < 40;
            if (!waiting) continue;
            e.dead = true;
            e.removed = true;
            v.passengers++;
        }
        if (v.timer > 4) hoistFromTower(v);
        v.recall = true;
        if (v.team == null) return 2;
        boolean anyLeft = false;
        for (int i = 0; i < v.team.size(); i++) {
            Entity s = v.team.get(i);
            if (s.dead) continue;
            if (World.hypot(s.x - v.tx, s.y - v.ty) < 20 || v.timer > 50) {
                if (v.padAt != null) w.fleet.board(v, s);
                else {
                    s.dead = true;
                    s.removed = true;
                }
            } else anyLeft = true;
        }
        return anyLeft ? 0 : 2;
    }

    void hoistFromTower(Fleet.Vehicle v) {
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.dead || e.type != Entity.CIVILIAN || !e.aloft || World.hypot(e.x - v.x, e.y - v.y) > 60) continue;
            e.aloft = false;
            if (e.jobStep >= 0 && w.towerKeeper != null && e.jobStep < w.towerKeeper.length && w.towerKeeper[e.jobStep] == e) w.towerKeeper[e.jobStep] = null;
            e.dead = true;
            e.removed = true;
            v.passengers++;
        }
    }

    /**
     * The team on the ground calls the helicopter back: they're hurt, low on rounds, someone's down, or
     * everyone they could find is at the landing spot.
     */
    boolean rescueCalled(Fleet.Vehicle v) {
        v.timer += 1 / 30f;
        if (v.team == null) return true;
        int alive = 0, waiting = 0;
        String why = null;
        for (int i = 0; i < v.team.size(); i++) {
            Entity s = v.team.get(i);
            if (s.dead) {
                if (why == null) why = "we've lost one of ours";
                continue;
            }
            alive++;
            if (s.hp < s.maxHp * 0.5f && why == null) why = "we've got wounded";
            if (s.ammo + s.reserve < World.fullReserve(s) * 0.25f && why == null) why = "we're low on ammo";
        }
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (!e.dead && e.task == Dispatch.T_RESCUE && e.type == Entity.CIVILIAN) waiting++;
        }
        if (alive == 0) why = "the team's gone quiet";
        if (why == null && v.timer > 25) {
            // Anyone left out there to fetch?
            boolean more = false;
            for (int i = 0, n = w.entities.size(); i < n && !more; i++) {
                Entity e = w.entities.get(i);
                if (e.dead || e.type != Entity.CIVILIAN || e.task == Dispatch.T_RESCUE || e.aloft) continue;
                if (World.hypot(e.x - v.tx, e.y - v.ty) < 220 && (e.leader == null || e.leader.role == Entity.ROLE_SAR)) more = true;
            }
            if (!more) why = "everyone's at the landing spot";
        }
        if (why == null && v.timer > 120) why = "we've done what we can";
        if (why == null) return false;
        Entity speaker = null;
        for (int i = 0; i < v.team.size() && speaker == null; i++) if (!v.team.get(i).dead) speaker = v.team.get(i);
        w.dispatch.say(Dispatch.WHO_POLICE, speaker, "Rescue " + v.number + ", " + why + ". " + (waiting > 0 ? waiting + " civilian" + (waiting == 1 ? "" : "s") + " to lift. " : "")
                + "Requesting pickup at " + (v.place != null ? v.place : "the landing spot") + ".", v.tx, v.ty);
        return true;
    }

    /** A search and rescue specialist on the ground. False to fight like any officer. */
    boolean rescuer(Entity e, float dt) {
        Fleet.Vehicle v = e.rig;
        if (v == null || v.removedFromFleet || v.team == null) {
            // (Left behind: just another officer now.)
            e.rig = null;
            e.task = Dispatch.T_NONE;
            return false;
        }
        if (e.roping > 0) {
            // Coming down the line.
            e.roping -= dt;
            e.x = v.tx + (e.member - 1) * 6;
            e.y = v.ty;
            this.w.movement.steer(e, 0, 0, 0);
            return true;
        }
        float hx = v.tx, hy = v.ty;
        if (v.recall && v.state != 20) {
            // The helicopter's here: back to the line.
            this.w.wilds.walkToSpot(e, hx, hy, e.runSpeed);
            return true;
        }
        // Anything close: deal with it first.
        Entity z = this.w.spatial.nearest(e, 80, true, true);
        if (z != null) return false;
        Entity c = e.escort;
        if (c == null || c.dead || c.task == Dispatch.T_RESCUE || (c.leader != null && c.leader != e)) {
            c = null;
            e.escort = null;
            e.jobTimer -= dt;
            if (e.jobTimer <= 0) {
                e.jobTimer = 1;
                // Anyone out on the streets around the landing spot, nearest first (and not someone a teammate has).
                this.w.sarClaimed.clear();
                for (int i = 0, n = this.w.entities.size(); i < n; i++) {
                    Entity t = this.w.entities.get(i);
                    if (t != e && !t.dead && t.role == Entity.ROLE_SAR && t.escort != null) this.w.sarClaimed.add(t.escort);
                }
                float bd = 420 * 420;
                for (int i = 0, n = this.w.entities.size(); i < n; i++) {
                    Entity o = this.w.entities.get(i);
                    if (o.dead || o.type != Entity.CIVILIAN || o.aloft || o.ride != null || o.task == Dispatch.T_RESCUE
                            || (o.leader != null && o.leader.role == Entity.ROLE_SAR) || this.w.sarClaimed.contains(o)) continue;
                    float d = (o.x - hx) * (o.x - hx) + (o.y - hy) * (o.y - hy);
                    if (d < bd) {
                        bd = d;
                        c = o;
                    }
                }
                e.escort = c;
                // Nobody outside: knock on the doors of the places people are holed up in.
                if (c == null && (e.building == null || e.building.occupants.isEmpty() || e.building.lurkers > 0)) {
                    e.building = null;
                    float bb = 420 * 420;
                    for (int i = 0, n = this.w.city.buildings.size(); i < n; i++) {
                        City.Building b = this.w.city.buildings.get(i);
                        if (b.occupants.isEmpty() || b.lurkers > 0 || b.collapsed || b.holdout != null) continue;
                        float d = (b.doorX - hx) * (b.doorX - hx) + (b.doorY - hy) * (b.doorY - hy);
                        // ((11.5) Survivors on the radio are heard from three times as far.)
                        if (World.useOf(b) == Variants.U_RADIO) d /= 9;
                        if (d < bb) {
                            bb = d;
                            e.building = b;
                        }
                    }
                }
            }
        }
        if (c == null && e.building != null && !e.building.occupants.isEmpty() && e.building.lurkers <= 0) {
            City.Building b = e.building;
            float dx = b.doorX - e.x, dy = b.doorY - e.y, d = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
            if (d > 14) {
                this.w.wilds.walkToSpot(e, b.doorX, b.doorY, e.runSpeed);
                return true;
            }
            // "Rescue team! Anyone in there?": someone comes out with them.
            Entity o = this.w.playerControl.popOccupant(b);
            if (o != null) {
                this.w.indoors.leaveBuilding(o, b, false);
                o.leader = e;
                o.aware = true;
                e.escort = o;
                e.talkTimer = 1.5f;
                c = o;
            }
            if (b.occupants.isEmpty()) e.building = null;
            if (c == null) return true;
        }
        if (c == null) {
            // Nobody found yet: sweep the streets round the landing spot, each of the team a different way.
            float a = e.member * World.TAU / 3 + this.w.time * 0.05f, r = 70 + ((int) (this.w.time / 20) % 3) * 60;
            float px = hx + (float) Math.cos(a) * r, py = hy + (float) Math.sin(a) * r;
            if (this.w.city.solidAt(px, py)) {
                float[] w = this.w.city.findWalkable(px, py);
                if (w != null) {
                    px = w[0];
                    py = w[1];
                }
            }
            if (World.hypot(px - e.x, py - e.y) > 12) this.w.wilds.walkToSpot(e, px, py, e.speed * 1.2f);
            else this.w.holdoutLife.standAt(e, px, py, 1);
            return true;
        }
        if (c.leader != e) {
            float dx = c.x - e.x, dy = c.y - e.y, d = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
            if (d > 14) this.w.wilds.walkToSpot(e, c.x, c.y, e.runSpeed);
            else {
                // "Come with me."
                c.leader = e;
                c.aware = true;
                e.talkTimer = 1.5f;
            }
            return true;
        }
        float dx = hx - e.x, dy = hy - e.y, d = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
        if (d > 16) this.w.wilds.walkToSpot(e, hx, hy, e.speed * 1.1f);
        else {
            // Here: wait for the helicopter.
            c.leader = null;
            c.task = Dispatch.T_RESCUE;
            c.postX = hx;
            c.postY = hy;
            e.escort = null;
        }
        return true;
    }

    /** Someone brought to a rescue team's landing spot: stays close, ducking away from the dead. */
    boolean waitForLift(Entity e, Entity threat, float threatDist) {
        if (e.task != Dispatch.T_RESCUE) return false;
        boolean team = false;
        for (int i = 0; i < w.fleet.vehicles.size() && !team; i++) {
            Fleet.Vehicle v = w.fleet.vehicles.get(i);
            if (v.kind == Fleet.K_RESCUE_HELI && Math.abs(v.tx - e.postX) < 1 && Math.abs(v.ty - e.postY) < 1) team = true;
        }
        if (!team) {
            e.task = Dispatch.T_NONE;
            return false;
        }
        if (threat != null && threatDist < 45) return false;
        // ((12.0) Each in a place of their own round the spot, near and further out: all on the one small ring, a big
        // group pushed and shuffled round it the whole time they waited.)
        float a = (e.nameSeed & 0xFF) / 256f * World.TAU, r = 8 + ((e.nameSeed >> 9) & 15);
        float sx = e.postX + (float) Math.cos(a) * r, sy = e.postY + (float) Math.sin(a) * r;
        if (w.city.solidAt(sx, sy)) {
            sx = e.postX + (float) Math.cos(a) * 8;
            sy = e.postY + (float) Math.sin(a) * 8;
        }
        w.holdoutLife.standAt(e, sx, sy, 0.6f);
        return true;
    }

    /** The rescue helicopter sets its survivors down where it lands, back at base. ((16.5) Not at a hospital: there's none.) */
    void dropRescued(Fleet.Vehicle v) {
        float x = v.x, y = v.y;
        int n = v.passengers;
        v.passengers = 0;
        for (int i = 0; i < n; i++) {
            Entity e = w.population.spawn(Entity.CIVILIAN, x + w.rnd.nextFloat() * 30 - 15, y + w.rnd.nextFloat() * 30 - 15);
            if (e == null) continue;
            e.aware = true;
            e.homeChecked = true;
        }
        w.dispatch.say(Dispatch.WHO_POLICE, null, "Rescue " + v.number + ": " + n + " survivor" + (n == 1 ? "" : "s") + " safe at base.", x, y);
        w.stats.highlight("Rescued: " + n, x, y);
    }
}
