package com.aylanetworks.agilelink;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaLanMode;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.device.devkit.AgileLinkDeviceCreator;
import com.aylanetworks.agilelink.device.zigbee.NexTurnDeviceCreator;
import com.aylanetworks.agilelink.fragments.AllDevicesFragment;
import com.aylanetworks.agilelink.fragments.SettingsFragment;
import com.aylanetworks.agilelink.fragments.SignInDialog;
import com.aylanetworks.agilelink.fragments.SignUpDialog;
import com.aylanetworks.agilelink.framework.SessionManager;
import com.google.gson.annotations.Expose;

public class MainActivity extends ActionBarActivity implements SignUpDialog.SignUpListener, SignInDialog.SignInDialogListener {

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

    public static MainActivity getInstance() {
        return _theInstance;
    }

    ProgressDialog _progressDialog;

    public void showWaitDialog(String title, String message) {
        if (_progressDialog != null) {
            dismissWaitDialog();
        }

        if (title == null) {
            title = getResources().getString(R.string.please_wait);
        }

        _progressDialog = ProgressDialog.show(this, title, message, true);
    }

    public void dismissWaitDialog() {
        if (_progressDialog != null) {
            _progressDialog.dismiss();
            _progressDialog = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        _theInstance = this;
        AylaNetworks.appContext = this;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ActionBar actionBar = getActionBar();

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // Set up the session manager

        /**
         * Parameters for nexTurn network
         */
        SessionManager.SessionParameters nexTurnParams = new SessionManager.SessionParameters(this);
        nexTurnParams.appId = "iNextTurnKitDev-id";
        nexTurnParams.appSecret = "iNextTurnKitDev-6124332";
        nexTurnParams.serviceType = AylaNetworks.AML_DEVELOPMENT_SERVICE;
        nexTurnParams.deviceCreator = new NexTurnDeviceCreator();
        nexTurnParams.appVersion = getAppVersion();
        // We want to enable LAN mode in this application
        nexTurnParams.enableLANMode = true;
        // We want enhanced logging. Default is AML_LOGGING_LEVEL_INFO;
        nexTurnParams.loggingLevel = AylaNetworks.AML_LOGGING_LEVEL_INFO;

        // Set the SessionManager's registration fields to our own values
        nexTurnParams.registrationEmailSubject = getResources().getString(R.string.registraion_email_subject);

        // For a custom HTML message, set REGISTRATION_EMAIL_TEMPLATE_ID to null and
        // REGISTRATION_EMAIL_BODY_HTML to an HTML string for the email message.
        nexTurnParams.registrationEmailTemplateId = "ayla_confirmation_template_01";

        if (nexTurnParams.registrationEmailTemplateId == null) {
            nexTurnParams.registrationEmailBodyHTML = getResources().getString(R.string.registration_email_body_html);
        } else {
            nexTurnParams.registrationEmailBodyHTML = null;
        }

        /**
         * Parameters for Ayla devkit
         */
        final SessionManager.SessionParameters devkitParams = new SessionManager.SessionParameters(this);
        devkitParams.appId = "aMCA-id";
        devkitParams.appSecret = "aMCA-9097620";
        devkitParams.serviceType = AylaNetworks.AML_DEVELOPMENT_SERVICE;
        devkitParams.deviceCreator = new AgileLinkDeviceCreator();
        devkitParams.appVersion = getAppVersion();

        // TODO: Find out why LAN mode is not working with the EVB
        devkitParams.enableLANMode = true;
        devkitParams.loggingLevel = AylaNetworks.AML_LOGGING_LEVEL_INFO;

        // Set the SessionManager's registration fields to our own values
        devkitParams.registrationEmailSubject = getResources().getString(R.string.registraion_email_subject);

        // For a custom HTML message, set REGISTRATION_EMAIL_TEMPLATE_ID to null and
        // REGISTRATION_EMAIL_BODY_HTML to an HTML string for the email message.
        devkitParams.registrationEmailTemplateId = "ayla_confirmation_template_01";

        if (devkitParams.registrationEmailTemplateId == null) {
            devkitParams.registrationEmailBodyHTML = getResources().getString(R.string.registration_email_body_html);
        } else {
            devkitParams.registrationEmailBodyHTML = null;
        }

        // SessionManager.setParameters(nexTurnParams);
        SessionManager.setParameters(devkitParams);

        // Bring up the login dialog if we're not already logged in
        if (!SessionManager.isLoggedIn()) {
            showLoginDialog();
        }
    }

    private void showLoginDialog() {
        _loginDialog = new SignInDialog();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String savedUsername = settings.getString(SessionManager.PREFS_USERNAME, "");
        String savedPassword = settings.getString(SessionManager.PREFS_PASSWORD, "");

        Bundle args = new Bundle();
        args.putString(SignInDialog.ARG_USERNAME, savedUsername);
        args.putString(SignInDialog.ARG_PASSWORD, savedPassword);
        _loginDialog.setArguments(args);
        _loginDialog.show(getSupportFragmentManager(), "signin");
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 0 && SessionManager.isLoggedIn()) {
            SessionManager.stopSession();
            showLoginDialog();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (SessionManager.deviceManager() != null) {
            SessionManager.deviceManager().stopPolling();
        }

        SessionManager.SessionParameters params = SessionManager.sessionParameters();
        if (params != null && params.enableLANMode) {
            AylaLanMode.pause(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SessionManager.SessionParameters params = SessionManager.sessionParameters();

        if (_loginDialog == null && params != null && params.enableLANMode) {
            AylaLanMode.resume();
        }

        if (SessionManager.deviceManager() != null) {
            SessionManager.deviceManager().startPolling();
        }
    }

    private SignInDialog _loginDialog;

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_logout) {
            SessionManager.stopSession();
            showLoginDialog();
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

            // Click the sign-in button
            Button b = (Button) _loginDialog.getView().findViewById(R.id.buttonSignIn);
            b.callOnClick();
            _loginDialog = null;
        }
    }

    //
    // SignInDialogListener methods
    //
    @Override
    public void signIn(String username, String password) {
        SessionManager.startSession(username, password);
        _loginDialog.dismiss();
        _loginDialog = null;
   }

    private class ErrorMessage {
        @Expose
        String error;
    }

    @Override
    public void signInOAuth(Message msg) {
        if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
            // Make sure we have an auth token. Sometimes we get back "OK" but there is
            // really an error message.
            AylaUser user = AylaSystemUtils.gson.fromJson((String)msg.obj, AylaUser.class);
            if ( user == null || user.getAccessToken() == null || user.getRefreshToken() == null ) {
                ErrorMessage errorMessage = AylaSystemUtils.gson.fromJson((String)msg.obj, ErrorMessage.class);
                if ( errorMessage != null ) {
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
            if ( msg.arg1 == AylaNetworks.AML_ERROR_UNREACHABLE ) {
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
            switch (position) {
                case 0:
                    return AllDevicesFragment.newInstance(AllDevicesFragment.DISPLAY_MODE_ALL);

                case 1:
                    return SettingsFragment.newInstance();

                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            // Show 2 pages (for now).
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.all_devices);
                case 1:
                    return getString(R.string.settings);
                default:
                    return null;
            }
        }
    }
}
