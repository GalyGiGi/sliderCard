package com.ramotion.cardslider;

import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import java.security.InvalidParameterException;

/**
 * Created by zengcheng on 2017/7/31.
 */

public class VerticalCardSanpHelper extends LinearSnapHelper {
    private static final String TAG = "VerticalCardSanpHelper";
    private RecyclerView recyclerView;

    /**
     * Attaches the {@link CardSnapHelper} to the provided RecyclerView, by calling
     * {@link RecyclerView#setOnFlingListener(RecyclerView.OnFlingListener)}.
     * You can call this method with {@code null} to detach it from the current RecyclerView.
     *
     * @param recyclerView The RecyclerView instance to which you want to add this helper or
     *                     {@code null} if you want to remove SnapHelper from the current
     *                     RecyclerView.
     * @throws IllegalArgumentException  if there is already a {@link RecyclerView.OnFlingListener}
     *                                   attached to the provided {@link RecyclerView}.
     * @throws InvalidParameterException if provided RecyclerView has LayoutManager which is not
     *                                   instance of CardSliderLayoutManager
     */
    @Override
    public void attachToRecyclerView(@Nullable RecyclerView recyclerView) throws IllegalStateException {
        super.attachToRecyclerView(recyclerView);

        if (recyclerView != null && !(recyclerView.getLayoutManager() instanceof CardSliderLayoutManager)) {
            throw new InvalidParameterException("LayoutManager must be instance of CardSliderLayoutManager");
        }

        this.recyclerView = recyclerView;
    }

    @Override
    public int findTargetSnapPosition(RecyclerView.LayoutManager layoutManager, int velocityX, int velocityY) {
        final CardSliderLayoutManager lm = (CardSliderLayoutManager) layoutManager;

        final int itemCount = lm.getItemCount();
        if (itemCount == 0) {
            return RecyclerView.NO_POSITION;
        }

        final RecyclerView.SmoothScroller.ScrollVectorProvider vectorProvider =
                (RecyclerView.SmoothScroller.ScrollVectorProvider) layoutManager;

        final PointF vectorForEnd = vectorProvider.computeScrollVectorForPosition(itemCount - 1);
        if (vectorForEnd == null) {
            return RecyclerView.NO_POSITION;
        }

        final int distance = calculateScrollDistance(velocityX, velocityY)[0];
        int deltaJump;

        if (distance > 0) {
            deltaJump = (int) Math.floor(distance / lm.getCardHeight());
        } else {
            deltaJump = (int) Math.ceil(distance / lm.getCardHeight());
        }

        final int deltaSign = Integer.signum(deltaJump);
        deltaJump = deltaSign * Math.min(3, Math.abs(deltaJump));

        if (vectorForEnd.y < 0) {
            deltaJump = -deltaJump;
        }

        if (deltaJump == 0) {
            return RecyclerView.NO_POSITION;
        }

        final int currentPosition = lm.getActiveCardPosition();
        if (currentPosition == RecyclerView.NO_POSITION) {
            return RecyclerView.NO_POSITION;
        }

        int targetPos = currentPosition + deltaJump;
        if (targetPos < 0 || targetPos >= itemCount) {
            targetPos = RecyclerView.NO_POSITION;
        }

        return targetPos;
    }

    @Override
    public View findSnapView(RecyclerView.LayoutManager layoutManager) {
        return ((CardSliderLayoutManager) layoutManager).getTopView();
    }

