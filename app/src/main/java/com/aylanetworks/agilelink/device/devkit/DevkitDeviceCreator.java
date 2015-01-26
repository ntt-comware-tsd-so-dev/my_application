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
import com.aylanetworks.agilelink.framework.SessionManager;

/**
 * Created by Brian King on 12/19/14.
 */


public class DevkitDeviceCreator extends DeviceCreator {
    private final static String LOG_TAG = "DevkitDeviceCreator";

    public final static int ITEM_VIEW_TYPE_GENERIC_DEVICE = 0;
    public final static int ITEM_VIEW_TYPE_DEVKIT_DEVICE = 1;

    public Device deviceForAylaDevice(AylaDevice aylaDevice) {

        if ( aylaDevice.oemModel.equals("ledevb") ) {
            // This is the Ayla devkit.
            return new DevkitDevice(aylaDevice);
        }

        //  We don't know what this is.
        return null;
    }

    @Override
    public RecyclerView.ViewHolder viewHolderForViewType(ViewGroup parent, int viewType) {
        View v = null;
        switch ( viewType ) {
             case ITEM_VIEW_TYPE_DEVKIT_DEVICE:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_devkit_device, parent, false);
                return new DevkitDeviceViewHolder(v);
        }

        return super.viewHolderForViewType(parent, viewType);
    }
}
