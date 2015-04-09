package com.aylanetworks.agilelink.device.devkit;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.aylanetworks.agilelink.R;

/*
 * AylaEVBDeviceViewHolder.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 1/23/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class AylaEVBDeviceViewHolder extends RecyclerView.ViewHolder {
    public TextView _deviceNameTextView;
    public ImageButton _greenButton;
    public ImageButton _blueButton;
    public ImageView _buttonStateImageView;
    public ProgressBar _spinner;

    public AylaEVBDeviceViewHolder(View v) {
        super(v);

        _deviceNameTextView = (TextView)v.findViewById(R.id.device_name);
        _greenButton = (ImageButton)v.findViewById(R.id.green_button);
        _blueButton = (ImageButton)v.findViewById(R.id.blue_button);
        _buttonStateImageView = (ImageView)v.findViewById(R.id.blue_button_state_image);
        _spinner = (ProgressBar)v.findViewById(R.id.spinner);
    }
}
