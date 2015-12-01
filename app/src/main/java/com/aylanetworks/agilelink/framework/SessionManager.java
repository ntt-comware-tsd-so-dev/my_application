package com.aylanetworks.agilelink.framework;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaCache;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.AgileLinkApplication;
import com.aylanetworks.agilelink.AgilelinkSSOManager;
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
 * <p>
 * The SessionManager is used as a singleton object. Users of this class should use the
 * {@link #getInstance()} static method to obtain the single instance of the SessionManager.
 * </p>
 * <p>
 * The manager is configured using the {@link com.aylanetworks.agilelink.framework.SessionManager.SessionParameters}
 * class. Implementers should create an instance of SessionParameters and pass this to the SessionManager
 * via {@link #setParameters(com.aylanetworks.agilelink.framework.SessionManager.SessionParameters)}.
 * Once the parameters have been set, the user can be logged in via a call to
 * {@link #startSession(String, String)} to log in normally, or can log in via OAuth using
 * {@link #startOAuthSession(android.os.Message)}.
 * </p>
 * <p>
 * Once the session has been started, the SessionManager creates the following objects:
 * <ul>
 * <li>{@link com.aylanetworks.agilelink.framework.AccountSettings} to store account-specific information</li>
 * <li>{@link com.aylanetworks.agilelink.framework.DeviceManager} for fetching and polling the device list</li>
 * <li>{@link com.aylanetworks.agilelink.framework.ContactManager} for adding, removing, updating contacts</li>
 * <li>{@link com.aylanetworks.agilelink.framework.GroupManager} for managing groups of devices</li>
 * </ul>
 *
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
     * This is a debug-only method used to switch service types. Production applications should
     * never call this method. This method currently is set up to use AgileLink app IDs and
     * secrets, which most likely will not work with a new application.
     *
     * @param serviceType Service type to set for login. Will use for the next login session.
     */
    public void setServiceType(int serviceType) {
        AylaSystemUtils.serviceType = serviceType;

        switch ( serviceType ) {
            case AylaNetworks.AML_PRODUCTION_SERVICE:
                _sessionParameters.appId = "AgileLinkProd-id";
                _sessionParameters.appSecret = "AgileLinkProd-8249425";
                Log.i(LOG_TAG, "Service set to DEVELOPMENT");
                break;

            case AylaNetworks.AML_STAGING_SERVICE:
                _sessionParameters.appId = "AgileLinkProd-id";
                _sessionParameters.appSecret = "AgileLinkProd-1530606";
                Log.i(LOG_TAG, "Service set to STAGING");
                break;

            default:
                Log.i(LOG_TAG, "Service type set to " + serviceType);
        }
    }

    /**
     * Interface for notifications of changes in login state (log in / out), reachability or LAN
     * mode. Generally only the main activity implements this interface and registers for notifications
     * via {@link #addSessionListener(SessionManager.SessionListener)}
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

        if(parameters.logsEmailAddress != null){
            AylaSystemUtils.setSupportEmailAddress(parameters.logsEmailAddress);
        }
    }

    /**
     * Clears out the saved user. Call this when the user wishes to log out without cached credentials.
     */
    public static void clearSavedUser() {
        SharedPreferences settings = AgileLinkApplication.getSharedPreferences();
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
     * Adds a listener who will be notified when the state of the session changes (log out,
     * log in, reachabilty, LAN mode changes)
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

    /**
     * Returns true if LAN mode is permitted during this session. This returns the value set in
     * {@link com.aylanetworks.agilelink.framework.SessionManager.SessionParameters#enableLANMode}.
     * @return true if LAN mode is permitted, false otherwise
     */
    public boolean lanModePermitted() {
        return (_foreground && _sessionParameters.enableLANMode);
    }

    // should probably be part of SessionParameters
    boolean _foreground;

    /**
     * Set by the MainActivity to indicate whether the app is in the foreground (true) or
     * background (false)
     * @param foreground
     */
    public void setForeground(boolean foreground) {
        _foreground = foreground;
    }

    /**
     * Set by the Add Device UI to shutdown all extra network activity while trying to register
     * new devices.
     * @param registration
     */
    public void setRegistrationMode(boolean registration) {
        if (registration) {
            Logger.logDebug(LOG_TAG, "rn: setRegistrationMode true");
            if (deviceManager() != null) {
                deviceManager().setRegistrationMode(registration);
            }
            // TODO: quick hack... remove this
            // TODO: NetworkOnMainThread is caused when we don't have a cached property and have to
            // TODO: immediately go to the network to get the current value.
            StrictMode.ThreadPolicy tp = StrictMode.ThreadPolicy.LAX;
            StrictMode.setThreadPolicy(tp);
        } else {
            if (deviceManager() != null) {
                deviceManager().setRegistrationMode(registration);
            }
            Logger.logDebug(LOG_TAG, "rn: setRegistrationMode false");
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
         * <p>
         * This string is used to filter the list of SSIDs returned from a WiFi network scan.
         * The default value returns SSIDs that begin with "Ayla", "T-Stat" or "Plug", followed
         * by a dash and a string of 12 hexadecimal characters.
         * </p>
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
         * Set to true to allow LAN mode operations application-wide. If this is set to true, the
         * app will attempt to connect via the LAN to devices under certain circumstances. If set
         * to false, LAN mode will not be attempted.
         */
        public boolean enableLANMode = false;

        /**
         * Set to true to allow the user to log in using cached credentials when the cloud
         * service is not available, but the local network with devices on it is.
         */
        public boolean allowLANLogin = false;

    /*    *//**
         * {@link com.aylanetworks.agilelink.framework.SSOManager} object.
         * Implementers should create a class derived from SSOManager
         *
         */
        public SSOManager ssoManager = new AgilelinkSSOManager();

        /**
         * Set to true to allow login to SSO Identity Provider service.
         * false by default
         */
        public boolean ssoLogin = false;
        /**
         * Logging level for the Ayla library. Set to one of:
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
         * Email address to which logs are to be sent by the user.
         * Default email address available from AylaSystemUtils.getSupportEmailAddress()
         */
        public String logsEmailAddress = null;

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
         * @param other Session parameters to copy
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
            this.loggingLevel = other.loggingLevel;
            this.enableLANMode = other.enableLANMode;
            this.allowLANLogin = other.allowLANLogin;
            this.registrationEmailSubject = other.registrationEmailSubject;
            this.registrationEmailTemplateId = other.registrationEmailTemplateId;
            this.registrationEmailBodyHTML = other.registrationEmailBodyHTML;
            this.resetPasswordEmailSubject = other.resetPasswordEmailSubject;
            this.resetPasswordEmailBodyHTML = other.resetPasswordEmailBodyHTML;
            this.resetPasswordEmailTemplateId = other.resetPasswordEmailTemplateId;
            this.notificationEmailSubject = other.notificationEmailSubject;
            this.notificationEmailTemplateId = other.notificationEmailTemplateId;
            this.notificationEmailBodyHTML = other.notificationEmailBodyHTML;
            this.logsEmailAddress = other.logsEmailAddress;
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
                    "  loggingLevel: " + loggingLevel + "\n" +
                    "  enableLANMode: " + enableLANMode +
                    "  allowLANLogin: " + allowLANLogin +
                    "  ssoLogin: " + ssoLogin +
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
     * <p>
     * This method logs the user in, and if successful creates a {@link com.aylanetworks.agilelink.framework.DeviceManager},
     * fetches the current {@link com.aylanetworks.agilelink.framework.AccountSettings} from the
     * server and notifies any {@link com.aylanetworks.agilelink.framework.SessionManager.SessionListener}
     * listeners that have registered that the session has been started.
     * </p><p>
     * At this point the DeviceManager will fetch the list of devices from the server and begin
     * polling them for updates.
     * </p>
     * If the user wishes to log out, the application should call {@link #stopSession()} to shut
     * down the library and clean up.
     *
     * @param username Username for the session
     * @param password Password for the session
     * @return true if the session was started, false if an error occurred
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
    public static void stopSession() {
        getInstance().stop();
    }

    /**
     * Returns a copy of the current session parameters
     * @return a copy of the current session parameters
     */
    public static SessionParameters sessionParameters() {
        if (getInstance()._sessionParameters == null) {
            return null;
        }
        // Return a copy. We don't want things changing underneath us.
        return new SessionParameters(getInstance()._sessionParameters);
    }

    /**
     * Returns a human-readable name for the service type the app is connected to
     * @return The service type the app is connected to
     */
    public static String getServiceTypeString() {
        Context c = MainActivity.getInstance();

        switch ( AylaSystemUtils.serviceType ) {
            case AylaNetworks.AML_DEVICE_SERVICE:
                return c.getString(R.string.device_service);

            case AylaNetworks.AML_FIELD_SERVICE:
                return c.getString(R.string.field_service);

            case AylaNetworks.AML_PRODUCTION_SERVICE:
                return c.getString(R.string.production_service);

            case AylaNetworks.AML_STAGING_SERVICE:
                return c.getString(R.string.staging_service);

            case AylaNetworks.AML_DEMO_SERVICE:
                return c.getString(R.string.demo_service);

            default:
                return c.getString(R.string.unknown_service, AylaSystemUtils.serviceType);
        }
    }

    /**
     * Returns the current device manager, or null if it does not exist.
     * @return the device manager, or null if not logged in yet
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
        Log.d(LOG_TAG, "nod: stop session");

        if (_deviceManager != null) {
            _deviceManager.shutDown();
            _deviceManager = null;
        }

        // Clear the saved password
        SharedPreferences settings = AgileLinkApplication.getSharedPreferences();
        settings.edit().putString(PREFS_PASSWORD, "").apply();

        Map<String, String> params = new HashMap<>();
        params.put("access_token", AylaUser.user.getauthHeaderValue());
        Log.d(LOG_TAG, "nod: stop logout");
        Resources resources = MainActivity.getInstance().getResources();
        MainActivity.getInstance().showWaitDialog(resources.getString(R.string.sign_out), resources.getString(R.string.signing_out));
        AylaUser.logout(logouthandler, params).execute();

        return true;
    }

    private final Handler logouthandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "nod: stop session handle message [" + msg + "]");
            //super.handleMessage(msg);
            dismissWaitDialog();
            AylaUser.user.setAccessToken(null);
            AylaCache.clearAll();
            notifyLoginStateChanged(false, null);
        }
    };

    static void dismissWaitDialog() {
        MainActivity activity = MainActivity.getInstance();
        if (activity != null) {
            activity.dismissWaitDialog();
        }
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
            Log.d(LOG_TAG, "Message: " + msg.toString());
            dismissWaitDialog();
            if (AylaNetworks.succeeded(msg)) {
                Log.d(LOG_TAG, "Login successful");

                if ( msg.arg1 == AylaNetworks.AML_ERROR_ASYNC_OK_CACHED ) {
                    // This is a LAN login. If we don't allow it, then fail. Otherwise we'll continue.
                    if (_sessionManager.get()._sessionParameters.allowLANLogin) {
                        Log.d(LOG_TAG, "LAN login!");
                        Toast.makeText(MainActivity.getInstance(), R.string.lan_login_message, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.getInstance(), R.string.network_not_reachable, Toast.LENGTH_LONG).show();
                        return;
                    }
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

                SharedPreferences settings = AgileLinkApplication.getSharedPreferences();
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(PREFS_PASSWORD, _sessionManager.get()._sessionParameters.password);
                editor.putString(PREFS_USERNAME, _sessionManager.get()._sessionParameters.username);
                editor.apply();

                // Fetches the account settings and stores them
                _sessionManager.get().fetchAccountSettings(null);

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
                    Log.e(LOG_TAG, e.getLocalizedMessage() + " on [" + json + "]");
                    e.printStackTrace();
                    errorMessage = json;
                }

                if (errorMessage != null) {
                    Toast.makeText(_sessionManager.get()._sessionParameters.context, errorMessage, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(_sessionManager.get()._sessionParameters.context, R.string.unknown_error, Toast.LENGTH_LONG).show();
                }
                _sessionManager.get().stop();
            }
        }
    }

    private LoginHandler _loginHandler = new LoginHandler(this);

    /**
     * Handle the result of the login request
     */
    static class SsoLoginHandler extends Handler {
        private WeakReference<SessionManager> _sessionManager;

        public SsoLoginHandler(SessionManager sessionManager) {
            _sessionManager = new WeakReference<SessionManager>(sessionManager);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "Login Handler");
            Log.d(LOG_TAG, "Message: " + msg.toString());
            dismissWaitDialog();
            if (AylaNetworks.succeeded(msg)) {
                Log.d(LOG_TAG, "Login successful");

                if ( msg.arg1 == AylaNetworks.AML_ERROR_ASYNC_OK_CACHED ) {
                    // This is a LAN login. If we don't allow it, then fail. Otherwise we'll continue.
                    if (_sessionManager.get()._sessionParameters.allowLANLogin) {
                        Log.d(LOG_TAG, "LAN login!");
                        Toast.makeText(MainActivity.getInstance(), R.string.lan_login_message, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.getInstance(), R.string.network_not_reachable, Toast.LENGTH_LONG).show();
                        return;
                    }
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

                SharedPreferences settings = AgileLinkApplication.getSharedPreferences();
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(PREFS_PASSWORD, _sessionManager.get()._sessionParameters.password);
                editor.putString(PREFS_USERNAME, _sessionManager.get()._sessionParameters.username);
                editor.apply();

                // Fetches the account settings and stores them
                _sessionManager.get().fetchAccountSettings(null);

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
                    errorMessage = results.getString("errorMessage");
                } catch (JSONException e) {
                    Log.e(LOG_TAG, e.getLocalizedMessage() + " on [" + json + "]");
                    e.printStackTrace();
                    errorMessage = json;
                }

                if (errorMessage != null) {
                    Toast.makeText(_sessionManager.get()._sessionParameters.context, errorMessage, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(_sessionManager.get()._sessionParameters.context, R.string.unknown_error, Toast.LENGTH_LONG).show();
                }
                _sessionManager.get().stop();
            }
        }
    }

    private SsoLoginHandler _ssoLoginHandler = new SsoLoginHandler(this);

    private void logIn() {

        Log.d(LOG_TAG, "User Login");

        if(!_sessionParameters.ssoLogin)
        {
            AylaUser.login(_loginHandler,
                    _sessionParameters.username,
                    _sessionParameters.password,
                    _sessionParameters.appId,
                    _sessionParameters.appSecret);
        }else{

            _sessionParameters.ssoManager.login(_ssoLoginHandler, _sessionParameters.username, _sessionParameters.password);
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
        if (_sessionParameters.ssoManager == null) {
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

    public void fetchAccountSettings(final AccountSettings.AccountSettingsCallback callback) {
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
                if ( callback != null ) {
                    callback.settingsUpdated(settings, msg);
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
