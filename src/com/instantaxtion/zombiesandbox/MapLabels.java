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
 * ((27.3) Out of GameView.) The lettering and tags over the map - place names, safe zones, calls, pins and the rest - and
 * the bookkeeping that keeps them from landing on one another or on the panels.
 */
final class MapLabels {
    final GameView gv;

    MapLabels(GameView gv) {
        this.gv = gv;
    }


    boolean claim(RectF r) {
        if (uiCovers(r)) return false;
        for (int i = 0; i < gv.placedCount; i++) if (overlaps(r, gv.placed[i])) return false;
        if (gv.placedCount < gv.placed.length) {
            if (gv.placed[gv.placedCount] == null) gv.placed[gv.placedCount] = new RectF();
            gv.placed[gv.placedCount++].set(r);
        }
        return true;
    }

    /** ((11.5) Letter-spaced capitals, as on a printed map. */
    static String spaced(String name) {
        StringBuilder sb = new StringBuilder();
        String up = name.toUpperCase(java.util.Locale.ROOT);
        for (int k = 0; k < up.length(); k++) {
            if (k > 0) sb.append(up.charAt(k) == ' ' || up.charAt(k - 1) == ' ' ? " " : "\u200A");
            sb.append(up.charAt(k));
        }
        return sb.toString();
    }

    /** ((16.3) A map name: a soft dark halo to stand off the streets, then the letters. */
    void haloText(Canvas c, String label, float x, float y, int col, float a) {
        gv.text.setStyle(Paint.Style.STROKE);
        gv.text.setStrokeWidth(2.6f * gv.dp);
        gv.text.setColor(alpha(0xFF0C1014, a * 0.5f));
        c.drawText(label, x, y, gv.text);
        gv.text.setStyle(Paint.Style.FILL);
        gv.text.setColor(alpha(col, a * 0.95f));
        c.drawText(label, x, y, gv.text);
    }

    void drawPlaceBanner(Canvas c, float overview) {
        City city0 = gv.world.city;
        if (city0.districts.isEmpty()) return;
        float cx = gv.camX + gv.getWidth() / gv.scale / 2, cy = gv.camY + (gv.barTop > 0 ? gv.barTop : gv.getHeight()) / gv.scale / 2;
        if (cx < 0 || cy < 0 || cx >= city0.worldW() || cy >= city0.worldH()) return;
        City.District d = city0.districtOf(cx, cy);
        if (d != null && d != gv.bannerDistrict) {
            gv.bannerDistrict = d;
            // (Not on the overview, where the names are on the map already; nor for a scrap of a district.)
            if (overview < 0.5f && d.labelled()) {
                gv.bannerTitle = spaced(d.name);
                int isl = city0.islandRects != null ? city0.island((int) (cx / City.T), (int) (cy / City.T)) : -1;
                String where = isl >= 0 && city0.islandNames != null ? city0.islandNames[isl] : city0.name;
                gv.bannerSub = where != null && !where.equals(d.name) ? where : null;
                gv.bannerT = 3.2f;
            }
        }
        if (gv.bannerT <= 0 || gv.bannerTitle == null) return;
        gv.bannerT -= gv.frameDt;
        if (overview >= 0.5f) gv.bannerT = Math.min(gv.bannerT, 0.4f);
        float a = Math.min(1, Math.min((3.2f - gv.bannerT) / 0.35f, gv.bannerT / 0.9f));
        if (a <= 0) return;
        gv.text.setTextAlign(Paint.Align.CENTER);
        gv.text.setTextSize(13 * gv.dp);
        float tw = gv.text.measureText(gv.bannerTitle);
        float subW = 0;
        if (gv.bannerSub != null) {
            gv.text.setTextSize(9.5f * gv.dp);
            subW = gv.text.measureText(gv.bannerSub);
        }
        float half = Math.max(tw, subW) / 2 + 26 * gv.dp, x = gv.getWidth() / 2f;
        // (Low down in the middle, just above the tools; if something's there, up under the top buttons; else not at all.)
        float hgt = (gv.bannerSub != null ? 30 : 18) * gv.dp;
        float y0 = gv.barTop - 14 * gv.dp - hgt;
        gv.oval.set(x - half, y0, x + half, y0 + hgt);
        if (!claim(gv.oval)) {
            float top = 8 * gv.dp;
            for (RectF t : gv.topRects) top = Math.max(top, t.bottom);
            y0 = top + 30 * gv.dp;
            gv.oval.set(x - half, y0, x + half, y0 + hgt);
            if (!claim(gv.oval)) return;
        }
        float base = y0 + 13 * gv.dp;
        gv.text.setTextSize(13 * gv.dp);
        haloText(c, gv.bannerTitle, x, base, 0xFFF6F1E2, a);
        // Fine rules either side of the name.
        gv.stroke.setStrokeWidth(1 * gv.dp);
        gv.stroke.setColor(alpha(0xFFF6F1E2, a * 0.55f));
        c.drawLine(x - tw / 2 - 22 * gv.dp, base - 4.5f * gv.dp, x - tw / 2 - 8 * gv.dp, base - 4.5f * gv.dp, gv.stroke);
        c.drawLine(x + tw / 2 + 8 * gv.dp, base - 4.5f * gv.dp, x + tw / 2 + 22 * gv.dp, base - 4.5f * gv.dp, gv.stroke);
        if (gv.bannerSub != null) {
            gv.text.setTextSize(9.5f * gv.dp);
            haloText(c, gv.bannerSub, x, base + 13 * gv.dp, 0xFFC8D0D6, a * 0.9f);
        }
    }

    boolean uiCovers(RectF r) {
        if (overlaps(r, gv.statsShown) || overlaps(r, gv.meterRect)) return true;
        for (int i = 0; i < gv.feedCount; i++) if (overlaps(r, gv.feedLines[i])) return true;
        for (RectF t : gv.topRects) if (overlaps(r, t)) return true;
        if (overlaps(r, gv.miniRect) || overlaps(r, gv.camChip) || overlaps(r, gv.mapChip) || overlaps(r, gv.foldTab)) return true;
        return r.bottom > gv.barTop;
    }

    static boolean overlaps(RectF a, RectF b) {
        return a.left < b.right && b.left < a.right && a.top < b.bottom && b.top < a.bottom;
    }

