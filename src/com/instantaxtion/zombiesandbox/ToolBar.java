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
 * ((27.3) Out of GameView.) The tool bar's picker: each tool's options, their names, costs and descriptions, and the icons
 * drawn for them.
 */
final class ToolBar {
    final GameView gv;

    ToolBar(GameView gv) {
        this.gv = gv;
    }


    static boolean hasPicker(int t) {
        // (People have just the one kind since 10.50: no picker.)
        return t == TOOL_COP || t == TOOL_MIL || t == TOOL_ZOMBIE || t == TOOL_PLACE || t == TOOL_EVENT || t == TOOL_BUILD || t == TOOL_ZONE || t == TOOL_SQUAD;
    }

    String[] optionNames(int t) {
        String[] n = sandboxNames(t);
        int[] cost = gv.commander() ? costs(t) : null;
        if (cost == null) return n;
        String[] out = new String[n.length];
        for (int k = 0; k < n.length; k++) out[k] = n[k] + (k >= cost.length || cost[k] < 0 ? "  (n/a)" : cost[k] == 0 ? "  free" : "  $" + cost[k]);
        return out;
    }

    /** ((18.0) What each choice of a tool costs in a mission, or null. */
    int[] costs(int t) {
        switch (t) {
            case TOOL_COP: return Campaign.unitCost(gv.world, true);
            case TOOL_MIL: return Campaign.unitCost(gv.world, false);
            case TOOL_PLACE: return Campaign.placeCost(gv.world);
            case TOOL_EVENT: return Campaign.eventCost(gv.world);
            default: return null;
        }
    }

    String[] sandboxNames(int t) {
        switch (t) {
            case TOOL_CIV: return CIV_NAMES;
            case TOOL_COP: return gv.copNames();
            case TOOL_MIL: return MIL_NAMES;
            case TOOL_ZOMBIE: return gv.enemyNames();
            case TOOL_PLACE: return PLACE_NAMES;
            case TOOL_BUILD: return BUILD_NAMES;
            case TOOL_ZONE: return ZONE_NAMES;
            case TOOL_SQUAD: return SQUAD_NAMES;
            default: return EVENT_NAMES;
        }
    }

    String[] optionInfo(int t) {
        switch (t) {
            case TOOL_CIV: return CIV_INFO;
            case TOOL_COP: return COP_INFO;
            case TOOL_MIL: return MIL_INFO;
            case TOOL_ZOMBIE: return ZOMBIE_INFO;
            case TOOL_PLACE: return PLACE_INFO;
            case TOOL_BUILD: return BUILD_INFO;
            case TOOL_ZONE: return ZONE_INFO;
            case TOOL_SQUAD: return SQUAD_INFO;
            default: return EVENT_INFO;
        }
    }

    int variant(int t) {
        switch (t) {
            case TOOL_CIV: return gv.civVariant;
            case TOOL_COP: return gv.copVariant;
            case TOOL_MIL: return gv.milVariant;
            case TOOL_ZOMBIE: return gv.zombieVariant;
            case TOOL_PLACE: return gv.placeVariant;
            case TOOL_BUILD: return gv.buildVariant;
            case TOOL_ZONE: return gv.zoneVariant;
            case TOOL_SQUAD: return gv.squadVariant;
            default: return gv.eventVariant;
        }
    }

    void setVariant(int t, int v) {
        switch (t) {
            case TOOL_CIV: gv.civVariant = v; break;
            case TOOL_COP: gv.copVariant = v; break;
            case TOOL_MIL: gv.milVariant = v; break;
            case TOOL_ZOMBIE: gv.zombieVariant = v; break;
            case TOOL_PLACE: gv.placeVariant = v; break;
            case TOOL_BUILD: gv.buildVariant = v; break;
            case TOOL_ZONE: gv.zoneVariant = v; break;
            case TOOL_SQUAD: gv.squadVariant = v; break;
            default: gv.eventVariant = v; break;
        }
    }

