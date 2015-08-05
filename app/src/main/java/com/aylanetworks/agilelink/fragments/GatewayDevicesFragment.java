package com.aylanetworks.agilelink.fragments;
/* 
 * GatewayDevicesFragment.java
 * AgileLink Application Framework
 *
 * Created by David N. Junod on 7/15/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

import android.view.View;

import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.fragments.adapters.DeviceListAdapter;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class GatewayDevicesFragment extends AllDevicesFragment {

    private final static String LOG_TAG = "GatewayDevicesFragment";

    public static GatewayDevicesFragment newInstance() {
        GatewayDevicesFragment fragment = new GatewayDevicesFragment();
        return fragment;
    }

    public GatewayDevicesFragment() { }

    @Override
    public void updateDeviceList() {
        List<Device> deviceList = new ArrayList<Device>();
        if (SessionManager.deviceManager() != null) {
            deviceList.addAll(SessionManager.deviceManager().getGatewayDevices());
        }

        if ( deviceList != null ) {
            MainActivity.getInstance().setNoDevicesMode(false);
            if ( _emptyView != null ) {
                if (deviceList.isEmpty()) {
                    _emptyView.setVisibility(View.VISIBLE);
                    _emptyView.setText(R.string.no_gateways);
                    _recyclerView.setVisibility(View.GONE);
                } else {
                    _emptyView.setVisibility(View.GONE);
                    _recyclerView.setVisibility(View.VISIBLE);
                    _adapter = new DeviceListAdapter(deviceList, this);
                    _recyclerView.setAdapter(_adapter);
                }
            }
        }
    }
}
