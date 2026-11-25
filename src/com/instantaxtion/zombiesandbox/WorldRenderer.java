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
 * ((27.3) Out of GameView.) Draws the city as it is this frame, layer by layer from the ground up: the map picture and its
 * close-ups, what lies on the ground (blood, bodies, wrecks, barricades, safe zones, bases), everyone and everything on it,
 * the buildings over them, then smoke, aircraft and the marks over it all.
 */
final class WorldRenderer {
    final GameView gv;

    WorldRenderer(GameView gv) {
        this.gv = gv;
    }


    /** (10.60) A faint line along the streets through tiles[from..to), from (x, y), with a dot where it ends. */
    void drawRoute(Canvas c, float x, float y, int[] tiles, int from, int to, int col, float u) {
        if (to - from < 1) return;
        int w = gv.world.city.w;
        gv.routePath.reset();
        gv.routePath.moveTo(x, y);
        float ex = x, ey = y;
        for (int i = from; i < to; i++) {
            ex = (tiles[i] % w + 0.5f) * City.T;
            ey = (tiles[i] / w + 0.5f) * City.T;
            gv.routePath.lineTo(ex, ey);
        }
        // (A dark edge under it, so it reads on the grey of the road.)
        gv.stroke.setStrokeWidth(5.5f * u);
        gv.stroke.setColor(0x55000000);
        c.drawPath(gv.routePath, gv.stroke);
        gv.stroke.setStrokeWidth(3.2f * u);
        gv.stroke.setColor(alpha(col, 0.8f));
        c.drawPath(gv.routePath, gv.stroke);
        gv.fill.setColor(alpha(col, 0.8f));
        c.drawCircle(ex, ey, 3.5f * u, gv.fill);
    }

    /** (10.59) One splat on the ground: blood, a puddle, glass, tyre marks; fade is how much of it is left. */
    void drawDecal(Canvas c, int i, float x, float y, float r, byte kind, float fade) {
        if (kind == World.D_SKID) {
            // Tyre marks.
            gv.stroke.setColor(Hud.fadeCol(gv.world.dcol[i], fade));
            gv.stroke.setStrokeWidth(1.4f);
            float ca = (float) Math.cos(gv.world.dang[i]) * r, sa = (float) Math.sin(gv.world.dang[i]) * r;
            c.drawLine(x - ca, y - sa, x, y, gv.stroke);
        } else if (kind == World.D_ACID) {
            gv.fill.setColor(Hud.fadeCol(gv.world.dcol[i], fade));
            c.drawCircle(x, y, r, gv.fill);
            gv.fill.setColor(Hud.fadeCol(0x55E8FF7A, fade));
            c.drawCircle(x + r * 0.3f, y - r * 0.2f, r * 0.3f, gv.fill);
            c.drawCircle(x - r * 0.4f, y + r * 0.3f, r * 0.2f, gv.fill);
        } else if (kind == World.D_GLASS) {
            gv.stroke.setColor(Hud.fadeCol(gv.world.dcol[i], fade));
            gv.stroke.setStrokeWidth(0.6f);
            float ca = (float) Math.cos(gv.world.dang[i]) * r, sa = (float) Math.sin(gv.world.dang[i]) * r;
            c.drawLine(x - ca, y - sa, x + ca, y + sa, gv.stroke);
        } else if (kind == World.D_PUDDLE) {
            gv.fill.setColor(Hud.fadeCol(gv.world.dcol[i], fade));
            c.drawCircle(x, y, r, gv.fill);
            gv.fill.setColor(Hud.fadeCol(0x20FFFFFF, fade));
            c.drawCircle(x - r * 0.3f, y - r * 0.3f, r * 0.35f, gv.fill);
        } else {
            gv.fill.setColor(Hud.fadeCol(gv.world.dcol[i], fade));
            c.drawCircle(x, y, r, gv.fill);
        }
    }