    /**
     * The option picker above the tool bar: every choice for the category as a big button with its picture and
     * name, and a line about the one that's selected.
     */
    void drawPicker(Canvas c) {
        String[] names = optionNames(gv.picker), info = optionInfo(gv.picker);
        int n = names.length, cur = variant(gv.picker);
        gv.pickerCount = n;
        int cols = gv.portrait ? Math.min(4, n) : n > 8 ? (n + 1) / 2 : n;
        int rowsN = (n + cols - 1) / cols;
        float gap = 6 * gv.dp, bh = 74 * gv.dp;
        float maxW = Math.min(gv.getWidth() - 20 * gv.dp, cols * 118 * gv.dp + gap * (cols - 1) + 20 * gv.dp);
        float bw = (maxW - 20 * gv.dp - gap * (cols - 1)) / cols;
        float ph = 44 * gv.dp + rowsN * bh + (rowsN - 1) * gap + 12 * gv.dp;
        float left = (gv.getWidth() - maxW) / 2, top = gv.barTop - ph - 6 * gv.dp;
        gv.pickerPanel.set(left, top, left + maxW, top + ph);
        gv.fill.setColor(0xF0141619);
        c.drawRoundRect(gv.pickerPanel, 14 * gv.dp, 14 * gv.dp, gv.fill);
        gv.text.setTextAlign(Paint.Align.LEFT);
        gv.text.setTextSize(14 * gv.dp);
        gv.text.setColor(0xFFFFFFFF);
        c.drawText(gv.toolName(gv.picker), left + 14 * gv.dp, top + 22 * gv.dp, gv.text);
        gv.text.setTextSize(11.5f * gv.dp);
        gv.text.setColor(0xFFB8BDC4);
        String line = names[cur] + ": " + info[cur];
        float titleW = 0;
        gv.text.setTextSize(14 * gv.dp);
        titleW = gv.text.measureText(gv.toolName(gv.picker));
        gv.text.setTextSize(11.5f * gv.dp);
        float room = maxW - titleW - 40 * gv.dp;
        while (gv.text.measureText(line) > room && line.length() > 4) line = line.substring(0, line.length() - 4) + "...";
        c.drawText(line, left + 24 * gv.dp + titleW, top + 22 * gv.dp, gv.text);
        int saveVariant = cur;
        // ((12.0) One size for the names, as far as it can: each was shrunk on its own, so "Sheriff" stood out large
        // beside a tiny "Highway Patrol".)
        gv.text.setTextSize(11.5f * gv.dp);
        float nameSize = 11.5f * gv.dp;
        for (int k = 0; k < n; k++) nameSize = Math.min(nameSize, 11.5f * gv.dp * Math.min(1, (bw - 8 * gv.dp) / gv.text.measureText(names[k])));
        nameSize = Math.max(nameSize, 9 * gv.dp);
        for (int k = 0; k < n; k++) {
            if (gv.pickerRects[k] == null) gv.pickerRects[k] = new RectF();
            int col = k % cols, row = k / cols;
            float bx = left + 10 * gv.dp + col * (bw + gap), by = top + 34 * gv.dp + row * (bh + gap);
            RectF r = gv.pickerRects[k];
            r.set(bx, by, bx + bw, by + bh);
            boolean sel = k == cur;
            gv.fill.setColor(sel ? 0xFF3A6EA5 : 0xFF23262C);
            c.drawRoundRect(r, 10 * gv.dp, 10 * gv.dp, gv.fill);
            if (sel) {
                gv.stroke.setColor(0xFFA8D0FF);
                gv.stroke.setStrokeWidth(2 * gv.dp);
                c.drawRoundRect(r, 10 * gv.dp, 10 * gv.dp, gv.stroke);
            }
            setVariant(gv.picker, k);
            drawToolIcon(c, gv.picker, r.centerX(), r.top + bh * 0.4f, bh * 0.26f);
            gv.text.setTextAlign(Paint.Align.CENTER);
            gv.text.setTextSize(nameSize);
            gv.text.setColor(sel ? 0xFFFFFFFF : 0xFFC8CCD2);
            String name = names[k];
            float fit = gv.text.measureText(name);
            if (fit > bw - 8 * gv.dp) gv.text.setTextSize(nameSize * (bw - 8 * gv.dp) / fit);
            c.drawText(name, r.centerX(), r.bottom - 8 * gv.dp, gv.text);
        }
        setVariant(gv.picker, saveVariant);
        gv.text.setTextSize(11.5f * gv.dp);
    }

