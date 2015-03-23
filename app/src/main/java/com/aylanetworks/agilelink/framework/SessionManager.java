package com.aylanetworks.agilelink.framework;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaCache;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaReachability;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
 * SessionManager.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 12/19/14.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

/**
 * The SessionManager class is used to manage the login session with the Ayla service, as well as
 * polling the list of devices.
 */
public class SessionManager {
    /**
     * Constants
     */

    // Local preferences keys
    public final static String PREFS_PASSWORD = "password";
    public final static String PREFS_USERNAME = "username";


    /**
     * Interfaces
     */

    public interface SessionListener {
        public void loginStateChanged(boolean loggedIn, AylaUser aylaUser);

        public void reachabilityChanged(int reachabilityState);

        public void lanModeChanged(boolean lanModeEnabled);
    }

    public static void setParameters(SessionParameters parameters) {
        getInstance()._sessionParameters = new SessionParameters(parameters);
        // Initialize the Ayla library
        AylaNetworks.init(getInstance()._sessionParameters.context,
                getInstance()._sessionParameters.deviceSsidRegex,
                getInstance()._sessionParameters.appId);

    }

    public static void clearSavedUser() {
        SharedPreferences settings =
                PreferenceManager.getDefaultSharedPreferences(getInstance()._sessionParameters.context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREFS_PASSWORD, "");
        editor.putString(PREFS_USERNAME, "");
        editor.apply();
        AylaSystemUtils.saveSetting(AYLA_SETTING_CURRENT_USER, "");
        AylaUser.setCurrent(new AylaUser());
    }

    public static SessionManager getInstance() {
        if (_globalSessionManager == null) {
            _globalSessionManager = new SessionManager();
        }
        return _globalSessionManager;
    }

    public AccountSettings getAccountSettings() {
        return _accountSettings;
    }

    public ContactManager getContactManager() {
        return _contactManager;
    }

    public static void registerNewUser(final AylaUser user, final Handler resultHandler) {
        Map<String, String> callParams = new HashMap<String, String>();
        callParams.put("email", user.email);
        callParams.put("password", user.password);
        callParams.put("firstname", user.firstname);
        callParams.put("lastname", user.lastname);
        callParams.put("country", user.country);
        callParams.put("zip", user.zip);
        callParams.put("phone", user.phone);
        callParams.put("aylaDevKitNum", user.aylaDevKitNum);

        SessionParameters params = getInstance()._sessionParameters;
        callParams.put(AylaNetworks.AML_EMAIL_TEMPLATE_ID, params.registrationEmailTemplateId);
        callParams.put(AylaNetworks.AML_EMAIL_SUBJECT, params.registrationEmailSubject);
        callParams.put(AylaNetworks.AML_EMAIL_BODY_HTML, params.registrationEmailBodyHTML);

        // We're not usually initialized at this point
        AylaNetworks.init(params.context, params.deviceSsidRegex, params.appId);

        AylaUser.signUp(resultHandler, callParams, params.appId, params.appSecret);
    }

    public static void addSessionListener(SessionListener listener) {
        synchronized (getInstance()._sessionListeners) {
            getInstance()._sessionListeners.add(listener);
        }
    }

    public static void removeSessionListener(SessionListener listener) {
        synchronized (getInstance()._sessionListeners) {
            getInstance()._sessionListeners.remove(listener);
        }
    }

    /** Inner Classes */

    /**
     * Class used to provide session parameters.
     */
    public static class SessionParameters {
        public Context context = null;
        public String deviceSsidRegex = "^Ayla-[0-9A-Fa-f]{12}|^T-Stat-[0-9A-Fa-f]{12}|^Plug-[0-9A-Fa-f]{12}";
        public String appVersion = "0.1";
        public String pushNotificationSenderId = "103052998040";
        public String appId = "aMCA-id";
        public String appSecret = "aMCA-9097620";
        public DeviceCreator deviceCreator = new DeviceCreator();
        public String username;
        public String password;
        public String accessToken;
        public String refreshToken;
        public boolean enableLANMode = false;
        public int serviceType = AylaNetworks.AML_STAGING_SERVICE;
        public int loggingLevel = AylaNetworks.AML_LOGGING_LEVEL_ERROR;

        // Strings for new user registration. Can be modified by the application if desired.
        public String registrationEmailTemplateId = "ayla_confirmation_template_01";
        public String registrationEmailSubject = "Ayla Sign-up Confirmation";
        public String registrationEmailBodyHTML = null;

