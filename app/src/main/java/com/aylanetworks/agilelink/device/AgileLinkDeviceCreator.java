package com.aylanetworks.agilelink.device;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.DeviceCreator;
import com.aylanetworks.agilelink.framework.Gateway;
import com.aylanetworks.agilelink.framework.GenericDeviceViewHolder;
import com.aylanetworks.agilelink.framework.Logger;
import com.aylanetworks.agilelink.framework.UIConfig;
import com.aylanetworks.agilelink.framework.ZigbeeGateway;

import java.util.ArrayList;
import java.util.List;

/*
 * AgileLinkDeviceCreator.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 12/19/14.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class AgileLinkDeviceCreator extends DeviceCreator {
    private final static String LOG_TAG = "AgileLinkDeviceCreator";

    public final static int ITEM_VIEW_TYPE_GENERIC_DEVICE = 0;
    public final static int ITEM_VIEW_TYPE_DEVKIT_DEVICE = 1;
    public final static int ITEM_VIEW_TYPE_SWITCHED = 2;
    public final static int ITEM_VIEW_TYPE_DIMMABLE = 3;
    public final static int ITEM_VIEW_TYPE_WITH_STATUS = 4;

    /**
     * This is the default device creator for AMAP.  Provide your own DeviceCreator and devices.
     * @param aylaDevice The AylaDevice object to be wrapped
     * @return Device
     */
    public Device deviceForAylaDevice(AylaDevice aylaDevice) {

        if (aylaDevice.oemModel == null) {
            Logger.logError(LOG_TAG, "No oemModel set on device: " + aylaDevice);

            // in some cases, the Generic Gateway OR Zigbee Gateway has a null oemModel (instead of generic)
            if (aylaDevice.model.equals("AY001MRT1")) {
                // This is a gateway.
                return new Gateway(aylaDevice);
            }
            return new GenericDevice(aylaDevice);
        }

        if (aylaDevice.oemModel.equals("ledevb")) {
            // This is the Ayla devkit.
            return new AylaEVBDevice(aylaDevice);
        }

        if (aylaDevice.oemModel.equals("smartplug1")) {
            // This is the Ayla Demo Smart plug
            return new SwitchedDevice(aylaDevice);
        }

        if (aylaDevice.oemModel.equals("EWPlug1")) {
            // This is an Everwin MFI/Homekit enabled smart plug.
            return new SwitchedDevice(aylaDevice);
        }

        // Gateway, including Zigbee, devices
        if (aylaDevice.oemModel.equals("generic")) {
            if (aylaDevice.model.equals("AY001MRT1")) {
                // This is a generic gateway.
                return new Gateway(aylaDevice);
            }
            if (aylaDevice.model.equals("GenericNode") || aylaDevice.model.equals("Generic Node")) {
                // This is a generic node
                return new GenericNodeDevice(aylaDevice);
            }
        }
        if (aylaDevice.model.equals("GenericNode") || aylaDevice.model.equals("Generic Node")) {
            if (aylaDevice.oemModel.equals("NexturnSmartPlug")) {
                // This is a Generic Gateway smart plug.
                return new ZigbeeSwitchedDevice(aylaDevice);
            }
            if (aylaDevice.oemModel.equals("NexturnSmart_Bulb_Converter")) {
                // This is a Generic Gateway smart bulb.
                return new ZigbeeLightDevice(aylaDevice);
            }
            if (aylaDevice.oemModel.equals("NexturnMotion_Sensor")) {
                // This is a Generic Gateway motion sensor.
                return new ZigbeeMotionSensor(aylaDevice);
            }
            if (aylaDevice.model.equals("NXPZHA-DimmableLight")) {
                // This is a Generic Gateway dimmable light.
                return new ZigbeeDimmableLightDevice(aylaDevice);
            }
            // NexturnZHA-Thermostat

            // This is a generic node
            return new GenericNodeDevice(aylaDevice);
        }
        if (aylaDevice.oemModel.equals("zigbee1") || aylaDevice.oemModel.equals("liunxex1")){
            if (aylaDevice.model.equals("AY001MRT1") && aylaDevice.isGateway()) {
                // This is a Zigbee gateway.
                return new ZigbeeGateway(aylaDevice);
            }
            if (aylaDevice.model.equals("Smart_Plug") || aylaDevice.model.equals("4256050-RZHAC")) {
                // This is a Zigbee smart plug.
                return new ZigbeeSwitchedDevice(aylaDevice);
            }
            if (aylaDevice.model.equals("Smart_Bulb_Converter")) {
                // This is a Zigbee smart bulb.
                return new ZigbeeLightDevice(aylaDevice);
            }
            if (aylaDevice.model.equals("ZHA-DimmableLight")) {
                // This is a Zigbee dimmable light.
                return new ZigbeeDimmableLightDevice(aylaDevice);
            }
            if (aylaDevice.model.equals("Wireless_Switch")) {
                // This is a Zigbee remote switch.
                return new ZigbeeWirelessSwitch(aylaDevice);
            }
            if (aylaDevice.model.equals("Motion_Sensor") || aylaDevice.model.equals("Motion_Sens")) {
                // This is a Zigbee motion sensor.
                return new ZigbeeMotionSensor(aylaDevice);
            }
            if (aylaDevice.model.equals("Door_Sensor")) {
                // This is a Zigbee door sensor.
                return new ZigbeeDoorSensor(aylaDevice);
            }
        }

        //  We don't know what this is. Create a generic device.
        Logger.logError(LOG_TAG, "Could not identify this device: " + aylaDevice);
        return new GenericDevice(aylaDevice);
    }

    public RecyclerView.ViewHolder viewHolderForViewType(ViewGroup parent, int viewType) {
        View v;
        UIConfig.ListStyle listStyle = MainActivity.getUIConfig()._listStyle;
        int resId;
        switch (viewType) {
            case ITEM_VIEW_TYPE_DEVKIT_DEVICE:
                resId = listStyle == UIConfig.ListStyle.Grid ? R.layout.cardview_ayla_evb_device_grid :
                        listStyle == UIConfig.ListStyle.ExpandingList ? R.layout.cardview_ayla_evb_device_expandable :
                                R.layout.cardview_ayla_evb_device;
                v = LayoutInflater.from(parent.getContext()).inflate(resId, parent, false);
                return new AylaEVBDeviceViewHolder(v);

            case ITEM_VIEW_TYPE_SWITCHED:
                resId = listStyle == UIConfig.ListStyle.Grid ? R.layout.cardview_switched_device_grid :
                        listStyle == UIConfig.ListStyle.ExpandingList ? R.layout.cardview_switched_device_expandable :
                                R.layout.cardview_switched_device;
                v = LayoutInflater.from(parent.getContext()).inflate(resId, parent, false);
                return new SwitchedDeviceViewHolder(v);

            case ITEM_VIEW_TYPE_DIMMABLE:
                resId = listStyle == UIConfig.ListStyle.Grid ? R.layout.cardview_dimmable_device_grid :
                        listStyle == UIConfig.ListStyle.ExpandingList ? R.layout.cardview_dimmable_device_expandable :
                                R.layout.cardview_dimmable_device;
                v = LayoutInflater.from(parent.getContext()).inflate(resId, parent, false);
                return new DimmableLightViewHolder(v);

            case ITEM_VIEW_TYPE_GENERIC_DEVICE:
                resId = listStyle == UIConfig.ListStyle.Grid ? R.layout.cardview_generic_device_grid :
                        listStyle == UIConfig.ListStyle.ExpandingList ? R.layout.cardview_generic_device_expandable :
                                R.layout.cardview_generic_device;
                v = LayoutInflater.from(parent.getContext()).inflate(resId, parent, false);
                return new GenericDeviceViewHolder(v);

            case ITEM_VIEW_TYPE_WITH_STATUS:
                resId = listStyle == UIConfig.ListStyle.Grid ? R.layout.cardview_text_status_grid :
                        listStyle == UIConfig.ListStyle.ExpandingList ? R.layout.cardview_switched_device_expandable :
                                R.layout.cardview_text_status;
                v = LayoutInflater.from(parent.getContext()).inflate(resId, parent, false);
                return new ZigbeeStatusDeviceHolder(v);
        }

        return null;
    }

    public List<Class<? extends Device>> getSupportedDeviceClasses() {
        List<Class<? extends Device>> classList = new ArrayList<Class<? extends Device>>();

        classList.add(AylaEVBDevice.class);
        classList.add(SwitchedDevice.class);
        classList.add(Gateway.class);
        classList.add(ZigbeeNodeDevice.class);
        return classList;
    }
}
