package com.thedrk.macoslauncher;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.wifi.WifiManager;
import android.bluetooth.BluetoothAdapter;
import android.provider.Settings;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;
import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {

    private FrameLayout root;
    private LinearLayout dock;
    private HorizontalScrollView scroll;
    private LinearLayout controlPanel;
    private TextView clock, wifiStatus, bluetoothStatus;
    private WallpaperView wallpaper;
    private boolean panelOpen = false;
    private final Handler handler = new Handler();

    private int dp(float v) {
        return (int)(v * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable glass(int color, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        root = new FrameLayout(this);

        wallpaper = new WallpaperView(this);
        root.addView(wallpaper,
            new FrameLayout.LayoutParams(-1, -1));

        buildDesktop();
        setContentView(root);
        loadApps();
        updateStatus();
        updateClock();
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setFontFeatureSettings("kern");
        return t;
    }

    private void buildDesktop() {

        LinearLayout desktop = new LinearLayout(this);
        desktop.setOrientation(LinearLayout.VERTICAL);
        desktop.setPadding(dp(52), dp(23), dp(52), dp(20));

        root.addView(desktop, new FrameLayout.LayoutParams(-1, -1));

        // TOP MENU BAR

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView logo = text("●", 19, Color.WHITE);
        TextView brand = text("  macOS", 17, Color.WHITE);
        brand.setTypeface(Typeface.create("sans-serif-medium", 0));

        top.addView(logo);
        top.addView(brand);

        TextView desktopLabel = text("     DESKTOP", 10,
            Color.rgb(145, 157, 184));
        top.addView(desktopLabel);

        View gap = new View(this);
        top.addView(gap, new LinearLayout.LayoutParams(0, 1, 1));

        wifiStatus = text("Wi-Fi", 11, Color.WHITE);
        bluetoothStatus = text("  Bluetooth", 11, Color.WHITE);

        top.addView(wifiStatus);
        top.addView(bluetoothStatus);

        TextView control = text("  ◉  Control Centre  ", 12,
            Color.WHITE);
        control.setFocusable(true);
        control.setPadding(dp(10), dp(8), dp(10), dp(8));
        control.setBackground(glass(0x442C3548, 0x557F8BA5, 20));
        control.setOnClickListener(v -> togglePanel());
        addFocus(control);

        LinearLayout.LayoutParams cp =
            new LinearLayout.LayoutParams(-2, dp(38));
        cp.leftMargin = dp(16);
        top.addView(control, cp);

        desktop.addView(top);

        // MAIN DESKTOP CONTENT

        View space = new View(this);
        desktop.addView(space,
            new LinearLayout.LayoutParams(1, 0, 1));

        TextView greeting = text("Your space.", 34, Color.WHITE);
        greeting.setTypeface(
            Typeface.create("sans-serif-light", 0));
        desktop.addView(greeting);

        TextView subtitle = text(
            "Everything you need, in one place.",
            14, 0xFFB2BBD0);
        LinearLayout.LayoutParams subParams =
            new LinearLayout.LayoutParams(-2, -2);
        subParams.topMargin = dp(8);
        desktop.addView(subtitle, subParams);

        // APP DOCK

        scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(dp(2), dp(20), dp(2), dp(20));

        dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER_VERTICAL);
        scroll.addView(dock);

        LinearLayout.LayoutParams dockParams =
            new LinearLayout.LayoutParams(-1, dp(150));
        dockParams.topMargin = dp(20);
        desktop.addView(scroll, dockParams);

        // BOTTOM BAR

        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER_VERTICAL);

        TextView hint = text(
            "←  →  NAVIGATE       OK  OPEN       MENU  CONTROL CENTRE",
            10, 0xFF8D9AB2);
        bottom.addView(hint);

        View bottomGap = new View(this);
        bottom.addView(bottomGap,
            new LinearLayout.LayoutParams(0, 1, 1));

        clock = text("", 14, Color.WHITE);
        bottom.addView(clock);

        LinearLayout.LayoutParams bottomParams =
            new LinearLayout.LayoutParams(-1, dp(35));
        bottomParams.topMargin = dp(10);
        desktop.addView(bottom, bottomParams);

        View endSpace = new View(this);
        desktop.addView(endSpace,
            new LinearLayout.LayoutParams(1, 0, 0.6f));
    }

    private void addFocus(View v) {
        v.setOnFocusChangeListener((view, focused) -> {
            view.animate()
                .scaleX(focused ? 1.08f : 1f)
                .scaleY(focused ? 1.08f : 1f)
                .setDuration(160)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        });
    }

    private void loadApps() {

        dock.removeAllViews();

        PackageManager pm = getPackageManager();

        Intent leanback = new Intent(Intent.ACTION_MAIN);
        leanback.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);

        Intent normal = new Intent(Intent.ACTION_MAIN);
        normal.addCategory(Intent.CATEGORY_LAUNCHER);

        ArrayList<ResolveInfo> apps = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();

        for (ResolveInfo info :
            pm.queryIntentActivities(leanback, 0)) {
            if (seen.add(info.activityInfo.packageName)) {
                apps.add(info);
            }
        }

        for (ResolveInfo info :
            pm.queryIntentActivities(normal, 0)) {
            String pkg = info.activityInfo.packageName;
            if (!pkg.equals(getPackageName()) && seen.add(pkg)) {
                apps.add(info);
            }
        }

        apps.sort((a, b) ->
            a.loadLabel(pm).toString().compareToIgnoreCase(
                b.loadLabel(pm).toString()));

        for (ResolveInfo info : apps) {
            addApp(info, pm);
        }

        if (dock.getChildCount() > 0) {
            dock.getChildAt(0).requestFocus();
        }
    }

    private void addApp(ResolveInfo info, PackageManager pm) {

        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setFocusable(true);
        item.setFocusableInTouchMode(true);
        item.setBackgroundColor(Color.TRANSPARENT);

        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);

        try {
            icon.setImageDrawable(info.loadIcon(pm));
        } catch (Exception ignored) {}

        item.addView(icon,
            new LinearLayout.LayoutParams(dp(76), dp(76)));

        TextView name = text(
            info.loadLabel(pm).toString(), 12, Color.WHITE);
        name.setGravity(Gravity.CENTER);
        name.setSingleLine(true);
        name.setMaxEms(12);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);

        LinearLayout.LayoutParams nameParams =
            new LinearLayout.LayoutParams(dp(112), dp(25));
        nameParams.topMargin = dp(8);
        item.addView(name, nameParams);

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(dp(125), dp(125));
        params.setMargins(dp(3), 0, dp(3), 0);
        dock.addView(item, params);

        item.setOnFocusChangeListener((v, focused) -> {
            float scale = focused ? 1.16f : 1f;

            v.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(190)
                .setInterpolator(new DecelerateInterpolator())
                .start();

            name.setTextColor(focused ?
                Color.WHITE : 0xFFB0BBD0);

            if (focused) {
                int index = dock.indexOfChild(v);
                wallpaper.setPosition(index);
                scroll.post(() ->
                    scroll.smoothScrollTo(
                        Math.max(0, v.getLeft() - dp(90)), 0));
            }
        });

        item.setOnClickListener(v -> {
            try {
                Intent launch = pm.getLaunchIntentForPackage(
                    info.activityInfo.packageName);

                if (launch != null) {
                    startActivity(launch);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open app",
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    // CONTROL CENTRE

    private void togglePanel() {
        if (panelOpen) {
            closePanel();
        } else {
            openPanel();
        }
    }

    private void openPanel() {

        if (panelOpen) return;
        panelOpen = true;

        controlPanel = new LinearLayout(this);
        controlPanel.setOrientation(LinearLayout.VERTICAL);
        controlPanel.setPadding(dp(24), dp(22), dp(24), dp(22));
        controlPanel.setBackground(
            glass(0xF21B2232, 0x667E8DAA, 25));
        controlPanel.setFocusable(true);

        FrameLayout.LayoutParams panelParams =
            new FrameLayout.LayoutParams(dp(330), -1,
                Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        panelParams.setMargins(0, dp(12), dp(12), dp(12));

        root.addView(controlPanel, panelParams);

        TextView heading = text("Control Centre", 23, Color.WHITE);
        heading.setTypeface(
            Typeface.create("sans-serif-medium", 0));
        controlPanel.addView(heading);

        TextView info = text("SYSTEM SHORTCUTS", 10, 0xFF91A0BD);
        LinearLayout.LayoutParams infoParams =
            new LinearLayout.LayoutParams(-1, dp(35));
        infoParams.topMargin = dp(15);
        controlPanel.addView(info, infoParams);

        addControl("⌁", "Wi-Fi", "Network settings",
            Settings.ACTION_WIFI_SETTINGS);

        addControl("ᛒ", "Bluetooth", "Bluetooth settings",
            Settings.ACTION_BLUETOOTH_SETTINGS);

        addControl("⚙", "System Settings", "Android TV settings",
            Settings.ACTION_SETTINGS);

        addControl("▣", "Display", "Display settings",
            Settings.ACTION_DISPLAY_SETTINGS);

        addControl("♫", "Sound", "Sound settings",
            Settings.ACTION_SOUND_SETTINGS);

        addControl("▤", "HDMI / Inputs", "TV input selection",
            "android.settings.HDMI_SETTINGS");

        addFileControl();

        TextView close = text("Close  ✕", 13, Color.WHITE);
        close.setGravity(Gravity.CENTER);
        close.setFocusable(true);
        close.setPadding(0, dp(12), 0, dp(12));
        close.setBackground(glass(0x442F3D55,
            0x557F8BA5, 16));
        close.setOnClickListener(v -> closePanel());

        LinearLayout.LayoutParams closeParams =
            new LinearLayout.LayoutParams(-1, dp(48));
        closeParams.topMargin = dp(12);
        controlPanel.addView(close, closeParams);

        controlPanel.setAlpha(0f);
        controlPanel.setTranslationX(dp(35));
        controlPanel.animate()
            .alpha(1f)
            .translationX(0)
            .setDuration(180)
            .start();

        wifiStatus.setText("Wi-Fi  ▾");
        heading.requestFocus();
    }

    private void addControl(String symbol, String title,
                            String description, String action) {

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setFocusable(true);
        row.setPadding(dp(12), dp(5), dp(10), dp(5));
        row.setBackground(glass(0x332E394D,
            0x447A8AA6, 16));

        TextView icon = text(symbol, 21, 0xFFB9CFFF);
        icon.setGravity(Gravity.CENTER);
        row.addView(icon,
            new LinearLayout.LayoutParams(dp(38), dp(42)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(9), 0, 0, 0);

        labels.addView(text(title, 14, Color.WHITE));
        labels.addView(text(description, 10, 0xFF9BA8C0));

        row.addView(labels,
            new LinearLayout.LayoutParams(0, -2, 1));

        LinearLayout.LayoutParams rowParams =
            new LinearLayout.LayoutParams(-1, dp(57));
        rowParams.topMargin = dp(7);
        controlPanel.addView(row, rowParams);

        addFocus(row);

        row.setOnClickListener(v -> {
            closePanel();
            openSettings(action);
        });
    }

    private void addFileControl() {

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setFocusable(true);
        row.setPadding(dp(12), dp(5), dp(10), dp(5));
        row.setBackground(glass(0x332E394D,
            0x447A8AA6, 16));

        TextView icon = text("▰", 21, 0xFFB9CFFF);
        row.addView(icon,
            new LinearLayout.LayoutParams(dp(38), dp(42)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(9), 0, 0, 0);
        labels.addView(text("USB / Files", 14, Color.WHITE));
        labels.addView(text("Browse storage", 10, 0xFF9BA8C0));

        row.addView(labels,
            new LinearLayout.LayoutParams(0, -2, 1));

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(-1, dp(57));
        params.topMargin = dp(7);
        controlPanel.addView(row, params);

        addFocus(row);

        row.setOnClickListener(v -> {
            closePanel();
            try {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivity(intent);
            } catch (Exception e) {
                openSettings(Settings.ACTION_SETTINGS);
            }
        });
    }

    private void openSettings(String action) {
        try {
            Intent intent = new Intent(action);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception ignored) {}
        }
    }

    private void closePanel() {
        if (!panelOpen || controlPanel == null) return;

        panelOpen = false;
        controlPanel.animate()
            .alpha(0f)
            .translationX(dp(35))
            .setDuration(140)
            .withEndAction(() -> {
                root.removeView(controlPanel);
                controlPanel = null;
                if (dock.getChildCount() > 0)
                    dock.getChildAt(0).requestFocus();
            })
            .start();
    }

    // LIVE STATUS

    private void updateStatus() {

        try {
            WifiManager wifi = (WifiManager)
                getApplicationContext().getSystemService(
                    Context.WIFI_SERVICE);

            boolean connected = wifi != null &&
                wifi.getConnectionInfo() != null &&
                wifi.getConnectionInfo().getNetworkId() != -1;

            wifiStatus.setText(connected ? "Wi-Fi ●" : "Wi-Fi ○");

        } catch (Exception e) {
            wifiStatus.setText("Wi-Fi");
        }

        try {
            BluetoothAdapter adapter =
                BluetoothAdapter.getDefaultAdapter();

            bluetoothStatus.setText(
                adapter != null && adapter.isEnabled()
                ? "  Bluetooth ●"
                : "  Bluetooth ○");

        } catch (Exception e) {
            bluetoothStatus.setText("  Bluetooth");
        }

        handler.postDelayed(this::updateStatus, 10000);
    }

    private void updateClock() {
        if (clock != null) {
            clock.setText(new SimpleDateFormat(
                "hh:mm a   •   EEE, dd MMM",
                Locale.getDefault()).format(new Date()));
        }

        handler.postDelayed(this::updateClock, 60000);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_MENU) {
            togglePanel();
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_BACK && panelOpen) {
            closePanel();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dock != null) loadApps();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    // LIGHTWEIGHT PARALLAX WALLPAPER

    private class WallpaperView extends View {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float position = 0f;

        WallpaperView(Context context) {
            super(context);
        }

        void setPosition(int index) {
            position = Math.min(index, 12) / 12f;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            int w = getWidth();
            int h = getHeight();

            paint.setShader(new LinearGradient(
                0, 0, w, h,
                new int[]{
                    Color.rgb(8, 12, 25),
                    Color.rgb(17, 22, 42),
                    Color.rgb(9, 12, 24)
                },
                null,
                Shader.TileMode.CLAMP
            ));

            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(null);

            float shift = position * dp(90);

            paint.setShader(new RadialGradient(
                w * 0.35f - shift, h * 0.42f,
                w * 0.68f,
                new int[]{0x554D68B8, 0x223C5799, 0x003C5799},
                null, Shader.TileMode.CLAMP
            ));

            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(null);

            paint.setShader(new RadialGradient(
                w * 0.82f - shift * 0.45f, h * 0.8f,
                w * 0.55f,
                new int[]{0x333F70A5, 0x113F70A5, 0x003F70A5},
                null, Shader.TileMode.CLAMP
            ));

            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(null);
        }
    }
}