    void drawToolIcon(Canvas c, int t, float cx, float cy, float size) {
        if (t == TOOL_SQUAD) {
            // ((22.4) Three of them together, the one in front largest.)
            int side = SQUAD_SIDE[gv.squadVariant];
            int type = side == 0 ? Entity.SOLDIER : side == 1 ? Entity.COP : side == 2 ? Entity.CIVILIAN : Entity.ZOMBIE;
            Entity e = gv.icons[type];
            int role = e.role, body = e.body, head = e.head;
            int[] roles = SQUAD_ROLES[gv.squadVariant];
            for (int k = 0; k < 3; k++) {
                float ox = k == 0 ? -size * 0.62f : k == 1 ? size * 0.62f : 0, oy = k < 2 ? -size * 0.2f : size * 0.12f;
                if (roles != null && side == 0) {
                    e.role = Math.max(0, roles[((roles.length - 1 - k) % roles.length + roles.length) % roles.length]);
                    e.head = e.role == Entity.ROLE_COMMANDER ? 0xFF8E1F1F : 0xFF3C4628;
                } else if (roles != null && side == 1) e.role = Math.max(0, roles[((roles.length - 1 - k) % roles.length + roles.length) % roles.length]);
                gv.entityRenderer.drawPersonBadge(c, type, e, cx + ox, cy + oy, size * (k < 2 ? 0.62f : 0.78f));
            }
            e.role = role;
            e.body = body;
            e.head = head;
            return;
        }
        int type = t == TOOL_ZOMBIE ? gv.enemyType() : t == TOOL_CIV ? CIV_VARIANTS[gv.civVariant] : TOOL_TYPE[t];
        if (type >= 0) {
            Entity e = gv.icons[type];
            if (type == Entity.COP) {
                int r = t == TOOL_COP ? COP_ROLES[gv.copVariant] : 0;
                if (gv.iconCopBody == 0) gv.iconCopBody = e.body;
                Country land = gv.world.city.country;
                e.role = r < 0 ? 0 : r;
                e.body = r == -1 ? land.hpShirt : r == -2 ? (land.ruralShirt != 0 ? land.ruralShirt : land.hpShirt) : r == -3 ? World.RANGER_SHIRT
                        : r == Entity.ROLE_SAR ? 0xFFE06A20 : gv.iconCopBody;
            }
            if (type == Entity.SOLDIER) {
                // Show the chosen kind of soldier.
                e.role = t == TOOL_MIL ? Math.max(0, MIL_ROLES[gv.milVariant]) : 0;
                e.head = e.role == Entity.ROLE_COMMANDER ? 0xFF8E1F1F : e.role == Entity.ROLE_GUARD ? 0xFF6A6248 : 0xFF3C4628;
                e.body = e.role == Entity.ROLE_GUARD ? 0xFF8C8260 : 0xFF55623A;
                e.radius = e.role == Entity.ROLE_GUNNER ? 4.3f : 4f;
            }
            gv.entityRenderer.drawPersonBadge(c, type, e, cx, cy, size);
            return;
        }
        if (t == TOOL_ORDER) {
            // A waypoint flag with a chevron.
            gv.stroke.setColor(0xFFFFD24A);
            gv.stroke.setStrokeWidth(2.2f * gv.dp);
            c.drawLine(cx - size * 0.7f, cy + size * 0.2f, cx - size * 0.1f, cy - size * 0.5f, gv.stroke);
            c.drawLine(cx - size * 0.1f, cy - size * 0.5f, cx + size * 0.5f, cy + size * 0.2f, gv.stroke);
            gv.fill.setColor(0xFFFFD24A);
            c.drawCircle(cx - size * 0.1f, cy + size * 0.65f, size * 0.22f, gv.fill);
            return;
        }
        drawPlainToolIcon(c, t, cx, cy, size);
    }

