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
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.internal.view.menu.MenuBuilder;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaLanMode;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaReachability;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.controls.AylaPagerTabStrip;
import com.aylanetworks.agilelink.device.AgileLinkDeviceCreator;
import com.aylanetworks.agilelink.fragments.AllDevicesFragment;
import com.aylanetworks.agilelink.fragments.DeviceGroupsFragment;
import com.aylanetworks.agilelink.fragments.GatewayDevicesFragment;
import com.aylanetworks.agilelink.fragments.SettingsFragment;
import com.aylanetworks.agilelink.fragments.SharesFragment;
import com.aylanetworks.agilelink.fragments.adapters.NestedMenuAdapter;
import com.aylanetworks.agilelink.framework.Logger;
import com.aylanetworks.agilelink.framework.MenuHandler;
import com.aylanetworks.agilelink.framework.SessionManager;
import com.aylanetworks.agilelink.framework.UIConfig;
import com.google.gson.annotations.Expose;

import java.util.List;

/*
 * MainActivity.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 12/17/14
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class MainActivity extends ActionBarActivity implements SessionManager.SessionListener, AgileLinkApplication.AgileLinkApplicationListener, View.OnClickListener {

    private static final String LOG_TAG = "Main Activity";

    // request IDs for intents we want results from
    public static final int REQ_PICK_CONTACT = 1;
    public static final int REQ_SIGN_IN = 2;

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
        _uiConfig._navStyle = UIConfig.NavStyle.Material;
    }

    ProgressDialog _progressDialog;
    long _progressDialogStart;

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
            title = getString(R.string.please_wait);
        }
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setTitle(title);
        dialog.setIcon(R.drawable.ic_launcher);
        dialog.setMessage(message);
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.setOnCancelListener(null);
        dialog.show();
        _progressDialog = dialog;
        _progressDialogStart = System.currentTimeMillis();
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
            title = getString(R.string.please_wait);
        }
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setTitle(title);
        dialog.setIcon(R.drawable.ic_launcher);
        dialog.setMessage(message);
        dialog.setIndeterminate(true);
        dialog.setCancelable(true);
        dialog.setOnCancelListener(cancelListener);
        dialog.show();
        _progressDialog = dialog;
        _progressDialogStart = System.currentTimeMillis();
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
        }
        _progressDialog = null;
        _progressDialogStart = 0;
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
            if (getUIConfig()._navStyle == UIConfig.NavStyle.Material) {
                setContentView(R.layout.activity_main_no_devices_material);
                _toolbar = (Toolbar) findViewById(R.id.toolbar);
                setSupportActionBar(_toolbar);
            } else {
                setContentView(R.layout.activity_main_no_devices);
            }
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
            Log.d(LOG_TAG, "nod: Login screen finished");

            if ( resultCode == RESULT_FIRST_USER ) {
                Log.d(LOG_TAG, "nod: Back pressed from login. Finishing.");
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
        setUITheme();
        super.onCreate(savedInstanceState);
        initUI();

        // Set up the session manager with our session parameters
        SessionManager.setParameters(getAppParameters());

        // We want to know when the user logs in or out
        SessionManager.addSessionListener(this);

        // We want to know about application state changes
        ((AgileLinkApplication)getApplication()).addListener(this);

        // force to Production Service
        //SessionManager.getInstance().setServiceType(AylaNetworks.AML_STAGING_SERVICE);
    }

    @Override
    public void onDestroy() {
        _theInstance = null;
        SessionManager.removeSessionListener(this);
        ((AgileLinkApplication)getApplication()).removeListener(this);
        super.onDestroy();
    }

    private void setUITheme() {
        switch ( getUIConfig()._navStyle ) {
            case Tabbed:
            case Pager:
            case Drawer:
                setTheme(R.style.AppTheme);
                break;

            case Material:
                setTheme(R.style.AppMaterialTheme);
                break;
        }
    }

    private void initUI() {
        switch ( getUIConfig()._navStyle ) {
            case Tabbed:
                initTab();
                break;

            case Pager:
                initPager();
                break;

            case Drawer:
                initDrawer();
                break;

            case Material:
                initMaterial();
                break;
        }
    }

    SectionsPagerAdapter _sectionsPagerAdapter;
    ViewPager _viewPager;

    private void initTab() {
        setContentView(R.layout.activity_main_tabbed);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        _sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        _viewPager = (ViewPager) findViewById(R.id.pager);
        _viewPager.setAdapter(_sectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(_viewPager);
    }

    private void initPager() {
         setContentView(R.layout.activity_main_pager);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        _sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        _viewPager = (ViewPager) findViewById(R.id.pager);
        _viewPager.setAdapter(_sectionsPagerAdapter);

        AylaPagerTabStrip tabLayout = (AylaPagerTabStrip) findViewById(R.id.pager_tab_strip);
        tabLayout.setupWithViewPager(_viewPager);
    }

    private ExpandableListView _drawerList;
    private Menu _drawerMenu;

    private DrawerLayout _drawerLayout;
    private ActionBarDrawerToggle _drawerToggle;

    // AppBar
    // http://www.android4devs.com/2014/12/how-to-make-material-design-app.html
    // Colors
    // http://www.sankk.in/material-mixer/

    private Toolbar _toolbar;
    private NavigationView _navigationView;
    private TextView _userView;
    private TextView _emailView;
    private View _sideBarView;

    private void initMaterial() {
        Log.d(LOG_TAG, "initMaterial");
        setContentView(R.layout.activity_main_material);

        _toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(_toolbar);
        final ActionBar ab = getSupportActionBar();
        //ab.setIcon(R.drawable.ic_launcher);
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeButtonEnabled(true);

        _sideBarView = findViewById(R.id.action_sidebar);
        if (_sideBarView != null) {
            findViewById(R.id.action_all_devices).setOnClickListener(this);
            findViewById(R.id.action_device_groups).setOnClickListener(this);
            findViewById(R.id.action_device_scenes).setOnClickListener(this);
            findViewById(R.id.action_gateways).setOnClickListener(this);
            findViewById(R.id.action_shares).setOnClickListener(this);
            findViewById(R.id.action_account).setOnClickListener(this);
            findViewById(R.id.action_contact_list).setOnClickListener(this);
            findViewById(R.id.action_help).setOnClickListener(this);
            findViewById(R.id.action_about).setOnClickListener(this);
        }

        _drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        _drawerToggle = new ActionBarDrawerToggle(this, _drawerLayout, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                AylaUser currentUser = AylaUser.getCurrent();
                StringBuilder sb = new StringBuilder(64);
                if (!TextUtils.isEmpty(currentUser.firstname)) {
                    sb.append(currentUser.firstname);
                }
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                if (!TextUtils.isEmpty(currentUser.lastname)) {
                    sb.append(currentUser.lastname);
                }
                _userView.setText(sb.toString());
                _emailView.setText(currentUser.email);
                MainActivity.this.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                MainActivity.this.onDrawerClosed(drawerView);
            }
        };

        _drawerLayout.setDrawerListener(_drawerToggle); // Drawer Listener set to the Drawer toggle
        _drawerToggle.syncState();

        _navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (_navigationView != null) {
            _navigationView.setNavigationItemSelectedListener(
                    new NavigationView.OnNavigationItemSelectedListener() {
                        @Override
                        public boolean onNavigationItemSelected(MenuItem menuItem) {
                            closeDrawer();
                            MenuHandler.handleMenuItem(menuItem);
                            return true;
                        }
                    });
            _drawerMenu = _navigationView.getMenu();
            _userView = (TextView)_navigationView.findViewById(R.id.username);
            _emailView = (TextView)_navigationView.findViewById(R.id.email);
            onDrawerItemClicked(_drawerMenu.getItem(0));
        }
    }

    public void onSelectMenuItemById(int id) {
        if (_navigationView != null) {
            Menu menu = _navigationView.getMenu();
            MenuItem item = menu.findItem(id);
            if (item != null) {
                MenuHandler.handleMenuItem(item);
            }
        } else {
            MenuHandler.handleMenuId(id);
        }
    }

    @Override
    public void onClick(View v) {
        onSelectMenuItemById(v.getId());
    }

    ImageView _sideBarIcon;

    public void activateMenuItem(MenuItem menuItem) {
        menuItem.setChecked(true);
        if (_sideBarView != null) {
            View view = findViewById(menuItem.getItemId());
            if (view instanceof ImageView) {
                if (_sideBarIcon != null) {
                    _sideBarIcon.clearColorFilter();
                }
                _sideBarIcon = (ImageView)view;
                if (_sideBarIcon != null) {
                    _sideBarIcon.setColorFilter(getResources().getColor(R.color.app_theme_accent), PorterDuff.Mode.SRC_ATOP);
                }
            }
        }
    }

    private void initDrawer() {
        Log.d(LOG_TAG, "initDrawer");

        setContentView(R.layout.activity_main_drawer);

        // Load the drawer menu resource. We will be using the menu items in our listview.
        _drawerMenu = new MenuBuilder(this);
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.menu_drawer, _drawerMenu);
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
            if (_drawerList != null) {
                _drawerLayout.openDrawer(_drawerList);
            } else {
                _drawerLayout.openDrawer(GravityCompat.START);
            }
        }
    }

    public void closeDrawer() {
        if ( _drawerLayout != null ) {
            if (_drawerList != null) {
                _drawerLayout.closeDrawer(_drawerList);
                for (int i = 0; i < _drawerList.getAdapter().getCount(); i++) {
                    _drawerList.collapseGroup(i);
                }
            } else {
                _drawerLayout.closeDrawers();
            }
        }
    }

    public boolean isDrawerOpen() {
        if ( _drawerLayout == null ) {
            return false;
        }
        if (_drawerList != null) {
            return _drawerLayout.isDrawerOpen(_drawerList);
        }
        return _drawerLayout.isDrawerOpen(GravityCompat.START);
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

    Fragment getVisibleFragment() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment != null && fragment.getUserVisibleHint()) {
                return fragment;
            }
        }
        return null;
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        int bsc = fm.getBackStackEntryCount();
        // Open the nav drawer on back unless it's already open (or we don't have one)
        if ( _drawerToggle != null &&
                !_noDevicesMode &&
                isDrawerOpen() ) {
            closeDrawer();
            //Log.e(LOG_TAG, "back: onBackPressed close menu");
            return;
        }

        // Go back to the dashboard
        if (bsc == 0) {
            Fragment frag = getVisibleFragment();
            if (frag != null) {
                //Log.e(LOG_TAG, "back: onBackPressed frag=" + frag.getClass().getSimpleName());
                if (!TextUtils.equals(frag.getClass().getSimpleName(), AllDevicesFragment.class.getSimpleName())) {
                    //Log.e(LOG_TAG, "back: onBackPressed select dashboard");
                    onSelectMenuItemById(R.id.action_all_devices);
                    return;
                }

            } else {

                //Log.e(LOG_TAG, "back: onBackPressed " + bsc + " frag=null");
            }
        } else if(bsc == 1 ){
            if(SessionManager.deviceManager().deviceList().isEmpty()){
                this.finish();

            }
        }else {
            FragmentManager.BackStackEntry bse = fm.getBackStackEntryAt(bsc - 1);
            //Log.e(LOG_TAG, "back: onBackPressed id=" + bse.getId() + ", name=" + bse.getName() + ", title=" + bse.getBreadCrumbTitle());
            Fragment frag = fm.findFragmentById(bse.getId());
            if (frag != null) {
                //Log.e(LOG_TAG, "back: onBackPressed frag=" + frag.getClass().getSimpleName());
            } else {
                ////Log.e(LOG_TAG, "back: onBackPressed " + bsc + " frag=null");
            }
        }
        Log.e(LOG_TAG, "back: onBackPressed super");
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
        if ( _viewPager != null ) {
            _viewPager.setCurrentItem(0);
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

        if (SessionManager.deviceManager() != null) {
            // we aren't going to "resume" LAN mode if we aren't logged in.
            AylaLanMode.resume();

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
                Log.d(LOG_TAG, "nod: Login state changed. Logged in: " + loggedIn);
                if (!loggedIn) {
                    // User logged out.
                    setNoDevicesMode(false);
                    if (!_loginScreenUp) {
                        showLoginDialog();
                    } else {
                        Log.e(LOG_TAG, "nod: Login screen is already up:");
                        Thread.dumpStack();
                    }
                } else {
                    // Finish  the login dialog
                    Log.d(LOG_TAG, "nod: finish login dialog");
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
        Logger.logInfo(LOG_TAG, "pushFragment " + frag.getClass().getSimpleName());
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
        if ( _drawerLayout != null && _drawerList != null && _drawerLayout.isDrawerOpen(_drawerList)) {
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
                case 0: // Dashboard
                    return AllDevicesFragment.newInstance();

                case 1: // Groups
                    return DeviceGroupsFragment.newInstance();

                case 2: // Gateways
                    return GatewayDevicesFragment.newInstance();

                case 3: // Shares
                    return SharesFragment.newInstance();

                case 4: // Settings
                    return SettingsFragment.newInstance();

                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            // Show 5 pages (for now).
            return 5;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: // Dashboard
                    return getString(R.string.dashboard);

                case 1: // Groups
                    return getString(R.string.groups);

                case 2: // Gateways
                    return getString(R.string.gateways);

                case 3: // Shares
                    return getString(R.string.shares);

                case 4: // Settings
                    return getString(R.string.settings);
                default:
                    return null;
            }
        }
    }
}
