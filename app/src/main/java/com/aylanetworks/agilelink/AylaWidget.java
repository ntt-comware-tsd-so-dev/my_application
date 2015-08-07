
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
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.aylanetworks.aaml.AylaDatapoint;
import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaLanMode;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaProperty;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.AgileLinkApplication;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.DeviceManager;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Implementation of App Widget functionality.
 */
public class AylaWidget extends AppWidgetProvider implements SessionManager.SessionListener,
        DeviceManager.DeviceListListener, Device.DeviceStatusListener
{
    //Tags for the pending intents
    private final String REFRESH_TAG = "refresh";
    private final String BLUE_LED_TOGGLE_TAG = "toggleblue";
    private final String GREEN_LED_TOGGLE_TAG = "togglegreen";
    private final String BLUE_BUTTON_START_TAG = "bluebuttonstart";
    private final String PLUG_TOGGLE_TAG = "toggleplug";
    private final String GO_UP_DEVICE_TAG = "goupdevice";
    private final String GO_DOWN_DEVICE_TAG = "godowndevice";

    SharedPreferences settings = AgileLinkApplication.getSharedPreferences();
    final String savedUsername = settings.getString(SessionManager.PREFS_USERNAME, "");
    final String savedPassword = settings.getString(SessionManager.PREFS_PASSWORD, "");

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

    //Get info from the signIn class
//    static int currentDeviceNum = signIn.pageNo;
//    final int MAX_DEVICES = signIn.maxNumDevices;

    final Handler handler = new Handler();
    Timer timer = new Timer();
    TimerTask doAsynchronousTask;



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

    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        super.onReceive(context, intent);

        AylaLanMode.resume();   //Resume the Lan Mode when an action is performed
        timer.cancel();         //Cancel any previous timed Lan Mode pausing

        //Test based on the tags created by the intents to find the button that was pressed
        String actionType = intent.getAction();
        if(actionType != null){
            switch (actionType){
                case AppWidgetManager.ACTION_APPWIDGET_UPDATE:
                case REFRESH_TAG:
                    getDeviceInfo(context);
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


            }
        }

        //Created a timed action that will pause the Lan Mode after 60 seconds
        final Handler handler = new Handler();
        timer = new Timer();
        doAsynchronousTask = new TimerTask() {

            @Override
            public void run() {

                handler.post(new Runnable() {
                    public void run() {
                        AylaLanMode.pause(false);

                    }
                });

            }

        };
        timer.schedule(doAsynchronousTask, 60000);
    }


    protected PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created

        AylaLanMode.resume();

        if ( !TextUtils.isEmpty(savedUsername) && !TextUtils.isEmpty(savedPassword)) {
            signIn(savedUsername, savedPassword);
        }

