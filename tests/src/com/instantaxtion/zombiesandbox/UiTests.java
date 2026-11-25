package com.instantaxtion.zombiesandbox;

import java.io.File;
import java.lang.reflect.Field;

/** Screens: every screen draws, the game view; and (28.2) no campaign. */
final class UiTests extends TestKit {
    static void register() {
        test("every screen draws", FAST, 45, UiTests.class,
                "everyScreen", "gamesLetGo", "recordsAndSettings");
        test("the game view: the after-action report, sound, visuals, overlays, the war meter, the map in tiles", FULL, 30, UiTests.class,
                "report", "sound", "visuals", "firesAndMeter", "overlays", "mapTiles");
        test("no campaign and no evacuation convoys (28.2); always Normal, the director, the last of the dead", FULL, 25, UiTests.class,
                "noCampaign", "noEvacConvoys", "difficulty", "lastOfTheDead");
    }

    static void everyScreen() throws Exception {
        GameView v = new GameView(new android.app.Activity());
        for (int[] size : new int[][]{{2400, 1080}, {1080, 2400}, {2560, 1600}}) {
            v.layout(size[0], size[1]);
            Menu m = (Menu) field(v, "menu");
            for (int s : new int[]{Menu.MAIN, Menu.SETUP, Menu.SETTINGS, Menu.NOTES, Menu.RECORDS}) {
                m.open(s);
                draw(v, size);
            }
            CityConfig c = new CityConfig();
            c.v[CityConfig.OPT_ZOMBIES] = 3;
            v.startGame(c);
            v.awaitLoad();
            draw(v, size);
            m.screen = Menu.NONE;
            for (int k = 0; k < 20; k++) draw(v, size);
            // Every spawn picker opens and draws.
            Field pk = GameView.class.getDeclaredField("picker");
            pk.setAccessible(true);
            for (int t = 0; t < 13; t++) {
                pk.setInt(v, t);
                draw(v, size);
            }
            pk.setInt(v, -1);
            m.open(Menu.PAUSE);
            draw(v, size);
            m.open(Menu.STATS);
            draw(v, size);
            m.open(Menu.LOG);
            draw(v, size);
            m.open(Menu.STATS);
            java.lang.reflect.Method act = Menu.class.getDeclaredMethod("act", int.class);
            act.setAccessible(true);
            act.invoke(m, 24);
            draw(v, size);
            m.open(Menu.SAVES);
            draw(v, size);
            check(v.drawErrors == 0, "every frame drew without an error (" + v.drawErrors + ")");
            // Both graphics styles draw, and switching redraws the map.
            Settings st = (Settings) field(v, "settings");
            World w = (World) field(v, "world");
            for (int g = 0; g < 2; g++) {
                st.put(13, g);
                v.settingsChanged();
                check(w.city.drawnRealistic == (g == 1), "map redrawn in the chosen style");
                m.screen = Menu.NONE;
                draw(v, size);
            }
            m.screen = Menu.NONE;
        }
    }

