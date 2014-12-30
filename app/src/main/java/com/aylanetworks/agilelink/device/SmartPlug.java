package com.aylanetworks.agilelink.device;

import com.aylanetworks.agilelink.framework.ALDevice;

import java.util.ArrayList;

/**
 * Created by Brian King on 12/19/14.
 */
public class SmartPlug extends ALDevice {
    @Override
    protected ArrayList<String> getPropertyNames() {
        // Get the superclass' property names (probably none)
        ArrayList<String> propertyNames = super.getPropertyNames();

        // Add our own
        propertyNames.add("1_in_0x0001_0x0010");
        propertyNames.add("1_in_0x0001_0x0021");
        propertyNames.add("1_out_0x0006_0x0000");
        propertyNames.add("1_profile_id");

        return propertyNames;
    }
}
