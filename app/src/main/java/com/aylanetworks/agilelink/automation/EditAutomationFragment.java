package com.aylanetworks.agilelink.automation;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
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
import android.widget.Switch;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.automation.Automation;
import com.aylanetworks.agilelink.framework.automation.AutomationManager;
import com.aylanetworks.agilelink.framework.geofence.GeofenceLocation;
import com.aylanetworks.agilelink.framework.geofence.LocationManager;
import com.aylanetworks.aylasdk.AylaAPIRequest;
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
    private Automation _automation;
    private String _locationName;
    private String _triggerID;
    private Map<String,String> _triggerIDMap;

    public EditAutomationFragment() {
        // Required empty public constructor
    }

    public static EditAutomationFragment newInstance(Automation automation) {
        EditAutomationFragment fragment = new EditAutomationFragment();

        if (automation != null) {
            Bundle args = new Bundle();
            args.putSerializable(OBJ_KEY, automation);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_automation, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_automation:
                if(_automation != null) {
                    confirmRemoveAutomation();
                }
                return true;
        }
        return false;
    }

    private void confirmRemoveAutomation() {
        new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_launcher)
                .setMessage(MainActivity.getInstance().getString(R.string
                        .confirm_remove_automation))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteAutomation();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void deleteAutomation() {
        //confirm_delete_automation
        AutomationManager.deleteAutomation(_automation, new Response.Listener<AylaAPIRequest
                .EmptyResponse>() {
            @Override
            public void onResponse(AylaAPIRequest.EmptyResponse response) {
                String msg = MainActivity.getInstance().getString(R.string.deleted_success);
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
            _automation = (Automation) getArguments().getSerializable(OBJ_KEY);
            if (_automation != null) {
                String automationName = _automation.getName();
                String triggerType = _automation.getAutomationTriggerType().stringValue();
                _triggerID = _automation.getTriggerUUID();
                _automatioNameEditText.setText(automationName);
                saveActionButton.setText(R.string.update);

                if (triggerType.equals(Automation.ALAutomationTriggerType
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
                LocationManager.fetchGeofenceLocations(new Response.Listener<GeofenceLocation[]>() {
                    @Override
                    public void onResponse(GeofenceLocation[] arrayAlAction) {
                        String locationNames[] = new String[arrayAlAction.length];
                        String selectedLocation = null;
                        int index = 0;
                        _triggerIDMap = new HashMap<>();
                        for (GeofenceLocation alGeofenceLocation : arrayAlAction) {
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
                    String msg = MainActivity.getInstance().getString(R.string
                            .automation_name_empty);
                    Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();
                   return;
                }
                String triggerId = _triggerIDMap.get(_locationName);
                if (TextUtils.isEmpty(triggerId)){
                    String msg = MainActivity.getInstance().getString(R.string
                            .unknown_geo_fence);
                    Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();
                    return;
                }

                Automation.ALAutomationTriggerType triggerType = Automation.
                        ALAutomationTriggerType.TriggerTypeGeofenceEnter;
                boolean bSwitchValue = _geofenceSwitch.isChecked();
                if(!bSwitchValue){
                    triggerType = Automation.ALAutomationTriggerType.TriggerTypeGeofenceExit;
                }

                AutomationActionsFragment frag = AutomationActionsFragment.newInstance
                        (_automation,automationName,triggerId,triggerType);
                MainActivity.getInstance().pushFragment(frag);
            }
        });
        return root;
    }
}
