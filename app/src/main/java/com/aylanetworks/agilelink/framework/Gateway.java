package com.aylanetworks.agilelink.framework;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;

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

    public interface GatewayNodeRegistrationListener {

        public void registrationScanNextStep(Message msg, int messageResourceId);

        public void registrationComplete(Message msg, int messageResourceId);

    }

    /**
     * Interface called whenever the status of a gateway changes.
     */
    public interface GatewayStatusListener {

        /**
         * Notify that the request to open the join window has completed.
         * @param gateway Gateway to open the join window
         * @param msg Message returned from the server in response to
         *            opening the join window.
         */
        void gatewayOpenJoinWindowComplete(Gateway gateway, Message msg);

        /**
         * Notify completion of registration candidates.
         * @param gateway Gateway to get registration candidates from.
         * @param list List of AylaDeviceNode registration candidates
         * @param msg Message returned from the server in response
         *            to getting registration candidates.  On
         *            failure, check arg1 for:
         *             412 - join_status is 0, retry open join window
         *             404 - no candidates found, try again after a small delay.
         * */
        void gatewayGetRegistrationCandidatesComplete(Gateway gateway, List<AylaDeviceNode> list, Message msg);

        /**
         * Notify when register candidate completes.
         * @param gateway Gateway to register candidate with
         * @param node Registered device node
         * @param msg Message returned from the server in response to
         *            register candidate.
         */
        void gatewayRegisterCandidateComplete(Gateway gateway, AylaDeviceNode node, Message msg);

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

    enum NodeRegistrationFindState {
        NotStarted,
        Started,
        OpenJoinWindow,
        FindDevices,
    }

    private NodeRegistrationFindState _nodeRegistrationState = NodeRegistrationFindState.NotStarted;
    private List<AylaDeviceNode> _nodeRegistrationCandidates;

    private void ensureJoinWindowClosed() {
        // We need to do this any time the join window is left open
        Logger.logInfo(LOG_TAG, "rn: Register node close join window");
        closeJoinWindow(null);        // close the join window
    }

    private void nextNodeRegistrationStep(GatewayNodeRegistrationListener listener) {
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

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public void cleanupRegistrationScan() {
        ensureJoinWindowClosed();
    }

    public void processRegistrationScan(GatewayNodeRegistrationListener listener) {
        nextNodeRegistrationStep(listener);
    }

    public void startRegistrationScan(GatewayNodeRegistrationListener listener) {
        _nodeRegistrationState = NodeRegistrationFindState.Started;
        nextNodeRegistrationStep(listener);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean getPropertyBooleanJoinStatus() {
        return getPropertyBoolean(PROPERTY_JOIN_STATUS);
    }

    /**
     * Open the device node registration join window.
     * @param listener
     */
    public void openJoinWindow(GatewayNodeRegistrationListener listener) {
        AylaDeviceGateway device = (AylaDeviceGateway) getDevice();
        final String joinWindowOpenTime = "240"; // optional window open duration, defaults to 200 seconds
        Map<String, String> callParams = new HashMap<String, String>();
        callParams.put(AylaDevice.kAylaDeviceJoinWindowDuration, joinWindowOpenTime);
        device.openRegistrationJoinWindow(new GetOpenJoinWindowHandler(this, listener), callParams);
    }

    public void gatewayOpenJoinWindowComplete(final Message msg, GatewayNodeRegistrationListener listener) {
        Logger.logInfo(LOG_TAG, "rn: gatewayOpenJoinWindowComplete " + msg.what + ":" + msg.arg1);
        if (AylaNetworks.succeeded(msg)) {
            if (listener != null) {
                listener.registrationScanNextStep(msg, 0);
            }
        } else {
            _nodeRegistrationState = NodeRegistrationFindState.NotStarted;
            if (listener != null) {
                listener.registrationScanNextStep(msg, R.string.error_gateway_join_window);
            }
        }
    }

    protected static class GetOpenJoinWindowHandler extends Handler {
        private WeakReference<Gateway> _gateway;
        private GatewayNodeRegistrationListener _listener;

        public GetOpenJoinWindowHandler(Gateway gateway, GatewayNodeRegistrationListener listener) {
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

    /**
     * Close the device node registration join window.
     * @param listener
     */
    public void closeJoinWindow(GatewayStatusListener listener) {
        AylaDeviceGateway device = (AylaDeviceGateway) getDevice();
        device.closeRegistrationJoinWindow(new Handler() {
            public void handleMessage(Message msg) {
                // nothing
            }
        }, null);
    }

    public void getRegistrationCandidates(GatewayNodeRegistrationListener listener) {
        AylaDeviceGateway device = (AylaDeviceGateway) getDevice();
        Map<String, String> callParams = new HashMap<String, String>();
        device.getRegistrationCandidates(new GetRegistrationCandidatesHandler(this, listener), callParams);
    }

    Message getSucceededMessage() {
        Message msg = new Message();
        msg.what = AylaNetworks.AML_ERROR_OK;
        return msg;
    }

    public void gatewayGetRegistrationCandidatesComplete(final List<AylaDeviceNode> list, final Message msg, final GatewayNodeRegistrationListener listener) {
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
                if (listener != null) {
                    listener.registrationScanNextStep(getSucceededMessage(), 0);
                }
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
                            if (listener != null) {
                                listener.registrationScanNextStep(msg, R.string.error_gateway_registration_candidates);
                            }
                        }
                    }
                }, 5000);									// Delay 5 seconds

            } else {
                // error message (restart)
                _nodeRegistrationState = NodeRegistrationFindState.NotStarted;
                if (listener != null) {
                    listener.registrationScanNextStep(msg, R.string.error_gateway_registration_candidates);
                }
            }
        }
    }

    protected static class GetRegistrationCandidatesHandler extends Handler {
        private WeakReference<Gateway> _gateway;
        private GatewayNodeRegistrationListener _listener;

        public GetRegistrationCandidatesHandler(Gateway gateway, GatewayNodeRegistrationListener listener) {
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

    public void registerCandidate(AylaDeviceNode node, GatewayNodeRegistrationListener listener) {
        AylaDeviceGateway device = (AylaDeviceGateway) getDevice();
        device.registerCandidate(new RegisterCandidateHandler(this, node, listener), node);
    }

    public void gatewayRegisterCandidateComplete(AylaDeviceNode node, Message msg, GatewayNodeRegistrationListener listener) {
        Logger.logInfo(LOG_TAG, "rn: gatewayRegisterCandidateComplete " + msg.what + ":" + msg.arg1);
        int messageId;
        if (AylaNetworks.succeeded(msg)) {
            messageId = R.string.gateway_registered_device_node;
            // TODO: do we need to add it to some list?
            Logger.logInfo(LOG_TAG, "rn: registered node [%s]:[%s]", node.dsn, node.model);
            Logger.logDebug(LOG_TAG, "rn: registered node [%s]", node);
            // TODO: rename it
            // now we need to rename it...
        } else {
            Logger.logError(LOG_TAG, "rn: failed to register node. error=" + msg.what + ":" + msg.arg1);
            messageId = R.string.error_gateway_register_device_node;
        }
        ensureJoinWindowClosed();
        if (listener != null) {
            listener.registrationComplete(msg, messageId);
        }
    }

    protected static class RegisterCandidateHandler extends Handler {
        private WeakReference<Gateway> _gateway;
        private WeakReference<AylaDeviceNode> _node;
        private GatewayNodeRegistrationListener _listener;

        public RegisterCandidateHandler(Gateway gateway, AylaDeviceNode node, GatewayNodeRegistrationListener listener) {
            _gateway = new WeakReference<Gateway>(gateway);
            _node = new WeakReference<AylaDeviceNode>(node);
            _listener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            List<AylaDeviceNode> list = new ArrayList<AylaDeviceNode>();
            String jsonResults = (String) msg.obj; // success = 204
            AylaDeviceNode node = null;
            Logger.logMessage(LOG_TAG, "registerCandidate", msg);
            if (AylaNetworks.succeeded(msg)) {
                node = AylaSystemUtils.gson.fromJson(jsonResults, AylaDeviceNode.class);
                // No way that it could get registered unless it was Online.
                node.connectionStatus = "Online";
                Logger.logVerbose(LOG_TAG, "rn: registered [" + node.dsn + "]");
            }
            _gateway.get().gatewayRegisterCandidateComplete(node, msg, _listener);
        }
    }
}
