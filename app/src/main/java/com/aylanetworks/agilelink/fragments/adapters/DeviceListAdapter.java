package com.aylanetworks.agilelink.fragments.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;

import java.util.List;

/**
 * Created by Brian King on 12/30/14.
 */
public class DeviceListAdapter extends ArrayAdapter<Device>{
    private final static String LOG_TAG = "DeviceListAdapter";
    private Context _context;

    public DeviceListAdapter(Context context, List<Device> objects) {
        super(context, 0, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Device device = getItem(position);
        convertView = device.getListItemView(getContext(), convertView, parent);
        return convertView;
    }
}
