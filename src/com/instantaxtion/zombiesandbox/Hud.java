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
 * ((27.3) Out of GameView.) What's drawn over the map round the edges of the screen: the stats panel and war meter, the
 * minimap with its hot spots, timeline and tactical map, the radio feed, headlines, banners, the
 * loading screen and the on-screen controls; and the text fitting they share.
 */
final class Hud {
    final GameView gv;

    Hud(GameView gv) {
        this.gv = gv;
    }


    /** (10.59) The living (blue), the dead (green) and the armed (gold) over the game so far, in the box given. */
    void drawLiveGraph(Canvas c, float x0, float y0, float x1, float y1) {
        int n = gv.world.histCount;
        if (y1 - y0 < 20 * gv.dp || x1 - x0 < 40 * gv.dp) return;
        gv.fill.setColor(0x40000000);
        c.drawRect(x0, y0, x1, y1, gv.fill);
        if (n < 2) {
            gv.text.setColor(0xFF7A808A);
            gv.text.setTextSize(10.5f * gv.dp);
            c.drawText("Graph fills in as the game goes on", x0 + 6 * gv.dp, (y0 + y1) / 2 + 4 * gv.dp, gv.text);
            return;
        }
        int max = 10;
        for (int i = 0; i < n; i++) max = Math.max(max, Math.max(Math.max(gv.world.histHumans[i], gv.world.histZombies[i]), gv.world.histKills[i]));
        int[][] series = {gv.world.histHumans, gv.world.histZombies, gv.world.histArmed, gv.world.histKills};
        int[] colors = {0xFF6FA8FF, 0xFF7CC24E, 0xFFE8C547, 0xFFE0504A};
        // ((23.0) What each line is.)
        String[] keys = {"People", "Zombies", "Armed", "Kills"};
        gv.text.setTextSize(9.5f * gv.dp);
        float kx = x0 + 4 * gv.dp;
        for (int k = 0; k < 4; k++) {
            gv.fill.setColor(colors[k]);
            c.drawRect(kx, y0 + 4 * gv.dp, kx + 6 * gv.dp, y0 + 10 * gv.dp, gv.fill);
            gv.text.setColor(0xFFB8BDC4);
            c.drawText(keys[k], kx + 8 * gv.dp, y0 + 10 * gv.dp, gv.text);
            kx += 10 * gv.dp + gv.text.measureText(keys[k]) + 6 * gv.dp;
        }
        float pw = x1 - x0 - 4 * gv.dp, ph = y1 - y0 - 18 * gv.dp, bx = x0 + 2 * gv.dp, by = y1 - 3 * gv.dp;
        // (No more than one segment a couple of pixels: a long game has hundreds of samples.)
        int step = Math.max(1, (int) Math.ceil(n / (pw / (2 * gv.dp))));
        gv.stroke.setStrokeWidth(1.6f * gv.dp);
        for (int sIdx = 0; sIdx < 4; sIdx++) {
            gv.stroke.setColor(colors[sIdx]);
            int[] h = series[sIdx];
            // ((12.0) On a square-root scale, as on the Stats screen: the dead and the armed were a flat line along the
            // bottom beside thousands of people.)
            float px = bx, py = by - ph * (float) Math.sqrt(h[0] / (float) max);
            for (int i = step; i < n; i += step) {
                float nx = bx + pw * i / (n - 1), ny = by - ph * (float) Math.sqrt(h[i] / (float) max);
                c.drawLine(px, py, nx, ny, gv.stroke);
                px = nx;
                py = ny;
            }
        }
    }

    /** (10.59) A colour with its alpha scaled down by f. */
    static int fadeCol(int col, float f) {
        if (f >= 1) return col;
        int a = (int) (((col >>> 24) & 0xFF) * f);
        return (a << 24) | (col & 0xFFFFFF);
    }

    /** "Building the city", over a dark screen, with a little spinner. */
    void drawLoading(Canvas c) {
        c.drawColor(0xFF15171B);
        float cx = gv.getWidth() / 2f, cy = gv.getHeight() / 2f;
        gv.text.setTextAlign(Paint.Align.CENTER);
        gv.text.setTextSize(20 * gv.dp);
        gv.text.setColor(0xFFE8E8E8);
        c.drawText("Building the city…", cx, cy - 18 * gv.dp, gv.text);
        gv.text.setTextSize(13 * gv.dp);
        gv.text.setColor(0xFF9AA0A8);
        String stage = City.stage;
        c.drawText(stage == null || stage.isEmpty() ? "Streets, homes, traffic and everyone in them" : stage + "…", cx, cy + 8 * gv.dp, gv.text);
        gv.fill.setStyle(Paint.Style.FILL);
        // (10.32) A bar that fills as the city goes up (the biggest maps take a while).
        gv.shownProgress += (Math.max(gv.shownProgress, City.progress) - gv.shownProgress) * 0.2f;
        float bw = Math.min(gv.getWidth() - 60 * gv.dp, 280 * gv.dp), by = cy + 34 * gv.dp;
        gv.oval.set(cx - bw / 2, by, cx + bw / 2, by + 8 * gv.dp);
        gv.fill.setColor(0xFF2A2E35);
        c.drawRoundRect(gv.oval, 4 * gv.dp, 4 * gv.dp, gv.fill);
        gv.oval.set(cx - bw / 2, by, cx - bw / 2 + bw * Math.min(1, gv.shownProgress), by + 8 * gv.dp);
        gv.fill.setColor(0xFF9BE070);
        c.drawRoundRect(gv.oval, 4 * gv.dp, 4 * gv.dp, gv.fill);
        gv.text.setTextSize(11 * gv.dp);
        gv.text.setColor(0xFF7A8088);
        c.drawText((int) (Math.min(1, gv.shownProgress) * 100) + "%   " + (int) gv.loadingTime + "s", cx, by + 26 * gv.dp, gv.text);
    }

    /** A message across the bottom of the screen. */
    void drawBanner(Canvas c, String msg) {
        gv.text.setTextSize(14 * gv.dp);
        gv.text.setTextAlign(Paint.Align.CENTER);
        float tw = Math.min(gv.getWidth() - 24 * gv.dp, gv.text.measureText(msg) + 32 * gv.dp);
        float y = gv.getHeight() - 70 * gv.dp;
        gv.fill.setStyle(Paint.Style.FILL);
        gv.fill.setColor(0xEE3A1E1E);
        gv.bannerRect.set(gv.getWidth() / 2f - tw / 2, y - 26 * gv.dp, gv.getWidth() / 2f + tw / 2, y + 12 * gv.dp);
        c.drawRoundRect(gv.bannerRect, 10 * gv.dp, 10 * gv.dp, gv.fill);
        gv.text.setColor(0xFFFFE0E0);
        c.drawText(msg, gv.getWidth() / 2f, y, gv.text);
    }

