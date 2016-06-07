package com.aylanetworks.agilelink.fragments.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.aylanetworks.agilelink.device.AgileLinkViewModelProvider;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.ViewModel;
import com.aylanetworks.agilelink.framework.ViewModelProvider;
import com.aylanetworks.aylasdk.AylaDevice;

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

    List<ViewModel> _deviceList;
    View.OnClickListener _onClickListener;

    public DeviceListAdapter(List<ViewModel> deviceList, View.OnClickListener listener) {
        _onClickListener = listener;
        _deviceList = deviceList;
    }

    public static DeviceListAdapter fromDeviceList(List<AylaDevice>deviceList,
                                                   View.OnClickListener listener) {
        List<ViewModel> viewModels;
        ViewModelProvider provider = AMAPCore.sharedInstance().getSessionParameters()
                .viewModelProvider;

        if (deviceList != null) {
            viewModels = new ArrayList<>(deviceList.size());
            for (AylaDevice d : deviceList) {
                ViewModel model = provider.viewModelForDevice(d);
                if (model != null) {
                    viewModels.add(model);
                }
            }
        } else {
            viewModels = new ArrayList<>(0);
        }

        return new DeviceListAdapter(viewModels, listener);
    }

    @Override
    public int getItemViewType(int position) {
        ViewModel d = _deviceList.get(position);

        return d.getItemViewType();
    }

    @Override
    public int getItemCount() {
        if ( _deviceList == null ) {
            return 0;
        }
        return _deviceList.size();
    }

    public ViewModel getItem(int index) {
        return _deviceList.get(index);
    }

    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        AgileLinkViewModelProvider dc = (AgileLinkViewModelProvider)AMAPCore.sharedInstance()
                .getSessionParameters().viewModelProvider;
        return dc.viewHolderForViewType(parent, viewType);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ViewModel d = _deviceList.get(position);

        // Set the onClickListener for this view and set the index as the tag so we can
        // retrieve it later
        holder.itemView.setOnClickListener(_onClickListener);
        holder.itemView.setTag(position);
        d.bindViewHolder(holder);
    }
}