        // Strings for password reset. Can be modified by the application if desired.
        public String resetPasswordEmailSubject = "Ayla Reset Password Confirmation";
        public String resetPasswordEmailTemplateId = "ayla_passwd_reset_template_01";
        public String resetPasswordEmailBodyHTML = null;

        // Strings for device notifications
        public String notificationEmailSubject = "Ayla Device Notification";
        public String notificationEmailTemplateId = "ayla_notify_template_01";
        public String notificationEmailBodyHTML = null;

        public SessionParameters(Context context) {
            this.context = context;
        }

        /**
         * Copy constructor
         */
        public SessionParameters(SessionParameters other) {
            this.context = other.context;
            this.deviceSsidRegex = other.deviceSsidRegex;
            this.appVersion = other.appVersion;
            this.pushNotificationSenderId = other.pushNotificationSenderId;
            this.appId = other.appId;
            this.appSecret = other.appSecret;
            this.deviceCreator = other.deviceCreator;
            this.username = other.username;
            this.password = other.password;
            this.accessToken = other.accessToken;
            this.refreshToken = other.refreshToken;
            this.serviceType = other.serviceType;
            this.loggingLevel = other.loggingLevel;
            this.enableLANMode = other.enableLANMode;
            this.registrationEmailSubject = other.registrationEmailSubject;
            this.registrationEmailTemplateId = other.registrationEmailTemplateId;
            this.registrationEmailBodyHTML = other.registrationEmailBodyHTML;
            this.resetPasswordEmailSubject = other.resetPasswordEmailSubject;
            this.resetPasswordEmailBodyHTML = other.resetPasswordEmailBodyHTML;
            this.resetPasswordEmailTemplateId = other.resetPasswordEmailTemplateId;
            this.notificationEmailSubject = other.notificationEmailSubject;
            this.notificationEmailTemplateId = other.notificationEmailTemplateId;
            this.notificationEmailBodyHTML = other.notificationEmailBodyHTML;
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
                    "  deviceCreator: " + deviceCreator + "\n" +
                    "  username: " + username + "\n" +
                    "  password: " + password + "\n" +
                    "  accessToken: " + accessToken + "\n" +
                    "  refreshToken: " + refreshToken + "\n" +
                    "  serviceType: " + serviceType + "\n" +
                    "  loggingLevel: " + loggingLevel + "\n" +
                    "  enableLANMode: " + enableLANMode +
                    "  registrationEmailTemplateId: " + registrationEmailTemplateId + "\n" +
                    "  registrationEmailSubject: " + registrationEmailSubject + "\n" +
                    "  registrationEmailBodyHTML: " + registrationEmailBodyHTML + "\n" +
                    "\n";
        }
    }

    /**
     * Returns true if the user is logged in, or false otherwise.
     *
     * @return true if the user is logged in
     */
    public static boolean isLoggedIn() {
        return (deviceManager() != null);
    }

    /**
     * Initializes and starts a new session, stopping any existing sessions first.
     */
    public static boolean startSession(String username, String password) {
        if (getInstance()._sessionParameters == null) {
            Log.e(LOG_TAG, "Can't start a session before the session parameters are set.");
            return false;
        }

        SessionManager sm = getInstance();

        sm._sessionParameters.username = username;
        sm._sessionParameters.password = password;

        Log.d(LOG_TAG, "Starting session with parameters:\n" + getInstance()._sessionParameters);

        return sm.start();
    }

    public static void startOAuthSession(Message oAuthResponseMessage) {
        getInstance()._loginHandler.handleMessage(oAuthResponseMessage);
    }

    /**
     * Closes network connections and logs out the user
     */
    public static boolean stopSession() {
        getInstance().stop();
        return true;
    }

    /**
     * Returns a copy of the current session parameters
     */
    public static SessionParameters sessionParameters() {
        if (getInstance()._sessionParameters == null) {
            return null;
        }
        // Return a copy. We don't want things changing underneath us.
        return new SessionParameters(getInstance()._sessionParameters);
    }

    /**
     * Returns the device manager, or null if not logged in yet
     */
    public static DeviceManager deviceManager() {
        return getInstance()._deviceManager;
    }


/**************************************************************************************************/


    /**
     * Constants
     */
    private final static String LOG_TAG = "SessionManager";

    // Ayla library setting keys
    public final static String AYLA_SETTING_CURRENT_USER = "currentUser";

    /** Private Members */

    /**
     * The session manager singleton object
     */
    private static SessionManager _globalSessionManager;

