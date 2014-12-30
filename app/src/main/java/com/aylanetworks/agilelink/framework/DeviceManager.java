package com.aylanetworks.agilelink.framework;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.agilelink.framework.Device.DeviceStatusListener;
import com.aylanetworks.aaml.AylaNetworks;
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
public class DeviceManager implements DeviceStatusListener {
    /** Interfaces */
    public interface DeviceListListener {
        void deviceListChanged();
    }

    /** Public Methods */

    public List<Device> deviceList() {
        return _deviceList;
    }

    public void refreshDeviceList() {
        fetchDeviceList();
    }

    public void setComparator(Comparator<Device> comparator) {
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

    public void shutDown() {
        Log.i(LOG_TAG, "Shut down");
        // Clear out our list of devices, and then notify listeners that the list has changed.
        // This should cause all listeners to clear any devices they may be displaying
        _deviceList.clear();
        notifyDeviceListChanged();

        // Get rid of our listeners.
        _deviceStatusListeners.clear();
        _deviceListListeners.clear();

        // Stop polling!
        stopPolling();
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
        boolean startTimer = (_deviceListListeners.size() == 0);
        _deviceListListeners.add(listener);
        if ( startTimer ) {
            _deviceListTimerHandler.removeCallbacksAndMessages(_deviceListTimerRunnable);
            _deviceListTimerHandler.postDelayed(_deviceListTimerRunnable, _deviceStatusPollInterval);
        }
    }

    public void removeDeviceListListener(DeviceListListener listener) {
        _deviceListListeners.remove(listener);
    }

    public void addDeviceStatusListener(DeviceStatusListener listener) {
        boolean startTimer = (_deviceStatusListeners.size() == 0);
        _deviceStatusListeners.add(listener);
        if ( startTimer ) {
            _deviceStatusTimerHandler.removeCallbacksAndMessages(_deviceStatusTimerRunnable);
            _deviceStatusTimerHandler.postDelayed(_deviceStatusTimerRunnable, _deviceStatusPollInterval);
        }
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

    private List<Device> _deviceList;
    private Set<DeviceListListener> _deviceListListeners;
    private Set<DeviceStatusListener> _deviceStatusListeners;

    private int _deviceListPollInterval = 30000;
    private int _deviceStatusPollInterval = 5000;

    // Default comparator uses the device's compareTo method. Can be updated with setComparator().
    private Comparator<Device> _deviceComparator = new Comparator<Device>() {
        @Override
        public int compare(Device lhs, Device rhs) {
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

            // Only continue polling if somebody is listening
            if (_deviceListListeners.size() > 0) {
                _deviceListTimerHandler.postDelayed(this, _deviceListPollInterval);
            } else {
                Log.d(LOG_TAG, "Device List Timer: Nobody listening");
            }
        }
    };

    private Runnable _deviceStatusTimerRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(LOG_TAG, "Device Status Timer");
            boolean changed = false;
            for ( Device device : _deviceList ) {
                device.updateStatus(DeviceManager.this);
            }

            // Only continue polling if somebody is listening
            if ( _deviceStatusListeners.size() > 0 ) {
                _deviceStatusTimerHandler.postDelayed(this, _deviceStatusPollInterval);
            } else {
                Log.d(LOG_TAG, "Device Status Timer: Nobody listening");
            }
        }
    };

    /** Private methods */

    /** Handler called when the list of devices has been obtained from the server. */
    private final Handler _getDevicesHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
                // Create our device array
                Log.i(LOG_TAG, "Device JSON: " + msg.obj);
                List<Device> newDeviceList = new ArrayList<>();
                JsonParser parser = new JsonParser();
                JsonArray array = parser.parse((String)msg.obj).getAsJsonArray();
                for ( JsonElement element : array ) {
                    // Log.d(LOG_TAG, "JSON element: " + element.toString());
                    // Get the correct class to create from the device class map.
                    Device device = SessionManager.sessionParameters()._deviceCreator.deviceFromJsonElement(element);
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

    /** Fetches the list of devices from the server */
    private void fetchDeviceList() {
        AylaDevice.getDevices(_getDevicesHandler);
    }

    /** Returns true if newDeviceList differs from our previous version (_deviceList) */
    private boolean deviceListChanged(List<Device>newDeviceList) {
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
            Device dev1 = _deviceList.get(i);
            Device dev2 = newDeviceList.get(i);
            if ( dev1.compareTo(dev2) != 0 ) {
                return true;
            }
        }
        return false;
    }

    /** This is where we are notified when a device's status has been updated. */
    @Override
    public void statusUpdated(Device device) {
        Log.d(LOG_TAG, "Device status updated: " + device);
        notifyDeviceStatusChanged(device);
    }

    // Notifications
    private void notifyDeviceListChanged() {
        Log.d(LOG_TAG, "Device list changed:\n" + _deviceList);
        for ( DeviceListListener listener : _deviceListListeners ) {
            listener.deviceListChanged();
        }
    }

    private void notifyDeviceStatusChanged(Device device) {
        Log.d(LOG_TAG, "Device status changed: " + device);
        for (DeviceStatusListener listener : _deviceStatusListeners) {
            listener.statusUpdated(device);
        }
    }
}
