package com.ember.demo;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Slipknot Launcher - multicolour all-at-once + gestures home launcher.
 * Pure Android framework, no third-party libs, programmatic UI, Burmese labels.
 *
 * Slipknot dark base (black) with the full colour palette shown SIMULTANEOUSLY:
 * grid item labels cycle RED/GOLD/BLUE/WHITE, favourites get coloured rings,
 * no theme switching. Swipe left/right only cycles the header accent colour.
 *
 * v13 adds PHONE SYSTEM gestures via GestureService (accessibility):
 * edge swipes act as Back / Home / Recents in every app. The button under
 * the grid opens Accessibility settings and shows whether it is on.
 */
public class MainActivity extends Activity {

    // ---- multicolour palette (all shown at once) ----
    static final int RED   = 0xFFE53935;
    static final int GOLD  = 0xFFFFCC00;
    static final int BLUE  = 0xFF2979FF;
    static final int WHITE = 0xFFFFFFFF;
    static final int[] PALETTE = {RED, GOLD, BLUE, WHITE};
    static final String[] ACCENT_NAMES = {"အနီ", "အဝါ", "အပြာ", "အဖြူ"};

    static final int BG        = 0xFF000000;
    static final int CELL_A    = 0xFF111111;
    static final int CELL_B    = 0xFF1A1A1A;
    static final int SEARCH_BG = 0xFF141414;
    static final int TEXT      = WHITE;
    static final int SUB       = 0xFFAAAAAA;

    static final String PREFS = "slipknot_prefs";
    static final String KEY_ACCENT = "accent_index";

    // header accent: cycles with swipe left/right; everything else stays multicolour
    static int ACCENT = RED;

    // { label, package-name candidates in preference order }
    static final String[][] FAVORITES = {
            {"ဖုန်း", "com.google.android.dialer",
                    "com.android.dialer", "com.samsung.android.dialer"},
            {"မက်ဆေ့ချ်", "com.google.android.apps.messaging",
                    "com.android.messaging", "com.samsung.android.messaging"},
            {"Chrome", "com.android.chrome"},
            {"ကင်မရာ", "com.google.android.GoogleCamera",
                    "com.android.camera2", "com.android.camera"},
    };

    PackageManager pm;
    final List<ResolveInfo> allApps = new ArrayList<ResolveInfo>();
    final List<ResolveInfo> shownApps = new ArrayList<ResolveInfo>();
    AppGridAdapter gridAdapter;
    GridView gridView;
    Button gestureBtn;
    EditText searchBox;
    TextView countView;
    TextView clockTime;
    TextView clockDate;
    LinearLayout favRow;
    GestureDetector gestureDetector;

    final Handler clockHandler = new Handler();
    final Runnable clockTick = new Runnable() {
        @Override public void run() {
            updateClock();
            clockHandler.postDelayed(this, 60000);
        }
    };

    // ---- gestures ----
    final class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override public boolean onFling(MotionEvent e1, MotionEvent e2,
                                         float velocityX, float velocityY) {
            if (e1 == null || e2 == null) return false;
            float dx = e2.getX() - e1.getX();
            float dy = e2.getY() - e1.getY();
            if (dy < -150 && Math.abs(velocityY) > 300) {
                // swipe up: scroll to top + focus search
                if (gridView != null) gridView.smoothScrollToPosition(0);
                focusSearch();
                return true;
            }
            if (dy > 150 && Math.abs(velocityY) > 300) {
                // swipe down: notification shade
                expandShade();
                return true;
            }
            if (dx < -150 && Math.abs(velocityX) > 300
                    && Math.abs(dy) < 250) {
                // swipe left: next header accent
                cycleAccent(1);
                return true;
            }
            if (dx > 150 && Math.abs(velocityX) > 300
                    && Math.abs(dy) < 250) {
                // swipe right: previous header accent
                cycleAccent(-1);
                return true;
            }
            return false;
        }

        @Override public boolean onDoubleTap(MotionEvent e) {
            showAlarm();
            return true;
        }

