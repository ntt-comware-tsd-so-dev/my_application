package com.aylanetworks.agilelink.framework;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import com.aylanetworks.aaml.AylaDatum;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.MainActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * AccountSettings.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 2/17/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */


/**
 * AccountSettings is a class used to store and retrieve information related to the user's account,
 * but not part of the AylaUser object. Currently the class stores the following information:
 * <ul>
 *     <li>The selected notification types for device status: Push, SMS and Email
 *     <li>The contact ID of the owner
 * </ul>
 * <p>
 * The AccountSettings object can be retreived from the server using tht static method
 * {@link #fetchAccountSettings(com.aylanetworks.aaml.AylaUser, com.aylanetworks.agilelink.framework.AccountSettings.AccountSettingsCallback)}.
 * This method will fetch the latest AccountSettings information from the server and return an
 * initialized AccountSettings object to the callback.
 *
 * The SessionManager fetches the AccountSettings from the server after login. This can be accessed
 * via {@link SessionManager#getAccountSettings()}.
 */
public class AccountSettings {
    private static final String LOG_TAG = "AccountSettings";

    // Keys set in the datum JSON
    // Key to an array of strings of enabled notification types. Can be either
    // NOTIFICATION_METHOD_EMAIL or NOTIFICATION_METHOD_SMS.
    private static final String NOTIFICATIONS_KEY = "device-notifications";

    // Key to a string representing the contact ID of the account owner (AylaUser).
    private static final String OWNER_ID_KEY = "owner-contact-id";

    // Keys into the local shared preferences database
    private static final String SETTINGS_KEY_PUSH = "enable-push-notifications";

    private Set<String> _notificationTypes;
    private Integer _ownerContactID;
    private AylaUser _aylaUser;

    public static class AccountSettingsCallback {
        /** Called once the account settings have been fetched or set. If an error occurred,
         * settings will be null and the msg parameter contains the result returned from the server.
         * @param settings Newly fetched or pushed AccountSettings object
         * @param msg Message returned from the server in response to fetching or pushing
         *            the AlyaDatum object
         */
        public void settingsUpdated(AccountSettings settings, Message msg){}
    };

    /**
     * Fetches the account settings from the server. The callback is called with an initialized
     * AccountSettings object, or null if an error occurred.
     *
     * @param user Account user that owns the settings
     * @param callback Callback called when the settings have been retrieved from the server
     */
    public static void fetchAccountSettings(AylaUser user, AccountSettingsCallback callback) {
        // Get the datum
        user.getDatumWithKey(new FetchDatumHandler(user, callback), getDatumName());
    }

    /**
     * Updates data on the server with the current AccountSettings information.
     * @param callback Callback called when the settings have been updated.
     */
    public void pushToServer(AccountSettingsCallback callback) {
        AylaDatum datum = createDatum();
        _aylaUser.updateDatum(new UpdateDatumHandler(this, callback), datum);
    }

    /**
     * Constructor with an AylaUser initializer
     * @param aylaUser AylaUser that owns the AccountSettings information
     */
    public AccountSettings(AylaUser aylaUser){
        _aylaUser = aylaUser;
        _ownerContactID = null;
        _notificationTypes = new HashSet<>();
    }

    /**
     * Returns a list of notification types that are enabled for this account. This list can include
     * one or more of:
     * <ul>
     *     <li>{@link com.aylanetworks.agilelink.framework.DeviceNotificationHelper#NOTIFICATION_METHOD_EMAIL}
     *     <li>{@link com.aylanetworks.agilelink.framework.DeviceNotificationHelper#NOTIFICATION_METHOD_PUSH}
     *     <li>{@link com.aylanetworks.agilelink.framework.DeviceNotificationHelper#NOTIFICATION_METHOD_SMS}
     * </ul>
     * @return an array of Strings representing the enabled device notification types for this account
     */
    public List<String> getNotificationTypes() {
        return new ArrayList<>(_notificationTypes);
    }

