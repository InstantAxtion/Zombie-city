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

        /** Save slots 1-6 (0 is the autosave): save, load, and a short description (null if empty). */
        boolean saveSlot(int slot);

        boolean loadSlot(int slot);

        String slotInfo(int slot);

        /** (12.0) Deletes a saved game; false if it couldn't. */
        boolean deleteSlot(int slot);

        /** Moves the camera to a spot. */
        void jumpTo(float x, float y);

        /** ((13.3) Back to the game, the camera following someone (or the car or truck they're in). */
        void watch(Entity e);

        /** Hides the buttons and panels until the next tap. */
        void hideHud();

        /** Asks the player to type a city code (shows the current one to copy or edit). */
        void askCityCode(String current);

        /** ((19.0) The Sound Lab: plays a sound as it is. */
        void preview(int sfx);

        /** (12.0) Puts text on the clipboard; false if it couldn't. */
        boolean copyText(String label, String text);
    }

    static final int NONE = 0, MAIN = 1, SETUP = 2, SETTINGS = 3, NOTES = 4, PAUSE = 5, STATS = 6, RECORDS = 8,
            SAVES = 9, LOG = 10, REPLAY = 11, TOP = 12, REPORT = 13, CAMPAIGN = 14, SOUNDS = 15;
    /** ((15.1) How many save slots there are (the autosave besides): six, up from three.) */
    static final int SLOTS = 6;

    /** The save slot screen: saving (from the pause menu) or loading (from the main menu). */
    private boolean savingMode;
    Records records;

    private static final int A_CONTINUE = 0, A_NEW = 1, A_SETTINGS = 2, A_NOTES = 3, A_QUIT = 4, A_RESUME = 5,
            A_MAIN_MENU = 6, A_BACK = 7, A_START = 8, A_RANDOM = 9, A_SAVE = 10, A_STATS = 11, A_HOW_TO = 12,
            A_TUT_NEXT = 13, A_TUT_PREV = 14, A_TUT_DONE = 15, A_CODE = 16, A_REROLL = 17, A_SHOT = 18, A_RECORDS = 19, A_SAVES = 20, A_LOAD = 21, A_LOG = 22, A_HUD = 23,
            A_REPLAY = 24, A_REPLAY_PLAY = 25, A_RESTART = 26, A_COPY_CODE = 27, A_SLOT = 100, A_RESET_SETTINGS = 28, A_LOAD_SLOT = 150, A_DELETE_SLOT = 170, A_LOG_ROW = 200, A_LOG_FILTER = 1000,
            A_TOP = 29, A_TOP_ROW = 600, A_REPORT = 30, A_CAMPAIGN = 31, A_RETRY = 32, A_NEXT = 33, A_MISSION = 5000,
            A_SOUNDS = 34, A_RESET_STATS = 35, A_SFX = 6000, A_LOOP = 7000, A_PERK = 8000;
    /** ((19.0) The ambience bed being tried in the Sound Lab, or -1. */
    int previewLoop = -1;
    /** ((19.6) The sound last played in the Sound Lab, and when: lit up, with a meter along the bottom. */
    private int previewId = -1;
    private float previewAt = -99;
    /** ((12.0) Which messages the radio log shows: 0 all, else one kind (Dispatch.WHO_* + 1).) */
    private int logFilter;

    // ((11.5) No How to Play pages any more.)

    private static final class Button {
        final RectF r = new RectF();
        String label;
        int action;
        boolean primary, invisible;
        /** ((12.0) The size to write it at, if one is set for its whole row; otherwise 0.) */
        float size;
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
        if (s == SETUP || s == SETTINGS || s == NOTES || s == STATS || s == RECORDS || s == SAVES || s == LOG || s == TOP || s == REPORT || s == CAMPAIGN || s == SOUNDS) {
            if (screen == MAIN || screen == PAUSE || screen == NONE) returnTo = screen;
        } else {
            returnTo = MAIN;
        }
        screen = s;
        scroll = 0;
        if (s == NOTES) settings.markNotesRead();
        if (s == TOP) topAt = -99;
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
            case TOP: drawTop(c, w, h); break;
            case REPORT: drawReport(c, w, h); break;
            case CAMPAIGN: drawCampaign(c, w, h); break;
            case SOUNDS: drawSounds(c, w, h); break;
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
        btn.size = 0;
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
        text.setTextSize(b.size > 0 ? b.size : labelSize(b));
        text.setColor(b.primary ? ACCENT_INK : INK);
        c.drawText(b.label, b.r.centerX(), b.r.centerY() + text.getTextSize() * 0.36f, text);
    }

    /** The size a button's label fits at, on its own. */
    private float labelSize(Button b) {
        text.setTextSize(Math.min(16.5f * dp, b.r.height() * 0.36f));
        float fit = text.measureText(b.label);
        if (fit > b.r.width() - 18 * dp) return text.getTextSize() * (b.r.width() - 18 * dp) / fit;
        return text.getTextSize();
    }

    /** ((12.0) The buttons made since index `from` all written at one size, the one the longest label fits at.) */
    private void sameSize(int from) {
        float size = Float.MAX_VALUE;
        for (int i = from; i < buttons.size(); i++) size = Math.min(size, labelSize(buttons.get(i)));
        for (int i = from; i < buttons.size(); i++) buttons.get(i).size = size;
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
        String tag = "S A N D B O X   ·   S U R V I V E   T H E   O U T B R E A K";
        // ((12.0) Smaller to fit a narrow screen: it ran off both sides.)
        float tagW = plain.measureText(tag);
        if (tagW > maxW) plain.setTextSize(plain.getTextSize() * maxW / tagW);
        c.drawText(tag, cx, uy + plain.getTextSize() * 1.9f, plain);
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
        boolean slots = false;
        for (int s = 1; s <= SLOTS; s++) slots |= host.slotInfo(s) != null;
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
            // ((12.0) Which game that is: the city and how long it's been going.)
            String which = null;
            if (host.hasGame() && host.world() != null) which = host.world().city.name + "  -  " + World.clock((int) host.world().time) + " played";
            else {
                String info = host.slotInfo(0);
                if (info != null) {
                    String[] parts = info.split("\n");
                    which = parts[0] + (parts.length > 1 ? "  -  " + parts[1].split(",")[0] : "");
                }
            }
            if (which != null) {
                plain.setTextSize(12 * dp);
                plain.setTextAlign(Paint.Align.CENTER);
                plain.setColor(0xCCFFFFFF);
                c.drawText(fit(plain, which, colW), x0 + colW / 2, y + 6 * dp, plain);
                y += 18 * dp;
            }
            button("New Game", A_NEW, false, x0, y, x0 + colW, y + tileH);
        } else button("New Game", A_NEW, true, x0, y, x0 + colW, y + bigH);
        y += (game ? tileH : bigH) + gap;
        // ((18.0) The campaign: missions to win as the commander, beside the sandbox's New Game.)
        button("Campaign  " + Campaign.stars(0).charAt(0) + " " + (records != null ? records.totalStars() : 0) + "/" + Campaign.MISSIONS.length * 3,
                A_CAMPAIGN, false, x0, y, x0 + colW, y + tileH);
        y += tileH + gap * 1.6f;
        // The rest, two to a row.
        java.util.ArrayList<Object[]> tiles = new java.util.ArrayList<Object[]>();
        if (slots) tiles.add(new Object[]{"Load Game", A_LOAD});
        tiles.add(new Object[]{"Settings", A_SETTINGS});
        tiles.add(new Object[]{"Records", A_RECORDS});
        tiles.add(new Object[]{"Sound Lab", A_SOUNDS});
        tiles.add(new Object[]{settings.hasUnreadNotes() ? "What's New  •" : "What's New", A_NOTES});
        tiles.add(new Object[]{"Quit", A_QUIT});
        float tw = (colW - gap) / 2;
        // ((12.0) All the tiles at one size: "What's New" was smaller than the rest.)
        int firstTile = buttons.size();
        for (int k = 0; k < tiles.size(); k++) {
            boolean lone = k == tiles.size() - 1 && tiles.size() % 2 == 1;
            float l = lone ? x0 : x0 + (k % 2) * (tw + gap), r = lone ? x0 + colW : l + tw;
            button((String) tiles.get(k)[0], (Integer) tiles.get(k)[1], false, l, y, r, y + tileH);
            if (k % 2 == 1 || lone) y += tileH + gap;
        }
        sameSize(firstTile);
        plain.setTextSize(12 * dp);
        plain.setTextAlign(Paint.Align.RIGHT);
        plain.setColor(0x88FFFFFF);
        c.drawText("v" + PatchNotes.VERSION, w - 14 * dp, h - 14 * dp, plain);
    }

    private void drawPause(Canvas c, int w, int h) {
        backdrop(c, w, h, 0.6f);
        // ((12.0) Restart: the same city from the start, with a second tap to be sure.)
        boolean sure = time - restartArmedAt < 4;
        String[] labels = {"Resume", "Top Fighters", "Save / Load", "Radio Log", "Stats", "Report", sure ? "Tap again to restart" : "Restart City",
                "New Game", "Settings", "Main Menu"};
        int[] actions = {A_RESUME, A_TOP, A_SAVES, A_LOG, A_STATS, A_REPORT, A_RESTART, A_NEW, A_SETTINGS, A_MAIN_MENU};
        int cols = w > h ? 2 : 1, rowsN = (labels.length + cols - 1) / cols;
        float gap = 7 * dp, bh = Math.min(46 * dp, (h - 138 * dp - gap * (rowsN - 1)) / rowsN);
        float colW = Math.min(300 * dp, (w - 60 * dp) / cols);
        float pw = colW * cols + gap * (cols - 1) + 40 * dp, ph = 94 * dp + bh * rowsN + gap * (rowsN - 1) + 20 * dp;
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
        // ((12.0) How it stands, at a glance.)
        plain.setColor(MUTED);
        String now = World.clock((int) world.time) + " played  -  " + world.humans + (world.humans == 1 ? " person" : " people") + ", "
                + world.zombies + (world.zombies == 1 ? " zombie" : " zombies");
        c.drawText(fit(plain, now, tmp.width() - 24 * dp), w / 2f, tmp.top + 76 * dp, plain);
        float top = tmp.top + 90 * dp;
        for (int i = 0; i < labels.length; i++) {
            int col = cols == 2 ? i % 2 : 0, row = cols == 2 ? i / 2 : i;
            float bx = tmp.left + 20 * dp + col * (colW + gap), by = top + row * (bh + gap);
            button(labels[i], actions[i], i == 0, bx, by, bx + colW, by + bh);
        }
    }

    /** The six save slots (and the autosave when loading). */
    private void drawSaves(Canvas c, int w, int h) {
        float top = header(c, w, h, savingMode ? "Save Game" : "Load Game");
        listArea.set(0, top, w, h - 12 * dp);
        float side = 20 * dp, maxW = Math.min(w - side * 2, 560 * dp), x0 = (w - maxW) / 2;
        float y = top + 10 * dp - scroll;
        int first = savingMode ? 1 : 0;
        for (int slot = first; slot <= SLOTS; slot++) {
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
            for (int k = 0; k < lines.length && k < 3; k++)
                c.drawText(fit(plain, lines[k], maxW - (info != null ? 244 : savingMode ? 150 : 28) * dp), x0 + 14 * dp, y + 46 * dp + k * 15 * dp, plain);
            boolean can = savingMode || info != null;
            // ((12.0) Overwriting a save asks for a second tap; and a save can be loaded from here too, not only
            // from the main menu - the pause menu's "Save / Load" only saved.)
            boolean confirm = savingMode && info != null && armedSlot == slot && time - armedAt < 4;
            if (can) button(savingMode ? (info == null ? "Save" : confirm ? "Sure?" : "Overwrite") : "Load", A_SLOT + slot, true,
                    x0 + maxW - 120 * dp, y + 22 * dp, x0 + maxW - 14 * dp, y + 64 * dp);
            if (savingMode && info != null)
                button("Load", A_LOAD_SLOT + slot, false, x0 + maxW - 214 * dp, y + 22 * dp, x0 + maxW - 128 * dp, y + 64 * dp);
            // ((12.0) And a save can be deleted, with a second tap to be sure.)
            if (!savingMode && info != null) {
                boolean sure = deleteArmed == slot && time - deleteArmedAt < 4;
                button(sure ? "Sure?" : "Delete", A_DELETE_SLOT + slot, false, x0 + maxW - 214 * dp, y + 22 * dp, x0 + maxW - 128 * dp, y + 64 * dp);
            }
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

    /** ((12.0) When Reset to defaults was first tapped.) */
    private float resetArmedAt = -99, statsArmedAt = -99;

    /** ((12.0) When Restart City was first tapped.) */
    private float restartArmedAt = -99;

    /** ((12.0) The slot waiting for a second tap to be deleted, and when it was first tapped.) */
    private int deleteArmed = -1;
    private float deleteArmedAt;

    /** ((12.0) The slot waiting for a second tap to be overwritten, and when it was first tapped.) */
    private int armedSlot = -1;
    private float armedAt;

    private final java.util.WeakHashMap<Dispatch.Message, ArrayList<String>> logLines = new java.util.WeakHashMap<Dispatch.Message, ArrayList<String>>();
    private float logW;

    /** ((13.3) The Top Fighters list, looked up again once a second rather than every frame. */
    private java.util.ArrayList<Entity> topList = new java.util.ArrayList<Entity>();
    private float topAt = -99;

    /**
     * ((17.5) The after-action report: how it ended (or how it stands), each side's losses, the fighting strength over
     * time, where the fighting was on a map of the city, and the best of the city's fighters, living and fallen.
     */
    private void drawReport(Canvas c, int w, int h) {
        World world = host.world();
        Campaign.Mission mission = world.mission();
        float top = header(c, w, h, mission != null ? "Mission Report" : "After-Action Report");
        float side = 16 * dp, maxW = Math.min(w - side * 2, 760 * dp), x0 = (w - maxW) / 2;
        // ((18.0) A mission's report has its buttons along the bottom: try again, the next mission, the campaign.)
        float barH = mission != null ? 70 * dp : 0;
        listArea.set(0, top, w, h - 8 * dp - barH);
        if (mission != null) missionButtons(world, w, h, barH, maxW, x0);
        c.save();
        c.clipRect(listArea);
        float y = top + 8 * dp - scroll;
        if (mission != null) y = missionSummary(c, world, mission, w, y, maxW, x0);
        // (A mission says how it ended above.)
        if (mission == null) {
            // How it ended.
            text.setTextAlign(Paint.Align.CENTER);
            text.setTextSize(22 * dp);
            boolean won = world.warResult == 1, lost = world.warResult == 2;
            text.setColor(won ? 0xFF8BE05A : lost ? 0xFFFF6A5A : INK);
            String title = world.warResult != 0 && world.warBanner != null ? world.warBanner
                    : world.outbreak ? "THE FIGHTING GOES ON" : "NOTHING YET";
            c.drawText(fit(text, title, maxW), w / 2f, y + 26 * dp, text);
            y += 36 * dp;
            plain.setTextAlign(Paint.Align.CENTER);
            plain.setTextSize(13 * dp);
            plain.setColor(MUTED);
            String sub = world.warResult != 0 && world.warSub != null ? world.warSub
                    : World.clock((int) world.outbreakTime) + " of fighting so far.";
            for (String line : wrap(sub, maxW, plain)) {
                c.drawText(line, w / 2f, y + 12 * dp, plain);
                y += 18 * dp;
            }
            y += 8 * dp;
        }
        // Each side's losses, side by side.
        String other = "The dead";
        String[][] ours = {
                {"Police officers lost", String.valueOf(world.copsLost)},
                {"Soldiers lost", String.valueOf(world.soldiersLost)},
                {"Civilians lost", String.valueOf(world.civiliansLost)},
                {"People turned", String.valueOf(world.turned)},
                {world.mission() != null ? "Score (" + Director.NAMES[world.difficulty] + ")" : "Score", String.valueOf(world.score())},
        };
        String[][] theirs = {
                {"Destroyed", String.valueOf(world.zombiesKilled)},
                {"Most at once", String.valueOf(world.peakZombies)},
                {"Biggest horde", String.valueOf(world.biggestHorde)},
                {"Still walking", String.valueOf(world.zombieCount())},
                {"Hordes from outside", String.valueOf(world.director.hordes + world.director.sieges + world.director.stragglers)},
                {"Mines set off", String.valueOf(world.minesBlown)},
                {"Alphas killed", world.alphasKilled + " of " + world.alphas},
                {"Died in the wire", String.valueOf(world.defences.wireKills)},
        };
        boolean wide = maxW > 520 * dp;
        float colW = wide ? (maxW - 12 * dp) / 2 : maxW;
        float yA = y, yB = wide ? y : y + 30 * dp + ours.length * 22 * dp + 16 * dp;
        y = Math.max(table(c, x0, yA, colW, "The city", 0xFF6FA8FF, ours), table(c, wide ? x0 + colW + 12 * dp : x0, yB, colW, other, 0xFFFF7A5A, theirs));
        y += 14 * dp;
        // Strength over time.
        RectF chart = new RectF(x0, y, x0 + maxW, y + 170 * dp);
        fill.setColor(0xFF1C1F24);
        c.drawRoundRect(chart, 10 * dp, 10 * dp, fill);
        RectF plot = new RectF(chart.left + 14 * dp, chart.top + 28 * dp, chart.right - 14 * dp, chart.bottom - 20 * dp);
        int n = world.histCount, max = 10;
        for (int i = 0; i < n; i++) max = Math.max(max, Math.max(world.histArmed[i], world.histZombies[i]));
        text.setTextAlign(Paint.Align.LEFT);
        text.setTextSize(12 * dp);
        String[] keys = {"Police & army", "Zombies"};
        int[] cols = {0xFFE8C547, 0xFFFF7A5A};
        float kx = chart.left + 14 * dp;
        for (int k = 0; k < 2; k++) {
            fill.setColor(cols[k]);
            c.drawCircle(kx + 4 * dp, chart.top + 15 * dp, 4 * dp, fill);
            text.setColor(INK);
            c.drawText(keys[k], kx + 12 * dp, chart.top + 19 * dp, text);
            kx += 24 * dp + text.measureText(keys[k]);
        }
        if (n >= 2) {
            int[][] series = {world.histArmed, world.histZombies};
            for (int sI = 0; sI < 2; sI++) {
                stroke.setColor(cols[sI]);
                stroke.setStrokeWidth(2.2f * dp);
                for (int i = 1; i < n; i++) {
                    float ax = plot.left + plot.width() * (i - 1) / (n - 1), bx = plot.left + plot.width() * i / (n - 1);
                    float ay = plot.bottom - plot.height() * series[sI][i - 1] / (float) max, by = plot.bottom - plot.height() * series[sI][i] / (float) max;
                    c.drawLine(ax, ay, bx, by, stroke);
                }
            }
            plain.setTextAlign(Paint.Align.RIGHT);
            plain.setTextSize(11 * dp);
            plain.setColor(MUTED);
            c.drawText(World.clock((int) (n * world.statStep)), plot.right, chart.bottom - 6 * dp, plain);
        }
        y = chart.bottom + 14 * dp;
        // Where the fighting was.
        City city = world.city;
        float mw = maxW, mh = mw * city.worldH() / city.worldW();
        if (mh > 420 * dp) {
            mh = 420 * dp;
            mw = mh * city.worldW() / city.worldH();
        }
        RectF map = new RectF(x0 + (maxW - mw) / 2, y, x0 + (maxW + mw) / 2, y + mh);
        city.drawMap(c, 0, 0, -1, -1, map, bmpPaint);
        fill.setColor(0x55000000);
        c.drawRect(map, fill);
        float[] bm = world.battleMap;
        if (bm != null) {
            int hw = world.heatW();
            float most = 0;
            for (float f : bm) most = Math.max(most, f);
            for (int i = 0; i < bm.length && most > 0; i++) {
                if (bm[i] <= 0) continue;
                float f = (float) Math.sqrt(bm[i] / most);
                float cx = map.left + ((i % hw) + 0.5f) * World.HEAT_CELL / city.worldW() * map.width();
                float cy = map.top + ((i / hw) + 0.5f) * World.HEAT_CELL / city.worldH() * map.height();
                float rr = World.HEAT_CELL / city.worldW() * map.width() * (0.3f + 0.6f * f);
                fill.setColor((((int) (40 + 150 * f)) << 24) | 0xFF3A20);
                c.drawCircle(cx, cy, rr, fill);
            }
        }
        plain.setTextAlign(Paint.Align.LEFT);
        plain.setTextSize(12 * dp);
        plain.setColor(INK);
        c.drawText("Where the fighting was", map.left + 8 * dp, map.top + 18 * dp, plain);
        y = map.bottom + 18 * dp;
        // The best of the city's fighters.
        text.setTextAlign(Paint.Align.LEFT);
        text.setTextSize(15 * dp);
        text.setColor(INK);
        c.drawText("Best of the city's fighters", x0, y + 4 * dp, text);
        y += 14 * dp;
        java.util.ArrayList<int[]> heroes = world.heroes(5);
        for (int i = 0; i < heroes.size(); i++) {
            int[] hr = heroes.get(i);
            tmp.set(x0, y, x0 + maxW, y + 40 * dp);
            fill.setColor(i == 0 ? 0xE02A2E22 : 0xE0202328);
            c.drawRoundRect(tmp, 8 * dp, 8 * dp, fill);
            text.setTextSize(14 * dp);
            text.setColor(i == 0 ? 0xFFFFD54A : INK);
            String who = (i + 1) + ".  " + Names.person(hr[0]) + "  -  " + (hr[1] >= 0 && hr[1] < Entity.NAMES.length ? Entity.NAMES[hr[1]] : "");
            c.drawText(fit(text, who, maxW - 150 * dp), x0 + 10 * dp, y + 25 * dp, text);
            text.setTextAlign(Paint.Align.RIGHT);
            text.setColor(hr[4] == 1 ? ACCENT : MUTED);
            c.drawText(hr[3] + (hr[3] == 1 ? " kill" : " kills") + (hr[4] == 1 ? "" : "  (fallen)"), x0 + maxW - 10 * dp, y + 25 * dp, text);
            text.setTextAlign(Paint.Align.LEFT);
            y += 46 * dp;
        }
        if (heroes.isEmpty()) {
            plain.setColor(MUTED);
            c.drawText("No kills yet.", x0, y + 16 * dp, plain);
            y += 24 * dp;
        }
        c.restore();
        contentHeight = y + scroll - top + 10 * dp;
        clampScroll();
        drawScrollHint(c, w);
    }

    /** ((18.0) A mission's result at the top of its report: how it ended, the stars, and each objective. */
    private float missionSummary(Canvas c, World world, Campaign.Mission m, int w, float y, float maxW, float x0) {
        int res = world.missionResult;
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(13 * dp);
        text.setColor(ACCENT);
        c.drawText((world.city.cfg.mission + 1) + ".  " + m.title.toUpperCase(java.util.Locale.US), w / 2f, y + 14 * dp, text);
        y += 22 * dp;
        text.setTextSize(24 * dp);
        text.setColor(res == 1 ? 0xFF8BE05A : res == 2 ? 0xFFFF6A5A : INK);
        c.drawText(res == 1 ? "MISSION COMPLETE" : res == 2 ? "MISSION FAILED" : "MISSION IN PROGRESS", w / 2f, y + 26 * dp, text);
        y += 36 * dp;
        if (res == 1) {
            text.setTextSize(30 * dp);
            text.setColor(0xFFFFD54A);
            c.drawText(Campaign.stars(world.missionStars), w / 2f, y + 28 * dp, text);
            y += 38 * dp;
        } else if (res == 2 && world.missionWhy != null) {
            plain.setTextAlign(Paint.Align.CENTER);
            plain.setTextSize(13 * dp);
            plain.setColor(MUTED);
            c.drawText(world.missionWhy, w / 2f, y + 12 * dp, plain);
            y += 22 * dp;
        }
        String[] goals = {Campaign.goalText(world), "Civilians lost: no more than " + m.starCivilians + "  (" + world.civiliansLost + ")",
                "Officers and soldiers lost: no more than " + m.starLosses + "  (" + Campaign.lost(world) + ")"};
        boolean[] met = {res == 1, Campaign.starCivilians(world), Campaign.starLosses(world)};
        tmp.set(x0, y, x0 + maxW, y + 16 * dp + goals.length * 24 * dp + 22 * dp);
        fill.setColor(0xFF1C1F24);
        c.drawRoundRect(tmp, 10 * dp, 10 * dp, fill);
        float ry = y + 12 * dp;
        plain.setTextAlign(Paint.Align.LEFT);
        plain.setTextSize(13 * dp);
        for (int i = 0; i < goals.length; i++) {
            plain.setColor(met[i] ? 0xFF8BE05A : res == 0 ? MUTED : 0xFFFF8A6A);
            String mark = i == 0 ? (met[i] ? "✔ " : res == 2 ? "✘ " : "○ ") : (met[i] ? "★ " : "☆ ");
            c.drawText(fit(plain, mark + goals[i], maxW - 24 * dp), x0 + 12 * dp, ry + 14 * dp, plain);
            ry += 24 * dp;
        }
        plain.setColor(MUTED);
        c.drawText(fit(plain, "Spent $" + world.spent + " of $" + (m.budget + Campaign.earned(world)) + "   Time "
                + World.clock((int) Campaign.missionTime(world)) + " of " + World.clock(m.limit), maxW - 24 * dp), x0 + 12 * dp, ry + 14 * dp, plain);
        return tmp.bottom + 16 * dp;
    }

    private void missionButtons(World world, int w, int h, float barH, float maxW, float x0) {
        float top = h - barH + 8 * dp, bh = barH - 22 * dp, gap = 10 * dp;
        boolean next = world.missionResult == 1 && Campaign.get(world.city.cfg.mission + 1) != null;
        int n = next ? 3 : 2;
        float bw = (maxW - gap * (n - 1)) / n, x = x0;
        int first = buttons.size();
        button("Campaign", A_CAMPAIGN, false, x, top, x + bw, top + bh);
        x += bw + gap;
        button(world.missionResult == 1 ? "Play again" : "Retry", A_RETRY, !next && world.missionResult != 0, x, top, x + bw, top + bh);
        x += bw + gap;
        if (next) button("Next mission", A_NEXT, true, x, top, x + bw, top + bh);
        sameSize(first);
    }

    /**
     * ((18.0) The campaign: each mission as a card, with its stars, the open ones to tap and play; the veterans on the
     * roster.
     */
    private void drawCampaign(Canvas c, int w, int h) {
        float top = header(c, w, h, "Campaign");
        float side = 16 * dp, maxW = Math.min(w - side * 2, 640 * dp), x0 = (w - maxW) / 2;
        listArea.set(0, top, w, h - 8 * dp);
        c.save();
        c.clipRect(listArea);
        float y = top + 4 * dp - scroll;
        plain.setTextAlign(Paint.Align.LEFT);
        plain.setTextSize(13 * dp);
        plain.setColor(MUTED);
        for (String line : wrap("You're the commander. You start with the city's own police and soldiers and a budget: more units, vehicles and air support "
                + "cost money, which comes in as funding and as a reward for doing well. Units come in from the stations and bases and go where you "
                + "send them; use Orders to move them. Win a mission to open the next. Officers and soldiers who make rank come back as veterans.", maxW, plain)) {
            c.drawText(line, x0, y + 13 * dp, plain);
            y += 18 * dp;
        }
        y += 6 * dp;
        int vets = records == null ? 0 : Campaign.rosterSize(records.veterans());
        text.setTextAlign(Paint.Align.LEFT);
        text.setTextSize(14 * dp);
        text.setColor(0xFFFFD54A);
        c.drawText("★ " + (records == null ? 0 : records.totalStars()) + " / " + Campaign.MISSIONS.length * 3
                + "      Veterans on the roster: " + vets, x0, y + 14 * dp, text);
        y += 28 * dp;
        // ((22.0) The commander's perks: a tree of three branches, a point to spend for every four stars. Tap one that's
        // open to take it.)
        int allStars = records == null ? 0 : records.totalStars();
        int mask = records == null ? 0 : records.perkMask();
        int spare = Perks.spare(mask, allStars);
        text.setTextSize(12.5f * dp);
        text.setColor(0xFFC8CCD2);
        c.drawText("PERKS  " + Perks.count(mask) + " / " + Perks.COUNT + (spare > 0 ? "      " + spare + (spare == 1 ? " point" : " points") + " to spend: tap a perk"
                : "      a point every " + Perks.STARS_PER_POINT + " stars"), x0, y + 12 * dp, text);
        y += 22 * dp;
        for (int br = 0; br < Perks.BRANCHES.length; br++) {
            text.setTextSize(11.5f * dp);
            text.setColor(ACCENT);
            c.drawText(Perks.BRANCH_NAMES[br].toUpperCase(java.util.Locale.US), x0, y + 12 * dp, text);
            y += 18 * dp;
            for (int k : Perks.BRANCHES[br]) {
                boolean got = Perks.taken(mask, k), canTake = Perks.open(mask, allStars, k);
                tmp.set(x0, y, x0 + maxW, y + 40 * dp);
                fill.setColor(got ? 0xFF1E2A1C : canTake ? 0xFF22303F : 0xFF1C1F24);
                c.drawRoundRect(tmp, 8 * dp, 8 * dp, fill);
                if (canTake) {
                    stroke.setColor(ACCENT);
                    stroke.setStrokeWidth(1.5f * dp);
                    c.drawRoundRect(tmp, 8 * dp, 8 * dp, stroke);
                    button("", A_PERK + k, false, tmp.left, tmp.top, tmp.right, tmp.bottom).invisible = true;
                }
                text.setTextSize(13 * dp);
                text.setColor(got ? 0xFF9BE07A : canTake ? 0xFFFFFFFF : 0xFF8A8F98);
                c.drawText((got ? "\u2713 " : canTake ? "+ " : "\u2022 ") + Perks.NAMES[k], x0 + 10 * dp, y + 16 * dp, text);
                plain.setTextSize(11.5f * dp);
                plain.setColor(got ? 0xFFC8CCD2 : MUTED);
                c.drawText(fit(plain, Perks.INFO[k], maxW - 20 * dp), x0 + 10 * dp, y + 32 * dp, plain);
                y += 46 * dp;
            }
        }
        y += 8 * dp;
        for (int i = 0; i < Campaign.MISSIONS.length; i++) {
            Campaign.Mission m = Campaign.MISSIONS[i];
            boolean open = records == null || records.unlocked(i);
            int stars = records == null ? 0 : records.missionStars(i);
            plain.setTextSize(12.5f * dp);
            ArrayList<String> lines = wrap(open ? m.brief : "Win the mission before to open this one.", maxW - 84 * dp, plain);
            float ch = 50 * dp + lines.size() * 17 * dp;
            tmp.set(x0, y, x0 + maxW, y + ch);
            fill.setColor(open ? CARD_HI : 0xFF15171A);
            c.drawRoundRect(tmp, 12 * dp, 12 * dp, fill);
            stroke.setStrokeWidth(1 * dp);
            stroke.setColor(stars > 0 ? 0x80FFD54A : EDGE);
            c.drawRoundRect(tmp, 12 * dp, 12 * dp, stroke);
            fill.setColor(open ? ACCENT : 0xFF3A3D42);
            c.drawCircle(x0 + 28 * dp, y + 28 * dp, 17 * dp, fill);
            text.setTextAlign(Paint.Align.CENTER);
            text.setTextSize(16 * dp);
            text.setColor(open ? ACCENT_INK : MUTED);
            c.drawText(open ? String.valueOf(i + 1) : "•", x0 + 28 * dp, y + 34 * dp, text);
            text.setTextAlign(Paint.Align.LEFT);
            text.setTextSize(16 * dp);
            text.setColor(open ? INK : MUTED);
            c.drawText(m.title, x0 + 56 * dp, y + 24 * dp, text);
            plain.setTextSize(11.5f * dp);
            plain.setColor(ACCENT);
            c.drawText(Campaign.GOAL_NAMES[m.goal] + "   " + World.clock(m.limit) + "   $" + m.budget, x0 + 56 * dp, y + 40 * dp, plain);
            text.setTextAlign(Paint.Align.RIGHT);
            text.setTextSize(18 * dp);
            text.setColor(stars > 0 ? 0xFFFFD54A : 0x55FFFFFF);
            c.drawText(Campaign.stars(stars), x0 + maxW - 12 * dp, y + 26 * dp, text);
            plain.setTextSize(12.5f * dp);
            plain.setColor(open ? MUTED : 0x66FFFFFF);
            float ly = y + 58 * dp;
            for (String line : lines) {
                c.drawText(line, x0 + 56 * dp, ly, plain);
                ly += 17 * dp;
            }
            if (open && tmp.bottom > listArea.top && tmp.top < listArea.bottom)
                button("", A_MISSION + i, false, tmp.left, Math.max(tmp.top, listArea.top), tmp.right, Math.min(tmp.bottom, listArea.bottom)).invisible = true;
            y += ch + 10 * dp;
        }
        text.setTextAlign(Paint.Align.LEFT);
        c.restore();
        contentHeight = y + scroll - top + 10 * dp;
        clampScroll();
        drawScrollHint(c, w);
    }

    /** ((19.0) The Sound Lab: every sound to try, by group, and the ambience beds to switch on one at a time. */
    private void drawSounds(Canvas c, int w, int h) {
        float top = header(c, w, h, "Sound Lab");
        float side = 16 * dp, maxW = Math.min(w - side * 2, 760 * dp), x0 = (w - maxW) / 2;
        listArea.set(0, top, w, h - 8 * dp);
        c.save();
        c.clipRect(listArea);
        float y = top + 4 * dp - scroll;
        plain.setTextAlign(Paint.Align.LEFT);
        plain.setTextSize(13 * dp);
        plain.setColor(MUTED);
        for (String line : wrap("Every sound in the game, made on the phone as it runs. Tap one to hear it. The ambience beds play under the menu "
                + "while they're switched on.", maxW, plain)) {
            c.drawText(line, x0, y + 13 * dp, plain);
            y += 18 * dp;
        }
        y += 6 * dp;
        String[] groups = {"Guns", "Blasts", "Voices", "Vehicles", "The world", "Radio", "Menus & missions"};
        int cols = maxW > 520 * dp ? 4 : 3;
        float gap = 8 * dp, bw = (maxW - gap * (cols - 1)) / cols, bh = 40 * dp;
        // The ambience beds first.
        y = section(c, "AMBIENCE", x0, y + 8 * dp);
        int first = buttons.size();
        for (int l = 0; l < Sfx.LOOPS; l++) {
            int col = l % cols;
            float bx = x0 + col * (bw + gap);
            if (col == 0 && l > 0) y += bh + gap;
            if (y + bh > listArea.top && y < listArea.bottom)
                button((previewLoop == l ? "■ " : "▶ ") + Sfx.LOOP_NAMES[l], A_LOOP + l, previewLoop == l, bx, y, bx + bw, y + bh);
        }
        y += bh + gap;
        for (int g = 0; g < Sfx.BUSES; g++) {
            y = section(c, groups[g].toUpperCase(java.util.Locale.US), x0, y + 8 * dp);
            int k = 0;
            for (int id = 0; id < Sfx.COUNT; id++) {
                if (Sfx.BUS[id] != g || id == 21) continue;
                int col = k % cols;
                float bx = x0 + col * (bw + gap);
                if (col == 0 && k > 0) y += bh + gap;
                boolean lit = id == previewId && time - previewAt < 0.9f;
                if (y + bh > listArea.top && y < listArea.bottom) button((lit ? "\u25B6 " : "") + Sfx.NAMES[id], A_SFX + id, lit, bx, y, bx + bw, y + bh);
                k++;
            }
            y += bh + gap;
        }
        sameSize(first);
        c.restore();
        contentHeight = y + scroll - top + 10 * dp + 70 * dp;
        clampScroll();
        drawScrollHint(c, w);
        // What's playing: its name and a meter dancing along the bottom, fading as it ends.
        float age = time - previewAt;
        boolean bed = previewLoop >= 0;
        if ((previewId >= 0 && age < 1.6f) || bed) {
            float a = bed && (previewId < 0 || age >= 1.6f) ? 1 : Math.max(0, 1 - Math.max(0, age - 0.8f) / 0.8f);
            String name = previewId >= 0 && age < 1.6f ? Sfx.NAMES[previewId] : Sfx.LOOP_NAMES[previewLoop] + " (ambience)";
            float ph = 54 * dp, pw = Math.min(maxW, 420 * dp), px = (w - pw) / 2, py = h - ph - 14 * dp;
            tmp.set(px, py, px + pw, py + ph);
            fill.setColor(((int) (a * 235) << 24) | 0x16191E);
            c.drawRoundRect(tmp, 14 * dp, 14 * dp, fill);
            stroke.setStrokeWidth(1.5f * dp);
            stroke.setColor(((int) (a * 255) << 24) | (ACCENT & 0xFFFFFF));
            c.drawRoundRect(tmp, 14 * dp, 14 * dp, stroke);
            text.setTextAlign(Paint.Align.LEFT);
            text.setTextSize(15 * dp);
            text.setColor(((int) (a * 255) << 24) | (INK & 0xFFFFFF));
            c.drawText("\u266A  " + name, px + 16 * dp, py + ph / 2 + 5 * dp, text);
            int bars = 14;
            float bx0 = px + pw - 16 * dp - bars * 7 * dp;
            for (int k = 0; k < bars; k++) {
                float lvl = 0.25f + 0.75f * Math.abs((float) Math.sin(time * (5 + k * 1.3f) + k * 0.7f)) * (bed ? 0.8f : Math.max(0.15f, 1 - age / 1.4f));
                float bh2 = (ph - 20 * dp) * lvl;
                fill.setColor(((int) (a * 255) << 24) | (ACCENT & 0xFFFFFF));
                c.drawRect(bx0 + k * 7 * dp, py + ph - 10 * dp - bh2, bx0 + k * 7 * dp + 4 * dp, py + ph - 10 * dp, fill);
            }
        }
    }

    /** A titled box of label / number rows; returns where it ends. */
    private float table(Canvas c, float x, float y, float width, String title, int col, String[][] rows) {
        tmp.set(x, y, x + width, y + 30 * dp + rows.length * 22 * dp + 8 * dp);
        fill.setColor(0xFF1C1F24);
        c.drawRoundRect(tmp, 10 * dp, 10 * dp, fill);
        text.setTextAlign(Paint.Align.LEFT);
        text.setTextSize(14 * dp);
        text.setColor(col);
        c.drawText(title, x + 12 * dp, y + 21 * dp, text);
        float ry = y + 30 * dp;
        plain.setTextSize(13 * dp);
        for (String[] r : rows) {
            plain.setTextAlign(Paint.Align.LEFT);
            plain.setColor(MUTED);
            c.drawText(r[0], x + 12 * dp, ry + 15 * dp, plain);
            plain.setTextAlign(Paint.Align.RIGHT);
            plain.setColor(INK);
            c.drawText(r[1], x + width - 12 * dp, ry + 15 * dp, plain);
            ry += 22 * dp;
        }
        return tmp.bottom;
    }

    /** ((13.3) The living with the most kills, most first. Tap one to go and watch them. */
    private void drawTop(Canvas c, int w, int h) {
        float top = header(c, w, h, "Top Fighters");
        World world = host.world();
        if (time - topAt > 1) {
            topAt = time;
            topList = world.topFighters(50);
        }
        float side = 16 * dp, maxW = Math.min(w - side * 2, 720 * dp), x0 = (w - maxW) / 2;
        listArea.set(0, top, w, h - 12 * dp);
        c.save();
        c.clipRect(listArea);
        float y = top + 8 * dp - scroll, rh = 52 * dp;
        for (int i = 0; i < topList.size(); i++) {
            Entity e = topList.get(i);
            if (y + rh > top && y < h) {
                tmp.set(x0, y, x0 + maxW, y + rh);
                fill.setColor(i < 3 ? 0xE02A2E22 : 0xE0202328);
                c.drawRoundRect(tmp, 8 * dp, 8 * dp, fill);
                text.setTextAlign(Paint.Align.LEFT);
                text.setTextSize(15 * dp);
                text.setColor(i == 0 ? 0xFFFFD54A : i < 3 ? ACCENT : INK);
                c.drawText((i + 1) + ".", x0 + 10 * dp, y + 22 * dp, text);
                String who = Names.person(e.nameSeed);
                if (e.type == Entity.COP || e.type == Entity.SOLDIER) who += "  (" + Dispatch.name(e) + ")";
                text.setColor(INK);
                c.drawText(fit(text, who, maxW - 130 * dp), x0 + 40 * dp, y + 22 * dp, text);
                plain.setTextAlign(Paint.Align.LEFT);
                plain.setTextSize(12 * dp);
                plain.setColor(MUTED);
                int hp = (int) Math.max(1, Math.ceil(100 * e.hp / Math.max(1, e.maxHp)));
                String what = World.kindOf(e) + "  -  " + hp + "% health" + (e.dead ? "  -  aboard a vehicle" : "");
                c.drawText(fit(plain, what, maxW - 130 * dp), x0 + 40 * dp, y + 41 * dp, plain);
                text.setTextAlign(Paint.Align.RIGHT);
                text.setTextSize(16 * dp);
                text.setColor(ACCENT);
                c.drawText(e.kills + (e.kills == 1 ? " kill" : " kills"), x0 + maxW - 12 * dp, y + 23 * dp, text);
                plain.setTextAlign(Paint.Align.RIGHT);
                plain.setColor(MUTED);
                c.drawText("Watch \u25B6", x0 + maxW - 12 * dp, y + 41 * dp, plain);
                Button b = button("", A_TOP_ROW + i, false, x0, Math.max(y, top), x0 + maxW, y + rh);
                b.invisible = true;
            }
            y += rh + 6 * dp;
        }
        c.restore();
        if (topList.isEmpty()) {
            plain.setTextAlign(Paint.Align.CENTER);
            plain.setColor(MUTED);
            c.drawText("Nobody has put down any of the dead yet.", w / 2f, top + 40 * dp, plain);
        }
        contentHeight = y + scroll - top;
        clampScroll();
        drawScrollHint(c, w);
    }

    /** Every radio message this game, newest first. Tap one to go there. */
    private void drawLog(Canvas c, int w, int h) {
        float top = header(c, w, h, "Radio Log");
        float side = 16 * dp, maxW = Math.min(w - side * 2, 720 * dp), x0 = (w - maxW) / 2;
        // ((12.0) Filters: everything, or just the 911 calls, the police, the army or the news.)
        String[] tabs = {"All", "911", "Police", "Military", "News"};
        float tg = 6 * dp, tw = (maxW - tg * (tabs.length - 1)) / tabs.length, th = 34 * dp;
        int firstTab = buttons.size();
        for (int k = 0; k < tabs.length; k++)
            button(tabs[k], A_LOG_FILTER + k, logFilter == k, x0 + k * (tw + tg), top, x0 + k * (tw + tg) + tw, top + th);
        sameSize(firstTab);
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
            // ((12.0) When it was said, in front.)
            String when = World.clock((int) m.time) + "  ";
            plain.setTextSize(11.5f * dp);
            float whenW = plain.measureText(when);
            plain.setTextSize(13 * dp);
            float whoW = text.measureText(m.who + ": ") + whenW;
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
                plain.setTextSize(11.5f * dp);
                plain.setColor(MUTED);
                plain.setTextAlign(Paint.Align.LEFT);
                c.drawText(when, x0 + 10 * dp, y + 20 * dp, plain);
                plain.setTextSize(13 * dp);
                text.setColor(m.color);
                c.drawText(m.who + ": ", x0 + 10 * dp + whenW, y + 20 * dp, text);
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
        button("Report", A_REPORT, false, w - 24 * dp - 220 * dp, 12 * dp, w - 24 * dp - 110 * dp, 12 * dp + 42 * dp);
        boolean landscape = w > h;
        float side = 20 * dp;
        // ((12.0) Half the width each sideways: at 58% for the chart, the list beside it cut most of its names short.)
        RectF chart = new RectF(side, top + 10 * dp, landscape ? w * 0.5f : w - side,
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
            // ((12.0) On a square-root scale: on a straight one, a few hundred of the dead beside fifteen thousand
            // people was a flat line along the bottom.)
            c.drawText(String.valueOf(Math.round(max * (k / 4f) * (k / 4f))), plot.left - 6 * dp, y + 4 * dp, plain);
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
                    float y0 = plot.bottom - plot.height() * (float) Math.sqrt(series[sIdx][i - 1] / (float) max);
                    float y1 = plot.bottom - plot.height() * (float) Math.sqrt(series[sIdx][i] / (float) max);
                    c.drawLine(x0, y0, x1, y1, stroke);
                }
            }
        } else {
            plain.setTextAlign(Paint.Align.CENTER);
            c.drawText("Play a little longer to see the chart.", plot.centerX(), plot.centerY(), plain);
        }
        // Legend. ((12.0) Spaced to the words, and smaller where it won't fit: "Police & army" ran off the chart.)
        text.setTextSize(12 * dp);
        text.setTextAlign(Paint.Align.LEFT);
        String[] keys = {"People", "Zombies", "Police & army"};
        int[] keyCols = {0xFF6FA8FF, 0xFF7CC24E, 0xFFE8C547};
        float keysW = 0;
        for (String k : keys) keysW += 10 * dp + text.measureText(k) + 18 * dp;
        float room = chart.width() - 22 * dp;
        if (keysW > room) {
            text.setTextSize(12 * dp * Math.max(0.7f, room / keysW));
            keysW = 0;
            for (String k : keys) keysW += 10 * dp + text.measureText(k) + 18 * dp;
        }
        float kx = chart.left + 16 * dp, kgap = keysW > room ? 10 * dp : 18 * dp;
        text.setColor(0xFFE6E6E6);
        for (int k = 0; k < keys.length; k++) {
            fill.setColor(keyCols[k]);
            c.drawCircle(kx, chart.top + 16 * dp, 4.5f * dp, fill);
            c.drawText(keys[k], kx + 10 * dp, chart.top + 20 * dp, text);
            kx += 10 * dp + text.measureText(keys[k]) + kgap;
        }

        int t = (int) world.time;
        String[][] totals = {
                {"Time", World.clock(t)},
                {"People now", String.valueOf(world.humanCount() + world.hiding + world.riding + world.visiting)},
                {"Zombies now", String.valueOf(world.zombieCount())},
                {"Most zombies at once", String.valueOf(world.peakZombies)},
                {"Zombies killed", String.valueOf(world.zombiesKilled)},
                // ((12.0) And by whom.)
                {"   by police, army, civilians", world.killsBy[0] + " / " + world.killsBy[1] + " / " + world.killsBy[2]},
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
                // ((12.0) How good a shot the city is.)
                {"Shots per zombie put down", world.killsBy[0] + world.killsBy[1] + world.killsBy[2] > 0
                        ? String.format(java.util.Locale.US, "%.1f", world.shotsFired / (float) (world.killsBy[0] + world.killsBy[1] + world.killsBy[2])) : "-"},
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
            // (A name a little smaller before it's cut short.)
            float nameRoom = tx1 - tx0 - vw - 10 * dp, nw = plain.measureText(row[0]);
            plain.setTextSize(nw > nameRoom ? Math.max(ts * 0.8f, ts * nameRoom / nw) : ts);
            c.drawText(fit(plain, row[0], nameRoom), tx0, ty, plain);
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
        return "Patient zero " + Names.person(w.patientZeroSeed) + ": " + World.count(ch[0], "person", "people") + ", " + World.count(ch[1], "generation", "generations")
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
        // ((12.0) Sideways, the map on the left and the time, counts and controls beside it: under it, on a phone
        // held sideways the map was a thumbnail and the timeline had no room at all.)
        boolean beside = w > h;
        if (beside) size = Math.max(0, Math.min(h - top - 16 * dp, w * 0.5f));
        if (beside) replayMap.set(16 * dp, top, 16 * dp + size, top + size);
        else replayMap.set((w - size) / 2, top, (w + size) / 2, top + size);
        float colL = beside ? replayMap.right + 24 * dp : replayMap.left, colR = beside ? w - 16 * dp : replayMap.right;
        float colCX = (colL + colR) / 2;
        City city = world.city;
        fill.setColor(0xFF1C1F24);
        c.drawRect(replayMap, fill);
        bmpPaint.setAlpha(150);
        city.drawMap(c, 0, 0, -1, -1, replayMap, bmpPaint);
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
        float ly = beside ? top + 30 * dp : replayMap.bottom + 22 * dp;
        text.setTextAlign(Paint.Align.CENTER);
        c.drawText(World.clock(secs), colCX, ly, text);
        ly += 22 * dp;
        text.setTextSize(13 * dp);
        text.setTextAlign(Paint.Align.RIGHT);
        int turnedSoFar = 0;
        for (int i = 0, n = world.turnEvents.size(); i < n && world.turnEvents.get(i)[0] <= m[0]; i++) turnedSoFar++;
        text.setColor(0xFF6FA8FF);
        String counts = "People " + (int) m[2];
        float cx = colCX + text.measureText(counts + "   Zombies " + (int) m[1] + "   Turned " + turnedSoFar) / 2;
        c.drawText(counts, cx - text.measureText("   Zombies " + (int) m[1] + "   Turned " + turnedSoFar), ly, text);
        text.setColor(0xFF8BE05A);
        c.drawText("   Zombies " + (int) m[1], cx - text.measureText("   Turned " + turnedSoFar), ly, text);
        text.setColor(0xFFFF6A5A);
        c.drawText("   Turned " + turnedSoFar, cx, ly, text);
        // The timeline: drag it to scrub, the button plays and pauses.
        float bw = 90 * dp;
        float by = Math.min(h - 16 * dp - barH, ly + 18 * dp);
        button(replayPlaying ? "Pause" : replayPos >= 1 ? "Again" : "Play", A_REPLAY_PLAY, true, colL, by, colL + bw, by + barH);
        replayBar.set(colL + bw + 14 * dp, by, colR, by + barH);
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

    /** A list of settings (or New Game choices), each a card with its value between arrows. */
    private void drawOptions(Canvas c, int w, int h, String title, OptionSet opts, boolean setup) {
        float top = header(c, w, h, title);
        // ((12.0) Settings has a way back to how everything came, along the bottom.)
        float footer = setup ? 72 * dp : 66 * dp;
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
            // ((12.0) A narrower value pill on a narrow screen, so the setting's name isn't cut to a word.)
            float vw = Math.min(190 * dp, colW * (colW < 320 * dp ? 0.46f : 0.55f));
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
        if (!setup) {
            boolean sure = time - resetArmedAt < 4;
            float bw = Math.min(260 * dp, w - 32 * dp);
            button(sure ? "Tap again to reset everything" : "Reset to defaults", A_RESET_SETTINGS, false, (w - bw) / 2, h - footer + 10 * dp, (w + bw) / 2, h - 12 * dp);
        }

        if (setup) {
            float bgap = 8 * dp, bw = Math.min(180 * dp, (w - 32 * dp - bgap * 2) / 3), bh = 50 * dp, by = h - footer + 11 * dp;
            float x = (w - bw * 3 - bgap * 2) / 2;
            int firstFoot = buttons.size();
            button("Surprise me", A_RANDOM, false, x, by, x + bw, by + bh);
            button(config.keepCity ? config.code() : "City code", A_CODE, false, x + bw + bgap, by, x + bw * 2 + bgap, by + bh);
            button("Start  ›", A_START, true, x + bw * 2 + bgap * 2, by, x + bw * 3 + bgap * 2, by + bh);
            sameSize(firstFoot);
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
            text.setTextAlign(Paint.Align.RIGHT);
            text.setTextSize(14.5f * dp);
            text.setColor(0xFFFFFFFF);
            String val = records.recordText(i);
            // ((12.0) The name cut short of its figure: on a narrow screen they ran into each other.)
            float room = maxW - text.measureText(val) - 12 * dp, nw = plain.measureText(Records.RECORD_NAMES[i]);
            if (nw > room) plain.setTextSize(Math.max(11 * dp, 14.5f * dp * room / nw));
            c.drawText(fit(plain, Records.RECORD_NAMES[i], room), x0, y + 14 * dp, plain);
            c.drawText(val, x0 + maxW, y + 14 * dp, text);
            y += 26 * dp;
        }
        // ((15.1) The hall of fame: the best police officers and soldiers ever, by kills and lives saved.)
        y += 14 * dp;
        text.setTextAlign(Paint.Align.LEFT);
        text.setTextSize(17 * dp);
        text.setColor(ACCENT);
        c.drawText("Hall of fame", x0, y + 18 * dp, text);
        y += 34 * dp;
        java.util.List<Records.Hero> hall = records.hall();
        if (hall.isEmpty()) {
            plain.setTextAlign(Paint.Align.LEFT);
            plain.setTextSize(13.5f * dp);
            plain.setColor(0xFF8A9099);
            c.drawText(fit(plain, "Police and soldiers with " + Records.HALL_MIN + " kills or more (a life saved counts as three) go here.", maxW), x0, y + 12 * dp, plain);
            y += 26 * dp;
        }
        for (int i = 0; i < hall.size(); i++) {
            Records.Hero hr = hall.get(i);
            int rank = 0;
            while (rank + 1 < World.RANK_KILLS.length && hr.kills >= World.RANK_KILLS[rank + 1]) rank++;
            String who = (i + 1) + ".  " + (rank > 0 ? World.RANK_NAMES[rank] + " " : "") + Names.person(hr.seed);
            String kind = hr.role > 0 && hr.role < Entity.ROLE_NAMES.length ? Entity.ROLE_NAMES[hr.role] : hr.type == Entity.COP ? "Police officer" : "Soldier";
            String val = hr.kills + (hr.kills == 1 ? " kill" : " kills") + (hr.saved > 0 ? "  -  " + hr.saved + " saved" : "");
            text.setTextAlign(Paint.Align.RIGHT);
            text.setTextSize(14 * dp);
            text.setColor(0xFFFFFFFF);
            float room = maxW - text.measureText(val) - 12 * dp;
            plain.setTextAlign(Paint.Align.LEFT);
            plain.setTextSize(14.5f * dp);
            plain.setColor(hr.type == Entity.COP ? 0xFF8FB8FF : 0xFFB8D08A);
            c.drawText(fit(plain, who, room), x0, y + 14 * dp, plain);
            c.drawText(val, x0 + maxW, y + 14 * dp, text);
            plain.setTextSize(12 * dp);
            plain.setColor(0xFF8A9099);
            c.drawText(fit(plain, kind + ", " + hr.city, maxW), x0 + 22 * dp, y + 30 * dp, plain);
            y += 42 * dp;
        }
        // ((21.4) Start the records over, with a second tap to be sure.)
        y += 14 * dp;
        float bw = Math.min(240 * dp, maxW);
        button(time - statsArmedAt < 4 ? "Tap again to reset" : "Reset stats", A_RESET_STATS, false, (w - bw) / 2, y, (w + bw) / 2, y + 44 * dp);
        y += 58 * dp;
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
                        // ((22.1) A preview is small enough to be a single tile.)
                        preview = city.mapTiles != null && city.mapTiles.length == 1 ? city.mapTiles[0] : null;
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
        // ((12.0) One size of writing for the whole row, the largest every choice fits at: each was sized to its own
        // length, so "Large" stood out bigger than "Massive" beside it.)
        float rowSize = 14 * dp;
        for (int k = 0; k < shown; k++) rowSize = Math.min(rowSize, bw / Math.max(3, values[index[k]].length()) * 1.5f);
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
            text.setTextSize(rowSize);
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
            } else if (i == CityConfig.TOWERS) {
                // ((15.2) Skyscrapers, packed close.)
                c.drawRect(ex - u * 1.8f, ey - u * 0.9f, ex - u * 1.05f, ey + u, fill);
                c.drawRect(ex - u * 0.9f, ey - u * 2.1f, ex - u * 0.15f, ey + u, fill);
                c.drawRect(ex, ey - u * 1.5f, ex + u * 0.7f, ey + u, fill);
                c.drawRect(ex + u * 0.85f, ey - u * 2.5f, ex + u * 1.75f, ey + u, fill);
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
        // ((12.0) Never less than nothing: on a short screen held sideways it went negative and was drawn inside out.)
        float pv = Math.max(0, landscape ? Math.min(h - top - footer - 72 * dp, w * 0.34f) : Math.min(w - side * 2, (h - top) * 0.3f));
        // (Too small to see sideways: no picture, and the column keeps a width for the words.)
        if (landscape && pv < 64 * dp) pv = 0;
        float col = landscape ? Math.max(pv, 170 * dp) : pv;
        float px = landscape ? side : (w - pv) / 2, py = top + 4 * dp;
        boolean current = config.code().equals(previewKey);
        Bitmap bmp = preview;
        if (pv > 0) {
            tmp.set(px - 4 * dp, py - 4 * dp, px + pv + 4 * dp, py + pv + 4 * dp);
            fill.setColor(CARD);
            c.drawRoundRect(tmp, 18 * dp, 18 * dp, fill);
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
        }
        // ((12.0) In portrait, New Map goes beside the picture where there's room; otherwise in its corner, with the
        // "Building city" note lifted clear of it - the button sat over the words.)
        boolean besideRoom = !landscape && w - side - (px + pv) >= 118 * dp;
        float lift = !landscape && !besideRoom ? Math.max(0, py + pv / 2 + 26 * dp - (py + pv - 42 * dp)) : 0;
        if (!current && pv > 0) {
            // (A spinning arc while the city is built.)
            stroke.setColor(ACCENT);
            stroke.setStrokeWidth(3 * dp);
            tmp.set(px + pv / 2 - 16 * dp, py + pv / 2 - 30 * dp - lift, px + pv / 2 + 16 * dp, py + pv / 2 + 2 * dp - lift);
            c.drawArc(tmp, time * 360, 260, false, stroke);
            text.setTextAlign(Paint.Align.CENTER);
            text.setTextSize(14 * dp);
            text.setColor(INK);
            c.drawText("Building city...", px + pv / 2, py + pv / 2 + 26 * dp - lift, text);
        }
        int preset = config.v[CityConfig.OPT_PRESET];
        float infoY = py + pv + 20 * dp;
        float infoW = landscape ? col : w - side * 2, infoX = landscape ? px : side;
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
            button("New map", A_REROLL, false, px, ly, px + col, ly + 34 * dp);
        } else {
            ly = drawInfo(c, preset, infoX, infoY + 18 * dp, infoW);
        }

        // The options, scrolling if they don't fit.
        float lx = landscape ? px + col + 24 * dp : side, lw = w - lx - side;
        float listTop = landscape ? top : ly + 2 * dp;
        if (!landscape) {
            if (besideRoom) button("New map", A_REROLL, false, w - side - 104 * dp, py + pv - 30 * dp, w - side, py + pv + 4 * dp);
            else button("New map", A_REROLL, false, w - side - 110 * dp, py + pv - 36 * dp, w - side - 6 * dp, py + pv - 6 * dp);
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
        // (Maps no longer offered are left out, unless a loaded city code is using one.)
        int cards = 0;
        int[] shown = new int[CityConfig.PRESETS.length];
        for (int i = 0; i < CityConfig.PRESETS.length; i++)
            if (!CityConfig.retired(CityConfig.OPT_PRESET, i) || preset == i) shown[cards++] = i;
        y = mapCards(c, shown, cards, preset, lx, y, cw, ch, gap, perRow);
        if (preset == CityConfig.ISLANDS || preset == CityConfig.ARCHIPELAGO) {
            // Islands comes in one size only.
            plain.setTextAlign(Paint.Align.LEFT);
            plain.setTextSize(13 * dp);
            plain.setColor(0xFFB8BDC4);
            String note = "Map size: " + (preset == CityConfig.ARCHIPELAGO ? "Archipelago" : "Islands") + " is always Colossal, "
                    + config.tiles() + " tiles across.";
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
                int needMb = size >= 8 ? 380 : size >= 7 ? 300 : size >= 6 ? 260 : 200; // ((16.11) Measured again: the Large map runs in 320 MB now, Big in 240, Default in 200.)
                long haveMb = Runtime.getRuntime().maxMemory() / 1048576;
                boolean low = haveMb < needMb;
                String note = low ? "Warning: this phone gives the game " + haveMb + " MB and a " + CityConfig.sizeName(size)
                        + " city needs about " + needMb + " MB. It may run out of memory - try a smaller size."
                        : CityConfig.sizeName(size) + " maps take a while to build (up to a minute on a slower phone) and need a recent phone to run smoothly.";
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
        int firstFoot = buttons.size();
        button("Surprise me", A_RANDOM, false, x, by, x + bw, by + bh);
        button("City code", A_CODE, false, x + bw + bgap, by, x + bw * 2 + bgap, by + bh);
        button("Start  ›", A_START, true, x + bw * 2 + bgap * 2, by, x + bw * 3 + bgap * 2, by + bh);
        sameSize(firstFoot);
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
            // ((12.0) A long heading wraps instead of running off the side.)
            text.setTextAlign(Paint.Align.LEFT);
            text.setTextSize(17 * dp);
            ArrayList<String> head = wrap(entry[0], maxW, text);
            for (int k = 0; k < head.size(); k++) {
                if (y + 32 * dp > top - 40 * dp && y < h) {
                    text.setColor(ACCENT);
                    c.drawText(head.get(k), x0, y + 18 * dp, text);
                }
                y += k < head.size() - 1 ? 22 * dp : 32 * dp;
            }
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
                // ((19.6) Every list scrolls: the Campaign, the reports, the Sound Lab and the top fighters were left out.)
                if (!dragging && Math.abs(y - downY) > 10 * dp && listArea.contains(downX, downY)
                        && screen != MAIN && screen != PAUSE && screen != REPLAY && screen != NONE) dragging = true;
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
        if (action >= A_LOOP && action < A_LOOP + Sfx.LOOPS) {
            int l = action - A_LOOP;
            previewLoop = previewLoop == l ? -1 : l;
            return;
        }
        if (action >= A_SFX && action < A_SFX + Sfx.COUNT) {
            host.preview(action - A_SFX);
            previewId = action - A_SFX;
            previewAt = time;
            return;
        }
        if (action >= A_PERK && action < A_PERK + Perks.COUNT) {
            if (records != null) records.takePerk(action - A_PERK);
            return;
        }
        if (action >= A_MISSION) {
            int id = action - A_MISSION;
            if (Campaign.get(id) != null && (records == null || records.unlocked(id))) host.startGame(Campaign.config(id));
            return;
        }
        if (action >= A_TOP_ROW && action < A_LOG_FILTER) {
            int idx = action - A_TOP_ROW;
            if (idx < topList.size()) host.watch(topList.get(idx));
            return;
        }
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
        if (action >= A_DELETE_SLOT) {
            int slot = action - A_DELETE_SLOT;
            if (!(deleteArmed == slot && time - deleteArmedAt < 4)) {
                deleteArmed = slot;
                deleteArmedAt = time;
                return;
            }
            deleteArmed = -1;
            note = host.deleteSlot(slot) ? (slot == 0 ? "Autosave deleted." : "Slot " + slot + " deleted.") : "That save couldn't be deleted.";
            noteTime = 3;
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
            case A_TOP:
                open(TOP);
                break;
            case A_REPORT:
                open(REPORT);
                break;
            case A_SOUNDS:
                open(SOUNDS);
                break;
            case A_CAMPAIGN:
                open(CAMPAIGN);
                returnTo = MAIN;
                break;
            case A_RETRY:
                if (host.world() != null && host.world().mission() != null) host.startGame(Campaign.config(host.world().city.cfg.mission));
                break;
            case A_NEXT:
                if (host.world() != null && Campaign.get(host.world().city.cfg.mission + 1) != null)
                    host.startGame(Campaign.config(host.world().city.cfg.mission + 1));
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
            case A_RESET_STATS:
                if (time - statsArmedAt >= 4) {
                    statsArmedAt = time;
                    break;
                }
                statsArmedAt = -99;
                records.resetStats();
                break;
            case A_RESET_SETTINGS:
                if (time - resetArmedAt >= 4) {
                    resetArmedAt = time;
                    break;
                }
                resetArmedAt = -99;
                settings.reset();
                host.settingsChanged();
                break;
        }
    }
}
