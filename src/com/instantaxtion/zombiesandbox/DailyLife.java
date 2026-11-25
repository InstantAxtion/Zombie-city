package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Everyday life: jobs, homes, errands, outings and chats, while all is quiet.
 * ((27.3) Out of World.java, which holds the state these work on.)
 */
final class DailyLife {
    private final World w;

    DailyLife(World w) {
        this.w = w;
    }

    /**
     * The home a civilian lives in. Residents have theirs from the start; anyone who turns up later (spawned in,
     * a driver who got out) moved into the nearest house before 10.10, and since then has nowhere: homeless.
     */
    City.Building homeOf(Entity e) {
        if (!e.homeChecked) {
            e.homeChecked = true;
            if (w.city.cfg.nature()) return null;
            float bd = 400 * 400;
            for (int i = 0, n = w.city.buildings.size(); i < n; i++) {
                City.Building b = w.city.buildings.get(i);
                if ((b.kind != City.HOUSE && b.kind != City.APARTMENT) || b.capacity == 0) continue;
                float d = (b.doorX - e.x) * (b.doorX - e.x) + (b.doorY - e.y) * (b.doorY - e.y) + w.rnd.nextFloat() * 3000;
                if (d < bd) {
                    bd = d;
                    e.home = b;
                }
            }
        }
        if (e.home != null && e.home.collapsed) e.home = null;
        return e.home;
    }

    /**
     * Everyday life while the city is calm: people go shopping, to church or the park, visit the mall and head
     * home again, stopping a while at each. Once they've heard about the outbreak they go home and stay there.
     */
    boolean routine(Entity e, float dt) {
        if (e.leader != null || e.task != Dispatch.T_NONE || (e.hasGun && !e.aware)) return false;
        City.Building home = homeOf(e);
        // Word gets round: a phone call, a neighbour shouting, the radio.
        // (Not someone who's just come out into a street that's been quiet, unless the dead come back near.)
        boolean outAgain = w.time - e.emerged < 150 && w.exodus.dangerDistAt(e.x, e.y) >= 3;
        if (w.alert >= 1 && !e.aware && !outAgain && w.rnd.nextFloat() < dt * (w.alert >= 2 ? 10 : 0.05f)) e.aware = true;
        if (e.aware || (w.zombies > 0 && e.fear > 0)) {
            e.aware = true;
            return w.civilianAI.getToSafety(e, home);
        }
        if (e.job == Entity.J_NONE) assignJob(e);
        else if (e.work == null && (e.job == Entity.J_WORKER || e.job == Entity.J_SHOPKEEPER || e.job == Entity.J_STUDENT)) {
            // Back from a save: find the workplace again.
            e.work = e.job == Entity.J_WORKER ? pickWorkplace(e) : e.job == Entity.J_SHOPKEEPER ? pickShop(e) : nearestOfKind(e, City.SCHOOL);
            if (e.work == null) e.job = Entity.J_ERRANDS;
        }
        if (chatting(e, dt)) return true;
        e.errandTimer -= dt;
        if (e.errand == null && e.errandTimer <= 0 && outing(e, dt, home)) return true;
        if (e.errand == null || e.errand.collapsed) {
            if (e.errandTimer > 0) return false;
            e.errand = nextPlace(e, home);
            // Nowhere in particular to be: home, rather than drifting round the streets.
            if (e.errand == null && home != null && !home.collapsed
                    && (home.doorX - e.x) * (home.doorX - e.x) + (home.doorY - e.y) * (home.doorY - e.y) > 40 * 40) e.errand = home;
            e.lastErrand = e.errand;
            e.errandTimer = 0;
            if (e.errand != null) e.errand.heading++;
            if (e.errand == null) {
                e.errandTimer = 10 + w.rnd.nextFloat() * 20;
                return false;
            }
        }
        City.Building b = e.errand;
        float ddx = b.doorX - e.x, ddy = b.doorY - e.y;
        float d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
        if (d > 12) {
            w.movement.walkTo(e, b, ddx, ddy, d, e.speed);
            if (e.errandTimer < -90) e.errand = null; // Taking too long: give up.
            return true;
        }
        // Arrived: go in for a while. If it's full, don't queue: go somewhere else.
        visit(e, b);
        return false;
    }

