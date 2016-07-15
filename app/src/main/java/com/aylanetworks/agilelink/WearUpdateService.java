package com.aylanetworks.agilelink;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
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
import com.google.android.gms.wearable.CapabilityApi;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class WearUpdateService extends Service implements AylaDevice.DeviceChangeListener,
        AylaDeviceManager.DeviceManagerListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        MessageApi.MessageListener {

    private static final String DEVICE_NAME = "device_name";
    private static final String DEVICE_DSN = "device_dsn";
    private static final String DEVICE_STATUS = "device_status";
    private static final String DEVICE_PROPERTIES = "device_properties";

    private static final String DEVICE_CONTROL_MSG_URI = "/device_control";
    private static final String DEVICE_CONTROL_RESULT_MSG_URI = "/device_control_result";
    private static final String DEVICE_CONTROL_CONNECTION_CHECK = "/device_control_connection_check";
    private static final String DEVICE_CONTROL_CONNECTION_RESULT = "/device_control_connection_result";

    private PowerManager.WakeLock mWakeLock;
    private GoogleApiClient mGoogleApiClient;
    private String mWearableNode;

    private Boolean[] TEST_A_STATUS = {true, false, true};
    private Boolean[] TEST_B_STATUS = {false, true, false};

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

        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle("AMAP5 Android Wear Service");
        builder.setContentText("Notification required to keep service in foreground");
        builder.setSmallIcon(R.drawable.ic_launcher);
        startForeground(999, builder.build());
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
        Queue<PutDataRequest> dataRequestQueue = new LinkedList<>();

        if (device == null) {
            dataRequestQueue.offer(getTestDeviceA());
            dataRequestQueue.offer(getTestDeviceB());
        } else if (!device.isGateway()) {
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
            dataRequestQueue.offer(putDataReq);
        }

        while (!dataRequestQueue.isEmpty()) {
            PendingResult<DataApi.DataItemResult> pendingResult =
                    Wearable.DataApi.putDataItem(mGoogleApiClient, dataRequestQueue.remove());
            pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                    // Log.e("AMAPW", "DELIVERY: " + dataItemResult.getStatus().isSuccess());
                }
            });
        }
    }

    private PutDataRequest getTestDeviceA() {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/TEST0001");
        DataMap deviceMap = putDataMapReq.getDataMap();
        deviceMap.putString(DEVICE_NAME, "Kitchen Lights");
        deviceMap.putString(DEVICE_DSN, "TEST0001");
        deviceMap.putString(DEVICE_STATUS, "LAN mode");

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
        return putDataReq;
    }

    private PutDataRequest getTestDeviceB() {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/TEST0002");
        DataMap deviceMap = putDataMapReq.getDataMap();
        deviceMap.putString(DEVICE_NAME, "Front Door");
        deviceMap.putString(DEVICE_DSN, "TEST0002");
        deviceMap.putString(DEVICE_STATUS, "Online");

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
        return putDataReq;
    }

    private void startListening() {
        AylaDeviceManager deviceManager = AMAPCore.sharedInstance().getDeviceManager();
        if (deviceManager != null) {
            deviceManager.addListener(this);

            for (AylaDevice device : deviceManager.getDevices()) {
                updateWearDataForDevice(device);
                device.addListener(this);
            }

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

        PendingResult<NodeApi.GetLocalNodeResult> pendingResult = Wearable.NodeApi.getLocalNode(mGoogleApiClient);
        pendingResult.setResultCallback(new ResultCallback<NodeApi.GetLocalNodeResult>() {
            @Override
            public void onResult(@NonNull NodeApi.GetLocalNodeResult getLocalNodeResult) {
                String localNodeId = getLocalNodeResult.getNode().getId();
                for (String dsn : dsns) {
                    if (dsn.equals("")) {
                        continue;
                    }

                    Wearable.DataApi.deleteDataItems(mGoogleApiClient,
                            new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).authority(localNodeId).path("/" + dsn).build());
                }
            }
        });
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

            if (dsn.equals("TEST0001")) {
                int propertyIndex = Integer.valueOf(propertyName);
                TEST_A_STATUS[propertyIndex] = Integer.valueOf(propertyState) == 1;
                updateWearDataForDevice(null);
                sendDeviceControlResultMessage(true);
                return;
            } else if (dsn.equals("TEST0002")) {
                int propertyIndex = Integer.valueOf(propertyName);
                TEST_B_STATUS[propertyIndex] = Integer.valueOf(propertyState) == 1;
                updateWearDataForDevice(null);
                sendDeviceControlResultMessage(true);
                return;
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
                    sendDeviceControlResultMessage(true);
                }
            }, new ErrorListener() {
                @Override
                public void onErrorResponse(AylaError error) {
                    sendDeviceControlResultMessage(false);
                }
            });
        } else if (TextUtils.equals(messageEvent.getPath(), DEVICE_CONTROL_CONNECTION_CHECK)) {
            if (!mWakeLock.isHeld()) {
                mWakeLock.acquire(3 * 1000);
            }

            
        }
    }

    private void sendDeviceControlResultMessage(boolean success) {
        byte[] resultArray = (success ? "1" : "0").getBytes();

        PendingResult<MessageApi.SendMessageResult> pendingResult = Wearable.MessageApi.sendMessage(mGoogleApiClient, mWearableNode,
                DEVICE_CONTROL_RESULT_MSG_URI, resultArray);
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
                        return;
                    }
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        stopListening();
        mGoogleApiClient.disconnect();

        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startListening();
        getWearableNode();
    }

    @Override
    public void deviceLanStateChanged(AylaDevice device, boolean lanModeEnabled) {
        updateWearDataForDevice(device);
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
