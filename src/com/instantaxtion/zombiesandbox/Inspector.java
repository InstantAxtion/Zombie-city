package com.instantaxtion.zombiesandbox;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.GameView.*;

/**
 * ((27.3) Out of GameView.) The cards for what you've tapped: a person, a vehicle, a building, a safe zone or a survivor base.
 */
final class Inspector {
    final GameView gv;

    Inspector(GameView gv) {
        this.gv = gv;
    }


    /**
     * ((33.0) Lines dropped from the end of a card until it fits between the buttons along the top and the tool bar
     * (and the undo button): on a phone held sideways a soldier's card ran up off the top of the screen.
     */
    void trimLines(java.util.List<String> lines, float lh, float extra) {
        float room = gv.barTop - 20 * gv.dp - (gv.lastAction.isEmpty() ? 0 : 44 * gv.dp) - (gv.topRects[0].bottom + 6 * gv.dp);
        while (lines.size() > 1 && 30 * gv.dp + lines.size() * lh + extra > room) lines.remove(lines.size() - 1);
    }

    /** A civilian with no home of their own (one who turned up after the city was built). */
    boolean homeless(Entity e) {
        return e.type == Entity.CIVILIAN && e.home == null && e.homeChecked && gv.world.city.cfg.nature();
    }

    /** A card about the person being followed: who they are, what they're doing and what they carry. */
    void drawInspect(Canvas c, Entity e) {
        java.util.ArrayList<String> lines = new java.util.ArrayList<String>();
        String title;
        if (e.type == Entity.DOG) title = Names.dog(e.nameSeed) + "  -  Dog";
        else if (e.type == Entity.ZOMBIE_DOG) title = Names.dog(e.nameSeed) + "  -  Zombie dog";
        else if (e.isUndead()) title = (e.armored ? "Armored " : "") + Entity.NAMES[e.type] + (e.origin >= 0 ? "  -  was " + Names.person(e.nameSeed) : "");
        else if (e.raider) title = Names.person(e.nameSeed) + "  -  Looter" + (e.leader == null ? ", gang leader" : "");
        else if (e.type == Entity.COP && e.role == Entity.ROLE_SWAT)
            title = Country.swat()[1] + " " + Names.person(e.nameSeed) + "  -  " + Dispatch.name(e);
        else if (e.type == Entity.COP && e.role == Entity.ROLE_SAR) title = "Rescue specialist " + Names.person(e.nameSeed) + "  -  " + Dispatch.name(e);
        else if (e.type == Entity.COP) title = (e.agency == 1 ? gv.world.city.country.hpName + " " + Names.copRank(e) + " "
                : e.agency == 2 ? gv.world.city.country.ruralName + "'s " + Names.copRank(e).toLowerCase(java.util.Locale.ROOT) + " "
                : e.agency == 3 ? "Park " + Names.copRank(e).toLowerCase(java.util.Locale.ROOT) + " "
                : e.agency == 4 ? Dispatch.federal(gv.world.city.country) + " " + Names.copRank(e).toLowerCase(java.util.Locale.ROOT) + " " : e.role == Entity.ROLE_PILOT ? "Pilot "
                // ((12.0) The specialists say what they are.)
                : e.role == Entity.ROLE_RIOT ? "Riot officer " : e.role == Entity.ROLE_K9 ? "K9 handler " : e.role == Entity.ROLE_MARKSMAN ? "Marksman "
                : e.role == Entity.ROLE_MOUNTED ? "Mounted officer " : e.role == Entity.ROLE_TRAFFIC ? "Traffic officer "
                : e.role == Entity.ROLE_PRISON_GUARD ? "Prison guard " : Names.copRank(e) + " ")
                + Names.person(e.nameSeed) + "  -  " + Dispatch.name(e);
        else if (e.type == Entity.SOLDIER) title = Names.soldierRank(e) + " " + Names.person(e.nameSeed) + "  -  " + Entity.ROLE_NAMES[e.role] + ", " + Dispatch.name(e);
        else if (homeless(e)) title = Names.person(e.nameSeed) + "  -  Homeless";
        // ((26.0) A survivor hero, and what someone did before.)
        else if (e.hero()) title = Names.hero(e.nameSeed) + "  -  Survivor hero" + (e.trade() != Entity.TR_NONE ? ", " + Entity.TRADE_NAMES[e.trade()].toLowerCase(java.util.Locale.ROOT) : "");
        else if (e.trade() != Entity.TR_NONE) title = Names.person(e.nameSeed) + "  -  " + Entity.TRADE_NAMES[e.trade()];
        else title = Names.person(e.nameSeed) + "  -  " + Entity.NAMES[e.type];
        lines.add(doing(e));
        // ((27.5) A civilian who signed up.)
        if (e.role == Entity.ROLE_VOLUNTEER && (e.type == Entity.COP || e.type == Entity.SOLDIER))
            lines.add((e.type == Entity.COP ? "Police auxiliary" : "Army volunteer") + (e.signedUpAt != null ? " (signed up at " + e.signedUpAt.name + ")" : ""));
        // ((27.4) Whose they are just now: a command post's guard, a roof's marksman, a truck's crew (see Jobs).)
        String job = Jobs.describe(e);
        if (job != null) lines.add(job);
        String cmd = gv.world.commanding.describe(e);
        if (cmd != null) lines.add(cmd);
        String life = gv.world.jobTitle(e);
        if (life != null) lines.add(life + (e.home != null ? ", lives on " + gv.world.city.placeName(e.home.doorX, e.home.doorY).split(" & ")[0]
                : homeless(e) ? ", no home" : ""));
        else if (homeless(e)) lines.add("Homeless: has nowhere of their own to go");
        String health = "Health " + Math.max(0, (int) e.hp) + "/" + (int) e.maxHp;
        if (e.infected) health += "   BITTEN: turns in " + Math.max(0, (int) e.infectTimer) + "s";
        if (e.fresh > 0) health += "   Freshly turned";
        lines.add(health);
        // ((25.0) The giant; an escaped inmate; a rioter.)
        if (e.type == Entity.GIANT) lines.add("A giant: shrugs off rifle fire and smashes through barricades - bring a squad, a tank or explosives");
        // ((27.0) The screamer and the armoured dead.)
        if (e.type == Entity.ARMOURED) lines.add("Armoured: body armour, and " + (e.helmet ? "a helmet that stops the first shot to the head" : "its helmet shot off"));
        if (e.escapee) lines.add("Escaped inmate: on the run from the police");
        if (e.riotUntil > gv.world.time) lines.add("Rioting");
        if (!e.canShoot() && e.melee > 0 && !e.isUndead()) lines.add("Carrying " + (e.melee == Entity.M_AXE ? "an axe" : "a " + Entity.MELEE_NAMES[e.melee].toLowerCase()));
        // ((12.0) What one of the dead was in life.)
        if (e.isUndead() && e.origin >= 0 && e.origin != Entity.CIVILIAN)
            lines.add(e.origin == Entity.COP ? "Was a police officer" : e.origin == Entity.SOLDIER ? "Was a soldier" : "Was a " + Entity.NAMES[e.origin].toLowerCase());
        // ((14.5) How far gone it is.)
        float rot = gv.world.rot(e);
        if (rot > 0.02f) lines.add((rot > World.ROT_MAX * 0.75f ? "Badly rotted" : "Rotting") + ": slower, and goes down easier");
        if (e.migrating) lines.add("Leading a horde across the city");
        // ((12.0) The gun by name, and a bat or axe if they carry one.)
        if (e.canShoot()) lines.add(e.gunName() + " " + e.ammo + " + " + e.reserve + (e.grenades > 0 ? "   Grenades " + e.grenades : "")
                + (e.melee > 0 ? "   " + Entity.MELEE_NAMES[e.melee] : "")
                + "   Aim +" + Math.round((World.aimBonus(e) + 0.05f * World.rank(e)) * 100) + "%" + (e.kills > 0 ? "   Kills " + e.kills : ""));
        // ((10.59) Their rank, with a star for each.)
        // ((23.0) Their level, experience and perks.)
        if (!e.isUndead() && e.type != Entity.DOG && (e.xp >= 1 || e.type == Entity.COP || e.type == Entity.SOLDIER)) {
            String perkStr = World.perkList(e);
            lines.add("Level " + (e.level + 1) + "   XP " + (int) e.xp + (e.level + 1 < World.LEVEL_XP.length ? "/" + World.LEVEL_XP[e.level + 1] : "")
                    + (perkStr.isEmpty() ? "" : "   " + perkStr));
        }
        int rk = World.rank(e);
        if (rk > 0 && !e.isUndead()) lines.add(World.RANK_NAMES[rk] + " " + "\u2605\u2605\u2605\u2605".substring(0, rk)
                + (rk < World.RANK_KILLS.length - 1 ? "   next at " + World.RANK_KILLS[rk + 1] + " kills" : ""));
        if (e.leader != null && !e.isUndead()) {
            String who = e.leader.type == Entity.DOG ? Names.dog(e.leader.nameSeed) : Names.person(e.leader.nameSeed);
            lines.add(e.type == Entity.DOG ? "Owner: " + who : e.type == Entity.SOLDIER ? "Sticking with " + who : "With " + who);
        }
        int followers = gv.world.followers(e);
        if (followers > 0 && !e.isArmed()) lines.add((e.task == Dispatch.T_PATROL ? "Leading a patrol of " : "Looking after ")
                + followers + (followers == 1 ? " other" : " others"));
        if (e.leadsHorde) lines.add("Leading a horde of about " + e.hordeSize);
        // Who bit whom.
        Integer biter = gv.world.bitBy.get(e.nameSeed);
        if (biter != null) lines.add((e.isUndead() ? "Turned by " : "Bitten by ") + Names.person(biter));
        Integer turnedN = gv.world.victims.get(e.nameSeed);
        if (e.isUndead() && turnedN != null) lines.add("Has turned " + turnedN + (turnedN == 1 ? " person" : " people"));
        if (e.nameSeed == gv.world.patientZeroSeed && gv.world.patientZeroSeed != 0) lines.add("PATIENT ZERO");
        // ((29.2) The "Lately" lines - the last few things they did - are off the card: it was too long.)
        gv.text.setTextAlign(Paint.Align.LEFT);
        gv.text.setTextSize(14 * gv.dp);
        float lh = 18 * gv.dp, cw = Math.min(gv.getWidth() - 20 * gv.dp, 420 * gv.dp);
        // ((22.6) Nothing cut short with "...": a title too long beside the buttons puts what they're doing (after the
        // dash) on the line below, and long lines go on to the next.)
        {
            float bw0 = cw < 380 * gv.dp ? 76 * gv.dp : 110 * gv.dp, room0 = cw - 14 * gv.dp - bw0 - 30 * gv.dp - 28 * gv.dp;
            int dash = title.indexOf("  -  ");
            if (gv.text.measureText(title) * 11.5f / 14 > room0 && dash > 0) {
                lines.add(0, title.substring(dash + 5));
                title = title.substring(0, dash);
            }
            gv.text.setTextSize(12.5f * gv.dp);
            java.util.List<String> wrapped = new java.util.ArrayList<String>();
            for (String ln : lines) wrapped.addAll(gv.hud.wrapLine(ln, cw - 24 * gv.dp));
            lines.clear();
            lines.addAll(wrapped);
            gv.text.setTextSize(14 * gv.dp);
        }
        // ((12.0) The first line clear of the star and Control buttons: below them, where it's too long to go beside.)
        boolean narrowCard = cw < 380 * gv.dp;
        float besideRoom = cw - 14 * gv.dp - (narrowCard ? 76 * gv.dp : 110 * gv.dp) - 30 * gv.dp - 28 * gv.dp;
        gv.text.setTextSize(12.5f * gv.dp);
        float drop = !lines.isEmpty() && gv.text.measureText(lines.get(0)) > besideRoom ? 10 * gv.dp : 0;
        gv.text.setTextSize(14 * gv.dp);
        // ((33.0) Never taller than the room under the buttons along the top: the last lines go instead.)
        trimLines(lines, lh, 8 * gv.dp + drop);
        float ch = 30 * gv.dp + lines.size() * lh + 8 * gv.dp + drop;
        float left = (gv.getWidth() - cw) / 2, top = gv.barTop - ch - 20 * gv.dp - (gv.lastAction.isEmpty() ? 0 : 44 * gv.dp);
        gv.oval.set(left, top, left + cw, top + ch);
        gv.fill.setColor(0xE8101216);
        c.drawRoundRect(gv.oval, 12 * gv.dp, 12 * gv.dp, gv.fill);
        gv.fill.setColor(e.isZombie() ? 0xFF7CC24E : e.isArmed() ? 0xFF4F7BE0 : 0xFFF0AD4E);
        c.drawRect(left, top + 10 * gv.dp, left + 4 * gv.dp, top + ch - 10 * gv.dp, gv.fill);
        gv.text.setColor(0xFFFFFFFF);
        // ((12.0) Cut to fit, clear of the buttons: a long name or a long street ran off the card, or under them.)
        // ((12.0) Narrower buttons on a narrow card, so the name still has room.)
        boolean narrow = cw < 380 * gv.dp;
        float bw = narrow ? 76 * gv.dp : 110 * gv.dp, bh = 30 * gv.dp;
        // (A little smaller before it's cut short.)
        float titleRoom = cw - 14 * gv.dp - bw - bh - 28 * gv.dp, titleW = gv.text.measureText(title);
        if (titleW > titleRoom) gv.text.setTextSize(Math.max(11.5f * gv.dp, 14 * gv.dp * titleRoom / titleW));
        c.drawText(gv.hud.fitText(title, titleRoom), left + 14 * gv.dp, top + 22 * gv.dp, gv.text);
        gv.text.setTextSize(12.5f * gv.dp);
        gv.text.setColor(0xFFB8BDC4);
        for (int i = 0; i < lines.size(); i++)
            c.drawText(gv.hud.fitText(lines.get(i), i == 0 && drop == 0 ? besideRoom : cw - 24 * gv.dp), left + 14 * gv.dp, top + 22 * gv.dp + drop + (i + 1) * lh, gv.text);
        // Pin them (a star), and take control of them.
        gv.takeRect.set(left + cw - bw - 10 * gv.dp, top + 8 * gv.dp, left + cw - 10 * gv.dp, top + 8 * gv.dp + bh);
        gv.pinRect.set(gv.takeRect.left - bh - 8 * gv.dp, gv.takeRect.top, gv.takeRect.left - 8 * gv.dp, gv.takeRect.bottom);
        boolean isPinned = gv.pinned.contains(e);
        gv.fill.setColor(isPinned ? 0xFFE8C547 : 0xFF2A2E34);
        c.drawRoundRect(gv.pinRect, 8 * gv.dp, 8 * gv.dp, gv.fill);
        gv.text.setTextAlign(Paint.Align.CENTER);
        gv.text.setTextSize(16 * gv.dp);
        gv.text.setColor(isPinned ? 0xFF1A1A1A : 0xFFE8C547);
        c.drawText("\u2605", gv.pinRect.centerX(), gv.pinRect.centerY() + 6 * gv.dp, gv.text);
        gv.fill.setColor(0xFF3A6EA5);
        c.drawRoundRect(gv.takeRect, 8 * gv.dp, 8 * gv.dp, gv.fill);
        gv.text.setTextSize(12.5f * gv.dp);
        gv.text.setTextAlign(Paint.Align.CENTER);
        gv.text.setColor(0xFFFFFFFF);
        c.drawText(narrow ? "Control" : "Take control", gv.takeRect.centerX(), gv.takeRect.centerY() + 4.5f * gv.dp, gv.text);
    }

