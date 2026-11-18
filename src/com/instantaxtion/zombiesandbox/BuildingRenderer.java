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
 * ((27.3) Out of GameView.) The buildings standing up: their walls and roofs leaning away from the middle of the screen, the
 * lettering on the roofs, damage and wear, what's inside when you're zoomed right in, and those up on the roofs.
 */
final class BuildingRenderer {
    final GameView gv;

    BuildingRenderer(GameView gv) {
        this.gv = gv;
    }


    /**
     * A roof's lettering (shop names, POLICE, FIRE...) drawn as text on the raised roof, so it stays sharp at any
     * zoom: s is the roof's perspective scale about (cx, cy), a its opacity.
     */
    void drawRoofLabels(Canvas c, City.Building b, float cx, float cy, float s, float a) {
        City city = gv.world.city;
        if (city != gv.labelCity || city.renderVersion != gv.labelVersion) {
            gv.labelCity = city;
            gv.labelVersion = city.renderVersion;
            gv.roofLabels.clear();
            for (City.Label l : city.labels) {
                if (l.building < 0 || l.building >= city.buildings.size()) continue;
                City.Building o = city.buildings.get(l.building);
                java.util.ArrayList<City.Label> list = gv.roofLabels.get(o);
                if (list == null) gv.roofLabels.put(o, list = new java.util.ArrayList<City.Label>());
                list.add(l);
            }
        }
        java.util.ArrayList<City.Label> list = gv.roofLabels.get(b);
        if (list == null || a <= 0.1f) return;
        gv.labelPaint.setTextAlign(Paint.Align.CENTER);
        gv.labelPaint.setFakeBoldText(true);
        for (int i = 0; i < list.size(); i++) {
            City.Label l = list.get(i);
            float size = l.size * s;
            // ((11.5) Not under five pixels tall: unreadable, and text is among the dearest things to draw.)
            if (size * gv.scale < 5f) continue;
            gv.labelPaint.setTextSize(size);
            float lx = cx + (l.x - cx) * s, ly = cy + (l.y - cy) * s;
            if (l.sign) {
                // (10.52) A sign: on a dark plate, the letters centred on it.
                float tw = gv.labelPaint.measureText(l.text) / 2 + size * 0.35f, th = size * 0.62f;
                gv.fill.setColor(alpha(0xC8141618, a));
                gv.oval.set(lx - tw, ly - th, lx + tw, ly + th);
                c.drawRoundRect(gv.oval, size * 0.2f, size * 0.2f, gv.fill);
                ly += size * 0.36f;
            }
            int col = l.color;
            // (Dark lettering on the dark plate: white instead.)
            if (l.sign && ((col >> 16) & 0xFF) + ((col >> 8) & 0xFF) + (col & 0xFF) < 300) col = (col & 0xFF000000) | 0xF2F2F2;
            gv.labelPaint.setColor(col);
            gv.labelPaint.setAlpha((int) (((col >>> 24) & 0xFF) * a));
            c.drawText(l.text, lx, ly, gv.labelPaint);
        }
    }

    /**
     * (17.1) How see-through a building's roof and walls are: all buildings alike as you zoom right in. ((26.5) No
     * longer any building with people in it from a middling zoom: shops, nearly always with someone in, lost their roofs
     * long before the buildings round them.)
     */
    float seeOf(City.Building b, float zoomSee) {
        return zoomSee;
    }

    void drawInsides(Canvas c, float vx0, float vy0, float vx1, float vy1) {
        float zoomSee = Math.max(0, Math.min(1, (gv.scale / gv.dp - 2.2f) / 1.3f));
        if (zoomSee <= 0) return;
        for (int i = 0, n = gv.world.city.buildings.size(); i < n; i++) {
            City.Building b = gv.world.city.buildings.get(i);
            if (b.collapsed || b.x1 < vx0 || b.x0 > vx1 || b.y1 < vy0 || b.y0 > vy1) continue;
            drawInterior(c, b, 1);
        }
    }

    /**
     * ((27.0) Marksmen and snipers up on roofs: drawn on the roof (leaning out with it, as the buildings do), over the
     * building, with a little scope mark over them. A dot when zoomed out, like everyone else.
     */
    void drawOverwatch(Canvas c, float vx0, float vy0, float vx1, float vy1, boolean detailed) {
        float cx = gv.camX + gv.getWidth() / gv.scale / 2, cy = gv.camY + gv.getHeight() / gv.scale / 2;
        float camH = Math.max(Math.max(gv.getWidth(), gv.getHeight()) / gv.scale * 0.9f, Math.max(260f, gv.world.city.maxHeight * 1.7f));
        boolean in3d = gv.settings.buildings3d();
        for (int i = 0, n = gv.world.overwatch.size(); i < n; i++) {
            Entity e = gv.world.overwatch.get(i);
            City.Building b = e.roof;
            if (b == null || !e.onTower || e.dead || e.removed) continue;
            float s = in3d ? camH / (camH - b.height) : 1;
            float px = cx + (e.roofX - cx) * s, py = cy + (e.roofY - cy) * s;
            if (px < vx0 - 20 || px > vx1 + 20 || py < vy0 - 20 || py > vy1 + 20) continue;
            if (detailed) {
                c.save();
                c.translate(px - e.x, py - e.y);
                gv.entityRenderer.drawEntity(c, e, gv.settings.healthBars());
                c.restore();
            } else {
                float r = Math.max(2.2f, 3.2f / gv.scale);
                gv.fill.setColor(0xFF101114);
                c.drawCircle(px, py, r + 0.8f / gv.scale, gv.fill);
                gv.fill.setColor(ROW_COLORS[e.type == Entity.COP ? 1 : 2]);
                c.drawCircle(px, py, r, gv.fill);
            }
            // The mark: a small scope ring with a tick, over them.
            float mr = Math.max(2.4f, 3 / gv.scale), my = py - Math.max(9, 11 / gv.scale);
            gv.stroke.setColor(0xF0FFE27A);
            gv.stroke.setStrokeWidth(Math.max(0.7f, 1 / gv.scale));
            c.drawCircle(px, my, mr, gv.stroke);
            c.drawLine(px - mr * 1.5f, my, px + mr * 1.5f, my, gv.stroke);
            c.drawLine(px, my - mr * 1.5f, px, my + mr * 1.5f, gv.stroke);
        }
    }