    @Override
    public int[] calculateDistanceToFinalSnap(@NonNull RecyclerView.LayoutManager layoutManager,
                                              @NonNull View targetView) {
        final CardSliderLayoutManager lm = (CardSliderLayoutManager) layoutManager;
        final int viewTop = lm.getDecoratedTop(targetView);
        final int activeCardTop = lm.getActiveCardTop();
        final int activeCardCenter = lm.getActiveCardTop() + lm.getCardHeight() / 2;
        final int activeCardBottom = lm.getActiveCardTop() + lm.getCardHeight();

        int[] out = new int[]{0, 0};
        int[] offset = new int[]{0, 0};
        if (viewTop < activeCardCenter) {
            final int targetPos = lm.getPosition(targetView);
            final int activeCardPos = lm.getActiveCardPosition();
            if (targetPos != activeCardPos) {
                out[1] = -(activeCardPos - targetPos) * lm.getCardHeight();
            } else {
                out[1] = viewTop - activeCardTop;
            }
            Log.i("snapHelper", "---calculateDistanceToFinalSnap条件1---out[1]:" + out[1] + " ,targetPos:" + targetPos + " ,activeCardPos:" + activeCardPos + " ,viewTop:" + viewTop + " ,activeCardTop:" + activeCardTop);
        } else {
//            out[1] = viewTop - activeCardBottom + 1;//如果不加一，activeCard位置会往下移动1个像素
            out[1] = viewTop - activeCardBottom;//注意这里是负数,这个数字只能保证屏幕范围内最底下的卡片滑出屏幕，还需保证它上面的卡片能滑动到activeCardTop,以及再往上的卡片对应的位置
            //zc --------------------------------------------
//            final int targetPos = lm.getPosition(targetView);
//            int maxOffset = viewTop - activeCardBottom;
//            int size = lm.getChildCount();
//            View upperView;
//            for (int i = 0; i < size; i++) {
//                upperView = lm.getChildAt(i);
//                int upperViewTop = lm.getDecoratedTop(upperView);
//                if (upperViewTop < activeCardTop) {
//                    int distance;
//                    int posDistance = targetPos - lm.getPosition(upperView);
//                    if (posDistance == 1) {//此卡片为activeCard上面的第一张，即将成为activeCard
//                        distance = activeCardTop - upperViewTop;
//                    } else if (posDistance == 2) {
//                        distance = (int) (activeCardTop - lm.getCardGap2to3() - upperViewTop);
//                        distance = (int) Math.ceil(distance * 1f * lm.getCardHeight() / lm.getCardGap2to3());
//                    } else {
//                        distance = (int) (activeCardTop - lm.getCardGap2to3() - (posDistance - 2) * lm.getCardGap1to2() - upperViewTop);
//                        distance = (int) Math.ceil(distance * 1f * lm.getCardHeight() / lm.getCardGap1to2());
//                    }
////                    assert distance >= 0;//理论值不能为负数
//                    if (distance < 0) {
//                        Log.e(TAG, "calculateDistanceToFinalSnap>wrong position for :" + lm.getPosition(upperView) + " ,posDistance:" + posDistance);
//                    }
//                    distance = -distance;
//                    if (Math.abs(distance) > Math.abs(maxOffset)) {
//                        maxOffset = distance;
//                    }
//                }
//            }
//            out[1] = maxOffset;
            //zc --------------------------------------------
        }
//        Log.i(TAG, "---calculateDistanceToFinalSnap---out[1]:" + out[1]);
//        Log.i(TAG, "---calculateDistanceToFinalSnap---out[1]:" + out[1] + " ,targetPos:" + lm.getPosition(targetView) + " ,activeCardPos:" + lm.getActiveCardPosition() + " ,targetViewTop:" + lm.getDecoratedTop(targetView));
//        if (out[1] > 0) {
//            Log.e(TAG, "！！！calculateDistanceToFinalSnap！！！out[1]:" + out[1] + " ,targetPos:" + lm.getPosition(targetView) + " ,activeCardPos:" + lm.getActiveCardPosition() + " ,targetViewTop:" + lm.getDecoratedTop(targetView));
//
//        }
        if (out[1] != 0) {
            recyclerView.smoothScrollBy(0, out[1], new AccelerateInterpolator());
        }


        return offset;
    }

    @Nullable
    @Override
    protected LinearSmoothScroller createSnapScroller(RecyclerView.LayoutManager layoutManager) {
        return ((CardSliderLayoutManager) layoutManager).getSmoothScroller(recyclerView);
    }
}

