package com.ramotion.cardslider;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

/**
 * Created by zengcheng on 2017/7/28.
 */

public class VerticalViewUpdater extends ViewUpdater {
    private static final String TAG = "VerticalViewUpdater";
    private static final float SCALE_TOP = 0.65f;
    private static final float SCALE_CENTER = 0.95f;
    private static final float SCALE_BOTTOM = 0.85f;
    private static final float SCALE_CENTER_TO_TOP = SCALE_CENTER - SCALE_TOP;
    private static final float SCALE_CENTER_TO_BOTTOM = SCALE_CENTER - SCALE_BOTTOM;
    private static final float ALPHA_TOP = 0.5f;
    private static final float ALPHA_ACTIVE = 1.0f;
    private static final float ALPHA_BOTTOM = 0.0f;
    private static final int Z_CENTER_1 = 12;
    private static final int Z_CENTER_2 = 16;
    private static final int Z_RIGHT = 8;

    private int cardHeight;
    private int activeCardTop;
    private int activeCardBottom;
    private int activeCardCenter;
    private float cardsGap;

    private int transitionEnd;
    private int transitionDistance;
    private float transitionBottom2Center;

    public VerticalViewUpdater(CardSliderLayoutManager lm) {
        super(lm);
    }

    @Override
    public void onLayoutManagerInitialized() {
        this.cardHeight = lm.getCardHeight();
        this.activeCardTop = lm.getActiveCardTop();
        this.activeCardBottom = lm.getActiveCardBottom();
        this.activeCardCenter = lm.getActiveCardCenter();
        this.cardsGap = lm.getCardsGap();

        this.transitionEnd = activeCardCenter;
        this.transitionDistance = activeCardBottom - transitionEnd;

        final float centerBorder = (cardHeight - cardHeight * SCALE_CENTER) / 2f;
        final float bottomBorder = (cardHeight - cardHeight * SCALE_BOTTOM) / 2f;
        final float bottom2centerDistance = (activeCardBottom + centerBorder) - (activeCardBottom - bottomBorder);
        this.transitionBottom2Center = bottom2centerDistance - cardsGap;

    }

    @Override
    public int getActiveCardPosition() {
        int result = RecyclerView.NO_POSITION;

        View biggestView = null;
        float lastScaleY = 0f;

        for (int i = 0, cnt = lm.getChildCount(); i < cnt; i++) {
            final View child = lm.getChildAt(i);
            final int viewTop = lm.getDecoratedTop(child);
            if (viewTop >= activeCardBottom) {
                continue;
            }

            final float scaleY = ViewCompat.getScaleY(child);
            if (lastScaleY < scaleY && viewTop < activeCardCenter) {
                lastScaleY = scaleY;
                biggestView = child;
            }
        }

        if (biggestView != null) {
            result = lm.getPosition(biggestView);
        }

        return result;
    }

    @Nullable
    @Override
    public View getTopView() {
        if (lm.getChildCount() == 0) {
            return null;
        }

        View result = null;
        float lastValue = cardHeight;

        for (int i = 0, cnt = lm.getChildCount(); i < cnt; i++) {
            final View child = lm.getChildAt(i);
            if (lm.getDecoratedTop(child) >= activeCardBottom) {
                continue;
            }

            final int viewTop = lm.getDecoratedTop(child);
            final int diff = activeCardBottom - viewTop;
            if (diff < lastValue) {
                lastValue = diff;
                result = child;
            }
        }

        return result;
    }

