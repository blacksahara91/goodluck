package com.ember.demo;

import android.app.Activity;
import android.app.Dialog;
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
 * Slipknot v15 — Niagara-style home, user-tweakable.
 *
 * 1. The GOLD System Gesture button hides itself once the gesture
 *    service is ON (it reappears if the service ever turns off, so
 *    the enable path is never lost).
 * 2. Alphabet scrubbers on BOTH the left and right edges.
 * 3. Theme picker (tap the header): label colours, background and
 *    font size, saved in slipknot_prefs. Icon packs are NOT included
 *    in this build — that needs a separate icon-pack lookup.
 */
public class MainActivity extends Activity {

    static final String BUILD_MARKER = "SlipknotNiagara-v16";

    static final String PREFS = "slipknot_prefs";
    static final String KEY_ACCENT = "accent_index";
    static final String KEY_LABEL_MODE = "label_mode";
    static final String KEY_BG = "bg_mode";
    static final String KEY_FONT = "font_size";

    // Label modes: 0 multicolour, 1 white, 2 gold, 3 red, 4 blue.
    // Bg modes: 0 black, 1 dark grey, 2 light.
    static final int BLACK = 0xFF000000;
    static final int DARK = 0xFF111111;
    static final int DARKGREY = 0xFF222222;
    static final int LIGHTBG = 0xFFF5F5F5;
    static final int LIGHTBOX = 0xFFE0E0E0;
    static final int GREY = 0xFF888888;
    static final int DARKTEXT = 0xFF666666;
    static final int RED = 0xFFFF5252;
    static final int GOLD = 0xFFFFCC00;
    static final int BLUE = 0xFF448AFF;
    static final int WHITE = 0xFFFFFFFF;
    static final int[] PALETTE = {RED, GOLD, BLUE, WHITE};

    static final String LETTERS = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    int accentIndex = 1; // GOLD default, same prefs keys as v12/v13/v14
    int labelMode = 0;
    int bgMode = 0;
    int fontSize = 18;

    LinearLayout rootLayout;
    LinearLayout colLayout;
    TextView titleView;
    TextView dateView;
    EditText searchBox;
    ListView listView;
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

    int bgColor() {
        if (bgMode == 1) return DARKGREY;
        if (bgMode == 2) return LIGHTBG;
        return BLACK;
    }

    int boxColor() {
        if (bgMode == 2) return LIGHTBOX;
        return DARK;
    }

    int mainTextColor() {
        return bgMode == 2 ? BLACK : WHITE;
    }

    int faintColor() {
        return bgMode == 2 ? DARKTEXT : GREY;
    }

    int labelColorFor(int position) {
        int c;
        if (labelMode == 0) {
            c = PALETTE[position % PALETTE.length];
        } else if (labelMode == 1) {
            c = WHITE;
        } else if (labelMode == 2) {
            c = GOLD;
        } else if (labelMode == 3) {
            c = RED;
        } else {
            c = BLUE;
        }
        // White labels are invisible on the light background.
        if (bgMode == 2 && c == WHITE) return BLACK;
        return c;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        accentIndex = prefs.getInt(KEY_ACCENT, 1) % PALETTE.length;
        labelMode = prefs.getInt(KEY_LABEL_MODE, 0);
        bgMode = prefs.getInt(KEY_BG, 0);
        fontSize = prefs.getInt(KEY_FONT, 18);

        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.HORIZONTAL);

        // ---- left alphabet scrubber ----
        rootLayout.addView(buildScrubber());

