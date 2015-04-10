package com.aylanetworks.agilelink.framework;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.AboutFragment;
import com.aylanetworks.agilelink.fragments.AddDeviceFragment;
import com.aylanetworks.agilelink.fragments.AllDevicesFragment;
import com.aylanetworks.agilelink.fragments.ContactListFragment;
import com.aylanetworks.agilelink.fragments.DeviceGroupsFragment;
import com.aylanetworks.agilelink.fragments.EditProfileDialog;
import com.aylanetworks.agilelink.fragments.NotificationsFragment;
import com.aylanetworks.agilelink.fragments.SharesFragment;
import com.aylanetworks.agilelink.fragments.WiFiSetupFragment;

import org.json.JSONException;
import org.json.JSONObject;

/*
 * MenuHandler.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 4/10/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

/**
 * The MenuHandler class is a static helper class used to process menu events. This class
 * will perform the necessary operations for menu items from the Settings menu or navigation
 * drawer menu.
 */
public class MenuHandler {
    private static final String LOG_TAG = "MenuHandler";

    /**
     * Handles the given menu item, or returns false if not handled
     * @param menuItem Menu item to handle
     * @return true if handled, false if not handled
     */
    public static boolean handleMenuItem(MenuItem menuItem) {
        switch ( menuItem.getItemId() ) {
            case R.id.action_all_devices:
                handleAllDevices();
                break;

            case R.id.action_device_groups:
                handleDeviceGroups();
                break;

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

            case R.id.action_about:
                about();
                break;

            default:
                Toast.makeText(MainActivity.getInstance(), "Coming soon!", Toast.LENGTH_SHORT).show();
                return false;
        }

        return true;
    }

    private static void handleAllDevices() {
        int displayMode = MainActivity.getUIConfig()._listStyle == UIConfig.ListStyle.List ?
                AllDevicesFragment.DISPLAY_MODE_LIST : AllDevicesFragment.DISPLAY_MODE_GRID;
        Fragment frag = AllDevicesFragment.newInstance(displayMode);
        FragmentManager fm = MainActivity.getInstance().getSupportFragmentManager();
        MainActivity.getInstance().popBackstackToRoot();
        fm.beginTransaction().replace(R.id.content_frame, frag).commit();
    }

    private static void handleDeviceGroups() {
        int displayMode = MainActivity.getUIConfig()._listStyle == UIConfig.ListStyle.List ?
                AllDevicesFragment.DISPLAY_MODE_LIST : AllDevicesFragment.DISPLAY_MODE_GRID;
        Fragment frag = DeviceGroupsFragment.newInstance(displayMode);
        FragmentManager fm = MainActivity.getInstance().getSupportFragmentManager();
        MainActivity.getInstance().popBackstackToRoot();
        fm.beginTransaction().replace(R.id.content_frame, frag).commit();
    }

    private static void handleRegistration() {
        // Bring up the Add Device UI
        AddDeviceFragment frag = AddDeviceFragment.newInstance();
        MainActivity.getInstance().pushFragment(frag);
    }

    private static void handleContacts() {
        ContactListFragment frag = ContactListFragment.newInstance();
        MainActivity.getInstance().pushFragment(frag);
    }

    private static void handleWiFiSetup() {
        WiFiSetupFragment frag = WiFiSetupFragment.newInstance();
        MainActivity.getInstance().pushFragment(frag);
    }

    private static void handleNotifications() {
        NotificationsFragment frag = new NotificationsFragment();
        MainActivity.getInstance().pushFragment(frag);
    }

    private static void updateProfile() {
        EditProfileDialog d = new EditProfileDialog(MainActivity.getInstance());
        d.show();
    }


    private static AlertDialog _confirmDeleteDialog;

    private static void deleteAccount() {
        // First confirm
        _confirmDeleteDialog = new AlertDialog.Builder(MainActivity.getInstance())
                .setTitle(R.string.confirm_delete_account_title)
                .setMessage(R.string.confirm_delete_account_message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Close the dialog
                        if (_confirmDeleteDialog != null) {
                            _confirmDeleteDialog.dismiss();
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

    private static void handleShares() {
        Log.d(LOG_TAG, "handleShares()");

        SharesFragment frag = SharesFragment.newInstance();
        MainActivity.getInstance().pushFragment(frag);
    }

    private static void signOut() {
        // Confirm
        new AlertDialog.Builder(MainActivity.getInstance())
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

    private static void about() {
        AboutFragment frag = new AboutFragment();
        MainActivity.getInstance().pushFragment(frag);
    }

}
