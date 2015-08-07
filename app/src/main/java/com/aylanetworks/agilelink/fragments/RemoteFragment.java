package com.aylanetworks.agilelink.fragments;
/* 
 * RemoteFragment
 * AgileLink Application Framework
 *
 * Created by David N. Junod on 6/2/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
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
import android.widget.TextView;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.zigbee.AylaBindingZigbee;
import com.aylanetworks.aaml.zigbee.AylaGroupZigbee;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.device.RemoteSwitchDevice;
import com.aylanetworks.agilelink.device.ZigbeeGateway;
import com.aylanetworks.agilelink.device.ZigbeeWirelessSwitch;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.DeviceManager;
import com.aylanetworks.agilelink.framework.Gateway;
import com.aylanetworks.agilelink.framework.Logger;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class RemoteFragment extends Fragment implements View.OnClickListener, DeviceManager.GetDeviceComparable {

    public final static String LOG_TAG = "RemoteFragment";

    private final static int FRAGMENT_RESOURCE_ID = R.layout.fragment_remote_detail;

    private static final String ARG_DSN = "dsn";

    Device _device;
    ZigbeeGateway _gateway;
    ListView _listView;
    SimpleDeviceListAdapter _adapter;
    TextView _titleView;
    TextView _dsnView;
    ImageView _imageView;
    Button _addButton;
    Button _removeButton;

    int _setupErrors;
    int _fixCount;
    View _errorContainer;
    TextView _errorMessage;

    class SimpleDeviceListAdapter extends ArrayAdapter<Device> {

        public SimpleDeviceListAdapter(Context c, Device[] objects) {
            super(c, android.R.layout.simple_list_item_1, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            TextView tv = (TextView)v.findViewById(android.R.id.text1);

            Device device = getItem(position);
            tv.setText(device.getProductName());
            tv.setTextAppearance(tv.getContext(), android.R.style.TextAppearance_Medium);
            tv.setTypeface(null, Typeface.BOLD);
            return v;
        }
    }

    SimpleDeviceListAdapter newSimpleDeviceListAdapter(List<Device> list) {
        Device[] objects = new Device[list.size()];
        list.toArray(objects);
        return new SimpleDeviceListAdapter(getActivity(), objects);
    }

    public static RemoteFragment newInstance(Device device) {
        RemoteFragment frag = new RemoteFragment();
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
            _device = SessionManager.deviceManager().deviceByDSN(dsn);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(FRAGMENT_RESOURCE_ID, container, false);

        if (( _device == null) || !_device.isDeviceNode()) {
            Log.e(LOG_TAG, "Unable to find device!");
            getFragmentManager().popBackStack();
            Toast.makeText(getActivity(), R.string.unknown_error, Toast.LENGTH_SHORT).show();

        } else {
            _gateway = (ZigbeeGateway)Gateway.getGatewayForDeviceNode(_device);

            // group/binding error message area
            _errorContainer = view.findViewById(R.id.error_container);
            Button errorButton = (Button)view.findViewById(R.id.error_fix);
            errorButton.setOnClickListener(this);
            _errorMessage = (TextView)view.findViewById(R.id.error_message);

            _listView = (ListView) view.findViewById(R.id.listView);
            _titleView = (TextView) view.findViewById(R.id.device_name);
            _dsnView = (TextView) view.findViewById(R.id.device_dsn);
            _imageView = (ImageView) view.findViewById(R.id.device_image);

            _addButton = (Button) view.findViewById(R.id.add_button);
            _addButton.setOnClickListener(this);

            _removeButton = (Button) view.findViewById(R.id.remove_button);
            _removeButton.setOnClickListener(this);

            TextView listLabel = (TextView) view.findViewById(R.id.paired_devices);
            if (_device instanceof RemoteSwitchDevice) {
                listLabel.setText(getString(R.string.remote_paired_devices));
                Logger.logDebug(LOG_TAG, "rm: we are a remote.");
            } else {
                listLabel.setText(getString(R.string.remote_paired_remote));
                List<Device> remotes = _gateway.getDevicesOfClass(new Class[]{ZigbeeWirelessSwitch.class});
                if ((remotes != null) && (remotes.size() > 0)) {
                    Logger.logInfo(LOG_TAG, "rm: we have %d remotes.", remotes.size());
                    // we only have one type of remote class, so it's safe to do this with the first one
                    // we find.
                    ZigbeeWirelessSwitch remote = (ZigbeeWirelessSwitch) remotes.get(0);
                    if (remote.isPairableDevice(_device)) {
                        Logger.logInfo(LOG_TAG, "rm: device [%s:%s] is pairable with the remote.", _device.getDeviceDsn(), _device.getClass().getSimpleName());
                    } else {
                        Logger.logInfo(LOG_TAG, "rm: device [%s:%s] is not pairable with the remote.", _device.getDeviceDsn(), _device.getClass().getSimpleName());
                    }
                } else {
                    Logger.logInfo(LOG_TAG, "rm: we don't have any remotes.");
                }
            }

            updateGroups();
        }

        return view;
    }

    List<Device> getAvailableDevicesForRemote(RemoteSwitchDevice remote) {
        // Get a list of the devices that can be paired to this remote
        final List<Device> devices = _gateway.getDevicesOfComparableType(this);
        final List<Device> list = new ArrayList<Device>();
        for (Device device : devices) {
            // This only checks to see if the device is paired with this remote
            if (!remote.isDevicePaired(device)) {
                list.add(device);
            }
        }
        return list;
    }

    void updateGroups() {
        int errors = 0;

        AylaBindingZigbee binding;
        AylaGroupZigbee group;

        if (_device instanceof RemoteSwitchDevice) {
            // we are a remote
            if (_device instanceof ZigbeeWirelessSwitch) {
                ZigbeeWirelessSwitch remote = (ZigbeeWirelessSwitch)_device;
                group = remote.getRemoteGroup();
                if (group == null) {
                    Logger.logError(LOG_TAG, "rm: no group found for [%s]", remote.getDeviceDsn());
                    errors++;
                }
                binding = remote.getRemoteBinding();
                if (binding == null) {
                    Logger.logError(LOG_TAG, "rm: no binding found for [%s]", remote.getDeviceDsn());
                    errors++;
                }
            }
        } else {
            // we are a device
            List<Device> remotes = _gateway.getDevicesOfClass(new Class[]{RemoteSwitchDevice.class});
            for (Device device : remotes) {
                if (device instanceof ZigbeeWirelessSwitch) {
                    ZigbeeWirelessSwitch remote = (ZigbeeWirelessSwitch) device;
                    group = remote.getRemoteGroup();
                    if (group == null) {
                        Logger.logError(LOG_TAG, "rm: no group found for [%s]", remote.getDeviceDsn());
                        errors++;
                    }
                    binding = remote.getRemoteBinding();
                    if (binding == null) {
                        Logger.logError(LOG_TAG, "rm: no binding found for [%s]", remote.getDeviceDsn());
                        errors++;
                    }
                }
            }
        }

        if (errors > 0 && _fixCount > 0) {
            // Which one?
            // if fixing it doesn't create the group & bindings again, usually a factory
            // reset will help.
            _errorMessage.setText(getString(R.string.trigger_setup_error_again));
        }

        _errorContainer.setVisibility((errors > 0) ? View.VISIBLE : View.GONE);
        _setupErrors = errors;

        updateUI();
    }

    void updateList() {
        boolean enableAdd = true;
        boolean enableRem = false;

        List<Device> devices;
        if (_device instanceof RemoteSwitchDevice) {
            // we are a remote
            devices = ((RemoteSwitchDevice)_device).getPairedDevices();
            enableRem = (devices.size() > 0);

            List<Device> available = getAvailableDevicesForRemote((RemoteSwitchDevice)_device);
            enableAdd = (available.size() > 0);
        } else {
            // we are a device

            devices = new ArrayList<Device>();
            List<Device> remotes = _gateway.getDevicesOfClass(new Class[]{RemoteSwitchDevice.class});
            for (Device device : remotes) {
                RemoteSwitchDevice remote = (RemoteSwitchDevice)device;
                if (remote.isDevicePaired(_device)) {
                    Logger.logInfo(LOG_TAG, "rm: device [%s:%s] is paired with remote [%s:%s]",
                            _device.getDeviceDsn(), _device.getClass().getSimpleName(),
                            device.getDeviceDsn(), device.getClass().getSimpleName());
                    devices.add(device);
                    enableAdd = false;
                    enableRem = true;
                }
            }
        }
        _adapter = newSimpleDeviceListAdapter(devices);
        _listView.setAdapter(_adapter);

        _addButton.setEnabled(enableAdd);
        _removeButton.setEnabled(enableRem);
    }

    void updateUI() {
        // Set the device title and image
        _titleView.setText(_device.toString());
        _dsnView.setText(_device.getDeviceDsn());
        _imageView.setImageDrawable(_device.getDeviceDrawable(getActivity()));
        updateList();
    }

    @Override
    public boolean isDeviceComparableType(Device another) {
        return ((ZigbeeWirelessSwitch)_device).isPairableDevice(another);
    }

    private void pairDevice(RemoteSwitchDevice remote, Device device) {
        MainActivity.getInstance().showWaitDialog(R.string.remote_pair_title, R.string.remote_pair_single_body);
        remote.pairDevice(device, this, new RemoteSwitchDevice.RemoteSwitchCompletionHandler() {
            @Override
            public void handle(RemoteSwitchDevice remote, Message msg, Object tag) {
                MainActivity.getInstance().dismissWaitDialog();
                updateList();
                Toast.makeText(getActivity(), R.string.remote_pair_toast, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void pairDevices(RemoteSwitchDevice remote, List<Device> list) {
        MainActivity.getInstance().showWaitDialog(R.string.remote_pair_title, R.string.remote_pair_multiple_body);
        remote.pairDevices(list, this, new RemoteSwitchDevice.RemoteSwitchCompletionHandler() {
            @Override
            public void handle(RemoteSwitchDevice remote, Message msg, Object tag) {
                MainActivity.getInstance().dismissWaitDialog();
                updateList();
                Toast.makeText(getActivity(), R.string.remote_pair_toast, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void unpairDevice(RemoteSwitchDevice remote, Device device) {
        MainActivity.getInstance().showWaitDialog(R.string.remote_unpair_title, R.string.remote_unpair_single_body);
        remote.unpairDevice(device, this, new RemoteSwitchDevice.RemoteSwitchCompletionHandler() {
            @Override
            public void handle(RemoteSwitchDevice remote, Message msg, Object tag) {
                MainActivity.getInstance().dismissWaitDialog();
                updateList();
                Toast.makeText(getActivity(), R.string.remote_unpair_toast, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void unpairDevices(RemoteSwitchDevice remote, List<Device> list) {
        MainActivity.getInstance().showWaitDialog(R.string.remote_unpair_title, R.string.remote_unpair_multiple_body);
        remote.unpairDevices(list, this, new RemoteSwitchDevice.RemoteSwitchCompletionHandler() {
            @Override
            public void handle(RemoteSwitchDevice remote, Message msg, Object tag) {
                MainActivity.getInstance().dismissWaitDialog();
                updateList();
                Toast.makeText(getActivity(), R.string.remote_unpair_toast, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void addButtonClicked() {
        if (_device instanceof RemoteSwitchDevice) {
            // Device is the remote switch
            RemoteSwitchDevice remote = (RemoteSwitchDevice)_device;

            // Get a list of the devices that can be paired to this remote
            final List<Device> list = getAvailableDevicesForRemote(remote);
            if (list.isEmpty()) {
                Toast.makeText(getActivity(), R.string.no_devices_available_to_pair, Toast.LENGTH_SHORT).show();
                return;
            }

            // show a multiple selection dialog with the available devices to add
            String apNames[] = new String[list.size()];
            final List<Device> selected = new ArrayList<Device>();
            boolean[] isSelectedArray = new boolean[list.size()];
            for ( int i = 0; i < list.size(); i++ ) {
                isSelectedArray[i] = true;
                Device d = list.get(i);
                Logger.logVerbose(LOG_TAG, "rm: device [%s:%s]", d.getDeviceDsn(), d.getProductName());
                apNames[i] = d.getProductName();
                selected.add(d);
            }

            new AlertDialog.Builder(getActivity())
                    .setIcon(R.drawable.ic_launcher)
                    .setTitle(R.string.remote_pair_to_devices)
                    .setMultiChoiceItems(apNames, isSelectedArray, new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                            Device d = list.get(which);
                            Logger.logVerbose(LOG_TAG, "rm: device [%s:%s]", d.getDeviceDsn(), d.getProductName());
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
                            pairDevices((RemoteSwitchDevice) _device, selected);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create().show();
        } else {
            // Device is or can be paired to a remote switch

            // Get a list of the remotes that this device can be paired with
            List<Device> remotes = _gateway.getDevicesOfClass(new Class[]{RemoteSwitchDevice.class});
            final List<Device> list = new ArrayList<Device>();
            for (Device device : remotes) {
                RemoteSwitchDevice remote = (RemoteSwitchDevice)device;
                if (remote.isPairableDevice(_device)) {
                    Logger.logInfo(LOG_TAG, "rm: device [%s:%s] is pairable with remote [%s:%s]",
                            _device.getDeviceDsn(), _device.getClass().getSimpleName(),
                            device.getDeviceDsn(), device.getClass().getSimpleName());
                    list.add(device);
                }
            }

            if (list.size() > 1) {
                // show single selection dialog with the available remotes to add to
                String apNames[] = new String[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    apNames[i] = list.get(i).getProductName();
                }
                new AlertDialog.Builder(getActivity())
                        .setIcon(R.drawable.ic_launcher)
                        .setTitle(R.string.remote_pair_to_device)
                        .setSingleChoiceItems(apNames, -1, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                pairDevice((RemoteSwitchDevice) list.get(which), _device);
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create().show();
            } else if (list.size() > 0) {
                // just pair it
                pairDevice((RemoteSwitchDevice) list.get(0), _device);
            } else {
                // nothing to do, we've already verified that this can't happen
            }
        }
    }

    private void removeButtonClicked() {
        if (_device instanceof RemoteSwitchDevice) {
            // Device is the remote switch
            RemoteSwitchDevice remote = (RemoteSwitchDevice)_device;

            // Get a list of the devices that can be unpaired from this remote
            final List<Device> list = remote.getPairedDevices();
            if (list.isEmpty()) {
                Toast.makeText(getActivity(), R.string.no_devices_available_to_unpair, Toast.LENGTH_SHORT).show();
                return;
            }

            // show a multiple selection dialog with the available devices to remove
            String apNames[] = new String[list.size()];
            final List<Device> selected = new ArrayList<Device>();
            boolean[] isSelectedArray = new boolean[list.size()];
            for ( int i = 0; i < list.size(); i++ ) {
                isSelectedArray[i] = true;
                Device d = list.get(i);
                Logger.logVerbose(LOG_TAG, "rm: device [%s:%s]", d.getDeviceDsn(), d.getProductName());
                apNames[i] = d.getProductName();
                selected.add(d);
            }
            new AlertDialog.Builder(getActivity())
                    .setIcon(R.drawable.ic_launcher)
                    .setTitle(R.string.remote_unpair_from_devices)
                    .setMultiChoiceItems(apNames, isSelectedArray, new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                            Device d = list.get(which);
                            Logger.logVerbose(LOG_TAG, "rm: device [%s:%s]", d.getDeviceDsn(), d.getProductName());
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
                            unpairDevices((RemoteSwitchDevice) _device, selected);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create().show();

        } else {
            // Device is or can be paired to a remote switch

            AlertDialog.Builder ad = new AlertDialog.Builder(getActivity());
            ad.setIcon(R.drawable.ic_launcher);
            ad.setTitle(R.string.remote_unpair_title);
            ad.setMessage(R.string.remote_unpair_body);
            ad.setNegativeButton(android.R.string.no, null);
            ad.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Unpair device from the remote that is paired with
                    List<Device> remotes = _gateway.getDevicesOfClass(new Class[]{RemoteSwitchDevice.class});
                    for (Device device : remotes) {
                        RemoteSwitchDevice remote = (RemoteSwitchDevice)device;
                        if (remote.isDevicePaired(_device)) {
                            Logger.logInfo(LOG_TAG, "rm: device [%s:%s] is paired with remote [%s:%s]",
                                    _device.getDeviceDsn(), _device.getClass().getSimpleName(),
                                    device.getDeviceDsn(), device.getClass().getSimpleName());
                            unpairDevice(remote, _device);
                        }
                    }
                }
            });
            ad.show();
        }
    }

    private void fixGroupBindingClicked() {
        MainActivity.getInstance().showWaitDialog(R.string.remote_action_fix_title, R.string.remote_action_fix_body);

        if (_device instanceof RemoteSwitchDevice) {
            // we are a remote
            if (_device instanceof ZigbeeWirelessSwitch) {
                ZigbeeWirelessSwitch remote = (ZigbeeWirelessSwitch)_device;
                remote.fixRegistrationForGatewayDevice(_gateway, this, new Gateway.AylaGatewayCompletionHandler() {
                    @Override
                    public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                        MainActivity.getInstance().dismissWaitDialog();
                        _fixCount++;
                        updateGroups();
                        Toast.makeText(getActivity(), AylaNetworks.succeeded(msg) ? R.string.remote_action_fix_complete : R.string.remote_action_fix_failure, Toast.LENGTH_LONG).show();
                    }
                });
            }
        } else {
            // we are a device
            List<Device> remotes = _gateway.getDevicesOfClass(new Class[]{RemoteSwitchDevice.class});
            for (Device device : remotes) {
                if (device instanceof ZigbeeWirelessSwitch) {
                    ZigbeeWirelessSwitch remote = (ZigbeeWirelessSwitch) device;
                    int errors = 0;
                    AylaGroupZigbee group = remote.getRemoteGroup();
                    if (group == null) {
                        Logger.logError(LOG_TAG, "rm: no group found for [%s]", remote.getDeviceDsn());
                        errors++;
                    }
                    AylaBindingZigbee binding = remote.getRemoteBinding();
                    if (binding == null) {
                        Logger.logError(LOG_TAG, "rm: no binding found for [%s]", remote.getDeviceDsn());
                        errors++;
                    }
                    if (errors > 0) {
                        remote.fixRegistrationForGatewayDevice(_gateway, this, new Gateway.AylaGatewayCompletionHandler() {
                            @Override
                            public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                                MainActivity.getInstance().dismissWaitDialog();
                                _fixCount++;
                                updateGroups();
                                Toast.makeText(getActivity(), AylaNetworks.succeeded(msg) ? R.string.remote_action_fix_complete : R.string.remote_action_fix_failure, Toast.LENGTH_LONG).show();
                            }
                        });
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add_button:
                addButtonClicked();
                break;

            case R.id.remove_button:
                removeButtonClicked();
                break;

            case R.id.error_fix:
                fixGroupBindingClicked();
                break;
        }
    }
}
