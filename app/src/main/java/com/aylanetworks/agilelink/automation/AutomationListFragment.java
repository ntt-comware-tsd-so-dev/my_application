package com.aylanetworks.agilelink.automation;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.GenericHelpFragment;
import com.aylanetworks.agilelink.framework.automation.Automation;
import com.aylanetworks.agilelink.framework.automation.AutomationManager;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.error.ServerError;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BleNotAvailableException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;


/*
 * AMAP_Android
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */

public class AutomationListFragment extends Fragment {
    private final static String LOG_TAG = "AutomationListFragment";
    private final static String OBJ_KEY = "automation_obj";

    private ListView _listViewAutomations;
    private ArrayList<Automation> _automationsList;
    private AutomationListAdapter _automationsAdapter;
    private final static int ERROR_NOT_FOUND = 404;
    private static final int MAX_AUTOMATIONS = 10;
    private ImageButton _addButton;
    private AlertDialog _alertDialog;

    public static AutomationListFragment newInstance(Automation automation) {
        AutomationListFragment fragment = new AutomationListFragment();
        if (automation != null) {
            Bundle args = new Bundle();
            args.putSerializable(OBJ_KEY, automation);
            fragment.setArguments(args);
        }
        return fragment;
    }

    public AutomationListFragment() {
        // Required empty public constructor
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_help_automation, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.help_automation:
                showHelpFragment();
                return true;
        }
        return false;
    }

    private void showHelpFragment() {
        String fileURL = MainActivity.getInstance().getString(R.string.automation_help_url);
        MainActivity.getInstance().pushFragment(GenericHelpFragment.newInstance(fileURL));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_all_automations, container, false);
        _listViewAutomations = (ListView) root.findViewById(R.id.listViewAutomations);
        _listViewAutomations.setEmptyView(root.findViewById(R.id.automations_empty));
        _addButton = (ImageButton) root.findViewById(R.id.add_button);
        _addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTapped();
            }
        });
        Automation automation=null;
        if (getArguments() != null) {
            //This is the automation we just updated in Edit Automation Fragment.
            automation = (Automation) getArguments().getSerializable(OBJ_KEY);
        }
        fetchAutomations(automation);
        showOrHideAddButton();
        return root;
    }

    private void fetchAutomations(final Automation automation) {
        MainActivity.getInstance().showWaitDialog(R.string.fetching_automations_title, R.string.fetching_automations_body);
        AutomationManager.fetchAutomation(new Response.Listener<Automation[]>() {
            @Override
            public void onResponse(Automation[] response) {
                MainActivity.getInstance().dismissWaitDialog();
                _automationsList = new ArrayList<>(Arrays.asList(response));
                if(automation != null) {
                    // We need to use this automation that we just updated, the reason is it takes
                    // a few seconds for the update datum field to have the values of automation
                    // that were updated in EditAutomation Fragment.
                    for(Automation automation1:_automationsList) {
                        if(automation1.getId().equals(automation.getId())){
                            automation1 = automation;
                        }
                    }
                }
                showOrHideAddButton();
                checkPermissions();
                if (isAdded()) {
                    if (_automationsAdapter == null) {
                        _automationsAdapter = new AutomationListAdapter(getContext(), _automationsList);
                        _listViewAutomations.setAdapter(_automationsAdapter);
                    } else {
                        _automationsAdapter.clear();
                        _automationsAdapter.addAll(_automationsList);
                        _listViewAutomations.setAdapter(_automationsAdapter);
                        _automationsAdapter.notifyDataSetChanged();
                    }
                }
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                MainActivity.getInstance().dismissWaitDialog();
                if (error instanceof ServerError) {
                    //Check if there are no existing automations. This is not an actual error and we
                    //don't want to show this error.
                    ServerError serverError = ((ServerError) error);
                    int code = serverError.getServerResponseCode();
                    if(code == ERROR_NOT_FOUND) {
                        Log.d(LOG_TAG, "No Existing Automations");
                        return;
                    }
                }
                String errorString = MainActivity.getInstance().getString(R.string.Toast_Error) +
                        error.toString();
                Toast.makeText(MainActivity.getInstance(), errorString, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addTapped() {
        Log.d(LOG_TAG, "Add button tapped");
        EditAutomationFragment frag = EditAutomationFragment.newInstance(null);
        MainActivity.getInstance().pushFragment(frag);
    }


    public class AutomationListAdapter extends ArrayAdapter<Automation> {
        public AutomationListAdapter(Context context, ArrayList<Automation> automations) {
            super(context, R.layout.automation_list, automations);
        }

        @NonNull
        public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.automation_list, parent, false);
            }

            final Automation automation = getItem(position);
            TextView tv1 = (TextView) convertView.findViewById(R.id.listItemAutomation);
            Switch enabledSwitch = (Switch) convertView.findViewById(R.id.toggle_switch);
            if(automation == null) {
               return  convertView;
            }
            tv1.setText(automation.getName());
            enabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                    if(automation.isEnabled(MainActivity.getInstance()) == isChecked) {
                        return;
                    }
                    automation.setEnabled(isChecked,MainActivity.getInstance());
                    AutomationManager.updateAutomation(automation, new Response.Listener<AylaAPIRequest
                            .EmptyResponse>() {
                        @Override
                        public void onResponse(AylaAPIRequest.EmptyResponse response) {
                            String msg = MainActivity.getInstance().getString(R
                                    .string.updated_success);
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
            });
            enabledSwitch.setChecked(automation.isEnabled(MainActivity.getInstance()));

            convertView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    EditAutomationFragment frag = EditAutomationFragment.newInstance(automation);
                    MainActivity.getInstance().pushFragment(frag);
                }
            });
            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    confirmRemoveAutomation(automation);
                    return true;
                }
            });
            return convertView;
        }
        private void confirmRemoveAutomation(final Automation automation) {
            new AlertDialog.Builder(getActivity())
                    .setIcon(R.drawable.ic_launcher)
                    .setMessage(MainActivity.getInstance().getString(R.string
                            .confirm_remove_automation))
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteAutomation(automation);
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        }

        private void deleteAutomation(final Automation automation) {
            MainActivity.getInstance().showWaitDialog(null, null);
            AutomationManager.deleteAutomation(automation, new Response.Listener<AylaAPIRequest
                    .EmptyResponse>() {
                @Override
                public void onResponse(AylaAPIRequest.EmptyResponse response) {
                    String msg = MainActivity.getInstance().getString(R.string.deleted_success);
                    Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();
                    AutomationManager.fetchAutomation(new Response.Listener<Automation[]>() {
                        @Override
                        public void onResponse(Automation[] response) {
                            _automationsList = new ArrayList<>(Arrays.asList(response));
                            _automationsAdapter.clear();
                            _automationsAdapter.addAll(_automationsList);
                            _listViewAutomations.setAdapter(_automationsAdapter);
                            _automationsAdapter.notifyDataSetChanged();
                            showOrHideAddButton();
                            MainActivity.getInstance().dismissWaitDialog();
                        }
                    }, new ErrorListener() {
                        @Override
                        public void onErrorResponse(AylaError error) {
                            if (error instanceof ServerError) {
                                //Check if there are no existing automations. This is not an actual error and we
                                //don't want to show this error.
                                ServerError serverError = ((ServerError) error);
                                int code = serverError.getServerResponseCode();
                                if(code == ERROR_NOT_FOUND) {
                                    Log.d(LOG_TAG, "No Existing Automations");
                                    return;
                                }
                            }
                            String errorString = MainActivity.getInstance().getString(R.string.Toast_Error) +
                                    error.toString();
                            Toast.makeText(MainActivity.getInstance(), errorString, Toast.LENGTH_SHORT).show();
                            MainActivity.getInstance().dismissWaitDialog();
                        }
                    });
                }
            }, new ErrorListener() {
                @Override
                public void onErrorResponse(AylaError error) {
                    String errorString = MainActivity.getInstance().getString(R.string.Toast_Error) +
                            error.toString();
                    Toast.makeText(MainActivity.getInstance(), errorString, Toast.LENGTH_LONG).show();
                    MainActivity.getInstance().dismissWaitDialog();
                    MainActivity.getInstance().popBackstackToRoot();
                }
            });
        }
    }

    /**
     * Hide Add Button if we reach MAX_AUTOMATIONS. Show it if it is less than MAX_AUTOMATIONS
     */
    private void showOrHideAddButton() {
        if(_automationsList !=null && _automationsList.size() >= MAX_AUTOMATIONS){
            _addButton.setVisibility(View.GONE);
        } else {
            _addButton.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Check for Location service permission and in case the user has beacons then check for BLE
     * Permissions
     */
    private void checkPermissions() {
        //First check if we have Location service permissions. Needed for both geofences and beacons
        if(checkLocationServices(MainActivity.getInstance())) {
            // Now check if the automation list has beacon trigger enter or exit in it. We don't
            // need to check for BLE Permissions if user has only geofences in their automations
            if(_automationsList != null && !_automationsList.isEmpty()) {

                Automation.ALAutomationTriggerType typeBeaconEnter =Automation.ALAutomationTriggerType
                        .TriggerTypeBeaconEnter;
                Automation.ALAutomationTriggerType typeBeaconExit =Automation.ALAutomationTriggerType
                        .TriggerTypeBeaconExit;

                for(Automation automation:_automationsList){
                    Automation.ALAutomationTriggerType triggerType = automation
                            .getAutomationTriggerType();
                    if(typeBeaconEnter.equals(triggerType) || typeBeaconExit.equals(triggerType)) {
                        checkBluetoothEnabled();
                        break;
                    }
                }

            }
        }
    }
    private void checkBluetoothEnabled() {
        try {
            if (!BeaconManager.getInstanceForApplication(MainActivity.getInstance()).checkAvailability()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.getInstance());
                builder.setTitle(getString(R.string.bluetooth_title));
                builder.setMessage(getString(R.string.bluetooth_dialog_summary));
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        MainActivity.getInstance().popBackstackToRoot();
                    }
                });
                builder.show();
            }
        }
        catch (BleNotAvailableException e) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.getInstance());
            builder.setTitle(getString(R.string.bluetooth_not_available_title));
            builder.setMessage(getString(R.string.bluetooth_not_available_summary));
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    MainActivity.getInstance().popBackstackToRoot();
                }

            });
            builder.show();

        }
    }
    private boolean checkLocationServices(final Context context) {
        android.location.LocationManager lm = (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled;
        boolean network_enabled;

        gps_enabled = lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
        network_enabled = lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER);

        if (!gps_enabled && !network_enabled) {
            // notify user
            if(_alertDialog == null) {
                _alertDialog = getAlertDialog(context);
            }
            if(!_alertDialog.isShowing()) {
                _alertDialog.show();
            }
        } else {
            if (ActivityCompat.checkSelfPermission(MainActivity.getInstance(), Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.getInstance(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MainActivity.REQUEST_FINE_LOCATION);

                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.getInstance().popBackstackToRoot();
                    }
                };
                Handler h = new Handler();
                int POST_DELAYED_TIME_MS = 30;
                h.postDelayed(r, POST_DELAYED_TIME_MS);
            }
            else {
                return true;
            }
        }
        return false;
    }

    private AlertDialog getAlertDialog(final Context context) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setMessage(context.getResources().getString(R.string.gps_network_not_enabled));
        dialog.setPositiveButton(context.getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                context.startActivity(myIntent);
            }
        });
        dialog.setNegativeButton(context.getString(android.R.string.cancel), new DialogInterface
                .OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                Toast.makeText(context, context.getString(R.string.location_permission_required_toast), Toast.LENGTH_SHORT).show();
            }
        });
        return dialog.create();
    }

}


