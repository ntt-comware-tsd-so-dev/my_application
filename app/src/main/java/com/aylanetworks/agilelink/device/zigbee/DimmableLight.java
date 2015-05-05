package com.aylanetworks.agilelink.device.zigbee;

import android.content.Context;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaProperty;
import com.aylanetworks.agilelink.AgileLinkApplication;
import com.aylanetworks.agilelink.R;

import java.util.ArrayList;

/*
 * SmartBulb.java
 * AgileLink Application Framework
 *
 * Created by David Junod on 5/5/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class DimmableLight extends SmartBulb {
    public DimmableLight(AylaDevice aylaDevice) {
        super(aylaDevice);
    }

    private final static String PROPERTY_ZB_DIMMABLE_LIGHT = "1_in_0x0008_0x04";
    private final static String PROPERTY_ZB_DIMMABLE_LIGHT_LEVEL = "1_in_0x0008_0x0000";

    @Override
    public String deviceTypeName() {
        return "Dimmable Light";
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

    @Override
    public String getDeviceState() {
        StringBuilder sb = new StringBuilder(64);
        sb.append(super.getDeviceState());
        sb.append(":");

        Context c = AgileLinkApplication.getAppContext();
        AylaProperty setLevelProp = getProperty(PROPERTY_ZB_DIMMABLE_LIGHT);
        if ( setLevelProp == null ) {
            sb.append(c.getString(R.string.device_state_unknown));
        } else {
            sb.append(Integer.parseInt(setLevelProp.value));
        }
        sb.append(":");

        AylaProperty curLevelProp = getProperty(PROPERTY_ZB_DIMMABLE_LIGHT_LEVEL);
        if ( curLevelProp == null ) {
            sb.append(c.getString(R.string.device_state_unknown));
        } else {
            sb.append(Integer.parseInt(curLevelProp.value));
        }

        return sb.toString();
    }

}