    void drawBuildings(Canvas c, float vx0, float vy0, float vx1, float vy1, boolean detailed) {
        float cx = gv.camX + gv.getWidth() / gv.scale / 2, cy = gv.camY + gv.getHeight() / gv.scale / 2;
        // Camera height grows with the visible area so the lean looks the same at every zoom level.
        float camH = Math.max(Math.max(gv.getWidth(), gv.getHeight()) / gv.scale * 0.9f,
                Math.max(260f, gv.world.city.maxHeight * 1.7f));
        boolean in3d = gv.settings.buildings3d();
        // Zoomed right in, roofs (and walls) turn see-through so you can look inside.
        float see = Math.max(0, Math.min(1, (gv.scale / gv.dp - 2.2f) / 1.3f));

        float ts = in3d ? camH / (camH - City.TREE_HEIGHT) : 1f;
        // (The trees are sorted top to bottom: just the rows that can be on screen.)
        float rowTop = Math.min(vy0, cy + (vy0 - cy) / ts) - 14, rowBottom = Math.max(vy1, cy + (vy1 - cy) / ts) + 14;
        // Zoomed well out on a big map, the treetops are already in the map picture.
        int firstTree = gv.scale < gv.world.city.canopyZoom() ? gv.world.city.trees.size() : gv.world.city.firstTreeAt(rowTop);
        drawTrees(c, vx0, vy0, vx1, vy1, cx, cy, see, ts, rowBottom, firstTree);
        if (!in3d) {
            // Bird's-eye: the flat roofs are already in the ground bitmap.
            for (int i = 0, count = gv.world.city.buildings.size(); i < count; i++) {
                City.Building b = gv.world.city.buildings.get(i);
                if (b.collapsed || b.x1 < vx0 || b.x0 > vx1 || b.y1 < vy0 || b.y0 > vy1) continue;
                drawDamage(c, b, b.x0, b.y0, b.x1, b.y1, 1);
                if (b.flash > 0) drawGunGlow(c, b, b.x0, b.y0, b.x1, b.y1);
                // ((17.1) The inside is drawn under everyone now (drawInsides).)
                // (The lettering isn't in the ground bitmap any more: it's drawn here, sharp at any zoom.)
                drawRoofLabels(c, b, cx, cy, 1, 1 - 0.82f * seeOf(b, see));
            }
            return;
        }

        int n = 0;
        for (int i = 0, count = gv.world.city.buildings.size(); i < count; i++) {
            City.Building b = gv.world.city.buildings.get(i);
            if (b.collapsed) continue;
            float s = camH / (camH - b.height);
            float rx0 = cx + (b.x0 - cx) * s, rx1 = cx + (b.x1 - cx) * s;
            float ry0 = cy + (b.y0 - cy) * s, ry1 = cy + (b.y1 - cy) * s;
            if (Math.max(b.x1, rx1) < vx0 || Math.min(b.x0, rx0) > vx1
                    || Math.max(b.y1, ry1) < vy0 || Math.min(b.y0, ry0) > vy1) continue;
            if (n == gv.visible.length) {
                City.Building[] nb = new City.Building[n * 2];
                float[] nk = new float[n * 2];
                System.arraycopy(gv.visible, 0, nb, 0, n);
                System.arraycopy(gv.visibleKey, 0, nk, 0, n);
                gv.visible = nb;
                gv.visibleKey = nk;
            }
            float ddx = (b.x0 + b.x1) / 2 - cx, ddy = (b.y0 + b.y1) / 2 - cy;
            // Short buildings first, then the far ones: tall and near buildings end up in front.
            float key = b.height * 10000f - (float) Math.sqrt(ddx * ddx + ddy * ddy);
            int j = n++;
            while (j > 0 && gv.visibleKey[j - 1] > key) {
                gv.visible[j] = gv.visible[j - 1];
                gv.visibleKey[j] = gv.visibleKey[j - 1];
                j--;
            }
            gv.visible[j] = b;
            gv.visibleKey[j] = key;
        }

        // Windows are only a few pixels tall when zoomed out, and there are thousands of them: skip them then.
        boolean windows = gv.scale > 2.2f;
        gv.wallFade = 1 - 0.55f * see;
        for (int i = 0; i < n; i++) {
            City.Building b = gv.visible[i];
            float s = camH / (camH - b.height);
            float rx0 = cx + (b.x0 - cx) * s, rx1 = cx + (b.x1 - cx) * s;
            float ry0 = cy + (b.y0 - cy) * s, ry1 = cy + (b.y1 - cy) * s;
            // ((17.1) Its inside was drawn under everyone (drawInsides); see-through by the building.)
            float sb = seeOf(b, see);
            gv.wallFade = 1 - 0.55f * sb;
            if (cy < b.y0) drawWall(c, b, b.x0, b.y0, b.x1, b.y0, rx0, ry0, rx1, ry0, 0.8f, windows, 0);
            if (cy > b.y1) drawWall(c, b, b.x1, b.y1, b.x0, b.y1, rx1, ry1, rx0, ry1, 0.52f, windows, 1);
            if (cx < b.x0) drawWall(c, b, b.x0, b.y1, b.x0, b.y0, rx0, ry1, rx0, ry0, 0.9f, windows, 2);
            if (cx > b.x1) drawWall(c, b, b.x1, b.y0, b.x1, b.y1, rx1, ry0, rx1, ry1, 0.62f, windows, 3);
            float dl = gv.world.city.detail;
            gv.roofSrc.set((int) (b.x0 * dl), (int) (b.y0 * dl), (int) Math.ceil(b.x1 * dl), (int) Math.ceil(b.y1 * dl));
            gv.roofDst.set(rx0, ry0, rx1, ry1);
            // ((10.53) All the way off when zoomed right in: a faint ghost of the roof - its skylights and plant - lay
            // blurred over the rooms.)
            float roofA = 1 - sb;
            gv.bmpPaint.setAlpha((int) (255 * roofA));
            gv.world.city.drawMap(c, gv.roofSrc.left, gv.roofSrc.top, gv.roofSrc.right, gv.roofSrc.bottom, gv.roofDst, gv.bmpPaint);
            gv.bmpPaint.setAlpha(255);
            if (roofA >= 0.999f) sharpRoof(c, b, s, cx, cy);
            drawDamage(c, b, rx0, ry0, rx1, ry1, roofA);
            if (b.flash > 0) drawGunGlow(c, b, rx0, ry0, rx1, ry1);
            drawRoofLabels(c, b, cx, cy, s, 1 - 0.82f * sb);
        }
        gv.wallFade = 1;
    }

    /** ((27.3) From drawBuildings.) The trees standing up, leaning like the buildings, by kind. */
    void drawTrees(Canvas c, float vx0, float vy0, float vx1, float vy1, float cx, float cy, float see, float ts, float rowBottom, int firstTree) {
        for (int i = firstTree, n = gv.world.city.trees.size(); i < n; i++) {
            float[] t = gv.world.city.trees.get(i);
            if (t[1] > rowBottom) break;
            float x = cx + (t[0] - cx) * ts, y = cy + (t[1] - cy) * ts, r = t[2] * ts;
            if (x + r < vx0 || x - r > vx1 || y + r < vy0 || y - r > vy1) continue;
            int kind = t.length > 3 ? (int) t[3] : 0;
            if (kind >= Country.TK_PALM) {
                // ((22.9) Desert City: a palm, a saguaro or desert scrub.)
                City.desertTree(c, gv.fill, x, y, r, kind, 1 - see * 0.5f, t[0] * 0.13f + t[1] * 0.07f);
                continue;
            }
            if (kind == Country.TK_PINE) {
                // A pine: dark, pointed boughs round a lighter tip.
                float a = 1 - see * 0.5f, spin = t[0] * 0.13f + t[1] * 0.07f;
                for (int layer = 0; layer < 2; layer++) {
                    float lr = layer == 0 ? r : r * 0.62f, ox = layer == 0 ? 0 : -r * 0.12f;
                    gv.pinePath.reset();
                    for (int k = 0; k < 16; k++) {
                        double ang = k * Math.PI / 8 + spin;
                        float rr = k % 2 == 0 ? lr : lr * 0.6f;
                        float px = x + ox + (float) Math.cos(ang) * rr, py = y + ox + (float) Math.sin(ang) * rr;
                        if (k == 0) gv.pinePath.moveTo(px, py);
                        else gv.pinePath.lineTo(px, py);
                    }
                    gv.pinePath.close();
                    gv.fill.setColor(alpha(layer == 0 ? 0xFF1C3E26 : 0xFF2C5C36, a));
                    c.drawPath(gv.pinePath, gv.fill);
                }
                gv.fill.setColor(alpha(0xFF447A4E, a));
                c.drawCircle(x - r * 0.15f, y - r * 0.15f, r * 0.16f, gv.fill);
                continue;
            }
            gv.fill.setColor(alpha(0xFF2C5A22, 1 - see * 0.5f));
            c.drawCircle(x, y, r, gv.fill);
            // The canopy sways gently in the breeze.
            float sway = (float) Math.sin(gv.world.time * 1.3f + t[0] * 0.07f + t[1] * 0.05f) * 0.9f * ts;
            if (gv.realistic && gv.scale > 2.5f) {
                gv.worldRenderer.drawRealTree(c, x, y, r, sway, ts, 1 - see * 0.5f, (int) (t[0] * 7 + t[1] * 13));
                continue;
            }
            gv.fill.setColor(alpha(0xFF3B742D, 1 - see * 0.5f));
            c.drawCircle(x - 1.5f * ts + sway * 0.6f, y - 1.5f * ts + sway * 0.3f, r * 0.65f, gv.fill);
            gv.fill.setColor(alpha(0xFF4C8A3A, 1 - see * 0.5f));
            c.drawCircle(x - 2.5f * ts + sway, y - 2.5f * ts + sway * 0.5f, r * 0.3f, gv.fill);
        }
    }

    /**
     * (10.26) A roof raised up in 3D, taken from the sharp close-ups of the map (where they're ready) instead of
     * the whole-map picture, which is much coarser and went blurry as soon as you zoomed in.
     */
    void sharpRoof(Canvas c, City.Building b, float s, float cx, float cy) {
        int res = gv.closeRes;
        if (res == 0) return;
        int kx0 = (int) (b.x0 / CHUNK), kx1 = (int) ((b.x1 - 0.01f) / CHUNK);
        int ky0 = (int) (b.y0 / CHUNK), ky1 = (int) ((b.y1 - 0.01f) / CHUNK);
        for (int ky = ky0; ky <= ky1; ky++)
            for (int kx = kx0; kx <= kx1; kx++) {
                android.graphics.Bitmap bm = gv.chunks.get(((long) res << 40) | ((long) ky << 20) | kx);
                if (bm == null) continue;
                float ox = kx * CHUNK, oy = ky * CHUNK;
                float ax0 = Math.max(b.x0, ox), ay0 = Math.max(b.y0, oy), ax1 = Math.min(b.x1, ox + CHUNK), ay1 = Math.min(b.y1, oy + CHUNK);
                if (ax1 <= ax0 || ay1 <= ay0) continue;
                gv.roofSrc.set((int) ((ax0 - ox) * res), (int) ((ay0 - oy) * res),
                        Math.min(bm.getWidth(), (int) Math.ceil((ax1 - ox) * res)), Math.min(bm.getHeight(), (int) Math.ceil((ay1 - oy) * res)));
                gv.roofDst.set(cx + (ax0 - cx) * s, cy + (ay0 - cy) * s, cx + (ax1 - cx) * s, cy + (ay1 - cy) * s);
                c.drawBitmap(bm, gv.roofSrc, gv.roofDst, gv.bmpPaint);
            }
    }

    /** Gunfire inside lights up the windows: a flicker along the walls, seen from anywhere. */
    void drawGunGlow(Canvas c, City.Building b, float x0, float y0, float x1, float y1) {
        int k = (int) (gv.world.time * 30) + b.seed;
        float w = x1 - x0, h = y1 - y0;
        for (int n = 0; n < 2; n++) {
            int side = (k + n * 3) & 3;
            float t = ((k * 37 + n * 101) % 100) / 100f;
            float px = side < 2 ? x0 + 3 + t * (w - 6) : side == 2 ? x0 + 1.5f : x1 - 1.5f;
            float py = side >= 2 ? y0 + 3 + t * (h - 6) : side == 0 ? y0 + 1.5f : y1 - 1.5f;
            gv.fill.setColor(0x80FFD27A);
            c.drawCircle(px, py, 5, gv.fill);
            gv.fill.setColor(0xF0FFF4C8);
            c.drawCircle(px, py, 1.8f, gv.fill);
        }
    }

