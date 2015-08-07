package com.aylanetworks.agilelink.device;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaProperty;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;

/*
 * ZigbeeDoorSensor.java
 * AgileLink Application Framework
 *
 * Created by David Junod on 5/21/15
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class ZigbeeDoorSensor extends ZigbeeTriggerDevice {

    private final static String LOG_TAG = "ZigbeeDoorSensor";

    public final static String PROPERTY_ZB_OUTPUT = "1_out_0x0006_0x0000";
    public final static String PROPERTY_ZB_DOOR_SENSOR = PROPERTY_ZB_OUTPUT;

    public ZigbeeDoorSensor(AylaDevice device) {
        super(device);
    }

    @Override
    public String[] getNotifiablePropertyNames() {
        return new String[]{PROPERTY_ZB_DOOR_SENSOR};
    }

    @Override
    public String getObservablePropertyName() { return PROPERTY_ZB_DOOR_SENSOR;  }

    @Override
    public String friendlyNameForPropertyName(String propertyName) {
        if (propertyName.equals(PROPERTY_ZB_DOOR_SENSOR)) {
            return MainActivity.getInstance().getString(R.string.property_door_sensor_friendly_name);
        }
        return super.friendlyNameForPropertyName(propertyName);
    }

    @Override
    public String getTriggerOnName() {
        return MainActivity.getInstance().getString(R.string.door_trigger_on_name);
    }

    @Override
    public String getTriggerOffName() {
        return MainActivity.getInstance().getString(R.string.door_trigger_off_name);
    }

    @Override
    public String deviceTypeName() {
        return "Door Sensor";
    }

    @Override
    public Drawable getDeviceDrawable(Context c) {
        return c.getResources().getDrawable(R.drawable.ic_door_red);
    }

    @Override
    public void bindViewHolder(RecyclerView.ViewHolder holder) {
        super.bindViewHolder(holder);
        if (holder instanceof ZigbeeStatusDeviceHolder) {
            Resources res = MainActivity.getInstance().getResources();
            String statusText = isOpen()? getTriggerOnName(): getTriggerOffName();
            ZigbeeStatusDeviceHolder h = (ZigbeeStatusDeviceHolder) holder;
            h.statusTextView.setText(statusText);

        }
    }

    public boolean isOpen() {
        AylaProperty prop = getProperty(getObservablePropertyName());
        if (prop != null && prop.value != null && Integer.parseInt(prop.value) != 0) {
            return true;
        }

        return false;
    }

}
