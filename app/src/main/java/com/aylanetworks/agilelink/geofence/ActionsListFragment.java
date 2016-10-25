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
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.geofence.Action;
import com.aylanetworks.agilelink.framework.geofence.AylaDeviceActions;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * AMAP_Android
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */

public class ActionsListFragment extends Fragment {
    private final static String LOG_TAG = "ActionsListFragment";
    private ExpandableListView _expandleListView;
    private final ArrayList<String> _deviceNames = new ArrayList<>();
    private final Map<String, String> _deviceMap = new HashMap<>();
    private final ArrayList<Object> _actionItems = new ArrayList<>();

    public static ActionsListFragment newInstance() {
        return new ActionsListFragment();
    }

    @Override
    public View onCreateView(LayoutInflater _inflater, ViewGroup container, Bundle savedInstanceState) {
        return _inflater.inflate(R.layout.fragment_geofence_actions, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        _deviceNames.clear();
        _actionItems.clear();
        _deviceMap.clear();

        _expandleListView = (ExpandableListView) view.findViewById(R.id.expListView);

        final List<AylaDevice> deviceList = AMAPCore.sharedInstance().getDeviceManager()
                .getDevices();
        AylaDeviceActions.fetchActions(new Response.Listener<Action[]>() {
            @Override
            public void onResponse(Action[] arrayAlAction) {
                for (AylaDevice aylaDevice : deviceList) {
                    _deviceMap.put(aylaDevice.getProductName(), aylaDevice.getDsn());
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
                DeviceActionAdapter adapter = new DeviceActionAdapter(_deviceNames, _actionItems);
                adapter.setInflater((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE), getActivity());
                _expandleListView.setAdapter(adapter);

                _expandleListView.setGroupIndicator(null);
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                Log.d(LOG_TAG, error.getMessage());
            }
        });

        DeviceActionAdapter adapter = new DeviceActionAdapter(_deviceNames, _actionItems);
        adapter.setInflater((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE), getActivity());
        _expandleListView.setAdapter(adapter);
        _expandleListView.setGroupIndicator(null);
    }

    public class DeviceActionAdapter extends BaseExpandableListAdapter {
        private Activity _activity;
        private final ArrayList<Object> _actionListItems;
        private LayoutInflater _inflater;
        private final ArrayList<String> _deviceListNames;
        private ArrayList<Action> _actionsList;

        public DeviceActionAdapter(ArrayList<String> parents, ArrayList<Object> objectArrayList) {
            this._deviceListNames = parents;
            this._actionListItems = objectArrayList;
        }

        public void setInflater(LayoutInflater _inflater, Activity _activity) {
            this._inflater = _inflater;
            this._activity = _activity;
        }

        @Override
        public View getChildView(int groupPosition, final int childPosition, boolean isLastChild,
                                 View convertView, ViewGroup parent) {
            String deviceName = _deviceListNames.get(groupPosition);
            final String dsn = _deviceMap.get(deviceName);

            _actionsList = (ArrayList<Action>) _actionListItems.get(groupPosition);

            TextView textBoxView;

            if (convertView == null) {
                convertView = _inflater.inflate(R.layout.device_action_row, null);
            }

            textBoxView = (TextView) convertView.findViewById(R.id.textView1);
            textBoxView.setText(_actionsList.get(childPosition).getName());

            convertView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    Action action = _actionsList.get(childPosition);
                    EditActionsFragment frag = EditActionsFragment.newInstance(dsn, action);
                    MainActivity.getInstance().pushFragment(frag);
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
            final String dsn = _deviceMap.get(deviceName);

            ImageView actionAddButton = (ImageView) convertView.findViewById(R.id.btn_add_action);
            actionAddButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditActionsFragment frag = EditActionsFragment.newInstance(dsn, null);
                    MainActivity.getInstance().pushFragment(frag);
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
    }
}