    static void gamesLetGo() throws Exception {
        GameView v = new GameView(new android.app.Activity());
        int[] size = {1200, 900};
        v.layout(size[0], size[1]);
        for (int k = 0; k < 3; k++) draw(v, size);
        java.lang.ref.WeakReference<Object> menuCity = new java.lang.ref.WeakReference<Object>(((World) field(v, "world")).city);
        CityConfig c = new CityConfig();
        c.v[CityConfig.OPT_ZOMBIES] = 2;
        v.startGame(c);
        for (int i = 0; i < 4 && menuCity.get() != null; i++) {
            System.gc();
            Thread.sleep(100);
        }
        check(menuCity.get() == null, "the menu's city is let go of as soon as the game starts building");
        v.awaitLoad();
        Menu m = (Menu) field(v, "menu");
        for (int k = 0; k < 10; k++) draw(v, size);
        m.screen = Menu.NONE;
        for (int k = 0; k < 10; k++) draw(v, size);
        java.lang.ref.WeakReference<Object> first = new java.lang.ref.WeakReference<Object>(field(v, "world"));
        CityConfig c2 = new CityConfig();
        c2.v[CityConfig.OPT_ZOMBIES] = 4;
        v.startGame(c2);
        for (int i = 0; i < 4 && first.get() != null; i++) {
            System.gc();
            Thread.sleep(100);
        }
        check(first.get() == null, "and the last game, once saved, while the next one builds");
        v.awaitLoad();
        for (int k = 0; k < 10; k++) draw(v, size);
        m.screen = Menu.NONE;
        for (int k = 0; k < 10; k++) draw(v, size);
        check(v.drawErrors == 0, "drew without an error");
        // The war bar: the dead against the city.
        World w = (World) field(v, "world");
        w.recount();
        for (int i = 0; i < 30 * 3; i++) {
            w.update(1 / 30f);
            w.evCount = 0;
        }
        check(w.outbreak, "the war's on");
        // All of the dead gone: it says so.
        for (int i = 0; i < 30 * 20; i++) {
            for (Entity e : w.entities) {
                if (e.isZombie()) e.hp = 0;
                e.infected = false;
            }
            w.update(1 / 30f);
            w.evCount = 0;
            if (!w.outbreak) break;
        }
        check(w.warBalance > 0.9f || !w.outbreak, "and the city's side ahead once they're down (" + w.warBalance + ")");
    }

    static void recordsAndSettings() throws Exception {
        Records rec = new Records(new android.app.Activity());
        rec.best(1, 500);
        check(rec.record(1) == 500, "a record is kept");
        rec.resetStats();
        check(rec.record(1) == 0 && rec.hall().isEmpty(), "Reset stats clears the records and the hall of fame");

        Settings st = new Settings(new android.app.Activity());
        check(st.count() < 15, "fewer settings (" + st.count() + ")");
        boolean view = false;
        for (int i = 0; i < st.count(); i++) view |= st.label(i).equals("View");
        check(view, "View (3D or bird's-eye) is in Settings");
        check(Dispatch.routine("Unit 4: All quiet at Elm St. Heading back.") && !Dispatch.routine("Dispatch: Officer down at Elm St!"),
                "routine chatter is left off the radio, not what matters");
    }

    static void report() throws Exception {
        // The report: drawn, at the end of a game it opens itself.
        GameView v = new GameView(new android.app.Activity());
        int[] size = {1080, 2000};
        v.layout(size[0], size[1]);
        Menu m = (Menu) field(v, "menu");
        CityConfig c2 = new CityConfig();
        c2.v[CityConfig.OPT_ZOMBIES] = 2;
        v.startGame(c2);
        v.awaitLoad();
        for (int k = 0; k < 3; k++) draw(v, size);
        m.screen = Menu.NONE;
        World gw = (World) field(v, "world");
        for (int f = 0; f < 30 * 3; f++) {
            gw.update(1 / 30f);
            gw.evCount = 0;
        }
        for (int f = 0; f < 30 * 30 && gw.warResult == 0; f++) {
            for (Entity e : gw.entities) {
                if (e.isZombie()) e.hp = 0;
                e.infected = false;
            }
            gw.update(1 / 30f);
            gw.evCount = 0;
        }
        check(gw.warResult == 1, "the outbreak's over");
        gw.warBannerTime = 0;
        draw(v, size);
        check(m.screen == Menu.REPORT, "and the report's up");
        java.lang.reflect.Field sc = Menu.class.getDeclaredField("scroll");
        sc.setAccessible(true);
        for (int k = 0; k < 4; k++) {
            sc.setFloat(m, k * 600);
            draw(v, size);
        }
        check(v.drawErrors == 0, "drawn without an error (" + v.drawErrors + ")");
    }

