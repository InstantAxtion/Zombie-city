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
 * ((27.3) Out of GameView.) Sound and feel: the events the simulation raised this frame turned into sound effects, the
 * ambience and the music's mood, far-off sounds, radio noises, captions and haptics.
 */
final class GameAudio {
    final GameView gv;

    GameAudio(GameView gv) {
        this.gv = gv;
    }


    /** (14.4) The heartbeat under the music, once things are tense, louder and quicker the worse it gets. */
    void heartbeat(float dt, float master) {
        gv.heartCd -= dt;
        if (gv.heartCd > 0 || gv.intensity < 0.35f || master <= 0) return;
        gv.heartCd = 2.6f - 0.8f * gv.intensity;
        gv.sound.play(Sfx.HEART, master * (gv.intensity - 0.3f) * 0.9f, 0, 0);
    }

    /** Music follows the action: calm when it's quiet, intense in a big fight. */
    void updateMood(float dt) {
        gv.moodTimer -= dt;
        gv.moodHold -= dt;
        if (gv.moodTimer > 0) return;
        gv.moodTimer = 1;
        int shots = gv.world.shotsFired - gv.lastShots;
        gv.lastShots = gv.world.shotsFired;
        float vx0 = gv.camX, vy0 = gv.camY, vx1 = gv.camX + gv.getWidth() / gv.scale, vy1 = gv.camY + gv.barTop / gv.scale;
        int onScreen = 0;
        for (int i = 0, n = gv.world.entities.size(); i < n; i++) {
            Entity e = gv.world.entities.get(i);
            if (e.isZombie() && e.x > vx0 && e.x < vx1 && e.y > vy0 && e.y < vy1) onScreen++;
        }
        // ((14.4) How tense it is, 0 to 1: how far the dead have spread through the city sets the floor, the fighting in
        // view the rest. The music follows it - calm, the tension, the fight - and sits louder the higher it is.)
        int zs = gv.world.zombieCount();
        float spread = zs == 0 ? 0 : Math.min(1, 3f * zs / Math.max(1, zs + gv.world.humans));
        float local = Math.min(1, shots / 18f + onScreen / 36f + (gv.world.explosions.isEmpty() ? 0 : 0.35f));
        float target = zs == 0 ? 0 : Math.max(local, 0.2f + 0.45f * spread);
        gv.intensity += (target - gv.intensity) * (target > gv.intensity ? 0.5f : 0.15f);
        gv.sound.setIntensity(gv.intensity);
        int want = zs == 0 || gv.intensity < 0.15f ? Synth.TRACK_CALM
                : gv.intensity >= 0.6f || shots >= 12 || onScreen >= 25 || !gv.world.explosions.isEmpty() ? Synth.TRACK_ACTION : Synth.TRACK_GAME;
        // (10.52) Each mood has three pieces: a new one, picked at random, every couple of minutes and whenever
        // the mood changes.
        gv.moodPlayed += 1;
        if (want == gv.moodBase) {
            if (want == Synth.TRACK_ACTION) gv.moodHold = 15;
            if (gv.moodPlayed > 140) {
                gv.moodPlayed = 0;
                gv.moodVariant = (gv.moodVariant + 1 + gv.rnd.nextInt(2)) % 3;
                gv.mood = Synth.variant(gv.moodBase, gv.moodVariant);
            }
            return;
        }
        if (gv.moodHold > 0 && want != Synth.TRACK_ACTION) return;
        gv.moodBase = want;
        gv.moodVariant = gv.rnd.nextInt(3);
        gv.moodPlayed = 0;
        gv.mood = Synth.variant(gv.moodBase, gv.moodVariant);
        gv.moodHold = 20;
    }