    /** The minimap: the whole city small, with zombies, people, safe zones, vehicles and the camera view. */
    void drawMinimap(Canvas c) {
        // While you're controlling someone, the attack button goes where the minimap was.
        gv.miniX.setEmpty();
        gv.mapChip.setEmpty();
        gv.tacBtn.setEmpty();
        gv.overlayBtn.setEmpty();
        gv.overlayKey.setEmpty();
        gv.overlayChip.setEmpty();
        gv.hotBtn.setEmpty();
        gv.hotChip.setEmpty();
        gv.timeBtn.setEmpty();
        gv.timeChip.setEmpty();
        if (!gv.settings.minimap() || gv.world.controlled != null || gv.miniClosed) {
            gv.miniRect.setEmpty();
        } else {
            float size = gv.portrait ? Math.min(gv.getWidth() - gv.statsRect.right - 18 * gv.dp, 120 * gv.dp) : Math.min(130 * gv.dp, gv.getHeight() * 0.3f);
            if (gv.portrait && size < 70 * gv.dp) size = 90 * gv.dp;
            if (gv.portrait && size >= 70 * gv.dp && gv.getWidth() - gv.statsRect.right - 18 * gv.dp >= 70 * gv.dp)
                gv.miniRect.set(gv.getWidth() - 10 * gv.dp - size, gv.statsRect.top, gv.getWidth() - 10 * gv.dp, gv.statsRect.top + size);
            else {
                gv.miniRect.set(gv.getWidth() - 10 * gv.dp - size, uiFloor() - 10 * gv.dp - size, gv.getWidth() - 10 * gv.dp, uiFloor() - 10 * gv.dp);
                // ((12.0) Down there it's where a card opens: hidden while one is, rather than poking out round it.)
                if (gv.picker < 0 && (gv.follow != null || gv.followV != null || (gv.tool == TOOL_PAN && (gv.inspectZ != null || gv.inspectB != null || gv.inspectBase != null))
                        // ((12.0) Or over the open stats panel, with big text on a narrow screen.)
                        || (!gv.statsCollapsed && RectF.intersects(gv.miniRect, gv.statsShown)))) {
                    gv.miniRect.setEmpty();
                    gv.camChip.setEmpty();
                    return;
                }
            }
            // ((12.0) The fallen buildings counted once a second, not every frame: tens of thousands on a big map.)
            if (gv.world != gv.miniCountWorld || gv.world.time - gv.miniCountAt > 1 || gv.world.time < gv.miniCountAt) {
                gv.miniCountWorld = gv.world;
                gv.miniCountAt = gv.world.time;
                int n = 0;
                for (int i = 0; i < gv.world.city.buildings.size(); i++) if (gv.world.city.buildings.get(i).collapsed) n++;
                gv.miniCount = n;
            }
            int collapsed = gv.miniCount;
            if (gv.miniMap == null || gv.miniWorld != gv.world || gv.miniCollapsed != collapsed) {
                // Redrawn only when the city changes.
                gv.miniWorld = gv.world;
                gv.miniCollapsed = collapsed;
                gv.miniMap = android.graphics.Bitmap.createBitmap(MINI, MINI, android.graphics.Bitmap.Config.ARGB_8888);
                Canvas mc = new Canvas(gv.miniMap);
                Paint p = new Paint(Paint.FILTER_BITMAP_FLAG);
                gv.roofDst.set(0, 0, MINI, MINI);
                gv.world.city.drawMap(mc, 0, 0, -1, -1, gv.roofDst, p);
                gv.miniTimer = 0;
            }
            gv.fill.setColor(0xE0101216);
            gv.oval.set(gv.miniRect.left - 3 * gv.dp, gv.miniRect.top - 3 * gv.dp, gv.miniRect.right + 3 * gv.dp, gv.miniRect.bottom + 3 * gv.dp);
            c.drawRoundRect(gv.oval, 8 * gv.dp, 8 * gv.dp, gv.fill);
            // The people and the infection heatmap are drawn into a copy of the map a few times a second, so
            // the minimap costs one picture per frame however many people there are.
            gv.miniTimer -= 1 / 60f;
            if (gv.miniDots == null || gv.miniTimer <= 0) {
                gv.miniTimer = 0.2f;
                drawMiniDots();
            }
            // (10.50) On the bigger maps it shows the part of the city round the view, not all of it at once: the
            // whole of a big map in a corner of the screen was too small to make anything out.
            float ww = gv.world.city.worldW(), wh = gv.world.city.worldH(), f = Math.min(1, 260 * City.T / Math.max(ww, wh));
            gv.miniW = ww * f;
            gv.miniH = wh * f;
            float ccx = gv.camX + gv.getWidth() / gv.scale / 2, ccy = gv.camY + gv.barTop / gv.scale / 2;
            gv.miniX0 = Math.max(0, Math.min(ww - gv.miniW, ccx - gv.miniW / 2));
            gv.miniY0 = Math.max(0, Math.min(wh - gv.miniH, ccy - gv.miniH / 2));
            gv.miniSrc.set((int) (gv.miniX0 / ww * MINI), (int) (gv.miniY0 / wh * MINI), (int) ((gv.miniX0 + gv.miniW) / ww * MINI), (int) ((gv.miniY0 + gv.miniH) / wh * MINI));
            gv.bmpPaint.setAlpha(230);
            c.drawBitmap(gv.miniDots, gv.miniSrc, gv.miniRect, gv.bmpPaint);
            gv.bmpPaint.setAlpha(255);
            float sx = gv.miniRect.width() / gv.miniW;
            c.save();
            c.clipRect(gv.miniRect);
            for (int i = 0, n = gv.world.dispatch.zones.size(); i < n; i++) {
                Dispatch.SafeZone z = gv.world.dispatch.zones.get(i);
                gv.fill.setColor(z.military ? 0x886FBF3F : 0x884F8FE0);
                c.drawCircle(mx(z.x), my(z.y), Math.max(2.5f * gv.dp, z.r * sx), gv.fill);
            }
            // ((19.5) Where something big just happened: a ring spreading out on the map.)
            for (int i = 0, n = gv.vfx.pings.size(); i < n; i++) {
                float[] p = gv.vfx.pings.get(i);
                float k = 1 - p[2] / 1.6f;
                gv.stroke.setStrokeWidth(1.5f * gv.dp);
                gv.stroke.setColor(alpha(p[3] > 0 ? 0xFFE8D8B0 : 0xFFFF9A40, 1 - k));
                c.drawCircle(mx(p[0]), my(p[1]), (2 + k * 9) * gv.dp, gv.stroke);
            }
            // ((12.0) The open 911 calls: a small red mark at each, bigger the more of the dead there.)
            for (int i = 0, n = gv.world.dispatch.incidents.size(); i < n; i++) {
                Dispatch.Incident inc = gv.world.dispatch.incidents.get(i);
                if (inc.resolved || inc.cordonPost >= 0 || inc.checkpointSpot != null) continue;
                float ix = mx(inc.x), iy = my(inc.y), ir = (1.8f + Math.min(2.5f, inc.zombiesNear / 6f)) * gv.dp;
                gv.fill.setColor(0xE0101216);
                c.drawCircle(ix, iy, ir + 1.2f * gv.dp, gv.fill);
                gv.fill.setColor(inc.zombiesNear > 0 ? 0xFFFF5A4A : 0xFFFFA64D);
                c.drawCircle(ix, iy, ir, gv.fill);
            }
            if (gv.world.rangerStation != null) {
                // The ranger station: a green square.
                float rx = mx(gv.world.rangerStation.doorX), ry = my(gv.world.rangerStation.doorY);
                gv.fill.setColor(0xFF3E8A3A);
                c.drawRect(rx - 2.5f * gv.dp, ry - 2.5f * gv.dp, rx + 2.5f * gv.dp, ry + 2.5f * gv.dp, gv.fill);
                gv.fill.setColor(0xFFE6E6E6);
                c.drawRect(rx - 1f * gv.dp, ry - 1f * gv.dp, rx + 1f * gv.dp, ry + 1f * gv.dp, gv.fill);
            }
            // Reinforcements on their way in: a line from the head of the column to where they're going.
            for (int i = 0; i < gv.world.dispatch.convoys.size(); i++) {
                Dispatch.Convoy cv = gv.world.dispatch.convoys.get(i);
                int col = WorldRenderer.convoyColor(cv);
                gv.stroke.setColor(col);
                gv.stroke.setStrokeWidth(1.5f * gv.dp);
                float hx = mx(cv.x), hy = my(cv.y);
                c.drawLine(hx, hy, mx(cv.toX), my(cv.toY), gv.stroke);
                gv.fill.setColor(col);
                c.drawCircle(hx, hy, (2.5f + 1.2f * (float) Math.abs(Math.sin(gv.world.time * 5))) * gv.dp, gv.fill);
            }
            // Calls for backup: a ring going out on the minimap too.
            for (int i = 0; i < gv.world.dispatch.callouts.size(); i++) {
                Dispatch.Callout co = gv.world.dispatch.callouts.get(i);
                gv.stroke.setColor(alpha(co.color, Math.max(0, 1 - co.age / 6f)));
                gv.stroke.setStrokeWidth(1.2f * gv.dp);
                c.drawCircle(mx(co.x), my(co.y), (2 + (co.age * 6) % 6) * gv.dp, gv.stroke);
            }
            // ((11.5) The cars and trucks are in the picture with the people now, drawn a few times a second.)
            // (10.50) Helicopters in the air: a rotor mark in the colour of who flies it, and a line to where it's going.
            for (int i = 0, n = gv.world.fleet.vehicles.size(); i < n; i++) {
                Fleet.Vehicle v = gv.world.fleet.vehicles.get(i);
                if (v.type != Fleet.HELI || !Fleet.airborne(v)) continue;
                int col = v.rescue ? 0xFFFF8A3A : v.kind == Fleet.K_POLICE_HELI ? 0xFF6FA8FF : 0xFFFFE27A;
                float hx = mx(v.x), hy = my(v.y);
                if (World.hypot(v.tx - v.x, v.ty - v.y) > 60) {
                    gv.stroke.setColor(alpha(col, 0.7f));
                    gv.stroke.setStrokeWidth(1.2f * gv.dp);
                    c.drawLine(hx, hy, mx(v.tx), my(v.ty), gv.stroke);
                }
                gv.fill.setColor(0xE0101216);
                c.drawCircle(hx, hy, 4.2f * gv.dp, gv.fill);
                gv.stroke.setColor(col);
                gv.stroke.setStrokeWidth(1.6f * gv.dp);
                float a = gv.world.time * 9, rr = 3.6f * gv.dp;
                c.drawLine(hx - (float) Math.cos(a) * rr, hy - (float) Math.sin(a) * rr, hx + (float) Math.cos(a) * rr, hy + (float) Math.sin(a) * rr, gv.stroke);
                c.drawLine(hx + (float) Math.sin(a) * rr, hy - (float) Math.cos(a) * rr, hx - (float) Math.sin(a) * rr, hy + (float) Math.cos(a) * rr, gv.stroke);
                gv.fill.setColor(col);
                c.drawCircle(hx, hy, 1.4f * gv.dp, gv.fill);
            }
            // ((12.0) The people you've pinned: a gold star-point each, and whoever you're following or controlling
            // ringed.)
            for (int i = 0; i < gv.pinned.size(); i++) {
                Entity p = gv.pinned.get(i);
                float[] at = gv.mapLabels.pinAt(p);
                if (at == null) continue;
                float px = mx(at[0]), py = my(at[1]);
                gv.fill.setColor(0xE0101216);
                c.drawCircle(px, py, 3.4f * gv.dp, gv.fill);
                gv.fill.setColor(0xFFE8C547);
                c.drawCircle(px, py, 2.2f * gv.dp, gv.fill);
            }
            Entity you = gv.world.controlled != null ? gv.world.controlled : gv.follow;
            if (you != null && !you.dead) {
                gv.stroke.setColor(0xFFFFFFFF);
                gv.stroke.setStrokeWidth(1.4f * gv.dp);
                c.drawCircle(mx(you.x), my(you.y), (3.5f + 1.5f * (float) Math.abs(Math.sin(gv.world.time * 4))) * gv.dp, gv.stroke);
            }
            gv.stroke.setColor(0xFFFFFFFF);
            gv.stroke.setStrokeWidth(1 * gv.dp);
            c.drawRect(mx(gv.camX), my(gv.camY), mx((gv.camX + gv.getWidth() / gv.scale)),
                    my((gv.camY + gv.barTop / gv.scale)), gv.stroke);
            c.restore();
            // (10.32) And a button in the other corner opens the tactical map.
            gv.tacBtn.set(gv.miniRect.left - 2 * gv.dp, gv.miniRect.top - 2 * gv.dp, gv.miniRect.left + 16 * gv.dp, gv.miniRect.top + 16 * gv.dp);
            gv.fill.setColor(0xE0101216);
            c.drawCircle(gv.tacBtn.centerX(), gv.tacBtn.centerY(), 8 * gv.dp, gv.fill);
            gv.stroke.setColor(0xFFC8CCD2);
            gv.stroke.setStrokeWidth(1.4f * gv.dp);
            float bx = gv.tacBtn.centerX(), by = gv.tacBtn.centerY(), bu = 3.6f * gv.dp;
            c.drawRect(bx - bu, by - bu, bx + bu, by + bu, gv.stroke);
            c.drawLine(bx - bu, by, bx + bu, by, gv.stroke);
            c.drawLine(bx, by - bu, bx, by + bu, gv.stroke);
            // ((13.8) And one in the corner below it opens the hot spots list.)
            gv.hotBtn.set(gv.miniRect.left - 2 * gv.dp, gv.miniRect.bottom - 16 * gv.dp, gv.miniRect.left + 16 * gv.dp, gv.miniRect.bottom + 2 * gv.dp);
            drawHotIcon(c, gv.hotBtn.centerX(), gv.hotBtn.centerY());
            // ((14.4) And the last corner, the timeline: a little clock.)
            gv.timeBtn.set(gv.miniRect.right - 16 * gv.dp, gv.miniRect.bottom - 16 * gv.dp, gv.miniRect.right + 2 * gv.dp, gv.miniRect.bottom + 2 * gv.dp);
            drawClockIcon(c, gv.timeBtn.centerX(), gv.timeBtn.centerY());
            // ((27.5) Halfway down the left edge, the overlays: a tap goes on to the next (heat, reach, off), with its key below the minimap.)
            gv.overlayBtn.set(gv.miniRect.left - 10 * gv.dp, gv.miniRect.centerY() - 9 * gv.dp, gv.miniRect.left + 8 * gv.dp, gv.miniRect.centerY() + 9 * gv.dp);
            drawOverlayIcon(c, gv.overlayBtn.centerX(), gv.overlayBtn.centerY());
            if (gv.overlays.mode != Overlays.OFF) drawOverlayKey(c, gv.miniRect.right, gv.miniRect.bottom + 6 * gv.dp);
            // A little x in the corner closes it.
            gv.miniX.set(gv.miniRect.right - 16 * gv.dp, gv.miniRect.top - 2 * gv.dp, gv.miniRect.right + 2 * gv.dp, gv.miniRect.top + 16 * gv.dp);
            gv.fill.setColor(0xE0101216);
            c.drawCircle(gv.miniX.centerX(), gv.miniX.centerY(), 8 * gv.dp, gv.fill);
            gv.stroke.setColor(0xFFC8CCD2);
            gv.stroke.setStrokeWidth(1.6f * gv.dp);
            float mx = gv.miniX.centerX(), my = gv.miniX.centerY(), u = 3.5f * gv.dp;
            c.drawLine(mx - u, my - u, mx + u, my + u, gv.stroke);
            c.drawLine(mx - u, my + u, mx + u, my - u, gv.stroke);
        }
        // (No Auto cam since 10.50.) Closed: a chip where the minimap was opens it again.
        gv.camChip.setEmpty();
        if (gv.miniClosed && gv.settings.minimap() && gv.world.controlled == null) {
            gv.text.setTextSize(11.5f * gv.dp);
            gv.text.setTextAlign(Paint.Align.CENTER);
            float mw = gv.text.measureText("Map") + 20 * gv.dp;
            float right = gv.getWidth() - 10 * gv.dp, top = uiFloor() - 40 * gv.dp;
            gv.mapChip.set(right - mw, top, right, top + 28 * gv.dp);
            gv.fill.setColor(0xE0202227);
            c.drawRoundRect(gv.mapChip, 9 * gv.dp, 9 * gv.dp, gv.fill);
            gv.text.setColor(0xFFF2F2F2);
            c.drawText("Map", gv.mapChip.centerX(), gv.mapChip.centerY() + 4 * gv.dp, gv.text);
            // ((13.8) And beside it, the hot spots.)
            float hw = gv.text.measureText("Hot spots") + 20 * gv.dp;
            gv.hotChip.set(gv.mapChip.left - 8 * gv.dp - hw, top, gv.mapChip.left - 8 * gv.dp, top + 28 * gv.dp);
            gv.fill.setColor(0xE0202227);
            c.drawRoundRect(gv.hotChip, 9 * gv.dp, 9 * gv.dp, gv.fill);
            gv.text.setColor(0xFFFFB0A0);
            c.drawText("Hot spots", gv.hotChip.centerX(), gv.hotChip.centerY() + 4 * gv.dp, gv.text);
            float tw = gv.text.measureText("Timeline") + 20 * gv.dp;
            gv.timeChip.set(gv.hotChip.left - 8 * gv.dp - tw, top, gv.hotChip.left - 8 * gv.dp, top + 28 * gv.dp);
            gv.fill.setColor(0xE0202227);
            c.drawRoundRect(gv.timeChip, 9 * gv.dp, 9 * gv.dp, gv.fill);
            gv.text.setColor(0xFFC8D8F0);
            c.drawText("Timeline", gv.timeChip.centerX(), gv.timeChip.centerY() + 4 * gv.dp, gv.text);
            float ow = gv.text.measureText("Overlay") + 20 * gv.dp;
            gv.overlayChip.set(gv.timeChip.left - 8 * gv.dp - ow, top, gv.timeChip.left - 8 * gv.dp, top + 28 * gv.dp);
            gv.fill.setColor(gv.overlays.mode != Overlays.OFF ? 0xE0404650 : 0xE0202227);
            c.drawRoundRect(gv.overlayChip, 9 * gv.dp, 9 * gv.dp, gv.fill);
            gv.text.setColor(0xFFD8E8C8);
            c.drawText("Overlay", gv.overlayChip.centerX(), gv.overlayChip.centerY() + 4 * gv.dp, gv.text);
            if (gv.overlays.mode != Overlays.OFF) drawOverlayKey(c, gv.mapChip.right, top - 30 * gv.dp);
        }
    }

    /** ((27.5) Three stacked squares, the top one tinted when an overlay is on. */
    void drawOverlayIcon(Canvas c, float x, float y) {
        gv.fill.setColor(0xE0101216);
        c.drawCircle(x, y, 9 * gv.dp, gv.fill);
        float u = 3.2f * gv.dp;
        gv.stroke.setColor(0xFFC8CCD2);
        gv.stroke.setStrokeWidth(1.2f * gv.dp);
        c.drawRect(x - u + 1.6f * gv.dp, y - u + 1.6f * gv.dp, x + u + 1.6f * gv.dp, y + u + 1.6f * gv.dp, gv.stroke);
        gv.fill.setColor(gv.overlays.mode == Overlays.HEAT ? 0xFFE05030 : gv.overlays.mode == Overlays.REACH ? 0xFF58A8FF : 0xFF60646C);
        c.drawRect(x - u - 1.2f * gv.dp, y - u - 1.2f * gv.dp, x + u - 1.2f * gv.dp, y + u - 1.2f * gv.dp, gv.fill);
    }

    /** ((27.5) The overlay's name and its shades, right-aligned at (right, top); a tap on it goes on to the next. */
    void drawOverlayKey(Canvas c, float right, float top) {
        int[] cols = gv.overlays.mode == Overlays.HEAT ? Overlays.HEAT_COL : Overlays.REACH_COL;
        String name = Overlays.NAMES[gv.overlays.mode];
        String lo = gv.overlays.mode == Overlays.HEAT ? "few" : "near", hi = gv.overlays.mode == Overlays.HEAT ? "many" : "nobody";
        gv.text.setTextSize(11 * gv.dp);
        gv.text.setTextAlign(Paint.Align.LEFT);
        float sw = 11 * gv.dp, nw = gv.text.measureText(name), lw = gv.text.measureText(lo), hw = gv.text.measureText(hi);
        float width = nw + lw + hw + 4 * sw + 34 * gv.dp, left = right - width;
        gv.overlayKey.set(left, top, right, top + 22 * gv.dp);
        gv.fill.setColor(0xE0101216);
        c.drawRoundRect(gv.overlayKey, 8 * gv.dp, 8 * gv.dp, gv.fill);
        float x = left + 8 * gv.dp, base = top + 15 * gv.dp;
        gv.text.setColor(0xFFF2F2F2);
        c.drawText(name, x, base, gv.text);
        x += nw + 8 * gv.dp;
        gv.text.setColor(0xFFA8ACB4);
        c.drawText(lo, x, base, gv.text);
        x += lw + 4 * gv.dp;
        for (int i = 1; i <= 4; i++) {
            gv.fill.setColor(cols[i] | 0xFF000000);
            c.drawRect(x, top + 6 * gv.dp, x + sw - 1 * gv.dp, top + 16 * gv.dp, gv.fill);
            x += sw;
        }
        x += 4 * gv.dp;
        c.drawText(hi, x, base, gv.text);
    }

