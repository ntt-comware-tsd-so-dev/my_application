package com.aylanetworks.agilelink.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.deprecated.Device;
import com.aylanetworks.agilelink.framework.deprecated.SessionManager;

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

    public static DeviceDetailListFragment newInstance(Device device) {
        Bundle args = new Bundle();
        args.putString(ARG_DEVICE_DSN, device.getDeviceDsn());
        DeviceDetailListFragment frag = new DeviceDetailListFragment();
        frag.setArguments(args);
        return frag;
    }

    private Device _device;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String dsn = getArguments().getString(ARG_DEVICE_DSN);
        _device = SessionManager.deviceManager().deviceByDSN(dsn);
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
        SessionManager.SessionParameters params = SessionManager.sessionParameters();

        AylaDevice d = _device.getDevice();

        if ( !TextUtils.isEmpty(d.productName)) items.add(new AboutItem("productName", d.productName));
        if ( !TextUtils.isEmpty(d.deviceName)) items.add(new AboutItem("deviceName", d.deviceName));
        if ( !TextUtils.isEmpty(d.dsn)) items.add(new AboutItem("DSN", d.dsn));
        if ( !TextUtils.isEmpty(d.productClass)) items.add(new AboutItem("productClass", d.productClass));
        if ( !TextUtils.isEmpty(d.model)) items.add(new AboutItem("model", d.model));
        if ( !TextUtils.isEmpty(d.oemModel)) items.add(new AboutItem("oemModel", d.oemModel));
        if ( !TextUtils.isEmpty(d.connectionStatus)) items.add(new AboutItem("connectionStatus", d.connectionStatus));
        if ( !TextUtils.isEmpty(d.mac)) items.add(new AboutItem("MAC", formatMAC(d.mac)));
        if ( !TextUtils.isEmpty(d.connectedAt)) items.add(new AboutItem("connectedAt", d.connectedAt));
        if ( !TextUtils.isEmpty(d.ip)) items.add(new AboutItem("IP", d.ip));
        if ( !TextUtils.isEmpty(d.swVersion)) items.add(new AboutItem("SW Version", d.swVersion));

        if ( !TextUtils.isEmpty(d.lanIp)) items.add(new AboutItem("LAN IP", d.lanIp));

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
