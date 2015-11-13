package com.aylanetworks.agilelink.device;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.TextView;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaProperty;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;

/*
 * ZigbeeMotionSensor.java
 * AgileLink Application Framework
 *
 * Created by David Junod on 5/21/15
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class ZigbeeMotionSensor extends ZigbeeTriggerDevice {

    private final static String LOG_TAG = "ZigbeeMotionSensor";

    public final static String PROPERTY_ZB_INPUT = "1_in_0x0006_0x0000";
    public final static String PROPERTY_ZB_INPUT_IAS = "1_in_0x0500_0x0002";

   // public final static String PROPERTY_ZB_MOTION_SENSOR = PROPERTY_ZB_INPUT;
   public final static String PROPERTY_ZB_MOTION_SENSOR = PROPERTY_ZB_INPUT_IAS;
    public final static String PROPERTY_ZB_MOTION_SENSOR_IAS = PROPERTY_ZB_INPUT_IAS;
    public final static String PROPERTY_ZB_MOTION_SENSOR_FAKE = PROPERTY_ZB_INPUT;

    public ZigbeeMotionSensor(AylaDevice device) {
        super(device);
    }

    @Override
    public String[] getNotifiablePropertyNames() {
      //  return new String[]{PROPERTY_ZB_INPUT};
        return new String[]{PROPERTY_ZB_INPUT_IAS};
    }

    @Override
    public String getObservablePropertyName() { return PROPERTY_ZB_MOTION_SENSOR;  }

    @Override
    public String friendlyNameForPropertyName(String propertyName) {
        if (propertyName.equals(PROPERTY_ZB_MOTION_SENSOR)) {

            return MainActivity.getInstance().getString(R.string.property_motion_sensor_friendly_name);
        }
        if(propertyName.equals(PROPERTY_ZB_INPUT_IAS)){
            return MainActivity.getInstance().getString(R.string.property_motion_friendly_name);
        }
        return super.friendlyNameForPropertyName(propertyName);
    }

    @Override
    public String getTriggerOnName() {
        return MainActivity.getInstance().getString(R.string.motion_trigger_on_name);
    }

    @Override
    public String getTriggerOffName() {
        return MainActivity.getInstance().getString(R.string.motion_trigger_off_name);
    }

    @Override
    public String deviceTypeName() {
        return "Motion Sensor";
    }

    @Override
    public Drawable getDeviceDrawable(Context c) {
        return c.getResources().getDrawable(R.drawable.ic_motionsensor_red);
    }

    @Override
    public int getItemViewType() {
        return AgileLinkDeviceCreator.ITEM_VIEW_TYPE_WITH_STATUS;
    }

    public boolean isMoving() {
        AylaProperty prop = getProperty(getObservablePropertyName());
        if (prop != null && prop.value != null && Integer.parseInt(prop.value) != 0) {
            return true;
        }

        return false;
    }

    @Override
    public void bindViewHolder(RecyclerView.ViewHolder holder) {
        super.bindViewHolder(holder);
        if (holder instanceof ZigbeeStatusDeviceHolder) {
            Resources res = MainActivity.getInstance().getResources();
            String statusText = isMoving()? getTriggerOnName(): getTriggerOffName();
            ZigbeeStatusDeviceHolder h = (ZigbeeStatusDeviceHolder) holder;
            if(h.statusTextView != null){
                h.statusTextView.setText(statusText);
            }


        }
    }
}