    static void sound() throws Exception {
        check(Sfx.MIN_GAP.length == Sfx.COUNT && Sfx.NAMES.length == Sfx.COUNT && Sfx.BUS.length == Sfx.COUNT, "every sound has its settings and a name");
        // Every new sound: made, not silent, nothing broken in it.
        for (int id = Sfx.CASH; id < Sfx.COUNT; id++)
            for (int v = 0; v < Sfx.VARIANTS[id]; v++) {
                short[] s = Synth.effect(id, v);
                int peak = 0;
                for (short x : s) peak = Math.max(peak, Math.abs(x));
                check(s.length > Synth.RATE / 50 && peak > 4000, Sfx.NAMES[id] + " take " + v + " (" + s.length + ", " + peak + ")");
            }
        // The ambience beds: the right length, not silent, and no click where they loop round.
        for (int l = 0; l < Sfx.LOOPS; l++) {
            short[] s = Synth.ambience(l);
            int peak = 0;
            for (short x : s) peak = Math.max(peak, Math.abs(x));
            check(s.length == (int) (Synth.LOOP_SECONDS * Synth.RATE) && peak > 4000, Sfx.LOOP_NAMES[l] + " bed (" + s.length + ", " + peak + ")");
            int seam = Math.abs(s[s.length - 1] - s[0]);
            check(seam < peak * 0.6f, Sfx.LOOP_NAMES[l] + " loops without a click (" + seam + " of " + peak + ")");
        }
        // The mixer: a bed playing; a group turned off; mono; deafened.
        Mixer m = new Mixer(48000);
        m.setLoopSamples(Sfx.LOOP_WIND, Synth.ambience(Sfx.LOOP_WIND));
        m.setLoop(Sfx.LOOP_WIND, 0.8f, 0.5f, 0);
        float[] buf = new float[512 * 2];
        float energy = 0;
        for (int k = 0; k < 60; k++) {
            m.render(buf, 512);
            for (float f : buf) energy += f * f;
        }
        check(energy > 1 && m.loopGain(Sfx.LOOP_WIND) > 0.4f, "an ambience bed plays (" + energy + ")");
        m.setLoop(Sfx.LOOP_WIND, 0, 0, 0);
        for (int k = 0; k < 200; k++) m.render(buf, 512);
        check(m.loopGain(Sfx.LOOP_WIND) < 0.02f, "and fades away");
        m.setSamples(Sfx.PISTOL, new short[][]{Synth.effect(Sfx.PISTOL, 0)});
        m.setBus(Sfx.BUS_GUNS, 0);
        m.trigger(Sfx.PISTOL, 1, 0, 0, 1);
        m.render(buf, 512);
        check(m.playing() == 0, "a group turned right down plays nothing");
        m.setBus(Sfx.BUS_GUNS, 1);
        m.setMono(true);
        m.trigger(Sfx.PISTOL, 1, 1, 0, 1);
        m.render(buf, 512);
        boolean same = true;
        for (int f = 0; f < 512; f++) same &= Math.abs(buf[f * 2] - buf[f * 2 + 1]) < 1e-5f;
        check(same, "mono: the same in both ears");
        m.setMono(false);
        m.shock(1);
        check(m.shockLevel() > 0.9f, "deafened by a blast");
        for (int k = 0; k < 400; k++) m.render(buf, 512);
        check(m.shockLevel() < 0.01f, "and it wears off");
        // The game: the ambience follows the screen; captions for sounds off it.
        GameView v = new GameView(new android.app.Activity());
        int[] size = {1080, 2000};
        v.layout(size[0], size[1]);
        Menu menu = (Menu) field(v, "menu");
        Settings st = (Settings) field(v, "settings");
        st.put(22, 1);
        CityConfig c = cfg(CityConfig.SMALL, 51);
        c.v[CityConfig.OPT_ZOMBIES] = 3;
        v.startGame(c);
        v.awaitLoad();
        for (int k = 0; k < 3; k++) draw(v, size);
        menu.screen = Menu.NONE;
        World w = (World) field(v, "world");
        for (int k = 0; k < 12; k++) draw(v, size);
        Sound snd = (Sound) field(v, "sound");
        check(snd.loopGain[Sfx.LOOP_CITY] > 0, "the city's hum under the game");
        w.emit(Sfx.RIFLE, w.city.worldW() / 2 + 2000, w.city.worldH() / 2);
        java.lang.reflect.Field cam = GameView.class.getDeclaredField("camX");
        cam.setAccessible(true);
        v.jumpTo(w.city.worldW() / 2, w.city.worldH() / 2);
        w.emit(Sfx.RIFLE, w.city.worldW() / 2 + 700, w.city.worldH() / 2);
        draw(v, size);
        java.util.List<?> caps = (java.util.List<?>) field(v, "captions");
        check(!caps.isEmpty(), "a caption for gunfire off the screen");
        // The Sound Lab draws, and plays what's tapped.
        menu.open(Menu.SOUNDS);
        draw(v, size);
        java.lang.reflect.Method act = Menu.class.getDeclaredMethod("act", int.class);
        act.setAccessible(true);
        act.invoke(menu, 6000 + Sfx.PANIC);
        check(snd.lastPlayed == Sfx.PANIC, "the Sound Lab plays a sound");
        java.lang.reflect.Field pid = Menu.class.getDeclaredField("previewId");
        pid.setAccessible(true);
        check(pid.getInt(menu) == Sfx.PANIC, "and shows which one's playing");
        act.invoke(menu, 7000 + Sfx.LOOP_BIRDS);
        draw(v, size);
        check(snd.loopGain[Sfx.LOOP_BIRDS] > 0.3f, "and an ambience bed");
        check(v.drawErrors == 0, "drawn without an error (" + v.drawErrors + ")");
    }

