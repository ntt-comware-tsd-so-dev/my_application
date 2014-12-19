package com.aylanetworks.agilelink.framework;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import com.aylanetworks.aaml.AylaCache;
import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaReachability;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Brian  King on 12/19/14.
 *
 * The SessionManager class is used to manage the login session with the Ayla service, as well as
 * polling the list of devices.
 *
 */


public class SessionManager {
    /** Constants */

    // Local preferences keys
    public final static String PREFS_PASSWORD = "password";
    public final static String PREFS_USERNAME = "username";

    /** Interfaces */

    public interface SessionListener {
        public void loginStateChanged(boolean loggedIn, AylaUser aylaUser);
        public void reachabilityChanged(int reachabilityState);
    }

    public static SessionManager getInstance() {
        if ( _globalSessionManager == null ) {
            _globalSessionManager = new SessionManager();
        }
        return _globalSessionManager;
    }

    public static void addSessionListener(SessionListener listener) {
        getInstance()._sessionListeners.add(listener);
    }

    public static void removeSessionListener(SessionListener listener) {
        getInstance()._sessionListeners.remove(listener);
    }

    /** Implementers should create the correct device type based on the provided AylaDevice. */
    public interface DeviceClassMap {
        public Class<? extends AylaDevice> classForDeviceType(String deviceType);
    }

    // Default implementation of the DeviceClassMap: All devices are AylaDevices.
    public static class DefaultDeviceClassMap implements DeviceClassMap {
        public Class<? extends AylaDevice> classForDeviceType(String deviceType) {
            return AylaDevice.class;
        }
    }

    /** Inner Classes */

    /** Class used to provide session parameters. */
    public static class SessionParameters {
        public Context context = null;
        public String deviceSsidRegex = "^Ayla-[0-9A-Fa-f]{12}|^T-Stat-[0-9A-Fa-f]{12}";
        public String appVersion = "0.1";
        public String pushNotificationSenderId = "103052998040";
        public String appId = "aMCA-id";
        public String appSecret = "aMCA-9097620";
        public DeviceClassMap deviceClassMap = new DefaultDeviceClassMap();
        public String username;
        public String password;
        public int serviceType = AylaNetworks.AML_STAGING_SERVICE;
        public int loggingLevel = AylaNetworks.AML_LOGGING_LEVEL_ERROR;

        public SessionParameters(Context context) {
            this.context = context;
        }

        /** Copy constructor */
        public SessionParameters(SessionParameters other) {
            this.context = other.context;
            this.deviceSsidRegex = other.deviceSsidRegex;
            this.appVersion = other.appVersion;
            this.pushNotificationSenderId = other.pushNotificationSenderId;
            this.appId = other.appId;
            this.appSecret = other.appSecret;
            this.deviceClassMap = other.deviceClassMap;
            this.username = other.username;
            this.password = other.password;
            this.serviceType = other.serviceType;
            this.loggingLevel = other.loggingLevel;
        }

        @Override
        public String toString() {
            return "SessionParameters: \n" +
                    "  Context: " + context + "\n" +
                    "  Regex: " + deviceSsidRegex + "\n" +
                    "  appVersion: " + appVersion + "\n" +
                    "  senderId: " + pushNotificationSenderId + "\n" +
                    "  appId: " + appId + "\n" +
                    "  appSecret: " + appSecret + "\n" +
                    "  deviceClassMap: " + deviceClassMap + "\n" +
                    "  username: " + username + "\n" +
                    "  password: " + password + "\n" +
                    "  serviceType: " + serviceType + "\n" +
                    "  loggingLevel: " + loggingLevel;
        }
    }

    /** Initializes and starts a new session, stopping any existing sessions first. */
    public static boolean startSession(SessionParameters params) {
        Log.d(LOG_TAG, "Starting session with parameters:\n" + params);

        SessionManager sm = getInstance();
        sm._sessionParameters = new SessionParameters(params);

        return sm.start();
    }

    /** Closes network connections and logs out the user */
    public static boolean stopSession() {
        getInstance().stop();
        return true;
    }

    /** Returns a copy of the current session parameters */
    public static SessionParameters sessionParameters() {
        // Return a copy. We don't want things changing underneath us.
        return new SessionParameters(getInstance()._sessionParameters);
    }

    /** Returns the device manager, or null if not logged in yet */
    public static DeviceManager deviceManager() {
        return getInstance()._deviceManager;
    }


/**************************************************************************************************/


    /** Constants */
    private final static String LOG_TAG = "SessionManager";

    // Ayla library setting keys
    private final static String AYLA_SETTING_CURRENT_USER = "currentUser";

    /** Private Members */

    /** The session manager singleton object */
    private static SessionManager _globalSessionManager;

