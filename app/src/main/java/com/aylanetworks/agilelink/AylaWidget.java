
/*
 * AylaWidget.java
 * AgileLink Application Framework
 *
 * Created by Julian Christensen on 06/26/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */
package com.aylanetworks.agilelink;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.android.volley.Response;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.aylasdk.AylaDatapoint;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaDeviceManager;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.aylasdk.AylaSessionManager;
import com.aylanetworks.aylasdk.auth.AylaAuthorization;
import com.aylanetworks.aylasdk.change.Change;
import com.aylanetworks.aylasdk.change.ListChange;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Implementation of App Widget functionality.
 */
public class AylaWidget extends AppWidgetProvider
        implements AylaSessionManager.SessionManagerListener,
        AylaDeviceManager.DeviceManagerListener,
        AylaDevice.DeviceChangeListener,
        AgileLinkApplication.AgileLinkApplicationListener {
    private final static String LOG_TAG = "AylaWidget";

    //Tags for the pending intents
    private final String REFRESH_TAG = "refresh";
    private final String BLUE_LED_TOGGLE_TAG = "toggleblue";
    private final String GREEN_LED_TOGGLE_TAG = "togglegreen";
    private final String BLUE_BUTTON_START_TAG = "bluebuttonstart";
    private final String PLUG_TOGGLE_TAG = "toggleplug";
    private final String GO_UP_DEVICE_TAG = "goupdevice";
    private final String GO_DOWN_DEVICE_TAG = "godowndevice";
    private final String LOGGED_IN_TAG = "loggedin";
    private final String LOGGED_OUT_TAG = "loggedout";

    SharedPreferences settings = AgileLinkApplication.getSharedPreferences();
    final String savedUsername = settings.getString(AMAPCore.PREFS_USERNAME, "");
    final String savedPassword = settings.getString(AMAPCore.PREFS_PASSWORD, "");

    final static int MAX_DEVICES = 8;
    private static int numOfDevices = 0;
   // AylaDatapoint datapoint;
    private static AylaProperty outlet1 = null;
    private static AylaProperty greenLed = null;
    private static AylaProperty blueLed = null;
    private static AylaProperty blueButton = null;
    private RemoteViews remoteViews;
    //AylaDevice device = null;

    //Uses a static context variable in order to change widget Views in handlers
    static Context contextStatic = null;

    static String currentVal = "UP";
    static boolean enabled = false;
    static int deviceNum = 0;

    boolean isListening;
    int _appWidgetId;
    Timer _timer;
    TimerTask _timerTask;

    void timerCancel() {
        if (_timer != null) {
            _timer.cancel();
        }
    }

    void timerSchedule() {
        if (_timer == null) {
            _timer = new Timer();
            _timerTask = new TimerTask() {
                @Override
                public void run() {
                    lanModePause();
                }
            };
        } else {
            _timer.cancel();
        }
        _timer.schedule(_timerTask, 60000);
    }

    void lanModeResume() {
        AgileLinkApplication.LifeCycleState state = AgileLinkApplication.getLifeCycleState();
        Log.d(LOG_TAG, "wig: lanModeResume state=" + state);
        if (AMAPCore.sharedInstance().getDeviceManager() != null) {
            timerSchedule();
        }
        Log.d(LOG_TAG, "wig: lanModeResume ignored");
    }

    void lanModePause() {
        AgileLinkApplication.LifeCycleState state = AgileLinkApplication.getLifeCycleState();
        Log.d(LOG_TAG, "wig: lanModePause state=" + state);
        timerCancel();
        AylaNetworks.sharedInstance().onPause();
        Log.d(LOG_TAG, "wig: lanModePause ignored");
    }

    void startListening() {
        AylaDeviceManager deviceManager = AMAPCore.sharedInstance().getDeviceManager();
        AylaSessionManager sessionManager = AMAPCore.sharedInstance().getSessionManager();
        if (!isListening) {
            isListening = true;

            Log.d(LOG_TAG, "wig: startListening - add session listener");
            sessionManager.addListener(this);
            if (deviceManager != null) {
                Log.d(LOG_TAG, "wig: startListening - add device listener");
                deviceManager.addListener(this);
                for (AylaDevice device : deviceManager.getDevices()) {
                    device.addListener(this);
                }
            } else {
                Log.d(LOG_TAG, "wig: startListening - no device manager");
            }
        } else {
            Log.d(LOG_TAG, "wig: startListening - already listening.");
        }
    }

    void stopListening() {
        AylaDeviceManager deviceManager = AMAPCore.sharedInstance().getDeviceManager();
        AylaSessionManager sessionManager = AMAPCore.sharedInstance().getSessionManager();
        if (isListening) {
            isListening = false;
            Log.d(LOG_TAG, "wig: stopListening - remove session listener.");
            sessionManager.removeListener(this);
            if (deviceManager != null) {
                Log.d(LOG_TAG, "wig: stopListening - remove device listener.");
                deviceManager.removeListener(this);
            } else {
                Log.d(LOG_TAG, "wig: stopListening - no device manager.");
            }
        } else {
            Log.d(LOG_TAG, "wig: stopListening - already stopped.");
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        contextStatic=context;
        ComponentName aylaWidget;
        remoteViews = new RemoteViews(context.getPackageName(), R.layout.ayla_widget);
        aylaWidget = new ComponentName(context, AylaWidget.class);

        remoteViews.setOnClickPendingIntent(R.id.refresh_button, getPendingSelfIntent(context, REFRESH_TAG));
        remoteViews.setOnClickPendingIntent(R.id.plug_toggle, getPendingSelfIntent(context, PLUG_TOGGLE_TAG));
        remoteViews.setOnClickPendingIntent(R.id.blue_led_toggle, getPendingSelfIntent(context, BLUE_LED_TOGGLE_TAG));
        remoteViews.setOnClickPendingIntent(R.id.green_led_toggle, getPendingSelfIntent(context, GREEN_LED_TOGGLE_TAG));
        remoteViews.setOnClickPendingIntent(R.id.blue_button, getPendingSelfIntent(context, BLUE_BUTTON_START_TAG));
        remoteViews.setOnClickPendingIntent(R.id.device_up, getPendingSelfIntent(context, GO_UP_DEVICE_TAG));
        remoteViews.setOnClickPendingIntent(R.id.device_down, getPendingSelfIntent(context, GO_DOWN_DEVICE_TAG));

        appWidgetManager.updateAppWidget(aylaWidget, remoteViews);
    }

    @Override
    public void applicationLifeCycleStateChange(AgileLinkApplication.LifeCycleState state) {
        Log.d(LOG_TAG, "wig: applicationLifeCycleStateChange " + state);
        if (state == AgileLinkApplication.LifeCycleState.ScreenOff) {
            lanModePause();
        }
    }

    void onAppWidgetUpdate(Context context, Intent intent) {
        _appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0);
        Log.d(LOG_TAG, "wig: onAppWidgetUpdate id=" + _appWidgetId);
        AgileLinkApplication.getsInstance().addListener(this);
        startListening();
        updateDeviceInfo(context);
    }

    void updateDeviceInfo(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        remoteViews = new RemoteViews(context.getPackageName(), R.layout.ayla_widget);
        ComponentName aylaWidget = new ComponentName(context, AylaWidget.class);
        getDeviceInfo(context);
        appWidgetManager.updateAppWidget(aylaWidget, remoteViews);
    }

    public void onReceive(Context context, Intent intent) {
        Log.d(LOG_TAG, "wig: onReceive " + intent);
        contextStatic = context;

        super.onReceive(context, intent);

        //Test based on the tags created by the intents to find the button that was pressed
        String actionType = intent.getAction();
        if(actionType != null){
            switch (actionType){
                case AppWidgetManager.ACTION_APPWIDGET_UPDATE:
                    onAppWidgetUpdate(context, intent);
                    break;

                case REFRESH_TAG:
                    updateDeviceInfo(context);
                    break;

                case PLUG_TOGGLE_TAG:
                    if(outlet1 != null){ // ensure that this gets called only after outlet1 has been set in getProperties handler
                        toggleUI(context, outlet1, R.id.plug_toggle);
                    }
                    break;
                case BLUE_LED_TOGGLE_TAG:
                    if(blueLed != null){
                        toggleUI(context, blueLed, R.id.blue_led_toggle);
                    }
                    break;
                case GREEN_LED_TOGGLE_TAG:
                    if(greenLed != null){
                        toggleUI(context, greenLed, R.id.green_led_toggle);
                    }
                    break;
                case GO_UP_DEVICE_TAG:
                    goUpDevice(context);
                    break;
                case GO_DOWN_DEVICE_TAG:
                    goDownDevice(context);
                    break;

                case LOGGED_IN_TAG:
                    onLoggedIn(context, intent);
                    break;

                case LOGGED_OUT_TAG:
                    onLoggedOut(context, intent);
                    break;
            }
        }
    }


    protected PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    @Override
    public void onEnabled(Context context) {
        Log.d(LOG_TAG, "wig: onEnabled");

        // Enter relevant functionality for when the first widget is created

        if ( !TextUtils.isEmpty(savedUsername) && !TextUtils.isEmpty(savedPassword)) {
            signIn(savedUsername, savedPassword);
        }

//        SessionManager.deviceManager().refreshDeviceList();

        contextStatic = context;

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName aylaWidget;
        remoteViews = new RemoteViews(context.getPackageName(), R.layout.ayla_widget);
        aylaWidget = new ComponentName(context, AylaWidget.class);

        AylaSessionManager sessionManager = AMAPCore.sharedInstance().getSessionManager();
        sessionManager.addListener(this);

        AylaDeviceManager deviceManager = AMAPCore.sharedInstance().getDeviceManager();
        if (deviceManager != null) {
            deviceManager.addListener(this);
        }

        List<AylaDevice> deviceList = null;
        if (deviceManager != null) {
            deviceList = deviceManager.getDevices();
            numOfDevices = deviceList.size();

            if (deviceList != null) {
               // device = deviceList.get(deviceNum).getDevice();
                getDeviceInfo(context);
            }
        }

        else
        {
            remoteViews.setTextViewText(R.id.appwidget_text, "Device Not Found");
            remoteViews.setViewVisibility(R.id.linearlayout_evb, View.GONE);
            remoteViews.setViewVisibility(R.id.linearlayout_plug, View.GONE);
            remoteViews.setViewVisibility(R.id.refresh_button, View.VISIBLE);
            remoteViews.setViewVisibility(R.id.device_down, View.GONE);
            remoteViews.setViewVisibility(R.id.device_up, View.GONE);

        }

        appWidgetManager.updateAppWidget(aylaWidget, remoteViews);
    }

    public void getDeviceInfo(Context context) {

        AylaDeviceManager deviceManager = AMAPCore.sharedInstance().getDeviceManager();
        if(deviceManager != null) {
            List<AylaDevice> deviceList = deviceManager.getDevices();
            if ( deviceList != null && !deviceList.isEmpty()) {
                Log.d(LOG_TAG, "wig: getDeviceInfo deviceNum=" + deviceNum);
                AylaDevice device = deviceList.get(deviceNum);
                if (device != null) {
                    if (deviceNum < deviceList.size() - 1) {
                        remoteViews.setViewVisibility(R.id.device_up, View.VISIBLE);
                    } else {
                        remoteViews.setViewVisibility(R.id.device_up, View.GONE);
                    }

                    if (deviceNum > 0) {
                        remoteViews.setViewVisibility(R.id.device_down, View.VISIBLE);
                    } else {
                        remoteViews.setViewVisibility(R.id.device_down, View.GONE);
                    }

                    String[] propertyNames;
                    if (device.getOemModel() != null && device.getOemModel()
                            .toLowerCase().contains("plug"))
                        propertyNames = new String[]{"outlet1", "oem_host_version"};
                    else
                        propertyNames = new String[] {"Green_LED", "Blue_LED", "Blue_button",
                                "oem_host_version"};

                    remoteViews.setViewVisibility(R.id.progress, View.VISIBLE);
                    remoteViews.setViewVisibility(R.id.linearlayout_evb, View.GONE);
                    remoteViews.setViewVisibility(R.id.linearlayout_plug, View.GONE);
                    device.fetchProperties(propertyNames, new Response.Listener<AylaProperty[]>() {
                                @Override
                                public void onResponse(AylaProperty[] response) {
                                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(contextStatic);

                                    RemoteViews remoteViews;
                                    ComponentName aylaWidget;

                                    remoteViews = new RemoteViews(contextStatic.getPackageName(), R.layout.ayla_widget);
                                    aylaWidget = new ComponentName(contextStatic, AylaWidget.class);
                                        //Creation of items for the manipulation of views in the widget
                                    remoteViews.setViewVisibility(R.id.progress, View.GONE);
                                    //Updates the widget UI
                                    appWidgetManager.updateAppWidget(aylaWidget, remoteViews);
                                }
                            },
                            new ErrorListener() {
                                @Override
                                public void onErrorResponse(AylaError error) {

                                }
                            });

                    remoteViews.setTextViewText(R.id.appwidget_text, device.getProductName());
                }
            }
            else
            {
                remoteViews.setTextViewText(R.id.appwidget_text, "Please add devices in AMAP");
                remoteViews.setViewVisibility(R.id.linearlayout_evb, View.GONE);
                remoteViews.setViewVisibility(R.id.linearlayout_plug, View.GONE);
                remoteViews.setViewVisibility(R.id.device_down, View.GONE);
                remoteViews.setViewVisibility(R.id.device_up, View.GONE);
                remoteViews.setViewVisibility(R.id.refresh_button, View.VISIBLE);
            }
        }
        else {
            remoteViews = new RemoteViews(context.getPackageName(), R.layout.ayla_widget);
            remoteViews.setTextViewText(R.id.appwidget_text, "Please login to AMAP");
            remoteViews.setViewVisibility(R.id.linearlayout_evb, View.GONE);
            remoteViews.setViewVisibility(R.id.linearlayout_plug, View.GONE);
            remoteViews.setViewVisibility(R.id.refresh_button, View.VISIBLE);
            remoteViews.setViewVisibility(R.id.up, View.GONE);
            remoteViews.setViewVisibility(R.id.device_down, View.GONE);
            remoteViews.setViewVisibility(R.id.device_up, View.GONE);
        }
    }

    private void updateViews(AylaDevice device) {
        //Goes through each of the properties that the method just received
        for (AylaProperty propertyTemp : device.getProperties()) {

            //Gets the name of the current property in order to test it
            String name = propertyTemp.getName();

            if (name.equals("outlet1"))
            {
                //Sets the static outlet property to the current property
                outlet1 = propertyTemp;

                //Sets the initial values of the buttons
                if ((Integer)propertyTemp.getValue() == 0) {
                    remoteViews.setTextViewText(R.id.plug_toggle, "OFF");
                } else {
                    remoteViews.setTextViewText(R.id.plug_toggle, "ON");
                }

                //If the outlet property is there, there will be no need for the EVB based buttons
                remoteViews.setViewVisibility(R.id.progress, View.GONE);
                remoteViews.setViewVisibility(R.id.linearlayout_evb, View.GONE);
                remoteViews.setViewVisibility(R.id.linearlayout_plug, View.VISIBLE);

            } else if (name.equals("Green_LED")) {
                //Sets the static greenLed property to the current property
                greenLed = propertyTemp;

                //Sets the initial values of the buttons
                if ((int) propertyTemp.getValue() == 0) {
                    remoteViews.setTextViewText(R.id.green_led_toggle, "OFF");
                } else {
                    remoteViews.setTextViewText(R.id.green_led_toggle, "ON");
                }

                //If the greenLed property is there, there will be no need for the plug based button
                remoteViews.setViewVisibility(R.id.progress, View.GONE);
                remoteViews.setViewVisibility(R.id.linearlayout_evb, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.linearlayout_plug, View.GONE);
            }

            else if (name.equals("Blue_LED"))
            {
                //Sets the static blueLed property to the current property
                blueLed = propertyTemp;

                //Sets the initial values of the buttons
                if ((int) propertyTemp.getValue() == 0) {
                    remoteViews.setTextViewText(R.id.blue_led_toggle, "OFF");
                } else {
                    remoteViews.setTextViewText(R.id.blue_led_toggle, "ON");
                }

                //If the blueLed property is there, it is known that it is an EVB, and so the blueButton needs to be initialized

                remoteViews.setViewVisibility(R.id.progress, View.GONE);
                remoteViews.setViewVisibility(R.id.linearlayout_evb, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.linearlayout_plug, View.GONE);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void toggleUI(Context context, AylaProperty property, int viewId){
        lanModeResume();

        //Creation of items for the manipulation of views in the widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        ComponentName aylaWidget;

        AylaProperty<Integer> toggledProperty = (AylaProperty<Integer>)property;

        remoteViews = new RemoteViews(context.getPackageName(), R.layout.ayla_widget);
        aylaWidget = new ComponentName(context, AylaWidget.class);

        List<AylaDevice> deviceList = null;
        AylaDeviceManager deviceManager = AMAPCore.sharedInstance().getDeviceManager();
        //Checks if the current device is not null
        if ( deviceManager != null ) {
            deviceList = deviceManager.getDevices();
            if(!deviceList.isEmpty()){
                final AylaDevice device = deviceList.get(deviceNum);
                Integer newValue;
                if (toggledProperty.getValue() == 0) {
                    newValue = 1;
                    remoteViews.setTextViewText(viewId, "ON");
                } else {
                    newValue = 0;
                    remoteViews.setTextViewText(viewId, "OFF");
                }

                toggledProperty.createDatapoint(newValue, null, new Response.Listener<AylaDatapoint>() {
                            @Override
                            public void onResponse(AylaDatapoint response) {
                                updateViews(device);
                            }
                        },
                        new ErrorListener() {
                            @Override
                            public void onErrorResponse(AylaError error) {

                            }
                        });
            }
        }

        //Update the widget UI
        appWidgetManager.updateAppWidget(aylaWidget, remoteViews);
    }

    public void goUpDevice(Context context) {
        Log.d(LOG_TAG, "wig: goUpDevice");

        //Creation of items for the manipulation of views in the widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        remoteViews = new RemoteViews(context.getPackageName(), R.layout.ayla_widget);
        ComponentName aylaWidget = new ComponentName(context, AylaWidget.class);
        //Checks if the current device number can increase
        Log.d(LOG_TAG, "wig: numOfDevices "+numOfDevices + " deviceNum "+deviceNum);
        if (deviceNum < numOfDevices-1)
        {
            deviceNum ++;
            getDeviceInfo(context);
            //Updates the widget UI
            appWidgetManager.updateAppWidget(aylaWidget, remoteViews);
        }
    }

    public void goDownDevice(Context context) {
        Log.d(LOG_TAG, "wig: goDownDevice");

        //Creation of items for the manipulation of views in the widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        remoteViews = new RemoteViews(context.getPackageName(), R.layout.ayla_widget);
        ComponentName aylaWidget = new ComponentName(context, AylaWidget.class);
        //Checks if the current device number can decrease
        Log.d(LOG_TAG, "wig: numOfDevices "+numOfDevices + " deviceNum "+deviceNum);
        if (deviceNum > 0)
        {
            //Switches device and loads the info for the device
            deviceNum --;
            getDeviceInfo(context);
            //Updates the widget UI
            appWidgetManager.updateAppWidget(aylaWidget, remoteViews);
        }
    }

    // Listener methods

    @Override
    public void onDisabled(Context context) {

    }

    @Override
    public void deviceChanged(AylaDevice device, Change change) {
        Log.d(LOG_TAG, "wig: Device " + device.getProductName() + " changed");
    }

    @Override
    public void deviceError(AylaDevice device, AylaError error) {

    }

    @Override
    public void deviceLanStateChanged(AylaDevice device, boolean lanModeEnabled) {

    }

    @Override
    public void deviceManagerInitComplete(Map<String, AylaError> deviceFailures) {

    }

    @Override
    public void deviceManagerInitFailure(AylaError error, AylaDeviceManager.DeviceManagerState failureState) {

    }

    @Override
    public void deviceListChanged(ListChange change) {
        Log.d(LOG_TAG, "wig: Device list changed");
        List<AylaDevice> deviceList = AMAPCore.sharedInstance().getDeviceManager().getDevices();
        numOfDevices = deviceList.size();
        updateDeviceInfo(contextStatic);
    }

    @Override
    public void deviceManagerError(AylaError error) {

    }

    @Override
    public void deviceManagerStateChanged(AylaDeviceManager.DeviceManagerState oldState, AylaDeviceManager.DeviceManagerState newState) {

    }

    @Override
    public void sessionClosed(String sessionName, AylaError error) {
        Log.d(LOG_TAG, "wig: sessionClosed");
        Intent intent = new Intent(contextStatic, AylaWidget.class);
        intent.setAction(LOGGED_OUT_TAG);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, _appWidgetId);
        contextStatic.sendBroadcast(intent);
    }

    @Override
    public void authorizationRefreshed(String sessionName, AylaAuthorization authorization) {

    }

    public void signIn(String username, String password) {
//        SessionManager.SessionParameters sessionParams = SessionManager.sessionParameters();
//
//        SessionManager.startSession(username, password);
    }

    void onLoggedIn(Context context, Intent intent) {
        Log.d(LOG_TAG, "wig: onLoggedIn");
        stopListening();
        if(contextStatic != null) {
            remoteViews = new RemoteViews(contextStatic.getPackageName(), R.layout.ayla_widget);
            remoteViews.setTextViewText(R.id.appwidget_text, "Loading...");
            //Updates the widget UI
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(contextStatic);
            ComponentName aylaWidget = new ComponentName(contextStatic, AylaWidget.class);
            appWidgetManager.updateAppWidget(aylaWidget, remoteViews);
        }
        startListening();
    }

    void onLoggedOut(Context context, Intent intent) {
        Log.d(LOG_TAG, "wig: onLoggedOut");
        stopListening();
        if(contextStatic != null) {
            remoteViews = new RemoteViews(contextStatic.getPackageName(), R.layout.ayla_widget);
            remoteViews.setTextViewText(R.id.appwidget_text, "Please login to AMAP");
            //Updates the widget UI
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(contextStatic);
            ComponentName aylaWidget = new ComponentName(contextStatic, AylaWidget.class);
            appWidgetManager.updateAppWidget(aylaWidget, remoteViews);
        }
    }
}

