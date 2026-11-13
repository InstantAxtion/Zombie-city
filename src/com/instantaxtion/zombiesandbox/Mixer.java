package com.instantaxtion.zombiesandbox;

import java.util.Random;

/**
 * (13.4) The game's own sound mixer, in plain Java: {@link Sound} feeds what it makes to the phone's audio output a
 * few milliseconds at a time. In place of the system's sound pool (a dozen streams, each sample played as it was,
 * every voice equally loud however far off, the oldest cut dead when it ran out):
 * <ul>
 * <li>a pool of voices, with each sound's importance deciding which gives way when they're all in use, a limit on how
 * many of one sound play at once, and anything cut short faded out rather than clicking off;</li>
 * <li>several takes of each sound, picked at random, and played a touch higher or lower each time;</li>
 * <li>equal-power stereo, and the further off a sound is the more muffled (the high end goes first, as in life);</li>
 * <li>an echo off the buildings, more of it for gunfire and explosions, and more for sounds further off;</li>
 * <li>a limiter on the whole mix, so a big firefight is loud but never breaks up.</li>
 * </ul>
 * Sounds are asked for from the game's thread ({@link #trigger}) and mixed on the audio thread ({@link #render}).
 */
final class Mixer {
    static final int VOICES = 32;
    /** Voices that were cut short, fading out over a few milliseconds. */
    private static final int TAILS = 8;

    final int rate;
    private final float step0;
    private final Random rnd = new Random();
    /** The takes of each sound: [id][take], 16-bit mono at Synth.RATE. Set from a loading thread. */
    private final short[][][] samples = new short[Sfx.COUNT][][];

    private static final class Voice {
        boolean on, tail;
        int id, prio;
        short[] data;
        double pos;
        float step;
        float gl, gr, cl, cr;
        float lp, lpK, echo;
        long started;
    }

    private final Voice[] voices = new Voice[VOICES + TAILS];
    private long triggers;

    // Requests from the game: {id, gain, pan, distance, pitch} each, in a ring.
    private static final int QUEUE = 128;
    private final float[] queue = new float[QUEUE * 5];
    private int qHead, qTail;

    // The echo: four comb filters a side into two all-pass filters (a small Schroeder reverberator).
    private final float[][] combL = new float[4][], combR = new float[4][];
    private final int[] combAtL = new int[4], combAtR = new int[4];
    private final float[] combLpL = new float[4], combLpR = new float[4];
    private final float[][] apL = new float[2][], apR = new float[2][];
    private final int[] apAtL = new int[2], apAtR = new int[2];

    private float master = 1, env, loud;

    // ---- (19.0) The Sound Update ----

    /** Each group's level (the guns, blasts, voices, vehicles, the world, the radio, the menus). */
    private final float[] bus = {1, 1, 1, 1, 1, 1, 1};
    /** Everything in the middle: for one ear, or one speaker. */
    private volatile boolean mono;
    /** Night mode: the loud brought down and the quiet brought up, for listening low. */
    private volatile boolean night;
    /** How deafened the listener is by a blast close by, 0 to 1: everything muffled, easing back over a few seconds. */
    private volatile float shock;
    private float shockLp0, shockLp1;

    /** The ambience beds: each a sample looping for ever, at a level that glides to where it's asked to be. */
    private static final class Loop {
        short[] data;
        double pos;
        float gain, target, pan, panTarget, muffle, lp;
    }

    private final Loop[] loops = new Loop[Sfx.LOOPS];
    private float ambience = 1;

    void setLoopSamples(int loop, short[] data) {
        Loop l = new Loop();
        l.data = data;
        l.pos = loop * 7919 % Math.max(1, data.length);
        loops[loop] = l;
    }

    boolean loopReady(int loop) {
        return loops[loop] != null;
    }

    /** How loud an ambience bed should be (0 off), where (-1 left to 1 right), and how muffled (0 to 1). */
    void setLoop(int loop, float gain, float pan, float muffle) {
        Loop l = loops[loop];
        if (l == null) return;
        l.target = Math.max(0, Math.min(1, gain));
        l.panTarget = Math.max(-1, Math.min(1, pan));
        l.muffle = Math.max(0, Math.min(1, muffle));
    }

    float loopGain(int loop) {
        return loops[loop] == null ? 0 : loops[loop].gain;
    }

