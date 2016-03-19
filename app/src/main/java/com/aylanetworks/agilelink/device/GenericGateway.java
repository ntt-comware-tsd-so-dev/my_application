package com.aylanetworks.agilelink.device;
/*
 * AMAP_Android
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.DeviceDetailFragment;
import com.aylanetworks.agilelink.framework.Gateway;
import com.aylanetworks.agilelink.framework.GenericDeviceViewHolder;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class GenericGateway extends Gateway implements DeviceUIProvider {
    public GenericGateway(AylaDevice aylaDevice) {
        super(aylaDevice);
    }

    public Drawable getDeviceDrawable(Context context) {
        return ContextCompat.getDrawable(context, R.drawable.ic_generic_gateway_red);
    }

    @Override
    public int getGridViewSpan() {
        return 1;
    }

    @Override
    public String getName() {
        return getProductName();
    }

    @Override
    public int getItemViewType() {
        return AgileLinkDeviceCreator.ITEM_VIEW_TYPE_GENERIC_DEVICE;
    }

    @Override
    public void bindViewHolder(RecyclerView.ViewHolder holder) {
        GenericDeviceViewHolder h = (GenericDeviceViewHolder) holder;
        h._deviceNameTextView.setText(getProductName());
        if (h._deviceStatusTextView != null) {
            h._deviceStatusTextView.setText(getDeviceState());
        }
        if ( !isIcon()) {
            h._spinner.setVisibility(getDevice().properties == null ? View.VISIBLE : View.GONE);
        } else {
            h._spinner.setVisibility(View.GONE);
        }

        Resources res = SessionManager.getContext().getResources();
        int color = isOnline() ? res.getColor(R.color.card_text) : res.getColor(R.color
                .disabled_text);
        if (!getDevice().amOwner()) {
            // Yes, this device is shared.
            color = res.getColor(R.color.card_shared_text);
        }
        h._deviceNameTextView.setTextColor(color);
        h._currentDevice = this;
    }

    @Override
    public Fragment getDetailsFragment() {
        return DeviceDetailFragment.newInstance(this);
    }

    @Override
    public Fragment getScheduleFragment() {
        return null;
    }

    @Override
    public Fragment getTriggerFragment() {
        return null;
    }

    @Override
    public Fragment getRemoteFragment() {
        return null;
    }

    public static List<DeviceUIProvider> fromGateways(List<Gateway> gatewayDevices) {
        List<DeviceUIProvider> genericGateways = new ArrayList<>(gatewayDevices.size());
        for (Gateway g : gatewayDevices) {
            if (g instanceof DeviceUIProvider) {
                genericGateways.add((DeviceUIProvider) g);
            }
        }
        return genericGateways;
    }

    public static class GatewayTypeAdapter extends ArrayAdapter<DeviceUIProvider> {

        public boolean useProductName;

        public GatewayTypeAdapter(Context c, DeviceUIProvider[] objects, boolean productName) {
            super(c, R.layout.spinner_device_selection, objects);
            useProductName = productName;
        }

        public GatewayTypeAdapter(Context c, Gateway[] objects, boolean productName) {
            super(c, R.layout.spinner_device_selection);
            List<DeviceUIProvider> gateways = new ArrayList<>(objects.length);
            for (Gateway g : objects) {
                if (g instanceof DeviceUIProvider) {
                    gateways.add((DeviceUIProvider) g);
                }
            }

            addAll(gateways);
            useProductName = productName;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View spinner = inflater.inflate(R.layout.spinner_device_selection, parent, false);

            DeviceUIProvider d = getItem(position);

            ImageView iv = (ImageView) spinner.findViewById(R.id.device_image);
            iv.setImageDrawable(d.getDeviceDrawable(SessionManager.getContext()));

            TextView name = (TextView) spinner.findViewById(R.id.device_name);
            name.setText(useProductName ? d.getName() : d.deviceTypeName());

            return spinner;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getDropDownView(position, convertView, parent);
        }
    }
}

