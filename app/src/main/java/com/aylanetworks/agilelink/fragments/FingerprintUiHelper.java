/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.aylanetworks.agilelink.fragments;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.support.v4.app.ActivityCompat;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;

/**
 * Small helper class to manage text/icon around fingerprint authentication UI.
 */
@TargetApi(Build.VERSION_CODES.M)
public class FingerprintUiHelper extends FingerprintManager.AuthenticationCallback {

    private static final long ERROR_TIMEOUT_MILLIS = 1600;
    private static final long SUCCESS_DELAY_MILLIS = 1300;

    private final FingerprintManager _fingerprintManager;
    private final ImageView _icon;
    private final TextView _errorTextView;
    private final Callback _callBack;
    private CancellationSignal _cancellationSignal;
    private boolean _selfCancelled;


    public static class FingerprintUiHelperBuilder {
        private final FingerprintManager _fingerprintManager;

        public FingerprintUiHelperBuilder(FingerprintManager fingerprintManager) {
            _fingerprintManager = fingerprintManager;
        }

        public FingerprintUiHelper build(ImageView icon, TextView errorTextView, Callback callback) {
            return new FingerprintUiHelper(_fingerprintManager, icon, errorTextView,
                    callback);
        }
    }

    private FingerprintUiHelper(FingerprintManager fingerprintManager,
                                ImageView icon, TextView errorTextView, Callback callback) {
        _fingerprintManager = fingerprintManager;
        _icon = icon;
        _errorTextView = errorTextView;
        _callBack = callback;
    }

    public static boolean isFingerprintAuthAvailable() {
        if (ActivityCompat.checkSelfPermission(MainActivity.getInstance(),
                "android.permission.USE_FINGERPRINT") == PackageManager.PERMISSION_GRANTED) {
            FingerprintManager fingerprintManager = (FingerprintManager) MainActivity
                    .getInstance().getSystemService(Context.FINGERPRINT_SERVICE);
            if(fingerprintManager.isHardwareDetected() && fingerprintManager
                    .hasEnrolledFingerprints() ){
                return true;
            }
        }
        return false;
    }
    public void startListening(FingerprintManager.CryptoObject cryptoObject) {
        if (!isFingerprintAuthAvailable()) {
            return;
        }
        _cancellationSignal = new CancellationSignal();
        _selfCancelled = false;
        if (ActivityCompat.checkSelfPermission(MainActivity.getInstance(),
                "android.permission.USE_FINGERPRINT") != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MainActivity.getInstance(),
                    "Fingerprint authentication permission not enabled",
                    Toast.LENGTH_LONG).show();

            return;
        }

        _fingerprintManager
                .authenticate(cryptoObject, _cancellationSignal, 0 /* flags */, this, null);
        _icon.setImageResource(R.drawable.ic_finger_print);
    }

    public void stopListening() {
        if (_cancellationSignal != null) {
            _selfCancelled = true;
            _cancellationSignal.cancel();
            _cancellationSignal = null;
        }
    }

    @Override
    public void onAuthenticationError(int errMsgId, CharSequence errString) {
        if (!_selfCancelled) {
            showError(errString);
            _icon.postDelayed(new Runnable() {
                @Override
                public void run() {
                    _callBack.onError();
                }
            }, ERROR_TIMEOUT_MILLIS);
        }
    }

    @Override
    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
        showError(helpString);
    }

    @Override
    public void onAuthenticationFailed() {
        showError(_icon.getResources().getString(
                R.string.fingerprint_not_recognized));
    }

    @Override
    public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
        _errorTextView.removeCallbacks(mResetErrorTextRunnable);
        _errorTextView.setTextColor(
                _errorTextView.getResources().getColor(R.color.green_500, null));
        _icon.postDelayed(new Runnable() {
            @Override
            public void run() {
                _callBack.onAuthenticated();
            }
        }, SUCCESS_DELAY_MILLIS);
    }

    private void showError(CharSequence error) {
        _errorTextView.setText(error);
        _errorTextView.setTextColor(
                _errorTextView.getResources().getColor(R.color.red_900, null));
        _errorTextView.removeCallbacks(mResetErrorTextRunnable);
        _errorTextView.postDelayed(mResetErrorTextRunnable, ERROR_TIMEOUT_MILLIS);
    }

    private final Runnable mResetErrorTextRunnable = new Runnable() {
        @Override
        public void run() {
            _errorTextView.setTextColor(
                    _errorTextView.getResources().getColor(R.color.red_900, null));
            _errorTextView.setText(
                    _errorTextView.getResources().getString(R.string.fingerprint_hint));
            _icon.setImageResource(R.drawable.ic_finger_print);
            _callBack.onError();
        }
    };

    public interface Callback {

        void onAuthenticated();

        void onError();
    }
}
