package com.aylanetworks.agilelink.geofence;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.ViewModel;
import com.aylanetworks.agilelink.framework.geofence.ALAction;
import com.aylanetworks.agilelink.framework.geofence.AylaDeviceActions;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/*
 * AMAP_Android
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */

public class EditActionsFragment extends Fragment {
    private final static String OBJ_KEY = "action_obj";
    private final static String ARG_DSN = "dsn";

    private String _actionID;
    private EditText _actionNameEditText;
    private EditText _actionValueEditText;
    private AylaDevice _device;
    private String _propertyName;

    private Spinner _propertyActionSpinner;
    private ViewModel _deviceModel;
    private boolean _isUpdateAction;

    public EditActionsFragment() {
        // Required empty public constructor
    }

    public static EditActionsFragment newInstance(String dsn, ALAction alAction) {
        EditActionsFragment fragment = new EditActionsFragment();
        Bundle args = new Bundle();
        if (alAction != null) {
            args.putSerializable(OBJ_KEY, alAction);
        }
        args.putString(ARG_DSN, dsn);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_edit_geofence_action, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete_action:
                deleteAction();
                return true;
        }

        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View root = inflater.inflate(R.layout.edit_geofence_action, container, false);
        _actionNameEditText = (EditText) root.findViewById(R.id.action_name);
        _actionValueEditText = (EditText) root.findViewById(R.id.action_value_text);
        if (getArguments() != null) {
            ALAction _alAction = (ALAction) getArguments().getSerializable(OBJ_KEY);
            String dsn = getArguments().getString(ARG_DSN);
            _device = AMAPCore.sharedInstance().getDeviceManager().deviceWithDSN(dsn);
            _deviceModel = AMAPCore.sharedInstance().getSessionParameters().viewModelProvider
                    .viewModelForDevice(_device);
            _isUpdateAction = false;
            if (_alAction != null) {
                _isUpdateAction = true;
                _actionID = _alAction.getId();
                String actionName = _alAction.getName();
                _propertyName = _alAction.getAylaProperty().getName();
                String propertyValue = _alAction.getValue();
                _actionNameEditText.setText(actionName);
                _actionValueEditText.setText(propertyValue);
                _actionValueEditText.setText(propertyValue);
            }
        }

        Button _saveActionButton = (Button) root.findViewById(R.id.button_action_save);
        _saveActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ALAction alAction = new ALAction();
                if (_isUpdateAction) {
                    alAction.setId(_actionID);
                } else {
                    alAction.setId(UUID.randomUUID().toString());
                }
                alAction.setName(_actionNameEditText.getText().toString());
                AylaProperty<Integer> property = _device.getProperty(_propertyName);
                alAction.setAylaProperty(property);
                alAction.setDSN(_device.getDsn());
                alAction.setValue(_actionValueEditText.getText().toString());
                if (!_isUpdateAction) {//This is a new Action just call add action
                    AylaDeviceActions.addAction(alAction, new Response.Listener<AylaAPIRequest
                            .EmptyResponse>() {
                        @Override
                        public void onResponse(AylaAPIRequest.EmptyResponse response) {
                            String msg = "Updated Successfully";
                            Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();
                            MainActivity.getInstance().popBackstackToRoot();
                        }
                    }, new ErrorListener() {
                        @Override
                        public void onErrorResponse(AylaError error) {
                            String errorString = MainActivity.getInstance().getString(R.string.Toast_Error) +
                                    error.toString();
                            Toast.makeText(MainActivity.getInstance(), errorString, Toast.LENGTH_LONG).show();
                            MainActivity.getInstance().popBackstackToRoot();
                        }
                    });
                } else {
                    AylaDeviceActions.updateAction(alAction, new Response.Listener<AylaAPIRequest
                            .EmptyResponse>() {
                        @Override
                        public void onResponse(AylaAPIRequest.EmptyResponse response) {
                            String msg = "Saved Successfully";
                            Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();
                            MainActivity.getInstance().popBackstackToRoot();
                        }
                    }, new ErrorListener() {
                        @Override
                        public void onErrorResponse(AylaError error) {
                            String errorString = MainActivity.getInstance().getString(R.string.Toast_Error) +
                                    error.toString();
                            Toast.makeText(MainActivity.getInstance(), errorString, Toast.LENGTH_LONG).show();
                            MainActivity.getInstance().popBackstackToRoot();
                        }
                    });
                }
            }
        });
        _propertyActionSpinner = (Spinner) root.findViewById(R.id.location_spinner);
        setPropertiesForSpinner();
        return root;
    }

    private void setPropertiesForSpinner() {
        List<String> list = new ArrayList<>();
        list.addAll(Arrays.asList(_deviceModel.getNotifiablePropertyNames()));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout
                .simple_spinner_dropdown_item, list);
        _propertyActionSpinner.setAdapter(adapter);
        _propertyActionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                _propertyName = (String) _propertyActionSpinner.getItemAtPosition
                        (position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        if (_propertyName != null) {
            int spinnerPosition = adapter.getPosition(_propertyName);
            _propertyActionSpinner.setSelection(spinnerPosition);
        }
    }

    private void deleteAction() {
        ALAction alAction = new ALAction();
        alAction.setId(_actionID);
        AylaDeviceActions.deleteAction(alAction, new Response.Listener<AylaAPIRequest
                .EmptyResponse>() {
            @Override
            public void onResponse(AylaAPIRequest.EmptyResponse response) {
                String msg = "Deleted Successfully";
                Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();
                MainActivity.getInstance().popBackstackToRoot();
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                String errorString = MainActivity.getInstance().getString(R.string.Toast_Error) +
                        error.toString();
                Toast.makeText(MainActivity.getInstance(), errorString, Toast.LENGTH_LONG).show();
                MainActivity.getInstance().popBackstackToRoot();
            }
        });
    }
}