    /** Cracks and scorch marks on the roof of a building that has taken blast damage. */
    void drawDamage(Canvas c, City.Building b, float x0, float y0, float x1, float y1, float a) {
        float dmg = b.maxHp > 0 ? 1 - b.hp / b.maxHp : 0;
        if (a > 0.05f) drawWear(c, b, x0, y0, x1, y1, a, dmg);
        if (dmg < 0.12f || a <= 0.05f) return;
        float w = x1 - x0, h = y1 - y0;
        int n = 2 + (int) (dmg * 9);
        int hsh = b.seed * 1103515245 + 12345;
        for (int k = 0; k < n; k++) {
            hsh = hsh * 1103515245 + 12345;
            float u = ((hsh >>> 8) & 1023) / 1023f;
            hsh = hsh * 1103515245 + 12345;
            float v = ((hsh >>> 8) & 1023) / 1023f;
            hsh = hsh * 1103515245 + 12345;
            float r = 2 + ((hsh >>> 8) & 255) / 255f * 5 * dmg;
            gv.fill.setColor(alpha(0x80141210, a * Math.min(1, dmg * 1.6f)));
            c.drawCircle(x0 + u * w, y0 + v * h, r, gv.fill);
            gv.stroke.setColor(alpha(0xC0201C18, a));
            gv.stroke.setStrokeWidth(0.8f);
            float ex = x0 + u * w + (((hsh >>> 4) & 15) - 7.5f) * 1.6f, ey = y0 + v * h + (((hsh >>> 12) & 15) - 7.5f) * 1.6f;
            c.drawLine(x0 + u * w, y0 + v * h, Math.max(x0, Math.min(x1, ex)), Math.max(y0, Math.min(y1, ey)), gv.stroke);
        }
        if (dmg > 0.6f && gv.world.rnd.nextFloat() < 0.15f) {
            // Still smouldering.
            gv.world.particle(x0 + gv.world.rnd.nextFloat() * w, y0 + gv.world.rnd.nextFloat() * h, 3, -8, 1.5f, 2.5f, 0xFF3A3A3A,
                    World.P_SMOKE);
        }
    }

    // ------------------------------------------------------------------ wear and tear (10.17)

    /**
     * A building shows what it's been through, in stages: windows broken (in a fight inside, a blast, a
     * shop smashed in), soot spreading over it after a fire, and at worst the roof caved in, with the beams
     * showing and rubble below.
     */
    void drawWear(Canvas c, City.Building b, float x0, float y0, float x1, float y1, float a, float dmg) {
        float w = x1 - x0, h = y1 - y0;
        if (w < 4 || h < 4) return;
        int hsh = b.seed * 69069 + 1;
        // Broken windows round the edge of the roof: dark holes, a glint of glass.
        int windows = (int) (b.shotUp * 8 + (b.smashed ? 4 : 0) + (dmg > 0.12f ? dmg * 8 : 0) + b.scorch * 4);
        for (int k = 0; k < windows; k++) {
            hsh = hsh * 1103515245 + 12345;
            int side = (hsh >>> 20) & 3;
            float t = 0.12f + ((hsh >>> 8) & 1023) / 1023f * 0.76f;
            float px = side < 2 ? x0 + t * w : side == 2 ? x0 + 1.4f : x1 - 1.4f;
            float py = side >= 2 ? y0 + t * h : side == 0 ? y0 + 1.4f : y1 - 1.4f;
            boolean across = side < 2;
            float hw = across ? 2.6f : 1.3f, hh = across ? 1.3f : 2.6f;
            if (b.scorch > 0.3f) {
                // Soot licked up out of a burnt-out window.
                gv.fill.setColor(alpha(0xFF141110, a * 0.35f * b.scorch));
                c.drawCircle(px, py, 4.5f, gv.fill);
            }
            gv.fill.setColor(alpha(0xF00C0F12, a));
            c.drawRect(px - hw, py - hh, px + hw, py + hh, gv.fill);
            // ((11.5) The glass only close enough to see it.)
            if (gv.scale < 1.2f) continue;
            // Jagged glass left in the frame.
            gv.stroke.setColor(alpha(0xD0CFE8F2, a));
            gv.stroke.setStrokeWidth(0.45f);
            c.drawLine(px - hw, py - hh, px - hw * 0.2f, py + hh * 0.1f, gv.stroke);
            c.drawLine(px + hw, py + hh, px + hw * 0.3f, py - hh * 0.2f, gv.stroke);
        }
        // Soot from a fire: blotches spreading from where it burned, the roof edge blackened at the end.
        if (b.scorch > 0.05f) {
            int blots = 3 + (int) (b.scorch * 7);
            hsh = b.seed * 31 + 7;
            for (int k = 0; k < blots; k++) {
                hsh = hsh * 1103515245 + 12345;
                float u = ((hsh >>> 8) & 1023) / 1023f, v = ((hsh >>> 18) & 1023) / 1023f;
                float r = (3 + ((hsh >>> 4) & 15) / 15f * 6) * (0.4f + b.scorch);
                // (Soft-edged: rings of thinner soot round a darker middle.)
                float sa = a * Math.min(0.75f, b.scorch * 0.9f);
                for (int ring = 0; ring < 3; ring++) {
                    gv.fill.setColor(alpha(0xFF141110, sa * 0.32f));
                    c.drawCircle(x0 + u * w, y0 + v * h, r * (1 - ring * 0.3f), gv.fill);
                }
            }
            if (b.scorch > 0.5f) {
                gv.stroke.setColor(alpha(0xFF0E0C0B, a * (b.scorch - 0.4f)));
                gv.stroke.setStrokeWidth(1.6f);
                c.drawRect(x0 + 0.8f, y0 + 0.8f, x1 - 0.8f, y1 - 0.8f, gv.stroke);
            }
        }
        // The roof caved in: a ragged hole, beams across it, rubble round it.
        float cave = Math.max(dmg > 0.5f ? (dmg - 0.5f) * 2 : 0, b.scorch > 0.85f ? (b.scorch - 0.85f) * 6 : 0);
        if (cave > 0) {
            cave = Math.min(1, cave);
            hsh = b.seed * 7919 + 3;
            hsh = hsh * 1103515245 + 12345;
            float cx = x0 + w * (0.35f + ((hsh >>> 8) & 255) / 255f * 0.3f);
            hsh = hsh * 1103515245 + 12345;
            float cy = y0 + h * (0.35f + ((hsh >>> 8) & 255) / 255f * 0.3f);
            float rx = w * (0.12f + 0.22f * cave), ry = h * (0.12f + 0.22f * cave);
            gv.wearPath.reset();
            for (int k = 0; k < 10; k++) {
                hsh = hsh * 1103515245 + 12345;
                double ang = k * Math.PI / 5;
                float rr = 0.7f + ((hsh >>> 8) & 255) / 255f * 0.45f;
                float px = cx + (float) Math.cos(ang) * rx * rr, py = cy + (float) Math.sin(ang) * ry * rr;
                if (k == 0) gv.wearPath.moveTo(px, py);
                else gv.wearPath.lineTo(px, py);
            }
            gv.wearPath.close();
            gv.fill.setColor(alpha(0xF0181412, a));
            c.drawPath(gv.wearPath, gv.fill);
            gv.stroke.setColor(alpha(0xFF6E4E30, a));
            gv.stroke.setStrokeWidth(0.9f);
            for (int k = -1; k <= 1; k++)
                c.drawLine(cx - rx * 0.9f, cy + k * ry * 0.45f - ry * 0.1f, cx + rx * 0.9f, cy + k * ry * 0.45f + ry * 0.1f, gv.stroke);
            for (int k = 0; k < 14; k++) {
                hsh = hsh * 1103515245 + 12345;
                double ang = ((hsh >>> 8) & 1023) / 1023f * Math.PI * 2;
                float rr = 1 + ((hsh >>> 18) & 255) / 255f * 0.5f;
                gv.fill.setColor(alpha(k % 2 == 0 ? 0xFF8A8478 : 0xFF6A645A, a));
                float px = cx + (float) Math.cos(ang) * rx * rr, py = cy + (float) Math.sin(ang) * ry * rr;
                float rs = 1.2f + ((hsh >>> 4) & 7) * 0.35f;
                c.drawRect(px - rs, py - rs * 0.7f, px + rs, py + rs * 0.7f, gv.fill);
            }
        }
    }

    /** A door the dead broke in: hanging off, splinters on the step (boarded back up once it's been fixed). */
    void drawBrokenDoor(Canvas c, City.Building b) {
        float x = b.doorX, y = b.doorY;
        if (b.barricade >= 60) {
            // Patched up: planks nailed across the gap.
            gv.fill.setColor(0xFF8A6238);
            c.drawRect(x - 4.5f, y - 2.2f, x + 4.5f, y - 1f, gv.fill);
            c.drawRect(x - 4f, y + 0.2f, x + 4f, y + 1.4f, gv.fill);
            gv.fill.setColor(0xFF4A3420);
            c.drawCircle(x - 3.5f, y - 1.6f, 0.35f, gv.fill);
            c.drawCircle(x + 3.5f, y + 0.8f, 0.35f, gv.fill);
            return;
        }
        gv.fill.setColor(0xE0141210);
        c.drawRect(x - 3.2f, y - 1.6f, x + 3.2f, y + 1.6f, gv.fill);
        // The door itself, knocked askew.
        gv.stroke.setColor(0xFF7A5634);
        gv.stroke.setStrokeWidth(1.4f);
        c.drawLine(x - 3.6f, y + 1.8f, x + 1.8f, y + 3.6f, gv.stroke);
        gv.fill.setColor(0xFFB08A5C);
        int hsh = b.seed;
        for (int k = 0; k < 6; k++) {
            hsh = hsh * 1103515245 + 12345;
            float u = (((hsh >>> 8) & 255) / 255f - 0.5f) * 9, v = 2 + ((hsh >>> 16) & 255) / 255f * 4;
            c.drawRect(x + u - 0.6f, y + v - 0.2f, x + u + 0.6f, y + v + 0.2f, gv.fill);
        }
    }