    void haptics() {
        // ((19.5) Whoever you're playing hurt: a red flash at the edges of the screen.)
        Entity me = gv.world.controlled;
        if (me != null && gv.hurtHp >= 0 && me.hp < gv.hurtHp - 0.5f) gv.vfx.hurt = Math.max(gv.vfx.hurt, Math.min(1, 0.4f + (gv.hurtHp - me.hp) / 40));
        gv.hurtHp = me != null ? me.hp : -1;
        if (!gv.settings.vibration()) return;
        Entity e = gv.world.controlled;
        int strength = 0;
        if (e != null) {
            if (gv.lastHp >= 0 && e.hp < gv.lastHp - 0.5f) strength = 2;
            if (gv.world.shotsFired > gv.buzzShots && gv.world.ctrlAttack) strength = Math.max(strength, 1);
            gv.lastHp = e.hp;
        } else gv.lastHp = -1;
        gv.buzzShots = gv.world.shotsFired;
        float vw = gv.getWidth() / gv.scale, vh = gv.getHeight() / gv.scale, cx = gv.camX + vw / 2, cy = gv.camY + vh / 2;
        for (int i = 0; i < gv.world.evCount; i++) {
            int t = gv.world.evType[i];
            if (t != Sfx.EXPLOSION && t != Sfx.CRASH && t != Sfx.CANNON) continue;
            if (Math.abs(gv.world.evX[i] - cx) < vw * 0.6f && Math.abs(gv.world.evY[i] - cy) < vh * 0.6f) strength = Math.max(strength, 2);
        }
        long now = System.currentTimeMillis();
        if (strength == 0 || now - gv.lastBuzz < (strength == 1 ? 90 : 160)) return;
        gv.lastBuzz = now;
        gv.performHapticFeedback(strength == 1 ? android.view.HapticFeedbackConstants.KEYBOARD_TAP
                : android.view.HapticFeedbackConstants.LONG_PRESS);
    }

    /**
     * (14.4) The city in the distance: gunfire, explosions and sirens from fights off screen, too far off to hear as
     * they happen, come through faint and muffled from their side of the screen, a little at a time.
     */
    void distantSounds(float dt, float master) {
        for (int k = 0; k < 3; k++) {
            gv.farCd[k] -= dt;
            if (gv.farCd[k] > 0 || gv.farCount[k] == 0) continue;
            int n = gv.farCount[k];
            float pan = Math.max(-1, Math.min(1, gv.farX[k] / n)) * 0.7f;
            float vol = master * Math.min(k == 0 ? 0.3f : 0.35f, (k == 2 ? 0.12f : 0.1f) + 0.03f * (float) (Math.log(n) / Math.log(2)));
            int id = k == 0 ? gv.farGun : k == 1 ? Sfx.EXPLOSION : Sfx.SIREN;
            gv.sound.play(id, vol, pan, 1);
            if (gv.settings.captions()) gv.hud.caption(id, pan, 0);
            // (Gunfire every second or so while it lasts; blasts now and then; a siren seldom.)
            gv.farCd[k] = k == 0 ? 0.8f + gv.rnd.nextFloat() * 1.2f : k == 1 ? 3 + gv.rnd.nextFloat() * 3 : 9 + gv.rnd.nextFloat() * 6;
            gv.farCount[k] = 0;
            gv.farX[k] = 0;
        }
    }

    void buzzError() {
        gv.sound.play(Sfx.ERROR, 0.6f, 0);
    }

    void later(float seconds, int id, float vol) {
        gv.soon.add(new float[]{seconds, id, vol});
    }

    void playLater(float dt) {
        gv.shockCd -= dt;
        for (int i = gv.soon.size() - 1; i >= 0; i--) {
            float[] s = gv.soon.get(i);
            s[0] -= dt;
            if (s[0] > 0) continue;
            gv.soon.remove(i);
            gv.sound.play((int) s[1], s[2], 0);
        }
    }