    @Override
    public void updateView() {
        View prevView = null;

        for (int i = 0, cnt = lm.getChildCount(); i < cnt; i++) {
            final View view = lm.getChildAt(i);
            final int viewTop = lm.getDecoratedTop(view);

            final float scale;
            final float alpha;
            final float z;
            final float y;
            if (viewTop < activeCardTop - lm.getCardGap2to3()) {//最顶部可能会被滑出屏幕的卡片
                View bottomView = lm.getChildAt(i + 3);//最底部即将出屏幕的卡片

                if (bottomView != null && lm.getDecoratedTop(bottomView) <= activeCardBottom) {
                    int bottomViewTop = lm.getDecoratedTop(bottomView);
                    final float alphaRatio = (float) Math.abs(bottomViewTop - activeCardCenter) / (activeCardBottom - activeCardCenter);
                    alpha = ALPHA_TOP * alphaRatio;
                } else {
                    alpha = ALPHA_TOP;
                }
                final float ratio = (float) viewTop / activeCardTop;
                scale = SCALE_TOP + SCALE_CENTER_TO_TOP * ratio;
                z = Z_CENTER_1 * ratio;
                y = 0;
            } else if (viewTop < activeCardTop) {
                final float ratio = (float) viewTop / activeCardTop;
                scale = SCALE_TOP + SCALE_CENTER_TO_TOP * ratio;
                final float alphaRatio = (activeCardTop - viewTop) / lm.getCardGap2to3();
                alpha = ALPHA_ACTIVE - (ALPHA_ACTIVE - ALPHA_TOP) * alphaRatio;
                z = Z_CENTER_1 * ratio;
                y = 0;
            } else if (viewTop < activeCardCenter) {
                scale = SCALE_CENTER;
                alpha = ALPHA_ACTIVE;
                z = Z_CENTER_1;
                y = 0;
            } else if (viewTop < activeCardBottom) {
                final float ratio = (float) (viewTop - activeCardCenter) / (activeCardBottom - activeCardCenter);
                scale = SCALE_CENTER - SCALE_CENTER_TO_BOTTOM * ratio;
                float alphaRatio = 1f * (viewTop - activeCardCenter) / (activeCardBottom - activeCardCenter);
                alpha = ALPHA_TOP - (ALPHA_TOP - ALPHA_BOTTOM) * alphaRatio;
                z = Z_CENTER_2;
                float realTranslation = transitionBottom2Center * (viewTop - transitionEnd) / transitionDistance;
                if (Math.abs(transitionBottom2Center) < Math.abs(realTranslation)) {
                    realTranslation = transitionBottom2Center;
                }
                y = -realTranslation;
                Log.i(TAG, "upestView---alpha:" + alpha + " ,position:" + lm.getPosition(view) + " ,alphaRatio:" + alphaRatio);
                //transitionDistance卡片长度，transitionEnd卡片中间点y坐标,
//                y = -Math.min(transitionBottom2Center, transitionBottom2Center * (viewTop - transitionEnd) / transitionDistance);
            } else {
                scale = SCALE_BOTTOM;
                alpha = 0.5f;
                z = Z_RIGHT;
                if (prevView != null) {
                    final float prevViewScale;
                    float prevTransition = 0;
                    final int prevBottom;

                    final boolean isFirstBottom = lm.getDecoratedBottom(prevView) <= activeCardBottom;
                    if (isFirstBottom) {
                        prevViewScale = SCALE_CENTER;
                        prevBottom = activeCardBottom;
                        prevTransition = 0;
                    } else {
                        prevViewScale = ViewCompat.getScaleY(prevView);
                        prevBottom = lm.getDecoratedBottom(prevView);
                        prevTransition = ViewCompat.getTranslationY(prevView);
                    }

                    final float prevBorder = (cardHeight - cardHeight * prevViewScale) / 2;
                    final float currentBorder = (cardHeight - cardHeight * SCALE_BOTTOM) / 2;
                    final float distance = (viewTop + currentBorder) - (prevBottom - prevBorder + prevTransition);

                    final float transition = distance - cardsGap;
                    y = -transition;

                } else {
                    y = 0;
                }

            }
            onUpdateViewScale(view, scale);
            onUpdateViewTransitionY(view, y);
            onUpdateViewZ(view, z);
            onUpdateViewAlpha(view, alpha);

            prevView = view;
        }
    }

    protected void onUpdateViewAlpha(@NonNull View view, float alpha) {
        if (ViewCompat.getAlpha(view) != alpha) {
            ViewCompat.setAlpha(view, alpha);
        }
    }

    protected void onUpdateViewScale(@NonNull View view, float scale) {
        if (ViewCompat.getScaleY(view) != scale) {
            ViewCompat.setScaleX(view, scale);
            ViewCompat.setScaleY(view, scale);
        }
    }

    protected void onUpdateViewZ(@NonNull View view, float z) {
        if (ViewCompat.getZ(view) != z) {
            ViewCompat.setZ(view, z);
        }
    }

    protected void onUpdateViewTransitionY(@NonNull View view, float y) {
        if (ViewCompat.getTranslationY(view) != y) {
            ViewCompat.setTranslationY(view, y);
        }
    }
}
