package com.aylanetworks.agilelink.framework;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.aylanetworks.aaml.AylaAppNotification;
import com.aylanetworks.aaml.AylaAppNotificationParameters;
import com.aylanetworks.aaml.AylaDeviceNotification;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Brian King on 2/17/15.
 */
public class DeviceNotificationHelper {
    private static final String LOG_TAG = "DNHelper";

    public static final int DEFAULT_NOTIFICATION_THRESHOLD = 300;

    public static final String NOTIFICATION_TYPE_ON_CONNECT = "on_connect";
    public static final String NOTIFICATION_TYPE_IP_CHANGE = "ip_change";
    public static final String NOTIFICATION_TYPE_ON_CONNECTION_LOST = "on_connection_lost";
    public static final String NOTIFICATION_TYPE_ON_CONNECTION_RESTORE = "on_connection_restore";

    public static final String NOTIFICATION_METHOD_EMAIL = "email";
    public static final String NOTIFICATION_METHOD_SMS = "sms";
    public static final String NOTIFICATION_METHOD_PUSH = "push_android";

    private Device _device;
    private AylaUser _aylaUser;
    private String _customMessage;

    private boolean _deepFetchComplete;

    static public class DeviceNotificationHelperListener {
        /**
         * Called when the helper class has fetched the array of AylaDeviceNotifications for
         * the given device.
         * @param device Device notifications were fetched for. The notifications will be contained
         *               in the Device.getDevice().deviceNotifications array.
         * @param error Error code. AML_ERROR_OK if the fetch was successful, or an error code otherwise
         */
        void deviceNotificationsFetched(Device device, int error){}

        /**
         * Called when the helper class has fetched an array of AylaAppNotifications for a particular
         * AylaDeviceNotification. Generally used internally to this class. However, implementers may
         * choose to override this method to gain more visibility into the steps taken to deal with
         * notifications.
         * @param device Device that owns these notifications
         * @param deviceNotification The AylaDeviceNotification that contains the newly fetched
         *                           AylaAppNotification array.
         * @param error Error code. AML_ERROR_OK if the fetch was successful, or an error code otherwise
         */
        void appNotificationsFetched(Device device, AylaDeviceNotification deviceNotification, int error){}

        /**
         * Called when the helper class has fetched all of the AylaAppNotifications for each AylaDeviceNotification.
         * The returned device will contain the device notifications, each of which will contain
         * an array of app notifications under Device.getDevice.deviceNotifications[].appNotifications[].
         *
         * As the name of the method indicates, when this is called, the device will contain the complete
         * array of device notifications and their own arrays of app notifications.
         * @param device Device that has been updated to inclue all notifications and apps
         * @param error AML_ERROR_OK if successful, or an error code otherwise. The last message
         *              returned from the library will be stored in the lastMessage member variable.
         */
        void deviceStateComplete(Device device, int error){}

        /**
         * Called when the helper class has added or removed a notification and its apps from the
         * Device object. The returned device will contain the updated set of notifications and their
         * apps.
         * @param device Device that was modified
         * @param notification Notification object that was updated (added or removed)
         * @param notificationType email | sms | push
         * @param error AML_ERROR_OK if successful, or an error code otherwise. The last message
         *              returned from the library will be stored in the lastMessage member variable.
         */
        void deviceNotificationUpdated(Device device,
                                       AylaDeviceNotification notification,
                                       String notificationType,
                                       int error){}

        /**
         * Stores the last message received from the server. This can be useful in determining details
         * about an error if one occurred, or if additional information is needed from the message
         * from the server.
         */
        public Message lastMessage;
    }

    /**
     * Constructor
     * @param device Device object to perform operations on
     * @param aylaUser AylaUser object owning this notification
     */
    public DeviceNotificationHelper(Device device, AylaUser aylaUser) {
        _device = device;
        _aylaUser = aylaUser;
    }

    /**
     * Call this method to set the custom message associated with the device notifications.
     * Default is no message (null).
     * @param message Custom message to set with all notifications created by this object
     */
    public void setCustomMessage(String message) {
        _customMessage = message;
    }

