package com.aylanetworks.agilelink;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.MenuBuilder;
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
import com.android.volley.Response;
import com.aylanetworks.agilelink.device.AMAPViewModelProvider;
import com.aylanetworks.agilelink.fragments.FingerPrintDialogFragment;
import com.aylanetworks.agilelink.fragments.FingerprintUiHelper;
import com.aylanetworks.agilelink.fragments.NotificationListFragment;
import com.aylanetworks.agilelink.fragments.ShareUpdateFragment;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.AccountSettings;
import com.aylanetworks.agilelink.framework.geofence.Action;
import com.aylanetworks.agilelink.framework.geofence.AylaDeviceActions;
import com.aylanetworks.agilelink.geofence.AMAPGeofenceService;
import com.aylanetworks.agilelink.geofence.AllGeofencesFragment;
import com.aylanetworks.aylasdk.AylaDatapoint;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaDeviceManager;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.aylasdk.AylaSessionManager;
import com.aylanetworks.aylasdk.AylaShare;
import com.aylanetworks.aylasdk.AylaSystemSettings;
import com.aylanetworks.aylasdk.AylaUser;
import com.aylanetworks.agilelink.controls.AylaPagerTabStrip;
import com.aylanetworks.agilelink.device.AMAPViewModelProvider;
import com.aylanetworks.agilelink.fragments.AllDevicesFragment;
import com.aylanetworks.agilelink.fragments.DeviceGroupsFragment;
import com.aylanetworks.agilelink.fragments.GatewayDevicesFragment;
import com.aylanetworks.agilelink.fragments.SettingsFragment;
import com.aylanetworks.agilelink.fragments.SharesFragment;
import com.aylanetworks.agilelink.fragments.adapters.NestedMenuAdapter;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.AccountSettings;
import com.aylanetworks.agilelink.framework.Logger;
import com.aylanetworks.agilelink.framework.UIConfig;
import com.aylanetworks.aylasdk.AylaDeviceManager;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaSessionManager;
import com.aylanetworks.aylasdk.AylaUser;
import com.aylanetworks.aylasdk.auth.AylaAuthorization;
import com.aylanetworks.aylasdk.auth.CachedAuthProvider;
import com.aylanetworks.aylasdk.auth.UsernameAuthProvider;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.util.TypeUtils;
import com.google.android.gms.location.Geofence;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import static com.aylanetworks.agilelink.framework.AMAPCore.SessionParameters;
/*
 * MainActivity.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 12/17/14
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class MainActivity extends AppCompatActivity
        implements AylaSessionManager.SessionManagerListener,
        AgileLinkApplication.AgileLinkApplicationListener,
        View.OnClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback{

    private static final String LOG_TAG = "Main Activity";

    private static int REQUEST_WRITE_EXTERNAL_STORAGE = 0;
    private static int REQUEST_CONTACT = 1;
    private static final int REQUEST_LOCATION = 2;

    // request IDs for intents we want results from
    public static final int REQ_PICK_CONTACT = 1;
    public static final int REQ_SIGN_IN = 2;
    public static final int REQ_CHECK_FINGERPRINT = 3;
    public static final int REQUEST_FINE_LOCATION = 4;
    public static final int PLACE_PICKER_REQUEST = 5;

    public static AylaLog.LogLevel LOG_PERMIT = AylaLog.LogLevel.None;

    public static final String ARG_SHARE = "share";
    private static MainActivity _theInstance;
    private KeyStore _keyStore;
    private Cipher _cipher;
    private static final String KEY_NAME = "finger-print-key-app";
    public final static String ARG_TRIGGER_TYPE = "trgigger_type";
    public final static String GEO_FENCE_LIST = "geo_fence_list";


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
        _uiConfig._listStyle = UIConfig.ListStyle.List;
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
        if(message != null){
            dialog.setMessage(message);
        }
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
            try {
                _progressDialog.dismiss();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        _progressDialog = null;
        _progressDialogStart = 0;
    }

    /**
     * Returns a string containing the app version defined in the package
     *
     * @return The app version string
     */
    private static String getAppVersion(Context context) {
        PackageInfo info;
        try {
            info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return context.getString(R.string.unknown_app_version);
        }

        return info.versionName + "." + info.versionCode;
    }

    public String getAppVersion() {
        return getAppVersion(this);
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
        if ( AMAPCore.sharedInstance().getDeviceManager() == null ) {
            _noDevicesMode = false;
        }

        if ( _noDevicesMode ) {
            AMAPCore.sharedInstance().getDeviceManager().stopPolling();
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

    /////////////////////////////
    // AylaSessionManagerListener methods

    @Override
    public void sessionClosed(String sessionName, AylaError error) {
        //Make sure the user did not sign out normally (i.e error=null)
        if(error !=null && checkFingerprintOption()){
            showFingerPrint();
        }
        else {
            // User logged out.
            setNoDevicesMode(false);
            if (!_loginScreenUp) {
                showLoginDialog(true);
            } else {
                Log.e(LOG_TAG, "nod: Login screen is already up:");
            }
        }
    }

    @Override
    public void authorizationRefreshed(String sessionName, AylaAuthorization authorization) {
        CachedAuthProvider.cacheAuthorization(this, authorization);
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

        if(ActivityCompat.checkSelfPermission(this, "android.permission.READ_CONTACTS") != PackageManager.PERMISSION_GRANTED){
            requestContactPermissions();
        } else{
            Intent intent = new Intent(Intent.ACTION_PICK,
                    ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(intent, REQ_PICK_CONTACT);
        }

    }

    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);
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
            Log.d(LOG_TAG, "nod: SignInActivity finished");
            _loginScreenUp = false;

            if (resultCode == Activity.RESULT_OK) {
                if(_theInstance == null){
                    _theInstance = this;
                }
                handleSignedIn();

                // Start wearable service. If there are no wearable devices connected, the service
                // will stop itself
                if(!AMAPCore.sharedInstance().getSessionManager().isCachedSession()){
                    Intent wearService = new Intent(this, WearUpdateService.class);
                    startService(wearService);
                }
            } else if ( resultCode == RESULT_FIRST_USER ) {
                Log.d(LOG_TAG, "nod: Back pressed from login. Finishing.");
                finish();
            }
        } else if(reqCode == REQ_CHECK_FINGERPRINT) {
            showLoginDialog(false);
        } else if(reqCode == PLACE_PICKER_REQUEST){
            Fragment fragment = getSupportFragmentManager().findFragmentByTag("AddGeofenceFragment");
            fragment.onActivityResult(reqCode, resultCode, data);
        }
}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Phones are portrait-only. Tablets support orientation changes.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermissions();
        }
        if (getResources().getBoolean(R.bool.portrait_only)) {
            Log.i("BOOL", "portrait_only: true");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            Log.i("BOOL", "portrait_only: false");
        }

        _theInstance = this;

        setUITheme();
        super.onCreate(savedInstanceState);
        initUI();

        if (AMAPCore.sharedInstance() == null) {
            AMAPCore.initialize(getAppParameters(this), this);
        }

        if (!_loginScreenUp) {
            boolean allowOfflineUse = AylaNetworks.sharedInstance().getSystemSettings().allowOfflineUse;
            if (allowOfflineUse) {
                //For off line mode don't disable existing cache
                showLoginDialog(false);
            } else {
                boolean expireAuthToken = AgileLinkApplication.getSharedPreferences()
                        .getBoolean(getString(R.string.always_expire_auth_token), false);
                showLoginDialog(expireAuthToken);
            }
        }

        // We want to know about application state changes
        ((AgileLinkApplication) getApplication()).addListener(this);
        if (checkFingerprintOption()) {
            createKey();
        }
        Bundle bundle = this.getIntent().getExtras();
        if (bundle != null){
           boolean bValue = bundle.getBoolean(ARG_TRIGGER_TYPE);
            final ArrayList<Geofence> geofenceList =(ArrayList<Geofence>)bundle.getSerializable(GEO_FENCE_LIST);
            AMAPGeofenceService.fetchAutomations(bValue,geofenceList);
        }
    }
    @Override
    public  void onNewIntent (Intent intent) {
        super.onNewIntent(intent);
        Bundle bundle = intent.getExtras();
        if (bundle != null){
            boolean bValue = bundle.getBoolean(ARG_TRIGGER_TYPE);
            final ArrayList<Geofence> geofenceList =(ArrayList<Geofence>)bundle.getSerializable(GEO_FENCE_LIST);
            if(geofenceList !=null) {
                //Now check if the app is running in the background
                if(!AgileLinkApplication.getsInstance().isActivityVisible()) {
                    moveTaskToBack(true);
                }

                AMAPGeofenceService.fetchAutomations(bValue, geofenceList);
            }
        }
    }

    @Override
    public void onDestroy() {
        AylaSessionManager sm = AMAPCore.sharedInstance().getSessionManager();
        if (sm != null) {
            sm.removeListener(this);
        }
        ((AgileLinkApplication)getApplication()).removeListener(this);

        // Only pause Ayla services here if the wearable service is not using it
        if (AgileLinkApplication.getsInstance().canPauseAylaNetworks(getClass().getName())) {
            AylaNetworks.sharedInstance().onPause();
        }

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
                updateDrawerHeader();
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
            View header = _navigationView.getHeaderView(0);
            _userView = (TextView)header.findViewById(R.id.username);
            _emailView = (TextView)header.findViewById(R.id.email);
            onDrawerItemClicked(_drawerMenu.getItem(0));

            boolean isGeofenceEnabled = AgileLinkApplication.getSharedPreferences().getBoolean
                    (getString(R.string.enable_geofence_feature), true);
            if(!isGeofenceEnabled) {
                MenuItem itemGeofence = _drawerMenu.findItem(R.id.action_geofences);
                MenuItem itemActions = _drawerMenu.findItem(R.id.action_al_actions);
                MenuItem itemAutomations = _drawerMenu.findItem(R.id.action_automations);
                if(itemGeofence != null) {
                    itemGeofence.setVisible(false);
                }
                if(itemActions != null) {
                    itemActions.setVisible(false);
                }
                if(itemAutomations != null) {
                    itemAutomations.setVisible(false);
                }
            }
        }
    }

    private void updateDrawerHeader() {
        AylaUser currentUser = AMAPCore.sharedInstance().getCurrentUser();
        if (currentUser != null) {
            if (_userView != null && _emailView != null) {
                StringBuilder sb = new StringBuilder(64);
                if (!TextUtils.isEmpty(currentUser.getFirstname())) {
                    sb.append(currentUser.getFirstname());
                }
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                if (!TextUtils.isEmpty(currentUser.getLastname())) {
                    sb.append(currentUser.getLastname());
                }
                _userView.setText(sb.toString());
                _emailView.setText(currentUser.getEmail());
            }
        } else {
            AMAPCore.sharedInstance().updateCurrentUserInfo();
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
                _sideBarIcon.setColorFilter(getResources().getColor(R.color.app_theme_accent), PorterDuff.Mode.SRC_ATOP);
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

    /**
    public void openDrawer() {
        if ( _drawerLayout != null ) {
            if (_drawerList != null) {
                _drawerLayout.openDrawer(_drawerList);
            } else {
                _drawerLayout.openDrawer(GravityCompat.START);
            }
        }
    }
     */

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
    public static SessionParameters getAppParameters(Context context) {
        final SessionParameters parameters = new SessionParameters(context);

        // Change this to false to connect to the production service
        boolean useDevService = true;
        if ( useDevService ) {
            // Development values
            parameters.appId = "AMAP-Dev-0dfc7900-id";
            parameters.appSecret = "AMAP-Dev-0dfc7900-sP7-7jkjjvSBrV5Pm3OFT0zpfM0";
            parameters.serviceType = AylaSystemSettings.ServiceType.Development;
        } else {
            // Production values
            parameters.appId = "AMAP-Prod-0dfc7900-id";
            parameters.appSecret = "AMAP-Prod-0dfc7900--svSZtnxon0EYPGDNqIFnfeT0F8";
            parameters.serviceType = AylaSystemSettings.ServiceType.Field;
        }

        parameters.viewModelProvider = new AMAPViewModelProvider();

        parameters.appVersion = getAppVersion(context);

        parameters.sessionName = AMAPCore.DEFAULT_SESSION_NAME;

        // Will attempt to put devices into LAN mode whenever possible
        parameters.enableLANMode = true;

        // Allows login when the service is not reachable, using cached data or connecting directly
        // with devices in LAN mode
        parameters.allowLANLogin = false;

        parameters.allowDSS = false;

        parameters.loggingLevel = LOG_PERMIT;

        parameters.ssoLogin = false;
        if(parameters.ssoLogin){
            parameters.appId = "client-id";
            parameters.appSecret = "client-2839357";
        }

        parameters.registrationEmailSubject = context.getResources().getString(R.string.registraion_email_subject);

        // For a custom HTML message, set REGISTRATION_EMAIL_TEMPLATE_ID to null and
        // REGISTRATION_EMAIL_BODY_HTML to an HTML string for the email message.
        if(getLocaleCountry(context).equalsIgnoreCase("ES")){
            parameters.registrationEmailTemplateId = "ayla_confirmation_template_01_es";
            parameters.resetPasswordEmailTemplateId = "ayla_passwd_reset_template_01_es";
        } else{
            parameters.registrationEmailTemplateId = "ayla_confirmation_template_01";
            parameters.resetPasswordEmailTemplateId = "ayla_passwd_reset_template_01";
        }

        if (parameters.registrationEmailTemplateId == null) {
            parameters.registrationEmailBodyHTML = context.getResources().getString(R.string.registration_email_body_html);
        } else {
            parameters.registrationEmailBodyHTML = null;
        }

        if(parameters.logsEmailAddress == null){
            parameters.logsEmailAddress = AMAPCore.DEFAULT_SUPPORT_EMAIL_ADDRESS;
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

            }
        } else if (bsc == 1 ){
            if(AMAPCore.sharedInstance().getDeviceManager().getDevices().isEmpty()){
                this.finish();
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
    private boolean _fingerPrintScreenUp;

    public void showFingerPrint(){
        Log.d(LOG_TAG, "nod: _fingerPrintScreenUp:");
        if ( _fingerPrintScreenUp ) {
            Log.i(LOG_TAG, "nod: _fingerPrintScreenUp: Already shown");
            return;
        }
        if (initCipher()) {

            // Show the fingerprint dialog. The user has the option to use the fingerprint with
            // crypto, or you can fall back to using a server-side verified password.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                FingerPrintDialogFragment mFragment = new FingerPrintDialogFragment();
                mFragment.setCancelable(false);
                mFragment.setCryptoObject(new FingerprintManager.CryptoObject(_cipher));
                _fingerPrintScreenUp=true;
                mFragment.show(getFragmentManager(), "DIALOG_FRAGMENT_TAG");
            }
        }
    }

    public boolean checkFingerprintOption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Fingerprint API only available on from Android 6.0 (M)
            boolean fingerPrintOption= AgileLinkApplication.getSharedPreferences()
                    .getBoolean(getString(R.string.use_fingerprint_to_authenticate), false);
            if(fingerPrintOption) {
                return FingerprintUiHelper.isFingerprintAuthAvailable();
            }
        }
        return false;
    }

    public void handleGeofenceSettingsChange(boolean enabled) {
        if(_drawerMenu == null) {
            return;
        }
        MenuItem itemGeofence = _drawerMenu.findItem(R.id.action_geofences);
        MenuItem itemActions = _drawerMenu.findItem(R.id.action_al_actions);
        MenuItem itemAutomations = _drawerMenu.findItem(R.id.action_automations);
        if(itemGeofence != null) {
            itemGeofence.setVisible(enabled);
        }
        if(itemActions != null) {
            itemActions.setVisible(enabled);
        }
        if(itemAutomations != null) {
            itemAutomations.setVisible(enabled);
        }
    }
    public void showLoginDialog(boolean disableCachedSignin) {
        Log.d(LOG_TAG, "nod: showLoginDialog:");
        if ( _loginScreenUp ) {
            Log.e(LOG_TAG, "nod: showLoginDialog: Already shown");
            return;
        }

        Intent intent = new Intent(this, SignInActivity.class);
        if (disableCachedSignin) {
            CachedAuthProvider.clearCachedAuthorization(this);
        } else {
            SharedPreferences settings = AgileLinkApplication.getSharedPreferences();
            final String savedUsername = settings.getString(AMAPCore.PREFS_USERNAME, "");
            final String savedPassword = settings.getString(AMAPCore.PREFS_PASSWORD, "");

            // If we've got the username / password, we have everything we need.
            if (!TextUtils.isEmpty(savedUsername) && !TextUtils.isEmpty(savedPassword)) {
                signIn(savedUsername, savedPassword);
                return;
            }

            Bundle args = new Bundle();
            args.putString(SignInActivity.ARG_USERNAME, savedUsername);
            args.putString(SignInActivity.ARG_PASSWORD, savedPassword);
            intent.putExtras(args);
        }

        _loginScreenUp = true;

        // We always want to show the "All Devices" page first
        if ( _viewPager != null ) {
            _viewPager.setCurrentItem(0);
        }
        popBackstackToRoot();

        intent.putExtra(SignInActivity.EXTRA_DISABLE_CACHED_SIGNIN, disableCachedSignin);
        Log.d(LOG_TAG, "nod: startActivityForResult SignInActivity");
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

        AylaDeviceManager dm = AMAPCore.sharedInstance().getDeviceManager();
        // Only pause Ayla services here if the wearable service is not using it
        if (dm != null && AgileLinkApplication.getsInstance().canPauseAylaNetworks(getClass().getName())) {
            dm.stopPolling();

            // we aren't going to "pause" LAN mode if we haven't been logged in.
            AylaNetworks.sharedInstance().onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(LOG_TAG, "onResume");
        if(_theInstance == null){
            _theInstance = this;
        }

        if (AMAPCore.sharedInstance() == null) {
            AMAPCore.initialize(getAppParameters(this), this);
        }
        // Only resume Ayla services if the wearable service hasn't already started it
        if (AgileLinkApplication.getsInstance().shouldResumeAylaNetworks(getClass().getName())) {
            AylaNetworks.sharedInstance().onResume();
        }
        else {
            checkLoginAndConnectivity();
        }
    }

    @Override
    public void applicationLifeCycleStateChange(AgileLinkApplication.LifeCycleState state) {
        Logger.logInfo(LOG_TAG, "app: applicationLifeCycleStateChange " + state);
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
        return MenuHandler.handleMenuItem(item) || super.onOptionsItemSelected(item);
    }

    public void signIn(String username, String password) {
        showWaitDialog(R.string.signingIn, R.string.signingIn);

        final Response.Listener<AylaAuthorization> successListener =
                new Response.Listener<AylaAuthorization>() {
                    @Override
                    public void onResponse(AylaAuthorization response) {
                        // We're signed in.
                        dismissWaitDialog();
                        // Finish  the login dialog
                        Log.d(LOG_TAG, "nod: finish login dialog");
                        finishActivity(REQ_SIGN_IN);
                        _loginScreenUp = false;
                        dismissWaitDialog();

                        CachedAuthProvider.cacheAuthorization(MainActivity.this, response);
                        // TODO: BSK: Determine if the cloud is reachable
                        setCloudConnectivityIndicator(true);
                        handleSignedIn();
                    }
                };

        final ErrorListener errorListener = new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                dismissWaitDialog();
                CachedAuthProvider.clearCachedAuthorization(MainActivity.this);
                Toast.makeText(MainActivity.this,
                        ErrorUtils.getUserMessage(MainActivity.this, error, R.string.unknown_error),
                        Toast.LENGTH_SHORT).show();
            }
        };

        if(getAppParameters(this).ssoLogin){
            AylaLog.d(LOG_TAG, " Login to Identity provider");
            // Login to Identity Provider first and get the access token for Ayla service
            final SSOAuthProvider ssoProvider = new SSOAuthProvider();
            ssoProvider.ssoLogin(username, password,
                    new Response.Listener<SSOAuthProvider.IdentityProviderAuth>() {
                @Override
                public void onResponse(SSOAuthProvider.IdentityProviderAuth response) {
                    AylaLog.d(LOG_TAG, " SSO login to Identitiy provider success");
                    AMAPCore.sharedInstance().startSession(ssoProvider,
                          successListener, errorListener);
                }
            }, new ErrorListener() {
                @Override
                public void onErrorResponse(AylaError error) {
                    AylaLog.d(LOG_TAG, " SSO login to Identitiy provider failed "+
                            error.getLocalizedMessage());
                }
            });


        } else{
            AylaLog.d(LOG_TAG, "Login to Ayla user service");
            UsernameAuthProvider authProvider = new UsernameAuthProvider(username, password);
            AMAPCore.sharedInstance().startSession(authProvider,
                    successListener, errorListener);
        }

    }

    /**
     * Called after the user has signed in
     */
    private void handleSignedIn() {
        // Let the all devices fragment know we are signed in
        if(AMAPCore.sharedInstance().getSessionManager().isCachedSession()){
            Toast.makeText(this, getString(R.string.lan_login_message), Toast.LENGTH_SHORT).show();
        }
        Fragment frag = getSupportFragmentManager().findFragmentById(R.id.content_frame);
        if (frag != null) {
            frag.onPause();
            frag.onResume();
        }

        // fetch account settings
        if(!AMAPCore.sharedInstance().getSessionManager().isCachedSession()){
            AMAPCore.sharedInstance().fetchAccountSettings(new AccountSettings.AccountSettingsCallback());
        }
        // update drawer header
        updateDrawerHeader();
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
    private static String getLocaleCountry(Context context){
        return context.getResources().getConfiguration().locale.getCountry();
    }


    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if( requestCode == REQUEST_WRITE_EXTERNAL_STORAGE){

            if(grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                LOG_PERMIT = AylaLog.LogLevel.Info;
            } else{
               //ToDo Add fallback case when write is denied and LogManager cannot work
                LOG_PERMIT = AylaLog.LogLevel.None;
            }
        }

        if(requestCode == REQUEST_CONTACT){
            if(grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, getResources().getText(R.string.contacts_permission_granted), Toast.LENGTH_SHORT).show();
            } else{
                Toast.makeText(this, getResources().getText(R.string.contacts_permission_denied), Toast.LENGTH_SHORT).show();
            }

        }

        if(requestCode == REQUEST_LOCATION){
            if(grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, getResources().getText(R.string.location_permission_granted), Toast.LENGTH_SHORT).show();
            }  else{
                Toast.makeText(this, getResources().getText(R.string.location_permission_denied), Toast.LENGTH_SHORT).show();
            }
        }

        if(requestCode == REQUEST_FINE_LOCATION){
            if(grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                MainActivity.getInstance().pushFragment(AllGeofencesFragment.newInstance());
            }  else{
                Toast.makeText(this, getResources().getText(R.string.location_permission_denied), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /*
    * Writing logs to memory requires android.permission.WRITE_EXTERNAL_STORAGE.
     */
    private void requestStoragePermissions(){
        ActivityCompat.requestPermissions(this, new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, REQUEST_WRITE_EXTERNAL_STORAGE);

    }

    /*
   * Picking contact from device's contacts requires android.permission.READ_CONTACTS.
    */
    private void requestContactPermissions(){
        ActivityCompat.requestPermissions(this, new String[]{"android.permission.READ_CONTACTS"}, REQUEST_CONTACT);

    }

    private void createKey() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        // The enrolling flow for fingerprint. This is where you ask the user to set up fingerprint
        // for your flow. Use of keys is necessary if you need to know if the set of
        // enrolled fingerprints has changed.
        try {
            _keyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        KeyGenerator keyGenerator;
        try {
            keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    "AndroidKeyStore");
        } catch (NoSuchAlgorithmException |
                NoSuchProviderException e) {
            String errMsg = "Failed to get KeyGenerator instance " + e.getMessage();
            Logger.logError(LOG_TAG, "Failed to create key " + e.getMessage());
            Toast.makeText(this, errMsg, Toast.LENGTH_LONG).show();
            return;
        }

        try {
            _keyStore.load(null);
            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder
            keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    // Require the user to authenticate with a fingerprint to authorize every use
                    // of the key
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException
                | CertificateException | IOException e) {
            String errMsg = "Failed to create key " + e.getMessage();
            Logger.logError(LOG_TAG, "Failed to create key " + e.getMessage());
            Toast.makeText(this, errMsg, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Initialize the {@link Cipher} instance with the created key in the {@link #createKey()}
     * method.
     *
     * @return {@code true} if initialization is successful, {@code false} if the lock screen has
     * been disabled or reset after the key was generated, or if a fingerprint got enrolled after
     * the key was generated.
     */
    private boolean initCipher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {

                _cipher = Cipher.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES + "/"
                                + KeyProperties.BLOCK_MODE_CBC + "/"
                                + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            } catch (NoSuchAlgorithmException |
                    NoSuchPaddingException e) {
                throw new RuntimeException("Failed to get Cipher", e);
            }

            try {
                if (_keyStore == null) {
                    createKey();
                }
                _keyStore.load(null);
                SecretKey key = (SecretKey) _keyStore.getKey(KEY_NAME, null);
                _cipher.init(Cipher.ENCRYPT_MODE, key);
                return true;
            } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException
                    | NoSuchAlgorithmException | InvalidKeyException e) {
                throw new RuntimeException("Failed to init Cipher", e);
            }
        }
        return false;
    }

    public void checkLoginAndConnectivity() {
        _fingerPrintScreenUp = false;
        AylaSessionManager sm = AMAPCore.sharedInstance().getSessionManager();
        if (sm != null) {
            setCloudConnectivityIndicator(true);
        } else if (!_loginScreenUp) {
            showLoginDialog(false);
        }
    }


}
