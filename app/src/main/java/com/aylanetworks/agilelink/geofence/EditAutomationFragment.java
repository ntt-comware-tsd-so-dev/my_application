package com.aylanetworks.agilelink.geofence;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.geofence.ALAutomation;
import com.aylanetworks.agilelink.framework.geofence.ALGeofenceLocation;
import com.aylanetworks.agilelink.framework.geofence.ALLocationManager;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;

import java.util.HashMap;
import java.util.Map;

/*
 * AMAP_Android
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */

public class EditAutomationFragment extends Fragment {
    private final static String LOG_TAG = "EditAutomationFragment";
    private final static String OBJ_KEY = "automation_obj";

    private EditText _automatioNameEditText;
    private Switch _geofenceSwitch;
    private Spinner _locationNameSpinner;
    private ALAutomation _alAutomation;
    private String _locationName;
    private String _triggerID;
    private Map<String,String> _triggerIDMap;

    public EditAutomationFragment() {
        // Required empty public constructor
    }

    public static EditAutomationFragment newInstance(ALAutomation alAutomation) {
        EditAutomationFragment fragment = new EditAutomationFragment();

        if (alAutomation != null) {
            Bundle args = new Bundle();
            args.putSerializable(OBJ_KEY, alAutomation);
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View root = inflater.inflate(R.layout.edit_automation, container, false);
        _geofenceSwitch = (Switch) root.findViewById(R.id.toggle_switch);
        _geofenceSwitch.setChecked(true);
        _locationNameSpinner = (Spinner)root.findViewById(R.id.geofence_location_names);
        _automatioNameEditText = (EditText) root.findViewById(R.id.automation_name);
        Button saveActionButton = (Button) root.findViewById(R.id.button_action_save);
        if (getArguments() != null) {

            _alAutomation = (ALAutomation) getArguments().getSerializable(OBJ_KEY);
            if (_alAutomation != null) {
                String automatioName = _alAutomation.getName();
                String triggerType = _alAutomation.getAutomationTriggerType().stringValue();
                _triggerID = _alAutomation.getTriggerUUID();
                _automatioNameEditText.setText(automatioName);
                saveActionButton.setText(R.string.update);

                if (triggerType.equals(ALAutomation.ALAutomationTriggerType
                        .TriggerTypeGeofenceEnter.stringValue())) {
                    _geofenceSwitch.setChecked(true);
                } else {
                    _geofenceSwitch.setChecked(false);
                }
            }
        }
        Runnable r = new Runnable() {
            @Override
            public void run() {
                ALLocationManager.fetchGeofenceLocations(new Response.Listener<ALGeofenceLocation[]>() {
                    @Override
                    public void onResponse(ALGeofenceLocation[] arrayAlAction) {
                        String locationNames[] = new String[arrayAlAction.length];
                        String selectedLocation = null;
                        int index = 0;
                        _triggerIDMap = new HashMap<>();
                        for (ALGeofenceLocation alGeofenceLocation : arrayAlAction) {
                            locationNames[index] = alGeofenceLocation.getName();
                            if (_triggerID != null && _triggerID.equals(alGeofenceLocation.getId()
                            )) {
                                selectedLocation = alGeofenceLocation.getName();

                            }
                            _triggerIDMap.put(alGeofenceLocation.getName(),alGeofenceLocation.getId());
                            index++;
                        }

                        ArrayAdapter locationAdapter = new ArrayAdapter<>(getActivity(),
                                android.R.layout.simple_list_item_1, locationNames);
                        _locationNameSpinner.setAdapter(locationAdapter);
                        if (selectedLocation != null) {
                            int spinnerPosition = locationAdapter.getPosition(selectedLocation);
                            _locationNameSpinner.setSelection(spinnerPosition);
                        }
                        _locationNameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                _locationName = (String) _locationNameSpinner.getItemAtPosition
                                        (position);
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {
                            }
                        });
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Log.d(LOG_TAG, error.getMessage());
                    }
                });
            }
        };

        android.os.Handler h = new android.os.Handler();
        h.post(r);

        saveActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String automationName = _automatioNameEditText.getText().toString();

                if (TextUtils.isEmpty(automationName)){
                    String msg = "Automation Name is empty";
                    Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();
                   return;
                }
                String triggerId = _triggerIDMap.get(_locationName);
                if (TextUtils.isEmpty(triggerId)){
                    String msg = "Unknown Geofence Location";
                    Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();
                    return;
                }

                ALAutomation.ALAutomationTriggerType triggerType = ALAutomation.
                        ALAutomationTriggerType.TriggerTypeGeofenceEnter;
                boolean bSwitchValue = _geofenceSwitch.isChecked();
                if(!bSwitchValue){
                    triggerType = ALAutomation.ALAutomationTriggerType.TriggerTypeGeofenceExit;
                }

                AutomationActionsFragment frag = AutomationActionsFragment.newInstance
                        (_alAutomation,automationName,triggerId,triggerType);
                MainActivity.getInstance().pushFragment(frag);
            }
        });
        return root;
    }
}