    /**
     * Method to set or clear notifications.
     * @param notificationType email | sms | push
     * @param enable True to enable the notification, false to disable the notification
     * @param listener Listener to receive the completion event
     */
    public void enableDeviceNotifications(final String notificationType,
                                          final boolean enable,
                                          final DeviceNotificationHelperListener listener) {
        // First we need to fetch the existing notifications for the device.
        if ( !_deepFetchComplete ) {
            deepFetchNotifications(new DeviceNotificationHelperListener() {
                @Override
                void deviceStateComplete(Device device, int error) {
                    if ( error != AylaNetworks.AML_ERROR_OK ) {
                        Log.e(LOG_TAG, "Failed to deep-fetch notifications for " + _device);
                        // Pass the last message through to the caller's listener
                        listener.lastMessage = this.lastMessage;
                        listener.deviceStateComplete(_device, error);
                    }
                    enableDeviceNotifications(notificationType, enable, listener);
                }
            });
            return;
        }

        // If we got here, we know that the device has all of its notifications and apps set up.
        if ( enable ) {
            addNotification(notificationType, listener);
        } else {
            removeNotification(notificationType, listener);
        }
    }

    /**
     * Fetches all notifications and apps for the device. When this method is finished, the listener's
     * deviceStateComplete method will be called with the results.
     * @param listener Listener to receive the completion event
     */
    public void deepFetchNotifications(final DeviceNotificationHelperListener listener) {

        DeviceNotificationHelperListener internalListener = new DeviceNotificationHelperListener() {
            private int _index = 0;

            @Override
            void deviceNotificationsFetched(Device device, int error) {
                if ( error != AylaNetworks.AML_ERROR_OK ) {
                    // Pass the last message through to the caller's listener
                    listener.lastMessage = this.lastMessage;
                    listener.deviceStateComplete(device, error);
                    return;
                }

                int nNotifications = device.getDevice().deviceNotifications.length;
                if ( nNotifications == 0 ) {
                    // We're done
                    Log.d(LOG_TAG, "Device has no notifications: " + _device);
                    // Pass the last message through to the caller's listener
                    listener.lastMessage = this.lastMessage;
                    _deepFetchComplete = true;
                    listener.deviceStateComplete(_device, 0);
                    return;
                }

                // We have some notifications. Get the apps for each of them.
                // We'll kick it off by setting the current index to 0 and calling
                // getApps on the first device notification. We'll be notified in
                // appNotificationsFetched when we get the app notifications for each device
                // notification.
                Log.d(LOG_TAG, "Device has " + nNotifications + " notifications. " +
                               "Fetching apps for the first device notification");
                _index = 0;
                AylaDeviceNotification n = device.getDevice().deviceNotifications[_index];
                n.getApps(new GetAppsHandler(device, n, this), null);
            }

            @Override
            void appNotificationsFetched(Device device, AylaDeviceNotification deviceNotification, int error) {
                if ( error != AylaNetworks.AML_ERROR_OK ) {
                    // Pass the last message through to the caller's listener
                    listener.lastMessage = this.lastMessage;
                    listener.deviceStateComplete(device, error);
                    return;
                }

                _index++;
                if ( _index >= device.getDevice().deviceNotifications.length ) {
                    // We're done fetching the apps for each notification. That means we're done!
                    _deepFetchComplete = true;
                    // Pass the last message through to the caller's listener
                    listener.lastMessage = this.lastMessage;
                    listener.deviceStateComplete(device, 0);
                } else {
                    Log.d(LOG_TAG, "Fetching apps for device notification " + _index);
                    AylaDeviceNotification n = device.getDevice().deviceNotifications[_index];
                    n.getApps(new GetAppsHandler(device, n, this), null);
                }
            }
        };

        _deepFetchComplete = false;
        // Kick it off by fetching the notifications using our specially-crafted handler above.
        _device.getDevice().getNotifications(new GetNotificationsHandler(_device, internalListener),
                null);
    }

    /**
     * Internal method that creates the notification and apps for the given notification type.
     * This method assumes that the device has already been deep-fetched so that all of the
     * notifications and their apps exist on the AylaDevice object.
     *
     * @param notificationType Type of notification: email | sms | push
     * @param listener Listener to receive the completion event (deviceNotificationUpdated)
     */
    private void addNotification(final String notificationType,
                                 final DeviceNotificationHelperListener listener) {

        // Set up a handler to be called back when we create the notification
        CreateNotificationHandler handler = new CreateNotificationHandler(_device,
                notificationType,
                new DeviceNotificationHelperListener() {
                    @Override
                    void deviceNotificationUpdated(Device device,
                                                   AylaDeviceNotification notification,
                                                   String type,
                                                   int error) {
                        Log.d(LOG_TAG, "deviceNotificationUpdated for " + device);
                        if ( error == AylaNetworks.AML_ERROR_OK ) {
                            // Now we need to create the app for this notification
                            Log.d(LOG_TAG, "Creating notification (" + notificationType + ") on " + device);
                            createApp(notificationType, notification, listener);
                        } else {
                            // Uh oh.
                            listener.deviceNotificationUpdated(_device, notification, notificationType, error);
                        }
                    }
                });

        // First make sure we have all required notifications set up on this device
        for ( String type : _device.getNotificationTypes() ) {
            AylaDeviceNotification foundNotification = null;
            for (AylaDeviceNotification n : _device.getDevice().deviceNotifications) {
                if (n.notificationType.equals(type)) {
                    foundNotification = n;
                    break;
                }
            }

            if ( foundNotification == null ) {
                // We need to create the notification
                Log.d(LOG_TAG, "Notification [" + type + "] not found. Creating...");

                AylaDeviceNotification n = new AylaDeviceNotification();
                n.notificationType = type;
                n.threshold = _device.getDeviceNotificationThresholdForType(type);
                n.message = _customMessage;
                _device.getDevice().createNotification(handler, n);
            } else {
                createApp(notificationType, foundNotification, listener);
            }
        }
    }