    // ------------------------------------------------------------------ marks on the ground (10.17)

    /** Footprints, blood trails, drag marks and spent cases, fading with age. */
    void drawMarks(Canvas c, float vx0, float vy0, float vx1, float vy1) {
        boolean close = gv.scale > 1.8f;
        float now = gv.world.time;
        gv.stroke.setStrokeWidth(1.5f);
        for (int i = 0; i < gv.world.mcount; i++) {
            float x = gv.world.mx[i], y = gv.world.my[i];
            if (x < vx0 || x > vx1 || y < vy0 || y > vy1) continue;
            byte kind = gv.world.mkind[i];
            float age = (now - gv.world.mborn[i]) / World.MARK_LIFE[kind];
            if (age >= 1 || age < 0) continue;
            float f = 1 - age;
            float ca = (float) Math.cos(gv.world.mang[i]), sa = (float) Math.sin(gv.world.mang[i]);
            switch (kind) {
                case World.M_FOOT:
                    // Toe and heel.
                    gv.fill.setColor(alpha(0x70302418, f));
                    c.drawCircle(x + ca * 0.75f, y + sa * 0.75f, 0.62f, gv.fill);
                    c.drawCircle(x - ca * 0.55f, y - sa * 0.55f, 0.48f, gv.fill);
                    break;
                case World.M_PAW:
                    gv.fill.setColor(alpha(0x60302418, f));
                    c.drawCircle(x, y, 0.5f, gv.fill);
                    c.drawCircle(x + ca * 0.8f - sa * 0.35f, y + sa * 0.8f + ca * 0.35f, 0.25f, gv.fill);
                    c.drawCircle(x + ca * 0.8f + sa * 0.35f, y + sa * 0.8f - ca * 0.35f, 0.25f, gv.fill);
                    break;
                case World.M_DRIP:
                    gv.fill.setColor(alpha(gv.world.mside[i] == 1 ? 0xC0600606 : 0x9A6E0A0A, f));
                    c.drawCircle(x, y, gv.world.mside[i] == 1 ? 0.9f : 0.6f, gv.fill);
                    break;
                case World.M_DRAG:
                    gv.stroke.setColor(alpha(0x70520606, f));
                    c.drawLine(x - ca * 3, y - sa * 3, x + ca * 3, y + sa * 3, gv.stroke);
                    break;
                case World.M_CASING:
                    if (!close) break;
                    // Brass, dulling as it lies there.
                    gv.fill.setColor(alpha(age < 0.3f ? 0xFFC9A442 : 0xFF8F7A48, Math.min(1, f * 2.5f)));
                    c.drawRect(x - ca * 0.45f - 0.2f, y - sa * 0.45f - 0.2f, x + ca * 0.45f + 0.2f, y + sa * 0.45f + 0.2f, gv.fill);
                    break;
                default:
                    break;
            }
        }
    }

    /** The next number from the building's own sequence, 0 to 1 (so its furniture never moves). */
    float irand() {
        gv.ihash = gv.ihash * 1103515245 + 12345;
        // ((16.12) Up to just under 1, never 1 itself: over 65535 it came to exactly 1 one time in 65536, and a pick
        // from a list by it (the colours of the goods on a supermarket's shelves) went one past the end and the game
        // stopped.)
        return ((gv.ihash >>> 8) & 0xFFFF) / 65536f;
    }

    void box(Canvas c, float l, float t, float r, float b, int color) {
        gv.fill.setColor(alpha(color, gv.ia));
        c.drawRect(l, t, r, b, gv.fill);
    }

    /**
     * What you see through a see-through roof: the floor and furniture laid out for what the building is, the
     * damage if it has been smashed or looted, and whoever (or whatever) is inside.
     */
    void drawInterior(Canvas c, City.Building b, float a) {
        float x0 = b.x0 + 1, y0 = b.y0 + 1, x1 = b.x1 - 1, y1 = b.y1 - 1, w = x1 - x0, h = y1 - y0;
        gv.ia = a;
        gv.ihash = b.seed * 69069 + 7;
        boolean wide = w >= h;
        int k = b.kind;
        int floorCol;
        switch (k) {
            case City.HOUSE: case City.BARN: floorCol = 0xFFB8946A; break;
            case City.APARTMENT: floorCol = 0xFFA88A6A; break;
            case City.CHURCH: case City.SPIRE: case City.CRYPT: floorCol = 0xFF9C8C76; break;
            case City.MARKET: case City.KIOSK: case City.PHARMACY: case City.MALL: floorCol = 0xFFDCDCD4; break;
            case City.HOSPITAL: floorCol = 0xFFD8E2E6; break;
            case City.WAREHOUSE: case City.POWER: case City.GARAGE: case City.FIRE_STATION: case City.WORKS: floorCol = 0xFF8A8A84; break;
            case City.BARRACKS: case City.TOWER: floorCol = 0xFF7E806E; break;
            case City.STATION: floorCol = 0xFFB4B8BE; break;
            case City.SCHOOL: floorCol = 0xFFC8B89A; break;
            case City.SHOP: floorCol = b.shopType == 2 ? 0xFFD8D0C0 : 0xFFC8C0B0; break;
            default: floorCol = 0xFFC4BFB4; break;
        }
        box(c, x0, y0, x1, y1, floorCol);
        int dark = City.darken(floorCol, 0.62f);
        int inside = Interiors.insideOf(b);
        Interiors.Plan plan = inside >= 0 ? planOf(b, inside) : null;
        if (plan != null) {
            drawPlannedInterior(c, b, a, x0, y0, x1, y1, k, plan);
        }
        else {
            drawFurnishings(c, b, a, x0, y0, x1, y1, w, h, wide, k, dark);
        }
        // Damage: overturned shelves and litter after looting, glass inside smashed shop windows, blood where
        // zombies have been.
        if (b.looted || b.smashed) {
            for (int n = 0; n < 8; n++) {
                float px = x0 + 3 + irand() * (w - 6), py = y0 + 3 + irand() * (h - 6);
                box(c, px, py, px + 2 + irand() * 3, py + 1 + irand() * 2, n % 2 == 0 ? dark : 0xFFE0D8C8);
            }
        }
        if (b.lurkers > 0 || b.infestKnown) {
            for (int n = 0; n < 5; n++) {
                gv.fill.setColor(alpha(0x996E0A0A, a));
                c.drawCircle(x0 + 3 + irand() * (w - 6), y0 + 3 + irand() * (h - 6), 1.5f + irand() * 2.5f, gv.fill);
            }
        }
        gv.stroke.setColor(alpha(City.darken(b.wall, 0.6f), a));
        gv.stroke.setStrokeWidth(2f);
        gv.oval.set(x0, y0, x1, y1);
        c.drawRect(gv.oval, gv.stroke);
        if (a < 0.5f) return;
        // The people inside, each at their own spot (a seat, a desk, a bed...), moving between them now
        // and then. Whoever doesn't fit on the ground floor is upstairs.
        java.util.ArrayList<float[]> spots = plan != null ? plan.spots : looseSpots(b, x0, y0, x1, y1);
        int ns = spots.size();
        if (b.fighting && ns > 1) {
            drawFightInside(c, b, spots, a);
            return;
        }
        for (int i = 0, n = Math.min(ns, b.occupants.size() + b.visitors.size()); i < n; i++) {
            Entity o = i < b.occupants.size() ? b.occupants.get(i) : b.visitors.get(i - b.occupants.size());
            placeInside(o, i, b, spots, plan);
            o.aiming = false;
            gv.entityRenderer.drawEntity(c, o, false);
        }
        // Zombies waiting in the dark: only a close look through the roof gives them away.
        for (int i = 0; i < b.lurkers; i++) {
            float lx = x0 + 5 + ((b.seed * 31 + i * 57) % Math.max(1, (int) (w - 10)));
            float ly = y0 + 5 + ((b.seed * 17 + i * 43) % Math.max(1, (int) (h - 10)));
            float sway = (float) Math.sin(gv.world.time * 1.5f + i) * 1.2f;
            gv.fill.setColor(alpha(0xFF3E5A2A, a * 0.75f));
            c.drawCircle(lx + sway, ly, 3.4f, gv.fill);
            gv.fill.setColor(alpha(0xFF7C9A5E, a * 0.75f));
            c.drawCircle(lx + sway + 0.4f, ly, 2f, gv.fill);
        }
    }

