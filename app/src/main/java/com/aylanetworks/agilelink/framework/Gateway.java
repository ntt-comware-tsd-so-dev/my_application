package com.aylanetworks.agilelink.framework;

import android.util.Log;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaDeviceGateway;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.SessionManager;
import com.google.gson.JsonElement;

import java.util.ArrayList;

/**
 * Created by Brian King on 12/22/14.
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
        propertyNames.add("conn_status");
        propertyNames.add("status");
        propertyNames.add("error");
        propertyNames.add("control");
        propertyNames.add("join_enable");
        propertyNames.add("join_status");

        return propertyNames;
    }

    @Override
    public boolean isGateway() {
        return true;
    }

    public void configureWithJsonElement(JsonElement json) {
        Log.e(LOG_TAG, "Configure with: " + json.toString());
    }
}
