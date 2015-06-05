package com.aylanetworks.agilelink.device;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaDeviceNode;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.zigbee.AylaBindingZigbee;
import com.aylanetworks.aaml.zigbee.AylaDeviceZigbeeNode;
import com.aylanetworks.aaml.zigbee.AylaGroupZigbee;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.DeviceManager;
import com.aylanetworks.agilelink.framework.Gateway;
import com.aylanetworks.agilelink.framework.Logger;
import com.aylanetworks.agilelink.framework.ZigbeeGroupManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * ZigbeeWirelessSwitch.java
 * AgileLink Application Framework
 *
 * Created by David Junod on 5/21/15
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class ZigbeeWirelessSwitch extends Device implements RemoteSwitchDevice {

    private final static String LOG_TAG = "ZigbeeWirelessSwitch";

    public final static String PROPERTY_ZB_OUTPUT = "1_out_0x0006_0x0000";
    public final static String PROPERTY_ZB_REMOTE_SWITCH = PROPERTY_ZB_OUTPUT;

    private final static String REMOTE_GROUP_BINDNG_NAME_PREFIX = "remote-";

    Gateway _gateway;
    AylaGroupZigbee _group;

    public ZigbeeWirelessSwitch(AylaDevice device) {
        super(device);
    }

    @Override
    public String getObservablePropertyName() {
        return PROPERTY_ZB_REMOTE_SWITCH;
    }

    @Override
    public String friendlyNameForPropertyName(String propertyName) {
        if (propertyName.equals(PROPERTY_ZB_REMOTE_SWITCH)) {
            return MainActivity.getInstance().getString(R.string.property_remote_switch_friendly_name);
        }
        return super.friendlyNameForPropertyName(propertyName);
    }

    @Override
    public String deviceTypeName() {
        return "Wireless Switch";
    }

    @Override
    public Drawable getDeviceDrawable(Context c) {
        return c.getResources().getDrawable(R.drawable.ic_remote_red);
    }

    @Override
    public boolean isDeviceNode() {
        return true;
    }

    @Override
    public int hasPostRegistrationProcessingResourceId() {
        return R.string.add_device_sensor_warning;
    }


    private void initializeRemoteBinding(Gateway gateway) {
        String name = REMOTE_GROUP_BINDNG_NAME_PREFIX + getDevice().dsn;
        AylaGroupZigbee group = gateway.getGroupManager().getByName(name);
        if (group == null) {
            Logger.logError(LOG_TAG, "zg: initializeRemoteBinding no group [%s]", name);
            return;
        }

        AylaBindingZigbee binding = gateway.getBindingManager().getByName(name);
        if (binding == null) {
            binding = new AylaBindingZigbee();
            binding.bindingName = name;
            binding.gatewayDsn = gateway.getDevice().dsn;
            binding.fromId = getDevice().dsn;
            binding.fromName = PROPERTY_ZB_REMOTE_SWITCH;
            binding.toId = group.getId().toString();
            binding.toName = name;
            binding.isGroup = true;
            Logger.logDebug(LOG_TAG, "zg: initializeRemoteBinding [%s] createBinding [%s:%s:%s]", getDevice().dsn, binding.bindingName, binding.toName, binding.fromId);
            gateway.createBinding(binding, this, new Gateway.AylaGatewayCompletionHandler() {
                @Override
                public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                    if (AylaNetworks.succeeded(msg)) {
                        // all done binding the remote
                        Logger.logInfo(LOG_TAG, "zg: initializeRemote [%s] on gateway [%s] success", ((Device) tag).getDevice().dsn, gateway.getDevice().dsn);
                    } else {
                        // failed :(
                        Logger.logError(LOG_TAG, "zg: initializeRemote [%s] on gateway [%s] failed", ((Device) tag).getDevice().dsn, gateway.getDevice().dsn);
                    }
                }
            });
        }
    }

    private void initializeRemoteGroup(Gateway gateway) {
        String name = REMOTE_GROUP_BINDNG_NAME_PREFIX + getDevice().dsn;
        AylaGroupZigbee group = gateway.getGroupManager().getByName(name);
        if (group == null) {
            Logger.logDebug(LOG_TAG, "zg: initializeRemoteGroup [%s] createGroup [%s]", getDevice().dsn, name);
            gateway.createGroup(name, null, this, new Gateway.AylaGatewayCompletionHandler() {
                @Override
                public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                    if (AylaNetworks.succeeded(msg)) {
                        initializeRemoteBinding(gateway);
                    } else {
                        // failed :(
                        Logger.logError(LOG_TAG, "zg: initializeRemote [%s] on gateway [%s] failed", ((Device) tag).getDevice().dsn, gateway.getDevice().dsn);
                    }
                }
            });
        } else {
            initializeRemoteBinding(gateway);
        }
    }

    private void removeRemoteBinding(Gateway gateway) {
        String name = REMOTE_GROUP_BINDNG_NAME_PREFIX + getDevice().dsn;
        AylaBindingZigbee binding = gateway.getBindingManager().getByName(name);
        if (binding != null) {
            Logger.logDebug(LOG_TAG, "zg: removeRemoteGroup [%s] deleteBinding [%s]", getDevice().dsn, name);
            gateway.deleteBinding(binding, this, new Gateway.AylaGatewayCompletionHandler() {
                @Override
                public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                    if (AylaNetworks.succeeded(msg)) {
                        Logger.logInfo(LOG_TAG, "zg: unregisterRemote [%s] on gateway [%s] succeeded", ((Device) tag).getDevice().dsn, gateway.getDevice().dsn);
                    } else {
                        // failed :(
                        Logger.logError(LOG_TAG, "zg: removeRemoteBinding [%s] on gateway [%s] failed", ((Device) tag).getDevice().dsn, gateway.getDevice().dsn);
                    }
                }
            });
        } else {
            Logger.logInfo(LOG_TAG, "zg: unregisterRemote [%s] on gateway [%s] succeeded", getDevice().dsn, gateway.getDevice().dsn);
        }
    }

    private void removeRemoteGroup(Gateway gateway) {
        String name = REMOTE_GROUP_BINDNG_NAME_PREFIX + getDevice().dsn;
        AylaGroupZigbee group = gateway.getGroupManager().getByName(name);
        if (group == null) {
            removeRemoteBinding(gateway);
        } else {
            Logger.logDebug(LOG_TAG, "zg: removeRemoteGroup [%s] deleteGroup [%s]", getDevice().dsn, name);
            gateway.deleteGroup(group, this, new Gateway.AylaGatewayCompletionHandler() {
                @Override
                public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                    if (AylaNetworks.succeeded(msg)) {
                        removeRemoteBinding(gateway);
                    } else {
                        // failed :(
                        Logger.logError(LOG_TAG, "zg: removeRemoteGroup [%s] on gateway [%s] failed", ((Device) tag).getDevice().dsn, gateway.getDevice().dsn);
                    }
                }
            });
        }
    }

    @Override
    public void postRegistrationForGatewayDevice(Gateway gateway) {
        // we need to create 1 group & 1 binding
        Logger.logInfo(LOG_TAG, "zg: initializeRemote [%s] on gateway [%s]", this.getDevice().dsn, gateway.getDevice().dsn);
        initializeRemoteGroup(gateway);
    }

    @Override
    public void deviceAdded(Device oldDevice) {
        super.deviceAdded(oldDevice);
        _gateway = Gateway.getGatewayForDeviceNode(this);
        Logger.logInfo(LOG_TAG, "rm: deviceAdded [%s] on gateway [%s]", this.getDevice().dsn, _gateway.getDevice().dsn);
    }

    @Override
    public void deviceRemoved() {
        super.deviceRemoved();
        Logger.logInfo(LOG_TAG, "rm: deviceRemoved [%s]", this.getDevice().dsn);
    }

    @Override
    public void unregisterDevice(Handler handler) {
        super.unregisterDevice(handler);
        Gateway gateway = Gateway.getGatewayForDeviceNode(this);
        Logger.logInfo(LOG_TAG, "zg: unregisterDevice [%s] on gateway [%s]", this.getDevice().dsn, gateway.getDevice().dsn);
        removeRemoteGroup(gateway);
    }

    @Override
    public boolean isPairableDevice(Device device) {
        if (device != null) {
            // made simple because all of them are of the switched class
            if (device instanceof ZigbeeSwitchedDevice) {
                return true;
            }
        }
        return false;
    }

    AylaGroupZigbee getGroup() {
        Gateway gateway = Gateway.getGatewayForDeviceNode(this);
        String name = REMOTE_GROUP_BINDNG_NAME_PREFIX + getDevice().dsn;
        return gateway.getGroupManager().getByName(name);
    }

    @Override
    public boolean isDevicePaired(Device device) {
        return ZigbeeGroupManager.isDeviceInGroup(device, getGroup());
    }

    @Override
    public List<Device> getPairedDevices() {
        return ZigbeeGroupManager.getDevices(getGroup());
    }

    @Override
    public void pairDevice(Device device, Object tag, RemoteSwitchCompletionHandler completion) {
        pairDevices(Arrays.asList(device), tag, completion);
    }

    @Override
    public void pairDevices(List<Device> list, final Object userTag, final RemoteSwitchCompletionHandler completion) {

        Logger.logInfo(LOG_TAG, "rm: pairDevices start");

        Gateway gateway = Gateway.getGatewayForDeviceNode(this);
        String name = REMOTE_GROUP_BINDNG_NAME_PREFIX + getDevice().dsn;
        AylaGroupZigbee group = gateway.getGroupManager().getByName(name);

        List<AylaDeviceNode> devices = ZigbeeGroupManager.getDeviceNodes(group);
        // add list to devices
        for (Device device : list) {
            AylaDeviceNode adn = (AylaDeviceNode)device.getDevice();
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
        gateway.updateGroup(group, this, new Gateway.AylaGatewayCompletionHandler() {
            @Override
            public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                Logger.logMessage(LOG_TAG, msg, "rm: pairDevices complete");
                completion.handle(ZigbeeWirelessSwitch.this, msg, userTag);
            }
        });
    }

    @Override
    public void unpairDevice(Device device, Object tag, RemoteSwitchCompletionHandler completion) {
        unpairDevices(Arrays.asList(device), tag,completion);
    }

    @Override
    public void unpairDevices(List<Device> list, final Object userTag, final RemoteSwitchCompletionHandler completion) {

        Logger.logDebug(LOG_TAG, "rm: unpairDevices start");

        Gateway gateway = Gateway.getGatewayForDeviceNode(this);
        String name = REMOTE_GROUP_BINDNG_NAME_PREFIX + getDevice().dsn;
        AylaGroupZigbee group = gateway.getGroupManager().getByName(name);

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
        gateway.updateGroup(group, this, new Gateway.AylaGatewayCompletionHandler() {
            @Override
            public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                Logger.logMessage(LOG_TAG, msg, "rm: unpairDevices complete");
                completion.handle(ZigbeeWirelessSwitch.this, msg, userTag);
            }
        });
    }
}
