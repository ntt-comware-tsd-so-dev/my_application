package com.aylanetworks.agilelink.device;

import android.view.View;
import android.widget.ImageButton;

import com.aylanetworks.agilelink.R;

/**
 * AMAP4.x
 * <p/>
 * Copyright 2016 Ayla Networks Inc, all rights reserved
 */
public class GenericNodeDeviceViewHolder extends GenericDeviceViewHolder {
    public ImageButton _switchButton;

    public GenericNodeDeviceViewHolder(View view) {
        super(view);
        _switchButton = (ImageButton)view.findViewById(R.id.power_button);
    }
}// end of GenericNodeDeviceViewHolder class



