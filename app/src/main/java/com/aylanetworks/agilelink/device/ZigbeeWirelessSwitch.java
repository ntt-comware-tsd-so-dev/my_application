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
import com.aylanetworks.agilelink.framework.ZigbeeGateway;
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

public class ZigbeeWirelessSwitch extends GenericDevice implements RemoteSwitchDevice {

    private final static String LOG_TAG = "ZigbeeWirelessSwitch";

    public final static String PROPERTY_ZB_OUTPUT = "1_out_0x0006_0x0000";
    public final static String PROPERTY_ZB_REMOTE_SWITCH = PROPERTY_ZB_OUTPUT;

    public final static String GROUP_PREFIX_REMOTE = "remote-";

    ZigbeeGateway _gateway;

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

    public String makeKeyForDevice() {
        return GROUP_PREFIX_REMOTE + getDeviceDsn();
    }

    public AylaGroupZigbee getRemoteGroup() {
        return _gateway.getGroupManager().getByName(makeKeyForDevice());
    }

    public AylaBindingZigbee getRemoteBinding() {
        return _gateway.getBindingManager().getByName(makeKeyForDevice());
    }

    class InitializeRemote {

        ZigbeeGateway _gateway;
        ZigbeeWirelessSwitch _device;
        Object _userTag;
        Gateway.AylaGatewayCompletionHandler _completion;

        Message _msg;

        public InitializeRemote(ZigbeeGateway gateway, ZigbeeWirelessSwitch device, Object userTag, Gateway.AylaGatewayCompletionHandler completion) {
            _gateway = gateway;
            _device = device;
            _userTag = userTag;
            _completion = completion;
        }

        public void initializeRemoteBinding () {
            String name = makeKeyForDevice();
            AylaGroupZigbee group = getRemoteGroup();
            if (group == null) {
                Logger.logError(LOG_TAG, "zg: initializeRemoteBinding no group [%s]", name);
                stop(null);
                return;
            }
            AylaBindingZigbee binding = getRemoteBinding();
            if (binding == null) {
                binding = new AylaBindingZigbee();
                binding.bindingName = name;
                binding.gatewayDsn = _gateway.getDeviceDsn();
                binding.fromId = getDeviceDsn();
                binding.fromName = PROPERTY_ZB_REMOTE_SWITCH;
                binding.toId = group.getId().toString();
                binding.toName = name;
                binding.isGroup = true;
                Logger.logDebug(LOG_TAG, "zg: initializeRemoteBinding [%s] createBinding [%s:%s:%s]", getDeviceDsn(), binding.bindingName, binding.toName, binding.fromId);
                _gateway.createBinding(binding, this, new Gateway.AylaGatewayCompletionHandler() {
                    @Override
                    public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                        stop(msg);
                    }
                });
            }
        }

