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

import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class ShareDevicesFragment extends DialogFragment {
    private final static String LOG_TAG = "ShareDevicesFragment";
    private ShareDevicesListener _listener;

    public interface ShareDevicesListener {
        void shareDevices(String email, List<Device> devicesToShare);
    }

    public static ShareDevicesFragment newInstance(ShareDevicesListener listener) {
        ShareDevicesFragment frag = new ShareDevicesFragment();
        frag._listener = listener;
        return frag;
    }

    private ListView _deviceList;
    private EditText _email;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View root = inflater.inflate(R.layout.fragment_share_devices, null);

        _deviceList = (ListView)root.findViewById(R.id.share_listview);
        _email = (EditText)root.findViewById(R.id.share_email);

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
                        _listener.shareDevices(_email.getText().toString(), devicesToAdd);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }
}
