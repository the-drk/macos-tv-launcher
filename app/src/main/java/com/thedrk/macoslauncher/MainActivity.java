package com.thedrk.macoslauncher;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.KeyEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private LinearLayout dock;
    private HorizontalScrollView scroll;
    private TextView clock;
    private final Handler handler = new Handler();

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable rounded(int color, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(22));
        if (stroke != 0) {
            d.setStroke(dp(1), stroke);
        }
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

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(12, 14, 22));

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(dp(48), dp(25), dp(48), dp(25));

        FrameLayout.LayoutParams mainParams =
            new FrameLayout.LayoutParams(-1, -1);

        root.addView(main, mainParams);

        // TOP BAR

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setOrientation(LinearLayout.HORIZONTAL);

        TextView brand = new TextView(this);
        brand.setText("macOS");
        brand.setTextColor(Color.WHITE);
        brand.setTextSize(23);
        brand.setTypeface(Typeface.create("sans-serif-medium", 0));

        TextView subtitle = new TextView(this);
        subtitle.setText("   TV EXPERIENCE");
        subtitle.setTextColor(Color.rgb(135, 145, 165));
        subtitle.setTextSize(10);

        clock = new TextView(this);
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(16);
        clock.setGravity(Gravity.RIGHT);

        top.addView(brand);
        top.addView(subtitle);

        View spacer = new View(this);
        top.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));

        top.addView(clock);
        main.addView(top);

        updateClock();

        // MAIN TITLE

        TextView title = new TextView(this);
        title.setText("Good to see you.");
        title.setTextColor(Color.WHITE);
        title.setTextSize(29);
        title.setTypeface(Typeface.create("sans-serif-medium", 0));
        title.setPadding(0, dp(65), 0, dp(7));
        main.addView(title);

        TextView description = new TextView(this);
        description.setText("Choose something to watch");
        description.setTextColor(Color.rgb(150, 155, 172));
        description.setTextSize(14);
        main.addView(description);

        // APPLICATION DOCK

        scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(dp(5), dp(18), dp(5), dp(18));

        dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER_VERTICAL);

        scroll.addView(dock);

        LinearLayout.LayoutParams dockParams =
            new LinearLayout.LayoutParams(-1, dp(180));

        dockParams.topMargin = dp(35);
        main.addView(scroll, dockParams);

        TextView footer = new TextView(this);
        footer.setText("SELECT  OPEN APP       •       BACK  HOME");
        footer.setTextColor(Color.rgb(105, 112, 130));
        footer.setTextSize(11);
        footer.setPadding(0, dp(22), 0, 0);
        main.addView(footer);

        setContentView(root);

        loadApps();
    }

    private void updateClock() {
        if (clock != null) {
            String time = new SimpleDateFormat(
                "hh:mm a    EEE, dd MMM",
                Locale.getDefault()
            ).format(new Date());

            clock.setText(time);
        }

        handler.postDelayed(this::updateClock, 60000);
    }

    private void loadApps() {
        dock.removeAllViews();

        PackageManager pm = getPackageManager();

        Intent leanback = new Intent(Intent.ACTION_MAIN);
        leanback.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);

        Intent normal = new Intent(Intent.ACTION_MAIN);
        normal.addCategory(Intent.CATEGORY_LAUNCHER);

        ArrayList<ResolveInfo> apps = new ArrayList<>();
        ArrayList<String> packages = new ArrayList<>();

        for (ResolveInfo info : pm.queryIntentActivities(leanback, 0)) {
            String pkg = info.activityInfo.packageName;
            if (!packages.contains(pkg)) {
                packages.add(pkg);
                apps.add(info);
            }
        }

        for (ResolveInfo info : pm.queryIntentActivities(normal, 0)) {
            String pkg = info.activityInfo.packageName;
            if (!packages.contains(pkg) &&
                !pkg.equals(getPackageName())) {
                packages.add(pkg);
                apps.add(info);
            }
        }

        apps.sort((a, b) ->
            a.loadLabel(pm).toString().compareToIgnoreCase(
                b.loadLabel(pm).toString()
            )
        );

        for (ResolveInfo info : apps) {
            addAppTile(info, pm);
        }

        if (dock.getChildCount() > 0) {
            dock.getChildAt(0).requestFocus();
        }
    }

    private void addAppTile(ResolveInfo info, PackageManager pm) {

        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setFocusable(true);
        tile.setFocusableInTouchMode(true);
        tile.setBackground(rounded(
            Color.rgb(29, 33, 46),
            Color.rgb(48, 53, 70)
        ));

        ImageView icon = new ImageView(this);

        try {
            icon.setImageDrawable(info.loadIcon(pm));
        } catch (Exception ignored) {}

        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);

        LinearLayout.LayoutParams iconParams =
            new LinearLayout.LayoutParams(dp(54), dp(54));

        tile.addView(icon, iconParams);

        TextView label = new TextView(this);
        label.setText(info.loadLabel(pm).toString());
        label.setTextColor(Color.WHITE);
        label.setTextSize(12);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(1);
        label.setSingleLine(true);

        LinearLayout.LayoutParams labelParams =
            new LinearLayout.LayoutParams(
                dp(110), -2
            );

        labelParams.topMargin = dp(13);
        tile.addView(label, labelParams);

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(dp(145), dp(145));

        params.setMargins(dp(8), 0, dp(8), 0);
        dock.addView(tile, params);

        tile.setOnFocusChangeListener((v, focused) -> {

            float scale = focused ? 1.09f : 1.0f;

            v.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(160)
                .setInterpolator(new DecelerateInterpolator())
                .start();

            v.setBackground(rounded(
                focused
                    ? Color.rgb(53, 61, 83)
                    : Color.rgb(29, 33, 46),
                focused
                    ? Color.rgb(130, 150, 190)
                    : Color.rgb(48, 53, 70)
            ));

            if (focused) {
                scroll.post(() -> {
                    int target = Math.max(
                        0,
                        v.getLeft() - dp(30)
                    );
                    scroll.smoothScrollTo(target, 0);
                });
            }
        });

        tile.setOnClickListener(v -> {
            try {
                Intent launch = pm.getLaunchIntentForPackage(
                    info.activityInfo.packageName
                );

                if (launch == null) {
                    launch = new Intent(Intent.ACTION_MAIN);
                    launch.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
                    launch.setPackage(info.activityInfo.packageName);
                }

                if (launch.resolveActivity(pm) != null) {
                    startActivity(launch);
                }
            } catch (Exception e) {
                android.widget.Toast.makeText(
                    this,
                    "Unable to open application",
                    android.widget.Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (dock != null) {
            loadApps();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
