package com.aylanetworks.agilelink.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.agilelink.R;

import java.util.ArrayList;
import java.util.List;

/**
 * DeviceDetailListFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 4/29/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */
public class DeviceDetailListFragment extends AboutFragment {

    private static String ARG_DEVICE_DSN = "device_dsn";

    public static DeviceDetailListFragment newInstance(AylaDevice device) {
        Bundle args = new Bundle();
        args.putString(ARG_DEVICE_DSN, device.getDsn());
        DeviceDetailListFragment frag = new DeviceDetailListFragment();
        frag.setArguments(args);
        return frag;
    }

    private AylaDevice _device;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String dsn = getArguments().getString(ARG_DEVICE_DSN);
        _device = AMAPCore.sharedInstance().getDeviceManager().deviceWithDSN(dsn);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        String headerText = getResources().getString(R.string.device_detail_header_text, _device.toString());
        _headerTextView.setText(headerText);

        return root;
    }

    @Override
    protected void populateList() {
        List<AboutItem> items = new ArrayList<>();
        AMAPCore.SessionParameters params = AMAPCore.sharedInstance().getSessionParameters();

        if ( !TextUtils.isEmpty(_device.getProductName())) items.add(new AboutItem("productName", _device.getProductName()));
        if ( !TextUtils.isEmpty(_device.getDeviceName())) items.add(new AboutItem("deviceName", _device.getDeviceName()));
        if ( !TextUtils.isEmpty(_device.getDsn())) items.add(new AboutItem("DSN", _device.getDsn()));
        if ( !TextUtils.isEmpty(_device.getProductClass())) items.add(new AboutItem("productClass", _device.getProductClass()));
        if ( !TextUtils.isEmpty(_device.getModel())) items.add(new AboutItem("model", _device.getModel()));
        if ( !TextUtils.isEmpty(_device.getOemModel())) items.add(new AboutItem("oemModel", _device.getOemModel()));
        if ( !TextUtils.isEmpty(_device.getConnectionStatus().toString())) items.add(new AboutItem("connectionStatus", _device.getConnectionStatus().toString()));
        if ( !TextUtils.isEmpty(_device.getMac())) items.add(new AboutItem("MAC", formatMAC(_device.getMac())));
        if ( !TextUtils.isEmpty(_device.getConnectedAt().toString())) items.add(new AboutItem("connectedAt", _device.getConnectedAt().toString()));
        if ( !TextUtils.isEmpty(_device.getIp())) items.add(new AboutItem("IP", _device.getIp()));
        if ( !TextUtils.isEmpty(_device.getSwVersion())) items.add(new AboutItem("SW Version", _device.getSwVersion()));

        if ( !TextUtils.isEmpty(_device.getLanIp())) items.add(new AboutItem("LAN IP", _device.getLanIp()));

        _listView.setAdapter(new AboutListAdapter(getActivity(), items.toArray(new AboutItem[items.size()])));
    }

    private String formatMAC(String mac) {
        if (mac.contains(":")) {
            // Already formatted
            return mac;
        }

        int start = 0;
        if ( mac.startsWith("0x") || mac.startsWith("0X") ) {
            start = 2;
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int i = start; i <= mac.length() - 2; i+=2 ) {
            if ( !first ) {
                sb.append(":");
            } else {
                first = false;
            }
            sb.append(mac.substring(i, i+2));
        }
        return sb.toString();
    }
}
