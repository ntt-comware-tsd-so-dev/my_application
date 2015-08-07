package com.aylanetworks.agilelink.device;

import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.GenericDeviceViewHolder;

/**
 * Created by Raji Pillay on 8/6/15.
 */
public class ZigbeeStatusDeviceHolder extends GenericDeviceViewHolder {

    public TextView statusTextView;

    public ZigbeeStatusDeviceHolder(View itemView) {
        super(itemView);
        statusTextView = (TextView)itemView.findViewById(R.id.textview_device_status);

    }
}
