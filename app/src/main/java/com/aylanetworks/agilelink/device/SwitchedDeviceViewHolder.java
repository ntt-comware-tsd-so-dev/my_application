package com.aylanetworks.agilelink.device;

import android.view.View;
import android.widget.ImageButton;

import com.aylanetworks.agilelink.R;

/*
 * SwitchedDeviceViewHolder.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 2/5/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class SwitchedDeviceViewHolder extends GenericDeviceViewHolder {
    public ImageButton _switchButton;

    public SwitchedDeviceViewHolder(View view) {
        super(view);
        _switchButton = (ImageButton)view.findViewById(R.id.power_button);
    }
}
