package com.aylanetworks.agilelink;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridPagerAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.TreeMap;

public class DevicesGridAdapter extends FragmentGridPagerAdapter {

    // TODO: Replace R.mipmap.card_bg3 because the current one is ugly
    private static final Integer[] BACKGROUND_IMAGE_ID_LIST = {R.mipmap.card_bg1, R.mipmap.card_bg2, R.mipmap.card_bg4, R.mipmap.card_bg5};

    private ArrayList<DeviceHolder> mDevices;
    private HashMap<String, Bitmap> mDeviceDrawablesMap;
    private Context mContext;
    private ArrayList<Integer> mBackgroundImages = new ArrayList<>(Arrays.asList(BACKGROUND_IMAGE_ID_LIST));

    public DevicesGridAdapter(Context context,
            FragmentManager fm,
            TreeMap<String, DeviceHolder> devicesMap,
            HashMap<String, Bitmap> deviceDrawables) {
        super(fm);
        mContext = context;
        mDevices = new ArrayList<>(devicesMap.values());
        mDeviceDrawablesMap = deviceDrawables;

        long seed = System.nanoTime();
        Collections.shuffle(mBackgroundImages, new Random(seed));
    }

    public void updateData(TreeMap<String, DeviceHolder> devicesMap,
                           HashMap<String, Bitmap> deviceDrawables) {
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
            Bitmap deviceDrawable = mDeviceDrawablesMap.get(mDevices.get(row).getDsn());
            arguments.putParcelable(DeviceFragment.ARG_DEVICE_DRAWABLE, deviceDrawable);
        } else {
            // property card; show property switch
            arguments.putBoolean(DeviceFragment.ARG_IS_OVERVIEW_CARD, false);
            String propertyName = device.getPropertyNameOrdered(col - 1); // property starts at column 2
            arguments.putString(DeviceFragment.ARG_DEVICE_PROPERTY, propertyName);
        }
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public Drawable getBackgroundForRow(int row) {
        return mContext.getResources().getDrawable(mBackgroundImages.get(row % mBackgroundImages.size()), null);
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
