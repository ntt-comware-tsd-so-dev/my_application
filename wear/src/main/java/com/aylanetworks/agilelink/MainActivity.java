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

    /**
     * Maximum amount of time to wait for the service to initialize on the handheld device
     */
    private static final int CONNECTION_START_TIMEOUT = 5 * 1000;
    /**
     * Maximum amount of time to wait for the command to be delivered to the handheld device
     */
    private static final int SENDING_TIMEOUT_DISMISS_MESSAGE_MS = 5 * 1000;

    private static final String DEVICE_NAME = "device_name";
    private static final String DEVICE_DSN = "device_dsn";
    private static final String DEVICE_STATUS = "device_status";
    private static final String DEVICE_PROPERTIES = "device_properties";

    private static final String DEVICE_CONTROL_MSG_URI = "/device_control";
    private static final String DEVICE_CONTROL_RESULT_MSG_URI = "/device_control_result";
    private static final String DEVICE_CONTROL_START_CONNECTION = "/device_control_start_connection";
    private static final String DEVICE_CONTROL_END_CONNECTION = "/device_control_end_connection";
    private static final String DEVICE_CONTROL_CONNECTION_STARTED = "/device_control_connection_started";

    private GoogleApiClient mGoogleApiClient;
    private LinearLayout mSending;
    private TextView mErrorView;
    private GridViewPager mPager;
    private GestureDetector mGestureDetector;
    private DevicesGridAdapter mAdapter;
    private Handler mHandler;

    private String mHandheldNode = "";
    private int mPropertyListViewCentralPosition = 0;

    /**
     * Idle if the user is currently not interacting with the pager / list
     */
    private boolean mGridPagerIdle = true;
    private boolean mPropertyListIdle = true;

    /**
     * Whether the UI should be refreshed after the user is done interacting with it
     */
    private boolean mPendingDataUpdate = false;

    /**
     * Map of Ayla devices, keyed by the DSN
     */
    private TreeMap<String, DeviceHolder> mDevicesMap = new TreeMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();

        // Error view will be displayed when the connection takes too long to establish
        mErrorView = (TextView) findViewById(R.id.load_error);
        mErrorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // In case of connection timeout, press to retry
                sendDeviceControlStartConnectionMessage();
                hideError();
            }
        });

        // This is the 2D grid pager
        mPager = (GridViewPager) findViewById(R.id.pager);

        // Loading view will be displayed while the wearable is sending a command to the handheld
        mSending = (LinearLayout) findViewById(R.id.sending);
        mSending.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Press to hide view
                hideSendingView(true);
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        // Long press anywhere and a menu will display to exit the app
        final DismissOverlayView dismissOverlay = (DismissOverlayView) findViewById(R.id.dismiss_overlay);
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent ev) {
                dismissOverlay.show();
            }
        });
        mGestureDetector.setIsLongpressEnabled(true);

        // The pager listener is used to only update the UI when the user is not currently
        // interacting with it
        mPager.setOnPageChangeListener(new GridViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int row, int column, float rowOffset, float columnOffset, int rowOffsetPixels, int columnOffsetPixels) {
            }
            @Override
            public void onPageSelected(int row, int column) {
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
        // Intercept all touch events to detect long presses (for exit menu)
        return mGestureDetector.onTouchEvent(ev) || super.dispatchTouchEvent(ev);
    }

    private Runnable mServiceConnectionTimedOut = new Runnable() {
        @Override
        public void run() {
            showError("Connection took too long to start.\nTap to retry.");
        }
    };

    /**
     * Sends message to the handheld device to initialize Ayla services and start Ayla session
     * Called when the wearable activity starts
     */
    private void sendDeviceControlStartConnectionMessage() {
        PendingResult<MessageApi.SendMessageResult> pendingResult = Wearable.MessageApi.sendMessage(
                mGoogleApiClient,
                mHandheldNode, // Destination node ID (Handheld ID)
                DEVICE_CONTROL_START_CONNECTION, // Message path
                "".getBytes()); // Body data is not used for this message
        pendingResult.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                if (!sendMessageResult.getStatus().isSuccess()) {
                    showError("Failed to communicate with handheld device");
                }
            }
        });

        // Display timeout message after #CONNECTION_START_TIMEOUT if no response has been received
        mHandler.postDelayed(mServiceConnectionTimedOut, CONNECTION_START_TIMEOUT);
    }

    /**
     * Sends message to the handheld device to exit Ayla session and destroy Ayla services
     * Called when the wearable activity exits
     */
    private void sendDeviceControlEndConnectionMessage() {
        PendingResult<MessageApi.SendMessageResult> pendingResult = Wearable.MessageApi.sendMessage(
                mGoogleApiClient,
                mHandheldNode, // Destination node ID (Handheld ID)
                DEVICE_CONTROL_END_CONNECTION, // Message path
                "".getBytes()); // Body data is not used for this message
        pendingResult.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                //
            }
        });
    }

    /**
     * Sends message to the handheld device to control Ayla device properties
     * @param dsn The DSN of the target Ayla device
     * @param propertyName The name of the target property
     * @param newStatus The desired state of the target property
     * @return Whether command delivery to the handheld device was successful
     */
    private boolean sendDeviceControlMessage(String dsn, String propertyName, boolean newStatus) {
        // Format of body data is <device DSN>/<property name>/<1 / 0>
        byte[] cmdArray = (dsn + "/" + propertyName + "/" + (newStatus ? "1" : "0")).getBytes();

        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, mHandheldNode,
                DEVICE_CONTROL_MSG_URI, cmdArray).await();
        return result.getStatus().isSuccess();
    }

    /**
     * Obtains the node ID of the handheld device (Only those with the AMAP application installed)
     */
    private void getHandheldNode() {
        PendingResult<CapabilityApi.GetCapabilityResult> result = Wearable.CapabilityApi.getCapability(
                mGoogleApiClient,
                "ayla_device_control", // Capability declared in the AMAP handheld application
                CapabilityApi.FILTER_REACHABLE); // Only devices that are currently connected

        result.setResultCallback(new ResultCallback<CapabilityApi.GetCapabilityResult>() {
            @Override
            public void onResult(@NonNull CapabilityApi.GetCapabilityResult getCapabilityResult) {
                Set<Node> connectedNodes = getCapabilityResult.getCapability().getNodes();
                for (Node node : connectedNodes) {
                    if (node.isNearby()) {
                        // After finding the handheld device, send a message to it to initialize
                        mHandheldNode = node.getId();
                        sendDeviceControlStartConnectionMessage();
                        return;
                    }
                }

                // If no capable handheld device was found, show error
                showError("Cannot find capable handheld device");
            }
        });
    }

    /**
     * Refresh device data for all Ayla devices from data store
     */
    private void updateAllDevicesData() {
        DataItemBuffer dataBuffer = Wearable.DataApi.getDataItems(mGoogleApiClient).await();
        for (DataItem deviceDataItem : dataBuffer) {
            updateDeviceData(deviceDataItem);
        }
        dataBuffer.release();
    }

    /**
     * Obtains the DSN of the Ayla device data contained in the DataItem
     * @param deviceDataItem The DataItem containing Ayla device data
     * @return The DSN of the Ayla device
     */
    private String getDataItemDeviceDsn(DataItem deviceDataItem) {
        String path = deviceDataItem.getUri().getPath();
        if (path.startsWith("/")) {
            return path.substring(1);
        }

        return null;
    }

    /**
     * Refresh device data for the Ayla device contained in the DataItem
     * @param deviceDataItem DataItem containing data for an Ayla device
     */
    private void updateDeviceData(DataItem deviceDataItem) {
        final DataMap deviceMap = DataMapItem.fromDataItem(deviceDataItem).getDataMap();
        String dsn = deviceMap.getString(DEVICE_DSN);

        // Format for path is /<device DSN>
        if (dsn == null || !deviceDataItem.getUri().getPath().equals("/" + dsn)) {
            return;
        }

        String name = deviceMap.getString(DEVICE_NAME); // Device name
        String status = deviceMap.getString(DEVICE_STATUS); // Device status (Offline/Online/LAN Mode)
        final DeviceHolder deviceHolder = new DeviceHolder(name, dsn, status);

        // Get all device properties and add to device holder for this Ayla device
        Gson gson = new Gson();
        ArrayList<DevicePropertyHolder> propertyHolders = gson.fromJson(deviceMap.getString(DEVICE_PROPERTIES),
                new TypeToken<ArrayList<DevicePropertyHolder>>(){}.getType());

        for (DevicePropertyHolder holder : propertyHolders) {
            deviceHolder.addBooleanProperty(holder);
        }

        // Add device holder to map
        mDevicesMap.put(dsn, deviceHolder);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);

        getHandheldNode();
    }

    @Override
    public void onConnectionSuspended(int cause) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        showError("Failed to connect to Google Play Services");
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        mHandler.removeCallbacks(mServiceConnectionTimedOut);

        // Get changed device data and start refreshing
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
        sendDeviceControlEndConnectionMessage();

        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (TextUtils.equals(messageEvent.getPath(), DEVICE_CONTROL_RESULT_MSG_URI)) {
            // Command has been successfully received by handheld device, get execution status
            String result = new String(messageEvent.getData());
            if (result.equals("0")) { // Command failed to execute
                updateGridAnchored();
                Toast.makeText(MainActivity.this, "Failed to execute device command", Toast.LENGTH_SHORT).show();
            }

            hideSendingView(false); // Hide loading view as the task is done
        } else if (TextUtils.equals(messageEvent.getPath(), DEVICE_CONTROL_CONNECTION_STARTED)) {
            // Ayla services and session has been initialized on the handheld device,
            // start refreshing the data and UI
            mHandler.removeCallbacks(mServiceConnectionTimedOut);
            new DataLoader(null).execute();
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
                        // If DataItem was deleted, remove the corresponding device from the map
                        String dsn = getDataItemDeviceDsn(dataItem);
                        if (dsn != null && mDevicesMap.containsKey(dsn)) {
                            mDevicesMap.remove(dsn);
                        }
                    } else if (event.getType() == DataEvent.TYPE_CHANGED) {
                        updateDeviceData(dataItem);
                    }
                }
            } else {
                // If no DataEvents are provided, refresh data for all devices
                updateAllDevicesData();
            }

            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (mDevicesMap.size() == 0) {
                showError("No devices available");
                return;
            } else {
                hideError();
            }

            if (mAdapter == null) {
                // On first run, initialize the pager
                mAdapter = new DevicesGridAdapter(MainActivity.this, getFragmentManager(), mDevicesMap);
                mPager.setAdapter(mAdapter);
                DotsPageIndicator pageDots = (DotsPageIndicator) findViewById(R.id.page_dots);
                pageDots.setPager(mPager);

                // Show the pager and hide the loading view
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

    private void showError(String message) {
        mErrorView.setText(message);
        mErrorView.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        if (mErrorView.getVisibility() == View.VISIBLE) {
            mErrorView.setVisibility(View.GONE);
        }
    }

    /**
     * Refresh the pager and list UI and restore the previous pager and list positions
     */
    private void updateGridAnchored() {
        // Don't refresh right now if the user is currently interacting with the UI,
        // do it after they're done
        if (!mGridPagerIdle || !mPropertyListIdle) {
            mPendingDataUpdate = true;
            return;
        }

        if (mPager == null || mAdapter == null) {
            return;
        }

        mPendingDataUpdate = false;
        // Get current pager and list positions
        Point gridPosition = mPager.getCurrentItem();
        int listPosition = mPropertyListViewCentralPosition;

        // Refresh pager and list data
        mAdapter.notifyDataSetChanged();

        // Restore previous pager and list positions (If index is out of bounds, set to the last item)
        if (mAdapter.getRowCount() > 0) {
            if (gridPosition.y < mAdapter.getRowCount()) {
                mPager.setCurrentItem(gridPosition.y, gridPosition.x, false);
                if (gridPosition.x == 1) { // Restore list position only if fragment is in focus
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
            // If the user pressed the loading view to hide it explicitly, the command
            // was probably taking too long to send so refresh the UI
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
            // The clicked row was a pager navigation arrow, switch to the new page accordingly
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
            // Clicked row was a property row, send command
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
