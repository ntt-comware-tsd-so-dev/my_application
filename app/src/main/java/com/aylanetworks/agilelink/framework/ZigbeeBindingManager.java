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

    private Gateway _gateway;
    private Set<ZigbeeBindingManagerListener> _listeners;
    protected Set<AylaBindingZigbee> _bindings;

    public interface ZigbeeBindingManagerListener {

        void zigbeeBindingListChanged(Gateway gateway);

        void zigbeeCreateBindingCompleted(Gateway gateway, String name, Message msg, AylaBindingZigbee binding);

        void zigbeeDeleteBindingCompleted(Gateway gateway, String name, Message msg);
    }

    /**
     * Default constructor
     */
    public ZigbeeBindingManager(Gateway gateway) {
        _gateway = gateway;
        _listeners = new HashSet<>();
        _bindings = new HashSet<>();
    };

    // Listeners

    public void addListener(ZigbeeBindingManagerListener listener) {
        _listeners.add(listener);
    }

    public void removeListener(ZigbeeBindingManagerListener listener) {
        _listeners.remove(listener);
    }

    // Public Methods

    public void fetchZigbeeBindingsIfNeeded() {
        if ((_bindings == null) || (_bindings.size() == 0)) {
            fetchZigbeeBindings();
        }
    }

    public void fetchZigbeeBindings() {
        Map<String, Object> callParams = new HashMap<String, Object>();
        callParams.put(AylaNetworksZigbee.kAylaZigbeeNodeParamDetail, "true");          // default is "false"
        callParams.put(AylaNetworksZigbee.kAylaZigbeeNodeParamStatusFilter, "active");  // default is "active"
        AylaDeviceZigbeeGateway gateway = (AylaDeviceZigbeeGateway)_gateway.getDevice();
        gateway.getBindings(new GetHandler(this), callParams, false);
    }

    public List<AylaBindingZigbee> getBindings() {
        List<AylaBindingZigbee> groups = new ArrayList<>(_bindings);
        Collections.sort(groups, new Comparator<AylaBindingZigbee>() {
            @Override
            public int compare(AylaBindingZigbee lhs, AylaBindingZigbee rhs) {
                return lhs.bindingName.compareToIgnoreCase(rhs.bindingName);
            }
        });

        return groups;
    }

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

    public void createBinding(AylaBindingZigbee binding, Object tag, Gateway.AylaGatewayCompletionHandler handler) {
        Map<String, Object> callParams = new HashMap<String, Object>();
        AylaDeviceZigbeeGateway gateway = (AylaDeviceZigbeeGateway)_gateway.getDevice();
        gateway.createBinding(new CreateHandler(this, binding.bindingName, tag, handler), binding, callParams, false);
    }

    public void deleteBinding(AylaBindingZigbee binding, Object tag, Gateway.AylaGatewayCompletionHandler handler) {
        Map<String, Object> callParams = new HashMap<String, Object>();
        AylaDeviceZigbeeGateway gateway = (AylaDeviceZigbeeGateway)_gateway.getDevice();
        gateway.deleteBinding(new DeleteHandler(this, binding.bindingName, tag, handler), binding, callParams, false);
    }

    // Internal Handlers

    static class GetHandler extends Handler {
        private WeakReference<ZigbeeBindingManager> _manager;

        GetHandler(ZigbeeBindingManager manager) {
            _manager = new WeakReference<ZigbeeBindingManager>(manager);
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
                        Logger.logDebug(LOG_TAG, "zg: [%s]", binding);
                        set.add(binding);
                    }
                }
                _manager.get().setBindings(set);
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
                _handler.handle(_manager.get()._gateway, msg, _tag);
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
                _handler.handle(_manager.get()._gateway, msg, _tag);
            }
        }
    }

    // Internal Methods

    private void setBindings(Set<AylaBindingZigbee> set) {
        _bindings = set;
        notifyListChanged();
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
