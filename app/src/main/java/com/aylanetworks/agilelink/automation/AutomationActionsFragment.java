package com.aylanetworks.agilelink.automation;

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
import com.aylanetworks.agilelink.framework.geofence.Action;
import com.aylanetworks.agilelink.framework.automation.Automation;
import com.aylanetworks.agilelink.framework.automation.AutomationManager;
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
    private Automation _automation;
    private final ArrayList<ArrayList<Action>> _actionItems = new ArrayList<>();
    private Button _saveButton;
    private String _automationName;
    private String _triggerUUID;
    private String _triggerType;
    private DeviceActionAdapter _adapter;

    public static AutomationActionsFragment newInstance(Automation automation, String
            automationName, String triggerID, Automation.ALAutomationTriggerType triggerType) {
        AutomationActionsFragment frag = new AutomationActionsFragment();
        Bundle args = new Bundle();
        if (automation != null) {
            args.putSerializable(OBJ_KEY, automation);
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
            _automation = (Automation) getArguments().getSerializable(OBJ_KEY);
            _automationName = getArguments().getString(PARAM_AUTOMATION_NAME);
            _triggerUUID = getArguments().getString(PARAM_TRIGGER_UUID);
            _triggerType = getArguments().getString(PARAM_TRIGGER_TYPE);
        }


        AylaDeviceActions.fetchActions(new Response.Listener<Action[]>() {
            @Override
            public void onResponse(Action[] arrayAlAction) {
                doFillData(arrayAlAction);
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                Log.d(LOG_TAG, error.getMessage());
            }
        });

        _saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSaveActions();
            }
        });
    }

    private void doSaveActions() {
        Log.d(LOG_TAG, "Clicked Save");
        String[] actionUUIDs = null;
        if (_automation != null) {
            actionUUIDs = _automation.getActions();
        }
        Automation automation = new Automation();
        automation.setName(_automationName);
        if (_automation != null) {
            automation.setId(_automation.getId());
        } else {
            automation.setId(UUID.randomUUID().toString());
        }
        automation.setTriggerUUID(_triggerUUID);
        automation.setEnabled(_automation.isEnabled());

        automation.setAutomationTriggerType(Automation
                .ALAutomationTriggerType.fromStringValue(_triggerType));
        Action[] actionsArray = new Action[_adapter.getCheckedItems().size()];
        actionsArray = _adapter.getCheckedItems().toArray(actionsArray);
        automation.setActions(actionsArray);

        if (_automation == null) {//This is a new Automation
            automation.setEnabled(true); //For new one always enable it
            AutomationManager.addAutomation(automation, new Response
                    .Listener<AylaAPIRequest
                    .EmptyResponse>() {
                @Override
                public void onResponse(AylaAPIRequest.EmptyResponse response) {
                    String msg = MainActivity.getInstance().getString(R
                            .string.saved_success);
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
        } else {
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
        MainActivity.getInstance().popBackstackToRoot();
    }

    private void doFillData(final Action[] arrayAlAction) {
        String[] actionUUIDs = null;
        if (_automation != null) {
            actionUUIDs = _automation.getActions();
        }
        final List<AylaDevice> deviceList = AMAPCore.sharedInstance().getDeviceManager()
                .getDevices();
        for (AylaDevice aylaDevice : deviceList) {
            _deviceNames.add(aylaDevice.getProductName());
            ArrayList<Action> actions = new ArrayList<>();
            for (Action alAction : arrayAlAction) {
                if (alAction == null) {
                    continue;
                }
                if (aylaDevice.getDsn().equals(alAction.getDSN())) {
                    actions.add(alAction);
                }
            }
            _actionItems.add(actions);
        }

        _adapter = new DeviceActionAdapter(_deviceNames, _actionItems, actionUUIDs);
        _adapter.setInflater((LayoutInflater) getActivity().getSystemService
                (Context.LAYOUT_INFLATER_SERVICE), getActivity());
        _expandableListView.setAdapter(_adapter);

        _expandableListView.setGroupIndicator(null);
        _saveButton.setVisibility(View.VISIBLE);
    }


    public class DeviceActionAdapter extends BaseExpandableListAdapter {
        private Activity _activity;
        private final ArrayList<ArrayList<Action>> _actionListItems;
        private LayoutInflater _inflater;
        private final ArrayList<String> _deviceListNames;
        private ArrayList<Action> _actionsList;
        private final ArrayList<Action> _checkedList;
        private final String[] _actionUUIDs;

        public DeviceActionAdapter(ArrayList<String> parents, ArrayList<ArrayList<Action>> objectArrayList, String[] actionUUIDs) {
            this._deviceListNames = parents;
            this._actionListItems = objectArrayList;
            _checkedList = new ArrayList<>();
            _actionUUIDs = actionUUIDs;
        }

        public void setInflater(LayoutInflater _inflater, Activity _activity) {
            this._inflater = _inflater;
            this._activity = _activity;
        }

        @Override
        public View getChildView(int groupPosition, final int childPosition, boolean isLastChild,
                                 View convertView, ViewGroup parent) {
            _actionsList = (ArrayList<Action>) _actionListItems.get(groupPosition);

            CheckBox checkBoxView;

            if (convertView == null) {
                convertView = _inflater.inflate(R.layout.automation_action_row, null);
            }

            checkBoxView = (CheckBox) convertView.findViewById(R.id.checkbox_action);
            checkBoxView.setText(_actionsList.get(childPosition).getName());
            final Action action = _actionsList.get(childPosition);
            if (_actionUUIDs != null) {
                for (String actionID : _actionUUIDs) {
                    if (actionID.equals(action.getId())) {
                        checkBoxView.setChecked(true);
                    }
                }
            }

            if (checkBoxView.isChecked()) {
                _checkedList.add(action);
            }
            checkBoxView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        if (!_checkedList.contains(action)) {
                            _checkedList.add(action);
                        }
                    } else {
                        if (_checkedList.contains(action)) {
                            _checkedList.remove(action);
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
            return _actionListItems.get(groupPosition).size();
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

        public ArrayList<Action> getCheckedItems() {
            return _checkedList;
        }
    }
}