    /** The card for a vehicle being followed: what it is, what it's doing, how fast, who's aboard, damage. */
    void drawVehicleCard(Canvas c, Fleet.Vehicle v) {
        java.util.ArrayList<String> lines = new java.util.ArrayList<String>();
        String title = gv.world.fleet.describe(v);
        String st = gv.world.fleet.status(v);
        if (st != null && st.indexOf(": ") > 0) st = st.substring(st.indexOf(": ") + 2);
        // ((12.0) And where an everyday car is going.)
        String to = v.field != null && (v.tx > 0 || v.ty > 0) ? gv.world.city.placeName(v.tx, v.ty) : null;
        lines.add(st != null ? st : v.parked ? "Parked" : (Math.abs(v.speed) < 3 ? "Waiting" : "Driving") + (to != null ? ", on the way to " + to : ""));
        // (Against the size of a car: about 0.15 m a unit at the pace things move here.)
        int kmh = Math.round(Math.abs(v.speed) * 0.55f);
        int aboard = v.crew.size() + v.riders.size() + v.passengers + (v.kind == Fleet.K_POLICE_BOAT ? 2 : 0);
        // ((11.3) A police car or army truck out with nobody else aboard has its own driver, by name.)
        String driver = v.driver != null ? (v.driver.type == Entity.COP ? Names.copRank(v.driver) + " " : v.driver.type == Entity.SOLDIER ? Names.soldierRank(v.driver) + " " : "")
                + Names.person(v.driver.nameSeed) : null;
        String people = aboard == 0 ? (v.player != null ? "you're driving" : driver != null ? "driver only (" + driver + ")"
                : v.occupied() ? "driver only" : "nobody aboard")
                : (aboard + (v.driver != null ? 1 : 0)) + " aboard";
        // ((12.0) A helicopter still on its pad isn't "flying".)
        boolean flying = Fleet.airborne(v) && (v.type != Fleet.HELI || v.alt > 0.05f);
        lines.add((flying ? "flying" : Fleet.airborne(v) ? "on the ground" : kmh < 2 ? "stopped" : kmh + " km/h") + (v.lightsOn() ? ", lights and siren" : "")
                + "  -  " + people + "  -  " + (v.maxHp > 0 ? Math.max(0, Math.round(100 * v.hp / v.maxHp)) + "% condition" : "")
                // ((12.0) And how much is in the tank: everyday cars run dry, and are left where they stop.)
                + (v.type == Fleet.CAR && v.player == null && v.model != Fleet.M_BUS && !v.broken ? "  -  fuel " + Math.round(v.fuel * 100) + "%" : ""));
        // ((12.0) Who's aboard, by name: the crew of a police car or truck, those who climbed in.)
        StringBuilder who = new StringBuilder();
        int named = 0, more = 0;
        for (int k = 0; k < v.crew.size() + v.riders.size(); k++) {
            Entity o = k < v.crew.size() ? v.crew.get(k) : v.riders.get(k - v.crew.size());
            if (o == v.driver) continue;
            if (named >= 3) {
                more++;
                continue;
            }
            who.append(named++ == 0 ? "" : ", ").append(Names.person(o.nameSeed));
        }
        if (named > 0) lines.add("With " + who + (more > 0 ? " and " + more + " more" : ""));
        // ((33.0) What the people aboard have put down between them, and how many are hurt: how a crew is doing at a glance.)
        int crewKills = v.driver != null && !v.crew.contains(v.driver) ? v.driver.kills : 0, hurt = 0;
        for (int k = 0; k < v.crew.size() + v.riders.size(); k++) {
            Entity o = k < v.crew.size() ? v.crew.get(k) : v.riders.get(k - v.crew.size());
            crewKills += o.kills;
            if (o.hp < o.maxHp * 0.35f) hurt++;
        }
        if (crewKills > 0 || hurt > 0)
            lines.add((crewKills > 0 ? "Kills by those aboard: " + World.big(crewKills) : "") + (crewKills > 0 && hurt > 0 ? "   " : "")
                    + (hurt > 0 ? World.count(hurt, "badly hurt", "badly hurt") : ""));
        // ((12.0) And its own people out of it, on foot at the scene.)
        if (v.type == Fleet.CRUISER || v.type == Fleet.TRUCK) {
            int out = gv.world.fleet.crewOnFoot(v);
            if (out > 0) lines.add((v.type == Fleet.CRUISER || Fleet.policeTruck(v) ? World.count(out, "officer", "officers") : World.count(out, "soldier", "soldiers")) + " out on foot");
        }
        gv.text.setTextAlign(Paint.Align.LEFT);
        gv.text.setTextSize(14 * gv.dp);
        float lh = 16 * gv.dp, cw = Math.min(gv.getWidth() - 20 * gv.dp, 420 * gv.dp);
        // ((12.0) A line too long for the card goes on to a second one rather than being cut short.)
        gv.text.setTextSize(12.5f * gv.dp);
        lines = gv.hud.wrapAll(lines, cw - 24 * gv.dp);
        gv.text.setTextSize(14 * gv.dp);
        // ((22.6) Long lines go on to the next instead of being cut short.)
        {
            float ts0 = gv.text.getTextSize();
            gv.text.setTextSize(12.5f * gv.dp);
            java.util.List<String> wrapped22 = new java.util.ArrayList<String>();
            for (String ln : lines) wrapped22.addAll(gv.hud.wrapLine(ln, cw - 24 * gv.dp));
            lines.clear();
            lines.addAll(wrapped22);
            gv.text.setTextSize(ts0);
        }
        // ((33.0) Never taller than the room under the buttons along the top: the last lines go instead.)
        trimLines(lines, lh, 8 * gv.dp);
        float ch = 30 * gv.dp + lines.size() * lh + 8 * gv.dp;
        float left = (gv.getWidth() - cw) / 2, top = gv.barTop - ch - 20 * gv.dp - (gv.lastAction.isEmpty() ? 0 : 44 * gv.dp);
        gv.oval.set(left, top, left + cw, top + ch);
        gv.fill.setColor(0xE8101216);
        c.drawRoundRect(gv.oval, 12 * gv.dp, 12 * gv.dp, gv.fill);
        gv.fill.setColor(v.lightsOn() ? 0xFF4F7BE0 : v.broken ? 0xFF8A9099 : 0xFFF0AD4E);
        c.drawRect(left, top + 10 * gv.dp, left + 4 * gv.dp, top + ch - 10 * gv.dp, gv.fill);
        gv.text.setColor(0xFFFFFFFF);
        c.drawText(gv.hud.fitText(title, cw - 24 * gv.dp), left + 14 * gv.dp, top + 22 * gv.dp, gv.text);
        gv.text.setTextSize(12.5f * gv.dp);
        for (int i = 0; i < lines.size(); i++) {
            gv.text.setColor(0xFFB8BDC4);
            c.drawText(gv.hud.fitText(lines.get(i), cw - 24 * gv.dp), left + 14 * gv.dp, top + 22 * gv.dp + (i + 1) * lh, gv.text);
        }
        // ((22.0) Drive it yourself: straight into the driver's seat, the joystick to steer.)
        gv.driveRect.setEmpty();
        if (gv.world.controlled == null && gv.world.canDrive(v)) {
            float bw = 74 * gv.dp, bh = 30 * gv.dp;
            gv.driveRect.set(left + cw - bw - 10 * gv.dp, top + 8 * gv.dp, left + cw - 10 * gv.dp, top + 8 * gv.dp + bh);
            gv.fill.setColor(0xFF2E7D4F);
            c.drawRoundRect(gv.driveRect, 8 * gv.dp, 8 * gv.dp, gv.fill);
            gv.text.setTextAlign(Paint.Align.CENTER);
            gv.text.setTextSize(13 * gv.dp);
            gv.text.setColor(0xFFFFFFFF);
            c.drawText(v.type == Fleet.HELI ? "Fly" : "Drive", gv.driveRect.centerX(), gv.driveRect.centerY() + 4.5f * gv.dp, gv.text);
            gv.text.setTextAlign(Paint.Align.LEFT);
        }
    }