    void radioSound() {
        // ((22.0) Fewer beeps: one at a time, at most every four seconds - several messages in a burst made a racket.
        // A 911 call always rings.)
        java.util.ArrayList<Dispatch.Message> log0 = gv.world.dispatch.log;
        String who0 = log0.isEmpty() || log0.get(log0.size() - 1).who == null ? "" : log0.get(log0.size() - 1).who;
        if (gv.world.time < gv.radioQuietUntil && !who0.startsWith("911")) return;
        gv.radioQuietUntil = gv.world.time + 4;
        java.util.ArrayList<Dispatch.Message> log = gv.world.dispatch.log;
        String who = log.isEmpty() ? "" : log.get(log.size() - 1).who;
        String w = who == null ? "" : who.toLowerCase(java.util.Locale.US);
        if (w.startsWith("911")) gv.sound.play(Sfx.PHONE, 0.35f, 0);
        else if (w.contains("milit") || w.contains("command") || w.contains("army")) gv.sound.play(Sfx.MIL_RADIO, 0.45f, 0);
        else gv.sound.play(Sfx.RADIO, 0.5f, 0);
        later(0.9f + gv.rnd.nextFloat() * 0.3f, Sfx.SQUELCH, 0.3f);
    }

    /** In the menus: just the city's hum behind them, the rest faded. */
    void ambienceQuiet() {
        for (int l = 0; l < Sfx.LOOPS; l++)
            gv.sound.setLoop(l, gv.menu.screen == Menu.SOUNDS && gv.menu.previewLoop == l ? 0.6f : l == Sfx.LOOP_CITY && gv.hasGame && gv.menu.screen != Menu.SOUNDS ? 0.08f : 0, 0, 0);
    }

    /**
     * The beds of sound under everything, from what's on the screen: traffic and the city's hum, wind (more zoomed out), birds in the parks when it's quiet, a crowd where people are gathered, water by the sea or a
     * lake, a low drone of tension under a fight.
     */
    void ambience(float dt) {
        gv.ambT -= dt;
        if (gv.ambT > 0) return;
        gv.ambT = 0.25f;
        float vw = gv.getWidth() / gv.scale, vh = gv.getHeight() / gv.scale, cx = gv.camX + vw / 2, cy = gv.camY + vh / 2;
        float zoomOut = Math.max(0, Math.min(1, (1.4f - gv.scale) / 1.2f));
        // What's on the screen: people, people running, cars; trees and water from a sample of the ground.
        int people = 0, fleeing = 0;
        for (int i = 0, n = gv.onScreen.size(); i < n; i++) {
            Entity e = gv.onScreen.get(i);
            if (e.dead || e.type != Entity.CIVILIAN) continue;
            people++;
            if (e.fleeTimer > 0) fleeing++;
        }
        int cars = 0;
        for (int i = 0, n = gv.world.fleet.vehicles.size(); i < n; i++) {
            Fleet.Vehicle v = gv.world.fleet.vehicles.get(i);
            if (Math.abs(v.x - cx) < vw * 0.7f && Math.abs(v.y - cy) < vh * 0.7f && v.speed > 5) cars++;
        }
        int trees = 0, water = 0, samples = 0;
        City city = gv.world.city;
        for (int gy = 0; gy < 5; gy++)
            for (int gx = 0; gx < 5; gx++) {
                int t = city.tileIndex(gv.camX + vw * (gx + 0.5f) / 5, gv.camY + vh * (gy + 0.5f) / 5);
                if (t < 0) continue;
                samples++;
                int k = city.tiles[t];
                if (k == City.WATER) water++;
                else if (k == City.TREE || k == City.GRASS) trees++;
            }
        float calm = Math.max(0, 1 - gv.intensity * 1.6f);
        float wTrees = samples == 0 ? 0 : trees / (float) samples, wWater = samples == 0 ? 0 : water / (float) samples;
        gv.sound.setLoop(Sfx.LOOP_CITY, 0.10f + Math.min(0.25f, cars * 0.03f) + 0.08f * zoomOut, 0, 0.35f * zoomOut);
        gv.sound.setLoop(Sfx.LOOP_WIND, 0.04f + 0.18f * zoomOut, 0, 0.2f);
        gv.sound.setLoop(Sfx.LOOP_BIRDS, wTrees * calm * (1 - zoomOut) * 0.35f, 0, 0);
        gv.sound.setLoop(Sfx.LOOP_CROWD, Math.min(0.35f, people * 0.006f) * (1 - zoomOut * 0.6f), 0, 0.2f);
        if (fleeing >= 8 && gv.rnd.nextFloat() < 0.15f) gv.sound.play(Sfx.PANIC, Math.min(0.5f, fleeing * 0.02f), (gv.rnd.nextFloat() - 0.5f) * 0.6f, 0.2f);
        gv.sound.setLoop(Sfx.LOOP_WATER, wWater * 0.5f, 0, 0.1f);
        gv.sound.setLoop(Sfx.LOOP_TENSION, Math.max(0, gv.intensity - 0.45f) * 0.35f, 0, 0.3f);
    }

