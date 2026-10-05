package com.ember.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("\u1019\u1004\u103a\u1039\u1002\u101c\u102c\u1015\u102b! Ember Builder \u1000 \u1011\u102f\u1010\u103a\u1010\u1032\u1037 APK \u1015\u102b \u104b");
        tv.setTextSize(24);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(48, 48, 48, 48);
        setContentView(tv);
    }
}
