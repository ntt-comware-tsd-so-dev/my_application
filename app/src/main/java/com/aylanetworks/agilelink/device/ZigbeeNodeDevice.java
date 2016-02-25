package com.aylanetworks.agilelink.device;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;

/*
 * ZigbeeNodeDevice.java
 * AgileLink Application Framework
 *
 * Created by David Junod on 5/14/15
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class ZigbeeNodeDevice extends GenericDevice {

    private final static String LOG_TAG = "ZigbeeNodeDevice";

    public ZigbeeNodeDevice(AylaDevice device) {
        super(device);
    }

    @Override
    public String friendlyNameForPropertyName(String propertyName) {
        if (propertyName.equals(ZigbeeSwitchedDevice.PROPERTY_ZB_INPUT)) {
            return MainActivity.getInstance().getString(R.string.property_zigbee_input_friendly_name);
        }
        return super.friendlyNameForPropertyName(propertyName);
    }

    @Override
    public boolean isDeviceNode() {
        return true;
    }

    @Override
    public String registrationType() {
        return AylaNetworks.AML_REGISTRATION_TYPE_NODE;
    }

    @Override
    public String deviceTypeName() {
        return "Zigbee Node";
    }

    @Override
    public Drawable getDeviceDrawable(Context c) {
        return c.getResources().getDrawable(R.drawable.smart_bulb);
    }
}
