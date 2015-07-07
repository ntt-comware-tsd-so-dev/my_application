package com.aylanetworks.agilelink;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.internal.view.menu.MenuBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaLanMode;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaReachability;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.device.AgileLinkDeviceCreator;
import com.aylanetworks.agilelink.fragments.AllDevicesFragment;
import com.aylanetworks.agilelink.fragments.DeviceGroupsFragment;
import com.aylanetworks.agilelink.fragments.SettingsFragment;
import com.aylanetworks.agilelink.fragments.adapters.NestedMenuAdapter;
import com.aylanetworks.agilelink.framework.Logger;
import com.aylanetworks.agilelink.framework.MenuHandler;
import com.aylanetworks.agilelink.framework.SessionManager;
import com.aylanetworks.agilelink.framework.UIConfig;
import com.google.gson.annotations.Expose;

/*
 * MainActivity.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 12/17/14
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class MainActivity extends ActionBarActivity implements SessionManager.SessionListener, AgileLinkApplication.AgileLinkApplicationListener {

    private static final String LOG_TAG = "Main Activity";

    // request IDs for intents we want results from
    public static final int REQ_PICK_CONTACT = 1;
    public static final int REQ_SIGN_IN = 2;

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
     * @return the MainActivity singleton object
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
     * The caller should call dismissWaitDialog() when finished.  This dialog is also cancelable
     * by pressing the back key.
     *
     * @param title title of the dialog.
     * @param message message to display.
     * @param cancelListener Cancel listener.
     */
    public void showWaitDialogWithCancel(String title, String message, DialogInterface.OnCancelListener cancelListener) {
        if (_progressDialog != null) {
            dismissWaitDialog();
        }

        if (title == null) {
            title = getResources().getString(R.string.please_wait);
        }

        _progressDialog = ProgressDialog.show(this, title, message, true, true, cancelListener);
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
        // Put the orientation back to what it was before we messed with it
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

    /**
     * Sets the appropriate connectivity icon in the action bar. This will be a cloud if we have
     * connectivity to the Ayla cloud service, or a WiFi icon if we do not.
     *
     * @param cloudConnectivity true if we can connect to the cloud, false otherwise
     */
    public void setCloudConnectivityIndicator(boolean cloudConnectivity) {
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        RelativeLayout layout = (RelativeLayout)getLayoutInflater().inflate(cloudConnectivity ?
                R.layout.cloud_actionbar : R.layout.wifi_actionbar, null);
        ImageView icon = (ImageView)layout.findViewById(R.id.icon);
        icon.setColorFilter(getResources().getColor(R.color.app_theme_accent), PorterDuff.Mode.SRC_ATOP);

        getSupportActionBar().setCustomView(layout);
    }

    private boolean _noDevicesMode = false;
    /**
     * Sets the application in a mode when no devices are present. The only available options to the
     * user at this point are to scan for devices in wifi setup, or register devices.
     * @param noDevices true to set No Devices Mode, false to enter normal mode
     */
    public void setNoDevicesMode(boolean noDevices) {
        if ( noDevices == _noDevicesMode ) {
            return;
        }

        _noDevicesMode = noDevices;
        popBackstackToRoot();

        // If we're logged out or logging out, don't enter no devices mode
        if ( SessionManager.deviceManager() == null || SessionManager.deviceManager().isShuttingDown() ) {
            _noDevicesMode = false;
        }

        if ( _noDevicesMode ) {
            SessionManager.deviceManager().stopPolling();
            setContentView(R.layout.activity_main_no_devices);
            MenuHandler.handleAddDevice();
        } else {
            initUI();
        }
    }

    /**
     * Returns true if the app is in "no devices" mode where only the wifi setup page is visible
     * @return true if the app is in "no devices" mode
     */
    public boolean isNoDevicesMode() {
        return _noDevicesMode;
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
            runOnUiThread(new Runnable() {
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
                        c.close();

                    } else {
                        _pickContactListener.contactPicked(null);
                    }
                    _pickContactListener = null;
                }
            });
        } else if ( reqCode == REQ_SIGN_IN ) {
            Log.d(LOG_TAG, "Login screen finished");

            if ( resultCode == RESULT_FIRST_USER ) {
                Log.d(LOG_TAG, "Back pressed from login. Finishing.");
                finish();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Phones are portrait-only. Tablets support orientation changes.
        if(getResources().getBoolean(R.bool.portrait_only)){
            Log.i("BOOL", "portrait_only: true");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            Log.i("BOOL", "portrait_only: false");
        }

        _theInstance = this;
        AylaNetworks.appContext = this;

        super.onCreate(savedInstanceState);

        initUI();

        // Set up the session manager with our session parameters
        SessionManager.setParameters(getAppParameters());

        // We want to know when the user logs in or out
        SessionManager.addSessionListener(this);

        // We want to know about application state changes
        ((AgileLinkApplication)getApplication()).addListener(this);
    }

    @Override
    public void onDestroy() {
        _theInstance = null;
        SessionManager.removeSessionListener(this);
        ((AgileLinkApplication)getApplication()).removeListener(this);
        super.onDestroy();
    }

    private void initUI() {
        switch ( getUIConfig()._navStyle ) {
            case Pager:
                initPager();
                break;

            case Drawer:
                initDrawer();
                break;
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
    private ExpandableListView _drawerList;
    private ActionBarDrawerToggle _drawerToggle;
    private Menu _drawerMenu;

    private void initDrawer() {
        Log.d(LOG_TAG, "initDrawer");

        setContentView(R.layout.activity_main_drawer);
        // Load the drawer menu resource. We will be using the menu items in our listview.
        _drawerMenu = new MenuBuilder(this);
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.menu_drawer, _drawerMenu);
        inflater.inflate(R.menu.menu_drawer_gateway, _drawerMenu);
        inflater.inflate(R.menu.menu_settings, _drawerMenu);
        _drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        Log.d(LOG_TAG, "_drawerLayout: " + _drawerLayout);

        _drawerList = (ExpandableListView)findViewById(R.id.left_drawer);

        NestedMenuAdapter drawerAdapter = new NestedMenuAdapter(this, R.layout.navigation_drawer_item, R.id.nav_textview, _drawerMenu);
        _drawerList.setAdapter(drawerAdapter);
        _drawerList.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                MenuItem item = _drawerMenu.getItem(groupPosition);
                if (item.hasSubMenu()) {
                    // Toggle the group
                    if (_drawerList.isGroupExpanded(groupPosition)) {
                        _drawerList.collapseGroup(groupPosition);
                    } else {
                        _drawerList.expandGroup(groupPosition);
                    }
                } else {
                    onDrawerItemClicked(item);
                }
                return true;
            }
        });

        _drawerList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                onDrawerItemClicked(_drawerMenu.getItem(groupPosition).getSubMenu().getItem(childPosition));
                return true;
            }
        });

        if ( _drawerLayout != null ) {
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

            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_launcher);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);

            _drawerToggle.setHomeAsUpIndicator(R.drawable.ic_launcher);
            _drawerLayout.setDrawerListener(_drawerToggle);
        }

        onDrawerItemClicked(_drawerMenu.getItem(0));
    }

    public void openDrawer() {
        if ( _drawerLayout != null ) {
            _drawerLayout.openDrawer(_drawerList);
        }
    }

    public void closeDrawer() {
        if ( _drawerLayout != null ) {
            _drawerLayout.closeDrawer(_drawerList);
            for ( int i = 0; i < _drawerList.getAdapter().getCount(); i++ ) {
                _drawerList.collapseGroup(i);
            }
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

    private void onDrawerItemClicked(MenuItem menuItem) {
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
    public SessionManager.SessionParameters getAppParameters() {
        final SessionManager.SessionParameters parameters = new SessionManager.SessionParameters(this);

        // Change this to false to connect to the production service
        boolean useDevService = true;
        if ( useDevService ) {
            // Development values
            parameters.appId = "AgileLinkProd-id";
            parameters.appSecret = "AgileLinkProd-8249425";
        } else {
            // Production values
            parameters.appId = "AgileLinkProd-id";
            parameters.appSecret = "AgileLinkProd-1530606";
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
                !_noDevicesMode &&
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
            Log.d(LOG_TAG, "Popping backstack (" + getSupportFragmentManager().getBackStackEntryCount() + ")");

            FragmentManager.BackStackEntry firstEntry = getSupportFragmentManager().getBackStackEntryAt(0);
            getSupportFragmentManager().popBackStackImmediate(firstEntry.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    private boolean _loginScreenUp;
    private void showLoginDialog() {
        Log.d(LOG_TAG, "showLoginDialog:");
        if ( _loginScreenUp ) {
            Log.e(LOG_TAG, "showLoginDialog: Already shown");
            return;
        }

        _loginScreenUp = true;

        SharedPreferences settings = AgileLinkApplication.getSharedPreferences();
        final String savedUsername = settings.getString(SessionManager.PREFS_USERNAME, "");
        final String savedPassword = settings.getString(SessionManager.PREFS_PASSWORD, "");

        // If we've got the username / password, we have everything we need.
        if ( !TextUtils.isEmpty(savedUsername) && !TextUtils.isEmpty(savedPassword)) {
            signIn(savedUsername, savedPassword);
            return;
        }

        // We always want to show the "All Devices" page first
        if ( mViewPager != null ) {
            mViewPager.setCurrentItem(0);
        }
        popBackstackToRoot();

        Bundle args = new Bundle();
        args.putString(SignInActivity.ARG_USERNAME, savedUsername);
        args.putString(SignInActivity.ARG_PASSWORD, savedPassword);

        Intent intent = new Intent(this, SignInActivity.class);
        intent.putExtras(args);
        startActivityForResult(intent, REQ_SIGN_IN);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // No call for super(). Bug on API Level > 11.
        // https://code.google.com/p/android/issues/detail?id=19917
        Log.d(LOG_TAG, "onSaveInstanceState: " + outState);
    }

    @Override
    protected void onPause() {
        super.onPause();

        dismissWaitDialog();

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
        AylaLanMode.resume();

        if (SessionManager.deviceManager() != null) {
            SessionManager.deviceManager().startPolling();
            setCloudConnectivityIndicator(AylaReachability.getReachability() == AylaNetworks.AML_REACHABILITY_REACHABLE);
        } else if ( !_loginScreenUp ) {
            showLoginDialog();
        }
    }

    @Override
    public void applicationLifeCycleStateChange(AgileLinkApplication.LifeCycleState state) {
        Logger.logInfo(LOG_TAG, "app: applicationLifeCycleStateChange " + state);
        SessionManager.getInstance().setForeground((state == AgileLinkApplication.LifeCycleState.Foreground));
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
                    // User logged out.
                    setNoDevicesMode(false);
                    if (!_loginScreenUp) {
                        showLoginDialog();
                    } else {
                        Log.e(LOG_TAG, "Login screen is already up:");
                        Thread.dumpStack();
                    }
                } else {
                    // Finish  the login dialog
                    MainActivity.this.finishActivity(REQ_SIGN_IN);
                    _loginScreenUp = false;
                    dismissWaitDialog();
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
        ft.setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out, R.anim.abc_fade_in, R.anim.abc_fade_out);
        if ( getUIConfig()._navStyle == UIConfig.NavStyle.Pager ) {
            // For the pager navigation, we push the fragment
            ft.add(android.R.id.content, frag).addToBackStack(null).commit();
        } else {
            // For the drawer navigation, we replace the fragment
            ft.replace(R.id.content_frame, frag).addToBackStack(null).commit();
        }
    }

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

    public void signIn(String username, String password) {
        showWaitDialog(R.string.signingIn, R.string.signingIn);
        SessionManager.startSession(username, password);
    }

    private class ErrorMessage {
        @Expose
        String error;
    }

    //@Override
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
            switch (position) {
                case 0:
                    return AllDevicesFragment.newInstance();

                case 1:
                    return DeviceGroupsFragment.newInstance();

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
