package com.aylanetworks.agilelink;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Response;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.ViewModel;
import com.aylanetworks.aylasdk.AylaDatapoint;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaDeviceManager;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.aylasdk.change.Change;
import com.aylanetworks.aylasdk.change.ListChange;
import com.aylanetworks.aylasdk.change.PropertyChange;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class WearUpdateService extends Service implements AylaDevice.DeviceChangeListener,
        AylaDeviceManager.DeviceManagerListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        MessageApi.MessageListener {

    private static final String DEVICE_NAME = "device_name";
    private static final String DEVICE_DSN = "device_dsn";
    private static final String DEVICE_PROPERTIES = "device_properties";
    private static final String DEVICE_DRAWABLE = "device_drawable";

    private static final String DEVICE_CONTROL_MSG_URI = "/device_control";

    private GoogleApiClient mGoogleApiClient;

    private Boolean[] TEST_A_STATUS = {true, false, true};
    private Boolean[] TEST_B_STATUS = {false, true, false};

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

    private byte[] getDrawableByteArray(Drawable drawable) {
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    private void updateWearData() {
        AylaDeviceManager deviceManager = AMAPCore.sharedInstance().getDeviceManager();
        if (deviceManager != null) {
            addTestDeviceA();
            addTestDeviceB();

            List<AylaDevice> allDevices = deviceManager.getDevices();

            if (allDevices != null) {
                for (AylaDevice device : allDevices) {
                    if (!device.isGateway()) {
                        ViewModel deviceModel = AMAPCore.sharedInstance().getSessionParameters().viewModelProvider
                                .viewModelForDevice(device);
                        if (deviceModel == null) {
                            continue;
                        }

                        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/" + device.getDsn());
                        DataMap deviceMap = putDataMapReq.getDataMap();
                        deviceMap.putString(DEVICE_NAME, device.getFriendlyName());
                        deviceMap.putString(DEVICE_DSN, device.getDsn());

                        List<AylaProperty> allDeviceProperties = device.getProperties();
                        List<String> readOnlyProperties = Arrays.asList(deviceModel.getNotifiablePropertyNames());
                        List<String> readWriteProperties = Arrays.asList(deviceModel.getSchedulablePropertyNames());
                        ArrayList<DevicePropertyHolder> propertyHolders = new ArrayList<>();

                        for (AylaProperty property : allDeviceProperties) {
                            if (property.getName() == null || property.getValue() == null ||
                                    !(property.getValue() instanceof Integer)) {
                                continue;
                            }

                            String propertyName = property.getName().trim();
                            String friendlyName = deviceModel.friendlyNameForPropertyName(propertyName);
                            boolean propertyState = (int) property.getValue() == 1;

                            if (readWriteProperties.contains(propertyName)) {
                                propertyHolders.add(new DevicePropertyHolder(friendlyName,
                                        propertyName, false, propertyState));
                            } else if (readOnlyProperties.contains(propertyName)) {
                                propertyHolders.add(new DevicePropertyHolder(friendlyName,
                                        propertyName, true, propertyState));
                            }
                        }

                        if (propertyHolders.size() == 0) {
                            continue;
                        }

                        Gson gson = new Gson();
                        deviceMap.putString(DEVICE_PROPERTIES, gson.toJson(propertyHolders));
                        deviceMap.putLong("timestamp", System.currentTimeMillis());

                        Asset deviceDrawable = Asset.createFromBytes(
                                getDrawableByteArray(deviceModel.getDeviceDrawable(this)));
                        deviceMap.putAsset(DEVICE_DRAWABLE, deviceDrawable);

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

    private void addTestDeviceA() {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/TEST0001");
        DataMap deviceMap = putDataMapReq.getDataMap();
        deviceMap.putString(DEVICE_NAME, "Kitchen Lights");
        deviceMap.putString(DEVICE_DSN, "TEST0001");

        String[] propertyNames = {"Bulb 1", "Bulb 2", "Power"};
        Boolean[] roProperties = {false, false, true};
        ArrayList<DevicePropertyHolder> propertyHolders = new ArrayList<>();

        for (int i = 0; i < propertyNames.length; i++) {
            propertyHolders.add(new DevicePropertyHolder(propertyNames[i],
                    Integer.toString(i), roProperties[i], TEST_A_STATUS[i]));
        }

        Gson gson = new Gson();
        deviceMap.putString(DEVICE_PROPERTIES, gson.toJson(propertyHolders));
        deviceMap.putLong("timestamp", System.currentTimeMillis());

        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.setUrgent();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                Log.e("AMAPW", "DELIVERY TEST A: " + dataItemResult.getStatus().isSuccess());
            }
        });
    }

    private void addTestDeviceB() {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/TEST0002");
        DataMap deviceMap = putDataMapReq.getDataMap();
        deviceMap.putString(DEVICE_NAME, "Front Door");
        deviceMap.putString(DEVICE_DSN, "TEST0002");

        String[] propertyNames = {"Lock", "Door Open", "Alarm"};
        Boolean[] roProperties = {false, true, true};
        ArrayList<DevicePropertyHolder> propertyHolders = new ArrayList<>();

        for (int i = 0; i < propertyNames.length; i++) {
            propertyHolders.add(new DevicePropertyHolder(propertyNames[i],
                    Integer.toString(i), roProperties[i], TEST_B_STATUS[i]));
        }

        Gson gson = new Gson();
        deviceMap.putString(DEVICE_PROPERTIES, gson.toJson(propertyHolders));
        deviceMap.putLong("timestamp", System.currentTimeMillis());

        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.setUrgent();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                Log.e("AMAPW", "DELIVERY TEST B: " + dataItemResult.getStatus().isSuccess());
            }
        });
    }

    private void startListening() {
        AylaDeviceManager deviceManager = AMAPCore.sharedInstance().getDeviceManager();
        if (deviceManager != null) {
            deviceManager.addListener(this);

            for (AylaDevice device : deviceManager.getDevices()) {
                device.addListener(this);
            }

            updateWearData();
            Wearable.MessageApi.addListener(mGoogleApiClient, this);
        }
    }

    private void stopListening() {
        AylaDeviceManager deviceManager = AMAPCore.sharedInstance().getDeviceManager();
        if (deviceManager != null) {
            deviceManager.removeListener(this);
            for (AylaDevice device : deviceManager.getDevices()) {
                device.removeListener(this);
            }

            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        }
    }

    @Override
    public void deviceChanged(AylaDevice device, Change change) {
        if (change.getType() == Change.ChangeType.Property) {
            String changedPropertyName = ((PropertyChange) change).getPropertyName();

            ViewModel deviceModel = AMAPCore.sharedInstance().getSessionParameters().viewModelProvider
                    .viewModelForDevice(device);
            if (deviceModel == null) {
                return;
            }

            List<String> readOnlyProperties = Arrays.asList(deviceModel.getNotifiablePropertyNames());
            List<String> readWriteProperties = Arrays.asList(deviceModel.getSchedulablePropertyNames());

            if (readOnlyProperties.contains(changedPropertyName) || readWriteProperties.contains(changedPropertyName)) {
                updateWearData();
            }
        }
    }

    @Override
    public void deviceListChanged(ListChange change) {
        updateWearData();

        AylaDeviceManager deviceManager = AMAPCore.sharedInstance().getDeviceManager();
        for (AylaDevice device : deviceManager.getDevices()) {
            device.addListener(this);
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (TextUtils.equals(messageEvent.getPath(), DEVICE_CONTROL_MSG_URI)) {
            String cmd = new String(messageEvent.getData());
            String[] cmdComponents = cmd.split("/");
            if (cmdComponents.length < 3) {
                return;
            }

            String dsn = cmdComponents[0];
            String propertyName = cmdComponents[1];
            String propertyState = cmdComponents[2];

            if (dsn.equals("TEST0001")) {
                int propertyIndex = Integer.valueOf(propertyName);
                TEST_A_STATUS[propertyIndex] = Integer.valueOf(propertyState) == 1;
                addTestDeviceA();
            } else if (dsn.equals("TEST0002")) {
                int propertyIndex = Integer.valueOf(propertyName);
                TEST_B_STATUS[propertyIndex] = Integer.valueOf(propertyState) == 1;
                addTestDeviceB();
            }

            AylaDevice device = AMAPCore.sharedInstance().getDeviceManager().deviceWithDSN(dsn);
            if (device == null) {
                return;
            }

            AylaProperty property = device.getProperty(propertyName);
            if (property == null) {
                return;
            }

            property.createDatapoint(Integer.valueOf(propertyState), null, new Response.Listener<AylaDatapoint>() {
                @Override
                public void onResponse(AylaDatapoint response) {

                }
            }, new ErrorListener() {
                @Override
                public void onErrorResponse(AylaError error) {

                }
            });
        }
    }

    @Override
    public void onDestroy() {
        stopListening();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startListening();
    }

    @Override
    public void deviceLanStateChanged(AylaDevice device, boolean lanModeEnabled) {
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
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("AMAPW", "GOOGLE CONNECTION FAILED: " + connectionResult.getErrorMessage());
    }

    private class DevicePropertyHolder implements Serializable {

        public String mFriendlyName;
        public String mPropertyName;
        public boolean mReadOnly;
        public boolean mState;

        public DevicePropertyHolder(String friendlyName, String propertyName,
                                    boolean readOnly, boolean state) {
            mFriendlyName = friendlyName;
            mPropertyName = propertyName;
            mReadOnly = readOnly;
            mState = state;
        }
    }
}
