package com.aylanetworks.agilelink.geofence;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.geofence.ALAction;
import com.aylanetworks.agilelink.framework.geofence.ALAutomation;
import com.aylanetworks.agilelink.framework.geofence.ALAutomationManager;
import com.aylanetworks.aylasdk.AylaDatapoint;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.util.TypeUtils;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

/*
 * AMAP_Android
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */

public class AMAPGeofenceService extends IntentService {
    private final String TAG = AMAPGeofenceService.class.getName();

    public AMAPGeofenceService() {
        super("AMAPGeofenceService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        if (event != null) {
            if (event.hasError()) {
                onError(event.getErrorCode());
            } else {
                int transition = event.getGeofenceTransition();
                if (transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                    if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                        onEnteredExitedGeofences(true);
                    } else {
                        onEnteredExitedGeofences(false);
                    }
                }
            }
        }
    }

    private void onEnteredExitedGeofences(final boolean entered) {
        ALAutomationManager.fetchAutomation(new Response.Listener<ALAutomation[]>() {
            @Override
            public void onResponse(ALAutomation[] response) {
                ALAutomation.ALAutomationTriggerType triggerType = ALAutomation.ALAutomationTriggerType.TriggerTypeGeofenceEnter;
                if (!entered) {
                    triggerType = ALAutomation.ALAutomationTriggerType.TriggerTypeGeofenceExit;
                }
                for (ALAutomation alAutomation : response) {
                    if (alAutomation.getAutomationTriggerType().equals(triggerType)) {
                        ALAction[] alActions = alAutomation.getALActions();
                        for (final ALAction alAction : alActions) {

                            AylaDevice device = AMAPCore.sharedInstance().getDeviceManager()
                                    .deviceWithDSN(alAction.getDSN());
                            final AylaProperty entryProperty = device.getProperty(alAction.getAylaProperty().getName());
                          // final AylaProperty entryProperty = alAction.getAylaProperty();

                            Object value = TypeUtils.getTypeConvertedValue(entryProperty.getBaseType(), alAction.getValue());
                            entryProperty.createDatapoint(value, null, new Response
                                    .Listener<AylaDatapoint<Integer>>() {
                                        @Override
                                        public void onResponse(final AylaDatapoint<Integer> response) {
                                            String str = "Property Name:" +entryProperty.getName();
                                            str += " value " +alAction.getValue();
                                            Log.d(TAG, "OnEnteredExitedGeofences success: " + str);
                                        }
                                    },
                                    new ErrorListener() {
                                        @Override
                                        public void onErrorResponse(AylaError error) {
                                            Toast.makeText(MainActivity.getInstance(), error.getMessage(), Toast
                                                    .LENGTH_LONG).show();
                                        }
                                    });
                        }
                    }
                }
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                Log.e(TAG, "Geofencing onEnteredExitedGeofences: " + error.toString());
            }
        });
    }

    private void onError(int i) {
        Log.e(TAG, "Geofencing Error: " + i);
    }
}

