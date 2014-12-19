package com.aylanetworks.agilelink.device;

import android.util.Log;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.agilelink.framework.SessionManager;

/**
 * Created by bri on 12/19/14.
 */
public class ALDeviceClassMap implements SessionManager.DeviceClassMap {
    public final static String MODEL_SMART_PLUG = "Smart_Plug";
    public final static String MODEL_SMART_PLUG_2 = "4256050-ZHAC";
    public final static String MODEL_REMOTE_SWITCH = "Wireless_Switch";
    public final static String MODEL_SMART_BULB = "Smart_Bulb_Converter";
    public final static String MODEL_MOTION_SENSOR = "Motion_Sensor";
    public final static String MODEL_MOTION_SENSOR_2 = "Motion_Sens";
    public final static String MODEL_DOOR_SENSOR = "Door_Sensor";

    public Class<? extends AylaDevice> classForDeviceType(String deviceType) {

        if ( deviceType.equals(MODEL_DOOR_SENSOR) )
            return DoorSensor.class;
        if ( deviceType.equals(MODEL_SMART_PLUG) )
            return SmartPlug.class;
        if ( deviceType.equals(MODEL_SMART_PLUG_2) )
            return SmartPlug2.class;
        if ( deviceType.equals(MODEL_REMOTE_SWITCH) )
            return RemoteSwitch.class;
        if ( deviceType.equals(MODEL_SMART_BULB) )
            return SmartBulb.class;
        if ( deviceType.equals(MODEL_MOTION_SENSOR) )
            return MotionSensor.class;
        if ( deviceType.equals(MODEL_MOTION_SENSOR_2) )
            return MotionSensor2.class;

        Log.e("ALDeviceClassMap", "Unknown device type " + deviceType + ": Using AylaDevice as a fallback");
        return AylaDevice.class;
    }
}
