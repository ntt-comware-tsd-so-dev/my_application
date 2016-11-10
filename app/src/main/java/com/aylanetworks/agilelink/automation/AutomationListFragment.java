package com.aylanetworks.agilelink.automation;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
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
import com.aylanetworks.agilelink.framework.automation.Automation;
import com.aylanetworks.agilelink.framework.automation.AutomationManager;
import com.aylanetworks.agilelink.geofence.GeofenceController;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.error.ServerError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
 * AMAP_Android
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */

public class AutomationListFragment extends Fragment {
    private final static String LOG_TAG = "AutomationListFragment";

    private ListView _listViewAutomations;
    private ArrayList<Automation> _automationsList;
    private AutomationListAdapter _automationsAdapter;
    private final static int ERROR_NOT_FOUND = 404;
    private boolean _initialState = false;
    private static final int MAX_AUTOMATIONS = 5;


    public static AutomationListFragment newInstance() {
        return new AutomationListFragment();
    }

    public AutomationListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_all_automations, container, false);
        _listViewAutomations = (ListView) root.findViewById(R.id.listViewAutomations);
        _listViewAutomations.setEmptyView(root.findViewById(R.id.automations_empty));


        ImageButton addButton = (ImageButton) root.findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTapped();
            }
        });
        fetchAutomations();
        if(_automationsList !=null && _automationsList.size() >= MAX_AUTOMATIONS){
            addButton.setVisibility(View.GONE);
        }
        return root;
    }

    private void fetchAutomations() {
        MainActivity.getInstance().showWaitDialog(R.string.fetching_automations_title, R.string.fetching_automations_body);
        AutomationManager.fetchAutomation(new Response.Listener<Automation[]>() {
            @Override
            public void onResponse(Automation[] response) {
                MainActivity.getInstance().dismissWaitDialog();
                ArrayList<Automation> automationsAllList = new ArrayList<>(Arrays.asList(response));

                Set<String> locationIDSet = getSavedLocations();
                _automationsList = new ArrayList<>();
                for (Automation automation : automationsAllList) {
                    if (locationIDSet.contains(automation.getTriggerUUID().toUpperCase())) {
                        _automationsList.add(automation);
                    }
                }

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
    private Set<String> getSavedLocations() {
        SharedPreferences prefs =MainActivity.getInstance().getSharedPreferences(GeofenceController.SHARED_PERFS_GEOFENCE,
                Context.MODE_PRIVATE);
        Map<String, ?> keys = prefs.getAll();
        Set<String> locationIDSet =  new HashSet<>();
        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            locationIDSet.add(entry.getKey().toUpperCase());
        }
        return locationIDSet;
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
            _initialState = automation.isEnabled();
            enabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                    if(_initialState == isChecked) {
                        return;
                    }
                    automation.setEnabled(isChecked);
                    AutomationManager.updateAutomation(automation, new Response.Listener<AylaAPIRequest
                            .EmptyResponse>() {
                        @Override
                        public void onResponse(AylaAPIRequest.EmptyResponse response) {
                            String msg = MainActivity.getInstance().getString(R
                                    .string.updated_success);
                            Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();
                            _initialState = isChecked;
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
            enabledSwitch.setChecked(_initialState);

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
            //confirm_delete_automation
            AutomationManager.deleteAutomation(automation, new Response.Listener<AylaAPIRequest
                    .EmptyResponse>() {
                @Override
                public void onResponse(AylaAPIRequest.EmptyResponse response) {
                    String msg = MainActivity.getInstance().getString(R.string.deleted_success);
                    Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();
                    _automationsList.remove(automation);
                    _automationsAdapter.notifyDataSetChanged();
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


