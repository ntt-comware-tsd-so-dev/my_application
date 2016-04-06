package com.aylanetworks.agilelink.device;

import android.view.View;
import android.widget.TextView;

import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.GenericDeviceViewHolder;

/**
 * AMAP4.x
 * <p/>
 * Copyright 2016 Ayla Networks Inc, all rights reserved
 */
public class GenericStatusDeviceHolder extends GenericDeviceViewHolder{

    public TextView statusTextView;

    public GenericStatusDeviceHolder(View itemView) {
        super(itemView);
        statusTextView = (TextView)itemView.findViewById(R.id.textview_device_status);
    }

} // end of GenericStatusDeviceHolder class



