package com.aylanetworks.agilelink.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.aylanetworks.agilelink.R;

/*
* This class is used to enter Hidden Wi-Fi(SSID) details. The security of the WiFi could be Open or
* secured (requires authentication). If the security type
* is Open the password is not needed and hence not displayed. Once user enters the WiFi name and password
* we call addHiddenWifi method on ChooseAPDialog and that method calls the existing method choseAccessPoint.
* */

public class ConnectToHiddenWiFi extends DialogFragment implements TextWatcher {

    private final static String LOG_TAG = "ConnectToHiddenWiFi";
    private EditText _wifiNameEditText;
    private CheckBox _wifiRequiresAuth;
    private EditText _passwordEditText;

    private Button _connectButton;
    private View _passwordContainer;

    public ConnectToHiddenWiFi() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_add_wifi, container);

        _wifiNameEditText = (EditText) view.findViewById(R.id.wifiNameEditText);
        _wifiNameEditText.addTextChangedListener(this);
        _wifiRequiresAuth = (CheckBox) view.findViewById(R.id.wifi_requires_auth);
        _passwordEditText = (EditText) view.findViewById(R.id.passwordEditText);
        _connectButton = (Button) view.findViewById(R.id.button_connect);
        _connectButton.setEnabled(false);
        _passwordContainer = view.findViewById(R.id.password_container);
        _passwordContainer.setVisibility(View.GONE);

        _passwordEditText.setEnabled(false);
        _passwordEditText.addTextChangedListener(this);

        _wifiRequiresAuth.setChecked(false);
        _wifiRequiresAuth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Hide any visible keyboards
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(_passwordEditText.getWindowToken(), 0);

                if (!isChecked) {
                    // No need for the password field
                    _passwordContainer.setVisibility(View.GONE);
                    if (_wifiNameEditText.getText().length() > 0) {
                        _connectButton.setEnabled(true);
                    } else{
                        _connectButton.setEnabled(false);
                    }
                } else {
                     _passwordContainer.setVisibility(View.VISIBLE);
                     _passwordEditText.setText("");
                     _passwordEditText.setEnabled(true);
                     _connectButton.setEnabled(false);
                }

                _passwordContainer.setVisibility(View.VISIBLE);
                _passwordEditText.setText("");
                _passwordEditText.setEnabled(true);
                _connectButton.setEnabled(false);
                Log.d(LOG_TAG, "onCheckedChanged: Wifi requires auth [" + isChecked + "]");
            }
        });


        CheckBox cb = (CheckBox) view.findViewById(R.id.wifi_show_password);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    _passwordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                } else {
                    _passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
            }
        });

        final AddHiddenWiFi listener = (AddHiddenWiFi) getTargetFragment();
        _connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (listener != null) {
                    listener.addHiddenWifi(_wifiNameEditText.getText().toString(), _passwordEditText.getText().toString());
                }
            }
        });

        Button cancelButton = (Button) view.findViewById(R.id.button_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (listener != null) {
                    listener.addHiddenWifi(null, null);
                }
            }
        });

        // getDialog().setTitle("Enter your WiFi Info");
        return view;
    }

    // Text change listener methods
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        //If the user chooses security as Open then make sure that the WiFi name is entered
        if (!_wifiRequiresAuth.isChecked()) {
            _connectButton.setEnabled(_wifiNameEditText.getText().length() > 0);
        } else {
            //If security is other than none make sure they enter both wifiname and password
            _connectButton.setEnabled(_wifiNameEditText.getText().length() > 0 && _passwordEditText.getText().length() > 0);
        }
    }

    public interface AddHiddenWiFi {
        void addHiddenWifi(String accessPoint, String password);
    }

}

