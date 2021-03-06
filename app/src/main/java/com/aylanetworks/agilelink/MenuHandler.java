package com.aylanetworks.agilelink;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.agilelink.batch.BatchActionsFragment;
import com.aylanetworks.agilelink.beacon.AMAPBeaconService;
import com.aylanetworks.agilelink.beacon.BeaconsListFragment;
import com.aylanetworks.agilelink.fragments.AboutFragment;
import com.aylanetworks.agilelink.fragments.AddDeviceFragment;
import com.aylanetworks.agilelink.fragments.AllDevicesFragment;
import com.aylanetworks.agilelink.fragments.ContactListFragment;
import com.aylanetworks.agilelink.fragments.DeviceGroupsFragment;
import com.aylanetworks.agilelink.fragments.DeviceNotificationsFragment;
import com.aylanetworks.agilelink.fragments.EditProfileFragment;
import com.aylanetworks.agilelink.fragments.GatewayDevicesFragment;
import com.aylanetworks.agilelink.fragments.HelpFragment;
import com.aylanetworks.agilelink.fragments.SharesFragment;
import com.aylanetworks.agilelink.fragments.WelcomeFragment;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.Logger;
import com.aylanetworks.agilelink.geofence.AllGeofencesFragment;
import com.aylanetworks.agilelink.automation.AutomationListFragment;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaSessionManager;
import com.aylanetworks.aylasdk.AylaUser;
import com.aylanetworks.aylasdk.auth.CachedAuthProvider;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;

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
    public static final String LOG_TAG = "MenuHandler";

    /**
     * Handles the given menu item, or returns false if not handled
     * @param menuItem Menu item to handle
     * @return true if handled, false if not handled
     */
    public static boolean handleMenuItem(MenuItem menuItem) {
        MainActivity.getInstance().activateMenuItem(menuItem);
        return handleMenuId(menuItem.getItemId());
    }

    /**
     * Handles the given menu id, or returns false if not handled.
     * @param id Menu item id to handle.
     * @return true if handled, false if not handled.
     */
    public static boolean handleMenuId(int id) {
        switch (id) {
            case R.id.action_all_devices:
                handleAllDevices();
                break;

            case R.id.action_device_groups:
                handleDeviceGroups();
                break;

            case R.id.action_gateways:
                handleGateways();
                break;

            case R.id.action_add_device:
                handleAddDevice();
                break;

            case R.id.action_contact_list:
                handleContacts();
                break;
            case R.id.action_settings:
                handleSettings();
                break;
            case R.id.action_notifications:
                handleNotifications();
                break;

            case R.id.action_account:
            case R.id.action_edit_profile:
                updateProfile();
                break;

            case R.id.action_geofences:
                handleGeoFences();
                break;
            case R.id.action_beacons:
                handleBeacons();
                break;

            case R.id.action_al_actions:
                handleActions();
                break;

            case R.id.action_automations:
                handleAutomations();
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

            case R.id.action_help:
                help();
                break;

            case R.id.action_about:
                about();
                break;

            default:
                return false;
        }
        return true;
    }

    static void replaceFragmentToRoot(Fragment frag) {
        Logger.logInfo(LOG_TAG, "replaceFragmentToRoot " + frag.getClass().getSimpleName());
        FragmentManager fm = MainActivity.getInstance().getSupportFragmentManager();
        MainActivity.getInstance().popBackstackToRoot();
        fm.beginTransaction().replace(R.id.content_frame, frag).commit();
    }

    public static void handleAllDevices() {
        replaceFragmentToRoot(AllDevicesFragment.newInstance());
    }

    public static void handleGateways() {
        replaceFragmentToRoot(GatewayDevicesFragment.newInstance());
    }

    public static void handleDeviceGroups() {
        replaceFragmentToRoot(DeviceGroupsFragment.newInstance());
    }

    public static void handleGeoFences() {
        replaceFragmentToRoot(AllGeofencesFragment.newInstance());
    }

    public static void handleBeacons() {
        replaceFragmentToRoot(BeaconsListFragment.newInstance());
    }

    public static void handleActions() {
        replaceFragmentToRoot(BatchActionsFragment.newInstance());
    }

    public static void handleAutomations() {
        replaceFragmentToRoot(AutomationListFragment.newInstance());
    }

    public static void handleAddDevice() {
        if(MainActivity.getInstance() != null){
            MainActivity.getInstance().pushFragment(AddDeviceFragment.newInstance());
        }
    }

    public static void handleGatewayWelcome() {
        MainActivity.getInstance().pushFragment(WelcomeFragment.newInstance());
    }

    public static void handleContacts() {
        replaceFragmentToRoot(ContactListFragment.newInstance());
    }

    public static void handleNotifications() {
        MainActivity.getInstance().pushFragment(DeviceNotificationsFragment.newInstance());
    }

    public static void updateProfile() {
        replaceFragmentToRoot(EditProfileFragment.newInstance());
    }

    public static void handleSettings() {
        Intent intent = new Intent(MainActivity.getInstance(), FingerPrintSettingsActivity.class);
        MainActivity.getInstance().startActivity(intent);
    }

    public static AlertDialog _confirmDeleteDialog;

    public static void deleteAccount() {
        // First confirm
        Resources res = MainActivity.getInstance().getResources();
        String msg = res.getString(R.string.confirm_delete_account_message);
        _confirmDeleteDialog = new AlertDialog.Builder(MainActivity.getInstance())
                .setIcon(R.drawable.ic_launcher)
                .setTitle(R.string.confirm_delete_account_title)
                .setMessage(msg)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Close the dialog
                        if (_confirmDeleteDialog != null) {
                            _confirmDeleteDialog.dismiss();
                        }
                        MainActivity.getInstance().showWaitDialog(R.string.deleting_account_title, R.string.deleting_account_message);
                        // Actually delete the account
                        Logger.logDebug(LOG_TAG, "user: AylaUser.delete");
                        AMAPCore.sharedInstance().getSessionManager().deleteAccount(
                                new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                                    @Override
                                    public void onResponse(AylaAPIRequest.EmptyResponse response) {
                                        // Log out and show a toast
                                        shutdownSession();
                                        Toast.makeText(MainActivity.getInstance(), R.string.account_deleted, Toast.LENGTH_LONG).show();
                                    }
                                },
                                new ErrorListener() {
                                    @Override
                                    public void onErrorResponse(AylaError error) {
                                        Toast.makeText(MainActivity.getInstance(),
                                                ErrorUtils.getUserMessage(MainActivity.getInstance(), error, R.string.unknown_error),
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                        );
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    public static void handleShares() {
        replaceFragmentToRoot(SharesFragment.newInstance());
    }

    public static void signOut() {
        final Activity activity = MainActivity.getInstance();
        if (activity != null) {
            // Confirm
            Resources res = activity.getResources();
            AylaUser currentUser = AMAPCore.sharedInstance().getCurrentUser();
            String email ="";
            if(currentUser != null && currentUser.getEmail() != null) {
                email = currentUser.getEmail();
            }

            String msg = res.getString(R.string.confirm_sign_out_message, email);
            new AlertDialog.Builder(activity)
                    .setIcon(R.drawable.ic_launcher)
                    .setTitle(R.string.confirm_sign_out)
                    .setMessage(msg)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Logger.logInfo(LOG_TAG, "signOut: stop session.");
                            shutdownSession();
                            stopBeaconService(activity);
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .create().show();
        } else {
            Logger.logInfo(LOG_TAG, "signOut: MainActivity has already closed, stop session.");
            shutdownSession();
        }
    }

    private static void shutdownSession() {
        if (AMAPCore.sharedInstance() == null) {
            Logger.logError(LOG_TAG, "AMAPCore.sharedInstance is null.");
            return;
        }

        AylaSessionManager sessionManager = AMAPCore.sharedInstance().getSessionManager();
        if (sessionManager == null) {
            Logger.logError(LOG_TAG, "AylaSessionManager is null.");
            return;
        }
        MainActivity.getInstance().showWaitDialog(MainActivity.getInstance().getString(
                R.string.signing_out), null);
        sessionManager.shutDown(new Response
                .Listener<AylaAPIRequest.EmptyResponse>() {
            @Override
            public void onResponse(AylaAPIRequest.EmptyResponse response) {
                MainActivity.getInstance().dismissWaitDialog();
                Toast.makeText(MainActivity.getInstance(), "Successfully exited session", Toast.LENGTH_SHORT).show();
                CachedAuthProvider.clearCachedAuthorization(MainActivity.getInstance());
                MainActivity.getInstance().showLoginDialog(true);
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                MainActivity.getInstance().dismissWaitDialog();
                CachedAuthProvider.clearCachedAuthorization(MainActivity.getInstance());
                Toast.makeText(MainActivity.getInstance(),
                        ErrorUtils.getUserMessage(MainActivity.getInstance(), error, R.string.unknown_error),
                        Toast.LENGTH_LONG).show();
                MainActivity.getInstance().showLoginDialog(true);
            }
        });
    }

    private static void stopBeaconService(Activity activity) {
        activity.stopService(new Intent(activity, AMAPBeaconService.class));
    }

    public static void about() {
        AboutFragment frag = new AboutFragment();
        MainActivity.getInstance().pushFragment(frag);
    }

    public static void help() {
        HelpFragment frag = new HelpFragment();
        MainActivity.getInstance().pushFragment(frag);
    }

}
