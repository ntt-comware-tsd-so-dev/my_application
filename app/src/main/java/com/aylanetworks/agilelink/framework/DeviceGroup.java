package com.aylanetworks.agilelink.framework;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.aylanetworks.aaml.AylaDatapoint;
import com.aylanetworks.aaml.AylaDatum;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * DeviceGroup.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 2/5/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class DeviceGroup {
    private final static String LOG_TAG = "DeviceGroup";

    // Key for the device list in our datum JSON
    private final static String DSN_ARRAY_KEY = "dsns";

    private String _groupName;
    private String _groupID;
    private Set<String> _deviceDSNs;
    private boolean _isDirty;
    private boolean _datumExistsOnServer;

    /**
     * Listener interface to receive notifications that the group has been updated
     */
    public class DeviceGroupListener {
        void groupUpdated(DeviceGroup group) {
        }
    }

    /**
     * Default constructor
     */
    public DeviceGroup() {
        _isDirty = false;
        _deviceDSNs = new HashSet<>();
    }

    /**
     * Constructor with group name and ID
     *
     * @param groupName Name of the group
     * @param groupID   ID of the group, set to NULL to have one created for you.
     */
    public DeviceGroup(String groupName, String groupID) {
        _deviceDSNs = new HashSet<>();
        _groupName = groupName;
        if (groupID != null) {
            _groupID = groupID;
        } else {
            _groupID = createGroupID();
        }
    }

    /**
     * Returns the name of the group
     *
     * @return Name of the group
     */
    public String getGroupName() {
        return _groupName;
    }

    /**
     * Returns the group ID
     *
     * @return the group ID
     */
    public String getGroupID() {
        return _groupID;
    }

    /**
     * Creates a unique group ID. This is used when creating new groups.
     *
     * @return the unique group ID
     */
    public static String createGroupID() {
        SecureRandom r = new SecureRandom();
        String uniquePart = new BigInteger(64, r).toString(16);
        String groupId = SessionManager.sessionParameters().appId + "-Group-" + uniquePart;
        Log.d(LOG_TAG, "createGroupID: " + groupId);
        return groupId;
    }

    /**
     * Connects to the server to fetch the list of devices that are members of this group
     */
    public void fetchGroupMembers(final DeviceGroupListener listener) {
        AylaUser.getCurrent().getDatumWithKey(new FetchGroupMembersHandler(this, listener), _groupID);
    }

    static class FetchGroupMembersHandler extends Handler {
        private WeakReference<DeviceGroup> _deviceGroup;
        private DeviceGroupListener _listener;

        public FetchGroupMembersHandler(DeviceGroup deviceGroup, DeviceGroupListener listener) {
            _deviceGroup = new WeakReference<DeviceGroup>(deviceGroup);
            _listener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "fetchGroupMembers: " + msg);
            if (msg.what == AylaNetworks.AML_ERROR_OK) {
                _deviceGroup.get()._isDirty = false;
                _deviceGroup.get()._datumExistsOnServer = true;

                AylaDatum datum = AylaSystemUtils.gson.fromJson((String) msg.obj, AylaDatum.class);
                _deviceGroup.get().updateGroupListFromDatum(datum);
                if (_listener != null) {
                    _listener.groupUpdated(_deviceGroup.get());
                }
            } else {
                Log.e(LOG_TAG, "fetchGroupMembers failed: " + msg);
                _deviceGroup.get()._datumExistsOnServer = false;
            }
        }
    }

    /**
     * Submits the group to the server. Call this after changes are made to the group.
     */
    public void pushToServer() {
        // Create the AylaDatum object with our group's JSON
        if (_isDirty) {
            AylaDatum datum = new AylaDatum();
            datum.key = getGroupID();
            JSONArray dsnArray = new JSONArray(_deviceDSNs);

            JSONObject datumMap = new JSONObject();
            try {
                datumMap.put(DSN_ARRAY_KEY, dsnArray);
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }

            datum.value = datumMap.toString();

            Log.d(LOG_TAG, "pushToServer: " + datum.key + ":" + datum.value);

            if (_datumExistsOnServer) {
                AylaUser.getCurrent().updateDatum(_updateDatumHandler, datum);
            } else {
                AylaUser.getCurrent().createDatum(_updateDatumHandler, datum);
            }
        } else {
            Log.d(LOG_TAG, "Not pushing group " + getGroupName() + " to server, as we haven't changed");
        }
    }

    /**
     * Adds a device to the group.
     *
     * @param device The device to add to the group
     * @return true if the device was added to the group, or false if the device already existed in the group
     */
    public boolean addDevice(Device device) {
        if (_deviceDSNs.add(device.getDevice().dsn)) {
            _isDirty = true;
            return true;
        }
        return false;
    }

    /**
     * Removes a device from the group
     *
     * @param device The device to remove from the group
     * @return true if the device was removed, or false if the device did not exist in the group
     */
    public boolean removeDevice(Device device) {
        if (_deviceDSNs.remove(device.getDevice().dsn)) {
            _isDirty = true;
            return true;
        }
        return false;
    }

    /**
     * Removes all devices from the group as well as the datum for this group.
     */
    public void removeAll() {
        _deviceDSNs.clear();
        AylaDatum datum = new AylaDatum();
        datum.key = getGroupID();
        AylaUser.getCurrent().deleteDatum(datum);
        _isDirty = false;
    }

    /**
     * Checks to see if a device exists in the group
     *
     * @param device Device to check
     * @return true if the device is in the group, or false if not
     */
    public boolean isDeviceInGroup(Device device) {
        String deviceDSN = device.getDevice().dsn;
        return _deviceDSNs.contains(deviceDSN);
    }

    /**
     * Returns a list of all devices in the group
     *
     * @return The list of all devices in the group
     */
    public List<Device> getDevices() {
        if ( SessionManager.deviceManager() == null ) {
            return new ArrayList<>();
        }

        Set<Device> devices = new HashSet<>();
        for (String dsn : _deviceDSNs) {
            Device device = SessionManager.deviceManager().deviceByDSN(dsn);
            if (device == null) {
                Log.e(LOG_TAG, "No device with DSN " + dsn + " found, but it is in a group!");
                continue;
            }

            devices.add(device);
        }

        List<Device>deviceList = new ArrayList<>(devices);
        Collections.sort(deviceList, SessionManager.deviceManager().getDeviceComparator());

        return deviceList;
    }

    /**
     * Sets the device list for this group, removing any other devices that may have been in this
     * group previously.
     * @param devices List of devices that are members of this group
     */
    public void setDevices(List<Device> devices) {
        _deviceDSNs.clear();
        for ( Device d : devices ) {
            _deviceDSNs.add(d.getDevice().dsn);
        }
        _isDirty = true;
    }

    public static DeviceGroup allDevicesGroup() {
        DeviceGroup allDevicesGroup = new DeviceGroup();
        allDevicesGroup._groupName = MainActivity.getInstance().getResources().getString(R.string.all_devices);
        allDevicesGroup._groupID = "ALL_DEVICES";
        if ( SessionManager.deviceManager() != null ) {
            for (Device d : SessionManager.deviceManager().deviceList()) {
                allDevicesGroup.addDevice(d);
            }
        }

        return allDevicesGroup;
    }

    @Override
    public String toString() {
        return "Group " + getGroupName() + " with " + _deviceDSNs.size() + " devices";
    }

    @Override
    public int hashCode() {
        return getGroupID().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!o.getClass().isInstance(this)) {
            return false;
        }

        return getGroupID().equals(((DeviceGroup)o).getGroupID());
    }

    //////////

    private void updateGroupListFromDatum(AylaDatum datum) {
        _deviceDSNs.clear();
        try {
            JSONObject map = new JSONObject(datum.value);
            JSONArray array = (JSONArray)map.get(DSN_ARRAY_KEY);
            for (int i = 0; i < array.length(); i++) {
                _deviceDSNs.add(array.get(i).toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
            _deviceDSNs.clear();
        } catch (ClassCastException e) {
            Log.e(LOG_TAG, "Old-style JSON found in group list. Clearing group list for now.");
            e.printStackTrace();
            _deviceDSNs.clear();
        }

        Log.d(LOG_TAG, "JSON: " + datum.value + "\nDSNs: " + _deviceDSNs);
    }

    private Handler _updateDatumHandler = new UpdateDatumHandler(this);

    static class UpdateDatumHandler extends Handler {
        private WeakReference<DeviceGroup> _deviceGroup;

        public UpdateDatumHandler(DeviceGroup deviceGroup) {
            _deviceGroup = new WeakReference<DeviceGroup>(deviceGroup);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "updateDatumHandler: " + msg);
            if (msg.what == AylaNetworks.AML_ERROR_OK) {
                Log.d(LOG_TAG, "Datum updated");
            } else {
                Log.e(LOG_TAG, "updateDatumHandler: Failed: " + msg);
            }
        }
    }
}
