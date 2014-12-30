package com.aylanetworks.agilelink;

import android.app.Application;
import android.content.Context;

/**
 * Created by Brian King on 12/30/14.
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
