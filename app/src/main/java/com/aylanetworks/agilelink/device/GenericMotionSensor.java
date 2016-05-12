package com.aylanetworks.agilelink.device;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;

import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;

/**
 * AMAP4.x
 * <p/>
 * Copyright 2016 Ayla Networks Inc, all rights reserved
 */
public class GenericMotionSensor extends GenericTriggerDevice {
    private static final String LOG_TAG = GenericMotionSensor.class.getSimpleName();

    public GenericMotionSensor(AylaDevice device) {
        super(device);
    }

    @Override
    public String deviceTypeName() {
        return LOG_TAG;
    }

    @Override
    public String getTriggerOnName() {
        return MainActivity.getInstance().getString(R.string.motion_trigger_on_name);
    }

    @Override
    public String getTriggerOffName() {
        return MainActivity.getInstance().getString(R.string.motion_trigger_off_name);
    }

    @Override
    public Drawable getDeviceDrawable(Context c) {
        return c.getResources().getDrawable(R.drawable.ic_motionsensor_red);
    }


    @Override
    public int getItemViewType() {
        return AgileLinkDeviceCreator.ITEM_VIEW_TYPE_WITH_STATUS;
    }


    @Override
    public void bindViewHolder(RecyclerView.ViewHolder holder) {
        super.bindViewHolder(holder);
        Resources res = MainActivity.getInstance().getResources();
        String statusText = isMoving()? getTriggerOnName(): getTriggerOffName();
        if (holder instanceof GenericStatusDeviceHolder) {
            GenericStatusDeviceHolder h = (GenericStatusDeviceHolder) holder;
            if ( h.statusTextView != null ) {
                h.statusTextView.setText(statusText);
            }
        }
    }


    //TODO: Implement this when generic motion sensor is available.
    public boolean isMoving() {
        return true;
    }

}// end of GenericMotionSensor class