    /** ((27.3) From drawInterior.) A building without a floor plan: its furnishings laid out by kind of building. */
    void drawFurnishings(Canvas c, City.Building b, float a, float x0, float y0, float x1, float y1, float w, float h, boolean wide, int k, int dark) {
        switch (k) {
            case City.HOUSE: case City.APARTMENT: {
                // Rooms: a kitchen, a living room with a sofa and rug, bedrooms, a bathroom.
                float mx = x0 + w * (0.45f + irand() * 0.1f), my = y0 + h * (0.45f + irand() * 0.1f);
                box(c, x0 + 2, y0 + 2, mx - 2, y0 + 5, 0xFFE8E4DA); // kitchen counter
                box(c, x0 + 2, y0 + 2, x0 + 5, my - 2, 0xFFE8E4DA);
                box(c, mx + 3, y0 + 3, Math.min(x1 - 3, mx + 12), y0 + 10, 0xFF8A3A3A); // rug
                box(c, mx + 3, y0 + 2, Math.min(x1 - 3, mx + 12), y0 + 5, 0xFF4E5A7A); // sofa
                box(c, x0 + 3, my + 3, x0 + 11, my + 13, 0xFFE8E8F0); // bed
                box(c, x0 + 3, my + 3, x0 + 11, my + 6, 0xFFB0C0E0);
                box(c, x1 - 9, y1 - 7, x1 - 2, y1 - 2, 0xFFF4F4F4); // bath
                gv.stroke.setColor(alpha(City.darken(b.wall, 0.7f), a));
                gv.stroke.setStrokeWidth(1.2f);
                c.drawLine(mx, y0, mx, my - 4, gv.stroke);
                c.drawLine(mx, my + 4, mx, y1, gv.stroke);
                c.drawLine(x0, my, mx - 4, my, gv.stroke);
                c.drawLine(mx + 4, my, x1, my, gv.stroke);
                break;
            }
            case City.SHOP: case City.KIOSK: case City.PHARMACY: {
                if (k == City.SHOP && b.shopType == 2) {
                    // Diner: booths along the wall, a counter with stools.
                    for (float t = 3; t < (wide ? w : h) - 8; t += 9) {
                        float u = (wide ? x0 : y0) + t;
                        if (wide) {
                            box(c, u, y0 + 2, u + 7, y0 + 4, 0xFFB83A30);
                            box(c, u + 1, y0 + 4, u + 6, y0 + 8, 0xFFE8E0D0);
                            box(c, u, y0 + 8, u + 7, y0 + 10, 0xFFB83A30);
                        } else {
                            box(c, x0 + 2, u, x0 + 4, u + 7, 0xFFB83A30);
                            box(c, x0 + 4, u + 1, x0 + 8, u + 6, 0xFFE8E0D0);
                            box(c, x0 + 8, u, x0 + 10, u + 7, 0xFFB83A30);
                        }
                    }
                    box(c, wide ? x0 + 3 : x1 - 6, wide ? y1 - 6 : y0 + 3, wide ? x1 - 3 : x1 - 3, wide ? y1 - 3 : y1 - 3, 0xFF6A4A30);
                } else {
                    // Shelves round the walls, a counter and till by the door.
                    int shelf = k == City.PHARMACY ? 0xFFF2F2F2 : dark;
                    box(c, x0 + 2, y0 + 2, x1 - 2, y0 + 4, shelf);
                    box(c, x0 + 2, y0 + 2, x0 + 4, y1 - 2, shelf);
                    box(c, x1 - 4, y0 + 2, x1 - 2, y1 - 2, shelf);
                    // Goods on the top shelf (fewer once the place has been looted).
                    int[] goods = {0xFFE05050, 0xFF5080E0, 0xFFE0C050, 0xFF50B060};
                    for (float u = x0 + 3; u < x1 - 3; u += 2.5f)
                        if (!b.looted || irand() < 0.2f) box(c, u, y0 + 2.3f, u + 1.6f, y0 + 3.8f, goods[(int) (irand() * 4) % 4]);
                    box(c, x0 + w * 0.3f, y1 - 7, x0 + w * 0.7f, y1 - 4, 0xFF6A4A30);
                }
                break;
            }
            case City.MARKET: case City.MALL: {
                // Aisles of colourful shelves, checkouts at the front. The mall has a fountain in its atrium.
                int[] goods = {0xFFE05050, 0xFF5080E0, 0xFFE0C050, 0xFF50B060, 0xFFE08840};
                for (float t = y0 + 8; t < y1 - 12; t += 9) {
                    box(c, x0 + 8, t, x1 - 8, t + 3, dark);
                    for (float u = x0 + 9; u < x1 - 9; u += 3)
                        box(c, u, t + 0.5f, u + 2, t + 2.5f, goods[(int) (irand() * goods.length)]);
                }
                for (float u = x0 + 8; u < x1 - 10; u += 12) box(c, u, y1 - 9, u + 6, y1 - 6, 0xFF5A5E66);
                if (k == City.MALL) {
                    float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
                    box(c, cx - 14, cy - 14, cx + 14, cy + 14, 0xFFE8E4DA);
                    gv.fill.setColor(alpha(0xFF6FA8DC, a));
                    c.drawCircle(cx, cy, 8, gv.fill);
                }
                break;
            }
            case City.HOSPITAL: {
                // Wards: rows of beds with blue sheets, a nurses' station.
                for (float t = y0 + 4; t < y1 - 10; t += 11)
                    for (float u = x0 + 4; u < x1 - 8; u += 10) {
                        box(c, u, t, u + 6, t + 8, 0xFFF4F4F4);
                        box(c, u, t + 3, u + 6, t + 8, 0xFF7AA8D8);
                    }
                box(c, x0 + w * 0.4f, y1 - 8, x0 + w * 0.6f, y1 - 3, 0xFFD83A3A);
                break;
            }
            case City.STATION: {
                // Desks up front, holding cells with bars at the back.
                for (float u = x0 + 5; u < x1 - 8; u += 10) box(c, u, y1 - 12, u + 6, y1 - 8, dark);
                float cy = y0 + h * 0.35f;
                box(c, x0 + 2, y0 + 2, x1 - 2, cy, 0xFF9A9EA4);
                gv.stroke.setColor(alpha(0xFF3A3E44, a));
                gv.stroke.setStrokeWidth(0.6f);
                for (float u = x0 + 3; u < x1 - 2; u += 2.5f) c.drawLine(u, cy - 2, u, cy, gv.stroke);
                for (float u = x0 + w / 3; u < x1 - 2; u += w / 3) c.drawLine(u, y0 + 2, u, cy, gv.stroke);
                break;
            }
            case City.SCHOOL: {
                // Classrooms of little desks facing the board.
                for (float t = y0 + 6; t < y1 - 6; t += 7)
                    for (float u = x0 + 6; u < x1 - 6; u += 7) box(c, u, t, u + 4, t + 3, 0xFFB08A5A);
                box(c, x0 + 3, y0 + 2, x0 + w * 0.5f, y0 + 3.5f, 0xFF2E4A36);
                break;
            }
            case City.CHURCH: case City.SPIRE: {
                for (float t = 6; t < (wide ? w : h) - 8; t += 5) {
                    if (wide) {
                        box(c, x0 + t, y0 + 4, x0 + t + 2, y0 + h / 2 - 3, dark);
                        box(c, x0 + t, y0 + h / 2 + 3, x0 + t + 2, y1 - 4, dark);
                    } else {
                        box(c, x0 + 4, y0 + t, x0 + w / 2 - 3, y0 + t + 2, dark);
                        box(c, x0 + w / 2 + 3, y0 + t, x1 - 4, y0 + t + 2, dark);
                    }
                }
                box(c, wide ? x1 - 6 : x0 + w / 2 - 4, wide ? y0 + h / 2 - 4 : y1 - 6, wide ? x1 - 3 : x0 + w / 2 + 4,
                        wide ? y0 + h / 2 + 4 : y1 - 3, 0xFFE8D24A);
                break;
            }
            case City.WAREHOUSE: case City.BARN: {
                // Pallets and crates (hay bales in a barn), and a forklift.
                int crate = k == City.BARN ? 0xFFD8B860 : 0xFFA0784A;
                for (float t = y0 + 4; t < y1 - 8; t += 9)
                    for (float u = x0 + 4; u < x1 - 8; u += 9)
                        if (irand() < 0.7f) box(c, u, t, u + 6, t + 6, crate);
                box(c, x0 + w * 0.6f, y1 - 9, x0 + w * 0.6f + 5, y1 - 4, 0xFFE8B830);
                break;
            }
            case City.BARRACKS: {
                for (float t = y0 + 3; t < y1 - 6; t += 7)
                    for (float u = x0 + 3; u < x1 - 9; u += 11) {
                        box(c, u, t, u + 8, t + 4, 0xFF5B6B3A);
                        box(c, u, t, u + 2, t + 4, 0xFFE8E8E0);
                    }
                break;
            }
            case City.POWER: {
                for (int n = 0; n < 3; n++) {
                    gv.fill.setColor(alpha(0xFF6A7078, a));
                    c.drawCircle(x0 + w * (0.2f + n * 0.3f), (y0 + y1) / 2, Math.min(w, h) * 0.14f, gv.fill);
                    gv.fill.setColor(alpha(0xFF9AA0A8, a));
                    c.drawCircle(x0 + w * (0.2f + n * 0.3f), (y0 + y1) / 2, Math.min(w, h) * 0.07f, gv.fill);
                }
                break;
            }
            default: {
                // Offices: desk clusters with glowing screens, a meeting room and a few plants.
                for (float t = y0 + 5; t < y1 - 8; t += 12)
                    for (float u = x0 + 5; u < x1 - 10; u += 13) {
                        if (irand() < 0.2f) continue;
                        box(c, u, t, u + 8, t + 4, dark);
                        box(c, u + 1, t + 0.5f, u + 3, t + 1.5f, 0xFF7FB0D8);
                        box(c, u + 5, t + 0.5f, u + 7, t + 1.5f, 0xFF7FB0D8);
                    }
                if (w > 30 && h > 30) {
                    gv.stroke.setColor(alpha(0xFF9CC3D9, a));
                    gv.stroke.setStrokeWidth(1f);
                    c.drawRect(x1 - w * 0.3f, y0 + 2, x1 - 2, y0 + h * 0.3f, gv.stroke);
                }
                for (int n = 0; n < 3; n++) {
                    gv.fill.setColor(alpha(0xFF3E8A34, a));
                    c.drawCircle(x0 + 3 + irand() * (w - 6), y0 + 3 + irand() * (h - 6), 1.8f, gv.fill);
                }
                break;
            }
        }
    }

