package com.aylanetworks.agilelink.device;

import com.aylanetworks.aaml.AylaDevice;

/**
 * AMAP4.x
 * <p/>
 * Copyright 2016 Ayla Networks Inc, all rights reserved
 */
public class GenericSwitchedDevice extends GenericNodeDevice {
    private final static String LOG_TAG = GenericSwitchedDevice.class.getSimpleName();

    //TODO: Do not have a generic switched device for now. Make it fully fledged.

    public GenericSwitchedDevice(AylaDevice device) {
        super(device);
    }

    @Override
    public String deviceTypeName() {
        return LOG_TAG;
    }
}// end of GenericSwitchedDevice class




