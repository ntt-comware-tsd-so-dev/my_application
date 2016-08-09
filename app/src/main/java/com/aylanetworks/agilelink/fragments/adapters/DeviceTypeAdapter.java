package com.aylanetworks.agilelink.fragments.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.ViewModel;

/*
 * DeviceTypeAdapter.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 1/27/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */
public class DeviceTypeAdapter extends ArrayAdapter<ViewModel> {

    public boolean useProductName;

    public DeviceTypeAdapter(Context c, ViewModel[] objects) {
        super(c, R.layout.spinner_device_selection, objects);
    }

    public DeviceTypeAdapter(Context c, ViewModel[] objects, boolean productName) {
        super(c, R.layout.spinner_device_selection, objects);
        useProductName = productName;
    }


    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View spinner = inflater.inflate(R.layout.spinner_device_selection, parent, false);

        ViewModel d = getItem(position);

        ImageView iv = (ImageView)spinner.findViewById(R.id.device_image);
        iv.setImageDrawable(d.getDeviceDrawable(getContext()));

        TextView name = (TextView)spinner.findViewById(R.id.device_name);
        name.setText(useProductName ? d.getName() : d.deviceTypeName());

        return spinner;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getDropDownView(position, convertView, parent);
    }
}
