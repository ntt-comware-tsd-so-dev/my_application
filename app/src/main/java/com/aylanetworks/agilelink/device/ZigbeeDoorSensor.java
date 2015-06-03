package com.aylanetworks.agilelink.device;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;

/*
 * ZigbeeDoorSensor.java
 * AgileLink Application Framework
 *
 * Created by David Junod on 5/21/15
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class ZigbeeDoorSensor extends Device {

    private final static String LOG_TAG = "ZigbeeDoorSensor";

    public final static String PROPERTY_ZB_OUTPUT = "1_out_0x0006_0x0000";
    public final static String PROPERTY_ZB_DOOR_SENSOR = PROPERTY_ZB_OUTPUT;

    public ZigbeeDoorSensor(AylaDevice device) {
        super(device);
    }

    @Override
    public String[] getNotifiablePropertyNames() {
        return new String[]{PROPERTY_ZB_DOOR_SENSOR};
    }

    @Override
    public String getObservablePropertyName() { return PROPERTY_ZB_DOOR_SENSOR;  }

    @Override
    public String friendlyNameForPropertyName(String propertyName) {
        if (propertyName.equals(PROPERTY_ZB_DOOR_SENSOR)) {
            return MainActivity.getInstance().getString(R.string.property_door_sensor_friendly_name);
        }
        return super.friendlyNameForPropertyName(propertyName);
    }

    @Override
    public String deviceTypeName() {
        return "Door Sensor";
    }

    @Override
    public Drawable getDeviceDrawable(Context c) {
        return c.getResources().getDrawable(R.drawable.ic_door_red);
    }

    @Override
    public boolean isDeviceNode() {
        return true;
    }

    @Override
    public int hasPostRegistrationProcessingResourceId() { return R.string.add_device_sensor_warning; }

}