        @Override public void onLongPress(MotionEvent e) {
            pickWallpaper();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int idx = prefs.getInt(KEY_ACCENT, 0);
        if (idx < 0 || idx >= PALETTE.length) idx = 0;
        ACCENT = PALETTE[idx];

        pm = getPackageManager();
        gestureDetector = new GestureDetector(this, new GestureListener());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        // bottom padding leaves room for the gesture service's bottom strip
        root.setPadding(dp(12), dp(16), dp(12), dp(30));

        // ---- clock + date ----
        clockTime = new TextView(this);
        clockTime.setTextColor(ACCENT);
        clockTime.setTextSize(34);
        clockTime.setTypeface(Typeface.DEFAULT_BOLD);
        clockTime.setGravity(Gravity.CENTER);
        root.addView(clockTime, wrapLp());

        clockDate = new TextView(this);
        clockDate.setTextColor(SUB);
        clockDate.setTextSize(14);
        clockDate.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams dateLp = wrapLp();
        dateLp.setMargins(0, dp(2), 0, dp(8));
        root.addView(clockDate, dateLp);
        updateClock();
        clockHandler.postDelayed(clockTick, 60000);

        // ---- header ----
        TextView title = new TextView(this);
        title.setText("Slipknot Launcher");
        title.setTextColor(ACCENT);
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, wrapLp());

        TextView hint = new TextView(this);
        hint.setText("အပေါ်ဆွဲ=ရှာဖွေ · အောက်ဆွဲ=အသိပေးချက် · ဘေးဆွဲ=အရောင် · ၂ချက်ထိ=နှိုးစက် · ကြာကြာဖိ=wallpaper");
        hint.setTextColor(SUB);
        hint.setTextSize(11);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintLp = wrapLp();
        hintLp.setMargins(0, dp(4), 0, dp(8));
        root.addView(hint, hintLp);

        // ---- favourites (multicolour rings) ----
        TextView favLabel = new TextView(this);
        favLabel.setText("အသုံးများသော");
        favLabel.setTextColor(SUB);
        favLabel.setTextSize(13);
        root.addView(favLabel, wrapLp());

