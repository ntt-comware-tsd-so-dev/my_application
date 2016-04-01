package com.aylanetworks.agilelink.device;

import com.aylanetworks.aaml.AylaDevice;

/**
 * AMAP4.x
 * <p/>
 * Copyright 2016 Ayla Networks Inc, all rights reserved
 */
public class GenericMotionSensor extends GenericNodeDevice {
    private static final String LOG_TAG = GenericMotionSensor.class.getSimpleName();

    //TODO: Do not have a motion sensor for now. Make it fully fledged.

    public GenericMotionSensor(AylaDevice device) {
        super(device);
    }

    @Override
    public String deviceTypeName() {
        return LOG_TAG;
    }


}// end of GenericMotionSensor class



