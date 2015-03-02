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
 * DoorSensor.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 12/19/14.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class DoorSensor extends Device {
    private static String LOG_TAG = "DoorSensor";

    private static String PROPERTY_DOOR_OPEN = "1_out_0x0006_0x0000";
    private static int DOOR_OPEN = 1;

    public DoorSensor(AylaDevice device) {
        super(device);
    }

    @Override
    public String deviceTypeName() {
        return "Door Sensor";
    }

    @Override
    public Drawable getDeviceDrawable(Context c) {
        return c.getResources().getDrawable(R.drawable.door_sensor);
    }

    @Override
    public String registrationType() {
        return AylaNetworks.AML_REGISTRATION_TYPE_BUTTON_PUSH;
    }

    @Override
    protected ArrayList<String> getPropertyNames() {
        // Get the superclass' property names (probably none)
        ArrayList<String> propertyNames = super.getPropertyNames();

        // Add our own
        propertyNames.add(PROPERTY_DOOR_OPEN);

        return propertyNames;
    }

    public boolean isOpen() {
        AylaProperty openProp = getProperty(PROPERTY_DOOR_OPEN);
        if ( openProp != null && openProp.value != null ) {
            return (Integer.parseInt(openProp.value) == DOOR_OPEN);
        }
        // Unknown
        Log.i(LOG_TAG, "No open property value for door open!");
        return false;
    }

    @Override
    public void updateStatus(DeviceStatusListener listener) {
        super.updateStatus(listener);
    }

     @Override
     public String getDeviceState() {
         Context c = AgileLinkApplication.getAppContext();
         AylaProperty openProp = getProperty(PROPERTY_DOOR_OPEN);
         if ( openProp == null ) {
             return c.getString(R.string.device_state_unknown);
         }

         String open = (Integer.parseInt(openProp.value) == DOOR_OPEN) ? c.getString(R.string.open) :
                 c.getString(R.string.closed);
         return open;
     }
}
