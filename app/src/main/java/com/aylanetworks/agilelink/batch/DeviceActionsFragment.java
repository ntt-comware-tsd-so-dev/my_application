package com.aylanetworks.agilelink.batch;


import android.content.Context;
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
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.actions.EditActionsFragment;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.ViewModel;
import com.aylanetworks.agilelink.framework.automation.Automation;
import com.aylanetworks.agilelink.framework.batch.BatchAction;
import com.aylanetworks.agilelink.framework.batch.BatchManager;
import com.aylanetworks.agilelink.framework.geofence.Action;
import com.aylanetworks.agilelink.framework.geofence.AylaDeviceActions;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaDatapoint;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.error.ServerError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
        
 /*
  * AMAP_Android
  *
  * Copyright 2017 Ayla Networks, all rights reserved
  */

/**
 * DeviceActionsFragment displays list of Device Actions for this User. The DeviceActions are
 * added from the Main Menu and clicking on Device Actions. The user needs to choose list of
 * actions for new Device Action or can update the existing Device Actions for the selected Batch
 * Action.
 */

public class DeviceActionsFragment extends Fragment {
    private final static String LOG_TAG = "DeviceActionsFragment";
    private final static String OBJ_ACTION_KEY = "batch_action_obj";
    private final ArrayList<String> _deviceNames = new ArrayList<>();
    private final ArrayList<ArrayList<Action>> _actionItems = new ArrayList<>();
    private ExpandableListView _expandableListView;
    private BatchAction _batchAction;
    private Button _saveButton;
    private DeviceActionAdapter _adapter;
    private EditText _batchActionEditText;
    private static final int MAX_ACTIONS_PER_DEVICE = 10;
    private final static String INPUT = "input";
    private final Map<String, String> _deviceMap = new HashMap<>();
    private Action[] _actionsArray=null;

    public static DeviceActionsFragment newInstance(BatchAction batchAction) {
        DeviceActionsFragment fragment = new DeviceActionsFragment();
        if (batchAction != null) {
            Bundle args = new Bundle();
            args.putSerializable(OBJ_ACTION_KEY, batchAction);
            fragment.setArguments(args);
        }
        return fragment;
    }

