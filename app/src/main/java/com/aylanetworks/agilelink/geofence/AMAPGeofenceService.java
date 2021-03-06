package com.aylanetworks.agilelink.geofence;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.automation.Automation;
import com.aylanetworks.agilelink.framework.automation.AutomationManager;
import com.aylanetworks.agilelink.framework.batch.BatchAction;
import com.aylanetworks.agilelink.framework.batch.BatchManager;
import com.aylanetworks.agilelink.framework.geofence.Action;
import com.aylanetworks.agilelink.framework.geofence.AylaDeviceActions;
import com.aylanetworks.aylasdk.AylaDatapoint;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.error.ServerError;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/*
 * AMAP_Android
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */

public class AMAPGeofenceService extends IntentService {
    private final static String TAG = AMAPGeofenceService.class.getName();

    public AMAPGeofenceService() {
        super("AMAPGeofenceService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        final ArrayList<Geofence> list = (ArrayList<Geofence>) event.getTriggeringGeofences();

        if (event != null) {
            if (event.hasError()) {
                onError(event.getErrorCode());
            } else {
                int transition = event.getGeofenceTransition();
                if (transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                    if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                        onEnteredExitedGeofences(true,list);
                    } else {
                        onEnteredExitedGeofences(false,list);
                    }
                }
            }
        }
    }

    private void onEnteredExitedGeofences(final boolean entered,final ArrayList<Geofence> geofenceList) {
        //Check if we have session manager
        AMAPCore coreInstance= AMAPCore.sharedInstance();
        if(coreInstance != null && coreInstance.getSessionManager() != null) {
            fireBatchActions(entered,geofenceList);
        } else {
            sendNotification(entered,geofenceList);
        }
    }

    /**
     * Send a Notification to Android Phone
     * @param entered true if entered a geofence, false otherwise
     * @param geofenceList list of triggering geofences
     */
    private void sendNotification(final boolean entered,final ArrayList<Geofence> geofenceList) {
        String contentText= getString(R.string.notif_geofence_leaving);
        if(entered) {
            contentText= getString(R.string.notif_geofence_entering);
        }
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentTitle(getString(R.string.geofence_event))
                        .setContentText(contentText)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setAutoCancel(true);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Bundle bundle = new Bundle();
        bundle.putBoolean(MainActivity.ARG_TRIGGER_TYPE, entered);
        bundle.putSerializable(MainActivity.GEO_FENCE_LIST, geofenceList);
        intent.putExtras(bundle);
        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    public static void fireBatchActions(final boolean entered,final ArrayList<Geofence> geofenceList) {
        if(geofenceList == null) {
            return;
        }
        BatchManager.fetchBatchActions(new Response.Listener<BatchAction[]>
                () {
            @Override
            public void onResponse(BatchAction[] response) {
                HashMap<String,BatchAction> mapBatchAction= new HashMap<>(response.length);
                for(BatchAction batchAction:response) {
                    mapBatchAction.put(batchAction.getUuid(), batchAction);
                }
                fetchAutomations(entered,geofenceList,mapBatchAction);
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                //Check if there are no existing automations. This is not an actual error and we
                //don't want to show this error. Just log it in case of no Existing automations
                if (error instanceof ServerError) {
                    ServerError serverError = ((ServerError) error);
                    int code = serverError.getServerResponseCode();
                    if (code == NanoHTTPD.Response.Status.NOT_FOUND.getRequestStatus()) {
                        Log.d(TAG, "No Existing BatchAction");
                    } else {
                        Log.e(TAG, "Error in fetch BatchAction " + error.getMessage());
                    }
                } else {
                    Log.e(TAG, "Error in fetch BatchAction " + error.getMessage());
                }
            }
        });
    }
    private static void fetchAutomations(final boolean entered,final ArrayList<Geofence> geofenceList,
                                         final Map<String,BatchAction> mapBatchAction){
        AutomationManager.fetchAutomation(new Response.Listener<Automation[]>
                () {
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
                            automation.isEnabled(MainActivity.getInstance())) {
                        //Now go through the geofence list and make sure the automation geofence ID
                        // is in this list
                        for(Geofence geofence:geofenceList) {
                            if(geofence.getRequestId().equals(automation.getTriggerUUID())) {
                                HashSet<String> actionSet = new HashSet<>();
                                String[] batchActionUUIDS= automation.getActions();
                                if(batchActionUUIDS == null) {
                                    return;
                                }
                                for(String batchActionUUID:batchActionUUIDS){
                                    BatchAction batchAction = mapBatchAction.get(batchActionUUID);
                                    if(batchAction != null && batchAction.getActionUuids()!= null){
                                        actionSet.addAll(Arrays.asList(batchAction.getActionUuids()));
                                    }
                                }
                                setGeofenceActions(actionSet);
                            }
                        }
                    }
                }
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                //Check if there are no existing automations. This is not an actual error and we
                //don't want to show this error. Just log it in case of no Existing automations
                if(error instanceof ServerError){
                    ServerError serverError = ((ServerError) error);
                    int code = serverError.getServerResponseCode();
                    if (code == NanoHTTPD.Response.Status.NOT_FOUND.getRequestStatus()) {
                        Log.d(TAG,"No Existing Automation");
                    } else {
                        Log.e(TAG,"Error in fetch automations "+error.getMessage());
                    }
                } else{
                    Log.e(TAG,"Error in fetch automations "+error.getMessage());
                }

            }
        });
    }

    private static void setGeofenceActions(final HashSet<String> actionSet) {
        AylaDeviceActions.fetchActions(new Response.Listener<Action[]>() {
            @Override
            public void onResponse(Action[] arrayAction) {
                for (final Action action : arrayAction) {
                    if (action != null && actionSet.contains((action.getId()))) {
                        AylaDevice device = AMAPCore.sharedInstance().getDeviceManager()
                                .deviceWithDSN(action.getDSN());
                        if(device == null) {
                            continue;
                        }
                        final AylaProperty entryProperty = device.getProperty(action.getPropertyName());
                        if(entryProperty == null) {
                            continue;
                        }
                        Object value= action.getValue();
                        entryProperty.createDatapoint(value, null, new Response
                                        .Listener<AylaDatapoint<Integer>>() {
                                    @Override
                                    public void onResponse(final AylaDatapoint<Integer> response) {
                                        String str = "Property Name:" + entryProperty.getName();
                                        str += " value " + action.getValue();
                                        Log.d("setGeofenceActions", "OnEnteredExitedGeofences success: " + str);
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
