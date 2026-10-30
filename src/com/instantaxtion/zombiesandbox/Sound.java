package com.instantaxtion.zombiesandbox;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.io.File;
import java.io.FileInputStream;

/**
 * Plays the sound effects and the music. ((13.4) Remade.) The effects go through the game's own {@link Mixer}, out
 * to the phone a few milliseconds at a time on an audio thread of their own (at the phone's own sample rate, in its
 * low-latency mode where it has one); the music loops on a MediaPlayer, crossfading from one piece to the next and
 * ducking under a firefight. All of it is made by {@link Synth} on a background thread the first time the app runs,
 * and kept as WAV files.
 */
final class Sound {
    /** Bump when the synthesized audio changes so old cached files are regenerated. */
    private static final int AUDIO_VERSION = 4;
    /** (10.52) The music on its own: ten pieces now. */
    private static final int MUSIC_VERSION = 3;
    /** Frames mixed at a time: about 5 ms. */
    private static final int BLOCK = 256;

    private final Context ctx;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Sfx.Fatigue fatigue = new Sfx.Fatigue();
    private final String[] trackPaths = new String[Synth.TRACKS];
    private final AudioAttributes musicAttrs;

    private Mixer mixer;
    private AudioTrack track;
    private Thread audio;
    private final Object lock = new Object();

    private MediaPlayer player, fading;
    private int playingTrack = -1, wantTrack = -1;
    private float musicVol = 0.5f, sfxVol = 0.75f, fadeIn = 1, fadeOut = 0, duck = 1;
    private volatile boolean paused, released;

