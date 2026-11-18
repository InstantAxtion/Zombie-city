package com.instantaxtion.zombiesandbox;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;
import android.text.InputType;
import android.widget.EditText;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

public class MainActivity extends Activity {
    private GameView game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recordCrashes();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        game = new GameView(this);
        game.codePrompt = new GameView.CodePrompt() {
            @Override
            public void ask(String current) {
                askCode(current);
            }
        };
        game.shareHandler = new GameView.ShareHandler() {
            @Override
            public void share(Bitmap picture) {
                saveAndShare(picture);
            }
        };
        setContentView(game);
        showLastCrash();
    }

    // ------------------------------------------------------------------ (14.1) when the game closes unexpectedly

    private java.io.File crashFile() {
        return new java.io.File(getFilesDir(), "last_crash.txt");
    }

    /**
     * (14.1) If the game ever closes unexpectedly, what went wrong is written down first (the version, the phone, and
     * where in the code), so it can be shown the next time and passed on to get it fixed.
     */
    private void recordCrashes() {
        final Thread.UncaughtExceptionHandler before = Thread.getDefaultUncaughtExceptionHandler();
        final java.io.File file = crashFile();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                try {
                    java.io.StringWriter sw = new java.io.StringWriter();
                    java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                    pw.println("Zombie City Sandbox " + PatchNotes.VERSION + " (" + PatchNotes.VERSION_CODE + ")");
                    pw.println("Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + "), " + Build.MANUFACTURER + " " + Build.MODEL);
                    Runtime r = Runtime.getRuntime();
                    pw.println("Memory: " + ((r.totalMemory() - r.freeMemory()) >> 20) + " of " + (r.maxMemory() >> 20) + " MB");
                    pw.println("Thread: " + t.getName());
                    e.printStackTrace(pw);
                    pw.flush();
                    java.io.FileWriter out = new java.io.FileWriter(file);
                    out.write(sw.toString());
                    out.close();
                } catch (Throwable ignored) {
                    // (Nothing more can be done: let it close as it would have.)
                }
                if (before != null) before.uncaughtException(t, e);
            }
        });
    }

    /** What went wrong last time, if the game closed unexpectedly: shown once, with a button to copy it. */
    private void showLastCrash() {
        java.io.File file = crashFile();
        if (!file.exists()) return;
        final StringBuilder sb = new StringBuilder();
        try {
            java.io.BufferedReader in = new java.io.BufferedReader(new java.io.FileReader(file));
            String line;
            for (int n = 0; (line = in.readLine()) != null && n < 60; n++) sb.append(line).append('\n');
            in.close();
        } catch (java.io.IOException ignored) {
        }
        file.delete();
        if (sb.length() == 0) return;
        TextView text = new TextView(this);
        text.setText(sb.toString());
        text.setTextIsSelectable(true);
        text.setTextSize(11);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        text.setPadding(pad, pad / 2, pad, pad / 2);
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(text);
        new AlertDialog.Builder(this)
                .setTitle("The game closed unexpectedly last time")
                .setMessage("This is what went wrong. Copy it and pass it on so it can be fixed.")
                .setView(scroll)
                .setPositiveButton("Copy", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        android.content.ClipboardManager cb = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        if (cb != null) cb.setPrimaryClip(android.content.ClipData.newPlainText("Zombie City crash", sb.toString()));
                        Toast.makeText(MainActivity.this, "Copied", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private Bitmap pendingShot;

    /** Saves a screenshot to the photo gallery and opens the share sheet. */
    private void saveAndShare(Bitmap picture) {
        if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 29
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            pendingShot = picture;
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            return;
        }
        try {
            String url = MediaStore.Images.Media.insertImage(getContentResolver(), picture,
                    "Zombie City " + System.currentTimeMillis(), "Zombie City Sandbox screenshot");
            if (url == null) throw new IllegalStateException();
            Intent send = new Intent(Intent.ACTION_SEND);
            // ((12.0) The gallery keeps it as a JPEG: said to be a PNG, some apps wouldn't take it.)
            send.setType("image/jpeg");
            send.putExtra(Intent.EXTRA_STREAM, Uri.parse(url));
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, "Share screenshot"));
            Toast.makeText(this, "Screenshot saved to your photos", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't save the screenshot", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (requestCode == 1 && pendingShot != null && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED)
            saveAndShare(pendingShot);
        else if (requestCode == 1) Toast.makeText(this, "Can't save screenshots without storage permission", Toast.LENGTH_SHORT).show();
        pendingShot = null;
    }

    /** A native text box for typing (or copying) a city code. */
    private void askCode(String current) {
        final EditText input = new EditText(this);
        // ((12.0) A text keyboard: codes have letters and signs in them (the map, "!", the "~" of built changes), and
        // the phone keypad it had couldn't type them.)
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        input.setText(current);
        input.setSelectAllOnFocus(true);
        // ((12.0) One line, and Go (or Enter on a keyboard) uses the code, as the button does.)
        input.setSingleLine(true);
        input.setImeOptions(EditorInfo.IME_ACTION_GO);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("City code")
                .setMessage("Type a code to play that city, or share this one so friends can play it.")
                .setView(input)
                .setPositiveButton("Use code", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        game.cityCodeEntered(input.getText().toString());
                        hideSystemUi();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        hideSystemUi();
                    }
                })
                .show();
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN;
                if (actionId != EditorInfo.IME_ACTION_GO && !enter) return false;
                game.cityCodeEntered(input.getText().toString());
                dialog.dismiss();
                hideSystemUi();
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
        game.resume();
        // (Keys go to the game again after a dialog.)
        game.requestFocus();
    }

    /** ((13.3) The phone is short of memory: the game lets go of what it can work out again. */
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_RUNNING_LOW) game.trimMemory();
    }

    @Override
    protected void onPause() {
        game.pause();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (!game.onBack()) super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        game.release();
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
}
