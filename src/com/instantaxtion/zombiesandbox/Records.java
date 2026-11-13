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
        // (19.6) Only the zombie missions are left: Dead City moved up from the seventh to the third.
        if (prefs.getInt("zombiesOnly", 0) == 0) {
            SharedPreferences.Editor ed = prefs.edit();
            int dead = prefs.getInt("mission6", 0);
            for (int i = 2; i <= 6; i++) ed.putInt("mission" + i, 0);
            if (dead > 0) ed.putInt("mission2", dead);
            ed.putInt("zombiesOnly", 1).apply();
        }
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

    // ---- ((18.0) The campaign: the best stars for each mission, and the veterans carried from one to the next.) ----

    int missionStars(int id) {
        return prefs.getInt("mission" + id, 0);
    }

    /** A mission's open to play once the one before it has been won. */
    boolean unlocked(int id) {
        return id == 0 || missionStars(id - 1) > 0;
    }

    String veterans() {
        return prefs.getString("veterans", "");
    }

    /** A mission won: its best stars kept, and the veterans who came through it are the roster now. */
    void missionWon(int id, int stars, String roster) {
        SharedPreferences.Editor ed = prefs.edit();
        if (stars > missionStars(id)) ed.putInt("mission" + id, stars);
        ed.putString("veterans", roster == null ? "" : roster);
        ed.apply();
    }

    int totalStars() {
        int n = 0;
        for (int i = 0; i < Campaign.MISSIONS.length; i++) n += missionStars(i);
        return n;
    }

    private int lastKilled;
    private World lastWorld;

    /** ((17.3) A new city's on the way: the last game's let go of. */
    void forget() {
        lastWorld = null;
    }

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
        checkHall(w);
    }

    // ------------------------------------------------------------------ (15.1) the unit hall of fame

    /** (15.1) How many make the hall of fame, and the least it takes (kills, and three for each life saved). */
    static final int HALL_SIZE = 10, HALL_MIN = 8;

    /** (15.1) One of the best police officers or soldiers ever. */
    static final class Hero {
        long game;
        int seed, type, role, kills, saved;
        String city;

        int score() {
            return kills + saved * 3;
        }

        String line() {
            return game + "|" + seed + "|" + type + "|" + role + "|" + kills + "|" + saved + "|" + city.replace('|', ' ').replace('\n', ' ');
        }
    }

    /** (15.1) The hall of fame, best first. */
    java.util.List<Hero> hall() {
        java.util.ArrayList<Hero> out = new java.util.ArrayList<Hero>();
        String s = prefs.getString("hall", "");
        if (s == null || s.isEmpty()) return out;
        for (String l : s.split("\n")) {
            String[] f = l.split("\\|", 7);
            if (f.length < 7) continue;
            try {
                Hero h = new Hero();
                h.game = Long.parseLong(f[0]);
                h.seed = Integer.parseInt(f[1]);
                h.type = Integer.parseInt(f[2]);
                h.role = Integer.parseInt(f[3]);
                h.kills = Integer.parseInt(f[4]);
                h.saved = Integer.parseInt(f[5]);
                h.city = f[6];
                out.add(h);
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    private String hallText;

    /**
     * (15.1) The police officers and soldiers in this game who've done enough to be among the best ever (by kills
     * and lives saved) go into the hall of fame, or move up it.
     */
    private void checkHall(World w) {
        java.util.List<Hero> list = hall();
        int least = list.size() >= HALL_SIZE ? list.get(list.size() - 1).score() : HALL_MIN;
        for (int i = 0, n = w.entities.size(); i < n; i++) {
            Entity e = w.entities.get(i);
            if ((e.type != Entity.COP && e.type != Entity.SOLDIER) || e.kills + e.saved * 3 < Math.max(HALL_MIN, least)) continue;
            Hero h = null;
            for (Hero o : list) if (o.game == w.gameId && o.seed == e.nameSeed && o.type == e.type) h = o;
            if (h == null) {
                h = new Hero();
                h.game = w.gameId;
                h.seed = e.nameSeed;
                h.type = e.type;
                h.city = w.city.name;
                list.add(h);
            }
            h.role = e.role;
            h.kills = Math.max(h.kills, e.kills);
            h.saved = Math.max(h.saved, e.saved);
        }
        java.util.Collections.sort(list, new java.util.Comparator<Hero>() {
            @Override
            public int compare(Hero a, Hero b) {
                return b.score() - a.score();
            }
        });
        while (list.size() > HALL_SIZE) list.remove(list.size() - 1);
        StringBuilder sb = new StringBuilder();
        for (Hero h : list) sb.append(sb.length() > 0 ? "\n" : "").append(h.line());
        String text = sb.toString();
        if (!text.equals(hallText == null ? prefs.getString("hall", "") : hallText)) prefs.edit().putString("hall", text).apply();
        hallText = text;
    }
}
