package com.aylanetworks.agilelink.geofence;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.framework.geofence.ALGeofenceLocation;
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
    private List<ALGeofenceLocation> _alGeofenceLocations;
    private Geofence _geofenceToAdd;
    private ALGeofenceLocation _alGeofenceLocationToAdd;

    public static GeofenceController _instance;

    public List<ALGeofenceLocation> getALGeofenceLocations() {
        return _alGeofenceLocations;
    }

    private List<ALGeofenceLocation> ALGeofenceLocationsToRemove;


    public static GeofenceController getInstance() {
        if (_instance == null) {
            _instance = new GeofenceController();
        }
        return _instance;
    }

    public void init(Context context) {
        _context = context.getApplicationContext();
        ALGeofenceLocationsToRemove = new ArrayList<>();
    }

    public void setALGeofenceLocations(List<ALGeofenceLocation> geofenceLocationList) {
        _alGeofenceLocations = geofenceLocationList;
    }

    public void addGeofence(ALGeofenceLocation alGeofenceLocation, GeofenceControllerListener
            _listener) {
        this._alGeofenceLocationToAdd = alGeofenceLocation;
        this._geofenceToAdd = geofence(alGeofenceLocation);
        this._listener = _listener;

        connectWithCallbacks(connectionAddListener);
    }

    private Geofence geofence(ALGeofenceLocation alGeofenceLocation) {
        return new Geofence.Builder()
                .setRequestId(alGeofenceLocation.getId())
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .setCircularRegion(alGeofenceLocation.getLatitude(), alGeofenceLocation
                        .getLongitude(), alGeofenceLocation.getRadius())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build();
    }

    public void removeGeofences(List<ALGeofenceLocation> ALGeofenceLocationsToRemove, GeofenceControllerListener _listener) {
        this.ALGeofenceLocationsToRemove = ALGeofenceLocationsToRemove;
        this._listener = _listener;

        connectWithCallbacks(connectionRemoveListener);
    }

    public void removeAllGeofences(GeofenceControllerListener _listener) {
        ALGeofenceLocationsToRemove = new ArrayList<>();
        for (com.aylanetworks.agilelink.framework.geofence.ALGeofenceLocation ALGeofenceLocation : _alGeofenceLocations) {
            ALGeofenceLocationsToRemove.add(ALGeofenceLocation);
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
        _alGeofenceLocations.add(_alGeofenceLocationToAdd);
        if (_listener != null) {
            _listener.onGeofencesUpdated();
        }
    }

    private void removeSavedGeofences() {
        for (com.aylanetworks.agilelink.framework.geofence.ALGeofenceLocation ALGeofenceLocation : ALGeofenceLocationsToRemove) {
            int index = _alGeofenceLocations.indexOf(ALGeofenceLocation);
            _alGeofenceLocations.remove(index);
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
            for (com.aylanetworks.agilelink.framework.geofence.ALGeofenceLocation ALGeofenceLocation : ALGeofenceLocationsToRemove) {
                removeIds.add(ALGeofenceLocation.getId());
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