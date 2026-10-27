package com.instantaxtion.zombiesandbox;

import android.content.Context;
import android.content.SharedPreferences;

/** Best results across every game played, saved on the phone. ((11.5) No achievements any more.) */
final class Records {
    static final String[] RECORD_NAMES = {"Longest outbreak survived", "Most zombies killed in a game",
            "Biggest horde", "Most volunteers in a game", "Zombies killed, all games", "Games played",
            // ((12.0) Two more.)
            "Most kills by one person", "Most survivors, city saved"};

    private final SharedPreferences prefs;

    Records(Context context) {
        prefs = context.getSharedPreferences("records", Context.MODE_PRIVATE);
    }

    int record(int i) {
        return prefs.getInt("rec" + i, 0);
    }

    String recordText(int i) {
        int v = record(i);
        if (i == 0) return World.clock(v);
        return String.valueOf(v);
    }

    void best(int i, int value) {
        if (value > record(i)) prefs.edit().putInt("rec" + i, value).apply();
    }

    void add(int i, int amount) {
        if (amount != 0) prefs.edit().putInt("rec" + i, record(i) + amount).apply();
    }

    void gameStarted() {
        add(5, 1);
    }

    private int lastKilled;
    private World lastWorld;

    /** Checks the current game against the records. */
    void check(World w) {
        if (w != lastWorld) {
            lastWorld = w;
            lastKilled = w.zombiesKilled;
        }
        add(4, w.zombiesKilled - lastKilled);
        lastKilled = w.zombiesKilled;
        if (w.humans > 0) best(0, (int) w.outbreakTime);
        best(1, w.zombiesKilled);
        best(2, w.biggestHorde);
        best(3, w.recruits);
        best(6, w.bestKills());
        if (w.warResult == 1) best(7, w.humans);
    }
}
