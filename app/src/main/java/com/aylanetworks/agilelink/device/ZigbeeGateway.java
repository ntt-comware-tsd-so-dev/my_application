package com.aylanetworks.agilelink.device;
/* 
 * ZigbeeGateway
 * com.aylanetworks.agilelink.device
 * Agile_Link_Android
 *
 * Created by David N. Junod on 7/8/15.
 * Copyright (c) 2015 Reality Check, Inc. All rights reserved.
 */

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaDeviceNode;
import com.aylanetworks.aaml.zigbee.AylaBindingZigbee;
import com.aylanetworks.aaml.zigbee.AylaDeviceZigbeeGateway;
import com.aylanetworks.aaml.zigbee.AylaDeviceZigbeeNode;
import com.aylanetworks.aaml.zigbee.AylaGroupZigbee;
import com.aylanetworks.aaml.zigbee.AylaSceneZigbee;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.DeviceManager;
import com.aylanetworks.agilelink.framework.Gateway;
import com.aylanetworks.agilelink.framework.Logger;
import com.aylanetworks.agilelink.framework.ZigbeeBindingManager;
import com.aylanetworks.agilelink.framework.ZigbeeGroupManager;
import com.aylanetworks.agilelink.framework.ZigbeeSceneManager;

import java.util.ArrayList;
import java.util.List;

public class ZigbeeGateway extends Gateway {

    private final static String LOG_TAG = "ZigbeeGateway";

    public ZigbeeGateway(AylaDevice aylaDevice) {
        super(aylaDevice);
    }

    public AylaDeviceZigbeeGateway getZigbeeGatewayDevice() {
        return (AylaDeviceZigbeeGateway)getDevice();
    }


    @Override
    public boolean isZigbeeGateway() {
        return true;
    }

    @Override
    public String deviceTypeName() {
        return "Zigbee Gateway";
    }

    @Override
    public Drawable getDeviceDrawable(Context c) {
        return c.getResources().getDrawable(R.drawable.ic_zigbee);
    }

    @Override
    public void deviceAdded(Device oldDevice) {
        super.deviceAdded(oldDevice);
        if (oldDevice != null) {
            Logger.logDebug(LOG_TAG, "zg: deviceAdded [%s] copy from old", getDeviceDsn());
            ZigbeeGateway gateway = (ZigbeeGateway)oldDevice;
            _groupManager = gateway._groupManager;
            _bindingManager = gateway._bindingManager;
            _sceneManager = gateway._sceneManager;
        } else {
            Logger.logDebug(LOG_TAG, "zg: deviceAdded [%s] new", getDeviceDsn());
        }
        getGroupManager().fetchZigbeeGroupsIfNeeded();
        getBindingManager().fetchZigbeeBindingsIfNeeded();
        getSceneManager().fetchZigbeeScenesIfNeeded();
    }

    private ZigbeeGroupManager _groupManager;

    public ZigbeeGroupManager getGroupManager() {
        if (_groupManager == null) {
            _groupManager = new ZigbeeGroupManager(this);
        }
        return _groupManager;
    }

    public List<AylaGroupZigbee> getGroups() {
        return getGroupManager().getGroups();
    }

    public AylaGroupZigbee getGroupByName(String name) {
        return getGroupManager().getByName(name);
    }

    private ZigbeeBindingManager _bindingManager;

    public ZigbeeBindingManager getBindingManager() {
        if (_bindingManager == null) {
            _bindingManager = new ZigbeeBindingManager(this);
        }
        return _bindingManager;
    }

    public List<AylaBindingZigbee> getBindings() {
        return getBindingManager().getBindings();
    }

    public AylaBindingZigbee getBindingByName(String name) {
        return getBindingManager().getByName(name);
    }

    private ZigbeeSceneManager _sceneManager;

    public ZigbeeSceneManager getSceneManager() {
        if (_sceneManager == null) {
            _sceneManager = new ZigbeeSceneManager(this);
        }
        return _sceneManager;
    }

    public void fetchScenes(Object tag, AylaGatewayCompletionHandler completion) {
        getSceneManager().fetchZigbeeScenes(tag, completion);
    }

    public List<AylaSceneZigbee> getScenes() {
        return getSceneManager().getScenes();
    }

