package com.aylanetworks.agilelink;

import android.content.Context;
import android.text.TextUtils;

import com.aylanetworks.aylasdk.AylaPropertyTriggerApp;
import com.baidu.android.pushservice.PushConstants;
import com.baidu.android.pushservice.PushManager;

/**
 * AgileLink Application Framework
 *
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class PushProvider {

    /***
     * Setup Baidu Push service
     * @param context
     */
    public static void start(Context context) {
        PushManager.startWork(context, PushConstants.LOGIN_TYPE_API_KEY, PushUtils.getMetaValue(context, "api_key"));
    }

    public static boolean checkDeviceMatchWithTriggerApp(AylaPropertyTriggerApp triggerApp) {
        return TextUtils.equals(BaiduPushMessageReceiver.getChannelId(),
                triggerApp.getChannelId());
    }
}
