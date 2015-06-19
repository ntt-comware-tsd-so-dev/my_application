package com.aylanetworks.agilelink.device;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Message;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.zigbee.AylaBindingZigbee;
import com.aylanetworks.aaml.zigbee.AylaGroupZigbee;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.Gateway;
import com.aylanetworks.agilelink.framework.Logger;
import com.aylanetworks.agilelink.framework.ZigbeeGroupManager;

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

    public final static String GROUP_PREFIX_REMOTE = "remote-";

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
        String name = GROUP_PREFIX_REMOTE + getDeviceDsn();
        AylaGroupZigbee group = gateway.getGroupManager().getByName(name);
        if (group == null) {
            Logger.logError(LOG_TAG, "zg: initializeRemoteBinding no group [%s]", name);
            return;
        }

        AylaBindingZigbee binding = gateway.getBindingManager().getByName(name);
        if (binding == null) {
            binding = new AylaBindingZigbee();
            binding.bindingName = name;
            binding.gatewayDsn = gateway.getDeviceDsn();
            binding.fromId = getDeviceDsn();
            binding.fromName = PROPERTY_ZB_REMOTE_SWITCH;
            binding.toId = group.getId().toString();
            binding.toName = name;
            binding.isGroup = true;
            Logger.logDebug(LOG_TAG, "zg: initializeRemoteBinding [%s] createBinding [%s:%s:%s]", getDeviceDsn(), binding.bindingName, binding.toName, binding.fromId);
            gateway.createBinding(binding, this, new Gateway.AylaGatewayCompletionHandler() {
                @Override
                public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                    if (AylaNetworks.succeeded(msg)) {
                        // all done binding the remote
                        Logger.logInfo(LOG_TAG, "zg: initializeRemote [%s] on gateway [%s] success", ((Device) tag).getDeviceDsn(), gateway.getDeviceDsn());
                    } else {
                        // failed :(
                        Logger.logError(LOG_TAG, "zg: initializeRemote [%s] on gateway [%s] failed", ((Device) tag).getDeviceDsn(), gateway.getDeviceDsn());
                    }
                }
            });
        }
    }

    private void initializeRemoteGroup(Gateway gateway) {
        String name = GROUP_PREFIX_REMOTE + getDeviceDsn();
        AylaGroupZigbee group = gateway.getGroupManager().getByName(name);
        if (group == null) {
            Logger.logDebug(LOG_TAG, "zg: initializeRemoteGroup [%s] createGroup [%s]", getDeviceDsn(), name);
            gateway.createGroup(name, null, this, new Gateway.AylaGatewayCompletionHandler() {
                @Override
                public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                    if (AylaNetworks.succeeded(msg)) {
                        initializeRemoteBinding(gateway);
                    } else {
                        // failed :(
                        Logger.logError(LOG_TAG, "zg: initializeRemote [%s] on gateway [%s] failed", ((Device) tag).getDeviceDsn(), gateway.getDeviceDsn());
                    }
                }
            });
        } else {
            initializeRemoteBinding(gateway);
        }
    }

    private void removeRemoteBinding(Gateway gateway) {
        String name = GROUP_PREFIX_REMOTE + getDeviceDsn();
        AylaBindingZigbee binding = gateway.getBindingManager().getByName(name);
        if (binding != null) {
            Logger.logDebug(LOG_TAG, "zg: removeRemoteGroup [%s] deleteBinding [%s]", getDeviceDsn(), name);
            gateway.deleteBinding(binding, this, new Gateway.AylaGatewayCompletionHandler() {
                @Override
                public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                    if (AylaNetworks.succeeded(msg)) {
                        Logger.logInfo(LOG_TAG, "zg: unregisterRemote [%s] on gateway [%s] succeeded", ((Device) tag).getDeviceDsn(), gateway.getDeviceDsn());
                    } else {
                        // failed :(
                        Logger.logError(LOG_TAG, "zg: removeRemoteBinding [%s] on gateway [%s] failed", ((Device) tag).getDeviceDsn(), gateway.getDeviceDsn());
                    }
                }
            });
        } else {
            Logger.logInfo(LOG_TAG, "zg: unregisterRemote [%s] on gateway [%s] succeeded", getDeviceDsn(), gateway.getDeviceDsn());
        }
    }

    private void removeRemoteGroup(Gateway gateway) {
        String name = GROUP_PREFIX_REMOTE + getDeviceDsn();
        AylaGroupZigbee group = gateway.getGroupManager().getByName(name);
        if (group == null) {
            removeRemoteBinding(gateway);
        } else {
            Logger.logDebug(LOG_TAG, "zg: removeRemoteGroup [%s] deleteGroup [%s]", getDeviceDsn(), name);
            gateway.deleteGroup(group, this, new Gateway.AylaGatewayCompletionHandler() {
                @Override
                public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                    if (AylaNetworks.succeeded(msg)) {
                        removeRemoteBinding(gateway);
                    } else {
                        // failed :(
                        Logger.logError(LOG_TAG, "zg: removeRemoteGroup [%s] on gateway [%s] failed", ((Device) tag).getDeviceDsn(), gateway.getDeviceDsn());
                    }
                }
            });
        }
    }

    @Override
    public void postRegistrationForGatewayDevice(Gateway gateway) {
        // we need to create 1 group & 1 binding
        Logger.logInfo(LOG_TAG, "zg: initializeRemote [%s] on gateway [%s]", this.getDeviceDsn(), gateway.getDeviceDsn());
        initializeRemoteGroup(gateway);
    }

    @Override
    public void preUnregistrationForGatewayDevice(Gateway gateway) {
        Logger.logInfo(LOG_TAG, "zg: unregisterDevice [%s] on gateway [%s]", this.getDeviceDsn(), gateway.getDeviceDsn());
        removeRemoteGroup(gateway);
    }

    @Override
    public void deviceAdded(Device oldDevice) {
        super.deviceAdded(oldDevice);
        _gateway = Gateway.getGatewayForDeviceNode(this);
        Logger.logInfo(LOG_TAG, "rm: deviceAdded [%s] on gateway [%s]", this.getDeviceDsn(), _gateway.getDeviceDsn());
    }

    @Override
    public void deviceRemoved() {
        super.deviceRemoved();
        Logger.logInfo(LOG_TAG, "rm: deviceRemoved [%s]", this.getDeviceDsn());
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
        String name = GROUP_PREFIX_REMOTE + getDeviceDsn();
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
        String name = GROUP_PREFIX_REMOTE + getDeviceDsn();
        AylaGroupZigbee group = gateway.getGroupManager().getByName(name);
        gateway.addDevicesToGroup(group, list, this, new Gateway.AylaGatewayCompletionHandler() {
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
        String name = GROUP_PREFIX_REMOTE + getDeviceDsn();
        AylaGroupZigbee group = gateway.getGroupManager().getByName(name);
        gateway.removeDevicesFromGroup(group, list, this, new Gateway.AylaGatewayCompletionHandler() {
            @Override
            public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                Logger.logMessage(LOG_TAG, msg, "rm: unpairDevices complete");
                completion.handle(ZigbeeWirelessSwitch.this, msg, userTag);
            }
        });
    }
}
