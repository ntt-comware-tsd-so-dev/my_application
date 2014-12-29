package com.aylanetworks.agilelink;

import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.agilelink.device.ALDeviceClassMap;
import com.aylanetworks.agilelink.framework.SessionManager;

public class MainActivity extends ActionBarActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // Bring up the login dialog
        loginDialog().show();
    }

    public AlertDialog loginDialog() {
        final Context c = this;
        LayoutInflater factory = LayoutInflater.from(c);
        final View textEntryView = factory.inflate(R.layout.login, null);
        final AlertDialog.Builder failAlert = new AlertDialog.Builder(c);
        failAlert.setTitle(R.string.login_failed);
        failAlert.setNegativeButton(R.string.ok, null);

        AlertDialog.Builder alert = new AlertDialog.Builder(c);
        alert.setTitle("Login/ Register");
        alert.setMessage("Please log in");
        alert.setView(textEntryView);
        final EditText usernameInput = (EditText) textEntryView.findViewById(R.id.userNameEditText);
        final EditText passwordInput = (EditText) textEntryView.findViewById(R.id.passwordEditText);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String savedUsername = settings.getString(SessionManager.PREFS_USERNAME, "");
        String savedPassword = settings.getString(SessionManager.PREFS_PASSWORD, "");

        usernameInput.setText(savedUsername);
        passwordInput.setText(savedPassword);

        alert.setPositiveButton("Login", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                // Start the login session
                SessionManager.SessionParameters params = new SessionManager.SessionParameters(c);
                params.username = usernameInput.getText().toString();
                params.password = passwordInput.getText().toString();
                params.appId = "iNextTurnKitDev-id";
                params.appSecret = "iNextTurnKitDev-6124332";
                params.serviceType =  AylaNetworks.AML_DEVELOPMENT_SERVICE;
                params.deviceClassMap = new ALDeviceClassMap();

                // We want enhanced logging. Default is AML_LOGGING_LEVEL_INFO;
                params.loggingLevel = AylaNetworks.AML_LOGGING_LEVEL_ERROR;
                SessionManager.startSession(params);
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        return alert.create();
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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
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
