package com.aylanetworks.agilelink.framework;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaDeviceManager;
import com.aylanetworks.aaml.AylaDeviceNode;
import com.aylanetworks.aaml.AylaDeviceNotification;
import com.aylanetworks.aaml.AylaLanMode;
import com.aylanetworks.aaml.AylaLanModule;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaNotify;
import com.aylanetworks.aaml.AylaShare;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.aaml.zigbee.AylaDeviceZigbeeNode;
import com.aylanetworks.aaml.zigbee.AylaGroupZigbee;
import com.aylanetworks.agilelink.AgileLinkApplication;
import com.aylanetworks.agilelink.framework.Device.DeviceStatusListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/*
 * DeviceManager.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 12/19/14.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */


/**
 * The DeviceManager manages the list of Device objects associated with the user's account. It
 * polls the list of devices and the status of each device, and can notify interested parties of
 * changes.
 *
 * The class also provides methods to add and remove devices (register / unregister), enter / exit
 * LAN mode and to update the device notifications for a particular device.
 *
 * The {@link com.aylanetworks.agilelink.framework.SessionManager} creates an instance of the
 * DeviceManager, which can be obtained via a call to
 * {@link SessionManager#deviceManager()}. This instance should be used throughout the application.
 * Additional instances of the DeviceManager should not be created.
 */
public class DeviceManager implements DeviceStatusListener {

    private final static String LOG_TAG = "DeviceManager";

    private int _deviceListPollInterval = 30000;
    private int _deviceStatusPollInterval = 5000;

    private static final String PREF_LAST_LAN_MODE_DEVICE = "lastLANModeDevice";

    /** Interfaces */
    public interface DeviceListListener {
        /**
         * Called when the list of devices has changed.
         */
        void deviceListChanged();
    }

    /**
     * Useful method for returning a Device list as a string for logging output
     * @param list Device list
     * @return String of Device DSN
     */
    static public String deviceListToString(List<Device> list) {
        StringBuilder sb = new StringBuilder(512);
        if (list != null) {
            if (list.isEmpty()) {
                sb.append("empty");
            } else {
                sb.append("{");
                for (Device device : list) {
                    if (sb.length() > 1) {
                        sb.append(",");
                    }
                    sb.append(device.getDeviceDsn());
                }
                sb.append("}");
            }
        } else {
            sb.append("null");
        }
        return sb.toString();
    }

    /**
     * Listener class called when a device has entered or exited LAN mode. The LANModeListener
     * retains a reference to the underlying device object, which is passed back to the lanModeResult
     * method when the LAN mode state has changed.
     */
    static public class LANModeListener {
        private Device _device;
        public LANModeListener(Device device) {
            _device = device;
        }
        public Device getDevice() {
            return _device;
        }

        public void lanModeResult(boolean isInLANMode){}
    }

    private List<LANModeListener> _lanModeListeners = new ArrayList<>();

    /** Public Helper Methods */

