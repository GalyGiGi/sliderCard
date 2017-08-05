package com.ramotion.cardslider.examples.simple.cards;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.ramotion.cardslider.examples.simple.R;

/**
 * Created by zengcheng on 2017/8/2.
 */

public class MijiaDeviceCard extends RecyclerView.ViewHolder {
    private static int viewWidth = 0;
    private static int viewHeight = 0;

    public MijiaDeviceCard(View itemView) {
        super(itemView);
    }

    public void setContent(final String title) {
        if (viewWidth == 0) {
            itemView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    itemView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    viewWidth = itemView.getWidth();
                    viewHeight = itemView.getHeight();
                    ((TextView) itemView.findViewById(R.id.text_card_title)).setText(title);
                }
            });
        } else {
            ((TextView) itemView.findViewById(R.id.text_card_title)).setText(title);
        }
    }

    public void clearContent() {

    }
}
