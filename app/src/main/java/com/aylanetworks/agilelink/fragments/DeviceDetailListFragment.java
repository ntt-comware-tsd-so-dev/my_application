package com.aylanetworks.agilelink.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.SessionManager;

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
        args.putString(ARG_DEVICE_DSN, device.getDevice().dsn);
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

        items.add(new AboutItem("productName", d.productName));
        items.add(new AboutItem("deviceName", d.deviceName));
        items.add(new AboutItem("DSN", d.dsn));
        items.add(new AboutItem("productClass", d.productClass));
        items.add(new AboutItem("model", d.model));
        items.add(new AboutItem("oemModel", d.oemModel));
        items.add(new AboutItem("connectionStatus", d.connectionStatus));
        items.add(new AboutItem("MAC", formatMAC(d.mac)));
        items.add(new AboutItem("connectedAt", d.connectedAt));
        items.add(new AboutItem("IP", d.ip));
        items.add(new AboutItem("LAN IP", d.lanIp));
        items.add(new AboutItem("key", d.getKey().toString()));

        _listView.setAdapter(new AboutListAdapter(getActivity(), items.toArray(new AboutItem[items.size()])));
    }

    private String formatMAC(String mac) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < mac.length() - 2; i++ ) {
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