    public static DeviceActionsFragment newInstance() {
        return new DeviceActionsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater _inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = _inflater.inflate(R.layout.group_device_actions, container, false);
        _saveButton = (Button) rootView.findViewById(R.id.button_save_selection);
        _batchActionEditText = (EditText) rootView.findViewById(R.id.batch_action_name);
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        _deviceNames.clear();
        _actionItems.clear();
        _deviceMap.clear();

        _expandableListView = (ExpandableListView) view.findViewById(R.id.expListView);

        if (getArguments() != null) {
            _batchAction = (BatchAction) getArguments().getSerializable(OBJ_ACTION_KEY);
        }
        MainActivity.getInstance().showWaitDialog(null, null);
        AylaDeviceActions.fetchActions(new Response.Listener<Action[]>() {
            @Override
            public void onResponse(Action[] arrayAlAction) {
                MainActivity.getInstance().dismissWaitDialog();
                _actionsArray = arrayAlAction;
                doFillData(arrayAlAction);
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                MainActivity.getInstance().dismissWaitDialog();
                if (error instanceof ServerError) {
                    ServerError serverError = ((ServerError) error);
                    int code = serverError.getServerResponseCode();
                    //Check if there are no existing actions. This is not an actual error and we
                    //don't want to show this error.
                    if (code == NanoHTTPD.Response.Status.NOT_FOUND.getRequestStatus()) {
                        doFillData(null);
                        return;
                    }
                }
                Log.d(LOG_TAG, error.getMessage());
            }
        });

        _saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveBatchAction();
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_batch_action, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.execute_batch_actions:
                executeActions();
                return true;
        }
        return false;
    }

    private void executeActions() {
        ArrayList<String> actionUUIDList = _adapter.getCheckedItems();
        if(actionUUIDList.isEmpty() || _actionsArray == null) {
            String msg =  MainActivity.getInstance().getString(R.string.device_actions_empty_batch);
            Toast.makeText(MainActivity.getInstance(), msg, Toast
                    .LENGTH_LONG).show();
            return;
        }

        ArrayList<Action> listActions = new ArrayList<>();
        for(Action action:_actionsArray) {
            if(actionUUIDList.contains(action.getId())) {
                listActions.add(action);
            }
        }
        for (final Action action : listActions) {
            AylaDevice device = AMAPCore.sharedInstance().getDeviceManager()
                    .deviceWithDSN(action.getDSN());
            if (device == null) {
                continue;
            }
            final AylaProperty entryProperty = device.getProperty(action.getPropertyName());
            if (entryProperty == null) {
                continue;
            }
            Object value = action.getValue();
            entryProperty.createDatapoint(value, null, new Response
                            .Listener<AylaDatapoint>() {
                        @Override
                        public void onResponse(final AylaDatapoint response) {
                            String str = "Property Name:" + entryProperty.getName();
                            str += " value " + action.getValue();
                            Log.d("setGeofenceActions", "OnEnteredExitedGeofences success: " + str);
                        }
                    },
                    new ErrorListener() {
                        @Override
                        public void onErrorResponse(AylaError error) {
                            Toast.makeText(MainActivity.getInstance(), error.getMessage(), Toast
                                    .LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void saveBatchAction() {
        Log.d(LOG_TAG, "Clicked Save");

        String batchActionName = _batchActionEditText.getText().toString();

        if (TextUtils.isEmpty(batchActionName)) {
            String msg = MainActivity.getInstance().getString(R.string.batch_action_name_empty);
            Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();
            return;
        }

        //Check if user selected any device actions
        int selectedSize = _adapter.getCheckedItems().size();
        if (selectedSize == 0) {
            String msg = MainActivity.getInstance().getString(R.string.device_actions_empty_batch);
            Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();
            return;
        }

        if (_batchAction == null) {
            _batchAction = new BatchAction();
        }
        _batchAction.setName(batchActionName);
        ArrayList<String> actionUUIDList = _adapter.getCheckedItems();
        _batchAction.setActionUuids(actionUUIDList.toArray(new String[actionUUIDList.size()]));

        final Response.Listener<AylaAPIRequest.EmptyResponse> successListener = new Response.Listener<AylaAPIRequest.EmptyResponse>() {
            @Override
            public void onResponse(AylaAPIRequest.EmptyResponse response) {
                String msg = MainActivity.getInstance().getString(R
                        .string.saved_batch_action_success);
                Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();
                BatchActionsFragment frag = BatchActionsFragment.newInstance();
                MainActivity.getInstance().pushFragment(frag);
            }
        };
        final ErrorListener errorListener = new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                String errorString = MainActivity.getInstance().getString(R.string.Toast_Error) +
                        error.toString();
                Toast.makeText(MainActivity.getInstance(), errorString, Toast.LENGTH_LONG).show();
                MainActivity.getInstance().popBackstackToRoot();
            }
        };

        if (_batchAction.getUuid() == null) {//This is a new Batch Action
            _batchAction.setUuid(Automation.randomUUID());
            BatchManager.addBatchAction(_batchAction, successListener, errorListener);
        } else {
            BatchManager.updateBatchAction(_batchAction, successListener, errorListener);
        }
    }

    private void doFillData(final Action[] arrayAlAction) {
        String[] actionUUIDs = null;
        if (_batchAction != null) {
            actionUUIDs = _batchAction.getActionUuids();
            _batchActionEditText.setText(_batchAction.getName());
        }
        final List<AylaDevice> deviceList = AMAPCore.sharedInstance().getDeviceManager()
                .getDevices();
        if (deviceList != null) {
            for (AylaDevice device : deviceList) {
                ViewModel model = AMAPCore.sharedInstance().getSessionParameters().viewModelProvider
                        .viewModelForDevice(device);
                //Add only those devices that have Notifiable properties
                if (checkInputPropertyNames(device, model)) {
                    _deviceMap.put(device.getProductName(), device.getDsn());
                    _deviceNames.add(device.getProductName());
                    if (arrayAlAction != null) {
                        ArrayList<Action> actions = new ArrayList<>();
                        for (Action alAction : arrayAlAction) {
                            if (alAction == null) {
                                continue;
                            }
                            if (device.getDsn().equals(alAction.getDSN())) {
                                actions.add(alAction);
                            }
                        }
                        _actionItems.add(actions);
                    }
                }
            }
        }

        _adapter = new DeviceActionAdapter(_deviceNames, _actionItems, actionUUIDs);
        _adapter.setInflater((LayoutInflater) getActivity().getSystemService
                (Context.LAYOUT_INFLATER_SERVICE));
        _expandableListView.setAdapter(_adapter);

        _expandableListView.setGroupIndicator(null);
        _saveButton.setVisibility(View.VISIBLE);
    }

    //Check if it the device has PropertyNames and has at least 1 property with INPUT
    // Direction
    private boolean checkInputPropertyNames(AylaDevice device, ViewModel model) {
        String[] propertyNames = model.getNotifiablePropertyNames();

        if (propertyNames != null && propertyNames.length > 0) {
            for (String propName : propertyNames) {
                AylaProperty aylaProperty = device.getProperty(propName);
                if (aylaProperty != null && INPUT.equals(aylaProperty.getDirection())) {
                    return true;
                }
            }
        }
        return false;
    }

    public class DeviceActionAdapter extends BaseExpandableListAdapter {
        private final ArrayList<ArrayList<Action>> _actionListItems;
        private final ArrayList<String> _deviceListNames;
        private final ArrayList<Action> _checkedList;
        private final String[] _actionUUIDs;
        private LayoutInflater _inflater;
        private ArrayList<Action> _actionsList;

        public DeviceActionAdapter(ArrayList<String> parents, ArrayList<ArrayList<Action>> objectArrayList, String[] actionUUIDs) {
            this._deviceListNames = parents;
            this._actionListItems = objectArrayList;
            _checkedList = new ArrayList<>();
            _actionUUIDs = actionUUIDs;
        }

        public void setInflater(LayoutInflater _inflater) {
            this._inflater = _inflater;
        }

        @Override
        public View getChildView(int groupPosition, final int childPosition, boolean isLastChild,
                                 View convertView, ViewGroup parent) {
            _actionsList = _actionListItems.get(groupPosition);

            CheckBox checkBoxView;

            if (convertView == null) {
                convertView = _inflater.inflate(R.layout.automation_action_row, parent, false);
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
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    EditActionsFragment frag = EditActionsFragment.newInstance(action.getDSN(), action);
                    MainActivity.getInstance().pushFragment(frag);
                }
            });

            return convertView;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = _inflater.inflate(R.layout.device_action_group, parent,false);
            }
            TextView deviceView = (TextView) convertView.findViewById(R.id.device_name_title);
            String deviceName = _deviceListNames.get(groupPosition);
            deviceView.setText(deviceName);
            final String dsn = _deviceMap.get(deviceName);

            ImageView actionAddButton = (ImageView) convertView.findViewById(R.id.btn_add_action);
            actionAddButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditActionsFragment frag = EditActionsFragment.newInstance(dsn, null);
                    MainActivity.getInstance().pushFragment(frag);
                }
            });
            if (getChildrenCount(groupPosition) >= MAX_ACTIONS_PER_DEVICE) {
                actionAddButton.setVisibility(View.GONE);
            }
            AylaDevice device = AMAPCore.sharedInstance().getDeviceManager().deviceWithDSN(dsn);
            ViewModel deviceModel = AMAPCore.sharedInstance().getSessionParameters().viewModelProvider
                    .viewModelForDevice(device);
            //Check if the device has Input properties. In case it does not have input properties
            // disable the action add button
            boolean hasInputProperties = false;
            if (device != null && deviceModel != null) {
                for (String propName : deviceModel.getNotifiablePropertyNames()) {
                    AylaProperty aylaProperty = device.getProperty(propName);
                    if (aylaProperty != null && INPUT.equals(aylaProperty.getDirection())) {
                        hasInputProperties = true;
                        break;
                    }
                }
            }
            if (!hasInputProperties) {
                actionAddButton.setVisibility(View.GONE);
            }

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
            if(_actionListItems == null || _actionListItems.isEmpty()) {
                return 0;
            }
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

        public ArrayList<String> getCheckedItems() {
            ArrayList<String> checkedUUIDList= new ArrayList<>();
            for(Action action:_checkedList) {
                if(!checkedUUIDList.contains(action.getId())) {
                    checkedUUIDList.add(action.getId());
                }
            }
            return checkedUUIDList;
        }
    }
}
