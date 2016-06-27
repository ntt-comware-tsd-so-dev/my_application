package com.aylanetworks.agilelink;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.ViewModel;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaDeviceManager;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.aylasdk.change.Change;
import com.aylanetworks.aylasdk.change.ListChange;
import com.aylanetworks.aylasdk.error.AylaError;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class WearUpdateService extends Service implements AylaDevice.DeviceChangeListener,
        AylaDeviceManager.DeviceManagerListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {

    private static final String DEVICE_NAME = "device_name";
    private static final String DEVICE_PROPERTIES = "device_properties";

    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

        //TODO: FOREGROUND
    }

    private void updateWearData() {
        AylaDeviceManager deviceManager = AMAPCore.sharedInstance().getDeviceManager();
        if (deviceManager != null) {
            List<AylaDevice> allDevices = deviceManager.getDevices();

            if (allDevices != null) {
                for (AylaDevice device : allDevices) {
                    if (!device.isGateway()) {
                        ViewModel deviceModel = AMAPCore.sharedInstance().getSessionParameters().viewModelProvider
                                .viewModelForDevice(device);
                        if (deviceModel == null) {
                            continue;
                        }

                        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/devices/" + device.getFriendlyName());
                        DataMap deviceMap = putDataMapReq.getDataMap();
                        deviceMap.putString(DEVICE_NAME, device.getFriendlyName());

                        Bundle propertiesBundle = new Bundle();
                        List<AylaProperty> allDeviceProperties = device.getProperties();
                        ArrayList<String> matchedDeviceProperties = new ArrayList<>();
                        matchedDeviceProperties.addAll(Arrays.asList(deviceModel.getSchedulablePropertyNames()));
                        matchedDeviceProperties.addAll(Arrays.asList(deviceModel.getNotifiablePropertyNames()));

                        for (AylaProperty property : allDeviceProperties) {
                            String propertyName = property.getName();
                            if (matchedDeviceProperties.contains(propertyName)) {
                                propertiesBundle.putBoolean(propertyName, (int) property.getValue() == 1);
                            }
                        }

                        if (propertiesBundle.size() == 0) {
                            continue;
                        }

                        deviceMap.putDataMap(DEVICE_PROPERTIES, DataMap.fromBundle(propertiesBundle));
                        deviceMap.putLong("timestamp", System.currentTimeMillis());

                        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
                        putDataReq.setUrgent();
                        PendingResult<DataApi.DataItemResult> pendingResult =
                                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
                        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                            @Override
                            public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                                Log.e("AMAPW", "DELIVERY: " + dataItemResult.getStatus().isSuccess());
                            }
                        });
                    }
                }
            }
        }
    }

    private void startListening() {
        AylaDeviceManager deviceManager = AMAPCore.sharedInstance().getDeviceManager();
        if (deviceManager != null) {
            deviceManager.addListener(this);

            for (AylaDevice device : deviceManager.getDevices()) {
                device.addListener(this);
            }

            updateWearData();
        }
    }

    private void stopListening() {
        AylaDeviceManager deviceManager = AMAPCore.sharedInstance().getDeviceManager();
        if (deviceManager != null) {
            deviceManager.removeListener(this);
            for (AylaDevice device : deviceManager.getDevices()) {
                device.removeListener(this);
            }
        }
    }

    @Override
    public void deviceChanged(AylaDevice device, Change change) {
        updateWearData();
    }

    @Override
    public void deviceListChanged(ListChange change) {
        updateWearData();
    }

    @Override
    public void onDestroy() {
        stopListening();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void deviceLanStateChanged(AylaDevice device, boolean lanModeEnabled) {
        //
    }

    @Override
    public void deviceError(AylaDevice device, AylaError error) {
    }

    @Override
    public void deviceManagerInitComplete(Map<String, AylaError> deviceFailures) {
    }

    @Override
    public void deviceManagerInitFailure(AylaError error, AylaDeviceManager.DeviceManagerState failureState) {
    }

    @Override
    public void deviceManagerError(AylaError error) {
    }

    @Override
    public void deviceManagerStateChanged(AylaDeviceManager.DeviceManagerState oldState, AylaDeviceManager.DeviceManagerState newState) {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startListening();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("AMAPW", "GOOGLE CONN FAILED");
    }
}
