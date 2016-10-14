package com.aylanetworks.agilelink;
/*
 * AMAP_Android
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */

import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.aylanetworks.agilelink.fragments.FingerprintUiHelper;
import com.aylanetworks.agilelink.framework.AMAPCore;

public class FingerPrintSettingsActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new SettingsFragment()).commit();
    }

    /**
     * Fragment for settings.
     */
    public static class SettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            final CheckBoxPreference fingerprintPreference = (CheckBoxPreference) getPreferenceManager()
                    .findPreference(getString(R.string.use_fingerprint_to_authenticate));
            final  CheckBoxPreference expireTokenPreference = (CheckBoxPreference) getPreferenceManager()
                    .findPreference(getString(R.string.always_expire_auth_token));
            //The finger print is available only on Android devices M and up.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                getPreferenceScreen().removePreference(fingerprintPreference);
            }
            fingerprintPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean checked = Boolean.valueOf(newValue.toString());
                    if (checked) {
                        //Make sure the device has finger print available
                        if (!FingerprintUiHelper.isFingerprintAuthAvailable()) {
                            String errString = MainActivity.getInstance().getString(R.string.fingerprint_not_available);
                            Toast.makeText(AMAPCore.sharedInstance().getContext(), errString,
                                    Toast.LENGTH_LONG).show();
                            return false;
                        }
                        //If fingerprint is checked make expire token as unchecked
                        expireTokenPreference.setChecked(false);
                    }
                    return true;
                }
            });

            expireTokenPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean checked = Boolean.valueOf(newValue.toString());
                    if (checked) {
                        //The finger print is available only on Android devices M and up.
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            //If expire token is checked make fingerprint as unchecked
                            fingerprintPreference.setChecked(false);
                        }
                    }
                    return true;
                }
            });
        }

    }

}

