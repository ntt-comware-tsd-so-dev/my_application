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

import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.device.RemoteSwitchDevice;
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

    private static final String ARG_DSN = "dsn";

    Device _device;
    Gateway _gateway;
    ListView _listView;
    SimpleDeviceListAdapter _adapter;
    TextView _titleView;
    ImageView _imageView;
    Button _addButton;
    Button _removeButton;

    class SimpleDeviceListAdapter extends ArrayAdapter<Device> {

        public SimpleDeviceListAdapter(Context c, Device[] objects) {
            super(c, android.R.layout.simple_list_item_1, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            TextView tv = (TextView)v.findViewById(android.R.id.text1);

            Device device = getItem(position);
            tv.setText(device.getDevice().productName);
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
            _device = SessionManager.deviceManager().deviceByDSN(dsn);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_remote_detail, container, false);

        if (( _device == null) || !_device.isDeviceNode()) {
            Log.e(LOG_TAG, "Unable to find device!");
            getFragmentManager().popBackStack();
            Toast.makeText(getActivity(), R.string.unknown_error, Toast.LENGTH_SHORT).show();

        } else {
            _gateway = Gateway.getGatewayForDeviceNode(_device);

            _listView = (ListView) view.findViewById(R.id.listView);
            _titleView = (TextView) view.findViewById(R.id.device_name);
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
                        Logger.logInfo(LOG_TAG, "rm: device [%s:%s] is pairable with the remote.", _device.getDevice().dsn, _device.getClass().getSimpleName());
                    } else {
                        Logger.logInfo(LOG_TAG, "rm: device [%s:%s] is not pairable with the remote.", _device.getDevice().dsn, _device.getClass().getSimpleName());
                    }
                } else {
                    Logger.logInfo(LOG_TAG, "rm: we don't have any remotes.");
                }
            }

            updateUI();
        }

        return view;
    }

    void updateList() {
        boolean enableAdd = true;
        boolean enableRem = false;

        List<Device> devices;
        if (_device instanceof RemoteSwitchDevice) {
            // we are a remote
            devices = ((RemoteSwitchDevice)_device).getPairedDevices();
            enableRem = (devices.size() > 0);
        } else {
            // we are a device

            devices = new ArrayList<Device>();
            List<Device> remotes = _gateway.getDevicesOfClass(new Class[]{RemoteSwitchDevice.class});
            for (Device device : remotes) {
                RemoteSwitchDevice remote = (RemoteSwitchDevice)device;
                if (remote.isDevicePaired(_device)) {
                    Logger.logInfo(LOG_TAG, "rm: device [%s:%s] is paired with remote [%s:%s]",
                            _device.getDevice().dsn, _device.getClass().getSimpleName(),
                            device.getDevice().dsn, device.getClass().getSimpleName());
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
        if ( _device == null ) {
            Log.e(LOG_TAG, "Unable to find device!");
            getFragmentManager().popBackStack();
            Toast.makeText(getActivity(), R.string.unknown_error, Toast.LENGTH_SHORT).show();
        } else {
            // Set the device title and image
            _titleView.setText(_device.toString());
            _imageView.setImageDrawable(_device.getDeviceDrawable(getActivity()));
            updateList();
        }
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
            final List<Device> devices = _gateway.getDevicesOfComparableType(this);
            final List<Device> list = new ArrayList<Device>();
            for (Device device : devices) {
                // This only checks to see if the device is paired with this remote
                if (!remote.isDevicePaired(device)) {
                    list.add(device);
                }
            }

            // show a multiple selection dialog with the available devices to add
            String apNames[] = new String[list.size()];
            final List<Device> selected = new ArrayList<Device>();
            boolean[] isSelectedArray = new boolean[list.size()];
            for ( int i = 0; i < list.size(); i++ ) {
                isSelectedArray[i] = true;
                Device d = list.get(i);
                Logger.logVerbose(LOG_TAG, "rm: device [%s:%s]", d.getDevice().dsn, d.getDevice().productName);
                apNames[i] = d.getDevice().productName;
                selected.add(d);
            }
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.remote_pair_to_devices)
                    .setMultiChoiceItems(apNames, isSelectedArray, new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                            Device d = list.get(which);
                            Logger.logVerbose(LOG_TAG, "rm: device [%s:%s]", d.getDevice().dsn, d.getDevice().productName);
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
                            _device.getDevice().dsn, _device.getClass().getSimpleName(),
                            device.getDevice().dsn, device.getClass().getSimpleName());
                    list.add(device);
                }
            }

            if (list.size() > 1) {
                // show single selection dialog with the available remotes to add to
                String apNames[] = new String[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    apNames[i] = list.get(i).getDevice().productName;
                }
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.remote_pair_to_device)
                        .setSingleChoiceItems(apNames, -1, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                pairDevice((RemoteSwitchDevice)list.get(which), _device);
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

            // show a multiple selection dialog with the available devices to remove
            String apNames[] = new String[list.size()];
            final List<Device> selected = new ArrayList<Device>();
            boolean[] isSelectedArray = new boolean[list.size()];
            for ( int i = 0; i < list.size(); i++ ) {
                isSelectedArray[i] = true;
                Device d = list.get(i);
                Logger.logVerbose(LOG_TAG, "rm: device [%s:%s]", d.getDevice().dsn, d.getDevice().productName);
                apNames[i] = d.getDevice().productName;
                selected.add(d);
            }
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.remote_unpair_from_devices)
                    .setMultiChoiceItems(apNames, isSelectedArray, new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                            Device d = list.get(which);
                            Logger.logVerbose(LOG_TAG, "rm: device [%s:%s]", d.getDevice().dsn, d.getDevice().productName);
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
                                    _device.getDevice().dsn, _device.getClass().getSimpleName(),
                                    device.getDevice().dsn, device.getClass().getSimpleName());
                            unpairDevice(remote, _device);
                        }
                    }
                }
            });
            ad.show();
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
        }
    }
}
