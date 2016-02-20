package com.aylanetworks.agilelink.device;
/*
 * AMAP_Android
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Gateway;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class GenericGateway extends Gateway {
    public GenericGateway(AylaDevice aylaDevice) {
        super(aylaDevice);
    }

    public Drawable getDeviceDrawable(Context context) {
        return ContextCompat.getDrawable(context, R.drawable.ic_generic_gateway_red);
    }

    public static List<GenericGateway> fromGateways(List<Gateway> gatewayDevices) {
        List<GenericGateway> genericGateways = new ArrayList<>(gatewayDevices.size());
        for (Gateway g : gatewayDevices) {
            genericGateways.add((GenericGateway)g);
        }
        return genericGateways;
    }

    public static class GatewayTypeAdapter extends ArrayAdapter<GenericGateway> {

        public boolean useProductName;

        public GatewayTypeAdapter(Context c, GenericGateway[] objects) {
            super(c, R.layout.spinner_device_selection, objects);
        }

        public GatewayTypeAdapter(Context c, GenericGateway[] objects, boolean productName) {
            super(c, R.layout.spinner_device_selection, objects);
            useProductName = productName;
        }


        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View spinner = inflater.inflate(R.layout.spinner_device_selection, parent, false);

            GenericGateway d = getItem(position);

            ImageView iv = (ImageView) spinner.findViewById(R.id.device_image);
            iv.setImageDrawable(d.getDeviceDrawable(SessionManager.getContext()));

            TextView name = (TextView) spinner.findViewById(R.id.device_name);
            name.setText(useProductName ? d.getProductName() : d.deviceTypeName());

            return spinner;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getDropDownView(position, convertView, parent);
        }
    }
}