    String doing(Entity e) {
        if (e.isUndead()) {
            // ((12.0) Knocked back, lunging, or lying low: it said "Wandering, hungry" for all three.)
            if (e.stun > 0) return "Knocked back, dazed";
            if (e.charge > 0) return "Lunging at someone";
            if (e.feedTimer > 0) return "Feeding";
            if (e.doorAt != null && e.doorTime > 0) return "Clawing at the door of " + (e.doorAt.name != null ? e.doorAt.name : "a building");
            if (e.memory > 0) return "Chasing someone it saw";
            if (e.searchTimer > 0) return "Sniffing about where it lost them";
            if (e.leadsHorde && e.hordeSize > 1) return "Leading a horde of " + e.hordeSize;
            if (e.hordeLeader != null) return "Shambling along with a horde";
            if (e.noiseTimer > 0) return "Heading for a noise";
            return "Wandering, hungry";
        }
        String s = doingNow(e);
        // ((11.3) And how they're holding up.) ((12.0) Both, when it's both.)
        boolean hurt = e.hp < e.maxHp * 0.35f;
        if (hurt && e.infected) s += " (badly hurt, bitten)";
        else if (hurt) s += " (badly hurt)";
        else if (e.infected) s += " (bitten)";
        return s;
    }

    /** (11.3) The living: what they're doing right now, most pressing first. */
    String doingNow(Entity e) {
        if (e.stun > 0) return "Dazed";
        if (e.aiming) return e.ammo <= 0 && e.reload > 0 ? "Reloading under fire" : "Shooting";
        if (e.isArmed() && e.reload > 0) return "Reloading";
        if (e.fleeTimer > 0 && !e.isArmed()) return "Running for their life";
        if (e.ride != null) return "Riding along";
        // ((12.0) Survivors' groups, lookouts and pilots called up: it said "On patrol", "Taking a stroll" or "Going to sign up".)
        if (e.type == Entity.CIVILIAN) {
            String group = e.holdout != null && !e.holdout.gone && e.holdout.name != null ? e.holdout.name : null;
            if (e.militia) return "Defending " + (group != null ? group : "their street") + " with the militia";
            if (e.scavenge != null) return "Scavenging supplies" + (group != null ? " for " + group : "");
            if (e.homeward) return "Bringing supplies back" + (group != null ? " to " + group : "");
            if (e.job == Entity.J_LOOKOUT && e.aloft) return "Keeping lookout from the fire tower";
            if (e.task == Dispatch.T_ENLIST && e.pilotFor != null) return "Called up: on the way to fly the helicopter";
        }
        // ((12.0) Run themselves out: slowed to a jog until they get their breath back.)
        float sp = (float) Math.sqrt(e.vx * e.vx + e.vy * e.vy);
        boolean sprinting = e.isArmed() && sp > e.runSpeed * 1.08f;
        if (e.isArmed() && e.winded && sp > 2 && e.task == Dispatch.T_RESPOND && !e.onScene) return "Out of breath, jogging on to " + (e.incident != null ? e.incident.place : "the call");
        switch (e.task) {
            case Dispatch.T_RESPOND: return (e.onScene ? "At the scene" : sprinting ? "Sprinting to a 911 call" : "Responding to a 911 call") + (e.incident != null ? " on " + e.incident.place : "");
            case Dispatch.T_GUARD: return "Guarding the " + (e.zone != null ? e.zone.place + " " : "") + "safe zone";
            case Dispatch.T_SEEK: return gv.world.queueingAtGate(e) ? "Queueing at the safe zone gate" : "Heading for a safe zone";
            case Dispatch.T_SHELTER: return "Sheltering in a safe zone";
            case Dispatch.T_POST: {
                // ((12.0) Where, and whether they've had to step back from it.)
                float pd = (float) Math.hypot(e.postX - e.x, e.postY - e.y);
                if (pd > 30 && e.aiming) return "Falling back from their post, shooting";
                if (pd > 30) return "Heading back to their post";
                String at = gv.world.postName(e.postX, e.postY);
                return at != null ? "On guard at " + at : "On guard duty";
            }
            case Dispatch.T_CORDON: return "Holding the perimeter";
            case Dispatch.T_EVACUATE: return "Getting out of the danger zone";
            case Dispatch.T_LEAVE: return "Fleeing the city";
            case Dispatch.T_HOMECOMING: return "Coming home";
            case Dispatch.T_SQUAD: return "Following you";
            case Dispatch.T_TRAIN: return e.trainT > 0 ? "Training to fly the helicopter (" + Math.min(99, (int) (e.trainT * 100 / World.PILOT_TRAINING)) + "%)" : "Going to train as a pilot";
            case Dispatch.T_BORROW: return "Borrowing ammo";
            case Dispatch.T_MOVE: return "Following orders";
            case Dispatch.T_HOLD: return "Holding position";
            case Dispatch.T_HIDE: return "Running to hide indoors";
            case Dispatch.T_PICKUP: return e.hasGun ? "Picking up rounds lying in the street" : "Grabbing a dropped gun";
            case Dispatch.T_RESUPPLY: return "Going to restock ammo";
            // ((27.5) In hospital, or waiting for the ambulance.)
            case Dispatch.T_HEAL: return e.inCare ? "In hospital, " + (int) (100 * e.hp / Math.max(1, e.maxHp)) + "%"
                    : e.waitPickup ? (e.casevac != null ? "Wounded: the ambulance is on its way" : "Wounded: waiting for an ambulance") : "Going to get patched up";
            case Dispatch.T_RIDE: return "Running for a car";
            case Dispatch.T_ENLIST: return "Going to sign up";
            case Dispatch.T_CLEANUP: return "Clearing away bodies";
            case Dispatch.T_LOOT: return "On a supply run";
            case Dispatch.T_PATROL: return "Leading an armed patrol";
            // ((12.0) The rescue: it said "On patrol", or nothing at all for those being rescued.)
            case Dispatch.T_RESCUE:
                if (e.isArmed()) return e.roping > 0 ? "Coming down the winch line" : e.escort != null ? "Bringing " + Names.person(e.escort.nameSeed) + " to the helicopter" : "Searching for people to bring out";
                return "Waiting to be winched up";
            case Dispatch.T_BOARD:
                // ((12.0) A ride on its way: waiting where they are for it.)
                if (e.rig != null && e.rig.pickup && e.rig.state == Fleet.DRIVE) return "Waiting for their ride (" + gv.world.fleet.describe(e.rig).split("  -  ")[0].toLowerCase() + " on its way)";
                return e.rig != null ? "Getting into the " + gv.world.fleet.describe(e.rig).split("  -  ")[0].toLowerCase() : "Mounting up";
        }
        if (e.isArmed()) {
            if (e.ammo <= 0 && e.reserve <= 0) return "Out of ammo";
            if (e.role == Entity.ROLE_PILOT) return "Standing by for the helicopter";
            String duty = gv.world.dutyText(e);
            if (duty != null) return duty;
            if (e.leader != null) return "Covering " + Names.person(e.leader.nameSeed);
            return e.type == Entity.SOLDIER ? "Keeping watch" : "On patrol";
        }
        if (e.leader != null) return "Staying close to " + Names.person(e.leader.nameSeed);
        return e.type == Entity.CIVILIAN ? gv.world.lifeActivity(e) : "Going about their day";
    }