        public void initializeRemoteGroup() {
            String name = makeKeyForDevice();
            AylaGroupZigbee group = getRemoteGroup();
            if (group == null) {
                Logger.logDebug(LOG_TAG, "zg: initializeRemoteGroup [%s] createGroup [%s]", getDeviceDsn(), name);
                _gateway.createGroup(name, null, this, new Gateway.AylaGatewayCompletionHandler() {
                    @Override
                    public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                        InitializeRemote setup = (InitializeRemote)tag;
                        if (AylaNetworks.succeeded(msg)) {
                            initializeRemoteBinding();
                        } else {
                            // failed :(
                            stop(msg);
                        }
                    }
                });
            } else {
                initializeRemoteBinding();
            }
        }

        public void start() {
            initializeRemoteGroup();
        }

        public void stop(Message msg) {
            _msg = msg;
            _device.setupRemoteComplete(this);
        }

        public Gateway getGateway() {
            return _gateway;
        }

        public boolean isSuccessful() {
            if (_msg == null) {
                return false;
            }
            return AylaNetworks.succeeded(_msg);
        }

        public void complete() {
            if (_completion != null) {
                Message msg = new Message();
                msg.what = isSuccessful() ? AylaNetworks.AML_ERROR_OK : AylaNetworks.AML_ERROR_FAIL;
                _completion.gatewayCompletion(_gateway, msg, _userTag);
            }
        }
    }

    Object _currentLock = new Object();
    InitializeRemote _current;

    void setupRemoteComplete(InitializeRemote current) {

        synchronized (_currentLock) {
            // make sure we still care...
            if (_current == current) {
                Gateway gateway = current.getGateway();
                if (current.isSuccessful()) {
                    Logger.logInfo(LOG_TAG, "zg: initializeRemote [%s] on gateway [%s] success", this.getDeviceDsn(), gateway.getDeviceDsn());
                } else {
                    Logger.logInfo(LOG_TAG, "zg: initializeRemote [%s] on gateway [%s] failure", this.getDeviceDsn(), gateway.getDeviceDsn());
                }
                current.complete();
                _current = null;
            }
        }
    }

    void setupRemote(ZigbeeGateway gateway, Object tag, Gateway.AylaGatewayCompletionHandler completion) {
        // make sure we have the latest info
        gateway.getGroupManager().fetchZigbeeGroups(null, null);
        gateway.getBindingManager().fetchZigbeeBindings(null, null);

        // we need to create 1 group & 1 binding
        Logger.logInfo(LOG_TAG, "zg: initializeRemote [%s] on gateway [%s]", this.getDeviceDsn(), gateway.getDeviceDsn());
        _gateway = gateway;

        synchronized (_currentLock) {
            if (_current == null) {
                _current = new InitializeRemote(gateway, this, tag, completion);
                _current.start();
            }
        }
    }

    @Override
    public void postRegistrationForGatewayDevice(Gateway gateway) {
        if (gateway.isZigbeeGateway()) {
            setupRemote((ZigbeeGateway)gateway, null, null);
        }
    }

    public void fixRegistrationForGatewayDevice(final Gateway gateway, final Object tag, final Gateway.AylaGatewayCompletionHandler completion) {
        if (gateway.isZigbeeGateway()) {
            setupRemote((ZigbeeGateway)gateway, tag, completion);
        }
    }

    private void removeRemoteBinding() {
        String name = GROUP_PREFIX_REMOTE + getDeviceDsn();
        AylaBindingZigbee binding = _gateway.getBindingManager().getByName(name);
        if (binding != null) {
            Logger.logDebug(LOG_TAG, "zg: removeRemoteGroup [%s] deleteBinding [%s]", getDeviceDsn(), name);
            _gateway.deleteBinding(binding, this, new Gateway.AylaGatewayCompletionHandler() {
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
            Logger.logInfo(LOG_TAG, "zg: unregisterRemote [%s] on gateway [%s] succeeded", getDeviceDsn(), _gateway.getDeviceDsn());
        }
    }

    private void removeRemoteGroup() {
        String name = GROUP_PREFIX_REMOTE + getDeviceDsn();
        AylaGroupZigbee group = _gateway.getGroupManager().getByName(name);
        if (group == null) {
            removeRemoteBinding();
        } else {
            Logger.logDebug(LOG_TAG, "zg: removeRemoteGroup [%s] deleteGroup [%s]", getDeviceDsn(), name);
            _gateway.deleteGroup(group, this, new Gateway.AylaGatewayCompletionHandler() {
                @Override
                public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                    if (AylaNetworks.succeeded(msg)) {
                        removeRemoteBinding();
                    } else {
                        // failed :(
                        Logger.logError(LOG_TAG, "zg: removeRemoteGroup [%s] on gateway [%s] failed", ((Device) tag).getDeviceDsn(), gateway.getDeviceDsn());
                    }
                }
            });
        }
    }

    @Override
    public void preUnregistrationForGatewayDevice(Gateway gateway) {
        _gateway = (ZigbeeGateway)gateway;
        Logger.logInfo(LOG_TAG, "zg: unregisterDevice [%s] on gateway [%s]", this.getDeviceDsn(), gateway.getDeviceDsn());
        removeRemoteGroup();
    }

    @Override
    public void deviceAdded(Device oldDevice) {
        super.deviceAdded(oldDevice);
        _gateway = (ZigbeeGateway)Gateway.getGatewayForDeviceNode(this);
        if (_gateway == null) {
            Logger.logError(LOG_TAG, "rm: deviceAdded [%s] no gateway found!", this.getDeviceDsn());
        } else {
            Logger.logInfo(LOG_TAG, "rm: deviceAdded [%s] on gateway [%s]", this.getDeviceDsn(), _gateway.getDeviceDsn());
        }
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
        ZigbeeGateway gateway = (ZigbeeGateway)Gateway.getGatewayForDeviceNode(this);
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
        ZigbeeGateway gateway = (ZigbeeGateway)Gateway.getGatewayForDeviceNode(this);
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
        ZigbeeGateway gateway = (ZigbeeGateway)Gateway.getGatewayForDeviceNode(this);
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
