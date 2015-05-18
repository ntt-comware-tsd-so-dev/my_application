package com.aylanetworks.agilelink.framework;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;

import java.util.ArrayList;
import java.util.List;

/*
 * DeviceCreator.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 1/23/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

/**
 * The DeviceCreator object is responsible for providing the framework with a set of devices that
 * the system understands. This set of devices is used by the framework to provide a list of
 * devices that can be registered, as well as turning {@link com.aylanetworks.aaml.AylaDevice}
 * objects into {@link com.aylanetworks.agilelink.framework.Device} objects. The Agile Link
 * framework wraps each AylaDevice object with a Device object to provide additional functionality
 * within the Agile Link framework.
 *
 * Agile Link application implementers should always override the DeviceCreator object and assign
 * an instance of it to the {@link com.aylanetworks.agilelink.framework.SessionManager}'s
 * {@link com.aylanetworks.agilelink.framework.SessionManager.SessionParameters#deviceCreator}
 * variable.
 *
 * When the list of devices is fetched from the server, the framework will call the
 * {@link #deviceForAylaDevice(com.aylanetworks.aaml.AylaDevice)} method for each AylaDevice received.
 * The DeviceCreator should return exactly one Device-derived object for each AylaDevice supplied.
 *
 * If an unknown device is encountered, the DeviceCreator may return null to indicate that the device
 * should not be presented to the user, or it may return a generic Device object
 */
public class DeviceCreator {
    public final static int ITEM_VIEW_TYPE_GENERIC_DEVICE = 0;

    /**
     * Returns a Device object for the supplied AylaDevice object. Derived classes should override
     * this method to return a Device-derived object specific to the application.
     *
     * @param aylaDevice The AylaDevice object to be wrapped
     * @return a Device-derived object wrapping the supplied AylaDevice
     */
    public Device deviceForAylaDevice(AylaDevice aylaDevice) {
        return new Device(aylaDevice);
    }

    /**
     * Returns a {@link android.support.v7.widget.RecyclerView.ViewHolder} object for the supplied
     * viewType. The viewType parameter is the value returned from
     * {@link Device#getItemViewType()}. This method should return a ViewHolder used to hold
     * views for devices returning the viewType value passed in to this method.
     *
     * @param parent Parent of the new view holder
     * @param viewType Type of view this holder is meant to work with
     * @return The ViewHolder appropriate for the viewType
     */
    public RecyclerView.ViewHolder viewHolderForViewType(ViewGroup parent, int viewType) {
        int resId = MainActivity.getUIConfig()._listStyle == UIConfig.ListStyle.List ?
                R.layout.cardview_generic_device : R.layout.cardview_generic_device_grid;
        View v = LayoutInflater.from(parent.getContext()).inflate(resId, parent, false);
        return new GenericDeviceViewHolder(v);
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
     *     <li>{@link Device#registrationType()}</li>
     *     <li>{@link Device#getDeviceDrawable(android.content.Context)}</li>
     *     <li>{@link Device#deviceTypeName()}</li>
     * </ul>
     *
     * @return A list of Class objects, one for each supported Device class
     */
    public List<Class<? extends Device>> getSupportedDeviceClasses() {
        List<Class<? extends Device>> classList = new ArrayList<Class<? extends Device>>();

        classList.add(Device.class);

        return classList;
    }
}
