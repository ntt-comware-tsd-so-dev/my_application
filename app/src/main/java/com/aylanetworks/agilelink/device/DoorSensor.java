package com.aylanetworks.agilelink.device;

import com.aylanetworks.agilelink.framework.ALDevice;

import java.util.ArrayList;

/**
 * Created by Brian King on 12/19/14.
 */
public class DoorSensor extends ALDevice {
    @Override
    protected ArrayList<String> getPropertyNames() {
        // Get the superclass' property names (probably none)
        ArrayList<String> propertyNames = super.getPropertyNames();

        // Add our own
        propertyNames.add("1_in_0x0006_0x0000");
        propertyNames.add("1_in_0x0702_0x0000");
        propertyNames.add("1_in_0x0702_0x0200");
        propertyNames.add("1_in_0x0702_0x0300");
        propertyNames.add("1_in_0x0702_0x0303");
        propertyNames.add("1_in_0x0702_0x0306");
        propertyNames.add("1_profile_id");
        propertyNames.add("242_profile_id");

        return propertyNames;
    }
}
