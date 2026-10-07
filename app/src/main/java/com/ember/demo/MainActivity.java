package com.ember.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI App Maker - simplified one-click AI app builder.
 * Type what you want in Burmese, tap once, free AI builds a
 * single-file HTML app, it is saved locally and opened instantly.
 */
public class MainActivity extends Activity {

    static final int BG = 0xFF121212;
    static final int CARD = 0xFF1E1E1E;
    static final int ACCENT = 0xFFFF8C1A;
    static final int GREEN = 0xFF2ECC71;
    static final int TEXT = 0xFFFFFFFF;
    static final int SUB = 0xFFAAAAAA;
    static final int DANGER = 0xFFE74C3C;

    static final String API_URL = "https://text.pollinations.ai/openai";
    static final String PREFS = "ai_app_maker";
    static final String KEY_INDEX = "apps_index";

    static final String SYSTEM_PROMPT =
            "You are a mobile web app builder. The user describes an app (in Burmese or English). "
            + "Respond with ONLY one complete self-contained HTML file: inline <style> and <script>, "
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">, mobile-friendly layout, "
            + "big touch buttons, clean modern design. ALL visible UI text must be in Burmese (Myanmar language). "
            + "Output raw HTML only, starting with <!DOCTYPE html>. No explanations, no markdown fences.";

    static final String[] EXAMPLES = {
            "\uD83D\uDCDD \u1019\u103E\u1010\u103A\u1005\u102F\u101E\u102D\u1019\u103A\u1038\u1010\u1032\u1037 app",
            "\uD83E\uDDEE \u101B\u102D\u102F\u1038\u101B\u103E\u1004\u103A\u1038\u1010\u1032\u1037 \u1010\u103D\u1000\u103A\u1005\u1000\u103A app",
            "\uD83D\uDCB0 \u1004\u103D\u1031\u1005\u102C\u101B\u1004\u103A\u1038 \u1019\u103E\u1010\u103A\u1010\u1019\u103A\u1038 app",
            "\uD83D\uDED2 \u1005\u103B\u1031\u1038\u101D\u101A\u103A\u1005\u102C\u101B\u1004\u103A\u1038 checklist app"
    };

    EditText promptInput;
    Button buildBtn;
    ProgressBar progress;
    TextView statusText;
    ListView appsList;
    TextView emptyText;
    AppsAdapter adapter;
    List<AppEntry> apps = new ArrayList<>();

    static class AppEntry {
        String file, title;
        long ts;
        AppEntry(String file, String title, long ts) {
            this.file = file; this.title = title; this.ts = ts;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        loadIndex();
        refreshList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadIndex();
        refreshList();
    }

    // ---------- UI ----------

    void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = dp(18);
        col.setPadding(p, p, p, p);

        TextView title = tv("\uD83E\uDD16 AI App Maker", 24, true, TEXT);
        col.addView(title);
        TextView sub = tv("\u1018\u102C\u1006\u1031\u102C\u1000\u103A\u1001\u103B\u1004\u103A\u101C\u1032 \u101B\u1031\u1038\u1015\u102B \u2014 \u1010\u1005\u103A\u1001\u103B\u1000\u103A\u1014\u103E\u102D\u1015\u103A\u101B\u102F\u1036\u1014\u1032\u1037 AI \u1000 \u1021\u1001\u1019\u1032\u1037 \u1006\u1031\u102C\u1000\u103A\u1015\u1031\u1038\u1019\u101A\u103A", 14, false, SUB);
        col.addView(sub);
        col.addView(space(14));

        promptInput = new EditText(this);
        promptInput.setHint("\u1025\u1015\u1019\u102B \u2014 \u1014\u1031\u1037\u1005\u1009\u103A\u1004\u103D\u1031\u1005\u102C\u101B\u1004\u103A\u1038 \u1019\u103E\u1010\u103A\u1010\u1036\u1037\u1037 app\u2026");
        promptInput.setHintTextColor(0xFF777777);
        promptInput.setTextColor(TEXT);
        promptInput.setBackgroundColor(CARD);
        promptInput.setMinLines(3);
        promptInput.setGravity(Gravity.TOP);
        promptInput.setPadding(dp(14), dp(14), dp(14), dp(14));
        promptInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        col.addView(promptInput);
        col.addView(space(10));

