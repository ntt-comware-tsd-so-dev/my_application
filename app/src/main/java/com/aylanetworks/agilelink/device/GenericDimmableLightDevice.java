package com.aylanetworks.agilelink.device;

import com.aylanetworks.aaml.AylaDevice;

/**
 * AMAP4.x
 * <p/>
 * Copyright 2016 Ayla Networks Inc, all rights reserved
 */
public class GenericDimmableLightDevice extends GenericLightDevice {
    private final static String LOG_TAG = GenericDimmableLightDevice.class.getSimpleName();

    //TODO: Do not have a dimmablelight for now, now sure about the light strength integer.

    public GenericDimmableLightDevice(AylaDevice device) {
        super(device);
    }

    @Override
    public String deviceTypeName() {
        return LOG_TAG;
    }

}// end of GenericDimmableLightDevice class


