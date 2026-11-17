package com.instantaxtion.zombiesandbox;

import static com.instantaxtion.zombiesandbox.Fleet.*;

import java.util.ArrayList;

/**
 * ((27.3) Part of the vehicle code, out of Fleet.) Survivors' armed pickups, and the vehicles of a convoy column (see Convoys).
 */
final class FleetPickups {
    final Fleet fleet;
    final World w;
    final City city;

    FleetPickups(Fleet fleet) {
        this.fleet = fleet;
        this.w = fleet.w;
        this.city = fleet.city;
    }

    // ------------------------------------------------------------------ (26.0) survivors' armed pickups

    int pickups() {
        int n = 0;
        for (int i = 0; i < fleet.vehicles.size(); i++) if (fleet.vehicles.get(i).kind == K_PICKUP && !fleet.vehicles.get(i).broken && !fleet.vehicles.get(i).parked) n++;
        return n;
    }

    int pickupsOf(SurvivorBases.Base b) {
        int n = 0;
        for (int i = 0; i < fleet.vehicles.size(); i++) {
            Vehicle v = fleet.vehicles.get(i);
            if (v.kind == K_PICKUP && v.pbase == b && !v.broken && !v.parked) n++;
        }
        return n;
    }

    /** A pickup with a gun in the bed, at (x, y), for base b (or none: a roaming crew), with crew aboard. */
    Vehicle makePickup(float x, float y, float angle, SurvivorBases.Base b, java.util.List<Entity> crew) {
        Vehicle v = fleet.make(CAR);
        v.kind = K_PICKUP;
        v.model = M_PICKUP;
        v.color = 0xFF6E6248;
        v.x = x;
        v.y = y;
        v.homeX = x;
        v.homeY = y;
        v.angle = angle;
        v.hp = v.maxHp = MAX_HP[CAR] * 1.6f;
        v.fuel = 1;
        v.state = CRUISE;
        v.pbase = b;
        v.pmode = b != null ? PK_HOME : PK_ROAM;
        v.pTimer = 5;
        v.number = ++fleet.unitCount;
        for (int i = 0; i < crew.size(); i++) fleet.crews.board(v, crew.get(i));
        fleet.vehicles.add(v);
        return v;
    }

    // ------------------------------------------------------------------ (26.0) convoy columns (see Convoys)

    /** One vehicle of a convoy column, at (x, y): an escort (a Humvee, or a BearCat for the police), a supply truck, a bus. */
    Vehicle convoyVehicle(int role, float x, float y, float angle, Convoys.Column c) {
        Vehicle v;
        if (role == Convoys.V_BUS) {
            v = fleet.make(CAR);
            v.model = M_BUS;
            v.color = 0xFFE8B030;
            v.hp = v.maxHp = MAX_HP[CAR] * 2.5f;
        } else if (role == Convoys.V_POLICE) {
            v = fleet.make(CRUISER);
            fleet.setKind(v, K_BEARCAT);
        } else {
            v = fleet.make(TRUCK);
            if (role == Convoys.V_ESCORT) fleet.setKind(v, K_HUMVEE);
        }
        v.x = x;
        v.y = y;
        v.homeX = x;
        v.homeY = y;
        v.angle = angle;
        v.fuel = 1;
        v.state = DRIVE;
        v.convoy = c;
        v.convoyRole = role;
        v.number = ++fleet.unitCount;
        v.reinforcement = true;
        v.place = c.place;
        fleet.vehicles.add(v);
        return v;
    }

    boolean routeTo(Vehicle v, float x, float y) {
        return pickupRoute(v, x, y);
    }

    /** A convoy vehicle's drive: along its way at no more than max, through the dead in the road. */
    boolean convoyDrive(Vehicle v, float dt, float max) {
        boolean going = fleet.driving.driveStep(v, dt, max, fleet.driving.carAhead(v) ? 0.5f : 1f);
        if (going) fleet.driving.hit(v, w.runOver(v.x, v.y, 9, v.speed, v.angle));
        return going;
    }

    /** A convoy's escort firing its mounted gun. */
    void convoyGun(Vehicle v, float dt) {
        if (v.convoyRole == Convoys.V_ESCORT || v.convoyRole == Convoys.V_POLICE) fleet.turret(v, dt);
    }

    /** Turns a parked car into a roaming crew's armed pickup. */
    void takeAsPickup(Vehicle v, java.util.List<Entity> crew) {
        v.kind = K_PICKUP;
        v.model = M_PICKUP;
        v.parked = false;
        v.standby = false;
        v.state = CRUISE;
        v.pbase = null;
        v.pmode = PK_ROAM;
        v.pTimer = 2;
        v.fuel = Math.max(v.fuel, 0.6f);
        for (int i = 0; i < crew.size(); i++) fleet.crews.board(v, crew.get(i));
    }

    /** The gunner in the back (the last aboard), firing at the nearest of the dead from the bed, with their own rounds. */
    void pickupGun(Vehicle v, float dt) {
        v.gunCd -= dt;
        if (v.gunCd > 0 || v.crew.size() < 2) return;
        Entity g = v.crew.get(v.crew.size() - 1);
        if (g.ammo + g.reserve <= 0) return;
        Entity z = w.nearestZombie(v.x, v.y, 170);
        if (z == null) {
            v.gunCd = 0.5f;
            return;
        }
        v.turret = (float) Math.atan2(z.y - v.y, z.x - v.x);
        if (g.ammo > 0) g.ammo--;
        else g.reserve--;
        if (w.turretShot(v.x, v.y, z, 16)) {
            v.burst++;
            v.gunCd = v.burst % 6 == 0 ? 1f : 0.16f;
        } else v.gunCd = 0.4f;
    }

