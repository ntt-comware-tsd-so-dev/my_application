package com.aylanetworks.agilelink.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.TextUtils;
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
import com.aylanetworks.aaml.AylaDeviceNode;
import com.aylanetworks.aaml.AylaHostScanResults;
import com.aylanetworks.aaml.AylaLanMode;
import com.aylanetworks.aaml.AylaModule;
import com.aylanetworks.aaml.AylaModuleScanResults;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaSetup;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.aaml.AylaWiFiStatus;
import com.aylanetworks.agilelink.AgileLinkApplication;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.adapters.DeviceTypeAdapter;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.DeviceNotificationHelper;
import com.aylanetworks.agilelink.framework.Gateway;
import com.aylanetworks.agilelink.framework.Logger;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * AddDeviceFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 1/21/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class AddDeviceFragment extends Fragment
        implements AdapterView.OnItemSelectedListener, View.OnClickListener,
        ChooseAPDialog.ChooseAPResults, Gateway.GatewayNodeRegistrationListener, DialogInterface.OnCancelListener {
    private static final String LOG_TAG = "AddDeviceFragment";

    private static final int REG_TYPE_SAME_LAN = 0;
    private static final int REG_TYPE_BUTTON_PUSH = 1;
    private static final int REG_TYPE_DISPLAY = 2;
    private static final int REG_TYPE_NODE = 3;

    /** Time to delay after completing wifi setup and trying to register the new device */
    private static final int REGISTRATION_DELAY_MS = 5000;

    /**
     * Default instance creator class method
     *
     * @return A new AddDeviceFragment ready for user interaction
     */
    public static AddDeviceFragment newInstance() {
        return new AddDeviceFragment();
    }

    private TextView _spinnerRegistrationTypeLabel;
    private Spinner _spinnerRegistrationType;
    private Spinner _spinnerGatewaySelection;
    private TextView _descriptionTextView;
    private int _registrationType = REG_TYPE_SAME_LAN;

    private static boolean _needsExit;

    private boolean _enableLANMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Logger.logVerbose(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        // we don't want the device manager to try entering lan mode when fetching the device list
        _enableLANMode = SessionManager.sessionParameters().enableLANMode;
        SessionManager.sessionParameters().enableLANMode = false;
    }

    @Override
    public void onResume() {
        Logger.logVerbose(LOG_TAG, "onResume");
        super.onResume();

        // Stop polling & LAN mode
        SessionManager.getInstance().setForeground(false);
        if ( SessionManager.deviceManager() != null ) {
            SessionManager.deviceManager().stopPolling();
        }
        AylaLanMode.pause(false);

        // TODO: quick hack... remove this
        // TODO: NetworkOnMainThread is caused when we don't have a cached property and have to
        // TODO: immediately go to the network to get the current value.
        StrictMode.ThreadPolicy tp = StrictMode.ThreadPolicy.LAX;
        StrictMode.setThreadPolicy(tp);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Logger.logVerbose(LOG_TAG, "onDetach");
        if ( _needsExit ) {
            exitSetup();
        }

        if (_nodeRegistrationGateway != null) {
            _nodeRegistrationGateway.cleanupRegistrationScan();
        }

        // make sure we are still in the foreground
        if (AgileLinkApplication.getLifeCycleState() == AgileLinkApplication.LifeCycleState.Foreground) {

            // Restart polling & LAN mode
            if ( SessionManager.deviceManager() != null ) {
                SessionManager.deviceManager().startPolling();
            }
            SessionManager.getInstance().setForeground(true);
            AylaLanMode.resume();
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        if ( MainActivity.getInstance().isNoDevicesMode() ) {
            getActivity().getMenuInflater().inflate(R.menu.menu_no_devices, menu);
        }

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
        _descriptionTextView.setText(getString(R.string.registration_same_lan_instructions));

        // Get the spinner registration type label
        _spinnerRegistrationTypeLabel = (TextView) view.findViewById(R.id.spinner_registration_type_label);
        _spinnerRegistrationTypeLabel.setText(getString(R.string.registration_type));

        // Populate the spinners for product type & registration type
        Spinner s = (Spinner) view.findViewById(R.id.spinner_product_type);
        s.setAdapter(createProductTypeAdapter());
        List<Gateway> gateways = SessionManager.deviceManager().getGatewayDevices();
        if ((gateways != null) && (gateways.size() > 0)) {
            // If they have a gateway, then default to Zigbee Node
            int index = 0;
            List<Class<? extends Device>> deviceClasses = SessionManager.sessionParameters().deviceCreator.getSupportedDeviceClasses();
            for (Class<? extends Device> c : deviceClasses) {
                if (c.getSimpleName().equals("ZigbeeNodeDevice")) {
                    _nodeRegistrationGateway = gateways.get(0);
                    _registrationType = REG_TYPE_NODE;
                    s.setSelection(index);
                    break;
                }
                index++;
            }
        }
        s.setOnItemSelectedListener(this);

        s = _spinnerRegistrationType = (Spinner)view.findViewById(R.id.spinner_registration_type);
        ArrayAdapter<CharSequence>  adapter = ArrayAdapter.createFromResource(getActivity(), R.array.registration_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setSelection(_registrationType);
        s.setOnItemSelectedListener(this);
        s.setAdapter(adapter);

        s = _spinnerGatewaySelection = (Spinner)view.findViewById(R.id.spinner_gateway_selection);
        s.setOnItemSelectedListener(this);
        s.setAdapter(createGatewayAdapter());

        final Button scanButton = (Button) view.findViewById(R.id.scan_button);
        scanButton.setOnClickListener(this);

        // Hook up the "Register" button
        Button registerButton = (Button) view.findViewById(R.id.register_button);
        registerButton.setOnClickListener(this);

        // Stop polling
        SessionManager.deviceManager().stopPolling();

        return view;
    }

    void dismissWaitDialog() {
        StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
        Logger.logDebug(LOG_TAG, "rn: dismissWaitDialog called from %s", stacktrace[3].getMethodName());
        MainActivity.getInstance().dismissWaitDialog();
    }

    ArrayAdapter<Device> createGatewayAdapter() {
        List<Gateway> gateways = SessionManager.deviceManager().getGatewayDevices();
        return new DeviceTypeAdapter(getActivity(), gateways.toArray(new Device[gateways.size()]), true);
    }

    ArrayAdapter<Device> createProductTypeAdapter() {
        List<Class<? extends Device>> deviceClasses = SessionManager.sessionParameters().deviceCreator.getSupportedDeviceClasses();
        ArrayList<Device> deviceList = new ArrayList<>();
        for (Class<? extends Device> c : deviceClasses) {
            try {
                AylaDevice fakeDevice = new AylaDevice();
                Device d = c.getDeclaredConstructor(AylaDevice.class).newInstance(fakeDevice);
                deviceList.add(d);
            } catch (java.lang.InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return new DeviceTypeAdapter(getActivity(), deviceList.toArray(new Device[deviceList.size()]));
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
                Logger.logError(LOG_TAG, "Unknown registration type: " + d.registrationType());
                regTypeSpinner.setSelection(REG_TYPE_SAME_LAN, true);
            }

        } else if (parent.getId() == R.id.spinner_registration_type) {
            // Update the display text
            int spinnerVisible = View.VISIBLE;
            boolean showGateways = false;
            int textId;
            _registrationType = position;
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

                case REG_TYPE_NODE: {
                    textId = R.string.gateway_find_devices;
                    showGateways = true;
                    List<Gateway> gateways = SessionManager.deviceManager().getGatewayDevices();
                    if ((gateways == null) || (gateways.size() == 0)) {
                        textId = R.string.error_no_gateway_instructions;
                        spinnerVisible = View.GONE;
                    }
                }
                    break;
            }
            _descriptionTextView.setText(Html.fromHtml(getActivity().getResources().getString(textId)));
            _spinnerRegistrationTypeLabel.setText(getString(showGateways ? R.string.select_gateway : R.string.registration_type));
            _spinnerRegistrationTypeLabel.setVisibility(spinnerVisible);
            _spinnerRegistrationType.setVisibility(showGateways ? View.GONE : View.VISIBLE);
            _spinnerGatewaySelection.setVisibility(showGateways ? spinnerVisible : View.GONE);
        } else if (parent.getId() == R.id.spinner_gateway_selection) {
            Gateway gateway = (Gateway) parent.getAdapter().getItem(position);
            if (gateway != null) {
                Logger.logInfo(LOG_TAG, "rn: selected gateway [" + gateway.getDevice().dsn + "]");
                _nodeRegistrationGateway = gateway;
            } else {
                Logger.logError(LOG_TAG, "rn: no gateway");
                _nodeRegistrationGateway = null;
            }
        }

        Logger.logInfo(LOG_TAG, "Selected " + position);
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
            case REG_TYPE_NODE:
                return AylaNetworks.AML_REGISTRATION_TYPE_NODE;
        }
        return null;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Logger.logInfo(LOG_TAG, "Nothing Selected");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// Node Scanning & Registration

    private Gateway _nodeRegistrationGateway;

    private void registerButtonClick() {
        // Register button clicked
        if (_registrationType == REG_TYPE_NODE) {
            // we need something to register...
            Logger.logError(LOG_TAG, "rn: Register node no device node to register!");
            Toast.makeText(MainActivity.getInstance(), R.string.error_gateway_register_no_device, Toast.LENGTH_LONG).show();
        } else {
            Logger.logInfo(LOG_TAG, "rn: registerNewDevice");
            MainActivity.getInstance().showWaitDialog(null, null);
            AylaDevice newDevice = new AylaDevice();
            newDevice.registrationType = getSelectedRegistrationType();
            registerNewDevice(newDevice);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.register_button:
                registerButtonClick();
                break;

            case R.id.scan_button:
                doScan();
                break;
        }
    }

    private static void exitSetup() {
        if ( _needsExit ) {
            MainActivity.getInstance().showWaitDialog(R.string.exiting_setup_title, R.string.exiting_setup_body);
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    Logger.logVerbose(LOG_TAG, "calling AylaSetup.exit()...");
                    AylaSetup.exit();
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    Logger.logVerbose(LOG_TAG, "AylaSetup.exit() completed.");
                    _needsExit = false;
                    MainActivity.getInstance().dismissWaitDialog();
                }
            }.execute();
        }
    }

    private void showMessage(int resourceId){
        new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_launcher)
                .setTitle(getString(R.string.attention))
                .setMessage(getString(resourceId))
                .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }

    public void gatewayRegistrationCandidateAdded(Device device, boolean moreComing, Object tag) {
        Toast.makeText(MainActivity.getInstance(), R.string.gateway_registered_device_node, Toast.LENGTH_LONG).show();
        Logger.logInfo(LOG_TAG, "rn: device [%s:%s] added", device.getDevice().dsn, device.getDevice().productName);

        /*
        int resourceId = device.hasPostRegistrationProcessingResourceId();
        if (resourceId > 0) {
            showMessage(resourceId);
        }

        // TODO: This shouldn't be here... this should be in the Device class...
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
        */

    }

    /**
     * Set _useSingleSelect to true to show a dialog that only allows one device to selected
     * for registration.  Set to false to show a dialog with a multiselect list of devices to
     * register.
     */
    boolean _useSingleSelect = false;

    public void gatewayRegistrationCandidates(final List<AylaDeviceNode> list, Object tag) {
        _scanTag = null;
        dismissWaitDialog();

        // Let the user choose which device to connect to
        String apNames[] = new String[list.size()];

        final List<AylaDeviceNode> selected = new ArrayList<AylaDeviceNode>();
        boolean[] isSelectedArray = new boolean[list.size()];
        for ( int i = 0; i < list.size(); i++ ) {
            isSelectedArray[i] = true;
            AylaDeviceNode adn = list.get(i);
            Logger.logVerbose(LOG_TAG, "rn: candidate [%s]", adn);
            apNames[i] = adn.productName;
            selected.add(adn);
        }

        if (_useSingleSelect) {
            new AlertDialog.Builder(getActivity())
                    .setIcon(R.drawable.ic_launcher)
                    .setTitle(R.string.choose_new_device)
                    .setSingleChoiceItems(apNames, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AylaDeviceNode adn = list.get(which);
                            Logger.logVerbose(LOG_TAG, "rn: register candidate [%s:%s]", adn.dsn, adn.model);
                            List<AylaDeviceNode> tmp = new ArrayList<AylaDeviceNode>();
                            tmp.add(adn);
                            MainActivity.getInstance().showWaitDialog(R.string.registering_device_title, R.string.registering_device_body);
                            _nodeRegistrationGateway.registerCandidates(tmp, _nodeRegistrationGateway, AddDeviceFragment.this);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create().show();
        } else {
            new AlertDialog.Builder(getActivity())
                    .setIcon(R.drawable.ic_launcher)
                    .setTitle(R.string.choose_new_devices)
                    .setMultiChoiceItems(apNames, isSelectedArray, new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                            AylaDeviceNode adn = list.get(which);
                            Logger.logVerbose(LOG_TAG, "rn: register candidate [%s:%s] %s", adn.dsn, adn.model, (isChecked ? "YES" : "NO"));
                            if (isChecked) {
                                selected.add(adn);
                            } else {
                                selected.remove(adn);
                            }
                        }
                    })
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (selected.size() > 0) {
                                MainActivity.getInstance().showWaitDialog(R.string.registering_device_title, R.string.registering_device_body);
                                _nodeRegistrationGateway.registerCandidates(selected, _nodeRegistrationGateway, AddDeviceFragment.this);
                            }
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create().show();
        }
    }

    public void gatewayRegistrationComplete(Message msg, int messageResourceId, Object tag) {
        Logger.logMessage(LOG_TAG, msg, "rn: gatewayRegistrationComplete");
        _scanTag = null;
        dismissWaitDialog();
        if (messageResourceId != 0) {
            Toast.makeText(MainActivity.getInstance(), messageResourceId, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (_scanTag != null) {
            Logger.logInfo(LOG_TAG, "rn: cancel Register node scan");
            _scanTag.cancel();
        }
    }

    Gateway.AylaGatewayScanCancelHandler _scanTag;

    private void doScan() {
        if (_registrationType==REG_TYPE_NODE) {
            if (_nodeRegistrationGateway == null) {
                Logger.logError(LOG_TAG, "rn: Register node has no gateway!");
                Toast.makeText(MainActivity.getInstance(), R.string.error_no_gateway, Toast.LENGTH_LONG).show();
            } else {
                // Put up a progress dialog
                MainActivity.getInstance().showWaitDialogWithCancel(getString(R.string.scanning_for_devices_title),
                        getString(R.string.scanning_for_gateway_devices_message), this);

                Logger.logInfo(LOG_TAG, "rn: startRegistration [" + _nodeRegistrationGateway.getDevice().dsn + "]");
                _scanTag = _nodeRegistrationGateway.startRegistrationScan(false, _nodeRegistrationGateway, this);
            }
        } else {
            // Put up a progress dialog
            MainActivity.getInstance().showWaitDialog(getString(R.string.scanning_for_devices_title),
                    getString(R.string.scanning_for_devices_message));

            Logger.logVerbose(LOG_TAG, "rn: returnHostScanForNewDevices");
            AylaSetup.returnHostScanForNewDevices(new DeviceScanHandler(this));
        }
    }

    @Override
    public void choseAccessPoint(String accessPoint, String security, String password) {
        Logger.logDebug(LOG_TAG, "choseAccessPoint: " + accessPoint + "[" + security + "]");
        if ( accessPoint == null ) {
            exitSetup();
        } else {
            connectDeviceToService(accessPoint, security, password);
        }
    }

    private void connectDeviceToService(String ssid, String security, String password) {
        AylaSetup.lanSsid = ssid;
        AylaSetup.lanPassword = password;
        AylaSetup.lanSecurityType = security;

        MainActivity.getInstance().showWaitDialog(getString(R.string.connecting_to_network_title),
                getString(R.string.connecting_to_network_body));

        //adding location details
        Context context = this.getActivity();
        Map<String, Object> callParams = new HashMap<String, Object>();
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean netEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if(gpsEnabled || netEnabled){
            String locationProvider = locationManager.getBestProvider(criteria, false);
            Location location = locationManager.getLastKnownLocation(locationProvider);
            if(location != null){
                callParams.put(AylaSetup.AML_SETUP_LOCATION_LATITUDE, location.getLatitude());
                callParams.put(AylaSetup.AML_SETUP_LOCATION_LONGITUDE, location.getLongitude());
            }

        }
        else{
            Toast.makeText(context, R.string.warning_location_accuracy, Toast.LENGTH_SHORT).show();
        }

        Logger.logVerbose(LOG_TAG, "calling connectNewDeviceToService: ssid = " + ssid + " security: " + security + "pass: " + password);
        AylaSetup.connectNewDeviceToService(new ConnectToServiceHandler(this), callParams);
    }

    static class RegisterHandler extends Handler {
        private WeakReference<AddDeviceFragment> _addDeviceFragment;

        public RegisterHandler(AddDeviceFragment addDeviceFragment) {
            _addDeviceFragment = new WeakReference<AddDeviceFragment>(addDeviceFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.logInfo(LOG_TAG, "Register handler called: " + msg);
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
                exitSetup();
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
                    .setIcon(R.drawable.ic_launcher)
                    .setTitle(R.string.choose_new_device)
                    .setSingleChoiceItems(apNames, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            connectToAP(scanResults[which]);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()
                    .show();
        }
    }

    private void connectToAP(AylaHostScanResults scanResult) {
        AylaSetup.newDevice.hostScanResults = scanResult;
        AylaSetup.lanSsid = scanResult.ssid;
        AylaSetup.lanSecurityType = scanResult.keyMgmt;

        // Connect to the device
        MainActivity.getInstance().showWaitDialog(getString(R.string.connecting_to_device_title),
                getString(R.string.connecting_to_device_body));
        Logger.logVerbose(LOG_TAG, "calling connectToNewDevice: ssid = " + scanResult.ssid + " key mgt: " + scanResult.keyMgmt);
        AylaSetup.connectToNewDevice(new ConnectHandler(this));
    }

    private void registerNewDevice(AylaDevice device) {
        MainActivity.getInstance().showWaitDialog(R.string.registering_device_title, R.string.registering_device_body);

        Logger.logVerbose(LOG_TAG, "Calling registerNewDevice...");
        device.registerNewDevice(new RegisterHandler(this));
    }

    private AylaModuleScanResults _savedScanResults[];
    private void chooseAP(AylaModuleScanResults scanResults[]) {
        _savedScanResults = scanResults;
        ChooseAPDialog d = ChooseAPDialog.newInstance(scanResults);
        d.setTargetFragment(this, 0);
        d.show(getFragmentManager(), "ap");
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
            Logger.logDebug(LOG_TAG, "Response from returnHostScanForNewDevices: " + msg);
            MainActivity.getInstance().dismissWaitDialog();

            if ( _frag.get() == null ) {
                // We've been dismissed.
                Logger.logError(LOG_TAG, "I no longer exist. I won't show the list of devices from the AP scan.");
                return;
            }

            if ( AylaNetworks.succeeded(msg) ) {
                String json = (String)msg.obj;
                _frag.get().handleScanResults(AylaSystemUtils.gson.fromJson(json, AylaHostScanResults[].class));
            } else {
                String errMsg = (String)msg.obj;
                if ( errMsg == null || errMsg.contains("DISABLED") ) {
                    errMsg = _frag.get().getString(R.string.error_wifi_not_enabled);
                }
                Toast.makeText(_frag.get().getActivity(), errMsg, Toast.LENGTH_SHORT).show();
                exitSetup();
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
            Logger.logDebug(LOG_TAG, "getNewDeviceScanForAPs results: " + msg);
            if ( AylaNetworks.succeeded(msg) ) {
                String json = (String)msg.obj;
                _fragment.get().chooseAP(AylaSystemUtils.gson.fromJson(json, AylaModuleScanResults[].class));
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
            Logger.logDebug(LOG_TAG, "Connect handler: " + msg);

            // We've mucked with our wifi access point. Make sure we go back to our original
            // AP when we're done with setup, regardless of whether or not it succeeds.
            _needsExit = true;

            MainActivity activity = MainActivity.getInstance();
            activity.dismissWaitDialog();

            if ( AylaNetworks.succeeded(msg) ) {
                String json = (String)msg.obj;

                AylaSetup.newDevice = AylaSystemUtils.gson.fromJson(json, AylaModule.class);
                Logger.logVerbose(LOG_TAG, "calling getNewDeviceScanForAPs. newDevice = " + AylaSetup.newDevice);
                AylaSetup.getNewDeviceScanForAPs(new ScanForAPsHandler(_fragment.get()));
                activity.showWaitDialog(activity.getString(R.string.scanning_for_aps_title),
                        activity.getString(R.string.scanning_for_aps_body));
            } else {
                Logger.logError(LOG_TAG, "Connect handler error: " + msg);
                Toast.makeText(activity, R.string.wifi_connect_failed, Toast.LENGTH_LONG).show();
                exitSetup();
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

            if ( AylaNetworks.succeeded(msg) ) {
                final AylaDevice device = AylaSystemUtils.gson.fromJson((String)msg.obj, AylaDevice.class);
                Logger.logDebug(LOG_TAG, "New device: " + device);
                // Set up the new device if it's not already registered
                if ( SessionManager.deviceManager().deviceByDSN(device.dsn) == null ) {
                    // We need to wait a bit before attempting to register. The service needs some
                    // time to get itself in order first.
                    MainActivity.getInstance().showWaitDialog(R.string.registering_device_title, R.string.registering_device_body);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            _frag.get().registerNewDevice(device);
                        }
                    }, REGISTRATION_DELAY_MS);

                } else {
                    MainActivity.getInstance().popBackstackToRoot();
                    Toast.makeText(MainActivity.getInstance(), R.string.connect_to_service_success, Toast.LENGTH_LONG).show();
                }
            } else {
                Logger.logError(LOG_TAG, "Confirm new device failed: " + msg);
                if ( msg.arg1 == AylaNetworks.AML_ERROR_NOT_FOUND ) {
                    // The service did not find the new device. Check with the device to see why.
                    MainActivity.getInstance().showWaitDialog(R.string.reconnecting_device_title, R.string.reconnecting_device_message);
                    AylaSetup.getNewDeviceWiFiStatus(new GetNewDeviceWiFiStatusHandler(_frag.get()));
                } else {
                    String message = (String) msg.obj;
                    if (TextUtils.isEmpty(message)) {
                        message = MainActivity.getInstance().getResources().getString(R.string.unknown_error);
                    }
                    Toast.makeText(MainActivity.getInstance(), message, Toast.LENGTH_LONG).show();
                    exitSetup();
                }
            }
        }
    }

    static class GetNewDeviceWiFiStatusHandler extends Handler {
        private WeakReference<AddDeviceFragment> _frag;
        public GetNewDeviceWiFiStatusHandler(AddDeviceFragment fragment) {
            _frag = new WeakReference<AddDeviceFragment>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.logDebug(LOG_TAG, "GetNewDeviceWiFiStatusHandler: " + msg);
            MainActivity.getInstance().dismissWaitDialog();
            if ( AylaNetworks.succeeded(msg) ) {
                AylaWiFiStatus status = AylaSystemUtils.gson.fromJson((String)msg.obj, AylaWiFiStatus.class);
                Logger.logDebug(LOG_TAG, "Wifi status: " + status);
                if ( status != null && status.connectHistory != null ) {
                    String connectionMessage = AylaNetworks.AML_wifiErrorMsg[status.connectHistory[0].error];
                    if ( !TextUtils.isEmpty(connectionMessage) ) {
                        Toast.makeText(MainActivity.getInstance(), connectionMessage, Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            }

            // Failed to connect for an unknown reason
            Toast.makeText(MainActivity.getInstance(), R.string.device_connection_failed, Toast.LENGTH_LONG).show();
        }
    }

    static class ConnectToServiceHandler extends Handler {
        private WeakReference<AddDeviceFragment> _frag;
        public ConnectToServiceHandler(AddDeviceFragment frag) {
            _frag = new WeakReference<AddDeviceFragment>(frag);
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.logDebug(LOG_TAG, "Connect to service handler: " + msg);
            MainActivity.getInstance().dismissWaitDialog();
            if ( AylaNetworks.succeeded(msg) ) {
                // Confirm service connection. We need to do this to get the device information
                // to register it.
                MainActivity.getInstance().showWaitDialog(R.string.confirm_new_device_title, R.string.confirm_new_device_body);
                Logger.logVerbose(LOG_TAG, "calling confirmNewDeviceToServiceConnection...");
                AylaSetup.confirmNewDeviceToServiceConnection(new ConfirmNewDeviceHandler(_frag.get()));
            } else {
                // Check for invalid key present in the error message
                String emsg = (String) msg.obj;
                if (emsg != null && emsg.contains("invalid key")) {
                    Toast.makeText(MainActivity.getInstance(), R.string.bad_wifi_password, Toast.LENGTH_LONG).show();
                    _frag.get().chooseAP(_frag.get()._savedScanResults);
                } else {
                    String anErrMsg = (String) msg.obj;
                    if ( anErrMsg == null ) {
                        anErrMsg = MainActivity.getInstance().getResources().getString(R.string.unknown_error);
                    }
                    Logger.logError(LOG_TAG, "Failed to connect to service. Calling exitSetup()");
                    Toast.makeText(MainActivity.getInstance(), anErrMsg, Toast.LENGTH_LONG).show();
                    exitSetup();
                }
            }
        }
    }
}
