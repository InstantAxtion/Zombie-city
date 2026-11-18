package com.instantaxtion.zombiesandbox;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.Random;

/**
 * ((19.5) The Visuals Update: everything drawn for the look of it and nothing else - the simulation never sees any of
 * it. Light thrown by gunfire, blasts, fires and the mothership; shockwaves, craters, flying debris and sparks; smoke
 * rising from wrecks and ruins and drifting with the wind; embers, floodwater ripples, a tornado's debris, the ground
 * cracking in a quake; and over the whole screen flashes, the red of danger, a daze after a blast close by,
 * each mode's own colour, and black bars for a cinematic look with the HUD hidden. Fed from the world's sound events
 * (every bang the city makes is something to see as well) and from what's going on, once a frame.
 */
final class Visuals {
    /** How much is drawn: 0 low, 1 medium, 2 high, 3 ultra. */
    int quality = 2;
    /** The effects over the whole screen, and each mode's colour. */
    boolean screen = true, grading = true;

    private final Random rnd = new Random();
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), stroke = new Paint(Paint.ANTI_ALIAS_FLAG), glowPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Rect src = new Rect();
    private final RectF dst = new RectF();

    // Glow sprites, one per colour: a soft round falloff.
    static final int G_FIRE = 0, G_FLASH = 1, G_RED = 2, G_BLUE = 3, G_GREEN = 4, G_WHITE = 5;
    private static final int[] GLOW_COLORS = {0xFFFF8A30, 0xFFFFE6A0, 0xFFFF3030, 0xFF4070FF, 0xFF60FF90, 0xFFFFFFFF};
    private final Bitmap[] glow = new Bitmap[GLOW_COLORS.length];
    private static final int GLOW = 64;

    /** Lights: {x, y, radius, life left, life, glow, strength, flicker}. */
    final ArrayList<float[]> lights = new ArrayList<float[]>();
    /** Shockwaves: {x, y, radius reached, top radius, life left, life}. */
    final ArrayList<float[]> waves = new ArrayList<float[]>();
    /** Craters left by blasts: {x, y, radius, age}. */
    final ArrayList<float[]> craters = new ArrayList<float[]>();
    /** Cracks in the ground after a quake: {x0, y0, x1, y1, age, width}. */
    final ArrayList<float[]> cracks = new ArrayList<float[]>();
    /** Smoke and dust: {x, y, vx, vy, radius, life left, life, shade (0 black to 1 pale), growth}. */
    final ArrayList<float[]> smoke = new ArrayList<float[]>();
    /** Sparks and embers: {x, y, vx, vy, life left, life, colour, rising (1) or falling (0)}. */
    final ArrayList<float[]> sparks = new ArrayList<float[]>();
    /** Debris thrown into the air: {x, y, z, vx, vy, vz, size, spin, colour, life}. */
    final ArrayList<float[]> debris = new ArrayList<float[]>();
    /** Flashes on the minimap where something big happened, anywhere in the city: {x, y, life left, kind (0 blast, 1 other)}. */
    final ArrayList<float[]> pings = new ArrayList<float[]>();
    /** Rings on water: {x, y, radius, life left}. */
    final ArrayList<float[]> ripples = new ArrayList<float[]>();
    /** Places still smoking: {x, y, seconds left, how heavily, burning (1) or just smoking (0)}. */
    final ArrayList<float[]> smoulder = new ArrayList<float[]>();
    /** Wrecks and when they were wrecked (seconds of the game), for their fire and then their smoke. */
    private final java.util.IdentityHashMap<Fleet.Vehicle, Float> wrecked = new java.util.IdentityHashMap<Fleet.Vehicle, Float>();

    /** The wind the smoke drifts on: slowly turning. */
    private float windA = 0.7f, windS = 6;
    /** Over the whole screen: a white flash, a red flash (hurt), a daze (deafened by a blast). */
    float flash, hurt, daze;
    private float time;
    private World lastWorld;

    Visuals() {
        for (int g = 0; g < glow.length; g++) {
            int[] px = new int[GLOW * GLOW];
            int rgb = GLOW_COLORS[g] & 0xFFFFFF;
            for (int y = 0; y < GLOW; y++)
                for (int x = 0; x < GLOW; x++) {
                    float dx = (x + 0.5f) / GLOW * 2 - 1, dy = (y + 0.5f) / GLOW * 2 - 1;
                    float d = (float) Math.sqrt(dx * dx + dy * dy);
                    float a = d >= 1 ? 0 : (1 - d) * (1 - d);
                    // (A brighter, whiter middle.)
                    int col = rgb;
                    if (d < 0.25f) col = mix(rgb, 0xFFFFFF, (0.25f - d) * 2);
                    px[y * GLOW + x] = ((int) (a * 255) << 24) | col;
                }
            glow[g] = Bitmap.createBitmap(px, GLOW, GLOW, Bitmap.Config.ARGB_8888);
        }
        src.set(0, 0, GLOW, GLOW);
    }

    private static int mix(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t), g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t),
                bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static int alpha(int color, float a) {
        return ((int) (Math.max(0, Math.min(1, a)) * 255) << 24) | (color & 0xFFFFFF);
    }

    /** A new city on its way: the last one let go of (its wrecks were held here too). */
    void forget() {
        reset();
        lastWorld = null;
    }

    /** A new city: nothing carried over. */
    void reset() {
        lights.clear();
        waves.clear();
        craters.clear();
        cracks.clear();
        smoke.clear();
        sparks.clear();
        debris.clear();
        ripples.clear();
        smoulder.clear();
        pings.clear();
        wrecked.clear();
        flash = hurt = daze = 0;
    }

    private int cap(int high) {
        return quality == 0 ? high / 4 : quality == 1 ? high / 2 : quality == 3 ? high * 3 / 2 : high;
    }

    void light(float x, float y, float r, float life, int glowKind, float strength, boolean flicker) {
        if (quality == 0 && r < 60) return;
        if (lights.size() >= cap(90)) lights.remove(0);
        lights.add(new float[]{x, y, r, life, life, glowKind, strength, flicker ? 1 : 0, 0});
    }

    private void puff(float x, float y, float vx, float vy, float r, float life, float shade, float grow) {
        if (smoke.size() >= cap(260)) smoke.remove(0);
        smoke.add(new float[]{x, y, vx, vy, r, life, life, shade, grow});
    }

    /** ((21.3) A fire's flame or glow on the ground: drawn under the people walking through it, not on top of them. */
    private void groundLight(float x, float y, float r, float life, float strength) {
        light(x, y, r, life, G_FIRE, strength, true);
        if (!lights.isEmpty()) lights.get(lights.size() - 1)[8] = 1;
    }

    private void groundSpark(float x, float y, float vx, float vy, float life, int color) {
        spark(x, y, vx, vy, life, color, true);
        if (!sparks.isEmpty()) sparks.get(sparks.size() - 1)[8] = 1;
    }

    private void spark(float x, float y, float vx, float vy, float life, int color, boolean rising) {
        if (sparks.size() >= cap(240)) sparks.remove(0);
        sparks.add(new float[]{x, y, vx, vy, life, life, color, rising ? 1 : 0, 0});
    }

    /**
     * The city's sounds this frame, as things to see: a muzzle's flash of light, a blast's fireball, shockwave, crater,
     * debris and smoke, a collapse's dust, a quake's cracks, the beam's glare. Only those near the screen.
     */
    void events(World w, float vx0, float vy0, float vx1, float vy1) {
        if (w != lastWorld) {
            reset();
            lastWorld = w;
        }
        float m = 250;
        for (int i = 0; i < w.evCount; i++) {
            float x = w.evX[i], y = w.evY[i];
            int pt = w.evType[i];
            if ((pt == Sfx.EXPLOSION || pt == Sfx.CANNON || pt == Sfx.COLLAPSE) && pings.size() < 24)
                pings.add(new float[]{x, y, 1.6f, pt == Sfx.COLLAPSE ? 1 : 0});
            if (x < vx0 - m || x > vx1 + m || y < vy0 - m || y > vy1 + m) continue;
            int t = w.evType[i];
            if (Sfx.gun(t)) {
                if (quality >= 1) light(x, y, t == Sfx.MG || t == Sfx.SHOTGUN ? 34 : 24, 0.08f, G_FLASH, 0.55f, false);
            } else if (t == Sfx.EXPLOSION || t == Sfx.CANNON || t == Sfx.LAUNCHER) {
                blast(w, x, y, t == Sfx.EXPLOSION ? 1f : 0.6f, vx0, vy0, vx1, vy1);
            } else if (t == Sfx.COLLAPSE) {
                for (int k = 0; k < cap(26); k++) {
                    float a = rnd.nextFloat() * 6.283f, s = 10 + rnd.nextFloat() * 40;
                    puff(x + (float) Math.cos(a) * 15, y + (float) Math.sin(a) * 15, (float) Math.cos(a) * s, (float) Math.sin(a) * s,
                            10 + rnd.nextFloat() * 12, 4 + rnd.nextFloat() * 4, 0.75f, 5);
                }
                smoulder.add(new float[]{x, y, 45, 1, 0});
            } else if (t == Sfx.FLARE) {
                light(x, y, 120, 0.5f, G_WHITE, 0.9f, false);
                for (int k = 0; k < cap(10); k++)
                    spark(x, y, rnd.nextFloat() * 120 - 60, rnd.nextFloat() * 120 - 60, 0.4f + rnd.nextFloat() * 0.4f, 0xFFFF7A5A, true);
            } else if (t == Sfx.CRASH) {
                for (int k = 0; k < cap(8); k++)
                    spark(x, y, rnd.nextFloat() * 160 - 80, rnd.nextFloat() * 160 - 80, 0.3f + rnd.nextFloat() * 0.3f, 0xFFFFD27A, false);
            }
        }
        while (ripples.size() > cap(80)) ripples.remove(0);
    }

    /** A blast: fireball light, a flash, the shockwave, a crater, debris, sparks and a column of smoke after. */
    private void blast(World w, float x, float y, float size, float vx0, float vy0, float vx1, float vy1) {
        light(x, y, 170 * size, 0.7f, G_FIRE, 1, true);
        light(x, y, 80 * size, 0.15f, G_WHITE, 0.9f, false);
        waves.add(new float[]{x, y, 4, 110 * size, 0.45f, 0.45f});
        int tile = w.city.tileIndex(x, y);
        boolean ground = tile >= 0 && w.city.tiles[tile] != City.WATER;
        if (ground) {
            if (craters.size() >= cap(140)) craters.remove(0);
            craters.add(new float[]{x, y, (9 + rnd.nextFloat() * 5) * size + 3, 0});
        } else for (int k = 0; k < 5; k++) ripples.add(new float[]{x, y, 4 + k * 6, 2.2f});
        for (int k = 0; k < cap(14) * size; k++) {
            float a = rnd.nextFloat() * 6.283f, s = 60 + rnd.nextFloat() * 160;
            spark(x, y, (float) Math.cos(a) * s, (float) Math.sin(a) * s, 0.35f + rnd.nextFloat() * 0.4f, rnd.nextBoolean() ? 0xFFFFC060 : 0xFFFFE8A0, false);
        }
        if (quality >= 1)
            for (int k = 0; k < cap(9) * size; k++) {
                if (debris.size() >= cap(120)) debris.remove(0);
                float a = rnd.nextFloat() * 6.283f, s = 30 + rnd.nextFloat() * 90;
                debris.add(new float[]{x, y, 0, (float) Math.cos(a) * s, (float) Math.sin(a) * s, 40 + rnd.nextFloat() * 70,
                        1.2f + rnd.nextFloat() * 1.8f, rnd.nextFloat() * 6, rnd.nextBoolean() ? 0xFF4A4642 : 0xFF6E655A, 0});
            }
        for (int k = 0; k < cap(10) * size; k++) {
            float a = rnd.nextFloat() * 6.283f, s = 10 + rnd.nextFloat() * 30;
            puff(x, y, (float) Math.cos(a) * s, (float) Math.sin(a) * s, 8 + rnd.nextFloat() * 8 * size, 2.5f + rnd.nextFloat() * 2, 0.2f, 6);
        }
        smoulder.add(new float[]{x, y, 18 * size, 0.6f * size, 1});
        // A flash over the screen if it went off right in the middle of it.
        float cx = (vx0 + vx1) / 2, cy = (vy0 + vy1) / 2, half = (vx1 - vx0) / 2;
        float d = (float) Math.hypot(x - cx, y - cy);
        if (d < half) flash = Math.max(flash, 0.35f * size * (1 - d / half));
    }


    /**
     * A frame on: lights fading, smoke rising and drifting, sparks and debris flying; and what's going on now -
     * wrecks burning then smoking, vehicles trailing smoke when badly hurt, fires throwing embers and light, the
     * tornado's debris, floodwater rings, bombs pulsing, the mothership's glow and its drones' eyes.
     */
    void update(World w, float dt, float vx0, float vy0, float vx1, float vy1, java.util.List<Entity> onScreen) {
        time += dt;
        flash = Math.max(0, flash - dt * 2.5f);
        hurt = Math.max(0, hurt - dt * 1.8f);
        daze = Math.max(0, daze - dt * 0.4f);
        if (dt <= 0) return;
        windA += (rnd.nextFloat() - 0.5f) * dt * 0.05f;
        windS = 5 + 3 * (float) Math.sin(time * 0.07f);
        float wx = (float) Math.cos(windA) * windS, wy = (float) Math.sin(windA) * windS;
        for (int i = pings.size() - 1; i >= 0; i--) {
            float[] p = pings.get(i);
            p[2] -= dt;
            if (p[2] <= 0) pings.remove(i);
        }
        for (int i = lights.size() - 1; i >= 0; i--) {
            float[] l = lights.get(i);
            l[3] -= dt;
            if (l[3] <= 0) lights.remove(i);
        }
        for (int i = waves.size() - 1; i >= 0; i--) {
            float[] s = waves.get(i);
            s[4] -= dt;
            s[2] = s[3] * (1 - s[4] / s[5]);
            if (s[4] <= 0) waves.remove(i);
        }
        for (float[] c : craters) c[3] += dt;
        for (int i = cracks.size() - 1; i >= 0; i--) {
            float[] c = cracks.get(i);
            c[4] += dt;
            if (c[4] > 240) cracks.remove(i);
        }
        for (int i = smoke.size() - 1; i >= 0; i--) {
            float[] s = smoke.get(i);
            s[5] -= dt;
            if (s[5] <= 0) {
                smoke.remove(i);
                continue;
            }
            s[2] += (wx - s[2]) * dt * 0.5f;
            s[3] += (wy - 8 - s[3]) * dt * 0.5f;
            s[0] += s[2] * dt;
            s[1] += s[3] * dt;
            s[4] += s[8] * dt;
        }
        for (int i = sparks.size() - 1; i >= 0; i--) {
            float[] s = sparks.get(i);
            s[4] -= dt;
            if (s[4] <= 0) {
                sparks.remove(i);
                continue;
            }
            float drag = (float) Math.exp(-dt * (s[7] > 0 ? 1.2f : 3.5f));
            s[2] *= drag;
            s[3] *= drag;
            if (s[7] > 0) {
                s[2] += wx * dt;
                s[3] += (wy - 18) * dt;
            }
            s[0] += s[2] * dt;
            s[1] += s[3] * dt;
        }
        for (int i = debris.size() - 1; i >= 0; i--) {
            float[] d = debris.get(i);
            d[9] += dt;
            if (d[2] > 0 || d[5] > 0) {
                d[5] -= 220 * dt;
                d[0] += d[3] * dt;
                d[1] += d[4] * dt;
                d[2] = Math.max(0, d[2] + d[5] * dt);
                d[7] += dt * 8;
                if (d[2] <= 0 && d[5] < 0) {
                    d[5] = 0;
                    d[3] = d[4] = 0;
                }
            }
            if (d[9] > 25) debris.remove(i);
        }
        for (int i = ripples.size() - 1; i >= 0; i--) {
            float[] r = ripples.get(i);
            r[3] -= dt;
            r[2] += dt * 18;
            if (r[3] <= 0) ripples.remove(i);
        }
        // Smoke from where something blew up or came down, and fire there at first.
        for (int i = smoulder.size() - 1; i >= 0; i--) {
            float[] s = smoulder.get(i);
            s[2] -= dt;
            if (s[2] <= 0) {
                smoulder.remove(i);
                continue;
            }
            boolean near = s[0] > vx0 - 300 && s[0] < vx1 + 300 && s[1] > vy0 - 300 && s[1] < vy1 + 300;
            if (!near) continue;
            if (rnd.nextFloat() < dt * 6 * s[3] * (quality + 1) / 3f)
                puff(s[0] + rnd.nextFloat() * 10 - 5, s[1] + rnd.nextFloat() * 10 - 5, 0, -6, 4 + rnd.nextFloat() * 4, 5 + rnd.nextFloat() * 3, 0.25f, 3);
            if (s[4] > 0 && s[2] > 8 && rnd.nextFloat() < dt * 8) {
                groundSpark(s[0] + rnd.nextFloat() * 8 - 4, s[1], rnd.nextFloat() * 10 - 5, -20 - rnd.nextFloat() * 20, 1 + rnd.nextFloat(), 0xFFFF9A40);
                if (rnd.nextFloat() < 0.3f) groundLight(s[0], s[1], 40, 0.3f, 0.5f);
            }
        }
        // Vehicles: wrecks burn for a while, then smoke; badly hurt ones trail smoke.
        for (int i = 0, n = w.fleet.vehicles.size(); i < n; i++) {
            Fleet.Vehicle v = w.fleet.vehicles.get(i);
            boolean on = v.x > vx0 - 100 && v.x < vx1 + 100 && v.y > vy0 - 100 && v.y < vy1 + 100;
            if (v.broken) {
                Float at = wrecked.get(v);
                if (at == null) {
                    wrecked.put(v, w.time);
                    at = w.time;
                }
                float age = w.time - at;
                if (!on || age > 120 || Fleet.airborne(v)) continue;
                if (age < 40) {
                    if (rnd.nextFloat() < dt * 10) groundSpark(v.x + rnd.nextFloat() * 8 - 4, v.y + rnd.nextFloat() * 8 - 4, rnd.nextFloat() * 10 - 5, -25 - rnd.nextFloat() * 20, 0.8f + rnd.nextFloat(), 0xFFFFA040);
                    if (rnd.nextFloat() < dt * 12) groundLight(v.x, v.y, 46 + rnd.nextFloat() * 10, 0.2f, 0.6f);
                }
                if (rnd.nextFloat() < dt * (age < 40 ? 6 : 2.5f) * (quality + 1) / 3f)
                    puff(v.x, v.y, 0, -8, 5 + rnd.nextFloat() * 3, 6 + rnd.nextFloat() * 3, age < 40 ? 0.1f : 0.4f, 3.5f);
            } else if (on && v.maxHp > 0 && v.hp < v.maxHp * 0.4f && v.type != Fleet.TRAIN && rnd.nextFloat() < dt * 4)
                puff(v.x, v.y, 0, -4, 3, 3, 0.55f, 3);
        }
        if (wrecked.size() > 200) wrecked.clear();
    }

    /** Under the people and vehicles: craters, cracks and ripples. */
    void drawGround(Canvas c, float vx0, float vy0, float vx1, float vy1) {
        for (int i = 0, n = craters.size(); i < n; i++) {
            float[] k = craters.get(i);
            if (k[0] < vx0 - 30 || k[0] > vx1 + 30 || k[1] < vy0 - 30 || k[1] > vy1 + 30) continue;
            float fade = Math.max(0.35f, 1 - k[3] / 600);
            fill.setColor(alpha(0xFF6A5F52, 0.45f * fade));
            c.drawCircle(k[0], k[1], k[2] * 1.25f, fill);
            fill.setColor(alpha(0xFF1A1714, 0.75f * fade));
            c.drawCircle(k[0], k[1], k[2], fill);
            fill.setColor(alpha(0xFF2E2924, 0.8f * fade));
            c.drawCircle(k[0] + k[2] * 0.15f, k[1] + k[2] * 0.15f, k[2] * 0.6f, fill);
        }
        stroke.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0, n = cracks.size(); i < n; i++) {
            float[] k = cracks.get(i);
            if (Math.max(k[0], k[2]) < vx0 || Math.min(k[0], k[2]) > vx1 || Math.max(k[1], k[3]) < vy0 || Math.min(k[1], k[3]) > vy1) continue;
            float fade = Math.max(0, 1 - k[4] / 240);
            stroke.setStrokeWidth(k[5]);
            stroke.setColor(alpha(0xFF141210, 0.8f * fade));
            c.drawLine(k[0], k[1], k[2], k[3], stroke);
        }
        stroke.setStrokeCap(Paint.Cap.BUTT);
        // ((21.3) Fires on the ground - burning wrecks, where something blew up - under everyone, glow and flames.)
        for (int i = 0, n = lights.size(); i < n; i++) {
            float[] l = lights.get(i);
            if (l[8] <= 0) continue;
            float r = l[2];
            if (l[0] + r < vx0 || l[0] - r > vx1 || l[1] + r < vy0 || l[1] - r > vy1) continue;
            float f = l[3] / l[4] * (0.8f + 0.2f * rnd.nextFloat());
            glowPaint.setAlpha((int) (Math.max(0, Math.min(1, f * l[6])) * 255 * 0.85f));
            dst.set(l[0] - r, l[1] - r, l[0] + r, l[1] + r);
            c.drawBitmap(glow[(int) l[5]], src, dst, glowPaint);
        }
        stroke.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0, n = sparks.size(); i < n; i++) {
            float[] s = sparks.get(i);
            if (s[8] <= 0 || s[0] < vx0 - 20 || s[0] > vx1 + 20 || s[1] < vy0 - 20 || s[1] > vy1 + 20) continue;
            float f = s[4] / s[5];
            stroke.setStrokeWidth(1.1f);
            stroke.setColor(alpha((int) s[6], Math.min(1, f * 1.5f)));
            c.drawLine(s[0], s[1], s[0] - s[2] * 0.03f, s[1] - s[3] * 0.03f, stroke);
        }
        stroke.setStrokeCap(Paint.Cap.BUTT);
        // ((21.0) Razor wire: coils across the tile between two posts, thinner as it wears.)
        if (lastWorld != null && lastWorld.defences.any()) {
            java.util.List<float[]> wires = lastWorld.defences.wires;
            float h = City.T / 2f;
            for (int i = 0, n = wires.size(); i < n; i++) {
                float[] k = wires.get(i);
                if (k[0] < vx0 - h || k[0] > vx1 + h || k[1] < vy0 - h || k[1] > vy1 + h) continue;
                float left = Math.max(0.3f, k[2] / Defences.WIRE_HP);
                stroke.setStrokeWidth(0.5f + 0.4f * left);
                stroke.setColor(alpha(0xFFC4C8CE, 0.55f + 0.4f * left));
                for (int q = 0; q < 4; q++) c.drawCircle(k[0] - h + (q + 0.5f) * h / 2, k[1], h * 0.34f, stroke);
                stroke.setColor(alpha(0xFF8A8E94, 0.9f));
                c.drawLine(k[0] - h, k[1], k[0] + h, k[1], stroke);
                fill.setColor(0xFF4A4E54);
                c.drawRect(k[0] - h, k[1] - 1.2f, k[0] - h + 1.2f, k[1] + 1.2f, fill);
                c.drawRect(k[0] + h - 1.2f, k[1] - 1.2f, k[0] + h, k[1] + 1.2f, fill);
            }
        }
        stroke.setStrokeWidth(0.9f);
        for (int i = 0, n = ripples.size(); i < n; i++) {
            float[] r = ripples.get(i);
            stroke.setColor(alpha(0xFFDDEEFF, Math.min(1, r[3]) * 0.45f));
            c.drawCircle(r[0], r[1], r[2], stroke);
        }
    }

    /** Over everything in the world: light, shockwaves, smoke, sparks and debris. */
    void drawAbove(Canvas c, float vx0, float vy0, float vx1, float vy1) {
        // Debris (and its shadow) first, then the smoke over it.
        for (int i = 0, n = debris.size(); i < n; i++) {
            float[] d = debris.get(i);
            if (d[0] < vx0 - 20 || d[0] > vx1 + 20 || d[1] < vy0 - 20 || d[1] > vy1 + 20) continue;
            float fade = d[9] > 20 ? (25 - d[9]) / 5 : 1;
            fill.setColor(alpha(0xFF000000, 0.3f * fade));
            c.drawCircle(d[0] + d[2] * 0.15f, d[1] + d[2] * 0.3f, d[6] * 0.8f, fill);
            fill.setColor(alpha((int) d[8], fade));
            float s = d[6] * (1 + d[2] / 60);
            c.save();
            c.translate(d[0], d[1] - d[2] * 0.6f);
            c.rotate(d[7] * 57.3f);
            c.drawRect(-s, -s * 0.6f, s, s * 0.6f, fill);
            c.restore();
        }
        for (int i = 0, n = smoke.size(); i < n; i++) {
            float[] s = smoke.get(i);
            if (s[0] < vx0 - 60 || s[0] > vx1 + 60 || s[1] < vy0 - 60 || s[1] > vy1 + 60) continue;
            float k = 1 - s[5] / s[6], a = (k < 0.1f ? k * 10 : 1 - (k - 0.1f) / 0.9f) * 0.42f;
            int shade = mix(0x1C1C1E, 0xC8C2B6, s[7]);
            fill.setColor(alpha(shade, a));
            c.drawCircle(s[0], s[1], s[4], fill);
        }
        for (int i = 0, n = waves.size(); i < n; i++) {
            float[] s = waves.get(i);
            float f = s[4] / s[5];
            stroke.setStrokeWidth(2 + 4 * f);
            stroke.setColor(alpha(0xFFFFF4DA, 0.5f * f));
            c.drawCircle(s[0], s[1], s[2], stroke);
            stroke.setStrokeWidth(1);
            stroke.setColor(alpha(0xFFB8A890, 0.35f * f));
            c.drawCircle(s[0], s[1], s[2] * 0.75f, stroke);
        }
        stroke.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0, n = sparks.size(); i < n; i++) {
            float[] s = sparks.get(i);
            if (s[8] > 0) continue;
            if (s[0] < vx0 - 20 || s[0] > vx1 + 20 || s[1] < vy0 - 20 || s[1] > vy1 + 20) continue;
            float f = s[4] / s[5];
            stroke.setStrokeWidth(s[7] > 0 ? 1.1f : 0.9f);
            stroke.setColor(alpha((int) s[6], Math.min(1, f * 1.5f)));
            float tl = s[7] > 0 ? 0.03f : 0.025f;
            c.drawLine(s[0], s[1], s[0] - s[2] * tl, s[1] - s[3] * tl, stroke);
        }
        stroke.setStrokeCap(Paint.Cap.BUTT);
        drawTools(c, vx0, vy0, vx1, vy1);
        // The light, last: glows over the ground and everyone on it.
        for (int i = 0, n = lights.size(); i < n; i++) {
            float[] l = lights.get(i);
            if (l[8] > 0) continue;
            float r = l[2];
            if (l[0] + r < vx0 || l[0] - r > vx1 || l[1] + r < vy0 || l[1] - r > vy1) continue;
            float f = l[3] / l[4];
            if (l[7] > 0) f *= 0.8f + 0.2f * rnd.nextFloat();
            glowPaint.setAlpha((int) (Math.max(0, Math.min(1, f * l[6])) * 255 * 0.85f));
            dst.set(l[0] - r, l[1] - r, l[0] + r, l[1] + r);
            c.drawBitmap(glow[(int) l[5]], src, dst, glowPaint);
        }
    }

    /**
     * Over the whole screen: the red edge of danger (tension) or of being hurt, a blast's flash, a daze,
     * the mode's colour, and black bars with the HUD hidden.
     */
    void drawScreen(Canvas c, int w, int h, float tension, boolean cinematic) {
        if (grading) {
            int tint = 0x0C406030;
            fill.setColor(tint);
            c.drawRect(0, 0, w, h, fill);
        }
        if (screen && quality >= 1) {
            // ((19.7) No vignette: the edges stay as bright as the middle.)
            float edge = Math.min(w, h) * 0.18f;
            float red = Math.max(hurt, tension > 0.6f ? (tension - 0.6f) * (0.6f + 0.4f * (float) Math.sin(time * 4)) : 0);
            if (red > 0.01f) {
                for (int k = 0; k < 5; k++) {
                    float in = edge * 1.2f * (1 - k / 5f);
                    fill.setColor(alpha(0xFFB00010, red * 0.06f));
                    c.drawRect(0, 0, w, in, fill);
                    c.drawRect(0, h - in, w, h, fill);
                    c.drawRect(0, in, in, h - in, fill);
                    c.drawRect(w - in, in, w, h - in, fill);
                }
            }
            if (daze > 0.01f) {
                fill.setColor(alpha(0xFFE8E4DA, daze * 0.35f));
                c.drawRect(0, 0, w, h, fill);
            }
            if (flash > 0.01f) {
                fill.setColor(alpha(0xFFFFF2DC, flash));
                c.drawRect(0, 0, w, h, fill);
            }
        }
        if (cinematic) {
            float bar = h * 0.085f;
            fill.setColor(0xFF000000);
            c.drawRect(0, 0, w, bar, fill);
            c.drawRect(0, h - bar, w, h, fill);
        }
    }

    // ---- ((20.0) Flares and mines ----

    /** Mines on the ground, and flares burning. */
    private void drawTools(Canvas c, float vx0, float vy0, float vx1, float vy1) {
        World w = lastWorld;
        if (w == null) return;
        for (int i = 0, n = w.mines.size(); i < n; i++) {
            float[] m = w.mines.get(i);
            if (m[0] < vx0 || m[0] > vx1 || m[1] < vy0 || m[1] > vy1) continue;
            fill.setColor(0xFF23261F);
            c.drawCircle(m[0], m[1], 2.2f, fill);
            fill.setColor(m[2] > 0 ? 0xFFE0C040 : ((int) (time * 2 + i)) % 2 == 0 ? 0xFFFF3020 : 0xFF601410);
            c.drawCircle(m[0], m[1], 0.8f, fill);
        }
        for (int i = 0, n = w.flares.size(); i < n; i++) {
            float[] f = w.flares.get(i);
            float r = 70 * (0.9f + 0.1f * rnd.nextFloat()) * Math.min(1, f[2] / 4);
            if (f[0] + r < vx0 || f[0] - r > vx1 || f[1] + r < vy0 || f[1] - r > vy1) continue;
            glowAt(c, f[0], f[1], r, G_RED, 0.8f);
            glowAt(c, f[0], f[1], 16, G_WHITE, 1);
            if (rnd.nextFloat() < 0.5f) spark(f[0], f[1], rnd.nextFloat() * 60 - 30, rnd.nextFloat() * 60 - 30, 0.4f, 0xFFFF6A4A, true);
        }
    }

    private void glowAt(Canvas c, float x, float y, float r, int kind, float strength) {
        glowPaint.setAlpha((int) (Math.max(0, Math.min(1, strength)) * 255 * 0.85f));
        dst.set(x - r, y - r, x + r, y + r);
        c.drawBitmap(glow[kind], src, dst, glowPaint);
    }

    /** How much is going on, for the tests. */
    int count() {
        return lights.size() + waves.size() + craters.size() + cracks.size() + smoke.size() + sparks.size() + debris.size() + ripples.size();
    }
}