    /** ((27.3) From drawInterior.) A building with a floor plan: its rooms, furniture and doors. */
    void drawPlannedInterior(Canvas c, City.Building b, float a, float x0, float y0, float x1, float y1, int k, Interiors.Plan plan) {
        gv.planWalls = gv.world.city.enterable(b) ? b.walls : null;
        drawPlan(c, plan, a);
        gv.planWalls = null;
        // (10.53) Windows along the outside walls, a pane every few steps (none on a warehouse or a barn).
        if (k != City.WAREHOUSE && k != City.BARN && k != City.GARAGE) {
            gv.fill.setColor(alpha(0xFF9CC4DA, a));
            float pane = 4.5f, step = 10;
            for (float u = x0 + 4; u + pane < x1 - 3; u += step) {
                c.drawRect(u, y0 - 0.6f, u + pane, y0 + 0.9f, gv.fill);
                c.drawRect(u, y1 - 0.9f, u + pane, y1 + 0.6f, gv.fill);
            }
            for (float u = y0 + 4; u + pane < y1 - 3; u += step) {
                c.drawRect(x0 - 0.6f, u, x0 + 0.9f, u + pane, gv.fill);
                c.drawRect(x1 - 0.9f, u, x1 + 0.6f, u + pane, gv.fill);
            }
        }
    }

    /**
     * A fight inside, seen through the roof: the dead have the rooms by the door, the people inside are
     * pushed back to the far end, facing them, with guns flashing.
     */
    void drawFightInside(Canvas c, final City.Building b, final java.util.ArrayList<float[]> spots, float a) {
        Integer[] order = doorOrder(b, spots);
        if (gv.puppets == null) {
            gv.puppets = new Entity[10];
            for (int i = 0; i < gv.puppets.length; i++) gv.puppets[i] = gv.world.puppetZombie(i);
        }
        int ns = order.length;
        int dead = Math.min(Math.min(gv.puppets.length, b.lurkers), Math.max(1, ns / 2));
        float fx = 0, fy = 0;
        for (int i = 0; i < dead; i++) {
            float[] sp = spots.get(order[i]);
            Entity z = gv.puppets[i];
            // Shuffling forwards, pulled back, forwards again.
            float lunge = (float) Math.sin(gv.world.time * 2.2f + i * 1.7f) * 2.5f;
            float[] far = spots.get(order[ns - 1]);
            float dx = far[0] - sp[0], dy = far[1] - sp[1], d = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
            z.x = sp[0] + dx / d * lunge;
            z.y = sp[1] + dy / d * lunge;
            z.angle = (float) Math.atan2(dy, dx);
            z.phase = gv.world.time * 3 + i;
            gv.entityRenderer.drawEntity(c, z, false);
            fx += z.x;
            fy += z.y;
        }
        if (dead > 0) {
            fx /= dead;
            fy /= dead;
        }
        int people = Math.min(b.occupants.size(), ns - dead);
        for (int i = 0; i < people; i++) {
            Entity o = b.occupants.get(i);
            float[] sp = spots.get(order[ns - 1 - i]);
            o.x = sp[0];
            o.y = sp[1];
            o.angle = (float) Math.atan2(fy - o.y, fx - o.x);
            o.aiming = o.canShoot() && dead > 0;
            gv.entityRenderer.drawEntity(c, o, false);
            if (o.aiming && b.flash > 0 && (i & 1) == ((int) (gv.world.time * 20) & 1)) {
                float ca = (float) Math.cos(o.angle), sa = (float) Math.sin(o.angle);
                gv.fill.setColor(alpha(0xFFFFE08A, a));
                c.drawCircle(o.x + ca * 7, o.y + sa * 7, 2.2f, gv.fill);
                gv.stroke.setColor(alpha(0x99FFF2C0, a));
                gv.stroke.setStrokeWidth(0.6f);
                c.drawLine(o.x + ca * 7, o.y + sa * 7, fx, fy, gv.stroke);
            }
        }
    }

    Interiors.Plan planOf(City.Building b, int inside) {
        // ((17.1) The city's own: the same rooms and walls everyone walks through.)
        return gv.world.city.planOf(b);
    }

    /** Floors, furniture and inside walls of a floor plan. */
    void drawPlan(Canvas c, Interiors.Plan p, float a) {
        // (10.53) Floors with a grain to them: boards in the homes and hotel rooms, tiles in the kitchens and
        // bathrooms, carpet tiles in the offices; then the furniture standing up off them, the walls with their
        // doors, and windows round the outside.
        for (int i = 0, n = p.rooms.size(); i < n; i++) {
            float[] r = p.rooms.get(i);
            int kind = (int) r[4], col = Interiors.floorOf(kind);
            box(c, r[0], r[1], r[2], r[3], col);
            floorGrain(c, r[0], r[1], r[2], r[3], kind, col, a);
        }
        for (int i = 0, n = p.items.size(); i < n; i++) {
            float[] it = p.items.get(i);
            int col = (int) it[4];
            if (it[5] > 0) {
                float x = (it[0] + it[2]) / 2, y = (it[1] + it[3]) / 2, rad = (it[2] - it[0]) / 2;
                gv.fill.setColor(alpha(0x33000000, a));
                c.drawCircle(x + 0.7f, y + 0.9f, rad, gv.fill);
                gv.fill.setColor(alpha(col, a));
                c.drawCircle(x, y, rad, gv.fill);
                gv.fill.setColor(alpha(City.lighten(col, 0.25f), a));
                c.drawCircle(x - rad * 0.3f, y - rad * 0.3f, rad * 0.45f, gv.fill);
            } else {
                box(c, it[0] + 0.7f, it[1] + 0.9f, it[2] + 0.7f, it[3] + 0.9f, 0x33000000);
                box(c, it[0], it[1], it[2], it[3], col);
                // A lit edge along the top and left, a shaded one along the bottom: it stands up off the floor.
                float e = Math.min(0.9f, Math.min(it[2] - it[0], it[3] - it[1]) * 0.18f);
                if (e > 0.25f) {
                    box(c, it[0], it[1], it[2], it[1] + e, City.lighten(col, 0.22f));
                    box(c, it[0], it[1], it[0] + e, it[3], City.lighten(col, 0.12f));
                    box(c, it[0], it[3] - e, it[2], it[3], City.darken(col, 0.78f));
                }
            }
        }
        // Walls: a dark edge and a lighter top, like a wall seen from above.
        for (int pass = 0; pass < 2; pass++) {
            gv.stroke.setColor(alpha(pass == 0 ? 0xFF3A3430 : 0xFF6E655C, a));
            gv.stroke.setStrokeWidth(pass == 0 ? 1.6f : 0.7f);
            // ((17.1) The walls people walk round, the outside ones with the doorway in them included.)
            if (gv.planWalls != null)
                for (int i = 0; i + 3 < gv.planWalls.length; i += 4) c.drawLine(gv.planWalls[i], gv.planWalls[i + 1], gv.planWalls[i + 2], gv.planWalls[i + 3], gv.stroke);
            else
                for (int i = 0, n = p.walls.size(); i < n; i++) {
                    float[] wl = p.walls.get(i);
                    c.drawLine(wl[0], wl[1], wl[2], wl[3], gv.stroke);
                }
        }
        // Each room's door, swung open into it.
        gv.stroke.setColor(alpha(0x99584C40, a));
        gv.stroke.setStrokeWidth(0.5f);
        for (int i = 0, n = p.rooms.size(); i < n; i++) {
            float[] r = p.rooms.get(i);
            if (r.length < 7 || r[5] <= 0) continue;
            float dx = r[5], dy = r[6];
            if (dx < r[0] - 3 || dx > r[2] + 3 || dy < r[1] - 3 || dy > r[3] + 3) continue;
            // (Which wall it's in: the nearest edge of the room.)
            float dl = Math.abs(dx - r[0]), dr = Math.abs(dx - r[2]), dt = Math.abs(dy - r[1]), db = Math.abs(dy - r[3]);
            float m = Math.min(Math.min(dl, dr), Math.min(dt, db)), len = 3.2f;
            float hx = dx, hy = dy, ox = 0, oy = 0;
            if (m == dl) { hx = r[0]; ox = 1; }
            else if (m == dr) { hx = r[2]; ox = -1; }
            else if (m == dt) { hy = r[1]; oy = 1; }
            else { hy = r[3]; oy = -1; }
            float px = hx - oy * len * 0.5f, py = hy - ox * len * 0.5f;
            c.drawLine(px, py, px + ox * len, py + oy * len, gv.stroke);
            int steps = 5;
            float lx = px + ox * len, ly = py + oy * len;
            for (int k = 1; k <= steps; k++) {
                double an = Math.PI / 2 * k / steps;
                float qx = px + (float) (ox * Math.cos(an) + oy * Math.sin(an)) * len, qy = py + (float) (oy * Math.cos(an) + ox * Math.sin(an)) * len;
                c.drawLine(lx, ly, qx, qy, gv.stroke);
                lx = qx;
                ly = qy;
            }
        }
    }

