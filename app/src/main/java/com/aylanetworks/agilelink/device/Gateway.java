package com.aylanetworks.agilelink.device;

import com.aylanetworks.agilelink.framework.ALDevice;

import java.util.ArrayList;

/**
 * Created by Brian King on 12/22/14.
 */
public class Gateway extends ALDevice {
    @Override
    protected ArrayList<String> getPropertyNames() {
        // Get the superclass' property names (probably none)
        ArrayList<String> propertyNames = super.getPropertyNames();

        // Add our own
        propertyNames.add("attr_set_cmd");
        propertyNames.add("attr_set_result");
        propertyNames.add("attr_read_data");
        propertyNames.add("conn_status");
        propertyNames.add("status");
        propertyNames.add("error");
        propertyNames.add("control");
        propertyNames.add("join_enable");
        propertyNames.add("join_status");

        return propertyNames;
    }
}
