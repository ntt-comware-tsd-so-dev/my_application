package com.aylanetworks.agilelink.device;

import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.aylanetworks.agilelink.R;

/*
 * AylaEVBDeviceViewHolder.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 1/23/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class AylaEVBDeviceViewHolder extends GenericDeviceViewHolder {
    public ImageButton _greenButton;
    public ImageButton _blueButton;
    public ImageView _buttonStateImageView;

    public AylaEVBDeviceViewHolder(View v) {
        super(v);

        _greenButton = (ImageButton)v.findViewById(R.id.green_button);
        _blueButton = (ImageButton)v.findViewById(R.id.blue_button);
        _buttonStateImageView = (ImageView)v.findViewById(R.id.blue_button_state_image);
    }
}
