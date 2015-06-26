package com.aylanetworks.agilelink.framework;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.aylanetworks.aaml.AylaDeviceNode;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.zigbee.AylaDeviceZigbeeGateway;
import com.aylanetworks.aaml.zigbee.AylaDeviceZigbeeNode;
import com.aylanetworks.aaml.zigbee.AylaGroupZigbee;
import com.aylanetworks.aaml.zigbee.AylaNetworksZigbee;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * ZigbeeGroupManager.java
 * AgileLink Application Framework
 *
 * Created by David Junod on 5/27/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class ZigbeeGroupManager {

    private final static String LOG_TAG = "ZigbeeGroupManager";

    private Gateway _gateway;
    private Set<ZigbeeGroupManagerListener> _listeners;
    protected Set<AylaGroupZigbee> _groups;

    /**
     * Interface to notify of changes to the group list or groups within the list
     */
    public interface ZigbeeGroupManagerListener {

        void zigbeeGroupListChanged(Gateway gateway);

        void zigbeeGroupMembersChanged(Gateway gateway, AylaGroupZigbee group);

        void zigbeeCreateGroupCompleted(Gateway gateway, String name, Message msg, AylaGroupZigbee group);

        void zigbeeUpdateGroupCompleted(Gateway gateway, String name, Message msg, AylaGroupZigbee group);

        void zigbeeDeleteGroupCompleted(Gateway gateway, String name, Message msg);
    }

    /**
     * Default constructor
     * @param gateway Gateway
     */
    public ZigbeeGroupManager(Gateway gateway) {
        _gateway = gateway;
        _listeners = new HashSet<>();
        _groups = new HashSet<>();
    };

    // Listeners

    public void addListener(ZigbeeGroupManagerListener listener) {
        _listeners.add(listener);
    }

    public void removeListener(ZigbeeGroupManagerListener listener) {
        _listeners.remove(listener);
    }

    // Public Methods

    /**
     * Useful utility method for logging the current devices in AylaGroupZigbee object.
     * @param group AylaGroupZigbee
     * @return A string containing the DSN's of the devices in the group.
     */
    static public String getGroupNodesToString(AylaGroupZigbee group) {
        StringBuilder sb = new StringBuilder(512);
        if (group != null) {
            if (group.nodeDsns != null) {
                for (String dsn : group.nodeDsns) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(dsn);
                }
            } else if (group.nodes != null) {
                for (AylaDeviceZigbeeNode node : group.nodes) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(node.dsn);
                }
            }
        }
        return sb.toString();
    }

    /**
     * Get a list of the AylaDeviceNode devices that are in a AylaGroupZigbee group. This method
     * is used to build the group.nodes array of an AylaGroupZigbee instance.
     * @param group AylaGroupZigbee
     * @return List of AylaDeviceNode devices in the group.
     */
    static public List<AylaDeviceNode> getDeviceNodes(AylaGroupZigbee group) {
        List<AylaDeviceNode> list = new ArrayList<AylaDeviceNode>();
        if (group != null) {
            // always try the String array first
            if (group.nodeDsns != null) {
                Logger.logDebug(LOG_TAG, "zg: getDevices [%s] nodeDsns", group.groupName);
                for (String nodeDsn : group.nodeDsns) {
                    Device d = SessionManager.deviceManager().deviceByDSN(nodeDsn);
                    if (d == null) {
                        Logger.logWarning(LOG_TAG, "zg: getDevices [%s] getDeviceByDSN [%s] NULL", group.groupName, nodeDsn);
                        continue;
                    }
                    Logger.logDebug(LOG_TAG, "zg: getDevices [%s] [%s]", group.groupName, nodeDsn);
                    list.add((AylaDeviceNode) d.getDevice());
                }
            } else if (group.nodes != null) {
                Logger.logDebug(LOG_TAG, "zg: getDevices [%s] nodes", group.groupName);
                for (AylaDeviceZigbeeNode dn : group.nodes) {
                    if (dn == null) {
                        Logger.logWarning(LOG_TAG, "zg: getDevices [%s] dn NULL", group.groupName);
                        continue;
                    }
                    if (TextUtils.equals(dn.action, "delete")) {
                        continue;
                    }
                    Device d = SessionManager.deviceManager().deviceByDSN(dn.dsn);
                    if (d == null) {
                        Logger.logWarning(LOG_TAG, "zg: getDevices [%s] getDeviceByDSN [%s] NULL", group.groupName, dn.dsn);
                        continue;
                    }
                    Logger.logDebug(LOG_TAG, "zg: getDevices [%s] [%s]", group.groupName, dn.dsn);
                    list.add(dn);
                }
            } else {
                Logger.logWarning(LOG_TAG, "zg: getDevices [%s] no nodes", group.groupName);
            }
        } else {
            Logger.logWarning(LOG_TAG, "zg: getDevices no group");
        }
        return list;
    }

    /**
     * Get a list of the Device devices that are in a AylaGroupZigbee group.
     * @param group AylaGroupZigbee
     * @return List of Device devices in the group.
     */
    static public List<Device> getDevices(AylaGroupZigbee group) {
        List<Device> list = new ArrayList<Device>();
        if (group != null) {
            // always try the String array first
            if (group.nodeDsns != null) {
                Logger.logDebug(LOG_TAG, "zg: getDevices [%s] nodeDsns", group.groupName);
                for (String nodeDsn : group.nodeDsns) {
                    Device d = SessionManager.deviceManager().deviceByDSN(nodeDsn);
                    if (d == null) {
                        Logger.logWarning(LOG_TAG, "zg: getDevices [%s] getDeviceByDSN [%s] NULL", group.groupName, nodeDsn);
                        continue;
                    }
                    Logger.logDebug(LOG_TAG, "zg: getDevices [%s] [%s]", group.groupName, nodeDsn);
                    list.add(d);
                }
            } else if (group.nodes != null) {
                Logger.logDebug(LOG_TAG, "zg: getDevices [%s] nodes", group.groupName);
                for (AylaDeviceZigbeeNode dn : group.nodes) {
                    if (dn == null) {
                        Logger.logWarning(LOG_TAG, "zg: getDevices [%s] dn NULL", group.groupName);
                        continue;
                    }
                    if (TextUtils.equals(dn.action, "delete")) {
                        continue;
                    }
                    Device d = SessionManager.deviceManager().deviceByDSN(dn.dsn);
                    if (d == null) {
                        Logger.logWarning(LOG_TAG, "zg: getDevices [%s] getDeviceByDSN [%s] NULL", group.groupName, dn.dsn);
                        continue;
                    }
                    Logger.logDebug(LOG_TAG, "zg: getDevices [%s] [%s]", group.groupName, dn.dsn);
                    list.add(d);
                }
            } else {
                Logger.logWarning(LOG_TAG, "zg: getDevices [%s] no nodes", group.groupName);
            }
        } else {
            Logger.logWarning(LOG_TAG, "zg: getDevices no group");
        }
        return list;
    }

    /**
     * Determine if a Device is in a AylaGroupZigbee.
     * @param device Device to search for.
     * @param group AylaGroupZigbee to search in
     * @return Return true if the device is in the group, false if it isn't.
     */
    static public boolean isDeviceInGroup(Device device, AylaGroupZigbee group) {
        String dsn = device.getDevice().dsn;
        // always try the String array first
        if (group.nodeDsns != null) {
            for (String nodeDsn : group.nodeDsns) {
                if (TextUtils.equals(dsn, nodeDsn)) {
                    return true;
                }
            }
        } else if (group.nodes != null) {
            for (AylaDeviceZigbeeNode dn : group.nodes) {
                if (dn == null) {
                    continue;
                }
                if (TextUtils.equals(dsn, dn.dsn)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void fetchZigbeeGroupsIfNeeded() {
        if ((_groups == null) || (_groups.size() == 0)) {
            fetchZigbeeGroups(null, null);
        }
    }

    /**
     * Fetches the list of groups from the server. Listeners will be notified when the call is
     * complete by being called with zigbeeGroupListChanged() if the group list has changed.
     * @param tag Optional user tag for completion handler.
     * @param completion Optional completion handler.
     */
    public void fetchZigbeeGroups(Object tag, Gateway.AylaGatewayCompletionHandler completion) {
        Map<String, Object> callParams = new HashMap<String, Object>();
        callParams.put(AylaNetworksZigbee.kAylaZigbeeNodeParamDetail, "true");          // default is "false"
        callParams.put(AylaNetworksZigbee.kAylaZigbeeNodeParamStatusFilter, "active");  // default is "active"
        AylaDeviceZigbeeGateway gateway = (AylaDeviceZigbeeGateway)_gateway.getDevice();
        gateway.getGroups(new GetGroupsHandler(this, _gateway, tag, completion), callParams, false);
    }

    public List<AylaGroupZigbee> getGroups() {
        return sortGroupSet(_groups);
    }

    /**
     * Get the AylaGroupZigbee group with the specified name.
     * @param name String name of the group to locate.
     * @return AylaGroupZigbee matching the specified name.
     */
    public AylaGroupZigbee getByName(String name) {
        if ((_groups != null) && (_groups.size() > 0)) {
            for (AylaGroupZigbee group : _groups) {
                if (TextUtils.equals(group.groupName, name)) {
                    return group;
                }
            }
        }
        return null;
    }

    public void createGroup(String name, List<Device>deviceList, Object tag, Gateway.AylaGatewayCompletionHandler handler) {
        AylaGroupZigbee group = new AylaGroupZigbee();
        group.groupName = name;
        group.gatewayDsn = _gateway.getDeviceDsn();
        if ((deviceList != null) && (deviceList.size() > 0)) {
            group.nodeDsns = new String[deviceList.size()];
            for (int i = 0; i < deviceList.size(); i++) {
                group.nodeDsns[i] = deviceList.get(i).getDevice().dsn;
            }
        }
        Map<String, Object> callParams = new HashMap<String, Object>();
        AylaDeviceZigbeeGateway gateway = (AylaDeviceZigbeeGateway)_gateway.getDevice();
        gateway.createGroup(new CreateHandler(this, name, tag, handler), group, callParams, false);
    }

    public void createGroup(AylaGroupZigbee group, Object tag, Gateway.AylaGatewayCompletionHandler handler) {
        Map<String, Object> callParams = new HashMap<String, Object>();
        AylaDeviceZigbeeGateway gateway = (AylaDeviceZigbeeGateway)_gateway.getDevice();
        gateway.createGroup(new CreateHandler(this, group.groupName, tag, handler), group, callParams, false);
    }

    public void updateGroup(AylaGroupZigbee group, Object tag, Gateway.AylaGatewayCompletionHandler handler) {
        Map<String, Object> callParams = new HashMap<String, Object>();
        AylaDeviceZigbeeGateway gateway = (AylaDeviceZigbeeGateway)_gateway.getDevice();
        gateway.updateGroup(new UpdateHandler(this, group, tag, handler), group, callParams, false);
    }

    public void deleteGroup(AylaGroupZigbee group, Object tag, Gateway.AylaGatewayCompletionHandler handler) {
        Map<String, Object> callParams = new HashMap<String, Object>();
        AylaDeviceZigbeeGateway gateway = (AylaDeviceZigbeeGateway)_gateway.getDevice();
        gateway.deleteGroup(new DeleteHandler(this, group.groupName, tag, handler), group, callParams, false);
    }

    // Internal Handlers

    static class GetGroupsHandler extends Handler {
        private WeakReference<ZigbeeGroupManager> _manager;
        Gateway _gateway;
        Object _tag;
        Gateway.AylaGatewayCompletionHandler _completion;

        GetGroupsHandler(ZigbeeGroupManager manager, Gateway gateway, Object tag, Gateway.AylaGatewayCompletionHandler completion) {
            _manager = new WeakReference<ZigbeeGroupManager>(manager);
            _gateway = gateway;
            _tag = tag;
            _completion = completion;
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.logMessage(LOG_TAG, msg, "zg: getGroups");
            if (AylaNetworks.succeeded(msg)) {
                Set<AylaGroupZigbee> groupSet = new HashSet<>();
                AylaGroupZigbee[] groups = AylaSystemUtils.gson.fromJson((String) msg.obj, AylaGroupZigbee[].class);
                if (groups != null) {
                    Logger.logDebug(LOG_TAG, "zg: getZigbeeGroups " + groups.length + " groups");
                    List<Device> devices = SessionManager.deviceManager().deviceList();
                    for (AylaGroupZigbee group : groups) {
                        if (group == null) {
                            continue;
                        }

                        // verify that it is an active group that we want to add
                        boolean add = true;
                        /* should we be doing this? I doubt it...
                        String prefix = null;
                        if (group.groupName.startsWith(ZigbeeTriggerDevice.GROUP_PREFIX_TRIGGER)) {
                            prefix = ZigbeeTriggerDevice.GROUP_PREFIX_TRIGGER;
                        } else if (group.groupName.startsWith(ZigbeeWirelessSwitch.GROUP_PREFIX_REMOTE)) {
                            prefix = ZigbeeWirelessSwitch.GROUP_PREFIX_REMOTE;
                        }
                        if (prefix != null) {
                            add = false;
                            for (Device device : devices) {
                                String key = prefix + device.getDevice().dsn;
                                if (group.groupName.startsWith(key)) {
                                    add = true;
                                    break;
                                }
                            }
                        }
                        */

                        if (add) {
                            Logger.logDebug(LOG_TAG, "zg: getZigbeeGroups + [" + group.groupName + "] [" + getGroupNodesToString(group) + "]");
                            Logger.logDebug(LOG_TAG, "zg: [%s]", group);
                            groupSet.add(group);
                        }
                    }
                }
                _manager.get().setGroups(groupSet);
            }
            if (_completion != null) {
                _completion.gatewayCompletion(_gateway, msg, _tag);
            }
        }
    }

    static class CreateHandler extends Handler {
        private WeakReference<ZigbeeGroupManager> _manager;
        private Object _tag;
        private Gateway.AylaGatewayCompletionHandler _handler;
        String _name;

        CreateHandler(ZigbeeGroupManager manager, String name, Object tag, Gateway.AylaGatewayCompletionHandler handler) {
            _manager = new WeakReference<ZigbeeGroupManager>(manager);
            _name = name;
            _tag = tag;
            _handler = handler;
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.logMessage(LOG_TAG, msg, "zg: createGroup [%s]", _name);
            if (_manager.get() == null) {
                Logger.logWarning(LOG_TAG, "zg: ZigbeeGroupManager went away.");
            }
            if (msg.what == AylaNetworks.AML_ERROR_OK) {
                if (msg.arg1 == 206) {
                    // has failing nodes...
                    Logger.logWarning(LOG_TAG, "zg: createGroup [%s] has failing nodes", _name);
                    _manager.get().notifyCreateCompleted(_name, msg, null);
                } else {
                    AylaGroupZigbee group = AylaSystemUtils.gson.fromJson((String) msg.obj, AylaGroupZigbee.class);
                    _manager.get().addGroup(group);
                    _manager.get().notifyCreateCompleted(_name, msg, group);
                }
            } else {
                _manager.get().notifyCreateCompleted(_name, msg, null);
            }
            if (_handler != null) {
                _handler.gatewayCompletion(_manager.get()._gateway, msg, _tag);
            }
        }
    }

    static class UpdateHandler extends Handler {
        private WeakReference<ZigbeeGroupManager> _manager;
        private Object _tag;
        private Gateway.AylaGatewayCompletionHandler _handler;
        private AylaGroupZigbee _group;
        String _name;

        UpdateHandler(ZigbeeGroupManager manager, AylaGroupZigbee group, Object tag, Gateway.AylaGatewayCompletionHandler handler) {
            _manager = new WeakReference<ZigbeeGroupManager>(manager);
            _group = group;
            _name = group.groupName;
            _tag = tag;
            _handler = handler;
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.logMessage(LOG_TAG, msg, "zg: updateGroup [%s]", _name);
            if (_manager.get() == null) {
                Logger.logWarning(LOG_TAG, "zg: ZigbeeGroupManager went away.");
            }
            if (msg.what == AylaNetworks.AML_ERROR_OK) {
                if (msg.arg1 == 206) {
                    // has failing nodes...
                    Logger.logWarning(LOG_TAG, "zg: updateGroup [%s] has failing nodes", _name);
                    _manager.get().notifyUpdateCompleted(_name, msg, null);
                } else {
                    AylaGroupZigbee group = AylaSystemUtils.gson.fromJson((String) msg.obj, AylaGroupZigbee.class);
                    _manager.get().updateGroup(group);
                    _manager.get().notifyUpdateCompleted(_name, msg, group);
                }
            } else {
                _manager.get().notifyUpdateCompleted(_name, msg, null);
            }
            if (_handler != null) {
                _handler.gatewayCompletion(_manager.get()._gateway, msg, _tag);
            }
        }
    }

    static class DeleteHandler extends Handler {
        private WeakReference<ZigbeeGroupManager> _manager;
        String _name;
        private Object _tag;
        private Gateway.AylaGatewayCompletionHandler _handler;

        DeleteHandler(ZigbeeGroupManager manager, String name, Object tag, Gateway.AylaGatewayCompletionHandler handler) {
            _manager = new WeakReference<ZigbeeGroupManager>(manager);
            _tag = tag;
            _handler = handler;
            _name = name;
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.logMessage(LOG_TAG, msg, "zg: deleteGroup [%s]", _name);
            if (_manager.get() == null) {
                Logger.logWarning(LOG_TAG, "zg: ZigbeeGroupManager went away.");
            }
            if (msg.what == AylaNetworks.AML_ERROR_OK) {
                if (msg.arg1 == 206) {
                    // has failing nodes...
                    Logger.logWarning(LOG_TAG, "zg: deleteGroup [%s] has failing nodes", _name);
                    _manager.get().notifyDeleteCompleted(_name, msg);
                } else {
                    _manager.get().removeGroupByName(_name);
                    _manager.get().notifyDeleteCompleted(_name, msg);
                }
            } else {
                if (msg.arg1 == 404) {
                    // treat it like success, since it is now gone...
                    msg.what = AylaNetworks.AML_ERROR_OK;
                    _manager.get().removeGroupByName(_name);
                }
                _manager.get().notifyDeleteCompleted(_name, msg);
            }
            if (_handler != null) {
                _handler.gatewayCompletion(_manager.get()._gateway, msg, _tag);
            }
        }
    }

    // Internal Methods

    private List<AylaGroupZigbee> sortGroupSet(Set<AylaGroupZigbee> groupSet) {
        List<AylaGroupZigbee> groups = new ArrayList<>(groupSet);
        Collections.sort(groups, new Comparator<AylaGroupZigbee>() {
            @Override
            public int compare(AylaGroupZigbee lhs, AylaGroupZigbee rhs) {
                return lhs.groupName.compareToIgnoreCase(rhs.groupName);
            }
        });
        return groups;
    }

    /**
     * Compare two groups to see if they are the same.
     * @param group1 AylaGroupZigbee
     * @param group2 AylaGroupZigbee
     * @return Returns true if they are the same, false if they are not
     */
    public static boolean isGroupSame(AylaGroupZigbee group1, AylaGroupZigbee group2) {
        if (!TextUtils.equals(group1.groupHexId, group2.groupHexId)) {
            return false;
        }
        if (!TextUtils.equals(group1.groupName, group2.groupName)) {
            return false;
        }
        if (!TextUtils.equals(group1.action, group2.action)) {
            return false;
        }
        if (!TextUtils.equals(group1.status, group2.status)) {
            return false;
        }
        if (!TextUtils.equals(group1.toString(), group2.toString())) {
            return false;
        }
        return true;
    }

    private boolean groupListChanged(Set<AylaGroupZigbee> newGroupSet) {
        if ((_groups == null) && (newGroupSet == null)) {
            return false;
        }
        if ((_groups == null) && (newGroupSet != null)) {
            return true;
        }
        if ((_groups != null) && (newGroupSet == null)) {
            return true;
        }
        if (_groups.size() != newGroupSet.size()) {
            return true;
        }
        List<AylaGroupZigbee> groups1 = sortGroupSet(_groups);
        List<AylaGroupZigbee> groups2 = sortGroupSet(newGroupSet);
        for (int i = 0; i < groups1.size(); i++) {
            AylaGroupZigbee group1 = groups1.get(i);
            AylaGroupZigbee group2 = groups2.get(i);
            if (!isGroupSame(group1, group2)) {
                return true;
            }
        }
        return false;
    }

    private void setGroups(Set<AylaGroupZigbee> groupSet) {
        if (groupListChanged(groupSet)) {
            _groups = groupSet;
            notifyListChanged();
        }
    }

    private void addGroup(AylaGroupZigbee group) {
        // add to internal list
        for (AylaGroupZigbee g : _groups) {
            if (g.groupName.equals(group.groupName)) {
                return;
            }
        }
        _groups.add(group);
        notifyListChanged();
    }

    private void updateGroup(AylaGroupZigbee group) {
        for (AylaGroupZigbee g : _groups) {
            if (g.groupName.equals(group.groupName)) {
                _groups.remove(g);
                break;
            }
        }
        _groups.add(group);
        notifyListChanged();
    }

    private void removeGroupByName(String groupName) {
        // remove from internal list
        for (AylaGroupZigbee g : _groups) {
            if (g.groupName.equals(groupName)) {
                _groups.remove(g);
                notifyListChanged();
                return;
            }
        }
    }

    protected void notifyListChanged() {
        for ( ZigbeeGroupManagerListener l : _listeners ) {
            l.zigbeeGroupListChanged(_gateway);
        }
    }

    protected void notifyMembersChanged(AylaGroupZigbee group) {
        for ( ZigbeeGroupManagerListener l : _listeners ) {
            l.zigbeeGroupMembersChanged(_gateway, group);
        }
    }

    protected void notifyCreateCompleted(String name, Message msg, AylaGroupZigbee group) {
        for ( ZigbeeGroupManagerListener l : _listeners ) {
            l.zigbeeCreateGroupCompleted(_gateway, name, msg, group);
        }
    }

    protected void notifyUpdateCompleted(String name, Message msg, AylaGroupZigbee group) {
        for ( ZigbeeGroupManagerListener l : _listeners ) {
            l.zigbeeUpdateGroupCompleted(_gateway, name, msg, group);
        }
    }

    protected void notifyDeleteCompleted(String name, Message msg) {
        for ( ZigbeeGroupManagerListener l : _listeners ) {
            l.zigbeeDeleteGroupCompleted(_gateway, name, msg);
        }
    }
}
