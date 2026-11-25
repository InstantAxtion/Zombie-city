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
 * ((27.3) Out of GameView.) What a touch does: the buttons and panels it lands on, dragging and pinching the map, the tools
 * (spawning, placing, events, orders, building, undo) and the controls for whoever you've taken over.
 */
final class TouchInput {
    final GameView gv;

    TouchInput(GameView gv) {
        this.gv = gv;
    }


    /** ((22.0) Into the driver's seat of v, as someone who'd be driving it: an officer, a soldier, or anyone. */
    void driveVehicle(Fleet.Vehicle v) {
        int type = v.type == Fleet.CRUISER || Fleet.policeTruck(v) || v.kind == Fleet.K_POLICE_HELI || v.kind == Fleet.K_RESCUE_HELI ? Entity.COP
                : Fleet.army(v) ? Entity.SOLDIER : Entity.CIVILIAN;
        // ((30.8) One of its own crew at the wheel, when it has one: a new driver used to appear for it every time, and
        // joined its crew when the player let go - one more aboard each time.)
        Entity e = null;
        for (int i = 0; i < v.crew.size() && e == null; i++) {
            Entity c = v.crew.get(i);
            if (c.type == type || type == Entity.CIVILIAN) e = c;
        }
        if (e != null) {
            v.crew.remove(e);
            e.dead = false;
            e.removed = false;
            e.rig = null;
            e.x = v.x;
            e.y = v.y;
        } else e = gv.world.spawn(type, v.x, v.y);
        if (e == null) return;
        takeControl(e);
        gv.world.driveInto(e, v);
        gv.follow = null;
        gv.followV = null;
        gv.world.say("You're driving. Steer with the stick; Get out to leave it.");
    }

    void takeControl(Entity e) {
        gv.world.letGoOfCar();
        gv.world.controlled = e;
        gv.world.joyX = gv.world.joyY = 0;
        gv.world.ctrlAttack = false;
        gv.joyId = gv.attackId = -1;
        gv.follow = e;
        gv.tool = TOOL_PAN;
        gv.picker = -1;
        gv.selection.clear();
        gv.scale = Math.max(gv.scale, 2.6f * gv.baseDp);
        gv.world.say("You are " + (e.isUndead() ? "a " + Entity.NAMES[e.type].toLowerCase() : e.type == Entity.DOG ? Names.dog(e.nameSeed)
                : Names.person(e.nameSeed)));
    }

    void releaseControl() {
        gv.world.letGoOfCar();
        gv.world.controlled = null;
        gv.world.joyX = gv.world.joyY = 0;
        gv.world.ctrlAttack = false;
        gv.joyId = gv.attackId = -1;
    }

