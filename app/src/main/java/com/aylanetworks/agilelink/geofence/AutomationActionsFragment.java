package com.aylanetworks.agilelink.geofence;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.geofence.ALAction;
import com.aylanetworks.agilelink.framework.geofence.ALAutomation;
import com.aylanetworks.agilelink.framework.geofence.ALAutomationManager;
import com.aylanetworks.agilelink.framework.geofence.AylaDeviceActions;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/*
 * AMAP_Android
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */

public class AutomationActionsFragment extends Fragment {
    private final static String LOG_TAG = "ActionsListFragment";
    private final static String OBJ_KEY = "automation_obj";
    private final static String PARAM_AUTOMATION_NAME = "automation_name";
    private final static String PARAM_TRIGGER_UUID = "trigger_uuid";
    private final static String PARAM_TRIGGER_TYPE = "trigger_type";

    private ExpandableListView _expandableListView;
    private final ArrayList<String> _deviceNames = new ArrayList<>();
    private ALAutomation _alAutomation;
    private final ArrayList<Object> _actionItems = new ArrayList<>();
    private Button _saveButton;
    private String _automationName;
    private String _triggerUUID;
    private String _triggerType;

    public static AutomationActionsFragment newInstance(ALAutomation alAutomation, String
            automationName, String triggerID, ALAutomation.ALAutomationTriggerType triggerType) {
        AutomationActionsFragment frag = new AutomationActionsFragment();
        Bundle args = new Bundle();
        if (alAutomation != null) {
            args.putSerializable(OBJ_KEY, alAutomation);
        }
        args.putString(PARAM_AUTOMATION_NAME, automationName);
        args.putString(PARAM_TRIGGER_UUID, triggerID);
        args.putString(PARAM_TRIGGER_TYPE, triggerType.stringValue());
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater _inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = _inflater.inflate(R.layout.fragment_geofence_actions, container, false);
        _saveButton = (Button) rootView.findViewById(R.id.button_save_automation);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        _deviceNames.clear();
        _actionItems.clear();

        _expandableListView = (ExpandableListView) view.findViewById(R.id.expListView);
        if (getArguments() != null) {
            _alAutomation = (ALAutomation) getArguments().getSerializable(OBJ_KEY);
            _automationName = getArguments().getString(PARAM_AUTOMATION_NAME);
            _triggerUUID = getArguments().getString(PARAM_TRIGGER_UUID);
            _triggerType = getArguments().getString(PARAM_TRIGGER_TYPE);
        }
        final List<AylaDevice> deviceList = AMAPCore.sharedInstance().getDeviceManager()
                .getDevices();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                AylaDeviceActions.fetchActions(new Response.Listener<ALAction[]>() {
                    @Override
                    public void onResponse(ALAction[] arrayAlAction) {
                        for (AylaDevice aylaDevice : deviceList) {
                            _deviceNames.add(aylaDevice.getProductName());
                            ArrayList<ALAction> actions = new ArrayList<>();
                            for (ALAction alAction : arrayAlAction) {
                                if (alAction == null) {
                                    continue;
                                }
                                if (aylaDevice.getDsn().equals(alAction.getDSN())) {
                                    actions.add(alAction);
                                }
                            }
                            _actionItems.add(actions);
                        }

                        ALAction[] alActions = null;
                        if (_alAutomation != null) {
                            alActions = _alAutomation.getALActions();
                        }
                        final DeviceActionAdapter adapter = new DeviceActionAdapter(_deviceNames, _actionItems, alActions);
                        adapter.setInflater((LayoutInflater) getActivity().getSystemService
                                (Context.LAYOUT_INFLATER_SERVICE), getActivity());
                        _expandableListView.setAdapter(adapter);

                        _expandableListView.setGroupIndicator(null);
                        _saveButton.setVisibility(View.VISIBLE);
                        _saveButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Log.d(LOG_TAG, "Clicked Save");
                                ALAutomation alAutomation = new ALAutomation();
                                alAutomation.setName(_automationName);
                                if (_alAutomation != null) {
                                    alAutomation.setId(_alAutomation.getId());
                                } else {
                                    alAutomation.setId(UUID.randomUUID().toString());
                                }
                                alAutomation.setTriggerUUID(_triggerUUID);

                                alAutomation.setAutomationTriggerType(ALAutomation
                                        .ALAutomationTriggerType.fromStringValue(_triggerType));

                                ALAction[] alActionsArray = new ALAction[adapter.getCheckedItems().size()];
                                alActionsArray = adapter.getCheckedItems().toArray(alActionsArray);
                                alAutomation.setALActions(alActionsArray);

                                if (_alAutomation == null) {//This is a new Automation
                                    alAutomation.setEnabled(true); //For new one always enable it
                                    ALAutomationManager.addAutomation(alAutomation, new Response
                                            .Listener<AylaAPIRequest
                                            .EmptyResponse>() {
                                        @Override
                                        public void onResponse(AylaAPIRequest.EmptyResponse response) {
                                            String msg = MainActivity.getInstance().getString(R
                                                    .string.saved_success);
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
                                    ALAutomationManager.updateAutomation(alAutomation, new Response.Listener<AylaAPIRequest
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
                                MainActivity.getInstance().popBackstackToRoot();
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
    }

    public class DeviceActionAdapter extends BaseExpandableListAdapter {
        private Activity _activity;
        private final ArrayList<Object> _actionListItems;
        private LayoutInflater _inflater;
        private final ArrayList<String> _deviceListNames;
        private ArrayList<ALAction> _actionsList;
        private final ArrayList<ALAction> _checkedList;
        private final ALAction[] _alActions;

        public DeviceActionAdapter(ArrayList<String> parents, ArrayList<Object> objectArrayList, ALAction[] alActions) {
            this._deviceListNames = parents;
            this._actionListItems = objectArrayList;
            _checkedList = new ArrayList<>();
            _alActions = alActions;
        }

        public void setInflater(LayoutInflater _inflater, Activity _activity) {
            this._inflater = _inflater;
            this._activity = _activity;
        }

        @Override
        public View getChildView(int groupPosition, final int childPosition, boolean isLastChild,
                                 View convertView, ViewGroup parent) {
            _actionsList = (ArrayList<ALAction>) _actionListItems.get(groupPosition);

            CheckBox checkBoxView;

            if (convertView == null) {
                convertView = _inflater.inflate(R.layout.automation_action_row, null);
            }

            checkBoxView = (CheckBox) convertView.findViewById(R.id.checkbox_action);
            checkBoxView.setText(_actionsList.get(childPosition).getName());
            final ALAction alAction = _actionsList.get(childPosition);
            if (_alActions != null) {
                for (ALAction alAction1 : _alActions) {
                    if (alAction1 == null) {
                        continue;
                    }
                    if (alAction1.getId().equals(alAction.getId())) {
                        checkBoxView.setChecked(true);
                    }
                }
            }

            if (checkBoxView.isChecked()) {
                _checkedList.add(alAction);
            }
            checkBoxView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        if (!_checkedList.contains(alAction)) {
                            _checkedList.add(alAction);
                        }
                    } else {
                        if (_checkedList.contains(alAction)) {
                            _checkedList.remove(alAction);
                        }
                    }
                }
            });
            return convertView;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = _inflater.inflate(R.layout.device_action_group, null);
            }
            TextView deviceView = (TextView) convertView.findViewById(R.id.device_name_title);
            String deviceName = _deviceListNames.get(groupPosition);
            deviceView.setText(deviceName);

            //Make the addition of action invisible. This is needed only when we are adding actions
            //in this fragment it is not allowed
            ImageView actionAddButton = (ImageView) convertView.findViewById(R.id.btn_add_action);
            actionAddButton.setVisibility(View.GONE);

            ExpandableListView expandView = (ExpandableListView) parent;
            expandView.expandGroup(groupPosition);
            return convertView;
        }

        @Override
        public Object getChild(int groupPosition, int _actionsListPosition) {
            return null;
        }

        @Override
        public long getChildId(int groupPosition, int _actionsListPosition) {
            return 0;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return ((ArrayList<String>) _actionListItems.get(groupPosition)).size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return null;
        }

        @Override
        public int getGroupCount() {
            return _deviceListNames.size();
        }

        @Override
        public long getGroupId(int groupPosition) {
            return 0;
        }


        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int _actionsListPosition) {
            return false;
        }

        public ArrayList<ALAction> getCheckedItems() {
            return _checkedList;
        }
    }
}
