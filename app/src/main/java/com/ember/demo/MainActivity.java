package com.ember.demo;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Slipknot v14 — Niagara-style home inside Slipknot.
 *
 * One app, one setup: a vertical alphabetical app list with an alphabet
 * scrubber on the right edge (Niagara-style), Slipknot black base with
 * RED / GOLD / BLUE / WHITE accents, live search, plus the v13 system
 * gesture service (Back / Home / Recents from screen edges) enabled
 * from the GOLD button below.
 *
 * No grid, no favourites rings in this build: the list IS the home.
 * Inline notification previews (Niagara Pro-style) are intentionally
 * NOT included — they need a NotificationListenerService, one more
 * permission gate, and this build is meant to stay simple.
 */
public class MainActivity extends Activity {

    static final String BUILD_MARKER = "SlipknotNiagara-v14";

    static final String PREFS = "slipknot_prefs";
    static final String KEY_ACCENT = "accent_index";

    static final int BLACK = 0xFF000000;
    static final int DARK = 0xFF111111;
    static final int GREY = 0xFF888888;
    static final int RED = 0xFFFF5252;
    static final int GOLD = 0xFFFFCC00;
    static final int BLUE = 0xFF448AFF;
    static final int WHITE = 0xFFFFFFFF;
    static final int[] PALETTE = {RED, GOLD, BLUE, WHITE};

    static final String LETTERS = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    int accentIndex = 1; // GOLD default, same prefs keys as v12/v13
    TextView titleView;
    TextView dateView;
    EditText searchBox;
    ListView listView;
    LinearLayout scrubber;
    TextView gestureBtn;
    TextView hintView;

    final ArrayList<AppEntry> fullApps = new ArrayList<AppEntry>();
    final ArrayList<AppEntry> shownApps = new ArrayList<AppEntry>();
    AppAdapter adapter;
    String query = "";

    static final class AppEntry {
        String label;
        String pkg;
        Drawable icon;
        char initial;
    }

    int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        accentIndex = prefs.getInt(KEY_ACCENT, 1) % PALETTE.length;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(BLACK);

        // ---- left column: header + search + list + gesture button ----
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colParams =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        col.setLayoutParams(colParams);