    boolean pickupDrive(Vehicle v, float dt) {
        boolean going = fleet.driving.driveStep(v, dt, 80, fleet.driving.carAhead(v) ? 0.5f : 1f);
        if (going) fleet.driving.hit(v, w.runOver(v.x, v.y, 8, v.speed, v.angle));
        return going;
    }

    boolean pickupRoute(Vehicle v, float x, float y) {
        float[] p = city.nearestDrivable(x, y);
        return p != null && fleet.routing.route(v, p[0], p[1]);
    }

    /**
     * An armed pickup: a base's goes out after the dead near it, runs rounds to a base short of them, and comes back;
     * a roaming crew's drives from one lot of the dead to the next. Out of rounds, or down to one aboard, it's left.
     */
    boolean updatePickup(Vehicle v, float dt) {
        if (v.broken || v.crew.isEmpty()) {
            if (!v.crew.isEmpty()) fleet.crews.crewOut(v, false);
            v.parked = true;
            v.speed = 0;
            return false;
        }
        pickupGun(v, dt);
        Entity g = v.crew.get(v.crew.size() - 1);
        if (v.pbase != null && v.pbase.fallen) {
            v.pbase = null;
            v.pmode = PK_ROAM;
        }
        // Nothing left to shoot with: they leave it and go on foot.
        if (g.ammo + g.reserve <= 0 && (v.pbase == null || v.pmode != PK_BACK)) {
            if (v.pbase == null) {
                fleet.crews.crewOut(v, false);
                v.parked = true;
                v.speed = 0;
                return false;
            }
            v.pmode = PK_BACK;
            pickupRoute(v, v.homeX, v.homeY);
        }
        v.pTimer -= dt;
        switch (v.pmode) {
            case PK_HOME: {
                v.speed = Math.max(0, v.speed - dt * 150);
                // (Topped up from the base's workshop while it's home.)
                if (v.pbase != null && g.reserve < 40 && v.pbase.stash >= 20) {
                    v.pbase.stash -= 20;
                    g.reserve += 20;
                }
                if (v.pTimer > 0) return false;
                v.pTimer = 12 + w.rnd.nextFloat() * 10;
                SurvivorBases.Base b = v.pbase;
                // A base short of rounds, and this one with plenty: a supply run.
                SurvivorBases.Base poor = null;
                if (b.stash >= 100)
                    for (int i = 0; i < w.bases.size(); i++) {
                        SurvivorBases.Base o = w.bases.get(i);
                        if (o != b && !o.fallen && o.stash < 40 && (poor == null || o.stash < poor.stash)) poor = o;
                    }
                if (poor != null && pickupRoute(v, poor.b.doorX, poor.b.doorY)) {
                    v.pCargo = 60;
                    b.stash -= 60;
                    v.ptarget = poor;
                    v.pmode = PK_CONVOY;
                    w.dispatch.chatter(Dispatch.WHO_INFO, null, b.name + ": Supply run heading out to " + poor.name + ".", v.x, v.y);
                    return false;
                }
                // The dead gathering near the base: out after them.
                Entity z = w.nearestZombie(b.cx(), b.cy(), 650);
                if (z != null && World.hypot(z.x - b.cx(), z.y - b.cy()) > 120 && pickupRoute(v, z.x, z.y)) {
                    v.pmode = PK_SORTIE;
                    v.pTimer = 40;
                }
                return false;
            }
            case PK_SORTIE: {
                Entity z = w.nearestZombie(v.x, v.y, 70);
                // In among them: stop and let the gunner work.
                if (z != null && v.pTimer > 0) {
                    v.speed = Math.max(0, v.speed - dt * 200);
                    return false;
                }
                if (!pickupDrive(v, dt) || v.pTimer <= 0) {
                    if (w.nearestZombie(v.x, v.y, 150) != null && v.pTimer > -15) {
                        v.speed = 0;
                        return false;
                    }
                    v.pmode = PK_BACK;
                    if (!pickupRoute(v, v.homeX, v.homeY)) v.pmode = PK_HOME;
                }
                return false;
            }
            case PK_BACK:
            case PK_CONVOY: {
                if (pickupDrive(v, dt) && v.pTimer > -120) return false;
                if (v.pmode == PK_CONVOY) {
                    SurvivorBases.Base t = v.ptarget;
                    if (t != null && !t.fallen) {
                        t.stash += v.pCargo;
                        w.dispatch.chatter(Dispatch.WHO_INFO, null, t.name + ": The supply run's here - " + (int) v.pCargo + " rounds. Thank you!", v.x, v.y);
                    }
                    v.pCargo = 0;
                    v.ptarget = null;
                    v.pmode = PK_BACK;
                    v.pTimer = 0;
                    if (!pickupRoute(v, v.homeX, v.homeY)) v.pmode = PK_HOME;
                    return false;
                }
                v.pmode = v.pbase != null ? PK_HOME : PK_ROAM;
                v.pTimer = 15 + w.rnd.nextFloat() * 10;
                v.speed = 0;
                return false;
            }
            default: {
                // Roaming: from one lot of the dead to the next.
                if (w.nearestZombie(v.x, v.y, 90) != null) {
                    v.speed = Math.max(0, v.speed - dt * 200);
                    return false;
                }
                if (v.pTimer <= 0) {
                    v.pTimer = 25;
                    Entity z = w.nearestZombie(v.x, v.y, 1200);
                    if (z != null) pickupRoute(v, z.x, z.y);
                }
                if (!pickupDrive(v, dt)) v.speed = 0;
                return false;
            }
        }
    }
}
