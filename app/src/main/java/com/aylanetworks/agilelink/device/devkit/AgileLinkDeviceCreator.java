package com.aylanetworks.agilelink.device.devkit;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.DeviceCreator;
import com.aylanetworks.agilelink.framework.GenericDeviceViewHolder;

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

    public final static int ITEM_VIEW_TYPE_DEVKIT_DEVICE = 1;
    public final static int ITEM_VIEW_TYPE_SMARTPLUG = 2;

    public Device deviceForAylaDevice(AylaDevice aylaDevice) {

       if ( aylaDevice.oemModel.equals("ledevb") ) {
            // This is the Ayla devkit.
            return new DevkitDevice(aylaDevice);
        }

        if ( aylaDevice.oemModel.equals("smartplug1") ) {
            // This is the Ayla Demo Smart plug
            return new SwitchedDevice(aylaDevice);
        }

        if ( aylaDevice.oemModel.equals("EWPlug1") )
        {
            // This is an Everwin MFI/Homekit enabled smart plug.
            return new SwitchedDevice(aylaDevice);
        }

        //  We don't know what this is. Create a generic device.
        Log.e(LOG_TAG, "Could not identify this device: " + aylaDevice);
        return new Device(aylaDevice);
    }

    @Override
    public RecyclerView.ViewHolder viewHolderForViewType(ViewGroup parent, int viewType) {
        View v = null;
        switch ( viewType ) {
             case ITEM_VIEW_TYPE_DEVKIT_DEVICE:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_devkit_device, parent, false);
                return new DevkitDeviceViewHolder(v);

            case ITEM_VIEW_TYPE_SMARTPLUG:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_switched_device, parent, false);
                return new SwitchedDeviceViewHolder(v);

            case ITEM_VIEW_TYPE_GENERIC_DEVICE:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_generic_device, parent, false);
                return new GenericDeviceViewHolder(v);
        }

        return super.viewHolderForViewType(parent, viewType);
    }

    public List<Class<? extends Device>> getSupportedDeviceClasses() {
        List<Class<? extends Device>> classList = new ArrayList<Class<? extends Device>>();

        classList.add(DevkitDevice.class);
        classList.add(SwitchedDevice.class);
        return classList;
    }
}
