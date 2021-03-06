package com.aylanetworks.agilelink.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
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

import com.aylanetworks.agilelink.device.GenericDeviceViewHolder;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.ViewModel;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaDeviceManager;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaSessionManager;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.adapters.DeviceListAdapter;
import com.aylanetworks.agilelink.MenuHandler;
import com.aylanetworks.aylasdk.auth.AylaAuthorization;
import com.aylanetworks.aylasdk.auth.CachedAuthProvider;
import com.aylanetworks.aylasdk.change.Change;
import com.aylanetworks.aylasdk.change.ListChange;
import com.aylanetworks.aylasdk.error.AylaError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 * AllDevicesFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 12/30/2014.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class AllDevicesFragment extends Fragment implements
        AylaDevice.DeviceChangeListener,
        AylaDeviceManager.DeviceManagerListener,
        AylaSessionManager.SessionManagerListener,
        View.OnClickListener,
        DialogInterface.OnCancelListener {

    private final static String LOG_TAG = "AllDevicesFragment";

    /**
     * The fragment's recycler view and helpers
     */
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
        _expandedDevices = new ArrayList<>();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_aldevice, container, false);
        _emptyView = (TextView) view.findViewById(R.id.empty);

        // Set up the list view
        _recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        _recyclerView.setHasFixedSize(true);
        _recyclerView.setVisibility(View.GONE);

        _emptyView.setVisibility(View.VISIBLE);

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
                        ViewModel device = _adapter.getItem(position);
                        return device.getGridViewSpan();
                    }
                });
                _layoutManager = gm;
                break;
        }

        _recyclerView.setLayoutManager(_layoutManager);

        ImageButton b = (ImageButton) view.findViewById(R.id.add_button);
        b.setOnClickListener(this);

        updateDeviceList();
        return view;
    }

    private void addDevice() {
        // Bring up the Add Device UI
        MenuHandler.handleAddDevice();
    }

    public void updateDeviceList() {
        if (_recyclerView == null || !isAdded()) {
            // We're not ready yet
            return;
        }

        List<AylaDevice> deviceList = null;
        AylaDeviceManager deviceManager = AMAPCore.sharedInstance().getDeviceManager();
        if (deviceManager != null) {
            List<AylaDevice> all = deviceManager.getDevices();
            if (all != null) {
                deviceList = new ArrayList<>();
                for (AylaDevice d : all) {
                    if (!d.isGateway()) {
                        deviceList.add(d);
                    }
                }
            }
        }

        if ( deviceList != null ) {
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
                _adapter = DeviceListAdapter.fromDeviceList(deviceList, this);
                _recyclerView.setAdapter(_adapter);
            }
        }

        if (deviceManager != null &&
                deviceManager.getState() == AylaDeviceManager.DeviceManagerState.Ready) {
            // Devices have been added / removed since initialization
            startListening();
        }
    }

    protected void startListening() {
        AylaDeviceManager deviceManager = AMAPCore.sharedInstance().getDeviceManager();
        if (deviceManager != null) {
            deviceManager.addListener(this);
            for (AylaDevice device : deviceManager.getDevices()) {
                device.addListener(this);
            }
        }
    }

    protected void stopListening() {
        AylaDeviceManager deviceManager = AMAPCore.sharedInstance().getDeviceManager();
        if (deviceManager != null) {
            deviceManager.removeListener(this);
            for (AylaDevice device : deviceManager.getDevices()) {
                device.removeListener(this);
            }
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

        // See if we have a device manager yet
        AylaDeviceManager deviceManager = AMAPCore.sharedInstance().getDeviceManager();
        if (deviceManager != null) {
            _adapter = DeviceListAdapter.fromDeviceList(deviceManager.getDevices(), this);
            startListening();
            updateDeviceList();
        }
        // Listen for login events. Our fragment exists before the user has logged in, so we need
        // to know when that happens so we can start listening to the device manager notifications.
        if(AMAPCore.sharedInstance().getSessionManager() != null) {
            AMAPCore.sharedInstance().getSessionManager().addListener(this);
        }
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
        final ViewModel d = _adapter.getItem(itemIndex);
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
                Fragment frag = d.getDetailsFragment();
                MainActivity.getInstance().pushFragment(frag);
            }
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        //MainActivity.getInstance().dismissWaitDialog();
    }

    // Device State Listener methods

    @Override
    public void deviceChanged(AylaDevice device, Change change) {
        Log.i(LOG_TAG, "dev: device [" + device + "] changed");
        for ( int i = 0; i < _adapter.getItemCount(); i++ ) {
            ViewModel model = _adapter.getItem(i);
            if ( model.getDevice().getDsn().equals(device.getDsn())) {
                _adapter.notifyItemChanged(i);
                break;
            }
        }
    }

    @Override
    public void deviceError(AylaDevice device, AylaError error) {
        AylaLog.e(LOG_TAG, "Device " + device + " error: " + error);
    }

    @Override
    public void deviceLanStateChanged(AylaDevice device, boolean lanModeEnabled) {
        AylaLog.i(LOG_TAG, "Device " + device + " LAN enabled: " + lanModeEnabled);
        for ( int i = 0; i < _adapter.getItemCount(); i++ ) {
            ViewModel model = _adapter.getItem(i);
            if ( model.getDevice().getDsn().equals(device.getDsn())) {
                _adapter.notifyItemChanged(i);
                break;
            }
        }
    }

    // Device Manager Listener methods

    @Override
    public void deviceManagerInitComplete(Map<String, AylaError> deviceFailures) {
        AylaLog.i(LOG_TAG, "Device manager init complete, failures: " + deviceFailures);
        startListening();
    }

    @Override
    public void deviceManagerInitFailure(AylaError error,
                                         AylaDeviceManager.DeviceManagerState failureState) {
        AylaLog.e(LOG_TAG, "Device manager init failure: " + error + " in state " + failureState);
        View view = getView();
        if (view != null) {
            String message = getActivity().getString(R.string.device_manager_init_failure,
                    error.getMessage());
            final Snackbar sb = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE);
            sb.setAction(R.string.retry, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sb.dismiss();
                    AylaDeviceManager dm = AMAPCore.sharedInstance().getDeviceManager();
                    // Try again to initialize the device manager
                    dm.fetchDevices();
                }
            });
            sb.show();
        }
    }

    @Override
    public void deviceListChanged(ListChange change) {
        AylaLog.i(LOG_TAG, "Device list changed: " + change);
        startListening();
        updateDeviceList();
    }

    @Override
    public void deviceManagerError(AylaError error) {
        AylaLog.e(LOG_TAG, "Device manager error: " + error);
    }

    @Override
    public void deviceManagerStateChanged(AylaDeviceManager.DeviceManagerState oldState,
                                          AylaDeviceManager.DeviceManagerState newState) {
        AylaLog.i(LOG_TAG, "Device manager state: " + oldState + " --> " + newState);
    }

    // SessionManager listener methods

    @Override
    public void sessionClosed(String sessionName, AylaError error) {
        MainActivity instance = MainActivity.getInstance();
        //Make sure the user did not sign out normally (i.e error=null)
        if (error != null && instance != null && instance.checkFingerprintOption()) {
            MainActivity.getInstance().showFingerPrint();
        }
    }

    @Override
    public void authorizationRefreshed(String sessionName, AylaAuthorization authorization) {
        CachedAuthProvider.cacheAuthorization(MainActivity.getInstance(), authorization);
    }
}
