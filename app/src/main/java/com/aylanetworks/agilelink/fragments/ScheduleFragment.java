package com.aylanetworks.agilelink.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.SessionManager;

/**
 * Fragment for presenting scheduling UI
 */
public class ScheduleFragment extends Fragment implements Device.DeviceStatusListener {
    private final static String LOG_TAG = "ScheduleFragment";
    private final static String ARG_DEVICE_DSN = "deviceDSN";

    private Device _device;

    static ScheduleFragment newInstance(Device device) {
        Bundle args = new Bundle();
        args.putString(ARG_DEVICE_DSN, device.getDevice().dsn);

        ScheduleFragment frag = new ScheduleFragment();
        frag.setArguments(args);

        return frag;
    }

    public ScheduleFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Get our device argument
        _device = SessionManager.deviceManager().deviceByDSN(getArguments().getString(ARG_DEVICE_DSN));

        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_schedule, container, false);
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        MainActivity.getInstance().showWaitDialog(R.string.updating_schedule_title,
                 R.string.updating_schedule_body);
        _device.fetchSchedules(this);
    }

    @Override
    public void statusUpdated(Device device, boolean changed) {
        MainActivity.getInstance().dismissWaitDialog();
        updateUI();
    }

    private void updateUI() {
        // Make the UI reflect the schedule for this device
        Log.d(LOG_TAG, "UpdateUI: device schedule: " + _device.getSchedules());
    }
}
