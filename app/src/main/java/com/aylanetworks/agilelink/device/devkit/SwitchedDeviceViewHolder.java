package com.aylanetworks.agilelink.device.devkit;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.aylanetworks.agilelink.R;

/**
 * Created by Brian King on 2/5/15.
 */
public class SwitchedDeviceViewHolder extends RecyclerView.ViewHolder {
    public ImageButton _switchButton;
    public TextView _deviceNameTextView;

    public SwitchedDeviceViewHolder(View view) {
        super(view);
        _switchButton = (ImageButton)view.findViewById(R.id.power_button);
        _deviceNameTextView = (TextView)view.findViewById(R.id.device_name);
    }
}
