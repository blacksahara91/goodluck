package com.ember.demo;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Slipknot Launcher - a simple home-screen launcher with a
 * black / blood-red Slipknot aesthetic. No permissions, no network,
 * no third-party libs. Pure programmatic UI.
 *
 * Features:
 *  - Acts as a home launcher (HOME + DEFAULT categories in manifest).
 *  - Grid of all launchable installed apps (icon + label), alphabetical.
 *  - Search box filters apps as you type.
 *  - Favorites row (Phone, Messages, Chrome, Camera) when installed.
 *  - Button to open Android home settings to set as default launcher.
 *  - Burmese UI labels throughout.
 */
public class MainActivity extends Activity {

    static final int BG = 0xFF000000;      // black
    static final int ACCENT = 0xFFCC0000;  // blood red
    static final int CARD = 0xFF141414;    // near-black card
    static final int TEXT = 0xFFFFFFFF;
    static final int SUB = 0xFFAAAAAA;

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
    EditText searchBox;
    TextView countView;
    LinearLayout favRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pm = getPackageManager();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(12), dp(16), dp(12), dp(8));

        // ---- header ----
        TextView title = new TextView(this);
        title.setText("SLIPKNOT LAUNCHER");
        title.setTextColor(ACCENT);
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, wrapLp());

        TextView sub = new TextView(this);
        sub.setText("အိမ်မျက်နှာပြင် — အက်ပ်အားလုံး ဒီမှာရှိတယ်");
        sub.setTextColor(SUB);
        sub.setTextSize(14);
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = wrapLp();
        subLp.setMargins(0, dp(4), 0, dp(10));
        root.addView(sub, subLp);

        // ---- favorites ----
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
        searchBox.setBackgroundColor(CARD);
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
        GridView grid = new GridView(this);
        grid.setNumColumns(4);
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setVerticalSpacing(dp(8));
        grid.setHorizontalSpacing(dp(4));
        grid.setGravity(Gravity.CENTER);
        grid.setSelector(android.R.color.transparent);
        gridAdapter = new AppGridAdapter();
        grid.setAdapter(gridAdapter);
        LinearLayout.LayoutParams gridLp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(grid, gridLp);

        // ---- set-as-default button ----
        Button homeBtn = new Button(this);
        homeBtn.setText("မူလ Home အဖြစ် သတ်မှတ်ရန်");
        homeBtn.setBackgroundColor(ACCENT);
        homeBtn.setTextColor(TEXT);
        homeBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                openHomeSettings();
            }
        });
        LinearLayout.LayoutParams homeLp = wrapLp();
        homeLp.setMargins(0, dp(8), 0, 0);
        root.addView(homeBtn, homeLp);

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadApps();
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

            ImageView iv = new ImageView(this);
            if (icon != null) {
                iv.setImageDrawable(icon);
            }
            item.addView(iv,
                    new LinearLayout.LayoutParams(dp(44), dp(44)));

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
                label.setTextColor(TEXT);
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