    void drawMapLabels(Canvas c) {
        gv.text.setTextAlign(Paint.Align.CENTER);
        gv.placedCount = 0;
        // Further out still, on the Islands: each island's town, in big letters.
        drawAreaNames(c);
        drawSiteLabels(c);
        drawCallLabels(c);
        drawBuildingLabels(c);
        // Names over people when zoomed right in, and the outbreak's first victim.
        drawPeopleLabels(c);
        // What each vehicle is up to. ((22.0) Only right up close, or the one being followed: the status of every
        // helicopter on its pad and every car on a call was more of the screen than the city.)
        drawVehicleLabels(c);
        // Zoomed out: the names of the districts across the map.
        // ((16.3) Only on the overview, zoomed well out: close in, where the playing is, they were big letters across
        // the streets and the people in them. Smaller now, quieter, and never the first thing claimed: a call, a
        // vehicle or a safe zone has its place first. Close in, the place shows by name below as you move into it.)
        drawDistrictLabels(c);
    }

    /** ((27.3) From drawMapLabels.) Zoomed out: the islands' towns, and the parks, lakes and woods, by name. */
    void drawAreaNames(Canvas c) {
        City city0 = gv.world.city;
        // ((16.3) On the island's own dry land, not the middle of its bounds out at sea; letter-spaced like the
        // districts, a little smaller, and only once you're well out.)
        float islandAlpha = Math.max(0, Math.min(1, (1.2f - gv.scale / gv.dp) / 0.45f));
        if (city0.islandRects != null && islandAlpha > 0) {
            gv.text.setTextAlign(Paint.Align.CENTER);
            gv.text.setTextSize(17 * gv.dp);
            for (int i = 0; i < city0.islandRects.length; i++) {
                int[] q = city0.islandRects[i];
                float lx = city0.islandLabel != null ? city0.islandLabel[i * 2] : (q[0] + q[2]) / 2f * City.T;
                float ly = city0.islandLabel != null ? city0.islandLabel[i * 2 + 1] : (q[1] + q[3]) / 2f * City.T;
                float sx = gv.screenX(lx), sy = gv.screenY(ly);
                if (sx < -200 * gv.dp || sx > gv.getWidth() + 200 * gv.dp || sy < 20 * gv.dp || sy > gv.barTop) continue;
                String label = spaced(city0.islandNames[i]);
                float tw = gv.text.measureText(label);
                gv.oval.set(sx - tw / 2, sy - 17 * gv.dp, sx + tw / 2, sy + 4 * gv.dp);
                if (!claim(gv.oval)) continue;
                haloText(c, label, sx, sy, 0xFFFFF4C8, islandAlpha);
            }
        }

        // Out in the country: the park, the mountains, a lake, the falls and the campsites, by name.
        float natureAlpha = Math.max(0, Math.min(1, (3.2f - gv.scale / gv.dp) / 1.2f));
        if (!city0.natureNames.isEmpty() && natureAlpha > 0) {
            for (int i = 0; i < city0.natureNames.size(); i++) {
                float[] q = city0.natureSpots.get(i);
                int kind = (int) q[2];
                // (Campsites and the falls only once you're closer in; the park from far out.)
                float a = kind == City.NL_PARK ? Math.max(0, Math.min(1, (4f - gv.scale / gv.dp) / 1.2f))
                        : kind == City.NL_CAMP || kind == City.NL_FALLS ? Math.max(0, Math.min(1, (gv.scale / gv.dp - 0.45f) / 0.3f)) * natureAlpha : natureAlpha;
                if (a <= 0) continue;
                gv.text.setTextSize((kind == City.NL_PARK ? 15 : kind == City.NL_PEAK ? 12.5f : 11) * gv.dp);
                float sx = gv.screenX(q[0]), sy = gv.screenY(q[1]);
                if (sx < -200 * gv.dp || sx > gv.getWidth() + 200 * gv.dp || sy < 20 * gv.dp || sy > gv.barTop) continue;
                String label = kind == City.NL_PARK ? city0.natureNames.get(i).toUpperCase(java.util.Locale.ROOT) : city0.natureNames.get(i);
                if (kind == City.NL_PEAK) label = "\u25B2 " + label;
                float tw = gv.text.measureText(label);
                gv.oval.set(sx - tw / 2, sy - gv.text.getTextSize(), sx + tw / 2, sy + 3 * gv.dp);
                if (!claim(gv.oval)) continue;
                int col = kind == City.NL_LANDMARK ? 0xFFF2D27A : kind == City.NL_LAKE || kind == City.NL_FALLS ? 0xFFBFE6F2 : kind == City.NL_CAMP ? 0xFFF2DDB0
                        : kind == City.NL_PEAK ? 0xFFF2F2EA : 0xFFD8F0C0;
                gv.text.setColor(alpha(0xFF000000, a * 0.6f));
                c.drawText(label, sx + 1.5f * gv.dp, sy + 1.5f * gv.dp, gv.text);
                gv.text.setColor(alpha(col, a * 0.95f));
                c.drawText(label, sx, sy, gv.text);
            }
        }

        // Most important first: what can't be missed gets its place, the rest fits round it.
        gv.text.setTextSize(11.5f * gv.dp);
    }