        favRow = new LinearLayout(this);
        favRow.setOrientation(LinearLayout.HORIZONTAL);
        favRow.setGravity(Gravity.CENTER_VERTICAL);
        HorizontalScrollView favScroll = new HorizontalScrollView(this);
        favScroll.setHorizontalScrollBarEnabled(false);
        favScroll.addView(favRow,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams favLp = wrapLp();
        favLp.setMargins(0, dp(4), 0, dp(8));
        root.addView(favScroll, favLp);

        // ---- search ----
        searchBox = new EditText(this);
        searchBox.setHint("အက်ပ်ရှာရန်...");
        searchBox.setHintTextColor(0xFF777777);
        searchBox.setTextColor(TEXT);
        searchBox.setBackgroundColor(SEARCH_BG);
        searchBox.setPadding(dp(14), dp(12), dp(14), dp(12));
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,
                                                    int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s,
                                                int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                applyFilter();
            }
        });
        LinearLayout.LayoutParams searchLp = wrapLp();
        searchLp.setMargins(0, 0, 0, dp(4));
        root.addView(searchBox, searchLp);

        countView = new TextView(this);
        countView.setTextColor(SUB);
        countView.setTextSize(12);
        countView.setGravity(Gravity.END);
        LinearLayout.LayoutParams countLp = wrapLp();
        countLp.setMargins(0, 0, dp(4), dp(4));
        root.addView(countView, countLp);

        // ---- app grid (fills remaining space, scrolls itself) ----
        gridView = new GridView(this);
        gridView.setNumColumns(4);
        gridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        gridView.setVerticalSpacing(dp(8));
        gridView.setHorizontalSpacing(dp(4));
        gridView.setGravity(Gravity.CENTER);
        gridView.setSelector(android.R.color.transparent);
        gridAdapter = new AppGridAdapter();
        gridView.setAdapter(gridAdapter);
        LinearLayout.LayoutParams gridLp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(gridView, gridLp);

        // ---- set-as-default button ----
        Button homeBtn = new Button(this);
        homeBtn.setText("မူလ Home အဖြစ် သတ်မှတ်ရန်");
        homeBtn.setBackgroundColor(RED);
        homeBtn.setTextColor(WHITE);
        homeBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                openHomeSettings();
            }
        });
        LinearLayout.LayoutParams homeLp = wrapLp();
        homeLp.setMargins(0, dp(8), 0, 0);
        root.addView(homeBtn, homeLp);

        // ---- system gesture (accessibility) button ----
        gestureBtn = new Button(this);
        gestureBtn.setBackgroundColor(GOLD);
        gestureBtn.setTextColor(0xFF000000);
        gestureBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (gestureServiceEnabled()) {
                    Toast.makeText(MainActivity.this,
                            "ဖွင့်ထားပြီ ✓ ဘယ်/ညာအစွန်းဆွဲ=Back · အောက်အစွန်းအပေါ်ဆွဲ=Home · အဝေးဆွဲ/ဖိထား=Recents",
                            Toast.LENGTH_LONG).show();
                } else {
                    openAccessibilitySettings();
                }
            }
        });
        LinearLayout.LayoutParams gesLp = wrapLp();
        gesLp.setMargins(0, dp(8), 0, 0);
        root.addView(gestureBtn, gesLp);
        refreshGestureBtn();

        TextView gesHint = new TextView(this);
        gesHint.setText("System gesture: ဘယ်/ညာ အစွန်းဆွဲ=Back · အောက် အစွန်းအပေါ်ဆွဲ=Home · အဝေးဆွဲ/ဖိထား=Recents (Accessibility မှာ အရင်ဖွင့်ရမယ်)");
        gesHint.setTextColor(SUB);
        gesHint.setTextSize(11);
        gesHint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams gesHintLp = wrapLp();
        gesHintLp.setMargins(0, dp(4), 0, 0);
        root.addView(gesHint, gesHintLp);

        // ---- gestures: forward to detector but don't consume ----
        final View.OnTouchListener forwarder = new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return false; // let taps and scrolls work normally
            }
        };
        root.setOnTouchListener(forwarder);
        gridView.setOnTouchListener(forwarder);

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadApps();
        refreshGestureBtn();
    }

    @Override
    protected void onDestroy() {
        clockHandler.removeCallbacks(clockTick);
        super.onDestroy();
    }

    // ---- header accent cycling (accent-only; rest stays multicolour) ----

    void cycleAccent(int dir) {
        int idx = 0;
        for (int i = 0; i < PALETTE.length; i++) {
            if (PALETTE[i] == ACCENT) { idx = i; break; }
        }
        idx = (idx + dir + PALETTE.length) % PALETTE.length;
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit().putInt(KEY_ACCENT, idx).apply();
        Toast.makeText(this, "အရောင်: " + ACCENT_NAMES[idx],
                Toast.LENGTH_SHORT).show();
        recreate();
    }

    // ---- clock ----

    void updateClock() {
        Date now = new Date();
        if (clockTime != null) {
            clockTime.setText(
                    new SimpleDateFormat("HH:mm", Locale.getDefault())
                            .format(now));
        }
        if (clockDate != null) {
            clockDate.setText(
                    new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .format(now));
        }
    }

    // ---- gesture actions ----

    void focusSearch() {
        if (searchBox == null) return;
        searchBox.requestFocus();
        InputMethodManager imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(searchBox, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    void expandShade() {
        try {
            Object sb = getSystemService("statusbar");
            Method m = sb.getClass().getMethod("expandNotificationsPanel");
            m.invoke(sb);
        } catch (Exception e) {
            Toast.makeText(this, "မဖွင့်နိုင်ပါ", Toast.LENGTH_SHORT).show();
        }
    }

    void showAlarm() {
        try {
            startActivity(new Intent(AlarmClock.ACTION_SHOW_ALARMS));
        } catch (Exception e) {
            Toast.makeText(this, "မဖွင့်နိုင်ပါ", Toast.LENGTH_SHORT).show();
        }
    }

    void pickWallpaper() {
        try {
            startActivity(new Intent(Intent.ACTION_SET_WALLPAPER));
        } catch (Exception e) {
            Toast.makeText(this, "မဖွင့်နိုင်ပါ", Toast.LENGTH_SHORT).show();
        }
    }

    // ---- data ----

    void reloadApps() {
        Intent main = new Intent(Intent.ACTION_MAIN);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(main, 0);
        Collections.sort(list, new Comparator<ResolveInfo>() {
            @Override public int compare(ResolveInfo a, ResolveInfo b) {
                String la = a.loadLabel(pm).toString();
                String lb = b.loadLabel(pm).toString();
                return la.compareToIgnoreCase(lb);
            }
        });
        allApps.clear();
        allApps.addAll(list);
        applyFilter();
        buildFavorites();
    }

    void applyFilter() {
        String q = "";
        if (searchBox != null && searchBox.getText() != null) {
            q = searchBox.getText().toString().trim().toLowerCase();
        }
        shownApps.clear();
        for (ResolveInfo info : allApps) {
            String label = info.loadLabel(pm).toString();
            if (q.isEmpty()
                    || label.toLowerCase().contains(q)) {
                shownApps.add(info);
            }
        }
        if (countView != null) {
            countView.setText("အက်ပ် " + shownApps.size() + " ခု");
        }
        if (gridAdapter != null) {
            gridAdapter.notifyDataSetChanged();
        }
    }

    void buildFavorites() {
        favRow.removeAllViews();
        int favPos = 0;
        for (String[] row : FAVORITES) {
            String label = row[0];
            Intent launch = null;
            String foundPkg = null;
            Drawable icon = null;
            for (int i = 1; i < row.length; i++) {
                Intent cand = pm.getLaunchIntentForPackage(row[i]);
                if (cand != null) {
                    launch = cand;
                    foundPkg = row[i];
                    try {
                        icon = pm.getApplicationIcon(foundPkg);
                    } catch (Exception ignored) {
                    }
                    break;
                }
            }
            if (launch == null) {
                continue; // not installed: skip
            }
            final Intent go = launch;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER_HORIZONTAL);
            item.setPadding(dp(8), dp(4), dp(8), dp(4));
            item.setClickable(true);

            // icon with multicolour ring
            ImageView iv = new ImageView(this);
            if (icon != null) {
                iv.setImageDrawable(icon);
            }
            int ringColor = PALETTE[favPos % PALETTE.length];
            GradientDrawable ring = new GradientDrawable();
            ring.setShape(GradientDrawable.OVAL);
            ring.setColor(0xFF141414);
            ring.setStroke(dp(3), ringColor);
            iv.setBackground(ring);
            iv.setPadding(dp(6), dp(6), dp(6), dp(6));
            item.addView(iv,
                    new LinearLayout.LayoutParams(dp(56), dp(56)));

            TextView tv = new TextView(this);
            tv.setText(label);
            tv.setTextColor(TEXT);
            tv.setTextSize(11);
            tv.setGravity(Gravity.CENTER);
            tv.setMaxLines(1);
            tv.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams tvLp = wrapLp();
            tvLp.width = dp(72);
            tvLp.setMargins(0, dp(4), 0, 0);
            item.addView(tv, tvLp);

            item.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    try {
                        startActivity(go);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this,
                                "ဖွင့်၍မရပါ",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
            favRow.addView(item);
            favPos++;
        }
    }

    void launchApp(ResolveInfo info) {
        try {
            ComponentName cn = new ComponentName(
                    info.activityInfo.packageName,
                    info.activityInfo.name);
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            i.setComponent(cn);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "ဖွင့်၍မရပါ", Toast.LENGTH_SHORT).show();
        }
    }

    // ---- system gesture service (accessibility) ----

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

    void openHomeSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this,
                    "Home settings ကို ဖွင့်၍မရပါ",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ---- grid adapter ----

    final class AppGridAdapter extends BaseAdapter {
        @Override public int getCount() {
            return shownApps.size();
        }

        @Override public Object getItem(int position) {
            return shownApps.get(position);
        }

        @Override public long getItemId(int position) {
            return position;
        }

        @Override public View getView(int position, View convertView,
                                      ViewGroup parent) {
            LinearLayout cell;
            ImageView icon;
            TextView label;
            if (convertView == null) {
                cell = new LinearLayout(MainActivity.this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER_HORIZONTAL);
                cell.setPadding(dp(4), dp(8), dp(4), dp(8));

                icon = new ImageView(MainActivity.this);
                cell.addView(icon,
                        new LinearLayout.LayoutParams(dp(48), dp(48)));

                label = new TextView(MainActivity.this);
                label.setTextSize(12);
                label.setGravity(Gravity.CENTER);
                label.setMaxLines(2);
                label.setEllipsize(TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams lLp = wrapLp();
                lLp.setMargins(0, dp(4), 0, 0);
                cell.addView(label, lLp);

                cell.setTag(new View[]{icon, label});
            } else {
                cell = (LinearLayout) convertView;
                View[] tag = (View[]) cell.getTag();
                icon = (ImageView) tag[0];
                label = (TextView) tag[1];
            }

            final ResolveInfo info = shownApps.get(position);
            Drawable d = info.loadIcon(pm);
            if (d != null) {
                icon.setImageDrawable(d);
            }
            // multicolour: label cycles the palette, cells alternate dark shades
            int c = PALETTE[position % PALETTE.length];
            label.setTextColor(c);
            cell.setBackgroundColor((position / 4) % 2 == 0 ? CELL_A : CELL_B);
            label.setText(info.loadLabel(pm));
            cell.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    launchApp(info);
                }
            });
            return cell;
        }
    }

    // ---- helpers ----

    LinearLayout.LayoutParams wrapLp() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
