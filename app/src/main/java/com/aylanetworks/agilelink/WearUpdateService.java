package com.aylanetworks.agilelink;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.android.volley.Response;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.AccountSettings;
import com.aylanetworks.agilelink.framework.ViewModel;
import com.aylanetworks.aylasdk.AylaDatapoint;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaDeviceManager;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.aylasdk.AylaSessionManager;
import com.aylanetworks.aylasdk.auth.AylaAuthorization;
import com.aylanetworks.aylasdk.auth.CachedAuthProvider;
import com.aylanetworks.aylasdk.change.Change;
import com.aylanetworks.aylasdk.change.ListChange;
import com.aylanetworks.aylasdk.change.PropertyChange;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WearUpdateService extends Service implements
        AylaDevice.DeviceChangeListener,
        AylaDeviceManager.DeviceManagerListener,
        AylaSessionManager.SessionManagerListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        CapabilityApi.CapabilityListener,
        MessageApi.MessageListener {

    private static final String DEVICE_NAME = "device_name";
    private static final String DEVICE_DSN = "device_dsn";
    private static final String DEVICE_STATUS = "device_status";
    private static final String DEVICE_PROPERTIES = "device_properties";

    private static final String DEVICE_CONTROL_MSG_URI = "/device_control";
    private static final String DEVICE_CONTROL_RESULT_MSG_URI = "/device_control_result";
    private static final String DEVICE_CONTROL_START_CONNECTION = "/device_control_start_connection";
    private static final String DEVICE_CONTROL_END_CONNECTION = "/device_control_end_connection";
    private static final String DEVICE_CONTROL_CONNECTION_STARTED = "/device_control_connection_started";

    private static final String INTENT_ACTION_STOP_SERVICE = "com.aylanetworks.agilelink.STOP_SERVICE";

    private ServiceCommandReceiver mServiceCommandReceiver;
    private PowerManager.WakeLock mWakeLock;
    private GoogleApiClient mGoogleApiClient;
    private String mWearableNode;
    private String mLocalNode;

    @Override
    public void onCreate() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AMAP5 Wear Background Service");

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

        mServiceCommandReceiver = new ServiceCommandReceiver();
        registerReceiver(mServiceCommandReceiver, new IntentFilter(INTENT_ACTION_STOP_SERVICE));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void startForegroundService() {
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle("AMAP5 Android Wear Service");
        builder.setContentText("Notification required to keep service in foreground. Press notification to stop service.");
        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setContentIntent(PendingIntent.getBroadcast(this, 0, new Intent(INTENT_ACTION_STOP_SERVICE), 0));
        startForeground(999, builder.build());
    }

    private void initAylaServices() {
        if (AMAPCore.sharedInstance() == null) {
            AMAPCore.initialize(MainActivity.getAppParameters(this), this);
        }

        CachedAuthProvider cachedProvider = CachedAuthProvider.getCachedProvider(this);
        if (cachedProvider != null) {
            AylaNetworks.sharedInstance().getLoginManager().signIn(cachedProvider,
                    AMAPCore.sharedInstance().getSessionParameters().sessionName,
                    new Response.Listener<AylaAuthorization>() {
                        @Override
                        public void onResponse(AylaAuthorization response) {
                            CachedAuthProvider.cacheAuthorization(WearUpdateService.this, response);
                            if (AgileLinkApplication.getsInstance().shouldResumeAylaNetworks(WearUpdateService.this.getClass().getName())) {
                                AylaNetworks.sharedInstance().onResume();
                            }
                            AMAPCore.sharedInstance().fetchAccountSettings(new AccountSettings.AccountSettingsCallback());

                            startListening();
                        }
                    },
                    new ErrorListener() {
                        @Override
                        public void onErrorResponse(AylaError error) {
                            //
                        }
                    });
        }
    }

    private void destroyAylaServices() {
        stopListening();

        AylaDeviceManager dm = AMAPCore.sharedInstance().getDeviceManager();
        if (dm != null && AgileLinkApplication.getsInstance().canPauseAylaNetworks(getClass().getName())) {
            dm.stopPolling();
            AylaNetworks.sharedInstance().onPause();
        }
        removeAllDevicesFromDataStore();
    }

    private String getDeviceStatus(AylaDevice device) {
        String status;
        if (device.getConnectionStatus() == AylaDevice.ConnectionStatus.Online) {
            if (device.isLanModeActive()) {
                status = "LAN Mode";
            } else {
                status = "Online";
            }
        } else {
            status = "Offline";
        }

        return status;
    }

    private void updateWearDataForDevice(AylaDevice device) {
        if (!device.isGateway()) {
            ViewModel deviceModel = AMAPCore.sharedInstance().getSessionParameters().viewModelProvider
                    .viewModelForDevice(device);
            if (deviceModel == null) {
                return;
            }

            PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/" + device.getDsn());
            DataMap deviceMap = putDataMapReq.getDataMap();
            deviceMap.putString(DEVICE_NAME, device.getFriendlyName());
            deviceMap.putString(DEVICE_DSN, device.getDsn());
            deviceMap.putString(DEVICE_STATUS, getDeviceStatus(device));

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
                return;
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
                    // Log.e("AMAPW", "DELIVERY: " + dataItemResult.getStatus().isSuccess());
                }
            });
        }
    }

    private void startListening() {
        AylaSessionManager sessionManager = AMAPCore.sharedInstance().getSessionManager();
        if (sessionManager != null) {
            sessionManager.addListener(this);
        }

        AylaDeviceManager deviceManager = AMAPCore.sharedInstance().getDeviceManager();
        if (deviceManager != null) {
            deviceManager.addListener(this);

            for (AylaDevice device : deviceManager.getDevices()) {
                updateWearDataForDevice(device);
                device.addListener(this);
            }
        }

        sendMessageToWearable(DEVICE_CONTROL_CONNECTION_STARTED, "");
    }

    private void stopListening() {
        AylaSessionManager sessionManager = AMAPCore.sharedInstance().getSessionManager();
        if (sessionManager != null) {
            sessionManager.removeListener(this);
        }

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
                updateWearDataForDevice(device);
            }
        } else if (change.getType() == Change.ChangeType.Field) {
            updateWearDataForDevice(device);
        }
    }

    private void removeDeviceFromDataStore(final Set<String> dsns) {
        if (dsns == null || dsns.isEmpty()) {
            return;
        }

        for (String dsn : dsns) {
            if (dsn.equals("")) {
                continue;
            }

            Wearable.DataApi.deleteDataItems(mGoogleApiClient,
                    new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).authority(mLocalNode).path("/" + dsn).build());
        }
    }

    private void removeAllDevicesFromDataStore() {
        Wearable.DataApi.deleteDataItems(mGoogleApiClient,
                new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).authority(mLocalNode).path("/").build(),
                DataApi.FILTER_PREFIX);
    }

    @Override
    public void deviceListChanged(ListChange change) {
        if (change != null) {
            List addedItems = change.getAddedItems();
            if (addedItems != null) {
                for (Object item : addedItems) {
                    if (item instanceof AylaDevice) {
                        AylaDevice device = (AylaDevice) item;
                        updateWearDataForDevice(device);
                        device.addListener(this);
                    }
                }
            }

            removeDeviceFromDataStore(change.getRemovedIdentifiers());
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (TextUtils.equals(messageEvent.getPath(), DEVICE_CONTROL_MSG_URI)) {
            if (!mWakeLock.isHeld()) {
                mWakeLock.acquire(5 * 1000);
            }

            String cmd = new String(messageEvent.getData());
            String[] cmdComponents = cmd.split("/");
            if (cmdComponents.length < 3) {
                return;
            }

            String dsn = cmdComponents[0];
            String propertyName = cmdComponents[1];
            String propertyState = cmdComponents[2];

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
                    sendMessageToWearable(DEVICE_CONTROL_RESULT_MSG_URI, "1");
                }
            }, new ErrorListener() {
                @Override
                public void onErrorResponse(AylaError error) {
                    sendMessageToWearable(DEVICE_CONTROL_RESULT_MSG_URI, "0");
                }
            });
        } else if (TextUtils.equals(messageEvent.getPath(), DEVICE_CONTROL_START_CONNECTION)) {
            if (!mWakeLock.isHeld()) {
                mWakeLock.acquire(5 * 1000);
            }

            initAylaServices();
        } else if (TextUtils.equals(messageEvent.getPath(), DEVICE_CONTROL_END_CONNECTION)) {
            if (!mWakeLock.isHeld()) {
                mWakeLock.acquire(3 * 1000);
            }

            destroyAylaServices();
        }
    }

    private void sendMessageToWearable(String path, String data) {
        PendingResult<MessageApi.SendMessageResult> pendingResult = Wearable.MessageApi.sendMessage(mGoogleApiClient, mWearableNode,
                path, data.getBytes());
        pendingResult.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                // Log.e("AMAPW", "RESULT: " + sendMessageResult.getStatus().isSuccess());
            }
        });
    }

    private void getWearableNode() {
        PendingResult<CapabilityApi.GetCapabilityResult> result = Wearable.CapabilityApi.getCapability(
                mGoogleApiClient,
                "ayla_device_control_result",
                CapabilityApi.FILTER_REACHABLE);

        result.setResultCallback(new ResultCallback<CapabilityApi.GetCapabilityResult>() {
            @Override
            public void onResult(@NonNull CapabilityApi.GetCapabilityResult getCapabilityResult) {
                Set<Node> connectedNodes = getCapabilityResult.getCapability().getNodes();
                for (Node node : connectedNodes) {
                    if (node.isNearby()) {
                        mWearableNode = node.getId();
                        startForegroundService();
                        return;
                    }
                }
            }
        });
    }

    private void getLocalNode() {
        PendingResult<NodeApi.GetLocalNodeResult> pendingResult = Wearable.NodeApi.getLocalNode(mGoogleApiClient);
        pendingResult.setResultCallback(new ResultCallback<NodeApi.GetLocalNodeResult>() {
            @Override
            public void onResult(@NonNull NodeApi.GetLocalNodeResult getLocalNodeResult) {
                mLocalNode = getLocalNodeResult.getNode().getId();
            }
        });
    }

    @Override
    public void onDestroy() {
        destroyAylaServices();
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        Wearable.CapabilityApi.removeCapabilityListener(mGoogleApiClient, this, "ayla_device_control_result");
        mGoogleApiClient.disconnect();

        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }

        try {
            unregisterReceiver(mServiceCommandReceiver);
        } catch (Exception e) {
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        getWearableNode();
        getLocalNode();
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.CapabilityApi.addCapabilityListener(mGoogleApiClient, this, "ayla_device_control_result");
    }

    @Override
    public void deviceLanStateChanged(AylaDevice device, boolean lanModeEnabled) {
        updateWearDataForDevice(device);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        stopSelf();
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
    public void sessionClosed(String sessionName, AylaError error) {
        stopSelf();
    }

    @Override
    public void authorizationRefreshed(String sessionName, AylaAuthorization authorization) {
    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        getWearableNode();
    }

    private class ServiceCommandReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(INTENT_ACTION_STOP_SERVICE)) {
                stopSelf();
            }
        }
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