    /** ((27.3) From drawMapLabels.) Safe zones, command posts, holdouts and survivor bases. */
    void drawSiteLabels(Canvas c) {
        for (int i = 0, n = gv.world.dispatch.zones.size(); i < n; i++) {
            Dispatch.SafeZone z = gv.world.dispatch.zones.get(i);
            float sx = gv.screenX(z.x), sy = gv.screenY(z.y - z.r) - 10 * gv.dp;
            if (sx < -100 * gv.dp || sx > gv.getWidth() + 100 * gv.dp || sy < 0 || sy > gv.barTop) continue;
            String label = !z.open ? (z.military ? "MILITARY" : "POLICE") + " SAFE ZONE  -  SETTING UP " + (int) (z.built * 100)
                    + "%" + (z.onSite == 0 ? " (on the way)" : "")
                    : (z.military ? "MILITARY" : "POLICE") + " SAFE ZONE  -  " + z.sheltered + "/" + z.capacity
                    + (z.full ? "  FULL" : z.fallingBack ? "  FALLING BACK" : z.breaches > 0 ? "  BREACHED" : "")
                    + "  -  food " + (z.foodMax > 0 ? (int) (100 * z.food / z.foodMax) : 0) + "%  ammo " + z.ammo + (z.supplyComing ? " (truck coming)" : "");
            float tw = gv.text.measureText(label);
            gv.oval.set(sx - tw / 2 - 8 * gv.dp, sy - 14 * gv.dp, sx + tw / 2 + 8 * gv.dp, sy + 5 * gv.dp);
            if (!claim(gv.oval)) continue;
            gv.fill.setColor(z.military ? 0xD0304A20 : 0xD0203A66);
            c.drawRoundRect(gv.oval, 8 * gv.dp, 8 * gv.dp, gv.fill);
            gv.text.setColor(0xFFFFFFFF);
            c.drawText(label, sx, sy, gv.text);
        }
        // ((26.0) The police's mobile command post.)
        for (Ops.Post post : gv.world.ops.posts) {
            if (!post.up || post.truck == null) continue;
            float sx = gv.screenX(post.x), sy = gv.screenY(post.y - 30) - 6 * gv.dp;
            if (sx > -100 * gv.dp && sx < gv.getWidth() + 100 * gv.dp && sy > 0 && sy < gv.barTop) {
                Fleet.Vehicle cp = post.truck;
                String label = (post.army ? "ARMY" : "POLICE") + " COMMAND POST  -  " + (int) (100 * cp.hp / cp.maxHp) + "%";
                float tw = gv.text.measureText(label);
                gv.oval.set(sx - tw / 2 - 8 * gv.dp, sy - 14 * gv.dp, sx + tw / 2 + 8 * gv.dp, sy + 5 * gv.dp);
                if (claim(gv.oval)) {
                    gv.fill.setColor(cp.hp < cp.maxHp * 0.35f ? 0xD08A2020 : post.army ? 0xD03E4A26 : 0xD0203A66);
                    c.drawRoundRect(gv.oval, 8 * gv.dp, 8 * gv.dp, gv.fill);
                    gv.text.setColor(0xFFFFFFFF);
                    c.drawText(label, sx, sy, gv.text);
                }
            }
        }
        // 911 calls: a small tag, "911 12" (the dead there), and a blue dot with how many units are coming.
        // Zoomed out, only the big ones.
        gv.text.setTextSize(10.5f * gv.dp);
        // Survivor groups: their name, how many, and how well dug in.
        gv.text.setTextSize(10.5f * gv.dp);
        for (int i = 0, n = gv.world.holdouts.size(); i < n; i++) {
            World.Holdout h = gv.world.holdouts.get(i);
            float sx = gv.screenX((h.b.x0 + h.b.x1) / 2), sy = gv.screenY(h.b.y0) - 8 * gv.dp;
            if (sx < -100 * gv.dp || sx > gv.getWidth() + 100 * gv.dp || sy < 0 || sy > gv.barTop) continue;
            String label = h.name.toUpperCase(java.util.Locale.ROOT) + "  -  " + h.count + "  -  "
                    + (h.fort >= 100 ? "walled in" : "fortified " + (int) h.fort + "%");
            float tw = gv.text.measureText(label);
            gv.oval.set(sx - tw / 2 - 7 * gv.dp, sy - 13 * gv.dp, sx + tw / 2 + 7 * gv.dp, sy + 5 * gv.dp);
            if (!claim(gv.oval)) continue;
            gv.fill.setColor(0xD04A3A1C);
            c.drawRoundRect(gv.oval, 7 * gv.dp, 7 * gv.dp, gv.fill);
            gv.text.setColor(0xFFF2E2B8);
            c.drawText(label, sx, sy, gv.text);
        }
        // ((25.0) Survivor bases: their name, how many, and how far they've got.)
        for (int i = 0, n = gv.world.bases.size(); i < n; i++) {
            SurvivorBases.Base s = gv.world.bases.get(i);
            float sx = gv.screenX(s.cx()), sy = gv.screenY(s.ty0 * City.T) - 6 * gv.dp;
            if (sx < -100 * gv.dp || sx > gv.getWidth() + 100 * gv.dp || sy < 0 || sy > gv.barTop) continue;
            String state = !s.walled ? "building its wall" : SurvivorBases.TIER_NAMES[s.tier] + " wall"
                    + (s.towersUp > 0 ? ", " + s.towersUp + (s.towersUp == 1 ? " tower" : " towers") : "");
            String label = s.name.toUpperCase(java.util.Locale.ROOT) + "  -  " + s.members + "  -  " + state;
            float tw = gv.text.measureText(label);
            gv.oval.set(sx - tw / 2 - 7 * gv.dp, sy - 13 * gv.dp, sx + tw / 2 + 7 * gv.dp, sy + 5 * gv.dp);
            if (!claim(gv.oval)) continue;
            gv.fill.setColor(0xD0503A16);
            c.drawRoundRect(gv.oval, 7 * gv.dp, 7 * gv.dp, gv.fill);
            gv.text.setColor(0xFFFFE2A0);
            c.drawText(label, sx, sy, gv.text);
        }
    }

    /** ((27.3) From drawMapLabels.) The calls: how many of the dead, who's there and who's coming. */
    void drawCallLabels(Canvas c) {
        for (int i = 0, n = gv.world.dispatch.incidents.size(); i < n; i++) {
            Dispatch.Incident inc = gv.world.dispatch.incidents.get(i);
            if (gv.scale / gv.dp < 0.6f && inc.zombiesNear < 6) continue;
            float sx = gv.screenX(inc.x), sy = gv.screenY(inc.y) - 14 * gv.dp;
            if (sx < -40 * gv.dp || sx > gv.getWidth() + 40 * gv.dp || sy < 0 || sy > gv.barTop) continue;
            int coming = inc.cops + inc.soldiers + gv.world.fleet.inbound(inc);
            boolean backup = inc.backupNeed > 0;
            String label = (backup ? "BACKUP " : "911 ") + inc.zombiesNear;
            // (10.24) Who's coming, spelled out: police, the army, or both.
            String units = coming <= 0 ? null : inc.soldiers > 0 && inc.cops + gv.world.fleet.inbound(inc) > 0
                    ? "POLICE " + (inc.cops + gv.world.fleet.inbound(inc)) + " + ARMY " + inc.soldiers
                    : inc.soldiers > 0 ? "ARMY " + inc.soldiers : "POLICE " + coming;
            float tw = gv.text.measureText(label), uw = units == null ? 0 : Math.max(12 * gv.dp, gv.text.measureText(units) + 7 * gv.dp);
            float w = tw + 10 * gv.dp + (units == null ? 0 : uw + 3 * gv.dp);
            gv.oval.set(sx - w / 2, sy - 10 * gv.dp, sx + w / 2, sy + 3.5f * gv.dp);
            if (!claim(gv.oval)) continue;
            gv.fill.setColor(backup ? 0xE02A58C0 : units == null ? 0xE0C0281E : 0xD08A1E16);
            c.drawRoundRect(gv.oval, 6 * gv.dp, 6 * gv.dp, gv.fill);
            gv.text.setColor(0xFFFFFFFF);
            gv.text.setTextAlign(Paint.Align.LEFT);
            c.drawText(label, gv.oval.left + 5 * gv.dp, sy, gv.text);
            if (units != null) {
                float ux = gv.oval.right - 2 * gv.dp - uw;
                RectF u = gv.oval2;
                u.set(ux, gv.oval.top + 2 * gv.dp, gv.oval.right - 2 * gv.dp, gv.oval.bottom - 2 * gv.dp);
                gv.fill.setColor(inc.soldiers > 0 && inc.cops == 0 ? 0xFF4A7A2A : 0xFF2E6AD0);
                c.drawRoundRect(u, 5 * gv.dp, 5 * gv.dp, gv.fill);
                gv.text.setTextAlign(Paint.Align.CENTER);
                c.drawText(units, u.centerX(), sy, gv.text);
            }
            gv.text.setTextAlign(Paint.Align.CENTER);
        }
        gv.text.setTextSize(10.5f * gv.dp);
    }