    /** (25.1) A survivor base's card: its people and guards, its wall, towers, gates and workshop. */
    void drawBaseInspect(Canvas c, SurvivorBases.Base s) {
        java.util.ArrayList<String> lines = new java.util.ArrayList<String>();
        String title = (s.raiders ? "Raiders  -  " : "Survivor base  -  ") + s.name;
        // ((27.5) Its ground and its raids.)
        if (s.raiders) lines.add("Raiders: they take in nobody unarmed, and rob convoys and other bases");
        int up = SurvivorBases.wallsUp(gv.world.city, s), want = s.ring.length - s.gates.length;
        lines.add(!s.walled ? "Building its wall: " + up + " of " + want + " sections up"
                : "A " + SurvivorBases.TIER_NAMES[s.tier] + " wall all the way round" + (s.tier < 2 ? " (" + (s.tier == 0 ? "sandbags" : "concrete") + " next)" : ""));
        lines.add(s.members + (s.members == 1 ? " person" : " people") + "   " + s.guards + (s.guards == 1 ? " guard" : " guards") + " on the wall");
        int shut = 0;
        for (int g : s.gates) if (gv.world.city.tiles[g] == City.FENCE) shut++;
        lines.add(s.towersUp + (s.towersUp == 1 ? " watchtower" : " watchtowers") + "   " + s.gates.length + (s.gates.length == 1 ? " gate" : " gates")
                + (shut > 0 ? " (shut)" : " (open)"));
        lines.add(s.walled ? "Workshop: " + (int) s.stash + " rounds in store" : "Workshop: once the wall is up");
        if (s.territory > 0 || s.raids > 0) {
            BaseRaids.Raid r = gv.world.baseRaids.raidOf(s);
            lines.add("Territory: " + s.territory + (s.territory == 1 ? " block" : " blocks") + "   Raids: " + s.raids
                    + (r != null ? " (" + r.team.size() + " out)" : ""));
        }
        if (s.robbed + s.struck > 0) lines.add("Held up " + s.robbed + (s.robbed == 1 ? " vehicle" : " vehicles") + ", robbed " + s.struck + (s.struck == 1 ? " base" : " bases"));
        if (s.inside > 0) lines.add("The dead are inside the wall!");
        gv.text.setTextAlign(Paint.Align.LEFT);
        float lh = 18 * gv.dp, cw = Math.min(gv.getWidth() - 20 * gv.dp, 460 * gv.dp);
        gv.text.setTextSize(12.5f * gv.dp);
        java.util.ArrayList<String> wrapped = new java.util.ArrayList<String>();
        for (String ln : lines) wrapped.addAll(gv.hud.wrapLine(ln, cw - 24 * gv.dp));
        lines = wrapped;
        // ((33.0) Never taller than the room under the buttons along the top: the last lines go instead.)
        trimLines(lines, lh, 8 * gv.dp);
        float ch = 30 * gv.dp + lines.size() * lh + 8 * gv.dp;
        float left = (gv.getWidth() - cw) / 2, top = gv.barTop - ch - 20 * gv.dp - (gv.lastAction.isEmpty() ? 0 : 44 * gv.dp);
        gv.oval.set(left, top, left + cw, top + ch);
        gv.fill.setColor(0xE8101216);
        c.drawRoundRect(gv.oval, 12 * gv.dp, 12 * gv.dp, gv.fill);
        gv.fill.setColor(s.raiders ? 0xFFC42A1E : 0xFFD8B04A);
        c.drawRect(left, top + 10 * gv.dp, left + 4 * gv.dp, top + ch - 10 * gv.dp, gv.fill);
        gv.text.setTextSize(14 * gv.dp);
        gv.text.setColor(0xFFFFFFFF);
        c.drawText(gv.hud.fitText(title, cw - 24 * gv.dp), left + 14 * gv.dp, top + 22 * gv.dp, gv.text);
        gv.text.setTextSize(12.5f * gv.dp);
        for (int i = 0; i < lines.size(); i++) {
            String l = lines.get(i);
            gv.text.setColor(l.startsWith("The dead") || l.startsWith("Raiders") ? 0xFFFF8A6A : 0xFFB8BDC4);
            c.drawText(gv.hud.fitText(l, cw - 24 * gv.dp), left + 14 * gv.dp, top + 22 * gv.dp + (i + 1) * lh, gv.text);
        }
        // Its ground outlined on the map.
        gv.stroke.setColor(0xCCFFD24A);
        gv.stroke.setStrokeWidth(2 * gv.dp);
        c.drawRect(gv.screenX(s.tx0 * City.T), gv.screenY(s.ty0 * City.T), gv.screenX((s.tx1 + 1) * City.T), gv.screenY((s.ty1 + 1) * City.T), gv.stroke);
    }

