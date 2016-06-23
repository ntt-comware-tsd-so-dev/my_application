package com.aylanetworks.agilelinkwear;

import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridPagerAdapter;

import java.util.ArrayList;

public class DevicesGridAdapter extends FragmentGridPagerAdapter {

    private ArrayList<DeviceHolder> mDevices = new ArrayList<>();

    public DevicesGridAdapter(FragmentManager fm, ArrayList<DeviceHolder> devices) {
        super(fm);

        if (devices != null) {
            mDevices = devices;
        }
    }

    @Override
    public Fragment getFragment(int row, int col) {
        DeviceFragment fragment = new DeviceFragment();

        Bundle arguments = new Bundle();
        DeviceHolder device = mDevices.get(row);
        arguments.putSerializable(DeviceFragment.ARG_DEVICE_HOLDER, device);
        if (col == 0) {
            // overview card; show only device name
            arguments.putBoolean(DeviceFragment.ARG_OVERVIEW_CARD, true);
        } else {
            // property card; show property switch
            arguments.putBoolean(DeviceFragment.ARG_OVERVIEW_CARD, false);
            String propertyName = device.getPropertyNameOrdered(col);
            arguments.putString(DeviceFragment.ARG_PROPERTY_NAME, propertyName);
        }
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public Drawable getBackgroundForPage(int row, int column) {
        return GridPagerAdapter.BACKGROUND_NONE;
    }

    @Override
    public int getRowCount() {
        return mDevices.size();
    }

    @Override
    public int getColumnCount(int rowNum) {
        return mDevices.get(rowNum).getPropertyCount();
    }
}
