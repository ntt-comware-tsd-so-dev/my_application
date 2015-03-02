package com.aylanetworks.agilelink;

import android.app.Application;
import android.content.Context;

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
        super.onCreate();
        AgileLinkApplication.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return AgileLinkApplication.context;
    }
}
