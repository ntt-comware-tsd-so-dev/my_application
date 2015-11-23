/*
 * ZigbeeSatusDeviceHolder.java
 * AgileLink Application Framework
 *
 * Created by Raji Pillay on 08/06/15
 * Copyright (c) 2015 Ayla. All rights reserved.
 */
package com.aylanetworks.agilelink.device;

import android.view.View;
import android.widget.TextView;

import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.GenericDeviceViewHolder;


public class ZigbeeStatusDeviceHolder extends GenericDeviceViewHolder {

    public TextView statusTextView;

    public ZigbeeStatusDeviceHolder(View itemView) {
        super(itemView);
        statusTextView = (TextView)itemView.findViewById(R.id.textview_device_status);

    }
}