    /** ((27.3) From drawMapLabels.) Buildings worth naming, the armory, the stations and bases, the rangers' station. */
    void drawBuildingLabels(Canvas c) {
        for (int i = 0, n = gv.world.city.buildings.size(); i < n; i++) {
            City.Building b = gv.world.city.buildings.get(i);
            if (b.occupants.isEmpty()) continue;
            // (Zoomed out, only the ones under attack.)
            boolean attacked = b.fighting || b.barricade < 99 && gv.world.countZombiesNear(b.doorX, b.doorY, 40) > 0;
            if (gv.scale / gv.dp < 1.3f && !attacked) continue;
            float sx = gv.screenX(b.doorX), sy = gv.screenY(b.doorY) - 12 * gv.dp;
            if (sx < -60 * gv.dp || sx > gv.getWidth() + 60 * gv.dp || sy < 0 || sy > gv.barTop) continue;
            String label = b.fighting ? b.occupants.size() + " FIGHTING " + b.lurkers + " INSIDE"
                    : b.occupants.size() + " hiding" + (attacked ? " - UNDER ATTACK" : "");
            float tw = gv.text.measureText(label);
            gv.oval.set(sx - tw / 2 - 6 * gv.dp, sy - 12 * gv.dp, sx + tw / 2 + 6 * gv.dp, sy + 7 * gv.dp);
            if (!claim(gv.oval)) continue;
            gv.fill.setColor(attacked ? 0xD0802018 : 0xC0302418);
            c.drawRoundRect(gv.oval, 6 * gv.dp, 6 * gv.dp, gv.fill);
            gv.text.setColor(0xFFFFFFFF);
            c.drawText(label, sx, sy, gv.text);
            float f = Math.max(0, b.barricade) / 100f;
            gv.fill.setColor(f > 0.5f ? 0xFFB08A5A : 0xFFFF6B4A);
            c.drawRect(gv.oval.left + 4 * gv.dp, gv.oval.bottom - 3 * gv.dp, gv.oval.left + 4 * gv.dp + (gv.oval.width() - 8 * gv.dp) * f,
                    gv.oval.bottom - 1.5f * gv.dp, gv.fill);
        }
        gv.text.setTextSize(11.5f * gv.dp);
        gv.text.setTextSize(10.5f * gv.dp);
        // The National Guard armory is labelled like the base.
        City.Building arm = gv.world.city.armory;
        if (arm != null) {
            float sx = gv.screenX((arm.x0 + arm.x1) / 2), sy = gv.screenY(arm.y1) + 22 * gv.dp;
            if (sx > -100 * gv.dp && sx < gv.getWidth() + 100 * gv.dp && sy > 0 && sy < gv.barTop) {
                String label = arm.name.toUpperCase();
                float tw = gv.text.measureText(label);
                gv.oval.set(sx - tw / 2 - 7 * gv.dp, sy - 13 * gv.dp, sx + tw / 2 + 7 * gv.dp, sy + 5 * gv.dp);
                if (claim(gv.oval)) {
                    gv.fill.setColor(0xB0303A1E);
                    c.drawRoundRect(gv.oval, 7 * gv.dp, 7 * gv.dp, gv.fill);
                    gv.text.setColor(0xFFE6E6E6);
                    c.drawText(label, sx, sy, gv.text);
                }
            }
        }
        // ((22.0) The stations and bases by name only from a middling zoom in: far out, the districts' names are enough.)
        // ((28.0) Further out now: the police, the army and the federal agents are where you look for help first.)
        for (int i = 0, n = gv.scale / gv.dp < 0.2f ? 0 : gv.world.city.facilities.size(); i < n; i++) {
            City.Facility f = gv.world.city.facilities.get(i);
            if (f.kind == City.FACILITY_FIRE || f.kind == City.FACILITY_HOSPITAL) continue;
            boolean zoned = false;
            for (int k = 0; k < gv.world.dispatch.zones.size(); k++) {
                Dispatch.SafeZone z = gv.world.dispatch.zones.get(k);
                if (World.hypot(z.x - f.x, z.y - f.y) < 150) zoned = true;
            }
            // (With a safe zone there, its name goes just above the zone's own label.)
            float sx = gv.screenX(f.x), sy = gv.screenY(f.y) + (zoned ? -6 : 26) * gv.dp;
            if (sx < -100 * gv.dp || sx > gv.getWidth() + 100 * gv.dp || sy < 0 || sy > gv.barTop) continue;
            String label = f.name.toUpperCase();
            float tw = gv.text.measureText(label);
            gv.oval.set(sx - tw / 2 - 7 * gv.dp, sy - 13 * gv.dp, sx + tw / 2 + 7 * gv.dp, sy + 5 * gv.dp);
            if (!claim(gv.oval)) continue;
            gv.fill.setColor(f.kind == City.FACILITY_BASE ? 0xB0303A1E : f.kind == City.FACILITY_FIRE ? 0xB0802018
                    : f.kind == City.FACILITY_HOSPITAL ? 0xB0703030 : 0xB01E2E50);
            c.drawRoundRect(gv.oval, 7 * gv.dp, 7 * gv.dp, gv.fill);
            gv.text.setColor(0xFFE6E6E6);
            c.drawText(label, sx, sy, gv.text);
        }
        // The ranger station, labelled like the stations in town.
        City.Building rs = gv.world.rangerStation;
        if (rs != null && !rs.collapsed) {
            float sx = gv.screenX(rs.doorX), sy = gv.screenY(rs.doorY) + 26 * gv.dp;
            if (sx > -100 * gv.dp && sx < gv.getWidth() + 100 * gv.dp && sy > 0 && sy < gv.barTop) {
                String label = rs.name.split(",")[0].toUpperCase();
                float tw = gv.text.measureText(label);
                gv.oval.set(sx - tw / 2 - 7 * gv.dp, sy - 13 * gv.dp, sx + tw / 2 + 7 * gv.dp, sy + 5 * gv.dp);
                if (claim(gv.oval)) {
                    gv.fill.setColor(0xB02E4A24);
                    c.drawRoundRect(gv.oval, 7 * gv.dp, 7 * gv.dp, gv.fill);
                    gv.text.setColor(0xFFE6E6E6);
                    c.drawText(label, sx, sy, gv.text);
                }
            }
        }
    }

