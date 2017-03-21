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
import android.widget.TextView;

import com.android.volley.Response;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.batch.BatchAction;
import com.aylanetworks.agilelink.framework.batch.BatchManager;
import com.aylanetworks.agilelink.framework.geofence.Action;
import com.aylanetworks.agilelink.framework.automation.Automation;
import com.aylanetworks.agilelink.framework.geofence.AylaDeviceActions;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/*
 * AMAP_Android
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */

public class AutomationActionsFragment extends Fragment {
    private final static String LOG_TAG = "ActionsListFragment";
    private final static String OBJ_KEY = "automation_obj";
    private ExpandableListView _batchListView;

    private Automation _automation;
    private Button _saveButton;
    private BatchActionAdapter _batchAdapter;


    public static AutomationActionsFragment newInstance(Automation automation) {
        AutomationActionsFragment frag = new AutomationActionsFragment();
        Bundle args = new Bundle();
        if (automation != null) {
            args.putSerializable(OBJ_KEY, automation);
            frag.setArguments(args);
        }
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater _inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = _inflater.inflate(R.layout.fragment_select_actions, container, false);
        _saveButton = (Button) rootView.findViewById(R.id.button_done_selection);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        _batchListView = (ExpandableListView) view.findViewById(R.id.expBatchListView);
        _batchListView.setEmptyView(view.findViewById(R.id.batches_empty));

        if (getArguments() != null) {
            _automation = (Automation) getArguments().getSerializable(OBJ_KEY);
        }

        MainActivity.getInstance().showWaitDialog(R.string.batch_action, R.string.fetching_batches_body);
        AylaDeviceActions.fetchActions(new Response.Listener<Action[]>() {
            @Override
            public void onResponse(Action[] arrayAlAction) {
                fetchBatchActions(arrayAlAction);
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                MainActivity.getInstance().dismissWaitDialog();
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

    private void fetchBatchActions(final Action[] arrayAlAction) {
        BatchManager.fetchBatchActions(new Response.Listener<BatchAction[]>() {
            @Override
            public void onResponse(BatchAction[] arrayBatchActions) {
                doFillData(arrayAlAction, arrayBatchActions);
                MainActivity.getInstance().dismissWaitDialog();
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                Log.d(LOG_TAG, error.getMessage());
                if (arrayAlAction != null) {
                    doFillData(arrayAlAction, null);
                }
                MainActivity.getInstance().dismissWaitDialog();
            }
        });
    }

    private void doSaveActions() {
        Log.d(LOG_TAG, "Clicked Save");
        //Get all the selections from Batch Actions
        if (_batchAdapter != null) {
            ArrayList<BatchAction> batchActionList = _batchAdapter.getCheckedItems();

            String[] uuidArray = new String[batchActionList.size()];
            for(int idx=0; idx < batchActionList.size(); idx++) {
                uuidArray[idx] = batchActionList.get(idx).getUuid();
            }
            _automation.setActions(uuidArray);
            EditAutomationFragment frag = EditAutomationFragment.newInstance(_automation);
            MainActivity.getInstance().pushFragment(frag);
        }
    }

    private void doFillData(final Action[] arrayAlAction, final BatchAction[] arrayBatchActions) {

        if (arrayBatchActions != null) {
            HashMap<String, Action> actionHashMap = new HashMap<>();
            for (Action action : arrayAlAction) {
                actionHashMap.put(action.getId(), action);
            }
            ArrayList<BatchAction> batchActionArray = new ArrayList<>();
            ArrayList<ArrayList<Action>> actionItemsArray = new ArrayList<>();

            for (BatchAction batchAction : arrayBatchActions) {
                String[] actionUUIDArray = batchAction.getActionUuids();
                if (actionUUIDArray != null) {
                    batchActionArray.add(batchAction);
                    ArrayList<Action> actions = new ArrayList<>();
                    for (String actionUUID : actionUUIDArray) {
                        actions.add(actionHashMap.get(actionUUID));
                    }
                    actionItemsArray.add(actions);
                }
            }
            _batchAdapter = new BatchActionAdapter(batchActionArray, actionItemsArray);
            _batchAdapter.setInflater((LayoutInflater) getActivity().getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE), getActivity());
            _batchListView.setAdapter(_batchAdapter);
            _batchListView.setVisibility(View.VISIBLE);
            _batchListView.setGroupIndicator(null);
        }
        _saveButton.setVisibility(View.VISIBLE);
    }


    public class BatchActionAdapter extends BaseExpandableListAdapter {
        private Activity _activity;
        private final ArrayList<ArrayList<Action>> _actionListItems;
        private LayoutInflater _inflater;
        private final ArrayList<BatchAction> _batchActionList;
        private ArrayList<Action> _actionsList;
        private final ArrayList<BatchAction> _checkedList;
        private final HashSet<String> _selectedList;

        public BatchActionAdapter(ArrayList<BatchAction> parents, ArrayList<ArrayList<Action>>
                objectArrayList) {
            _batchActionList = parents;
            _actionListItems = objectArrayList;
            _checkedList = new ArrayList<>();
            _selectedList = new HashSet<>();
            if(_automation != null && _automation.getActions() != null) {
                _selectedList.addAll(new ArrayList<>(Arrays.asList(_automation.getActions())));
            }
        }

        public void setInflater(LayoutInflater _inflater, Activity _activity) {
            this._inflater = _inflater;
            this._activity = _activity;
        }

        @Override
        public View getChildView(int groupPosition, final int childPosition, boolean isLastChild,
                                 View convertView, ViewGroup parent) {
            _actionsList =  _actionListItems.get(groupPosition);

            if (convertView == null) {
                convertView = _inflater.inflate(R.layout.device_action_row,  parent,false);
            }
            TextView textBoxView = (TextView) convertView.findViewById(R.id.textView1);
            textBoxView.setText(_actionsList.get(childPosition).getName());
            return convertView;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

            CheckBox checkBoxView;

            if (convertView == null) {
                convertView = _inflater.inflate(R.layout.batch_action_header_row,  parent,false);
            }

            checkBoxView = (CheckBox) convertView.findViewById(R.id.checkbox_action);

            final BatchAction batchAction= _batchActionList.get(groupPosition);
            checkBoxView.setText(batchAction.getName());
            if(_selectedList.contains(batchAction.getUuid())) {
                checkBoxView.setChecked(true);
            }
            if (checkBoxView.isChecked()) {
                _checkedList.add(batchAction);
            }

            checkBoxView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        _checkedList.add(batchAction);
                    } else {
                        _checkedList.remove(batchAction);
                    }
                }
            });
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
            return _batchActionList.size();
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

        public ArrayList<BatchAction> getCheckedItems() {
            return _checkedList;
        }
    }
}