    /**
     * Internal method that removes the notification and apps for the given notification type.
     * This method assumes that the device has already been deep-fetched so that all of the
     * notifications and their apps exist on the AylaDevice object.
     *
     * @param notificationType Type of notification: email | sms | push
     * @param listener Listener to receive the completion event
     */
    private void removeNotification(String notificationType,
                                    DeviceNotificationHelperListener listener) {
        // Get all of the apps for the given notification type and store them
        // in a list

        List<AylaAppNotification> appNotifications = new ArrayList<>();
        for ( AylaDeviceNotification dn : _device.getDevice().deviceNotifications ) {
            for ( AylaAppNotification an : dn.appNotifications ) {
                if ( an.appType.equals(notificationType)) {
                    // For push notifications, we need to check the ID, as there could be multiples
                    if ( notificationType.equals(NOTIFICATION_METHOD_PUSH) ) {
                        if ( !an.notificationAppParameters.registrationId.equals(PushNotification.registrationId) ) {
                            continue;
                        }
                    }
                    appNotifications.add(an);
                }
            }
        }

        // We now have a list of all of the app notifications we want to delete.
        RemoveAppHandler removeAppHandler =
                new RemoveAppHandler(_device, notificationType, appNotifications, listener);
        removeAppHandler.removeNextApp();
    }

    /**
     * Internal method that creates the apps on the given notification for the given type.
     *
     * This method assumes that a deep fetch has been performed on the device so that the
     * deviceNotifications array and each appNotifications array is filled out.
     *
     * @param notificationType email | sms | push
     * @param deviceNotification Device notification to host the apps
     * @param listener Listener to receive the completion event
     */
    private void createApp(String notificationType,
                           AylaDeviceNotification deviceNotification,
                           DeviceNotificationHelperListener listener) {
        // First check to see if we already have the notification
        for ( AylaAppNotification app : deviceNotification.appNotifications ) {
            if ( app.appType.equals(notificationType) ) {
                // If this is a push notification, make sure the registration ID matches ours
                if ( notificationType.equals(NOTIFICATION_METHOD_PUSH) ) {
                    if ( !app.notificationAppParameters.registrationId.equals(PushNotification.registrationId) ) {
                        continue;
                    }
                }
                // We already have the app.
                Log.e(LOG_TAG, "createApp: We already have the app. Not creating again!");
                listener.deviceNotificationUpdated(_device, deviceNotification, notificationType, 0);
                return;
            }
        }
        
        AylaAppNotification appNotification = new AylaAppNotification();

        AylaUser currentUser = AylaUser.getCurrent();
        SessionManager.SessionParameters sessionParameters = SessionManager.sessionParameters();
        AylaAppNotificationParameters params = appNotification.notificationAppParameters;

        appNotification.appType = notificationType;

        params.applicationId = sessionParameters.appId;
        params.countryCode = currentUser.phoneCountryCode;
        params.username = currentUser.firstname;
        params.message = _customMessage;
        params.emailSubject = sessionParameters.notificationEmailSubject;
        params.emailTemplateId = sessionParameters.notificationEmailTemplateId;
        params.emailBodyHtml = sessionParameters.notificationEmailBodyHTML;
        params.phoneNumber = currentUser.phone;
        params.pushSound = "default";
        params.registrationId = PushNotification.registrationId;

        deviceNotification.createApp(new CreateAppHandler(_device, deviceNotification, listener), appNotification);
    }

    /**
     * Handler for calls to getNotifications()
     */
    static class GetNotificationsHandler extends Handler {
        Device _device;
        DeviceNotificationHelperListener _listener;

