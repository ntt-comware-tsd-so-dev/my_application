package com.aylanetworks.agilelink.framework;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaProperty;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.DeviceDetailFragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Brian King on 12/22/14.
 */
public class Device implements Comparable<Device> {

    public interface DeviceStatusListener {
        void statusUpdated(Device device, boolean changed);
    }

    private static final String LOG_TAG = "Device";

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
                (!getDevice().dsn.equals(other.getDevice().dsn));
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
     * Enables or disables notifications of the given type for this device.
     *
     * @param notificationType Type of notification to enable or disable
     * @param enable True to enable, false to disable
     * @param listener Listener to receive the results of the operation. The following method will be
     *                 called on the listener once the result has been obtained:
     *                 {@link DeviceNotificationHelper.DeviceNotificationHelperListener#deviceNotificationUpdated(Device, com.aylanetworks.aaml.AylaDeviceNotification, String, int)}
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
        switch ( notificationType ) {
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
                if ((myProperty == null) || (!myProperty.value.equals(prop.value))) {
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
    static class GetPropertiesHandler extends Handler {
        private WeakReference<Device> _device;
        private DeviceStatusListener _listener;

        public GetPropertiesHandler(Device device, DeviceStatusListener listener) {
            _device = new WeakReference<Device>(device);
            _listener = listener;
        }

        @Override
        public void handleMessage(Message msg) {

            AylaDevice d = _device.get().getDevice();

            if (msg.what == AylaNetworks.AML_ERROR_OK) {
                // Update our properties
                AylaProperty[] properties = AylaSystemUtils.gson.fromJson((String) msg.obj,
                        AylaProperty[].class);

                Log.v(LOG_TAG, "request: " + _device.get().getPropertyArgumentMap());
                Log.v(LOG_TAG, "Properties for " + d.productName + " [" + _device.get().getClass().getSimpleName() + "]");
                if ( properties.length == 0 ) {
                    Log.e(LOG_TAG, "No properties found!! Message: " + msg);
                }
                for (AylaProperty prop : properties) {
                    Log.v(LOG_TAG, "Prop: " + prop.name + ": " + prop.value);
                }

                // Did something change?
                boolean changed = _device.get().setProperties(properties);
                if (_listener != null) {
                    _listener.statusUpdated(_device.get(), changed);
                }

            } else {
                Log.e(LOG_TAG, "Failed to get properties for " + d.getProductName() + ": error " + msg.what);
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

    public boolean isGateway() {
        return false;
    }

    /**
     * UI Methods
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
        h._currentDevice = this;
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
}
