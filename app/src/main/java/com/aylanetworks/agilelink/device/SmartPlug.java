package com.aylanetworks.agilelink.device;

import android.content.Context;
import android.util.Log;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaProperty;
import com.aylanetworks.agilelink.AgileLinkApplication;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;

import java.util.ArrayList;

/**
 * Created by Brian King on 12/19/14.
 */
public class SmartPlug extends Device {
    private final static String LOG_TAG = "SmartPlug";
    private final static String PROPERTY_SWITCH_ON = "1_in_0x0006_0x0000";
    private final static int SWITCH_ON = 1;

    public SmartPlug(AylaDevice aylaDevice) {
        super(aylaDevice);
    }

    public boolean isOn() {
        AylaProperty onProp = getProperty(PROPERTY_SWITCH_ON);
        if ( onProp != null && onProp.value != null ) {
            return (Integer.parseInt(onProp.value) == SWITCH_ON);
        }
        // Unknown
        Log.i(LOG_TAG, "No property value for SWITCH_ON!");
        return false;

    }

    @Override
    protected ArrayList<String> getPropertyNames() {
        // Get the superclass' property names (probably none)
        ArrayList<String> propertyNames = super.getPropertyNames();

        // Add our own
        propertyNames.add(PROPERTY_SWITCH_ON);

        return propertyNames;
    }

    @Override
    public String getDeviceState() {
        Context c = AgileLinkApplication.getAppContext();
        if ( getProperty(PROPERTY_SWITCH_ON) == null ) {
            return c.getString(R.string.device_state_unknown);
        }

        String on = isOn() ? c.getString(R.string.on) : c.getString(R.string.off);
        return on;
    }
}
