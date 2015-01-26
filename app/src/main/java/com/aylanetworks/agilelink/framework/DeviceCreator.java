package com.aylanetworks.agilelink.framework;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.agilelink.R;

/**
 * Created by Brian King on 1/23/15.
 */
public class DeviceCreator {

    public Device deviceForAylaDevice(AylaDevice aylaDevice) {
        return new Device(aylaDevice);
    }

    public RecyclerView.ViewHolder viewHolderForViewType(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_generic_device, parent, false);
        return new GenericDeviceViewHolder(v);
    }
}