        // ---- middle column: header + search + list + gesture button ----
        colLayout = new LinearLayout(this);
        colLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colParams =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        colLayout.setLayoutParams(colParams);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(16), dp(20), dp(8), dp(8));
        // Full-width so the whole top strip is tappable (theme dialog),
        // not just the title text itself.
        header.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        titleView = new TextView(this);
        titleView.setText("Slipknot");
        titleView.setTextSize(30);
        titleView.setTextColor(PALETTE[accentIndex]);
        titleView.setGravity(Gravity.START);
        dateView = new TextView(this);
        dateView.setTextSize(14);
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
                    } else {
                        // Plain tap on the header: theme picker.
                        showThemeDialog();
                    }
                }
                return true;
            }
        });
        colLayout.addView(header);

        searchBox = new EditText(this);
        searchBox.setHint("ရှာရန်…");
        searchBox.setTextSize(16);
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
        colLayout.addView(searchBox);

        listView = new ListView(this);
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
        colLayout.addView(listView);

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
        colLayout.addView(gestureBtn);

        hintView = new TextView(this);
        hintView.setTextSize(12);
        hintView.setGravity(Gravity.CENTER);
        hintView.setPadding(dp(8), dp(2), dp(8), dp(12));
        hintView.setText("Header နှိပ်ရင် Theme ပြောင်းလို့ရ • အွန်း/ယာ အွန်း: Back • အောက်ဆွဲ: Home • ကြာကြာကိုင်: Recents");
        colLayout.addView(hintView);

        rootLayout.addView(colLayout);

        // ---- right alphabet scrubber ----
        rootLayout.addView(buildScrubber());

        setContentView(rootLayout);
        applyTheme();
        loadAppsAsync();
        refreshDate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAppsAsync();
        refreshDate();
        applyTheme();
        refreshGestureBtn();
    }

    LinearLayout buildScrubber() {
        LinearLayout scrub = new LinearLayout(this);
        scrub.setOrientation(LinearLayout.VERTICAL);
        scrub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams scrubParams = new LinearLayout.LayoutParams(
                dp(28), ViewGroup.LayoutParams.MATCH_PARENT);
        scrub.setLayoutParams(scrubParams);
        for (int i = 0; i < LETTERS.length(); i++) {
            TextView letter = new TextView(this);
            letter.setText(String.valueOf(LETTERS.charAt(i)));
            letter.setTextSize(11);
            letter.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
            letter.setLayoutParams(lp);
            scrub.addView(letter);
        }
        scrub.setOnTouchListener(new View.OnTouchListener() {
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
        return scrub;
    }

    void applyTheme() {
        int bg = bgColor();
        int box = boxColor();
        int main = mainTextColor();
        int faint = faintColor();
        rootLayout.setBackgroundColor(bg);
        colLayout.setBackgroundColor(bg);
        listView.setBackgroundColor(bg);
        searchBox.setBackgroundColor(box);
        searchBox.setTextColor(main);
        searchBox.setHintTextColor(faint);
        dateView.setTextColor(faint);
        hintView.setTextColor(faint);
        recolorScrubbers(rootLayout, faint);
        if (adapter != null) adapter.notifyDataSetChanged();
        // Force every visible row to rebind (colour/size), not just a
        // layout pass — ListView recycling can otherwise keep old rows.
        if (listView != null) listView.invalidateViews();
    }

    void recolorScrubbers(ViewGroup parent, int color) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout lay = (LinearLayout) child;
                // A scrubber is a vertical strip of single-letter TextViews.
                if (lay.getOrientation() == LinearLayout.VERTICAL
                        && lay.getChildCount() == LETTERS.length()) {
                    boolean lettersOnly = true;
                    for (int j = 0; j < lay.getChildCount(); j++) {
                        View c = lay.getChildAt(j);
                        if (!(c instanceof TextView)
                                || ((TextView) c).getText().length() != 1) {
                            lettersOnly = false;
                            break;
                        }
                    }
                    if (lettersOnly) {
                        for (int j = 0; j < lay.getChildCount(); j++) {
                            ((TextView) lay.getChildAt(j)).setTextColor(color);
                        }
                    }
                }
            }
        }
    }

    void cycleAccent(int dir) {
        accentIndex = (accentIndex + dir + PALETTE.length) % PALETTE.length;
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt(KEY_ACCENT, accentIndex).apply();
        titleView.setTextColor(PALETTE[accentIndex]);
        adapter.notifyDataSetChanged();
    }

    void saveTheme() {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt(KEY_LABEL_MODE, labelMode)
                .putInt(KEY_BG, bgMode)
                .putInt(KEY_FONT, fontSize)
                .apply();
        applyTheme();
    }

    void showThemeDialog() {
        final Dialog dialog = new Dialog(this);
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setBackgroundColor(BLACK);
        view.setPadding(dp(20), dp(20), dp(20), dp(20));

        TextView title = new TextView(this);
        title.setText("🎨 Theme ရွေးပါ");
        title.setTextSize(20);
        title.setTextColor(GOLD);
        title.setPadding(0, 0, 0, dp(16));
        view.addView(title);

        addSection(view, "Label အရောင်");
        addOption(view, dialog, "အရောင်စုံ (RED/GOLD/BLUE/WHITE)", labelMode == 0,
                new Runnable() { @Override public void run() { labelMode = 0; } });
        addOption(view, dialog, "အဖြူချည်း", labelMode == 1,
                new Runnable() { @Override public void run() { labelMode = 1; } });
        addOption(view, dialog, "ရွှေရောင်ချည်း", labelMode == 2,
                new Runnable() { @Override public void run() { labelMode = 2; } });
        addOption(view, dialog, "အနီချည်း", labelMode == 3,
                new Runnable() { @Override public void run() { labelMode = 3; } });
        addOption(view, dialog, "အပြာချည်း", labelMode == 4,
                new Runnable() { @Override public void run() { labelMode = 4; } });

        addSection(view, "နောက်ခံ");
        addOption(view, dialog, "အနက်", bgMode == 0,
                new Runnable() { @Override public void run() { bgMode = 0; } });
        addOption(view, dialog, "မီးခိုး", bgMode == 1,
                new Runnable() { @Override public void run() { bgMode = 1; } });
        addOption(view, dialog, "အဖြူ", bgMode == 2,
                new Runnable() { @Override public void run() { bgMode = 2; } });

        addSection(view, "စာလုံးအရွယ်");
        addOption(view, dialog, "သေး", fontSize == 14,
                new Runnable() { @Override public void run() { fontSize = 14; } });
        addOption(view, dialog, "လတ်", fontSize == 18,
                new Runnable() { @Override public void run() { fontSize = 18; } });
        addOption(view, dialog, "ကြီး", fontSize == 22,
                new Runnable() { @Override public void run() { fontSize = 22; } });

        dialog.setContentView(view);
        dialog.show();
    }

    void addSection(LinearLayout view, String text) {
        TextView section = new TextView(this);
        section.setText(text);
        section.setTextSize(14);
        section.setTextColor(GREY);
        section.setPadding(0, dp(12), 0, dp(4));
        view.addView(section);
    }

    void addOption(LinearLayout view, final Dialog dialog, String text,
                   boolean selected, final Runnable action) {
        TextView row = new TextView(this);
        row.setText((selected ? "✓ " : "") + text);
        row.setTextSize(16);
        row.setTextColor(selected ? GOLD : WHITE);
        row.setBackgroundColor(DARK);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(4));
        row.setLayoutParams(lp);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action.run();
                saveTheme();
                dialog.dismiss();
                Toast.makeText(MainActivity.this, "Theme ပြောင်းပြီ ✓",
                        Toast.LENGTH_SHORT).show();
            }
        });
        view.addView(row);
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

    // v16: app loading runs off the UI thread. Querying every app and
    // loading every icon on the main thread (v14/v15) is what caused the
    // occasional hitch when returning home; the old list stays visible
    // until the fresh one is ready.
    void loadAppsAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final ArrayList<AppEntry> loaded = queryApps();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        fullApps.clear();
                        fullApps.addAll(loaded);
                        applyFilter();
                    }
                });
            }
        }).start();
    }

    ArrayList<AppEntry> queryApps() {
        ArrayList<AppEntry> out = new ArrayList<AppEntry>();
        try {
            PackageManager pm = getPackageManager();
            Intent main = new Intent(Intent.ACTION_MAIN);
            main.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> infos = pm.queryIntentActivities(main, 0);
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
                out.add(app);
            }
            Collections.sort(out, new Comparator<AppEntry>() {
                @Override
                public int compare(AppEntry a, AppEntry b) {
                    return a.label.compareToIgnoreCase(b.label);
                }
            });
        } catch (Exception e) {
            // Return whatever we got; never crash the home screen.
        }
        return out;
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
        if (gestureServiceEnabled()) {
            // Service is ON: hide the button; the gesture itself is the UI.
            gestureBtn.setVisibility(View.GONE);
        } else {
            gestureBtn.setVisibility(View.VISIBLE);
            gestureBtn.setText("🖐 System Gesture ဖွင့်ရန် နှိပ်ပါ");
        }
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

    // ---- list adapter: icon + label, colours/sizes from the theme ----
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
            labelView.setTextSize(fontSize);
            labelView.setTextColor(labelColorFor(position));
            return row;
        }
    }
}
