package com.aylanetworks.agilelink;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.GridViewPager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;

public class MainActivity extends WearableActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {

    private static final String DEVICE_NAME = "device_name";
    private static final String DEVICE_DSN = "device_dsn";
    private static final String DEVICE_PROPERTIES = "device_properties";
    private static final String DEVICE_CONTROL_MSG_URI = "/device_control";
    private static final String DEVICE_DRAWABLE = "device_drawable";

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
    private HashMap<String, Bitmap> mDeviceDrawablesMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setAmbientEnabled();

        mPager = (GridViewPager) findViewById(R.id.pager);

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
    }

    private void sendDeviceControlMessage(String dsn, String propertyName, boolean newStatus) {
        byte[] cmdArray = (dsn + "/" + propertyName + "/" + (newStatus ? "1" : "0")).getBytes();

        Wearable.MessageApi.sendMessage(mGoogleApiClient, mHandheldNode,
                DEVICE_CONTROL_MSG_URI, cmdArray).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        boolean success = sendMessageResult.getStatus().isSuccess();

                        Intent confirm = new Intent(MainActivity.this, ConfirmationActivity.class);
                        confirm.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                                success ? ConfirmationActivity.SUCCESS_ANIMATION : ConfirmationActivity.FAILURE_ANIMATION);
                        confirm.putExtra(ConfirmationActivity.EXTRA_MESSAGE,
                                success ? "Command sent" : "Failed to send command");
                        startActivity(confirm);
                    }
                }
        );
    }

    private void updateDeviceDrawableFromAsset(DeviceHolder deviceHolder, Asset asset) {
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                mGoogleApiClient, asset).await().getInputStream();
        if (assetInputStream == null) {
            return;
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        opts.inDither = true;
        Bitmap deviceDrawable = BitmapFactory.decodeStream(assetInputStream, null, opts);
        try {
            assetInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mDeviceDrawablesMap.put(deviceHolder.getDsn(), deviceDrawable);

        if (mAdapter != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Point currentPosition = mPager.getCurrentItem();
                    mAdapter.updateData(null, mDeviceDrawablesMap);
                    mAdapter.notifyDataSetChanged();
                    mPager.setCurrentItem(currentPosition.y, currentPosition.x, false);
                }
            });
        }
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
                    final DataMap deviceMap = DataMapItem.fromDataItem(deviceDataItem).getDataMap();
                    String dsn = deviceMap.getString(DEVICE_DSN);

                    if (dsn == null || !deviceDataItem.getUri().getPath().equals("/" + dsn)) {
                        continue;
                    }

                    String name = deviceMap.getString(DEVICE_NAME);
                    final DeviceHolder deviceHolder = new DeviceHolder(name, dsn);

                    new Thread() {
                        @Override
                        public void run() {
                            updateDeviceDrawableFromAsset(deviceHolder, deviceMap.getAsset(DEVICE_DRAWABLE));
                        }
                    }.start();

                    Gson gson = new Gson();
                    ArrayList<DevicePropertyHolder> propertyHolders = gson.fromJson(deviceMap.getString(DEVICE_PROPERTIES),
                            new TypeToken<ArrayList<DevicePropertyHolder>>(){}.getType());

                    for (DevicePropertyHolder holder : propertyHolders) {
                        deviceHolder.addBooleanProperty(holder);
                        // Log.e("AMAPW", "RECEIVED: " + holder.mFriendlyName + ": " + holder.mState);
                    }

                    mDevicesMap.put(dsn, deviceHolder);
                }
                dataItems.release();

                if (mAdapter == null) {
                    mAdapter = new DevicesGridAdapter(MainActivity.this, getFragmentManager(), mDevicesMap, mDeviceDrawablesMap);
                    mPager.setAdapter(mAdapter);
                } else {
                    Point currentPosition = mPager.getCurrentItem();
                    mAdapter.updateData(mDevicesMap, mDeviceDrawablesMap);
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

        mActionReceiver = new ActionReceiver();
        mLocalBroadcastManager.registerReceiver(mActionReceiver, new IntentFilter(INTENT_PROPERTY_TOGGLED));

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
