package com.ramotion.cardslider.examples.simple;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.WindowManager;

import com.ramotion.cardslider.CardSliderLayoutManager;
import com.ramotion.cardslider.VerticalCardSanpHelper;
import com.ramotion.cardslider.examples.simple.cards.MijiaCardAdapter;
import com.ramotion.cardslider.examples.simple.cards.SliderAdapter;

/**
 * Created by zengcheng on 2017/8/2.
 */

public class ChooseDeviceCardActivity extends Activity {
    private final static String[] DEVICES = {"米家HIFI跑鞋", "小米智能插座", "墙壁双键开关", "智米智能马桶盖", "米家小白摄像机", "飞米相机","AI音箱","小方摄像机"};
    private final int[] pics = {R.drawable.p1, R.drawable.p2, R.drawable.p3, R.drawable.p4, R.drawable.p5};

    private final MijiaCardAdapter sliderAdapter = new MijiaCardAdapter(DEVICES, DEVICES.length, null);
    private CardSliderLayoutManager layoutManger;
    private RecyclerView recyclerView;
    private int currentPosition;
//    private final SliderAdapter sliderAdapter = new SliderAdapter(pics, 20, null);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.card_chooser);
        initRecyclerView();
    }

    private void initRecyclerView() {
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setAdapter(sliderAdapter);
        recyclerView.setHasFixedSize(true);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    onActiveCardChange();
                }
            }
        });
        recyclerView.setItemAnimator(null);
        int itemCount = sliderAdapter.getItemCount();
        if (itemCount >= 3) {
            recyclerView.scrollToPosition(2);
        } else if (itemCount == 2) {
            recyclerView.scrollToPosition(1);
        }
        layoutManger = (CardSliderLayoutManager) recyclerView.getLayoutManager();

//        new CardSnapHelper().attachToRecyclerView(recyclerView);
        new VerticalCardSanpHelper().attachToRecyclerView(recyclerView);
    }

    private void onActiveCardChange() {
        final int pos = layoutManger.getActiveCardPosition();
        if (pos == RecyclerView.NO_POSITION || pos == currentPosition) {
            return;
        }

        onActiveCardChange(pos);
    }

    private void onActiveCardChange(int pos) {
        currentPosition = pos;

    }
}
