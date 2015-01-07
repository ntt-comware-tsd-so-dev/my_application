package com.aylanetworks.agilelink.device;

import android.util.Log;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.Gateway;
import com.aylanetworks.agilelink.framework.SessionManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Created by Brian King on 12/19/14.
 */
public class ALDeviceCreator implements SessionManager.DeviceCreator {
    private final static String LOG_TAG = "ALDeviceCreator";

    public final static String PRODUCT_CLASS_GATEWAY = "zigbee";
    public final static String MODEL_SMART_PLUG = "Smart_Plug";
    public final static String MODEL_SMART_PLUG_2 = "4256050-ZHAC";
    public final static String MODEL_REMOTE_SWITCH = "Wireless_Switch";
    public final static String MODEL_SMART_BULB = "Smart_Bulb_Converter";
    public final static String MODEL_MOTION_SENSOR = "Motion_Sensor";
    public final static String MODEL_MOTION_SENSOR_2 = "Motion_Sens";
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
        if ( deviceType.equals(MODEL_SMART_PLUG) )
            device = new SmartPlug(aylaDevice);
        if ( deviceType.equals(MODEL_SMART_PLUG_2) )
            device = new SmartPlug2(aylaDevice);
        if ( deviceType.equals(MODEL_REMOTE_SWITCH) )
            device = new RemoteSwitch(aylaDevice);
        if ( deviceType.equals(MODEL_SMART_BULB) )
            device = new SmartBulb(aylaDevice);
        if ( deviceType.equals(MODEL_MOTION_SENSOR) )
            device = new MotionSensor(aylaDevice);
        if ( deviceType.equals(MODEL_MOTION_SENSOR_2) )
            device = new MotionSensor2(aylaDevice);

        if ( device == null ) {
            Log.e(LOG_TAG, "Unknown device type: " + deviceType);
            device = new Device(aylaDevice);
        }

        return device;
    }
}