    void setBus(int b, float v) {
        bus[b] = Math.max(0, Math.min(1.5f, v));
    }

    void setAmbience(float v) {
        ambience = Math.max(0, Math.min(1.5f, v));
    }

    void setMono(boolean m) {
        mono = m;
    }

    void setNight(boolean n) {
        night = n;
    }

    /** A blast close by: the ears ring and everything goes dull for a while (0 to 1, the most kept). */
    void shock(float amount) {
        shock = Math.max(shock, Math.max(0, Math.min(1, amount)));
    }

    float shockLevel() {
        return shock;
    }

    Mixer(int outRate) {
        rate = outRate;
        step0 = Synth.RATE / (float) outRate;
        for (int i = 0; i < voices.length; i++) voices[i] = new Voice();
        int[] comb = {1116, 1188, 1277, 1356}, ap = {556, 441};
        float k = outRate / 44100f;
        for (int i = 0; i < 4; i++) {
            combL[i] = new float[(int) (comb[i] * k)];
            combR[i] = new float[(int) ((comb[i] + 23) * k)];
        }
        for (int i = 0; i < 2; i++) {
            apL[i] = new float[(int) (ap[i] * k)];
            apR[i] = new float[(int) ((ap[i] + 23) * k)];
        }
    }

    /** The takes of a sound, once they're made. */
    void setSamples(int id, short[][] takes) {
        samples[id] = takes;
    }

    boolean ready(int id) {
        return samples[id] != null;
    }

    void setMaster(float v) {
        master = Math.max(0, Math.min(1, v));
    }

    /** How loud the mix has been lately, 0 to about 1: for the music to duck under a firefight. */
    float loudness() {
        return loud;
    }

    /**
     * Asks for a sound: {@code gain} 0 to 1, {@code pan} -1 (left) to 1 (right), {@code distance} 0 (right here) to
     * 1 (as far as it carries), {@code pitch} about 1. From any thread; it starts with the next block mixed.
     */
    synchronized void trigger(int id, float gain, float pan, float distance, float pitch) {
        int next = (qTail + 1) % QUEUE;
        if (next == qHead) return;
        int q = qTail * 5;
        queue[q] = id;
        queue[q + 1] = gain;
        queue[q + 2] = pan;
        queue[q + 3] = distance;
        queue[q + 4] = pitch;
        qTail = next;
    }

    private void drain() {
        while (true) {
            int id;
            float gain, pan, dist, pitch;
            synchronized (this) {
                if (qHead == qTail) return;
                int q = qHead * 5;
                id = (int) queue[q];
                gain = queue[q + 1];
                pan = queue[q + 2];
                dist = queue[q + 3];
                pitch = queue[q + 4];
                qHead = (qHead + 1) % QUEUE;
            }
            start(id, gain, pan, dist, pitch);
        }
    }

    private void start(int id, float gain, float pan, float dist, float pitch) {
        short[][] takes = samples[id];
        if (takes == null || takes.length == 0 || gain <= 0.001f) return;
        gain *= bus[Sfx.BUS[id]];
        if (gain <= 0.001f) return;
        if (mono) pan = 0;
        int prio = Sfx.PRIORITY[id];
        // Too many of this one already: the oldest of them makes way.
        int same = 0;
        Voice oldest = null;
        for (int i = 0; i < VOICES; i++) {
            Voice v = voices[i];
            if (!v.on || v.id != id) continue;
            same++;
            if (oldest == null || v.started < oldest.started) oldest = v;
        }
        Voice slot = null;
        if (same >= Sfx.MAX_VOICES[id]) slot = oldest;
        if (slot == null)
            for (int i = 0; i < VOICES && slot == null; i++) if (!voices[i].on) slot = voices[i];
        if (slot == null) {
            // Every voice busy: the least of them, if it matters less than this one.
            float worst = Float.MAX_VALUE;
            for (int i = 0; i < VOICES; i++) {
                Voice v = voices[i];
                float score = v.prio * 10 + (Math.abs(v.gl) + Math.abs(v.gr)) * 5 - (triggers - v.started) * 0.01f;
                if (score < worst) {
                    worst = score;
                    slot = voices[i];
                }
            }
            if (slot == null || worst > prio * 10 + gain * 10) return;
        }
        if (slot.on) fadeOut(slot);
        short[] data = takes[rnd.nextInt(takes.length)];
        float a = (Math.max(-1, Math.min(1, pan)) + 1) * (float) (Math.PI / 4);
        float g = Math.min(1.5f, gain);
        dist = Math.max(0, Math.min(1, dist));
        slot.on = true;
        slot.tail = false;
        slot.id = id;
        slot.prio = prio;
        slot.data = data;
        slot.pos = 0;
        // (A shade higher or lower each time, more for the dead and the crowds than for the guns.)
        float spread = Sfx.gun(id) ? 0.05f : Sfx.REPEATS[id] ? 0.12f : 0.08f;
        if (id == Sfx.CLICK) spread = 0;
        slot.step = step0 * pitch * (1 + (rnd.nextFloat() - 0.5f) * spread);
        slot.gl = slot.cl = g * (float) Math.cos(a);
        slot.gr = slot.cr = g * (float) Math.sin(a);
        // Far off, the high end goes: a one-pole low-pass from wide open close by to muffled at the edge of hearing.
        slot.lpK = Math.max(0.08f, 1 - 0.92f * dist);
        slot.lp = 0;
        slot.echo = Sfx.ECHO[id] * (0.6f + 0.8f * dist);
        slot.started = ++triggers;
    }

