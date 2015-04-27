package com.aylanetworks.agilelink.framework;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.aylanetworks.aaml.AylaDatapoint;
import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaProperty;
import com.aylanetworks.aaml.AylaReachability;
import com.aylanetworks.aaml.AylaSchedule;
import com.aylanetworks.aaml.AylaScheduleAction;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaTimezone;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.DeviceDetailFragment;

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
 * <p/>
 * Each Device class wraps an AylaDevice object, which is passed to the Device constructor. Devices
 * are created via the {@link DeviceCreator#deviceForAylaDevice(com.aylanetworks.aaml.AylaDevice)}
 * method of the {@link DeviceCreator} class, which parses information from the underlying
 * {@link AylaDevice} object to identify the correct Device-derived class to create with the object.
 * <p/>
 * When creating an Agile Link-derived application, implementers should create a Device-derived
 * class for each type of hardware device supported by the application. The AylaDevice objects
 * received from the server will be passed to the DeviceCreator's deviceForAylaDevice method in
 * order that the DeviceCreator can create the appropriate Device-derived object to contain it.
 * <p/>
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
            Log.d(LOG_TAG, "updateSchedule results: " + msg);

            if (AylaNetworks.succeeded(msg)) {
                _listener.statusUpdated(_device.get(), true);
            } else {
                Log.e(LOG_TAG, "updateSchedule failed!");
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

    /**
     * Returns an array of notification types that should be set when creating a new notification.
     * The DeviceNotificationHelper class calls this method to determine which notification types
     * should be created for this device when email, push or SMS notifications are enabled for
     * the device (device notifications).
     *
     * @return An array of notification types
     */

    private final static String[] _notificationTypes = {
            DeviceNotificationHelper.NOTIFICATION_TYPE_ON_CONNECTION_LOST,
            DeviceNotificationHelper.NOTIFICATION_TYPE_ON_CONNECTION_RESTORE
    };

    public String[] getNotificationTypes() {
        return _notificationTypes;
    }

    /**
     * Called when the framework fetches properties from the service for this device. The method
     * should return true if something has changed, or false if the properties are the same. This
     * allows the framework to notify listeners only if the device's properties are different.
     * <p/>
     * The device's property array should also be replaced with newProperties, unless nothing has changed.
     *
     * @param newProperties An array of properties just fetched from the library
     * @return true if the device has changed as a result of these new properites, or false if
     * the device has not changed.
     * <p/>
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
                    Log.v(LOG_TAG, prop.name + " Changed!");
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
                Log.e(LOG_TAG, "Device _device.get() is null in GetPropertiesHandler.handleMessage: error " + msg.toString());
                return;
            }

            AylaDevice d = _device.get().getDevice();

            if (d == null) {
                Log.e(LOG_TAG, "AylaDevice d is null in GetPropertiesHandler.handleMessage: error " + msg.toString());
                return;
            }

            if (AylaNetworks.succeeded(msg)) {
                // Update our properties
                AylaProperty[] properties = AylaSystemUtils.gson.fromJson((String) msg.obj,
                        AylaProperty[].class);

                Log.v(LOG_TAG, "request: " + _device.get().getPropertyArgumentMap());
                Log.v(LOG_TAG, "Properties for " + d.productName + " [" + _device.get().getClass().getSimpleName() + "]");
                if (properties.length == 0) {
                    Log.e(LOG_TAG, "No properties found!! Message: " + msg);
                }
                for (AylaProperty prop : properties) {
                    Log.v(LOG_TAG, "Prop: " + prop.name + ": " + prop.value);
                }

                // At this point, enable ourselves in LAN mode if we were the last device
                // to enter LAN mode. If we try to enter LAN mode before we have fetched our properties,
                // things don't seem to work very well.
                DeviceManager dm = SessionManager.deviceManager();
                if ( dm != null && dm.isLastLanModeDevice(_device.get())) {
                    Log.d(LOG_TAG, "Entering LAN mode (I was the last LAN mode device): " + _device.get());
                    dm.enterLANMode(new DeviceManager.LANModeListener(_device.get()));
                }

                // Did something change?
                boolean changed = _device.get().setProperties(properties);
                if (_listener != null) {
                    _listener.statusUpdated(_device.get(), changed);
                }

            } else {
                Log.e(LOG_TAG, "Failed to get properties for " + d.getProductName() + ": error " + msg.what);
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

    public static class SetDatapointListener {
        public void setDatapointComplete(boolean succeeded, AylaDatapoint newDatapoint) {
        }
    }

    /**
     * Sets the specified property to the specified value and calls the listener when complete.
     *
     * @param propertyName   Name of the property to set the datapoint on
     * @param datapointValue Value to set the datapoint to
     */
    public void setDatapoint(String propertyName, Object datapointValue, final SetDatapointListener listener) {
        final AylaProperty property = getProperty(propertyName);
        if (property == null) {
            Log.e(LOG_TAG, "setProperty: Can't find property named " + propertyName);
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
            Log.e(LOG_TAG, "setDatapoint: Unknown value type: " + datapointValue.getClass().toString());
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
                Log.d("CDP", "Datapoint " + _property.name + " set to " + _property.value);
                if ( _listener != null ) {
                    _listener.setDatapointComplete(true, dp);
                }
                SessionManager.deviceManager().statusUpdated(_device.get(), true);
            } else {
                Log.e(LOG_TAG, "createDatapoint failed: " + msg);
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
            Log.d(LOG_TAG, "fetchSchedules: " + msg);

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
                Log.e(LOG_TAG, "fetchSchedules failed! " + msg);
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
                Log.d(LOG_TAG, "All schedule actions updated for " + _device);
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
                Log.e(LOG_TAG, "Failed to fetch schedule actions for " + _currentSchedule);
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
     * <p/>
     * The value returned from this method will be passed to the
     * {@link com.aylanetworks.agilelink.framework.DeviceCreator#viewHolderForViewType(android.view.ViewGroup, int)}
     * method of the {@link com.aylanetworks.agilelink.framework.DeviceCreator} object, which uses
     * it to determine the appropriate ViewHolder object to create for the Device.
     * <p/>
     * Multiple device types may use the same item view type if the view displayed for these devices
     * are the same. Most devices will have their own unique views displayed.
     *
     * @return An integer representing the type of view for this item.
     */
    public int getItemViewType() {
        return DeviceCreator.ITEM_VIEW_TYPE_GENERIC_DEVICE;
    }

    /**
     * Updates the views in the ViewHolder with information from the Device object.
     * <p/>
     * Derived classes should override this method to set up a ViewHolder for display in
     * RecyclerViews.
     *
     * @param holder The view holder for this object
     */
    public void bindViewHolder(RecyclerView.ViewHolder holder) {
        GenericDeviceViewHolder h = (GenericDeviceViewHolder) holder;
        h._deviceNameTextView.setText(getDevice().getProductName());
        h._deviceStatusTextView.setText(getDeviceState());
        h._spinner.setVisibility(getDevice().properties == null ? View.VISIBLE : View.GONE);

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
     * @param c Context to access resources
     * @return a fragment showing device details
     */
    public Fragment getDetailsFragment(Context c) {
        return new DeviceDetailFragment();
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
     * Returns a string representing the state of the device (on, off, open, closed, etc.)
     * The default implementation returns nothing.
     *
     * @return A string representing the state of the device
     */
    public String getDeviceState() {
        return "";
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
            Log.d(LOG_TAG, "Set timezone response: " + msg);

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
                Log.d(LOG_TAG, "Timezone: " + _device.get().getDevice().timezone);
            }
            _listener.statusUpdated(_device.get(), AylaNetworks.succeeded(msg));
        }
    }
}
