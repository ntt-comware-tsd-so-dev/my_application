/*
 * AMAP_Android
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */
package com.aylanetworks.agilelink.fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;


import static android.content.Context.FINGERPRINT_SERVICE;

/**
 * A dialog which uses fingerprint APIs to authenticate the user, and falls back to password
 * authentication if fingerprint is not available.
 */
public class FingerPrintDialogFragment extends DialogFragment
        implements FingerprintUiHelper.Callback {
    private FingerprintManager.CryptoObject _cryptoObject;
    private FingerprintUiHelper _fingerprintUiHelper;
    private MainActivity _mainActivity;
    private int _failedLogins;
    private static final int MAX_FAILED_FINGERPRINT_LOGINS=5;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        setRetainInstance(true);
        //setStyle(R.style.PageHeader, android.R.style.Theme_NoTitleBar_Fullscreen);
        setStyle(R.style.PageHeader, R.style.FingerPrintDialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().setTitle(getString(R.string.signIn));
        View v = inflater.inflate(R.layout.fingerprint_dialog_content, container, false);
        FingerprintManager fingerprintManager;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            fingerprintManager = (FingerprintManager) getActivity().getSystemService(FINGERPRINT_SERVICE);
        } else {
            return null;
        }

        FingerprintUiHelper.FingerprintUiHelperBuilder _fingerprintUiHelperBuilder = new
                FingerprintUiHelper.FingerprintUiHelperBuilder(fingerprintManager);
        _fingerprintUiHelper = _fingerprintUiHelperBuilder.build(
                (ImageView) v.findViewById(R.id.fingerprint_icon),
                (TextView) v.findViewById(R.id.fingerprint_status), this);
        _failedLogins =0;
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        _fingerprintUiHelper.startListening(_cryptoObject);
    }

    @Override
    public void onPause() {
        super.onPause();
        _fingerprintUiHelper.stopListening();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        _mainActivity = (MainActivity) activity;
    }


    @Override
    public void onAuthenticated() {
        // Callback from FingerprintUiHelper. Let the activity know that authentication was
        // successful.
        _mainActivity.checkLoginAndConnectivity();
        _failedLogins =0;
        dismiss();
    }

    @Override
    public void onError() {
        _failedLogins ++;
        if(_failedLogins >= MAX_FAILED_FINGERPRINT_LOGINS) {
            String body = MainActivity.getInstance().getString(R.string.fingerprint_failed_authentication);
            Toast.makeText(MainActivity.getInstance(), body, Toast.LENGTH_LONG).show();
            _mainActivity.showLoginDialog(true);
        }

    }

    /**
     * Sets the crypto object to be passed in when authenticating with fingerprint.
     */
    public void setCryptoObject(FingerprintManager.CryptoObject cryptoObject) {
        _cryptoObject = cryptoObject;
    }
}
