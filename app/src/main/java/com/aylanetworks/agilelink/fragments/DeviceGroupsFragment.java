package com.aylanetworks.agilelink.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.adapters.DeviceListAdapter;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.DeviceGroup;
import com.aylanetworks.agilelink.framework.GroupManager;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.util.ArrayList;
import java.util.List;

/*
 * DeviceGroupsFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 2/5/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class DeviceGroupsFragment extends AllDevicesFragment {
    private static final String LOG_TAG = "DeviceGroupsFragment";

    private HorizontalScrollView _buttonScrollView;
    private DeviceGroup _selectedGroup;
    private TextView _emptyView;

    public static DeviceGroupsFragment newInstance() {
        return new DeviceGroupsFragment();
    }

    public DeviceGroupsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        LinearLayout l = (LinearLayout) root.findViewById(R.id.button_tray);
        l.setVisibility(View.VISIBLE);
        _buttonScrollView = (HorizontalScrollView) root.findViewById(R.id.button_scroll_view);
        _emptyView = (TextView) root.findViewById(R.id.empty);
        createGroupButtonHeader();
        return root;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_groups, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Change the name of the "Add Device" menu item
        MenuItem addItem = menu.findItem(R.id.action_add_device);
        addItem.setTitle(R.string.action_manage_devices_in_group);

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_group:
                onAddGroup();
                break;

            case R.id.action_delete_group:
                onDeleteGroup();
                break;

            case R.id.action_add_device:
                onAddDeviceToGroup();
                break;

            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    protected void updateDeviceList() {
        if (_selectedGroup != null) {
            List<Device> selectedGroupDeviceList = _selectedGroup.getDevices();
            _adapter = new DeviceListAdapter(selectedGroupDeviceList, this);
            _recyclerView.setAdapter(_adapter);
            if ( selectedGroupDeviceList.isEmpty() ) {
                _emptyView.setText(R.string.no_devices_in_group);
                _recyclerView.setVisibility(View.GONE);
                _emptyView.setVisibility(View.VISIBLE);
            } else {
                _recyclerView.setVisibility(View.VISIBLE);
                _emptyView.setVisibility(View.GONE);
            }
        } else {
            _adapter = new DeviceListAdapter(null, this);
            _recyclerView.setAdapter(_adapter);
            _recyclerView.setVisibility(View.GONE);
            _emptyView.setText(R.string.group_empty_text);
            _emptyView.setVisibility(View.VISIBLE);
        }
    }

    protected void createGroupButtonHeader() {
        if (SessionManager.deviceManager() == null) {
            Log.d(LOG_TAG, "Not yet ready to create group buttons...");
            return;
        }

        List<DeviceGroup> groups;
        if (SessionManager.deviceManager() != null) {
            groups = SessionManager.deviceManager().getGroupManager().getGroups();
        } else {
            groups = new ArrayList<>();
        }

        if (_selectedGroup == null && groups.size() > 0) {
            _selectedGroup = groups.get(0);
        }

        // Make a linear layout to hold all of the buttons
        LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(5, 5, 5, 5);
        layoutParams.gravity = Gravity.CENTER_VERTICAL;
        layout.setLayoutParams(layoutParams);

        for (DeviceGroup group : groups) {
            Button b = new Button(getActivity());

            layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(8, 0, 8, 0);
            layoutParams.gravity = Gravity.CENTER_VERTICAL;
            b.setLayoutParams(layoutParams);

            b.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
            b.setPadding(24, 24, 24, 24);

            b.setText(group.getGroupName());
            b.setTag(group);
            b.setLayoutParams(layoutParams);
            b.setBackground(getResources().getDrawable(R.drawable.toggle_button_bg));

            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LinearLayout l = (LinearLayout) v.getParent();
                    for (int i = 0; i < l.getChildCount(); i++) {
                        l.getChildAt(i).setSelected(false);
                    }
                    v.setSelected(true);
                    onGroupSelected((DeviceGroup) v.getTag());
                }
            });

            b.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    v.performClick();
                    return true;
                }
            });

            b.setSelected(group.equals(_selectedGroup));
            layout.addView(b);
        }

        _buttonScrollView.removeAllViews();
        _buttonScrollView.addView(layout);
    }

    @Override
    public void onResume() {
        super.onResume();
        createGroupButtonHeader();
        updateDeviceList();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.add_button) {
            if ( _selectedGroup == null ) {
                // There is no group yet. We need to offer to add one.
                onAddGroup();
                return;
            }

            // Put up a menu to see if they want to add a device or a group.
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.add_device_or_group_title);
            builder.setItems(R.array.device_or_group_items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        // Add a device to this group
                        onAddDeviceToGroup();
                    } else {
                        onAddGroup();
                    }
                }
            });
            builder.create().show();
        } else {
            super.onClick(v);
        }
    }

    @Override
    public void deviceListChanged() {
        super.deviceListChanged();
        createGroupButtonHeader();
        updateDeviceList();
    }

    protected void onAddDeviceToGroup() {
        final List<Device> allDevices = SessionManager.deviceManager().deviceList();
        final String deviceNames[] = new String[allDevices.size()];
        final boolean isGroupMember[] = new boolean[allDevices.size()];

        for (int i = 0; i < allDevices.size(); i++) {
            Device d = allDevices.get(i);
            deviceNames[i] = d.toString();
            isGroupMember[i] = (_selectedGroup.isDeviceInGroup(d));
        }

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.choose_group_devices)
                .setMultiChoiceItems(deviceNames, isGroupMember, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        isGroupMember[which] = isChecked;
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        List<Device> newGroupList = new ArrayList<>();
                        for (int i = 0; i < allDevices.size(); i++) {
                            Device d = allDevices.get(i);
                            if (isGroupMember[i]) {
                                newGroupList.add(d);
                            }
                        }
                        _selectedGroup.setDevices(newGroupList);
                        _selectedGroup.pushToServer();
                        updateDeviceList();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create().show();
    }

    protected void onAddGroup() {
        final EditText et = new EditText(getActivity());
        final GroupManager gm = SessionManager.deviceManager().getGroupManager();

        AlertDialog dlg = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.add_group_title)
                .setMessage(R.string.add_group_message)
                .setView(et)
                .setPositiveButton(R.string.add_group, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Make sure the group is not called "All Devices"
                        String allDevicesName = getResources().getString(R.string.all_devices);
                        if (et.getText().toString().equals(allDevicesName)) {
                            Toast.makeText(getActivity(), R.string.invalid_group_name, Toast.LENGTH_LONG).show();
                        } else {
                            // Add the group
                            gm.createGroup(et.getText().toString(), null);
                            gm.pushGroupList();
                            deviceListChanged();

                            // Group is not useful with nothing in it! Bring up the UI to add
                            // devices to the group
                            onAddDeviceToGroup();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dlg.show();
    }

    protected void onDeleteGroup() {
        Log.d(LOG_TAG, "onDeleteGroup");
        String msg = getResources().getString(R.string.confirm_delete_group_body, _selectedGroup.getGroupName());
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.confirm_delete_group)
                .setMessage(msg)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GroupManager gm = SessionManager.deviceManager().getGroupManager();
                        gm.removeGroup(_selectedGroup);
                        gm.pushGroupList();
                        if ( gm.getGroups().isEmpty() ) {
                            _selectedGroup = null;
                        } else {
                            _selectedGroup = gm.getGroups().get(0);
                        }
                        deviceListChanged();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .create().show();
    }

    protected void onGroupSelected(DeviceGroup group) {
        Log.d(LOG_TAG, "Selected group: " + group);
        _selectedGroup = group;
        updateDeviceList();
    }
}