    /** ((27.3) From drawMapLabels.) Name tags close up, and patient zero. */
    void drawPeopleLabels(Canvas c) {
        if (gv.settings.nameTags() && gv.scale / gv.dp > 3f) {
            gv.text.setTextSize(9.5f * gv.dp);
            int placed = 0;
            for (int i = 0, n = gv.onScreen.size(); i < n; i++) {
                Entity e = gv.onScreen.get(i);
                float sx = gv.screenX(e.x), sy = gv.screenY(e.y) - (e.radius * gv.scale + 12 * gv.dp);
                if (sx < 0 || sx > gv.getWidth() || sy < 0 || sy > gv.barTop) continue;
                String label = e.type == Entity.DOG || e.type == Entity.ZOMBIE_DOG ? Names.dog(e.nameSeed)
                        : e.isUndead() ? null : e.isArmed() ? Dispatch.name(e) : Names.person(e.nameSeed).split(" ")[0];
                if (label == null) continue;
                float tw = gv.text.measureText(label);
                gv.oval.set(sx - tw / 2 - 3 * gv.dp, sy - 9 * gv.dp, sx + tw / 2 + 3 * gv.dp, sy + 3 * gv.dp);
                // (Skipped if it would sit on a panel, or on a tag already drawn in a crowd.)
                if (!claim(gv.oval)) continue;
                if (++placed > 60) break;
                gv.fill.setColor(0x90000000);
                c.drawRoundRect(gv.oval, 4 * gv.dp, 4 * gv.dp, gv.fill);
                gv.text.setColor(e.isArmed() ? 0xFFA8C8FF : 0xFFF2F2F2);
                c.drawText(label, sx, sy, gv.text);
            }
        }
        Entity p0 = gv.world.patientZero;
        if (p0 != null && !p0.dead) {
            gv.text.setTextSize(10 * gv.dp);
            float sx = gv.screenX(p0.x), sy = gv.screenY(p0.y) - (p0.radius * gv.scale + 16 * gv.dp);
            if (sx > 0 && sx < gv.getWidth() && sy > 0 && sy < gv.barTop) {
                String label = "PATIENT ZERO";
                float tw = gv.text.measureText(label);
                gv.oval.set(sx - tw / 2 - 5 * gv.dp, sy - 11 * gv.dp, sx + tw / 2 + 5 * gv.dp, sy + 4 * gv.dp);
                if (claim(gv.oval)) {
                    gv.fill.setColor(0xD04A7A1A);
                    c.drawRoundRect(gv.oval, 6 * gv.dp, 6 * gv.dp, gv.fill);
                    gv.text.setColor(0xFFFFFFFF);
                    c.drawText(label, sx, sy, gv.text);
                }
            }
        }
    }

    /** ((27.3) From drawMapLabels.) What the vehicles are doing, over them. */
    void drawVehicleLabels(Canvas c) {
        boolean close = gv.scale / gv.dp > 2.2f;
        gv.text.setTextSize(10 * gv.dp);
        for (int i = 0, n = gv.world.fleet.vehicles.size(); i < n; i++) {
            Fleet.Vehicle v = gv.world.fleet.vehicles.get(i);
            if (v != gv.followV && (!close || v.type == Fleet.CAR || (!v.lightsOn() && !Fleet.airborne(v) && v.type != Fleet.TANK))) continue;
            // ((12.0) Only those on screen have their label worked out: it was put together for every vehicle on the map, every frame.)
            float sx = gv.screenX(v.x), sy = gv.screenY(v.y) - (v.type == Fleet.HELI ? 22 + 14 * v.alt : 14) * gv.dp;
            if (sx < -80 * gv.dp || sx > gv.getWidth() + 80 * gv.dp || sy < 0 || sy > gv.barTop) continue;
            String label = gv.world.fleet.status(v);
            if (label == null) continue;
            float tw = gv.text.measureText(label);
            gv.oval.set(sx - tw / 2 - 6 * gv.dp, sy - 11 * gv.dp, sx + tw / 2 + 6 * gv.dp, sy + 4 * gv.dp);
            if (!claim(gv.oval)) continue;
            int col = Fleet.army(v) ? 0xC8304A20
                    : v.type == Fleet.AMBULANCE ? 0xC8A03030
                    : v.type == Fleet.CRUISER || Fleet.policeTruck(v) || v.type == Fleet.HELI ? 0xC8203A66 : 0xB0202226;
            gv.fill.setColor(col);
            c.drawRoundRect(gv.oval, 6 * gv.dp, 6 * gv.dp, gv.fill);
            gv.text.setColor(0xFFF2F2F2);
            c.drawText(label, sx, sy, gv.text);
        }
            gv.text.setTextAlign(Paint.Align.CENTER);
    }

