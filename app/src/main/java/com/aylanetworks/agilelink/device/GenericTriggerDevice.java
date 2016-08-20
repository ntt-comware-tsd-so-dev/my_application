package com.aylanetworks.agilelink.device;

import android.text.TextUtils;

import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;

/**
 * AMAP4.x
 * <p/>
 * Copyright 2016 Ayla Networks Inc, all rights reserved
 */
public class GenericTriggerDevice extends GenericDevice {

    private final static String LOG_TAG = GenericTriggerDevice.class.getSimpleName();

    //TODO: Do not have generic trigger device for now, not sure what it is.

    public GenericTriggerDevice (AylaDevice device) {
        super(device);
    }

    @Override
    public String deviceTypeName() {
        return LOG_TAG;
    }

    /**
     * Text to use for trigger on
     * @return The text
     */
    public String getTriggerOnName() {
        return MainActivity.getInstance().getString(R.string.trigger_on_name);
    }

    /**
     * Text to use for trigger off
     * @return The text
     */
    public String getTriggerOffName() {
        return MainActivity.getInstance().getString(R.string.trigger_off_name);
    }

    @Override
    public String friendlyNameForPropertyName(String propertyName) {
        AylaProperty ap = getProperty(propertyName);
        if (ap == null || TextUtils.isEmpty(ap.getDisplayName())) {
            return super.friendlyNameForPropertyName(propertyName);
        }
        return ap.getDisplayName();
    }

}// end of GenericTriggerDevice class



