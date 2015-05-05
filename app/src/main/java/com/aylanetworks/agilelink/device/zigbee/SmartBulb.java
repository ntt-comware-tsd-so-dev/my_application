package com.aylanetworks.agilelink.device.zigbee;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaProperty;
import com.aylanetworks.agilelink.AgileLinkApplication;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;

import java.util.ArrayList;

/*
 * SmartBulb.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 12/19/14.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class SmartBulb extends Device {
    private static String LOG_TAG = "SmartBulb";

    public SmartBulb(AylaDevice aylaDevice) {
        super(aylaDevice);
    }

    private final static String PROPERTY_ZB_LIGHT_SWITCH = "1_in_0x0006_0x0000";
    private static int LIGHT_ON = 1;

    @Override
    public String deviceTypeName() {
        return "Smart Bulb";
    }

    @Override
    public String registrationType() {
        return AylaNetworks.AML_REGISTRATION_TYPE_BUTTON_PUSH;
    }

    @Override
    public Drawable getDeviceDrawable(Context c) {
        return c.getResources().getDrawable(R.drawable.smart_bulb);
    }

    @Override
    protected ArrayList<String> getPropertyNames() {
        // Get the superclass' property names (probably none)
        ArrayList<String> propertyNames = super.getPropertyNames();

        // Add our own
        propertyNames.add(PROPERTY_ZB_LIGHT_SWITCH);

        return propertyNames;
    }

    public boolean isOn() {
        AylaProperty openProp = getProperty(PROPERTY_ZB_LIGHT_SWITCH);
        if ( openProp != null && openProp.value != null ) {
            return (Integer.parseInt(openProp.value) == LIGHT_ON);
        }
        // Unknown
        Log.i(LOG_TAG, "No open property value for light on!");
        return false;
    }

    @Override
    public String getDeviceState() {
        Context c = AgileLinkApplication.getAppContext();
        AylaProperty openProp = getProperty(PROPERTY_ZB_LIGHT_SWITCH);
        if ( openProp == null ) {
            return c.getString(R.string.device_state_unknown);
        }

        String state = (Integer.parseInt(openProp.value) == LIGHT_ON) ? c.getString(R.string.on) :
                c.getString(R.string.off);
        return state;
    }

}
