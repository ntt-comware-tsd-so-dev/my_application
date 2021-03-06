package com.aylanetworks.agilelink.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.Manifest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.InputType;
import android.text.TextUtils;
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
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.util.Predicate;
import com.android.volley.Response;

import com.aylanetworks.agilelink.ErrorUtils;
import com.aylanetworks.agilelink.device.AMAPViewModelProvider;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.ViewModel;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.device.GenericGateway;
import com.aylanetworks.agilelink.fragments.adapters.DeviceTypeAdapter;
import com.aylanetworks.agilelink.framework.DeviceNotificationHelper;
import com.aylanetworks.agilelink.framework.Logger;
import com.aylanetworks.agilelink.MenuHandler;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaConnectivity;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaDeviceGateway;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.setup.AylaRegistration;
import com.aylanetworks.aylasdk.setup.AylaRegistrationCandidate;
import com.aylanetworks.aylasdk.setup.AylaSetup;
import com.aylanetworks.aylasdk.setup.AylaSetupDevice;
import com.aylanetworks.aylasdk.setup.AylaWifiScanResults;
import com.aylanetworks.aylasdk.setup.AylaWifiStatus;
import com.aylanetworks.aylasdk.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * AddDeviceFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 1/21/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */
public class AddDeviceFragment extends Fragment
        implements AdapterView.OnItemSelectedListener,
        View.OnClickListener,
        ChooseAPDialog.ChooseAPResults,
        DialogInterface.OnCancelListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        AylaSetup.DeviceWifiStateChangeListener{

    private static final String LOG_TAG = "AddDeviceFragment";
    private static final String ZIGBEE_PRODUCT_CLASS = "zigbee";
    private static final int REQUEST_LOCATION = 2;

    private static final boolean USE_WELCOME_FRAGMENT = true;

    /** Time to delay after completing wifi setup and trying to register the new device */
    private static final int REGISTRATION_DELAY_MS = 9000;

    private static final AylaDevice.RegistrationType[] __supportedRegTypes = {AylaDevice
            .RegistrationType.SameLan,
            AylaDevice.RegistrationType.ButtonPush,
            AylaDevice.RegistrationType.Display,
            AylaDevice.RegistrationType.APMode,
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
    private Button _scanButton;
    private TextView _spinnerRegistrationTypeLabel;
    private Spinner _spinnerRegistrationType;
    private Spinner _spinnerGatewaySelection;
    private TextView _descriptionTextView;
    private AylaDevice.RegistrationType _registrationType = AylaDevice.RegistrationType.SameLan;
    private Spinner _spinnerProductType;
    private static boolean _needsExit;
    private AylaRegistration _aylaRegistration;
    private AylaSetup _aylaSetup;
    private String _setupToken;

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
            AMAPViewModelProvider dc = (AMAPViewModelProvider)AMAPCore.sharedInstance()
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

        // Hook up the "Register" button
        _registerButton = (Button) view.findViewById(R.id.register_button);
        _registerButton.setOnClickListener(this);

        // Hook up the "Scan" button
        _scanButton = (Button) view.findViewById(R.id.scan_button);
        _scanButton.setOnClickListener(this);

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

        if (_aylaSetup != null) {
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

    private ArrayAdapter<ViewModel> createGatewayAdapter() {
        List<AylaDevice> gatewayDevices = AMAPCore.sharedInstance().getDeviceManager()
                .getDevices(new Predicate<AylaDevice>() {
                    @Override
                    public boolean apply(AylaDevice aylaDevice) {
                        return aylaDevice.isGateway() &&
                                !(ZIGBEE_PRODUCT_CLASS.equals(aylaDevice.getProductClass()));
                    }
                });

        List<ViewModel> gateways = ViewModel.fromDeviceList(gatewayDevices);
        return new GenericGateway.GatewayTypeAdapter(getActivity(), gateways.toArray(new
                ViewModel[gateways.size()]), true);
    }

    private ArrayAdapter<ViewModel> createProductTypeAdapter() {
        AMAPViewModelProvider viewModelProvider =
                (AMAPViewModelProvider)AMAPCore.sharedInstance().getSessionParameters()
                        .viewModelProvider;

        List<Class<? extends ViewModel>> deviceClasses = viewModelProvider.getSupportedDeviceClasses();
        ArrayList<ViewModel> deviceList = new ArrayList<>();
        for (Class<? extends ViewModel> c : deviceClasses) {
            try {
                AylaDevice fakeDevice = new AylaDevice();
                ViewModel d = c.getConstructor(AylaDevice.class).newInstance(fakeDevice);
                deviceList.add(d);
            } catch (NoSuchMethodException e) {
               try {
                   AylaDeviceGateway fakeGateway = new AylaDeviceGateway();
                   ViewModel d = c.getConstructor(AylaDeviceGateway.class).newInstance(fakeGateway);
                   deviceList.add(d);
               } catch (Exception exc) {
                   exc.printStackTrace();
               }
            } catch (Exception ex) {
                ex.printStackTrace();
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

                case APMode:
                    //The instructions are same as display. No need of any extra instructions
                    textId = R.string.registration_display_instructions;
                    break;

                case Node: {
                    textId = R.string.gateway_find_devices;
                    showGateways = true;
                    List<AylaDevice> gateways = AMAPCore.sharedInstance().getDeviceManager()
                            .getDevices(new Predicate<AylaDevice>() {
                                @Override
                                public boolean apply(AylaDevice aylaDevice) {
                                    return aylaDevice.isGateway() &&
                                            !(ZIGBEE_PRODUCT_CLASS.equals(aylaDevice.getProductClass()));
                                }
                            });

                    if ((gateways == null) || (gateways.size() == 0)) {
                        textId = R.string.error_no_gateway_instructions;
                        spinnerVisible = View.GONE;
                    }
                }
                    break;
            }
            boolean showRegButton = true;
            if(showGateways || _registrationType == AylaDevice.RegistrationType.APMode) {
                showRegButton = false;
            }
            _registerButton.setVisibility(showRegButton ? View.VISIBLE : View.GONE);
            _descriptionTextView.setText(Html.fromHtml(getActivity().getResources().getString(textId)));
            _spinnerRegistrationTypeLabel.setText(getString(showGateways ? R.string.select_gateway : R.string.registration_type));
            _spinnerRegistrationTypeLabel.setVisibility(spinnerVisible);
            _spinnerRegistrationType.setVisibility(showGateways ? View.GONE : View.VISIBLE);
            _spinnerGatewaySelection.setVisibility(showGateways ? spinnerVisible : View.GONE);
            if (showGateways && spinnerVisible == View.GONE) {
                // They want to register a node, but have no gateways. Don't let them.
                _scanButton.setEnabled(false);
            } else {
                _scanButton.setEnabled(true);
            }
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
        } else if (_registrationType == AylaDevice.RegistrationType.Display) {
            showDisplayRegDialog(null);
        } else if (_registrationType == AylaDevice.RegistrationType.ButtonPush) {
            showPushButtonDialog(null);
        } else {
            Logger.logInfo(LOG_TAG, "rn: registerNewDevice");
            MainActivity.getInstance().showWaitDialog(null, null);
            fetchCandidateAndRegister(null, AylaDevice.RegistrationType.SameLan, null);
        }
    }

    private void scanButtonClick() {

        //Check runtime permission before scanning.
        //Scanning requires permissions ACCESS_COARSE_LOCATION and ACCESS_FINE_LOCATION
        //TODO

        if(ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestScanPermissions();
        } else{
            try {
                _aylaSetup = new AylaSetup(AMAPCore.sharedInstance().getContext(),
                        AMAPCore.sharedInstance().getSessionManager());
                _aylaSetup.addListener(this);
            } catch (AylaError aylaError) {
                AylaLog.e(LOG_TAG, "Failed to create AylaSetup object: " + aylaError);
                Toast.makeText(AMAPCore.sharedInstance().getContext(), aylaError.toString(),
                        Toast.LENGTH_LONG).show();
            }

            ViewModel provider = (ViewModel) _spinnerProductType.getSelectedItem();
            if (USE_WELCOME_FRAGMENT && provider.isGateway()) {
                MenuHandler.handleGatewayWelcome();
            } else if (provider.isNode()) {
                _nodeRegistrationGateway.getGateway().fetchRegistrationCandidates(new Response.Listener<AylaRegistrationCandidate[]>() {
                        @Override
                            public void onResponse(AylaRegistrationCandidate[] response) {
                                presentNodeCandidates(Arrays.asList(response));
                            }
                        },
                        new ErrorListener() {
                            @Override
                            public void onErrorResponse(AylaError error) {
                                Toast.makeText(getActivity(),
                                        ErrorUtils.getUserMessage(error, "Error fetching node registration candidates"),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
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
        dismissWaitDialog();

        if (_aylaSetup != null) {
            _aylaSetup.removeListener(this);
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

    /**
     * Set _useSingleSelect to true to show a dialog that only allows one device to selected
     * for registration.  Set to false to show a dialog with a multiselect list of devices to
     * register.
     */
    boolean _useSingleSelect = false;

    public void presentNodeCandidates(final List<AylaRegistrationCandidate> list) {
        dismissWaitDialog();

        // Let the user choose which device to connect to
        String apNames[] = new String[list.size()];

        final List<AylaRegistrationCandidate> selected = new ArrayList<>();
        boolean[] isSelectedArray = new boolean[list.size()];
        for ( int i = 0; i < list.size(); i++ ) {
            isSelectedArray[i] = true;
            AylaRegistrationCandidate adn = list.get(i);
            Logger.logVerbose(LOG_TAG, "rn: candidate [%s]", adn);
            apNames[i] = adn.getProductName();
            selected.add(adn);
        }

        if (_useSingleSelect) {
            new AlertDialog.Builder(getActivity())
                    .setIcon(R.drawable.ic_launcher)
                    .setTitle(R.string.choose_new_device)
                    .setSingleChoiceItems(apNames, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AylaRegistrationCandidate adn = list.get(which);
                            Logger.logVerbose(LOG_TAG, "rn: register candidate [%s:%s]", adn.getDsn(), adn.getModel());
                            fetchCandidateAndRegister(adn.getDsn(), AylaDevice.RegistrationType.Node, null);
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
                            AylaRegistrationCandidate adn = list.get(which);
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
                            }

                            // Register the nodes
                            registerCandidates(selected);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create().show();
        }
    }

    private void registerCandidates(final List<AylaRegistrationCandidate> candidates) {
        if (candidates.size() == 0) {
            // We're done
            dismissWaitDialog();
            showMessage(R.string.registration_success);
            return;
        }

        AylaRegistrationCandidate candidate = candidates.remove(0);
        _aylaRegistration.registerCandidate(candidate, new Response.Listener<AylaDevice>() {
                    @Override
                    public void onResponse(AylaDevice response) {
                        registerCandidates(candidates);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        dismissWaitDialog();
                        showMessage(R.string.error_register_candidate);
                        Log.e(LOG_TAG, "Error registering node candidate: " + error);
                    }
                });
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        //
    }

    private void doScan() {
        AylaConnectivity connectivity = new AylaConnectivity(getContext());
        if(!connectivity.isWifiEnabled()){
            Toast.makeText(getContext(), getString(R.string.error_wifi_not_enabled),
                    Toast.LENGTH_SHORT).show();
            return;
        }


        if (_registrationType == AylaDevice.RegistrationType.Node) {
            if (_nodeRegistrationGateway == null) {
                MenuHandler.handleGatewayWelcome();
            } else {
                // Put up a progress dialog
                if(_nodeRegistrationGateway.isOnline()){
                    MainActivity.getInstance().showWaitDialogWithCancel(getString(R.string.scanning_for_devices_title), getString(R.string.scanning_for_gateway_devices_message), this);
                    Logger.logInfo(LOG_TAG, "rn: startRegistration [" + _nodeRegistrationGateway
                            .getDevice().getDsn() + "]");
                } else{
                    Toast.makeText(getActivity(), getResources().getString(R.string.gateway_offline), Toast.LENGTH_LONG).show();
                }
            }
        } else {
            // Put up a progress dialog
            MainActivity.getInstance().showWaitDialog(getString(R.string.scanning_for_devices_title), getString(R.string.scanning_for_devices_message));
            Logger.logVerbose(LOG_TAG, "rn: returnHostScanForNewDevices");

            _aylaSetup.scanForAccessPoints(10, new Predicate<ScanResult>() {
                        @Override
                        public boolean apply(ScanResult scanResult) {
                            return scanResult.SSID.matches(DEFAULT_HOST_SCAN_REGEX);
                        }
                    },
                    new Response.Listener<ScanResult[]>() {
                        @Override
                        public void onResponse(ScanResult[] results) {
                            dismissWaitDialog();
                            if (results.length == 0) {
                                new AlertDialog.Builder(getActivity())
                                        .setIcon(R.drawable.ic_launcher)
                                        .setTitle(R.string.no_devices)
                                        .setMessage(R.string.no_devices_found)
                                        .setNegativeButton(android.R.string.ok, null)
                                        .create()
                                        .show();
                            } else {
                                // Let the user choose which device to connect to
                                final String apNames[] = new String[results.length];
                                for (int i = 0; i < results.length; i++) {
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
                    },
                    new ErrorListener() {
                        @Override
                        public void onErrorResponse(AylaError error) {
                            Toast.makeText(getActivity(),
                                    ErrorUtils.getUserMessage(getContext(), error, R.string.error_device_scan),
                                    Toast.LENGTH_LONG).show();
                            exitSetup();
                        }
                    });
        }
    }

    @Override
    public void choseAccessPoint(String accessPoint, String security, String password) {
        Logger.logDebug(LOG_TAG, "rn: choseAccessPoint: " + accessPoint + "[" + security + "]");
        if ( accessPoint == null ) {
            exitSetup();
        } else {
            _setupToken = ObjectUtils.generateRandomToken(8);
            connectDeviceToService(accessPoint, password, _setupToken);
        }
    }

    private void connectDeviceToService(String ssid, String password, final String setupToken) {
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

            if(ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
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

        _aylaSetup.connectDeviceToService(ssid, password,
                setupToken,
                locationAcquired ? lat : null,
                locationAcquired ? lng : null,
                15,
                new Response.Listener<AylaWifiStatus>() {
                    @Override
                    public void onResponse(AylaWifiStatus response) {
                        dismissWaitDialog();

                        // Confirm service connection. We need to do this to get the device information
                        // to register it.
                        connectMobileToOriginalNetworkAndConfirmDeviceConnection(_setupDevice.getDsn(), setupToken);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        dismissWaitDialog();

                        // Due to device bug and poor wifi reliability, we ignore the error and directly check
                        // with ayla service for the device connection status
                        connectMobileToOriginalNetworkAndConfirmDeviceConnection(_setupDevice.getDsn(), setupToken);
                    }
                });
    }

    private void connectToDeviceAP(String ssid) {
        MainActivity.getInstance().showWaitDialog(getString(R.string.connecting_to_device_title), getString(R.string.connecting_to_device_body));

        _aylaSetup.connectToNewDevice(ssid, 15,
                new Response.Listener<AylaSetupDevice>() {
                    @Override
                    public void onResponse(AylaSetupDevice setupDevice) {
                        // We've mucked with our wifi access point. Make sure we go back to our original
                        // AP when we're done with setup, regardless of whether or not it succeeds.
                        Logger.logDebug(LOG_TAG, "rn: set _needsExit");
                        _needsExit = true;
                        _setupDevice = setupDevice;

                        dismissWaitDialog();
                        Logger.logVerbose(LOG_TAG, "rn: calling getNewDeviceScanForAPs. newDevice = " + setupDevice.toString());
                        deviceScanForNetworks();
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Toast.makeText(getActivity(),
                                ErrorUtils.getUserMessage(getContext(), error, R.string.retry_setup),
                                Toast.LENGTH_LONG).show();
                        exitSetup();
                    }
                });
    }

    private void deviceScanForNetworks() {
        MainActivity.getInstance().showWaitDialog(getActivity().getString(R.string.scanning_for_aps_title), getActivity().getString(R.string.scanning_for_aps_body));

        _aylaSetup.startDeviceScanForAccessPoints(
                new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                    @Override
                    public void onResponse(AylaAPIRequest.EmptyResponse response) {
                        fetchDeviceScannedNetworks();
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Toast.makeText(getActivity(),
                                ErrorUtils.getUserMessage(getContext(), error, R.string.unknown_error),
                                Toast.LENGTH_LONG).show();
                        exitSetup();
                    }
                });
    }

    private void fetchDeviceScannedNetworks() {
        final Predicate<AylaWifiScanResults.Result> filter =
                new Predicate<AylaWifiScanResults.Result>() {
                    @Override
                    public boolean apply(AylaWifiScanResults.Result result) {
                        return result.ssid != null && !result.ssid.startsWith("Ayla-");
                    }
                };


        _aylaSetup.fetchDeviceAccessPoints(filter,
                new Response.Listener<AylaWifiScanResults>() {
                    @Override
                    public void onResponse(AylaWifiScanResults results) {
                        Logger.logDebug(LOG_TAG, "rn: chooseAP show");
                        dismissWaitDialog();
                        ChooseAPDialog _chooseAPDialog = ChooseAPDialog.newInstance(results.results, _ssid, _bssid);
                        _chooseAPDialog.setTargetFragment(AddDeviceFragment.this, 0);
                        _chooseAPDialog.show(getFragmentManager(), "ap");
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Toast.makeText(getActivity(),
                                ErrorUtils.getUserMessage(getContext(), error, R.string.unknown_error),
                                Toast.LENGTH_LONG).show();
                        exitSetup();
                    }
                });
    }

    private void connectMobileToOriginalNetworkAndConfirmDeviceConnection(final String dsn, final String setupToken) {
        MainActivity.getInstance().showWaitDialog(R.string.confirm_new_device_title, R.string.confirm_new_device_body);

        _aylaSetup.reconnectToOriginalNetwork(20,
                new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                    @Override
                    public void onResponse(AylaAPIRequest.EmptyResponse response) {
                        confirmDeviceConnection(dsn, setupToken);
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Toast.makeText(getActivity(),
                                ErrorUtils.getUserMessage(getContext(), error, R.string.unknown_error),
                                Toast.LENGTH_LONG).show();
                        exitSetup();
                    }
                });
    }

    private void confirmDeviceConnection(final String dsn, String setupToken) {
        _aylaSetup.confirmDeviceConnected(10, dsn, setupToken,
                new Response.Listener<AylaSetupDevice>() {
                    @Override
                    public void onResponse(AylaSetupDevice device) {
                        dismissWaitDialog();

                        // Set up the new device if it's not already registered
                        if (AMAPCore.sharedInstance().getDeviceManager().deviceWithDSN(device.getDsn()) == null) {
                            if (_registrationType == AylaDevice.RegistrationType.ButtonPush) {
                                // we need to prompt the user to press the Button for registration
                                showPushButtonDialog(dsn);
                            } else if (_registrationType == AylaDevice.RegistrationType.Display) {
                                showDisplayRegDialog(dsn);
                            } else if (_registrationType == AylaDevice.RegistrationType.APMode) {
                                AylaRegistrationCandidate candidate = new AylaRegistrationCandidate();
                                candidate.setDsn(dsn);
                                registerCandidate(candidate,dsn,AylaDevice.RegistrationType.APMode, null);
                            } else {
                                fetchCandidateAndRegister(dsn, AylaDevice.RegistrationType.SameLan, null);
                            }
                        } else {
                            Logger.logDebug(LOG_TAG, "rn: device [" + device.getDsn() + "] already registered");
                            MainActivity.getInstance().popBackstackToRoot();
                            Toast.makeText(MainActivity.getInstance(), R.string.connect_to_service_success, Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Toast.makeText(getActivity(),
                                ErrorUtils.getUserMessage(getContext(), error, R.string.error_confirm_device),
                                Toast.LENGTH_LONG).show();
                        exitSetup();
                    }
                });
    }

    private void fetchCandidateAndRegister(final String dsn, final AylaDevice.RegistrationType regType, final String regToken) {
        MainActivity.getInstance().showWaitDialog(R.string.registering_device_title, R.string.registering_device_body);

        _aylaRegistration.fetchCandidate(dsn, regType,
                new Response.Listener<AylaRegistrationCandidate>() {
                    @Override
                    public void onResponse(AylaRegistrationCandidate candidate) {
                        registerCandidate(candidate, dsn, regType, regToken);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        if(isAdded()){
                            Toast.makeText(getActivity(),
                                    ErrorUtils.getUserMessage(getContext(), error, R.string.error_fetch_candidates),
                                    Toast.LENGTH_LONG).show();
                        }
                        exitSetup();
                    }
                });
    }

    private void registerCandidate(AylaRegistrationCandidate candidate, String dsn, AylaDevice.RegistrationType regType, String regToken) {
        if (regToken != null) {
            candidate.setRegistrationToken(regToken);
        } else if (TextUtils.equals(dsn, candidate.getDsn()) || regType == AylaDevice.RegistrationType.APMode) {
            candidate.setSetupToken(_setupToken);
        } else {
            candidate.setRegistrationToken(ObjectUtils.generateRandomToken(8));
        }
        candidate.setRegistrationType(regType);

        // This is optional. Add location information to send latitude and longitude during
        // registration of device.
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestScanPermissions();
        } else {
            LocationManager locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            Location currentLocation;
            List<String> locationProviders = locationManager.getAllProviders();
            for (String provider: locationProviders) {
                currentLocation = locationManager.getLastKnownLocation(provider);
                if (currentLocation != null) {
                    candidate.setLatitude(String.valueOf(currentLocation.getLatitude()));
                    candidate.setLongitude(String.valueOf(currentLocation.getLongitude()));
                    break;
                }
            }
        }

        _aylaRegistration.registerCandidate(candidate,
                new Response.Listener<AylaDevice>() {
                    @Override
                    public void onResponse(AylaDevice registeredDevice) {
                        dismissWaitDialog();

                        MainActivity.getInstance().showWaitDialog(R.string.updating_notifications_title, R.string.updating_notifications_body);

                        // Now update the device notifications
                        DeviceNotificationHelper helper = new DeviceNotificationHelper(registeredDevice);
                        helper.initializeNewDeviceNotifications(new DeviceNotificationHelper.DeviceNotificationHelperListener() {
                            @Override
                            public void newDeviceUpdated(AylaDevice device, AylaError error) {
                                MainActivity mainActivity = MainActivity.getInstance();
                                mainActivity.dismissWaitDialog();
                                MainActivity.getInstance().getSupportFragmentManager().popBackStack();

                                if (error != null) {
                                    AMAPCore.sharedInstance().getDeviceManager().fetchDevices();
                                }
                            }
                        });
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Toast.makeText(getActivity(),
                                ErrorUtils.getUserMessage(getContext(), error, R.string.error_register_candidate),
                                Toast.LENGTH_LONG).show();
                        exitSetup();
                    }
                });
    }

    private void showPushButtonDialog(final String dsn) {
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
                        fetchCandidateAndRegister(dsn, AylaDevice.RegistrationType.ButtonPush, null);
                    }
                })
                .create()
                .show();
    }

    private void showDisplayRegDialog(String dsn) {
        final EditText regTokenEditText = new EditText(getContext());
        regTokenEditText.setInputType(InputType.TYPE_CLASS_TEXT );
        new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_launcher)
                .setTitle(R.string.enter_reg_token)
                .setView(regTokenEditText)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        exitSetup();
                    }
                })
                .setPositiveButton(R.string.register, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String token = regTokenEditText.getText().toString();
                        fetchCandidateAndRegister(null, AylaDevice.RegistrationType.Display, token);
                    }
                })
                .create()
                .show();
    }

    // TODO: IMPLEMENT
    /**
    private void registerDevice(final AylaDevice device) {
        // We need to wait a bit before attempting to register. The service needs some
        // time to get itself in order first.
        MainActivity.getInstance().showWaitDialog(R.string.registering_device_title, R.string.registering_device_body);
        Logger.logDebug(LOG_TAG, "rn: register new device [" + device.getDsn() + "] after delay");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Logger.logDebug(LOG_TAG, "rn: register new device [" + device.getDsn() + "]");
                fetchCandidateAndRegister(device.getDsn());
            }
        }, REGISTRATION_DELAY_MS);
    }
     */

    /*
    * Scan needs location permissions. This method requests Location permission
     */
    private void requestScanPermissions(){
        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
    }

    @Override
    public void wifiStateChanged(String currentState) {
        if(currentState != null){
            MainActivity.getInstance().updateDialogText(String.format("%s: %s", getString(R.string.device_wifi_state),
                    currentState));
        }
    }
}
