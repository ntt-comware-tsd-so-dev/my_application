package com.aylanetworks.agilelink.device;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.agilelink.R;

/*
 * GenericNodeDevice.java
 * AgileLink Application Framework
 *
 * Created by David Junod on 7/9/15
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class GenericNodeDevice extends GenericDevice {

    private final static String LOG_TAG = GenericNodeDevice.class.getSimpleName();

    public GenericNodeDevice(AylaDevice device) {
        super(device);
    }

    @Override
    public String friendlyNameForPropertyName(String propertyName) {
        AylaProperty ap = getProperty(propertyName);
        if (ap == null || TextUtils.isEmpty(ap.displayName)) {
            return super.friendlyNameForPropertyName(propertyName);
        }
        return ap.displayName;
    }

    @Override
    public boolean isDeviceNode() {
        return true;
    }

    @Override
    public String registrationType() {
        return AylaNetworks.AML_REGISTRATION_TYPE_NODE;
    }

    @Override
    public String deviceTypeName() {
        return LOG_TAG;
    }

    @Override
    public Drawable getDeviceDrawable(Context c) {
        return c.getResources().getDrawable(R.drawable.smart_bulb);
    }
}// end of GenericSwitchedDevice class
