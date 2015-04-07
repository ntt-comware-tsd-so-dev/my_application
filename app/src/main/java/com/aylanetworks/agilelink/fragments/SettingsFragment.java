package com.aylanetworks.agilelink.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.internal.view.menu.MenuBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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

import java.util.ArrayList;
import java.util.List;

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
    private List<MenuItem> _menuItems;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        _listView = (ListView)view.findViewById(R.id.listView);

        // Load the menu resource. We will be using the menu items in our listview.
        Menu menu = new MenuBuilder(getActivity());
        new MenuInflater(getActivity()).inflate(R.menu.menu_settings, menu);

        _menuItems = new ArrayList<MenuItem>();
        for ( int i = 0; i < menu.size(); i++ ) {
            MenuItem item = menu.getItem(i);
            Log.d(LOG_TAG, "Menu item " + i + ": " + item);
            _menuItems.add(item);
        }

        ArrayAdapter<MenuItem>adapter = new ArrayAdapter<MenuItem>(getActivity(), android.R.layout.simple_list_item_1, _menuItems);
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

    private void signOut() {
        // Confirm
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.confirm_sign_out)
                .setMessage(R.string.confirm_sign_out_message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SessionManager.stopSession();
                            }
                        })
                .setNegativeButton(android.R.string.no, null)
                .create().show();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(LOG_TAG, "onItemClick: " + position);
        MenuItem item = _menuItems.get(position);
        switch ( item.getItemId() ) {
            case R.id.action_registration:
                handleRegistration();
                break;

            case R.id.action_contact_list:
                handleContacts();
                break;

            case R.id.action_wifi_setup:
                handleWiFiSetup();
                break;

            case R.id.action_notifications:
                handleNotifications();
                break;

            case R.id.action_edit_profile:
                updateProfile();
                break;

            case R.id.action_delete_account:
                deleteAccount();
                break;

            case R.id.action_shares:
                handleShares();
                break;

            case R.id.action_sign_out:
                signOut();
                break;

            default:
                Toast.makeText(getActivity(), "Coming soon!", Toast.LENGTH_SHORT).show();
        }
    }
}
