package com.aylanetworks.agilelink.framework;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.agilelink.device.ALDevice;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Brian King on 12/19/14.
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

    public List<ALDevice> deviceList() {
        return _deviceList;
    }

    public void refreshDeviceList() {
        fetchDeviceList();
    }

    public void setComparator(Comparator<ALDevice> comparator) {
        _deviceComparator = comparator;
        Collections.sort(_deviceList, _deviceComparator);
    }
    
    public void startPolling() {
        stopPolling();
        _deviceListTimerHandler.postDelayed(_deviceListTimerRunnable, 0);
        _deviceStatusTimerHandler.postDelayed(_deviceStatusTimerRunnable, 0);
    }
    
    public void stopPolling() {
        _deviceListTimerHandler.removeCallbacksAndMessages(_deviceListTimerRunnable);
        _deviceStatusTimerHandler.removeCallbacksAndMessages(_deviceStatusTimerRunnable);
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

    private final static String LOG_TAG = "DeviceManager";

    private List<ALDevice> _deviceList;
    private Set<DeviceListListener> _deviceListListeners;
    private Set<DeviceStatusListener> _deviceStatusListeners;

    private int _deviceListPollInterval = 30000;
    private int _deviceStatusPollInterval = 5000;

    // Default comparator uses the device's compareTo method. Can be updated with setComparator().
    private Comparator<ALDevice> _deviceComparator = new Comparator<ALDevice>() {
        @Override
        public int compare(ALDevice lhs, ALDevice rhs) {
            return lhs.compareTo(rhs);
        }
    };

    // Timer handlers and runnables

    private Handler _deviceListTimerHandler = new Handler();
    private Handler _deviceStatusTimerHandler = new Handler();

    private Runnable _deviceListTimerRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(LOG_TAG, "Device List Timer");
            fetchDeviceList();
            _deviceListTimerHandler.postDelayed(this, _deviceListPollInterval);
        }
    };

    private Runnable _deviceStatusTimerRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(LOG_TAG, "Device Status Timer");
            _deviceStatusTimerHandler.postDelayed(this, _deviceStatusPollInterval);
        }
    };

    /** Private methods */

    private final Handler _getDevicesHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
                // Create our device array
                List<ALDevice> newDeviceList = new ArrayList<>();
                JsonParser parser = new JsonParser();
                JsonArray array = parser.parse((String)msg.obj).getAsJsonArray();
                for ( JsonElement element : array ) {
                    // Log.d(LOG_TAG, "JSON element: " + element.toString());
                    // Get the correct class to create from the device class map.
                    ALDevice device = SessionManager.sessionParameters()._deviceCreator.deviceFromJsonElement(element);
                    // Log.d(LOG_TAG, "Created device: " + device);
                    newDeviceList.add(device);
                }

                if ( deviceListChanged(newDeviceList) ) {
                    _deviceList = newDeviceList;
                    notifyDeviceListChanged();
                }
            }
        }
    };

    private void fetchDeviceList() {
        AylaDevice.getDevices(_getDevicesHandler);
    }

    private boolean deviceListChanged(List<ALDevice>newDeviceList) {
        if ( _deviceList == null && newDeviceList != null ) {
            return true;
        }

        if ( newDeviceList == null && _deviceList != null ) {
            return true;
        }

        if ( newDeviceList == null && _deviceList == null ) {
            return false;
        }

        if ( newDeviceList.size() != _deviceList.size() ) {
            return true;
        }

        // Sort the new list of devices
        Collections.sort(newDeviceList, _deviceComparator);

        // See if we're identical
        for ( int i = 0; i < _deviceList.size(); i++ ) {
            ALDevice dev1 = _deviceList.get(i);
            ALDevice dev2 = newDeviceList.get(i);
            if ( dev1.compareTo(dev2) != 0 ) {
                return true;
            }
        }
        return false;
    }

    // Notifications
    private void notifyDeviceListChanged() {
        Log.d(LOG_TAG, "Device list changed:\n" + _deviceList);
        for ( DeviceListListener listener : _deviceListListeners ) {
            listener.deviceListChanged();
        }
    }

    private void notifyDeviceStatusChanged(AylaDevice device) {
        Log.d(LOG_TAG, "Device status changed: " + device);
        for (DeviceStatusListener listener : _deviceStatusListeners) {
            listener.deviceStatusChanged(device);
        }
    }
}
