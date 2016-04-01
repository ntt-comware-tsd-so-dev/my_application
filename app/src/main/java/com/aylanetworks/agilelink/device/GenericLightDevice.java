package com.aylanetworks.agilelink.device;

import com.aylanetworks.aaml.AylaDevice;

/**
 * AMAP4.x
 * <p/>
 * Copyright 2016 Ayla Networks Inc, all rights reserved
 */
public class GenericLightDevice extends GenericNodeDevice {
    private static final String LOG_TAG = GenericLightDevice.class.getSimpleName();

    public GenericLightDevice(AylaDevice device) {
        super(device);
    }

    @Override
    public String deviceTypeName() {
        return LOG_TAG;
    }


}// end of GenericLightDevice class



