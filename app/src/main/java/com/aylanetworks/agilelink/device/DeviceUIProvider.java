package com.aylanetworks.agilelink.device;
/*
 * AMAP_Android
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;

public interface DeviceUIProvider {
    Drawable getDeviceDrawable(Context context);
    int getGridViewSpan();
    String getName();
    String deviceTypeName();
    int getItemViewType();
    void bindViewHolder(RecyclerView.ViewHolder holder);
    Fragment getDetailsFragment();
}
