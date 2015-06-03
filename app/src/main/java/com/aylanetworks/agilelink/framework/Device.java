package com.aylanetworks.agilelink.framework;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;

import com.aylanetworks.aaml.AylaDatapoint;
import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaProperty;
import com.aylanetworks.aaml.AylaSchedule;
import com.aylanetworks.aaml.AylaScheduleAction;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaTimezone;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.DeviceDetailFragment;
import com.aylanetworks.agilelink.fragments.NotificationListFragment;
import com.aylanetworks.agilelink.fragments.PropertyNotificationFragment;
import com.aylanetworks.agilelink.fragments.RemoteFragment;
import com.aylanetworks.agilelink.fragments.ScheduleContainerFragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/*
 * Device.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 12/22/14.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

/**
 * The Device class represents a physical device connected to the Ayla network. This class is
 * designed to be overridden to provide device-specific functionality and user interface
 * components.
 * <p>
 * Each Device class wraps an AylaDevice object, which is passed to the Device constructor. Devices
 * are created via the {@link DeviceCreator#deviceForAylaDevice(com.aylanetworks.aaml.AylaDevice)}
 * method of the {@link DeviceCreator} class, which parses information from the underlying
 * {@link AylaDevice} object to identify the correct Device-derived class to create with the object.
 * <p>
 * When creating an Agile Link-derived application, implementers should create a Device-derived
 * class for each type of hardware device supported by the application. The AylaDevice objects
 * received from the server will be passed to the DeviceCreator's deviceForAylaDevice method in
 * order that the DeviceCreator can create the appropriate Device-derived object to contain it.
 * <p>
 * Derived classes should override the following methods:
 * <ul>
 * <li>{@link #getPropertyNames()}</li>
 * <li>{@link #deviceTypeName()}</li>
 * <li>{@link #getDeviceDrawable(android.content.Context)}</li>
 * <li>{@link #registrationType()}</li>
 * <li>{@link #getItemViewType()}</li>
 * <li>{@link #bindViewHolder(android.support.v7.widget.RecyclerView.ViewHolder)}</li>
 * <li>{@link #getSchedulablePropertyNames()}</li>
 * <li>{@link #friendlyNameForPropertyName(String)}</li>
 * </ul>
 */
public class Device implements Comparable<Device> {

    /**
     * Interface called whenever the status of a device changes.
     */
    public interface DeviceStatusListener {
        /**
         * Method called whenever the status of a device has changed
         *
         * @param device  Device whose status has changed
         * @param changed true if the status changed, or false if an error occurred
         */
        void statusUpdated(Device device, boolean changed);
    }

    private static final String LOG_TAG = "Device";

    private static final String STATUS_ONLINE = "Online";

    /**
     * Default comparator. Sorts alphabetically by DSN.
     *
     * @param another Device to compare to
     * @return the standard comparator result
     */
    @Override
    public int compareTo(Device another) {
        // Base class just compares DSNs.
        return this.getDevice().dsn.compareTo(another.getDevice().dsn);
    }

    /**
     * Compares this device with another device, and returns true if the device has changed.
     * This method is used to determine if devices have changed, and will notify listeners.
     * The base class implementation returns true if the DSN or connection status has changed. Derived
     * classes can override this method to check other properties or attributes to determine that
     * something has changed.
     *
     * @param other Device to compare with
     * @return true if the device has changed compared with other
     */
    public boolean isDeviceChanged(Device other) {
        return (!getDevice().connectionStatus.equals(other.getDevice().connectionStatus)) ||
                (!getDevice().dsn.equals(other.getDevice().dsn) ||
                !toString().equals(other.toString()));
    }

    /**
     * The AylaDevice object wrapped by this class
     */
    private AylaDevice _device;

    /**
     * Returns the underlying AylaDevice object wrapped by this class.
     *
     * @return the AylaDevice object owned by this object
     */
    public AylaDevice getDevice() {
        return _device;
    }

    /**
     * Constructor using the AylaDevice parameter
     * @param aylaDevice AylaDevice object this device represents
     */
    public Device(AylaDevice aylaDevice) {
        _device = aylaDevice;
    }

    /**
     * Private default constructor.
     */
    private Device() {
        // Private constructor. Do not use.
    }

    /**
     * This method is invoked after the device is added to the device list.
     *
     * Override to provide additional initialization that must be completed after the device
     * has been added to the device list.  Do not do any lengthy or server related initialization
     * in the constructor, save all that for this deviceAdded method.
     *
     * Always invoke the super.
     *
     * @param oldDevice The device that this device replaces in the device list.  Null if new.
     */
    public void deviceAdded(Device oldDevice) {
        // copy properties
    }

    /**
     * This method is invoked after devices have been added to the device list and this
     * device is not on the new device list.
     *
     * Always invoke the super.
     *
     */
    public void deviceRemoved() { }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    interface ResetTagCompletionHandler {

        void handle(Message msg, ResetTag tag);
    }

    class ResetTag {
        private final static int FACTORY_RESET_INC = 10 * 1000;
        private final static int FACTORY_RESET_WAIT_TIME = 30 * 1000;
        private final static int FACTORY_RESET_SCAN_TIME = 20 * 1000;

