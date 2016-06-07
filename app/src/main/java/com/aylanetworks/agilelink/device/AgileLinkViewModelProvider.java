package com.aylanetworks.agilelink.device;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aylanetworks.agilelink.framework.ViewModel;
import com.aylanetworks.agilelink.framework.ViewModelProvider;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Logger;
import com.aylanetworks.agilelink.framework.UIConfig;

import java.util.ArrayList;
import java.util.List;

/*
 * AgileLinkViewModelProvider.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 12/19/14.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class AgileLinkViewModelProvider extends ViewModelProvider {
    private final static String LOG_TAG = "AgileLinkViewModelProvider";

    public final static int ITEM_VIEW_TYPE_GENERIC_DEVICE = 0;
    public final static int ITEM_VIEW_TYPE_DEVKIT_DEVICE = 1;
    public final static int ITEM_VIEW_TYPE_SWITCHED = 2;
    public final static int ITEM_VIEW_TYPE_DIMMABLE = 3;
    public final static int ITEM_VIEW_TYPE_GENERIC_NODE_DEVICE = 5;

    /**
     * This is the default view model creator for AMAP.  Provide your own ViewModelProvider and
     * ViewModel classes to provide custom implementations tailored to your own devices
     *
     * @param device The AylaDevice object to be wrapped
     * @return Device
     */
    public ViewModel viewModelForDevice(AylaDevice device) {
        String oemModel = device.getOemModel();
        String model = device.getModel();
        if (oemModel == null) {
            Logger.logError(LOG_TAG, "No oemModel set on device: " + device);

            // in some cases, the Generic Gateway OR Zigbee Gateway has a null oemModel (instead of generic)
            if ("AY001MRT1".equals(model)) {
                // This is a gateway.
                return new GenericGateway(device);
            }
            return new GenericDevice(device);
        }

        if ( "ledevb".equals(oemModel) ) {
            // This is the Ayla devkit.
            return new AylaEVBDevice(device);
        }

        if ( "smartplug1".equals(oemModel) ) {
            // This is the Ayla Demo Smart plug
            return new SwitchedDevice(device);
        }

        if ( "EWPlug1".equals(oemModel) ) {
            // This is an Everwin MFI/Homekit enabled smart plug.
            return new SwitchedDevice(device);
        }

        // Gateway, including Zigbee, devices
        if ( "generic".equals(oemModel) ) {
            if (model.equals("AY001MRT1")) {
                // This is a generic gateway.
                return new GenericGateway(device);
            }
            if (model.equals("GenericNode") || model.equals("Generic Node")) {
                // This is a generic node
                return new GenericNodeDevice(device);
            }
        }
        if ( "GenericNode".equals(model) || "Generic Node".equals(model) ) {
            if (oemModel.equals("NexturnSmartPlug")) {
                // This is a Generic Gateway smart plug.
                return new GenericSwitchedDevice(device);
            }
            if (oemModel.equals("NexturnSmart_Bulb_Converter")) {
                // This is a Generic Gateway smart bulb.
                return new GenericLightDevice(device);
            }
            if (oemModel.equals("NexturnMotion_Sensor")) {
                // This is a Generic Gateway motion sensor.
                return new GenericMotionSensor(device);
            }
            if (model.equals("NXPZHA-DimmableLight")) {
                // This is a Generic Gateway dimmable light.
                return new GenericDimmableLightDevice(device);
            }
            // NexturnZHA-Thermostat

            // This is a generic node
            return new GenericNodeDevice(device);
        }

        //  We don't know what this is. Create a generic device.
        Logger.logError(LOG_TAG, "Could not identify this device: " + device);
        return new GenericDevice(device);
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

            case ITEM_VIEW_TYPE_GENERIC_NODE_DEVICE:
                resId = listStyle == UIConfig.ListStyle.Grid ? R.layout.cardview_switched_device_grid :
                        listStyle == UIConfig.ListStyle.ExpandingList ? R.layout.cardview_switched_device_expandable :
                                R.layout.cardview_switched_device;
                v = LayoutInflater.from(parent.getContext()).inflate(resId, parent, false);
                return new GenericNodeDeviceViewHolder(v);
        }

        return null;
    }

    /**
     * Returns a list of Device class objects that are supported by this application.
     *
     * This list is used during device registration to provide a list of Device types that the
     * user can choose from when registering a new device.
     *
     * The device classes returned from this method are instantiated using a dummy AylaDevice object
     * and queried to provide the name of the device, the preferred registration type and an image
     * representing the device. This information is gained from calling the following methods on each
     * device returned from this method:
     *
     * <ul>
     *     <li>{@link ViewModel#registrationType()}</li>
     *     <li>{@link ViewModel#deviceTypeName()}</li>
     * </ul>
     *
     * @return A list of Class objects, one for each supported Device class
     */
    public List<Class<? extends ViewModel>> getSupportedDeviceClasses() {
        List<Class<? extends ViewModel>> classList = new ArrayList<>();

        classList.add(AylaEVBDevice.class);
        classList.add(SwitchedDevice.class);
        classList.add(GenericGateway.class);
        classList.add(GenericNodeDevice.class);
        return classList;
    }
}