        GetNotificationsHandler(Device device, DeviceNotificationHelperListener listener) {
            _device = device;
            _listener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            _listener.lastMessage = msg;
            Log.d(LOG_TAG, "getNotifications: " + msg);

            if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
                _device.getDevice().deviceNotifications = AylaSystemUtils.gson.fromJson((String)msg.obj,
                        AylaDeviceNotification[].class);
                _listener.deviceNotificationsFetched(_device, 0);
            } else {
                _listener.deviceNotificationsFetched(_device, msg.what);
            }
        }
    }

    /**
     * Handler for calls to getApps()
     */
    static class GetAppsHandler extends Handler {
        Device _device;
        DeviceNotificationHelperListener _listener;
        AylaDeviceNotification _deviceNotification;

        public GetAppsHandler(Device device,
                              AylaDeviceNotification deviceNotification,
                              DeviceNotificationHelperListener listener) {
            _device = device;
            _deviceNotification = deviceNotification;
            _listener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "getApps: " + msg);
            _listener.lastMessage = msg;
            if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
                _deviceNotification.appNotifications = AylaSystemUtils.gson.fromJson((String)msg.obj,
                        AylaAppNotification[].class);

                _listener.appNotificationsFetched(_device, _deviceNotification, 0);
            } else {
                _listener.appNotificationsFetched(_device, _deviceNotification, msg.what);
            }
        }
    }

    /**
     * Handler for calls to createNotification()
     */
    static class CreateNotificationHandler extends Handler {
        Device _device;
        DeviceNotificationHelperListener _listener;
        String _notificationType;

        public CreateNotificationHandler(Device device, String type, DeviceNotificationHelperListener listener) {
            _device = device;
            _notificationType = type;
            _listener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "CreateNotificationHandler: " + msg);
            _listener.lastMessage = msg;
            if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
                AylaDeviceNotification n = AylaSystemUtils.gson.fromJson((String)msg.obj, AylaDeviceNotification.class);

                // Add the notification to the existing array of notifications on the device object
                List<AylaDeviceNotification> notifications = new ArrayList<>(Arrays.asList(_device.getDevice().deviceNotifications));
                notifications.add(n);
                _device.getDevice().deviceNotifications = notifications.toArray(new AylaDeviceNotification[notifications.size()]);

                // And we're done.
                _listener.deviceNotificationUpdated(_device, n, _notificationType, AylaNetworks.AML_ERROR_OK);
            } else {
                Log.e(LOG_TAG, "CreateNotificationHandler failed: " + msg);
                _listener.deviceNotificationUpdated(_device, null, _notificationType, msg.what);
            }
        }
    }

    static class CreateAppHandler extends Handler {
        private Device _device;
        private AylaDeviceNotification _deviceNotification;
        private DeviceNotificationHelperListener _listener;

        public CreateAppHandler(Device device,
                                AylaDeviceNotification notification,
                                DeviceNotificationHelperListener listener) {
            _device = device;
            _deviceNotification = notification;
            _listener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "CreateAppHandler: " + msg);
            _listener.lastMessage = msg;
            if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
                AylaAppNotification appNotification =
                        AylaSystemUtils.gson.fromJson((String)msg.obj, AylaAppNotification.class);

                // Add the app notification to the device notification's array
                List<AylaAppNotification>appNotifications;
                if ( _deviceNotification.appNotifications == null ) {
                    appNotifications = new ArrayList<>();
                } else {
                    appNotifications = new ArrayList<>(Arrays.asList(_deviceNotification.appNotifications));
                }

                appNotifications.add(appNotification);
                _deviceNotification.appNotifications = appNotifications.toArray(new AylaAppNotification[appNotifications.size()]);
            }
            _listener.deviceNotificationUpdated(_device, _deviceNotification, _deviceNotification.notificationType, msg.what);
        }
    }

    private static class RemoveAppHandler extends Handler {
        private Device _device;
        private String _notificationType;
        private List<AylaAppNotification> _appNotifications;
        private DeviceNotificationHelperListener _listener;
        public RemoveAppHandler(Device device,
                                String notificationType,
                                List<AylaAppNotification> appNotifications,
                                DeviceNotificationHelperListener listener) {
            _device = device;
            _notificationType = notificationType;
            _appNotifications = appNotifications;
            _listener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "RemoveAppHandler: " + msg);
            _listener.lastMessage = msg;
            if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
                removeNextApp();
            } else {
                _listener.deviceNotificationUpdated(_device, null, _notificationType, msg.what);
            }
        }

        void removeNextApp() {
            if ( _appNotifications.isEmpty() ) {
                // We're done.
                _listener.deviceNotificationUpdated(_device, null, _notificationType, 0);
                return;
            }

            AylaAppNotification app = _appNotifications.remove(0);
            app.destroy(this);
        }
    }
}