    /** (10.53) The look of a floor: boards, tiles or carpet squares, faintly lined over its colour. */
    void floorGrain(Canvas c, float x0, float y0, float x1, float y1, int kind, int col, float a) {
        int style;
        switch (kind) {
            case Interiors.LIVING: case Interiors.BEDROOM: case Interiors.DINING: case Interiors.HOTELROOM:
            case Interiors.BARROOM: case Interiors.BOOKS: case Interiors.DORMROOM: case Interiors.GYMFLOOR: style = 1; break;
            case Interiors.KITCHEN: case Interiors.BATH: case Interiors.COOKING: case Interiors.LAUNDRY: case Interiors.BAKERY:
            case Interiors.LAB: case Interiors.WARD: case Interiors.SALON: style = 2; break;
            case Interiors.OPENPLAN: case Interiors.MEETING: case Interiors.CLASS: case Interiors.LOBBY: style = 3; break;
            default: return;
        }
        float w = x1 - x0, h = y1 - y0;
        if (w < 4 || h < 4) return;
        gv.stroke.setColor(alpha(City.darken(col, style == 2 ? 0.86f : 0.9f), a * (style == 3 ? 0.5f : 0.75f)));
        gv.stroke.setStrokeWidth(0.3f);
        if (style == 1) {
            // Boards along the room's long side, their ends staggered.
            boolean along = w >= h;
            float gap = 2.2f;
            int row = 0;
            for (float v = (along ? y0 : x0) + gap; v < (along ? y1 : x1); v += gap, row++) {
                if (along) c.drawLine(x0, v, x1, v, gv.stroke);
                else c.drawLine(v, y0, v, y1, gv.stroke);
                float off = (row * 7.3f) % 9;
                for (float u = (along ? x0 : y0) + off; u < (along ? x1 : y1); u += 9) {
                    if (along) c.drawLine(u, v - gap, u, v, gv.stroke);
                    else c.drawLine(v - gap, u, v, u, gv.stroke);
                }
            }
        } else {
            float g = style == 2 ? 3f : 6f;
            for (float x = x0 + g; x < x1; x += g) c.drawLine(x, y0, x, y1, gv.stroke);
            for (float y = y0 + g; y < y1; y += g) c.drawLine(x0, y, x1, y, gv.stroke);
        }
    }

    /** Spots spread over the floor of a building without a plan (a warehouse floor, a barracks...). */
    java.util.ArrayList<float[]> looseSpots(City.Building b, float x0, float y0, float x1, float y1) {
        java.util.ArrayList<float[]> s = gv.loose.get(b);
        if (s != null) return s;
        if (gv.loose.size() > 400) gv.loose.clear();
        s = new java.util.ArrayList<float[]>();
        java.util.Random r = new java.util.Random(b.seed);
        for (float y = y0 + 6; y < y1 - 4; y += 12)
            for (float x = x0 + 6; x < x1 - 4; x += 12)
                s.add(new float[]{x + r.nextFloat() * 4 - 2, y + r.nextFloat() * 4 - 2, 0, r.nextFloat() * 6.28f});
        gv.loose.put(b, s);
        return s;
    }

    /**
     * Where someone inside is right now: at one spot for a while, then walking to the next (through the
     * doorways if it's in another room). Everyone in a building moves on the same beat to different spots,
     * so nobody ends up on top of anyone else.
     */
    /** The spots inside a building, nearest the door first. */
    Integer[] doorOrder(final City.Building b, final java.util.ArrayList<float[]> spots) {
        Integer[] order = gv.byDoor.get(b);
        if (order == null || order.length != spots.size()) {
            if (gv.byDoor.size() > 100) gv.byDoor.clear();
            order = new Integer[spots.size()];
            for (int i = 0; i < order.length; i++) order[i] = i;
            java.util.Arrays.sort(order, new java.util.Comparator<Integer>() {
                public int compare(Integer p, Integer q) {
                    float[] u = spots.get(p), v = spots.get(q);
                    return Float.compare(Math.abs(u[0] - b.doorX) + Math.abs(u[1] - b.doorY), Math.abs(v[0] - b.doorX) + Math.abs(v[1] - b.doorY));
                }
            });
            gv.byDoor.put(b, order);
        }
        return order;
    }

    void placeInside(Entity o, int i, City.Building b, java.util.ArrayList<float[]> spots, Interiors.Plan plan) {
        int n = spots.size();
        // ((11.5) The dead at the door: two of them put their backs to it (the strongest, or whoever has a gun),
        // everyone else gets as far from it as the place allows and keeps still - no wandering from room to room.)
        if (i < b.occupants.size() && b.barricade < 99 && b.calmTimer < 2 && n > 2) {
            Integer[] order = doorOrder(b, spots);
            float cx = (b.x0 + b.x1) / 2, cy = (b.y0 + b.y1) / 2;
            float ix = cx - b.doorX, iy = cy - b.doorY, il = (float) Math.sqrt(ix * ix + iy * iy) + 0.001f;
            ix /= il;
            iy /= il;
            if (i < 2) {
                float side = i == 0 ? 3.5f : -3.5f;
                o.x = b.doorX + ix * 6 - iy * side;
                o.y = b.doorY + iy * 6 + ix * side;
                o.angle = (float) Math.atan2(-iy, -ix);
                // (Shoving back as the door shakes.)
                float shove = (float) Math.sin(gv.world.time * 9 + i * 2) * (b.barricade < 50 ? 1.2f : 0.6f);
                o.x -= ix * shove;
                o.y -= iy * shove;
                return;
            }
            float[] sp = spots.get(order[Math.max(0, n - 1 - (i - 2) % Math.max(1, n - 2))]);
            o.x = sp[0];
            o.y = sp[1];
            o.angle = (float) Math.atan2(b.doorY - sp[1], b.doorX - sp[0]);
            return;
        }
        // ((11.5) With the city under attack, those sheltering here move about a lot less.)
        float cycle = (14 + (b.seed & 7)) * (gv.world.alert >= 1 && i < b.occupants.size() ? 2.5f : 1);
        float t = gv.world.time + (b.seed & 255) * 0.13f;
        int k = (int) (t / cycle);
        float f = t / cycle - k;
        int off = (b.seed >>> 4) & 1023;
        float[] A = spots.get((i + k * 3 + off) % n), B = spots.get((i + (k + 1) * 3 + off) % n);
        // Some stay put through a whole cycle (sitting at a desk, in bed, reading).
        boolean stays = ((o.nameSeed >>> (k & 15)) & 3) == 0;
        float walkFrom = 0.7f + ((o.nameSeed & 15) / 15f) * 0.12f;
        if (stays || f < walkFrom || A == B) {
            o.x = A[0];
            o.y = A[1];
            o.angle = A[3];
            if (stays) {
                B = A;
            }
            return;
        }
        float g = (f - walkFrom) / (1 - walkFrom);
        float px, py, qx, qy;
        if (plan != null && A[2] != B[2]) {
            // Out through this room's doorway and in through the other's.
            float[] ra = plan.rooms.get((int) A[2]), rb = plan.rooms.get((int) B[2]);
            float[][] pts = {{A[0], A[1]}, {ra[5], ra[6]}, {rb[5], rb[6]}, {B[0], B[1]}};
            float seg = g * 3;
            int si = Math.min(2, (int) seg);
            float u = seg - si;
            px = pts[si][0];
            py = pts[si][1];
            qx = pts[si + 1][0];
            qy = pts[si + 1][1];
            o.x = px + (qx - px) * u;
            o.y = py + (qy - py) * u;
        } else {
            px = A[0];
            py = A[1];
            qx = B[0];
            qy = B[1];
            o.x = px + (qx - px) * g;
            o.y = py + (qy - py) * g;
        }
        if (Math.abs(qx - px) + Math.abs(qy - py) > 0.5f) o.angle = (float) Math.atan2(qy - py, qx - px);
        o.phase += 0.2f;
    }

