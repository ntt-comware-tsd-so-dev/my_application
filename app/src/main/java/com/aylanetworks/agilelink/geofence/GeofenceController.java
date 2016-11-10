package com.aylanetworks.agilelink.geofence;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.framework.geofence.GeofenceLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

/*
 * AMAP_Android
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */

public class GeofenceController {
    private final String TAG = GeofenceController.class.getName();

    private Context _context;
    private GoogleApiClient _googleApiClient;
    private GeofenceControllerListener _listener;
    private List<GeofenceLocation> _geofenceLocations;
    private Geofence _geofenceToAdd;
    private GeofenceLocation _geofenceLocationToAdd;
    private SharedPreferences _prefs;
    public static final String SHARED_PERFS_GEOFENCE = "SHARED_PREFS_GEOFENCES";

    private static GeofenceController __instance;

    public List<GeofenceLocation> getALGeofenceLocations() {
        return _geofenceLocations;
    }

    private List<GeofenceLocation> GeofenceLocationsToRemove;


    public static GeofenceController getInstance() {
        if (__instance == null) {
            __instance = new GeofenceController();
        }
        return __instance;
    }

    public void init(Context context) {
        _context = context.getApplicationContext();
        GeofenceLocationsToRemove = new ArrayList<>();
        _prefs = context.getSharedPreferences(SHARED_PERFS_GEOFENCE, Context.MODE_PRIVATE);
    }

    public void setALGeofenceLocations(List<GeofenceLocation> geofenceLocationList) {
        _geofenceLocations = geofenceLocationList;
    }

    public void addGeofence(GeofenceLocation geofenceLocation, GeofenceControllerListener
            _listener) {
        this._geofenceLocationToAdd = geofenceLocation;
        this._geofenceToAdd = geofence(geofenceLocation);
        this._listener = _listener;

        connectWithCallbacks(connectionAddListener);
    }

    private Geofence geofence(GeofenceLocation geofenceLocation) {
        return new Geofence.Builder()
                .setRequestId(geofenceLocation.getId())
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .setCircularRegion(geofenceLocation.getLatitude(), geofenceLocation
                        .getLongitude(), geofenceLocation.getRadius())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setLoiteringDelay(30000)
                .build();
    }

    public void removeGeofences(List<GeofenceLocation> GeofenceLocationsToRemove, GeofenceControllerListener _listener) {
        this.GeofenceLocationsToRemove = GeofenceLocationsToRemove;
        this._listener = _listener;

        connectWithCallbacks(connectionRemoveListener);
    }

    public void removeAllGeofences(GeofenceControllerListener _listener) {
        GeofenceLocationsToRemove = new ArrayList<>();
        for (GeofenceLocation GeofenceLocation : _geofenceLocations) {
            GeofenceLocationsToRemove.add(GeofenceLocation);
        }
        this._listener = _listener;

        connectWithCallbacks(connectionRemoveListener);
    }

    private void connectWithCallbacks(GoogleApiClient.ConnectionCallbacks callbacks) {
            _googleApiClient = new GoogleApiClient.Builder(_context)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(callbacks)
                    .addOnConnectionFailedListener(connectionFailedListener)
                    .build();
            _googleApiClient.connect();
    }

    private GeofencingRequest getAddGeofencingRequest() {
        List<Geofence> geofencesToAdd = new ArrayList<>();
        geofencesToAdd.add(_geofenceToAdd);
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofencesToAdd);
        return builder.build();
    }

    private void saveGeofence() {
        _geofenceLocations.add(_geofenceLocationToAdd);
        if (_listener != null) {
            _listener.onGeofencesUpdated();
        }
        SharedPreferences.Editor editor = _prefs.edit();
        editor.putString(_geofenceLocationToAdd.getId(), "true");
        editor.apply();
    }

    private void removeSavedGeofences() {
        SharedPreferences.Editor editor = _prefs.edit();
        for (GeofenceLocation geofenceLocation : GeofenceLocationsToRemove) {
            int index = _geofenceLocations.indexOf(geofenceLocation);
            _geofenceLocations.remove(index);
            editor.remove(geofenceLocation.getId());
            editor.apply();
        }

        if (_listener != null) {
            _listener.onGeofencesUpdated();
        }
    }

    private void sendError() {
        if (_listener != null) {
            _listener.onError();
        }
    }

    private final GoogleApiClient.ConnectionCallbacks connectionAddListener = new GoogleApiClient
            .ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {

            if (ActivityCompat.checkSelfPermission(MainActivity.getInstance(), Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.getInstance(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MainActivity.REQUEST_FINE_LOCATION);
            }
            Intent intent = new Intent(_context, AMAPGeofenceService.class);
            PendingIntent pendingIntent = PendingIntent.getService(_context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            if(_googleApiClient.isConnected() == false) {
                return;
            }

            PendingResult<Status> result = LocationServices.GeofencingApi.addGeofences(_googleApiClient, getAddGeofencingRequest(), pendingIntent);
            result.setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    if (status.isSuccess()) {
                        saveGeofence();
                    } else {
                        Log.e(TAG, "Registering geofence failed: " + status.getStatusMessage() + " : " + status.getStatusCode());
                        sendError();
                    }
                }
            });
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.e(TAG, "Connecting to GoogleApiClient suspended.");
            sendError();
        }
    };

    private final GoogleApiClient.ConnectionCallbacks connectionRemoveListener = new GoogleApiClient
            .ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            List<String> removeIds = new ArrayList<>();
            for (GeofenceLocation GeofenceLocation : GeofenceLocationsToRemove) {
                removeIds.add(GeofenceLocation.getId());
            }

            if (removeIds.size() > 0) {
                PendingResult<Status> result = LocationServices.GeofencingApi.removeGeofences(_googleApiClient, removeIds);
                result.setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            removeSavedGeofences();
                        } else {
                            Log.e(TAG, "Removing geofence failed: " + status.getStatusMessage());
                            sendError();
                        }
                    }
                });
            }
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.e(TAG, "Connecting to GoogleApiClient suspended.");
            sendError();
        }
    };

    private final GoogleApiClient.OnConnectionFailedListener connectionFailedListener = new
            GoogleApiClient.OnConnectionFailedListener() {
                @Override
                public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                    Log.e(TAG, "Connecting to GoogleApiClient failed.");
                    sendError();
                }
            };

    public interface GeofenceControllerListener {
        void onGeofencesUpdated();

        void onError();
    }
}