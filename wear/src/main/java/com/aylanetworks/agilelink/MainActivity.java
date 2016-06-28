package com.aylanetworks.agilelink;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.GridViewPager;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.Set;
import java.util.TreeMap;

public class MainActivity extends WearableActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {

    private static final String DEVICE_NAME = "device_name";
    private static final String DEVICE_DSN = "device_dsn";
    private static final String DEVICE_PROPERTIES = "device_properties";
    private static final String DEVICE_CONTROL_MSG_URI = "/device_control";

    public static final String INTENT_PROPERTY_TOGGLED = "com.aylanetworks.agilelink.PROPERTY_TOGGLED";
    public static final String EXTRA_DEVICE_DSN = "device_dsn";
    public static final String EXTRA_PROPERTY_NAME = "property_name";
    public static final String EXTRA_PROPERTY_STATE = "property_state";

    private GoogleApiClient mGoogleApiClient;
    private LocalBroadcastManager mLocalBroadcastManager;
    private ActionReceiver mActionReceiver;
    private GridViewPager mPager;
    private DevicesGridAdapter mAdapter;
    private String mHandheldNode = "";

    private TreeMap<String, DeviceHolder> mDevicesMap = new TreeMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setAmbientEnabled();

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        mPager = (GridViewPager) findViewById(R.id.pager);
    }

    private void sendDeviceControlMessage(String dsn, String propertyName, boolean newStatus) {
        byte[] cmdArray = (dsn + "/" + propertyName + "/" + (newStatus ? "1" : "0")).getBytes();

        Wearable.MessageApi.sendMessage(mGoogleApiClient, mHandheldNode,
                DEVICE_CONTROL_MSG_URI, cmdArray).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        Log.e("AMAPW", "DEV CON MSG: " + sendMessageResult.getStatus().isSuccess());
                    }
                }
        );
    }

    private void getHandheldNode() {
        CapabilityApi.GetCapabilityResult result = Wearable.CapabilityApi.getCapability(
                mGoogleApiClient,
                "ayla_device_control",
                CapabilityApi.FILTER_REACHABLE).await();
        Set<Node> connectedNodes = result.getCapability().getNodes();
        for (Node node : connectedNodes) {
            if (node.isNearby()) {
                mHandheldNode = node.getId();
                return;
            }
        }
    }

    private void getDevicesData() {
        PendingResult<DataItemBuffer> result = Wearable.DataApi.getDataItems(mGoogleApiClient);
        result.setResultCallback(new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(@NonNull DataItemBuffer dataItems) {
                for (DataItem deviceDataItem : dataItems) {
                    DataMap deviceMap = DataMapItem.fromDataItem(deviceDataItem).getDataMap();
                    String dsn = deviceMap.getString(DEVICE_DSN);

                    if (dsn == null || !deviceDataItem.getUri().toString().contains(dsn)) {
                        continue;
                    }

                    String name = deviceMap.getString(DEVICE_NAME);
                    DeviceHolder deviceHolder = new DeviceHolder(name, dsn);

                    DataMap propertiesMap = deviceMap.getDataMap(DEVICE_PROPERTIES);
                    for (String propertyName : propertiesMap.keySet()) {
                        deviceHolder.setBooleanProperty(propertyName, propertiesMap.getBoolean(propertyName));
                        Log.e("AMAPW", "RECEIVED: " + propertyName + ": " + propertiesMap.getBoolean(propertyName));
                    }

                    mDevicesMap.put(dsn, deviceHolder);
                }
                dataItems.release();

                if (mAdapter == null) {
                    mAdapter = new DevicesGridAdapter(getFragmentManager(), mDevicesMap);
                    mPager.setAdapter(mAdapter);
                } else {
                    Point currentPosition = mPager.getCurrentItem();
                    mAdapter.updateData(mDevicesMap);
                    mAdapter.notifyDataSetChanged();
                    mPager.setCurrentItem(currentPosition.y, currentPosition.x, false);
                }
            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        getDevicesData();

        new Thread() {
            @Override
            public void run() {
                getHandheldNode();
            }
        }.start();
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
        getDevicesData();
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
                String deviceDsn = intent.getStringExtra(EXTRA_DEVICE_DSN);
                String propertyName = intent.getStringExtra(EXTRA_PROPERTY_NAME);
                if (deviceDsn != null && propertyName != null) {
                    boolean propertyState = intent.getBooleanExtra(EXTRA_PROPERTY_STATE, false);
                    sendDeviceControlMessage(deviceDsn, propertyName, propertyState);
                }
            }
        }
    }
}
