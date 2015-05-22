package com.aylanetworks.agilelink.framework;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaDeviceGateway;
import com.aylanetworks.aaml.AylaDeviceNode;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.google.gson.JsonElement;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Gateway.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 12/22/14.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class Gateway extends Device {

    private final static String LOG_TAG = "Gateway";

    private final static String PROPERTY_JOIN_ENABLE = "join_enable";
    private final static String PROPERTY_JOIN_STATUS = "join_status";

    /**
     * Interface used when scanning for and registering a gateway's device nodes
     */
    public interface GatewayNodeRegistrationListener {

        /**
         * Notify that another step in the process of scanning and registering a gateway's
         * device nodes has occurred.  Provides the application with a way to update
         * the user interface accordingly.
         *
         * @param msg The current Message.
         * @param messageResourceId String resource id to use to display a toast
         *                          or dialog.
         */
        public void registrationScanNextStep(Message msg, int messageResourceId);

        /**
         * Notify that the the processs of scanning and registering a gateway's
         * device nodes has completed.
         *
         * @param device The device that has been registered.
         * @param msg The final Message.
         * @param messageResourceId String resource id to display a toast to the user.
         */
        public void registrationComplete(Device device, Message msg, int messageResourceId);

    }

    public Gateway(AylaDevice aylaDevice) {
        super(aylaDevice);
    }

    public AylaDeviceGateway getGatewayDevice() {
        return (AylaDeviceGateway)getDevice();
    }

    @Override
    protected ArrayList<String> getPropertyNames() {
        // Get the superclass' property names (probably none)
        ArrayList<String> propertyNames = super.getPropertyNames();

        // Add our own
        propertyNames.add("attr_set_cmd");
        propertyNames.add("attr_set_result");
        propertyNames.add("attr_read_data");
        propertyNames.add("join_enable");
        propertyNames.add("join_status");

        return propertyNames;
    }

    @Override
    public String registrationType() {
        return AylaNetworks.AML_REGISTRATION_TYPE_BUTTON_PUSH;
    }

    @Override
    public boolean isGateway() {
        return true;
    }

    @Override
    public String deviceTypeName() {
        return "Zigbee Gateway";
    }

    @Override
    public Drawable getDeviceDrawable(Context c) {
        return c.getResources().getDrawable(R.drawable.ic_zigbee);
    }

    public void configureWithJsonElement(JsonElement json) {
        Logger.logError(LOG_TAG, "Configure with: " + json.toString());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// Node Scanning & Registration

    /**
     * Start the Gateway device scan & registration process
     * @param listener Listener used to indicate progress through the process.
     */
    public void startRegistrationScan(GatewayNodeRegistrationListener listener) {
        _nodeRegistrationState = NodeRegistrationFindState.Started;
        nextNodeRegistrationStep(listener);
    }

    /**
     * Incremental progress through the Gateway scan & registration process.  Must be called
     * by the GatewayNodeRegistrationListener that invoked startRegistrationScan
     * @param listener Listener used to indicate progress through the process.
     */
    public void processRegistrationScan(GatewayNodeRegistrationListener listener) {
        nextNodeRegistrationStep(listener);
    }

    /**
     * Must be called when the application UI that invoked startRegistrationScan has
     * completed.
     */
    public void cleanupRegistrationScan() {
        closeJoinWindow();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    enum NodeRegistrationFindState {
        NotStarted,
        Started,
        OpenJoinWindow,
        FindDevices,
    }

    NodeRegistrationFindState _nodeRegistrationState = NodeRegistrationFindState.NotStarted;
    List<AylaDeviceNode> _nodeRegistrationCandidates;

    void nextNodeRegistrationStep(GatewayNodeRegistrationListener listener) {
        Logger.logInfo(LOG_TAG, "rn: Register node state=" + _nodeRegistrationState);
        if (_nodeRegistrationState == NodeRegistrationFindState.Started) {
            Logger.logInfo(LOG_TAG, "rn: Register node get property join_status");
            if (getPropertyBooleanJoinStatus()) {
                Logger.logInfo(LOG_TAG, "rn: Register node (JOIN_STATUS=true)");
                Logger.logInfo(LOG_TAG, "rn: Register node FindDevices");
                _nodeRegistrationState = NodeRegistrationFindState.FindDevices;
                getRegistrationCandidates(listener);
            } else {
                Logger.logInfo(LOG_TAG, "rn: Register node (JOIN_STATUS=false)");
                Logger.logInfo(LOG_TAG, "rn: Register node OpenJoinWindow");
                _nodeRegistrationState = NodeRegistrationFindState.OpenJoinWindow;
                openJoinWindow(listener);
            }
        } else if (_nodeRegistrationState == NodeRegistrationFindState.OpenJoinWindow) {
            Logger.logInfo(LOG_TAG, "rn: Register node FindDevices");
            _nodeRegistrationState = NodeRegistrationFindState.FindDevices;
            getRegistrationCandidates(listener);
        } else if (_nodeRegistrationState == NodeRegistrationFindState.FindDevices) {
            Logger.logInfo(LOG_TAG, "rn: Register node FindDevices");
            _nodeRegistrationState = NodeRegistrationFindState.FindDevices;
            getRegistrationCandidates(listener);
        }
    }

    boolean getPropertyBooleanJoinStatus() {
        return getPropertyBoolean(PROPERTY_JOIN_STATUS);
    }

    // Dan said to do the open join window using the property like the iOS version does,
    // as this way of doing it was only done with the nexTurn gateway.
    // 2015/5/22 When I looked at the iOS code, it too calls an Ayla library method that
    // invokes registration_window.json with a duration.
    void openJoinWindow(GatewayNodeRegistrationListener listener) {
        AylaDeviceGateway device = (AylaDeviceGateway) getDevice();
        final String joinWindowOpenTime = "240"; // optional window open duration, defaults to 200 seconds
        Map<String, String> callParams = new HashMap<String, String>();
        callParams.put(AylaDevice.kAylaDeviceJoinWindowDuration, joinWindowOpenTime);
        device.openRegistrationJoinWindow(new GetOpenJoinWindowHandler(this, listener), callParams);
    }

    void gatewayOpenJoinWindowComplete(final Message msg, GatewayNodeRegistrationListener listener) {
        Logger.logInfo(LOG_TAG, "rn: gatewayOpenJoinWindowComplete " + msg.what + ":" + msg.arg1);
        if (AylaNetworks.succeeded(msg)) {
            listener.registrationScanNextStep(msg, 0);
        } else {
            _nodeRegistrationState = NodeRegistrationFindState.NotStarted;
            listener.registrationScanNextStep(msg, R.string.error_gateway_join_window);
        }
    }

    static class GetOpenJoinWindowHandler extends Handler {
        private WeakReference<Gateway> _gateway;
        private GatewayNodeRegistrationListener _listener;

        GetOpenJoinWindowHandler(Gateway gateway, GatewayNodeRegistrationListener listener) {
            _gateway = new WeakReference<Gateway>(gateway);
            _listener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            String jsonResults = (String) msg.obj; // success = 204
            Logger.logMessage(LOG_TAG, "openRegistrationJoinWindow", msg);
            if (AylaNetworks.succeeded(msg)) {
                // HACK: per Dan (on nexTurn project) wait for the command to succeed on the gateway
                // We've opened the Join Window, but it can take a while for everybody to think so
                try {
                    Thread.sleep(5000);
                } catch (Exception ex) {
                }
            }
            _gateway.get().gatewayOpenJoinWindowComplete(msg, _listener);
        }
    }

    void closeJoinWindow() {
        // We need to do this any time the join window is left open
        Logger.logInfo(LOG_TAG, "rn: Register node close join window");
        AylaDeviceGateway device = (AylaDeviceGateway) getDevice();
        device.closeRegistrationJoinWindow(new Handler() {
            public void handleMessage(Message msg) {
                // nothing
            }
        }, null);
    }

    void getRegistrationCandidates(GatewayNodeRegistrationListener listener) {
        AylaDeviceGateway device = (AylaDeviceGateway) getDevice();
        Map<String, String> callParams = new HashMap<String, String>();
        device.getRegistrationCandidates(new GetRegistrationCandidatesHandler(this, listener), callParams);
    }

    Message getSucceededMessage() {
        Message msg = new Message();
        msg.what = AylaNetworks.AML_ERROR_OK;
        return msg;
    }

    void gatewayGetRegistrationCandidatesComplete(final List<AylaDeviceNode> list, final Message msg, final GatewayNodeRegistrationListener listener) {
        Logger.logInfo(LOG_TAG, "rn: gatewayGetRegistrationCandidatesComplete " + msg.what + ":" + msg.arg1);
        if (AylaNetworks.succeeded(msg)) {
            // we have a list of candidates...
            _nodeRegistrationCandidates = list;

            // TODO: Bring up a dialog showing which ones to register.

            // for now, we are just going to register the first one...
            final AylaDeviceNode node = list.get(0);
            MainActivity.getInstance().runOnUiThread(new Runnable() {
                public void run() {
                    registerCandidate(node, listener);
                }
            });
        } else {
            if (msg.arg1 == 412) {
                // invoke it again manually (412: retry open join window)
                _nodeRegistrationState = NodeRegistrationFindState.Started;
                listener.registrationScanNextStep(getSucceededMessage(), 0);
            } else if (msg.arg1 == 404) {
                // invoke it again manually (404: retry get candidates)
                Logger.logInfo(LOG_TAG, "rn: Register node GRC postDelayed 404");
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {		// don't flood with retries
                    @Override
                    public void run() {
                        Logger.logInfo(LOG_TAG, "rn: Register node GRC postDelayed run");
                        if (getPropertyBooleanJoinStatus()) {
                            Logger.logInfo(LOG_TAG, "rn: Register node GRC FindDevices");
                            getRegistrationCandidates(listener);
                        } else {
                            _nodeRegistrationState = NodeRegistrationFindState.NotStarted;
                            listener.registrationScanNextStep(msg, R.string.error_gateway_registration_candidates);
                        }
                    }
                }, 5000);									// Delay 5 seconds

            } else {
                // error message (restart)
                _nodeRegistrationState = NodeRegistrationFindState.NotStarted;
                listener.registrationScanNextStep(msg, R.string.error_gateway_registration_candidates);
            }
        }
    }

    static class GetRegistrationCandidatesHandler extends Handler {
        private WeakReference<Gateway> _gateway;
        private GatewayNodeRegistrationListener _listener;

        GetRegistrationCandidatesHandler(Gateway gateway, GatewayNodeRegistrationListener listener) {
            _gateway = new WeakReference<Gateway>(gateway);
            _listener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            List<AylaDeviceNode> list = new ArrayList<AylaDeviceNode>();
            String jsonResults = (String) msg.obj; // success = 204
            Logger.logMessage(LOG_TAG, "getRegistrationCandidates", msg);
            if (AylaNetworks.succeeded(msg)) {
                AylaDeviceNode[] nodes = AylaSystemUtils.gson.fromJson(jsonResults, AylaDeviceNode[].class);
                String amOwnerStr = "";
                for (AylaDeviceNode node : nodes) {
                    if (node.amOwner()) {
                        amOwnerStr = "true";
                    } else {
                        amOwnerStr = "false";
                    }
                    Logger.logInfo(LOG_TAG, "getRegistrationCandidates DSN:%s amOwner:%s", node.dsn, amOwnerStr);
                    list.add(node);
                }
            }
            _gateway.get().gatewayGetRegistrationCandidatesComplete(list, msg, _listener);
        }
    }

    void registerCandidate(AylaDeviceNode node, GatewayNodeRegistrationListener listener) {
        AylaDeviceGateway device = (AylaDeviceGateway) getDevice();
        device.registerCandidate(new RegisterCandidateHandler(this, node, listener), node);
    }

    static class RegisterCandidateHandler extends Handler {
        private WeakReference<Gateway> _gateway;
        private WeakReference<AylaDeviceNode> _node;
        private GatewayNodeRegistrationListener _listener;

        RegisterCandidateHandler(Gateway gateway, AylaDeviceNode node, GatewayNodeRegistrationListener listener) {
            _gateway = new WeakReference<Gateway>(gateway);
            _node = new WeakReference<AylaDeviceNode>(node);
            _listener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.logMessage(LOG_TAG, "registerCandidate", msg);
            Device device = null;
            if (AylaNetworks.succeeded(msg)) {
                AylaDeviceNode aylaDevice = AylaSystemUtils.gson.fromJson((String) msg.obj, AylaDeviceNode.class);
                // No way that it could get registered unless it was Online.
                aylaDevice.connectionStatus = "Online";
                if (TextUtils.isEmpty(aylaDevice.oemModel)) {
                    aylaDevice.oemModel = "zigbee1";
                }
                Logger.logInfo(LOG_TAG, "rn: registered node [%s]:[%s]", aylaDevice.dsn, aylaDevice.model);
                Logger.logDebug(LOG_TAG, "rn: registered node [%s]", aylaDevice);
                device = SessionManager.sessionParameters().deviceCreator.deviceForAylaDevice(aylaDevice);
                // TODO: do we need to add it to some list?

                // rename it
                String name = device.deviceTypeName();
                String dsn = aylaDevice.dsn;
                List<Device> devices = SessionManager.deviceManager().deviceList();
                boolean existsName = false;
                int nameIteration = 1;
                do {
                    existsName = false;
                    for (Device d : devices) {
                        if (TextUtils.equals(d.getDevice().dsn, dsn)) {
                            continue;
                        }
                        if (TextUtils.equals(d.getDevice().deviceName, name)) {
                            existsName = true;
                        }
                    }
                    if (existsName) {
                        name = name + " " + (nameIteration++);
                    }
                } while (existsName);
                Logger.logInfo(LOG_TAG, "rn: rename [%s] [%s] to [%s]", dsn, device.getDevice().deviceName, name);
                Map<String, String> params = new HashMap<>();
                params.put("productName", name);
                device.getDevice().update(new ChangeNameHandler(_gateway.get(), device, name, _listener), params);
            } else {
                _gateway.get().closeJoinWindow();
                _listener.registrationComplete(device, msg, R.string.error_gateway_register_device_node);
           }
        }
    }

    static class ChangeNameHandler extends Handler {
        private WeakReference<Gateway> _gateway;
        private WeakReference<Device> _device;
        private String _newDeviceName;
        private GatewayNodeRegistrationListener _listener;

        ChangeNameHandler(Gateway gateway, Device device, String newDeviceName, GatewayNodeRegistrationListener listener) {
            _gateway = new WeakReference<Gateway>(gateway);
            _device = new WeakReference<Device>(device);
            _newDeviceName = newDeviceName;
            _listener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.logMessage(LOG_TAG, "rn: ChangeName", msg);
            if (AylaNetworks.succeeded(msg)) {
                _device.get().getDevice().productName = _newDeviceName;
                // Let the world know something is different
                SessionManager.deviceManager().deviceChanged(_device.get());
            }
            _gateway.get().closeJoinWindow();

            // Here we will create our groups & bindings
            _device.get().postRegistration();

            _listener.registrationComplete(_device.get(), msg, R.string.gateway_registered_device_node);
        }
    }
}
