package com.aylanetworks.agilelink.device.devkit;

import android.view.View;

import com.aylanetworks.aaml.AylaDevice;

import java.util.ArrayList;

/*
 * ZigbeeDimmableLightDevice.java
 * AgileLink Application Framework
 *
 * Created by David Junod on 5/7/15
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class ZigbeeDimmableLightDevice extends ZigbeeLightDevice implements View.OnClickListener {

    private final static String LOG_TAG = "ZigbeeDimmableLightDevice";

    public final static String PROPERTY_ZB_DIMMABLE_LIGHT = "1_in_0x0008_0x04";
    public final static String PROPERTY_ZB_DIMMABLE_LIGHT_LEVEL = "1_in_0x0008_0x0000";

    public ZigbeeDimmableLightDevice(AylaDevice device) {
        super(device);
    }

    @Override
    public String deviceTypeName() {
        return "Dimmable Bulb";
    }

    @Override
    protected ArrayList<String> getPropertyNames() {
        // Get the superclass' property names (probably none)
        ArrayList<String> propertyNames = super.getPropertyNames();

        // Add our own
        propertyNames.add(PROPERTY_ZB_DIMMABLE_LIGHT);
        propertyNames.add(PROPERTY_ZB_DIMMABLE_LIGHT_LEVEL);

        return propertyNames;
    }
}
