package com.instantaxtion.zombiesandbox;

import java.io.File;
import java.lang.reflect.Field;

/** Screens: every screen draws, the game view, the campaign. */
final class UiTests extends TestKit {
    static void register() {
        test("every screen draws", FAST, 45, UiTests.class,
                "everyScreen", "gamesLetGo", "recordsAndSettings");
        test("the game view: the after-action report, sound, visuals, overlays, the war meter, the map in tiles", FULL, 30, UiTests.class,
                "report", "sound", "visuals", "firesAndMeter", "overlays", "mapTiles");
        test("campaign: missions, budgets, objectives, difficulty, perks, veterans and the commander's tools", FULL, 25, UiTests.class,
                "missions", "difficulty", "objectives", "perksAndHolding", "perkTreeAndRetake");
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

    static void missions() throws Exception {
        Campaign.Mission m0 = Campaign.MISSIONS[0];
        CityConfig c = mission(0);
        check(c.mission == 0 && c.zombies() == m0.foes, "the mission's city and enemy");
        World w = new World(c);
        w.populate(c);
        Campaign.begin(w, "");
        w.recount();
        check(w.mission() == m0, "it's a mission");
        // (On foot and in the cars parked at the stations, each once: w.counts has the crews already.)
        int police = policeNow(w);
        check(police <= m0.cops && police >= m0.cops / 2, "the police cut to the mission's (" + police + " for " + m0.cops + ")");
        check(w.counts[Entity.SOLDIER] <= m0.soldiers, "and the soldiers (" + w.counts[Entity.SOLDIER] + ")");
        check(w.dispatch.policeReserve == 0 && w.dispatch.squadReserve == 0, "no help from outside");
        // The budget: start money, spent, earned.
        check(Campaign.budget(w) == m0.budget, "starts with the mission's money");
        check(Campaign.spend(w, 10) && Campaign.budget(w) == m0.budget - 10, "spending takes it off");
        check(!Campaign.spend(w, 100000), "can't spend what isn't there");
        int before = Campaign.budget(w);
        w.zombiesKilled += 5;
        check(Campaign.budget(w) > before, "kills earn money");
        float[] at = Campaign.arrival(w, true, w.city.worldW() / 2, w.city.worldH() / 2);
        check(at != null, "police have somewhere to come in from");
        // Run out the clock: failed.
        w.missionStart = w.time - m0.limit - 1;
        WM life = wm("cityLife");
        life.setAccessible(true);
        life.invoke(w);
        check(w.missionResult == 2 && "Out of time.".equals(w.missionWhy), "out of time, the mission's failed (" + w.missionResult + ", " + w.missionWhy + ")");
        // Won: every zombie down.
        World w2 = new World(mission(0));
        w2.populate(w2.city.cfg);
        Campaign.begin(w2, "");
        for (int f = 0; f < 30 * 40 && w2.missionResult == 0; f++) {
            for (Entity e : w2.entities) if (e.isUndead()) e.hp = 0;
            w2.update(1 / 30f);
            w2.evCount = 0;
        }
        check(w2.missionResult == 1 && w2.missionStars >= 1, "cleared: won with stars (" + w2.missionResult + ", " + w2.missionStars + ")");
        // Saved and loaded.
        w2.spent = 37;
        File file = File.createTempFile("zcs", ".dat");
        SaveGame.save(w2, file);
        World l = SaveGame.load(file);
        file.delete();
        check(l.mission() == m0 && l.spent == 37 && l.missionResult == 1 && l.missionStars == w2.missionStars, "the mission's kept in a save");
        // Veterans: a ranked officer goes on the roster and comes back in the next mission.
        Entity hero = null;
        for (Entity e : w2.entities) if (!e.dead && e.type == Entity.COP) { hero = e; break; }
        // (Or one sitting in a car.)
        for (Fleet.Vehicle v : w2.fleet.vehicles) for (Entity e : v.crew) if (hero == null && e.type == Entity.COP) hero = e;
        hero.kills = 20;
        String roster = Campaign.veterans(w2);
        check(Campaign.rosterSize(roster) >= 1 && roster.contains("," + hero.nameSeed + ";"), "a ranked officer's a veteran (" + roster + ")");
        World w3 = new World(mission(1));
        w3.populate(w3.city.cfg);
        Campaign.begin(w3, roster);
        boolean back = false;
        for (Entity e : w3.entities) back |= e.type == Entity.COP && e.nameSeed == hero.nameSeed && e.kills == 20;
        check(back, "and is back in the next mission");
        // The campaign's progress on the phone.
        Records rec = new Records(new android.app.Activity());
        check(rec.unlocked(0) && !rec.unlocked(1), "only the first mission open at first");
        rec.missionWon(0, 2, roster);
        check(rec.unlocked(1) && rec.missionStars(0) == 2 && rec.veterans().equals(roster), "winning opens the next and keeps the stars and the roster");
        rec.missionWon(0, 1, "");
        check(rec.missionStars(0) == 2, "the best stars kept");
        // The commander's screen: only their tools, buying units that come in and are sent to the spot.
        GameView v = new GameView(new android.app.Activity());
        int[] size = {1080, 2000};
        v.layout(size[0], size[1]);
        Menu menu = (Menu) field(v, "menu");
        v.startGame(mission(2));
        v.awaitLoad();
        for (int k = 0; k < 3; k++) draw(v, size);
        menu.screen = Menu.NONE;
        World gw = (World) field(v, "world");
        check(gw.mission() == Campaign.MISSIONS[2], "the mission's started from the view");
        java.lang.reflect.Method hidden = GameView.class.getDeclaredMethod("hiddenTool", int.class);
        hidden.setAccessible(true);
        check((Boolean) hidden.invoke(v, 2) && (Boolean) hidden.invoke(v, 6) && !(Boolean) hidden.invoke(v, 3) && !(Boolean) hidden.invoke(v, 1),
                "no people or enemy to spawn, police and orders there");
        java.lang.reflect.Field tool = GameView.class.getDeclaredField("tool");
        tool.setAccessible(true);
        tool.setInt(v, 3);
        gw.recount();
        int cops = gw.counts[Entity.COP], money = Campaign.budget(gw);
        java.lang.reflect.Method cmd = GameView.class.getDeclaredMethod("commandAt", float.class, float.class);
        cmd.setAccessible(true);
        float tx = 0, ty = 0;
        for (Entity e : gw.entities)
            if (!e.dead && e.type == Entity.CIVILIAN && e.ride == null && gw.city.insideOf(e.x, e.y) == null) {
                tx = e.x;
                ty = e.y;
                break;
            }
        cmd.invoke(v, tx, ty);
        gw.recount();
        check(gw.counts[Entity.COP] == cops + 1 && Campaign.budget(gw) == money - Campaign.COP_COST[0], "a cop bought (" + cops + " -> "
                + gw.counts[Entity.COP] + ", $" + money + " -> $" + Campaign.budget(gw) + ")");
        Entity newest = null;
        for (Entity e : gw.entities) if (e.type == Entity.COP && e.task == Dispatch.T_MOVE) newest = e;
        check(newest != null && World.hypot(newest.postX - tx, newest.postY - ty) < 120, "and sent to the spot");
        gw.spent = 1000000;
        cmd.invoke(v, tx, ty);
        gw.recount();
        check(gw.counts[Entity.COP] == cops + 1, "no money, no more");
        for (int k = 0; k < 3; k++) draw(v, size);
        // The campaign screen and the mission report draw, and a finished mission opens its report.
        menu.open(Menu.CAMPAIGN);
        draw(v, size);
        menu.screen = Menu.NONE;
        Campaign.finish(gw, 1, null);
        gw.warBannerTime = 0;
        draw(v, size);
        check(menu.screen == Menu.REPORT, "the report's up at the end");
        draw(v, size);
        check(v.drawErrors == 0, "drawn without an error (" + v.drawErrors + ")");
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
        Entity sz = null;
        for (Entity e : sb.entities) if (e.type == Entity.ZOMBIE) { sz = e; break; }
        float normalHp = sz.maxHp;
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
        // A mission on Hard (Overrun): tougher dead, stragglers, a horde, in time a siege.
        CityConfig mc = mission(5);
        World w = new World(mc);
        w.setMaxPopulation(3000);
        w.populate(mc);
        Campaign.begin(w, "");
        check(w.difficulty == Director.HARD && w.director.level == Director.HARD, "a mission has its own difficulty");
        Entity mz = null;
        for (Entity e : w.entities) if (e.type == Entity.ZOMBIE) { mz = e; break; }
        check(mz.maxHp > normalHp * 1.1f, "the dead are tougher on Hard (" + normalHp + " -> " + mz.maxHp + ")");
        int before = w.zombieCount();
        for (int t = 0; t < 700 && (w.director.hordes == 0 || w.director.sieges == 0 || w.director.stragglers == 0); t++) {
            w.outbreak = true;
            w.director.stress = 0;
            w.director.tick(w);
        }
        w.recount();
        check(w.director.stragglers >= 1 && w.director.hordes >= 1 && w.director.sieges >= 1 && w.zombieCount() > before,
                "stragglers, a horde and a siege come in (" + w.director.stragglers + ", " + w.director.hordes + ", " + w.director.sieges + ")");
        // Kept through a save, and a sandbox save that says Hard loads as Normal.
        File file = File.createTempFile("zcs", ".dat");
        SaveGame.save(w, file);
        World l = SaveGame.load(file);
        file.delete();
        check(l.difficulty == Director.HARD && l.director.level == Director.HARD && l.mission() == w.mission(), "a mission's difficulty is kept in a save");
        sb.city.cfg.difficulty = Director.HARD;
        file = File.createTempFile("zcs", ".dat");
        SaveGame.save(sb, file);
        World sl = SaveGame.load(file);
        file.delete();
        check(sl.difficulty == Director.NORMAL && sl.director.level == Director.NORMAL, "a sandbox save loads as Normal");
        // The score: kills count, and more in a harder mission.
        sb.zombiesKilled = w.zombiesKilled = 100;
        sb.civiliansLost = w.civiliansLost = 0;
        sb.copsLost = w.copsLost = sb.soldiersLost = w.soldiersLost = 0;
        sb.alphasKilled = w.alphasKilled = 0;
        sb.warResult = w.warResult = 0;
        check(sb.score() == 1000 && w.score() > sb.score(), "a hundred kills score, more on Hard (" + sb.score() + ", " + w.score() + ")");
        // No difficulty to choose on the New Game screen.
        boolean offered = false;
        CityConfig oc = new CityConfig();
        for (int i = 0; i < oc.count(); i++) offered |= oc.label(i).toLowerCase().contains("difficult");
        check(!offered && oc.difficulty == Director.NORMAL && oc.copy().difficulty == Director.NORMAL, "no difficulty on the New Game screen");
    }

    static void objectives() throws Exception {
        check(Campaign.MISSIONS.length >= 8, "the missions");
        for (int i = 0; i < Campaign.MISSIONS.length; i++) {
            Campaign.Mission m = Campaign.MISSIONS[i];
            CityConfig mc = mission(i);
            check(mc.mission == i && mc.difficulty == m.difficulty && mc.exodus == m.exodus, m.title + ": the mission's rules");
            check(m.goal >= 0 && m.goal < Campaign.GOAL_NAMES.length, m.title + " has an objective");
        }
        // One small city stands in for each counted objective.
        CityConfig c = mission(5);
        World w = new World(c);
        w.setMaxPopulation(1800);
        w.populate(c);
        Campaign.begin(w, "");
        check(w.difficulty == Director.HARD, "Overrun is on Hard");
        for (int id : new int[]{3, 6}) {
            Campaign.Mission m = Campaign.MISSIONS[id];
            w.city.cfg.mission = id;
            w.missionResult = 0;
            w.missionStart = w.time;
            w.fled = 0;
            w.zombiesKilled = 0;
            check(w.mission() == m && !Campaign.goalText(w).isEmpty() && Campaign.progress(w).startsWith("0 / " + m.goalN), m.title + " says what it wants");
            Campaign.tick(w);
            check(w.missionResult == 0, m.title + ": not won yet");
            if (m.goal == Campaign.G_EVAC) w.fled = m.goalN;
            else w.zombiesKilled = m.goalN;
            Campaign.tick(w);
            check(w.missionResult == 1 && w.missionStars >= 1, m.title + ": won on its objective (" + w.missionResult + ")");
        }
        // And they run out of time like the rest.
        w.city.cfg.mission = 6;
        w.missionResult = 0;
        w.zombiesKilled = 0;
        w.missionStart = w.time - Campaign.MISSIONS[6].limit - 1;
        Campaign.tick(w);
        check(w.missionResult == 2, "out of time");
        // The campaign screen draws with all of them.
        GameView v = new GameView(new android.app.Activity());
        int[] size = {1080, 2000};
        v.layout(size[0], size[1]);
        Menu menu = (Menu) field(v, "menu");
        for (int i = 0; i < 3; i++) draw(v, size);
        menu.open(Menu.CAMPAIGN);
        java.lang.reflect.Field sc = Menu.class.getDeclaredField("scroll");
        sc.setAccessible(true);
        for (int i = 0; i < 4; i++) {
            sc.setFloat(menu, i * 500);
            draw(v, size);
        }
        menu.open(Menu.SETUP);
        for (int i = 0; i < 5; i++) {
            sc.setFloat(menu, i * 500);
            draw(v, size);
        }
        check(v.drawErrors == 0, "the campaign and setup screens draw (" + v.drawErrors + ")");
    }

    static void perksAndHolding() throws Exception {
        check(Perks.count(0) == 0 && Perks.count(Perks.legacy(9)) == 3 && Perks.points(24) == 6 && Perks.NAMES.length == Perks.COUNT
                && Perks.INFO.length == Perks.COUNT, "perks by stars");
        check(Campaign.MISSIONS.length >= 12, "the missions (" + Campaign.MISSIONS.length + ")");
        for (Campaign.Mission m : Campaign.MISSIONS) check(m.goal >= 0 && m.goal < Campaign.GOAL_NAMES.length, m.title + " has an objective");
        CityConfig c = mission(8);
        World w = new World(c);
        w.setMaxPopulation(1800);
        w.populate(c);
        Campaign.Mission m = Campaign.MISSIONS[8];
        w.setMaxPopulation(2400);
        // No perks without stars; all of them with enough.
        Campaign.begin(w, "", 0);
        check(w.perkStars == 0 && !Perks.has(w, Perks.ARMOUR) && Campaign.budget(w) == m.budget && Campaign.placeCost(w) == Campaign.PLACE_COST
                && Campaign.eventCost(w) == Campaign.EVENT_COST, "no stars, no perks");
        Entity any = null;
        for (Entity e : w.entities) if (!e.dead && e.type == Entity.COP && e.ride == null && w.city.insideOf(e.x, e.y) == null) { any = e; break; }
        Entity plain = w.spawnCop(0, any.x + 6, any.y);
        Campaign.begin(w, "", (1 << Perks.COUNT) - 1);
        check(Perks.has(w, Perks.OLD_HANDS) && Campaign.budget(w) == m.budget * 6 / 5, "Logistics: a fifth more to start with");
        Entity tough = w.spawnCop(0, any.x - 6, any.y);
        check(plain != null && tough != null && Math.abs(tough.maxHp - plain.maxHp * 1.15f) < 0.5f, "Body armour (" + plain.maxHp + " -> " + tough.maxHp + ")");
        check(Campaign.placeCost(w)[9] == (Campaign.PLACE_COST[9] + 1) / 2 && Campaign.placeCost(w)[1] == Campaign.PLACE_COST[1]
                && Campaign.eventCost(w)[7] == Campaign.EVENT_COST[7] / 2 && Campaign.eventCost(w)[4] == Campaign.EVENT_COST[4] * 2 / 3
                && Campaign.eventCost(w)[9] == Campaign.EVENT_COST[9] * 2 / 3, "Quartermaster and Fire support take money off");
        float[] bar = w.placeBarricade(tough.x + City.T * 3, tough.y);
        check(bar == null || bar[2] > 300, "Sappers build tougher");
        // Never in the sandbox.
        w.city.cfg.mission = -1;
        check(!Perks.has(w, Perks.ARMOUR), "perks are for missions only");
        w.city.cfg.mission = 8;
        // Holding the station: lost once the dead have had it, with nobody armed there, for twenty seconds.
        City.Facility f = Campaign.site(w);
        check(f != null && Campaign.goalText(w).startsWith("Hold ") && m.goal == Campaign.G_DEFEND, "a station to hold");
        Campaign.begin(w, "", 0);
        Campaign.tick(w);
        check(w.missionResult == 0 && w.siteT == 0, "holding");
        // (Everyone of the city's gone from there: off the streets, out of the cars, out of the buildings.)
        for (Entity e : w.entities) {
            if (e.type == Entity.COP || e.type == Entity.SOLDIER || World.hypot(e.x - f.x, e.y - f.y) < 600 && !e.isZombie()) { e.removed = true; e.dead = true; }
            e.hasGun = false;
        }
        for (Fleet.Vehicle v : new java.util.ArrayList<Fleet.Vehicle>(w.fleet.vehicles)) w.fleet.discard(v);
        w.update(1 / 30f);
        float[] gate = w.city.findWalkable(f.x, f.y);
        java.util.ArrayList<Entity> at = new java.util.ArrayList<Entity>();
        for (int k = 0; k < 9; k++) {
            Entity z = w.spawn(Entity.ZOMBIE, gate[0] + (k % 3) * 6, gate[1] + (k / 3) * 6);
            if (z != null) at.add(z);
        }
        float[][] pos = new float[at.size()][];
        for (int k = 0; k < at.size(); k++) pos[k] = new float[]{at.get(k).x, at.get(k).y};
        check(at.size() >= 6, "the dead at the gate");
        for (int f2 = 0; f2 < 30 * 26 && w.missionResult == 0; f2++) {
            for (int k = 0; k < at.size(); k++) { at.get(k).x = pos[k][0]; at.get(k).y = pos[k][1]; at.get(k).hp = at.get(k).maxHp; }
            w.update(1 / 30f);
            w.evCount = 0;
        }
        check(w.missionResult == 2 && w.missionWhy != null && w.missionWhy.endsWith("has fallen."), "the station falls (" + w.missionResult + ", " + w.missionWhy + ", " + w.siteT + ", dead there " + w.countZombiesNear(f.x, f.y, Campaign.reach(f))
                + ", armed " + w.armedNear(f.x, f.y, Campaign.reach(f) + 30) + ")");
        // Keeping people alive.
        Campaign.Mission pm = Campaign.MISSIONS[9];
        w.city.cfg.mission = 9;
        w.missionResult = 0;
        w.missionStart = w.time - 30;
        w.warResult = 0;
        check(pm.goal == Campaign.G_PROTECT && Campaign.goalText(w).startsWith("Keep " + pm.goalN), "people to keep alive");
        w.humans = pm.goalN + 50;
        Campaign.tick(w);
        check(w.missionResult == 0 && Campaign.progress(w).startsWith((pm.goalN + 50) + " alive"), "enough alive");
        w.missionStart = w.time - pm.limit - 1;
        Campaign.tick(w);
        check(w.missionResult == 1, "and still at the end: won");
        w.missionResult = 0;
        w.missionStart = w.time - 30;
        w.humans = pm.goalN - 1;
        Campaign.tick(w);
        check(w.missionResult == 2 && w.missionWhy.startsWith("Too many lost"), "too many lost");
        // Saved with its perks.
        w.perkStars = 12;
        File file = File.createTempFile("zcs", ".dat");
        SaveGame.save(w, file);
        World l = SaveGame.load(file);
        file.delete();
        check(l.perkStars == 12, "the perks are kept in a save");
        // The campaign screen, with perks earned and one to come.
        GameView v = new GameView(new android.app.Activity());
        int[] size = {1080, 2000};
        v.layout(size[0], size[1]);
        Menu menu = (Menu) field(v, "menu");
        Records rec = (Records) field(v, "records");
        for (int i = 0; i < 4; i++) rec.missionWon(i, 3, "");
        check(rec.totalStars() == 12 && Perks.count(rec.perkMask()) == 4, "twelve stars, four perks");
        for (int i = 0; i < 3; i++) draw(v, size);
        menu.open(Menu.CAMPAIGN);
        java.lang.reflect.Field sc = Menu.class.getDeclaredField("scroll");
        sc.setAccessible(true);
        for (int i = 0; i < 6; i++) {
            sc.setFloat(menu, i * 500);
            draw(v, size);
        }
        check(v.drawErrors == 0, "the campaign screen draws with its perks (" + v.drawErrors + ")");
        // In a game: the last of the dead pointed out, an alpha drawn, wire on the ground.
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

    static void perkTreeAndRetake() throws Exception {
        // The perk tree: a perk needs the one before it, and points from stars.
        check(!Perks.open(0, 40, Perks.SAPPERS), "Sappers needs Body armour first");
        check(Perks.open(1 << Perks.ARMOUR, 40, Perks.SAPPERS), "and is open once Body armour is taken");
        check(!Perks.open(0, 3, Perks.ARMOUR), "no perk without a point (four stars)");
        check(Perks.legacy(9) == 7, "the perks earned before the tree are kept");
        // Retake the district: clear the named district and hold it.
        CityConfig rc = mission(13);
        World rw = new World(rc);
        rw.populate(rc);
        Campaign.begin(rw, "", 0);
        City.District d = Campaign.retakeDistrict(rw);
        check(d != null && rw.mission().goal == Campaign.G_RETAKE, "a mission to retake " + (d == null ? "?" : d.name));
        for (Entity e : rw.entities) if (e.isZombie()) e.hp = 0;
        rw.update(1 / 30f);
        for (int k = 0; k < Campaign.RETAKE_HOLD + 2 && rw.missionResult == 0; k++) {
            for (Entity e : rw.entities) if (e.isZombie() && rw.city.districtOf(e.x, e.y) == d) e.hp = 0;
            Campaign.tick(rw);
        }
        check(rw.missionResult == 1, "cleared and held: the mission is won");
    }
}
