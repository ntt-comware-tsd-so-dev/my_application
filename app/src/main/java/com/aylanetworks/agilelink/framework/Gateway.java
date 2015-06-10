package com.aylanetworks.agilelink.framework;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;

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
     * Get the gateway for a device node.
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
     * Given a list of devices, returns a list containing only devices owned by this gateway.
     * @param devices List of devices.
     * @return List of devices owned by this gateway.
     */
    public List<Device> filterDeviceList(List<Device> devices) {
        List<Device> list = new ArrayList<>();
        AylaDeviceGateway gateway = (AylaDeviceGateway)getDevice();
        if (gateway.nodes != null) {
            for (AylaDeviceNode node : gateway.nodes) {
                if (DeviceManager.isDsnInDeviceList(node.dsn, devices)) {
                    list.add(SessionManager.deviceManager().deviceByDSN(node.dsn));
                }
            }
        } else {
            Logger.logError(LOG_TAG, "rm: gateway [%s] has no nodes!", gateway.dsn);
        }
        return list;
    }

    /**
     * Returns the list of all devices for this gateway.
     * @return The list of devices.
     */
    public List<Device> deviceList() {
        return filterDeviceList(SessionManager.deviceManager().deviceList());
    }

    /**
     * Returns a list of available devices for this gateway of the specified class
     *
     * @param classes Array of class to match against
     * @return List of matching devices
     */
    public List<Device> getDevicesOfClass(Class[] classes) {
        return filterDeviceList(SessionManager.deviceManager().getDevicesOfClass(classes));
    }

    /**
     * Returns a list of available devices for this gateway of the specified type, as determined by
     * the GetDeviceComparable interface.
     *
     * @param comparable GetDeviceComparable used to compare.
     * @return List of matching devices
     */
    public List<Device> getDevicesOfComparableType(DeviceManager.GetDeviceComparable comparable) {
        return filterDeviceList(SessionManager.deviceManager().getDevicesOfComparableType(comparable));
    }


    /**
     * Interface used when scanning for and registering a gateway's device nodes
     */
    public interface GatewayNodeRegistrationListener {

        /**
         * Notify that a registration candidate has been successfully registered as a device.
         *
         * @param device Device
         * @param moreComing When register
         * @param tag Optional user specified data.
         */
        public void gatewayRegistrationCandidateAdded(Device device, boolean moreComing, Object tag);

        /**
         * Notify that registration candidates are available, the UI is then
         * given a chance to allow the user to select which devices to actually
         * register.  Then call back to gateway.registerCandidates
         *
         * @param list List of AylaDeviceNode objects
         * @param tag Optional user specified data.
         */
        public void gatewayRegistrationCandidates(List<AylaDeviceNode> list, Object tag);

        /**
         * Notify that the the processs of scanning and registering a gateway's
         * device nodes has completed.
         *
         * @param msg The final Message.
         * @param messageResourceId String resource id to display a toast to the user.
         * @param tag Optional user specified data.
         */
        public void gatewayRegistrationComplete(Message msg, int messageResourceId, Object tag);

    }

    // We need a simple, generic completion handler to use when performing a lot of different
    // steps that do not need notification along the way to anybody but the one performing
    // the steps.
    public interface AylaGatewayCompletionHandler {

        public void gatewayCompletion(Gateway gateway, Message msg, Object tag);
    }

    public Gateway(AylaDevice aylaDevice) {
        super(aylaDevice);
    }

    @Override
    public void deviceAdded(Device oldDevice) {
        super.deviceAdded(oldDevice);
        if (oldDevice != null) {
            Logger.logDebug(LOG_TAG, "zg: deviceAdded [%s] copy from old", getDevice().dsn);
            Gateway gateway = (Gateway)oldDevice;
            _groupManager = gateway._groupManager;
            _bindingManager = gateway._bindingManager;
        } else {
            Logger.logDebug(LOG_TAG, "zg: deviceAdded [%s] new", getDevice().dsn);
         }
        getGroupManager().fetchZigbeeGroupsIfNeeded();
        getBindingManager().fetchZigbeeBindingsIfNeeded();
    }

    public AylaDeviceGateway getGatewayDevice() {
        return (AylaDeviceGateway)getDevice();
    }

    /**
     * Called when a unregisterDevice is called on a DeviceNode
     *
     * @param device
     */
    public void removeDeviceNode(Device device) {
    }

    private ZigbeeGroupManager _groupManager;

    public ZigbeeGroupManager getGroupManager() {
        if (_groupManager == null) {
            _groupManager = new ZigbeeGroupManager(this);
        }
        return _groupManager;
    }

    private ZigbeeBindingManager _bindingManager;

    public ZigbeeBindingManager getBindingManager() {
        if (_bindingManager == null) {
            _bindingManager = new ZigbeeBindingManager(this);
        }
        return _bindingManager;
    }

    public void createGroup(String groupName, List<Device>deviceList, Object tag, AylaGatewayCompletionHandler handler) {
        getGroupManager().createGroup(groupName, deviceList, tag, handler);
    }

    public void updateGroup(AylaGroupZigbee group, Object tag, AylaGatewayCompletionHandler handler) {
        getGroupManager().updateGroup(group, tag, handler);
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

    void registerCandidate(AylaDeviceNode node, GatewayTag tag) {
        AylaDeviceGateway gateway = (AylaDeviceGateway) getDevice();
        gateway.registerCandidate(new RegistrationCandidateHandler(node, tag), node);
    }

    static class RegistrationCandidateHandler extends Handler {
        AylaDeviceNode _node;
        GatewayTag _tag;

        RegistrationCandidateHandler(AylaDeviceNode node, GatewayTag tag) { _tag = tag; _node = node; }

        @Override
        public void handleMessage(Message msg) {
            _tag.registerComplete(_node, msg); }
    }

    void setDefaultName(Device device, GatewayTag tag) {
        String name = device.deviceTypeName();
        String dsn = device.getDeviceDsn();
        List<Device> devices = SessionManager.deviceManager().deviceList();
        boolean existsName = false;
        int nameIteration = 2;
        do {
            existsName = false;
            for (Device d : devices) {
                if (TextUtils.equals(d.getDeviceDsn(), dsn)) {
                    continue;
                }
                if (TextUtils.equals(d.getDevice().productName, name)) {
                    existsName = true;
                }
            }
            if (existsName) {
                name = name + " " + (nameIteration++);
            }
        } while (existsName);
        Logger.logInfo(LOG_TAG, "rn: Register node rename [%s:%s] to [%s]", dsn, device.getDevice().productName, name);
        Map<String, String> params = new HashMap<>();
        params.put("productName", name);
        device.getDevice().update(new UpdateHandler(device, name, tag), params);
    }

    static class UpdateHandler extends Handler {
        Device _device;
        String _name;
        GatewayTag _tag;

        UpdateHandler(Device device, String name, GatewayTag tag) { _device = device; _name = name; _tag = tag; }

        @Override
        public void handleMessage(Message msg) {
            if (AylaNetworks.succeeded(msg)) {
                _device.getDevice().productName = _name;
                SessionManager.deviceManager().deviceChanged(_device);
            }
            _tag.updateComplete(_device, msg);
        }
    }

    class GatewayTag {
        Object tag;
        GatewayNodeRegistrationListener listener;
        Gateway gateway;
        List<AylaDeviceNode> list;
        Device device;
        int currentIndex;
        long startTicks;
        int resourceId;

        void completion(Message msg) { }

        void register() {
            AylaDeviceNode adn = list.get(currentIndex);
            Logger.logInfo(LOG_TAG, "rn: register node [%s:%s]", adn.dsn, adn.model);
            gateway.registerCandidate(adn, this);
        }

        void registerComplete(AylaDeviceNode node, Message msg) {
            Logger.logMessage(LOG_TAG, msg, "rn: registerCandidate [%s] for [%s]", node.dsn, gateway.getDeviceDsn());
            if (AylaNetworks.succeeded(msg)) {
                AylaDeviceNode adn = AylaSystemUtils.gson.fromJson((String) msg.obj, AylaDeviceNode.class);
                adn.connectionStatus = "Online";
                Logger.logInfo(LOG_TAG, "rn: registered node [%s:%s]", adn.dsn, adn.model);
                Logger.logDebug(LOG_TAG, "rn: registered node [%s]", adn);
                // let's get the real device list now from the server/gateway
                SessionManager.deviceManager().refreshDeviceListWithCompletion(adn, new DeviceManager.GetDevicesCompletion() {
                    @Override
                    public void complete(Message msg, List<Device> list, Object tag) {
                        AylaDeviceNode adn = (AylaDeviceNode) tag;
                        Logger.logMessage(LOG_TAG, msg, "rn: got devices");
                        if (AylaNetworks.succeeded(msg)) {
                            for (Device d : list) {
                                if (d.getDeviceDsn().equals(adn.dsn)) {
                                    // The device has successfully been added to the device list, now lets rename it.
                                    Logger.logInfo(LOG_TAG, "rn: registered device [%s:%s]", d.getDeviceDsn(), d.getDevice().model);
                                    Logger.logDebug(LOG_TAG, "rn: registered device [%s]", d);
                                    gateway.setDefaultName(d, GatewayTag.this);
                                    return;
                                }
                            }
                        }
                        Logger.logError(LOG_TAG, "rn: registered device [%s] not found on device list", adn.dsn);
                        resourceId = R.string.error_gateway_register_device_node;
                        completion(msg);
                    }
                });

            } else {
                resourceId = R.string.error_gateway_register_device_node;
                completion(msg);
            }
        }

        void updateComplete(Device device, Message msg) {
            Logger.logMessage(LOG_TAG, msg, "rn: update [%s:%s]", device.getDeviceDsn(), device.getDevice().productName);
            this.device = device;
            device.postRegistrationForGatewayDevice(gateway);
            if (currentIndex < list.size() - 1) {
                listener.gatewayRegistrationCandidateAdded(device, true, tag);

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {        // don't flood gateway
                    @Override
                    public void run() {
                        currentIndex++;
                        register();
                    }
                }, 500);                                    // Delay .5 seconds
            } else {
                listener.gatewayRegistrationCandidateAdded(device, false, tag);
                resourceId = R.string.gateway_registered_device_node;
                completion(msg);
            }
        }

        Message getTimeoutMessage() {
            Message msg = new Message();
            msg.what = AylaNetworks.AML_ERROR_FAIL;
            msg.arg1 = AylaNetworks.AML_ERROR_TIMEOUT;
            msg.obj = null;
            return msg;
        }
    }

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

    static class GetPropertyJoinEnableHandler extends Handler {
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

    static class RegistrationCandidatesHandler extends Handler {
        ScanTag _tag;

        RegistrationCandidatesHandler(ScanTag tag) { _tag = tag; }

        @Override
        public void handleMessage(Message msg) { _tag.candidatesComplete(msg); }
    }

    interface ScanTagCompletionHandler {

        void handle(Gateway gateway, Message msg, ScanTag tag);
    }

    /**
     * Interface for canceling the gateway registration candidate scan
     */
    public interface AylaGatewayScanCancelHandler {

        public void cancel();
    }

    class ScanTag extends GatewayTag implements AylaGatewayScanCancelHandler {
        ScanTagCompletionHandler _completion; // not weak, because it is the only thing holding on to it
        NodeRegistrationFindState state;
        boolean autoRegister;
        boolean running;

        ScanTag(Gateway gateway, boolean autoRegister, Object tag, GatewayNodeRegistrationListener listener, ScanTagCompletionHandler completion) {
            _completion = completion;
            this.autoRegister = autoRegister;
            this.tag = tag;
            this.listener = listener;
            this.gateway = gateway;
            this.state = NodeRegistrationFindState.Started;
            this.startTicks = System.currentTimeMillis();
            this.running = true;
        }

        void nextStep() {
            if (running == false) {
                Logger.logVerbose(LOG_TAG, "rn: Register node canceled.");
                return;
            }
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
            if (running == false) {
                Logger.logVerbose(LOG_TAG, "rn: Register node canceled.");
                return;
            }
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
                        if (_completion != null) {
                            Log.i(LOG_TAG, "rn: OJWTO completion handler");
                        } else {
                            Log.e(LOG_TAG, "rn: OJWTO no completion handler!");
                        }
                        completion(getTimeoutMessage());
                    } else {
                        Logger.logVerbose(LOG_TAG, "rn: Register node OJW postDelayed");
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {        // don't flood with retries
                            @Override
                            public void run() {
                                if (running == false) {
                                    Logger.logVerbose(LOG_TAG, "rn: Register node canceled.");
                                    return;
                                }
                                Logger.logVerbose(LOG_TAG, "rn: Register node OJW postDelayed run");
                                // verify/set values again
                                gateway.openJoinWindow(ScanTag.this);
                            }
                        }, 5000);                                    // Delay 5 seconds
                    }
                } else {
                    state = NodeRegistrationFindState.NotStarted;
                    resourceId = R.string.error_gateway_join_window;
                    completion(msg);
                }
            } else {
                state = NodeRegistrationFindState.NotStarted;
                resourceId = R.string.error_gateway_join_window;
                completion(msg);
            }
        }

        void candidatesComplete(final Message msg) {
            if (running == false) {
                Logger.logVerbose(LOG_TAG, "rn: Register node canceled.");
                return;
            }
            Logger.logMessage(LOG_TAG, msg, "rn: getRegistrationCandidates");
            if (AylaNetworks.succeeded(msg)) {
                // we have a list of candidates...
                list = new ArrayList<AylaDeviceNode>();
                AylaDeviceNode[] nodes = AylaSystemUtils.gson.fromJson((String)msg.obj, AylaDeviceNode[].class);
                String amOwnerStr = "";
                String oemModel = gateway.getDevice().oemModel;
                String gatewayDsn = gateway.getDeviceDsn();
                for (AylaDeviceNode node : nodes) {
                    if (node.amOwner()) {
                        amOwnerStr = "true";
                    } else {
                        amOwnerStr = "false";
                    }
                    node.connectionStatus = "Online";
                    if (TextUtils.isEmpty(node.oemModel)) {
                        node.oemModel = oemModel;
                    }
                    if (TextUtils.isEmpty(node.gatewayDsn)) {
                        node.gatewayDsn = gatewayDsn;
                    }
                    if (TextUtils.equals(node.productName, node.dsn)) {
                        Device fakeDevice = SessionManager.sessionParameters().deviceCreator.deviceForAylaDevice(node);
                        node.productName = fakeDevice.deviceTypeName();
                    }
                    Logger.logInfo(LOG_TAG, "rn: candidate DSN:%s amOwner:%s", node.dsn, amOwnerStr);
                    Logger.logDebug(LOG_TAG, "rn: candidate [%s]", node);
                    list.add(node);
                }

                if (autoRegister) {
                    // Auto register all the devices in the list
                    register();
                } else {
                    // Allow the UI to bring up a dialog showing which ones to register.
                    completion(msg);
                }
            } else {
                if (msg.arg1 == 412) {
                    if (System.currentTimeMillis() - startTicks > SCAN_TIMEOUT) {
                        Logger.logVerbose(LOG_TAG, "rn: Register node timeout");
                        state = NodeRegistrationFindState.Timeout;
                        resourceId = R.string.error_gateway_registration_candidates;
                        completion(getTimeoutMessage());
                    } else {
                        // invoke it again manually (412: retry open join window)
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {        // don't flood gateway
                            @Override
                            public void run() {
                                state = NodeRegistrationFindState.Started;
                                nextStep();
                            }
                        }, 500);                                    // Delay .5 seconds
                    }
                } else if (msg.arg1 == 404) {
                    if (System.currentTimeMillis() - startTicks > SCAN_TIMEOUT) {
                        Logger.logVerbose(LOG_TAG, "rn: Register node timeout");
                        state = NodeRegistrationFindState.Timeout;
                        resourceId = R.string.no_devices_found;
                        completion(getTimeoutMessage());
                    } else {
                        // invoke it again manually (404: retry get candidates)
                        Logger.logVerbose(LOG_TAG, "rn: Register node GRC postDelayed 404");
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {        // don't flood with retries
                            @Override
                            public void run() {
                                if (running == false) {
                                    Logger.logVerbose(LOG_TAG, "rn: Register node canceled.");
                                    return;
                                }
                                Logger.logVerbose(LOG_TAG, "rn: Register node GRC postDelayed run");
                                if (getPropertyBoolean(PROPERTY_JOIN_STATUS)) {
                                    Logger.logVerbose(LOG_TAG, "rn: Register node GRC FindDevices");
                                    gateway.registrationCandidates(ScanTag.this);
                                } else {
                                    state = NodeRegistrationFindState.NotStarted;
                                    resourceId = R.string.error_gateway_registration_candidates;
                                    completion(msg);
                                }
                            }
                        }, 5000);                                    // Delay 5 seconds
                    }
                } else {
                    // error message (restart)
                    state = NodeRegistrationFindState.NotStarted;
                    resourceId = R.string.error_gateway_registration_candidates;
                    completion(msg);
                }
            }
        }

        @Override
        void completion(Message msg) {
            if (_completion != null) {
                _completion.handle(gateway, msg, this);
            }
        }

        @Override
        public void cancel() {
            Logger.logInfo(LOG_TAG, "rn: Register node cancel");
            this.running = false;
            Message msg = new Message();
            msg.what = AylaNetworks.AML_ERROR_FAIL;
            msg.arg1 = AylaNetworks.AML_ERROR_FAIL;
            completion(msg);
        }
    }

    ScanTag _scanTag;

    /**
     * Start the gateway registration candidate scan.
     *
     * @param autoRegister When set to true, then all devices found will be automatically registered
     *                     to the same gateway. When set to false, then the UI is given the chance to
     *                     show a dialog through the GatewayNodeRegistrationListener
     *                     gatewayRegistrationCandidates method for the user to select which devices
     *                     to register.
     * @param userTag Optional user object to pair with all method calls to the listener.
     * @param listener GatewayNodeRegistrationListener for tracking progress.
     * @return AylaGatewayScanCancelHandler used to cancel the scan.
     */
    public AylaGatewayScanCancelHandler startRegistrationScan(boolean autoRegister, Object userTag, GatewayNodeRegistrationListener listener) {
        Logger.logInfo(LOG_TAG, "rn: startRegistrationScan start");
        _scanTag = new ScanTag(this, autoRegister, userTag, listener, new ScanTagCompletionHandler() {
            @Override
            public void handle(Gateway gateway, Message msg, ScanTag scanTag) {
                Logger.logMessage(LOG_TAG, msg, "rn: startRegistrationScan");
                if (AylaNetworks.succeeded(msg)) {
                    if (scanTag.autoRegister) {
                        // all candidates have been registered
                        gateway.closeJoinWindow();
                        scanTag.listener.gatewayRegistrationComplete(msg, scanTag.resourceId, scanTag.tag);
                    } else if ((scanTag.list == null) || (scanTag.list.size() == 0)) {
                        gateway.closeJoinWindow();
                        Logger.logWarning(LOG_TAG, "rn: startRegistrationScan success but empty/null list");
                        scanTag.resourceId = R.string.error_gateway_registration_candidates;
                        scanTag.listener.gatewayRegistrationComplete(msg, scanTag.resourceId, scanTag.tag);
                    } else {
                        // present the candidate list to the user
                        scanTag.listener.gatewayRegistrationCandidates(scanTag.list, scanTag.tag);
                    }
                } else {
                    gateway.closeJoinWindow();
                    scanTag.listener.gatewayRegistrationComplete(msg, scanTag.resourceId, scanTag.tag);
                }
                Logger.logInfo(LOG_TAG, "rn: startRegistrationScan complete");
                _scanTag = null;
            }
        });
        _scanTag.nextStep();
        return _scanTag;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// Register candidate


    interface RegisterCompletionHandler {

        void handle(Gateway gateway, Message msg, RegisterTag tag);
    }

    class RegisterTag extends GatewayTag {
        RegisterCompletionHandler _completion; // not weak, because it is the only thing holding on to it

        RegisterTag(Gateway gateway, List<AylaDeviceNode> list, Object tag, GatewayNodeRegistrationListener listener, RegisterCompletionHandler completion) {
            _completion = completion;
            this.tag = tag;
            this.listener = listener;
            this.gateway = gateway;
            this.list = new ArrayList<AylaDeviceNode>(list);
            this.startTicks = System.currentTimeMillis();
            this.currentIndex = 0;
        }

        @Override
        void completion(Message msg) {
            if (_completion != null) {
                _completion.handle(gateway, msg, this);
            }
        }
    }

    RegisterTag _registerTag;

    public void registerCandidates(List<AylaDeviceNode> list, Object userTag, GatewayNodeRegistrationListener listener) {
        Logger.logInfo(LOG_TAG, "rn: registerCandidates start");
        _registerTag = new RegisterTag(this, list, userTag, listener, new RegisterCompletionHandler() {
            @Override
            public void handle(Gateway gateway, Message msg, RegisterTag registerTag) {
                gateway.closeJoinWindow();
                registerTag.listener.gatewayRegistrationComplete(msg, registerTag.resourceId, registerTag.tag);
                Logger.logInfo(LOG_TAG, "rn: registerCandidates complete");
                _registerTag = null;
            }
        });
        _registerTag.register();;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// Close Join Window

    /**
     * Must be called when the application UI that invoked startRegistrationScan has
     * completed.
     */
    public void cleanupRegistrationScan() {
        _scanTag = null;
        closeJoinWindow();
    }

    void closeJoinWindowProperties(CloseTag tag) {
        AylaDeviceGateway gateway = (AylaDeviceGateway) getDevice();
        Map<String, String> callParams = new HashMap<String, String>();
        callParams.put("names", PROPERTY_JOIN_ENABLE + " " + PROPERTY_JOIN_STATUS);
        gateway.getProperties(new GetPropertyJoinDisableHandler(tag), callParams);
    }

    static class GetPropertyJoinDisableHandler extends Handler {
        private CloseTag _tag;

        GetPropertyJoinDisableHandler(CloseTag tag) {  _tag = tag; }

        @Override
        public void handleMessage(Message msg) { _tag.joinDisableProperty(msg); }
    }

    interface CloseTagCompletionHandler {

        void handle(Gateway gateway, Message msg, CloseTag tag);
    }

    class CloseTag {
        CloseTagCompletionHandler _completion; // not weak, because it is the only thing holding on to it
        Gateway gateway;
        long startTicks;

        CloseTag(Gateway gateway, CloseTagCompletionHandler completion) {
            _completion = completion;
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
                    completion(msg);
                } else if (set > 0) {
                    if (System.currentTimeMillis() - startTicks > SCAN_TIMEOUT) {
                        completion(getTimeoutMessage());
                    } else {
                        // verify/set values again
                        gateway.closeJoinWindowProperties(this);
                    }
                } else {
                    msg.what = errorWhat;
                    msg.arg1 = errorArg1;
                    completion(msg);
                }
            } else {
                completion(msg);
            }
        }

        Message getTimeoutMessage() {
            Message msg = new Message();
            msg.what = AylaNetworks.AML_ERROR_FAIL;
            msg.arg1 = AylaNetworks.AML_ERROR_TIMEOUT;
            msg.obj = null;
            return msg;
        }

        void completion(Message msg) {
            if (_completion != null) {
                _completion.handle(gateway, msg, this);
            }
        }
    }

    CloseTag _closeTag;

    void closeJoinWindow() {
        // We need to do this any time the join window is left open
        Logger.logInfo(LOG_TAG, "rn: closeJoinWindow start");
        _closeTag = new CloseTag(this, new CloseTagCompletionHandler() {
            @Override
            public void handle(Gateway gateway, Message msg, CloseTag tag) {
                Logger.logInfo(LOG_TAG, "rn: closeJoinWindow complete %d:%d", msg.what, msg.arg1);
                _closeTag = null;
            }
        });
        closeJoinWindowProperties(_closeTag);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// Identify Device Node

    static class IdentifyHandler extends Handler {
        WeakReference<Gateway> _gateway;
        Object _userTag;
        AylaGatewayCompletionHandler _completion;

        public IdentifyHandler(Gateway gateway, Object userTag, AylaGatewayCompletionHandler completion) {
            _gateway = new WeakReference<>(gateway);
            _userTag = userTag;
            _completion = completion;
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.logMessage(LOG_TAG, msg, "adn: identify");
            Logger.logInfo(LOG_TAG, "adn: identifyDeviceNode complete");
            if (_completion != null) {
                _completion.gatewayCompletion(_gateway.get(), msg, _userTag);
            }
        }
    }

    /**
     * Used to identify a node by blinking a light, making a sound, vibrating, etc
     *
     * @param device Gateway Device to identify.
     * @param on true to turn on, false to turn off.
     * @param time Duration, in seconds, to identify device. 0 - 255.
     * @param userTag Option user data to return with handler.
     * @param handler AylaGatewayCompletionHandler completion handler.
     */
    public void identifyDeviceNode(Device device, boolean on, int time, Object userTag, AylaGatewayCompletionHandler handler) {
        if ((device == null) || !device.isDeviceNode()) {
            handler.gatewayCompletion(this, null, userTag);
            return;
        }
        identifyAylaDeviceNode((AylaDeviceNode)device.getDevice(), on, time, userTag, handler);
    }

    /**
     * Used to identify an AylaDeviceNode by blinking a light, making a sound, vibrating, etc.
     * @param adn AylaDeviceNode to identify.
     * @param on true to turn on, false to turn off.
     * @param time Duration, in seconds, to identify device. 0 - 255.
     * @param userTag Option user data to return with handler.
     * @param handler AylaGatewayCompletionHandler completion handler.
     */
    public void identifyAylaDeviceNode(AylaDeviceNode adn, boolean on, int time, Object userTag, AylaGatewayCompletionHandler handler) {
        if (adn == null) {
            handler.gatewayCompletion(this, null, userTag);
            return;
        }
        Logger.logInfo(LOG_TAG, "adn: identifyDeviceNode start");
        Map<String, String> callParams = new HashMap<String, String>();
        //callParams.put(AylaDeviceNode.kAylaNodeParamIdentifyValue, AylaDeviceNode.kAylaNodeParamIdentifyResult);
        if (on) {
            callParams.put(AylaDeviceNode.kAylaNodeParamIdentifyValue, AylaDeviceNode.kAylaNodeParamIdentifyOn);
            callParams.put(AylaDeviceNode.kAylaNodeParamIdentifyTime, ""+time);
        } else {
            callParams.put(AylaDeviceNode.kAylaNodeParamIdentifyValue, AylaDeviceNode.kAylaNodeParamIdentifyOff);
        }
        adn.identify(new IdentifyHandler(this, userTag, handler), callParams);
    }

}
