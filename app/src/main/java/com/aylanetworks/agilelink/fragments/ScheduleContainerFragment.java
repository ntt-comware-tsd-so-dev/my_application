package com.aylanetworks.agilelink.fragments;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.Schedule;
import com.aylanetworks.agilelink.framework.SessionManager;

/*
 * ScheduleContainerFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 2/17/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class ScheduleContainerFragment extends Fragment {
    private final static String ARG_DEVICE_DSN = "deviceDSN";

    private ViewPager _pager;
    private Device _device;
    private SchedulePagerAdapter _adapter;

    public ScheduleContainerFragment() {
        // Required empty public constructor
    }

    public static ScheduleContainerFragment newInstance(Device device) {
        ScheduleContainerFragment frag = new ScheduleContainerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DEVICE_DSN, device.getDevice().dsn);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_schedule_container, container, false);

        _pager = (ViewPager)root.findViewById(R.id.pager);
        _device = SessionManager.deviceManager().deviceByDSN(getArguments().getString(ARG_DEVICE_DSN));
        _device.fetchSchedules(new Device.DeviceStatusListener() {
            @Override
            public void statusUpdated(Device device, boolean changed) {
                MainActivity.getInstance().dismissWaitDialog();
                onDeviceUpdated();
            }
        });

        MainActivity.getInstance().showWaitDialog(R.string.updating_schedule_title, R.string.updating_schedule_body);

        return root;
    }

    private void onDeviceUpdated() {
        _adapter = new SchedulePagerAdapter(getChildFragmentManager());
        _pager.setAdapter(_adapter);
        _adapter.notifyDataSetChanged();
    }

    public static class EmptyFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            TextView tv = new TextView(getActivity());
            tv.setText(R.string.no_schedules_found);
            return tv;
        }
    }

    public class SchedulePagerAdapter extends FragmentPagerAdapter {

        public SchedulePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if ( _device.getSchedules().size() == 0 ) {
                return new EmptyFragment();
            }

            Schedule schedule = _device.getSchedules().get(position);
            ScheduleFragment frag = ScheduleFragment.newInstance(_device, position);
            return frag;
        }

        @Override
        public int getCount() {
            int count = _device.getSchedules().size();
            if ( count == 0 ) {
                return 1;
            }

            return _device.getSchedules().size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if ( _device.getSchedules().size() == 0 ) {
                return getString(R.string.no_schedules_found);
            }

            return _device.getSchedules().get(position).getName();
        }
    }
}
