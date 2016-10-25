package com.aylanetworks.agilelink.automation;

import android.content.Context;
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
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;

import java.util.ArrayList;
import java.util.Arrays;

/*
 * AMAP_Android
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */

public class AutomationListFragment extends Fragment {
    private final static String LOG_TAG = "AutomationListFragment";

    private ListView _listViewAutomations;
    private Automation[] _Automations;
    private AutomationListAdapter _automationsAdapter;

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
        Runnable r = new Runnable() {
            @Override
            public void run() {
                fetchAutomations();
            }
        };

        android.os.Handler h = new android.os.Handler();
        h.post(r);
        return root;
    }

    private void fetchAutomations() {
        AutomationManager.fetchAutomation(new Response.Listener<Automation[]>() {
            @Override
            public void onResponse(Automation[] response) {
                _Automations = response;
                if (isAdded()) {
                    if (_automationsAdapter == null) {
                        _automationsAdapter = new AutomationListAdapter(getContext(), new ArrayList<>(Arrays.asList(response)));
                        _listViewAutomations.setAdapter(_automationsAdapter);
                    } else {
                        _automationsAdapter.clear();
                        _automationsAdapter.addAll(_Automations);
                        _listViewAutomations.setAdapter(_automationsAdapter);
                        _automationsAdapter.notifyDataSetChanged();
                    }
                }
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
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
            enabledSwitch.setChecked(automation.isEnabled());
            enabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    automation.setEnabled(isChecked);
                    AutomationManager.updateAutomation(automation, new Response.Listener<AylaAPIRequest
                            .EmptyResponse>() {
                        @Override
                        public void onResponse(AylaAPIRequest.EmptyResponse response) {
                            String msg = MainActivity.getInstance().getString(R
                                    .string.updated_success);
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
            });

            convertView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    EditAutomationFragment frag = EditAutomationFragment.newInstance(automation);
                    MainActivity.getInstance().pushFragment(frag);
                }
            });
            return convertView;
        }
    }
}


