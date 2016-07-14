package com.aylanetworks.agilelink;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridPagerAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.TreeMap;

public class DevicesGridAdapter extends FragmentGridPagerAdapter {

    private static final Integer[] BACKGROUND_IMAGE_ID_LIST = {R.mipmap.card_bg1, R.mipmap.card_bg2, R.mipmap.card_bg4, R.mipmap.card_bg5};

    private Context mContext;
    private ArrayList<DeviceHolder> mDevices;
    private ArrayList<Integer> mBackgroundImages = new ArrayList<>(Arrays.asList(BACKGROUND_IMAGE_ID_LIST));

    public DevicesGridAdapter(Context context,
            FragmentManager fm,
            TreeMap<String, DeviceHolder> devicesMap) {
        super(fm);
        mContext = context;
        mDevices = new ArrayList<>(devicesMap.values());

        long seed = System.nanoTime();
        Collections.shuffle(mBackgroundImages, new Random(seed));
    }

    public void updateData(TreeMap<String, DeviceHolder> devicesMap) {
        mDevices = new ArrayList<>(devicesMap.values());
    }

    @Override
    public Fragment getFragment(int row, int col) {
        DeviceFragment fragment = new DeviceFragment();

        Bundle arguments = new Bundle();
        DeviceHolder device = mDevices.get(row);
        arguments.putSerializable(DeviceFragment.ARG_DEVICE_HOLDER, device);
        // overview card; show only device name
        // property card; show property list
        arguments.putBoolean(DeviceFragment.ARG_IS_OVERVIEW_CARD, col == 0);
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
        return 2; // Overview card and device properties card
    }
}
