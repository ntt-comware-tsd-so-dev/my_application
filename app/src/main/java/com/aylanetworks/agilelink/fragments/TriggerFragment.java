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
import android.util.Log;
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

    private static final String ARG_DSN = "dsn";

    Gateway _gateway;
    ZigbeeTriggerDevice _device;

    AylaGroupZigbee _openTurnOn;
    AylaGroupZigbee _openTurnOff;
    AylaGroupZigbee _closeTurnOn;
    AylaGroupZigbee _closeTurnOff;

    ListView _listView;
    ArrayAdapter<String> _adapter;
    TextView _titleView;
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
        args.putString(ARG_DSN, device.getDevice().dsn);
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
        View view = inflater.inflate(R.layout.fragment_trigger_manage, container, false);

        if (( _device == null) || !_device.isDeviceNode()) {
            Log.e(LOG_TAG, "Unable to find device!");
            getFragmentManager().popBackStack();
            Toast.makeText(getActivity(), R.string.unknown_error, Toast.LENGTH_SHORT).show();

        } else {
            _gateway = Gateway.getGatewayForDeviceNode(_device);

            _listView = (ListView) view.findViewById(R.id.listView);
            _titleView = (TextView) view.findViewById(R.id.device_name);
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

            TextView listLabel = (TextView) view.findViewById(R.id.list_label);

            updateGroups();
        }
        return view;
    }

    private void updateGroups() {
        _openTurnOn = _device.getTriggerGroup(true, true);
        _openTurnOff = _device.getTriggerGroup(true, false);
        _closeTurnOn = _device.getTriggerGroup(false, true);
        _closeTurnOff = _device.getTriggerGroup(false, false);
        updateUI();
    }

    void updateList() {
        boolean enableAddOn = true;
        boolean enableAddOff = true;
        boolean enableRemOn = false;
        boolean enableRemOff = false;

        AylaGroupZigbee turnOn;
        AylaGroupZigbee turnOff;
        if (_onSensorRadio.isChecked()) {
            Log.e(LOG_TAG, "tf: updateList TriggerOn");
            turnOn = _openTurnOn;
            turnOff = _openTurnOff;
        } else {
            Log.e(LOG_TAG, "tf: updateList TriggerOff");
            turnOn = _closeTurnOn;
            turnOff = _closeTurnOff;
        }
        List<String> values = new ArrayList<>();
        List<Device> onDevs = _gateway.getGroupManager().getDevices(turnOn);
        List<Device> ofDevs = _gateway.getGroupManager().getDevices(turnOff);
        for (Device device : onDevs) {
            values.add("Turn on " + device.getDevice().getProductName());
            enableRemOn = true;
        }
        for (Device device : ofDevs) {
            values.add("Turn off " + device.getDevice().getProductName());
            enableRemOff = true;
        }
        _adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, values);
        _listView.setAdapter(_adapter);
        _adapter.notifyDataSetChanged();

        _turnOn.setEnabled(enableAddOn);
        _turnOff.setEnabled(enableAddOff);
        _removeOn.setEnabled(enableRemOn);
        _removeOff.setEnabled(enableRemOff);
    }

    void updateUI() {
        if ( _device == null ) {
            Log.e(LOG_TAG, "Unable to find device!");
            getFragmentManager().popBackStack();
            Toast.makeText(getActivity(), R.string.unknown_error, Toast.LENGTH_SHORT).show();
        } else {
            // Set the device title and image
            _titleView.setText(_device.toString());
            _imageView.setImageDrawable(_device.getDeviceDrawable(getActivity()));

            // set the radio button labels
            _onSensorRadio.setText(_device.getTriggerOnName());
            _offSensorRadio.setText(_device.getTriggerOffName());

            updateList();
        }
    }

    @Override
    public boolean isDeviceComparableType(Device another) {
        return _device.isTriggerTarget(another);
    }

    private void addDevicesSelected(boolean turnOn, List<Device> list) {
        MainActivity.getInstance().showWaitDialog(R.string.trigger_update_title, R.string.trigger_update_body);
        AylaGroupZigbee group;
        if (_onSensorRadio.isChecked()) {
            group = turnOn ? _openTurnOn : _openTurnOff;
        } else {
            group = turnOn ? _closeTurnOn : _closeTurnOff;
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
            group = turnOn ? _openTurnOn : _openTurnOff;
        } else {
            group = turnOn ? _closeTurnOn : _closeTurnOff;
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
        Log.e(LOG_TAG, "tf: addDevicesClicked " + turnOn);
        List<Device> filterList;
        if (_onSensorRadio.isChecked()) {
            filterList = _gateway.getGroupManager().getDevices(turnOn ? _openTurnOn : _openTurnOff);
        } else {
            filterList = _gateway.getGroupManager().getDevices(turnOn ? _closeTurnOn : _closeTurnOff);
        }
        final List<Device> list = getAvailableDevices(filterList);

        // show a multiple selection dialog with the available devices to add
        String apNames[] = new String[list.size()];
        final List<Device> selected = new ArrayList<Device>();
        boolean[] isSelectedArray = new boolean[list.size()];
        for ( int i = 0; i < list.size(); i++ ) {
            isSelectedArray[i] = true;
            Device d = list.get(i);
            Logger.logVerbose(LOG_TAG, "tf: device [%s:%s]", d.getDevice().dsn, d.getDevice().productName);
            apNames[i] = d.getDevice().productName;
            selected.add(d);
        }
        new AlertDialog.Builder(getActivity())
                .setTitle(turnOn ? R.string.trigger_devices_on_title : R.string.trigger_devices_off_title)
                .setMultiChoiceItems(apNames, isSelectedArray, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        Device d = list.get(which);
                        Logger.logVerbose(LOG_TAG, "tf: device [%s:%s]", d.getDevice().dsn, d.getDevice().productName);
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
        Log.e(LOG_TAG, "tf: removeDevicesClicked " + turnOn);
        List<Device> filterList;
        if (_onSensorRadio.isChecked()) {
            filterList = _gateway.getGroupManager().getDevices(turnOn ? _openTurnOn : _openTurnOff);
        } else {
            filterList = _gateway.getGroupManager().getDevices(turnOn ? _closeTurnOn : _closeTurnOff);
        }
        final List<Device> list = filterList;

        // show a multiple selection dialog with the available devices to remove
        String apNames[] = new String[list.size()];
        final List<Device> selected = new ArrayList<Device>();
        boolean[] isSelectedArray = new boolean[list.size()];
        for ( int i = 0; i < list.size(); i++ ) {
            isSelectedArray[i] = true;
            Device d = list.get(i);
            Logger.logVerbose(LOG_TAG, "tf: device [%s:%s]", d.getDevice().dsn, d.getDevice().productName);
            apNames[i] = d.getDevice().productName;
            selected.add(d);
        }
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.trigger_devices_remove_title)
                .setMultiChoiceItems(apNames, isSelectedArray, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        Device d = list.get(which);
                        Logger.logVerbose(LOG_TAG, "tf: device [%s:%s]", d.getDevice().dsn, d.getDevice().productName);
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
        }
    }
}