    /** ((27.3) From drawToolIcon.) The tools with icons of their own (move, build, clear, order and the rest). */
    void drawPlainToolIcon(Canvas c, int t, float cx, float cy, float size) {
        if (t == TOOL_PAN) {
            gv.stroke.setColor(0xFFE6E6E6);
            gv.stroke.setStrokeWidth(2.2f * gv.dp);
            float a = size * 0.9f;
            c.drawLine(cx - a, cy, cx + a, cy, gv.stroke);
            c.drawLine(cx, cy - a, cx, cy + a, gv.stroke);
            float hd = size * 0.3f;
            c.drawLine(cx - a, cy, cx - a + hd, cy - hd, gv.stroke);
            c.drawLine(cx - a, cy, cx - a + hd, cy + hd, gv.stroke);
            c.drawLine(cx + a, cy, cx + a - hd, cy - hd, gv.stroke);
            c.drawLine(cx + a, cy, cx + a - hd, cy + hd, gv.stroke);
            c.drawLine(cx, cy - a, cx - hd, cy - a + hd, gv.stroke);
            c.drawLine(cx, cy - a, cx + hd, cy - a + hd, gv.stroke);
            c.drawLine(cx, cy + a, cx - hd, cy + a - hd, gv.stroke);
            c.drawLine(cx, cy + a, cx + hd, cy + a - hd, gv.stroke);
        } else if (t == TOOL_PLACE) {
            drawPlaceIcon(c, cx, cy, size);
        } else if (t == TOOL_BUILD) {
            float a = size * 0.85f;
            switch (gv.buildVariant) {
                case City.ED_ROAD:
                    gv.fill.setColor(0xFF3A3C40);
                    c.drawRect(cx - a, cy - a, cx + a, cy + a, gv.fill);
                    gv.fill.setColor(0xFFE8C547);
                    for (int k = -1; k <= 1; k++) c.drawRect(cx - a * 0.08f, cy + k * a * 0.6f - a * 0.18f, cx + a * 0.08f, cy + k * a * 0.6f + a * 0.18f, gv.fill);
                    break;
                case City.ED_PAVE:
                    gv.fill.setColor(0xFFB8B4AC);
                    c.drawRect(cx - a, cy - a, cx + a, cy + a, gv.fill);
                    gv.stroke.setColor(0xFF8A867E);
                    gv.stroke.setStrokeWidth(1 * gv.dp);
                    c.drawLine(cx, cy - a, cx, cy + a, gv.stroke);
                    c.drawLine(cx - a, cy, cx + a, cy, gv.stroke);
                    break;
                case City.ED_GRASS:
                    gv.fill.setColor(0xFF5E9A3A);
                    c.drawRect(cx - a, cy - a, cx + a, cy + a, gv.fill);
                    break;
                case City.ED_TREES:
                    gv.fill.setColor(0xFF3E7A2A);
                    c.drawCircle(cx - a * 0.35f, cy + a * 0.2f, a * 0.55f, gv.fill);
                    c.drawCircle(cx + a * 0.35f, cy - a * 0.2f, a * 0.6f, gv.fill);
                    break;
                case City.ED_WALL:
                    gv.fill.setColor(0xFFC0643C);
                    for (int row = 0; row < 3; row++)
                        for (int k = 0; k < 3; k++) {
                            float bx = cx - size * 0.9f + k * size * 0.62f + (row % 2) * size * 0.3f, by = cy - size * 0.6f + row * size * 0.45f;
                            c.drawRect(bx, by, bx + size * 0.55f, by + size * 0.38f, gv.fill);
                        }
                    break;
                case City.ED_HOUSE:
                case City.ED_SHOP: {
                    boolean shop = gv.buildVariant == City.ED_SHOP;
                    gv.fill.setColor(shop ? 0xFFE8E2D4 : 0xFFE0C8A0);
                    c.drawRect(cx - a * 0.75f, cy - a * 0.1f, cx + a * 0.75f, cy + a * 0.85f, gv.fill);
                    gv.fill.setColor(shop ? 0xFF4F7BE0 : 0xFFB0442A);
                    android.graphics.Path roof = new android.graphics.Path();
                    roof.moveTo(cx - a * 0.95f, cy - a * 0.05f);
                    roof.lineTo(cx, cy - a * 0.85f);
                    roof.lineTo(cx + a * 0.95f, cy - a * 0.05f);
                    roof.close();
                    if (shop) c.drawRect(cx - a * 0.9f, cy - a * 0.4f, cx + a * 0.9f, cy - a * 0.05f, gv.fill);
                    else c.drawPath(roof, gv.fill);
                    break;
                }
                default:
                    gv.stroke.setColor(0xFFE05A4A);
                    gv.stroke.setStrokeWidth(2.4f * gv.dp);
                    c.drawLine(cx - a * 0.7f, cy - a * 0.7f, cx + a * 0.7f, cy + a * 0.7f, gv.stroke);
                    c.drawLine(cx + a * 0.7f, cy - a * 0.7f, cx - a * 0.7f, cy + a * 0.7f, gv.stroke);
                    break;
            }
        } else if (t == TOOL_EVENT && drawEventIcon(c, cx, cy, size)) {
            // ((25.0) Drawn.)
        } else if (t == TOOL_EVENT) {
            // A warning triangle.
            gv.fill.setColor(0xFFFFC24A);
            android.graphics.Path tri = new android.graphics.Path();
            tri.moveTo(cx, cy - size * 0.85f);
            tri.lineTo(cx + size * 0.9f, cy + size * 0.7f);
            tri.lineTo(cx - size * 0.9f, cy + size * 0.7f);
            tri.close();
            c.drawPath(tri, gv.fill);
            gv.fill.setColor(0xFF1A1A1A);
            c.drawRect(cx - size * 0.09f, cy - size * 0.35f, cx + size * 0.09f, cy + size * 0.25f, gv.fill);
            c.drawCircle(cx, cy + size * 0.45f, size * 0.1f, gv.fill);
        } else if (t == TOOL_ZONE) {
            gv.stroke.setColor(0xFF9BD46A);
            gv.stroke.setStrokeWidth(2 * gv.dp);
            gv.oval.set(cx - size * 0.85f, cy + size * 0.25f, cx + size * 0.85f, cy + size * 0.85f);
            c.drawOval(gv.oval, gv.stroke);
            gv.stroke.setColor(0xFFDDDDDD);
            c.drawLine(cx - size * 0.2f, cy + size * 0.55f, cx - size * 0.2f, cy - size * 0.8f, gv.stroke);
            gv.fill.setColor(0xFF4F8FE0);
            c.drawRect(cx - size * 0.2f, cy - size * 0.8f, cx + size * 0.55f, cy - size * 0.3f, gv.fill);
            gv.fill.setColor(0xFFFFFFFF);
            c.drawRect(cx + size * 0.05f, cy - size * 0.66f, cx + size * 0.3f, cy - size * 0.44f, gv.fill);
        } else if (t == TOOL_BOMB) {
            gv.fill.setColor(0xFF15171A);
            c.drawCircle(cx - size * 0.1f, cy + size * 0.15f, size * 0.62f, gv.fill);
            gv.fill.setColor(0x55FFFFFF);
            c.drawCircle(cx - size * 0.3f, cy - size * 0.05f, size * 0.15f, gv.fill);
            gv.stroke.setColor(0xFFB08A5A);
            gv.stroke.setStrokeWidth(2 * gv.dp);
            c.drawLine(cx + size * 0.25f, cy - size * 0.3f, cx + size * 0.55f, cy - size * 0.7f, gv.stroke);
            gv.fill.setColor(0xFFFFB030);
            c.drawCircle(cx + size * 0.6f, cy - size * 0.78f, size * 0.18f, gv.fill);
        } else if (t == TOOL_ERASE) {
            gv.stroke.setColor(0xFFFF6B6B);
            gv.stroke.setStrokeWidth(3 * gv.dp);
            float a = size * 0.55f;
            c.drawLine(cx - a, cy - a, cx + a, cy + a, gv.stroke);
            c.drawLine(cx - a, cy + a, cx + a, cy - a, gv.stroke);
        }
    }

