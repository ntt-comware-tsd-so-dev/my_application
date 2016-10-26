package com.aylanetworks.agilelink.geofence;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.android.volley.Response;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.framework.automation.Automation;
import com.aylanetworks.agilelink.framework.automation.AutomationManager;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.Arrays;
import java.util.HashSet;

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
        HashSet<String> actionSet = new HashSet<>(Arrays.asList(actionUUIDs));
        Intent intent = new Intent();
        intent.setClass(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Bundle bundle = new Bundle();
        bundle.putSerializable(MainActivity.ARG_ACTION_UUIDS, actionSet);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void onError(int i) {
        Log.e(TAG, "Geofencing Error: " + i);
    }

}

