package com.aylanetworks.agilelink.device;
/* 
 * ZigbeeTriggerDevice
 * AgileLink Application Framework
 *
 * Created by David N. Junod on 6/9/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

import android.os.Handler;
import android.os.Message;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.zigbee.AylaBindingZigbee;
import com.aylanetworks.aaml.zigbee.AylaGroupZigbee;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.Gateway;
import com.aylanetworks.agilelink.framework.Logger;

import java.util.ArrayDeque;

public class ZigbeeTriggerDevice extends Device {

    private final static String LOG_TAG = "ZigbeeTriggerDevice";

    public final static String ZB_IAS_OPEN_TURN_ON = "2_out_0x0006_0x0000";
    public final static String ZB_IAS_OPEN_TURN_OFF = "3_out_0x0006_0x0000";
    public final static String ZB_IAS_CLOSE_TURN_ON = "4_out_0x0006_0x0000";
    public final static String ZB_IAS_CLOSE_TURN_OFF = "5_out_0x0006_0x0000";

    Gateway _gateway;

    public ZigbeeTriggerDevice(AylaDevice device) {
        super(device);
        initializeDeque();
    }

    @Override
    public boolean isDeviceNode() {
        return true;
    }

    @Override
    public int hasPostRegistrationProcessingResourceId() { return R.string.add_device_sensor_warning; }

    static public String makeGroupKeyForSensor(Device device, boolean open, boolean turnOn) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("sensor-");
        sb.append(device.getDevice().dsn);
        sb.append(open ? "Open" : "Close");
        sb.append(turnOn ? "On" : "Off");
        return sb.toString();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    DequeState _state;
    DequeRunState _dequeRunState;
    ArrayDeque<DequeEntry> _deque;

    enum DequeRunState {
        NotRunning,
        RunningDeque,
    }

    enum DequeState {
        NotStarted,
        Connecting,
        Failure,
        Success,
    };

    class DequeEntry {
        public Device _device;
        public DequeState _state = DequeState.NotStarted;

        public DequeEntry(Device device) {
            _device = device;
        }

        public boolean isReady() {
            return true;
        }

        public String getDeviceDsn() {
            return _device.getDevice().dsn;
        }

        public void run() {
        }

        public synchronized void runComplete(int what) {
            _state = (what == 0) ? DequeState.Success : DequeState.Failure;
        }
    }

    class AddTriggerGroupForSensor extends DequeEntry {

        public boolean _open;
        public boolean _turnOn;

        public AddTriggerGroupForSensor(Device device, boolean open, boolean turnOn) {
            super(device);
            _open = open;
            _turnOn = turnOn;
        }

        void initializeSensorBinding() {
            String name = makeGroupKeyForSensor(_device, _open, _turnOn);
            AylaGroupZigbee group = _gateway.getGroupManager().getByName(name);
            if (group == null) {
                Logger.logError(LOG_TAG, "zg: initializeSensorBinding no group [%s]", name);
                return;
            }

            AylaBindingZigbee binding = _gateway.getBindingManager().getByName(name);
            if (binding == null) {
                // figure out the property name that we are going to bind
                String propertyName;
                if (_open) {
                    if (_turnOn) {
                        propertyName = ZB_IAS_OPEN_TURN_ON;
                    } else {
                        propertyName = ZB_IAS_OPEN_TURN_OFF;
                    }
                } else {
                    if (_turnOn) {
                        propertyName = ZB_IAS_CLOSE_TURN_ON;
                    } else {
                        propertyName = ZB_IAS_CLOSE_TURN_OFF;
                    }
                }

                binding = new AylaBindingZigbee();
                binding.bindingName = name;
                binding.gatewayDsn = _gateway.getDeviceDsn();
                binding.fromId = getDeviceDsn();
                binding.fromName = propertyName;
                binding.toId = group.getId().toString();
                binding.toName = name;
                binding.isGroup = true;
                Logger.logDebug(LOG_TAG, "zg: initializeSensorBinding [%s] createBinding [%s:%s:%s]", getDeviceDsn(), binding.bindingName, binding.toName, binding.fromId);
                _gateway.createBinding(binding, this, new Gateway.AylaGatewayCompletionHandler() {
                    @Override
                    public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                        AddTriggerGroupForSensor proc = (AddTriggerGroupForSensor)tag;
                        if (AylaNetworks.succeeded(msg)) {
                            // all done binding the remote
                            Logger.logInfo(LOG_TAG, "zg: initializeSensor [%s] on gateway [%s] success", proc.getDeviceDsn(), gateway.getDeviceDsn());
                        } else {
                            // failed :(
                            Logger.logError(LOG_TAG, "zg: initializeSensor [%s] on gateway [%s] failed", proc.getDeviceDsn(), gateway.getDeviceDsn());
                        }
                        proc.runComplete(msg.what);
                    }
                });
            } else {
                runComplete(0);
            }
        }

        void initializeSensorGroup() {
            String name = makeGroupKeyForSensor(_device, _open, _turnOn);
            AylaGroupZigbee group = _gateway.getGroupManager().getByName(name);
            if (group == null) {
                Logger.logDebug(LOG_TAG, "zg: initializeSensorGroup [%s] createGroup [%s]", getDeviceDsn(), name);
                _gateway.createGroup(name, null, this, new Gateway.AylaGatewayCompletionHandler() {
                    @Override
                    public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                        AddTriggerGroupForSensor proc = (AddTriggerGroupForSensor)tag;
                        if (AylaNetworks.succeeded(msg)) {
                            initializeSensorBinding();
                        } else {
                            // failed :(
                            Logger.logError(LOG_TAG, "zg: initializeSensor [%s] on gateway [%s] failed", proc.getDeviceDsn(), gateway.getDeviceDsn());
                            proc.runComplete(msg.what);
                        }
                    }
                });
            } else {
                initializeSensorBinding();
            }
        }

        @Override
        public synchronized void run() {
            if (_state == DequeState.NotStarted) {
                _state = DequeState.Connecting;
                // create the group & binding
                initializeSensorGroup();
            }
        }
    }

    class RemoveTriggerGroupForSensor extends DequeEntry {

        public boolean _open;
        public boolean _turnOn;

        public RemoveTriggerGroupForSensor(Device device, boolean open, boolean turnOn) {
            super(device);
            _open = open;
            _turnOn = turnOn;
        }

        void removeSensorBinding() {
            String name = makeGroupKeyForSensor(_device, _open, _turnOn);
            AylaBindingZigbee binding = _gateway.getBindingManager().getByName(name);
            if (binding != null) {
                Logger.logDebug(LOG_TAG, "zg: removeSensorBinding [%s] deleteBinding [%s]", getDeviceDsn(), name);
                _gateway.deleteBinding(binding, this, new Gateway.AylaGatewayCompletionHandler() {
                    @Override
                    public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                        AddTriggerGroupForSensor proc = (AddTriggerGroupForSensor)tag;
                        if (AylaNetworks.succeeded(msg)) {
                            Logger.logInfo(LOG_TAG, "zg: unregisterRemote [%s] on gateway [%s] succeeded", proc.getDeviceDsn(), gateway.getDeviceDsn());
                        } else {
                            // failed :(
                            Logger.logError(LOG_TAG, "zg: removeSensorBinding [%s] on gateway [%s] failed", proc.getDeviceDsn(), gateway.getDeviceDsn());
                        }
                    }
                });
            } else {
                Logger.logInfo(LOG_TAG, "zg: unregisterDevice [%s] on gateway [%s] succeeded", getDeviceDsn(), _gateway.getDeviceDsn());
            }
        }

        void removeSensorGroup() {
            String name = makeGroupKeyForSensor(_device, _open, _turnOn);
            AylaGroupZigbee group = _gateway.getGroupManager().getByName(name);
            if (group == null) {
                removeSensorBinding();
            } else {
                Logger.logDebug(LOG_TAG, "zg: removeSensorGroup [%s] deleteGroup [%s]", getDeviceDsn(), name);
                _gateway.deleteGroup(group, this, new Gateway.AylaGatewayCompletionHandler() {
                    @Override
                    public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                        AddTriggerGroupForSensor proc = (AddTriggerGroupForSensor)tag;
                        if (!AylaNetworks.succeeded(msg)) {
                            // failed :(
                            Logger.logError(LOG_TAG, "zg: removeSensorGroup [%s] on gateway [%s] failed", proc.getDeviceDsn(), gateway.getDeviceDsn());
                        }
                        removeSensorBinding();
                    }
                });
            }
        }

        @Override
        public synchronized void run() {
            if (_state == DequeState.NotStarted) {
                _state = DequeState.Connecting;
                // remove the group & binding
                removeSensorGroup();
            }
        }
    }

    public void initializeDeque() {
        _state = DequeState.NotStarted;
        _dequeRunState = DequeRunState.NotRunning;
        _deque = new ArrayDeque<DequeEntry>();
    }

    public void addRunEntry(DequeEntry entry) {
        _deque.addLast(entry);
    }

    void runIfNeeded() {
        if (_dequeRunState != DequeRunState.RunningDeque) {
            run();
        }
    }

    void run() {
        synchronized (_deque) {
            DequeEntry first = _deque.peekFirst();
            int count = 0;
            while (true) {
                DequeEntry entry = _deque.peekFirst();
                if (entry != null) {
                    if (entry.isReady()) {
                        _dequeRunState = DequeRunState.RunningDeque;
                        entry.run();
                        break;
                    } else if ((count > 0) && (entry == first)) {
                        break;
                    } else {
                        _deque.remove(entry);
                        _deque.addLast(entry);
                        count++;
                    }
                } else {
                    _dequeRunState = DequeRunState.NotRunning;
                    break;
                }
            }
        }
    }

    void runComplete(DequeEntry entry, Device device, AylaGroupZigbee group, int what) {
        synchronized (_deque) {
            if (entry != null) {
                entry.runComplete(what);
                _deque.remove(entry);
            }
            // run the next entry in the Deque
            run();
        }
    }

    public void clearDeque() {
        synchronized (_deque) {
            _deque.clear();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void postRegistrationForGatewayDevice(Gateway gateway) {

        // create 4 bindings & 4 groups
        Logger.logInfo(LOG_TAG, "zg: initializeSensor [%s] on gateway [%s]", this.getDeviceDsn(), gateway.getDeviceDsn());
        _gateway = gateway;
        synchronized (_deque) {
            addRunEntry(new AddTriggerGroupForSensor(this, true, true));
            addRunEntry(new AddTriggerGroupForSensor(this, true, false));
            addRunEntry(new AddTriggerGroupForSensor(this, false, true));
            addRunEntry(new AddTriggerGroupForSensor(this, false, false));
            runIfNeeded();
        }
    }

    @Override
    public void deviceAdded(Device oldDevice) {
        super.deviceAdded(oldDevice);
        _gateway = Gateway.getGatewayForDeviceNode(this);
        Logger.logInfo(LOG_TAG, "zg: deviceAdded [%s] on gateway [%s]", this.getDeviceDsn(), _gateway.getDeviceDsn());
    }

    @Override
    public void deviceRemoved() {
        super.deviceRemoved();
        Logger.logInfo(LOG_TAG, "zg: deviceRemoved [%s]", this.getDeviceDsn());
    }

    @Override
    public void unregisterDevice(Handler handler) {
        super.unregisterDevice(handler);

        // remove 4 bindings & 4 groups
        Logger.logInfo(LOG_TAG, "zg: unregisterDevice [%s] on gateway [%s]", this.getDeviceDsn(), _gateway.getDeviceDsn());
        synchronized (_deque) {
            // stop any add that may be going on...
            clearDeque();
            // queue up the removals
            addRunEntry(new RemoveTriggerGroupForSensor(this, true, true));
            addRunEntry(new RemoveTriggerGroupForSensor(this, true, false));
            addRunEntry(new RemoveTriggerGroupForSensor(this, false, true));
            addRunEntry(new RemoveTriggerGroupForSensor(this, false, false));
            runIfNeeded();
        }
    }
}

