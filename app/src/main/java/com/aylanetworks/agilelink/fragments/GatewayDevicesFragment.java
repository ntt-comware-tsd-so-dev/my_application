package com.aylanetworks.agilelink.fragments;
/* 
 * GatewayDevicesFragment.java
 * AgileLink Application Framework
 *
 * Created by David N. Junod on 7/15/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

import android.view.View;

import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.MenuHandler;
import com.aylanetworks.agilelink.fragments.adapters.DeviceListAdapter;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaDeviceGateway;

import java.util.ArrayList;
import java.util.List;

public class GatewayDevicesFragment extends AllDevicesFragment {

    public static GatewayDevicesFragment newInstance() {
        GatewayDevicesFragment fragment = new GatewayDevicesFragment();
        return fragment;
    }

    public GatewayDevicesFragment() { }

    @Override
    public void updateDeviceList() {
        List<AylaDevice> gatewaysList = new ArrayList<>();
        if (AMAPCore.sharedInstance().getDeviceManager() != null) {
            List<AylaDeviceGateway> gateways = AMAPCore.sharedInstance().getDeviceManager().getGateways();
            for (AylaDevice device : gateways) {
                if (device.isGateway()) {
                    gatewaysList.add(device);
                }
            }
        }

        MainActivity.getInstance().setNoDevicesMode(false);

        if (_emptyView != null) {
            if (gatewaysList.isEmpty()) {
                _emptyView.setVisibility(View.VISIBLE);
                _emptyView.setText(R.string.no_gateways);
                _recyclerView.setVisibility(View.GONE);
            } else {
                _emptyView.setVisibility(View.GONE);
                _recyclerView.setVisibility(View.VISIBLE);
                _adapter = DeviceListAdapter.fromDeviceList(gatewaysList, this);
                _recyclerView.setAdapter(_adapter);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.add_button) {
            MenuHandler.handleGatewayWelcome();
        } else {
            super.onClick(v);
        }
    }
}
