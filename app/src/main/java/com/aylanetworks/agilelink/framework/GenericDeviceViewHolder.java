package com.aylanetworks.agilelink.framework;

import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.DeviceDetailFragment;

/*
 * GenericDeviceViewHolder.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 1/23/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class GenericDeviceViewHolder extends RecyclerView.ViewHolder {
    private static final String LOG_TAG = "GenericDeviceViewHolder";

    public Device _currentDevice;
    public TextView _deviceNameTextView;
    public TextView _deviceStatusTextView;

    public GenericDeviceViewHolder(View itemView) {
        super(itemView);

        _currentDevice = null;
        _deviceNameTextView = (TextView)itemView.findViewById(R.id.device_name);
        _deviceStatusTextView = (TextView)itemView.findViewById(R.id.device_state);
    }
}
