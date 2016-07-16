package com.aylanetworks.agilelink;

import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.DismissOverlayView;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.WearableListView;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
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
import com.google.android.gms.wearable.MessageEvent;
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
        PropertyListAdapter.PropertyToggleListener,
        WearableListView.OnScrollListener,
        WearableListView.ClickListener,
        MessageApi.MessageListener {

    private static final int SENDING_TIMEOUT_DISMISS_MESSAGE_MS = 5 * 1000;
    private static final int DEVICE_CONTROL_CONNECTION_CHECK_TIMEOUT_MS = 5 * 1000;

    private static final String DEVICE_NAME = "device_name";
    private static final String DEVICE_DSN = "device_dsn";
    private static final String DEVICE_STATUS = "device_status";
    private static final String DEVICE_PROPERTIES = "device_properties";

    private static final String DEVICE_CONTROL_MSG_URI = "/device_control";
    private static final String DEVICE_CONTROL_RESULT_MSG_URI = "/device_control_result";
    private static final String DEVICE_CONTROL_CONNECTION_CHECK = "/device_control_connection_check";
    private static final String DEVICE_CONTROL_CONNECTION_RESULT = "/device_control_connection_result";

    private GoogleApiClient mGoogleApiClient;
    private LinearLayout mSending;
    private GridViewPager mPager;
    private DotsPageIndicator mPageDots;
    private GestureDetector mGestureDetector;
    private DevicesGridAdapter mAdapter;
    private Handler mHandler;

    private String mHandheldNode = "";
    private boolean mGridPagerIdle = true;
    private boolean mPropertyListIdle = true;
    private boolean mPendingDataUpdate = false;
    private int mPropertyListViewCentralPosition = 0;

    public enum ConnectionStatus {CONNECTING, CONNECTED, NOT_CONNECTED}
    private ConnectionStatus mDeviceControlConnection = ConnectionStatus.CONNECTING;

    private TreeMap<String, DeviceHolder> mDevicesMap = new TreeMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();
        mPager = (GridViewPager) findViewById(R.id.pager);
        mPager.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mPageDots = (DotsPageIndicator) findViewById(R.id.page_dots);
        mSending = (LinearLayout) findViewById(R.id.sending);
        mSending.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mSending.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSendingView(true);
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        final DismissOverlayView dismissOverlay = (DismissOverlayView) findViewById(R.id.dismiss_overlay);
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent ev) {
                dismissOverlay.show();
            }
        });
        mGestureDetector.setIsLongpressEnabled(true);

        mPager.setOnPageChangeListener(new GridViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int row, int column, float rowOffset, float columnOffset, int rowOffsetPixels, int columnOffsetPixels) {
            }

            @Override
            public void onPageSelected(int row, int column) {
                // mPageDots.onPageSelected(column, row);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                mGridPagerIdle = state == GridViewPager.SCROLL_STATE_IDLE;

                if (mPendingDataUpdate) {
                    updateGridAnchored();
                }
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return mGestureDetector.onTouchEvent(ev) || super.dispatchTouchEvent(ev);
    }

    private Runnable mDeviceControlConnectionCheckTimeOut = new Runnable() {
        @Override
        public void run() {
            mDeviceControlConnection = ConnectionStatus.NOT_CONNECTED;
            updateGridAnchored();
        }
    };

    public ConnectionStatus getDeviceControlConnectionStatus() {
        return mDeviceControlConnection;
    }

    private void sendDeviceControlConnectionCheckMessage() {
        mDeviceControlConnection = ConnectionStatus.CONNECTING;
        PendingResult<MessageApi.SendMessageResult> pendingResult = Wearable.MessageApi.sendMessage(mGoogleApiClient, mHandheldNode,
                DEVICE_CONTROL_CONNECTION_CHECK, "".getBytes());
        pendingResult.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                if (!sendMessageResult.getStatus().isSuccess()) {
                    mHandler.removeCallbacks(mDeviceControlConnectionCheckTimeOut);
                    mDeviceControlConnection = ConnectionStatus.NOT_CONNECTED;
                    updateGridAnchored();
                }
            }
        });

        mHandler.removeCallbacks(mDeviceControlConnectionCheckTimeOut);
        mHandler.postDelayed(mDeviceControlConnectionCheckTimeOut, DEVICE_CONTROL_CONNECTION_CHECK_TIMEOUT_MS);
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
                        sendDeviceControlConnectionCheckMessage();
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
        Wearable.MessageApi.addListener(mGoogleApiClient, this);

        getHandheldNode();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.e("AMAPW", "onConnectionSuspended: " + cause);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Toast.makeText(this, result.getErrorMessage(), Toast.LENGTH_LONG).show();
        finish();
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
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (TextUtils.equals(messageEvent.getPath(), DEVICE_CONTROL_RESULT_MSG_URI)) {
            String result = new String(messageEvent.getData());
            if (result.equals("0")) {
                updateGridAnchored();
                Toast.makeText(MainActivity.this, "Failed to execute device command", Toast.LENGTH_SHORT).show();
            }

            hideSendingView(false);
        } else if (TextUtils.equals(messageEvent.getPath(), DEVICE_CONTROL_CONNECTION_RESULT)) {
            mHandler.removeCallbacks(mDeviceControlConnectionCheckTimeOut);
            mDeviceControlConnection = ConnectionStatus.CONNECTED;
            updateGridAnchored();
        }
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

                mPager.setVisibility(View.VISIBLE);
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
                updateGridAnchored();

                hideSendingView(false);
            }
        }
    }

    private void updateGridAnchored() {
        if (!mGridPagerIdle || !mPropertyListIdle) {
            mPendingDataUpdate = true;
            return;
        }

        if (mPager == null || mAdapter == null) {
            return;
        }

        mPendingDataUpdate = false;
        Point gridPosition = mPager.getCurrentItem();
        int listPosition = mPropertyListViewCentralPosition;

        mAdapter.notifyDataSetChanged();

        if (mAdapter.getRowCount() > 0) {
            if (gridPosition.y < mAdapter.getRowCount()) {
                mPager.setCurrentItem(gridPosition.y, gridPosition.x, false);
                if (gridPosition.x == 1) {
                    DeviceFragment currentFragment = (DeviceFragment) mAdapter.findExistingFragment(gridPosition.y, gridPosition.x);
                    currentFragment.setPropertyListViewPosition(listPosition);
                }
            } else {
                mPager.setCurrentItem(mAdapter.getRowCount() - 1, 0, false);
            }
        }
    }

    private void hideSendingView(boolean manual) {
        if (mSending.getAlpha() == 1 && mSending.getVisibility() == View.VISIBLE) {
            mHandler.removeCallbacks(mShowSendingDismissMessage);
            if (manual) {
                updateGridAnchored();
            }

            mSending.animate().withLayer().alpha(0).setDuration(250).withEndAction(new Runnable() {
                @Override
                public void run() {
                    mSending.setVisibility(View.GONE);

                    TextView sendingDismiss = (TextView) findViewById(R.id.sending_dismiss);
                    sendingDismiss.setVisibility(View.INVISIBLE);
                }
            });
        }
    }

    private Runnable mShowSendingDismissMessage = new Runnable() {
        @Override
        public void run() {
            TextView sendingDismiss = (TextView) findViewById(R.id.sending_dismiss);
            sendingDismiss.setVisibility(View.VISIBLE);
        }
    };

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
            mHandler.removeCallbacks(mShowSendingDismissMessage);
            mHandler.postDelayed(mShowSendingDismissMessage, SENDING_TIMEOUT_DISMISS_MESSAGE_MS);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            return sendDeviceControlMessage(mDsn, mPropertyName, mPropertyState);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (!success) {
                updateGridAnchored();
                Toast.makeText(MainActivity.this, "Failed to communicate with handheld device", Toast.LENGTH_SHORT).show();
                hideSendingView(false);
            }
        }
    }

    @Override
    public void onPropertyToggled(String deviceDsn, String propertyName, boolean propertyState) {
        new CommandSender(deviceDsn, propertyName, propertyState).execute();
    }

    private Runnable mUpdateGridAnchored = new Runnable() {
        @Override
        public void run() {
            updateGridAnchored();
        }
    };

    @Override
    public void onScrollStateChanged(int state) {
        mPropertyListIdle = state == RecyclerView.SCROLL_STATE_IDLE;

        if (mPendingDataUpdate) {
            // Delay is workaround for Android platform bug where SCROLL_STATE_SETTLING is never fired
            mHandler.removeCallbacks(mUpdateGridAnchored);
            mHandler.postDelayed(mUpdateGridAnchored, 150);
        }
    }

    @Override
    public void onClick(WearableListView.ViewHolder viewHolder) {
        DevicePropertyHolder propertyHolder = (DevicePropertyHolder) viewHolder.itemView.getTag();
        if (propertyHolder instanceof RowPropertyHolder) {
            RowPropertyHolder.RowType type = ((RowPropertyHolder) propertyHolder).mRowType;
            Point gridPosition = mPager.getCurrentItem();
            if (type == RowPropertyHolder.RowType.TOP) {
                if (gridPosition.y > 0) {
                    mPager.setCurrentItem(gridPosition.y - 1, 0, true);
                }
            } else if (type == RowPropertyHolder.RowType.BOTTOM) {
                int rowCount = mAdapter.getRowCount();
                if (gridPosition.y < rowCount - 1) {
                    mPager.setCurrentItem(gridPosition.y + 1, 0, true);
                }
            }
        } else {
            PropertyListAdapter.ItemViewHolder itemHolder = (PropertyListAdapter.ItemViewHolder) viewHolder;
            Switch propertyToggle = itemHolder.mReadWriteProperty;
            if (propertyToggle.getVisibility() == View.VISIBLE) {
                propertyToggle.toggle();
            }
        }
    }

    @Override
    public void onCentralPositionChanged(int position) {
        mPropertyListViewCentralPosition = position;
    }

    @Override
    public void onTopEmptyRegionClick() {
    }
    @Override
    public void onScroll(int scroll) {
    }
    @Override
    public void onAbsoluteScrollChange(int scroll) {
    }
}
