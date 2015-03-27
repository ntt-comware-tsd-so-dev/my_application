package com.aylanetworks.agilelink.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaShare;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

/*
 * SettingsFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 1/28/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class SettingsFragment extends Fragment implements AdapterView.OnItemClickListener {
    private final static String LOG_TAG = "SettingsFragment";
    private ListView _listView;

    // List view indexes
    private final int INDEX_REGISTRATION = 0;
    private final int INDEX_CONTACTS = 1;
    private final int INDEX_WIFI_SETUP = 2;
    private final int INDEX_NOTIFICATIONS = 3;
    private final int INDEX_PROFILE = 4;
    private final int INDEX_DELETE_ACCOUNT = 5;
    private final int INDEX_SHARES = 6;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        _listView = (ListView)view.findViewById(R.id.listView);
        ArrayAdapter<CharSequence>adapter = ArrayAdapter.createFromResource(getActivity(), R.array.settings_items, android.R.layout.simple_list_item_1);
        _listView.setAdapter(adapter);
        _listView.setOnItemClickListener(this);

        return view;
    }

    private void handleRegistration() {
        // Bring up the Add Device UI
        AddDeviceFragment frag = AddDeviceFragment.newInstance();
        MainActivity.getInstance().pushFragment(frag);
    }

    private void handleContacts() {
        ContactListFragment frag = ContactListFragment.newInstance();
        MainActivity.getInstance().pushFragment(frag);
    }

    private void handleWiFiSetup() {
        WiFiSetupFragment frag = WiFiSetupFragment.newInstance();
        MainActivity.getInstance().pushFragment(frag);
    }

    private void handleNotifications() {
        NotificationsFragment frag = new NotificationsFragment();
        MainActivity.getInstance().pushFragment(frag);
    }

    private void updateProfile() {
        EditProfileDialog d = new EditProfileDialog(getActivity());
        d.show();
    }

    private AlertDialog _confirmDeleteDialog;

    private void deleteAccount() {
        // First confirm
        _confirmDeleteDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.confirm_delete_account_title)
                .setMessage(R.string.confirm_delete_account_message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Close the dialog
                        if (SettingsFragment.this._confirmDeleteDialog != null) {
                            SettingsFragment.this._confirmDeleteDialog.dismiss();
                        }

                        MainActivity.getInstance().showWaitDialog(R.string.deleting_account_title,
                                R.string.deleting_account_message);

                        // Actually delete the account
                        AylaUser.delete(new DeleteAccountHandler());
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    static class DeleteAccountHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "Delete account result: " + msg);
            MainActivity.getInstance().dismissWaitDialog();
            if ( AylaNetworks.succeeded(msg) ) {
                // Log out and show a toast
                SessionManager.clearSavedUser();
                SessionManager.stopSession();
                Toast.makeText(MainActivity.getInstance(), R.string.account_deleted, Toast.LENGTH_LONG).show();
            } else {
                // Look for an error message in the returned JSON
                String errorMessage = null;
                try {
                    JSONObject results = new JSONObject((String)msg.obj);
                    errorMessage = results.getString("error");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if ( errorMessage != null ) {
                    Toast.makeText(MainActivity.getInstance(), errorMessage, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.getInstance(), R.string.unknown_error, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private static class GetSharesHandler extends Handler {
        private final static String LOG_TAG = "GetSharesHandler";

        public GetSharesHandler() {}

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "GetSharesHandler: " + msg);
            if ( AylaNetworks.succeeded(msg) ) {
                AylaShare[] shares = AylaSystemUtils.gson.fromJson((String)msg.obj, AylaShare[].class);
                for ( AylaShare share : shares ) {
                    Log.d(LOG_TAG, share.toString());
                }
            }
        }
    }

    private void handleShares() {
        Log.d(LOG_TAG, "handleShares()");

        SharesFragment frag = SharesFragment.newInstance();
        MainActivity.getInstance().pushFragment(frag);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(LOG_TAG, "onItemClick: " + position);
        switch ( position ) {
            case INDEX_REGISTRATION:
                handleRegistration();
                break;

            case INDEX_CONTACTS:
                handleContacts();
                break;

            case INDEX_WIFI_SETUP:
                handleWiFiSetup();
                break;

            case INDEX_NOTIFICATIONS:
                handleNotifications();
                break;

            case INDEX_PROFILE:
                updateProfile();
                break;

            case INDEX_DELETE_ACCOUNT:
                deleteAccount();
                break;

            case INDEX_SHARES:
                handleShares();
                break;

            default:
                Toast.makeText(getActivity(), "Coming soon!", Toast.LENGTH_SHORT).show();
        }
    }
}
