package com.aylanetworks.agilelink.framework;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaDeviceGateway;
import com.aylanetworks.aaml.AylaDeviceNode;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaSystemUtils;
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

    private final String LOG_TAG = "Gateway";

    private final static String PROPERTY_JOIN_ENABLE = "join_enable";
    private final static String PROPERTY_JOIN_STATUS = "join_status";

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
        Log.e(LOG_TAG, "Configure with: " + json.toString());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// Node Scanning & Registration

    public boolean getPropertyBooleanJoinStatus() {
        return getPropertyBoolean(PROPERTY_JOIN_STATUS);
    }

    /**
     * Open the device node registration join window.
     * @param listener
     */
    public void openJoinWindow(GatewayStatusListener listener) {
        AylaDeviceGateway device = (AylaDeviceGateway) getDevice();
        final String joinWindowOpenTime = "240"; // optional window open duration, defaults to 200 seconds
        Map<String, String> callParams = new HashMap<String, String>();
        callParams.put(AylaDevice.kAylaDeviceJoinWindowDuration, joinWindowOpenTime);
        device.openRegistrationJoinWindow(new GetOpenJoinWindowHandler(this, listener), callParams);
    }

    protected static class GetOpenJoinWindowHandler extends Handler {
        private WeakReference<Gateway> _gateway;
        private GatewayStatusListener _listener;

        public GetOpenJoinWindowHandler(Gateway gateway, GatewayStatusListener listener) {
            _gateway = new WeakReference<Gateway>(gateway);
            _listener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            String jsonResults = (String) msg.obj; // success = 204
            if (AylaNetworks.succeeded(msg)) {
                AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", "openRegistrationJoinWindow", "results", jsonResults, "openJoinWindow");
                // HACK: per Dan (on nexTurn project) wait for the command to succeed on the gateway
                // We've opened the Join Window, but it can take a while for everybody to think so
                try {
                    Thread.sleep(5000);
                } catch (Exception ex) {
                }
            } else {
                AylaSystemUtils.saveToLog("%s, %s, %s:%s:%d, %s", "E", "openRegistrationJoinWindow", "results", jsonResults, msg.arg1, "openJoinWindow");
            }
            if (_listener != null) {
                _listener.gatewayOpenJoinWindowComplete(_gateway.get(), msg);
            }
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
            }
        }, null);
    }

    public void getRegistrationCandidates(GatewayStatusListener listener) {
        AylaDeviceGateway device = (AylaDeviceGateway) getDevice();
        Map<String, String> callParams = new HashMap<String, String>();
        device.getRegistrationCandidates(new GetRegistrationCandidatesHandler(this, listener), callParams);
    }

    protected static class GetRegistrationCandidatesHandler extends Handler {
        private WeakReference<Gateway> _gateway;
        private GatewayStatusListener _listener;

        public GetRegistrationCandidatesHandler(Gateway gateway, GatewayStatusListener listener) {
            _gateway = new WeakReference<Gateway>(gateway);
            _listener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            List<AylaDeviceNode> list = new ArrayList<AylaDeviceNode>();
            String jsonResults = (String) msg.obj; // success = 204
            if (AylaNetworks.succeeded(msg)) {
                AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", "getRegistrationCandidates", "results", jsonResults, "getRegistrationCandidates");
                AylaDeviceNode[] nodes = AylaSystemUtils.gson.fromJson(jsonResults, AylaDeviceNode[].class);
                String amOwnerStr = "";
                for (AylaDeviceNode node : nodes) {
                    if (node.amOwner()) {
                        amOwnerStr = "true";
                    } else {
                        amOwnerStr = "false";
                    }
                    Log.v("Gateway", "rn: candidate [" + node.dsn + "] amOwner=" + amOwnerStr);
                    AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s:%s, %s", "I", "getRegistrationCandidates", "DSN", node.dsn, "amOwner", amOwnerStr, "getRegistrationCandidates");
                    list.add(node);
                }
            } else {
                AylaSystemUtils.saveToLog("%s, %s, %s:%s:%d, %s", "E", "getRegistrationCandidates", "results", jsonResults, msg.arg1, "getRegistrationCandidates");
            }
            if (_listener != null) {
                _listener.gatewayGetRegistrationCandidatesComplete(_gateway.get(), list, msg);
            }
        }
    }

    public void registerCandidate(AylaDeviceNode node, GatewayStatusListener listener) {
        AylaDeviceGateway device = (AylaDeviceGateway) getDevice();
        device.registerCandidate(new RegisterCandidateHandler(this, node, listener), node);
    }

    protected static class RegisterCandidateHandler extends Handler {
        private WeakReference<Gateway> _gateway;
        private WeakReference<AylaDeviceNode> _node;
        private GatewayStatusListener _listener;

        public RegisterCandidateHandler(Gateway gateway, AylaDeviceNode node, GatewayStatusListener listener) {
            _gateway = new WeakReference<Gateway>(gateway);
            _node = new WeakReference<AylaDeviceNode>(node);
            _listener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            List<AylaDeviceNode> list = new ArrayList<AylaDeviceNode>();
            String jsonResults = (String) msg.obj; // success = 204
            AylaDeviceNode node = null;
            if (AylaNetworks.succeeded(msg)) {
                AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "I", "registerCandidate", "results", jsonResults, "registerCandidate");
                node = AylaSystemUtils.gson.fromJson(jsonResults, AylaDeviceNode.class);
                // No way that it could get registered unless it was Online.
                node.connectionStatus = "Online";
                Log.v("Gateway", "rn: registered [" + node.dsn + "]");
            } else {
                AylaSystemUtils.saveToLog("%s, %s, %s:%s:%d, %s", "E", "registerCandidate", "results", jsonResults, msg.arg1, "registerCandidate");
            }
            if (_listener != null) {
                _listener.gatewayRegisterCandidateComplete(_gateway.get(), node, msg);
            }
        }
    }
}
