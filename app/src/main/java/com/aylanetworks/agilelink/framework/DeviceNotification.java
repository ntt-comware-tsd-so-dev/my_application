package com.aylanetworks.agilelink.framework;

import com.aylanetworks.aaml.AylaAppNotification;
import com.aylanetworks.aaml.AylaDeviceNotification;
import com.aylanetworks.aaml.AylaUser;

/**
 * Created by Brian King on 2/17/15.
 */
public class DeviceNotification {
    public static final int DEFAULT_NOTIFICATION_THRESHOLD = 300;

    public static final String NOTIFICATION_TYPE_ON_CONNECT = "on_connect";
    public static final String NOTIFICATION_TYPE_IP_CHANGE = "ip_change";
    public static final String NOTIFICATION_TYPE_ON_CONNECTION_LOST = "on_connection_lost";
    public static final String NOTIFICATION_TYPE_ON_CONNECTION_RESTORE = "on_connection_restore";

    public static final String NOTIFICATION_METHOD_EMAIL = "email";
    public static final String NOTIFICATION_METHOD_SMS = "sms";
    public static final String NOTIFICATION_METHOD_PUSH = "push";

    private String _notificationType;
    private int _notificationThreshold;
    private AylaUser _aylaUser;
    private String _message;

    private AylaDeviceNotification _deviceNotification;

    /**
     * Constructor
     * @param notificationType Type of notification
     * @param aylaUser AylaUser object owning this notification
     */
    public DeviceNotification(String notificationType, AylaUser aylaUser) {
        this(notificationType, aylaUser, DEFAULT_NOTIFICATION_THRESHOLD);
    }

    public DeviceNotification(String notificationType, AylaUser aylaUser, int notificationThreshold) {
        _notificationType = notificationType;
        _aylaUser = aylaUser;
        _notificationThreshold = notificationThreshold;
    }

    public AylaAppNotification[] getAppNotifications() {
        if ( _deviceNotification == null ) {
            return null;
        }

        return _deviceNotification.appNotifications;
    }
}
