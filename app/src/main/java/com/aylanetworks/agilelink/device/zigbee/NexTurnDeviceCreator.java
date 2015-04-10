package com.aylanetworks.agilelink.device.zigbee;

import android.util.Log;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.DeviceCreator;
import com.aylanetworks.agilelink.framework.Gateway;

import java.util.ArrayList;
import java.util.List;

/*
 * NexTurnDeviceCreator.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 12/19/14.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class NexTurnDeviceCreator extends DeviceCreator {
    private final static String LOG_TAG = "nexTurnDeviceCreator";

    public final static String PRODUCT_CLASS_GATEWAY = "zigbee";
    public final static String MODEL_SMART_PLUG = "Smart_Plug";
    public final static String MODEL_REMOTE_SWITCH = "Wireless_Switch";
    public final static String MODEL_SMART_BULB = "Smart_Bulb_Converter";
    public final static String MODEL_MOTION_SENSOR = "Motion_Sensor";
    public final static String MODEL_DOOR_SENSOR = "Door_Sensor";

    public Device deviceForAylaDevice(AylaDevice aylaDevice) {

        // Check to see if this is a gateway device first. It needs additional
        // initialization.
        String productClass = aylaDevice.productClass;
        if ( productClass.equals(PRODUCT_CLASS_GATEWAY) ) {
            return new Gateway(aylaDevice);
        }

        String deviceType = aylaDevice.getModel();

        Device device = null;

        if ( deviceType.equals(MODEL_DOOR_SENSOR) )
            device = new DoorSensor(aylaDevice);
        else if ( deviceType.equals(MODEL_SMART_PLUG) )
            device = new SmartPlug(aylaDevice);
        else if ( deviceType.equals(MODEL_REMOTE_SWITCH) )
            device = new RemoteSwitch(aylaDevice);
        else if ( deviceType.equals(MODEL_SMART_BULB) )
            device = new SmartBulb(aylaDevice);
        else if ( deviceType.equals(MODEL_MOTION_SENSOR) )
            device = new MotionSensor(aylaDevice);

        if ( device == null ) {
            Log.e(LOG_TAG, "Unknown device type: " + deviceType);
            device = new Device(aylaDevice);
        }

        return device;
    }

    @Override
    public List<Class<? extends Device>> getSupportedDeviceClasses() {
        List<Class<? extends Device>> classList = new ArrayList<Class<? extends Device>>();

        classList.add(DoorSensor.class);
        classList.add(SmartPlug.class);
        classList.add(SmartBulb.class);
        classList.add(RemoteSwitch.class);
        classList.add(MotionSensor.class);

        return classList;
    }
}
