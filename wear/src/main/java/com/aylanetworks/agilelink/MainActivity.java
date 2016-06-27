package com.aylanetworks.agilelink;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.GridViewPager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.TreeMap;

public class MainActivity extends WearableActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {

    private static final String DEVICE_NAME = "device_name";
    private static final String DEVICE_PROPERTIES = "device_properties";

    public static final String INTENT_PROPERTY_TOGGLED = "com.aylanetworks.agilelink.PROPERTY_TOGGLED";
    public static final String EXTRA_DEVICE_NAME = "device_name";
    public static final String EXTRA_PROPERTY_NAME = "property_name";
    public static final String EXTRA_PROPERTY_STATE = "property_state";

    private GoogleApiClient mGoogleApiClient;
    private LocalBroadcastManager mLocalBroadcastManager;
    private ActionReceiver mActionReceiver;
    private GridViewPager mPager;

    private TreeMap<String, DeviceHolder> mDevicesMap = new TreeMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        mPager = (GridViewPager) findViewById(R.id.pager);
    }

    private void setDeviceData(String deviceName, String propertyName, boolean newState) {
        if (mDevicesMap.containsKey(deviceName)) {
            DeviceHolder device = mDevicesMap.get(deviceName);
            device.setBooleanProperty(propertyName, newState);

            PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/devices/" + device.getName());
            DataMap deviceMap = putDataMapReq.getDataMap();
            deviceMap.putString(DEVICE_NAME, deviceName);

            Bundle propertiesBundle = new Bundle();
            propertiesBundle.putBoolean(propertyName, newState);
            deviceMap.putDataMap(DEVICE_PROPERTIES, DataMap.fromBundle(propertiesBundle));
            deviceMap.putLong("timestamp", System.currentTimeMillis());

            PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
            putDataReq.setUrgent();
            PendingResult<DataApi.DataItemResult> pendingResult =
                    Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
            pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                    Log.e("AMAPW", "W: DELIVERY: " + dataItemResult.getStatus().isSuccess());
                }
            });
        }
    }

    private void updateDevicesData() {
        PendingResult<DataItemBuffer> result = Wearable.DataApi.getDataItems(mGoogleApiClient);
        result.setResultCallback(new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(@NonNull DataItemBuffer dataItems) {
                Log.e("AMAPW", "ITEMS COUNT: " + dataItems.getCount());
                for (DataItem deviceDataItem : dataItems) {
                    DataMap deviceMap = DataMapItem.fromDataItem(deviceDataItem).getDataMap();
                    String name = deviceMap.getString(DEVICE_NAME);
                    DeviceHolder deviceHolder = new DeviceHolder(name);

                    DataMap propertiesMap = deviceMap.getDataMap(DEVICE_PROPERTIES);
                    Log.e("AMAPW", "PROPERTIES COUNT: " + propertiesMap.size());
                    for (String propertyName : propertiesMap.keySet()) {
                        deviceHolder.setBooleanProperty(propertyName, propertiesMap.getBoolean(propertyName));
                        Log.e("AMAPW", "RECEIVED: " + propertyName + ": " + propertiesMap.getBoolean(propertyName));
                    }

                    mDevicesMap.put(name, deviceHolder);
                }
                dataItems.release();

                DevicesGridAdapter adapter = new DevicesGridAdapter(getFragmentManager(), mDevicesMap);
                mPager.setAdapter(adapter);
            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        updateDevicesData();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.e("AMAPW", "onConnectionSuspended: " + cause);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e("AMAPW", "onConnectionFailed: " + result);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        dataEventBuffer.release();
        // updateDevicesData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();

        mActionReceiver = new ActionReceiver();
        mLocalBroadcastManager.registerReceiver(mActionReceiver, new IntentFilter(INTENT_PROPERTY_TOGGLED));
    }

    @Override
    protected void onPause() {
        super.onPause();

        mLocalBroadcastManager.unregisterReceiver(mActionReceiver);
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    private class ActionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(INTENT_PROPERTY_TOGGLED)) {
                String deviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
                String propertyName = intent.getStringExtra(EXTRA_PROPERTY_NAME);
                if (deviceName != null && propertyName != null) {
                    boolean propertyState = intent.getBooleanExtra(EXTRA_PROPERTY_STATE, false);
                    setDeviceData(deviceName, propertyName, propertyState);
                }
            }
        }
    }
}