//        SessionManager.deviceManager().refreshDeviceList();

        contextStatic = context;

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName aylaWidget;
        remoteViews = new RemoteViews(context.getPackageName(), R.layout.ayla_widget);
        aylaWidget = new ComponentName(context, AylaWidget.class);

        SessionManager.addSessionListener(this);

        DeviceManager deviceManager = SessionManager.deviceManager();
        if (deviceManager != null) {
            SessionManager.deviceManager().addDeviceListListener(this);
            SessionManager.deviceManager().addDeviceStatusListener(this);
        }

        List<Device> deviceList = null;
        if ( SessionManager.deviceManager() != null ) {
            deviceList = SessionManager.deviceManager().deviceList();
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

        if(SessionManager.deviceManager() != null){
            //Creation of items for the manipulation of views in the widget
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName aylaWidget;
            remoteViews = new RemoteViews(context.getPackageName(), R.layout.ayla_widget);
            aylaWidget = new ComponentName(context, AylaWidget.class);

            SessionManager.deviceManager().refreshDeviceList();

            List<Device> deviceList = null;
            if ( SessionManager.deviceManager() != null ) {

                deviceList = SessionManager.deviceManager().deviceList();

                AylaDevice device = deviceList.get(deviceNum).getDevice();


                if (deviceNum < deviceList.size() - 1)
                    remoteViews.setViewVisibility(R.id.device_up, View.VISIBLE);
                else
                    remoteViews.setViewVisibility(R.id.device_up, View.GONE);

                if (deviceNum > 0)
                    remoteViews.setViewVisibility(R.id.device_down, View.VISIBLE);
                else
                    remoteViews.setViewVisibility(R.id.device_down, View.GONE);


                Map<String, String> callParams = new HashMap<String, String>();
                if (device.oemModel != null && device.oemModel.toLowerCase().contains("plug"))
                    callParams.put("names", "outlet1 oem_host_version");
                else
                    callParams.put("names", "Green_LED Blue_LED Blue_button oem_host_version");

                remoteViews.setViewVisibility(R.id.progress, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.linearlayout_evb, View.GONE);
                remoteViews.setViewVisibility(R.id.linearlayout_plug, View.GONE);
                device.getProperties(new PropertiesHandler(device), callParams);

                remoteViews.setTextViewText(R.id.appwidget_text, device.getProductName());
            }

            else
            {
                remoteViews.setTextViewText(R.id.appwidget_text, "Please login to AMAP");
                remoteViews.setViewVisibility(R.id.linearlayout_evb, View.GONE);
                remoteViews.setViewVisibility(R.id.linearlayout_plug, View.GONE);
                remoteViews.setViewVisibility(R.id.device_down, View.GONE);
                remoteViews.setViewVisibility(R.id.device_up, View.GONE);
                remoteViews.setViewVisibility(R.id.refresh_button, View.VISIBLE);
            }



            appWidgetManager.updateAppWidget(aylaWidget, remoteViews);
        }
        else{
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


    public void toggleUI(Context context, AylaProperty property, int viewId){
        //Creation of items for the manipulation of views in the widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        ComponentName aylaWidget;

        AylaProperty toggledProperty = property;
        AylaDatapoint datapoint;

        remoteViews = new RemoteViews(context.getPackageName(), R.layout.ayla_widget);
        aylaWidget = new ComponentName(context, AylaWidget.class);

        List<Device> deviceList = null;
        //Checks if the current device is not null
        if ( SessionManager.deviceManager() != null ) {

            deviceList = SessionManager.deviceManager().deviceList();
            if(!deviceList.isEmpty()){
                AylaDevice device = deviceList.get(deviceNum).getDevice();

                if (toggledProperty.value.equals("0"))
                {
                    toggledProperty.value = "1";

                    datapoint = new AylaDatapoint();
                    datapoint.nValue(1);
                    datapoint.createDatapoint(createDatapoint, property); // next test: no delay

                    remoteViews.setTextViewText(viewId, "ON");
                }else if (toggledProperty.value.equals("1"))
                {
                    toggledProperty.value = "0";

                    datapoint = new AylaDatapoint();
                    datapoint.nValue(0);
                    datapoint.createDatapoint(createDatapoint, property); // next test: no delay

                    remoteViews.setTextViewText(viewId, "OFF");
                }
            }


        }

        //Update the widget UI
        appWidgetManager.updateAppWidget(aylaWidget, remoteViews);

    }


    public void goUpDevice(Context context)
    {

        //Creation of items for the manipulation of views in the widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        ComponentName aylaWidget;

        remoteViews = new RemoteViews(context.getPackageName(), R.layout.ayla_widget);
        aylaWidget = new ComponentName(context, AylaWidget.class);

        //Checks if the current device number can increase
        Log.d("WIDGET", "numOfDevices "+numOfDevices + " deviceNum "+deviceNum);

        if (deviceNum < numOfDevices-1)
        {
            deviceNum ++;
            getDeviceInfo(context);

            //Updates the widget UI
            appWidgetManager.updateAppWidget(aylaWidget, remoteViews);

        }
    }

    public void goDownDevice(Context context)
    {

        //Creation of items for the manipulation of views in the widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        RemoteViews remoteViews;
        ComponentName aylaWidget;

        remoteViews = new RemoteViews(context.getPackageName(), R.layout.ayla_widget);
        aylaWidget = new ComponentName(context, AylaWidget.class);

        //Checks if the current device number can decrease
        if (deviceNum > 0)
        {
            //Switches device and loads the info for the device
            deviceNum --;
            getDeviceInfo(context);

            //Updates the widget UI
            appWidgetManager.updateAppWidget(aylaWidget, remoteViews);

        }
    }

    @Override
    public void onDisabled(Context context) {

    }

    private final Handler createDatapoint = new Handler() {

        public void handleMessage(Message msg) {
            //progress("stop");
            String jsonResults = (String)msg.obj;
            if (msg.what == AylaNetworks.AML_ERROR_OK) {
               // property.datapoint = AylaSystemUtils.gson.fromJson(jsonResults,AylaDatapoint.class);
            }
        }
    };


    private static class PropertiesHandler extends Handler{
        AylaDevice device;


        PropertiesHandler(AylaDevice device){
            this.device = device;
        }
        public void handleMessage(Message msg) {

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(contextStatic);

            RemoteViews remoteViews;
            ComponentName aylaWidget;

            remoteViews = new RemoteViews(contextStatic.getPackageName(), R.layout.ayla_widget);
            aylaWidget = new ComponentName(contextStatic, AylaWidget.class);
            if(AylaNetworks.succeeded(msg)){
                //Creation of items for the manipulation of views in the widget



                //Sets the device's properties to the results of the getProperties call
                String jsonResults = (String) msg.obj;
                device.properties = AylaSystemUtils.gson.fromJson(jsonResults,AylaProperty[].class);

                //Goes through each of the properties that the method just received
                for (AylaProperty propertyTemp : device.properties) {

                    //Gets the name of the current property in order to test it
                    String name = propertyTemp.name();

                    if (name.equals("outlet1"))
                    {
                        //Sets the static outlet property to the current property
                        outlet1 = propertyTemp;

                        //Sets the initial values of the buttons
                        if (propertyTemp.value.equals("0"))
                            remoteViews.setTextViewText(R.id.plug_toggle, "OFF");

                        else if (propertyTemp.value.equals("1"))
                            remoteViews.setTextViewText(R.id.plug_toggle, "ON");

                        else
                            propertyTemp.value = "0";

                        //If the outlet property is there, there will be no need for the EVB based buttons
                        remoteViews.setViewVisibility(R.id.progress, View.GONE);
                        remoteViews.setViewVisibility(R.id.linearlayout_evb, View.GONE);
                        remoteViews.setViewVisibility(R.id.linearlayout_plug, View.VISIBLE);

                    }

                    else if (name.equals("Green_LED"))
                    {
                        //Sets the static greenLed property to the current property
                        greenLed = propertyTemp;

                        //Sets the initial values of the buttons
                        if (propertyTemp.value.equals("0"))
                            remoteViews.setTextViewText(R.id.green_led_toggle, "OFF");
                        else if (propertyTemp.value.equals("1"))
                            remoteViews.setTextViewText(R.id.green_led_toggle, "ON");

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
                        if (propertyTemp.value.equals("0"))
                            remoteViews.setTextViewText(R.id.blue_led_toggle, "OFF");
                        else if (propertyTemp.value.equals("1"))
                            remoteViews.setTextViewText(R.id.blue_led_toggle, "ON");

                        //If the blueLed property is there, it is known that it is an EVB, and so the blueButton needs to be initialized

                        remoteViews.setViewVisibility(R.id.progress, View.GONE);
                        remoteViews.setViewVisibility(R.id.linearlayout_evb, View.VISIBLE);
                        remoteViews.setViewVisibility(R.id.linearlayout_plug, View.GONE);
                   /* if (!enabled) {
                        AylaLanMode.enable(widgetNotifierHandler, widgetReachabilityHandler);
                        enabled = true;
                        remoteViews.setTextViewText(R.id.blue_button, currentVal);
                    }*/
                    }
                }


            }
            remoteViews.setViewVisibility(R.id.progress, View.GONE);
            //Updates the widget UI
            appWidgetManager.updateAppWidget(aylaWidget, remoteViews);

        }

    }

    static Handler widgetNotifierHandler = new Handler() {

        public void handleMessage(Message msg) {

            //Creation of items for the manipulation of views in the widget (with the static context)
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(contextStatic);

            RemoteViews remoteViews;
            ComponentName aylaWidget;

            remoteViews = new RemoteViews(contextStatic.getPackageName(), R.layout.ayla_widget);
            aylaWidget = new ComponentName(contextStatic, AylaWidget.class);


            //Updates the current value of the button
            if (currentVal.equals("UP"))
            {
                currentVal = "DOWN";
            }
            else
            {
                currentVal = "UP";
            }

            //Changes the value of the button text
            remoteViews.setTextViewText(R.id.blue_button, currentVal);

            //Updates the widget UI
            appWidgetManager.updateAppWidget(aylaWidget, remoteViews);
        }
    };

    //Unnecessary Handler which is passed when the Lan Mode is enabled for the EVB
    final static Handler widgetReachabilityHandler = new Handler() {
        public void handleMessage(Message msg) {

        }
    };

    public void signIn(String username, String password) {
//        SessionManager.SessionParameters sessionParams = SessionManager.sessionParameters();
//
//        SessionManager.startSession(username, password);
    }

    @Override
    public void deviceListChanged() {
        Log.e("Ayla Widget", "Device list changed");
        //updateDeviceList();
    }

    @Override
    public void statusUpdated(Device device, boolean changed) {
        if ( changed ) {
           Log.e("Ayla Widget", "Device " + device.getDevice().productName + " changed");
           // _recyclerView.setAdapter(_adapter);
        }
    }

    @Override
    public void loginStateChanged(boolean loggedIn, AylaUser aylaUser) {
        Log.d("WIDGET", "login changed");
        if(!loggedIn){
            if(contextStatic != null){
                remoteViews = new RemoteViews(contextStatic.getPackageName(), R.layout.ayla_widget);
                remoteViews.setTextViewText(R.id.appwidget_text, "Please login to AMAP");
                //Updates the widget UI
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(contextStatic);
                ComponentName aylaWidget = new ComponentName(contextStatic, AylaWidget.class);
                appWidgetManager.updateAppWidget(aylaWidget, remoteViews);
            }
            SessionManager.deviceManager().removeDeviceListListener(this);
            SessionManager.deviceManager().removeDeviceStatusListener(this);


        }
    }

    @Override
    public void reachabilityChanged(int reachabilityState) {
        Log.e("Ayla Widget", "Reachability changed: " + reachabilityState);
    }

    @Override
    public void lanModeChanged(boolean lanModeEnabled) {
        Log.e("Ayla Widget", "lanModeChanged: " + (lanModeEnabled ? "ENABLED" : "DISABLED"));
    }
}