    /**
     * Session parameters used to configure the Session Manager
     */
    private SessionParameters _sessionParameters;

    /**
     * The AylaUser object returned on successful login
     */
    private AylaUser _aylaUser;

    /**
     * The device manager object. Null until the user logs in.
     */
    private DeviceManager _deviceManager;

    /**
     * Our listeners
     */
    private Set<SessionListener> _sessionListeners;

    /**
     * Private constructor. Call startSession instead of creating a SessionManager directly.
     */
    private SessionManager() {
        _sessionListeners = new HashSet<>();
    }

    /**
     * Account settings object
     */
    private AccountSettings _accountSettings;

    /**
     * Contact manager object
     */
    private ContactManager _contactManager;

    /** Private Methods */

    /**
     * Initializes the Ayla library, logs in and begins the session.
     */
    private boolean start() {
        // First check the session parameters
        if (!checkParameters()) {
            return false;
        }

        // Set up library logging
        AylaSystemUtils.loggingLevel = _sessionParameters.loggingLevel;
        AylaSystemUtils.serviceType = _sessionParameters.serviceType;
        if (_sessionParameters.loggingLevel != AylaNetworks.AML_LOGGING_LEVEL_NONE) {
            AylaSystemUtils.loggingEnabled = AylaNetworks.YES;
            AylaSystemUtils.loggingInit();
            AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", LOG_TAG, "version",
                    _sessionParameters.appVersion, "Session Start");
        } else {
            AylaSystemUtils.loggingEnabled = AylaNetworks.NO;
        }

        // Set up push notifications
        PushNotification pushNotification = new PushNotification();
        pushNotification.init(_sessionParameters.pushNotificationSenderId,
                _sessionParameters.username,
                _sessionParameters.appId);
        Log.d(LOG_TAG, "Push notification registration ID: " + PushNotification.registrationId);

        // Log in
        logIn();