        // Header: title + date, swipe left/right cycles accent.
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(16), dp(20), dp(8), dp(8));
        titleView = new TextView(this);
        titleView.setText("Slipknot");
        titleView.setTextSize(30);
        titleView.setTextColor(PALETTE[accentIndex]);
        titleView.setGravity(Gravity.START);
        dateView = new TextView(this);
        dateView.setTextSize(14);
        dateView.setTextColor(GREY);
        header.addView(titleView);
        header.addView(dateView);
        final float[] downX = new float[1];
        header.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    downX[0] = e.getX();
                } else if (e.getAction() == MotionEvent.ACTION_UP) {
                    float dx = e.getX() - downX[0];
                    if (dx > 100) {
                        cycleAccent(1);
                    } else if (dx < -100) {
                        cycleAccent(-1);
                    }
                }
                return true;
            }
        });
        col.addView(header);

        // Search box.
        searchBox = new EditText(this);
        searchBox.setHint("ရှာရန်…");
        searchBox.setHintTextColor(GREY);
        searchBox.setTextColor(WHITE);
        searchBox.setTextSize(16);
        searchBox.setBackgroundColor(DARK);
        searchBox.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchParams.setMargins(dp(12), dp(4), dp(4), dp(8));
        searchBox.setLayoutParams(searchParams);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                query = s.toString().trim().toLowerCase(Locale.US);
                applyFilter();
            }
        });
        col.addView(searchBox);

        // App list.
        listView = new ListView(this);
        listView.setBackgroundColor(BLACK);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        adapter = new AppAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> parent, View view,
                                    int position, long id) {
                AppEntry app = shownApps.get(position);
                launchApp(app);
            }
        });
        listView.setOnItemLongClickListener(new android.widget.AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(android.widget.AdapterView<?> parent, View view,
                                           int position, long id) {
                AppEntry app = shownApps.get(position);
                openAppInfo(app);
                return true;
            }
        });
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        listView.setLayoutParams(listParams);
        col.addView(listView);

        // GOLD gesture button + hint.
        gestureBtn = new TextView(this);
        gestureBtn.setTextSize(16);
        gestureBtn.setGravity(Gravity.CENTER);
        gestureBtn.setPadding(dp(8), dp(12), dp(8), dp(12));
        gestureBtn.setBackgroundColor(GOLD);
        gestureBtn.setTextColor(BLACK);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(dp(12), dp(8), dp(4), dp(2));
        gestureBtn.setLayoutParams(btnParams);
        gestureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAccessibilitySettings();
            }
        });
        col.addView(gestureBtn);

        hintView = new TextView(this);
        hintView.setTextSize(12);
        hintView.setTextColor(GREY);
        hintView.setGravity(Gravity.CENTER);
        hintView.setPadding(dp(8), dp(2), dp(8), dp(12));
        hintView.setText("အွန်း/ယာ အွန်း: Back • အောက်ဆွဲ: Home • ကြာကြာကိုင်: Recents");
        col.addView(hintView);

        root.addView(col);

        // ---- right edge: alphabet scrubber ----
        scrubber = new LinearLayout(this);
        scrubber.setOrientation(LinearLayout.VERTICAL);
        scrubber.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams scrubParams = new LinearLayout.LayoutParams(
                dp(28), ViewGroup.LayoutParams.MATCH_PARENT);
        scrubber.setLayoutParams(scrubParams);
        for (int i = 0; i < LETTERS.length(); i++) {
            TextView letter = new TextView(this);
            letter.setText(String.valueOf(LETTERS.charAt(i)));
            letter.setTextSize(11);
            letter.setTextColor(GREY);
            letter.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
            letter.setLayoutParams(lp);
            scrubber.addView(letter);
        }
        scrubber.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN
                        || e.getAction() == MotionEvent.ACTION_MOVE) {
                    int h = v.getHeight();
                    if (h > 0) {
                        int idx = (int) (e.getY() / h * LETTERS.length());
                        if (idx < 0) idx = 0;
                        if (idx >= LETTERS.length()) idx = LETTERS.length() - 1;
                        jumpToLetter(LETTERS.charAt(idx));
                    }
                    return true;
                }
                return false;
            }
        });
        root.addView(scrubber);

        setContentView(root);
        loadApps();
        refreshDate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadApps();
        refreshDate();
        refreshGestureBtn();
    }

    void cycleAccent(int dir) {
        accentIndex = (accentIndex + dir + PALETTE.length) % PALETTE.length;
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt(KEY_ACCENT, accentIndex).apply();
        titleView.setTextColor(PALETTE[accentIndex]);
        adapter.notifyDataSetChanged();
    }

    void refreshDate() {
        try {
            String d = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
                    .format(new Date());
            dateView.setText(d);
        } catch (Exception e) {
            dateView.setText("");
        }
    }

    void loadApps() {
        try {
            PackageManager pm = getPackageManager();
            Intent main = new Intent(Intent.ACTION_MAIN);
            main.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> infos = pm.queryIntentActivities(main, 0);
            fullApps.clear();
            String self = getPackageName();
            for (ResolveInfo info : infos) {
                String pkg = info.activityInfo.packageName;
                if (pkg.equals(self)) continue;
                AppEntry app = new AppEntry();
                app.pkg = pkg;
                CharSequence label = info.loadLabel(pm);
                app.label = label == null ? pkg : label.toString();
                try {
                    app.icon = info.loadIcon(pm);
                } catch (Exception e) {
                    app.icon = null;
                }
                char c = app.label.length() > 0
                        ? Character.toUpperCase(app.label.charAt(0)) : '#';
                app.initial = (c >= 'A' && c <= 'Z') ? c : '#';
                fullApps.add(app);
            }
            Collections.sort(fullApps, new Comparator<AppEntry>() {
                @Override
                public int compare(AppEntry a, AppEntry b) {
                    return a.label.compareToIgnoreCase(b.label);
                }
            });
        } catch (Exception e) {
            // Keep whatever list we already had; never crash the home screen.
        }
        applyFilter();
    }

    void applyFilter() {
        shownApps.clear();
        for (AppEntry app : fullApps) {
            if (query.length() == 0
                    || app.label.toLowerCase(Locale.US).contains(query)) {
                shownApps.add(app);
            }
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    void jumpToLetter(char letter) {
        for (int i = 0; i < shownApps.size(); i++) {
            if (shownApps.get(i).initial == letter) {
                listView.setSelection(i);
                return;
            }
        }
        // No app with that letter: jump to the next letter that has one.
        for (int i = 0; i < shownApps.size(); i++) {
            if (shownApps.get(i).initial > letter) {
                listView.setSelection(i);
                return;
            }
        }
    }

    void launchApp(AppEntry app) {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(app.pkg);
            if (intent != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "ဖွင့်၍မရပါ: " + app.label,
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "ဖွင့်၍မရပါ: " + app.label,
                    Toast.LENGTH_SHORT).show();
        }
    }

    void openAppInfo(AppEntry app) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + app.pkg));
            startActivity(intent);
        } catch (Exception e) {
            // Nothing: long-press is a bonus, never a crash.
        }
    }

    boolean gestureServiceEnabled() {
        try {
            String enabled = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (enabled == null || enabled.length() == 0) return false;
            String flat = new ComponentName(this, GestureService.class)
                    .flattenToString();
            String full = getPackageName() + "/" + GestureService.class.getName();
            return enabled.contains(flat) || enabled.contains(full)
                    || enabled.contains(getPackageName() + "/.GestureService");
        } catch (Exception e) {
            return false;
        }
    }

    void refreshGestureBtn() {
        if (gestureBtn == null) return;
        gestureBtn.setText(gestureServiceEnabled()
                ? "🖐 System Gesture: ဖွင့်ထားပြီ ✓"
                : "🖐 System Gesture ဖွင့်ရန် နှိပ်ပါ");
    }

    void openAccessibilitySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            Toast.makeText(this,
                    "Accessibility → Installed services → Slipknot Gesture → On လုပ်ပေးပါ",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this,
                    "Accessibility settings ကို ဖွင့်၍မရပါ",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ---- list adapter: icon + label, label colour cycles the palette ----
    final class AppAdapter extends BaseAdapter {
        @Override public int getCount() {
            return shownApps.size();
        }

        @Override public Object getItem(int position) {
            return shownApps.get(position);
        }

        @Override public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout row;
            ImageView iconView;
            TextView labelView;
            if (convertView == null) {
                row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(12), dp(10), dp(8), dp(10));
                iconView = new ImageView(MainActivity.this);
                LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(
                        dp(40), dp(40));
                ip.setMargins(0, 0, dp(12), 0);
                iconView.setLayoutParams(ip);
                labelView = new TextView(MainActivity.this);
                labelView.setTextSize(18);
                row.addView(iconView);
                row.addView(labelView);
                row.setTag(new View[]{iconView, labelView});
            } else {
                row = (LinearLayout) convertView;
                View[] holder = (View[]) row.getTag();
                iconView = (ImageView) holder[0];
                labelView = (TextView) holder[1];
            }
            AppEntry app = shownApps.get(position);
            if (app.icon != null) {
                iconView.setImageDrawable(app.icon);
            } else {
                iconView.setImageDrawable(null);
            }
            labelView.setText(app.label);
            labelView.setTextColor(PALETTE[position % PALETTE.length]);
            return row;
        }
    }
}
