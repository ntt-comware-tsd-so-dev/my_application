package com.aylanetworks.agilelink;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.internal.view.menu.MenuBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaLanMode;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaReachability;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.device.devkit.AgileLinkDeviceCreator;
import com.aylanetworks.agilelink.fragments.AddDeviceFragment;
import com.aylanetworks.agilelink.fragments.AllDevicesFragment;
import com.aylanetworks.agilelink.fragments.ContactListFragment;
import com.aylanetworks.agilelink.fragments.DeviceGroupsFragment;
import com.aylanetworks.agilelink.fragments.EditProfileDialog;
import com.aylanetworks.agilelink.fragments.NotificationsFragment;
import com.aylanetworks.agilelink.fragments.ResetPasswordDialog;
import com.aylanetworks.agilelink.fragments.SettingsFragment;
import com.aylanetworks.agilelink.fragments.SignInDialog;
import com.aylanetworks.agilelink.fragments.SignUpDialog;
import com.aylanetworks.agilelink.fragments.WiFiSetupFragment;
import com.aylanetworks.agilelink.framework.MenuHandler;
import com.aylanetworks.agilelink.framework.SessionManager;
import com.aylanetworks.agilelink.framework.UIConfig;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * MainActivity.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 12/17/14
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class MainActivity extends ActionBarActivity implements SignUpDialog.SignUpListener, SignInDialog.SignInDialogListener, SessionManager.SessionListener {

    private static final String LOG_TAG = "Main Activity";
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    private static MainActivity _theInstance;

    /**
     * Returns the one and only instance of this activity
     */
    public static MainActivity getInstance() {
        return _theInstance;
    }

    private static UIConfig _uiConfig;
    public static UIConfig getUIConfig() {
        return _uiConfig;
    }

    /**
     * Static class initializer.
     *
     * Here we configure the global UIConfig instance. This object is used throughout the application
     * to customize the look and feel of the application. Implementers should modify the UIConfig
     * instance to customize the app appearance to taste.
     */
    static {
        _uiConfig = new UIConfig();
        _uiConfig._listStyle = UIConfig.ListStyle.Grid;
        _uiConfig._navStyle = UIConfig.NavStyle.Drawer;
    }

    ProgressDialog _progressDialog;

    /**
     * Shows a system-modal dialog with a spinning progress bar, the specified title and message.
     * The caller should call dismissWaitDialog() when finished.
     *
     * @param title   title of the dialog
     * @param message message of the dialog
     */
    public void showWaitDialog(String title, String message) {
        if (_progressDialog != null) {
            dismissWaitDialog();
        }

        if (title == null) {
            title = getResources().getString(R.string.please_wait);
        }

        _progressDialog = ProgressDialog.show(this, title, message, true);
    }

    /**
     * Shows a system-modal dialog with a spinning progress bar, the specified title and message.
     * The caller should call dismissWaitDialog() when finished.
     *
     * @param titleId   String ID for the title of the dialog
     * @param messageId String ID for the message of the dialog
     */
    public void showWaitDialog(int titleId, int messageId) {
        String title = getResources().getString(titleId);
        String message = getResources().getString(messageId);
        showWaitDialog(title, message);
    }

    /**
     * Dismisses the wait dialog shown with showWaitDialog()
     */
    public void dismissWaitDialog() {
        if (_progressDialog != null) {
            _progressDialog.dismiss();
            _progressDialog = null;
        }
    }

    /**
     * Returns a string containing the app version defined in the package
     *
     * @return The app version string
     */
    public String getAppVersion() {
        PackageInfo info = null;
        try {
            info = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return getString(R.string.unknown_app_version);
        }

        return info.versionName + "." + info.versionCode;
    }

    public void setCloudConnectivityIndicator(boolean cloudConnectivity) {
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        RelativeLayout layout = (RelativeLayout)getLayoutInflater().inflate(cloudConnectivity ?
                R.layout.cloud_actionbar : R.layout.wifi_actionbar, null);
        ImageView icon = (ImageView)layout.findViewById(R.id.icon);
        icon.setColorFilter(getResources().getColor(R.color.app_theme_accent), PorterDuff.Mode.SRC_ATOP);

        getSupportActionBar().setCustomView(layout);
    }

    /**
     * Listener interface for the pickContact method
     */
    public interface PickContactListener {
        /**
         * When the contact picker activity has finished, this method will be called with a
         * Cursor object that can be examined to obtain the contact information needed to
         * populate an AylaContact object.
         *
         * @param cursor Cursor into the selected contact query results, or null if the
         *               operation was canceled.
         */
        void contactPicked(Cursor cursor);
    }

    private PickContactListener _pickContactListener;
    private static final int REQ_PICK_CONTACT = 1;

    /**
     * Launches the system's contact picker activity and calls the PickContactListener with
     * a cursor containing query results for the selected contact, or null if the user canceled
     * the operation.
     *
     * @param listener listener to be notified with the selected contact cursor, or null if canceled
     */
    public void pickContact(PickContactListener listener) {
        _pickContactListener = listener;
        Intent intent = new Intent(Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, REQ_PICK_CONTACT);
    }

    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        final int rc = resultCode;
        final Intent finalData = data;

        if (reqCode == REQ_PICK_CONTACT && _pickContactListener != null) {
            // Run on the UI thread
            mViewPager.post(new Runnable() {
                @Override
                public void run() {
                    if (rc == RESULT_OK) {
                        // Query for all the contact info we care about
                        Uri contactData = finalData.getData();
                        Uri dataUri = Uri
                                .withAppendedPath(
                                        contactData,
                                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY);
                        final String PROJECTION[] = {ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                                ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
                                ContactsContract.CommonDataKinds.Phone.NUMBER,
                                ContactsContract.CommonDataKinds.Phone.TYPE,
                                ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
                                ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE,
                                ContactsContract.CommonDataKinds.Email.DATA};

                        final String SELECTION = ContactsContract.Data.MIMETYPE + "=? OR "
                                + ContactsContract.Data.MIMETYPE + "=? OR "
                                + ContactsContract.Data.MIMETYPE + "=? OR "
                                + ContactsContract.Data.MIMETYPE + "=?";

                        String SELECTION_ARGS[] = {
                                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                                ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE,
                                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE};

                        String SORT = ContactsContract.Data.MIMETYPE;
                        Cursor c = getContentResolver().query(dataUri,
                                PROJECTION, SELECTION, SELECTION_ARGS,
                                SORT);
                        _pickContactListener.contactPicked(c);

                    } else {
                        _pickContactListener.contactPicked(null);
                    }
                    _pickContactListener = null;
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        _theInstance = this;
        AylaNetworks.appContext = this;

        super.onCreate(savedInstanceState);

        switch ( getUIConfig()._navStyle ) {
            case Pager:
                initPager();
                break;

            case Drawer:
                initDrawer();
                break;
        }

        // Set up the session manager with our session parameters
        SessionManager.setParameters(getAppParameters());

        // We want to know when
        SessionManager.addSessionListener(this);

        // Bring up the login dialog if we're not already logged in
        if (!SessionManager.isLoggedIn()) {
            showLoginDialog();
        }
    }

    private void initPager() {
         setContentView(R.layout.activity_main_pager);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
    }

    private DrawerLayout _drawerLayout;
    private ListView _drawerList;
    private ActionBarDrawerToggle _drawerToggle;
    private List<MenuItem> _drawerMenuItems;

    private void initDrawer() {
        Log.d(LOG_TAG, "initDrawer");

        setContentView(R.layout.activity_main_drawer);
        // Load the drawer menu resource. We will be using the menu items in our listview.
        Menu menu = new MenuBuilder(this);
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.menu_drawer, menu);
        inflater.inflate(R.menu.menu_settings, menu);

        _drawerMenuItems = new ArrayList<MenuItem>();
        for ( int i = 0; i < menu.size(); i++ ) {
            MenuItem item = menu.getItem(i);
            Log.d(LOG_TAG, "Menu item " + i + ": " + item);
            _drawerMenuItems.add(item);
        }

        _drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        _drawerList = (ListView)findViewById(R.id.left_drawer);
        _drawerList.setAdapter(new ArrayAdapter<MenuItem>(this, R.layout.navigation_drawer_item, R.id.nav_textview, _drawerMenuItems));
        _drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onDrawerItemClicked(position);
            }
        });

        _drawerToggle = new ActionBarDrawerToggle(this,
                _drawerLayout, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                MainActivity.this.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                MainActivity.this.onDrawerClosed(drawerView);
            }
        };

        _drawerToggle.setHomeAsUpIndicator(R.drawable.ic_launcher);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_launcher);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        _drawerLayout.setDrawerListener(_drawerToggle);
        onDrawerItemClicked(0);
    }

    public void openDrawer() {
        if ( _drawerLayout != null ) {
            _drawerLayout.openDrawer(_drawerList);
        }
    }

    public void closeDrawer() {
        if ( _drawerLayout != null ) {
            _drawerLayout.closeDrawer(_drawerList);
        }
    }

    public boolean isDrawerOpen() {
        if ( _drawerLayout == null ) {
            return false;
        }

        return _drawerLayout.isDrawerOpen(_drawerList);
    }

    private void onDrawerOpened(View drawerView) {
        Log.d(LOG_TAG, "Drawer Opened");
        supportInvalidateOptionsMenu();
    }

    private void onDrawerClosed(View drawerView) {
        Log.d(LOG_TAG, "Drawer Closed");
        supportInvalidateOptionsMenu();
    }
    public void settingsMenuItemClicked(MenuItem item) {
        MenuHandler.handleMenuItem(item);
    }

    private void onDrawerItemClicked(int position) {
        MenuItem menuItem = _drawerMenuItems.get(position);
        popBackstackToRoot();
        if (MenuHandler.handleMenuItem(menuItem)) {
            closeDrawer();
        }
    }

    /**
     * Returns session parameters for the default Agile Link application. Implementers should
     * modify this method to return a SessionParameters object initialized with values specific
     * to the application being developed.
     *
     * @return the SessionParameters for this application
     */
    private SessionManager.SessionParameters getAppParameters() {
        final SessionManager.SessionParameters parameters = new SessionManager.SessionParameters(this);

        // Change this to false to connect to the production service
        boolean useDevService = true;
        if ( useDevService ) {
            // Development values
            parameters.appId = "AgileLinkDev-id";
            parameters.appSecret = "AgileLinkDev-4780291";
            parameters.serviceType = AylaNetworks.AML_DEVELOPMENT_SERVICE;
        } else {
            // Production values
            parameters.appId = "AgileLinkProd-id";
            parameters.appSecret = "AgileLinkProd-8249425";
            parameters.serviceType = AylaNetworks.AML_STAGING_SERVICE;
        }

        parameters.deviceCreator = new AgileLinkDeviceCreator();

        parameters.appVersion = getAppVersion();

        // Will attempt to put devices into LAN mode whenever possible
        parameters.enableLANMode = true;

        // Allows login when the service is not reachable, using cached data or connecting directly
        // with devices in LAN mode
        parameters.allowLANLogin = true;

        parameters.loggingLevel = AylaNetworks.AML_LOGGING_LEVEL_INFO;

        parameters.registrationEmailSubject = getResources().getString(R.string.registraion_email_subject);

        // For a custom HTML message, set REGISTRATION_EMAIL_TEMPLATE_ID to null and
        // REGISTRATION_EMAIL_BODY_HTML to an HTML string for the email message.
        parameters.registrationEmailTemplateId = "ayla_confirmation_template_01";

        if (parameters.registrationEmailTemplateId == null) {
            parameters.registrationEmailBodyHTML = getResources().getString(R.string.registration_email_body_html);
        } else {
            parameters.registrationEmailBodyHTML = null;
        }

        return parameters;
    }

    @Override
    public void onBackPressed() {
        // Open the nav drawer on back unless it's already open (or we don't have one)
        if ( _drawerToggle != null &&
                getSupportFragmentManager().getBackStackEntryCount() == 0 &&
                !isDrawerOpen() ) {
            openDrawer();
            return;
        }
        super.onBackPressed();
    }

    public void popBackstackToRoot() {
        // Pop to the root of the backstack
        if ( getSupportFragmentManager().getBackStackEntryCount() > 0 ) {
            FragmentManager.BackStackEntry firstEntry = getSupportFragmentManager().getBackStackEntryAt(0);
            getSupportFragmentManager().popBackStackImmediate(firstEntry.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    private void showLoginDialog() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        final String savedUsername = settings.getString(SessionManager.PREFS_USERNAME, "");
        final String savedPassword = settings.getString(SessionManager.PREFS_PASSWORD, "");

        // If we've got the username / password, we have everything we need.
        if ( !TextUtils.isEmpty(savedUsername) && !TextUtils.isEmpty(savedPassword)) {
            signIn(savedUsername, savedPassword);
            return;
        }

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (_loginDialog == null) {
                    _loginDialog = new SignInDialog();

                    // We always want to show the "All Devices" page first
                    if ( mViewPager != null ) {
                        mViewPager.setCurrentItem(0);
                    }
                    popBackstackToRoot();

                    Bundle args = new Bundle();
                    args.putString(SignInDialog.ARG_USERNAME, savedUsername);
                    args.putString(SignInDialog.ARG_PASSWORD, savedPassword);
                    _loginDialog.setArguments(args);
                    _loginDialog.show(getSupportFragmentManager(), "signin");
                } else {
                    Log.e(LOG_TAG, "Login dialog is already being shown!");
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // No call for super(). Bug on API Level > 11.
        // https://code.google.com/p/android/issues/detail?id=19917
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.d(LOG_TAG, "onPause");
        if (SessionManager.deviceManager() != null) {
            SessionManager.deviceManager().stopPolling();
        }

        AylaLanMode.pause(false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(LOG_TAG, "onResume");
        // Check to see if we're resuming due to the user tapping on the confirmation email link
        Uri uri = AccountConfirmActivity.uri;
        if (uri == null) {
            uri = getIntent().getData();
        }

        if (uri != null) {
            Log.i(LOG_TAG, "onResume: URI is " + uri);
            handleOpenURI(uri);
            // Clear out the URI
            AccountConfirmActivity.uri = null;
        }

        AylaLanMode.resume();

        if (SessionManager.deviceManager() != null) {
            SessionManager.deviceManager().startPolling();
        }
    }

    private static final String SIGNUP_TOKEN = "user_sign_up_token";
    private static final String RESET_PASSWORD_TOKEN = "user_reset_password_token";

    private void handleOpenURI(Uri uri) {
        // sign-up confirmation:
        // aylacontrol://user_sign_up_token?token=pdsWFmcU

        // Reset password confirmation:
        // aylacontrol://user_reset_password_token?token=3DrjCTqs

        String path = uri.getLastPathSegment();
        if (path == null) {
            // Some URIs are formatted without a path after the host. Just use the hostname in
            // this case.
            path = uri.getHost();
        }
        String query = uri.getEncodedQuery();
        String parts[] = null;
        if (query != null) {
            parts = query.split("=");
        }

        if (path.equals(SIGNUP_TOKEN)) {
            if (parts == null || parts.length != 2 || !parts[0].equals("token")) {
                // Unknown query string
                Toast.makeText(this, R.string.error_open_uri, Toast.LENGTH_SHORT).show();
            } else {
                handleUserSignupToken(parts[1]);
            }
        } else if (path.equals(RESET_PASSWORD_TOKEN)) {
            if (parts == null || parts.length != 2 || !parts[0].equals("token")) {
                // Unknown query string
                Toast.makeText(this, R.string.error_open_uri, Toast.LENGTH_SHORT).show();
            } else {
                handleUserResetPasswordToken(parts[1]);
            }
        } else {
            Log.e(LOG_TAG, "Unknown URI: " + uri);
        }
    }

    // SessionListener methods
    @Override
    public void loginStateChanged(final boolean loggedIn, AylaUser aylaUser) {
        // Post in a handler in case we just resumed and the instance state has changed
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                dismissWaitDialog();
                Log.d(LOG_TAG, "Login state changed. Logged in: " + loggedIn);
                if (!loggedIn) {
                    if (_loginDialog == null) {
                        showLoginDialog();
                    }
                } else {
                    if (_loginDialog != null) {
                        _loginDialog.dismiss();
                        _loginDialog = null;
                    }
                }
                setCloudConnectivityIndicator(AylaReachability.getReachability() == AylaNetworks.AML_REACHABILITY_REACHABLE);
            }
        });
    }

    @Override
    public void reachabilityChanged(int reachabilityState) {
        setCloudConnectivityIndicator(reachabilityState == AylaNetworks.AML_REACHABILITY_REACHABLE);
    }

    @Override
    public void lanModeChanged(boolean lanModeEnabled) {

    }

    /**
     * Pushes the specified fragment onto the back stack using a fade animation
     *
     * @param frag The fragment to be pushed
     */
    public void pushFragment(Fragment frag) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out,
                R.anim.abc_fade_in, R.anim.abc_fade_out);
        ft.add(android.R.id.content, frag).addToBackStack(null).commit();
    }

    static class SignUpConfirmationHandler extends Handler {

        public void handleMessage(android.os.Message msg) {
            String jsonResults = (String) msg.obj;

            // clear sign-up token
            if (AylaNetworks.succeeded(msg)) {
                // save auth info of current user
                AylaUser aylaUser = AylaSystemUtils.gson.fromJson(jsonResults, AylaUser.class);
                AylaSystemUtils.saveSetting(SessionManager.AYLA_SETTING_CURRENT_USER, jsonResults);

                String toastMessage = MainActivity.getInstance().getString(R.string.welcome_new_account);
                Toast.makeText(MainActivity.getInstance(), toastMessage, Toast.LENGTH_LONG).show();

                MainActivity.getInstance()._loginDialog.setUsername(aylaUser.email);

                // save existing user info
                AylaSystemUtils.saveSetting("currentUser", jsonResults);    // Allow lib access for accessToken refresh
            } else {
                AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "E", "amca.signin", "userSignUpConfirmation", "Failed", "userSignUpConfirmation_handler");
                int resID;
                if (msg.arg1 == 422) {
                    resID = R.string.error_invalid_token; // Invalid token
                } else {
                    resID = R.string.error_account_confirm_failed; // Unknown error occurred
                }

                AlertDialog.Builder ad = new AlertDialog.Builder(MainActivity.getInstance());
                ad.setTitle(R.string.error_sign_up_title);
                ad.setMessage(resID);
                ad.setPositiveButton(android.R.string.ok, null);
                ad.show();
            }
        }
    }

    private SignUpConfirmationHandler _signUpConfirmationHandler = new SignUpConfirmationHandler();

    private void handleUserSignupToken(String token) {
        Log.d(LOG_TAG, "handleUserSignupToken: " + token);

        if (AylaUser.user.getauthHeaderValue().contains("none")) {
            // authenticate the token
            Map<String, String> callParams = new HashMap<String, String>();
            callParams.put("confirmation_token", token); // required
            AylaUser.signUpConfirmation(_signUpConfirmationHandler, callParams);
        } else {
            Toast.makeText(AylaNetworks.appContext, R.string.error_sign_out_first, Toast.LENGTH_SHORT).show();
        }
    }

    void handleUserResetPasswordToken(String token) {
        Log.i(LOG_TAG, "handleUserResetPasswordToken: " + token);
        ResetPasswordDialog d = new ResetPasswordDialog();
        d.setToken(token);
        d.show(getSupportFragmentManager(), "reset_password");
    }

    private SignInDialog _loginDialog;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if ( _drawerLayout != null && _drawerLayout.isDrawerOpen(_drawerList)) {
            menu.clear();
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if ( _drawerToggle != null ) {
            if ( _drawerToggle.onOptionsItemSelected(item) ) {
                return true;
            }
        }

        if (MenuHandler.handleMenuItem(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Launches the sign-up dialog
     */
    private void launchSignUpDialog() {
        Log.i(LOG_TAG, "Sign up");
        SignUpDialog d = new SignUpDialog(this, this);
        d.show();
    }

    @Override
    public void signUpSucceeded(AylaUser newUser) {
        Toast.makeText(this, R.string.sign_up_success, Toast.LENGTH_LONG).show();

        // Update the username / password fields in our sign-in dialog and sign in the user
        if (_loginDialog != null) {
            EditText username = (EditText) _loginDialog.getView().findViewById(R.id.userNameEditText);
            EditText password = (EditText) _loginDialog.getView().findViewById(R.id.passwordEditText);
            username.setText(newUser.email);
            password.setText(newUser.password);
        }
    }

    //
    // SignInDialogListener methods
    //
    @Override
    public void signIn(String username, String password) {
        showWaitDialog(R.string.signingIn, R.string.signingIn);
        SessionManager.startSession(username, password);
    }

    private class ErrorMessage {
        @Expose
        String error;
    }

    @Override
    public void signInOAuth(Message msg) {
        if (AylaNetworks.succeeded(msg)) {
            // Make sure we have an auth token. Sometimes we get back "OK" but there is
            // really an error message.
            AylaUser user = AylaSystemUtils.gson.fromJson((String) msg.obj, AylaUser.class);
            if (user == null || user.getAccessToken() == null || user.getRefreshToken() == null) {
                ErrorMessage errorMessage = AylaSystemUtils.gson.fromJson((String) msg.obj, ErrorMessage.class);
                if (errorMessage != null) {
                    Toast.makeText(this, errorMessage.error, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, getString(R.string.error_signing_in), Toast.LENGTH_LONG).show();
                }
                Log.e(LOG_TAG, "OAuth: No access token returned! Message: " + msg);
            } else {
                _loginDialog.dismiss();
                _loginDialog = null;
                SessionManager.startOAuthSession(msg);
            }
        } else {
            if (msg.arg1 == AylaNetworks.AML_ERROR_UNREACHABLE) {
                Toast.makeText(this, getString(R.string.error_no_connectivity), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, getString(R.string.unknown_error), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void signUp() {
        launchSignUpDialog();
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            int displayMode = getUIConfig()._listStyle == UIConfig.ListStyle.List ?
                    AllDevicesFragment.DISPLAY_MODE_LIST : AllDevicesFragment.DISPLAY_MODE_GRID;

            switch (position) {
                case 0:
                    return AllDevicesFragment.newInstance(displayMode);

                case 1:
                    return DeviceGroupsFragment.newInstance(displayMode);

                case 2:
                    return SettingsFragment.newInstance();

                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            // Show 3 pages (for now).
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.all_devices);

                case 1:
                    return getString(R.string.groups);

                case 2:
                    return getString(R.string.settings);
                default:
                    return null;
            }
        }
    }
}
