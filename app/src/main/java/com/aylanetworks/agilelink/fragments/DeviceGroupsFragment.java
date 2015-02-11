package com.aylanetworks.agilelink.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.adapters.DeviceListAdapter;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.DeviceGroup;
import com.aylanetworks.agilelink.framework.DeviceManager;
import com.aylanetworks.agilelink.framework.GroupManager;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by Brian King on 2/5/15.
 */
public class DeviceGroupsFragment extends AllDevicesFragment {
    private static final String LOG_TAG = "DeviceGroupsFragment";

    private HorizontalScrollView _buttonScrollView;
    private DeviceGroup _selectedGroup;
    private List<Device> _selectedGroupDeviceList;

    public static DeviceGroupsFragment newInstance() {
        return new DeviceGroupsFragment();
    }

    public DeviceGroupsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        LinearLayout l = (LinearLayout) root.findViewById(R.id.button_tray);
        l.setVisibility(View.VISIBLE);
        _buttonScrollView = (HorizontalScrollView) root.findViewById(R.id.button_scroll_view);
        createGroupButtonHeader();
        return root;
    }

    protected void updateDeviceList() {
        if (_selectedGroup != null) {
            _selectedGroupDeviceList = _selectedGroup.getDevices();
            _adapter = new DeviceListAdapter(_selectedGroupDeviceList, this);
            _recyclerView.setAdapter(_adapter);
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

        // Add the "All Devices" group
        groups.add(0, DeviceGroup.allDevicesGroup());

        if (_selectedGroup == null) {
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

            b.setSelected(group == _selectedGroup);
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
            // Put up a menu to see if they want to add a device or a group.
            // If we're currently on the "All Devices" group, we can't add a device, so we won't
            // bother to ask.
            String allDevicesGroupName = getResources().getString(R.string.all_devices);
            if (_selectedGroup.getGroupName().equals(allDevicesGroupName)) {
                onAddGroup();
            } else {
                // Put up a context menu with the choice
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
            }
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

        for ( int i = 0; i < allDevices.size(); i++ ) {
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
                for ( int i = 0; i < allDevices.size(); i++ ) {
                    Device d = allDevices.get(i);
                    if ( isGroupMember[i] ) {
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

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.add_group_title)
                .setMessage(R.string.add_group_message)
                .setView(et)
                .setPositiveButton(R.string.add_group, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Add the group
                        gm.createGroup(et.getText().toString(), null);
                        gm.pushGroupList();
                        deviceListChanged();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    protected void onGroupSelected(DeviceGroup group) {
        Log.d(LOG_TAG, "Selected group: " + group);
        _selectedGroup = group;
        updateDeviceList();
    }
}
