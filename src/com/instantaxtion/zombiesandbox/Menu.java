package com.instantaxtion.zombiesandbox;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Random;

/** Main menu, pause menu, New Game setup, settings and patch notes, all drawn on the game canvas. */
final class Menu {
    interface Host {
        boolean hasGame();

        void continueGame();

        void startGame(CityConfig cfg);

        void toMainMenu();

        void quit();

        void settingsChanged();

        void click();

        /** True if there is a game in memory or a saved one to load. */
        boolean canContinue();

        /** Saves the current game; returns false if it failed. */
        boolean saveGame();

        World world();

        /** Saves a picture of the city and offers to share it. */
        void screenshot();

        /** Save slots 1-3 (0 is the autosave): save, load, and a short description (null if empty). */
        boolean saveSlot(int slot);

        boolean loadSlot(int slot);

        String slotInfo(int slot);

        /** Moves the camera to a spot. */
        void jumpTo(float x, float y);

        /** Hides the buttons and panels until the next tap. */
        void hideHud();

        /** Asks the player to type a city code (shows the current one to copy or edit). */
        void askCityCode(String current);

        /** (12.0) Puts text on the clipboard; false if it couldn't. */
        boolean copyText(String label, String text);
    }

    static final int NONE = 0, MAIN = 1, SETUP = 2, SETTINGS = 3, NOTES = 4, PAUSE = 5, STATS = 6, RECORDS = 8,
            SAVES = 9, LOG = 10, REPLAY = 11;
    /** The save slot screen: saving (from the pause menu) or loading (from the main menu). */
    private boolean savingMode;
    Records records;

    private static final int A_CONTINUE = 0, A_NEW = 1, A_SETTINGS = 2, A_NOTES = 3, A_QUIT = 4, A_RESUME = 5,
            A_MAIN_MENU = 6, A_BACK = 7, A_START = 8, A_RANDOM = 9, A_SAVE = 10, A_STATS = 11, A_HOW_TO = 12,
            A_TUT_NEXT = 13, A_TUT_PREV = 14, A_TUT_DONE = 15, A_CODE = 16, A_REROLL = 17, A_SHOT = 18, A_RECORDS = 19, A_SAVES = 20, A_LOAD = 21, A_LOG = 22, A_HUD = 23,
            A_REPLAY = 24, A_REPLAY_PLAY = 25, A_RESTART = 26, A_COPY_CODE = 27, A_SLOT = 100, A_LOAD_SLOT = 150, A_LOG_ROW = 200, A_LOG_FILTER = 1000;
    /** ((12.0) Which messages the radio log shows: 0 all, else one kind (Dispatch.WHO_* + 1).) */
    private int logFilter;

    // ((11.5) No How to Play pages any more.)

    private static final class Button {
        final RectF r = new RectF();
        String label;
        int action;
        boolean primary, invisible;
    }

    /** One tappable choice on the New Game screen: an option set to a value. */
    private static final class Seg {
        final RectF r = new RectF();
        int option, value;
    }

    private final ArrayList<Seg> segs = new ArrayList<Seg>();
    private final ArrayList<Seg> segPool = new ArrayList<Seg>();
    private final Paint bmpPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Rect bmpSrc = new Rect();
    /** A small picture of the city about to be played, built in the background whenever it changes. */
    private volatile Bitmap preview;
    private volatile String previewKey, buildingKey;
    /** How many people the previewed city's homes house. */
    private volatile int previewResidents;
    /** (10.56) Everyone the previewed city will start with: {people, police, soldiers} (World.forecast). */
    private volatile int[] previewForce;

    private static final class Row {
        final RectF r = new RectF(), prev = new RectF();
        int index;
    }

    int screen = MAIN;
    private int returnTo = MAIN;
    final CityConfig config = new CityConfig();

    private final Host host;
    private final Settings settings;
    private float dp;
    private final Random rnd = new Random();
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint plain = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final ArrayList<Button> buttons = new ArrayList<Button>();
    private final ArrayList<Button> pool = new ArrayList<Button>();
    private final ArrayList<Row> rows = new ArrayList<Row>();
    private final RectF listArea = new RectF();
    private final RectF tmp = new RectF();
    private float scroll, contentHeight;
    /** A short line shown above the New Game buttons (city code accepted or not). */
    private String note;
    private float noteTime;
    /** ((12.0) This frame's length: the note above the buttons lasted twice as long at 30 frames a second.) */
    private float frameDt = 1 / 60f;
    private float time;

    private boolean dragging, pressed;
    private float downX, downY, lastY;
    /** ((11.5) Scrolling carries on after the finger lifts, slowing to a stop: how fast, and when it last moved.) */
    private float flingV;
    private long lastMoveAt;

    /** ((11.5) The menus' look: a dark night palette with one toxic-green accent.) */
    static final int BG = 0xFF0C0F13, CARD = 0xFF161A20, CARD_HI = 0xFF1E242C, EDGE = 0x22FFFFFF, ACCENT = 0xFF7CE04A,
            ACCENT_INK = 0xFF0B1A06, INK = 0xFFF1F4F7, MUTED = 0xFF8C96A2;

    Menu(Host host, Settings settings, float dp) {
        this.host = host;
        this.settings = settings;
        this.dp = dp;
        stroke.setStyle(Paint.Style.STROKE);
        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        plain.setTypeface(Typeface.DEFAULT);
    }

    void setDp(float dp) {
        this.dp = dp;
    }

    boolean isOpen() {
        return screen != NONE;
    }

    /** True while the city behind the menu should keep simulating (not when the game is paused). */
    boolean liveBackground() {
        return screen == MAIN || (screen != NONE && screen != PAUSE && returnTo == MAIN);
    }

    void open(int s) {
        if (s == SETUP || s == SETTINGS || s == NOTES || s == STATS || s == RECORDS || s == SAVES || s == LOG) {
            if (screen == MAIN || screen == PAUSE || screen == NONE) returnTo = screen;
        } else {
            returnTo = MAIN;
        }
        screen = s;
        scroll = 0;
        if (s == NOTES) settings.markNotesRead();
        if (s == SETUP && !config.keepCity) {
            config.newSeed(rnd);
            config.dropRetired();
        }
    }

    /** A city code typed in by the player. */
    void cityCodeEntered(String text) {
        boolean ok = config.applyCode(text);
        note = ok ? "City " + config.code() + " (" + CityConfig.PRESETS[config.v[CityConfig.OPT_PRESET]]
                + "). Press Start to play it." : "That isn't a city code. Codes look like 12-483920.";
        noteTime = 4;
    }

    /** Handles the system back button. Returns false when the app should close. */
    boolean back() {
        switch (screen) {
            case MAIN:
                return false;
            case PAUSE:
                host.continueGame();
                return true;
            case NONE:
                open(PAUSE);
                return true;
            case REPLAY:
                screen = STATS;
                return true;
            default:
                screen = returnTo;
                scroll = 0;
                return true;
        }
    }

    // ------------------------------------------------------------------ drawing

    void draw(Canvas c, int w, int h, float dt) {
        time += dt;
        frameDt = dt;
        // ((11.5) Momentum: a flicked list keeps going, easing off.)
        float flung = -1;
        if (!dragging && Math.abs(flingV) > 4 * dp) {
            scroll += flingV * Math.min(dt, 0.05f);
            flingV *= (float) Math.exp(-3.2f * Math.min(dt, 0.05f));
            if (scroll < 0) {
                scroll = 0;
                flingV = 0;
            }
            flung = scroll;
        } else if (!dragging) flingV = 0;
        for (Button b : buttons) pool.add(b);
        buttons.clear();
        segPool.addAll(segs);
        segs.clear();
        rows.clear();
        switch (screen) {
            case MAIN: drawMain(c, w, h); break;
            case PAUSE: drawPause(c, w, h); break;
            case SETUP: drawSetup(c, w, h); break;
            case SETTINGS: drawOptions(c, w, h, "Settings", settings, false); break;
            case NOTES: drawNotes(c, w, h); break;
            case STATS: drawStats(c, w, h); break;
            case RECORDS: drawRecords(c, w, h); break;
            case SAVES: drawSaves(c, w, h); break;
            case LOG: drawLog(c, w, h); break;
            case REPLAY: drawReplay(c, w, h, dt); break;
        }
        for (Button b : buttons) drawButton(c, b);
        // (Run into the end of the list: stop there.)
        if (flung >= 0 && scroll != flung) flingV = 0;
    }