        // example chips
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        for (String ex : EXAMPLES) {
            Button chip = new Button(this);
            // strip emoji prefix for the prompt value
            final String val = ex.replaceAll("^[\\uD83C-\\uDBFF\\uDC00-\\uDFFF\\s]+", "");
            chip.setText(ex);
            chip.setTextColor(TEXT);
            chip.setBackgroundColor(0xFF2A2A2A);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dp(8), 0);
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> promptInput.setText(val));
            chips.addView(chip);
        }
        hsv.addView(chips);
        col.addView(hsv);
        col.addView(space(12));

        buildBtn = new Button(this);
        buildBtn.setText("\uD83D\uDE80 \u1010\u1005\u103A\u1001\u103B\u1000\u103A\u1014\u103E\u102D\u1015\u103A\u1015\u102E\u1038 \u1006\u1031\u102C\u1000\u103A");
        buildBtn.setTextColor(Color.WHITE);
        buildBtn.setBackgroundColor(ACCENT);
        buildBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        buildBtn.setTypeface(null, Typeface.BOLD);
        buildBtn.setPadding(dp(16), dp(16), dp(16), dp(16));
        buildBtn.setOnClickListener(v -> onBuild());
        col.addView(buildBtn);
        col.addView(space(8));

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(dp(48), dp(48));
        plp.gravity = Gravity.CENTER_HORIZONTAL;
        progress.setLayoutParams(plp);
        col.addView(progress);

        statusText = tv("", 14, false, SUB);
        statusText.setGravity(Gravity.CENTER_HORIZONTAL);
        statusText.setVisibility(View.GONE);
        col.addView(statusText);
        col.addView(space(16));

        TextView myApps = tv("\uD83D\uDCF1 \u1000\u103B\u103D\u1014\u103A\u1010\u1031\u102C\u1037 app \u1019\u103B\u102C\u1038", 18, true, TEXT);
        col.addView(myApps);
        col.addView(space(8));

        emptyText = tv("\u1001\u102F\u1011\u102D app \u1019\u1006\u1031\u102C\u1000\u103A\u101B\u1006\u1031\u1038\u1018\u1030\u1038\n\u1021\u1015\u1031\u102B\u1033\u1019\u103E\u102C \u101B\u1031\u1038\u1015\u102E\u1038 \u1005\u1019\u103A\u1038\u1000\u103C\u100A\u1037\u104B", 14, false, SUB);
        emptyText.setGravity(Gravity.CENTER_HORIZONTAL);
        col.addView(emptyText);

        appsList = new ListView(this);
        adapter = new AppsAdapter();
        appsList.setAdapter(adapter);
        // fixed height list inside scrollview: compute in refresh
        col.addView(appsList);
        col.addView(space(8));

        TextView foot = tv("\u1021\u1001\u1019\u1032\u1037 AI \u1018\u1031\u103A \u2014 key \u101C\u102D\u102F\u1036\u1019\u101C\u102D\u102F", 12, false, 0xFF777777);
        foot.setGravity(Gravity.CENTER_HORIZONTAL);
        col.addView(foot);

        scroll.addView(col);
        root.addView(scroll);
        setContentView(root);
    }

    TextView tv(String s, int sp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    View space(int dpv) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(dpv)));
        return v;
    }

    int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }

    // ---------- build flow ----------

    void onBuild() {
        final String prompt = promptInput.getText().toString().trim();
        if (TextUtils.isEmpty(prompt)) {
            Toast.makeText(this, "\u1018\u102C\u1006\u1031\u102C\u1000\u103A\u1001\u103B\u1004\u103A\u101C\u1032 \u1021\u101B\u1004\u103A\u101B\u1031\u1038\u1015\u102B \u2710\uFE0F", Toast.LENGTH_SHORT).show();
            return;
        }
        buildBtn.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        statusText.setVisibility(View.VISIBLE);
        statusText.setText("\u23F3 AI \u1000 \u1006\u1031\u102C\u1000\u103A\u1014\u1031\u1015\u102B\u1010\u101A\u103A\u2026\n\u0031-\u0032 \u1019\u102D\u1014\u1005\u103A \u1000\u103C\u102C\u1014\u102D\u102F\u1004\u103A\u1015\u102B\u1010\u101A\u103A");

        new Thread(() -> {
            try {
                String html = callAi(prompt);
                String title = extractTitle(html, prompt);
                String fname = saveApp(html, title);
                runOnUiThread(() -> {
                    resetBuildUi();
                    Toast.makeText(this, "\u2705 \u1006\u1031\u102C\u1000\u103A\u1015\u102E\u1038\u1015\u102B\u1010\u101A\u103A!", Toast.LENGTH_SHORT).show();
                    loadIndex();
                    refreshList();
                    openApp(fname);
                });
            } catch (Exception e) {
                final String msg = e.getMessage() != null && e.getMessage().contains("UnknownHost")
                        ? "\u274C \u1021\u1004\u103A\u1010\u102C\u1014\u1000\u103A \u1019\u101B\u1018\u1030\u1038 \u2014 \u1016\u103D\u1004\u103A\u1015\u102E\u1038 \u1015\u103C\u1014\u103A\u1000\u103C\u102D\u102F\u1038\u1005\u102C\u1038\u1015\u102B"
                        : "\u274C AI \u1000 \u1015\u103C\u1014\u103A\u1019\u1006\u103E\u1039\u1018\u1030\u1038 \u2014 \u1001\u100F\u1014\u1031\u1015\u103C\u1014\u103A\u1000\u103C\u102D\u102F\u1038\u1005\u102C\u1038\u1015\u102B";
                runOnUiThread(() -> {
                    resetBuildUi();
                    statusText.setVisibility(View.VISIBLE);
                    statusText.setText(msg);
                });
            }
        }).start();
    }

    void resetBuildUi() {
        buildBtn.setEnabled(true);
        progress.setVisibility(View.GONE);
        statusText.setVisibility(View.GONE);
    }

    String callAi(String prompt) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", "openai");
        JSONArray msgs = new JSONArray();
        msgs.put(new JSONObject().put("role", "system").put("content", SYSTEM_PROMPT));
        msgs.put(new JSONObject().put("role", "user")
                .put("content", prompt + "\n\nOutput ONLY the raw HTML."));
        body.put("messages", msgs);

        byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
        HttpURLConnection c = (HttpURLConnection) new URL(API_URL).openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setConnectTimeout(30000);
        c.setReadTimeout(180000);
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        c.setRequestProperty("Accept", "application/json");
        OutputStream os = c.getOutputStream();
        os.write(payload);
        os.flush();
        os.close();

        int code = c.getResponseCode();
        InputStream in = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line).append('\n');
        br.close();
        if (code < 200 || code >= 300) throw new Exception("HTTP " + code);

        String content = new JSONObject(sb.toString())
                .getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content");
        String html = extractHtml(content);
        if (!html.toLowerCase(Locale.US).contains("<html")) throw new Exception("bad html");
        return html;
    }

    static String extractHtml(String s) {
        int a = s.indexOf("```html");
        if (a >= 0) {
            int b = s.indexOf("```", a + 7);
            if (b > a) return s.substring(a + 7, b).trim();
        }
        int h = s.indexOf("<!DOCTYPE");
        if (h < 0) h = s.toLowerCase(Locale.US).indexOf("<html");
        int e = s.toLowerCase(Locale.US).lastIndexOf("</html>");
        if (h >= 0 && e > h) return s.substring(h, e + 7).trim();
        return s.trim();
    }

    static String extractTitle(String html, String fallback) {
        Matcher m = Pattern.compile("<title[^>]*>(.*?)</title>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
        if (m.find()) {
            String t = m.group(1).trim().replaceAll("\\s+", " ");
            if (!t.isEmpty()) return t;
        }
        return fallback.length() > 30 ? fallback.substring(0, 30) + "\u2026" : fallback;
    }

    String saveApp(String html, String title) throws Exception {
        File dir = new File(getFilesDir(), "apps");
        if (!dir.exists()) dir.mkdirs();
        String fname = "app_" + System.currentTimeMillis() + ".html";
        FileOutputStream fos = new FileOutputStream(new File(dir, fname));
        fos.write(html.getBytes(StandardCharsets.UTF_8));
        fos.close();

        JSONArray arr = readIndex();
        JSONObject o = new JSONObject();
        o.put("file", fname);
        o.put("title", title);
        o.put("ts", System.currentTimeMillis());
        arr.put(o);
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_INDEX, arr.toString()).apply();
        return fname;
    }

    JSONArray readIndex() {
        try {
            String s = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getString(KEY_INDEX, "[]");
            return new JSONArray(s);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    void loadIndex() {
        apps.clear();
        try {
            JSONArray arr = readIndex();
            for (int i = arr.length() - 1; i >= 0; i--) {
                JSONObject o = arr.getJSONObject(i);
                apps.add(new AppEntry(o.getString("file"), o.getString("title"), o.getLong("ts")));
            }
        } catch (Exception ignored) { }
    }

    void refreshList() {
        adapter.notifyDataSetChanged();
        emptyText.setVisibility(apps.isEmpty() ? View.VISIBLE : View.GONE);
        // expand listview inside scrollview
        int total = 0;
        int wSpec = View.MeasureSpec.makeMeasureSpec(
                getResources().getDisplayMetrics().widthPixels - dp(36), View.MeasureSpec.AT_MOST);
        for (int i = 0; i < adapter.getCount(); i++) {
            View v = adapter.getView(i, null, appsList);
            v.measure(wSpec,
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            total += v.getMeasuredHeight();
        }
        ViewGroup.LayoutParams lp = appsList.getLayoutParams();
        lp.height = total + appsList.getDividerHeight() * Math.max(0, adapter.getCount() - 1);
        appsList.setLayoutParams(lp);
    }

    void openApp(String fname) {
        File f = new File(new File(getFilesDir(), "apps"), fname);
        Intent i = new Intent(this, AppViewActivity.class);
        i.putExtra("file", f.getAbsolutePath());
        startActivity(i);
    }

    void deleteApp(int pos) {
        final AppEntry e = apps.get(pos);
        new AlertDialog.Builder(this)
                .setTitle("\u1016\u103B\u1000\u103A\u1019\u101C\u102C\u1038?")
                .setMessage("\u0022" + e.title + "\u0022 \u1000\u102D\u1016\u103B\u1000\u103A\u1019\u101C\u102C\u1038")
                .setPositiveButton("\u1016\u103B\u1000\u103A", (d, w) -> {
                    new File(new File(getFilesDir(), "apps"), e.file).delete();
                    try {
                        JSONArray arr = readIndex();
                        JSONArray out = new JSONArray();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.getJSONObject(i);
                            if (!o.getString("file").equals(e.file)) out.put(o);
                        }
                        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                                .putString(KEY_INDEX, out.toString()).apply();
                    } catch (Exception ignored) { }
                    loadIndex();
                    refreshList();
                })
                .setNegativeButton("\u1019\u1016\u103B\u1000\u103A\u1018\u1030\u1038", null)
                .show();
    }

    // ---------- list adapter ----------

    class AppsAdapter extends BaseAdapter {
        final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

        public int getCount() { return apps.size(); }
        public Object getItem(int p) { return apps.get(p); }
        public long getItemId(int p) { return p; }

        public View getView(final int pos, View cv, ViewGroup parent) {
            AppEntry e = apps.get(pos);
            LinearLayout row = new LinearLayout(MainActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setBackgroundColor(CARD);
            row.setPadding(dp(14), dp(12), dp(14), dp(12));
            row.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout info = new LinearLayout(MainActivity.this);
            info.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            info.setLayoutParams(ilp);
            TextView t = tv(e.title, 16, true, TEXT);
            t.setSingleLine(true);
            t.setEllipsize(TextUtils.TruncateAt.END);
            info.addView(t);
            info.addView(tv(fmt.format(new Date(e.ts)), 12, false, SUB));
            row.addView(info);

            Button open = new Button(MainActivity.this);
            open.setText("\u1016\u103D\u1004\u103A\u1037");
            open.setTextColor(Color.WHITE);
            open.setBackgroundColor(GREEN);
            open.setOnClickListener(v -> openApp(e.file));
            row.addView(open);

            Button del = new Button(MainActivity.this);
            del.setText("\u1016\u103B\u1000\u103A");
            del.setTextColor(Color.WHITE);
            del.setBackgroundColor(DANGER);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dlp.setMargins(dp(8), 0, 0, 0);
            del.setLayoutParams(dlp);
            del.setOnClickListener(v -> deleteApp(pos));
            row.addView(del);

            LinearLayout wrap = new LinearLayout(MainActivity.this);
            wrap.setOrientation(LinearLayout.VERTICAL);
            wrap.addView(row);
            wrap.addView(space(8));
            return wrap;
        }
    }
}