        WeakReference<Handler> _handler;
        Device _device;
        ResetTagCompletionHandler _completion; // not weak, because it is the only thing holding on to it
        long startTicks;

        ResetTag(Handler handler, Device device, ResetTagCompletionHandler completion) {
            _handler = new WeakReference<Handler>(handler);
            _device = device;
            _completion = completion;
            this.startTicks = System.currentTimeMillis();
        }

        void resetDevice(Message msg) {
            Logger.logMessage(LOG_TAG, msg, "fr: ResetTag resetDevice");
            if (AylaNetworks.succeeded(msg)) {
                // check to see if the device is still in the list
                delayedCheckIfGone();
            } else {
                completion(msg);
            }
        }

        void checkIfGone() {
            // let's get the device list now from the server/gateway
            SessionManager.deviceManager().refreshDeviceListWithCompletion(this, new DeviceManager.GetDevicesCompletion() {
                @Override
                public void complete(Message msg, List<Device> list, Object tag) {
                    ResetTag frt = (ResetTag) tag;
                    Device device = frt._device;
                    String dsn = device.getDevice().dsn;
                    boolean found = false;
                    Logger.logMessage(LOG_TAG, msg, "fr: ResetTag got devices");
                    if (AylaNetworks.succeeded(msg)) {
                        for (Device d : list) {
                            if (d.getDevice().dsn.equals(dsn)) {
                                Logger.logInfo(LOG_TAG, "fr: ResetTag device [%s] still in device list", dsn);
                                found = true;
                                break;
                            }
                        }
                    }
                    if (found) {
                        if (System.currentTimeMillis() - startTicks > FACTORY_RESET_WAIT_TIME) {
                            Logger.logVerbose(LOG_TAG, "fr: ResetTag timeout");
                            frt.completion(getTimeoutMessage());
                        } else {
                            frt.delayedCheckIfGone();
                        }
                    } else {
                        Logger.logInfo(LOG_TAG, "fr: ResetTag successful.");
                        frt.completion(getSuccessMessage());
                    }
                }
            });
        }

        Message getSuccessMessage() {
            Message msg = new Message();
            msg.what = AylaNetworks.AML_ERROR_OK;
            msg.arg1 = AylaNetworks.AML_ERROR_ASYNC_OK;
            msg.obj = null;
            return msg;
        }

        Message getTimeoutMessage() {
            Message msg = new Message();
            msg.what = AylaNetworks.AML_ERROR_FAIL;
            msg.arg1 = AylaNetworks.AML_ERROR_TIMEOUT;
            msg.obj = null;
            return msg;
        }

