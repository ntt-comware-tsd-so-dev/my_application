/*
 * SharesFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 3/26/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

package com.aylanetworks.agilelink.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;

import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * This class presents an interface to the user for sharing devices. The dialog can be configured
 * to present a list of devices to share, or can be set with a specific device to share ahead of
 * time. When the user has entered all of the necessary information for sharing, the listener
 * is notified with the results.
 *
 * This class does not itself set up the sharing, but rather provides the UI for doing so.
 */
public class ShareDevicesFragment extends DialogFragment {
    private final static String LOG_TAG = "ShareDevicesFragment";
    private ShareDevicesListener _listener;
    private Calendar _shareStartDate;
    private Calendar _shareEndDate;
    private boolean _readOnly;

    public interface ShareDevicesListener {
        /**
         * When the ShareDevicesFragment is dismissed, this listener method will be called with
         * information about the shares.
         *
         * @param email Email address of the user to share devices with, or null if canceled
         * @param startDate Date the sharing begins, or null if none selected
         * @param endDate Date the sharing ends, or null if none selected
         * @param readOnly Set to true if the share can not be controlled by the recipient
         * @param devicesToShare A list of devices to be shared, or null if the user canceled
         */
        void shareDevices(String email, Calendar startDate, Calendar endDate, boolean readOnly, List<Device> devicesToShare);
    }

    /**
     * Creates an instance of the ShareDevicesFragment. This fragment is a DialogFragment, and
     * should be launched via the {@link #show(android.support.v4.app.FragmentManager, String)}
     * method after creation.
     *
     * This version of this method should be used to present a list of devices to be shared.
     *
     * @param listener Listener to receive the sharing information
     * @return the ShareDevicesFragment ready to be shown.
     */
    public static ShareDevicesFragment newInstance(ShareDevicesListener listener) {
        return newInstance(listener, null);
    }

    /**
     * Creates an instance of the ShareDevicesFragment. This fragment is a DialogFragment, and
     * should be launched via the {@link #show(android.support.v4.app.FragmentManager, String)}
     * method after creation.
     *
     * This version of this method should be used when the device to be shared is known ahead of
     * time (e.g. from the Device Details page). If the user should be presented with a list of
     * devices to select from for sharing (e.g. from the Sharing page in Settings), then the
     * device parameter should be null or {@link #newInstance(com.aylanetworks.agilelink.fragments.ShareDevicesFragment.ShareDevicesListener)}
     * should be used instead of this method.
     *
     * @param listener Listener to receive the sharing information
     * @param device Device to be shared, or null to present a list of devices in the dialog
     * @return
     */
    public static ShareDevicesFragment newInstance(ShareDevicesListener listener, Device device) {
        ShareDevicesFragment frag = new ShareDevicesFragment();
        frag._listener = listener;
        return frag;
    }

    private ListView _deviceList;
    private EditText _email;
    private RadioGroup _radioGroup;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View root = inflater.inflate(R.layout.fragment_share_devices, null);

        _deviceList = (ListView)root.findViewById(R.id.share_listview);
        _email = (EditText)root.findViewById(R.id.share_email);
        _radioGroup = (RadioGroup)root.findViewById(R.id.read_only_radio_group);

        _deviceList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        List<Device> deviceList = SessionManager.deviceManager().deviceList();
        // Remove devices that we don't own from this list
        List<Device> filteredList = new ArrayList<Device>();
        for ( Device d : deviceList ) {
            if ( d.getDevice().amOwner() ) {
                filteredList.add(d);
            }
        }

        if ( filteredList.isEmpty() ) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.new_share)
                    .setMessage(R.string.no_devices_to_share)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
        }

        Device devices[] = deviceList.toArray(new Device[deviceList.size()]);
        _deviceList.setAdapter(new ArrayAdapter<Device>(inflater.getContext(), android.R.layout.simple_list_item_multiple_choice, devices));

        return new AlertDialog.Builder(getActivity())
                .setView(root)
                .setTitle(R.string.new_share)
                .setMessage(R.string.add_share_devices_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        List<Device> devicesToAdd = new ArrayList<Device>();
                        SparseBooleanArray checkedItems = _deviceList.getCheckedItemPositions();
                        for (int i = 0; i < _deviceList.getAdapter().getCount(); i++) {
                            if (checkedItems.get(i)) {
                                Device device = (Device) _deviceList.getAdapter().getItem(i);
                                devicesToAdd.add(device);
                            }
                        }

                        Log.d(LOG_TAG, "Add Shares: " + devicesToAdd);
                        _listener.shareDevices(
                                _email.getText().toString(),
                                _shareStartDate,
                                _shareEndDate,
                                _readOnly,
                                devicesToAdd);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG, "User cancel");
                        _listener.shareDevices(null, null, null, false, null);
                    }
                })
                .create();
    }
}
