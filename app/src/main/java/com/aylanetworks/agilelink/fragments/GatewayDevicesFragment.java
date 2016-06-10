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
import com.aylanetworks.agilelink.framework.MenuHandler;


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
        //TODO : Brian check this if we need this method
//        List<Device> deviceList = new ArrayList<Device>();
//        if (AMAPCore.sharedInstance().getDeviceManager() != null) {
//            deviceList.addAll(AMAPCore.sharedInstance().getDeviceManager().getGatewayDevices());
//        }
//
//        if ( deviceList != null ) {
//            MainActivity.getInstance().setNoDevicesMode(false);
//            if ( _emptyView != null ) {
//                if (deviceList.isEmpty()) {
//                    _emptyView.setVisibility(View.VISIBLE);
//                    _emptyView.setText(R.string.no_gateways);
//                    _recyclerView.setVisibility(View.GONE);
//                } else {
//                    _emptyView.setVisibility(View.GONE);
//                    _recyclerView.setVisibility(View.VISIBLE);
//                    _adapter = DeviceListAdapter.fromDeviceList(deviceList, this);
//                    _recyclerView.setAdapter(_adapter);
//                }
//            }
//        }
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