    /** Goes inside on an errand, with the family. A full place is skipped rather than queued at. */
    void visit(Entity e, City.Building b) {
        e.errand = null;
        boolean home = b == e.home, work = b == e.work;
        if ((!home && !work && b.visitors.size() + b.occupants.size() >= Math.max(4, b.capacity)) || b.collapsed) {
            e.errandTimer = 1 + w.rnd.nextFloat() * 3;
            return;
        }
        float stay = work ? (e.job == Entity.J_SHOPKEEPER ? 220 + w.rnd.nextFloat() * 300 : e.job == Entity.J_STUDENT
                ? 120 + w.rnd.nextFloat() * 150 : 120 + w.rnd.nextFloat() * 220)
                : home ? (e.job == Entity.J_HOMEBODY ? 180 + w.rnd.nextFloat() * 400 : 80 + w.rnd.nextFloat() * 180)
                : b.kind == City.SCHOOL || b.kind == City.CHURCH ? 40 + w.rnd.nextFloat() * 50 : 15 + w.rnd.nextFloat() * 35;
        w.indoors.goInside(e, b, stay);
    }

    void assignJob(Entity e) {
        if (e.job != Entity.J_NONE) return;
        City.Building home = homeOf(e);
        // Children in a family go to school.
        boolean child = e.leader != null && e.leader.type == Entity.CIVILIAN && (e.nameSeed & 3) != 0;
        if (child) {
            e.job = Entity.J_STUDENT;
            e.work = nearestOfKind(e, City.SCHOOL);
            if (e.work == null) e.job = Entity.J_HOMEBODY;
            return;
        }
        boolean dog = false;
        // (No dogs since 10.18: no need to look through everyone for one.)
        for (int i = 0, n = w.counts[Entity.DOG] > 0 ? w.entities.size() : 0; i < n && !dog; i++)
            if (w.entities.get(i).leader == e && w.entities.get(i).type == Entity.DOG) dog = true;
        if (dog) {
            e.job = Entity.J_PARK;
            return;
        }
        // Out in the wilds: campers round the fire, hikers, anglers (and the odd townie out for a hike).
        if (w.city.cfg.wilds() && home != null) {
            float q = w.rnd.nextFloat();
            boolean camp = home.name != null && (home.name.startsWith("Camp Store") || home.name.startsWith("Cabin "));
            boolean hikes = !w.city.hikes.isEmpty();
            if (camp) {
                e.job = q < 0.4f ? Entity.J_CAMPER : q < 0.75f && hikes ? Entity.J_HIKER : Entity.J_FISHER;
                return;
            }
            if (!w.city.inTown(home.doorX, home.doorY)) {
                if (hikes && q < 0.16f) {
                    e.job = Entity.J_HIKER;
                    return;
                }
                if (q > 0.9f) {
                    e.job = Entity.J_FISHER;
                    return;
                }
            } else if (hikes && q < 0.025f) {
                e.job = Entity.J_HIKER;
                return;
            }
        }
        // (Counted as they're handed out, not by looking through everyone each time: a big city has tens of
        // thousands.)
        int posties = w.jobsGiven[0], vendors = w.jobsGiven[1], civs = w.counts[Entity.CIVILIAN];
        float r = w.rnd.nextFloat();
        if (posties < 1 + civs / 250 && r < 0.03f) e.job = Entity.J_POSTIE;
        else if (vendors < 1 + civs / 300 && r < 0.05f && !w.city.openAreas.isEmpty()) e.job = Entity.J_VENDOR;
        else if (r < 0.36f) e.job = Entity.J_WORKER;
        else if (r < 0.52f) e.job = Entity.J_HOMEBODY;
        else if (r < 0.70f) e.job = Entity.J_ERRANDS;
        else if (r < 0.79f) e.job = Entity.J_SHOPKEEPER;
        else if (r < 0.88f) e.job = Entity.J_JOGGER;
        // ((11.1) By the sea, half of those who'd go to the park go to the beach instead.)
        else if (r < 0.95f) e.job = r < 0.915f && !w.city.beachSpots().isEmpty() ? Entity.J_BEACH : Entity.J_PARK;
        else e.job = Entity.J_STUDENT;
        if (e.job == Entity.J_POSTIE) w.jobsGiven[0]++;
        if (e.job == Entity.J_VENDOR) w.jobsGiven[1]++;
        if (e.job == Entity.J_WORKER) e.work = pickWorkplace(e);
        else if (e.job == Entity.J_SHOPKEEPER) e.work = pickShop(e);
        else if (e.job == Entity.J_STUDENT) e.work = nearestOfKind(e, City.SCHOOL);
        if ((e.job == Entity.J_WORKER || e.job == Entity.J_SHOPKEEPER || e.job == Entity.J_STUDENT) && e.work == null)
            e.job = Entity.J_ERRANDS;
    }

