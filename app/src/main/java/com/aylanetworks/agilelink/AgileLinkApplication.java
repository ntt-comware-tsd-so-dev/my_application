package com.aylanetworks.agilelink;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.StrictMode;
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

    private static Context context;
    private ScreenPowerReceiver _receiver;
    private Set<AgileLinkApplicationListener> _listeners;

    public void onCreate(){
        _background = true;
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

        _receiver = new ScreenPowerReceiver(this);

        // Catch screen off event
        IntentFilter screenOffFilter = new IntentFilter();
        screenOffFilter.addAction(Intent.ACTION_SCREEN_OFF);
        context.registerReceiver(_receiver, screenOffFilter);

        // Catch power off event
        IntentFilter powerOffFilter = new IntentFilter();
        powerOffFilter.addAction(Intent.ACTION_SHUTDOWN);
        context.registerReceiver(_receiver, powerOffFilter);

        _listeners = new HashSet<>();
    }

    /**
     * Interface used for notifying of application state changes.
     *
     */
    public interface AgileLinkApplicationListener {

        /**
         * Notify of the power down or screen off.
         *
         * @param power true indicates that the power has been shut off,
         *              false indicates that just the screen has been shut off.
         */
        public void onScreenOff(boolean power);

        /**
         * Application has entered the background.
         */
        public void onApplicationBackground();

        /**
         * Application has entered the foreground
         */
        public void onApplicationForeground();
    }

    public void addListener(AgileLinkApplicationListener listener) {
        _listeners.add(listener);
    }

    public void removeListener(AgileLinkApplicationListener listener) {
        _listeners.remove(listener);
    }

    void notifyScreenOff(boolean power) {
        for ( AgileLinkApplicationListener l : _listeners) {
            l.onScreenOff(power);
        }
    }

    void notifyBackground() {
        for ( AgileLinkApplicationListener l : _listeners) {
            l.onApplicationBackground();
        }
    }

    void notifyForeground() {
        for ( AgileLinkApplicationListener l : _listeners ) {
            l.onApplicationForeground();
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
            //Logger.logVerbose(LOG_TAG, "ScreenPowerReceiver onReceive [%s]", action);
            if (TextUtils.equals(action, "android.intent.action.SCREEN_OFF")) {
                Logger.logInfo(LOG_TAG, "app: Screen off");
                if (app != null) {
                    app.notifyScreenOff(false);
                }
            } else if (TextUtils.equals(action, "android.intent.action.ACTION_SHUTDOWN")) {
                Logger.logInfo(LOG_TAG, "app: Power off");
                if (app != null) {
                    app.notifyScreenOff(true);
                }
            }
        }
    }

    public static Context getAppContext() {
        return AgileLinkApplication.context;
    }

    enum LifeCycleState {
        Create,
        Start,
        Resume,
        Pause,
        Stop,
        Destroy
    }

    LifeCycleState _lifeCycleState;
    boolean _background;

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        _lifeCycleState = LifeCycleState.Create;
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (_background) {
            _background = false;
            Logger.logInfo(LOG_TAG, "app: Foreground");
            notifyForeground();
        }
        _lifeCycleState = LifeCycleState.Start;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        _lifeCycleState = LifeCycleState.Resume;
    }

    @Override
    public void onActivityPaused(Activity activity) {
        _lifeCycleState = LifeCycleState.Pause;
    }

    @Override
    public void onActivityStopped(Activity activity) {
        _lifeCycleState = LifeCycleState.Stop;
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        _lifeCycleState = LifeCycleState.Destroy;
        _background = false;
    }

    @Override
    public void onTrimMemory(int level) {
        if (_lifeCycleState == LifeCycleState.Stop) {
            _background = true;
            Logger.logInfo(LOG_TAG, "app: Background");
            notifyBackground();
        }
        super.onTrimMemory(level);
    }
}
