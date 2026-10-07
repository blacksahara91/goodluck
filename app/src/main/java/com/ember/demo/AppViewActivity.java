package com.ember.demo;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Full-screen viewer for an AI-built HTML app stored locally.
 */
public class AppViewActivity extends Activity {

    WebView wv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wv = new WebView(this);
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        wv.setWebViewClient(new WebViewClient());
        setContentView(wv);

        String f = getIntent().getStringExtra("file");
        if (f != null) {
            wv.loadUrl("file://" + f);
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (wv != null && wv.canGoBack()) {
            wv.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (wv != null) {
            wv.destroy();
            wv = null;
        }
        super.onDestroy();
    }
}
