package com.instantaxtion.zombiesandbox;

import java.util.ArrayList;

/**
 * ((27.4) Who a unit is working for. Entity.task says what a unit is doing; a job says which system has it doing that
 * and how much it matters, so that no other system takes it away in the meantime - a command post's guards, a marksman
 * up on a roof, a crew walking out to their truck, a survivor base's guards, a cordon post, a special forces team.
 *
 * A system that wants a unit for something of its own asks isFree(e, priority) first: a unit on a job of lower priority
 * (a station post) can be taken for something that matters more (the command truck); one on a job that matters as much
 * or more is left alone. An emergency (the unit attacked, its post overrun) is the unit's own business, and always
 * comes first: the holder sees it and lets go.
 *
 * The holder claims the unit when it gives it the job and releases it when it's done with it. A claim remembers what the
 * unit was doing when it was claimed (task, call, vehicle): if that changes without the holder letting go, someone else
 * has taken the unit - a "yank" - which audit counts (and clears the claim, which no longer means anything).
 */
final class Jobs {
    /** How much a job matters: a post at a station or on a beat; a guard post, a cordon or a base's wall; a vehicle's
     *  crew or a marksman's roof; a mission (the command post, special forces). */
    static final int P_DUTY = 1, P_POST = 2, P_CREW = 3, P_MISSION = 4;

    /** ((27.4) Whether other systems keep their hands off claimed units: the game's way. Off, claims are only kept count
     *  of (for measuring what happens without them). */
    static boolean enforce = !"off".equals(System.getProperty("zc.jobs"));

    /** A claim on a unit. */
    static final class Job {
        final Object holder;
        final int priority;
        final String what;
        /** What the unit was doing when it was claimed: its task, call and vehicle. */
        final int task;
        final Dispatch.Incident incident;
        final Fleet.Vehicle rig;
        /** Checked by audit (most are); not, for a job whose unit goes through tasks of its own. */
        final boolean strict;

        Job(Object holder, int priority, String what, Entity e, boolean strict) {
            this.holder = holder;
            this.priority = priority;
            this.what = what;
            this.task = e.task;
            this.incident = e.incident;
            this.rig = e.rig;
            this.strict = strict;
        }
    }

    private final World w;
    /** Claims made and let go, and units taken off a job by someone other than its holder (see audit). */
    int claims, releases, yanks, refused, emergencies;
    /** Yanks by the job they were taken off. */
    final java.util.HashMap<String, Integer> yanked = new java.util.HashMap<String, Integer>();
    static boolean DEBUG = System.getProperty("zc.jobsdebug") != null;
    final java.util.TreeMap<String, Integer> debug = new java.util.TreeMap<String, Integer>();

    Jobs(World w) {
        this.w = w;
    }

    /** The holder gives e the job (call it once e's task, call and vehicle are set for it). */
    void claim(Entity e, Object holder, int priority, String what) {
        claim(e, holder, priority, what, true);
    }

    void claim(Entity e, Object holder, int priority, String what, boolean strict) {
        if (e == null) return;
        e.owner = new Job(holder, priority, what, e, strict);
        claims++;
    }

    /** The holder is done with e (no-op if someone else has it now). True if it was the holder's. */
    boolean release(Entity e, Object holder) {
        if (e == null || e.owner == null || e.owner.holder != holder) return false;
        e.owner = null;
        releases++;
        return true;
    }

    /** Lets go of every unit the holder has (a command post lost, a base fallen). */
    void releaseAll(Object holder) {
        for (int i = 0, n = w.entities.size(); i < n; i++) release(w.entities.get(i), holder);
    }

    /**
     * Whether e is free for a job of this priority: on no job, or on one that matters less. (Off: always, but a unit on
     * a job that would have been kept from it is counted.)
     */
    boolean isFree(Entity e, int priority) {
        if (e.owner == null || e.owner.priority < priority) return true;
        refused++;
        return !enforce;
    }

    /**
     * A grabber taking e for something of this priority that it doesn't claim itself (a call, a lift): true if it may,
     * and then e is off any job it was on (handed over, not yanked). False, and e is left be, if its job matters as much
     * or more. (Off: always true, and a job that would have kept it stays on e, for audit to find.)
     */
    boolean take(Entity e, int priority) {
        if (e.owner == null) return true;
        if (e.owner.priority < priority) {
            e.owner = null;
            return true;
        }
        refused++;
        return !enforce;
    }

