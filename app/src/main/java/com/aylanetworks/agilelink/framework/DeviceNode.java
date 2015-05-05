package com.aylanetworks.agilelink.framework;

import android.util.Log;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaDeviceNode;
import com.google.gson.JsonElement;

import java.util.ArrayList;

/*
 * DeviceNode.java
 * AgileLink Application Framework
 *
 * Created by David Junod on 5/5/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class DeviceNode extends Device {
    private final String LOG_TAG = "DeviceNode";

    public DeviceNode(AylaDevice aylaDevice) { super(aylaDevice); }

    public AylaDeviceNode getDeviceNode() {
        return (AylaDeviceNode)getDevice();
    }

    @Override
    protected ArrayList<String> getPropertyNames() {
        // Get the superclass' property names (probably none)
        ArrayList<String> propertyNames = super.getPropertyNames();

        // Add our own
        propertyNames.add("attr_set_cmd");
        propertyNames.add("attr_set_result");
        propertyNames.add("attr_read_data");

        return propertyNames;
    }

    @Override
    public boolean isDeviceNode() {
        return true;
    }

    public void configureWithJsonElement(JsonElement json) {
        Log.e(LOG_TAG, "Configure with: " + json.toString());
    }
}