    void openHot() {
        gv.click();
        gv.hotOpen = true;
        gv.hotDown = false;
        gv.tactical = false;
        gv.hotAt = -99;
    }

    /** A small red circle with a "!" in it. */
    void drawHotIcon(Canvas c, float x, float y) {
        gv.fill.setColor(0xE0101216);
        c.drawCircle(x, y, 8 * gv.dp, gv.fill);
        gv.fill.setColor(0xFFE0503C);
        c.drawCircle(x, y, 6 * gv.dp, gv.fill);
        gv.fill.setColor(0xFFFFFFFF);
        c.drawRect(x - 0.9f * gv.dp, y - 3.6f * gv.dp, x + 0.9f * gv.dp, y + 0.8f * gv.dp, gv.fill);
        c.drawCircle(x, y + 2.6f * gv.dp, 1 * gv.dp, gv.fill);
    }

    void drawHot(Canvas c) {
        // (Worked out again once a second while it's open.)
        if (gv.hotWorld != gv.world || gv.world.time - gv.hotAt > 1 || gv.world.time < gv.hotAt) {
            gv.hotWorld = gv.world;
            gv.hotAt = gv.world.time;
            gv.hot = gv.world.hotSpots(HOT_MAX);
        }
        gv.fill.setColor(0xF0101216);
        c.drawRect(0, 0, gv.getWidth(), gv.getHeight(), gv.fill);
        gv.text.setTextAlign(Paint.Align.CENTER);
        gv.text.setTextSize(16 * gv.dp);
        gv.text.setColor(0xFFF2F2F2);
        c.drawText("Hot spots", gv.getWidth() / 2f, 30 * gv.dp, gv.text);
        float left = 16 * gv.dp, right = gv.getWidth() - 16 * gv.dp, y = 56 * gv.dp, rowH = 50 * gv.dp;
        gv.hotRows.clear();
        if (gv.hot.isEmpty()) {
            gv.text.setTextSize(12.5f * gv.dp);
            gv.text.setColor(0xFFB8BCC4);
            c.drawText("No fighting anywhere right now.", gv.getWidth() / 2f, y + 30 * gv.dp, gv.text);
        }
        for (int i = 0; i < gv.hot.size() && y + rowH < gv.getHeight() - 8 * gv.dp; i++) {
            World.HotSpot h = gv.hot.get(i);
            RectF r = pooled(gv.hotPool, gv.hotRows.size());
            r.set(left, y, right, y + rowH - 6 * gv.dp);
            gv.hotRows.add(r);
            gv.fill.setColor(0xE0262A30);
            c.drawRoundRect(r, 9 * gv.dp, 9 * gv.dp, gv.fill);
            // A bar for how bad it is next to the worst.
            float share = (float) h.dead / Math.max(1, gv.hot.get(0).dead);
            gv.fill.setColor(0xFFC8402E);
            c.drawRect(r.left, r.bottom - 3 * gv.dp, r.left + (r.width()) * share, r.bottom, gv.fill);
            gv.text.setTextAlign(Paint.Align.LEFT);
            gv.text.setTextSize(13 * gv.dp);
            gv.text.setColor(0xFFF2F2F2);
            String name = (h.zone ? "Safe zone: " : "") + h.place;
            c.drawText(fitText(name, r.width() - 24 * gv.dp), r.left + 12 * gv.dp, r.top + 19 * gv.dp, gv.text);
            gv.text.setTextSize(11.5f * gv.dp);
            gv.text.setColor(0xFFB8BCC4);
            String line = h.dead + " of the dead   " + (h.armed == 0 ? "no police or soldiers" : h.armed + (h.armed == 1 ? " officer or soldier" : " police and soldiers"));
            c.drawText(fitText(line, r.width() - 24 * gv.dp), r.left + 12 * gv.dp, r.top + 36 * gv.dp, gv.text);
            y += rowH;
        }
        gv.text.setTextAlign(Paint.Align.CENTER);
        gv.hotClose.set(gv.getWidth() - 44 * gv.dp, 8 * gv.dp, gv.getWidth() - 8 * gv.dp, 40 * gv.dp);
        gv.fill.setColor(0xE0303238);
        c.drawRoundRect(gv.hotClose, 9 * gv.dp, 9 * gv.dp, gv.fill);
        gv.stroke.setColor(0xFFE0E0E0);
        gv.stroke.setStrokeWidth(2 * gv.dp);
        float mx = gv.hotClose.centerX(), my = gv.hotClose.centerY(), u = 6 * gv.dp;
        c.drawLine(mx - u, my - u, mx + u, my + u, gv.stroke);
        c.drawLine(mx - u, my + u, mx + u, my - u, gv.stroke);
    }

    void hotTap(float x, float y) {
        gv.click();
        for (int i = 0; i < gv.hotRows.size() && i < gv.hot.size(); i++) {
            if (!gv.hotRows.get(i).contains(x, y)) continue;
            World.HotSpot h = gv.hot.get(i);
            gv.follow = null;
            gv.followV = null;
            gv.camX = h.x - gv.getWidth() / gv.scale / 2;
            gv.camY = h.y - gv.barTop / gv.scale / 2;
            gv.clampCamera();
            break;
        }
        gv.hotOpen = false;
    }

    void openTimeline() {
        gv.click();
        gv.timeOpen = true;
        gv.timeDown = false;
        gv.hotOpen = false;
        gv.tactical = false;
    }

    void drawClockIcon(Canvas c, float x, float y) {
        gv.fill.setColor(0xE0101216);
        c.drawCircle(x, y, 8 * gv.dp, gv.fill);
        gv.stroke.setColor(0xFFC8D8F0);
        gv.stroke.setStrokeWidth(1.4f * gv.dp);
        c.drawCircle(x, y, 5.2f * gv.dp, gv.stroke);
        c.drawLine(x, y, x, y - 3.4f * gv.dp, gv.stroke);
        c.drawLine(x, y, x + 2.6f * gv.dp, y, gv.stroke);
    }

    void drawTimeline(Canvas c) {
        gv.fill.setColor(0xF0101216);
        c.drawRect(0, 0, gv.getWidth(), gv.getHeight(), gv.fill);
        gv.text.setTextAlign(Paint.Align.CENTER);
        gv.text.setTextSize(16 * gv.dp);
        gv.text.setColor(0xFFF2F2F2);
        c.drawText("Timeline", gv.getWidth() / 2f, 30 * gv.dp, gv.text);
        gv.timeRows.clear();
        gv.timeShown.clear();
        java.util.List<World.Moment> all = gv.world.timeline;
        float left = 16 * gv.dp, right = gv.getWidth() - 16 * gv.dp, y = 52 * gv.dp, rowH = 40 * gv.dp;
        if (all.isEmpty()) {
            gv.text.setTextSize(12.5f * gv.dp);
            gv.text.setColor(0xFFB8BCC4);
            c.drawText("Nothing has happened yet.", gv.getWidth() / 2f, y + 30 * gv.dp, gv.text);
        }
        for (int i = all.size() - 1; i >= 0 && y + rowH < gv.getHeight() - 8 * gv.dp; i--) {
            World.Moment m = all.get(i);
            boolean there = !Float.isNaN(m.x);
            RectF r = pooled(gv.timePool, gv.timeRows.size());
            r.set(left, y, right, y + rowH - 5 * gv.dp);
            gv.timeRows.add(r);
            gv.timeShown.add(m);
            gv.fill.setColor(there ? 0xE0262A30 : 0xC01C1F24);
            c.drawRoundRect(r, 8 * gv.dp, 8 * gv.dp, gv.fill);
            gv.text.setTextAlign(Paint.Align.LEFT);
            gv.text.setTextSize(11.5f * gv.dp);
            gv.text.setColor(0xFF9AA4B4);
            String when = World.clock((int) m.time);
            c.drawText(when, r.left + 10 * gv.dp, r.centerY() + 4 * gv.dp, gv.text);
            float tx = r.left + 10 * gv.dp + gv.text.measureText("00:00:00") + 8 * gv.dp;
            gv.text.setTextSize(12.5f * gv.dp);
            gv.text.setColor(there ? 0xFFF2F2F2 : 0xFFC8CCD2);
            c.drawText(fitText(m.text, r.right - tx - (there ? 24 : 10) * gv.dp), tx, r.centerY() + 4 * gv.dp, gv.text);
            if (there) {
                // (A little arrow: this one can be gone to.)
                gv.stroke.setColor(0xFF8CA8D8);
                gv.stroke.setStrokeWidth(1.6f * gv.dp);
                float ax = r.right - 14 * gv.dp, ay = r.centerY();
                c.drawLine(ax - 4 * gv.dp, ay - 4 * gv.dp, ax, ay, gv.stroke);
                c.drawLine(ax - 4 * gv.dp, ay + 4 * gv.dp, ax, ay, gv.stroke);
            }
            y += rowH;
        }
        gv.text.setTextAlign(Paint.Align.CENTER);
        gv.timeClose.set(gv.getWidth() - 44 * gv.dp, 8 * gv.dp, gv.getWidth() - 8 * gv.dp, 40 * gv.dp);
        gv.fill.setColor(0xE0303238);
        c.drawRoundRect(gv.timeClose, 9 * gv.dp, 9 * gv.dp, gv.fill);
        gv.stroke.setColor(0xFFE0E0E0);
        gv.stroke.setStrokeWidth(2 * gv.dp);
        float mx = gv.timeClose.centerX(), my = gv.timeClose.centerY(), u = 6 * gv.dp;
        c.drawLine(mx - u, my - u, mx + u, my + u, gv.stroke);
        c.drawLine(mx - u, my + u, mx + u, my - u, gv.stroke);
    }

    void timelineTap(float x, float y) {
        gv.click();
        for (int i = 0; i < gv.timeRows.size() && i < gv.timeShown.size(); i++) {
            if (!gv.timeRows.get(i).contains(x, y)) continue;
            World.Moment m = gv.timeShown.get(i);
            if (Float.isNaN(m.x)) return;
            gv.follow = null;
            gv.followV = null;
            gv.camX = m.x - gv.getWidth() / gv.scale / 2;
            gv.camY = m.y - gv.barTop / gv.scale / 2;
            gv.clampCamera();
            break;
        }
        gv.timeOpen = false;
    }

