package com.aylanetworks.agilelink.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
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
import com.aylanetworks.agilelink.framework.GenericDeviceViewHolder;
import com.aylanetworks.agilelink.framework.Logger;
import com.aylanetworks.agilelink.framework.MenuHandler;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.util.ArrayList;
import java.util.List;

/*
 * AllDevicesFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 12/30/2014.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class AllDevicesFragment extends Fragment
    implements
        Device.DeviceStatusListener,
        DeviceManager.DeviceListListener,
        SessionManager.SessionListener,
        View.OnClickListener,
        DialogInterface.OnCancelListener {

    private final static String LOG_TAG = "AllDevicesFragment";

    /**
     * The fragment's recycler view and helpers
     */
    protected SwipeRefreshLayout _swipe;
    protected RecyclerView _recyclerView;
    protected RecyclerView.LayoutManager _layoutManager;
    protected DeviceListAdapter _adapter;
    protected TextView _emptyView;

    protected List<String> _expandedDevices;

    public static AllDevicesFragment newInstance() {
        AllDevicesFragment fragment = new AllDevicesFragment();
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AllDevicesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        _expandedDevices = new ArrayList<>();

        // See if we have a device manager yet
        DeviceManager dm = SessionManager.deviceManager();
        if (dm != null) {
            _adapter = new DeviceListAdapter(SessionManager.deviceManager().deviceList(), this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_aldevice, container, false);
        _emptyView = (TextView) view.findViewById(R.id.empty);

        // setup swipe refresh
        _swipe = (SwipeRefreshLayout)view.findViewById(R.id.swiperefresh);
        if (_swipe != null) {
            _swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    swipeRefreshStart();
                }
            });
        }

        // Set up the list view
        _recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        _recyclerView.setHasFixedSize(true);
        _recyclerView.getItemAnimator().setSupportsChangeAnimations(true);
        _recyclerView.setVisibility(View.GONE);

        _emptyView.setVisibility(View.VISIBLE);
        _emptyView.setText(R.string.fetching_devices);

        switch ( MainActivity.getUIConfig()._listStyle ) {
            case List:
            case ExpandingList:
                _layoutManager = new LinearLayoutManager(getActivity());
                break;

            case Grid:
                int nColumns = getResources().getInteger(R.integer.grid_width);
                Log.d("COLS", "Columns: " + nColumns);
                GridLayoutManager gm = new GridLayoutManager(getActivity(), nColumns);
                gm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        Device device = _adapter.getItem(position);
                        return device.getGridViewSpan();
                    }
                });
                _layoutManager = gm;
                break;
        }

        _recyclerView.setLayoutManager(_layoutManager);

        ImageButton b = (ImageButton) view.findViewById(R.id.add_button);
        b.setOnClickListener(this);

        return view;
    }

    void swipeRefreshStart() {
        if (SessionManager.deviceManager() != null) {
            SessionManager.deviceManager().refreshDeviceListWithCompletion(this, new DeviceManager.GetDevicesCompletion() {
                @Override
                public void complete(Message msg, List<Device> newDeviceList, Object tag) {
                    swipeRefreshComplete();
                }
            });
        } else {
            swipeRefreshComplete();
        }
    }

    void swipeRefreshComplete() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            _swipe.setRefreshing(false);
        } else {
            // Run on the UI thread
            try {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        _swipe.setRefreshing(false);
                    }
                });
            } catch (Exception ex) { }
        }
    }

    private void addDevice() {
        // Bring up the Add Device UI
        MenuHandler.handleAddDevice();
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

    // This method is called when the fragment is paged in to view
    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        if ( menuVisible ) {
            updateDeviceList();
        }
    }

    public void updateDeviceList() {
        boolean hasDevices = false;

        List<Device> deviceList = null;
        if (SessionManager.deviceManager() != null) {
            List<Device> all = SessionManager.deviceManager().deviceList();
            if (all != null) {
                hasDevices = !all.isEmpty();
                deviceList = new ArrayList<>();
                for (Device d : all) {
                    if (!d.isGateway()) {
                        deviceList.add(d);
                    }
                }
            }
            if (SessionManager.deviceManager().isShuttingDown()) {
                // all done.
                return;
            }
        }

        if ( deviceList != null ) {
            if (!hasDevices) {
                // Enter absolutely no devices mode
                Log.e(LOG_TAG, "Received an empty device list!");
                Thread.dumpStack();
                MainActivity.getInstance().setNoDevicesMode(true);
                return;
            }

            MainActivity.getInstance().setNoDevicesMode(false);
            if (_emptyView != null) {
                if (deviceList.isEmpty()) {
                    _emptyView.setVisibility(View.VISIBLE);
                    _emptyView.setText(R.string.no_devices);
                    _recyclerView.setVisibility(View.GONE);
                } else {
                    _emptyView.setVisibility(View.GONE);
                    _recyclerView.setVisibility(View.VISIBLE);
                }
                _adapter = new DeviceListAdapter(deviceList, this);
                _recyclerView.setAdapter(_adapter);
            }
        }
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
    public void deviceListChanged() {
        Log.i(LOG_TAG, "Device list changed");
        updateDeviceList();
    }

    @Override
    public void statusUpdated(Device device, boolean changed) {
        if ( changed ) {
            Log.i(LOG_TAG, "Device " + device.getProductName() + " changed");
            for ( int i = 0; i < _adapter.getItemCount(); i++ ) {
                Device d = _adapter.getItem(i);
                if ( d.getDeviceDsn().equals(device.getDeviceDsn())) {
                    _adapter.notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    @Override
    public void loginStateChanged(boolean loggedIn, AylaUser aylaUser) {
        Log.d(LOG_TAG, "nod: Login state changed. Logged in: " + loggedIn);
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
            handleItemClick(v);
        }
    }

    protected void handleItemClick(View v) {
        int itemIndex = (int)v.getTag();
        final Device d = _adapter.getItem(itemIndex);
        if (d != null) {
            ViewGroup expandedLayout = (ViewGroup)v.findViewById(R.id.expanded_layout);
            if ( expandedLayout != null ) {
                // We need to expand / contract the selected item
                int lastExpanded = GenericDeviceViewHolder._expandedIndex;
                if (GenericDeviceViewHolder._expandedIndex == itemIndex ) {
                    GenericDeviceViewHolder._expandedIndex = -1;
                    expandedLayout.setVisibility(View.GONE);
                } else {
                    GenericDeviceViewHolder._expandedIndex = itemIndex;
                    expandedLayout.setVisibility(View.VISIBLE);
                }
                _adapter.notifyItemChanged(itemIndex);
                if ( lastExpanded != -1 ) {
                    _adapter.notifyItemChanged(lastExpanded);
                }
            } else {
                // Put the device into LAN mode before pushing the detail fragment
                // Sometime this can take forever... so make it cancelable.
                Logger.logDebug(LOG_TAG, "lm: [" + d.getDeviceDsn() + "] connecting to device");
                MainActivity.getInstance().showWaitDialogWithCancel(getString(R.string.connecting_to_device_title), getString(R.string.connecting_to_device_body), this);
                Logger.logDebug(LOG_TAG, "lm: [" + d.getDeviceDsn() + "] enterLANMode");
                SessionManager.deviceManager().enterLANMode(new DeviceManager.LANModeListener(d) {
                    @Override
                    public void lanModeResult(boolean isInLANMode) {
                        Logger.logDebug(LOG_TAG, "lm: [" + getDevice().getDeviceDsn() + "] lanModeResult " + isInLANMode);
                        MainActivity.getInstance().dismissWaitDialog();
                        Fragment frag = d.getDetailsFragment();
                        MainActivity.getInstance().pushFragment(frag);
                        Logger.logDebug(LOG_TAG, "lm: [" + d.getDeviceDsn() + "] connected to device");
                    }
                });
            }
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        //MainActivity.getInstance().dismissWaitDialog();
    }
}