    void drawCloseUps(Canvas c) {
        City city = gv.world.city;
        if (city != gv.chunkCity || city.renderVersion != gv.chunkVersion) {
            gv.chunkCity = city;
            gv.chunkVersion = city.renderVersion;
            gv.chunks.clear();
            gv.chunkJobs.clear();
            gv.chunkPending.clear();
            city.changes.clear();
        }
        // Small changes: the close-ups over them are drawn again, the old ones staying up until they're ready.
        for (float[] r; (r = city.changes.poll()) != null; ) {
            // (With room for shadows, as a close-up is drawn.)
            int ax0 = Math.max(0, (int) ((r[0] - 30) / CHUNK)), ax1 = (int) ((r[2] + 90) / CHUNK);
            int ay0 = Math.max(0, (int) ((r[1] - 30) / CHUNK)), ay1 = (int) ((r[3] + 90) / CHUNK);
            for (int ky = ay0; ky <= ay1; ky++)
                for (int kx = ax0; kx <= ax1; kx++)
                    for (int kr = 2; kr <= 8; kr *= 2) {
                        long key = ((long) kr << 40) | ((long) ky << 20) | kx;
                        if (!gv.chunks.containsKey(key)) continue;
                        if (!gv.chunksOnScreen.contains(key)) gv.chunks.remove(key);
                        else if (gv.chunkPending.add(key)) gv.chunkJobs.addFirst(new Object[]{city, key, kx * CHUNK, ky * CHUNK, (float) kr, gv.chunkVersion});
                    }
        }
        // Only once the map picture is being stretched noticeably.
        gv.closeRes = 0;
        if (gv.scale < city.detail * 1.6f) return;
        // (10.26) Zoomed right in, an even finer close-up, so roofs and ground stay sharp at the closest zoom.
        int res = gv.scale >= 5.5f && gv.world.quality == World.Q_FULL ? 8 : gv.scale >= 3.2f && gv.world.quality < World.Q_LOW ? 4 : 2;
        gv.closeRes = res;
        int cx0 = Math.max(0, (int) (gv.camX / CHUNK)), cy0 = Math.max(0, (int) (gv.camY / CHUNK));
        int cx1 = (int) ((gv.camX + gv.getWidth() / gv.scale) / CHUNK), cy1 = (int) ((gv.camY + gv.barTop / gv.scale) / CHUNK);
        int nx = (int) Math.ceil(city.worldW() / CHUNK), ny = (int) Math.ceil(city.worldH() / CHUNK);
        cx1 = Math.min(nx - 1, cx1);
        cy1 = Math.min(ny - 1, cy1);
        gv.chunksWanted.clear();
        float mx = gv.camX + gv.getWidth() / gv.scale / 2, my = gv.camY + gv.barTop / gv.scale / 2;
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                long key = ((long) res << 40) | ((long) cy << 20) | cx;
                gv.chunksWanted.add(key);
                android.graphics.Bitmap b = gv.chunks.get(key);
                if (b == null) {
                    // A coarser one will do until the finer one is ready.
                    b = gv.chunks.get(((long) (res == 8 ? 4 : 6 - res) << 40) | ((long) cy << 20) | cx);
                    if (b == null && res == 8) b = gv.chunks.get(((long) 2 << 40) | ((long) cy << 20) | cx);
                    if (gv.chunkPending.add(key)) {
                        Object[] job = {city, key, cx * CHUNK, cy * CHUNK, (float) res, gv.chunkVersion};
                        // Nearest the middle of the screen first.
                        if (Math.abs((cx + 0.5f) * CHUNK - mx) < CHUNK && Math.abs((cy + 0.5f) * CHUNK - my) < CHUNK) gv.chunkJobs.addFirst(job);
                        else gv.chunkJobs.addLast(job);
                    }
                }
                if (b == null) continue;
                gv.mapSrc.set(0, 0, b.getWidth(), b.getHeight());
                gv.mapDst.set(cx * CHUNK, cy * CHUNK, (cx + 1) * CHUNK, (cy + 1) * CHUNK);
                c.drawBitmap(b, gv.mapSrc, gv.mapDst, gv.bmpPaint);
            }
        // Just off the edges too (at the usual sharpness), so panning doesn't show the soft picture first.
        if (res == 2)
            for (int cy = Math.max(0, cy0 - 1); cy <= Math.min(ny - 1, cy1 + 1); cy++)
                for (int cx = Math.max(0, cx0 - 1); cx <= Math.min(nx - 1, cx1 + 1); cx++) {
                    if (cx >= cx0 && cx <= cx1 && cy >= cy0 && cy <= cy1) continue;
                    long key = ((long) res << 40) | ((long) cy << 20) | cx;
                    gv.chunksWanted.add(key);
                    if (!gv.chunks.containsKey(key) && gv.chunkPending.add(key))
                        gv.chunkJobs.addLast(new Object[]{city, key, cx * CHUNK, cy * CHUNK, (float) res, gv.chunkVersion});
                }
        gv.chunksOnScreen = new java.util.HashSet<Long>(gv.chunksWanted);
        // Forget the ones furthest from view when there are too many.
        // (The finest close-ups are big: fewer of those are kept.)
        // ((22.6) Twelve of the finest, not eighteen: each is several megabytes.)
        int keep = res == 8 ? 12 : MAX_CHUNKS;
        if (gv.chunks.size() > keep)
            for (Long k : gv.chunks.keySet()) {
                if (gv.chunks.size() <= keep) break;
                if (!gv.chunksWanted.contains(k)) gv.chunks.remove(k);
            }
        if (gv.chunkWorker == null) {
            gv.chunkWorker = new Thread(new Runnable() {
                public void run() {
                    while (true) {
                        Object[] job;
                        try {
                            job = gv.chunkJobs.takeFirst();
                        } catch (InterruptedException e) {
                            return;
                        }
                        City jc = (City) job[0];
                        long key = (Long) job[1];
                        // (Skipped if the view has moved on, or the map has changed since.)
                        if (jc != gv.chunkCity || (Integer) job[5] != gv.chunkVersion || !gv.chunksOnScreen.contains(key)) {
                            // (An out-of-date one isn't kept for later either.)
                            if (jc == gv.chunkCity && (Integer) job[5] == gv.chunkVersion) gv.chunks.remove(key);
                            gv.chunkPending.remove(key);
                            continue;
                        }
                        try {
                            android.graphics.Bitmap b = jc.renderRegion((Float) job[2], (Float) job[3], CHUNK, (Float) job[4]);
                            if (jc == gv.chunkCity && (Integer) job[5] == gv.chunkVersion) gv.chunks.put(key, b);
                        } catch (Throwable t) {
                            // (Out of memory or similar: the soft picture will do.)
                        }
                        gv.chunkPending.remove(key);
                    }
                }
            }, "map close-ups");
            gv.chunkWorker.setDaemon(true);
            // (Not the lowest priority: on a phone that left the close-ups waiting on everything else.)
            gv.chunkWorker.setPriority(Thread.NORM_PRIORITY - 1);
            gv.chunkWorker.start();
        }
    }

    void drawWorld(Canvas c) {
        setUpView(c);

        float vx0 = gv.camX - 20, vy0 = gv.camY - 20;
        float vx1 = gv.camX + gv.getWidth() / gv.scale + 20, vy1 = gv.camY + gv.getHeight() / gv.scale + 20;
        boolean detailed = gv.scale > 1.1f;
        // ((11.5) Who's on screen, found in one pass: the loops below that draw people only look at them, not at
        // every one of the thousands in a big outbreak, again and again.)
        gv.viewL = vx0;
        gv.viewT = vy0;
        gv.viewR = vx1;
        gv.viewB = vy1;
        gv.onScreen.clear();
        gv.world.collectInView(vx0, vy0, vx1, vy1, gv.onScreen);
        drawGround(c, vx0, vy0, vx1, vy1);

        drawGroundThings(c, vx0, vy0, vx1, vy1);
        // Zoomed out, a crowd is a heap of dots on top of each other: one dot per side in each little patch
        // of the screen looks the same and saves drawing thousands.
        drawPeople(c, vx0, vy0, vx1, vy1, detailed);
        drawMarkings(c, vx0, vy0, vx1, vy1);
        // Emergency lights wash the road around them in red and blue.
        drawVehicles(c, vx0, vy0, vx1, vy1, detailed);

        drawShots(c, vx0, vy0, vx1, vy1);

        gv.buildingRenderer.drawBuildings(c, vx0, vy0, vx1, vy1, detailed);
        if (!gv.world.overwatch.isEmpty()) gv.buildingRenderer.drawOverwatch(c, vx0, vy0, vx1, vy1, detailed);
        drawHaze(c, vx0, vy0, vx1, vy1);
        // ((27.5) Infection heat or police/army reach, over the streets and roofs.)
        gv.overlays.draw(c, gv.world, vx0, vy0, vx1, vy1);
        // ((19.5) Light, shockwaves, smoke, sparks and debris over it all.)
        gv.vfx.drawAbove(c, vx0, vy0, vx1, vy1);
        drawAircraft(c, vx0, vy0, vx1, vy1);

        drawBrushRing(c);
        // (10.46) Infection zones: a dashed hazard ring round each, red while it's held, amber while it waits its turn.
        drawMarkers(c);
        // ((11.0) Police drones: a little quadcopter, its shadow below, rotors blurring, lights blinking, and the patch
        // of ground its camera's watching.)
        drawDronesAndBlasts(c);
        c.restore();
    }

    /** ((27.3) Step 1 of drawWorld.) The canvas set to the camera (shaken when the ground shakes), and the map picture under it all with its close-ups. */
    void setUpView(Canvas c) {
        c.drawColor(0xFF1B1C1F);
        c.save();
        if (gv.world.shake > 0 && gv.settings.shake()) {
            float s = gv.world.shake * 5 * gv.dp;
            c.translate((gv.rnd.nextFloat() - 0.5f) * s, (gv.rnd.nextFloat() - 0.5f) * s);
        }
        c.scale(gv.scale, gv.scale);
        c.translate(-gv.camX, -gv.camY);
        gv.world.viewX0 = gv.camX;
        gv.world.viewY0 = gv.camY;
        gv.world.viewX1 = gv.camX + gv.getWidth() / gv.scale;
        gv.world.viewY1 = gv.camY + gv.getHeight() / gv.scale;
        // Realistic graphics smooth the map when zoomed in; classic keeps the crisp pixels.
        gv.bmpPaint.setFilterBitmap(gv.realistic || gv.scale < 1.5f);
        // ((22.1) Only the tiles of the map picture on screen.)
        {
            City mc = gv.world.city;
            float vx0 = Math.max(0, gv.camX), vy0 = Math.max(0, gv.camY);
            float vx1 = Math.min(mc.worldW(), gv.camX + gv.getWidth() / gv.scale), vy1 = Math.min(mc.worldH(), gv.camY + gv.getHeight() / gv.scale);
            float dl = mc.detail;
            float sx0 = (float) Math.floor(vx0 * dl), sy0 = (float) Math.floor(vy0 * dl);
            float sx1 = Math.min(mc.mapW, (float) Math.ceil(vx1 * dl)), sy1 = Math.min(mc.mapH, (float) Math.ceil(vy1 * dl));
            if (sx1 > sx0 && sy1 > sy0) {
                gv.mapDst.set(sx0 / dl, sy0 / dl, sx1 / dl, sy1 / dl);
                mc.drawMap(c, sx0, sy0, sx1, sy1, gv.mapDst, gv.bmpPaint);
            }
        }
        if (gv.world.city.detail < 0.999f) {
            // ((16.6) Close in on a low-detail map: the parked vehicles drawn sharp, not left as smudges.)
            if (gv.scale > 1.1f) gv.world.city.drawParked(c, gv.camX - City.T, gv.camY - City.T, gv.camX + gv.getWidth() / gv.scale + City.T, gv.camY + gv.getHeight() / gv.scale + City.T);
        }
        drawCloseUps(c);
    }

    /** ((27.3) Step 2.) The ground itself: water and terrain, blood and marks, ground effects and what lies there. */
    void drawGround(Canvas c, float vx0, float vy0, float vx1, float vy1) {
        if (gv.scale > 0.8f && (gv.world.city.waterKind != CityConfig.W_NONE || gv.world.city.elev != null)) drawWaterLife(c, vx0, vy0, vx1, vy1);

        for (int i = 0; i < gv.world.dcount; i++) {
            float x = gv.world.dx[i], y = gv.world.dy[i], r = gv.world.dr[i];
            if (r <= 0 || x < vx0 || x > vx1 || y < vy0 || y > vy1) continue;
            byte kind = gv.world.dkind[i];
            // ((10.59) Fading away.)
            float fade = gv.world.decalFade(i);
            if (fade <= 0) continue;
            drawDecal(c, i, x, y, r, kind, fade);
        }

        if (gv.scale > 0.7f) gv.buildingRenderer.drawMarks(c, vx0, vy0, vx1, vy1);
        // ((19.5) Craters, cracks and ripples on the ground.)
        gv.vfx.drawGround(c, vx0, vy0, vx1, vy1);
        // ((21.3) Flames on the ground, under the people.)
        for (int i = 0; i < World.MAXP; i++) {
            if (gv.world.plife[i] <= 0 || gv.world.ptype[i] != World.P_FIRE) continue;
            float x = gv.world.px[i], y = gv.world.py[i];
            if (x < vx0 || x > vx1 || y < vy0 || y > vy1) continue;
            float f = gv.world.plife[i] / gv.world.pmax[i];
            gv.fill.setColor(alpha(gv.world.pcol[i], f));
            c.drawCircle(x, y, gv.world.psize[i] * (0.4f + f * 0.6f), gv.fill);
        }
    }

    /** ((27.3) Step 3.) What stands on the ground: safe zones, survivor bases, holdouts, barriers, pickups, animals, the boats, bodies, crash sites, command posts and the danger shading. */
    void drawGroundThings(Canvas c, float vx0, float vy0, float vx1, float vy1) {
        for (int i = 0, n = gv.world.dispatch.zones.size(); i < n; i++) drawZone(c, gv.world.dispatch.zones.get(i));
        for (int i = 0, n = gv.world.bases.size(); i < n; i++) drawBase(c, gv.world.bases.get(i), vx0, vy0, vx1, vy1);
        for (int i = 0, n = gv.world.holdouts.size(); i < n; i++) {
            World.Holdout h = gv.world.holdouts.get(i);
            if (h.b.x1 + 40 < vx0 || h.b.x0 - 40 > vx1 || h.b.y1 + 40 < vy0 || h.b.y0 - 40 > vy1) continue;
            drawHoldout(c, h);
        }

        drawBarricades(c, vx0, vy0, vx1, vy1);
        drawPickups(c, vx0, vy0, vx1, vy1);
        // Deer and foxes.
        drawAnimals(c, vx0, vy0, vx1, vy1);
        for (int i = 0, n = gv.world.city.buildings.size(); i < n; i++) {
            City.Building b = gv.world.city.buildings.get(i);
            if (!b.doorBroken || b.collapsed || b.doorX < vx0 || b.doorX > vx1 || b.doorY < vy0 || b.doorY > vy1) continue;
            gv.buildingRenderer.drawBrokenDoor(c, b);
        }
        for (int i = 0, n = gv.world.city.buildings.size(); i < n; i++) {
            City.Building b = gv.world.city.buildings.get(i);
            if (b.occupants.isEmpty() || b.doorX < vx0 || b.doorX > vx1 || b.doorY < vy0 || b.doorY > vy1) continue;
            // Boarded-up door.
            gv.fill.setColor(0xFF7A5634);
            c.drawRect(b.doorX - 5, b.doorY - 1.5f, b.doorX + 5, b.doorY + 0.2f, gv.fill);
            c.drawRect(b.doorX - 4, b.doorY + 1.2f, b.doorX + 4, b.doorY + 2.9f, gv.fill);
        }

        // ((15.6) The ferry across the bay.)
        if (gv.world.ferryBoat != null) gv.vehicleRenderer.drawBoat(c, gv.world.ferryBoat);
        for (int i = 0, n = gv.world.boats.size(); i < n; i++) {
            float[] b = gv.world.boats.get(i);
            if (b[0] < vx0 - 30 || b[0] > vx1 + 30 || b[1] < vy0 - 30 || b[1] > vy1 + 30) continue;
            // (Out of sight under a bridge.)
            if (gv.world.city.isBridge((int) (b[0] / City.T), (int) (b[1] / City.T))) continue;
            gv.vehicleRenderer.drawBoat(c, b);
        }
        // Waterfalls: white water streaming down and spray drifting up from the foot.
        for (int i = 0, n = gv.world.city.falls.size(); i < n; i++) {
            float[] f = gv.world.city.falls.get(i);
            if (f[0] < vx0 - 30 || f[0] > vx1 + 30 || f[1] < vy0 - 30 || f[1] > vy1 + 30) continue;
            // (Spread across the width of the stream at the lip, falling away downstream of it.)
            float half = f.length > 4 ? f[4] - 3 : 5;
            for (int k = 0; k < 6; k++) {
                float ph = (gv.world.time * 1.6f + k / 6f) % 1f, side = (k - 2.5f) / 2.5f * half * 0.8f;
                float along = -1 + ph * 12;
                float px = f[0] + f[2] * along - f[3] * side, py = f[1] + f[3] * along + f[2] * side;
                gv.fill.setColor(alpha(0xFFF4FAFC, 0.85f * (1 - ph)));
                c.drawCircle(px, py, 1.1f + ph * 1.6f, gv.fill);
            }
        }
        for (int i = 0, n = gv.world.corpses.size(); i < n; i++) {
            World.Corpse k = gv.world.corpses.get(i);
            if (k.x < vx0 || k.x > vx1 || k.y < vy0 || k.y > vy1) continue;
            gv.entityRenderer.drawCorpse(c, k);
        }
        for (int i = 0, n = gv.world.medkits.size(); i < n; i++) {
            float[] m = gv.world.medkits.get(i);
            gv.fill.setColor(alpha(0xFF63E06B, 0.18f + 0.1f * (float) Math.sin(gv.world.time * 3)));
            c.drawCircle(m[0], m[1], 40, gv.fill);
            gv.fill.setColor(0xFFF2F2F2);
            c.drawRect(m[0] - 4, m[1] - 3, m[0] + 4, m[1] + 3, gv.fill);
            gv.fill.setColor(0xFFD83A3A);
            c.drawRect(m[0] - 0.7f, m[1] - 2.2f, m[0] + 0.7f, m[1] + 2.2f, gv.fill);
            c.drawRect(m[0] - 2.2f, m[1] - 0.7f, m[0] + 2.2f, m[1] + 0.7f, gv.fill);
        }
        if (!gv.world.crashes.isEmpty()) drawCrashSites(c, vx0, vy0, vx1, vy1);
        for (Ops.Post p : gv.world.ops.posts) if (p.up && p.truck != null) drawCommandPost(c, p, vx0, vy0, vx1, vy1);
        // ((27.0) A command post's fire mission on its way in: a red ring round where the shells will land, closing.
        for (Ops.Post p : gv.world.ops.posts) {
            if (gv.world.time >= p.fireUntil || p.fireX < vx0 - 90 || p.fireX > vx1 + 90 || p.fireY < vy0 - 90 || p.fireY > vy1 + 90) continue;
            float left = p.fireUntil - gv.world.time, k = Math.max(0, Math.min(1, (left - World.FIRE_LASTS) / World.FIRE_DELAY));
            float pulse = 0.6f + 0.4f * (float) Math.sin(gv.world.time * 9);
            gv.fill.setColor(alpha(0xFFE0362A, 0.10f + 0.06f * pulse));
            c.drawCircle(p.fireX, p.fireY, 66, gv.fill);
            gv.stroke.setColor(alpha(0xFFFF4A3A, 0.55f + 0.35f * pulse));
            gv.stroke.setStrokeWidth(Math.max(1.6f, 2.2f / gv.scale));
            c.drawCircle(p.fireX, p.fireY, 66, gv.stroke);
            c.drawCircle(p.fireX, p.fireY, 14 + 52 * k, gv.stroke);
            c.drawLine(p.fireX - 10, p.fireY, p.fireX + 10, p.fireY, gv.stroke);
            c.drawLine(p.fireX, p.fireY - 10, p.fireX, p.fireY + 10, gv.stroke);
        }

        if (gv.scale < 1.8f) drawDanger(c, vx0, vy0, vx1, vy1);
    }

    /** ((27.3) From drawGroundThings.) Barricades, by what they're built of and how damaged. */
    void drawBarricades(Canvas c, float vx0, float vy0, float vx1, float vy1) {
        for (int i = 0, n = gv.world.barriers.size(); i < n; i++) {
            float[] b = gv.world.barriers.get(i);
            if (b[0] < vx0 || b[0] > vx1 || b[1] < vy0 || b[1] > vy1) continue;
            if (b.length > 5 && b[5] >= 1) {
                drawToughBarricade(c, b);
                continue;
            }
            // Planks and sandbags; they splinter as they take hits.
            gv.fill.setColor(0x55000000);
            c.drawRect(b[0] - 7, b[1] - 5, b[0] + 9, b[1] + 8, gv.fill);
            for (int k = 0; k < 3; k++) {
                gv.fill.setColor(k % 2 == 0 ? 0xFFA38D5E : 0xFF917C50);
                c.drawCircle(b[0] - 5 + k * 5, b[1] + 3, 3, gv.fill);
            }
            gv.fill.setColor(0xFF8A6238);
            c.drawRect(b[0] - 8, b[1] - 4, b[0] + 8, b[1] - 1.5f, gv.fill);
            if (b[2] > 110) c.drawRect(b[0] - 7, b[1] - 1, b[0] + 7, b[1] + 1.2f, gv.fill);
            gv.fill.setColor(0xFFE8C547);
            c.drawRect(b[0] - 6, b[1] - 4, b[0] - 3, b[1] - 1.5f, gv.fill);
            c.drawRect(b[0] + 2, b[1] - 4, b[0] + 5, b[1] - 1.5f, gv.fill);
        }
    }

    /** ((27.3) From drawGroundThings.) Things lying about to be picked up: crates, guns, ammo. */
    void drawPickups(Canvas c, float vx0, float vy0, float vx1, float vy1) {
        for (int i = 0, n = gv.world.pickups.size(); i < n; i++) {
            World.Pickup p = gv.world.pickups.get(i);
            if (p.x < vx0 || p.x > vx1 || p.y < vy0 || p.y > vy1) continue;
            if (p.uses > 0) {
                // Supply crate (on a parachute while it drops).
                float h = Math.max(0, p.drop) * 30;
                gv.fill.setColor(0x55000000);
                c.drawRect(p.x - 5 + h * 0.3f, p.y - 4 + h * 0.4f, p.x + 6 + h * 0.3f, p.y + 5 + h * 0.4f, gv.fill);
                float cy = p.y - h;
                if (p.drop > 0) {
                    gv.fill.setColor(0xE0E8E8E8);
                    gv.oval.set(p.x - 12, cy - 22, p.x + 12, cy - 8);
                    c.drawOval(gv.oval, gv.fill);
                    gv.stroke.setColor(0xAA555555);
                    gv.stroke.setStrokeWidth(0.5f);
                    c.drawLine(p.x - 11, cy - 14, p.x - 3, cy - 3, gv.stroke);
                    c.drawLine(p.x + 11, cy - 14, p.x + 3, cy - 3, gv.stroke);
                }
                gv.fill.setColor(0xFF4F5E36);
                c.drawRect(p.x - 5, cy - 4, p.x + 5, cy + 4, gv.fill);
                gv.fill.setColor(0xFFF2F2F2);
                c.drawRect(p.x - 0.8f, cy - 3, p.x + 0.8f, cy + 3, gv.fill);
                c.drawRect(p.x - 3, cy - 0.8f, p.x + 3, cy + 0.8f, gv.fill);
                continue;
            }
            // A dropped weapon, gently pulsing so it can be spotted.
            gv.fill.setColor(alpha(0xFFFFE27A, 0.25f + 0.15f * (float) Math.sin(gv.world.time * 4)));
            c.drawCircle(p.x, p.y, 5.5f, gv.fill);
            gv.stroke.setStrokeWidth(1.4f);
            if (p.melee == Entity.M_BAT) {
                gv.stroke.setColor(0xFF9A6A3A);
                gv.stroke.setStrokeWidth(1.8f);
                c.drawLine(p.x - 4, p.y + 2, p.x + 4, p.y - 2, gv.stroke);
            } else if (p.melee == Entity.M_AXE) {
                gv.stroke.setColor(0xFF7A5030);
                c.drawLine(p.x - 4, p.y + 2, p.x + 3, p.y - 2, gv.stroke);
                gv.fill.setColor(0xFFC8302A);
                c.drawRect(p.x + 2, p.y - 4, p.x + 4.5f, p.y, gv.fill);
            } else if (p.melee == Entity.M_MACHETE || p.melee == Entity.M_CROWBAR) {
                gv.stroke.setColor(p.melee == Entity.M_MACHETE ? 0xFFC8CCD0 : 0xFF7A1E1A);
                gv.stroke.setStrokeWidth(1.6f);
                c.drawLine(p.x - 4, p.y + 2, p.x + 4, p.y - 2, gv.stroke);
                if (p.melee == Entity.M_CROWBAR) c.drawLine(p.x + 4, p.y - 2, p.x + 5, p.y + 0.5f, gv.stroke);
            } else if (p.weapon == Entity.W_SHOTGUN || p.weapon == Entity.W_RIFLE || p.weapon == Entity.W_HUNTING || p.weapon >= Entity.W_SMG) {
                gv.stroke.setColor(0xFF1A1A1A);
                c.drawLine(p.x - 4.5f, p.y + 1, p.x + 5, p.y - 1, gv.stroke);
                gv.stroke.setColor(p.weapon == Entity.W_SHOTGUN || p.weapon == Entity.W_HUNTING ? 0xFF8A5A30 : 0xFF3A4430);
                gv.stroke.setStrokeWidth(2f);
                c.drawLine(p.x - 4.5f, p.y + 1, p.x - 2, p.y + 0.5f, gv.stroke);
            } else {
                gv.stroke.setColor(0xFF1A1A1A);
                c.drawLine(p.x - 3, p.y, p.x + 3.5f, p.y - 1, gv.stroke);
                c.drawLine(p.x - 2.5f, p.y, p.x - 3, p.y + 2.2f, gv.stroke);
            }
        }
    }

    /** ((27.3) From drawGroundThings.) Animals, and the birds on the ground. */
    void drawAnimals(Canvas c, float vx0, float vy0, float vx1, float vy1) {
        for (int i = 0, n = gv.world.animals.size(); i < n; i++) {
            World.Animal a = gv.world.animals.get(i);
            if (a.x < vx0 - 10 || a.x > vx1 + 10 || a.y < vy0 - 10 || a.y > vy1 + 10) continue;
            if (gv.scale < 0.3f) {
                // (A speck from up here.)
                gv.fill.setColor(a.kind == 0 ? 0xFF8A6238 : 0xFFC8682A);
                c.drawCircle(a.x, a.y, 3, gv.fill);
                continue;
            }
            c.save();
            c.translate(a.x, a.y);
            c.rotate((float) Math.toDegrees(a.angle));
            boolean deer = a.kind == 0;
            gv.fill.setColor(0x33000000);
            gv.oval.set(-4 + 1.5f, -2 + 1.5f, 4 + 1.5f, 2 + 1.5f);
            c.drawOval(gv.oval, gv.fill);
            gv.fill.setColor(deer ? 0xFF8A6238 : 0xFFC8682A);
            gv.oval.set(deer ? -4.5f : -3.5f, deer ? -2 : -1.5f, deer ? 3.5f : 2.5f, deer ? 2 : 1.5f);
            c.drawOval(gv.oval, gv.fill);
            c.drawCircle(deer ? 4.2f : 3.2f, 0, deer ? 1.4f : 1.2f, gv.fill);
            if (deer) {
                gv.fill.setColor(0xFFF2E8D8);
                c.drawCircle(-4.3f, 0, 0.8f, gv.fill);
            } else {
                gv.fill.setColor(0xFFF2E8D8);
                c.drawCircle(-4.2f, 0, 0.9f, gv.fill);
            }
            c.restore();
        }
        for (int i = 0, n = gv.world.birds.size(); i < n; i++) {
            World.Bird b = gv.world.birds.get(i);
            if (b.flying || b.x < vx0 || b.x > vx1 || b.y < vy0 || b.y > vy1) continue;
            gv.fill.setColor(0xFF7D8088);
            c.drawCircle(b.x, b.y, 1.5f, gv.fill);
            gv.fill.setColor(0xFF4C5058);
            c.drawCircle(b.x + (float) Math.cos(b.flap) * 1.2f, b.y + (float) Math.sin(b.flap) * 0.6f, 0.8f, gv.fill);
        }
    }

    /** ((27.3) Step 4.) Everyone on screen (dots when zoomed out), and the selection and orders over them. */
    void drawPeople(Canvas c, float vx0, float vy0, float vx1, float vy1, boolean detailed) {
        float patch = 2.6f * gv.dp / gv.scale;
        // ((33.0) How big a crowd is on screen: the dead in a big one are drawn plainly (EntityRenderer.lite).)
        int many = gv.onScreen.size();
        gv.entityRenderer.crowd = many >= EntityRenderer.GREAT_CROWD ? 2 : many >= EntityRenderer.CROWD ? 1 : 0;
        int pw = 0, ph = 0;
        if (!detailed) {
            pw = (int) ((vx1 - vx0) / patch) + 2;
            ph = (int) ((vy1 - vy0) / patch) + 2;
            if (gv.dotSeen == null || gv.dotSeen.length < pw * ph) gv.dotSeen = new int[pw * ph];
            gv.dotFrame++;
        }
        // ((17.1) The insides of buildings with people in them (and of every building, zoomed right in): floors, rooms
        // and furniture, under the people walking about in them.)
        // ((17.2) Who holds which part of the city in a gang war.)
        // ((32.9) No building interiors drawn: rooms and furniture under every roof, zoomed in, for nothing much to see.)
        // ((14.9) Mortar pits under their teams.)
        if (detailed)
            for (int i = 0, n = gv.onScreen.size(); i < n; i++) {
                Entity e = gv.onScreen.get(i);
                if (e.role != Entity.ROLE_MORTAR || e.type != Entity.SOLDIER || e.task != Dispatch.T_POST) continue;
                if (e.postX < vx0 - 20 || e.postX > vx1 + 20 || e.postY < vy0 - 20 || e.postY > vy1 + 20) continue;
                drawMortarPit(c, e);
            }
        // ((33.0) In a crowd, close in, the dead and the civilians first and the police and the army over them: one
        // officer in a horde of hundreds was lost under it.)
        int passes = detailed && gv.entityRenderer.crowd > 0 ? 2 : 1;
        for (int pass = 0; pass < passes; pass++)
        for (int i = 0, n = gv.onScreen.size(); i < n; i++) {
            Entity e = gv.onScreen.get(i);
            if (e.x < vx0 || e.x > vx1 || e.y < vy0 || e.y > vy1) continue;
            if (passes == 2 && (e.isUndead() || e.type == Entity.CIVILIAN) != (pass == 0)) continue;
            if (detailed && e.job == Entity.J_FISHER && e.jobStep == 1 && e.type == Entity.CIVILIAN && !e.aware) {
                // A line out over the water, the float bobbing.
                float ca = (float) Math.cos(e.angle), sa = (float) Math.sin(e.angle);
                float tipX = e.x + ca * 13, tipY = e.y + sa * 13;
                gv.stroke.setColor(0xFF4A3A2A);
                gv.stroke.setStrokeWidth(0.8f);
                c.drawLine(e.x + ca * 3, e.y + sa * 3, tipX, tipY, gv.stroke);
                float bob = (float) Math.sin(gv.world.time * 2.2f + e.nameSeed) * 0.6f;
                float fx = e.x + ca * 22, fy = e.y + sa * 22 + bob;
                gv.stroke.setColor(0x90E8E8E8);
                gv.stroke.setStrokeWidth(0.4f);
                c.drawLine(tipX, tipY, fx, fy, gv.stroke);
                gv.fill.setColor(0xFFE83A2A);
                c.drawCircle(fx, fy, 1.1f, gv.fill);
            }
            // ((27.0) Up on a roof: drawn there, over the building (drawOverwatch).)
            if (e.roof != null && e.onTower) continue;
            if (detailed) {
                gv.entityRenderer.drawEntity(c, e, gv.settings.healthBars());
                if (e.type == Entity.COP && e.role == Entity.ROLE_TRAFFIC) gv.entityRenderer.drawTrafficKit(c, e);
            } else {
                // Zoomed out: a dot in the colour of their side (the same colours as the counts), outlined so it
                // stands out on any ground, and never smaller than a few pixels.
                int side = e.isZombie() ? 3 : e.type == Entity.COP ? 1 : e.type == Entity.SOLDIER ? 2 : 0;
                if (e != gv.world.controlled && e != gv.follow) {
                    int cell = (int) ((e.y - vy0) / patch) * pw + (int) ((e.x - vx0) / patch);
                    int mark = (gv.dotFrame << 8) | (1 << side);
                    int seen = gv.dotSeen[cell];
                    if ((seen >>> 8) == (gv.dotFrame & 0xFFFFFF) && (seen & (1 << side)) != 0) continue;
                    gv.dotSeen[cell] = (seen >>> 8) == (gv.dotFrame & 0xFFFFFF) ? seen | (1 << side) : mark;
                }
                // ((11.5) Gathered up and drawn below, each side's dots in one go, not two circles for each person.)
                boolean civ = side == 0 && e.type == Entity.CIVILIAN;
                int k = side * 2 + (civ ? 1 : 0);
                float[] b = gv.dotBuf[k];
                if (b == null || gv.dotN[k] + 2 > b.length) gv.dotBuf[k] = b = java.util.Arrays.copyOf(b == null ? new float[256] : b, Math.max(256, gv.dotN[k] * 2));
                b[gv.dotN[k]++] = e.x;
                b[gv.dotN[k]++] = e.y;
            }
        }
        if (!detailed) {
            // (Civilians are the crowd, not the story: smaller, see-through, with a faint edge.)
            float base = Math.max(4 * 1.35f, 2.6f * gv.dp / gv.scale);
            // ((33.0) The dead's dots first, then civilians, then the police and the army on top: drawn last, a horde
            // covered the units fighting it, and from far out the police and the army seemed to have gone.)
            for (int kk = 0; kk < gv.dotBuf.length; kk++) {
                int k = (kk + 6) % 8;
                if (gv.dotN[k] == 0) continue;
                boolean civ = (k & 1) == 1;
                float r = civ ? base * 0.7f : base;
                gv.dotPaint.setColor(civ ? 0x60101010 : 0xD0101010);
                gv.dotPaint.setStrokeWidth(2 * (r + (civ ? 0.6f : 1.1f) * gv.dp / gv.scale));
                c.drawPoints(gv.dotBuf[k], 0, gv.dotN[k], gv.dotPaint);
                int col = ROW_COLORS[k / 2];
                gv.dotPaint.setColor(civ ? (col & 0x00FFFFFF) | 0x99000000 : col);
                gv.dotPaint.setStrokeWidth(2 * r);
                c.drawPoints(gv.dotBuf[k], 0, gv.dotN[k], gv.dotPaint);
                gv.dotN[k] = 0;
            }
        }
        if (gv.boxing) {
            gv.fill.setColor(0x22FFD24A);
            c.drawRect(Math.min(gv.boxX0, gv.boxX1), Math.min(gv.boxY0, gv.boxY1), Math.max(gv.boxX0, gv.boxX1), Math.max(gv.boxY0, gv.boxY1), gv.fill);
            gv.stroke.setColor(0xCCFFD24A);
            gv.stroke.setStrokeWidth(1.2f);
            c.drawRect(Math.min(gv.boxX0, gv.boxX1), Math.min(gv.boxY0, gv.boxY1), Math.max(gv.boxX0, gv.boxX1), Math.max(gv.boxY0, gv.boxY1), gv.stroke);
        }
        for (int i = 0, n = gv.selection.size(); i < n; i++) {
            Entity e = gv.selection.get(i);
            gv.stroke.setColor(0xFFFFD24A);
            gv.stroke.setStrokeWidth(1.2f);
            c.drawCircle(e.x, e.y, e.radius + 4, gv.stroke);
        }
        if (gv.orderMarker > 0) {
            gv.stroke.setColor(alpha(0xFFFFD24A, Math.min(1, gv.orderMarker)));
            gv.stroke.setStrokeWidth(1.5f);
            c.drawCircle(gv.orderX, gv.orderY, 6 + (1.5f - gv.orderMarker) * 14, gv.stroke);
        }
    }

    /** ((27.3) Step 5.) Crossings and signals, noise rings, the field hospital, the recovery, a quarantine's exits and an air strike's target. */
    void drawMarkings(Canvas c, float vx0, float vy0, float vx1, float vy1) {
        drawCrossings(c, vx0, vx1, vy0, vy1);
        drawSignals(c, vx0, vx1, vy0, vy1);
        // What the dead can hear: rings spreading out from gunfire, alarms, horns and screams.
        gv.stroke.setStrokeWidth(1.5f / Math.max(0.5f, gv.scale) * 2);
        for (int i = 0, n = gv.world.noiseRings.size(); i < n; i++) {
            float[] r = gv.world.noiseRings.get(i);
            float t = r[3] / 1.4f, rad = r[2] * Math.min(1, t * 1.6f);
            if (r[0] + rad < vx0 || r[0] - rad > vx1 || r[1] + rad < vy0 || r[1] - rad > vy1) continue;
            gv.stroke.setColor(alpha(0xFFFFE8B0, 0.35f * (1 - t)));
            c.drawCircle(r[0], r[1], rad, gv.stroke);
        }
        if (!Float.isNaN(gv.world.hospitalX) && gv.world.hospitalX > vx0 - 30 && gv.world.hospitalX < vx1 + 30 && gv.world.hospitalY > vy0 - 30 && gv.world.hospitalY < vy1 + 30) {
            // ((14.5) The field hospital: a big white tent with a red cross on the roof.)
            float hx = gv.world.hospitalX, hy = gv.world.hospitalY;
            gv.fill.setColor(0x50000000);
            c.drawRect(hx - 13, hy - 8, hx + 15, hy + 11, gv.fill);
            gv.fill.setColor(0xFFEDEDE6);
            c.drawRect(hx - 14, hy - 10, hx + 14, hy + 9, gv.fill);
            gv.fill.setColor(0xFFD2D2CA);
            c.drawRect(hx - 14, hy - 10, hx + 14, hy - 7, gv.fill);
            gv.fill.setColor(0xFFD83A3A);
            c.drawRect(hx - 5, hy - 1.5f, hx + 5, hy + 1.5f, gv.fill);
            c.drawRect(hx - 1.5f, hy - 5, hx + 1.5f, hy + 5, gv.fill);
        }
        if (gv.world.aftermath == World.AFTER_RECOVERY) {
            // ((14.2) A fallen building being rebuilt: the site fenced off, scaffolding going up.)
            for (int i = 0, n = gv.world.city.buildings.size(); i < n; i++) {
                City.Building b = gv.world.city.buildings.get(i);
                if (b.rebuildT <= 0 || b.x1 < vx0 || b.x0 > vx1 || b.y1 < vy0 || b.y0 > vy1) continue;
                float done = 1 - b.rebuildT / World.REBUILD_TIME;
                gv.stroke.setStrokeWidth(1.6f);
                gv.stroke.setColor(0xFF8C6E3A);
                float sx0 = b.x0 + 2, sy0 = b.y0 + 2, sx1 = b.x1 - 2, sy1 = b.y1 - 2;
                for (float x = sx0; x <= sx1; x += 8) c.drawLine(x, sy0, x, sy0 + (sy1 - sy0) * done, gv.stroke);
                c.drawLine(sx0, sy0, sx1, sy0 + (sy1 - sy0) * done, gv.stroke);
                c.drawLine(sx1, sy0, sx0, sy0 + (sy1 - sy0) * done, gv.stroke);
                gv.stroke.setStrokeWidth(1.4f);
                gv.stroke.setColor(0xFFF2C21C);
                c.drawRect(b.x0 - 1, b.y0 - 1, b.x1 + 1, b.y1 + 1, gv.stroke);
                gv.stroke.setColor(0xFF1C1C1C);
                for (float x = b.x0; x < b.x1; x += 6) {
                    c.drawLine(x, b.y0 - 1, Math.min(b.x1, x + 3), b.y0 - 1, gv.stroke);
                    c.drawLine(x, b.y1 + 1, Math.min(b.x1, x + 3), b.y1 + 1, gv.stroke);
                }
            }
        }
        float[][] exits = gv.world.quarantined ? gv.world.exits() : null;
        if (exits != null) {
            // ((14.2) The city's quarantine: a red and white barrier across each road out, and a red ring round it.)
            for (float[] q : exits) {
                if (q[0] < vx0 - 40 || q[0] > vx1 + 40 || q[1] < vy0 - 40 || q[1] > vy1 + 40) continue;
                gv.stroke.setColor(0xC0E0302A);
                gv.stroke.setStrokeWidth(2.5f);
                c.drawCircle(q[0], q[1], 30, gv.stroke);
                for (int k = -3; k < 3; k++) {
                    gv.fill.setColor(k % 2 == 0 ? 0xFFE0302A : 0xFFF2F2F2);
                    c.drawRect(q[0] + k * 5, q[1] - 2, q[0] + k * 5 + 5, q[1] + 2, gv.fill);
                }
            }
        }
        if (gv.world.firebombTime > 0) {
            // The target area, pulsing.
            float pulse = 0.5f + 0.5f * (float) Math.sin(gv.world.time * 6);
            gv.fill.setColor(alpha(0xFFFF2A1A, 0.08f + 0.08f * pulse));
            c.drawCircle(gv.world.firebombX, gv.world.firebombY, 230, gv.fill);
            gv.stroke.setColor(alpha(0xFFFF3A2A, 0.6f + 0.4f * pulse));
            gv.stroke.setStrokeWidth(3f);
            c.drawCircle(gv.world.firebombX, gv.world.firebombY, 230, gv.stroke);
        }
    }

    /** ((27.3) Step 6.) The vehicles on the ground, and the bridges and overpasses over them. */
    void drawVehicles(Canvas c, float vx0, float vy0, float vx1, float vy1, boolean detailed) {
        for (int i = 0, n = gv.world.fleet.vehicles.size(); i < n; i++) {
            Fleet.Vehicle v = gv.world.fleet.vehicles.get(i);
            if (v.broken || Fleet.airborne(v)) continue;
            if (!v.lightsOn() || v.x < vx0 - 30 || v.x > vx1 + 30 || v.y < vy0 - 30 || v.y > vy1 + 30) continue;
            boolean blink = ((int) (v.anim * 8)) % 2 == 0;
            int col = blink ? 0xFFFF3A30 : 0xFF3A7BFF;
            float side = blink ? -1 : 1, ox = (float) -Math.sin(v.angle) * side * 4, oy = (float) Math.cos(v.angle) * side * 4;
            for (int k = 0; k < 3; k++) {
                gv.fill.setColor(alpha(col, 0.07f));
                c.drawCircle(v.x + ox, v.y + oy, 10 + k * 7, gv.fill);
            }
        }
        for (int i = 0, n = gv.world.fleet.vehicles.size(); i < n; i++) {
            Fleet.Vehicle v = gv.world.fleet.vehicles.get(i);
            if (Fleet.airborne(v)) continue;
            // Trains are long, so they get a wider margin before they count as off screen.
            float m = v.type == Fleet.TRAIN ? Fleet.TRAIN_LENGTH + 20 : 20;
            if (v.x < vx0 - m || v.x > vx1 + m || v.y < vy0 - m || v.y > vy1 + m) continue;
            gv.vehicleRenderer.drawVehicle(c, v);
            if (v.cones) {
                // A line of traffic cones across the road in front of the roadblock.
                boolean vertical = v.blockDir == 1;
                for (int k = -2; k <= 2; k++) {
                    float cx = v.x + (vertical ? k * 6 : 14), cy = v.y + (vertical ? 14 : k * 6);
                    gv.fill.setColor(0x50000000);
                    c.drawCircle(cx + 0.6f, cy + 0.8f, 2f, gv.fill);
                    gv.fill.setColor(0xFFFF7A1A);
                    c.drawCircle(cx, cy, 1.9f, gv.fill);
                    gv.fill.setColor(0xFFF2F2F2);
                    c.drawCircle(cx, cy, 1.1f, gv.fill);
                    gv.fill.setColor(0xFFFF7A1A);
                    c.drawCircle(cx, cy, 0.6f, gv.fill);
                }
            }
            if (v.barX0 != v.barX1 || v.barY0 != v.barY1) drawBarriers(c, v);
        }
        drawRailBridge(c, vx0, vx1, vy0, vy1);
        drawOverpasses(c, vx0, vx1, vy0, vy1);
        // ((28.5) Zoomed out, the police's cars and the army's vehicles carry the dot of their side too, the same as
        // the officers and soldiers on foot: they were specks, easy to lose, while everyone out of them was a dot.)
        // ((29.2) Not a dot like the people on foot: a little car, pointing the way it's going, in its side's colour but
        // darker, so a unit's vehicle and its people on foot are told apart at a glance.)
        if (!detailed) {
            float r = Math.max(4 * 1.35f, 2.6f * gv.dp / gv.scale), edge = 1.1f * gv.dp / gv.scale;
            float hl = r * 1.25f, hw = r * 0.75f;
            for (int i = 0, n = gv.world.fleet.vehicles.size(); i < n; i++) {
                Fleet.Vehicle v = gv.world.fleet.vehicles.get(i);
                if (v.broken || Fleet.airborne(v) || v.x < vx0 - 20 || v.x > vx1 + 20 || v.y < vy0 - 20 || v.y > vy1 + 20) continue;
                int side = unitSide(v);
                if (side < 0) continue;
                c.save();
                c.translate(v.x, v.y);
                c.rotate((float) Math.toDegrees(v.angle));
                gv.fill.setColor(0xD0101010);
                tmp.set(-hl - edge, -hw - edge, hl + edge, hw + edge);
                c.drawRoundRect(tmp, hw * 0.6f, hw * 0.6f, gv.fill);
                gv.fill.setColor(City.darken(ROW_COLORS[side], 0.62f));
                tmp.set(-hl, -hw, hl, hw);
                c.drawRoundRect(tmp, hw * 0.5f, hw * 0.5f, gv.fill);
                // The windscreen, towards the front.
                gv.fill.setColor(0xD0DCE6F0);
                tmp.set(hl * 0.25f, -hw * 0.7f, hl * 0.62f, hw * 0.7f);
                c.drawRect(tmp, gv.fill);
                c.restore();
            }
        }

        if (detailed) {
            for (int i = 0, n = gv.onScreen.size(); i < n; i++) {
                Entity e = gv.onScreen.get(i);
                if (e.phoneTimer <= 0 && e.talkTimer <= 0) continue;
                if (e.x < vx0 || e.x > vx1 || e.y < vy0 || e.y > vy1) continue;
                gv.entityRenderer.drawSpeechIcon(c, e);
            }
        }
    }

    /**
     * ((33.0) The brush's reach, for a moment after it's put people down: a ring the size it spreads them (10, 50 or
     * 100), so it's clear where they went and how big the brush is.
     */
    void drawBrushRing(Canvas c) {
        if (gv.brushT <= 0) return;
        gv.brushT -= gv.frameDt;
        float a = Math.max(0, Math.min(1, gv.brushT / 0.6f));
        gv.stroke.setStrokeWidth(1.6f * gv.dp / gv.scale);
        gv.stroke.setColor(alpha(0xFFFFFFFF, 0.55f * a));
        c.drawCircle(gv.brushX, gv.brushY, gv.brushR, gv.stroke);
        gv.fill.setColor(alpha(0xFFFFFFFF, 0.08f * a));
        c.drawCircle(gv.brushX, gv.brushY, gv.brushR, gv.fill);
    }

    /** ((28.5) Which side's dot a vehicle carries zoomed out: 1 the police's, 2 the army's, -1 none. */
    private final android.graphics.RectF tmp = new android.graphics.RectF();

    static int unitSide(Fleet.Vehicle v) {
        if (v.type == Fleet.CRUISER || Fleet.policeTruck(v) || (v.type == Fleet.BOAT && v.kind == Fleet.K_POLICE_BOAT)) return 1;
        if (v.type == Fleet.TANK || (v.type == Fleet.AMBULANCE && v.kind == Fleet.K_ARMD_AMB)) return 2;
        if (v.type == Fleet.TRUCK) return v.passengerType == Entity.COP ? 1 : 2;
        return -1;
    }

    /** ((27.3) Step 7.) Grenades in the air, particles and tracers. */
    void drawShots(Canvas c, float vx0, float vy0, float vx1, float vy1) {
        for (int i = 0, n = gv.world.grenades.size(); i < n; i++) {
            World.Grenade g = gv.world.grenades.get(i);
            float t = g.t / g.dur;
            float x = g.sx + (g.tx - g.sx) * t, y = g.sy + (g.ty - g.sy) * t;
            float hgt = (float) Math.sin(t * Math.PI) * (g.shell ? 90 : 22);
            if (g.shell) {
                // ((14.9) Where a mortar shell will come down, marked while it's in the air: a red ring closing in
                // and cross hairs, and the shell's trail behind it.)
                float left = 1 - t, pulse = 0.5f + 0.5f * (float) Math.sin(t * 40);
                gv.stroke.setStrokeWidth(1.4f);
                gv.stroke.setColor(alpha(0xFFE8392C, 0.5f + 0.35f * pulse));
                c.drawCircle(g.aimX, g.aimY, World.MORTAR_BLAST * (0.35f + 0.65f * left), gv.stroke);
                gv.stroke.setStrokeWidth(0.9f);
                gv.stroke.setColor(alpha(0xFFE8392C, 0.75f));
                c.drawLine(g.aimX - 7, g.aimY, g.aimX + 7, g.aimY, gv.stroke);
                c.drawLine(g.aimX, g.aimY - 7, g.aimX, g.aimY + 7, gv.stroke);
                for (int k = 1; k <= 5; k++) {
                    float tt = Math.max(0, t - k * 0.025f);
                    float tx = g.sx + (g.tx - g.sx) * tt, ty = g.sy + (g.ty - g.sy) * tt - (float) Math.sin(tt * Math.PI) * 90;
                    gv.fill.setColor(alpha(0xFFDDD8CC, 0.35f - k * 0.06f));
                    c.drawCircle(tx, ty, 1.6f + k * 0.4f, gv.fill);
                }
            }
            gv.fill.setColor(0x55000000);
            c.drawCircle(x, y, g.shell ? 2.4f : 1.6f, gv.fill);
            gv.fill.setColor(0xFF2F3A1E);
            c.drawCircle(x, y - hgt, g.shell ? 2.6f : 1.9f, gv.fill);
        }


        for (int i = 0; i < World.MAXP; i++) {
            float life = gv.world.plife[i];
            if (life <= 0) continue;
            float x = gv.world.px[i], y = gv.world.py[i];
            if (x < vx0 || x > vx1 || y < vy0 || y > vy1) continue;
            // ((21.3) Flames were drawn here, over everyone running through them: now under them (drawGroundFire).)
            if (gv.world.ptype[i] == World.P_FIRE) continue;
            float f = life / gv.world.pmax[i];
            int col = gv.world.pcol[i];
            float size = gv.world.psize[i];
            switch (gv.world.ptype[i]) {
                case World.P_SMOKE:
                    gv.fill.setColor(alpha(col, 0.4f * f));
                    break;
                case World.P_FIRE:
                    gv.fill.setColor(alpha(col, f));
                    size *= 0.4f + f * 0.6f;
                    break;
                case World.P_FLASH:
                    // The flash lights up the ground around the muzzle.
                    // ((14.4) Brighter and wider, with a white-hot core.)
                    gv.fill.setColor(alpha(0xFFFFD27A, 0.2f * f + 0.06f));
                    c.drawCircle(x, y, size * 4.2f, gv.fill);
                    gv.fill.setColor(alpha(col, 0.95f));
                    c.drawCircle(x, y, size * 1.15f, gv.fill);
                    gv.fill.setColor(alpha(0xFFFFFFFF, 0.9f * f));
                    size *= 0.5f;
                    break;
                case World.P_CASING:
                    // ((14.4) A little bigger, catching the light as it tumbles.)
                    gv.fill.setColor(alpha((int) (life * 40) % 2 == 0 ? col : 0xFFFFE8A0, Math.min(1, f * 3)));
                    c.drawRect(x - 0.65f, y - 0.32f, x + 0.65f, y + 0.32f, gv.fill);
                    continue;
                case World.P_DEBRIS:
                    gv.fill.setColor(alpha(col, Math.min(1, f * 2)));
                    c.drawRect(x - size, y - size * 0.7f, x + size, y + size * 0.7f, gv.fill);
                    continue;
                default:
                    gv.fill.setColor(alpha(col, Math.min(1, f * 2)));
                    break;
            }
            c.drawCircle(x, y, size, gv.fill);
        }

        // ((14.4) A tracer: a faint glow along the shot and a bright streak racing from the muzzle to where it hits.)
        for (int i = 0; i < World.MAXT; i++) {
            float life = gv.world.tlife[i];
            if (life <= 0) continue;
            float f = life / World.TRACER_LIFE, p = 1 - f;
            float x0 = gv.world.tx0[i], y0 = gv.world.ty0[i], x1 = gv.world.tx1[i], y1 = gv.world.ty1[i];
            // ((19.5) Every tracer's glow a little wider on the higher effects settings.)
            gv.stroke.setStrokeWidth(gv.vfx.quality >= 2 ? 2.4f : 1.6f);
            gv.stroke.setColor(alpha(0xFFFFC860, 0.22f * f));
            c.drawLine(x0, y0, x1, y1, gv.stroke);
            float hx = x0 + (x1 - x0) * Math.min(1, p * 1.4f), hy = y0 + (y1 - y0) * Math.min(1, p * 1.4f);
            float tl = Math.max(0, Math.min(1, p * 1.4f) - 0.35f);
            float sx = x0 + (x1 - x0) * tl, sy = y0 + (y1 - y0) * tl;
            gv.stroke.setStrokeWidth(1.2f);
            gv.stroke.setColor(alpha(0xFFFFF0B0, Math.min(1, f * 1.6f)));
            c.drawLine(sx, sy, hx, hy, gv.stroke);
        }
    }

    /** ((27.3) Step 9, after the buildings.) Helicopters and planes, and the birds in the air. */
    void drawAircraft(Canvas c, float vx0, float vy0, float vx1, float vy1) {
        for (int i = 0, n = gv.world.fleet.vehicles.size(); i < n; i++) {
            Fleet.Vehicle v = gv.world.fleet.vehicles.get(i);
            if (v.type == Fleet.HELI) gv.vehicleRenderer.drawHeli(c, v);
            else if (v.type == Fleet.JET) gv.vehicleRenderer.drawJet(c, v);
        }
        for (int i = 0, n = gv.world.birds.size(); i < n; i++) {
            World.Bird b = gv.world.birds.get(i);
            if (!b.flying || b.x < vx0 || b.x > vx1 || b.y < vy0 || b.y > vy1) continue;
            float wing = (float) Math.sin(b.flap) * 2.2f;
            gv.fill.setColor(0x30000000);
            c.drawCircle(b.x + 10, b.y + 14, 1.4f, gv.fill);
            gv.stroke.setColor(0xFF6E717A);
            gv.stroke.setStrokeWidth(0.9f);
            c.drawLine(b.x - 2.5f, b.y - wing, b.x, b.y, gv.stroke);
            c.drawLine(b.x, b.y, b.x + 2.5f, b.y - wing, gv.stroke);
        }
    }

    /** ((27.3) Step 10.) The marks over it all: infection, calls, call-outs, whoever is being followed, and the hazards. */
    void drawMarkers(Canvas c) {
        for (int i = 0, n = gv.world.infection.size(); i < n; i++) {
            World.InfectionZone z = gv.world.infection.get(i);
            // ((11.0) Its quarantine wall: concrete barriers with a chain-link fence along the top.)
            float wx0 = gv.camX - 20, wy0 = gv.camY - 20, wx1 = gv.camX + gv.getWidth() / gv.scale + 20, wy1 = gv.camY + gv.getHeight() / gv.scale + 20;
            int cw = gv.world.city.w;
            for (int k = 0, m = z.wall.size(); k < m; k++) {
                int t = z.wall.get(k)[0];
                float x = (t % cw) * City.T, y = (t / cw) * City.T;
                if (x > wx1 || y > wy1 || x + City.T < wx0 || y + City.T < wy0) continue;
                gv.fill.setColor(0xFF9A9A92);
                c.drawRect(x + 1, y + 1, x + City.T - 1, y + City.T - 1, gv.fill);
                gv.fill.setColor(0xFF6E6E68);
                c.drawRect(x + 1, y + City.T * 0.62f, x + City.T - 1, y + City.T - 1, gv.fill);
                gv.stroke.setStrokeWidth(0.8f);
                gv.stroke.setColor(0xFFD8D8D0);
                c.drawLine(x + 1, y + 2, x + City.T - 1, y + 2, gv.stroke);
                c.drawLine(x + City.T * 0.5f, y + 1, x + City.T * 0.5f, y + City.T - 1, gv.stroke);
            }
            int col = z.critical ? 0xFFFF4A3A : 0xFFFFB040;
            gv.fill.setColor(alpha(col, 0.06f));
            c.drawCircle(z.x, z.y, z.r, gv.fill);
            gv.stroke.setStrokeWidth(2.4f / Math.max(0.4f, gv.scale) + 0.4f);
            gv.stroke.setColor(alpha(col, 0.8f));
            int segs = Math.max(40, (int) (z.r / 5)) / 2 * 2;
            float spin = gv.world.time * 0.04f;
            for (int k = 0; k < segs; k += 2) {
                float a0 = 6.2832f * k / segs + spin, a1 = 6.2832f * (k + 1) / segs + spin;
                c.drawLine(z.x + (float) Math.cos(a0) * z.r, z.y + (float) Math.sin(a0) * z.r,
                        z.x + (float) Math.cos(a1) * z.r, z.y + (float) Math.sin(a1) * z.r, gv.stroke);
            }
        }
        for (int i = 0, n = gv.world.dispatch.incidents.size(); i < n; i++) {
            Dispatch.Incident inc = gv.world.dispatch.incidents.get(i);
            float pulse = (gv.world.time * 1.5f + i * 0.3f) % 1f;
            gv.stroke.setStrokeWidth(2f / Math.max(0.5f, gv.scale) + 0.6f);
            gv.stroke.setColor(alpha(0xFFFF4A3A, 1 - pulse));
            c.drawCircle(inc.x, inc.y, 8 + pulse * 18, gv.stroke);
            gv.stroke.setColor(0xCCFF4A3A);
            c.drawCircle(inc.x, inc.y, 5, gv.stroke);
        }
        // ((10.60) Who's on their way to a call. No more straight lines across the rooftops from each car and truck:
        // a badge over each call with how many police and soldiers are coming, a thin ring in their colour round it,
        // and for whoever you're following, the way they'll actually go, faintly, along the streets.)
        drawCallBadges(c);
        // Calls for help on the radio: rings going out from where the call was made.
        for (int i = 0, n = gv.world.dispatch.callouts.size(); i < n; i++) {
            Dispatch.Callout co = gv.world.dispatch.callouts.get(i);
            for (int k = 0; k < 3; k++) {
                float ring = (co.age * 0.9f + k / 3f) % 1f;
                gv.stroke.setStrokeWidth(1.6f / Math.max(0.5f, gv.scale) + 0.4f);
                gv.stroke.setColor(alpha(co.color, (1 - ring) * Math.max(0, 1 - co.age / 6f)));
                c.drawCircle(co.x, co.y, 6 + ring * 34, gv.stroke);
            }
        }

        if (gv.follow != null) {
            gv.stroke.setColor(0xCCFFFFFF);
            gv.stroke.setStrokeWidth(1f);
            c.drawCircle(gv.follow.x, gv.follow.y, gv.follow.radius + 4 + (float) Math.sin(gv.world.time * 6), gv.stroke);
        }
        if (gv.followV != null) {
            gv.stroke.setColor(0xCCFFFFFF);
            gv.stroke.setStrokeWidth(1.2f);
            c.drawCircle(gv.followV.x, gv.followV.y, gv.followV.length() + 5 + (float) Math.sin(gv.world.time * 6), gv.stroke);
        }

        drawHazards(c);
    }

    /** ((27.3) From drawMarkers.) Over each call, how many police and soldiers are coming, and the way the one followed will go. */
    void drawCallBadges(Canvas c) {
        {
            float lx0 = gv.camX - 60, ly0 = gv.camY - 60, lx1 = gv.camX + gv.getWidth() / gv.scale + 60, ly1 = gv.camY + gv.getHeight() / gv.scale + 60;
            // ((11.5) Counted twice a second, not every frame: it means going through everyone.)
            if (gv.world.time - gv.inboundAt > 0.5f || gv.world.time < gv.inboundAt) {
                gv.inboundAt = gv.world.time;
                gv.inbound.clear();
                for (int i = 0, n = gv.world.entities.size(); i < n; i++) {
                    Entity e = gv.world.entities.get(i);
                    if (e.dead || e.task != Dispatch.T_RESPOND || e.incident == null || e.onScene || e.incident.resolved) continue;
                    gv.tally(e.incident, e.type == Entity.SOLDIER, 1);
                }
                for (int i = 0, n = gv.world.fleet.vehicles.size(); i < n; i++) {
                    Fleet.Vehicle v = gv.world.fleet.vehicles.get(i);
                    Dispatch.Incident inc = v.incident;
                    if (inc == null || inc.resolved || v.broken || (v.state != Fleet.DRIVE && v.state != Fleet.MUSTER)) continue;
                    int aboard = v.crew.size() + v.passengers;
                    if (aboard > 0) gv.tally(inc, Fleet.army(v) || v.passengerType == Entity.SOLDIER, aboard);
                }
            }
            float u = 1f / Math.max(0.35f, gv.scale);
            gv.text.setTextSize(10.5f * gv.dp * u);
            gv.text.setTextAlign(Paint.Align.LEFT);
            for (java.util.Map.Entry<Dispatch.Incident, int[]> en : gv.inbound.entrySet()) {
                Dispatch.Incident inc = en.getKey();
                int[] k = en.getValue();
                // (A lone officer on the way needs no badge: only calls with a response worth showing.)
                if (k[0] + k[1] < 2 || inc.resolved || inc.x < lx0 || inc.x > lx1 || inc.y < ly0 || inc.y > ly1) continue;
                int col = k[1] > k[0] ? 0xFFA8E070 : 0xFF70B0FF;
                gv.stroke.setStrokeWidth(1.4f * u);
                gv.stroke.setColor(alpha(col, 0.45f + 0.25f * (float) Math.sin(gv.world.time * 4)));
                c.drawCircle(inc.x, inc.y, 30, gv.stroke);
                // The badge: a dark pill with a blue dot and the police coming, a green one and the soldiers.
                String sp = k[0] > 0 ? String.valueOf(k[0]) : null, ss = k[1] > 0 ? String.valueOf(k[1]) : null;
                float dot = 3.2f * gv.dp * u, gap = 3 * gv.dp * u, padX = 5 * gv.dp * u, hh = 8 * gv.dp * u;
                float wP = sp == null ? 0 : dot * 2 + gap + gv.text.measureText(sp), wS = ss == null ? 0 : dot * 2 + gap + gv.text.measureText(ss);
                float wTot = padX * 2 + wP + wS + (sp != null && ss != null ? 6 * gv.dp * u : 0);
                float bx = inc.x - wTot / 2, by = inc.y - 30 - 6 * gv.dp * u - hh * 2;
                gv.fill.setColor(0xD0101418);
                gv.badgeRect.set(bx, by, bx + wTot, by + hh * 2);
                c.drawRoundRect(gv.badgeRect, hh, hh, gv.fill);
                float x = bx + padX, cy = by + hh;
                if (sp != null) {
                    gv.fill.setColor(0xFF70B0FF);
                    c.drawCircle(x + dot, cy, dot, gv.fill);
                    gv.text.setColor(0xFFE8F0FF);
                    c.drawText(sp, x + dot * 2 + gap, cy + gv.text.getTextSize() * 0.36f, gv.text);
                    x += wP + 6 * gv.dp * u;
                }
                if (ss != null) {
                    gv.fill.setColor(0xFFA8E070);
                    c.drawCircle(x + dot, cy, dot, gv.fill);
                    gv.text.setColor(0xFFEFFFE0);
                    c.drawText(ss, x + dot * 2 + gap, cy + gv.text.getTextSize() * 0.36f, gv.text);
                }
            }
            // The followed one's way there.
            if (gv.follow != null && !gv.follow.dead && gv.follow.task == Dispatch.T_RESPOND && gv.follow.incident != null && !gv.follow.onScene
                    && gv.follow.path != null && gv.follow.path.length > 0) {
                int col = gv.follow.type == Entity.SOLDIER ? 0xFFA8E070 : 0xFF70B0FF;
                drawRoute(c, gv.follow.x, gv.follow.y, gv.follow.path, gv.follow.pathIdx, gv.follow.path.length, col, u);
            }
            if (gv.followV != null && gv.followV.state == Fleet.DRIVE && gv.followV.field != null) {
                int n = gv.world.fleet.traceRoute(gv.followV, gv.routeBuf);
                int col = Fleet.army(gv.followV) ? 0xFFA8E070 : 0xFF70B0FF;
                drawRoute(c, gv.followV.x, gv.followV.y, gv.routeBuf, 0, n, col, u);
            }
        }
    }

    /** ((27.3) Step 11.) Drones, and the explosions over everything. */
    void drawDronesAndBlasts(Canvas c) {
        for (int i = 0, n = gv.world.drones.size(); i < n; i++) {
            World.Drone d = gv.world.drones.get(i);
            boolean up = d.state != World.DRONE_DOCKED;
            if (up) {
                gv.fill.setColor(0x10A0C8FF);
                c.drawCircle(d.x, d.y, World.DRONE_SEE, gv.fill);
                gv.fill.setColor(0x40000000);
                c.drawCircle(d.x + 6, d.y + 9, 3.2f, gv.fill);
            }
            float h = 0, r = 2.6f;
            gv.fill.setColor(0xFF2A2F36);
            c.drawRect(d.x - 1.6f, d.y - 1.6f - h, d.x + 1.6f, d.y + 1.6f - h, gv.fill);
            gv.stroke.setStrokeWidth(0.7f);
            gv.stroke.setColor(0xFF2A2F36);
            c.drawLine(d.x - r, d.y - r, d.x + r, d.y + r, gv.stroke);
            c.drawLine(d.x - r, d.y + r, d.x + r, d.y - r, gv.stroke);
            gv.fill.setColor(up ? 0x70C8D0D8 : 0xFF6A7078);
            for (int k = 0; k < 4; k++) c.drawCircle(d.x + (k % 2 == 0 ? -r : r), d.y + (k < 2 ? -r : r), up ? 1.7f : 1f, gv.fill);
            boolean blink = ((int) (gv.world.time * 3 + i)) % 2 == 0;
            gv.fill.setColor(blink ? 0xFFFF3030 : 0xFF3060FF);
            c.drawCircle(d.x, d.y, 0.8f, gv.fill);
        }

        for (int i = 0, n = gv.world.explosions.size(); i < n; i++) {
            World.Explosion ex = gv.world.explosions.get(i);
            float t = 1 - ex.life / ex.max;
            float r = ex.r * (0.3f + 0.9f * t);
            // ((14.4) The first instant: a white flash lighting up the street well beyond the blast.)
            if (t < 0.35f) {
                float ft = 1 - t / 0.35f;
                gv.fill.setColor(alpha(0xFFFFF6D8, 0.3f * ft));
                c.drawCircle(ex.x, ex.y, ex.r * 2.4f, gv.fill);
            }
            gv.fill.setColor(alpha(0xFFFF8A20, (1 - t) * 0.8f));
            c.drawCircle(ex.x, ex.y, r, gv.fill);
            gv.fill.setColor(alpha(0xFFFFF2B0, (1 - t)));
            c.drawCircle(ex.x, ex.y, r * 0.5f, gv.fill);
            gv.stroke.setStrokeWidth(2f);
            gv.stroke.setColor(alpha(0xFFFFFFFF, (1 - t) * 0.6f));
            c.drawCircle(ex.x, ex.y, ex.r * (0.5f + t), gv.stroke);
        }
    }

    /** ((18.4) The police's drones overhead. */
    void drawHazards(Canvas c) {
        float tm = gv.world.time;
        // ((18.4) The police's drones: small and dark with blue and red lights, the ground they watch ringed in blue.)
        for (int i = 0, n = gv.world.policeDrones.size(); i < n; i++) {
            float[] d = gv.world.policeDrones.get(i);
            if (World.hypot(d[0] - d[2], d[1] - d[3]) < 60) {
                gv.stroke.setColor(0x506FA8FF);
                gv.stroke.setStrokeWidth(1.5f);
                c.drawCircle(d[2], d[3], World.WATCH_SEE, gv.stroke);
            }
            gv.fill.setColor(0x50000000);
            c.drawCircle(d[0] + 6, d[1] + 8, 5, gv.fill);
            gv.fill.setColor(0xFF22252A);
            c.drawRect(d[0] - 4, d[1] - 1, d[0] + 4, d[1] + 1, gv.fill);
            c.drawRect(d[0] - 1, d[1] - 4, d[0] + 1, d[1] + 4, gv.fill);
            for (int k = 0; k < 4; k++) {
                float rx = d[0] + (k % 2 == 0 ? -4 : 4), ry = d[1] + (k < 2 ? -4 : 4);
                gv.fill.setColor(0x80C8CCD2);
                c.drawCircle(rx, ry, 2.6f, gv.fill);
            }
            boolean blink = ((int) (tm * 4)) % 2 == 0;
            gv.fill.setColor(blink ? 0xFF4F7BFF : 0xFFFF4040);
            c.drawCircle(d[0], d[1], 1.4f, gv.fill);
        }
    }

    /**
     * A survivor group's defences, growing as they work: boards over the door, then a ring of sandbags
     * round the building (gap at the door), then a plank wall further out.
     */
    /**
     * ((25.0) A survivor base: its ground (the wall is its barricades, drawn with the rest), a watchtower in each
     * corner that's been built, the workshop's bench and crates by the door, and a flag.
     */
    void drawBase(Canvas c, SurvivorBases.Base s, float vx0, float vy0, float vx1, float vy1) {
        float T = City.T;
        float x0 = s.tx0 * T, y0 = s.ty0 * T, x1 = (s.tx1 + 1) * T, y1 = (s.ty1 + 1) * T;
        if (s.territory > 0) {
            // ((27.5) The ground it's cleared round it: a faint ring, red for raiders.)
            float r = BaseRaids.territoryR(s), mx = s.cx(), my = s.cy();
            if (mx + r >= vx0 && mx - r <= vx1 && my + r >= vy0 && my - r <= vy1) {
                gv.fill.setColor(s.raiders ? 0x0CC03020 : 0x0C60A040);
                c.drawCircle(mx, my, r, gv.fill);
                gv.stroke.setColor(s.raiders ? 0x40C03020 : 0x4060A040);
                gv.stroke.setStrokeWidth(1.5f);
                c.drawCircle(mx, my, r, gv.stroke);
            }
        }
        if (x1 < vx0 || x0 > vx1 || y1 < vy0 || y0 > vy1) return;
        gv.fill.setColor(s.raiders ? 0x18C04030 : 0x16C89A40);
        c.drawRect(x0 + T, y0 + T, x1 - T, y1 - T, gv.fill);
        for (int k = 0; k < s.towersUp && k < s.towers.length; k++) {
            float tx = s.towers[k][0], ty = s.towers[k][1];
            // Shadow, four legs, the platform and its rail.
            gv.fill.setColor(0x50000000);
            c.drawRect(tx - 6, ty - 4, tx + 9, ty + 9, gv.fill);
            gv.fill.setColor(0xFF4E3820);
            c.drawRect(tx - 7, ty - 7, tx - 5, ty - 5, gv.fill);
            c.drawRect(tx + 5, ty - 7, tx + 7, ty - 5, gv.fill);
            c.drawRect(tx - 7, ty + 5, tx - 5, ty + 7, gv.fill);
            c.drawRect(tx + 5, ty + 5, tx + 7, ty + 7, gv.fill);
            gv.fill.setColor(0xFF9C7446);
            c.drawRect(tx - 6, ty - 6, tx + 6, ty + 6, gv.fill);
            gv.fill.setColor(0xFFB88E5A);
            for (int q = -5; q <= 4; q += 3) c.drawRect(tx - 5.5f, ty + q, tx + 5.5f, ty + q + 1.6f, gv.fill);
            gv.stroke.setColor(0xFF5A4024);
            gv.stroke.setStrokeWidth(1.2f);
            c.drawRect(tx - 6, ty - 6, tx + 6, ty + 6, gv.stroke);
        }
        City.Building b = s.b;
        float dx = b.doorX, dy = b.doorY;
        float ox = Math.signum(s.cx() - dx), oy = Math.signum(s.cy() - dy);
        if (s.walled) {
            // The workshop: a bench, crates of rounds.
            float wx = dx - oy * 14 - ox * 4, wy = dy + ox * 14 - oy * 4;
            gv.fill.setColor(0xFF6A5236);
            c.drawRect(wx - 6, wy - 3, wx + 6, wy + 3, gv.fill);
            gv.fill.setColor(0xFF8C8F94);
            c.drawCircle(wx - 2, wy, 1.4f, gv.fill);
            gv.fill.setColor(0xFF5E6B3A);
            int crates = Math.min(4, (int) (s.stash / 40) + 1);
            for (int q = 0; q < crates; q++) c.drawRect(wx + 8 + q * 4.5f, wy - 2, wx + 11.5f + q * 4.5f, wy + 2, gv.fill);
        }
        // A flag on a pole by the door.
        float fx = dx + oy * 12, fy = dy - ox * 12;
        gv.stroke.setColor(0xFF3A3A3A);
        gv.stroke.setStrokeWidth(1f);
        c.drawLine(fx, fy, fx, fy - 12, gv.stroke);
        // (A raider base flies black with a red bar; the others yellow with a red square.)
        gv.fill.setColor(s.raiders ? 0xFF1E1E1E : 0xFFD8B04A);
        c.drawRect(fx, fy - 12, fx + 7, fy - 7.5f, gv.fill);
        gv.fill.setColor(s.raiders ? 0xFFC42A1E : 0xFF8E2E24);
        if (s.raiders) c.drawRect(fx, fy - 10.5f, fx + 7, fy - 9, gv.fill);
        else c.drawRect(fx + 2.5f, fy - 11, fx + 4.5f, fy - 8.5f, gv.fill);
    }

    void drawHoldout(Canvas c, World.Holdout h) {
        City.Building b = h.b;
        float f = h.fort;
        if (f >= 10) {
            // Boards nailed across the door.
            gv.stroke.setColor(0xFF8A6238);
            gv.stroke.setStrokeWidth(1.6f);
            float dx = b.doorX, dy = b.doorY;
            c.drawLine(dx - 4, dy - 3, dx + 4, dy + 3, gv.stroke);
            c.drawLine(dx - 4, dy + 3, dx + 4, dy - 3, gv.stroke);
        }
        if (f >= 25) ringOf(c, b, 6, Math.min(1, (f - 25) / 40f), 0xFFC4AA78, 0xFF6E5A36, 4.2f, b.doorX, b.doorY);
        if (f >= 65) ringOf(c, b, 14, Math.min(1, (f - 65) / 35f), 0xFF7A5634, 0xFF3E2A18, 3f, b.doorX, b.doorY);
    }

    /** Sandbags (or planks) laid round a building at margin m, the first part of the way round so far. */
    void ringOf(Canvas c, City.Building b, float m, float part, int col, int edge, float size, float gx, float gy) {
        float x0 = b.x0 - m, y0 = b.y0 - m, x1 = b.x1 + m, y1 = b.y1 + m;
        float w = x1 - x0, h = y1 - y0, per = 2 * (w + h);
        float step = size * 1.5f;
        int count = (int) (per / step), show = (int) (count * part);
        for (int k = 0; k < show; k++) {
            float t = k * step, px, py;
            boolean across;
            if (t < w) { px = x0 + t; py = y0; across = true; }
            else if (t < w + h) { px = x1; py = y0 + t - w; across = false; }
            else if (t < 2 * w + h) { px = x1 - (t - w - h); py = y1; across = true; }
            else { px = x0; py = y1 - (t - 2 * w - h); across = false; }
            // A gap to get in and out by the door.
            if (Math.abs(px - gx) < 7 && Math.abs(py - gy) < m + 7) continue;
            City.Building o = gv.world.city.buildingAt(px, py);
            if (o != null && o != b) continue;
            float hw = across ? size * 0.75f : size * 0.45f, hh = across ? size * 0.45f : size * 0.75f;
            gv.fill.setColor(edge);
            c.drawRect(px - hw - 0.4f, py - hh - 0.4f, px + hw + 0.4f, py + hh + 0.4f, gv.fill);
            gv.fill.setColor(col);
            c.drawRect(px - hw, py - hh, px + hw, py + hh, gv.fill);
        }
    }

    /** Gunsmoke drifting off a firefight. */
    void drawHaze(Canvas c, float vx0, float vy0, float vx1, float vy1) {
        float now = gv.world.time;
        for (int i = 0; i < World.MAXH; i++) {
            float age = now - gv.world.hborn[i];
            if (gv.world.hsize[i] <= 0 || age < 0 || age >= World.HAZE_LIFE) continue;
            float x = gv.world.hx[i] + age * 2.6f, y = gv.world.hy[i] - age * 1.1f;
            if (x < vx0 - 20 || x > vx1 + 20 || y < vy0 - 20 || y > vy1 + 20) continue;
            float k = age / World.HAZE_LIFE, f = k < 0.1f ? k * 10 : 1 - (k - 0.1f) / 0.9f;
            gv.fill.setColor(alpha(0xFFCFCFC6, 0.2f * f));
            c.drawCircle(x, y, gv.world.hsize[i] + age * 1.7f, gv.fill);
        }
    }

    /** A passenger train: a locomotive pulling three carriages along the line. */
    /**
     * Traffic lights on the corners of the big junctions (two show the north-south lights, two the east-west
     * ones) and stop signs at the small ones.
     */
    void drawSignals(Canvas c, float vx0, float vx1, float vy0, float vy1) {
        City city = gv.world.city;
        if (gv.scale < 1.2f) return;
        for (int i = 0, n = city.junctions.size(); i < n; i++) {
            int[] j = city.junctions.get(i);
            float x0 = j[0] * City.T - 4, y0 = j[1] * City.T - 4, x1 = (j[2] + 1) * City.T + 4, y1 = (j[3] + 1) * City.T + 4;
            if (x1 < vx0 - 10 || x0 > vx1 + 10 || y1 < vy0 - 10 || y0 > vy1 + 10) continue;
            if (j[4] == City.J_LIGHTS) {
                int ns = city.lightState(i, true, gv.world.time), ew = city.lightState(i, false, gv.world.time);
                signal(c, x0, y0, ns);
                signal(c, x1, y1, ns);
                signal(c, x1, y0, ew);
                signal(c, x0, y1, ew);
            } else if (j[4] == City.J_STOP) {
                stopSign(c, x0, y0);
                stopSign(c, x1, y1);
                stopSign(c, x1, y0);
                stopSign(c, x0, y1);
            }
        }
    }

    void signal(Canvas c, float x, float y, int state) {
        gv.fill.setColor(0x50000000);
        c.drawRect(x - 1.4f, y - 3.6f, x + 3, y + 4.6f, gv.fill);
        gv.fill.setColor(0xFF1E2024);
        c.drawRect(x - 2, y - 4.2f, x + 2, y + 4.2f, gv.fill);
        int[] on = {0xFF3AE070, 0xFFF0B020, 0xFFFF3A30};
        for (int k = 0; k < 3; k++) {
            boolean lit = state == 2 - k;
            gv.fill.setColor(lit ? on[2 - k] : 0xFF3A3C40);
            c.drawCircle(x, y - 2.6f + k * 2.6f, 1.05f, gv.fill);
        }
        int lit = state == 0 ? on[0] : state == 1 ? on[1] : on[2];
        gv.fill.setColor(alpha(lit, 0.22f));
        c.drawCircle(x, y - 2.6f + (2 - state) * 2.6f, 2.6f, gv.fill);
    }

    void stopSign(Canvas c, float x, float y) {
        gv.fill.setColor(0xFF8A8C90);
        c.drawRect(x - 0.4f, y, x + 0.4f, y + 3, gv.fill);
        gv.fill.setColor(0xFFD02A2A);
        c.drawCircle(x, y, 2.4f, gv.fill);
        gv.fill.setColor(0xFFF2F2F2);
        c.drawRect(x - 1.5f, y - 0.35f, x + 1.5f, y + 0.35f, gv.fill);
    }

    /** Level crossings: the barriers are up, or down with the red lights flashing while a train is near. */
    void drawCrossings(Canvas c, float vx0, float vx1, float vy0, float vy1) {
        City city = gv.world.city;
        if (city.railY0 < 0 || city.crossings.isEmpty()) return;
        float y0 = city.railY0 * City.T, y1 = (city.railY0 + city.railRows) * City.T;
        if (y1 < vy0 - 20 || y0 > vy1 + 20) return;
        boolean blink = ((int) (gv.world.time * 3)) % 2 == 0;
        for (int k = 0, n = city.crossings.size(); k < n; k++) {
            int[] cr = city.crossings.get(k);
            float x0 = cr[0] * City.T, x1 = cr[1] * City.T;
            if (x1 < vx0 - 20 || x0 > vx1 + 20) continue;
            boolean closed = gv.world.fleet.crossingClosed((x0 + x1) / 2);
            for (int side = 0; side < 2; side++) {
                // A post on the kerb at each end, the arm across the road on the approach side.
                float px = side == 0 ? x0 - 1.5f : x1 + 1.5f, py = side == 0 ? y0 - 2.5f : y1 + 2.5f, dir = side == 0 ? 1 : -1;
                float len = closed ? x1 - x0 + 1 : 4;
                for (float a = 0; a < len; a += 3) {
                    gv.fill.setColor(((int) (a / 3)) % 2 == 0 ? 0xFFD83A3A : 0xFFF2F2F2);
                    float ax = px + dir * a, bx = px + dir * Math.min(len, a + 3);
                    c.drawRect(Math.min(ax, bx), py - 0.8f, Math.max(ax, bx), py + 0.8f, gv.fill);
                }
                gv.fill.setColor(0xFF2C2C2E);
                c.drawCircle(px, py, 1.8f, gv.fill);
                if (closed) {
                    float ly = py - dir * 3;
                    gv.fill.setColor(blink ? 0xFFFF3A2A : 0xFF4A1414);
                    c.drawCircle(px, ly, 1.3f, gv.fill);
                    if (blink) {
                        gv.fill.setColor(0x33FF3A2A);
                        c.drawCircle(px, ly, 5, gv.fill);
                    }
                }
            }
        }
    }

    /**
     * Where the railway crosses the highway it's on a bridge: the deck is drawn over the traffic underneath
     * (with its shadow on the road), and any train on it over the deck.
     */
    /**
     * (10.32) The road bridges over the highway: drawn over the traffic going underneath, with a shadow below,
     * concrete parapets and the road across; then whatever is driving over them, on top.
     */
    void drawOverpasses(Canvas c, float vx0, float vx1, float vy0, float vy1) {
        City city = gv.world.city;
        if (city.overpasses.isEmpty()) return;
        boolean ew = city.hwyAxis == 0;
        for (float[] o : city.overpasses) {
            if (o[2] < vx0 || o[0] > vx1 || o[3] < vy0 || o[1] > vy1) continue;
            // The shadow it throws on the highway.
            gv.fill.setColor(0x48000000);
            if (ew) c.drawRect(o[2], o[1] + 4, o[2] + 7, o[3] - 4, gv.fill);
            else c.drawRect(o[0] + 4, o[3], o[2] - 4, o[3] + 7, gv.fill);
            // Deck and road.
            gv.fill.setColor(0xFF8E8A82);
            c.drawRect(o[0] - (ew ? 2 : 0), o[1] - (ew ? 0 : 2), o[2] + (ew ? 2 : 0), o[3] + (ew ? 0 : 2), gv.fill);
            gv.fill.setColor(0xFF3A3C40);
            c.drawRect(o[0] + (ew ? 1.5f : 0), o[1] + (ew ? 0 : 1.5f), o[2] - (ew ? 1.5f : 0), o[3] - (ew ? 0 : 1.5f), gv.fill);
            // Parapets along both sides.
            gv.fill.setColor(0xFFC4C0B6);
            if (ew) {
                c.drawRect(o[0] - 2, o[1], o[0] + 1.5f, o[3], gv.fill);
                c.drawRect(o[2] - 1.5f, o[1], o[2] + 2, o[3], gv.fill);
            } else {
                c.drawRect(o[0], o[1] - 2, o[2], o[1] + 1.5f, gv.fill);
                c.drawRect(o[0], o[3] - 1.5f, o[2], o[3] + 2, gv.fill);
            }
            // A line down the middle.
            gv.fill.setColor(0xD0E8C440);
            float mid = ew ? (o[0] + o[2]) / 2 : (o[1] + o[3]) / 2;
            for (float t = ew ? o[1] + 3 : o[0] + 3; t < (ew ? o[3] : o[2]) - 3; t += 9) {
                if (ew) c.drawRect(mid - 0.5f, t, mid + 0.5f, t + 5, gv.fill);
                else c.drawRect(t, mid - 0.5f, t + 5, mid + 0.5f, gv.fill);
            }
            for (int i = 0, n = gv.world.fleet.vehicles.size(); i < n; i++) {
                Fleet.Vehicle v = gv.world.fleet.vehicles.get(i);
                if (Fleet.offRoad(v) || !gv.world.fleet.onBridge(v)) continue;
                if (v.x < o[0] - 10 || v.x > o[2] + 10 || v.y < o[1] - 10 || v.y > o[3] + 10) continue;
                gv.vehicleRenderer.drawVehicle(c, v);
            }
        }
    }

    void drawRailBridge(Canvas c, float vx0, float vx1, float vy0, float vy1) {
        City city = gv.world.city;
        if (city.railY0 < 0 || city.hwyAxis != 1) return;
        float x0 = (city.hwyAt - 1) * City.T, x1 = (city.hwyAt + 8) * City.T;
        float y0 = city.railY0 * City.T - 3, y1 = (city.railY0 + city.railRows) * City.T + 3;
        if (x1 < vx0 || x0 > vx1 || y1 < vy0 || y0 > vy1) return;
        gv.fill.setColor(0x40000000);
        c.drawRect(x0, y1, x1, y1 + 7, gv.fill);
        // Deck, parapets and the track across it.
        gv.fill.setColor(0xFF8E8A82);
        c.drawRect(x0, y0, x1, y1, gv.fill);
        gv.fill.setColor(0xFFB4B0A6);
        c.drawRect(x0, y0, x1, y0 + 2.5f, gv.fill);
        c.drawRect(x0, y1 - 2.5f, x1, y1, gv.fill);
        gv.fill.setColor(0xFF6E5A46);
        float ry = (city.railY0 + city.railRows / 2f) * City.T;
        for (float x = x0 + 2; x < x1; x += 5) c.drawRect(x, ry - 7, x + 2.5f, ry + 7, gv.fill);
        gv.fill.setColor(0xFFB8BCC2);
        c.drawRect(x0, ry - 4.5f, x1, ry - 3.5f, gv.fill);
        c.drawRect(x0, ry + 3.5f, x1, ry + 4.5f, gv.fill);
        for (int i = 0, n = gv.world.fleet.vehicles.size(); i < n; i++) {
            Fleet.Vehicle v = gv.world.fleet.vehicles.get(i);
            if (v.type != Fleet.TRAIN) continue;
            float dir = (float) Math.cos(v.angle), back = v.x - dir * Fleet.TRAIN_LENGTH;
            if (Math.max(v.x, back) < x0 || Math.min(v.x, back) > x1) continue;
            c.save();
            c.clipRect(x0, y0 - 10, x1, y1 + 10);
            gv.vehicleRenderer.drawTrain(c, v);
            c.restore();
        }
    }

    /**
     * (11.0) The water moving: glints of light drifting over it, and a line of surf along every shore that swells
     * and fades. Only on screen, and only close enough in to see it.
     */
    void drawWaterLife(Canvas c, float vx0, float vy0, float vx1, float vy1) {
        City city = gv.world.city;
        int tx0 = Math.max(1, (int) (vx0 / City.T)), ty0 = Math.max(1, (int) (vy0 / City.T));
        int tx1 = Math.min(city.w - 2, (int) (vx1 / City.T)), ty1 = Math.min(city.h - 2, (int) (vy1 / City.T));
        if ((long) (tx1 - tx0) * (ty1 - ty0) > 40000) return;
        float t = gv.world.time, T = City.T;
        gv.stroke.setStyle(Paint.Style.STROKE);
        for (int y = ty0; y <= ty1; y++)
            for (int x = tx0; x <= tx1; x++) {
                int i = y * city.w + x;
                if (city.tiles[i] != City.WATER || city.isBridge(x, y)) continue;
                int hsh = (x * 73856093) ^ (y * 19349663);
                float px = x * T, py = y * T;
                if ((hsh & 7) == 0) {
                    // A glint, drifting slowly with the current and catching the light now and then.
                    float ph = t * 0.9f + (hsh >>> 8 & 255) * 0.0245f;
                    float a = (float) Math.sin(ph);
                    if (a > 0.35f) {
                        float gx = px + ((hsh >>> 4 & 15) + t * 3) % T, gy = py + (hsh >>> 12 & 15);
                        gv.stroke.setColor(alpha(0xFFE8F4FA, (a - 0.35f) * 0.55f));
                        gv.stroke.setStrokeWidth(0.9f);
                        c.drawLine(gx - 2.5f, gy, gx + 2.5f, gy, gv.stroke);
                    }
                }
                // Surf where the water meets the land. ((22.4) Along River City's smooth banks, below, not the tiles.)
                if (city.riverC != null && city.river != null && city.river[i]) continue;
                byte up = city.tiles[i - city.w], dn = city.tiles[i + city.w], lf = city.tiles[i - 1], rt = city.tiles[i + 1];
                boolean su = shore(up), sd = shore(dn), sl = shore(lf), sr = shore(rt);
                if (!su && !sd && !sl && !sr) continue;
                float swell = 0.5f + 0.5f * (float) Math.sin(t * 1.4f + x * 0.35f + y * 0.21f);
                gv.stroke.setColor(alpha(0xFFF4FAFC, 0.18f + 0.3f * swell));
                gv.stroke.setStrokeWidth(1.1f + swell * 0.9f);
                float in = 1.2f + swell * 1.6f;
                if (su) c.drawLine(px, py + in, px + T, py + in, gv.stroke);
                if (sd) c.drawLine(px, py + T - in, px + T, py + T - in, gv.stroke);
                if (sl) c.drawLine(px + in, py, px + in, py + T, gv.stroke);
                if (sr) c.drawLine(px + T - in, py, px + T - in, py + T, gv.stroke);
            }
        if (city.riverC != null) {
            boolean across = city.riverAcross;
            int a0 = Math.max(0, across ? tx0 : ty0), a1 = Math.min(city.riverC.length - 1, across ? tx1 : ty1);
            for (int a = a0; a <= a1; a++)
                for (int side = 0; side < 2; side++) {
                    boolean top = side == 0;
                    int land = top ? city.riverEdge0[a] - 1 : city.riverEdge1[a] + 1;
                    int lx = across ? a : land, ly = across ? land : a;
                    if (lx < 0 || ly < 0 || lx >= city.w || ly >= city.h || !shore(city.tiles[ly * city.w + lx]) || city.isBridge(lx, ly)) continue;
                    float swell = 0.5f + 0.5f * (float) Math.sin(t * 1.4f + a * 0.35f + side * 1.7f);
                    gv.stroke.setColor(alpha(0xFFF4FAFC, 0.14f + 0.22f * swell));
                    gv.stroke.setStrokeWidth(1.1f + swell * 0.9f);
                    float in = (1.6f + swell * 1.8f) * (top ? 1 : -1);
                    float s0 = city.bankAt(a, top) * T + in, s1 = city.bankAt(a + 1, top) * T + in;
                    if (across) c.drawLine(a * T, s0, (a + 1) * T, s1, gv.stroke);
                    else c.drawLine(s0, a * T, s1, (a + 1) * T, gv.stroke);
                }
        }
        gv.stroke.setStyle(Paint.Style.STROKE);
    }

    /** Land a wave breaks on (not a bridge's water, a pier or more water). */
    static boolean shore(byte t) {
        return t == City.SAND || t == City.GRASS || t == City.ROCK || t == City.TREE || t == City.SIDEWALK || t == City.DIRT;
    }

    /** ((25.0) Where planes came down: the broken, blackened fuselage, a torn-off wing and the scorched ground. */
    void drawCrashSites(Canvas c, float vx0, float vy0, float vx1, float vy1) {
        for (int i = 0, n = gv.world.crashes.size(); i < n; i++) {
            float[] k = gv.world.crashes.get(i);
            if (k[0] < vx0 - 140 || k[0] > vx1 + 140 || k[1] < vy0 - 140 || k[1] > vy1 + 140) continue;
            float u = 14f;
            boolean burning = k[3] < World.CRASH_BURN;
            c.save();
            c.translate(k[0], k[1]);
            c.rotate((float) Math.toDegrees(k[2]));
            gv.fill.setColor(0x70141210);
            gv.oval.set(-9.5f * u, -1.8f * u, 1.8f * u, 1.8f * u);
            c.drawOval(gv.oval, gv.fill);
            int hull = burning ? 0xFF4E4A46 : 0xFF6A6660, dark = 0xFF2A2724;
            // The nose section, broken off and slewed round.
            c.save();
            c.rotate(14);
            gv.fill.setColor(hull);
            c.drawRect(-0.6f * u, -0.55f * u, 2.4f * u, 0.55f * u, gv.fill);
            c.drawCircle(2.4f * u, 0, 0.55f * u, gv.fill);
            gv.fill.setColor(dark);
            c.drawRect(-0.6f * u, -0.55f * u, -0.2f * u, 0.55f * u, gv.fill);
            c.restore();
            // The middle of it, with what's left of a wing, and the other wing torn off and lying apart.
            c.save();
            c.translate(-3.2f * u, 0.3f * u);
            c.rotate(-9);
            gv.fill.setColor(0xFF8A8C90);
            c.drawRect(-1.2f * u, -3.2f * u, 0.2f * u, -0.4f * u, gv.fill);
            gv.fill.setColor(hull);
            c.drawRect(-2f * u, -0.55f * u, 1.8f * u, 0.55f * u, gv.fill);
            gv.fill.setColor(dark);
            for (int w = 0; w < 4; w++) c.drawRect((-1.6f + w * 0.9f) * u, -0.2f * u, (-1.2f + w * 0.9f) * u, 0.2f * u, gv.fill);
            c.restore();
            c.save();
            c.translate(-2.4f * u, 3.4f * u);
            c.rotate(64);
            gv.fill.setColor(0xFF7E8084);
            c.drawRect(-2f * u, -0.45f * u, 2f * u, 0.45f * u, gv.fill);
            gv.fill.setColor(0xFF505258);
            c.drawCircle(-0.4f * u, 0.7f * u, 0.4f * u, gv.fill);
            c.restore();
            // The tail, the fin still standing.
            c.save();
            c.translate(-6.6f * u, -0.4f * u);
            c.rotate(22);
            gv.fill.setColor(hull);
            c.drawRect(-1.6f * u, -0.45f * u, 1.2f * u, 0.45f * u, gv.fill);
            gv.fill.setColor(burning ? 0xFF6A2A22 : 0xFF8A3A30);
            c.drawRect(-1.7f * u, -0.22f * u, -0.6f * u, 0.22f * u, gv.fill);
            gv.fill.setColor(0xFF8A8C90);
            c.drawRect(-1.6f * u, -1.4f * u, -1.0f * u, 1.4f * u, gv.fill);
            c.restore();
            // Bits of it all over.
            gv.fill.setColor(dark);
            for (int b = 0; b < 9; b++) {
                float bx = ((b * 37) % 11 - 5) * 0.9f * u - 3 * u, by = ((b * 23) % 7 - 3) * 0.8f * u;
                c.drawRect(bx, by, bx + 0.35f * u, by + 0.25f * u, gv.fill);
            }
            if (burning) {
                float f = 0.5f + 0.5f * (float) Math.sin(gv.world.time * 9 + i);
                gv.fill.setColor(alpha(0xFFFF8A2A, 0.35f + 0.2f * f));
                c.drawCircle(-3f * u, 0, 1.6f * u, gv.fill);
                c.drawCircle(0.6f * u, 0.6f * u, 1.0f * u, gv.fill);
            }
            c.restore();
        }
    }

    /**
     * ((26.0) The police's mobile command post, set up: a canopy tent beside the truck with its table of radios and maps,
     * a tall radio mast with a light blinking on top, and a sign.
     */
    void drawCommandPost(Canvas c, Ops.Post post, float vx0, float vy0, float vx1, float vy1) {
        float x = post.x, y = post.y;
        if (x < vx0 - 60 || x > vx1 + 60 || y < vy0 - 60 || y > vy1 + 60) return;
        float a = post.truck.angle;
        c.save();
        c.translate(x, y);
        c.rotate((float) Math.toDegrees(a));
        float tw = 13, th = 9;
        gv.fill.setColor(0x40000000);
        c.drawRect(-tw + 2, -th + 2.5f, tw + 2, th + 2.5f, gv.fill);
        // Blue and white canopy, its ridge, and the poles at the corners.
        for (int k = 0; k < 6; k++) {
            gv.fill.setColor(post.army ? (k % 2 == 0 ? 0xFF55603A : 0xFF6E7A4A) : k % 2 == 0 ? 0xFF2A55B8 : 0xFFF0F2F4);
            float x0 = -tw + k * (tw * 2 / 6f);
            c.drawRect(x0, -th, x0 + tw * 2 / 6f, th, gv.fill);
        }
        gv.stroke.setColor(0xFF1A2A55);
        gv.stroke.setStrokeWidth(0.7f);
        c.drawLine(-tw, 0, tw, 0, gv.stroke);
        gv.stroke.setColor(0x60000000);
        c.drawRect(-tw, -th, tw, th, gv.stroke);
        if (gv.scale > 1.2f) {
            // The table under it with the radios and the map, seen at its open side.
            gv.fill.setColor(0xFF6A5A44);
            c.drawRect(-6, th + 1, 6, th + 4, gv.fill);
            gv.fill.setColor(0xFFE8E2C8);
            c.drawRect(-4.5f, th + 1.4f, 0, th + 3.6f, gv.fill);
            gv.fill.setColor(0xFF2A2C30);
            c.drawRect(1.5f, th + 1.5f, 3.5f, th + 3.4f, gv.fill);
            c.drawRect(4, th + 1.8f, 5.4f, th + 3.2f, gv.fill);
        }
        c.restore();
        // The radio mast: guy lines, the pole, the light.
        float mx = x + (float) Math.cos(a) * 18, my = y + (float) Math.sin(a) * 18;
        gv.stroke.setColor(0x80303030);
        gv.stroke.setStrokeWidth(0.4f);
        for (int k = 0; k < 3; k++) {
            double g = a + k * 2.094;
            c.drawLine(mx, my - 14, mx + (float) Math.cos(g) * 6, my + (float) Math.sin(g) * 4, gv.stroke);
        }
        gv.stroke.setColor(0xFF4A4C50);
        gv.stroke.setStrokeWidth(1f);
        c.drawLine(mx, my, mx, my - 16, gv.stroke);
        gv.stroke.setStrokeWidth(0.6f);
        c.drawLine(mx - 3, my - 13, mx + 3, my - 13, gv.stroke);
        c.drawLine(mx - 2, my - 10, mx + 2, my - 10, gv.stroke);
        boolean on = ((int) (gv.world.time * 1.5f)) % 2 == 0;
        gv.fill.setColor(on ? 0xFFFF3A30 : 0xFF7A1A18);
        c.drawCircle(mx, my - 16.5f, 1.2f, gv.fill);
        if (on) {
            gv.fill.setColor(0x40FF3A30);
            c.drawCircle(mx, my - 16.5f, 3.2f, gv.fill);
        }
        // The sign out front.
        if (gv.scale > 1.6f) {
            float sx = x - (float) Math.cos(a) * 17, sy = y - (float) Math.sin(a) * 17;
            gv.fill.setColor(0xFF1E2A55);
            c.drawRect(sx - 4.5f, sy - 2.2f, sx + 4.5f, sy + 2.2f, gv.fill);
            gv.fill.setColor(0xFFF2F2F2);
            c.drawRect(sx - 3.5f, sy - 0.5f, sx + 3.5f, sy + 0.5f, gv.fill);
        }
    }

    /**
     * A safe zone: the compound tinted, its wall (concrete barriers with fencing on top; broken sections as
     * rubble and a sagging fence), the gates (a boom, sandbags either side), and the tents inside: rows to
     * sleep in, the medical tent with its red cross and the food tent with crates stacked by it.
     */
    void drawZone(Canvas c, Dispatch.SafeZone z) {
        // ((11.5) Not at all when it's off screen, and only the bits of wall that are on it.)
        if (z.right() < gv.viewL || z.left() > gv.viewR || z.bottom() < gv.viewT || z.top() > gv.viewB) return;
        int tint = z.military ? 0xFF6FBF3F : 0xFF4F8FE0;
        float T = City.T;
        int W = gv.world.city.w;
        gv.fill.setColor(alpha(tint, z.open ? (z.fallingBack ? 0.08f : 0.13f) : 0.06f));
        c.drawRect(z.left() + T, z.top() + T, z.right() - T, z.bottom() - T, gv.fill);
        // ((11.5) Zoomed out, each section of wall is a grey block: the barriers, fence and posts are too small to see.)
        boolean plain = gv.scale < 1.6f;
        // The wall.
        drawZoneWall(c, z, T, W, plain);
        // The gates.
        drawZoneGates(c, z, T);
        // The tents.
        int canvas = z.military ? 0xFF5B6B3A : 0xFF3F64A6, ridge = z.military ? 0xFF6F8048 : 0xFF5A7EC0;
        for (int i = 0; i < z.tents.size(); i++) {
            float[] t = z.tents.get(i);
            drawTent(c, t[0], t[1], 11, 7, canvas, ridge);
        }
        if (z.medX > 0) {
            drawTent(c, z.medX, z.medY, 13, 8, 0xFFEDEDED, 0xFFFFFFFF);
            gv.fill.setColor(0xFFD83A3A);
            c.drawRect(z.medX - 1.2f, z.medY - 4, z.medX + 1.2f, z.medY + 4, gv.fill);
            c.drawRect(z.medX - 4, z.medY - 1.2f, z.medX + 4, z.medY + 1.2f, gv.fill);
        }
        if (z.foodX > 0) {
            drawTent(c, z.foodX, z.foodY, 13, 8, 0xFFB09068, 0xFFC8A880);
            int crates = z.foodMax > 0 ? (int) Math.ceil(5 * z.food / z.foodMax) : 0;
            for (int q = 0; q < crates; q++) {
                float cx = z.foodX + 16 + (q % 2) * 5, cy = z.foodY - 5 + (q / 2) * 5;
                gv.fill.setColor(0xFF7A5A34);
                c.drawRect(cx - 2.2f, cy - 2.2f, cx + 2.2f, cy + 2.2f, gv.fill);
                gv.fill.setColor(0xFF9A7A4E);
                c.drawRect(cx - 1.6f, cy - 1.6f, cx + 1.6f, cy - 0.4f, gv.fill);
            }
        }
        // A flag on a pole by the medical tent.
        float fx = z.medX > 0 ? z.medX - 18 : z.x, fy = z.medX > 0 ? z.medY : z.y;
        gv.stroke.setColor(0xFFDDDDDD);
        gv.stroke.setStrokeWidth(1f);
        c.drawLine(fx, fy + 6, fx, fy - 12, gv.stroke);
        gv.fill.setColor(tint);
        c.drawRect(fx, fy - 12, fx + 8, fy - 7, gv.fill);
    }

    /** ((27.3) From drawZone.) A safe zone's wall, section by section, by what it's built of. */
    void drawZoneWall(Canvas c, Dispatch.SafeZone z, float T, int W, boolean plain) {
        for (int k = 0; k < z.wall.length; k++) {
            int t = z.wall[k], tx = t % W, ty = t / W;
            float x0 = tx * T, y0 = ty * T;
            if (x0 + T < gv.viewL || x0 > gv.viewR || y0 + T < gv.viewT || y0 > gv.viewB) continue;
            boolean horizontal = ty == z.ty0 || ty == z.ty1;
            if (z.up[k] && plain) {
                gv.fill.setColor(0xFF8E8A80);
                if (horizontal) c.drawRect(x0, y0 + 2.5f, x0 + T, y0 + T - 2.5f, gv.fill);
                else c.drawRect(x0 + 2.5f, y0, x0 + T - 2.5f, y0 + T, gv.fill);
            } else if (z.up[k]) {
                // Concrete barriers in a line, a fence along the top with a post at each.
                gv.fill.setColor(0x66000000);
                c.drawRect(x0 + 2, y0 + 3, x0 + T + 2, y0 + T + 3, gv.fill);
                gv.fill.setColor(0xFF56534C);
                if (horizontal) c.drawRect(x0, y0 + 2.5f, x0 + T, y0 + T - 2.5f, gv.fill);
                else c.drawRect(x0 + 2.5f, y0, x0 + T - 2.5f, y0 + T, gv.fill);
                gv.fill.setColor(0xFFA8A49A);
                if (horizontal) c.drawRect(x0 + 0.6f, y0 + 3.5f, x0 + T - 0.6f, y0 + T - 3.5f, gv.fill);
                else c.drawRect(x0 + 3.5f, y0 + 0.6f, x0 + T - 3.5f, y0 + T - 0.6f, gv.fill);
                gv.fill.setColor(0xFFCAC6BC);
                if (horizontal) c.drawRect(x0 + 0.6f, y0 + 5.5f, x0 + T - 0.6f, y0 + T - 7, gv.fill);
                else c.drawRect(x0 + 5.5f, y0 + 0.6f, x0 + T - 7, y0 + T - 0.6f, gv.fill);
                gv.stroke.setColor(0xFF2E3236);
                gv.stroke.setStrokeWidth(1.3f);
                if (horizontal) c.drawLine(x0, y0 + T / 2, x0 + T, y0 + T / 2, gv.stroke);
                else c.drawLine(x0 + T / 2, y0, x0 + T / 2, y0 + T, gv.stroke);
                gv.stroke.setStrokeWidth(0.5f);
                gv.stroke.setColor(0x9033363A);
                for (int q = 1; q < 4; q++) {
                    if (horizontal) c.drawLine(x0 + q * 4, y0 + T / 2 - 2, x0 + q * 4 + 2, y0 + T / 2 + 2, gv.stroke);
                    else c.drawLine(x0 + T / 2 - 2, y0 + q * 4, x0 + T / 2 + 2, y0 + q * 4 + 2, gv.stroke);
                }
                gv.fill.setColor(0xFF22262A);
                c.drawCircle(x0 + T / 2, y0 + T / 2, 1.5f, gv.fill);
                if (z.hp[k] < 60) {
                    // Dented and buckling.
                    gv.fill.setColor(0x80302820);
                    c.drawCircle(x0 + T / 2 + 2, y0 + T / 2 - 1, 2.4f, gv.fill);
                }
            } else if (z.built >= 1 || z.hp[k] <= 0) {
                // A breach: barriers knocked aside, the fence torn down.
                gv.fill.setColor(0xFF8E8A80);
                c.drawRect(x0 + 1, y0 + 2, x0 + 6, y0 + 5, gv.fill);
                c.drawRect(x0 + T - 6, y0 + T - 5, x0 + T - 1, y0 + T - 2, gv.fill);
                gv.stroke.setColor(0xFF5E6266);
                gv.stroke.setStrokeWidth(0.6f);
                c.drawLine(x0 + 2, y0 + T / 2, x0 + T * 0.4f, y0 + T * 0.7f, gv.stroke);
            } else if (k < (int) (z.built * z.wall.length) + 4) {
                // The next sections, waiting to go up.
                gv.fill.setColor(0xFF9E9A90);
                c.drawRect(x0 + 3, y0 + 3, x0 + T - 3, y0 + 7, gv.fill);
            }
        }
    }

    /** ((27.3) From drawZone.) A safe zone's gates, open or shut. */
    void drawZoneGates(Canvas c, Dispatch.SafeZone z, float T) {
        for (int g = 0; g < z.gateTiles.length; g++) {
            float gx = (z.gateOut[g][0] + z.gateIn[g][0]) / 2, gy = (z.gateOut[g][1] + z.gateIn[g][1]) / 2;
            float dx = z.gateOut[g][0] - z.gateIn[g][0], dy = z.gateOut[g][1] - z.gateIn[g][1], l = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
            dx /= l;
            dy /= l;
            float px = -dy, py = dx;
            // Sandbags either side, on the inside.
            for (int s = -1; s <= 1; s += 2)
                for (int q = 0; q < 3; q++) {
                    float bx = gx + px * s * (T + 4) - dx * (5 + q * 3), by = gy + py * s * (T + 4) - dy * (5 + q * 3);
                    gv.fill.setColor(0x60000000);
                    c.drawCircle(bx + 0.8f, by + 1, 3, gv.fill);
                    gv.fill.setColor(q % 2 == 0 ? 0xFFA38D5E : 0xFF917C50);
                    c.drawCircle(bx, by, 3, gv.fill);
                }
            if (z.gateHp[g] <= 0) continue;
            if (!z.open) continue;
            // The boom: across the gap when shut, swung up when open.
            float len = z.gateOpen[g] ? 6 : T * 2 - 2;
            float sx = gx - px * (T - 1), sy = gy - py * (T - 1);
            for (int q = 0; q < 4; q++) {
                float s0 = len * q / 4, s1 = len * (q + 1) / 4;
                gv.stroke.setColor(q % 2 == 0 ? 0xFFE03A30 : 0xFFF2F2F2);
                gv.stroke.setStrokeWidth(1.6f);
                c.drawLine(sx + px * s0, sy + py * s0, sx + px * s1, sy + py * s1, gv.stroke);
            }
            gv.fill.setColor(0xFF3A3E42);
            c.drawCircle(sx, sy, 1.6f, gv.fill);
        }
    }

    void drawTent(Canvas c, float x, float y, float hw, float hh, int canvas, int ridge) {
        gv.fill.setColor(0x55000000);
        c.drawRect(x - hw + 2, y - hh + 2.5f, x + hw + 2, y + hh + 2.5f, gv.fill);
        gv.fill.setColor(canvas);
        c.drawRect(x - hw, y - hh, x + hw, y + hh, gv.fill);
        gv.fill.setColor(ridge);
        c.drawRect(x - hw, y - hh, x + hw, y - 0.6f, gv.fill);
        gv.stroke.setColor(City.darken(canvas, 0.7f));
        gv.stroke.setStrokeWidth(0.7f);
        c.drawLine(x - hw, y, x + hw, y, gv.stroke);
    }

    /** A leafy canopy: overlapping clumps, shaded underneath and lit on top, each tree a little different. */
    void drawRealTree(Canvas c, float x, float y, float r, float sway, float ts, float a, int seed) {
        int v = (seed & 0x7FFFFFFF) % 3;
        gv.fill.setColor(alpha(LEAF_DARK[v], a));
        c.drawCircle(x + 0.8f * ts, y + 0.8f * ts, r, gv.fill);
        for (int k = 0; k < 5; k++) {
            double ang = k * 1.2566 + (seed % 7) * 0.3;
            float lx = x + (float) Math.cos(ang) * r * 0.45f + sway * 0.4f, ly = y + (float) Math.sin(ang) * r * 0.45f + sway * 0.2f;
            gv.fill.setColor(alpha(LEAF_MID[(v + k) % 3], a));
            c.drawCircle(lx - 0.6f * ts, ly - 0.6f * ts, r * 0.52f, gv.fill);
        }
        gv.fill.setColor(alpha(LEAF_LIGHT[v], a));
        c.drawCircle(x - r * 0.3f + sway * 0.8f, y - r * 0.3f + sway * 0.4f, r * 0.38f, gv.fill);
        gv.fill.setColor(alpha(0xFF7AB05A, a * 0.6f));
        c.drawCircle(x - r * 0.42f + sway, y - r * 0.45f + sway * 0.5f, r * 0.16f, gv.fill);
    }

    void drawDanger(Canvas c, float vx0, float vy0, float vx1, float vy1) {
        int gw = (int) (gv.world.city.worldW() / DANGER_CELL) + 1, gh = (int) (gv.world.city.worldH() / DANGER_CELL) + 1;
        if (gv.dangerGrid == null || gv.dangerGrid.length != gw * gh) gv.dangerGrid = new int[gw * gh];
        gv.dangerTimer -= 1 / 60f;
        if (gv.dangerTimer <= 0) {
            gv.dangerTimer = 0.3f;
            java.util.Arrays.fill(gv.dangerGrid, 0);
            for (int i = 0, n = gv.world.entities.size(); i < n; i++) {
                Entity e = gv.world.entities.get(i);
                if (e.dead || !e.isZombie()) continue;
                int gx = (int) (e.x / DANGER_CELL), gy = (int) (e.y / DANGER_CELL);
                if (gx >= 0 && gy >= 0 && gx < gw && gy < gh) gv.dangerGrid[gy * gw + gx]++;
            }
        }
        float alphaScale = Math.min(1, (1.8f - gv.scale) / 0.8f);
        for (int gy = Math.max(0, (int) (vy0 / DANGER_CELL)); gy <= Math.min(gh - 1, (int) (vy1 / DANGER_CELL)); gy++)
            for (int gx = Math.max(0, (int) (vx0 / DANGER_CELL)); gx <= Math.min(gw - 1, (int) (vx1 / DANGER_CELL)); gx++) {
                int n = gv.dangerGrid[gy * gw + gx];
                if (n < 2) continue;
                float a = Math.min(0.42f, 0.08f + n * 0.025f) * alphaScale;
                float cx = (gx + 0.5f) * DANGER_CELL, cy = (gy + 0.5f) * DANGER_CELL;
                gv.fill.setColor(alpha(0xFFE0302A, a * 0.5f));
                c.drawCircle(cx, cy, DANGER_CELL * 0.95f, gv.fill);
                gv.fill.setColor(alpha(0xFFE0302A, a));
                c.drawCircle(cx, cy, DANGER_CELL * 0.6f, gv.fill);
            }
    }

    /** ((22.0) Sandbags (a stack of sacks) and concrete barriers (a grey block), cracking as they take hits. */
    void drawToughBarricade(Canvas c, float[] b) {
        boolean concrete = b[5] >= 2;
        float left = Math.max(0, b[2] / World.BARRICADE_HP[concrete ? 2 : 1]);
        gv.fill.setColor(0x55000000);
        c.drawRect(b[0] - 7, b[1] - 5, b[0] + 9, b[1] + 8, gv.fill);
        if (concrete) {
            gv.fill.setColor(0xFF9A9C9E);
            c.drawRect(b[0] - 8, b[1] - 6, b[0] + 8, b[1] + 6, gv.fill);
            gv.fill.setColor(0xFFB8BABC);
            c.drawRect(b[0] - 8, b[1] - 6, b[0] + 8, b[1] - 2, gv.fill);
            gv.fill.setColor(0xFFE0B020);
            c.drawRect(b[0] - 6, b[1] + 1, b[0] - 2, b[1] + 3, gv.fill);
            c.drawRect(b[0] + 2, b[1] + 1, b[0] + 6, b[1] + 3, gv.fill);
            if (left < 0.6f) {
                gv.stroke.setColor(0xFF505254);
                gv.stroke.setStrokeWidth(0.8f);
                c.drawLine(b[0] - 3, b[1] - 6, b[0] + 1, b[1] + 6, gv.stroke);
                if (left < 0.3f) c.drawLine(b[0] + 4, b[1] - 6, b[0] + 2, b[1] + 2, gv.stroke);
            }
        } else {
            for (int row = 0; row < 2; row++)
                for (int k = 0; k < 3 - row; k++) {
                    gv.fill.setColor((k + row) % 2 == 0 ? 0xFFB9A57A : 0xFFA8946A);
                    gv.oval.set(b[0] - 8 + k * 5.4f + row * 2.7f, b[1] - row * 4.5f - 1, b[0] - 2.4f + k * 5.4f + row * 2.7f, b[1] - row * 4.5f + 4);
                    c.drawRoundRect(gv.oval, 2, 2, gv.fill);
                }
            if (left < 0.5f) {
                gv.fill.setColor(0xFF8A7650);
                c.drawCircle(b[0] + 3, b[1] + 4, 1.6f, gv.fill);
            }
        }
    }

    // ------------------------------------------------------------------ reinforcements (10.17)

    static int convoyColor(Dispatch.Convoy cv) {
        return cv.type == Entity.SOLDIER ? 0xFFB4C85A : 0xFF5A9CFF;
    }

    /**
     * Reinforcements coming in from outside: where they came onto the map (for a while), a dashed line from
     * the head of the column to where they're going, a tag over the column, and an arrow at the edge of the
     * screen pointing to them while they're off it.
     */
    void drawConvoys(Canvas c) {
        int w = gv.getWidth();
        float top = gv.topRects[0].bottom + (gv.portrait ? 150 : 115) * gv.dp, bottom = gv.barTop - 60 * gv.dp;
        // The radio calls for help, tagged where they were made (for their first few seconds).
        for (int i = 0; i < gv.world.dispatch.callouts.size(); i++) {
            Dispatch.Callout co = gv.world.dispatch.callouts.get(i);
            if (co.age > 4) continue;
            float sx = gv.screenX(co.x), sy = gv.screenY(co.y);
            if (sx < 0 || sx > w || sy < top || sy > bottom) continue;
            gv.mapLabels.smallTag(c, co.label, sx, sy - 18 * gv.dp, co.color);
        }
        float pulse = (float) Math.abs(Math.sin(gv.world.time * 4));
        for (int i = 0; i < gv.world.dispatch.convoys.size(); i++) {
            Dispatch.Convoy cv = gv.world.dispatch.convoys.get(i);
            int col = convoyColor(cv);
            float hx = gv.screenX(cv.x), hy = gv.screenY(cv.y), tx = gv.screenX(cv.toX), ty = gv.screenY(cv.toY);
            // The route: dashes from the column to the destination, marching along.
            float dx = tx - hx, dy = ty - hy, len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > 1) {
                gv.stroke.setColor((col & 0x00FFFFFF) | 0x70000000);
                gv.stroke.setStrokeWidth(1.4f * gv.dp);
                float dash = 6 * gv.dp, off = (gv.world.time * 24 * gv.dp) % (dash * 2);
                for (float d = off; d < len; d += dash * 2) {
                    float e = Math.min(len, d + dash);
                    c.drawLine(hx + dx * d / len, hy + dy * d / len, hx + dx * e / len, hy + dy * e / len, gv.stroke);
                }
            }
            // Where they're going.
            gv.stroke.setColor(col);
            gv.stroke.setStrokeWidth(1.3f * gv.dp);
            c.drawCircle(tx, ty, (5 + 2.5f * pulse) * gv.dp, gv.stroke);
            // Where they came in (for the first quarter minute): just a dot.
            if (cv.age < 15) {
                float ex = gv.screenX(cv.fromX), ey = gv.screenY(cv.fromY);
                gv.fill.setColor((col & 0x00FFFFFF) | ((int) (0x70 + 0x50 * pulse) << 24));
                c.drawCircle(ex, ey, (5 + 3 * pulse) * gv.dp, gv.fill);
            }
            boolean on = hx > 0 && hx < w && hy > top && hy < bottom;
            if (on) {
                gv.mapLabels.smallTag(c, cv.label, hx, hy - 13 * gv.dp, col);
                continue;
            }
            // Off screen: an arrow at the edge pointing to them.
            float cx = w / 2f, cy = (top + bottom) / 2, ddx = hx - cx, ddy = hy - cy;
            float k = Math.min(Math.abs((w / 2f - 30 * gv.dp) / (ddx == 0 ? 0.001f : ddx)), Math.abs(((bottom - top) / 2 - 30 * gv.dp) / (ddy == 0 ? 0.001f : ddy)));
            float ix = cx + ddx * k, iy = cy + ddy * k;
            float a = (float) Math.atan2(ddy, ddx);
            android.graphics.Path p = new android.graphics.Path();
            float r = 8 * gv.dp;
            p.moveTo(ix + (float) Math.cos(a) * r, iy + (float) Math.sin(a) * r);
            p.lineTo(ix + (float) Math.cos(a + 2.4f) * r, iy + (float) Math.sin(a + 2.4f) * r);
            p.lineTo(ix + (float) Math.cos(a - 2.4f) * r, iy + (float) Math.sin(a - 2.4f) * r);
            p.close();
            gv.fill.setColor(col);
            c.drawPath(p, gv.fill);
            float lx = ix - (float) Math.cos(a) * 18 * gv.dp, ly = iy - (float) Math.sin(a) * 18 * gv.dp;
            gv.mapLabels.smallTag(c, cv.label, Math.max(40 * gv.dp, Math.min(w - 40 * gv.dp, lx)), ly, col);
        }
    }

    /** (14.9) A mortar pit: a ring of sandbags round the post, the tube on its baseplate in the middle, pointing where it last fired. */
    void drawMortarPit(Canvas c, Entity e) {
        float x = e.postX, y = e.postY;
        gv.fill.setColor(0x40000000);
        c.drawCircle(x + 1.2f, y + 1.8f, 19, gv.fill);
        gv.fill.setColor(0xFF5E573E);
        c.drawCircle(x, y, 13, gv.fill);
        for (int k = 0; k < 16; k++) {
            float a = k * (float) Math.PI * 2 / 16;
            float bx = x + (float) Math.cos(a) * 16, by = y + (float) Math.sin(a) * 16;
            gv.fill.setColor(k % 2 == 0 ? 0xFFC2B07C : 0xFFAA9A6C);
            c.drawCircle(bx, by, 3.4f, gv.fill);
        }
        // Rounds stacked by the wall of the pit.
        gv.fill.setColor(0xFF3E4430);
        for (int k = 0; k < Math.min(6, (e.grenades + 2) / 3); k++) c.drawRect(x - 9 + k * 2.4f, y + 7, x - 7.4f + k * 2.4f, y + 10.5f, gv.fill);
        // The tube, angled up the way it fires, beside where the team stands.
        float a = e.angle, tx = x + (float) Math.cos(a + 1.9f) * 5, ty = y + (float) Math.sin(a + 1.9f) * 5;
        gv.fill.setColor(0xFF2A2E22);
        c.drawCircle(tx, ty, 2.2f, gv.fill);
        gv.stroke.setStrokeWidth(2f);
        gv.stroke.setColor(0xFF2A2E22);
        c.drawLine(tx, ty, tx + (float) Math.cos(a) * 6, ty + (float) Math.sin(a) * 6, gv.stroke);
    }

    /**
     * (14.8) A roadblock's crowd barriers: steel panels end to end across the road and pavements, each on its two
     * feet - or, once the dead have pushed them over, lying flat and skewed where they fell.
     */
    void drawBarriers(Canvas c, Fleet.Vehicle v) {
        float dx = v.barX1 - v.barX0, dy = v.barY1 - v.barY0, len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return;
        int panels = Math.max(2, Math.round(len / 9));
        float ux = dx / len, uy = dy / len;
        gv.stroke.setStrokeCap(Paint.Cap.ROUND);
        for (int k = 0; k < panels; k++) {
            float a0 = k * len / panels + 0.6f, a1 = (k + 1) * len / panels - 0.6f;
            float x0 = v.barX0 + ux * a0, y0 = v.barY0 + uy * a0, x1 = v.barX0 + ux * a1, y1 = v.barY0 + uy * a1;
            if (v.barDown) {
                // Knocked flat, each panel skewed a different way, pushed in towards the zone.
                float sk = ((k * 37) % 7 - 3) * 0.12f, push = 3 + (k % 3) * 2;
                float ox = v.barNx * v.barIn * push, oy = v.barNy * v.barIn * push;
                float mx = (x0 + x1) / 2 + ox, my = (y0 + y1) / 2 + oy, hl = (a1 - a0) / 2;
                float cs = (float) Math.cos(sk), sn = (float) Math.sin(sk);
                float rx = ux * cs - uy * sn, ry = ux * sn + uy * cs;
                gv.stroke.setStrokeWidth(2.6f);
                gv.stroke.setColor(0xFF8C9298);
                c.drawLine(mx - rx * hl, my - ry * hl, mx + rx * hl, my + ry * hl, gv.stroke);
                continue;
            }
            gv.stroke.setStrokeWidth(2.2f);
            gv.stroke.setColor(0x55000000);
            c.drawLine(x0 + 0.7f, y0 + 0.9f, x1 + 0.7f, y1 + 0.9f, gv.stroke);
            gv.stroke.setColor(0xFFB8BEC4);
            c.drawLine(x0, y0, x1, y1, gv.stroke);
            gv.stroke.setStrokeWidth(0.8f);
            gv.stroke.setColor(0xFF5A6066);
            c.drawLine(x0, y0, x1, y1, gv.stroke);
            // The feet.
            gv.fill.setColor(0xFF4A4F55);
            for (int f = 0; f < 2; f++) {
                float fx = f == 0 ? x0 : x1, fy = f == 0 ? y0 : y1;
                c.drawRect(fx - v.barNx * 1.8f - 0.6f, fy - v.barNy * 1.8f - 0.6f, fx + v.barNx * 1.8f + 0.6f, fy + v.barNy * 1.8f + 0.6f, gv.fill);
            }
        }
    }
}
