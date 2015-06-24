package com.aylanetworks.agilelink.framework;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.aylanetworks.aaml.zigbee.AylaSceneZigbeeNodeEntity;
import com.aylanetworks.agilelink.R;

/*
 * GenericDeviceViewHolder.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 1/23/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class GenericDeviceViewHolder extends RecyclerView.ViewHolder {
    private static final String LOG_TAG = "GenericDeviceViewHolder";

    public Device _currentDevice;
    public AylaSceneZigbeeNodeEntity _sceneDeviceEntity;
    public TextView _deviceNameTextView;
    public TextView _deviceStatusTextView;
    public ProgressBar _spinner;
    public ViewGroup _expandedLayout;
    public Button _scheduleButton;
    public Button _notificationsButton;
    public ImageButton _detailsButton;

    public static int _expandedIndex = -1;

    public GenericDeviceViewHolder(View itemView) {
        super(itemView);

        _currentDevice = null;
        _deviceNameTextView = (TextView)itemView.findViewById(R.id.device_name);
        _deviceStatusTextView = (TextView)itemView.findViewById(R.id.device_state);
        _spinner = (ProgressBar)itemView.findViewById(R.id.spinner);
        _expandedLayout = (ViewGroup)itemView.findViewById(R.id.expanded_layout);
        if ( _expandedLayout != null ) {
            // We have an expanded layout with some extra controls
            _scheduleButton = (Button)itemView.findViewById(R.id.schedule_button);
            _notificationsButton = (Button)itemView.findViewById(R.id.notifications_button);
            _detailsButton = (ImageButton)itemView.findViewById(R.id.details_button);
        }
    }
}
