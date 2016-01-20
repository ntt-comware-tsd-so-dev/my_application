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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;

import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.agilelink.R;

/*
* This class is used to enter Hidden Wi-Fi(SSID) details. The security of the WiFi could be Open or one
* of the following WEP,WPA2,WPA,WPA_EAP,IEEE8021. If the security type
* is Open the password is not needed and hence not displayed. Once user enters the WiFi name and password
* we call addHiddenWifi method on ChooseAPDialog and that method calls the existing method choseAccessPoint.
* */

public class ConnectToHiddenWiFi extends DialogFragment implements AdapterView.OnItemSelectedListener, TextWatcher {

    static final String[] SECURITY_MODES = {AylaNetworks.AML_OPEN, AylaNetworks.AML_WEP, AylaNetworks.AML_WPA, AylaNetworks.AML_WPA2, AylaNetworks.AML_WPA_EAP,AylaNetworks.AML_IEEE8021X};
    private final static String LOG_TAG = "ConnectToHiddenWiFi";
    private EditText _wifiNameEditText;
    private Spinner _spinnerSecurityType;
    private EditText _passwordEditText;

    private Button _cancelButton;
    private Button _connectButton;
    private View _passwordContainer;
    private String _selectedSecurity;

    public ConnectToHiddenWiFi() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_add_wifi, container);

        _wifiNameEditText = (EditText) view.findViewById(R.id.wifiNameEditText);
        _wifiNameEditText.addTextChangedListener(this);
        _spinnerSecurityType = (Spinner) view.findViewById(R.id.spinnerSecurityType);
        _passwordEditText = (EditText) view.findViewById(R.id.passwordEditText);
        _connectButton = (Button) view.findViewById(R.id.button_connect);
        _connectButton.setEnabled(false);
        _passwordContainer = view.findViewById(R.id.password_container);
        _passwordContainer.setVisibility(View.GONE);

        _passwordEditText.setEnabled(false);
        _passwordEditText.addTextChangedListener(this);


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
                    listener.addHiddenWifi(_wifiNameEditText.getText().toString(), _selectedSecurity, _passwordEditText.getText().toString());
                }
            }
        });

        _cancelButton = (Button) view.findViewById(R.id.button_cancel);


        ArrayAdapter securityTypeAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, SECURITY_MODES);
        securityTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        _spinnerSecurityType.setAdapter(securityTypeAdapter);
        _spinnerSecurityType.setOnItemSelectedListener(this);

        _cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (listener != null) {
                    listener.addHiddenWifi(null, null, null);
                }
            }
        });

        // getDialog().setTitle("Enter your WiFi Info");
        return view;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        view.setSelected(true);
        _selectedSecurity = SECURITY_MODES[position];

        // Hide any visible keyboards
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(_passwordEditText.getWindowToken(), 0);

        if (_selectedSecurity.equals(AylaNetworks.AML_OPEN)) {
            // No need for the password field
            _passwordContainer.setVisibility(View.GONE);
            if (_wifiNameEditText.getText().length() > 0) {
                _connectButton.setEnabled(true);
            }
            else{
                _connectButton.setEnabled(false);
            }
        }
        else {
            _passwordContainer.setVisibility(View.VISIBLE);
            _passwordEditText.setText("");
            _passwordEditText.setEnabled(true);
            _connectButton.setEnabled(false);
        }
        Log.d(LOG_TAG, "onItemClick: " + " [" + _selectedSecurity + "]");
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    // Text change listener methods

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        if(_selectedSecurity == null)
            return;
        //If the user chooses security as Open then make sure that the WiFi name is entered
        if (_selectedSecurity.equals(AylaNetworks.AML_OPEN)) {
            _connectButton.setEnabled(_wifiNameEditText.getText().length() > 0);
        } else {
            //If security is other than none make sure they enter both wifiname and password
            _connectButton.setEnabled(_wifiNameEditText.getText().length() > 0 && _passwordEditText.getText().length() > 0);
        }
    }

    public interface AddHiddenWiFi {
        void addHiddenWifi(String accessPoint, String security, String password);
    }

}

