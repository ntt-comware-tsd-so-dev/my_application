package com.aylanetworks.agilelink;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaLanMode;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.device.devkit.DevkitDeviceCreator;
import com.aylanetworks.agilelink.device.zigbee.NexTurnDeviceCreator;
import com.aylanetworks.agilelink.fragments.AllDevicesFragment;
import com.aylanetworks.agilelink.fragments.SignUpDialog;
import com.aylanetworks.agilelink.framework.SessionManager;

public class MainActivity extends ActionBarActivity implements SignUpDialog.SignUpListener {

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
        if ( _progressDialog != null ) {
            dismissWaitDialog();
        }

        if ( title == null ) {
            title = getResources().getString(R.string.please_wait);
        }

        _progressDialog = ProgressDialog.show(this, title, message, true);
    }

    public void dismissWaitDialog() {
        if ( _progressDialog != null ) {
            _progressDialog.dismiss();
            _progressDialog = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        _theInstance = this;

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
        nexTurnParams.serviceType =  AylaNetworks.AML_DEVELOPMENT_SERVICE;
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

        if ( nexTurnParams.registrationEmailTemplateId == null ) {
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
        devkitParams.deviceCreator = new DevkitDeviceCreator();
        devkitParams.appVersion = getAppVersion();

        // TODO: Find out why LAN mode is not working with the EVB
        devkitParams.enableLANMode = true;
        devkitParams.loggingLevel = AylaNetworks.AML_LOGGING_LEVEL_INFO;

        // Set the SessionManager's registration fields to our own values
        devkitParams.registrationEmailSubject = getResources().getString(R.string.registraion_email_subject);

        // For a custom HTML message, set REGISTRATION_EMAIL_TEMPLATE_ID to null and
        // REGISTRATION_EMAIL_BODY_HTML to an HTML string for the email message.
        devkitParams.registrationEmailTemplateId = "ayla_confirmation_template_01";

        if ( devkitParams.registrationEmailTemplateId == null ) {
            devkitParams.registrationEmailBodyHTML = getResources().getString(R.string.registration_email_body_html);
        } else {
            devkitParams.registrationEmailBodyHTML = null;
        }

        SessionManager.setParameters(nexTurnParams);
        // SessionManager.setParameters(devkitParams);

        // Bring up the login dialog if we're not already logged in
        if ( !SessionManager.isLoggedIn() ) {
            loginDialog().show();
        }
    }

    @Override
    public void onBackPressed() {
        if ( getSupportFragmentManager().getBackStackEntryCount() == 0 && SessionManager.isLoggedIn() ) {
            SessionManager.stopSession();
            loginDialog().show();
        } else {
            super.onBackPressed();
         }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if ( SessionManager.deviceManager() != null ) {
            SessionManager.deviceManager().stopPolling();
        }

        SessionManager.SessionParameters params = SessionManager.sessionParameters();
        if ( params != null && params.enableLANMode ) {
            AylaLanMode.pause(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SessionManager.SessionParameters params = SessionManager.sessionParameters();

        if ( params != null &&  params.enableLANMode ) {
            AylaLanMode.resume();
        }

        if ( SessionManager.deviceManager() != null ) {
            SessionManager.deviceManager().startPolling();
        }
    }

    private AlertDialog _loginDialog;

    public AlertDialog loginDialog() {
        final Context c = this;
        LayoutInflater factory = LayoutInflater.from(c);
        final View textEntryView = factory.inflate(R.layout.login, null);
        final AlertDialog.Builder failAlert = new AlertDialog.Builder(c);
        failAlert.setTitle(R.string.login_failed);
        failAlert.setNegativeButton(R.string.ok, null);

        AlertDialog.Builder alert = new AlertDialog.Builder(c);
        alert.setView(textEntryView);
        final EditText usernameInput = (EditText) textEntryView.findViewById(R.id.userNameEditText);
        final EditText passwordInput = (EditText) textEntryView.findViewById(R.id.passwordEditText);
        final Button signInButton = (Button) textEntryView.findViewById(R.id.buttonSignIn);
        final TextView signUpTextView = (TextView)textEntryView.findViewById(R.id.signUpTextView);



        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String savedUsername = settings.getString(SessionManager.PREFS_USERNAME, "");
        String savedPassword = settings.getString(SessionManager.PREFS_PASSWORD, "");

        usernameInput.setText(savedUsername);
        passwordInput.setText(savedPassword);

        // Set up the handler for login click
        signInButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Set up our session parameters and start the session.

                // Use the nexTurn settings
                // SessionManager.SessionParameters params = nexTurnParams;

                String username = usernameInput.getText().toString();
                String password = passwordInput.getText().toString();

                SessionManager.startSession(username, password);
                _loginDialog.dismiss();
                _loginDialog = null;
            }
        });

        alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                // Finish the activity. This will get called if the back button is pressed
                // while the dialog is active.
                finish();
            }
        });

        // Set the handler for a click on "Sign Up"
        signUpTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchSignUpDialog();
            }
        });

        _loginDialog = alert.create();
        return _loginDialog;
    }

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
            loginDialog().show();
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
        if ( _loginDialog != null ) {
            EditText username = (EditText)_loginDialog.findViewById(R.id.userNameEditText);
            EditText password = (EditText)_loginDialog.findViewById(R.id.passwordEditText);
            username.setText(newUser.email);
            password.setText(newUser.password);

            // Click the sign-in button
            Button b = (Button)_loginDialog.findViewById(R.id.buttonSignIn);
            b.callOnClick();
            _loginDialog = null;
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
            // Return a PlaceholderFragment (defined as a static inner class below).
            if ( position == 0 ) {
                return AllDevicesFragment.newInstance(AllDevicesFragment.DISPLAY_MODE_ALL);
            }
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 1 page (for now).
            return 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if ( position == 0 ) {
                return getString(R.string.all_devices);
            }
            return Integer.toString(position);
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

}
