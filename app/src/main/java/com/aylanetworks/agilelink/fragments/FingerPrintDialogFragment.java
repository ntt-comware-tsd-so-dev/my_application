/*
 * AMAP_Android
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */
package com.aylanetworks.agilelink.fragments;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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
    private AlertDialog _alertDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        setRetainInstance(true);
        //setStyle(R.style.PageHeader, android.R.style.Theme_NoTitleBar_Fullscreen);
        setStyle(R.style.PageHeader, R.style.FingerPrintDialog);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().setTitle(getString(R.string.signIn));
        View v = inflater.inflate(R.layout.fingerprint_dialog_content, container, false);
        FingerprintManager fingerprintManager =
                (FingerprintManager) getActivity().getSystemService(FINGERPRINT_SERVICE);
        FingerprintUiHelper.FingerprintUiHelperBuilder _fingerprintUiHelperBuilder = new
                FingerprintUiHelper.FingerprintUiHelperBuilder(fingerprintManager);
        _fingerprintUiHelper = _fingerprintUiHelperBuilder.build(
                (ImageView) v.findViewById(R.id.fingerprint_icon),
                (TextView) v.findViewById(R.id.fingerprint_status), this);
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
        if (_alertDialog != null) {
            _alertDialog.dismiss();
        }
        dismiss();
    }

    @Override
    public void onError() {
        if (_alertDialog != null && _alertDialog.isShowing()) {
            return;
        }
        String body = MainActivity.getInstance().getString(R.string.fingerprint_retry_authentication);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.getInstance())
                .setTitle(R.string.attention)
                .setMessage(body)
                .setPositiveButton(android.R.string.yes, null)
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        _mainActivity.showLoginDialog(true);
                        dismiss();
                    }
                });

        _alertDialog = builder.create();
        _alertDialog.show();
    }

    /**
     * Sets the crypto object to be passed in when authenticating with fingerprint.
     */
    public void setCryptoObject(FingerprintManager.CryptoObject cryptoObject) {
        _cryptoObject = cryptoObject;
    }
}
