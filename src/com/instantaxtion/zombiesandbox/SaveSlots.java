package com.instantaxtion.zombiesandbox;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.GameView.*;

/**
 * ((27.3) Out of GameView.) The save files: the autosave and the numbered slots, their descriptions, saving, loading
 * and deleting.
 */
final class SaveSlots {
    final GameView gv;

    SaveSlots(GameView gv) {
        this.gv = gv;
    }


    java.io.File saveFile() {
        return new java.io.File(gv.getContext().getFilesDir(), "save.dat");
    }

    java.io.File slotFile(int slot) {
        return slot == 0 ? saveFile() : new java.io.File(gv.getContext().getFilesDir(), "save" + slot + ".dat");
    }

    public boolean saveSlot(int slot) {
        if (!gv.hasGame || gv.world == null) return false;
        gv.slotKnown[slot] = false;
        try {
            SaveGame.save(gv.world, slotFile(slot));
            // A short description to show in the slot list without loading the whole game.
            int t = (int) gv.world.time;
            String label = gv.world.city.cfg.mapLabel();
            String info = gv.world.city.name + (label != null ? " (" + label + ")" : "") + "\n"
                    + World.clock(t) + " played, " + World.count(gv.world.humans, "person", "people") + ", " + World.count(gv.world.zombies, "zombie", "zombies") + "\n"
                    + new java.text.SimpleDateFormat("d MMM HH:mm").format(new java.util.Date());
            java.io.FileOutputStream out = new java.io.FileOutputStream(slotFile(slot).getPath() + ".info");
            try {
                out.write(info.getBytes("UTF-8"));
            } finally {
                out.close();
            }
            return true;
        } catch (OutOfMemoryError oom) {
            // ((13.3) Out of memory saving (on the way out of the app): let go of what can be worked out again and
            // say it didn't save, rather than crash.)
            gv.trimMemory();
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public String slotInfo(int slot) {
        if (!gv.slotKnown[slot]) {
            gv.slotText[slot] = readSlotInfo(slot);
            gv.slotKnown[slot] = true;
        }
        return gv.slotText[slot];
    }

    public boolean deleteSlot(int slot) {
        java.io.File f = slotFile(slot), info = new java.io.File(f.getPath() + ".info");
        boolean ok = !f.exists() || f.delete();
        info.delete();
        gv.slotKnown[slot] = false;
        return ok;
    }

    String readSlotInfo(int slot) {
        java.io.File f = new java.io.File(slotFile(slot).getPath() + ".info");
        if (!slotFile(slot).exists()) return null;
        if (!f.exists()) return "Saved game";
        try {
            java.io.FileInputStream in = new java.io.FileInputStream(f);
            byte[] b = new byte[(int) f.length()];
            int n = in.read(b);
            in.close();
            return new String(b, 0, Math.max(0, n), "UTF-8");
        } catch (Exception e) {
            return "Saved game";
        }
    }

    public boolean loadSlot(int slot) {
        java.io.File f = slotFile(slot);
        if (!f.exists()) return false;
        // (Loaded on the loader thread; if it fails the screen says so.)
        gv.loadSave(f);
        return true;
    }
}
