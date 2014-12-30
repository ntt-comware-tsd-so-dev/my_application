package com.aylanetworks.agilelink.fragments.adapters;

import android.content.Context;
import android.widget.ArrayAdapter;

import com.aylanetworks.agilelink.framework.Device;

import java.util.List;

/**
 * Created by Brian King on 12/30/14.
 */
public class DeviceListAdapter extends ArrayAdapter<Device>{
    private final static String LOG_TAG = "DeviceListAdapter";
    private Context _context;

    public DeviceListAdapter(Context context, List<Device> objects) {
        super(context, android.R.layout.simple_list_item_1, android.R.id.text1, objects);
    }
}