    /**
     * ((25.0) The pictures for some of the events: the airstrike's jet (dimmed, with the seconds left, while it's not
     * ready), a plane coming down, the prison's bars bent apart, a petrol bomb for a riot. False for the rest.
     */
    boolean drawEventIcon(Canvas c, float cx, float cy, float size) {
        switch (gv.eventVariant) {
            case 4: {
                int wait = gv.commander() ? 0 : gv.world.airstrikeWait();
                c.save();
                c.translate(cx, cy);
                c.rotate(-45);
                c.scale(size / 17, size / 17);
                gv.fill.setColor(wait > 0 ? 0xFF4A4E54 : 0xFFB8C0C8);
                gv.vehicleRenderer.drawJetShape(c, 1f);
                c.restore();
                if (wait > 0) {
                    gv.fill.setColor(0xA0101114);
                    c.drawCircle(cx, cy, size * 0.95f, gv.fill);
                    gv.text.setTextAlign(Paint.Align.CENTER);
                    float ts = gv.text.getTextSize();
                    gv.text.setTextSize(size * 0.8f);
                    gv.text.setColor(0xFFFFC24A);
                    c.drawText(wait + "s", cx, cy + size * 0.28f, gv.text);
                    gv.text.setTextSize(ts);
                }
                return true;
            }
            case 12: {
                City.drawAirliner(c, gv.fill, cx, cy, size * 0.2f, 0.6f, 0xFFB8302A);
                float ex = cx - size * 0.45f, ey = cy + size * 0.1f;
                gv.fill.setColor(0xFFFF6A1A);
                c.drawCircle(ex, ey, size * 0.28f, gv.fill);
                gv.fill.setColor(0xFFFFE08A);
                c.drawCircle(ex, ey, size * 0.13f, gv.fill);
                gv.fill.setColor(0x995A5A5A);
                c.drawCircle(ex - size * 0.45f, ey - size * 0.35f, size * 0.24f, gv.fill);
                c.drawCircle(ex - size * 0.8f, ey - size * 0.65f, size * 0.18f, gv.fill);
                return true;
            }
            case 13: {
                gv.fill.setColor(0xFF8A8C90);
                c.drawRect(cx - size * 0.9f, cy - size * 0.85f, cx + size * 0.9f, cy - size * 0.7f, gv.fill);
                c.drawRect(cx - size * 0.9f, cy + size * 0.7f, cx + size * 0.9f, cy + size * 0.85f, gv.fill);
                gv.stroke.setColor(0xFF8A8C90);
                gv.stroke.setStrokeWidth(size * 0.14f);
                for (int k = 0; k < 5; k++) {
                    float bx = cx - size * 0.7f + k * size * 0.35f;
                    if (k == 2) {
                        c.drawLine(bx, cy - size * 0.7f, bx - size * 0.25f, cy, gv.stroke);
                        c.drawLine(bx - size * 0.25f, cy, bx, cy + size * 0.7f, gv.stroke);
                    } else if (k == 3) {
                        c.drawLine(bx, cy - size * 0.7f, bx + size * 0.25f, cy, gv.stroke);
                        c.drawLine(bx + size * 0.25f, cy, bx, cy + size * 0.7f, gv.stroke);
                    } else c.drawLine(bx, cy - size * 0.7f, bx, cy + size * 0.7f, gv.stroke);
                }
                gv.fill.setColor(0xFFE07A2A);
                c.drawCircle(cx, cy - size * 0.1f, size * 0.17f, gv.fill);
                c.drawRect(cx - size * 0.15f, cy + size * 0.05f, cx + size * 0.15f, cy + size * 0.5f, gv.fill);
                return true;
            }
            case 15: {
                // ((26.0) A supply convoy: a lorry with crates, a little one in front.)
                gv.fill.setColor(0xFF55623A);
                c.drawRect(cx - size * 0.9f, cy - size * 0.35f, cx + size * 0.25f, cy + size * 0.35f, gv.fill);
                gv.fill.setColor(0xFF6E7A48);
                c.drawRect(cx + size * 0.25f, cy - size * 0.3f, cx + size * 0.6f, cy + size * 0.3f, gv.fill);
                gv.fill.setColor(0xFFC8A060);
                c.drawRect(cx - size * 0.75f, cy - size * 0.22f, cx - size * 0.35f, cy + size * 0.2f, gv.fill);
                c.drawRect(cx - size * 0.28f, cy - size * 0.22f, cx + size * 0.12f, cy + size * 0.2f, gv.fill);
                gv.fill.setColor(0xFF3C4628);
                c.drawRect(cx + size * 0.72f, cy - size * 0.2f, cx + size * 0.98f, cy + size * 0.2f, gv.fill);
                return true;
            }
            case 16: {
                // ((26.0) An evacuation convoy: a yellow bus.)
                gv.fill.setColor(0xFFE8B030);
                gv.oval.set(cx - size * 0.95f, cy - size * 0.38f, cx + size * 0.95f, cy + size * 0.38f);
                c.drawRoundRect(gv.oval, size * 0.12f, size * 0.12f, gv.fill);
                gv.fill.setColor(0xFF2A3640);
                for (int k = 0; k < 5; k++) c.drawRect(cx - size * 0.75f + k * size * 0.32f, cy - size * 0.24f, cx - size * 0.55f + k * size * 0.32f, cy + size * 0.24f, gv.fill);
                return true;
            }
            case 14: {
                // A bottle with a burning rag.
                c.save();
                c.translate(cx, cy + size * 0.1f);
                c.rotate(25);
                gv.fill.setColor(0xFF4E8A4A);
                gv.oval.set(-size * 0.32f, -size * 0.2f, size * 0.32f, size * 0.8f);
                c.drawRoundRect(gv.oval, size * 0.15f, size * 0.15f, gv.fill);
                c.drawRect(-size * 0.11f, -size * 0.55f, size * 0.11f, -size * 0.15f, gv.fill);
                gv.fill.setColor(0xFFE8DCC0);
                c.drawRect(-size * 0.08f, -size * 0.75f, size * 0.08f, -size * 0.5f, gv.fill);
                gv.fill.setColor(0xFFFF6A1A);
                c.drawCircle(0, -size * 0.88f, size * 0.22f, gv.fill);
                gv.fill.setColor(0xFFFFD24A);
                c.drawCircle(0, -size * 0.84f, size * 0.11f, gv.fill);
                c.restore();
                return true;
            }
            default:
                return false;
        }
    }

