package com.aylanetworks.agilelink.device.zigbee;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;

/**
 * Created by Brian King on 12/19/14.
 */
public class MotionSensor extends Device {
    public MotionSensor(AylaDevice aylaDevice) {
        super(aylaDevice);
    }

    @Override
    public String deviceTypeName() {
        return "Motion Sensor";
    }

    @Override
    public String registrationType() {
        return AylaNetworks.AML_REGISTRATION_TYPE_BUTTON_PUSH;
    }

    @Override
    public Drawable getDeviceDrawable(Context c) {
        return c.getResources().getDrawable(R.drawable.motion_sensor);
    }

}