    /** Moves a voice that's being cut short to a tail slot, to fade out over a few milliseconds. */
    private void fadeOut(Voice v) {
        for (int i = VOICES; i < voices.length; i++) {
            Voice t = voices[i];
            if (t.on) continue;
            t.on = true;
            t.tail = true;
            t.id = v.id;
            t.data = v.data;
            t.pos = v.pos;
            t.step = v.step;
            t.cl = v.cl;
            t.cr = v.cr;
            t.gl = 0;
            t.gr = 0;
            t.lp = v.lp;
            t.lpK = v.lpK;
            t.echo = v.echo;
            return;
        }
    }

    private final float[] wet = new float[2];

    /** Mixes the next {@code frames} frames into {@code out}, interleaved left and right, -1 to 1. */
    void render(float[] out, int frames) {
        drain();
        java.util.Arrays.fill(out, 0, frames * 2, 0);
        float[] send = sendBuf(frames);
        java.util.Arrays.fill(send, 0, frames, 0);
        for (Voice v : voices) {
            if (!v.on) continue;
            short[] d = v.data;
            int n = d.length;
            double pos = v.pos;
            float step = v.step, lp = v.lp, k = v.lpK, cl = v.cl, cr = v.cr, gl = v.gl, gr = v.gr, echo = v.echo;
            // (Gains glide: 5 ms to reach a new value, so a voice faded out doesn't click.)
            float glide = v.tail ? 1 - (float) Math.exp(-1.0 / (0.005 * rate)) : 0;
            for (int f = 0; f < frames; f++) {
                int i = (int) pos;
                if (i + 1 >= n || (v.tail && Math.abs(cl) + Math.abs(cr) < 1e-4f)) {
                    v.on = false;
                    break;
                }
                float fr = (float) (pos - i);
                float x = (d[i] + (d[i + 1] - d[i]) * fr) * (1f / 32768f);
                lp += (x - lp) * k;
                if (glide > 0) {
                    cl += (gl - cl) * glide;
                    cr += (gr - cr) * glide;
                }
                out[f * 2] += lp * cl;
                out[f * 2 + 1] += lp * cr;
                send[f] += lp * (cl + cr) * 0.5f * echo;
                pos += step;
            }
            v.pos = pos;
            v.lp = lp;
            v.cl = cl;
            v.cr = cr;
        }
        mixLoops(out, frames);
        float relEnv = (float) Math.exp(-1.0 / (0.15 * rate)), atkEnv = (float) Math.exp(-1.0 / (0.001 * rate));
        // ((19.0) Deafened by a blast: a low-pass over the whole mix that opens back up over about three seconds.)
        float sh = shock, shK = 1 - 0.93f * sh;
        shock = Math.max(0, sh - frames / (3f * rate));
        // (Night mode: a gentler, earlier limiter, and the quiet lifted.)
        float lim = night ? 0.35f : 0.7f, makeup = night ? 1.6f : 1f;
        float relLoud = (float) Math.exp(-1.0 / (0.8 * rate));
        for (int f = 0; f < frames; f++) {
            reverb(send[f]);
            float l = (out[f * 2] + wet[0] * 0.32f) * master * makeup, r = (out[f * 2 + 1] + wet[1] * 0.32f) * master * makeup;
            if (sh > 0.01f) {
                shockLp0 += (l - shockLp0) * shK;
                shockLp1 += (r - shockLp1) * shK;
                l = shockLp0 * (1 - 0.4f * sh);
                r = shockLp1 * (1 - 0.4f * sh);
            }
            if (mono) l = r = (l + r) * 0.5f;
            // The limiter: above 0.7 the whole mix comes down, quickly, and goes back up over a moment.
            float peak = Math.max(Math.abs(l), Math.abs(r));
            env = peak > env ? peak + (env - peak) * atkEnv : env * relEnv;
            float g = env > lim ? lim / env : 1;
            l *= g;
            r *= g;
            loud = Math.max(peak, loud * relLoud);
            // (And anything still over the top rounded off, not clipped.)
            out[f * 2] = soft(l);
            out[f * 2 + 1] = soft(r);
        }
    }