    City.Building nearestOfKind(Entity e, int kind) {
        if (w.ofKindFor != w.city.buildings.size()) {
            w.ofKind.clear();
            w.ofKindFor = w.city.buildings.size();
        }
        ArrayList<City.Building> list = w.ofKind.get(kind);
        if (list == null) {
            list = new ArrayList<City.Building>();
            for (int i = 0, n = w.city.buildings.size(); i < n; i++) if (w.city.buildings.get(i).kind == kind) list.add(w.city.buildings.get(i));
            w.ofKind.put(kind, list);
        }
        City.Building best = null;
        float bd = Float.MAX_VALUE;
        for (int i = 0, n = list.size(); i < n; i++) {
            City.Building b = list.get(i);
            if (b.kind != kind || b.collapsed || b.doorX == 0) continue;
            float d = (b.doorX - e.x) * (b.doorX - e.x) + (b.doorY - e.y) * (b.doorY - e.y);
            if (d < bd) {
                bd = d;
                best = b;
            }
        }
        return best;
    }

    City.Building pickWorkplace(Entity e) {
        if (w.workplaces == null) {
            w.workplaces = new ArrayList<City.Building>();
            ArrayList<City.Building> offices = new ArrayList<City.Building>();
            for (City.Building b : w.city.buildings) {
                if (b.doorX == 0 || b.collapsed) continue;
                if (b.kind == City.HOSPITAL || b.kind == City.SCHOOL || b.kind == City.MALL || b.kind == City.MARKET
                        || b.kind == City.CITY_HALL || b.kind == City.COURTHOUSE || b.kind == City.CALL_CENTRE || b.kind == City.WORKS)
                    w.workplaces.add(b);
                else if (b.kind == City.OFFICE || b.kind == City.WAREHOUSE) offices.add(b);
            }
            java.util.Collections.shuffle(offices, w.rnd);
            // A limited set of workplaces, so the routes to them stay remembered.
            for (int i = 0; i < offices.size() && w.workplaces.size() < 50; i++) w.workplaces.add(offices.get(i));
        }
        City.Building best = null;
        float bestScore = Float.MAX_VALUE;
        for (int k = 0; k < 8 && !w.workplaces.isEmpty(); k++) {
            City.Building b = w.workplaces.get(w.rnd.nextInt(w.workplaces.size()));
            float dd = (float) World.hypot(b.doorX - e.x, b.doorY - e.y);
            if (dd > 1400) continue;
            float score = dd + w.rnd.nextFloat() * 300;
            if (score < bestScore) {
                bestScore = score;
                best = b;
            }
        }
        return best;
    }

    City.Building pickShop(Entity e) {
        pickErrand(e, null);
        City.Building best = null;
        float bd = Float.MAX_VALUE;
        for (int k = 0; k < 10 && w.errandSpots != null && !w.errandSpots.isEmpty(); k++) {
            City.Building b = w.errandSpots.get(w.rnd.nextInt(w.errandSpots.size()));
            if (b.kind != City.SHOP && b.kind != City.PHARMACY && b.kind != City.KIOSK && b.kind != City.MARKET) continue;
            float d = (float) World.hypot(b.doorX - e.x, b.doorY - e.y) + w.rnd.nextFloat() * 200;
            if (d < bd) {
                bd = d;
                best = b;
            }
        }
        return best;
    }

    /** Where someone goes next, by the life they lead. */
    City.Building nextPlace(Entity e, City.Building home) {
        switch (e.job) {
            case Entity.J_WORKER:
            case Entity.J_SHOPKEEPER:
            case Entity.J_STUDENT:
                if (e.work != null && !e.work.collapsed && e.lastErrand != e.work) return e.work;
                return home != null && w.rnd.nextFloat() < 0.65f ? home : pickErrand(e, home);
            case Entity.J_HOMEBODY:
                if (home != null && e.lastErrand != home) return home;
                return w.rnd.nextFloat() < 0.5f || home == null ? pickErrand(e, home) : home;
            default:
                return home != null && e.lastErrand != home && w.rnd.nextFloat() < 0.35f ? home : pickErrand(e, home);
        }
    }