    /** Draws one wall from ground edge a-b up to roof edge ta-tb, with windows and doors on it. */
    void drawWall(Canvas c, City.Building b, float ax, float ay, float bx, float by, float tax, float tay,
                          float tbx, float tby, float shade, boolean windows, int side) {
        float len = Math.abs(bx - ax) + Math.abs(by - ay), hgt = b.height;
        gv.wallSrc[0] = 0;
        gv.wallSrc[1] = 0;
        gv.wallSrc[2] = len;
        gv.wallSrc[3] = 0;
        gv.wallSrc[4] = len;
        gv.wallSrc[5] = hgt;
        gv.wallSrc[6] = 0;
        gv.wallSrc[7] = hgt;
        gv.wallDst[0] = ax;
        gv.wallDst[1] = ay;
        gv.wallDst[2] = bx;
        gv.wallDst[3] = by;
        gv.wallDst[4] = tbx;
        gv.wallDst[5] = tby;
        gv.wallDst[6] = tax;
        gv.wallDst[7] = tay;
        if (!gv.wallMatrix.setPolyToPoly(gv.wallSrc, 0, gv.wallDst, 0, 4)) return;
        c.save();
        c.concat(gv.wallMatrix);
        gv.fill.setColor(gv.hud.fade(City.darken(b.wall, shade)));
        c.drawRect(0, 0, len, hgt, gv.fill);
        drawWindows(c, b, shade, windows, side, len, hgt);
        if (gv.realistic && gv.scale > 2f) {
            // Soft light: darker towards the ground, a lit cornice along the top.
            float band = Math.min(hgt * 0.35f, 14);
            gv.fill.setColor(gv.hud.fade(0x14000000));
            c.drawRect(0, hgt - band, len, hgt, gv.fill);
            gv.fill.setColor(gv.hud.fade(0x18000000));
            c.drawRect(0, hgt - band * 0.45f, len, hgt, gv.fill);
            gv.fill.setColor(gv.hud.fade(0x30FFFFFF));
            c.drawRect(0, 0, len, 1.2f, gv.fill);
            gv.fill.setColor(gv.hud.fade(0x22000000));
            c.drawRect(0, 1.2f, len, 2f, gv.fill);
        }
        // Bullet holes and scorch marks from the fighting.
        for (int m = 0; m < b.markCount; m++) {
            int k = m * 4;
            if ((int) b.marks[k] != side) continue;
            float u = b.marks[k + 1], v = b.marks[k + 2], sz = b.marks[k + 3];
            if (u > len || v > hgt) continue;
            if (sz > 2) {
                gv.fill.setColor(gv.hud.fade(0x66141210));
                c.drawRect(u - sz, Math.max(0, v - sz * 0.6f), u + sz, Math.min(hgt, v + sz * 1.6f), gv.fill);
                gv.fill.setColor(gv.hud.fade(0x88141210));
                c.drawRect(u - sz * 0.5f, Math.max(0, v - sz * 0.2f), u + sz * 0.5f, Math.min(hgt, v + sz), gv.fill);
            } else {
                gv.fill.setColor(gv.hud.fade(0xFF2A2622));
                c.drawRect(u - sz, v - sz, u + sz, v + sz, gv.fill);
            }
        }
        c.restore();
    }

    /** ((27.3) From drawWall.) The windows down a wall, floor by floor, by kind of building, lit or broken. */
    void drawWindows(Canvas c, City.Building b, float shade, boolean windows, int side, float len, float hgt) {
        if (windows) {
            int floors = (int) (hgt / City.FLOOR);
            int cols = Math.max(1, (int) (len / City.T));
            float cw = len / cols;
            float glass = 0.55f + shade * 0.45f;
            int litMask = 7;
            boolean block = b.kind == City.OFFICE || b.kind == City.APARTMENT;
            for (int k = 0; k < floors; k++) {
                float v0 = k * City.FLOOR;
                for (int i = 0; i < cols; i++) {
                    float u0 = i * cw;
                    int hsh = (b.seed * 73856093) ^ (side * 19349663) ^ (k * 83492791) ^ (i * 26544357);
                    boolean lit = ((hsh >>> 9) & litMask) == 0;
                    int litColor = 0xFFF0D98C;
                    if (b.kind == City.SHOP && k == 0) {
                        // Shopfront: big window under a striped awning.
                        gv.fill.setColor(gv.hud.fade(City.darken(0xFF6F8EA6, glass)));
                        c.drawRect(u0 + 1.5f, 0.5f, u0 + cw - 1.5f, 7, gv.fill);
                        int awn = SHOP_AWNINGS[(b.seed + i) % SHOP_AWNINGS.length];
                        for (int st = 0; st < 4; st++) {
                            gv.fill.setColor(gv.hud.fade(City.darken(st % 2 == 0 ? awn : 0xFFF2F2F2, glass)));
                            c.drawRect(u0 + cw * st / 4f, 7.5f, u0 + cw * (st + 1) / 4f, 10.5f, gv.fill);
                        }
                    } else if (b.kind == City.CHURCH || b.kind == City.SPIRE) {
                        // Tall stained-glass windows spanning the floors.
                        if (k == 0 && i % 2 == 1) {
                            gv.fill.setColor(gv.hud.fade(City.darken(i % 4 == 1 ? 0xFF6A4E9A : 0xFF3F7AA8, glass)));
                            c.drawRect(u0 + cw * 0.35f, 2, u0 + cw * 0.65f, Math.min(hgt - 4, City.FLOOR * floors - 3), gv.fill);
                        }
                    } else if (b.kind == City.FIRE_STATION && k == 0) {
                        gv.fill.setColor(gv.hud.fade(City.darken(0xFFC8302A, glass)));
                        c.drawRect(u0 + 1.5f, 0, u0 + cw - 1.5f, 9.5f, gv.fill);
                        gv.fill.setColor(gv.hud.fade(City.darken(0xFFE8E8E8, glass)));
                        for (float v = 2; v < 9; v += 2.5f) c.drawRect(u0 + 1.5f, v, u0 + cw - 1.5f, v + 0.5f, gv.fill);
                    } else if (b.kind == City.STADIUM || b.kind == City.SILO) {
                        // Bare concrete with ribs.
                        if (k == 0 && i % 2 == 0) {
                            gv.fill.setColor(gv.hud.fade(City.darken(b.wall, shade * 0.85f)));
                            c.drawRect(u0 + cw * 0.4f, 0, u0 + cw * 0.6f, hgt, gv.fill);
                        }
                    } else if (b.kind == City.BARN) {
                        if (k == 0 && i == cols / 2) {
                            gv.fill.setColor(gv.hud.fade(City.darken(0xFFE8E0D0, glass)));
                            c.drawRect(u0 + 1, 0, u0 + cw - 1, 10, gv.fill);
                            gv.fill.setColor(gv.hud.fade(City.darken(0xFF7A2A20, glass)));
                            c.drawRect(u0 + 2, 1, u0 + cw - 2, 9, gv.fill);
                        }
                    } else if ((b.kind == City.MARKET || b.kind == City.KIOSK || b.kind == City.MALL) && k == 0) {
                        gv.fill.setColor(gv.hud.fade(City.darken(0xFF7FA6C0, glass)));
                        c.drawRect(u0, 0.5f, u0 + cw, 8.5f, gv.fill);
                        gv.fill.setColor(gv.hud.fade(City.darken(b.kind == City.KIOSK ? 0xFFD83A3A : 0xFF3E8A4A, glass)));
                        c.drawRect(u0, 8.5f, u0 + cw, 11, gv.fill);
                    } else if (b.kind == City.CRYPT) {
                        if (k == 0 && i == cols / 2) {
                            gv.fill.setColor(gv.hud.fade(City.darken(0xFF3A3632, glass)));
                            c.drawRect(u0 + cw * 0.3f, 0, u0 + cw * 0.7f, 8, gv.fill);
                        }
                    } else if (b.kind == City.WAREHOUSE || b.kind == City.POWER) {
                        if (k == 0 && i % 2 == 0) {
                            gv.fill.setColor(gv.hud.fade(City.darken(0xFFA4A8AC, glass)));
                            c.drawRect(u0 + 2, 0, u0 + cw - 2, 9, gv.fill);
                            gv.fill.setColor(gv.hud.fade(City.darken(0xFF7E8286, glass)));
                            for (float v = 2; v < 9; v += 2.2f) c.drawRect(u0 + 2, v, u0 + cw - 2, v + 0.6f, gv.fill);
                        } else if (k == floors - 1) {
                            gv.fill.setColor(gv.hud.fade(lit ? litColor : City.darken(0xFF3A4652, glass)));
                            c.drawRect(u0 + 1, v0 + 5, u0 + cw - 1, v0 + 8, gv.fill);
                        }
                    } else if (b.kind == City.HOUSE) {
                        boolean door = k == 0 && side == 1 && i == cols / 2;
                        if (door) {
                            gv.fill.setColor(gv.hud.fade(City.darken(0xFF5A3A28, glass)));
                            c.drawRect(u0 + cw * 0.35f, 0, u0 + cw * 0.65f, 8, gv.fill);
                        } else {
                            gv.fill.setColor(gv.hud.fade(City.darken(0xFFEEEEEE, glass)));
                            c.drawRect(u0 + cw * 0.25f, v0 + 3.5f, u0 + cw * 0.75f, v0 + 9.5f, gv.fill);
                            gv.fill.setColor(gv.hud.fade(lit ? litColor : City.darken(0xFF34414E, glass)));
                            c.drawRect(u0 + cw * 0.25f + 1, v0 + 4.5f, u0 + cw * 0.75f - 1, v0 + 8.5f, gv.fill);
                        }
                    } else if (k == 0) {
                        gv.fill.setColor(gv.hud.fade(City.darken(0xFF5A7890, glass)));
                        c.drawRect(u0 + 2, v0 + 1.5f, u0 + cw - 2, v0 + 8.5f, gv.fill);
                    } else {
                        gv.fill.setColor(gv.hud.fade(lit ? litColor : City.darken(0xFF27313B, glass)));
                        c.drawRect(u0 + 3, v0 + 3.5f, u0 + cw - 3, v0 + 9.5f, gv.fill);
                        if (gv.realistic && gv.scale > 6f) {
                            // Sky reflected in the top of the glass, and a sill beneath (only when close enough to see).
                            gv.fill.setColor(gv.hud.fade(lit ? 0x30FFFFFF : 0x2A9CC3E8));
                            c.drawRect(u0 + 3, v0 + 3.5f, u0 + cw - 3, v0 + 5.5f, gv.fill);
                            gv.fill.setColor(gv.hud.fade(0x40000000));
                            c.drawRect(u0 + 2.5f, v0 + 9.5f, u0 + cw - 2.5f, v0 + 10.3f, gv.fill);
                        }
                    }
                }
            }
            gv.fill.setColor(gv.hud.fade(City.darken(b.wall, shade * 0.8f)));
            c.drawRect(0, hgt - 2.5f, len, hgt, gv.fill);
        }
    }
}
