package com.aylanetworks.agilelink.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.SessionManager;

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
                AylaUser.resetPasswordWithToken(_resetHandler, params);
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

    static class ResetHandler extends Handler {
        private WeakReference<ResetPasswordDialog> _resetPasswordDialog;
        public ResetHandler(ResetPasswordDialog resetPasswordDialog) {
            _resetPasswordDialog = new WeakReference<ResetPasswordDialog>(resetPasswordDialog);
        }

        @Override
        public void handleMessage(Message msg) {
            if ( AylaNetworks.succeeded(msg) ) {
                Toast.makeText(_resetPasswordDialog.get().getActivity(), R.string.password_changed, Toast.LENGTH_LONG).show();
            } else {
                if ( msg.arg1 == 422 ) {
                    Toast.makeText(_resetPasswordDialog.get().getActivity(), R.string.error_invalid_token, Toast.LENGTH_LONG).show();
                } else {
                    String json = (String)msg.obj;
                    try {
                        JSONObject obj = new JSONObject(json);
                        String message = obj.getString("password");
                        if ( message != null ) {
                            Toast.makeText(_resetPasswordDialog.get().getActivity(), message, Toast.LENGTH_LONG);
                            _resetPasswordDialog.get().dismiss();
                            return;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Toast.makeText(_resetPasswordDialog.get().getActivity(), R.string.unknown_error, Toast.LENGTH_LONG).show();
                }
            }
            _resetPasswordDialog.get().dismiss();
        }
    }
    private ResetHandler _resetHandler = new ResetHandler(this);
}
