package com.aylanetworks.agilelink.framework;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.aylanetworks.aaml.AylaDatum;
import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaLanMode;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaNotify;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.framework.Device.DeviceStatusListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
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

    /** Constructor */
    DeviceManager() {
        _deviceList = new ArrayList<>();
        _deviceListListeners = new HashSet<>();
        _deviceStatusListeners = new HashSet<>();

        _groupManager = new GroupManager();
        _groupManager.fetchDeviceGroups();
    }

    /**
     * Returns the list of all devices
     * @return The list of all devices
     */
    public List<Device> deviceList() {
        return new ArrayList<>(_deviceList);
    }

    /**
     * Returns the device with the given DSN, or null if not found
     * @param dsn the DSN of the device to find
     * @return The found device, or null if not found
     */
    public Device deviceByDSN(String dsn) {
        for ( Device d : _deviceList ) {
            if ( d.getDevice().dsn.compareTo(dsn) == 0 ) {
                return d;
            }
        }
        return null;
    }

    /**
     * Refreshes the list of devices.
     */
    public void refreshDeviceList() {
        fetchDeviceList();
    }

    /**
     * Refreshes the status of the supplied device
     * @param device Device to refresh the status of
     */
    public void refreshDeviceStatus(Device device) {
        if ( device == null ) {
            // Refresh all devices
            _deviceStatusTimerRunnable.run();
        } else {
            device.updateStatus(this);
        }
    }

    public void setDeviceComparator(Comparator<Device> comparator) {
        _deviceComparator = comparator;
        Collections.sort(_deviceList, _deviceComparator);
    }

    public Comparator<Device>getDeviceComparator() {
        return _deviceComparator;
    }

    /**
     * Starts polling for device list and status changes
     */
    public void startPolling() {
        Log.v(LOG_TAG, "startPolling");
        stopPolling();
        _deviceListTimerRunnable.run();
        _deviceStatusTimerHandler.postDelayed(_deviceStatusTimerRunnable, _deviceStatusPollInterval);
    }

    /**
     * Stops polling for device list and status changes
     */
    public void stopPolling() {
        _deviceListTimerHandler.removeCallbacksAndMessages(null);
        _deviceStatusTimerHandler.removeCallbacksAndMessages(null);
    }

    /**
     * Clears the list of devices and stops polling.
     */
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

    /**
     * Returns the gateway device, or null if one is not found
     * @return The gateway device, or null if one is not found
     */
    public Gateway getGatewayDevice() {
        for ( Device gateway : _deviceList ) {
            if ( gateway.isGateway() ) {
                return (Gateway)gateway;
            }
        }
        return null;
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

    public boolean isLanModeEnabled() {
        return _lanModeEnabled;
    }

    // Listener methods

    public void addDeviceListListener(DeviceListListener listener) {
        boolean startTimer = (_deviceListListeners.size() == 0);
        _deviceListListeners.add(listener);
        if ( startTimer ) {
            _deviceListTimerHandler.removeCallbacksAndMessages(null);
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
            _deviceStatusTimerHandler.removeCallbacksAndMessages(null);
            _deviceStatusTimerHandler.postDelayed(_deviceStatusTimerRunnable, _deviceStatusPollInterval);
        }
    }

    public void removeDeviceStatusListener(DeviceStatusListener listener) {
        _deviceStatusListeners.remove(listener);
    }

    // Groups
    protected GroupManager _groupManager;
    public GroupManager getGroupManager() {
        return _groupManager;
    }

     /** Private members */

    private final static String LOG_TAG = "DeviceManager";

    private List<Device> _deviceList;
    private Set<DeviceListListener> _deviceListListeners;
    private Set<DeviceStatusListener> _deviceStatusListeners;

    // This is set to true while we are attempting to enable LAN mode.
    // Device queries should be disabled while this is true.
    private boolean _startingLANMode = false;

    // This gets set to true once LAN mode has been enabled
    private boolean _lanModeEnabled = false;

    private int _deviceListPollInterval = 30000;
    private int _deviceStatusPollInterval = 5000;

    // Default comparator uses the device's compareTo method. Can be updated with setComparator().
    private Comparator<Device> _deviceComparator = new Comparator<Device>() {
        @Override
        public int compare(Device lhs, Device rhs) {
            return lhs.compareTo(rhs);
        }
    };

    // LAN mode handler
    private Handler _lanModeHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            _startingLANMode = false;

            String notifyResults = (String) msg.obj;
            AylaNotify notify = AylaSystemUtils.gson.fromJson(notifyResults, AylaNotify.class);

            String type = notify.type;
            String dsn = notify.dsn;
            String names[] = notify.names;

            Log.d(LOG_TAG, "lanModeHandler: [" + type + "] " + notifyResults);

            if (type.compareTo(AylaNetworks.AML_NOTIFY_TYPE_SESSION) == 0) {
                if (msg.arg1 > 399) {
                    // LAN mode could not be enabled
                    Log.e(LOG_TAG, "Failed to enter LAN mode: " + msg.arg1 + " " + msg.obj);
                    _lanModeEnabled = false;
                    notifyLANModeChange();
                } else {
                    if (msg.arg1 >= 200 && msg.arg1 < 300) {
                        if (!_lanModeEnabled) {
                            // Get the gateway properties
                            Gateway g = getGatewayDevice();
                            if ( g != null ) {
                                _lanModeEnabled = true;
                                Log.i(LOG_TAG, "LAN mode enabled: " + msg.obj);
                                notifyLANModeChange();
                            } else {
                                // Enable LAN mode on all devices that support it
                                for ( Device d : _deviceList ) {
                                    AylaDevice aylaDevice = d.getDevice();
                                    if ( aylaDevice.lanEnabled ) {
                                        Log.i(LOG_TAG, "Enabling LAN mode on " + d);
                                        aylaDevice.lanModeEnable();
                                    }
                                }
                                Log.e(LOG_TAG, "Lan mode enabled (no gateway)");
                            }
                        } else {
                            Log.v(LOG_TAG, "Already in LAN mode");
                        }
                    } else {
                        Log.e(LOG_TAG, "Unknown LAN mode \"session\" message: " + msg.what + " " + msg.obj);
                    }
                }
            } else if (type.compareTo(AylaNetworks.AML_NOTIFY_TYPE_PROPERTY) == 0 ||
                    type.compareTo(AylaNetworks.AML_NOTIFY_TYPE_NODE) == 0) {
                _deviceStatusTimerHandler.postDelayed(_deviceStatusTimerRunnable, 0);

                // BSK: The commented-out code below doesn't work right.
                /*
                // Update the property given to us here.
                Device device = deviceByDSN(dsn);
                if ( device != null ) {
                    Log.i(LOG_TAG, "LAN mode handler: Updating status for " + device);
                    device.updateStatus(DeviceManager.this);
                }
                */
            }
        }
    };

    // Reachability handler
    private Handler _reachabilityHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            String json = (String)msg.obj;

            Log.d(LOG_TAG, "Reachability handler: " + json);
        }
    };

    private Handler _getNodesHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Gateway gateway = getGatewayDevice();
            Log.i(LOG_TAG, "getNodesHandler");
            Log.i(LOG_TAG, "Entering LAN mode with gateway " + gateway);
            gateway.getDevice().lanModeEnable();
        }
    };

    private void enterLANMode() {
        Log.d(LOG_TAG, "enterLANMode");

        if ( _startingLANMode ) {
            Log.i(LOG_TAG, "enterLANMode: Already entering...");
            return;
        }

        _startingLANMode = true;
        AylaLanMode.enable(_lanModeHandler, _reachabilityHandler);

        if ( AylaLanMode.lanModeState == AylaLanMode.lanMode.ENABLED ||
             AylaLanMode.lanModeState == AylaLanMode.lanMode.RUNNING ) {
            // Enable LAN mode on the gateway, if present
            Gateway gateway = getGatewayDevice();
            if ( gateway != null ) {
                // Get the nodes from the gateway. This establishes the mapping between devices
                // and nodes, required for LAN mode operation to work right.
                Log.i(LOG_TAG, "Fetching nodes from gateway...");
                gateway.getGatewayDevice().getNodes(_getNodesHandler, null);
            } else {
                Log.i(LOG_TAG, "LAN mode: No gateway found");
                _startingLANMode = false;
                _lanModeEnabled = true;
            }
        } else {
            Log.e(LOG_TAG, "LAN mode: lanModeState is " + AylaLanMode.lanModeState + " - not entering LAN mode");
            _startingLANMode = false;
        }
    }

    // Timer handlers and runnables

    private Handler _deviceListTimerHandler = new Handler();
    private Handler _deviceStatusTimerHandler = new Handler();

    private Runnable _deviceListTimerRunnable = new Runnable() {
        @Override
        public void run() {
            Log.v(LOG_TAG, "Device List Timer");

            if ( _startingLANMode ) {
                Log.i(LOG_TAG, "Not querying device list while entering LAN mode");
                return;
            }

            fetchDeviceList();
            getGroupManager().fetchDeviceGroups();

            // Only continue polling if somebody is listening
            if (_deviceListListeners.size() > 0) {
                _deviceListTimerHandler.removeCallbacksAndMessages(null);
                _deviceListTimerHandler.postDelayed(this, _deviceListPollInterval);
            } else {
                Log.d(LOG_TAG, "Device List Timer: Nobody listening or in LAN mode");
            }
        }
    };

    private ArrayList<Device> _devicesToPoll;

    private Runnable _deviceStatusTimerRunnable = new Runnable() {
        @Override
        public void run() {
            Log.v(LOG_TAG, "Device Status Timer");

            // If we're in the process of entering LAN mode, don't query devices yet.
            if ( _startingLANMode ) {
                Log.i(LOG_TAG, "Not querying device status while entering LAN mode");
                return;
            }

            // Update each device via the service. We need to do this one device at a time; the
            // library does not handle a series of requests all at once at this time.
            if ( _devicesToPoll == null ) {
                _devicesToPoll = new ArrayList<Device>(_deviceList);
                updateNextDeviceStatus();
            } else {
                Log.d(LOG_TAG, "Already polling device status. Not doing it again so soon.");
                Log.v(LOG_TAG, "Waiting for responses from:");
                for ( Device d : _devicesToPoll ) {
                    Log.v(LOG_TAG, d.getDevice().getProductName());
                }

                // TODO: BSK: Should we really be trying again here?
                Log.w(LOG_TAG, "Canceling pending list!");
                _devicesToPoll = new ArrayList<Device>(_deviceList);
                updateNextDeviceStatus();
            }

            // Only continue polling if we're not in LAN mode and somebody is listening
            if ( !isLanModeEnabled() && _deviceStatusListeners.size() > 0 ) {
                _deviceStatusTimerHandler.removeCallbacksAndMessages(null);
                _deviceStatusTimerHandler.postDelayed(this, _deviceStatusPollInterval);
            } else {
                Log.d(LOG_TAG, "Device Status Timer: Nobody listening or in LAN mode");
            }
        }
    };

    private void updateNextDeviceStatus() {
        Log.v(LOG_TAG, "updateNextDeviceStatus");
        if ( _devicesToPoll == null || _devicesToPoll.size() == 0 ) {
            // We're done.
            _devicesToPoll = null;
            Log.v(LOG_TAG, "No more devices to poll");
            return;
        }

        Device d = _devicesToPoll.remove(0);
        Log.v(LOG_TAG, "Updating status for " + d.getDevice().productName);
        if ( _devicesToPoll.size() == 0 ) {
            _devicesToPoll = null;
        }
        d.updateStatus(this);
    }

    /** Private methods */

    /** Handler called when the list of devices has been obtained from the server. */
    private final Handler _getDevicesHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
                // Create our device array
                Log.v(LOG_TAG, "Device list JSON: " + msg.obj);
                List<Device> newDeviceList = new ArrayList<Device>();
                AylaDevice[] devices = AylaSystemUtils.gson.fromJson((String)msg.obj, AylaDevice[].class);
                SessionManager.SessionParameters params = SessionManager.sessionParameters();
                for ( AylaDevice aylaDevice : devices ) {
                    // Get the correct object from the device creator.
                    Device device = params.deviceCreator.deviceForAylaDevice(aylaDevice);
                    if ( device != null ) {
                        newDeviceList.add(device);
                    } else {
                        Log.i(LOG_TAG, "No device created for " + aylaDevice.getProductName());
                    }
                }

                if ( deviceListChanged(newDeviceList) ) {
                    _deviceList = newDeviceList;
                    notifyDeviceListChanged();

                    // Do we need to enter LAN mode?
                    if ( params.enableLANMode && !DeviceManager.this.isLanModeEnabled() ) {
                        enterLANMode();
                    }
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

        // See if any of the devices have changed.
        for ( int i = 0; i < _deviceList.size(); i++ ) {
            Device dev1 = _deviceList.get(i);
            Device dev2 = newDeviceList.get(i);
            if ( dev1.isDeviceChanged(dev2) ) {
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
        if ( _devicesToPoll != null ) {
            updateNextDeviceStatus();
        }
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

    private void notifyLANModeChange() {
        // TODO: Notify whoever cares?
    }
}
