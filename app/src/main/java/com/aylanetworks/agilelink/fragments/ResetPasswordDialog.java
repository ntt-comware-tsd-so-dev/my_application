package com.aylanetworks.agilelink.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaUser;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Logger;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/*
 * ResetPasswordDialog.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 2/11/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class ResetPasswordDialog extends DialogFragment {
    private static final String LOG_TAG = "ResetPasswordDialog";

    private EditText _passwordEditText;
    private EditText _confirmEditText;
    private Button _okButton;
    private Button _cancelButton;
    private ProgressBar _progressBar;
    private String _token;

    public ResetPasswordDialog() {
        // Empty constructor
    }

    public void setToken(String token) {
        _token = token;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_reset_password, container);
        _passwordEditText = (EditText)view.findViewById(R.id.passwordEditText);
        _confirmEditText = (EditText)view.findViewById(R.id.confirmEditText);
        _okButton = (Button)view.findViewById(R.id.ok);
        _cancelButton = (Button)view.findViewById(R.id.cancel);
        _progressBar = (ProgressBar)view.findViewById(R.id.progress);

        _okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String password = _passwordEditText.getText().toString();
                String confirm = _confirmEditText.getText().toString();
                if ( !password.equals(confirm) ) {
                    Toast.makeText(getActivity(), R.string.password_no_match, Toast.LENGTH_LONG).show();
                    return;
                }

                Log.d(LOG_TAG, "Change password to: " + password);
                Map<String, String> params = new HashMap<>();
                params.put("reset_password_token", _token);
                params.put("password", password);
                params.put("password_confirmation", confirm);

                _okButton.setVisibility(View.GONE);
                _cancelButton.setVisibility(View.GONE);
                _progressBar.setVisibility(View.VISIBLE);
                AylaNetworks.sharedInstance().getLoginManager().resetPassword(password,_token,
                        new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                            @Override
                            public void onResponse(AylaAPIRequest.EmptyResponse response) {
                                Toast.makeText(MainActivity.getInstance(), R.string.password_changed, Toast.LENGTH_LONG).show();
                                getFragmentManager().popBackStack();
                            }
                        },
                        new ErrorListener() {
                            @Override
                            public void onErrorResponse(AylaError error) {
                                Toast.makeText(MainActivity.getInstance(), error.toString(), Toast.LENGTH_LONG)
                                        .show();
                            }
                        });
            }
        });

        _cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        getDialog().setTitle(R.string.enter_new_password);
        return view;
    }
}
