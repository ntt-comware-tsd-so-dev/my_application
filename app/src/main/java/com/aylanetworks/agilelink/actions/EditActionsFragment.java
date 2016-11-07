package com.aylanetworks.agilelink.actions;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
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
import com.aylanetworks.agilelink.framework.geofence.Action;
import com.aylanetworks.agilelink.framework.geofence.AylaDeviceActions;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.util.TypeUtils;

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
    private Action _action;

    public EditActionsFragment() {
        // Required empty public constructor
    }

    public static EditActionsFragment newInstance(String dsn, Action action) {
        EditActionsFragment fragment = new EditActionsFragment();
        Bundle args = new Bundle();
        if (action != null) {
            args.putSerializable(OBJ_KEY, action);
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
                if (_actionID != null) {
                    confirmRemoveAction();
                }
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
            _action = (Action) getArguments().getSerializable(OBJ_KEY);
            String dsn = getArguments().getString(ARG_DSN);
            _device = AMAPCore.sharedInstance().getDeviceManager().deviceWithDSN(dsn);
            _deviceModel = AMAPCore.sharedInstance().getSessionParameters().viewModelProvider
                    .viewModelForDevice(_device);
            _isUpdateAction = false;
            if (_action != null) {
                _isUpdateAction = true;
                _actionID = _action.getId();
                String actionName = _action.getName();
                _propertyName = _action.getPropertyName();
                String propertyValue = _action.getValue().toString();
                _actionNameEditText.setText(actionName);
                _actionValueEditText.setText(propertyValue);
            }
        }
        Button _cancelButton = (Button) root.findViewById(R.id.button_action_cancel);
        _cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        Button _saveActionButton = (Button) root.findViewById(R.id.button_action_save);
        _saveActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(TextUtils.isEmpty(_actionNameEditText.getText())){
                    String errorString = "Invalid Name";
                    Toast.makeText(MainActivity.getInstance(), errorString, Toast.LENGTH_LONG).show();
                    return;
                }

                AylaProperty property = _device.getProperty(_propertyName);
                String propValue = _actionValueEditText.getText().toString();
                if(!isValidValue(property,propValue)){
                    String errorString = "Invalid Property Value";
                    Toast.makeText(MainActivity.getInstance(), errorString, Toast.LENGTH_LONG).show();
                    return;
                }
                Object value = TypeUtils.getTypeConvertedValue(property.getBaseType(), propValue);
                if(value == null) {
                    String errorString = "Invalid Property Value";
                    Toast.makeText(MainActivity.getInstance(), errorString, Toast.LENGTH_LONG).show();
                    return;
                }
                final Action action = new Action();
                if (_isUpdateAction) {
                    action.setId(_actionID);
                } else {
                    action.setId(UUID.randomUUID().toString());
                }
                action.setName(_actionNameEditText.getText().toString());

                action.setPropertyName(property.getName());
                action.setDSN(_device.getDsn());
                action.setValue(value);
                if (!_isUpdateAction) {//This is a new Action just call add action
                    AylaDeviceActions.addAction(action, new Response.Listener<AylaAPIRequest
                            .EmptyResponse>() {
                        @Override
                        public void onResponse(AylaAPIRequest.EmptyResponse response) {
                            String msg = MainActivity.getInstance().getString(R.string.saved_success);
                            Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();
                            MainActivity.getInstance().onBackPressed();
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
                    AylaDeviceActions.updateAction(action, new Response.Listener<AylaAPIRequest
                            .EmptyResponse>() {
                        @Override
                        public void onResponse(AylaAPIRequest.EmptyResponse response) {
                            String msg = MainActivity.getInstance().getString(R.string.updated_success);
                            Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();
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
        if(_action == null) {
            String name= (String) _propertyActionSpinner.getItemAtPosition(0);
            _actionNameEditText.setText(name);

        }
        return root;
    }
    private boolean isValidValue(AylaProperty property,String value) {
        if(property == null || value == null) {
            return false;
        }
        if(property.getBaseType().equals("boolean")){
            try {
                int val= Integer.parseInt(value);
                if(val == 0 || val ==1) {
                    return true;
                } else {
                    return false;
                }
            }catch(NumberFormatException ex){
                return false;
            }
        }
        return true;
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
        AylaDeviceActions.deleteAction(_action, new Response.Listener<AylaAPIRequest
                .EmptyResponse>() {
            @Override
            public void onResponse(AylaAPIRequest.EmptyResponse response) {
                String msg = MainActivity.getInstance().getString(R.string.deleted_success);
                Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();
                MainActivity.getInstance().onBackPressed();
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

    private void confirmRemoveAction() {
        new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_launcher)
                .setMessage(MainActivity.getInstance().getString(R.string
                        .confirm_remove_action))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteAction();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

}
