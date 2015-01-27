package com.aylanetworks.agilelink.fragments.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;

import java.util.List;

/**
 * Created by Brian King on 1/27/15.
 */
public class DeviceTypeAdapter extends ArrayAdapter<Device> {

    public DeviceTypeAdapter(Context c, Device[] objects) {
        super(c, R.layout.spinner_device_selection, objects);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View spinner = inflater.inflate(R.layout.spinner_device_selection, parent, false);

        Device d = getItem(position);

        ImageView iv = (ImageView)spinner.findViewById(R.id.device_image);
        iv.setImageDrawable(d.getDeviceDrawable(getContext()));

        TextView name = (TextView)spinner.findViewById(R.id.device_name);
        name.setText(d.deviceTypeName());

        return spinner;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getDropDownView(position, convertView, parent);
    }
}
