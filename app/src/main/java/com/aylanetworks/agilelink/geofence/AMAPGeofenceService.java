package com.aylanetworks.agilelink.geofence;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.geofence.Action;
import com.aylanetworks.agilelink.framework.geofence.AylaDeviceActions;
import com.aylanetworks.agilelink.framework.automation.Automation;
import com.aylanetworks.agilelink.framework.automation.AutomationManager;
import com.aylanetworks.aylasdk.AylaDatapoint;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.util.TypeUtils;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
        AutomationManager.fetchAutomation(new Response.Listener<Automation[]>() {
            @Override
            public void onResponse(Automation[] response) {
                Automation.ALAutomationTriggerType triggerType = Automation.ALAutomationTriggerType.TriggerTypeGeofenceEnter;
                if (!entered) {
                    triggerType = Automation.ALAutomationTriggerType.TriggerTypeGeofenceExit;
                }
                for (Automation automation : response) {
                    //Make sure Trigger Type matches and the automation is also enabled before
                    // firing the events
                    if (automation.getAutomationTriggerType().equals(triggerType) &&
                            automation.isEnabled()) {
                        getActions(automation.getActions());
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

    private void getActions(final String[] actionUUIDs) {
        AylaDeviceActions.fetchActions(new Response.Listener<Action[]>() {
            @Override
            public void onResponse(Action[] arrayAction) {
                Set<String> actionSet = new HashSet<>(Arrays.asList(actionUUIDs));
                for (final Action action : arrayAction) {
                    if (actionSet.contains((action.getId()))) {
                        AylaDevice device = AMAPCore.sharedInstance().getDeviceManager()
                                .deviceWithDSN(action.getDSN());
                        final AylaProperty entryProperty = device.getProperty(action.getPropertyName());

                        Object value = TypeUtils.getTypeConvertedValue(entryProperty.getBaseType(), action.getValue());
                        entryProperty.createDatapoint(value, null, new Response
                                        .Listener<AylaDatapoint<Integer>>() {
                                    @Override
                                    public void onResponse(final AylaDatapoint<Integer> response) {
                                        String str = "Property Name:" + entryProperty.getName();
                                        str += " value " + action.getValue();
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
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                Toast.makeText(MainActivity.getInstance(), error.getMessage(), Toast
                        .LENGTH_LONG).show();
            }
        });
    }

    private void onError(int i) {
        Log.e(TAG, "Geofencing Error: " + i);
    }
}