    /** (12.0) A safe zone's card: how it's holding, who's inside, its guards, food, ammunition and wall. */
    void drawZoneInspect(Canvas c, Dispatch.SafeZone z) {
        java.util.ArrayList<String> lines = new java.util.ArrayList<String>();
        String title = (z.military ? "Military" : "Police") + " safe zone  -  " + z.place;
        if (!z.open) lines.add("Walling it in: " + (int) (z.built * 100) + "% done" + (z.onSite == 0 ? " (the guards are on their way)" : ""));
        else lines.add(z.fallingBack ? "The dead are inside the wall! Falling back on the tents"
                : z.full ? "Full: the gates are shut to newcomers" : "Open: taking people in at the gates");
        lines.add("Sheltering " + z.sheltered + " of " + z.capacity + "   Guards " + z.onSite + " here" + (z.guards > z.onSite ? ", " + (z.guards - z.onSite) + " on the way" : ""));
        int open = 0;
        for (boolean g : z.gateOpen) if (g) open++;
        lines.add("Food " + (int) z.food + "/" + (int) z.foodMax + "   Ammunition " + z.ammo + "   Gates " + z.gateOpen.length + (open > 0 ? " (" + open + " open)" : ""));
        lines.add(z.breaches > 0 ? z.breaches + (z.breaches == 1 ? " breach" : " breaches") + " in the wall" : "The wall is holding");
        gv.text.setTextAlign(Paint.Align.LEFT);
        gv.text.setTextSize(14 * gv.dp);
        float lh = 18 * gv.dp, cw = Math.min(gv.getWidth() - 20 * gv.dp, 460 * gv.dp);
        // ((12.0) A line too long for the card goes on to a second one rather than being cut short.)
        gv.text.setTextSize(12.5f * gv.dp);
        lines = gv.hud.wrapAll(lines, cw - 24 * gv.dp);
        gv.text.setTextSize(14 * gv.dp);
        // ((22.6) Long lines go on to the next instead of being cut short.)
        {
            float ts0 = gv.text.getTextSize();
            gv.text.setTextSize(12.5f * gv.dp);
            java.util.List<String> wrapped22 = new java.util.ArrayList<String>();
            for (String ln : lines) wrapped22.addAll(gv.hud.wrapLine(ln, cw - 24 * gv.dp));
            lines.clear();
            lines.addAll(wrapped22);
            gv.text.setTextSize(ts0);
        }
        // ((33.0) Never taller than the room under the buttons along the top: the last lines go instead.)
        trimLines(lines, lh, 8 * gv.dp);
        float ch = 30 * gv.dp + lines.size() * lh + 8 * gv.dp;
        float left = (gv.getWidth() - cw) / 2, top = gv.barTop - ch - 20 * gv.dp - (gv.lastAction.isEmpty() ? 0 : 44 * gv.dp);
        gv.oval.set(left, top, left + cw, top + ch);
        gv.fill.setColor(0xE8101216);
        c.drawRoundRect(gv.oval, 12 * gv.dp, 12 * gv.dp, gv.fill);
        gv.fill.setColor(z.military ? 0xFF6FBF3F : 0xFF4F8FE0);
        c.drawRect(left, top + 10 * gv.dp, left + 4 * gv.dp, top + ch - 10 * gv.dp, gv.fill);
        gv.text.setColor(0xFFFFFFFF);
        c.drawText(gv.hud.fitText(title, cw - 24 * gv.dp), left + 14 * gv.dp, top + 22 * gv.dp, gv.text);
        gv.text.setTextSize(12.5f * gv.dp);
        for (int i = 0; i < lines.size(); i++) {
            String l = lines.get(i);
            gv.text.setColor(l.startsWith("The dead") || l.contains("breach") ? 0xFFFF8A6A : 0xFFB8BDC4);
            c.drawText(gv.hud.fitText(l, cw - 24 * gv.dp), left + 14 * gv.dp, top + 22 * gv.dp + (i + 1) * lh, gv.text);
        }
    }

