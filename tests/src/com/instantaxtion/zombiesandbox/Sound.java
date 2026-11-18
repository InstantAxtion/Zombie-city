package com.instantaxtion.zombiesandbox;

/** Silent stand-in for the real Sound (which needs Android's audio classes) so the game runs on a desktop JVM. */
final class Sound {
    Sound(android.content.Context c) {
    }

    void play(int id, float volume, float pan) {
        lastPlayed = id;
    }

    void play(int id, float volume, float pan, float distance) {
        lastPlayed = id;
    }

    void setVolumes(float music, float sfx) {
    }

    /** ((19.0) What the game asked for last, so the tests can see it.) */
    final float[] loopGain = new float[Sfx.LOOPS];
    float shocked;
    int lastPlayed = -1;

    void setLoop(int loop, float gain, float pan, float muffle) {
        loopGain[loop] = gain;
    }

    void setMix(float[] buses, float ambience, boolean mono, boolean night) {
    }

    void shock(float amount) {
        shocked = Math.max(shocked, amount);
    }

    void playTrack(int track) {
    }

    void setIntensity(float i) {
    }

    void pause() {
    }

    void resume() {
    }

    void release() {
    }
}
