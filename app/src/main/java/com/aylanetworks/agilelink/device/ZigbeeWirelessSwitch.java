package com.aylanetworks.agilelink.device;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;

/*
 * ZigbeeWirelessSwitch.java
 * AgileLink Application Framework
 *
 * Created by David Junod on 5/21/15
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class ZigbeeWirelessSwitch extends Device {

    private final static String LOG_TAG = "ZigbeeWirelessSwitch";

    public final static String PROPERTY_ZB_OUTPUT = "1_out_0x0006_0x0000";
    public final static String PROPERTY_ZB_REMOTE_SWITCH = PROPERTY_ZB_OUTPUT;

    public ZigbeeWirelessSwitch(AylaDevice device) {
        super(device);
    }

    @Override
    public String getObservablePropertyName() { return PROPERTY_ZB_REMOTE_SWITCH;  }

    @Override
    public String deviceTypeName() {
        return "Wireless Switch";
    }

    @Override
    public Drawable getDeviceDrawable(Context c) {
        return c.getResources().getDrawable(R.drawable.ic_remote_red);
    }

    @Override
    public boolean isDeviceNode() {
        return true;
    }

}
