package com.aylanetworks.agilelink.framework;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.aylanetworks.aaml.AylaDeviceNode;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.zigbee.AylaDeviceZigbeeGateway;
import com.aylanetworks.aaml.zigbee.AylaDeviceZigbeeNode;
import com.aylanetworks.aaml.zigbee.AylaGroupZigbee;
import com.aylanetworks.aaml.zigbee.AylaNetworksZigbee;
import com.aylanetworks.aaml.zigbee.AylaSceneZigbee;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * ZigbeeSceneManager.java
 * AgileLink Application Framework
 *
 * Created by David Junod on 6/18/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class ZigbeeSceneManager {

    private final static String LOG_TAG = "ZigbeeSceneManager";

    private Gateway _gateway;
    private Set<ZigbeeSceneManagerListener> _listeners;
    protected Set<AylaSceneZigbee> _scenes;

    /**
     * Interface to notify of changes to the scene list or scenes within the list
     */
    public interface ZigbeeSceneManagerListener {

        void zigbeeSceneListChanged(Gateway gateway);

        void zigbeeSceneMembersChanged(Gateway gateway, AylaSceneZigbee scene);

        void zigbeeCreateSceneCompleted(Gateway gateway, String name, Message msg, AylaSceneZigbee scene);

        void zigbeeUpdateSceneCompleted(Gateway gateway, String name, Message msg, AylaSceneZigbee scene);

        void zigbeeRecallSceneCompleted(Gateway gateway, String name, Message msg, AylaSceneZigbee scene);

        void zigbeeDeleteSceneCompleted(Gateway gateway, String name, Message msg);
    }

    /**
     * Default constructor
     * @param gateway Gateway
     */
    public ZigbeeSceneManager(Gateway gateway) {
        _gateway = gateway;
        _listeners = new HashSet<>();
        _scenes = new HashSet<>();
    };

    // Listeners

    public void addListener(ZigbeeSceneManagerListener listener) {
        _listeners.add(listener);
    }

    public void removeListener(ZigbeeSceneManagerListener listener) {
        _listeners.remove(listener);
    }

    // Public Methods

    /**
     * Useful utility method for logging the current devices in AylaSceneZigbee object.
     * @param scene AylaSceneZigbee
     * @return A string containing the DSN's of the devices in the scene.
     */
    static public String getSceneNodesToString(AylaSceneZigbee scene) {
        StringBuilder sb = new StringBuilder(512);
        if (scene != null) {
            if (scene.nodeDsns != null) {
                for (String dsn : scene.nodeDsns) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(dsn);
                }
            } else if (scene.nodes != null) {
                for (AylaDeviceZigbeeNode node : scene.nodes) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(node.dsn);
                }
            }
        }
        return sb.toString();
    }

    /**
     * Get a list of the AylaDeviceNode devices that are in a AylaSceneZigbee scene. This method
     * is used to build the scene.nodes array of an AylaSceneZigbee instance.
     * @param scene AylaSceneZigbee
     * @return List of AylaDeviceNode devices in the scene.
     */
    static public List<AylaDeviceNode> getDeviceNodes(AylaSceneZigbee scene) {
        List<AylaDeviceNode> list = new ArrayList<>();
        if (scene != null) {
            // always try the String array first
            if (scene.nodeDsns != null) {
                Logger.logDebug(LOG_TAG, "zs: getDevices [%s] nodeDsns", scene.sceneName);
                for (String nodeDsn : scene.nodeDsns) {
                    Device d = SessionManager.deviceManager().deviceByDSN(nodeDsn);
                    if (d == null) {
                        Logger.logWarning(LOG_TAG, "zs: getDevices [%s] getDeviceByDSN [%s] NULL", scene.sceneName, nodeDsn);
                        continue;
                    }
                    Logger.logDebug(LOG_TAG, "zs: getDevices [%s] [%s]", scene.sceneName, nodeDsn);
                    list.add((AylaDeviceNode) d.getDevice());
                }
            } else if (scene.nodes != null) {
                Logger.logDebug(LOG_TAG, "zs: getDevices [%s] nodes", scene.sceneName);
                for (AylaDeviceZigbeeNode dn : scene.nodes) {
                    if (dn == null) {
                        Logger.logWarning(LOG_TAG, "zs: getDevices [%s] dn NULL", scene.sceneName);
                        continue;
                    }
                    if (TextUtils.equals(dn.action, "delete")) {
                        continue;
                    }
                    Device d = SessionManager.deviceManager().deviceByDSN(dn.dsn);
                    if (d == null) {
                        Logger.logWarning(LOG_TAG, "zs: getDevices [%s] getDeviceByDSN [%s] NULL", scene.sceneName, dn.dsn);
                        continue;
                    }
                    Logger.logDebug(LOG_TAG, "zs: getDevices [%s] [%s]", scene.sceneName, dn.dsn);
                    list.add(dn);
                }
            } else {
                Logger.logWarning(LOG_TAG, "zs: getDevices [%s] no nodes", scene.sceneName);
            }
        } else {
            Logger.logWarning(LOG_TAG, "zs: getDevices no scene");
        }
        return list;
    }

    /**
     * Get a list of the Device devices that are in a AylaSceneZigbee scene.
     * @param scene AylaSceneZigbee
     * @return List of Device devices in the scene.
     */
    static public List<Device> getDevices(AylaSceneZigbee scene) {
        List<Device> list = new ArrayList<>();
        if (scene != null) {
            // always try the String array first
            if (scene.nodeDsns != null) {
                Logger.logDebug(LOG_TAG, "zs: getDevices [%s] nodeDsns", scene.sceneName);
                for (String nodeDsn : scene.nodeDsns) {
                    Device d = SessionManager.deviceManager().deviceByDSN(nodeDsn);
                    if (d == null) {
                        Logger.logWarning(LOG_TAG, "zs: getDevices [%s] getDeviceByDSN [%s] NULL", scene.sceneName, nodeDsn);
                        continue;
                    }
                    Logger.logDebug(LOG_TAG, "zs: getDevices [%s] [%s]", scene.sceneName, nodeDsn);
                    list.add(d);
                }
            } else if (scene.nodes != null) {
                Logger.logDebug(LOG_TAG, "zs: getDevices [%s] nodes", scene.sceneName);
                for (AylaDeviceZigbeeNode dn : scene.nodes) {
                    if (dn == null) {
                        Logger.logWarning(LOG_TAG, "zs: getDevices [%s] dn NULL", scene.sceneName);
                        continue;
                    }
                    if (TextUtils.equals(dn.action, "delete")) {
                        continue;
                    }
                    Device d = SessionManager.deviceManager().deviceByDSN(dn.dsn);
                    if (d == null) {
                        Logger.logWarning(LOG_TAG, "zs: getDevices [%s] getDeviceByDSN [%s] NULL", scene.sceneName, dn.dsn);
                        continue;
                    }
                    Logger.logDebug(LOG_TAG, "zs: getDevices [%s] [%s]", scene.sceneName, dn.dsn);
                    list.add(d);
                }
            } else {
                Logger.logWarning(LOG_TAG, "zs: getDevices [%s] no nodes", scene.sceneName);
            }
        } else {
            Logger.logWarning(LOG_TAG, "zs: getDevices no scene");
        }
        return list;
    }

    /**
     * Determine if a Device is in a AylaSceneZigbee.
     * @param device Device to search for.
     * @param scene AylaSceneZigbee to search in
     * @return Return true if the device is in the scene, false if it isn't.
     */
    static public boolean isDeviceInScene(Device device, AylaSceneZigbee scene) {
        String dsn = device.getDevice().dsn;
        // always try the String array first
        if (scene.nodeDsns != null) {
            for (String nodeDsn : scene.nodeDsns) {
                if (TextUtils.equals(dsn, nodeDsn)) {
                    return true;
                }
            }
        } else if (scene.nodes != null) {
            for (AylaDeviceZigbeeNode dn : scene.nodes) {
                if (dn == null) {
                    continue;
                }
                if (TextUtils.equals(dsn, dn.dsn)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void fetchZigbeeScenesIfNeeded() {
        if ((_scenes == null) || (_scenes.size() == 0)) {
            fetchZigbeeScenes();
        }
    }

    /**
     * Fetches the list of scenes from the server. Listeners will be notified when the call is
     * complete by being called with zigbeeSceneListChanged() if the scene list has changed.
     */
    public void fetchZigbeeScenes() {
        Map<String, Object> callParams = new HashMap<>();
        callParams.put(AylaNetworksZigbee.kAylaZigbeeNodeParamDetail, "true");          // default is "false"
        callParams.put(AylaNetworksZigbee.kAylaZigbeeNodeParamStatusFilter, "active");  // default is "active"
        AylaDeviceZigbeeGateway gateway = (AylaDeviceZigbeeGateway)_gateway.getDevice();
        gateway.getScenes(new GetScenesHandler(this), callParams, false);
    }

    public List<AylaSceneZigbee> getScenes() {
        List<AylaSceneZigbee> scenes = new ArrayList<>(_scenes);
        Collections.sort(scenes, new Comparator<AylaSceneZigbee>() {
            @Override
            public int compare(AylaSceneZigbee lhs, AylaSceneZigbee rhs) {
                return lhs.sceneName.compareToIgnoreCase(rhs.sceneName);
            }
        });

        return scenes;
    }

    /**
     * Get the AylaSceneZigbee scene with the specified name.
     * @param name String name of the scene to locate.
     * @return AylaSceneZigbee matching the specified name.
     */
    public AylaSceneZigbee getByName(String name) {
        if ((_scenes != null) && (_scenes.size() > 0)) {
            for (AylaSceneZigbee scene : _scenes) {
                if (TextUtils.equals(scene.sceneName, name)) {
                    return scene;
                }
            }
        }
        return null;
    }

    public void createScene(String name, List<Device> devices, List<AylaGroupZigbee> groups, Object tag, Gateway.AylaGatewayCompletionHandler handler) {
        AylaSceneZigbee scene = new AylaSceneZigbee();
        scene.sceneName = name;
        scene.gatewayDsn = _gateway.getDeviceDsn();
        if ((devices != null) && (devices.size() > 0)) {
            scene.nodeDsns = new String[devices.size()];
            for (int i = 0; i < devices.size(); i++) {
                scene.nodeDsns[i] = devices.get(i).getDevice().dsn;
            }
        }
        if ((groups != null) && (groups.size() > 0)) {
            scene.groups = new Integer[groups.size()];
            for (int i = 0; i < groups.size(); i++) {
                scene.groups[i] = groups.get(i).getId();
            }
        }
        Map<String, Object> callParams = new HashMap<>();
        AylaDeviceZigbeeGateway gateway = (AylaDeviceZigbeeGateway)_gateway.getDevice();
        gateway.createScene(new CreateHandler(this, name, tag, handler), scene, callParams, false);
    }

    public void createScene(AylaSceneZigbee scene, Object tag, Gateway.AylaGatewayCompletionHandler handler) {
        Map<String, Object> callParams = new HashMap<>();
        AylaDeviceZigbeeGateway gateway = (AylaDeviceZigbeeGateway)_gateway.getDevice();
        gateway.createScene(new CreateHandler(this, scene.sceneName, tag, handler), scene, callParams, false);
    }

    public void updateScene(AylaSceneZigbee scene, Object tag, Gateway.AylaGatewayCompletionHandler handler) {
        Map<String, Object> callParams = new HashMap<>();
        AylaDeviceZigbeeGateway gateway = (AylaDeviceZigbeeGateway)_gateway.getDevice();
        gateway.updateScene(new UpdateHandler(this, scene, tag, handler), scene, callParams, false);
    }

    public void recallScene(AylaSceneZigbee scene, Object tag, Gateway.AylaGatewayCompletionHandler handler) {
        Map<String, Object> callParams = new HashMap<>();
        AylaDeviceZigbeeGateway gateway = (AylaDeviceZigbeeGateway)_gateway.getDevice();
        gateway.recallScene(new RecallHandler(this, scene, tag, handler), scene, callParams, false);
    }

    public void deleteScene(AylaSceneZigbee scene, Object tag, Gateway.AylaGatewayCompletionHandler handler) {
        Map<String, Object> callParams = new HashMap<>();
        AylaDeviceZigbeeGateway gateway = (AylaDeviceZigbeeGateway)_gateway.getDevice();
        gateway.deleteScene(new DeleteHandler(this, scene.sceneName, tag, handler), scene, callParams, false);
    }

    // Internal Handlers

    static class GetScenesHandler extends Handler {
        private WeakReference<ZigbeeSceneManager> _manager;

        GetScenesHandler(ZigbeeSceneManager manager) {
            _manager = new WeakReference<ZigbeeSceneManager>(manager);
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.logMessage(LOG_TAG, msg, "zs: getScenes");
            if (AylaNetworks.succeeded(msg)) {
                Set<AylaSceneZigbee> set = new HashSet<>();
                AylaSceneZigbee[] scenes = AylaSystemUtils.gson.fromJson((String) msg.obj, AylaSceneZigbee[].class);
                if (scenes != null) {
                    Logger.logDebug(LOG_TAG, "zs: getZigbeeScenes " + scenes.length + " scenes");
                    for (AylaSceneZigbee scene : scenes) {
                        if (scene == null) {
                            continue;
                        }
                        Logger.logDebug(LOG_TAG, "zs: getZigbeeScenes + [" + scene.sceneName + "] [" + getSceneNodesToString(scene) + "]");
                        Logger.logDebug(LOG_TAG, "zs: [%s]", scene);
                        set.add(scene);
                    }
                }
                _manager.get().setScenes(set);
            }
        }
    }

    static class CreateHandler extends Handler {
        private WeakReference<ZigbeeSceneManager> _manager;
        private Object _tag;
        private Gateway.AylaGatewayCompletionHandler _handler;
        String _name;

        CreateHandler(ZigbeeSceneManager manager, String name, Object tag, Gateway.AylaGatewayCompletionHandler handler) {
            _manager = new WeakReference<>(manager);
            _name = name;
            _tag = tag;
            _handler = handler;
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.logMessage(LOG_TAG, msg, "zs: createScene [%s]", _name);
            if (_manager.get() == null) {
                Logger.logWarning(LOG_TAG, "zs: ZigbeeSceneManager went away.");
            }
            if (msg.what == AylaNetworks.AML_ERROR_OK) {
                if (msg.arg1 == 206) {
                    // has failing nodes...
                    Logger.logWarning(LOG_TAG, "zs: createScene [%s] has failing nodes", _name);
                    _manager.get().notifyCreateCompleted(_name, msg, null);
                } else {
                    AylaSceneZigbee scene = AylaSystemUtils.gson.fromJson((String) msg.obj, AylaSceneZigbee.class);
                    _manager.get().addScene(scene);
                    _manager.get().notifyCreateCompleted(_name, msg, scene);
                }
            } else {
                _manager.get().notifyCreateCompleted(_name, msg, null);
            }
            if (_handler != null) {
                _handler.gatewayCompletion(_manager.get()._gateway, msg, _tag);
            }
        }
    }

    static class UpdateHandler extends Handler {
        private WeakReference<ZigbeeSceneManager> _manager;
        private Object _tag;
        private Gateway.AylaGatewayCompletionHandler _handler;
        private AylaSceneZigbee _scene;
        String _name;

        UpdateHandler(ZigbeeSceneManager manager, AylaSceneZigbee scene, Object tag, Gateway.AylaGatewayCompletionHandler handler) {
            _manager = new WeakReference<>(manager);
            _scene = scene;
            _name = scene.sceneName;
            _tag = tag;
            _handler = handler;
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.logMessage(LOG_TAG, msg, "zs: updateScene [%s]", _name);
            if (_manager.get() == null) {
                Logger.logWarning(LOG_TAG, "zs: ZigbeeSceneManager went away.");
            }
            if (msg.what == AylaNetworks.AML_ERROR_OK) {
                if (msg.arg1 == 206) {
                    // has failing nodes...
                    Logger.logWarning(LOG_TAG, "zs: updateScene [%s] has failing nodes", _name);
                    _manager.get().notifyUpdateCompleted(_name, msg, null);
                } else {
                    AylaSceneZigbee scene = AylaSystemUtils.gson.fromJson((String) msg.obj, AylaSceneZigbee.class);
                    _manager.get().updateScene(scene);
                    _manager.get().notifyUpdateCompleted(_name, msg, scene);
                }
            } else {
                _manager.get().notifyUpdateCompleted(_name, msg, null);
            }
            if (_handler != null) {
                _handler.gatewayCompletion(_manager.get()._gateway, msg, _tag);
            }
        }
    }

    static class RecallHandler extends Handler {
        private WeakReference<ZigbeeSceneManager> _manager;
        private Object _tag;
        private Gateway.AylaGatewayCompletionHandler _handler;
        private AylaSceneZigbee _scene;
        String _name;

        RecallHandler(ZigbeeSceneManager manager, AylaSceneZigbee scene, Object tag, Gateway.AylaGatewayCompletionHandler handler) {
            _manager = new WeakReference<>(manager);
            _scene = scene;
            _name = scene.sceneName;
            _tag = tag;
            _handler = handler;
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.logMessage(LOG_TAG, msg, "zs: recallScene [%s]", _name);
            if (_manager.get() == null) {
                Logger.logWarning(LOG_TAG, "zs: ZigbeeSceneManager went away.");
            }
            if (msg.what == AylaNetworks.AML_ERROR_OK) {
                if (msg.arg1 == 206) {
                    // has failing nodes...
                    Logger.logWarning(LOG_TAG, "zs: recallScene [%s] has failing nodes", _name);
                    _manager.get().notifyRecallCompleted(_name, msg, null);
                } else {
                    AylaSceneZigbee scene = AylaSystemUtils.gson.fromJson((String) msg.obj, AylaSceneZigbee.class);
                    // TODO: do we need to update the scene?
                    _manager.get().updateScene(scene);

                    _manager.get().notifyRecallCompleted(_name, msg, scene);
                }
            } else {
                _manager.get().notifyRecallCompleted(_name, msg, null);
            }
            if (_handler != null) {
                _handler.gatewayCompletion(_manager.get()._gateway, msg, _tag);
            }
        }
    }

    static class DeleteHandler extends Handler {
        private WeakReference<ZigbeeSceneManager> _manager;
        String _name;
        private Object _tag;
        private Gateway.AylaGatewayCompletionHandler _handler;

        DeleteHandler(ZigbeeSceneManager manager, String name, Object tag, Gateway.AylaGatewayCompletionHandler handler) {
            _manager = new WeakReference<>(manager);
            _tag = tag;
            _handler = handler;
            _name = name;
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.logMessage(LOG_TAG, msg, "zs: deleteScene [%s]", _name);
            if (_manager.get() == null) {
                Logger.logWarning(LOG_TAG, "zs: ZigbeeSceneManager went away.");
            }
            if (msg.what == AylaNetworks.AML_ERROR_OK) {
                if (msg.arg1 == 206) {
                    // has failing nodes...
                    Logger.logWarning(LOG_TAG, "zs: deleteScene [%s] has failing nodes", _name);
                    _manager.get().notifyDeleteCompleted(_name, msg);
                } else {
                    _manager.get().removeSceneByName(_name);
                    _manager.get().notifyDeleteCompleted(_name, msg);
                }
            } else {
                if (msg.arg1 == 404) {
                    // treat it like success, since it is now gone...
                    msg.what = AylaNetworks.AML_ERROR_OK;
                    _manager.get().removeSceneByName(_name);
                }
                _manager.get().notifyDeleteCompleted(_name, msg);
            }
            if (_handler != null) {
                _handler.gatewayCompletion(_manager.get()._gateway, msg, _tag);
            }
        }
    }

    // Internal Methods

    private void setScenes(Set<AylaSceneZigbee> set) {
        _scenes = set;
        notifyListChanged();
    }

    private void addScene(AylaSceneZigbee scene) {
        // add to internal list
        for (AylaSceneZigbee g : _scenes) {
            if (g.sceneName.equals(scene.sceneName)) {
                return;
            }
        }
        _scenes.add(scene);
        notifyListChanged();
    }

    private void updateScene(AylaSceneZigbee scene) {
        for (AylaSceneZigbee g : _scenes) {
            if (g.sceneName.equals(scene.sceneName)) {
                _scenes.remove(g);
                break;
            }
        }
        _scenes.add(scene);
        notifyListChanged();
    }

    private void removeSceneByName(String sceneName) {
        // remove from internal list
        for (AylaSceneZigbee g : _scenes) {
            if (g.sceneName.equals(sceneName)) {
                _scenes.remove(g);
                notifyListChanged();
                return;
            }
        }
    }

    protected void notifyListChanged() {
        for ( ZigbeeSceneManagerListener l : _listeners ) {
            l.zigbeeSceneListChanged(_gateway);
        }
    }

    protected void notifyMembersChanged(AylaSceneZigbee scene) {
        for ( ZigbeeSceneManagerListener l : _listeners ) {
            l.zigbeeSceneMembersChanged(_gateway, scene);
        }
    }

    protected void notifyCreateCompleted(String name, Message msg, AylaSceneZigbee scene) {
        for ( ZigbeeSceneManagerListener l : _listeners ) {
            l.zigbeeCreateSceneCompleted(_gateway, name, msg, scene);
        }
    }

    protected void notifyUpdateCompleted(String name, Message msg, AylaSceneZigbee scene) {
        for ( ZigbeeSceneManagerListener l : _listeners ) {
            l.zigbeeUpdateSceneCompleted(_gateway, name, msg, scene);
        }
    }

    protected void notifyRecallCompleted(String name, Message msg, AylaSceneZigbee scene) {
        for ( ZigbeeSceneManagerListener l : _listeners ) {
            l.zigbeeRecallSceneCompleted(_gateway, name, msg, scene);
        }
    }

    protected void notifyDeleteCompleted(String name, Message msg) {
        for ( ZigbeeSceneManagerListener l : _listeners ) {
            l.zigbeeDeleteSceneCompleted(_gateway, name, msg);
        }
    }

}
