package com.ramotion.cardslider;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;
import android.view.WindowManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;

/**
 * A {@link android.support.v7.widget.RecyclerView.LayoutManager} implementation.
 */
public class CardSliderLayoutManager extends RecyclerView.LayoutManager
        implements RecyclerView.SmoothScroller.ScrollVectorProvider {
    public static final int MODE_HORIZONTAL = 0;
    public static final int MODE_VERTICL = 1;
    private static int mScrollMode = MODE_HORIZONTAL;
    private static final boolean DEFAULT_CENTER_HORIZONTAL = false;//for vertical mode是否让卡片水平居中
    private static final int DEFAULT_ACTIVE_CARD_LEFT_OFFSET = 50;
    private static final int DEFAULT_ACTIVE_CARD_TOP_OFFSET = 100;//for vertical mode
    private static final int DEFAULT_CARD_WIDTH = 148;
    private static final int DEFAULT_CARD_HEIGHT = 160;//for vertical mode
    private static final int DEFAULT_CARDS_GAP = 90;//12
    private static final int DEFAULT_CARDS_GAP_1TO2 = 84;
    private static final int DEFAULT_CARDS_GAP_2TO3 = 78;
    private static final int LEFT_CARD_COUNT = 2;
    private static final int TOP_CARD_COUNT = 2;//for vertical mode
    private final SparseArray<View> viewCache = new SparseArray<>();
    private final SparseIntArray cardsXCoords = new SparseIntArray();
    private final SparseIntArray cardsYCoords = new SparseIntArray();//for vertical mode

    private int cardWidth;
    private int cardHeight;
    private int activeCardLeft;
    private int activeCardTop;//for vertical mode
    private boolean isCenterHorizontal;
    private int activeCardLeftOffset = 0;//for vertical mode
    private int activeCardRight;
    private int activeCardBottom;//for vertical mode
    private int activeCardCenter;
    //    private int activeCardCenterVertical;//for vertical mode
    private float cardsGap;

    private float cardsGap1to2;
    private float cardsGap2to3;
    private int scrollRequestedPosition = 0;

    private ViewUpdater viewUpdater;
    private Context mContext;

    /**
     * Creates CardSliderLayoutManager with default values
     *
     * @param context Current context, will be used to access resources.
     */
    public CardSliderLayoutManager(@NonNull Context context) {
        this(context, null, 0, 0);
    }

    /**
     * Constructor used when layout manager is set in XML by RecyclerView attribute
     * "layoutManager".
     *
     * @attr ref R.styleable#CardSlider_activeCardLeftOffset
     * @attr ref R.styleable#CardSlider_cardWidth
     * @attr ref R.styleable#CardSlider_cardsGap
     */
    public CardSliderLayoutManager(@NonNull Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this.mContext = context;
        final float density = context.getResources().getDisplayMetrics().density;

        final int defaultCardWidth = (int) (DEFAULT_CARD_WIDTH * density);
        final int defaultActiveCardLeft = (int) (DEFAULT_ACTIVE_CARD_LEFT_OFFSET * density);
        final float defaultCardsGap = DEFAULT_CARDS_GAP * density;
        final int defaultActiveCardTop = (int) (DEFAULT_ACTIVE_CARD_TOP_OFFSET * density);
        final int defaultCardHeight = (int) (DEFAULT_CARD_HEIGHT * density);
        final boolean defaultCenterHorizontal = DEFAULT_CENTER_HORIZONTAL;
        final float defaultCardGap1to2 = DEFAULT_CARDS_GAP_1TO2 * density;
        final float defaultCardGap2to3 = DEFAULT_CARDS_GAP_2TO3 * density;

        if (attrs == null) {
            if (mScrollMode == MODE_VERTICL) {
                initializeVertical(defaultActiveCardTop, defaultCardHeight, defaultCardWidth, defaultCardsGap, null, defaultCenterHorizontal, defaultCardGap1to2, defaultCardGap2to3);
            } else {
                initialize(defaultActiveCardLeft, defaultCardWidth, defaultCardsGap, null);
            }
        } else {
            int attrCardWidth;
            int attrActiveCardLeft;
            float attrCardsGap;
            int attrCardHeight;
            int attrActivieCardTop;
            boolean attrCenterHorizontal;
            String viewUpdateClassName;
            int attrMode;
            float attrCardGap1to2;
            float attrCardGap2to3;
            final TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CardSlider, 0, 0);
            try {
                attrCardWidth = a.getDimensionPixelSize(R.styleable.CardSlider_cardWidth, defaultCardWidth);
                attrCardHeight = a.getDimensionPixelSize(R.styleable.CardSlider_cardHeight, defaultCardHeight);
                attrActiveCardLeft = a.getDimensionPixelSize(R.styleable.CardSlider_activeCardLeftOffset, defaultActiveCardLeft);
                attrActivieCardTop = a.getDimensionPixelSize(R.styleable.CardSlider_activeCardTopOffset, defaultActiveCardTop);
                attrCardsGap = a.getDimension(R.styleable.CardSlider_cardsGap, defaultCardsGap);
                attrCardGap1to2 = a.getDimension(R.styleable.CardSlider_cardsGap1to2, DEFAULT_CARDS_GAP_1TO2);
                attrCardGap2to3 = a.getDimension(R.styleable.CardSlider_cardsGap2to3, DEFAULT_CARDS_GAP_2TO3);
                viewUpdateClassName = a.getString(R.styleable.CardSlider_viewUpdater);
                attrMode = a.getInt(R.styleable.CardSlider_mode, MODE_HORIZONTAL);
                mScrollMode = attrMode;
                attrCenterHorizontal = a.getBoolean(R.styleable.CardSlider_centerHorizontal, defaultCenterHorizontal);
            } finally {
                a.recycle();
            }

            final ViewUpdater viewUpdater = loadViewUpdater(context, viewUpdateClassName, attrs);
            if (mScrollMode == MODE_VERTICL) {
                initializeVertical(attrActivieCardTop, attrCardHeight, attrCardWidth, attrCardsGap, viewUpdater, attrCenterHorizontal, attrCardGap1to2, attrCardGap2to3);
            } else {
                initialize(attrActiveCardLeft, attrCardWidth, attrCardsGap, viewUpdater);
            }
        }
    }

    /**
     * Creates CardSliderLayoutManager with specified values in pixels.
     *
     * @param activeCardLeft Active card offset from start of RecyclerView. Default value is 50dp.
     * @param cardWidth      Card width. Default value is 148dp.
     * @param cardsGap       Distance between cards. Default value is 12dp.
     */
    public CardSliderLayoutManager(int activeCardLeft, int cardWidth, float cardsGap) {
        initialize(activeCardLeft, cardWidth, cardsGap, null);
    }

    public static int getMode() {
        return mScrollMode;
    }

    public float getCardGap1to2() {
        return cardsGap1to2;
    }

    public float getCardGap2to3() {
        return cardsGap2to3;
    }

    private void initialize(int left, int width, float gap, @Nullable ViewUpdater updater) {
        this.cardWidth = width;
        this.activeCardLeft = left;
        this.activeCardRight = activeCardLeft + cardWidth;
        this.activeCardCenter = activeCardLeft + ((this.activeCardRight - activeCardLeft) / 2);
        this.cardsGap = gap;

        this.viewUpdater = updater;
        if (this.viewUpdater == null) {
            if (mScrollMode == MODE_VERTICL) {
                this.viewUpdater = new VerticalViewUpdater(this);
            } else {
                this.viewUpdater = new DefaultViewUpdater(this);
            }
        }
        viewUpdater.onLayoutManagerInitialized();
    }

    private void initializeVertical(int top, int height, int width, float gap, @Nullable ViewUpdater updater, boolean isCenterHorizontal, float gap1to2, float gat2to3) {
        this.cardHeight = height;
        this.cardWidth = width;//add by ZC
        this.activeCardTop = top;
        this.activeCardBottom = activeCardTop + cardHeight;
        this.activeCardCenter = activeCardTop + ((activeCardBottom - activeCardTop) / 2);
        this.cardsGap = getPhoneHeight(mContext) - activeCardBottom;//zc 强制让底下的卡片出屏幕
        this.cardsGap1to2 = gap1to2;
        this.cardsGap2to3 = gat2to3;
        this.viewUpdater = updater;
        if (this.viewUpdater == null) {
            this.viewUpdater = new VerticalViewUpdater(this);
        }
        this.isCenterHorizontal = isCenterHorizontal;
        if (isCenterHorizontal) {
            this.activeCardLeftOffset = (getPhoneWidth(mContext) - cardWidth) / 2;
        }
        viewUpdater.onLayoutManagerInitialized();
    }

    private int getPhoneWidth(Context context) {
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        return wm.getDefaultDisplay().getWidth();
    }

    private int getPhoneHeight(Context context) {
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        return wm.getDefaultDisplay().getHeight();
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.WRAP_CONTENT,
                RecyclerView.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getItemCount() == 0) {
            removeAndRecycleAllViews(recycler);
            return;
        }

        if (getChildCount() == 0 && state.isPreLayout()) {
            return;
        }

        int anchorPos = getActiveCardPosition();//最大的view在adapter中的index
