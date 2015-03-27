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
 * <p/>
 * The SessionManager is used as a singleton object. Users of this class should use the
 * {@link #getInstance()} static method to obtain the single instance of the SessionManager.
 * <p/>
 * <p/>
 * The manager is configured using the {@link com.aylanetworks.agilelink.framework.SessionManager.SessionParameters}
 * class. Implementers should create an instance of SessionParameters and pass this to the SessionManager
 * via {@link #setParameters(com.aylanetworks.agilelink.framework.SessionManager.SessionParameters)}.
 * Once the parameters have been set, the user can be logged in via a call to
 * {@link #startSession(String, String)} to log in normally, or can log in via OAuth using
 * {@link #startOAuthSession(android.os.Message)}.
 * <p/>
 * <p/>
 * Once the session has been started, the SessionManager creates the following objects:
 * <ul>
 * <li>{@link com.aylanetworks.agilelink.framework.AccountSettings} to store account-specific information</li>
 * <li>{@link com.aylanetworks.agilelink.framework.DeviceManager} for fetching and polling the device list</li>
 * <li>{@link com.aylanetworks.agilelink.framework.ContactManager} for adding, removing, updating contacts</li>
 * <li>{@link com.aylanetworks.agilelink.framework.GroupManager} for managing groups of devices</li>
 * </ul>
 * These objects are used throughout the application.
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

    /**
     * Sets the parameters for the session. This method should be called before any other
     * SessionManager methods.
     *
     * @param parameters Parameters for this session
     */
    public static void setParameters(SessionParameters parameters) {
        getInstance()._sessionParameters = new SessionParameters(parameters);
        // Initialize the Ayla library
        AylaNetworks.init(getInstance()._sessionParameters.context,
                getInstance()._sessionParameters.deviceSsidRegex,
                getInstance()._sessionParameters.appId);

    }

    /**
     * Clears out the saved user. Call this when the user wishes to log out without cached credentials.
     */
    public static void clearSavedUser() {
        SharedPreferences settings =
                PreferenceManager.getDefaultSharedPreferences(getInstance()._sessionParameters.context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREFS_PASSWORD, "");
        editor.putString(PREFS_USERNAME, "");
        editor.apply();
        AylaSystemUtils.saveSetting(AYLA_SETTING_CURRENT_USER, "");
        AylaUser.setCurrent(new AylaUser());
        AylaUser.setCachedUser(AylaUser.getCurrent());
    }

    /**
     * Returns the singleton instance of the SessionManager
     *
     * @return the SessionManager object
     */
    public static SessionManager getInstance() {
        if (_globalSessionManager == null) {
            _globalSessionManager = new SessionManager();
        }
        return _globalSessionManager;
    }

    /**
     * Returns the current AccountSettings object for the logged-in user
     *
     * @return the AccountSettings for this account
     */
    public AccountSettings getAccountSettings() {
        return _accountSettings;
    }

    /**
     * Returns the contact manager for this account.
     *
     * @return the ContactManager for this account.
     */
    public ContactManager getContactManager() {
        return _contactManager;
    }

    /**
     * Registers the given AylaUser with the AylaService. This creates an account for the user
     * and sends the user an email with confirmation instructions.
     *
     * @param user          The AylaUser to register
     * @param resultHandler Handler to receive the results of the call to
     *                      {@link AylaUser#signUp(android.os.Handler, java.util.Map, String, String)}
     */
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

    /**
     * Adds a listener who will be notified when the state of the session changes (log out, log in, etc.)
     *
     * @param listener Listener to be notified of session state changes
     */
    public static void addSessionListener(SessionListener listener) {
        synchronized (getInstance()._sessionListeners) {
            getInstance()._sessionListeners.add(listener);
        }
    }

    /**
     * Removes a session listener from the list of listeners to be notified of session state changes
     *
     * @param listener Listener to remove
     */
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
        /**
         * Context used for UI / resources / etc. Android-specific.
         */
        public Context context = null;

        /**
         * Regular expression string for SSID scans.
         * <p/>
         * This string is used to filter the list of SSIDs returned from a WiFi network scan.
         * The default value returns SSIDs that begin with "Ayla", "T-Stat" or "Plug", followed
         * by a dash and a string of 12 hexadecimal characters.
         * <p/>
         * If your device broadcasts a different SSID for WiFi setup, set this parameter to a
         * regular expression that matches any supported devices' SSID.
         */
        public String deviceSsidRegex = "^Ayla-[0-9A-Fa-f]{12}|^T-Stat-[0-9A-Fa-f]{12}|^Plug-[0-9A-Fa-f]{12}";

        /**
         * Application version. Passed to the Ayla library during initialization.
         */
        public String appVersion = "0.1";

        /**
         * Sender ID for push notifications. Android-specific. @see {@link com.aylanetworks.agilelink.framework.PushNotification}
         * for details.
         */
        public String pushNotificationSenderId = "103052998040";

        /**
         * App ID passed to the Ayla library. Each app has its own unique ID.
         */
        public String appId = "aMCA-id";

        /**
         * App secret passed to the Ayla library. Unique to the app ID.
         */
        public String appSecret = "aMCA-9097620";

        /**
         * {@link com.aylanetworks.agilelink.framework.DeviceCreator} object.
         * Implementers should create a class derived from DeviceCreator
         * to return Device objects for this application.
         */
        public DeviceCreator deviceCreator = new DeviceCreator();

        /**
         * Username for login
         */
        public String username;

        /**
         * Password for login
         */
        public String password;

        /**
         * Access token used to log in without username or password
         */
        public String accessToken;

        /**
         * Refresh token used to continue a previous session
         */
        public String refreshToken;

        /**
         * Set to true to allow LAN mode operations application-wide
         */
        public boolean enableLANMode = false;

        /**
         * Service type the Ayla library should connect to. Set to one of:
         * <ul>
         * <li>{@link com.aylanetworks.aaml.AylaNetworks#AML_DEVICE_SERVICE}</li>
         * <li>{@link com.aylanetworks.aaml.AylaNetworks#AML_FIELD_SERVICE}</li>
         * <li>{@link com.aylanetworks.aaml.AylaNetworks#AML_DEVELOPMENT_SERVICE}</li>
         * <li>{@link com.aylanetworks.aaml.AylaNetworks#AML_STAGING_SERVICE}</li>
         * <li>{@link com.aylanetworks.aaml.AylaNetworks#AML_DEMO_SERVICE}</li>
         * </ul>
         */
        public int serviceType = AylaNetworks.AML_STAGING_SERVICE;

        /**
         * Logging level for the Ayla library. Set to one of:
         * <p/>
         * <ul>
         * <li>{@link AylaNetworks#AML_LOGGING_LEVEL_NONE}</li>
         * <li>{@link AylaNetworks#AML_LOGGING_LEVEL_ERROR}</li>
         * <li>{@link AylaNetworks#AML_LOGGING_LEVEL_WARNING}</li>
         * <li>{@link AylaNetworks#AML_LOGGING_LEVEL_INFO}</li>
         * <li>{@link AylaNetworks#AML_LOGGING_LEVEL_ALL}</li>
         * </ul>
         */
        public int loggingLevel = AylaNetworks.AML_LOGGING_LEVEL_ERROR;

        // Registration email parameters

        /**
         * Template ID for the initial registration email sent to the user upon account creation.
         * If this is null, the {@link #registrationEmailBodyHTML} should contain an HTML
         * string used for this email message.
         */
        public String registrationEmailTemplateId = "ayla_confirmation_template_01";

        /**
         * Subject for the email sent to the user upon initial account creation.
         */
        public String registrationEmailSubject = "Ayla Sign-up Confirmation";

        /**
         * HTML body for the email sent to the user upon initial account creation. This is used
         * if the template ID is null.
         */
        public String registrationEmailBodyHTML = null;

        // Reset password email parameters

        /**
         * Subject for the email sent to the user upon requesting a password reset.
         */
        public String resetPasswordEmailSubject = "Ayla Reset Password Confirmation";

        /**
         * Template ID for the email sent to the user upon requesting a password reset.
         * If this is null, the {@link #resetPasswordEmailBodyHTML} should contain an HTML
         * string used for this email message.
         */
        public String resetPasswordEmailTemplateId = "ayla_passwd_reset_template_01";

        /**
         * HTML body for the email sent to the user upon requesting a password reset. This is used
         * if the template ID is null.
         */
        public String resetPasswordEmailBodyHTML = null;

        // Device notification email parameters


        /**
         * Subject for the email sent to the user when a device goes online or offline.
         */
        public String notificationEmailSubject = "Ayla Device Notification";


        /**
         * Template ID for the email sent to the user when a device goes online or offline.
         * If this is null, the {@link #notificationEmailBodyHTML} should contain an HTML
         * string used for this email message.
         */
        public String notificationEmailTemplateId = "ayla_notify_template_01";


        /**
         * HTML body for the email sent to the user when a device goes online or offline. This is used
         * if the template ID is null.
         */
        public String notificationEmailBodyHTML = null;

        /**
         * SessionParameters constructor
         *
         * @param context Context for resources
         */
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
     * <p/>
     * This method logs the user in, and if successful creates a {@link com.aylanetworks.agilelink.framework.DeviceManager},
     * fetches the current {@link com.aylanetworks.agilelink.framework.AccountSettings} from the
     * server and notifies any {@link com.aylanetworks.agilelink.framework.SessionManager.SessionListener}
     * listeners that have registered that the session has been started.
     * <p/>
     * At this point the DeviceManager will fetch the list of devices from the server and begin
     * polling them for updates.
     * <p/>
     * If the user wishes to log out, the application should call {@link #stopSession()} to shut
     * down the library and clean up.
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

    /**
     * Starts a session using the response Message from a call to {@link AylaUser#loginThroughOAuth}
     *
     * @param oAuthResponseMessage The Message returned from a successful call to {@link AylaUser#loginThroughOAuth}
     */
    public static void startOAuthSession(Message oAuthResponseMessage) {
        getInstance()._loginHandler.handleMessage(oAuthResponseMessage);
    }

    /**
     * Closes network connections and logs out the user.
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

        // Set the timeout for network searches for the service
        AylaSystemUtils.serviceReachableTimeout = AylaNetworks.AML_SERVICE_REACHABLE_TIMEOUT;

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
            if (AylaNetworks.succeeded(msg)) {
                Log.d(LOG_TAG, "Login successful");

                if ( msg.what == AylaNetworks.AML_ERROR_ASYNC_OK_CACHED ) {
                    // This is a LAN login
                    Log.d(LOG_TAG, "LAN login!");
                    Toast.makeText(MainActivity.getInstance(), R.string.lan_login_message, Toast.LENGTH_LONG).show();
                }

                // Save the auth info for the user
                _sessionManager.get()._aylaUser = AylaSystemUtils.gson.fromJson((String) msg.obj, AylaUser.class);
                _sessionManager.get()._aylaUser.email = _sessionManager.get()._sessionParameters.username;
                _sessionManager.get()._aylaUser.password = _sessionManager.get()._sessionParameters.password;
                _sessionManager.get()._aylaUser = AylaUser.setCurrent(_sessionManager.get()._aylaUser);

                // Store the access / refresh token in our session parameters
                _sessionManager.get()._sessionParameters.refreshToken = _sessionManager.get()._aylaUser.getRefreshToken();
                _sessionManager.get()._sessionParameters.accessToken = _sessionManager.get()._aylaUser.getAccessToken();

                _sessionManager.get()._aylaUser.password = _sessionManager.get()._sessionParameters.password;

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

                // Cache this user so we can log in with the same credentials again
                AylaUser.setCachedUser(_sessionManager.get()._aylaUser);
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

        Log.d(LOG_TAG, "User Login");
        AylaUser.login(_loginHandler,
                _sessionParameters.username,
                _sessionParameters.password,
                _sessionParameters.appId,
                _sessionParameters.appSecret);

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
                if (settings != null) {
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