    /**
     * Checks to see if the specified DSN is in the provided AylaDeviceNode list.
     * @param dsn Device DSN
     * @param devices List of AylaDeviceNode devices.
     * @return Returns true if the DSN is in the list, false if the DSN is not in the list.
     */
    static public boolean isDsnInAylaDeviceNodeList(String dsn, List<AylaDeviceNode> devices) {
        if (devices == null || devices.size() == 0)
            return false;
        for (AylaDeviceNode adn : devices) {
            if (TextUtils.equals(dsn, adn.dsn)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks to see if the specified DSN is in the provided Device list.
     * @param dsn Device DSN
     * @param devices List of Device devices.
     * @return Returns true if the DSN is in the list, false if the DSN is not in the list.
     */
    static public boolean isDsnInDeviceList(String dsn, List<Device> devices) {
        if (devices == null || devices.size() == 0)
            return false;
        for (Device d : devices) {
            if (TextUtils.equals(dsn, d.getDeviceDsn())) {
                return true;
            }
        }
        return false;
    }

    /** Public Methods */

    /** Constructor */
    DeviceManager() {
        _registrationMode = false;

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
        if ( _deviceList == null ) {
            return null;
        }
        return new ArrayList<>(_deviceList);
    }

    /**
     * Returns the gateway device, or null if one is not found
     * @return The gateway device, or null if one is not found
     */
    public Gateway getGatewayDevice() {
        if ((_deviceList != null) && (_deviceList.size() > 0)) {
            for (Device gateway : _deviceList) {
                if (gateway.isGateway()) {
                    return (Gateway) gateway;
                }
            }
        }
        return null;
    }

    /**
     * Get all gateways belong to this accout, from device collection in AMAP space
     *
     * @return gateway collection
     * */
    List<Gateway> getAllGateways() {
        List<Gateway> gws = new ArrayList<>();
        if (_deviceList == null || _deviceList.isEmpty()) {
            Log.w(LOG_TAG, "DeviceManager, getAllGateways, _deviceList not initialized.");
            return gws;
        }

        for (Device d : _deviceList) {
            if (d.isGateway()) {
                gws.add((Gateway)d);
            }
        }
        return gws;
    }// end of getAllGateways

    /**
     * Returns a list of available gateway devices.
     * @return List of gateway devices.
     */
    public List<Gateway> getGatewayDevices() {
        List<Gateway> list = new ArrayList<Gateway>();
        if ((_deviceList != null) && (_deviceList.size() > 0)) {
            for (Device gateway : _deviceList) {
                if (gateway.isGateway()) {
                    Log.i(LOG_TAG, "zn: getGatewayDevices [" + gateway.getDeviceDsn() + "]");
                    list.add((Gateway) gateway);
                }
            }
        }
        return list;
    }

    /**
     * Returns a list of available devices of the specified class
     *
     * @param classes Array of class to match against
     * @return List of matching devices
     */
    public List<Device> getDevicesOfClass(Class[] classes) {
        if ((_deviceList==null) || (classes==null) || (classes.length==0)) {
            return null;
        }

        List<Device> devices = new ArrayList<>(_deviceList);
        List<Device> list = new ArrayList<Device>();
        for (Device device : devices) {
            for (Class clazz : classes) {
                if (clazz.isInstance(device)) {
                    list.add(device);
                    break;
                }
            }
        }
        return list;
    }

    /**
     * The GetDeviceComparable interface is used by the getDevicesOfComparableType method
     * to return a list of devices of a certain type.
     */
    public interface GetDeviceComparable {

        /**
         * Interface method used to determine if a device is of a certain type.
         * @param another Device to examine.
         * @return true if the device is of the correct type. false if it is not.
         */
        public boolean isDeviceComparableType(Device another);
    }

    /**
     * Returns a list of available devices of the specified type, as determined by
     * the GetDeviceComparable interface.
     *
     * @param comparable GetDeviceComparable used to compare.
     * @return List of matching devices
     */
    public List<Device> getDevicesOfComparableType(GetDeviceComparable comparable) {
        if ((_deviceList==null) || (comparable==null)) {
            return null;
        }

        List<Device> devices = new ArrayList<>(_deviceList);
        List<Device> list = new ArrayList<Device>();
        for (Device device : devices) {
            if (comparable.isDeviceComparableType(device)) {
                list.add(device);
            }
        }
        return list;
    }

    /**
     * Returns the device with the given DSN, or null if not found
     * @param dsn the DSN of the device to find
     * @return The found device, or null if not found
     */
    @Nullable
    public Device deviceByDSN(String dsn) {
        if (_deviceList != null) {
            for (Device d : _deviceList) {
                if (d.getDeviceDsn().compareTo(dsn) == 0) {
                    return d;
                }
            }
        }
        return null;
    }

    /**
     * Remove the device from the device list and any groups it may belong in.
     *
     * @param device Device to remove.
     */
    public void removeDevice(Device device) {
        if (device != null) {
            // remove from all groups
            getGroupManager().removeDeviceFromAllGroups(device);

            // If a device node, then let the Gateway know we are removing it
            if (device.isDeviceNode()) {
                Gateway gateway = Gateway.getGatewayForDeviceNode(device);
                gateway.removeDeviceNode(device);
                device.preUnregistrationForGatewayDevice(gateway);
            }

            // remove from the property list so that we don't try to get properties for it.
            _deviceList.remove(device);
        }
    }

    /**
     * Refreshes the list of devices.
     */
    public void refreshDeviceList() {
        fetchDeviceList();
    }

    /**
     * Optional interface to be used when fetching devices.
     */
    public interface GetDevicesCompletion {

        /**
         * Method invoked when fetchDeviceList has completed.
         *
         * @param msg Message
         * @param newDeviceList Device list.
         * @param tag Optional user data.
         */
        public void complete(Message msg, List<Device> newDeviceList, Object tag);
    }

    /**
     * Refreshes the list of devices, calling the provided completion handler when the list
     * has been returned and processed.
     *
     * @param tag Optional user data.
     * @param completion Completion handler.
     */
    public void refreshDeviceListWithCompletion(Object tag, GetDevicesCompletion completion) {
        fetchDeviceListWithCompletion(tag, completion);
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

    /**
     * Method to notify the device manager that a device has changed. The device manager will
     * update listeners to let them know.
     *
     * @param device device that changed
     */
    public void deviceChanged(Device device) {
        Log.v(LOG_TAG, "dev: deviceChanged [" + device.getDeviceDsn() + "]");
        notifyDeviceStatusChanged(device);
        notifyDeviceListChanged();
    }

    /**
     * Sets the comparator used for sorting the list of devices. The default comparator uses the
     * Device object's compareTo method.
     *
     * @param comparator Comparator used to sort the list of devices
     */
    public void setDeviceComparator(Comparator<Device> comparator) {
        _deviceComparator = comparator;
        Collections.sort(_deviceList, _deviceComparator);
    }

    /**
     * Returns the Comparator used to sort the device list
     * @return the Comparator used to sort the device list
     */
    public Comparator<Device>getDeviceComparator() {
        return _deviceComparator;
    }

    /**
     * Starts polling for device list and status changes
     */
    public void startPolling() {
        Logger.logDebug(LOG_TAG, "poll: startPolling");
        stopPollingWithLog(false);
        _pollingStopped = false;
        _deviceListTimerRunnable.run();
        _deviceStatusTimerHandler.postDelayed(_deviceStatusTimerRunnable, _deviceStatusPollInterval);
    }

    /**
     * Stops polling for device list and status changes
     */
    private boolean _pollingStopped;

    public void stopPolling() {
        stopPollingWithLog(true);
    }

    private void stopPollingWithLog(boolean verbose) {
        if (verbose) {
            Logger.logDebug(LOG_TAG, "poll: stopPolling");
        }
        _pollingStopped = true;
        _deviceListTimerHandler.removeCallbacksAndMessages(null);
        _deviceStatusTimerHandler.removeCallbacksAndMessages(null);
        if ((_devicesToPoll != null) && (_devicesToPoll.size() > 0)) {
            _devicesToPoll.clear();
        }
    }

    public void enterLANMode(LANModeListener listener) {
        Device device = listener.getDevice();
        if (!SessionManager.getInstance().lanModePermitted()) {
            // We can't enter LAN mode for any devices.
            Logger.logDebug(LOG_TAG, "lm: enterLANMode [" + device.getDeviceDsn() + "] not permitted");
            listener.lanModeResult(false);
        } else {
            if ( device.isInLanMode() ) {
                Logger.logDebug(LOG_TAG, "lm: enterLANMode [" + device.getDeviceDsn() + "] already in LAN mode.");
                listener.lanModeResult(true);
            } else {
                _lanModeListeners.add(listener);
                Logger.logDebug(LOG_TAG, "lm: enterLANMode [" + device.getDeviceDsn() + "] lanModeEnable");
                device.getDevice().lanModeEnable();
            }
        }
    }

    /**
     * Exits LAN mode. The listener is immediately notified.
     * @param listener Listener to be notified when LAN mode has been exited.
     */
    public void exitLANMode(LANModeListener listener) {
        // BSK: There is no notification received for disabling LAN mode.
        // The interface is kept consistent with enterLANMode(), but is strictly speaking not
        // necessary, as there is nothing asynchronous that goes on here.
        //
        // Also it appears that lanModeDisable() doesn't currently do anything other than write
        // the device's properties to the cache, so we won't call that here either, as it
        // seems to screw up our property notifications, etc.

        // _lanModeListeners.add(listener);
        // listener.getDevice().getDevice().lanModeDisable();
        listener.lanModeResult(false);
        if (listener._device != null) {
            deviceChanged(listener._device);
        }

        // Start polling our device status again so we include this device
        startPolling();
    }

    /**
     * Saves the specified device as the last device to enter LAN mode. This device will be
     * asked to re-enter LAN mode the next time the application is launched.
     *
     * @param device Device that has just entered LAN mode
     */
    public void setLastLanModeDevice(Device device) {
        // Save the last LAN mode device in user settings so we can re-enable it next time
        SharedPreferences prefs = AgileLinkApplication.getSharedPreferences();
        if ( device != null ) {
            prefs.edit().putString(PREF_LAST_LAN_MODE_DEVICE, device.getDeviceDsn()).apply();
        } else {
            prefs.edit().remove(PREF_LAST_LAN_MODE_DEVICE);
        }
    }

    /**
     * Returns true if the specified device was the last device supplied to setLastLanModeDevice().
     * @param device Device to check
     * @return true if this device was the last LAN-mode enabled device, otherwise false
     */
    public boolean isLastLanModeDevice(Device device) {
        SharedPreferences prefs = AgileLinkApplication.getSharedPreferences();
        String lastDSN = prefs.getString(PREF_LAST_LAN_MODE_DEVICE, "");
        return lastDSN != null && device.getDeviceDsn() != null && device.getDeviceDsn().equals(lastDSN);
    }

    /**
     * Listener class called when device notifications have been updated
     */
    static public class DeviceNotificationListener {
        /**
         * Called in response to updateDeviceNotifications after all devices have been updated or
         * an error was encountered.
         * @param succeeded true if all devices were updated, false otherwise
         * @param failureMessage If succeeded == false, this contains the message from the server
         *                       that contains the error
         */
        public void notificationsUpdated(boolean succeeded, Message failureMessage) {}
    }

    /**
     * Sets or clears notifications on all devices.
     *
     * @param notificationType Type of notification to set / clear
     * @param enable true to set the notification, false to clear it
     * @param listener listener to be notified when the job is done
     */
    public void updateDeviceNotifications(String notificationType, boolean enable, DeviceNotificationListener listener) {
        List<Device> devicesToUpdate = new ArrayList<>(_deviceList);
        UpdateAllNotificationsHelper helper =
                new UpdateAllNotificationsHelper(devicesToUpdate,
                        notificationType,
                        enable,
                        listener);
        helper.updateNextDevice();
    }

    private static class UpdateAllNotificationsHelper extends DeviceNotificationHelper.DeviceNotificationHelperListener {
        private List<Device> _devicesToUpdate;
        private DeviceNotificationListener _listener;
        private String _notificationType;
        private boolean _enable;

        public UpdateAllNotificationsHelper(List<Device> devices,
                                            String notificationType,
                                            boolean enable,
                                            DeviceNotificationListener listener) {
            _devicesToUpdate = devices;
            _notificationType = notificationType;
            _enable = enable;
            _listener = listener;
        }

        public void updateNextDevice() {
            if (_registrationMode) {
                Log.d(LOG_TAG, "rn: updateNextDevice - ignoring in registration mode");
                _devicesToUpdate.clear();
                return;
            }

            if ( _devicesToUpdate.isEmpty() ) {
                // We're done!
                _listener.notificationsUpdated(true, null);
                return;
            }

            // Get the device at the head of the list  and update it.
            Device d = _devicesToUpdate.remove(0);
            DeviceNotificationHelper helper = new DeviceNotificationHelper(d, AylaUser.getCurrent());
            helper.enableDeviceNotifications(
                    _notificationType,
                    _enable,
                    new DeviceNotificationHelper.DeviceNotificationHelperListener() {
                        @Override
                        public void deviceNotificationUpdated(Device device, AylaDeviceNotification notification, String notificationType, int error) {
                            if (error == AylaNetworks.AML_ERROR_OK) {
                                // We're done with this device. On to the next!
                                updateNextDevice();
                            } else {
                                // Something went wrong.
                                _listener.notificationsUpdated(false, lastMessage);
                            }
                        }
                    });
        }
    }

    /**
     * Clears the list of devices and stops polling.
     */
    boolean _shuttingDown;
    public void shutDown() {
        Log.i(LOG_TAG, "Shut down");
        _shuttingDown = true;
        // Clear out our list of devices, and then notify listeners that the list has changed.
        // This should cause all listeners to clear any devices they may be displaying
        if ( _deviceList != null ) {
            _deviceList.clear();
            notifyDeviceListChanged();
        }

        // Get rid of our listeners.
        _deviceStatusListeners.clear();
        _deviceListListeners.clear();

        // Stop polling!
        stopPolling();
        _shuttingDown = false;
    }

    public boolean isShuttingDown() {
        return _shuttingDown;
    }

    // Poll interval methods

    /**
     * Returns the poll interval for polling the list of devices, in milliseconds.
     * @return Number of ms between calls to fetch the list of devices
     */
    public int getDeviceListPollInterval() {
        return _deviceListPollInterval;
    }

    /**
     * Sets the poll interval for polling the list of devices, in milliseconds.
     * @param deviceListPollInterval Number of ms to wait before requesting the device list again
     */
    public void setDeviceListPollInterval(int deviceListPollInterval) {
        _deviceListPollInterval = deviceListPollInterval;
    }

    /**
     * Returns the poll interval for polling the status of devices, in milliseconds.
     * @return The number of milliseconds to wait before fetching the device statuses again
     */
    public int getDeviceStatusPollInterval() {
        return _deviceStatusPollInterval;
    }

    /**
     * Sets the poll interval for polling the status of devices, in milliseconds
     * @param deviceStatusPollInterval Number of ms to wait between device status fetches
     */
    public void setDeviceStatusPollInterval(int deviceStatusPollInterval) {
        _deviceStatusPollInterval = deviceStatusPollInterval;
    }

    // Listener methods

    /**
     * Adds a DeviceListListener to be notified when the list of devices has changed.
     * @param listener Listener to be notified of device list changes
     */
    public void addDeviceListListener(DeviceListListener listener) {
        boolean startTimer = (_deviceListListeners.size() == 0);
        _deviceListListeners.add(listener);
        if ( startTimer ) {
            _deviceListTimerHandler.removeCallbacksAndMessages(null);
            _deviceListTimerHandler.postDelayed(_deviceListTimerRunnable, _deviceStatusPollInterval);
        }
    }

    /**
     * Removes a listener to be notified on device list changes
     * @param listener Listener to be removed
     */
    public void removeDeviceListListener(DeviceListListener listener) {
        _deviceListListeners.remove(listener);
    }

    /**
     * Adds a listener to be notified of device status changes. The listener will be called once
     * for each device that has changed status.
     *
     * @param listener Listener to be notified of device status changes.
     */
    public void addDeviceStatusListener(DeviceStatusListener listener) {
        boolean startTimer = (_deviceStatusListeners.size() == 0);
        _deviceStatusListeners.add(listener);
        if ( startTimer ) {
            _deviceStatusTimerHandler.removeCallbacksAndMessages(null);
            _deviceStatusTimerHandler.postDelayed(_deviceStatusTimerRunnable, _deviceStatusPollInterval);
        }
    }

    /**
     * Removes a listener to be notfied of changes to device status
     * @param listener Listener to be removed
     */
    public void removeDeviceStatusListener(DeviceStatusListener listener) {
        _deviceStatusListeners.remove(listener);
    }

    // Groups
    protected GroupManager _groupManager;
    public GroupManager getGroupManager() {
        return _groupManager;
    }

     /** Private members */

    private List<Device> _deviceList;
    private Set<DeviceListListener> _deviceListListeners;
    private Set<DeviceStatusListener> _deviceStatusListeners;

    // This is set to true while we are attempting to added nodes
    private static boolean _registrationMode = false;

    public void setRegistrationMode(boolean value) {
        _registrationMode = value;
        if (value) {
            // make sure we ARE in LAN mode.
            if (!AylaLanMode.isLanModeRunning()) {
                enterLANMode();
            } else {
                Logger.logVerbose(LOG_TAG, "rn: lan mode is already running.");
            }
        }
    }

    // Default comparator uses the device's compareTo method. Can be updated with setComparator().
    private Comparator<Device> _deviceComparator = new Comparator<Device>() {
        @Override
        public int compare(Device lhs, Device rhs) {
            return lhs.compareTo(rhs);
        }
    };

    private LanModeHandler _lanModeHandler = new LanModeHandler(this);

    // LAN mode handler
    static class LanModeHandler extends Handler {
        private WeakReference<DeviceManager> _deviceManager;

        public LanModeHandler(DeviceManager deviceManager) {
            _deviceManager = new WeakReference<DeviceManager>(deviceManager);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            String notifyResults = (String) msg.obj;
            AylaNotify notify = AylaSystemUtils.gson.fromJson(notifyResults, AylaNotify.class);

            String type = notify.type;
            String dsn = notify.dsn;
            String names[] = notify.properties;

            Log.d(LOG_TAG, "lanModeHandler: " + msg);

            if (_deviceManager.get() == null) {
                Log.i(LOG_TAG, "deviceManager has been removed.");
                return;
            }
            if (TextUtils.equals(type, AylaNetworks.AML_NOTIFY_TYPE_SESSION)) {
                if (msg.arg1 > 399) {
                    // LAN mode could not be enabled
                    Log.i(LOG_TAG, "Failed to enter LAN mode: " + msg.arg1 + " " + msg.obj);
                    _deviceManager.get().notifyLANModeChange();

                    // Notify our listeners for this device, if any, and clear our list
                    for (Iterator<LANModeListener> iter = _deviceManager.get()._lanModeListeners.iterator(); iter.hasNext(); ) {
                        LANModeListener listener = iter.next();
                        boolean shouldNotify = listener.getDevice().getDevice().dsn.equals(dsn);
                        if ( !shouldNotify ) {
                            // Check for a node / gateway
                            if ( listener.getDevice().isDeviceNode() ) {
                                AylaDeviceNode node = (AylaDeviceNode)listener.getDevice().getDevice();
                                shouldNotify = (node.gatewayDsn.equals(dsn));
                            }
                        }
                        if (shouldNotify) {
                            listener.lanModeResult(false);
                            iter.remove();
                            _deviceManager.get().notifyDeviceStatusChanged(listener.getDevice());
                        }
                    }
                } else {
                    if (msg.arg1 >= 200 && msg.arg1 < 300) {
                        // LAN mode has been enabled on a device.
                        Device device = _deviceManager.get().deviceByDSN(dsn);
                        _deviceManager.get().setLastLanModeDevice(device);

                        if (device != null) {
                            Logger.logDebug(LOG_TAG, "lm: [" + device.getDeviceDsn() + "] handleMessage ENABLED " + msg);
                            Log.d(LOG_TAG, "LAN mode enabled on " + device.toString());
                            // Update the properties on this device to the AylaDeviceManager's properties
                            device.syncLanProperties();

                            // Also request an update, as these properties could be from the cache.
                            Log.e("BSK", "Requesting LAN update for " + device);
                            device.updateStatus(_deviceManager.get());

                            // Remove the LAN-mode-enabled device from the list of devices to poll
                            List<Device> devicesToPoll = _deviceManager.get()._devicesToPoll;
                            if (devicesToPoll != null) {
                                devicesToPoll.remove(device);
                            }

                            // Notify listeners listening for this device
                            for (Iterator<LANModeListener> iter = _deviceManager.get()._lanModeListeners.iterator(); iter.hasNext(); ) {
                                LANModeListener listener = iter.next();

                                boolean shouldNotify = listener.getDevice().getDevice().dsn.equals(dsn);
                                if ( !shouldNotify ) {
                                    // Check for a node / gateway
                                    if ( listener.getDevice().isDeviceNode() ) {
                                        AylaDeviceNode node = (AylaDeviceNode)listener.getDevice().getDevice();
                                        shouldNotify = (node.gatewayDsn.equals(dsn));
                                    }
                                }

                                if (shouldNotify) {

                                    // Notify the listener that LAN mode has been enabled
                                    listener.lanModeResult(true);
                                    iter.remove();
                                    _deviceManager.get().notifyDeviceStatusChanged(listener.getDevice());
                                }
                            }
                            _deviceManager.get().deviceChanged(device);
                        } else {
                            Log.e(LOG_TAG, "Unknown device [" + dsn + "] has entered LAN mode???");
                        }
                    }

                    // If the LAN device has changed, then the list has changed.
                    _deviceManager.get().notifyDeviceListChanged();
                }
            } else if (TextUtils.equals(type, AylaNetworks.AML_NOTIFY_TYPE_PROPERTY) || TextUtils.equals(type, AylaNetworks.AML_NOTIFY_TYPE_NODE)) {
                // A device's property has changed.
                Device d = _deviceManager.get().deviceByDSN(dsn);
                if ( d != null ) {
                    Log.d(LOG_TAG, "LAN mode handler: Device changed: " + d);
                    d.syncLanProperties();
                    _deviceManager.get().notifyDeviceStatusChanged(d);
                } else {
                    Log.e(LOG_TAG, "Notify for a device I don't have: " + dsn);
                }
            } else {
                // We don't know what changed, so let's just get everything
                Log.e(LOG_TAG, "LAN mode handler (property change): Couldn't find device with DSN: " + dsn);
                _deviceManager.get()._deviceStatusTimerHandler.postDelayed(_deviceManager.get()._deviceStatusTimerRunnable, 0);
            }
        }
    }

    // Reachability handler
    static class ReachabilityHandler extends Handler {
        private WeakReference<DeviceManager> _deviceManager;

        public ReachabilityHandler(DeviceManager deviceManager) {
            _deviceManager = new WeakReference<DeviceManager>(deviceManager);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String json = (String)msg.obj;
            Log.d(LOG_TAG, "Reachability handler: " + json);
        }
    }

    private ReachabilityHandler _reachabilityHandler = new ReachabilityHandler(this);

    static class GetNodesHandler extends Handler {
        private WeakReference<DeviceManager> _deviceManager;

        public GetNodesHandler(DeviceManager deviceManager) {
            _deviceManager = new WeakReference<DeviceManager>(deviceManager);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(LOG_TAG, "getNodesHandler");
            Gateway gateway = _deviceManager.get().getGatewayDevice();
            Log.i(LOG_TAG, "Entering LAN mode with gateway " + gateway);
            if (gateway != null) {
                gateway.getDevice().lanModeEnable();
            }
        }
    }

    private GetNodesHandler _getNodesHandler = new GetNodesHandler(this);

    private void enterLANMode() {
        Log.d(LOG_TAG, "rn: enterLANMode");
        AylaLanMode.enable(_lanModeHandler, _reachabilityHandler);

      /*  if ( AylaLanMode.lanModeState == AylaLanMode.lanMode.ENABLED ||
             AylaLanMode.lanModeState == AylaLanMode.lanMode.RUNNING ) {*/
        if (  AylaLanMode.isLanModeRunning() || AylaLanMode.isLanModeEnabled()) {
            // Enable LAN mode on the gateway, if present
            List<Gateway> gateways = getAllGateways();

            if (gateways == null || gateways.isEmpty()) {
                Log.i(LOG_TAG, "rn: LAN mode: No gateway found");
                return;
            }

            for (Gateway gw: gateways) {
                Log.d(LOG_TAG, "rn: Fetching nodes for gateway " + gw.getDeviceDsn());
                gw.getGatewayDevice().getNodes(_getNodesHandler, null);
            }
        } else {
            Log.e(LOG_TAG, "rn: LAN mode: lanModeState is " + AylaLanMode.getLanModeState() + " - not entering LAN mode");
        }
    }

    // Timer handlers and runnables

    private Handler _deviceListTimerHandler = new Handler();
    private Handler _deviceStatusTimerHandler = new Handler();

    private Runnable _deviceListTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (_pollingStopped) {
                Logger.logDebug(LOG_TAG, "poll:d polling stopped.");
                return;
            }
            if (_registrationMode) {
                Log.d(LOG_TAG, "rn: deviceListTimer - ignoring in registration mode");
                return;
            }
            Logger.logVerbose(LOG_TAG, "poll:Device List Timer");

            fetchDeviceList();
            getGroupManager().fetchDeviceGroups();

            // Only continue polling if somebody is listening
            if (!_pollingStopped && _deviceListListeners.size() > 0) {
                _deviceListTimerHandler.removeCallbacksAndMessages(null);
                _deviceListTimerHandler.postDelayed(this, _deviceListPollInterval);
            } else {
                Logger.logDebug(LOG_TAG, "poll:Device List Timer: Nobody listening or in LAN mode");
            }
        }
    };

    private List<Device>getDevicesToPoll() {
        List<Device> devices = new ArrayList<Device>();
        for ( Device d : _deviceList ) {
            if (    !d.isInLanMode() ||
                    d.getDevice().properties == null ||
                    d.getDevice().properties.length == 0 ) {
                devices.add(d);
            }
        }
        return devices;
    }

    // List of devices we will poll the status of. Each device is removed from the head of the
    // list and queried for its properties.
    private List<Device> _devicesToPoll;
    private Runnable _deviceStatusTimerRunnable = new Runnable() {
        @Override
        public void run() {
            Logger.logVerbose(LOG_TAG, "Device Status Timer");

            // Trigger the next timer
            _deviceStatusTimerHandler.removeCallbacksAndMessages(null);
            _deviceStatusTimerHandler.postDelayed(this, _deviceStatusPollInterval);

            if (_pollingStopped) {
                Logger.logDebug(LOG_TAG, "poll:s polling stopped");
                return;
            }
            if (_registrationMode) {
                Log.d(LOG_TAG, "rn: deviceStatusTimer - ignoring in registration mode");
                return;
            }
            Logger.logVerbose(LOG_TAG, "poll:Device Status Timer");

            // Update each device via the service. We need to do this one device at a time; the
            // library does not handle a series of requests all at once at this time.
            if ( _devicesToPoll == null ) {
                if ( _deviceList != null ) {
                    _devicesToPoll = getDevicesToPoll();
                }
                updateNextDeviceStatus();
            } else {
                Logger.logDebug(LOG_TAG, "poll:s Still not done polling from the last timer event.");
                Logger.logVerbose(LOG_TAG, "poll:s Current queue is:");
                for ( Device d : _devicesToPoll ) {
                    Logger.logVerbose(LOG_TAG, d.getProductName());
                }
                updateNextDeviceStatus();
            }
        }
    };

    private void updateNextDeviceStatus() {
        if (_registrationMode) {
            Log.d(LOG_TAG, "rn: updateNextDeviceStatus - ignoring in registration mode");
            return;
        }
        Logger.logVerbose(LOG_TAG, "updateNextDeviceStatus");
        if ( _devicesToPoll == null || _devicesToPoll.size() == 0 ) {
            // We're done.
            _devicesToPoll = null;
            Logger.logVerbose(LOG_TAG, "No more devices to poll");
            return;
        }

        Device d = _devicesToPoll.remove(0);
        Logger.logVerbose(LOG_TAG, "poll: device status [" + d.getDeviceDsn() + ":" + d.getProductName() + "]");
        if ( _devicesToPoll.size() == 0 ) {
            _devicesToPoll = null;
        }
        d.updateStatus(this);
    }

    /** Private methods */

    /** Handler called when the list of devices has been obtained from the server. */
    static class GetDevicesHandler extends Handler {
        private final WeakReference<DeviceManager> _deviceManager;
        final ArrayList<GetDevicesCompletion> _completionSet;
        ArrayList<Object> _tagSet;

        GetDevicesHandler(DeviceManager manager) {
            _deviceManager = new WeakReference<DeviceManager>(manager);
            _completionSet = new ArrayList<>();
            _tagSet = new ArrayList<>();
        }

        void addCompletion(Object tag, GetDevicesCompletion completion) {
            synchronized (_completionSet) {
                _completionSet.add(completion);
                _tagSet.add(tag);
            }
        }

        private Device getDeviceFromList(List<Device> list, Device device) {
            String dsn = device.getDeviceDsn();
            for (Device d : list) {
                if (TextUtils.equals(d.getDeviceDsn(), dsn)) {
                    return d;
                }
            }
            return null;
        }

        @Override
        public void handleMessage(Message msg) {
            DeviceManager deviceManager = _deviceManager.get();
            if (deviceManager == null) {
                Logger.logError(LOG_TAG, "no deviceManager");
                return;
            }
            List<Device> newDeviceList = new ArrayList<Device>();
            if ( AylaNetworks.succeeded(msg) ) {
                // Create our device array
                Log.v(LOG_TAG, "dev: device list JSON " + msg.obj);
                AylaDevice[] devices = AylaSystemUtils.gson.fromJson((String)msg.obj, AylaDevice[].class);
                SessionManager.SessionParameters params = SessionManager.sessionParameters();
                for ( AylaDevice aylaDevice : devices ) {
                    // Get the correct object from the device creator.
                    Device device = params.deviceCreator.deviceForAylaDevice(aylaDevice);
                    if ( device != null ) {
                        newDeviceList.add(device);
                        if ( AylaLanMode.isLanModeRunning() ) {
                            if ( !device.isDeviceNode() && !device.isInLanMode() ) {
                                Log.d("BSK", "LAN mode enabling " + device);
                                AylaDevice lanDevice = AylaDeviceManager.sharedManager().deviceWithDSN(device.getDevice().dsn);
                                if (lanDevice != null) {
                                    lanDevice.lanModeEnable();
                                } else {
                                    Logger.logError(LOG_TAG, "no device with dsn [%s]", device.getDevice().dsn);
                                }
                            }
                        }
                    } else {
                        Log.i(LOG_TAG, "No device created for " + aylaDevice.getProductName());
                    }
                }

                if ( deviceManager.deviceListChanged(newDeviceList) ) {

                    // remember the old device list for a little bit
                    List<Device> oldDeviceList = new ArrayList<Device>();
                    if (deviceManager._deviceList != null) {
                        oldDeviceList = deviceManager._deviceList;
                    }

                    // replace the old device list with the new list
                    deviceManager._deviceList = newDeviceList;

                    // The device list has changed.  Give these new devices a chance to initialize
                    // before notifying anybody
                    for (Device device : newDeviceList) {
                        device.deviceAdded(getDeviceFromList(oldDeviceList, device));
                    }
                    // Step through the old device list to see if there are any devices on it
                    // that aren't on the new device list and send them a removal notice.
                    for (Device device : oldDeviceList) {
                        if (getDeviceFromList(newDeviceList, device) == null) {
                            device.deviceRemoved();
                        }
                    }

                    deviceManager.notifyDeviceListChanged();

                    // Do we need to enter LAN mode?
                    if ( params.enableLANMode ) {
                        deviceManager.enterLANMode();
                    }
                }
                deviceManager.refreshDeviceStatus(null);
            }

            synchronized (_completionSet) {
                for (int i = 0; i < _completionSet.size(); i++) {
                    _completionSet.get(i).complete(msg, newDeviceList, _tagSet.get(i));
                }
                _completionSet.clear();
                _tagSet.clear();
            }
        }
    }

    private GetDevicesHandler _getDevicesHandler = new GetDevicesHandler(this);

    /** Fetches the list of devices from the server */
    private void fetchDeviceList() {
        AylaDevice.getDevices(_getDevicesHandler);
    }

    private void fetchDeviceListWithCompletion(Object tag, GetDevicesCompletion completion) {
        _getDevicesHandler.addCompletion(tag, completion);
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

        if ( newDeviceList != null ) {
            // Sort the new list of devices
            Collections.sort(newDeviceList, _deviceComparator);
        }
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

    public static class FetchSharesListener {
        public AylaShare ownedShares[];
        public AylaShare receivedShares[];
        public Message lastMessage;
        public boolean fetchOwned;
        public boolean fetchReceived;

        public FetchSharesListener(boolean fetchOwned, boolean fetchReceived) {
            this.fetchOwned = fetchOwned;
            this.fetchReceived = fetchReceived;
        }

        public void sharesFetched(boolean successful){}
    }

    public void fetchShares(FetchSharesListener listener) {
        FetchSharesHandler handler = new FetchSharesHandler(listener);
        if ( listener.fetchOwned ) {
            AylaShare.getOwnsOrReceives(handler, AylaUser.getCurrent(), false, null);
        } else if ( listener.fetchReceived ) {
            AylaShare.getOwnsOrReceives(handler, AylaUser.getCurrent(), true, null);
        } else {
            // Nothing selected!
            Log.e(LOG_TAG, "fetchShares called without wanting any type of share!");
            listener.sharesFetched(false);
        }
    }

    private static class FetchSharesHandler extends Handler {
        private FetchSharesListener _listener;
        private boolean _fetchingOwned;

        public FetchSharesHandler(FetchSharesListener listener) {
            _listener = listener;
            if (listener.fetchOwned) {
                _fetchingOwned = true;
            }
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            _listener.lastMessage = msg;
            if (AylaNetworks.succeeded(msg)) {
                AylaShare shares[] = AylaSystemUtils.gson.fromJson((String) msg.obj, AylaShare[].class);

                if (_fetchingOwned) {
                    _listener.ownedShares = shares;
                    _fetchingOwned = false;
                    if (_listener.fetchReceived) {
                        AylaShare.getReceives(this, AylaUser.getCurrent(), null);
                    } else {
                        // We're done.
                        _listener.sharesFetched(true);
                    }
                } else {
                    _listener.receivedShares = shares;
                    _listener.sharesFetched(true);
                }
            } else {
                _listener.sharesFetched(false);
            }
        }
    };


    /** This is where we are notified when a device's status has been updated. */
    @Override
    public void statusUpdated(Device device, boolean changed) {
        //Log.d(LOG_TAG, "Device status updated (" + changed + "): " + device);
        Log.d(LOG_TAG, "lm: statusUpdated [" + device.getDeviceDsn() + "] changed=" + changed);
        if ( changed ) {
            notifyDeviceStatusChanged(device);
        }

        if ( _devicesToPoll != null ) {
            updateNextDeviceStatus();
        }
    }

    // Notifications
    private void notifyDeviceListChanged() {
        Log.v(LOG_TAG, "dev: device list changed [" + deviceListToString(_deviceList) + "]");
        if (_deviceListListeners.size() > 0) {
            for (DeviceListListener listener : _deviceListListeners) {
                Log.v(LOG_TAG, "dev: notify [" + listener.getClass().getSimpleName() + "]");
                listener.deviceListChanged();
            }
        } else {
            Log.w(LOG_TAG, "dev: device list changed (no listeners)!");
        }
    }

    /**
     * This method may be called whenever the status of a device changes. It notifies all listeners
     * that the device should be updated in the UI.
     * @param device Device that changed
     */
    public void notifyDeviceStatusChanged(Device device) {
        Log.d(LOG_TAG, "dev: device status changed [" + device + "]");
        for (DeviceStatusListener listener : _deviceStatusListeners) {
            listener.statusUpdated(device, true);
        }
    }

    private void notifyLANModeChange() {
        // TODO: Notify whoever cares?
    }
}
