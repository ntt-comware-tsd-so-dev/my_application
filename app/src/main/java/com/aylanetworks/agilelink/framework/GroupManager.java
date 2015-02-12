package com.aylanetworks.agilelink.framework;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.aylanetworks.aaml.AylaDatum;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Brian King on 2/9/15.
 */
public class GroupManager {
    private final static String LOG_TAG = "GroupManager";
    private boolean _isDirty;
    private boolean _datumExistsOnServer;

    /**
     * Interface to notify of changes to the group list or groups within the list
     */
    public interface GroupManagerListener {
        void groupListChanged();
        void groupMembersChanged(DeviceGroup changedGroup);
    }

    /**
     * Default constructor
     */
    public GroupManager() {
        _groupManagerListeners = new HashSet<>();
        _deviceGroups = new HashSet<>();
    };

    // Listeners

    private Set<GroupManagerListener> _groupManagerListeners;
    public void addListener(GroupManagerListener listener) {
        _groupManagerListeners.add(listener);
    }

    public void removeListener(GroupManagerListener listener) {
        _groupManagerListeners.remove(listener);
    }

    // Public Methods

    /**
     * Fetches the list of groups from the server. Listeners will be notified when the call is
     * complete by being called with groupListChanged() if the group list has changed.
     */
    public void fetchDeviceGroups() {
        AylaUser.getCurrent().getDatumWithKey(_fetchGroupIndexHandler, getGroupIndexKey());
    }

    public List<DeviceGroup> getGroups() {
        List<DeviceGroup> groups = new ArrayList<>(_deviceGroups);
        Collections.sort(groups, new Comparator<DeviceGroup>() {
            @Override
            public int compare(DeviceGroup lhs, DeviceGroup rhs) {
                return lhs.getGroupName().compareToIgnoreCase(rhs.getGroupName());
            }
        });

        return groups;
    }

    public DeviceGroup createGroup(String groupName, List<Device>deviceList) {
        // Create the new group
        DeviceGroup g = new DeviceGroup(groupName, DeviceGroup.createGroupID());
        if ( deviceList != null ) {
            for (Device d : deviceList) {
                g.addDevice(d);
            }
        }

        if ( !_deviceGroups.add(g) ) {
            Log.e(LOG_TAG, "Group already exists");
            return null;
        }

        _isDirty = true;

        return g;
    }

    public void removeGroup(DeviceGroup group) {
        group.removeAll();
        _deviceGroups.remove(group);
        _isDirty = true;
    }

    public void pushGroupList() {
        if ( _isDirty ) {
            AylaDatum datum = new AylaDatum();
            // Make a map of our group names to group IDs
            Map<String, String> groupMap = new HashMap<>();
            for ( DeviceGroup group : _deviceGroups ) {
                // Push each group to the server
                group.pushToServer();
                groupMap.put(group.getGroupName(), group.getGroupID());
            }

            JSONObject jsonMap = new JSONObject(groupMap);
            datum.key = getGroupIndexKey();
            datum.value = jsonMap.toString();

            if (_datumExistsOnServer) {
                AylaUser.getCurrent().updateDatum(_createGroupListHandler, datum);
            } else {
                AylaUser.getCurrent().createDatum(_createGroupListHandler, datum);
            }
        }
    }

    // Internal Methods

    protected String getGroupIndexKey() {
        return SessionManager.sessionParameters().appId + "-Groups";
    }

    protected Set<DeviceGroup> deviceGroupSetFromJsonString(String json) {
        Set<DeviceGroup> deviceGroups = new HashSet<>();
        // Get the list of groups from the JSON string
        try {
            JSONObject groupList = new JSONObject(json);
            Iterator<String> keys = groupList.keys();
            while ( keys.hasNext() ) {
                String groupName = keys.next();
                String groupDatumKey = groupList.optString(groupName);

                // Add the group to our set of groups. If this is a new group, fetch
                // its members as well.
                DeviceGroup group = new DeviceGroup(groupName, groupDatumKey);
                boolean changed = false;
                if ( deviceGroups.add(group) ) {
                    group.fetchGroupMembers(null);
                    changed = true;
                }

                if ( changed ) {
                    notifyGroupListChanged();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            deviceGroups = null;
        }

        return deviceGroups;
    }

    protected Set<DeviceGroup> _deviceGroups;

    protected Handler _fetchGroupIndexHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "fetchGroupIndexHandler: " + msg);

            if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
                _datumExistsOnServer = true;
                String json = (String)msg.obj;
                AylaDatum datum = AylaSystemUtils.gson.fromJson(json, AylaDatum.class);
                json = datum.value;

                _deviceGroups = deviceGroupSetFromJsonString(json);
                notifyGroupListChanged();
             } else {
                Log.e(LOG_TAG, "Failed to fetch group indexes: " + msg.obj);
                if ( msg.arg1 == 404 ) {
                    _datumExistsOnServer = false;
                }
            }
        }
    };

    protected void notifyGroupListChanged() {
        for ( GroupManagerListener l : _groupManagerListeners ) {
            l.groupListChanged();
        }
    }

    protected void notifyGroupMembersChanged(DeviceGroup group) {
        for ( GroupManagerListener l : _groupManagerListeners ) {
            l.groupMembersChanged(group);
        }
    }

    private Handler _createGroupListHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "Create group list handler: " + msg);
            if (msg.what == AylaNetworks.AML_ERROR_OK) {
                _datumExistsOnServer = true;
            } else {
                Log.e(LOG_TAG, "Create / update group list failed: " + msg);
            }
        }
    };
}