    void drawTactical(Canvas c) {
        City city = gv.world.city;
        gv.fill.setColor(0xFA101216);
        c.drawRect(0, 0, gv.getWidth(), gv.getHeight(), gv.fill);
        gv.tacUsed.clear();
        // ((12.0) Held sideways, the key goes beside the map rather than under it, and the map takes the height.)
        boolean keySide = !gv.portrait && gv.getWidth() - 24 * gv.dp - (gv.getHeight() - 46 * gv.dp - 16 * gv.dp) >= 180 * gv.dp;
        float top = 46 * gv.dp, size = keySide ? gv.getHeight() - top - 16 * gv.dp : Math.min(gv.getWidth() - 24 * gv.dp, gv.getHeight() - top - 70 * gv.dp);
        float aspect = city.worldW() / city.worldH();
        float mw = aspect >= 1 ? size : size * aspect, mh = aspect >= 1 ? size / aspect : size;
        float tacL = keySide ? Math.max(12 * gv.dp, (gv.getWidth() - mw - 190 * gv.dp) / 2) : (gv.getWidth() - mw) / 2;
        gv.tacRect.set(tacL, top, tacL + mw, top + mh);
        int collapsed = 0;
        for (int i = 0; i < city.buildings.size(); i++) if (city.buildings.get(i).collapsed) collapsed++;
        if (gv.tacMap == null || gv.tacWorld != gv.world || gv.tacCollapsed != collapsed) {
            gv.tacWorld = gv.world;
            gv.tacCollapsed = collapsed;
            int bw = 512, bh = Math.max(64, Math.round(512 / aspect));
            if (gv.tacMap == null || gv.tacMap.getWidth() != bw || gv.tacMap.getHeight() != bh)
                gv.tacMap = android.graphics.Bitmap.createBitmap(bw, bh, android.graphics.Bitmap.Config.ARGB_8888);
            Canvas mc = new Canvas(gv.tacMap);
            Paint p = new Paint(Paint.FILTER_BITMAP_FLAG);
            gv.roofDst.set(0, 0, bw, bh);
            city.drawMap(mc, 0, 0, -1, -1, gv.roofDst, p);
            // Dimmed, so what's on it stands out.
            p.setColor(0x70000000);
            mc.drawRect(0, 0, bw, bh, p);
        }
        gv.miniSrc.set(0, 0, gv.tacMap.getWidth(), gv.tacMap.getHeight());
        c.drawBitmap(gv.tacMap, gv.miniSrc, gv.tacRect, gv.bmpPaint);
        float sx = gv.tacRect.width() / city.worldW(), sy = gv.tacRect.height() / city.worldH();
        // The dead (red) and the living (blue), counted on a grid a couple of times a second.
        gv.tacTimer -= gv.frameDt;
        if (gv.tacZ == null || gv.tacTimer <= 0) {
            gv.tacTimer = 0.5f;
            if (gv.tacZ == null) {
                gv.tacZ = new int[TAC_GRID * TAC_GRID];
                gv.tacH = new int[TAC_GRID * TAC_GRID];
            }
            java.util.Arrays.fill(gv.tacZ, 0);
            java.util.Arrays.fill(gv.tacH, 0);
            for (int i = 0, n = gv.world.entities.size(); i < n; i++) {
                Entity e = gv.world.entities.get(i);
                if (e.dead) continue;
                int gx = Math.min(TAC_GRID - 1, Math.max(0, (int) (e.x / city.worldW() * TAC_GRID)));
                int gy = Math.min(TAC_GRID - 1, Math.max(0, (int) (e.y / city.worldH() * TAC_GRID)));
                if (e.isZombie()) gv.tacZ[gy * TAC_GRID + gx]++;
                else gv.tacH[gy * TAC_GRID + gx]++;
            }
        }
        float cw = gv.tacRect.width() / TAC_GRID, ch = gv.tacRect.height() / TAC_GRID;
        for (int k = 0; k < gv.tacZ.length; k++) {
            int z = gv.tacZ[k], hu = gv.tacH[k];
            if (z == 0 && hu == 0) continue;
            float x0 = gv.tacRect.left + (k % TAC_GRID) * cw, y0 = gv.tacRect.top + (k / TAC_GRID) * ch;
            if (z > 0) gv.fill.setColor(alpha(0xFFFF3A20, Math.min(0.8f, 0.25f + z * 0.05f)));
            else gv.fill.setColor(alpha(0xFF4F8FE0, Math.min(0.55f, 0.18f + hu * 0.015f)));
            c.drawRect(x0, y0, x0 + cw, y0 + ch, gv.fill);
        }
        // Safe zones.
        for (int i = 0, n = gv.world.dispatch.zones.size(); i < n; i++) {
            Dispatch.SafeZone z = gv.world.dispatch.zones.get(i);
            gv.stroke.setColor(z.military ? 0xFF8FDF5F : 0xFF6FAFFF);
            gv.stroke.setStrokeWidth(2 * gv.dp);
            c.drawCircle(gv.tacRect.left + z.x * sx, gv.tacRect.top + z.y * sy, Math.max(5 * gv.dp, z.r * sx), gv.stroke);
        }
        // Calls: a ring at each open one.
        for (int i = 0, n = gv.world.dispatch.incidents.size(); i < n; i++) {
            Dispatch.Incident inc = gv.world.dispatch.incidents.get(i);
            if (inc.resolved || inc.cordonPost >= 0) continue;
            gv.stroke.setColor(0xFFFFC24A);
            gv.stroke.setStrokeWidth(1.6f * gv.dp);
            c.drawCircle(gv.tacRect.left + inc.x * sx, gv.tacRect.top + inc.y * sy, (5 + 2 * (float) Math.abs(Math.sin(gv.world.time * 4))) * gv.dp, gv.stroke);
        }
        // Help coming in.
        for (int i = 0; i < gv.world.dispatch.convoys.size(); i++) {
            Dispatch.Convoy cv = gv.world.dispatch.convoys.get(i);
            int col = WorldRenderer.convoyColor(cv);
            gv.stroke.setColor(col);
            gv.stroke.setStrokeWidth(2 * gv.dp);
            float hx = gv.tacRect.left + cv.x * sx, hy = gv.tacRect.top + cv.y * sy;
            c.drawLine(hx, hy, gv.tacRect.left + cv.toX * sx, gv.tacRect.top + cv.toY * sy, gv.stroke);
            gv.fill.setColor(col);
            c.drawCircle(hx, hy, 4 * gv.dp, gv.fill);
        }
        float dot = 1.6f * gv.dp;
        for (int i = 0, n = gv.world.fleet.vehicles.size(); i < n; i++) {
            Fleet.Vehicle v = gv.world.fleet.vehicles.get(i);
            if (v.type == Fleet.CAR || v.type == Fleet.TRAIN) continue;
            gv.fill.setColor(Fleet.army(v) ? 0xFFFFE27A : 0xFFE0E0E0);
            c.drawCircle(gv.tacRect.left + v.x * sx, gv.tacRect.top + v.y * sy, dot, gv.fill);
        }
        gv.text.setTextAlign(Paint.Align.CENTER);
        // (10.46) Infection zones.
        for (int i = 0; i < gv.world.infection.size(); i++) {
            World.InfectionZone z = gv.world.infection.get(i);
            gv.stroke.setColor(z.critical ? 0xFFFF4A3A : 0xFFFFB040);
            gv.stroke.setStrokeWidth(1.6f * gv.dp);
            c.drawCircle(gv.tacRect.left + z.x * sx, gv.tacRect.top + z.y * sy, Math.max(4 * gv.dp, z.r * sx), gv.stroke);
        }
        // Checkpoints.
        for (int i = 0; i < city.checkpoints.size(); i++) {
            float[] cp = city.checkpoints.get(i);
            float x = gv.tacRect.left + cp[0] * sx, y = gv.tacRect.top + cp[1] * sy;
            gv.fill.setColor(0xFF55623A);
            c.drawRect(x - 3.5f * gv.dp, y - 3.5f * gv.dp, x + 3.5f * gv.dp, y + 3.5f * gv.dp, gv.fill);
            gv.stroke.setColor(0xFFE8E2C0);
            gv.stroke.setStrokeWidth(1 * gv.dp);
            c.drawRect(x - 3.5f * gv.dp, y - 3.5f * gv.dp, x + 3.5f * gv.dp, y + 3.5f * gv.dp, gv.stroke);
        }
        // Bases, police stations, hospitals.
        gv.text.setTextSize(9 * gv.dp);
        for (int i = 0; i < city.facilities.size(); i++) {
            City.Facility f = city.facilities.get(i);
            // ((10.62) The fire stations aren't marked: there's no fire service any more. (16.5) Nor a hospital left
            // standing in an older city.)
            if (f.kind == City.FACILITY_FIRE || f.kind == City.FACILITY_HOSPITAL) continue;
            float x = gv.tacRect.left + f.x * sx, y = gv.tacRect.top + f.y * sy;
            int col = f.kind == City.FACILITY_BASE ? (f.fallen ? 0xFF8A3A30 : 0xFF6F8F3F) : f.kind == City.FACILITY_POLICE ? 0xFF3A6EC5
                    : f.kind == City.FACILITY_FIRE ? 0xFFD0402A : 0xFFE8E8E8;
            gv.fill.setColor(col);
            float r = (f.kind == City.FACILITY_BASE ? 5 : 3) * gv.dp;
            c.drawCircle(x, y, r, gv.fill);
            if (f.kind == City.FACILITY_HOSPITAL) {
                gv.fill.setColor(0xFFD03030);
                c.drawRect(x - r * 0.7f, y - r * 0.2f, x + r * 0.7f, y + r * 0.2f, gv.fill);
                c.drawRect(x - r * 0.2f, y - r * 0.7f, x + r * 0.2f, y + r * 0.7f, gv.fill);
            }
            if (f.kind == City.FACILITY_BASE) {
                String label = f.fallen ? "BASE (OVERRUN)" : "ARMY BASE";
                x = tacX(label, x);
                if (!tacClaim(label, x, y + r + 10 * gv.dp)) continue;
                gv.text.setColor(0xFF000000);
                c.drawText(label, x + 1, y + r + 11 * gv.dp + 1, gv.text);
                gv.text.setColor(f.fallen ? 0xFFFF8A7A : 0xFFD8E8B0);
                c.drawText(label, x, y + r + 11 * gv.dp, gv.text);
            }
        }
        // The towns and districts by name.
        gv.text.setTextSize(9.5f * gv.dp);
        for (int i = 0, n = city.districts.size(); i < n; i++) {
            City.District d = city.districts.get(i);
            if (!d.labelled()) continue;
            float x = tacX(d.name, gv.tacRect.left + d.cx * sx), y = gv.tacRect.top + d.cy * sy;
            if (!tacClaim(d.name, x, y)) continue;
            gv.text.setColor(0xC0000000);
            c.drawText(d.name, x + 1, y + 1, gv.text);
            gv.text.setColor(0xFFF4EED8);
            c.drawText(d.name, x, y, gv.text);
        }
        // Where the camera is.
        gv.stroke.setColor(0xFFFFFFFF);
        gv.stroke.setStrokeWidth(1.2f * gv.dp);
        c.drawRect(gv.tacRect.left + gv.camX * sx, gv.tacRect.top + gv.camY * sy, gv.tacRect.left + (gv.camX + gv.getWidth() / gv.scale) * sx,
                gv.tacRect.top + (gv.camY + gv.barTop / gv.scale) * sy, gv.stroke);
        // Title, key and close.
        gv.text.setTextSize(15 * gv.dp);
        gv.text.setColor(0xFFF2F2F2);
        // ((12.0) Short of the close button on a narrow screen.)
        float titleRoom = gv.getWidth() - 2 * 52 * gv.dp;
        String title = city.name + " - tactical map";
        if (gv.text.measureText(title) > titleRoom) title = city.name;
        c.drawText(fitText(title, titleRoom), gv.getWidth() / 2f, 30 * gv.dp, gv.text);
        gv.text.setTextSize(11 * gv.dp);
        gv.text.setColor(0xFFB8BCC4);
        int dead = 0, living = 0;
        for (int k = 0; k < gv.tacZ.length; k++) {
            dead += gv.tacZ[k];
            living += gv.tacH[k];
        }
        // ((12.0) On two lines where one won't fit: it ran off both sides of a phone held upright.)
        String key = "Dead " + dead + "   Living (outdoors) " + living, hint = "Rings: calls   Tap to go there";
        if (keySide) {
            gv.text.setTextAlign(Paint.Align.LEFT);
            float kx = gv.tacRect.right + 16 * gv.dp, ky = gv.tacRect.top + 20 * gv.dp, kw = gv.getWidth() - kx - 12 * gv.dp;
            String[] keyLines = {"Dead " + dead, "Living (outdoors) " + living, "Rings: calls", "Tap to go there"};
            for (String l : keyLines) {
                c.drawText(fitText(l, kw), kx, ky, gv.text);
                ky += 18 * gv.dp;
            }
            gv.text.setTextAlign(Paint.Align.CENTER);
        } else if (gv.text.measureText(key + "   " + hint) <= gv.getWidth() - 16 * gv.dp)
            c.drawText(key + "   " + hint, gv.getWidth() / 2f, gv.tacRect.bottom + 20 * gv.dp, gv.text);
        else {
            c.drawText(fitText(key, gv.getWidth() - 16 * gv.dp), gv.getWidth() / 2f, gv.tacRect.bottom + 20 * gv.dp, gv.text);
            c.drawText(fitText(hint, gv.getWidth() - 16 * gv.dp), gv.getWidth() / 2f, gv.tacRect.bottom + 36 * gv.dp, gv.text);
        }
        gv.tacClose.set(gv.getWidth() - 44 * gv.dp, 8 * gv.dp, gv.getWidth() - 8 * gv.dp, 40 * gv.dp);
        gv.fill.setColor(0xE0303238);
        c.drawRoundRect(gv.tacClose, 9 * gv.dp, 9 * gv.dp, gv.fill);
        gv.stroke.setColor(0xFFE0E0E0);
        gv.stroke.setStrokeWidth(2 * gv.dp);
        float mx = gv.tacClose.centerX(), my = gv.tacClose.centerY(), u = 6 * gv.dp;
        c.drawLine(mx - u, my - u, mx + u, my + u, gv.stroke);
        c.drawLine(mx - u, my + u, mx + u, my - u, gv.stroke);
    }

    /** Room for a label centred on (x, y) on the tactical map, without running into one already there. */
    /** ((12.0) A label's middle moved in so all of it is on the map: names at the edge were cut off by it.) */
    float tacX(String label, float x) {
        float half = gv.text.measureText(label) / 2 + 2 * gv.dp;
        return Math.max(gv.tacRect.left + half, Math.min(gv.tacRect.right - half, x));
    }

    boolean tacClaim(String label, float x, float y) {
        float tw = gv.text.measureText(label) / 2 + 2 * gv.dp;
        RectF r = pooled(gv.tacPool, gv.tacUsed.size());
        r.set(x - tw, y - gv.text.getTextSize(), x + tw, y + 3 * gv.dp);
        for (int i = 0; i < gv.tacUsed.size(); i++) if (RectF.intersects(gv.tacUsed.get(i), r)) return false;
        gv.tacUsed.add(r);
        return true;
    }

    void tacticalTap(float x, float y) {
        gv.click();
        if (gv.tacRect.contains(x, y)) {
            gv.follow = null;
            gv.followV = null;
            gv.camX = (x - gv.tacRect.left) / gv.tacRect.width() * gv.world.city.worldW() - gv.getWidth() / gv.scale / 2;
            gv.camY = (y - gv.tacRect.top) / gv.tacRect.height() * gv.world.city.worldH() - gv.barTop / gv.scale / 2;
            gv.clampCamera();
        }
        gv.tactical = false;
    }

    /** Where things sitting on the tool bar go down to (above its fold tab). */
    float uiFloor() {
        return gv.toolsFolded ? gv.barTop : gv.foldTab.top;
    }

    float mx(float x) {
        return gv.miniRect.left + (x - gv.miniX0) / gv.miniW * gv.miniRect.width();
    }

    float my(float y) {
        return gv.miniRect.top + (y - gv.miniY0) / gv.miniH * gv.miniRect.height();
    }