        return true;
    }

    private boolean stop() {
        if (_deviceManager != null) {
            _deviceManager.shutDown();
        }
        Map<String, String> params = new HashMap<>();
        params.put("access_token", AylaUser.user.getauthHeaderValue());
        AylaUser.logout(params).execute();
        AylaUser.user.setAccessToken(null);
        AylaCache.clearAll();
        notifyLoginStateChanged(false, null);
        _deviceManager = null;
        return true;
    }

    /**
     * Handle the result of the login request
     */
    static class LoginHandler extends Handler {
        private WeakReference<SessionManager> _sessionManager;

        public LoginHandler(SessionManager sessionManager) {
            _sessionManager = new WeakReference<SessionManager>(sessionManager);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "Login Handler");
            Log.d(LOG_TAG, "Message: " + msg);
            MainActivity.getInstance().dismissWaitDialog();
            if (msg.what == AylaNetworks.AML_ERROR_OK) {
                Log.d(LOG_TAG, "Login successful");

                // Save the auth info for the user
                _sessionManager.get()._aylaUser = AylaSystemUtils.gson.fromJson((String) msg.obj, AylaUser.class);
                _sessionManager.get()._aylaUser.email = _sessionManager.get()._sessionParameters.username;
                _sessionManager.get()._aylaUser.password = _sessionManager.get()._sessionParameters.password;
                _sessionManager.get()._aylaUser = AylaUser.setCurrent(_sessionManager.get()._aylaUser);
                String userJson = AylaSystemUtils.gson.toJson(_sessionManager.get()._aylaUser, AylaUser.class);
                AylaSystemUtils.saveSetting(AYLA_SETTING_CURRENT_USER, userJson);

                // Store the access / refresh token in our session parameters
                _sessionManager.get()._sessionParameters.refreshToken = _sessionManager.get()._aylaUser.getRefreshToken();
                _sessionManager.get()._sessionParameters.accessToken = _sessionManager.get()._aylaUser.getAccessToken();

                _sessionManager.get()._aylaUser.password = _sessionManager.get()._sessionParameters.password;
                AylaCache.clearAll();
                SharedPreferences settings =
                        PreferenceManager.getDefaultSharedPreferences(_sessionManager.get()._sessionParameters.context);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(PREFS_PASSWORD, _sessionManager.get()._sessionParameters.password);
                editor.putString(PREFS_USERNAME, _sessionManager.get()._sessionParameters.username);
                editor.apply();

                // Fetches the account settings and stores them
                _sessionManager.get().fetchAccountSettings();

                // Create the contacts manager. It will fetch the contact list once the account
                // settings have been fetched (so it knows who the owner contact is)
                _sessionManager.get()._contactManager = new ContactManager();

                // Create the device manager and have it start polling the device list
                _sessionManager.get()._deviceManager = new DeviceManager();
                _sessionManager.get()._deviceManager.startPolling();

                AylaUser.setCurrent(_sessionManager.get()._aylaUser);
                _sessionManager.get().notifyLoginStateChanged(true, _sessionManager.get()._aylaUser);
            } else {
                String json = (String) msg.obj;
                String errorMessage = null;
                try {
                    JSONObject results = new JSONObject(json);
                    errorMessage = results.getString("error");
                } catch (JSONException e) {
                    e.printStackTrace();
                    errorMessage = json;
                }

                if (errorMessage != null) {
                    Toast.makeText(_sessionManager.get()._sessionParameters.context, errorMessage, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(_sessionManager.get()._sessionParameters.context, R.string.unknown_error, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private LoginHandler _loginHandler = new LoginHandler(this);

    private void logIn() {
        // Make sure the network is reachable
        AylaReachability.determineReachability(true);

        // Get the saved user and password
        AylaUser savedUser = null;
        String savedUsername = null;
        String savedUserJson = AylaSystemUtils.loadSavedSetting(AYLA_SETTING_CURRENT_USER, "");
        if (savedUserJson.length() > 0) {
            savedUser = AylaSystemUtils.gson.fromJson(savedUserJson, AylaUser.class);
            savedUsername = savedUser.email;
        }
        SharedPreferences settings =
                PreferenceManager.getDefaultSharedPreferences(_sessionParameters.context);
        String savedPassword = settings.getString(PREFS_PASSWORD, "");

        // If cached credentials match, use the refresh token
        if (savedUser != null && savedUser.email != null && savedUser.email.equals(_sessionParameters.username) &&
                savedPassword.equals(_sessionParameters.password)) {
            // We have a cached user and password that match. Try to refresh the access token.
            // First make sure it hasn't expired:
            int secondsToExpiry = savedUser.accessTokenSecondsToExpiry();
            if (secondsToExpiry < 21600) {    // Less than 6 hours
                // Normal login
                Log.d(LOG_TAG, "Normal login (access token expired)");
                AylaUser.login(_loginHandler,
                        _sessionParameters.username,
                        _sessionParameters.password,
                        _sessionParameters.appId,
                        _sessionParameters.appSecret);
            } else {
                Log.d(LOG_TAG, "Refreshing access token");
                AylaUser.refreshAccessToken(_loginHandler, savedUser.getRefreshToken());
            }
        } else {
            // Normal login
            Log.d(LOG_TAG, "Normal login");
            AylaUser.login(_loginHandler,
                    _sessionParameters.username,
                    _sessionParameters.password,
                    _sessionParameters.appId,
                    _sessionParameters.appSecret);
        }
    }

    private boolean checkParameters() {
        if (_sessionParameters.context == null) {
            return false;
        }
        if (_sessionParameters.deviceSsidRegex == null) {
            return false;
        }
        if (_sessionParameters.appId == null) {
            return false;
        }
        if (_sessionParameters.appSecret == null) {
            return false;
        }
        if (_sessionParameters.appVersion == null) {
            return false;
        }
        if (_sessionParameters.deviceCreator == null) {
            return false;
        }
        if (_sessionParameters.password == null && _sessionParameters.refreshToken == null) {
            return false;
        }
        if (_sessionParameters.username == null && _sessionParameters.refreshToken == null) {
            return false;
        }

        return true;
    }

    private void fetchAccountSettings() {
        Log.d(LOG_TAG, "Fetching account settings...");
        AccountSettings.fetchAccountSettings(AylaUser.getCurrent(), new AccountSettings.AccountSettingsCallback() {
            @Override
            public void settingsUpdated(AccountSettings settings, Message msg) {
                Log.d(LOG_TAG, "Account settings fetch result: " + msg);
                if ( settings != null ) {
                    _accountSettings = settings;
                    // Now we can create our contact manager
                    _contactManager.fetchContacts(new ContactManager.ContactManagerListener(), false);
                }
            }
        });
    }

    /**
     * Notifiers
     */
    private void notifyLoginStateChanged(boolean loggedIn, AylaUser aylaUser) {
        synchronized (_sessionListeners) {
            for (SessionListener l : _sessionListeners) {
                l.loginStateChanged(loggedIn, aylaUser);
            }
        }
    }
}