    /** ((27.3) From drawMapLabels.) The districts' names zoomed out, and the streets' closer in. */
    void drawDistrictLabels(Canvas c) {
        float districtAlpha = Math.max(0, Math.min(1, (1.3f - gv.scale / gv.dp) / 0.45f));
        if (districtAlpha > 0) {
            for (int i = 0, n = gv.world.city.districts.size(); i < n; i++) {
                City.District d = gv.world.city.districts.get(i);
                if (!d.labelled()) continue;
                float sx0 = gv.screenX(d.cx), sy0 = gv.screenY(d.cy);
                if (sx0 < -200 * gv.dp || sx0 > gv.getWidth() + 200 * gv.dp || sy0 < -40 * gv.dp || sy0 > gv.barTop + 40 * gv.dp) continue;
                String label = gv.spacedNames.get(d);
                if (label == null) {
                    label = spaced(d.name);
                    gv.spacedNames.put(d, label);
                }
                float size = Math.max(10.5f, Math.min(14.5f, 9.5f + (float) Math.sqrt(d.tiles) / 18)) * gv.dp;
                gv.text.setTextSize(size);
                float tw = gv.text.measureText(label);
                boolean put = false;
                float sx = sx0, sy = sy0;
                for (int k = 0; k < 5 && !put; k++) {
                    sx = sx0 + (k == 3 ? -tw * 0.4f : k == 4 ? tw * 0.4f : 0);
                    sy = sy0 + (k == 1 ? -size * 1.8f : k == 2 ? size * 1.8f : 0);
                    if (sy < size || sy > gv.barTop) continue;
                    gv.oval.set(sx - tw / 2 - 3 * gv.dp, sy - size, sx + tw / 2 + 3 * gv.dp, sy + 4 * gv.dp);
                    put = claim(gv.oval);
                }
                if (!put) continue;
                haloText(c, label, sx, sy, d.type == City.DT_INDUSTRIAL ? 0xFFE6DCC8 : d.type == City.DT_PARKSIDE ? 0xFFDDEFC8 : 0xFFF6F1E2,
                        districtAlpha * 0.85f);
            }
        }
        drawPlaceBanner(c, districtAlpha);
        // (The rest as before: from a little way out, not right in close.)
        if (gv.scale < 3.2f) {
            // (10.46) Infection zones: their name, how many of the dead are in them, and whether they're held.
            gv.text.setTextSize(11 * gv.dp);
            for (int i = 0; i < gv.world.infection.size(); i++) {
                World.InfectionZone z = gv.world.infection.get(i);
                float sx = gv.screenX(z.x), sy = gv.screenY(z.y - z.r) - 6 * gv.dp;
                if (sx < -120 * gv.dp || sx > gv.getWidth() + 120 * gv.dp || sy < 12 * gv.dp || sy > gv.barTop) continue;
                String label = "INFECTION ZONE \u00b7 " + z.zombies + (z.critical ? "" : " \u00b7 WAITING");
                float tw = gv.text.measureText(label);
                gv.oval.set(sx - tw / 2 - 5 * gv.dp, sy - 11 * gv.dp, sx + tw / 2 + 5 * gv.dp, sy + 4 * gv.dp);
                if (!claim(gv.oval)) continue;
                gv.fill.setColor(z.critical ? 0xDD8A1E16 : 0xDD8A5A10);
                c.drawRoundRect(gv.oval, 4 * gv.dp, 4 * gv.dp, gv.fill);
                gv.text.setColor(0xFFFFF0E8);
                c.drawText(label, sx, sy, gv.text);
            }
            // (10.49) Where the evacuation buses pick up (and the station, for the trains), while people are leaving.
            if (gv.world.exodusOn() && gv.world.byTransit()) {
                gv.text.setTextSize(10 * gv.dp);
                for (int i = 0; i <= gv.world.evacStops.size(); i++) {
                    boolean train = i == gv.world.evacStops.size();
                    if (train && gv.world.city.stationX < 0) break;
                    World.EvacStop st = train ? null : gv.world.evacStops.get(i);
                    float wx = train ? gv.world.city.stationX : st.x, wy = train ? gv.world.city.stationY : st.y;
                    float sx = gv.screenX(wx), sy = gv.screenY(wy) - 12 * gv.dp;
                    if (sx < -80 * gv.dp || sx > gv.getWidth() + 80 * gv.dp || sy < 12 * gv.dp || sy > gv.barTop) continue;
                    int n = gv.world.waitingFor(train ? World.STOP_TRAIN : i, wx, wy, true);
                    String label = (train ? "EVACUATION TRAINS" : st.closed ? "BUS STOP CLOSED" : "EVACUATION BUS") + (n > 0 ? " \u00b7 " + n : "");
                    float tw = gv.text.measureText(label);
                    gv.oval.set(sx - tw / 2 - 4 * gv.dp, sy - 10 * gv.dp, sx + tw / 2 + 4 * gv.dp, sy + 3 * gv.dp);
                    if (!claim(gv.oval)) continue;
                    gv.fill.setColor(st != null && st.closed ? 0xDD5A5A5A : 0xDD1F5A9A);
                    c.drawRoundRect(gv.oval, 4 * gv.dp, 4 * gv.dp, gv.fill);
                    gv.text.setColor(0xFFF2F6FA);
                    c.drawText(label, sx, sy, gv.text);
                }
            }
            // (10.32) The army's checkpoints, and a base that's been overrun.
            gv.text.setTextSize(10 * gv.dp);
            for (int i = 0; i < gv.world.city.checkpoints.size(); i++) {
                float[] cp = gv.world.city.checkpoints.get(i);
                float sx = gv.screenX(cp[0]), sy = gv.screenY(cp[1]) - 14 * gv.dp;
                if (sx < -60 * gv.dp || sx > gv.getWidth() + 60 * gv.dp || sy < 0 || sy > gv.barTop) continue;
                String label = "CHECKPOINT";
                float tw = gv.text.measureText(label);
                gv.oval.set(sx - tw / 2 - 4 * gv.dp, sy - 10 * gv.dp, sx + tw / 2 + 4 * gv.dp, sy + 3 * gv.dp);
                if (!claim(gv.oval)) continue;
                gv.fill.setColor(alpha(0xFF3C4A2A, districtAlpha * 0.8f));
                c.drawRoundRect(gv.oval, 4 * gv.dp, 4 * gv.dp, gv.fill);
                gv.text.setColor(alpha(0xFFE8E2C0, districtAlpha));
                c.drawText(label, sx, sy, gv.text);
            }
            for (int i = 0; i < gv.world.city.facilities.size(); i++) {
                City.Facility f = gv.world.city.facilities.get(i);
                if (f.kind != City.FACILITY_BASE || !f.fallen) continue;
                float sx = gv.screenX(f.x), sy = gv.screenY(f.y) + 20 * gv.dp;
                if (sx < -80 * gv.dp || sx > gv.getWidth() + 80 * gv.dp || sy < 0 || sy > gv.barTop) continue;
                String label = "BASE OVERRUN";
                float tw = gv.text.measureText(label);
                gv.oval.set(sx - tw / 2 - 4 * gv.dp, sy - 10 * gv.dp, sx + tw / 2 + 4 * gv.dp, sy + 3 * gv.dp);
                if (!claim(gv.oval)) continue;
                gv.fill.setColor(alpha(0xFF6A2018, districtAlpha * 0.85f));
                c.drawRoundRect(gv.oval, 4 * gv.dp, 4 * gv.dp, gv.fill);
                gv.text.setColor(alpha(0xFFFFD8D0, districtAlpha));
                c.drawText(label, sx, sy, gv.text);
            }
        }
    }

