package com.aylanetworks.agilelink.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.aylanetworks.aaml.AylaModuleScanResults;
import com.aylanetworks.agilelink.R;

import java.util.ArrayList;
import java.util.List;

/*
 * ChooseAPDialog.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 1/28/2015.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class ChooseAPDialog extends DialogFragment implements AdapterView.OnItemClickListener, TextWatcher, TextView.OnEditorActionListener {

    private final static String LOG_TAG = "ChooseAPDialog";
    private final static String ARG_SSIDS = "ssids";
    private final static String ARG_SECURITY = "security";
    private final static String SECURITY_OPEN = "None";

    /**
     * This interface is called on the calling fragment. If the user cancels, accessPoint
     * and security will be null.
     */
    public interface ChooseAPResults {
        void choseAccessPoint(String accessPoint, String security, String password);
    }

    public static ChooseAPDialog newInstance(AylaModuleScanResults[] scanResults) {
        // Convert scanResults into something parcelable that we can pass as arguments
        ArrayList<String>ssids = new ArrayList<>();
        ArrayList<String>keyMgmt = new ArrayList<>();

        for ( AylaModuleScanResults result : scanResults ) {
            ssids.add(result.ssid);
            keyMgmt.add(result.security);
        }

        ChooseAPDialog d = new ChooseAPDialog();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_SSIDS, ssids);
        args.putStringArrayList(ARG_SECURITY, keyMgmt);
        d.setArguments(args);

        return d;
    }

    private List<String> _ssidList;
    private List<String> _securityList;
    private EditText _passwordEditText;
    private TextView _textView;
    private Button _connectButton;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        View root = inflater.inflate(R.layout.dialog_choose_ap, container, false);
        final ListView listView = (ListView)root.findViewById(R.id.ap_list);

        final ChooseAPResults listener = (ChooseAPResults)getTargetFragment();

        Button b = (Button)root.findViewById(R.id.button_cancel);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if ( listener != null ) {
                    listener.choseAccessPoint(null, null, null);
                }
            }
        });

        _textView = (TextView)root.findViewById(R.id.choose_ap_textview);
        _connectButton = (Button)root.findViewById(R.id.button_connect);
        _connectButton.setEnabled(false);
        _connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "AP selected");
                dismiss();
                if ( listener != null ) {
                    listener.choseAccessPoint(_selectedSSID, _selectedSecurity, _passwordEditText.getText().toString());
                }
            }
        });

        _passwordEditText = (EditText)root.findViewById(R.id.passwordEditText);
        _passwordEditText.setEnabled(false);
        _passwordEditText.addTextChangedListener(this);
        _passwordEditText.setOnEditorActionListener(this);

        // Get our arguments and set up the listview
        Bundle args = getArguments();
        _ssidList = args.getStringArrayList(ARG_SSIDS);
        _securityList = args.getStringArrayList(ARG_SECURITY);
        String[] aps = _ssidList.toArray(new String[_ssidList.size()]);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_activated_1, aps);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

        // If there's only one SSID, select it (after a short delay)
        if ( _ssidList.size() == 1 ) {
            listView.performItemClick(adapter.getView(0, null, null), 0, adapter.getItemId(0));
        }

        return root;
    }

    private String _selectedSSID;
    private String _selectedSecurity;
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        view.setSelected(true);
        _selectedSSID = _ssidList.get(position);
        _selectedSecurity = _securityList.get(position);

        // Hide any visible keyboards
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(_passwordEditText.getWindowToken(), 0);

        if ( _selectedSecurity.equals(SECURITY_OPEN) ) {
            // No need for the password field
            _passwordEditText.setVisibility(View.GONE);
            _connectButton.setEnabled(true);
        } else {
            _passwordEditText.setVisibility(View.VISIBLE);
            _passwordEditText.setText("");
            _passwordEditText.setEnabled(true);
            _connectButton.setEnabled(false);
            String message = getString(R.string.choose_ap_details, _selectedSSID);
            _textView.setText(message);
        }
        Log.d(LOG_TAG, "onItemClick: " + _selectedSSID + " [" + _selectedSecurity + "]");
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
        _connectButton.setEnabled(s.length() > 0);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        _connectButton.callOnClick();
        return true;
    }
}
