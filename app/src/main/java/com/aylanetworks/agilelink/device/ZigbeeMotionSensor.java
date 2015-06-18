package com.aylanetworks.agilelink.device;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;

/*
 * ZigbeeMotionSensor.java
 * AgileLink Application Framework
 *
 * Created by David Junod on 5/21/15
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class ZigbeeMotionSensor extends ZigbeeTriggerDevice {

    private final static String LOG_TAG = "ZigbeeMotionSensor";

    public final static String PROPERTY_ZB_INPUT = "1_in_0x0006_0x0000";
    public final static String PROPERTY_ZB_INPUT_IAS = "1_in_0x0500_0x0002";

    public final static String PROPERTY_ZB_MOTION_SENSOR = PROPERTY_ZB_INPUT;
    public final static String PROPERTY_ZB_MOTION_SENSOR_IAS = PROPERTY_ZB_INPUT_IAS;
    public final static String PROPERTY_ZB_MOTION_SENSOR_FAKE = PROPERTY_ZB_INPUT;

    public ZigbeeMotionSensor(AylaDevice device) {
        super(device);
    }

    @Override
    public String[] getNotifiablePropertyNames() {
        return new String[]{PROPERTY_ZB_INPUT};
    }

    @Override
    public String getObservablePropertyName() { return PROPERTY_ZB_MOTION_SENSOR;  }

    @Override
    public String friendlyNameForPropertyName(String propertyName) {
        if (propertyName.equals(PROPERTY_ZB_MOTION_SENSOR)) {
            return MainActivity.getInstance().getString(R.string.property_remote_switch_friendly_name);
        }
        return super.friendlyNameForPropertyName(propertyName);
    }

    @Override
    public String deviceTypeName() {
        return "Motion Sensor";
    }

    @Override
    public Drawable getDeviceDrawable(Context c) {
        return c.getResources().getDrawable(R.drawable.ic_motionsensor_red);
    }
}
