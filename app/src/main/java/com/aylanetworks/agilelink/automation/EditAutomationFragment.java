package com.aylanetworks.agilelink.automation;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.beacon.AMAPBeaconService;
import com.aylanetworks.agilelink.framework.automation.Automation;
import com.aylanetworks.agilelink.framework.automation.AutomationManager;
import com.aylanetworks.agilelink.framework.batch.BatchAction;
import com.aylanetworks.agilelink.framework.batch.BatchManager;
import com.aylanetworks.agilelink.framework.beacon.AMAPBeacon;
import com.aylanetworks.agilelink.framework.beacon.AMAPBeaconManager;
import com.aylanetworks.agilelink.framework.geofence.GeofenceLocation;
import com.aylanetworks.agilelink.framework.geofence.LocationManager;
import com.aylanetworks.agilelink.geofence.GeofenceController;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.error.ServerError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

import static com.aylanetworks.agilelink.framework.automation.Automation.ALAutomationTriggerType.TriggerTypeGeofenceEnter;

/*
 * AMAP_Android
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */

public class EditAutomationFragment extends Fragment {
    private final static String LOG_TAG = "EditAutomationFragment";
    private final static String OBJ_KEY = "automation_obj";


    private EditText _automationNameEditText;
    private Switch _triggerTypeSwitch;
    private Spinner _locationNameSpinner;
    private Spinner _beaconNameSpinner;
    private Automation _automation;
    private String _locationName;
    private String _beaconName;
    private String _triggerID;
    private Map<String, String> _triggerIDMap;
    private View _root;
    private Button _saveActionButton;
    private ImageView _actionAddButton;

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
                if (_automation != null) {
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        _root = inflater.inflate(R.layout.edit_automation, container, false);
        _triggerTypeSwitch = (Switch) _root.findViewById(R.id.toggle_switch);
        _triggerTypeSwitch.setChecked(false);
        _locationNameSpinner = (Spinner) _root.findViewById(R.id.geofence_location_names);
        _beaconNameSpinner = (Spinner) _root.findViewById(R.id.beacon_names);
        _automationNameEditText = (EditText) _root.findViewById(R.id.automation_name);
        Button _cancelButton = (Button) _root.findViewById(R.id.button_action_cancel);
        _cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        final CheckBox cbGeofence = (CheckBox) _root.findViewById(R.id.checkbox_geofences);
        final CheckBox cbBeacons = (CheckBox) _root.findViewById(R.id.checkbox_beacons);
        final View viewGeofences = _root.findViewById(R.id.property_selection_spinner_layout);
        final View viewBeacons = _root.findViewById(R.id.beacon_actions_layout);

        cbGeofence.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    cbBeacons.setChecked(false);
                    viewGeofences.setVisibility(View.VISIBLE);
                    viewBeacons.setVisibility(View.GONE);
                    fetchLocations();
                } else {
                    cbBeacons.setChecked(true);
                    viewGeofences.setVisibility(View.GONE);
                    viewBeacons.setVisibility(View.VISIBLE);
                }
            }
        });

        cbBeacons.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    cbGeofence.setChecked(false);
                    viewGeofences.setVisibility(View.GONE);
                    viewBeacons.setVisibility(View.VISIBLE);
                    fetchBeacons();
                } else {
                    cbGeofence.setChecked(true);
                    viewGeofences.setVisibility(View.VISIBLE);
                    viewBeacons.setVisibility(View.GONE);
                }
            }
        });

        _saveActionButton = (Button) _root.findViewById(R.id.button_action_save);
        _actionAddButton = (ImageView) _root.findViewById(R.id.btn_add_action);
        _saveActionButton.setEnabled(false);
        _actionAddButton.setEnabled(false);

        final TextView emptyActionsView = (TextView) _root.findViewById(R.id.empty_actions);
        if (getArguments() != null) {
            _automation = (Automation) getArguments().getSerializable(OBJ_KEY);
            if (_automation != null) {
                String automationName = _automation.getName();
                _triggerID = _automation.getTriggerUUID();
                if (automationName != null) {
                    _automationNameEditText.setText(automationName);
                }
                if (_automation.getAutomationTriggerType() != null) {
                    switch (_automation.getAutomationTriggerType()) {
                        case TriggerTypeGeofenceEnter:
                            cbGeofence.setChecked(true);
                            _triggerTypeSwitch.setChecked(false);
                            break;
                        case TriggerTypeGeofenceExit:
                            cbGeofence.setChecked(true);
                            _triggerTypeSwitch.setChecked(true);
                            break;
                        case TriggerTypeBeaconEnter:
                            cbBeacons.setChecked(true);
                            _triggerTypeSwitch.setChecked(false);
                            break;
                        case TriggerTypeBeaconExit:
                            cbBeacons.setChecked(true);
                            _triggerTypeSwitch.setChecked(true);
                            break;
                        default:
                            break;
                    }
                }
            }
            final ListView listView = (ListView) _root.findViewById(R.id.actions_list);
            fillActions(listView, emptyActionsView);
        }
        if (cbGeofence.isChecked()) {
            fetchLocations();
            viewBeacons.setVisibility(View.GONE);
        } else {
            fetchBeacons();
            viewGeofences.setVisibility(View.GONE);
        }

        _actionAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (_automation == null) {
                    _automation = createNewAutomation();
                }
                if (_automation != null) {
                    //Call AutomationFragment for user to add the Actions for this fragment
                    AutomationActionsFragment frag = AutomationActionsFragment.newInstance(_automation);
                    MainActivity.getInstance().pushFragment(frag);
                }
            }
        });

        _saveActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAutomation();
            }
        });
        _beaconNameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                _beaconName = (String) _beaconNameSpinner.getItemAtPosition
                        (position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        _locationNameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                _locationName = (String) _locationNameSpinner.getItemAtPosition
                        (position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        return _root;
    }

    private Automation createNewAutomation() {
        String automationName = _automationNameEditText.getText().toString();

        final CheckBox cbGeofence = (CheckBox) _root.findViewById(R.id.checkbox_geofences);

        String triggerUUID = null;
        Automation.ALAutomationTriggerType triggerType = TriggerTypeGeofenceEnter;

        //Check if Geolocation check box is checked
        if (cbGeofence.isChecked() && _locationName != null) {
            triggerUUID = _triggerIDMap.get(_locationName);
            boolean bSwitchValue = _triggerTypeSwitch.isChecked();
            if (bSwitchValue) {
                triggerType = Automation.ALAutomationTriggerType.TriggerTypeGeofenceExit;
            }
        } else if (_beaconName != null) {
            triggerUUID = _triggerIDMap.get(_beaconName);

            boolean bSwitchValue = _triggerTypeSwitch.isChecked();
            if (bSwitchValue) {
                triggerType = Automation.ALAutomationTriggerType.TriggerTypeBeaconExit;
            } else {
                triggerType = Automation.ALAutomationTriggerType.TriggerTypeBeaconEnter;
            }
        }

        final Automation automation = new Automation();
        automation.setName(automationName);

        if (triggerUUID != null) {
            automation.setTriggerUUID(triggerUUID);
        }
        automation.setAutomationTriggerType(triggerType);
        return automation;
    }

    private void fillActions(final ListView listView, final View emptyActionsView) {
        String[] batchActionUUIDS = _automation.getActions();
        if (batchActionUUIDS != null && batchActionUUIDS.length > 0) {
            final ArrayList<String> actionNames = new ArrayList<>();
            final HashSet<String> hashSet = new HashSet(Arrays.asList(batchActionUUIDS));

            BatchManager.fetchBatchActions(new Response.Listener<BatchAction[]>() {
                @Override
                public void onResponse(BatchAction[] arrayAlAction) {
                    for (BatchAction batchAction : arrayAlAction) {
                        if (hashSet.contains(batchAction.getUuid())) {
                            actionNames.add(batchAction.getName());
                        }
                    }
                    if (actionNames.size() > 0) {
                        String[] arrayNames = actionNames.toArray(new String[actionNames.size()]);
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_activated_1, arrayNames);
                        listView.setAdapter(adapter);
                        emptyActionsView.setVisibility(View.GONE);
                    }
                }
            }, new ErrorListener() {
                @Override
                public void onErrorResponse(AylaError error) {
                    Log.d(LOG_TAG, error.getMessage());
                }
            });
        }
    }

    private void fetchLocations() {
        LocationManager.fetchGeofenceLocations(new Response.Listener<GeofenceLocation[]>() {
            @Override
            public void onResponse(GeofenceLocation[] arrayGeofences) {
                List<GeofenceLocation> geofenceLocations = new ArrayList<>(Arrays.asList(arrayGeofences));

                //Now get the list of Geofences that are not added from this phone.
                SharedPreferences prefs = MainActivity.getInstance().getSharedPreferences(GeofenceController.SHARED_PERFS_GEOFENCE, Context.MODE_PRIVATE);
                List<GeofenceLocation> listNotAdded = LocationManager.getGeofencesNotInPrefs(prefs, geofenceLocations);
                if (listNotAdded != null && !listNotAdded.isEmpty()) {
                    geofenceLocations.removeAll(listNotAdded);
                }
                if (geofenceLocations.isEmpty()) {
                    return;
                }

                String locationNames[] = new String[geofenceLocations.size()];
                String selectedLocation = null;
                int index = 0;
                _triggerIDMap = new HashMap<>();
                for (GeofenceLocation alGeofenceLocation : geofenceLocations) {
                    locationNames[index] = alGeofenceLocation.getName();
                    if (_triggerID != null && _triggerID.equals(alGeofenceLocation.getId())) {
                        selectedLocation = alGeofenceLocation.getName();
                    }
                    _triggerIDMap.put(alGeofenceLocation.getName(), alGeofenceLocation.getId());
                    index++;
                }
                //check if selected location is null
                if (selectedLocation == null) {
                    selectedLocation = MainActivity.getInstance().getString(R.string
                            .choose_geofence);
                    String[] newLocationNames = new String[locationNames.length + 1];
                    newLocationNames[0] = selectedLocation;
                    System.arraycopy(locationNames, 0, newLocationNames, 1, locationNames.length);
                    locationNames = newLocationNames;
                }
                ArrayAdapter locationAdapter = new ArrayAdapter<>(getActivity(),
                        android.R.layout.simple_list_item_1, locationNames);
                _locationNameSpinner.setAdapter(locationAdapter);

                int spinnerPosition = locationAdapter.getPosition(selectedLocation);
                _locationNameSpinner.setSelection(spinnerPosition);

                _saveActionButton.setEnabled(true);
                _actionAddButton.setEnabled(true);
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                _saveActionButton.setEnabled(false);
                _actionAddButton.setEnabled(false);
                if (error instanceof ServerError) {
                    ServerError serverError = ((ServerError) error);
                    int code = serverError.getServerResponseCode();
                    if (code == NanoHTTPD.Response.Status.NOT_FOUND.getRequestStatus()) {
                        String errorString = MainActivity.getInstance().getString(R.string
                                .geofences_empty);
                        Toast.makeText(MainActivity.getInstance(), errorString, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                String errorString = MainActivity.getInstance().getString(R.string.Toast_Error) +
                        error.toString();
                Toast.makeText(MainActivity.getInstance(), errorString, Toast.LENGTH_SHORT).show();
                Log.d(LOG_TAG, error.getMessage());
            }
        });

    }

    private void fetchBeacons() {
        AMAPBeaconManager.fetchBeacons(new Response.Listener<AMAPBeacon[]>() {
            @Override
            public void onResponse(AMAPBeacon[] arrayBeacons) {
                String beaconNames[] = new String[arrayBeacons.length];
                _triggerIDMap = new HashMap<>();
                String selectedBeacon = null;
                for (int idx = 0; idx < arrayBeacons.length; idx++) {
                    AMAPBeacon beacon = arrayBeacons[idx];
                    beaconNames[idx] = beacon.getName();
                    if (beacon.getBeaconType().equals(AMAPBeacon.BeaconType.EddyStone) ||
                            beacon.getBeaconType().equals(AMAPBeacon.BeaconType.IBeacon)) {
                        if (_triggerID != null && _triggerID.equals(beacon.getId())) {
                            selectedBeacon = beacon.getName();
                        }
                    } else {
                        Log.e(LOG_TAG, "Unknown Beacon Type" + beacon.getBeaconType());
                        return;
                    }
                    _triggerIDMap.put(beacon.getName(), beacon.getId());
                }
                if (arrayBeacons.length == 0) {
                    return;
                }
                //check if selectedBeacon
                if (selectedBeacon == null) {
                    selectedBeacon = MainActivity.getInstance().getString(R.string
                            .choose_beacon);
                    String[] newBeaconNames = new String[arrayBeacons.length + 1];
                    newBeaconNames[0] = selectedBeacon;
                    System.arraycopy(beaconNames, 0, newBeaconNames, 1, beaconNames.length);
                    beaconNames = newBeaconNames;
                }
                ArrayAdapter beaconAdapter = new ArrayAdapter<>(getActivity(),
                        android.R.layout.simple_list_item_1, beaconNames);
                _beaconNameSpinner.setAdapter(beaconAdapter);

                int spinnerPosition = beaconAdapter.getPosition(selectedBeacon);
                _beaconNameSpinner.setSelection(spinnerPosition);

                _saveActionButton.setEnabled(true);
                _actionAddButton.setEnabled(true);
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                _saveActionButton.setEnabled(false);
                _actionAddButton.setEnabled(false);
                if (error instanceof ServerError) {
                    ServerError serverError = ((ServerError) error);
                    int code = serverError.getServerResponseCode();
                    if (code == NanoHTTPD.Response.Status.NOT_FOUND.getRequestStatus()) {
                        String errorString = MainActivity.getInstance().getString(R.string.beacons_empty);
                        Toast.makeText(MainActivity.getInstance(), errorString, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                String errorString = MainActivity.getInstance().getString(R.string.Toast_Error) +
                        error.toString();
                Toast.makeText(MainActivity.getInstance(), errorString, Toast.LENGTH_SHORT).show();
                Log.d(LOG_TAG, error.getMessage());
            }
        });
    }

    /**
     * Returns a iBeacon Identifier String that has id1 from Beacon Id, id2 from Beacon Major
     * Version and Id3 from Beacon Minor version
     *
     * @param beacon AMAPBeacon object
     * @return iBeacon string of type id1 id2 and id3
     */

    public static String getBeaconString(AMAPBeacon beacon) {
        StringBuilder beaconString = new StringBuilder("id1: ");
        beaconString.append(beacon.getProximityUuid());
        beaconString.append(" id2: ");
        beaconString.append(beacon.getMajorValue());
        beaconString.append(" id3: ");
        beaconString.append(beacon.getMinorValue());
        return beaconString.toString();
    }

    private void saveAutomation() {
        String automationName = _automationNameEditText.getText().toString();

        if (TextUtils.isEmpty(automationName)) {
            String msg = MainActivity.getInstance().getString(R.string
                    .automation_name_empty);
            Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();
            return;
        }
        if (_automation == null || _automation.getActions() == null) {
            String msg = MainActivity.getInstance().getString(R.string
                    .batch_action_empty);
            Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();
            return;
        }
        Automation.ALAutomationTriggerType triggerType = TriggerTypeGeofenceEnter;
        final CheckBox cbGeofence = (CheckBox) _root.findViewById(R.id.checkbox_geofences);
        boolean bSwitchValue = _triggerTypeSwitch.isChecked();
        String triggerUUID;
        if (cbGeofence.isChecked()) {
            if (bSwitchValue) {
                triggerType = Automation.ALAutomationTriggerType.TriggerTypeGeofenceExit;
            }
            if (_locationName == null) {
                String msg = MainActivity.getInstance().getString(R.string
                        .unknown_geo_fence);
                Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();
                return;
            }
            triggerUUID = _triggerIDMap.get(_locationName);
            if (TextUtils.isEmpty(triggerUUID)) {
                String msg = MainActivity.getInstance().getString(R.string
                        .unknown_geo_fence);
                Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            if (bSwitchValue) {
                triggerType = Automation.ALAutomationTriggerType.TriggerTypeBeaconExit;
            } else {
                triggerType = Automation.ALAutomationTriggerType.TriggerTypeBeaconEnter;
            }
            if (_beaconName == null) {
                String msg = MainActivity.getInstance().getString(R.string.unknown_beacon);
                Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();
                return;
            }
            triggerUUID = _triggerIDMap.get(_beaconName);
            if (TextUtils.isEmpty(triggerUUID)) {
                String msg = MainActivity.getInstance().getString(R.string.unknown_beacon);
                Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Log.d(LOG_TAG, "Clicked saveAutomation");
        _automation.setName(automationName);
        _automation.setTriggerUUID(triggerUUID);
        _automation.setAutomationTriggerType(triggerType);

        if (_automation.getId() == null) {//This is a new Automation
            _automation.setId(Automation.randomUUID());
            _automation.setEnabled(true, MainActivity.getInstance()); //For new one always enable it
            AutomationManager.addAutomation(_automation, new Response.Listener<Automation[]>() {
                @Override
                public void onResponse(Automation[] response) {
                    String msg = MainActivity.getInstance().getString(R
                            .string.saved_automation_success);
                    Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();

                    //In case of Beacons update the map so that we track this beacon enter/exit a
                    // region
                    if (!cbGeofence.isChecked()) {
                        //Thie method will start monitoring the region for this Beacon
                        AMAPBeaconService.fetchAndMonitorBeacons();
                    }
                    ArrayList<Automation> automationsList = new ArrayList<>(Arrays.asList(response));
                    AutomationListFragment frag = AutomationListFragment.newInstance(automationsList);
                    MainActivity.getInstance().pushFragment(frag);
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
            AutomationManager.updateAutomation(_automation, new Response.Listener<Automation[]>() {
                @Override
                public void onResponse(Automation[] response) {
                    String msg = MainActivity.getInstance().getString(R
                            .string.saved_automation_success);
                    Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();
                    ArrayList<Automation> automationsList = new ArrayList<>(Arrays.asList(response));
                    AutomationListFragment frag = AutomationListFragment.newInstance(automationsList);
                    MainActivity.getInstance().pushFragment(frag);
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
}