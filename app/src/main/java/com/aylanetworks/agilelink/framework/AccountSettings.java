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

/**
 * Created by Brian King on 2/17/15.
 * Class used to store / retrieve AylaDatum-based settings for the account
 */
public class AccountSettings {
    private static final String LOG_TAG = "AccountSettings";

    private static final String NOTIFICATIONS_KEY = "device-notifications";

    private static final String SETTINGS_KEY_PUSH = "enable-push-notifications";

    private Set<String> _notificationTypes;
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
     * Fetches the account settings from the server
     * @param user Account user that owns the settings
     * @param callback Callback called when the settings have been retrieved from the server
     */
    public static void fetchAccountSettings(AylaUser user, AccountSettingsCallback callback) {
        // Get the datum
        user.getDatumWithKey(new FetchDatumHandler(user, callback), getDatumName());
    }

    public void pushToServer(AccountSettingsCallback callback) {
        AylaDatum datum = createDatum();
        _aylaUser.updateDatum(new UpdateDatumHandler(this, callback), datum);
    }

    public AccountSettings(AylaUser aylaUser){
        _aylaUser = aylaUser;
        _notificationTypes = new HashSet<>();
    }

    public List<String> getNotificationTypes() {
        return new ArrayList<>(_notificationTypes);
    }

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

    public boolean isNotificationMethodSet(String notificationMethod) {
        if ( notificationMethod.equals(DeviceNotificationHelper.NOTIFICATION_METHOD_PUSH) ) {
            // Push is stored in local settings
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.getInstance());
            return settings.getBoolean(SETTINGS_KEY_PUSH, false);
        }

        return _notificationTypes.contains(notificationMethod);
    }

    /**
     * Returns the key of the AylaDatum object used to store the account settings
     * @return the key into the account settings datum
     */
    static protected String getDatumName() {
        return SessionManager.sessionParameters().appId + "-settings";
    }

    protected AylaDatum createDatum() {
        AylaDatum datum = new AylaDatum();
        datum.key = getDatumName();

        JSONArray array = new JSONArray(_notificationTypes);
        JSONObject obj = new JSONObject();

        try {
            obj.put(NOTIFICATIONS_KEY, array);
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