    /** ((19.0) The ambience beds, under everything (and under the echo: they're everywhere already). */
    private void mixLoops(float[] out, int frames) {
        float glide = 1 - (float) Math.exp(-frames / (0.6 * rate));
        for (Loop l : loops) {
            if (l == null) continue;
            float tg = l.target * ambience * master;
            l.gain += (tg - l.gain) * glide;
            l.pan += (l.panTarget - l.pan) * glide;
            if (l.gain < 0.0005f && tg == 0) continue;
            float a = ((mono ? 0 : l.pan) + 1) * (float) (Math.PI / 4);
            float gl = l.gain * (float) Math.cos(a), gr = l.gain * (float) Math.sin(a);
            float k = Math.max(0.05f, 1 - 0.95f * l.muffle);
            short[] d = l.data;
            int n = d.length;
            double pos = l.pos;
            float lp = l.lp;
            for (int f = 0; f < frames; f++) {
                int i = (int) pos;
                int j = i + 1 >= n ? 0 : i + 1;
                float fr = (float) (pos - i);
                float x = (d[i] + (d[j] - d[i]) * fr) * (1f / 32768f);
                lp += (x - lp) * k;
                out[f * 2] += lp * gl;
                out[f * 2 + 1] += lp * gr;
                pos += step0;
                if (pos >= n) pos -= n;
            }
            l.pos = pos;
            l.lp = lp;
        }
    }

    private static float soft(float x) {
        if (x > 0.9f) return 0.9f + 0.1f * (float) Math.tanh((x - 0.9f) * 10);
        if (x < -0.9f) return -0.9f + 0.1f * (float) Math.tanh((x + 0.9f) * 10);
        return x;
    }

    private float[] sendBuf = new float[0];

    private float[] sendBuf(int frames) {
        if (sendBuf.length < frames) sendBuf = new float[frames];
        return sendBuf;
    }

    /** One frame of the echo: in, the reverberated left and right into {@link #wet}. */
    private void reverb(float in) {
        float l = 0, r = 0;
        for (int c = 0; c < 4; c++) {
            l += comb(combL[c], combAtL, combLpL, c, in);
            r += comb(combR[c], combAtR, combLpR, c, in);
        }
        for (int a = 0; a < 2; a++) {
            l = allpass(apL[a], apAtL, a, l);
            r = allpass(apR[a], apAtR, a, r);
        }
        wet[0] = l * 0.25f;
        wet[1] = r * 0.25f;
    }

    private static float comb(float[] buf, int[] at, float[] damp, int c, float in) {
        int i = at[c];
        float y = buf[i];
        damp[c] = y * 0.7f + damp[c] * 0.3f;
        buf[i] = in + damp[c] * 0.8f;
        at[c] = i + 1 == buf.length ? 0 : i + 1;
        return y;
    }

    private static float allpass(float[] buf, int[] at, int a, float in) {
        int i = at[a];
        float b = buf[i];
        float y = b - 0.5f * in;
        buf[i] = in + 0.5f * y;
        at[a] = i + 1 == buf.length ? 0 : i + 1;
        return y;
    }

    /** Voices sounding now (not counting those fading out): for tests. */
    int playing() {
        int n = 0;
        for (int i = 0; i < VOICES; i++) if (voices[i].on) n++;
        return n;
    }
}
