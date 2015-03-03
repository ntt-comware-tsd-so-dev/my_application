package com.aylanetworks.agilelink.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaHostScanResults;
import com.aylanetworks.aaml.AylaModule;
import com.aylanetworks.aaml.AylaModuleScanResults;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaSetup;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.adapters.ScanResultsAdapter;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/*
 * WiFiSetupFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 1/28/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class WiFiSetupFragment extends Fragment implements View.OnClickListener,
        AdapterView.OnItemClickListener, ChooseAPDialog.ChooseAPResults {
    private static final String LOG_TAG = "WiFiSetupFragment";
    private static final String LAN_SECURITY_NONE = "None";

    private ListView _listView;

    public static WiFiSetupFragment newInstance() {
        return new WiFiSetupFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_wifi_setup, container, false);
        _listView = (ListView)v.findViewById(R.id.listView);

        TextView emptyTextView = (TextView)v.findViewById(android.R.id.empty);
        _listView.setEmptyView(emptyTextView);
        _listView.setOnItemClickListener(this);

        Button b = (Button)v.findViewById(R.id.scan_button);
        b.setOnClickListener(this);

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Simulate a button click so we start the scan for devices right away
        onClick(null);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        AylaSetup.exit();
    }

    private AylaHostScanResults _scanResults[];

    static class DeviceScanHandler extends Handler {
        private WeakReference<WiFiSetupFragment> _wiFiSetupFragment;

        public DeviceScanHandler(WiFiSetupFragment wiFiSetupFragment) {
            _wiFiSetupFragment = new WeakReference<WiFiSetupFragment>(wiFiSetupFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "Got scan results: " + msg);
            MainActivity.getInstance().dismissWaitDialog();

            if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
                TextView tv = (TextView)_wiFiSetupFragment.get()._listView.getEmptyView();
                tv.setText(R.string.no_devices_found);

                String json = (String)msg.obj;
                _wiFiSetupFragment.get()._scanResults = AylaSystemUtils.gson.fromJson(json, AylaHostScanResults[].class);
                ScanResultsAdapter adapter = new ScanResultsAdapter(_wiFiSetupFragment.get().getActivity(), _wiFiSetupFragment.get()._scanResults);
                _wiFiSetupFragment.get()._listView.setAdapter(adapter);
            } else {
                String errMsg = (String)msg.obj;
                if ( errMsg == null || errMsg.contains("DISABLED") ) {
                    errMsg = _wiFiSetupFragment.get().getString(R.string.error_wifi_not_enabled);
                }
                Toast.makeText(_wiFiSetupFragment.get().getActivity(), errMsg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private DeviceScanHandler _deviceScanHandler = new DeviceScanHandler(this);

    @Override
    public void onDestroy() {
        super.onDestroy();
        AylaSetup.exit();
    }

    @Override
    public void onClick(View v) {
        Log.i(LOG_TAG, "Scan clicked");

        // Put up a progress dialog
        MainActivity.getInstance().showWaitDialog(getString(R.string.scanning_for_devices_title),
                getString(R.string.scanning_for_devices_message));

        AylaSetup.returnHostScanForNewDevices(_deviceScanHandler);
    }

    /**
     * Handler called when attempting to connect to the device's AP
     */
    static class ConnectHandler extends Handler {
        private WeakReference<WiFiSetupFragment> _fragment;

        public ConnectHandler(WiFiSetupFragment frag) {
            _fragment = new WeakReference<WiFiSetupFragment>(frag);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "Connect handler: " + msg);
            MainActivity activity = MainActivity.getInstance();
            activity.dismissWaitDialog();

            if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
                String json = (String)msg.obj;
                AylaSetup.newDevice = AylaSystemUtils.gson.fromJson(json, AylaModule.class);
                AylaSetup.getNewDeviceScanForAPs(new ScanForAPsHandler(_fragment.get()));
                activity.showWaitDialog(activity.getString(R.string.scanning_for_aps_title),
                        activity.getString(R.string.scanning_for_aps_body));
            } else {
                Log.e(LOG_TAG, "Connect handler error: " + msg);
                Toast.makeText(activity, R.string.wifi_connect_failed, Toast.LENGTH_LONG).show();
                AylaSetup.exit();
            }
        }
    };

    /**
     * Handler called with the results for the AP scan (scan for devices)
     */
    private AylaModuleScanResults _apScanResults[];
    static class ScanForAPsHandler extends Handler {
        private WeakReference<WiFiSetupFragment> _fragment;

        public ScanForAPsHandler(WiFiSetupFragment fragment) {
            _fragment = new WeakReference<WiFiSetupFragment>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity.getInstance().dismissWaitDialog();
            Log.d(LOG_TAG, "Scan for APs handler: " + msg);
            if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
                String json = (String)msg.obj;
                _fragment.get()._apScanResults = AylaSystemUtils.gson.fromJson(json, AylaModuleScanResults[].class);
                ChooseAPDialog d = ChooseAPDialog.newInstance(_fragment.get()._apScanResults);
                d.setTargetFragment(_fragment.get(), 0);
                d.show(MainActivity.getInstance().getSupportFragmentManager(), "ap");
            }
        }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ScanResultsAdapter adapter = (ScanResultsAdapter)parent.getAdapter();
        AylaHostScanResults result = adapter.getItem(position);
        Log.d(LOG_TAG, "Item click: " + result);

        AylaSetup.newDevice.hostScanResults = result;
        AylaSetup.lanSsid = result.ssid;
        AylaSetup.lanSecurityType = result.keyMgmt;

        // Connect to the device
        MainActivity.getInstance().showWaitDialog(getString(R.string.connecting_to_device_title),
                getString(R.string.connecting_to_device_body));
        AylaSetup.connectToNewDevice(new ConnectHandler(this));
    }

    static class ConnectToServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "Connect to service handler: " + msg);
            MainActivity.getInstance().dismissWaitDialog();
            AylaSetup.exit();
            if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
                Toast.makeText(MainActivity.getInstance(), R.string.connect_to_service_success, Toast.LENGTH_SHORT).show();
                MainActivity.getInstance().getSupportFragmentManager().popBackStack();
            } else {
                // Check for invalid key present in the error message
                String emsg = (String) msg.obj;
                if (emsg.contains("invalid key")) {
                    Toast.makeText(MainActivity.getInstance(), R.string.bad_wifi_password, Toast.LENGTH_LONG).show();
                } else {
                    String anErrMsg = (String) msg.obj;
                    Toast.makeText(MainActivity.getInstance(), anErrMsg, Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    private void connectDeviceToService(String ssid, String security, String password) {
        AylaSetup.lanSsid = ssid;
        AylaSetup.lanPassword = password;
        AylaSetup.lanSecurityType = security;

        AylaSetup.connectNewDeviceToService(new ConnectToServiceHandler());
        MainActivity.getInstance().showWaitDialog(getString(R.string.connecting_to_network_title),
                getString(R.string.connecting_to_network_body));
    }

    @Override
    public void choseAccessPoint(String accessPoint, String security, String password) {
        Log.d(LOG_TAG, "choseAccessPoint: " + accessPoint + "[" + security + "]");
        if ( accessPoint == null ) {
            AylaSetup.exit();
        } else {
            connectDeviceToService(accessPoint, security, password);
        }
    }
}
