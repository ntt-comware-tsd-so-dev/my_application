package com.aylanetworks.agilelink.device;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;

import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.zigbee.AylaSceneZigbeeNodeProperty;
import com.aylanetworks.agilelink.MainActivity;

/*
 * ZigbeeSwitchedDevice.java
 * AgileLink Application Framework
 *
 * Created by David Junod on 5/5/15
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class ZigbeeSwitchedDevice extends SwitchedDevice implements View.OnClickListener {
    private final static String LOG_TAG = "ZigbeeSwitchedDevice";

    public final static String PROPERTY_ZB_INPUT = "1_in_0x0006_0x0000";

    public ZigbeeSwitchedDevice(AylaDevice device) {
        super(device);
    }

    @Override
    public String getObservablePropertyName() { return PROPERTY_ZB_INPUT;  }

    @Override
    public boolean isDeviceNode() {
        return true;
    }

    @Override
    public void bindViewHolder(RecyclerView.ViewHolder holder) {
        super.bindViewHolder(holder);

        if (holder instanceof SwitchedDeviceViewHolder) {
            SwitchedDeviceViewHolder h = (SwitchedDeviceViewHolder) holder;

            Resources res = MainActivity.getInstance().getResources();
            boolean onOff = isOn();
            if (h._sceneDeviceEntity != null) {
                h._switchButton.setOnClickListener(null);
                for (AylaSceneZigbeeNodeProperty prop : h._sceneDeviceEntity.properties) {
                    if (TextUtils.equals("1_in_0x0006_0x0000", prop.name)) {
                        onOff = "1".equals(prop.value);
                    }
                }
            } else {
                h._switchButton.setOnClickListener(this);
            }

            Drawable buttonDrawable = getSwitchedDrawable(res, onOff);
            h._switchButton.setImageDrawable(buttonDrawable);
        }
    }
}