    /** Keeps pins on the right people: someone who turned is followed as the zombie they became. */
    void updatePins() {
        gv.pinWhere.clear();
        for (int i = 0; i < gv.pinned.size(); i++) {
            Entity p = gv.pinned.get(i);
            if (p.dead && p.removed && gv.world.controlled != p) {
                Object at = whereIs(p);
                if (at != null) gv.pinWhere.put(p, at);
            }
        }
        for (int i = gv.pinned.size() - 1; i >= 0; i--) {
            Entity p = gv.pinned.get(i);
            if (!p.dead || gv.world.controlled == p) continue;
            Entity now = null;
            for (int k = 0, n = gv.world.entities.size(); k < n && now == null; k++) {
                Entity o = gv.world.entities.get(k);
                if (!o.dead && o.nameSeed == p.nameSeed && o != p) now = o;
            }
            if (now != null) {
                gv.pinned.set(i, now);
                if (now.isZombie()) gv.world.say(Names.person(p.nameSeed) + " has turned");
            } else if (!p.removed || !stillAround(p)) {
                // Dead for good (or gone into a building and not out yet: keep them a while). ((12.0) But not for good:
                // one who died in there, or turned in there, stayed pinned, the icon pointing at nobody.)
                gv.pinned.remove(i);
            }
        }
    }

    /** ((12.0) Someone out of sight - in a building, a vehicle or out of town - who is still somewhere.) */
    boolean stillAround(Entity p) {
        return whereIs(p) != null || gv.world.away.contains(p);
    }

    /** ((12.0) The building or vehicle someone out of sight is in, or null. */
    Object whereIs(Entity p) {
        for (int i = 0, n = gv.world.city.buildings.size(); i < n; i++) {
            City.Building b = gv.world.city.buildings.get(i);
            if (b.occupants.contains(p) || b.visitors.contains(p)) return b;
        }
        for (int i = 0, n = gv.world.fleet.vehicles.size(); i < n; i++) {
            Fleet.Vehicle v = gv.world.fleet.vehicles.get(i);
            if (v.driver == p || v.crew.contains(p) || v.riders.contains(p)) return v;
        }
        return null;
    }

    /** Where a pinned person is to be shown: {x, y}, or null if nowhere. */
    float[] pinAt(Entity p) {
        if (!p.dead || gv.world.controlled == p) return new float[]{p.x, p.y};
        Object at = gv.pinWhere.get(p);
        if (at instanceof City.Building) return new float[]{((City.Building) at).doorX, ((City.Building) at).doorY};
        if (at instanceof Fleet.Vehicle && !((Fleet.Vehicle) at).removedFromFleet) return new float[]{((Fleet.Vehicle) at).x, ((Fleet.Vehicle) at).y};
        return null;
    }

    /** A compact tag: small text on a dark pill, the colour as a thin edge. */
    void smallTag(Canvas c, String label, float x, float y, int col) {
        gv.text.setTextAlign(Paint.Align.CENTER);
        gv.text.setTextSize(8.5f * gv.dp);
        float tw = gv.text.measureText(label);
        gv.tmpRect.set(x - tw / 2 - 3.5f * gv.dp, y - 8.5f * gv.dp, x + tw / 2 + 3.5f * gv.dp, y + 2.5f * gv.dp);
        gv.fill.setColor(0xB0141619);
        c.drawRoundRect(gv.tmpRect, 3.5f * gv.dp, 3.5f * gv.dp, gv.fill);
        gv.stroke.setColor(col);
        gv.stroke.setStrokeWidth(0.8f * gv.dp);
        c.drawRoundRect(gv.tmpRect, 3.5f * gv.dp, 3.5f * gv.dp, gv.stroke);
        gv.text.setColor(0xFFFFFFFF);
        c.drawText(label, x, y, gv.text);
    }

    /** Stars over pinned people on screen, and arrows at the edge pointing to those off it. */
    /**
     * ((21.0) The last of the dead, once it's down to a handful: a green ring round each on the screen, and for those
     * off it an arrow at the edge pointing the way, so the end of an outbreak isn't a search.
     */
    void drawLastDead(Canvas c) {
        float[] at = gv.world.lastDead;
        if (at.length == 0) return;
        int w = gv.getWidth();
        float top = gv.topRects[0].bottom + (gv.portrait ? 150 : 115) * gv.dp, bottom = gv.barTop - 60 * gv.dp;
        if (bottom - top < 70 * gv.dp) return;
        float cx = w / 2f, cy = (top + bottom) / 2;
        for (int i = 0; i + 1 < at.length; i += 2) {
            float sx = gv.screenX(at[i]), sy = gv.screenY(at[i + 1]);
            if (sx > 0 && sx < w && sy > top && sy < bottom) {
                gv.stroke.setStrokeWidth(1.6f * gv.dp);
                gv.stroke.setColor(0xD07CC24E);
                c.drawCircle(sx, sy, (13 + 2 * (float) Math.sin(gv.world.time * 5)) * gv.dp, gv.stroke);
                continue;
            }
            float dx = sx - cx, dy = sy - cy;
            float k = Math.min(Math.abs((w / 2f - 16 * gv.dp) / (dx == 0 ? 0.001f : dx)), Math.abs(((bottom - top) / 2 - 16 * gv.dp) / (dy == 0 ? 0.001f : dy)));
            float ix = cx + dx * k, iy = cy + dy * k, a = (float) Math.atan2(dy, dx);
            c.save();
            c.translate(ix, iy);
            c.rotate((float) Math.toDegrees(a));
            gv.lastPath.reset();
            gv.lastPath.moveTo(9 * gv.dp, 0);
            gv.lastPath.lineTo(-6 * gv.dp, -7 * gv.dp);
            gv.lastPath.lineTo(-2 * gv.dp, 0);
            gv.lastPath.lineTo(-6 * gv.dp, 7 * gv.dp);
            gv.lastPath.close();
            gv.fill.setColor(0xE07CC24E);
            c.drawPath(gv.lastPath, gv.fill);
            c.restore();
        }
    }

