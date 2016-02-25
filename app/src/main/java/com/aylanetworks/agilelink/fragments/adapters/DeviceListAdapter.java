package com.aylanetworks.agilelink.fragments.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.aylanetworks.agilelink.device.AgileLinkDeviceCreator;
import com.aylanetworks.agilelink.device.GenericDevice;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.util.ArrayList;
import java.util.List;

/*
 * DeviceListAdapter.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 12/30/14.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */


/**
 * Default list adapter for displaying devices. Used by the AllDevicesFragment.
 */
public class DeviceListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final static String LOG_TAG = "DeviceListAdapter";

    List<GenericDevice> _deviceList;
    View.OnClickListener _onClickListener;

    public DeviceListAdapter(List<GenericDevice> deviceList, View.OnClickListener listener) {
        _onClickListener = listener;
        _deviceList = deviceList;
    }

    public static DeviceListAdapter fromDeviceList(List<Device>deviceList,
                                                   View.OnClickListener listener) {
        List<GenericDevice> genericDevices = new ArrayList<>(deviceList.size());
        for (Device d : deviceList) {
            genericDevices.add((GenericDevice)d);
        }
        return new DeviceListAdapter(genericDevices, listener);
    }

    @Override
    public int getItemViewType(int position) {
        GenericDevice d = _deviceList.get(position);
        return d.getItemViewType();
    }

    @Override
    public int getItemCount() {
        if ( _deviceList == null ) {
            return 0;
        }
        return _deviceList.size();
    }

    public GenericDevice getItem(int index) {
        return _deviceList.get(index);
    }

    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        AgileLinkDeviceCreator dc = (AgileLinkDeviceCreator)SessionManager.sessionParameters()
                .deviceCreator;
        return dc.viewHolderForViewType(parent, viewType);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        GenericDevice d = _deviceList.get(position);

        // Set the onClickListener for this view and set the index as the tag so we can
        // retrieve it later
        holder.itemView.setOnClickListener(_onClickListener);
        holder.itemView.setTag(position);
        d.bindViewHolder(holder);
    }
}
