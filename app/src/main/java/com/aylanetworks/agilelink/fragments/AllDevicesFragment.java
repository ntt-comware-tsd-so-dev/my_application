package com.aylanetworks.agilelink.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;

import com.aylanetworks.agilelink.fragments.adapters.DeviceListAdapter;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.DeviceManager;
import com.aylanetworks.agilelink.framework.MenuHandler;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.util.List;

/*
 * AllDevicesFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 12/30/2014.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class AllDevicesFragment extends Fragment
        implements Device.DeviceStatusListener,
        DeviceManager.DeviceListListener, SessionManager.SessionListener, View.OnClickListener {

    private final static String LOG_TAG = "AllDevicesFragment";

    public final static int DISPLAY_MODE_LIST = 0;
    public final static int DISPLAY_MODE_GRID = 1;

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    protected static final String ARG_DISPLAY_MODE = "display_mode";

    /**
     * Mode we should display
     */
    protected int _displayMode = DISPLAY_MODE_LIST;

    /**
     * The fragment's recycler view and helpers
     */
    protected RecyclerView _recyclerView;
    protected RecyclerView.LayoutManager _layoutManager;
    protected DeviceListAdapter _adapter;
    protected TextView _emptyView;

    public static AllDevicesFragment newInstance(int displayMode) {
        AllDevicesFragment fragment = new AllDevicesFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_DISPLAY_MODE, displayMode);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AllDevicesFragment() {
    }

    private void addDevice() {
        Log.i(LOG_TAG, "Add Device called");

        // Bring up the Add Device UI
        AddDeviceFragment frag = AddDeviceFragment.newInstance();
        MainActivity.getInstance().pushFragment(frag);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_all_devices, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add_device) {
            addDevice();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (getArguments() != null) {
            _displayMode = getArguments().getInt(ARG_DISPLAY_MODE);
        }

        // See if we have a device manager yet
        DeviceManager dm = SessionManager.deviceManager();
        if (dm != null) {
            _adapter = new DeviceListAdapter(SessionManager.deviceManager().deviceList(), this);
        }
    }

    // This method is called when the fragment is paged in to view
    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        if ( menuVisible ) {
            updateDeviceList();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_aldevice, container, false);
        _emptyView = (TextView) view.findViewById(R.id.empty);

        // Set up the list view

        _recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        _recyclerView.setHasFixedSize(true);

        _recyclerView.setVisibility(View.GONE);
        _emptyView.setVisibility(View.VISIBLE);
        _emptyView.setText(R.string.fetching_devices);

        if ( _displayMode == DISPLAY_MODE_LIST ) {
            _layoutManager = new LinearLayoutManager(getActivity());
        } else {
            GridLayoutManager gm = new GridLayoutManager(getActivity(), 2);
            gm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    Device device = _adapter.getItem(position);
                    return device.getGridViewSpan();
                }
            });
            _layoutManager = gm;
        }

        _recyclerView.setLayoutManager(_layoutManager);

        ImageButton b = (ImageButton) view.findViewById(R.id.add_button);
        b.setOnClickListener(this);

        return view;
    }

    protected void startListening() {
        SessionManager.addSessionListener(this);

        DeviceManager deviceManager = SessionManager.deviceManager();
        if (deviceManager != null) {
            SessionManager.deviceManager().addDeviceListListener(this);
            SessionManager.deviceManager().addDeviceStatusListener(this);
        }
    }

    protected void stopListening() {
        SessionManager.removeSessionListener(this);

        DeviceManager deviceManager = SessionManager.deviceManager();
        if (deviceManager != null) {
            SessionManager.deviceManager().removeDeviceListListener(this);
            SessionManager.deviceManager().removeDeviceStatusListener(this);
        }
    }

    protected void updateDeviceList() {
        List<Device> deviceList = null;
        if ( SessionManager.deviceManager() != null ) {
            deviceList = SessionManager.deviceManager().deviceList();
        }

        if ( deviceList != null ) {
            if ( deviceList.isEmpty() ) {
                // Bring up the wifi setup page
                MenuHandler.handleWiFiSetup();
                return;
            }
            _emptyView.setVisibility(View.GONE);
            _recyclerView.setVisibility(View.VISIBLE);

            _adapter = new DeviceListAdapter(deviceList, this);
            _recyclerView.setAdapter(_adapter);
        }
    }

     @Override
    public void onPause() {
        super.onPause();
        stopListening();
    }

    @Override
    public void onResume() {
        super.onResume();
        startListening();
        deviceListChanged();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        startListening();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        stopListening();
    }

    @Override
    public void deviceListChanged() {
        Log.i(LOG_TAG, "Device list changed");
        updateDeviceList();
    }

    @Override
    public void statusUpdated(Device device, boolean changed) {
        if ( changed ) {
            Log.i(LOG_TAG, "Device " + device.getDevice().productName + " changed");
            _recyclerView.setAdapter(_adapter);
        }
    }

    @Override
    public void loginStateChanged(boolean loggedIn, AylaUser aylaUser) {
        DeviceManager deviceManager = SessionManager.deviceManager();
        if (deviceManager != null) {
            if (loggedIn) {
                SessionManager.deviceManager().addDeviceListListener(this);
                SessionManager.deviceManager().addDeviceStatusListener(this);
            } else {
                // Logged out
                SessionManager.deviceManager().removeDeviceListListener(this);
                SessionManager.deviceManager().removeDeviceStatusListener(this);
            }
        }
    }

    @Override
    public void reachabilityChanged(int reachabilityState) {
        Log.v(LOG_TAG, "Reachability changed: " + reachabilityState);
    }

    @Override
    public void lanModeChanged(boolean lanModeEnabled) {
        Log.v(LOG_TAG, "lanModeChanged: " + (lanModeEnabled ? "ENABLED" : "DISABLED"));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.add_button) {
            addDevice();
        } else {
            // This is a click from an item in the list.
            Device d = (Device) v.getTag();
            if (d != null) {
                // Put the device into LAN mode before pushing the detail fragment
                MainActivity.getInstance().showWaitDialog(R.string.connecting_to_device_title, R.string.connecting_to_device_body);
                SessionManager.deviceManager().enterLANMode(new DeviceManager.LANModeListener(d) {
                    @Override
                    public void lanModeResult(boolean isInLANMode) {
                        MainActivity.getInstance().dismissWaitDialog();
                        Log.d(LOG_TAG, "Pushing details page, lanModeResult for " + getDevice() + ": " + isInLANMode);
                        DeviceDetailFragment frag = DeviceDetailFragment.newInstance(getDevice());
                        MainActivity.getInstance().pushFragment(frag);
                    }
                });
            }
        }
    }
}
