package com.aylanetworks.agilelink.fragments;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;

import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.adapters.DeviceListAdapter;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.util.List;

/**
 * Created by Brian King on 2/5/15.
 */
public class DeviceGroupsFragment extends AllDevicesFragment {
    private static final String LOG_TAG = "DeviceGroupsFragment";

    public static DeviceGroupsFragment newInstance() {
        return new DeviceGroupsFragment();
    }

    public DeviceGroupsFragment() {
    }

    protected void updateDeviceList() {
        List<Device> deviceList = null;
        if ( SessionManager.deviceManager() != null ) {
            deviceList = SessionManager.deviceManager().deviceList();
        }

        if ( deviceList != null ) {
            // XXX Debug
            deviceList.remove(0);

            _adapter = new DeviceListAdapter(deviceList, this);
            _recyclerView.setAdapter(_adapter);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.add_button) {
            Log.d(LOG_TAG, "Add Group");
        } else {
            super.onClick(v);
        }
    }
}