    Sound(Context context) {
        ctx = context.getApplicationContext();
        musicAttrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        startOutput();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                prepareAudio();
            }
        }, "synth");
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
        main.postDelayed(musicTick, 50);
    }

    // ------------------------------------------------------------------ the effects: the mixer and its output

    /** The audio output, and the thread that keeps it fed. With no output to be had, the game goes on silent. */
    private void startOutput() {
        try {
            int rate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);
            if (rate < 8000 || rate > 192000) rate = 48000;
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            AudioFormat format = new AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build();
            int min = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT);
            int size = Math.max(min * 2, BLOCK * 8 * 4);
            AudioTrack t;
            if (Build.VERSION.SDK_INT >= 23) {
                AudioTrack.Builder b = new AudioTrack.Builder()
                        .setAudioAttributes(attrs)
                        .setAudioFormat(format)
                        .setBufferSizeInBytes(size)
                        .setTransferMode(AudioTrack.MODE_STREAM);
                // (Low-latency mode from Android 8, asked for by name: the game is built against an older SDK.)
                if (Build.VERSION.SDK_INT >= 26) {
                    try {
                        AudioTrack.Builder.class.getMethod("setPerformanceMode", int.class).invoke(b, 1);
                    } catch (Exception ignored) {
                    }
                }
                t = b.build();
            } else {
                t = new AudioTrack(attrs, format, size, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
            }
            if (t.getState() != AudioTrack.STATE_INITIALIZED) {
                t.release();
                return;
            }
            track = t;
            mixer = new Mixer(rate);
            mixer.setMaster(sfxVol);
            track.play();
            audio = new Thread(new Runnable() {
                @Override
                public void run() {
                    pump();
                }
            }, "audio");
            audio.start();
        } catch (Exception e) {
            track = null;
            mixer = null;
        }
    }

    /** The audio thread: mixes a block, hands it over (waiting while the output has enough), again. */
    private void pump() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        float[] buf = new float[BLOCK * 2];
        while (!released) {
            synchronized (lock) {
                while (paused && !released) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
            if (released) return;
            try {
                mixer.render(buf, BLOCK);
                if (track.write(buf, 0, buf.length, AudioTrack.WRITE_BLOCKING) < 0) return;
            } catch (Exception e) {
                return;
            }
        }
    }

    /** Plays an effect. {@code pan} runs from -1 (left) to 1 (right). */
    void play(int id, float volume, float pan) {
        play(id, volume, pan, 0);
    }

    /** ((13.4) And how far off it is: 0 right here, 1 at the edge of hearing (quieter is the caller's doing). */
    void play(int id, float volume, float pan, float distance) {
        if (released || paused || mixer == null || sfxVol <= 0 || !mixer.ready(id)) return;
        float g = fatigue.gain(id, SystemClock.uptimeMillis());
        if (g <= 0) return;
        mixer.trigger(id, Math.max(0, volume) * g, pan, distance, 1);
    }

    // ------------------------------------------------------------------ making the sounds

    private void prepareAudio() {
        File dir = ctx.getCacheDir();
        // (Sounds from before are cleared out, and anything half written.)
        File[] old = dir.listFiles();
        if (old != null)
            for (File o : old) {
                String n = o.getName();
                if ((n.startsWith("music") && !n.startsWith("music" + MUSIC_VERSION + "_")) || (n.startsWith("sfx") && !n.startsWith("sfx" + AUDIO_VERSION + "_"))
                        || n.endsWith(".part") || n.endsWith(".tmp")) o.delete();
            }
        // The effects first, the commonest first: the guns, the dead, the radio.
        for (int i = 0; i < Sfx.COUNT && !released; i++) {
            if (mixer == null) break;
            short[][] takes = new short[Sfx.VARIANTS[i]][];
            int got = 0;
            for (int v = 0; v < takes.length; v++) {
                short[] s = loadOrMake(sfxFile(i, v), i, v);
                if (s != null) takes[got++] = s;
            }
            if (got > 0) mixer.setSamples(i, got == takes.length ? takes : java.util.Arrays.copyOf(takes, got));
        }
        // Menu music first: it is what the player hears first.
        for (int track = 0; track < Synth.TRACKS && !released; track++) {
            final int tr = track;
            final File f = musicFile(track);
            try {
                if (!f.exists()) writeSafely(f, Synth.music(track));
            } catch (Exception e) {
                continue;
            }
            main.post(new Runnable() {
                @Override
                public void run() {
                    trackPaths[tr] = f.getPath();
                    updateMusic();
                }
            });
        }
    }

    /** A take of a sound from its file, made (and kept) first if it isn't there or won't read. */
    private static short[] loadOrMake(File f, int id, int variant) {
        if (f.exists()) {
            short[] s = readWav(f);
            if (s != null) return s;
            f.delete();
        }
        try {
            short[] s = Synth.effect(id, variant);
            writeSafely(f, s);
            return s;
        } catch (Exception e) {
            return null;
        }
    }

    /** The samples of one of our own WAV files (16-bit mono after a 44-byte header), or null if it's no good. */
    private static short[] readWav(File f) {
        long len = f.length();
        if (len <= 44 || len > 8L * 1024 * 1024) return null;
        byte[] b = new byte[(int) len];
        try {
            FileInputStream in = new FileInputStream(f);
            try {
                int at = 0;
                while (at < b.length) {
                    int n = in.read(b, at, b.length - at);
                    if (n < 0) return null;
                    at += n;
                }
            } finally {
                in.close();
            }
        } catch (Exception e) {
            return null;
        }
        if (b[0] != 'R' || b[1] != 'I' || b[8] != 'W') return null;
        short[] s = new short[(b.length - 44) / 2];
        for (int i = 0; i < s.length; i++) s[i] = (short) ((b[44 + i * 2] & 0xFF) | (b[45 + i * 2] << 8));
        return s;
    }

    private File sfxFile(int i, int variant) {
        return new File(ctx.getCacheDir(), "sfx" + AUDIO_VERSION + "_" + i + "_" + variant + ".wav");
    }

    private File musicFile(int track) {
        return new File(ctx.getCacheDir(), "music" + MUSIC_VERSION + "_" + track + ".wav");
    }

    /** ((12.0) Music already being made again, so a bad file isn't redone twice at once.) */
    private final boolean[] rebuildingMusic = new boolean[Synth.TRACKS];

    /** (12.0) A piece of music that won't play is thrown away and made again, once. */
    private void rebuildMusic(final int index) {
        if (released || rebuildingMusic[index]) return;
        rebuildingMusic[index] = true;
        final File f = musicFile(index);
        f.delete();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    writeSafely(f, Synth.music(index));
                } catch (Exception e) {
                    return;
                }
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        if (released) return;
                        trackPaths[index] = f.getPath();
                        updateMusic();
                    }
                });
            }
        }, "synth");
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    /** (12.0) Written under another name first, then renamed, so a half-written file is never kept. */
    private static void writeSafely(File f, short[] samples) throws Exception {
        File tmp = new File(f.getPath() + ".part");
        Synth.writeWav(tmp, samples);
        if (!tmp.renameTo(f)) {
            tmp.delete();
            throw new java.io.IOException("Could not keep " + f);
        }
    }

    // ------------------------------------------------------------------ the music

    void setVolumes(float music, float sfx) {
        musicVol = music;
        sfxVol = sfx;
        if (mixer != null) mixer.setMaster(sfx);
        updateMusic();
    }

    /** Selects the music track to loop (Synth.TRACK_MENU or Synth.TRACK_GAME). */
    void playTrack(int track) {
        if (track == wantTrack) return;
        wantTrack = track;
        updateMusic();
    }

    private void updateMusic() {
        if (released) return;
        int target = musicVol > 0 && wantTrack >= 0 && trackPaths[wantTrack] != null ? wantTrack : -1;
        if (target != playingTrack) {
            // ((13.4) The piece playing fades out over a second and a half while the next comes in.)
            if (fading != null) release(fading);
            fading = player;
            fadeOut = 1;
            player = null;
            playingTrack = -1;
            if (target >= 0) {
                try {
                    MediaPlayer mp = new MediaPlayer();
                    mp.setAudioAttributes(musicAttrs);
                    mp.setDataSource(trackPaths[target]);
                    mp.setLooping(true);
                    mp.prepare();
                    player = mp;
                    playingTrack = target;
                    fadeIn = fading != null ? 0 : 1;
                } catch (Exception e) {
                    // ((12.0) It won't play: made again, and played when it's ready.)
                    trackPaths[target] = null;
                    rebuildMusic(target);
                }
            }
        }
        applyMusic();
    }

    /** Sets the players' volumes for the fades and the ducking, and starts or pauses them. */
    private void applyMusic() {
        if (player != null) {
            float v = musicVol * fadeIn * duck;
            player.setVolume(v, v);
            if (paused) {
                if (player.isPlaying()) player.pause();
            } else if (!player.isPlaying()) player.start();
        }
        if (fading != null) {
            float v = musicVol * fadeOut * duck;
            fading.setVolume(v, v);
            if (paused && fading.isPlaying()) fading.pause();
            if (fadeOut <= 0) {
                release(fading);
                fading = null;
            }
        }
    }

    /** Twenty times a second: the crossfade moves on, and the music dips while the fighting is loud. */
    private final Runnable musicTick = new Runnable() {
        @Override
        public void run() {
            if (released) return;
            fadeIn = Math.min(1, fadeIn + 0.05f / 1.5f);
            fadeOut = Math.max(0, fadeOut - 0.05f / 1.5f);
            float loud = mixer != null ? mixer.loudness() : 0;
            float want = 1 - 0.45f * Math.min(1, loud / 0.6f);
            duck += (want - duck) * (want < duck ? 0.5f : 0.06f);
            applyMusic();
            main.postDelayed(this, 50);
        }
    };

    private static void release(MediaPlayer mp) {
        try {
            mp.release();
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------------ the app going and coming back

    void pause() {
        paused = true;
        if (track != null) {
            try {
                track.pause();
                track.flush();
            } catch (Exception ignored) {
            }
        }
        applyMusic();
    }

    void resume() {
        if (track != null) {
            try {
                track.play();
            } catch (Exception ignored) {
            }
        }
        synchronized (lock) {
            paused = false;
            lock.notifyAll();
        }
        applyMusic();
    }

    void release() {
        released = true;
        synchronized (lock) {
            lock.notifyAll();
        }
        main.removeCallbacks(musicTick);
        if (player != null) release(player);
        if (fading != null) release(fading);
        player = fading = null;
        playingTrack = -1;
        if (audio != null) {
            try {
                audio.join(200);
            } catch (InterruptedException ignored) {
            }
        }
        if (track != null) {
            try {
                track.release();
            } catch (Exception ignored) {
            }
        }
    }
}
