package com.aylanetworks.agilelink.device.devkit;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.aylanetworks.agilelink.R;

/*
 * LightDeviceViewHolder.java
 * AgileLink Application Framework
 *
 * Created by David Junod on 5/5/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class LightDeviceViewHolder extends RecyclerView.ViewHolder {
    public ImageButton _switchButton;
    public TextView _deviceNameTextView;
    public ProgressBar _spinner;

    public LightDeviceViewHolder(View view) {
        super(view);
        _switchButton = (ImageButton)view.findViewById(R.id.power_button);
        _deviceNameTextView = (TextView)view.findViewById(R.id.device_name);
        _spinner = (ProgressBar)view.findViewById(R.id.spinner);
    }
}