    /** The building card: what it is, where, who is inside, what's left in it, and what it does for the city. */
    void drawBuildingInspect(Canvas c, City.Building b) {
        java.util.ArrayList<String> lines = new java.util.ArrayList<String>();
        String kind = b.typeName() != null ? b.typeName() : b.kind == City.SHOP ? (b.shopType == 2 ? "Diner" : "Shop")
                : KIND_NAMES[Math.min(KIND_NAMES.length - 1, b.kind)];
        City.District d = gv.world.city.districtOf((b.x0 + b.x1) / 2, (b.y0 + b.y1) / 2);
        // ((12.0) Not "City Hall - City hall": the kind only when the name doesn't already say it.)
        String title = (b.name != null ? (b.name.equalsIgnoreCase(kind) ? b.name : b.name + "  -  " + kind) : kind) + (d != null ? "  -  " + d.name : "");
        // ((12.0) Too long for the card: the district goes on a line of its own under it, instead of off the end.)
        gv.text.setTextSize(14 * gv.dp);
        if (d != null && gv.text.measureText(title) > Math.min(gv.getWidth() - 20 * gv.dp, 460 * gv.dp) - 24 * gv.dp) {
            title = b.name != null ? (b.name.equalsIgnoreCase(kind) ? b.name : b.name + "  -  " + kind) : kind;
            lines.add("In " + d.name);
        }
        if (b.collapsed) lines.add("Collapsed: just a pile of rubble now");
        // ((12.0) With people still in there fighting them, that's what it says - not "Keep out!" as if they'd gone.)
        else if (b.lurkers > 0 && !b.occupants.isEmpty())
            lines.add("FIGHTING: " + World.count(b.occupants.size(), "person", "people") + " inside against " + (b.lurkers == 1 ? "one of the dead" : "about " + b.lurkers + " of the dead"));
        else if (b.infestKnown && b.lurkers > 0) lines.add("INFESTED: " + (b.lurkers == 1 ? "one of them" : "about " + b.lurkers + " of them") + " inside. Keep out!");
        else if (!b.occupants.isEmpty())
            lines.add(b.occupants.size() + " hiding inside, door barricaded (" + Math.max(0, (int) b.barricade) + "%)");
        if (!b.visitors.isEmpty()) {
            int[] in = gv.world.insideCounts(b);
            String s = "Inside: ";
            if (in[0] > 0) s += in[0] + " at home";
            if (in[1] > 0) s += (in[0] > 0 ? ", " : "") + in[1] + (b.kind == City.SCHOOL ? " at school" : " at work");
            if (in[2] > 0) s += (in[0] + in[1] > 0 ? ", " : "") + in[2] + " visiting";
            lines.add(s);
        }
        // ((12.0) "Empty" only when it is: with people hiding inside it said both at once.)
        else if (!b.occupants.isEmpty() || b.collapsed) {
            // ((29.2) No "Room for so many more" line either.)
        }
        else if (b.infestKnown) lines.add("Zombies were inside. Nobody has gone back in.");
        // ((29.2) No "Empty (room for so many to hide)" line on the card any more.)
        if (!b.collapsed) {
            String supplies = "Food: " + b.food + (b.food == 0 && !b.occupants.isEmpty() ? " (they're starving)" : "");
            if (b.kind == City.MARKET || World.useOf(b) == Variants.U_ROUNDS)
                supplies += "   Ammo: " + b.stock + " rounds";
            if (b.kind == City.PHARMACY) supplies += "   Medicine: " + b.stock;
            lines.add(supplies);
            if (b.smashed || b.looted) lines.add((b.smashed ? "Windows smashed" : "") + (b.smashed && b.looted ? ", " : "")
                    + (b.looted ? "shelves stripped bare" : ""));
        }
        String role = null;
        // ((33.0) No "can become a safe zone" on the maps that have none (map version 58 on).)
        boolean zones = gv.world.city.cfg.civic < 58;
        switch (b.kind) {
            case City.POWER: role = gv.world.blackout ? "BLACKOUT: overrun. Clear the zombies out to get the power back." : "Keeps the lights, sirens and broadcasts on"; break;
            case City.HOSPITAL: role = "A big place to work, and to hide"; break;
            case City.STATION: role = "Police armoury: officers restock here"; break;
            case City.BARRACKS: role = "Army barracks: soldiers restock at the base"; break;
            case City.MARKET: role = "Food and hunting ammo" + (zones ? "; can become a safe zone" : ""); break;
            case City.MALL: role = "Plenty of food and room" + (zones ? "; can become a safe zone" : ""); break;
            case City.SCHOOL: case City.CHURCH: case City.STADIUM: role = zones ? "Can become a safe zone" : null; break;
            case City.PHARMACY: role = "Patches up the hurt (nothing stops a bite)"; break;
            case City.FIRE_STATION: role = "The fire station (no crews any more): the paramedics are based here"; break;
            case City.HOUSE: case City.APARTMENT: role = "Home: the people who live here run back to it"; break;
            case City.CITY_HALL: role = "The Mayor's office"; break;
            case City.COURTHOUSE: role = "The county courthouse" + (zones ? "; can become a safe zone" : ""); break;
            case City.JAIL: role = "The county jail"; break;
            case City.CALL_CENTRE: role = "The 911 call centre"; break;
            case City.ARMORY: {
                String[] g = Country.guard();
                role = gv.world.armoryLost ? "FALLEN: the " + g[1] + " can't muster here; they'll have to come from out of town"
                        : gv.world.guardCalled ? "The " + g[1] + " has mustered here and gone into the city"
                        : "The " + g[1] + ": reservists muster here and drive out when the " + g[2] + " calls them up";
                break;
            }
            case City.WORKS: role = "The city works depot"; break;
        }
        if (role == null) {
            // ((11.5) What the kinds of building are good for.)
            switch (World.useOf(b)) {
                case Variants.U_FOOD: role = "Well stocked with food: somewhere to hold out"; break;
                case Variants.U_MEDICAL: role = "Medical supplies: the hurt sheltering here are patched up"; break;
                case Variants.U_TOOLS: role = "Tools and timber: barricades go up fast and hold better"; break;
                case Variants.U_VAULT: role = "A heavy door: the dead take more than twice as long to get in"; break;
                case Variants.U_GLASS: role = "All glass at the front: the dead are through it quickly"; break;
                case Variants.U_RADIO: role = "A radio: survivors here can call for help and be heard"; break;
                case Variants.U_ROUNDS: role = "Some guns and rounds on the shelves"; break;
            }
        }
        if (role != null) lines.add(role);
        // ((12.0) Who lives there.)
        if ((b.kind == City.HOUSE || b.kind == City.APARTMENT) && b.residents > 0 && !b.collapsed)
            lines.add("Home to " + (b.residents == 1 ? "one person" : b.residents + " people"));
        gv.text.setTextAlign(Paint.Align.LEFT);
        float lh = 18 * gv.dp, cw = Math.min(gv.getWidth() - 20 * gv.dp, 460 * gv.dp);
        // ((12.0) A line too long for the card goes on to a second one, in the same colour, rather than being cut short.)
        gv.text.setTextSize(12.5f * gv.dp);
        java.util.ArrayList<String> wrapped = new java.util.ArrayList<String>();
        java.util.ArrayList<Boolean> warn = new java.util.ArrayList<Boolean>();
        for (String l : lines) {
            boolean red = l.startsWith("INFESTED") || l.startsWith("FIGHTING") || l.startsWith("BLACKOUT") || l.startsWith("FALLEN") || l.startsWith("EMPTY");
            for (String part : gv.hud.wrap2(l, cw - 24 * gv.dp)) {
                wrapped.add(part);
                warn.add(red);
            }
        }
        lines = wrapped;
        gv.text.setTextSize(14 * gv.dp);
        // ((22.6) Long lines go on to the next instead of being cut short.)
        {
            float ts0 = gv.text.getTextSize();
            gv.text.setTextSize(12.5f * gv.dp);
            // ((24.0) Each line's colour goes with it: a line split again here shifted the colours, or ran past the
            // end of them and the card didn't draw at all.)
            java.util.ArrayList<String> wrapped22 = new java.util.ArrayList<String>();
            java.util.ArrayList<Boolean> warn22 = new java.util.ArrayList<Boolean>();
            for (int i = 0; i < lines.size(); i++)
                for (String part : gv.hud.wrapLine(lines.get(i), cw - 24 * gv.dp)) {
                    wrapped22.add(part);
                    warn22.add(warn.get(i));
                }
            lines = wrapped22;
            warn = warn22;
            gv.text.setTextSize(ts0);
        }
        // ((33.0) Never taller than the room under the buttons along the top: the last lines go instead.)
        trimLines(lines, lh, 8 * gv.dp);
        while (warn.size() > lines.size()) warn.remove(warn.size() - 1);
        float ch = 30 * gv.dp + lines.size() * lh + 8 * gv.dp;
        float left = (gv.getWidth() - cw) / 2, top = gv.barTop - ch - 20 * gv.dp - (gv.lastAction.isEmpty() ? 0 : 44 * gv.dp);
        gv.oval.set(left, top, left + cw, top + ch);
        gv.fill.setColor(0xE8101216);
        c.drawRoundRect(gv.oval, 12 * gv.dp, 12 * gv.dp, gv.fill);
        gv.fill.setColor(b.infestKnown && b.lurkers > 0 ? 0xFF7CC24E : !b.occupants.isEmpty() ? 0xFFB08A5A : 0xFF8A9099);
        c.drawRect(left, top + 10 * gv.dp, left + 4 * gv.dp, top + ch - 10 * gv.dp, gv.fill);
        gv.text.setColor(0xFFFFFFFF);
        c.drawText(gv.hud.fitText(title, cw - 24 * gv.dp), left + 14 * gv.dp, top + 22 * gv.dp, gv.text);
        gv.text.setTextSize(12.5f * gv.dp);
        for (int i = 0; i < lines.size(); i++) {
            String l = lines.get(i);
            gv.text.setColor(warn.get(i) ? 0xFFFF8A6A : 0xFFB8BDC4);
            c.drawText(gv.hud.fitText(l, cw - 24 * gv.dp), left + 14 * gv.dp, top + 22 * gv.dp + (i + 1) * lh, gv.text);
        }
        // Outline the building on the map so it's clear which one this is.
        gv.stroke.setColor(0xCCFFD24A);
        gv.stroke.setStrokeWidth(2 * gv.dp);
        c.drawRect(gv.screenX(b.x0), gv.screenY(b.y0), gv.screenX(b.x1), gv.screenY(b.y1), gv.stroke);
    }
}
