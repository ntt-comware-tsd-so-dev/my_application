package com.aylanetworks.agilelink.framework;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;

import com.aylanetworks.aaml.AylaDatapoint;
import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaDeviceGateway;
import com.aylanetworks.aaml.AylaDeviceNode;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaProperty;
import com.aylanetworks.aaml.AylaRestService;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.zigbee.AylaBindingZigbee;
import com.aylanetworks.aaml.zigbee.AylaGroupZigbee;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;

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

    private final static long SCAN_TIMEOUT = (30 * 1000);

    /**
     * Get the gateway for a device.
     *
     * @param device The Device to get the gateway for.
     * @return Gateway.  Null if the device is not a DeviceNode.
     */
    public static Gateway getGatewayForDeviceNode(Device device) {
        if (device.isDeviceNode()) {
            AylaDeviceNode adn = (AylaDeviceNode)device.getDevice();
            return (Gateway)SessionManager.deviceManager().deviceByDSN(adn.gatewayDsn);
        }
        return null;
    }

    /**
     * Interface used when scanning for and registering a gateway's device nodes
     */
    public interface GatewayNodeRegistrationListener {

        //public void registrationCandidates(ArrayList<AylaDeviceNode> list, Object tag);

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

    // We need a simple, generic completion handler to use when performing a lot of different
    // steps that do not need notification along the way to anybody but the one performing
    // the steps.
    public interface AylaGatewayCompletionHandler {

        public void handle(Gateway gateway, Message msg, Object tag);
    }

    public Gateway(AylaDevice aylaDevice) {
        super(aylaDevice);
    }

    @Override
    public void deviceAdded() {
        getGroupManager().fetchZigbeeGroupsIfNeeded();
        getBindingManager().fetchZigbeeBindingsIfNeeded();
    }

    public AylaDeviceGateway getGatewayDevice() {
        return (AylaDeviceGateway)getDevice();
    }

    public void removeDeviceNode(Device device) {

    }

    private ZigbeeGroupManager _groupManager;

    public ZigbeeGroupManager getGroupManager() {
        if (_groupManager == null) {
            _groupManager = (ZigbeeGroupManager)SessionManager.deviceManager().getDeviceAssistantManager(this, ZigbeeGroupManager.class);
        }
        return _groupManager;
    }

    private ZigbeeBindingManager _bindingManager;

    public ZigbeeBindingManager getBindingManager() {
        if (_bindingManager == null) {
            _bindingManager = (ZigbeeBindingManager)SessionManager.deviceManager().getDeviceAssistantManager(this, ZigbeeBindingManager.class);
        }
        return _bindingManager;
    }

    public void createGroup(String groupName, List<Device>deviceList, Object tag, AylaGatewayCompletionHandler handler) {
        getGroupManager().createGroup(groupName, deviceList, tag, handler);
    }

    public void deleteGroup(AylaGroupZigbee group, Object tag, AylaGatewayCompletionHandler handler) {
        getGroupManager().deleteGroup(group, tag, handler);
    }

    public void createBinding(AylaBindingZigbee binding, Object tag, AylaGatewayCompletionHandler handler) {
        getBindingManager().createBinding(binding, tag, handler);
    }

    public void deleteBinding(AylaBindingZigbee binding, Object tag, AylaGatewayCompletionHandler handler) {
        getBindingManager().deleteBinding(binding, tag, handler);
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

    @Override
    public void bindViewHolder(RecyclerView.ViewHolder holder) {
        super.bindViewHolder(holder);
        try {
            CardView cardView = (CardView) holder.itemView;
            cardView.setCardBackgroundColor(MainActivity.getInstance().getResources().getColor(R.color.card_background_gateway));
        } catch (ClassCastException e) {
            // This is not a cardview. Just set the background color of the view.
            holder.itemView.setBackgroundColor(MainActivity.getInstance().getResources().getColor(R.color.card_background_gateway));
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// Node Scanning & Registration

    enum NodeRegistrationFindState {
        NotStarted,
        Started,
        OpenJoinWindow,
        FindDevices,
        Timeout,
    }

    void openJoinWindow(ScanTag tag) {
        AylaDeviceGateway gateway = (AylaDeviceGateway) getDevice();
        Map<String, String> callParams = new HashMap<String, String>();
        callParams.put("names", PROPERTY_JOIN_ENABLE + " " + PROPERTY_JOIN_STATUS);
        gateway.getProperties(new GetPropertyJoinEnableHandler(tag), callParams);
    }

    class GetPropertyJoinEnableHandler extends Handler {
        ScanTag _tag;

        GetPropertyJoinEnableHandler(ScanTag tag) {  _tag = tag; }

        @Override
        public void handleMessage(Message msg) { _tag.joinEnableProperty(msg); }
    }

    void registrationCandidates(ScanTag tag) {
        AylaDeviceGateway device = (AylaDeviceGateway) getDevice();
        Map<String, String> callParams = new HashMap<String, String>();
        device.getRegistrationCandidates(new RegistrationCandidatesHandler(tag), callParams);
    }

    class RegistrationCandidatesHandler extends Handler {
        ScanTag _tag;

        RegistrationCandidatesHandler(ScanTag tag) { _tag = tag; }

        @Override
        public void handleMessage(Message msg) { _tag.candidatesComplete(msg); }
    }

    void registerCandidate(AylaDeviceNode node, ScanTag tag) {
        AylaDeviceGateway device = (AylaDeviceGateway) getDevice();
        device.registerCandidate(new RegistrationCandidateHandler(node, tag), node);
    }

    class RegistrationCandidateHandler extends Handler {
        AylaDeviceNode _node;
        ScanTag _tag;

        RegistrationCandidateHandler(AylaDeviceNode node, ScanTag tag) { _tag = tag; _node = node; }

        @Override
        public void handleMessage(Message msg) { _tag.registerComplete(_node, msg); }
    }

    void setDefaultName(Device device, ScanTag tag) {
        String name = device.deviceTypeName();
        String dsn = device.getDevice().dsn;
        List<Device> devices = SessionManager.deviceManager().deviceList();
        boolean existsName = false;
        int nameIteration = 2;
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
        Logger.logInfo(LOG_TAG, "rn: Register node rename [%s] [%s] to [%s]", dsn, device.getDevice().deviceName, name);
        Map<String, String> params = new HashMap<>();
        params.put("productName", name);
        device.getDevice().update(new UpdateHandler(device, name, tag), params);
    }

    class UpdateHandler extends Handler {
        Device _device;
        String _name;
        ScanTag _tag;

        UpdateHandler(Device device, String name, ScanTag tag) { _device = device; _name = name; _tag = tag; }

        @Override
        public void handleMessage(Message msg) {
            if (AylaNetworks.succeeded(msg)) {
                _device.getDevice().productName = _name;
                SessionManager.deviceManager().deviceChanged(_device);
            }
            _tag.updateComplete(_device, msg);
        }
    }

    interface ScanTagCompletionHandler {

        void handle(Gateway gateway, Message msg, ScanTag tag);
    }

    class ScanTag {
        WeakReference<ScanTagCompletionHandler> _completion;
        Gateway gateway;
        NodeRegistrationFindState state;
        long startTicks;
        GatewayNodeRegistrationListener listener;
        List<AylaDeviceNode> list;
        Device device;
        int resourceId;

        ScanTag(Gateway gateway, GatewayNodeRegistrationListener listener, ScanTagCompletionHandler completion) {
            _completion = new WeakReference<ScanTagCompletionHandler>(completion);
            this.gateway = gateway;
            this.state = NodeRegistrationFindState.Started;
            this.startTicks = System.currentTimeMillis();
            this.listener = listener;
        }

        void nextStep() {
            Logger.logInfo(LOG_TAG, "rn: Register node state=" + state);
            if (state == NodeRegistrationFindState.Started) {
                Logger.logInfo(LOG_TAG, "rn: Register node get property join_status");
                if (gateway.getPropertyBoolean(PROPERTY_JOIN_STATUS)) {
                    Logger.logInfo(LOG_TAG, "rn: Register node (JOIN_STATUS=true)");
                    Logger.logInfo(LOG_TAG, "rn: Register node FindDevices");
                    state = NodeRegistrationFindState.FindDevices;
                    gateway.registrationCandidates(this);
                } else {
                    Logger.logInfo(LOG_TAG, "rn: Register node (JOIN_STATUS=false)");
                    Logger.logInfo(LOG_TAG, "rn: Register node OpenJoinWindow");
                    state = NodeRegistrationFindState.OpenJoinWindow;
                    gateway.openJoinWindow(this);
                }
            } else if (state == NodeRegistrationFindState.OpenJoinWindow) {
                Logger.logInfo(LOG_TAG, "rn: Register node FindDevices");
                state = NodeRegistrationFindState.FindDevices;
                gateway.registrationCandidates(this);
            } else if (state == NodeRegistrationFindState.FindDevices) {
                Logger.logInfo(LOG_TAG, "rn: Register node FindDevices");
                state = NodeRegistrationFindState.FindDevices;
                gateway.registrationCandidates(this);
            }
        }

        void joinEnableProperty(Message msg) {
            Logger.logMessage(LOG_TAG, msg, "rn: getProperties JOIN_ENABLE/STATUS");
            if (AylaNetworks.succeeded(msg)) {
                AylaProperty[] props = AylaSystemUtils.gson.fromJson((String)msg.obj, AylaProperty[].class);
                int got = 0;
                int set = 0;
                for (AylaProperty prop : props) {
                    if (prop.name.equals(PROPERTY_JOIN_ENABLE)) {
                        Logger.logVerbose(LOG_TAG, "rn: prop [%s]=[%s]", prop.name, prop.value);
                        if (prop.datapoint.nValue().intValue() == 240) {
                           got++;
                        } else {
                            AylaDatapoint dp = new AylaDatapoint();
                            dp.nValue(240);
                            AylaRestService rs = prop.createDatapoint(dp);
                            Message m = rs.execute();
                            Logger.logMessage(LOG_TAG, m, "rn: setProperty JOIN_ENABLE");
                            if (AylaNetworks.succeeded(m)) {
                                set++;
                            }
                        }
                    } else if (prop.name.equals(PROPERTY_JOIN_STATUS)) {
                        Logger.logVerbose(LOG_TAG, "rn: prop [%s]=[%s]", prop.name, prop.value);
                        if (prop.datapoint.nValue().intValue() == 1) {
                            got++;
                        } else{
                            AylaDatapoint dp = new AylaDatapoint();
                            dp.nValue(1);
                            AylaRestService rs = prop.createDatapoint(dp);
                            Message m = rs.execute();
                            Logger.logMessage(LOG_TAG, m, "rn: setProperty JOIN_STATUS");
                            if (AylaNetworks.succeeded(m)) {
                                set++;
                            }
                        }
                    }
                }
                if (got == 2) {
                    // good to go
                    nextStep();
                } else if (set > 0) {
                    if (System.currentTimeMillis() - startTicks > SCAN_TIMEOUT) {
                        Logger.logVerbose(LOG_TAG, "rn: Register node timeout");
                        state = NodeRegistrationFindState.Timeout;
                        resourceId = R.string.error_gateway_registration_candidates;
                        if (_completion.get() != null) {
                            _completion.get().handle(gateway, msg, this);
                        }
                        //listener.registrationScanNextStep(msg, R.string.error_gateway_registration_candidates);
                    } else {
                        // verify/set values again
                        gateway.openJoinWindow(this);
                    }
                } else {
                    state = NodeRegistrationFindState.NotStarted;
                    resourceId = R.string.error_gateway_join_window;
                    if (_completion.get() != null) {
                        _completion.get().handle(gateway, msg, this);
                    }
                }
            } else {
                state = NodeRegistrationFindState.NotStarted;
                resourceId = R.string.error_gateway_join_window;
                if (_completion.get() != null) {
                    _completion.get().handle(gateway, msg, this);
                }
            }
        }

        void candidatesComplete(final Message msg) {
            Logger.logMessage(LOG_TAG, msg, "rn: getRegistrationCandidates");
            if (AylaNetworks.succeeded(msg)) {
                // we have a list of candidates...
                list = new ArrayList<AylaDeviceNode>();
                AylaDeviceNode[] nodes = AylaSystemUtils.gson.fromJson((String)msg.obj, AylaDeviceNode[].class);
                String amOwnerStr = "";
                for (AylaDeviceNode node : nodes) {
                    if (node.amOwner()) {
                        amOwnerStr = "true";
                    } else {
                        amOwnerStr = "false";
                    }
                    Logger.logInfo(LOG_TAG, "rn: candidate DSN:%s amOwner:%s", node.dsn, amOwnerStr);
                    list.add(node);
                }

                // TODO: Bring up a dialog showing which ones to register.

                // pull the first candidate off the list
                AylaDeviceNode node = list.get(0);
                gateway.registerCandidate(list.get(0), this);

                /*
                for (AylaDeviceNode node : list) {
                    gateway.registerCandidate(this, node);
                }
                */

            } else {
                if (msg.arg1 == 412) {
                    if (System.currentTimeMillis() - startTicks > SCAN_TIMEOUT) {
                        Logger.logVerbose(LOG_TAG, "rn: Register node timeout");
                        state = NodeRegistrationFindState.Timeout;
                        resourceId = R.string.error_gateway_registration_candidates;
                        if (_completion.get() != null) {
                            _completion.get().handle(gateway, msg, this);
                        }
                    } else {
                        // invoke it again manually (412: retry open join window)
                        state = NodeRegistrationFindState.Started;
                        nextStep();;
                    }
                } else if (msg.arg1 == 404) {
                    if (System.currentTimeMillis() - startTicks > SCAN_TIMEOUT) {
                        Logger.logVerbose(LOG_TAG, "rn: Register node timeout");
                        state = NodeRegistrationFindState.Timeout;
                        resourceId = R.string.no_devices_found;
                        if (_completion.get() != null) {
                            _completion.get().handle(gateway, msg, this);
                        }
                    } else {
                        // invoke it again manually (404: retry get candidates)
                        Logger.logVerbose(LOG_TAG, "rn: Register node GRC postDelayed 404");
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {        // don't flood with retries
                            @Override
                            public void run() {
                                Logger.logVerbose(LOG_TAG, "rn: Register node GRC postDelayed run");
                                if (getPropertyBoolean(PROPERTY_JOIN_STATUS)) {
                                    Logger.logVerbose(LOG_TAG, "rn: Register node GRC FindDevices");
                                    gateway.registrationCandidates(ScanTag.this);
                                } else {
                                    state = NodeRegistrationFindState.NotStarted;
                                    resourceId = R.string.error_gateway_registration_candidates;
                                    if (_completion.get() != null) {
                                        _completion.get().handle(gateway, msg, ScanTag.this);
                                    }
                                }
                            }
                        }, 5000);                                    // Delay 5 seconds
                    }
                } else {
                    // error message (restart)
                    state = NodeRegistrationFindState.NotStarted;
                    resourceId = R.string.error_gateway_registration_candidates;
                    if (_completion.get() != null) {
                        _completion.get().handle(gateway, msg, this);
                    }
                }
            }
        }

        void registerComplete(AylaDeviceNode node, Message msg) {
            Logger.logMessage(LOG_TAG, msg, "rn: registerCandidate [%s] for [%s]", node.dsn, node.gatewayDsn);
            if (AylaNetworks.succeeded(msg)) {
                AylaDeviceNode adn = AylaSystemUtils.gson.fromJson((String) msg.obj, AylaDeviceNode.class);
                // No way that it could get registered unless it was Online.
                adn.connectionStatus = "Online";
                if (TextUtils.isEmpty(adn.oemModel)) {
                    adn.oemModel = "zigbee1";
                }
                Logger.logInfo(LOG_TAG, "rn: registered node [%s]:[%s]", adn.dsn, adn.model);
                Logger.logDebug(LOG_TAG, "rn: registered node [%s]", adn);
                Device device = SessionManager.sessionParameters().deviceCreator.deviceForAylaDevice(adn);
                gateway.setDefaultName(device, this);
            } else {
                state = NodeRegistrationFindState.NotStarted;
                resourceId = R.string.error_gateway_register_device_node;
                if (_completion.get() != null) {
                    _completion.get().handle(gateway, msg, this);
                }
            }
        }

        void updateComplete(Device device, Message msg) {
            this.device = device;
            Logger.logMessage(LOG_TAG, msg, "rn: update [%s:%s]", device.getDevice().dsn, device.getDevice().productName);
            device.postRegistrationForGatewayDevice(gateway);
            resourceId = R.string.gateway_registered_device_node;
            if (_completion.get() != null) {
                _completion.get().handle(gateway, msg, this);
            }
        }
    }

    ScanTag _nodeRegistrationTag;

    /**
     * Start the registration scan for this gateway.
     *
     * @param listener GatewayNodeRegistrationListener
     */
    public void startRegistrationScan(final GatewayNodeRegistrationListener listener) {
        Logger.logInfo(LOG_TAG, "rn: startRegistrationScan start");
        _nodeRegistrationTag = new ScanTag(this, listener, new ScanTagCompletionHandler() {
            @Override
            public void handle(Gateway gateway, Message msg, ScanTag tag) {
                gateway.closeJoinWindow();
                listener.registrationComplete(tag.device, msg, tag.resourceId);
                Logger.logInfo(LOG_TAG, "rn: startRegistrationScan complete");
                _nodeRegistrationTag = null;
            }
        });
        _nodeRegistrationTag.nextStep();;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// Close Join Window

    /**
     * Must be called when the application UI that invoked startRegistrationScan has
     * completed.
     */
    public void cleanupRegistrationScan() {
        _nodeRegistrationTag = null;
        closeJoinWindow();
    }

    void closeJoinWindowProperties(CloseTag tag) {
        AylaDeviceGateway gateway = (AylaDeviceGateway) getDevice();
        Map<String, String> callParams = new HashMap<String, String>();
        callParams.put("names", PROPERTY_JOIN_ENABLE + " " + PROPERTY_JOIN_STATUS);
        gateway.getProperties(new GetPropertyJoinDisableHandler(tag), callParams);
    }

    class GetPropertyJoinDisableHandler extends Handler {
        private CloseTag _tag;

        GetPropertyJoinDisableHandler(CloseTag tag) {  _tag = tag; }

        @Override
        public void handleMessage(Message msg) { _tag.joinDisableProperty(msg); }
    }

    interface CloseTagCompletionHandler {

        void handle(Gateway gateway, Message msg, CloseTag tag);
    }

    class CloseTag {
        WeakReference<CloseTagCompletionHandler> _completion;
        Gateway gateway;
        long startTicks;

        CloseTag(Gateway gateway, CloseTagCompletionHandler completion) {
            _completion = new WeakReference<CloseTagCompletionHandler>(completion);
            this.gateway = gateway;
            this.startTicks = System.currentTimeMillis();
        }

        void joinDisableProperty(Message msg) {
            Logger.logMessage(LOG_TAG, msg, "rn: closeJoinWindow getProperties JOIN_ENABLE/STATUS");
            if (AylaNetworks.succeeded(msg)) {
                AylaProperty[] props = AylaSystemUtils.gson.fromJson((String)msg.obj, AylaProperty[].class);
                int errorWhat = 0;
                int errorArg1 = 0;
                int got = 0;
                int set = 0;
                for (AylaProperty prop : props) {
                    if (prop.name.equals(PROPERTY_JOIN_ENABLE)) {
                        Logger.logVerbose(LOG_TAG, "rn: closeJoinWindow prop [%s]=[%s]", prop.name, prop.value);
                        if (prop.datapoint.nValue().intValue() == 0) {
                            got++;
                        } else {
                            AylaDatapoint dp = new AylaDatapoint();
                            dp.nValue(0);
                            AylaRestService rs = prop.createDatapoint(dp);
                            Message m = rs.execute();
                            Logger.logMessage(LOG_TAG, m, "rn: setProperty JOIN_ENABLE");
                            if (AylaNetworks.succeeded(m)) {
                                set++;
                            } else {
                                errorWhat = m.what;
                                errorArg1 = m.arg1;
                            }
                        }
                    } else if (prop.name.equals(PROPERTY_JOIN_STATUS)) {
                        Logger.logVerbose(LOG_TAG, "rn: closeJoinWindow prop [%s]=[%s]", prop.name, prop.value);
                        if (prop.datapoint.nValue().intValue() == 0) {
                            got++;
                        } else{
                            AylaDatapoint dp = new AylaDatapoint();
                            dp.nValue(0);
                            AylaRestService rs = prop.createDatapoint(dp);
                            Message m = rs.execute();
                            Logger.logMessage(LOG_TAG, m, "rn: setProperty JOIN_STATUS");
                            if (AylaNetworks.succeeded(m)) {
                                set++;
                            } else {
                                errorWhat = m.what;
                                errorArg1 = m.arg1;
                            }
                        }
                    }
                }
                if (got == 2) {
                    // good to go
                    if (_completion.get() != null) {
                        _completion.get().handle(gateway, msg, null);
                    }
                } else if (set > 0) {
                    if (System.currentTimeMillis() - startTicks > SCAN_TIMEOUT) {
                        msg.what = AylaNetworks.AML_ERROR_FAIL;
                        msg.arg1 = AylaNetworks.AML_ERROR_UNREACHABLE;
                        if (_completion.get() != null) {
                            _completion.get().handle(gateway, msg, null);
                        }
                    } else {
                        // verify/set values again
                        gateway.closeJoinWindowProperties(this);
                    }
                } else {
                    msg.what = errorWhat;
                    msg.arg1 = errorArg1;
                    if (_completion.get() != null) {
                        _completion.get().handle(gateway, msg, null);
                    }
                }
            } else {
                if (_completion.get() != null) {
                    _completion.get().handle(gateway, msg, null);
                }
            }
        }
    }

    CloseTag _closeJoinWindowTag;

    void closeJoinWindow() {
        // We need to do this any time the join window is left open
        Logger.logInfo(LOG_TAG, "rn: closeJoinWindow start");
        _closeJoinWindowTag = new CloseTag(this, new CloseTagCompletionHandler() {
            @Override
            public void handle(Gateway gateway, Message msg, CloseTag tag) {
                Logger.logInfo(LOG_TAG, "rn: closeJoinWindow complete %d:%d", msg.what, msg.arg1);
                _closeJoinWindowTag = null;
            }
        });
        closeJoinWindowProperties(_closeJoinWindowTag);
    }
}
