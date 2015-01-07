package com.aylanetworks.agilelink.device;

import android.content.Context;
import android.util.Log;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaProperty;
import com.aylanetworks.agilelink.AgileLinkApplication;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;

import java.util.ArrayList;

/**
 * Created by Brian King on 12/19/14.
 */
public class DoorSensor extends Device {
    private static String LOG_TAG = "DoorSensor";

    private static String PROPERTY_DOOR_OPEN = "1_out_0x0006_0x0000";

    public DoorSensor(AylaDevice device) {
        super(device);
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
        if ( openProp != null && openProp.datapoint != null ) {
            return openProp.datapoint.nValue().intValue() == 0;
        }
        // Unknown
        Log.i(LOG_TAG, "No datapoint for door open!");
        return false;
    }

    @Override
    public String toString() {
        Context c = AgileLinkApplication.getAppContext();
        String open = isOpen() ? c.getString(R.string.open) : c.getString(R.string.closed);
        return super.toString() + " " + open;
    }
}
