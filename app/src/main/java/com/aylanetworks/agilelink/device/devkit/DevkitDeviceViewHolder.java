package com.aylanetworks.agilelink.device.devkit;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.aylanetworks.agilelink.R;

/**
 * Created by Brian King on 1/23/15.
 */
public class DevkitDeviceViewHolder extends RecyclerView.ViewHolder {
    public TextView _deviceNameTextView;
    public ImageButton _greenButton;
    public ImageButton _blueButton;
    public ImageView _buttonStateImageView;

    public DevkitDeviceViewHolder(View v) {
        super(v);

        _deviceNameTextView = (TextView)v.findViewById(R.id.device_name);
        _greenButton = (ImageButton)v.findViewById(R.id.green_button);
        _blueButton = (ImageButton)v.findViewById(R.id.blue_button);
        _buttonStateImageView = (ImageView)v.findViewById(R.id.blue_button_state_image);
    }
}
