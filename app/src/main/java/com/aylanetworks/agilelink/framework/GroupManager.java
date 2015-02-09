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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by Brian King on 2/9/15.
 */
public class GroupManager {
    private final static String LOG_TAG = "GroupManager";

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
    public void addGroupManagerListener(GroupManagerListener listener) {
        _groupManagerListeners.add(listener);
    }

    public void removeGroupManagerListener(GroupManagerListener listener) {
        _groupManagerListeners.remove(listener);
    }

    // Public Methods

    /**
     * Fetches the list of groups from the server.
     */
    public void fetchDeviceGroups() {
        AylaUser.getCurrent().getDatumWithKey(_fetchGroupIndexHandler, getGroupIndexKey());
    }

    public Set<DeviceGroup> getGroups() {
        return new HashSet<>(_deviceGroups);
    }

    public DeviceGroup createGroup(String groupName, List<Device>deviceList) {
        DeviceGroup g = new DeviceGroup(groupName, DeviceGroup.createGroupID());
        for ( Device d : deviceList ) {
            g.addDevice(d);
        }

        if ( !_deviceGroups.add(g) ) {
            Log.e(LOG_TAG, "Group already exists");
            return null;
        }

        return g;
    }

    // Internal Methods

    protected String getGroupIndexKey() {
        return SessionManager.sessionParameters().appId + "-Groups";
    }

    protected Set<DeviceGroup> _deviceGroups;

    protected Handler _fetchGroupIndexHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "fetchGroupIndexHandler: " + msg);

            if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
                String json = (String)msg.obj;
                AylaDatum datum = AylaSystemUtils.gson.fromJson(json, AylaDatum.class);
                json = datum.value;

                if ( _deviceGroups == null ) {
                    _deviceGroups = new HashSet<>();
                }

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
                        if ( _deviceGroups.add(group) ) {
                            group.fetchGroupMembers(null);
                            changed = true;
                        }

                        if ( changed ) {
                            notifyGroupListChanged();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
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
}
