package com.aylanetworks.agilelink.framework;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.aylanetworks.aaml.AylaUser;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Brian King on 2/5/15.
 */
public class DeviceGroup {
    private final static String LOG_TAG = "DeviceGroup";

    private String _groupName;
    private String _groupID;
    private Set<String> _deviceDSNs;
    private boolean _isDirty;

    /**
     * Default constructor
     */
    public DeviceGroup() {
        _isDirty = false;
        _deviceDSNs = new HashSet<>();
    }

    public DeviceGroup(String groupName, String groupID) {
        _deviceDSNs = new HashSet<>();
        _groupName = groupName;
        _groupID = groupID;
    }

    public class DeviceGroupListener {
        void groupUpdated(DeviceGroup group){}
    }

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
        AylaUser.getCurrent().getDatumWithKey(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Log.d(LOG_TAG, "fetchGroupMembers: " + msg);

                // TODO: Process the results
                if ( listener != null ) {
                    listener.groupUpdated(DeviceGroup.this);
                }
            }
        }, _groupID);
    }

    public void pushToServer(final DeviceGroupListener listener) {

    }

    /**
     * Adds a device to the group.
     * @param device The device to add to the group
     * @return true if the device was added to the group, or false if the device already existed in the group
     */
    public boolean addDevice(Device device) {
        return _deviceDSNs.add(device.getDevice().dsn);
    }

    /**
     * Removes a device from the group
     * @param device The device to remove from the group
     * @return true if the device was removed, or false if the device did not exist in the group
     */
    public boolean removeDevice(Device device) {
        return _deviceDSNs.remove(device.getDevice().dsn);
    }

    /**
     * Checks to see if a device exists in the group
     * @param device Device to check
     * @return true if the device is in the group, or false if not
     */
    public boolean isDeviceInGroup(Device device) {
        String deviceDSN = device.getDevice().dsn;
        return _deviceDSNs.contains(deviceDSN);
    }

    /**
     * Returns a list of all devices in the group
     * @return The list of all devices in the group
     */
    public List<Device> getDevices() {
        List<Device> devices = new ArrayList<>();
        for ( String dsn : _deviceDSNs ) {
            Device device = SessionManager.deviceManager().deviceByDSN(dsn);
            if ( device == null ) {
                Log.e(LOG_TAG, "No device with DSN " + dsn + " found, but it is in a group!");
                continue;
            }

            devices.add(device);
        }

        return devices;
    }
}
