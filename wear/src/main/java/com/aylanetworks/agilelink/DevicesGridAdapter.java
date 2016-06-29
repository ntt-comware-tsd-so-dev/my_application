package com.aylanetworks.agilelink;

import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridPagerAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

public class DevicesGridAdapter extends FragmentGridPagerAdapter {

    private ArrayList<DeviceHolder> mDevices;
    private HashMap<String, Drawable> mDeviceDrawablesMap;

    public DevicesGridAdapter(FragmentManager fm,
                              TreeMap<String, DeviceHolder> devicesMap,
                              HashMap<String, Drawable> deviceDrawables) {
        super(fm);

        mDevices = new ArrayList<>(devicesMap.values());
        mDeviceDrawablesMap = deviceDrawables;
    }

    public void updateData(TreeMap<String, DeviceHolder> devicesMap,
                           HashMap<String, Drawable> deviceDrawables) {
        if (devicesMap != null) {
            mDevices = new ArrayList<>(devicesMap.values());
        }

        if (deviceDrawables != null) {
            mDeviceDrawablesMap = deviceDrawables;
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
            arguments.putBoolean(DeviceFragment.ARG_IS_OVERVIEW_CARD, true);
        } else {
            // property card; show property switch
            arguments.putBoolean(DeviceFragment.ARG_IS_OVERVIEW_CARD, false);
            String propertyName = device.getPropertyNameOrdered(col - 1); // property starts at column 2
            arguments.putSerializable(DeviceFragment.ARG_DEVICE_PROPERTY, device.getBooleanProperty(propertyName));
        }
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public Drawable getBackgroundForRow(int row) {
        Drawable deviceDrawable = mDeviceDrawablesMap.get(mDevices.get(row).getDsn());
        if (deviceDrawable != null) {
            return deviceDrawable;
        } else {
            return super.getBackgroundForRow(row);
        }
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
        return mDevices.get(rowNum).getPropertyCount() + 1; // first column is always overview
    }
}