    /**
     * Adds the specified notification method to the user's preferences. Note that this only stores
     * the information and does not update any device notifications on the devices themselves.
     *
     * Callers should call the {@link #pushToServer(com.aylanetworks.agilelink.framework.AccountSettings.AccountSettingsCallback)}
     * method in order to persist changes to AccountSettings once all local updates have been made.
     *
     * @param notificationMethod
     */
    public void addNotificationMethod(String notificationMethod) {
        if ( notificationMethod.equals(DeviceNotificationHelper.NOTIFICATION_METHOD_PUSH)) {
            // We write this into shared preferences, as this setting is on a per-device basis
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.getInstance());
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(SETTINGS_KEY_PUSH, true);
            editor.apply();
        } else {
            _notificationTypes.add(notificationMethod);
        }
    }

    /**
     * Removes the supplied notification method from the list of enabled notification methods.
     *
     * Callers should call the {@link #pushToServer(com.aylanetworks.agilelink.framework.AccountSettings.AccountSettingsCallback)}
     * method in order to persist changes to AccountSettings once all local updates have been made.
     *
     * @param notificationMethod
     */
    public void removeNotificationMethod(String notificationMethod) {
        if ( notificationMethod.equals(DeviceNotificationHelper.NOTIFICATION_METHOD_PUSH)) {
            // We write this into shared preferences, as this setting is on a per-device basis
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.getInstance());
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(SETTINGS_KEY_PUSH, false);
            editor.apply();
        }
        _notificationTypes.remove(notificationMethod);
    }

    /**
     * Returns true if the supplied notification method is set on the user's account.
     * @param notificationMethod String of the notification method to test
     * @return true if the method is set, or false if not
     */
    public boolean isNotificationMethodSet(String notificationMethod) {
        if ( notificationMethod.equals(DeviceNotificationHelper.NOTIFICATION_METHOD_PUSH) ) {
            // Push is stored in local settings
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.getInstance());
            return settings.getBoolean(SETTINGS_KEY_PUSH, false);
        }

        return _notificationTypes.contains(notificationMethod);
    }

    /**
     * Sets the owner's contact ID. This ID is used when examining the Contacts list to identify
     * the contact associated with the owner's account (AylaUser).
     *
     * @param ownerContactID The contact ID of the owner
     */
    public void setOwnerContactID(Integer ownerContactID) {
        _ownerContactID = ownerContactID;
    }

    /**
     * Returns the owner's contact ID. This ID is used when examining the Contacts list to identify
     * the contact associated with the owner's account (AylaUser).
     *
     * @return the contact ID of the owner of the account
     */
    public Integer getOwnerContactID() {
        return _ownerContactID;
    }

    /**
     * Returns the key of the AylaDatum object used to store the account settings
     * @return the key into the account settings datum
     */
    static protected String getDatumName() {
        return SessionManager.sessionParameters().appId + "-settings";
    }

    /**
     * Creates the AylaDatum object containing the AccountSettings JSON data
     * @return The AylaDatum object to be set on the server with the AccountSettings JSON data
     */
    protected AylaDatum createDatum() {
        AylaDatum datum = new AylaDatum();
        datum.key = getDatumName();

        JSONArray array = new JSONArray(_notificationTypes);
        JSONObject obj = new JSONObject();

        try {
            obj.put(NOTIFICATIONS_KEY, array);
            if ( _ownerContactID != null ) {
                obj.put(OWNER_ID_KEY, _ownerContactID);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        datum.value = obj.toString();

        return datum;
    }

    protected static class FetchDatumHandler extends Handler {
        private AccountSettingsCallback _callback;
        private AylaUser _user;

        public FetchDatumHandler(AylaUser user, AccountSettingsCallback callback) {
            _user = user;
            _callback = callback;
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "Message: " + msg);

            if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
                AccountSettings settings = new AccountSettings(_user);
                // We have our settings datum.
                AylaDatum datum = AylaSystemUtils.gson.fromJson((String)msg.obj, AylaDatum.class);
                String json = datum.value;
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    JSONArray notifications = jsonObject.getJSONArray(NOTIFICATIONS_KEY);

                    if ( notifications != null ) {
                        for ( int i = 0; i < notifications.length(); i++ ) {
                            String type = notifications.getString(i);
                            settings.addNotificationMethod(type);
                        }
                    }

                    // Get the owner ID
                    settings._ownerContactID = jsonObject.getInt(OWNER_ID_KEY);

                    // Done. We can call the callback now.
                    _callback.settingsUpdated(settings, msg);
                } catch (JSONException e) {
                    e.printStackTrace();
                    _callback.settingsUpdated(null, msg);
                }
            } else {
                // Error from the server.
                // Is this just a "not found"? If so, make a new settings object and return that
                AccountSettings settings = null;
                if ( msg.arg1 == 404 ) {
                    Log.d(LOG_TAG, "Creating a new AccountSettings object (none found on server)");
                    settings = new AccountSettings(AylaUser.getCurrent());
                }
                _callback.settingsUpdated(settings, msg);
            }
        }
    }

    protected static class UpdateDatumHandler extends Handler {
        private WeakReference<AccountSettings> _accountSettings;
        private AccountSettingsCallback _callback;
        private boolean _callingCreate;

        public UpdateDatumHandler(AccountSettings accountSettings, AccountSettingsCallback callback) {
            _accountSettings = new WeakReference<>(accountSettings);
            _callback = callback;
        }

        @Override
        public void handleMessage(Message msg) {
            if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
                _callback.settingsUpdated(_accountSettings.get(), msg);
            } else {
                // Not found? Try calling create.
                if ( !_callingCreate ) {
                    Log.e(LOG_TAG, "UpdateDatum failed, trying Create instead...");
                    _callingCreate = true;
                    _accountSettings.get()._aylaUser.createDatum(this, _accountSettings.get().createDatum());
                } else {
                    Log.e(LOG_TAG, "Failed to create datum after update failed... bailing!");
                    _callback.settingsUpdated(null, msg);
                }
            }
        }
    }
}
