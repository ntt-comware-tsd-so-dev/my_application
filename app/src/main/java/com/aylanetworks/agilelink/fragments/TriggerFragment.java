package com.aylanetworks.agilelink.fragments;
/* 
 * RemoteFragment
 * AgileLink Application Framework
 *
 * Created by David N. Junod on 6/18/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.zigbee.AylaBindingZigbee;
import com.aylanetworks.aaml.zigbee.AylaGroupZigbee;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.device.ZigbeeTriggerDevice;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.DeviceManager;
import com.aylanetworks.agilelink.framework.Gateway;
import com.aylanetworks.agilelink.framework.Logger;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class TriggerFragment extends Fragment implements View.OnClickListener, DeviceManager.GetDeviceComparable {

    public final static String LOG_TAG = "TriggerFragment";

    private final static int FRAGMENT_RESOURCE_ID = R.layout.fragment_trigger_manage;

    private static final String ARG_DSN = "dsn";

    Gateway _gateway;
    ZigbeeTriggerDevice _device;

    AylaGroupZigbee _openTurnOnGroup;
    AylaBindingZigbee _openTurnOnBinding;
    AylaGroupZigbee _openTurnOffGroup;
    AylaBindingZigbee _openTurnOffBinding;
    AylaGroupZigbee _closeTurnOnGroup;
    AylaBindingZigbee _closeTurnOnBinding;
    AylaGroupZigbee _closeTurnOffGroup;
    AylaBindingZigbee _closeTurnOffBinding;
    int _setupErrors;
    int _fixCount;
    View _errorContainer;
    TextView _errorMessage;

    ListView _listView;
    ArrayAdapter<String> _adapter;
    TextView _titleView;
    TextView _dsnView;
    ImageView _imageView;
    RadioButton _onSensorRadio;
    RadioButton _offSensorRadio;
    Button _turnOn;
    Button _turnOff;
    Button _removeOn;
    Button _removeOff;

    public static TriggerFragment newInstance(Device device) {
        TriggerFragment frag = new TriggerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DSN, device.getDeviceDsn());
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setHasOptionsMenu(true);
        _device = null;
        if (getArguments() != null ) {
            String dsn = getArguments().getString(ARG_DSN);
            _device = (ZigbeeTriggerDevice)SessionManager.deviceManager().deviceByDSN(dsn);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(FRAGMENT_RESOURCE_ID, container, false);

        if (( _device == null) || !_device.isDeviceNode()) {
            Logger.logError(LOG_TAG, "No device specified");
            getFragmentManager().popBackStack();
            Toast.makeText(getActivity(), R.string.unknown_error, Toast.LENGTH_SHORT).show();
        } else {
            _gateway = Gateway.getGatewayForDeviceNode(_device);

            // group/binding error message area
            _errorContainer = view.findViewById(R.id.error_container);
            Button errorButton = (Button)view.findViewById(R.id.error_fix);
            errorButton.setOnClickListener(this);
            _errorMessage = (TextView)view.findViewById(R.id.error_message);

            _listView = (ListView) view.findViewById(R.id.listView);
            _titleView = (TextView) view.findViewById(R.id.device_name);
            _dsnView = (TextView) view.findViewById(R.id.device_dsn);
            _imageView = (ImageView) view.findViewById(R.id.device_image);

            _onSensorRadio  = (RadioButton) view.findViewById(R.id.radio_turn_on);
            _onSensorRadio.setOnClickListener(this);
            _offSensorRadio  = (RadioButton) view.findViewById(R.id.radio_turn_off);
            _offSensorRadio.setOnClickListener(this);

            _turnOn = (Button) view.findViewById(R.id.action_turn_on_add);
            _turnOn.setOnClickListener(this);

            _turnOff = (Button) view.findViewById(R.id.action_turn_off_add);
            _turnOff.setOnClickListener(this);

            _removeOn = (Button) view.findViewById(R.id.action_turn_on_remove);
            _removeOn.setOnClickListener(this);

            _removeOff = (Button) view.findViewById(R.id.action_turn_off_remove);
            _removeOff.setOnClickListener(this);

            updateGroups();
        }
        return view;
    }

    private void updateGroups() {
        String key;
        int errors = 0;

        key = ZigbeeTriggerDevice.makeGroupKeyForSensor(_device, true, true);
        _openTurnOnGroup = _device.getTriggerGroupByName(key);
        if (_openTurnOnGroup == null) {
            Logger.logError(LOG_TAG, "tf: no group found for " + key);
            errors++;
        }
        _openTurnOnBinding = _device.getTriggerBindingByName(key);
        if (_openTurnOnBinding == null) {
            Logger.logError(LOG_TAG, "tf: no binding found for " + key);
            errors++;
        } else {
            Logger.logDebug(LOG_TAG, "tf: binding " + _openTurnOnBinding);
        }

        key = ZigbeeTriggerDevice.makeGroupKeyForSensor(_device, true, false);
        _openTurnOffGroup = _device.getTriggerGroupByName(key);
        if (_openTurnOffGroup == null) {
            Logger.logError(LOG_TAG, "tf: no group found for " + key);
            errors++;
        }
        _openTurnOffBinding = _device.getTriggerBindingByName(key);
        if (_openTurnOffBinding == null) {
            Logger.logError(LOG_TAG, "tf: no binding found for " + key);
            errors++;
        } else {
            Logger.logDebug(LOG_TAG, "tf: binding " + _openTurnOffBinding);
        }

        key = ZigbeeTriggerDevice.makeGroupKeyForSensor(_device, false, true);
        _closeTurnOnGroup = _device.getTriggerGroupByName(key);
        if (_closeTurnOnGroup == null) {
            Logger.logError(LOG_TAG, "tf: no group found for " + key);
            errors++;
        }
        _closeTurnOnBinding = _device.getTriggerBindingByName(key);
        if (_closeTurnOnBinding == null) {
            Logger.logError(LOG_TAG, "tf: no binding found for " + key);
            errors++;
        } else {
            Logger.logDebug(LOG_TAG, "tf: binding " + _closeTurnOnBinding);
        }

        key = ZigbeeTriggerDevice.makeGroupKeyForSensor(_device, false, false);
        _closeTurnOffGroup = _device.getTriggerGroupByName(key);
        if (_closeTurnOffGroup == null) {
            Logger.logError(LOG_TAG, "tf: no group found for " + key);
            errors++;
        }
        _closeTurnOffBinding = _device.getTriggerBindingByName(key);
        if (_closeTurnOffBinding == null) {
            Logger.logError(LOG_TAG, "tf: no binding found for " + key);
            errors++;
        } else {
            Logger.logDebug(LOG_TAG, "tf: binding " + _closeTurnOffBinding);
        }

        if (errors > 0 && _fixCount > 0) {
            // if fixing it doesn't create the group & bindings again, usually a factory
            // reset will help.
            _errorMessage.setText(getString(R.string.trigger_setup_error_again));
        }
        _errorContainer.setVisibility((errors > 0) ? View.VISIBLE : View.GONE);
        _setupErrors = errors;

        updateUI();
    }

    void updateList() {
        boolean enableAddOn = true;
        boolean enableAddOff = true;
        boolean enableRemOn = false;
        boolean enableRemOff = false;

        if (_setupErrors > 0) {
            enableAddOn = enableAddOff = false;
        } else {
            AylaGroupZigbee turnOn;
            AylaGroupZigbee turnOff;
            if (_onSensorRadio.isChecked()) {
                Logger.logInfo(LOG_TAG, "tf: updateList TriggerOn");
                turnOn = _openTurnOnGroup;
                turnOff = _openTurnOffGroup;
            } else {
                Logger.logInfo(LOG_TAG, "tf: updateList TriggerOff");
                turnOn = _closeTurnOnGroup;
                turnOff = _closeTurnOffGroup;
            }
            List<String> values = new ArrayList<>();
            List<Device> onDevs = _gateway.getGroupManager().getDevices(turnOn);
            List<Device> ofDevs = _gateway.getGroupManager().getDevices(turnOff);
            for (Device device : onDevs) {
                values.add("Turn on " + device.getProductName());
                enableRemOn = true;
            }
            for (Device device : ofDevs) {
                values.add("Turn off " + device.getProductName());
                enableRemOff = true;
            }
            _adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, values);
            _listView.setAdapter(_adapter);
            _adapter.notifyDataSetChanged();
        }

        _turnOn.setEnabled(enableAddOn);
        _turnOff.setEnabled(enableAddOff);
        _removeOn.setEnabled(enableRemOn);
        _removeOff.setEnabled(enableRemOff);
    }

    void updateUI() {
        // Set the device title and image
        _titleView.setText(_device.toString());
        _dsnView.setText(_device.getDeviceDsn());
        _imageView.setImageDrawable(_device.getDeviceDrawable(getActivity()));

        // set the radio button labels
        _onSensorRadio.setText(_device.getTriggerOnName());
        _offSensorRadio.setText(_device.getTriggerOffName());

        updateList();
    }

    @Override
    public boolean isDeviceComparableType(Device another) {
        return _device.isTriggerTarget(another);
    }

    private void addDevicesSelected(boolean turnOn, List<Device> list) {
        MainActivity.getInstance().showWaitDialog(R.string.trigger_update_title, R.string.trigger_update_body);
        AylaGroupZigbee group;
        if (_onSensorRadio.isChecked()) {
            group = turnOn ? _openTurnOnGroup : _openTurnOffGroup;
        } else {
            group = turnOn ? _closeTurnOnGroup : _closeTurnOffGroup;
        }
        _gateway.addDevicesToGroup(group, list, this, new Gateway.AylaGatewayCompletionHandler() {
            @Override
            public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                MainActivity.getInstance().dismissWaitDialog();
                updateList();
                Toast.makeText(getActivity(), R.string.trigger_update_complete, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void removeDevicesSelected(boolean turnOn, List<Device> list) {
        MainActivity.getInstance().showWaitDialog(R.string.trigger_update_title, R.string.trigger_update_body);
        AylaGroupZigbee group;
        if (_onSensorRadio.isChecked()) {
            group = turnOn ? _openTurnOnGroup : _openTurnOffGroup;
        } else {
            group = turnOn ? _closeTurnOnGroup : _closeTurnOffGroup;
        }
        _gateway.removeDevicesFromGroup(group, list, this, new Gateway.AylaGatewayCompletionHandler() {
            @Override
            public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                MainActivity.getInstance().dismissWaitDialog();
                updateList();
                Toast.makeText(getActivity(), R.string.trigger_update_complete, Toast.LENGTH_LONG).show();
            }
        });
    }

    public List<Device> getAvailableDevices(List<Device> filter) {
        List<Device> devices = _gateway.getDevicesOfComparableType(this);
        List<Device> list = new ArrayList<>();
        if (devices != null) {
            for (Device device : devices) {
                if (!DeviceManager.isDsnInDeviceList(device.getDeviceDsn(), filter)) {
                    list.add(SessionManager.deviceManager().deviceByDSN(device.getDeviceDsn()));
                }
            }
        }
        return list;
    }

    private void addDevicesClicked(final boolean turnOn) {

        // Get a list of the devices that can be targeted
        Logger.logDebug(LOG_TAG, "tf: addDevicesClicked " + turnOn);
        List<Device> filterList;
        if (_onSensorRadio.isChecked()) {
            filterList = _gateway.getGroupManager().getDevices(turnOn ? _openTurnOnGroup : _openTurnOffGroup);
        } else {
            filterList = _gateway.getGroupManager().getDevices(turnOn ? _closeTurnOnGroup : _closeTurnOffGroup);
        }
        final List<Device> list = getAvailableDevices(filterList);

        // show a multiple selection dialog with the available devices to add
        String apNames[] = new String[list.size()];
        final List<Device> selected = new ArrayList<Device>();
        boolean[] isSelectedArray = new boolean[list.size()];
        for ( int i = 0; i < list.size(); i++ ) {
            isSelectedArray[i] = true;
            Device d = list.get(i);
            Logger.logVerbose(LOG_TAG, "tf: device [%s:%s]", d.getDeviceDsn(), d.getProductName());
            apNames[i] = d.getProductName();
            selected.add(d);
        }
        new AlertDialog.Builder(getActivity())
                .setTitle(turnOn ? R.string.trigger_devices_on_title : R.string.trigger_devices_off_title)
                .setMultiChoiceItems(apNames, isSelectedArray, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        Device d = list.get(which);
                        Logger.logVerbose(LOG_TAG, "tf: device [%s:%s]", d.getDeviceDsn(), d.getProductName());
                        if (isChecked) {
                            selected.add(d);
                        } else {
                            selected.remove(d);
                        }
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        addDevicesSelected(turnOn, selected);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create().show();
    }

    private void removeDevicesClicked(final boolean turnOn) {
        Logger.logDebug(LOG_TAG, "tf: removeDevicesClicked " + turnOn);
        List<Device> filterList;
        if (_onSensorRadio.isChecked()) {
            filterList = _gateway.getGroupManager().getDevices(turnOn ? _openTurnOnGroup : _openTurnOffGroup);
        } else {
            filterList = _gateway.getGroupManager().getDevices(turnOn ? _closeTurnOnGroup : _closeTurnOffGroup);
        }
        final List<Device> list = filterList;

        // show a multiple selection dialog with the available devices to remove
        String apNames[] = new String[list.size()];
        final List<Device> selected = new ArrayList<Device>();
        boolean[] isSelectedArray = new boolean[list.size()];
        for ( int i = 0; i < list.size(); i++ ) {
            isSelectedArray[i] = true;
            Device d = list.get(i);
            Logger.logVerbose(LOG_TAG, "tf: device [%s:%s]", d.getDeviceDsn(), d.getProductName());
            apNames[i] = d.getProductName();
            selected.add(d);
        }
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.trigger_devices_remove_title)
                .setMultiChoiceItems(apNames, isSelectedArray, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        Device d = list.get(which);
                        Logger.logVerbose(LOG_TAG, "tf: device [%s:%s]", d.getDeviceDsn(), d.getProductName());
                        if (isChecked) {
                            selected.add(d);
                        } else {
                            selected.remove(d);
                        }
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeDevicesSelected(turnOn, selected);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create().show();
    }

    private void fixGroupBindingClicked() {
        MainActivity.getInstance().showWaitDialog(R.string.trigger_action_fix_title, R.string.trigger_action_fix_body);
        _device.fixRegistrationForGatewayDevice(_gateway, this, new Gateway.AylaGatewayCompletionHandler() {
            @Override
            public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                MainActivity.getInstance().dismissWaitDialog();
                _fixCount++;
                updateGroups();
                Toast.makeText(getActivity(), AylaNetworks.succeeded(msg) ? R.string.trigger_action_fix_complete : R.string.trigger_action_fix_failure, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onClick(View v) {
        v.playSoundEffect(android.view.SoundEffectConstants.CLICK);
        switch (v.getId()) {
            case R.id.radio_turn_on:
            case R.id.radio_turn_off:
                updateList();
                break;

            case R.id.action_turn_on_add:
                addDevicesClicked(true);
                break;

            case R.id.action_turn_off_add:
                addDevicesClicked(false);
                break;

            case R.id.action_turn_on_remove:
                removeDevicesClicked(true);
                break;

            case R.id.action_turn_off_remove:
                removeDevicesClicked(false);
                break;

            case R.id.error_fix:
                fixGroupBindingClicked();
                break;
        }
    }
}
