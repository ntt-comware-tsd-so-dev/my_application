package com.aylanetworks.agilelink.device;
/* 
 * ZigbeeTriggerDevice
 * AgileLink Application Framework
 *
 * Created by David N. Junod on 6/9/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

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

import java.util.ArrayDeque;

public class ZigbeeTriggerDevice extends Device  {

    private final static String LOG_TAG = "ZigbeeTriggerDevice";

    public final static String ZB_IAS_OPEN_TURN_ON = "2_out_0x0006_0x0000";
    public final static String ZB_IAS_OPEN_TURN_OFF = "3_out_0x0006_0x0000";
    public final static String ZB_IAS_CLOSE_TURN_ON = "4_out_0x0006_0x0000";
    public final static String ZB_IAS_CLOSE_TURN_OFF = "5_out_0x0006_0x0000";

    public final static String GROUP_PREFIX_TRIGGER = "sensor-";
    Gateway _gateway;

    public ZigbeeTriggerDevice(AylaDevice device) {
        super(device);
    }

    @Override
    public boolean isDeviceNode() {
        return true;
    }

    @Override
    public int hasPostRegistrationProcessingResourceId() { return R.string.add_device_sensor_warning; }

    /**
     * Static method used for building the name of the trigger group & binding for a device.  This
     * needs to be consistent between all operating systems.
     *
     * @param device Trigger device.
     * @param open True indicates open, false indicates close.
     * @param turnOn True indicates on, false indicates off.
     * @return Name to use for the group or binding object.
     */
    static public String makeGroupKeyForSensor(Device device, boolean open, boolean turnOn) {
        StringBuilder sb = new StringBuilder(512);
        sb.append(GROUP_PREFIX_TRIGGER);
        sb.append(device.getDeviceDsn());
        // don't localize these words
        sb.append(open ? "Open" : "Close");
        sb.append(turnOn ? "On" : "Off");
        return sb.toString();
    }

    /**
     * Text to use for trigger on
     * @return The text
     */
    public String getTriggerOnName() {
        return MainActivity.getInstance().getString(R.string.trigger_on_name);
    }

    /**
     * Text to use for trigger off
     * @return The text
     */
    public String getTriggerOffName() {
        return MainActivity.getInstance().getString(R.string.trigger_off_name);
    }

    /**
     * Get the trigger group for the specified combination.
     *
     * @param open True indicates open, false indicates close.
     * @param turnOn True indicates on, false indicates off.
     * @return AylaGroupZigbee
     */
    public AylaGroupZigbee getTriggerGroup(boolean open, boolean turnOn) {
        return _gateway.getGroupByName(makeGroupKeyForSensor(this, open, turnOn));
    }

    /**
     * Get the trigger group with the specified name.
     * @param name Trigger group name constructed by makeGroupKeyForSensor
     * @return AylaGroupZigbee
     */
    public AylaGroupZigbee getTriggerGroupByName(String name) {
        return _gateway.getGroupByName(name);
    }

    /**
     * Get the trigger binding for the specified combination
     * @param open True indicates open, false indicates close.
     * @param turnOn True indicates on, false indicates off.
     * @return AylaBindingZigbee
     */
    public AylaBindingZigbee getTriggerBinding(boolean open, boolean turnOn) {
        return _gateway.getBindingByName(makeGroupKeyForSensor(this, open, turnOn));
    }

    /**
     * Get the trigger binding with the specified name.
     * @param name Trigger binding name constructed by makeGroupKeyForSensor
     * @return AylaBindingZigbee
     */
    public AylaBindingZigbee getTriggerBindingByName(String name) {
        return _gateway.getBindingByName(name);
    }

    /**
     * Determines if the specified device can be used as a trigger target.
     * @param another Device
     * @return true indicates Device is a Zigbee switched device, false indicates that it
     * isn't a switched device.
     */
    public boolean isTriggerTarget(Device another) {
        return (another instanceof ZigbeeSwitchedDevice);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

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
        public DequeSet _dequeSet;
        public Device _device;
        public DequeState _state = DequeState.NotStarted;

        public DequeEntry(DequeSet dequeSet, Device device) {
            _dequeSet = dequeSet;
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

        public synchronized void runComplete(Message msg) {
            if ((msg == null) || AylaNetworks.succeeded(msg)) {
                _state = DequeState.Success;
            } else {
                _state = DequeState.Failure;
            }
            _dequeSet.runNext(this, msg);
        }
    }

    class AddTriggerGroupForSensor extends DequeEntry {

        public boolean _open;
        public boolean _turnOn;

        public AddTriggerGroupForSensor(DequeSet dequeSet, Device device, boolean open, boolean turnOn) {
            super(dequeSet, device);
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
                            Logger.logInfo(LOG_TAG, "zg: initializeSensorBinding [%s] on gateway [%s] success", proc.getDeviceDsn(), gateway.getDeviceDsn());
                        } else {
                            // failed :(
                            Logger.logError(LOG_TAG, "zg: initializeSensorBinding [%s] on gateway [%s] failed", proc.getDeviceDsn(), gateway.getDeviceDsn());
                        }
                        proc.runComplete(msg);
                    }
                });
            } else {
                runComplete(null);
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
                        AddTriggerGroupForSensor proc = (AddTriggerGroupForSensor) tag;
                        if (AylaNetworks.succeeded(msg)) {
                            initializeSensorBinding();
                        } else {
                            // failed :(
                            Logger.logError(LOG_TAG, "zg: initializeSensorGroup [%s] on gateway [%s] failed", proc.getDeviceDsn(), gateway.getDeviceDsn());
                            proc.runComplete(msg);
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

        public RemoveTriggerGroupForSensor(DequeSet dequeSet, Device device, boolean open, boolean turnOn) {
            super(dequeSet, device);
            _open = open;
            _turnOn = turnOn;
        }

        void removeSensorGroup() {
            String name = makeGroupKeyForSensor(_device, _open, _turnOn);
            AylaGroupZigbee group = _gateway.getGroupManager().getByName(name);
            if (group == null) {
                runComplete(null);
            } else {
                Logger.logDebug(LOG_TAG, "zg: removeSensorGroup [%s] deleteGroup [%s]", getDeviceDsn(), name);
                _gateway.deleteGroup(group, this, new Gateway.AylaGatewayCompletionHandler() {
                    @Override
                    public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                        RemoveTriggerGroupForSensor proc = (RemoveTriggerGroupForSensor)tag;
                        if (!AylaNetworks.succeeded(msg)) {
                            // failed :(
                            Logger.logError(LOG_TAG, "zg: removeSensorGroup [%s] on gateway [%s] failed", proc.getDeviceDsn(), gateway.getDeviceDsn());
                        }
                        proc.runComplete(msg);
                    }
                });
            }
        }

        void removeSensorBinding() {
            String name = makeGroupKeyForSensor(_device, _open, _turnOn);
            AylaBindingZigbee binding = _gateway.getBindingManager().getByName(name);
            if (binding != null) {
                Logger.logDebug(LOG_TAG, "zg: removeSensorBinding [%s] deleteBinding [%s]", getDeviceDsn(), name);
                _gateway.deleteBinding(binding, this, new Gateway.AylaGatewayCompletionHandler() {
                    @Override
                    public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                        RemoveTriggerGroupForSensor proc = (RemoveTriggerGroupForSensor) tag;
                        if (AylaNetworks.succeeded(msg)) {
                            Logger.logInfo(LOG_TAG, "zg: unregisterRemote [%s] on gateway [%s] succeeded", proc.getDeviceDsn(), gateway.getDeviceDsn());
                        } else {
                            // failed :(
                            Logger.logError(LOG_TAG, "zg: removeSensorBinding [%s] on gateway [%s] failed", proc.getDeviceDsn(), gateway.getDeviceDsn());
                        }
                        removeSensorGroup();
                    }
                });
            } else {
                Logger.logInfo(LOG_TAG, "zg: unregisterDevice [%s] on gateway [%s] succeeded", getDeviceDsn(), _gateway.getDeviceDsn());
                removeSensorGroup();
            }
        }

        @Override
        public synchronized void run() {
            if (_state == DequeState.NotStarted) {
                _state = DequeState.Connecting;
                // remove the binding & then the group
                removeSensorBinding();
            }
        }
    }

    public class DequeSet {

        Gateway _gateway;
        Device _device;
        Object _tag;
        Object _userTag;
        Gateway.AylaGatewayCompletionHandler _completion;
        DequeRunState _dequeRunState;
        ArrayDeque<DequeEntry> _deque;
        int countTotal;
        int countSuccess;
        int countFailure;

        public DequeSet(Gateway gateway, Device device, Object tag, Object userTag, Gateway.AylaGatewayCompletionHandler completion) {
            _gateway = gateway;
            _device = device;
            _tag = tag;
            _userTag = userTag;
            _completion = completion;
            _dequeRunState = DequeRunState.NotRunning;
            _deque = new ArrayDeque<>();
            countTotal = countSuccess = countFailure = 0;
        }

        public void addRunEntry(DequeEntry entry) {
            entry._dequeSet = this;
            _deque.addLast(entry);
            countTotal++;
        }

        public boolean isSuccessful() {
            return (countTotal == countSuccess);
        }

        public Gateway getGateway() {
            return _gateway;
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
                            // the entry isn't ready to run, so move it to the bottom
                            _deque.remove(entry);
                            _deque.addLast(entry);
                            count++;
                        }
                    } else {
                        _dequeRunState = DequeRunState.NotRunning;

                        // the set is complete, but more might still be running...
                        if (countTotal == countFailure + countSuccess) {
                            ((ZigbeeTriggerDevice) _device).dequeSetComplete(this, _tag);
                        }
                        break;
                    }
                }
            }
        }

        void runNext(DequeEntry entry, Message msg) {
            synchronized (_deque) {
                if ((msg==null) || AylaNetworks.succeeded(msg)) {
                    countSuccess++;
                } else {
                    countFailure++;
                }
                if (entry != null) {
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

        public void complete() {
            if (_completion != null) {
                Message msg = new Message();
                msg.what = isSuccessful() ? AylaNetworks.AML_ERROR_OK : AylaNetworks.AML_ERROR_FAIL;
                _completion.gatewayCompletion(_gateway, msg, _userTag);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    Object _currentDequeSetLock = new Object();
    DequeSet _currentDequeSet;

    void dequeSetComplete(DequeSet dequeSet, Object tag) {

        synchronized (_currentDequeSetLock) {
            // make sure we still care...
            if (_currentDequeSet == dequeSet) {
                Gateway gateway = dequeSet.getGateway();
                if (dequeSet.isSuccessful()) {
                    Logger.logInfo(LOG_TAG, "zg: %s [%s] on gateway [%s] success", (String) tag, this.getDeviceDsn(), gateway.getDeviceDsn());
                } else {
                    Logger.logInfo(LOG_TAG, "zg: %s [%s] on gateway [%s] failure", (String) tag, this.getDeviceDsn(), gateway.getDeviceDsn());
                }
                dequeSet.complete();
                _currentDequeSet = null;
            }
        }
    }

    void setupTriggers(Gateway gateway, Object tag, Gateway.AylaGatewayCompletionHandler completion) {
        // make sure we have the latest info
        gateway.getGroupManager().fetchZigbeeGroups(null, null);
        gateway.getBindingManager().fetchZigbeeBindings(null, null);

        // create 4 bindings & 4 groups
        Logger.logInfo(LOG_TAG, "zg: initializeSensor [%s] on gateway [%s]", this.getDeviceDsn(), gateway.getDeviceDsn());
        _gateway = gateway;

        synchronized (_currentDequeSetLock) {
            // it would be null, since we are just starting
            if (_currentDequeSet == null) {
                _currentDequeSet = new DequeSet(gateway, this, "initializeSensor", tag, completion);
                _currentDequeSet.addRunEntry(new AddTriggerGroupForSensor(_currentDequeSet, this, true, true));
                _currentDequeSet.addRunEntry(new AddTriggerGroupForSensor(_currentDequeSet, this, true, false));
                _currentDequeSet.addRunEntry(new AddTriggerGroupForSensor(_currentDequeSet, this, false, true));
                _currentDequeSet.addRunEntry(new AddTriggerGroupForSensor(_currentDequeSet, this, false, false));
                _currentDequeSet.runIfNeeded();
            }
        }
    }

    @Override
    public void postRegistrationForGatewayDevice(Gateway gateway) {
        setupTriggers(gateway, null, null);
    }

    public void fixRegistrationForGatewayDevice(final Gateway gateway, final Object tag, final Gateway.AylaGatewayCompletionHandler completion) {
        setupTriggers(gateway, tag, completion);
    }

    @Override
    public void preUnregistrationForGatewayDevice(Gateway gateway) {

        // make sure we have the latest info
        gateway.getGroupManager().fetchZigbeeGroups(null, null);
        gateway.getBindingManager().fetchZigbeeBindings(null, null);

        // remove 4 bindings & 4 groups
        Logger.logInfo(LOG_TAG, "zg: unregisterDevice [%s] on gateway [%s]", this.getDeviceDsn(), _gateway.getDeviceDsn());

        synchronized (_currentDequeSetLock) {
            if (_currentDequeSet != null) {
                // stop any add that may be going on...
                _currentDequeSet.clearDeque();
                _currentDequeSet = null;
            }
            // queue up the removals
            _currentDequeSet = new DequeSet(gateway, this, "unregisterDevice", null, null);
            _currentDequeSet.addRunEntry(new RemoveTriggerGroupForSensor(_currentDequeSet, this, true, true));
            _currentDequeSet.addRunEntry(new RemoveTriggerGroupForSensor(_currentDequeSet, this, true, false));
            _currentDequeSet.addRunEntry(new RemoveTriggerGroupForSensor(_currentDequeSet, this, false, true));
            _currentDequeSet.addRunEntry(new RemoveTriggerGroupForSensor(_currentDequeSet, this, false, false));
            _currentDequeSet.runIfNeeded();
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
}

