package com.aylanetworks.agilelink.device;

import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;

import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.aylasdk.zigbee.AylaSceneZigbeeNodeProperty;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;

import java.util.ArrayList;

/*
 * ZigbeeDimmableLightDevice.java
 * AgileLink Application Framework
 *
 * Created by David Junod on 5/7/15
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class ZigbeeDimmableLightDevice extends ZigbeeLightDevice {

    private final static String LOG_TAG = "ZigbeeDimmableLightDevice";

    public final static String PROPERTY_ZB_DIMMABLE_LIGHT = "1_in_0x0008_0x04";
    public final static String PROPERTY_ZB_DIMMABLE_LIGHT_LEVEL = "1_in_0x0008_0x0000";

    public ZigbeeDimmableLightDevice(AylaDevice device) {
        super(device);
    }

    public int dimmableLightLevel() {
        AylaProperty prop = getProperty(PROPERTY_ZB_DIMMABLE_LIGHT);
        int value = 0;
        if (prop != null && prop.value != null) {
            value = Integer.parseInt(prop.value);
        }
        return value;
    }


    @Override
    public String deviceTypeName() {
        return "Dimmable Bulb";
    }

    @Override
    protected ArrayList<String> getPropertyNames() {
        // Get the superclass' property names (probably none)
        ArrayList<String> propertyNames = super.getPropertyNames();

        // Add our own
        propertyNames.add(PROPERTY_ZB_DIMMABLE_LIGHT);
        propertyNames.add(PROPERTY_ZB_DIMMABLE_LIGHT_LEVEL);

        return propertyNames;
    }

    @Override
    public int getItemViewType() {
        return AgileLinkDeviceCreator.ITEM_VIEW_TYPE_DIMMABLE;
    }

    @Override
    public void bindViewHolder(RecyclerView.ViewHolder holder) {
        super.bindViewHolder(holder);

        DimmableLightViewHolder h = (DimmableLightViewHolder) holder;

        Resources res = MainActivity.getInstance().getResources();
        int value = dimmableLightLevel();
        boolean onOff = isOn();
        if (h._sceneDeviceEntity != null) {
            h._slider.setVisibility(View.GONE);
            h._slider.setOnSeekBarChangeListener(null);
            h._switchButton.setOnClickListener(null);
            for (AylaSceneZigbeeNodeProperty prop : h._sceneDeviceEntity.properties) {
                if (TextUtils.equals("1_in_0x0006_0x0000", prop.name)) {
                    try {
                        onOff = "1".equals(prop.value);
                    } catch (Exception ex) {}
                } else if (TextUtils.equals(PROPERTY_ZB_DIMMABLE_LIGHT, prop.name)) {
                    try {
                        value = Integer.parseInt(prop.value);
                    } catch (Exception ex) {}
                }

            }
        } else {
            h._slider.setTag(h);
            h._slider.setVisibility(View.VISIBLE);
            h._slider.setOnSeekBarChangeListener(h);
            //h._switchButton.setOnClickListener(h);
        }
        h._switchButton.setSelected(onOff);
        h._switchLabel.setText(res.getString(onOff ? R.string.switched_on_name : R.string.switched_off_name));
        h._slider.setProgress(value);
    }
}
