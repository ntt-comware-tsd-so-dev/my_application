package com.aylanetworks.agilelink;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;

import com.aylanetworks.agilelink.framework.Logger;

/*
 * AgileLinkApplication.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 12/30/14.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */
public class AgileLinkApplication extends Application {
    private static Context context;

    public void onCreate(){
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectAll()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }

        super.onCreate();
        AgileLinkApplication.context = getApplicationContext();

        Logger.getInstance().initialize();
    }

    public static Context getAppContext() {
        return AgileLinkApplication.context;
    }
}