    /** How many walking routes to keep: about 24 MB worth. */
    int fieldBudget() {
        return Math.max(12, (int) (24L * 1024 * 1024 / (2L * w.city.w * w.city.h)));
    }

    float[] pickArea(Entity e, boolean plaza) {
        float[] best = null;
        float bd = Float.MAX_VALUE;
        for (int k = 0; k < 8; k++) {
            float[] a = w.city.openAreas.get(w.rnd.nextInt(w.city.openAreas.size()));
            if (plaza && a[2] == 2) continue;
            float d = (float) World.hypot(a[0] - e.x, a[1] - e.y) + w.rnd.nextFloat() * 250;
            if (d < bd && d < 1100) {
                bd = d;
                best = a;
            }
        }
        return best;
    }

    /** Somewhere in an open area people can actually stand (its middle may be a statue or a flower bed). */
    float[] areaPoint(float[] a) {
        float[] p = w.areaPoints.get(a);
        if (p == null) {
            p = w.city.findWalkable(a[0], a[1]);
            if (p == null) p = a;
            w.areaPoints.put(a, p);
        }
        return p;
    }

    /** A street vendor at their stall. */
    /**
     * (11.1) A spot on the beach within reach of (x, y), at least min away (a walk along the sand goes on in
     * much the same direction), or null.
     */
    float[] pickBeach(Entity e, float x, float y, float reach, float min) {
        java.util.List<float[]> spots = w.city.beachSpots();
        float[] best = null;
        float bs = -Float.MAX_VALUE;
        for (int k = 0, n = spots.size(); k < n; k++) {
            float[] s = spots.get(k);
            float dx = s[0] - x, dy = s[1] - y, d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d > reach || d < min) continue;
            float score = w.rnd.nextFloat() * 60 - d * (min > 0 ? 0.1f : 0.03f);
            if (min > 0) score += 40 * (float) Math.cos(Math.atan2(dy, dx) - e.wanderAngle);
            if (score > bs) {
                bs = score;
                best = s;
            }
        }
        if (best != null && min > 0) e.wanderAngle = (float) Math.atan2(best[1] - y, best[0] - x);
        // ((15.9) A place of their own a little way from the spot, on open sand: everyone near went for the very same
        // spot, crowded round it and stood there pushing, never quite getting to it.)
        if (best != null) {
            for (int k = 0; k < 6; k++) {
                float jx = best[0] + (w.rnd.nextFloat() * 2 - 1) * 14, jy = best[1] + (w.rnd.nextFloat() * 2 - 1) * 14;
                int t = w.city.tileIndex(jx, jy);
                if (t >= 0 && !w.city.solid[t] && w.city.tiles[t] == City.SAND && !w.city.circleBlocked(jx, jy, e.radius + 1)) return new float[]{jx, jy};
            }
            return new float[]{best[0], best[1]};
        }
        return best;
    }

    /** (11.1) On a towel at the beach. */
    boolean onTowel(Entity e) {
        return e.job == Entity.J_BEACH && e.jobStep == 0 && e.spot != null && e.task == Dispatch.T_NONE
                && Math.abs(e.spot[0] - e.x) < 12 && Math.abs(e.spot[1] - e.y) < 12 && e.vx * e.vx + e.vy * e.vy < 9;
    }

    boolean atStall(Entity e) {
        if (e.job != Entity.J_VENDOR || e.spot == null) return false;
        float[] p = areaPoint(e.spot);
        return World.hypot(p[0] - e.x, p[1] - e.y) < 18;
    }

    /** Walks to an open area by its own route. Returns true once there. */
    boolean walkToArea(Entity e, float[] a, float near, float speed) {
        float[] p = areaPoint(a);
        float ddx = p[0] - e.x, ddy = p[1] - e.y, d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
        if (d < near) return true;
        City.WalkField f = w.fieldCache.get(a);
        if (f == null) {
            if (w.pathBudget < 1) {
                w.movement.wander(e, speed * 0.7f);
                return false;
            }
            w.pathBudget -= 1;
            f = w.city.walkField16(p[0], p[1], 260);
            w.fieldCache.put(a, f);
        }
        if (d < 24 || !w.movement.followField(e, f, speed)) w.movement.steer(e, ddx / d, ddy / d, speed);
        return false;
    }

    /**
     * Lives lived out in the open: a run along the pavements, an afternoon in the park (walking the dog), a
     * food stall at a plaza, a delivery round. Returns false when there's nothing to do outdoors (the
     * building trips take over), and sends them home when the outing is over.
     */
    boolean outing(Entity e, float dt, City.Building home) {
        switch (e.job) {
            case Entity.J_JOGGER: {
                e.jobTimer += dt;
                if (e.jobTimer > 70 + (e.nameSeed & 63)) break;
                if (e.wanderTimer > 4) e.wanderTimer = 4;
                w.movement.wander(e, e.speed * 1.9f);
                e.paused = false;
                return true;
            }
            case Entity.J_PARK: {
                if (e.spot == null) {
                    if (w.city.openAreas.isEmpty()) return false;
                    e.spot = pickArea(e, false);
                    if (e.spot == null) return false;
                    e.jobTimer = 0;
                }
                if (walkToArea(e, e.spot, 70, e.speed)) {
                    e.jobTimer += dt;
                    // Amble about the park, drifting back towards the middle rather than bouncing off an edge.
                    float[] mid = areaPoint(e.spot);
                    if (World.hypot(mid[0] - e.x, mid[1] - e.y) > 45 && e.jobStep != 1) {
                        // Head back in on a slant, so the next stretch takes them somewhere new in the park.
                        e.wanderAngle = (float) Math.atan2(mid[1] - e.y, mid[0] - e.x) + (w.rnd.nextFloat() - 0.5f) * 2.2f;
                        e.wanderTimer = 3 + w.rnd.nextFloat() * 3;
                        e.paused = false;
                        e.jobStep = 1;
                    } else if (World.hypot(mid[0] - e.x, mid[1] - e.y) < 30) e.jobStep = 0;
                    // Stop a while (on a bench, watching the dog), then wander on.
                    if (((int) ((e.jobTimer + (e.nameSeed & 7)) / 7)) % 2 == 0) w.movement.steer(e, 0, 0, 0);
                    else w.movement.wander(e, e.speed * 0.4f);
                }
                if (e.jobTimer > 60 + (e.nameSeed & 127)) {
                    e.spot = null;
                    break;
                }
                return true;
            }
            case Entity.J_BEACH: {
                // (11.1) Down to the beach: a towel on the sand for a while, or a walk along the water's edge.
                if (e.spot == null) {
                    e.spot = pickBeach(e, e.x, e.y, 1800, 0);
                    if (e.spot == null) return false;
                    e.jobTimer = 0;
                    e.dutyWait = 0;
                    e.jobStep = (e.nameSeed >> 3) % 3 == 0 ? 1 : 0;
                }
                float bx = e.spot[0] - e.x, by = e.spot[1] - e.y;
                // ((15.9) Near enough, held up by the crowd or the water's edge: here will do.)
                if (bx * bx + by * by < 40 * 40 && (e.blocked || e.stuckTime > 0.6f)) {
                    e.spot = new float[]{e.x, e.y};
                    bx = by = 0;
                }
                if (bx * bx + by * by > 12 * 12) {
                    // (Time on the way there counts too, so a beach that can't be reached is given up on.)
                    e.dutyWait += dt;
                    w.wilds.walkToSpot(e, e.spot[0], e.spot[1], e.jobStep == 1 && e.jobTimer > 0 ? e.speed * 0.55f : e.speed);
                } else {
                    e.jobTimer += dt;
                    if (e.jobStep == 1) {
                        float[] next = pickBeach(e, e.spot[0], e.spot[1], 110, 30);
                        if (next != null) e.spot = next;
                        else w.movement.steer(e, 0, 0, 0);
                    } else w.movement.steer(e, 0, 0, 0);
                }
                if (e.jobTimer > 90 + (e.nameSeed & 127) || e.dutyWait > 240) {
                    e.spot = null;
                    break;
                }
                return true;
            }
            case Entity.J_VENDOR: {
                if (e.spot == null) {
                    if (w.city.openAreas.isEmpty()) return false;
                    e.spot = pickArea(e, true);
                    if (e.spot == null) return false;
                    e.jobTimer = 0;
                }
                if (walkToArea(e, e.spot, 14, e.speed * 0.8f)) {
                    e.jobTimer += dt;
                    w.movement.steer(e, 0, 0, 0);
                }
                if (e.jobTimer > 300 + (e.nameSeed & 255)) {
                    e.spot = null;
                    break;
                }
                return true;
            }
            case Entity.J_POSTIE: {
                if (e.round == null || e.jobStep >= e.round.size()) {
                    if (e.round != null && !e.round.isEmpty()) {
                        e.round = null;
                        break;
                    }
                    e.round = new ArrayList<City.Building>();
                    e.jobStep = 0;
                    for (int i = 0, n = w.city.buildings.size(); i < n && e.round.size() < 7; i++) {
                        City.Building b = w.city.buildings.get((i * 37 + (e.nameSeed & 0x7FFF)) % n);
                        if (b.residents > 0 && !b.collapsed && World.hypot(b.doorX - e.x, b.doorY - e.y) < 320) e.round.add(b);
                    }
                    if (e.round.isEmpty()) {
                        e.round = null;
                        return false;
                    }
                }
                City.Building b = e.round.get(e.jobStep);
                float ddx = b.doorX - e.x, ddy = b.doorY - e.y, d = (float) Math.sqrt(ddx * ddx + ddy * ddy) + 0.001f;
                if (d > 14) {
                    w.movement.walkTo(e, b, ddx, ddy, d, e.speed * 1.1f);
                    return true;
                }
                // At the door: post goes through the letterbox.
                w.movement.steer(e, 0, 0, 0);
                e.jobTimer += dt;
                if (e.jobTimer > 2.5f) {
                    e.jobTimer = 0;
                    e.jobStep++;
                }
                return true;
            }
            case Entity.J_HIKER: {
                if (e.hike == null) {
                    if (!w.wilds.pickHike(e)) {
                        e.job = Entity.J_ERRANDS;
                        return false;
                    }
                }
                if (w.wilds.followHike(e, e.speed * 0.85f, dt)) return true;
                e.hike = null;
                break;
            }
            case Entity.J_FISHER: case Entity.J_CAMPER: {
                if (e.spot == null) {
                    e.spot = e.job == Entity.J_FISHER ? w.wilds.fishingSpot(e) : w.wilds.campfireSpot(e, home);
                    e.jobTimer = 0;
                    e.jobStep = 0;
                    if (e.spot == null) {
                        e.job = w.city.hikes.isEmpty() ? Entity.J_ERRANDS : Entity.J_HIKER;
                        return false;
                    }
                }
                float[] sp = e.spot;
                float sx = sp[0] - e.x, sy = sp[1] - e.y, sd = (float) Math.sqrt(sx * sx + sy * sy) + 0.001f;
                // ((33.0) Held up a few steps short - by a wall, a fence or someone else's spot: here will do, as on a
                // beach. They used to stand pushing at it for the best part of six minutes. Map version 61.)
                if (e.jobStep == 0 && sd > 6 && sd < 22 && (e.blocked || e.stuckTime > 0.5f) && w.city.cfg.civic >= 61) {
                    e.spot = sp = new float[]{e.x, e.y, sp[2]};
                    sd = 0.001f;
                }
                if (e.jobStep == 0 && sd > 6) {
                    w.wilds.walkToSpot(e, sp[0], sp[1], e.speed * 0.9f);
                    e.jobTimer += dt * 0.25f;
                } else {
                    // There: a line in the water, or a seat by the fire (facing the water, or the fire).
                    e.jobStep = 1;
                    e.jobTimer += dt;
                    if (sd > 8) w.movement.steer(e, sx / sd, sy / sd, e.speed * 0.5f);
                    else w.movement.steer(e, 0, 0, 0);
                    e.angle = World.turn(e.angle, sp[2], dt * 3);
                }
                if (e.jobTimer > (e.job == Entity.J_FISHER ? 90 : 70) + (e.nameSeed & 63)) {
                    e.spot = null;
                    e.jobStep = 0;
                    break;
                }
                return true;
            }
            case Entity.J_RELIEF: {
                // On the way up to the tower to take over.
                int k = e.jobStep;
                if (k < 0 || k >= w.city.fireTowers.size()) {
                    e.job = Entity.J_OFFDUTY;
                    return false;
                }
                float[] t = w.city.fireTowers.get(k);
                float dx = t[0] - e.x, dy = t[1] - e.y;
                if (dx * dx + dy * dy > 14 * 14) {
                    w.wilds.walkToSpot(e, t[0], t[1], e.speed);
                    return true;
                }
                w.wilds.takeOverTower(e, k);
                return true;
            }
            case Entity.J_OFFDUTY: {
                // Shift's over: back to the ranger station, and off home.
                City.Building b = w.rangerStation != null ? w.rangerStation : home;
                if (b == null) {
                    e.job = Entity.J_HOMEBODY;
                    return false;
                }
                float dx = b.doorX - e.x, dy = b.doorY - e.y, d = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
                if (d > 14) {
                    w.movement.walkTo(e, b, dx, dy, d, e.speed);
                    return true;
                }
                e.dead = true;
                e.removed = true;
                return true;
            }
            default:
                return false;
        }
        // The outing's over: home for a while.
        e.jobTimer = 0;
        e.errand = home != null ? home : pickErrand(e, home);
        e.lastErrand = e.errand;
        if (e.errand != null) e.errand.heading++;
        return false;
    }

    /** Neighbours stop for a chat now and then. */
    boolean chatting(Entity e, float dt) {
        if (e.chat > 0) {
            Entity o = e.chatWith;
            e.chat -= dt;
            if (o == null || o.dead || o.chatWith != e || o.fleeTimer > 0) {
                e.chat = 0;
                return false;
            }
            w.movement.steer(e, 0, 0, 0);
            e.angle = World.turn(e.angle, (float) Math.atan2(o.y - e.y, o.x - e.x), 0.2f);
            // They take turns talking.
            boolean mine = (((int) (e.chat / 2.2f)) % 2 == 0) == (e.nameSeed > o.nameSeed);
            e.talkTimer = mine ? 0.3f : 0;
            if (e.chat <= 0) {
                e.chatWith = null;
                e.errandTimer = Math.min(e.errandTimer, 0);
            }
            return true;
        }
        if (e.job == Entity.J_JOGGER || e.job == Entity.J_POSTIE || e.leader != null || w.rnd.nextFloat() > dt * 0.012f) return false;
        Entity o = w.spatial.nearestOfType(e, Entity.CIVILIAN, 14);
        if (o == null || o.chat > 0 || o.leader != null || o.task != Dispatch.T_NONE || o.fear > 0 || o.fleeTimer > 0
                || o.job == Entity.J_JOGGER || o == w.controlled || e == w.controlled) return false;
        float len = 4 + w.rnd.nextFloat() * 7;
        e.chat = o.chat = len;
        e.chatWith = o;
        o.chatWith = e;
        return true;
    }

    /** What someone's life is, for their card: "Nurse at City Hospital", "Postal worker". */
    String jobTitle(Entity e) {
        if (e.type != Entity.CIVILIAN || e.job == Entity.J_NONE) return null;
        String at = e.work != null ? World.placeLabel(e.work) : null;
        switch (e.job) {
            case Entity.J_WORKER:
                if (e.work == null) return "Works in town";
                switch (e.work.kind) {
                    case City.HOSPITAL: return "Nurse at " + at;
                    case City.SCHOOL: return "Teacher at " + at;
                    case City.WAREHOUSE: return "Warehouse worker";
                    case City.MALL: return "Works at " + at;
                    case City.MARKET: return "Cashier at " + at;
                    case City.CITY_HALL: return "Clerk at " + at;
                    case City.COURTHOUSE: return "Court officer at " + at;
                    case City.CALL_CENTRE: return "911 dispatcher at " + at;
                    case City.WORKS: return "Road crew, " + at;
                    default: return "Office worker";
                }
            case Entity.J_SHOPKEEPER: return "Runs " + at;
            case Entity.J_STUDENT: return "Student at " + at;
            case Entity.J_HOMEBODY: return "Spends most of the day at home";
            case Entity.J_ERRANDS: return "Always out and about";
            case Entity.J_JOGGER: return "Keen runner";
            case Entity.J_PARK: return "Loves the park";
            case Entity.J_BEACH: return "Beach lover";
            case Entity.J_POSTIE: return "Postal worker";
            case Entity.J_VENDOR: return "Street food vendor";
            case Entity.J_HIKER: return "Out hiking the trails";
            case Entity.J_FISHER: return "Keen angler";
            case Entity.J_CAMPER: return e.home != null && e.home.name != null ? "Camping at " + e.home.name.replace("Camp Store, ", "") : "Camping";
            case Entity.J_LOOKOUT: return "Fire lookout" + (e.jobStep >= 0 && e.jobStep < w.city.fireTowerNames.size() ? " on " + w.city.fireTowerNames.get(e.jobStep) : "");
            case Entity.J_RELIEF: return "Fire lookout, on the way to start a shift";
            case Entity.J_OFFDUTY: return "Fire lookout, off shift";
        }
        return null;
    }

    /** What someone is doing with their day right now, for their card. */
    String lifeActivity(Entity e) {
        if (e.chat > 0) return "Chatting with a neighbour";
        switch (e.job) {
            case Entity.J_JOGGER:
                if (e.errand == null) return "Out for a run";
                break;
            case Entity.J_PARK:
                if (e.spot != null) return "Spending time at " + w.city.areaName(e.spot);
                break;
            case Entity.J_BEACH:
                if (e.spot != null) return onTowel(e) ? "Sunbathing at the beach" : e.jobStep == 1 ? "Walking along the beach" : "Off to the beach";
                break;
            case Entity.J_VENDOR:
                if (e.spot != null)
                    return (atStall(e) ? "Selling food at " : "Pushing the food cart to ")
                            + w.city.areaName(e.spot);
                break;
            case Entity.J_POSTIE:
                if (e.round != null && e.errand == null) return "Delivering the post (" + Math.min(e.jobStep + 1, e.round.size()) + " of " + e.round.size() + ")";
                break;
        }
        if (e.errand != null) {
            if (e.errand == e.home) return "Heading home";
            if (e.errand == e.work) return e.job == Entity.J_STUDENT ? "On the way to school" : "On the way to work";
            return "Going to " + World.placeLabel(e.errand);
        }
        return "Taking a stroll";
    }

    /** Who is inside a building going about their day: {at home, at work, visiting}. */
    int[] insideCounts(City.Building b) {
        int[] c = new int[3];
        for (int i = 0, n = b.visitors.size(); i < n; i++) {
            Entity o = b.visitors.get(i);
            if (o.home == b) c[0]++;
            else if (o.work == b) c[1]++;
            else c[2]++;
        }
        return c;
    }

    /** Somewhere to go: a shop, supermarket, the mall, a church or school, or a pharmacy. */
    City.Building pickErrand(Entity e, City.Building home) {
        if (w.errandSpots == null) {
            w.errandSpots = new ArrayList<City.Building>();
            ArrayList<City.Building> shops = new ArrayList<City.Building>();
            for (City.Building b : w.city.buildings) {
                if (b.doorX == 0) continue;
                if (b.name != null) w.errandSpots.add(b);
                else if (b.kind == City.SHOP || b.kind == City.PHARMACY || b.kind == City.KIOSK) shops.add(b);
            }
            java.util.Collections.shuffle(shops, w.rnd);
            // A bigger city has more places to go (their routes are remembered, so not every shop).
            int want = Math.min(110, Math.max(24, w.city.buildings.size() / 5));
            for (int i = 0; i < shops.size() && w.errandSpots.size() < want; i++) w.errandSpots.add(shops.get(i));
        }
        if (w.errandSpots.isEmpty()) return null;
        City.Building best = null;
        float bestScore = Float.MAX_VALUE;
        for (int k = 0; k < 6; k++) {
            City.Building b = w.errandSpots.get(w.rnd.nextInt(w.errandSpots.size()));
            if (b.collapsed || b == e.lastErrand) continue;
            float d = (float) World.hypot(b.doorX - e.x, b.doorY - e.y);
            if (d > 700) continue;
            // Busy places put people off: nobody wants to queue.
            int busy = b.heading + b.visitors.size();
            if (busy >= Math.max(4, b.capacity) * 2) continue;
            float score = d + w.rnd.nextFloat() * 200 + busy * 40;
            if (score < bestScore) {
                bestScore = score;
                best = b;
            }
        }
        return best;
    }
}