    /** ((12.0) s cut to fit width in paint p, with "..." where it was cut.) */
    private static String fit(Paint p, String s, float width) {
        if (p.measureText(s) <= width) return s;
        while (s.length() > 4 && p.measureText(s + "...") > width) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    private Button button(String label, int action, boolean primary, float l, float t, float r, float b) {
        Button btn = pool.isEmpty() ? new Button() : pool.remove(pool.size() - 1);
        btn.label = label;
        btn.action = action;
        btn.primary = primary;
        btn.invisible = false;
        btn.r.set(l, t, r, b);
        buttons.add(btn);
        return btn;
    }

    private void drawButton(Canvas c, Button b) {
        if (b.invisible) return;
        // ((11.5) Flat, rounded, modern: the main action in the accent colour, the rest dark cards with a fine edge.)
        float r = Math.min(14 * dp, b.r.height() / 2);
        if (b.primary) {
            fill.setColor(0x40000000);
            tmp.set(b.r.left, b.r.top + 3 * dp, b.r.right, b.r.bottom + 3 * dp);
            c.drawRoundRect(tmp, r, r, fill);
            fill.setColor(ACCENT);
            c.drawRoundRect(b.r, r, r, fill);
            fill.setColor(0x26FFFFFF);
            tmp.set(b.r.left, b.r.top, b.r.right, b.r.centerY());
            c.drawRoundRect(tmp, r, r, fill);
        } else {
            fill.setColor(CARD_HI);
            c.drawRoundRect(b.r, r, r, fill);
            stroke.setStrokeWidth(1 * dp);
            stroke.setColor(EDGE);
            c.drawRoundRect(b.r, r, r, stroke);
        }
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(Math.min(16.5f * dp, b.r.height() * 0.36f));
        float fit = text.measureText(b.label);
        if (fit > b.r.width() - 18 * dp) text.setTextSize(text.getTextSize() * (b.r.width() - 18 * dp) / fit);
        text.setColor(b.primary ? ACCENT_INK : INK);
        c.drawText(b.label, b.r.centerX(), b.r.centerY() + text.getTextSize() * 0.36f, text);
    }

    /** ((11.5) The backdrop behind the menus: the city dimmed under a dusk-to-night fade.) */
    private void backdrop(Canvas c, int w, int h, float strength) {
        int bands = 14;
        for (int k = 0; k < bands; k++) {
            float f = k / (float) (bands - 1);
            // (Darkest at the top, behind the logo; the city shows through a little more lower down.)
            float g = 1 - Math.abs(f - 0.35f) * 0.5f;
            int a = (int) Math.min(255, 255 * strength * (0.62f + 0.36f * g));
            fill.setColor((a << 24) | 0x080B0F);
            c.drawRect(0, (float) Math.floor(h * k / (float) bands), w, (float) Math.floor(h * (k + 1) / (float) bands), fill);
        }
        // A faint green glow low on the screen, like the city's lights through smoke.
        fill.setColor(0x147CE04A);
        c.drawCircle(w * 0.5f, h * 1.05f, Math.max(w, h) * 0.55f, fill);
        fill.setColor(0x0E7CE04A);
        c.drawCircle(w * 0.5f, h * 1.05f, Math.max(w, h) * 0.75f, fill);
    }

    /** ((11.5) A page's top bar: a round back button and the title beside it. Returns where the page starts.) */
    private float header(Canvas c, int w, int h, String title) {
        fill.setColor(BG);
        c.drawRect(0, 0, w, h, fill);
        float top = 14 * dp, hh = 44 * dp;
        Button back = button("", A_BACK, false, 14 * dp, top, 14 * dp + hh, top + hh);
        back.invisible = true;
        fill.setColor(CARD_HI);
        c.drawCircle(14 * dp + hh / 2, top + hh / 2, hh / 2, fill);
        stroke.setColor(INK);
        stroke.setStrokeWidth(2.4f * dp);
        float cx = 14 * dp + hh / 2 - 2 * dp, cy = top + hh / 2;
        c.drawLine(cx + 5 * dp, cy - 8 * dp, cx - 3 * dp, cy, stroke);
        c.drawLine(cx - 3 * dp, cy, cx + 5 * dp, cy + 8 * dp, stroke);
        text.setTextAlign(Paint.Align.LEFT);
        text.setTextSize(24 * dp);
        float room = w - (14 * dp + hh + 16 * dp) - 14 * dp, fit = text.measureText(title);
        if (fit > room) text.setTextSize(24 * dp * room / fit);
        text.setColor(INK);
        c.drawText(title, 14 * dp + hh + 16 * dp, top + hh / 2 + text.getTextSize() * 0.36f, text);
        fill.setColor(0x14FFFFFF);
        c.drawRect(0, top + hh + 12 * dp, w, top + hh + 12 * dp + 1 * dp, fill);
        return top + hh + 20 * dp;
    }

    /** ((11.5) A small, spaced-out capitals heading for a section of a page; returns the y below it.) */
    private float section(Canvas c, String title, float x, float y) {
        plain.setTextAlign(Paint.Align.LEFT);
        plain.setTextSize(11.5f * dp);
        plain.setColor(ACCENT);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < title.length(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(Character.toUpperCase(title.charAt(i)));
        }
        c.drawText(sb.toString(), x + 2 * dp, y + 13 * dp, plain);
        return y + 24 * dp;
    }

    /**
     * ((11.5) The logo: a round badge - a city skyline inside a broken ring, a biohazard trefoil over it - and the
     * name beside or under it in heavy capitals, the second word in the accent colour.)
     */
    private void drawTitle(Canvas c, float cx, float cy, float maxW) {
        float bob = (float) Math.sin(time * 1.1f) * 1.2f * dp;
        float badge = Math.min(64 * dp, maxW * 0.16f);
        float by = cy - badge * 1.45f + bob;
        drawBadge(c, cx, by, badge);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(100);
        String a = "ZOMBIE", b = "CITY";
        float size = Math.min(58 * dp, maxW / text.measureText(a + " " + b) * 100);
        text.setTextSize(size);
        float wa = text.measureText(a + " "), wb = text.measureText(b), left = cx - (wa + wb) / 2, ty = cy + size * 0.42f + bob;
        text.setTextAlign(Paint.Align.LEFT);
        text.setColor(0x99000000);
        c.drawText(a + " " + b, left + 2 * dp, ty + 3 * dp, text);
        text.setColor(INK);
        c.drawText(a, left, ty, text);
        text.setColor(ACCENT);
        c.drawText(b, left + wa, ty, text);
        // The underline, broken like a bite out of it, and the tag line.
        fill.setColor(ACCENT);
        float uy = ty + size * 0.2f, uw = wa + wb;
        c.drawRect(left, uy, left + uw * 0.62f, uy + 3 * dp, fill);
        c.drawRect(left + uw * 0.7f, uy, left + uw, uy + 3 * dp, fill);
        plain.setTextAlign(Paint.Align.CENTER);
        plain.setTextSize(Math.max(11 * dp, size * 0.24f));
        plain.setColor(MUTED);
        c.drawText("S A N D B O X   ·   S U R V I V E   T H E   O U T B R E A K", cx, uy + plain.getTextSize() * 1.9f, plain);
    }

    private void drawBadge(Canvas c, float cx, float cy, float r) {
        fill.setColor(0x55000000);
        c.drawCircle(cx + 2 * dp, cy + 4 * dp, r, fill);
        fill.setColor(0xFF141A12);
        c.drawCircle(cx, cy, r, fill);
        // The skyline, cut off at the badge's edge.
        c.save();
        c.clipRect(cx - r * 0.72f, cy - r * 0.8f, cx + r * 0.72f, cy + r * 0.52f);
        fill.setColor(0xFF2C3A26);
        float[][] towers = {{-0.68f, 0.18f, 0.2f}, {-0.48f, -0.22f, 0.18f}, {-0.3f, 0.02f, 0.14f}, {-0.16f, -0.46f, 0.2f},
                {0.04f, -0.12f, 0.16f}, {0.2f, -0.34f, 0.18f}, {0.38f, 0.0f, 0.14f}, {0.52f, -0.16f, 0.18f}};
        for (float[] t : towers) c.drawRect(cx + t[0] * r, cy + t[1] * r, cx + (t[0] + t[2]) * r, cy + r * 0.52f, fill);
        // Lit windows, one or two going on and off.
        fill.setColor(0xFFB8E07A);
        for (int k = 0; k < 9; k++) {
            float wx = cx + (-0.6f + k * 0.14f) * r, wy = cy + (0.25f - (k * 37 % 5) * 0.12f) * r;
            if (((int) (time * 1.5f) + k) % 7 == 0) continue;
            c.drawRect(wx, wy, wx + r * 0.04f, wy + r * 0.05f, fill);
        }
        c.restore();
        // The ground line.
        fill.setColor(ACCENT);
        c.drawRect(cx - r * 0.72f, cy + r * 0.52f, cx + r * 0.72f, cy + r * 0.58f, fill);
        // The trefoil: three blades round a hub, slowly turning.
        float tr = r * 0.36f, ty = cy - r * 0.3f;
        tmp.set(cx - tr, ty - tr, cx + tr, ty + tr);
        float spin = time * 12;
        fill.setColor(ACCENT);
        for (int k = 0; k < 3; k++) c.drawArc(tmp, spin + k * 120, 60, true, fill);
        fill.setColor(0xFF141A12);
        c.drawCircle(cx, ty, tr * 0.38f, fill);
        fill.setColor(ACCENT);
        c.drawCircle(cx, ty, tr * 0.2f, fill);
        // The ring, broken at the bottom right like something got through.
        stroke.setStrokeWidth(r * 0.12f);
        tmp.set(cx - r * 0.9f, cy - r * 0.9f, cx + r * 0.9f, cy + r * 0.9f);
        c.drawArc(tmp, 70, 300, false, stroke);
        stroke.setColor(0x887CE04A);
        stroke.setStrokeWidth(r * 0.05f);
        c.drawArc(tmp, 22, 30, false, stroke);
    }

    private void drawMain(Canvas c, int w, int h) {
        // ((11.5) A modern main menu: the logo, one big button for what you most likely want, and the rest as tiles.)
        backdrop(c, w, h, 0.82f);
        boolean landscape = w > h;
        boolean game = host.canContinue();
        boolean slots = host.slotInfo(1) != null || host.slotInfo(2) != null || host.slotInfo(3) != null;
        float colW, x0, y;
        if (landscape) {
            drawTitle(c, w * 0.3f, h * 0.5f, w * 0.48f);
            colW = Math.min(340 * dp, w * 0.4f);
            x0 = w * 0.74f - colW / 2;
            y = h * 0.16f;
        } else {
            drawTitle(c, w / 2f, h * 0.24f, w - 48 * dp);
            colW = Math.min(360 * dp, w - 48 * dp);
            x0 = (w - colW) / 2;
            y = h * 0.42f;
        }
        float bigH = Math.min(62 * dp, h * 0.085f), tileH = Math.min(54 * dp, h * 0.075f), gap = 12 * dp;
        if (game) {
            button("Continue", A_CONTINUE, true, x0, y, x0 + colW, y + bigH);
            y += bigH + gap;
            button("New Game", A_NEW, false, x0, y, x0 + colW, y + tileH);
        } else button("New Game", A_NEW, true, x0, y, x0 + colW, y + bigH);
        y += (game ? tileH : bigH) + gap * 1.6f;
        // The rest, two to a row.
        java.util.ArrayList<Object[]> tiles = new java.util.ArrayList<Object[]>();
        if (slots) tiles.add(new Object[]{"Load Game", A_LOAD});
        tiles.add(new Object[]{"Settings", A_SETTINGS});
        tiles.add(new Object[]{"Records", A_RECORDS});
        tiles.add(new Object[]{settings.hasUnreadNotes() ? "What's New  •" : "What's New", A_NOTES});
        tiles.add(new Object[]{"Quit", A_QUIT});
        float tw = (colW - gap) / 2;
        for (int k = 0; k < tiles.size(); k++) {
            boolean lone = k == tiles.size() - 1 && tiles.size() % 2 == 1;
            float l = lone ? x0 : x0 + (k % 2) * (tw + gap), r = lone ? x0 + colW : l + tw;
            button((String) tiles.get(k)[0], (Integer) tiles.get(k)[1], false, l, y, r, y + tileH);
            if (k % 2 == 1 || lone) y += tileH + gap;
        }
        plain.setTextSize(12 * dp);
        plain.setTextAlign(Paint.Align.RIGHT);
        plain.setColor(0x88FFFFFF);
        c.drawText("v" + PatchNotes.VERSION, w - 14 * dp, h - 14 * dp, plain);
    }

    private void drawPause(Canvas c, int w, int h) {
        backdrop(c, w, h, 0.6f);
        // ((12.0) Restart: the same city from the start, with a second tap to be sure.)
        boolean sure = time - restartArmedAt < 4;
        String[] labels = {"Resume", "Save / Load", "Screenshot", "Radio Log", "Hide Buttons", "Stats", sure ? "Tap again to restart" : "Restart City",
                "New Game", "Settings", "Main Menu"};
        int[] actions = {A_RESUME, A_SAVES, A_SHOT, A_LOG, A_HUD, A_STATS, A_RESTART, A_NEW, A_SETTINGS, A_MAIN_MENU};
        int cols = w > h ? 2 : 1, rowsN = (labels.length + cols - 1) / cols;
        float gap = 7 * dp, bh = Math.min(46 * dp, (h - 120 * dp - gap * (rowsN - 1)) / rowsN);
        float colW = Math.min(300 * dp, (w - 60 * dp) / cols);
        float pw = colW * cols + gap * (cols - 1) + 40 * dp, ph = 76 * dp + bh * rowsN + gap * (rowsN - 1) + 20 * dp;
        tmp.set((w - pw) / 2, (h - ph) / 2, (w + pw) / 2, (h + ph) / 2);
        fill.setColor(0xF2161A20);
        c.drawRoundRect(tmp, 22 * dp, 22 * dp, fill);
        stroke.setColor(EDGE);
        stroke.setStrokeWidth(1 * dp);
        c.drawRoundRect(tmp, 22 * dp, 22 * dp, stroke);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(24 * dp);
        text.setColor(INK);
        c.drawText("Paused", w / 2f, tmp.top + 38 * dp, text);
        plain.setTextAlign(Paint.Align.CENTER);
        plain.setTextSize(12 * dp);
        plain.setColor(MUTED);
        World world = host.world();
        // ((12.0) Tap it to copy the code, to share the city.)
        boolean copied = time - copiedAt < 2;
        String line = copied ? "City code copied" : world.city.name + "  -  city code " + world.city.cfg.code() + "  (tap to copy)";
        plain.setColor(copied ? ACCENT : MUTED);
        c.drawText(fit(plain, line, tmp.width() - 24 * dp), w / 2f, tmp.top + 58 * dp, plain);
        button("", A_COPY_CODE, false, tmp.left + 20 * dp, tmp.top + 44 * dp, tmp.right - 20 * dp, tmp.top + 64 * dp).invisible = true;
        float top = tmp.top + 72 * dp;
        for (int i = 0; i < labels.length; i++) {
            int col = cols == 2 ? i % 2 : 0, row = cols == 2 ? i / 2 : i;
            float bx = tmp.left + 20 * dp + col * (colW + gap), by = top + row * (bh + gap);
            button(labels[i], actions[i], i == 0, bx, by, bx + colW, by + bh);
        }
    }

    /** The three save slots (and the autosave when loading). */
    private void drawSaves(Canvas c, int w, int h) {
        float top = header(c, w, h, savingMode ? "Save Game" : "Load Game");
        listArea.set(0, top, w, h - 12 * dp);
        float side = 20 * dp, maxW = Math.min(w - side * 2, 560 * dp), x0 = (w - maxW) / 2;
        float y = top + 10 * dp - scroll;
        int first = savingMode ? 1 : 0;
        for (int slot = first; slot <= 3; slot++) {
            String info = host.slotInfo(slot);
            if (slot == 0 && info == null) continue;
            tmp.set(x0, y, x0 + maxW, y + 86 * dp);
            fill.setColor(0xE0202328);
            c.drawRoundRect(tmp, 12 * dp, 12 * dp, fill);
            text.setTextAlign(Paint.Align.LEFT);
            text.setTextSize(16 * dp);
            text.setColor(ACCENT);
            c.drawText(slot == 0 ? "Autosave" : "Slot " + slot, x0 + 14 * dp, y + 26 * dp, text);
            plain.setTextAlign(Paint.Align.LEFT);
            plain.setTextSize(12.5f * dp);
            plain.setColor(0xFFB4BCC6);
            String[] lines = info == null ? new String[]{"Empty"} : info.split("\n");
            // ((12.0) Clear of the button: a long city name ran under it.)
            for (int k = 0; k < lines.length && k < 3; k++) c.drawText(fit(plain, lines[k], maxW - (savingMode && info != null ? 244 : 150) * dp), x0 + 14 * dp, y + 46 * dp + k * 15 * dp, plain);
            boolean can = savingMode || info != null;
            // ((12.0) Overwriting a save asks for a second tap; and a save can be loaded from here too, not only
            // from the main menu - the pause menu's "Save / Load" only saved.)
            boolean confirm = savingMode && info != null && armedSlot == slot && time - armedAt < 4;
            if (can) button(savingMode ? (info == null ? "Save" : confirm ? "Sure?" : "Overwrite") : "Load", A_SLOT + slot, true,
                    x0 + maxW - 120 * dp, y + 22 * dp, x0 + maxW - 14 * dp, y + 64 * dp);
            if (savingMode && info != null)
                button("Load", A_LOAD_SLOT + slot, false, x0 + maxW - 214 * dp, y + 22 * dp, x0 + maxW - 128 * dp, y + 64 * dp);
            y += 96 * dp;
        }
        contentHeight = y + scroll - top;
        clampScroll();
        if (noteTime > 0 && note != null) {
            noteTime -= frameDt;
            text.setTextAlign(Paint.Align.CENTER);
            text.setTextSize(13 * dp);
            fill.setColor(0xF0121418);
            c.drawRect(0, h - 44 * dp, w, h - 12 * dp, fill);
            text.setColor(0xFFFFE27A);
            c.drawText(note, w / 2f, h - 23 * dp, text);
        }
    }

    /** ((12.0) When the city code was last copied.) */
    private float copiedAt = -99;

    /** ((12.0) When Restart City was first tapped.) */
    private float restartArmedAt = -99;

    /** ((12.0) The slot waiting for a second tap to be overwritten, and when it was first tapped.) */
    private int armedSlot = -1;
    private float armedAt;

    private final java.util.WeakHashMap<Dispatch.Message, ArrayList<String>> logLines = new java.util.WeakHashMap<Dispatch.Message, ArrayList<String>>();
    private float logW;

    /** Every radio message this game, newest first. Tap one to go there. */
    private void drawLog(Canvas c, int w, int h) {
        float top = header(c, w, h, "Radio Log");
        float side = 16 * dp, maxW = Math.min(w - side * 2, 720 * dp), x0 = (w - maxW) / 2;
        // ((12.0) Filters: everything, or just the 911 calls, the police, the army or the news.)
        String[] tabs = {"All", "911", "Police", "Military", "News"};
        float tg = 6 * dp, tw = (maxW - tg * (tabs.length - 1)) / tabs.length, th = 34 * dp;
        for (int k = 0; k < tabs.length; k++)
            button(tabs[k], A_LOG_FILTER + k, logFilter == k, x0 + k * (tw + tg), top, x0 + k * (tw + tg) + tw, top + th);
        top += th + 8 * dp;
        listArea.set(0, top, w, h - 12 * dp);
        c.save();
        c.clipRect(listArea);
        float y = top + 8 * dp - scroll;
        java.util.ArrayList<Dispatch.Message> log = host.world().dispatch.log;
        plain.setTextSize(13 * dp);
        text.setTextSize(13 * dp);
        int shown = 0;
        for (int i = log.size() - 1; i >= 0; i--) {
            Dispatch.Message m = log.get(i);
            if (logFilter > 0 && m.color != Dispatch.WHO_COLORS[logFilter - 1]) continue;
            shown++;
            text.setTextAlign(Paint.Align.LEFT);
            float whoW = text.measureText(m.who + ": ");
            // ((12.0) Each message wrapped once, not every frame.)
            if (logW != maxW) {
                logW = maxW;
                logLines.clear();
            }
            ArrayList<String> lines = logLines.get(m);
            if (lines == null) logLines.put(m, lines = wrap(m.text, maxW - whoW - 24 * dp, plain));
            float rh = 12 * dp + lines.size() * 18 * dp;
            if (y + rh > top && y < h) {
                tmp.set(x0, y, x0 + maxW, y + rh);
                fill.setColor(0xE0202328);
                c.drawRoundRect(tmp, 8 * dp, 8 * dp, fill);
                text.setColor(m.color);
                c.drawText(m.who + ": ", x0 + 10 * dp, y + 20 * dp, text);
                plain.setTextAlign(Paint.Align.LEFT);
                plain.setColor(0xFFE6E6E6);
                for (int k = 0; k < lines.size(); k++) c.drawText(lines.get(k), x0 + 10 * dp + whoW, y + 20 * dp + k * 18 * dp, plain);
                // The whole row is a button (drawn invisibly by keeping it out of the button list's paint).
                Button b = button("", A_LOG_ROW + (log.size() - 1 - i), false, x0, Math.max(y, top), x0 + maxW, y + rh);
                b.invisible = true;
            }
            y += rh + 6 * dp;
        }
        c.restore();
        if (shown == 0) {
            plain.setTextAlign(Paint.Align.CENTER);
            plain.setColor(MUTED);
            c.drawText(log.isEmpty() ? "Nothing on the radio yet." : "Nothing like that on the radio yet.", w / 2f, top + 40 * dp, plain);
        }
        contentHeight = y + scroll - top;
        clampScroll();
        drawScrollHint(c, w);
    }

    /** Humans vs zombies over time (and police and army), plus the totals and heroes of this game. */
    /** What's been found out about the strain so far, with a "?" for each trait still unknown. */
    private static String strainTraits(World world) {
        StringBuilder sb = new StringBuilder();
        int hidden = 0;
        for (int t = 0; t < World.TR_COUNT; t++) {
            if (!world.trait[t]) continue;
            if (!world.traitKnown[t]) {
                hidden++;
                continue;
            }
            if (sb.length() > 0) sb.append(", ");
            sb.append(World.TRAIT_NAMES[t]);
        }
        for (int i = 0; i < hidden; i++) sb.append(sb.length() > 0 ? ", ?" : "?");
        return sb.length() == 0 ? "Nothing unusual" : sb.toString();
    }

    private void drawStats(Canvas c, int w, int h) {
        float top = header(c, w, h, "Stats");
        World world = host.world();
        button("Replay", A_REPLAY, true, w - 12 * dp - 110 * dp, 12 * dp, w - 12 * dp, 12 * dp + 42 * dp);
        boolean landscape = w > h;
        float side = 20 * dp;
        RectF chart = new RectF(side, top + 10 * dp, landscape ? w * 0.58f : w - side,
                landscape ? h - 20 * dp : top + Math.min(260 * dp, h * 0.36f));
        fill.setColor(0xFF1C1F24);
        c.drawRoundRect(chart, 10 * dp, 10 * dp, fill);
        RectF plot = new RectF(chart.left + 36 * dp, chart.top + 30 * dp, chart.right - 14 * dp, chart.bottom - 24 * dp);
        int n = world.histCount;
        int max = 10;
        for (int i = 0; i < n; i++) max = Math.max(max, Math.max(world.histHumans[i], world.histZombies[i]));
        max = (max + 9) / 10 * 10;
        plain.setTextSize(11 * dp);
        plain.setTextAlign(Paint.Align.RIGHT);
        stroke.setStrokeWidth(1);
        for (int k = 0; k <= 4; k++) {
            float y = plot.bottom - plot.height() * k / 4;
            stroke.setColor(0x30FFFFFF);
            c.drawLine(plot.left, y, plot.right, y, stroke);
            plain.setColor(0xFF8A9099);
            c.drawText(String.valueOf(max * k / 4), plot.left - 6 * dp, y + 4 * dp, plain);
        }
        plain.setTextAlign(Paint.Align.LEFT);
        int secs = (int) (n * world.statStep);
        plain.setColor(0xFF8A9099);
        c.drawText("0:00", plot.left, plot.bottom + 16 * dp, plain);
        plain.setTextAlign(Paint.Align.RIGHT);
        c.drawText(World.clock(secs), plot.right, plot.bottom + 16 * dp, plain);
        if (n >= 2) {
            int[][] series = {world.histHumans, world.histZombies, world.histArmed};
            int[] colors = {0xFF6FA8FF, 0xFF7CC24E, 0xFFE8C547};
            for (int sIdx = 0; sIdx < 3; sIdx++) {
                stroke.setColor(colors[sIdx]);
                stroke.setStrokeWidth(2.2f * dp);
                for (int i = 1; i < n; i++) {
                    float x0 = plot.left + plot.width() * (i - 1) / (n - 1), x1 = plot.left + plot.width() * i / (n - 1);
                    float y0 = plot.bottom - plot.height() * series[sIdx][i - 1] / max;
                    float y1 = plot.bottom - plot.height() * series[sIdx][i] / max;
                    c.drawLine(x0, y0, x1, y1, stroke);
                }
            }
        } else {
            plain.setTextAlign(Paint.Align.CENTER);
            c.drawText("Play a little longer to see the chart.", plot.centerX(), plot.centerY(), plain);
        }
        // Legend.
        text.setTextSize(12 * dp);
        text.setTextAlign(Paint.Align.LEFT);
        fill.setColor(0xFF6FA8FF);
        c.drawCircle(chart.left + 16 * dp, chart.top + 16 * dp, 4.5f * dp, fill);
        text.setColor(0xFFE6E6E6);
        c.drawText("People", chart.left + 26 * dp, chart.top + 20 * dp, text);
        fill.setColor(0xFF7CC24E);
        c.drawCircle(chart.left + 96 * dp, chart.top + 16 * dp, 4.5f * dp, fill);
        c.drawText("Zombies", chart.left + 106 * dp, chart.top + 20 * dp, text);
        fill.setColor(0xFFE8C547);
        c.drawCircle(chart.left + 186 * dp, chart.top + 16 * dp, 4.5f * dp, fill);
        c.drawText("Police & army", chart.left + 196 * dp, chart.top + 20 * dp, text);

        int t = (int) world.time;
        String[][] totals = {
                {"Time", World.clock(t)},
                {"People now", String.valueOf(world.humanCount() + world.hiding + world.riding + world.visiting)},
                {"Zombies now", String.valueOf(world.zombieCount())},
                {"Most zombies at once", String.valueOf(world.peakZombies)},
                {"Zombies killed", String.valueOf(world.zombiesKilled)},
                {"People turned", String.valueOf(world.turned)},
                {"Civilians lost", String.valueOf(world.civiliansLost)},
                {"Left by bus or train", String.valueOf(world.fledByTransit)},
                // ((12.0) More of what the game was already keeping count of.)
                {"Fled the city (came home)", world.fled + " (" + world.cameHome + ")"},
                {"Bites", String.valueOf(world.bites)},
                {"Biggest horde", String.valueOf(world.biggestHorde)},
                {"Most police, army at once", world.peakCops + ", " + world.peakSoldiers},
                {"Cars wrecked, buildings collapsed", world.carsWrecked + ", " + world.collapsedCount},
                {"911 calls", String.valueOf(world.dispatch.calls)},
                {"Shots fired", String.valueOf(world.shotsFired)},
                {"Volunteers joined (said no)", world.recruits + " (" + world.refused + ")"},
                {"City", world.city.name + "  (" + world.city.cfg.code() + ")"},
                {"Bitten people turned away at checkpoints", String.valueOf(world.turnedAway)},
                {"Outbreak started", world.outbreakPlace == null ? "-" : world.outbreakPlace},
                {"Infection chain", chainText(world)},
                {world.strainName, strainTraits(world)},
                {"Balance of power", world.warResult == 1 ? "The city survived" : world.warResult == 2 ? "The city fell"
                        : "People " + (int) (world.warBalance * 100) + "%"},
        };
        java.util.ArrayList<int[]> heroes = world.heroes(3);
        String[][] withHeroes = new String[totals.length + heroes.size()][];
        System.arraycopy(totals, 0, withHeroes, 0, totals.length);
        for (int i = 0; i < heroes.size(); i++) {
            int[] hr = heroes.get(i);
            String who = hr[1] == Entity.DOG ? Names.dog(hr[0]) : Names.person(hr[0]);
            withHeroes[totals.length + i] = new String[]{(i == 0 ? "Top killer: " : "   ") + who + (hr[4] == 0 ? " (fallen)" : ""),
                    hr[3] + " kills"};
        }
        totals = withHeroes;
        // (10.32) The outbreak as it happened: the key moments, with when.
        if (!world.timeline.isEmpty()) {
            int show = Math.min(world.timeline.size(), 10);
            String[][] withTimeline = new String[totals.length + show + 1][];
            System.arraycopy(totals, 0, withTimeline, 0, totals.length);
            withTimeline[totals.length] = new String[]{"Timeline", ""};
            for (int i = 0; i < show; i++) {
                World.Moment m = world.timeline.get(world.timeline.size() <= 10 ? i : (i == 0 ? 0 : world.timeline.size() - show + i));
                int mt = (int) m.time;
                withTimeline[totals.length + 1 + i] = new String[]{"   " + m.text, World.clock(mt)};
            }
            totals = withTimeline;
        }
        float tx0 = landscape ? chart.right + 24 * dp : side, tx1 = w - side;
        float ty = landscape ? top + 26 * dp : chart.bottom + 30 * dp, lh = Math.min(26 * dp, (h - ty - 10 * dp) / totals.length);
        // ((12.0) Each figure kept clear of its label; and when the rows don't all fit, they scroll rather than
        // shrink to nothing.)
        lh = Math.max(lh, 19 * dp);
        float ts = Math.max(9 * dp, Math.min(14 * dp, lh * 0.75f));
        listArea.set(tx0Of(landscape, chart, side), ty - lh, w, h - 6 * dp);
        contentHeight = totals.length * lh + 10 * dp;
        clampScroll();
        c.save();
        c.clipRect(listArea);
        ty -= scroll;
        plain.setTextSize(ts);
        for (String[] row : totals) {
            plain.setTextAlign(Paint.Align.LEFT);
            plain.setColor(0xFFB8BDC4);
            text.setTextAlign(Paint.Align.RIGHT);
            text.setTextSize(ts);
            text.setColor(0xFFFFFFFF);
            float vw = Math.min(text.measureText(row[1]), (tx1 - tx0) * 0.6f);
            c.drawText(fit(plain, row[0], tx1 - tx0 - vw - 10 * dp), tx0, ty, plain);
            c.drawText(fit(text, row[1], vw), tx1, ty, text);
            ty += lh;
        }
        c.restore();
        drawScrollHint(c, w, listArea.left);
    }

    private float tx0Of(boolean landscape, RectF chart, float side) {
        return landscape ? chart.right + 24 * dp : 0;
    }

    // ------------------------------------------------------------------ outbreak replay

    /** Where the replay is (0 to 1), whether it's playing, and the timeline's touch area. */
    private float replayPos;
    private boolean replayPlaying;
    private final RectF replayBar = new RectF(), replayMap = new RectF();
    /** A full replay lasts this many seconds, however long the game was. */
    private static final float REPLAY_SECONDS = 24;

    private static String chainText(World w) {
        if (w.patientZeroSeed == 0) return "-";
        int[] ch = w.chainFromPatientZero();
        Integer most = null;
        int mostN = 0;
        for (java.util.Map.Entry<Integer, Integer> en : w.victims.entrySet())
            if (en.getValue() > mostN) {
                mostN = en.getValue();
                most = en.getKey();
            }
        return "Patient zero " + Names.person(w.patientZeroSeed) + ": " + ch[0] + " people, " + ch[1] + " generations"
                + (most != null ? ". Worst: " + Names.person(most) + " (" + mostN + ")" : "");
    }

    private void scrubReplay(float x) {
        replayPos = Math.max(0, Math.min(1, (x - replayBar.left) / replayBar.width()));
        replayPlaying = false;
        // Snap to a big moment if one is close.
        World w = host.world();
        if (w.replayMeta.size() > 1) {
            float t0 = w.replayMeta.get(0)[0], t1 = w.replayMeta.get(w.replayMeta.size() - 1)[0];
            float best = 14 * dp;
            for (float[] h : w.highlightAt) {
                if (h[0] < t0 || h[0] > t1) continue;
                float hx = replayBar.left + replayBar.width() * (h[0] - t0) / (t1 - t0);
                if (Math.abs(hx - x) < best) {
                    best = Math.abs(hx - x);
                    replayPos = (h[0] - t0) / (t1 - t0);
                }
            }
        }
    }

    /**
     * The whole game played back on the city map: people in blue, the dead in green, and a red mark everywhere
     * someone turned, building up as the infection spreads.
     */
    private void drawReplay(Canvas c, int w, int h, float dt) {
        float top = header(c, w, h, "Outbreak replay");
        World world = host.world();
        java.util.ArrayList<float[]> meta = world.replayMeta;
        int frames = meta.size();
        if (replayPlaying && frames > 1) {
            replayPos += dt / REPLAY_SECONDS;
            if (replayPos >= 1) {
                replayPos = 1;
                replayPlaying = false;
            }
        }
        float barH = 44 * dp, bottom = h - 16 * dp - barH - 56 * dp;
        float size = Math.min(w - 32 * dp, bottom - top);
        replayMap.set((w - size) / 2, top, (w + size) / 2, top + size);
        City city = world.city;
        fill.setColor(0xFF1C1F24);
        c.drawRect(replayMap, fill);
        bmpSrc.set(0, 0, city.bitmap.getWidth(), city.bitmap.getHeight());
        bmpPaint.setAlpha(150);
        c.drawBitmap(city.bitmap, bmpSrc, replayMap, bmpPaint);
        bmpPaint.setAlpha(255);
        if (frames == 0) {
            text.setTextAlign(Paint.Align.CENTER);
            text.setTextSize(15 * dp);
            text.setColor(0xFFE6E6E6);
            c.drawText("Play a little longer to see the replay.", replayMap.centerX(), replayMap.centerY(), text);
            return;
        }
        int f = Math.min(frames - 1, (int) (replayPos * (frames - 1) + 0.5f));
        float[] m = meta.get(f);
        short[] dots = world.replayDots.get(f);
        float sx = replayMap.width() / city.worldW(), sy = replayMap.height() / city.worldH();
        float r = Math.max(1.4f * dp, size / 260);
        // Where people turned, up to now: old marks fade.
        for (int i = 0, n = world.turnEvents.size(); i < n; i++) {
            float[] t = world.turnEvents.get(i);
            if (t[0] > m[0]) break;
            float age = m[0] - t[0];
            fill.setColor(age < 20 ? 0xFFFF4A3A : 0x80B03028);
            c.drawCircle(replayMap.left + t[1] * sx, replayMap.top + t[2] * sy, r * (age < 20 ? 2.2f : 1.3f), fill);
        }
        int zn = (int) m[3], k = 0;
        for (int i = 0; i + 1 < dots.length; i += 2, k++) {
            fill.setColor(k < zn ? 0xFF8BE05A : 0xFF6FA8FF);
            c.drawCircle(replayMap.left + dots[i] * sx, replayMap.top + dots[i + 1] * sy, r, fill);
        }
        // Time and counts.
        int secs = (int) m[0];
        text.setTextAlign(Paint.Align.LEFT);
        text.setTextSize(14 * dp);
        text.setColor(0xFFFFFFFF);
        float ly = replayMap.bottom + 22 * dp;
        text.setTextAlign(Paint.Align.CENTER);
        c.drawText(World.clock(secs), replayMap.centerX(), ly, text);
        ly += 22 * dp;
        text.setTextSize(13 * dp);
        text.setTextAlign(Paint.Align.RIGHT);
        int turnedSoFar = 0;
        for (int i = 0, n = world.turnEvents.size(); i < n && world.turnEvents.get(i)[0] <= m[0]; i++) turnedSoFar++;
        text.setColor(0xFF6FA8FF);
        String counts = "People " + (int) m[2];
        float cx = (w + text.measureText(counts + "   Zombies " + (int) m[1] + "   Turned " + turnedSoFar)) / 2;
        c.drawText(counts, cx - text.measureText("   Zombies " + (int) m[1] + "   Turned " + turnedSoFar), ly, text);
        text.setColor(0xFF8BE05A);
        c.drawText("   Zombies " + (int) m[1], cx - text.measureText("   Turned " + turnedSoFar), ly, text);
        text.setColor(0xFFFF6A5A);
        c.drawText("   Turned " + turnedSoFar, cx, ly, text);
        // The timeline: drag it to scrub, the button plays and pauses.
        float bw = 90 * dp;
        float by = Math.min(h - 16 * dp - barH, ly + 18 * dp);
        button(replayPlaying ? "Pause" : replayPos >= 1 ? "Again" : "Play", A_REPLAY_PLAY, true, replayMap.left, by, replayMap.left + bw, by + barH);
        replayBar.set(replayMap.left + bw + 14 * dp, by, replayMap.right, by + barH);
        float mid = replayBar.centerY();
        fill.setColor(0xFF2A2E34);
        tmp.set(replayBar.left, mid - 4 * dp, replayBar.right, mid + 4 * dp);
        c.drawRoundRect(tmp, 4 * dp, 4 * dp, fill);
        fill.setColor(0xFF3F8A3A);
        tmp.set(replayBar.left, mid - 4 * dp, replayBar.left + replayBar.width() * replayPos, mid + 4 * dp);
        c.drawRoundRect(tmp, 4 * dp, 4 * dp, fill);
        // The big moments, as ticks on the timeline; tap near one to jump to it.
        float t0 = meta.get(0)[0], t1 = meta.get(frames - 1)[0];
        java.util.ArrayList<float[]> hl = world.highlightAt;
        String showing = null;
        for (int i = 0; i < hl.size(); i++) {
            float ht = hl.get(i)[0];
            if (ht < t0 || ht > t1 || t1 <= t0) continue;
            float hx = replayBar.left + replayBar.width() * (ht - t0) / (t1 - t0);
            fill.setColor(0xFFE8C547);
            c.drawRect(hx - 1.5f * dp, mid - 12 * dp, hx + 1.5f * dp, mid - 5 * dp, fill);
            if (Math.abs(ht - m[0]) < Math.max(6, (t1 - t0) / 60)) {
                showing = world.highlightText.get(i);
                float[] at = hl.get(i);
                stroke.setColor(0xFFE8C547);
                stroke.setStrokeWidth(2 * dp);
                c.drawCircle(replayMap.left + at[1] * sx, replayMap.top + at[2] * sy, 12 * dp, stroke);
            }
        }
        if (showing != null) {
            text.setTextAlign(Paint.Align.CENTER);
            text.setTextSize(14 * dp);
            showing = fit(text, showing, replayMap.width() - 40 * dp);
            float tw = text.measureText(showing) + 24 * dp;
            tmp.set(replayMap.centerX() - tw / 2, replayMap.top + 8 * dp, replayMap.centerX() + tw / 2, replayMap.top + 34 * dp);
            fill.setColor(0xE0101216);
            c.drawRoundRect(tmp, 8 * dp, 8 * dp, fill);
            text.setColor(0xFFE8C547);
            c.drawText(showing, replayMap.centerX(), replayMap.top + 26 * dp, text);
        }
        fill.setColor(0xFFFFFFFF);
        c.drawCircle(replayBar.left + replayBar.width() * replayPos, mid, 10 * dp, fill);
    }

    /** The How to Play cards. */
    private void drawOptions(Canvas c, int w, int h, String title, OptionSet opts, boolean setup) {
        float top = header(c, w, h, title);
        float footer = setup ? 72 * dp : 12 * dp;
        listArea.set(0, top, w, h - footer);
        int cols = w >= 700 * dp ? 2 : 1;
        float gap = 10 * dp, side = 16 * dp, rowH = 50 * dp;
        float colW = (w - side * 2 - gap * (cols - 1)) / cols;
        int n = opts.count();
        int perCol = (n + cols - 1) / cols;
        contentHeight = perCol * (rowH + gap);
        clampScroll();

        c.save();
        c.clipRect(listArea);
        for (int i = 0; i < n; i++) {
            int col = i / perCol, row = i % perCol;
            float x = side + col * (colW + gap), y = top + row * (rowH + gap) - scroll;
            Row r = new Row();
            r.index = i;
            r.r.set(x, y, x + colW, y + rowH);
            float vw = Math.min(190 * dp, colW * 0.55f);
            r.prev.set(r.r.right - vw, y, r.r.right - vw + 44 * dp, y + rowH);
            rows.add(r);

            // ((11.5) A card per setting, its value in a pill with arrows either side.)
            fill.setColor(CARD);
            c.drawRoundRect(r.r, 14 * dp, 14 * dp, fill);
            plain.setTextSize(15 * dp);
            plain.setTextAlign(Paint.Align.LEFT);
            plain.setColor(INK);
            c.drawText(fit(plain, opts.label(i), colW - vw - 24 * dp), x + 16 * dp, y + rowH / 2 + 5 * dp, plain);

            tmp.set(r.r.right - vw, y + 7 * dp, r.r.right - 7 * dp, y + rowH - 7 * dp);
            fill.setColor(CARD_HI);
            c.drawRoundRect(tmp, tmp.height() / 2, tmp.height() / 2, fill);
            stroke.setColor(ACCENT);
            stroke.setStrokeWidth(2 * dp);
            float ay = tmp.centerY();
            c.drawLine(tmp.left + 19 * dp, ay - 5 * dp, tmp.left + 14 * dp, ay, stroke);
            c.drawLine(tmp.left + 14 * dp, ay, tmp.left + 19 * dp, ay + 5 * dp, stroke);
            c.drawLine(tmp.right - 19 * dp, ay - 5 * dp, tmp.right - 14 * dp, ay, stroke);
            c.drawLine(tmp.right - 14 * dp, ay, tmp.right - 19 * dp, ay + 5 * dp, stroke);
            text.setTextSize(14 * dp);
            text.setTextAlign(Paint.Align.CENTER);
            text.setColor(INK);
            c.drawText(opts.values(i)[opts.get(i)], tmp.centerX(), tmp.centerY() + 5 * dp, text);
        }
        c.restore();
        drawScrollHint(c, w);

        if (setup) {
            float bgap = 8 * dp, bw = Math.min(180 * dp, (w - 32 * dp - bgap * 2) / 3), bh = 50 * dp, by = h - footer + 11 * dp;
            float x = (w - bw * 3 - bgap * 2) / 2;
            button("Surprise me", A_RANDOM, false, x, by, x + bw, by + bh);
            button(config.keepCity ? config.code() : "City code", A_CODE, false, x + bw + bgap, by, x + bw * 2 + bgap, by + bh);
            button("Start  ›", A_START, true, x + bw * 2 + bgap * 2, by, x + bw * 3 + bgap * 2, by + bh);
            if (noteTime > 0 && note != null) {
                noteTime -= frameDt;
                fill.setColor(0xF0121418);
                c.drawRect(0, by - 30 * dp, w, by - 4 * dp, fill);
                text.setTextAlign(Paint.Align.CENTER);
                text.setTextSize(13 * dp);
                text.setColor(0xFFFFE27A);
                c.drawText(note, w / 2f, by - 12 * dp, text);
            }
        }
    }

    /** Best results across all games. */
    private void drawRecords(Canvas c, int w, int h) {
        float top = header(c, w, h, "Records");
        listArea.set(0, top, w, h - 12 * dp);
        float side = 20 * dp, maxW = Math.min(w - side * 2, 640 * dp), x0 = (w - maxW) / 2;
        c.save();
        c.clipRect(listArea);
        float y = top + 10 * dp - scroll;
        text.setTextAlign(Paint.Align.LEFT);
        text.setTextSize(17 * dp);
        text.setColor(ACCENT);
        c.drawText("Records", x0, y + 18 * dp, text);
        y += 34 * dp;
        for (int i = 0; i < Records.RECORD_NAMES.length; i++) {
            plain.setTextAlign(Paint.Align.LEFT);
            plain.setTextSize(14.5f * dp);
            plain.setColor(0xFFB8BDC4);
            c.drawText(Records.RECORD_NAMES[i], x0, y + 14 * dp, plain);
            text.setTextAlign(Paint.Align.RIGHT);
            text.setTextSize(14.5f * dp);
            text.setColor(0xFFFFFFFF);
            c.drawText(records.recordText(i), x0 + maxW, y + 14 * dp, text);
            y += 26 * dp;
        }
        c.restore();
        contentHeight = y + scroll - top;
        clampScroll();
        drawScrollHint(c, w);
    }

    // ------------------------------------------------------------------ New Game

    private Seg seg(int option, int value, float l, float t, float r, float b) {
        Seg sg = segPool.isEmpty() ? new Seg() : segPool.remove(segPool.size() - 1);
        sg.option = option;
        sg.value = value;
        sg.r.set(l, t, r, b);
        segs.add(sg);
        return sg;
    }

    /** Starts building the preview picture if the city changed since the last one. */
    private void updatePreview() {
        final String key = config.code();
        if (key.equals(previewKey) || key.equals(buildingKey)) return;
        buildingKey = key;
        final CityConfig copy = new CityConfig();
        System.arraycopy(config.v, 0, copy.v, 0, copy.v.length);
        copy.seed = config.seed;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    City city = new City(copy, 360f / (copy.tiles() * City.T));
                    if (key.equals(buildingKey)) {
                        preview = city.bitmap;
                        previewResidents = city.totalResidents;
                        previewForce = World.forecast(copy, city, settings.maxPopulation());
                        previewKey = key;
                    }
                } catch (Throwable ignored) {
                    // No preview; the game itself still works.
                }
            }
        });
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    /** A row of equal buttons, one per value, with the chosen one lit up. */
    private float segRow(Canvas c, int option, String label, float x, float y, float width) {
        // ((11.5) A heading, then the choices as a segmented control: a dark track, the chosen one lit up.)
        y = section(c, label, x, y);
        String[] values = config.values(option);
        // (Choices that are no longer offered are left out, unless a loaded city code is using one.)
        int shown = 0;
        int[] index = new int[values.length];
        for (int i = 0; i < values.length; i++)
            if (!CityConfig.retired(option, i) || config.get(option) == i) index[shown++] = i;
        int lines = shown > 12 ? 3 : shown > 6 ? 2 : 1;
        int perRow = (shown + lines - 1) / lines;
        int rows = (shown + perRow - 1) / perRow;
        float pad = 4 * dp, bh = 38 * dp, bw = (width - pad * 2) / perRow;
        tmp.set(x, y, x + width, y + rows * bh + pad * 2);
        fill.setColor(CARD);
        c.drawRoundRect(tmp, 14 * dp, 14 * dp, fill);
        for (int k = 0; k < shown; k++) {
            int i = index[k];
            float l = x + pad + (k % perRow) * bw, t = y + pad + (k / perRow) * bh;
            Seg sg = seg(option, i, l, t, l + bw, t + bh);
            boolean on = config.get(option) == i;
            if (on) {
                fill.setColor(ACCENT);
                c.drawRoundRect(sg.r, 11 * dp, 11 * dp, fill);
            }
            text.setTextAlign(Paint.Align.CENTER);
            text.setTextSize(Math.min(14 * dp, bw / Math.max(3, values[i].length()) * 1.5f));
            text.setColor(on ? ACCENT_INK : 0xFFC4CBD3);
            c.drawText(values[i], sg.r.centerX(), sg.r.centerY() + text.getTextSize() * 0.36f, text);
        }
        return y + rows * bh + pad * 2 + 10 * dp;
    }

    /** A grid of map cards (the maps given by index); returns the y below it. */
    private float mapCards(Canvas c, int[] shown, int cards, int preset, float lx, float y, float cw, float ch, float gap, int perRow) {
        // ((11.5) Map cards: a tall tile each, a colour wash for the map, its name along the bottom.)
        for (int k = 0; k < cards; k++) {
            int i = shown[k];
            float l = lx + (k % perRow) * (cw + gap), t = y + (k / perRow) * (ch + gap);
            Seg sg = seg(CityConfig.OPT_PRESET, i, l, t, l + cw, t + ch);
            boolean on = preset == i;
            fill.setColor(CARD);
            c.drawRoundRect(sg.r, 14 * dp, 14 * dp, fill);
            int col = MAP_COLORS[i % MAP_COLORS.length];
            fill.setColor((col & 0x00FFFFFF) | (on ? 0x55000000 : 0x26000000));
            tmp.set(l, t, l + cw, t + ch * 0.62f);
            c.drawRoundRect(tmp, 14 * dp, 14 * dp, fill);
            // A little emblem: water maps get waves, the city a skyline.
            fill.setColor((col & 0x00FFFFFF) | 0xCC000000);
            float ex = l + cw / 2, ey = t + ch * 0.34f, u = Math.min(cw, ch) * 0.12f;
            if (i == CityConfig.ISLANDS || i == CityConfig.ARCHIPELAGO) {
                c.drawCircle(ex - u * 1.2f, ey, u * 0.9f, fill);
                c.drawCircle(ex + u * 1.1f, ey + u * 0.4f, u * 0.65f, fill);
            } else {
                c.drawRect(ex - u * 1.6f, ey - u * 0.4f, ex - u * 0.8f, ey + u, fill);
                c.drawRect(ex - u * 0.6f, ey - u * 1.4f, ex + u * 0.3f, ey + u, fill);
                c.drawRect(ex + u * 0.5f, ey - u * 0.8f, ex + u * 1.5f, ey + u, fill);
            }
            if (on) {
                stroke.setColor(ACCENT);
                stroke.setStrokeWidth(2 * dp);
                c.drawRoundRect(sg.r, 14 * dp, 14 * dp, stroke);
            }
            text.setTextAlign(Paint.Align.CENTER);
            text.setTextSize(Math.min(14 * dp, cw / 7f));
            text.setColor(on ? INK : 0xFFB8C0C8);
            c.drawText(CityConfig.PRESETS[i], l + cw / 2, t + ch - 12 * dp, text);
        }
        return y + ((cards + perRow - 1) / perRow) * (ch + gap) + 12 * dp;
    }

    private void drawSetup(Canvas c, int w, int h) {
        float top = header(c, w, h, "New Game");
        updatePreview();
        boolean landscape = w > h;
        float footer = 72 * dp, side = 16 * dp;
        // The city preview: left in landscape, on top in portrait.
        float pv = landscape ? Math.min(h - top - footer - 72 * dp, w * 0.34f) : Math.min(w - side * 2, (h - top) * 0.3f);
        float px = landscape ? side : (w - pv) / 2, py = top + 4 * dp;
        tmp.set(px - 4 * dp, py - 4 * dp, px + pv + 4 * dp, py + pv + 4 * dp);
        fill.setColor(CARD);
        c.drawRoundRect(tmp, 18 * dp, 18 * dp, fill);
        Bitmap bmp = preview;
        boolean current = config.code().equals(previewKey);
        if (bmp != null) {
            bmpSrc.set(0, 0, bmp.getWidth(), bmp.getHeight());
            tmp.set(px, py, px + pv, py + pv);
            bmpPaint.setAlpha(current ? 255 : 110);
            c.drawBitmap(bmp, bmpSrc, tmp, bmpPaint);
        }
        // ((11.5) Rounded corners on the picture, and a fine edge.)
        stroke.setColor(CARD);
        stroke.setStrokeWidth(8 * dp);
        tmp.set(px - 4 * dp, py - 4 * dp, px + pv + 4 * dp, py + pv + 4 * dp);
        c.drawRoundRect(tmp, 18 * dp, 18 * dp, stroke);
        stroke.setColor(EDGE);
        stroke.setStrokeWidth(1 * dp);
        c.drawRoundRect(tmp, 18 * dp, 18 * dp, stroke);
        if (!current) {
            // (A spinning arc while the city is built.)
            stroke.setColor(ACCENT);
            stroke.setStrokeWidth(3 * dp);
            tmp.set(px + pv / 2 - 16 * dp, py + pv / 2 - 30 * dp, px + pv / 2 + 16 * dp, py + pv / 2 + 2 * dp);
            c.drawArc(tmp, time * 360, 260, false, stroke);
            text.setTextAlign(Paint.Align.CENTER);
            text.setTextSize(14 * dp);
            text.setColor(INK);
            c.drawText("Building city...", px + pv / 2, py + pv / 2 + 26 * dp, text);
        }
        int preset = config.v[CityConfig.OPT_PRESET];
        float infoY = py + pv + 20 * dp;
        float infoW = landscape ? pv : w - side * 2, infoX = landscape ? px : side;
        text.setTextAlign(Paint.Align.LEFT);
        text.setTextSize(19 * dp);
        text.setColor(INK);
        c.drawText(CityConfig.PRESETS[preset], infoX, infoY, text);
        // (The city code as a little chip.)
        plain.setTextSize(12 * dp);
        String chip = "CITY " + config.code();
        float cwid = plain.measureText(chip) + 18 * dp;
        tmp.set(infoX + infoW - cwid, infoY - 15 * dp, infoX + infoW, infoY + 6 * dp);
        fill.setColor(CARD_HI);
        c.drawRoundRect(tmp, 10 * dp, 10 * dp, fill);
        plain.setTextAlign(Paint.Align.CENTER);
        plain.setColor(MUTED);
        c.drawText(chip, tmp.centerX(), infoY - 0.5f * dp, plain);
        float ly = infoY + 10 * dp;
        if (landscape) {
            button("New map", A_REROLL, false, px, ly, px + pv, ly + 34 * dp);
        } else {
            ly = drawInfo(c, preset, infoX, infoY + 18 * dp, infoW);
        }

        // The options, scrolling if they don't fit.
        float lx = landscape ? px + pv + 24 * dp : side, lw = w - lx - side;
        float listTop = landscape ? top : ly + 2 * dp;
        if (!landscape) {
            button("New map", A_REROLL, false, w - side - 110 * dp, py + pv - 36 * dp, w - side - 6 * dp, py + pv - 6 * dp);
        }
        listArea.set(0, listTop, w, h - footer);
        c.save();
        c.clipRect(listArea);
        float y = listTop + 4 * dp - scroll;
        if (landscape) y = drawInfo(c, preset, lx, y + 14 * dp, lw) + 4 * dp;
        // Map cards.
        y = section(c, "Map", lx, y);
        int perRow = lw > 520 * dp ? 5 : lw > 330 * dp ? 4 : 3;
        float gap = 10 * dp, cw = (lw - gap * (perRow - 1)) / perRow, ch = Math.min(96 * dp, cw * 0.9f);
        // (Maps no longer offered are left out, unless a loaded city code is using one. The real cities have
        // their own section below.)
        int cards = 0;
        int[] shown = new int[CityConfig.PRESETS.length];
        for (int i = 0; i < CityConfig.PRESETS.length; i++)
            if ((!CityConfig.retired(CityConfig.OPT_PRESET, i) || preset == i) && !RealCities.isReal(i)) shown[cards++] = i;
        y = mapCards(c, shown, cards, preset, lx, y, cw, ch, gap, perRow);
        // (The real cities were taken out in 10.28; a loaded code using one still shows it above.)
        if (RealCities.isReal(preset)) {
            shown[0] = preset;
            y = mapCards(c, shown, 1, preset, lx, y, cw, ch, gap, perRow);
        }
        boolean real = RealCities.isReal(preset);
        if (preset == CityConfig.ISLANDS || preset == CityConfig.ARCHIPELAGO || real) {
            // Islands comes in one size only; a real city is always the same city.
            plain.setTextAlign(Paint.Align.LEFT);
            plain.setTextSize(13 * dp);
            plain.setColor(0xFFB8BDC4);
            String note = real ? "Map size: Massive, and always the same city."
                    : preset == CityConfig.ARCHIPELAGO ? "Map size: Archipelago is always one size, " + CityConfig.ARCHIPELAGO_TILES + " tiles across (just over Massive)."
                    : "Map size: Islands is always one size, " + CityConfig.ISLANDS_TILES + " tiles across (between Large and Massive).";
            for (String line : wrap(note, lw - 4 * dp, plain)) {
                c.drawText(line, lx + 2 * dp, y + 14 * dp, plain);
                y += 17 * dp;
            }
            y += 14 * dp;
        } else {
            y = segRow(c, CityConfig.OPT_SIZE, "Map size", lx, y, lw);
            // (10.32) The biggest maps: how long they take to build, and whether this phone has the memory.
            int size = config.v[CityConfig.OPT_SIZE];
            if (size >= 5) {
                int needMb = size >= 6 ? 480 : 256;
                long haveMb = Runtime.getRuntime().maxMemory() / 1048576;
                boolean low = haveMb < needMb;
                String note = low ? "Warning: this phone gives the game " + haveMb + " MB and a " + (size >= 6 ? "Gigantic" : "Massive")
                        + " city needs about " + needMb + " MB. It may run out of memory - try a smaller size."
                        : (size >= 6 ? "Gigantic" : "Massive") + " maps take a while to build (up to a minute on a slower phone) and need a recent phone to run smoothly.";
                plain.setTextAlign(Paint.Align.LEFT);
                plain.setTextSize(12.5f * dp);
                plain.setColor(low ? 0xFFFF9A7A : 0xFF9AA0A8);
                y -= 6 * dp;
                for (String line : wrap(note, lw - 4 * dp, plain)) {
                    c.drawText(line, lx + 2 * dp, y + 10 * dp, plain);
                    y += 16 * dp;
                }
                y += 12 * dp;
            }
        }
        int[] fc = previewForce;
        boolean known = fc != null && previewResidents > 0 && config.code().equals(previewKey);
        // (10.50) How many people are about isn't a choice: they come and go from their homes as they please.
        // ((10.56) The numbers are the game's own count as it starts, not just the residents and the town's
        // officers: it used to show a good deal fewer police and soldiers than there then were.)
        plain.setTextSize(12.5f * dp);
        plain.setColor(MUTED);
        if (known) {
            c.drawText(fc[0] + " people in the city.", lx + 2 * dp, y + 10 * dp, plain);
            y += 16 * dp;
        }
        // Police and the army are set by the size of the city.
        plain.setTextSize(12.5f * dp);
        plain.setColor(MUTED);
        String force = known ? "Police and army are set by the city: " + fc[1] + " police"
                + (fc[2] > 0 ? " and " + fc[2] + " soldiers." : ", and no army on this map.")
                : "Police and army are set by the size of the city.";
        for (String line : wrap(force, lw - 4 * dp, plain)) {
            c.drawText(line, lx + 2 * dp, y + 10 * dp, plain);
            y += 16 * dp;
        }
        y += 10 * dp;
        y = segRow(c, CityConfig.OPT_RESERVES, "Reinforcements", lx, y, lw);
        plain.setTextSize(12.5f * dp);
        plain.setColor(MUTED);
        y -= 6 * dp;
        for (String line : wrap(CityConfig.RESERVE_INFO[config.v[CityConfig.OPT_RESERVES]], lw - 4 * dp, plain)) {
            c.drawText(line, lx + 2 * dp, y + 10 * dp, plain);
            y += 16 * dp;
        }
        y += 12 * dp;
        y = segRow(c, CityConfig.OPT_EXODUS, "Fleeing citizens", lx, y, lw);
        plain.setTextSize(12.5f * dp);
        plain.setColor(MUTED);
        y -= 6 * dp;
        String[] exodusInfo = {"Off: the people stay in town and sit it out (or don't).",
                "On foot: once the news breaks, most people walk out of the city for the country. They come home once the outbreak is over.",
                "Bus & train: once the news breaks, evacuation buses pick people up at stops around town and trains leave from the station (if there is one). They come home once it's over.",
                "All ways: once the news breaks, most people leave the city - by bus or train if a stop is near, on foot if not. They come home once it's over."};
        for (String line : wrap(exodusInfo[Math.max(0, Math.min(3, config.exodus))], lw - 4 * dp, plain)) {
            c.drawText(line, lx + 2 * dp, y + 10 * dp, plain);
            y += 16 * dp;
        }
        y += 12 * dp;
        c.restore();
        contentHeight = y + scroll - listTop;
        clampScroll();
        drawScrollHint(c, w, landscape ? lx - 8 * dp : 0);

        // ((11.5) The buttons on a bar of their own along the bottom.)
        fill.setColor(0xF20C0F13);
        c.drawRect(0, h - footer, w, h, fill);
        fill.setColor(0x14FFFFFF);
        c.drawRect(0, h - footer, w, h - footer + 1 * dp, fill);
        float bgap = 10 * dp, bw = Math.min(180 * dp, (w - 32 * dp - bgap * 2) / 3), bh = 50 * dp, by = h - footer + 11 * dp;
        float x = (w - bw * 3 - bgap * 2) / 2;
        button("Surprise me", A_RANDOM, false, x, by, x + bw, by + bh);
        button("City code", A_CODE, false, x + bw + bgap, by, x + bw * 2 + bgap, by + bh);
        button("Start  ›", A_START, true, x + bw * 2 + bgap * 2, by, x + bw * 3 + bgap * 2, by + bh);
        if (noteTime > 0 && note != null) {
            noteTime -= frameDt;
            fill.setColor(0xF0121418);
            c.drawRect(0, by - 30 * dp, w, by - 4 * dp, fill);
            text.setTextAlign(Paint.Align.CENTER);
            text.setTextSize(13 * dp);
            text.setColor(0xFFFFE27A);
            c.drawText(note, w / 2f, by - 12 * dp, text);
        }
    }

    /** The chosen map's description; returns the y below it. */
    private float drawInfo(Canvas c, int preset, float x, float y, float width) {
        plain.setTextAlign(Paint.Align.LEFT);
        plain.setTextSize(12.5f * dp);
        plain.setColor(0xFFB4BCC6);
        for (String line : wrap(CityConfig.PRESET_INFO[preset], width, plain)) {
            c.drawText(line, x, y, plain);
            y += 16 * dp;
        }
        return y;
    }

    private static final int[] MAP_COLORS = {0xFF8BD450, 0xFF6F8EC8, 0xFFE0A050, 0xFF9A9A90, 0xFF4CAF50, 0xFFB07050,
            0xFFD8C050, 0xFF50A0C0, 0xFFC050C0, 0xFF90B060, 0xFF3A9AD8, 0xFF2F6EA8, 0xFF48B0B0, 0xFF6A8098, 0xFF30B8D0,
            0xFFE8A040, 0xFF4E9A5A, 0xFF5A86B0, 0xFFC8C8C8, 0xFF3AA8D0, 0xFFE05A6A, 0xFFB898D0};

    private ArrayList<ArrayList<ArrayList<String>>> notesLines;
    private float notesW;

    private void drawNotes(Canvas c, int w, int h) {
        float top = header(c, w, h, "Patch Notes");
        listArea.set(0, top, w, h - 12 * dp);
        float side = 20 * dp, maxW = Math.min(w - side * 2, 640 * dp), x0 = (w - maxW) / 2;
        c.save();
        c.clipRect(listArea);
        float y = top + 10 * dp - scroll;
        plain.setTextSize(14.5f * dp);
        // ((12.0) Every note is wrapped into lines once for the screen's width, not again every frame - and only
        // what's on screen is drawn. With hundreds of notes the page crawled when scrolled.)
        if (notesLines == null || notesW != maxW) {
            notesW = maxW;
            notesLines = new ArrayList<ArrayList<ArrayList<String>>>();
            for (String[] entry : PatchNotes.NOTES) {
                ArrayList<ArrayList<String>> items = new ArrayList<ArrayList<String>>();
                for (int i = 1; i < entry.length; i++) items.add(wrap(entry[i], maxW - 18 * dp, plain));
                notesLines.add(items);
            }
        }
        for (int e = 0; e < PatchNotes.NOTES.length; e++) {
            String[] entry = PatchNotes.NOTES[e];
            if (y + 32 * dp > top - 40 * dp && y < h) {
                text.setTextAlign(Paint.Align.LEFT);
                text.setTextSize(17 * dp);
                text.setColor(ACCENT);
                c.drawText(entry[0], x0, y + 18 * dp, text);
            }
            y += 32 * dp;
            plain.setTextSize(14.5f * dp);
            plain.setTextAlign(Paint.Align.LEFT);
            for (ArrayList<String> lines : notesLines.get(e)) {
                float ih = lines.size() * 21 * dp;
                if (y + ih > top - 40 * dp && y < h) {
                    fill.setColor(ACCENT);
                    c.drawCircle(x0 + 5 * dp, y + 10 * dp, 2.5f * dp, fill);
                    plain.setColor(0xFFDADDE2);
                    for (int k = 0; k < lines.size(); k++) c.drawText(lines.get(k), x0 + 18 * dp, y + 15 * dp + k * 21 * dp, plain);
                }
                y += ih + 6 * dp;
            }
            y += 18 * dp;
        }
        c.restore();
        contentHeight = y + scroll - top;
        clampScroll();
        drawScrollHint(c, w);
    }

    /** Shows a "more below" arrow when the list can scroll further down. */
    private void drawScrollHint(Canvas c, int w) {
        drawScrollHint(c, w, 0);
    }

    private void drawScrollHint(Canvas c, int w, float left) {
        float max = contentHeight - listArea.height() + 12 * dp;
        if (scroll >= max - 2 * dp) return;
        // ((11.5) A fade over the last of the list and a small bobbing chevron, not a bar of text.)
        float cx = (left + w) / 2f, b = listArea.bottom;
        for (int k = 0; k < 8; k++) {
            fill.setColor(((int) (28 * (k + 1)) << 24) | 0x0C0F13);
            c.drawRect(left, b - (8 - k) * 5 * dp, w, b - (7 - k) * 5 * dp, fill);
        }
        float bob = (float) Math.sin(time * 4) * 2 * dp;
        stroke.setColor(ACCENT);
        stroke.setStrokeWidth(2.4f * dp);
        c.drawLine(cx - 8 * dp, b - 18 * dp + bob, cx, b - 10 * dp + bob, stroke);
        c.drawLine(cx, b - 10 * dp + bob, cx + 8 * dp, b - 18 * dp + bob, stroke);
    }

    private static ArrayList<String> wrap(String s, float maxW, Paint p) {
        ArrayList<String> lines = new ArrayList<String>();
        String line = "";
        for (String word : s.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && p.measureText(next) > maxW) {
                lines.add(line);
                line = word;
            } else {
                line = next;
            }
        }
        lines.add(line);
        return lines;
    }

    /** ((12.0) The mouse wheel turned v notches over a list: it scrolls it.) */
    void wheel(float v) {
        flingV = 0;
        scroll -= v * 60 * dp;
        clampScroll();
    }

    private void clampScroll() {
        float max = Math.max(0, contentHeight - listArea.height() + 12 * dp);
        scroll = Math.max(0, Math.min(max, scroll));
    }

    // ------------------------------------------------------------------ input

    boolean onTouch(MotionEvent ev) {
        float x = ev.getX(), y = ev.getY();
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressed = true;
                downX = x;
                downY = y;
                lastY = y;
                dragging = false;
                // (A touch stops a list that's still moving, and doesn't count as a tap if it did.)
                if (Math.abs(flingV) > 60 * dp) pressed = false;
                flingV = 0;
                lastMoveAt = System.nanoTime();
                break;
            case MotionEvent.ACTION_MOVE:
                if (screen == REPLAY && replayBar.contains(downX, downY)) {
                    scrubReplay(x);
                    break;
                }
                if (!dragging && Math.abs(y - downY) > 10 * dp && listArea.contains(downX, downY)
                        && (screen == SETUP || screen == SETTINGS || screen == NOTES || screen == RECORDS || screen == LOG
                        || screen == SAVES || screen == STATS)) dragging = true;
                if (dragging) {
                    scroll -= y - lastY;
                    clampScroll();
                    long now = System.nanoTime();
                    float dt = Math.max(0.004f, (now - lastMoveAt) / 1e9f);
                    float v = -(y - lastY) / dt;
                    flingV = flingV * 0.6f + v * 0.4f;
                    lastMoveAt = now;
                }
                lastY = y;
                break;
            case MotionEvent.ACTION_UP:
                // Ignore the lift of a finger that went down before the menu opened.
                if (pressed && !dragging) tap(x, y);
                // (Lifted after holding still: no fling.)
                if (!dragging || System.nanoTime() - lastMoveAt > 90000000L) flingV = 0;
                flingV = Math.max(-5000 * dp, Math.min(5000 * dp, flingV));
                dragging = false;
                pressed = false;
                break;
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                pressed = false;
                break;
        }
        return true;
    }

    private void tap(float x, float y) {
        if (screen == REPLAY && replayBar.contains(x, y)) {
            scrubReplay(x);
            return;
        }
        for (Button b : buttons) {
            if (b.r.contains(x, y)) {
                host.click();
                act(b.action);
                return;
            }
        }
        if (!listArea.contains(x, y)) return;
        if (screen == SETUP) {
            for (Seg sg : segs) {
                if (!sg.r.contains(x, y)) continue;
                if (config.get(sg.option) != sg.value) {
                    config.set(sg.option, sg.value);
                    // A different map or size is a different city.
                    if (sg.option == CityConfig.OPT_PRESET || sg.option == CityConfig.OPT_SIZE || sg.option == CityConfig.OPT_OUTSKIRTS) config.keepCity = false;
                }
                host.click();
                return;
            }
            return;
        }
        OptionSet opts = screen == SETUP ? config : screen == SETTINGS ? settings : null;
        if (opts == null) return;
        for (Row r : rows) {
            if (!r.r.contains(x, y)) continue;
            int count = opts.values(r.index).length;
            int dir = r.prev.contains(x, y) ? -1 : 1;
            opts.set(r.index, (opts.get(r.index) + dir + count) % count);
            host.click();
            if (screen == SETTINGS) host.settingsChanged();
            return;
        }
    }

    private void act(int action) {
        if (action >= A_LOG_FILTER) {
            logFilter = action - A_LOG_FILTER;
            scroll = 0;
            return;
        }
        if (action >= A_LOG_ROW) {
            java.util.ArrayList<Dispatch.Message> log = host.world().dispatch.log;
            int idx = log.size() - 1 - (action - A_LOG_ROW);
            if (idx >= 0 && idx < log.size()) {
                Dispatch.Message m = log.get(idx);
                host.continueGame();
                host.jumpTo(m.x, m.y);
            }
            return;
        }
        if (action >= A_LOAD_SLOT) {
            if (!host.loadSlot(action - A_LOAD_SLOT)) {
                note = "That save couldn't be loaded.";
                noteTime = 3;
            }
            return;
        }
        if (action >= A_SLOT) {
            int slot = action - A_SLOT;
            if (savingMode && host.slotInfo(slot) != null && !(armedSlot == slot && time - armedAt < 4)) {
                armedSlot = slot;
                armedAt = time;
                return;
            }
            armedSlot = -1;
            if (savingMode) {
                boolean ok = host.saveSlot(slot);
                host.world().say(ok ? "Saved to slot " + slot : "Could not save");
                host.continueGame();
            } else if (!host.loadSlot(slot)) {
                note = "That save couldn't be loaded.";
                noteTime = 3;
            }
            return;
        }
        switch (action) {
            case A_CONTINUE:
            case A_RESUME:
                host.continueGame();
                break;
            case A_NEW:
                open(SETUP);
                break;
            case A_SETTINGS:
                open(SETTINGS);
                break;
            case A_NOTES:
                open(NOTES);
                break;
            case A_QUIT:
                host.quit();
                break;
            case A_MAIN_MENU:
                host.toMainMenu();
                break;
            case A_BACK:
                back();
                break;
            case A_RANDOM:
                config.randomize(rnd);
                config.newSeed(rnd);
                break;
            case A_REROLL:
                config.newSeed(rnd);
                config.keepCity = false;
                break;
            case A_CODE:
                host.askCityCode(config.code());
                break;
            case A_START:
                // The seed is the one in the preview.
                config.keepCity = false;
                host.startGame(config);
                break;
            case A_SAVE:
                host.world().say(host.saveGame() ? "Game saved" : "Could not save the game");
                host.continueGame();
                break;
            case A_STATS:
                open(STATS);
                break;
            case A_REPLAY:
                screen = REPLAY;
                replayPos = 0;
                replayPlaying = true;
                break;
            case A_REPLAY_PLAY:
                if (replayPos >= 1) replayPos = 0;
                replayPlaying = !replayPlaying;
                break;
            case A_RECORDS:
                open(RECORDS);
                break;
            case A_SAVES:
                savingMode = true;
                open(SAVES);
                break;
            case A_LOAD:
                savingMode = false;
                open(SAVES);
                break;
            case A_LOG:
                open(LOG);
                break;
            case A_HUD:
                host.continueGame();
                host.hideHud();
                break;
            case A_SHOT:
                host.continueGame();
                host.screenshot();
                break;
            case A_COPY_CODE:
                if (host.copyText("City code", host.world().city.cfg.code())) copiedAt = time;
                break;
            case A_RESTART:
                if (time - restartArmedAt >= 4) {
                    restartArmedAt = time;
                    break;
                }
                restartArmedAt = -99;
                host.startGame(host.world().city.cfg.copy());
                break;
        }
    }
}
