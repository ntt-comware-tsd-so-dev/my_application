package com.aylanetworks.agilelink.framework;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaDeviceGateway;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.agilelink.R;
import com.google.gson.JsonElement;

import java.util.ArrayList;

/*
 * Gateway.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 12/22/14.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class Gateway extends Device {
    private final String LOG_TAG = "Gateway";

    public Gateway(AylaDevice aylaDevice) {
        super(aylaDevice);
    }

    public AylaDeviceGateway getGatewayDevice() {
        return (AylaDeviceGateway)getDevice();
    }

    @Override
    protected ArrayList<String> getPropertyNames() {
        // Get the superclass' property names (probably none)
        ArrayList<String> propertyNames = super.getPropertyNames();

        // Add our own
        propertyNames.add("attr_set_cmd");
        propertyNames.add("attr_set_result");
        propertyNames.add("attr_read_data");
        propertyNames.add("join_enable");
        propertyNames.add("join_status");

        return propertyNames;
    }

    @Override
    public String registrationType() {
        return AylaNetworks.AML_REGISTRATION_TYPE_BUTTON_PUSH;
    }

    @Override
    public boolean isGateway() {
        return true;
    }

    @Override
    public String deviceTypeName() {
        return "Zigbee Gateway";
    }

    @Override
    public Drawable getDeviceDrawable(Context c) {
        return c.getResources().getDrawable(R.drawable.ic_zigbee);
    }

    public void configureWithJsonElement(JsonElement json) {
        Log.e(LOG_TAG, "Configure with: " + json.toString());
    }
}
