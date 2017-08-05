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
    private static final float SCALE_TOP = 0.65f;
    private static final float SCALE_CENTER = 0.95f;
    private static final float SCALE_BOTTOM = 0.85f;
    private static final float SCALE_CENTER_TO_TOP = SCALE_CENTER - SCALE_TOP;
    private static final float SCALE_CENTER_TO_BOTTOM = SCALE_CENTER - SCALE_BOTTOM;

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

            if (viewTop < activeCardTop) {
                final float ratio = (float) viewTop / activeCardTop;
                scale = SCALE_TOP + SCALE_CENTER_TO_TOP * ratio;
                alpha = 0.1f + ratio;
                z = Z_CENTER_1 * ratio;
                y = 0;
            } else if (viewTop < activeCardCenter) {
                scale = SCALE_CENTER;
                alpha = 1;
                z = Z_CENTER_1;
                y = 0;
            } else if (viewTop < activeCardBottom) {
                final float ratio = (float) (viewTop - activeCardCenter) / (activeCardBottom - activeCardCenter);
                scale = SCALE_CENTER - SCALE_CENTER_TO_BOTTOM * ratio;
                alpha = 1;
                z = Z_CENTER_2;
                //transitionDistance卡片长度，transitionEnd卡片中间点y坐标,
//                y = -Math.min(transitionBottom2Center, transitionBottom2Center * (viewTop - transitionEnd) / transitionDistance);
                y = 0;
//                Log.i("viewUpdater", "index:" + i + "---updateView---center2bottom>>>y:" + y);
            } else {
                scale = SCALE_BOTTOM;
                alpha = 1;
                z = Z_RIGHT;
                y=0;
//                if (lm.getActiveCardPosition() < lm.getPosition(view)) {
//                    y = lm.getHeight() - lm.getDecoratedTop(view);
//                }else{
//                    y=0;
//                }
//                if (prevView != null) {
//                    final float prevViewScale;
//                    float prevTransition = 0;
//                    final int prevBottom;
//
//                    final boolean isFirstBottom = lm.getDecoratedBottom(prevView) <= activeCardBottom;
//                    if (isFirstBottom) {
//                        prevViewScale = SCALE_CENTER;
//                        prevBottom = activeCardBottom;
//                        prevTransition = 0;
//                    } else {
//                        prevViewScale = ViewCompat.getScaleY(prevView);
//                        prevBottom = lm.getDecoratedBottom(prevView);
//                        if (ViewCompat.getTranslationY(prevView) - prevTransition > 300) {
//                            Log.i("viewUpdater", "---突变---");
//                        }
//                        prevTransition = ViewCompat.getTranslationY(prevView);
//                    }
//
//                    final float prevBorder = (cardHeight - cardHeight * prevViewScale) / 2;
//                    final float currentBorder = (cardHeight - cardHeight * SCALE_BOTTOM) / 2;
//                    final float distance = (viewTop + currentBorder) - (prevBottom - prevBorder + prevTransition);
//
//                    final float transition = distance - cardsGap;
//                    y = -transition;
//                    Log.i("viewUpdater", "index:" + i + "---updateView---bottom2end>>>y:" + y + "   ,isFirstBottom:" + isFirstBottom + " ,prevBottom:" + prevBottom + " ,preTransition:" + prevTransition);
//
//                } else {
//                    y = 0;
//                }

            }
//            Log.i("viewUpdater", "---updateView---y:" + y + ",scale:" + scale + ",z:" + z + ",index:" + i);
            onUpdateViewScale(view, scale);
//            onUpdateViewTransitionY(view, y);//注释掉之后没有了因为gap变大而出现的顿挫（y坐标漂移）
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

    protected void onUpdateViewTransitionX(@NonNull View view, float x) {
        if (ViewCompat.getTranslationX(view) != x) {
            ViewCompat.setTranslationX(view, x);
        }
    }

    protected void onUpdateViewTransitionY(@NonNull View view, float y) {
        if (ViewCompat.getTranslationY(view) != y) {
            ViewCompat.setTranslationY(view, y);
        }
    }
}
