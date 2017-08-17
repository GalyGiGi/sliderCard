package com.ramotion.cardslider.examples.simple.cards;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.CardView;
import android.view.View;

import com.ramotion.cardslider.CardSliderLayoutManager;
import com.ramotion.cardslider.VerticalViewUpdater;

/**
 * Created by zengcheng on 2017/7/30.
 */

public class CardsVerticalUpdater extends VerticalViewUpdater {
    public CardsVerticalUpdater(CardSliderLayoutManager lm) {
        super(lm);
    }

    @Override
    public void onLayoutManagerInitialized() {
        super.onLayoutManagerInitialized();
    }

    @Override
    protected void onUpdateViewAlpha(@NonNull View view, float alpha) {
        ViewCompat.setAlpha(view, alpha);
    }

    @Override
    protected void onUpdateViewZ(@NonNull View view, float z) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ((CardView) view).setCardElevation(Math.max(0, z));
        } else {
            super.onUpdateViewZ(view, z);
        }
    }
}
