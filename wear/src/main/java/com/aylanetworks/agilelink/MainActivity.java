package com.aylanetworks.agilelink;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.DismissOverlayView;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;

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

public class MainActivity extends WearableActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        DataApi.DataListener,
        DeviceFragment.OnPropertyToggleListener {

    private static final String DEVICE_NAME = "device_name";
    private static final String DEVICE_DSN = "device_dsn";
    private static final String DEVICE_PROPERTIES = "device_properties";
    private static final String DEVICE_DRAWABLE = "device_drawable";
    private static final String DEVICE_CONTROL_MSG_URI = "/device_control";

    private GoogleApiClient mGoogleApiClient;
    private GridViewPager mPager;
    private DotsPageIndicator mPageDots;
    private DevicesGridAdapter mAdapter;
    private DismissOverlayView mDismissOverlay;
    private GestureDetector mDetector;

    private String mHandheldNode = "";
    private boolean mInitialized = false;

    private TreeMap<String, DeviceHolder> mDevicesMap = new TreeMap<>();
    private HashMap<String, Bitmap> mDeviceDrawablesMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setAmbientEnabled();

        mPager = (GridViewPager) findViewById(R.id.pager);
        mPageDots = (DotsPageIndicator) findViewById(R.id.page_dots);
        mPageDots.setPager(mPager);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        mInitialized = false;
        mGoogleApiClient.connect();

        mDismissOverlay = (DismissOverlayView) findViewById(R.id.dismiss_overlay);
        mDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent ev) {
                mDismissOverlay.show();
            }
        });
        mPager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mDetector.onTouchEvent(event);
            }
        });
    }

    private boolean sendDeviceControlMessage(String dsn, String propertyName, boolean newStatus) {
        byte[] cmdArray = (dsn + "/" + propertyName + "/" + (newStatus ? "1" : "0")).getBytes();

        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, mHandheldNode,
                DEVICE_CONTROL_MSG_URI, cmdArray).await();
        return result.getStatus().isSuccess();
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
    }

    private void getHandheldNode() {
        PendingResult<CapabilityApi.GetCapabilityResult> result = Wearable.CapabilityApi.getCapability(
                mGoogleApiClient,
                "ayla_device_control",
                CapabilityApi.FILTER_REACHABLE);

        result.setResultCallback(new ResultCallback<CapabilityApi.GetCapabilityResult>() {
            @Override
            public void onResult(@NonNull CapabilityApi.GetCapabilityResult getCapabilityResult) {
                Set<Node> connectedNodes = getCapabilityResult.getCapability().getNodes();
                for (Node node : connectedNodes) {
                    if (node.isNearby()) {
                        mHandheldNode = node.getId();
                        return;
                    }
                }
            }
        });
    }

    private void getDevicesData() {
        DataItemBuffer dataBuffer = Wearable.DataApi.getDataItems(mGoogleApiClient).await();
        for (DataItem deviceDataItem : dataBuffer) {
            final DataMap deviceMap = DataMapItem.fromDataItem(deviceDataItem).getDataMap();
            String dsn = deviceMap.getString(DEVICE_DSN);

            if (dsn == null || !deviceDataItem.getUri().getPath().equals("/" + dsn)) {
                continue;
            }

            String name = deviceMap.getString(DEVICE_NAME);
            final DeviceHolder deviceHolder = new DeviceHolder(name, dsn);

            final Asset deviceDrawable = deviceMap.getAsset(DEVICE_DRAWABLE);
            if (deviceDrawable != null) {
                updateDeviceDrawableFromAsset(deviceHolder, deviceDrawable);
            }

            Gson gson = new Gson();
            ArrayList<DevicePropertyHolder> propertyHolders = gson.fromJson(deviceMap.getString(DEVICE_PROPERTIES),
                    new TypeToken<ArrayList<DevicePropertyHolder>>(){}.getType());

            for (DevicePropertyHolder holder : propertyHolders) {
                deviceHolder.addBooleanProperty(holder);
            }

            mDevicesMap.put(dsn, deviceHolder);
        }
        dataBuffer.release();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mInitialized = true;
        new DataLoader().execute();
        Wearable.DataApi.addListener(mGoogleApiClient, this);

        getHandheldNode();
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
        new DataLoader().execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected() && !mInitialized) {
            new DataLoader().execute();
            Wearable.DataApi.addListener(mGoogleApiClient, this);

            getHandheldNode();
            mInitialized = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mInitialized = false;
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }

    private class DataLoader extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... params) {
            getDevicesData();
            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (mAdapter == null) {
                mAdapter = new DevicesGridAdapter(MainActivity.this, getFragmentManager(), mDevicesMap, mDeviceDrawablesMap);
                mPager.setAdapter(mAdapter);

                mPager.animate().withLayer().alpha(1).setDuration(250).start();
                final ProgressBar loading = (ProgressBar) findViewById(R.id.loading);
                loading.animate().withLayer().alpha(0).setDuration(250).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        loading.setVisibility(View.GONE);
                    }
                });
            } else {
                Point currentPosition = mPager.getCurrentItem();
                mAdapter.updateData(mDevicesMap, mDeviceDrawablesMap);
                mAdapter.notifyDataSetChanged();
                mPager.setCurrentItem(currentPosition.y, currentPosition.x, false);
                mPageDots.onPageSelected(currentPosition.y, currentPosition.x);
            }
        }
    }

    private class CommandSender extends AsyncTask<String, Void, Boolean> {

        private String mDsn;
        private String mPropertyName;
        private boolean mPropertyState;

        public CommandSender(String dsn, String propertyName, boolean propertyState) {
            mDsn = dsn;
            mPropertyName = propertyName;
            mPropertyState = propertyState;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            return sendDeviceControlMessage(mDsn, mPropertyName, mPropertyState);
        }

        @Override
        protected void onPostExecute(Boolean success) {
                Intent confirm = new Intent(MainActivity.this, ConfirmationActivity.class);
                confirm.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                        success ? ConfirmationActivity.SUCCESS_ANIMATION : ConfirmationActivity.FAILURE_ANIMATION);
                confirm.putExtra(ConfirmationActivity.EXTRA_MESSAGE,
                        success ? "Command sent" : "Failed to send command");
                startActivity(confirm);
        }
    }

    @Override
    public void onPropertyToggled(String deviceDsn, String propertyName, boolean propertyState) {
        new CommandSender(deviceDsn, propertyName, propertyState).execute();
    }
}
