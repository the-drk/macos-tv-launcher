package com.thedrk.macoslauncher;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.animation.ValueAnimator;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvInputManager;
import android.net.wifi.WifiManager;
import android.bluetooth.BluetoothAdapter;
import android.provider.Settings;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;
import android.text.TextUtils;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {

    private FrameLayout root;
    private AppRow favourites, allApps;
    private LinearLayout panel;
    private TextView clock, wifiChip, btChip;
    private Wallpaper wallpaper;
    private View previousFocus;
    private boolean panelOpen = false;

    private final Handler handler = new Handler();
    private final Set<String> pinned = new HashSet<>();
    private final ArrayList<ResolveInfo> applications = new ArrayList<>();
    private final String PREF = "launcher_settings";

    private int dp(float n) {
        return (int)(n * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable shape(int color, int border, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (border != 0) d.setStroke(dp(1), border);
        return d;
    }

    private TextView label(String s, int size, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        pinned.addAll(getSharedPreferences(PREF, MODE_PRIVATE)
            .getStringSet("favourites", new HashSet<>()));

        root = new FrameLayout(this);
        wallpaper = new Wallpaper(this);
        root.addView(wallpaper, new FrameLayout.LayoutParams(-1, -1));

        buildDesktop();
        setContentView(root);

        loadApps();
        updateStatus();
        updateClock();
    }

    // ---------------- DESKTOP ----------------

    private void buildDesktop() {

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(48), dp(20), dp(48), dp(15));

        root.addView(page, new FrameLayout.LayoutParams(-1, -1));

        // TOP MENU BAR

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView apple = label("●", 19, Color.WHITE);
        TextView brand = label("  macOS", 17, Color.WHITE);
        brand.setTypeface(Typeface.create("sans-serif-medium", 0));

        top.addView(apple);
        top.addView(brand);

        TextView desk = label("    DESKTOP", 10, 0xFF9BAAC6);
        top.addView(desk);

        View gap = new View(this);
        top.addView(gap, new LinearLayout.LayoutParams(0, 1, 1));

        wifiChip = chip("Wi-Fi", Settings.ACTION_WIFI_SETTINGS);
        btChip = chip("Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS);

        top.addView(wifiChip);
        top.addView(btChip);

        TextView settings = chip("⚙", Settings.ACTION_SETTINGS);
        top.addView(settings);

        TextView control = chip("☷", null);
        control.setOnClickListener(v -> openPanel());
        top.addView(control);

        page.addView(top, new LinearLayout.LayoutParams(-1, dp(44)));

        // MAIN TITLE

        TextView heading = label("Your space.", 34, Color.WHITE);
        heading.setTypeface(Typeface.create("sans-serif-light", 0));
        LinearLayout.LayoutParams hp =
            new LinearLayout.LayoutParams(-1, dp(52));
        hp.topMargin = dp(35);
        page.addView(heading, hp);

        TextView sub = label(
            "Everything you need, in one place.",
            14, 0xFFB8C4D9);
        page.addView(sub);

        // FAVOURITES

        TextView favTitle = label("FAVOURITES", 10, 0xFFA8B7D3);
        favTitle.setLetterSpacing(0.15f);
        LinearLayout.LayoutParams fh =
            new LinearLayout.LayoutParams(-1, dp(25));
        fh.topMargin = dp(27);
        page.addView(favTitle, fh);

        favourites = new AppRow();
        page.addView(favourites.frame,
            new LinearLayout.LayoutParams(-1, dp(116)));

        // ALL APPLICATIONS

        LinearLayout appHeading = new LinearLayout(this);
        appHeading.setGravity(Gravity.CENTER_VERTICAL);

        TextView appTitle = label("APPLICATIONS", 10, 0xFFA8B7D3);
        appTitle.setLetterSpacing(0.15f);
        appHeading.addView(appTitle);

        TextView hint = label(
            "     HOLD OK TO PIN / UNPIN",
            9, 0xFF75839F);
        appHeading.addView(hint);

        LinearLayout.LayoutParams ah =
            new LinearLayout.LayoutParams(-1, dp(25));
        ah.topMargin = dp(12);
        page.addView(appHeading, ah);

        allApps = new AppRow();
        page.addView(allApps.frame,
            new LinearLayout.LayoutParams(-1, dp(116)));

        View space = new View(this);
        page.addView(space,
            new LinearLayout.LayoutParams(1, 0, 1));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER_VERTICAL);

        TextView instructions = label(
            "← →  NAVIGATE     OK  OPEN     MENU  CONTROL CENTRE",
            10, 0xFF8593AD);
        bottom.addView(instructions);

        View spacer = new View(this);
        bottom.addView(spacer,
            new LinearLayout.LayoutParams(0, 1, 1));

        clock = label("", 13, Color.WHITE);
        bottom.addView(clock);

        page.addView(bottom,
            new LinearLayout.LayoutParams(-1, dp(28)));
    }

    private TextView chip(String text, String action) {

        TextView v = label(text, 12, Color.WHITE);
        v.setGravity(Gravity.CENTER);
        v.setFocusable(true);
        v.setPadding(dp(12), 0, dp(12), 0);
        v.setBackground(shape(0x55333F56, 0x667887A5, 18));

        v.setOnFocusChangeListener((view, focused) -> {
            v.setBackground(shape(
                focused ? 0xCC526A91 : 0x55333F56,
                focused ? 0xFFD0E0FF : 0x667887A5,
                18));
            v.animate().scaleX(focused ? 1.08f : 1f)
                .scaleY(focused ? 1.08f : 1f)
                .setDuration(140).start();
        });

        v.setOnClickListener(view -> {
            if (action != null) openAction(action, true);
            else openPanel();
        });

        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(-2, dp(34));
        p.setMargins(dp(5), 0, dp(5), 0);
        v.setLayoutParams(p);

        return v;
    }

    // ---------------- APP DOCK ----------------

    private class AppRow {

        FrameLayout frame;
        HorizontalScrollView scroll;
        LinearLayout items;
        View indicator;

        AppRow() {

            frame = new FrameLayout(MainActivity.this);

            indicator = new View(MainActivity.this);
            indicator.setBackground(shape(
                0x88566F9D,
                0xFFD4E3FF,
                20));
            indicator.setAlpha(0f);

            FrameLayout.LayoutParams ip =
                new FrameLayout.LayoutParams(dp(108), dp(108),
                    Gravity.TOP | Gravity.LEFT);

            frame.addView(indicator, ip);

            scroll = new HorizontalScrollView(MainActivity.this);
            scroll.setHorizontalScrollBarEnabled(false);
            scroll.setClipToPadding(false);

            items = new LinearLayout(MainActivity.this);
            items.setGravity(Gravity.CENTER_VERTICAL);
            items.setOrientation(LinearLayout.HORIZONTAL);

            scroll.addView(items);

            frame.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        }

        void clear() {
            items.removeAllViews();
            indicator.setAlpha(0f);
            scroll.scrollTo(0, 0);
        }

        void add(ResolveInfo info, boolean favourite) {

            PackageManager pm = getPackageManager();
            String pkg = info.activityInfo.packageName;

            LinearLayout tile = new LinearLayout(MainActivity.this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setFocusable(true);
            tile.setFocusableInTouchMode(true);
            tile.setBackgroundColor(Color.TRANSPARENT);

            ImageView icon = new ImageView(MainActivity.this);

            try {
                icon.setImageDrawable(info.loadIcon(pm));
            } catch (Exception ignored) {}

            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);

            tile.addView(icon,
                new LinearLayout.LayoutParams(dp(62), dp(62)));

            TextView name = label(
                info.loadLabel(pm).toString(),
                11, 0xFFE0E6F2);
            name.setGravity(Gravity.CENTER);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);

            LinearLayout.LayoutParams np =
                new LinearLayout.LayoutParams(dp(104), dp(24));
            np.topMargin = dp(7);
            tile.addView(name, np);

            LinearLayout.LayoutParams tp =
                new LinearLayout.LayoutParams(dp(112), dp(108));
            items.addView(tile, tp);

            tile.setOnFocusChangeListener((v, focused) -> {

                icon.animate()
                    .scaleX(focused ? 1.10f : 1f)
                    .scaleY(focused ? 1.10f : 1f)
                    .setDuration(170)
                    .start();

                name.setTextColor(focused
                    ? Color.WHITE : 0xFFB7C3D8);

                if (focused) moveIndicator(tile);
            });

            tile.setOnClickListener(v -> launchApp(info));

            tile.setOnLongClickListener(v -> {
                if (pinned.contains(pkg)) pinned.remove(pkg);
                else pinned.add(pkg);

                getSharedPreferences(PREF, MODE_PRIVATE)
                    .edit()
                    .putStringSet("favourites", new HashSet<>(pinned))
                    .apply();

                Toast.makeText(MainActivity.this,
                    pinned.contains(pkg) ? "Added to favourites"
                                         : "Removed from favourites",
                    Toast.LENGTH_SHORT).show();

                loadApps();
                return true;
            });
        }

        void moveIndicator(View tile) {
            wallpaper.setPosition(items.indexOfChild(tile));

            scroll.post(() -> {

                int left = tile.getLeft();
                int visible = left - scroll.getScrollX();

                if (visible < dp(5) ||
                    visible + dp(112) > scroll.getWidth()) {

                    scroll.smoothScrollTo(
                        Math.max(0, left - scroll.getWidth()/2
                            + dp(56)), 0);
                }

                scroll.post(() -> {
                    float x = tile.getLeft() - scroll.getScrollX();
                    indicator.animate().cancel();
                    indicator.animate()
                        .translationX(x)
                        .alpha(1f)
                        .setDuration(320)
                        .setInterpolator(new DecelerateInterpolator(1.5f))
                        .start();
                });
            });
        }
    }

    private void loadApps() {

        if (allApps == null || favourites == null) return;

        allApps.clear();
        favourites.clear();
        applications.clear();

        PackageManager pm = getPackageManager();

        Intent leanback = new Intent(Intent.ACTION_MAIN);
        leanback.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);

        Intent normal = new Intent(Intent.ACTION_MAIN);
        normal.addCategory(Intent.CATEGORY_LAUNCHER);

        HashSet<String> seen = new HashSet<>();

        for (ResolveInfo r : pm.queryIntentActivities(leanback, 0)) {
            if (seen.add(r.activityInfo.packageName))
                applications.add(r);
        }

        for (ResolveInfo r : pm.queryIntentActivities(normal, 0)) {
            String pkg = r.activityInfo.packageName;

            if (!pkg.equals(getPackageName()) && seen.add(pkg))
                applications.add(r);
        }

        applications.sort((a,b) ->
            a.loadLabel(pm).toString().compareToIgnoreCase(
                b.loadLabel(pm).toString()));

        boolean hasFavourites = false;
        for (ResolveInfo r : applications) {

            boolean fav = pinned.contains(
                r.activityInfo.packageName);

            allApps.add(r, false);

            if (fav) favourites.add(r, true);
            if (fav) hasFavourites = true;
        }

        if (favourites.items.getChildCount() == 0) {
            TextView empty = label(
                "Hold OK on an application below to add it here.",
                12, 0xFF9EACC3);
            empty.setPadding(dp(10), 0, 0, 0);
            favourites.items.addView(empty);
        }

        AppRow first = (favourites.items.getChildCount() > 0 && favourites.items.getChildAt(0).isFocusable()) ? favourites : allApps;

        if (first.items.getChildCount() > 0)
            first.items.getChildAt(0).requestFocus();
    }

    private void launchApp(ResolveInfo info) {

        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(
                info.activityInfo.packageName);

            if (i == null) {
                i = new Intent(Intent.ACTION_MAIN);
                i.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
                i.setClassName(info.activityInfo.packageName,
                    info.activityInfo.name);
            }

            startActivity(i);

        } catch (Exception e) {
            Toast.makeText(this, "Unable to launch app",
                Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------- CONTROL CENTRE ----------------

    private void openPanel() {

        if (panelOpen) return;
        panelOpen = true;

        previousFocus = getCurrentFocus();

        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(18), dp(20), dp(18));
        panel.setBackground(shape(
            0xF21B2436, 0xFF667C9F, 24));

        FrameLayout.LayoutParams pp =
            new FrameLayout.LayoutParams(dp(350), -1,
                Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        pp.setMargins(0, dp(12), dp(12), dp(12));

        root.addView(panel, pp);

        TextView heading = label("Control Centre", 23, Color.WHITE);
        panel.addView(heading,
            new LinearLayout.LayoutParams(-1, dp(42)));

        TextView desc = label("SYSTEM SHORTCUTS", 10, 0xFF9DAEC9);
        panel.addView(desc,
            new LinearLayout.LayoutParams(-1, dp(28)));

        ScrollView list = new ScrollView(this);
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);

        list.addView(rows);
        panel.addView(list,
            new LinearLayout.LayoutParams(-1, 0, 1));

        addControl(rows, "⌁", "Wi-Fi",
            "Network settings", Settings.ACTION_WIFI_SETTINGS);

        addControl(rows, "ᛒ", "Bluetooth",
            "Bluetooth settings", Settings.ACTION_BLUETOOTH_SETTINGS);

        addControl(rows, "⚙", "System Settings",
            "Android TV settings", Settings.ACTION_SETTINGS);

        addControl(rows, "▣", "Display",
            "Display settings", Settings.ACTION_DISPLAY_SETTINGS);

        addControl(rows, "♫", "Sound",
            "Sound settings", Settings.ACTION_SOUND_SETTINGS);

        addControl(rows, "▤", "HDMI / Inputs",
            "TV input setup (firmware dependent)",
            "android.media.tv.action.SETUP_INPUTS");

        addControl(rows, "▰", "USB / Files",
            "Browse files and USB storage", "FILES");

        TextView close = label("Close     ✕", 13, Color.WHITE);
        close.setGravity(Gravity.CENTER);
        close.setFocusable(true);
        close.setBackground(shape(0x553D4D68, 0x668A9EBD, 15));
        close.setOnFocusChangeListener((v, focused) ->
            v.setBackground(shape(
                focused ? 0xFF526B91 : 0x553D4D68,
                focused ? Color.WHITE : 0x668A9EBD, 15)));
        close.setOnClickListener(v -> closePanel());

        LinearLayout.LayoutParams closeParams =
            new LinearLayout.LayoutParams(-1, dp(45));
        closeParams.topMargin = dp(10);
        panel.addView(close, closeParams);

        panel.setTranslationX(dp(350));
        panel.animate().translationX(0)
            .setDuration(220)
            .setInterpolator(new DecelerateInterpolator())
            .start();

        rows.post(() -> {
            if (rows.getChildCount() > 0)
                rows.getChildAt(0).requestFocus();
        });
    }

    private void addControl(LinearLayout rows, String symbol,
                            String title, String detail, String action) {

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(5), dp(10), dp(5));
        row.setFocusable(true);
        row.setBackground(shape(0x44323F56, 0x55788DAE, 16));

        TextView icon = label(symbol, 23, 0xFFD0DEFA);
        icon.setGravity(Gravity.CENTER);
        row.addView(icon,
            new LinearLayout.LayoutParams(dp(40), dp(44)));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(dp(10), 0, 0, 0);

        texts.addView(label(title, 14, Color.WHITE));
        texts.addView(label(detail, 10, 0xFFAAB8CF));

        row.addView(texts,
            new LinearLayout.LayoutParams(0, -2, 1));

        row.setOnFocusChangeListener((v, focused) -> {

            v.setBackground(shape(
                focused ? 0xFF536B91 : 0x44323F56,
                focused ? 0xFFE0EAFF : 0x55788DAE,
                16));

            v.animate()
                .scaleX(focused ? 1.025f : 1f)
                .scaleY(focused ? 1.025f : 1f)
                .setDuration(130).start();
        });

        row.setOnClickListener(v -> {
            closePanel();

            if (action.equals("FILES")) {
                openFiles();
            } else if (action.equals(
                "android.media.tv.action.SETUP_INPUTS")) {
                openAction(action, false);
            } else {
                openAction(action, true);
            }
        });

        LinearLayout.LayoutParams rp =
            new LinearLayout.LayoutParams(-1, dp(55));
        rp.setMargins(0, dp(4), 0, dp(4));
        rows.addView(row, rp);
    }

    private void closePanel() {

        if (!panelOpen || panel == null) return;

        panelOpen = false;
        LinearLayout old = panel;
        panel = null;

        old.animate()
            .translationX(dp(350))
            .setDuration(150)
            .withEndAction(() -> {
                root.removeView(old);
                if (previousFocus != null)
                    previousFocus.requestFocus();
            }).start();
    }

    // ---------------- SYSTEM SHORTCUTS ----------------

    private void openAction(String action, boolean fallback) {
        try {
            Intent i = new Intent(action);
            if (i.resolveActivity(getPackageManager()) != null) {
                startActivity(i);
                return;
            }
        } catch (Exception ignored) {}
        Toast.makeText(this,
            "This function is not exposed by your TV firmware.",
            Toast.LENGTH_LONG).show();
    }
    private void openFiles() {

        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.setType("*/*");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "No file browser available",
                Toast.LENGTH_SHORT).show();
        }
    }

    private void updateStatus() {

        try {
            WifiManager wifi = (WifiManager)
                getApplicationContext().getSystemService(WIFI_SERVICE);

            boolean connected = wifi != null &&
                wifi.getConnectionInfo() != null &&
                wifi.getConnectionInfo().getNetworkId() != -1;

            wifiChip.setText(connected ? "Wi-Fi ●" : "Wi-Fi ○");

        } catch (Exception e) {
            wifiChip.setText("Wi-Fi");
        }

        try {
            BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
            btChip.setText(bt != null && bt.isEnabled()
                ? "Bluetooth ●" : "Bluetooth ○");
        } catch (Exception e) {
            btChip.setText("Bluetooth");
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

    // ---------------- REMOTE NAVIGATION ----------------

    @Override
    public boolean onKeyDown(int key, android.view.KeyEvent event) {

        if (key == android.view.KeyEvent.KEYCODE_MENU) {
            openPanel();
            return true;
        }

        if (key == android.view.KeyEvent.KEYCODE_BACK && panelOpen) {
            closePanel();
            return true;
        }

        return super.onKeyDown(key, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (allApps != null) {
            loadApps();
            updateStatus();
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    // ---------------- CINEMATIC PARALLAX WALLPAPER ----------------

    private class Wallpaper extends View {

        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float position = 0f;
        private ValueAnimator animation;

        Wallpaper(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        void setPosition(int index) {

            float target = (index % 10) / 9f;

            if (animation != null) animation.cancel();

            animation = ValueAnimator.ofFloat(position, target);
            animation.setDuration(750);
            animation.setInterpolator(new DecelerateInterpolator());

            animation.addUpdateListener(a -> {
                position = (float)a.getAnimatedValue();
                invalidate();
            });

            animation.start();
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);

            float w = getWidth();
            float h = getHeight();
            float shift = position * w * 0.20f;

            // Deep midnight base
            p.setShader(new LinearGradient(
                0, 0, w, h,
                new int[]{
                    0xFF080D1A,
                    0xFF111B32,
                    0xFF090D1B
                }, null, Shader.TileMode.CLAMP));

            c.drawRect(0, 0, w, h, p);
            p.setShader(null);

            // Moving blue-violet ambient light
            p.setShader(new RadialGradient(
                w * 0.24f - shift, h * 0.42f,
                w * 0.72f,
                new int[]{
                    0x775D72D6,
                    0x33465DB4,
                    0x00465DB4
                }, null, Shader.TileMode.CLAMP));

            c.drawRect(0, 0, w, h, p);
            p.setShader(null);

            // Secondary moving light
            p.setShader(new RadialGradient(
                w * 0.85f - shift * 0.55f, h * 0.78f,
                w * 0.62f,
                new int[]{
                    0x554A83C0,
                    0x223B609E,
                    0x003B609E
                }, null, Shader.TileMode.CLAMP));

            c.drawRect(0, 0, w, h, p);
            p.setShader(null);

            // Flowing translucent ribbons
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(h * 0.12f);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setShader(new LinearGradient(
                0, 0, w, h,
                new int[]{
                    0x004A64BC,
                    0x334E7BC5,
                    0x445A4CB0,
                    0x004E62C0
                }, null, Shader.TileMode.CLAMP));

            Path ribbon = new Path();
            ribbon.moveTo(-w * 0.2f - shift, h * 0.92f);
            ribbon.cubicTo(
                w * 0.2f - shift, h * 0.25f,
                w * 0.7f - shift, h * 0.95f,
                w * 1.2f - shift, h * 0.20f
            );

            c.drawPath(ribbon, p);

            p.setStrokeWidth(h * 0.055f);
            p.setShader(new LinearGradient(
                0, h * 0.2f, w, h,
                new int[]{
                    0x002E8DAB,
                    0x443E8DAA,
                    0x334D6DAA,
                    0x003E6DAA
                }, null, Shader.TileMode.CLAMP));

            Path ribbon2 = new Path();
            ribbon2.moveTo(-w * 0.1f + shift * 0.3f, h * 0.15f);
            ribbon2.cubicTo(
                w * 0.3f + shift * 0.3f, h * 0.85f,
                w * 0.8f + shift * 0.3f, h * 0.10f,
                w * 1.1f + shift * 0.3f, h * 0.65f
            );

            c.drawPath(ribbon2, p);

            p.setShader(null);
            p.setStyle(Paint.Style.FILL);
        }
    }
}
