package com.ramotion.cardslider.examples.simple.cards;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ramotion.cardslider.examples.simple.R;

/**
 * Created by zengcheng on 2017/8/2.
 */

public class MijiaCardAdapter extends RecyclerView.Adapter<MijiaDeviceCard>  {
    private final int count;
    private final String[] content;
    private final View.OnClickListener listener;

    public MijiaCardAdapter(String[] content, int count, View.OnClickListener listener) {
        this.content = content;
        this.count = count;
        this.listener = listener;
    }

    @Override
    public MijiaDeviceCard onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.device_card_base, parent, false);

//        if (listener != null) {
//            view.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    listener.onClick(view);
//                }
//            });
//        }

        return new MijiaDeviceCard(view);
    }

    @Override
    public void onBindViewHolder(MijiaDeviceCard holder, int position) {
        holder.setContent(content[position % content.length]);
    }

    @Override
    public void onViewRecycled(MijiaDeviceCard holder) {
        holder.clearContent();
    }

    @Override
    public int getItemCount() {
        return count;
    }
}