    void drawPlaceIcon(Canvas c, float cx, float cy, float size) {
        switch (gv.placeVariant) {
            case 3:
                // Barricade: planks on trestles.
                gv.fill.setColor(0xFF8A6238);
                c.drawRect(cx - size, cy - size * 0.35f, cx + size, cy - size * 0.05f, gv.fill);
                c.drawRect(cx - size, cy + size * 0.15f, cx + size, cy + size * 0.45f, gv.fill);
                gv.fill.setColor(0xFFE8C547);
                c.drawRect(cx - size * 0.8f, cy - size * 0.35f, cx - size * 0.5f, cy - size * 0.05f, gv.fill);
                c.drawRect(cx + size * 0.2f, cy + size * 0.15f, cx + size * 0.5f, cy + size * 0.45f, gv.fill);
                break;
            case 4:
                gv.fill.setColor(0xFF4F5E36);
                c.drawRect(cx - size * 0.8f, cy - size * 0.6f, cx + size * 0.8f, cy + size * 0.6f, gv.fill);
                gv.fill.setColor(0xFFF2F2F2);
                c.drawRect(cx - size * 0.1f, cy - size * 0.45f, cx + size * 0.1f, cy + size * 0.45f, gv.fill);
                c.drawRect(cx - size * 0.45f, cy - size * 0.1f, cx + size * 0.45f, cy + size * 0.1f, gv.fill);
                break;
            case 5:
                // Medkit: a white box with a red cross.
                gv.fill.setColor(0xFFF2F2F2);
                c.drawRect(cx - size * 0.8f, cy - size * 0.6f, cx + size * 0.8f, cy + size * 0.6f, gv.fill);
                gv.fill.setColor(0xFFD83A3A);
                c.drawRect(cx - size * 0.12f, cy - size * 0.45f, cx + size * 0.12f, cy + size * 0.45f, gv.fill);
                c.drawRect(cx - size * 0.45f, cy - size * 0.12f, cx + size * 0.45f, cy + size * 0.12f, gv.fill);
                break;
            case 15:
                // ((26.0) Special forces: four in black, on the ropes from a helicopter's open door.)
                gv.fill.setColor(0xFF6A7480);
                gv.oval.set(cx - size, cy - size * 0.8f, cx + size, cy + size * 0.8f);
                c.drawRoundRect(gv.oval, size * 0.25f, size * 0.25f, gv.fill);
                gv.fill.setColor(0xFF22262A);
                for (int k = 0; k < 4; k++) c.drawCircle(cx + (k % 2 == 0 ? -0.4f : 0.4f) * size, cy + (k < 2 ? -0.35f : 0.35f) * size, size * 0.28f, gv.fill);
                gv.fill.setColor(0xFF121416);
                for (int k = 0; k < 4; k++) c.drawCircle(cx + (k % 2 == 0 ? -0.4f : 0.4f) * size, cy + (k < 2 ? -0.35f : 0.35f) * size, size * 0.15f, gv.fill);
                break;
            case 16:
                // ((26.0) A police launch on the water.)
                gv.fill.setColor(0xFF2E6A9A);
                c.drawRect(cx - size, cy - size * 0.7f, cx + size, cy + size * 0.7f, gv.fill);
                gv.fill.setColor(0xFFF2F2F2);
                gv.oval.set(cx - size * 0.85f, cy - size * 0.35f, cx + size * 0.85f, cy + size * 0.35f);
                c.drawRoundRect(gv.oval, size * 0.3f, size * 0.3f, gv.fill);
                gv.fill.setColor(0xFF1E4AA8);
                c.drawRect(cx - size * 0.7f, cy - size * 0.08f, cx + size * 0.6f, cy + size * 0.08f, gv.fill);
                break;
            default: {
                int col = gv.placeVariant == 0 ? 0xFF2E5FB0 : gv.placeVariant == 1 || gv.placeVariant == 6 ? 0xFF1C1D22 : gv.placeVariant == 7 ? 0xFF9C8A5E : gv.placeVariant == 14 ? 0xFFE4E6EA : 0xFF55623A;
                gv.fill.setColor(col);
                gv.oval.set(cx - size, cy - size * 0.5f, cx + size, cy + size * 0.5f);
                c.drawRoundRect(gv.oval, size * 0.25f, size * 0.25f, gv.fill);
                gv.fill.setColor(0xFF1E2A33);
                c.drawRect(cx + size * 0.2f, cy - size * 0.4f, cx + size * 0.55f, cy + size * 0.4f, gv.fill);
                if (gv.placeVariant == 2) {
                    gv.stroke.setColor(0xFF3A4228);
                    gv.stroke.setStrokeWidth(size * 0.2f);
                    c.drawLine(cx, cy, cx + size * 1.3f, cy, gv.stroke);
                    gv.fill.setColor(0xFF627042);
                    c.drawCircle(cx, cy, size * 0.35f, gv.fill);
                }
                break;
            }
        }
    }
}
