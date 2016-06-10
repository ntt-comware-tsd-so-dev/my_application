package com.aylanetworks.agilelink.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.text.Html;
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

import com.android.internal.util.Predicate;
import com.android.volley.Response;
import com.aylanetworks.agilelink.device.AgileLinkViewModelProvider;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.ViewModel;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaDeviceNode;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaUser;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.device.GenericGateway;
import com.aylanetworks.agilelink.fragments.adapters.DeviceTypeAdapter;
import com.aylanetworks.agilelink.framework.DeviceNotificationHelper;
import com.aylanetworks.agilelink.framework.Logger;
import com.aylanetworks.agilelink.framework.MenuHandler;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.error.RequestFuture;
import com.aylanetworks.aylasdk.setup.AylaRegistration;
import com.aylanetworks.aylasdk.setup.AylaRegistrationCandidate;
import com.aylanetworks.aylasdk.setup.AylaSetup;
import com.aylanetworks.aylasdk.setup.AylaSetupDevice;
import com.aylanetworks.aylasdk.setup.AylaWifiScanResults;
import com.aylanetworks.aylasdk.setup.AylaWifiStatus;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/*
 * AddDeviceFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 1/21/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */
public class AddDeviceFragment extends Fragment
        implements AdapterView.OnItemSelectedListener, View.OnClickListener,
        ChooseAPDialog.ChooseAPResults, GenericGateway.GatewayNodeRegistrationListener,
        DialogInterface.OnCancelListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String LOG_TAG = "AddDeviceFragment";
    private static final int REQUEST_LOCATION = 2;

    private static final boolean USE_WELCOME_FRAGMENT = true;

    /** Time to delay after completing wifi setup and trying to register the new device */
    private static final int REGISTRATION_DELAY_MS = 9000;

    private static final AylaDevice.RegistrationType[] __supportedRegTypes = {AylaDevice
            .RegistrationType.SameLan,
            AylaDevice.RegistrationType.ButtonPush,
            AylaDevice.RegistrationType.Display,
            AylaDevice.RegistrationType.Node};

    private static final String DEFAULT_HOST_SCAN_REGEX = "Ayla-[0-9a-zA-Z]{12}";

    /**
     * Default instance creator class method
     *
     * @return A new AddDeviceFragment ready for user interaction
     */
    public static AddDeviceFragment newInstance() {
        return new AddDeviceFragment();
    }

    private WifiManager _wifiManager;
    private String _ssid;
    private String _bssid;
    private AylaSetupDevice _setupDevice;

    private Button _registerButton;
    private TextView _spinnerRegistrationTypeLabel;
    private Spinner _spinnerRegistrationType;
    private Spinner _spinnerGatewaySelection;
    private TextView _descriptionTextView;
    private AylaDevice.RegistrationType _registrationType = AylaDevice.RegistrationType.SameLan;
    private Spinner _spinnerProductType;
    private static boolean _needsExit;
    private AylaRegistration _aylaRegistration;
    private AylaSetup _aylaSetup;

    void updateConnectionInfo() {
        WifiInfo info = _wifiManager.getConnectionInfo();
        if(info == null || info.getSSID() == null) {
            return;
        }

        _ssid = info.getSSID().replaceAll("^\"|\"$", "");

        if(info.getBSSID() == null) {
            return;
        }

        _bssid = info.getBSSID().replaceAll("^\"|\"$", "");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Logger.logVerbose(LOG_TAG, "rn: onCreate");
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        _aylaRegistration = AMAPCore.sharedInstance().getDeviceManager().getAylaRegistration();
        try {
            _aylaSetup = new AylaSetup(AMAPCore.sharedInstance().getContext(),
                    AMAPCore.sharedInstance().getSessionManager());
        } catch (AylaError aylaError) {
            AylaLog.e(LOG_TAG, "Failed to create AylaSetup object: " + aylaError);
            Toast.makeText(AMAPCore.sharedInstance().getContext(), aylaError.toString(),
                    Toast.LENGTH_LONG).show();
        }

        _wifiManager = (WifiManager)getActivity().getSystemService(Context.WIFI_SERVICE);
        updateConnectionInfo();
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
        _spinnerProductType = (Spinner) view.findViewById(R.id.spinner_product_type);
        _spinnerProductType.setAdapter(createProductTypeAdapter());
        List<AylaDevice> gateways = AMAPCore.sharedInstance().getSessionManager()
                .getDeviceManager().getDevices(new Predicate<AylaDevice>() {
                    @Override
                    public boolean apply(AylaDevice aylaDevice) {
                        return aylaDevice.isGateway();
                    }
                });

        if ((gateways != null) && (gateways.size() > 0)) {
            // If they have a gateway, then default to Generic Node
            int index = 0;
            AgileLinkViewModelProvider dc = (AgileLinkViewModelProvider)AMAPCore.sharedInstance()
                    .getSessionParameters().viewModelProvider;

            List<Class<? extends ViewModel>> deviceClasses = dc.getSupportedDeviceClasses();
            for (Class<? extends ViewModel> c : deviceClasses) {
                if (c.getSimpleName().equals("GenericNodeDevice")) {
                    _nodeRegistrationGateway = (GenericGateway)
                            dc.viewModelForDevice(gateways.get(0));
                    _registrationType = AylaDevice.RegistrationType.Node;
                    _spinnerProductType.setSelection(index);
                    break;
                }
                index++;
            }
        }
        _spinnerProductType.setOnItemSelectedListener(this);

        _spinnerRegistrationType = (Spinner)view.findViewById(R.id.spinner_registration_type);
        ArrayAdapter<CharSequence>  adapter = ArrayAdapter.createFromResource(getActivity(), R.array.registration_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        _spinnerRegistrationType.setSelection(registrationTypeIndex(_registrationType));
        _spinnerRegistrationType.setOnItemSelectedListener(this);
        _spinnerRegistrationType.setAdapter(adapter);

        _spinnerGatewaySelection = (Spinner)view.findViewById(R.id.spinner_gateway_selection);
        _spinnerGatewaySelection.setOnItemSelectedListener(this);
        _spinnerGatewaySelection.setAdapter(createGatewayAdapter());

        final Button scanButton = (Button) view.findViewById(R.id.scan_button);
        scanButton.setOnClickListener(this);

        // Hook up the "Register" button
        _registerButton = (Button) view.findViewById(R.id.register_button);
        _registerButton.setOnClickListener(this);

        return view;
    }

    /**
     * Returns the index for the spinner for the provided registration type
     * @param regType Registration type to check
     * @return index into the spinner array for this registration type, or -1 if not found
     */
    private int registrationTypeIndex(AylaDevice.RegistrationType regType) {
        for (int i = 0; i< __supportedRegTypes.length; i++) {
            if (__supportedRegTypes[i] == regType) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateConnectionInfo();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Logger.logVerbose(LOG_TAG, "rn: onDetach");

        if (_nodeRegistrationGateway != null) {
            _nodeRegistrationGateway.cleanupRegistrationScan();
        }

        if (_needsExit) {
            exitSetup();
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

    private static void dismissWaitDialog() {
        StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
        Logger.logDebug(LOG_TAG, "rn: dismissWaitDialog called from %s", stacktrace[3].getMethodName());
        MainActivity activity = MainActivity.getInstance();
        if (activity != null) {
            activity.dismissWaitDialog();
        }
    }

    ArrayAdapter<ViewModel> createGatewayAdapter() {
        List<AylaDevice> gatewayDevices = AMAPCore.sharedInstance().getDeviceManager()
                .getDevices(new Predicate<AylaDevice>() {
                    @Override
                    public boolean apply(AylaDevice aylaDevice) {
                        return aylaDevice.isGateway();
                    }
                });

        List<ViewModel> gateways = ViewModel.fromDeviceList(gatewayDevices);
        return new GenericGateway.GatewayTypeAdapter(getActivity(), gateways.toArray(new
                ViewModel[gateways.size()]), true);
    }

    ArrayAdapter<ViewModel> createProductTypeAdapter() {
        AgileLinkViewModelProvider viewModelProvider =
                (AgileLinkViewModelProvider)AMAPCore.sharedInstance().getSessionParameters()
                        .viewModelProvider;

        List<Class<? extends ViewModel>> deviceClasses = viewModelProvider.getSupportedDeviceClasses();
        ArrayList<ViewModel> deviceList = new ArrayList<>();
        for (Class<? extends ViewModel> c : deviceClasses) {
            try {
                AylaDevice fakeDevice = new AylaDevice();
                ViewModel d = c.getConstructor(AylaDevice.class).newInstance(fakeDevice);
                deviceList.add(d);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new DeviceTypeAdapter(getContext(), deviceList.toArray(new ViewModel[deviceList
                .size()]));
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == R.id.spinner_product_type) {
            // Update the product type. We will set the appropriate value on the registration
            // type when this is selected.
            if ( getView() == null ) {
                Logger.logError(LOG_TAG, "rn: No view!!!");
                return;
            }
            Spinner regTypeSpinner = (Spinner) getView().findViewById(R.id.spinner_registration_type);

            ViewModel d = (ViewModel) parent.getAdapter().getItem(position);

            // Find the index of the preferred registration type of the selected device
            Adapter adapter = regTypeSpinner.getAdapter();
            int i;
            for (i = 0; i < adapter.getCount(); i++) {
                String type = (String) adapter.getItem(i);
                if (type.equals(d.registrationType().stringValue()))
                    break;
            }

            // Set the appropriate registration type
            if (i < (adapter.getCount())) {
                regTypeSpinner.setSelection(i, true);
            } else {
                Logger.logError(LOG_TAG, "rn: Unknown registration type: " + d.registrationType());
                regTypeSpinner.setSelection(registrationTypeIndex(AylaDevice.RegistrationType.SameLan), true);
            }

        } else if (parent.getId() == R.id.spinner_registration_type) {
            // Update the display text
            int spinnerVisible = View.VISIBLE;
            boolean showGateways = false;
            int textId;
            _registrationType = __supportedRegTypes[position];
            switch (_registrationType) {
                case ButtonPush:
                default:
                    textId = R.string.registration_button_push_instructions;
                    break;

                case SameLan:
                    textId = R.string.registration_same_lan_instructions;
                    break;

                case Display:
                    textId = R.string.registration_display_instructions;
                    break;

                case Node: {
                    textId = R.string.gateway_find_devices;
                    showGateways = true;
                    List<AylaDevice> gateways = AMAPCore.sharedInstance().getDeviceManager()
                            .getDevices(new Predicate<AylaDevice>() {
                                @Override
                                public boolean apply(AylaDevice aylaDevice) {
                                    return aylaDevice.isGateway();
                                }
                            });

                    if ((gateways == null) || (gateways.size() == 0)) {
                        textId = R.string.error_no_gateway_instructions;
                        spinnerVisible = View.GONE;
                    }
                }
                    break;
            }
            _registerButton.setVisibility(showGateways ? View.GONE : View.VISIBLE);
            _descriptionTextView.setText(Html.fromHtml(getActivity().getResources().getString(textId)));
            _spinnerRegistrationTypeLabel.setText(getString(showGateways ? R.string.select_gateway : R.string.registration_type));
            _spinnerRegistrationTypeLabel.setVisibility(spinnerVisible);
            _spinnerRegistrationType.setVisibility(showGateways ? View.GONE : View.VISIBLE);
            _spinnerGatewaySelection.setVisibility(showGateways ? spinnerVisible : View.GONE);
        } else if (parent.getId() == R.id.spinner_gateway_selection) {
            GenericGateway gateway = (GenericGateway) parent.getAdapter().getItem(position);
            if (gateway != null) {
                Logger.logInfo(LOG_TAG, "rn: selected gateway [" + gateway.getDevice().getDsn() +
                        "]");
                _nodeRegistrationGateway = gateway;
            } else {
                Logger.logError(LOG_TAG, "rn: no gateway");
                _nodeRegistrationGateway = null;
            }
        }
        Logger.logInfo(LOG_TAG, "rn: Selected " + position);
    }

    private AylaDevice.RegistrationType getSelectedRegistrationType() {
        Spinner regTypeSpinner = (Spinner) getView().findViewById(R.id.spinner_registration_type);
        switch (__supportedRegTypes[regTypeSpinner.getSelectedItemPosition()]) {
            case SameLan:
                return AylaDevice.RegistrationType.SameLan;
            case ButtonPush:
                return AylaDevice.RegistrationType.ButtonPush;
            case Display:
                return AylaDevice.RegistrationType.Display;
            case Node:
                return AylaDevice.RegistrationType.Node;
        }
        return null;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Logger.logInfo(LOG_TAG, "rn: Nothing Selected");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// Node Scanning & Registration

    private GenericGateway _nodeRegistrationGateway;

    private void registerButtonClick() {
        // Register button clicked
        ViewModel d = (ViewModel) _spinnerProductType.getSelectedItem();
        if (USE_WELCOME_FRAGMENT && d.isGateway()) {
            MenuHandler.handleGatewayWelcome();
        } else if (_registrationType == AylaDevice.RegistrationType.Node) {
            // we need something to register...
            Logger.logError(LOG_TAG, "rn: Register node no device node to register!");
            Toast.makeText(MainActivity.getInstance(), R.string.error_gateway_register_no_device, Toast.LENGTH_LONG).show();
        } else {
            Logger.logInfo(LOG_TAG, "rn: registerNewDevice");
            MainActivity.getInstance().showWaitDialog(null, null);
            registerNewDevice(null);
        }
    }

    private void scanButtonClick() {

        //Check runtime permission before scanning.
        //Scanning requires permissions ACCESS_COARSE_LOCATION and ACCESS_FINE_LOCATION
        //TODO

        if(ActivityCompat.checkSelfPermission(getActivity(), "android.permission.ACCESS_COARSE_LOCATION") != PackageManager.PERMISSION_GRANTED){
            requestScanPermissions();
        } else{
            if (USE_WELCOME_FRAGMENT) {
                ViewModel provider = (ViewModel)
                        _spinnerProductType.getSelectedItem();
                if (provider instanceof GenericGateway) {
                    MenuHandler.handleGatewayWelcome();
                } else {
                    doScan();
                }
            } else {
                doScan();
            }
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
                scanButtonClick();
                break;
        }
    }

    //set cancelAylaSetup to false when exitSetup() is called based on an error message
    // returned from the library.
    private void exitSetup() {
        if (_aylaSetup != null) {
            _aylaSetup.exitSetup(new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                @Override
                public void onResponse(AylaAPIRequest.EmptyResponse response) {
                    AylaLog.d(LOG_TAG, "AylaSetup.exitSetup returned success");
                }
            }, new ErrorListener() {
                @Override
                public void onErrorResponse(AylaError error) {
                    AylaLog.e(LOG_TAG, "AylaSetup.exitSetup returned " + error);
                }
            });
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

    @Override
    public void gatewayRegistrationCandidateAdded(AylaDevice device, boolean moreComing, Object
            tag) {
        Toast.makeText(MainActivity.getInstance(), R.string.gateway_registered_device_node, Toast.LENGTH_LONG).show();
        Logger.logInfo(LOG_TAG, "rn: device [%s:%s] added", device.getDsn(),
                device.getProductName());

        if (!moreComing) {
            MainActivity.getInstance().onSelectMenuItemById(R.id.action_all_devices);
        }
    }

    /**
     * Set _useSingleSelect to true to show a dialog that only allows one device to selected
     * for registration.  Set to false to show a dialog with a multiselect list of devices to
     * register.
     */
    boolean _useSingleSelect = false;

    @Override
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
            apNames[i] = adn.getProductName();
            selected.add(adn);
        }

        /* Quicky test to see if Identify will work on the AylaDeviceNode candidate...
        if (list != null && list.size() > 0) {
            _nodeRegistrationGateway.identifyAylaDeviceNode(list.get(0), true, 255, this, null);
        }
        */

        if (_useSingleSelect) {
            new AlertDialog.Builder(getActivity())
                    .setIcon(R.drawable.ic_launcher)
                    .setTitle(R.string.choose_new_device)
                    .setSingleChoiceItems(apNames, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AylaDeviceNode adn = list.get(which);
                            Logger.logVerbose(LOG_TAG, "rn: register candidate [%s:%s]", adn.getDsn(), adn.getModel());
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
                            Logger.logVerbose(LOG_TAG, "rn: register candidate [%s:%s] %s", adn.getDsn(), adn.getModel(), (isChecked ? "YES" : "NO"));
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

    @Override
    public void gatewayRegistrationComplete(AylaError error, int messageResourceId, Object tag) {
        Logger.logDebug(LOG_TAG, "rn: gatewayRegistrationComplete, error: " + error);
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
        if (_registrationType == AylaDevice.RegistrationType.Node) {
            if (_nodeRegistrationGateway == null) {
                MenuHandler.handleGatewayWelcome();
            } else {
                // Put up a progress dialog
                if(_nodeRegistrationGateway.isOnline()){
                    MainActivity.getInstance().showWaitDialogWithCancel(getString(R.string.scanning_for_devices_title), getString(R.string.scanning_for_gateway_devices_message), this);
                    Logger.logInfo(LOG_TAG, "rn: startRegistration [" + _nodeRegistrationGateway
                            .getDevice().getDsn() + "]");
                    _scanTag = _nodeRegistrationGateway.startRegistrationScan(false, _nodeRegistrationGateway, this);
                } else{
                    Toast.makeText(getActivity(), getResources().getString(R.string.gateway_offline), Toast.LENGTH_LONG).show();
                }
            }
        } else {
            // Put up a progress dialog
            MainActivity.getInstance().showWaitDialog(getString(R.string.scanning_for_devices_title), getString(R.string.scanning_for_devices_message));
            Logger.logVerbose(LOG_TAG, "rn: returnHostScanForNewDevices");

            RequestFuture<ScanResult[]> hostScanFuture = RequestFuture.newFuture();
            AylaAPIRequest hostScanRequest = _aylaSetup.scanForAccessPoints(10, new Predicate<ScanResult>() {
                @Override
                public boolean apply(ScanResult scanResult) {
                    return scanResult.SSID.matches(DEFAULT_HOST_SCAN_REGEX);

                }
            },
            hostScanFuture, hostScanFuture);
            ScanResult[] results = null;
            try {
                results = hostScanFuture.get(10 * 1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Toast.makeText(MainActivity.getInstance(), "Failed to scan for Device AP. Exiting setup.", Toast.LENGTH_LONG).show();
                exitSetup();
                return;
            } catch (ExecutionException e) {
                Toast.makeText(MainActivity.getInstance(), "Failed to scan for Device AP. Exiting setup.", Toast.LENGTH_LONG).show();
                exitSetup();
                return;
            } catch (TimeoutException e) {
                Toast.makeText(MainActivity.getInstance(), "Failed to scan for Device AP. Exiting setup.", Toast.LENGTH_LONG).show();
                exitSetup();
                return;
            }

            if (results == null || results.length == 0) {
                Toast.makeText(MainActivity.getInstance(), "No Device AP found. Exiting setup.", Toast.LENGTH_LONG).show();
                exitSetup();
                return;
            }

            dismissWaitDialog();

            // Let the user choose which device to connect to
            final String apNames[] = new String[results.length];
            for ( int i = 0; i < results.length; i++ ) {
                apNames[i] = results[i].SSID;
            }

            new AlertDialog.Builder(getActivity())
                    .setIcon(R.drawable.ic_launcher)
                    .setTitle(R.string.choose_new_device)
                    .setSingleChoiceItems(apNames, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            connectToDeviceAP(apNames[which]);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()
                    .show();
        }
    }

    @Override
    public void choseAccessPoint(String accessPoint, String security, String password) {
        Logger.logDebug(LOG_TAG, "rn: choseAccessPoint: " + accessPoint + "[" + security + "]");
        if ( accessPoint == null ) {
            exitSetup();
        } else {
            connectDeviceToService(accessPoint, password, null); // TODO: GEN TOKEN
        }
    }

    private void connectDeviceToService(String ssid, String password, String setupToken) {
        MainActivity.getInstance().showWaitDialog(getString(R.string.connecting_to_network_title),
                getString(R.string.connecting_to_network_body));

        //adding location details
        LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean netEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean locationAcquired = false;
        double lat = 0;
        double lng = 0;

        if (gpsEnabled || netEnabled) {
            String locationProvider = locationManager.getBestProvider(criteria, false);

            if(ActivityCompat.checkSelfPermission(getActivity(), "android.permission.ACCESS_COARSE_LOCATION") != PackageManager.PERMISSION_GRANTED){
                requestScanPermissions();
            } else{
                Location location = locationManager.getLastKnownLocation(locationProvider);
                if(location != null) {
                    locationAcquired = true;
                    lat = location.getLatitude();
                    lng = location.getLongitude();
                }
            }
        } else {
            Toast.makeText(getActivity(), R.string.warning_location_accuracy, Toast.LENGTH_SHORT).show();
        }

        RequestFuture<AylaWifiStatus> reconnectFuture = RequestFuture.newFuture();
        AylaAPIRequest reconnectRequest = _aylaSetup.connectDeviceToService(ssid, password,
                setupToken,
                locationAcquired ? lat : null,
                locationAcquired ? lng : null,
                15,
                reconnectFuture,
                reconnectFuture);
        AylaWifiStatus status;
        try {
            status = reconnectFuture.get(15 * 1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Toast.makeText(MainActivity.getInstance(), "Failed to connect to service. Exiting setup.", Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        } catch (ExecutionException e) {
            Toast.makeText(MainActivity.getInstance(), "Failed to connect to service. Exiting setup.", Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        } catch (TimeoutException e) {
            Toast.makeText(MainActivity.getInstance(), "Failed to connect to service. Exiting setup.", Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        }

        if (status == null) {
            Toast.makeText(MainActivity.getInstance(), "Failed to connect to service. Exiting setup.", Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        }

        dismissWaitDialog();

        // Confirm service connection. We need to do this to get the device information
        // to register it.
        MainActivity.getInstance().showWaitDialog(R.string.confirm_new_device_title, R.string.confirm_new_device_body);
        confirmConnection(_setupDevice.getDsn(), setupToken);
    }

    private void connectToDeviceAP(String ssid) {
        MainActivity.getInstance().showWaitDialog(getString(R.string.connecting_to_device_title), getString(R.string.connecting_to_device_body));

        RequestFuture<AylaSetupDevice> setupFuture = RequestFuture.newFuture();
        AylaAPIRequest setupRequest = _aylaSetup.connectToNewDevice(ssid, 15,
                setupFuture, setupFuture);
        AylaSetupDevice setupDevice;
        try {
            setupDevice = setupFuture.get(15 * 1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Toast.makeText(getActivity(), R.string.wifi_connect_failed, Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        } catch (ExecutionException e) {
            Toast.makeText(getActivity(), R.string.wifi_connect_failed, Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        } catch (TimeoutException e) {
            Toast.makeText(getActivity(), R.string.wifi_connect_failed, Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        }

        // We've mucked with our wifi access point. Make sure we go back to our original
        // AP when we're done with setup, regardless of whether or not it succeeds.
        Logger.logDebug(LOG_TAG, "rn: set _needsExit");
        _needsExit = true;

        if (setupDevice == null) {
            Toast.makeText(getActivity(), R.string.wifi_connect_failed, Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        }
        _setupDevice = setupDevice;

        MainActivity activity = MainActivity.getInstance();
        dismissWaitDialog();

        Logger.logVerbose(LOG_TAG, "rn: calling getNewDeviceScanForAPs. newDevice = " + setupDevice.toString());
        deviceScanForNetworks();
        activity.showWaitDialog(activity.getString(R.string.scanning_for_aps_title), activity.getString(R.string.scanning_for_aps_body));
    }

    private void deviceScanForNetworks() {
        RequestFuture<AylaAPIRequest.EmptyResponse> future = RequestFuture.newFuture();
        AylaAPIRequest request = _aylaSetup.startDeviceScanForAccessPoints(
                future, future);
        try {
            AylaAPIRequest.EmptyResponse response = future.get(10 * 1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Toast.makeText(getActivity(), "Failed to connect mobile to original wifi network", Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        } catch (ExecutionException e) {
            Toast.makeText(getActivity(), "Failed to connect mobile to original wifi network", Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        } catch (TimeoutException e) {
            Toast.makeText(getActivity(), "Failed to connect mobile to original wifi network", Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        }

        fetchDeviceScannedNetworks();
    }

    private void fetchDeviceScannedNetworks() {
        final Predicate<AylaWifiScanResults.Result> filter =
                new Predicate<AylaWifiScanResults.Result>() {
                    @Override
                    public boolean apply(AylaWifiScanResults.Result result) {
                        return result.ssid != null && !result.ssid.startsWith("Ayla-");
                    }
                };

        RequestFuture<AylaWifiScanResults> future = RequestFuture.newFuture();
        AylaAPIRequest request = _aylaSetup.fetchDeviceAccessPoints(
                filter, future, future);
        AylaWifiScanResults results = null;
        try {
            results = future.get(10 * 1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Toast.makeText(getActivity(), "Failed to fetch Device scanned WiFi networks", Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        } catch (ExecutionException e) {
            Toast.makeText(getActivity(), "Failed to fetch Device scanned WiFi networks", Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        } catch (TimeoutException e) {
            Toast.makeText(getActivity(), "Failed to fetch Device scanned WiFi networks", Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        }

        if (results == null) {
            Toast.makeText(getActivity(), "Failed to fetch Device scanned WiFi networks", Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        }

        Logger.logDebug(LOG_TAG, "rn: chooseAP show");
        ChooseAPDialog _chooseAPDialog = ChooseAPDialog.newInstance(results.results, _ssid, _bssid);
        _chooseAPDialog.setTargetFragment(this, 0);
        _chooseAPDialog.show(getFragmentManager(), "ap");
    }

    private void confirmConnection(String dsn, String setupToken) {
        // Part 1: Reconnect mobile to original wifi network
        RequestFuture<AylaAPIRequest.EmptyResponse> emptyFuture = RequestFuture.newFuture();
        AylaAPIRequest request = _aylaSetup.reconnectToOriginalNetwork(10,
                emptyFuture, emptyFuture);
        try {
            AylaAPIRequest.EmptyResponse response = emptyFuture.get(10 * 1000,
                    TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Toast.makeText(getActivity(), "Failed to connect mobile to original wifi network", Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        } catch (ExecutionException e) {
            Toast.makeText(getActivity(), "Failed to connect mobile to original wifi network", Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        } catch (TimeoutException e) {
            Toast.makeText(getActivity(), "Failed to connect mobile to original wifi network", Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        }

        // Part 2: Check device connection to Ayla service
        RequestFuture<AylaSetupDevice> confirmFuture = RequestFuture.newFuture();
        AylaAPIRequest confirmRequest = _aylaSetup.confirmDeviceConnected(10, dsn, setupToken,
                confirmFuture, confirmFuture);
        AylaSetupDevice device = null;
        try {
            device = confirmFuture.get(10 * 1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Toast.makeText(getActivity(), "Failed to confirm device connection to Ayla service", Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        } catch (ExecutionException e) {
            Toast.makeText(getActivity(), "Failed to confirm device connection to Ayla service", Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        } catch (TimeoutException e) {
            Toast.makeText(getActivity(), "Failed to confirm device connection to Ayla service", Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        }

        if (device == null) {
            Toast.makeText(getActivity(), "Failed to confirm device connection to Ayla service", Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        }

        dismissWaitDialog();

        // Set up the new device if it's not already registered
        if (AMAPCore.sharedInstance().getDeviceManager().deviceWithDSN(device.getDsn()) == null) {
            if (_registrationType == AylaDevice.RegistrationType.ButtonPush) {
                // we need to prompt the user to press the Button for registration
                showPushButtonDialog(device);
            } else {
                fetchCandidateWithDsn(dsn);
            }
        } else {
            Logger.logDebug(LOG_TAG, "rn: device [" + device.getDsn() + "] already registered");
            MainActivity.getInstance().popBackstackToRoot();
            Toast.makeText(MainActivity.getInstance(), R.string.connect_to_service_success, Toast.LENGTH_LONG).show();
        }
    }

    private void fetchCandidateWithDsn(String dsn) {
        AylaDevice.RegistrationType regType = AylaDevice.RegistrationType.valueOf("SameLan");
        RequestFuture<AylaRegistrationCandidate> candidateFuture = RequestFuture.newFuture();
        AylaAPIRequest candidateRequest = _aylaRegistration.fetchCandidate(dsn, regType,
                candidateFuture,
                candidateFuture);
        AylaRegistrationCandidate candidate = null;
        try {
            candidate = candidateFuture.get(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Toast.makeText(getActivity(), "Failed to fetch registration candidate", Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        } catch (ExecutionException e) {
            Toast.makeText(getActivity(), "Failed to fetch registration candidate", Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        } catch (TimeoutException e) {
            Toast.makeText(getActivity(), "Failed to fetch registration candidate", Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        }

        if (candidate == null) {
            Toast.makeText(getActivity(), "Failed to fetch registration candidate", Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        }

        registerCandidate(candidate, dsn);
    }

    /* Register the device candidate */
    private void registerCandidate(AylaRegistrationCandidate candidate, String dsn) {
        candidate.setDsn(dsn);
        candidate.setRegistrationToken("");
        candidate.setRegistrationType(AylaDevice.RegistrationType.valueOf("SameLan"));

        RequestFuture<AylaDevice> registrationFuture = RequestFuture.newFuture();
        final AylaAPIRequest registrationRequest = _aylaRegistration.registerCandidate(candidate,
                registrationFuture,
                registrationFuture);
        AylaDevice registeredDevice;
        try {
            registeredDevice = registrationFuture.get(10 * 1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Toast.makeText(getActivity(), "Failed to register candidate", Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        } catch (ExecutionException e) {
            Toast.makeText(getActivity(), "Failed to register candidate", Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        } catch (TimeoutException e) {
            Toast.makeText(getActivity(), "Failed to register candidate", Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        }

        if (registeredDevice == null) {
            Toast.makeText(getActivity(), "Failed to register candidate", Toast.LENGTH_LONG).show();
            exitSetup();
            return;
        }

        dismissWaitDialog();

        MainActivity.getInstance().showWaitDialog(R.string.updating_notifications_title, R.string.updating_notifications_body);

        // Now update the device notifications
        DeviceNotificationHelper helper = new DeviceNotificationHelper(registeredDevice, AylaUser.getCurrent());
        helper.initializeNewDeviceNotifications(new DeviceNotificationHelper.DeviceNotificationHelperListener() {
            @Override
            public void newDeviceUpdated(AylaDevice device, int error) {
                MainActivity mainActivity = MainActivity.getInstance();
                mainActivity.dismissWaitDialog();

                int msgId = (error == AylaNetworks.AML_ERROR_OK ? R.string.registration_success : R.string.registration_success_notification_fail);
                Toast.makeText(mainActivity, msgId, Toast.LENGTH_LONG).show();
                SessionManager.deviceManager().refreshDeviceList();

                if (error == AylaNetworks.AML_ERROR_OK) {
                    // TODO: Major! unfortunately, we aren't able to transition from nodevicesmode to having devices!
                    MenuHandler.handleAllDevices();
                }
            }
        });
    }

    private void registerNewDevice(String dsn) {
        MainActivity.getInstance().showWaitDialog(R.string.registering_device_title, R.string.registering_device_body);
        fetchCandidateWithDsn(dsn);
    }

    private void showPushButtonDialog(final AylaDevice device) {
        new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_launcher)
                .setTitle(getString(R.string.attention))
                .setMessage(getString(R.string.push_registration_button))
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        exitSetup();
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        registerDevice(device);
                    }
                })
                .create()
                .show();
    }

    private void registerDevice(final AylaDevice device) {
        // We need to wait a bit before attempting to register. The service needs some
        // time to get itself in order first.
        MainActivity.getInstance().showWaitDialog(R.string.registering_device_title, R.string.registering_device_body);
        Logger.logDebug(LOG_TAG, "rn: register new device [" + device.getDsn() + "] after delay");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Logger.logDebug(LOG_TAG, "rn: register new device [" + device.getDsn() + "]");
                registerNewDevice(device.getDsn());
            }
        }, REGISTRATION_DELAY_MS);
    }

    /*
    * Scan needs location permissions. This method requests Location permission
     */
    private void requestScanPermissions(){
        ActivityCompat.requestPermissions(getActivity(), new String[]{"android.permission.ACCESS_COARSE_LOCATION"}, REQUEST_LOCATION);
    }
}
