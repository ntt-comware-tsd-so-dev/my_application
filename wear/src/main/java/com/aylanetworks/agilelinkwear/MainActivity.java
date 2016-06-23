package com.aylanetworks.agilelinkwear;

import android.os.Bundle;
import android.support.annotation.NonNull;
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
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;

public class MainActivity extends WearableActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {

    private static final String DEVICE_NAME = "device_name";
    private static final String DEVICE_PROPERTIES = "device_properties";

    private GoogleApiClient mGoogleApiClient;
    private GridViewPager mPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        mPager = (GridViewPager) findViewById(R.id.pager);
        updateDevicesData();
    }

    private void updateDevicesData() {
        PendingResult<DataItemBuffer> result = Wearable.DataApi.getDataItems(mGoogleApiClient);
        result.setResultCallback(new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(@NonNull DataItemBuffer dataItems) {
                ArrayList<DeviceHolder> devices = new ArrayList<>();

                for (DataItem deviceDataItem : dataItems) {
                    DataMap deviceMap = DataMapItem.fromDataItem(deviceDataItem).getDataMap();

                    String name = deviceMap.getString(DEVICE_NAME);
                    DataMap propertiesMap = deviceMap.getDataMap(DEVICE_PROPERTIES);

                    DeviceHolder deviceHolder = new DeviceHolder(name);
                    for (String propertyName : propertiesMap.keySet()) {
                        deviceHolder.setBooleanProperty(propertyName, propertiesMap.getBoolean(propertyName));
                    }
                    devices.add(deviceHolder);
                }
                dataItems.release();

                DevicesGridAdapter adapter = new DevicesGridAdapter(getFragmentManager(), devices);
                mPager.setAdapter(adapter);
            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
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
        updateDevicesData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();

        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }
}