    void drawMiniDots() {
        if (gv.miniDots == null) {
            gv.miniDots = android.graphics.Bitmap.createBitmap(MINI, MINI, android.graphics.Bitmap.Config.ARGB_8888);
            gv.miniDotsCanvas = new Canvas(gv.miniDots);
        }
        Canvas mc = gv.miniDotsCanvas;
        mc.drawBitmap(gv.miniMap, 0, 0, null);
        float sx = (float) MINI / gv.world.city.worldW(), sy = (float) MINI / gv.world.city.worldH();
        // Infection heatmap: where the zombies are thickest.
        int hg = 24;
        if (gv.heat == null) gv.heat = new int[hg * hg];
        java.util.Arrays.fill(gv.heat, 0);
        for (int i = 0, n = gv.world.entities.size(); i < n; i++) {
            Entity e = gv.world.entities.get(i);
            if (!e.isZombie()) continue;
            int hx = Math.min(hg - 1, Math.max(0, (int) (e.x / gv.world.city.worldW() * hg)));
            int hy = Math.min(hg - 1, Math.max(0, (int) (e.y / gv.world.city.worldH() * hg)));
            gv.heat[hy * hg + hx]++;
        }
        float cw = (float) MINI / hg;
        for (int k = 0; k < gv.heat.length; k++) {
            if (gv.heat[k] == 0) continue;
            gv.fill.setColor(alpha(0xFFFF3A20, Math.min(0.65f, 0.15f + gv.heat[k] * 0.04f)));
            float hx = (k % hg) * cw, hy = (k / hg) * cw;
            mc.drawRect(hx, hy, hx + cw, hy + cw, gv.fill);
        }
        // ((11.5) One dot per kind on each pixel of the minimap: a horde of thousands is a few hundred dots.)
        if (gv.miniSeen == null) gv.miniSeen = new int[MINI * MINI];
        gv.miniStamp += 8;
        if (gv.miniStamp > Integer.MAX_VALUE - 16) {
            gv.miniStamp = 8;
            java.util.Arrays.fill(gv.miniSeen, 0);
        }
        for (int i = 0, n = gv.world.entities.size(); i < n; i++) {
            Entity e = gv.world.entities.get(i);
            int col, kind;
            // (Just the police, the army and the dead: the crowd of everyone else hid them.)
            float d = 1.4f;
            if (e.dead) continue;
            if (e.isZombie()) {
                col = 0xFF7CE04A;
                kind = 1;
            } else if (e.type == Entity.COP || e.type == Entity.SOLDIER) {
                col = e.type == Entity.SOLDIER ? 0xFFB8E07A : 0xFF6FA8FF;
                kind = e.type == Entity.SOLDIER ? 3 : 2;
                d = 1.8f;
            } else continue;
            float x = e.x * sx, y = e.y * sy;
            int px = Math.max(0, Math.min(MINI - 1, (int) x)), py = Math.max(0, Math.min(MINI - 1, (int) y)), k = py * MINI + px;
            if (gv.miniSeen[k] == gv.miniStamp + kind) continue;
            gv.miniSeen[k] = gv.miniStamp + kind;
            gv.fill.setColor(col);
            mc.drawRect(x - d, y - d, x + d, y + d, gv.fill);
        }
        for (int i = 0, n = gv.world.fleet.vehicles.size(); i < n; i++) {
            Fleet.Vehicle v = gv.world.fleet.vehicles.get(i);
            if (v.type == Fleet.CAR && v.parked) continue;
            if (v.type == Fleet.HELI || v.type == Fleet.JET) continue;
            int kind = v.type == Fleet.TANK ? 4 : v.type == Fleet.CRUISER || Fleet.policeTruck(v) ? 5 : v.type == Fleet.TRUCK ? 6 : 7;
            int col = kind == 4 ? 0xFFFFE27A : kind == 5 ? 0xFF6FA8FF : kind == 6 ? 0xFFB8E07A : 0xB0E0E0E0;
            float x = v.x * sx, y = v.y * sy, d = kind == 7 ? 1.1f : 1.6f;
            int px = Math.max(0, Math.min(MINI - 1, (int) x)), py = Math.max(0, Math.min(MINI - 1, (int) y)), k = py * MINI + px;
            if (gv.miniSeen[k] == gv.miniStamp + kind) continue;
            gv.miniSeen[k] = gv.miniStamp + kind;
            gv.fill.setColor(col);
            mc.drawRect(x - d, y - d, x + d, y + d, gv.fill);
        }
    }

    void caption(int type, float dx, float dy) {
        int k = Sfx.gun(type) ? 0 : type == Sfx.EXPLOSION || type == Sfx.CANNON || type == Sfx.LAUNCHER ? 1 : type == Sfx.SIREN || type == Sfx.AMB_SIREN ? 2
                : type == Sfx.SCREAM || type == Sfx.PANIC ? 3 : type == Sfx.ALARM ? 4 : type == Sfx.ROTOR ? 5 : type == Sfx.CRASH ? 6
                : type == Sfx.COLLAPSE || type == Sfx.DEBRIS ? 7 : type == Sfx.GROAN_DEEP ? 8 : -1;
        if (k < 0) return;
        float d = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
        for (float[] c : gv.captions)
            if ((int) c[0] == k && (c[1] * dx + c[2] * dy) / d > 0.7f) {
                c[3] = 2.5f;
                return;
            }
        if (gv.captions.size() >= 5) gv.captions.remove(0);
        gv.captions.add(new float[]{k, dx / d, dy / d, 2.5f});
    }

    void drawCaptions(Canvas c, float dt) {
        if (gv.captions.isEmpty()) return;
        float w = gv.getWidth(), h = gv.getHeight(), top = gv.topRects[0].bottom + 60 * gv.dp, bottom = gv.barTop - 70 * gv.dp;
        gv.text.setTextSize(13 * gv.dp);
        gv.text.setTextAlign(Paint.Align.CENTER);
        for (int i = gv.captions.size() - 1; i >= 0; i--) {
            float[] cp = gv.captions.get(i);
            cp[3] -= dt;
            if (cp[3] <= 0) {
                gv.captions.remove(i);
                continue;
            }
            float ax = cp[1], ay = cp[2];
            // Towards the edge of the screen on its side.
            float sx = w / 2 + ax * (w / 2 - 90 * gv.dp), sy = (top + bottom) / 2 + ay * ((bottom - top) / 2 - 20 * gv.dp);
            String arrow = Math.abs(ax) > Math.abs(ay) ? (ax > 0 ? "→" : "←") : (ay > 0 ? "↓" : "↑");
            String s = (ax < -0.5f || ay < -0.5f ? arrow + " " : "") + CAPTION_TEXT[(int) cp[0]] + (ax >= -0.5f && ay >= -0.5f ? " " + arrow : "");
            float tw = gv.text.measureText(s) + 20 * gv.dp;
            int alpha = (int) (Math.min(1, cp[3]) * 220);
            gv.fill.setColor((alpha << 24) | 0x101216);
            gv.oval.set(sx - tw / 2, sy - 16 * gv.dp, sx + tw / 2, sy + 10 * gv.dp);
            c.drawRoundRect(gv.oval, 8 * gv.dp, 8 * gv.dp, gv.fill);
            gv.text.setColor((Math.min(255, alpha + 35) << 24) | 0xFFE6A0);
            c.drawText(s, sx, sy + 2 * gv.dp, gv.text);
        }
    }

    /** Takes in new radio messages and lets them onto the screen one at a time, a few seconds apart. */
    void paceFeed(float dt, int maxLines) {
        Dispatch d = gv.world.dispatch;
        if (d.messageCount < gv.feedSeen) gv.feedSeen = d.messageCount;
        int fresh = Math.min(d.messageCount - gv.feedSeen, d.log.size());
        for (int i = d.log.size() - fresh; i < d.log.size(); i++) gv.feedQueue.add(d.log.get(i));
        gv.feedSeen = d.messageCount;
        // A backlog: what's old news by now is dropped (calls for help are kept over chatter).
        while (gv.feedQueue.size() > 4) {
            int drop = 0;
            for (int i = 0; i < gv.feedQueue.size() - 1; i++)
                if (!gv.feedQueue.get(i).who.equals("911")) {
                    drop = i;
                    break;
                }
            gv.feedQueue.remove(drop);
        }
        for (int i = gv.feedOn.size() - 1; i >= 0; i--) {
            float age = gv.feedOnAge.get(i) + dt;
            if (age > FEED_LIFE) {
                gv.feedOn.remove(i);
                gv.feedOnAge.remove(i);
            } else gv.feedOnAge.set(i, age);
        }
        // (Fewer lines chosen in Settings: down to that many now.)
        while (gv.feedOn.size() > maxLines) {
            gv.feedOn.remove(0);
            gv.feedOnAge.remove(0);
        }
        gv.feedGap -= dt;
        if (!gv.feedQueue.isEmpty() && gv.feedGap <= 0) {
            // Full: the oldest line makes way (but only once it's been up a while).
            if (gv.feedOn.size() >= maxLines && gv.feedOnAge.get(0) < 6) return;
            if (gv.feedOn.size() >= maxLines) {
                gv.feedOn.remove(0);
                gv.feedOnAge.remove(0);
            }
            gv.feedOn.add(gv.feedQueue.remove(0));
            gv.feedOnAge.add(0f);
            gv.feedGap = FEED_EVERY;
        }
    }

    /** The last few radio messages. Tapping one moves the camera to where it happened. */
    void drawFeed(Canvas c) {
        gv.feedCount = 0;
        gv.feedBottom = 0;
        if (!gv.settings.radio()) return;
        // ((12.0) As many as the setting says.)
        int maxLines = gv.settings.feedLines();
        paceFeed(gv.frameDt, maxLines);
        int shown = gv.feedOn.size();
        float lineH = 22 * gv.dp, gap = 3 * gv.dp, y = gv.feedRect.top;
        // (10.50) With the stats folded to one line, the messages take the space the panel leaves: just under
        // that line, from the left.
        boolean underStats = gv.statsCollapsed && !gv.statsShown.isEmpty();
        float left = underStats ? gv.statsRect.left : gv.feedRect.left;
        if (underStats) y = gv.statsShown.bottom + 8 * gv.dp;
        // In portrait the war meter sits where the messages start: they go below it.
        // ((12.0) Under the folded stats in landscape too: it was drawn on top of the meter there.)
        if ((gv.portrait || underStats) && gv.meterBottom > 0) y = Math.max(y, gv.meterBottom + 8 * gv.dp);
        // Never down over the Auto cam switch (or the minimap) on the right.
        float limit = gv.portrait ? gv.barTop : (gv.camChip.isEmpty() ? gv.barTop : gv.camChip.top) - 6 * gv.dp;
        while (shown > 0 && y + shown * (lineH + gap) > limit) shown--;
        gv.feedBottom = y;
        gv.text.setTextSize(11.5f * gv.dp);
        for (int k = 0; k < shown; k++) {
            int idx = gv.feedOn.size() - shown + k;
            Dispatch.Message m = gv.feedOn.get(idx);
            float age = gv.feedOnAge.get(idx);
            float a = Math.min(Math.min(1, age / 0.3f), (FEED_LIFE - age) / 1.5f);
            String who = m.who + ": ";
            gv.text.setTextAlign(Paint.Align.LEFT);
            float whoW = gv.text.measureText(who);
            // (Never under the minimap.)
            float rightLimit = gv.feedRect.right;
            if ((gv.portrait || underStats) && !gv.miniRect.isEmpty() && y < gv.miniRect.bottom && y + lineH > gv.miniRect.top && gv.miniRect.left > left)
                rightLimit = Math.min(rightLimit, gv.miniRect.left - 6 * gv.dp);
            float maxText = rightLimit - left - whoW - 20 * gv.dp;
            // ((12.0) Cut to fit once, not a letter at a time again every frame it's up.)
            String body = gv.feedFit.get(m);
            if (body == null || gv.feedFitW.get(m) != maxText) {
                body = m.text;
                if (gv.text.measureText(body) > maxText) {
                    // (A first guess from the widths, then letter by letter from there.)
                    int guess = Math.max(4, (int) (body.length() * maxText / gv.text.measureText(body)));
                    if (guess < body.length()) body = body.substring(0, guess);
                    while (body.length() > 4 && gv.text.measureText(body + "...") > maxText) body = body.substring(0, body.length() - 1);
                    body = body + "...";
                }
                gv.feedFit.put(m, body);
                gv.feedFitW.put(m, maxText);
            }
            float lw = whoW + gv.text.measureText(body) + 16 * gv.dp;
            RectF r = gv.feedLines[gv.feedCount];
            r.set(gv.feedRect.right - lw, y, gv.feedRect.right, y + lineH);
            if (gv.portrait || underStats) r.offsetTo(left, y);
            gv.fill.setColor(alpha(0xC8000000, a));
            c.drawRoundRect(r, 7 * gv.dp, 7 * gv.dp, gv.fill);
            gv.text.setColor(alpha(m.color, a));
            c.drawText(who, r.left + 8 * gv.dp, y + lineH * 0.68f, gv.text);
            gv.text.setColor(alpha(0xFFFFFFFF, a));
            c.drawText(body, r.left + 8 * gv.dp + whoW, y + lineH * 0.68f, gv.text);
            gv.feedMsgs[gv.feedCount++] = m;
            y += lineH + gap;
            gv.feedBottom = y;
        }
    }

    int fade(int color) {
        return gv.wallFade >= 1 ? color : alpha(color, gv.wallFade);
    }

    /** The text, cut short with "..." to fit the width (in the current text size). */
    /** ((12.0) Each of the lines on one line or two, as {@link #wrap2} has it, at the text size set now.) */
    java.util.ArrayList<String> wrapAll(java.util.List<String> lines, float width) {
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        for (String l : lines) out.addAll(wrap2(l, width));
        return out;
    }

