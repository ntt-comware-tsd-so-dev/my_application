package com.aylanetworks.agilelink.framework;
/* 
 * ZigbeeBindingManager.java
 * AgileLink Application Framework
 *
 * Created by David N. Junod on 5/27/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.zigbee.AylaBindingZigbee;
import com.aylanetworks.aaml.zigbee.AylaDeviceZigbeeGateway;
import com.aylanetworks.aaml.zigbee.AylaNetworksZigbee;
import com.aylanetworks.agilelink.device.ZigbeeGateway;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ZigbeeBindingManager {

    private final static String LOG_TAG = "ZigbeeBindingManager";
    private boolean _isDirty;

    private ZigbeeGateway _gateway;
    private Set<ZigbeeBindingManagerListener> _listeners;
    protected Set<AylaBindingZigbee> _bindings;

    /**
     * Default constructor
     * @param gateway Gateway
     */
    public ZigbeeBindingManager(ZigbeeGateway gateway) {
        _gateway = gateway;
        _listeners = new HashSet<>();
        _bindings = new HashSet<>();
    };

    // Listeners

    /**
     * Interface to notify of changes to the binding list or bindings within the list.
     */
    public interface ZigbeeBindingManagerListener {

        /**
         * Binding list has changed.
         * @param gateway Gateway
         */
        void zigbeeBindingListChanged(Gateway gateway);

        /**
         * Binding has been created.
         * @param gateway Gateway
         * @param name Binding name.
         * @param msg Ayla Message
         * @param binding AylaBindingZigbee
         */
        void zigbeeCreateBindingCompleted(Gateway gateway, String name, Message msg, AylaBindingZigbee binding);

        /**
         * Binding has been deleted.
         * @param gateway Gateway
         * @param name Binding name.
         * @param msg Ayla Message
         */
        void zigbeeDeleteBindingCompleted(Gateway gateway, String name, Message msg);
    }

    /**
     * Add a listener for observing changes to the ZigbeeBindingManager.
     * @param listener ZigbeeBindingManagerListener to add.
     */
    public void addListener(ZigbeeBindingManagerListener listener) {
        _listeners.add(listener);
    }

    /**
     * Remove a formerly added observer.
     * @param listener ZigbeeBindingManagerListener to remove.
     */
    public void removeListener(ZigbeeBindingManagerListener listener) {
        _listeners.remove(listener);
    }

    // Public Methods

    /**
     * Fetch the binding list if it hasn't been fetched before.
     */
    public void fetchZigbeeBindingsIfNeeded() {
        if ((_bindings == null) || (_bindings.size() == 0)) {
            fetchZigbeeBindings(null, null);
        }
    }

    /**
     * Fetches the list of bindings from the server. Listeners will be notified when the call is
     * complete by being called with zigbeeBindingListChanged() if the binding list has changed.
     * @param tag Optional user tag for completion handler.
     * @param completion Optional completion handler.
     */
    public void fetchZigbeeBindings(Object tag, Gateway.AylaGatewayCompletionHandler completion) {
        Map<String, Object> callParams = new HashMap<String, Object>();
        callParams.put(AylaNetworksZigbee.kAylaZigbeeNodeParamDetail, "true");          // default is "false"
        callParams.put(AylaNetworksZigbee.kAylaZigbeeNodeParamStatusFilter, "active");  // default is "active"
        AylaDeviceZigbeeGateway gateway = (AylaDeviceZigbeeGateway)_gateway.getDevice();
        gateway.getBindings(new GetHandler(this, _gateway, tag, completion), callParams, false);
    }

    /**
     * Get the current list of all the AylaBindingZigbee objects.
     * @return List of AylaBindingZigbee objects.
     */
    public List<AylaBindingZigbee> getBindings() {
        return sortBindingSet(_bindings);
    }

    /**
     * Get the AylaBindingZigbee binding with the specified name.
     * @param name String name of the binding to locate
     * @return AylaBindingZigbee matching the name
     */
    public AylaBindingZigbee getByName(String name) {
        if ((_bindings != null) && (_bindings.size() > 0)) {
            for (AylaBindingZigbee binding : _bindings) {
                if (TextUtils.equals(binding.bindingName, name)) {
                    return binding;
                }
            }
        }
        return null;
    }

    /**
     * Compare two bindings to see if they are the same
     * @param binding1 AylaBindingZigbee
     * @param binding2 AylaBindingZigbee
     * @return Returns true if they are the same, false if they are not
     */
    public static boolean isBindingSame(AylaBindingZigbee binding1, AylaBindingZigbee binding2) {
        if (binding1.id != binding2.id) {
            return false;
        }
        if (!TextUtils.equals(binding1.bindingName, binding2.bindingName)) {
            return false;
        }
        if (!TextUtils.equals(binding1.action, binding2.action)) {
            return false;
        }
        if (!TextUtils.equals(binding1.status, binding2.status)) {
            return false;
        }
        if (!TextUtils.equals(binding1.toString(), binding2.toString())) {
            return false;
        }
        return true;
    }

    /**
     * Create the specified AylaBindingZigbee object.
     * @param binding AylaBindingZigbee to create.
     * @param tag Optional user data.
     * @param handler Optional completion handler
     */
    public void createBinding(AylaBindingZigbee binding, Object tag, Gateway.AylaGatewayCompletionHandler handler) {
        Map<String, Object> callParams = new HashMap<>();
        AylaDeviceZigbeeGateway gateway = (AylaDeviceZigbeeGateway)_gateway.getDevice();
        gateway.createBinding(new CreateHandler(this, binding.bindingName, tag, handler), binding, callParams, false);
    }

    /**
     * Delete the specified AylaBindingZigbee object.
     * @param binding AylaBindingZigbee object to delete.
     * @param tag Optional user data.
     * @param handler Optional completion handler.
     */
    public void deleteBinding(AylaBindingZigbee binding, Object tag, Gateway.AylaGatewayCompletionHandler handler) {
        Map<String, Object> callParams = new HashMap<>();
        AylaDeviceZigbeeGateway gateway = (AylaDeviceZigbeeGateway)_gateway.getDevice();
        gateway.deleteBinding(new DeleteHandler(this, binding.bindingName, tag, handler), binding, callParams, false);
    }

    // Internal Handlers

    static class GetHandler extends Handler {
        private WeakReference<ZigbeeBindingManager> _manager;
        ZigbeeGateway _gateway;
        Object _tag;
        Gateway.AylaGatewayCompletionHandler _completion;

        GetHandler(ZigbeeBindingManager manager, ZigbeeGateway gateway, Object tag, Gateway.AylaGatewayCompletionHandler completion) {
            _manager = new WeakReference<ZigbeeBindingManager>(manager);
            _gateway = gateway;
            _tag = tag;
            _completion = completion;
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.logMessage(LOG_TAG, msg, "zg: getBindings");
            if (AylaNetworks.succeeded(msg)) {
                Set<AylaBindingZigbee> set = new HashSet<>();
                AylaBindingZigbee[] bindings = AylaSystemUtils.gson.fromJson((String)msg.obj, AylaBindingZigbee[].class);
                if (bindings != null) {
                    Logger.logDebug(LOG_TAG, "zg: getZigbeeBindings %d bindings", bindings.length);
                    for (AylaBindingZigbee binding : bindings) {
                        if (binding == null) {
                            continue;
                        }
                        Logger.logDebug(LOG_TAG, "zg: getZigbeeBindings + [%s]", binding.bindingName);
                        //Logger.logDebug(LOG_TAG, "zg: [%s]", binding);
                        set.add(binding);
                    }
                }
                _manager.get().setBindings(set);
            }
            if (_completion != null) {
                _completion.gatewayCompletion(_gateway, msg, _tag);
            }
        }
    }

    static class CreateHandler extends Handler {
        private WeakReference<ZigbeeBindingManager> _manager;
        String _name;
        private Object _tag;
        private Gateway.AylaGatewayCompletionHandler _handler;

        CreateHandler(ZigbeeBindingManager manager, String name, Object tag, Gateway.AylaGatewayCompletionHandler handler) {
            _manager = new WeakReference<ZigbeeBindingManager>(manager);
            _name = name;
            _tag = tag;
            _handler = handler;
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.logMessage(LOG_TAG, msg, "zg: createBinding [%s]", _name);
            if (_manager.get() == null) {
                Logger.logWarning(LOG_TAG, "zg: ZigbeeBindingManager went away.");
            }
            if (msg.what == AylaNetworks.AML_ERROR_OK) {
                if (msg.arg1 == 206) {
                    // has failing nodes...
                    Logger.logWarning(LOG_TAG, "zg: createBinding [%s] has failing nodes", _name);
                    _manager.get().notifyCreateCompleted(_name, msg, null);
                } else {
                    AylaBindingZigbee binding = AylaSystemUtils.gson.fromJson((String) msg.obj, AylaBindingZigbee.class);
                    _manager.get().addBinding(binding);
                    _manager.get().notifyCreateCompleted(_name, msg, binding);
                }
            } else {
                _manager.get().notifyCreateCompleted(_name, msg, null);
            }
            if (_handler != null) {
                _handler.gatewayCompletion(_manager.get()._gateway, msg, _tag);
            }
        }
    }

    static class DeleteHandler extends Handler {
        private WeakReference<ZigbeeBindingManager> _manager;
        String _name;
        private Object _tag;
        private Gateway.AylaGatewayCompletionHandler _handler;

        DeleteHandler(ZigbeeBindingManager manager, String name, Object tag, Gateway.AylaGatewayCompletionHandler handler) {
            _manager = new WeakReference<ZigbeeBindingManager>(manager);
            _name = name;
            _tag = tag;
            _handler = handler;
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.logMessage(LOG_TAG, msg, "zg: deleteBinding [%s]", _name);
            if (_manager.get() == null) {
                Logger.logWarning(LOG_TAG, "zg: ZigbeeBindingManager went away.");
            }
            if (msg.what == AylaNetworks.AML_ERROR_OK) {
                if (msg.arg1 == 206) {
                    // has failing nodes...
                    Logger.logWarning(LOG_TAG, "zg: deleteBinding [%s] has failing nodes", _name);
                    _manager.get().notifyDeleteCompleted(_name, msg);
                } else {
                    _manager.get().removeBindingByName(_name);
                    _manager.get().notifyDeleteCompleted(_name, msg);
                }
            } else {
                if (msg.arg1 == 404) {
                    // treat it like success, since it is now gone...
                    msg.what = AylaNetworks.AML_ERROR_OK;
                    _manager.get().removeBindingByName(_name);
                }
                _manager.get().notifyDeleteCompleted(_name, msg);
            }
            if (_handler != null) {
                _handler.gatewayCompletion(_manager.get()._gateway, msg, _tag);
            }
        }
    }

    // Internal Methods

    private List<AylaBindingZigbee> sortBindingSet(Set<AylaBindingZigbee> bindingSet) {
        List<AylaBindingZigbee> bindings = new ArrayList<>(bindingSet);
        Collections.sort(bindings, new Comparator<AylaBindingZigbee>() {
            @Override
            public int compare(AylaBindingZigbee lhs, AylaBindingZigbee rhs) {
                return lhs.bindingName.compareToIgnoreCase(rhs.bindingName);
            }
        });
        return bindings;
    }

    private boolean bindingListChanged(Set<AylaBindingZigbee> newBindingSet) {
        if ((_bindings == null) && (newBindingSet == null)) {
            return false;
        }
        if ((_bindings == null) && (newBindingSet != null)) {
            return true;
        }
        if ((_bindings != null) && (newBindingSet == null)) {
            return true;
        }
        if (_bindings.size() != newBindingSet.size()) {
            return true;
        }
        List<AylaBindingZigbee> bindings1 = sortBindingSet(_bindings);
        List<AylaBindingZigbee> bindings2 = sortBindingSet(newBindingSet);
        for (int i = 0; i < bindings1.size(); i++) {
            AylaBindingZigbee binding1 = bindings1.get(i);
            AylaBindingZigbee binding2 = bindings2.get(i);
            if (!isBindingSame(binding1, binding2)) {
                return true;
            }
        }
        return false;
    }

    private void setBindings(Set<AylaBindingZigbee> set) {
        if (bindingListChanged(set)) {
            _bindings = set;
            notifyListChanged();
        }
    }

    private void addBinding(AylaBindingZigbee binding) {
        // add to internal list
        for (AylaBindingZigbee b : _bindings) {
            if (b.bindingName.equals(binding.bindingName)) {
                return;
            }
        }
        _bindings.add(binding);
        notifyListChanged();
    }

    private void removeBindingByName(String name) {
        // remove from internal list
        for (AylaBindingZigbee b : _bindings) {
            if (b.bindingName.equals(name)) {
                _bindings.remove(b);
                notifyListChanged();
                return;
            }
        }
    }

    protected void notifyListChanged() {
        for ( ZigbeeBindingManagerListener l : _listeners ) {
            l.zigbeeBindingListChanged(_gateway);
        }
    }

    protected void notifyCreateCompleted(String name, Message msg, AylaBindingZigbee binding) {
        for ( ZigbeeBindingManagerListener l : _listeners ) {
            l.zigbeeCreateBindingCompleted(_gateway, name, msg, binding);
        }
    }

    protected void notifyDeleteCompleted(String name, Message msg) {
        for ( ZigbeeBindingManagerListener l : _listeners ) {
            l.zigbeeDeleteBindingCompleted(_gateway, name, msg);
        }
    }
}
