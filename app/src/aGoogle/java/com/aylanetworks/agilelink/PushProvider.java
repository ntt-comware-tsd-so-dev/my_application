package com.aylanetworks.agilelink;

import android.content.Context;
import android.text.TextUtils;

import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.PushNotification;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaPropertyTriggerApp;

/**
 * AgileLink Application Framework
 *
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class PushProvider {

    static final String LOG_TAG = "PushProvider";

    public static boolean isUsingBaiduPush() {
        return false;
    }

    public static void start() {
        // Set up push notifications
        PushNotification pushNotification = new PushNotification();
        AMAPCore.SessionParameters parameters = AMAPCore.sharedInstance().getSessionParameters();
        pushNotification.init(parameters.pushNotificationSenderId, parameters.username, parameters.appId);
        AylaLog.d(LOG_TAG, "Push notification registration ID: " + PushNotification.registrationId);
    }

    /**
     * check if the registration id matches
     */
    public static boolean checkDeviceMatchWithTriggerApp(AylaPropertyTriggerApp triggerApp) {
        return TextUtils.equals(PushNotification.registrationId, triggerApp.getRegistrationId());
    }
}
