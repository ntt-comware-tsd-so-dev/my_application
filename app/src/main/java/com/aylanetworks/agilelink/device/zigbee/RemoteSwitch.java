package com.aylanetworks.agilelink.device.zigbee;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.agilelink.framework.Device;

/**
 * Created by Brian King on 12/19/14.
 */
public class RemoteSwitch extends Device {
    public RemoteSwitch(AylaDevice aylaDevice) {
        super(aylaDevice);
    }

    @Override
    public String deviceTypeName() {
        return "Remote Switch";
    }

    @Override
    public String registrationType() {
        return AylaNetworks.AML_REGISTRATION_TYPE_BUTTON_PUSH;
    }

}
