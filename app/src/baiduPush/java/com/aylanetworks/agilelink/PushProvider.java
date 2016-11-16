package com.aylanetworks.agilelink;

import android.content.Context;
import android.text.TextUtils;

import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaPropertyTriggerApp;
import com.baidu.android.pushservice.PushConstants;
import com.baidu.android.pushservice.PushManager;

/**
 * AgileLink Application Framework
 *
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class PushProvider {

    public static boolean isUsingBaiduPush() {
        return true;
    }

    /***
     * Setup Baidu Push service
     */
    public static void start() {
        Context context = AylaNetworks.sharedInstance().getContext();
        PushManager.startWork(context, PushConstants.LOGIN_TYPE_API_KEY, PushUtils.getMetaValue(context, "api_key"));
    }

    public static boolean checkDeviceMatchWithTriggerApp(AylaPropertyTriggerApp triggerApp) {
        return TextUtils.equals(BaiduPushMessageReceiver.getChannelId(), triggerApp.getChannelId());
    }
}