        void delayedCheckIfGone() {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() { checkIfGone(); }
            }, 10000);
        }

        void completion(Message msg) {
            if (_handler.get() != null) {
                _handler.get().handleMessage(msg);
            }
            _completion.handle(msg, this);
        }
    }

    static class ResetHandler extends Handler {
        private ResetTag _tag;

        ResetHandler(ResetTag tag) {  _tag = tag; }

        @Override
        public void handleMessage(Message msg) { _tag.resetDevice(msg); }
    }

    ResetTag _resetTag;

    public void factoryResetDevice(Handler handler) {
        Logger.logInfo(LOG_TAG, "fr: factory reset start");
        _resetTag = new ResetTag(handler, this, new ResetTagCompletionHandler() {
            @Override
            public void handle(Message msg, ResetTag tag) {
                Logger.logInfo(LOG_TAG, "fr: factory reset complete %d:%d", msg.what, msg.arg1);
                _resetTag = null;
            }
        });

        // remove the device from the device manager list...
        // so that we don't try getting properties for it, etc.
        SessionManager.deviceManager().removeDevice(this);

        // factory reset the device
        getDevice().factoryReset(new ResetHandler(_resetTag), null);
    }

    /**
     * Override to provide additional actions that must be performed when unregistering a device.
     * Always invoke the super
     *
     * @param handler
     */
    public void unregisterDevice(Handler handler) {

        Logger.logInfo(LOG_TAG, "fr: unregisterDevice start");
        _resetTag = new ResetTag(handler, this, new ResetTagCompletionHandler() {
            @Override
            public void handle(Message msg, ResetTag tag) {
                Logger.logInfo(LOG_TAG, "fr: unregisterDevice complete %d:%d", msg.what, msg.arg1);
                _resetTag = null;
            }
        });

        // remove the device from the device manager list...
        // so that we don't try getting properties for it, etc.
        SessionManager.deviceManager().removeDevice(this);

        // unregister the device
        getDevice().unregisterDevice(new ResetHandler(_resetTag));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Gets the latest device status from the server and calls listener when done.
     * Derived classes can perform other operations to obtain information about the device state.
     * This method is called whenever the DeviceManager's device status timer is hit, or if in
     * LAN mode, whenever the LAN mode handler receives a message that device properties have changed.
     *
     * @param listener Listener to be notified when the status has been updated.
     */
    public void updateStatus(final DeviceStatusListener listener) {
        Map<String, String> getPropertyArguments = getPropertyArgumentMap();

        // NOTE: The library will crash if the device's properties are null.
        if (getDevice().properties == null) {
            getDevice().properties = new AylaProperty[0];
        }
        getDevice().getProperties(new GetPropertiesHandler(this, listener), getPropertyArguments);
    }

    /**
     * Returns the list of {@link Schedule} objects associated with this device.
     *
     * @return A list of {@link Schedule} objects, or null if none exist or have not been
     * retrieved from the server.
     */
    @Nullable
    public List<Schedule> getSchedules() {
        if (getDevice().schedules == null) {
            // Schedules probably not fetched yet
            return null;
        }

        List<Schedule> schedules = new ArrayList<>(getDevice().schedules.length);
        TimeZone tz = TimeZone.getTimeZone("UTC");
        if (getDevice().timezone.tzId != null) {
            tz = TimeZone.getTimeZone(getDevice().timezone.tzId);
        }

        for (AylaSchedule s : getDevice().schedules) {
            schedules.add(new Schedule(s, tz));
        }

        return schedules;
    }

    /**
     * Returns the schedule for this device with the supplied name, or null if not found
     *
     * @param scheduleName Name of the schedule to retrieve
     * @return The schedule with the supplied name, or null
     */
    @Nullable
    public Schedule getSchedule(String scheduleName) {
        if (_device.schedules == null) {
            return null;
        }

        for (AylaSchedule as : _device.schedules) {
            if (as.name.equals(scheduleName)) {
                return new Schedule(as, getTimeZone());
            }
        }

        return null;
    }

    /**
     * Fetches the schedules for this device from the server. On completion, the listener's
     * statusUpdated method will be called with the changed parameter set to true if the fetch
     * was successful, or false if the schedules could not be fetched.
     *
     * @param listener Listener to be notified when the schedules have been fetched
     */
    public void fetchSchedules(final DeviceStatusListener listener) {
        // First make sure the timezone on the device is up to date
        fetchTimezone(new DeviceStatusListener() {
            @Override
            public void statusUpdated(Device device, boolean changed) {
                if (changed) {
                    _device.getAllSchedules(new FetchSchedulesHandler(Device.this, listener), null);
                } else {
                    // Failed to get the timezone.
                    listener.statusUpdated(Device.this, false);
                }
            }
        });
    }

    /**
     * Returns an array of property names that can be used in schedules. The default implementation
     * returns an empty array, as we know nothing about the schedules of an unknown device.
     *
     * @return An array of property names that can be set in a schedule
     */
    public String[] getSchedulablePropertyNames() {
        return new String[0];
    }

    /**
     * Returns an array of property names that can be used in notifications. The defatlt implementation
     * returns an empty array, as we know nothing about the notifications of an unknown device.
     * @return An array of property names that can be used to set a notification
     */
    public String[] getNotifiablePropertyNames() {
        return new String[0];
    }

    /**
     * Returns a friendly name for a property name. This is used in schedules when presenting
     * a choice to the user regarding which properties to control. For example, a SwitchedDevice
     * overrides this method to return "Switch" when refering to the "outlet1" property used to
     * control the switch.
     *
     * The default behavior is to return the actual property name.
     *
     * @param propertyName Property name to translate into something presentable to the user
     * @return The friendly name for the property
     */
    public String friendlyNameForPropertyName(String propertyName) {
        return propertyName;
    }

    /**
     * Returns an action string for a particular property for when it is set or cleared in a
     * schedule. This string represents the action the user will be performing when setting
     * or clearing a property for a schedule. The default implementation returns
     * "Turn on" if setAction is true, or "Turn off" if setAction is false. The text
     * returned from this method will be presented to the user as options to manipulate this
     * property in response to a schedule event.
     *
     * @param propertyName Name of the property
     * @param setAction    True if the action is to set, false if the action is to clear
     * @return a string indicating the action
     */
    public String scheduledActionNameForProperty(String propertyName, boolean setAction) {
        return MainActivity.getInstance().getString(setAction ? R.string.turn_on : R.string.turn_off);
    }

    /**
     * Returns the property value that should be set in a schedule action. The activate parameter
     * indicates whether the action is being activated (set, turned on, etc.) or deactivated
     * (cleared, turned off, etc.). The default implementation returns "1" to set and "0" to clear
     * an action.
     *
     * @param propertyName Name of the property
     * @param activate     True to return the value for activating a property, false for deactivate
     * @return The value the schedule action should be set to for the given property and activate
     * parameter
     */
    public String propertyValueForScheduleAction(String propertyName, boolean activate) {
        if (activate) {
            return "1";
        } else {
            return "0";
        }
    }

    /**
     * Updates the schedule on the server
     *
     * @param schedule Schedule to update on the server
     * @param listener listener to receive the results of the operation
     */
    public void updateSchedule(Schedule schedule, DeviceStatusListener listener) {
        getDevice().updateSchedule(new UpdateScheduleHandler(this, listener),
                schedule.getSchedule(),
                schedule.getSchedule().scheduleActions);
    }

    private static class UpdateScheduleHandler extends Handler {
        private WeakReference<Device> _device;
        private DeviceStatusListener _listener;

        public UpdateScheduleHandler(Device device, DeviceStatusListener listener) {
            _device = new WeakReference<Device>(device);
            _listener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.logDebug(LOG_TAG, "updateSchedule results: " + msg);

            if (AylaNetworks.succeeded(msg)) {
                _listener.statusUpdated(_device.get(), true);
            } else {
                Logger.logError(LOG_TAG, "updateSchedule failed!");
                _listener.statusUpdated(_device.get(), false);
            }
        }
    }

    /**
     * Enables or disables notifications of the given type for this device.
     *
     * @param notificationType Type of notification to enable or disable
     * @param enable           True to enable, false to disable
     * @param listener         Listener to receive the results of the operation. The following method will be
     *                         called on the listener once the result has been obtained:
     *                         {@link DeviceNotificationHelper.DeviceNotificationHelperListener#deviceNotificationUpdated(Device, com.aylanetworks.aaml.AylaDeviceNotification, String, int)}
     */
    public void enableDeviceNotification(final String notificationType,
                                         final boolean enable,
                                         final DeviceNotificationHelper.DeviceNotificationHelperListener listener) {
        DeviceNotificationHelper helper = new DeviceNotificationHelper(this, AylaUser.getCurrent());
        helper.enableDeviceNotifications(notificationType, enable, listener);
    }

    /**
     * Returns the notification threshold used for creating DeviceNotifications. Derived classes may
     * override this value if desired. 300 seconds (5 minutes) is the default.
     *
     * @param notificationType Type of notification in question
     * @return Notification threshold in seconds for the supplied notificationType.
     */
    public int getDeviceNotificationThresholdForType(String notificationType) {
        // Same value for all types in the base class
        switch (notificationType) {
            case DeviceNotificationHelper.NOTIFICATION_TYPE_IP_CHANGE:
            case DeviceNotificationHelper.NOTIFICATION_TYPE_ON_CONNECT:
            case DeviceNotificationHelper.NOTIFICATION_TYPE_ON_CONNECTION_LOST:
            case DeviceNotificationHelper.NOTIFICATION_TYPE_ON_CONNECTION_RESTORE:
            default:
                return 300;
        }
    }

    private final static String[] _notificationTypes = {
            DeviceNotificationHelper.NOTIFICATION_TYPE_ON_CONNECTION_LOST,
            DeviceNotificationHelper.NOTIFICATION_TYPE_ON_CONNECTION_RESTORE
    };

    /**
     * Returns an array of notification types that should be set when creating a new notification.
     * The DeviceNotificationHelper class calls this method to determine which notification types
     * should be created for this device when email, push or SMS notifications are enabled for
     * the device (device notifications).
     *
     * @return An array of notification types
     */
    public String[] getNotificationTypes() {
        return _notificationTypes;
    }

    /**
     * Called when the framework fetches properties from the service for this device. The method
     * should return true if something has changed, or false if the properties are the same. This
     * allows the framework to notify listeners only if the device's properties are different.
     * <p>
     * The device's property array should also be replaced with newProperties, unless nothing has changed.
     *
     * @param newProperties An array of properties just fetched from the library
     * @return true if the device has changed as a result of these new properites, or false if
     * the device has not changed.
     * <p>
     * Derived classes may override this method to customize which properties are evaluated to
     * determine if a device change event should be sent.
     */
    protected boolean setProperties(AylaProperty[] newProperties) {
        boolean hasChanged = (newProperties.length != getDevice().properties.length);
        if (!hasChanged) {
            for (AylaProperty prop : newProperties) {
                AylaProperty myProperty = getProperty(prop.name());
                if ((myProperty == null) || (!TextUtils.equals(myProperty.value, prop.value))) {
                    hasChanged = true;
                    Logger.logVerbose(LOG_TAG, prop.name + " Changed!");
                    break;
                }
            }
        }

        // Keep the new list of properties
        getDevice().properties = newProperties;
        return hasChanged;
    }

    /**
     * Class used to receive the results of a request to get the device's properties
     */
    protected static class GetPropertiesHandler extends Handler {
        private WeakReference<Device> _device;
        private DeviceStatusListener _listener;

        public GetPropertiesHandler(Device device, DeviceStatusListener listener) {
            _device = new WeakReference<Device>(device);
            _listener = listener;
        }

        @Override
        public void handleMessage(Message msg) {

            if (_device.get() == null) {
                Logger.logError(LOG_TAG, "Device _device.get() is null in GetPropertiesHandler.handleMessage: error " + msg.toString());
                return;
            }

            AylaDevice d = _device.get().getDevice();

            if (d == null) {
                Logger.logError(LOG_TAG, "AylaDevice d is null in GetPropertiesHandler.handleMessage: error " + msg.toString());
                return;
            }

            if (AylaNetworks.succeeded(msg)) {
                // Update our properties
                AylaProperty[] properties = AylaSystemUtils.gson.fromJson((String) msg.obj,
                        AylaProperty[].class);

                Logger.logVerbose(LOG_TAG, "request: " + _device.get().getPropertyArgumentMap());
                Logger.logVerbose(LOG_TAG, "Properties for " + d.productName + " [" + _device.get().getClass().getSimpleName() + "]");
                if (properties.length == 0) {
                    Logger.logError(LOG_TAG, "No properties found!! Message: " + msg);
                }
                for (AylaProperty prop : properties) {
                    Logger.logVerbose(LOG_TAG, "Prop: " + prop.name + ": " + prop.value);
                }

                // At this point, enable ourselves in LAN mode if we were the last device
                // to enter LAN mode. If we try to enter LAN mode before we have fetched our properties,
                // things don't seem to work very well.
                DeviceManager dm = SessionManager.deviceManager();
                if ( dm != null && dm.isLastLanModeDevice(_device.get())) {
                    Logger.logDebug(LOG_TAG, "Entering LAN mode (I was the last LAN mode device): " + _device.get());
                    dm.enterLANMode(new DeviceManager.LANModeListener(_device.get()));
                }

                // Did something change?
                boolean changed = _device.get().setProperties(properties);
                if (_listener != null) {
                    _listener.statusUpdated(_device.get(), changed);
                }

            } else {
                Logger.logError(LOG_TAG, "Failed to get properties for " + d.getProductName() + ": error " + msg.what);
                if (_listener != null) {
                    _listener.statusUpdated(_device.get(), true);
                }
            }
        }
    }

    /**
     * Returns the AylaProperty of the given name, or null if no property was found.
     *
     * @param propertyName Name of the property to return
     * @return AylaProperty of the given name, or null if not found
     */
    @Nullable
    public AylaProperty getProperty(String propertyName) {
        AylaProperty[] properties = getDevice().properties;
        if (properties == null)
            return null;

        for (AylaProperty prop : properties) {
            if (prop.name.equals(propertyName))
                return prop;
        }
        return null;
    }

    /**
     * Returns a boolean value for the AylaProperty of the given name,
     * or false if no property was found.
     *
     * @param propertyName
     * @return Boolean value of property
     */
    public boolean getPropertyBoolean(String propertyName) {
        AylaProperty prop = getProperty(propertyName);
        if (prop == null) {
            return false;
        }
        String value = prop.value;
        if (TextUtils.isEmpty(value)) {
            if (prop.datapoint == null) {
                return false;
            }
            if (prop.datapoint.nValue() == null) {
                return false;
            }
            return (prop.datapoint.nValue().intValue() != 0);
        } else {
            try {
                return (Integer.parseInt(value) != 0);
            } catch (Exception ex) {

            }
        }
        return false;
    }

    /**
     * Listener class used to receive the list of property notifications. When notifications
     * have been fetched, the notificationsFetched method will be called. The device object will
     * have its notifiable properties (as returned from getNotifiablePropertyNames) and their
     * associated triggers filled out.
     *
     * If an error occurred, the succeeded parameter will be set to false.
     *
     * The last message received from the service, whether success or failure, will be set in the
     * _lastMessage field of the listener when it calls notificationsFetched.
     */
    public static abstract class FetchNotificationsListener {
        public Message _lastMessage;
        public abstract void notificationsFetched(Device device, boolean succeeded);
    }

    public void fetchNotifications(FetchNotificationsListener listener) {
        PropertyNotificationHelper helper = new PropertyNotificationHelper(this);
        helper.fetchNotifications(listener);
    }

    public static class SetDatapointListener {
        public void setDatapointComplete(boolean succeeded, AylaDatapoint newDatapoint) {
        }
    }

    /**
     * Sets the specified property to the specified value and calls the listener when complete.
     *
     * @param propertyName   Name of the property to set the datapoint on
     * @param datapointValue Value to set the datapoint to
     * @param listener Listener to receive the results of the operation
     */
    public void setDatapoint(String propertyName, Object datapointValue, final SetDatapointListener listener) {
        final AylaProperty property = getProperty(propertyName);
        if (property == null) {
            Logger.logError(LOG_TAG, "setProperty: Can't find property named " + propertyName);
            if ( listener != null ) {
                listener.setDatapointComplete(false, null);
            }
            return;
        }

        final AylaDatapoint datapoint = new AylaDatapoint();
        if (String.class.isInstance(datapointValue)) {
            datapoint.sValue((String) datapointValue);
        } else if (Number.class.isInstance(datapointValue)) {
            datapoint.nValue((Number) datapointValue);
        } else if (Boolean.class.isInstance(datapointValue)) {
            Boolean b = (Boolean)datapointValue;
            datapoint.nValue(b ? 1 : 0);
        } else {
            Logger.logError(LOG_TAG, "setDatapoint: Unknown value type: " + datapointValue.getClass().toString());
            datapoint.sValue(datapointValue.toString());
        }

        // Put the device into LAN mode before setting the property
        SessionManager.deviceManager().enterLANMode(new DeviceManager.LANModeListener(this) {
            @Override
            public void lanModeResult(boolean isInLANMode) {
                final CreateDatapointHandler handler = new CreateDatapointHandler(Device.this,property,listener);
                property.createDatapoint(handler,datapoint);
            }
        });
    }

    private static class CreateDatapointHandler extends Handler {
        private WeakReference<Device> _device;
        private AylaProperty _property;
        private SetDatapointListener _listener;

        public CreateDatapointHandler(Device device, AylaProperty property, SetDatapointListener listener) {
            _device = new WeakReference<Device>(device);
            _property = property;
            _listener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            if (AylaNetworks.succeeded(msg)) {
                // Set the value of the property
                AylaDatapoint dp = AylaSystemUtils.gson.fromJson((String) msg.obj, AylaDatapoint.class);
                _property.datapoint = dp;
                _property.value = dp.value();
                Logger.logDebug("CDP", "Datapoint " + _property.name + " set to " + _property.value);
                if ( _listener != null ) {
                    _listener.setDatapointComplete(true, dp);
                }
                SessionManager.deviceManager().statusUpdated(_device.get(), true);
            } else {
                Logger.logError(LOG_TAG, "createDatapoint failed: " + msg);
                if ( _listener != null ) {
                    _listener.setDatapointComplete(false, null);
                    SessionManager.deviceManager().statusUpdated(_device.get(), false);
                }
            }
        }
    }

    protected static class FetchSchedulesHandler extends Handler {
        private DeviceStatusListener _listener;
        private WeakReference<Device> _device;

        public FetchSchedulesHandler(Device device, DeviceStatusListener listener) {
            _device = new WeakReference<Device>(device);
            _listener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.logDebug(LOG_TAG, "fetchSchedules: " + msg);

            if (AylaNetworks.succeeded(msg)) {
                AylaSchedule[] schedules =
                        AylaSystemUtils.gson.fromJson((String) msg.obj, AylaSchedule[].class);
                _device.get().getDevice().schedules = schedules;

                List<AylaSchedule> scheduleList = new ArrayList<>();

                // BSK: At some point, the service will be filling out the schedule actions so we
                // don't have to fetch them. Check each schedule for actions and remove from the
                // list if they are present.
                for (AylaSchedule s : schedules) {
                    if (s.scheduleActions == null) {
                        scheduleList.add(s);
                    }
                }

                if (scheduleList.isEmpty()) {
                    // Done! No need to fetch any schedule actions.
                    _listener.statusUpdated(_device.get(), true);
                } else {
                    FetchScheduleActionsHandler handler = new FetchScheduleActionsHandler(_device.get(), scheduleList, _listener);
                    handler.fetchNextAction();
                }
            } else {
                Logger.logError(LOG_TAG, "fetchSchedules failed! " + msg);
                _listener.statusUpdated(_device.get(), false);
            }
        }
    }

    protected static class FetchScheduleActionsHandler extends Handler {
        private Device _device;
        private DeviceStatusListener _listener;
        private List<AylaSchedule> _schedulesToUpdate;
        private AylaSchedule _currentSchedule;

        public FetchScheduleActionsHandler(Device device,
                                           List<AylaSchedule> schedulesToUpdate,
                                           DeviceStatusListener listener) {
            _device = device;
            _schedulesToUpdate = schedulesToUpdate;
            _listener = listener;
        }

        public void fetchNextAction() {
            if (_schedulesToUpdate.isEmpty()) {
                // We're done.
                Logger.logDebug(LOG_TAG, "All schedule actions updated for " + _device);
                _listener.statusUpdated(_device, true);
                return;
            }

            _currentSchedule = _schedulesToUpdate.remove(0);
            _currentSchedule.getAllActions(this, null);
        }

        @Override
        public void handleMessage(Message msg) {
            if (AylaNetworks.succeeded(msg)) {
                _currentSchedule.scheduleActions =
                        AylaSystemUtils.gson.fromJson((String) msg.obj, AylaScheduleAction[].class);
                fetchNextAction();
            } else {
                Logger.logError(LOG_TAG, "Failed to fetch schedule actions for " + _currentSchedule);
                _listener.statusUpdated(_device, false);
            }
        }
    }

    /**
     * Returns true if this device is a gateway device. Derived classes should override this
     * method if the device is a gateway device.
     *
     * @return True if this device is a gateway device, or false otherwise.
     */
    public boolean isGateway() {
        return false;
    }

    /**
     * Returns true if this device is a gateway device node. Derived classes should override this
     * method if the device is a gateway device node.
     *
     * @return True if this device is a gateway device node, or false otherwise.
     */
    public boolean isDeviceNode() {
        return false;
    }

    public boolean isOnline() {
        return STATUS_ONLINE.equals(getDevice().connectionStatus);
    }

    /**
     * UI Methods
     */

    /**
     * Returns an integer representing the item view type for this device. This method is called
     * when displaying a CardView representing this device. The item view type should be different
     * for each type of CardView displayed for a device.
     * <p>
     * The value returned from this method will be passed to the
     * {@link com.aylanetworks.agilelink.framework.DeviceCreator#viewHolderForViewType(android.view.ViewGroup, int)}
     * method of the {@link com.aylanetworks.agilelink.framework.DeviceCreator} object, which uses
     * it to determine the appropriate ViewHolder object to create for the Device.
     * <p>
     * Multiple device types may use the same item view type if the view displayed for these devices
     * are the same. Most devices will have their own unique views displayed.
     * <p>
     * View types should be unique, and are generally defined as static members of the
     * {@link DeviceCreator} class. This keeps them all in the same place and makes it easy to
     * ensure that each identifier is unique.
     *
     * @return An integer representing the type of view for this item.
     */
    public int getItemViewType() {
        return DeviceCreator.ITEM_VIEW_TYPE_GENERIC_DEVICE;
    }

    /**
     * Updates the views in the ViewHolder with information from the Device object.
     * <p>
     * Derived classes should override this method to set up a ViewHolder for display in
     * RecyclerViews.
     *
     * @param holder The view holder for this object
     */
    public void bindViewHolder(RecyclerView.ViewHolder holder) {
        final GenericDeviceViewHolder h = (GenericDeviceViewHolder) holder;
        h._deviceNameTextView.setText(getDevice().getProductName());
        if ( h._deviceStatusTextView != null ) {
            h._deviceStatusTextView.setText(getDeviceState());
        }

        h._spinner.setVisibility(getDevice().properties == null ? View.VISIBLE : View.GONE);

        if ( h._expandedLayout != null ) {
            h._expandedLayout.setVisibility(h.getPosition() == GenericDeviceViewHolder._expandedIndex ? View.VISIBLE : View.GONE);
            // Set up handlers for the buttons in the expanded view
            h._notificationsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.getInstance().pushFragment(NotificationListFragment.newInstance(Device.this));
                }
            });
            h._notificationsButton.setVisibility(getNotifiablePropertyNames().length > 0 ? View.VISIBLE : View.GONE);

            h._scheduleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.getInstance().pushFragment(ScheduleContainerFragment.newInstance(Device.this));
                }
            });
            h._scheduleButton.setVisibility(getSchedulablePropertyNames().length > 0 ? View.VISIBLE : View.GONE);

            h._detailsButton.setColorFilter(MainActivity.getInstance().getResources().getColor(R.color.card_text), PorterDuff.Mode.SRC_ATOP);
            h._detailsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.getInstance().pushFragment(DeviceDetailFragment.newInstance(Device.this));
                }
            });
        }


        // Is this a shared device?
        Resources res = MainActivity.getInstance().getResources();
        int color = isOnline() ? res.getColor(R.color.card_text) : res.getColor(R.color.disabled_text);
        if (!getDevice().amOwner()) {
            // Yes, this device is shared.
            color = res.getColor(R.color.card_shared_text);
        }
        h._deviceNameTextView.setTextColor(color);
        h._currentDevice = this;
    }

    /**
     * Returns the number of elements the device's view should span when displayed in a grid view.
     * The default value is 1. Override this in your device if the device's grid view requires
     * more room.
     *
     * @return The number of columns the device's view should span when displayed in a grid
     */
    public int getGridViewSpan() {
        return 1;
    }

    /**
     * Returns a Drawable representing the device (thumbnail image)
     *
     * @param c Context to access resources
     * @return A Drawable object representing the device
     */
    public Drawable getDeviceDrawable(Context c) {
        return c.getResources().getDrawable(R.drawable.generic_device);
    }

    /**
     * Returns a fragment used to display details about the device. This fragment is pushed onto
     * the back stack when the user selects an item from the device list.
     *
     * @return a fragment showing device details
     */
    public Fragment getDetailsFragment() {
        return DeviceDetailFragment.newInstance(this);
    }

    /**
     * Returns a fragment used to set up a schedule for this device. This fragment is pushed onto
     * the back stack when the user taps the Schedules button from the device details page
     * @return a Fragment used to configure schedules for this device
     */
    public Fragment getScheduleFragment() {
        return ScheduleContainerFragment.newInstance(this);
    }

    /**
     * Returns a fragment used to set up notifications for this device. The default implementation
     * returns a generic PropertyNotificationFragment.
     * @return a Fragment used to configure property notifications for this device
     */
    public Fragment getNotificationsFragment() {
        return PropertyNotificationFragment.newInstance(this);
    }

    /**
     * Returns a fragment used to set up remote switch pairing for this device.
     * @return a Fragment used to configure remote switch pairing for this device.
     */
    public Fragment getRemoteFragment() {
        return RemoteFragment.newInstance(this);
    }

    @Override
    public String toString() {
        return getDevice().getProductName();
    }

    /**
     * Returns a string indicating the device type. This string is used when referring to the actual
     * type of the device, such as "Door Sensor" or "Ayla EVB". It should not change based on any
     * characteristics of the specific device, such as device name or state.
     *
     * @return The name of the type of the device
     */
    public String deviceTypeName() {
        return "Unknown";
    }

    /**
     * Returns the registration type used to register this type of device. Derived classes should
     * override this method to return the appropriate registration type for the device.
     *
     * @return The registration type used to register this device
     */
    public String registrationType() {
        return AylaNetworks.AML_REGISTRATION_TYPE_SAME_LAN;
    }

    /**
     * Override to provide additional initialization that must be completed after registering
     * the device.
     */
    public void postRegistration() { }

    /**
     * Override to provide additional initialization that must be completed after registering
     * the device. notifyDeviceStatusChanged is called when complete.
     *
     * @param gateway The gateway that the device node is linked to.
     */
    public void postRegistrationForGatewayDevice(Gateway gateway) { }

    /**
     * Override to provide the resource string for a dialog box presented to the user after
     * registering the device.
     *
     * @return String resource id.
     */
    public int hasPostRegistrationProcessingResourceId() { return 0; }

    /**
     * Returns a string representing the state of the device (on, off, open, closed, etc.)
     * The default implementation returns nothing.
     *
     * @return A string representing the state of the device
     */
    public String getDeviceState() {
        if ( SessionManager.deviceManager().isLastLanModeDevice(this) ) {
            return MainActivity.getInstance().getString(R.string.lan_mode_enabled);
        }
        if ( isOnline() ) {
            return MainActivity.getInstance().getString(R.string.online);
        } else {
            return MainActivity.getInstance().getString(R.string.offline);
        }
    }

    /**
     * Fetches the timezone for this device and notifies the listener when done. If the fetch was
     * successful, the listener's statusUpdated method will be called with changed set to true.
     * Otherwise statusUpdated will be called with changed set to false.
     *
     * @param listener Listener to be notified when the timezone has been fetched
     */
    public void fetchTimezone(DeviceStatusListener listener) {
        _device.getTimezone(new FetchTimezoneHandler(this, listener));
    }

    /**
     * Sets the time zone for the device. This method also updates any schedules using local time
     * to use the new time zone.
     *
     * @param timeZone Time zone identifier (e.g. "America/Los_Angeles")
     * @param listener Listener to be notified when the timezone has been updated. The changed
     *                 argument to statusUpdated() will be set to true if the time zone was updated,
     *                 or false if it could not be updated.
     */
    public void setTimeZone(String timeZone, DeviceStatusListener listener) {
        String oldTimeZone = _device.timezone.tzId;
        _device.timezone.tzId = timeZone;
        _device.updateTimezone(new SetTimezoneHandler(this, oldTimeZone, listener));
    }

    /**
     * Returns the time zone associated with this device. Schedules are displayed in the time zone
     * of the device rather than the time zone of the phone / tablet running this app.
     *
     * @return The time zone of the device, or null if none found
     */
    public TimeZone getTimeZone() {
        if (_device.timezone.tzId == null) {
            return null;
        }

        return TimeZone.getTimeZone(_device.timezone.tzId);
    }

    /**
     * Returns the observable property name for this device.
     * @return The observable property name of the device, or null if none.
     */
    public String getObservablePropertyName() {
        return null;
    }

    /**
     * Returns the arguments for the call to getProperties(). Derived classes should override this
     * to return the correct array of properties to be fetched in updateStatus().
     *
     * @return The map of properties to fetch from the server
     */
    protected ArrayList<String> getPropertyNames() {
        return new ArrayList<>();
    }

    /**
     * Turns the array of property names returned from getPropertyNames() into a space-
     * separated string. This is the format required by the library for a list of properties to
     * fetch.
     *
     * @return a map of the property arguments that can be directly submitted to a getProperties()
     * request.
     */
    protected Map<String, String> getPropertyArgumentMap() {
        ArrayList<String> myProperties = getPropertyNames();
        if (myProperties.size() == 0) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String name : myProperties) {
            if (!first) {
                sb.append(" ");
            } else {
                first = false;
            }
            sb.append(name);
        }

        Map<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("names", sb.toString());
        return paramMap;
    }

    private static class SetTimezoneHandler extends Handler {
        private WeakReference<Device> _device;
        private String _previousTimeZone;
        private DeviceStatusListener _listener;

        public SetTimezoneHandler(Device device, String previousTimeZone, DeviceStatusListener listener) {
            _device = new WeakReference<Device>(device);
            _previousTimeZone = previousTimeZone;
            _listener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.logDebug(LOG_TAG, "Set timezone response: " + msg);

            MainActivity.getInstance().dismissWaitDialog();
            if (AylaNetworks.succeeded(msg)) {
                // Set the updated timezone on the device object
                _device.get()._device.timezone = AylaSystemUtils.gson.fromJson((String) msg.obj,
                        AylaTimezone.class);

                _listener.statusUpdated(_device.get(), true);
            } else {
                // Uh-oh.
                _listener.statusUpdated(_device.get(), false);
            }
        }
    }

    private static class FetchTimezoneHandler extends Handler {
        private WeakReference<Device> _device;
        private DeviceStatusListener _listener;

        public FetchTimezoneHandler(Device device, DeviceStatusListener listener) {
            _device = new WeakReference<Device>(device);
            _listener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            if (AylaNetworks.succeeded(msg)) {
                _device.get().getDevice().timezone = AylaSystemUtils.gson.fromJson((String) msg.obj, AylaTimezone.class);
                Logger.logDebug(LOG_TAG, "Timezone: " + _device.get().getDevice().timezone);
            }
            _listener.statusUpdated(_device.get(), AylaNetworks.succeeded(msg));
        }
    }
}
