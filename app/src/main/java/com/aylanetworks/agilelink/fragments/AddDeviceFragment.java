package com.aylanetworks.agilelink.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaHostScanResults;
import com.aylanetworks.aaml.AylaModule;
import com.aylanetworks.aaml.AylaModuleScanResults;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaSetup;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.adapters.DeviceTypeAdapter;
import com.aylanetworks.agilelink.fragments.adapters.ScanResultsAdapter;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.DeviceNotificationHelper;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/*
 * AddDeviceFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 1/21/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class AddDeviceFragment extends Fragment
        implements AdapterView.OnItemSelectedListener, View.OnClickListener,
        ChooseAPDialog.ChooseAPResults {
    private static final String LOG_TAG = "AddDeviceFragment";

    private static final int REG_TYPE_SAME_LAN = 0;
    private static final int REG_TYPE_BUTTON_PUSH = 1;
    private static final int REG_TYPE_DISPLAY = 2;

    /**
     * Default instance creator class method
     *
     * @return A new AddDeviceFragment ready for user interaction
     */
    public static AddDeviceFragment newInstance() {
        return new AddDeviceFragment();
    }

    private TextView _descriptionTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        SessionManager.deviceManager().stopPolling();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        SessionManager.deviceManager().startPolling();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_device, container, false);

        // Set up an onTouchListener to our root view so touches are not passed through to
        // fragments below us in the navigation stack
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        // Get our description text view
        _descriptionTextView = (TextView) view.findViewById(R.id.registration_description);
        _descriptionTextView.setText(getActivity().getResources().getString(R.string.registration_same_lan_instructions));

        // Populate the spinners for product type & registration type
        Spinner s = (Spinner) view.findViewById(R.id.spinner_product_type);
        s.setOnItemSelectedListener(this);
        s.setAdapter(createProductTypeAdapter());

        s = (Spinner) view.findViewById(R.id.spinner_registration_type);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.registration_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setSelection(REG_TYPE_SAME_LAN);
        s.setOnItemSelectedListener(this);
        s.setAdapter(adapter);

        final Button scanButton = (Button) view.findViewById(R.id.scan_button);
        scanButton.setOnClickListener(this);

        // Hook up the "Register" button
        Button registerButton = (Button) view.findViewById(R.id.register_button);
        registerButton.setOnClickListener(this);

        // Stop polling
        SessionManager.deviceManager().stopPolling();

        doScan();

        return view;
    }

    ArrayAdapter<Device> createProductTypeAdapter() {
        List<Class<? extends Device>> deviceClasses = SessionManager.sessionParameters().deviceCreator.getSupportedDeviceClasses();
        ArrayList<Device> deviceList = new ArrayList<>();
        for (Class<? extends Device> c : deviceClasses) {
            try {
                AylaDevice fakeDevice = new AylaDevice();
                Device d = c.getDeclaredConstructor(AylaDevice.class).newInstance(fakeDevice);
                deviceList.add(d);
            } catch (java.lang.InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        ArrayAdapter<Device> adapter = new DeviceTypeAdapter(getActivity(),
                deviceList.toArray(new Device[deviceList.size()]));

        return adapter;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == R.id.spinner_product_type) {
            // Update the product type. We will set the appropriate value on the registration
            // type when this is selected.
            Spinner regTypeSpinner = (Spinner) getView().findViewById(R.id.spinner_registration_type);

            Device d = (Device) parent.getAdapter().getItem(position);

            // Find the index of the preferred registration type of the selected device
            Adapter adapter = regTypeSpinner.getAdapter();
            int i;
            for (i = 0; i < adapter.getCount(); i++) {
                String type = (String) adapter.getItem(i);
                if (type.equals(d.registrationType()))
                    break;
            }

            // Set the appropriate registration type
            if (i < (adapter.getCount())) {
                regTypeSpinner.setSelection(i, true);
            } else {
                Log.e(LOG_TAG, "Unknown registration type: " + d.registrationType());
                regTypeSpinner.setSelection(REG_TYPE_SAME_LAN, true);
            }

        } else if (parent.getId() == R.id.spinner_registration_type) {
            // Update the display text
            int textId;
            switch (position) {
                case REG_TYPE_BUTTON_PUSH:
                default:
                    textId = R.string.registration_button_push_instructions;
                    break;

                case REG_TYPE_SAME_LAN:
                    textId = R.string.registration_same_lan_instructions;
                    break;

                case REG_TYPE_DISPLAY:
                    textId = R.string.registration_display_instructions;
                    break;
            }
            _descriptionTextView.setText(getActivity().getResources().getString(textId));
        }

        Log.i(LOG_TAG, "Selected " + position);
    }

    private String getSelectedRegistrationType() {
        Spinner regTypeSpinner = (Spinner) getView().findViewById(R.id.spinner_registration_type);
        switch (regTypeSpinner.getSelectedItemPosition()) {
            case REG_TYPE_SAME_LAN:
                return AylaNetworks.AML_REGISTRATION_TYPE_SAME_LAN;
            case REG_TYPE_BUTTON_PUSH:
                return AylaNetworks.AML_REGISTRATION_TYPE_BUTTON_PUSH;
            case REG_TYPE_DISPLAY:
                return AylaNetworks.AML_REGISTRATION_TYPE_DISPLAY;
        }
        return null;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Log.i(LOG_TAG, "Nothing Selected");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.register_button:
                // Register button clicked
                Log.i(LOG_TAG, "Register clicked");

                MainActivity.getInstance().showWaitDialog(null, null);
                AylaDevice newDevice = new AylaDevice();
                newDevice.registrationType = getSelectedRegistrationType();
                registerNewDevice(newDevice);
                break;

            case R.id.scan_button:
                Log.i(LOG_TAG, "Scan clicked");
                doScan();
                break;
        }
    }

    private void doScan() {
        // Put up a progress dialog
        MainActivity.getInstance().showWaitDialog(getString(R.string.scanning_for_devices_title),
                getString(R.string.scanning_for_devices_message));

        AylaSetup.returnHostScanForNewDevices(new DeviceScanHandler(this));
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

    private void connectDeviceToService(String ssid, String security, String password) {
        AylaSetup.lanSsid = ssid;
        AylaSetup.lanPassword = password;
        AylaSetup.lanSecurityType = security;

        AylaSetup.connectNewDeviceToService(new ConnectToServiceHandler(this));
        MainActivity.getInstance().showWaitDialog(getString(R.string.connecting_to_network_title),
                getString(R.string.connecting_to_network_body));
    }

    static class RegisterHandler extends Handler {
        private WeakReference<AddDeviceFragment> _addDeviceFragment;

        public RegisterHandler(AddDeviceFragment addDeviceFragment) {
            _addDeviceFragment = new WeakReference<AddDeviceFragment>(addDeviceFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(LOG_TAG, "Register handler called: " + msg);
            MainActivity.getInstance().dismissWaitDialog();
            if (msg.arg1 >= 200 && msg.arg1 < 300) {
                // Success!
                AylaDevice aylaDevice = AylaSystemUtils.gson.fromJson((String) msg.obj, AylaDevice.class);
                Device device = SessionManager.sessionParameters().deviceCreator.deviceForAylaDevice(aylaDevice);
                MainActivity.getInstance().showWaitDialog(R.string.updating_notifications_title, R.string.updating_notifications_body);
                // Now update the device notifications
                DeviceNotificationHelper helper = new DeviceNotificationHelper(device, AylaUser.getCurrent());
                helper.initializeNewDeviceNotifications(new DeviceNotificationHelper.DeviceNotificationHelperListener() {
                    @Override
                    public void newDeviceUpdated(Device device, int error) {
                        MainActivity mainActivity = MainActivity.getInstance();
                        mainActivity.dismissWaitDialog();
                        int msgId = (error == AylaNetworks.AML_ERROR_OK ? R.string.registration_success : R.string.registration_success_notification_fail);
                        Toast.makeText(mainActivity, msgId, Toast.LENGTH_LONG).show();
                        SessionManager.deviceManager().refreshDeviceList();
                    }
                });

            } else {
                // Something went wrong
                Toast.makeText(_addDeviceFragment.get().getActivity(), R.string.registration_failure, Toast.LENGTH_LONG).show();
            }
        }
    }

    private AylaHostScanResults _hostScanResults[];
    private void handleScanResults(final AylaHostScanResults[] scanResults) {
        _hostScanResults = scanResults;
        if ( scanResults == null || scanResults.length == 0 ) {
            Toast.makeText(getActivity(), R.string.no_devices_found, Toast.LENGTH_LONG).show();
        } else {
            // Let the user choose which device to connect to
            String apNames[] = new String[scanResults.length];
            for ( int i = 0; i < scanResults.length; i++ ) {
                apNames[i] = scanResults[i].ssid;
            }

            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.choose_new_device)
                    .setSingleChoiceItems(apNames, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            connectToAP(scanResults[which]);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create().show();
        }
    }

    private void connectToAP(AylaHostScanResults scanResult) {
        AylaSetup.newDevice.hostScanResults = scanResult;
        AylaSetup.lanSsid = scanResult.ssid;
        AylaSetup.lanSecurityType = scanResult.keyMgmt;

        // Connect to the device
        MainActivity.getInstance().showWaitDialog(getString(R.string.connecting_to_device_title),
                getString(R.string.connecting_to_device_body));
        AylaSetup.connectToNewDevice(new ConnectHandler(this));
    }

    private void registerNewDevice(AylaDevice device) {
        MainActivity.getInstance().showWaitDialog(R.string.registering_device_title, R.string.registering_device_body);
        device.registerNewDevice(new RegisterHandler(this));
    }

    /**
     * Handler called with the results from {@link com.aylanetworks.aaml.AylaSetup#returnHostScanForNewDevices}.
     * We will get a set of APs that match the SSID regex in the SessionParameters.
     */
    static class DeviceScanHandler extends Handler {
        private WeakReference<AddDeviceFragment> _frag;

        public DeviceScanHandler(AddDeviceFragment frag) {
            _frag = new WeakReference<AddDeviceFragment>(frag);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "Got scan results: " + msg);
            MainActivity.getInstance().dismissWaitDialog();

            if ( AylaNetworks.succeeded(msg) ) {
                String json = (String)msg.obj;
                _frag.get().handleScanResults(AylaSystemUtils.gson.fromJson(json, AylaHostScanResults[].class));
            } else {
                String errMsg = (String)msg.obj;
                if ( errMsg == null || errMsg.contains("DISABLED") ) {
                    errMsg = _frag.get().getString(R.string.error_wifi_not_enabled);
                }
                Toast.makeText(_frag.get().getActivity(), errMsg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Handler called with the results for the AP scan (the device is scanning for APs)
     */
    static class ScanForAPsHandler extends Handler {
        private WeakReference<AddDeviceFragment> _fragment;

        public ScanForAPsHandler(AddDeviceFragment fragment) {
            _fragment = new WeakReference<AddDeviceFragment>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity.getInstance().dismissWaitDialog();
            Log.d(LOG_TAG, "Scan for APs handler: " + msg);
            if ( AylaNetworks.succeeded(msg) ) {
                String json = (String)msg.obj;
                ChooseAPDialog d = ChooseAPDialog.newInstance(AylaSystemUtils.gson.fromJson(json, AylaModuleScanResults[].class));
                d.setTargetFragment(_fragment.get(), 0);
                d.show(MainActivity.getInstance().getSupportFragmentManager(), "ap");
            }
        }
    }

    /**
     * Handler called when attempting to connect to the device's AP
     */
    static class ConnectHandler extends Handler {
        private WeakReference<AddDeviceFragment> _fragment;

        public ConnectHandler(AddDeviceFragment frag) {
            _fragment = new WeakReference<AddDeviceFragment>(frag);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "Connect handler: " + msg);
            MainActivity activity = MainActivity.getInstance();
            activity.dismissWaitDialog();

            if ( AylaNetworks.succeeded(msg) ) {
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
    }

    static class ConfirmNewDeviceHandler extends Handler {
        private WeakReference<AddDeviceFragment> _frag;

        public ConfirmNewDeviceHandler(AddDeviceFragment frag) {
            _frag = new WeakReference<AddDeviceFragment>(frag);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity.getInstance().dismissWaitDialog();
            AylaSetup.exit();

            if ( AylaNetworks.succeeded(msg) ) {
                AylaDevice device = AylaSystemUtils.gson.fromJson((String)msg.obj, AylaDevice.class);
                Log.d(LOG_TAG, "New device: " + device);
                // Set up the new device
                _frag.get().registerNewDevice(device);
            } else {
                Log.e(LOG_TAG, "Confirm new device failed: " + msg);
                Toast.makeText(MainActivity.getInstance(), (String)msg.obj, Toast.LENGTH_LONG).show();
            }
        }
    }
    static class ConnectToServiceHandler extends Handler {
        private WeakReference<AddDeviceFragment> _frag;
        public ConnectToServiceHandler(AddDeviceFragment frag) {
            _frag = new WeakReference<AddDeviceFragment>(frag);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "Connect to service handler: " + msg);
            MainActivity.getInstance().dismissWaitDialog();
            if ( AylaNetworks.succeeded(msg) ) {
                Toast.makeText(MainActivity.getInstance(), R.string.connect_to_service_success, Toast.LENGTH_SHORT).show();
                // Confirm service connection. We need to do this to get the device information
                // to register it.
                MainActivity.getInstance().showWaitDialog(R.string.confirm_new_device_title, R.string.confirm_new_device_body);
                AylaSetup.confirmNewDeviceToServiceConnection(new ConfirmNewDeviceHandler(_frag.get()));
            } else {
                // Check for invalid key present in the error message
                String emsg = (String) msg.obj;
                if (emsg != null && emsg.contains("invalid key")) {
                    Toast.makeText(MainActivity.getInstance(), R.string.bad_wifi_password, Toast.LENGTH_LONG).show();
                } else {
                    String anErrMsg = (String) msg.obj;
                    if ( anErrMsg == null ) {
                        anErrMsg = MainActivity.getInstance().getResources().getString(R.string.unknown_error);
                    }
                    Toast.makeText(MainActivity.getInstance(), anErrMsg, Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
