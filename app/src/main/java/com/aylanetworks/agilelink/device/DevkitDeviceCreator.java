package com.aylanetworks.agilelink.device;

import android.util.Log;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.Gateway;
import com.aylanetworks.agilelink.framework.SessionManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Created by Brian King on 12/19/14.
 */
public class DevkitDeviceCreator implements SessionManager.DeviceCreator {
    private final static String LOG_TAG = "DevkitDeviceCreator";

    public Device deviceForAylaDevice(AylaDevice aylaDevice) {

        if ( aylaDevice.oemModel.equals("ledevb") ) {
            // This is the Ayla devkit.
            return new DevkitDevice(aylaDevice);
        }

        //  We don't know what this is.
        return null;
    }
}