    /** Whether e is free for this holder: its own units, or as isFree. */
    boolean isFree(Entity e, int priority, Object holder) {
        return e.owner != null && e.owner.holder == holder || isFree(e, priority);
    }

    /** One line for the unit's card: "On: command post guard", or null. */
    static String describe(Entity e) {
        return e.owner == null ? null : "On: " + e.owner.what;
    }

    /**
     * Once a second (Ops.step): every claim still matches what its unit is doing, or the unit was taken by someone else
     * - counted, and the claim cleared. The dead and those aboard a vehicle (off the map) are let go or skipped.
     */
    void audit() {
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            Job j = e.owner;
            if (j == null) continue;
            if (e.dead && !e.removed || e.isZombie()) {
                e.owner = null;
                continue;
            }
            if (e.removed || !j.strict) continue;
            if (e.task == j.task && e.incident == j.incident && e.rig == j.rig) continue;
            // (Doing the same thing, with a lift there or out of one - a cordon post across town: still on the job.)
            if (e.task == j.task && e.incident == j.incident) continue;
            if (e.task == Dispatch.T_BOARD || e.rideFrom != null) continue;
            // (Driven out to its own post - a car taking a crew to a gap in the cordon, a truck to a checkpoint.)
            if (e.incident != null && (e.incident.cordonPost >= 0 || e.incident.checkpointSpot != null)) continue;
            e.owner = null;
            // (The job's over, not taken: nothing to do now and no vehicle - nobody takes a unit only to stand it down -
            // or the vehicle it was for gone, or it let them out to their call, or the zone lifted.)
            if (e.task == Dispatch.T_NONE) continue;
            if (j.rig != null && (j.rig.removedFromFleet || j.rig.broken || e.rig == null)) continue;
            if (j.holder instanceof World.InfectionZone && (!w.infection.contains(j.holder) || !((World.InfectionZone) j.holder).critical)) continue;
            // (Off for rounds or a patch-up, or the dead on them: the unit's own business, not another system's - an
            // emergency always comes first.)
            if (e.task == Dispatch.T_RESUPPLY || e.task == Dispatch.T_BORROW || e.task == Dispatch.T_HEAL || e.fear > 0
                    || w.spatial.countZombiesNear(e.x, e.y, 60) > 0) {
                emergencies++;
                continue;
            }
            yanks++;
            Integer c = yanked.get(j.what);
            yanked.put(j.what, c == null ? 1 : c + 1);
            if (DEBUG) {
                String k = j.what + ": task " + j.task + "->" + e.task + (e.incident != j.incident ? " call" : "") + (e.rig != j.rig ? " rig" : "")
                        + (e.rideFrom != null ? " ride" : "") + " fear " + (e.fear > 0) + " dz " + (w.spatial.countZombiesNear(e.x, e.y, 60) > 0);
                Integer d = debug.get(k);
                debug.put(k, d == null ? 1 : d + 1);
            }
        }
        // A crew on their way to a vehicle are its, whoever sent them: nobody else's to send somewhere else.
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.owner == null && !e.dead && e.task == Dispatch.T_BOARD && e.rig != null && !e.rig.removedFromFleet && e.rideFrom == null)
                claim(e, e.rig, P_CREW, "crew of a " + vehicleWord(e.rig));
        }
    }

    private static String vehicleWord(Fleet.Vehicle v) {
        switch (v.type) {
            case Fleet.CRUISER: return "police car";
            case Fleet.TRUCK: return v.passengerType == Entity.COP ? "police truck" : "truck";
            case Fleet.HELI: return "helicopter";
            case Fleet.BOAT: return "boat";
            case Fleet.AMBULANCE: return "ambulance";
            default: return "vehicle";
        }
    }

    /** Whether e's job keeps it from one of this priority (no count kept): for the checks that only look. */
    boolean keeps(Entity e, int priority) {
        return enforce && e.owner != null && e.owner.priority >= priority;
    }

    /** Units on a job right now (for the probe and the test). */
    int held() {
        int n = 0;
        for (int i = 0, m = w.entities.size(); i < m; i++) if (w.entities.get(i).owner != null) n++;
        return n;
    }

    /** Lists the units the holder has. */
    ArrayList<Entity> of(Object holder) {
        ArrayList<Entity> out = new ArrayList<Entity>();
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if (e.owner != null && e.owner.holder == holder) out.add(e);
        }
        return out;
    }
}