//        Log.i("LAYOUTMANAGER", "onLayoutChildren---anchorPos:" + anchorPos);
        if (state.isPreLayout()) {
            final LinkedList<Integer> removedPositions = new LinkedList<>();
            for (int i = 0, cnt = getChildCount(); i < cnt; i++) {
                final View child = getChildAt(i);
                final boolean isRemoved = ((RecyclerView.LayoutParams) child.getLayoutParams()).isItemRemoved();
                if (isRemoved) {
                    removedPositions.add(getPosition(child));
                }
            }

            if (removedPositions.contains(anchorPos)) {
                final int last = removedPositions.getLast();
                final int first = removedPositions.getFirst();

                final int right = Math.min(last, getItemCount() - 1);

                int left = right;
                if (last != first) {
                    left = Math.max(first, 0);
                }

                anchorPos = Math.max(left, right);
            }
        }

        detachAndScrapAttachedViews(recycler);
        if (mScrollMode == MODE_VERTICL) {
            fill(anchorPos, recycler, state);
//            Log.i("layoutmanager", "---onLayoutChildren---,cardsYCoords.size()：" + cardsYCoords.size());
            if (cardsYCoords.size() != 0) {
                layoutByCoordsVertical();
            }
        } else {
            fill(anchorPos, recycler, state);
            if (cardsXCoords.size() != 0) {
                layoutByCoords();
            }
        }
    }

    @Override
    public boolean supportsPredictiveItemAnimations() {
        return true;
    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        removeAllViews();
    }

    @Override
    public boolean canScrollHorizontally() {
        return getChildCount() != 0 && mScrollMode == MODE_HORIZONTAL;
    }

    @Override
    public boolean canScrollVertically() {
        return getChildCount() != 0 && mScrollMode == MODE_VERTICL;
    }

    @Override
    public void scrollToPosition(int position) {
        if (position < 0 || position >= getItemCount()) {
            return;
        }

        scrollRequestedPosition = position;
        requestLayout();
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (mScrollMode != MODE_VERTICL) {
            return super.scrollVerticallyBy(dy, recycler, state);
        }
        scrollRequestedPosition = RecyclerView.NO_POSITION;
        int delta;
        if (dy < 0) {
            delta = scrollBottom(Math.max(dy, -cardHeight));
        } else {
            delta = scrollTop(dy);
        }

        fill(getActiveCardPosition(), recycler, state);

        cardsYCoords.clear();
        for (int i = 0, cnt = getChildCount(); i < cnt; i++) {
            final View view = getChildAt(i);
            cardsYCoords.put(getPosition(view), getDecoratedTop(view));
        }

        return delta;

    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (mScrollMode == MODE_VERTICL) {
            return super.scrollHorizontallyBy(dx, recycler, state);
        }
        scrollRequestedPosition = RecyclerView.NO_POSITION;

        int delta;
        if (dx < 0) {
            delta = scrollRight(Math.max(dx, -cardWidth));
        } else {
            delta = scrollLeft(dx);
        }

        fill(getActiveCardPosition(), recycler, state);

        cardsXCoords.clear();
        for (int i = 0, cnt = getChildCount(); i < cnt; i++) {
            final View view = getChildAt(i);
            cardsXCoords.put(getPosition(view), getDecoratedLeft(view));
        }

        return delta;
    }

    @Override
    public PointF computeScrollVectorForPosition(int targetPosition) {
        if (mScrollMode == MODE_VERTICL) {
            return new PointF(0, targetPosition - getActiveCardPosition());

        }
        return new PointF(targetPosition - getActiveCardPosition(), 0);
    }

    @Override
    public void smoothScrollToPosition(final RecyclerView recyclerView, RecyclerView.State state, final int position) {
        if (position < 0 || position >= getItemCount()) {
            return;
        }

        final LinearSmoothScroller scroller = getSmoothScroller(recyclerView);
        scroller.setTargetPosition(position);
        startSmoothScroll(scroller);
    }

    @Override
    public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int count) {
        final int anchorPos = getActiveCardPosition();
        if (positionStart + count <= anchorPos) {
            scrollRequestedPosition = anchorPos - 1;
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        SavedState state = new SavedState();
        state.anchorPos = getActiveCardPosition();
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable parcelable) {
        if (parcelable instanceof SavedState) {
            SavedState state = (SavedState) parcelable;
            scrollRequestedPosition = state.anchorPos;
            requestLayout();
        }
    }

    /**
     * @return active card position or RecyclerView.NO_POSITION
     */
    public int getActiveCardPosition() {
        if (scrollRequestedPosition != RecyclerView.NO_POSITION) {
//            Log.i("LAYOUTMANAGER", "---getActiveCardPosition---scrollRequestedPosition:" + scrollRequestedPosition);
            return scrollRequestedPosition;
        } else {
//            Log.i("LAYOUTMANAGER", "---getActiveCardPosition---viewUpdater.getActiveCardPosition():" + viewUpdater.getActiveCardPosition());
            return viewUpdater.getActiveCardPosition();
        }
    }

    @Nullable
    public View getTopView() {
        return viewUpdater.getTopView();
    }

    public int getActiveCardLeft() {
        return activeCardLeft;
    }

    public int getActiveCardTop() {
        return activeCardTop;
    }

    public int getActiveCardRight() {
        return activeCardRight;
    }

    public int getActiveCardBottom() {
        return activeCardBottom;
    }

    public int getActiveCardCenter() {
        return activeCardCenter;
    }

    public int getCardWidth() {
        return cardWidth;
    }

    public int getCardHeight() {
        return cardHeight;
    }

    public float getCardsGap() {
        return cardsGap;
    }

    public LinearSmoothScroller getSmoothScroller(final RecyclerView recyclerView) {
        return new LinearSmoothScroller(recyclerView.getContext()) {

            @Override
            public int calculateDxToMakeVisible(View view, int snapPreference) {
                if (mScrollMode == MODE_VERTICL) {
//                    return calculateDxToMakeVisibleVertical(view, snapPreference);
                    return super.calculateDxToMakeVisible(view, snapPreference);
                }
                final int viewStart = getDecoratedLeft(view);
                if (viewStart > activeCardLeft) {
                    return activeCardLeft - viewStart;
                } else {
                    int delta = 0;
                    int topViewPos = 0;

                    final View topView = getTopView();
                    if (topView != null) {
                        topViewPos = getPosition(topView);
                        if (topViewPos != getTargetPosition()) {
                            final int topViewLeft = getDecoratedLeft(topView);
                            if (topViewLeft >= activeCardLeft && topViewLeft < activeCardRight) {
                                delta = activeCardRight - topViewLeft;
                            }
                        }
                    }

                    return delta + (cardWidth) * Math.max(0, topViewPos - getTargetPosition() - 1);
                }
            }


            @Override
            public int calculateDyToMakeVisible(View view, int snapPreference) {
                if (mScrollMode != MODE_VERTICL) {
//                    return calculateDxToMakeVisibleVertical(view, snapPreference);
                    return super.calculateDyToMakeVisible(view, snapPreference);
                }
                final int viewStart = getDecoratedTop(view);
                Log.i("layoutmanager", "---calculateDyToMakeVisible---pos:" + getPosition(view) + " ,viewStart:" + viewStart);
                if (viewStart > activeCardTop) {
                    return activeCardTop - viewStart;
                } else {
                    int delta = 0;
                    int topViewPos = 0;

                    final View topView = getTopView();
                    if (topView != null) {
                        topViewPos = getPosition(topView);
                        if (topViewPos != getTargetPosition()) {
                            final int topViewTop = getDecoratedTop(topView);
                            if (topViewTop >= activeCardTop && topViewTop < activeCardBottom) {
                                delta = activeCardBottom - topViewTop;
                            }
                        }
                    }

                    return delta + (cardHeight) * Math.max(0, topViewPos - getTargetPosition() - 1);
                }
            }

            @Override
            protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                return 0.5f;
            }

        };
    }

    private ViewUpdater loadViewUpdater(Context context, String className, AttributeSet attrs) {
        if (className == null || className.trim().length() == 0) {
            return null;
        }

        final String fullClassName;
        if (className.charAt(0) == '.') {
            fullClassName = context.getPackageName() + className;
        } else if (className.contains(".")) {
            fullClassName = className;
        } else {
            fullClassName = CardSliderLayoutManager.class.getPackage().getName() + '.' + className;
        }

        ViewUpdater updater = null;
        try {
            final ClassLoader classLoader = context.getClassLoader();

            final Class<? extends ViewUpdater> viewUpdaterClass =
                    classLoader.loadClass(fullClassName).asSubclass(ViewUpdater.class);
            final Constructor<? extends ViewUpdater> constructor =
                    viewUpdaterClass.getConstructor(CardSliderLayoutManager.class);

            constructor.setAccessible(true);
            updater = constructor.newInstance(this);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(attrs.getPositionDescription() +
                    ": Error creating LayoutManager " + className, e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(attrs.getPositionDescription()
                    + ": Unable to find ViewUpdater" + className, e);
        } catch (InvocationTargetException | InstantiationException e) {
            throw new IllegalStateException(attrs.getPositionDescription()
                    + ": Could not instantiate the ViewUpdater: " + className, e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(attrs.getPositionDescription()
                    + ": Cannot access non-public constructor " + className, e);
        } catch (ClassCastException e) {
            throw new IllegalStateException(attrs.getPositionDescription()
                    + ": Class is not a ViewUpdater " + className, e);
        }

        return updater;
    }

    private int scrollBottom2(int dy) {  //for vertical mode
        final int childCount = getChildCount();

        if (childCount == 0) {
            return 0;
        }
        final View bottomestView = getChildAt(childCount - 1);

        final int deltaBorder = activeCardTop + getPosition(bottomestView) * cardHeight;

        final int delta = getAllowedBottomDelta(bottomestView, dy, deltaBorder);
//        final int delta = dy;


        final LinkedList<View> bottomViews = new LinkedList<>();
        final LinkedList<View> topViews = new LinkedList<>();

        for (int i = childCount - 1; i >= 0; i--) {
            final View view = getChildAt(i);
            final int viewTop = getDecoratedTop(view);

            if (viewTop >= activeCardBottom) {
                bottomViews.add(view);
            } else {
                topViews.add(view);
            }
        }
        for (View view : bottomViews) {
//            final int border = activeCardTop + getPosition(view) * cardHeight;
            final int border = getHeight();
            final int allowedDelta = getAllowedBottomDelta(view, dy, border);
            view.offsetTopAndBottom(-allowedDelta);
        }

//        final int step = activeCardTop / TOP_CARD_COUNT;
//        final int jDelta = (int) Math.floor(1f * delta * step / cardHeight);
        View prevView = null;
        int j = 0;
        final int activeCardPosition = getActiveCardPosition();
        for (int i = 0, cnt = topViews.size(); i < cnt; i++) {
            final View view = topViews.get(i);
            if (prevView == null || getDecoratedTop(prevView) >= activeCardBottom) {
                final int border = activeCardTop + getPosition(view) * cardHeight;
                final int allowedDelta = getAllowedBottomDelta(view, dy, border);
                view.offsetTopAndBottom(-allowedDelta);
            } else {
                final int jDelta;
                final int border;
                if (activeCardPosition - getPosition(view) == 2) {
                    jDelta = (int) Math.floor(1f * delta * cardsGap1to2 / cardHeight);
                    border = (int) (activeCardTop - cardsGap1to2 - cardsGap2to3);
                } else {
                    jDelta = (int) Math.floor(1f * delta * cardsGap1to2 / cardHeight);
                    border = (int) (activeCardTop - cardsGap2to3);
                }

//                final int border = activeCardTop - step * j;
                view.offsetTopAndBottom(-getAllowedBottomDelta(view, jDelta, border));
                j++;
            }
            prevView = view;
        }

        return delta;
    }

    private int scrollBottom(int dy) {  //for vertical mode
        final int childCount = getChildCount();

        if (childCount == 0) {
            return 0;
        }
        final View bottomestView = getChildAt(childCount - 1);
        final int deltaBorder = activeCardTop + getPosition(bottomestView) * cardHeight;
//        final int deltaBorder = (int) (activeCardTop + getPosition(bottomestView) * cardHeight+cardsGap);

        final int delta = getAllowedBottomDelta(bottomestView, dy, deltaBorder);

        final LinkedList<View> bottomViews = new LinkedList<>();
        final LinkedList<View> topViews = new LinkedList<>();

        for (int i = childCount - 1; i >= 0; i--) {
            final View view = getChildAt(i);
            final int viewTop = getDecoratedTop(view);

            if (viewTop >= activeCardBottom) {
                bottomViews.add(view);
            } else {
                topViews.add(view);
            }
        }
//        int maxDelta = 0;
        for (View view : bottomViews) {
            final int border = activeCardTop + getPosition(view) * cardHeight;
//            final int border = (int) (activeCardTop + getPosition(view) * cardHeight + cardsGap);

            final int allowedDelta = getAllowedBottomDelta(view, dy, border);
            view.offsetTopAndBottom(-allowedDelta);
//            if (Math.abs(allowedDelta) > Math.abs(maxDelta)) {
//                maxDelta = Math.abs(allowedDelta);
//            }
        }

//        final int step = activeCardTop / TOP_CARD_COUNT;
//        final int jDelta = (int) Math.floor(1f * delta * step / cardHeight);
        View prevView = null;
//        final int activeCardPostion = getActiveCardPosition();
        int j = 0;
        int jDelta = (int) Math.floor(1f * delta * cardsGap2to3 / cardHeight);
        int jBorder = activeCardTop;
        boolean needForceReset = false;//是否强制所有卡片调整至初始状态
        for (int i = 0, cnt = topViews.size(); i < cnt; i++) {

            final View view = topViews.get(i);
            if (prevView == null || getDecoratedTop(prevView) > activeCardBottom) {
//            if (prevView == null || getDecoratedTop(prevView) > activeCardBottom) {
                final int border = activeCardTop + getPosition(view) * cardHeight;
                final int allowedDelta = getAllowedBottomDelta(view, dy, border);
                view.offsetTopAndBottom(-allowedDelta);
                if (prevView == null && getDecoratedTop(view) == activeCardBottom) {
                    needForceReset = true;
                    break;
                }
            } else {
                view.offsetTopAndBottom(-getAllowedBottomDelta(view, jDelta, jBorder));
                if (j == 0) {
                    jBorder -= cardsGap2to3;
                    jDelta = (int) Math.floor(1f * delta * cardsGap1to2 / cardHeight);
                } else {
                    jBorder -= cardsGap1to2;
                }
                j++;
            }
            prevView = view;
        }
        if (needForceReset) {
            for (int i = 1, cnt = topViews.size(); i < cnt; i++) {
                final View view = topViews.get(i);
                final int resetDelta;
                if (i == 1) {
                    resetDelta = activeCardTop - getDecoratedTop(view);
                } else if (i == 2) {
                    resetDelta = (int) Math.floor((activeCardTop - cardsGap2to3) - getDecoratedTop(view));
                } else {
                    resetDelta = (int) Math.floor((activeCardTop - cardsGap2to3 - cardsGap1to2 * (i - 2)) - getDecoratedTop(view));
                }
                view.offsetTopAndBottom(resetDelta);
            }
        }
        return delta;
    }

    private int scrollRight(int dx) {
        final int childCount = getChildCount();

        if (childCount == 0) {
            return 0;
        }

        final View rightestView = getChildAt(childCount - 1);
        final int deltaBorder = activeCardLeft + getPosition(rightestView) * cardWidth;
        final int delta = getAllowedRightDelta(rightestView, dx, deltaBorder);

        final LinkedList<View> rightViews = new LinkedList<>();
        final LinkedList<View> leftViews = new LinkedList<>();

        for (int i = childCount - 1; i >= 0; i--) {
            final View view = getChildAt(i);
            final int viewLeft = getDecoratedLeft(view);

            if (viewLeft >= activeCardRight) {
                rightViews.add(view);
            } else {
                leftViews.add(view);
            }
        }

        for (View view : rightViews) {
            final int border = activeCardLeft + getPosition(view) * cardWidth;
            final int allowedDelta = getAllowedRightDelta(view, dx, border);
            view.offsetLeftAndRight(-allowedDelta);
        }

        final int step = activeCardLeft / LEFT_CARD_COUNT;
        final int jDelta = (int) Math.floor(1f * delta * step / cardWidth);

        View prevView = null;
        int j = 0;

        for (int i = 0, cnt = leftViews.size(); i < cnt; i++) {
            final View view = leftViews.get(i);
            if (prevView == null || getDecoratedLeft(prevView) >= activeCardRight) {
                final int border = activeCardLeft + getPosition(view) * cardWidth;
                final int allowedDelta = getAllowedRightDelta(view, dx, border);
                view.offsetLeftAndRight(-allowedDelta);
            } else {
                final int border = activeCardLeft - step * j;
                view.offsetLeftAndRight(-getAllowedRightDelta(view, jDelta, border));
                j++;
            }

            prevView = view;
        }

        return delta;
    }

    private int scrollLeft(int dx) {
        final int childCount = getChildCount();
        if (childCount == 0) {
            return 0;
        }

        final View lastView = getChildAt(childCount - 1);
        final boolean isLastItem = getPosition(lastView) == getItemCount() - 1;

        final int delta;
        if (isLastItem) {
            delta = Math.min(dx, getDecoratedRight(lastView) - activeCardRight);
        } else {
            delta = dx;
        }

        final int step = activeCardLeft / LEFT_CARD_COUNT;
        final int jDelta = (int) Math.ceil(1f * delta * step / cardWidth);

        for (int i = childCount - 1; i >= 0; i--) {
            final View view = getChildAt(i);
            final int viewLeft = getDecoratedLeft(view);

            if (viewLeft > activeCardLeft) {
                view.offsetLeftAndRight(getAllowedLeftDelta(view, delta, activeCardLeft));
            } else {
                int border = activeCardLeft - step;
                for (int j = i; j >= 0; j--) {
                    final View jView = getChildAt(j);
                    jView.offsetLeftAndRight(getAllowedLeftDelta(jView, jDelta, border));
                    border -= step;
                }

                break;
            }
        }

        return delta;
    }

    private int scrollTop(int dy) {
        final int childCount = getChildCount();
        if (childCount == 0) {
            return 0;
        }

        final View lastView = getChildAt(childCount - 1);
        final boolean isLastItem = getPosition(lastView) == getItemCount() - 1;

        final int delta;
        if (isLastItem) {
            delta = Math.min(dy, getDecoratedBottom(lastView) - activeCardBottom);
        } else {
            delta = dy;
        }
        boolean needForceReset = false;//是否强制把卡片复位
        int activeCardIndex = -1;
        for (int i = childCount - 1; i >= 0; i--) {

            final View view = getChildAt(i);
            final int viewTop = getDecoratedTop(view);

            if (viewTop > activeCardTop) {
                int realDelta = getAllowedTopDelta(view, delta, activeCardTop);
                view.offsetTopAndBottom(realDelta);
                if (getDecoratedTop(view) == activeCardTop) {
                    needForceReset = true;
                    activeCardIndex = i;
                    break;
                }
            } else {
                int border = (int) (activeCardTop - cardsGap2to3);
                int jDelta = (int) Math.ceil(1f * delta * cardsGap2to3 / cardHeight);
                for (int j = i; j >= 0; j--) {
                    final View jView = getChildAt(j);
                    jView.offsetTopAndBottom(getAllowedTopDelta(jView, jDelta, border));
                    border -= cardsGap1to2;
                    jDelta = (int) Math.ceil(1f * delta * cardsGap1to2 / cardHeight);
                }

                break;
            }
        }
        if (needForceReset) {
            int counter = 0;
            for (int i = activeCardIndex - 1; i >= 0; i--) {
                View view = getChildAt(i);
                final int resetDelta = (int) Math.floor(activeCardTop - cardsGap2to3 - cardsGap1to2 * counter - getDecoratedTop(view));
                view.offsetTopAndBottom(resetDelta);
                counter++;
            }
        }
        return delta;
    }

    private int scrollTop2(int dy) {
        final int childCount = getChildCount();
        if (childCount == 0) {
            return 0;
        }

        final View lastView = getChildAt(childCount - 1);
        final boolean isLastItem = getPosition(lastView) == getItemCount() - 1;

        final int delta;
        if (isLastItem) {
            delta = Math.min(dy, getDecoratedBottom(lastView) - activeCardBottom);
        } else {
            delta = dy;
        }
        final int activeCardPosition = getActiveCardPosition();
        final int step = activeCardTop / TOP_CARD_COUNT;
//        final int jDelta = (int) Math.ceil(1f * delta * step / cardHeight);
//        Log.i("layoutmanager", "jDelta:" + jDelta);
        for (int i = childCount - 1; i >= 0; i--) {
            final View view = getChildAt(i);
            final int viewTop = getDecoratedTop(view);

            if (viewTop > activeCardTop) {
                view.offsetTopAndBottom(getAllowedTopDelta(view, delta, activeCardTop));

            } else {
                int border = activeCardTop - step;
                for (int j = i; j >= 0; j--) {
                    final View jView = getChildAt(j);
                    final int jDelta;
                    if (activeCardPosition - getPosition(view) == 2) {
                        jDelta = (int) Math.ceil(1f * delta * cardsGap1to2 / cardHeight);
                    } else {
                        jDelta = (int) Math.ceil(1f * delta * cardsGap2to3 / cardHeight);
                    }
                    jView.offsetTopAndBottom(getAllowedTopDelta(jView, jDelta, border));
                    border -= step;
                }

                break;
            }
        }

        return delta;
    }

    private int getAllowedLeftDelta(@NonNull View view, int dx, int border) {
        final int viewLeft = getDecoratedLeft(view);
        if (viewLeft - dx > border) {
            return -dx;
        } else {
            return border - viewLeft;
        }
    }

    private int getAllowedTopDelta(@NonNull View view, int dy, int border) {
        final int viewTop = getDecoratedTop(view);
        if (viewTop - dy > border) {
            return -dy;
        } else {
            return border - viewTop;
        }
    }

    private int getAllowedRightDelta(@NonNull View view, int dx, int border) {
        final int viewLeft = getDecoratedLeft(view);
        if (viewLeft + Math.abs(dx) < border) {
            return dx;
        } else {
            return viewLeft - border;
        }
    }

    private int getAllowedBottomDelta(@NonNull View view, int dy, int border) {// for vertical mode
        final int viewTop = getDecoratedTop(view);
        if (viewTop + Math.abs(dy) < border) {
            return dy;
        } else {
            return viewTop - border;
        }
    }

    private void layoutByCoords() {
        final int count = Math.min(getChildCount(), cardsXCoords.size());
        for (int i = 0; i < count; i++) {
            final View view = getChildAt(i);
            final int viewLeft = cardsXCoords.get(getPosition(view));
            layoutDecorated(view, viewLeft, 0, viewLeft + cardWidth, getDecoratedBottom(view));
        }
        updateViewScale();
        cardsXCoords.clear();
    }

    private void layoutByCoordsVertical() {
        final int count = Math.min(getChildCount(), cardsYCoords.size());
        for (int i = 0; i < count; i++) {
            final View view = getChildAt(i);
            final int viewTop = cardsYCoords.get(getPosition(view));
//            layoutDecorated(view, 0, viewTop, getDecoratedRight(view), viewTop + cardHeight);
            int activeCardLeftOffset = 0;
            layoutDecorated(view, activeCardLeftOffset, viewTop, getDecoratedRight(view) + activeCardLeftOffset, viewTop + cardHeight);
        }
        updateViewScale();
        cardsYCoords.clear();
    }


    private void fill(int anchorPos, RecyclerView.Recycler recycler, RecyclerView.State state) {
        viewCache.clear();
//        Log.i("LAYOUTMANAGER", "fill---anchorPos:" + anchorPos);
        for (int i = 0, cnt = getChildCount(); i < cnt; i++) {

            View view = getChildAt(i);
            int pos = getPosition(view);
            viewCache.put(pos, view);
//            Log.i("LAYOUTMANAGER", "position of " + i + ">>>" + pos);

        }

        for (int i = 0, cnt = viewCache.size(); i < cnt; i++) {
            detachView(viewCache.valueAt(i));
        }
        if (!state.isPreLayout()) {
            if (mScrollMode == MODE_VERTICL) {
                fillTop(anchorPos, recycler);
                fillBottom(anchorPos, recycler);
//                fillBottom2(anchorPos, recycler);
            } else {
                fillLeft(anchorPos, recycler);
                fillRight(anchorPos, recycler);
            }
        }

        for (int i = 0, cnt = viewCache.size(); i < cnt; i++) {
            recycler.recycleView(viewCache.valueAt(i));
        }

        updateViewScale();
    }

    private void fillLeft(int anchorPos, RecyclerView.Recycler recycler) {
        if (anchorPos == RecyclerView.NO_POSITION) {
            return;
        }

        final int layoutStep = activeCardLeft / LEFT_CARD_COUNT;
        int pos = Math.max(0, anchorPos - LEFT_CARD_COUNT - 1);
        int viewLeft = Math.max(-1, LEFT_CARD_COUNT - (anchorPos - pos)) * layoutStep;

        while (pos < anchorPos) {//zc <
            View view = viewCache.get(pos);
            if (view != null) {
                attachView(view);
                viewCache.remove(pos);
            } else {
                view = recycler.getViewForPosition(pos);
                addView(view);
                measureChildWithMargins(view, 0, 0);
                final int viewHeight = getDecoratedMeasuredHeight(view);
                layoutDecorated(view, viewLeft, 0, viewLeft + cardWidth, viewHeight);
            }

            viewLeft += layoutStep;
            pos++;
        }

    }

    private void fillTop(int anchorPos, RecyclerView.Recycler recycler) {
        if (anchorPos == RecyclerView.NO_POSITION) {
            return;
        }
        int pos = Math.max(0, anchorPos - TOP_CARD_COUNT);//zc 避免有时候上面多一个view
        while (pos < anchorPos) {
            View view = viewCache.get(pos);
            if (view != null) {
                attachView(view);
                viewCache.remove(pos);
            } else {
                view = recycler.getViewForPosition(pos);
                addView(view);
                measureChildWithMargins(view, 0, 0);
                final int viewWidth = getDecoratedMeasuredWidth(view);
                final int viewTop;
                if (anchorPos - pos == 2) {
                    viewTop = (int) (activeCardTop - cardsGap2to3 - cardsGap1to2);
                } else if (anchorPos - pos == 1) {
                    viewTop = (int) (activeCardTop - cardsGap2to3);
                } else {
                    viewTop = activeCardTop;
                }
                layoutDecorated(view, activeCardLeftOffset, viewTop, viewWidth + activeCardLeftOffset, viewTop + cardHeight);

            }
            pos++;
        }

    }

    private void fillRight(int anchorPos, RecyclerView.Recycler recycler) {
        if (anchorPos == RecyclerView.NO_POSITION) {
            return;
        }

        final int width = getWidth();
        final int itemCount = getItemCount();

        int pos = anchorPos;
        int viewLeft = activeCardLeft;
        boolean fillRight = true;

        while (fillRight && pos < itemCount) {
            View view = viewCache.get(pos);
            if (view != null) {
                attachView(view);
                viewCache.remove(pos);
            } else {
                view = recycler.getViewForPosition(pos);
                addView(view);
                measureChildWithMargins(view, 0, 0);
                final int viewHeight = getDecoratedMeasuredHeight(view);
                layoutDecorated(view, viewLeft, 0, viewLeft + cardWidth, viewHeight);
            }

            viewLeft = getDecoratedRight(view);
            fillRight = viewLeft < width + cardWidth;
            pos++;
        }
    }

    private void fillBottom(int anchorPos, RecyclerView.Recycler recycler) {
//        Log.i("layoutManager", "---fillBottom---");
        if (anchorPos == RecyclerView.NO_POSITION) {
            return;
        }

        final int height = getHeight();
        final int itemCount = getItemCount();

        int pos = anchorPos;
        int viewTop = activeCardTop;
        boolean fillBottom = true;

        while (fillBottom && pos < itemCount) {
            View view = viewCache.get(pos);
            if (view != null) {
                attachView(view);
                viewCache.remove(pos);
            } else {
                view = recycler.getViewForPosition(pos);
                addView(view);
                measureChildWithMargins(view, 0, 0);
                final int viewWidth = getDecoratedMeasuredWidth(view);
                layoutDecorated(view, activeCardLeftOffset, viewTop, viewWidth + activeCardLeftOffset, viewTop + cardHeight);

            }

            viewTop = getDecoratedBottom(view);
            fillBottom = viewTop < height + cardHeight;
            pos++;
        }
    }

    private void fillBottom2(int anchorPos, RecyclerView.Recycler recycler) {
//        Log.i("layoutManager", "---fillBottom2---");
        if (anchorPos == RecyclerView.NO_POSITION) {
            return;
        }

        final int itemCount = getItemCount();

        int pos = anchorPos;
        int viewTop = activeCardTop;
        int fillViewCount = 2;//activeCard下面只绘制一个
        View preView = null;
        while (pos < itemCount && fillViewCount > 0) {
            View view = viewCache.get(pos);
            if (view != null) {
                attachView(view);
                viewCache.remove(pos);
            } else {
                if (pos == anchorPos + 1) {
                    viewTop = getDecoratedBottom(preView) + (getHeight() - activeCardBottom);
                }
                if (viewTop < getHeight()) {
                    view = recycler.getViewForPosition(pos);
                    addView(view);
                    measureChildWithMargins(view, 0, 0);
                    final int viewWidth = getDecoratedMeasuredWidth(view);
                    layoutDecorated(view, activeCardLeftOffset, viewTop, viewWidth + activeCardLeftOffset, viewTop + cardHeight);
                }

            }
            preView = view;
            pos++;
            fillViewCount--;
        }
    }


    private void updateViewScale() {
        viewUpdater.updateView();
    }

    private static class SavedState implements Parcelable {

        int anchorPos;

        SavedState() {

        }

        SavedState(Parcel in) {
            anchorPos = in.readInt();
        }

        public SavedState(SavedState other) {
            anchorPos = other.anchorPos;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(anchorPos);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

}