package com.aylanetworks.agilelink.device.devkit;

import android.view.View;

import com.aylanetworks.aaml.AylaDevice;

/*
 * ZigbeeSwitchedDevice.java
 * AgileLink Application Framework
 *
 * Created by David Junod on 5/5/15
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class ZigbeeSwitchedDevice extends SwitchedDevice implements View.OnClickListener {
    private final static String LOG_TAG = "ZigbeeSwitchedDevice";

    private final static String PROPERTY_NAME = "1_in_0x0006_0x0000";

    public ZigbeeSwitchedDevice(AylaDevice device) {
        super(device);
    }

    @Override
    public String getObservablePropertyName() { return PROPERTY_NAME;  }

    @Override
    public boolean isDeviceNode() {
        return true;
    }

}
