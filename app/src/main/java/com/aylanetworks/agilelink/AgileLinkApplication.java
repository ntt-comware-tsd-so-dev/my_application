package com.aylanetworks.agilelink;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.aylanetworks.agilelink.framework.Logger;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

/*
 * AgileLinkApplication.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 12/30/14.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class AgileLinkApplication extends Application implements ComponentCallbacks2, Application.ActivityLifecycleCallbacks {

    private static final String LOG_TAG = "App";

    /**
     * Life cycle of the application
     */
    public enum LifeCycleState {
        /**
         * Application is running in the foreground
         */
        Foreground,

        /**
         * Application is running in the background.
         */
        Background,

        /**
         * Device has entered idle mode
         */
        ScreenOff,

        /**
         * Device has been powered down.
         */
        PowerOff
    }

    /**
     * Interface used for notifying of application state changes.
     *
     */
    public interface AgileLinkApplicationListener {

        /**
         * Indicate a change in the application life cycle state.
         *
         * @param state LifeCycleState
         */
        public void applicationLifeCycleStateChange(LifeCycleState state);

    }

    private static AgileLinkApplication sInstance;
    private static Context context;
    private ScreenPowerReceiver _receiver;
    private Set<AgileLinkApplicationListener> _listeners;
    LifeCycleState _lifeCycleState;
    ActivityLifeCycleState _activityLifeCycleState;

    public void onCreate() {
        sInstance = this;
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

        // we are in the background until we have an active activity
        _lifeCycleState = LifeCycleState.Background;

        _listeners = new HashSet<>();

        // notification of screen off & power off
        _receiver = new ScreenPowerReceiver(this);

        // Catch screen off event
        IntentFilter screenOffFilter = new IntentFilter();
        screenOffFilter.addAction(Intent.ACTION_SCREEN_OFF);
        context.registerReceiver(_receiver, screenOffFilter);

        // Catch power off event
        IntentFilter powerOffFilter = new IntentFilter();
        powerOffFilter.addAction(Intent.ACTION_SHUTDOWN);
        context.registerReceiver(_receiver, powerOffFilter);

        // notification of activity life cycle
        registerActivityLifecycleCallbacks(this);
    }

    /* This method is for use in emulated process environments. It will never be called on a
       production Android device, where processes are removed by simply killing them; no user code
      (including this callback) is executed when doing so.
     */
    @Override
    public void onTerminate() {
        sInstance = null;
    }

    public static AgileLinkApplication getsInstance() {
        return sInstance;
    }

    /**
     * Obtain the current LifeCycleState of the application
     *
     * @return LifeCycleState
     */
    public static LifeCycleState getLifeCycleState() {
        return sInstance._lifeCycleState;
    }

    public static Context getAppContext() {
        return AgileLinkApplication.context;
    }

    public static SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(AgileLinkApplication.context);
    }

    public void addListener(AgileLinkApplicationListener listener) {
        _listeners.add(listener);
    }

    public void removeListener(AgileLinkApplicationListener listener) {
        _listeners.remove(listener);
    }

    void notifyLifeCycleStateChange(LifeCycleState state) {
        for ( AgileLinkApplicationListener l : _listeners) {
            l.applicationLifeCycleStateChange(state);
        }
    }

    static class ScreenPowerReceiver extends BroadcastReceiver {

        WeakReference<AgileLinkApplication> _app;

        ScreenPowerReceiver(AgileLinkApplication app) {
            _app = new WeakReference<>(app);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            AgileLinkApplication app = _app.get();
            String action = intent.getAction();
            if (TextUtils.equals(action, "android.intent.action.SCREEN_OFF")) {
                Logger.logInfo(LOG_TAG, "app: Screen off");
                if (app != null) {
                    app.onScreenOff(false);
                }
            } else if (TextUtils.equals(action, "android.intent.action.ACTION_SHUTDOWN")) {
                Logger.logInfo(LOG_TAG, "app: Power off");
                if (app != null) {
                    app.onScreenOff(true);
                }
            }
        }
    }

    enum ActivityLifeCycleState {
        Create,
        Start,
        Resume,
        Pause,
        Stop,
        Destroy
    }

    void onScreenOff(boolean power) {
        _lifeCycleState = (power) ? LifeCycleState.PowerOff : LifeCycleState.ScreenOff;
        notifyLifeCycleStateChange(_lifeCycleState);
    }

    void checkForeground() {
        if (_lifeCycleState != LifeCycleState.Foreground) {
            Logger.logInfo(LOG_TAG, "app: Foreground");
            _lifeCycleState = LifeCycleState.Foreground;
            notifyLifeCycleStateChange(_lifeCycleState);
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        _activityLifeCycleState = ActivityLifeCycleState.Create;
    }

    @Override
    public void onActivityStarted(Activity activity) {
        checkForeground();
        _activityLifeCycleState = ActivityLifeCycleState.Start;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        checkForeground();
        _activityLifeCycleState = ActivityLifeCycleState.Resume;
    }

    @Override
    public void onActivityPaused(Activity activity) {
        _activityLifeCycleState = ActivityLifeCycleState.Pause;
    }

    @Override
    public void onActivityStopped(Activity activity) {
        _activityLifeCycleState = ActivityLifeCycleState.Stop;
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        _activityLifeCycleState = ActivityLifeCycleState.Destroy;
        _lifeCycleState = LifeCycleState.Foreground;
    }

    @Override
    public void onTrimMemory(int level) {
        if (_activityLifeCycleState == ActivityLifeCycleState.Stop) {
            Logger.logInfo(LOG_TAG, "app: Background");
            _lifeCycleState = LifeCycleState.Background;
            notifyLifeCycleStateChange(_lifeCycleState);
        }
        super.onTrimMemory(level);
    }
}
