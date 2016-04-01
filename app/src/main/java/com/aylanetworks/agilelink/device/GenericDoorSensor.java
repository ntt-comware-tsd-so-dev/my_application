package com.aylanetworks.agilelink.device;

import com.aylanetworks.aaml.AylaDevice;

/**
 * AMAP4.x
 * <p/>
 * Copyright 2016 Ayla Networks Inc, all rights reserved
 */
public class GenericDoorSensor extends GenericNodeDevice {
    private static final String LOG_TAG = GenericDoorSensor.class.getSimpleName();

    public GenericDoorSensor(AylaDevice device) {
        super(device);
    }

    @Override
    public String deviceTypeName() {
        return LOG_TAG;
    }
}// end of GenericDoorSensor class