    /** ((21.0) A horde just come in from outside: a red arrow at the edge of the screen towards it, and how many. */
    void drawIncoming(Canvas c) {
        if (gv.world.incoming.isEmpty()) return;
        int w = gv.getWidth();
        float top = gv.topRects[0].bottom + (gv.portrait ? 150 : 115) * gv.dp, bottom = gv.barTop - 60 * gv.dp;
        if (bottom - top < 70 * gv.dp) return;
        float cx = w / 2f, cy = (top + bottom) / 2;
        for (int i = 0; i < gv.world.incoming.size(); i++) {
            Object[] h = gv.world.incoming.get(i);
            Entity lead = (Entity) h[0];
            float sx = gv.screenX(lead.x), sy = gv.screenY(lead.y);
            if (sx > 0 && sx < w && sy > top && sy < bottom) continue;
            float dx = sx - cx, dy = sy - cy;
            float k = Math.min(Math.abs((w / 2f - 22 * gv.dp) / (dx == 0 ? 0.001f : dx)), Math.abs(((bottom - top) / 2 - 22 * gv.dp) / (dy == 0 ? 0.001f : dy)));
            float ix = cx + dx * k, iy = cy + dy * k, a = (float) Math.atan2(dy, dx);
            float pulse = 0.75f + 0.25f * (float) Math.sin(gv.world.time * 6);
            c.save();
            c.translate(ix, iy);
            c.rotate((float) Math.toDegrees(a));
            gv.lastPath.reset();
            gv.lastPath.moveTo(13 * gv.dp, 0);
            gv.lastPath.lineTo(-8 * gv.dp, -10 * gv.dp);
            gv.lastPath.lineTo(-3 * gv.dp, 0);
            gv.lastPath.lineTo(-8 * gv.dp, 10 * gv.dp);
            gv.lastPath.close();
            gv.fill.setColor(alpha(0xFFE0453A, pulse));
            c.drawPath(gv.lastPath, gv.fill);
            c.restore();
            gv.text.setTextAlign(Paint.Align.CENTER);
            gv.text.setTextSize(11 * gv.dp);
            gv.text.setColor(0xFFFFD0C8);
            float lx = Math.max(34 * gv.dp, Math.min(w - 34 * gv.dp, ix - (float) Math.cos(a) * 30 * gv.dp)), ly = iy - (float) Math.sin(a) * 26 * gv.dp + 4 * gv.dp;
            c.drawText("HORDE " + h[2], lx, ly, gv.text);
        }
    }

    void drawPins(Canvas c) {
        gv.pinCount = 0;
        int w = gv.getWidth();
        // (Below the radio feed, above the tool bar.)
        float top = gv.topRects[0].bottom + (gv.portrait ? 150 : 115) * gv.dp, bottom = gv.barTop - 60 * gv.dp;
        // ((12.0) On a short screen held sideways that band was upside down - its top below its bottom - and the
        // pins went down over the tool bar: never less than a pin's height, under the buttons along the top.)
        if (bottom - top < 70 * gv.dp) top = Math.max(gv.topRects[0].bottom + 10 * gv.dp, bottom - 70 * gv.dp);
        if (bottom - top < 44 * gv.dp) bottom = top + 44 * gv.dp;
        for (int i = 0; i < gv.pinned.size(); i++) {
            Entity p = gv.pinned.get(i);
            float[] at = pinAt(p);
            if (at == null) continue;
            float sx = gv.screenX(at[0]), sy = gv.screenY(at[1]);
            boolean on = sx > 0 && sx < w && sy > top && sy < bottom;
            gv.text.setTextAlign(Paint.Align.CENTER);
            if (on) {
                gv.text.setTextSize(14 * gv.dp);
                gv.text.setColor(0xFFE8C547);
                c.drawText("\u2605", sx, sy - 14 * gv.dp, gv.text);
                continue;
            }
            float cx = w / 2f, cy = (top + bottom) / 2, dx = sx - cx, dy = sy - cy;
            float k = Math.min(Math.abs((w / 2f - 26 * gv.dp) / (dx == 0 ? 0.001f : dx)), Math.abs(((bottom - top) / 2 - 26 * gv.dp) / (dy == 0 ? 0.001f : dy)));
            float ix = cx + dx * k, iy = cy + dy * k;
            // ((12.0) Two pinned people off the same way: side by side along the edge, not one on top of the other.)
            boolean side = Math.abs(ix - cx) > (w / 2f - 30 * gv.dp);
            for (int tries = 0; tries < 6; tries++) {
                boolean hit = false;
                for (int q = 0; q < gv.pinCount && !hit; q++)
                    if (Math.abs(gv.pinRects[q].centerX() - ix) < 40 * gv.dp && Math.abs(gv.pinRects[q].centerY() - iy) < 40 * gv.dp) hit = true;
                if (!hit) break;
                if (side) iy = iy + 42 * gv.dp > bottom - 22 * gv.dp ? iy - 42 * gv.dp * (tries + 1) : iy + 42 * gv.dp;
                else ix = ix + 42 * gv.dp > w - 22 * gv.dp ? ix - 42 * gv.dp * (tries + 1) : ix + 42 * gv.dp;
            }
            iy = Math.max(top + 20 * gv.dp, Math.min(Math.max(top + 20 * gv.dp, gv.barTop - 24 * gv.dp), iy));
            RectF r = gv.pinRects[gv.pinCount];
            r.set(ix - 20 * gv.dp, iy - 20 * gv.dp, ix + 20 * gv.dp, iy + 20 * gv.dp);
            gv.pinIcons[gv.pinCount++] = p;
            gv.fill.setColor(p.isZombie() ? 0xE04F7A2F : 0xE0202227);
            c.drawCircle(ix, iy, 18 * gv.dp, gv.fill);
            gv.stroke.setColor(0xFFE8C547);
            gv.stroke.setStrokeWidth(2 * gv.dp);
            c.drawCircle(ix, iy, 18 * gv.dp, gv.stroke);
            String name = Names.person(p.nameSeed);
            String init = name.length() > 0 ? name.substring(0, 1) : "?";
            int sp = name.indexOf(' ');
            if (sp > 0 && sp + 1 < name.length()) init += name.charAt(sp + 1);
            gv.text.setTextSize(12 * gv.dp);
            gv.text.setColor(0xFFFFFFFF);
            c.drawText(init, ix, iy + 4 * gv.dp, gv.text);
            // A little arrow towards them.
            float a = (float) Math.atan2(dy, dx);
            gv.fill.setColor(0xFFE8C547);
            c.drawCircle(ix + (float) Math.cos(a) * 21 * gv.dp, iy + (float) Math.sin(a) * 21 * gv.dp, 4 * gv.dp, gv.fill);
            if (gv.pinCount >= gv.pinRects.length) break;
        }
    }
}