    public AylaSceneZigbee getSceneByName(String name) {
        return getSceneManager().getByName(name);
    }

    public List<Device> getDevicesForScene(AylaSceneZigbee scene) {
        return getSceneManager().getDevices(scene);
    }

    public void createGroup(String groupName, List<Device> devices, Object tag, AylaGatewayCompletionHandler handler) {
        getGroupManager().createGroup(groupName, devices, tag, handler);
    }

    public void createGroup(AylaGroupZigbee group, Object tag, AylaGatewayCompletionHandler handler) {
        getGroupManager().createGroup(group, tag, handler);
    }

    public void updateGroup(AylaGroupZigbee group, Object tag, AylaGatewayCompletionHandler handler) {
        getGroupManager().updateGroup(group, tag, handler);
    }

    public void addDevicesToGroup(AylaGroupZigbee group, List<Device> list, Object tag, AylaGatewayCompletionHandler handler) {
        List<AylaDeviceNode> devices = ZigbeeGroupManager.getDeviceNodes(group);
        // add list to devices
        for (Device device : list) {
            AylaDeviceNode adn = (AylaDeviceNode)device.getDevice();
            // make sure it isn't already in the list
            if (!DeviceManager.isDsnInAylaDeviceNodeList(adn.dsn, devices)) {
                devices.add(adn);
            }
        }
        group.nodeDsns = new String[devices.size()];
        group.nodes = new AylaDeviceZigbeeNode[devices.size()];
        for (int i = 0; i < devices.size(); i++) {
            group.nodeDsns[i] = devices.get(i).dsn;
            group.nodes[i] = (AylaDeviceZigbeeNode)devices.get(i);
        }
        updateGroup(group, tag, handler);
    }

    public void removeDevicesFromGroup(AylaGroupZigbee group, List<Device> list, Object tag, AylaGatewayCompletionHandler handler) {
        // remove list from devices
        List<AylaDeviceNode> current = ZigbeeGroupManager.getDeviceNodes(group);
        List<AylaDeviceNode> devices = new ArrayList<AylaDeviceNode>();
        for (AylaDeviceNode adn : current) {
            if (!DeviceManager.isDsnInDeviceList(adn.dsn, list)) {
                devices.add(adn);
            }
        }
        group.nodeDsns = new String[devices.size()];
        group.nodes = new AylaDeviceZigbeeNode[devices.size()];
        for (int i = 0; i < devices.size(); i++) {
            group.nodeDsns[i] = devices.get(i).dsn;
            group.nodes[i] = (AylaDeviceZigbeeNode)devices.get(i);
        }
        updateGroup(group, tag, handler);
    }

    public void deleteGroup(AylaGroupZigbee group, Object tag, AylaGatewayCompletionHandler handler) {
        getGroupManager().deleteGroup(group, tag, handler);
    }

    public void createBinding(AylaBindingZigbee binding, Object tag, AylaGatewayCompletionHandler handler) {
        getBindingManager().createBinding(binding, tag, handler);
    }

    public void deleteBinding(AylaBindingZigbee binding, Object tag, AylaGatewayCompletionHandler handler) {
        getBindingManager().deleteBinding(binding, tag, handler);
    }

    public void createScene(String sceneName, List<Device> devices, Object tag, AylaGatewayCompletionHandler handler) {
        getSceneManager().createScene(sceneName, devices, tag, handler);
    }

    public void createScene(AylaSceneZigbee scene, Object tag, AylaGatewayCompletionHandler handler) {
        getSceneManager().createScene(scene, tag, handler);
    }

    public void updateScene(AylaSceneZigbee scene, List<Device> devices, Object tag, AylaGatewayCompletionHandler handler) {
        getSceneManager().updateScene(scene, devices, tag, handler);
    }

    public void updateSceneDevices(AylaSceneZigbee scene, List<Device> devices, Object tag, AylaGatewayCompletionHandler handler) {
        updateScene(scene, devices, tag, handler);
    }

    public void recallScene(AylaSceneZigbee scene, Object tag, AylaGatewayCompletionHandler handler) {
        getSceneManager().recallScene(scene, tag, handler);
    }

    public void deleteScene(AylaSceneZigbee scene, Object tag, AylaGatewayCompletionHandler handler) {
        getSceneManager().deleteScene(scene, tag, handler);
    }


}
