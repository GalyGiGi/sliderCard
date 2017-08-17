package com.ramotion.cardslider.examples.simple;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;

/**
 * Created by zengcheng on 2017/8/17.
 */

public class TestCardAlphaAct extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_test);
        ViewCompat.setAlpha(findViewById(R.id.card), 0.1f);
    }
}
