package com.aylanetworks.agilelink.device;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.agilelink.R;

/**
 * AMAP4.x
 * <p/>
 * Copyright 2016 Ayla Networks Inc, all rights reserved
 */
public class GenericLightDevice extends GenericSwitchedDevice {
    private static final String LOG_TAG = GenericLightDevice.class.getSimpleName();

    public GenericLightDevice(AylaDevice device) {
        super(device);
    }

    @Override
    public String deviceTypeName() {
        return LOG_TAG;
    }


    @Override
    public Drawable getDeviceDrawable(Context c) {
        return c.getResources().getDrawable(R.drawable.smart_bulb);
    }

    @Override
    public Drawable getSwitchedDrawable(Resources res, boolean isOn) {
        return res.getDrawable(isOn ? R.drawable.ic_light_on : R.drawable.ic_light_off);
    }

    @Override
    public Drawable getSwitchedPendingDrawable(Resources res) {
        return res.getDrawable(R.drawable.ic_light_pending);
    }


}// end of GenericLightDevice class



