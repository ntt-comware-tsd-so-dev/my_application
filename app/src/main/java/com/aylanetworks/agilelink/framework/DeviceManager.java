package com.aylanetworks.agilelink.framework;

import com.aylanetworks.aaml.AylaDevice;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by bri on 12/19/14.
 */
public class DeviceManager {
    /** Interfaces */
    interface DeviceListListener {
        void deviceListChanged();
    }

    interface DeviceStatusListener {
        void deviceStatusChanged(AylaDevice device);
    }

    /** Public Methods */

    public List<AylaDevice> deviceList() {
        return _deviceList;
    }

    public void refreshDeviceList() {

    }

    // Poll interval methods

    public int getDeviceListPollInterval() {
        return _deviceListPollInterval;
    }

    public void setDeviceListPollInterval(int deviceListPollInterval) {
        _deviceListPollInterval = deviceListPollInterval;
    }

    public int getDeviceStatusPollInterval() {
        return _deviceStatusPollInterval;
    }

    public void setDeviceStatusPollInterval(int deviceStatusPollInterval) {
        _deviceStatusPollInterval = deviceStatusPollInterval;
    }


    // Listener methods

    public void addDeviceListListener(DeviceListListener listener) {
        _deviceListListeners.add(listener);
    }

    public void removeDeviceListListener(DeviceListListener listener) {
        _deviceListListeners.remove(listener);
    }

    public void addDeviceStatusListener(DeviceStatusListener listener) {
        _deviceStatusListeners.add(listener);
    }

    public void removeDeviceStatusListener(DeviceStatusListener listener) {
        _deviceStatusListeners.remove(listener);
    }

    /** Constructor */

    DeviceManager() {
        _deviceList = new ArrayList<>();
        _deviceListListeners = new HashSet<>();
        _deviceStatusListeners = new HashSet<>();
    }

    /** Private members */

    private List<AylaDevice> _deviceList;
    private Set<DeviceListListener> _deviceListListeners;
    private Set<DeviceStatusListener> _deviceStatusListeners;

    private int _deviceListPollInterval = 30000;
    private int _deviceStatusPollInterval = 5000;

    /** Private methods */

    private void notifyDeviceListChanged() {
        for ( DeviceListListener listener : _deviceListListeners ) {
            listener.deviceListChanged();
        }
    }

    private void notifyDeviceStatusChanged(AylaDevice device) {
        for (DeviceStatusListener listener : _deviceStatusListeners) {
            listener.deviceStatusChanged(device);
        }
    }
}
