package com.ember.demo;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

/**
 * System gesture support for Slipknot Launcher.
 *
 * A launcher cannot intercept the phone's Back/Home/Recents keys itself,
 * so - like every gesture-navigation app - this uses an accessibility
 * service. Thin invisible overlay strips on the screen edges turn swipes
 * into system actions that work in ANY app:
 *
 *   left edge swipe in   -> Back
 *   right edge swipe in  -> Back
 *   bottom edge swipe up        -> Home
 *   bottom edge long swipe up   -> Recents
 *   bottom edge swipe up + hold -> Recents
 *
 * The user must switch this service on by hand in
 * Settings > Accessibility > Installed services > Slipknot Gesture.
 * It reads no window content (canRetrieveWindowContent is false in the
 * service config) and only watches its own edge strips.
 */
public class GestureService extends AccessibilityService {

    // WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY (API 22+).
    // Literal value so this file also compiles against older stub jars;
    // the real compileSdk 34 has the constant with this same value.
    static final int TYPE_OVERLAY = 2032;

    static final int MODE_LEFT = 0;
    static final int MODE_RIGHT = 1;
    static final int MODE_BOTTOM = 2;

    WindowManager wm;
    Vibrator vibrator;
    View leftStrip;
    View rightStrip;
    View bottomStrip;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        addStrips();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Not used: gestures come only from our own edge strips.
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public boolean onUnbind(Intent intent) {
        removeStrips();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        removeStrips();
        super.onDestroy();
    }

    // ---- overlay strips ----

    void addStrips() {
        if (wm == null) return;
        removeStrips();
        int screenH = getResources().getDisplayMetrics().heightPixels;
        int sideH = Math.max(200, screenH - dp(48)); // leave the status-bar strip free

        leftStrip = makeStrip(new StripListener(MODE_LEFT));
        wm.addView(leftStrip, stripParams(dp(20), sideH, Gravity.LEFT | Gravity.TOP, 0, dp(48)));

        rightStrip = makeStrip(new StripListener(MODE_RIGHT));
        wm.addView(rightStrip, stripParams(dp(20), sideH, Gravity.RIGHT | Gravity.TOP, 0, dp(48)));

        bottomStrip = makeStrip(new StripListener(MODE_BOTTOM));
        wm.addView(bottomStrip, stripParams(
                WindowManager.LayoutParams.MATCH_PARENT, dp(22),
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0));
    }

    View makeStrip(View.OnTouchListener listener) {
        View v = new View(this);
        v.setBackgroundColor(0x10FFFFFF); // nearly invisible
        v.setOnTouchListener(listener);
        return v;
    }

    WindowManager.LayoutParams stripParams(int w, int h, int gravity, int x, int y) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                w, h,
                TYPE_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = gravity;
        lp.x = x;
        lp.y = y;
        return lp;
    }

    void removeStrips() {
        if (wm == null) return;
        View[] vs = {leftStrip, rightStrip, bottomStrip};
        for (View v : vs) {
            if (v != null) {
                try { wm.removeView(v); } catch (Exception ignored) { }
            }
        }
        leftStrip = rightStrip = bottomStrip = null;
    }

    // ---- swipe detection per strip ----

    final class StripListener implements View.OnTouchListener {
        final int mode;
        float downX;
        float downY;
        long downT;
        float maxUp;
        boolean fired;

        StripListener(int mode) {
            this.mode = mode;
        }

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            int action = e.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                downX = e.getRawX();
                downY = e.getRawY();
                downT = e.getEventTime();
                maxUp = 0;
                fired = false;
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                float dx = e.getRawX() - downX;
                float dy = e.getRawY() - downY;
                if (mode == MODE_LEFT && dx > dp(46)) {
                    fire(GLOBAL_ACTION_BACK);
                } else if (mode == MODE_RIGHT && dx < -dp(46)) {
                    fire(GLOBAL_ACTION_BACK);
                } else if (mode == MODE_BOTTOM) {
                    if (-dy > maxUp) maxUp = -dy;
                    // swipe up and hold in place -> Recents
                    if (maxUp > dp(56) && e.getEventTime() - downT > 500) {
                        fire(GLOBAL_ACTION_RECENTS);
                    }
                }
                return true;
            }
            if (action == MotionEvent.ACTION_UP
                    || action == MotionEvent.ACTION_CANCEL) {
                if (mode == MODE_BOTTOM && !fired) {
                    if (maxUp > dp(150)) {
                        fire(GLOBAL_ACTION_RECENTS);
                    } else if (maxUp > dp(52)) {
                        fire(GLOBAL_ACTION_HOME);
                    }
                }
                fired = true; // nothing more until the next touch
                return true;
            }
            return true;
        }

        void fire(int globalAction) {
            if (fired) return;
            fired = true;
            tick();
            try {
                performGlobalAction(globalAction);
            } catch (Exception ignored) { }
        }
    }

    @SuppressWarnings("deprecation")
    void tick() {
        try {
            if (vibrator != null) vibrator.vibrate(18);
        } catch (Exception ignored) { }
    }

    int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