    /** ((12.0) s on one line if it fits in width, else on two, broken between words (the second cut to fit if it must). */
    java.util.List<String> wrap2(String s, float width) {
        if (gv.text.measureText(s) <= width) return java.util.Collections.singletonList(s);
        int cut = s.length();
        while (true) {
            int sp = s.lastIndexOf(' ', cut - 1);
            if (sp <= 0) return java.util.Collections.singletonList(s);
            cut = sp;
            if (gv.text.measureText(s.substring(0, cut)) <= width) break;
        }
        return java.util.Arrays.asList(s.substring(0, cut), s.substring(cut + 1).trim());
    }

    /** ((12.0) s at the text size set now, or a little smaller (down to 80%) if that's what it takes, then cut to fit.) */
    String shrinkToFit(String s, float width) {
        float size = gv.text.getTextSize(), w = gv.text.measureText(s);
        if (w > width) gv.text.setTextSize(Math.max(size * 0.8f, size * width / w));
        return fitText(s, width);
    }

    /** ((22.6) A line broken into lines that fit the width (in the current text size), at spaces where it can. */
    java.util.List<String> wrapLine(String s, float width) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        while (gv.text.measureText(s) > width && s.length() > 1) {
            int cut = s.length();
            while (cut > 1 && gv.text.measureText(s.substring(0, cut)) > width) cut--;
            int sp = s.lastIndexOf(' ', cut);
            if (sp > cut / 3) cut = sp;
            out.add(s.substring(0, cut).trim());
            s = s.substring(cut).trim();
        }
        if (s.length() > 0) out.add(s);
        return out;
    }

    String fitText(String s, float width) {
        if (gv.text.measureText(s) <= width) return s;
        while (s.length() > 4 && gv.text.measureText(s + "...") > width) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    void drawWarMeter(Canvas c) {
        gv.meterBottom = 0;
        gv.meterRect.setEmpty();
        if (gv.world.outbreak || gv.world.warBannerTime > 0) {
            // ((21.3) A set width, not that of the line of figures above it: as that line grew (the director's phase,
            // the clock), the meter grew with it, most of the way across the screen.)
            float left = gv.statsShown.left, right = left + 210 * gv.dp, top = gv.statsShown.bottom + 8 * gv.dp;
            if (!gv.portrait) right = Math.min(right, Math.max(gv.statsRect.right, gv.topRects[0].left - 8 * gv.dp));
            float bh = 10 * gv.dp, b = Math.max(0.03f, Math.min(0.97f, gv.world.warBalance));
            // An air strike on the way gets a red line of its own inside the panel.
            boolean strike = gv.world.firebombTime > 0 && gv.world.firebombPlace != null;
            // (Where there's no room under the meter, the warning sits beside it.)
            boolean inside = strike && top + bh + 38 * gv.dp < gv.barTop - 6 * gv.dp;
            gv.oval.set(left, top, right, top + bh + 18 * gv.dp + (inside ? 20 * gv.dp : 0));
            gv.meterRect.set(gv.oval);
            gv.meterBottom = gv.oval.bottom;
            gv.fill.setColor(0xB0101114);
            c.drawRoundRect(gv.oval, 8 * gv.dp, 8 * gv.dp, gv.fill);
            if (strike) {
                gv.text.setTextSize(11 * gv.dp);
                gv.text.setTextAlign(Paint.Align.LEFT);
                String msg = "AIR STRIKE: " + gv.world.firebombPlace + " in " + (int) Math.ceil(gv.world.firebombTime) + "s";
                float tx = left + 10 * gv.dp, ty = gv.oval.bottom - 7 * gv.dp;
                if (!inside) {
                    float tw = Math.min(gv.text.measureText(msg), 260 * gv.dp) + 20 * gv.dp;
                    gv.oval.set(right + 8 * gv.dp, top, right + 8 * gv.dp + tw, top + 24 * gv.dp);
                    gv.fill.setColor(0xD0601010);
                    c.drawRoundRect(gv.oval, 8 * gv.dp, 8 * gv.dp, gv.fill);
                    gv.meterRect.set(gv.meterRect.left, gv.meterRect.top, gv.oval.right, Math.max(gv.meterRect.bottom, gv.oval.bottom));
                    tx = gv.oval.left + 10 * gv.dp;
                    ty = gv.oval.bottom - 8 * gv.dp;
                }
                gv.text.setColor(((int) (gv.world.time * 3)) % 2 == 0 ? 0xFFFF6A5A : 0xFFFFB0A0);
                c.drawText(fitText(msg, inside ? right - left - 20 * gv.dp : 260 * gv.dp), tx, ty, gv.text);
            }
            float bx0 = left + 10 * gv.dp, bx1 = right - 10 * gv.dp, by = top + 15 * gv.dp, split = bx0 + (bx1 - bx0) * b;
            gv.fill.setColor(0xFF4F7BE0);
            c.drawRect(bx0, by, split, by + bh * 0.6f, gv.fill);
            gv.fill.setColor(0xFF6FB03A);
            c.drawRect(split, by, bx1, by + bh * 0.6f, gv.fill);
            gv.fill.setColor(0xFFFFFFFF);
            c.drawRect(split - 1.5f * gv.dp, by - 3 * gv.dp, split + 1.5f * gv.dp, by + bh * 0.6f + 3 * gv.dp, gv.fill);
            gv.text.setTextSize(10 * gv.dp);
            gv.text.setTextAlign(Paint.Align.LEFT);
            gv.text.setColor(0xFFB8C8F0);
            // ((17.3) The two always add up to 100, and say it as it is: not held at 97 and 3.)
            int pp = Math.round(Math.max(0, Math.min(1, gv.world.warBalance)) * 100);
            c.drawText("PEOPLE " + pp + "%", bx0, top + 11 * gv.dp, gv.text);
            gv.text.setTextAlign(Paint.Align.RIGHT);
            gv.text.setColor(0xFFB8E09A);
            c.drawText((100 - pp) + "% " + "ZOMBIES", bx1, top + 11 * gv.dp, gv.text);
        }
    }

    void drawUi(Canvas c) {
        int w = gv.getWidth(), h = gv.getHeight();

        // Once the outbreak starts the panel shrinks to one line (tap for the rest), to leave the map clear.
        if (gv.world.outbreak && !gv.autoCollapsed) {
            gv.autoCollapsed = true;
            gv.statsCollapsed = true;
        }
        // Stats panel: one column in landscape, two in portrait. Tap it to collapse to one line.
        float lh = 17 * gv.dp;
        Dispatch d = gv.world.dispatch;
        int[] rows = {gv.world.civilians, gv.world.counts[Entity.COP], gv.world.counts[Entity.SOLDIER], gv.world.zombieCount()};
        gv.text.setTextSize(12.5f * gv.dp);
        drawStatsPanel(c, lh, d, rows);
        // Top buttons.
        String[] top = {"", SPEEDS[gv.speedIdx] + "x", "Brush " + BRUSHES[gv.brushIdx], "Clear", "Menu", "Kills"};
        gv.text.setTextSize((gv.portrait ? 11 : 12) * gv.dp);
        gv.text.setTextAlign(Paint.Align.CENTER);
        drawTopButtons(c, top);

        // Bottom tool bar (or, folded away, a strip to bring it back).
        gv.fill.setColor(0xD80E0F12);
        c.drawRect(0, gv.barTop, w, h, gv.fill);
        gv.text.setTextAlign(Paint.Align.CENTER);
        drawFoldedToolBar(c, w);
        drawToolButtons(c);
        // ((12.0) Clear of the tab that folds the tool bar away: cards and lines here used to sit over it.)
        float infoY = gv.barTop - 24 * gv.dp;
        gv.worldRenderer.drawConvoys(c);
        gv.mapLabels.drawPins(c);
        gv.mapLabels.drawLastDead(c);
        gv.mapLabels.drawIncoming(c);
        gv.text.setTextSize(13 * gv.dp);
        drawToolHint(c, w, infoY);
    }

    /** ((27.3) From drawUi.) The stats panel: one line while collapsed; opened, the counts, the outbreak's figures, the live graph, the top units and the fallen. */
    void drawStatsPanel(Canvas c, float lh, Dispatch d, int[] rows) {
        if (gv.statsCollapsed) {
            int at = (int) gv.world.afterTime;
            String phase = gv.world.aftermath == World.AFTER_RECOVERY ? "   Recovery " + World.clock(at)
                    : gv.world.aftermath == World.AFTER_FALLEN ? "   City fallen"
                    // ((21.0) How hard the dead are pressing, as the director sees it; or that it's down to the last of them.)
                    : gv.world.mopping ? "   Mopping up" : gv.world.outbreak && gv.world.director.phase != Director.CALM ? "   " + Director.PHASES[gv.world.director.phase] : "";
            int secs0 = (int) gv.world.time;
            String line = "People " + (gv.world.humanCount() + gv.world.hiding + gv.world.riding + gv.world.visiting) + "   Zombies " + gv.world.zombieCount()
                    + phase + "   " + World.clock(secs0);
            // (Short of the buttons along the top in landscape.)
            float room = (gv.portrait ? gv.getWidth() - 10 * gv.dp : gv.topRects[0].left - 8 * gv.dp) - gv.statsRect.left - 24 * gv.dp;
            String clock = "   " + World.clock(secs0);
            if (gv.text.measureText(line) > room) line = line.substring(0, line.length() - clock.length());
            if (gv.text.measureText(line) > room && !phase.isEmpty()) line = line.substring(0, line.length() - phase.length());
            float tw = Math.min(gv.text.measureText(line), room);
            gv.oval.set(gv.statsRect.left, gv.statsRect.top, gv.statsRect.left + tw + 24 * gv.dp, gv.statsRect.top + 28 * gv.dp);
            gv.statsShown.set(gv.oval);
            gv.fill.setColor(0xB0101114);
            c.drawRoundRect(gv.oval, 10 * gv.dp, 10 * gv.dp, gv.fill);
            gv.text.setTextAlign(Paint.Align.LEFT);
            gv.text.setColor(0xFFE6E6E6);
            c.drawText(fitText(line, room), gv.statsRect.left + 12 * gv.dp, gv.statsRect.top + 19 * gv.dp, gv.text);
        } else {
            gv.statsShown.set(gv.statsRect);
            gv.fill.setColor(0xB0101114);
            c.drawRoundRect(gv.statsRect, 10 * gv.dp, 10 * gv.dp, gv.fill);
            int perCol = gv.portrait ? 2 : rows.length;
            float colW = gv.portrait ? gv.statsRect.width() / 2 : gv.statsRect.width();
            // ((12.0) Every row at the one size the longest fits at: each shrank on its own, "Civilians 14921" small
            // beside a large "Cops 414".)
            float rowScale = 1;
            for (int t = 0; t < rows.length; t++) {
                float need = gv.text.measureText(gv.rowLabel(t)) + gv.text.measureText(String.valueOf(rows[t]));
                rowScale = Math.min(rowScale, (colW - 46 * gv.dp) / need);
            }
            for (int t = 0; t < rows.length; t++) {
                float x0 = gv.statsRect.left + (t / perCol) * colW;
                float ty = gv.statsRect.top + 6 * gv.dp + lh * 0.8f + (t % perCol) * lh;
                gv.fill.setColor(ROW_COLORS[t]);
                c.drawCircle(x0 + 14 * gv.dp, ty - 4.5f * gv.dp, 4.5f * gv.dp, gv.fill);
                gv.text.setColor(0xFFE6E6E6);
                gv.text.setTextAlign(Paint.Align.LEFT);
                // Label and number never run into each other: shrink the row if the column is narrow.
                String num = String.valueOf(rows[t]);
                // ((15.9) And those called in from outside and on their way.)
                int coming = t == 1 ? d.incoming(Entity.COP) : t == 2 ? d.incoming(Entity.SOLDIER) : 0;
                if (coming > 0) num += "  +" + coming;
                float size = gv.text.getTextSize();
                if (rowScale < 1) gv.text.setTextSize(size * rowScale);
                c.drawText(gv.rowLabel(t), x0 + 26 * gv.dp, ty, gv.text);
                gv.text.setTextAlign(Paint.Align.RIGHT);
                c.drawText(num, x0 + colW - 12 * gv.dp, ty, gv.text);
                gv.text.setTextSize(size);
            }
            gv.text.setTextAlign(Paint.Align.LEFT);
            float pad = gv.statsRect.left;
            float y = gv.statsRect.top + 6 * gv.dp + lh * 0.8f + perCol * lh;
            gv.text.setColor(0xFFA0A4AA);
            gv.text.setTextSize(11.5f * gv.dp);
            // ((12.0) And each line cut to the panel: on a narrow screen with big text they ran off its edge.)
            float lineRoom = gv.statsRect.width() - 24 * gv.dp;
            c.drawText(shrinkToFit("Turned " + gv.world.turned + "   Killed " + gv.world.zombiesKilled
                    + (gv.world.recruits + gv.world.refused > 0 ? "   Recruits " + gv.world.recruits : "")
                    + (gv.world.volunteersPolice + gv.world.volunteersArmy > 0 ? "   Volunteers " + (gv.world.volunteersPolice + gv.world.volunteersArmy) : ""), lineRoom), pad + 12 * gv.dp, y, gv.text);
            gv.text.setTextSize(11.5f * gv.dp);
            y += lh;
            // ((26.1) Only what's in use: no Hiding line (nobody hides out of sight any more), people aboard only when there are some.)
            c.drawText(shrinkToFit("911 calls " + d.calls + "   Safe zones " + d.zones.size() + "   Sheltered " + d.sheltered
                    + (gv.world.riding > 0 ? "   In vehicles " + gv.world.riding : ""), lineRoom), pad + 12 * gv.dp, y, gv.text);
            gv.text.setTextSize(11.5f * gv.dp);
            y += lh;
            // ((12.0) Cut to the panel: on a narrow screen it ran off the edge.)
            c.drawText(shrinkToFit("Outside help: " + d.policeReserve + " police, " + (d.squadReserve + d.airReserve) + " army"
                    + (d.armyCalled && !d.armyArrived ? " (convoy in " + Math.max(0, (int) d.armyEta) + "s)" : "")
                    + (d.pressure > 0 ? "   " + PRESSURE[d.pressure] : ""), gv.statsRect.width() - 24 * gv.dp),
                    pad + 12 * gv.dp, y, gv.text);
            gv.text.setTextSize(11.5f * gv.dp);
            y += lh;
            int secs = (int) gv.world.time;
            c.drawText("Time " + World.clock(secs), pad + 12 * gv.dp, y, gv.text);
            // ((10.59) The war so far: the living, the dead and those with guns, over the whole game.)
            float gBottom = gv.statsRect.bottom - 8 * gv.dp - lh * 5;
            drawLiveGraph(c, gv.statsRect.left + 12 * gv.dp, y + 8 * gv.dp, gv.statsRect.right - 12 * gv.dp, gBottom);
            // ((23.0) The best of them so far, and those who fell.)
            y = gBottom + lh;
            gv.text.setColor(0xFFE6E6E6);
            gv.text.setTextSize(11.5f * gv.dp);
            c.drawText("Top units", pad + 12 * gv.dp, y, gv.text);
            gv.text.setColor(0xFFA0A4AA);
            Entity[] top3 = gv.world.topUnits();
            for (int k = 0; k < 3; k++) {
                y += lh;
                Entity tu = top3[k];
                String ln = tu == null ? "-" : (k + 1) + ". " + (Names.rank(tu) != null ? Names.rank(tu) + " " : "") + Names.person(tu.nameSeed) + "  L" + (tu.level + 1) + "  " + tu.kills + " kills"
                        + (tu.saved > 0 ? ", " + tu.saved + " saved" : "");
                c.drawText(shrinkToFit(ln, lineRoom), pad + 12 * gv.dp, y, gv.text);
                gv.text.setTextSize(11.5f * gv.dp);
            }
            y += lh;
            int bestK = 0, bestSeed = 0;
            for (int[] fh : gv.world.fallenHeroes) if (fh[3] > bestK) {
                bestK = fh[3];
                bestSeed = fh[0];
            }
            c.drawText(shrinkToFit("Fallen heroes " + gv.world.fallenHeroes.size() + (bestK > 0 ? "   bravest: " + Names.person(bestSeed) + ", " + bestK + " kills" : ""), lineRoom),
                    pad + 12 * gv.dp, y, gv.text);
            gv.text.setTextSize(11.5f * gv.dp);
        }

        // The minimap and war meter go first so the radio feed can stack round them, then the feed so
        // map labels can keep out of its way.
        drawMinimap(c);
        drawWarMeter(c);
        drawFeed(c);
        gv.mapLabels.drawMapLabels(c);
        drawPopups(c);
    }

    /** ((27.3) From drawUi.) The buttons along the top: pause, speed, brush, clear, menu, kills. */
    void drawTopButtons(Canvas c, String[] top) {
        for (int i = 0; i < TOP_BUTTONS; i++) {
            RectF r = gv.topRects[i];
            boolean hot = i == BTN_PAUSE && gv.simPaused;
            gv.fill.setColor(hot ? 0xE0D9534F : 0xC0202227);
            c.drawRoundRect(r, 9 * gv.dp, 9 * gv.dp, gv.fill);
            gv.stroke.setColor(0x40FFFFFF);
            gv.stroke.setStrokeWidth(1 * gv.dp);
            c.drawRoundRect(r, 9 * gv.dp, 9 * gv.dp, gv.stroke);
            gv.text.setColor(0xFFF2F2F2);
            float base = gv.text.getTextSize(), fit = gv.text.measureText(top[i]);
            if (fit > r.width() - 8 * gv.dp) gv.text.setTextSize(base * (r.width() - 8 * gv.dp) / fit);
            if (i == BTN_PAUSE) {
                // Pause bars, or a play triangle.
                gv.fill.setColor(0xFFF2F2F2);
                float cx = r.centerX(), cy = r.centerY(), u = 6 * gv.dp;
                if (gv.simPaused) {
                    android.graphics.Path tri = new android.graphics.Path();
                    tri.moveTo(cx - u * 0.7f, cy - u);
                    tri.lineTo(cx + u, cy);
                    tri.lineTo(cx - u * 0.7f, cy + u);
                    tri.close();
                    c.drawPath(tri, gv.fill);
                } else {
                    c.drawRect(cx - u * 0.8f, cy - u, cx - u * 0.25f, cy + u, gv.fill);
                    c.drawRect(cx + u * 0.25f, cy - u, cx + u * 0.8f, cy + u, gv.fill);
                }
            } else c.drawText(top[i], r.centerX(), r.centerY() + 4.5f * gv.dp, gv.text);
            gv.text.setTextSize(base);
        }
    }

    /** ((27.3) From drawUi.) The tool bar folded away: a strip with the tool in hand, to bring it back. */
    void drawFoldedToolBar(Canvas c, int w) {
        if (gv.toolsFolded) {
            gv.text.setTextSize(12 * gv.dp);
            gv.text.setColor(0xFFC8CCD2);
            String name = gv.tool == TOOL_ZOMBIE ? gv.enemyNames()[gv.zombieVariant] : gv.tool == TOOL_CIV ? Entity.NAMES[CIV_VARIANTS[gv.civVariant]]
                    : gv.tool == TOOL_MIL ? MIL_NAMES[gv.milVariant] : gv.tool == TOOL_COP ? gv.copNames()[gv.copVariant]
                    : gv.tool == TOOL_SQUAD ? SQUAD_NAMES[gv.squadVariant] : TOOL_NAMES[gv.tool];
            c.drawText("\u25B2  Tools  -  " + name, w / 2f, gv.barTop + 19 * gv.dp, gv.text);
        } else {
            gv.fill.setColor(0xD80E0F12);
            c.drawRoundRect(gv.foldTab, 8 * gv.dp, 8 * gv.dp, gv.fill);
            gv.stroke.setColor(0xFFC8CCD2);
            gv.stroke.setStrokeWidth(2 * gv.dp);
            float fx = gv.foldTab.centerX(), fy = gv.foldTab.centerY() + 1 * gv.dp;
            c.drawLine(fx - 6 * gv.dp, fy - 3 * gv.dp, fx, fy + 3 * gv.dp, gv.stroke);
            c.drawLine(fx, fy + 3 * gv.dp, fx + 6 * gv.dp, fy - 3 * gv.dp, gv.stroke);
        }
        gv.text.setTextSize(11.5f * gv.dp);
    }

    /** ((27.3) From drawUi.) The tool bar's buttons, the one in hand lit. */
    void drawToolButtons(Canvas c) {
        for (int i = 0; i < gv.toolRects.length; i++) {
            RectF r = gv.toolRects[i];
            if (r.isEmpty()) continue;
            boolean sel = i == gv.tool;
            gv.fill.setColor(sel ? 0xFF3A6EA5 : 0xFF23262C);
            c.drawRoundRect(r, 10 * gv.dp, 10 * gv.dp, gv.fill);
            if (sel) {
                gv.stroke.setColor(0xFFA8D0FF);
                gv.stroke.setStrokeWidth(2 * gv.dp);
                c.drawRoundRect(r, 10 * gv.dp, 10 * gv.dp, gv.stroke);
            }
            float cx = r.centerX(), cy = r.top + r.height() * 0.4f;
            gv.toolBar.drawToolIcon(c, i, cx, cy, r.height() * 0.26f);
            gv.text.setColor(sel ? 0xFFFFFFFF : 0xFFC8CCD2);
            String name = i == TOOL_ZOMBIE ? gv.enemyNames()[gv.zombieVariant]
                    : i == TOOL_CIV ? Entity.NAMES[CIV_VARIANTS[gv.civVariant]]
                    : i == TOOL_MIL ? MIL_NAMES[gv.milVariant]
                    : i == TOOL_COP ? gv.copNames()[gv.copVariant]
                    : i == TOOL_PLACE ? PLACE_NAMES[gv.placeVariant] : i == TOOL_EVENT ? gv.eventLabel()
                    : i == TOOL_BUILD ? BUILD_NAMES[gv.buildVariant] : i == TOOL_SQUAD ? SQUAD_NAMES[gv.squadVariant] : TOOL_NAMES[i];
            float fit = gv.text.measureText(name);
            if (fit > r.width() - 6 * gv.dp) gv.text.setTextSize(11.5f * gv.dp * (r.width() - 6 * gv.dp) / fit);
            c.drawText(name, cx, r.bottom - 7 * gv.dp, gv.text);
            gv.text.setTextSize(11.5f * gv.dp);
            if (ToolBar.hasPicker(i)) {
                // A small arrow: this button opens a picker.
                gv.fill.setColor(sel ? 0xFFFFFFFF : 0xFF8A9099);
                float ax = r.right - 9 * gv.dp, ay = r.top + 8 * gv.dp;
                c.drawCircle(ax, ay, 2 * gv.dp, gv.fill);
            }
        }
        if (gv.picker >= 0) gv.toolBar.drawPicker(c);

        drawHeadlines(c);
        if (gv.settings.captions()) drawCaptions(c, gv.frameDt);
    }

    /** ((27.3) From drawUi.) The line over the tool bar saying what the tool in hand will do. */
    void drawToolHint(Canvas c, int w, float infoY) {
        if (gv.tool == TOOL_ORDER && gv.follow == null) {
            Entity lead = gv.selection.isEmpty() ? null : gv.selection.get(0);
            drawBanner(c, lead == null ? "Orders: tap a cop or soldier to select them."
                    : "Selected " + Dispatch.name(lead) + (gv.selection.size() > 1 ? " and " + (gv.selection.size() - 1) + " more" : "")
                    + ". Tap where to send them, or tap them again to release.", w / 2f, infoY);
        } else if (gv.world.controlled != null) {
            drawControls(c, gv.world.controlled);
        } else if (gv.follow != null && gv.picker < 0 && !gv.clearMenu) {
            gv.inspector.drawInspect(c, gv.follow);
        } else if (gv.followV != null && gv.picker < 0 && !gv.clearMenu) {
            gv.inspector.drawVehicleCard(c, gv.followV);
        } else if (gv.inspectZ != null && gv.tool == TOOL_PAN && !gv.clearMenu) {
            // ((12.0) Cards make way for the Clear menu: they were drawn over it.)
            if (gv.inspectZ.removed) gv.inspectZ = null;
            else gv.inspector.drawZoneInspect(c, gv.inspectZ);
        } else if (gv.inspectBase != null && gv.tool == TOOL_PAN && !gv.clearMenu) {
            if (gv.inspectBase.fallen) gv.inspectBase = null;
            else gv.inspector.drawBaseInspect(c, gv.inspectBase);
        } else if (gv.inspectB != null && gv.tool == TOOL_PAN && !gv.clearMenu) {
            gv.inspector.drawBuildingInspect(c, gv.inspectB);
        } else if (gv.hintTime > 0 && gv.picker < 0 && gv.headlines.isEmpty()) {
            drawBanner(c, "Pick a unit or tool below, then tap the city.  Use Move to drag around, pinch to zoom.",
                    w / 2f, infoY);
        } else if (gv.simPaused) {
            drawBanner(c, "PAUSED", w / 2f, infoY);
        }
    }

    /**
     * The headline band (one announcement at a time, across the whole screen) and, under it, the short
     * line about what you just did. In portrait they go below the radio feed; in landscape across the middle.
     */
    void drawHeadlines(Canvas c) {
        int w = gv.getWidth();
        float top = gv.portrait ? Math.max(gv.feedBottom, Math.max(gv.meterBottom, gv.statsShown.bottom)) + 12 * gv.dp : gv.barTop * 0.4f;
        gv.text.setTextAlign(Paint.Align.CENTER);
        // ((12.0) Not over the Clear menu while it's open: its lower choices were hidden under the band.)
        // ((12.0) Nor over an open picker, or the buttons while you're controlling someone.)
        // ((12.0) Nor, held sideways, across the stats panel while it's open.)
        if (!gv.headlines.isEmpty() && !gv.clearMenu && gv.picker < 0 && gv.world.controlled == null && (gv.portrait || gv.statsCollapsed)) {
            Object[] hd = gv.headlines.get(0);
            float a = Math.min(1, Math.min(gv.headTime * 4, (HEAD_SECONDS - gv.headTime) / 0.6f));
            String title = (String) hd[0], sub = (String) hd[1];
            // ((12.0) The line under the headline goes onto a second line where it must, rather than being cut short.)
            String sub2 = null;
            if (sub != null) {
                gv.text.setTextSize(13 * gv.dp);
                float room = w - 24 * gv.dp;
                if (gv.text.measureText(sub) > room) {
                    int cut = sub.length();
                    while (cut > 0) {
                        int sp = sub.lastIndexOf(' ', cut - 1);
                        if (sp <= 0) break;
                        cut = sp;
                        if (gv.text.measureText(sub.substring(0, cut)) <= room) break;
                    }
                    if (cut > 0 && cut < sub.length()) {
                        sub2 = sub.substring(cut + 1);
                        sub = sub.substring(0, cut);
                    }
                }
            }
            float bandH = sub == null ? 44 * gv.dp : sub2 == null ? 64 * gv.dp : 82 * gv.dp;
            // ((12.0) Not when an open stats panel has pushed it down onto the tool bar: the panel is what's being
            // looked at then.)
            if (top + bandH <= gv.barTop - 8 * gv.dp) {
                gv.fill.setColor(alpha(0xE6101114, a));
                c.drawRect(0, top, w, top + bandH, gv.fill);
                gv.fill.setColor(alpha((Integer) hd[2], a));
                c.drawRect(0, top, w, top + 2 * gv.dp, gv.fill);
                gv.text.setTextSize(Math.min(26 * gv.dp, w / 18f));
                gv.text.setColor(alpha((Integer) hd[2], a));
                c.drawText(fitText(title, w - 24 * gv.dp), w / 2f, top + 32 * gv.dp, gv.text);
                if (sub != null) {
                    gv.text.setTextSize(13 * gv.dp);
                    gv.text.setColor(alpha(0xFFE6E6E6, a));
                    c.drawText(fitText(sub, w - 24 * gv.dp), w / 2f, top + 53 * gv.dp, gv.text);
                    if (sub2 != null) c.drawText(fitText(sub2, w - 24 * gv.dp), w / 2f, top + 71 * gv.dp, gv.text);
                }
                top += bandH + 8 * gv.dp;
            }
        }
        // ((12.0) Not over an open picker: it was drawn across the top of it.)
        if (gv.world.messageTime > 0 && gv.world.message != null && gv.picker < 0 && !gv.clearMenu) {
            float a = Math.min(1, gv.world.messageTime);
            gv.text.setTextSize(17 * gv.dp);
            // Fit between whatever else is on screen at that height (stats, meter, minimap, Auto cam).
            float bottom = top + 34 * gv.dp, l = 10 * gv.dp, rr = w - 10 * gv.dp;
            RectF[] around = {gv.statsShown, gv.meterRect, gv.miniRect, gv.camChip};
            for (RectF o : around) {
                if (o.isEmpty() || o.bottom < top || o.top > bottom) continue;
                if (o.centerX() < w / 2f) l = Math.max(l, o.right + 8 * gv.dp);
                else rr = Math.min(rr, o.left - 8 * gv.dp);
            }
            if (rr - l < 120 * gv.dp) {
                l = 10 * gv.dp;
                rr = w - 10 * gv.dp;
            }
            String msg = fitText(gv.world.message, rr - l - 28 * gv.dp);
            if (gv.text.measureText(gv.world.message) > rr - l - 28 * gv.dp) {
                gv.text.setTextSize(14 * gv.dp);
                msg = fitText(gv.world.message, rr - l - 28 * gv.dp);
            }
            float tw = gv.text.measureText(msg) + 28 * gv.dp, mid = (l + rr) / 2;
            gv.oval.set(mid - tw / 2, top, mid + tw / 2, bottom);
            gv.fill.setColor(alpha(0xD8101114, a));
            c.drawRoundRect(gv.oval, 10 * gv.dp, 10 * gv.dp, gv.fill);
            gv.text.setColor(alpha(0xFFFFE27A, a));
            c.drawText(msg, mid, gv.oval.centerY() + 6 * gv.dp, gv.text);
        }
    }

    /** The Clear menu under the Clear button, and the Undo button above the tool bar. */
    void drawPopups(Canvas c) {
        if (!gv.lastAction.isEmpty()) {
            gv.text.setTextSize(13 * gv.dp);
            gv.text.setTextAlign(Paint.Align.CENTER);
            gv.undoRect.set(10 * gv.dp, gv.barTop - 44 * gv.dp, 10 * gv.dp + 84 * gv.dp, gv.barTop - 10 * gv.dp);
            gv.fill.setColor(0xE0202227);
            c.drawRoundRect(gv.undoRect, 9 * gv.dp, 9 * gv.dp, gv.fill);
            gv.stroke.setColor(0x60FFFFFF);
            gv.stroke.setStrokeWidth(1 * gv.dp);
            c.drawRoundRect(gv.undoRect, 9 * gv.dp, 9 * gv.dp, gv.stroke);
            gv.text.setColor(0xFFF2F2F2);
            c.drawText("Undo", gv.undoRect.centerX(), gv.undoRect.centerY() + 4.5f * gv.dp, gv.text);
        } else gv.undoRect.setEmpty();
        if (!gv.clearMenu) return;
        RectF b = gv.topRects[BTN_CLEAR];
        // ((12.0) Shorter rows where five at 40 won't fit above the tool bar: on a short screen held sideways the
        // last of them were under it, out of reach.)
        float mw = Math.max(b.width(), 150 * gv.dp), ih = Math.max(26 * gv.dp, Math.min(40 * gv.dp, (gv.barTop - b.bottom - 6 * gv.dp) / gv.clearRects.length - 4 * gv.dp));
        float left = Math.min(b.left, gv.getWidth() - mw - 8 * gv.dp);
        gv.text.setTextSize(13 * gv.dp);
        gv.text.setTextAlign(Paint.Align.LEFT);
        for (int i = 0; i < gv.clearRects.length; i++) {
            RectF r = gv.clearRects[i];
            r.set(left, b.bottom + 6 * gv.dp + i * (ih + 4 * gv.dp), left + mw, b.bottom + 6 * gv.dp + i * (ih + 4 * gv.dp) + ih);
            gv.fill.setColor(i == 0 ? 0xF0802020 : 0xF0202227);
            c.drawRoundRect(r, 9 * gv.dp, 9 * gv.dp, gv.fill);
            gv.text.setColor(0xFFF2F2F2);
            boolean sure = i == 0 && System.nanoTime() - gv.clearArmedAt < 3000000000L;
            c.drawText(sure ? "Tap again to clear" : (i == 0 ? "Clear " : "") + CLEAR_NAMES[i], r.left + 12 * gv.dp, r.centerY() + 4.5f * gv.dp, gv.text);
        }
    }

    /** The stick, the attack button, what you're carrying and the way out. */
    void drawControls(Canvas c, Entity e) {
        int w = gv.getWidth();
        // Stick: where the thumb is, or a faint hint of where to put it.
        // (The idle hint is smaller and low down, clear of the panels.)
        float jr = gv.joyId >= 0 ? 55 * gv.dp : 38 * gv.dp;
        float jx = gv.joyId >= 0 ? gv.joyCX : w * 0.3f, jy = gv.joyId >= 0 ? gv.joyCY : gv.barTop - 50 * gv.dp;
        gv.fill.setColor(gv.joyId >= 0 ? 0x40FFFFFF : 0x22FFFFFF);
        c.drawCircle(jx, jy, jr, gv.fill);
        gv.stroke.setColor(0x60FFFFFF);
        gv.stroke.setStrokeWidth(1.5f * gv.dp);
        c.drawCircle(jx, jy, jr, gv.stroke);
        gv.fill.setColor(gv.joyId >= 0 ? 0xC0FFFFFF : 0x50FFFFFF);
        c.drawCircle(gv.joyId >= 0 ? gv.joyKX : jx, gv.joyId >= 0 ? gv.joyKY : jy, gv.joyId >= 0 ? 22 * gv.dp : 15 * gv.dp, gv.fill);
        // Attack (there's nothing to attack from inside a building).
        gv.attackR = 38 * gv.dp;
        gv.attackX = w - 70 * gv.dp;
        gv.attackY = gv.barTop - 80 * gv.dp;
        Fleet.Vehicle car = gv.world.controlledCar;
        String verb = car != null ? (car.type == Fleet.TANK ? "Fire" : car.type == Fleet.CRUISER ? "Siren" : "Horn")
                : e.isUndead() ? "Bite" : e.canShoot() && e.ammo + e.reserve > 0 ? (e.reload > 0 ? "Reload" : "Shoot")
                : e.melee > 0 ? "Swing" : "Shove";
        gv.text.setTextAlign(Paint.Align.CENTER);
        gv.fill.setColor(gv.world.ctrlAttack ? 0xF0D9534F : 0xB0D9534F);
        c.drawCircle(gv.attackX, gv.attackY, gv.attackR, gv.fill);
        gv.stroke.setColor(0xA0FFFFFF);
        c.drawCircle(gv.attackX, gv.attackY, gv.attackR, gv.stroke);
        gv.text.setTextSize(14 * gv.dp);
        gv.text.setColor(0xFFFFFFFF);
        c.drawText(verb, gv.attackX, gv.attackY + 5 * gv.dp, gv.text);
        // What else you can do here, stacked above the attack button.
        gv.ctxCount = gv.world.ctrlOptions(gv.ctxActions);
        float cbw = 118 * gv.dp, cbh = 38 * gv.dp, cby = gv.attackY - gv.attackR - 12 * gv.dp - cbh;
        gv.text.setTextSize(13 * gv.dp);
        // ((12.0) As many as fit under the buttons along the top, then another column to the left: on a short screen
        // held sideways the stack went up off the top of it.)
        float ceiling = gv.topRects[0].isEmpty() ? 8 * gv.dp : gv.topRects[0].bottom + 8 * gv.dp;
        int perCol = Math.max(1, (int) ((cby - ceiling) / (cbh + 8 * gv.dp)) + 1);
        for (int k = 0; k < gv.ctxCount; k++) {
            RectF r = gv.ctxRects[k];
            int col = k / perCol, row = k % perCol;
            float rx = gv.attackX + gv.attackR - col * (cbw + 8 * gv.dp);
            r.set(rx - cbw, cby - row * (cbh + 8 * gv.dp), rx, cby - row * (cbh + 8 * gv.dp) + cbh);
            gv.fill.setColor(0xE02F5E2B);
            c.drawRoundRect(r, 9 * gv.dp, 9 * gv.dp, gv.fill);
            gv.stroke.setColor(0x909BE08A);
            c.drawRoundRect(r, 9 * gv.dp, 9 * gv.dp, gv.stroke);
            gv.text.setColor(0xFFFFFFFF);
            String label = World.CA_NAMES[gv.ctxActions[k]] + (gv.ctxActions[k] == World.CA_GRENADE ? " (" + e.grenades + ")" : "");
            c.drawText(label, r.centerX(), r.centerY() + 4.5f * gv.dp, gv.text);
        }
        // Who you are, and the way out.
        String who = e.isUndead() ? Entity.NAMES[e.type] : e.type == Entity.DOG ? Names.dog(e.nameSeed) : Names.person(e.nameSeed);
        String gun = e.canShoot() ? "   " + e.gunName() + " " + e.ammo + " + " + e.reserve : "";
        String place = car != null ? "   Driving (" + Math.max(0, (int) (100 * car.hp / car.maxHp)) + "%)" : "";
        int group = gv.world.followers(e);
        String info = who + "   Health " + Math.max(0, (int) e.hp) + "/" + (int) e.maxHp + gun
                + (e.melee > 0 ? "   " + Entity.MELEE_NAMES[e.melee] : "") + place + (group > 0 ? "   Group " + group : "")
                + (e.infected ? "   BITTEN: " + Math.max(0, (int) e.infectTimer) + "s" : "")
                + (!e.isUndead() && e.stamina < 0.3f ? "   Out of breath" : "");
        gv.text.setTextSize(13 * gv.dp);
        // ((12.0) Smaller when it won't fit across the screen, rather than running off both sides.)
        float room = w - 24 * gv.dp;
        if (gv.text.measureText(info) > room) gv.text.setTextSize(Math.max(9 * gv.dp, 13 * gv.dp * room / gv.text.measureText(info)));
        info = fitText(info, room);
        float tw = gv.text.measureText(info) + 24 * gv.dp, top = gv.barTop - 46 * gv.dp;
        gv.oval.set((w - tw) / 2, top, (w + tw) / 2, top + 26 * gv.dp);
        gv.fill.setColor(0xD0101216);
        c.drawRoundRect(gv.oval, 8 * gv.dp, 8 * gv.dp, gv.fill);
        gv.text.setColor(e.infected ? 0xFF9BE08A : 0xFFF2F2F2);
        c.drawText(info, w / 2f, top + 18 * gv.dp, gv.text);
        float bw = 150 * gv.dp, bh = 34 * gv.dp, by = top - bh - 10 * gv.dp;
        gv.releaseRect.set((w - bw) / 2, by, (w + bw) / 2, by + bh);
        // ((12.0) On a narrow screen, clear of the attack button: it was drawn half over it.)
        if (gv.releaseRect.right > gv.attackX - gv.attackR - 6 * gv.dp) {
            float right = gv.attackX - gv.attackR - 8 * gv.dp;
            gv.releaseRect.set(Math.max(10 * gv.dp, right - bw), by, right, by + bh);
        }
        gv.fill.setColor(0xE0202227);
        c.drawRoundRect(gv.releaseRect, 9 * gv.dp, 9 * gv.dp, gv.fill);
        gv.stroke.setColor(0x80FFFFFF);
        c.drawRoundRect(gv.releaseRect, 9 * gv.dp, 9 * gv.dp, gv.stroke);
        gv.text.setColor(0xFFFFFFFF);
        c.drawText("Stop controlling", gv.releaseRect.centerX(), gv.releaseRect.centerY() + 4.5f * gv.dp, gv.text);
    }

    /** Draws text on a dark pill above the tool bar, wrapping onto more lines on narrow screens. */
    void drawBanner(Canvas c, String s, float cx, float by) {
        float maxW = gv.getWidth() - 48 * gv.dp;
        java.util.ArrayList<String> lines = new java.util.ArrayList<String>();
        String line = "";
        for (String word : s.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && gv.text.measureText(next) > maxW) {
                lines.add(line.trim());
                line = word;
            } else {
                line = next;
            }
        }
        lines.add(line.trim());
        float lineH = gv.text.getTextSize() * 1.35f, widest = 0;
        for (String l : lines) widest = Math.max(widest, gv.text.measureText(l));
        float top = by - lineH * (lines.size() - 1);
        gv.oval.set(cx - widest / 2 - 12 * gv.dp, top - 18 * gv.dp, cx + widest / 2 + 12 * gv.dp, by + 7 * gv.dp);
        gv.fill.setColor(0xB0000000);
        c.drawRoundRect(gv.oval, 8 * gv.dp, 8 * gv.dp, gv.fill);
        gv.text.setColor(0xFFFFFFFF);
        for (int i = 0; i < lines.size(); i++) c.drawText(lines.get(i), cx, top + i * lineH, gv.text);
    }
}