    /** Session parameters used to configure the Session Manager */
    private SessionParameters _sessionParameters;

    /** The AylaUser object returned on successful login */
    private AylaUser _aylaUser;

    /** The device manager object. Null until the user logs in. */
    private DeviceManager _deviceManager;

    /** Our listeners */
    private Set<SessionListener> _sessionListeners;

    /** Private constructor. Call startSession instead of creating a SessionManager directly. */
    private SessionManager() {
        _sessionListeners = new HashSet<>();
    }

    /** Private Methods */

    /** Logs in and fetches the initial list of devices */
    private boolean start() {
        // First check the session parameters
        if ( !checkParameters() ) {
            return false;
        }

        // Initialize the Ayla library
        AylaNetworks.init(_sessionParameters.context,
                          _sessionParameters.deviceSsidRegex,
                          _sessionParameters.appId);

        AylaSystemUtils.loggingLevel = _sessionParameters.loggingLevel;
        AylaSystemUtils.serviceType = _sessionParameters.serviceType;

        // Log in
        logIn();

        return true;
    }

    private boolean stop() {
        AylaCache.clearAll();
        return true;
    }

    /** Handle the result of the login request */
    private final Handler loginHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "Login Handler");
            Log.d(LOG_TAG, "Message: " + msg);

            if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
                Log.d(LOG_TAG, "Login successful");

                // Save the auth info for the user
                _aylaUser = AylaSystemUtils.gson.fromJson((String)msg.obj, AylaUser.class);
                _aylaUser.email = _sessionParameters.username;
                _aylaUser.password = _sessionParameters.password;
                _aylaUser = AylaUser.setCurrent(_aylaUser);
                String userJson = AylaSystemUtils.gson.toJson(_aylaUser, AylaUser.class);
                AylaSystemUtils.saveSetting(AYLA_SETTING_CURRENT_USER, userJson);

                _aylaUser.password = _sessionParameters.password;
                AylaCache.clearAll();
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(_sessionParameters.context);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(PREFS_PASSWORD, _sessionParameters.password);
                editor.putString(PREFS_USERNAME, _sessionParameters.username);
                editor.apply();

                // Create the device manager
                _deviceManager = new DeviceManager();
                _deviceManager.refreshDeviceList();

                notifyLoginSuccess();
            }
        }
    };

    private void logIn() {
        // Make sure the network is reachable
        AylaReachability.determineReachability(true);
        if ( AylaReachability.getReachability() == AylaNetworks.AML_REACHABILITY_UNREACHABLE ) {
            // We can't go on
            Log.d(LOG_TAG, "Network is not reachable. Not attempting login.");
            notifyLoginFailed();
            return;
        }

        // First attempt to update the access token using the refresh token
        String savedUser = AylaSystemUtils.loadSavedSetting(AYLA_SETTING_CURRENT_USER, "");
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(_sessionParameters.context);
        String savedPassword = settings.getString(PREFS_PASSWORD, "");

        AylaUser user = null;
        if ( !savedUser.equals("") ) {
            user = AylaSystemUtils.gson.fromJson(savedUser, AylaUser.class);
        }

        // If cached credentials match, use the refresh token
        if ( user != null && user.email != null && user.email.equals(_sessionParameters.username) &&
             savedPassword.equals(_sessionParameters.password) ) {
            // We have a cached user and password that match. Try to refresh the access token.
            // the refresh token.
            Log.d(LOG_TAG, "Refreshing access token");
            AylaUser.refreshAccessToken(loginHandler, user.getRefreshToken());
        } else {
            // Normal login
            Log.d(LOG_TAG, "Normal login");
            AylaUser.login(loginHandler,
                    _sessionParameters.username,
                    _sessionParameters.password,
                    _sessionParameters.appId,
                    _sessionParameters.appSecret);
        }
    }

    private boolean checkParameters() {
        if ( _sessionParameters.context == null ) {
            return false;
        }
        if ( _sessionParameters.deviceSsidRegex == null ) {
            return false;
        }
        if ( _sessionParameters.appId == null ) {
            return false;
        }
        if ( _sessionParameters.appSecret == null ) {
            return false;
        }
        if ( _sessionParameters.appVersion == null ) {
            return false;
        }
        if ( _sessionParameters.deviceClassMap == null ) {
            return false;
        }
        if ( _sessionParameters.password == null ) {
            return false;
        }
        if ( _sessionParameters.username == null ) {
            return false;
        }

        return true;
    }

    /** Notifiers */
    private void notifyLoginSuccess() {
        for ( SessionListener l : _sessionListeners ) {
            l.loginStateChanged(true, _aylaUser);
        }
    }

    private void notifyLoginFailed() {
        for ( SessionListener l : _sessionListeners ) {
            l.loginStateChanged(false, null);
        }
    }
}
