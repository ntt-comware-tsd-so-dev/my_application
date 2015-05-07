package com.aylanetworks.agilelink.device.devkit;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;

/*
 * ZigbeeLightDevice.java
 * AgileLink Application Framework
 *
 * Created by David Junod on 5/5/15
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class ZigbeeLightDevice extends SwitchedDevice implements View.OnClickListener {

    private final static String LOG_TAG = "ZigbeeLightDevice";

    private final static String PROPERTY_NAME = "1_in_0x0006_0x0000";

    public ZigbeeLightDevice(AylaDevice device) {
        super(device);
    }

    @Override
    public String getObservablePropertyName() { return PROPERTY_NAME;  }

    @Override
    public boolean isDeviceNode() {
        return true;
    }

    @Override
    public String friendlyNameForPropertyName(String propertyName) {
        if (propertyName.equals(PROPERTY_NAME)) {
            return MainActivity.getInstance().getString(R.string.property_light_friendly_name);
        }
        return super.friendlyNameForPropertyName(propertyName);
    }

    @Override
    public String deviceTypeName() {
        return "Smart Bulb";
    }

    @Override
    public Drawable getDeviceDrawable(Context c) {
        return c.getResources().getDrawable(R.drawable.smart_bulb);
    }

    @Override
    public Drawable getSwitchedDrawable(Resources res) {
        return res.getDrawable(isOn() ? R.drawable.ic_light_on : R.drawable.ic_light_off);
    }

    @Override
    public Drawable getSwitchedPendingDrawable(Resources res) {
        return res.getDrawable(R.drawable.ic_light_pending);
    }
}
