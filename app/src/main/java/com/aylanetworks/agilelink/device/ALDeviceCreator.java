package com.aylanetworks.agilelink.device;

import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.agilelink.framework.ALDevice;
import com.aylanetworks.agilelink.framework.SessionManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Created by Brian King on 12/19/14.
 */
public class ALDeviceCreator implements SessionManager.DeviceCreator {
    public final static String PRODUCT_CLASS_GATEWAY = "zigbee";
    public final static String MODEL_SMART_PLUG = "Smart_Plug";
    public final static String MODEL_SMART_PLUG_2 = "4256050-ZHAC";
    public final static String MODEL_REMOTE_SWITCH = "Wireless_Switch";
    public final static String MODEL_SMART_BULB = "Smart_Bulb_Converter";
    public final static String MODEL_MOTION_SENSOR = "Motion_Sensor";
    public final static String MODEL_MOTION_SENSOR_2 = "Motion_Sens";
    public final static String MODEL_DOOR_SENSOR = "Door_Sensor";

    public ALDevice deviceFromJsonElement(JsonElement deviceJson) {

        JsonObject jsonObject = deviceJson.getAsJsonObject();

        // Check to see if this is a gateway device first
        String productClass = jsonObject.get("product_class").getAsString();
        if ( productClass.equals(PRODUCT_CLASS_GATEWAY) ) {
            return AylaSystemUtils.gson.fromJson(deviceJson, Gateway.class);
        }

        String deviceType = null;

        deviceType = jsonObject.get("model").getAsString();
        Class cl = ALDevice.class;

        if ( deviceType.equals(MODEL_DOOR_SENSOR) )
            cl = DoorSensor.class;
        if ( deviceType.equals(MODEL_SMART_PLUG) )
            cl = SmartPlug.class;
        if ( deviceType.equals(MODEL_SMART_PLUG_2) )
            cl = SmartPlug2.class;
        if ( deviceType.equals(MODEL_REMOTE_SWITCH) )
            cl = RemoteSwitch.class;
        if ( deviceType.equals(MODEL_SMART_BULB) )
            cl = SmartBulb.class;
        if ( deviceType.equals(MODEL_MOTION_SENSOR) )
            cl = MotionSensor.class;
        if ( deviceType.equals(MODEL_MOTION_SENSOR_2) )
            cl = MotionSensor2.class;

        return (ALDevice)AylaSystemUtils.gson.fromJson(deviceJson, cl);
    }
}