    static void visuals() throws Exception {
        CityConfig c = cfg(CityConfig.SMALL, 61);
        World w = new World(c);
        w.populate(c);
        Visuals fx = new Visuals();
        float cx = w.city.worldW() / 2, cy = w.city.worldH() / 2;
        float vx0 = cx - 400, vy0 = cy - 400, vx1 = cx + 400, vy1 = cy + 400;
        // A blast on the screen: light, a shockwave, a crater, debris, sparks, smoke, a ping on the map.
        w.evCount = 0;
        w.emit(Sfx.EXPLOSION, cx, cy);
        fx.events(w, vx0, vy0, vx1, vy1);
        check(!fx.lights.isEmpty() && !fx.waves.isEmpty() && fx.craters.size() == 1 && !fx.debris.isEmpty() && !fx.sparks.isEmpty()
                && !fx.smoke.isEmpty() && fx.pings.size() == 1, "a blast's light, shockwave, crater, debris, sparks, smoke and ping");
        check(fx.flash > 0, "a flash when it's in the middle of the screen");
        // Far off the screen: only the ping.
        w.evCount = 0;
        w.emit(Sfx.EXPLOSION, cx + 3000, cy);
        int before = fx.count();
        fx.events(w, vx0, vy0, vx1, vy1);
        check(fx.count() == before && fx.pings.size() == 2, "one off the screen: just its ping on the map");
        // A while on: the flash and light gone, the crater kept, debris come down.
        for (int k = 0; k < 90; k++) fx.update(w, 1 / 30f, vx0, vy0, vx1, vy1, null);
        check(fx.flash == 0 && fx.waves.isEmpty() && fx.craters.size() == 1, "the flash and shockwave gone, the crater stays");
        boolean landed = true;
        for (float[] d : fx.debris) landed &= d[2] <= 0;
        check(landed, "the debris has come down");
        // A wreck burns.
        Fleet.Vehicle car = w.fleet.vehicles.isEmpty() ? null : w.fleet.vehicles.get(0);
        if (car != null) {
            car.broken = true;
            fx.lights.clear();
            fx.sparks.clear();
            float wx0 = car.x - 400, wy0 = car.y - 400;
            for (int k = 0; k < 60; k++) fx.update(w, 1 / 30f, wx0, wy0, wx0 + 800, wy0 + 800, null);
            check(!fx.sparks.isEmpty() || !fx.lights.isEmpty(), "a wreck burning");
        }
        // Low effects: far less.
        Visuals low = new Visuals();
        low.quality = 0;
        w.evCount = 0;
        for (int k = 0; k < 20; k++) w.emit(Sfx.EXPLOSION, cx + k, cy);
        low.events(w, vx0, vy0, vx1, vy1);
        Visuals high = new Visuals();
        high.events(w, vx0, vy0, vx1, vy1);
        check(low.count() < high.count() / 2, "Low draws far less (" + low.count() + " / " + high.count() + ")");
        // A new city: nothing carried over.
        World w2 = new World(c);
        w2.evCount = 0;
        fx.events(w2, vx0, vy0, vx1, vy1);
        check(fx.craters.isEmpty() && fx.count() == 0, "a new city starts clean");
        // Drawn, all of it: the ground, above, and the screen (with and without the bars).
        android.graphics.Bitmap bm = android.graphics.Bitmap.createBitmap(800, 800, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas cv = new android.graphics.Canvas(bm);
        fx.events(w, vx0, vy0, vx1, vy1);
        w.evCount = 0;
        w.emit(Sfx.EXPLOSION, cx, cy);
        fx.events(w, vx0, vy0, vx1, vy1);
        fx.drawGround(cv, vx0, vy0, vx1, vy1);
        fx.drawAbove(cv, vx0, vy0, vx1, vy1);
        fx.hurt = 1;
        fx.daze = 1;
        for (int m = 0; m < 2; m++) fx.drawScreen(cv, 800, 800, 0.9f, m == 0);
        check(true, "drawn");
    }

    static void firesAndMeter() throws Exception {
        CityConfig c = cfg(CityConfig.TINY, 96);
        World w = new World(c);
        w.populate(c);
        w.update(1 / 30f);
        // A runner on its own, nothing to chase: still at a run, not a shamble.
        float[] p = w.city.findWalkable(w.city.worldW() / 2, w.city.worldH() / 2);
        // Fires on the ground are drawn with the ground, under people.
        Visuals fx = new Visuals();
        fx.events(w, p[0] - 300, p[1] - 300, p[0] + 300, p[1] + 300);
        w.evCount = 0;
        w.emit(Sfx.EXPLOSION, p[0], p[1]);
        fx.events(w, p[0] - 300, p[1] - 300, p[0] + 300, p[1] + 300);
        w.evCount = 0;
        for (int k = 0; k < 60; k++) fx.update(w, 1 / 30f, p[0] - 300, p[1] - 300, p[0] + 300, p[1] + 300, null);
        int ground = 0, above = 0;
        for (float[] s2 : fx.sparks) if (s2[8] > 0) ground++; else above++;
        for (float[] l : fx.lights) if (l[8] > 0) ground++;
        check(ground > 0, "where something blew up, the fire after is on the ground (" + ground + " ground, " + above + " in the air)");
        android.graphics.Bitmap bm = android.graphics.Bitmap.createBitmap(600, 600, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas cv = new android.graphics.Canvas(bm);
        fx.drawGround(cv, p[0] - 300, p[1] - 300, p[0] + 300, p[1] + 300);
        fx.drawAbove(cv, p[0] - 300, p[1] - 300, p[0] + 300, p[1] + 300);
        // The war meter: the same size however long the line of figures above it.
        GameView v = new GameView(new android.app.Activity());
        int[] size = {1080, 2000};
        v.layout(size[0], size[1]);
        Menu menu = (Menu) field(v, "menu");
        CityConfig gc = cfg(CityConfig.TINY, 97);
        gc.v[CityConfig.OPT_ZOMBIES] = 3;
        v.startGame(gc);
        v.awaitLoad();
        for (int i = 0; i < 3; i++) draw(v, size);
        menu.screen = Menu.NONE;
        World gw = (World) field(v, "world");
        gw.outbreak = true;
        draw(v, size);
        android.graphics.RectF meter = (android.graphics.RectF) field(v, "meterRect");
        float dp = (Float) field(v, "dp");
        check(meter.width() > 150 * dp && meter.width() < 260 * dp, "the war meter is a set size (" + meter.width() / dp + " dp)");
        check(v.drawErrors == 0, "drawn without an error");
    }

    static void overlays() throws Exception {
        GameView v = new GameView(new android.app.Activity());
        int[] size = {1080, 2400};
        v.layout(size[0], size[1]);
        CityConfig c = new CityConfig();
        c.v[CityConfig.OPT_SIZE] = CityConfig.TINY;
        c.v[CityConfig.OPT_ZOMBIES] = 3;
        v.startGame(c);
        v.awaitLoad();
        draw(v, size);
        ((Menu) field(v, "menu")).screen = Menu.NONE;
        World w = (World) field(v, "world");
        for (int f = 0; f < 60; f++) w.update(1 / 30f);
        // Overlays: one at a time, each worked out from the grid and drawn.
        Overlays o = v.overlays;
        o.cycle();
        draw(v, size);
        check(o.mode == Overlays.HEAT && o.found > 0 && o.level != null, "infection heat from the dead on the grid (" + o.found + ")");
        o.cycle();
        draw(v, size);
        int covered = 0;
        for (byte b : o.level) if (b == 1) covered++;
        check(o.mode == Overlays.REACH && o.found > 0 && covered > 0, "police/army reach from where they are (" + o.found + " cells, " + covered + " covered)");
        o.cycle();
        check(o.mode == Overlays.OFF, "and off again");
    }

    static void mapTiles() throws Exception {
        CityConfig c = cfg(CityConfig.MEDIUM, 2210);
        World w = new World(c);
        w.populate(c);
        // The map picture: tiles, no one big picture kept.
        City city = w.city;
        check(city.mapTiles != null && city.bitmap == null && city.mapTiles.length == city.mapCols * city.mapRows && city.mapCols > 1,
                "the map is kept in tiles (" + city.mapCols + " x " + city.mapRows + ")");
        android.graphics.Bitmap out = android.graphics.Bitmap.createBitmap(200, 200, android.graphics.Bitmap.Config.ARGB_8888);
        city.drawMap(new android.graphics.Canvas(out), 0, 0, -1, -1, new android.graphics.RectF(0, 0, 200, 200), null);
        int lit = 0;
        for (int y = 0; y < 200; y += 10) for (int x = 0; x < 200; x += 10) if ((out.img.getRGB(x, y) & 0xFFFFFF) != 0) lit++;
        check(lit > 300, "and drawn whole from them (" + lit + " of 400 points)");
    }

    /** (28.2) The campaign is gone: no button for it, nothing of it kept on the phone, and the sandbox has every tool. */
    static void noCampaign() throws Exception {
        GameView v = new GameView(new android.app.Activity());
        int[] size = {1080, 2000};
        v.layout(size[0], size[1]);
        Menu menu = (Menu) field(v, "menu");
        menu.open(Menu.MAIN);
        draw(v, size);
        boolean campaign = false;
        for (Object b : (java.util.List<?>) field(menu, "buttons")) {
            String label = (String) field(b, "label");
            campaign |= label != null && label.toLowerCase().contains("campaign");
        }
        check(!campaign, "no Campaign on the main menu");
        // An old phone's campaign stars, veterans and perks are let go of; the records are kept.
        android.app.Activity a = new android.app.Activity();
        android.content.SharedPreferences prefs = a.getSharedPreferences("records", 0);
        prefs.edit().putInt("mission0", 3).putInt("mission1", 2).putInt("zombiesOnly", 1).putString("veterans", ",1;").putInt("perkMask", 5)
                .putInt("rec4", 77).apply();
        Records rec = new Records(a);
        check(!prefs.contains("mission0") && !prefs.contains("mission1") && !prefs.contains("veterans") && !prefs.contains("perkMask")
                && !prefs.contains("zombiesOnly") && rec.record(4) == 77, "the campaign's left on the phone is cleared, the records kept");
        // A game: every sandbox tool on the bar, the airstrike's wait shown, Clear on the button.
        CityConfig gc = cfg(CityConfig.TINY, 83);
        gc.v[CityConfig.OPT_ZOMBIES] = 2;
        v.startGame(gc);
        v.awaitLoad();
        for (int i = 0; i < 3; i++) draw(v, size);
        menu.screen = Menu.NONE;
        java.lang.reflect.Method hidden = GameView.class.getDeclaredMethod("hiddenTool", int.class);
        hidden.setAccessible(true);
        check(!(Boolean) hidden.invoke(v, 2) && !(Boolean) hidden.invoke(v, 6) && !(Boolean) hidden.invoke(v, 3),
                "people, the dead and police to put down");
        for (int i = 0; i < 4; i++) draw(v, size);
        check(v.drawErrors == 0, "drawn without an error (" + v.drawErrors + ")");
        // The report at the end of a game is the after-action report.
        World gw = (World) field(v, "world");
        menu.open(Menu.REPORT);
        draw(v, size);
        check(menu.screen == Menu.REPORT && v.drawErrors == 0 && gw.difficulty == Director.NORMAL, "the report draws (" + v.drawErrors + ")");
    }

    /** (28.2) No evacuation convoys: none on the Events tool, none sent by the city, and one in an old save goes home empty. */
    static void noEvacConvoys() throws Exception {
        java.lang.reflect.Field names = GameView.class.getDeclaredField("EVENT_NAMES");
        names.setAccessible(true);
        boolean evac = false;
        for (String n : (String[]) names.get(null)) evac |= n.toLowerCase().contains("evac");
        check(!evac, "no evacuation convoy on the Events tool");
        CityConfig c = cfg(CityConfig.SMALL, 91);
        World w = new World(c);
        w.populate(c);
        // A safe zone full of people, and the city's own convoys looked at: none comes for them.
        City.Facility st = null;
        for (City.Facility f : w.city.facilities) if (f.kind == City.FACILITY_POLICE && !f.fallen) { st = f; break; }
        check(st != null, "a police station");
        st.ammo = 10000;
        w.outbreak = true;
        w.outbreakTime = 300;
        for (int k = 0; k < 6; k++) w.convoys.update(5);
        for (Convoys.Column col : w.convoys.columns) check(col.purpose == Convoys.SUPPLY, "only supply convoys");
        // An evacuation convoy as an old save has it: it lets its people off and turns for home, empty.
        Convoys.Column col = w.convoys.supply(null, null, st, 300);
        check(col != null, "a column on its way");
        for (int f = 0; f < 30 * 20 && col.vs.isEmpty(); f++) w.update(1 / 30f);
        check(!col.vs.isEmpty(), "its vehicles out");
        Entity rider = null;
        for (Entity e : w.entities) if (!e.dead && e.type == Entity.CIVILIAN && e.ride == null && w.city.insideOf(e.x, e.y) == null) { rider = e; break; }
        Fleet.Vehicle bus = col.vs.get(0);
        rider.dead = true;
        rider.removed = true;
        bus.riders.add(rider);
        w.entities.remove(rider);
        rider.listed = false;
        col.purpose = Convoys.EVAC;
        col.aboard = 1;
        w.convoys.dropEvacuations();
        check(col.purpose == Convoys.SUPPLY && col.state == Convoys.LEAVING && bus.riders.isEmpty() && col.cargo == 0,
                "turned for home (" + col.purpose + ", state " + col.state + ", " + bus.riders.size() + " aboard)");
        check(!rider.dead && !rider.removed && w.entities.contains(rider), "and its passenger let off");
        for (int f = 0; f < 30 * 5; f++) w.update(1 / 30f);
        check(true, "and it runs on");
    }

    static void difficulty() throws Exception {
        // The sandbox: always Normal, and nothing comes in by itself however long it goes on.
        CityConfig c = new CityConfig();
        c.v[CityConfig.OPT_SIZE] = CityConfig.TINY;
        c.v[CityConfig.OPT_ZOMBIES] = 3;
        c.difficulty = Director.NIGHTMARE;
        c.seed = 73;
        World sb = new World(c);
        sb.populate(c);
        check(sb.difficulty == Director.NORMAL && sb.director.level == Director.NORMAL, "a sandbox game is always Normal");
        int zs = sb.zombieCount(), crates = sb.pickups.size();
        for (int t = 0; t < 1500; t++) {
            sb.outbreak = true;
            sb.outbreakTime = 400 + t;
            sb.warBalance = 0.1f;
            sb.director.stress = 0;
            sb.director.tick(sb);
        }
        sb.recount();
        check(sb.director.hordes == 0 && sb.director.stragglers == 0 && sb.director.sieges == 0 && sb.zombieCount() == zs && sb.pickups.size() == crates
                && sb.incoming.isEmpty(), "nothing comes into a sandbox game by itself: no hordes, stragglers, sieges or supplies");
        // The director still follows how it's going there (for the line at the top of the screen).
        boolean peaked = false, rested = false;
        sb.director.tick(sb);
        check(sb.director.phase == Director.BUILD, "building");
        for (int t = 0; t < 60; t++) {
            sb.civiliansLost += 6;
            sb.director.tick(sb);
            peaked |= sb.director.phase == Director.PEAK;
        }
        check(peaked, "a peak when people are dying fast");
        for (int t = 0; t < 120; t++) {
            sb.director.stress = Math.min(sb.director.stress, 0.3f);
            sb.director.tick(sb);
            rested |= sb.director.phase == Director.RESPITE;
        }
        check(rested && sb.director.phase == Director.BUILD, "then a respite, then building again");
        // A sandbox save that says Hard loads as Normal.
        File file;
        sb.city.cfg.difficulty = Director.HARD;
        file = File.createTempFile("zcs", ".dat");
        SaveGame.save(sb, file);
        World sl = SaveGame.load(file);
        file.delete();
        check(sl.difficulty == Director.NORMAL && sl.director.level == Director.NORMAL, "a sandbox save loads as Normal");
        // The score: kills count.
        sb.zombiesKilled = 100;
        sb.civiliansLost = 0;
        sb.copsLost = sb.soldiersLost = 0;
        sb.alphasKilled = 0;
        sb.warResult = 0;
        check(sb.score() == 1000, "a hundred kills score a thousand (" + sb.score() + ")");
        // No difficulty to choose on the New Game screen.
        boolean offered = false;
        CityConfig oc = new CityConfig();
        for (int i = 0; i < oc.count(); i++) offered |= oc.label(i).toLowerCase().contains("difficult");
        check(!offered && oc.difficulty == Director.NORMAL && oc.copy().difficulty == Director.NORMAL, "no difficulty on the New Game screen");
    }

    /** In a game: the last of the dead pointed out, an alpha drawn, wire on the ground. */
    static void lastOfTheDead() throws Exception {
        GameView v = new GameView(new android.app.Activity());
        int[] size = {1080, 2000};
        v.layout(size[0], size[1]);
        Menu menu = (Menu) field(v, "menu");
        CityConfig gc = cfg(CityConfig.TINY, 83);
        gc.v[CityConfig.OPT_ZOMBIES] = 2;
        v.startGame(gc);
        v.awaitLoad();
        for (int i = 0; i < 3; i++) draw(v, size);
        menu.screen = Menu.NONE;
        World gw = (World) field(v, "world");
        Entity gz = null;
        for (Entity e : gw.entities) if (e.isZombie()) { gz = e; break; }
        gz.alpha = true;
        gw.layWire(gz.x + 10, gz.y);
        gw.outbreak = true;
        gw.outbreakTime = 300;
        WM mop = wm("mopUp");
        mop.setAccessible(true);
        mop.invoke(gw);
        check(gw.lastDead.length > 0, "the last of them known");
        for (int i = 0; i < 4; i++) draw(v, size);
        v.jumpTo(gz.x, gz.y);
        for (int i = 0; i < 4; i++) draw(v, size);
        check(v.drawErrors == 0, "drawn without an error (" + v.drawErrors + ")");
    }
}
