package com.aylanetworks.agilelink.device.zigbee;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.agilelink.framework.Device;

/**
 * Created by Brian King on 12/19/14.
 */
public class SmartBulb extends Device {
    public SmartBulb(AylaDevice aylaDevice) {
        super(aylaDevice);
    }

    @Override
    public String deviceTypeName() {
        return "Smart Bulb";
    }

    @Override
    public String registrationType() {
        return AylaNetworks.AML_REGISTRATION_TYPE_BUTTON_PUSH;
    }

}
