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
import android.widget.Toast;

import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.adapters.DeviceListAdapter;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.DeviceGroup;
import com.aylanetworks.agilelink.framework.GroupManager;
import com.aylanetworks.agilelink.framework.ViewModel;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.change.ListChange;

import java.util.ArrayList;
import java.util.List;

/*
 * DeviceGroupsFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 2/5/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class DeviceGroupsFragment extends AllDevicesFragment implements GroupManager.GroupManagerListener {
    private static final String LOG_TAG = "DeviceGroupsFragment";

    private HorizontalScrollView _buttonScrollView;
    private DeviceGroup _selectedGroup;

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
        createGroupButtonHeader();
        updateDeviceList();
        return root;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_groups, menu);
        super.onCreateOptionsMenu(menu, inflater);
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

            case R.id.action_add_device_group:
                onAddDeviceToGroup();
                break;

            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void updateDeviceList() {
        if(!isAdded()){
            return;
        }
        if (_selectedGroup != null) {

            _adapter = DeviceListAdapter.fromDeviceList(_selectedGroup.getDevices(), this);
            _recyclerView.setAdapter(_adapter);
            if ( _selectedGroup.getDevices().isEmpty() ) {
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
        if (AMAPCore.sharedInstance().getDeviceManager() == null) {
            Log.d(LOG_TAG, "Not yet ready to create group buttons...");
            return;
        }

        List<DeviceGroup> groups;
        if (AMAPCore.sharedInstance().getDeviceManager() != null) {
            groups = AMAPCore.sharedInstance().getGroupManager().getGroups();
        } else {
            groups = new ArrayList<>();
        }

        if (_selectedGroup == null && groups.size() > 0) {
            _selectedGroup = groups.get(0);
        }

        int headerMargin = (int)getResources().getDimension(R.dimen.group_header_margin);
        int buttonMargin = (int)getResources().getDimension(R.dimen.group_button_margin);
        int buttonPadding = (int)getResources().getDimension(R.dimen.group_button_padding);
        Log.d("DIMENS", "hm: " + headerMargin + " bm: " + buttonMargin + " bp: " + buttonPadding);

        // Make a linear layout to hold all of the buttons
        LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(headerMargin, headerMargin, headerMargin, headerMargin);
        layoutParams.gravity = Gravity.CENTER_VERTICAL;
        layout.setLayoutParams(layoutParams);

        for (DeviceGroup group : groups) {
            Button b = new Button(getActivity());

            layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(buttonMargin, 0, buttonMargin, 0);
            layoutParams.gravity = Gravity.CENTER_VERTICAL;
            b.setLayoutParams(layoutParams);

            b.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
            b.setPadding(buttonPadding, buttonPadding, buttonPadding, buttonPadding);

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
            builder.setIcon(R.drawable.ic_launcher)
            .setTitle(R.string.add_device_or_group_title)
            .setItems(R.array.device_or_group_items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        // Add a device to this group
                        onAddDeviceToGroup();
                    } else {
                        onAddGroup();
                    }
                }
            })
            .create().show();
        } else {
            super.onClick(v);
        }
    }

    @Override
    public void deviceListChanged(ListChange change) {
        super.deviceListChanged(change);
        if (this.isAdded()) {
            createGroupButtonHeader();
            updateDeviceList();
        }
    }

    protected void onAddDeviceToGroup() {
        if ( _selectedGroup == null ) {
            // There is no group yet. We need to offer to add one.
            onAddGroup();
            return;
        }

        final List<ViewModel> allDevices = ViewModel.fromDeviceList(AMAPCore.sharedInstance().getDeviceManager()
                .getDevices());

        if ((allDevices != null) && (allDevices.size() > 0)) {
            final String deviceNames[] = new String[allDevices.size()];
            final boolean isGroupMember[] = new boolean[allDevices.size()];

            for (int i = 0; i < allDevices.size(); i++) {
                ViewModel d = allDevices.get(i);
                deviceNames[i] = d.getDevice().getFriendlyName();
                isGroupMember[i] = (_selectedGroup.isDeviceInGroup(d.getDevice()));
            }

            new AlertDialog.Builder(getActivity())
                    .setIcon(R.drawable.ic_launcher)
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
                            List<AylaDevice> newGroupList = new ArrayList<>();
                            for (int i = 0; i < allDevices.size(); i++) {
                                ViewModel d = allDevices.get(i);
                                if (isGroupMember[i]) {
                                    newGroupList.add(d.getDevice());
                                }
                            }
                            _selectedGroup.setDevices(newGroupList);
                            _selectedGroup.pushToServer();
                            updateDeviceList();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create().show();
        } else {
            Toast.makeText(getActivity(), R.string.no_devices, Toast.LENGTH_SHORT).show();
        }
    }

    protected void onAddGroup() {
        final GroupManager gm = AMAPCore.sharedInstance().getGroupManager();

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final View alertView = inflater.inflate(R.layout.dialog_add_group, null);
        final EditText et = (EditText)alertView.findViewById(R.id.group_name);

        AlertDialog dlg = new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_launcher)
                .setTitle(R.string.add_group_title)
                .setView(alertView)
                .setPositiveButton(R.string.add_group, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Make sure the group is not called "All Devices"
                        String allDevicesName = getResources().getString(R.string.all_devices);
                        if (et.getText().toString().equals(allDevicesName)) {
                            Toast.makeText(getActivity(), R.string.invalid_group_name, Toast.LENGTH_LONG).show();
                        } else {
                            // Add the group and select it
                            _selectedGroup = gm.createGroup(et.getText().toString(), null);
                            gm.pushGroupList();

                            // Group is not useful with nothing in it! Bring up the UI to add
                            // devices to the group
                            deviceListChanged(null);
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
        if(_selectedGroup != null){
            String msg = getResources().getString(R.string.confirm_delete_group_body, _selectedGroup.getGroupName());
            new AlertDialog.Builder(getActivity())
                    .setIcon(R.drawable.ic_launcher)
                    .setTitle(R.string.confirm_delete_group)
                    .setMessage(msg)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            GroupManager gm = AMAPCore.sharedInstance().getGroupManager();
                            gm.removeGroup(_selectedGroup);
                            gm.pushGroupList();
                            if ( gm.getGroups().isEmpty() ) {
                                _selectedGroup = null;
                            } else {
                                _selectedGroup = gm.getGroups().get(0);
                            }
                            deviceListChanged(null);
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .create().show();
        }

    }

    protected void onGroupSelected(DeviceGroup group) {
        Log.d(LOG_TAG, "Selected group: " + group);
        _selectedGroup = group;
        updateDeviceList();
    }

    public void groupListChanged() {
        createGroupButtonHeader();
    }
    public void groupMembersChanged(DeviceGroup changedGroup){

    }
}