    void playSounds(float master) {
        haptics();
        float vw = gv.getWidth() / gv.scale, vh = gv.getHeight() / gv.scale;
        float cx = gv.camX + vw / 2, cy = gv.camY + vh / 2, reach = Math.max(vw, vh) * 0.75f;
        for (int i = 0; i < gv.world.evCount; i++) {
            float ddx = gv.world.evX[i] - cx, ddy = gv.world.evY[i] - cy;
            float d = (float) Math.sqrt(ddx * ddx + ddy * ddy);
            int type = gv.world.evType[i];
            // (10.32) Loud things carry: a battle, an explosion or a helicopter is heard a long way off, faintly;
            // a groan only close by.
            float r = reach * Sfx.carry(type);
            if (d > r) {
                // ((14.4) Too far off to hear as it is: counted for the distant sound of the city (see distantSounds).
                int k = Sfx.gun(type) ? 0 : type == Sfx.EXPLOSION || type == Sfx.CANNON || type == Sfx.COLLAPSE ? 1
                        : type == Sfx.SIREN || type == Sfx.AMB_SIREN ? 2 : -1;
                if (k >= 0) {
                    gv.farCount[k]++;
                    gv.farX[k] += ddx / Math.max(1, d);
                    if (k == 0) gv.farGun = type;
                }
                continue;
            }
            float f = d / r;
            float vol = master * (1 - 0.85f * f) * (1 - 0.15f * f * f);
            // ((19.0) From inside a building: through its walls, duller and quieter.)
            if (!Sfx.ui(type) && gv.world.city.insideOf(gv.world.evX[i], gv.world.evY[i]) != null) {
                f = Math.min(1, f + 0.4f);
                vol *= 0.7f;
            }
            // ((19.0) Off the screen: a caption for it, from its side.)
            if (gv.settings.captions() && master >= 1 && (Math.abs(ddx) > vw / 2 || Math.abs(ddy) > vh / 2)) gv.hud.caption(type, ddx, ddy);
            // ((19.0) A big blast right by the middle of the screen, zoomed in: ringing ears.)
            if (master >= 1 && gv.settings.shellShock() && (type == Sfx.EXPLOSION || type == Sfx.CANNON)
                    && d < 90 && gv.scale > 1.2f && gv.shockCd <= 0) {
                float amt = Math.min(1, (1 - d / 90) * (gv.scale / 2.5f));
                if (amt > 0.25f) {
                    gv.sound.shock(amt);
                    gv.vfx.daze = Math.max(gv.vfx.daze, amt);
                    gv.sound.play(Sfx.WHINE, 0.25f + 0.35f * amt, 0);
                    gv.shockCd = 4;
                }
            }
            if (type == Sfx.GROAN || type == Sfx.GROAN_DEEP || type == Sfx.SCREAM) vol *= 0.6f;
            // ((11.7) Gunshots a bit louder, near and far.)
            if (Sfx.gun(type)) vol *= 1.3f;
            // ((13.4) And how far off: the mixer muffles it and gives it more echo the further away it is.)
            gv.sound.play(type, vol, Math.max(-1, Math.min(1, ddx / (vw / 2))) * 0.8f, f);
        }
        gv.world.evCount = 0;
    }
}