    /** While controlling someone: the left thumb steers, the right thumb attacks; taps on buttons still work. */
    boolean controlTouch(MotionEvent ev) {
        int action = ev.getActionMasked();
        int idx = ev.getActionIndex();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                float x = ev.getX(idx), y = ev.getY(idx);
                int id = ev.getPointerId(idx);
                for (int k = 0; k < gv.ctxCount; k++)
                    if (gv.ctxRects[k].contains(x, y)) {
                        gv.click();
                        gv.world.ctrlAction(gv.ctxActions[k]);
                        return true;
                    }
                if (World.hypot(x - gv.attackX, y - gv.attackY) < gv.attackR * 1.3f) {
                    gv.attackId = id;
                    gv.world.ctrlAttack = true;
                } else if (action == MotionEvent.ACTION_DOWN && hitUi(x, y)) {
                    gv.mode = MODE_UI;
                } else if (x < gv.getWidth() * 0.55f && y < gv.barTop && gv.joyId < 0) {
                    gv.joyId = id;
                    gv.joyCX = gv.joyKX = x;
                    gv.joyCY = gv.joyKY = y;
                } else if (y < gv.barTop && gv.attackId < 0) {
                    gv.attackId = id;
                    gv.world.ctrlAttack = true;
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < ev.getPointerCount(); i++) {
                    if (ev.getPointerId(i) != gv.joyId) continue;
                    float r = 55 * gv.dp, dx = ev.getX(i) - gv.joyCX, dy = ev.getY(i) - gv.joyCY;
                    float d = (float) Math.sqrt(dx * dx + dy * dy);
                    if (d > r) {
                        dx *= r / d;
                        dy *= r / d;
                    }
                    gv.joyKX = gv.joyCX + dx;
                    gv.joyKY = gv.joyCY + dy;
                    gv.world.joyX = dx / r;
                    gv.world.joyY = dy / r;
                }
                return true;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                int id = action == MotionEvent.ACTION_CANCEL ? -2 : ev.getPointerId(idx);
                if (id == gv.joyId || id == -2) {
                    gv.joyId = -1;
                    gv.world.joyX = gv.world.joyY = 0;
                }
                if (id == gv.attackId || id == -2) {
                    gv.attackId = -1;
                    gv.world.ctrlAttack = false;
                }
                if (action != MotionEvent.ACTION_POINTER_UP) gv.mode = MODE_NONE;
                return true;
            }
        }
        return true;
    }

    boolean touch(MotionEvent ev) {
        // ((22.0) A finger on the screen: back to drawing at the screen's own rate, straight away.)
        gv.lastInputNs = System.nanoTime();
        gv.postInvalidateOnAnimation();
        if (gv.loading) return true;
        if (gv.hudHidden && !gv.menu.isOpen()) {
            // The first tap brings the buttons back.
            if (ev.getActionMasked() == MotionEvent.ACTION_UP) gv.hudHidden = false;
            return true;
        }
        if (gv.menu.isOpen()) {
            gv.mode = MODE_NONE;
            return gv.menu.onTouch(ev);
        }
        if (gv.world.controlled != null) {
            gv.tactical = false;
            gv.hotOpen = false;
            gv.timeOpen = false;
            return controlTouch(ev);
        }
        if (gv.hotOpen) {
            gv.mode = MODE_NONE;
            if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) gv.hotDown = true;
            else if (ev.getActionMasked() == MotionEvent.ACTION_UP && gv.hotDown) {
                gv.hotDown = false;
                gv.hud.hotTap(ev.getX(), ev.getY());
            }
            return true;
        }
        if (gv.timeOpen) {
            gv.mode = MODE_NONE;
            if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) gv.timeDown = true;
            else if (ev.getActionMasked() == MotionEvent.ACTION_UP && gv.timeDown) {
                gv.timeDown = false;
                gv.hud.timelineTap(ev.getX(), ev.getY());
            }
            return true;
        }
        if (gv.tactical) {
            gv.mode = MODE_NONE;
            // (10.50) Only a tap that starts on the open map counts: lifting the finger that opened it used to
            // close it again at once, so it had to be held open.
            if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) gv.tacDown = true;
            else if (ev.getActionMasked() == MotionEvent.ACTION_UP && gv.tacDown) {
                gv.tacDown = false;
                gv.hud.tacticalTap(ev.getX(), ev.getY());
            }
            return true;
        }
        int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                float x = ev.getX(), y = ev.getY();
                if (hitUi(x, y)) {
                    gv.mode = MODE_UI;
                } else {
                    gv.mode = MODE_WORLD;
                    gv.downX = gv.lastX = x;
                    gv.downY = gv.lastY = y;
                    gv.dragged = false;
                    gv.panVX = gv.panVY = 0;
                    worldDown(x, y);
                }
                return true;
            }
            case MotionEvent.ACTION_POINTER_DOWN:
                if (gv.mode != MODE_UI && ev.getPointerCount() >= 2) {
                    gv.mode = MODE_GESTURE;
                    gv.follow = null;
                    gv.followV = null;
                    startPinch(ev);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (gv.mode == MODE_GESTURE && ev.getPointerCount() >= 2) {
                    doPinch(ev);
                } else if (gv.mode == MODE_WORLD) {
                    worldMove(ev.getX(), ev.getY());
                }
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                if (gv.mode == MODE_GESTURE && ev.getPointerCount() > 2) startPinchExcluding(ev, ev.getActionIndex());
                return true;
            case MotionEvent.ACTION_UP:
                if (gv.mode == MODE_WORLD) worldUp(ev.getX(), ev.getY());
                gv.mode = MODE_NONE;
                if (gv.buildDirty) {
                    // Redraw the map once the stroke is done.
                    gv.buildDirty = false;
                    gv.world.city.editsDone();
                    gv.world.redrawCity();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                gv.mode = MODE_NONE;
                return true;
        }
        return gv.superTouch(ev);
    }

    boolean hitUi(float x, float y) {
        if (gv.world.controlled != null && gv.releaseRect.contains(x, y)) {
            gv.click();
            releaseControl();
            return true;
        }
        if (gv.world.controlled == null && gv.follow != null && !gv.follow.dead && gv.picker < 0 && !gv.clearMenu && gv.pinRect.contains(x, y)) {
            gv.click();
            if (!gv.pinned.remove(gv.follow)) {
                if (gv.pinned.size() >= 6) gv.pinned.remove(0);
                gv.pinned.add(gv.follow);
            }
            return true;
        }
        for (int k = 0; k < gv.pinCount; k++)
            if (gv.pinRects[k].contains(x, y)) {
                gv.click();
                Entity p = gv.pinIcons[k];
                Object at = p.dead && gv.world.controlled != p ? gv.pinWhere.get(p) : null;
                gv.inspectB = null;
                gv.inspectZ = null;
                gv.inspectBase = null;
                if (at instanceof Fleet.Vehicle) {
                    // ((12.0) In a car: the car, followed; in a building: the building's card, there on screen.)
                    gv.follow = null;
                    gv.followV = (Fleet.Vehicle) at;
                } else if (at instanceof City.Building) {
                    City.Building b = (City.Building) at;
                    gv.follow = null;
                    gv.followV = null;
                    if (gv.tool == TOOL_PAN) gv.inspectB = b;
                    gv.camX = b.doorX - gv.getWidth() / gv.scale / 2;
                    gv.camY = b.doorY - gv.barTop / gv.scale / 2;
                    gv.clampCamera();
                } else gv.follow = p;
                return true;
            }
        if (gv.world.controlled == null && gv.followV != null && gv.picker < 0 && !gv.clearMenu && gv.driveRect.contains(x, y)) {
            gv.click();
            driveVehicle(gv.followV);
            return true;
        }
        if (gv.world.controlled == null && gv.follow != null && !gv.follow.dead && gv.picker < 0 && !gv.clearMenu && gv.takeRect.contains(x, y)) {
            gv.click();
            takeControl(gv.follow);
            return true;
        }
        if (gv.picker >= 0 && y < gv.barTop) {
            // Choosing from the open picker; a tap anywhere else just closes it.
            for (int k = 0; k < gv.pickerCount; k++)
                if (gv.pickerRects[k].contains(x, y)) {
                    gv.sound.play(Sfx.TAP, 0.7f, 0);
                    gv.toolBar.setVariant(gv.picker, k);
                    gv.toolBar.rememberVariant(gv.picker);
                    gv.picker = -1;
                    return true;
                }
            gv.picker = -1;
            return true;
        }
        if (gv.toolsFolded && y >= gv.barTop || !gv.toolsFolded && gv.foldTab.contains(x, y)) {
            gv.click();
            gv.toolsFolded = !gv.toolsFolded;
            gv.picker = -1;
            gv.onSizeChanged(gv.getWidth(), gv.getHeight(), gv.getWidth(), gv.getHeight());
            return true;
        }
        if (y >= gv.barTop) {
            for (int i = 0; i < gv.toolRects.length; i++) {
                if (gv.toolRects[i].contains(x, y)) {
                    gv.click();
                    // Spawn buttons open their picker (tap again to close it).
                    if (ToolBar.hasPicker(i)) gv.picker = gv.picker == i ? -1 : i;
                    else gv.picker = -1;
                    if (gv.world.controlled != null && i != TOOL_PAN) releaseControl();
                    if (i != TOOL_ORDER) gv.selection.clear();
                    gv.tool = i;
                    gv.hintTime = Math.min(gv.hintTime, 3);
                }
            }
            return true;
        }
        for (int i = 0; i < gv.feedCount; i++) {
            if (gv.feedLines[i].contains(x, y)) {
                Dispatch.Message m = gv.feedMsgs[i];
                gv.follow = null;
                gv.followV = null;
                gv.camX = m.x - gv.getWidth() / gv.scale / 2;
                gv.camY = m.y - gv.barTop / gv.scale / 2;
                gv.clampCamera();
                gv.click();
                return true;
            }
        }
        if (gv.clearMenu) {
            for (int i = 0; i < gv.clearRects.length; i++)
                if (gv.clearRects[i].contains(x, y)) {
                    gv.click();
                    // ((12.0) Clearing everyone takes a second tap, to be sure.)
                    if (i == 0 && System.nanoTime() - gv.clearArmedAt > 3000000000L) {
                        gv.clearArmedAt = System.nanoTime();
                        return true;
                    }
                    gv.clearArmedAt = 0;
                    gv.clearMenu = false;
                    switch (i) {
                        case 0: gv.world.clearAll(); gv.follow = null; gv.followV = null; break;
                        case 1: gv.world.clearZombies(); break;
                        case 2: gv.world.clearBodies(); break;
                        case 3: gv.world.clearWrecks(); break;
                        default: gv.world.clearBarricades(); break;
                    }
                    return true;
                }
        }
        if (!gv.hotBtn.isEmpty() && gv.hotBtn.contains(x, y) || !gv.hotChip.isEmpty() && gv.hotChip.contains(x, y)) {
            gv.hud.openHot();
            return true;
        }
        if (!gv.timeBtn.isEmpty() && gv.timeBtn.contains(x, y) || !gv.timeChip.isEmpty() && gv.timeChip.contains(x, y)) {
            gv.hud.openTimeline();
            return true;
        }
        // ((27.5) The overlays: on to the next one (heat, reach, off).)
        if (!gv.overlayBtn.isEmpty() && gv.overlayBtn.contains(x, y) || !gv.overlayKey.isEmpty() && gv.overlayKey.contains(x, y)
                || !gv.overlayChip.isEmpty() && gv.overlayChip.contains(x, y)) {
            gv.click();
            gv.overlays.cycle();
            return true;
        }
        if (!gv.tacBtn.isEmpty() && gv.tacBtn.contains(x, y)) {
            gv.click();
            gv.hotOpen = false;
            gv.tactical = true;
            gv.tacDown = false;
            gv.tacTimer = 0;
            return true;
        }
        if (!gv.miniX.isEmpty() && gv.miniX.contains(x, y) || !gv.mapChip.isEmpty() && gv.mapChip.contains(x, y)) {
            gv.click();
            gv.miniClosed = !gv.miniClosed;
            return true;
        }
        if (!gv.miniRect.isEmpty() && gv.miniRect.contains(x, y)) {
            // Jump there.
            gv.follow = null;
            gv.followV = null;
            float wx = gv.miniX0 + (x - gv.miniRect.left) / gv.miniRect.width() * gv.miniW;
            float wy = gv.miniY0 + (y - gv.miniRect.top) / gv.miniRect.height() * gv.miniH;
            gv.camX = wx - gv.getWidth() / gv.scale / 2;
            gv.camY = wy - gv.barTop / gv.scale / 2;
            gv.clampCamera();
            gv.click();
            return true;
        }
        if (!gv.lastAction.isEmpty() && gv.undoRect.contains(x, y)) {
            gv.click();
            undo();
            return true;
        }
        for (int i = 0; i < gv.topRects.length; i++) {
            RectF r = gv.topRects[i];
            if (x >= r.left - 4 * gv.dp && x <= r.right + 4 * gv.dp && y >= r.top - 6 * gv.dp && y <= r.bottom + 6 * gv.dp) {
                pressTop(i);
                return true;
            }
        }
        if (gv.clearMenu) {
            gv.clearMenu = false;
            return true;
        }
        if (gv.statsCollapsed ? (x < gv.statsRect.left + 300 * gv.dp && y < gv.statsRect.top + 30 * gv.dp && x > gv.statsRect.left
                && y > gv.statsRect.top) : gv.statsRect.contains(x, y)) {
            gv.statsCollapsed = !gv.statsCollapsed;
            gv.click();
            return true;
        }
        return false;
    }

    void pressTop(int i) {
        gv.click();
        switch (i) {
            case BTN_PAUSE:
                gv.simPaused = !gv.simPaused;
                break;
            case BTN_SPEED:
                gv.speedIdx = (gv.speedIdx + 1) % SPEEDS.length;
                break;
            case BTN_BRUSH:
                gv.brushIdx = (gv.brushIdx + 1) % BRUSHES.length;
                // ((33.0) Kept for next time.)
                gv.settings.remember("brush", gv.brushIdx);
                break;
            case BTN_CLEAR:
                gv.clearMenu = !gv.clearMenu;
                break;
            case BTN_TOP:
                // ((13.3) Straight to the Top Fighters list.)
                gv.menu.open(Menu.TOP);
                break;
            case BTN_MENU:
                gv.menu.open(Menu.PAUSE);
                break;
        }
    }

    void worldDown(float x, float y) {
        float wx = gv.worldX(x), wy = gv.worldY(y);
        if (gv.tool == TOOL_PAN) return;
        gv.follow = null;
        gv.followV = null;
        if (gv.tool != TOOL_ORDER && gv.tool != TOOL_BOMB && gv.tool != TOOL_ERASE) gv.lastAction.clear();
        if (gv.tool == TOOL_PLACE) {
            place(wx, wy);
        } else if (gv.tool == TOOL_EVENT) {
            event(wx, wy);
        } else if (gv.tool == TOOL_BUILD) {
            gv.lastBuildTile = -1;
            buildAt(wx, wy);
        } else if (gv.tool == TOOL_ORDER) {
            // Tap or drag a box: decided when the finger lifts.
            gv.boxX0 = wx;
            gv.boxY0 = wy;
            gv.boxX1 = wx;
            gv.boxY1 = wy;
        } else if (gv.tool == TOOL_BOMB) {
            // ((29.1) The player's own bomb: it kills whoever it catches, the living too.)
            gv.world.playerBomb = true;
            try {
                gv.world.explode(wx, wy, 70, 400);
            } finally {
                gv.world.playerBomb = false;
            }
        } else if (gv.tool == TOOL_ZONE) {
            gv.world.dispatch.orderZone(wx, wy, gv.zoneVariant == 1);
        } else if (gv.tool == TOOL_ERASE) {
            if (!gv.world.dispatch.removeZoneAt(wx, wy)) gv.world.erase(wx, wy, 22);
        } else {
            spawnBrush(wx, wy);
        }
        gv.lastSpawnX = wx;
        gv.lastSpawnY = wy;
    }

    /** One Build stroke: each tile once per drag. */
    void buildAt(float wx, float wy) {
        int tile = gv.world.city.tileIndex(wx, wy);
        if (tile == gv.lastBuildTile) return;
        gv.lastBuildTile = tile;
        if (gv.world.build(wx, wy, gv.buildVariant)) gv.buildDirty = true;
    }

    void worldMove(float x, float y) {
        if (Math.abs(x - gv.downX) > 8 * gv.dp || Math.abs(y - gv.downY) > 8 * gv.dp) gv.dragged = true;
        float wx = gv.worldX(x), wy = gv.worldY(y);
        if (gv.tool == TOOL_PAN) {
            if (gv.dragged) {
                gv.follow = null;
                gv.followV = null;
                gv.inspectB = null;
                gv.inspectZ = null;
                gv.inspectBase = null;
                gv.camX -= (x - gv.lastX) / gv.scale;
                gv.camY -= (y - gv.lastY) / gv.scale;
                gv.clampCamera();
                long now = System.nanoTime();
                float sec = Math.max(0.004f, (now - gv.panAt) / 1e9f);
                if (sec < 0.1f) {
                    gv.panVX = gv.panVX * 0.5f - (x - gv.lastX) / gv.scale / sec * 0.5f;
                    gv.panVY = gv.panVY * 0.5f - (y - gv.lastY) / gv.scale / sec * 0.5f;
                } else gv.panVX = gv.panVY = 0;
                gv.panAt = now;
            }
        } else if (gv.tool == TOOL_ERASE) {
            gv.world.erase(wx, wy, 22);
        } else if (gv.tool == TOOL_BUILD) {
            if (gv.buildVariant != City.ED_HOUSE && gv.buildVariant != City.ED_SHOP) buildAt(wx, wy);
        } else if (gv.tool == TOOL_ORDER) {
            gv.boxX1 = wx;
            gv.boxY1 = wy;
            gv.boxing = gv.dragged;
        } else if (gv.tool == TOOL_PLACE) {
            // Drag to build a line of barricades.
            if ((gv.placeVariant == 3 || gv.placeVariant == 9 || gv.placeVariant == 12 || gv.placeVariant == 13) && (Math.abs(wx - gv.lastSpawnX) > City.T * 0.8f || Math.abs(wy - gv.lastSpawnY) > City.T * 0.8f)) {
                place(wx, wy);
                gv.lastSpawnX = wx;
                gv.lastSpawnY = wy;
            }
        } else if (gv.tool != TOOL_BOMB && gv.tool != TOOL_ZONE && gv.tool != TOOL_ORDER && gv.tool != TOOL_EVENT && gv.tool != TOOL_BUILD && gv.tool != TOOL_SQUAD) {
            float ddx = wx - gv.lastSpawnX, ddy = wy - gv.lastSpawnY;
            float spacing = BRUSHES[gv.brushIdx] > 1 ? 22 : 12;
            if (ddx * ddx + ddy * ddy > spacing * spacing) {
                spawnBrush(wx, wy);
                gv.lastSpawnX = wx;
                gv.lastSpawnY = wy;
            }
        }
        gv.lastX = x;
        gv.lastY = y;
    }

    void worldUp(float x, float y) {
        if (gv.tool == TOOL_ORDER) {
            if (!gv.boxing) orderTap(gv.worldX(x), gv.worldY(y));
            else {
                // Everyone in uniform inside the box.
                gv.selection.clear();
                float x0 = Math.min(gv.boxX0, gv.boxX1), x1 = Math.max(gv.boxX0, gv.boxX1), y0 = Math.min(gv.boxY0, gv.boxY1), y1 = Math.max(gv.boxY0, gv.boxY1);
                for (int i = 0, n = gv.world.entities.size(); i < n; i++) {
                    Entity e = gv.world.entities.get(i);
                    // ((12.0) Not the fallen, still lying where they fell.)
                    if (e.isArmed() && !e.dead && e.x >= x0 && e.x <= x1 && e.y >= y0 && e.y <= y1) gv.selection.add(e);
                }
                if (gv.selection.isEmpty()) gv.world.say("No police or soldiers in that box");
            }
            gv.boxing = false;
            return;
        }
        if (gv.tool == TOOL_PAN && gv.dragged) {
            // Let go mid-swipe and the map glides on, gently; held still first and it stays put.
            if ((System.nanoTime() - gv.panAt) / 1e9f > 0.08f) gv.panVX = gv.panVY = 0;
            else {
                gv.panVX *= 0.55f;
                gv.panVY *= 0.55f;
            }
        }
        if (gv.tool != TOOL_PAN || gv.dragged) return;
        // ((12.0) A double tap zooms in on the spot.)
        long now = System.nanoTime();
        if (now - gv.lastTapAt < 320000000L && World.hypot(x - gv.lastTapX, y - gv.lastTapY) < 32 * gv.dp) {
            gv.lastTapAt = 0;
            gv.zoomBy(1.8f, x, y);
            return;
        }
        gv.lastTapAt = now;
        gv.lastTapX = x;
        gv.lastTapY = y;
        float wx = gv.worldX(x), wy = gv.worldY(y);
        float best = Math.max(12, 28 * gv.dp / gv.scale);
        best *= best;
        Entity pick = null;
        for (int i = 0, n = gv.world.entities.size(); i < n; i++) {
            Entity e = gv.world.entities.get(i);
            // ((12.0) Only someone you can see: not one of the dead shut in a building out of sight, or someone
            // who's just died and is about to be taken off the map.)
            if (e.dead) continue;
            float ddx = e.x - wx, ddy = e.y - wy, d2 = ddx * ddx + ddy * ddy;
            if (d2 < best) {
                best = d2;
                pick = e;
            }
        }
        gv.follow = pick;
        gv.followV = null;
        if (pick == null) {
            // A vehicle there: follow it.
            float bv = Float.MAX_VALUE;
            for (int i = 0, n = gv.world.fleet.vehicles.size(); i < n; i++) {
                Fleet.Vehicle v = gv.world.fleet.vehicles.get(i);
                float r = Math.max(v.length() + 3, 22 * gv.dp / gv.scale), ddx = v.x - wx, ddy = v.y - wy, d2 = ddx * ddx + ddy * ddy;
                if (d2 < r * r && d2 < bv) {
                    bv = d2;
                    gv.followV = v;
                }
            }
        }
        // Nobody there: tap a building to see what it is and how it's doing.
        gv.inspectB = pick == null && gv.followV == null ? gv.world.city.buildingAt(wx, wy) : null;
        // ((12.0) Or a safe zone: its card.)
        gv.inspectZ = null;
        gv.inspectBase = null;
        if (pick == null && gv.followV == null)
            for (int i = 0; i < gv.world.dispatch.zones.size(); i++) {
                Dispatch.SafeZone z = gv.world.dispatch.zones.get(i);
                if (z.contains(wx, wy, 1)) {
                    gv.inspectZ = z;
                    gv.inspectB = null;
                }
            }
        // ((25.1) Or a survivor base: its card.)
        if (pick == null && gv.followV == null && gv.inspectZ == null)
            for (int i = 0; i < gv.world.bases.size(); i++) {
                SurvivorBases.Base sb = gv.world.bases.get(i);
                if (sb.contains(wx, wy, 0)) {
                    gv.inspectBase = sb;
                    gv.inspectB = null;
                }
            }
    }

    /**
     * Orders tool: tap a cop or soldier to select them (a soldier brings their whole squad), then tap where
     * to send them. Tapping the selection again sends them back to normal duty.
     */
    void orderTap(float wx, float wy) {
        float reach = Math.max(12, 26 * gv.dp / gv.scale);
        Entity pick = null;
        float best = reach * reach;
        for (int i = 0, n = gv.world.entities.size(); i < n; i++) {
            Entity e = gv.world.entities.get(i);
            if (!e.isArmed()) continue;
            float d = (e.x - wx) * (e.x - wx) + (e.y - wy) * (e.y - wy);
            if (d < best) {
                best = d;
                pick = e;
            }
        }
        if (pick != null) {
            if (gv.selection.contains(pick)) {
                gv.world.dispatch.dismiss(gv.selection);
                gv.selection.clear();
                return;
            }
            gv.selection.clear();
            gv.selection.add(pick);
            if (pick.type == Entity.SOLDIER)
                for (int i = 0, n = gv.world.entities.size(); i < n; i++) {
                    Entity e = gv.world.entities.get(i);
                    if (e != pick && e.type == Entity.SOLDIER && e.squad == pick.squad) gv.selection.add(e);
                }
            return;
        }
        if (gv.selection.isEmpty()) return;
        gv.world.dispatch.order(gv.selection, wx, wy);
        gv.orderX = wx;
        gv.orderY = wy;
        gv.orderMarker = 1.5f;
        gv.selection.clear();
    }

    void spawnBrush(float wx, float wy) {
        if (gv.tool == TOOL_SQUAD) {
            spawnSquad(wx, wy);
            return;
        }
        int type = gv.tool == TOOL_ZOMBIE ? gv.enemyType() : gv.tool == TOOL_CIV ? CIV_VARIANTS[gv.civVariant] : TOOL_TYPE[gv.tool];
        boolean mixed = type < 0;
        int n = BRUSHES[gv.brushIdx];
        // Only one commander at a time.
        if (gv.tool == TOOL_MIL && MIL_ROLES[gv.milVariant] == Entity.ROLE_COMMANDER) n = 1;
        int failed = 0;
        for (int i = 0; i < n; i++) {
            float ox = 0, oy = 0;
            if (n > 1) {
                double a = gv.rnd.nextDouble() * Math.PI * 2, r = Math.sqrt(gv.rnd.nextDouble()) * brushRadius(n);
                ox = (float) (Math.cos(a) * r);
                oy = (float) (Math.sin(a) * r);
            }
            Entity e = gv.tool == TOOL_MIL ? gv.world.spawnSoldier(MIL_ROLES[gv.milVariant], wx + ox, wy + oy)
                    : gv.tool == TOOL_COP ? gv.world.spawnCop(COP_ROLES[gv.copVariant], wx + ox, wy + oy)
                    : gv.world.spawn(mixed ? mixedZombie(gv.rnd.nextFloat()) : type, wx + ox, wy + oy);
            if (e != null) {
                gv.lastAction.add(e);
                if (e.type == Entity.GIANT && gv.giantSaid != e) {
                    gv.giantSaid = e;
                    gv.world.giantPlaced(e);
                }
            } else failed++;
        }
        // ((33.0) The ring of where they went, for a moment (WorldRenderer.drawBrushRing).)
        gv.brushX = wx;
        gv.brushY = wy;
        gv.brushR = n > 1 ? brushRadius(n) : 8;
        gv.brushT = 0.6f;
        // ((12.0) Nobody placed: say why, rather than nothing happening at all.)
        boolean full = gv.world.entities.size() >= gv.world.maxEntities;
        if (failed == n && n > 0 && gv.world.messageTime <= 0)
            gv.world.say(full ? "The city is full: no room for anyone else (Max population, in Settings)"
                    : "Nowhere to put them there");
        // ((33.0) Some placed but not all because the city is full: it said nothing, and the brush seemed to stop working.)
        else if (failed > 0 && full && gv.world.messageTime <= 0)
            gv.world.say("Only " + (n - failed) + " of " + n + " placed: the city is full (Max population, in Settings)");
    }

    /** ((33.0) How far round the finger a brush of n spreads them. */
    static float brushRadius(int n) {
        return n >= 100 ? 76 : n >= 50 ? 54 : n > 10 ? 36 : 20;
    }

    /** ((33.0) The mixed brush's pick: 70 in 100 walkers, 15 runners, 9 bruisers, 4 brutes, 2 armoured. */
    static int mixedZombie(float r) {
        return r < 0.70f ? Entity.ZOMBIE : r < 0.85f ? Entity.RUNNER : r < 0.94f ? Entity.BRUISER : r < 0.98f ? Entity.BRUTE : Entity.ARMOURED;
    }

    /** ((22.4) The Squads tool: one whole group where the finger lands. */
    void spawnSquad(float wx, float wy) {
        int v = gv.squadVariant, side = SQUAD_SIDE[v];
        java.util.ArrayList<Entity> g;
        if (side <= 1) g = gv.world.spawnGroup(SQUAD_ROLES[v], side == 1, wx, wy);
        else {
            g = new java.util.ArrayList<Entity>();
            // ((29.2) A horde of 35, not 20.)
            int n = side == 2 ? 6 : side == 4 ? 150 : side == 5 ? 500 : 35;
            for (int i = 0; i < n; i++) {
                double a = i * 2.4, r = 6 + Math.sqrt(i) * (n > 35 ? 5.5 : 7);
                int type = side == 2 ? Entity.CIVILIAN : i == 0 ? Entity.BRUTE : i % 7 == 3 ? Entity.BRUISER : i % 4 == 1 ? Entity.RUNNER : Entity.ZOMBIE;
                Entity e = gv.world.spawn(type, wx + (float) (Math.cos(a) * r), wy + (float) (Math.sin(a) * r));
                if (e != null) g.add(e);
            }
            // (The survivors stick together: everyone follows the first.)
            if (side == 2) for (int i = 1; i < g.size(); i++) g.get(i).leader = g.get(0);
        }
        gv.lastAction.addAll(g);
        if (g.isEmpty() && gv.world.messageTime <= 0) gv.world.say("Nowhere to put them there");
    }

    /** The Place tool: vehicles, barricades, supply crates and medkits. */
    void place(float wx, float wy) {
        Object made = null;
        switch (gv.placeVariant) {
            case 0: made = gv.world.fleet.placeCar(wx, wy); break;
            case 1: made = gv.world.fleet.placePolice(wx, wy); break;
            case 2: made = gv.world.fleet.placeTank(wx, wy); break;
            case 3: made = gv.world.placeBarricade(wx, wy); break;
            case 4: made = gv.world.placeCrate(wx, wy, 0); break;
            case 6: made = gv.world.fleet.placeBearCat(wx, wy); break;
            case 7: made = gv.world.fleet.startReconAt(wx, wy); break;
            case 8: made = gv.world.launchDrone(wx, wy); break;
            case 9: gv.world.layWire(wx, wy); break;
            case 10:
                if (gv.world.mgNest(wx, wy).isEmpty()) gv.world.say("No room for a nest there");
                break;
            case 11: made = gv.world.fleet.placeShelterVan(wx, wy); break;
            case 12: made = gv.world.placeBarricade(wx, wy, 1); break;
            case 13: made = gv.world.placeBarricade(wx, wy, 2); break;
            case 14:
                made = gv.world.ops.placePost(wx, wy);
                if (made == null) gv.world.say("The command post can't go there (no road, no truck, or nobody to drive it)");
                break;
            case 15:
                if (gv.world.ops.callIn(wx, wy, "", true).isEmpty()) gv.world.say("No room for them");
                break;
            case 16:
                made = gv.world.ops.placeBoat(wx, wy);
                if (made == null) gv.world.say("Tap near open water for a police boat");
                break;
            default: made = gv.world.placeMedkit(wx, wy); break;
        }
        if (made != null) gv.lastAction.add(made);
    }

    /** The Events tool: things that happen to the city at the spot you tap. */
    void event(float wx, float wy) {
        switch (gv.eventVariant) {
            case 0: gv.lastAction.addAll(gv.world.hordeEvent(wx, wy)); break;
            case 1: gv.world.panic(wx, wy); break;
            case 2:
                if (!gv.world.indoorOutbreakAt(wx, wy)) gv.world.say("Tap closer to a building");
                break;
            case 3: gv.world.supplyDrop(wx, wy); break;
            case 4:
                if (!gv.world.freeAirstrike(wx, wy)) gv.audio.buzzError();
                break;
            case 5: gv.world.cityAlarm(); break;
            case 7: gv.world.flare(wx, wy); break;
            case 8:
                if (gv.world.layMines(wx, wy) == 0) gv.world.say("No room for mines there");
                break;
            case 12:
                if (!gv.world.planeCrash(wx, wy)) gv.world.say("Nowhere for it to come down there");
                break;
            case 13:
                if (!gv.world.prisonBreak()) gv.audio.buzzError();
                break;
            case 14:
                if (!gv.world.riot(wx, wy) && gv.world.messageTime <= 0) gv.world.say("Nobody here to riot");
                break;
            case 15:
                // ((26.0) A supply convoy to the safe zone, survivor base or police station tapped.)
                if (!gv.world.convoys.supplyAt(wx, wy)) gv.world.say("Tap a safe zone, survivor base or police station (or a convoy's already going there)");
                break;
            case 9: gv.world.barrage(wx, wy); break;
            case 10: gv.lastAction.addAll(gv.world.airborne(wx, wy)); break;
            case 11: gv.lastAction.addAll(gv.world.megaHorde(wx, wy)); break;
            default: {
                Entity bitten = gv.world.infectAt(wx, wy);
                gv.world.say(bitten == null ? "Tap closer to someone" : "Bitten: " + Names.person(bitten.nameSeed));
                break;
            }
        }
    }

    /** Takes back whatever the last tap or drag created. */
    void undo() {
        for (Object o : gv.lastAction) {
            if (o instanceof Entity) {
                Entity e = (Entity) o;
                e.dead = true;
                e.removed = true;
            } else if (o instanceof float[]) {
                if (((float[]) o).length == 3) gv.world.medkits.remove(o);
                else gv.world.removeBarricade((float[]) o);
            } else if (o instanceof World.Pickup) {
                gv.world.pickups.remove(o);
            } else if (o instanceof Fleet.Vehicle) {
                // ((12.0) Properly gone: its officers no longer think it's theirs.)
                Fleet.Vehicle v = (Fleet.Vehicle) o;
                v.crew.clear();
                v.driver = null;
                v.removedFromFleet = true;
                gv.world.fleet.vehicles.remove(v);
                if (gv.followV == v) gv.followV = null;
            }
        }
        gv.lastAction.clear();
    }

    void startPinch(MotionEvent ev) {
        startPinchExcluding(ev, -1);
    }

    void startPinchExcluding(MotionEvent ev, int skip) {
        int a = -1, b = -1;
        for (int i = 0; i < ev.getPointerCount(); i++) {
            if (i == skip) continue;
            if (a < 0) a = i;
            else if (b < 0) b = i;
        }
        if (a < 0 || b < 0) return;
        float x0 = ev.getX(a), y0 = ev.getY(a), x1 = ev.getX(b), y1 = ev.getY(b);
        gv.pinchDist = (float) World.hypot(x1 - x0, y1 - y0);
        gv.pinchMidX = (x0 + x1) / 2;
        gv.pinchMidY = (y0 + y1) / 2;
    }

    void doPinch(MotionEvent ev) {
        float x0 = ev.getX(0), y0 = ev.getY(0), x1 = ev.getX(1), y1 = ev.getY(1);
        float d = (float) World.hypot(x1 - x0, y1 - y0);
        float mx = (x0 + x1) / 2, my = (y0 + y1) / 2;
        float wx = gv.worldX(gv.pinchMidX), wy = gv.worldY(gv.pinchMidY);
        if (gv.pinchDist > 10 && d > 10) {
            float minScale = Math.min(gv.getWidth() / gv.world.city.worldW(), gv.getHeight() / gv.world.city.worldH()) * 0.8f;
            gv.scale = Math.max(minScale, Math.min(10f, gv.scale * d / gv.pinchDist));
        }
        gv.camX = wx - mx / gv.scale;
        gv.camY = wy - my / gv.scale;
        gv.clampCamera();
        gv.pinchDist = d;
        gv.pinchMidX = mx;
        gv.pinchMidY = my;
    }
}
