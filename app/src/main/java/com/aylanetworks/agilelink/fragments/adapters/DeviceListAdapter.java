package com.aylanetworks.agilelink.fragments.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.SessionManager;

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

    private List<Device> _deviceList;
    private View.OnClickListener _onClickListener;
    public DeviceListAdapter(List<Device> deviceList, View.OnClickListener listener) {
        _onClickListener = listener;
        _deviceList = deviceList;
    }

    @Override
    public int getItemViewType(int position) {
        Device d = _deviceList.get(position);
        return d.getItemViewType();
    }

    @Override
    public int getItemCount() {
        if ( _deviceList == null ) {
            return 0;
        }
        return _deviceList.size();
    }

    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return SessionManager.sessionParameters().deviceCreator.viewHolderForViewType(parent, viewType);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Device d = _deviceList.get(position);

        // Set the onClickListener for this view and set the device as the tag so we can
        // retrieve it later
        holder.itemView.setOnClickListener(_onClickListener);
        holder.itemView.setTag(d);
        d.bindViewHolder(holder);
    }
}
