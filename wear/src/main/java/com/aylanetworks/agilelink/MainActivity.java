package com.aylanetworks.agilelink;

import android.content.Intent;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
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

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

public class MainActivity extends WearableActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        DataApi.DataListener,
        PropertyListAdapter.OnPropertyToggleListener,
        PropertyListView.ScrollStatusListener {

    private static final String DEVICE_NAME = "device_name";
    private static final String DEVICE_DSN = "device_dsn";
    private static final String DEVICE_STATUS = "device_status";
    private static final String DEVICE_PROPERTIES = "device_properties";
    private static final String DEVICE_CONTROL_MSG_URI = "/device_control";

    private GoogleApiClient mGoogleApiClient;
    private LinearLayout mSending;
    private GridViewPager mPager;
    private DotsPageIndicator mPageDots;
    private DevicesGridAdapter mAdapter;
    private DismissOverlayView mDismissOverlay;
    private GestureDetector mDetector;
    private Handler mHandler;

    private String mHandheldNode = "";
    private int mGridPagerScrollState = GridViewPager.SCROLL_STATE_IDLE;
    private boolean mPropertyListScrollIdle = false;
    private boolean mPendingDataUpdate = false;

    private TreeMap<String, DeviceHolder> mDevicesMap = new TreeMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setAmbientEnabled();

        mHandler = new Handler();
        mPager = (GridViewPager) findViewById(R.id.pager);
        mPageDots = (DotsPageIndicator) findViewById(R.id.page_dots);
        mSending = (LinearLayout) findViewById(R.id.sending);
        mSending.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSendingView();
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

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
        mPager.setOnPageChangeListener(new GridViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int row, int column, float rowOffset, float columnOffset, int rowOffsetPixels, int columnOffsetPixels) {
            }

            @Override
            public void onPageSelected(int row, int column) {
                mPageDots.onPageSelected(column, row);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                mGridPagerScrollState = state;

                if (mPendingDataUpdate &&
                        state == GridViewPager.SCROLL_STATE_IDLE &&
                        mPropertyListScrollIdle) {
                    mPendingDataUpdate = false;
                    updateGridAnchored();
                }
            }
        });
    }

    private boolean sendDeviceControlMessage(String dsn, String propertyName, boolean newStatus) {
        byte[] cmdArray = (dsn + "/" + propertyName + "/" + (newStatus ? "1" : "0")).getBytes();

        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, mHandheldNode,
                DEVICE_CONTROL_MSG_URI, cmdArray).await();
        return result.getStatus().isSuccess();
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

    private void updateAllDevicesData() {
        DataItemBuffer dataBuffer = Wearable.DataApi.getDataItems(mGoogleApiClient).await();
        for (DataItem deviceDataItem : dataBuffer) {
            updateDeviceData(deviceDataItem);
        }
        dataBuffer.release();
    }

    private String getDataItemDeviceDsn(DataItem deviceDataItem) {
        final DataMap deviceMap = DataMapItem.fromDataItem(deviceDataItem).getDataMap();
        String dsn = deviceMap.getString(DEVICE_DSN);

        if (dsn == null || !deviceDataItem.getUri().getPath().equals("/" + dsn)) {
            return null;
        }

        return dsn;
    }

    private void updateDeviceData(DataItem deviceDataItem) {
        final DataMap deviceMap = DataMapItem.fromDataItem(deviceDataItem).getDataMap();
        String dsn = deviceMap.getString(DEVICE_DSN);

        if (dsn == null || !deviceDataItem.getUri().getPath().equals("/" + dsn)) {
            return;
        }

        String name = deviceMap.getString(DEVICE_NAME);
        String status = deviceMap.getString(DEVICE_STATUS);
        final DeviceHolder deviceHolder = new DeviceHolder(name, dsn, status);

        Gson gson = new Gson();
        ArrayList<DevicePropertyHolder> propertyHolders = gson.fromJson(deviceMap.getString(DEVICE_PROPERTIES),
                new TypeToken<ArrayList<DevicePropertyHolder>>(){}.getType());

        for (DevicePropertyHolder holder : propertyHolders) {
            deviceHolder.addBooleanProperty(holder);
        }

        mDevicesMap.put(dsn, deviceHolder);
    }

    @Override
    public void onConnected(Bundle bundle) {
        new DataLoader(null).execute();
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
        DataEvent[] dataEvents = new DataEvent[dataEventBuffer.getCount()];
        for (int i = 0; i < dataEventBuffer.getCount(); i++) {
            dataEvents[i] = dataEventBuffer.get(i).freeze();
        }
        dataEventBuffer.release();

        new DataLoader(dataEvents).execute();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }

    private class DataLoader extends AsyncTask<Void, Void, Integer> {

        private DataEvent[] mDataEvents;

        public DataLoader(DataEvent[] dataEvents) {
            mDataEvents = dataEvents;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            if (mDataEvents != null) {
                for (DataEvent event : mDataEvents) {
                    DataItem dataItem = event.getDataItem();
                    if (event.getType() == DataEvent.TYPE_DELETED) {
                        String dsn = getDataItemDeviceDsn(dataItem);
                        if (dsn != null && mDevicesMap.containsKey(dsn)) {
                            mDevicesMap.remove(dsn);
                        }
                    } else if (event.getType() == DataEvent.TYPE_CHANGED) {
                        updateDeviceData(dataItem);
                    }
                }
            } else {
                updateAllDevicesData();
            }

            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (mAdapter == null) {
                mAdapter = new DevicesGridAdapter(MainActivity.this, getFragmentManager(), mDevicesMap);
                mPager.setAdapter(mAdapter);
                mPageDots.setPager(mPager);

                mPager.animate().withLayer().alpha(1).setDuration(250).start();
                final ProgressBar loading = (ProgressBar) findViewById(R.id.loading);
                loading.animate().withLayer().alpha(0).setDuration(250).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        loading.setVisibility(View.GONE);
                    }
                });
            } else {
                mAdapter.updateData(mDevicesMap);

                if (mGridPagerScrollState == GridViewPager.SCROLL_STATE_IDLE &&
                        mPropertyListScrollIdle) {
                    updateGridAnchored();
                } else {
                    mPendingDataUpdate = true;
                }

                Log.e("AMAPW", "DATA LOADER HIDE VIEW");
                hideSendingView();
            }
        }
    }

    private void updateGridAnchored() {
        Point gridPosition = mPager.getCurrentItem();
        DeviceFragment currentFragment = (DeviceFragment) mAdapter.findExistingFragment(gridPosition.y, gridPosition.x);
        int listPosition = currentFragment.getPropertyListViewPosition();

        mAdapter.notifyDataSetChanged();

        if (mAdapter.getRowCount() != 0) {
            if (gridPosition.y < mAdapter.getRowCount()) {
                mPager.setCurrentItem(gridPosition.y, gridPosition.x, false);
                if (gridPosition.x != 0) {
                    currentFragment = (DeviceFragment) mAdapter.findExistingFragment(gridPosition.y, gridPosition.x);
                    currentFragment.setPropertyListViewPosition(listPosition);
                }
                mPageDots.onPageSelected(gridPosition.x, gridPosition.y);
            } else {
                mPager.setCurrentItem(mAdapter.getRowCount() - 1, gridPosition.x, false);
                mPageDots.onPageSelected(gridPosition.x, mAdapter.getRowCount() - 1);
            }
        }
    }

    private void hideSendingView() {
        if (mSending.getAlpha() == 1) {
            mSending.animate().withLayer().alpha(0).setDuration(250).withEndAction(new Runnable() {
                @Override
                public void run() {
                    mSending.setVisibility(View.GONE);
                }
            });
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
        protected void onPreExecute() {
            mSending.setVisibility(View.VISIBLE);
            mSending.animate().withLayer().alpha(1).setDuration(250).start();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            return sendDeviceControlMessage(mDsn, mPropertyName, mPropertyState);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (!success) {
                new DataLoader(null).execute();
                Toast.makeText(MainActivity.this, "Failed to send command", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onPropertyToggled(String deviceDsn, String propertyName, boolean propertyState) {
        new CommandSender(deviceDsn, propertyName, propertyState).execute();
    }

    @Override
    public void onScrollStatusChanged(PropertyListView.ScrollStatus status) {
        Point gridPosition = mPager.getCurrentItem();
        int rowCount = mAdapter.getRowCount();

        switch (status) {
            case SCROLL_NOT_IDLE:
            case SCROLL_IDLE:
                mPropertyListScrollIdle = status == PropertyListView.ScrollStatus.SCROLL_IDLE;

                if (mPendingDataUpdate &&
                        mGridPagerScrollState == GridViewPager.SCROLL_STATE_IDLE &&
                        mPropertyListScrollIdle) {
                    mPendingDataUpdate = false;
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            updateGridAnchored();
                        }
                    }, 150); // Delay is workaround for Android platform bug where SCROLL_STATE_SETTLING is never fired
                }
                break;

            case SHOW_NEXT_ROW_HINT:
                if (gridPosition.y < rowCount - 1) {
                    DeviceFragment currentFragment = (DeviceFragment) mAdapter.findExistingFragment(gridPosition.y, gridPosition.x);
                    currentFragment.showRowHint(true);
                }
                break;

            case SHOW_PREVIOUS_ROW_HINT:
                if (gridPosition.y > 0) {
                    DeviceFragment currentFragment = (DeviceFragment) mAdapter.findExistingFragment(gridPosition.y, gridPosition.x);
                    currentFragment.showRowHint(false);
                }
                break;

            case HIDE_NEXT_ROW_HINT:
                if (gridPosition.y < rowCount - 1) {
                    DeviceFragment currentFragment = (DeviceFragment) mAdapter.findExistingFragment(gridPosition.y, gridPosition.x);
                    currentFragment.hideRowHint();
                }
                break;

            case HIDE_PREVIOUS_ROW_HINT:
                if (gridPosition.y > 0) {
                    DeviceFragment currentFragment = (DeviceFragment) mAdapter.findExistingFragment(gridPosition.y, gridPosition.x);
                    currentFragment.hideRowHint();
                }
                break;

            case ADJUST_NEXT_ROW:
                if (gridPosition.y < rowCount - 1) {
                    mPager.setCurrentItem(gridPosition.y + 1, 0, true);
                }
                break;

            case ADJUST_PREVIOUS_ROW:
                if (gridPosition.y > 0) {
                    mPager.setCurrentItem(gridPosition.y - 1, 0, true);
                }
                break;
        }
    }
}
