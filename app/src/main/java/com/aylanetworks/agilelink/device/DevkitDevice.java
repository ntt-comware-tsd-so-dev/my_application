package com.aylanetworks.agilelink.device;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.agilelink.framework.Device;

import java.util.ArrayList;

/**
 * Created by Brian King on 1/14/15.
 */
public class DevkitDevice extends Device {
    private static final String LOG_TAG = "DevkitDevice";

    private static final String PROPERTY_BLUE_LED = "Blue_LED";
    private static final String PROPERTY_GREEN_LED = "Green_LED";
    private static final String PROPERTY_BLUE_BUTTON = "Blue_button";

    public DevkitDevice(AylaDevice device) {
        super(device);
    }

    @Override
    protected ArrayList<String> getPropertyNames() {
        ArrayList<String> propertyNames = super.getPropertyNames();

        // Add property names we care about
        propertyNames.add(PROPERTY_BLUE_BUTTON);
        propertyNames.add(PROPERTY_BLUE_LED);
        propertyNames.add(PROPERTY_GREEN_LED);

        return propertyNames;
    }

    @Override
    public String toString() {
        AylaDevice d = getDevice();
        if ( d != null && d.dsn != null ) {
            return "Ayla DevKit " + getDevice().dsn;
        } else {
            return "Ayla DevKit";
        }
    }
}
